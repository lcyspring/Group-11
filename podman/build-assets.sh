#!/usr/bin/env bash
# Build the application artifacts consumed by podman/Containerfile.
# Use --check to report every missing prerequisite without building anything.

set -Eeuo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd -- "${SCRIPT_DIR}/.." && pwd)"
USE_HOST_PROXY="${USE_HOST_PROXY:-false}"
# A VMware shared folder (and some network filesystems) cannot create the
# symlinks used by pnpm's default node_modules layout. Leave this empty to
# detect that case automatically; set it to a native, writable directory to
# force staging there.
WEB_BUILD_WORKDIR="${WEB_BUILD_WORKDIR:-}"

CHECK_ONLY=false
SKIP_MALL_CHECK=false
BUILD_MALL=false
WEB_ONLY=false
JAVA_17_HOME=""
MISSING=()
MALL_BUILD_SCRIPT="${SCRIPT_DIR}/build-mall-h5.sh"
WEB_BUILD_DIR=""
WEB_BUILD_STAGING_DIR=""
WEB_BUILD_INPUT_FILES=(
    '.env'
    '.env.prod'
    'package.json'
    'pnpm-lock.yaml'
    'vite.config.ts'
    'build/vite/index.ts'
    'build/vite/optimize.ts'
)

usage() {
    cat <<'EOF'
Usage: bash ./build-assets.sh [--check] [--web-only] [--build-mall] [--skip-mall-check]

Builds the artifacts consumed by podman/Containerfile:
  - Server/mitedtsm-server/target/mitedtsm-server.jar
  - InitService/target/mitedtsm-init-service.jar
  - Web/dist-prod/

Before building, all missing tools and required Mall H5 output are reported
together. --check only performs this preflight and never builds anything.

Options:
  --check             Report prerequisites only; do not install or build.
  --web-only          Build only Web/dist-prod/. Use only when the deployed
                      Server and InitService JARs are confirmed unchanged.
  --build-mall        Build Mall H5 through build-mall-h5.sh and HBuilderX CLI.
  --skip-mall-check   Do not require the manually issued Mall H5 artifact.
                       Podman up.sh still requires it before deployment.

Without --build-mall, the existing Mall output at
MallFrontend/unpackage/dist/build/web/ is required. --build-mall requires the
official HBuilderX CLI; set HBUILDERX_CLI when `cli` is not on PATH.

Proxy settings are disabled by default. Set USE_HOST_PROXY=true to allow Maven
and pnpm to use the host's proxy environment while building.

On filesystems without symbolic-link support (for example VMware hgfs), the
Web source is staged automatically on a native filesystem and only dist-prod
is copied back. Set WEB_BUILD_WORKDIR=/native/writable/path to choose that
staging location explicitly.
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
    local web_build_input

    if [[ "$WEB_ONLY" == false ]]; then
        if ! find_java_17; then
            add_missing 'OpenJDK 17 (Ubuntu: openjdk-17-jdk; CachyOS/Arch: jdk17-openjdk; set JAVA_HOME if installed elsewhere)'
        fi

        if ! command -v mvn >/dev/null 2>&1; then
            add_missing 'Maven (install maven)'
        fi
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

    if ! command -v tar >/dev/null 2>&1; then
        add_missing 'tar (required to stage the Web build on filesystems without symbolic-link support)'
    fi

    for web_build_input in "${WEB_BUILD_INPUT_FILES[@]}"; do
        if [[ ! -f "$PROJECT_ROOT/Web/$web_build_input" ]]; then
            add_missing "Web build input: Web/$web_build_input (the repository checkout is incomplete; update it before building)"
        fi
    done

    if [[ "$WEB_ONLY" == false ]] && ! command -v podman >/dev/null 2>&1; then
        add_missing 'Podman (install podman for the deployment step)'
    fi

    if [[ "$WEB_ONLY" == false ]]; then
        if [[ "$BUILD_MALL" == true ]] && ! "$MALL_BUILD_SCRIPT" --check >/dev/null 2>&1; then
            add_missing 'HBuilderX CLI for Mall H5 (run build-mall-h5.sh --check for details)'
        elif [[ "$SKIP_MALL_CHECK" == false ]] && [[ ! -f "$PROJECT_ROOT/MallFrontend/unpackage/dist/build/web/index.html" ]]; then
            add_missing 'Mall H5 artifact: use HBuilderX to issue MallFrontend/unpackage/dist/build/web/index.html, or pass --skip-mall-check for an automated-assets-only build'
        fi
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

supports_symlinks() {
    local directory="$1"
    local probe_dir

    [[ -d "$directory" && -w "$directory" ]] || return 1
    probe_dir="$(mktemp -d "${directory%/}/.podman-symlink-probe.XXXXXX" 2>/dev/null)" || return 1

    if ln -s target "${probe_dir}/link" 2>/dev/null; then
        rm -rf -- "$probe_dir" || true
        return 0
    fi

    rm -rf -- "$probe_dir" || true
    return 1
}

cleanup_web_build_staging_dir() {
    if [[ -n "$WEB_BUILD_STAGING_DIR" && -d "$WEB_BUILD_STAGING_DIR" ]]; then
        rm -rf -- "$WEB_BUILD_STAGING_DIR" || true
    fi
}

create_web_build_staging_dir() {
    local base
    local -a candidates=()

    if [[ -n "$WEB_BUILD_WORKDIR" ]]; then
        candidates+=("$WEB_BUILD_WORKDIR")
    fi
    if [[ -n "${TMPDIR:-}" && "${TMPDIR:-}" != "$WEB_BUILD_WORKDIR" ]]; then
        candidates+=("$TMPDIR")
    fi
    if [[ "/tmp" != "$WEB_BUILD_WORKDIR" && "/tmp" != "${TMPDIR:-}" ]]; then
        candidates+=(/tmp)
    fi

    for base in "${candidates[@]}"; do
        [[ -d "$base" && -w "$base" ]] || continue
        supports_symlinks "$base" || continue
        mktemp -d "${base%/}/mitedtsm-web-build.XXXXXX" && return 0
    done
    return 1
}

prepare_web_build_dir() {
    local web_source="$PROJECT_ROOT/Web"
    local source_supports_symlinks=false

    if supports_symlinks "$web_source"; then
        source_supports_symlinks=true
    fi

    if [[ -z "$WEB_BUILD_WORKDIR" && "$source_supports_symlinks" == true ]]; then
        WEB_BUILD_DIR="$web_source"
        return
    fi

    WEB_BUILD_STAGING_DIR="$(create_web_build_staging_dir)" || {
        printf '%s\n' 'Web source does not support pnpm symlinks, and no native writable staging directory is available.' >&2
        printf '%s\n' 'Set WEB_BUILD_WORKDIR to a writable local filesystem, for example WEB_BUILD_WORKDIR=/tmp.' >&2
        exit 1
    }
    WEB_BUILD_DIR="$WEB_BUILD_STAGING_DIR"
    trap cleanup_web_build_staging_dir EXIT

    if [[ "$source_supports_symlinks" == true ]]; then
        printf 'Building Web in requested native workspace %s.\n' "$WEB_BUILD_DIR"
    else
        printf 'Web source does not support pnpm symlinks; building in native workspace %s.\n' "$WEB_BUILD_DIR"
    fi
    tar --exclude='./node_modules' --exclude='./dist-prod' \
        -C "$web_source" -cf - . | tar -C "$WEB_BUILD_DIR" -xf -
}

publish_web_build_output() {
    local web_source="$PROJECT_ROOT/Web"

    [[ "$WEB_BUILD_DIR" == "$web_source" ]] && return

    rm -rf -- "$web_source/dist-prod"
    cp -a "$WEB_BUILD_DIR/dist-prod" "$web_source/dist-prod"
}

clear_web_build_output() {
    local web_output="$PROJECT_ROOT/Web/dist-prod"

    # A failed Vite run must not leave a previous build available for up.sh to
    # package. Otherwise index.html and assets/ from different builds can be
    # deployed together.
    rm -rf -- "$web_output"
    if [[ "$WEB_BUILD_DIR" != "$PROJECT_ROOT/Web" ]]; then
        rm -rf -- "$WEB_BUILD_DIR/dist-prod"
    fi
}

verify_web_entry_assets() {
    local web_output="$1"
    local entry_html="${web_output}/index.html"
    local asset_path
    local found_asset=false

    while IFS= read -r asset_path; do
        [[ -n "$asset_path" ]] || continue
        found_asset=true
        if [[ ! -s "${web_output}${asset_path}" ]]; then
            printf 'Web entry references a missing asset: %s%s\n' "$web_output" "$asset_path" >&2
            printf '%s\n' 'Rebuild Web successfully before running up.sh; do not deploy this output.' >&2
            exit 1
        fi
    done < <(sed -nE 's/.*(src|href)="(\/assets\/[^"?]+)(\?[^" ]*)?".*/\2/p' "$entry_html" | sort -u)

    if [[ "$found_asset" == false ]]; then
        printf 'Web entry does not reference any hashed assets: %s\n' "$entry_html" >&2
        exit 1
    fi
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
        --web-only)
            WEB_ONLY=true
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

if [[ "$WEB_ONLY" == true && ( "$BUILD_MALL" == true || "$SKIP_MALL_CHECK" == true ) ]]; then
    printf '%s\n' '--web-only cannot be combined with --build-mall or --skip-mall-check.' >&2
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

if [[ "$WEB_ONLY" == false ]]; then
    printf 'Building Server with OpenJDK 17.\n'
    mvn_with_java_17 -f "$PROJECT_ROOT/Server/pom.xml" clean package -DskipTests
    require_file "$PROJECT_ROOT/Server/mitedtsm-server/target/mitedtsm-server.jar"

    printf 'Building InitService with OpenJDK 17.\n'
    mvn_with_java_17 -f "$PROJECT_ROOT/InitService/pom.xml" clean package -DskipTests
    require_file "$PROJECT_ROOT/InitService/target/mitedtsm-init-service.jar"
fi

printf 'Installing Web dependencies and building the production frontend.\n'
prepare_web_build_dir
clear_web_build_output
run_host_command pnpm --dir "$WEB_BUILD_DIR" install --frozen-lockfile
# Podman publishes the Web frontend for remote browsers, so its production API
# endpoint must remain same-origin (/admin-api). Do not let an exported local
# development VITE_BASE_URL override the empty value in .env.prod.
run_host_command env -u VITE_BASE_URL pnpm --dir "$WEB_BUILD_DIR" run build:prod
publish_web_build_output
require_dir "$PROJECT_ROOT/Web/dist-prod"
require_file "$PROJECT_ROOT/Web/dist-prod/index.html"
verify_web_entry_assets "$PROJECT_ROOT/Web/dist-prod"

if [[ "$WEB_ONLY" == false ]]; then
    if [[ "$BUILD_MALL" == true ]]; then
        printf 'Building Mall H5 with HBuilderX CLI.\n'
        USE_HOST_PROXY="$USE_HOST_PROXY" "$MALL_BUILD_SCRIPT"
    fi
    require_dir "$PROJECT_ROOT/MallFrontend/unpackage/dist/build/web"
    require_file "$PROJECT_ROOT/MallFrontend/unpackage/dist/build/web/index.html"
fi

if [[ "$WEB_ONLY" == true ]]; then
    printf '\nWeb production assets are ready. Existing Java artifacts were not rebuilt.\n'
else
    printf '\nApplication assets are ready. Start the Pod with:\n'
    printf '  cd %s && bash ./up.sh\n' "$SCRIPT_DIR"
fi
