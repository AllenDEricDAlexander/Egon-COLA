# IdP/RBAC3 DDC Service Registration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make host-local IdP and RBAC3 register healthy HTTP Provider instances in the DDC service catalog.

**Architecture:** Reuse the existing DDC registry client and `HttpProviderLeaseRuntime`. Enable the existing DDC config-client readiness gates together with Registry and HTTP Provider in the local runtime, while keeping Gateway Reporting disabled and decoupling its optional status observer from Provider registration.

**Tech Stack:** Bash, Spring Boot YAML configuration, JUnit 5, Maven.

## Global Constraints

- Do not add dependencies or a second service-registration implementation.
- Preserve the existing rule that HTTP Provider publication requires a ready DDC config client.
- Do not expose generated DDC or Redis credentials in output or tracked files.
- Do not start or restart the project automatically after implementation.

---

### Task 1: Local IdP/RBAC3 service-registration configuration

**Files:**
- Modify: `scripts/unified-platform/test-direct-run-contract.sh`
- Modify: `scripts/unified-identity-local.sh`
- Modify: `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/resources/application-local.yml`
- Modify: `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/integration/runtime/Rbac3PlatformIntegrationConfiguration.java`
- Modify: `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/integration/GatewayDdcConfigurationTest.java`
- Modify: `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/integration/Rbac3AdminApplicationContextTest.java`

**Interfaces:**
- Consumes: `DdcRegistryAutoConfig`, `GatewayHttpProviderAutoConfiguration`, and `HttpProviderLeaseRuntime`.
- Produces: generated `idp.properties` and `rbac3.properties` with Registry connectivity and HTTP Provider identities.

- [x] **Step 1: Write failing runtime-generation and local-profile tests**

Add a direct-run test that invokes `write_service_env_files` in an isolated temporary runtime and asserts the generated IdP/RBAC3 environment files enable Registry/Provider with literal expected identities and ports. Change the RBAC3 local-profile assertion to expect independently configurable placeholders.

- [x] **Step 2: Verify the tests fail for the missing local registration configuration**

Run:

```bash
bash scripts/unified-platform/test-direct-run-contract.sh
./mvnw -B -ntp -pl egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin -am -Dtest=GatewayDdcConfigurationTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: direct-run assertions fail because IdP/RBAC3 Registry variables are absent, and the RBAC3 test fails because the local profile contains literal `false` values.

- [x] **Step 3: Generate the minimal registration properties**

Add DDC config-client and HTTP Provider enable flags, `identity/local/default` scope, stable instance IDs, DDC signed endpoint credentials, explicit development plaintext, and Registry Redis database 10. Bind the RBAC3 local profile to those flags, leave Gateway Reporting disabled, and condition its lease-status observer on the Reporting service identity.

- [x] **Step 4: Verify targeted tests and build**

Run:

```bash
bash scripts/unified-platform/test-direct-run-contract.sh
./mvnw -B -ntp -pl egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-admin,egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin -am -Dtest=GatewayDdcConfigurationTest -Dsurefire.failIfNoSpecifiedTests=false test
./mvnw -B -ntp -pl egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-admin,egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin -am package -DskipTests
```

Expected: all commands exit 0.

- [x] **Step 5: Commit**

```bash
git add docs/superpowers/specs/2026-08-03-idp-rbac3-ddc-service-registration-design.md docs/superpowers/plans/2026-08-03-idp-rbac3-ddc-service-registration.md scripts/unified-platform/test-direct-run-contract.sh scripts/unified-identity-local.sh egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-admin/src/main/resources/application.yml egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/resources/application-local.yml egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/integration/GatewayDdcConfigurationTest.java
git commit -m "fix(identity): register local idp and rbac3 services"
```
