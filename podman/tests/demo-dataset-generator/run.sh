#!/usr/bin/env bash

set -Eeuo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
PODMAN_DIR="$(cd -- "${SCRIPT_DIR}/../.." && pwd)"
PROJECT_ROOT="$(cd -- "${PODMAN_DIR}/.." && pwd)"
CONFIG="$(mktemp "${PODMAN_DIR}/config/.demo-generator-test.XXXXXX.kdl")"
OUTPUT="${PROJECT_ROOT}/database/generated/crm-demo-contract-test"

# shellcheck source=../../lib/kdl-config.sh
source "${PODMAN_DIR}/lib/kdl-config.sh"

cleanup() {
    rm -f -- "$CONFIG"
    rm -rf -- "$OUTPUT"
}
trap cleanup EXIT

cp "${PODMAN_DIR}/config/generate-demo-dataset.example.kdl" "$CONFIG"
kdl_set_file "$CONFIG" operation.mode string generate
kdl_set_file "$CONFIG" dataset_generation.dataset_name string crm-demo-contract-test
kdl_set_file "$CONFIG" dataset_generation.output_dir string ../../database/generated/crm-demo-contract-test

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

kdl_set_file "$CONFIG" dataset_generation.business_count number 72
if bash "${PODMAN_DIR}/operations/database/generate-demo-dataset.sh" "$CONFIG" >/dev/null 2>&1; then
    printf 'Invalid equal business/contract cardinality was accepted.\n' >&2
    exit 1
fi

kdl_set_file "$CONFIG" dataset_generation.business_count number 180
kdl_set_file "$CONFIG" dataset_generation.demo_user_password_source string plaintext
if bash "${PODMAN_DIR}/operations/database/generate-demo-dataset.sh" "$CONFIG" >/dev/null 2>&1; then
    printf 'Unsupported demo password source was accepted.\n' >&2
    exit 1
fi

printf 'Demo dataset generator contract tests passed.\n'
