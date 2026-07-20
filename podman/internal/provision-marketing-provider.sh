#!/usr/bin/env bash

# Idempotently provisions one CRM marketing SMS channel/template and/or one
# mail account/template from the runtime KDL. Secrets are base64-encoded into
# SQL sent through stdin and are never printed or passed as mysql arguments.

set -Eeuo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
PODMAN_DIR="$(cd -- "${SCRIPT_DIR}/.." && pwd)"
CONTAINER_ENGINE="${CONTAINER_ENGINE:-podman}"

[[ $# -eq 1 ]] || {
    printf 'Usage: bash ./internal/provision-marketing-provider.sh <runtime-config.kdl>\n' >&2
    exit 2
}

# shellcheck source=../lib/kdl-config.sh
source "${PODMAN_DIR}/lib/kdl-config.sh"
kdl_config_init "$1"
[[ "$(kdl_require schema_version)" == 1 ]] || exit 2

START_MODE="$(kdl_require operation.startup_mode)"
MYSQL_CONTAINER="$(kdl_require container.mysql)"
MYSQL_DATABASE="$(kdl_require mysql.database)"
MYSQL_ROOT_PASSWORD="$(kdl_require mysql.root_password)"
MYSQL_CHARACTER_SET="$(kdl_require mysql.character_set)"
MYSQL_ADMIN_USERNAME="$(kdl_require mysql.administration_username)"
CRM_PROVIDER_MODE="$(kdl_require crm_marketing.provider_mode)"
PROVISION_MODE="$(kdl_require marketing_provider.provision_mode)"

SMS_ENABLED="$(kdl_bool marketing_provider.sms_enabled)"
SMS_CHANNEL_CODE="$(kdl_require marketing_provider.sms_channel_code)"
SMS_SIGNATURE="$(kdl_require marketing_provider.sms_signature)"
SMS_API_KEY="$(kdl_require marketing_provider.sms_api_key)"
SMS_API_SECRET="$(kdl_require marketing_provider.sms_api_secret)"
SMS_CALLBACK_URL="$(kdl_require marketing_provider.sms_callback_url)"
SMS_TEMPLATE_CODE="$(kdl_require marketing_provider.sms_template_code)"
SMS_TEMPLATE_NAME="$(kdl_require marketing_provider.sms_template_name)"
SMS_TEMPLATE_CONTENT="$(kdl_require marketing_provider.sms_template_content)"
SMS_TEMPLATE_PARAMS="$(kdl_require marketing_provider.sms_template_params)"
SMS_API_TEMPLATE_ID="$(kdl_require marketing_provider.sms_api_template_id)"

MAIL_ENABLED="$(kdl_bool marketing_provider.mail_enabled)"
MAIL_ADDRESS="$(kdl_require marketing_provider.mail_address)"
MAIL_USERNAME="$(kdl_require marketing_provider.mail_username)"
MAIL_PASSWORD="$(kdl_require marketing_provider.mail_password)"
MAIL_HOST="$(kdl_require marketing_provider.mail_host)"
MAIL_PORT="$(kdl_port marketing_provider.mail_port)"
MAIL_SSL_ENABLED="$(kdl_bool marketing_provider.mail_ssl_enabled)"
MAIL_STARTTLS_ENABLED="$(kdl_bool marketing_provider.mail_starttls_enabled)"
MAIL_TEMPLATE_CODE="$(kdl_require marketing_provider.mail_template_code)"
MAIL_TEMPLATE_NAME="$(kdl_require marketing_provider.mail_template_name)"
MAIL_TEMPLATE_NICKNAME="$(kdl_require marketing_provider.mail_template_nickname)"
MAIL_TEMPLATE_TITLE="$(kdl_require marketing_provider.mail_template_title)"
MAIL_TEMPLATE_CONTENT="$(kdl_require marketing_provider.mail_template_content)"
MAIL_TEMPLATE_PARAMS="$(kdl_require marketing_provider.mail_template_params)"

case "$PROVISION_MODE" in
    disabled|create-only|managed) ;;
    *)
        printf 'marketing_provider.provision_mode must be disabled, create-only, or managed; got: %s\n' \
            "$PROVISION_MODE" >&2
        exit 2
        ;;
esac
[[ "$MYSQL_CONTAINER" =~ ^[A-Za-z0-9][A-Za-z0-9_.-]*$ ]] || exit 2
[[ "$MYSQL_DATABASE" =~ ^[A-Za-z0-9_]+$ ]] || exit 2
[[ "$MYSQL_CHARACTER_SET" =~ ^[A-Za-z0-9_]+$ ]] || exit 2
[[ "$MYSQL_ADMIN_USERNAME" == root ]] || exit 2

command -v jq >/dev/null 2>&1 || {
    printf 'jq is required to validate Provider template parameter arrays.\n' >&2
    exit 1
}
command -v base64 >/dev/null 2>&1 || {
    printf 'base64 is required for safe Provider SQL transport.\n' >&2
    exit 1
}

valid_code() {
    [[ "$1" =~ ^[A-Za-z0-9][A-Za-z0-9._-]{0,62}$ ]]
}

valid_params() {
    jq -e 'type == "array" and all(.[]; type == "string")' >/dev/null <<< "$1"
}

is_placeholder() {
    case "${1,,}" in
        not-configured|change-me|changeme|example|example-value|none|'') return 0 ;;
        *) return 1 ;;
    esac
}

valid_code "$SMS_TEMPLATE_CODE" || {
    printf 'marketing_provider.sms_template_code is invalid.\n' >&2
    exit 2
}
valid_code "$MAIL_TEMPLATE_CODE" || {
    printf 'marketing_provider.mail_template_code is invalid.\n' >&2
    exit 2
}
valid_params "$SMS_TEMPLATE_PARAMS" || {
    printf 'marketing_provider.sms_template_params must be a JSON string array.\n' >&2
    exit 2
}
valid_params "$MAIL_TEMPLATE_PARAMS" || {
    printf 'marketing_provider.mail_template_params must be a JSON string array.\n' >&2
    exit 2
}
(( ${#SMS_TEMPLATE_NAME} <= 63 && ${#SMS_TEMPLATE_CONTENT} <= 255 )) || {
    printf 'SMS template name or content exceeds the System module column limit.\n' >&2
    exit 2
}
(( ${#MAIL_TEMPLATE_NAME} <= 63 && ${#MAIL_TEMPLATE_TITLE} <= 255 && ${#MAIL_TEMPLATE_CONTENT} <= 10240 )) || {
    printf 'Mail template name, title, or content exceeds the System module column limit.\n' >&2
    exit 2
}

if [[ "$PROVISION_MODE" == disabled ]]; then
    [[ "$SMS_ENABLED" == false && "$MAIL_ENABLED" == false ]] || {
        printf 'Disabled Provider provision requires sms_enabled=false and mail_enabled=false.\n' >&2
        exit 2
    }
else
    [[ "$CRM_PROVIDER_MODE" == system ]] || {
        printf 'Provider provision requires crm_marketing.provider_mode=system.\n' >&2
        exit 2
    }
    [[ "$SMS_ENABLED" == true || "$MAIL_ENABLED" == true ]] || {
        printf 'Provider provision must enable at least one of SMS or mail.\n' >&2
        exit 2
    }
fi

if [[ "$SMS_ENABLED" == true ]]; then
    case "$SMS_CHANNEL_CODE" in
        ALIYUN|TENCENT|HUAWEI|QINIU|DEBUG_DING_TALK) ;;
        *)
            printf 'marketing_provider.sms_channel_code is not supported by the System module.\n' >&2
            exit 2
            ;;
    esac
    (( ${#SMS_SIGNATURE} > 0 && ${#SMS_SIGNATURE} <= 12 )) || {
        printf 'marketing_provider.sms_signature must contain 1 to 12 characters.\n' >&2
        exit 2
    }
    is_placeholder "$SMS_API_KEY" && {
        printf 'marketing_provider.sms_api_key must be configured when SMS provision is enabled.\n' >&2
        exit 2
    }
    is_placeholder "$SMS_API_SECRET" && {
        printf 'marketing_provider.sms_api_secret must be configured when SMS provision is enabled.\n' >&2
        exit 2
    }
    is_placeholder "$SMS_API_TEMPLATE_ID" && {
        printf 'marketing_provider.sms_api_template_id must be configured when SMS provision is enabled.\n' >&2
        exit 2
    }
    [[ "$SMS_CALLBACK_URL" == none || "$SMS_CALLBACK_URL" =~ ^https?://[^[:space:]]+$ ]] || {
        printf 'marketing_provider.sms_callback_url must be none or an HTTP(S) URL.\n' >&2
        exit 2
    }
fi

if [[ "$MAIL_ENABLED" == true ]]; then
    [[ "$MAIL_ADDRESS" =~ ^[^[:space:]@]+@[^[:space:]@]+\.[^[:space:]@]+$ ]] || {
        printf 'marketing_provider.mail_address must be an email address.\n' >&2
        exit 2
    }
    is_placeholder "$MAIL_USERNAME" && {
        printf 'marketing_provider.mail_username must be configured when mail provision is enabled.\n' >&2
        exit 2
    }
    is_placeholder "$MAIL_PASSWORD" && {
        printf 'marketing_provider.mail_password must be configured when mail provision is enabled.\n' >&2
        exit 2
    }
    [[ "$MAIL_HOST" =~ ^[A-Za-z0-9.-]+$ ]] || {
        printf 'marketing_provider.mail_host must be a plain host name or address.\n' >&2
        exit 2
    }
    [[ "$MAIL_SSL_ENABLED" == false || "$MAIL_STARTTLS_ENABLED" == false ]] || {
        printf 'Mail SSL and STARTTLS cannot both be enabled.\n' >&2
        exit 2
    }
fi

if [[ "$START_MODE" == check ]]; then
    printf 'Marketing Provider preflight passed: mode=%s sms=%s mail=%s. No account was changed.\n' \
        "$PROVISION_MODE" "$SMS_ENABLED" "$MAIL_ENABLED"
    exit 0
fi
case "$START_MODE" in
    replace|replace-server) ;;
    *)
        printf 'Marketing Provider provision skipped for startup_mode=%s.\n' "$START_MODE"
        exit 0
        ;;
esac
[[ "$PROVISION_MODE" != disabled ]] || {
    printf 'Marketing Provider provision is disabled; existing database accounts were preserved.\n'
    exit 0
}

case "$CONTAINER_ENGINE" in
    podman|docker) ;;
    *)
        printf 'CONTAINER_ENGINE must be podman or docker; got: %s\n' "$CONTAINER_ENGINE" >&2
        exit 2
        ;;
esac
command -v "$CONTAINER_ENGINE" >/dev/null 2>&1 || {
    printf 'Container engine is required for Provider provision: %s\n' "$CONTAINER_ENGINE" >&2
    exit 1
}

container_cmd() {
    "$CONTAINER_ENGINE" "$@"
}

b64() {
    printf '%s' "$1" | base64 --wrap=0
}

mysql_command() {
    container_cmd exec --env "MYSQL_PWD=${MYSQL_ROOT_PASSWORD}" "$MYSQL_CONTAINER" \
        mysql "--default-character-set=${MYSQL_CHARACTER_SET}" \
        "--user=${MYSQL_ADMIN_USERNAME}" "--database=${MYSQL_DATABASE}" "$@"
}

mysql_scalar() {
    mysql_command --batch --skip-column-names --execute "$1"
}

sql_text() {
    printf "CONVERT(FROM_BASE64('%s') USING utf8mb4) COLLATE utf8mb4_unicode_ci" "$(b64 "$1")"
}

check_single_active() {
    local table="$1" column="$2" value="$3" count
    count="$(mysql_scalar "SELECT COUNT(*) FROM ${table} WHERE ${column}=$(sql_text "$value") AND deleted=b'0';")"
    [[ "$count" =~ ^[0-9]+$ ]] || return 1
    ((count <= 1)) || {
        printf 'Provider provision found duplicate active %s.%s rows for stable key %s.\n' \
            "$table" "$column" "$value" >&2
        return 1
    }
}

if [[ "$SMS_ENABLED" == true ]]; then
    check_single_active system_sms_channel code "$SMS_CHANNEL_CODE"
    check_single_active system_sms_template code "$SMS_TEMPLATE_CODE"
fi
if [[ "$MAIL_ENABLED" == true ]]; then
    check_single_active system_mail_account mail "$MAIL_ADDRESS"
    check_single_active system_mail_template code "$MAIL_TEMPLATE_CODE"
fi

managed=0
[[ "$PROVISION_MODE" == managed ]] && managed=1
sms_enabled=0
[[ "$SMS_ENABLED" == true ]] && sms_enabled=1
mail_enabled=0
[[ "$MAIL_ENABLED" == true ]] && mail_enabled=1
mail_ssl=0
[[ "$MAIL_SSL_ENABLED" == true ]] && mail_ssl=1
mail_starttls=0
[[ "$MAIL_STARTTLS_ENABLED" == true ]] && mail_starttls=1

container_cmd exec --env "MYSQL_PWD=${MYSQL_ROOT_PASSWORD}" -i "$MYSQL_CONTAINER" \
    mysql "--default-character-set=${MYSQL_CHARACTER_SET}" \
    "--user=${MYSQL_ADMIN_USERNAME}" "--database=${MYSQL_DATABASE}" <<SQL
SET @managed=${managed}, @sms_enabled=${sms_enabled}, @mail_enabled=${mail_enabled};
SET @sms_channel_code=$(sql_text "$SMS_CHANNEL_CODE"), @sms_signature=$(sql_text "$SMS_SIGNATURE"),
    @sms_api_key=$(sql_text "$SMS_API_KEY"), @sms_api_secret=$(sql_text "$SMS_API_SECRET"),
    @sms_callback_url=$(sql_text "$SMS_CALLBACK_URL"), @sms_template_code=$(sql_text "$SMS_TEMPLATE_CODE"),
    @sms_template_name=$(sql_text "$SMS_TEMPLATE_NAME"), @sms_template_content=$(sql_text "$SMS_TEMPLATE_CONTENT"),
    @sms_template_params=$(sql_text "$SMS_TEMPLATE_PARAMS"), @sms_api_template_id=$(sql_text "$SMS_API_TEMPLATE_ID");
SET @mail_address=$(sql_text "$MAIL_ADDRESS"), @mail_username=$(sql_text "$MAIL_USERNAME"),
    @mail_password=$(sql_text "$MAIL_PASSWORD"), @mail_host=$(sql_text "$MAIL_HOST"),
    @mail_port=${MAIL_PORT}, @mail_ssl=${mail_ssl}, @mail_starttls=${mail_starttls},
    @mail_template_code=$(sql_text "$MAIL_TEMPLATE_CODE"), @mail_template_name=$(sql_text "$MAIL_TEMPLATE_NAME"),
    @mail_template_nickname=$(sql_text "$MAIL_TEMPLATE_NICKNAME"), @mail_template_title=$(sql_text "$MAIL_TEMPLATE_TITLE"),
    @mail_template_content=$(sql_text "$MAIL_TEMPLATE_CONTENT"), @mail_template_params=$(sql_text "$MAIL_TEMPLATE_PARAMS");

START TRANSACTION;
INSERT INTO system_sms_channel
    (signature, code, status, remark, api_key, api_secret, callback_url, creator, updater, deleted)
SELECT @sms_signature, @sms_channel_code, 0, 'managed-by:podman-kdl', @sms_api_key, @sms_api_secret,
       NULLIF(@sms_callback_url, 'none'), '1', '1', b'0'
WHERE @sms_enabled=1 AND NOT EXISTS (
    SELECT 1 FROM system_sms_channel WHERE code=@sms_channel_code AND deleted=b'0');
SET @sms_channel_id=(SELECT id FROM system_sms_channel WHERE code=@sms_channel_code AND deleted=b'0' LIMIT 1);
UPDATE system_sms_channel
SET signature=@sms_signature, status=0, remark='managed-by:podman-kdl', api_key=@sms_api_key,
    api_secret=@sms_api_secret, callback_url=NULLIF(@sms_callback_url, 'none'), updater='1'
WHERE @sms_enabled=1 AND @managed=1 AND id=@sms_channel_id;

INSERT INTO system_sms_template
    (type, status, code, name, content, params, remark, api_template_id, channel_id, channel_code, creator, updater, deleted)
SELECT 3, 0, @sms_template_code, @sms_template_name, @sms_template_content, @sms_template_params,
       'managed-by:podman-kdl', @sms_api_template_id, @sms_channel_id, @sms_channel_code, '1', '1', b'0'
WHERE @sms_enabled=1 AND NOT EXISTS (
    SELECT 1 FROM system_sms_template WHERE code=@sms_template_code AND deleted=b'0');
UPDATE system_sms_template
SET type=3, status=0, name=@sms_template_name, content=@sms_template_content, params=@sms_template_params,
    remark='managed-by:podman-kdl', api_template_id=@sms_api_template_id, channel_id=@sms_channel_id,
    channel_code=@sms_channel_code, updater='1'
WHERE @sms_enabled=1 AND @managed=1 AND code=@sms_template_code AND deleted=b'0';

INSERT INTO system_mail_account
    (mail, username, password, host, port, ssl_enable, starttls_enable, creator, updater, deleted)
SELECT @mail_address, @mail_username, @mail_password, @mail_host, @mail_port,
       IF(@mail_ssl=1,b'1',b'0'), IF(@mail_starttls=1,b'1',b'0'), '1', '1', b'0'
WHERE @mail_enabled=1 AND NOT EXISTS (
    SELECT 1 FROM system_mail_account WHERE mail=@mail_address AND deleted=b'0');
SET @mail_account_id=(SELECT id FROM system_mail_account WHERE mail=@mail_address AND deleted=b'0' LIMIT 1);
UPDATE system_mail_account
SET username=@mail_username, password=@mail_password, host=@mail_host, port=@mail_port,
    ssl_enable=IF(@mail_ssl=1,b'1',b'0'), starttls_enable=IF(@mail_starttls=1,b'1',b'0'), updater='1'
WHERE @mail_enabled=1 AND @managed=1 AND id=@mail_account_id;

INSERT INTO system_mail_template
    (name, code, account_id, nickname, title, content, params, status, remark, creator, updater, deleted)
SELECT @mail_template_name, @mail_template_code, @mail_account_id, @mail_template_nickname,
       @mail_template_title, @mail_template_content, @mail_template_params, 0,
       'managed-by:podman-kdl', '1', '1', b'0'
WHERE @mail_enabled=1 AND NOT EXISTS (
    SELECT 1 FROM system_mail_template WHERE code=@mail_template_code AND deleted=b'0');
UPDATE system_mail_template
SET name=@mail_template_name, account_id=@mail_account_id, nickname=@mail_template_nickname,
    title=@mail_template_title, content=@mail_template_content, params=@mail_template_params,
    status=0, remark='managed-by:podman-kdl', updater='1'
WHERE @mail_enabled=1 AND @managed=1 AND code=@mail_template_code AND deleted=b'0';
COMMIT;
SQL

printf 'Marketing Provider provision completed: mode=%s sms=%s mail=%s.\n' \
    "$PROVISION_MODE" "$SMS_ENABLED" "$MAIL_ENABLED"
