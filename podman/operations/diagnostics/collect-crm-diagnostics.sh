#!/usr/bin/env bash
# Collect a sanitized-indexed local CRM diagnostic bundle. The only CLI argument is a KDL path.

set -Eeuo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
PODMAN_DIR="$(cd -- "${SCRIPT_DIR}/../.." && pwd)"
source "${PODMAN_DIR}/lib/kdl-config.sh"

[[ $# -eq 1 ]] || { printf 'Usage: bash ./operations/diagnostics/collect-crm-diagnostics.sh <config.kdl>\n' >&2; exit 2; }
kdl_config_init "$1"
[[ "$(kdl_require schema_version)" == "1" ]] || { printf 'Unsupported schema_version.\n' >&2; exit 2; }

POD_NAME="$(kdl_require deployment.pod_name)"
SERVER_URL="$(kdl_require endpoint.server_health_url)"
WEB_URL="$(kdl_require endpoint.web_url)"
MALL_URL="$(kdl_require endpoint.mall_url)"
TIMEOUT_SECONDS="$(kdl_positive_integer endpoint.timeout_seconds)"
EXPECTED_HEALTH="$(kdl_require endpoint.expected_health_status)"
LOG_SINCE="$(kdl_require collection.log_since)"
OUTPUT_DIR="$(kdl_path collection.output_dir)"
MYSQL_USER="$(kdl_require mysql.user)"
MYSQL_PASSWORD="$(kdl_require mysql.password)"
MYSQL_DATABASE="$(kdl_require mysql.database)"
MIN_DISK_FREE="$(kdl_positive_integer thresholds.min_disk_free_percent)"
MAX_MEMORY_USED="$(kdl_positive_integer thresholds.max_host_memory_used_percent)"
MAX_CONNECTION_USED="$(kdl_positive_integer thresholds.max_mysql_connection_used_percent)"
MAX_ERROR_LINES="$(kdl_positive_integer thresholds.max_recent_error_lines)"
MAX_RESTARTS="$(kdl_require thresholds.max_container_restarts)"

declare -a CONTAINERS=(
    "$(kdl_require containers.mysql)"
    "$(kdl_require containers.redis)"
    "$(kdl_require containers.rabbitmq)"
    "$(kdl_require containers.tdengine)"
    "$(kdl_require containers.server)"
    "$(kdl_require containers.web)"
    "$(kdl_require containers.mall)"
)
MYSQL_CONTAINER="${CONTAINERS[0]}"

[[ "$POD_NAME" =~ ^[a-zA-Z0-9_.-]+$ && "$MYSQL_DATABASE" =~ ^[a-zA-Z0-9_]+$ ]] || {
    printf 'Invalid Pod or database identifier.\n' >&2; exit 2;
}
[[ "$LOG_SINCE" =~ ^[1-9][0-9]*[smhd]$ && "$MAX_RESTARTS" =~ ^[0-9]+$ ]] || {
    printf 'Invalid collection.log_since or max_container_restarts.\n' >&2; exit 2;
}
((MIN_DISK_FREE <= 100 && MAX_MEMORY_USED <= 100 && MAX_CONNECTION_USED <= 100)) || {
    printf 'Percentage thresholds must not exceed 100.\n' >&2; exit 2;
}
for url in "$SERVER_URL" "$WEB_URL" "$MALL_URL"; do
    [[ "$url" =~ ^https?://[^[:space:]]+$ ]] || { printf 'Invalid endpoint URL: %s\n' "$url" >&2; exit 2; }
done
for command in curl jq awk rg sed date df podman tar; do
    command -v "$command" >/dev/null || { printf 'Missing command: %s\n' "$command" >&2; exit 1; }
done

RUN_ID="$(date -u +%Y%m%dT%H%M%SZ)"
RUN_DIR="${OUTPUT_DIR}/crm-diagnostics-${RUN_ID}"
ARCHIVE="${OUTPUT_DIR}/crm-diagnostics-${RUN_ID}.tar.gz"
mkdir -p "$RUN_DIR"

podman pod exists "$POD_NAME" || { printf 'Pod does not exist: %s\n' "$POD_NAME" >&2; exit 1; }
for container in "${CONTAINERS[@]}"; do
    podman container exists "$container" || { printf 'Container does not exist: %s\n' "$container" >&2; exit 1; }
done

podman ps --pod --filter "pod=${POD_NAME}" \
    --format 'table {{.PodName}}\t{{.Names}}\t{{.Status}}\t{{.Image}}' >"${RUN_DIR}/containers.txt"
podman stats --no-stream --format 'table {{.Name}}\t{{.CPU}}\t{{.MemUsage}}\t{{.NetIO}}\t{{.BlockIO}}' \
    "${CONTAINERS[@]}" >"${RUN_DIR}/container-stats.txt"

printf 'container\tstatus\trestarts\n' >"${RUN_DIR}/container-state.tsv"
running_count=0
restart_max=0
for container in "${CONTAINERS[@]}"; do
    state="$(podman inspect --format '{{.State.Status}}' "$container")"
    restarts="$(podman inspect --format '{{.RestartCount}}' "$container")"
    printf '%s\t%s\t%s\n' "$container" "$state" "$restarts" >>"${RUN_DIR}/container-state.tsv"
    [[ "$state" == "running" ]] && ((running_count+=1))
    ((restarts > restart_max)) && restart_max="$restarts"
    podman logs --since "$LOG_SINCE" "$container" >"${RUN_DIR}/${container}.log" 2>&1 || true
done

server_health="$(curl --noproxy '*' --silent --show-error --max-time "$TIMEOUT_SECONDS" "$SERVER_URL")"
printf '%s\n' "$server_health" | jq . >"${RUN_DIR}/server-health.json"
health_status="$(jq -r '.status // "INVALID"' <<<"$server_health")"
web_status="$(curl --noproxy '*' --silent --show-error --output /dev/null --write-out '%{http_code}' \
    --max-time "$TIMEOUT_SECONDS" "$WEB_URL" || printf '000')"
mall_status="$(curl --noproxy '*' --silent --show-error --output /dev/null --write-out '%{http_code}' \
    --max-time "$TIMEOUT_SECONDS" "$MALL_URL" || printf '000')"

error_lines=0
for log_file in "$RUN_DIR"/*.log; do
    count="$(sed -r 's/\x1B\[[0-9;]*[mK]//g' "$log_file" | \
        rg -c '(^|[[:space:]])(ERROR|FATAL)([[:space:]]|$)' || true)"
    error_lines=$((error_lines + count))
done

podman exec "$MYSQL_CONTAINER" mysql "-u${MYSQL_USER}" "-p${MYSQL_PASSWORD}" \
    "--database=${MYSQL_DATABASE}" -Nse \
    "SELECT VARIABLE_NAME, VARIABLE_VALUE FROM performance_schema.global_status
       WHERE VARIABLE_NAME IN ('Threads_connected','Threads_running','Aborted_connects');
     SELECT 'max_connections', @@max_connections;
     SELECT 'all_tables', COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE();
     SELECT 'crm_tables', COUNT(*) FROM information_schema.tables
       WHERE table_schema=DATABASE() AND table_name LIKE 'crm\\_%';
     SELECT 'database_mib', ROUND(COALESCE(SUM(data_length+index_length),0)/1024/1024,2)
       FROM information_schema.tables WHERE table_schema=DATABASE();" >"${RUN_DIR}/mysql-metrics.tsv"
threads_connected="$(awk '$1=="Threads_connected" {print $2}' "${RUN_DIR}/mysql-metrics.tsv")"
max_connections="$(awk '$1=="max_connections" {print $2}' "${RUN_DIR}/mysql-metrics.tsv")"
connection_used="$(awk -v used="$threads_connected" -v max="$max_connections" 'BEGIN {printf "%.2f", used*100/max}')"

df -P "${PODMAN_DIR}/.." >"${RUN_DIR}/disk.txt"
disk_free="$(df -P "${PODMAN_DIR}/.." | awk 'NR==2 {gsub(/%/,"",$5); print 100-$5}')"
awk '/MemTotal/ {total=$2} /MemAvailable/ {available=$2} END {
  printf "MemTotalKiB\t%d\nMemAvailableKiB\t%d\nMemoryUsedPercent\t%.2f\n", total, available, (total-available)*100/total
}' /proc/meminfo >"${RUN_DIR}/memory.tsv"
memory_used="$(awk '$1=="MemoryUsedPercent" {print $2}' "${RUN_DIR}/memory.tsv")"
{
    uname -a
    printf 'cpu_count\t%s\n' "$(nproc)"
    printf 'collected_at_utc\t%s\n' "$RUN_ID"
} >"${RUN_DIR}/host.txt"

overall=PASS
declare -a failures=()
[[ "$health_status" == "$EXPECTED_HEALTH" ]] || { overall=FAIL; failures+=("Server health=${health_status}"); }
[[ "$web_status" == "200" ]] || { overall=FAIL; failures+=("Web HTTP=${web_status}"); }
[[ "$mall_status" == "200" ]] || { overall=FAIL; failures+=("Mall HTTP=${mall_status}"); }
[[ "$running_count" == "${#CONTAINERS[@]}" ]] || { overall=FAIL; failures+=("running=${running_count}/${#CONTAINERS[@]}"); }
((restart_max <= MAX_RESTARTS)) || { overall=FAIL; failures+=("restart_max=${restart_max}"); }
((error_lines <= MAX_ERROR_LINES)) || { overall=FAIL; failures+=("recent_error_lines=${error_lines}"); }
((disk_free >= MIN_DISK_FREE)) || { overall=FAIL; failures+=("disk_free=${disk_free}%"); }
awk -v actual="$memory_used" -v maximum="$MAX_MEMORY_USED" 'BEGIN {exit !(actual <= maximum)}' || {
    overall=FAIL; failures+=("memory_used=${memory_used}%");
}
awk -v actual="$connection_used" -v maximum="$MAX_CONNECTION_USED" 'BEGIN {exit !(actual <= maximum)}' || {
    overall=FAIL; failures+=("mysql_connections=${connection_used}%");
}

{
    printf '# CRM 可观测诊断摘要（%s）\n\n' "$RUN_ID"
    printf -- '- 结论：`%s`\n' "$overall"
    printf -- '- Server health：`%s`；Web/Mall HTTP：`%s/%s`\n' "$health_status" "$web_status" "$mall_status"
    printf -- '- 运行容器：%s/%s；最大重启次数：%s\n' "$running_count" "${#CONTAINERS[@]}" "$restart_max"
    printf -- '- 最近 %s 的 ERROR/FATAL：%s 行\n' "$LOG_SINCE" "$error_lines"
    printf -- '- 主机磁盘可用：%s%%；内存使用：%s%%\n' "$disk_free" "$memory_used"
    printf -- '- MySQL 连接：%s/%s（%s%%）\n\n' "$threads_connected" "$max_connections" "$connection_used"
    printf '## SLI 阈值\n\n'
    printf '| SLI | 当前值 | 阈值 |\n|---|---:|---:|\n'
    printf '| 服务健康 | %s | %s |\n' "$health_status" "$EXPECTED_HEALTH"
    printf '| 容器运行 | %s/%s | %s/%s |\n' "$running_count" "${#CONTAINERS[@]}" "${#CONTAINERS[@]}" "${#CONTAINERS[@]}"
    printf '| 最大重启次数 | %s | ≤ %s |\n' "$restart_max" "$MAX_RESTARTS"
    printf '| ERROR/FATAL 行 | %s | ≤ %s |\n' "$error_lines" "$MAX_ERROR_LINES"
    printf '| 磁盘可用 | %s%% | ≥ %s%% |\n' "$disk_free" "$MIN_DISK_FREE"
    printf '| 主机内存使用 | %s%% | ≤ %s%% |\n' "$memory_used" "$MAX_MEMORY_USED"
    printf '| MySQL 连接使用 | %s%% | ≤ %s%% |\n' "$connection_used" "$MAX_CONNECTION_USED"
    if ((${#failures[@]})); then
        printf '\n## 失败项\n\n'
        printf -- '- %s\n' "${failures[@]}"
    fi
    printf '\n> 原始日志和指标只保存在本机 ignored 诊断包，交接前需按数据安全要求复核。\n'
} >"${RUN_DIR}/SUMMARY.md"

tar -C "$OUTPUT_DIR" -czf "$ARCHIVE" "$(basename "$RUN_DIR")"
printf 'CRM diagnostics: %s\nsummary=%s\narchive=%s\n' "$overall" "${RUN_DIR}/SUMMARY.md" "$ARCHIVE"
[[ "$overall" == "PASS" ]]
