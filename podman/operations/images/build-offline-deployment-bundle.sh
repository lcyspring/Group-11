#!/usr/bin/env bash
# Build a transferable, source-free Podman/Docker Compose deployment directory.

set -Eeuo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
PODMAN_DIR="$(cd -- "${SCRIPT_DIR}/../.." && pwd)"
PROJECT_ROOT="$(cd -- "${PODMAN_DIR}/.." && pwd)"

usage() {
    printf 'Usage: bash ./operations/images/build-offline-deployment-bundle.sh <config.kdl>\n' >&2
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

MODE="$(kdl_require operation.mode)"
OUTPUT_DIR="$(realpath -m "$(kdl_path bundle.output_dir)")"
OVERWRITE="$(kdl_bool bundle.overwrite)"
RUNTIME_CONFIG="$(realpath -m "$(kdl_path source.runtime_config)")"

case "$MODE" in
    check|build) ;;
    *)
        printf 'operation.mode must be check or build; got: %s\n' "$MODE" >&2
        exit 2
        ;;
esac

case "$OUTPUT_DIR" in
    /|"$PROJECT_ROOT"|"$PODMAN_DIR")
        printf 'Refusing unsafe bundle.output_dir: %s\n' "$OUTPUT_DIR" >&2
        exit 2
        ;;
esac

require_command() {
    command -v "$1" >/dev/null 2>&1 || {
        printf 'Required command is unavailable: %s\n' "$1" >&2
        exit 1
    }
}

require_file() {
    [[ -s "$1" ]] || {
        printf 'Required bundle source file is missing or empty: %s\n' "$1" >&2
        exit 1
    }
}

for command in podman jq sha256sum realpath; do
    require_command "$command"
done
require_file "$RUNTIME_CONFIG"
require_file "${PODMAN_DIR}/tools/bin/dasel"

[[ "$(uname -m)" == "x86_64" ]] || {
    printf 'The bundled dasel binary and this delivery profile currently require x86_64.\n' >&2
    exit 1
}

rootless="$(podman info --format '{{.Host.Security.Rootless}}')" || {
    printf 'Podman is installed but unavailable to the current user.\n' >&2
    exit 1
}
[[ "$rootless" == "true" ]] || {
    printf 'Run this script as the normal rootless Podman user.\n' >&2
    exit 1
}

# Load the selected real runtime configuration after preserving bundle options.
kdl_config_init "$RUNTIME_CONFIG"

SQL_ROOT="$(realpath -m "$(kdl_path mysql.sql_root)")"
BPM_MANIFEST="$(realpath -m "$(kdl_path bpm.provision_manifest)")"
NOTIFICATION_CONFIG="$(realpath -m "$(kdl_path bpm.notification_template_config)")"
BPM_PROVISION_AFTER_START="$(kdl_bool bpm.provision_after_start)"
DEMO_DATASET="$(kdl_require mysql.dataset)"
DEMO_DATASET_MANIFEST="$(realpath -m "$(kdl_path mysql.dataset_manifest)")"
DEMO_DATASET_MODE="$(kdl_require mysql.dataset_mode)"

[[ "$SQL_ROOT" == "${PROJECT_ROOT}/database" ]] || {
    printf 'Portable bundles require mysql.sql_root to resolve to %s; got %s\n' \
        "${PROJECT_ROOT}/database" "$SQL_ROOT" >&2
    exit 2
}
[[ "$BPM_PROVISION_AFTER_START" == "true" ]] || {
    printf 'Portable first deployment requires bpm.provision_after_start=true.\n' >&2
    exit 2
}
require_file "$BPM_MANIFEST"
require_file "$NOTIFICATION_CONFIG"
require_file "$DEMO_DATASET_MANIFEST"
require_file "${SQL_ROOT}/datasets/none.manifest"
[[ "$DEMO_DATASET_MANIFEST" == "${SQL_ROOT}/"* ]] || {
    printf 'The selected demo dataset manifest must be under mysql.sql_root: %s\n' \
        "$DEMO_DATASET_MANIFEST" >&2
    exit 2
}
case "$DEMO_DATASET_MODE" in
    preserve|insert|replace) ;;
    *) printf 'mysql.dataset_mode must be preserve, insert, or replace.\n' >&2; exit 2 ;;
esac

images=(
    "$(kdl_require image.redis_base)"
    "$(kdl_require image.rabbitmq_base)"
    "$(kdl_require image.tdengine_base)"
    "$(kdl_require image.mysql_base)"
    "$(kdl_require image.init_runtime)"
    "$(kdl_require image.server_runtime)"
    "$(kdl_require image.web_runtime)"
    "$(kdl_require image.mall_runtime)"
)
export_images=(
    "localhost/mitedtsm-offline-redis:6.2.22"
    "localhost/mitedtsm-offline-rabbitmq:3.13.7"
    "localhost/mitedtsm-offline-tdengine:3.3.6.0"
    "localhost/mitedtsm-offline-mysql:8.0.46"
    "${images[4]}"
    "${images[5]}"
    "${images[6]}"
    "${images[7]}"
)
archives=(
    "$(kdl_require archive.redis_base)"
    "$(kdl_require archive.rabbitmq_base)"
    "$(kdl_require archive.tdengine_base)"
    "$(kdl_require archive.mysql_base)"
    "$(kdl_require archive.init_runtime)"
    "$(kdl_require archive.server_runtime)"
    "$(kdl_require archive.web_runtime)"
    "$(kdl_require archive.mall_runtime)"
)

for ((index = 0; index < ${#images[@]}; index++)); do
    image="${images[index]}"
    archive="${archives[index]}"
    [[ "$archive" == "$(basename -- "$archive")" && "$archive" == *.tar ]] || {
        printf 'Archive filename must be a plain .tar basename: %s\n' "$archive" >&2
        exit 2
    }
    podman image exists "$image" || {
        printf 'Required deployment image is unavailable locally: %s\n' "$image" >&2
        exit 1
    }
done

# Validate all model files named by the aggregate manifest before any output is written.
mapfile -t MODEL_CONFIG_REFS < <(
    "${PODMAN_DIR}/tools/bin/dasel" -i kdl -o json --compact --root < "$BPM_MANIFEST" |
        jq -r '.models[]'
)
[[ ${#MODEL_CONFIG_REFS[@]} -gt 0 ]] || {
    printf 'BPM aggregate manifest contains no model configurations: %s\n' "$BPM_MANIFEST" >&2
    exit 1
}
for model_ref in "${MODEL_CONFIG_REFS[@]}"; do
    [[ "$model_ref" == "$(basename -- "$model_ref")" ]] || {
        printf 'BPM model reference must be a plain filename: %s\n' "$model_ref" >&2
        exit 2
    }
    require_file "$(dirname -- "$BPM_MANIFEST")/${model_ref}"
done

if [[ "$MODE" == "check" ]]; then
    printf 'Offline deployment bundle preflight passed: 8 images and %s BPM model configs are available.\n' \
        "${#MODEL_CONFIG_REFS[@]}"
    printf 'No directory or image archive was written.\n'
    exit 0
fi

if [[ -e "$OUTPUT_DIR" && "$OVERWRITE" != "true" ]]; then
    printf 'Bundle output already exists and overwrite=false: %s\n' "$OUTPUT_DIR" >&2
    exit 1
fi

mkdir -p "$(dirname -- "$OUTPUT_DIR")"
TEMP_DIR="$(mktemp -d "$(dirname -- "$OUTPUT_DIR")/.mitedtsm-offline.XXXXXX")"
cleanup() {
    [[ -z "${TEMP_DIR:-}" || ! -e "$TEMP_DIR" ]] || rm -rf -- "$TEMP_DIR"
}
trap cleanup EXIT

mkdir -p \
    "${TEMP_DIR}/docker" \
    "${TEMP_DIR}/podman/lib" \
    "${TEMP_DIR}/podman/internal" \
    "${TEMP_DIR}/podman/operations/bpm" \
    "${TEMP_DIR}/podman/config" \
    "${TEMP_DIR}/podman/tools/bin" \
    "${TEMP_DIR}/podman/images"

cp "${PODMAN_DIR}/deploy.sh" "${TEMP_DIR}/podman/deploy.sh"
cp "${PODMAN_DIR}/lib/kdl-config.sh" "${TEMP_DIR}/podman/lib/kdl-config.sh"
cp "${PODMAN_DIR}/internal/provision-database.sh" "${TEMP_DIR}/podman/internal/provision-database.sh"
cp "${PODMAN_DIR}/internal/provision-marketing-provider.sh" "${TEMP_DIR}/podman/internal/provision-marketing-provider.sh"
cp "${PODMAN_DIR}/internal/provision-bpm-notifications.sh" "${TEMP_DIR}/podman/internal/provision-bpm-notifications.sh"
cp "${PODMAN_DIR}/operations/bpm/provision-bpm-model.sh" "${TEMP_DIR}/podman/operations/bpm/provision-bpm-model.sh"
cp "${PODMAN_DIR}/operations/bpm/provision-bpm-models.sh" "${TEMP_DIR}/podman/operations/bpm/provision-bpm-models.sh"
cp "${PODMAN_DIR}/tools/bin/dasel" "${TEMP_DIR}/podman/tools/bin/dasel"
cp -a "$SQL_ROOT" "${TEMP_DIR}/database"

# Older generated datasets stored source-host absolute paths in SHA256SUMS.
# Keep the existing digests but make every copied checksum entry portable.
while IFS= read -r -d '' checksum_file; do
    sed -E -i 's#^([0-9a-fA-F]{64})  .*/#\1  #' "$checksum_file"
done < <(find "${TEMP_DIR}/database" -type f -name SHA256SUMS -print0)

for wrapper in README_ZH.md configure.sh configure-host.sh container-engine.sh deploy.sh start.sh stop.sh verify.sh; do
    cp "${PODMAN_DIR}/offline/${wrapper}" "${TEMP_DIR}/${wrapper}"
done
cp "${PODMAN_DIR}/offline/deployment-config.example.kdl" "${TEMP_DIR}/deployment-config.example.kdl"
cp "${PODMAN_DIR}/offline/docker/compose.yaml" "${TEMP_DIR}/docker/compose.yaml"
cp "${PODMAN_DIR}/offline/docker/runtime.sh" "${TEMP_DIR}/docker/runtime.sh"

TARGET_RUNTIME="${TEMP_DIR}/podman/config/runtime-template.kdl"
cp "$RUNTIME_CONFIG" "$TARGET_RUNTIME"
kdl_set_file "$TARGET_RUNTIME" image.source string archive
kdl_set_file "$TARGET_RUNTIME" image.archive_dir string ../images
kdl_set_file "$TARGET_RUNTIME" image.redis_base string "${export_images[0]}"
kdl_set_file "$TARGET_RUNTIME" image.rabbitmq_base string "${export_images[1]}"
kdl_set_file "$TARGET_RUNTIME" image.tdengine_base string "${export_images[2]}"
kdl_set_file "$TARGET_RUNTIME" image.mysql_base string "${export_images[3]}"
kdl_set_file "$TARGET_RUNTIME" operation.startup_mode string replace
kdl_set_file "$TARGET_RUNTIME" operation.shutdown_mode string stop
kdl_set_file "$TARGET_RUNTIME" mysql.root_password string CHANGE_ME
kdl_set_file "$TARGET_RUNTIME" mysql.application_username string mitedtsm
kdl_set_file "$TARGET_RUNTIME" mysql.application_password string CHANGE_ME
kdl_set_file "$TARGET_RUNTIME" redis.password string CHANGE_ME
kdl_set_file "$TARGET_RUNTIME" rabbitmq.username string mitedtsm
kdl_set_file "$TARGET_RUNTIME" rabbitmq.password string CHANGE_ME
kdl_set_file "$TARGET_RUNTIME" tdengine.username string root
kdl_set_file "$TARGET_RUNTIME" tdengine.password string CHANGE_ME
kdl_set_file "$TARGET_RUNTIME" mysql.dataset string none
kdl_set_file "$TARGET_RUNTIME" mysql.dataset_manifest string ../../database/datasets/none.manifest
kdl_set_file "$TARGET_RUNTIME" mysql.dataset_mode string preserve

TARGET_PROFILES="${TEMP_DIR}/podman/config/data-profiles.kdl"
cp "${PODMAN_DIR}/offline/data-profiles.example.kdl" "$TARGET_PROFILES"
kdl_set_file "$TARGET_PROFILES" demo.dataset string "$DEMO_DATASET"
kdl_set_file "$TARGET_PROFILES" demo.dataset_manifest string \
    "../../database/${DEMO_DATASET_MANIFEST#${SQL_ROOT}/}"
kdl_set_file "$TARGET_PROFILES" demo.dataset_mode string "$DEMO_DATASET_MODE"

cp "$BPM_MANIFEST" "${TEMP_DIR}/podman/config/$(basename -- "$BPM_MANIFEST")"
for model_ref in "${MODEL_CONFIG_REFS[@]}"; do
    target_model="${TEMP_DIR}/podman/config/${model_ref}"
    cp "$(dirname -- "$BPM_MANIFEST")/${model_ref}" "$target_model"
    # A new bundled database always starts with the documented bootstrap admin.
    # Never copy a source-host BPM API password into a transferable bundle.
    kdl_set_file "$target_model" account.username string admin
    kdl_set_file "$target_model" account.password string admin123
    kdl_set_file "$target_model" approval.approver_username string admin
done
cp "$NOTIFICATION_CONFIG" "${TEMP_DIR}/podman/config/$(basename -- "$NOTIFICATION_CONFIG")"

for ((index = 0; index < ${#images[@]}; index++)); do
    image="${images[index]}"
    export_image="${export_images[index]}"
    archive="${archives[index]}"
    if [[ "$export_image" != "$image" ]]; then
        podman tag "$image" "$export_image"
    fi
    printf 'Exporting %s as Docker-compatible archive %s\n' "$export_image" "$archive"
    podman save --format docker-archive --output "${TEMP_DIR}/podman/images/${archive}" "$export_image"
done

(
    cd "${TEMP_DIR}/podman/images"
    sha256sum "${archives[@]}" > SHA256SUMS
)

source_commit="$(git -C "$PROJECT_ROOT" rev-parse HEAD 2>/dev/null || printf 'unknown')"
{
    printf 'format=mitedtsm-offline-v2\n'
    printf 'created_at_utc=%s\n' "$(date -u +%Y-%m-%dT%H:%M:%SZ)"
    printf 'architecture=x86_64\n'
    printf 'source_commit=%s\n' "$source_commit"
    printf 'container_engines=podman,docker-compose\n'
    printf 'default_data_profile=production\n'
    printf 'source_host_infrastructure_credentials=false\n'
    printf 'image_count=%s\n' "${#images[@]}"
    for ((index = 0; index < ${#images[@]}; index++)); do
        printf 'image_%s=%s|%s|archive_tag=%s\n' "$((index + 1))" "${images[index]}" \
            "$(podman image inspect --format '{{.Id}}' "${images[index]}")" \
            "${export_images[index]}"
    done
} > "${TEMP_DIR}/BUNDLE_INFO.txt"

chmod 0755 \
    "${TEMP_DIR}/configure-host.sh" \
    "${TEMP_DIR}/configure.sh" \
    "${TEMP_DIR}/container-engine.sh" \
    "${TEMP_DIR}/deploy.sh" \
    "${TEMP_DIR}/start.sh" \
    "${TEMP_DIR}/stop.sh" \
    "${TEMP_DIR}/verify.sh" \
    "${TEMP_DIR}/docker/runtime.sh" \
    "${TEMP_DIR}/podman/deploy.sh" \
    "${TEMP_DIR}/podman/internal/"*.sh \
    "${TEMP_DIR}/podman/operations/bpm/"*.sh \
    "${TEMP_DIR}/podman/tools/bin/dasel"

if [[ -e "$OUTPUT_DIR" ]]; then
    rm -rf -- "$OUTPUT_DIR"
fi
mv -- "$TEMP_DIR" "$OUTPUT_DIR"
TEMP_DIR=''
trap - EXIT

printf 'Offline deployment bundle is ready: %s\n' "$OUTPUT_DIR"
du -sh "$OUTPUT_DIR"
