#!/usr/bin/env bash

# Validates and idempotently provisions the four System SMS templates used by BPM.
# The only public input is the runtime KDL; it points to the explicit template KDL.

set -Eeuo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
PODMAN_DIR="$(cd -- "${SCRIPT_DIR}/.." && pwd)"

[[ $# -eq 1 ]] || {
    printf 'Usage: bash ./internal/provision-bpm-notifications.sh <runtime-config.kdl>\n' >&2
    exit 2
}

# shellcheck source=../lib/kdl-config.sh
source "${PODMAN_DIR}/lib/kdl-config.sh"
kdl_config_init "$1"
[[ "$(kdl_require schema_version)" == 1 ]] || exit 2

START_MODE="$(kdl_require operation.startup_mode)"
SMS_ENABLED="$(kdl_bool bpm.notification_sms_enabled)"
TEMPLATE_CONFIG="$(kdl_path bpm.notification_template_config)"
MYSQL_CONTAINER="$(kdl_require container.mysql)"
MYSQL_DATABASE="$(kdl_require mysql.database)"
MYSQL_ROOT_PASSWORD="$(kdl_require mysql.root_password)"
MYSQL_CHARACTER_SET="$(kdl_require mysql.character_set)"
MYSQL_ADMIN_USERNAME="$(kdl_require mysql.administration_username)"

kdl_config_init "$TEMPLATE_CONFIG"
[[ "$(kdl_require schema_version)" == 1 ]] || exit 2
PROVISION_MODE="$(kdl_require provision.mode)"
SMS_CHANNEL_CODE="$(kdl_require provision.sms_channel_code)"

case "$PROVISION_MODE" in
    disabled|create-only|managed) ;;
    *) printf 'provision.mode must be disabled, create-only, or managed; got: %s\n' "$PROVISION_MODE" >&2; exit 2 ;;
esac
case "$SMS_CHANNEL_CODE" in
    ALIYUN|TENCENT|HUAWEI|QINIU|DEBUG_DING_TALK) ;;
    *) printf 'provision.sms_channel_code is not supported by the System module.\n' >&2; exit 2 ;;
esac

command -v jq >/dev/null 2>&1 || { printf 'jq is required.\n' >&2; exit 1; }
command -v base64 >/dev/null 2>&1 || { printf 'base64 is required.\n' >&2; exit 1; }

declare -a TEMPLATE_SECTIONS=(process_approve process_reject task_assigned task_timeout)
declare -a EXPECTED_CODES=(bpm_process_instance_approve bpm_process_instance_reject bpm_task_assigned bpm_task_timeout)
declare -a TEMPLATE_CODES=() TEMPLATE_NAMES=() TEMPLATE_CONTENTS=() TEMPLATE_PARAMS=() TEMPLATE_API_IDS=()

is_placeholder() {
    case "${1,,}" in
        not-configured|change-me|changeme|example|example-value|none|'') return 0 ;;
        *) return 1 ;;
    esac
}

for index in "${!TEMPLATE_SECTIONS[@]}"; do
    section="${TEMPLATE_SECTIONS[$index]}"
    code="$(kdl_require "${section}.code")"
    name="$(kdl_require "${section}.name")"
    content="$(kdl_require "${section}.content")"
    params="$(kdl_require "${section}.params")"
    api_id="$(kdl_require "${section}.api_template_id")"
    [[ "$code" == "${EXPECTED_CODES[$index]}" ]] || {
        printf '%s.code must remain %s because the BPM enum uses this stable key.\n' \
            "$section" "${EXPECTED_CODES[$index]}" >&2
        exit 2
    }
    (( ${#name} <= 63 && ${#content} <= 255 )) || {
        printf '%s name or content exceeds the System SMS column limit.\n' "$section" >&2
        exit 2
    }
    jq -e 'type == "array" and all(.[]; type == "string")' >/dev/null <<< "$params" || {
        printf '%s.params must be a JSON string array.\n' "$section" >&2
        exit 2
    }
    if [[ "$PROVISION_MODE" != disabled ]] && is_placeholder "$api_id"; then
        printf '%s.api_template_id must be configured when template provision is enabled.\n' "$section" >&2
        exit 2
    fi
    TEMPLATE_CODES+=("$code")
    TEMPLATE_NAMES+=("$name")
    TEMPLATE_CONTENTS+=("$content")
    TEMPLATE_PARAMS+=("$params")
    TEMPLATE_API_IDS+=("$api_id")
done

if [[ "$START_MODE" == check ]]; then
    printf 'BPM notification template preflight passed: mode=%s sms=%s. No template was changed.\n' \
        "$PROVISION_MODE" "$SMS_ENABLED"
    exit 0
fi
case "$START_MODE" in
    replace|replace-server) ;;
    *) printf 'BPM notification template provision skipped for startup_mode=%s.\n' "$START_MODE"; exit 0 ;;
esac

b64() { printf '%s' "$1" | base64 --wrap=0; }
sql_text() { printf "CONVERT(FROM_BASE64('%s') USING utf8mb4) COLLATE utf8mb4_unicode_ci" "$(b64 "$1")"; }
mysql_command() {
    podman exec --env "MYSQL_PWD=${MYSQL_ROOT_PASSWORD}" -i "$MYSQL_CONTAINER" \
        mysql "--default-character-set=${MYSQL_CHARACTER_SET}" \
        "--user=${MYSQL_ADMIN_USERNAME}" "--database=${MYSQL_DATABASE}" "$@"
}
mysql_scalar() { mysql_command --batch --skip-column-names --execute "$1"; }

channel_count="$(mysql_scalar "SELECT COUNT(*) FROM system_sms_channel WHERE code=$(sql_text "$SMS_CHANNEL_CODE") AND deleted=b'0';")"
if [[ "$PROVISION_MODE" != disabled || "$SMS_ENABLED" == true ]]; then
    [[ "$channel_count" == 1 ]] || {
        printf 'BPM SMS requires exactly one active channel with code %s; found %s.\n' \
            "$SMS_CHANNEL_CODE" "$channel_count" >&2
        exit 1
    }
fi

if [[ "$PROVISION_MODE" != disabled ]]; then
    managed=0
    [[ "$PROVISION_MODE" == managed ]] && managed=1
    for index in "${!TEMPLATE_SECTIONS[@]}"; do
        code="${TEMPLATE_CODES[$index]}"
        duplicate_count="$(mysql_scalar "SELECT COUNT(*) FROM system_sms_template WHERE code=$(sql_text "$code") AND deleted=b'0';")"
        [[ "$duplicate_count" =~ ^[01]$ ]] || {
            printf 'Duplicate active BPM SMS templates found for code %s.\n' "$code" >&2
            exit 1
        }
        mysql_command <<SQL
SET @managed=${managed}, @channel_code=$(sql_text "$SMS_CHANNEL_CODE"),
    @template_code=$(sql_text "$code"), @template_name=$(sql_text "${TEMPLATE_NAMES[$index]}"),
    @template_content=$(sql_text "${TEMPLATE_CONTENTS[$index]}"),
    @template_params=$(sql_text "${TEMPLATE_PARAMS[$index]}"),
    @api_template_id=$(sql_text "${TEMPLATE_API_IDS[$index]}");
SET @channel_id=(SELECT id FROM system_sms_channel WHERE code=@channel_code AND deleted=b'0' LIMIT 1);
INSERT INTO system_sms_template
    (type,status,code,name,content,params,remark,api_template_id,channel_id,channel_code,creator,updater,deleted)
SELECT 2,0,@template_code,@template_name,@template_content,@template_params,'managed-by:bpm-template-kdl',
       @api_template_id,@channel_id,@channel_code,'1','1',b'0'
WHERE NOT EXISTS (SELECT 1 FROM system_sms_template WHERE code=@template_code AND deleted=b'0');
UPDATE system_sms_template
SET type=2,status=0,name=@template_name,content=@template_content,params=@template_params,
    remark='managed-by:bpm-template-kdl',api_template_id=@api_template_id,
    channel_id=@channel_id,channel_code=@channel_code,updater='1'
WHERE @managed=1 AND code=@template_code AND deleted=b'0';
SQL
    done
fi

if [[ "$SMS_ENABLED" == true ]]; then
    for code in "${TEMPLATE_CODES[@]}"; do
        template_count="$(mysql_scalar "SELECT COUNT(*) FROM system_sms_template WHERE code=$(sql_text "$code") AND status=0 AND deleted=b'0';")"
        [[ "$template_count" == 1 ]] || {
            printf 'BPM SMS is enabled but active template %s is missing.\n' "$code" >&2
            exit 1
        }
    done
fi

printf 'BPM notification template provision completed: mode=%s sms=%s.\n' "$PROVISION_MODE" "$SMS_ENABLED"
