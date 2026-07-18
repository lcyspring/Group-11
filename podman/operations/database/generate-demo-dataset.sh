#!/usr/bin/env bash

set -Eeuo pipefail
PODMAN_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/../.." && pwd)"
PROJECT_ROOT="$(cd -- "${PODMAN_DIR}/.." && pwd)"
source "${PODMAN_DIR}/lib/yaml-config.sh"
[[ $# -eq 1 ]] || { printf 'Usage: bash ./operations/database/generate-demo-dataset.sh <config.yaml>\n' >&2; exit 2; }
yaml_config_init "$1"
[[ "$(yaml_require schema_version)" == 1 ]] || exit 2

MODE="$(yaml_require operation.mode)"
NAME="$(yaml_require dataset_generation.dataset_name)"
SEED="$(yaml_positive_integer dataset_generation.random_seed)"
TENANT="$(yaml_positive_integer dataset_generation.tenant_id)"
OWNER="$(yaml_positive_integer dataset_generation.owner_user_id)"
START="$(yaml_require dataset_generation.time_start)"
END="$(yaml_require dataset_generation.time_end)"
CUSTOMERS="$(yaml_positive_integer dataset_generation.customer_count)"
BUSINESSES="$(yaml_positive_integer dataset_generation.business_count)"
WORK_ORDERS="$(yaml_positive_integer dataset_generation.work_order_count)"
OUTPUT="$(realpath -m -- "$(yaml_path dataset_generation.output_dir)")"
CLEANUP="$(yaml_bool dataset_generation.cleanup_existing_generated_data)"
CONFIRM="$(yaml_bool dataset_generation.confirm_persistent_data_change)"

[[ "$MODE" == check || "$MODE" == generate ]] || { printf 'operation.mode must be check or generate.\n' >&2; exit 2; }
[[ "$NAME" =~ ^[a-z0-9][a-z0-9._-]*$ ]] || exit 2
[[ "$START" =~ ^[0-9]{4}-[0-9]{2}-[0-9]{2}$ && "$END" =~ ^[0-9]{4}-[0-9]{2}-[0-9]{2}$ ]] || exit 2
((CUSTOMERS<=10000 && BUSINESSES<=50000 && WORK_ORDERS<=100000)) || { printf 'Configured demo scale exceeds the safety ceiling.\n' >&2; exit 2; }
[[ "$CLEANUP" == "$CONFIRM" ]] || { printf 'Cleanup and persistent-data confirmation must have the same value.\n' >&2; exit 2; }
DATABASE_GENERATED="$(realpath -m -- "${PROJECT_ROOT}/database/generated")"
case "$OUTPUT" in
  "$DATABASE_GENERATED"/*) ;;
  *) printf 'Generated output must stay under database/generated/.\n' >&2; exit 2 ;;
esac
span="$(( ( $(date -d "$END" +%s) - $(date -d "$START" +%s) ) / 86400 + 1 ))"
((span>0)) || { printf 'time_end must not precede time_start.\n' >&2; exit 2; }
BATCH="DEMO2-${SEED}"

printf 'Demo dataset plan: name=%s batch=%s customers=%s contacts=%s businesses=%s work-orders=%s span-days=%s\n' \
  "$NAME" "$BATCH" "$CUSTOMERS" "$CUSTOMERS" "$BUSINESSES" "$WORK_ORDERS" "$span"
[[ "$MODE" == generate ]] || { printf 'Check passed. No generated file was written.\n'; exit 0; }

mkdir -p "$OUTPUT"
render() {
  sed -e "s/__BATCH__/${BATCH}/g" -e "s/__SEED__/${SEED}/g" -e "s/__TENANT__/${TENANT}/g" \
    -e "s/__OWNER__/${OWNER}/g" -e "s/__START__/${START}/g" -e "s/__SPAN__/${span}/g" \
    -e "s/__CUSTOMERS__/${CUSTOMERS}/g" -e "s/__BUSINESSES__/${BUSINESSES}/g" \
    -e "s/__WORK_ORDERS__/${WORK_ORDERS}/g" "$1" >"$2"
}
render "${PROJECT_ROOT}/database/generator/templates/crm-core-work-order.sql.tpl" "${OUTPUT}/02-insert.sql"
render "${PROJECT_ROOT}/database/generator/templates/crm-core-work-order-cleanup.sql.tpl" "${OUTPUT}/01-cleanup.sql"
printf '%s\n' './01-cleanup.sql' './02-insert.sql' >"${OUTPUT}/${NAME}.manifest"
sha256sum "${OUTPUT}/01-cleanup.sql" "${OUTPUT}/02-insert.sql" "${OUTPUT}/${NAME}.manifest" >"${OUTPUT}/SHA256SUMS"
printf 'Generated deterministic dataset files under %s. No database was changed.\n' "$OUTPUT"
