#!/usr/bin/env bash

set -Eeuo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
PODMAN_DIR="$(cd -- "${SCRIPT_DIR}/../.." && pwd)"
PROJECT_ROOT="$(cd -- "${PODMAN_DIR}/.." && pwd)"
BUILD_SCRIPT="${PODMAN_DIR}/compile.sh"

if [[ $# -ne 1 ]]; then
    printf 'Usage: bash ./run.sh <config.kdl>\n' >&2
    exit 2
fi

# shellcheck source=../../lib/kdl-config.sh
source "${PODMAN_DIR}/lib/kdl-config.sh"
kdl_config_init "$1"
BUILD_IMAGE="$(kdl_require image.hbuilderx)"
DEPENDENCY_IMAGE="$(kdl_require image.dependency)"
NODE_MODULES_VOLUME="$(kdl_require cache.mall_node_modules_volume)"
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

bash -n "$BUILD_SCRIPT" \
    "${PODMAN_DIR}/internal/hbuilderx-build-entrypoint.sh" \
    "${PODMAN_DIR}/internal/mall-dependencies-entrypoint.sh"
pass 'build scripts pass bash syntax validation'

expect_status 2 "$BUILD_SCRIPT"
expect_status 2 "$BUILD_SCRIPT" "$1" unexpected-extra-argument
pass 'build command accepts exactly one KDL path'

rg -q --fixed-strings -- \
    '--volume "$node_modules_volume:/workspace/MallFrontend/node_modules:rw"' \
    "$BUILD_SCRIPT"
rg -q --fixed-strings -- \
    '--entrypoint /workspace/podman/internal/mall-dependencies-entrypoint.sh' \
    "$BUILD_SCRIPT"
pass 'dependency install and H5 compile both use the Podman node_modules volume'

build_log="$(mktemp)"
trap 'rm -f -- "$build_log"' EXIT
if ! "$BUILD_SCRIPT" "$1" >"$build_log" 2>&1; then
    tail -n 80 "$build_log" >&2
    exit 1
fi
tail -n 4 "$build_log"
pass 'Mall dependencies install at container runtime and H5 builds offline'

podman run --rm --pull=never --network=none \
    --volume "$NODE_MODULES_VOLUME:/node_modules:ro" \
    --entrypoint /bin/sh "$DEPENDENCY_IMAGE" -eu -c \
    'test -d /node_modules/.pnpm && test -f /node_modules/dayjs/package.json'
pass 'runtime dependency volume contains Mall packages independently of the host directory'

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

missing_static_assets=''
while IFS= read -r asset; do
    source_asset="${PROJECT_ROOT}/MallFrontend${asset}"
    built_asset="${OUTPUT_DIR}${asset}"
    if [[ ! -f "$source_asset" || ! -f "$built_asset" ]]; then
        missing_static_assets+="${asset}"$'\n'
    fi
done < <(rg --no-filename --only-matching "['\"](/static/[^'\"?#]+\.(png|jpg|jpeg|gif|svg|webp|ico))" \
    "${PROJECT_ROOT}/MallFrontend/sheep/components/s-menu-tools/s-menu-tools.vue" \
    | sed -E "s/^[\"']//" | sort -u)
[[ -z "$missing_static_assets" ]] || {
    printf 'Referenced static assets are missing from source or build output:\n%s' "$missing_static_assets" >&2
    exit 1
}
pass 'shortcut menu static asset references exist in source and H5 build output'

[[ "$(stat -c '%u' "$OUTPUT_DIR/index.html")" == "$(id -u)" ]] || {
    printf 'Mall H5 output is not owned by the current host user.\n' >&2
    exit 1
}
pass 'rootless container output is owned by the current host user'

git -C "$PROJECT_ROOT" check-ignore -q \
    MallFrontend/unpackage/dist/build/web/index.html
pass 'generated H5 output is ignored by Git'

printf '1..%d\n' "$pass_count"
