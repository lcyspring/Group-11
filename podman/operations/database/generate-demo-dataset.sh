#!/usr/bin/env bash

set -Eeuo pipefail
PODMAN_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/../.." && pwd)"
PROJECT_ROOT="$(cd -- "${PODMAN_DIR}/.." && pwd)"
source "${PODMAN_DIR}/lib/kdl-config.sh"
[[ $# -eq 1 ]] || { printf 'Usage: bash ./operations/database/generate-demo-dataset.sh <config.kdl>\n' >&2; exit 2; }
kdl_config_init "$1"
[[ "$(kdl_require schema_version)" == 1 ]] || exit 2

MODE="$(kdl_require operation.mode)"
NAME="$(kdl_require dataset_generation.dataset_name)"
SEED="$(kdl_positive_integer dataset_generation.random_seed)"
TENANT="$(kdl_positive_integer dataset_generation.tenant_id)"
OWNER="$(kdl_positive_integer dataset_generation.owner_user_id)"
CLEANUP_SCOPE="$(kdl_require dataset_generation.replacement_cleanup_scope)"
DEMO_USERS="$(kdl_positive_integer dataset_generation.demo_user_count)"
DEMO_PASSWORD_SOURCE="$(kdl_require dataset_generation.demo_user_password_source)"
START="$(kdl_require dataset_generation.time_start)"
END="$(kdl_require dataset_generation.time_end)"
CUSTOMERS="$(kdl_positive_integer dataset_generation.customer_count)"
PUBLIC_CUSTOMERS="$(kdl_positive_integer dataset_generation.customer_public_pool_count)"
CONTACTS="$(kdl_positive_integer dataset_generation.contact_count)"
BUSINESSES="$(kdl_positive_integer dataset_generation.business_count)"
BUSINESS_STAGES="$(kdl_positive_integer dataset_generation.business_stage_count)"
CLUES="$(kdl_positive_integer dataset_generation.clue_count)"
PUBLIC_CLUES="$(kdl_positive_integer dataset_generation.clue_public_pool_count)"
FOLLOW_UPS="$(kdl_positive_integer dataset_generation.follow_up_count)"
PRODUCTS="$(kdl_positive_integer dataset_generation.product_count)"
COMPETITORS="$(kdl_positive_integer dataset_generation.competitor_count)"
WORK_ORDER_GROUPS="$(kdl_positive_integer dataset_generation.work_order_group_count)"
ERP_CUSTOMER_MAPPINGS="$(kdl_positive_integer dataset_generation.erp_customer_mapping_count)"
ERP_PRODUCT_MAPPINGS="$(kdl_positive_integer dataset_generation.erp_product_mapping_count)"
WORK_ORDERS="$(kdl_positive_integer dataset_generation.work_order_count)"
CONTRACTS="$(kdl_positive_integer dataset_generation.contract_count)"
PLANS="$(kdl_positive_integer dataset_generation.receivable_plan_count)"
RECEIVABLES="$(kdl_positive_integer dataset_generation.receivable_count)"
INVOICES="$(kdl_positive_integer dataset_generation.invoice_count)"
REIMBURSEMENTS="$(kdl_positive_integer dataset_generation.reimbursement_count)"
REFUNDS="$(kdl_positive_integer dataset_generation.refund_count)"
CAMPAIGNS="$(kdl_positive_integer dataset_generation.marketing_campaign_count)"
CARE_RECORDS="$(kdl_positive_integer dataset_generation.customer_care_record_count)"
OA_EVENTS="$(kdl_positive_integer dataset_generation.oa_event_count)"
OA_TASKS="$(kdl_positive_integer dataset_generation.oa_task_count)"
OUTPUT="$(realpath -m -- "$(kdl_path dataset_generation.output_dir)")"

[[ "$MODE" == check || "$MODE" == generate ]] || { printf 'operation.mode must be check or generate.\n' >&2; exit 2; }
[[ "$DEMO_PASSWORD_SOURCE" == owner ]] || { printf 'dataset_generation.demo_user_password_source must be owner.\n' >&2; exit 2; }
[[ "$CLEANUP_SCOPE" == tenant-crm-demo ]] || {
  printf 'dataset_generation.replacement_cleanup_scope must be tenant-crm-demo.\n' >&2; exit 2;
}
[[ "$NAME" =~ ^[a-z0-9][a-z0-9._-]*$ ]] || exit 2
[[ "$START" =~ ^[0-9]{4}-[0-9]{2}-[0-9]{2}$ && "$END" =~ ^[0-9]{4}-[0-9]{2}-[0-9]{2}$ ]] || exit 2
((DEMO_USERS>=8 && DEMO_USERS<=20 && CUSTOMERS<=10000 && CONTACTS<=50000 && BUSINESSES<=50000 && CLUES<=50000 && FOLLOW_UPS<=100000 &&
  PRODUCTS<=20000 && COMPETITORS<=10000 && WORK_ORDER_GROUPS<=100 &&
  ERP_CUSTOMER_MAPPINGS<=10000 && ERP_PRODUCT_MAPPINGS<=10000 && WORK_ORDERS<=100000)) || {
  printf 'Configured demo scale exceeds the safety ceiling.\n' >&2; exit 2;
}
((CONTRACTS<=20000 && PLANS<=50000 && RECEIVABLES<=50000 && INVOICES<=20000 &&
  REIMBURSEMENTS<=20000 && REFUNDS<=20000 && CAMPAIGNS<=1000 &&
  CARE_RECORDS<=100000 && OA_EVENTS<=50000 && OA_TASKS<=100000)) || {
  printf 'Configured associated-domain scale exceeds the safety ceiling.\n' >&2; exit 2;
}
((BUSINESS_STAGES>=3 && BUSINESS_STAGES<=8 && CONTRACTS<=CUSTOMERS && CONTRACTS<BUSINESSES &&
  PUBLIC_CUSTOMERS<CUSTOMERS && PUBLIC_CLUES<CLUES && CONTACTS>=CUSTOMERS &&
  PLANS>=CONTRACTS && PLANS%CONTRACTS==0 && RECEIVABLES<PLANS &&
  INVOICES<=CONTRACTS && REIMBURSEMENTS<=CONTRACTS &&
  REFUNDS<=RECEIVABLES)) || {
  printf 'Counts must keep 3..8 stages, non-contract opportunities, and outstanding receivable plans.\n' >&2; exit 2;
}
((ERP_CUSTOMER_MAPPINGS<=CUSTOMERS && ERP_PRODUCT_MAPPINGS<=PRODUCTS &&
  WORK_ORDER_GROUPS<=WORK_ORDERS)) || {
  printf 'ERP mappings must not exceed their CRM masters and work-order groups must not exceed work orders.\n' >&2
  exit 2
}
DATABASE_GENERATED="$(realpath -m -- "${PROJECT_ROOT}/database/generated")"
case "$OUTPUT" in
  "$DATABASE_GENERATED"/*) ;;
  *) printf 'Generated output must stay under database/generated/.\n' >&2; exit 2 ;;
esac
span="$(( ( $(date -d "$END" +%s) - $(date -d "$START" +%s) ) / 86400 + 1 ))"
((span>0)) || { printf 'time_end must not precede time_start.\n' >&2; exit 2; }
BATCH="DEMO2-${SEED}"

printf 'Demo dataset plan: name=%s batch=%s cleanup-scope=%s demo-users=%s customers=%s public-customers=%s contacts=%s clues=%s public-clues=%s follow-ups=%s businesses=%s stages=%s products=%s competitors=%s work-order-groups=%s erp-customer-mappings=%s erp-product-mappings=%s contracts=%s plans=%s receivables=%s invoices=%s reimbursements=%s refunds=%s campaigns=%s care=%s oa-events=%s oa-tasks=%s work-orders=%s span-days=%s\n' \
  "$NAME" "$BATCH" "$CLEANUP_SCOPE" "$DEMO_USERS" "$CUSTOMERS" "$PUBLIC_CUSTOMERS" "$CONTACTS" "$CLUES" "$PUBLIC_CLUES" "$FOLLOW_UPS" "$BUSINESSES" "$BUSINESS_STAGES" "$PRODUCTS" \
  "$COMPETITORS" "$WORK_ORDER_GROUPS" "$ERP_CUSTOMER_MAPPINGS" "$ERP_PRODUCT_MAPPINGS" "$CONTRACTS" "$PLANS" \
  "$RECEIVABLES" "$INVOICES" "$REIMBURSEMENTS" "$REFUNDS" "$CAMPAIGNS" \
  "$CARE_RECORDS" "$OA_EVENTS" "$OA_TASKS" "$WORK_ORDERS" "$span"
[[ "$MODE" == generate ]] || { printf 'Check passed. No generated file was written.\n'; exit 0; }

mkdir -p "$OUTPUT"
render() {
  sed -e "s/__BATCH__/${BATCH}/g" -e "s/__SEED__/${SEED}/g" -e "s/__TENANT__/${TENANT}/g" \
    -e "s/__OWNER__/${OWNER}/g" -e "s/__START__/${START}/g" -e "s/__END__/${END}/g" -e "s/__SPAN__/${span}/g" \
    -e "s/__DEMO_USERS__/${DEMO_USERS}/g" \
    -e "s/__CUSTOMERS__/${CUSTOMERS}/g" -e "s/__PUBLIC_CUSTOMERS__/${PUBLIC_CUSTOMERS}/g" \
    -e "s/__CONTACTS__/${CONTACTS}/g" -e "s/__BUSINESSES__/${BUSINESSES}/g" \
    -e "s/__STAGES__/${BUSINESS_STAGES}/g" \
    -e "s/__CLUES__/${CLUES}/g" -e "s/__PUBLIC_CLUES__/${PUBLIC_CLUES}/g" -e "s/__FOLLOW_UPS__/${FOLLOW_UPS}/g" \
    -e "s/__PRODUCTS__/${PRODUCTS}/g" \
    -e "s/__COMPETITORS__/${COMPETITORS}/g" -e "s/__WORK_ORDER_GROUPS__/${WORK_ORDER_GROUPS}/g" \
    -e "s/__ERP_CUSTOMER_MAPPINGS__/${ERP_CUSTOMER_MAPPINGS}/g" \
    -e "s/__ERP_PRODUCT_MAPPINGS__/${ERP_PRODUCT_MAPPINGS}/g" \
    -e "s/__WORK_ORDERS__/${WORK_ORDERS}/g" -e "s/__CONTRACTS__/${CONTRACTS}/g" \
    -e "s/__PLANS__/${PLANS}/g" -e "s/__RECEIVABLES__/${RECEIVABLES}/g" \
    -e "s/__INVOICES__/${INVOICES}/g" -e "s/__REIMBURSEMENTS__/${REIMBURSEMENTS}/g" \
    -e "s/__REFUNDS__/${REFUNDS}/g" -e "s/__CAMPAIGNS__/${CAMPAIGNS}/g" \
    -e "s/__CARE_RECORDS__/${CARE_RECORDS}/g" -e "s/__OA_EVENTS__/${OA_EVENTS}/g" \
    -e "s/__OA_TASKS__/${OA_TASKS}/g" "$1" >"$2"
}
render "${PROJECT_ROOT}/database/generator/templates/crm-core-work-order.sql.tpl" "${OUTPUT}/02-insert.sql"
render "${PROJECT_ROOT}/database/generator/templates/crm-associated-domains.sql.tpl" "${OUTPUT}/03-associated.sql"
render "${PROJECT_ROOT}/database/generator/templates/crm-core-work-order-cleanup.sql.tpl" "${OUTPUT}/01-cleanup.sql"
render "${PROJECT_ROOT}/database/generator/templates/crm-demo-validation.sql.tpl" "${OUTPUT}/04-validate.sql"
printf '%s\n' './01-cleanup.sql' './02-insert.sql' './03-associated.sql' './04-validate.sql' >"${OUTPUT}/${NAME}.manifest"
(
  cd "$OUTPUT"
  sha256sum 01-cleanup.sql 02-insert.sql 03-associated.sql 04-validate.sql \
    "${NAME}.manifest" > SHA256SUMS
)
printf 'Generated deterministic dataset files under %s. No database was changed.\n' "$OUTPUT"
