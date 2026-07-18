#!/usr/bin/env bash

set -Eeuo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
PODMAN_DIR="$(cd -- "${SCRIPT_DIR}/../.." && pwd)"

[[ $# -eq 1 ]] || {
    printf 'Usage: bash ./tests/marketing-provider-provision/run.sh <runtime-config.yaml>\n' >&2
    exit 2
}

# shellcheck source=../../lib/yaml-config.sh
source "${PODMAN_DIR}/lib/yaml-config.sh"
yaml_config_init "$1"
SOURCE_CONFIG="$YAML_CONFIG_PATH"
MYSQL_CONTAINER="$(yaml_require container.mysql)"
SOURCE_DATABASE="$(yaml_require mysql.database)"
MYSQL_PASSWORD="$(yaml_require mysql.root_password)"
MYSQL_CHARACTER_SET="$(yaml_require mysql.character_set)"
MYSQL_USER="$(yaml_require health.mysql_user)"

suffix="$$"
TEST_DATABASE="mitedtsm_provider_test_${suffix}"
SMS_TEMPLATE_CODE="crm-provider-sms-${suffix}"
MAIL_TEMPLATE_CODE="crm-provider-mail-${suffix}"
MAIL_ADDRESS="crm-provider-${suffix}@example.com"
TEMP_DIR="$(mktemp -d)"

mysql_command() {
    podman exec --env "MYSQL_PWD=${MYSQL_PASSWORD}" "$MYSQL_CONTAINER" \
        mysql "--default-character-set=${MYSQL_CHARACTER_SET}" --user="$MYSQL_USER" "$@"
}

cleanup() {
    mysql_command --execute "DROP DATABASE IF EXISTS ${TEST_DATABASE};" >/dev/null 2>&1 || true
    rm -rf -- "$TEMP_DIR"
}
trap cleanup EXIT

mysql_command --execute "
CREATE DATABASE ${TEST_DATABASE} CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE TABLE ${TEST_DATABASE}.system_sms_channel LIKE ${SOURCE_DATABASE}.system_sms_channel;
CREATE TABLE ${TEST_DATABASE}.system_sms_template LIKE ${SOURCE_DATABASE}.system_sms_template;
CREATE TABLE ${TEST_DATABASE}.system_mail_account LIKE ${SOURCE_DATABASE}.system_mail_account;
CREATE TABLE ${TEST_DATABASE}.system_mail_template LIKE ${SOURCE_DATABASE}.system_mail_template;
"

write_config() {
    local output="$1" mode="$2" version="$3" sms_enabled=true mail_enabled=true
    [[ "$mode" != disabled ]] || { sms_enabled=false; mail_enabled=false; }
    awk -v database="$TEST_DATABASE" -v mode="$mode" -v version="$version" \
        -v sms_enabled="$sms_enabled" -v mail_enabled="$mail_enabled" \
        -v sms_code="$SMS_TEMPLATE_CODE" -v mail_code="$MAIL_TEMPLATE_CODE" \
        -v mail_address="$MAIL_ADDRESS" '
        /^[^ ]/ { section=$1; sub(/:$/, "", section) }
        section=="operation" && /^  startup_mode:/ { print "  startup_mode: replace-server"; next }
        section=="mysql" && /^  database:/ { print "  database: " database; next }
        section=="crm_marketing" && /^  provider_mode:/ { print "  provider_mode: system"; next }
        section=="marketing_provider" && /^  provision_mode:/ { print "  provision_mode: " mode; next }
        section=="marketing_provider" && /^  sms_enabled:/ { print "  sms_enabled: " sms_enabled; next }
        section=="marketing_provider" && /^  sms_channel_code:/ { print "  sms_channel_code: ALIYUN"; next }
        section=="marketing_provider" && /^  sms_signature:/ { print "  sms_signature: CRM验收" version; next }
        section=="marketing_provider" && /^  sms_api_key:/ { print "  sms_api_key: acceptance-key-" version; next }
        section=="marketing_provider" && /^  sms_api_secret:/ { print "  sms_api_secret: acceptance-secret-" version; next }
        section=="marketing_provider" && /^  sms_template_code:/ { print "  sms_template_code: " sms_code; next }
        section=="marketing_provider" && /^  sms_template_name:/ { print "  sms_template_name: CRM Provider 验收 " version; next }
        section=="marketing_provider" && /^  sms_template_content:/ { print "  sms_template_content: \"Provider " version " {content}\""; next }
        section=="marketing_provider" && /^  sms_api_template_id:/ { print "  sms_api_template_id: provider-api-" version; next }
        section=="marketing_provider" && /^  mail_enabled:/ { print "  mail_enabled: " mail_enabled; next }
        section=="marketing_provider" && /^  mail_address:/ { print "  mail_address: " mail_address; next }
        section=="marketing_provider" && /^  mail_username:/ { print "  mail_username: provider-user-" version; next }
        section=="marketing_provider" && /^  mail_password:/ { print "  mail_password: provider-password-" version; next }
        section=="marketing_provider" && /^  mail_host:/ { print "  mail_host: smtp" version ".example.com"; next }
        section=="marketing_provider" && /^  mail_template_code:/ { print "  mail_template_code: " mail_code; next }
        section=="marketing_provider" && /^  mail_template_name:/ { print "  mail_template_name: CRM Provider 邮件 " version; next }
        section=="marketing_provider" && /^  mail_template_title:/ { print "  mail_template_title: Provider " version; next }
        section=="marketing_provider" && /^  mail_template_content:/ { print "  mail_template_content: \"<p>Provider " version " {content}</p>\""; next }
        { print }
    ' "$SOURCE_CONFIG" > "$output"
}

query_test() {
    mysql_command --database="$TEST_DATABASE" --batch --skip-column-names --execute "$1"
}

create_config="${TEMP_DIR}/create.yaml"
managed_config="${TEMP_DIR}/managed.yaml"
disabled_config="${TEMP_DIR}/disabled.yaml"
write_config "$create_config" create-only v1
write_config "$managed_config" managed v2
write_config "$disabled_config" disabled v3

bash "${PODMAN_DIR}/internal/provision-marketing-provider.sh" "$create_config"
bash "${PODMAN_DIR}/internal/provision-marketing-provider.sh" "$create_config"
[[ "$(query_test "SELECT COUNT(*) FROM system_sms_channel WHERE code='ALIYUN' AND deleted=b'0';")" == 1 ]]
[[ "$(query_test "SELECT COUNT(*) FROM system_sms_template WHERE code='${SMS_TEMPLATE_CODE}' AND deleted=b'0';")" == 1 ]]
[[ "$(query_test "SELECT COUNT(*) FROM system_mail_account WHERE mail='${MAIL_ADDRESS}' AND deleted=b'0';")" == 1 ]]
[[ "$(query_test "SELECT COUNT(*) FROM system_mail_template WHERE code='${MAIL_TEMPLATE_CODE}' AND deleted=b'0';")" == 1 ]]
printf 'ok 1 - create-only is idempotent for SMS and mail rows\n'

bash "${PODMAN_DIR}/internal/provision-marketing-provider.sh" "$managed_config"
[[ "$(query_test "SELECT signature FROM system_sms_channel WHERE code='ALIYUN' AND deleted=b'0';")" == CRM验收v2 ]]
[[ "$(query_test "SELECT name FROM system_sms_template WHERE code='${SMS_TEMPLATE_CODE}' AND deleted=b'0';")" == 'CRM Provider 验收 v2' ]]
[[ "$(query_test "SELECT host FROM system_mail_account WHERE mail='${MAIL_ADDRESS}' AND deleted=b'0';")" == smtpv2.example.com ]]
[[ "$(query_test "SELECT title FROM system_mail_template WHERE code='${MAIL_TEMPLATE_CODE}' AND deleted=b'0';")" == 'Provider v2' ]]
printf 'ok 2 - managed mode updates all stable-key aggregates\n'

bash "${PODMAN_DIR}/internal/provision-marketing-provider.sh" "$disabled_config"
[[ "$(query_test "SELECT signature FROM system_sms_channel WHERE code='ALIYUN' AND deleted=b'0';")" == CRM验收v2 ]]
[[ "$(query_test "SELECT host FROM system_mail_account WHERE mail='${MAIL_ADDRESS}' AND deleted=b'0';")" == smtpv2.example.com ]]
printf 'ok 3 - disabled mode preserves existing Provider configuration\n'

[[ "$(query_test "SELECT COUNT(*) FROM system_sms_template t JOIN system_sms_channel c ON c.id=t.channel_id WHERE t.code='${SMS_TEMPLATE_CODE}' AND c.code='ALIYUN';")" == 1 ]]
[[ "$(query_test "SELECT COUNT(*) FROM system_mail_template t JOIN system_mail_account a ON a.id=t.account_id WHERE t.code='${MAIL_TEMPLATE_CODE}' AND a.mail='${MAIL_ADDRESS}';")" == 1 ]]
printf 'ok 4 - templates reference their provisioned channel and account\n'
printf '1..4\n'
