# Four Platform Direct Run Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make Tianquan-Shoubing, Tianquan-Jianshen, Yuheng, and Tianshu launchable from packaged JARs and frontend `npm run dev` commands, then prove the unified SSO and xingyuan feature chains end to end.

**Architecture:** The existing local preparation workflow remains the authority for database initialization and secret generation. It emits both shell `.env` files and Java `.properties` files from the same values; each executable Spring Boot JAR imports only its own optional properties file, while process environment and command-line arguments retain override precedence. Vite development proxies use the documented local backend ports by default, and the existing deep verifier is extended with authenticated admin read coverage.

**Tech Stack:** Bash 3.2, Java 21, Spring Boot 3, Maven, PostgreSQL, Redis, React, TypeScript, Vite, Vitest, Playwright, curl, jq.

## Global Constraints

- Keep PostgreSQL and Redis as real host-local dependencies; do not introduce embedded or fake persistence.
- Never commit generated passwords, client secrets, private keys, `.env` files, or `.properties` runtime files.
- Generated runtime configuration lives under `target/local-unified-xingyuan/env` and has mode `0600`.
- Direct backend commands are `java -jar <module>/target/*-exec.jar`; direct frontend commands are `npm run dev` from each admin web directory.
- Local endpoints are Tianquan-Shoubing `18120/18121`, Tianquan-Jianshen `18130/18131`, Yuheng Admin `18140/18141`, Yuheng Engine `18181`, and Tianshu `18150/18152`.
- Preserve environment-variable and command-line overrides and all existing unified-stack orchestration commands.
- Do not add dependencies, modify existing Flyway migrations, expose secret values, or refactor unrelated code.
- Pattern decision: use one small Shell-to-Java-properties adapter helper; reject Strategy, Factory, and inheritance because there is one stable encoding variation and no runtime strategy selection.

---

### Task 1: Generate Spring-readable local runtime configuration

**Files:**
- Modify: `scripts/unified-identity-local.sh`
- Test: `scripts/unified-xingyuan/test-direct-run-contract.sh`

**Interfaces:**
- Consumes: existing `new_env_file <name>` and `write_env <env-file> <key> <value>` calls.
- Produces: `properties_escape <value>` and matching `<name>.properties` files with the same keys as `<name>.env`.

- [ ] **Step 1: Add a failing direct-run contract test**

Create an executable Bash test which reads the implementation as text and fails unless `new_env_file` creates both file formats, `write_env` calls `properties_escape`, and all five required properties files are generated. It must also create a temporary properties sample through a test-only sourced-function path and assert that backslashes, tabs, carriage returns, and newlines are encoded as `\\`, `\t`, `\r`, and `\n` without printing the source value.

- [ ] **Step 2: Run the contract test and observe the red state**

Run:

```bash
bash scripts/unified-xingyuan/test-direct-run-contract.sh
```

Expected: non-zero with a message that `.properties` generation or `properties_escape` is missing.

- [ ] **Step 3: Implement the minimal twin-file adapter**

Add these responsibilities without changing existing callers:

```bash
properties_escape() {
  local value="$1"
  value="${value//\\/\\\\}"
  value="${value//$'\t'/\\t}"
  value="${value//$'\r'/\\r}"
  value="${value//$'\n'/\\n}"
  printf '%s' "${value}"
}
```

`new_env_file` truncates and `chmod 600`s both `${name}.env` and `${name}.properties`. `write_env` keeps the current `%q` shell output and appends `key=$(properties_escape "$value")` to the sibling properties file. The helper never logs values.

- [ ] **Step 4: Run the contract test and local prepare**

Run:

```bash
bash scripts/unified-xingyuan/test-direct-run-contract.sh
bash scripts/unified-identity-local.sh prepare
for name in tianquan-shoubing tianquan-jianshen yuheng-admin yuheng-biz-gateway tianshu; do
  test -s "target/local-unified-xingyuan/env/${name}.properties"
  test "$(stat -f '%Lp' "target/local-unified-xingyuan/env/${name}.properties")" = 600
done
```

Expected: every command exits `0`; no secret value appears in output.

- [ ] **Step 5: Commit**

```bash
git add scripts/unified-identity-local.sh scripts/unified-xingyuan/test-direct-run-contract.sh
git commit -m "feat(xingyuan): generate direct jar runtime properties"
```

### Task 2: Auto-import per-service runtime properties

**Files:**
- Modify: `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/resources/application.yml`
- Modify: `egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin/src/main/resources/application.yml`
- Modify: `egon-cola-xingyuan/egon-cola-yuheng/yuheng-admin/src/main/resources/application.yml`
- Modify: `egon-cola-xingyuan/egon-cola-yuheng/yuheng-biz-gateway/src/main/resources/application.yml`
- Modify: `egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/resources/application.yml`
- Test: `scripts/unified-xingyuan/test-direct-run-contract.sh`

**Interfaces:**
- Consumes: `target/local-unified-xingyuan/env/{tianquan-shoubing,tianquan-jianshen,yuheng-admin,yuheng-biz-gateway,tianshu}.properties` from Task 1.
- Produces: optional service-specific Spring imports controlled by `UNIFIED_XINGYUAN_RUNTIME_DIR`.

- [ ] **Step 1: Extend the failing contract assertions**

For every application YAML, assert both `profiles.default: local` and the exact service import:

```yaml
spring:
  profiles:
    default: local
  config:
    import: optional:file:${UNIFIED_XINGYUAN_RUNTIME_DIR:target/local-unified-xingyuan}/env/<service>.properties
```

For Tianshu, assert that `classpath:META-INF/egon-cola-tianshu.properties` remains in the import list.

- [ ] **Step 2: Verify the new assertions fail**

Run `bash scripts/unified-xingyuan/test-direct-run-contract.sh`.

Expected: non-zero identifying the first missing import.

- [ ] **Step 3: Add the five optional imports and local defaults**

Use the exact filenames `tianquan-shoubing.properties`, `tianquan-jianshen.properties`, `yuheng-admin.properties`, `yuheng-biz-gateway.properties`, and `tianshu.properties`. Preserve every existing YAML property and express Tianshu imports as a YAML list containing both the classpath resource and optional file.

- [ ] **Step 4: Validate resources and package executable JARs**

Run:

```bash
bash scripts/unified-xingyuan/test-direct-run-contract.sh
mvn -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin,egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin,egon-cola-xingyuan/egon-cola-yuheng/yuheng-admin,egon-cola-xingyuan/egon-cola-yuheng/yuheng-biz-gateway,egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin -am clean package -DskipTests
```

Expected: contract PASS and reactor `BUILD SUCCESS` with five `*-exec.jar` files.

- [ ] **Step 5: Commit**

```bash
git add scripts/unified-xingyuan/test-direct-run-contract.sh egon-cola-xingyuan/*/*/src/main/resources/application.yml
git commit -m "feat(xingyuan): auto-load direct jar configuration"
```

### Task 3: Make frontend development commands direct

**Files:**
- Modify: `egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin-web/vite.config.ts`
- Modify: `egon-cola-xingyuan/egon-cola-yuheng/yuheng-admin-web/vite.config.ts`
- Modify: `egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin-web/vite.config.ts`
- Test: `scripts/unified-xingyuan/test-direct-run-contract.sh`

**Interfaces:**
- Consumes: backend endpoints Tianquan-Jianshen `18130`, Yuheng Admin `18140`, Tianshu `18150`.
- Produces: working `/api` proxy defaults when the user runs plain `npm run dev`.

- [ ] **Step 1: Add failing Vite proxy assertions**

Assert that the four frontend configs resolve their API targets to Tianquan-Shoubing `18120`, Tianquan-Jianshen `18130`, Yuheng Admin `18140`, and Tianshu `18150` when no override is set.

- [ ] **Step 2: Verify the current Tianquan-Jianshen/Yuheng/Tianshu defaults fail**

Run `bash scripts/unified-xingyuan/test-direct-run-contract.sh`.

Expected: non-zero identifying `8080` or `18080` as stale defaults.

- [ ] **Step 3: Replace only the three stale default targets**

Keep the current environment override names and proxy options. Change only fallback URLs to:

```text
Tianquan-Jianshen  http://127.0.0.1:18130
Yuheng http://127.0.0.1:18140
Tianshu http://127.0.0.1:18150
```

- [ ] **Step 4: Run frontend validation**

Run the contract test, then each workspace's existing test, lint, and build commands. Expected: contract PASS, all Vitest suites pass, all linters exit `0`, and all Vite builds complete.

- [ ] **Step 5: Commit**

```bash
git add scripts/unified-xingyuan/test-direct-run-contract.sh egon-cola-xingyuan/*/*-admin-web/vite.config.ts
git commit -m "fix(xingyuan): align direct frontend proxy defaults"
```

### Task 4: Provide one safe preparation command and operator documentation

**Files:**
- Create: `scripts/unified-xingyuan/prepare-local-stack.sh`
- Modify: `docs/runbooks/unified-identity-local.md`
- Modify: `docs/operations/unified-identity-mcp-local-runbook.md`
- Test: `scripts/unified-xingyuan/test-direct-run-contract.sh`

**Interfaces:**
- Consumes: `scripts/unified-identity-local.sh prepare`, Maven, npm, and the five imports from Tasks 1-2.
- Produces: one idempotent preparation command and copyable direct-start commands with no sourced secrets.

- [ ] **Step 1: Add failing preparation/documentation assertions**

Assert that the wrapper invokes the existing prepare command, checks all five JARs and properties files, installs missing frontend dependencies using each existing lockfile, and never echoes secret file content. Assert that the runbooks contain all five direct `java -jar` commands and four plain `npm run dev` commands.

- [ ] **Step 2: Verify the assertions fail**

Run `bash scripts/unified-xingyuan/test-direct-run-contract.sh`.

Expected: non-zero because the wrapper does not exist.

- [ ] **Step 3: Implement the wrapper and exact runbook**

The wrapper resolves the repository root through `scripts/unified-xingyuan/lib/common.sh`, calls the existing prepare workflow, runs `npm ci` only when the relevant local Vite executable is absent, verifies artifacts and `0600` modes, and prints paths plus commands but no values. Documentation must distinguish first-time initialized setup from later direct launches and must use `127.0.0.1` consistently.

- [ ] **Step 4: Execute the preparation command twice**

Run:

```bash
bash scripts/unified-xingyuan/prepare-local-stack.sh
bash scripts/unified-xingyuan/prepare-local-stack.sh
bash scripts/unified-xingyuan/test-direct-run-contract.sh
```

Expected: both preparations exit `0`, proving idempotency, and the contract passes.

- [ ] **Step 5: Commit**

```bash
git add scripts/unified-xingyuan/prepare-local-stack.sh scripts/unified-xingyuan/test-direct-run-contract.sh docs/runbooks/unified-identity-local.md docs/operations/unified-identity-mcp-local-runbook.md
git commit -m "docs(xingyuan): document verified direct startup"
```

### Task 5: Expand authenticated admin feature-chain verification

**Files:**
- Modify: `scripts/unified-xingyuan/verify-local-stack.sh`

**Interfaces:**
- Consumes: existing browser OAuth tokens, initialized local topology, and four frontend proxy ports.
- Produces: a fail-fast read-only admin feature matrix recorded in the existing evidence summary.

- [ ] **Step 1: Add a reusable authenticated JSON assertion**

Implement `verify_authenticated_json <label> <url> <token>` using `curl --fail-with-body`, bearer authorization, JSON parsing through `jq`, and evidence entries that record only label/status—not token or response bodies.

- [ ] **Step 2: Add the read-only endpoint matrix**

Cover Tianquan-Shoubing users/clients/signing keys/audits; Tianquan-Jianshen runtime/tenants/users/applications/roles/management policies/SoD/data/field/operation rules/audits/session; Yuheng session/scopes/dashboard/groups/applications/draft/releases/runtime consistency/traces/audit; and Tianshu biz/env/app/namespace/binding/config/cache/registry/publish-task reads. Reuse initialized IDs from the verifier and preserve its existing mutation, revocation, Tianshu LKG, MCP failover, and recovery scenarios.

- [ ] **Step 3: Run the verifier against the managed stack and fix endpoint assumptions**

Run:

```bash
bash scripts/unified-xingyuan/start-local-stack.sh
bash scripts/unified-xingyuan/verify-local-stack.sh
```

Expected: every new matrix row and every existing scenario passes; evidence contains no token or secret values.

- [ ] **Step 4: Commit**

```bash
git add scripts/unified-xingyuan/verify-local-stack.sh
git commit -m "test(xingyuan): cover authenticated admin feature chains"
```

### Task 6: Prove exact direct commands and complete the release gate

**Files:**
- Verify only: all files from Tasks 1-5
- Evidence: `target/local-unified-xingyuan/evidence/`

**Interfaces:**
- Consumes: packaged JARs, generated properties, plain npm commands, and the full verifier.
- Produces: a clean Git commit set, running services, and reproducible acceptance evidence.

- [ ] **Step 1: Stop the managed stack and launch exact commands**

Run the stop command, then start the five primary JVMs from repository root using only `java -jar <absolute-exec-jar>` and the four web apps using only `npm run dev`. Do not source `.env`; record PIDs under the existing runtime directory so orchestration can add test fixtures without replacing the direct processes.

- [ ] **Step 2: Prove health and frontend proxy behavior**

Wait for backend Actuator health endpoints and frontend roots, then call `/api` through each Vite origin using its xingyuan token. Expected: HTTP `200`, valid JSON, and no `Failed to fetch`/CORS response.

- [ ] **Step 3: Run the complete regression gate**

Run clean backend tests/package, all four frontend test/lint/build suites, the direct-run contract, shell syntax checks, and `verify-local-stack.sh`. Expected: all exit `0`; no skipped required stage.

- [ ] **Step 4: Restore a persistent running topology and inspect evidence**

Restart any verifier-controlled process that intentionally stopped during fault scenarios. Confirm every documented port is listening, all health endpoints return `UP`, evidence has the current timestamp, and the process command lines still prove the primary services were launched by exact direct commands.

- [ ] **Step 5: Final review and commit any evidence-free corrections**

Run:

```bash
git diff --check
git status --short
git log --oneline --decorate -10
```

Expected: no whitespace errors, no generated runtime files tracked, a clean worktree, and all implementation commits present.
