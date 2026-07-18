#!/usr/bin/env bash

set -Eeuo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
DASEL_VERSION="v3.11.2"
DASEL_COMMIT="008b0ed9cae7d5d5b0c72e23c84836c5b2f0338b"
SOURCE_DIR="$(mktemp -d)"
OUTPUT_DIR="${SCRIPT_DIR}/bin"
CACHE_DIR="${SCRIPT_DIR}/.cache"

cleanup() {
    rm -rf -- "$SOURCE_DIR"
}
trap cleanup EXIT

command -v git >/dev/null 2>&1 || { printf 'git is required to build dasel.\n' >&2; exit 2; }
command -v go >/dev/null 2>&1 || { printf 'Go 1.25 or newer is required to build dasel.\n' >&2; exit 2; }

git -c advice.detachedHead=false clone --quiet --depth 1 --branch "$DASEL_VERSION" \
    https://github.com/TomWright/dasel.git "$SOURCE_DIR/dasel"
[[ "$(git -C "$SOURCE_DIR/dasel" rev-parse HEAD)" == "$DASEL_COMMIT" ]] || {
    printf 'Unexpected dasel source revision for %s.\n' "$DASEL_VERSION" >&2
    exit 1
}

mkdir -p "$OUTPUT_DIR" "${CACHE_DIR}/go-build" "${CACHE_DIR}/go-mod"
(
    cd -- "$SOURCE_DIR/dasel"
    env CGO_ENABLED=0 \
        GOCACHE="${CACHE_DIR}/go-build" \
        GOMODCACHE="${CACHE_DIR}/go-mod" \
        go build -trimpath \
        -ldflags="-s -w -X=github.com/tomwright/dasel/v3/internal.Version=${DASEL_VERSION}" \
        -o "${OUTPUT_DIR}/dasel" ./cmd/dasel
)

[[ "$("${OUTPUT_DIR}/dasel" version)" == "${DASEL_VERSION#v}" ]] || {
    printf 'Built dasel version verification failed.\n' >&2
    exit 1
}
printf 'Built dasel %s: %s\n' "${DASEL_VERSION#v}" "${OUTPUT_DIR}/dasel"
