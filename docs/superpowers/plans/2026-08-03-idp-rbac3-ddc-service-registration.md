# Tianquan-Shoubing/Tianquan-Jianshen Tianshu Service Registration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make host-local Tianquan-Shoubing and Tianquan-Jianshen register healthy HTTP Provider instances in the Tianshu service catalog.

**Architecture:** Reuse the existing Tianshu registry client and `HttpProviderLeaseRuntime`. Enable the existing Tianshu config-client readiness gates together with Registry and HTTP Provider in the local runtime, while keeping Yuheng Reporting disabled and decoupling its optional status observer from Provider registration.

**Tech Stack:** Bash, Spring Boot YAML configuration, JUnit 5, Maven.

## Global Constraints

- Do not add dependencies or a second service-registration implementation.
- Preserve the existing rule that HTTP Provider publication requires a ready Tianshu config client.
- Do not expose generated Tianshu or Redis credentials in output or tracked files.
- Do not start or restart the project automatically after implementation.

---

### Task 1: Local Tianquan-Shoubing/Tianquan-Jianshen service-registration configuration

**Files:**
- Modify: `scripts/unified-xingyuan/test-direct-run-contract.sh`
- Modify: `scripts/unified-identity-local.sh`
- Modify: `egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin/src/main/resources/application-local.yml`
- Modify: `egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin/src/main/java/top/egon/cola/platform/tianquan-jianshen/admin/integration/runtime/Rbac3PlatformIntegrationConfiguration.java`
- Modify: `egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin/src/test/java/top/egon/cola/platform/tianquan-jianshen/admin/integration/GatewayDdcConfigurationTest.java`
- Modify: `egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin/src/test/java/top/egon/cola/platform/tianquan-jianshen/admin/integration/Rbac3AdminApplicationContextTest.java`

**Interfaces:**
- Consumes: `DdcRegistryAutoConfig`, `GatewayHttpProviderAutoConfiguration`, and `HttpProviderLeaseRuntime`.
- Produces: generated `tianquan-shoubing.properties` and `tianquan-jianshen.properties` with Registry connectivity and HTTP Provider identities.

- [x] **Step 1: Write failing runtime-generation and local-profile tests**

Add a direct-run test that invokes `write_service_env_files` in an isolated temporary runtime and asserts the generated Tianquan-Shoubing/Tianquan-Jianshen environment files enable Registry/Provider with literal expected identities and ports. Change the Tianquan-Jianshen local-profile assertion to expect independently configurable placeholders.

- [x] **Step 2: Verify the tests fail for the missing local registration configuration**

Run:

```bash
bash scripts/unified-xingyuan/test-direct-run-contract.sh
./mvnw -B -ntp -pl egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin -am -Dtest=GatewayDdcConfigurationTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: direct-run assertions fail because Tianquan-Shoubing/Tianquan-Jianshen Registry variables are absent, and the Tianquan-Jianshen test fails because the local profile contains literal `false` values.

- [x] **Step 3: Generate the minimal registration properties**

Add Tianshu config-client and HTTP Provider enable flags, `identity/local/default` scope, stable instance IDs, Tianshu signed endpoint credentials, explicit development plaintext, and Registry Redis database 10. Bind the Tianquan-Jianshen local profile to those flags, leave Yuheng Reporting disabled, and condition its lease-status observer on the Reporting service identity.

- [x] **Step 4: Verify targeted tests and build**

Run:

```bash
bash scripts/unified-xingyuan/test-direct-run-contract.sh
./mvnw -B -ntp -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin,egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin -am -Dtest=GatewayDdcConfigurationTest -Dsurefire.failIfNoSpecifiedTests=false test
./mvnw -B -ntp -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin,egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin -am package -DskipTests
```

Expected: all commands exit 0.

- [x] **Step 5: Commit**

```bash
git add docs/superpowers/specs/2026-08-03-tianquan-shoubing-tianquan-jianshen-tianshu-service-registration-design.md docs/superpowers/plans/2026-08-03-tianquan-shoubing-tianquan-jianshen-tianshu-service-registration.md scripts/unified-xingyuan/test-direct-run-contract.sh scripts/unified-identity-local.sh egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/resources/application.yml egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin/src/main/resources/application-local.yml egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin/src/test/java/top/egon/cola/platform/tianquan-jianshen/admin/integration/GatewayDdcConfigurationTest.java
git commit -m "fix(identity): register local tianquan-shoubing and tianquan-jianshen services"
```
