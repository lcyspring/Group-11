#!/usr/bin/env bash
# Real API and MySQL reconciliation for CRM statistics authorization. The only CLI argument is a YAML path.

set -Eeuo pipefail

PODMAN_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/../.." && pwd)"
source "${PODMAN_DIR}/lib/yaml-config.sh"

[[ $# -eq 1 ]] || { printf 'Usage: bash ./tests/acceptance/verify-crm-statistics-runtime.sh <config.yaml>\n' >&2; exit 2; }
yaml_config_init "$1"
[[ "$(yaml_require schema_version)" == "1" ]] || { printf 'Unsupported schema_version.\n' >&2; exit 2; }

BASE_URL="$(yaml_require endpoint.base_url)"
TENANT_ID="$(yaml_positive_integer endpoint.tenant_id)"
NEGATIVE_TENANT_ID="$(yaml_positive_integer endpoint.negative_tenant_id)"
TEMPLATE_USERNAME="$(yaml_require account.template_username)"
TEMPLATE_PASSWORD="$(yaml_require account.template_password)"
MYSQL_CONTAINER="$(yaml_require mysql.container)"
MYSQL_USER="$(yaml_require mysql.user)"
MYSQL_PASSWORD="$(yaml_require mysql.password)"
MYSQL_DATABASE="$(yaml_require mysql.database)"
DEPARTMENT_ID="$(yaml_positive_integer acceptance.department_id)"
PEER_USER_ID="$(yaml_positive_integer acceptance.peer_user_id)"
CUSTOMER_PERMISSION="$(yaml_require acceptance.customer_permission)"
DENIED_SCOPE="$(yaml_require acceptance.denied_scope)"
START_TIME="$(yaml_require acceptance.start_time)"
END_TIME="$(yaml_require acceptance.end_time)"

[[ "$BASE_URL" =~ ^https?://[^[:space:]]+$ ]] || { printf 'Invalid endpoint.base_url.\n' >&2; exit 2; }
[[ "$MYSQL_CONTAINER" =~ ^[a-zA-Z0-9_.-]+$ && "$MYSQL_USER" =~ ^[a-zA-Z0-9_.-]+$ \
    && "$MYSQL_DATABASE" =~ ^[a-zA-Z0-9_]+$ ]] || { printf 'Invalid MySQL identifier.\n' >&2; exit 2; }
[[ "$CUSTOMER_PERMISSION" =~ ^[a-z0-9:-]+$ && "$DENIED_SCOPE" =~ ^[a-z]+$ ]] || {
    printf 'Invalid permission or denied scope.\n' >&2; exit 2;
}
for command in curl jq podman date; do
    command -v "$command" >/dev/null || { printf 'Missing command: %s\n' "$command" >&2; exit 1; }
done

RUN_ID="$(date +%s)"
USERNAME="crmstat${RUN_ID}"
ROLE_CODE="crm_stat_self_${RUN_ID}"
CUSTOMER_NAME="CRM-STAT-RUNTIME-${RUN_ID}"
USER_ID=''
ROLE_ID=''
CUSTOMER_ID=''
ADMIN_TOKEN=''

mysql_exec() {
    podman exec "$MYSQL_CONTAINER" mysql "-u${MYSQL_USER}" "-p${MYSQL_PASSWORD}" \
        "--database=${MYSQL_DATABASE}" --default-character-set=utf8mb4 -Nse "$1"
}

cleanup() {
    set +e
    if [[ -n "$CUSTOMER_ID" ]]; then
        mysql_exec "DELETE FROM crm_permission WHERE tenant_id=${TENANT_ID} AND biz_type=2 AND biz_id=${CUSTOMER_ID};
          DELETE FROM crm_customer_owner_record WHERE tenant_id=${TENANT_ID} AND customer_id=${CUSTOMER_ID};
          DELETE FROM crm_customer_lifecycle_record WHERE tenant_id=${TENANT_ID} AND customer_id=${CUSTOMER_ID};
          DELETE FROM crm_customer WHERE tenant_id=${TENANT_ID} AND id=${CUSTOMER_ID};" >/dev/null
    fi
    if [[ -n "$USER_ID" ]]; then
        if [[ -n "$ADMIN_TOKEN" ]]; then
            curl --noproxy '*' --silent --request DELETE \
                --header "Authorization: Bearer ${ADMIN_TOKEN}" --header "tenant-id: ${TENANT_ID}" \
                --get --data-urlencode "id=${USER_ID}" "${BASE_URL}/system/user/delete" >/dev/null
        fi
        mysql_exec "DELETE FROM system_oauth2_access_token WHERE tenant_id=${TENANT_ID} AND user_id=${USER_ID};
          DELETE FROM system_oauth2_refresh_token WHERE tenant_id=${TENANT_ID} AND user_id=${USER_ID};
          DELETE FROM system_login_log WHERE tenant_id=${TENANT_ID} AND user_id=${USER_ID};
          DELETE FROM system_user_role WHERE tenant_id=${TENANT_ID} AND user_id=${USER_ID};
          DELETE FROM system_users WHERE tenant_id=${TENANT_ID} AND id=${USER_ID};" >/dev/null
    fi
    if [[ -n "$ROLE_ID" ]]; then
        if [[ -n "$ADMIN_TOKEN" ]]; then
            curl --noproxy '*' --silent --request DELETE \
                --header "Authorization: Bearer ${ADMIN_TOKEN}" --header "tenant-id: ${TENANT_ID}" \
                --get --data-urlencode "id=${ROLE_ID}" "${BASE_URL}/system/role/delete" >/dev/null
        fi
        mysql_exec "DELETE FROM system_role_menu WHERE tenant_id=${TENANT_ID} AND role_id=${ROLE_ID};
          DELETE FROM system_role WHERE tenant_id=${TENANT_ID} AND id=${ROLE_ID};" >/dev/null
    fi
}
trap cleanup EXIT

login() {
    local username="$1" password="$2" tenant="$3" response
    response="$(curl --noproxy '*' --fail --silent --show-error --header 'Content-Type: application/json' \
        --header "tenant-id: ${tenant}" \
        --data "$(jq -n --arg username "$username" --arg password "$password" \
          '{username:$username,password:$password,captchaVerification:""}')" \
        "${BASE_URL}/system/auth/login")"
    jq -e '.code == 0 and (.data.accessToken | length > 0)' >/dev/null <<< "$response" || {
        printf 'Login failed for %s: %s\n' "$username" "$(jq -r '.msg' <<< "$response")" >&2
        return 1
    }
    jq -r '.data.accessToken' <<< "$response"
}

api_raw() {
    local token="$1" tenant="$2" path="$3"
    shift 3
    local args=(--noproxy '*' --silent --show-error --get
        --header "Authorization: Bearer ${token}" --header "tenant-id: ${tenant}")
    while [[ $# -gt 0 ]]; do args+=(--data-urlencode "$1"); shift; done
    curl "${args[@]}" "${BASE_URL}${path}"
}

api_json() {
    local method="$1" path="$2" body="$3" response
    response="$(curl --noproxy '*' --fail --silent --show-error --request "$method" \
        --header "Authorization: Bearer ${ADMIN_TOKEN}" --header "tenant-id: ${TENANT_ID}" \
        --header 'Content-Type: application/json' --data "$body" "${BASE_URL}${path}")"
    jq -e '.code == 0' >/dev/null <<< "$response" || {
        printf 'Admin API %s %s failed: %s\n' "$method" "$path" "$(jq -r '.msg' <<< "$response")" >&2
        return 1
    }
    printf '%s' "$response"
}

ADMIN_TOKEN="$(login "$TEMPLATE_USERNAME" "$TEMPLATE_PASSWORD" "$TENANT_ID")"
MENU_ID="$(mysql_exec "SELECT id FROM system_menu WHERE permission='${CUSTOMER_PERMISSION}'
  AND deleted=b'0' LIMIT 1;")"
[[ "$MENU_ID" =~ ^[0-9]+$ ]] || { printf 'Statistics permission menu was not found.\n' >&2; exit 1; }

ROLE_ID="$(api_json POST '/system/role/create' "$(jq -n --arg code "$ROLE_CODE" \
  '{name:"CRM 统计 SELF 验收",code:$code,sort:999,status:0,remark:"temporary runtime acceptance"}')" | jq -r '.data')"
api_json POST '/system/permission/assign-role-data-scope' \
  "$(jq -n --argjson role "$ROLE_ID" '{roleId:$role,dataScope:5,dataScopeDeptIds:[]}')" >/dev/null
api_json POST '/system/permission/assign-role-menu' \
  "$(jq -n --argjson role "$ROLE_ID" --argjson menu "$MENU_ID" '{roleId:$role,menuIds:[$menu]}')" >/dev/null
USER_ID="$(api_json POST '/system/user/create' "$(jq -n --arg username "$USERNAME" \
  --arg password "$TEMPLATE_PASSWORD" --argjson dept "$DEPARTMENT_ID" \
  '{username:$username,password:$password,nickname:"CRM 统计 SELF 验收",deptId:$dept,status:0,sex:0,
    remark:"temporary runtime acceptance"}')" | jq -r '.data')"
[[ "$ROLE_ID" =~ ^[0-9]+$ && "$USER_ID" =~ ^[0-9]+$ ]] || { printf 'Fixture creation failed.\n' >&2; exit 1; }
api_json POST '/system/permission/assign-user-role' \
  "$(jq -n --argjson user "$USER_ID" --argjson role "$ROLE_ID" '{userId:$user,roleIds:[$role]}')" >/dev/null

CUSTOMER_RESPONSE="$(curl --noproxy '*' --fail --silent --show-error --request POST \
  --header "Authorization: Bearer ${ADMIN_TOKEN}" --header "tenant-id: ${TENANT_ID}" \
  --header 'Content-Type: application/json' \
  --data "$(jq -n --arg name "$CUSTOMER_NAME" --argjson owner "$USER_ID" \
    '{name:$name,ownerUserId:$owner,duplicateCheckConfirmed:true,remark:"statistics runtime reconciliation"}')" \
  "${BASE_URL}/crm/customer/create")"
jq -e '.code == 0 and (.data | type == "number")' >/dev/null <<< "$CUSTOMER_RESPONSE" || {
    printf 'Acceptance customer creation failed: %s\n' "$(jq -r '.msg' <<< "$CUSTOMER_RESPONSE")" >&2
    exit 1
}
CUSTOMER_ID="$(jq -r '.data' <<< "$CUSTOMER_RESPONSE")"

TOKEN="$(login "$USERNAME" "$TEMPLATE_PASSWORD" "$TENANT_ID")"
OWN="$(api_raw "$TOKEN" "$TENANT_ID" '/crm/statistics-customer/get-customer-summary-by-user' \
  "deptId=${DEPARTMENT_ID}" "userId=${USER_ID}" 'interval=1' "times=${START_TIME}" "times=${END_TIME}")"
jq -e --argjson user "$USER_ID" '.code == 0 and (.data | length == 1)
  and .data[0].ownerUserId == $user and .data[0].customerCreateCount == 1' >/dev/null <<< "$OWN" || {
    printf 'SELF statistics reconciliation failed: %s\n' "$OWN" >&2
    mysql_exec "SELECT CONCAT('user=',u.id,',dept=',u.dept_id,',role=',r.id,',scope=',r.data_scope)
      FROM system_users u JOIN system_user_role ur ON ur.user_id=u.id AND ur.deleted=b'0'
      JOIN system_role r ON r.id=ur.role_id AND r.deleted=b'0'
      WHERE u.id=${USER_ID} AND u.tenant_id=${TENANT_ID};" >&2
    exit 1
}
DB_COUNT="$(mysql_exec "SELECT COUNT(*) FROM crm_customer WHERE tenant_id=${TENANT_ID}
  AND owner_user_id=${USER_ID} AND deleted=b'0' AND create_time BETWEEN '${START_TIME}' AND '${END_TIME}';")"
[[ "$DB_COUNT" == "1" ]] || { printf 'MySQL reconciliation expected 1, got %s.\n' "$DB_COUNT" >&2; exit 1; }

PEER="$(api_raw "$TOKEN" "$TENANT_ID" '/crm/statistics-customer/get-customer-summary-by-user' \
  "deptId=${DEPARTMENT_ID}" "userId=${PEER_USER_ID}" 'interval=1' "times=${START_TIME}" "times=${END_TIME}")"
jq -e '.code == 1020014006' >/dev/null <<< "$PEER" || { printf 'Peer user scope was not denied: %s\n' "$PEER" >&2; exit 1; }
DEPARTMENT="$(api_raw "$TOKEN" "$TENANT_ID" '/crm/statistics-customer/get-customer-summary-by-user' \
  "deptId=${DEPARTMENT_ID}" 'interval=1' "times=${START_TIME}" "times=${END_TIME}")"
jq -e '.code == 1020014006' >/dev/null <<< "$DEPARTMENT" || { printf 'SELF department aggregation was not denied: %s\n' "$DEPARTMENT" >&2; exit 1; }

CUSTOMER_CATALOG="$(api_raw "$TOKEN" "$TENANT_ID" '/crm/statistics-metadata/catalog' 'scope=customer')"
jq -e '.code == 0 and .data.scope == "customer"' >/dev/null <<< "$CUSTOMER_CATALOG" || {
    printf 'Granted statistics catalog failed: %s\n' "$CUSTOMER_CATALOG" >&2; exit 1;
}
DENIED_CATALOG="$(api_raw "$TOKEN" "$TENANT_ID" '/crm/statistics-metadata/catalog' "scope=${DENIED_SCOPE}")"
jq -e '.code == 403' >/dev/null <<< "$DENIED_CATALOG" || {
    printf 'Cross-domain statistics permission was not denied: %s\n' "$DENIED_CATALOG" >&2; exit 1;
}
CROSS_TENANT="$(api_raw "$TOKEN" "$NEGATIVE_TENANT_ID" '/crm/statistics-metadata/catalog' 'scope=customer')"
jq -e '.code == 403' >/dev/null <<< "$CROSS_TENANT" || {
    printf 'Cross-tenant request was not denied: %s\n' "$CROSS_TENANT" >&2; exit 1;
}
NO_TOKEN="$(curl --noproxy '*' --silent --show-error --get --header "tenant-id: ${TENANT_ID}" \
  --data-urlencode 'scope=customer' "${BASE_URL}/crm/statistics-metadata/catalog")"
jq -e '.code == 401' >/dev/null <<< "$NO_TOKEN" || { printf 'Anonymous request was not denied: %s\n' "$NO_TOKEN" >&2; exit 1; }

ACCEPTED_USER_ID="$USER_ID"
ACCEPTED_ROLE_ID="$ROLE_ID"
ACCEPTED_CUSTOMER_ID="$CUSTOMER_ID"
cleanup
USER_ID=''
ROLE_ID=''
CUSTOMER_ID=''
RESIDUAL="$(mysql_exec "SELECT
  (SELECT COUNT(*) FROM system_users WHERE tenant_id=${TENANT_ID} AND id=${ACCEPTED_USER_ID}) +
  (SELECT COUNT(*) FROM system_role WHERE tenant_id=${TENANT_ID} AND id=${ACCEPTED_ROLE_ID}) +
  (SELECT COUNT(*) FROM crm_customer WHERE tenant_id=${TENANT_ID} AND id=${ACCEPTED_CUSTOMER_ID});")"
[[ "$RESIDUAL" == "0" ]] || { printf 'Statistics acceptance cleanup left %s primary rows.\n' "$RESIDUAL" >&2; exit 1; }

printf 'CRM statistics runtime acceptance passed: user=%s role=%s customer=%s db=1 api=1.\n' \
  "$ACCEPTED_USER_ID" "$ACCEPTED_ROLE_ID" "$ACCEPTED_CUSTOMER_ID"
