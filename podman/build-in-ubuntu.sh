#!/usr/bin/env bash

set -Eeuo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd -- "${SCRIPT_DIR}/.." && pwd)"

usage() {
    printf 'Usage: bash ./build-in-ubuntu.sh <config.yaml>\n' >&2
}

if [[ $# -ne 1 ]]; then
    usage
    exit 2
fi

CONFIG_PATH="$1"
if [[ "$CONFIG_PATH" != /* ]]; then
    CONFIG_PATH="$(cd -- "$(dirname -- "$CONFIG_PATH")" && pwd)/$(basename -- "$CONFIG_PATH")"
fi
[[ -f "$CONFIG_PATH" ]] || {
    printf 'Build configuration does not exist: %s\n' "$CONFIG_PATH" >&2
    exit 2
}

yaml_get() {
    local wanted="$1"
    awk -v wanted="$wanted" '
        function trim(value) {
            sub(/^[[:space:]]+/, "", value)
            sub(/[[:space:]]+$/, "", value)
            return value
        }
        /^[[:space:]]*($|#)/ { next }
        {
            line = $0
            indent = match(line, /[^ ]/) - 1
            content = substr(line, indent + 1)
            separator = index(content, ":")
            if (!separator) next
            key = trim(substr(content, 1, separator - 1))
            value = trim(substr(content, separator + 1))
            sub(/[[:space:]]+#.*$/, "", value)
            value = trim(value)
            if (indent == 0) {
                section = key
                path = key
            } else if (indent == 2) {
                path = section "." key
            } else {
                next
            }
            if (path == wanted && value != "") {
                if ((substr(value, 1, 1) == "\"" && substr(value, length(value), 1) == "\"") ||
                    (substr(value, 1, 1) == "\047" && substr(value, length(value), 1) == "\047")) {
                    value = substr(value, 2, length(value) - 2)
                }
                print value
                exit
            }
        }
    ' "$CONFIG_PATH"
}

config_value() {
    local key="$1" default_value="${2:-}" value
    value="$(yaml_get "$key")"
    printf '%s' "${value:-$default_value}"
}

normalize_bool() {
    case "${1,,}" in
        true|1|yes) printf 'true' ;;
        false|0|no) printf 'false' ;;
        *)
            printf 'Configuration value must be boolean; got: %s\n' "$1" >&2
            exit 2
            ;;
    esac
}

[[ "$(config_value schema_version)" == "1" ]] || {
    printf 'Unsupported or missing schema_version; expected 1.\n' >&2
    exit 2
}

BASE_IMAGE="$(config_value image.base docker.io/library/ubuntu:26.04)"
BUILD_IMAGE="$(config_value image.name localhost/mitedtsm-build-ubuntu:26.04)"
REBUILD_IMAGE="$(normalize_bool "$(config_value image.rebuild false)")"
PNPM_VERSION="$(config_value toolchain.pnpm_version 11.3.0)"
BUILD_SERVER="$(normalize_bool "$(config_value build.server true)")"
BUILD_INIT_SERVICE="$(normalize_bool "$(config_value build.init_service true)")"
BUILD_WEB="$(normalize_bool "$(config_value build.web true)")"
BUILD_CLEAN="$(normalize_bool "$(config_value build.clean true)")"
BUILD_CRM_TESTS="$(normalize_bool "$(config_value build.crm_tests true)")"
BUILD_CRM_COVERAGE="$(normalize_bool "$(config_value build.crm_coverage true)")"
BUILD_ERP_TESTS="$(normalize_bool "$(config_value build.erp_tests false)")"
BUILD_ERP_COVERAGE="$(normalize_bool "$(config_value build.erp_coverage false)")"
BUILD_INFRA_TESTS="$(normalize_bool "$(config_value build.infra_tests false)")"
BUILD_INFRA_COVERAGE="$(normalize_bool "$(config_value build.infra_coverage false)")"
BUILD_BPM_TESTS="$(normalize_bool "$(config_value build.bpm_tests false)")"
BUILD_BPM_COVERAGE="$(normalize_bool "$(config_value build.bpm_coverage false)")"
BUILD_COMMON_TESTS="$(normalize_bool "$(config_value build.common_tests false)")"
BUILD_COMMON_COVERAGE="$(normalize_bool "$(config_value build.common_coverage false)")"
BUILD_COMMON_TEST_PATTERN="$(config_value build.common_test_pattern '')"
BUILD_FRAMEWORK_TESTS="$(normalize_bool "$(config_value build.framework_tests false)")"
BUILD_FRAMEWORK_COVERAGE="$(normalize_bool "$(config_value build.framework_coverage false)")"
BUILD_FRAMEWORK_TEST_PATTERN="$(config_value build.framework_test_pattern '')"
BUILD_CI="$(normalize_bool "$(config_value build.ci true)")"
BAIDU_ANALYTICS_CODE="$(config_value web.baidu_analytics_code '')"
WEB_LEGACY_MEDIA_ORIGINS="$(config_value web.legacy_media_origins '')"
WEB_TEST_SCRIPT="$(config_value web.test_script '')"
MAVEN_THREADS="$(config_value build.maven_threads 1C)"
PNPM_FROZEN_LOCKFILE="$(normalize_bool "$(config_value build.pnpm_frozen_lockfile true)")"
USE_HOST_PROXY="$(normalize_bool "$(config_value network.use_host_proxy false)")"
MAVEN_VOLUME="$(config_value cache.maven_volume mitedtsm-build-maven-cache)"
PNPM_VOLUME="$(config_value cache.pnpm_store_volume mitedtsm-build-pnpm-store)"
PNPM_STORE_PATH="$(config_value cache.pnpm_store_path /pnpm-store)"
WEB_NODE_MODULES_VOLUME="$(config_value cache.web_node_modules_volume mitedtsm-build-web-node-modules)"
MEMORY="$(config_value runtime.memory 8g)"
CPUS="$(config_value runtime.cpus 4)"
HOST_PROXY_NAME="host.containers.internal"

[[ "$PNPM_STORE_PATH" == /* && "$PNPM_STORE_PATH" != "/" && "$PNPM_STORE_PATH" != /workspace* ]] || {
    printf 'cache.pnpm_store_path must be an absolute container path outside /workspace.\n' >&2
    exit 2
}

if [[ "$BAIDU_ANALYTICS_CODE" == "disabled" ]]; then
    BAIDU_ANALYTICS_CODE=""
fi
if [[ -n "$BAIDU_ANALYTICS_CODE" && ! "$BAIDU_ANALYTICS_CODE" =~ ^[A-Za-z0-9_-]+$ ]]; then
    printf 'web.baidu_analytics_code contains unsupported characters.\n' >&2
    exit 2
fi
if [[ -n "$WEB_LEGACY_MEDIA_ORIGINS" &&
      ! "$WEB_LEGACY_MEDIA_ORIGINS" =~ ^https?://[^,[:space:]]+(,https?://[^,[:space:]]+)*$ ]]; then
    printf 'web.legacy_media_origins must be a comma-separated list of HTTP(S) origins.\n' >&2
    exit 2
fi
if [[ "$BUILD_WEB" == "true" && -z "$WEB_LEGACY_MEDIA_ORIGINS" ]]; then
    printf 'web.legacy_media_origins is required when build.web is true; explicitly list retired media origins.\n' >&2
    exit 2
fi
if [[ -n "$WEB_TEST_SCRIPT" && ! "$WEB_TEST_SCRIPT" =~ ^[A-Za-z0-9:_-]+$ ]]; then
    printf 'web.test_script contains unsupported characters.\n' >&2
    exit 2
fi
if [[ "$BUILD_COMMON_TESTS" == "true" && ! "$BUILD_COMMON_TEST_PATTERN" =~ ^[A-Za-z0-9_.*?,]+$ ]]; then
    printf 'build.common_test_pattern is required for common tests and contains unsupported characters.\n' >&2
    exit 2
fi
if [[ "$BUILD_FRAMEWORK_COVERAGE" == "true" && "$BUILD_FRAMEWORK_TESTS" != "true" ]]; then
    printf 'Framework coverage requires framework tests to be enabled.\n' >&2
    exit 2
fi
if [[ "$BUILD_FRAMEWORK_TESTS" == "true" && ! "$BUILD_FRAMEWORK_TEST_PATTERN" =~ ^[A-Za-z0-9_.*?,]+$ ]]; then
    printf 'build.framework_test_pattern is required for framework tests and contains unsupported characters.\n' >&2
    exit 2
fi

container_proxy_url() {
    local url="${1:-}"
    url="${url//127.0.0.1/${HOST_PROXY_NAME}}"
    url="${url//localhost/${HOST_PROXY_NAME}}"
    printf '%s' "$url"
}

podman_proxy_args=(--http-proxy=false)
if [[ "$USE_HOST_PROXY" == "true" ]]; then
    podman_proxy_args=()
fi

command -v podman >/dev/null 2>&1 || {
    printf 'Podman is required.\n' >&2
    exit 1
}
[[ "$(podman info --format '{{.Host.Security.Rootless}}')" == "true" ]] || {
    printf 'Run this script as the rootless Podman user.\n' >&2
    exit 1
}

if [[ "$REBUILD_IMAGE" == "true" ]] || ! podman image exists "$BUILD_IMAGE"; then
    podman image exists "$BASE_IMAGE" || {
        printf 'Configured Ubuntu base image is not local: %s\n' "$BASE_IMAGE" >&2
        exit 1
    }
    printf 'Building dedicated Ubuntu toolchain image: %s\n' "$BUILD_IMAGE"
    podman build "${podman_proxy_args[@]}" --pull=never \
        --build-arg "UBUNTU_BASE_IMAGE=$BASE_IMAGE" \
        --build-arg "PNPM_VERSION=$PNPM_VERSION" \
        --tag "$BUILD_IMAGE" \
        --file "$SCRIPT_DIR/Containerfile.build-ubuntu" \
        "$PROJECT_ROOT"
fi

podman volume inspect "$MAVEN_VOLUME" >/dev/null 2>&1 || podman volume create "$MAVEN_VOLUME" >/dev/null
podman volume inspect "$PNPM_VOLUME" >/dev/null 2>&1 || podman volume create "$PNPM_VOLUME" >/dev/null
podman volume inspect "$WEB_NODE_MODULES_VOLUME" >/dev/null 2>&1 || podman volume create "$WEB_NODE_MODULES_VOLUME" >/dev/null

proxy_args=()
if [[ "$USE_HOST_PROXY" == "true" ]]; then
    for proxy_name in http_proxy HTTP_PROXY https_proxy HTTPS_PROXY all_proxy ALL_PROXY no_proxy NO_PROXY; do
        if [[ -n "${!proxy_name:-}" ]]; then
            proxy_args+=(--env "$proxy_name=$(container_proxy_url "${!proxy_name}")")
        fi
    done
fi

printf 'Running configured build in %s.\n' "$BUILD_IMAGE"
podman run "${podman_proxy_args[@]}" --rm --pull=never \
    --memory "$MEMORY" \
    --cpus "$CPUS" \
    --volume "$PROJECT_ROOT:/workspace:rw" \
    --volume "$MAVEN_VOLUME:/root/.m2:rw" \
    --volume "$PNPM_VOLUME:$PNPM_STORE_PATH:rw" \
    --volume "$WEB_NODE_MODULES_VOLUME:/workspace/Web/node_modules:rw" \
    --env "BUILD_SERVER=$BUILD_SERVER" \
    --env "BUILD_INIT_SERVICE=$BUILD_INIT_SERVICE" \
    --env "BUILD_WEB=$BUILD_WEB" \
    --env "BUILD_CLEAN=$BUILD_CLEAN" \
    --env "BUILD_CRM_TESTS=$BUILD_CRM_TESTS" \
    --env "BUILD_CRM_COVERAGE=$BUILD_CRM_COVERAGE" \
    --env "BUILD_ERP_TESTS=$BUILD_ERP_TESTS" \
    --env "BUILD_ERP_COVERAGE=$BUILD_ERP_COVERAGE" \
    --env "BUILD_INFRA_TESTS=$BUILD_INFRA_TESTS" \
    --env "BUILD_INFRA_COVERAGE=$BUILD_INFRA_COVERAGE" \
    --env "BUILD_BPM_TESTS=$BUILD_BPM_TESTS" \
    --env "BUILD_BPM_COVERAGE=$BUILD_BPM_COVERAGE" \
    --env "BUILD_COMMON_TESTS=$BUILD_COMMON_TESTS" \
    --env "BUILD_COMMON_COVERAGE=$BUILD_COMMON_COVERAGE" \
    --env "BUILD_COMMON_TEST_PATTERN=$BUILD_COMMON_TEST_PATTERN" \
    --env "BUILD_FRAMEWORK_TESTS=$BUILD_FRAMEWORK_TESTS" \
    --env "BUILD_FRAMEWORK_COVERAGE=$BUILD_FRAMEWORK_COVERAGE" \
    --env "BUILD_FRAMEWORK_TEST_PATTERN=$BUILD_FRAMEWORK_TEST_PATTERN" \
    --env "BUILD_CI=$BUILD_CI" \
    --env "BUILD_MAVEN_THREADS=$MAVEN_THREADS" \
    --env "PNPM_FROZEN_LOCKFILE=$PNPM_FROZEN_LOCKFILE" \
    --env "PNPM_STORE_PATH=$PNPM_STORE_PATH" \
    --env "BUILD_USE_HOST_PROXY=$USE_HOST_PROXY" \
    --env "VITE_APP_BAIDU_CODE=$BAIDU_ANALYTICS_CODE" \
    --env "VITE_APP_LEGACY_MEDIA_ORIGINS=$WEB_LEGACY_MEDIA_ORIGINS" \
    --env "WEB_TEST_SCRIPT=$WEB_TEST_SCRIPT" \
    "${proxy_args[@]}" \
    --entrypoint /bin/bash \
    "$BUILD_IMAGE" \
    /workspace/podman/ubuntu-build-entrypoint.sh
