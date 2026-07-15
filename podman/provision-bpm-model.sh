#!/usr/bin/env bash
# Provision the CRM reimbursement approval model through the managed BPM APIs.
# The only command-line argument is an explicit YAML configuration path.

set -Eeuo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"

usage() {
    printf 'Usage: bash ./provision-bpm-model.sh <config.yaml>\n' >&2
}

[[ $# -eq 1 ]] || {
    usage
    exit 2
}

# shellcheck source=lib/yaml-config.sh
source "${SCRIPT_DIR}/lib/yaml-config.sh"
yaml_config_init "$1"

[[ "$(yaml_require schema_version)" == "1" ]] || {
    printf 'Unsupported schema_version; expected 1.\n' >&2
    exit 2
}

BASE_URL="$(yaml_require endpoint.base_url)"
TENANT_ID="$(yaml_positive_integer endpoint.tenant_id)"
USERNAME="$(yaml_require account.username)"
PASSWORD="$(yaml_require account.password)"
ROLE_CODE="$(yaml_require approval.role_code)"
ROLE_NAME="$(yaml_require approval.role_name)"
ROLE_SORT="$(yaml_positive_integer approval.role_sort)"
APPROVER_USERNAME="$(yaml_require approval.approver_username)"
PERMISSION_CODES="$(yaml_require approval.permission_codes)"
CATEGORY_CODE="$(yaml_require category.code)"
CATEGORY_NAME="$(yaml_require category.name)"
CATEGORY_SORT="$(yaml_positive_integer category.sort)"
MODEL_KEY="$(yaml_require model.key)"
MODEL_NAME="$(yaml_require model.name)"
MODEL_DESCRIPTION="$(yaml_require model.description)"
FORM_CREATE_PATH="$(yaml_require model.form_create_path)"
FORM_VIEW_PATH="$(yaml_require model.form_view_path)"

[[ "$BASE_URL" =~ ^https?://[^[:space:]]+$ ]] || {
    printf 'endpoint.base_url must be an HTTP(S) URL.\n' >&2
    exit 2
}
[[ "$FORM_CREATE_PATH" == /* && "$FORM_VIEW_PATH" == /* ]] || {
    printf 'Custom form paths must start with /.\n' >&2
    exit 2
}

for command in curl jq; do
    command -v "$command" >/dev/null 2>&1 || {
        printf 'Required command is unavailable: %s\n' "$command" >&2
        exit 1
    }
done

LOGIN_PAYLOAD="$(jq -n \
    --arg username "$USERNAME" \
    --arg password "$PASSWORD" \
    '{username:$username,password:$password,captchaVerification:""}')"
printf 'Connecting to BPM administration API at %s (tenant %s).\n' "$BASE_URL" "$TENANT_ID"
LOGIN_RESPONSE="$(curl --noproxy '*' --fail --silent --show-error \
    --header 'Content-Type: application/json' \
    --header "tenant-id: ${TENANT_ID}" \
    --data "$LOGIN_PAYLOAD" \
    "${BASE_URL}/system/auth/login")"
if ! jq -e '.code == 0 and (.data.accessToken | length > 0)' >/dev/null <<< "$LOGIN_RESPONSE"; then
    printf 'BPM provisioning login failed: %s\n' "$(jq -r '.msg // "unknown error"' <<< "$LOGIN_RESPONSE")" >&2
    exit 1
fi
TOKEN="$(jq -r '.data.accessToken' <<< "$LOGIN_RESPONSE")"
MANAGER_USER_ID="$(jq -r '.data.userId' <<< "$LOGIN_RESPONSE")"

api() {
    local method="$1" path="$2" body="${3:-}" response
    local args=(--noproxy '*' --fail --silent --show-error
        --request "$method"
        --header "Authorization: Bearer ${TOKEN}"
        --header "tenant-id: ${TENANT_ID}")
    if [[ -n "$body" ]]; then
        args+=(--header 'Content-Type: application/json' --data "$body")
    fi
    response="$(curl "${args[@]}" "${BASE_URL}${path}")"
    if ! jq -e '.code == 0' >/dev/null <<< "$response"; then
        printf 'API %s %s failed: %s\n' "$method" "$path" "$(jq -r '.msg // "unknown error"' <<< "$response")" >&2
        return 1
    fi
    printf '%s' "$response"
}

roles="$(api GET '/system/role/simple-list')"
role_id="$(jq -r --arg code "$ROLE_CODE" '.data[] | select(.code == $code) | .id' <<< "$roles" | head -n 1)"
if [[ -z "$role_id" ]]; then
    role_payload="$(jq -n --arg name "$ROLE_NAME" --arg code "$ROLE_CODE" --argjson sort "$ROLE_SORT" \
        '{name:$name,code:$code,sort:$sort,status:0,remark:"CRM reimbursement approval candidate role"}')"
    role_response="$(api POST '/system/role/create' "$role_payload")"
    role_id="$(jq -r '.data' <<< "$role_response")"
    printf 'Created approval role %s (%s).\n' "$ROLE_CODE" "$role_id"
else
    printf 'Reusing approval role %s (%s).\n' "$ROLE_CODE" "$role_id"
fi

user_response="$(curl --noproxy '*' --fail --silent --show-error --get \
    --header "Authorization: Bearer ${TOKEN}" \
    --header "tenant-id: ${TENANT_ID}" \
    --data-urlencode 'pageNo=1' \
    --data-urlencode 'pageSize=100' \
    --data-urlencode "username=${APPROVER_USERNAME}" \
    "${BASE_URL}/system/user/page")"
if ! jq -e '.code == 0' >/dev/null <<< "$user_response"; then
    printf 'Unable to query configured approver: %s\n' "$(jq -r '.msg // "unknown error"' <<< "$user_response")" >&2
    exit 1
fi
approver_user_id="$(jq -r --arg username "$APPROVER_USERNAME" \
    '.data.list[] | select(.username == $username and .status == 0) | .id' <<< "$user_response" | head -n 1)"
[[ -n "$approver_user_id" ]] || {
    printf 'Configured active approver account was not found: %s\n' "$APPROVER_USERNAME" >&2
    exit 1
}

user_roles="$(api GET "/system/permission/list-user-roles?userId=${approver_user_id}")"
if ! jq -e --argjson roleId "$role_id" '.data | index($roleId) != null' >/dev/null <<< "$user_roles"; then
    role_ids="$(jq --argjson roleId "$role_id" '.data + [$roleId] | unique' <<< "$user_roles")"
    assign_payload="$(jq -n --argjson userId "$approver_user_id" --argjson roleIds "$role_ids" \
        '{userId:$userId,roleIds:$roleIds}')"
    api POST '/system/permission/assign-user-role' "$assign_payload" >/dev/null
    printf 'Assigned %s to approval role %s.\n' "$APPROVER_USERNAME" "$ROLE_CODE"
else
    printf 'Approver %s already has role %s.\n' "$APPROVER_USERNAME" "$ROLE_CODE"
fi

menus="$(api GET '/system/menu/list')"
permission_codes_json="$(jq -cn --arg codes "$PERMISSION_CODES" '$codes | split(",") | map(gsub("^\\s+|\\s+$"; "")) | map(select(length > 0)) | unique')"
resolved_codes="$(jq -c --argjson codes "$permission_codes_json" '[.data[] | select(.permission as $permission | $codes | index($permission)) | .permission] | unique' <<< "$menus")"
missing_codes="$(jq -cn --argjson wanted "$permission_codes_json" --argjson resolved "$resolved_codes" '$wanted - $resolved')"
if [[ "$(jq 'length' <<< "$missing_codes")" != "0" ]]; then
    printf 'Configured approval permissions were not found in the menu registry: %s\n' "$(jq -c '.' <<< "$missing_codes")" >&2
    exit 1
fi
target_menu_ids="$(jq -c --argjson codes "$permission_codes_json" '[.data[] | select(.permission as $permission | $codes | index($permission)) | .id] | unique' <<< "$menus")"
role_menu_ids="$(api GET "/system/permission/list-role-menus?roleId=${role_id}")"
all_menu_ids="$(jq -c '.data' <<< "$role_menu_ids")"
while true; do
    parent_ids="$(jq -c --argjson selected "$target_menu_ids" '
        [.data[] | select(.id as $id | $selected | index($id)) | .parentId | select(. != 0)] | unique' <<< "$menus")"
    expanded_ids="$(jq -cn --argjson selected "$target_menu_ids" --argjson parents "$parent_ids" '$selected + $parents | unique')"
    [[ "$expanded_ids" == "$target_menu_ids" ]] && break
    target_menu_ids="$expanded_ids"
done
merged_menu_ids="$(jq -cn --argjson current "$all_menu_ids" --argjson required "$target_menu_ids" '$current + $required | unique')"
if [[ "$merged_menu_ids" != "$(jq -c 'sort' <<< "$all_menu_ids")" ]]; then
    menu_payload="$(jq -n --argjson roleId "$role_id" --argjson menuIds "$merged_menu_ids" '{roleId:$roleId,menuIds:$menuIds}')"
    api POST '/system/permission/assign-role-menu' "$menu_payload" >/dev/null
    printf 'Assigned required BPM and reimbursement permissions to role %s.\n' "$ROLE_CODE"
else
    printf 'Approval role %s already has the required permissions.\n' "$ROLE_CODE"
fi

categories="$(api GET '/bpm/category/simple-list')"
if ! jq -e --arg code "$CATEGORY_CODE" '.data[] | select(.code == $code)' >/dev/null <<< "$categories"; then
    category_payload="$(jq -n --arg name "$CATEGORY_NAME" --arg code "$CATEGORY_CODE" --argjson sort "$CATEGORY_SORT" \
        '{name:$name,description:"CRM finance approval processes",code:$code,status:0,sort:$sort}')"
    api POST '/bpm/category/create' "$category_payload" >/dev/null
    printf 'Created BPM category %s.\n' "$CATEGORY_CODE"
else
    printf 'Reusing BPM category %s.\n' "$CATEGORY_CODE"
fi

simple_model="$(jq -n --arg roleId "$role_id" '
  {
    id:"StartUserNode",type:10,name:"发起人",showText:"全体成员",
    childNode:{
      id:"Activity_CrmFinanceApproval",type:11,name:"财务审批",showText:"角色：CRM 财务审批人",
      candidateStrategy:10,candidateParam:$roleId,approveType:1,approveMethod:4,
      signEnable:false,reasonRequire:true,
      rejectHandler:{type:1},timeoutHandler:{enable:false},assignEmptyHandler:{type:4},
      assignStartUserHandlerType:1,
      taskCreateListener:{enable:false},taskAssignListener:{enable:false},taskCompleteListener:{enable:false},
      childNode:{id:"EndEvent",type:1,name:"结束"}
    }
  }')"

models="$(api GET '/bpm/model/list')"
model_id="$(jq -r --arg key "$MODEL_KEY" '.data[] | select(.key == $key) | .id' <<< "$models" | head -n 1)"
model_payload="$(jq -n \
    --arg id "$model_id" \
    --arg key "$MODEL_KEY" \
    --arg name "$MODEL_NAME" \
    --arg category "$CATEGORY_CODE" \
    --arg description "$MODEL_DESCRIPTION" \
    --arg createPath "$FORM_CREATE_PATH" \
    --arg viewPath "$FORM_VIEW_PATH" \
    --argjson managerUserId "$MANAGER_USER_ID" \
    --argjson simpleModel "$simple_model" '
    {
      key:$key,name:$name,category:$category,description:$description,type:20,formType:20,
      formCustomCreatePath:$createPath,formCustomViewPath:$viewPath,visible:true,
      startUserIds:[],startDeptIds:[],managerUserIds:[$managerUserId],
      allowCancelRunningProcess:true,allowWithdrawTask:false,autoApprovalType:0,
      processIdRule:{enable:false,prefix:"",infix:"",postfix:"",length:5},
      titleSetting:{enable:false,title:""},summarySetting:{enable:false,summary:[]},
      printTemplateSetting:{enable:false},simpleModel:$simpleModel
    } + (if $id == "" then {} else {id:$id} end)')"

if [[ -z "$model_id" ]]; then
    model_response="$(api POST '/bpm/model/create' "$model_payload")"
    model_id="$(jq -r '.data' <<< "$model_response")"
    printf 'Created BPM model %s (%s).\n' "$MODEL_KEY" "$model_id"
    deploy_required=true
else
    current_model="$(api GET "/bpm/model/get?id=${model_id}")"
    normalize_model='walk(if type == "object" then with_entries(select(.value != null)) else . end) | {key,name,category,description,type,formType,formCustomCreatePath,formCustomViewPath,visible,startUserIds,startDeptIds,managerUserIds,allowCancelRunningProcess,allowWithdrawTask,autoApprovalType,processIdRule,titleSetting,summarySetting,printTemplateSetting,simpleModel}'
    desired_state="$(jq -cS "$normalize_model" <<< "$model_payload")"
    current_state="$(jq -cS ".data | $normalize_model" <<< "$current_model")"
    definition_id="$(jq -r --arg key "$MODEL_KEY" '.data[] | select(.key == $key) | .processDefinition.id // empty' <<< "$models" | head -n 1)"
    if [[ "$desired_state" == "$current_state" && -n "$definition_id" ]]; then
        deploy_required=false
        printf 'BPM model %s is already up to date (%s).\n' "$MODEL_KEY" "$model_id"
    else
        api PUT '/bpm/model/update' "$model_payload" >/dev/null
        deploy_required=true
        printf 'Updated BPM model %s (%s).\n' "$MODEL_KEY" "$model_id"
    fi
fi

if [[ "$deploy_required" == "true" ]]; then
    api POST "/bpm/model/deploy?id=${model_id}" >/dev/null
    printf 'Deployed BPM model %s with approval role %s.\n' "$MODEL_KEY" "$ROLE_CODE"
else
    printf 'Skipped deployment because the governed model is unchanged.\n'
fi
