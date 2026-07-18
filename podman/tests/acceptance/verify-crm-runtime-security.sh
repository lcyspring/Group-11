#!/usr/bin/env bash

set -Eeuo pipefail

PODMAN_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/../.." && pwd)"
PROJECT_ROOT="$(cd -- "${PODMAN_DIR}/.." && pwd)"

usage() {
    printf 'Usage: bash ./tests/acceptance/verify-crm-runtime-security.sh <runtime-config.yaml>\n' >&2
}

[[ $# -eq 1 ]] || {
    usage
    exit 2
}

# shellcheck source=../../lib/yaml-config.sh
source "${PODMAN_DIR}/lib/yaml-config.sh"
yaml_config_init "$1"

SERVER_PORT="$(yaml_port network.server_host_port)"
SERVER_CONTAINER="$(yaml_require container.server)"
MOCK_ENABLED="$(yaml_bool security.mock_login_enabled)"
BCRYPT_LENGTH="$(yaml_positive_integer security.password_encoder_length)"
XSS_ENABLED="$(yaml_bool security.xss_enabled)"
ALLOWED_ORIGINS="$(yaml_require security.cors_allowed_origins)"
ACTUATOR_EXPOSURE="$(yaml_require security.actuator_exposure)"
API_DOCS_ENABLED="$(yaml_bool security.api_docs_enabled)"
DRUID_ENABLED="$(yaml_bool security.druid_console_enabled)"
TDENGINE_USERNAME="$(yaml_require tdengine.username)"
TDENGINE_PASSWORD="$(yaml_require tdengine.password)"
BASE_URL="http://127.0.0.1:${SERVER_PORT}"
ALLOWED_ORIGIN="${ALLOWED_ORIGINS%%,*}"

require_command() {
    command -v "$1" >/dev/null 2>&1 || {
        printf 'Required command is unavailable: %s\n' "$1" >&2
        exit 1
    }
}

fail() {
    printf 'FAIL: %s\n' "$1" >&2
    exit 1
}

http_status() {
    curl --noproxy '*' --silent --show-error --output /dev/null --write-out '%{http_code}' \
        --max-time 15 "$@"
}

require_command curl
require_command jq
require_command podman

[[ "$MOCK_ENABLED" == "false" ]] || fail 'runtime mock login must be disabled'
((BCRYPT_LENGTH >= 10 && BCRYPT_LENGTH <= 16)) || fail 'BCrypt strength must be 10..16'
[[ "$XSS_ENABLED" == "true" ]] || fail 'runtime XSS filtering must be enabled'
[[ "$ACTUATOR_EXPOSURE" == "health,info" || "$ACTUATOR_EXPOSURE" == "info,health" ]] || \
    fail 'Actuator exposure must be limited to health and info'
[[ "$API_DOCS_ENABLED" == "false" ]] || fail 'API documentation must be disabled'
[[ "$DRUID_ENABLED" == "false" ]] || fail 'Druid console must be disabled'

health="$(curl --noproxy '*' --silent --show-error --fail --max-time 15 "${BASE_URL}/actuator/health")"
[[ "$(jq -r '.status' <<<"$health")" == "UP" ]] || fail 'server health is not UP'

endpoint_body="$(mktemp)"
allowed_headers="$(mktemp)"
denied_headers="$(mktemp)"
trap 'rm -f -- "$endpoint_body" "$allowed_headers" "$denied_headers"' EXIT
for path in /actuator/env /v3/api-docs /swagger-ui/index.html /druid/index.html; do
    status="$(curl --noproxy '*' --silent --show-error --output "$endpoint_body" --write-out '%{http_code}' \
        --max-time 15 "${BASE_URL}${path}")"
    if [[ "$status" == "301" || "$status" == "302" ]]; then
        fail "management endpoint redirects to exposed content: ${path} (${status})"
    fi
    if [[ "$status" == "200" ]] && ! jq -e '.code == 401 or .code == 403 or .code == 404' \
        "$endpoint_body" >/dev/null 2>&1; then
        fail "management endpoint is exposed: ${path} (${status})"
    fi
done

mock_status="$(curl --noproxy '*' --silent --show-error --output "$endpoint_body" --write-out '%{http_code}' \
    --max-time 15 \
    --header 'Authorization: Bearer test1' \
    --header 'tenant-id: 1' \
    "${BASE_URL}/admin-api/system/auth/get-permission-info")"
if [[ "$mock_status" != "401" ]] && ! { [[ "$mock_status" == "200" ]] && \
    jq -e '.code == 401' "$endpoint_body" >/dev/null 2>&1; }; then
    fail "legacy mock token was accepted or handled unexpectedly (${mock_status})"
fi

curl --noproxy '*' --silent --show-error --output /dev/null --dump-header "$allowed_headers" \
    --request OPTIONS \
    --header "Origin: ${ALLOWED_ORIGIN}" \
    --header 'Access-Control-Request-Method: POST' \
    "${BASE_URL}/admin-api/system/auth/login"
rg -qi "^Access-Control-Allow-Origin: ${ALLOWED_ORIGIN//./\\.}\r?$" "$allowed_headers" || \
    fail 'configured Web origin was not accepted by CORS'

curl --noproxy '*' --silent --show-error --output /dev/null --dump-header "$denied_headers" \
    --request OPTIONS \
    --header 'Origin: https://untrusted.invalid' \
    --header 'Access-Control-Request-Method: POST' \
    "${BASE_URL}/admin-api/system/auth/login"
if rg -qi '^Access-Control-Allow-Origin:' "$denied_headers"; then
    fail 'untrusted CORS origin received an allow-origin header'
fi

container_env="$(podman inspect --format '{{range .Config.Env}}{{println .}}{{end}}' "$SERVER_CONTAINER")"
for expected in \
    "MITEDTSM_SECURITY_MOCK_ENABLE=${MOCK_ENABLED}" \
    "MITEDTSM_SECURITY_PASSWORD_ENCODER_LENGTH=${BCRYPT_LENGTH}" \
    "MITEDTSM_XSS_ENABLE=${XSS_ENABLED}" \
    "MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE=${ACTUATOR_EXPOSURE}" \
    "SPRINGDOC_API_DOCS_ENABLED=${API_DOCS_ENABLED}" \
    "SPRING_DATASOURCE_DRUID_STAT_VIEW_SERVLET_ENABLED=${DRUID_ENABLED}" \
    "TDENGINE_USER=${TDENGINE_USERNAME}" \
    "TDENGINE_PASSWORD=${TDENGINE_PASSWORD}"; do
    rg -Fxq "$expected" <<<"$container_env" || fail "server container is missing explicit environment: ${expected%%=*}"
done

if ! awk '
    /^[[:space:]]*#/ { next }
    /^[[:space:]]*(api-key|secret-key|appKey|secretKey|request-key|response-key):/ {
        value = $0
        sub(/^[^:]+:[[:space:]]*/, "", value)
        sub(/[[:space:]]+#.*$/, "", value)
        if (value !~ /^\$\{[A-Z0-9_]+:/) {
            print FNR ":" $0
            bad = 1
        }
    }
    END { exit bad }
' "${PROJECT_ROOT}/Server/mitedtsm-server/src/main/resources/application.yaml"; then
    fail 'common application configuration still contains an inline integration secret'
fi

if ! awk '
    /^[[:space:]]*#/ { next }
    /^[[:space:]]*(password|secret|client-secret|tencent-lbs-key):/ {
        value = $0
        sub(/^[^:]+:[[:space:]]*/, "", value)
        sub(/[[:space:]]+#.*$/, "", value)
        if (value !~ /^\$\{[A-Z0-9_]+:/) {
            print FNR ":" $0
            bad = 1
        }
    }
    END { exit bad }
' "${PROJECT_ROOT}/Server/mitedtsm-server/src/main/resources/application-local.yaml"; then
    fail 'local application configuration still contains an inline credential'
fi

if rg -n "mock-enable:[[:space:]]*true|include:[[:space:]]*['\"]?\\*['\"]?|addAllowedOriginPattern\\(\"\\*\"\\)" \
    "${PROJECT_ROOT}/Server/mitedtsm-server/src/main/resources/application.yaml" \
    "${PROJECT_ROOT}/Server/mitedtsm-server/src/main/resources/application-local.yaml" \
    "${PROJECT_ROOT}/Server/mitedtsm-framework/mitedtsm-spring-boot-starter-web/src/main/java"; then
    fail 'unsafe wildcard or mock-login default remains in the active Podman source path'
fi

printf 'crm-runtime-security=ok\n'
printf 'health=UP hidden-management=4/4 mock-token=blocked cors=allowed+denied explicit-env=8/8\n'
