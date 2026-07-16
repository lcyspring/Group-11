#!/usr/bin/env bash

set -Eeuo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"

[[ $# -eq 1 ]] || {
    printf 'Usage: bash ./build-image-archives.sh <config.yaml>\n' >&2
    exit 2
}

# shellcheck source=lib/yaml-config.sh
source "${SCRIPT_DIR}/lib/yaml-config.sh"
yaml_config_init "$1"

MODE="$(yaml_require operation.mode)"
ARCHIVE_DIR="$(yaml_path archive.directory)"
OVERWRITE="$(yaml_bool archive.overwrite)"
REGISTRY="$(yaml_require registry.host)"

sources=(
    "$(yaml_require image.server_builder_source)"
    "$(yaml_require image.mall_builder_source)"
)
destinations=(
    "$(yaml_require image.server_builder_destination)"
    "$(yaml_require image.mall_builder_destination)"
)
archives=(
    "${ARCHIVE_DIR}/$(yaml_require archive.server_builder_filename)"
    "${ARCHIVE_DIR}/$(yaml_require archive.mall_builder_filename)"
)

case "$MODE" in
    check|save|load|push) ;;
    *)
        printf 'operation.mode must be check, save, load, or push; got: %s\n' "$MODE" >&2
        exit 2
        ;;
esac

command -v podman >/dev/null
command -v sha256sum >/dev/null
[[ "$(podman info --format '{{.Host.Security.Rootless}}')" == "true" ]] || {
    printf 'Run this script as the normal rootless Podman user.\n' >&2
    exit 1
}

if [[ "$MODE" == "check" ]]; then
    for image in "${sources[@]}"; do
        podman image exists "$image" || {
            printf 'Build toolchain image is missing: %s\n' "$image" >&2
            exit 1
        }
    done
    printf 'Build image preflight passed. Archives were not written or pushed.\n'
    exit 0
fi

mkdir -p -- "$ARCHIVE_DIR"
for ((index = 0; index < ${#sources[@]}; index++)); do
    source_image="${sources[index]}"
    destination_image="${destinations[index]}"
    archive="${archives[index]}"

    case "$MODE" in
        save)
            podman image exists "$source_image" || {
                printf 'Build toolchain image is missing: %s\n' "$source_image" >&2
                exit 1
            }
            if [[ "$OVERWRITE" != "true" && ( -e "$archive" || -e "${archive}.sha256" ) ]]; then
                printf 'Build image archive exists and overwrite is false: %s\n' "$archive" >&2
                exit 1
            fi
            temporary_archive="${archive}.tmp.$$"
            trap 'rm -f -- "$temporary_archive"' EXIT
            printf 'Saving build image: %s -> %s\n' "$source_image" "$archive"
            podman save --format oci-archive --output "$temporary_archive" "$source_image"
            mv -f -- "$temporary_archive" "$archive"
            trap - EXIT
            (
                cd -- "$ARCHIVE_DIR"
                sha256sum -- "$(basename -- "$archive")" > "$(basename -- "$archive").sha256"
            )
            ;;
        load)
            [[ -s "$archive" && -s "${archive}.sha256" ]] || {
                printf 'Build image archive or checksum is missing: %s\n' "$archive" >&2
                exit 1
            }
            (
                cd -- "$ARCHIVE_DIR"
                sha256sum --check --status "$(basename -- "$archive").sha256"
            )
            podman load --input "$archive"
            ;;
        push)
            podman image exists "$source_image" || {
                printf 'Build toolchain image is missing: %s\n' "$source_image" >&2
                exit 1
            }
            [[ "$destination_image" == "${REGISTRY}/"* ]] || {
                printf 'Destination is outside configured registry: %s\n' "$destination_image" >&2
                exit 1
            }
            podman login --get-login "$REGISTRY" >/dev/null 2>&1 || {
                printf 'Not logged into build image registry: %s\n' "$REGISTRY" >&2
                exit 1
            }
            printf 'Pushing build image: %s -> %s\n' "$source_image" "$destination_image"
            podman tag "$source_image" "$destination_image"
            podman push "$destination_image"
            ;;
    esac
done

printf 'build-image-operation=%s count=%s\n' "$MODE" "${#sources[@]}"
