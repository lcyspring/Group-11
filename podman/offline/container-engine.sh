#!/usr/bin/env bash

# Shared engine selection for the generated offline bundle. The first
# successful deployment records its engine so later start/stop/verify calls
# cannot accidentally operate on a different set of named volumes.

podman_is_usable() {
    local rootless
    command -v podman >/dev/null 2>&1 || return 1
    rootless="$(podman info --format '{{.Host.Security.Rootless}}' 2>/dev/null)" || return 1
    [[ "$rootless" == true ]]
}

docker_is_usable() {
    command -v docker >/dev/null 2>&1 || return 1
    docker info >/dev/null 2>&1 || return 1
    docker compose version >/dev/null 2>&1
}

engine_is_usable() {
    case "$1" in
        podman) podman_is_usable ;;
        docker) docker_is_usable ;;
        *) return 1 ;;
    esac
}

resolve_container_engine() {
    local requested="${MITEDTSM_CONTAINER_ENGINE:-}" recorded=''
    CONTAINER_ENGINE_STATE_FILE="${BUNDLE_ROOT}/.container-engine"

    if [[ -n "$requested" ]]; then
        case "$requested" in
            podman|docker) ;;
            *)
                printf 'MITEDTSM_CONTAINER_ENGINE must be podman or docker; got: %s\n' "$requested" >&2
                return 2
                ;;
        esac
        if [[ -s "$CONTAINER_ENGINE_STATE_FILE" ]]; then
            IFS= read -r recorded < "$CONTAINER_ENGINE_STATE_FILE"
            if [[ "$recorded" == podman || "$recorded" == docker ]]; then
                [[ "$requested" == "$recorded" ]] || {
                    printf 'This deployment is already bound to %s; refusing engine switch to %s.\n' \
                        "$recorded" "$requested" >&2
                    return 1
                }
            fi
        fi
        engine_is_usable "$requested" || {
            printf 'Requested container engine is not usable by the current user: %s\n' "$requested" >&2
            return 1
        }
        printf '%s' "$requested"
        return
    fi

    if [[ -s "$CONTAINER_ENGINE_STATE_FILE" ]]; then
        IFS= read -r recorded < "$CONTAINER_ENGINE_STATE_FILE"
        case "$recorded" in
            podman|docker) ;;
            *)
                printf 'Invalid recorded container engine in %s.\n' "$CONTAINER_ENGINE_STATE_FILE" >&2
                return 1
                ;;
        esac
        engine_is_usable "$recorded" || {
            printf 'This deployment uses %s, but that engine is not currently usable.\n' "$recorded" >&2
            return 1
        }
        printf '%s' "$recorded"
        return
    fi

    if podman_is_usable; then
        printf 'podman'
    elif docker_is_usable; then
        printf 'docker'
    else
        printf 'Neither rootless Podman nor Docker with Compose is usable by the current user.\n' >&2
        return 1
    fi
}

record_container_engine() {
    local engine="$1" temp_file
    CONTAINER_ENGINE_STATE_FILE="${BUNDLE_ROOT}/.container-engine"
    temp_file="$(mktemp "${CONTAINER_ENGINE_STATE_FILE}.XXXXXX")"
    printf '%s\n' "$engine" > "$temp_file"
    chmod 0600 "$temp_file"
    mv -- "$temp_file" "$CONTAINER_ENGINE_STATE_FILE"
}
