#!/usr/bin/env bash
# Stop the rootless Pasta Pod started by up.sh.
# Pass --volumes only when all service data should be removed.

set -Eeuo pipefail

POD_NAME="${POD_NAME:-mitedtsm-rootless}"
# Quartz is configured to wait for active jobs on shutdown, so Podman's
# default 10-second stop window is too short for the Java server.
STOP_TIMEOUT="${STOP_TIMEOUT:-120}"
VOLUMES=(
    "${POD_NAME}-mysql-data"
    "${POD_NAME}-redis-data"
    "${POD_NAME}-rabbitmq-data"
    "${POD_NAME}-tdengine-data"
)

[[ "$STOP_TIMEOUT" =~ ^[1-9][0-9]*$ ]] || {
    printf 'STOP_TIMEOUT must be a positive number of seconds; got: %s\n' "$STOP_TIMEOUT" >&2
    exit 2
}

if podman pod inspect "$POD_NAME" >/dev/null 2>&1; then
    printf 'Stopping Pod %s gracefully (timeout: %ss).\n' "$POD_NAME" "$STOP_TIMEOUT"
    if ! podman pod stop --time "$STOP_TIMEOUT" "$POD_NAME"; then
        printf 'Graceful stop failed; forcibly removing Pod %s.\n' "$POD_NAME" >&2
        podman pod rm --force "$POD_NAME"
    else
        podman pod rm "$POD_NAME"
    fi
fi

if [[ "${1:-}" == "--volumes" ]]; then
    for volume in "${VOLUMES[@]}"; do
        if podman volume inspect "$volume" >/dev/null 2>&1; then
            podman volume rm "$volume"
        fi
    done
fi
