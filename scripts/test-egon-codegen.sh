#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
PROFILE="${1:-all}"
WORK="$(mktemp -d "${TMPDIR:-/tmp}/egon-codegen-acceptance.XXXXXX")"
cleanup() {
  rm -rf "${WORK}"
}
trap cleanup EXIT

if [[ -z "${EGON_CODEGEN_CLASSPATH:-}" ]]; then
  echo '{"code":"BLOCKED_TOOLING","message":"EGON_CODEGEN_CLASSPATH is required"}' >&2
  exit 7
fi

copy_project() {
  local name="$1"
  mkdir -p "${WORK}/${name}"
  rsync -a --exclude target --exclude .git \
    "${ROOT}/egon-cola-archetypes/source-projects/${name}/" "${WORK}/${name}/"
}

write_config() {
  local name="$1"
  local project_type="$2"
  local output="${WORK}/${name}"
  local schema="${ROOT}/egon-cola-components/egon-cola-component-code-generator/src/test/resources/ddl/schema.sql"
  local config="${output}/codegen-fixture.json"
  local modules=""
  if [[ "${project_type}" != "light" ]]; then
    modules=$(cat <<EOF
  "modulePaths": {
    "domain": "${name}-domain",
    "application": "${name}-application",
    "infrastructure": "${name}-infrastructure",
    "adapter": "${name}-adapter"
  },
EOF
)
  fi
  cat > "${config}" <<EOF
{
  "configVersion": 1,
  "projectType": "${project_type}",
  "basePackage": "com.example.codegenfixture",
  "domain": "codegenfixture",
  "outputRoot": "${output}",
${modules}
  "input": {"mode": "schema", "schemaFiles": ["${schema}"]},
  "logicalTables": ["orders"],
  "artifacts": ["backend-crud"],
  "existingTypeMappings": {},
  "fieldPolicies": {"create": ["code"], "update": ["code"], "result": ["code"], "filter": ["code"], "sort": ["id"]},
  "apiContract": {
    "existingErrorMapper": "com.example.codegenfixture.support.FixtureErrorMapper",
    "contextSymbol": "com.example.codegenfixture.support.FixtureContext",
    "basePath": "/api/codegenfixture"
  },
  "events": {"enabled": false}
}
EOF
  local support_root="${output}/src/main/java/com/example/codegenfixture/support"
  if [[ "${project_type}" != "light" ]]; then
    support_root="${output}/${name}-infrastructure/src/main/java/com/example/codegenfixture/support"
  fi
  mkdir -p "${support_root}"
  cat > "${support_root}/FixtureErrorMapper.java" <<'JAVA'
package com.example.codegenfixture.support;

public final class FixtureErrorMapper {
    private FixtureErrorMapper() {}

    public static RuntimeException missing(Long id) {
        return new IllegalStateException("missing:" + id);
    }

    public static RuntimeException zeroRows(String operation, Long id, Long version) {
        return new IllegalStateException(operation + ":" + id + ":" + version);
    }
}
JAVA
  echo "${config}"
}

generate_into() {
  local config="$1"
  local java_bin="${JAVA:-java}"
  local plan_output
  plan_output="$("${java_bin}" -cp "${EGON_CODEGEN_CLASSPATH}" top.egon.cola.component.codegen.cli.CodegenCommand plan --config "${config}")"
  local plan_id
  plan_id="$(printf '%s' "${plan_output}" | python3 -c 'import json,sys; lines=[line for line in sys.stdin.read().splitlines() if line.strip()]; print(json.loads(lines[-1])["planId"])')"
  "${java_bin}" -cp "${EGON_CODEGEN_CLASSPATH}" top.egon.cola.component.codegen.cli.CodegenCommand apply --config "${config}" --plan "${plan_id}"
}

compile_copy() {
  local name="$1"
  (cd "${WORK}/${name}" && ./mvnw -B -ntp compile -DskipTests)
}

run_profile() {
  local name="$1"
  local project_type="$2"
  copy_project "${name}"
  local config
  config="$(write_config "${name}" "${project_type}")"
  generate_into "${config}"
  if grep -R "freemarker" -n "${WORK}/${name}/src" "${WORK}/${name}"/*infrastructure/src 2>/dev/null; then
    echo "generated sources must not reference freemarker" >&2
    exit 3
  fi
  compile_copy "${name}"
}

case "${PROFILE}" in
  light) run_profile egon-cola-source-light light ;;
  web) run_profile egon-cola-source-web web ;;
  service) run_profile egon-cola-source-service service ;;
  all)
    run_profile egon-cola-source-light light
    run_profile egon-cola-source-web web
    run_profile egon-cola-source-service service
    ;;
  *)
    echo '{"code":"CONFIG_REQUIRED","message":"profile must be light, web, service or all"}' >&2
    exit 2
    ;;
esac
