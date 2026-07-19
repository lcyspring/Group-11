#!/usr/bin/env bash

set -Eeuo pipefail
trap 'printf "CRM orphan approval verification failed at line %s.\n" "$LINENO" >&2' ERR

PODMAN_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/../.." && pwd)"
source "${PODMAN_DIR}/lib/kdl-config.sh"

[[ $# -eq 1 ]] || {
    printf 'Usage: bash ./tests/acceptance/verify-crm-orphan-approvals.sh <config.kdl>\n' >&2
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

mysql_exec() {
    podman exec --env "MYSQL_PWD=${MYSQL_PASSWORD}" "$MYSQL_CONTAINER" mysql "-u${MYSQL_USER}" \
        "--database=${MYSQL_DATABASE}" --default-character-set=utf8mb4 -Nse "$1"
}

LOGIN_PAYLOAD="$(jq -n --arg username "$USERNAME" --arg password "$PASSWORD" \
    '{username:$username,password:$password,captchaVerification:""}')"
LOGIN_RESPONSE="$(curl --noproxy '*' --fail --silent --show-error \
    --header 'Content-Type: application/json' --header "tenant-id: ${TENANT_ID}" \
    --data "$LOGIN_PAYLOAD" "${BASE_URL}/system/auth/login")"
jq -e '.code == 0 and (.data.accessToken | length > 0)' >/dev/null <<< "$LOGIN_RESPONSE"
TOKEN="$(jq -r '.data.accessToken' <<< "$LOGIN_RESPONSE")"

api_get() {
    curl --noproxy '*' --fail --silent --show-error --get \
        --header "Authorization: Bearer ${TOKEN}" --header "tenant-id: ${TENANT_ID}" "$@"
}

contract_page="$(api_get --data-urlencode 'pageNo=1' --data-urlencode 'pageSize=100' \
    --data-urlencode 'auditStatus=10' "${BASE_URL}/crm/contract/page")"
receivable_page="$(api_get --data-urlencode 'pageNo=1' --data-urlencode 'pageSize=100' \
    --data-urlencode 'auditStatus=10' "${BASE_URL}/crm/receivable/page")"
jq -e '.code == 0 and (.data.list | type == "array")' >/dev/null <<< "$contract_page"
jq -e '.code == 0 and (.data.list | type == "array")' >/dev/null <<< "$receivable_page"

while IFS= read -r process_id; do
    [[ -n "$process_id" && "$process_id" != "null" ]]
    [[ "$process_id" =~ ^[A-Za-z0-9._:-]+$ ]]
    [[ "$(mysql_exec "SELECT COUNT(*) FROM ACT_HI_PROCINST WHERE ID_='${process_id}';")" == "1" ]]
done < <(jq -r '.data.list[].processInstanceId' <<< "$contract_page")
while IFS= read -r process_id; do
    [[ -n "$process_id" && "$process_id" != "null" ]]
    [[ "$process_id" =~ ^[A-Za-z0-9._:-]+$ ]]
    [[ "$(mysql_exec "SELECT COUNT(*) FROM ACT_HI_PROCINST WHERE ID_='${process_id}';")" == "1" ]]
done < <(jq -r '.data.list[].processInstanceId' <<< "$receivable_page")

orphan_count="$(mysql_exec "SELECT SUM(orphan_count) FROM (
  SELECT COUNT(*) orphan_count FROM crm_contract x LEFT JOIN ACT_HI_PROCINST p ON p.ID_=x.process_instance_id WHERE x.deleted=b'0' AND x.audit_status=10 AND p.ID_ IS NULL
  UNION ALL SELECT COUNT(*) FROM crm_receivable x LEFT JOIN ACT_HI_PROCINST p ON p.ID_=x.process_instance_id WHERE x.deleted=b'0' AND x.audit_status=10 AND p.ID_ IS NULL
  UNION ALL SELECT COUNT(*) FROM crm_receivable_refund x LEFT JOIN ACT_HI_PROCINST p ON p.ID_=x.process_instance_id WHERE x.deleted=b'0' AND x.audit_status=10 AND p.ID_ IS NULL
  UNION ALL SELECT COUNT(*) FROM crm_reimbursement x LEFT JOIN ACT_HI_PROCINST p ON p.ID_=x.process_instance_id WHERE x.deleted=b'0' AND x.audit_status=10 AND p.ID_ IS NULL
  UNION ALL SELECT COUNT(*) FROM crm_contract_amendment x LEFT JOIN ACT_HI_PROCINST p ON p.ID_=x.process_instance_id WHERE x.deleted=b'0' AND x.audit_status=10 AND p.ID_ IS NULL
) totals;")"
[[ "$orphan_count" == "0" ]]

printf 'contract-pending-api=%s\n' "$(jq '.data.total' <<< "$contract_page")"
printf 'receivable-pending-api=%s\n' "$(jq '.data.total' <<< "$receivable_page")"
printf 'orphan-approval-count=0\n'
printf 'approval-detail-targets=valid\n'
