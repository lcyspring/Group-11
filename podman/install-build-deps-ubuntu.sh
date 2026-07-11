#!/usr/bin/env bash
# Install the host-side dependencies used to build and deploy this project on Ubuntu.
# This script deliberately does not build application artifacts or start Podman.

set -Eeuo pipefail

NODE_MAJOR="${NODE_MAJOR:-20}"
PNPM_VERSION="${PNPM_VERSION:-9.15.4}"
USE_HOST_PROXY="${USE_HOST_PROXY:-false}"

usage() {
    cat <<'EOF'
Usage: bash ./install-build-deps-ubuntu.sh

Installs the Ubuntu host-side requirements for podman/build-assets.sh:
  - OpenJDK 17 and Maven
  - Node.js 20 and pnpm 9
  - Podman, Pasta (`passt`), and rootless-storage helpers
  - native build prerequisites used by JavaScript dependencies

Optional environment variables:
  NODE_MAJOR=20    NodeSource major version to install (default: 20)
  PNPM_VERSION=9.15.4
  USE_HOST_PROXY=true  Allow apt, curl, and npm to use host proxy settings.

This script needs sudo when it is not run as root. It does not install
HBuilderX: build the Mall H5 artifact with HBuilderX separately. Proxy
settings are disabled by default.
EOF
}

if [[ "${1:-}" == "--help" || "${1:-}" == "-h" ]]; then
    usage
    exit 0
fi

if [[ $# -ne 0 ]]; then
    usage >&2
    exit 2
fi

if [[ ! -r /etc/os-release ]]; then
    printf 'Cannot identify the operating system: /etc/os-release is unavailable.\n' >&2
    exit 1
fi

# shellcheck disable=SC1091
source /etc/os-release
if [[ "${ID:-}" != "ubuntu" ]]; then
    printf 'This installer supports Ubuntu only; detected: %s.\n' "${PRETTY_NAME:-unknown}" >&2
    exit 1
fi

if [[ "$EUID" -eq 0 ]]; then
    SUDO=()
elif command -v sudo >/dev/null 2>&1; then
    SUDO=(sudo)
else
    printf 'sudo is required when the script is not run as root.\n' >&2
    exit 1
fi

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

run_root() {
    if [[ "$USE_HOST_PROXY" == "true" ]]; then
        "${SUDO[@]}" "$@"
    else
        "${SUDO[@]}" env -u http_proxy -u HTTP_PROXY -u https_proxy -u HTTPS_PROXY \
            -u all_proxy -u ALL_PROXY -u no_proxy -u NO_PROXY "$@"
    fi
}

run_host_command() {
    if [[ "$USE_HOST_PROXY" == "true" ]]; then
        "$@"
    else
        env -u http_proxy -u HTTP_PROXY -u https_proxy -u HTTPS_PROXY \
            -u all_proxy -u ALL_PROXY -u no_proxy -u NO_PROXY "$@"
    fi
}

node_major() {
    node --version 2>/dev/null | sed -E 's/^v([0-9]+).*/\1/'
}

pnpm_major() {
    pnpm --version 2>/dev/null | sed -E 's/^([0-9]+).*/\1/'
}

printf 'Installing Ubuntu build and Podman dependencies.\n'
run_root apt-get update
run_root env DEBIAN_FRONTEND=noninteractive apt-get install --yes --no-install-recommends \
    ca-certificates \
    curl \
    gnupg \
    openjdk-17-jdk \
    maven \
    podman \
    passt \
    uidmap \
    fuse-overlayfs \
    build-essential \
    python3

if ! command -v node >/dev/null 2>&1 || [[ "$(node_major)" -lt "$NODE_MAJOR" ]]; then
    keyring=/etc/apt/keyrings/nodesource.gpg
    source_list=/etc/apt/sources.list.d/nodesource.list

    printf 'Installing Node.js %s from NodeSource.\n' "$NODE_MAJOR"
    run_root install -d -m 0755 /etc/apt/keyrings
    run_host_command curl -fsSL https://deb.nodesource.com/gpgkey/nodesource-repo.gpg.key \
        | run_root gpg --dearmor --yes --output "$keyring"
    printf 'deb [signed-by=%s] https://deb.nodesource.com/node_%s.x nodistro main\n' \
        "$keyring" "$NODE_MAJOR" \
        | run_root tee "$source_list" >/dev/null
    run_root apt-get update
    run_root env DEBIAN_FRONTEND=noninteractive apt-get install --yes nodejs
fi

if ! command -v pnpm >/dev/null 2>&1 || [[ "$(pnpm_major)" -lt 9 ]]; then
    printf 'Installing pnpm %s.\n' "$PNPM_VERSION"
    run_root npm install --global "pnpm@${PNPM_VERSION}"
fi

printf '\nInstalled tool versions:\n'
java -version 2>&1 | sed -n '1,2p'
mvn --version | sed -n '1,3p'
node --version
pnpm --version
podman --version

cat <<'EOF'

HBuilderX CLI is not installed by this script. Install HBuilderX CLI 3.1.5+
separately, then run: bash ./build-mall-h5.sh --check
EOF
