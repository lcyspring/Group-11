#!/usr/bin/env bash
# Real two-user API acceptance for CRM marketing broadcast object scope. Only argument: KDL path.

set -Eeuo pipefail

PODMAN_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/../.." && pwd)"
source "${PODMAN_DIR}/lib/kdl-config.sh"

[[ $# -eq 1 ]] || { printf 'Usage: bash ./tests/acceptance/verify-crm-marketing-object-scope.sh <config.kdl>\n' >&2; exit 2; }
kdl_config_init "$1"
[[ "$(kdl_require schema_version)" == "1" ]] || exit 2

BASE_URL="$(kdl_require endpoint.base_url)"
TENANT_ID="$(kdl_positive_integer endpoint.tenant_id)"
ADMIN_USERNAME="$(kdl_require account.admin_username)"
PASSWORD="$(kdl_require account.password)"
MYSQL_CONTAINER="$(kdl_require mysql.container)"
MYSQL_USER="$(kdl_require mysql.user)"
MYSQL_PASSWORD="$(kdl_require mysql.password)"
MYSQL_DATABASE="$(kdl_require mysql.database)"
DEPARTMENT_ID="$(kdl_positive_integer acceptance.department_id)"
QUERY_PERMISSION="$(kdl_require acceptance.query_permission)"
REVIEW_PERMISSION="$(kdl_require acceptance.review_permission)"
DENIED_CODE="$(kdl_positive_integer acceptance.denied_code)"

[[ "$BASE_URL" =~ ^https?://[^[:space:]]+$ ]] || exit 2
for command in curl jq podman date; do command -v "$command" >/dev/null || exit 1; done

mysql_exec() {
    podman exec "$MYSQL_CONTAINER" mysql "-u${MYSQL_USER}" "-p${MYSQL_PASSWORD}" \
        "--database=${MYSQL_DATABASE}" --default-character-set=utf8mb4 -Nse "$1"
}

login() {
    local username="$1" response
    response="$(curl --noproxy '*' --fail --silent --show-error \
        --header 'Content-Type: application/json' --header "tenant-id: ${TENANT_ID}" \
        --data "$(jq -n --arg username "$username" --arg password "$PASSWORD" \
          '{username:$username,password:$password,captchaVerification:""}')" \
        "${BASE_URL}/system/auth/login")"
    jq -e '.code == 0 and (.data.accessToken | length > 0)' >/dev/null <<<"$response"
    jq -r '.data.accessToken' <<<"$response"
}

api_admin() {
    curl --noproxy '*' --fail --silent --show-error \
        --header "Authorization: Bearer ${ADMIN_TOKEN}" --header "tenant-id: ${TENANT_ID}" "$@"
}

api_get() {
    local token="$1" path="$2"; shift 2
    curl --noproxy '*' --silent --show-error --get \
        --header "Authorization: Bearer ${token}" --header "tenant-id: ${TENANT_ID}" \
        "$@" "${BASE_URL}${path}"
}

RUN_ID="$(date +%s%N)"
QUERY_USERNAME="mktscopeq${RUN_ID}"
REVIEW_USERNAME="mktscoper${RUN_ID}"
QUERY_ROLE_CODE="crm_mkt_query_${RUN_ID}"
REVIEW_ROLE_CODE="crm_mkt_review_${RUN_ID}"
PREFIX="CRM-MKT-SCOPE-${RUN_ID}"
QUERY_USER_ID=''
REVIEW_USER_ID=''
QUERY_ROLE_ID=''
REVIEW_ROLE_ID=''
OWN_BROADCAST_ID=''
OTHER_BROADCAST_ID=''
ADMIN_TOKEN=''

cleanup() {
    set +e
    [[ -n "$OWN_BROADCAST_ID" ]] && mysql_exec "DELETE FROM crm_marketing_broadcast_recipient WHERE broadcast_id=${OWN_BROADCAST_ID}; DELETE FROM crm_marketing_broadcast WHERE id=${OWN_BROADCAST_ID};" >/dev/null
    [[ -n "$OTHER_BROADCAST_ID" ]] && mysql_exec "DELETE FROM crm_marketing_broadcast_recipient WHERE broadcast_id=${OTHER_BROADCAST_ID}; DELETE FROM crm_marketing_broadcast WHERE id=${OTHER_BROADCAST_ID};" >/dev/null
    for user_id in "$QUERY_USER_ID" "$REVIEW_USER_ID"; do
        [[ "$user_id" =~ ^[0-9]+$ ]] || continue
        if [[ -n "$ADMIN_TOKEN" ]]; then
            api_admin --request DELETE --get --data-urlencode "id=${user_id}" "${BASE_URL}/system/user/delete" >/dev/null
        fi
        mysql_exec "DELETE FROM system_oauth2_access_token WHERE tenant_id=${TENANT_ID} AND user_id=${user_id};
          DELETE FROM system_oauth2_refresh_token WHERE tenant_id=${TENANT_ID} AND user_id=${user_id};
          DELETE FROM system_login_log WHERE tenant_id=${TENANT_ID} AND user_id=${user_id};
          DELETE FROM system_user_role WHERE tenant_id=${TENANT_ID} AND user_id=${user_id};
          DELETE FROM system_users WHERE tenant_id=${TENANT_ID} AND id=${user_id};" >/dev/null
    done
    for role_id in "$QUERY_ROLE_ID" "$REVIEW_ROLE_ID"; do
        [[ "$role_id" =~ ^[0-9]+$ ]] || continue
        if [[ -n "$ADMIN_TOKEN" ]]; then
            api_admin --request DELETE --get --data-urlencode "id=${role_id}" "${BASE_URL}/system/role/delete" >/dev/null
        fi
        mysql_exec "DELETE FROM system_role_menu WHERE tenant_id=${TENANT_ID} AND role_id=${role_id};
          DELETE FROM system_role WHERE tenant_id=${TENANT_ID} AND id=${role_id};" >/dev/null
    done
}
trap cleanup EXIT

ADMIN_TOKEN="$(login "$ADMIN_USERNAME")"
QUERY_MENU_ID="$(mysql_exec "SELECT id FROM system_menu WHERE permission='${QUERY_PERMISSION}' AND deleted=b'0' LIMIT 1;")"
REVIEW_MENU_ID="$(mysql_exec "SELECT id FROM system_menu WHERE permission='${REVIEW_PERMISSION}' AND deleted=b'0' LIMIT 1;")"
CUSTOMER_ID="$(mysql_exec "SELECT id FROM crm_customer WHERE tenant_id=${TENANT_ID} AND deleted=b'0' ORDER BY id LIMIT 1;")"
[[ "$QUERY_MENU_ID" =~ ^[0-9]+$ && "$REVIEW_MENU_ID" =~ ^[0-9]+$ && "$CUSTOMER_ID" =~ ^[0-9]+$ ]]

create_role() {
    local name="$1" code="$2" response
    response="$(api_admin --header 'Content-Type: application/json' \
      --data "$(jq -n --arg name "$name" --arg code "$code" '{name:$name,code:$code,sort:999,status:0}')" \
      "${BASE_URL}/system/role/create")"
    jq -e '.code == 0 and (.data | type == "number")' >/dev/null <<<"$response"
    jq -r '.data' <<<"$response"
}

create_user() {
    local username="$1" nickname="$2" response
    response="$(api_admin --header 'Content-Type: application/json' \
      --data "$(jq -n --arg username "$username" --arg password "$PASSWORD" --arg nickname "$nickname" \
        --argjson dept "$DEPARTMENT_ID" '{username:$username,password:$password,nickname:$nickname,deptId:$dept,status:0,sex:0}')" \
      "${BASE_URL}/system/user/create")"
    jq -e '.code == 0 and (.data | type == "number")' >/dev/null <<<"$response"
    jq -r '.data' <<<"$response"
}

assign_role_menus() {
    local role_id="$1" menus_json="$2"
    api_admin --header 'Content-Type: application/json' \
      --data "$(jq -n --argjson role "$role_id" --argjson menus "$menus_json" '{roleId:$role,menuIds:$menus}')" \
      "${BASE_URL}/system/permission/assign-role-menu" | jq -e '.code == 0' >/dev/null
    api_admin --header 'Content-Type: application/json' \
      --data "$(jq -n --argjson role "$role_id" '{roleId:$role,dataScope:5,dataScopeDeptIds:[]}')" \
      "${BASE_URL}/system/permission/assign-role-data-scope" | jq -e '.code == 0' >/dev/null
}

assign_user_role() {
    local user_id="$1" role_id="$2"
    api_admin --header 'Content-Type: application/json' \
      --data "$(jq -n --argjson user "$user_id" --argjson role "$role_id" '{userId:$user,roleIds:[$role]}')" \
      "${BASE_URL}/system/permission/assign-user-role" | jq -e '.code == 0' >/dev/null
}

QUERY_ROLE_ID="$(create_role 'CRM 群发范围查询验收' "$QUERY_ROLE_CODE")"
REVIEW_ROLE_ID="$(create_role 'CRM 群发范围审核验收' "$REVIEW_ROLE_CODE")"
assign_role_menus "$QUERY_ROLE_ID" "[${QUERY_MENU_ID}]"
assign_role_menus "$REVIEW_ROLE_ID" "[${QUERY_MENU_ID},${REVIEW_MENU_ID}]"
QUERY_USER_ID="$(create_user "$QUERY_USERNAME" 'CRM 群发普通查询验收')"
REVIEW_USER_ID="$(create_user "$REVIEW_USERNAME" 'CRM 群发审核验收')"
assign_user_role "$QUERY_USER_ID" "$QUERY_ROLE_ID"
assign_user_role "$REVIEW_USER_ID" "$REVIEW_ROLE_ID"

OWN_BROADCAST_ID="$(mysql_exec "INSERT INTO crm_marketing_broadcast
  (name,channel,status,total_count,valid_count,suppressed_count,sent_count,failed_count,creator,create_time,updater,update_time,deleted,tenant_id)
  VALUES ('${PREFIX}-OWN',1,10,1,1,0,0,0,'${QUERY_USER_ID}',NOW(),'${QUERY_USER_ID}',NOW(),b'0',${TENANT_ID}); SELECT LAST_INSERT_ID();")"
OTHER_BROADCAST_ID="$(mysql_exec "INSERT INTO crm_marketing_broadcast
  (name,channel,status,total_count,valid_count,suppressed_count,sent_count,failed_count,creator,create_time,updater,update_time,deleted,tenant_id)
  VALUES ('${PREFIX}-OTHER',1,10,1,1,0,0,0,'1',NOW(),'1',NOW(),b'0',${TENANT_ID}); SELECT LAST_INSERT_ID();")"
mysql_exec "INSERT INTO crm_marketing_broadcast_recipient
  (broadcast_id,customer_id,channel,mobile,status,attempt_count,creator,create_time,updater,update_time,deleted,tenant_id)
  VALUES (${OWN_BROADCAST_ID},${CUSTOMER_ID},1,'13800000001',10,0,'${QUERY_USER_ID}',NOW(),'${QUERY_USER_ID}',NOW(),b'0',${TENANT_ID}),
         (${OTHER_BROADCAST_ID},${CUSTOMER_ID},1,'13800000002',10,0,'1',NOW(),'1',NOW(),b'0',${TENANT_ID});" >/dev/null

QUERY_TOKEN="$(login "$QUERY_USERNAME")"
REVIEW_TOKEN="$(login "$REVIEW_USERNAME")"

QUERY_PAGE="$(api_get "$QUERY_TOKEN" '/crm/marketing/outreach/broadcast/page' \
  --data-urlencode 'pageNo=1' --data-urlencode 'pageSize=20' --data-urlencode "name=${PREFIX}")"
jq -e --argjson own "$OWN_BROADCAST_ID" '.code == 0 and .data.total == 1 and .data.list[0].id == $own' >/dev/null <<<"$QUERY_PAGE"

OWN_DETAIL="$(api_get "$QUERY_TOKEN" '/crm/marketing/outreach/broadcast/get' --data-urlencode "id=${OWN_BROADCAST_ID}")"
jq -e --argjson own "$OWN_BROADCAST_ID" '.code == 0 and .data.id == $own' >/dev/null <<<"$OWN_DETAIL"
OTHER_DETAIL="$(api_get "$QUERY_TOKEN" '/crm/marketing/outreach/broadcast/get' --data-urlencode "id=${OTHER_BROADCAST_ID}")"
jq -e --argjson denied "$DENIED_CODE" '.code == $denied' >/dev/null <<<"$OTHER_DETAIL"
OTHER_RECIPIENTS="$(api_get "$QUERY_TOKEN" '/crm/marketing/outreach/broadcast/recipients' \
  --data-urlencode "broadcastId=${OTHER_BROADCAST_ID}" --data-urlencode 'pageNo=1' --data-urlencode 'pageSize=20')"
jq -e --argjson denied "$DENIED_CODE" '.code == $denied and (.data == null)' >/dev/null <<<"$OTHER_RECIPIENTS"
OWN_SUMMARY="$(api_get "$QUERY_TOKEN" '/crm/marketing/outreach/broadcast/delivery-summary' \
  --data-urlencode "id=${OWN_BROADCAST_ID}")"
jq -e --argjson own "$OWN_BROADCAST_ID" '.code == 0 and .data.broadcastId == $own' >/dev/null <<<"$OWN_SUMMARY"
OTHER_SUMMARY="$(api_get "$QUERY_TOKEN" '/crm/marketing/outreach/broadcast/delivery-summary' \
  --data-urlencode "id=${OTHER_BROADCAST_ID}")"
jq -e --argjson denied "$DENIED_CODE" '.code == $denied and (.data == null)' >/dev/null <<<"$OTHER_SUMMARY"
OTHER_SYNC="$(api_get "$QUERY_TOKEN" '/crm/marketing/outreach/broadcast/sync-results' \
  --request PUT --data-urlencode "id=${OTHER_BROADCAST_ID}")"
jq -e --argjson denied "$DENIED_CODE" '.code == $denied and (.data == null)' >/dev/null <<<"$OTHER_SYNC"

REVIEW_PAGE="$(api_get "$REVIEW_TOKEN" '/crm/marketing/outreach/broadcast/page' \
  --data-urlencode 'pageNo=1' --data-urlencode 'pageSize=20' --data-urlencode "name=${PREFIX}")"
jq -e '.code == 0 and .data.total == 2' >/dev/null <<<"$REVIEW_PAGE"
REVIEW_RECIPIENTS="$(api_get "$REVIEW_TOKEN" '/crm/marketing/outreach/broadcast/recipients' \
  --data-urlencode "broadcastId=${OTHER_BROADCAST_ID}" --data-urlencode 'pageNo=1' --data-urlencode 'pageSize=20')"
jq -e '.code == 0 and .data.total == 1 and .data.list[0].mobile == "13800000002"' >/dev/null <<<"$REVIEW_RECIPIENTS"
REVIEW_SUMMARY="$(api_get "$REVIEW_TOKEN" '/crm/marketing/outreach/broadcast/delivery-summary' \
  --data-urlencode "id=${OTHER_BROADCAST_ID}")"
jq -e --argjson other "$OTHER_BROADCAST_ID" '.code == 0 and .data.broadcastId == $other' >/dev/null <<<"$REVIEW_SUMMARY"
REVIEW_SYNC="$(api_get "$REVIEW_TOKEN" '/crm/marketing/outreach/broadcast/sync-results' \
  --request PUT --data-urlencode "id=${OTHER_BROADCAST_ID}")"
jq -e '.code == 0 and .data == 0' >/dev/null <<<"$REVIEW_SYNC"

ACCEPTED_QUERY_USER="$QUERY_USER_ID"
ACCEPTED_REVIEW_USER="$REVIEW_USER_ID"
cleanup
OWN_BROADCAST_ID=''; OTHER_BROADCAST_ID=''; QUERY_USER_ID=''; REVIEW_USER_ID=''; QUERY_ROLE_ID=''; REVIEW_ROLE_ID=''
RESIDUAL="$(mysql_exec "SELECT
  (SELECT COUNT(*) FROM crm_marketing_broadcast WHERE name LIKE '${PREFIX}%') +
  (SELECT COUNT(*) FROM system_users WHERE username IN ('${QUERY_USERNAME}','${REVIEW_USERNAME}')) +
  (SELECT COUNT(*) FROM system_role WHERE code IN ('${QUERY_ROLE_CODE}','${REVIEW_ROLE_CODE}'));")"
[[ "$RESIDUAL" == "0" ]]

printf 'marketing-object-scope=ok\nquery-user=%s reviewer-user=%s list=1/2 detail=allow+deny recipients=allow+deny analytics=allow+deny cleanup=0\n' \
  "$ACCEPTED_QUERY_USER" "$ACCEPTED_REVIEW_USER"
