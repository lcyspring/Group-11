#!/usr/bin/env bash
# Reconcile orphan CRM receivables across the ledger, reference-aware list/detail APIs and statistics.

set -Eeuo pipefail
trap 'printf "CRM receivable reference-integrity acceptance failed at line %s.\n" "$LINENO" >&2' ERR

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
source "${SCRIPT_DIR}/lib/yaml-config.sh"

[[ $# -eq 1 ]] || {
    printf 'Usage: bash ./verify-crm-receivable-reference-integrity.sh <config.yaml>\n' >&2
    exit 2
}
yaml_config_init "$1"
[[ "$(yaml_require schema_version)" == "1" ]] || {
    printf 'Unsupported schema_version.\n' >&2
    exit 2
}

BASE_URL="$(yaml_require endpoint.base_url)"
TENANT_ID="$(yaml_positive_integer endpoint.tenant_id)"
USERNAME="$(yaml_require account.username)"
PASSWORD="$(yaml_require account.password)"
MYSQL_CONTAINER="$(yaml_require mysql.container)"
MYSQL_USER="$(yaml_require mysql.user)"
MYSQL_PASSWORD="$(yaml_require mysql.password)"
MYSQL_DATABASE="$(yaml_require mysql.database)"
RECEIVABLE_ID="$(yaml_positive_integer acceptance.receivable_id)"
RECEIVABLE_NO="$(yaml_require acceptance.receivable_no)"
CUSTOMER_ID="$(yaml_positive_integer acceptance.customer_id)"
CONTRACT_ID="$(yaml_positive_integer acceptance.contract_id)"
OWNER_USER_ID="$(yaml_positive_integer acceptance.owner_user_id)"
DEPARTMENT_ID="$(yaml_positive_integer acceptance.department_id)"
EXPECTED_AMOUNT="$(yaml_require acceptance.expected_amount)"
EXPECTED_AUDIT_STATUS="$(yaml_positive_integer acceptance.expected_audit_status)"
EXPECTED_ORPHAN_COUNT="$(yaml_positive_integer acceptance.expected_orphan_count)"
START_TIME="$(yaml_require acceptance.start_time)"
END_TIME="$(yaml_require acceptance.end_time)"

[[ "$BASE_URL" =~ ^https?://[^[:space:]]+$ ]] || {
    printf 'Invalid endpoint.base_url.\n' >&2
    exit 2
}
[[ "$MYSQL_CONTAINER" =~ ^[a-zA-Z0-9_.-]+$ && "$MYSQL_USER" =~ ^[a-zA-Z0-9_.-]+$ \
    && "$MYSQL_DATABASE" =~ ^[a-zA-Z0-9_]+$ && "$RECEIVABLE_NO" =~ ^[a-zA-Z0-9._-]+$ ]] || {
    printf 'Invalid MySQL identifier or receivable number.\n' >&2
    exit 2
}
[[ "$EXPECTED_AMOUNT" =~ ^[0-9]+([.][0-9]{1,6})?$ ]] || {
    printf 'acceptance.expected_amount must be a non-negative decimal with at most six places.\n' >&2
    exit 2
}
timestamp_pattern='^[0-9]{4}-[0-9]{2}-[0-9]{2} [0-9]{2}:[0-9]{2}:[0-9]{2}$'
[[ "$START_TIME" =~ $timestamp_pattern && "$END_TIME" =~ $timestamp_pattern \
    && "$START_TIME" < "$END_TIME" ]] || {
    printf 'Acceptance timestamps must be ordered YYYY-MM-DD HH:MM:SS values.\n' >&2
    exit 2
}
for command in curl jq podman awk; do
    command -v "$command" >/dev/null || {
        printf 'Missing command: %s\n' "$command" >&2
        exit 1
    }
done

mysql_exec() {
    podman exec "$MYSQL_CONTAINER" mysql "-u${MYSQL_USER}" "-p${MYSQL_PASSWORD}" \
        "--database=${MYSQL_DATABASE}" --default-character-set=utf8mb4 -Nse "$1"
}

normalize_decimal() {
    awk -v value="$1" 'BEGIN { printf "%.6f", value + 0 }'
}

login_response="$(curl --noproxy '*' --fail --silent --show-error --retry 5 --retry-connrefused --retry-delay 1 \
    --header 'Content-Type: application/json' --header "tenant-id: ${TENANT_ID}" \
    --data "$(jq -n --arg username "$USERNAME" --arg password "$PASSWORD" \
        '{username:$username,password:$password,captchaVerification:""}')" \
    "${BASE_URL}/system/auth/login")"
jq -e '.code == 0 and (.data.accessToken | length > 0)' >/dev/null <<< "$login_response" || {
    printf 'CRM acceptance login failed: %s\n' "$(jq -r '.msg' <<< "$login_response")" >&2
    exit 1
}
TOKEN="$(jq -r '.data.accessToken' <<< "$login_response")"

api_get() {
    local path="$1"
    shift
    local args=(--noproxy '*' --silent --show-error --retry 5 --retry-connrefused --retry-delay 1 --get
        --header "Authorization: Bearer ${TOKEN}" --header "tenant-id: ${TENANT_ID}")
    while [[ $# -gt 0 ]]; do
        args+=(--data-urlencode "$1")
        shift
    done
    curl "${args[@]}" "${BASE_URL}${path}"
}

ROW_SNAPSHOT_BEFORE="$(mysql_exec "SELECT CONCAT_WS('|',id,no,customer_id,contract_id,owner_user_id,
  CAST(price AS CHAR),audit_status,DATE_FORMAT(return_time,'%Y-%m-%d %H:%i:%s'),deleted)
  FROM crm_receivable WHERE tenant_id=${TENANT_ID} AND id=${RECEIVABLE_ID} LIMIT 1;")"
[[ -n "$ROW_SNAPSHOT_BEFORE" ]] || {
    printf 'Configured receivable %s does not exist.\n' "$RECEIVABLE_ID" >&2
    exit 1
}

EXPECTED_ROW_PREFIX="${RECEIVABLE_ID}|${RECEIVABLE_NO}|${CUSTOMER_ID}|${CONTRACT_ID}|${OWNER_USER_ID}|"
[[ "$ROW_SNAPSHOT_BEFORE" == "${EXPECTED_ROW_PREFIX}"* ]] || {
    printf 'Configured receivable identity does not match the database row: %s\n' "$ROW_SNAPSHOT_BEFORE" >&2
    exit 1
}
ROW_AUDIT_STATUS="$(mysql_exec "SELECT audit_status FROM crm_receivable
  WHERE tenant_id=${TENANT_ID} AND id=${RECEIVABLE_ID} AND deleted=b'0' LIMIT 1;")"
[[ "$ROW_AUDIT_STATUS" == "$EXPECTED_AUDIT_STATUS" ]] || {
    printf 'Expected audit status %s, got %s.\n' "$EXPECTED_AUDIT_STATUS" "$ROW_AUDIT_STATUS" >&2
    exit 1
}
ROW_AMOUNT="$(mysql_exec "SELECT CAST(price AS DECIMAL(24,6)) FROM crm_receivable
  WHERE tenant_id=${TENANT_ID} AND id=${RECEIVABLE_ID} AND deleted=b'0' LIMIT 1;")"
[[ "$(normalize_decimal "$ROW_AMOUNT")" == "$(normalize_decimal "$EXPECTED_AMOUNT")" ]] || {
    printf 'Expected receivable amount %s, got %s.\n' "$EXPECTED_AMOUNT" "$ROW_AMOUNT" >&2
    exit 1
}

ORPHAN_COUNT="$(mysql_exec "SELECT COUNT(*) FROM crm_receivable receivable
  LEFT JOIN crm_customer customer ON customer.id=receivable.customer_id
    AND customer.tenant_id=receivable.tenant_id AND customer.deleted=b'0'
  LEFT JOIN crm_contract contract ON contract.id=receivable.contract_id
    AND contract.tenant_id=receivable.tenant_id AND contract.deleted=b'0'
  WHERE receivable.tenant_id=${TENANT_ID} AND receivable.deleted=b'0'
    AND (customer.id IS NULL OR contract.id IS NULL);")"
[[ "$ORPHAN_COUNT" == "$EXPECTED_ORPHAN_COUNT" ]] || {
    printf 'Expected %s orphan receivables, got %s.\n' "$EXPECTED_ORPHAN_COUNT" "$ORPHAN_COUNT" >&2
    exit 1
}
MISSING_REFERENCES="$(mysql_exec "SELECT CONCAT(IF(customer.id IS NULL,1,0),'|',IF(contract.id IS NULL,1,0))
  FROM crm_receivable receivable
  LEFT JOIN crm_customer customer ON customer.id=receivable.customer_id
    AND customer.tenant_id=receivable.tenant_id AND customer.deleted=b'0'
  LEFT JOIN crm_contract contract ON contract.id=receivable.contract_id
    AND contract.tenant_id=receivable.tenant_id AND contract.deleted=b'0'
  WHERE receivable.tenant_id=${TENANT_ID} AND receivable.id=${RECEIVABLE_ID} LIMIT 1;")"
[[ "$MISSING_REFERENCES" == "1|1" ]] || {
    printf 'Expected both source references to be missing, got %s.\n' "$MISSING_REFERENCES" >&2
    exit 1
}

PAGE_BROKEN="$(api_get '/crm/receivable/page' 'pageNo=1' 'pageSize=100' 'sceneType=1' \
    "no=${RECEIVABLE_NO}" 'referenceStatus=30')"
jq -e --argjson id "$RECEIVABLE_ID" --argjson customer "$CUSTOMER_ID" --argjson contract "$CONTRACT_ID" '
  .code == 0 and .data.total == 1 and (.data.list | length) == 1
  and .data.list[0].id == $id and .data.list[0].referenceStatus == 30
  and .data.list[0].customerId == $customer and .data.list[0].contractId == $contract
  and .data.list[0].customerName == null and .data.list[0].contract == null
' >/dev/null <<< "$PAGE_BROKEN" || {
    printf 'Broken-reference page filter did not return the archived receivable: %s\n' "$PAGE_BROKEN" >&2
    exit 1
}

DETAIL="$(api_get '/crm/receivable/get' "id=${RECEIVABLE_ID}")"
jq -e --argjson id "$RECEIVABLE_ID" --argjson customer "$CUSTOMER_ID" --argjson contract "$CONTRACT_ID" '
  .code == 0 and .data.id == $id and .data.referenceStatus == 30
  and .data.customerId == $customer and .data.contractId == $contract
  and .data.customerName == null and .data.contract == null
' >/dev/null <<< "$DETAIL" || {
    printf 'Archived receivable detail is not readable with raw reference IDs: %s\n' "$DETAIL" >&2
    exit 1
}

PAGE_VALID="$(api_get '/crm/receivable/page' 'pageNo=1' 'pageSize=100' 'sceneType=1' \
    "no=${RECEIVABLE_NO}" 'referenceStatus=0')"
jq -e '.code == 0 and .data.total == 0 and (.data.list | length) == 0' >/dev/null <<< "$PAGE_VALID" || {
    printf 'Valid-reference filter unexpectedly included the orphan receivable: %s\n' "$PAGE_VALID" >&2
    exit 1
}

DB_LEDGER_TOTAL="$(mysql_exec "SELECT CAST(IFNULL(SUM(receivable.price),0) AS DECIMAL(24,6))
  FROM crm_receivable receivable
  WHERE receivable.tenant_id=${TENANT_ID} AND receivable.deleted=b'0'
    AND receivable.audit_status=${EXPECTED_AUDIT_STATUS} AND receivable.owner_user_id=${OWNER_USER_ID}
    AND receivable.return_time BETWEEN '${START_TIME}' AND '${END_TIME}';")"
DB_CUSTOMER_TOTAL="$(mysql_exec "SELECT CAST(IFNULL(SUM(receivable.price),0) AS DECIMAL(24,6))
  FROM crm_receivable receivable
  INNER JOIN crm_customer customer ON customer.id=receivable.customer_id
    AND customer.tenant_id=receivable.tenant_id AND customer.deleted=b'0'
  INNER JOIN crm_contract contract ON contract.id=receivable.contract_id
    AND contract.tenant_id=receivable.tenant_id AND contract.customer_id=receivable.customer_id
    AND contract.deleted=b'0'
  WHERE receivable.tenant_id=${TENANT_ID} AND receivable.deleted=b'0'
    AND receivable.audit_status=${EXPECTED_AUDIT_STATUS} AND receivable.owner_user_id=${OWNER_USER_ID}
    AND receivable.return_time BETWEEN '${START_TIME}' AND '${END_TIME}';")"
DB_ORPHAN_TOTAL="$(mysql_exec "SELECT CAST((${DB_LEDGER_TOTAL})-(${DB_CUSTOMER_TOTAL}) AS DECIMAL(24,6));")"
[[ "$(normalize_decimal "$DB_ORPHAN_TOTAL")" == "$(normalize_decimal "$EXPECTED_AMOUNT")" ]] || {
    printf 'Ledger/customer-statistics difference expected %s, got %s.\n' "$EXPECTED_AMOUNT" "$DB_ORPHAN_TOTAL" >&2
    exit 1
}

common_statistics_params=("deptId=${DEPARTMENT_ID}" "userId=${OWNER_USER_ID}"
    "times=${START_TIME}" "times=${END_TIME}")
CUSTOMER_SUMMARY="$(api_get '/crm/statistics-customer/get-customer-summary-by-user' \
    "${common_statistics_params[@]}" 'interval=1')"
API_CUSTOMER_TOTAL="$(jq -er --argjson owner "$OWNER_USER_ID" '
  if .code != 0 then error(.msg) else ([.data[] | select(.ownerUserId == $owner) | .receivablePrice] | add // 0) end
' <<< "$CUSTOMER_SUMMARY")"
[[ "$(normalize_decimal "$API_CUSTOMER_TOTAL")" == "$(normalize_decimal "$DB_CUSTOMER_TOTAL")" ]] || {
    printf 'Customer statistics mismatch: API %s, MySQL %s.\n' "$API_CUSTOMER_TOTAL" "$DB_CUSTOMER_TOTAL" >&2
    exit 1
}

PERFORMANCE="$(api_get '/crm/statistics-performance/get-receivable-price-performance' \
    "${common_statistics_params[@]}")"
API_LEDGER_TOTAL="$(jq -er '
  if .code != 0 then error(.msg) else ([.data[].currentMonthCount] | add // 0) end
' <<< "$PERFORMANCE")"
[[ "$(normalize_decimal "$API_LEDGER_TOTAL")" == "$(normalize_decimal "$DB_LEDGER_TOTAL")" ]] || {
    printf 'Employee performance ledger mismatch: API %s, MySQL %s.\n' "$API_LEDGER_TOTAL" "$DB_LEDGER_TOTAL" >&2
    exit 1
}

ROW_SNAPSHOT_AFTER="$(mysql_exec "SELECT CONCAT_WS('|',id,no,customer_id,contract_id,owner_user_id,
  CAST(price AS CHAR),audit_status,DATE_FORMAT(return_time,'%Y-%m-%d %H:%i:%s'),deleted)
  FROM crm_receivable WHERE tenant_id=${TENANT_ID} AND id=${RECEIVABLE_ID} LIMIT 1;")"
[[ "$ROW_SNAPSHOT_AFTER" == "$ROW_SNAPSHOT_BEFORE" ]] || {
    printf 'Acceptance changed or rebound the archived receivable. Before=%s After=%s\n' \
        "$ROW_SNAPSHOT_BEFORE" "$ROW_SNAPSHOT_AFTER" >&2
    exit 1
}

printf 'CRM receivable reference-integrity acceptance passed: id=%s status=30 raw=%s/%s ledger=%s customer-statistics=%s preserved-difference=%s.\n' \
    "$RECEIVABLE_ID" "$CUSTOMER_ID" "$CONTRACT_ID" "$DB_LEDGER_TOTAL" "$DB_CUSTOMER_TOTAL" "$DB_ORPHAN_TOTAL"
