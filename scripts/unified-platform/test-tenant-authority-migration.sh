#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
repo_root="$(cd "${script_dir}/../.." && pwd)"
tool="${script_dir}/migrate-tenant-authority.sh"

fail() {
  printf 'tenant-authority-migration-contract: %s\n' "$*" >&2
  exit 1
}

[[ -x "${tool}" ]] || fail 'migration tool must be executable'

help_output="$(${tool} --help)"
for command_name in export-rbac3 import-idp verify-idp verify-rbac report; do
  grep -Fq -- "${command_name}" <<<"${help_output}" \
    || fail "help must document ${command_name}"
done

temporary_dir="$(mktemp -d "${TMPDIR:-/tmp}/egon-tenant-authority-contract.XXXXXX")"
trap 'rm -rf "${temporary_dir}"' EXIT

fixture_dir="${temporary_dir}/psql-fixture"
mkdir -p "${fixture_dir}"
printf '%s\n' \
  $'42001\tacme\tAcme Corporation\tACTIVE\t{}' \
  $'42002\tglobex\tGlobex Corporation\tSUSPENDED\t{"region":"cn"}' \
  >"${fixture_dir}/tenants.tsv"
printf '%s\n' \
  $'42001\tuser-alice\tACTIVE' \
  $'42002\tuser-bob\tDISABLED' \
  >"${fixture_dir}/memberships.tsv"

psql_shim="${temporary_dir}/psql"
cat >"${psql_shim}" <<'EOF'
#!/usr/bin/env bash
set -euo pipefail

fixture_dir="${UNIFIED_PLATFORM_MIGRATION_FIXTURE_DIR:?fixture directory is required}"
query="${*: -1}"
case "${query}" in
  *identity_sub*|*membership*) cat "${fixture_dir}/memberships.tsv" ;;
  *) cat "${fixture_dir}/tenants.tsv" ;;
esac
EOF
chmod 700 "${psql_shim}"

freeze_marker="${temporary_dir}/freeze.marker"
printf 'FROZEN\n' >"${freeze_marker}"
artifact="${temporary_dir}/tenant-authority-v1.json"
report="${temporary_dir}/tenant-authority-report.json"
import_log="${temporary_dir}/import.log"

export_output="$(
  UNIFIED_PLATFORM_PSQL_BIN="${psql_shim}" \
  UNIFIED_PLATFORM_MIGRATION_FIXTURE_DIR="${fixture_dir}" \
  "${tool}" export-rbac3 --db-url 'postgresql://fixture/metadata' \
    --freeze-marker "${freeze_marker}" --output "${artifact}"
)"
grep -Fq 'export-rbac3: PASS' <<<"${export_output}" \
  || fail 'export must report PASS'
[[ -f "${artifact}.sha256" ]] || fail 'export must write a checksum sidecar'
[[ "$(stat -f '%Lp' "${artifact}")" == '600' ]] \
  || fail 'artifact must be mode 600'
[[ "$(stat -f '%Lp' "${artifact}.sha256")" == '600' ]] \
  || fail 'checksum sidecar must be mode 600'
grep -Fq 'user-alice' "${artifact}" || fail 'artifact must include memberships'
if grep -Eiq 'secret|password|private[_-]?key|token' "${artifact}"; then
  fail 'artifact must not contain credentials or token material'
fi

verify_idp_output="$(${tool} verify-idp --artifact "${artifact}")"
grep -Fq 'verify-idp: PASS' <<<"${verify_idp_output}" \
  || fail 'IdP verification must report PASS'
verify_rbac_output="$(${tool} verify-rbac --artifact "${artifact}")"
grep -Fq 'verify-rbac: PASS' <<<"${verify_rbac_output}" \
  || fail 'RBAC verification must report PASS'

report_output="$(${tool} report --artifact "${artifact}" --output "${report}")"
grep -Fq 'report: PASS' <<<"${report_output}" || fail 'report must report PASS'
jq -e '
  .schemaVersion == 1
  and .status == "PASS"
  and .counts.tenants == 2
  and .counts.memberships == 2
  and .counts.tenantStatuses.ACTIVE == 1
  and .counts.tenantStatuses.SUSPENDED == 1
  and .counts.membershipStatuses.ACTIVE == 1
  and .counts.membershipStatuses.DISABLED == 1
  and (.checksum | test("^[0-9a-f]{64}$"))
' "${report}" >/dev/null || fail 'report must contain counts and checksum'

import_output="$(
  UNIFIED_PLATFORM_PSQL_BIN="${psql_shim}" \
  UNIFIED_PLATFORM_MIGRATION_FIXTURE_DIR="${fixture_dir}" \
  UNIFIED_PLATFORM_MIGRATION_IMPORT_LOG="${import_log}" \
  "${tool}" import-idp --db-url 'postgresql://fixture/idp' \
    --freeze-marker "${freeze_marker}" --artifact "${artifact}"
)"
grep -Fq 'import-idp: PASS' <<<"${import_output}" \
  || fail 'import must report PASS'
[[ "$(wc -l <"${import_log}" | tr -d ' ')" == '4' ]] \
  || fail 'import must record deterministic tenant/member upserts'

cp "${artifact}.sha256" "${temporary_dir}/original.sha256"
printf '\n' >>"${artifact}"
if ${tool} verify-idp --artifact "${artifact}" >/dev/null 2>&1; then
  fail 'checksum mismatch must fail verification'
fi
mv "${temporary_dir}/original.sha256" "${artifact}.sha256"

if ${tool} export-rbac3 --db-url 'postgresql://fixture/metadata' \
    --freeze-marker "${temporary_dir}/missing.marker" \
    --output "${temporary_dir}/rejected.json" >/dev/null 2>&1; then
  fail 'non-frozen marker must fail export'
fi

printf 'tenant-authority-migration-contract: PASS\n'
