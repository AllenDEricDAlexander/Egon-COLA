# Stateless Identity JWT / Session Removal Verification

Date: 2026-08-14  
Repository: `/Users/mario/SelfProject/Egon-COLA`  
Scope: the implementation described by
`docs/superpowers/plans/2026-08-14-unified-identity-stateless-jwt-session-removal.md`.

## Boundary

No Tianquan-Shoubing, Yuheng, Tianquan-Jianshen, Tianshu, Redis, PostgreSQL, or frontend development server was started. The evidence below is
offline/module evidence only. Runtime verification remains a user-run step against a clean database/Redis namespace.

## Java verification

The following sequential Maven commands exited `0`:

```text
./mvnw -pl egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-starter -am test -q
./mvnw -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am test -q
./mvnw -pl egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin -am test -q
./mvnw -pl egon-cola-xingyuan/egon-cola-yuheng/yuheng-admin -am test -q
./mvnw -pl egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin -am test -q
```

The Tianquan-Jianshen, Tianquan-Shoubing, Yuheng Admin, and Tianshu Admin commands were rerun sequentially after an earlier parallel run exposed only
a protobuf temporary-directory race. The sequential runs are the authoritative results. Test logs contain expected
warning/error output from negative-path tests; Maven exited successfully.

After the final configuration and runbook audit, the affected modules were rerun sequentially and
again exited `0`:

```text
./mvnw -B -ntp -f pom.xml -pl egon-cola-xingyuan/egon-cola-yuheng/yuheng-biz-gateway -am -Dtest=GatewayEngineRbac3ConfigurationTest -Dsurefire.failIfNoSpecifiedTests=false test -q
./mvnw -B -ntp -f pom.xml -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am test -q
./mvnw -B -ntp -f pom.xml -pl egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin -am test -q
./mvnw -B -ntp -f pom.xml -pl egon-cola-xingyuan/egon-cola-yuheng/yuheng-admin -am test -q
./mvnw -B -ntp -f pom.xml -pl egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin -am test -q
```

The final MCP/Tianquan-Shoubing boundary changes were then verified with these focused commands, all exiting
`0`:

```text
./mvnw -B -ntp -f pom.xml -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-gateway-adapter -am -Dtest=IdpGatewaySecurityProviderTest,IdpUserCookieCredentialExtractorTest,IdpUserCredentialRecoveryProviderTest,IdpGatewayAdapterAutoConfigurationTest -Dsurefire.failIfNoSpecifiedTests=false test -q
./mvnw -B -ntp -f pom.xml -pl egon-cola-xingyuan/egon-cola-yuheng/yuheng-mcp-core -am -Dtest=McpSecurityGateTest,McpFederationTest,McpLocalToolFlowTest -Dsurefire.failIfNoSpecifiedTests=false test -q
./mvnw -B -ntp -f pom.xml -pl egon-cola-xingyuan/egon-cola-yuheng/yuheng-biz-gateway -am -Dtest=McpRbac3IntegrationTest,McpTransportIntegrationTest,McpGatewayIdentityAuthenticatorTest -Dsurefire.failIfNoSpecifiedTests=false test -q
./mvnw -B -ntp -f pom.xml -pl egon-cola-xingyuan/egon-cola-yuheng/yuheng-test/yuheng-test-suite -am -DskipTests compile -q
```

These focused tests assert that USER MCP identity can derive its protocol isolation value from the
verified fixed audience without a USER `client_id` claim, while SERVICE client identity remains a
separate machine-token path. The MCP transport session store is intentionally still covered as
protocol state.

The post-audit offline checks also exited `0`: shell syntax, direct-run contract, Tianquan-Jianshen static
verification, cleanup-script help, the exact executable-script forbidden scan, production-runtime
forbidden scan, required-presence scan, and `git diff --check`.

## Frontend verification

Sequential Vitest suites and TypeScript typechecks passed for all six frontend packages:

- `egon-cola-xingyuan-admin-web-shared`: 1 file, 4 tests; typecheck passed.
- Tianshu Admin Web: 19 files, 45 tests; typecheck passed.
- Yuheng Admin Web: 20 files, 52 tests; typecheck passed.
- Tianquan-Shoubing Admin Web: 2 files, 5 tests; typecheck passed.
- Tianquan-Jianshen Admin Web: 13 files, 20 tests; typecheck passed.
- Tianquan-Jianshen React SDK: 6 files, 26 tests; typecheck passed.

`npm run lint` passed for shared, Tianshu, Yuheng, and the React SDK. Tianquan-Shoubing Admin Web still reports one pre-existing unused
`deleteMutation` in `src/features/resource-grants/ClientResourceGrantPage.tsx`; Tianquan-Jianshen Admin Web reports one existing
React Hooks exhaustive-deps warning and no error. Builds were not run because the shared package's `postbuild` script
removes its local `node_modules`, which is outside this change's requested validation boundary.

Playwright configuration was checked without starting servers:

```text
yuheng-admin-web: 12 tests listed
tianquan-jianshen-admin-web: 3 tests listed
```

## Structural and migration checks

The following checks passed:

```text
git diff --check
exactly one Tianquan-Shoubing V4 migration
exactly one Tianquan-Jianshen V5 migration
production Java scan has no old identity/session authority classes
frontend source scan has no token store, sessionStorage, OAuth authorize/token, admin session, or session-version symbols
```

The V5 migration fails fast when legacy Tianquan-Jianshen identity/session tables contain rows; it does not use `CASCADE`,
`TRUNCATE`, or silent shared-database deletion. Existing migrations were not modified. Tianshu lease/session terminology and
MCP protocol session identifiers remain intentionally because they are infrastructure/protocol state, not personnel
login sessions.

The local harness scripts were also checked after the stateless cutover:

```text
bash -n scripts/unified-identity-local.sh scripts/unified-xingyuan/*.sh scripts/unified-xingyuan/lib/*.sh egon-cola-xingyuan/egon-cola-tianquan-jianshen/scripts/verification/*.sh  # exit 0
bash scripts/unified-xingyuan/test-direct-run-contract.sh                                                                                                      # exit 0
bash egon-cola-xingyuan/egon-cola-tianquan-jianshen/scripts/verification/verify-static.sh --verify                                                                  # exit 0
bash scripts/unified-xingyuan/cleanup-legacy-identity-keys.sh --help                                                                                               # exit 0
git diff --check                                                                                                                                                   # exit 0
```

The browser harness now logs in, refreshes and logs out through the public Yuheng routes with
one Cookie jar; it does not execute Authorization Code/PKCE or extract per-client USER tokens.
The legacy script keeps short-lived USER Access Token files only as explicit CLI verification
artifacts for direct-service and Tianquan-Jianshen role-activation checks; browser applications never read
those files. Runtime Tianquan-Jianshen authorization uses Tianquan-Shoubing Client Assertion `service-token` configuration;
the old static `service-credential-file` properties were removed. Short-lived SERVICE token files
remain only for explicit local control-plane/MCP verification. SERVICE Client Credentials and MCP
protocol `Mcp-Session-Id`/`McpSessionStore` state remain machine or transport concerns and are not
personnel login state. USER MCP protocol isolation uses the verified fixed USER audience as an
internal `clientId` value; it does not add a USER JWT `client_id` claim or restore personnel
Session semantics.

## Known remaining implementation boundary

The offline implementation now covers the real control-plane chain: the local harness creates or
reuses reporting applications and credentials for Tianquan-Shoubing, Tianquan-Jianshen, Yuheng Admin, Tianshu Admin, and the mock
backend; obtains a dedicated Yuheng Admin SERVICE token; waits for each HTTP catalog; compiles one
operation-scoped Yuheng route per active reported HTTP operation; and validates/releases the route
set. Yuheng Admin maps only `yuheng:*` SERVICE scopes to its capability authorities, while USER
requests continue through the Tianquan-Shoubing USER-token and Tianquan-Jianshen authorization path.

Runtime receipts are intentionally not claimed here. The user still needs to start the stack and
verify catalog publication, route release, Yuheng login/refresh/retry, direct-service rejection of
an expired Access Token, refresh-token deletion and failed refresh after forced logout, and the
active-role snapshot behavior. The cleanup script is dry-run by default and requires an explicit
Redis endpoint plus `--execute`; it was only syntax/help tested in this offline run.

## Remaining user-run checks

Against a clean schema and Redis namespace, verify: Yuheng login through Tianquan-Shoubing, the same USER AT/RT across Admin Web
clients, Yuheng refresh only after AT expiry, direct-service rejection of expired AT, RT deletion on forced logout,
refresh failure after RT deletion, Tianquan-Shoubing Admin USER AT plus Tianquan-Jianshen authorization, and persistence of only currently active
roles in authorization snapshots.
