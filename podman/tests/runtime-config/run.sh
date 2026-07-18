#!/usr/bin/env bash

set -Eeuo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
PODMAN_DIR="$(cd -- "${SCRIPT_DIR}/../.." && pwd)"
DATABASE_DIR="$(cd -- "${PODMAN_DIR}/../database" && pwd)"

usage() {
    printf 'Usage: bash ./tests/runtime-config/run.sh <runtime-config.yaml>\n' >&2
}

[[ $# -eq 1 ]] || {
    usage
    exit 2
}

CONFIG_PATH="$1"
if [[ "$CONFIG_PATH" != /* ]]; then
    CONFIG_PATH="$(cd -- "$(dirname -- "$CONFIG_PATH")" && pwd)/$(basename -- "$CONFIG_PATH")"
fi

# shellcheck source=../../lib/yaml-config.sh
source "${PODMAN_DIR}/lib/yaml-config.sh"

fail() {
    printf 'FAIL: %s\n' "$1" >&2
    exit 1
}

expect_exit_2() {
    local output status
    set +e
    output="$("$@" 2>&1)"
    status=$?
    set -e
    [[ $status -eq 2 ]] || fail "expected exit 2 from '$*', got $status: $output"
}

expect_exit_1() {
    local output status
    set +e
    output="$("$@" 2>&1)"
    status=$?
    set -e
    [[ $status -eq 1 ]] || fail "expected exit 1 from '$*', got $status: $output"
}

pod_snapshot() {
    local pod_name="$1"
    podman pod inspect --format '{{.Id}}:{{.State}}' "$pod_name" 2>/dev/null || printf 'absent'
}

bash -n "${PODMAN_DIR}/compile.sh"
bash -n "${PODMAN_DIR}/internal/compile-standard.sh"
bash -n "${PODMAN_DIR}/internal/hbuilderx-build-entrypoint.sh"
bash -n "${PODMAN_DIR}/internal/mall-dependencies-entrypoint.sh"
bash -n "${PODMAN_DIR}/internal/ubuntu-build-entrypoint.sh"
bash -n "${PODMAN_DIR}/internal/provision-database.sh"
bash -n "${PODMAN_DIR}/internal/provision-marketing-provider.sh"
for container_entrypoint in \
    "${PODMAN_DIR}/internal/hbuilderx-build-entrypoint.sh" \
    "${PODMAN_DIR}/internal/mall-dependencies-entrypoint.sh" \
    "${PODMAN_DIR}/internal/ubuntu-build-entrypoint.sh"; do
    [[ -x "$container_entrypoint" ]] || \
        fail "container entrypoint must be executable: ${container_entrypoint#${PODMAN_DIR}/}"
done
bash -n "${PODMAN_DIR}/deploy.sh"
bash -n "${PODMAN_DIR}/build-images.sh"
bash -n "${PODMAN_DIR}/stop.sh"
bash -n "${PODMAN_DIR}/operations/images/image-archives.sh"
bash -n "${PODMAN_DIR}/lib/yaml-config.sh"
bash -n "${PODMAN_DIR}/operations/database/database-backup.sh"
bash -n "${PODMAN_DIR}/operations/database/database-restore.sh"
bash -n "${PODMAN_DIR}/operations/database/database-dataset.sh"
bash -n "${PODMAN_DIR}/operations/images/build-image-archives.sh"
bash -n "${PODMAN_DIR}/operations/bpm/provision-bpm-model.sh"
bash -n "${PODMAN_DIR}/operations/bpm/provision-bpm-models.sh"
bash -n "${PODMAN_DIR}/tests/database-deploy-provision/run.sh"
bash -n "${PODMAN_DIR}/tests/marketing-provider-provision/run.sh"
for bpm_key in leave receivable reimbursement contract refund trip loan customer_visit; do
    grep -Eq "for key in .*${bpm_key}" "${PODMAN_DIR}/operations/bpm/provision-bpm-models.sh" ||
        fail "BPM aggregate provisioner does not include configured model: ${bpm_key}"
done
for acceptance_script in "${PODMAN_DIR}"/tests/acceptance/verify-*.sh; do
    bash -n "$acceptance_script"
done

mapfile -t root_scripts < <(find "$PODMAN_DIR" -maxdepth 1 -type f -name '*.sh' -printf '%f\n' | sort)
[[ "${root_scripts[*]}" == "build-images.sh compile.sh deploy.sh stop.sh" ]] || \
    fail "Podman root must contain only the four daily scripts: ${root_scripts[*]}"
rg -q 'podman_cmd pod create --replace' "${PODMAN_DIR}/deploy.sh" || \
    fail 'full deployment must use podman pod create --replace'
if rg -q 'podman_cmd pod rm' "${PODMAN_DIR}/deploy.sh"; then
    fail 'deploy.sh must not manually remove a Pod before replacement'
fi
if rg -q 'COPY[[:space:]]+(database/|podman/init/init-mysql\.sh)' "${PODMAN_DIR}/Containerfile"; then
    fail 'database SQL or initialization scripts must not be packaged into runtime images'
fi
if rg -q 'mysql,init-service|target_selected mysql|mysql_runtime' \
    "${PODMAN_DIR}/build-images.sh" "${PODMAN_DIR}/config/runtime-images"*.yaml; then
    fail 'runtime image packaging must not build a project-specific MySQL image'
fi
rg -q 'internal/provision-database\.sh' "${PODMAN_DIR}/deploy.sh" || \
    fail 'deploy.sh must run the explicit deploy-time database provisioner'
rg -q 'internal/provision-marketing-provider\.sh' "${PODMAN_DIR}/deploy.sh" || \
    fail 'deploy.sh must run the explicit marketing Provider provisioner'

while IFS= read -r build_config; do
    yaml_config_init "$build_config"
    include_targets="$(yaml_require build.include_targets)"
    exclude_targets="$(yaml_require build.exclude_targets)"
    [[ "$include_targets" =~ ^(all|none|(server|init-service|web|mall-h5)(,(server|init-service|web|mall-h5))*)$ ]] || \
        fail "invalid build.include_targets: ${build_config}"
    [[ "$exclude_targets" =~ ^(all|none|(server|init-service|web|mall-h5)(,(server|init-service|web|mall-h5))*)$ ]] || \
        fail "invalid build.exclude_targets: ${build_config}"
done < <(rg -l '^  (standard|hbuilderx): ghcr.io/elel-code/group-11-(build|hbuilderx)-ubuntu:' \
    "${PODMAN_DIR}/config" --glob '*.yaml')
if rg -q '^  (engine|server|init_service|web): (standard|hbuilderx|true|false)$' \
    "${PODMAN_DIR}/config" --glob '*.yaml'; then
    fail 'legacy engine or per-artifact build whitelist remains in a compile configuration'
fi

bash "${PODMAN_DIR}/operations/database/database-backup.sh" "${PODMAN_DIR}/config/database-backup-check.yaml"
bash "${PODMAN_DIR}/operations/database/database-restore.sh" "${PODMAN_DIR}/config/database-backup-check.yaml"
bash "${PODMAN_DIR}/operations/database/database-dataset.sh" "${PODMAN_DIR}/config/database-dataset-check.yaml"
expect_exit_2 bash "${PODMAN_DIR}/operations/database/database-dataset.sh" \
    "${SCRIPT_DIR}/fixtures/dataset-replace-unconfirmed.yaml"
expect_exit_2 bash "${PODMAN_DIR}/operations/database/database-dataset.sh" \
    "${SCRIPT_DIR}/fixtures/dataset-cleanup-not-authorized.yaml"
bash "${PODMAN_DIR}/operations/images/build-image-archives.sh" "${PODMAN_DIR}/config/build-image-archives-check.yaml"
bash "${PODMAN_DIR}/build-images.sh" "${PODMAN_DIR}/config/runtime-images-check.yaml"
if rg -q 'podman(_cmd)?[[:space:]]+build|Containerfile|target/mitedtsm|dist-prod|unpackage/dist' \
    "${PODMAN_DIR}/deploy.sh"; then
    fail 'deploy.sh must not build images or read host build artifacts'
fi

required_examples=(
    cleanup-stop.example.yaml
    cleanup-reset.example.yaml
    runtime-images.example.yaml
    runtime-images-server.example.yaml
    runtime-images-web.example.yaml
    compile-all-ubuntu-26.04.example.yaml
    verify-crm-marketing-link-click.example.yaml
    verify-crm-work-order-performance.example.yaml
    verify-crm-work-order-security.example.yaml
    bpm-provision.example.yaml
    bpm-provision-receivable.example.yaml
    bpm-provision-contract.example.yaml
    bpm-provision-amendment.example.yaml
    bpm-provision-refund.example.yaml
    bpm-provision-trip.example.yaml
    bpm-provision-loan.example.yaml
    bpm-provision-leave.example.yaml
    bpm-provision-customer-visit.example.yaml
    bpm-provision-all.example.yaml
)
for example in "${required_examples[@]}"; do
    example_path="${PODMAN_DIR}/config/${example}"
    [[ -s "$example_path" ]] || fail "required example configuration is missing: $example"
    if git -C "${PODMAN_DIR}/.." check-ignore -q "podman/config/${example}"; then
        fail "example configuration must not be ignored: $example"
    fi
    git -C "${PODMAN_DIR}/.." ls-files --error-unmatch "podman/config/${example}" >/dev/null 2>&1 || \
        fail "required example configuration must be tracked by Git: $example"
done

validate_sql_manifest() {
    local manifest="$1"
    local manifest_dir entry resolved

    [[ -s "$manifest" ]] || fail "SQL manifest is missing: $manifest"
    manifest_dir="$(dirname -- "$manifest")"
    while IFS= read -r entry || [[ -n "$entry" ]]; do
        [[ -n "$entry" && "$entry" != \#* ]] || continue
        [[ "$entry" != /* ]] || fail "SQL manifest contains an absolute path: $entry"
        resolved="$(realpath -m -- "${manifest_dir}/${entry}")"
        [[ "$resolved" == "${DATABASE_DIR}/"* ]] || fail "SQL manifest escapes database root: $entry"
        [[ -s "$resolved" ]] || fail "SQL manifest entry is missing: $entry"
        [[ "$resolved" != "${DATABASE_DIR}/maintenance/cleanup/"* ]] || \
            fail "cleanup SQL must not be automated: $entry"
        [[ "$resolved" != "${DATABASE_DIR}/teardown/"* ]] || \
            fail "teardown SQL must not be automated: $entry"
    done < "$manifest"
}

bootstrap_manifest="${DATABASE_DIR}/manifests/mysql-bootstrap.manifest"
compatibility_manifest="${DATABASE_DIR}/manifests/mysql-compatibility.manifest"
validate_sql_manifest "$bootstrap_manifest"
validate_sql_manifest "$compatibility_manifest"

validate_dataset_manifest() {
    local manifest="$1" manifest_dir entry resolved
    [[ -s "$manifest" ]] || fail "dataset manifest is missing: $manifest"
    manifest_dir="$(dirname -- "$manifest")"
    while IFS= read -r entry || [[ -n "$entry" ]]; do
        [[ -n "$entry" && "$entry" != \#* ]] || continue
        [[ "$entry" != /* ]] || fail "dataset manifest contains an absolute path: $entry"
        resolved="$(realpath -m -- "${manifest_dir}/${entry}")"
        [[ "$resolved" == "${DATABASE_DIR}/"* ]] || fail "dataset manifest escapes database root: $entry"
        [[ -s "$resolved" ]] || fail "dataset entry is missing: $entry"
        [[ "$resolved" != "${DATABASE_DIR}/teardown/"* ]] || fail "dataset must not execute teardown SQL: $entry"
    done < "$manifest"
}

for dataset_manifest in "${DATABASE_DIR}"/datasets/*.manifest; do
    validate_dataset_manifest "$dataset_manifest"
done
rg -q --fixed-strings '../maintenance/cleanup/cleanup-upstream-crm-demo.sql' \
    "${DATABASE_DIR}/datasets/none.manifest" || fail 'none dataset must remove upstream CRM demo rows'
if rg -q 'maintenance/cleanup' "$bootstrap_manifest" "$compatibility_manifest"; then
    fail 'cleanup SQL must never enter bootstrap or compatibility manifests'
fi

missing_from_bootstrap="$(comm -23 \
    <(sed '/^[[:space:]]*#/d; /^[[:space:]]*$/d' "$compatibility_manifest" | sort) \
    <(sed '/^[[:space:]]*#/d; /^[[:space:]]*$/d' "$bootstrap_manifest" | sort))"
[[ -z "$missing_from_bootstrap" ]] || fail \
    "Compatibility migration missing from empty-database bootstrap: ${missing_from_bootstrap//$'\n'/, }"

if rg -ni '^[[:space:]]*(CREATE|ALTER|UPDATE|DELETE|DROP|TRUNCATE)[[:space:]]' \
    "${DATABASE_DIR}/seed"/*.sql; then
    fail 'seed SQL contains schema, cleanup, or teardown statements'
fi

missing_crm_migrations="$(comm -23 \
    <(find "${DATABASE_DIR}/migrations" -maxdepth 1 -type f -name 'new-crm-*.sql' -printf '%f\n' | sort) \
    <(sed -n 's#^.*/\(new-crm-[^/]*\.sql\)$#\1#p' \
        "$compatibility_manifest" | sort))"
[[ -z "$missing_crm_migrations" ]] || fail \
    "CRM compatibility migrations missing from manifest: ${missing_crm_migrations//$'\n'/, }"

yaml_config_init "${SCRIPT_DIR}/fixtures/parser-valid.yaml"
[[ "$(yaml_require sample.plain)" == "value" ]] || fail 'plain scalar parsing'
[[ "$(yaml_require sample.quoted)" == "value # kept" ]] || fail 'quoted comment parsing'
[[ "$(yaml_bool sample.enabled)" == "true" ]] || fail 'boolean parsing'
[[ "$(yaml_port sample.port)" == "18080" ]] || fail 'port parsing'

yaml_config_init "${SCRIPT_DIR}/fixtures/parser-duplicate.yaml"
if yaml_require sample.value >/dev/null 2>&1; then
    fail 'duplicate keys must be rejected'
fi

yaml_config_init "${SCRIPT_DIR}/fixtures/parser-invalid-depth.yaml"
if yaml_require sample.nested >/dev/null 2>&1; then
    fail 'mappings deeper than two levels must be rejected'
fi

expect_exit_2 bash "${PODMAN_DIR}/deploy.sh"
expect_exit_2 bash "${PODMAN_DIR}/deploy.sh" "$CONFIG_PATH" extra
expect_exit_2 bash "${PODMAN_DIR}/compile.sh"
expect_exit_2 bash "${PODMAN_DIR}/compile.sh" \
    "${PODMAN_DIR}/config/build-ubuntu-26.04.yaml" extra
expect_exit_2 bash "${PODMAN_DIR}/compile.sh" \
    "${SCRIPT_DIR}/fixtures/compile-invalid-selector.yaml"
expect_exit_2 bash "${PODMAN_DIR}/compile.sh" \
    "${SCRIPT_DIR}/fixtures/compile-empty-selection.yaml"
expect_exit_2 bash "${PODMAN_DIR}/internal/provision-marketing-provider.sh" \
    "${SCRIPT_DIR}/fixtures/provider-placeholder-secret.yaml"
expect_exit_2 bash "${PODMAN_DIR}/build-images.sh"
expect_exit_2 bash "${PODMAN_DIR}/build-images.sh" \
    "${PODMAN_DIR}/config/runtime-images-check.yaml" extra
expect_exit_2 bash "${PODMAN_DIR}/stop.sh"
expect_exit_2 bash "${PODMAN_DIR}/stop.sh" "$CONFIG_PATH" extra
expect_exit_2 bash "${PODMAN_DIR}/stop.sh" \
    "${SCRIPT_DIR}/fixtures/cleanup-reset-unconfirmed.yaml"
expect_exit_2 bash "${PODMAN_DIR}/operations/images/image-archives.sh"
expect_exit_2 bash "${PODMAN_DIR}/operations/images/image-archives.sh" "$CONFIG_PATH" extra
expect_exit_1 bash "${PODMAN_DIR}/operations/images/image-archives.sh" \
    "${SCRIPT_DIR}/fixtures/archive-check-missing.yaml"

yaml_config_init "$CONFIG_PATH"
dataset_name="$(yaml_require mysql.dataset)"
[[ "$dataset_name" =~ ^[a-z0-9][a-z0-9._-]*$ ]] || fail 'mysql.dataset has invalid characters'
[[ -s "${DATABASE_DIR}/datasets/${dataset_name}.manifest" ]] || fail 'selected dataset manifest is missing'
[[ "$(yaml_bool security.mock_login_enabled)" == "false" ]] || fail 'mock login must be explicitly disabled'
[[ "$(yaml_positive_integer security.password_encoder_length)" -ge 10 ]] || fail 'BCrypt strength must be explicit'
[[ "$(yaml_bool security.xss_enabled)" == "true" ]] || fail 'XSS filtering must be explicitly enabled'
[[ "$(yaml_require security.actuator_exposure)" == "health,info" ]] || fail 'Actuator exposure must be explicit'
[[ "$(yaml_bool security.api_docs_enabled)" == "false" ]] || fail 'API documentation must be explicitly disabled'
[[ "$(yaml_bool integration.justauth_enabled)" == "false" ]] || fail 'JustAuth startup must be explicit'
pod_name="$(yaml_require deployment.pod_name)"
before="$(pod_snapshot "$pod_name")"
bash "${PODMAN_DIR}/deploy.sh" "$CONFIG_PATH"
bash "${PODMAN_DIR}/stop.sh" "$CONFIG_PATH"
env POD_NAME=ignored SERVER_PORT=invalid IMAGE_SOURCE=invalid USE_HOST_PROXY=invalid \
    bash "${PODMAN_DIR}/deploy.sh" "$CONFIG_PATH"
env POD_NAME=ignored STOP_TIMEOUT=invalid \
    bash "${PODMAN_DIR}/stop.sh" "$CONFIG_PATH"
after="$(pod_snapshot "$pod_name")"
[[ "$after" == "$before" ]] || fail "check modes changed Pod state: before=$before after=$after"

printf 'Runtime YAML configuration tests passed. Pod state remained %s.\n' "$after"
