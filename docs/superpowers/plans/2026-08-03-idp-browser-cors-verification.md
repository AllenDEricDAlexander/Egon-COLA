# IdP Browser CORS and Admin Web Verification Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make the four local Admin Web applications complete their browser OAuth flow without CORS failures and prevent future handoff before browser-equivalent verification passes.

**Architecture:** Keep the existing cross-origin OAuth design: all Admin Web applications call IdP on `127.0.0.1:18120`, while their `/api` calls stay same-origin through Vite proxies. Register the existing restrictive IdP CORS policy under the bean name Spring Security actually resolves, then extend automated verification across every browser boundary.

**Tech Stack:** Java 21, Spring Boot 3.5, Spring Security, JUnit 5, MockMvc, Bash, curl, jq, Vite.

## Global Constraints

- Keep exact-origin allowlisting; do not use wildcard CORS origins.
- Keep credentialed OAuth cookies and the existing PKCE flow.
- Keep DDC Admin Web on `http://127.0.0.1:18152` and Remote MCP on `http://127.0.0.1:18151`.
- Do not change frontend authentication behavior or introduce runtime dependencies.
- Leave the complete local stack running after verification.

---

### Task 1: Activate the IdP CORS configuration in Spring Security

**Files:**
- Modify: `egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-admin/src/main/java/top/egon/cola/platform/idp/admin/security/IdpAdminSecurityConfiguration.java`
- Test: `egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-admin/src/test/java/top/egon/cola/platform/idp/admin/interfaces/http/IdpSsoLoginControllerIT.java`

**Interfaces:**
- Consumes: `egon.idp.oauth.allowed-origins` and Spring Security's conventional `corsConfigurationSource` bean lookup.
- Produces: exact `Access-Control-Allow-Origin` and `Access-Control-Allow-Credentials: true` headers for configured origins under `/oauth2/**`.

- [x] **Step 1: Write the failing CORS integration tests**

Add test property `egon.idp.oauth.allowed-origins=http://127.0.0.1:18121,http://127.0.0.1:18152`. Assert that `GET /oauth2/login/csrf` with origin `http://127.0.0.1:18152` returns that exact allow-origin header and credentials header. Assert that `OPTIONS /oauth2/login` permits `POST`, `Content-Type`, and `X-IDP-CSRF`. Assert that `http://localhost:18152` receives no allow-origin header.

- [x] **Step 2: Run the focused test and verify RED**

Run:

```bash
./mvnw -B -ntp -f egon-cola-platforms/pom.xml \
  -pl egon-cola-platform-idp/egon-cola-platform-idp-admin -am \
  -DskipITs=true -Dtest=IdpSsoLoginControllerIT \
  -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: configured-origin assertions fail because the response has no `Access-Control-Allow-Origin` header.

- [x] **Step 3: Register the existing restrictive CORS source under the standard name**

Change the existing bean declaration to:

```java
@Bean(name = "corsConfigurationSource")
CorsConfigurationSource idpCorsConfigurationSource(...)
```

Do not change the allowed origins, methods, headers, credential setting, or URL scope.

- [x] **Step 4: Run focused and full IdP Admin tests**

Run the focused command from Step 2, then:

```bash
./mvnw -B -ntp -f egon-cola-platforms/pom.xml \
  -pl egon-cola-platform-idp/egon-cola-platform-idp-admin -am test
```

Expected: all tests pass with zero failures and zero errors.

### Task 2: Add browser-equivalent local acceptance checks

**Files:**
- Modify: `scripts/unified-platform/verify-local-stack.sh`
- Modify: `scripts/unified-identity-local.sh`

**Interfaces:**
- Consumes: four Admin Web origins, OAuth client IDs and token files, Vite proxy endpoints.
- Produces: a failing local verification whenever CORS, PKCE token exchange, an Admin Web root, or an authenticated frontend proxy path is unusable.

- [x] **Step 1: Add browser-boundary checks before rebuilding IdP**

Add reusable Bash assertions that check exact allow-origin and credential response headers for all four Admin Web origins. Make OAuth token exchange send the matching `Origin` header and reject responses missing the exact CORS headers. Add authenticated Vite proxy reads for IdP, RBAC3, Gateway, and DDC.

- [x] **Step 2: Run verification and verify RED against the old running IdP**

Run:

```bash
scripts/unified-platform/verify-local-stack.sh
```

Expected: it fails at the new IdP CORS boundary before deep platform checks.

- [x] **Step 3: Package and restart IdP in the persistent local-stack session**

Run:

```bash
./mvnw -B -ntp -f egon-cola-platforms/pom.xml \
  -pl egon-cola-platform-idp/egon-cola-platform-idp-admin -am \
  -DskipTests package
```

Restart IdP through the managed local scripts, then start all four Admin Web applications in the same persistent terminal session.

- [x] **Step 4: Run complete browser-equivalent and deep acceptance verification**

Run the full local verifier. Confirm all four page roots return `200`; all four configured origins receive exact CORS headers; OAuth authorization-code exchange succeeds for all clients; authenticated API requests through all four Vite proxies return `200`; and the existing identity, RBAC3, DDC, Gateway, and MCP deep checks still pass.

- [x] **Step 5: Review, commit, and leave services running**

Run `bash -n` for changed scripts, `git diff --check`, inspect the complete diff, confirm a clean post-commit worktree, and commit once with:

```bash
git commit -m "fix(idp): enable browser CORS verification"
```

### Task 3: Close frontend quality-gate gaps discovered during verification

**Files:**
- Modify: `egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-admin-web/package.json`
- Add: `egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-admin-web/eslint.config.js`
- Modify: IdP, RBAC3, and DDC frontend files reported by ESLint.

- [x] Add the same ESLint development toolchain and configuration already used by Gateway and DDC to IdP Admin Web.
- [x] Fix reported issues without suppressing rules or changing authentication semantics.
- [x] Give the complex DDC namespace interaction test an explicit per-test timeout and prove it stable with two complete consecutive runs.
- [x] Run lint, tests, and production builds across all four Admin Web applications.
