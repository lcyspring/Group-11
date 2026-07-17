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
PNPM_STORE_PATH="${PNPM_STORE_PATH:?PNPM_STORE_PATH is required}"
FROZEN_LOCKFILE="${PNPM_FROZEN_LOCKFILE:?PNPM_FROZEN_LOCKFILE is required}"
USE_HOST_PROXY="${BUILD_USE_HOST_PROXY:?BUILD_USE_HOST_PROXY is required}"

[[ -f "$PROJECT_DIR/package.json" && -f "$PROJECT_DIR/pnpm-lock.yaml" ]] || {
    printf 'Mall package manifest or lockfile is missing.\n' >&2
    exit 1
}
[[ "$PNPM_STORE_PATH" == /* && "$PNPM_STORE_PATH" != /workspace* ]] || {
    printf 'PNPM_STORE_PATH must be absolute and outside /workspace.\n' >&2
    exit 2
}
case "$FROZEN_LOCKFILE" in true|false) ;; *) exit 2 ;; esac
case "$USE_HOST_PROXY" in true|false) ;; *) exit 2 ;; esac

install_args=(--dir "$PROJECT_DIR" --store-dir "$PNPM_STORE_PATH" install)
[[ "$FROZEN_LOCKFILE" == "true" ]] && install_args+=(--frozen-lockfile)

printf 'Dependency OS: Ubuntu 26.04\n'
printf 'Installing Mall node_modules into the mounted Podman volume.\n'
if [[ "$USE_HOST_PROXY" == "true" ]]; then
    env CI=true pnpm "${install_args[@]}"
else
    env -u http_proxy -u HTTP_PROXY -u https_proxy -u HTTPS_PROXY \
        -u all_proxy -u ALL_PROXY -u no_proxy -u NO_PROXY \
        CI=true pnpm "${install_args[@]}"
fi

[[ -d "$PROJECT_DIR/node_modules/.pnpm" ]] || {
    printf 'pnpm completed without a valid node_modules volume.\n' >&2
    exit 1
}
