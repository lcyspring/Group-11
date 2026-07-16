#!/usr/bin/env bash
# Create OCI image archives with Podman for an offline Podman deployment.

set -Eeuo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
IMAGE_ARCHIVE_DIR="${IMAGE_ARCHIVE_DIR:-${SCRIPT_DIR}/images}"
USE_HOST_PROXY="${USE_HOST_PROXY:-false}"
CHECK_ONLY=false
PULL_IMAGES=false

# Keep these defaults in sync with up.sh. They are fully qualified so no
# short-name registry configuration is needed.
RUNTIME_BASE_IMAGE="${RUNTIME_BASE_IMAGE:-docker.io/library/eclipse-temurin:17-jdk}"
MYSQL_BASE_IMAGE="${MYSQL_BASE_IMAGE:-docker.io/library/mysql:8.0}"
REDIS_BASE_IMAGE="${REDIS_BASE_IMAGE:-docker.io/library/redis:6-alpine}"
RABBITMQ_BASE_IMAGE="${RABBITMQ_BASE_IMAGE:-docker.io/library/rabbitmq:3-management-alpine}"
TDENGINE_BASE_IMAGE="${TDENGINE_BASE_IMAGE:-docker.io/tdengine/tdengine:3.3.6.0}"
NGINX_BASE_IMAGE="${NGINX_BASE_IMAGE:-docker.io/library/nginx:stable-alpine}"

usage() {
    cat <<'EOF'
Usage: bash ./image-archives.sh [--check|--pull]

Writes one OCI archive per base image into podman/images/ by default. These
archives are consumed by IMAGE_SOURCE=archive bash ./up.sh and are generated
entirely with Podman.

Options:
  --check  Verify that every required archive already exists; do not change it.
  --pull   Pull every base image with Podman before writing its archive.

Optional environment variables:
  IMAGE_ARCHIVE_DIR=/absolute/path/to/oci-archives
  USE_HOST_PROXY=true
EOF
}

case "$#" in
    0) ;;
    1)
        case "$1" in
            --check)
                CHECK_ONLY=true
                ;;
            --pull)
                PULL_IMAGES=true
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

case "$USE_HOST_PROXY" in
    true|TRUE|1|yes|YES)
        USE_HOST_PROXY=true
        ;;
    false|FALSE|0|no|NO|'')
        USE_HOST_PROXY=false
        ;;
    *)
        printf 'USE_HOST_PROXY must be true or false; got: %s\n' "$USE_HOST_PROXY" >&2
        exit 2
        ;;
esac

clear_host_proxy() {
    unset http_proxy HTTP_PROXY https_proxy HTTPS_PROXY \
        all_proxy ALL_PROXY no_proxy NO_PROXY || true
}

if [[ "$USE_HOST_PROXY" == false ]]; then
    clear_host_proxy
fi

archives=(
    "${IMAGE_ARCHIVE_DIR}/eclipse-temurin-17-jdk.tar"
    "${IMAGE_ARCHIVE_DIR}/mysql-8.0.tar"
    "${IMAGE_ARCHIVE_DIR}/redis-6-alpine.tar"
    "${IMAGE_ARCHIVE_DIR}/rabbitmq-3-management-alpine.tar"
    "${IMAGE_ARCHIVE_DIR}/tdengine-3.3.6.0.tar"
    "${IMAGE_ARCHIVE_DIR}/nginx-stable-alpine.tar"
)
images=(
    "$RUNTIME_BASE_IMAGE"
    "$MYSQL_BASE_IMAGE"
    "$REDIS_BASE_IMAGE"
    "$RABBITMQ_BASE_IMAGE"
    "$TDENGINE_BASE_IMAGE"
    "$NGINX_BASE_IMAGE"
)

if [[ "$CHECK_ONLY" == true ]]; then
    missing=()
    for archive in "${archives[@]}"; do
        [[ -s "$archive" ]] || missing+=("$archive")
    done
    if ((${#missing[@]})); then
        printf 'Missing Podman OCI image archives:\n' >&2
        printf '  - %s\n' "${missing[@]}" >&2
        exit 1
    fi
    printf 'All Podman OCI image archives are available in %s.\n' "$IMAGE_ARCHIVE_DIR"
    exit 0
fi

command -v podman >/dev/null 2>&1 || {
    printf 'Podman is required.\n' >&2
    exit 1
}

rootless="$(podman info --format '{{.Host.Security.Rootless}}')" || {
    printf 'Podman is installed but not usable by this user. Run podman info for details.\n' >&2
    exit 1
}
[[ "$rootless" == true ]] || {
    printf 'Run this script as the normal rootless Podman user.\n' >&2
    exit 1
}

mkdir -p "$IMAGE_ARCHIVE_DIR"
for ((index = 0; index < ${#images[@]}; index++)); do
    image="${images[index]}"
    archive="${archives[index]}"

    if [[ "$PULL_IMAGES" == true ]]; then
        printf 'Pulling %s with Podman.\n' "$image"
        podman pull "$image"
    elif ! podman image exists "$image"; then
        printf 'Base image is unavailable locally: %s\n' "$image" >&2
        printf 'Run bash ./image-archives.sh --pull to fetch it with Podman.\n' >&2
        exit 1
    fi

    temporary_archive="${archive}.tmp.$$"
    rm -f "$temporary_archive"
    printf 'Writing OCI archive %s\n' "$archive"
    podman save --format oci-archive --output "$temporary_archive" "$image"
    mv -f "$temporary_archive" "$archive"
done

printf 'Podman OCI image archives are ready in %s.\n' "$IMAGE_ARCHIVE_DIR"
