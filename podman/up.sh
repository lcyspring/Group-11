#!/usr/bin/env bash
# Start Mitedtsm directly on the real host with one rootless Podman Pod.
# Docker Engine, the Docker CLI, and Compose are deliberately not used.

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
# auto: load a local OCI archive when present, otherwise pull from its registry.
# archive: never use registries; pull: always pull from registries.
IMAGE_SOURCE="${IMAGE_SOURCE:-auto}"
# This directory is populated with Podman's image-archives.sh.
IMAGE_ARCHIVE_DIR="${IMAGE_ARCHIVE_DIR:-${SCRIPT_DIR}/images}"
# Proxy use is opt-in. Set USE_HOST_PROXY=true to reuse host proxy settings.
USE_HOST_PROXY="${USE_HOST_PROXY:-false}"
# Podman/Pasta creates this dedicated host mapping in each container's
# /etc/hosts. Use it for a proxy listening on the real host's loopback.
HOST_PROXY_NAME="host.containers.internal"

# Use fully qualified references so Podman never needs short-name resolution.
RUNTIME_BASE_IMAGE="${RUNTIME_BASE_IMAGE:-docker.io/library/eclipse-temurin:17-jdk}"
MYSQL_BASE_IMAGE="${MYSQL_BASE_IMAGE:-docker.io/library/mysql:8.0}"
REDIS_BASE_IMAGE="${REDIS_BASE_IMAGE:-docker.io/library/redis:6-alpine}"
RABBITMQ_BASE_IMAGE="${RABBITMQ_BASE_IMAGE:-docker.io/library/rabbitmq:3-management-alpine}"
TDENGINE_BASE_IMAGE="${TDENGINE_BASE_IMAGE:-docker.io/tdengine/tdengine:3.3.6.0}"
NGINX_BASE_IMAGE="${NGINX_BASE_IMAGE:-docker.io/library/nginx:stable-alpine}"

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

# `localhost/` explicitly selects Podman's local image storage.
MYSQL_IMAGE="localhost/${POD_NAME}-mysql:latest"
INIT_IMAGE="localhost/${POD_NAME}-init-service:latest"
SERVER_IMAGE="localhost/${POD_NAME}-server:latest"
WEB_IMAGE="localhost/${POD_NAME}-web:latest"
MALL_IMAGE="localhost/${POD_NAME}-mall:latest"
PODMAN_PROXY_ARGS=(--http-proxy=false)
START_MODE=full
PROXY_ENV=()

usage() {
    cat <<'EOF'
Usage: bash ./up.sh [--check|--fast|--no-build|--frontends-only|--rebuild-web]

Without arguments, build runtime images and start the complete rootless Pod.
--check verifies the Podman deployment prerequisites only. It never loads,
        pulls, builds, creates, or starts anything.
--fast starts an existing Pod without packaging images or recreating containers.
        It is the quickest restart path after `podman pod stop` or a reboot.
--no-build recreates the complete Pod from existing local images. Use it after
        `down.sh` when application artifacts have not changed.
--frontends-only starts or replaces only the Web and Mall Nginx containers in
        an existing running Pod. It does not build images, recreate the Pod,
        or alter persistent data.
--rebuild-web packages the current Web/dist-prod/ into the Web Nginx image and
        replaces only that container in an existing running Pod. It does not
        rebuild Java artifacts or restart Spring Boot, databases, or Mall.
        Use it only after confirming the deployed Java artifacts are current.

Optional environment variables:
  IMAGE_SOURCE=auto|archive|pull
  IMAGE_ARCHIVE_DIR=/absolute/path/to/oci-archives
  SERVER_PORT=8080 WEB_PORT=8081 MALL_PORT=8082
  USE_HOST_PROXY=true
EOF
}

case "$#" in
    0) ;;
    1)
        case "$1" in
            --check)
                START_MODE=check
                ;;
            --fast)
                START_MODE=fast
                ;;
            --no-build)
                START_MODE=no-build
                ;;
            --frontends-only)
                START_MODE=frontends-only
                ;;
            --rebuild-web)
                START_MODE=rebuild-web
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

validate_port() {
    local name="$1"
    local port="$2"
    [[ "$port" =~ ^[1-9][0-9]{0,4}$ ]] && ((port <= 65535)) || {
        printf '%s must be a TCP port between 1 and 65535; got: %s\n' "$name" "$port" >&2
        exit 2
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
        sleep 2
    done

    printf 'Timed out waiting for %s.\n' "$description" >&2
    if [[ -n "$last_output" ]]; then
        printf 'Last %s probe output:\n%s\n' "$description" "${last_output:0:4000}" >&2
    fi
    return 1
}

initialize_tdengine() {
    local max_attempts=30
    local attempt exit_code

    for ((attempt = 1; attempt <= max_attempts; attempt++)); do
        if ((attempt == 1)); then
            printf 'Initializing TDengine database.\n'
        else
            printf 'TDengine is reachable but not ready to create a database; retrying (%s/%s).\n' \
                "$attempt" "$max_attempts" >&2
            sleep 2
        fi

        podman_cmd run -d --replace --name "$INIT_CONTAINER" --pod "$POD_NAME" --pull=never \
            --env TDENGINE_HOST=127.0.0.1 \
            --env TDENGINE_PORT=6041 \
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

start_web_frontend() {
    printf 'Starting Web frontend container.\n'
    podman_cmd run -d --replace --name "$WEB_CONTAINER" --pod "$POD_NAME" --pull=never \
        "$WEB_IMAGE"
}

wait_for_frontends() {
    wait_for 'Web frontend' 30 host_curl --fail --silent --show-error "http://127.0.0.1:${WEB_PORT}/"
    wait_for 'Mall frontend' 30 host_curl --fail --silent --show-error "http://127.0.0.1:${MALL_PORT}/"
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
            printf 'Run bash ./up.sh once without --no-build to package it.\n' >&2
            exit 1
        }
    done
}

fast_start_existing_pod() {
    podman_cmd pod inspect "$POD_NAME" >/dev/null 2>&1 || {
        printf 'Pod does not exist: %s. Run bash ./up.sh first.\n' "$POD_NAME" >&2
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
            printf 'Pod %s is in state %s and cannot use --fast. Run bash ./up.sh.\n' \
                "$POD_NAME" "$pod_state" >&2
            return 1
            ;;
    esac

    podman_cmd container exists "$SERVER_CONTAINER" || {
        printf 'Server container is missing from Pod %s. Run bash ./up.sh.\n' "$POD_NAME" >&2
        return 1
    }
    if ! container_is_running "$SERVER_CONTAINER"; then
        printf 'Starting existing server container without rebuilding images.\n'
        podman_cmd start "$SERVER_CONTAINER"
    fi

    if ! container_is_running "$WEB_CONTAINER" || ! container_is_running "$MALL_CONTAINER"; then
        podman_cmd image exists "$WEB_IMAGE" || {
            printf 'Required Web image is unavailable: %s. Run bash ./up.sh first.\n' "$WEB_IMAGE" >&2
            return 1
        }
        podman_cmd image exists "$MALL_IMAGE" || {
            printf 'Required Mall image is unavailable: %s. Run bash ./up.sh first.\n' "$MALL_IMAGE" >&2
            return 1
        }
        start_frontends
    fi

    wait_for 'Spring Boot server' 180 host_curl --fail --silent --show-error "http://127.0.0.1:${SERVER_PORT}/actuator/health" &
    local server_ready_pid=$!
    wait_for_frontends &
    local frontends_ready_pid=$!
    wait "$server_ready_pid"
    wait "$frontends_ready_pid"
}

show_access_urls() {
    trap - EXIT
    printf '\nRootless Podman Pod is running on the real host.\n'
    printf '  Web:    http://127.0.0.1:%s/\n' "$WEB_PORT"
    printf '  Mall:   http://127.0.0.1:%s/\n' "$MALL_PORT"
    printf '  Server: http://127.0.0.1:%s/actuator/health\n' "$SERVER_PORT"
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

    [[ "$STOP_TIMEOUT" =~ ^[1-9][0-9]*$ ]] || {
        printf 'STOP_TIMEOUT must be a positive number of seconds; got: %s\n' "$STOP_TIMEOUT" >&2
        exit 2
    }
    validate_port SERVER_PORT "$SERVER_PORT"
    validate_port WEB_PORT "$WEB_PORT"
    validate_port MALL_PORT "$MALL_PORT"
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
    local container_http_proxy container_https_proxy container_all_proxy
    container_http_proxy="${CONTAINER_HTTP_PROXY:-$(container_proxy_url "${http_proxy:-${HTTP_PROXY:-}}")}"
    container_https_proxy="${CONTAINER_HTTPS_PROXY:-$(container_proxy_url "${https_proxy:-${HTTPS_PROXY:-}}")}"
    container_all_proxy="${CONTAINER_ALL_PROXY:-$(container_proxy_url "${all_proxy:-${ALL_PROXY:-}}")}"

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
        PROXY_ENV+=(--env "no_proxy=127.0.0.1,localhost,${HOST_PROXY_NAME}" --env "NO_PROXY=127.0.0.1,localhost,${HOST_PROXY_NAME}")
    fi
}

require_project_assets() {
    require_file "${PROJECT_ROOT}/InitService/target/mitedtsm-init-service.jar"
    require_file "${PROJECT_ROOT}/Server/mitedtsm-server/target/mitedtsm-server.jar"
    require_file "${SCRIPT_DIR}/init/init-mysql.sh"
    require_dir "${PROJECT_ROOT}/database"
    require_web_assets
    require_dir "${PROJECT_ROOT}/MallFrontend/unpackage/dist/build/web"
    require_file "${PROJECT_ROOT}/MallFrontend/unpackage/dist/build/web/index.html"
}

require_web_assets() {
    require_file "${SCRIPT_DIR}/Containerfile"
    require_dir "${PROJECT_ROOT}/Web/dist-prod"
    require_file "${PROJECT_ROOT}/Web/dist-prod/index.html"
    verify_web_entry_assets "${PROJECT_ROOT}/Web/dist-prod"
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
        "$(image_archive_path eclipse-temurin-17-jdk.tar)"
        "$(image_archive_path mysql-8.0.tar)"
        "$(image_archive_path redis-6-alpine.tar)"
        "$(image_archive_path rabbitmq-3-management-alpine.tar)"
        "$(image_archive_path tdengine-3.3.6.0.tar)"
        "$(image_archive_path nginx-stable-alpine.tar)"
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

rebuild_web_only() {
    podman_cmd pod inspect "$POD_NAME" >/dev/null 2>&1 || {
        printf 'Pod does not exist: %s. Run bash ./up.sh first.\n' "$POD_NAME" >&2
        return 1
    }
    [[ "$(podman_cmd pod inspect --format '{{.State}}' "$POD_NAME")" == "Running" ]] || {
        printf 'Pod is not running: %s. Run bash ./up.sh --fast or bash ./up.sh.\n' "$POD_NAME" >&2
        return 1
    }

    ensure_image "$NGINX_BASE_IMAGE" "$(image_archive_path nginx-stable-alpine.tar)"
    build_web_image
    start_web_frontend
    wait_for 'Web frontend' 30 host_curl --fail --silent --show-error "http://127.0.0.1:${WEB_PORT}/"
}

validate_configuration
verify_rootless_podman
configure_proxy

if [[ "$START_MODE" == "full" || "$START_MODE" == "check" ]]; then
    require_project_assets
    check_archive_mode_prerequisites
elif [[ "$START_MODE" == "no-build" ]]; then
    require_runtime_images
elif [[ "$START_MODE" == "rebuild-web" ]]; then
    require_web_assets
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
        printf 'Pod does not exist: %s. Run bash ./up.sh first.\n' "$POD_NAME" >&2
        exit 1
    }
    [[ "$(podman_cmd pod inspect --format '{{.State}}' "$POD_NAME")" == "Running" ]] || {
        printf 'Pod is not running: %s. Run bash ./up.sh to recreate it.\n' "$POD_NAME" >&2
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

if [[ "$START_MODE" == "rebuild-web" ]]; then
    rebuild_web_only
    show_access_urls
    exit 0
fi

if [[ "$START_MODE" == "full" ]]; then
    ensure_image "$RUNTIME_BASE_IMAGE" "$(image_archive_path eclipse-temurin-17-jdk.tar)"
    ensure_image "$MYSQL_BASE_IMAGE" "$(image_archive_path mysql-8.0.tar)"
    ensure_image "$REDIS_BASE_IMAGE" "$(image_archive_path redis-6-alpine.tar)"
    ensure_image "$RABBITMQ_BASE_IMAGE" "$(image_archive_path rabbitmq-3-management-alpine.tar)"
    ensure_image "$TDENGINE_BASE_IMAGE" "$(image_archive_path tdengine-3.3.6.0.tar)"
    ensure_image "$NGINX_BASE_IMAGE" "$(image_archive_path nginx-stable-alpine.tar)"

    build_runtime_images
fi

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

printf 'Creating rootless Podman Pod %s.\n' "$POD_NAME"
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
    "$REDIS_BASE_IMAGE"

podman_cmd run -d --replace --name "$RABBITMQ_CONTAINER" --pod "$POD_NAME" --pull=never \
    --volume "${RABBITMQ_VOLUME}:/var/lib/rabbitmq" \
    --env RABBITMQ_DEFAULT_USER=rabbit \
    --env RABBITMQ_DEFAULT_PASS=rabbit \
    "$RABBITMQ_BASE_IMAGE"

podman_cmd run -d --replace --name "$TDENGINE_CONTAINER" --pod "$POD_NAME" --pull=never \
    --volume "${TDENGINE_VOLUME}:/var/lib/taos" \
    --env TAOS_FQDN=localhost \
    "$TDENGINE_BASE_IMAGE"

wait_for 'MySQL' 90 podman_cmd exec "$MYSQL_CONTAINER" mysqladmin ping -h 127.0.0.1 --silent &
mysql_ready_pid=$!
wait_for 'MySQL schema initialization' 180 podman_cmd exec "$MYSQL_CONTAINER" \
    mysql -uroot -p1234 --database=mitedtsm_database -Nse 'SELECT 1 FROM system_users LIMIT 1' &
mysql_schema_ready_pid=$!
wait_for 'Redis' 60 podman_cmd exec "$REDIS_CONTAINER" redis-cli ping &
redis_ready_pid=$!
# Run the probe as RabbitMQ's own user. Running this Erlang client as root
# during the broker's first few seconds creates a root-owned cookie and makes
# the broker's later privilege drop fail.
wait_for 'RabbitMQ' 90 podman_cmd exec --user rabbitmq "$RABBITMQ_CONTAINER" /opt/rabbitmq/sbin/rabbitmq-diagnostics -q ping &
rabbitmq_ready_pid=$!
wait_for 'TDengine' 120 podman_cmd exec "$TDENGINE_CONTAINER" taos -s 'SHOW DATABASES;' &
tdengine_ready_pid=$!

# The InitService only talks to TDengine, so run it while MySQL, Redis, and
# RabbitMQ finish their own health checks instead of serializing those waits.
wait "$tdengine_ready_pid"
initialize_tdengine &
tdengine_init_pid=$!

wait "$mysql_ready_pid"
wait "$mysql_schema_ready_pid"
wait "$redis_ready_pid"
wait "$rabbitmq_ready_pid"
wait "$tdengine_init_pid"

printf 'Starting server and frontends.\n'
podman_cmd run -d --replace --name "$SERVER_CONTAINER" --pod "$POD_NAME" --pull=never \
    "${PROXY_ENV[@]}" \
    --env SPRING_PROFILES_ACTIVE=local \
    "$SERVER_IMAGE"
start_frontends

# Nginx can serve its static files before the backend finishes its Spring Boot
# initialization. Probe both paths concurrently to shorten the critical path.
wait_for 'Spring Boot server' 180 host_curl --fail --silent --show-error "http://127.0.0.1:${SERVER_PORT}/actuator/health" &
server_ready_pid=$!
wait_for_frontends &
frontends_ready_pid=$!
wait "$server_ready_pid"
wait "$frontends_ready_pid"
show_access_urls
