#!/usr/bin/env bash

set -Eeuo pipefail
umask 077

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd -- "${SCRIPT_DIR}/.." && pwd)"
STATE_DIR="${ROOT_DIR}/.runtime/docker-compose"
COMPOSE_FILE="${SCRIPT_DIR}/compose.yaml"
COMPOSE_ENV="${STATE_DIR}/compose.env"
PROJECT_NAME="mitedtsm-offline"
ACTION="${1:-}"

case "$ACTION" in
    check|deploy|start|stop) ;;
    *) printf 'Usage: bash ./docker/runtime.sh <check|deploy|start|stop>\n' >&2; exit 2 ;;
esac
if [[ "$ACTION" == check ]]; then
    RUNTIME_CONFIG="${ROOT_DIR}/podman/config/runtime-check-local.kdl"
else
    RUNTIME_CONFIG="${ROOT_DIR}/podman/config/runtime-local.kdl"
fi

for command in docker curl jq; do
    command -v "$command" >/dev/null 2>&1 || {
        printf 'Required Docker deployment command is unavailable: %s\n' "$command" >&2
        exit 1
    }
done
docker info >/dev/null 2>&1 || {
    printf 'Docker is installed but its daemon is not usable by the current user.\n' >&2
    exit 1
}
docker compose version >/dev/null 2>&1 || {
    printf 'Docker Compose v2 (docker compose) is required.\n' >&2
    exit 1
}
[[ -f "$RUNTIME_CONFIG" && -f "$COMPOSE_FILE" ]] || {
    printf 'Docker deployment files are incomplete. Run ./configure.sh or re-copy the bundle.\n' >&2
    exit 1
}

# shellcheck source=../../lib/kdl-config.sh
source "${ROOT_DIR}/podman/lib/kdl-config.sh"
kdl_config_init "$RUNTIME_CONFIG"
[[ "$(kdl_require schema_version)" == 1 ]] || exit 2

STOP_TIMEOUT="$(kdl_positive_integer deployment.stop_timeout_seconds)"
HOST_ADDRESS="$(kdl_require network.host_address)"
SERVER_PORT="$(kdl_port network.server_host_port)"
WEB_PORT="$(kdl_port network.web_host_port)"
MALL_PORT="$(kdl_port network.mall_host_port)"
HEALTH_INTERVAL="$(kdl_positive_integer health.interval_seconds)"
MYSQL_ATTEMPTS="$(kdl_positive_integer health.mysql_attempts)"
MYSQL_SCHEMA_ATTEMPTS="$(kdl_positive_integer health.mysql_schema_attempts)"
REDIS_ATTEMPTS="$(kdl_positive_integer health.redis_attempts)"
RABBITMQ_ATTEMPTS="$(kdl_positive_integer health.rabbitmq_attempts)"
TDENGINE_ATTEMPTS="$(kdl_positive_integer health.tdengine_attempts)"
TDENGINE_INIT_ATTEMPTS="$(kdl_positive_integer tdengine.initialization_attempts)"
SERVER_ATTEMPTS="$(kdl_positive_integer health.server_attempts)"
WEB_ATTEMPTS="$(kdl_positive_integer health.web_attempts)"
MALL_ATTEMPTS="$(kdl_positive_integer health.mall_attempts)"

MYSQL_CONTAINER="$(kdl_require container.mysql)"
REDIS_CONTAINER="$(kdl_require container.redis)"
RABBITMQ_CONTAINER="$(kdl_require container.rabbitmq)"
TDENGINE_CONTAINER="$(kdl_require container.tdengine)"
SERVER_CONTAINER="$(kdl_require container.server)"
WEB_CONTAINER="$(kdl_require container.web)"
MALL_CONTAINER="$(kdl_require container.mall)"

MYSQL_ROOT_PASSWORD="$(kdl_require mysql.root_password)"
MYSQL_APPLICATION_USERNAME="$(kdl_require mysql.application_username)"
MYSQL_APPLICATION_PASSWORD="$(kdl_require mysql.application_password)"
MYSQL_DATABASE="$(kdl_require mysql.database)"
MYSQL_PORT="$(kdl_port mysql.port)"
MYSQL_CHARACTER_SET="$(kdl_require mysql.character_set)"
MYSQL_ADMIN_USERNAME="$(kdl_require mysql.administration_username)"
MYSQL_SCHEMA_QUERY="$(kdl_require health.mysql_schema_query)"
REDIS_PASSWORD="$(kdl_require redis.password)"
RABBITMQ_USERNAME="$(kdl_require rabbitmq.username)"
RABBITMQ_PASSWORD="$(kdl_require rabbitmq.password)"
RABBITMQ_OS_USER="$(kdl_require health.rabbitmq_os_user)"
TDENGINE_USERNAME="$(kdl_require tdengine.username)"
TDENGINE_PASSWORD="$(kdl_require tdengine.password)"
TDENGINE_HEALTH_QUERY="$(kdl_require health.tdengine_query)"
BPM_PROVISION_AFTER_START="$(kdl_bool bpm.provision_after_start)"
BPM_PROVISION_MANIFEST="$(kdl_path bpm.provision_manifest)"
FILE_CLIENT_ID="$(kdl_positive_integer file.client_id)"
FILE_PUBLIC_BASE_URL="$(kdl_require file.public_base_url)"

for name in "$MYSQL_CONTAINER" "$REDIS_CONTAINER" "$RABBITMQ_CONTAINER" "$TDENGINE_CONTAINER" \
    "$SERVER_CONTAINER" "$WEB_CONTAINER" "$MALL_CONTAINER"; do
    [[ "$name" =~ ^[A-Za-z0-9][A-Za-z0-9_.-]*$ ]] || {
        printf 'Container name contains unsupported characters: %s\n' "$name" >&2
        exit 2
    }
done
for password_name in MYSQL_ROOT_PASSWORD MYSQL_APPLICATION_PASSWORD REDIS_PASSWORD RABBITMQ_PASSWORD TDENGINE_PASSWORD; do
    password="${!password_name}"
    [[ "$password" =~ ^[A-Za-z0-9._~-]{8,128}$ ]] || {
        printf '%s must use 8-128 portable characters. Run ./configure.sh after fixing deployment-config.kdl.\n' \
            "$password_name" >&2
        exit 2
    }
done
[[ "$TDENGINE_USERNAME" == root ]] || {
    printf 'tdengine.username must remain root for first-volume initialization.\n' >&2
    exit 2
}

append_env() {
    local file="$1" key="$2" value="$3"
    [[ "$key" =~ ^[A-Za-z_][A-Za-z0-9_]*$ ]] || return 2
    [[ "$value" != *$'\n'* && "$value" != *$'\r'* ]] || {
        printf 'Multiline environment values are not supported: %s\n' "$key" >&2
        return 2
    }
    # Compose interpolates dollar signs in env files. Doubling preserves the
    # literal secret/configuration value passed to the container.
    value="${value//\$/\$\$}"
    printf '%s=%s\n' "$key" "$value" >> "$file"
}

append_kdl_env() {
    local file="$1" env_name="$2" config_key="$3" suffix="${4:-}"
    append_env "$file" "$env_name" "$(kdl_require "$config_key")${suffix}"
}

render_runtime() {
    mkdir -p "$STATE_DIR"
    : > "$COMPOSE_ENV"
    : > "${STATE_DIR}/mysql.env"
    : > "${STATE_DIR}/redis.env"
    : > "${STATE_DIR}/rabbitmq.env"
    : > "${STATE_DIR}/tdengine.env"
    : > "${STATE_DIR}/server.env"

    append_kdl_env "$COMPOSE_ENV" MYSQL_IMAGE image.mysql_base
    append_kdl_env "$COMPOSE_ENV" REDIS_IMAGE image.redis_base
    append_kdl_env "$COMPOSE_ENV" RABBITMQ_IMAGE image.rabbitmq_base
    append_kdl_env "$COMPOSE_ENV" TDENGINE_IMAGE image.tdengine_base
    append_kdl_env "$COMPOSE_ENV" SERVER_IMAGE image.server_runtime
    append_kdl_env "$COMPOSE_ENV" WEB_IMAGE image.web_runtime
    append_kdl_env "$COMPOSE_ENV" MALL_IMAGE image.mall_runtime
    append_env "$COMPOSE_ENV" MYSQL_CONTAINER "$MYSQL_CONTAINER"
    append_env "$COMPOSE_ENV" REDIS_CONTAINER "$REDIS_CONTAINER"
    append_env "$COMPOSE_ENV" RABBITMQ_CONTAINER "$RABBITMQ_CONTAINER"
    append_env "$COMPOSE_ENV" TDENGINE_CONTAINER "$TDENGINE_CONTAINER"
    append_env "$COMPOSE_ENV" SERVER_CONTAINER "$SERVER_CONTAINER"
    append_env "$COMPOSE_ENV" WEB_CONTAINER "$WEB_CONTAINER"
    append_env "$COMPOSE_ENV" MALL_CONTAINER "$MALL_CONTAINER"
    append_kdl_env "$COMPOSE_ENV" MYSQL_VOLUME volume.mysql
    append_kdl_env "$COMPOSE_ENV" REDIS_VOLUME volume.redis
    append_kdl_env "$COMPOSE_ENV" RABBITMQ_VOLUME volume.rabbitmq
    append_kdl_env "$COMPOSE_ENV" TDENGINE_VOLUME volume.tdengine
    append_env "$COMPOSE_ENV" STATE_DIR "$STATE_DIR"
    append_env "$COMPOSE_ENV" STOP_TIMEOUT_SECONDS "$STOP_TIMEOUT"
    append_env "$COMPOSE_ENV" HOST_ADDRESS "$HOST_ADDRESS"
    append_env "$COMPOSE_ENV" SERVER_HOST_PORT "$SERVER_PORT"
    append_kdl_env "$COMPOSE_ENV" SERVER_CONTAINER_PORT network.server_container_port
    append_env "$COMPOSE_ENV" WEB_HOST_PORT "$WEB_PORT"
    append_kdl_env "$COMPOSE_ENV" WEB_CONTAINER_PORT network.web_container_port
    append_env "$COMPOSE_ENV" MALL_HOST_PORT "$MALL_PORT"
    append_kdl_env "$COMPOSE_ENV" MALL_CONTAINER_PORT network.mall_container_port
    append_env "$COMPOSE_ENV" MYSQL_PORT "$MYSQL_PORT"
    append_env "$COMPOSE_ENV" MYSQL_CHARACTER_SET "$MYSQL_CHARACTER_SET"
    append_kdl_env "$COMPOSE_ENV" MYSQL_COLLATION mysql.collation
    append_kdl_env "$COMPOSE_ENV" MYSQL_AUTHENTICATION_PLUGIN mysql.authentication_plugin
    append_kdl_env "$COMPOSE_ENV" HOST_PROXY_NAME network.host_proxy_name

    append_env "${STATE_DIR}/mysql.env" MYSQL_DATABASE "$MYSQL_DATABASE"
    append_env "${STATE_DIR}/mysql.env" MYSQL_ROOT_PASSWORD "$MYSQL_ROOT_PASSWORD"
    append_kdl_env "${STATE_DIR}/mysql.env" TZ mysql.timezone
    if [[ "$MYSQL_APPLICATION_USERNAME" != root ]]; then
        append_env "${STATE_DIR}/mysql.env" MYSQL_USER "$MYSQL_APPLICATION_USERNAME"
        append_env "${STATE_DIR}/mysql.env" MYSQL_PASSWORD "$MYSQL_APPLICATION_PASSWORD"
    fi
    append_env "${STATE_DIR}/redis.env" REDIS_PASSWORD "$REDIS_PASSWORD"
    append_env "${STATE_DIR}/rabbitmq.env" RABBITMQ_DEFAULT_USER "$RABBITMQ_USERNAME"
    append_env "${STATE_DIR}/rabbitmq.env" RABBITMQ_DEFAULT_PASS "$RABBITMQ_PASSWORD"
    append_kdl_env "${STATE_DIR}/tdengine.env" TAOS_FQDN tdengine.fqdn

    local jdbc_url
    jdbc_url="jdbc:mysql://$(kdl_require mysql.host):${MYSQL_PORT}/${MYSQL_DATABASE}?$(kdl_require mysql.jdbc_parameters)"
    append_kdl_env "${STATE_DIR}/server.env" SPRING_PROFILES_ACTIVE server.spring_profile
    append_env "${STATE_DIR}/server.env" MYSQL_JDBC_URL "$jdbc_url"
    append_env "${STATE_DIR}/server.env" MYSQL_USERNAME "$MYSQL_APPLICATION_USERNAME"
    append_env "${STATE_DIR}/server.env" MYSQL_PASSWORD "$MYSQL_APPLICATION_PASSWORD"
    append_env "${STATE_DIR}/server.env" SPRING_DATASOURCE_DYNAMIC_DATASOURCE_MASTER_URL "$jdbc_url"
    append_env "${STATE_DIR}/server.env" SPRING_DATASOURCE_DYNAMIC_DATASOURCE_MASTER_USERNAME "$MYSQL_APPLICATION_USERNAME"
    append_env "${STATE_DIR}/server.env" SPRING_DATASOURCE_DYNAMIC_DATASOURCE_MASTER_PASSWORD "$MYSQL_APPLICATION_PASSWORD"
    append_env "${STATE_DIR}/server.env" SPRING_DATA_REDIS_PASSWORD "$REDIS_PASSWORD"
    append_env "${STATE_DIR}/server.env" SPRING_RABBITMQ_USERNAME "$RABBITMQ_USERNAME"
    append_env "${STATE_DIR}/server.env" SPRING_RABBITMQ_PASSWORD "$RABBITMQ_PASSWORD"
    append_env "${STATE_DIR}/server.env" TDENGINE_USER "$TDENGINE_USERNAME"
    append_env "${STATE_DIR}/server.env" TDENGINE_PASSWORD "$TDENGINE_PASSWORD"

    append_kdl_env "${STATE_DIR}/server.env" MITEDTSM_SECURITY_MOCK_ENABLE security.mock_login_enabled
    append_kdl_env "${STATE_DIR}/server.env" MITEDTSM_SECURITY_MOCK_SECRET security.mock_secret
    append_kdl_env "${STATE_DIR}/server.env" MITEDTSM_SECURITY_PASSWORD_ENCODER_LENGTH security.password_encoder_length
    append_kdl_env "${STATE_DIR}/server.env" MITEDTSM_XSS_ENABLE security.xss_enabled
    append_kdl_env "${STATE_DIR}/server.env" MITEDTSM_WEB_CORS_ALLOWED_ORIGIN_PATTERNS security.cors_allowed_origins
    append_kdl_env "${STATE_DIR}/server.env" MITEDTSM_WEB_CORS_ALLOWED_HEADERS security.cors_allowed_headers
    append_kdl_env "${STATE_DIR}/server.env" MITEDTSM_WEB_CORS_ALLOWED_METHODS security.cors_allowed_methods
    append_kdl_env "${STATE_DIR}/server.env" MITEDTSM_WEB_CORS_ALLOW_CREDENTIALS security.cors_allow_credentials
    append_kdl_env "${STATE_DIR}/server.env" MITEDTSM_WEB_CORS_MAX_AGE security.cors_max_age_seconds s
    append_kdl_env "${STATE_DIR}/server.env" SPRINGDOC_API_DOCS_ENABLED security.api_docs_enabled
    append_kdl_env "${STATE_DIR}/server.env" SPRINGDOC_SWAGGER_UI_ENABLED security.api_docs_enabled
    append_kdl_env "${STATE_DIR}/server.env" KNIFE4J_ENABLE security.api_docs_enabled
    append_kdl_env "${STATE_DIR}/server.env" SPRING_DATASOURCE_DRUID_WEB_STAT_FILTER_ENABLED security.druid_console_enabled
    append_kdl_env "${STATE_DIR}/server.env" SPRING_DATASOURCE_DRUID_STAT_VIEW_SERVLET_ENABLED security.druid_console_enabled
    append_kdl_env "${STATE_DIR}/server.env" MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE security.actuator_exposure
    append_kdl_env "${STATE_DIR}/server.env" MITEDTSM_API_ENCRYPT_ENABLE security.api_encryption_enabled
    append_kdl_env "${STATE_DIR}/server.env" MITEDTSM_CAPTCHA_ENABLE security.captcha_enabled
    append_kdl_env "${STATE_DIR}/server.env" SPRING_BOOT_ADMIN_CLIENT_ENABLED security.boot_admin_client_enabled
    append_kdl_env "${STATE_DIR}/server.env" MITEDTSM_BPM_NOTIFICATION_SMS_ENABLED bpm.notification_sms_enabled
    append_kdl_env "${STATE_DIR}/server.env" MITEDTSM_BPM_NOTIFICATION_FAIL_FAST bpm.notification_fail_fast
    append_kdl_env "${STATE_DIR}/server.env" MITEDTSM_WEB_ADMIN_UI_URL network.admin_ui_public_url

    append_kdl_env "${STATE_DIR}/server.env" JUSTAUTH_ENABLED integration.justauth_enabled
    append_kdl_env "${STATE_DIR}/server.env" WX_MP_APP_ID integration.wx_mp_app_id
    append_kdl_env "${STATE_DIR}/server.env" WX_MP_SECRET integration.wx_mp_secret
    append_kdl_env "${STATE_DIR}/server.env" WX_MINIAPP_APP_ID integration.wx_miniapp_app_id
    append_kdl_env "${STATE_DIR}/server.env" WX_MINIAPP_SECRET integration.wx_miniapp_secret
    append_kdl_env "${STATE_DIR}/server.env" MITEDTSM_TENCENT_LBS_KEY integration.tencent_lbs_key
    append_kdl_env "${STATE_DIR}/server.env" MITEDTSM_PAY_ORDER_NOTIFY_URL integration.pay_order_notify_url
    append_kdl_env "${STATE_DIR}/server.env" MITEDTSM_PAY_REFUND_NOTIFY_URL integration.pay_refund_notify_url
    append_kdl_env "${STATE_DIR}/server.env" MITEDTSM_PAY_TRANSFER_NOTIFY_URL integration.pay_transfer_notify_url
    append_kdl_env "${STATE_DIR}/server.env" MITEDTSM_EXPRESS_CLIENT integration.express_client
    append_kdl_env "${STATE_DIR}/server.env" MITEDTSM_EXPRESS_KDNIAO_API_KEY integration.express_kdniao_api_key
    append_kdl_env "${STATE_DIR}/server.env" MITEDTSM_EXPRESS_KDNIAO_BUSINESS_ID integration.express_kdniao_business_id
    append_kdl_env "${STATE_DIR}/server.env" MITEDTSM_EXPRESS_KDNIAO_REQUEST_TYPE integration.express_kdniao_request_type
    append_kdl_env "${STATE_DIR}/server.env" MITEDTSM_EXPRESS_KD100_KEY integration.express_kd100_key
    append_kdl_env "${STATE_DIR}/server.env" MITEDTSM_EXPRESS_KD100_CUSTOMER integration.express_kd100_customer

    append_kdl_env "${STATE_DIR}/server.env" MITEDTSM_CRM_MARKETING_PROVIDER_MODE crm_marketing.provider_mode
    append_kdl_env "${STATE_DIR}/server.env" MITEDTSM_CRM_MARKETING_PROCESS_DEFINITION_KEY crm_marketing.process_definition_key
    append_kdl_env "${STATE_DIR}/server.env" MITEDTSM_CRM_MARKETING_TRACKING_ENABLED crm_marketing.tracking_enabled
    append_kdl_env "${STATE_DIR}/server.env" MITEDTSM_CRM_MARKETING_PUBLIC_BASE_URL crm_marketing.public_base_url
    append_kdl_env "${STATE_DIR}/server.env" MITEDTSM_CRM_MARKETING_DELIVERY_SYNC_BATCH_SIZE crm_marketing.delivery_sync_batch_size
    append_kdl_env "${STATE_DIR}/server.env" MITEDTSM_CRM_MARKETING_CLICK_TRACKING_ENABLED crm_marketing.click_tracking_enabled
    append_kdl_env "${STATE_DIR}/server.env" MITEDTSM_CRM_MARKETING_CLICK_ALLOWED_HOSTS crm_marketing.click_allowed_hosts
    append_kdl_env "${STATE_DIR}/server.env" MITEDTSM_CRM_MARKETING_MAX_LINKS_PER_BROADCAST crm_marketing.max_links_per_broadcast
    append_kdl_env "${STATE_DIR}/server.env" MITEDTSM_CRM_CUSTOMER_IMPORT_MAX_ROWS crm_customer_import.max_rows
    append_kdl_env "${STATE_DIR}/server.env" MITEDTSM_CRM_CUSTOMER_IMPORT_PREVIEW_TTL_MINUTES crm_customer_import.preview_ttl_minutes
    append_kdl_env "${STATE_DIR}/server.env" MITEDTSM_CRM_EXPORT_TASK_ENABLED crm_export_task.enabled
    append_kdl_env "${STATE_DIR}/server.env" MITEDTSM_CRM_EXPORT_TASK_BATCH_SIZE crm_export_task.batch_size
    append_kdl_env "${STATE_DIR}/server.env" MITEDTSM_CRM_EXPORT_TASK_MAX_BATCH_SIZE crm_export_task.max_batch_size
    append_kdl_env "${STATE_DIR}/server.env" MITEDTSM_CRM_EXPORT_TASK_MAX_PENDING_PER_USER crm_export_task.max_pending_per_user
    append_kdl_env "${STATE_DIR}/server.env" MITEDTSM_CRM_EXPORT_TASK_MAX_ROWS crm_export_task.max_rows
    append_kdl_env "${STATE_DIR}/server.env" MITEDTSM_CRM_EXPORT_TASK_RETENTION_HOURS crm_export_task.retention_hours
    append_kdl_env "${STATE_DIR}/server.env" MITEDTSM_CRM_EXPORT_TASK_TOKEN_TTL_SECONDS crm_export_task.token_ttl_seconds
    append_kdl_env "${STATE_DIR}/server.env" MITEDTSM_CRM_EXPORT_TASK_CRON crm_export_task.cron
    append_kdl_env "${STATE_DIR}/server.env" MITEDTSM_CRM_EXPORT_TASK_ZONE crm_export_task.zone
    append_kdl_env "${STATE_DIR}/server.env" MITEDTSM_CRM_EXPORT_TASK_LOCK_KEY crm_export_task.lock_key
    append_kdl_env "${STATE_DIR}/server.env" MITEDTSM_CRM_EXPORT_TASK_LOCK_LEASE_SECONDS crm_export_task.lock_lease_seconds

    if [[ "$(kdl_bool network.use_host_proxy)" == true ]]; then
        local proxy_name proxy value
        proxy_name="$(kdl_require network.host_proxy_name)"
        for proxy in http_proxy https_proxy all_proxy; do
            value="$(kdl_require "network.${proxy}")"
            [[ "$value" == none ]] && continue
            value="${value//127.0.0.1/${proxy_name}}"
            value="${value//localhost/${proxy_name}}"
            append_env "${STATE_DIR}/server.env" "$proxy" "$value"
            append_env "${STATE_DIR}/server.env" "${proxy^^}" "$value"
        done
        append_kdl_env "${STATE_DIR}/server.env" no_proxy network.no_proxy
        append_kdl_env "${STATE_DIR}/server.env" NO_PROXY network.no_proxy
    fi

    chmod 0600 "$COMPOSE_ENV" "${STATE_DIR}/"*.env
    compose config --quiet
}

compose() {
    docker compose --project-name "$PROJECT_NAME" --env-file "$COMPOSE_ENV" --file "$COMPOSE_FILE" "$@"
}

archive_path() {
    printf '%s/%s' "$(kdl_path image.archive_dir)" "$1"
}

ensure_images() {
    local -a images=(
        "$(kdl_require image.redis_base)" "$(kdl_require image.rabbitmq_base)"
        "$(kdl_require image.tdengine_base)" "$(kdl_require image.mysql_base)"
        "$(kdl_require image.init_runtime)" "$(kdl_require image.server_runtime)"
        "$(kdl_require image.web_runtime)" "$(kdl_require image.mall_runtime)"
    )
    local -a archives=(
        "$(archive_path "$(kdl_require archive.redis_base)")"
        "$(archive_path "$(kdl_require archive.rabbitmq_base)")"
        "$(archive_path "$(kdl_require archive.tdengine_base)")"
        "$(archive_path "$(kdl_require archive.mysql_base)")"
        "$(archive_path "$(kdl_require archive.init_runtime)")"
        "$(archive_path "$(kdl_require archive.server_runtime)")"
        "$(archive_path "$(kdl_require archive.web_runtime)")"
        "$(archive_path "$(kdl_require archive.mall_runtime)")"
    )
    local index image archive
    for ((index = 0; index < ${#images[@]}; index++)); do
        image="${images[index]}"
        archive="${archives[index]}"
        [[ -s "$archive" ]] || { printf 'Missing image archive: %s\n' "$archive" >&2; return 1; }
        if ! docker image inspect "$image" >/dev/null 2>&1; then
            printf 'Loading OCI image archive into Docker: %s\n' "$(basename -- "$archive")"
            docker load --input "$archive" >/dev/null
        fi
        docker image inspect "$image" >/dev/null 2>&1 || {
            printf 'Image archive did not provide the expected tag: %s\n' "$image" >&2
            return 1
        }
    done
}

check_archives() {
    local filename
    for key in redis_base rabbitmq_base tdengine_base mysql_base init_runtime server_runtime web_runtime mall_runtime; do
        filename="$(kdl_require "archive.${key}")"
        [[ -s "$(archive_path "$filename")" ]] || {
            printf 'Missing image archive: %s\n' "$filename" >&2
            return 1
        }
    done
}

wait_for() {
    local description="$1" attempts="$2" last_output='' attempt
    shift 2
    for ((attempt = 1; attempt <= attempts; attempt++)); do
        if last_output="$("$@" 2>&1)"; then
            printf '%s is ready.\n' "$description"
            return
        fi
        sleep "$HEALTH_INTERVAL"
    done
    printf 'Timed out waiting for %s.\n%s\n' "$description" "${last_output:0:4000}" >&2
    return 1
}

mysql_probe() {
    docker exec --env "MYSQL_PWD=${MYSQL_ROOT_PASSWORD}" "$MYSQL_CONTAINER" mysql \
        --user "$MYSQL_ADMIN_USERNAME" --host 127.0.0.1 --port "$MYSQL_PORT" \
        --batch --skip-column-names --execute 'SELECT 1'
}

redis_probe() {
    docker exec --env "REDISCLI_AUTH=${REDIS_PASSWORD}" "$REDIS_CONTAINER" redis-cli ping
}

rabbitmq_probe() {
    docker exec --user "$RABBITMQ_OS_USER" "$RABBITMQ_CONTAINER" \
        /opt/rabbitmq/sbin/rabbitmq-diagnostics -q ping
}

tdengine_command() {
    local password="$1"
    shift
    docker exec "$TDENGINE_CONTAINER" taos --user "$TDENGINE_USERNAME" \
        "-p${password}" --commands "$*"
}

tdengine_probe_any() {
    tdengine_command "$TDENGINE_PASSWORD" "$TDENGINE_HEALTH_QUERY" >/dev/null 2>&1 ||
        tdengine_command taosdata "$TDENGINE_HEALTH_QUERY" >/dev/null 2>&1
}

configure_tdengine_credentials() {
    if tdengine_command "$TDENGINE_PASSWORD" "$TDENGINE_HEALTH_QUERY" >/dev/null 2>&1; then
        return
    fi
    tdengine_command taosdata "$TDENGINE_HEALTH_QUERY" >/dev/null 2>&1 || {
        printf 'TDengine rejects both configured and factory credentials. Check deployment-config.kdl.\n' >&2
        return 1
    }
    printf 'Applying the configured TDengine root credential on this new data volume.\n'
    tdengine_command taosdata "ALTER USER root PASS '${TDENGINE_PASSWORD}';" >/dev/null
    tdengine_command "$TDENGINE_PASSWORD" "$TDENGINE_HEALTH_QUERY" >/dev/null
}

initialize_tdengine() {
    wait_for 'TDengine database initialization' "$TDENGINE_INIT_ATTEMPTS" \
        tdengine_command "$TDENGINE_PASSWORD" "CREATE DATABASE IF NOT EXISTS ${MYSQL_DATABASE};"
}

apply_runtime_file_storage() {
    [[ "$FILE_PUBLIC_BASE_URL" =~ ^https?://[A-Za-z0-9._:/-]+$ ]] || {
        printf 'file.public_base_url must be a plain HTTP(S) URL.\n' >&2
        return 2
    }
    docker exec --env "MYSQL_PWD=${MYSQL_ROOT_PASSWORD}" "$MYSQL_CONTAINER" mysql \
        "--default-character-set=${MYSQL_CHARACTER_SET}" --user "$MYSQL_ADMIN_USERNAME" \
        "--database=${MYSQL_DATABASE}" --execute \
        "UPDATE infra_file_config SET master=(id=${FILE_CLIENT_ID}), config=CASE WHEN id=${FILE_CLIENT_ID} THEN JSON_SET(config, '$.domain', '${FILE_PUBLIC_BASE_URL}') ELSE config END WHERE deleted=b'0';"
}

wait_http_services() {
    wait_for 'Spring Boot server' "$SERVER_ATTEMPTS" curl --noproxy '*' --fail --silent --show-error \
        "http://127.0.0.1:${SERVER_PORT}$(kdl_require health.server_path)"
    wait_for 'Web frontend' "$WEB_ATTEMPTS" curl --noproxy '*' --fail --silent --show-error \
        "http://127.0.0.1:${WEB_PORT}$(kdl_require health.web_path)"
    wait_for 'Mall frontend' "$MALL_ATTEMPTS" curl --noproxy '*' --fail --silent --show-error \
        "http://127.0.0.1:${MALL_PORT}$(kdl_require health.mall_path)"
}

show_logs_on_error() {
    local status=$?
    if ((status != 0)); then
        printf '\nDocker Compose deployment failed; recent logs follow.\n' >&2
        compose logs --tail 80 >&2 || true
    fi
}

render_runtime

if [[ "$ACTION" == check ]]; then
    check_archives
    CONTAINER_ENGINE=docker bash "${ROOT_DIR}/podman/internal/provision-database.sh" "$RUNTIME_CONFIG"
    CONTAINER_ENGINE=docker bash "${ROOT_DIR}/podman/internal/provision-marketing-provider.sh" "$RUNTIME_CONFIG"
    CONTAINER_ENGINE=docker bash "${ROOT_DIR}/podman/internal/provision-bpm-notifications.sh" "$RUNTIME_CONFIG"
    printf 'Docker Compose deployment preflight passed. No image was loaded and no container was changed.\n'
    exit 0
fi

if [[ "$ACTION" == stop ]]; then
    compose stop --timeout "$STOP_TIMEOUT"
    printf 'Docker Compose services are stopped; definitions and named volumes are preserved.\n'
    exit 0
fi

if [[ "$ACTION" == start ]]; then
    ensure_images
    compose up --detach --no-build
    wait_http_services
    printf 'Docker Compose deployment is running on ports %s, %s, and %s.\n' "$SERVER_PORT" "$WEB_PORT" "$MALL_PORT"
    exit 0
fi

trap show_logs_on_error EXIT
ensure_images
compose stop --timeout "$STOP_TIMEOUT" server web mall >/dev/null 2>&1 || true
compose up --detach --no-build mysql redis rabbitmq tdengine

wait_for MySQL "$MYSQL_ATTEMPTS" mysql_probe
wait_for Redis "$REDIS_ATTEMPTS" redis_probe
wait_for RabbitMQ "$RABBITMQ_ATTEMPTS" rabbitmq_probe
wait_for TDengine "$TDENGINE_ATTEMPTS" tdengine_probe_any
configure_tdengine_credentials
initialize_tdengine

CONTAINER_ENGINE=docker bash "${ROOT_DIR}/podman/internal/provision-database.sh" "$RUNTIME_CONFIG"
wait_for 'MySQL schema initialization' "$MYSQL_SCHEMA_ATTEMPTS" \
    docker exec --env "MYSQL_PWD=${MYSQL_ROOT_PASSWORD}" "$MYSQL_CONTAINER" mysql \
    "--default-character-set=${MYSQL_CHARACTER_SET}" --user "$MYSQL_ADMIN_USERNAME" \
    "--database=${MYSQL_DATABASE}" --batch --skip-column-names --execute "$MYSQL_SCHEMA_QUERY"
CONTAINER_ENGINE=docker bash "${ROOT_DIR}/podman/internal/provision-marketing-provider.sh" "$RUNTIME_CONFIG"
CONTAINER_ENGINE=docker bash "${ROOT_DIR}/podman/internal/provision-bpm-notifications.sh" "$RUNTIME_CONFIG"
apply_runtime_file_storage

compose up --detach --no-build --no-deps server
wait_for 'Spring Boot server' "$SERVER_ATTEMPTS" curl --noproxy '*' --fail --silent --show-error \
    "http://127.0.0.1:${SERVER_PORT}$(kdl_require health.server_path)"
if [[ "$BPM_PROVISION_AFTER_START" == true ]]; then
    bash "${ROOT_DIR}/podman/operations/bpm/provision-bpm-models.sh" "$BPM_PROVISION_MANIFEST"
fi
compose up --detach --no-build --no-deps web mall
wait_http_services
trap - EXIT
printf 'Docker Compose deployment completed on ports %s, %s, and %s.\n' "$SERVER_PORT" "$WEB_PORT" "$MALL_PORT"
