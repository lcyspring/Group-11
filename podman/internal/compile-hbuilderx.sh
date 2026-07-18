#!/usr/bin/env bash

set -Eeuo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
PROJECT_ROOT="$(cd -- "${SCRIPT_DIR}/.." && pwd)"

if [[ $# -ne 1 ]]; then
    printf 'Usage: bash ./compile.sh <config.yaml>\n' >&2
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
DEPENDENCY_IMAGE="$(yaml_require image.dependency)"
REBUILD_IMAGE="$(yaml_bool image.rebuild)"
HBUILDERX_SOURCE_DIR="$(yaml_path hbuilderx.source_dir)"
PLATFORM="$(yaml_require build.platform)"
CLEAN_OUTPUT="$(yaml_bool build.clean_output)"
LEGACY_MEDIA_ORIGINS="$(yaml_require media.legacy_origins)"
LEGACY_MEDIA_FALLBACK="$(yaml_require media.legacy_fallback)"
NETWORK_MODE="$(yaml_require network.mode)"
DEPENDENCY_NETWORK_MODE="$(yaml_require network.dependency_mode)"
USE_HOST_PROXY="$(yaml_bool network.use_host_proxy)"
PNPM_STORE_VOLUME="$(yaml_require cache.pnpm_store_volume)"
PNPM_STORE_PATH="$(yaml_require cache.pnpm_store_path)"
NODE_MODULES_VOLUME="$(yaml_require cache.node_modules_volume)"
FROZEN_LOCKFILE="$(yaml_bool dependency.frozen_lockfile)"
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
case "$DEPENDENCY_NETWORK_MODE" in
    pasta|slirp4netns|bridge) ;;
    *)
        printf 'network.dependency_mode must be pasta, slirp4netns or bridge; got: %s\n' \
            "$DEPENDENCY_NETWORK_MODE" >&2
        exit 2
        ;;
esac
[[ "$PNPM_STORE_PATH" == /* && "$PNPM_STORE_PATH" != /workspace* ]] || {
    printf 'cache.pnpm_store_path must be an absolute path outside /workspace.\n' >&2
    exit 2
}
[[ "$LEGACY_MEDIA_ORIGINS" =~ ^https?://[^[:space:]]+(,https?://[^[:space:]]+)*$ ]] || {
    printf 'media.legacy_origins must be a comma-separated list of HTTP(S) origins.\n' >&2
    exit 2
}
[[ "$LEGACY_MEDIA_FALLBACK" == /static/* && "$LEGACY_MEDIA_FALLBACK" != *'..'* ]] || {
    printf 'media.legacy_fallback must be a safe absolute /static/ path.\n' >&2
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
if [[ "$USE_HOST_PROXY" == "true" ]]; then
    podman_proxy_args=()
fi

if [[ "$REBUILD_IMAGE" == "true" ]]; then
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
elif ! podman image exists "$BUILD_IMAGE"; then
    printf 'Pulling published HBuilderX toolchain image: %s\n' "$BUILD_IMAGE"
    if [[ "$USE_HOST_PROXY" == "true" ]]; then
        podman pull "$BUILD_IMAGE"
    else
        env -u http_proxy -u HTTP_PROXY -u https_proxy -u HTTPS_PROXY \
            -u all_proxy -u ALL_PROXY -u no_proxy -u NO_PROXY \
            podman pull "$BUILD_IMAGE"
    fi
fi

if ! podman image exists "$DEPENDENCY_IMAGE"; then
    printf 'Pulling published Ubuntu dependency image: %s\n' "$DEPENDENCY_IMAGE"
    if [[ "$USE_HOST_PROXY" == "true" ]]; then
        podman pull "$DEPENDENCY_IMAGE"
    else
        env -u http_proxy -u HTTP_PROXY -u https_proxy -u HTTPS_PROXY \
            -u all_proxy -u ALL_PROXY -u no_proxy -u NO_PROXY \
            podman pull "$DEPENDENCY_IMAGE"
    fi
fi
podman volume inspect "$PNPM_STORE_VOLUME" >/dev/null 2>&1 \
    || podman volume create "$PNPM_STORE_VOLUME" >/dev/null
podman volume inspect "$NODE_MODULES_VOLUME" >/dev/null 2>&1 \
    || podman volume create "$NODE_MODULES_VOLUME" >/dev/null

proxy_args=()
if [[ "$USE_HOST_PROXY" == "true" ]]; then
    for proxy_name in http_proxy HTTP_PROXY https_proxy HTTPS_PROXY all_proxy ALL_PROXY no_proxy NO_PROXY; do
        [[ -n "${!proxy_name:-}" ]] && proxy_args+=(--env "$proxy_name=${!proxy_name}")
    done
fi

printf 'Installing Mall dependencies at container runtime in %s.\n' "$DEPENDENCY_IMAGE"
podman run "${podman_proxy_args[@]}" --rm --pull=never \
    --network "$DEPENDENCY_NETWORK_MODE" \
    --memory "$MEMORY" \
    --cpus "$CPUS" \
    --volume "$PROJECT_ROOT:/workspace:rw" \
    --volume "$PNPM_STORE_VOLUME:$PNPM_STORE_PATH:rw" \
    --volume "$NODE_MODULES_VOLUME:/workspace/MallFrontend/node_modules:rw" \
    --env "PNPM_STORE_PATH=$PNPM_STORE_PATH" \
    --env "PNPM_FROZEN_LOCKFILE=$FROZEN_LOCKFILE" \
    --env "BUILD_USE_HOST_PROXY=$USE_HOST_PROXY" \
    "${proxy_args[@]}" \
    --entrypoint /workspace/podman/mall-dependencies-entrypoint.sh \
    "$DEPENDENCY_IMAGE"

printf 'Building Mall H5 inside %s.\n' "$BUILD_IMAGE"
podman run "${podman_proxy_args[@]}" --rm --pull=never \
    --network=none \
    --memory "$MEMORY" \
    --cpus "$CPUS" \
    --volume "$PROJECT_ROOT:/workspace:rw" \
    --volume "$NODE_MODULES_VOLUME:/workspace/MallFrontend/node_modules:rw" \
    --env "HBUILDERX_PROJECT_DIR=/workspace/MallFrontend" \
    --env "HBUILDERX_PLATFORM=$PLATFORM" \
    --env "HBUILDERX_CLEAN_OUTPUT=$CLEAN_OUTPUT" \
    --env "SHOPRO_LEGACY_MEDIA_ORIGINS=$LEGACY_MEDIA_ORIGINS" \
    --env "SHOPRO_LEGACY_MEDIA_FALLBACK=$LEGACY_MEDIA_FALLBACK" \
    --entrypoint /workspace/podman/hbuilderx-build-entrypoint.sh \
    "$BUILD_IMAGE"
