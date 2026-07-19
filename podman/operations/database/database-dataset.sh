#!/usr/bin/env bash

set -Eeuo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
PODMAN_DIR="$(cd -- "${SCRIPT_DIR}/../.." && pwd)"
PROJECT_ROOT="$(cd -- "${PODMAN_DIR}/.." && pwd)"

usage() {
    printf 'Usage: bash ./operations/database/database-dataset.sh <config.kdl>\n' >&2
}

[[ $# -eq 1 ]] || { usage; exit 2; }

CONFIG_PATH="$1"
if [[ "$CONFIG_PATH" != /* ]]; then
    CONFIG_PATH="$(cd -- "$(dirname -- "$CONFIG_PATH")" && pwd)/$(basename -- "$CONFIG_PATH")"
fi

# shellcheck source=../../lib/kdl-config.sh
source "${PODMAN_DIR}/lib/kdl-config.sh"
kdl_config_init "$CONFIG_PATH"

[[ "$(kdl_require schema_version)" == 1 ]] || {
    printf 'Unsupported schema_version; expected 1.\n' >&2
    exit 2
}

ACTION="$(kdl_require operation.action)"
MYSQL_CONTAINER="$(kdl_require container.mysql)"
MYSQL_DATABASE="$(kdl_require mysql.database)"
MYSQL_USERNAME="$(kdl_require mysql.username)"
MYSQL_PASSWORD="$(kdl_require mysql.password)"
DATASET_NAME="$(kdl_require mysql.dataset)"
DATASET_MODE="$(kdl_require mysql.dataset_mode)"

[[ "$ACTION" == check || "$ACTION" == apply ]] || {
    printf 'operation.action must be check or apply.\n' >&2
    exit 2
}
[[ "$DATASET_MODE" == insert || "$DATASET_MODE" == replace ]] || {
    printf 'mysql.dataset_mode must be insert or replace for an explicit dataset operation.\n' >&2
    exit 2
}
[[ "$DATASET_NAME" =~ ^[a-z0-9][a-z0-9._-]*$ ]] || {
    printf 'mysql.dataset contains unsupported characters: %s\n' "$DATASET_NAME" >&2
    exit 2
}

MANIFEST="${PROJECT_ROOT}/database/datasets/${DATASET_NAME}.manifest"
[[ -s "$MANIFEST" ]] || {
    printf 'Selected dataset manifest is missing or empty: %s\n' "$DATASET_NAME" >&2
    exit 2
}

manifest_dir="$(dirname -- "$MANIFEST")"
sql_files=()
has_cleanup=false
first_entry_is_cleanup=false
while IFS= read -r entry || [[ -n "$entry" ]]; do
    [[ -n "$entry" && "$entry" != \#* ]] || continue
    [[ "$entry" != /* ]] || { printf 'Absolute dataset paths are forbidden.\n' >&2; exit 2; }
    resolved="$(realpath -m -- "${manifest_dir}/${entry}")"
    [[ "$resolved" == "${PROJECT_ROOT}/database/"* && -s "$resolved" ]] || {
        printf 'Invalid dataset entry: %s\n' "$entry" >&2
        exit 2
    }
    [[ "$resolved" != "${PROJECT_ROOT}/database/teardown/"* ]] || {
        printf 'Dataset replacement cannot execute teardown SQL.\n' >&2
        exit 2
    }
    if [[ "$resolved" == "${PROJECT_ROOT}/database/maintenance/cleanup/"* ||
          "$(basename -- "$resolved")" == *cleanup*.sql ]]; then
        has_cleanup=true
        ((${#sql_files[@]} == 0)) && first_entry_is_cleanup=true
    fi
    sql_files+=("$resolved")
done < "$MANIFEST"

[[ ${#sql_files[@]} -gt 0 ]] || {
    printf 'Dataset %s has no executable replacement entries.\n' "$DATASET_NAME" >&2
    exit 2
}
case "$DATASET_MODE" in
    insert)
        [[ "$has_cleanup" == false ]] || {
            printf 'mysql.dataset_mode=insert forbids cleanup SQL in the dataset manifest.\n' >&2
            exit 2
        }
        ;;
    replace)
        [[ "$has_cleanup" == true && "$first_entry_is_cleanup" == true ]] || {
            printf 'mysql.dataset_mode=replace requires cleanup SQL as the first dataset manifest entry.\n' >&2
            exit 2
        }
        ;;
esac

if [[ "$ACTION" == check ]]; then
    printf 'Dataset configuration is valid: dataset=%s mode=%s files=%s\n' \
        "$DATASET_NAME" "$DATASET_MODE" "${#sql_files[@]}"
    exit 0
fi

command -v podman >/dev/null 2>&1 || { printf 'Podman is required.\n' >&2; exit 1; }
podman container exists "$MYSQL_CONTAINER" || { printf 'MySQL container is not running.\n' >&2; exit 1; }

for sql_file in "${sql_files[@]}"; do
    printf 'Applying dataset %s SQL: %s\n' "$DATASET_MODE" "${sql_file#${PROJECT_ROOT}/database/}"
    podman exec -i "$MYSQL_CONTAINER" mysql --default-character-set=utf8mb4 \
        -u"$MYSQL_USERNAME" -p"$MYSQL_PASSWORD" "$MYSQL_DATABASE" < "$sql_file"
done
printf 'Dataset %s completed: %s\n' "$DATASET_MODE" "$DATASET_NAME"
