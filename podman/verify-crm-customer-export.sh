#!/usr/bin/env bash

set -Eeuo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
source "${SCRIPT_DIR}/lib/yaml-config.sh"

[[ $# -eq 1 ]] || {
    printf 'Usage: bash ./verify-crm-customer-export.sh <config.yaml>\n' >&2
    exit 2
}
yaml_config_init "$1"
[[ "$(yaml_require schema_version)" == "1" ]] || exit 2

BASE_URL="$(yaml_require endpoint.base_url)"
TENANT_ID="$(yaml_positive_integer endpoint.tenant_id)"
OWNER_USERNAME="$(yaml_require account.owner_username)"
OWNER_PASSWORD="$(yaml_require account.owner_password)"
OTHER_USERNAME="$(yaml_require account.other_username)"
OTHER_PASSWORD="$(yaml_require account.other_password)"
MYSQL_CONTAINER="$(yaml_require mysql.container)"
MYSQL_USER="$(yaml_require mysql.user)"
MYSQL_PASSWORD="$(yaml_require mysql.password)"
MYSQL_DATABASE="$(yaml_require mysql.database)"
REDIS_CONTAINER="$(yaml_require redis.container)"
OWNER_USER_ID="$(yaml_positive_integer test.owner_user_id)"
OWNER_ROLE_ID="$(yaml_positive_integer test.owner_role_id)"
PRIVILEGED_ROLE_ID="$(yaml_positive_integer test.privileged_role_id)"
EXPORT_MENU_ID="$(yaml_positive_integer test.customer_export_menu_id)"
CUSTOMER_ID="$(yaml_positive_integer test.customer_id)"
POLL_ATTEMPTS="$(yaml_positive_integer test.poll_attempts)"
POLL_INTERVAL="$(yaml_positive_integer test.poll_interval_seconds)"

[[ "$BASE_URL" =~ ^https?://[^[:space:]]+$ ]] || exit 2
for command in curl jq podman; do
    command -v "$command" >/dev/null
done

TMP_DIR="$(mktemp -d)"
TASK_IDS=()
PERMISSION_ID=""
ROLE_MENU_ID=""
ROLE_MENU_PREEXISTED=false
PRIVILEGED_ASSIGNMENT_ID=""

mysql_exec() {
    podman exec "$MYSQL_CONTAINER" mysql "-u${MYSQL_USER}" "-p${MYSQL_PASSWORD}" \
        "--database=${MYSQL_DATABASE}" --default-character-set=utf8mb4 -Nse "$1"
}

evict_permission_cache() {
    podman exec "$REDIS_CONTAINER" redis-cli DEL \
        "user_role_ids:${OWNER_USER_ID}" \
        "menu_role_ids:${TENANT_ID}:${EXPORT_MENU_ID}" >/dev/null
}

cleanup() {
    set +e
    if ((${#TASK_IDS[@]} > 0)); then
        ids="$(IFS=,; printf '%s' "${TASK_IDS[*]}")"
        file_urls="$(mysql_exec "SELECT file_url FROM crm_export_task WHERE id IN (${ids})
          AND file_url IS NOT NULL;" 2>/dev/null)"
        if [[ -n "$file_urls" ]]; then
            while IFS= read -r url; do
                [[ -n "$url" ]] && mysql_exec "UPDATE infra_file SET deleted=b'1'
                  WHERE url='${url//\'/\'\'}';" >/dev/null 2>&1
            done <<< "$file_urls"
        fi
        mysql_exec "DELETE FROM crm_export_task WHERE id IN (${ids});" >/dev/null 2>&1
    fi
    [[ -n "$PERMISSION_ID" ]] && mysql_exec \
        "DELETE FROM crm_permission WHERE id=${PERMISSION_ID};" >/dev/null 2>&1
    if [[ "$ROLE_MENU_PREEXISTED" == "false" && -n "$ROLE_MENU_ID" ]]; then
        mysql_exec "DELETE FROM system_role_menu WHERE id=${ROLE_MENU_ID};" >/dev/null 2>&1
    fi
    if [[ -n "$PRIVILEGED_ASSIGNMENT_ID" ]]; then
        mysql_exec "UPDATE system_user_role SET deleted=b'0'
          WHERE id=${PRIVILEGED_ASSIGNMENT_ID};" >/dev/null 2>&1
    fi
    evict_permission_cache >/dev/null 2>&1
    rm -rf "$TMP_DIR"
}
trap cleanup EXIT

login() {
    local username="$1" password="$2" response
    response="$(curl --noproxy '*' --fail --silent --show-error \
        --header 'Content-Type: application/json' --header "tenant-id: ${TENANT_ID}" \
        --data "$(jq -n --arg username "$username" --arg password "$password" \
          '{username:$username,password:$password,captchaVerification:""}')" \
        "${BASE_URL}/system/auth/login")"
    jq -e '.code == 0 and (.data.accessToken | length > 0)' >/dev/null <<< "$response"
    jq -r '.data.accessToken' <<< "$response"
}

api_json() {
    local token="$1" method="$2" path="$3" payload="${4:-}"
    local args=(--noproxy '*' --fail --silent --show-error --request "$method"
        --header "Authorization: Bearer ${token}" --header "tenant-id: ${TENANT_ID}")
    if [[ -n "$payload" ]]; then
        args+=(--header 'Content-Type: application/json' --data "$payload")
    fi
    curl "${args[@]}" "${BASE_URL}${path}"
}

wait_status() {
    local token="$1" id="$2" expected="$3" response status
    for ((attempt=1; attempt<=POLL_ATTEMPTS; attempt++)); do
        response="$(api_json "$token" GET "/crm/export-task/get?id=${id}")"
        jq -e '.code == 0' >/dev/null <<< "$response"
        status="$(jq -r '.data.status' <<< "$response")"
        [[ "$status" == "$expected" ]] && return 0
        sleep "$POLL_INTERVAL"
    done
    printf 'Task %s did not reach status %s; last status was %s.\n' "$id" "$expected" "$status" >&2
    return 1
}

PRIVILEGED_ASSIGNMENT_ID="$(mysql_exec "SELECT id FROM system_user_role WHERE tenant_id=${TENANT_ID}
  AND user_id=${OWNER_USER_ID} AND role_id=${PRIVILEGED_ROLE_ID} AND deleted=b'0' LIMIT 1;")"
[[ -n "$PRIVILEGED_ASSIGNMENT_ID" ]] || {
    printf 'Configured owner account must have the privileged role before the isolated test.\n' >&2
    exit 2
}
mysql_exec "UPDATE system_user_role SET deleted=b'1' WHERE id=${PRIVILEGED_ASSIGNMENT_ID};"

existing_role_menu="$(mysql_exec "SELECT id FROM system_role_menu WHERE tenant_id=${TENANT_ID}
  AND role_id=${OWNER_ROLE_ID} AND menu_id=${EXPORT_MENU_ID} AND deleted=b'0' LIMIT 1;")"
if [[ -n "$existing_role_menu" ]]; then
    ROLE_MENU_PREEXISTED=true
    ROLE_MENU_ID="$existing_role_menu"
else
    mysql_exec "INSERT INTO system_role_menu(role_id,menu_id,creator,tenant_id)
      VALUES (${OWNER_ROLE_ID},${EXPORT_MENU_ID},'crm-export-uat',${TENANT_ID});"
    ROLE_MENU_ID="$(mysql_exec "SELECT id FROM system_role_menu WHERE tenant_id=${TENANT_ID}
      AND role_id=${OWNER_ROLE_ID} AND menu_id=${EXPORT_MENU_ID} AND creator='crm-export-uat'
      ORDER BY id DESC LIMIT 1;")"
fi

mysql_exec "INSERT INTO crm_permission(biz_type,biz_id,user_id,level,creator,tenant_id)
  VALUES (2,${CUSTOMER_ID},${OWNER_USER_ID},3,'crm-export-uat',${TENANT_ID});"
PERMISSION_ID="$(mysql_exec "SELECT id FROM crm_permission WHERE tenant_id=${TENANT_ID}
  AND biz_type=2 AND biz_id=${CUSTOMER_ID} AND user_id=${OWNER_USER_ID}
  AND creator='crm-export-uat' ORDER BY id DESC LIMIT 1;")"
evict_permission_cache

OWNER_TOKEN="$(login "$OWNER_USERNAME" "$OWNER_PASSWORD")"
OTHER_TOKEN="$(login "$OTHER_USERNAME" "$OTHER_PASSWORD")"

customer_name="$(mysql_exec "SELECT name FROM crm_customer WHERE tenant_id=${TENANT_ID}
  AND id=${CUSTOMER_ID} AND deleted=b'0';")"
[[ -n "$customer_name" ]]
create_payload="$(jq -n --arg name "$customer_name" '{name:$name,sceneType:2,pageNo:1,pageSize:10}')"
create_response="$(api_json "$OWNER_TOKEN" POST /crm/export-task/customer "$create_payload")"
jq -e '.code == 0 and (.data | type == "number")' >/dev/null <<< "$create_response"
SUCCESS_TASK_ID="$(jq -r '.data' <<< "$create_response")"
TASK_IDS+=("$SUCCESS_TASK_ID")
wait_status "$OWNER_TOKEN" "$SUCCESS_TASK_ID" 30
task_response="$(api_json "$OWNER_TOKEN" GET "/crm/export-task/get?id=${SUCCESS_TASK_ID}")"
jq -e '.code == 0 and .data.totalCount == 1' >/dev/null <<< "$task_response"

cross_user_response="$(api_json "$OTHER_TOKEN" GET "/crm/export-task/get?id=${SUCCESS_TASK_ID}")"
jq -e '.code == 1020024003' >/dev/null <<< "$cross_user_response"

token_response="$(api_json "$OWNER_TOKEN" POST /crm/export-task/download-token \
  "$(jq -n --argjson id "$SUCCESS_TASK_ID" '{id:$id}')")"
jq -e '.code == 0 and (.data.token | length == 48)' >/dev/null <<< "$token_response"
DOWNLOAD_TOKEN="$(jq -r '.data.token' <<< "$token_response")"
curl --noproxy '*' --fail --silent --show-error \
    --header "Authorization: Bearer ${OWNER_TOKEN}" --header "tenant-id: ${TENANT_ID}" \
    "${BASE_URL}/crm/export-task/download?id=${SUCCESS_TASK_ID}&token=${DOWNLOAD_TOKEN}" \
    --output "${TMP_DIR}/customer.xlsx"
[[ -s "${TMP_DIR}/customer.xlsx" ]]

replay_response="$(curl --noproxy '*' --fail --silent --show-error \
    --header "Authorization: Bearer ${OWNER_TOKEN}" --header "tenant-id: ${TENANT_ID}" \
    "${BASE_URL}/crm/export-task/download?id=${SUCCESS_TASK_ID}&token=${DOWNLOAD_TOKEN}")"
jq -e '.code == 1020024006' >/dev/null <<< "$replay_response"

second_token_response="$(api_json "$OWNER_TOKEN" POST /crm/export-task/download-token \
  "$(jq -n --argjson id "$SUCCESS_TASK_ID" '{id:$id}')")"
SECOND_TOKEN="$(jq -r '.data.token' <<< "$second_token_response")"
mysql_exec "DELETE FROM crm_permission WHERE id=${PERMISSION_ID};"
permission_response="$(curl --noproxy '*' --fail --silent --show-error \
    --header "Authorization: Bearer ${OWNER_TOKEN}" --header "tenant-id: ${TENANT_ID}" \
    "${BASE_URL}/crm/export-task/download?id=${SUCCESS_TASK_ID}&token=${SECOND_TOKEN}")"
jq -e '.code == 1020007010 or .code == 1020024007' >/dev/null <<< "$permission_response"
mysql_exec "INSERT INTO crm_permission(id,biz_type,biz_id,user_id,level,creator,tenant_id)
  VALUES (${PERMISSION_ID},2,${CUSTOMER_ID},${OWNER_USER_ID},3,'crm-export-uat',${TENANT_ID});"

file_url="$(mysql_exec "SELECT file_url FROM crm_export_task WHERE id=${SUCCESS_TASK_ID};")"
[[ -n "$file_url" ]]
mysql_exec "UPDATE crm_export_task SET expires_at=DATE_SUB(NOW(),INTERVAL 1 SECOND)
  WHERE id=${SUCCESS_TASK_ID};"
expired_response="$(api_json "$OWNER_TOKEN" POST /crm/export-task/download-token \
  "$(jq -n --argjson id "$SUCCESS_TASK_ID" '{id:$id}')")"
jq -e '.code == 1020024005' >/dev/null <<< "$expired_response"
expired_state="$(mysql_exec "SELECT CONCAT(status,':',IFNULL(file_url,'NULL'))
  FROM crm_export_task WHERE id=${SUCCESS_TASK_ID};")"
[[ "$expired_state" == "50:NULL" ]]
active_file_count="$(mysql_exec "SELECT COUNT(*) FROM infra_file
  WHERE url='${file_url//\'/\'\'}' AND deleted=b'0';")"
[[ "$active_file_count" == "0" ]]

printf 'task-success=%s cross-user=denied first-download=ok replay=denied\n' "$SUCCESS_TASK_ID"
printf 'permission-change=denied expired-state=%s protected-file-active=%s\n' \
    "$expired_state" "$active_file_count"
printf 'acceptance-cleanup=scheduled\n'
