#!/usr/bin/env bash

# Stateful work-order negative security matrix with temporary user/role/data.

set -Eeuo pipefail
PODMAN_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/../.." && pwd)"
source "${PODMAN_DIR}/lib/yaml-config.sh"

[[ $# -eq 1 ]] || { printf 'Usage: bash ./tests/acceptance/verify-crm-work-order-security.sh <config.yaml>\n' >&2; exit 2; }
yaml_config_init "$1"
[[ "$(yaml_require schema_version)" == 1 ]] || exit 2

BASE_URL="$(yaml_require endpoint.base_url)"
TENANT_ID="$(yaml_positive_integer endpoint.tenant_id)"
FOREIGN_TENANT_ID="$(yaml_positive_integer endpoint.foreign_tenant_id)"
ADMIN_USERNAME="$(yaml_require account.admin_username)"
PASSWORD="$(yaml_require account.password)"
DEPARTMENT_ID="$(yaml_positive_integer acceptance.department_id)"
CUSTOMER_ID="$(yaml_positive_integer acceptance.customer_id)"
HANDLER_USER_ID="$(yaml_positive_integer acceptance.handler_user_id)"
QUERY_DENIED_CODE="$(yaml_positive_integer acceptance.query_denied_code)"
MYSQL_CONTAINER="$(yaml_require mysql.container)"
MYSQL_USER="$(yaml_require mysql.user)"
MYSQL_PASSWORD="$(yaml_require mysql.password)"
MYSQL_DATABASE="$(yaml_require mysql.database)"

for command in curl jq podman date; do command -v "$command" >/dev/null || exit 1; done
RUN_ID="$(date +%s%N)"
PREFIX="WO-SEC-${RUN_ID}"
USERNAME="wosec${RUN_ID}"
ROLE_CODE="crm_wo_sec_${RUN_ID}"
USER_ID=''; ROLE_ID=''; ADMIN_TOKEN=''; ORDER_IDS=()

mysql_exec() {
    podman exec --env "MYSQL_PWD=${MYSQL_PASSWORD}" "$MYSQL_CONTAINER" mysql --user "$MYSQL_USER" \
      --database "$MYSQL_DATABASE" --default-character-set=utf8mb4 --batch --skip-column-names --execute "$1"
}

login() {
    local username="$1" response
    response="$(curl --noproxy '*' --fail --silent --show-error --header 'Content-Type: application/json' \
      --header "tenant-id: ${TENANT_ID}" --data "$(jq -n --arg username "$username" --arg password "$PASSWORD" \
      '{username:$username,password:$password,captchaVerification:""}')" "${BASE_URL}/system/auth/login")"
    jq -er 'select(.code == 0) | .data.accessToken' <<<"$response"
}

api_raw() {
    local token="$1" tenant="$2" method="$3" path="$4" body="${5:-}" args
    args=(--noproxy '*' --silent --show-error --request "$method" --header "Authorization: Bearer ${token}" --header "tenant-id: ${tenant}")
    [[ -z "$body" ]] || args+=(--header 'Content-Type: application/json' --data "$body")
    curl "${args[@]}" "${BASE_URL}${path}"
}

cleanup() {
    set +e
    mysql_exec "DELETE n FROM system_notify_message n WHERE n.tenant_id=${TENANT_ID} AND n.template_params LIKE '%${PREFIX}%';
      DELETE c FROM crm_work_order_check_in c JOIN crm_work_order w ON w.id=c.work_order_id WHERE w.tenant_id=${TENANT_ID} AND w.title LIKE '${PREFIX}%';
      DELETE s FROM crm_work_order_sla s JOIN crm_work_order w ON w.id=s.work_order_id WHERE w.tenant_id=${TENANT_ID} AND w.title LIKE '${PREFIX}%';
      DELETE c FROM crm_work_order_cc c JOIN crm_work_order w ON w.id=c.work_order_id WHERE w.tenant_id=${TENANT_ID} AND w.title LIKE '${PREFIX}%';
      DELETE r FROM crm_work_order_record r JOIN crm_work_order w ON w.id=r.work_order_id WHERE w.tenant_id=${TENANT_ID} AND w.title LIKE '${PREFIX}%';
      DELETE FROM crm_work_order WHERE tenant_id=${TENANT_ID} AND title LIKE '${PREFIX}%';" >/dev/null 2>&1
    if [[ "$USER_ID" =~ ^[0-9]+$ ]]; then
      mysql_exec "DELETE FROM system_oauth2_access_token WHERE tenant_id=${TENANT_ID} AND user_id=${USER_ID};
        DELETE FROM system_oauth2_refresh_token WHERE tenant_id=${TENANT_ID} AND user_id=${USER_ID};
        DELETE FROM system_login_log WHERE tenant_id=${TENANT_ID} AND user_id=${USER_ID};
        DELETE FROM system_user_role WHERE tenant_id=${TENANT_ID} AND user_id=${USER_ID};
        DELETE FROM system_users WHERE tenant_id=${TENANT_ID} AND id=${USER_ID};" >/dev/null 2>&1
    fi
    if [[ "$ROLE_ID" =~ ^[0-9]+$ ]]; then
      mysql_exec "DELETE FROM system_role_menu WHERE tenant_id=${TENANT_ID} AND role_id=${ROLE_ID};
        DELETE FROM system_role WHERE tenant_id=${TENANT_ID} AND id=${ROLE_ID};" >/dev/null 2>&1
    fi
}
trap cleanup EXIT

ADMIN_TOKEN="$(login "$ADMIN_USERNAME")"
menu_ids="$(mysql_exec "SELECT GROUP_CONCAT(id ORDER BY id) FROM system_menu WHERE permission IN
 ('crm:work-order:query','crm:work-order:create','crm:work-order:process') AND deleted=b'0';")"
[[ "$menu_ids" =~ ^[0-9]+(,[0-9]+){2}$ ]] || { printf 'Work-order security permissions are incomplete.\n' >&2; exit 1; }

role_response="$(api_raw "$ADMIN_TOKEN" "$TENANT_ID" POST '/system/role/create' \
  "$(jq -n --arg code "$ROLE_CODE" '{name:"CRM 工单安全验收",code:$code,sort:999,status:0}')")"
ROLE_ID="$(jq -er 'select(.code == 0) | .data' <<<"$role_response")"
menus_json="$(jq -cn --arg ids "$menu_ids" '$ids|split(",")|map(tonumber)')"
jq -e '.code == 0' >/dev/null <<<"$(api_raw "$ADMIN_TOKEN" "$TENANT_ID" POST '/system/permission/assign-role-menu' \
  "$(jq -n --argjson role "$ROLE_ID" --argjson menus "$menus_json" '{roleId:$role,menuIds:$menus}')")"
jq -e '.code == 0' >/dev/null <<<"$(api_raw "$ADMIN_TOKEN" "$TENANT_ID" POST '/system/permission/assign-role-data-scope' \
  "$(jq -n --argjson role "$ROLE_ID" '{roleId:$role,dataScope:5,dataScopeDeptIds:[]}' )")"

user_response="$(api_raw "$ADMIN_TOKEN" "$TENANT_ID" POST '/system/user/create' \
  "$(jq -n --arg username "$USERNAME" --arg password "$PASSWORD" --argjson dept "$DEPARTMENT_ID" \
  '{username:$username,password:$password,nickname:"CRM 工单受限验收",deptId:$dept,status:0,sex:0}')")"
USER_ID="$(jq -er 'select(.code == 0) | .data' <<<"$user_response")"
jq -e '.code == 0' >/dev/null <<<"$(api_raw "$ADMIN_TOKEN" "$TENANT_ID" POST '/system/permission/assign-user-role' \
  "$(jq -n --argjson user "$USER_ID" --argjson role "$ROLE_ID" '{userId:$user,roleIds:[$role]}')")"
RESTRICTED_TOKEN="$(login "$USERNAME")"

create_admin_order() {
    local title="$1" description="$2" cc="${3:-[]}" response id
    response="$(api_raw "$ADMIN_TOKEN" "$TENANT_ID" POST '/crm/work-order/create' \
      "$(jq -n --arg title "$title" --arg description "$description" --argjson customer "$CUSTOMER_ID" \
      --argjson handler "$HANDLER_USER_ID" --argjson cc "$cc" \
      '{title:$title,type:1,priority:2,customerId:$customer,sourceType:0,handlerUserId:$handler,ccUserIds:$cc,attachmentUrls:[],description:$description}')")"
    id="$(jq -er 'select(.code == 0) | .data' <<<"$response")"
    ORDER_IDS+=("$id")
    printf '%s' "$id"
}

BASE_ORDER_ID="$(create_admin_order "${PREFIX}-BASE" '安全负向验收基础工单，必须保持对象权限和状态机不变量。')"

denied_detail="$(api_raw "$RESTRICTED_TOKEN" "$TENANT_ID" GET "/crm/work-order/get?id=${BASE_ORDER_ID}")"
jq -e --argjson code "$QUERY_DENIED_CODE" '.code == $code and .data == null' >/dev/null <<<"$denied_detail"
denied_start="$(api_raw "$RESTRICTED_TOKEN" "$TENANT_ID" PUT '/crm/work-order/start' "$(jq -n --argjson id "$BASE_ORDER_ID" '{id:$id,remark:"越权开始"}')")"
jq -e '.code != 0 and .data == null' >/dev/null <<<"$denied_start"
[[ "$(mysql_exec "SELECT status FROM crm_work_order WHERE id=${BASE_ORDER_ID};")" == 10 ]]

cross_tenant="$(api_raw "$RESTRICTED_TOKEN" "$FOREIGN_TENANT_ID" GET "/crm/work-order/get?id=${BASE_ORDER_ID}")"
jq -e '.code != 0 and .data == null' >/dev/null <<<"$cross_tenant"

invalid_complete="$(api_raw "$ADMIN_TOKEN" "$TENANT_ID" PUT '/crm/work-order/complete' \
  "$(jq -n --argjson id "$BASE_ORDER_ID" '{id:$id,solution:"尚未开始处理时不得直接完结该工单。"}')")"
jq -e '.code != 0' >/dev/null <<<"$invalid_complete"
[[ "$(mysql_exec "SELECT status FROM crm_work_order WHERE id=${BASE_ORDER_ID};")" == 10 ]]

SQL_TITLE="${PREFIX}-SQL-' OR 1=1 --"
SQL_ID="$(create_admin_order "$SQL_TITLE" 'SQL 元字符必须作为普通参数保存，不能改变查询或写入范围。')"
[[ "$(mysql_exec "SELECT COUNT(*) FROM crm_work_order WHERE tenant_id=${TENANT_ID} AND id=${SQL_ID} AND title=CONCAT('${PREFIX}-SQL-', CHAR(39), ' OR 1=1 --');")" == 1 ]]

before_xss="$(mysql_exec "SELECT COUNT(*) FROM crm_work_order WHERE tenant_id=${TENANT_ID} AND title='${PREFIX}-XSS';")"
xss_response="$(api_raw "$ADMIN_TOKEN" "$TENANT_ID" POST '/crm/work-order/create' \
  "$(jq -n --arg title "${PREFIX}-XSS" --argjson customer "$CUSTOMER_ID" --argjson handler "$HANDLER_USER_ID" \
  '{title:$title,type:1,priority:2,customerId:$customer,sourceType:0,handlerUserId:$handler,ccUserIds:[],attachmentUrls:[],description:"<script>alert(1)</script>安全验收描述必须被拒绝或净化。"}')")"
if [[ "$(jq -r '.code' <<<"$xss_response")" == 0 ]]; then
    XSS_ID="$(jq -r '.data' <<<"$xss_response")"; ORDER_IDS+=("$XSS_ID")
    stored="$(mysql_exec "SELECT description FROM crm_work_order WHERE id=${XSS_ID};")"
    [[ "${stored,,}" != *'<script'* && "${stored,,}" != *'javascript:'* ]] || {
        printf 'Stored work-order description contains executable XSS markup.\n' >&2; exit 1;
    }
    xss_outcome=sanitized
else
    [[ "$(mysql_exec "SELECT COUNT(*) FROM crm_work_order WHERE tenant_id=${TENANT_ID} AND title='${PREFIX}-XSS';")" == "$before_xss" ]]
    xss_outcome=rejected
fi

cc_json="$(jq -cn '[range(0;101)|1]')"
boundary_response="$(api_raw "$ADMIN_TOKEN" "$TENANT_ID" POST '/crm/work-order/create' \
  "$(jq -n --arg title "${PREFIX}-BOUNDARY" --argjson customer "$CUSTOMER_ID" --argjson handler "$HANDLER_USER_ID" \
  --argjson cc "$cc_json" '{title:$title,type:1,priority:2,customerId:$customer,sourceType:0,handlerUserId:$handler,ccUserIds:$cc,attachmentUrls:[],description:"超过一百个抄送人的请求必须被参数边界拒绝。"}')")"
jq -e '.code != 0' >/dev/null <<<"$boundary_response"
[[ "$(mysql_exec "SELECT COUNT(*) FROM crm_work_order WHERE tenant_id=${TENANT_ID} AND title='${PREFIX}-BOUNDARY';")" == 0 ]]

cleanup
USER_ID=''; ROLE_ID=''; ORDER_IDS=()
residual="$(mysql_exec "SELECT
 (SELECT COUNT(*) FROM crm_work_order WHERE tenant_id=${TENANT_ID} AND title LIKE '${PREFIX}%') +
 (SELECT COUNT(*) FROM system_users WHERE tenant_id=${TENANT_ID} AND username='${USERNAME}') +
 (SELECT COUNT(*) FROM system_role WHERE tenant_id=${TENANT_ID} AND code='${ROLE_CODE}');")"
[[ "$residual" == 0 ]]
trap - EXIT
printf 'CRM work-order security passed: object-deny=2 cross-tenant=1 invalid-state=1 sql-meta=1 xss=%s boundary=1 cleanup=0\n' "$xss_outcome"
