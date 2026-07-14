#!/usr/bin/env bash

set -Eeuo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd -- "${SCRIPT_DIR}/.." && pwd)"

if [[ $# -ne 1 ]]; then
    printf 'Usage: bash ./build-mall-h5-in-ubuntu.sh <config.yaml>\n' >&2
    exit 2
fi

# shellcheck source=lib/yaml-config.sh
source "${SCRIPT_DIR}/lib/yaml-config.sh"
yaml_config_init "$1"

[[ "$(yaml_require schema_version)" == "1" ]] || {
    printf 'Unsupported schema_version; expected 1.\n' >&2
    exit 2
}

BASE_IMAGE="$(yaml_require image.base)"
BUILD_IMAGE="$(yaml_require image.name)"
REBUILD_IMAGE="$(yaml_bool image.rebuild)"
HBUILDERX_SOURCE_DIR="$(yaml_path hbuilderx.source_dir)"
PLATFORM="$(yaml_require build.platform)"
CLEAN_OUTPUT="$(yaml_bool build.clean_output)"
NETWORK_MODE="$(yaml_require network.mode)"
MEMORY="$(yaml_require runtime.memory)"
CPUS="$(yaml_positive_integer runtime.cpus)"

case "$PLATFORM" in
    h5) ;;
    *)
        printf 'build.platform must be h5; got: %s\n' "$PLATFORM" >&2
        exit 2
        ;;
esac
[[ "$NETWORK_MODE" == "none" ]] || {
    printf 'network.mode must be none; got: %s\n' "$NETWORK_MODE" >&2
    exit 2
}

command -v podman >/dev/null 2>&1 || {
    printf 'Podman is required.\n' >&2
    exit 1
}
[[ "$(podman info --format '{{.Host.Security.Rootless}}')" == "true" ]] || {
    printf 'Run this script as the rootless Podman user.\n' >&2
    exit 1
}

podman_proxy_args=(--http-proxy=false)

if [[ "$REBUILD_IMAGE" == "true" ]] || ! podman image exists "$BUILD_IMAGE"; then
    [[ -x "$HBUILDERX_SOURCE_DIR/plugins/node/node" \
        && -f "$HBUILDERX_SOURCE_DIR/plugins/uniapp-cli-vite/node_modules/@dcloudio/vite-plugin-uni/bin/uni.js" \
        && -f "$HBUILDERX_SOURCE_DIR/plugins/weapp-miniprogram-ci/node_modules/sass/package.json" ]] || {
        printf 'Configured HBuilderX source directory is invalid: %s\n' \
            "$HBUILDERX_SOURCE_DIR" >&2
        exit 1
    }
    podman image exists "$BASE_IMAGE" || {
        printf 'Configured Ubuntu base image is not local: %s\n' "$BASE_IMAGE" >&2
        exit 1
    }
    printf 'Building self-contained HBuilderX image: %s\n' "$BUILD_IMAGE"
    podman build "${podman_proxy_args[@]}" --network=none --pull=never \
        --build-arg "UBUNTU_BASE_IMAGE=$BASE_IMAGE" \
        --tag "$BUILD_IMAGE" \
        --file "$SCRIPT_DIR/Containerfile.hbuilderx-ubuntu" \
        --ignorefile "$SCRIPT_DIR/hbuilderx.containerignore" \
        "$HBUILDERX_SOURCE_DIR"
fi

printf 'Building Mall H5 inside %s.\n' "$BUILD_IMAGE"
podman run "${podman_proxy_args[@]}" --rm --pull=never \
    --network=none \
    --memory "$MEMORY" \
    --cpus "$CPUS" \
    --volume "$PROJECT_ROOT:/workspace:rw" \
    --env "HBUILDERX_PROJECT_DIR=/workspace/MallFrontend" \
    --env "HBUILDERX_PLATFORM=$PLATFORM" \
    --env "HBUILDERX_CLEAN_OUTPUT=$CLEAN_OUTPUT" \
    --entrypoint /workspace/podman/hbuilderx-build-entrypoint.sh \
    "$BUILD_IMAGE"
