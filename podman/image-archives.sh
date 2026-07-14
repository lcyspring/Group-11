#!/usr/bin/env bash
# Create OCI image archives with Podman for an offline Podman deployment.

set -Eeuo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"

usage() {
    printf 'Usage: bash ./image-archives.sh <config.yaml>\n' >&2
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

ARCHIVE_MODE="$(yaml_require operation.archive_mode)"
IMAGE_ARCHIVE_DIR="$(yaml_path image.archive_dir)"
USE_HOST_PROXY="$(yaml_bool network.use_host_proxy)"
HTTP_PROXY_URL="$(yaml_require network.http_proxy)"
HTTPS_PROXY_URL="$(yaml_require network.https_proxy)"
ALL_PROXY_URL="$(yaml_require network.all_proxy)"
NO_PROXY_VALUE="$(yaml_require network.no_proxy)"

clear_host_proxy() {
    unset http_proxy HTTP_PROXY https_proxy HTTPS_PROXY \
        all_proxy ALL_PROXY no_proxy NO_PROXY || true
}

clear_host_proxy
if [[ "$USE_HOST_PROXY" == "true" ]]; then
    if [[ "$HTTP_PROXY_URL" == "none" && "$HTTPS_PROXY_URL" == "none" && "$ALL_PROXY_URL" == "none" ]]; then
        printf 'At least one explicit proxy URL is required when network.use_host_proxy=true.\n' >&2
        exit 2
    fi
    [[ "$HTTP_PROXY_URL" == "none" ]] || export http_proxy="$HTTP_PROXY_URL" HTTP_PROXY="$HTTP_PROXY_URL"
    [[ "$HTTPS_PROXY_URL" == "none" ]] || export https_proxy="$HTTPS_PROXY_URL" HTTPS_PROXY="$HTTPS_PROXY_URL"
    [[ "$ALL_PROXY_URL" == "none" ]] || export all_proxy="$ALL_PROXY_URL" ALL_PROXY="$ALL_PROXY_URL"
    export no_proxy="$NO_PROXY_VALUE" NO_PROXY="$NO_PROXY_VALUE"
fi

archives=(
    "${IMAGE_ARCHIVE_DIR}/$(yaml_require archive.runtime_base)"
    "${IMAGE_ARCHIVE_DIR}/$(yaml_require archive.mysql_base)"
    "${IMAGE_ARCHIVE_DIR}/$(yaml_require archive.redis_base)"
    "${IMAGE_ARCHIVE_DIR}/$(yaml_require archive.rabbitmq_base)"
    "${IMAGE_ARCHIVE_DIR}/$(yaml_require archive.tdengine_base)"
    "${IMAGE_ARCHIVE_DIR}/$(yaml_require archive.nginx_base)"
)
images=(
    "$(yaml_require image.runtime_base)"
    "$(yaml_require image.mysql_base)"
    "$(yaml_require image.redis_base)"
    "$(yaml_require image.rabbitmq_base)"
    "$(yaml_require image.tdengine_base)"
    "$(yaml_require image.nginx_base)"
)

case "$ARCHIVE_MODE" in
    check) ;;
    save|pull-save) ;;
    *)
        printf 'operation.archive_mode must be check, save, or pull-save; got: %s\n' "$ARCHIVE_MODE" >&2
        exit 2
        ;;
esac

if [[ "$ARCHIVE_MODE" == "check" ]]; then
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

    if [[ "$ARCHIVE_MODE" == "pull-save" ]]; then
        printf 'Pulling %s with Podman.\n' "$image"
        podman pull "$image"
    elif ! podman image exists "$image"; then
        printf 'Base image is unavailable locally: %s\n' "$image" >&2
        printf 'Use operation.archive_mode=pull-save to fetch it with Podman.\n' >&2
        exit 1
    fi

    temporary_archive="${archive}.tmp.$$"
    rm -f "$temporary_archive"
    printf 'Writing OCI archive %s\n' "$archive"
    podman save --format oci-archive --output "$temporary_archive" "$image"
    mv -f "$temporary_archive" "$archive"
done

printf 'Podman OCI image archives are ready in %s.\n' "$IMAGE_ARCHIVE_DIR"
