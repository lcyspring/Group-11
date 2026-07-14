#!/usr/bin/env bash

set -Eeuo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
PODMAN_DIR="$(cd -- "${SCRIPT_DIR}/../.." && pwd)"

usage() {
    printf 'Usage: bash ./tests/runtime-config/run.sh <runtime-config.yaml>\n' >&2
}

[[ $# -eq 1 ]] || {
    usage
    exit 2
}

CONFIG_PATH="$1"
if [[ "$CONFIG_PATH" != /* ]]; then
    CONFIG_PATH="$(cd -- "$(dirname -- "$CONFIG_PATH")" && pwd)/$(basename -- "$CONFIG_PATH")"
fi

# shellcheck source=../../lib/yaml-config.sh
source "${PODMAN_DIR}/lib/yaml-config.sh"

fail() {
    printf 'FAIL: %s\n' "$1" >&2
    exit 1
}

expect_exit_2() {
    local output status
    set +e
    output="$("$@" 2>&1)"
    status=$?
    set -e
    [[ $status -eq 2 ]] || fail "expected exit 2 from '$*', got $status: $output"
}

expect_exit_1() {
    local output status
    set +e
    output="$("$@" 2>&1)"
    status=$?
    set -e
    [[ $status -eq 1 ]] || fail "expected exit 1 from '$*', got $status: $output"
}

pod_snapshot() {
    local pod_name="$1"
    podman pod inspect --format '{{.Id}}:{{.State}}' "$pod_name" 2>/dev/null || printf 'absent'
}

bash -n "${PODMAN_DIR}/up.sh"
bash -n "${PODMAN_DIR}/down.sh"
bash -n "${PODMAN_DIR}/image-archives.sh"
bash -n "${PODMAN_DIR}/lib/yaml-config.sh"

yaml_config_init "${SCRIPT_DIR}/fixtures/parser-valid.yaml"
[[ "$(yaml_require sample.plain)" == "value" ]] || fail 'plain scalar parsing'
[[ "$(yaml_require sample.quoted)" == "value # kept" ]] || fail 'quoted comment parsing'
[[ "$(yaml_bool sample.enabled)" == "true" ]] || fail 'boolean parsing'
[[ "$(yaml_port sample.port)" == "18080" ]] || fail 'port parsing'

yaml_config_init "${SCRIPT_DIR}/fixtures/parser-duplicate.yaml"
if yaml_require sample.value >/dev/null 2>&1; then
    fail 'duplicate keys must be rejected'
fi

yaml_config_init "${SCRIPT_DIR}/fixtures/parser-invalid-depth.yaml"
if yaml_require sample.nested >/dev/null 2>&1; then
    fail 'mappings deeper than two levels must be rejected'
fi

expect_exit_2 bash "${PODMAN_DIR}/up.sh"
expect_exit_2 bash "${PODMAN_DIR}/up.sh" "$CONFIG_PATH" extra
expect_exit_2 bash "${PODMAN_DIR}/down.sh"
expect_exit_2 bash "${PODMAN_DIR}/down.sh" "$CONFIG_PATH" extra
expect_exit_2 bash "${PODMAN_DIR}/image-archives.sh"
expect_exit_2 bash "${PODMAN_DIR}/image-archives.sh" "$CONFIG_PATH" extra
expect_exit_1 bash "${PODMAN_DIR}/image-archives.sh" \
    "${SCRIPT_DIR}/fixtures/archive-check-missing.yaml"

yaml_config_init "$CONFIG_PATH"
pod_name="$(yaml_require deployment.pod_name)"
before="$(pod_snapshot "$pod_name")"
bash "${PODMAN_DIR}/up.sh" "$CONFIG_PATH"
bash "${PODMAN_DIR}/down.sh" "$CONFIG_PATH"
env POD_NAME=ignored SERVER_PORT=invalid IMAGE_SOURCE=invalid USE_HOST_PROXY=invalid \
    bash "${PODMAN_DIR}/up.sh" "$CONFIG_PATH"
env POD_NAME=ignored STOP_TIMEOUT=invalid \
    bash "${PODMAN_DIR}/down.sh" "$CONFIG_PATH"
after="$(pod_snapshot "$pod_name")"
[[ "$after" == "$before" ]] || fail "check modes changed Pod state: before=$before after=$after"

printf 'Runtime YAML configuration tests passed. Pod state remained %s.\n' "$after"
