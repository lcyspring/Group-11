#!/usr/bin/env bash
# CRM read-only performance baseline. The only CLI argument is a YAML path.

set -Eeuo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
source "${SCRIPT_DIR}/lib/yaml-config.sh"

[[ $# -eq 1 ]] || { printf 'Usage: bash ./verify-crm-performance-baseline.sh <config.yaml>\n' >&2; exit 2; }
yaml_config_init "$1"
[[ "$(yaml_require schema_version)" == "1" ]] || { printf 'Unsupported schema_version.\n' >&2; exit 2; }

BASE_URL="$(yaml_require endpoint.base_url)"
TENANT_ID="$(yaml_positive_integer endpoint.tenant_id)"
USERNAME="$(yaml_require account.username)"
PASSWORD="$(yaml_require account.password)"
WARMUP_REQUESTS="$(yaml_positive_integer workload.warmup_requests)"
REQUESTS="$(yaml_positive_integer workload.requests_per_scenario)"
CONCURRENCY="$(yaml_positive_integer workload.concurrency)"
TIMEOUT_SECONDS="$(yaml_positive_integer workload.timeout_seconds)"
PAGE_SIZE="$(yaml_positive_integer workload.page_size)"
DEPARTMENT_ID="$(yaml_positive_integer workload.department_id)"
INTERVAL="$(yaml_positive_integer workload.interval)"
START_TIME="$(yaml_require workload.start_time)"
END_TIME="$(yaml_require workload.end_time)"
MAX_ERROR_RATE="$(yaml_require thresholds.max_error_rate_percent)"
MAX_P95_MS="$(yaml_positive_integer thresholds.max_p95_ms)"
MAX_P99_MS="$(yaml_positive_integer thresholds.max_p99_ms)"
MIN_THROUGHPUT="$(yaml_require thresholds.min_throughput_rps)"
SERVER_CONTAINER="$(yaml_require containers.server)"
MYSQL_CONTAINER="$(yaml_require containers.mysql)"
REDIS_CONTAINER="$(yaml_require containers.redis)"
MYSQL_USER="$(yaml_require mysql.user)"
MYSQL_PASSWORD="$(yaml_require mysql.password)"
MYSQL_DATABASE="$(yaml_require mysql.database)"
OUTPUT_DIR="$(yaml_path evidence.output_dir)"

[[ "$BASE_URL" =~ ^https?://[^[:space:]]+$ ]] || { printf 'Invalid endpoint.base_url.\n' >&2; exit 2; }
[[ "$MAX_ERROR_RATE" =~ ^[0-9]+([.][0-9]+)?$ && "$MIN_THROUGHPUT" =~ ^[0-9]+([.][0-9]+)?$ ]] || {
    printf 'Performance rate thresholds must be non-negative decimals.\n' >&2; exit 2;
}
((CONCURRENCY <= REQUESTS)) || { printf 'workload.concurrency cannot exceed requests_per_scenario.\n' >&2; exit 2; }
for command in curl jq awk sort date nproc podman seq xargs uname tail; do
    command -v "$command" >/dev/null || { printf 'Missing command: %s\n' "$command" >&2; exit 1; }
done
for container in "$SERVER_CONTAINER" "$MYSQL_CONTAINER" "$REDIS_CONTAINER"; do
    podman container exists "$container" || { printf 'Container does not exist: %s\n' "$container" >&2; exit 1; }
done

RUN_ID="$(date -u +%Y%m%dT%H%M%SZ)"
mkdir -p "$OUTPUT_DIR"
RESULT_DIR="$(mktemp -d)"
trap 'rm -rf -- "$RESULT_DIR"' EXIT

login_response="$(curl --noproxy '*' --fail --silent --show-error --max-time "$TIMEOUT_SECONDS" \
    --header 'Content-Type: application/json' --header "tenant-id: ${TENANT_ID}" \
    --data "$(jq -n --arg username "$USERNAME" --arg password "$PASSWORD" \
      '{username:$username,password:$password,captchaVerification:""}')" \
    "${BASE_URL}/system/auth/login")"
jq -e '.code == 0 and (.data.accessToken | length > 0)' >/dev/null <<<"$login_response" || {
    printf 'CRM login failed: %s\n' "$(jq -r '.msg // "unknown error"' <<<"$login_response")" >&2; exit 1;
}
TOKEN="$(jq -r '.data.accessToken' <<<"$login_response")"

encoded_start="$(jq -rn --arg value "$START_TIME" '$value|@uri')"
encoded_end="$(jq -rn --arg value "$END_TIME" '$value|@uri')"
declare -a SCENARIOS=(
    "customer-page|/crm/customer/page?pageNo=1&pageSize=${PAGE_SIZE}"
    "business-page|/crm/business/page?pageNo=1&pageSize=${PAGE_SIZE}"
    "customer-summary|/crm/statistics-customer/get-customer-summary-by-date?deptId=${DEPARTMENT_ID}&interval=${INTERVAL}&times=${encoded_start}&times=${encoded_end}"
    "contract-rank|/crm/statistics-rank/get-contract-price-rank?deptId=${DEPARTMENT_ID}&times=${encoded_start}&times=${encoded_end}"
    "sales-funnel|/crm/statistics-funnel/get-funnel-summary?deptId=${DEPARTMENT_ID}&interval=${INTERVAL}&times=${encoded_start}&times=${encoded_end}"
)

request_once() {
    local url="$1" destination="$2" body http_metrics http_code seconds millis app_code ok
    body="$(mktemp "${RESULT_DIR}/body.XXXXXX")"
    http_metrics="$(curl --noproxy '*' --silent --show-error --max-time "$TIMEOUT_SECONDS" \
        --output "$body" --write-out '%{http_code} %{time_total}' \
        --header "Authorization: Bearer ${TOKEN}" --header "tenant-id: ${TENANT_ID}" \
        "$url" 2>/dev/null || printf '000 0')"
    read -r http_code seconds <<<"$http_metrics"
    app_code="$(jq -r '.code // "invalid"' "$body" 2>/dev/null || printf 'invalid')"
    ok=0
    [[ "$http_code" == "200" && "$app_code" == "0" ]] && ok=1
    millis="$(awk -v seconds="$seconds" 'BEGIN { printf "%d", (seconds * 1000) + 0.5 }')"
    printf '%s\t%s\t%s\t%s\n' "$millis" "$ok" "$http_code" "$app_code" >>"$destination"
    rm -f -- "$body"
}
export -f request_once
export RESULT_DIR TOKEN TENANT_ID TIMEOUT_SECONDS

percentile() {
    local file="$1" percentile="$2"
    awk -v p="$percentile" '{values[NR]=$1} END { if (!NR) {print 0; exit}; rank=int((NR*p+99)/100); print values[rank] }' "$file"
}

SUMMARY_TSV="${OUTPUT_DIR}/crm-performance-${RUN_ID}.tsv"
REPORT_MD="${OUTPUT_DIR}/crm-performance-${RUN_ID}.md"
printf 'scenario\trequests\terrors\terror_rate_percent\tp50_ms\tp95_ms\tp99_ms\tthroughput_rps\tstatus\n' >"$SUMMARY_TSV"
overall_status=PASS

for scenario_spec in "${SCENARIOS[@]}"; do
    scenario="${scenario_spec%%|*}"
    path="${scenario_spec#*|}"
    url="${BASE_URL}${path}"
    for ((i=0; i<WARMUP_REQUESTS; i++)); do
        warmup_file="${RESULT_DIR}/${scenario}.warmup"
        request_once "$url" "$warmup_file"
    done
    if awk '$2 != 1 {bad=1} END {exit bad}' "$warmup_file"; then :; else
        printf 'Warm-up failed for %s.\n' "$scenario" >&2; exit 1
    fi

    raw_file="${RESULT_DIR}/${scenario}.raw"
    sorted_file="${RESULT_DIR}/${scenario}.sorted"
    : >"$raw_file"
    started_ns="$(date +%s%N)"
    seq "$REQUESTS" | xargs -P "$CONCURRENCY" -I '{}' bash -c 'request_once "$1" "$2"' _ "$url" "$raw_file"
    ended_ns="$(date +%s%N)"
    sort -n -k1,1 "$raw_file" >"$sorted_file"
    errors="$(awk '$2 != 1 {count++} END {print count+0}' "$raw_file")"
    error_rate="$(awk -v errors="$errors" -v total="$REQUESTS" 'BEGIN {printf "%.2f", errors*100/total}')"
    elapsed_seconds="$(awk -v start="$started_ns" -v end="$ended_ns" 'BEGIN {printf "%.6f", (end-start)/1000000000}')"
    throughput="$(awk -v total="$REQUESTS" -v elapsed="$elapsed_seconds" 'BEGIN {printf "%.2f", total/elapsed}')"
    p50="$(percentile "$sorted_file" 50)"
    p95="$(percentile "$sorted_file" 95)"
    p99="$(percentile "$sorted_file" 99)"
    status=PASS
    if ! awk -v actual="$error_rate" -v maximum="$MAX_ERROR_RATE" 'BEGIN {exit !(actual <= maximum)}' || \
       ((p95 > MAX_P95_MS || p99 > MAX_P99_MS)) || \
       ! awk -v actual="$throughput" -v minimum="$MIN_THROUGHPUT" 'BEGIN {exit !(actual >= minimum)}'; then
        status=FAIL
        overall_status=FAIL
    fi
    printf '%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\n' \
        "$scenario" "$REQUESTS" "$errors" "$error_rate" "$p50" "$p95" "$p99" "$throughput" "$status" >>"$SUMMARY_TSV"
done

table_count="$(podman exec "$MYSQL_CONTAINER" mysql "-u${MYSQL_USER}" "-p${MYSQL_PASSWORD}" \
    "--database=${MYSQL_DATABASE}" -Nse 'SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE();')"
crm_table_count="$(podman exec "$MYSQL_CONTAINER" mysql "-u${MYSQL_USER}" "-p${MYSQL_PASSWORD}" \
    "--database=${MYSQL_DATABASE}" -Nse "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name LIKE 'crm\\_%';")"
container_snapshot="$(podman stats --no-stream --format '{{.Name}}|{{.CPU}}|{{.MemUsage}}' \
    "$SERVER_CONTAINER" "$MYSQL_CONTAINER" "$REDIS_CONTAINER")"

{
    printf '# CRM 性能与容量基线（%s）\n\n' "$RUN_ID"
    printf -- '- 结论：`%s`\n' "$overall_status"
    printf -- '- 工作负载：5 个只读场景，每场景预热 %s 次、采样 %s 次、并发 %s\n' "$WARMUP_REQUESTS" "$REQUESTS" "$CONCURRENCY"
    printf -- '- 阈值：错误率 ≤ %s%%，p95 ≤ %s ms，p99 ≤ %s ms，吞吐 ≥ %s req/s\n' \
        "$MAX_ERROR_RATE" "$MAX_P95_MS" "$MAX_P99_MS" "$MIN_THROUGHPUT"
    printf -- '- 主机：`%s`，CPU：%s 核，内存：%s\n' "$(uname -srmo)" "$(nproc)" "$(awk '/MemTotal/ {printf "%.1f GiB", $2/1048576}' /proc/meminfo)"
    printf -- '- 数据库规模：%s 张表，其中 CRM %s 张\n\n' "$table_count" "$crm_table_count"
    printf '## 结果\n\n'
    printf '| 场景 | 请求 | 错误 | 错误率 | p50 | p95 | p99 | 吞吐 | 结论 |\n'
    printf '|---|---:|---:|---:|---:|---:|---:|---:|---|\n'
    tail -n +2 "$SUMMARY_TSV" | awk -F '\t' '{printf "| %s | %s | %s | %s%% | %s ms | %s ms | %s ms | %s req/s | %s |\n",$1,$2,$3,$4,$5,$6,$7,$8,$9}'
    printf '\n## 容器采样\n\n| 容器 | CPU | 内存 |\n|---|---:|---:|\n'
    awk -F '|' '{printf "| %s | %s | %s |\n",$1,$2,$3}' <<<"$container_snapshot"
    printf '\n> 该结果是本地单节点验收基线，不等同于生产容量承诺。原始汇总见 `%s`。\n' "$(basename "$SUMMARY_TSV")"
} >"$REPORT_MD"

printf 'CRM performance baseline: %s\nreport=%s\nsummary=%s\n' "$overall_status" "$REPORT_MD" "$SUMMARY_TSV"
[[ "$overall_status" == "PASS" ]]
