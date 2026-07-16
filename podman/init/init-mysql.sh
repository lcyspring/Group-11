#!/usr/bin/env bash

set -Eeuo pipefail

readonly SQL_ROOT="/podman-init-sql"
readonly BOOTSTRAP_MANIFEST="${SQL_ROOT}/manifests/mysql-bootstrap.manifest"

mysql_utf8() {
    command mysql --default-character-set=utf8mb4 "$@"
}

require_file() {
    [[ -s "$1" ]] || {
        printf 'Required SQL file is missing or empty: %s\n' "$1" >&2
        exit 1
    }
}

resolve_manifest_entry() {
    local manifest_dir="$1"
    local entry="$2"
    local resolved

    [[ "$entry" != /* ]] || {
        printf 'Absolute SQL manifest paths are forbidden: %s\n' "$entry" >&2
        return 1
    }
    resolved="$(realpath -m -- "${manifest_dir}/${entry}")"
    [[ "$resolved" == "${SQL_ROOT}/"* ]] || {
        printf 'SQL manifest path escapes database root: %s\n' "$entry" >&2
        return 1
    }
    printf '%s' "$resolved"
}

execute_manifest() {
    local manifest="$1"
    local manifest_dir entry sql_file

    require_file "$manifest"
    manifest_dir="$(dirname -- "$manifest")"
    while IFS= read -r entry || [[ -n "$entry" ]]; do
        [[ -n "$entry" && "$entry" != \#* ]] || continue
        sql_file="$(resolve_manifest_entry "$manifest_dir" "$entry")"
        require_file "$sql_file"
        printf 'Executing bootstrap SQL: %s\n' "${sql_file#${SQL_ROOT}/}"
        mysql_utf8 -uroot -p"${MYSQL_ROOT_PASSWORD}" "${MYSQL_DATABASE}" < "$sql_file"
    done < "$manifest"
}

printf 'Waiting for MySQL bootstrap connection.\n'
until mysql_utf8 -uroot -p"${MYSQL_ROOT_PASSWORD}" -e 'SELECT 1' >/dev/null 2>&1; do
    sleep 2
done

printf 'Executing explicit MySQL bootstrap manifest.\n'
execute_manifest "$BOOTSTRAP_MANIFEST"
printf 'MySQL bootstrap completed successfully.\n'
