#!/usr/bin/env bash

set -Eeuo pipefail

ROOT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
BUNDLE_ROOT="$ROOT_DIR"
RUNTIME_CONFIG="${ROOT_DIR}/podman/config/runtime-local.kdl"
CHECK_CONFIG="${ROOT_DIR}/podman/config/runtime-check-local.kdl"

[[ $# -le 1 ]] || {
    printf 'Usage: ./deploy.sh [public-ip-or-dns-name]\n' >&2
    exit 2
}

if [[ $# -eq 1 ]]; then
    bash "${ROOT_DIR}/configure.sh" --host "$1"
else
    bash "${ROOT_DIR}/configure.sh"
fi

for command in curl jq sha256sum; do
    command -v "$command" >/dev/null 2>&1 || {
        printf 'Required target-host command is unavailable: %s\n' "$command" >&2
        exit 1
    }
done

# shellcheck source=container-engine.sh
source "${ROOT_DIR}/container-engine.sh"
ENGINE="$(resolve_container_engine)"
printf 'Selected container engine: %s.\n' "$ENGINE"

printf 'Verifying common Podman/Docker image archives.\n'
(
    cd "${ROOT_DIR}/podman/images"
    sha256sum --check SHA256SUMS
)

printf 'Running stateless %s deployment preflight.\n' "$ENGINE"
case "$ENGINE" in
    podman) bash "${ROOT_DIR}/podman/deploy.sh" "$CHECK_CONFIG" ;;
    docker) bash "${ROOT_DIR}/docker/runtime.sh" check ;;
esac

# Record before creating stateful resources so a failed first deployment is
# always retried with the same engine and named-volume namespace.
record_container_engine "$ENGINE"
printf 'Deploying the offline bundle with %s.\n' "$ENGINE"
case "$ENGINE" in
    podman) bash "${ROOT_DIR}/podman/deploy.sh" "$RUNTIME_CONFIG" ;;
    docker) bash "${ROOT_DIR}/docker/runtime.sh" deploy ;;
esac
bash "${ROOT_DIR}/verify.sh"
