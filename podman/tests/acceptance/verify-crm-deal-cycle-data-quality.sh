#!/usr/bin/env bash
# Reconcile persisted negative CRM deal-cycle samples with all four statistics APIs.

set -Eeuo pipefail

PODMAN_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/../.." && pwd)"
source "${PODMAN_DIR}/lib/yaml-config.sh"

[[ $# -eq 1 ]] || {
    printf 'Usage: bash ./tests/acceptance/verify-crm-deal-cycle-data-quality.sh <config.yaml>\n' >&2
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
DEPARTMENT_ID="$(yaml_positive_integer acceptance.department_id)"
OWNER_USER_ID="$(yaml_positive_integer acceptance.owner_user_id)"
START_TIME="$(yaml_require acceptance.start_time)"
END_TIME="$(yaml_require acceptance.end_time)"
MIN_NEGATIVE_COUNT="$(yaml_positive_integer acceptance.minimum_negative_sample_count)"

[[ "$BASE_URL" =~ ^https?://[^[:space:]]+$ ]] || {
    printf 'Invalid endpoint.base_url.\n' >&2
    exit 2
}
[[ "$MYSQL_CONTAINER" =~ ^[a-zA-Z0-9_.-]+$ && "$MYSQL_USER" =~ ^[a-zA-Z0-9_.-]+$ \
    && "$MYSQL_DATABASE" =~ ^[a-zA-Z0-9_]+$ ]] || {
    printf 'Invalid MySQL identifier.\n' >&2
    exit 2
}
timestamp_pattern='^[0-9]{4}-[0-9]{2}-[0-9]{2} [0-9]{2}:[0-9]{2}:[0-9]{2}$'
[[ "$START_TIME" =~ $timestamp_pattern && "$END_TIME" =~ $timestamp_pattern \
    && "$START_TIME" < "$END_TIME" ]] || {
    printf 'Acceptance timestamps must be ordered YYYY-MM-DD HH:MM:SS values.\n' >&2
    exit 2
}
for command in curl jq podman; do
    command -v "$command" >/dev/null || {
        printf 'Missing command: %s\n' "$command" >&2
        exit 1
    }
done

mysql_exec() {
    podman exec "$MYSQL_CONTAINER" mysql "-u${MYSQL_USER}" "-p${MYSQL_PASSWORD}" \
        "--database=${MYSQL_DATABASE}" --default-character-set=utf8mb4 -Nse "$1"
}

OWNER_DEPARTMENT_ID="$(mysql_exec "SELECT dept_id FROM system_users
  WHERE tenant_id=${TENANT_ID} AND id=${OWNER_USER_ID} AND deleted=b'0' LIMIT 1;")"
[[ "$OWNER_DEPARTMENT_ID" == "$DEPARTMENT_ID" ]] || {
    printf 'Configured owner %s belongs to department %s, not %s.\n' \
        "$OWNER_USER_ID" "${OWNER_DEPARTMENT_ID:-missing}" "$DEPARTMENT_ID" >&2
    exit 1
}

login_response="$(curl --noproxy '*' --fail --silent --show-error \
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
    local args=(--noproxy '*' --fail --silent --show-error --get
        --header "Authorization: Bearer ${TOKEN}" --header "tenant-id: ${TENANT_ID}")
    while [[ $# -gt 0 ]]; do
        args+=(--data-urlencode "$1")
        shift
    done
    curl "${args[@]}" "${BASE_URL}${path}"
}

first_contract_join="JOIN crm_contract contract ON contract.id = (
  SELECT first_contract.id FROM crm_contract first_contract
   WHERE first_contract.customer_id = customer.id
     AND first_contract.deleted = 0 AND first_contract.audit_status = 20
   ORDER BY first_contract.order_date IS NULL, first_contract.order_date, first_contract.id LIMIT 1
)"
negative_where="customer.tenant_id = ${TENANT_ID} AND customer.deleted = 0
  AND customer.owner_user_id = ${OWNER_USER_ID}
  AND customer.create_time BETWEEN '${START_TIME}' AND '${END_TIME}'
  AND contract.order_date < customer.create_time"

DB_NEGATIVE_COUNT="$(mysql_exec "SELECT COUNT(*) FROM crm_customer customer ${first_contract_join}
  WHERE ${negative_where};")"
[[ "$DB_NEGATIVE_COUNT" =~ ^[0-9]+$ && "$DB_NEGATIVE_COUNT" -ge "$MIN_NEGATIVE_COUNT" ]] || {
    printf 'Expected at least %s negative deal-cycle samples, got %s.\n' \
        "$MIN_NEGATIVE_COUNT" "$DB_NEGATIVE_COUNT" >&2
    exit 1
}
DB_CYCLES="$(mysql_exec "SELECT GROUP_CONCAT(TIMESTAMPDIFF(DAY, customer.create_time, contract.order_date)
  ORDER BY customer.id SEPARATOR ',') FROM crm_customer customer ${first_contract_join}
  WHERE ${negative_where};")"

common_params=("deptId=${DEPARTMENT_ID}" "userId=${OWNER_USER_ID}" 'interval=1'
    "times=${START_TIME}" "times=${END_TIME}")
DATE_RESPONSE="$(api_get '/crm/statistics-customer/get-customer-deal-cycle-by-date' "${common_params[@]}")"
USER_RESPONSE="$(api_get '/crm/statistics-customer/get-customer-deal-cycle-by-user' "${common_params[@]}")"
AREA_RESPONSE="$(api_get '/crm/statistics-customer/get-customer-deal-cycle-by-area' "${common_params[@]}")"
PRODUCT_RESPONSE="$(api_get '/crm/statistics-customer/get-customer-deal-cycle-by-product' "${common_params[@]}")"
CATALOG_RESPONSE="$(api_get '/crm/statistics-metadata/catalog' 'scope=customer')"

jq -e --argjson expected "$DB_NEGATIVE_COUNT" '
  .code == 0
  and (([.data[].negativeSampleCount] | add // 0) == $expected)
  and any(.data[]; .negativeSampleCount > 0 and .customerDealCycle < 0)
' >/dev/null <<< "$DATE_RESPONSE" || {
    printf 'Date deal-cycle API did not preserve negative samples: %s\n' "$DATE_RESPONSE" >&2
    exit 1
}
jq -e --argjson owner "$OWNER_USER_ID" --argjson expected "$DB_NEGATIVE_COUNT" '
  .code == 0 and any(.data[]; .ownerUserId == $owner and .negativeSampleCount == $expected)
' >/dev/null <<< "$USER_RESPONSE" || {
    printf 'User deal-cycle API negative count mismatch: %s\n' "$USER_RESPONSE" >&2
    exit 1
}
jq -e --argjson expected "$DB_NEGATIVE_COUNT" '
  .code == 0 and (([.data[].negativeSampleCount] | add // 0) == $expected)
' >/dev/null <<< "$AREA_RESPONSE" || {
    printf 'Area deal-cycle API negative count mismatch: %s\n' "$AREA_RESPONSE" >&2
    exit 1
}
jq -e '.code == 0 and any(.data[]; .negativeSampleCount > 0)' >/dev/null <<< "$PRODUCT_RESPONSE" || {
    printf 'Product deal-cycle API did not expose negative samples: %s\n' "$PRODUCT_RESPONSE" >&2
    exit 1
}
jq -e '.code == 0 and any(.data.metrics[];
  .code == "customer.deal-cycle" and (.formula | contains("negativeSampleCount")))' \
  >/dev/null <<< "$CATALOG_RESPONSE" || {
    printf 'Deal-cycle YAML metadata is absent from the runtime catalog: %s\n' "$CATALOG_RESPONSE" >&2
    exit 1
}

printf 'CRM deal-cycle data-quality acceptance passed: owner=%s db-negative=%s cycles=%s; date/user/area/product/catalog APIs reconciled.\n' \
    "$OWNER_USER_ID" "$DB_NEGATIVE_COUNT" "$DB_CYCLES"
