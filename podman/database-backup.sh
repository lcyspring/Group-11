#!/usr/bin/env bash

set -Eeuo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"

[[ $# -eq 1 ]] || {
    printf 'Usage: bash ./database-backup.sh <backup-config.yaml>\n' >&2
    exit 2
}

# shellcheck source=lib/yaml-config.sh
source "${SCRIPT_DIR}/lib/yaml-config.sh"
yaml_config_init "$1"

MODE="$(yaml_require operation.mode)"
MYSQL_CONTAINER="$(yaml_require container.mysql)"
MYSQL_DATABASE="$(yaml_require mysql.database)"
MYSQL_USERNAME="$(yaml_require mysql.username)"
MYSQL_PASSWORD="$(yaml_require mysql.password)"
ARCHIVE_DIR="$(yaml_path archive.directory)"
ARCHIVE_FILENAME="$(yaml_require archive.filename)"
OVERWRITE="$(yaml_bool archive.overwrite)"
ARCHIVE_PATH="${ARCHIVE_DIR}/${ARCHIVE_FILENAME}"
CHECKSUM_PATH="${ARCHIVE_PATH}.sha256"

[[ "$MODE" == "check" || "$MODE" == "backup" ]] || {
    printf 'operation.mode must be check or backup; got: %s\n' "$MODE" >&2
    exit 2
}
[[ "$ARCHIVE_FILENAME" =~ ^[A-Za-z0-9._-]+\.sql\.gz$ ]] || {
    printf 'archive.filename must be a plain .sql.gz filename.\n' >&2
    exit 2
}
command -v podman >/dev/null
command -v gzip >/dev/null
command -v sha256sum >/dev/null

if [[ "$MODE" == "check" ]]; then
    printf 'Database backup preflight passed. No archive was written.\n'
    exit 0
fi

podman container exists "$MYSQL_CONTAINER" || {
    printf 'MySQL container does not exist: %s\n' "$MYSQL_CONTAINER" >&2
    exit 1
}
[[ "$(podman inspect --format '{{.State.Running}}' "$MYSQL_CONTAINER")" == "true" ]] || {
    printf 'MySQL container is not running: %s\n' "$MYSQL_CONTAINER" >&2
    exit 1
}

mkdir -p -- "$ARCHIVE_DIR"
if [[ "$OVERWRITE" != "true" && ( -e "$ARCHIVE_PATH" || -e "$CHECKSUM_PATH" ) ]]; then
    printf 'Backup archive already exists and overwrite is false: %s\n' "$ARCHIVE_PATH" >&2
    exit 1
fi

temporary_archive="$(mktemp "${ARCHIVE_DIR}/.${ARCHIVE_FILENAME}.XXXXXX")"
trap 'rm -f -- "$temporary_archive"' EXIT

podman exec --env "MYSQL_PWD=${MYSQL_PASSWORD}" "$MYSQL_CONTAINER" \
    mysqldump --user="$MYSQL_USERNAME" --single-transaction --quick --routines --triggers --events \
    --hex-blob --set-gtid-purged=OFF "$MYSQL_DATABASE" | gzip -9 > "$temporary_archive"
gzip -t "$temporary_archive"
mv -f -- "$temporary_archive" "$ARCHIVE_PATH"
trap - EXIT

(
    cd -- "$ARCHIVE_DIR"
    sha256sum -- "$ARCHIVE_FILENAME" > "${ARCHIVE_FILENAME}.sha256"
)

printf 'database-backup=ok archive=%s\n' "$ARCHIVE_PATH"
