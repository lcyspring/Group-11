#!/usr/bin/env bash

set -Eeuo pipefail
trap 'printf "CRM receivable candidate acceptance failed at line %s.\n" "$LINENO" >&2' ERR

PODMAN_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/../.." && pwd)"
source "${PODMAN_DIR}/lib/yaml-config.sh"

[[ $# -eq 1 ]] || {
    printf 'Usage: bash ./tests/acceptance/verify-crm-receivable-candidates.sh <config.yaml>\n' >&2
    exit 2
}
yaml_config_init "$1"
[[ "$(yaml_require schema_version)" == "1" ]] || exit 2

BASE_URL="$(yaml_require endpoint.base_url)"
TENANT_ID="$(yaml_positive_integer endpoint.tenant_id)"
USERNAME="$(yaml_require account.username)"
PASSWORD="$(yaml_require account.password)"
EXPECT_CRM_ADMIN="$(yaml_bool account.expect_crm_admin)"
MYSQL_CONTAINER="$(yaml_require mysql.container)"
MYSQL_USER="$(yaml_require mysql.user)"
MYSQL_PASSWORD="$(yaml_require mysql.password)"
MYSQL_DATABASE="$(yaml_require mysql.database)"

[[ "$BASE_URL" =~ ^https?://[^[:space:]]+$ ]] || exit 2
for command in curl jq podman; do
    command -v "$command" >/dev/null
done

mysql_exec() {
    podman exec "$MYSQL_CONTAINER" mysql "-u${MYSQL_USER}" "-p${MYSQL_PASSWORD}" \
        "--database=${MYSQL_DATABASE}" --default-character-set=utf8mb4 -Nse "$1"
}

LOGIN_PAYLOAD="$(jq -n --arg username "$USERNAME" --arg password "$PASSWORD" \
    '{username:$username,password:$password,captchaVerification:""}')"
login_response="$(curl --noproxy '*' --fail --silent --show-error \
    --header 'Content-Type: application/json' --header "tenant-id: ${TENANT_ID}" \
    --data "$LOGIN_PAYLOAD" \
    "${BASE_URL}/system/auth/login")"
jq -e '.code == 0 and (.data.accessToken | length > 0)' >/dev/null <<< "$login_response"
TOKEN="$(jq -r '.data.accessToken' <<< "$login_response")"

api_get() {
    curl --noproxy '*' --fail --silent --show-error --get \
        --header "Authorization: Bearer ${TOKEN}" --header "tenant-id: ${TENANT_ID}" \
        "$@"
}

response="$(api_get "${BASE_URL}/crm/contract/receivable-candidates")"
jq -e '.code == 0 and (.data | type == "array")' >/dev/null <<< "$response"
candidate_count="$(jq '.data | length' <<< "$response")"

if [[ "$EXPECT_CRM_ADMIN" == "true" ]]; then
    expected_count="$(mysql_exec "SELECT COUNT(*) FROM crm_contract
      WHERE tenant_id=${TENANT_ID} AND deleted=b'0' AND audit_status=20;")"
    [[ "$candidate_count" == "$expected_count" ]] || {
        printf 'Expected %s approved admin candidates, got %s.\n' "$expected_count" "$candidate_count" >&2
        exit 1
    }
fi

if (( candidate_count > 0 )); then
    contract_id="$(jq -r '.data[0].id' <<< "$response")"
    customer_id="$(jq -r '.data[0].customerId' <<< "$response")"
    expected="$(mysql_exec "SELECT CONCAT_WS('|',contract.no,contract.name,contract.customer_id,
      customer.name,CAST(contract.total_price AS DECIMAL(24,2)),
      CAST(COALESCE(SUM(CASE WHEN receivable.audit_status IN (10,20) THEN receivable.price ELSE 0 END),0) AS DECIMAL(24,2)),
      CAST(GREATEST(contract.total_price-COALESCE(SUM(CASE WHEN receivable.audit_status IN (10,20)
        THEN receivable.price ELSE 0 END),0),0) AS DECIMAL(24,2)))
      FROM crm_contract contract
      JOIN crm_customer customer ON customer.id=contract.customer_id AND customer.deleted=b'0'
      LEFT JOIN crm_receivable receivable ON receivable.contract_id=contract.id AND receivable.deleted=b'0'
      WHERE contract.id=${contract_id} AND contract.tenant_id=${TENANT_ID} AND contract.deleted=b'0'
        AND contract.audit_status=20 GROUP BY contract.id,customer.id;")"
    actual="$(jq -r '.data[0] | [.no,.name,.customerId,.customerName,
      (.totalPrice|tonumber*100|round/100),(.totalReceivablePrice|tonumber*100|round/100),
      (.remainingReceivablePrice|tonumber*100|round/100)] | join("|")' <<< "$response")"
    expected_normalized="$(awk -F'|' '{printf "%s|%s|%s|%s|%.2f|%.2f|%.2f",$1,$2,$3,$4,$5,$6,$7}' <<< "$expected")"
    actual_normalized="$(awk -F'|' '{printf "%s|%s|%s|%s|%.2f|%.2f|%.2f",$1,$2,$3,$4,$5,$6,$7}' <<< "$actual")"
    [[ "$actual_normalized" == "$expected_normalized" ]] || {
        printf 'Candidate mismatch. expected=%s actual=%s\n' "$expected_normalized" "$actual_normalized" >&2
        exit 1
    }

    filtered="$(api_get --data-urlencode "customerId=${customer_id}" \
        "${BASE_URL}/crm/contract/receivable-candidates")"
    jq -e --argjson customerId "$customer_id" \
        '.code == 0 and all(.data[]; .customerId == $customerId)' >/dev/null <<< "$filtered"
fi

printf 'candidate-count=%s\n' "$candidate_count"
printf 'approved-only=ok\n'
printf 'reserved-amount-formula=ok\n'
printf 'customer-filter=ok\n'
