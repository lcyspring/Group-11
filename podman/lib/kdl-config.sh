#!/usr/bin/env bash

# Strict KDL 2.0 configuration access backed by dasel. The public configuration
# contract intentionally permits only root scalar nodes and one level of
# scalar child nodes. Repeated nodes are rejected because dasel represents
# them as arrays and project configuration keys must be unambiguous.

KDL_CONFIG_LIB_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
KDL_PODMAN_DIR="$(cd -- "${KDL_CONFIG_LIB_DIR}/.." && pwd)"
KDL_DASEL_BIN="${KDL_PODMAN_DIR}/tools/bin/dasel"

_kdl_require_tools() {
    [[ -x "$KDL_DASEL_BIN" ]] || {
        printf 'Project dasel binary is missing: %s\nRun: bash %s/tools/build-dasel.sh\n' \
            "$KDL_DASEL_BIN" "$KDL_PODMAN_DIR" >&2
        return 2
    }
    command -v jq >/dev/null 2>&1 || {
        printf 'jq is required to decode dasel JSON output.\n' >&2
        return 2
    }
}

kdl_config_init() {
    [[ $# -eq 1 ]] || {
        printf 'kdl_config_init requires exactly one configuration path.\n' >&2
        return 2
    }

    KDL_CONFIG_PATH="$1"
    if [[ "$KDL_CONFIG_PATH" != /* ]]; then
        KDL_CONFIG_PATH="$(cd -- "$(dirname -- "$KDL_CONFIG_PATH")" && pwd)/$(basename -- "$KDL_CONFIG_PATH")"
    fi
    [[ "$KDL_CONFIG_PATH" == *.kdl ]] || {
        printf 'Configuration must use the .kdl extension: %s\n' "$KDL_CONFIG_PATH" >&2
        return 2
    }
    [[ -f "$KDL_CONFIG_PATH" ]] || {
        printf 'Configuration file does not exist: %s\n' "$KDL_CONFIG_PATH" >&2
        return 2
    }
    _kdl_require_tools || return

    if ! KDL_CONFIG_JSON="$("$KDL_DASEL_BIN" -i kdl -o json --compact --root < "$KDL_CONFIG_PATH")"; then
        printf 'Invalid KDL configuration: %s\n' "$KDL_CONFIG_PATH" >&2
        return 2
    fi

    jq -e '
        type == "object"
        and ([.. | select(type == "array")] | length == 0)
        and ([.. | select(type == "null")] | length == 0)
        and all(.[];
            type != "object"
            or all(.[]; type != "object" and type != "array" and type != "null")
        )
    ' >/dev/null 2>&1 <<< "$KDL_CONFIG_JSON" || {
        printf 'KDL configuration permits unique scalar keys at the root and one child level only: %s\n' \
            "$KDL_CONFIG_PATH" >&2
        return 2
    }

    KDL_CONFIG_DIR="$(cd -- "$(dirname -- "$KDL_CONFIG_PATH")" && pwd)"
}

kdl_get() {
    local wanted="$1"
    [[ "$wanted" =~ ^[A-Za-z_][A-Za-z0-9_-]*(\.[A-Za-z_][A-Za-z0-9_-]*)?$ ]] || {
        printf 'Invalid KDL configuration path: %s\n' "$wanted" >&2
        return 2
    }
    jq -r --arg wanted "$wanted" '
        getpath($wanted | split("."))
        | if . == null then empty
          elif type == "object" or type == "array" then error("scalar value required")
          elif type == "string" then .
          else tostring
          end
    ' <<< "$KDL_CONFIG_JSON"
}

kdl_require() {
    local key="$1" value
    value="$(kdl_get "$key")" || return
    [[ -n "$value" ]] || {
        printf 'Missing required configuration value: %s\n' "$key" >&2
        return 2
    }
    printf '%s' "$value"
}

kdl_bool() {
    local key="$1" value
    value="$(kdl_require "$key")" || return
    case "$value" in
        true|false) printf '%s' "$value" ;;
        *) printf '%s must be #true or #false; got: %s\n' "$key" "$value" >&2; return 2 ;;
    esac
}

kdl_positive_integer() {
    local key="$1" value
    value="$(kdl_require "$key")" || return
    [[ "$value" =~ ^[1-9][0-9]*$ ]] || {
        printf '%s must be a positive integer; got: %s\n' "$key" "$value" >&2
        return 2
    }
    printf '%s' "$value"
}

kdl_port() {
    local key="$1" value
    value="$(kdl_positive_integer "$key")" || return
    ((value <= 65535)) || {
        printf '%s must be a TCP port between 1 and 65535; got: %s\n' "$key" "$value" >&2
        return 2
    }
    printf '%s' "$value"
}

kdl_path() {
    local key="$1" value
    value="$(kdl_require "$key")" || return
    if [[ "$value" == /* ]]; then
        printf '%s' "$value"
    else
        printf '%s/%s' "$KDL_CONFIG_DIR" "$value"
    fi
}

# Update a temporary/test KDL document through dasel. Tracked user examples are
# kept hand-authored for readability; this helper replaces legacy text-based
# rewriting in automated tests.
kdl_set_file() {
    [[ $# -eq 4 ]] || {
        printf 'kdl_set_file requires: <file.kdl> <path> <string|bool|number> <value>.\n' >&2
        return 2
    }
    local file="$1" path="$2" value_type="$3" value="$4" json_value temp_file
    [[ -f "$file" && "$file" == *.kdl ]] || {
        printf 'KDL file does not exist: %s\n' "$file" >&2
        return 2
    }
    [[ "$path" =~ ^[A-Za-z_][A-Za-z0-9_-]*(\.[A-Za-z_][A-Za-z0-9_-]*)?$ ]] || {
        printf 'Invalid KDL configuration path: %s\n' "$path" >&2
        return 2
    }
    _kdl_require_tools || return
    case "$value_type" in
        string) json_value="$(jq -cn --arg value "$value" '$value')" ;;
        bool)
            [[ "$value" == true || "$value" == false ]] || {
                printf 'Invalid boolean KDL replacement: %s\n' "$value" >&2
                return 2
            }
            json_value="$value"
            ;;
        number)
            [[ "$value" =~ ^-?[0-9]+([.][0-9]+)?$ ]] || {
                printf 'Invalid numeric KDL replacement: %s\n' "$value" >&2
                return 2
            }
            json_value="$value"
            ;;
        *) printf 'Unsupported KDL replacement type: %s\n' "$value_type" >&2; return 2 ;;
    esac
    temp_file="$(mktemp "${file}.dasel.XXXXXX")"
    if ! "$KDL_DASEL_BIN" -i kdl -o kdl --root \
        --var "replacement=json:${json_value}" \
        "${path} = \$replacement" < "$file" > "$temp_file"; then
        rm -f -- "$temp_file"
        return 2
    fi
    mv -- "$temp_file" "$file"
}
