#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
repo_root="$(cd "${script_dir}/../.." && pwd)"
start_script="${repo_root}/scripts/unified-xingyuan/start-local-stack.sh"
identity_script="${repo_root}/scripts/unified-identity-local.sh"

rg -q 'UNIFIED_PLATFORM_ADVERTISED_HOST:-127\.0\.0\.1' "${start_script}"
rg -q 'declared_hosts="127\.0\.0\.1"' "${identity_script}"
rg -q 'declared_hosts\+=",\$\{advertised_host\}"' "${identity_script}"
! rg -q 'a non-loopback provider host is required' "${start_script}"
! rg -q 'route -n get default|ipconfig getifaddr|hostname -I' "${start_script}"

printf '%s\n' 'local address policy: default=127.0.0.1, explicit advertised host is opt-in'
