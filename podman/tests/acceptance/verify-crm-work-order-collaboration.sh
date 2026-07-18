#!/usr/bin/env bash
# CRM work-order groups, automatic dispatch, unassigned pool, claim and CC real API acceptance.

set -Eeuo pipefail

PODMAN_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/../.." && pwd)"
source "${PODMAN_DIR}/lib/kdl-config.sh"

[[ $# -eq 1 ]] || { printf 'Usage: bash ./tests/acceptance/verify-crm-work-order-collaboration.sh <config.kdl>\n' >&2; exit 2; }
kdl_config_init "$1"
[[ "$(kdl_require schema_version)" == "1" ]] || { printf 'Unsupported schema_version.\n' >&2; exit 2; }

BASE_URL="$(kdl_require endpoint.base_url)"
TENANT_ID="$(kdl_positive_integer endpoint.tenant_id)"
USERNAME="$(kdl_require account.username)"
PASSWORD="$(kdl_require account.password)"
CUSTOMER_ID="$(kdl_positive_integer acceptance.customer_id)"
MANAGER_USER_ID="$(kdl_positive_integer acceptance.manager_user_id)"
MEMBER_USER_ID="$(kdl_positive_integer acceptance.member_user_id)"
CC_USER_ID="$(kdl_positive_integer acceptance.cc_user_id)"
MYSQL_CONTAINER="$(kdl_require mysql.container)"
MYSQL_USER="$(kdl_require mysql.user)"
MYSQL_PASSWORD="$(kdl_require mysql.password)"
MYSQL_DATABASE="$(kdl_require mysql.database)"

[[ "$BASE_URL" =~ ^https?://[^[:space:]]+$ ]] || { printf 'Invalid endpoint.base_url.\n' >&2; exit 2; }
[[ "$MYSQL_CONTAINER" =~ ^[a-zA-Z0-9_.-]+$ && "$MYSQL_USER" =~ ^[a-zA-Z0-9_.-]+$ && "$MYSQL_DATABASE" =~ ^[a-zA-Z0-9_]+$ ]] || {
    printf 'Invalid MySQL identifier in KDL.\n' >&2; exit 2;
}
for command in curl jq podman date; do
    command -v "$command" >/dev/null || { printf 'Missing command: %s\n' "$command" >&2; exit 1; }
done

RUN_ID="$(date +%s)"
GROUP_CODE="accept_${RUN_ID}"
POOL_GROUP_CODE="pool_${RUN_ID}"
TITLE="wo-collab-${RUN_ID}"
POOL_TITLE="wo-pool-${RUN_ID}"
GROUP_ID=''
POOL_GROUP_ID=''
WORK_ORDER_ID=''
POOL_WORK_ORDER_ID=''
SEED_IDS=''

mysql_exec() {
    local sql="$1" output='' attempt
    for attempt in 1 2 3 4 5 6 7 8; do
        if output="$(podman exec "$MYSQL_CONTAINER" mysql "-u${MYSQL_USER}" "-p${MYSQL_PASSWORD}" \
            "--database=${MYSQL_DATABASE}" --default-character-set=utf8mb4 -Nse "$sql")"; then
            printf '%s' "$output"
            return 0
        fi
        sleep 1
    done
    return 1
}

cleanup() {
    set +e
    mysql_exec "DELETE FROM system_notify_message WHERE tenant_id=${TENANT_ID}
        AND (template_params LIKE '%${TITLE}%' OR template_params LIKE '%${POOL_TITLE}%');
      DELETE FROM crm_work_order_cc WHERE tenant_id=${TENANT_ID}
        AND work_order_id IN (${WORK_ORDER_ID:-0},${POOL_WORK_ORDER_ID:-0});
      DELETE FROM crm_work_order_record WHERE tenant_id=${TENANT_ID}
        AND work_order_id IN (${WORK_ORDER_ID:-0},${POOL_WORK_ORDER_ID:-0});
      DELETE FROM crm_work_order WHERE tenant_id=${TENANT_ID}
        AND (id IN (${WORK_ORDER_ID:-0},${POOL_WORK_ORDER_ID:-0}) OR no LIKE 'ACC-${RUN_ID}-%');
      DELETE FROM crm_work_order_group_member WHERE tenant_id=${TENANT_ID}
        AND group_id IN (${GROUP_ID:-0},${POOL_GROUP_ID:-0});
      DELETE FROM crm_work_order_group WHERE tenant_id=${TENANT_ID}
        AND id IN (${GROUP_ID:-0},${POOL_GROUP_ID:-0});" >/dev/null 2>&1
}
trap cleanup EXIT

for user_id in "$MANAGER_USER_ID" "$MEMBER_USER_ID" "$CC_USER_ID"; do
    [[ "$(mysql_exec "SELECT COUNT(*) FROM system_users WHERE tenant_id=${TENANT_ID} AND id=${user_id} AND status=0 AND deleted=b'0';")" == "1" ]] || {
        printf 'Configured acceptance user does not exist or is disabled: %s\n' "$user_id" >&2; exit 1;
    }
done
[[ "$(mysql_exec "SELECT COUNT(*) FROM crm_customer WHERE tenant_id=${TENANT_ID} AND id=${CUSTOMER_ID} AND deleted=b'0';")" == "1" ]] || {
    printf 'Configured CRM customer does not exist: %s\n' "$CUSTOMER_ID" >&2; exit 1;
}

LOGIN_PAYLOAD="$(jq -n --arg username "$USERNAME" --arg password "$PASSWORD" \
    '{username:$username,password:$password,captchaVerification:""}')"
LOGIN_RESPONSE="$(curl --noproxy '*' --fail --silent --show-error --header 'Content-Type: application/json' \
    --header "tenant-id: ${TENANT_ID}" --data "$LOGIN_PAYLOAD" "${BASE_URL}/system/auth/login")"
jq -e '.code == 0 and (.data.accessToken | length > 0)' >/dev/null <<< "$LOGIN_RESPONSE" || {
    printf 'Login failed.\n' >&2; exit 1;
}
TOKEN="$(jq -r '.data.accessToken' <<< "$LOGIN_RESPONSE")"

api_raw() {
    local method="$1" path="$2" body="${3:-}"
    local args=(--noproxy '*' --fail --silent --show-error --request "$method"
        --header "Authorization: Bearer ${TOKEN}" --header "tenant-id: ${TENANT_ID}")
    [[ -z "$body" ]] || args+=(--header 'Content-Type: application/json' --data "$body")
    curl "${args[@]}" "${BASE_URL}${path}"
}

api() {
    local response
    response="$(api_raw "$@")"
    jq -e '.code == 0' >/dev/null <<< "$response" || {
        printf 'API %s %s failed: %s\n' "$1" "$2" "$(jq -r '.msg' <<< "$response")" >&2; return 1;
    }
    printf '%s' "$response"
}

group_payload() {
    local code="$1" name="$2" manager="$3" members_json="$4" id="${5:-}"
    jq -n --arg code "$code" --arg name "$name" --argjson manager "$manager" --argjson members "$members_json" \
        --arg id "$id" '{code:$code,name:$name,managerUserId:$manager,supportedTypes:[1,2,3,4],
          memberUserIds:$members,status:0,sort:10,remark:"real API acceptance"}
          + (if $id == "" then {} else {id:($id | tonumber)} end)'
}

GROUP_RESPONSE="$(api POST '/crm/work-order-group/save' \
    "$(group_payload "$GROUP_CODE" "验收处理组-${RUN_ID}" "$MANAGER_USER_ID" "[${MANAGER_USER_ID},${MEMBER_USER_ID}]")")"
GROUP_ID="$(jq -r '.data' <<< "$GROUP_RESPONSE")"
POOL_GROUP_RESPONSE="$(api POST '/crm/work-order-group/save' \
    "$(group_payload "$POOL_GROUP_CODE" "验收未分配池-${RUN_ID}" "$MANAGER_USER_ID" "[${MANAGER_USER_ID}]")")"
POOL_GROUP_ID="$(jq -r '.data' <<< "$POOL_GROUP_RESPONSE")"

api POST '/crm/work-order-group/save' \
    "$(group_payload "$GROUP_CODE" "验收处理组已更新-${RUN_ID}" "$MANAGER_USER_ID" \
      "[${MANAGER_USER_ID},${MEMBER_USER_ID}]" "$GROUP_ID")" >/dev/null
GROUP_LIST="$(api GET '/crm/work-order-group/list')"
jq -e --argjson id "$GROUP_ID" '.data | any(.id == $id and (.name | contains("已更新"))
  and (.memberUserIds | length) == 2)' >/dev/null <<< "$GROUP_LIST" || {
    printf 'Work-order group create/update/list acceptance failed.\n' >&2; exit 1;
}

MANAGER_OPEN="$(mysql_exec "SELECT COUNT(*) FROM crm_work_order WHERE tenant_id=${TENANT_ID}
  AND handler_user_id=${MANAGER_USER_ID} AND status IN (10,20) AND deleted=b'0';")"
MEMBER_OPEN="$(mysql_exec "SELECT COUNT(*) FROM crm_work_order WHERE tenant_id=${TENANT_ID}
  AND handler_user_id=${MEMBER_USER_ID} AND status IN (10,20) AND deleted=b'0';")"
if (( MANAGER_OPEN <= MEMBER_OPEN )); then
    SEED_COUNT=$((MEMBER_OPEN - MANAGER_OPEN + 1))
    for ((index=1; index<=SEED_COUNT; index++)); do
        mysql_exec "INSERT INTO crm_work_order
          (no,title,type,priority,status,customer_id,source_type,group_id,handler_user_id,dispatch_mode,
           description,creator,updater,deleted,tenant_id)
          VALUES ('ACC-${RUN_ID}-${index}','${TITLE}-seed-${index}',1,1,10,${CUSTOMER_ID},0,${GROUP_ID},
            ${MANAGER_USER_ID},1,'acceptance load seed description','acceptance','acceptance',b'0',${TENANT_ID});" >/dev/null
    done
fi

CONTEXT="$(api GET "/crm/work-order/dispatch-context?type=1&groupId=${GROUP_ID}")"
jq -e --argjson manager "$MANAGER_USER_ID" --argjson member "$MEMBER_USER_ID" \
  '.data.manualAssignmentAllowed == true and (.data.candidates | map(.id) | index($manager)) != null
   and (.data.candidates | map(.id) | index($member)) != null' >/dev/null <<< "$CONTEXT" || {
    printf 'Dispatch context did not expose the managed group candidates.\n' >&2; exit 1;
}

CREATE_PAYLOAD="$(jq -n --arg title "$TITLE" --argjson customer "$CUSTOMER_ID" --argjson group "$GROUP_ID" \
    --argjson manager "$MANAGER_USER_ID" --argjson cc "$CC_USER_ID" \
    '{title:$title,type:1,priority:3,customerId:$customer,sourceType:0,groupId:$group,handlerUserId:null,
      ccUserIds:[$manager,$manager,$cc],description:"客户反馈的服务问题已经持续多日，需要处理组立即跟进并形成闭环。",attachmentUrls:[]}')"
WORK_ORDER_ID="$(api POST '/crm/work-order/create' "$CREATE_PAYLOAD" | jq -r '.data')"

AUTO_ROW="$(mysql_exec "SELECT CONCAT_WS(',',handler_user_id,group_id,dispatch_mode)
  FROM crm_work_order WHERE tenant_id=${TENANT_ID} AND id=${WORK_ORDER_ID};")"
[[ "$AUTO_ROW" == "${MEMBER_USER_ID},${GROUP_ID},2" ]] || {
    printf 'Least-loaded automatic dispatch failed: %s\n' "$AUTO_ROW" >&2; exit 1;
}
[[ "$(mysql_exec "SELECT COUNT(*) FROM crm_work_order_cc WHERE tenant_id=${TENANT_ID}
  AND work_order_id=${WORK_ORDER_ID};")" == "2" ]] || { printf 'CC de-duplication failed.\n' >&2; exit 1; }
[[ "$(mysql_exec "SELECT COUNT(*) FROM system_notify_message WHERE tenant_id=${TENANT_ID}
  AND template_code='crm-work-order-copied' AND user_id IN (${MANAGER_USER_ID},${CC_USER_ID})
  AND template_params LIKE '%${TITLE}%';")" == "2" ]] || { printf 'CC notification acceptance failed.\n' >&2; exit 1; }

COPIED_PAGE="$(api GET '/crm/work-order/page?pageNo=1&pageSize=20&sceneType=3')"
jq -e --argjson id "$WORK_ORDER_ID" '.data.list | any(.id == $id)' >/dev/null <<< "$COPIED_PAGE" || {
    printf 'Copied-to-me scene did not contain the work order.\n' >&2; exit 1;
}

mysql_exec "UPDATE crm_work_order_group_member SET deleted=b'1' WHERE tenant_id=${TENANT_ID}
  AND group_id=${POOL_GROUP_ID} AND user_id=${MANAGER_USER_ID};" >/dev/null
POOL_PAYLOAD="$(jq -n --arg title "$POOL_TITLE" --argjson customer "$CUSTOMER_ID" --argjson group "$POOL_GROUP_ID" \
    '{title:$title,type:1,priority:2,customerId:$customer,sourceType:0,groupId:$group,handlerUserId:null,
      ccUserIds:[],description:"该问题需要先进入处理组未分配池，再由值班成员主动领取并处理。",attachmentUrls:[]}')"
POOL_WORK_ORDER_ID="$(api POST '/crm/work-order/create' "$POOL_PAYLOAD" | jq -r '.data')"
[[ "$(mysql_exec "SELECT CONCAT(IFNULL(handler_user_id,'NULL'),',',dispatch_mode) FROM crm_work_order
  WHERE tenant_id=${TENANT_ID} AND id=${POOL_WORK_ORDER_ID};")" == "NULL,0" ]] || {
    printf 'Unassigned-pool fallback failed.\n' >&2; exit 1;
}
mysql_exec "UPDATE crm_work_order_group_member SET deleted=b'0' WHERE tenant_id=${TENANT_ID}
  AND group_id=${POOL_GROUP_ID} AND user_id=${MANAGER_USER_ID};" >/dev/null
POOL_PAGE="$(api GET '/crm/work-order/page?pageNo=1&pageSize=20&sceneType=4')"
jq -e --argjson id "$POOL_WORK_ORDER_ID" '.data.list | any(.id == $id)' >/dev/null <<< "$POOL_PAGE" || {
    printf 'Unassigned group scene did not contain the work order.\n' >&2; exit 1;
}
api PUT '/crm/work-order/claim' "$(jq -n --argjson id "$POOL_WORK_ORDER_ID" '{id:$id,remark:"值班领取"}')" >/dev/null
[[ "$(mysql_exec "SELECT CONCAT(handler_user_id,',',dispatch_mode) FROM crm_work_order
  WHERE tenant_id=${TENANT_ID} AND id=${POOL_WORK_ORDER_ID};")" == "${MANAGER_USER_ID},3" ]] || {
    printf 'Atomic work-order claim failed.\n' >&2; exit 1;
}

api PUT '/crm/work-order/assign' "$(jq -n --argjson id "$WORK_ORDER_ID" --argjson group "$GROUP_ID" \
  --argjson handler "$MANAGER_USER_ID" '{id:$id,groupId:$group,handlerUserId:$handler,remark:"验收改派"}')" >/dev/null
api PUT '/crm/work-order/start' "$(jq -n --argjson id "$WORK_ORDER_ID" '{id:$id,remark:"开始验收处理"}')" >/dev/null
api PUT '/crm/work-order/complete' "$(jq -n --argjson id "$WORK_ORDER_ID" \
  '{id:$id,solution:"已完成问题定位、客户沟通、处理复核与结果回访，当前服务工单正式闭环。"}')" >/dev/null
DETAIL="$(api GET "/crm/work-order/get?id=${WORK_ORDER_ID}")"
jq -e '.data.status == 30 and .data.dispatchMode == 4
  and (.data.records | map(.actionType) | index(7)) != null
  and (.data.records | map(.actionType) | index(3)) != null
  and (.data.records | map(.actionType) | index(6)) != null' >/dev/null <<< "$DETAIL" || {
    printf 'Reassign/start/complete trajectory acceptance failed.\n' >&2; exit 1;
}

ACCEPTED_GROUP_ID="$GROUP_ID"
ACCEPTED_POOL_GROUP_ID="$POOL_GROUP_ID"
ACCEPTED_ORDER_ID="$WORK_ORDER_ID"
ACCEPTED_POOL_ORDER_ID="$POOL_WORK_ORDER_ID"
cleanup
GROUP_ID=''; POOL_GROUP_ID=''; WORK_ORDER_ID=''; POOL_WORK_ORDER_ID=''
RESIDUAL="$(mysql_exec "SELECT
  (SELECT COUNT(*) FROM crm_work_order WHERE tenant_id=${TENANT_ID} AND (id IN (${ACCEPTED_ORDER_ID},${ACCEPTED_POOL_ORDER_ID}) OR no LIKE 'ACC-${RUN_ID}-%')) +
  (SELECT COUNT(*) FROM crm_work_order_group WHERE tenant_id=${TENANT_ID} AND id IN (${ACCEPTED_GROUP_ID},${ACCEPTED_POOL_GROUP_ID})) +
  (SELECT COUNT(*) FROM system_notify_message WHERE tenant_id=${TENANT_ID}
    AND (template_params LIKE '%${TITLE}%' OR template_params LIKE '%${POOL_TITLE}%')); ")"
[[ "$RESIDUAL" == "0" ]] || { printf 'Acceptance cleanup left %s rows.\n' "$RESIDUAL" >&2; exit 1; }

printf 'CRM work-order collaboration real API acceptance passed: auto=%s pool=%s; cleanup residual=0.\n' \
    "$ACCEPTED_ORDER_ID" "$ACCEPTED_POOL_ORDER_ID"
