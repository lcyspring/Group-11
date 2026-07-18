#!/usr/bin/env bash

# Stateful 50-concurrency work-order benchmark. Test rows are uniquely prefixed
# and removed on every exit; the only command-line argument is a KDL path.

set -Eeuo pipefail

PODMAN_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/../.." && pwd)"
source "${PODMAN_DIR}/lib/kdl-config.sh"

[[ $# -eq 1 ]] || { printf 'Usage: bash ./tests/acceptance/verify-crm-work-order-performance.sh <config.kdl>\n' >&2; exit 2; }
kdl_config_init "$1"
[[ "$(kdl_require schema_version)" == 1 ]] || exit 2

BASE_URL="$(kdl_require endpoint.base_url)"
TENANT_ID="$(kdl_positive_integer endpoint.tenant_id)"
USERNAME="$(kdl_require account.username)"
PASSWORD="$(kdl_require account.password)"
ORDER_COUNT="$(kdl_positive_integer workload.work_orders)"
CONCURRENCY="$(kdl_positive_integer workload.concurrency)"
TIMEOUT_SECONDS="$(kdl_positive_integer workload.timeout_seconds)"
CUSTOMER_ID="$(kdl_positive_integer workload.customer_id)"
HANDLER_USER_ID="$(kdl_positive_integer workload.handler_user_id)"
MAX_ERROR_RATE="$(kdl_require thresholds.max_error_rate_percent)"
MAX_P95_MS="$(kdl_positive_integer thresholds.max_p95_ms)"
MAX_P99_MS="$(kdl_positive_integer thresholds.max_p99_ms)"
SERVER_CONTAINER="$(kdl_require containers.server)"
MYSQL_CONTAINER="$(kdl_require containers.mysql)"
REDIS_CONTAINER="$(kdl_require containers.redis)"
MYSQL_USER="$(kdl_require mysql.user)"
MYSQL_PASSWORD="$(kdl_require mysql.password)"
MYSQL_DATABASE="$(kdl_require mysql.database)"
OUTPUT_DIR="$(kdl_path evidence.output_dir)"

[[ "$BASE_URL" =~ ^https?://[^[:space:]]+$ ]] || { printf 'Invalid endpoint.base_url.\n' >&2; exit 2; }
[[ "$MAX_ERROR_RATE" =~ ^[0-9]+([.][0-9]+)?$ ]] || { printf 'Invalid error-rate threshold.\n' >&2; exit 2; }
((ORDER_COUNT >= 50 && CONCURRENCY >= 50 && CONCURRENCY <= ORDER_COUNT)) || {
    printf 'The work-order benchmark requires at least 50 orders and concurrency 50 or greater.\n' >&2; exit 2;
}
for command in curl jq awk sort date podman seq xargs; do command -v "$command" >/dev/null || exit 1; done

RUN_ID="$(date -u +%Y%m%dT%H%M%SZ)-$$"
TITLE_PREFIX="wo-perf-${RUN_ID}"
RESULT_DIR="$(mktemp -d)"
mkdir -p "$OUTPUT_DIR"

mysql_exec() {
    podman exec --env "MYSQL_PWD=${MYSQL_PASSWORD}" "$MYSQL_CONTAINER" mysql \
        --user "$MYSQL_USER" --database "$MYSQL_DATABASE" --batch --skip-column-names --execute "$1"
}

cleanup() {
    set +e
    mysql_exec "DELETE n FROM system_notify_message n WHERE n.tenant_id=${TENANT_ID} AND n.template_params LIKE '%${TITLE_PREFIX}%';
      DELETE c FROM crm_work_order_check_in c JOIN crm_work_order w ON w.id=c.work_order_id
        WHERE w.tenant_id=${TENANT_ID} AND w.title LIKE '${TITLE_PREFIX}%';
      DELETE s FROM crm_work_order_sla s JOIN crm_work_order w ON w.id=s.work_order_id
        WHERE w.tenant_id=${TENANT_ID} AND w.title LIKE '${TITLE_PREFIX}%';
      DELETE c FROM crm_work_order_cc c JOIN crm_work_order w ON w.id=c.work_order_id
        WHERE w.tenant_id=${TENANT_ID} AND w.title LIKE '${TITLE_PREFIX}%';
      DELETE r FROM crm_work_order_record r JOIN crm_work_order w ON w.id=r.work_order_id
        WHERE w.tenant_id=${TENANT_ID} AND w.title LIKE '${TITLE_PREFIX}%';
      DELETE FROM crm_work_order WHERE tenant_id=${TENANT_ID} AND title LIKE '${TITLE_PREFIX}%';" >/dev/null 2>&1
    rm -rf -- "$RESULT_DIR"
}
trap cleanup EXIT

[[ "$(mysql_exec "SELECT COUNT(*) FROM crm_customer WHERE tenant_id=${TENANT_ID} AND id=${CUSTOMER_ID} AND deleted=b'0';")" == 1 ]] || {
    printf 'Configured customer is unavailable.\n' >&2; exit 1;
}
[[ "$(mysql_exec "SELECT COUNT(*) FROM system_users WHERE tenant_id=${TENANT_ID} AND id=${HANDLER_USER_ID} AND status=0 AND deleted=b'0';")" == 1 ]] || {
    printf 'Configured handler is unavailable.\n' >&2; exit 1;
}

LOGIN="$(curl --noproxy '*' --fail --silent --show-error --max-time "$TIMEOUT_SECONDS" \
  --header 'Content-Type: application/json' --header "tenant-id: ${TENANT_ID}" \
  --data "$(jq -n --arg username "$USERNAME" --arg password "$PASSWORD" '{username:$username,password:$password,captchaVerification:""}')" \
  "${BASE_URL}/system/auth/login")"
TOKEN="$(jq -er 'select(.code == 0) | .data.accessToken' <<<"$LOGIN")"

request() {
    local stage="$1" index="$2" id="${3:-}" method path body output response metrics http_code seconds app_code ok=0 returned_id=''
    output="${RESULT_DIR}/${stage}-${index}.body"
    case "$stage" in
        create)
            method=POST; path=/crm/work-order/create
            body="$(jq -n --arg title "${TITLE_PREFIX}-${index}" --argjson customer "$CUSTOMER_ID" --argjson handler "$HANDLER_USER_ID" \
              '{title:$title,type:1,priority:2,customerId:$customer,sourceType:0,handlerUserId:$handler,ccUserIds:[],attachmentUrls:[],description:"50 并发工单容量与状态机验收数据，执行结束后自动清理。"}')" ;;
        sla) method=GET; path="/crm/work-order/sla?id=${id}"; body='' ;;
        start) method=PUT; path=/crm/work-order/start; body="$(jq -n --argjson id "$id" '{id:$id,remark:"并发开始处理"}')" ;;
        complete) method=PUT; path=/crm/work-order/complete; body="$(jq -n --argjson id "$id" '{id:$id,solution:"专项并发验收已完成问题定位、处理、复核与客户反馈记录。"}')" ;;
        statistics) method=GET; path='/crm/statistics-work-order/summary?interval=1&times=2026-01-01%2000%3A00%3A00&times=2026-12-31%2023%3A59%3A59'; body='' ;;
        *) return 2 ;;
    esac
    args=(--noproxy '*' --silent --show-error --max-time "$TIMEOUT_SECONDS" --request "$method"
      --output "$output" --write-out '%{http_code} %{time_total}'
      --header "Authorization: Bearer ${TOKEN}" --header "tenant-id: ${TENANT_ID}")
    [[ -z "$body" ]] || args+=(--header 'Content-Type: application/json' --data "$body")
    metrics="$(curl "${args[@]}" "${BASE_URL}${path}" 2>/dev/null || printf '000 0')"
    read -r http_code seconds <<<"$metrics"
    app_code="$(jq -r '.code // "invalid"' "$output" 2>/dev/null || printf invalid)"
    [[ "$http_code" == 200 && "$app_code" == 0 ]] && ok=1
    [[ "$stage" == create && "$ok" == 1 ]] && returned_id="$(jq -r '.data' "$output")"
    awk -v s="$seconds" -v ok="$ok" -v http="$http_code" -v app="$app_code" -v id="$returned_id" \
      'BEGIN {printf "%d\t%s\t%s\t%s\t%s\n",(s*1000)+0.5,ok,http,app,id}' >"${RESULT_DIR}/${stage}-${index}.raw"
}
export -f request
export BASE_URL TOKEN TENANT_ID TIMEOUT_SECONDS CUSTOMER_ID HANDLER_USER_ID TITLE_PREFIX RESULT_DIR

percentile() { awk -v p="$2" '{v[NR]=$1} END {r=int((NR*p+99)/100); print NR?v[r]:0}' "$1"; }
summarize() {
    local stage="$1" raw sorted errors rate p95 p99 status=PASS
    raw="${RESULT_DIR}/${stage}.raw"
    sorted="${RESULT_DIR}/${stage}.sorted"
    cat "${RESULT_DIR}/${stage}-"*.raw >"$raw"; sort -n -k1,1 "$raw" >"$sorted"
    errors="$(awk '$2 != 1 {n++} END {print n+0}' "$raw")"
    rate="$(awk -v e="$errors" -v n="$ORDER_COUNT" 'BEGIN {printf "%.2f",e*100/n}')"
    p95="$(percentile "$sorted" 95)"; p99="$(percentile "$sorted" 99)"
    if ! awk -v a="$rate" -v m="$MAX_ERROR_RATE" 'BEGIN {exit !(a<=m)}' || ((p95>MAX_P95_MS || p99>MAX_P99_MS)); then status=FAIL; fi
    printf '%s\t%s\t%s\t%s\t%s\t%s\n' "$stage" "$ORDER_COUNT" "$errors" "$rate" "$p95" "$p99" >>"$SUMMARY"
    [[ "$status" == PASS ]]
}

SUMMARY="${OUTPUT_DIR}/crm-work-order-performance-${RUN_ID}.tsv"
REPORT="${OUTPUT_DIR}/crm-work-order-performance-${RUN_ID}.md"
printf 'scenario\trequests\terrors\terror_rate_percent\tp95_ms\tp99_ms\n' >"$SUMMARY"

seq "$ORDER_COUNT" | xargs -P "$CONCURRENCY" -I '{}' bash -c 'request create "$1"' _ '{}'
summarize create
mapfile -t IDS < <(cat "${RESULT_DIR}"/create-*.raw | awk '$2==1 && $5~/^[0-9]+$/ {print $5}')
[[ "${#IDS[@]}" == "$ORDER_COUNT" ]] || { printf 'Only %s work orders were created.\n' "${#IDS[@]}" >&2; exit 1; }

for stage in sla start; do
    printf '%s\n' "${IDS[@]}" | nl -ba | xargs -P "$CONCURRENCY" -n 2 bash -c 'request "$1" "$2" "$3"' _ "$stage"
    summarize "$stage"
done
seq "$ORDER_COUNT" | xargs -P "$CONCURRENCY" -I '{}' bash -c 'request statistics "$1"' _ '{}'
summarize statistics
printf '%s\n' "${IDS[@]}" | nl -ba | xargs -P "$CONCURRENCY" -n 2 bash -c 'request complete "$1" "$2"' _
summarize complete

completed="$(mysql_exec "SELECT COUNT(*) FROM crm_work_order WHERE tenant_id=${TENANT_ID} AND title LIKE '${TITLE_PREFIX}%' AND status=30 AND deleted=b'0';")"
[[ "$completed" == "$ORDER_COUNT" ]] || { printf 'Completed row count mismatch: %s\n' "$completed" >&2; exit 1; }
stats="$(podman stats --no-stream --format '{{.Name}}|{{.CPU}}|{{.MemUsage}}' "$SERVER_CONTAINER" "$MYSQL_CONTAINER" "$REDIS_CONTAINER")"
{
    printf '# CRM 工单 50 并发专项（%s）\n\n' "$RUN_ID"
    printf -- '- 结论：`PASS`\n- 数据：%s 个临时工单，并发 %s，完成后清理\n' "$ORDER_COUNT" "$CONCURRENCY"
    printf -- '- 阈值：错误率 ≤ %s%%，p95 ≤ %s ms，p99 ≤ %s ms\n\n' "$MAX_ERROR_RATE" "$MAX_P95_MS" "$MAX_P99_MS"
    printf '| 场景 | 请求 | 错误 | 错误率 | p95 | p99 |\n|---|---:|---:|---:|---:|---:|\n'
    tail -n +2 "$SUMMARY" | awk -F '\t' '{printf "| %s | %s | %s | %s%% | %s ms | %s ms |\n",$1,$2,$3,$4,$5,$6}'
    printf '\n## 容器采样\n\n| 容器 | CPU | 内存 |\n|---|---:|---:|\n'
    awk -F '|' '{printf "| %s | %s | %s |\n",$1,$2,$3}' <<<"$stats"
    printf '\n> 本地单节点结果不等同于生产容量承诺；测试行由退出清理器删除。\n'
} >"$REPORT"

cleanup
trap - EXIT
residual="$(mysql_exec "SELECT COUNT(*) FROM crm_work_order WHERE tenant_id=${TENANT_ID} AND title LIKE '${TITLE_PREFIX}%';")"
[[ "$residual" == 0 ]]
printf 'CRM work-order performance passed. report=%s summary=%s cleanup=0\n' "$REPORT" "$SUMMARY"
