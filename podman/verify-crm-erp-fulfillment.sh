#!/usr/bin/env bash
# Real API acceptance for CRM contract to ERP fulfillment. The only CLI argument is a YAML path.

set -Eeuo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
source "${SCRIPT_DIR}/lib/yaml-config.sh"

[[ $# -eq 1 ]] || { printf 'Usage: bash ./verify-crm-erp-fulfillment.sh <config.yaml>\n' >&2; exit 2; }
yaml_config_init "$1"
[[ "$(yaml_require schema_version)" == "1" ]] || { printf 'Unsupported schema_version.\n' >&2; exit 2; }

BASE_URL="$(yaml_require endpoint.base_url)"
TENANT_ID="$(yaml_positive_integer endpoint.tenant_id)"
USERNAME="$(yaml_require account.username)"
PASSWORD="$(yaml_require account.password)"
CRM_CUSTOMER_ID="$(yaml_positive_integer acceptance.crm_customer_id)"
CRM_PRODUCT_ID="$(yaml_positive_integer acceptance.crm_product_id)"
OWNER_USER_ID="$(yaml_positive_integer acceptance.owner_user_id)"
SOURCE_CURRENCY="$(yaml_require acceptance.source_currency)"
BASE_CURRENCY="$(yaml_require acceptance.base_currency)"
EXCHANGE_RATE="$(yaml_require acceptance.exchange_rate)"
SOURCE_PRICE="$(yaml_require acceptance.source_price)"
COUNT="$(yaml_require acceptance.count)"
TAX_RATE="$(yaml_require acceptance.tax_rate_percent)"
EXPECTED_ERP_TOTAL="$(yaml_require acceptance.expected_erp_total)"
MYSQL_CONTAINER="$(yaml_require mysql.container)"
MYSQL_USER="$(yaml_require mysql.user)"
MYSQL_PASSWORD="$(yaml_require mysql.password)"
MYSQL_DATABASE="$(yaml_require mysql.database)"

[[ "$BASE_URL" =~ ^https?://[^[:space:]]+$ ]] || { printf 'Invalid endpoint.base_url.\n' >&2; exit 2; }
[[ "$SOURCE_CURRENCY" =~ ^[A-Z]{3}$ && "$BASE_CURRENCY" =~ ^[A-Z]{3}$ ]] || {
    printf 'Currency codes must be uppercase ISO-4217 values.\n' >&2; exit 2;
}
for value in "$EXCHANGE_RATE" "$SOURCE_PRICE" "$COUNT" "$TAX_RATE" "$EXPECTED_ERP_TOTAL"; do
    [[ "$value" =~ ^[0-9]+([.][0-9]+)?$ ]] || { printf 'Invalid numeric value: %s\n' "$value" >&2; exit 2; }
done
[[ "$MYSQL_CONTAINER" =~ ^[a-zA-Z0-9_.-]+$ && "$MYSQL_USER" =~ ^[a-zA-Z0-9_.-]+$ && "$MYSQL_DATABASE" =~ ^[a-zA-Z0-9_]+$ ]] || {
    printf 'Invalid MySQL identifier in YAML.\n' >&2; exit 2;
}
for command in curl jq podman date; do
    command -v "$command" >/dev/null || { printf 'Missing command: %s\n' "$command" >&2; exit 1; }
done

RUN_ID="$(date +%s)"
CONTRACT_NAME="crm-erp-acceptance-${RUN_ID}"
CONTRACT_NO="HT-ERP-${RUN_ID}"
ERP_CUSTOMER_NAME="erp-acceptance-customer-${RUN_ID}"
ERP_PRODUCT_NAME="erp-acceptance-product-${RUN_ID}"
ERP_PRODUCT_BARCODE="ERP-${RUN_ID}"
CONTRACT_ID=''
ERP_CUSTOMER_ID=''
ERP_PRODUCT_ID=''
ERP_ORDER_ID=''

mysql_exec() {
    podman exec "$MYSQL_CONTAINER" mysql "-u${MYSQL_USER}" "-p${MYSQL_PASSWORD}" \
        "--database=${MYSQL_DATABASE}" --default-character-set=utf8mb4 -Nse "$1"
}

cleanup() {
    set +e
    mysql_exec "DELETE FROM crm_contract_fulfillment WHERE tenant_id=${TENANT_ID} AND contract_id=${CONTRACT_ID:-0};
      DELETE FROM crm_erp_customer_mapping WHERE tenant_id=${TENANT_ID} AND crm_customer_id=${CRM_CUSTOMER_ID};
      DELETE FROM crm_erp_product_mapping WHERE tenant_id=${TENANT_ID} AND crm_product_id=${CRM_PRODUCT_ID};
      DELETE FROM erp_sale_order_items WHERE tenant_id=${TENANT_ID} AND order_id=${ERP_ORDER_ID:-0};
      DELETE FROM erp_sale_order WHERE tenant_id=${TENANT_ID} AND id=${ERP_ORDER_ID:-0};
      DELETE FROM crm_contract_signing WHERE tenant_id=${TENANT_ID} AND contract_id=${CONTRACT_ID:-0};
      DELETE FROM crm_contract_product WHERE tenant_id=${TENANT_ID} AND contract_id=${CONTRACT_ID:-0};
      DELETE FROM crm_permission WHERE tenant_id=${TENANT_ID} AND biz_type=5 AND biz_id=${CONTRACT_ID:-0};
      DELETE FROM crm_contract WHERE tenant_id=${TENANT_ID} AND id=${CONTRACT_ID:-0};
      DELETE FROM erp_product WHERE tenant_id=${TENANT_ID} AND id=${ERP_PRODUCT_ID:-0};
      DELETE FROM erp_customer WHERE tenant_id=${TENANT_ID} AND id=${ERP_CUSTOMER_ID:-0};" >/dev/null 2>&1
}
trap cleanup EXIT

LOGIN_PAYLOAD="$(jq -n --arg username "$USERNAME" --arg password "$PASSWORD" \
    '{username:$username,password:$password,captchaVerification:""}')"
LOGIN_RESPONSE="$(curl --noproxy '*' --fail --silent --show-error --header 'Content-Type: application/json' \
    --header "tenant-id: ${TENANT_ID}" --data "$LOGIN_PAYLOAD" "${BASE_URL}/system/auth/login")"
jq -e '.code == 0 and (.data.accessToken | length > 0)' >/dev/null <<< "$LOGIN_RESPONSE" || {
    printf 'Login failed.\n' >&2; exit 1;
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
        printf 'API %s %s failed: %s\n' "$1" "$2" "$(jq -r '.msg' <<< "$response")" >&2; return 1;
    }
    printf '%s' "$response"
}

ERP_CUSTOMER_ID="$(mysql_exec "INSERT INTO erp_customer
  (name,status,sort,creator,updater,deleted,tenant_id) VALUES
  ('${ERP_CUSTOMER_NAME}',0,1,'crm-erp-acceptance','crm-erp-acceptance',b'0',${TENANT_ID});
  SELECT id FROM erp_customer WHERE tenant_id=${TENANT_ID} AND name='${ERP_CUSTOMER_NAME}';")"
ERP_PRODUCT_ID="$(mysql_exec "INSERT INTO erp_product
  (name,bar_code,category_id,unit_id,status,sale_price,creator,updater,deleted,tenant_id) VALUES
  ('${ERP_PRODUCT_NAME}','${ERP_PRODUCT_BARCODE}',1,1,0,${SOURCE_PRICE},'crm-erp-acceptance','crm-erp-acceptance',b'0',${TENANT_ID});
  SELECT id FROM erp_product WHERE tenant_id=${TENANT_ID} AND bar_code='${ERP_PRODUCT_BARCODE}';")"

CONTRACT_ID="$(mysql_exec "INSERT INTO crm_contract
  (name,no,customer_id,owner_user_id,audit_status,order_date,total_product_price,discount_percent,total_price,
   currency_code,base_currency_code,exchange_rate_to_base,tax_amount,gross_amount,base_gross_amount,
   creator,updater,deleted,tenant_id) VALUES
  ('${CONTRACT_NAME}','${CONTRACT_NO}',${CRM_CUSTOMER_ID},${OWNER_USER_ID},0,NOW(),${SOURCE_PRICE},0,${SOURCE_PRICE},
   '${SOURCE_CURRENCY}','${BASE_CURRENCY}',${EXCHANGE_RATE},0,${SOURCE_PRICE},${EXPECTED_ERP_TOTAL},
   'crm-erp-acceptance','crm-erp-acceptance',b'0',${TENANT_ID});
  SELECT id FROM crm_contract WHERE tenant_id=${TENANT_ID} AND no='${CONTRACT_NO}';")"
mysql_exec "INSERT INTO crm_contract_product
  (contract_id,product_id,product_name_snapshot,product_no_snapshot,product_price,contract_price,count,total_price,
   tax_rate_percent,tax_amount,gross_amount,creator,updater,deleted,tenant_id)
  SELECT ${CONTRACT_ID},id,name,no,price,${SOURCE_PRICE},${COUNT},${SOURCE_PRICE},${TAX_RATE},0,${SOURCE_PRICE},
    'crm-erp-acceptance','crm-erp-acceptance',b'0',${TENANT_ID}
  FROM crm_product WHERE tenant_id=${TENANT_ID} AND id=${CRM_PRODUCT_ID};
  INSERT INTO crm_permission (biz_type,biz_id,user_id,level,creator,updater,deleted,tenant_id)
  VALUES (5,${CONTRACT_ID},${OWNER_USER_ID},3,'crm-erp-acceptance','crm-erp-acceptance',b'0',${TENANT_ID});" >/dev/null

NOT_READY="$(api GET "/crm/contract-fulfillment/get?contractId=${CONTRACT_ID}")"
jq -e '.data.eligible == false and (.data.blockers | index("CONTRACT_NOT_APPROVED")) != null
  and (.data.blockers | index("CONTRACT_NOT_SIGNED")) != null' >/dev/null <<< "$NOT_READY" || {
    printf 'Draft/unsigned blocker contract failed.\n' >&2; exit 1;
}

mysql_exec "UPDATE crm_contract SET audit_status=20 WHERE tenant_id=${TENANT_ID} AND id=${CONTRACT_ID};
  INSERT INTO crm_contract_signing
  (contract_id,contract_version,status,method,signed_time,signed_attachment_id,handler_user_id,provider_code,
   provider_request_id,creator,updater,deleted,tenant_id) VALUES
  (${CONTRACT_ID},1,10,1,NOW(),1,${OWNER_USER_ID},'LOCAL','crm-erp-${RUN_ID}',
   'crm-erp-acceptance','crm-erp-acceptance',b'0',${TENANT_ID});" >/dev/null

MISSING_MAPPING="$(api GET "/crm/contract-fulfillment/get?contractId=${CONTRACT_ID}")"
jq -e '.data.eligible == false and (.data.blockers | index("CUSTOMER_MAPPING_MISSING")) != null
  and (.data.blockers | index("PRODUCT_MAPPING_MISSING")) != null' >/dev/null <<< "$MISSING_MAPPING" || {
    printf 'Missing mapping blockers failed.\n' >&2; exit 1;
}

api POST '/crm/erp-mapping/customer/save' "$(jq -n --argjson crm "$CRM_CUSTOMER_ID" --argjson erp "$ERP_CUSTOMER_ID" '{crmId:$crm,erpId:$erp,remark:"acceptance"}')" >/dev/null
api POST '/crm/erp-mapping/product/save' "$(jq -n --argjson crm "$CRM_PRODUCT_ID" --argjson erp "$ERP_PRODUCT_ID" '{crmId:$crm,erpId:$erp,remark:"acceptance"}')" >/dev/null

READY="$(api GET "/crm/contract-fulfillment/get?contractId=${CONTRACT_ID}")"
jq -e '.data.eligible == true and (.data.blockers | length) == 0' >/dev/null <<< "$READY" || {
    printf 'Mapped contract did not become eligible.\n' >&2; exit 1;
}

CREATED="$(api POST "/crm/contract-fulfillment/create-or-retry?contractId=${CONTRACT_ID}")"
ERP_ORDER_ID="$(jq -r '.data.record.erpOrderId' <<< "$CREATED")"
jq -e --argjson total "$EXPECTED_ERP_TOTAL" --arg source "$SOURCE_CURRENCY" --arg target "$BASE_CURRENCY" \
  '.data.record.status == 10 and .data.record.erpTotalAmount == $total
   and .data.record.sourceCurrencyCode == $source and .data.record.erpCurrencyCode == $target' \
  >/dev/null <<< "$CREATED" || {
    printf 'ERP fulfillment creation or currency conversion failed.\n' >&2; exit 1;
}

IDEMPOTENT="$(api POST "/crm/contract-fulfillment/create-or-retry?contractId=${CONTRACT_ID}")"
[[ "$(jq -r '.data.record.erpOrderId' <<< "$IDEMPOTENT")" == "$ERP_ORDER_ID" ]] || {
    printf 'Repeated creation returned a different ERP order.\n' >&2; exit 1;
}

DELETE_RESPONSE="$(api_raw DELETE "/erp/sale-order/delete?ids=${ERP_ORDER_ID}")"
jq -e '.code != 0' >/dev/null <<< "$DELETE_RESPONSE" || { printf 'External ERP order was deletable.\n' >&2; exit 1; }
api PUT "/erp/sale-order/update-status?id=${ERP_ORDER_ID}&status=20" >/dev/null
mysql_exec "UPDATE erp_sale_order SET out_count=1.250000,return_count=0.250000 WHERE tenant_id=${TENANT_ID} AND id=${ERP_ORDER_ID};
  UPDATE erp_sale_order_items SET out_count=1.250000,return_count=0.250000 WHERE tenant_id=${TENANT_ID} AND order_id=${ERP_ORDER_ID};" >/dev/null
SYNCED="$(api POST "/crm/contract-fulfillment/refresh?contractId=${CONTRACT_ID}")"
jq -e '.data.record.erpOrderStatus == 20 and .data.record.outCount == 1.25 and .data.record.returnCount == 0.25
  and .data.record.lastSyncTime != null' >/dev/null <<< "$SYNCED" || {
    printf 'ERP status/count refresh failed.\n' >&2; exit 1;
}

ACCEPTED_CONTRACT_ID="$CONTRACT_ID"
ACCEPTED_ERP_ORDER_ID="$ERP_ORDER_ID"
ACCEPTED_ERP_CUSTOMER_ID="$ERP_CUSTOMER_ID"
ACCEPTED_ERP_PRODUCT_ID="$ERP_PRODUCT_ID"
cleanup
set -e
RESIDUAL="$(mysql_exec "SELECT
  (SELECT COUNT(*) FROM crm_contract_fulfillment WHERE tenant_id=${TENANT_ID} AND contract_id=${ACCEPTED_CONTRACT_ID}) +
  (SELECT COUNT(*) FROM crm_contract WHERE tenant_id=${TENANT_ID} AND id=${ACCEPTED_CONTRACT_ID}) +
  (SELECT COUNT(*) FROM erp_sale_order WHERE tenant_id=${TENANT_ID} AND id=${ACCEPTED_ERP_ORDER_ID}) +
  (SELECT COUNT(*) FROM erp_customer WHERE tenant_id=${TENANT_ID} AND id=${ACCEPTED_ERP_CUSTOMER_ID}) +
  (SELECT COUNT(*) FROM erp_product WHERE tenant_id=${TENANT_ID} AND id=${ACCEPTED_ERP_PRODUCT_ID});")"
[[ "$RESIDUAL" == "0" ]] || { printf 'Acceptance cleanup left %s rows.\n' "$RESIDUAL" >&2; exit 1; }
CONTRACT_ID=''

printf 'CRM ERP fulfillment real API acceptance passed: contract=%s erpOrder=%s; cleanup residual=0.\n' \
    "$ACCEPTED_CONTRACT_ID" "$ACCEPTED_ERP_ORDER_ID"
