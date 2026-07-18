#!/usr/bin/env bash

set -Eeuo pipefail
trap 'printf "CRM campaign acceptance failed at line %s.\n" "$LINENO" >&2' ERR

PODMAN_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/../.." && pwd)"
source "${PODMAN_DIR}/lib/yaml-config.sh"

[[ $# -eq 1 ]] || {
    printf 'Usage: bash ./tests/acceptance/verify-crm-campaign.sh <config.yaml>\n' >&2
    exit 2
}
yaml_config_init "$1"
[[ "$(yaml_require schema_version)" == "1" ]] || exit 2

BASE_URL="$(yaml_require endpoint.base_url)"
TENANT_ID="$(yaml_positive_integer endpoint.tenant_id)"
USERNAME="$(yaml_require account.username)"
PASSWORD="$(yaml_require account.password)"
[[ "$BASE_URL" =~ ^https?://[^[:space:]]+$ ]] || exit 2
for command in curl jq; do command -v "$command" >/dev/null; done

LOGIN_PAYLOAD="$(jq -n --arg username "$USERNAME" --arg password "$PASSWORD" \
    '{username:$username,password:$password,captchaVerification:""}')"
LOGIN_RESPONSE="$(curl --noproxy '*' --fail --silent --show-error \
    --header 'Content-Type: application/json' --header "tenant-id: ${TENANT_ID}" \
    --data "$LOGIN_PAYLOAD" "${BASE_URL}/system/auth/login")"
jq -e '.code == 0 and (.data.accessToken | length > 0)' >/dev/null <<< "$LOGIN_RESPONSE"
TOKEN="$(jq -r '.data.accessToken' <<< "$LOGIN_RESPONSE")"

api() {
    curl --noproxy '*' --fail --silent --show-error \
        --header "Authorization: Bearer ${TOKEN}" --header "tenant-id: ${TENANT_ID}" "$@"
}

permission_info="$(api "${BASE_URL}/system/auth/get-permission-info")"
jq -e '.code == 0 and (.data.user.id | numbers) and
    any(.data.permissions[]; . == "crm:marketing-campaign:delete")' >/dev/null <<< "$permission_info"
OWNER_USER_ID="$(jq -r '.data.user.id' <<< "$permission_info")"

campaign_id=''
cleanup() {
    if [[ -n "$campaign_id" ]]; then
        api --request DELETE --get --data-urlencode "id=${campaign_id}" \
            "${BASE_URL}/crm/marketing/campaign/delete" >/dev/null || true
    fi
}
trap cleanup EXIT

nonce="$(date +%s%N)"
start_time="$(date +%s000)"
end_time="$((start_time + 86400000))"
create_payload="$(jq -n --arg code "ACCEPT-${nonce}" --arg name "营销活动运行验收-${nonce}" \
    --argjson ownerUserId "$OWNER_USER_ID" --argjson startTime "$start_time" --argjson endTime "$end_time" \
    '{code:$code,name:$name,ownerUserId:$ownerUserId,startTime:$startTime,endTime:$endTime,
      budgetAmount:100.25,targetLeadCount:10,targetCustomerCount:3,description:"运行验收",relations:[]}')"
create_response="$(api --header 'Content-Type: application/json' --data "$create_payload" \
    "${BASE_URL}/crm/marketing/campaign/save")"
jq -e '.code == 0 and (.data | numbers)' >/dev/null <<< "$create_response"
campaign_id="$(jq -r '.data' <<< "$create_response")"

detail="$(api --get --data-urlencode "id=${campaign_id}" "${BASE_URL}/crm/marketing/campaign/get")"
jq -e --argjson id "$campaign_id" --argjson owner "$OWNER_USER_ID" \
    '.code == 0 and .data.id == $id and .data.ownerUserId == $owner and
     (.data.ownerUserName | length > 0) and .data.status == 10 and .data.budgetAmount == 100.25 and
     .data.targetLeadCount == 10 and .data.targetCustomerCount == 3 and
     (.data.startTime | numbers) and (.data.endTime | numbers) and (.data.relations | length == 0)' \
    >/dev/null <<< "$detail"

update_payload="$(jq '.name += "-已编辑" | .description = "运行验收编辑" |
    {id,code,name,ownerUserId,startTime,endTime,budgetAmount,targetLeadCount,targetCustomerCount,description,relations}' \
    <<< "$(jq '.data' <<< "$detail")")"
update_response="$(api --header 'Content-Type: application/json' --data "$update_payload" \
    "${BASE_URL}/crm/marketing/campaign/save")"
jq -e --argjson id "$campaign_id" '.code == 0 and .data == $id' >/dev/null <<< "$update_response"

delete_response="$(api --request DELETE --get --data-urlencode "id=${campaign_id}" \
    "${BASE_URL}/crm/marketing/campaign/delete")"
jq -e '.code == 0 and .data == true' >/dev/null <<< "$delete_response"
campaign_id=''

printf 'owner-from-session=ok\n'
printf 'delete-permission=ok\n'
printf 'create-get-update-delete=ok\n'
printf 'date-contract=ok\n'
