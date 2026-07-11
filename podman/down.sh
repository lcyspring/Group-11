#!/usr/bin/env bash
# Stop the rootless Pasta Pod started by up.sh.
# Pass --volumes only when all service data should be removed.

set -Eeuo pipefail

POD_NAME="${POD_NAME:-mitedtsm-rootless}"
VOLUMES=(
    "${POD_NAME}-mysql-data"
    "${POD_NAME}-redis-data"
    "${POD_NAME}-rabbitmq-data"
    "${POD_NAME}-tdengine-data"
)

if podman pod inspect "$POD_NAME" >/dev/null 2>&1; then
    podman pod rm --force "$POD_NAME"
fi

if [[ "${1:-}" == "--volumes" ]]; then
    for volume in "${VOLUMES[@]}"; do
        if podman volume inspect "$volume" >/dev/null 2>&1; then
            podman volume rm "$volume"
        fi
    done
fi
