#!/usr/bin/env bash
# Real API acceptance for clue task/call/SMS migration. The only CLI argument is an explicit KDL path.

set -Eeuo pipefail

PODMAN_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/../.." && pwd)"
source "${PODMAN_DIR}/lib/kdl-config.sh"

[[ $# -eq 1 ]] || { printf 'Usage: bash ./tests/acceptance/verify-crm-clue-activity.sh <config.kdl>\n' >&2; exit 2; }
kdl_config_init "$1"
[[ "$(kdl_require schema_version)" == "1" ]] || { printf 'Unsupported schema_version.\n' >&2; exit 2; }

BASE_URL="$(kdl_require endpoint.base_url)"
TENANT_ID="$(kdl_positive_integer endpoint.tenant_id)"
USERNAME="$(kdl_require account.username)"
PASSWORD="$(kdl_require account.password)"
OWNER_USER_ID="$(kdl_positive_integer acceptance.owner_user_id)"
CONTACT_NAME="$(kdl_require acceptance.contact_name)"
MOBILE="$(kdl_require acceptance.mobile)"
PHONE="$(kdl_require acceptance.phone)"
MYSQL_CONTAINER="$(kdl_require mysql.container)"
MYSQL_USER="$(kdl_require mysql.user)"
MYSQL_PASSWORD="$(kdl_require mysql.password)"
MYSQL_DATABASE="$(kdl_require mysql.database)"

[[ "$BASE_URL" =~ ^https?://[^[:space:]]+$ ]] || { printf 'Invalid endpoint.base_url.\n' >&2; exit 2; }
[[ "$MYSQL_CONTAINER" =~ ^[a-zA-Z0-9_.-]+$ && "$MYSQL_USER" =~ ^[a-zA-Z0-9_.-]+$ && "$MYSQL_DATABASE" =~ ^[a-zA-Z0-9_]+$ ]] || {
    printf 'Invalid MySQL identifier in KDL.\n' >&2; exit 2;
}
for command in curl jq podman date; do command -v "$command" >/dev/null || { printf 'Missing command: %s\n' "$command" >&2; exit 1; }; done

RUN_ID="$(date +%s)"
CLUE_NAME="crm-activity-acceptance-${RUN_ID}"
TASK_TITLE_1="crm-activity-task-finished-${RUN_ID}"
TASK_TITLE_2="crm-activity-task-open-${RUN_ID}"
CLUE_ID=''
CUSTOMER_ID=''
CONTACT_ID=''
TASK_ID_1=''
TASK_ID_2=''
CALL_ID=''
SMS_ID=''

mysql_exec() {
    podman exec --env "MYSQL_PWD=${MYSQL_PASSWORD}" "$MYSQL_CONTAINER" mysql "-u${MYSQL_USER}" \
        "--database=${MYSQL_DATABASE}" --default-character-set=utf8mb4 -Nse "$1"
}

cleanup() {
    set +e
    if [[ -n "$CLUE_ID" ]]; then
        mysql_exec "DELETE FROM system_notify_message WHERE tenant_id=${TENANT_ID} AND template_code IN ('crm-task-assigned','crm-task-finished') AND template_params LIKE '%${RUN_ID}%';
          DELETE FROM crm_task_action_record WHERE tenant_id=${TENANT_ID} AND task_id IN (SELECT id FROM crm_task WHERE tenant_id=${TENANT_ID} AND source_clue_id=${CLUE_ID});
          DELETE FROM crm_task WHERE tenant_id=${TENANT_ID} AND (source_clue_id=${CLUE_ID} OR (biz_type=1 AND biz_id=${CLUE_ID}));
          DELETE FROM crm_call_record WHERE tenant_id=${TENANT_ID} AND (source_clue_id=${CLUE_ID} OR (biz_type=1 AND biz_id=${CLUE_ID}));
          DELETE FROM crm_sms_record WHERE tenant_id=${TENANT_ID} AND (source_clue_id=${CLUE_ID} OR (biz_type=1 AND biz_id=${CLUE_ID}));
          DELETE FROM crm_clue_conversion_record WHERE tenant_id=${TENANT_ID} AND clue_id=${CLUE_ID};
          DELETE FROM crm_follow_up_record WHERE tenant_id=${TENANT_ID} AND ((biz_type=1 AND biz_id=${CLUE_ID}) OR (biz_type=2 AND biz_id=${CUSTOMER_ID:-0}));
          DELETE FROM crm_permission WHERE tenant_id=${TENANT_ID} AND ((biz_type=1 AND biz_id=${CLUE_ID}) OR (biz_type=2 AND biz_id=${CUSTOMER_ID:-0}) OR (biz_type=3 AND biz_id=${CONTACT_ID:-0}));
          DELETE FROM crm_contact WHERE tenant_id=${TENANT_ID} AND customer_id=${CUSTOMER_ID:-0};
          DELETE FROM crm_customer_lifecycle_record WHERE tenant_id=${TENANT_ID} AND customer_id=${CUSTOMER_ID:-0};
          DELETE FROM crm_customer_owner_record WHERE tenant_id=${TENANT_ID} AND customer_id=${CUSTOMER_ID:-0};
          DELETE FROM crm_customer WHERE tenant_id=${TENANT_ID} AND id=${CUSTOMER_ID:-0};
          DELETE FROM crm_clue_owner_record WHERE tenant_id=${TENANT_ID} AND clue_id=${CLUE_ID};
          DELETE FROM crm_clue WHERE tenant_id=${TENANT_ID} AND id=${CLUE_ID};" >/dev/null
    fi
}
trap cleanup EXIT

LOGIN_PAYLOAD="$(jq -n --arg username "$USERNAME" --arg password "$PASSWORD" '{username:$username,password:$password,captchaVerification:""}')"
LOGIN_RESPONSE="$(curl --noproxy '*' --fail --silent --show-error --header 'Content-Type: application/json' \
    --header "tenant-id: ${TENANT_ID}" --data "$LOGIN_PAYLOAD" "${BASE_URL}/system/auth/login")"
jq -e '.code == 0 and (.data.accessToken | length > 0)' >/dev/null <<< "$LOGIN_RESPONSE" || {
    printf 'Login failed: %s\n' "$(jq -r '.msg' <<< "$LOGIN_RESPONSE")" >&2; exit 1;
}
TOKEN="$(jq -r '.data.accessToken' <<< "$LOGIN_RESPONSE")"

api() {
    local method="$1" path="$2" body="${3:-}" response
    local args=(--noproxy '*' --fail --silent --show-error --request "$method"
        --header "Authorization: Bearer ${TOKEN}" --header "tenant-id: ${TENANT_ID}")
    [[ -z "$body" ]] || args+=(--header 'Content-Type: application/json' --data "$body")
    response="$(curl "${args[@]}" "${BASE_URL}${path}")"
    if ! jq -e '.code == 0' >/dev/null <<< "$response"; then
        printf 'API %s %s failed: %s\n' "$method" "$path" "$(jq -r '.msg' <<< "$response")" >&2
        return 1
    fi
    printf '%s' "$response"
}

NOW_MS="$(( $(date +%s) * 1000 ))"
DUE_MS="$(( NOW_MS + 172800000 ))"
REMIND_MS="$(( NOW_MS + 86400000 ))"
CALL_START_MS="$(( NOW_MS - 60000 ))"

CLUE_ID="$(api POST '/crm/clue/create' "$(jq -n --arg name "$CLUE_NAME" --arg mobile "$MOBILE" --argjson owner "$OWNER_USER_ID" \
    '{name:$name,mobile:$mobile,ownerUserId:$owner,description:"real API activity migration acceptance"}')" | jq -r '.data')"

create_task() {
    local title="$1"
    api POST '/crm/activity/task/create' "$(jq -n --arg title "$title" --argjson clue "$CLUE_ID" --argjson owner "$OWNER_USER_ID" \
        --argjson due "$DUE_MS" --argjson remind "$REMIND_MS" \
        '{bizType:1,bizId:$clue,type:2,title:$title,description:"acceptance task",priority:2,assigneeUserId:$owner,dueTime:$due,remindTime:$remind,notifySystem:true,notifyEmail:false,notifySms:false}')" | jq -r '.data'
}
TASK_ID_1="$(create_task "$TASK_TITLE_1")"
TASK_ID_2="$(create_task "$TASK_TITLE_2")"

CALL_ID="$(api POST '/crm/activity/call/create' "$(jq -n --arg phone "$PHONE" --argjson clue "$CLUE_ID" --argjson start "$CALL_START_MS" --argjson end "$NOW_MS" \
    '{bizType:1,bizId:$clue,direction:1,status:10,phone:$phone,startTime:$start,endTime:$end,summary:"acceptance call"}')" | jq -r '.data')"
SMS_ID="$(api POST '/crm/activity/sms/create' "$(jq -n --arg mobile "$MOBILE" --argjson clue "$CLUE_ID" --argjson occurred "$NOW_MS" \
    '{bizType:1,bizId:$clue,direction:1,status:10,mobile:$mobile,content:"acceptance sms",occurredTime:$occurred}')" | jq -r '.data')"

api PUT '/crm/activity/task/start' "$(jq -n --argjson id "$TASK_ID_1" '{id:$id,remark:"acceptance start"}')" >/dev/null
api PUT '/crm/activity/task/complete' "$(jq -n --argjson id "$TASK_ID_1" '{id:$id,remark:"acceptance complete"}')" >/dev/null
HISTORY="$(api GET "/crm/activity/task/action-records?taskId=${TASK_ID_1}")"
jq -e '.data | map(.actionType) == [1,3,4]' >/dev/null <<< "$HISTORY" || { printf 'Task history mismatch.\n' >&2; exit 1; }

api PUT '/crm/clue/transform' "$(jq -n --argjson id "$CLUE_ID" --arg contact "$CONTACT_NAME" --arg mobile "$MOBILE" \
    '{id:$id,contactName:$contact,contactMobile:$mobile}')" >/dev/null
CLUE_DETAIL="$(api GET "/crm/clue/get?id=${CLUE_ID}")"
CUSTOMER_ID="$(jq -r '.data.customerId' <<< "$CLUE_DETAIL")"
CONVERSION="$(api GET "/crm/activity/conversion-record?clueId=${CLUE_ID}")"
CONTACT_ID="$(jq -r '.data.primaryContactId' <<< "$CONVERSION")"
jq -e --argjson clue "$CLUE_ID" --argjson customer "$CUSTOMER_ID" \
    '.data.clueId == $clue and .data.customerId == $customer and .data.followUpCount == 0 and .data.taskCount == 2 and .data.callCount == 1 and .data.smsCount == 1' \
    >/dev/null <<< "$CONVERSION" || { printf 'Conversion audit mismatch.\n' >&2; exit 1; }

TASK_PAGE="$(api GET "/crm/activity/task/page?pageNo=1&pageSize=10&bizType=2&bizId=${CUSTOMER_ID}")"
CALL_PAGE="$(api GET "/crm/activity/call/page?pageNo=1&pageSize=10&bizType=2&bizId=${CUSTOMER_ID}")"
SMS_PAGE="$(api GET "/crm/activity/sms/page?pageNo=1&pageSize=10&bizType=2&bizId=${CUSTOMER_ID}")"
jq -e --argjson clue "$CLUE_ID" '.data.total == 2 and ([.data.list[].sourceClueId] | all(. == $clue))' >/dev/null <<< "$TASK_PAGE" || { printf 'Migrated task page mismatch.\n' >&2; exit 1; }
jq -e --argjson clue "$CLUE_ID" '.data.total == 1 and .data.list[0].sourceClueId == $clue and .data.list[0].durationSeconds == 60' >/dev/null <<< "$CALL_PAGE" || { printf 'Migrated call page mismatch.\n' >&2; exit 1; }
jq -e --argjson clue "$CLUE_ID" '.data.total == 1 and .data.list[0].sourceClueId == $clue' >/dev/null <<< "$SMS_PAGE" || { printf 'Migrated SMS page mismatch.\n' >&2; exit 1; }

OLD_TASK_PAGE="$(api GET "/crm/activity/task/page?pageNo=1&pageSize=10&bizType=1&bizId=${CLUE_ID}")"
jq -e '.data.total == 0' >/dev/null <<< "$OLD_TASK_PAGE" || { printf 'Source clue still exposes migrated tasks.\n' >&2; exit 1; }

DUPLICATE_RESPONSE="$(curl --noproxy '*' --fail --silent --show-error --request PUT \
    --header "Authorization: Bearer ${TOKEN}" --header "tenant-id: ${TENANT_ID}" --header 'Content-Type: application/json' \
    --data "$(jq -n --argjson id "$CLUE_ID" --arg contact "$CONTACT_NAME" --arg mobile "$MOBILE" '{id:$id,contactName:$contact,contactMobile:$mobile}')" \
    "${BASE_URL}/crm/clue/transform")"
jq -e '.code != 0' >/dev/null <<< "$DUPLICATE_RESPONSE" || { printf 'Duplicate conversion was not rejected.\n' >&2; exit 1; }

printf 'CRM clue activity migration acceptance passed: clue=%s customer=%s contact=%s tasks=2 calls=1 sms=1.\n' \
    "$CLUE_ID" "$CUSTOMER_ID" "$CONTACT_ID"
