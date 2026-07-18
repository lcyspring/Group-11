#!/usr/bin/env bash
# Restore all governed BPM models after a fresh database volume.
# The only command-line argument is an explicit YAML configuration path.
set -Eeuo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
PODMAN_DIR="$(cd -- "${SCRIPT_DIR}/../.." && pwd)"
[[ $# -eq 1 ]] || { printf 'Usage: bash ./operations/bpm/provision-bpm-models.sh <config.yaml>\n' >&2; exit 2; }
source "${PODMAN_DIR}/lib/yaml-config.sh"
yaml_config_init "$1"
[[ "$(yaml_require schema_version)" == "1" ]] || { printf 'Unsupported schema_version.\n' >&2; exit 2; }

CONFIG_DIR="$(cd -- "$(dirname -- "$1")" && pwd)"
resolve_config() {
    local configured="$1"
    if [[ "$configured" == /* ]]; then printf '%s' "$configured"; else printf '%s/%s' "$CONFIG_DIR" "$configured"; fi
}

for key in leave receivable reimbursement contract refund trip loan customer_visit work_request; do
    configured="$(yaml_require "models.${key}")"
    model_config="$(resolve_config "$configured")"
    [[ -f "$model_config" ]] || { printf 'BPM model config is missing for %s: %s\n' "$key" "$model_config" >&2; exit 1; }
    printf 'Provisioning BPM model group: %s\n' "$key"
    bash "${SCRIPT_DIR}/provision-bpm-model.sh" "$model_config"
done

printf 'All configured BPM models are deployed.\n'
