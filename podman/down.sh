#!/usr/bin/env bash
# Stop the rootless Podman Pod started by up.sh.

set -Eeuo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"

usage() {
    printf 'Usage: bash ./down.sh <config.yaml>\n' >&2
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

POD_NAME="$(yaml_require deployment.pod_name)"
STOP_TIMEOUT="$(yaml_positive_integer deployment.stop_timeout_seconds)"
SERVER_CONTAINER="$(yaml_require container.server)"
SHUTDOWN_MODE="$(yaml_require operation.shutdown_mode)"
REMOVE_VOLUMES="$(yaml_bool operation.remove_volumes_on_down)"
VOLUMES=(
    "$(yaml_require volume.mysql)"
    "$(yaml_require volume.redis)"
    "$(yaml_require volume.rabbitmq)"
    "$(yaml_require volume.tdengine)"
)

command -v podman >/dev/null 2>&1 || {
    printf 'Podman is required.\n' >&2
    exit 1
}

rootless="$(podman info --format '{{.Host.Security.Rootless}}')" || {
    printf 'Podman is installed but not usable by this user. Run podman info for details.\n' >&2
    exit 1
}
[[ "$rootless" == "true" ]] || {
    printf 'Run this script as the normal rootless Podman user.\n' >&2
    exit 1
}

case "$SHUTDOWN_MODE" in
    check)
        printf 'Podman shutdown preflight passed. No Pod or volume was stopped or removed.\n'
        exit 0
        ;;
    stop) ;;
    *)
        printf 'operation.shutdown_mode must be check or stop; got: %s\n' "$SHUTDOWN_MODE" >&2
        exit 2
        ;;
esac

if podman pod inspect "$POD_NAME" >/dev/null 2>&1; then
    printf 'Stopping Pod %s gracefully (timeout: %ss).\n' "$POD_NAME" "$STOP_TIMEOUT"
    # Spring shutdown hooks may require MySQL/RabbitMQ, so stop the app first.
    if [[ "$(podman inspect --format '{{.State.Running}}' "$SERVER_CONTAINER" 2>/dev/null)" == "true" ]]; then
        printf 'Stopping application server before infrastructure.\n'
        podman stop --time "$STOP_TIMEOUT" "$SERVER_CONTAINER"
    fi
    if ! podman pod stop --time "$STOP_TIMEOUT" "$POD_NAME"; then
        printf 'Graceful stop failed; forcibly removing Pod %s.\n' "$POD_NAME" >&2
        podman pod rm --force "$POD_NAME"
    else
        podman pod rm "$POD_NAME"
    fi
else
    printf 'Pod does not exist: %s\n' "$POD_NAME"
fi

if [[ "$REMOVE_VOLUMES" == true ]]; then
    for volume in "${VOLUMES[@]}"; do
        if podman volume inspect "$volume" >/dev/null 2>&1; then
            podman volume rm "$volume"
        fi
    done
fi
