#!/usr/bin/env bash

set -Eeuo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
DASEL_VERSION="v3.11.2"
OUTPUT_DIR="${SCRIPT_DIR}/bin"
TEMP_DIR="$(mktemp -d)"

cleanup() {
    rm -rf -- "$TEMP_DIR"
}
trap cleanup EXIT

case "$(uname -m)" in
    x86_64|amd64)
        DASEL_ASSET="dasel_linux_amd64"
        DASEL_SHA256="5006ee3a4239ab6a3edb1bf5c932874d814f7c276117ca677352697a4f547799"
        ;;
    aarch64|arm64)
        DASEL_ASSET="dasel_linux_arm64"
        DASEL_SHA256="df33920a792cd8ee5573cec3632a710cd2178d7bc4fc3892c6be78f35e7cbcc6"
        ;;
    *)
        printf 'Unsupported dasel host architecture: %s\n' "$(uname -m)" >&2
        exit 2
        ;;
esac

command -v curl >/dev/null 2>&1 || { printf 'curl is required to install dasel.\n' >&2; exit 2; }
command -v sha256sum >/dev/null 2>&1 || { printf 'sha256sum is required to verify dasel.\n' >&2; exit 2; }

DASEL_URL="https://github.com/TomWright/dasel/releases/download/${DASEL_VERSION}/${DASEL_ASSET}"
DOWNLOAD_PATH="${TEMP_DIR}/${DASEL_ASSET}"

curl --fail --location --retry 3 --retry-delay 2 --output "$DOWNLOAD_PATH" "$DASEL_URL"
printf '%s  %s\n' "$DASEL_SHA256" "$DOWNLOAD_PATH" | sha256sum --check --status || {
    printf 'dasel checksum verification failed: %s\n' "$DASEL_URL" >&2
    exit 1
}

mkdir -p "$OUTPUT_DIR"
install -m 0755 "$DOWNLOAD_PATH" "${OUTPUT_DIR}/dasel"
[[ "$("${OUTPUT_DIR}/dasel" version)" == "${DASEL_VERSION#v}" ]] || {
    printf 'Installed dasel version verification failed.\n' >&2
    exit 1
}
printf 'Installed official dasel %s release: %s\n' \
    "${DASEL_VERSION#v}" "${OUTPUT_DIR}/dasel"
