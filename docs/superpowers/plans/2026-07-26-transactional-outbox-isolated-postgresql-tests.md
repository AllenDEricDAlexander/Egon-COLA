# Transactional Outbox Isolated PostgreSQL Tests Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make the Transactional Outbox PostgreSQL integration tests independent of local database credentials while preserving real PostgreSQL coverage.

**Architecture:** Restore the approved Testcontainers-based test boundary. A singleton PostgreSQL 16.6 container supplies JDBC coordinates to the existing schema-per-test-class support; an explicit environment switch enables these database tests, while a dedicated GitHub Actions job always runs them on a Docker-capable host.

**Tech Stack:** Java 21, JUnit Jupiter, Testcontainers 1.21.4, PostgreSQL 16.6, Maven, GitHub Actions.

## Global Constraints

- Do not modify production Outbox behavior or the existing PostgreSQL migration.
- Do not connect to or mutate a developer's local PostgreSQL instance by default.
- Keep Testcontainers dependencies test-scoped and out of the starter dependency graph.
- Preserve real PostgreSQL semantics for transaction, lease, concurrency, recovery, and query-plan tests.
- Do not start a long-running application.

---

### Task 1: Restore isolated PostgreSQL integration-test infrastructure

**Files:**
- Modify: `egon-cola-components/egon-cola-component-transactional-outbox/egon-cola-component-transactional-outbox-test/pom.xml`
- Modify: `egon-cola-components/egon-cola-component-transactional-outbox/egon-cola-component-transactional-outbox-test/src/test/java/top/egon/cola/component/outbox/test/PostgresqlOutboxTestSupport.java`

**Interfaces:**
- Consumes: the existing nine integration-test subclasses of `PostgresqlOutboxTestSupport`.
- Produces: one shared `PostgreSQLContainer<?>` and a `PGSimpleDataSource` configured exclusively from that container.

- [ ] **Step 1: Reproduce the credential failure**

Run:

```bash
./mvnw -B -ntp \
  -pl :egon-cola-component-transactional-outbox-test \
  -am clean test
```

Expected before the fix: the PostgreSQL-backed tests fail from `PostgresqlOutboxTestSupport.initializePostgresql` because an empty fallback password is sent to the local SCRAM-enabled server.

- [ ] **Step 2: Restore test-scoped Testcontainers dependencies**

Add `testcontainers.version` with value `1.21.4` and the test-scoped `org.testcontainers:postgresql` dependency.

- [ ] **Step 3: Replace local connection defaults with the isolated container**

At the start of the inherited `@BeforeAll` method, abort the PostgreSQL test class with a JUnit assumption unless `EGON_OUTBOX_TEST_POSTGRES_ENABLED=true`. Add a static `PostgreSQLContainer<?>` using `postgres:16.6-alpine`, start it once when necessary, and configure `PGSimpleDataSource` from `getJdbcUrl()`, `getUsername()`, and `getPassword()`. Remove the local host, port, database, user, password, and environment-property fallback logic.

- [ ] **Step 4: Verify the focused module**

Run the same Maven command from Step 1.

Expected by default: the nine PostgreSQL-backed test classes are explicitly skipped and all remaining tests pass without Docker discovery. Expected with `EGON_OUTBOX_TEST_POSTGRES_ENABLED=true`: all 31 tests run, and the build fails if Docker or PostgreSQL cannot start.

- [ ] **Step 5: Verify the starter dependency boundary**

Run:

```bash
./mvnw -B -ntp \
  -pl :egon-cola-component-transactional-outbox-starter \
  -am dependency:tree \
  -Dincludes=org.testcontainers
```

Expected: the starter dependency tree contains no Testcontainers artifacts.

- [ ] **Step 6: Commit the isolated test infrastructure**

```bash
git add \
  egon-cola-components/egon-cola-component-transactional-outbox/egon-cola-component-transactional-outbox-test/pom.xml \
  egon-cola-components/egon-cola-component-transactional-outbox/egon-cola-component-transactional-outbox-test/src/test/java/top/egon/cola/component/outbox/test/PostgresqlOutboxTestSupport.java
git commit -m "test(outbox): isolate PostgreSQL integration tests"
```

### Task 2: Guarantee real PostgreSQL coverage in CI

**Files:**
- Modify: `.github/workflows/ci.yaml`
- Modify: `egon-cola-components/egon-cola-component-transactional-outbox/README.md`
- Modify: `egon-cola-components/egon-cola-component-transactional-outbox/README.zh-CN.md`

**Interfaces:**
- Consumes: the Testcontainers-enabled Outbox test module from Task 1 and the Docker service available on `ubuntu-24.04` runners.
- Produces: a required CI job that fails if the real PostgreSQL integration tests cannot start or pass.

- [ ] **Step 1: Add a host-level Outbox PostgreSQL job**

Add a Java 21 job that checks out the repository, configures Maven caching, verifies Docker availability, and executes:

```bash
./mvnw -B -ntp \
  -pl :egon-cola-component-transactional-outbox-test \
  -am clean verify
```

Set `EGON_OUTBOX_TEST_POSTGRES_ENABLED=true` so Docker discovery and PostgreSQL startup failures remain visible in this dedicated job.

- [ ] **Step 2: Validate workflow syntax and focused tests**

Parse `.github/workflows/ci.yaml` with the repository's available YAML parser, then rerun the focused Outbox Maven verification.

- [ ] **Step 3: Document the integration-test switch**

Document that the default reactor does not require a developer database, and that setting `EGON_OUTBOX_TEST_POSTGRES_ENABLED=true` runs the real PostgreSQL suite through Testcontainers and requires Docker.

- [ ] **Step 4: Commit the CI coverage and documentation**

```bash
git add \
  .github/workflows/ci.yaml \
  egon-cola-components/egon-cola-component-transactional-outbox/README.md \
  egon-cola-components/egon-cola-component-transactional-outbox/README.zh-CN.md
git commit -m "ci: verify outbox against PostgreSQL"
```

### Task 3: Final regression verification

**Files:**
- Verify only; no planned source changes.

**Interfaces:**
- Consumes: Tasks 1 and 2.
- Produces: evidence that the standard reactor no longer depends on local PostgreSQL credentials.

- [ ] **Step 1: Run the full reactor**

```bash
./mvnw -B -ntp clean verify
```

Expected: the full reactor completes without attempting to authenticate to `127.0.0.1:5432`.

- [ ] **Step 2: Check repository integrity**

```bash
git diff --check
git status --short --branch
```

Expected: no whitespace errors and no uncommitted implementation changes.
