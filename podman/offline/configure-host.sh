#!/usr/bin/env bash

set -Eeuo pipefail

ROOT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"

[[ $# -eq 1 ]] || {
    printf 'Usage: ./configure-host.sh <public-ip-or-dns-name>\n' >&2
    exit 2
}

bash "${ROOT_DIR}/configure.sh" --host "$1"
