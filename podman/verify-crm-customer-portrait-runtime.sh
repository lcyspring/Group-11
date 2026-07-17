#!/usr/bin/env bash

set -Eeuo pipefail
trap 'cleanup' EXIT

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
source "${SCRIPT_DIR}/lib/yaml-config.sh"

[[ $# -eq 1 ]] || { printf 'Usage: bash ./verify-crm-customer-portrait-runtime.sh <config.yaml>\n' >&2; exit 2; }
yaml_config_init "$1"
[[ "$(yaml_require schema_version)" == 1 ]] || exit 2

BASE_URL="$(yaml_require endpoint.base_url)"
TENANT_ID="$(yaml_positive_integer endpoint.tenant_id)"
USERNAME="$(yaml_require account.username)"
PASSWORD="$(yaml_require account.password)"
MYSQL_CONTAINER="$(yaml_require mysql.container)"
MYSQL_USER="$(yaml_require mysql.user)"
MYSQL_PASSWORD="$(yaml_require mysql.password)"
MYSQL_DATABASE="$(yaml_require mysql.database)"
DEPT_ID="$(yaml_positive_integer acceptance.dept_id)"
OWNER_USER_ID="$(yaml_positive_integer acceptance.owner_user_id)"
AREA_ID="$(yaml_positive_integer acceptance.area_id)"
PREFIX="$(yaml_require acceptance.name_prefix)"
START_TIME="$(yaml_require acceptance.start_time)"
END_TIME="$(yaml_require acceptance.end_time)"

[[ "$PREFIX" =~ ^[A-Za-z0-9_-]+$ && "$MYSQL_DATABASE" =~ ^[A-Za-z0-9_]+$ ]] || exit 2

mysql_exec() {
    podman exec "$MYSQL_CONTAINER" mysql "-u${MYSQL_USER}" "-p${MYSQL_PASSWORD}" \
        "--database=${MYSQL_DATABASE}" --default-character-set=utf8mb4 -Nse "$1"
}

cleanup() {
    [[ -n "${PREFIX:-}" ]] || return 0
    mysql_exec "DELETE FROM crm_permission WHERE biz_type=2 AND biz_id IN
      (SELECT id FROM crm_customer WHERE name LIKE '${PREFIX}%');
      DELETE FROM crm_customer WHERE name LIKE '${PREFIX}%';" >/dev/null 2>&1 || true
}

cleanup
mysql_exec "INSERT INTO crm_customer
  (name, owner_user_id, owner_time, pool_status, lock_status, deal_status, lifecycle_status,
   lifecycle_status_change_time, lifecycle_lost_reason, area_id, industry_id, level, source, creator, create_time,
   updater, update_time, deleted, tenant_id)
VALUES
  ('${PREFIX}-potential',${OWNER_USER_ID},'2099-07-17 10:00:00',0,b'0',b'0',10,'2099-07-17 10:00:00',NULL,${AREA_ID},9911,9913,9912,'1','2099-07-17 10:00:00','1','2099-07-17 10:00:00',b'0',${TENANT_ID}),
  ('${PREFIX}-following',${OWNER_USER_ID},'2099-07-17 10:00:00',0,b'0',b'0',20,'2099-07-17 10:00:00',NULL,${AREA_ID},9911,9913,9912,'1','2099-07-17 10:00:01','1','2099-07-17 10:00:01',b'0',${TENANT_ID}),
  ('${PREFIX}-deal',${OWNER_USER_ID},'2099-07-17 10:00:00',0,b'0',b'1',30,'2099-07-17 10:00:00',NULL,${AREA_ID},9911,9913,9912,'1','2099-07-17 10:00:02','1','2099-07-17 10:00:02',b'0',${TENANT_ID}),
  ('${PREFIX}-lost',${OWNER_USER_ID},'2099-07-17 10:00:00',0,b'0',b'0',40,'2099-07-17 10:00:00','runtime acceptance',${AREA_ID},9911,9913,9912,'1','2099-07-17 10:00:03','1','2099-07-17 10:00:03',b'0',${TENANT_ID});"

login="$(curl --noproxy '*' --fail --silent --show-error --header 'Content-Type: application/json' \
    --header "tenant-id: ${TENANT_ID}" \
    --data "$(jq -n --arg username "$USERNAME" --arg password "$PASSWORD" \
      '{username:$username,password:$password,captchaVerification:""}')" \
    "${BASE_URL}/system/auth/login")"
TOKEN="$(jq -er '.data.accessToken' <<< "$login")"

api_get() {
    local path="$1"; shift
    local args=(--noproxy '*' --silent --show-error --get --header "Authorization: Bearer ${TOKEN}"
      --header "tenant-id: ${TENANT_ID}")
    while [[ $# -gt 0 ]]; do args+=(--data-urlencode "$1"); shift; done
    curl "${args[@]}" "${BASE_URL}${path}"
}

common=("deptId=${DEPT_ID}" "userId=${OWNER_USER_ID}" "times=${START_TIME}" "times=${END_TIME}")
industry="$(api_get '/crm/statistics-portrait/get-customer-industry-summary' "${common[@]}")"
source="$(api_get '/crm/statistics-portrait/get-customer-source-summary' "${common[@]}")"
level="$(api_get '/crm/statistics-portrait/get-customer-level-summary' "${common[@]}")"
status="$(api_get '/crm/statistics-portrait/get-customer-deal-status-summary' "${common[@]}")"
area="$(api_get '/crm/statistics-portrait/get-customer-area-summary' "${common[@]}")"

jq -e '.code==0 and (.data[] | select(.industryId==9911) | .customerCount==4 and .dealCount==1)' <<< "$industry" >/dev/null
jq -e '.code==0 and (.data[] | select(.source==9912) | .customerCount==4 and .dealCount==1)' <<< "$source" >/dev/null
jq -e '.code==0 and (.data[] | select(.level==9913) | .customerCount==4 and .dealCount==1)' <<< "$level" >/dev/null
jq -e '.code==0 and ([.data[] | select(.lifecycleStatus==10 or .lifecycleStatus==20 or .lifecycleStatus==30 or .lifecycleStatus==40) | .customerCount] | add)==4' <<< "$status" >/dev/null
province_id="$(jq -er '.data[] | select(.customerCount==4 and .dealCount==1) | .areaId' <<< "$area")"
page="$(api_get '/crm/statistics-portrait/get-customer-page-by-area' "${common[@]}" \
    'areaType=2' "areaId=${province_id}" 'pageNo=1' 'pageSize=10')"
jq -e --arg prefix "$PREFIX" '.code==0 and .data.total==4 and ([.data.list[].name | startswith($prefix)] | all)' <<< "$page" >/dev/null

printf 'CRM customer portrait runtime acceptance passed: dimensions=3 lifecycle=4 area-drill=4 cleanup=pending.\n'
