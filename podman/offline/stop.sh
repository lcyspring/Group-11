#!/usr/bin/env bash

set -Eeuo pipefail

ROOT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
BUNDLE_ROOT="$ROOT_DIR"
POD_NAME="mitedtsm-rootless"

[[ $# -eq 0 ]] || {
    printf 'Usage: ./stop.sh\n' >&2
    exit 2
}

# shellcheck source=container-engine.sh
source "${ROOT_DIR}/container-engine.sh"
ENGINE="$(resolve_container_engine)"
if [[ "$ENGINE" == docker ]]; then
    bash "${ROOT_DIR}/docker/runtime.sh" stop
    exit 0
fi

if ! podman pod exists "$POD_NAME"; then
    printf 'Pod does not exist: %s\n' "$POD_NAME"
    exit 0
fi

state="$(podman pod inspect --format '{{.State}}' "$POD_NAME")"
if [[ "$state" == Running ]]; then
    podman pod stop --time 120 "$POD_NAME"
fi
printf 'Pod %s is stopped; its definition and persistent volumes are preserved.\n' "$POD_NAME"
