#!/usr/bin/env bash

set -Eeuo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
PODMAN_DIR="$(cd -- "${SCRIPT_DIR}/../.." && pwd)"
PROJECT_ROOT="$(cd -- "${PODMAN_DIR}/.." && pwd)"

usage() {
    printf 'Usage: bash ./tests/documentation/run.sh <config.kdl>\n' >&2
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
kdl_config_init "$CONFIG_PATH"

[[ ! -e "${PROJECT_ROOT}/Server/sql/tools/docker-compose.yaml" ]] || {
    printf 'Retired Docker Compose file must not return.\n' >&2
    exit 1
}
[[ ! -e "${PROJECT_ROOT}/Server/mitedtsm-server/src/main/resources/application-docker.yaml" ]] || {
    printf 'Retired Spring Docker profile must not return.\n' >&2
    exit 1
}

failures=0
checked_links=0

while IFS= read -r markdown_file; do
    while IFS= read -r markdown_link; do
        target="${markdown_link#*](}"
        target="${target%)}"
        target="${target#<}"
        target="${target%>}"
        case "$target" in
            ''|'#'*|http://*|https://*|mailto:*|tel:*) continue ;;
        esac
        target="${target%%#*}"
        [[ -n "$target" ]] || continue
        ((checked_links += 1))
        if [[ "$target" == /* ]]; then
            printf 'Absolute local Markdown link is not allowed: %s -> %s\n' \
                "${markdown_file#${PROJECT_ROOT}/}" "$target" >&2
            ((failures += 1))
            continue
        fi
        resolved="$(realpath -m -- "$(dirname -- "$markdown_file")/$target")"
        if [[ ! -e "$resolved" ]]; then
            printf 'Broken Markdown link: %s -> %s\n' \
                "${markdown_file#${PROJECT_ROOT}/}" "$target" >&2
            ((failures += 1))
        fi
    done < <(rg -o --no-filename '\[[^]]+\]\([^)]+\)' "$markdown_file" || true)
done < <(
    find \
        "${PROJECT_ROOT}/README.md" \
        "${PROJECT_ROOT}/docs/20-CRM-Delivery" \
        "${PROJECT_ROOT}/podman" \
        -type f -name '*.md' \
        ! -path '*/docs/20-CRM-Delivery/bugs/logs/*' \
        ! -path '*/docs/20-CRM-Delivery/daily/*' \
        -print
)

stale_pattern='podman/config/[[:alnum:]_.-]+\.yaml|\./config/[[:alnum:]_.-]+\.yaml|yaml-podman-deployment|docs/(10-Testing|18-Review)|docs/20-CRM-Delivery/tests/|startup_mode: (full|rebuild-(server|web|mall))|runtime-local-rebuild-(server|web|mall)'
if rg -n "$stale_pattern" \
    "${PROJECT_ROOT}/README.md" \
    "${PROJECT_ROOT}/docs/20-CRM-Delivery" \
    "${PROJECT_ROOT}/podman" \
    --glob '*.md' \
    --glob '!docs/20-CRM-Delivery/bugs/logs/**' \
    --glob '!docs/20-CRM-Delivery/daily/**'; then
    printf 'Current documentation still references retired paths or Podman YAML.\n' >&2
    ((failures += 1))
fi

current_entry_docs=(
    "${PROJECT_ROOT}/README.md"
    "${PROJECT_ROOT}/podman/README.md"
    "${PROJECT_ROOT}/podman/README_ZH.md"
    "${PROJECT_ROOT}/podman/DEPLOY_GUIDE_ZH.md"
    "${PROJECT_ROOT}/podman/OPERATIONS_ZH.md"
    "${PROJECT_ROOT}/podman/config/KDL_FIELDS_ZH.md"
    "${PROJECT_ROOT}/podman/config/README_ZH.md"
    "${PROJECT_ROOT}/podman/images/README_ZH.md"
    "${PROJECT_ROOT}/docs/20-CRM-Delivery/TECH_STACK_ZH.md"
    "${PROJECT_ROOT}/docs/20-CRM-Delivery/build/build-toolchain-images/README.md"
    "${PROJECT_ROOT}/docs/20-CRM-Delivery/build/ubuntu-26.04-container-build/README.md"
)
if rg --pcre2 -n '^(?!.*(?:稳定别名|stable alias)).*ghcr\.io/elel-code/group-11-build-ubuntu:26\.04(?!-deno-2\.9\.3)' \
    "${current_entry_docs[@]}"; then
    printf 'Current entry documentation still references the retired unversioned build image.\n' >&2
    ((failures += 1))
fi
if rg -n 'pnpm-store|pnpm store' "${current_entry_docs[@]}"; then
    printf 'Current entry documentation still presents the retired pnpm cache as active.\n' >&2
    ((failures += 1))
fi

((failures == 0)) || {
    printf 'Documentation gate failed with %d problem(s).\n' "$failures" >&2
    exit 1
}

printf 'Documentation gate passed: %d relative Markdown links checked; retired references absent.\n' \
    "$checked_links"
