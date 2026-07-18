#!/usr/bin/env bash

set -Eeuo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
PODMAN_DIR="$(cd -- "${SCRIPT_DIR}/../.." && pwd)"

[[ $# -eq 1 ]] || {
    printf 'Usage: bash ./tests/database-deploy-provision/run.sh <runtime-config.kdl>\n' >&2
    exit 2
}

# shellcheck source=../../lib/kdl-config.sh
source "${PODMAN_DIR}/lib/kdl-config.sh"
kdl_config_init "$1"
SOURCE_CONFIG="$KDL_CONFIG_PATH"
MYSQL_IMAGE="$(kdl_require image.mysql_base)"
MYSQL_PASSWORD="$(kdl_require mysql.root_password)"
MYSQL_CHARACTER_SET="$(kdl_require mysql.character_set)"
MYSQL_COLLATION="$(kdl_require mysql.collation)"
MYSQL_AUTHENTICATION_PLUGIN="$(kdl_require mysql.authentication_plugin)"
MYSQL_ADMIN_USERNAME="$(kdl_require mysql.administration_username)"
MYSQL_SQL_ROOT="$(kdl_path mysql.sql_root)"
BOOTSTRAP_MANIFEST="$(kdl_path mysql.bootstrap_manifest)"
COMPATIBILITY_MANIFEST="$(kdl_path mysql.compatibility_migration_manifest)"

suffix="$$"
MYSQL_CONTAINER="mitedtsm-db-provision-${suffix}"
MYSQL_VOLUME="mitedtsm-db-provision-${suffix}"
EMPTY_DATABASE="provider_empty_${suffix}"
PARTIAL_DATABASE="provider_partial_${suffix}"
REQUIRED_DATABASE="provider_required_${suffix}"
TEMP_DIR="$(mktemp -d)"

cleanup() {
    podman rm --force "$MYSQL_CONTAINER" >/dev/null 2>&1 || true
    podman volume rm "$MYSQL_VOLUME" >/dev/null 2>&1 || true
    rm -rf -- "$TEMP_DIR"
}
trap cleanup EXIT

podman volume create "$MYSQL_VOLUME" >/dev/null
podman run -d --replace --name "$MYSQL_CONTAINER" --network none --pull=never \
    --volume "${MYSQL_VOLUME}:/var/lib/mysql" \
    --env "MYSQL_DATABASE=${EMPTY_DATABASE}" \
    --env "MYSQL_ROOT_PASSWORD=${MYSQL_PASSWORD}" \
    "$MYSQL_IMAGE" \
    "--character-set-server=${MYSQL_CHARACTER_SET}" \
    "--collation-server=${MYSQL_COLLATION}" \
    "--default-authentication-plugin=${MYSQL_AUTHENTICATION_PLUGIN}" >/dev/null

for ((attempt=1; attempt<=120; attempt++)); do
    if podman exec --env "MYSQL_PWD=${MYSQL_PASSWORD}" "$MYSQL_CONTAINER" \
        mysql --user "$MYSQL_ADMIN_USERNAME" --batch --skip-column-names \
        --execute 'SELECT 1' >/dev/null 2>&1; then
        break
    fi
    ((attempt < 120)) || {
        printf 'Temporary MySQL did not become ready.\n' >&2
        podman logs --tail 80 "$MYSQL_CONTAINER" >&2 || true
        exit 1
    }
    sleep 1
done

mysql_command() {
    podman exec --env "MYSQL_PWD=${MYSQL_PASSWORD}" "$MYSQL_CONTAINER" \
        mysql "--default-character-set=${MYSQL_CHARACTER_SET}" --user="$MYSQL_ADMIN_USERNAME" "$@"
}

write_config() {
    local output="$1" database="$2" policy="$3" dataset="$4" dataset_manifest
    dataset_manifest="$(realpath -m -- "${PODMAN_DIR}/../database/datasets/${dataset}.manifest")"
    cp "$SOURCE_CONFIG" "$output"
    kdl_set_file "$output" operation.startup_mode string replace-server
    kdl_set_file "$output" container.mysql string "$MYSQL_CONTAINER"
    kdl_set_file "$output" mysql.sql_root string "$MYSQL_SQL_ROOT"
    kdl_set_file "$output" mysql.database string "$database"
    kdl_set_file "$output" mysql.dataset string "$dataset"
    kdl_set_file "$output" mysql.dataset_manifest string "$dataset_manifest"
    kdl_set_file "$output" mysql.bootstrap_policy string "$policy"
    kdl_set_file "$output" mysql.bootstrap_manifest string "$BOOTSTRAP_MANIFEST"
    kdl_set_file "$output" mysql.compatibility_migration_manifest string "$COMPATIBILITY_MANIFEST"
}

empty_config="${TEMP_DIR}/empty.kdl"
partial_config="${TEMP_DIR}/partial.kdl"
required_config="${TEMP_DIR}/required.kdl"
write_config "$empty_config" "$EMPTY_DATABASE" initialize-empty none
write_config "$partial_config" "$PARTIAL_DATABASE" initialize-empty none
write_config "$required_config" "$REQUIRED_DATABASE" require-existing none

bash "${PODMAN_DIR}/internal/provision-database.sh" "$empty_config"
[[ "$(mysql_command --database="$EMPTY_DATABASE" --batch --skip-column-names \
    --execute "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE() AND table_type='BASE TABLE';")" -gt 300 ]]
[[ "$(mysql_command --database="$EMPTY_DATABASE" --batch --skip-column-names \
    --execute 'SELECT COUNT(*) FROM system_users;')" == 1 ]]
[[ "$(mysql_command --database="$EMPTY_DATABASE" --batch --skip-column-names \
    --execute 'SELECT COUNT(*) FROM crm_customer;')" == 0 ]]
printf 'ok 1 - confirmed empty database receives bootstrap, compatibility, and selected dataset\n'

bash "${PODMAN_DIR}/internal/provision-database.sh" "$empty_config"
[[ "$(mysql_command --database="$EMPTY_DATABASE" --batch --skip-column-names \
    --execute 'SELECT COUNT(*) FROM system_users;')" == 1 ]]
printf 'ok 2 - existing schema preserves data and only reapplies idempotent compatibility SQL\n'

mysql_command --execute "CREATE DATABASE ${PARTIAL_DATABASE}; CREATE TABLE ${PARTIAL_DATABASE}.unexpected(id INT PRIMARY KEY);"
set +e
bash "${PODMAN_DIR}/internal/provision-database.sh" "$partial_config" >/dev/null 2>&1
partial_status=$?
set -e
[[ "$partial_status" == 1 ]]
printf 'ok 3 - non-empty unrecognized database is never destructively bootstrapped\n'

mysql_command --execute "CREATE DATABASE ${REQUIRED_DATABASE};"
set +e
bash "${PODMAN_DIR}/internal/provision-database.sh" "$required_config" >/dev/null 2>&1
required_status=$?
set -e
[[ "$required_status" == 1 ]]
printf 'ok 4 - require-existing rejects an empty database\n'

printf '1..4\n'
