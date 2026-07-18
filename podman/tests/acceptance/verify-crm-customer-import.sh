#!/usr/bin/env bash

set -Eeuo pipefail

PODMAN_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/../.." && pwd)"
source "${PODMAN_DIR}/lib/kdl-config.sh"

[[ $# -eq 1 ]] || {
    printf 'Usage: bash ./tests/acceptance/verify-crm-customer-import.sh <config.kdl>\n' >&2
    exit 2
}
kdl_config_init "$1"
[[ "$(kdl_require schema_version)" == "1" ]] || exit 2

BASE_URL="$(kdl_require endpoint.base_url)"
TENANT_ID="$(kdl_positive_integer endpoint.tenant_id)"
USERNAME="$(kdl_require account.username)"
PASSWORD="$(kdl_require account.password)"
MYSQL_CONTAINER="$(kdl_require mysql.container)"
MYSQL_USER="$(kdl_require mysql.user)"
MYSQL_PASSWORD="$(kdl_require mysql.password)"
MYSQL_DATABASE="$(kdl_require mysql.database)"
GENERATOR_IMAGE="$(kdl_require image.generator)"
PREFIX="$(kdl_require test.customer_prefix)"

[[ "$BASE_URL" =~ ^https?://[^[:space:]]+$ ]] || exit 2
[[ "$PREFIX" =~ ^[A-Za-z0-9_-]+$ ]] || {
    printf 'test.customer_prefix only accepts ASCII letters, digits, underscore and dash.\n' >&2
    exit 2
}
for command in curl jq podman; do
    command -v "$command" >/dev/null
done

STAMP="$(date +%s%N)"
NAME_A="${PREFIX}_${STAMP}_A"
NAME_B="${PREFIX}_${STAMP}_B"
MOBILE_A="138${STAMP: -8}"
MOBILE_B="139${STAMP: -8}"
PREVIEW_ID=""
TMP_DIR="$(mktemp -d)"

mysql_exec() {
    podman exec "$MYSQL_CONTAINER" mysql "-u${MYSQL_USER}" "-p${MYSQL_PASSWORD}" \
        "--database=${MYSQL_DATABASE}" --default-character-set=utf8mb4 -Nse "$1"
}

cleanup() {
    set +e
    mysql_exec "DELETE p FROM crm_permission p JOIN crm_customer c ON c.id=p.biz_id
      WHERE p.tenant_id=${TENANT_ID} AND p.biz_type=2
        AND c.tenant_id=${TENANT_ID} AND c.name IN ('${NAME_A}','${NAME_B}');
      DELETE r FROM crm_customer_owner_record r JOIN crm_customer c ON c.id=r.customer_id
      WHERE r.tenant_id=${TENANT_ID} AND c.tenant_id=${TENANT_ID}
        AND c.name IN ('${NAME_A}','${NAME_B}');
      DELETE FROM crm_customer WHERE tenant_id=${TENANT_ID} AND name IN ('${NAME_A}','${NAME_B}');
      DELETE FROM crm_customer_import_preview WHERE tenant_id=${TENANT_ID}
        AND id=${PREVIEW_ID:-0};" >/dev/null 2>&1
    rm -rf "$TMP_DIR"
}
trap cleanup EXIT

login_payload="$(jq -n --arg username "$USERNAME" --arg password "$PASSWORD" \
    '{username:$username,password:$password,captchaVerification:""}')"
login_response="$(curl --noproxy '*' --fail --silent --show-error \
    --header 'Content-Type: application/json' --header "tenant-id: ${TENANT_ID}" \
    --data "$login_payload" "${BASE_URL}/system/auth/login")"
jq -e '.code == 0 and (.data.accessToken | length > 0)' >/dev/null <<< "$login_response"
TOKEN="$(jq -r '.data.accessToken' <<< "$login_response")"
AUTH_HEADERS=(--header "Authorization: Bearer ${TOKEN}" --header "tenant-id: ${TENANT_ID}")

curl --noproxy '*' --fail --silent --show-error "${AUTH_HEADERS[@]}" \
    "${BASE_URL}/crm/customer/get-import-template" -o "${TMP_DIR}/template.xls"

python_code="import zipfile;zin=zipfile.ZipFile('/work/template.xls');zout=zipfile.ZipFile('/work/runtime.xls','w',zipfile.ZIP_DEFLATED);[(lambda i,d:zout.writestr(i,d.replace('密讯'.encode(),'${NAME_A}'.encode()).replace('源码'.encode(),'${NAME_B}'.encode()).replace(b'15601691300',b'${MOBILE_A}',1).replace(b'15601691300',b'${MOBILE_B}',1) if i.filename=='xl/worksheets/sheet1.xml' else d))(i,zin.read(i.filename)) for i in zin.infolist()];zout.close();zin.close()"
podman run --rm --entrypoint python3 -v "${TMP_DIR}:/work:Z" "$GENERATOR_IMAGE" \
    -c "$python_code"

before_count="$(mysql_exec "SELECT COUNT(*) FROM crm_customer WHERE tenant_id=${TENANT_ID}
  AND deleted=0 AND name IN ('${NAME_A}','${NAME_B}');")"
[[ "$before_count" == "0" ]]

preview_response="$(curl --noproxy '*' --fail --silent --show-error "${AUTH_HEADERS[@]}" \
    --form 'updateSupport=false' --form "ownerUserId=1" \
    --form "file=@${TMP_DIR}/runtime.xls;type=application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" \
    "${BASE_URL}/crm/customer/import-preview")"
jq -e '.code == 0 and .data.totalCount == 2 and .data.createCount == 2
  and .data.updateCount == 0 and .data.failureCount == 0
  and all(.data.rows[]; .action == "CREATE")' >/dev/null <<< "$preview_response"
PREVIEW_ID="$(jq -r '.data.id' <<< "$preview_response")"

after_preview_count="$(mysql_exec "SELECT COUNT(*) FROM crm_customer WHERE tenant_id=${TENANT_ID}
  AND deleted=0 AND name IN ('${NAME_A}','${NAME_B}');")"
[[ "$after_preview_count" == "0" ]]

confirm_payload="$(jq -n --argjson id "$PREVIEW_ID" '{id:$id}')"
first_confirm="$(curl --noproxy '*' --fail --silent --show-error "${AUTH_HEADERS[@]}" \
    --header 'Content-Type: application/json' --data "$confirm_payload" \
    "${BASE_URL}/crm/customer/import-preview/confirm")"
jq -e --arg a "$NAME_A" --arg b "$NAME_B" \
    '.code == 0 and (.data.createCustomerNames | sort) == ([$a,$b] | sort)' \
    >/dev/null <<< "$first_confirm"
after_first_count="$(mysql_exec "SELECT COUNT(*) FROM crm_customer WHERE tenant_id=${TENANT_ID}
  AND deleted=0 AND name IN ('${NAME_A}','${NAME_B}');")"
[[ "$after_first_count" == "2" ]]

second_confirm="$(curl --noproxy '*' --fail --silent --show-error "${AUTH_HEADERS[@]}" \
    --header 'Content-Type: application/json' --data "$confirm_payload" \
    "${BASE_URL}/crm/customer/import-preview/confirm")"
[[ "$(jq -cS '.data' <<< "$first_confirm")" == "$(jq -cS '.data' <<< "$second_confirm")" ]]
after_second_count="$(mysql_exec "SELECT COUNT(*) FROM crm_customer WHERE tenant_id=${TENANT_ID}
  AND deleted=0 AND name IN ('${NAME_A}','${NAME_B}');")"
[[ "$after_second_count" == "2" ]]

printf 'preview-total=2 preview-business-writes=%s\n' "$after_preview_count"
printf 'first-confirm-count=%s second-confirm-count=%s idempotent-result=ok\n' \
    "$after_first_count" "$after_second_count"
printf 'acceptance-cleanup=scheduled\n'
