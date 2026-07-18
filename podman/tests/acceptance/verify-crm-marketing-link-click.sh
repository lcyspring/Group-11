#!/usr/bin/env bash

set -Eeuo pipefail
trap 'printf "CRM marketing link click acceptance failed at line %s.\n" "$LINENO" >&2' ERR

PODMAN_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/../.." && pwd)"
# shellcheck source=../../lib/yaml-config.sh
source "${PODMAN_DIR}/lib/yaml-config.sh"

[[ $# -eq 1 ]] || {
    printf 'Usage: bash ./tests/acceptance/verify-crm-marketing-link-click.sh <config.yaml>\n' >&2
    exit 2
}
yaml_config_init "$1"
[[ "$(yaml_require schema_version)" == "1" ]] || exit 2

ADMIN_BASE_URL="$(yaml_require endpoint.admin_base_url)"
PUBLIC_BASE_URL="$(yaml_require endpoint.public_base_url)"
TARGET_URL="$(yaml_require endpoint.allowed_target_url)"
TENANT_ID="$(yaml_positive_integer endpoint.tenant_id)"
USERNAME="$(yaml_require account.username)"
PASSWORD="$(yaml_require account.password)"
MYSQL_CONTAINER="$(yaml_require mysql.container)"
MYSQL_USER="$(yaml_require mysql.user)"
MYSQL_PASSWORD="$(yaml_require mysql.password)"
MYSQL_DATABASE="$(yaml_require mysql.database)"

simple_url_pattern='^https?://[A-Za-z0-9._:/-]+$'
target_url_pattern='^https?://[A-Za-z0-9._:/?#=&%+-]+$'
[[ "$ADMIN_BASE_URL" =~ $simple_url_pattern ]] || exit 2
[[ "$PUBLIC_BASE_URL" =~ $simple_url_pattern ]] || exit 2
[[ "$TARGET_URL" =~ $target_url_pattern ]] || exit 2

mysql_exec() {
    podman exec -e "MYSQL_PWD=${MYSQL_PASSWORD}" "$MYSQL_CONTAINER" mysql \
        "--user=${MYSQL_USER}" "--database=${MYSQL_DATABASE}" \
        --default-character-set=utf8mb4 -Nse "$1"
}

login_payload="$(jq -n --arg username "$USERNAME" --arg password "$PASSWORD" \
    '{username:$username,password:$password,captchaVerification:""}')"
login_response="$(curl --noproxy '*' --fail --silent --show-error \
    --header 'Content-Type: application/json' --header "tenant-id: ${TENANT_ID}" \
    --data "$login_payload" "${ADMIN_BASE_URL}/system/auth/login")"
jq -e '.code == 0 and (.data.accessToken | length > 0)' >/dev/null <<< "$login_response"
TOKEN="$(jq -r '.data.accessToken' <<< "$login_response")"

api() {
    curl --noproxy '*' --fail --silent --show-error \
        --header "Authorization: Bearer ${TOKEN}" --header "tenant-id: ${TENANT_ID}" "$@"
}

customer_id="$(mysql_exec "SELECT id FROM crm_customer WHERE tenant_id=${TENANT_ID} AND deleted=b'0' ORDER BY id LIMIT 1;")"
[[ "$customer_id" =~ ^[1-9][0-9]*$ ]]

blocked_payload="$(jq -n --argjson customerId "$customer_id" \
    '{name:"点击白名单负向验收",channel:1,smsTemplateCode:"uat",templateParams:"{}",customerIds:[$customerId],contactIds:[],links:[{code:"unsafe",name:"不允许目标",targetUrl:"https://blocked.invalid/phishing"}]}')"
blocked_response="$(api --header 'Content-Type: application/json' --data "$blocked_payload" \
    "${ADMIN_BASE_URL}/crm/marketing/outreach/broadcast/save")"
jq -e '.code == 1020014023' >/dev/null <<< "$blocked_response"

nonce="$(date +%s%N)"
[[ "$nonce" =~ ^[0-9]+$ ]]
name="link-click-acceptance-${nonce}"
valid_token="$(printf 'valid-%s' "$nonce" | sha256sum | cut -c1-48)"
failed_token="$(printf 'failed-%s' "$nonce" | sha256sum | cut -c1-48)"
[[ "$valid_token" =~ ^[a-f0-9]{48}$ && "$failed_token" =~ ^[a-f0-9]{48}$ ]]

broadcast_id=''
draft_id=''
link_id=''
sent_recipient_id=''
failed_recipient_id=''
cleanup() {
    if [[ -n "$draft_id" && "$draft_id" =~ ^[1-9][0-9]*$ ]]; then
        mysql_exec "DELETE FROM crm_marketing_link_recipient WHERE link_id IN (SELECT id FROM crm_marketing_link WHERE broadcast_id=${draft_id}); DELETE FROM crm_marketing_link WHERE broadcast_id=${draft_id}; DELETE FROM crm_marketing_broadcast_recipient WHERE broadcast_id=${draft_id}; DELETE FROM crm_marketing_broadcast WHERE id=${draft_id};" >/dev/null || true
    fi
    if [[ -n "$broadcast_id" && "$broadcast_id" =~ ^[1-9][0-9]*$ ]]; then
        mysql_exec "DELETE FROM crm_marketing_link_recipient WHERE link_id IN (SELECT id FROM crm_marketing_link WHERE broadcast_id=${broadcast_id}); DELETE FROM crm_marketing_link WHERE broadcast_id=${broadcast_id}; DELETE FROM crm_marketing_broadcast_recipient WHERE broadcast_id=${broadcast_id}; DELETE FROM crm_marketing_broadcast WHERE id=${broadcast_id};" >/dev/null || true
    fi
}
trap cleanup EXIT

valid_draft_payload="$(jq -n --arg name "链接草稿验收-${nonce}" --arg target "$TARGET_URL" \
    --argjson customerId "$customer_id" \
    '{name:$name,channel:1,smsTemplateCode:"uat",templateParams:"{}",customerIds:[$customerId],contactIds:[],links:[{code:"landing",name:"验收落地页",targetUrl:$target}]}')"
valid_draft_response="$(api --header 'Content-Type: application/json' --data "$valid_draft_payload" \
    "${ADMIN_BASE_URL}/crm/marketing/outreach/broadcast/save")"
jq -e '.code == 0 and (.data | numbers)' >/dev/null <<< "$valid_draft_response"
draft_id="$(jq -r '.data' <<< "$valid_draft_response")"
draft_detail="$(api --get --data-urlencode "id=${draft_id}" \
    "${ADMIN_BASE_URL}/crm/marketing/outreach/broadcast/get")"
jq -e --arg target "$TARGET_URL" \
    '.code == 0 and (.data.links | length) == 1 and .data.links[0].code == "landing" and .data.links[0].targetUrl == $target' \
    >/dev/null <<< "$draft_detail"
delete_response="$(api --request DELETE --get --data-urlencode "id=${draft_id}" \
    "${ADMIN_BASE_URL}/crm/marketing/outreach/broadcast/delete")"
jq -e '.code == 0 and .data == true' >/dev/null <<< "$delete_response"
[[ "$(mysql_exec "SELECT COUNT(*) FROM crm_marketing_link WHERE broadcast_id=${draft_id};")" == 0 ]]
draft_id=''

mysql_exec "INSERT INTO crm_marketing_broadcast (name,channel,sms_template_code,template_params,status,total_count,valid_count,suppressed_count,sent_count,failed_count,creator,updater,deleted,tenant_id) VALUES ('${name}',1,'uat','{}',60,2,2,0,1,1,'1','1',b'0',${TENANT_ID});" >/dev/null
broadcast_id="$(mysql_exec "SELECT id FROM crm_marketing_broadcast WHERE tenant_id=${TENANT_ID} AND name='${name}' AND deleted=b'0';")"
[[ "$broadcast_id" =~ ^[1-9][0-9]*$ ]]

mysql_exec "INSERT INTO crm_marketing_broadcast_recipient (broadcast_id,customer_id,channel,mobile,status,provider_log_id,attempt_count,sent_at,last_attempt_at,delivery_status,creator,updater,deleted,tenant_id) VALUES (${broadcast_id},${customer_id},1,'13800000000',20,900001,1,NOW(),NOW(),20,'1','1',b'0',${TENANT_ID}),(${broadcast_id},${customer_id}+1000000000,1,'13800000001',30,900002,1,NULL,NOW(),30,'1','1',b'0',${TENANT_ID});" >/dev/null
sent_recipient_id="$(mysql_exec "SELECT id FROM crm_marketing_broadcast_recipient WHERE broadcast_id=${broadcast_id} AND status=20;")"
failed_recipient_id="$(mysql_exec "SELECT id FROM crm_marketing_broadcast_recipient WHERE broadcast_id=${broadcast_id} AND status=30;")"
[[ "$sent_recipient_id" =~ ^[1-9][0-9]*$ && "$failed_recipient_id" =~ ^[1-9][0-9]*$ ]]

mysql_exec "INSERT INTO crm_marketing_link (broadcast_id,code,name,target_url,creator,updater,deleted,tenant_id) VALUES (${broadcast_id},'landing','验收落地页','${TARGET_URL}','1','1',b'0',${TENANT_ID});" >/dev/null
link_id="$(mysql_exec "SELECT id FROM crm_marketing_link WHERE broadcast_id=${broadcast_id} AND code='landing';")"
[[ "$link_id" =~ ^[1-9][0-9]*$ ]]
mysql_exec "INSERT INTO crm_marketing_link_recipient (link_id,recipient_id,tracking_token,click_count,creator,updater,deleted,tenant_id) VALUES (${link_id},${sent_recipient_id},'${valid_token}',0,'1','1',b'0',${TENANT_ID}),(${link_id},${failed_recipient_id},'${failed_token}',0,'1','1',b'0',${TENANT_ID});" >/dev/null

headers="$(mktemp)"
trap 'rm -f -- "$headers"; cleanup' EXIT
status="$(curl --noproxy '*' --silent --show-error --output /dev/null --dump-header "$headers" \
    --write-out '%{http_code}' "${PUBLIC_BASE_URL}/app-api/crm/marketing/click/${valid_token}")"
location="$(awk 'tolower($1)=="location:" {sub(/\r$/, "", $2); print $2}' "$headers")"
[[ "$status" == 302 && "$location" == "$TARGET_URL" ]]
first_click="$(mysql_exec "SELECT CONCAT(click_count,'|',first_clicked_at) FROM crm_marketing_link_recipient WHERE tracking_token='${valid_token}';")"
[[ "$first_click" =~ ^1\|.+$ ]]

status="$(curl --noproxy '*' --silent --show-error --output /dev/null \
    --write-out '%{http_code}' "${PUBLIC_BASE_URL}/app-api/crm/marketing/click/${valid_token}")"
[[ "$status" == 302 ]]
second_click="$(mysql_exec "SELECT CONCAT(click_count,'|',first_clicked_at) FROM crm_marketing_link_recipient WHERE tracking_token='${valid_token}';")"
[[ "${second_click#*|}" == "${first_click#*|}" && "$second_click" =~ ^2\|.+$ ]]

failed_status="$(curl --noproxy '*' --silent --show-error --output /dev/null \
    --write-out '%{http_code}' "${PUBLIC_BASE_URL}/app-api/crm/marketing/click/${failed_token}")"
invalid_status="$(curl --noproxy '*' --silent --show-error --output /dev/null \
    --write-out '%{http_code}' "${PUBLIC_BASE_URL}/app-api/crm/marketing/click/not-a-valid-token")"
[[ "$failed_status" == 404 && "$invalid_status" == 404 ]]

summary="$(api --get --data-urlencode "id=${broadcast_id}" \
    "${ADMIN_BASE_URL}/crm/marketing/outreach/broadcast/delivery-summary")"
jq -e '.code == 0 and .data.trackedRecipientCount == 2 and .data.uniqueClickCount == 1 and
    .data.totalClickCount == 2 and .data.uniqueClickRate == 50.00 and (.data.links | length) == 1 and
    .data.links[0].trackedRecipientCount == 2 and .data.links[0].uniqueClickCount == 1 and
    .data.links[0].totalClickCount == 2 and .data.links[0].uniqueClickRate == 50.00' \
    >/dev/null <<< "$summary"

printf 'allowed-target-302=ok\n'
printf 'repeat-click-atomic-count=ok\n'
printf 'failed-and-invalid-token-404=ok\n'
printf 'blocked-target-validation=ok\n'
printf 'draft-link-save-detail-delete=ok\n'
printf 'unique-and-total-summary=ok\n'
printf 'runtime-cleanup=ok\n'
