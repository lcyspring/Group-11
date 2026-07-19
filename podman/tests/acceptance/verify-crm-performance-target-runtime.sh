#!/usr/bin/env bash
# Verify performance-target schema, permissions and read APIs using one explicit KDL configuration.

set -Eeuo pipefail
trap 'printf "CRM performance-target runtime acceptance failed at line %s.\n" "$LINENO" >&2' ERR

PODMAN_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/../.." && pwd)"
source "${PODMAN_DIR}/lib/kdl-config.sh"

[[ $# -eq 1 ]] || {
    printf 'Usage: bash ./tests/acceptance/verify-crm-performance-target-runtime.sh <config.kdl>\n' >&2
    exit 2
}
kdl_config_init "$1"
[[ "$(kdl_require schema_version)" == "1" ]] || {
    printf 'Unsupported schema_version.\n' >&2
    exit 2
}

BASE_URL="$(kdl_require endpoint.base_url)"
TENANT_ID="$(kdl_positive_integer endpoint.tenant_id)"
USERNAME="$(kdl_require account.username)"
PASSWORD="$(kdl_require account.password)"
MYSQL_CONTAINER="$(kdl_require mysql.container)"
MYSQL_USER="$(kdl_require mysql.user)"
MYSQL_PASSWORD="$(kdl_require mysql.password)"
MYSQL_DATABASE="$(kdl_require mysql.database)"
SCOPE_TYPE="$(kdl_positive_integer acceptance.scope_type)"
SCOPE_ID="$(kdl_require acceptance.scope_id)"
DEPARTMENT_ID="$(kdl_positive_integer acceptance.department_id)"
TARGET_YEAR="$(kdl_positive_integer acceptance.target_year)"
TARGET_TYPE="$(kdl_positive_integer acceptance.target_type)"
EXPECTED_PERMISSION_COUNT="$(kdl_positive_integer acceptance.expected_permission_count)"
EXPECTED_I18N_COUNT="$(kdl_positive_integer acceptance.expected_i18n_count)"
START_TIME="$(kdl_require acceptance.start_time)"
END_TIME="$(kdl_require acceptance.end_time)"

[[ "$BASE_URL" =~ ^https?://[^[:space:]]+$ && "$SCOPE_ID" =~ ^[0-9]+$ ]] || {
    printf 'Invalid endpoint.base_url or acceptance.scope_id.\n' >&2
    exit 2
}
[[ "$MYSQL_CONTAINER" =~ ^[a-zA-Z0-9_.-]+$ && "$MYSQL_USER" =~ ^[a-zA-Z0-9_.-]+$ \
    && "$MYSQL_DATABASE" =~ ^[a-zA-Z0-9_]+$ ]] || {
    printf 'Invalid MySQL identifier.\n' >&2
    exit 2
}
timestamp_pattern='^[0-9]{4}-[0-9]{2}-[0-9]{2} [0-9]{2}:[0-9]{2}:[0-9]{2}$'
[[ "$START_TIME" =~ $timestamp_pattern && "$END_TIME" =~ $timestamp_pattern \
    && "$START_TIME" < "$END_TIME" ]] || {
    printf 'Acceptance timestamps must be ordered YYYY-MM-DD HH:MM:SS values.\n' >&2
    exit 2
}
for command in curl jq podman grep; do
    command -v "$command" >/dev/null || {
        printf 'Missing command: %s\n' "$command" >&2
        exit 1
    }
done

mysql_exec() {
    podman exec --env "MYSQL_PWD=${MYSQL_PASSWORD}" "$MYSQL_CONTAINER" mysql "-u${MYSQL_USER}" \
        "--database=${MYSQL_DATABASE}" --default-character-set=utf8mb4 -Nse "$1"
}

grep -Fx '../migrations/new-crm-performance-target.sql' \
    "${PODMAN_DIR}/../database/manifests/mysql-compatibility.manifest" >/dev/null || {
    printf 'Performance-target migration is absent from the compatibility manifest.\n' >&2
    exit 1
}

TABLE_COUNT="$(mysql_exec "SELECT COUNT(*) FROM information_schema.tables
  WHERE table_schema=DATABASE() AND table_name='crm_performance_target';")"
[[ "$TABLE_COUNT" == "1" ]] || {
    printf 'crm_performance_target table is missing.\n' >&2
    exit 1
}
PERMISSION_COUNT="$(mysql_exec "SELECT COUNT(*) FROM system_menu WHERE deleted=b'0'
  AND permission IN ('crm:performance-target:update','crm:performance-target:delete');")"
[[ "$PERMISSION_COUNT" == "$EXPECTED_PERMISSION_COUNT" ]] || {
    printf 'Expected %s performance-target permissions, got %s.\n' \
        "$EXPECTED_PERMISSION_COUNT" "$PERMISSION_COUNT" >&2
    exit 1
}
I18N_COUNT="$(mysql_exec "SELECT COUNT(*) FROM system_menu_i18n i
  JOIN system_menu m ON m.id=i.menu_id AND m.deleted=b'0'
  WHERE i.deleted=b'0' AND m.permission IN
    ('crm:performance-target:update','crm:performance-target:delete');")"
[[ "$I18N_COUNT" == "$EXPECTED_I18N_COUNT" ]] || {
    printf 'Expected %s performance-target translations, got %s.\n' \
        "$EXPECTED_I18N_COUNT" "$I18N_COUNT" >&2
    exit 1
}

login_response="$(curl --noproxy '*' --fail --silent --show-error --retry 5 --retry-all-errors --retry-delay 1 \
    --header 'Content-Type: application/json' --header "tenant-id: ${TENANT_ID}" \
    --data "$(jq -n --arg username "$USERNAME" --arg password "$PASSWORD" \
        '{username:$username,password:$password,captchaVerification:""}')" \
    "${BASE_URL}/system/auth/login")"
jq -e '.code == 0 and (.data.accessToken | length > 0)' >/dev/null <<< "$login_response" || {
    printf 'CRM acceptance login failed: %s\n' "$(jq -r '.msg' <<< "$login_response")" >&2
    exit 1
}
TOKEN="$(jq -r '.data.accessToken' <<< "$login_response")"

api_get() {
    local path="$1"
    shift
    local args=(--noproxy '*' --silent --show-error --retry 5 --retry-all-errors --retry-delay 1 --get
        --header "Authorization: Bearer ${TOKEN}" --header "tenant-id: ${TENANT_ID}")
    while [[ $# -gt 0 ]]; do
        args+=(--data-urlencode "$1")
        shift
    done
    curl "${args[@]}" "${BASE_URL}${path}"
}

TARGET_LIST="$(api_get '/crm/performance-target/list' "scopeType=${SCOPE_TYPE}" \
    "scopeId=${SCOPE_ID}" "targetYear=${TARGET_YEAR}")"
jq -e '.code == 0 and (.data | type == "array")' >/dev/null <<< "$TARGET_LIST" || {
    printf 'Performance-target list API failed: %s\n' "$TARGET_LIST" >&2
    exit 1
}

COMPLETION="$(api_get '/crm/statistics-performance/get-target-completion' \
    "scopeType=${SCOPE_TYPE}" "deptId=${DEPARTMENT_ID}" "times=${START_TIME}" "times=${END_TIME}" \
    "targetType=${TARGET_TYPE}")"
jq -e --argjson type "$TARGET_TYPE" '
  .code == 0 and .data.targetType == $type and (.data.monthlyList | length) == 12
  and (.data.annualTarget | type == "string") and (.data.annualActual | type == "string")
' >/dev/null <<< "$COMPLETION" || {
    printf 'Performance-target completion API failed: %s\n' "$COMPLETION" >&2
    exit 1
}

printf 'CRM performance-target runtime acceptance passed: table=%s permissions=%s i18n=%s list=ok completion-months=12.\n' \
    "$TABLE_COUNT" "$PERMISSION_COUNT" "$I18N_COUNT"
