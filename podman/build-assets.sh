#!/usr/bin/env bash
# Build the application artifacts consumed by podman/Containerfile.
# Use --check to report every missing prerequisite without building anything.

set -Eeuo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd -- "${SCRIPT_DIR}/.." && pwd)"
USE_HOST_PROXY="${USE_HOST_PROXY:-false}"

CHECK_ONLY=false
SKIP_MALL_CHECK=false
BUILD_MALL=false
JAVA_17_HOME=""
MISSING=()
MALL_BUILD_SCRIPT="${SCRIPT_DIR}/build-mall-h5.sh"

usage() {
    cat <<'EOF'
Usage: bash ./build-assets.sh [--check] [--build-mall] [--skip-mall-check]

Builds the artifacts consumed by podman/Containerfile:
  - Server/mitedtsm-server/target/mitedtsm-server.jar
  - InitService/target/mitedtsm-init-service.jar
  - Web/dist-prod/

Before building, all missing tools and required Mall H5 output are reported
together. --check only performs this preflight and never builds anything.

Options:
  --check             Report prerequisites only; do not install or build.
  --build-mall        Build Mall H5 through build-mall-h5.sh and HBuilderX CLI.
  --skip-mall-check   Do not require the manually issued Mall H5 artifact.
                       Podman up.sh still requires it before deployment.

Without --build-mall, the existing Mall output at
MallFrontend/unpackage/dist/build/web/ is required. --build-mall requires the
official HBuilderX CLI; set HBUILDERX_CLI when `cli` is not on PATH.

Proxy settings are disabled by default. Set USE_HOST_PROXY=true to allow Maven
and pnpm to use the host's proxy environment while building.
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

add_missing() {
    MISSING+=("$1")
}

java_major() {
    "$1" -version 2>&1 | awk -F'"' '/version/ { split($2, parts, "."); print parts[1] == "1" ? parts[2] : parts[1]; exit }'
}

find_java_17() {
    local candidate java_binary candidate_major
    local candidates=()

    if [[ -n "${JAVA_HOME:-}" ]]; then
        candidates+=("$JAVA_HOME")
    fi
    if command -v java >/dev/null 2>&1; then
        java_binary="$(readlink -f "$(command -v java)")"
        candidates+=("$(dirname "$(dirname "$java_binary")")")
    fi
    # Ubuntu uses java-17-openjdk-amd64; Arch/CachyOS uses the exact directory
    # /usr/lib/jvm/java-17-openjdk.
    candidates+=(/usr/lib/jvm/java-17-openjdk /usr/lib/jvm/java-17-openjdk-* /usr/lib/jvm/java-17-*)

    for candidate in "${candidates[@]}"; do
        [[ -x "$candidate/bin/java" && -x "$candidate/bin/javac" ]] || continue
        candidate_major="$(java_major "$candidate/bin/java")"
        if [[ "$candidate_major" == "17" ]]; then
            JAVA_17_HOME="$candidate"
            return 0
        fi
    done
    return 1
}

node_major() {
    node --version 2>/dev/null | sed -E 's/^v([0-9]+).*/\1/'
}

pnpm_major() {
    pnpm --version 2>/dev/null | sed -E 's/^([0-9]+).*/\1/'
}

check_prerequisites() {
    if ! find_java_17; then
        add_missing 'OpenJDK 17 (Ubuntu: openjdk-17-jdk; CachyOS/Arch: jdk17-openjdk; set JAVA_HOME if installed elsewhere)'
    fi

    if ! command -v mvn >/dev/null 2>&1; then
        add_missing 'Maven (install maven)'
    fi

    if ! command -v node >/dev/null 2>&1; then
        add_missing 'Node.js 18 or later (Node.js 20 is recommended)'
    elif [[ "$(node_major)" -lt 18 ]]; then
        add_missing "Node.js 18 or later (found $(node --version))"
    fi

    if ! command -v pnpm >/dev/null 2>&1; then
        add_missing 'pnpm 9 or later (install pnpm)'
    elif [[ "$(pnpm_major)" -lt 9 ]]; then
        add_missing "pnpm 9 or later (found $(pnpm --version))"
    fi

    if ! command -v podman >/dev/null 2>&1; then
        add_missing 'Podman (install podman for the deployment step)'
    fi

    if [[ "$BUILD_MALL" == true ]] && ! "$MALL_BUILD_SCRIPT" --check >/dev/null 2>&1; then
        add_missing 'HBuilderX CLI for Mall H5 (run build-mall-h5.sh --check for details)'
    elif [[ "$SKIP_MALL_CHECK" == false ]] && [[ ! -f "$PROJECT_ROOT/MallFrontend/unpackage/dist/build/web/index.html" ]]; then
        add_missing 'Mall H5 artifact: use HBuilderX to issue MallFrontend/unpackage/dist/build/web/index.html, or pass --skip-mall-check for an automated-assets-only build'
    fi
}

report_missing() {
    printf 'Cannot prepare Podman deployment assets. Missing prerequisites:\n' >&2
    local item
    for item in "${MISSING[@]}"; do
        printf '  - %s\n' "$item" >&2
    done
    if [[ -r /etc/os-release ]]; then
        # shellcheck disable=SC1091
        source /etc/os-release
        case "${ID:-}" in
            cachyos|arch)
                printf '\nOn CachyOS/Arch, install the automated prerequisites with:\n' >&2
                printf '  bash %s/install-build-deps-cachyos.sh\n' "$SCRIPT_DIR" >&2
                return
                ;;
            ubuntu)
                printf '\nOn Ubuntu, install the automated prerequisites with:\n' >&2
                printf '  bash %s/install-build-deps-ubuntu.sh\n' "$SCRIPT_DIR" >&2
                return
                ;;
        esac
    fi
    printf '\nInstall the missing prerequisites listed above, then rerun --check.\n' >&2
}

require_file() {
    local path="$1"
    [[ -s "$path" ]] || {
        printf 'Expected build output is missing or empty: %s\n' "$path" >&2
        exit 1
    }
}

require_dir() {
    local path="$1"
    [[ -d "$path" ]] || {
        printf 'Expected build output directory is missing: %s\n' "$path" >&2
        exit 1
    }
}

mvn_with_java_17() {
    run_host_command env JAVA_HOME="$JAVA_17_HOME" PATH="$JAVA_17_HOME/bin:$PATH" mvn "$@"
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

while [[ $# -gt 0 ]]; do
    case "$1" in
        --check)
            CHECK_ONLY=true
            ;;
        --skip-mall-check)
            SKIP_MALL_CHECK=true
            ;;
        --build-mall)
            BUILD_MALL=true
            ;;
        --help|-h)
            usage
            exit 0
            ;;
        *)
            printf 'Unknown option: %s\n' "$1" >&2
            usage >&2
            exit 2
            ;;
    esac
    shift
done

if [[ "$BUILD_MALL" == true && "$SKIP_MALL_CHECK" == true ]]; then
    printf '%s\n' '--build-mall and --skip-mall-check cannot be used together.' >&2
    exit 2
fi

check_prerequisites
if ((${#MISSING[@]})); then
    report_missing
    exit 1
fi

if [[ "$CHECK_ONLY" == true ]]; then
    printf 'All Podman build prerequisites are available. No build was run.\n'
    exit 0
fi

printf 'Building Server with OpenJDK 17.\n'
mvn_with_java_17 -f "$PROJECT_ROOT/Server/pom.xml" clean package -DskipTests
require_file "$PROJECT_ROOT/Server/mitedtsm-server/target/mitedtsm-server.jar"

printf 'Building InitService with OpenJDK 17.\n'
mvn_with_java_17 -f "$PROJECT_ROOT/InitService/pom.xml" clean package -DskipTests
require_file "$PROJECT_ROOT/InitService/target/mitedtsm-init-service.jar"

printf 'Installing Web dependencies and building the production frontend.\n'
run_host_command pnpm --dir "$PROJECT_ROOT/Web" install --no-frozen-lockfile
run_host_command pnpm --dir "$PROJECT_ROOT/Web" run build:prod
require_dir "$PROJECT_ROOT/Web/dist-prod"
require_file "$PROJECT_ROOT/Web/dist-prod/index.html"

if [[ "$BUILD_MALL" == true ]]; then
    printf 'Building Mall H5 with HBuilderX CLI.\n'
    USE_HOST_PROXY="$USE_HOST_PROXY" "$MALL_BUILD_SCRIPT"
fi
require_dir "$PROJECT_ROOT/MallFrontend/unpackage/dist/build/web"
require_file "$PROJECT_ROOT/MallFrontend/unpackage/dist/build/web/index.html"

printf '\nApplication assets are ready. Start the Pod with:\n'
printf '  cd %s && bash ./up.sh\n' "$SCRIPT_DIR"
