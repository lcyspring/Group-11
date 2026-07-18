#!/usr/bin/env bash

# Unified compilation entry point. KDL include/exclude sets select Server,
# InitService, Web, and Mall H5 targets; exclusions always win. The command
# never selects work from file names, Host tools, or environment variables.

set -Eeuo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd -- "${SCRIPT_DIR}/.." && pwd)"

[[ $# -eq 1 ]] || {
    printf 'Usage: bash ./compile.sh <config.kdl>\n' >&2
    exit 2
}

# shellcheck source=lib/kdl-config.sh
source "${SCRIPT_DIR}/lib/kdl-config.sh"
kdl_config_init "$1"

[[ "$(kdl_require schema_version)" == "1" ]] || {
    printf 'Unsupported schema_version; expected 1.\n' >&2
    exit 2
}

INCLUDE_TARGETS="$(kdl_require build.include_targets)"
EXCLUDE_TARGETS="$(kdl_require build.exclude_targets)"

validate_selector() {
    local key="$1" value="$2" item
    case "$value" in
        all|none) return ;;
    esac
    [[ "$value" =~ ^(server|init-service|web|mall-h5)(,(server|init-service|web|mall-h5))*$ ]] || {
        printf '%s must be all, none, or a comma-separated subset of server,init-service,web,mall-h5; got: %s\n' \
            "$key" "$value" >&2
        exit 2
    }
    declare -A seen=()
    IFS=',' read -r -a items <<< "$value"
    for item in "${items[@]}"; do
        [[ -z "${seen[$item]:-}" ]] || {
            printf '%s contains a duplicate target: %s\n' "$key" "$item" >&2
            exit 2
        }
        seen[$item]=true
    done
}

selector_contains() {
    local selector="$1" target="$2"
    [[ "$selector" == all ]] && return 0
    [[ "$selector" == none ]] && return 1
    [[ ",${selector}," == *",${target},"* ]]
}

target_selected() {
    selector_contains "$INCLUDE_TARGETS" "$1" && ! selector_contains "$EXCLUDE_TARGETS" "$1"
}

optional_bool() {
    local key="$1" fallback="$2" value
    value="$(kdl_get "$key")"
    value="${value:-$fallback}"
    case "$value" in
        true|false) printf '%s' "$value" ;;
        *) printf '%s must be true or false; got: %s\n' "$key" "$value" >&2; exit 2 ;;
    esac
}

validate_selector build.include_targets "$INCLUDE_TARGETS"
validate_selector build.exclude_targets "$EXCLUDE_TARGETS"

BUILD_SERVER=false
BUILD_INIT_SERVICE=false
BUILD_WEB=false
BUILD_MALL_H5=false
target_selected server && BUILD_SERVER=true
target_selected init-service && BUILD_INIT_SERVICE=true
target_selected web && BUILD_WEB=true
target_selected mall-h5 && BUILD_MALL_H5=true

STANDARD_REQUIRED=false
if [[ "$BUILD_SERVER" == true || "$BUILD_INIT_SERVICE" == true || "$BUILD_WEB" == true ]]; then
    STANDARD_REQUIRED=true
fi
for task_key in \
    crm_tests crm_coverage erp_tests erp_coverage infra_tests infra_coverage \
    bpm_tests bpm_coverage pay_tests pay_coverage common_tests common_coverage \
    framework_tests framework_coverage system_tests system_coverage; do
    [[ "$(optional_bool "build.${task_key}" false)" == true ]] && STANDARD_REQUIRED=true
done
[[ -n "$(kdl_get web.test_script)" ]] && STANDARD_REQUIRED=true

if [[ "$STANDARD_REQUIRED" == false && "$BUILD_MALL_H5" == false ]]; then
    printf 'Compilation selection is empty: include=%s exclude=%s and no standard test task is enabled.\n' \
        "$INCLUDE_TARGETS" "$EXCLUDE_TARGETS" >&2
    exit 2
fi

if [[ "$STANDARD_REQUIRED" == true ]]; then
    printf 'Selected standard targets: server=%s init-service=%s web=%s.\n' \
        "$BUILD_SERVER" "$BUILD_INIT_SERVICE" "$BUILD_WEB"
    COMPILE_BUILD_SERVER="$BUILD_SERVER" \
    COMPILE_BUILD_INIT_SERVICE="$BUILD_INIT_SERVICE" \
    COMPILE_BUILD_WEB="$BUILD_WEB" \
        bash "${SCRIPT_DIR}/internal/compile-standard.sh" "$KDL_CONFIG_PATH"
fi

run_mall_h5() {
    local base_image hbuilderx_image dependency_image rebuild_image hbuilderx_source_dir
    local platform clean_output legacy_media_origins legacy_media_fallback network_mode
    local dependency_network_mode use_host_proxy pnpm_store_volume pnpm_store_path
    local node_modules_volume frozen_lockfile memory cpus proxy_name

    base_image="$(kdl_require image.base)"
    hbuilderx_image="$(kdl_require image.hbuilderx)"
    dependency_image="$(kdl_require image.dependency)"
    rebuild_image="$(kdl_bool image.rebuild)"
    hbuilderx_source_dir="$(kdl_path hbuilderx.source_dir)"
    platform="$(kdl_require build.platform)"
    clean_output="$(kdl_bool build.clean_output)"
    legacy_media_origins="$(kdl_require media.legacy_origins)"
    legacy_media_fallback="$(kdl_require media.legacy_fallback)"
    network_mode="$(kdl_require network.mall_mode)"
    dependency_network_mode="$(kdl_require network.mall_dependency_mode)"
    use_host_proxy="$(kdl_bool network.use_host_proxy)"
    pnpm_store_volume="$(kdl_require cache.mall_pnpm_store_volume)"
    pnpm_store_path="$(kdl_require cache.mall_pnpm_store_path)"
    node_modules_volume="$(kdl_require cache.mall_node_modules_volume)"
    frozen_lockfile="$(kdl_bool dependency.mall_frozen_lockfile)"
    memory="$(kdl_require runtime.mall_memory)"
    cpus="$(kdl_positive_integer runtime.mall_cpus)"
    proxy_name=host.containers.internal

    [[ "$platform" == h5 ]] || {
        printf 'build.platform must be h5; got: %s\n' "$platform" >&2
        exit 2
    }
    [[ "$network_mode" == none ]] || {
        printf 'network.mall_mode must be none; got: %s\n' "$network_mode" >&2
        exit 2
    }
    case "$dependency_network_mode" in
        pasta|slirp4netns|bridge) ;;
        *)
            printf 'network.mall_dependency_mode must be pasta, slirp4netns or bridge; got: %s\n' \
                "$dependency_network_mode" >&2
            exit 2
            ;;
    esac
    [[ "$pnpm_store_path" == /* && "$pnpm_store_path" != /workspace* ]] || {
        printf 'cache.mall_pnpm_store_path must be an absolute path outside /workspace.\n' >&2
        exit 2
    }
    [[ "$legacy_media_origins" =~ ^https?://[^[:space:]]+(,https?://[^[:space:]]+)*$ ]] || {
        printf 'media.legacy_origins must be a comma-separated list of HTTP(S) origins.\n' >&2
        exit 2
    }
    [[ "$legacy_media_fallback" == /static/* && "$legacy_media_fallback" != *'..'* ]] || {
        printf 'media.legacy_fallback must be a safe absolute /static/ path.\n' >&2
        exit 2
    }

    command -v podman >/dev/null 2>&1 || { printf 'Podman is required.\n' >&2; exit 1; }
    [[ "$(podman info --format '{{.Host.Security.Rootless}}')" == true ]] || {
        printf 'Run this script as the rootless Podman user.\n' >&2
        exit 1
    }

    local -a podman_proxy_args=(--http-proxy=false)
    [[ "$use_host_proxy" == true ]] && podman_proxy_args=()

    if [[ "$rebuild_image" == true ]]; then
        [[ -x "$hbuilderx_source_dir/plugins/node/node" \
            && -f "$hbuilderx_source_dir/plugins/uniapp-cli-vite/node_modules/@dcloudio/vite-plugin-uni/bin/uni.js" \
            && -f "$hbuilderx_source_dir/plugins/weapp-miniprogram-ci/node_modules/sass/package.json" ]] || {
            printf 'Configured HBuilderX source directory is invalid: %s\n' "$hbuilderx_source_dir" >&2
            exit 1
        }
        podman image exists "$base_image" || {
            printf 'Configured Ubuntu base image is not local: %s\n' "$base_image" >&2
            exit 1
        }
        printf 'Building self-contained HBuilderX image: %s\n' "$hbuilderx_image"
        podman build "${podman_proxy_args[@]}" --network=none --pull=never \
            --build-arg "UBUNTU_BASE_IMAGE=$base_image" \
            --tag "$hbuilderx_image" \
            --file "$SCRIPT_DIR/Containerfile.hbuilderx-ubuntu" \
            --ignorefile "$SCRIPT_DIR/hbuilderx.containerignore" \
            "$hbuilderx_source_dir"
    elif ! podman image exists "$hbuilderx_image"; then
        printf 'Pulling published HBuilderX toolchain image: %s\n' "$hbuilderx_image"
        if [[ "$use_host_proxy" == true ]]; then
            podman pull "$hbuilderx_image"
        else
            env -u http_proxy -u HTTP_PROXY -u https_proxy -u HTTPS_PROXY \
                -u all_proxy -u ALL_PROXY -u no_proxy -u NO_PROXY podman pull "$hbuilderx_image"
        fi
    fi

    if ! podman image exists "$dependency_image"; then
        printf 'Pulling published Ubuntu dependency image: %s\n' "$dependency_image"
        if [[ "$use_host_proxy" == true ]]; then
            podman pull "$dependency_image"
        else
            env -u http_proxy -u HTTP_PROXY -u https_proxy -u HTTPS_PROXY \
                -u all_proxy -u ALL_PROXY -u no_proxy -u NO_PROXY podman pull "$dependency_image"
        fi
    fi
    podman volume inspect "$pnpm_store_volume" >/dev/null 2>&1 \
        || podman volume create "$pnpm_store_volume" >/dev/null
    podman volume inspect "$node_modules_volume" >/dev/null 2>&1 \
        || podman volume create "$node_modules_volume" >/dev/null

    local -a proxy_args=()
    if [[ "$use_host_proxy" == true ]]; then
        local proxy_var proxy_value
        for proxy_var in http_proxy HTTP_PROXY https_proxy HTTPS_PROXY all_proxy ALL_PROXY no_proxy NO_PROXY; do
            proxy_value="${!proxy_var:-}"
            proxy_value="${proxy_value//127.0.0.1/$proxy_name}"
            proxy_value="${proxy_value//localhost/$proxy_name}"
            [[ -n "$proxy_value" ]] && proxy_args+=(--env "$proxy_var=$proxy_value")
        done
    fi

    printf 'Installing Mall dependencies at container runtime in %s.\n' "$dependency_image"
    podman run "${podman_proxy_args[@]}" --rm --pull=never \
        --network "$dependency_network_mode" --memory "$memory" --cpus "$cpus" \
        --volume "$PROJECT_ROOT:/workspace:rw" \
        --volume "$pnpm_store_volume:$pnpm_store_path:rw" \
        --volume "$node_modules_volume:/workspace/MallFrontend/node_modules:rw" \
        --env "PNPM_STORE_PATH=$pnpm_store_path" \
        --env "PNPM_FROZEN_LOCKFILE=$frozen_lockfile" \
        --env "BUILD_USE_HOST_PROXY=$use_host_proxy" \
        "${proxy_args[@]}" \
        --entrypoint /workspace/podman/internal/mall-dependencies-entrypoint.sh \
        "$dependency_image"

    printf 'Building Mall H5 inside %s.\n' "$hbuilderx_image"
    podman run "${podman_proxy_args[@]}" --rm --pull=never \
        --network=none --memory "$memory" --cpus "$cpus" \
        --volume "$PROJECT_ROOT:/workspace:rw" \
        --volume "$node_modules_volume:/workspace/MallFrontend/node_modules:rw" \
        --env "HBUILDERX_PROJECT_DIR=/workspace/MallFrontend" \
        --env "HBUILDERX_PLATFORM=$platform" \
        --env "HBUILDERX_CLEAN_OUTPUT=$clean_output" \
        --env "SHOPRO_LEGACY_MEDIA_ORIGINS=$legacy_media_origins" \
        --env "SHOPRO_LEGACY_MEDIA_FALLBACK=$legacy_media_fallback" \
        --entrypoint /workspace/podman/internal/hbuilderx-build-entrypoint.sh \
        "$hbuilderx_image"
}

if [[ "$BUILD_MALL_H5" == true ]]; then
    printf 'Selected Mall H5 target.\n'
    run_mall_h5
fi
