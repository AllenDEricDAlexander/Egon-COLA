#!/usr/bin/env bash
set -euo pipefail

if [[ -n "${JAVA:-}" && -x "${JAVA}" ]]; then
  JAVA_BIN="${JAVA}"
elif [[ -n "${JAVA_HOME:-}" && -x "${JAVA_HOME}/bin/java" ]]; then
  JAVA_BIN="${JAVA_HOME}/bin/java"
elif command -v java >/dev/null 2>&1; then
  JAVA_BIN="$(command -v java)"
else
  echo '{"code":"BLOCKED_TOOLING","message":"JAVA executable is missing"}' >&2
  exit 7
fi

if [[ -z "${EGON_CODEGEN_CLASSPATH:-}" ]]; then
  echo '{"code":"BLOCKED_TOOLING","message":"EGON_CODEGEN_CLASSPATH is required"}' >&2
  exit 7
fi

IFS=':' read -r -a CLASSPATH_ENTRIES <<< "${EGON_CODEGEN_CLASSPATH}"
for entry in "${CLASSPATH_ENTRIES[@]}"; do
  if [[ ! -e "${entry}" ]]; then
    echo '{"code":"BLOCKED_TOOLING","message":"missing classpath entry"}' >&2
    exit 7
  fi
done

exec "${JAVA_BIN}" -cp "${EGON_CODEGEN_CLASSPATH}" top.egon.cola.component.codegen.cli.CodegenCommand "$@"
