#!/usr/bin/env bash

# Minimal, non-evaluating YAML reader for this project's explicit configuration
# files. Only top-level keys and one nested mapping level are supported.

yaml_config_init() {
    [[ $# -eq 1 ]] || {
        printf 'yaml_config_init requires exactly one configuration path.\n' >&2
        return 2
    }

    YAML_CONFIG_PATH="$1"
    if [[ "$YAML_CONFIG_PATH" != /* ]]; then
        YAML_CONFIG_PATH="$(cd -- "$(dirname -- "$YAML_CONFIG_PATH")" && pwd)/$(basename -- "$YAML_CONFIG_PATH")"
    fi
    [[ -f "$YAML_CONFIG_PATH" ]] || {
        printf 'Configuration file does not exist: %s\n' "$YAML_CONFIG_PATH" >&2
        return 2
    }
    YAML_CONFIG_DIR="$(cd -- "$(dirname -- "$YAML_CONFIG_PATH")" && pwd)"
}

yaml_get() {
    local wanted="$1"
    awk -v wanted="$wanted" '
        function trim(value) {
            sub(/^[[:space:]]+/, "", value)
            sub(/[[:space:]]+$/, "", value)
            return value
        }
        function scalar(value, first, last) {
            value = trim(value)
            first = substr(value, 1, 1)
            last = substr(value, length(value), 1)
            if ((first == "\"" && last == "\"") || (first == "\047" && last == "\047")) {
                return substr(value, 2, length(value) - 2)
            }
            sub(/[[:space:]]+#.*$/, "", value)
            return trim(value)
        }
        /^[[:space:]]*($|#)/ { next }
        /\t/ {
            printf "Tabs are not supported in YAML configuration: line %d\n", NR > "/dev/stderr"
            exit 2
        }
        {
            line = $0
            indent = match(line, /[^ ]/) - 1
            content = substr(line, indent + 1)
            separator = index(content, ":")
            if (!separator) {
                printf "Invalid YAML mapping at line %d\n", NR > "/dev/stderr"
                exit 2
            }
            key = trim(substr(content, 1, separator - 1))
            value = scalar(substr(content, separator + 1))
            if (indent == 0) {
                section = key
                path = key
            } else if (indent == 2 && section != "") {
                path = section "." key
            } else {
                printf "Only two-level YAML mappings are supported: line %d\n", NR > "/dev/stderr"
                exit 2
            }
            if (path == wanted && value != "") {
                print value
                found++
            }
        }
        END {
            if (found > 1) {
                printf "Duplicate YAML key: %s\n", wanted > "/dev/stderr"
                exit 2
            }
        }
    ' "$YAML_CONFIG_PATH"
}

yaml_require() {
    local key="$1" value
    value="$(yaml_get "$key")" || return
    [[ -n "$value" ]] || {
        printf 'Missing required configuration value: %s\n' "$key" >&2
        return 2
    }
    printf '%s' "$value"
}

yaml_bool() {
    local key="$1" value
    value="$(yaml_require "$key")" || return
    case "${value,,}" in
        true) printf 'true' ;;
        false) printf 'false' ;;
        *)
            printf '%s must be true or false; got: %s\n' "$key" "$value" >&2
            return 2
            ;;
    esac
}

yaml_positive_integer() {
    local key="$1" value
    value="$(yaml_require "$key")" || return
    [[ "$value" =~ ^[1-9][0-9]*$ ]] || {
        printf '%s must be a positive integer; got: %s\n' "$key" "$value" >&2
        return 2
    }
    printf '%s' "$value"
}

yaml_port() {
    local key="$1" value
    value="$(yaml_positive_integer "$key")" || return
    ((value <= 65535)) || {
        printf '%s must be a TCP port between 1 and 65535; got: %s\n' "$key" "$value" >&2
        return 2
    }
    printf '%s' "$value"
}

yaml_path() {
    local key="$1" value
    value="$(yaml_require "$key")" || return
    if [[ "$value" == /* ]]; then
        printf '%s' "$value"
    else
        printf '%s/%s' "$YAML_CONFIG_DIR" "$value"
    fi
}
