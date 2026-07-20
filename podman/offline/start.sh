#!/usr/bin/env bash

set -Eeuo pipefail

ROOT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
BUNDLE_ROOT="$ROOT_DIR"

[[ $# -eq 0 ]] || {
    printf 'Usage: ./start.sh\n' >&2
    exit 2
}
[[ -f "${ROOT_DIR}/podman/config/runtime-local.kdl" ]] || {
    printf 'Deployment is not configured. Run ./deploy.sh first.\n' >&2
    exit 1
}

# shellcheck source=container-engine.sh
source "${ROOT_DIR}/container-engine.sh"
ENGINE="$(resolve_container_engine)"
case "$ENGINE" in
    podman)
        bash "${ROOT_DIR}/podman/deploy.sh" "${ROOT_DIR}/podman/config/runtime-fast-local.kdl"
        ;;
    docker)
        bash "${ROOT_DIR}/docker/runtime.sh" start
        ;;
esac
bash "${ROOT_DIR}/verify.sh"
