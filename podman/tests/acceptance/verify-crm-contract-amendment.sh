#!/usr/bin/env bash

# End-to-end acceptance for the signed-contract amendment workflow.
# The only command-line argument is an explicit KDL configuration path.

set -Eeuo pipefail

PODMAN_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/../.." && pwd)"

usage() {
    printf 'Usage: bash ./tests/acceptance/verify-crm-contract-amendment.sh <config.kdl>\n' >&2
}

[[ $# -eq 1 ]] || {
    usage
    exit 2
}

# shellcheck source=../../lib/kdl-config.sh
source "${PODMAN_DIR}/lib/kdl-config.sh"
kdl_config_init "$1"

[[ "$(kdl_require schema_version)" == "1" ]] || {
    printf 'Unsupported schema_version; expected 1.\n' >&2
    exit 2
}

BASE_URL="$(kdl_require endpoint.base_url)"
TENANT_ID="$(kdl_positive_integer endpoint.tenant_id)"
NEGATIVE_TENANT_ID="$(kdl_positive_integer endpoint.negative_tenant_id)"
USERNAME="$(kdl_require account.username)"
PASSWORD="$(kdl_require account.password)"
CONTRACT_ID="$(kdl_positive_integer acceptance.contract_id)"
OTHER_CONTRACT_ID="$(kdl_positive_integer acceptance.other_contract_id)"
HANDLER_USER_ID="$(kdl_positive_integer acceptance.handler_user_id)"
EVIDENCE_FILE="$(kdl_path acceptance.evidence_file)"
TITLE="$(kdl_require acceptance.title)"
REASON="$(kdl_require acceptance.reason)"
POLL_ATTEMPTS="$(kdl_positive_integer acceptance.poll_attempts)"
POLL_INTERVAL="$(kdl_positive_integer acceptance.poll_interval_seconds)"

[[ "$BASE_URL" =~ ^https?://[^[:space:]]+$ ]] || {
    printf 'endpoint.base_url must be an HTTP(S) URL.\n' >&2
    exit 2
}
[[ "$NEGATIVE_TENANT_ID" != "$TENANT_ID" ]] || {
    printf 'endpoint.negative_tenant_id must differ from endpoint.tenant_id.\n' >&2
    exit 2
}
[[ -s "$EVIDENCE_FILE" ]] || {
    printf 'Configured acceptance evidence is missing or empty: %s\n' "$EVIDENCE_FILE" >&2
    exit 2
}

for command in curl jq date; do
    command -v "$command" >/dev/null 2>&1 || {
        printf 'Required command is unavailable: %s\n' "$command" >&2
        exit 1
    }
done

LOGIN_PAYLOAD="$(jq -n --arg username "$USERNAME" --arg password "$PASSWORD" \
    '{username:$username,password:$password,captchaVerification:""}')"
LOGIN_RESPONSE="$(curl --noproxy '*' --fail --silent --show-error \
    --header 'Content-Type: application/json' \
    --header "tenant-id: ${TENANT_ID}" \
    --data "$LOGIN_PAYLOAD" \
    "${BASE_URL}/system/auth/login")"
if ! jq -e '.code == 0 and (.data.accessToken | length > 0)' >/dev/null <<< "$LOGIN_RESPONSE"; then
    printf 'Acceptance login failed: %s\n' "$(jq -r '.msg // "unknown error"' <<< "$LOGIN_RESPONSE")" >&2
    exit 1
fi
TOKEN="$(jq -r '.data.accessToken' <<< "$LOGIN_RESPONSE")"

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
        printf 'API %s %s failed: %s\n' "$method" "$path" \
            "$(jq -r '.msg // "unknown error"' <<< "$response")" >&2
        return 1
    fi
    printf '%s' "$response"
}

upload_file() {
    local contract_id="$1" response
    response="$(curl --noproxy '*' --fail --silent --show-error \
        --header "Authorization: Bearer ${TOKEN}" \
        --header "tenant-id: ${TENANT_ID}" \
        --form "contractId=${contract_id}" \
        --form "file=@${EVIDENCE_FILE};type=text/plain" \
        "${BASE_URL}/crm/contract-lifecycle/attachment/upload")"
    if ! jq -e '.code == 0 and (.data | length > 0)' >/dev/null <<< "$response"; then
        printf 'Protected contract file upload failed: %s\n' \
            "$(jq -r '.msg // "unknown error"' <<< "$response")" >&2
        return 1
    fi
    jq -r '.data' <<< "$response"
}

printf 'Loading approved contract %s for amendment acceptance.\n' "$CONTRACT_ID"
contract_response="$(api GET "/crm/contract/get?id=${CONTRACT_ID}")"
contract="$(jq -c '.data' <<< "$contract_response")"
jq -e '.auditStatus == 20 and (.products | type == "array") and (.products | length > 0)' \
    >/dev/null <<< "$contract" || {
    printf 'Configured contract must be approved and contain at least one product.\n' >&2
    exit 1
}

lifecycle_response="$(api GET "/crm/contract-lifecycle/get?contractId=${CONTRACT_ID}")"
if [[ "$(jq -r '.data.signing.status // 0' <<< "$lifecycle_response")" != "10" ]]; then
    printf 'Creating protected signed copy and actual signing fact.\n'
    signed_url="$(upload_file "$CONTRACT_ID")"
    signed_attachment_payload="$(jq -n --argjson contractId "$CONTRACT_ID" \
        --arg fileName "$(basename -- "$EVIDENCE_FILE")" --arg fileUrl "$signed_url" \
        '{contractId:$contractId,category:2,fileName:$fileName,fileUrl:$fileUrl}')"
    signed_attachment_response="$(api POST '/crm/contract-lifecycle/attachment' "$signed_attachment_payload")"
    signed_attachment_id="$(jq -r '.data' <<< "$signed_attachment_response")"
    signed_time="$(date +%s%3N)"
    sign_payload="$(jq -n --argjson contractId "$CONTRACT_ID" --argjson method 1 \
        --argjson signedTime "$signed_time" --argjson signedAttachmentId "$signed_attachment_id" \
        --argjson handlerUserId "$HANDLER_USER_ID" \
        '{contractId:$contractId,method:$method,signedTime:$signedTime,signedAttachmentId:$signedAttachmentId,handlerUserId:$handlerUserId}')"
    api PUT '/crm/contract-lifecycle/sign' "$sign_payload" >/dev/null
fi

amendment_list="$(api GET "/crm/contract-amendment/list?contractId=${CONTRACT_ID}")"
amendment_id="$(jq -r '.data[]? | select(.auditStatus != 20) | .id' <<< "$amendment_list" | head -n 1)"
amendment_status="$(jq -r --argjson id "${amendment_id:-0}" \
    '.data[]? | select(.id == $id) | .auditStatus' <<< "$amendment_list" | head -n 1)"
if [[ -z "$amendment_id" ]]; then
    request_id="crm-amendment-acceptance-$(date +%s%N)"
    amendment_payload="$(jq -n --argjson contract "$contract" --argjson contractId "$CONTRACT_ID" \
        --arg requestId "$request_id" --arg title "$TITLE" --arg reason "$REASON" '
        {
          contractId:$contractId,clientRequestId:$requestId,title:$title,reason:$reason,
          contractName:$contract.name,startTime:$contract.startTime,endTime:$contract.endTime,
          discountPercent:($contract.discountPercent // 0),signContactId:$contract.signContactId,
          signUserId:$contract.signUserId,remark:$contract.remark,
          products:($contract.products | map({id,productId,contractPrice,count}))
        }')"
    amendment_response="$(api POST '/crm/contract-amendment/create' "$amendment_payload")"
    amendment_id="$(jq -r '.data' <<< "$amendment_response")"
    amendment_status=0
    printf 'Created contract amendment %s.\n' "$amendment_id"
else
    printf 'Reusing unfinished contract amendment %s (status %s).\n' "$amendment_id" "$amendment_status"
fi

evidence_attachment_id=''
if [[ "$amendment_status" != "10" ]]; then
    evidence_url="$(upload_file "$CONTRACT_ID")"
    evidence_payload="$(jq -n --argjson contractId "$CONTRACT_ID" --argjson amendmentId "$amendment_id" \
        --arg fileName "$(basename -- "$EVIDENCE_FILE")" --arg fileUrl "$evidence_url" \
        '{contractId:$contractId,amendmentId:$amendmentId,category:3,fileName:$fileName,fileUrl:$fileUrl}')"
    evidence_response="$(api POST '/crm/contract-lifecycle/attachment' "$evidence_payload")"
    evidence_attachment_id="$(jq -r '.data' <<< "$evidence_response")"

    submit_payload="$(jq -n --argjson contractId "$CONTRACT_ID" --argjson id "$amendment_id" \
        '{contractId:$contractId,id:$id}')"
    api PUT '/crm/contract-amendment/submit' "$submit_payload" >/dev/null
fi
amendment_detail="$(api GET "/crm/contract-amendment/get?contractId=${CONTRACT_ID}&id=${amendment_id}")"
process_instance_id="$(jq -r '.data.processInstanceId' <<< "$amendment_detail")"
[[ -n "$process_instance_id" && "$process_instance_id" != "null" ]] || {
    printf 'Submitted amendment did not expose a process instance.\n' >&2
    exit 1
}

task_id=''
for ((attempt = 1; attempt <= POLL_ATTEMPTS; attempt++)); do
    todo="$(api GET '/bpm/task/todo-page?pageNo=1&pageSize=100&processDefinitionKey=crm-contract-amendment-audit')"
    task_id="$(jq -r --arg process "$process_instance_id" \
        '.data.list[]? | select(.processInstanceId == $process) | .id' <<< "$todo" | head -n 1)"
    [[ -z "$task_id" ]] || break
    sleep "$POLL_INTERVAL"
done
[[ -n "$task_id" ]] || {
    printf 'Timed out waiting for amendment approval task (%s).\n' "$process_instance_id" >&2
    exit 1
}

approve_payload="$(jq -n --arg id "$task_id" \
    '{id:$id,reason:"CRM amendment automated acceptance",variables:{},nextAssignees:{}}')"
api PUT '/bpm/task/approve' "$approve_payload" >/dev/null

effective_detail=''
for ((attempt = 1; attempt <= POLL_ATTEMPTS; attempt++)); do
    effective_detail="$(api GET "/crm/contract-amendment/get?contractId=${CONTRACT_ID}&id=${amendment_id}")"
    [[ "$(jq -r '.data.auditStatus' <<< "$effective_detail")" == "20" ]] && break
    sleep "$POLL_INTERVAL"
done
jq -e '.data.auditStatus == 20 and .data.effectiveTime != null' >/dev/null <<< "$effective_detail" || {
    printf 'Amendment did not become effective after approval.\n' >&2
    exit 1
}

lifecycle_after="$(api GET "/crm/contract-lifecycle/get?contractId=${CONTRACT_ID}")"
if [[ -z "$evidence_attachment_id" ]]; then
    evidence_attachment_id="$(jq -r --argjson amendmentId "$amendment_id" \
        '.data.attachments[]? | select(.amendmentId == $amendmentId) | .id' \
        <<< "$lifecycle_after" | head -n 1)"
fi
jq -e --argjson attachmentId "$evidence_attachment_id" --argjson amendmentId "$amendment_id" \
    '.data.attachments[] | select(.id == $attachmentId and .amendmentId == $amendmentId and .immutable == true)' \
    >/dev/null <<< "$lifecycle_after" || {
    printf 'Effective amendment evidence was not locked.\n' >&2
    exit 1
}

cross_contract="$(curl --noproxy '*' --fail --silent --show-error \
    --header "Authorization: Bearer ${TOKEN}" --header "tenant-id: ${TENANT_ID}" \
    "${BASE_URL}/crm/contract-amendment/get?contractId=${OTHER_CONTRACT_ID}&id=${amendment_id}")"
jq -e '.code != 0' >/dev/null <<< "$cross_contract" || {
    printf 'Cross-contract amendment read was not rejected.\n' >&2
    exit 1
}

cross_tenant="$(curl --noproxy '*' --silent --show-error \
    --header "Authorization: Bearer ${TOKEN}" --header "tenant-id: ${NEGATIVE_TENANT_ID}" \
    "${BASE_URL}/crm/contract-amendment/get?contractId=${CONTRACT_ID}&id=${amendment_id}")"
jq -e '.code != 0' >/dev/null <<< "$cross_tenant" || {
    printf 'Cross-tenant amendment read was not rejected.\n' >&2
    exit 1
}

printf 'CRM contract amendment acceptance passed: contract=%s amendment=%s process=%s evidence=%s.\n' \
    "$CONTRACT_ID" "$amendment_id" "$process_instance_id" "$evidence_attachment_id"
