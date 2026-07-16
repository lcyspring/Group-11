#!/usr/bin/env bash
# Build the Mall H5 frontend through the official HBuilderX CLI.
# HBuilderX 3.1.5+ is required for `publish --platform h5`.

set -Eeuo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd -- "${SCRIPT_DIR}/.." && pwd)"
MALL_DIR="${PROJECT_ROOT}/MallFrontend"
WEB_OUTPUT_DIR="${MALL_DIR}/unpackage/dist/build/web"
H5_OUTPUT_DIR="${MALL_DIR}/unpackage/dist/build/h5"

# `cli` is in the HBuilderX installation directory on Linux. Set this to an
# absolute executable path when it is not on PATH, for example:
# HBUILDERX_CLI=/opt/HBuilderX/cli
HBUILDERX_CLI="${HBUILDERX_CLI:-cli}"
HBUILDERX_PROJECT="${HBUILDERX_PROJECT:-${MALL_DIR}}"
# h5 works with HBuilderX 3.1.5+. `web` uses `cli publish web` and requires
# HBuilderX 4.67-alpha+.
HBUILDERX_PLATFORM="${HBUILDERX_PLATFORM:-h5}"
USE_HOST_PROXY="${USE_HOST_PROXY:-false}"
CHECK_ONLY=false

usage() {
    cat <<'EOF'
Usage: bash ./build-mall-h5.sh [--check]

Builds MallFrontend with the official HBuilderX CLI and leaves the static site
at MallFrontend/unpackage/dist/build/web/, the location consumed by Podman.

Requirements:
  - HBuilderX CLI 3.1.5+ (the CLI executable is named cli on Ubuntu)
  - The MallFrontend project source

Optional environment variables:
  HBUILDERX_CLI=/opt/HBuilderX/cli  Absolute path to the HBuilderX CLI.
  HBUILDERX_PROJECT=/path/to/project
                                    Project path or HBuilderX project name.
  HBUILDERX_PLATFORM=h5|web         h5 is the default; web needs 4.67-alpha+.
  USE_HOST_PROXY=true               Allow HBuilderX to use host proxy settings.

Options:
  --check   Verify the CLI and project only; do not issue a build.

For the default h5 mode, the script moves HBuilderX's build/h5 output to
build/web so that podman/Containerfile can consume it consistently.
EOF
}

run_host_command() {
    if [[ "$USE_HOST_PROXY" == "true" ]]; then
        "$@"
    else
        env -u http_proxy -u HTTP_PROXY -u https_proxy -u HTTPS_PROXY \
            -u all_proxy -u ALL_PROXY -u no_proxy -u NO_PROXY "$@"
    fi
}

run_cli() {
    run_host_command "$HBUILDERX_CLI" "$@"
}

cli_available() {
    if [[ "$HBUILDERX_CLI" == */* ]]; then
        [[ -x "$HBUILDERX_CLI" ]]
    else
        command -v "$HBUILDERX_CLI" >/dev/null 2>&1
    fi
}

normalize_output() {
    if [[ -f "${WEB_OUTPUT_DIR}/index.html" ]]; then
        return 0
    fi

    if [[ -f "${H5_OUTPUT_DIR}/index.html" ]]; then
        rm -rf "$WEB_OUTPUT_DIR"
        mv "$H5_OUTPUT_DIR" "$WEB_OUTPUT_DIR"
    fi

    [[ -f "${WEB_OUTPUT_DIR}/index.html" ]] || {
        printf 'HBuilderX completed without the expected H5 output. Expected: %s\n' \
            "${WEB_OUTPUT_DIR}/index.html" >&2
        exit 1
    }
}

case "$USE_HOST_PROXY" in
    true|TRUE|1|yes|YES)
        USE_HOST_PROXY=true
        ;;
    false|FALSE|0|no|NO|'')
        USE_HOST_PROXY=false
        ;;
    *)
        printf 'USE_HOST_PROXY must be true or false; got: %s\n' "$USE_HOST_PROXY" >&2
        exit 2
        ;;
esac

case "$HBUILDERX_PLATFORM" in
    h5|web)
        ;;
    *)
        printf 'HBUILDERX_PLATFORM must be h5 or web; got: %s\n' "$HBUILDERX_PLATFORM" >&2
        exit 2
        ;;
esac

case "${1:-}" in
    '')
        ;;
    --check)
        CHECK_ONLY=true
        ;;
    --help|-h)
        usage
        exit 0
        ;;
    *)
        usage >&2
        exit 2
        ;;
esac

[[ -d "$MALL_DIR" ]] || {
    printf 'MallFrontend directory is missing: %s\n' "$MALL_DIR" >&2
    exit 1
}

cli_available || {
    printf 'HBuilderX CLI was not found: %s\n' "$HBUILDERX_CLI" >&2
    printf 'Install HBuilderX CLI 3.1.5+ or set HBUILDERX_CLI to its executable path.\n' >&2
    exit 1
}

if [[ "$CHECK_ONLY" == true ]]; then
    run_cli ver
    printf 'HBuilderX CLI and MallFrontend source are available. No build was run.\n'
    exit 0
fi

printf 'Opening MallFrontend in HBuilderX CLI.\n'
run_cli open
run_cli project open --path "$MALL_DIR"

case "$HBUILDERX_PLATFORM" in
    h5)
        run_cli publish --platform h5 --project "$HBUILDERX_PROJECT"
        ;;
    web)
        run_cli publish web --project "$HBUILDERX_PROJECT"
        ;;
esac

normalize_output
printf 'Mall H5 assets are ready: %s\n' "$WEB_OUTPUT_DIR"
