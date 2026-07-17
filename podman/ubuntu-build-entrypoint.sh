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
BUILD_ERP_TESTS="$(bool_value "${BUILD_ERP_TESTS:-false}")"
BUILD_ERP_COVERAGE="$(bool_value "${BUILD_ERP_COVERAGE:-false}")"
BUILD_INFRA_TESTS="$(bool_value "${BUILD_INFRA_TESTS:-false}")"
BUILD_INFRA_COVERAGE="$(bool_value "${BUILD_INFRA_COVERAGE:-false}")"
BUILD_BPM_TESTS="$(bool_value "${BUILD_BPM_TESTS:-false}")"
BUILD_BPM_COVERAGE="$(bool_value "${BUILD_BPM_COVERAGE:-false}")"
BUILD_COMMON_TESTS="$(bool_value "${BUILD_COMMON_TESTS:-false}")"
BUILD_COMMON_COVERAGE="$(bool_value "${BUILD_COMMON_COVERAGE:-false}")"
BUILD_COMMON_TEST_PATTERN="${BUILD_COMMON_TEST_PATTERN:-}"
BUILD_FRAMEWORK_TESTS="$(bool_value "${BUILD_FRAMEWORK_TESTS:-false}")"
BUILD_FRAMEWORK_COVERAGE="$(bool_value "${BUILD_FRAMEWORK_COVERAGE:-false}")"
BUILD_FRAMEWORK_TEST_PATTERN="${BUILD_FRAMEWORK_TEST_PATTERN:-}"
BUILD_SYSTEM_TESTS="$(bool_value "${BUILD_SYSTEM_TESTS:-false}")"
BUILD_SYSTEM_COVERAGE="$(bool_value "${BUILD_SYSTEM_COVERAGE:-false}")"
BUILD_SYSTEM_TEST_PATTERN="${BUILD_SYSTEM_TEST_PATTERN:-}"
PNPM_FROZEN_LOCKFILE="$(bool_value "${PNPM_FROZEN_LOCKFILE:-true}")"
BUILD_CI="$(bool_value "${BUILD_CI:-true}")"
PNPM_STORE_PATH="${PNPM_STORE_PATH:-/pnpm-store}"

if [[ "$BUILD_CRM_COVERAGE" == "true" && "$BUILD_CRM_TESTS" != "true" ]]; then
    printf 'CRM coverage requires CRM tests to be enabled.\n' >&2
    exit 2
fi
if [[ "$BUILD_ERP_COVERAGE" == "true" && "$BUILD_ERP_TESTS" != "true" ]]; then
    printf 'ERP coverage requires ERP tests to be enabled.\n' >&2
    exit 2
fi
if [[ "$BUILD_INFRA_COVERAGE" == "true" && "$BUILD_INFRA_TESTS" != "true" ]]; then
    printf 'Infra coverage requires Infra tests to be enabled.\n' >&2
    exit 2
fi
if [[ "$BUILD_BPM_COVERAGE" == "true" && "$BUILD_BPM_TESTS" != "true" ]]; then
    printf 'BPM coverage requires BPM tests to be enabled.\n' >&2
    exit 2
fi
if [[ "$BUILD_COMMON_COVERAGE" == "true" && "$BUILD_COMMON_TESTS" != "true" ]]; then
    printf 'Common coverage requires common tests to be enabled.\n' >&2
    exit 2
fi
if [[ "$BUILD_COMMON_TESTS" == "true" && ! "$BUILD_COMMON_TEST_PATTERN" =~ ^[A-Za-z0-9_.*?,]+$ ]]; then
    printf 'BUILD_COMMON_TEST_PATTERN is required for common tests and contains unsupported characters.\n' >&2
    exit 2
fi
if [[ "$BUILD_FRAMEWORK_COVERAGE" == "true" && "$BUILD_FRAMEWORK_TESTS" != "true" ]]; then
    printf 'Framework coverage requires framework tests to be enabled.\n' >&2
    exit 2
fi
if [[ "$BUILD_FRAMEWORK_TESTS" == "true" && ! "$BUILD_FRAMEWORK_TEST_PATTERN" =~ ^[A-Za-z0-9_.*?,]+$ ]]; then
    printf 'BUILD_FRAMEWORK_TEST_PATTERN is required for framework tests and contains unsupported characters.\n' >&2
    exit 2
fi
if [[ "$BUILD_SYSTEM_COVERAGE" == "true" && "$BUILD_SYSTEM_TESTS" != "true" ]]; then
    printf 'System coverage requires system tests to be enabled.\n' >&2
    exit 2
fi
if [[ "$BUILD_SYSTEM_TESTS" == "true" && ! "$BUILD_SYSTEM_TEST_PATTERN" =~ ^[A-Za-z0-9_.*?,]+$ ]]; then
    printf 'BUILD_SYSTEM_TEST_PATTERN is required for system tests and contains unsupported characters.\n' >&2
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

# Run module tests after the clean Server package. Otherwise Server clean removes
# the JaCoCo and Surefire evidence that this build is expected to preserve.
if [[ "$BUILD_COMMON_TESTS" == "true" ]]; then
    printf 'Running framework common tests%s inside Ubuntu 26.04.\n' \
        "$([[ "$BUILD_COMMON_COVERAGE" == "true" ]] && printf ' with JaCoCo' || true)"
    rm -f /workspace/Server/mitedtsm-framework/mitedtsm-common/target/jacoco.exec
    rm -rf /workspace/Server/mitedtsm-framework/mitedtsm-common/target/site/jacoco
    common_test_args=(
        -pl mitedtsm-framework/mitedtsm-common
        -am
        "-Dtest=${BUILD_COMMON_TEST_PATTERN}"
        -Dsurefire.failIfNoSpecifiedTests=false
    )
    if [[ "$BUILD_COMMON_COVERAGE" == "true" ]]; then
        common_test_args+=(
            org.jacoco:jacoco-maven-plugin:0.8.13:prepare-agent
            test
            org.jacoco:jacoco-maven-plugin:0.8.13:report
        )
    else
        common_test_args+=(test)
    fi
    maven_goal /workspace/Server/pom.xml "${common_test_args[@]}"
    if [[ "$BUILD_COMMON_COVERAGE" == "true" ]]; then
        require_file /workspace/Server/mitedtsm-framework/mitedtsm-common/target/site/jacoco/jacoco.csv
    fi
fi

if [[ "$BUILD_FRAMEWORK_TESTS" == "true" ]]; then
    printf 'Running framework security and web tests%s inside Ubuntu 26.04.\n' \
        "$([[ "$BUILD_FRAMEWORK_COVERAGE" == "true" ]] && printf ' with JaCoCo' || true)"
    framework_modules='mitedtsm-framework/mitedtsm-spring-boot-starter-security,mitedtsm-framework/mitedtsm-spring-boot-starter-web'
    framework_test_args=(
        -pl "$framework_modules"
        -am
        "-Dtest=${BUILD_FRAMEWORK_TEST_PATTERN}"
        -Dsurefire.failIfNoSpecifiedTests=false
    )
    if [[ "$BUILD_FRAMEWORK_COVERAGE" == "true" ]]; then
        framework_test_args+=(
            org.jacoco:jacoco-maven-plugin:0.8.13:prepare-agent
            test
            org.jacoco:jacoco-maven-plugin:0.8.13:report
        )
    else
        framework_test_args+=(test)
    fi
    maven_goal /workspace/Server/pom.xml "${framework_test_args[@]}"
    if [[ "$BUILD_FRAMEWORK_COVERAGE" == "true" ]]; then
        require_file /workspace/Server/mitedtsm-framework/mitedtsm-spring-boot-starter-security/target/site/jacoco/jacoco.csv
        require_file /workspace/Server/mitedtsm-framework/mitedtsm-spring-boot-starter-web/target/site/jacoco/jacoco.csv
    fi
fi

if [[ "$BUILD_SYSTEM_TESTS" == "true" ]]; then
    printf 'Running System module tests%s inside Ubuntu 26.04.\n' \
        "$([[ "$BUILD_SYSTEM_COVERAGE" == "true" ]] && printf ' with JaCoCo' || true)"
    rm -f /workspace/Server/mitedtsm-module-system/target/jacoco.exec
    rm -rf /workspace/Server/mitedtsm-module-system/target/site/jacoco
    system_test_args=(
        -pl mitedtsm-module-system
        -am
        "-Dtest=${BUILD_SYSTEM_TEST_PATTERN}"
        -Dsurefire.failIfNoSpecifiedTests=false
    )
    if [[ "$BUILD_SYSTEM_COVERAGE" == "true" ]]; then
        system_test_args+=(
            org.jacoco:jacoco-maven-plugin:0.8.13:prepare-agent
            test
            org.jacoco:jacoco-maven-plugin:0.8.13:report
        )
    else
        system_test_args+=(test)
    fi
    maven_goal /workspace/Server/pom.xml "${system_test_args[@]}"
    if [[ "$BUILD_SYSTEM_COVERAGE" == "true" ]]; then
        require_file /workspace/Server/mitedtsm-module-system/target/site/jacoco/jacoco.csv
    fi
fi

if [[ "$BUILD_INFRA_TESTS" == "true" ]]; then
    printf 'Running Infra file tests%s inside Ubuntu 26.04.\n' \
        "$([[ "$BUILD_INFRA_COVERAGE" == "true" ]] && printf ' with JaCoCo' || true)"
    infra_test_args=(
        -pl mitedtsm-module-infra
        -am
        '-Dtest=File*Test'
        -Dsurefire.failIfNoSpecifiedTests=false
    )
    if [[ "$BUILD_INFRA_COVERAGE" == "true" ]]; then
        infra_test_args+=(
            -Djacoco.propertyName=unusedJacocoArgLine
            '-DargLine=-javaagent:/root/.m2/repository/org/jacoco/org.jacoco.agent/0.8.13/org.jacoco.agent-0.8.13-runtime.jar=destfile=/workspace/Server/mitedtsm-module-infra/target/jacoco.exec,excludes=net/sf/jsqlparser/**'
            org.jacoco:jacoco-maven-plugin:0.8.13:prepare-agent
            test
            org.jacoco:jacoco-maven-plugin:0.8.13:report
        )
    else
        infra_test_args+=(test)
    fi
    maven_goal /workspace/Server/pom.xml "${infra_test_args[@]}"
    if [[ "$BUILD_INFRA_COVERAGE" == "true" ]]; then
        require_file /workspace/Server/mitedtsm-module-infra/target/site/jacoco/jacoco.csv
    fi
fi

if [[ "$BUILD_BPM_TESTS" == "true" ]]; then
    printf 'Running BPM tests%s inside Ubuntu 26.04.\n' \
        "$([[ "$BUILD_BPM_COVERAGE" == "true" ]] && printf ' with JaCoCo' || true)"
    bpm_test_args=(
        -pl mitedtsm-module-bpm
        -am
        '-Dtest=Bpm*Test'
        -Dsurefire.failIfNoSpecifiedTests=false
    )
    if [[ "$BUILD_BPM_COVERAGE" == "true" ]]; then
        bpm_test_args+=(
            -Djacoco.propertyName=unusedJacocoArgLine
            '-DargLine=-javaagent:/root/.m2/repository/org/jacoco/org.jacoco.agent/0.8.13/org.jacoco.agent-0.8.13-runtime.jar=destfile=/workspace/Server/mitedtsm-module-bpm/target/jacoco.exec,excludes=net/sf/jsqlparser/**'
            org.jacoco:jacoco-maven-plugin:0.8.13:prepare-agent
            test
            org.jacoco:jacoco-maven-plugin:0.8.13:report
        )
    else
        bpm_test_args+=(test)
    fi
    maven_goal /workspace/Server/pom.xml "${bpm_test_args[@]}"
    if [[ "$BUILD_BPM_COVERAGE" == "true" ]]; then
        require_file /workspace/Server/mitedtsm-module-bpm/target/site/jacoco/jacoco.csv
    fi
fi

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

if [[ "$BUILD_ERP_TESTS" == "true" ]]; then
    printf 'Running ERP tests%s inside Ubuntu 26.04.\n' \
        "$([[ "$BUILD_ERP_COVERAGE" == "true" ]] && printf ' with JaCoCo' || true)"
    erp_test_args=(
        -pl mitedtsm-module-erp
        -am
        '-Dtest=Erp*Test'
        -Dsurefire.failIfNoSpecifiedTests=false
    )
    if [[ "$BUILD_ERP_COVERAGE" == "true" ]]; then
        erp_test_args+=(
            org.jacoco:jacoco-maven-plugin:0.8.13:prepare-agent
            test
            org.jacoco:jacoco-maven-plugin:0.8.13:report
        )
    else
        erp_test_args+=(test)
    fi
    maven_goal /workspace/Server/pom.xml "${erp_test_args[@]}"
    if [[ "$BUILD_ERP_COVERAGE" == "true" ]]; then
        require_file /workspace/Server/mitedtsm-module-erp/target/site/jacoco/jacoco.csv
    fi
fi

if [[ "$BUILD_WEB" == "true" || -n "${WEB_TEST_SCRIPT:-}" ]]; then
    if [[ "$BUILD_WEB" == "true" ]]; then
        printf 'Building Web inside Ubuntu 26.04.\n'
        rm -rf /workspace/Web/dist-prod
    else
        printf 'Testing Web inside Ubuntu 26.04.\n'
    fi
    install_args=(--dir /workspace/Web --store-dir "$PNPM_STORE_PATH" install)
    if [[ "$PNPM_FROZEN_LOCKFILE" == "true" ]]; then
        install_args+=(--frozen-lockfile)
    fi
    if [[ "$BUILD_CI" == "true" ]]; then
        run_without_proxy env CI=true pnpm "${install_args[@]}"
        if [[ -n "${WEB_TEST_SCRIPT:-}" ]]; then
            run_without_proxy env CI=true pnpm --dir /workspace/Web run "$WEB_TEST_SCRIPT"
        fi
        if [[ "$BUILD_WEB" == "true" ]]; then
            run_without_proxy env -u VITE_BASE_URL CI=true pnpm --dir /workspace/Web run build:prod
        fi
    else
        run_without_proxy pnpm "${install_args[@]}"
        if [[ -n "${WEB_TEST_SCRIPT:-}" ]]; then
            run_without_proxy pnpm --dir /workspace/Web run "$WEB_TEST_SCRIPT"
        fi
        if [[ "$BUILD_WEB" == "true" ]]; then
            run_without_proxy env -u VITE_BASE_URL pnpm --dir /workspace/Web run build:prod
        fi
    fi
    if [[ "$BUILD_WEB" == "true" ]]; then
        require_file /workspace/Web/dist-prod/index.html
    fi
fi

printf 'Ubuntu 26.04 build completed successfully.\n'
