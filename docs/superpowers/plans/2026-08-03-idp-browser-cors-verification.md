# Tianquan-Shoubing Browser CORS and Admin Web Verification Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make the four local Admin Web applications complete their browser OAuth flow without CORS failures and prevent future handoff before browser-equivalent verification passes.

**Architecture:** Keep the existing cross-origin OAuth design: all Admin Web applications call Tianquan-Shoubing on `127.0.0.1:18120`, while their `/api` calls stay same-origin through Vite proxies. Register the existing restrictive Tianquan-Shoubing CORS policy under the bean name Spring Security actually resolves, then extend automated verification across every browser boundary.

**Tech Stack:** Java 21, Spring Boot 3.5, Spring Security, JUnit 5, MockMvc, Bash, curl, jq, Vite.

## Global Constraints

- Keep exact-origin allowlisting; do not use wildcard CORS origins.
- Keep credentialed OAuth cookies and the existing PKCE flow.
- Keep Tianshu Admin Web on `http://127.0.0.1:18152` and Remote MCP on `http://127.0.0.1:18151`.
- Do not change frontend authentication behavior or introduce runtime dependencies.
- Leave the complete local stack running after verification.

---

### Task 1: Activate the Tianquan-Shoubing CORS configuration in Spring Security

**Files:**
- Modify: `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan-shoubing/admin/security/IdpAdminSecurityConfiguration.java`
- Test: `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/tianquan-shoubing/admin/interfaces/http/IdpSsoLoginControllerIT.java`

**Interfaces:**
- Consumes: `egon.tianquan-shoubing.oauth.allowed-origins` and Spring Security's conventional `corsConfigurationSource` bean lookup.
- Produces: exact `Access-Control-Allow-Origin` and `Access-Control-Allow-Credentials: true` headers for configured origins under `/oauth2/**`.

- [x] **Step 1: Write the failing CORS integration tests**

Add test property `egon.tianquan-shoubing.oauth.allowed-origins=http://127.0.0.1:18121,http://127.0.0.1:18152`. Assert that `GET /oauth2/login/csrf` with origin `http://127.0.0.1:18152` returns that exact allow-origin header and credentials header. Assert that `OPTIONS /oauth2/login` permits `POST`, `Content-Type`, and `X-Tianquan-Shoubing-CSRF`. Assert that `http://localhost:18152` receives no allow-origin header.

- [x] **Step 2: Run the focused test and verify RED**

Run:

```bash
./mvnw -B -ntp -f egon-cola-xingyuan/pom.xml \
  -pl egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am \
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

- [x] **Step 4: Run focused and full Tianquan-Shoubing Admin tests**

Run the focused command from Step 2, then:

```bash
./mvnw -B -ntp -f egon-cola-xingyuan/pom.xml \
  -pl egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am test
```

Expected: all tests pass with zero failures and zero errors.

### Task 2: Add browser-equivalent local acceptance checks

**Files:**
- Modify: `scripts/unified-xingyuan/verify-local-stack.sh`
- Modify: `scripts/unified-identity-local.sh`

**Interfaces:**
- Consumes: four Admin Web origins, OAuth client IDs and token files, Vite proxy endpoints.
- Produces: a failing local verification whenever CORS, PKCE token exchange, an Admin Web root, or an authenticated frontend proxy path is unusable.

- [x] **Step 1: Add browser-boundary checks before rebuilding Tianquan-Shoubing**

Add reusable Bash assertions that check exact allow-origin and credential response headers for all four Admin Web origins. Make OAuth token exchange send the matching `Origin` header and reject responses missing the exact CORS headers. Add authenticated Vite proxy reads for Tianquan-Shoubing, Tianquan-Jianshen, Yuheng, and Tianshu.

- [x] **Step 2: Run verification and verify RED against the old running Tianquan-Shoubing**

Run:

```bash
scripts/unified-xingyuan/verify-local-stack.sh
```

Expected: it fails at the new Tianquan-Shoubing CORS boundary before deep xingyuan checks.

- [x] **Step 3: Package and restart Tianquan-Shoubing in the persistent local-stack session**

Run:

```bash
./mvnw -B -ntp -f egon-cola-xingyuan/pom.xml \
  -pl egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am \
  -DskipTests package
```

Restart Tianquan-Shoubing through the managed local scripts, then start all four Admin Web applications in the same persistent terminal session.

- [x] **Step 4: Run complete browser-equivalent and deep acceptance verification**

Run the full local verifier. Confirm all four page roots return `200`; all four configured origins receive exact CORS headers; OAuth authorization-code exchange succeeds for all clients; authenticated API requests through all four Vite proxies return `200`; and the existing identity, Tianquan-Jianshen, Tianshu, Yuheng, and MCP deep checks still pass.

- [x] **Step 5: Review, commit, and leave services running**

Run `bash -n` for changed scripts, `git diff --check`, inspect the complete diff, confirm a clean post-commit worktree, and commit once with:

```bash
git commit -m "fix(tianquan-shoubing): enable browser CORS verification"
```

### Task 3: Close frontend quality-gate gaps discovered during verification

**Files:**
- Modify: `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin-web/package.json`
- Add: `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin-web/eslint.config.js`
- Modify: Tianquan-Shoubing, Tianquan-Jianshen, and Tianshu frontend files reported by ESLint.

- [x] Add the same ESLint development toolchain and configuration already used by Yuheng and Tianshu to Tianquan-Shoubing Admin Web.
- [x] Fix reported issues without suppressing rules or changing authentication semantics.
- [x] Give the complex Tianshu namespace interaction test an explicit per-test timeout and prove it stable with two complete consecutive runs.
- [x] Run lint, tests, and production builds across all four Admin Web applications.
