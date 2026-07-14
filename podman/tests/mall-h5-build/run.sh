#!/usr/bin/env bash

set -Eeuo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
PODMAN_DIR="$(cd -- "${SCRIPT_DIR}/../.." && pwd)"
PROJECT_ROOT="$(cd -- "${PODMAN_DIR}/.." && pwd)"
BUILD_SCRIPT="${PODMAN_DIR}/build-mall-h5-in-ubuntu.sh"

if [[ $# -ne 1 ]]; then
    printf 'Usage: bash ./run.sh <config.yaml>\n' >&2
    exit 2
fi

# shellcheck source=../../lib/yaml-config.sh
source "${PODMAN_DIR}/lib/yaml-config.sh"
yaml_config_init "$1"
BUILD_IMAGE="$(yaml_require image.name)"
OUTPUT_DIR="${PROJECT_ROOT}/MallFrontend/unpackage/dist/build/web"

pass_count=0
pass() {
    pass_count=$((pass_count + 1))
    printf 'ok %d - %s\n' "$pass_count" "$1"
}

expect_status() {
    local expected="$1"
    shift
    local actual
    set +e
    "$@" >/dev/null 2>&1
    actual=$?
    set -e
    [[ "$actual" -eq "$expected" ]] || {
        printf 'Expected exit %s, got %s: %s\n' "$expected" "$actual" "$*" >&2
        exit 1
    }
}

bash -n "$BUILD_SCRIPT" "${PODMAN_DIR}/hbuilderx-build-entrypoint.sh"
pass 'build scripts pass bash syntax validation'

expect_status 2 "$BUILD_SCRIPT"
expect_status 2 "$BUILD_SCRIPT" "$1" unexpected-extra-argument
pass 'build command accepts exactly one YAML path'

build_log="$(mktemp)"
trap 'rm -f -- "$build_log"' EXIT
if ! "$BUILD_SCRIPT" "$1" >"$build_log" 2>&1; then
    tail -n 80 "$build_log" >&2
    exit 1
fi
tail -n 4 "$build_log"
pass 'Mall H5 builds in the configured image'

podman run --rm --pull=never --network=none \
    --entrypoint /bin/sh "$BUILD_IMAGE" -eu -c '
        . /etc/os-release
        test "$ID" = ubuntu
        test "$VERSION_ID" = 26.04
        test -x /opt/HBuilderX/plugins/node/node
        test -f /opt/HBuilderX/plugins/uniapp-cli-vite/node_modules/@dcloudio/vite-plugin-uni/bin/uni.js
        test ! -e /opt/HBuilderX/HBuilderX
        test ! -e /opt/HBuilderX/cli
        test ! -e /usr/bin/Xvfb
    '
pass 'image is Ubuntu 26.04 and contains no IDE, GUI CLI, or Xvfb'

[[ -s "$OUTPUT_DIR/index.html" && -d "$OUTPUT_DIR/assets" ]] || {
    printf 'Mall H5 output is incomplete: %s\n' "$OUTPUT_DIR" >&2
    exit 1
}
asset_count="$(find "$OUTPUT_DIR/assets" -type f | wc -l)"
[[ "$asset_count" -gt 0 ]] || {
    printf 'Mall H5 output contains no assets.\n' >&2
    exit 1
}
pass "output entry and ${asset_count} assets exist"

[[ "$(stat -c '%u' "$OUTPUT_DIR/index.html")" == "$(id -u)" ]] || {
    printf 'Mall H5 output is not owned by the current host user.\n' >&2
    exit 1
}
pass 'rootless container output is owned by the current host user'

git -C "$PROJECT_ROOT" check-ignore -q \
    MallFrontend/unpackage/dist/build/web/index.html
pass 'generated H5 output is ignored by Git'

printf '1..%d\n' "$pass_count"
