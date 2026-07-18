#!/usr/bin/env bash

set -Eeuo pipefail

PODMAN_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/../.." && pwd)"
PROJECT_ROOT="$(cd -- "${PODMAN_DIR}/.." && pwd)"

[[ $# -eq 1 ]] || {
    printf 'Usage: bash ./tests/acceptance/verify-crm-user-guide.sh <runtime-config.yaml>\n' >&2
    exit 2
}

# shellcheck source=../../lib/yaml-config.sh
source "${PODMAN_DIR}/lib/yaml-config.sh"
yaml_config_init "$1"

MYSQL_CONTAINER="$(yaml_require container.mysql)"
MYSQL_DATABASE="$(yaml_require mysql.database)"
MYSQL_USER="$(yaml_require health.mysql_user)"
MYSQL_PASSWORD="$(yaml_require mysql.root_password)"
MANIFEST="${PROJECT_ROOT}/docs/20-CRM-Delivery/user-guide/routes.manifest"
VIEWS_ROOT="${PROJECT_ROOT}/Web/src/views"

[[ -s "$MANIFEST" ]] || {
    printf 'CRM user-guide route manifest is missing.\n' >&2
    exit 1
}

runtime_routes="$(mktemp)"
trap 'rm -f -- "$runtime_routes"' EXIT

podman exec "$MYSQL_CONTAINER" mysql "-u${MYSQL_USER}" "-p${MYSQL_PASSWORD}" \
    "--database=${MYSQL_DATABASE}" --default-character-set=utf8mb4 -Nse "
WITH RECURSIVE tree AS (
  SELECT id,parent_id,path,component,type,CAST(path AS CHAR(500)) full_path
  FROM system_menu WHERE id=2397 AND deleted=b'0'
  UNION ALL
  SELECT menu.id,menu.parent_id,menu.path,menu.component,menu.type,
         CONCAT(tree.full_path,'/',menu.path)
  FROM system_menu menu JOIN tree ON menu.parent_id=tree.id
  WHERE menu.deleted=b'0'
)
SELECT CONCAT(full_path,'|',component) FROM tree WHERE type=2 ORDER BY full_path;
" > "$runtime_routes"

route_count=0
while IFS='|' read -r route component || [[ -n "$route" ]]; do
    [[ -n "$route" && "$route" != \#* ]] || continue
    route_count=$((route_count + 1))
    rg -Fxq "${route}|${component}" "$runtime_routes" || {
        printf 'Documented CRM route is absent or points to another component: %s\n' "$route" >&2
        exit 1
    }
    if [[ "$component" == *.vue ]]; then
        component_file="${VIEWS_ROOT}/${component}"
    else
        component_file="${VIEWS_ROOT}/${component}.vue"
    fi
    [[ -s "$component_file" ]] || {
        printf 'Documented CRM component is missing: %s\n' "$component_file" >&2
        exit 1
    }
done < "$MANIFEST"

[[ "$route_count" -ge 30 ]] || {
    printf 'CRM guide route coverage unexpectedly low: %s\n' "$route_count" >&2
    exit 1
}

printf 'crm-user-guide-routes=%s/%s\n' "$route_count" "$route_count"
