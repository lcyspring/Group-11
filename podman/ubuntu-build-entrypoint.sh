#!/usr/bin/env bash

set -Eeuo pipefail

if [[ $# -ne 0 ]]; then
    printf 'The Ubuntu build entrypoint does not accept command-line arguments.\n' >&2
    exit 2
fi

bool_value() {
    case "${1,,}" in
        true|1|yes) printf 'true' ;;
        false|0|no|'') printf 'false' ;;
        *)
            printf 'Invalid boolean value: %s\n' "$1" >&2
            exit 2
            ;;
    esac
}

require_file() {
    [[ -s "$1" ]] || {
        printf 'Expected build output is missing or empty: %s\n' "$1" >&2
        exit 1
    }
}

verify_ubuntu_release() {
    # shellcheck disable=SC1091
    source /etc/os-release
    [[ "${ID:-}" == "ubuntu" && "${VERSION_ID:-}" == "26.04" ]] || {
        printf 'Build image must be Ubuntu 26.04; found %s %s.\n' "${ID:-unknown}" "${VERSION_ID:-unknown}" >&2
        exit 1
    }
}

run_without_proxy() {
    if [[ "$(bool_value "${BUILD_USE_HOST_PROXY:-false}")" == "true" ]]; then
        "$@"
    else
        env -u http_proxy -u HTTP_PROXY -u https_proxy -u HTTPS_PROXY \
            -u all_proxy -u ALL_PROXY -u no_proxy -u NO_PROXY "$@"
    fi
}

maven_goal() {
    local pom="$1"
    shift
    local args=(-f "$pom")
    if [[ -n "${BUILD_MAVEN_THREADS:-}" ]]; then
        args+=(-T "$BUILD_MAVEN_THREADS")
    fi
    run_without_proxy mvn "${args[@]}" "$@"
}

verify_ubuntu_release
printf 'Build OS: Ubuntu 26.04\n'
java -version
mvn -version
node --version
pnpm --version

BUILD_SERVER="$(bool_value "${BUILD_SERVER:-true}")"
BUILD_INIT_SERVICE="$(bool_value "${BUILD_INIT_SERVICE:-true}")"
BUILD_WEB="$(bool_value "${BUILD_WEB:-true}")"
BUILD_CLEAN="$(bool_value "${BUILD_CLEAN:-true}")"
BUILD_CRM_TESTS="$(bool_value "${BUILD_CRM_TESTS:-true}")"
BUILD_CRM_COVERAGE="$(bool_value "${BUILD_CRM_COVERAGE:-true}")"
PNPM_FROZEN_LOCKFILE="$(bool_value "${PNPM_FROZEN_LOCKFILE:-true}")"
BUILD_CI="$(bool_value "${BUILD_CI:-true}")"
PNPM_STORE_PATH="${PNPM_STORE_PATH:-/pnpm-store}"

if [[ "$BUILD_CRM_COVERAGE" == "true" && "$BUILD_CRM_TESTS" != "true" ]]; then
    printf 'CRM coverage requires CRM tests to be enabled.\n' >&2
    exit 2
fi

package_goal=(package -DskipTests)
if [[ "$BUILD_CLEAN" == "true" ]]; then
    package_goal=(clean "${package_goal[@]}")
fi

if [[ "$BUILD_SERVER" == "true" ]]; then
    printf 'Building Server inside Ubuntu 26.04.\n'
    maven_goal /workspace/Server/pom.xml -pl mitedtsm-server -am "${package_goal[@]}"
    require_file /workspace/Server/mitedtsm-server/target/mitedtsm-server.jar
fi

if [[ "$BUILD_INIT_SERVICE" == "true" ]]; then
    printf 'Building InitService inside Ubuntu 26.04.\n'
    maven_goal /workspace/InitService/pom.xml "${package_goal[@]}"
    require_file /workspace/InitService/target/mitedtsm-init-service.jar
fi

# Run CRM tests after the clean Server package. Otherwise Server clean removes
# the JaCoCo and Surefire evidence that this build is expected to preserve.
if [[ "$BUILD_CRM_TESTS" == "true" ]]; then
    printf 'Running CRM tests%s inside Ubuntu 26.04.\n' \
        "$([[ "$BUILD_CRM_COVERAGE" == "true" ]] && printf ' with JaCoCo' || true)"
    crm_test_args=(
        -pl mitedtsm-module-crm
        -am
        '-Dtest=Crm*Test'
        -Dsurefire.failIfNoSpecifiedTests=false
    )
    if [[ "$BUILD_CRM_COVERAGE" == "true" ]]; then
        crm_test_args+=(
            org.jacoco:jacoco-maven-plugin:0.8.13:prepare-agent
            test
            org.jacoco:jacoco-maven-plugin:0.8.13:report
        )
    else
        crm_test_args+=(test)
    fi
    maven_goal /workspace/Server/pom.xml "${crm_test_args[@]}"
    if [[ "$BUILD_CRM_COVERAGE" == "true" ]]; then
        require_file /workspace/Server/mitedtsm-module-crm/target/site/jacoco/jacoco.csv
    fi
fi

if [[ "$BUILD_WEB" == "true" ]]; then
    printf 'Building Web inside Ubuntu 26.04.\n'
    rm -rf /workspace/Web/dist-prod
    install_args=(--dir /workspace/Web --store-dir "$PNPM_STORE_PATH" install)
    if [[ "$PNPM_FROZEN_LOCKFILE" == "true" ]]; then
        install_args+=(--frozen-lockfile)
    fi
    if [[ "$BUILD_CI" == "true" ]]; then
        run_without_proxy env CI=true pnpm "${install_args[@]}"
        run_without_proxy env -u VITE_BASE_URL CI=true pnpm --dir /workspace/Web run build:prod
    else
        run_without_proxy pnpm "${install_args[@]}"
        run_without_proxy env -u VITE_BASE_URL pnpm --dir /workspace/Web run build:prod
    fi
    require_file /workspace/Web/dist-prod/index.html
fi

printf 'Ubuntu 26.04 build completed successfully.\n'
