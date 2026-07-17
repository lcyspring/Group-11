#!/usr/bin/env bash
# Start Mitedtsm directly on the real host with one rootless Podman Pod.
# Docker Engine, the Docker CLI, and Compose are deliberately not used.

set -Eeuo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd -- "${SCRIPT_DIR}/.." && pwd)"
PODMAN=(podman)
PODMAN_PROXY_ARGS=(--http-proxy=false)
PROXY_ENV=()

usage() {
    printf 'Usage: bash ./up.sh <config.yaml>\n' >&2
}

[[ $# -eq 1 ]] || {
    usage
    exit 2
}

# shellcheck source=lib/yaml-config.sh
source "${SCRIPT_DIR}/lib/yaml-config.sh"
yaml_config_init "$1"

[[ "$(yaml_require schema_version)" == "1" ]] || {
    printf 'Unsupported schema_version; expected 1.\n' >&2
    exit 2
}

START_MODE="$(yaml_require operation.startup_mode)"
POD_NAME="$(yaml_require deployment.pod_name)"
STOP_TIMEOUT="$(yaml_positive_integer deployment.stop_timeout_seconds)"
HOST_ADDRESS="$(yaml_require network.host_address)"
SERVER_PORT="$(yaml_port network.server_host_port)"
SERVER_CONTAINER_PORT="$(yaml_port network.server_container_port)"
WEB_PORT="$(yaml_port network.web_host_port)"
WEB_CONTAINER_PORT="$(yaml_port network.web_container_port)"
MALL_PORT="$(yaml_port network.mall_host_port)"
MALL_CONTAINER_PORT="$(yaml_port network.mall_container_port)"
USE_HOST_PROXY="$(yaml_bool network.use_host_proxy)"
HOST_PROXY_NAME="$(yaml_require network.host_proxy_name)"
HTTP_PROXY_URL="$(yaml_require network.http_proxy)"
HTTPS_PROXY_URL="$(yaml_require network.https_proxy)"
ALL_PROXY_URL="$(yaml_require network.all_proxy)"
NO_PROXY_VALUE="$(yaml_require network.no_proxy)"

IMAGE_SOURCE="$(yaml_require image.source)"
IMAGE_ARCHIVE_DIR="$(yaml_path image.archive_dir)"
RUNTIME_BASE_IMAGE="$(yaml_require image.runtime_base)"
MYSQL_BASE_IMAGE="$(yaml_require image.mysql_base)"
REDIS_BASE_IMAGE="$(yaml_require image.redis_base)"
RABBITMQ_BASE_IMAGE="$(yaml_require image.rabbitmq_base)"
TDENGINE_BASE_IMAGE="$(yaml_require image.tdengine_base)"
NGINX_BASE_IMAGE="$(yaml_require image.nginx_base)"
MYSQL_IMAGE="$(yaml_require image.mysql_runtime)"
INIT_IMAGE="$(yaml_require image.init_runtime)"
SERVER_IMAGE="$(yaml_require image.server_runtime)"
WEB_IMAGE="$(yaml_require image.web_runtime)"
MALL_IMAGE="$(yaml_require image.mall_runtime)"

RUNTIME_ARCHIVE="$(yaml_require archive.runtime_base)"
MYSQL_ARCHIVE="$(yaml_require archive.mysql_base)"
REDIS_ARCHIVE="$(yaml_require archive.redis_base)"
RABBITMQ_ARCHIVE="$(yaml_require archive.rabbitmq_base)"
TDENGINE_ARCHIVE="$(yaml_require archive.tdengine_base)"
NGINX_ARCHIVE="$(yaml_require archive.nginx_base)"

MYSQL_CONTAINER="$(yaml_require container.mysql)"
REDIS_CONTAINER="$(yaml_require container.redis)"
RABBITMQ_CONTAINER="$(yaml_require container.rabbitmq)"
TDENGINE_CONTAINER="$(yaml_require container.tdengine)"
INIT_CONTAINER="$(yaml_require container.init)"
SERVER_CONTAINER="$(yaml_require container.server)"
WEB_CONTAINER="$(yaml_require container.web)"
MALL_CONTAINER="$(yaml_require container.mall)"

MYSQL_VOLUME="$(yaml_require volume.mysql)"
REDIS_VOLUME="$(yaml_require volume.redis)"
RABBITMQ_VOLUME="$(yaml_require volume.rabbitmq)"
TDENGINE_VOLUME="$(yaml_require volume.tdengine)"

MYSQL_DATABASE="$(yaml_require mysql.database)"
MYSQL_DATASET="$(yaml_require mysql.dataset)"
MYSQL_ROOT_PASSWORD="$(yaml_require mysql.root_password)"
MYSQL_CHARACTER_SET="$(yaml_require mysql.character_set)"
MYSQL_COLLATION="$(yaml_require mysql.collation)"
MYSQL_AUTHENTICATION_PLUGIN="$(yaml_require mysql.authentication_plugin)"
MYSQL_TIMEZONE="$(yaml_require mysql.timezone)"
MYSQL_COMPATIBILITY_MIGRATION_MANIFEST="$(yaml_path mysql.compatibility_migration_manifest)"

[[ "$MYSQL_DATASET" =~ ^[a-z0-9][a-z0-9._-]*$ ]] || {
    printf 'mysql.dataset contains unsupported characters: %s\n' "$MYSQL_DATASET" >&2
    exit 2
}
[[ -s "${PROJECT_ROOT}/database/datasets/${MYSQL_DATASET}.manifest" ]] || {
    printf 'Selected MySQL dataset manifest is missing: %s\n' "$MYSQL_DATASET" >&2
    exit 2
}
RABBITMQ_USERNAME="$(yaml_require rabbitmq.username)"
RABBITMQ_PASSWORD="$(yaml_require rabbitmq.password)"
TDENGINE_HOST="$(yaml_require tdengine.host)"
TDENGINE_PORT="$(yaml_port tdengine.port)"
TDENGINE_FQDN="$(yaml_require tdengine.fqdn)"
TDENGINE_USERNAME="$(yaml_require tdengine.username)"
TDENGINE_PASSWORD="$(yaml_require tdengine.password)"
TDENGINE_INITIALIZATION_ATTEMPTS="$(yaml_positive_integer tdengine.initialization_attempts)"
SPRING_PROFILE="$(yaml_require server.spring_profile)"
SECURITY_MOCK_LOGIN_ENABLED="$(yaml_bool security.mock_login_enabled)"
SECURITY_MOCK_SECRET="$(yaml_require security.mock_secret)"
SECURITY_PASSWORD_ENCODER_LENGTH="$(yaml_positive_integer security.password_encoder_length)"
SECURITY_XSS_ENABLED="$(yaml_bool security.xss_enabled)"
SECURITY_CORS_ALLOWED_ORIGINS="$(yaml_require security.cors_allowed_origins)"
SECURITY_CORS_ALLOWED_HEADERS="$(yaml_require security.cors_allowed_headers)"
SECURITY_CORS_ALLOWED_METHODS="$(yaml_require security.cors_allowed_methods)"
SECURITY_CORS_ALLOW_CREDENTIALS="$(yaml_bool security.cors_allow_credentials)"
SECURITY_CORS_MAX_AGE_SECONDS="$(yaml_positive_integer security.cors_max_age_seconds)"
SECURITY_API_DOCS_ENABLED="$(yaml_bool security.api_docs_enabled)"
SECURITY_DRUID_CONSOLE_ENABLED="$(yaml_bool security.druid_console_enabled)"
SECURITY_ACTUATOR_EXPOSURE="$(yaml_require security.actuator_exposure)"
SECURITY_API_ENCRYPTION_ENABLED="$(yaml_bool security.api_encryption_enabled)"
SECURITY_CAPTCHA_ENABLED="$(yaml_bool security.captcha_enabled)"
SECURITY_BOOT_ADMIN_CLIENT_ENABLED="$(yaml_bool security.boot_admin_client_enabled)"
INTEGRATION_JUSTAUTH_ENABLED="$(yaml_bool integration.justauth_enabled)"
INTEGRATION_WX_MP_APP_ID="$(yaml_require integration.wx_mp_app_id)"
INTEGRATION_WX_MP_SECRET="$(yaml_require integration.wx_mp_secret)"
INTEGRATION_WX_MINIAPP_APP_ID="$(yaml_require integration.wx_miniapp_app_id)"
INTEGRATION_WX_MINIAPP_SECRET="$(yaml_require integration.wx_miniapp_secret)"
INTEGRATION_TENCENT_LBS_KEY="$(yaml_require integration.tencent_lbs_key)"
INTEGRATION_PAY_ORDER_NOTIFY_URL="$(yaml_require integration.pay_order_notify_url)"
INTEGRATION_PAY_REFUND_NOTIFY_URL="$(yaml_require integration.pay_refund_notify_url)"
INTEGRATION_PAY_TRANSFER_NOTIFY_URL="$(yaml_require integration.pay_transfer_notify_url)"
INTEGRATION_EXPRESS_CLIENT="$(yaml_require integration.express_client)"
INTEGRATION_EXPRESS_KDNIAO_API_KEY="$(yaml_require integration.express_kdniao_api_key)"
INTEGRATION_EXPRESS_KDNIAO_BUSINESS_ID="$(yaml_require integration.express_kdniao_business_id)"
INTEGRATION_EXPRESS_KDNIAO_REQUEST_TYPE="$(yaml_require integration.express_kdniao_request_type)"
INTEGRATION_EXPRESS_KD100_KEY="$(yaml_require integration.express_kd100_key)"
INTEGRATION_EXPRESS_KD100_CUSTOMER="$(yaml_require integration.express_kd100_customer)"
FILE_STORAGE_MODE="$(yaml_require file.storage_mode)"
FILE_CLIENT_ID="$(yaml_positive_integer file.client_id)"
FILE_PUBLIC_BASE_URL="$(yaml_require file.public_base_url)"
CRM_MARKETING_PROVIDER_MODE="$(yaml_require crm_marketing.provider_mode)"
CRM_MARKETING_TRACKING_ENABLED="$(yaml_bool crm_marketing.tracking_enabled)"
CRM_MARKETING_PUBLIC_BASE_URL="$(yaml_require crm_marketing.public_base_url)"
CRM_MARKETING_DELIVERY_SYNC_BATCH_SIZE="$(yaml_positive_integer crm_marketing.delivery_sync_batch_size)"

HEALTH_INTERVAL="$(yaml_positive_integer health.interval_seconds)"
HEALTH_HTTP_HOST="$(yaml_require health.http_host)"
MYSQL_ATTEMPTS="$(yaml_positive_integer health.mysql_attempts)"
MYSQL_HEALTH_HOST="$(yaml_require health.mysql_host)"
MYSQL_USER="$(yaml_require health.mysql_user)"
MYSQL_SCHEMA_ATTEMPTS="$(yaml_positive_integer health.mysql_schema_attempts)"
MYSQL_SCHEMA_QUERY="$(yaml_require health.mysql_schema_query)"
REDIS_ATTEMPTS="$(yaml_positive_integer health.redis_attempts)"
RABBITMQ_ATTEMPTS="$(yaml_positive_integer health.rabbitmq_attempts)"
RABBITMQ_OS_USER="$(yaml_require health.rabbitmq_os_user)"
TDENGINE_ATTEMPTS="$(yaml_positive_integer health.tdengine_attempts)"
TDENGINE_HEALTH_QUERY="$(yaml_require health.tdengine_query)"
SERVER_ATTEMPTS="$(yaml_positive_integer health.server_attempts)"
SERVER_HEALTH_PATH="$(yaml_require health.server_path)"
WEB_ATTEMPTS="$(yaml_positive_integer health.web_attempts)"
WEB_HEALTH_PATH="$(yaml_require health.web_path)"
MALL_ATTEMPTS="$(yaml_positive_integer health.mall_attempts)"
MALL_HEALTH_PATH="$(yaml_require health.mall_path)"

clear_host_proxy() {
    unset http_proxy HTTP_PROXY https_proxy HTTPS_PROXY \
        all_proxy ALL_PROXY no_proxy NO_PROXY || true
}

podman_cmd() {
    local subcommand="$1"
    shift
    local command=("${PODMAN[@]}" "$subcommand")

    case "$subcommand" in
        build|run)
            command+=("${PODMAN_PROXY_ARGS[@]}")
            ;;
    esac
    command+=("$@")

    "${command[@]}"
}

host_curl() {
    curl "$@"
}

require_command() {
    local command="$1"
    command -v "$command" >/dev/null 2>&1 || {
        printf 'Required command is unavailable: %s\n' "$command" >&2
        exit 1
    }
}

require_file() {
    local path="$1"
    [[ -s "$path" ]] || {
        printf 'Required file is missing or empty: %s\n' "$path" >&2
        exit 1
    }
}

require_dir() {
    local path="$1"
    [[ -d "$path" ]] || {
        printf 'Required directory is missing: %s\n' "$path" >&2
        exit 1
    }
}

container_proxy_url() {
    local url="${1:-}"
    url="${url//127.0.0.1/${HOST_PROXY_NAME}}"
    url="${url//localhost/${HOST_PROXY_NAME}}"
    printf '%s' "$url"
}

image_archive_path() {
    local filename="$1"
    printf '%s/%s' "$IMAGE_ARCHIVE_DIR" "$filename"
}

ensure_image() {
    local image="$1"
    local archive="$2"

    if [[ "$IMAGE_SOURCE" != "pull" ]] && podman_cmd image exists "$image"; then
        return
    fi

    case "$IMAGE_SOURCE" in
        auto)
            if [[ -r "$archive" ]]; then
                printf 'Loading %s from Podman image archive %s\n' "$image" "$archive"
                podman_cmd load --input "$archive"
            else
                printf 'Archive not found; pulling %s\n' "$image"
                podman_cmd pull "$image"
            fi
            ;;
        archive)
            [[ -r "$archive" ]] || {
                printf 'Missing required Podman image archive: %s\n' "$archive" >&2
                exit 1
            }
            printf 'Loading %s from Podman image archive %s\n' "$image" "$archive"
            podman_cmd load --input "$archive"
            ;;
        pull)
            printf 'Pulling %s\n' "$image"
            podman_cmd pull "$image"
            ;;
    esac

    podman_cmd image exists "$image" || {
        printf 'Image is unavailable after %s: %s\n' "$IMAGE_SOURCE" "$image" >&2
        exit 1
    }
}

ensure_volume() {
    local volume="$1"
    podman_cmd volume inspect "$volume" >/dev/null 2>&1 || podman_cmd volume create "$volume" >/dev/null
}

wait_for() {
    local description="$1"
    local attempts="$2"
    local last_output=''
    shift 2

    for ((attempt = 1; attempt <= attempts; attempt++)); do
        if last_output="$("$@" 2>&1)"; then
            printf '%s is ready.\n' "$description"
            return 0
        fi
        sleep "$HEALTH_INTERVAL"
    done

    printf 'Timed out waiting for %s.\n' "$description" >&2
    if [[ -n "$last_output" ]]; then
        printf 'Last %s probe output:\n%s\n' "$description" "${last_output:0:4000}" >&2
    fi
    return 1
}

initialize_tdengine() {
    local max_attempts="$TDENGINE_INITIALIZATION_ATTEMPTS"
    local attempt exit_code

    for ((attempt = 1; attempt <= max_attempts; attempt++)); do
        if ((attempt == 1)); then
            printf 'Initializing TDengine database.\n'
        else
            printf 'TDengine is reachable but not ready to create a database; retrying (%s/%s).\n' \
                "$attempt" "$max_attempts" >&2
            sleep "$HEALTH_INTERVAL"
        fi

        podman_cmd run -d --replace --name "$INIT_CONTAINER" --pod "$POD_NAME" --pull=never \
            --env "TDENGINE_HOST=${TDENGINE_HOST}" \
            --env "TDENGINE_PORT=${TDENGINE_PORT}" \
            "$INIT_IMAGE" >/dev/null
        exit_code="$(podman_cmd wait "$INIT_CONTAINER")"
        if [[ "$exit_code" == "0" ]]; then
            podman_cmd rm "$INIT_CONTAINER" >/dev/null
            return 0
        fi

        if ((attempt < max_attempts)); then
            podman_cmd logs --tail 20 "$INIT_CONTAINER" >&2 || true
        fi
    done

    printf 'TDengine initialization failed after %s attempts.\n' "$max_attempts" >&2
    return 1
}

start_frontends() {
    printf 'Starting frontend containers.\n'
    podman_cmd run -d --replace --name "$WEB_CONTAINER" --pod "$POD_NAME" --pull=never \
        "$WEB_IMAGE"
    podman_cmd run -d --replace --name "$MALL_CONTAINER" --pod "$POD_NAME" --pull=never \
        "$MALL_IMAGE"
}

start_server() {
    printf 'Starting Server container.\n'
    podman_cmd run -d --replace --name "$SERVER_CONTAINER" --pod "$POD_NAME" --pull=never \
        "${PROXY_ENV[@]}" \
        --env "SPRING_PROFILES_ACTIVE=${SPRING_PROFILE}" \
        --env "SPRING_DATASOURCE_DYNAMIC_DATASOURCE_MASTER_USERNAME=${MYSQL_USER}" \
        --env "SPRING_DATASOURCE_DYNAMIC_DATASOURCE_MASTER_PASSWORD=${MYSQL_ROOT_PASSWORD}" \
        --env "SPRING_RABBITMQ_USERNAME=${RABBITMQ_USERNAME}" \
        --env "SPRING_RABBITMQ_PASSWORD=${RABBITMQ_PASSWORD}" \
        --env "MITEDTSM_SECURITY_MOCK_ENABLE=${SECURITY_MOCK_LOGIN_ENABLED}" \
        --env "MITEDTSM_SECURITY_MOCK_SECRET=${SECURITY_MOCK_SECRET}" \
        --env "MITEDTSM_SECURITY_PASSWORD_ENCODER_LENGTH=${SECURITY_PASSWORD_ENCODER_LENGTH}" \
        --env "MITEDTSM_XSS_ENABLE=${SECURITY_XSS_ENABLED}" \
        --env "MITEDTSM_WEB_CORS_ALLOWED_ORIGIN_PATTERNS=${SECURITY_CORS_ALLOWED_ORIGINS}" \
        --env "MITEDTSM_WEB_CORS_ALLOWED_HEADERS=${SECURITY_CORS_ALLOWED_HEADERS}" \
        --env "MITEDTSM_WEB_CORS_ALLOWED_METHODS=${SECURITY_CORS_ALLOWED_METHODS}" \
        --env "MITEDTSM_WEB_CORS_ALLOW_CREDENTIALS=${SECURITY_CORS_ALLOW_CREDENTIALS}" \
        --env "MITEDTSM_WEB_CORS_MAX_AGE=${SECURITY_CORS_MAX_AGE_SECONDS}s" \
        --env "SPRINGDOC_API_DOCS_ENABLED=${SECURITY_API_DOCS_ENABLED}" \
        --env "SPRINGDOC_SWAGGER_UI_ENABLED=${SECURITY_API_DOCS_ENABLED}" \
        --env "KNIFE4J_ENABLE=${SECURITY_API_DOCS_ENABLED}" \
        --env "SPRING_DATASOURCE_DRUID_WEB_STAT_FILTER_ENABLED=${SECURITY_DRUID_CONSOLE_ENABLED}" \
        --env "SPRING_DATASOURCE_DRUID_STAT_VIEW_SERVLET_ENABLED=${SECURITY_DRUID_CONSOLE_ENABLED}" \
        --env "MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE=${SECURITY_ACTUATOR_EXPOSURE}" \
        --env "MITEDTSM_API_ENCRYPT_ENABLE=${SECURITY_API_ENCRYPTION_ENABLED}" \
        --env "MITEDTSM_CAPTCHA_ENABLE=${SECURITY_CAPTCHA_ENABLED}" \
        --env "SPRING_BOOT_ADMIN_CLIENT_ENABLED=${SECURITY_BOOT_ADMIN_CLIENT_ENABLED}" \
        --env "TDENGINE_USER=${TDENGINE_USERNAME}" \
        --env "TDENGINE_PASSWORD=${TDENGINE_PASSWORD}" \
        --env "JUSTAUTH_ENABLED=${INTEGRATION_JUSTAUTH_ENABLED}" \
        --env "WX_MP_APP_ID=${INTEGRATION_WX_MP_APP_ID}" \
        --env "WX_MP_SECRET=${INTEGRATION_WX_MP_SECRET}" \
        --env "WX_MINIAPP_APP_ID=${INTEGRATION_WX_MINIAPP_APP_ID}" \
        --env "WX_MINIAPP_SECRET=${INTEGRATION_WX_MINIAPP_SECRET}" \
        --env "MITEDTSM_TENCENT_LBS_KEY=${INTEGRATION_TENCENT_LBS_KEY}" \
        --env "MITEDTSM_PAY_ORDER_NOTIFY_URL=${INTEGRATION_PAY_ORDER_NOTIFY_URL}" \
        --env "MITEDTSM_PAY_REFUND_NOTIFY_URL=${INTEGRATION_PAY_REFUND_NOTIFY_URL}" \
        --env "MITEDTSM_PAY_TRANSFER_NOTIFY_URL=${INTEGRATION_PAY_TRANSFER_NOTIFY_URL}" \
        --env "MITEDTSM_EXPRESS_CLIENT=${INTEGRATION_EXPRESS_CLIENT}" \
        --env "MITEDTSM_EXPRESS_KDNIAO_API_KEY=${INTEGRATION_EXPRESS_KDNIAO_API_KEY}" \
        --env "MITEDTSM_EXPRESS_KDNIAO_BUSINESS_ID=${INTEGRATION_EXPRESS_KDNIAO_BUSINESS_ID}" \
        --env "MITEDTSM_EXPRESS_KDNIAO_REQUEST_TYPE=${INTEGRATION_EXPRESS_KDNIAO_REQUEST_TYPE}" \
        --env "MITEDTSM_EXPRESS_KD100_KEY=${INTEGRATION_EXPRESS_KD100_KEY}" \
        --env "MITEDTSM_EXPRESS_KD100_CUSTOMER=${INTEGRATION_EXPRESS_KD100_CUSTOMER}" \
        --env "MITEDTSM_CRM_MARKETING_PROVIDER_MODE=${CRM_MARKETING_PROVIDER_MODE}" \
        --env "MITEDTSM_CRM_MARKETING_TRACKING_ENABLED=${CRM_MARKETING_TRACKING_ENABLED}" \
        --env "MITEDTSM_CRM_MARKETING_PUBLIC_BASE_URL=${CRM_MARKETING_PUBLIC_BASE_URL}" \
        --env "MITEDTSM_CRM_MARKETING_DELIVERY_SYNC_BATCH_SIZE=${CRM_MARKETING_DELIVERY_SYNC_BATCH_SIZE}" \
        "$SERVER_IMAGE"
}

start_web_frontend() {
    printf 'Starting Web frontend container.\n'
    podman_cmd run -d --replace --name "$WEB_CONTAINER" --pod "$POD_NAME" --pull=never \
        "$WEB_IMAGE"
}

start_mall_frontend() {
    printf 'Starting Mall frontend container.\n'
    podman_cmd run -d --replace --name "$MALL_CONTAINER" --pod "$POD_NAME" --pull=never \
        "$MALL_IMAGE"
}

wait_for_frontends() {
    wait_for 'Web frontend' "$WEB_ATTEMPTS" host_curl --fail --silent --show-error "http://${HEALTH_HTTP_HOST}:${WEB_PORT}${WEB_HEALTH_PATH}"
    wait_for 'Mall frontend' "$MALL_ATTEMPTS" host_curl --fail --silent --show-error "http://${HEALTH_HTTP_HOST}:${MALL_PORT}${MALL_HEALTH_PATH}"
}

container_is_running() {
    local container="$1"
    [[ "$(podman_cmd inspect --format '{{.State.Running}}' "$container" 2>/dev/null)" == "true" ]]
}

require_runtime_images() {
    local image
    local -a images=(
        "$MYSQL_IMAGE"
        "$INIT_IMAGE"
        "$SERVER_IMAGE"
        "$WEB_IMAGE"
        "$MALL_IMAGE"
        "$REDIS_BASE_IMAGE"
        "$RABBITMQ_BASE_IMAGE"
        "$TDENGINE_BASE_IMAGE"
    )

    for image in "${images[@]}"; do
        podman_cmd image exists "$image" || {
            printf 'Required local image is unavailable: %s\n' "$image" >&2
            printf 'Run up.sh with operation.startup_mode=full to package it.\n' >&2
            exit 1
        }
    done
}

fast_start_existing_pod() {
    podman_cmd pod inspect "$POD_NAME" >/dev/null 2>&1 || {
        printf 'Pod does not exist: %s. Run up.sh with operation.startup_mode=full first.\n' "$POD_NAME" >&2
        return 1
    }

    local pod_state
    pod_state="$(podman_cmd pod inspect --format '{{.State}}' "$POD_NAME")"
    case "$pod_state" in
        Running)
            ;;
        Created|Exited|Stopped)
            printf 'Starting existing Pod %s without rebuilding images.\n' "$POD_NAME"
            podman_cmd pod start "$POD_NAME"
            ;;
        *)
            printf 'Pod %s is in state %s and cannot use startup_mode=fast. Use startup_mode=full.\n' \
                "$POD_NAME" "$pod_state" >&2
            return 1
            ;;
    esac

    podman_cmd container exists "$SERVER_CONTAINER" || {
        printf 'Server container is missing from Pod %s. Use startup_mode=full.\n' "$POD_NAME" >&2
        return 1
    }
    if ! container_is_running "$SERVER_CONTAINER"; then
        printf 'Starting existing server container without rebuilding images.\n'
        podman_cmd start "$SERVER_CONTAINER"
    fi

    if ! container_is_running "$WEB_CONTAINER" || ! container_is_running "$MALL_CONTAINER"; then
        podman_cmd image exists "$WEB_IMAGE" || {
            printf 'Required Web image is unavailable: %s. Use startup_mode=full first.\n' "$WEB_IMAGE" >&2
            return 1
        }
        podman_cmd image exists "$MALL_IMAGE" || {
            printf 'Required Mall image is unavailable: %s. Use startup_mode=full first.\n' "$MALL_IMAGE" >&2
            return 1
        }
        start_frontends
    fi

    wait_for 'Spring Boot server' "$SERVER_ATTEMPTS" host_curl --fail --silent --show-error "http://${HEALTH_HTTP_HOST}:${SERVER_PORT}${SERVER_HEALTH_PATH}" &
    local server_ready_pid=$!
    wait_for_frontends &
    local frontends_ready_pid=$!
    wait "$server_ready_pid"
    wait "$frontends_ready_pid"
}

show_access_urls() {
    trap - EXIT
    printf '\nRootless Podman Pod is running on the real host.\n'
    printf '  Web:    http://%s:%s%s\n' "$HOST_ADDRESS" "$WEB_PORT" "$WEB_HEALTH_PATH"
    printf '  Mall:   http://%s:%s%s\n' "$HOST_ADDRESS" "$MALL_PORT" "$MALL_HEALTH_PATH"
    printf '  Server: http://%s:%s%s\n' "$HOST_ADDRESS" "$SERVER_PORT" "$SERVER_HEALTH_PATH"
    podman_cmd ps --pod --filter "pod=${POD_NAME}" --format 'table {{.PodName}}\t{{.Names}}\t{{.Status}}\t{{.Ports}}'
}

show_logs_on_error() {
    local status=$?
    if ((status != 0)); then
        local pid active_jobs
        active_jobs="$(jobs -pr)" || true
        for pid in $active_jobs; do
            kill "$pid" 2>/dev/null || true
        done
        printf '\nStartup failed; recent container logs follow.\n' >&2
        local container
        for container in "$MYSQL_CONTAINER" "$REDIS_CONTAINER" "$RABBITMQ_CONTAINER" "$TDENGINE_CONTAINER" "$INIT_CONTAINER" "$SERVER_CONTAINER" "$WEB_CONTAINER" "$MALL_CONTAINER"; do
            if podman_cmd container exists "$container" 2>/dev/null; then
                printf '\n[%s]\n' "$container" >&2
                podman_cmd logs --tail 80 "$container" >&2 || true
            fi
        done
    fi
}

validate_configuration() {
    case "$START_MODE" in
        full|check|fast|no-build|frontends-only|rebuild-server|rebuild-web|rebuild-mall) ;;
        *)
            printf 'operation.startup_mode must be full, check, fast, no-build, frontends-only, rebuild-server, rebuild-web, or rebuild-mall; got: %s\n' "$START_MODE" >&2
            exit 2
            ;;
    esac

    case "$IMAGE_SOURCE" in
        auto|archive|pull) ;;
        *)
            printf 'IMAGE_SOURCE must be auto, archive, or pull; got: %s\n' "$IMAGE_SOURCE" >&2
            exit 2
            ;;
    esac

    [[ "$SERVER_HEALTH_PATH" == /* && "$WEB_HEALTH_PATH" == /* && "$MALL_HEALTH_PATH" == /* ]] || {
        printf 'All health-check paths must start with /.\n' >&2
        exit 2
    }

    if [[ "$HOST_ADDRESS:$SERVER_PORT" == "$HOST_ADDRESS:$WEB_PORT" ||
          "$HOST_ADDRESS:$SERVER_PORT" == "$HOST_ADDRESS:$MALL_PORT" ||
          "$HOST_ADDRESS:$WEB_PORT" == "$HOST_ADDRESS:$MALL_PORT" ]]; then
        printf 'Configured host ports must be unique.\n' >&2
        exit 2
    fi

    ((SECURITY_PASSWORD_ENCODER_LENGTH >= 10 && SECURITY_PASSWORD_ENCODER_LENGTH <= 16)) || {
        printf 'security.password_encoder_length must be between 10 and 16.\n' >&2
        exit 2
    }
    if [[ "$SECURITY_MOCK_LOGIN_ENABLED" == "true" && "$SECURITY_MOCK_SECRET" == "not-enabled" ]]; then
        printf 'security.mock_secret must be an explicit non-placeholder value when mock login is enabled.\n' >&2
        exit 2
    fi
    [[ "$SECURITY_ACTUATOR_EXPOSURE" == "health,info" || "$SECURITY_ACTUATOR_EXPOSURE" == "info,health" ]] || {
        printf 'security.actuator_exposure must contain only health and info.\n' >&2
        exit 2
    }
    if [[ "$SECURITY_CORS_ALLOW_CREDENTIALS" == "true" && "$SECURITY_CORS_ALLOWED_ORIGINS" == "*" ]]; then
        printf 'Credentialed CORS cannot use a wildcard origin.\n' >&2
        exit 2
    fi
    [[ "$INTEGRATION_PAY_ORDER_NOTIFY_URL" =~ ^https?://[^[:space:]]+$ &&
       "$INTEGRATION_PAY_REFUND_NOTIFY_URL" =~ ^https?://[^[:space:]]+$ &&
       "$INTEGRATION_PAY_TRANSFER_NOTIFY_URL" =~ ^https?://[^[:space:]]+$ ]] || {
        printf 'Integration callback URLs must be explicit HTTP(S) URLs.\n' >&2
        exit 2
    }

    if [[ "$USE_HOST_PROXY" == "true" ]]; then
        PODMAN_PROXY_ARGS=()
        if [[ "$HTTP_PROXY_URL" == "none" && "$HTTPS_PROXY_URL" == "none" && "$ALL_PROXY_URL" == "none" ]]; then
            printf 'At least one explicit proxy URL is required when network.use_host_proxy=true.\n' >&2
            exit 2
        fi
    fi
}

verify_rootless_podman() {
    require_command podman
    require_command curl

    local rootless
    rootless="$(podman_cmd info --format '{{.Host.Security.Rootless}}')" || {
        printf 'Podman is installed but not usable by this user. Run podman info for details.\n' >&2
        exit 1
    }
    [[ "$rootless" == "true" ]] || {
        printf 'Run this script as the normal rootless Podman user.\n' >&2
        exit 1
    }
}

configure_proxy() {
    if [[ "$USE_HOST_PROXY" == "false" ]]; then
        clear_host_proxy
        return
    fi

    # In this shared Pod, 127.0.0.1 is the Pod itself, not the real host.
    # Pasta's host.containers.internal mapping reaches the host loopback.
    local container_http_proxy='' container_https_proxy='' container_all_proxy=''
    [[ "$HTTP_PROXY_URL" == "none" ]] || container_http_proxy="$(container_proxy_url "$HTTP_PROXY_URL")"
    [[ "$HTTPS_PROXY_URL" == "none" ]] || container_https_proxy="$(container_proxy_url "$HTTPS_PROXY_URL")"
    [[ "$ALL_PROXY_URL" == "none" ]] || container_all_proxy="$(container_proxy_url "$ALL_PROXY_URL")"

    if [[ -n "$container_http_proxy" ]]; then
        PROXY_ENV+=(--env "http_proxy=${container_http_proxy}" --env "HTTP_PROXY=${container_http_proxy}")
    fi
    if [[ -n "$container_https_proxy" ]]; then
        PROXY_ENV+=(--env "https_proxy=${container_https_proxy}" --env "HTTPS_PROXY=${container_https_proxy}")
    fi
    if [[ -n "$container_all_proxy" ]]; then
        PROXY_ENV+=(--env "all_proxy=${container_all_proxy}" --env "ALL_PROXY=${container_all_proxy}")
    fi
    if ((${#PROXY_ENV[@]})); then
        PROXY_ENV+=(--env "no_proxy=${NO_PROXY_VALUE}" --env "NO_PROXY=${NO_PROXY_VALUE}")
    fi
}

require_project_assets() {
    require_file "${PROJECT_ROOT}/InitService/target/mitedtsm-init-service.jar"
    require_file "${PROJECT_ROOT}/Server/mitedtsm-server/target/mitedtsm-server.jar"
    require_file "${SCRIPT_DIR}/init/init-mysql.sh"
    require_dir "${PROJECT_ROOT}/database"
    require_web_assets
    require_mall_assets
}

require_web_assets() {
    require_file "${SCRIPT_DIR}/Containerfile"
    require_dir "${PROJECT_ROOT}/Web/dist-prod"
    require_file "${PROJECT_ROOT}/Web/dist-prod/index.html"
    verify_web_entry_assets "${PROJECT_ROOT}/Web/dist-prod"
}

require_mall_assets() {
    require_file "${SCRIPT_DIR}/Containerfile"
    require_dir "${PROJECT_ROOT}/MallFrontend/unpackage/dist/build/web"
    require_file "${PROJECT_ROOT}/MallFrontend/unpackage/dist/build/web/index.html"
    verify_web_entry_assets "${PROJECT_ROOT}/MallFrontend/unpackage/dist/build/web"
}

require_server_assets() {
    require_file "${SCRIPT_DIR}/Containerfile"
    require_file "${PROJECT_ROOT}/Server/mitedtsm-server/target/mitedtsm-server.jar"
}

apply_mysql_compatibility_migrations() {
    require_file "$MYSQL_COMPATIBILITY_MIGRATION_MANIFEST"
    local manifest_dir migration_path migration_file
    manifest_dir="$(cd -- "$(dirname -- "$MYSQL_COMPATIBILITY_MIGRATION_MANIFEST")" && pwd)"
    while IFS= read -r migration_path || [[ -n "$migration_path" ]]; do
        [[ -n "$migration_path" && "$migration_path" != \#* ]] || continue
        if [[ "$migration_path" == /* ]]; then
            migration_file="$migration_path"
        else
            migration_file="${manifest_dir}/${migration_path}"
        fi
        require_file "$migration_file"
        printf 'Applying idempotent MySQL compatibility migration: %s\n' "$(basename -- "$migration_file")"
        podman_cmd exec -i "$MYSQL_CONTAINER" \
            mysql "--default-character-set=${MYSQL_CHARACTER_SET}" \
            "-u${MYSQL_USER}" "-p${MYSQL_ROOT_PASSWORD}" "--database=${MYSQL_DATABASE}" \
            < "$migration_file"
    done < "$MYSQL_COMPATIBILITY_MIGRATION_MANIFEST"
}

apply_runtime_file_storage() {
    [[ "$FILE_STORAGE_MODE" == "database" ]] || {
        printf 'file.storage_mode currently supports only database; got: %s\n' "$FILE_STORAGE_MODE" >&2
        return 2
    }
    [[ "$FILE_PUBLIC_BASE_URL" =~ ^https?://[A-Za-z0-9._:/-]+$ ]] || {
        printf 'file.public_base_url must be a plain HTTP(S) URL; got: %s\n' "$FILE_PUBLIC_BASE_URL" >&2
        return 2
    }
    printf 'Selecting explicit runtime file client %s (%s storage).\n' "$FILE_CLIENT_ID" "$FILE_STORAGE_MODE"
    podman_cmd exec "$MYSQL_CONTAINER" mysql "--default-character-set=${MYSQL_CHARACTER_SET}" \
        "-u${MYSQL_USER}" "-p${MYSQL_ROOT_PASSWORD}" \
        "--database=${MYSQL_DATABASE}" -e \
        "UPDATE infra_file_config SET master=(id=${FILE_CLIENT_ID}), config=CASE WHEN id=${FILE_CLIENT_ID} THEN JSON_SET(config, '$.domain', '${FILE_PUBLIC_BASE_URL}') ELSE config END WHERE deleted=b'0';"
}

verify_web_entry_assets() {
    local web_output="$1"
    local entry_html="${web_output}/index.html"
    local asset_path
    local found_asset=false

    while IFS= read -r asset_path; do
        [[ -n "$asset_path" ]] || continue
        found_asset=true
        if [[ ! -s "${web_output}${asset_path}" ]]; then
            printf 'Web entry references a missing asset: %s%s\n' "$web_output" "$asset_path" >&2
            printf '%s\n' 'Rebuild Web successfully before running up.sh; do not deploy this output.' >&2
            exit 1
        fi
    done < <(sed -nE 's/.*(src|href)="(\/assets\/[^"?]+)(\?[^" ]*)?".*/\2/p' "$entry_html" | sort -u)

    if [[ "$found_asset" == false ]]; then
        printf 'Web entry does not reference any hashed assets: %s\n' "$entry_html" >&2
        exit 1
    fi
}

check_archive_mode_prerequisites() {
    [[ "$IMAGE_SOURCE" == "archive" ]] || return 0

    local image archive
    local -a images=(
        "$RUNTIME_BASE_IMAGE"
        "$MYSQL_BASE_IMAGE"
        "$REDIS_BASE_IMAGE"
        "$RABBITMQ_BASE_IMAGE"
        "$TDENGINE_BASE_IMAGE"
        "$NGINX_BASE_IMAGE"
    )
    local -a archives=(
        "$(image_archive_path "$RUNTIME_ARCHIVE")"
        "$(image_archive_path "$MYSQL_ARCHIVE")"
        "$(image_archive_path "$REDIS_ARCHIVE")"
        "$(image_archive_path "$RABBITMQ_ARCHIVE")"
        "$(image_archive_path "$TDENGINE_ARCHIVE")"
        "$(image_archive_path "$NGINX_ARCHIVE")"
    )

    for ((index = 0; index < ${#images[@]}; index++)); do
        image="${images[index]}"
        archive="${archives[index]}"
        podman_cmd image exists "$image" || require_file "$archive"
    done
}

build_runtime_images() {
    local -a build_args=(
        --build-arg "MYSQL_BASE_IMAGE=${MYSQL_BASE_IMAGE}"
        --build-arg "RUNTIME_BASE_IMAGE=${RUNTIME_BASE_IMAGE}"
        --build-arg "NGINX_BASE_IMAGE=${NGINX_BASE_IMAGE}"
    )

    printf 'Packaging existing runtime assets with podman/Containerfile.\n'
    podman_cmd build --pull=never "${build_args[@]}" --target mysql --tag "$MYSQL_IMAGE" --file "${SCRIPT_DIR}/Containerfile" "$PROJECT_ROOT"
    podman_cmd build --pull=never "${build_args[@]}" --target init-service --tag "$INIT_IMAGE" --file "${SCRIPT_DIR}/Containerfile" "$PROJECT_ROOT"
    podman_cmd build --pull=never "${build_args[@]}" --target server --tag "$SERVER_IMAGE" --file "${SCRIPT_DIR}/Containerfile" "$PROJECT_ROOT"
    podman_cmd build --pull=never "${build_args[@]}" --target web --tag "$WEB_IMAGE" --file "${SCRIPT_DIR}/Containerfile" "$PROJECT_ROOT"
    podman_cmd build --pull=never "${build_args[@]}" --target mall --tag "$MALL_IMAGE" --file "${SCRIPT_DIR}/Containerfile" "$PROJECT_ROOT"
}

build_web_image() {
    local -a build_args=(
        --build-arg "MYSQL_BASE_IMAGE=${MYSQL_BASE_IMAGE}"
        --build-arg "RUNTIME_BASE_IMAGE=${RUNTIME_BASE_IMAGE}"
        --build-arg "NGINX_BASE_IMAGE=${NGINX_BASE_IMAGE}"
    )

    printf 'Packaging current Web assets without rebuilding Java artifacts.\n'
    podman_cmd build --pull=never "${build_args[@]}" --target web --tag "$WEB_IMAGE" --file "${SCRIPT_DIR}/Containerfile" "$PROJECT_ROOT"
}

build_mall_image() {
    local -a build_args=(
        --build-arg "MYSQL_BASE_IMAGE=${MYSQL_BASE_IMAGE}"
        --build-arg "RUNTIME_BASE_IMAGE=${RUNTIME_BASE_IMAGE}"
        --build-arg "NGINX_BASE_IMAGE=${NGINX_BASE_IMAGE}"
    )

    printf 'Packaging current Mall H5 assets without rebuilding Java artifacts.\n'
    podman_cmd build --pull=never "${build_args[@]}" --target mall --tag "$MALL_IMAGE" --file "${SCRIPT_DIR}/Containerfile" "$PROJECT_ROOT"
}

build_server_image() {
    local -a build_args=(
        --build-arg "MYSQL_BASE_IMAGE=${MYSQL_BASE_IMAGE}"
        --build-arg "RUNTIME_BASE_IMAGE=${RUNTIME_BASE_IMAGE}"
        --build-arg "NGINX_BASE_IMAGE=${NGINX_BASE_IMAGE}"
    )
    printf 'Packaging current Server artifact without rebuilding frontends.\n'
    podman_cmd build --pull=never "${build_args[@]}" --target server --tag "$SERVER_IMAGE" --file "${SCRIPT_DIR}/Containerfile" "$PROJECT_ROOT"
}

rebuild_server_only() {
    podman_cmd pod inspect "$POD_NAME" >/dev/null 2>&1 || {
        printf 'Pod does not exist: %s. Use startup_mode=full first.\n' "$POD_NAME" >&2
        return 1
    }
    [[ "$(podman_cmd pod inspect --format '{{.State}}' "$POD_NAME")" == "Running" ]] || {
        printf 'Pod is not running: %s. Use startup_mode=fast or full.\n' "$POD_NAME" >&2
        return 1
    }
    ensure_image "$RUNTIME_BASE_IMAGE" "$(image_archive_path "$RUNTIME_ARCHIVE")"
    build_server_image
    container_is_running "$MYSQL_CONTAINER" || {
        printf 'MySQL container is not running: %s. Use startup_mode=full first.\n' "$MYSQL_CONTAINER" >&2
        return 1
    }
    apply_mysql_compatibility_migrations
    apply_runtime_file_storage
    if container_is_running "$SERVER_CONTAINER"; then
        podman_cmd stop --time "$STOP_TIMEOUT" "$SERVER_CONTAINER"
    fi
    start_server
    wait_for 'Spring Boot server' "$SERVER_ATTEMPTS" host_curl --fail --silent --show-error "http://${HEALTH_HTTP_HOST}:${SERVER_PORT}${SERVER_HEALTH_PATH}"
}

rebuild_web_only() {
    podman_cmd pod inspect "$POD_NAME" >/dev/null 2>&1 || {
        printf 'Pod does not exist: %s. Use startup_mode=full first.\n' "$POD_NAME" >&2
        return 1
    }
    [[ "$(podman_cmd pod inspect --format '{{.State}}' "$POD_NAME")" == "Running" ]] || {
        printf 'Pod is not running: %s. Use startup_mode=fast or full.\n' "$POD_NAME" >&2
        return 1
    }

    ensure_image "$NGINX_BASE_IMAGE" "$(image_archive_path "$NGINX_ARCHIVE")"
    build_web_image
    start_web_frontend
    wait_for 'Web frontend' "$WEB_ATTEMPTS" host_curl --fail --silent --show-error "http://${HEALTH_HTTP_HOST}:${WEB_PORT}${WEB_HEALTH_PATH}"
}

rebuild_mall_only() {
    podman_cmd pod inspect "$POD_NAME" >/dev/null 2>&1 || {
        printf 'Pod does not exist: %s. Use startup_mode=full first.\n' "$POD_NAME" >&2
        return 1
    }
    [[ "$(podman_cmd pod inspect --format '{{.State}}' "$POD_NAME")" == "Running" ]] || {
        printf 'Pod is not running: %s. Use startup_mode=fast or full.\n' "$POD_NAME" >&2
        return 1
    }

    ensure_image "$NGINX_BASE_IMAGE" "$(image_archive_path "$NGINX_ARCHIVE")"
    build_mall_image
    start_mall_frontend
    wait_for 'Mall frontend' "$MALL_ATTEMPTS" host_curl --fail --silent --show-error "http://${HEALTH_HTTP_HOST}:${MALL_PORT}${MALL_HEALTH_PATH}"
}

validate_configuration
verify_rootless_podman
configure_proxy

if [[ "$START_MODE" == "full" || "$START_MODE" == "check" ]]; then
    require_project_assets
    check_archive_mode_prerequisites
elif [[ "$START_MODE" == "no-build" ]]; then
    require_runtime_images
elif [[ "$START_MODE" == "rebuild-server" ]]; then
    require_server_assets
elif [[ "$START_MODE" == "rebuild-web" ]]; then
    require_web_assets
elif [[ "$START_MODE" == "rebuild-mall" ]]; then
    require_mall_assets
fi

if [[ "$START_MODE" == "check" ]]; then
    printf 'Podman deployment preflight passed. No images were loaded, pulled, built, or started.\n'
    exit 0
fi

trap show_logs_on_error EXIT

if [[ "$START_MODE" == "fast" ]]; then
    fast_start_existing_pod
    show_access_urls
    exit 0
fi

if [[ "$START_MODE" == "frontends-only" ]]; then
    podman_cmd pod inspect "$POD_NAME" >/dev/null 2>&1 || {
        printf 'Pod does not exist: %s. Use startup_mode=full first.\n' "$POD_NAME" >&2
        exit 1
    }
    [[ "$(podman_cmd pod inspect --format '{{.State}}' "$POD_NAME")" == "Running" ]] || {
        printf 'Pod is not running: %s. Use startup_mode=full to recreate it.\n' "$POD_NAME" >&2
        exit 1
    }
    podman_cmd image exists "$WEB_IMAGE" || {
        printf 'Required Web image is unavailable: %s. Use startup_mode=full first.\n' "$WEB_IMAGE" >&2
        exit 1
    }
    podman_cmd image exists "$MALL_IMAGE" || {
        printf 'Required Mall image is unavailable: %s. Use startup_mode=full first.\n' "$MALL_IMAGE" >&2
        exit 1
    }

    start_frontends
    wait_for_frontends
    show_access_urls
    exit 0
fi

if [[ "$START_MODE" == "rebuild-server" ]]; then
    rebuild_server_only
    show_access_urls
    exit 0
fi

if [[ "$START_MODE" == "rebuild-web" ]]; then
    rebuild_web_only
    show_access_urls
    exit 0
fi

if [[ "$START_MODE" == "rebuild-mall" ]]; then
    rebuild_mall_only
    show_access_urls
    exit 0
fi

if [[ "$START_MODE" == "full" ]]; then
    ensure_image "$RUNTIME_BASE_IMAGE" "$(image_archive_path "$RUNTIME_ARCHIVE")"
    ensure_image "$MYSQL_BASE_IMAGE" "$(image_archive_path "$MYSQL_ARCHIVE")"
    ensure_image "$REDIS_BASE_IMAGE" "$(image_archive_path "$REDIS_ARCHIVE")"
    ensure_image "$RABBITMQ_BASE_IMAGE" "$(image_archive_path "$RABBITMQ_ARCHIVE")"
    ensure_image "$TDENGINE_BASE_IMAGE" "$(image_archive_path "$TDENGINE_ARCHIVE")"
    ensure_image "$NGINX_BASE_IMAGE" "$(image_archive_path "$NGINX_ARCHIVE")"

    build_runtime_images
fi

ensure_volume "$MYSQL_VOLUME"
ensure_volume "$REDIS_VOLUME"
ensure_volume "$RABBITMQ_VOLUME"
ensure_volume "$TDENGINE_VOLUME"

if podman_cmd pod inspect "$POD_NAME" >/dev/null 2>&1; then
    printf 'Replacing existing Pod %s gracefully (timeout: %ss).\n' "$POD_NAME" "$STOP_TIMEOUT"
    # Keep databases and brokers available while Spring executes shutdown hooks.
    if container_is_running "$SERVER_CONTAINER"; then
        printf 'Stopping application server before infrastructure.\n'
        podman_cmd stop --time "$STOP_TIMEOUT" "$SERVER_CONTAINER"
    fi
    if ! podman_cmd pod stop --time "$STOP_TIMEOUT" "$POD_NAME"; then
        printf 'Graceful stop failed; forcibly removing Pod %s.\n' "$POD_NAME" >&2
        podman_cmd pod rm --force "$POD_NAME"
    else
        podman_cmd pod rm "$POD_NAME"
    fi
fi

printf 'Creating rootless Podman Pod %s.\n' "$POD_NAME"
podman_cmd pod create \
    --name "$POD_NAME" \
    --publish "${HOST_ADDRESS}:${SERVER_PORT}:${SERVER_CONTAINER_PORT}" \
    --publish "${HOST_ADDRESS}:${WEB_PORT}:${WEB_CONTAINER_PORT}" \
    --publish "${HOST_ADDRESS}:${MALL_PORT}:${MALL_CONTAINER_PORT}"

printf 'Starting infrastructure containers.\n'
podman_cmd run -d --replace --name "$MYSQL_CONTAINER" --pod "$POD_NAME" --pull=never \
    --volume "${MYSQL_VOLUME}:/var/lib/mysql" \
    --env "MYSQL_DATABASE=${MYSQL_DATABASE}" \
    --env "MYSQL_DATASET=${MYSQL_DATASET}" \
    --env "MYSQL_ROOT_PASSWORD=${MYSQL_ROOT_PASSWORD}" \
    --env "TZ=${MYSQL_TIMEZONE}" \
    "$MYSQL_IMAGE" \
    "--character-set-server=${MYSQL_CHARACTER_SET}" \
    "--collation-server=${MYSQL_COLLATION}" \
    "--default-authentication-plugin=${MYSQL_AUTHENTICATION_PLUGIN}"

podman_cmd run -d --replace --name "$REDIS_CONTAINER" --pod "$POD_NAME" --pull=never \
    --volume "${REDIS_VOLUME}:/data" \
    "$REDIS_BASE_IMAGE"

podman_cmd run -d --replace --name "$RABBITMQ_CONTAINER" --pod "$POD_NAME" --pull=never \
    --volume "${RABBITMQ_VOLUME}:/var/lib/rabbitmq" \
    --env "RABBITMQ_DEFAULT_USER=${RABBITMQ_USERNAME}" \
    --env "RABBITMQ_DEFAULT_PASS=${RABBITMQ_PASSWORD}" \
    "$RABBITMQ_BASE_IMAGE"

podman_cmd run -d --replace --name "$TDENGINE_CONTAINER" --pod "$POD_NAME" --pull=never \
    --volume "${TDENGINE_VOLUME}:/var/lib/taos" \
    --env "TAOS_FQDN=${TDENGINE_FQDN}" \
    "$TDENGINE_BASE_IMAGE"

wait_for 'MySQL' "$MYSQL_ATTEMPTS" podman_cmd exec "$MYSQL_CONTAINER" mysqladmin ping -h "$MYSQL_HEALTH_HOST" --silent &
mysql_ready_pid=$!
wait_for 'MySQL schema initialization' "$MYSQL_SCHEMA_ATTEMPTS" podman_cmd exec "$MYSQL_CONTAINER" \
    mysql "--default-character-set=${MYSQL_CHARACTER_SET}" \
    "-u${MYSQL_USER}" "-p${MYSQL_ROOT_PASSWORD}" "--database=${MYSQL_DATABASE}" -Nse "$MYSQL_SCHEMA_QUERY" &
mysql_schema_ready_pid=$!
wait_for 'Redis' "$REDIS_ATTEMPTS" podman_cmd exec "$REDIS_CONTAINER" redis-cli ping &
redis_ready_pid=$!
# Run the probe as RabbitMQ's own user. Running this Erlang client as root
# during the broker's first few seconds creates a root-owned cookie and makes
# the broker's later privilege drop fail.
wait_for 'RabbitMQ' "$RABBITMQ_ATTEMPTS" podman_cmd exec --user "$RABBITMQ_OS_USER" "$RABBITMQ_CONTAINER" /opt/rabbitmq/sbin/rabbitmq-diagnostics -q ping &
rabbitmq_ready_pid=$!
wait_for 'TDengine' "$TDENGINE_ATTEMPTS" podman_cmd exec "$TDENGINE_CONTAINER" taos -s "$TDENGINE_HEALTH_QUERY" &
tdengine_ready_pid=$!

# The InitService only talks to TDengine, so run it while MySQL, Redis, and
# RabbitMQ finish their own health checks instead of serializing those waits.
wait "$tdengine_ready_pid"
initialize_tdengine &
tdengine_init_pid=$!

wait "$mysql_ready_pid"
wait "$mysql_schema_ready_pid"
apply_mysql_compatibility_migrations
apply_runtime_file_storage
wait "$redis_ready_pid"
wait "$rabbitmq_ready_pid"
wait "$tdengine_init_pid"

printf 'Starting server and frontends.\n'
start_server
# Do not expose a ready-looking Web UI while its API upstream is still booting.
wait_for 'Spring Boot server' "$SERVER_ATTEMPTS" host_curl --fail --silent --show-error "http://${HEALTH_HTTP_HOST}:${SERVER_PORT}${SERVER_HEALTH_PATH}"
start_frontends
wait_for_frontends
show_access_urls
