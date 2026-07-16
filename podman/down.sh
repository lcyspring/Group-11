#!/usr/bin/env bash
# Stop the rootless Podman Pod started by up.sh.
# Pass --volumes only when all service data should be removed.

set -Eeuo pipefail

POD_NAME="${POD_NAME:-mitedtsm-rootless}"
# Quartz is configured to wait for active jobs on shutdown, so Podman's
# default 10-second stop window is too short for the Java server.
STOP_TIMEOUT="${STOP_TIMEOUT:-120}"
REMOVE_VOLUMES=false
VOLUMES=(
    "${POD_NAME}-mysql-data"
    "${POD_NAME}-redis-data"
    "${POD_NAME}-rabbitmq-data"
    "${POD_NAME}-tdengine-data"
)

usage() {
    cat <<'EOF'
Usage: bash ./down.sh [--volumes]

Stops and removes the rootless Podman Pod. Persistent volumes are retained
unless --volumes is supplied.
EOF
}

case "$#" in
    0) ;;
    1)
        case "$1" in
            --volumes)
                REMOVE_VOLUMES=true
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
