# Unified Identity, OAuth Client, and Tenant Authority Cutover Runbook

This operator procedure follows the accepted [multitenant OAuth/OIDC Spec](../egon/spec/2026-09-22-17-14-tianquan-multitenant-oauth-oidc-design.md) and its current-code amendment under review, [2026-10-08 alignment](../egon/spec/2026-10-08-10-18-oauth-current-code-alignment.md). It covers the controlled Shoubing identity/OAuth data import, per-realm cutover, and local Edge BFF checks. It does not migrate Jianshen business tables or claim live runtime health.

Run this procedure only in an approved maintenance window. The coding workflow does not run the commands below, start services, connect to a database or Redis, or use a browser. Replace every placeholder through an operator-only environment or secret manager. Never put credentials, access tokens, refresh tokens, cookies, encryption keys, or secret contents in shell history, logs, Git, reports, or tickets.

## 1. Prepare the frozen source and evidence directory

Stop every writer to the legacy identity/OAuth source tables across the full fleet. A component-level migration flag is not a fleet-wide write fence. Take restorable source and target database backups, record their checksums, and verify that the source snapshot will remain immutable until all realm imports finish.

Create a protected evidence directory and exact snapshot marker. The marker is a human-attested maintenance gate; it does not replace stopping all writers or reconciling the report against the backed-up snapshot.

```bash
export CUTOVER_EVIDENCE_DIR="/secure/operator/evidence/<release-id>"
export SOURCE_SNAPSHOT_ID="<immutable-snapshot-id>"
export REALM_KEY="platform" # or tenant:<canonical-positive-decimal-id>
export FREEZE_MARKER="${CUTOVER_EVIDENCE_DIR}/source.freeze"
export OWNERSHIP_MAP="${CUTOVER_EVIDENCE_DIR}/client-ownership.json"
export SECRET_OUTPUT_DIR="${CUTOVER_EVIDENCE_DIR}/client-secrets"
export CHECKPOINT="${CUTOVER_EVIDENCE_DIR}/checkpoint-${REALM_KEY//:/_}.json"
export DRY_RUN_REPORT="${CUTOVER_EVIDENCE_DIR}/dry-run-${REALM_KEY//:/_}.json"
export APPLY_REPORT="${CUTOVER_EVIDENCE_DIR}/apply-${REALM_KEY//:/_}.json"

install -d -m 700 "${CUTOVER_EVIDENCE_DIR}" "${SECRET_OUTPUT_DIR}"
printf 'realm=%s\nsnapshot=%s\n' "${REALM_KEY}" "${SOURCE_SNAPSHOT_ID}" >"${FREEZE_MARKER}"
chmod 600 "${FREEZE_MARKER}"
```

The ownership map must be owner-only JSON and name every legacy client exactly once. It binds each client to the same snapshot, one exact realm, purpose, and explicit effective scopes per resource. Do not derive ownership from `clientId`/`appId`, choose a default tenant, infer empty USER scopes, or copy old confidential-client secret hashes. Example shape only:

```json
{
  "schemaVersion": 1,
  "sourceSnapshotId": "<immutable-snapshot-id>",
  "assignments": [
    {
      "clientId": "<legacy-client-id>",
      "realmKey": "tenant:<canonical-positive-decimal-id>",
      "purpose": "USER",
      "approvedGrantScopesByResource": {
        "<legacy-resource-id>": ["<approved-scope>"]
      },
      "scopeApprovalId": "<approval-reference-for-empty-legacy-user-scopes>"
    }
  ]
}
```

Validate the source tables, realm membership, grant contexts, client/resource ownership, redirect URIs, and scope decisions before proceeding. The CLI reads only its fixed legacy table allowlist. Old signing-key ciphertext and legacy identity-Outbox payloads are not imported; prepare independent signing keys for every new realm before enabling issuance. A report that says signing-key replacement is required is a blocking operator task, not an invitation to copy old private material.

## 2. Run and review the dry-run report

Use the already-built Shoubing admin executable JAR. Production-like execution must use the `oauth-migration` profile; local source imports that require loopback redirect URIs may use exactly `local,oauth-migration`, with migration last. The runner rejects other profile combinations, web mode, enabled RPC/registration, mismatched markers, and unsafe file permissions.

```bash
export SHOUBING_ADMIN_JAR="egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/target/egon-cola-tianquan-shoubing-admin-exec.jar"

java -jar "${SHOUBING_ADMIN_JAR}" \
  --spring.profiles.active=oauth-migration \
  --spring.main.web-application-type=none \
  --egon.tianquan-shoubing.oauth.migration.enabled=true \
  --egon.tianquan-shoubing.oauth.migration.realm-key="${REALM_KEY}" \
  --egon.tianquan-shoubing.oauth.migration.source-snapshot-id="${SOURCE_SNAPSHOT_ID}" \
  --egon.tianquan-shoubing.oauth.migration.dry-run=true \
  --egon.tianquan-shoubing.oauth.migration.freeze-marker-file="${FREEZE_MARKER}" \
  --egon.tianquan-shoubing.oauth.migration.ownership-map-file="${OWNERSHIP_MAP}" \
  --egon.tianquan-shoubing.oauth.migration.checkpoint-file="${CHECKPOINT}" \
  --egon.tianquan-shoubing.oauth.migration.secret-distribution-directory="${SECRET_OUTPUT_DIR}" \
  --egon.tianquan-shoubing.oauth.migration.report-file="${DRY_RUN_REPORT}"
```

The runner writes a new mode-`600` JSON report to the requested path and logs only counts, the report digest, checkpoint ID, and report path. Keep the parent directory mode `700`. Review all conflicts, counts, source and ownership fingerprints, target realm, scope approvals, signing-key replacement count, and secret-file references. Require zero unresolved conflicts. Approve the exact `reportSha256` from the protected JSON report; do not edit or reuse that report file.

## 3. Apply one realm and reconcile before the next

Run the platform realm first so platform clients and Resource records exist before dependent tenant imports. Then run each tenant with its own checkpoint and report paths. Directory rows are global and replayed idempotently into the trusted storage partition; tenant OAuth rows are limited to the named tenant realm. Every source page is bounded to at most 500 rows. The checkpoint advances only after target commits and content verification.

```bash
export APPROVED_REPORT_SHA256="<reviewed-64-hex-reportSha256>"

java -jar "${SHOUBING_ADMIN_JAR}" \
  --spring.profiles.active=oauth-migration \
  --spring.main.web-application-type=none \
  --egon.tianquan-shoubing.oauth.migration.enabled=true \
  --egon.tianquan-shoubing.oauth.migration.realm-key="${REALM_KEY}" \
  --egon.tianquan-shoubing.oauth.migration.source-snapshot-id="${SOURCE_SNAPSHOT_ID}" \
  --egon.tianquan-shoubing.oauth.migration.dry-run=false \
  --egon.tianquan-shoubing.oauth.migration.freeze-marker-file="${FREEZE_MARKER}" \
  --egon.tianquan-shoubing.oauth.migration.ownership-map-file="${OWNERSHIP_MAP}" \
  --egon.tianquan-shoubing.oauth.migration.checkpoint-file="${CHECKPOINT}" \
  --egon.tianquan-shoubing.oauth.migration.secret-distribution-directory="${SECRET_OUTPUT_DIR}" \
  --egon.tianquan-shoubing.oauth.migration.report-file="${APPLY_REPORT}" \
  --egon.tianquan-shoubing.oauth.migration.approved-report-sha256="${APPROVED_REPORT_SHA256}"
```

The apply recomputes the report digest and stops before target writes if it differs. A source conflict, ownership mismatch, transaction failure, or unknown commit stops that realm; never skip a row or advance a checkpoint manually. If the process stops after a target batch commits, rerun only with the same snapshot, ownership map, approval digest, and protected checkpoint; the importer re-reads and verifies target content before replay. If report writing fails after an apply, the checkpoint is still authoritative: use a new report path and rerun the same approved command to retrieve a reconciled report.

Confidential clients receive newly generated BCrypt credentials. The clear value is written once to an owner-only file under `SECRET_OUTPUT_DIR`; distribute it to the intended operator-owned secret manager and record only the file reference, client ID, credential version, and distribution acknowledgement. Do not print the file or copy old hashes. If a failed transaction leaves an orphaned secret file or the target/client state is uncertain, stop and reconcile the protected artifact against the target before another attempt. Never delete or distribute an ambiguous artifact automatically.

For each realm, record `PREPARED` (frozen snapshot, backups, ownership decisions), `VERIFIED` (zero-conflict report and approved digest), `CUTOVER` (apply report and target reconciliation accepted), and `RETIRED` (legacy writers/issuer path disabled and old source retained read-only). Do not advance a realm on partial reconciliation.

## 4. Local Edge BFF verification

Set the public Edge HTTP(S) origin explicitly before running the local stack. `prepare-local-stack.sh` and `start-local-stack.sh` reject a missing origin. Auth and callback proxies point at Edge; `/api` and Portal API proxies remain on Yuheng Engine. Generated `.env.local` files contain only `VITE_YUHENG_ORIGIN=<Edge origin>` and have mode `600`; they do not prefill a browser tenant.

```bash
export YUHENG_EDGE_BASE_URL="http://127.0.0.1:<edge-port>"
scripts/unified-xingyuan/prepare-local-stack.sh
UNIFIED_XINGYUAN_SKIP_BUILD=true scripts/unified-xingyuan/start-local-stack.sh
```

The launcher checks `/bff/csrf`, an anonymous `/bff/session` response, and the configured OIDC authorization route. Its frontend preflight checks CORS for all five origins without posting a password, persisting a browser cookie, reading UserInfo, or refreshing a USER token. `verify-local-stack.sh --static-only`, `test-direct-run-contract.sh`, and `bash -n` are static checks; live Edge, issuer, Redis, browser login, database import, and cutover remain operator-run gates.

### Local retirement prerequisites

The lower `scripts/unified-identity-local.sh` and upper platform tool share one protected `UNIFIED_IDENTITY_OAUTH_REGISTRY_FILE`. Its exact bounded schema is defined in the alignment Spec §16.1. It contains configured origin/partition/management tenant/identity/fence/key references, finite issuer/tenant/client maps and protected formal USER input-file references; raw secrets and tokens do not belong in it. Decimal IDs/versions are strings. Every named local client has an explicit PLATFORM slot and each tenant slot, with its current credentialVersion and one-time secret-file reference. The default Spring registration is PLATFORM; the Jianshen caller selects explicit tenant registrations through `service-client.tenant-registration-ids`.

Before new local startup, separately complete and review MP identity/tenant/membership import or provisioning, realm/key activation, client/Resource/Grant setup and real Jianshen tenant-authority state. The startup tools read these current facts and reject missing/conflicting evidence; they never update old identity clients, guess directory ownership or run a tenant-authority import. Source tables and every old migration remain untouched. The explicitly enabled local AS client-registration factory avoids self-discovery using already trusted endpoints; normal environments retain Boot ownership.

Formal CLI USER input is supplied by the operator, checked for early format/expiry/issuer/tenant/subject rejection, and verified at the configured issuer UserInfo before USER-dependent operations. This is not a BFF token export. The compatibility `issue-user-token` command only copies verified formal input into a protected explicit output. It cannot log in, mint a USER credential, refresh browser tokens or widen an MCP Grant. Provide a separate protected revoked-token artifact for backend revocation evidence; actual Edge session/refresh/logout remains operator browser acceptance.

New-deployment retirement must check the ten exact API-035–044 method/path pairs for safe410 without token/cookie/redirect. Formal `/platform` and `/tenants/<id>` protocol endpoints belong solely to SAS, and management/health/fallback stay in their ordered chains. Capture service/USER/issuer-negative evidence before declaring external cutover. Local MCP B keeps its established PID/env alias for compatibility but must run the dedicated MCP artifact with data18184/management18183 and a distinct MCP node/instance identity.

```bash
export UNIFIED_IDENTITY_OAUTH_REGISTRY_FILE="/secure/operator/local-oauth-registry.json"
export YUHENG_EDGE_BASE_URL="<operator-configured-edge-origin>"
# Only the operator executes startup or live validation after these prerequisites.
```

## 5. Rollback and evidence boundaries

Before any target writes, the operator may abandon the run and keep the legacy source authoritative. Once a new realm, client, secret generation, audit event, or security generation is committed, do not roll back binaries alone or copy legacy secrets/keys over new facts. Freeze all issuers and writers, reconcile source/target counts and security state, then fix forward or use a coordinated database/configuration/secret restore approved for the entire fleet. Never decrease a security generation or restore a revoked credential.

Retain source and target backup references, immutable snapshot identity, freeze marker, owner-map fingerprint, dry-run/apply reports, checkpoints, secret-distribution acknowledgements, and reconciliation evidence in the protected evidence directory. Static/unit tests do not prove production data correctness, TLS, Redis durability, issuer behavior, browser login, or distributed cutover.
