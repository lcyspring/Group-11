#!/usr/bin/env bash

set -Eeuo pipefail
trap 'printf "CRM customer-care acceptance failed at line %s.\n" "$LINENO" >&2' ERR

PODMAN_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/../.." && pwd)"
source "${PODMAN_DIR}/lib/kdl-config.sh"

[[ $# -eq 1 ]] || {
    printf 'Usage: bash ./tests/acceptance/verify-crm-customer-care.sh <config.kdl>\n' >&2
    exit 2
}
kdl_config_init "$1"
[[ "$(kdl_require schema_version)" == "1" ]] || exit 2

BASE_URL="$(kdl_require endpoint.base_url)"
TENANT_ID="$(kdl_positive_integer endpoint.tenant_id)"
USERNAME="$(kdl_require account.username)"
PASSWORD="$(kdl_require account.password)"
MYSQL_CONTAINER="$(kdl_require mysql.container)"
MYSQL_USER="$(kdl_require mysql.user)"
MYSQL_PASSWORD="$(kdl_require mysql.password)"
MYSQL_DATABASE="$(kdl_require mysql.database)"
[[ "$BASE_URL" =~ ^https?://[^[:space:]]+$ ]] || exit 2

mysql_exec() {
    podman exec "$MYSQL_CONTAINER" mysql "-u${MYSQL_USER}" "-p${MYSQL_PASSWORD}" \
        "--database=${MYSQL_DATABASE}" --default-character-set=utf8mb4 -Nse "$1"
}

login_payload="$(jq -n --arg username "$USERNAME" --arg password "$PASSWORD" \
    '{username:$username,password:$password,captchaVerification:""}')"
login_response="$(curl --noproxy '*' --fail --silent --show-error \
    --header 'Content-Type: application/json' --header "tenant-id: ${TENANT_ID}" \
    --data "$login_payload" "${BASE_URL}/system/auth/login")"
jq -e '.code == 0 and (.data.accessToken | length > 0)' >/dev/null <<< "$login_response"
TOKEN="$(jq -r '.data.accessToken' <<< "$login_response")"

api() {
    curl --noproxy '*' --fail --silent --show-error \
        --header "Authorization: Bearer ${TOKEN}" --header "tenant-id: ${TENANT_ID}" "$@"
}

permission_info="$(api "${BASE_URL}/system/auth/get-permission-info")"
jq -e '.code == 0 and
    any(.data.permissions[]; . == "crm:customer-care:query") and
    any(.data.permissions[]; . == "crm:customer-care:update") and
    any(.data.permissions[]; . == "crm:customer-care:delete")' >/dev/null <<< "$permission_info"

schema_state="$(mysql_exec "SELECT CONCAT(is_nullable,':',data_type) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='crm_customer_care_plan' AND column_name='event_month_day'; SELECT data_type FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='crm_customer_care_plan' AND column_name='follow_up_days';")"
grep -q '^YES:varchar$' <<< "$schema_state"
grep -q '^int$' <<< "$schema_state"

contact_row="$(mysql_exec "SELECT c.id,c.customer_id,COALESCE(DATE_FORMAT(c.birthday,'%Y-%m-%d'),'NULL') FROM crm_contact c JOIN crm_customer customer ON customer.id=c.customer_id AND customer.tenant_id=c.tenant_id AND customer.deleted=b'0' WHERE c.tenant_id=${TENANT_ID} AND c.deleted=b'0' ORDER BY c.id LIMIT 1;")"
IFS=$'\t' read -r CONTACT_ID CUSTOMER_ID ORIGINAL_BIRTHDAY <<< "$contact_row"
[[ "$CONTACT_ID" =~ ^[1-9][0-9]*$ && "$CUSTOMER_ID" =~ ^[1-9][0-9]*$ ]]

nonce="$(date +%s%N)"
today="$(date +%F)"
month_day="$(date +%m-%d)"
plan_id=''
follow_up_id=''
cleanup() {
    mysql_exec "UPDATE crm_contact SET birthday=$(if [[ "$ORIGINAL_BIRTHDAY" == 'NULL' ]]; then printf 'NULL'; else printf "'%s'" "$ORIGINAL_BIRTHDAY"; fi) WHERE id=${CONTACT_ID} AND tenant_id=${TENANT_ID};" >/dev/null || true
    if [[ -n "$plan_id" && "$plan_id" =~ ^[1-9][0-9]*$ ]]; then
        mysql_exec "DELETE FROM crm_customer_care_record WHERE plan_id=${plan_id}; DELETE FROM crm_customer_care_plan WHERE id=${plan_id};" >/dev/null || true
    fi
    if [[ -n "$follow_up_id" && "$follow_up_id" =~ ^[1-9][0-9]*$ ]]; then
        mysql_exec "DELETE FROM crm_customer_care_record WHERE plan_id=${follow_up_id}; DELETE FROM crm_customer_care_plan WHERE id=${follow_up_id};" >/dev/null || true
    fi
}
trap cleanup EXIT

birthday_payload="$(jq -n --arg code "CARE-BIRTHDAY-${nonce}" --arg name "生日关怀验收-${nonce}" \
    '{code:$code,name:$name,ruleType:1,channel:1,smsTemplateCode:"care-birthday",enabled:false}')"
create_response="$(api --header 'Content-Type: application/json' --data "$birthday_payload" \
    "${BASE_URL}/crm/marketing/care/plan/save")"
jq -e '.code == 0 and (.data | numbers)' >/dev/null <<< "$create_response"
plan_id="$(jq -r '.data' <<< "$create_response")"

detail="$(api --get --data-urlencode "id=${plan_id}" "${BASE_URL}/crm/marketing/care/plan/get")"
jq -e --argjson id "$plan_id" '.code == 0 and .data.id == $id and .data.ruleType == 1 and
    .data.targetScope == "BIRTHDAY_CONTACTS" and .data.enabled == false' >/dev/null <<< "$detail"

holiday_payload="$(jq -n --argjson id "$plan_id" --arg code "CARE-BIRTHDAY-${nonce}" \
    --arg name "节日关怀验收-${nonce}" --arg eventMonthDay "$month_day" \
    '{id:$id,code:$code,name:$name,ruleType:2,eventMonthDay:$eventMonthDay,channel:2,mailTemplateCode:"care-holiday",enabled:false}')"
update_response="$(api --header 'Content-Type: application/json' --data "$holiday_payload" \
    "${BASE_URL}/crm/marketing/care/plan/save")"
jq -e --argjson id "$plan_id" '.code == 0 and .data == $id' >/dev/null <<< "$update_response"

status_response="$(api --request PUT --header 'Content-Type: application/json' \
    --data "$(jq -n --argjson id "$plan_id" '{id:$id,enabled:true}')" \
    "${BASE_URL}/crm/marketing/care/plan/status")"
jq -e '.code == 0 and .data == true' >/dev/null <<< "$status_response"
enabled_delete="$(api --request DELETE --get --data-urlencode "id=${plan_id}" \
    "${BASE_URL}/crm/marketing/care/plan/delete")"
jq -e '.code == 1020014020' >/dev/null <<< "$enabled_delete"
api --request PUT --header 'Content-Type: application/json' \
    --data "$(jq -n --argjson id "$plan_id" '{id:$id,enabled:false}')" \
    "${BASE_URL}/crm/marketing/care/plan/status" >/dev/null

mysql_exec "UPDATE crm_contact SET birthday='${today}' WHERE id=${CONTACT_ID} AND tenant_id=${TENANT_ID}; INSERT INTO crm_customer_care_record (plan_id,customer_id,contact_id,event_date,channel,status,sent_at,creator,create_time,updater,update_time,deleted,tenant_id) VALUES (${plan_id},${CUSTOMER_ID},${CONTACT_ID},'${today}',2,50,NOW(),'care-acceptance',NOW(),'care-acceptance',NOW(),b'0',${TENANT_ID});" >/dev/null

birthday_page="$(api --get --data-urlencode 'pageNo=1' --data-urlencode 'pageSize=100' \
    --data-urlencode 'upcomingDays=1' "${BASE_URL}/crm/marketing/care/birthday/page")"
jq -e --argjson contact "$CONTACT_ID" '.code == 0 and any(.data.list[]; .contactId == $contact and .daysUntil == 0)' \
    >/dev/null <<< "$birthday_page"
record_page="$(api --get --data-urlencode 'pageNo=1' --data-urlencode 'pageSize=10' \
    --data-urlencode "planId=${plan_id}" "${BASE_URL}/crm/marketing/care/record/page")"
jq -e --argjson contact "$CONTACT_ID" '.code == 0 and .data.total == 1 and
    .data.list[0].contactId == $contact and (.data.list[0].planName | length > 0) and
    (.data.list[0].customerName | length > 0) and (.data.list[0].contactName | length > 0)' \
    >/dev/null <<< "$record_page"

delete_response="$(api --request DELETE --get --data-urlencode "id=${plan_id}" \
    "${BASE_URL}/crm/marketing/care/plan/delete")"
jq -e '.code == 0 and .data == true' >/dev/null <<< "$delete_response"
mysql_exec "DELETE FROM crm_customer_care_record WHERE plan_id=${plan_id}; DELETE FROM crm_customer_care_plan WHERE id=${plan_id};" >/dev/null
plan_id=''

follow_up_payload="$(jq -n --arg code "CARE-FOLLOWUP-${nonce}" --arg name "成交回访验收-${nonce}" \
    '{code:$code,name:$name,ruleType:3,followUpDays:7,channel:1,smsTemplateCode:"care-followup",enabled:false}')"
follow_up_response="$(api --header 'Content-Type: application/json' --data "$follow_up_payload" \
    "${BASE_URL}/crm/marketing/care/plan/save")"
jq -e '.code == 0 and (.data | numbers)' >/dev/null <<< "$follow_up_response"
follow_up_id="$(jq -r '.data' <<< "$follow_up_response")"
follow_up_detail="$(api --get --data-urlencode "id=${follow_up_id}" "${BASE_URL}/crm/marketing/care/plan/get")"
jq -e '.code == 0 and .data.ruleType == 3 and .data.followUpDays == 7 and .data.targetScope == "DEAL_CUSTOMERS"' \
    >/dev/null <<< "$follow_up_detail"
api --request DELETE --get --data-urlencode "id=${follow_up_id}" \
    "${BASE_URL}/crm/marketing/care/plan/delete" >/dev/null
mysql_exec "DELETE FROM crm_customer_care_plan WHERE id=${follow_up_id};" >/dev/null
follow_up_id=''

printf 'plan-create-get-update-status-delete=ok\n'
printf 'birthday-scope-and-record-names=ok\n'
printf 'post-deal-follow-up-contract=ok\n'
printf 'runtime-cleanup=ok\n'
