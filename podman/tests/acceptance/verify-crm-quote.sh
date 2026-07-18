#!/usr/bin/env bash
# Real API acceptance for CRM quote versioning. The only CLI argument is an explicit KDL path.

set -Eeuo pipefail

PODMAN_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/../.." && pwd)"
source "${PODMAN_DIR}/lib/kdl-config.sh"

[[ $# -eq 1 ]] || { printf 'Usage: bash ./tests/acceptance/verify-crm-quote.sh <config.kdl>\n' >&2; exit 2; }
kdl_config_init "$1"
[[ "$(kdl_require schema_version)" == "1" ]] || { printf 'Unsupported schema_version.\n' >&2; exit 2; }

BASE_URL="$(kdl_require endpoint.base_url)"
TENANT_ID="$(kdl_positive_integer endpoint.tenant_id)"
USERNAME="$(kdl_require account.username)"
PASSWORD="$(kdl_require account.password)"
OWNER_USER_ID="$(kdl_positive_integer acceptance.owner_user_id)"
CUSTOMER_ID="$(kdl_positive_integer acceptance.customer_id)"
PRODUCT_ID="$(kdl_positive_integer acceptance.product_id)"
STATUS_TYPE_ID="$(kdl_positive_integer acceptance.status_type_id)"
CURRENCY_CODE="$(kdl_require acceptance.currency_code)"
BASE_CURRENCY="$(kdl_require acceptance.expected_base_currency)"
EXCHANGE_RATE="$(kdl_require acceptance.expected_exchange_rate)"
CATALOG_PRICE="$(kdl_require acceptance.expected_catalog_price)"
PRODUCT_NAME="$(kdl_require acceptance.expected_product_name)"
V1_DISCOUNT="$(kdl_require acceptance.v1_discount_percent)"
V1_PRICE="$(kdl_require acceptance.v1_business_price)"
V1_COUNT="$(kdl_require acceptance.v1_count)"
V1_TAX="$(kdl_require acceptance.v1_tax_rate_percent)"
V1_SUBTOTAL="$(kdl_require acceptance.v1_expected_subtotal)"
V1_NET="$(kdl_require acceptance.v1_expected_net_amount)"
V1_TAX_AMOUNT="$(kdl_require acceptance.v1_expected_tax_amount)"
V1_GROSS="$(kdl_require acceptance.v1_expected_gross_amount)"
V1_BASE_GROSS="$(kdl_require acceptance.v1_expected_base_gross_amount)"
V2_DISCOUNT="$(kdl_require acceptance.v2_discount_percent)"
V2_PRICE="$(kdl_require acceptance.v2_business_price)"
V2_COUNT="$(kdl_require acceptance.v2_count)"
V2_TAX="$(kdl_require acceptance.v2_tax_rate_percent)"
V2_SUBTOTAL="$(kdl_require acceptance.v2_expected_subtotal)"
V2_DISCOUNT_AMOUNT="$(kdl_require acceptance.v2_expected_discount_amount)"
V2_NET="$(kdl_require acceptance.v2_expected_net_amount)"
V2_TAX_AMOUNT="$(kdl_require acceptance.v2_expected_tax_amount)"
V2_GROSS="$(kdl_require acceptance.v2_expected_gross_amount)"
V2_BASE_GROSS="$(kdl_require acceptance.v2_expected_base_gross_amount)"
LOCKED_EDIT_DISCOUNT="$(kdl_require acceptance.locked_edit_discount_percent)"
LOCKED_EDIT_PRICE="$(kdl_require acceptance.locked_edit_business_price)"
LOCKED_EDIT_COUNT="$(kdl_require acceptance.locked_edit_count)"
LOCKED_EDIT_TAX="$(kdl_require acceptance.locked_edit_tax_rate_percent)"
FORGED_PRODUCT_PRICE="$(kdl_require acceptance.forged_product_price)"
FORGED_CONTRACT_ROW_ID="$(kdl_positive_integer acceptance.forged_contract_row_id)"
FORGED_CONTRACT_PRICE="$(kdl_require acceptance.forged_contract_price)"
FORGED_CONTRACT_COUNT="$(kdl_require acceptance.forged_contract_count)"
FORGED_CONTRACT_DISCOUNT="$(kdl_require acceptance.forged_contract_discount_percent)"
MYSQL_CONTAINER="$(kdl_require mysql.container)"
MYSQL_USER="$(kdl_require mysql.user)"
MYSQL_PASSWORD="$(kdl_require mysql.password)"
MYSQL_DATABASE="$(kdl_require mysql.database)"

[[ "$BASE_URL" =~ ^https?://[^[:space:]]+$ ]] || { printf 'Invalid endpoint.base_url.\n' >&2; exit 2; }
[[ "$CURRENCY_CODE" =~ ^[A-Z]{3}$ && "$BASE_CURRENCY" =~ ^[A-Z]{3}$ ]] || {
    printf 'Currency codes must be uppercase ISO-4217 values.\n' >&2; exit 2;
}
NUMERIC_VALUES=("$EXCHANGE_RATE" "$CATALOG_PRICE" "$V1_DISCOUNT" "$V1_PRICE" "$V1_COUNT" "$V1_TAX"
    "$V1_SUBTOTAL" "$V1_NET" "$V1_TAX_AMOUNT" "$V1_GROSS" "$V1_BASE_GROSS"
    "$V2_DISCOUNT" "$V2_PRICE" "$V2_COUNT" "$V2_TAX" "$V2_SUBTOTAL" "$V2_DISCOUNT_AMOUNT"
    "$V2_NET" "$V2_TAX_AMOUNT" "$V2_GROSS" "$V2_BASE_GROSS" "$LOCKED_EDIT_DISCOUNT"
    "$LOCKED_EDIT_PRICE" "$LOCKED_EDIT_COUNT" "$LOCKED_EDIT_TAX" "$FORGED_PRODUCT_PRICE"
    "$FORGED_CONTRACT_PRICE" "$FORGED_CONTRACT_COUNT" "$FORGED_CONTRACT_DISCOUNT")
for value in "${NUMERIC_VALUES[@]}"; do
    [[ "$value" =~ ^[0-9]+([.][0-9]+)?$ ]] || {
        printf 'Acceptance numeric value is invalid: %s\n' "$value" >&2; exit 2;
    }
done
[[ "$MYSQL_CONTAINER" =~ ^[a-zA-Z0-9_.-]+$ && "$MYSQL_USER" =~ ^[a-zA-Z0-9_.-]+$ && "$MYSQL_DATABASE" =~ ^[a-zA-Z0-9_]+$ ]] || {
    printf 'Invalid MySQL identifier in KDL.\n' >&2; exit 2;
}
for command in curl jq podman date; do
    command -v "$command" >/dev/null || { printf 'Missing command: %s\n' "$command" >&2; exit 1; }
done

RUN_ID="$(date +%s)"
BUSINESS_NAME="crm-quote-acceptance-${RUN_ID}"
CONTRACT_NAME="crm-quote-contract-${RUN_ID}"
BUSINESS_ID=''
CONTRACT_ID=''

mysql_exec() {
    podman exec "$MYSQL_CONTAINER" mysql "-u${MYSQL_USER}" "-p${MYSQL_PASSWORD}" \
        "--database=${MYSQL_DATABASE}" --default-character-set=utf8mb4 -Nse "$1"
}

cleanup() {
    set +e
    if [[ -n "$BUSINESS_ID" ]]; then
        mysql_exec "DELETE FROM crm_contract_change_record WHERE tenant_id=${TENANT_ID} AND contract_id=${CONTRACT_ID:-0};
          DELETE FROM crm_contract_product WHERE tenant_id=${TENANT_ID} AND contract_id=${CONTRACT_ID:-0};
          DELETE FROM crm_permission WHERE tenant_id=${TENANT_ID} AND biz_type=5 AND biz_id=${CONTRACT_ID:-0};
          DELETE FROM crm_contract WHERE tenant_id=${TENANT_ID} AND id=${CONTRACT_ID:-0};
          DELETE FROM crm_business_quote_action_record WHERE tenant_id=${TENANT_ID} AND quote_id IN
            (SELECT id FROM crm_business_quote WHERE tenant_id=${TENANT_ID} AND business_id=${BUSINESS_ID});
          DELETE FROM crm_business_quote_item WHERE tenant_id=${TENANT_ID} AND quote_id IN
            (SELECT id FROM crm_business_quote WHERE tenant_id=${TENANT_ID} AND business_id=${BUSINESS_ID});
          DELETE FROM crm_business_quote WHERE tenant_id=${TENANT_ID} AND business_id=${BUSINESS_ID};
          DELETE FROM crm_business_product WHERE tenant_id=${TENANT_ID} AND business_id=${BUSINESS_ID};
          DELETE FROM crm_permission WHERE tenant_id=${TENANT_ID} AND biz_type=4 AND biz_id=${BUSINESS_ID};
          DELETE FROM crm_business WHERE tenant_id=${TENANT_ID} AND id=${BUSINESS_ID};" >/dev/null
    fi
}
trap cleanup EXIT

LOGIN_PAYLOAD="$(jq -n --arg username "$USERNAME" --arg password "$PASSWORD" \
    '{username:$username,password:$password,captchaVerification:""}')"
LOGIN_RESPONSE="$(curl --noproxy '*' --fail --silent --show-error --header 'Content-Type: application/json' \
    --header "tenant-id: ${TENANT_ID}" --data "$LOGIN_PAYLOAD" "${BASE_URL}/system/auth/login")"
jq -e '.code == 0 and (.data.accessToken | length > 0)' >/dev/null <<< "$LOGIN_RESPONSE" || {
    printf 'Login failed: %s\n' "$(jq -r '.msg' <<< "$LOGIN_RESPONSE")" >&2; exit 1;
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
        printf 'API %s %s failed: %s\n' "$1" "$2" "$(jq -r '.msg' <<< "$response")" >&2
        return 1
    }
    printf '%s' "$response"
}

expect_error() {
    local response
    response="$(api_raw "$@")"
    jq -e '.code != 0' >/dev/null <<< "$response" || {
        printf 'API %s %s unexpectedly succeeded.\n' "$1" "$2" >&2
        return 1
    }
}

POLICY="$(api GET '/crm/business-quote/policy')"
jq -e --arg currency "$CURRENCY_CODE" --arg base "$BASE_CURRENCY" --argjson rate "$EXCHANGE_RATE" \
    --argjson v1Tax "$V1_TAX" --argjson v2Tax "$V2_TAX" \
    '.data.baseCurrency == $base and .data.exchangeRatesToBase[$currency] == $rate
     and (.data.allowedTaxRates | index($v1Tax)) != null and (.data.allowedTaxRates | index($v2Tax)) != null' \
    >/dev/null <<< "$POLICY" || { printf 'KDL quote policy mismatch.\n' >&2; exit 1; }
CONTRACT_AMOUNT_COLUMN="$(mysql_exec "SELECT CONCAT(numeric_precision,':',numeric_scale)
  FROM information_schema.columns
  WHERE table_schema=DATABASE() AND table_name='crm_contract' AND column_name='total_price';")"
[[ "$CONTRACT_AMOUNT_COLUMN" == "24:6" ]] || {
    printf 'Contract amount precision mismatch: expected 24:6, got %s.\n' "$CONTRACT_AMOUNT_COLUMN" >&2
    exit 1
}

NOW_MS="$(( $(date +%s) * 1000 ))"
business_payload() {
    local discount="$1" price="$2" count="$3" tax="$4"
    jq -n --arg name "$BUSINESS_NAME" --arg currency "$CURRENCY_CODE" \
        --argjson customer "$CUSTOMER_ID" --argjson owner "$OWNER_USER_ID" \
        --argjson statusType "$STATUS_TYPE_ID" --argjson dealTime "$NOW_MS" \
        --argjson product "$PRODUCT_ID" --argjson discount "$discount" --argjson price "$price" \
        --argjson count "$count" --argjson tax "$tax" \
        --argjson forgedProductPrice "$FORGED_PRODUCT_PRICE" \
        '{name:$name,customerId:$customer,ownerUserId:$owner,statusTypeId:$statusType,
          dealTime:$dealTime,discountPercent:$discount,currencyCode:$currency,remark:"real API quote acceptance",
          products:[{productId:$product,productPrice:$forgedProductPrice,businessPrice:$price,count:$count,taxRatePercent:$tax}]}'
}

CREATE_PAYLOAD="$(business_payload "$V1_DISCOUNT" "$V1_PRICE" "$V1_COUNT" "$V1_TAX")"
BUSINESS_ID="$(api POST '/crm/business/create' "$CREATE_PAYLOAD" | jq -r '.data')"
V1="$(api GET "/crm/business-quote/current?businessId=${BUSINESS_ID}")"
V1_ID="$(jq -r '.data.id' <<< "$V1")"
jq -e --arg name "$PRODUCT_NAME" --arg currency "$CURRENCY_CODE" --argjson catalog "$CATALOG_PRICE" \
    --argjson subtotal "$V1_SUBTOTAL" --argjson net "$V1_NET" --argjson tax "$V1_TAX_AMOUNT" \
    --argjson gross "$V1_GROSS" --argjson baseGross "$V1_BASE_GROSS" --argjson count "$V1_COUNT" \
    '.data.versionNo == 1 and .data.status == 0 and .data.currencyCode == $currency
     and .data.subtotal == $subtotal and .data.netAmount == $net
     and .data.taxAmount == $tax and .data.grossAmount == $gross
     and .data.baseGrossAmount == $baseGross and .data.items[0].productNameSnapshot == $name
     and .data.items[0].listPrice == $catalog and .data.items[0].count == $count' \
    >/dev/null <<< "$V1" || { printf 'V1 snapshot or amount calculation mismatch.\n' >&2; exit 1; }

expect_error PUT '/crm/business/update-status' "$(jq -n --argjson id "$BUSINESS_ID" '{id:$id,endStatus:1}')"
LOCKED_V1="$(api PUT '/crm/business-quote/lock' \
    "$(jq -n --argjson id "$BUSINESS_ID" '{businessId:$id,remark:"客户确认第一版报价"}')")"
jq -e '.data.status == 10 and .data.lockedAt != null' >/dev/null <<< "$LOCKED_V1" || {
    printf 'V1 lock mismatch.\n' >&2; exit 1;
}

LOCKED_UPDATE="$(business_payload "$LOCKED_EDIT_DISCOUNT" "$LOCKED_EDIT_PRICE" "$LOCKED_EDIT_COUNT" \
    "$LOCKED_EDIT_TAX" | jq --argjson id "$BUSINESS_ID" '. + {id:$id}')"
expect_error PUT '/crm/business/update' "$LOCKED_UPDATE"

V2="$(api PUT '/crm/business-quote/reopen' \
    "$(jq -n --argjson id "$BUSINESS_ID" '{businessId:$id,remark:"客户调整采购数量和税率"}')")"
V2_ID="$(jq -r '.data.id' <<< "$V2")"
jq -e --argjson source "$V1_ID" '.data.versionNo == 2 and .data.status == 0 and .data.sourceQuoteId == $source' \
    >/dev/null <<< "$V2" || { printf 'V2 reopen mismatch.\n' >&2; exit 1; }

UPDATE_PAYLOAD="$(business_payload "$V2_DISCOUNT" "$V2_PRICE" "$V2_COUNT" "$V2_TAX" \
    | jq --argjson id "$BUSINESS_ID" '. + {id:$id}')"
api PUT '/crm/business/update' "$UPDATE_PAYLOAD" >/dev/null
CURRENT_V2="$(api GET "/crm/business-quote/current?businessId=${BUSINESS_ID}")"
jq -e --argjson subtotal "$V2_SUBTOTAL" --argjson discount "$V2_DISCOUNT_AMOUNT" \
    --argjson net "$V2_NET" --argjson tax "$V2_TAX_AMOUNT" --argjson gross "$V2_GROSS" \
    --argjson baseGross "$V2_BASE_GROSS" --argjson price "$V2_PRICE" --argjson count "$V2_COUNT" \
    '.data.versionNo == 2 and .data.subtotal == $subtotal and .data.discountAmount == $discount
     and .data.netAmount == $net and .data.taxAmount == $tax
     and .data.grossAmount == $gross and .data.baseGrossAmount == $baseGross
     and .data.items[0].businessPrice == $price and .data.items[0].count == $count' \
    >/dev/null <<< "$CURRENT_V2" || { printf 'V2 update calculation mismatch.\n' >&2; exit 1; }

VERSIONS="$(api GET "/crm/business-quote/versions?businessId=${BUSINESS_ID}")"
jq -e --argjson v1 "$V1_ID" --argjson v2 "$V2_ID" \
    --argjson v1Gross "$V1_GROSS" \
    '.data | length == 2 and .[0].id == $v2 and .[0].versionNo == 2 and .[1].id == $v1
     and .[1].versionNo == 1 and .[1].status == 20 and .[1].grossAmount == $v1Gross' \
    >/dev/null <<< "$VERSIONS" || { printf 'Version history immutability mismatch.\n' >&2; exit 1; }

api PUT '/crm/business-quote/lock' \
    "$(jq -n --argjson id "$BUSINESS_ID" '{businessId:$id,remark:"客户确认第二版报价"}')" >/dev/null
api PUT '/crm/business/update-status' "$(jq -n --argjson id "$BUSINESS_ID" '{id:$id,endStatus:1}')" >/dev/null

CONTRACT_PAYLOAD="$(jq -n --arg name "$CONTRACT_NAME" --argjson business "$BUSINESS_ID" \
    --argjson customer "$CUSTOMER_ID" --argjson owner "$OWNER_USER_ID" --argjson orderDate "$NOW_MS" \
    --argjson product "$PRODUCT_ID" --argjson rowId "$FORGED_CONTRACT_ROW_ID" \
    --argjson forgedProductPrice "$FORGED_PRODUCT_PRICE" --argjson forgedContractPrice "$FORGED_CONTRACT_PRICE" \
    --argjson forgedCount "$FORGED_CONTRACT_COUNT" --argjson forgedDiscount "$FORGED_CONTRACT_DISCOUNT" \
    '{name:$name,customerId:$customer,businessId:$business,ownerUserId:$owner,orderDate:$orderDate,
      discountPercent:$forgedDiscount,remark:"contract snapshot acceptance",
      products:[{id:$rowId,productId:$product,productPrice:$forgedProductPrice,
                 contractPrice:$forgedContractPrice,count:$forgedCount}]}')"
CONTRACT_ID="$(api POST '/crm/contract/create-from-business' "$CONTRACT_PAYLOAD" | jq -r '.data')"
CONTRACT="$(api GET "/crm/contract/get?id=${CONTRACT_ID}")"
jq -e --argjson quote "$V2_ID" --argjson product "$PRODUCT_ID" --argjson catalog "$CATALOG_PRICE" \
    --argjson rowId "$FORGED_CONTRACT_ROW_ID" --arg currency "$CURRENCY_CODE" --argjson net "$V2_NET" \
    --argjson taxAmount "$V2_TAX_AMOUNT" --argjson gross "$V2_GROSS" --argjson price "$V2_PRICE" \
    --argjson count "$V2_COUNT" --argjson taxRate "$V2_TAX" \
    '.data.sourceQuoteId == $quote and .data.currencyCode == $currency
     and .data.totalPrice == $net and .data.taxAmount == $taxAmount and .data.grossAmount == $gross
     and .data.products[0].id != $rowId and .data.products[0].productId == $product
     and .data.products[0].productPrice == $catalog and .data.products[0].contractPrice == $price
     and .data.products[0].count == $count and .data.products[0].taxRatePercent == $taxRate' \
    >/dev/null <<< "$CONTRACT" || {
        printf 'Contract source quote snapshot mismatch. Actual snapshot:\n' >&2
        jq '{sourceQuoteId:.data.sourceQuoteId,currencyCode:.data.currencyCode,totalPrice:.data.totalPrice,
             taxAmount:.data.taxAmount,grossAmount:.data.grossAmount,product:.data.products[0]}' \
            <<< "$CONTRACT" >&2
        exit 1
    }

IDEMPOTENT_ID="$(api POST '/crm/contract/create-from-business' "$CONTRACT_PAYLOAD" | jq -r '.data')"
[[ "$IDEMPOTENT_ID" == "$CONTRACT_ID" ]] || { printf 'Business conversion is not idempotent.\n' >&2; exit 1; }

ACCEPTED_BUSINESS_ID="$BUSINESS_ID"
ACCEPTED_CONTRACT_ID="$CONTRACT_ID"
cleanup
set -e
RESIDUAL_ROWS="$(mysql_exec "SELECT
    (SELECT COUNT(*) FROM crm_contract_change_record WHERE tenant_id=${TENANT_ID} AND contract_id=${ACCEPTED_CONTRACT_ID}) +
    (SELECT COUNT(*) FROM crm_contract_product WHERE tenant_id=${TENANT_ID} AND contract_id=${ACCEPTED_CONTRACT_ID}) +
    (SELECT COUNT(*) FROM crm_contract WHERE tenant_id=${TENANT_ID} AND id=${ACCEPTED_CONTRACT_ID}) +
    (SELECT COUNT(*) FROM crm_business_quote_action_record WHERE tenant_id=${TENANT_ID} AND quote_id IN (${V1_ID},${V2_ID})) +
    (SELECT COUNT(*) FROM crm_business_quote_item WHERE tenant_id=${TENANT_ID} AND quote_id IN (${V1_ID},${V2_ID})) +
    (SELECT COUNT(*) FROM crm_business_quote WHERE tenant_id=${TENANT_ID} AND business_id=${ACCEPTED_BUSINESS_ID}) +
    (SELECT COUNT(*) FROM crm_business_product WHERE tenant_id=${TENANT_ID} AND business_id=${ACCEPTED_BUSINESS_ID}) +
    (SELECT COUNT(*) FROM crm_permission WHERE tenant_id=${TENANT_ID} AND biz_type=5 AND biz_id=${ACCEPTED_CONTRACT_ID}) +
    (SELECT COUNT(*) FROM crm_permission WHERE tenant_id=${TENANT_ID} AND biz_type=4 AND biz_id=${ACCEPTED_BUSINESS_ID}) +
    (SELECT COUNT(*) FROM crm_business WHERE tenant_id=${TENANT_ID} AND id=${ACCEPTED_BUSINESS_ID});")"
[[ "$RESIDUAL_ROWS" == "0" ]] || {
    printf 'Quote acceptance cleanup left %s temporary rows.\n' "$RESIDUAL_ROWS" >&2
    exit 1
}
BUSINESS_ID=''
CONTRACT_ID=''

printf 'CRM quote acceptance passed: business=%s v1=%s v2=%s contract=%s; residual rows=0.\n' \
    "$ACCEPTED_BUSINESS_ID" "$V1_ID" "$V2_ID" "$ACCEPTED_CONTRACT_ID"
