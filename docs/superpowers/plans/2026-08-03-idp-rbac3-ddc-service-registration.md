# IdP/RBAC3 DDC Service Registration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make host-local IdP and RBAC3 register healthy HTTP Provider instances in the DDC service catalog.

**Architecture:** Reuse the existing DDC registry client and `HttpProviderLeaseRuntime`. Keep dynamic configuration and service registration independently switchable in the local profile, then make the local runtime generator enable only Registry and HTTP Provider for IdP/RBAC3.

**Tech Stack:** Bash, Spring Boot YAML configuration, JUnit 5, Maven.

## Global Constraints

- Do not add dependencies or a second service-registration implementation.
- Do not enable full DDC dynamic configuration merely to register a service.
- Do not expose generated DDC or Redis credentials in output or tracked files.
- Do not start or restart the project automatically after implementation.

---

### Task 1: Local IdP/RBAC3 service-registration configuration

**Files:**
- Modify: `scripts/unified-platform/test-direct-run-contract.sh`
- Modify: `scripts/unified-identity-local.sh`
- Modify: `egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-admin/src/main/resources/application.yml`
- Modify: `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/resources/application-local.yml`
- Modify: `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/integration/GatewayDdcConfigurationTest.java`

**Interfaces:**
- Consumes: `DdcRegistryAutoConfig`, `GatewayHttpProviderAutoConfiguration`, and `HttpProviderLeaseRuntime`.
- Produces: generated `idp.properties` and `rbac3.properties` with Registry connectivity and HTTP Provider identities.

- [ ] **Step 1: Write failing runtime-generation and local-profile tests**

Add a direct-run test that invokes `write_service_env_files` in an isolated temporary runtime and asserts the generated IdP/RBAC3 environment files enable Registry/Provider with literal expected identities and ports. Change the RBAC3 local-profile assertion to expect independently configurable placeholders.

- [ ] **Step 2: Verify the tests fail for the missing local registration configuration**

Run:

```bash
bash scripts/unified-platform/test-direct-run-contract.sh
./mvnw -B -ntp -pl egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin -am -Dtest=GatewayDdcConfigurationTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: direct-run assertions fail because IdP/RBAC3 Registry variables are absent, and the RBAC3 test fails because the local profile contains literal `false` values.

- [ ] **Step 3: Generate the minimal registration properties**

Add app-specific Registry enable flags, HTTP Provider enable flags, `identity/local/default` scope, stable instance IDs, DDC signed endpoint credentials, explicit development plaintext, and Registry Redis database 10. Bind the IdP and RBAC3 local profiles to those independent flags while leaving full DDC and Gateway Reporting default-disabled.

- [ ] **Step 4: Verify targeted tests and build**

Run:

```bash
bash scripts/unified-platform/test-direct-run-contract.sh
./mvnw -B -ntp -pl egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-admin,egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin -am -Dtest=GatewayDdcConfigurationTest -Dsurefire.failIfNoSpecifiedTests=false test
./mvnw -B -ntp -pl egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-admin,egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin -am package -DskipTests
```

Expected: all commands exit 0.

- [ ] **Step 5: Commit**

```bash
git add docs/superpowers/specs/2026-08-03-idp-rbac3-ddc-service-registration-design.md docs/superpowers/plans/2026-08-03-idp-rbac3-ddc-service-registration.md scripts/unified-platform/test-direct-run-contract.sh scripts/unified-identity-local.sh egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-admin/src/main/resources/application.yml egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/resources/application-local.yml egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/integration/GatewayDdcConfigurationTest.java
git commit -m "fix(identity): register local idp and rbac3 services"
```

