#!/usr/bin/env bash

# Package already compiled artifacts into project runtime images. This stage
# never invokes Maven, pnpm, HBuilderX, or deploy.sh.

set -Eeuo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd -- "${SCRIPT_DIR}/.." && pwd)"

[[ $# -eq 1 ]] || {
    printf 'Usage: bash ./build-images.sh <config.yaml>\n' >&2
    exit 2
}

# shellcheck source=lib/yaml-config.sh
source "${SCRIPT_DIR}/lib/yaml-config.sh"
yaml_config_init "$1"
[[ "$(yaml_require schema_version)" == "1" ]] || exit 2

MODE="$(yaml_require operation.mode)"
TARGETS_VALUE="$(yaml_require build.targets)"
CONTAINERFILE="$(yaml_path build.containerfile)"
USE_HOST_PROXY="$(yaml_bool network.use_host_proxy)"
HTTP_PROXY_URL="$(yaml_require network.http_proxy)"
HTTPS_PROXY_URL="$(yaml_require network.https_proxy)"
ALL_PROXY_URL="$(yaml_require network.all_proxy)"
NO_PROXY_VALUE="$(yaml_require network.no_proxy)"
IMAGE_SOURCE="$(yaml_require image.source)"
IMAGE_ARCHIVE_DIR="$(yaml_path image.archive_dir)"
RUNTIME_BASE_IMAGE="$(yaml_require image.runtime_base)"
NGINX_BASE_IMAGE="$(yaml_require image.nginx_base)"
INIT_IMAGE="$(yaml_require image.init_runtime)"
SERVER_IMAGE="$(yaml_require image.server_runtime)"
WEB_IMAGE="$(yaml_require image.web_runtime)"
MALL_IMAGE="$(yaml_require image.mall_runtime)"
RUNTIME_ARCHIVE="$(yaml_require archive.runtime_base)"
NGINX_ARCHIVE="$(yaml_require archive.nginx_base)"

case "$MODE" in
    check|package) ;;
    *) printf 'operation.mode must be check or package; got: %s\n' "$MODE" >&2; exit 2 ;;
esac
case "$IMAGE_SOURCE" in
    auto|archive|pull) ;;
    *) printf 'image.source must be auto, archive, or pull; got: %s\n' "$IMAGE_SOURCE" >&2; exit 2 ;;
esac

if [[ "$TARGETS_VALUE" == "all" ]]; then
    TARGETS_VALUE="init-service,server,web,mall"
fi
[[ "$TARGETS_VALUE" =~ ^(init-service|server|web|mall)(,(init-service|server|web|mall))*$ ]] || {
    printf 'build.targets must be all or a comma-separated subset of init-service,server,web,mall.\n' >&2
    exit 2
}
IFS=',' read -r -a TARGETS <<< "$TARGETS_VALUE"

require_file() {
    [[ -s "$1" ]] || { printf 'Required file is missing or empty: %s\n' "$1" >&2; exit 1; }
}

verify_web_entry_assets() {
    local output="$1" entry="${1}/index.html" asset found=false
    require_file "$entry"
    while IFS= read -r asset; do
        [[ -n "$asset" ]] || continue
        found=true
        require_file "${output}${asset}"
    done < <(sed -nE 's/.*(src|href)="(\/assets\/[^"?]+)(\?[^" ]*)?".*/\2/p' "$entry" | sort -u)
    [[ "$found" == "true" ]] || {
        printf 'Web entry does not reference any hashed assets: %s\n' "$entry" >&2
        exit 1
    }
}

target_selected() {
    local wanted="$1" target
    for target in "${TARGETS[@]}"; do
        [[ "$target" == "$wanted" ]] && return 0
    done
    return 1
}

require_file "$CONTAINERFILE"
target_selected init-service && require_file "${PROJECT_ROOT}/InitService/target/mitedtsm-init-service.jar"
target_selected server && require_file "${PROJECT_ROOT}/Server/mitedtsm-server/target/mitedtsm-server.jar"
target_selected web && verify_web_entry_assets "${PROJECT_ROOT}/Web/dist-prod"
target_selected mall && verify_web_entry_assets "${PROJECT_ROOT}/MallFrontend/unpackage/dist/build/web"

command -v podman >/dev/null 2>&1 || { printf 'Podman is required.\n' >&2; exit 1; }
[[ "$(podman info --format '{{.Host.Security.Rootless}}')" == "true" ]] || {
    printf 'Run this script as the normal rootless Podman user.\n' >&2
    exit 1
}

image_archive_path() {
    printf '%s/%s' "$IMAGE_ARCHIVE_DIR" "$1"
}

if [[ "$MODE" == "check" ]]; then
    if [[ "$IMAGE_SOURCE" == "archive" ]]; then
        podman image exists "$RUNTIME_BASE_IMAGE" || require_file "$(image_archive_path "$RUNTIME_ARCHIVE")"
        podman image exists "$NGINX_BASE_IMAGE" || require_file "$(image_archive_path "$NGINX_ARCHIVE")"
    fi
    printf 'Runtime image packaging preflight passed. No image was loaded, pulled, or built.\n'
    exit 0
fi

PODMAN_PROXY_ARGS=(--http-proxy=false)
if [[ "$USE_HOST_PROXY" == "true" ]]; then
    PODMAN_PROXY_ARGS=()
    [[ "$HTTP_PROXY_URL" != "none" || "$HTTPS_PROXY_URL" != "none" || "$ALL_PROXY_URL" != "none" ]] || {
        printf 'At least one explicit proxy URL is required when network.use_host_proxy=true.\n' >&2
        exit 2
    }
    [[ "$HTTP_PROXY_URL" == "none" ]] || export http_proxy="$HTTP_PROXY_URL" HTTP_PROXY="$HTTP_PROXY_URL"
    [[ "$HTTPS_PROXY_URL" == "none" ]] || export https_proxy="$HTTPS_PROXY_URL" HTTPS_PROXY="$HTTPS_PROXY_URL"
    [[ "$ALL_PROXY_URL" == "none" ]] || export all_proxy="$ALL_PROXY_URL" ALL_PROXY="$ALL_PROXY_URL"
    export no_proxy="$NO_PROXY_VALUE" NO_PROXY="$NO_PROXY_VALUE"
else
    unset http_proxy HTTP_PROXY https_proxy HTTPS_PROXY all_proxy ALL_PROXY no_proxy NO_PROXY || true
fi

ensure_base_image() {
    local image="$1" archive="$2"
    if [[ "$IMAGE_SOURCE" != "pull" ]] && podman image exists "$image"; then
        return
    fi
    case "$IMAGE_SOURCE" in
        auto)
            if [[ -r "$archive" ]]; then
                podman load --input "$archive"
            else
                podman pull "$image"
            fi
            ;;
        archive)
            require_file "$archive"
            podman load --input "$archive"
            ;;
        pull) podman pull "$image" ;;
    esac
    podman image exists "$image" || { printf 'Base image is unavailable: %s\n' "$image" >&2; exit 1; }
}

if target_selected init-service || target_selected server; then
    ensure_base_image "$RUNTIME_BASE_IMAGE" "$(image_archive_path "$RUNTIME_ARCHIVE")"
fi
if target_selected web || target_selected mall; then
    ensure_base_image "$NGINX_BASE_IMAGE" "$(image_archive_path "$NGINX_ARCHIVE")"
fi

build_args=(
    --pull=never
    "${PODMAN_PROXY_ARGS[@]}"
    --build-arg "RUNTIME_BASE_IMAGE=${RUNTIME_BASE_IMAGE}"
    --build-arg "NGINX_BASE_IMAGE=${NGINX_BASE_IMAGE}"
    --file "$CONTAINERFILE"
)

for target in "${TARGETS[@]}"; do
    case "$target" in
        init-service) image="$INIT_IMAGE" ;;
        server) image="$SERVER_IMAGE" ;;
        web) image="$WEB_IMAGE" ;;
        mall) image="$MALL_IMAGE" ;;
    esac
    printf 'Packaging target %s as %s.\n' "$target" "$image"
    podman build "${build_args[@]}" --target "$target" --tag "$image" "$PROJECT_ROOT"
done

printf 'Runtime image packaging completed. deploy.sh was not invoked and no container was replaced.\n'
