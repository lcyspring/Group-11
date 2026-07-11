#!/usr/bin/env bash
# Start Mitedtsm directly on the real host with one rootless Podman Pasta Pod.
# This directory is self-contained and does not use docker-compose/.

set -Eeuo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd -- "${SCRIPT_DIR}/.." && pwd)"
PODMAN=(podman)

POD_NAME="${POD_NAME:-mitedtsm-rootless}"
SERVER_PORT="${SERVER_PORT:-8080}"
WEB_PORT="${WEB_PORT:-8081}"
MALL_PORT="${MALL_PORT:-8082}"
# Used when replacing an already-running Pod. The Java server may wait for
# Quartz jobs during shutdown, so this intentionally exceeds Podman's 10s default.
STOP_TIMEOUT="${STOP_TIMEOUT:-120}"
# auto: load a local archive when present, otherwise pull from its registry.
# archive: never use registries; pull: always pull from registries.
IMAGE_SOURCE="${IMAGE_SOURCE:-auto}"
# Proxy use is opt-in. Set USE_HOST_PROXY=true to reuse host proxy settings.
USE_HOST_PROXY="${USE_HOST_PROXY:-false}"
# Podman/Pasta creates this dedicated host mapping in each container's
# /etc/hosts. Use it for a proxy listening on the real host's loopback.
HOST_PROXY_NAME="host.containers.internal"

MYSQL_CONTAINER="${POD_NAME}-mysql"
REDIS_CONTAINER="${POD_NAME}-redis"
RABBITMQ_CONTAINER="${POD_NAME}-rabbitmq"
TDENGINE_CONTAINER="${POD_NAME}-tdengine"
INIT_CONTAINER="${POD_NAME}-init"
SERVER_CONTAINER="${POD_NAME}-server"
WEB_CONTAINER="${POD_NAME}-web"
MALL_CONTAINER="${POD_NAME}-mall"

MYSQL_VOLUME="${POD_NAME}-mysql-data"
REDIS_VOLUME="${POD_NAME}-redis-data"
RABBITMQ_VOLUME="${POD_NAME}-rabbitmq-data"
TDENGINE_VOLUME="${POD_NAME}-tdengine-data"

MYSQL_IMAGE="${POD_NAME}-mysql:latest"
INIT_IMAGE="${POD_NAME}-init-service:latest"
SERVER_IMAGE="${POD_NAME}-server:latest"
WEB_IMAGE="${POD_NAME}-web:latest"
MALL_IMAGE="${POD_NAME}-mall:latest"
PODMAN_PROXY_ARGS=(--http-proxy=false)
START_MODE=full

usage() {
    cat <<'EOF'
Usage: bash ./up.sh [--frontends-only]

Without arguments, build runtime images and start the complete Pod.
--frontends-only starts or replaces only the Web and Mall Nginx containers in
an existing Pod. It does not build images, recreate the Pod, or alter data.
EOF
}

case "$#" in
    0) ;;
    1)
        case "$1" in
            --frontends-only)
                START_MODE=frontends-only
                ;;
            -h|--help)
                usage
                exit 0
                ;;
            *)
                usage >&2
                exit 2
                ;;
        esac
        ;;
    *)
        usage >&2
        exit 2
        ;;
esac

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

ensure_image() {
    local image="$1"
    local archive="$2"

    if [[ "$IMAGE_SOURCE" != "pull" ]] && podman_cmd image exists "$image"; then
        return
    fi

    case "$IMAGE_SOURCE" in
        auto)
            if [[ -r "$archive" ]]; then
                printf 'Loading %s from %s\n' "$image" "$archive"
                podman_cmd load --input "$archive"
            else
                printf 'Archive not found; pulling %s\n' "$image"
                podman_cmd pull "$image"
            fi
            ;;
        archive)
            [[ -r "$archive" ]] || {
                printf 'Missing required image archive: %s\n' "$archive" >&2
                exit 1
            }
            printf 'Loading %s from %s\n' "$image" "$archive"
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
        sleep 2
    done

    printf 'Timed out waiting for %s.\n' "$description" >&2
    if [[ -n "$last_output" ]]; then
        printf 'Last %s probe output:\n%s\n' "$description" "${last_output:0:4000}" >&2
    fi
    return 1
}

start_frontends() {
    printf 'Starting frontend containers.\n'
    podman_cmd run -d --replace --name "$WEB_CONTAINER" --pod "$POD_NAME" --pull=never \
        "$WEB_IMAGE"
    podman_cmd run -d --replace --name "$MALL_CONTAINER" --pod "$POD_NAME" --pull=never \
        "$MALL_IMAGE"
}

wait_for_frontends() {
    wait_for 'Web frontend' 30 host_curl --fail --silent --show-error "http://127.0.0.1:${WEB_PORT}/"
    wait_for 'Mall frontend' 30 host_curl --fail --silent --show-error "http://127.0.0.1:${MALL_PORT}/"
}

show_access_urls() {
    trap - EXIT
    printf '\nRootless Pasta Pod is running on the real host.\n'
    printf '  Web:    http://127.0.0.1:%s/\n' "$WEB_PORT"
    printf '  Mall:   http://127.0.0.1:%s/\n' "$MALL_PORT"
    printf '  Server: http://127.0.0.1:%s/actuator/health\n' "$SERVER_PORT"
    podman_cmd ps --pod --format 'table {{.PodName}}\t{{.Names}}\t{{.Status}}\t{{.Ports}}'
}

show_logs_on_error() {
    local status=$?
    if ((status != 0)); then
        printf '\nStartup failed; recent container logs follow.\n' >&2
        for container in "$MYSQL_CONTAINER" "$REDIS_CONTAINER" "$RABBITMQ_CONTAINER" "$TDENGINE_CONTAINER" "$INIT_CONTAINER" "$SERVER_CONTAINER" "$WEB_CONTAINER" "$MALL_CONTAINER"; do
            if podman_cmd container exists "$container" 2>/dev/null; then
                printf '\n[%s]\n' "$container" >&2
                podman_cmd logs --tail 80 "$container" >&2 || true
            fi
        done
    fi
}

trap show_logs_on_error EXIT

command -v podman >/dev/null || {
    printf 'Podman is required.\n' >&2
    exit 1
}

case "$USE_HOST_PROXY" in
    true|TRUE|1|yes|YES)
        USE_HOST_PROXY=true
        PODMAN_PROXY_ARGS=()
        ;;
    false|FALSE|0|no|NO|'')
        USE_HOST_PROXY=false
        ;;
    *)
        printf 'USE_HOST_PROXY must be true or false; got: %s\n' "$USE_HOST_PROXY" >&2
        exit 1
        ;;
esac

if [[ "$USE_HOST_PROXY" == "false" ]]; then
    clear_host_proxy
fi

if [[ "$(podman_cmd info --format '{{.Host.Security.Rootless}}')" != "true" ]]; then
    printf 'Run this script as the normal rootless Podman user.\n' >&2
    exit 1
fi

case "$IMAGE_SOURCE" in
    auto|archive|pull) ;;
    *)
        printf 'IMAGE_SOURCE must be auto, archive, or pull; got: %s\n' "$IMAGE_SOURCE" >&2
        exit 1
        ;;
esac

[[ "$STOP_TIMEOUT" =~ ^[1-9][0-9]*$ ]] || {
    printf 'STOP_TIMEOUT must be a positive number of seconds; got: %s\n' "$STOP_TIMEOUT" >&2
    exit 1
}

PROXY_ENV=()
if [[ "$USE_HOST_PROXY" == "true" ]]; then
    # In this shared Pod, 127.0.0.1 is the Pod itself, not the real host.
    # Pasta's host.containers.internal mapping reaches the host loopback.
    CONTAINER_HTTP_PROXY="${CONTAINER_HTTP_PROXY:-$(container_proxy_url "${http_proxy:-${HTTP_PROXY:-}}")}"
    CONTAINER_HTTPS_PROXY="${CONTAINER_HTTPS_PROXY:-$(container_proxy_url "${https_proxy:-${HTTPS_PROXY:-}}")}"
    CONTAINER_ALL_PROXY="${CONTAINER_ALL_PROXY:-$(container_proxy_url "${all_proxy:-${ALL_PROXY:-}}")}"
    if [[ -n "$CONTAINER_HTTP_PROXY" ]]; then
        PROXY_ENV+=(--env "http_proxy=${CONTAINER_HTTP_PROXY}" --env "HTTP_PROXY=${CONTAINER_HTTP_PROXY}")
    fi
    if [[ -n "$CONTAINER_HTTPS_PROXY" ]]; then
        PROXY_ENV+=(--env "https_proxy=${CONTAINER_HTTPS_PROXY}" --env "HTTPS_PROXY=${CONTAINER_HTTPS_PROXY}")
    fi
    if [[ -n "$CONTAINER_ALL_PROXY" ]]; then
        PROXY_ENV+=(--env "all_proxy=${CONTAINER_ALL_PROXY}" --env "ALL_PROXY=${CONTAINER_ALL_PROXY}")
    fi
    if ((${#PROXY_ENV[@]})); then
        PROXY_ENV+=(--env "no_proxy=127.0.0.1,localhost,${HOST_PROXY_NAME}" --env "NO_PROXY=127.0.0.1,localhost,${HOST_PROXY_NAME}")
    fi
fi

if [[ "$START_MODE" == "frontends-only" ]]; then
    podman_cmd pod inspect "$POD_NAME" >/dev/null 2>&1 || {
        printf 'Pod does not exist: %s. Run bash ./up.sh first.\n' "$POD_NAME" >&2
        exit 1
    }
    podman_cmd image exists "$WEB_IMAGE" || {
        printf 'Required Web image is unavailable: %s. Run bash ./up.sh first.\n' "$WEB_IMAGE" >&2
        exit 1
    }
    podman_cmd image exists "$MALL_IMAGE" || {
        printf 'Required Mall image is unavailable: %s. Run bash ./up.sh first.\n' "$MALL_IMAGE" >&2
        exit 1
    }

    start_frontends
    wait_for_frontends
    show_access_urls
    exit 0
fi

require_file "${PROJECT_ROOT}/InitService/target/mitedtsm-init-service.jar"
require_file "${PROJECT_ROOT}/Server/mitedtsm-server/target/mitedtsm-server.jar"
require_file "${SCRIPT_DIR}/init/init-mysql.sh"
require_dir "${PROJECT_ROOT}/database"
require_dir "${PROJECT_ROOT}/Web/dist-prod"
require_dir "${PROJECT_ROOT}/MallFrontend/unpackage/dist/build/web"

ensure_image "docker.io/library/eclipse-temurin:17-jdk" "${PROJECT_ROOT}/docker-images/eclipse-temurin-17-jdk.tar"
ensure_image "docker.io/library/mysql:8.0" "${PROJECT_ROOT}/docker-images/mysql-8.0.tar"
ensure_image "docker.io/library/redis:6-alpine" "${PROJECT_ROOT}/docker-images/redis-6-alpine.tar"
ensure_image "docker.io/library/rabbitmq:3-management-alpine" "${PROJECT_ROOT}/docker-images/rabbitmq-3-management-alpine.tar"
ensure_image "docker.io/tdengine/tdengine:3.3.6.0" "${PROJECT_ROOT}/docker-images/tdengine-3.3.6.0.tar"
ensure_image "docker.io/library/nginx:stable-alpine" "${PROJECT_ROOT}/docker-images/nginx-stable-alpine.tar"

printf 'Packaging existing runtime assets with podman/Containerfile.\n'
podman_cmd build --pull=never --target mysql --tag "$MYSQL_IMAGE" --file "${SCRIPT_DIR}/Containerfile" "$PROJECT_ROOT"
podman_cmd build --pull=never --target init-service --tag "$INIT_IMAGE" --file "${SCRIPT_DIR}/Containerfile" "$PROJECT_ROOT"
podman_cmd build --pull=never --target server --tag "$SERVER_IMAGE" --file "${SCRIPT_DIR}/Containerfile" "$PROJECT_ROOT"
podman_cmd build --pull=never --target web --tag "$WEB_IMAGE" --file "${SCRIPT_DIR}/Containerfile" "$PROJECT_ROOT"
podman_cmd build --pull=never --target mall --tag "$MALL_IMAGE" --file "${SCRIPT_DIR}/Containerfile" "$PROJECT_ROOT"

ensure_volume "$MYSQL_VOLUME"
ensure_volume "$REDIS_VOLUME"
ensure_volume "$RABBITMQ_VOLUME"
ensure_volume "$TDENGINE_VOLUME"

if podman_cmd pod inspect "$POD_NAME" >/dev/null 2>&1; then
    printf 'Replacing existing Pod %s gracefully (timeout: %ss).\n' "$POD_NAME" "$STOP_TIMEOUT"
    if ! podman_cmd pod stop --time "$STOP_TIMEOUT" "$POD_NAME"; then
        printf 'Graceful stop failed; forcibly removing Pod %s.\n' "$POD_NAME" >&2
        podman_cmd pod rm --force "$POD_NAME"
    else
        podman_cmd pod rm "$POD_NAME"
    fi
fi

printf 'Creating rootless Pasta Pod %s.\n' "$POD_NAME"
podman_cmd pod create \
    --name "$POD_NAME" \
    --publish "${SERVER_PORT}:8080" \
    --publish "${WEB_PORT}:8081" \
    --publish "${MALL_PORT}:8082"

printf 'Starting infrastructure containers.\n'
podman_cmd run -d --replace --name "$MYSQL_CONTAINER" --pod "$POD_NAME" --pull=never \
    --volume "${MYSQL_VOLUME}:/var/lib/mysql" \
    --env MYSQL_DATABASE=mitedtsm_database \
    --env MYSQL_ROOT_PASSWORD=1234 \
    "$MYSQL_IMAGE" \
    --character-set-server=utf8mb4 \
    --collation-server=utf8mb4_unicode_ci \
    --default-authentication-plugin=mysql_native_password

podman_cmd run -d --replace --name "$REDIS_CONTAINER" --pod "$POD_NAME" --pull=never \
    --volume "${REDIS_VOLUME}:/data" \
    redis:6-alpine

podman_cmd run -d --replace --name "$RABBITMQ_CONTAINER" --pod "$POD_NAME" --pull=never \
    --volume "${RABBITMQ_VOLUME}:/var/lib/rabbitmq" \
    --env RABBITMQ_DEFAULT_USER=rabbit \
    --env RABBITMQ_DEFAULT_PASS=rabbit \
    rabbitmq:3-management-alpine

podman_cmd run -d --replace --name "$TDENGINE_CONTAINER" --pod "$POD_NAME" --pull=never \
    --volume "${TDENGINE_VOLUME}:/var/lib/taos" \
    --env TAOS_FQDN=localhost \
    tdengine/tdengine:3.3.6.0

wait_for 'MySQL' 90 podman_cmd exec "$MYSQL_CONTAINER" mysqladmin ping -h 127.0.0.1 --silent
wait_for 'MySQL schema initialization' 180 podman_cmd exec "$MYSQL_CONTAINER" sh -ec \
    'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" -e "SELECT 1 FROM mitedtsm_database.system_users LIMIT 1" >/dev/null'
wait_for 'Redis' 60 podman_cmd exec "$REDIS_CONTAINER" redis-cli ping
wait_for 'RabbitMQ' 90 podman_cmd exec "$RABBITMQ_CONTAINER" /opt/rabbitmq/sbin/rabbitmq-diagnostics -q ping
wait_for 'TDengine' 120 podman_cmd exec "$TDENGINE_CONTAINER" taos -s 'SHOW DATABASES;'

printf 'Initializing TDengine database.\n'
podman_cmd run --rm --replace --name "$INIT_CONTAINER" --pod "$POD_NAME" --pull=never \
    --env TDENGINE_HOST=127.0.0.1 \
    --env TDENGINE_PORT=6041 \
    "$INIT_IMAGE"

printf 'Starting server and frontends.\n'
podman_cmd run -d --replace --name "$SERVER_CONTAINER" --pod "$POD_NAME" --pull=never \
    "${PROXY_ENV[@]}" \
    --env SPRING_PROFILES_ACTIVE=local \
    "$SERVER_IMAGE"

wait_for 'Spring Boot server' 180 host_curl --fail --silent --show-error "http://127.0.0.1:${SERVER_PORT}/actuator/health"

start_frontends
wait_for_frontends
show_access_urls
