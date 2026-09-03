#!/usr/bin/env bash

set -Eeuo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd -P)"
GENERATOR="${SCRIPT_DIR}/generate_archetypes.sh"

if [[ "$#" -ne 0 ]]; then
  printf 'Usage: scripts/check_archetypes.sh\n' >&2
  exit 2
fi
[[ -x "$GENERATOR" ]] || {
  printf 'check-archetypes: generator is missing or not executable: %s\n' "$GENERATOR" >&2
  exit 1
}

exec "$GENERATOR" check
