# Unified Identity, OAuth Client, and Tenant Authority Cutover Runbook

This runbook is the operator procedure for the breaking migration described by
the [identity and tenant ownership Spec](../egon/spec/2026-08-21-07-51-tianquan-shoubing-oauth-client-tenant-ownership.md).
It moves tenant catalog and membership authority to Tianquan-Shoubing, changes machine
authentication to Spring Security OAuth2 Client `client_credentials`, and
retains only Tianquan-Jianshen authorization state in the RBAC database.

The procedure is deliberately offline and phase-gated. It does not start a
service, connect to a production database, print a credential, or claim live
topology health. Every command below uses placeholders; replace them through a
secret manager or an operator-only environment, never by committing values.

## 0. Scope and non-negotiable boundaries

- Tianquan-Shoubing owns `identity_tenant`, `identity_tenant_membership`, confidential
  `appId`/`client_id`, one-time client Secret issuance, OAuth grants, and token
  claims.
- Tianquan-Jianshen owns policy snapshots, roles, permissions, fences, and the external
  `tenantId` authorization-state row. It does not provide tenant catalog or
  membership CRUD.
- A biz service receives its `appId`/`client_id` and Secret from an Tianquan-Shoubing
  administrator and uses Spring's OAuth2 Client manager to obtain a SERVICE
  token. The Secret is never stored in the repository, artifact, database
  migration output, Tianshu, or log.
- Tianshu registration uses a SERVICE token whose audience is the Tianshu Resource and
  whose grant context is `PLATFORM`. There is no second Admission Ticket or
  Admission RPC. Tianshu still validates token audience, source/application claims,
  replay protection, and lease/instance binding; lease expiry cannot outlive
  the token validity boundary.
- USER Authorization Code + PKCE, refresh-token rotation, and signing-key
  behavior are compatibility gates and must remain green.
- V5 and V8 are breaking migration boundaries. There is no online dual-write
  window and no single-service or single-database rollback after V8.

## 1. Operator variables and evidence directory

Set only non-secret connection identifiers in the operator shell. Passwords are
resolved by the PostgreSQL client or secret manager and are not placed in the
command line.

```bash
export CUTOVER_EVIDENCE_DIR="/secure/operator/evidence/<release-id>"
export FREEZE_MARKER="${CUTOVER_EVIDENCE_DIR}/write-freeze.marker"
export TIANQUAN_JIANSHEN_DSN="postgresql://<tianquan-jianshen-host>:5432/<tianquan-jianshen-database>"
export TIANQUAN_SHOUBING_DSN="postgresql://<tianquan-shoubing-host>:5432/<tianquan-shoubing-database>"
export ARTIFACT="${CUTOVER_EVIDENCE_DIR}/tenant-authority-v1.json"
export REPORT="${CUTOVER_EVIDENCE_DIR}/tenant-authority-report.json"

install -d -m 700 "${CUTOVER_EVIDENCE_DIR}"
printf 'FROZEN\n' >"${FREEZE_MARKER}"
chmod 600 "${FREEZE_MARKER}"
```

Record the release commit, Flyway locations, PostgreSQL major versions, Tianquan-Shoubing
issuer, OAuth Resource URIs, Tianshu/RBAC RPC targets, and the operator identity in
the evidence directory. Do not record DSN passwords, client Secrets, bearer
tokens, cookies, private keys, or full request headers.

## 2. Preflight inventory and static gate

Before changing data, stop if any in-scope consumer still expects the removed
machine authentication or local tenant-owner contract. The inventory must cover
the Tianquan-Shoubing starter/admin client boundary, Tianshu registration caller, Tianquan-Jianshen
authorization/bootstrap boundary, unified-xingyuan scripts, generated release
fixture, and both Tianquan-Shoubing/Tianquan-Jianshen Web bundles. Yuheng or other external consumers
must be added to the same inventory by the release owner; this repository gate
does not claim a live Yuheng deployment.

```bash
if rg -n --glob '!target/**' --glob '!node_modules/**' --glob '!**/*.test.*' \
    'private_key_jwt|client[_-]?jwks?|TIANQUAN_SHOUBING_[A-Z0-9_]*ADMISSION|[A-Z0-9_]*RESOURCE_ADMISSION|/iam/tenants|targetTenantId|rbac3UserId' \
    scripts/unified-xingyuan/prepare-local-stack.sh \
    scripts/unified-xingyuan/fixtures/unified-xingyuan-release.json \
    egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin-web/src \
    egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin-web/src; then
  echo 'legacy in-scope identity/tenant consumer found' >&2
  exit 1
fi

bash scripts/unified-xingyuan/verify-local-stack.sh --static-only
```

Expected result is zero legacy consumer hits in the listed runtime/config/Web
paths. Historical migration SQL, this runbook's explicit removal language,
negative tests, and Tianshu's retained non-secret Resource-version audit mapping
are not runtime client consumers. Resolve every other hit before proceeding.
Run the module and Web gates from the plan and save their exit codes and logs in
`${CUTOVER_EVIDENCE_DIR}`.

## 3. Maintenance freeze and two readable backups

1. Announce the maintenance window and stop tenant, membership, client/grant,
   role, and permission writes in all Admin Web/API entry points.
2. Stop old Tianquan-Shoubing, Tianshu registration callers, and SERVICE-token consumers before
   applying V5. Keep the old binaries available for the pre-V8 restore point;
   never run them against a V5/V8 schema.
3. Take independent, restorable Tianquan-Shoubing and Tianquan-Jianshen PostgreSQL snapshots/dumps. Test
   that each backup can be listed and restored into an isolated database. Store
   backup identifiers and checksums, not credentials, in the evidence record.
4. Confirm Redis/Tianshu/Yuheng snapshots and the release artifact are available
   if the release includes those components.

Abort here if the marker is absent, a backup is unreadable, or the write freeze
cannot be enforced. No schema or data state has changed at this point.

## 4. Export Tianquan-Jianshen authority artifact

Export the ordered catalog and `identitySub` membership facts while the freeze is
held. The tool writes mode-`600` JSON and a SHA-256 sidecar; it does not include
passwords, client Secrets, signing material, or database URLs.

```bash
UNIFIED_XINGYUAN_PSQL_BIN="psql" \
  scripts/unified-xingyuan/migrate-tenant-authority.sh export-tianquan-jianshen \
  --db-url "${TIANQUAN_JIANSHEN_DSN}" --freeze-marker "${FREEZE_MARKER}" \
  --output "${ARTIFACT}"

scripts/unified-xingyuan/migrate-tenant-authority.sh verify-rbac \
  --artifact "${ARTIFACT}"
```

Capture the `tenants`, `memberships`, status counts, and checksum in the phase
report. A duplicate ID/code/member pair, an orphan, a `migrating-*` placeholder,
an invalid status, an unsafe DSN, or a checksum mismatch is a hard failure.

## 5. Apply Tianquan-Shoubing V5 and import real tenant authority

Apply exactly the new Tianquan-Shoubing V5 migration on the isolated/release Tianquan-Shoubing database;
never edit an existing Flyway file or run a down migration. V5 creates the
tenant/membership tables, app ID and Secret history, converts existing grant
context, and removes obsolete client-key/admission storage.

After Flyway succeeds, import the artifact transactionally and idempotently:

```bash
scripts/unified-xingyuan/migrate-tenant-authority.sh import-tianquan-shoubing \
  --db-url "${TIANQUAN_SHOUBING_DSN}" --freeze-marker "${FREEZE_MARKER}" \
  --artifact "${ARTIFACT}"

scripts/unified-xingyuan/migrate-tenant-authority.sh verify-tianquan-shoubing \
  --artifact "${ARTIFACT}" --db-url "${TIANQUAN_SHOUBING_DSN}"

scripts/unified-xingyuan/migrate-tenant-authority.sh report \
  --artifact "${ARTIFACT}" --output "${REPORT}"
```

The import preserves decimal tenant IDs and `identitySub` values; rerunning it
updates the same tenant/member rows instead of creating a second identity. The
Tianquan-Shoubing verification must prove exact counts/checksums, no duplicate tenant code or
tenant/member pair, no orphan membership, no placeholder tenant, and no grant
pointing at a missing tenant. Do not continue on any mismatch.

## 6. Provision clients, Secret, and grants

With old traffic still stopped, start the new Tianquan-Shoubing backend and Tianquan-Shoubing Admin Web in
the maintenance network. For every Confidential Client:

1. Confirm the administrator-owned `appId` and `client_id` mapping.
2. Create or rotate the Secret in the Web page. The clear Secret is returned
   once; copy it directly into the consumer Secret Manager and record only its
   hint, credential ID, version, and distribution timestamp.
3. Configure the consumer's standard Spring registration:

   ```yaml
   spring:
     security:
       oauth2:
         client:
           registration:
             egon-tianquan-shoubing:
               client-id: ${EGON_TIANQUAN_SHOUBING_APP_KEY}
               client-secret: ${EGON_TIANQUAN_SHOUBING_APP_SECRET}
               authorization-grant-type: client_credentials
               client-authentication-method: client_secret_basic
           provider:
             egon-tianquan-shoubing:
               token-uri: ${EGON_TIANQUAN_SHOUBING_TOKEN_URI}
   egon:
     cola:
       xingyuan:
         tianquan-shoubing:
           service-client:
             app-id: ${EGON_TIANQUAN_SHOUBING_APP_ID}
   ```

4. Add only the exact Resource Grant and scopes needed by that consumer. Tianshu
   registration uses a `CLIENT_CREDENTIALS` grant with `grantContext: PLATFORM`;
   a tenant business call uses an explicit `TENANT` context and decimal
   `tenantId`.
5. Verify that no Secret is written to Git, a database row, Tianshu configuration,
   browser storage, request log, or migration artifact. A missing client stays
   disabled; do not invent a fallback credential.

## 7. Token, RPC, Tianshu, and USER smoke gates

Use a disposable client and representative tenant in the maintenance network.
Redact every token before storing evidence; preferably store only decoded claim
names, status codes, latency, and a token hash prefix.

- Obtain a SERVICE token through `OAuth2AuthorizedClientManager` and verify
  `sub`, `client_id`, `principal_type=SERVICE`, exact `aud`, grant context,
  scope, and expiry. A token request with an invalid Secret or wrong Resource
  must fail closed.
- Call the Tianquan-Shoubing→RBAC membership RPC with an `identitySub` and tenant ID. An
  ACTIVE membership succeeds; a DISABLED/unknown subject fails before any RBAC
  write. Confirm timeout and error metrics are present without logging token
  material.
- Register and heartbeat a Tianshu instance using the Tianshu-audience PLATFORM SERVICE
  token. Confirm lease identity, instance binding, expiry cap, replay rejection,
  and recovery after token renewal. Confirm there is no Admission RPC or
  second-ticket traffic.
- Execute the selected USER Authorization Code + PKCE, refresh, signing-key,
  cookie/session, and revoked-user regression tests. USER behavior is a release
  gate, not an optional smoke test.

Abort before V8 if any token audience/scope, membership status, lease binding,
or USER/Signing regression is wrong.

## 8. Apply Tianquan-Jianshen V8 and bootstrap by external ID

Run the Tianquan-Jianshen V8 migration only after the Tianquan-Shoubing report, Secret distribution, and
all smoke gates are PASS. V8 copies policy version/audit state, retargets every
inbound tenant foreign key without changing child values, verifies the gate
facts, and drops the local tenant catalog table in one transaction.

```bash
./mvnw -B -ntp -pl egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin \
  -am -Dflyway.url="${TIANQUAN_JIANSHEN_DSN}" migrate

scripts/unified-xingyuan/migrate-tenant-authority.sh verify-rbac \
  --artifact "${ARTIFACT}" --db-url "${TIANQUAN_JIANSHEN_DSN}"
```

Deploy the Tianquan-Jianshen backend and Web as the same release unit. Development/bootstrap
must use `--tenant-id` plus `--identity-sub` (or the decimal `tenant-ids` list)
and must pass the Tianquan-Shoubing ACTIVE membership gate; it must not create a tenant or
fall back to a tenant code. Confirm the RBAC Web has no tenant catalog route or
resource definition and that Tianquan-Shoubing Web owns tenant/member administration.

Inside V8, a failed FK inventory, row-count/checksum comparison, or authorization
state copy rolls back the database transaction. After V8, restore is coordinated
across both database snapshots, all binaries, and all Secret distributions; a
single service or single database rollback is prohibited.

## 9. Restore traffic and observe

1. Enable Tianquan-Shoubing grants and release the new Tianquan-Shoubing, biz services, Tianshu, Tianquan-Jianshen, Yuheng,
   and Admin Web in the tested order. Keep the write freeze until all health and
   smoke probes pass.
2. Re-enable client/tenant/member/role/permission writes and then business
   traffic. Record the exact time and release IDs.
3. Observe for the agreed stability window:
   - OAuth token failures by client/resource/error, token latency, cache hit rate;
   - membership RPC latency, timeout, ACTIVE/DISABLED decisions, and zero-write
     failures;
   - Tianshu register/heartbeat/replay/lease-expiry counters and instance identity;
   - RBAC policyVersion monotonicity, fence/reprojection errors, and Yuheng
     route consistency;
   - Tianquan-Shoubing Admin Web Secret rotation and tenant/member audit events.
4. Retain backups, artifact, report, static/module/Web logs, and redacted smoke
   evidence until the stability window and restore drill are accepted.

## 10. Abort and rollback matrix

| Boundary | Safe action |
| --- | --- |
| Before V5 | Cancel the window; remove the freeze marker after operators agree. |
| After V5, before import/cutover | Stop new binaries and restore the Tianquan-Shoubing snapshot with the old app set. |
| After import, before V8 | Fix forward or restore the Tianquan-Shoubing snapshot; do not run old binaries against the new schema. |
| Inside V8 | Let the transaction roll back; retain evidence and repair the gate. |
| After V8 | Restore both database snapshots and the complete compatible binary/config/Secret release, or fix forward. Never roll back one service/database alone. |

If a newly issued Secret was exposed during an aborted release, revoke it and
rotate it before the next attempt, even if the old snapshot is restored. Never
put a Secret or bearer token in this runbook, a ticket, a shell history, or an
artifact report.

## 11. Completion checklist

- [ ] Freeze marker and readable Tianquan-Shoubing/RBAC backups recorded.
- [ ] Export artifact, sidecar checksum, import log, and PASS report stored mode 600.
- [ ] V5 and V8 Flyway migrations applied without modifying history.
- [ ] Tianquan-Shoubing tenant/member/grant invariants and RBAC authorization-state/FK gates PASS.
- [ ] Every biz service has administrator-provisioned app ID, one-time Secret,
      Spring OAuth2 Client configuration, and least-privilege grants.
- [ ] Tianshu PLATFORM SERVICE token registration/heartbeat smoke PASS with no
      Admission RPC or second ticket.
- [ ] USER Authorization Code + PKCE, refresh, signing, cookie, and revoked-user
      regressions PASS.
- [ ] RBAC bootstrap and Web use external tenant IDs; Tianquan-Shoubing Web owns tenant/member
      administration; legacy static gate is zero for runtime consumers.
- [ ] Observability and coordinated restore evidence accepted by the release owner.
