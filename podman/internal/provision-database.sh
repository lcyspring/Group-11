#!/usr/bin/env bash

# Deploy-time MySQL schema and dataset provisioner. SQL remains in the
# repository and is streamed into the official MySQL container; it is never
# copied into a project runtime image or mounted into the running container.

set -Eeuo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
PODMAN_DIR="$(cd -- "${SCRIPT_DIR}/.." && pwd)"
CONTAINER_ENGINE="${CONTAINER_ENGINE:-podman}"

[[ $# -eq 1 ]] || {
    printf 'Usage: bash ./internal/provision-database.sh <runtime-config.kdl>\n' >&2
    exit 2
}

# shellcheck source=../lib/kdl-config.sh
source "${PODMAN_DIR}/lib/kdl-config.sh"
kdl_config_init "$1"

[[ "$(kdl_require schema_version)" == "1" ]] || exit 2

DATABASE_ROOT="$(realpath -m -- "$(kdl_path mysql.sql_root)")"
START_MODE="$(kdl_require operation.startup_mode)"
MYSQL_CONTAINER="$(kdl_require container.mysql)"
MYSQL_DATABASE="$(kdl_require mysql.database)"
MYSQL_DATASET="$(kdl_require mysql.dataset)"
MYSQL_DATASET_MANIFEST="$(kdl_path mysql.dataset_manifest)"
MYSQL_DATASET_MODE="$(kdl_require mysql.dataset_mode)"
MYSQL_BOOTSTRAP_POLICY="$(kdl_require mysql.bootstrap_policy)"
MYSQL_BOOTSTRAP_MANIFEST="$(kdl_path mysql.bootstrap_manifest)"
MYSQL_COMPATIBILITY_MANIFEST="$(kdl_path mysql.compatibility_migration_manifest)"
MYSQL_ROOT_PASSWORD="$(kdl_require mysql.root_password)"
MYSQL_CHARACTER_SET="$(kdl_require mysql.character_set)"
MYSQL_ADMIN_USERNAME="$(kdl_require mysql.administration_username)"
DATASET_MANIFEST="$MYSQL_DATASET_MANIFEST"

case "$MYSQL_BOOTSTRAP_POLICY" in
    initialize-empty|require-existing) ;;
    *)
        printf 'mysql.bootstrap_policy must be initialize-empty or require-existing; got: %s\n' \
            "$MYSQL_BOOTSTRAP_POLICY" >&2
        exit 2
        ;;
esac
case "$MYSQL_DATASET_MODE" in
    preserve|insert|replace) ;;
    *) printf 'mysql.dataset_mode must be preserve, insert, or replace.\n' >&2; exit 2 ;;
esac
[[ "$MYSQL_CONTAINER" =~ ^[A-Za-z0-9][A-Za-z0-9_.-]*$ ]] || {
    printf 'container.mysql contains unsupported characters.\n' >&2
    exit 2
}
[[ "$MYSQL_DATABASE" =~ ^[A-Za-z0-9_]+$ ]] || {
    printf 'mysql.database contains unsupported characters.\n' >&2
    exit 2
}
[[ "$MYSQL_DATASET" =~ ^[a-z0-9][a-z0-9._-]*$ ]] || {
    printf 'mysql.dataset contains unsupported characters.\n' >&2
    exit 2
}
[[ "$MYSQL_CHARACTER_SET" =~ ^[A-Za-z0-9_]+$ ]] || {
    printf 'mysql.character_set contains unsupported characters.\n' >&2
    exit 2
}
[[ -d "$DATABASE_ROOT" ]] || {
    printf 'mysql.sql_root is not a directory: %s\n' "$DATABASE_ROOT" >&2
    exit 2
}
[[ "$MYSQL_ADMIN_USERNAME" == root ]] || {
    printf 'mysql.administration_username must be root for database provision.\n' >&2
    exit 2
}

require_file() {
    [[ -s "$1" ]] || {
        printf 'Required database file is missing or empty: %s\n' "$1" >&2
        exit 1
    }
}

resolve_manifest_entry() {
    local manifest="$1" entry="$2" manifest_dir resolved
    [[ "$entry" != /* ]] || {
        printf 'Absolute SQL manifest paths are forbidden: %s\n' "$entry" >&2
        return 1
    }
    manifest_dir="$(dirname -- "$manifest")"
    resolved="$(realpath -m -- "${manifest_dir}/${entry}")"
    [[ "$resolved" == "${DATABASE_ROOT}/"* ]] || {
        printf 'SQL manifest path escapes database root: %s\n' "$entry" >&2
        return 1
    }
    printf '%s' "$resolved"
}

DATASET_HAS_CLEANUP=false
DATASET_FIRST_ENTRY_IS_CLEANUP=false
validate_manifest() {
    local manifest="$1" purpose="$2" entry sql_file entries=0
    manifest="$(realpath -m -- "$manifest")"
    [[ "$manifest" == "${DATABASE_ROOT}/"* ]] || {
        printf '%s manifest must be located under mysql.sql_root: %s\n' "$purpose" "$manifest" >&2
        exit 2
    }
    require_file "$manifest"
    while IFS= read -r entry || [[ -n "$entry" ]]; do
        [[ -n "$entry" && "$entry" != \#* ]] || continue
        sql_file="$(resolve_manifest_entry "$manifest" "$entry")"
        require_file "$sql_file"
        [[ "$sql_file" != "${DATABASE_ROOT}/teardown/"* ]] || {
            printf 'Teardown SQL is forbidden in %s manifest: %s\n' "$purpose" "$entry" >&2
            exit 2
        }
        if [[ "$purpose" != dataset && "$sql_file" == "${DATABASE_ROOT}/maintenance/cleanup/"* ]]; then
            printf 'Cleanup SQL is forbidden in %s manifest: %s\n' "$purpose" "$entry" >&2
            exit 2
        fi
        if [[ "$purpose" == dataset &&
              ( "$sql_file" == "${DATABASE_ROOT}/maintenance/cleanup/"* ||
                "$(basename -- "$sql_file")" == *cleanup*.sql ) ]]; then
            DATASET_HAS_CLEANUP=true
            ((entries == 0)) && DATASET_FIRST_ENTRY_IS_CLEANUP=true
        fi
        entries=$((entries + 1))
    done < "$manifest"
    [[ "$purpose" == dataset ]] || ((entries > 0)) || {
        printf '%s manifest has no SQL entries: %s\n' "$purpose" "$manifest" >&2
        exit 2
    }
}

validate_manifest "$MYSQL_BOOTSTRAP_MANIFEST" bootstrap
validate_manifest "$MYSQL_COMPATIBILITY_MANIFEST" compatibility
validate_manifest "$DATASET_MANIFEST" dataset

case "$MYSQL_DATASET_MODE" in
    preserve)
        ;;
    insert)
        [[ "$DATASET_HAS_CLEANUP" == false ]] || {
            printf 'mysql.dataset_mode=insert forbids cleanup SQL in the dataset manifest.\n' >&2
            exit 2
        }
        ;;
    replace)
        [[ "$DATASET_HAS_CLEANUP" == true && "$DATASET_FIRST_ENTRY_IS_CLEANUP" == true ]] || {
            printf 'mysql.dataset_mode=replace requires cleanup SQL as the first dataset manifest entry.\n' >&2
            exit 2
        }
        ;;
esac

case "$START_MODE" in
    check)
        printf 'Database deployment preflight passed: bootstrap=%s dataset=%s mode=%s. No SQL was executed.\n' \
            "$MYSQL_BOOTSTRAP_POLICY" "$MYSQL_DATASET" "$MYSQL_DATASET_MODE"
        exit 0
        ;;
    replace|replace-server) ;;
    *)
        printf 'Database provision skipped for startup_mode=%s.\n' "$START_MODE"
        exit 0
        ;;
esac

case "$CONTAINER_ENGINE" in
    podman|docker) ;;
    *)
        printf 'CONTAINER_ENGINE must be podman or docker; got: %s\n' "$CONTAINER_ENGINE" >&2
        exit 2
        ;;
esac
command -v "$CONTAINER_ENGINE" >/dev/null 2>&1 || {
    printf 'Container engine is required for database provision: %s\n' "$CONTAINER_ENGINE" >&2
    exit 1
}

container_cmd() {
    "$CONTAINER_ENGINE" "$@"
}

mysql_command() {
    container_cmd exec --env "MYSQL_PWD=${MYSQL_ROOT_PASSWORD}" "$MYSQL_CONTAINER" \
        mysql "--default-character-set=${MYSQL_CHARACTER_SET}" \
        "--user=${MYSQL_ADMIN_USERNAME}" "--database=${MYSQL_DATABASE}" "$@"
}

mysql_scalar() {
    mysql_command --batch --skip-column-names --execute "$1"
}

execute_manifest() {
    local manifest="$1" label="$2" entry sql_file
    while IFS= read -r entry || [[ -n "$entry" ]]; do
        [[ -n "$entry" && "$entry" != \#* ]] || continue
        sql_file="$(resolve_manifest_entry "$manifest" "$entry")"
        printf 'Applying %s SQL: %s\n' "$label" "${sql_file#${DATABASE_ROOT}/}"
        container_cmd exec --env "MYSQL_PWD=${MYSQL_ROOT_PASSWORD}" -i "$MYSQL_CONTAINER" \
            mysql "--default-character-set=${MYSQL_CHARACTER_SET}" \
            "--user=${MYSQL_ADMIN_USERNAME}" "--database=${MYSQL_DATABASE}" < "$sql_file"
    done < "$manifest"
}

table_count="$(mysql_scalar "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE() AND table_type='BASE TABLE';")"
marker_count="$(mysql_scalar "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name='system_users';")"
[[ "$table_count" =~ ^[0-9]+$ && "$marker_count" =~ ^[01]$ ]] || {
    printf 'Unable to classify current MySQL schema.\n' >&2
    exit 1
}

if ((table_count == 0)); then
    [[ "$MYSQL_BOOTSTRAP_POLICY" == initialize-empty ]] || {
        printf 'MySQL database is empty but mysql.bootstrap_policy=require-existing.\n' >&2
        exit 1
    }
    printf 'Initializing confirmed empty MySQL database from explicit bootstrap manifest.\n'
    execute_manifest "$MYSQL_BOOTSTRAP_MANIFEST" bootstrap
    printf 'Applying explicit empty-database dataset: %s\n' "$MYSQL_DATASET"
    execute_manifest "$DATASET_MANIFEST" dataset
elif ((marker_count == 0)); then
    printf 'MySQL database contains %s tables but has no system_users marker; refusing destructive bootstrap.\n' \
        "$table_count" >&2
    exit 1
else
    if [[ "$MYSQL_DATASET_MODE" == replace ]]; then
        printf 'Existing MySQL schema detected (%s tables); bootstrap is preserved.\n' "$table_count"
        printf 'Replacing existing dataset (cleanup first, then insert): %s\n' "$MYSQL_DATASET"
        execute_manifest "$DATASET_MANIFEST" dataset
    elif [[ "$MYSQL_DATASET_MODE" == insert ]]; then
        printf 'Existing MySQL schema detected (%s tables); bootstrap is preserved.\n' "$table_count"
        printf 'Inserting selected dataset without cleanup: %s\n' "$MYSQL_DATASET"
        execute_manifest "$DATASET_MANIFEST" dataset
    else
        printf 'Existing MySQL schema detected (%s tables); bootstrap and dataset are preserved.\n' "$table_count"
    fi
fi

printf 'Applying idempotent compatibility manifest.\n'
execute_manifest "$MYSQL_COMPATIBILITY_MANIFEST" compatibility

final_marker="$(mysql_scalar "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name='system_users';")"
[[ "$final_marker" == 1 ]] || {
    printf 'Database provision completed without the required system_users marker.\n' >&2
    exit 1
}
printf 'Database deploy-time provision completed successfully.\n'
