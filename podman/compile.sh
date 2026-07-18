#!/usr/bin/env bash

# Unified compilation entry point. The selected YAML explicitly chooses the
# Ubuntu 26.04 standard or HBuilderX toolchain; this dispatcher never guesses
# from file names, Host tools, or environment variables.

set -Eeuo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"

[[ $# -eq 1 ]] || {
    printf 'Usage: bash ./compile.sh <config.yaml>\n' >&2
    exit 2
}

# shellcheck source=lib/yaml-config.sh
source "${SCRIPT_DIR}/lib/yaml-config.sh"
yaml_config_init "$1"

[[ "$(yaml_require schema_version)" == "1" ]] || {
    printf 'Unsupported schema_version; expected 1.\n' >&2
    exit 2
}

ENGINE="$(yaml_require build.engine)"
case "$ENGINE" in
    standard)
        ENTRYPOINT="${SCRIPT_DIR}/internal/compile-standard.sh"
        ;;
    hbuilderx)
        ENTRYPOINT="${SCRIPT_DIR}/internal/compile-hbuilderx.sh"
        ;;
    *)
        printf 'build.engine must be standard or hbuilderx; got: %s\n' "$ENGINE" >&2
        exit 2
        ;;
esac

exec bash "$ENTRYPOINT" "$YAML_CONFIG_PATH"
