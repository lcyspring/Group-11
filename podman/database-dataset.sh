#!/usr/bin/env bash

set -Eeuo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd -- "${SCRIPT_DIR}/.." && pwd)"

usage() {
    printf 'Usage: bash ./database-dataset.sh <config.yaml>\n' >&2
}

[[ $# -eq 1 ]] || { usage; exit 2; }

CONFIG_PATH="$1"
if [[ "$CONFIG_PATH" != /* ]]; then
    CONFIG_PATH="$(cd -- "$(dirname -- "$CONFIG_PATH")" && pwd)/$(basename -- "$CONFIG_PATH")"
fi

# shellcheck source=lib/yaml-config.sh
source "${SCRIPT_DIR}/lib/yaml-config.sh"
yaml_config_init "$CONFIG_PATH"

[[ "$(yaml_require schema_version)" == 1 ]] || {
    printf 'Unsupported schema_version; expected 1.\n' >&2
    exit 2
}

MODE="$(yaml_require operation.dataset_mode)"
MYSQL_CONTAINER="$(yaml_require container.mysql)"
MYSQL_DATABASE="$(yaml_require mysql.database)"
MYSQL_USERNAME="$(yaml_require mysql.username)"
MYSQL_PASSWORD="$(yaml_require mysql.password)"
DATASET_NAME="$(yaml_require mysql.dataset)"
CLEANUP_EXISTING="$(yaml_bool mysql.cleanup_existing_before_dataset)"
CONFIRM_CHANGE="$(yaml_bool mysql.confirm_persistent_data_change)"

[[ "$MODE" == check || "$MODE" == replace ]] || {
    printf 'operation.dataset_mode must be check or replace.\n' >&2
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
    [[ "$resolved" != "${PROJECT_ROOT}/database/maintenance/cleanup/"* ]] || has_cleanup=true
    sql_files+=("$resolved")
done < "$MANIFEST"

[[ ${#sql_files[@]} -gt 0 ]] || {
    printf 'Dataset %s has no executable replacement entries.\n' "$DATASET_NAME" >&2
    exit 2
}
if [[ "$CLEANUP_EXISTING" == true && "$has_cleanup" != true ]]; then
    printf 'Cleanup was requested, but the dataset has no explicit cleanup entry.\n' >&2
    exit 2
fi
if [[ "$CLEANUP_EXISTING" == false && "$has_cleanup" == true ]]; then
    printf 'Dataset contains cleanup SQL; set mysql.cleanup_existing_before_dataset=true explicitly.\n' >&2
    exit 2
fi

if [[ "$MODE" == check ]]; then
    printf 'Dataset replacement configuration is valid: dataset=%s cleanup_existing=%s files=%s\n' \
        "$DATASET_NAME" "$CLEANUP_EXISTING" "${#sql_files[@]}"
    exit 0
fi

[[ "$CONFIRM_CHANGE" == true ]] || {
    printf 'Refusing persistent data replacement: mysql.confirm_persistent_data_change must be true.\n' >&2
    exit 2
}
command -v podman >/dev/null 2>&1 || { printf 'Podman is required.\n' >&2; exit 1; }
podman container exists "$MYSQL_CONTAINER" || { printf 'MySQL container is not running.\n' >&2; exit 1; }

for sql_file in "${sql_files[@]}"; do
    printf 'Applying dataset replacement SQL: %s\n' "${sql_file#${PROJECT_ROOT}/database/}"
    podman exec -i "$MYSQL_CONTAINER" mysql --default-character-set=utf8mb4 \
        -u"$MYSQL_USERNAME" -p"$MYSQL_PASSWORD" "$MYSQL_DATABASE" < "$sql_file"
done
printf 'Dataset replacement completed: %s\n' "$DATASET_NAME"
