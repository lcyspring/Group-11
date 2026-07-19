#!/usr/bin/env bash

set -Eeuo pipefail

if [[ $# -ne 0 ]]; then
    printf 'The Mall dependency entrypoint does not accept command-line arguments.\n' >&2
    exit 2
fi

source /etc/os-release
[[ "${ID:-}" == "ubuntu" && "${VERSION_ID:-}" == "26.04" ]] || {
    printf 'Mall dependencies must be installed in Ubuntu 26.04; found %s %s.\n' \
        "${ID:-unknown}" "${VERSION_ID:-unknown}" >&2
    exit 1
}

PROJECT_DIR=/workspace/MallFrontend
DENO_DIR="${DENO_DIR:?DENO_DIR is required}"
FROZEN_LOCKFILE="${DENO_FROZEN_LOCKFILE:?DENO_FROZEN_LOCKFILE is required}"
USE_HOST_PROXY="${BUILD_USE_HOST_PROXY:?BUILD_USE_HOST_PROXY is required}"

[[ -f "$PROJECT_DIR/package.json" && -f "$PROJECT_DIR/deno.json" && -f "$PROJECT_DIR/deno.lock" ]] || {
    printf 'Mall package manifest, Deno configuration, or Deno lockfile is missing.\n' >&2
    exit 1
}
[[ "$DENO_DIR" == /* && "$DENO_DIR" != /workspace* ]] || {
    printf 'DENO_DIR must be absolute and outside /workspace.\n' >&2
    exit 2
}
case "$FROZEN_LOCKFILE" in true|false) ;; *) exit 2 ;; esac
case "$USE_HOST_PROXY" in true|false) ;; *) exit 2 ;; esac

install_args=(install --node-modules-dir=auto --quiet)
[[ "$FROZEN_LOCKFILE" == "true" ]] && install_args+=(--frozen)

if [[ -d "$PROJECT_DIR/node_modules/.pnpm" ]]; then
    printf 'Removing retired pnpm layout from the Mall dependency cache volume.\n'
    find "$PROJECT_DIR/node_modules" -mindepth 1 -maxdepth 1 -exec rm -rf -- {} +
fi

printf 'Dependency OS: Ubuntu 26.04\n'
deno --version
printf 'Installing Mall node_modules into the mounted Podman volume.\n'
cd -- "$PROJECT_DIR"
if [[ "$USE_HOST_PROXY" == "true" ]]; then
    env CI=true deno "${install_args[@]}"
else
    env -u http_proxy -u HTTP_PROXY -u https_proxy -u HTTPS_PROXY \
        -u all_proxy -u ALL_PROXY -u no_proxy -u NO_PROXY \
        CI=true deno "${install_args[@]}"
fi

[[ -f "$PROJECT_DIR/node_modules/dayjs/package.json" ]] || {
    printf 'Deno completed without a valid node_modules volume.\n' >&2
    exit 1
}
[[ ! -e "$PROJECT_DIR/node_modules/.pnpm" \
    && ! -e "$PROJECT_DIR/node_modules/.pnpm-workspace-state-v1.json" \
    && ! -e "$PROJECT_DIR/node_modules/.modules.yaml" ]] || {
    printf 'Retired pnpm layout remains in the Mall dependency cache volume.\n' >&2
    exit 1
}
