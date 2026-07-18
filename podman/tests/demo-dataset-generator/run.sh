#!/usr/bin/env bash

set -Eeuo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
PODMAN_DIR="$(cd -- "${SCRIPT_DIR}/../.." && pwd)"
PROJECT_ROOT="$(cd -- "${PODMAN_DIR}/.." && pwd)"
CONFIG="$(mktemp "${PODMAN_DIR}/config/.demo-generator-test.XXXXXX.yaml")"
OUTPUT="${PROJECT_ROOT}/database/generated/crm-demo-contract-test"

cleanup() {
    rm -f -- "$CONFIG"
    rm -rf -- "$OUTPUT"
}
trap cleanup EXIT

cp "${PODMAN_DIR}/config/generate-demo-dataset.example.yaml" "$CONFIG"
sed -i \
    -e 's/mode: check/mode: generate/' \
    -e 's/crm-demo-v2/crm-demo-contract-test/g' \
    "$CONFIG"

bash "${PODMAN_DIR}/operations/database/generate-demo-dataset.sh" "$CONFIG"
sha256sum -c "${OUTPUT}/SHA256SUMS"

[[ "$(sed -n '/^\.\/.*\.sql$/p' "${OUTPUT}/crm-demo-contract-test.manifest" | wc -l)" == 4 ]]
[[ -s "${OUTPUT}/04-validate.sql" ]]
! grep -ERn '__[A-Z0-9_]+__' "$OUTPUT"
grep -q 'empty opportunity stage' "${OUTPUT}/04-validate.sql"
grep -q 'status_type_id' "${OUTPUT}/02-insert.sql"
grep -q 'receivable_id IS NULL AND return_time' "${OUTPUT}/04-validate.sql"
grep -q "username REGEXP '\^\[A-Za-z0-9\]" "${OUTPUT}/04-validate.sql"
grep -q 'role permission separation' "${OUTPUT}/04-validate.sql"
grep -q 'demo_role_menu_frontier' "${OUTPUT}/02-insert.sql"

sed -i 's/business_count: 180/business_count: 72/' "$CONFIG"
if bash "${PODMAN_DIR}/operations/database/generate-demo-dataset.sh" "$CONFIG" >/dev/null 2>&1; then
    printf 'Invalid equal business/contract cardinality was accepted.\n' >&2
    exit 1
fi

sed -i \
    -e 's/business_count: 72/business_count: 180/' \
    -e 's/demo_user_password_source: owner/demo_user_password_source: plaintext/' \
    "$CONFIG"
if bash "${PODMAN_DIR}/operations/database/generate-demo-dataset.sh" "$CONFIG" >/dev/null 2>&1; then
    printf 'Unsupported demo password source was accepted.\n' >&2
    exit 1
fi

printf 'Demo dataset generator contract tests passed.\n'
