# Stateless Identity JWT / Session Removal Verification

Date: 2026-08-14  
Repository: `/Users/mario/SelfProject/Egon-COLA`  
Scope: the implementation described by
`docs/superpowers/plans/2026-08-14-unified-identity-stateless-jwt-session-removal.md`.

## Boundary

No IdP, Gateway, RBAC3, DDC, Redis, PostgreSQL, or frontend development server was started. The evidence below is
offline/module evidence only. Runtime verification remains a user-run step against a clean database/Redis namespace.

## Java verification

The following sequential Maven commands exited `0`:

```text
./mvnw -pl egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-starter -am test -q
./mvnw -pl egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-admin -am test -q
./mvnw -pl egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin -am test -q
./mvnw -pl egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin -am test -q
./mvnw -pl egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin -am test -q
```

The RBAC3, IdP, Gateway Admin, and DDC Admin commands were rerun sequentially after an earlier parallel run exposed only
a protobuf temporary-directory race. The sequential runs are the authoritative results. Test logs contain expected
warning/error output from negative-path tests; Maven exited successfully.

After the final configuration and runbook audit, the affected modules were rerun sequentially and
again exited `0`:

```text
./mvnw -B -ntp -f pom.xml -pl egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-engine -am -Dtest=GatewayEngineRbac3ConfigurationTest -Dsurefire.failIfNoSpecifiedTests=false test -q
./mvnw -B -ntp -f pom.xml -pl egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-admin -am test -q
./mvnw -B -ntp -f pom.xml -pl egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin -am test -q
./mvnw -B -ntp -f pom.xml -pl egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin -am test -q
./mvnw -B -ntp -f pom.xml -pl egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin -am test -q
```

The final MCP/IdP boundary changes were then verified with these focused commands, all exiting
`0`:

```text
./mvnw -B -ntp -f pom.xml -pl egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-gateway-adapter -am -Dtest=IdpGatewaySecurityProviderTest,IdpUserCookieCredentialExtractorTest,IdpUserCredentialRecoveryProviderTest,IdpGatewayAdapterAutoConfigurationTest -Dsurefire.failIfNoSpecifiedTests=false test -q
./mvnw -B -ntp -f pom.xml -pl egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-mcp-core -am -Dtest=McpSecurityGateTest,McpFederationTest,McpLocalToolFlowTest -Dsurefire.failIfNoSpecifiedTests=false test -q
./mvnw -B -ntp -f pom.xml -pl egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-engine -am -Dtest=McpRbac3IntegrationTest,McpTransportIntegrationTest,McpGatewayIdentityAuthenticatorTest -Dsurefire.failIfNoSpecifiedTests=false test -q
./mvnw -B -ntp -f pom.xml -pl egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-test/egon-cola-platform-gateway-test-suite -am -DskipTests compile -q
```

These focused tests assert that USER MCP identity can derive its protocol isolation value from the
verified fixed audience without a USER `client_id` claim, while SERVICE client identity remains a
separate machine-token path. The MCP transport session store is intentionally still covered as
protocol state.

The post-audit offline checks also exited `0`: shell syntax, direct-run contract, RBAC3 static
verification, cleanup-script help, the exact executable-script forbidden scan, production-runtime
forbidden scan, required-presence scan, and `git diff --check`.

## Frontend verification

Sequential Vitest suites and TypeScript typechecks passed for all six frontend packages:

- `egon-cola-platform-admin-web-shared`: 1 file, 4 tests; typecheck passed.
- DDC Admin Web: 19 files, 45 tests; typecheck passed.
- Gateway Admin Web: 20 files, 52 tests; typecheck passed.
- IdP Admin Web: 2 files, 5 tests; typecheck passed.
- RBAC3 Admin Web: 13 files, 20 tests; typecheck passed.
- RBAC3 React SDK: 6 files, 26 tests; typecheck passed.

`npm run lint` passed for shared, DDC, Gateway, and the React SDK. IdP Admin Web still reports one pre-existing unused
`deleteMutation` in `src/features/resource-grants/ClientResourceGrantPage.tsx`; RBAC3 Admin Web reports one existing
React Hooks exhaustive-deps warning and no error. Builds were not run because the shared package's `postbuild` script
removes its local `node_modules`, which is outside this change's requested validation boundary.

Playwright configuration was checked without starting servers:

```text
gateway-admin-web: 12 tests listed
rbac3-admin-web: 3 tests listed
```

## Structural and migration checks

The following checks passed:

```text
git diff --check
exactly one IdP V4 migration
exactly one RBAC3 V5 migration
production Java scan has no old identity/session authority classes
frontend source scan has no token store, sessionStorage, OAuth authorize/token, admin session, or session-version symbols
```

The V5 migration fails fast when legacy RBAC3 identity/session tables contain rows; it does not use `CASCADE`,
`TRUNCATE`, or silent shared-database deletion. Existing migrations were not modified. DDC lease/session terminology and
MCP protocol session identifiers remain intentionally because they are infrastructure/protocol state, not personnel
login sessions.

The local harness scripts were also checked after the stateless cutover:

```text
bash -n scripts/unified-identity-local.sh scripts/unified-platform/*.sh scripts/unified-platform/lib/*.sh egon-cola-platforms/egon-cola-platform-rbac3/scripts/verification/*.sh  # exit 0
bash scripts/unified-platform/test-direct-run-contract.sh                                                                                                      # exit 0
bash egon-cola-platforms/egon-cola-platform-rbac3/scripts/verification/verify-static.sh --verify                                                                  # exit 0
bash scripts/unified-platform/cleanup-legacy-identity-keys.sh --help                                                                                               # exit 0
git diff --check                                                                                                                                                   # exit 0
```

The browser harness now logs in, refreshes and logs out through the public Gateway routes with
one Cookie jar; it does not execute Authorization Code/PKCE or extract per-client USER tokens.
The legacy script keeps short-lived USER Access Token files only as explicit CLI verification
artifacts for direct-service and RBAC3 role-activation checks; browser applications never read
those files. Runtime RBAC3 authorization uses IdP Client Assertion `service-token` configuration;
the old static `service-credential-file` properties were removed. Short-lived SERVICE token files
remain only for explicit local control-plane/MCP verification. SERVICE Client Credentials and MCP
protocol `Mcp-Session-Id`/`McpSessionStore` state remain machine or transport concerns and are not
personnel login state. USER MCP protocol isolation uses the verified fixed USER audience as an
internal `clientId` value; it does not add a USER JWT `client_id` claim or restore personnel
Session semantics.

## Known remaining implementation boundary

The offline implementation now covers the real control-plane chain: the local harness creates or
reuses reporting applications and credentials for IdP, RBAC3, Gateway Admin, DDC Admin, and the mock
backend; obtains a dedicated Gateway Admin SERVICE token; waits for each HTTP catalog; compiles one
operation-scoped Gateway route per active reported HTTP operation; and validates/releases the route
set. Gateway Admin maps only `gateway:*` SERVICE scopes to its capability authorities, while USER
requests continue through the IdP USER-token and RBAC3 authorization path.

Runtime receipts are intentionally not claimed here. The user still needs to start the stack and
verify catalog publication, route release, Gateway login/refresh/retry, direct-service rejection of
an expired Access Token, refresh-token deletion and failed refresh after forced logout, and the
active-role snapshot behavior. The cleanup script is dry-run by default and requires an explicit
Redis endpoint plus `--execute`; it was only syntax/help tested in this offline run.

## Remaining user-run checks

Against a clean schema and Redis namespace, verify: Gateway login through IdP, the same USER AT/RT across Admin Web
clients, Gateway refresh only after AT expiry, direct-service rejection of expired AT, RT deletion on forced logout,
refresh failure after RT deletion, IdP Admin USER AT plus RBAC3 authorization, and persistence of only currently active
roles in authorization snapshots.
