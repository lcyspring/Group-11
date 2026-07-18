#!/usr/bin/env bash

set -Eeuo pipefail
trap 'printf "CRM outreach acceptance failed at line %s.\n" "$LINENO" >&2' ERR

PODMAN_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/../.." && pwd)"
source "${PODMAN_DIR}/lib/kdl-config.sh"

[[ $# -eq 1 ]] || {
    printf 'Usage: bash ./tests/acceptance/verify-crm-outreach.sh <config.kdl>\n' >&2
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
ALTERNATE_CREATOR_ID="$(kdl_positive_integer acceptance.alternate_creator_id)"
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
    any(.data.permissions[]; . == "crm:marketing-outreach:update") and
    any(.data.permissions[]; . == "crm:marketing-outreach:review") and
    any(.data.permissions[]; . == "crm:marketing-outreach:send") and
    any(.data.permissions[]; . == "crm:marketing-outreach:consent")' >/dev/null <<< "$permission_info"

customer_id="$(mysql_exec "SELECT id FROM crm_customer WHERE tenant_id=${TENANT_ID} AND deleted=b'0' AND mobile IS NOT NULL AND mobile<>'' AND NOT EXISTS (SELECT 1 FROM crm_contact c WHERE c.tenant_id=crm_customer.tenant_id AND c.customer_id=crm_customer.id AND c.primary_contact=b'1' AND c.deleted=b'0') ORDER BY id LIMIT 1;")"
[[ "$customer_id" =~ ^[1-9][0-9]*$ ]]
target_options="$(api "${BASE_URL}/crm/marketing/outreach/broadcast/target-options")"
jq -e --argjson customer "$customer_id" \
    '.code == 0 and any(.data.customers[]; .id == $customer) and (.data.contacts | type == "array")' \
    >/dev/null <<< "$target_options"

nonce="$(date +%s%N)"
source_name="outreach-acceptance-${nonce}"
draft_id=''
broadcast_id=''
cleanup() {
    if [[ -n "$draft_id" && "$draft_id" =~ ^[1-9][0-9]*$ ]]; then
        mysql_exec "DELETE FROM crm_marketing_broadcast_recipient WHERE broadcast_id=${draft_id}; DELETE FROM crm_marketing_broadcast WHERE id=${draft_id};" >/dev/null || true
    fi
    if [[ -n "$broadcast_id" && "$broadcast_id" =~ ^[1-9][0-9]*$ ]]; then
        mysql_exec "DELETE FROM crm_marketing_broadcast_recipient WHERE broadcast_id=${broadcast_id}; DELETE FROM crm_marketing_broadcast WHERE id=${broadcast_id};" >/dev/null || true
    fi
    mysql_exec "DELETE FROM crm_marketing_consent WHERE tenant_id=${TENANT_ID} AND source='${source_name}';" >/dev/null || true
}
trap cleanup EXIT

consent_payload="$(jq -n --argjson customerId "$customer_id" --arg source "$source_name" \
    '{customerId:$customerId,channel:1,status:1,source:$source}')"
consent_response="$(api --header 'Content-Type: application/json' --data "$consent_payload" \
    "${BASE_URL}/crm/marketing/outreach/consent/save")"
jq -e '.code == 0 and .data == true' >/dev/null <<< "$consent_response"

create_payload="$(jq -n --arg name "群发草稿验收-${nonce}" --argjson customerId "$customer_id" \
    '{name:$name,channel:1,smsTemplateCode:"crm-outreach-acceptance",templateParams:"{\"name\":\"acceptance\"}",customerIds:[$customerId],contactIds:[]}')"
create_response="$(api --header 'Content-Type: application/json' --data "$create_payload" \
    "${BASE_URL}/crm/marketing/outreach/broadcast/save")"
jq -e '.code == 0 and (.data | numbers)' >/dev/null <<< "$create_response"
draft_id="$(jq -r '.data' <<< "$create_response")"

detail="$(api --get --data-urlencode "id=${draft_id}" "${BASE_URL}/crm/marketing/outreach/broadcast/get")"
jq -e --argjson id "$draft_id" --argjson customer "$customer_id" \
    '.code == 0 and .data.id == $id and .data.status == 10 and .data.channel == 1 and
     (.data.customerIds | index($customer)) != null and .data.validCount == 1' >/dev/null <<< "$detail"

update_payload="$(jq '.data | .name += "-已编辑" |
    {id,campaignId,name,channel,smsTemplateCode,mailTemplateCode,templateParams,scheduledAt,customerIds,contactIds}' <<< "$detail")"
update_response="$(api --header 'Content-Type: application/json' --data "$update_payload" \
    "${BASE_URL}/crm/marketing/outreach/broadcast/save")"
jq -e --argjson id "$draft_id" '.code == 0 and .data == $id' >/dev/null <<< "$update_response"

delete_response="$(api --request DELETE --get --data-urlencode "id=${draft_id}" \
    "${BASE_URL}/crm/marketing/outreach/broadcast/delete")"
jq -e '.code == 0 and .data == true' >/dev/null <<< "$delete_response"
draft_id=''

create_response="$(api --header 'Content-Type: application/json' --data "$create_payload" \
    "${BASE_URL}/crm/marketing/outreach/broadcast/save")"
jq -e '.code == 0 and (.data | numbers)' >/dev/null <<< "$create_response"
broadcast_id="$(jq -r '.data' <<< "$create_response")"
mysql_exec "UPDATE crm_marketing_broadcast SET creator='${ALTERNATE_CREATOR_ID}' WHERE id=${broadcast_id} AND tenant_id=${TENANT_ID};" >/dev/null

submit_response="$(api --request PUT --get --data-urlencode "id=${broadcast_id}" \
    "${BASE_URL}/crm/marketing/outreach/broadcast/submit-review")"
jq -e '.code == 0 and .data == true' >/dev/null <<< "$submit_response"

reject_payload="$(jq -n --argjson id "$broadcast_id" '{id:$id,comment:"验收驳回后修订"}')"
reject_response="$(api --request PUT --header 'Content-Type: application/json' --data "$reject_payload" \
    "${BASE_URL}/crm/marketing/outreach/broadcast/reject")"
jq -e '.code == 0 and .data == true' >/dev/null <<< "$reject_response"

rejected_detail="$(api --get --data-urlencode "id=${broadcast_id}" "${BASE_URL}/crm/marketing/outreach/broadcast/get")"
jq -e '.code == 0 and .data.status == 30 and .data.reviewComment == "验收驳回后修订"' >/dev/null <<< "$rejected_detail"
revision_payload="$(jq '.data | .name += "-修订" |
    {id,campaignId,name,channel,smsTemplateCode,mailTemplateCode,templateParams,scheduledAt,customerIds,contactIds}' <<< "$rejected_detail")"
revision_response="$(api --header 'Content-Type: application/json' --data "$revision_payload" \
    "${BASE_URL}/crm/marketing/outreach/broadcast/save")"
jq -e '.code == 0' >/dev/null <<< "$revision_response"

api --request PUT --get --data-urlencode "id=${broadcast_id}" \
    "${BASE_URL}/crm/marketing/outreach/broadcast/submit-review" >/dev/null
approve_payload="$(jq -n --argjson id "$broadcast_id" '{id:$id,comment:"验收通过"}')"
approve_response="$(api --request PUT --header 'Content-Type: application/json' --data "$approve_payload" \
    "${BASE_URL}/crm/marketing/outreach/broadcast/approve")"
jq -e '.code == 0 and .data == true' >/dev/null <<< "$approve_response"
send_response="$(api --request PUT --get --data-urlencode "id=${broadcast_id}" \
    "${BASE_URL}/crm/marketing/outreach/broadcast/send")"
jq -e '.code == 0 and .data == true' >/dev/null <<< "$send_response"

sent_detail="$(api --get --data-urlencode "id=${broadcast_id}" "${BASE_URL}/crm/marketing/outreach/broadcast/get")"
jq -e '.code == 0 and .data.status == 60 and .data.sentCount == 1 and .data.failedCount == 0' >/dev/null <<< "$sent_detail"
recipients="$(api --get --data-urlencode "broadcastId=${broadcast_id}" --data-urlencode 'pageNo=1' \
    --data-urlencode 'pageSize=10' "${BASE_URL}/crm/marketing/outreach/broadcast/recipients")"
jq -e --argjson customer "$customer_id" \
    '.code == 0 and .data.total == 1 and .data.list[0].customerId == $customer and .data.list[0].status == 50' \
    >/dev/null <<< "$recipients"

printf 'draft-create-get-update-delete=ok\n'
printf 'reject-revise-resubmit-approve=ok\n'
printf 'send-recipient-results=ok\n'
printf 'runtime-cleanup=ok\n'
