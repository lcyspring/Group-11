#!/usr/bin/env bash

set -Eeuo pipefail
umask 077

ROOT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
CONFIG_DIR="${ROOT_DIR}/podman/config"
DEPLOYMENT_CONFIG="${ROOT_DIR}/deployment-config.kdl"
DEPLOYMENT_EXAMPLE="${ROOT_DIR}/deployment-config.example.kdl"
PROFILE_CONFIG="${CONFIG_DIR}/data-profiles.kdl"
RUNTIME_TEMPLATE="${CONFIG_DIR}/runtime-template.kdl"
PUBLIC_HOST_OVERRIDE=''
DATA_PROFILE_OVERRIDE=''

usage() {
    cat >&2 <<'EOF'
Usage: ./configure.sh [--host <IPv4-or-DNS>] [--profile production|demo]
       ./configure.sh [--no-demo-data|--demo-data]

The first run creates deployment-config.kdl with random infrastructure
credentials. Edit that chmod-600 KDL and run configure.sh again to apply
custom credentials before the first deployment.
EOF
}

while (($#)); do
    case "$1" in
        --host)
            (($# >= 2)) || { usage; exit 2; }
            PUBLIC_HOST_OVERRIDE="$2"
            shift 2
            ;;
        --profile)
            (($# >= 2)) || { usage; exit 2; }
            DATA_PROFILE_OVERRIDE="$2"
            shift 2
            ;;
        --no-demo-data|--production)
            DATA_PROFILE_OVERRIDE=production
            shift
            ;;
        --demo-data|--demo)
            DATA_PROFILE_OVERRIDE=demo
            shift
            ;;
        -h|--help)
            usage
            exit 0
            ;;
        *)
            printf 'Unknown configure option: %s\n' "$1" >&2
            usage
            exit 2
            ;;
    esac
done

for command in jq od tr mktemp; do
    command -v "$command" >/dev/null 2>&1 || {
        printf 'Required configuration command is unavailable: %s\n' "$command" >&2
        exit 1
    }
done
[[ -f "$DEPLOYMENT_EXAMPLE" && -f "$PROFILE_CONFIG" && -f "$RUNTIME_TEMPLATE" ]] || {
    printf 'Offline configuration templates are incomplete. Re-copy the entire bundle.\n' >&2
    exit 1
}

# shellcheck source=../lib/kdl-config.sh
source "${ROOT_DIR}/podman/lib/kdl-config.sh"

generate_secret() {
    od -An -N24 -tx1 /dev/urandom | tr -d ' \n'
}

if [[ ! -e "$DEPLOYMENT_CONFIG" ]]; then
    cp "$DEPLOYMENT_EXAMPLE" "$DEPLOYMENT_CONFIG"
    kdl_set_file "$DEPLOYMENT_CONFIG" credential.mysql_root_password string "$(generate_secret)"
    kdl_set_file "$DEPLOYMENT_CONFIG" credential.mysql_application_password string "$(generate_secret)"
    kdl_set_file "$DEPLOYMENT_CONFIG" credential.redis_password string "$(generate_secret)"
    kdl_set_file "$DEPLOYMENT_CONFIG" credential.rabbitmq_password string "$(generate_secret)"
    kdl_set_file "$DEPLOYMENT_CONFIG" credential.tdengine_password string "$(generate_secret)"
    chmod 0600 "$DEPLOYMENT_CONFIG"
    printf 'Created target-host credential configuration: %s\n' "$DEPLOYMENT_CONFIG"
fi

if [[ -n "$PUBLIC_HOST_OVERRIDE" ]]; then
    [[ "$PUBLIC_HOST_OVERRIDE" =~ ^[A-Za-z0-9.-]+$ ]] || {
        printf 'Public host must be an IPv4 address or DNS name without scheme, port, path, or spaces.\n' >&2
        exit 2
    }
    kdl_set_file "$DEPLOYMENT_CONFIG" deployment.public_host string "$PUBLIC_HOST_OVERRIDE"
fi
if [[ -n "$DATA_PROFILE_OVERRIDE" ]]; then
    case "$DATA_PROFILE_OVERRIDE" in
        production|demo) ;;
        *) printf 'Data profile must be production or demo.\n' >&2; exit 2 ;;
    esac
    kdl_set_file "$DEPLOYMENT_CONFIG" deployment.data_profile string "$DATA_PROFILE_OVERRIDE"
fi

kdl_config_init "$DEPLOYMENT_CONFIG"
[[ "$(kdl_require schema_version)" == 1 ]] || exit 2
PUBLIC_HOST="$(kdl_require deployment.public_host)"
DATA_PROFILE="$(kdl_require deployment.data_profile)"
MYSQL_ROOT_PASSWORD="$(kdl_require credential.mysql_root_password)"
MYSQL_APPLICATION_USERNAME="$(kdl_require credential.mysql_application_username)"
MYSQL_APPLICATION_PASSWORD="$(kdl_require credential.mysql_application_password)"
REDIS_PASSWORD="$(kdl_require credential.redis_password)"
RABBITMQ_USERNAME="$(kdl_require credential.rabbitmq_username)"
RABBITMQ_PASSWORD="$(kdl_require credential.rabbitmq_password)"
TDENGINE_USERNAME="$(kdl_require credential.tdengine_username)"
TDENGINE_PASSWORD="$(kdl_require credential.tdengine_password)"

[[ "$PUBLIC_HOST" =~ ^[A-Za-z0-9.-]+$ ]] || {
    printf 'deployment.public_host is not a plain IPv4 address or DNS name.\n' >&2
    exit 2
}
case "$DATA_PROFILE" in
    production|demo) ;;
    *) printf 'deployment.data_profile must be production or demo.\n' >&2; exit 2 ;;
esac
[[ "$MYSQL_APPLICATION_USERNAME" =~ ^[A-Za-z0-9_]{1,32}$ ]] || {
    printf 'credential.mysql_application_username must contain only letters, digits, and underscore.\n' >&2
    exit 2
}
[[ "$RABBITMQ_USERNAME" =~ ^[A-Za-z0-9._~-]{1,64}$ ]] || {
    printf 'credential.rabbitmq_username contains unsupported characters.\n' >&2
    exit 2
}
[[ "$TDENGINE_USERNAME" == root ]] || {
    printf 'credential.tdengine_username must remain root for first-volume initialization.\n' >&2
    exit 2
}
for password_name in MYSQL_ROOT_PASSWORD MYSQL_APPLICATION_PASSWORD REDIS_PASSWORD RABBITMQ_PASSWORD TDENGINE_PASSWORD; do
    password="${!password_name}"
    [[ "$password" =~ ^[A-Za-z0-9._~-]{8,128}$ ]] || {
        printf '%s must use 8-128 portable characters: A-Z a-z 0-9 . _ ~ -.\n' "$password_name" >&2
        exit 2
    }
done
if [[ "$MYSQL_APPLICATION_USERNAME" == root && "$MYSQL_APPLICATION_PASSWORD" != "$MYSQL_ROOT_PASSWORD" ]]; then
    printf 'MySQL application and root passwords must match when the application username is root.\n' >&2
    exit 2
fi

kdl_config_init "$PROFILE_CONFIG"
[[ "$(kdl_require schema_version)" == 1 ]] || exit 2
DATASET="$(kdl_require "${DATA_PROFILE}.dataset")"
DATASET_MANIFEST="$(kdl_require "${DATA_PROFILE}.dataset_manifest")"
DATASET_MODE="$(kdl_require "${DATA_PROFILE}.dataset_mode")"

RUNTIME_CONFIG="${CONFIG_DIR}/runtime-local.kdl"
cp "$RUNTIME_TEMPLATE" "$RUNTIME_CONFIG"
kdl_set_file "$RUNTIME_CONFIG" network.admin_ui_public_url string "http://${PUBLIC_HOST}:8081"
kdl_set_file "$RUNTIME_CONFIG" integration.pay_order_notify_url string "http://${PUBLIC_HOST}:8080/admin-api/pay/notify/order"
kdl_set_file "$RUNTIME_CONFIG" integration.pay_refund_notify_url string "http://${PUBLIC_HOST}:8080/admin-api/pay/notify/refund"
kdl_set_file "$RUNTIME_CONFIG" integration.pay_transfer_notify_url string "http://${PUBLIC_HOST}:8080/admin-api/pay/notify/transfer"
kdl_set_file "$RUNTIME_CONFIG" file.public_base_url string "http://${PUBLIC_HOST}:8080"
kdl_set_file "$RUNTIME_CONFIG" crm_marketing.public_base_url string "http://${PUBLIC_HOST}:8080"
kdl_set_file "$RUNTIME_CONFIG" crm_marketing.click_allowed_hosts string "${PUBLIC_HOST},localhost,127.0.0.1"
kdl_set_file "$RUNTIME_CONFIG" mysql.root_password string "$MYSQL_ROOT_PASSWORD"
kdl_set_file "$RUNTIME_CONFIG" mysql.application_username string "$MYSQL_APPLICATION_USERNAME"
kdl_set_file "$RUNTIME_CONFIG" mysql.application_password string "$MYSQL_APPLICATION_PASSWORD"
kdl_set_file "$RUNTIME_CONFIG" redis.password string "$REDIS_PASSWORD"
kdl_set_file "$RUNTIME_CONFIG" rabbitmq.username string "$RABBITMQ_USERNAME"
kdl_set_file "$RUNTIME_CONFIG" rabbitmq.password string "$RABBITMQ_PASSWORD"
kdl_set_file "$RUNTIME_CONFIG" tdengine.username string "$TDENGINE_USERNAME"
kdl_set_file "$RUNTIME_CONFIG" tdengine.password string "$TDENGINE_PASSWORD"
kdl_set_file "$RUNTIME_CONFIG" mysql.dataset string "$DATASET"
kdl_set_file "$RUNTIME_CONFIG" mysql.dataset_manifest string "$DATASET_MANIFEST"
kdl_set_file "$RUNTIME_CONFIG" mysql.dataset_mode string "$DATASET_MODE"
kdl_set_file "$RUNTIME_CONFIG" operation.startup_mode string replace
kdl_set_file "$RUNTIME_CONFIG" operation.shutdown_mode string stop

cp "$RUNTIME_CONFIG" "${CONFIG_DIR}/runtime-fast-local.kdl"
kdl_set_file "${CONFIG_DIR}/runtime-fast-local.kdl" operation.startup_mode string fast

cp "$RUNTIME_CONFIG" "${CONFIG_DIR}/runtime-check-local.kdl"
kdl_set_file "${CONFIG_DIR}/runtime-check-local.kdl" operation.startup_mode string check
kdl_set_file "${CONFIG_DIR}/runtime-check-local.kdl" operation.shutdown_mode string check
kdl_set_file "${CONFIG_DIR}/runtime-check-local.kdl" bpm.provision_after_start bool false

chmod 0600 "$DEPLOYMENT_CONFIG" "$RUNTIME_CONFIG" \
    "${CONFIG_DIR}/runtime-fast-local.kdl" "${CONFIG_DIR}/runtime-check-local.kdl"

printf 'Offline deployment configuration applied: host=%s profile=%s.\n' "$PUBLIC_HOST" "$DATA_PROFILE"
printf 'Infrastructure credentials remain only in chmod-600 target-host configuration files.\n'
