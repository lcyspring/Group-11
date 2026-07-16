#!/usr/bin/env bash

set -Eeuo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"

[[ $# -eq 1 ]] || {
    printf 'Usage: bash ./database-restore.sh <restore-config.yaml>\n' >&2
    exit 2
}

# shellcheck source=lib/yaml-config.sh
source "${SCRIPT_DIR}/lib/yaml-config.sh"
yaml_config_init "$1"

MODE="$(yaml_require operation.mode)"
MYSQL_CONTAINER="$(yaml_require container.mysql)"
SERVER_CONTAINER="$(yaml_require container.server)"
SOURCE_DATABASE="$(yaml_require mysql.database)"
MYSQL_USERNAME="$(yaml_require mysql.username)"
MYSQL_PASSWORD="$(yaml_require mysql.password)"
ARCHIVE_DIR="$(yaml_path archive.directory)"
ARCHIVE_FILENAME="$(yaml_require archive.filename)"
ARCHIVE_PATH="${ARCHIVE_DIR}/${ARCHIVE_FILENAME}"
CHECKSUM_PATH="${ARCHIVE_PATH}.sha256"
TARGET_DATABASE="$(yaml_require restore.target_database)"
ALLOW_REPLACE="$(yaml_bool restore.allow_replace)"
ALLOW_LIVE_REPLACE="$(yaml_bool restore.allow_live_database_replace)"
DROP_AFTER_VERIFY="$(yaml_bool restore.drop_after_verify)"

[[ "$MODE" == "check" || "$MODE" == "restore" ]] || {
    printf 'operation.mode must be check or restore; got: %s\n' "$MODE" >&2
    exit 2
}
[[ "$TARGET_DATABASE" =~ ^[A-Za-z0-9_]+$ ]] || {
    printf 'restore.target_database must be a plain MySQL identifier.\n' >&2
    exit 2
}
command -v podman >/dev/null
command -v gzip >/dev/null
command -v sha256sum >/dev/null

if [[ "$MODE" == "check" ]]; then
    printf 'Database restore preflight passed. No database was changed.\n'
    exit 0
fi

[[ -s "$ARCHIVE_PATH" && -s "$CHECKSUM_PATH" ]] || {
    printf 'Backup archive or checksum is missing.\n' >&2
    exit 1
}
if ! (
    cd -- "$ARCHIVE_DIR"
    sha256sum --check --status "${ARCHIVE_FILENAME}.sha256"
); then
    printf 'Backup checksum verification failed: %s\n' "$ARCHIVE_PATH" >&2
    exit 1
fi
if ! gzip -t "$ARCHIVE_PATH"; then
    printf 'Backup gzip verification failed: %s\n' "$ARCHIVE_PATH" >&2
    exit 1
fi

podman container exists "$MYSQL_CONTAINER" || {
    printf 'MySQL container does not exist: %s\n' "$MYSQL_CONTAINER" >&2
    exit 1
}
[[ "$(podman inspect --format '{{.State.Running}}' "$MYSQL_CONTAINER")" == "true" ]] || {
    printf 'MySQL container is not running: %s\n' "$MYSQL_CONTAINER" >&2
    exit 1
}
if [[ "$TARGET_DATABASE" == "$SOURCE_DATABASE" && "$ALLOW_LIVE_REPLACE" != "true" ]]; then
    printf 'Replacing the configured live database requires restore.allow_live_database_replace: true.\n' >&2
    exit 1
fi
if [[ "$TARGET_DATABASE" == "$SOURCE_DATABASE" ]] && \
    podman container exists "$SERVER_CONTAINER" && \
    [[ "$(podman inspect --format '{{.State.Running}}' "$SERVER_CONTAINER")" == "true" ]]; then
    printf 'Stop the Server container before replacing the live database: %s\n' "$SERVER_CONTAINER" >&2
    exit 1
fi

database_exists="$(podman exec --env "MYSQL_PWD=${MYSQL_PASSWORD}" "$MYSQL_CONTAINER" \
    mysql --user="$MYSQL_USERNAME" -Nse \
    "SELECT COUNT(*) FROM information_schema.schemata WHERE schema_name='${TARGET_DATABASE}';")"
if [[ "$database_exists" != "0" && "$ALLOW_REPLACE" != "true" ]]; then
    printf 'Restore target exists and restore.allow_replace is false: %s\n' "$TARGET_DATABASE" >&2
    exit 1
fi

podman exec --env "MYSQL_PWD=${MYSQL_PASSWORD}" "$MYSQL_CONTAINER" mysql --user="$MYSQL_USERNAME" -e \
    "DROP DATABASE IF EXISTS \`${TARGET_DATABASE}\`; CREATE DATABASE \`${TARGET_DATABASE}\` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"

gzip -dc "$ARCHIVE_PATH" | podman exec -i --env "MYSQL_PWD=${MYSQL_PASSWORD}" "$MYSQL_CONTAINER" \
    mysql --user="$MYSQL_USERNAME" --database="$TARGET_DATABASE"

core_tables="$(podman exec --env "MYSQL_PWD=${MYSQL_PASSWORD}" "$MYSQL_CONTAINER" \
    mysql --user="$MYSQL_USERNAME" --database="$TARGET_DATABASE" -Nse \
    "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name IN ('system_users','crm_customer','crm_contract','crm_receivable','crm_invoice','crm_reimbursement','crm_work_order');")"
[[ "$core_tables" == "7" ]] || {
    printf 'Restored database failed CRM core-table verification: %s/7\n' "$core_tables" >&2
    exit 1
}

table_count="$(podman exec --env "MYSQL_PWD=${MYSQL_PASSWORD}" "$MYSQL_CONTAINER" \
    mysql --user="$MYSQL_USERNAME" --database="$TARGET_DATABASE" -Nse \
    "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE();")"

if [[ "$DROP_AFTER_VERIFY" == "true" ]]; then
    podman exec --env "MYSQL_PWD=${MYSQL_PASSWORD}" "$MYSQL_CONTAINER" mysql --user="$MYSQL_USERNAME" -e \
        "DROP DATABASE \`${TARGET_DATABASE}\`;"
fi

printf 'database-restore=ok target=%s tables=%s dropped_after_verify=%s\n' \
    "$TARGET_DATABASE" "$table_count" "$DROP_AFTER_VERIFY"
