#!/usr/bin/env bash

set -Eeuo pipefail

ROOT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
BUNDLE_ROOT="$ROOT_DIR"
RUNTIME_CONFIG="${ROOT_DIR}/podman/config/runtime-local.kdl"

[[ $# -eq 0 ]] || {
    printf 'Usage: ./verify.sh\n' >&2
    exit 2
}

for command in curl jq; do
    command -v "$command" >/dev/null 2>&1 || {
        printf 'Required verification command is unavailable: %s\n' "$command" >&2
        exit 1
    }
done
[[ -f "$RUNTIME_CONFIG" ]] || {
    printf 'Deployment is not configured. Run ./deploy.sh first.\n' >&2
    exit 1
}

# shellcheck source=container-engine.sh
source "${ROOT_DIR}/container-engine.sh"
ENGINE="$(resolve_container_engine)"

# shellcheck source=../lib/kdl-config.sh
source "${ROOT_DIR}/podman/lib/kdl-config.sh"
kdl_config_init "$RUNTIME_CONFIG"

containers=(
    "$(kdl_require container.mysql)"
    "$(kdl_require container.redis)"
    "$(kdl_require container.rabbitmq)"
    "$(kdl_require container.tdengine)"
    "$(kdl_require container.server)"
    "$(kdl_require container.web)"
    "$(kdl_require container.mall)"
)

for container in "${containers[@]}"; do
    running="$($ENGINE inspect --format '{{.State.Running}}' "$container" 2>/dev/null || true)"
    [[ "$running" == true ]] || {
        printf '%s container is not running: %s\n' "$ENGINE" "$container" >&2
        exit 1
    }
done

server_port="$(kdl_port network.server_host_port)"
web_port="$(kdl_port network.web_host_port)"
mall_port="$(kdl_port network.mall_host_port)"
server_path="$(kdl_require health.server_path)"
web_path="$(kdl_require health.web_path)"
mall_path="$(kdl_require health.mall_path)"

health="$(curl --noproxy '*' --fail --silent --show-error "http://127.0.0.1:${server_port}${server_path}")"
jq -e '.status == "UP"' >/dev/null <<< "$health" || {
    printf 'Server health is not UP: %s\n' "$health" >&2
    exit 1
}
curl --noproxy '*' --fail --silent --show-error --output /dev/null "http://127.0.0.1:${web_port}${web_path}"
curl --noproxy '*' --fail --silent --show-error --output /dev/null "http://127.0.0.1:${mall_port}${mall_path}"

printf 'Offline %s deployment is healthy: 7 services running, Server UP, Web 200, Mall 200.\n' "$ENGINE"
printf 'Configured Web URL: %s\n' "$(kdl_require network.admin_ui_public_url)"
