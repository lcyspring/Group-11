#!/usr/bin/env bash

set -Eeuo pipefail

if [[ $# -ne 0 ]]; then
    printf 'The Mall H5 build entrypoint does not accept command-line arguments.\n' >&2
    exit 2
fi

source /etc/os-release
[[ "${ID:-}" == "ubuntu" && "${VERSION_ID:-}" == "26.04" ]] || {
    printf 'Mall H5 build image must be Ubuntu 26.04; found %s %s.\n' \
        "${ID:-unknown}" "${VERSION_ID:-unknown}" >&2
    exit 1
}

PROJECT_DIR="${HBUILDERX_PROJECT_DIR:-/workspace/MallFrontend}"
PLATFORM="${HBUILDERX_PLATFORM:-h5}"
CLEAN_OUTPUT="${HBUILDERX_CLEAN_OUTPUT:-true}"
LEGACY_MEDIA_ORIGINS="${SHOPRO_LEGACY_MEDIA_ORIGINS:?SHOPRO_LEGACY_MEDIA_ORIGINS is required}"
LEGACY_MEDIA_FALLBACK="${SHOPRO_LEGACY_MEDIA_FALLBACK:?SHOPRO_LEGACY_MEDIA_FALLBACK is required}"
OUTPUT_DIR="${PROJECT_DIR}/unpackage/dist/build/web"
NODE=/opt/HBuilderX/plugins/node/node
UNI_CLI_CONTEXT=/opt/HBuilderX/plugins/uniapp-cli-vite
UNI_CLI="${UNI_CLI_CONTEXT}/node_modules/@dcloudio/vite-plugin-uni/bin/uni.js"

case "$PLATFORM" in
    h5) ;;
    *)
        printf 'HBUILDERX_PLATFORM must be h5; got: %s\n' "$PLATFORM" >&2
        exit 2
        ;;
esac
case "$CLEAN_OUTPUT" in
    true|false) ;;
    *)
        printf 'HBUILDERX_CLEAN_OUTPUT must be true or false; got: %s\n' "$CLEAN_OUTPUT" >&2
        exit 2
        ;;
esac

[[ -d "$PROJECT_DIR" ]] || {
    printf 'Mall H5 project directory is missing: %s\n' "$PROJECT_DIR" >&2
    exit 1
}
[[ -x "$NODE" && -f "$UNI_CLI" ]] || {
    printf 'The image does not contain the HBuilderX headless uni-app compiler.\n' >&2
    exit 1
}

export HOME=/tmp/hbuilderx-home
export XDG_CACHE_HOME="$HOME/.cache"
export NODE_ENV=production
export UNI_PLATFORM=h5
export UNI_INPUT_DIR="$PROJECT_DIR"
export UNI_OUTPUT_DIR="$OUTPUT_DIR"
export UNI_CLI_CONTEXT
export UNI_HBUILDERX_PLUGINS=/opt/HBuilderX/plugins
export HX_APP_ROOT=/opt/HBuilderX
mkdir -p "$HOME" "$XDG_CACHE_HOME"

if [[ "$CLEAN_OUTPUT" == "true" ]]; then
    rm -rf -- "$OUTPUT_DIR" "${PROJECT_DIR}/unpackage/dist/build/h5"
fi

printf 'Build OS: Ubuntu 26.04\n'
printf 'Build engine: HBuilderX uni-app CLI %s (%s)\n' \
    "$("$NODE" -p "require('${UNI_CLI_CONTEXT}/package.json').version")" \
    "$("$NODE" --version)"
printf 'Checking Mall media URL normalization.\n'
"$NODE" --experimental-test-coverage --test "${PROJECT_DIR}/sheep/url/legacy-media.test.mjs"

cd -- "$PROJECT_DIR"
"$NODE" "$UNI_CLI" build

[[ -s "$OUTPUT_DIR/index.html" ]] || {
    printf 'uni-app CLI completed without the expected output: %s\n' \
        "$OUTPUT_DIR/index.html" >&2
    exit 1
}

printf 'Mall H5 assets are ready: %s\n' "$OUTPUT_DIR"
