#!/usr/bin/env bash

set -Eeuo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
PODMAN_DIR="$(cd -- "${SCRIPT_DIR}/../.." && pwd)"
DATABASE_DIR="$(cd -- "${PODMAN_DIR}/../database" && pwd)"
WEB_DIR="$(cd -- "${PODMAN_DIR}/../Web" && pwd)"
MALL_DIR="$(cd -- "${PODMAN_DIR}/../MallFrontend" && pwd)"

usage() {
    printf 'Usage: bash ./tests/runtime-config/run.sh <runtime-config.kdl>\n' >&2
}

[[ $# -eq 1 ]] || {
    usage
    exit 2
}

CONFIG_PATH="$1"
if [[ "$CONFIG_PATH" != /* ]]; then
    CONFIG_PATH="$(cd -- "$(dirname -- "$CONFIG_PATH")" && pwd)/$(basename -- "$CONFIG_PATH")"
fi

# shellcheck source=../../lib/kdl-config.sh
source "${PODMAN_DIR}/lib/kdl-config.sh"

TEMP_CONFIGS=()
cleanup() {
    ((${#TEMP_CONFIGS[@]} == 0)) || rm -f -- "${TEMP_CONFIGS[@]}"
}
trap cleanup EXIT

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
rg -q 'Removing retired pnpm layout from the Web dependency cache volume' \
    "${PODMAN_DIR}/internal/ubuntu-build-entrypoint.sh" || \
    fail 'Web dependency entrypoint must remove a detected retired pnpm cache layout'
rg -q 'Removing retired pnpm layout from the Mall dependency cache volume' \
    "${PODMAN_DIR}/internal/mall-dependencies-entrypoint.sh" || \
    fail 'Mall dependency entrypoint must remove a detected retired pnpm cache layout'
for dependency_entrypoint in \
    "${PODMAN_DIR}/internal/ubuntu-build-entrypoint.sh" \
    "${PODMAN_DIR}/internal/mall-dependencies-entrypoint.sh"; do
    rg -q 'install --node-modules-dir=auto --quiet' "$dependency_entrypoint" || \
        fail "Deno dependency installation must use deterministic quiet output: $dependency_entrypoint"
done
rg -q 'BUILD_PAY_TESTS' "${PODMAN_DIR}/internal/compile-standard.sh" || \
    fail 'compile-standard.sh must pass the explicit Pay test selector'
rg -q 'mitedtsm-module-pay/target/site/jacoco/jacoco.csv' \
    "${PODMAN_DIR}/internal/ubuntu-build-entrypoint.sh" || \
    fail 'Ubuntu entrypoint must preserve Pay JaCoCo evidence'
for project_dir in "$WEB_DIR" "$MALL_DIR"; do
    [[ -s "${project_dir}/deno.json" && -s "${project_dir}/deno.lock" ]] || \
        fail "Deno manifest and frozen lockfile are required: ${project_dir#${PODMAN_DIR}/../}"
    [[ ! -e "${project_dir}/pnpm-lock.yaml" \
        && ! -e "${project_dir}/pnpm-workspace.yaml" \
        && ! -e "${project_dir}/package-lock.json" \
        && ! -e "${project_dir}/yarn.lock" ]] || \
        fail "retired Node package-manager lock or workspace file remains: ${project_dir#${PODMAN_DIR}/../}"
done
[[ -s "${WEB_DIR}/src/components/Icon/src/offline-icon-collections.generated.json" ]] || \
    fail 'committed offline Iconify snapshot is required'
rg -q 'verify:offline-icons' "${WEB_DIR}/package.json" || \
    fail 'Web build scripts must verify the offline icon snapshot'
if rg -q '@iconify/iconify' "${WEB_DIR}/package.json" "${WEB_DIR}/src/components/Icon/src/Icon.vue"; then
    fail 'Web must not restore the Iconify browser network runtime'
fi
rg -q '@iconify/utils' "${WEB_DIR}/package.json" || \
    fail 'Web must render the committed icon snapshot with pure Iconify utilities'
rg -q 'rolldownOptions' "${WEB_DIR}/vite.config.mts" || \
    fail 'Vite 8 must use the native Rolldown options key'
rg -q 'pluginTimings: false' "${WEB_DIR}/vite.config.mts" || \
    fail 'Vite plugin timing diagnostics must use an explicit zero-warning policy'
if rg -n 'pnpm|pnpm_store|pnpm-store' "${PODMAN_DIR}/config" --glob '*.kdl'; then
    fail 'current KDL configurations must not contain retired pnpm fields or volumes'
fi
if rg -n '(^|[[:space:]])(nodejs|node|npm|pnpm)([[:space:]]|$)' \
    "${PODMAN_DIR}/Containerfile.build-ubuntu"; then
    fail 'standard Ubuntu build image must not install Node, npm, or pnpm'
fi
rg -q '^ARG DENO_BIN_IMAGE=.*:bin-[0-9]+\.[0-9]+\.[0-9]+@sha256:[0-9a-f]{64}$' \
    "${PODMAN_DIR}/Containerfile.build-ubuntu" || \
    fail 'standard build image must pin the official Deno binary image by version and digest'
if jq -er '.scripts | to_entries[] | select(.value | test("(^|[;&|[:space:]])(pnpm|npx|node)([;&|[:space:]]|$)"))' \
    "${WEB_DIR}/package.json" "${MALL_DIR}/package.json" >/dev/null; then
    fail 'project package scripts must not invoke retired pnpm, npx, or Node compatibility commands'
fi
[[ -s "${WEB_DIR}/vite.config.mts" && ! -e "${WEB_DIR}/vite.config.ts" ]] || \
    fail 'Web Vite configuration must use the native ESM .mts entry'
if rg -n '(^|[^.[:alnum:]_])(map-merge|map-get|map-has-key|type-of|desaturate|darken|mix|nth|append|zip)\(' \
    "${MALL_DIR}/sheep/scss" --glob '*.scss'; then
    fail 'Mall-owned Sass must not reintroduce deprecated global built-in functions'
fi
if rg -n '@import[[:space:]]' \
    "${MALL_DIR}/uni.scss" "${MALL_DIR}/App.vue" "${MALL_DIR}/sheep" \
    --glob '*.scss' --glob '*.vue'; then
    fail 'Mall-owned Sass must use the module system instead of deprecated @import rules'
fi
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
bash -n "${PODMAN_DIR}/lib/kdl-config.sh"
bash -n "${PODMAN_DIR}/operations/database/database-backup.sh"
bash -n "${PODMAN_DIR}/operations/database/database-restore.sh"
bash -n "${PODMAN_DIR}/operations/database/database-dataset.sh"
bash -n "${PODMAN_DIR}/operations/database/generate-demo-dataset.sh"
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
    "${PODMAN_DIR}/build-images.sh" "${PODMAN_DIR}/config/runtime-images"*.kdl; then
    fail 'runtime image packaging must not build a project-specific MySQL image'
fi
rg -q 'internal/provision-database\.sh' "${PODMAN_DIR}/deploy.sh" || \
    fail 'deploy.sh must run the explicit deploy-time database provisioner'
rg -q 'internal/provision-marketing-provider\.sh' "${PODMAN_DIR}/deploy.sh" || \
    fail 'deploy.sh must run the explicit marketing Provider provisioner'

while IFS= read -r build_config; do
    kdl_config_init "$build_config"
    include_targets="$(kdl_require build.include_targets)"
    exclude_targets="$(kdl_require build.exclude_targets)"
    [[ "$include_targets" =~ ^(all|none|(server|init-service|web|mall-h5)(,(server|init-service|web|mall-h5))*)$ ]] || \
        fail "invalid build.include_targets: ${build_config}"
    [[ "$exclude_targets" =~ ^(all|none|(server|init-service|web|mall-h5)(,(server|init-service|web|mall-h5))*)$ ]] || \
        fail "invalid build.exclude_targets: ${build_config}"
    if [[ -n "$(kdl_get web.test_script)" || -n "$(kdl_get web.coverage_enabled)" ]]; then
        coverage_enabled="$(kdl_require web.coverage_enabled)"
        [[ "$coverage_enabled" == "true" || "$coverage_enabled" == "false" ]] || \
            fail "web.coverage_enabled must be explicit boolean: ${build_config}"
        coverage_threshold="$(kdl_require web.coverage_threshold)"
        [[ "$coverage_threshold" =~ ^[0-9]+$ && "$coverage_threshold" -le 100 ]] || \
            fail "web.coverage_threshold must be an integer from 0 to 100: ${build_config}"
        if [[ "$coverage_enabled" == "true" && -z "$(kdl_get web.test_script)" ]]; then
            fail "web.coverage_enabled requires web.test_script: ${build_config}"
        fi
    fi
done < <(rg -l '^  (standard|hbuilderx) "ghcr.io/elel-code/group-11-(build|hbuilderx)-ubuntu:' \
    "${PODMAN_DIR}/config" --glob '*.kdl')
if rg -q '^  (engine|server|init_service|web) ("(standard|hbuilderx)"|#true|#false)$' \
    "${PODMAN_DIR}/config" --glob '*.kdl'; then
    fail 'legacy engine or per-artifact build whitelist remains in a compile configuration'
fi
if rg --pcre2 -n '^(ARG (RUNTIME|NGINX)_BASE_IMAGE=docker\.io/|  (runtime|nginx|redis|rabbitmq|tdengine|mysql)_base "docker\.io/)(?!.*@sha256:[0-9a-f]{64}"?$)' \
    "${PODMAN_DIR}/Containerfile" \
    "${PODMAN_DIR}/config/runtime-local-check.kdl" \
    "${PODMAN_DIR}/config/runtime-images-check.kdl" \
    "${PODMAN_DIR}/config/runtime-images.example.kdl" \
    "${PODMAN_DIR}/config/runtime-images-server.example.kdl" \
    "${PODMAN_DIR}/config/runtime-images-web.example.kdl"; then
    fail 'tracked runtime base images must use an exact version plus sha256 digest'
fi

bash "${PODMAN_DIR}/operations/database/database-backup.sh" "${PODMAN_DIR}/config/database-backup-check.kdl"
bash "${PODMAN_DIR}/operations/database/database-restore.sh" "${PODMAN_DIR}/config/database-backup-check.kdl"
bash "${PODMAN_DIR}/operations/database/database-dataset.sh" "${PODMAN_DIR}/config/database-dataset-check.kdl"
expect_exit_2 bash "${PODMAN_DIR}/operations/database/database-dataset.sh" \
    "${SCRIPT_DIR}/fixtures/dataset-replace-without-cleanup.kdl"
expect_exit_2 bash "${PODMAN_DIR}/operations/database/database-dataset.sh" \
    "${SCRIPT_DIR}/fixtures/dataset-insert-with-cleanup.kdl"
bash "${PODMAN_DIR}/operations/images/build-image-archives.sh" "${PODMAN_DIR}/config/build-image-archives-check.kdl"
bash "${PODMAN_DIR}/build-images.sh" "${PODMAN_DIR}/config/runtime-images-check.kdl"
if rg -q 'podman(_cmd)?[[:space:]]+build|Containerfile|target/mitedtsm|dist-prod|unpackage/dist' \
    "${PODMAN_DIR}/deploy.sh"; then
    fail 'deploy.sh must not build images or read host build artifacts'
fi
if rg -q 'generate-demo-dataset' "${PODMAN_DIR}/deploy.sh"; then
    fail 'deployment must never generate demo datasets'
fi

required_examples=(
    cleanup-stop.example.kdl
    cleanup-reset.example.kdl
    runtime-images.example.kdl
    runtime-images-server.example.kdl
    runtime-images-web.example.kdl
    compile-all-ubuntu-26.04.example.kdl
    test-pay-ubuntu-26.04.kdl
    verify-crm-marketing-link-click.example.kdl
    verify-crm-work-order-performance.example.kdl
    verify-crm-work-order-security.example.kdl
    generate-demo-dataset.example.kdl
    bpm-provision.example.kdl
    bpm-provision-receivable.example.kdl
    bpm-provision-contract.example.kdl
    bpm-provision-amendment.example.kdl
    bpm-provision-refund.example.kdl
    bpm-provision-trip.example.kdl
    bpm-provision-loan.example.kdl
    bpm-provision-leave.example.kdl
    bpm-provision-customer-visit.example.kdl
    bpm-provision-all.example.kdl
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

kdl_config_init "${SCRIPT_DIR}/fixtures/parser-valid.kdl"
[[ "$(kdl_require sample.plain)" == "value" ]] || fail 'plain scalar parsing'
[[ "$(kdl_require sample.quoted)" == "value # kept" ]] || fail 'quoted comment parsing'
[[ "$(kdl_bool sample.enabled)" == "true" ]] || fail 'boolean parsing'
[[ "$(kdl_port sample.port)" == "18080" ]] || fail 'port parsing'

expect_exit_2 kdl_config_init "${SCRIPT_DIR}/fixtures/parser-duplicate.kdl"
expect_exit_2 kdl_config_init "${SCRIPT_DIR}/fixtures/parser-invalid-depth.kdl"

expect_exit_2 bash "${PODMAN_DIR}/deploy.sh"
expect_exit_2 bash "${PODMAN_DIR}/deploy.sh" "$CONFIG_PATH" extra
expect_exit_2 bash "${PODMAN_DIR}/compile.sh"
expect_exit_2 bash "${PODMAN_DIR}/compile.sh" \
    "${PODMAN_DIR}/config/build-ubuntu-26.04.kdl" extra
expect_exit_2 bash "${PODMAN_DIR}/compile.sh" \
    "${SCRIPT_DIR}/fixtures/compile-invalid-selector.kdl"
expect_exit_2 bash "${PODMAN_DIR}/compile.sh" \
    "${SCRIPT_DIR}/fixtures/compile-empty-selection.kdl"
expect_exit_2 bash "${PODMAN_DIR}/compile.sh" \
    "${SCRIPT_DIR}/fixtures/compile-pay-coverage-without-tests.kdl"
expect_exit_2 bash "${PODMAN_DIR}/internal/provision-marketing-provider.sh" \
    "${SCRIPT_DIR}/fixtures/provider-placeholder-secret.kdl"
invalid_sql_root_config="$(mktemp "${PODMAN_DIR}/config/.invalid-sql-root.XXXXXX.kdl")"
TEMP_CONFIGS+=("$invalid_sql_root_config")
cp "$CONFIG_PATH" "$invalid_sql_root_config"
kdl_set_file "$invalid_sql_root_config" mysql.sql_root string ../../Server
expect_exit_2 bash "${PODMAN_DIR}/deploy.sh" "$invalid_sql_root_config"
expect_exit_2 bash "${PODMAN_DIR}/build-images.sh"
expect_exit_2 bash "${PODMAN_DIR}/build-images.sh" \
    "${PODMAN_DIR}/config/runtime-images-check.kdl" extra
expect_exit_2 bash "${PODMAN_DIR}/stop.sh"
expect_exit_2 bash "${PODMAN_DIR}/stop.sh" "$CONFIG_PATH" extra
expect_exit_2 bash "${PODMAN_DIR}/stop.sh" \
    "${SCRIPT_DIR}/fixtures/cleanup-reset-unconfirmed.kdl"
expect_exit_2 bash "${PODMAN_DIR}/operations/images/image-archives.sh"
expect_exit_2 bash "${PODMAN_DIR}/operations/images/image-archives.sh" "$CONFIG_PATH" extra
expect_exit_1 bash "${PODMAN_DIR}/operations/images/image-archives.sh" \
    "${SCRIPT_DIR}/fixtures/archive-check-missing.kdl"

kdl_config_init "$CONFIG_PATH"
dataset_name="$(kdl_require mysql.dataset)"
[[ "$dataset_name" =~ ^[a-z0-9][a-z0-9._-]*$ ]] || fail 'mysql.dataset has invalid characters'
[[ -s "${DATABASE_DIR}/datasets/${dataset_name}.manifest" ]] || fail 'selected dataset manifest is missing'
[[ "$(realpath -m -- "$(kdl_path mysql.sql_root)")" == "$DATABASE_DIR" ]] || \
    fail 'mysql.sql_root must explicitly resolve to the database lifecycle root'
[[ "$(kdl_require mysql.host)" == "127.0.0.1" ]] || fail 'Pod-local MySQL host must be explicit'
[[ "$(kdl_port mysql.port)" == "3306" ]] || fail 'MySQL port must be explicit'
[[ "$(kdl_require mysql.administration_username)" == "root" ]] || fail 'MySQL administrator must be explicit'
[[ -n "$(kdl_require mysql.application_username)" ]] || fail 'MySQL application user must be explicit'
[[ -n "$(kdl_require mysql.jdbc_parameters)" ]] || fail 'MySQL JDBC parameters must be explicit'
rg -q 'SPRING_DATASOURCE_DYNAMIC_DATASOURCE_MASTER_URL' "${PODMAN_DIR}/deploy.sh" || \
    fail 'deploy.sh must inject the explicit JDBC URL'
rg -q 'DATABASE_ROOT=.*kdl_path mysql.sql_root' "${PODMAN_DIR}/internal/provision-database.sh" || \
    fail 'database provision must load its SQL root from KDL'
if rg -q 'url:[[:space:]]+jdbc:mysql://127\.0\.0\.1:3306/mitedtsm_database' \
    "${PODMAN_DIR}/../Server/mitedtsm-server/src/main/resources/application-local.yaml"; then
    fail 'Podman local profile must not retain a hardcoded master JDBC URL'
fi
[[ "$(kdl_bool security.mock_login_enabled)" == "false" ]] || fail 'mock login must be explicitly disabled'
[[ "$(kdl_positive_integer security.password_encoder_length)" -ge 10 ]] || fail 'BCrypt strength must be explicit'
[[ "$(kdl_bool security.xss_enabled)" == "true" ]] || fail 'XSS filtering must be explicitly enabled'
[[ "$(kdl_require security.actuator_exposure)" == "health,info" ]] || fail 'Actuator exposure must be explicit'
[[ "$(kdl_bool security.api_docs_enabled)" == "false" ]] || fail 'API documentation must be explicitly disabled'
[[ "$(kdl_bool integration.justauth_enabled)" == "false" ]] || fail 'JustAuth startup must be explicit'
pod_name="$(kdl_require deployment.pod_name)"
before="$(pod_snapshot "$pod_name")"
bash "${PODMAN_DIR}/deploy.sh" "$CONFIG_PATH"
bash "${PODMAN_DIR}/stop.sh" "$CONFIG_PATH"
env POD_NAME=ignored SERVER_PORT=invalid IMAGE_SOURCE=invalid USE_HOST_PROXY=invalid \
    bash "${PODMAN_DIR}/deploy.sh" "$CONFIG_PATH"
env POD_NAME=ignored STOP_TIMEOUT=invalid \
    bash "${PODMAN_DIR}/stop.sh" "$CONFIG_PATH"
after="$(pod_snapshot "$pod_name")"
[[ "$after" == "$before" ]] || fail "check modes changed Pod state: before=$before after=$after"

printf 'Runtime KDL configuration tests passed. Pod state remained %s.\n' "$after"
