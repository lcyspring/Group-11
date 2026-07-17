#!/usr/bin/env bash
# Install the host-side dependencies used to build and deploy this project on
# CachyOS and other Arch-based distributions. It never builds application
# artifacts or starts Podman.

set -Eeuo pipefail

USE_HOST_PROXY="${USE_HOST_PROXY:-false}"
CHECK_ONLY=false

usage() {
    cat <<'EOF'
Usage: bash ./install-build-deps-cachyos.sh [--check]

Installs the CachyOS/Arch host-side requirements for rootless Podman workflows:
  - OpenJDK 17 and Maven
  - Node.js and pnpm
  - Podman with rootless networking/storage helpers
  - native build prerequisites used by JavaScript dependencies

Options:
  --check              Report missing packages only; do not install anything.

Optional environment variables:
  USE_HOST_PROXY=true  Allow pacman to use host proxy settings.

The install performs a normal full pacman upgrade before installing packages,
which avoids unsafe partial upgrades on Arch-based systems. HBuilderX CLI is
not packaged here; it is only needed to regenerate the Mall H5 artifact.
EOF
}

case "${1:-}" in
    '') ;;
    --check)
        CHECK_ONLY=true
        ;;
    -h|--help)
        usage
        exit 0
        ;;
    *)
        usage >&2
        exit 2
        ;;
esac

if [[ $# -gt 1 ]]; then
    usage >&2
    exit 2
fi

[[ -r /etc/os-release ]] || {
    printf 'Cannot identify the operating system: /etc/os-release is unavailable.\n' >&2
    exit 1
}

# shellcheck disable=SC1091
source /etc/os-release
if [[ "${ID:-}" != "cachyos" && " ${ID_LIKE:-} " != *" arch "* && "${ID:-}" != "arch" ]]; then
    printf 'This installer supports CachyOS/Arch only; detected: %s.\n' "${PRETTY_NAME:-unknown}" >&2
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

if [[ "$EUID" -eq 0 ]]; then
    SUDO=()
elif command -v sudo >/dev/null 2>&1; then
    SUDO=(sudo)
else
    printf 'sudo is required when the script is not run as root.\n' >&2
    exit 1
fi

run_root() {
    if [[ "$USE_HOST_PROXY" == "true" ]]; then
        "${SUDO[@]}" "$@"
    else
        "${SUDO[@]}" env -u http_proxy -u HTTP_PROXY -u https_proxy -u HTTPS_PROXY \
            -u all_proxy -u ALL_PROXY -u no_proxy -u NO_PROXY "$@"
    fi
}

PACKAGES=(
    ca-certificates
    curl
    git
    base-devel
    python
    jdk17-openjdk
    maven
    nodejs
    pnpm
    podman
    passt
    shadow
)

MISSING=()
for package in "${PACKAGES[@]}"; do
    pacman -Q "$package" >/dev/null 2>&1 || MISSING+=("$package")
done

if [[ "$CHECK_ONLY" == true ]]; then
    if ((${#MISSING[@]})); then
        printf 'Missing CachyOS/Arch packages:\n'
        printf '  - %s\n' "${MISSING[@]}"
        exit 1
    fi
    printf 'All CachyOS/Arch build and rootless-Podman packages are installed.\n'
    exit 0
fi

printf 'Updating CachyOS/Arch packages and installing build dependencies.\n'
run_root pacman --sync --refresh --sysupgrade --needed --noconfirm "${PACKAGES[@]}"

printf '\nInstalled tool versions:\n'
java -version 2>&1 | sed -n '1,2p'
mvn --version | sed -n '1,3p'
node --version
pnpm --version
podman --version

if command -v archlinux-java >/dev/null 2>&1; then
    printf '\nAvailable Java environments:\n'
    archlinux-java status
fi

cat <<'EOF'

Mall H5 is built in the dedicated Ubuntu image:
bash ./build-mall-h5-in-ubuntu.sh ./config/build-mall-h5-ubuntu-26.04.yaml
EOF
