# DDC Module Boundary and Starter Runtime Closure Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Remove the standalone DDC management-client artifact, make Starter the only consumer SDK, restore a Starter-only test module, and make every Admin connection path explicitly configured and verifiable.

**Architecture:** Move the existing management OpenAPI adapter, DTOs, HMAC primitives, and shared service metadata into the DDC Starter without changing Java package names or wire contracts. Keep DDC Admin as a standalone server depending on Starter, move Admin implementation tests back to Admin, and make config-client auto-configuration opt-in so Gateway/RPC users can consume shared contracts without starting the DDC runtime.

**Pattern Decision:** Retain the existing Adapter boundary (`DdcAdminClient` plus its HTTP implementation) and make that bean overrideable for tests. Do not introduce Strategy, Factory, or Facade layers: runtime variation is only Spring property gating, and another abstraction would not isolate a real new variation point.

**Tech Stack:** Java 21, Spring Boot 3.5.16, Maven reactor, Redisson, Spring `RestClient`, JUnit 5, AssertJ, Mockito, Testcontainers.

**Design:** `docs/superpowers/specs/2026-07-27-ddc-module-boundary-runtime-closure-design.md`

## Global Constraints

- Preserve existing REST paths, JSON fields, HMAC canonicalization, Redis keys, leases, synchronous publish, and ACK semantics.
- Preserve packages under `top.egon.cola.component.ddc.management` and `top.egon.cola.component.ddc.security`.
- Do not add third-party libraries; consolidate existing management-client dependencies into Starter and move Testcontainers to Admin test scope.
- Do not modify existing Flyway migrations under `classpath:db`; this change requires no migration.
- No consumer may depend on the executable DDC Admin artifact.
- Preserve unrelated workspace changes, especially the untracked architecture-audit Spec and `DdcAdminContextSmokeTest.java`.
- Before Task 1, create an isolated `codex/ddc-module-boundary-closure` worktree with `superpowers:using-git-worktrees`.
- Use TDD for behavior changes and observe each new test fail before implementation.
- Commit each task exactly once.
- Do not start DDC Admin, Gateway, RPC, Redis, PostgreSQL, or any application process.

---

## File Structure and Ownership

- DDC Starter `config/DdcProperties.java` owns explicit enablement and normalized Admin Endpoint validation.
- DDC Starter `config/DdcAutoConfig.java` owns opt-in config-client auto-configuration, adapter override, and offline-mode warning.
- DDC Starter `management/**` and `security/**` own the merged management adapter, DTOs, HMAC contract, and shared metadata types.
- DDC Admin `pom.xml` owns Admin runtime dependencies and Admin-only Testcontainers profiles.
- DDC Admin `egon-cola-component-dynamic-config-center-admin/src/test/java/top/egon/cola/component/ddc/admin/integration/**` owns synchronous-publish and Redis topology integration tests.
- DDC Test `pom.xml` contains only Starter, Spring Boot Web, and Spring Boot Test dependencies.
- DDC Test `DdcStarterRuntimeFlowTest.java` proves Starter orchestration without Admin classes.
- Gateway Admin and Gateway Starter depend on DDC Starter; Gateway Engine explicitly configures DDC runtime.
- Components BOM manages only DDC Starter.

---

### Task 1: Make runtime enablement and Admin Endpoint explicit

**Files:**
- Modify: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-starter/src/main/java/top/egon/cola/component/ddc/config/DdcProperties.java`
- Modify: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-starter/src/main/java/top/egon/cola/component/ddc/config/DdcAutoConfig.java`
- Modify: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-starter/src/main/java/top/egon/cola/component/ddc/client/HttpDdcAdminClient.java`
- Modify: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-starter/src/main/java/top/egon/cola/component/ddc/registry/DdcOpenApiServiceRegistryClient.java`
- Modify: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-starter/src/main/java/top/egon/cola/component/ddc/service/DdcRuntimeCoordinator.java`
- Create: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-starter/src/test/java/top/egon/cola/component/ddc/config/DdcPropertiesTest.java`
- Modify: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-starter/src/test/java/top/egon/cola/component/ddc/config/DdcAutoConfigTest.java`
- Modify: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-starter/src/test/java/top/egon/cola/component/ddc/config/DdcRegistryAutoConfigTest.java`
- Modify: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-starter/src/test/java/top/egon/cola/component/ddc/client/HttpDdcAdminClientTest.java`

**Interfaces:**
- Consumes: existing `DdcProperties.Admin` and both HTTP adapters.
- Produces: `String DdcProperties.Admin.requireEndpoint()`, `void DdcProperties.Admin.validateCredentials()`, `void DdcProperties.Instance.validate()`, and opt-in `DdcAutoConfig`.

- [ ] **Step 1: Write failing Endpoint tests**

Create `DdcPropertiesTest`:

```java
package top.egon.cola.component.ddc.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DdcPropertiesTest {

    @Test
    void adminEndpointIsRequiredAndNormalized() {
        DdcProperties.Admin admin = new DdcProperties.Admin();

        assertThatThrownBy(admin::requireEndpoint)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("egon.cola.component.ddc.admin.endpoint is required");

        admin.setEndpoint("http://ddc.test/");

        assertThat(admin.requireEndpoint()).isEqualTo("http://ddc.test");
    }

    @Test
    void adminEndpointRejectsNonRootUris() {
        DdcProperties.Admin admin = new DdcProperties.Admin();
        for (String endpoint : java.util.List.of(
                "file:///tmp/ddc",
                "http://ddc.test/context",
                "http://ddc.test?node=1",
                "http://ddc.test#fragment"
        )) {
            admin.setEndpoint(endpoint);
            assertThatThrownBy(admin::requireEndpoint)
                    .hasMessage("egon.cola.component.ddc.admin.endpoint must be an HTTP or HTTPS root URI");
        }
    }

    @Test
    void signedRequestsRequireBothCredentials() {
        DdcProperties.Admin admin = new DdcProperties.Admin();
        admin.setSignatureEnabled(true);

        assertThatThrownBy(admin::validateCredentials)
                .hasMessage("egon.cola.component.ddc.admin.access-key is required when signature is enabled");

        admin.setAccessKey("ak");
        assertThatThrownBy(admin::validateCredentials)
                .hasMessage("egon.cola.component.ddc.admin.secret-key is required when signature is enabled");

        admin.setSecretKey("sk");
        admin.validateCredentials();
    }

    @Test
    void configClientHeartbeatMustBePositiveAndShorterThanLease() {
        DdcProperties.Instance instance = new DdcProperties.Instance();
        instance.setHeartbeatIntervalSeconds(0);
        instance.setLeaseSeconds(30);

        assertThatThrownBy(instance::validate)
                .hasMessage("egon.cola.component.ddc.instance.heartbeat-interval-seconds must be positive and less than lease-seconds");

        instance.setHeartbeatIntervalSeconds(30);
        instance.setLeaseSeconds(30);

        assertThatThrownBy(instance::validate)
                .hasMessage("egon.cola.component.ddc.instance.heartbeat-interval-seconds must be positive and less than lease-seconds");

        instance.setHeartbeatIntervalSeconds(10);
        instance.validate();
    }
}
```

Add to `DdcAutoConfigTest`:

```java
@Test
void doesNotCreateBeansWhenEnableFlagIsMissing() {
    contextRunner.run(context ->
            assertThat(context).doesNotHaveBean(DdcAdminClient.class));
}

@Test
void warnsWhenRemoteLifecycleIsDisabled(CapturedOutput output) {
    contextRunner.withPropertyValues(
                    "egon.cola.component.ddc.enabled=true",
                    "egon.cola.component.ddc.redis.enabled=false",
                    "egon.cola.component.ddc.admin.endpoint=http://ddc.test",
                    "egon.cola.component.ddc.admin.tls.development-plaintext=true"
            )
            .run(context -> assertThat(output).contains(
                    "DDC remote lifecycle is disabled because "
                            + "egon.cola.component.ddc.redis.enabled=false; "
                            + "no registration, pull, subscription, heartbeat, or ACK will run"
            ));
}
```

Annotate the test class with `@ExtendWith(OutputCaptureExtension.class)` and import
`CapturedOutput`/`OutputCaptureExtension` from `org.springframework.boot.test.system`.
Add `egon.cola.component.ddc.admin.endpoint=http://ddc.test` to every enabled config-client and registry-only context fixture.
Then add this separate negative case to `DdcRegistryAutoConfigTest`:

```java
@Test
void registryFailsBeforeNetworkAccessWhenEndpointIsMissing() {
    contextRunner
            .withBean(
                    "ddcRegistryRedissonClient",
                    RedissonClient.class,
                    () -> mock(RedissonClient.class)
            )
            .withPropertyValues("egon.cola.component.ddc.admin.endpoint=")
            .run(context -> {
                assertThat(context).hasFailed();
                assertThat(context.getStartupFailure())
                        .hasRootCauseInstanceOf(IllegalArgumentException.class)
                        .hasRootCauseMessage(
                                "egon.cola.component.ddc.admin.endpoint is required"
                        );
            });
}
```

- [ ] **Step 2: Run the focused tests and verify red**

Run:

```bash
./mvnw -B -ntp \
  -pl egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-starter \
  -am -Dtest=DdcPropertiesTest,DdcAutoConfigTest,DdcRegistryAutoConfigTest \
  -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: compilation fails because `requireEndpoint()` is absent; after only adding that method, the missing-enable-flag test still fails because DDC currently defaults on.

- [ ] **Step 3: Implement the explicit property contract**

In `DdcProperties` change:

```java
private boolean enabled;
```

Remove the localhost Endpoint default and add:

```java
private String endpoint;

public String requireEndpoint() {
    if (endpoint == null || endpoint.isBlank()) {
        throw new IllegalArgumentException(
                "egon.cola.component.ddc.admin.endpoint is required"
        );
    }
    URI uri;
    try {
        uri = URI.create(endpoint.trim());
    } catch (IllegalArgumentException exception) {
        throw invalidEndpoint(exception);
    }
    boolean rootPath = uri.getPath() == null
            || uri.getPath().isBlank()
            || "/".equals(uri.getPath());
    if (!(("http".equalsIgnoreCase(uri.getScheme())
            || "https".equalsIgnoreCase(uri.getScheme()))
            && uri.getHost() != null
            && rootPath
            && uri.getRawQuery() == null
            && uri.getRawFragment() == null
            && uri.getUserInfo() == null)) {
        throw invalidEndpoint(null);
    }
    String normalized = endpoint.trim();
    return normalized.endsWith("/")
            ? normalized.substring(0, normalized.length() - 1)
            : normalized;
}

private IllegalArgumentException invalidEndpoint(Throwable cause) {
    String message =
            "egon.cola.component.ddc.admin.endpoint must be an HTTP or HTTPS root URI";
    return cause == null
            ? new IllegalArgumentException(message)
            : new IllegalArgumentException(message, cause);
}
```

Import `java.net.URI`. Change the class-level condition on `DdcAutoConfig` to `matchIfMissing=false`. Call `requireEndpoint()` in both HTTP adapters before scheme validation and use its normalized value as the `RestClient` base URL.

Add direct credential validation to `DdcProperties.Admin`:

```java
public void validateCredentials() {
    if (!signatureEnabled) {
        return;
    }
    if (accessKey == null || accessKey.isBlank()) {
        throw new IllegalArgumentException(
                "egon.cola.component.ddc.admin.access-key is required when signature is enabled"
        );
    }
    if (secretKey == null || secretKey.isBlank()) {
        throw new IllegalArgumentException(
                "egon.cola.component.ddc.admin.secret-key is required when signature is enabled"
        );
    }
}
```

Call it from both HTTP adapters. Add this method to `DdcProperties.Instance`:

```java
public void validate() {
    if (heartbeatIntervalSeconds <= 0 || heartbeatIntervalSeconds >= leaseSeconds) {
        throw new IllegalArgumentException(
                "egon.cola.component.ddc.instance.heartbeat-interval-seconds must be positive and less than lease-seconds"
        );
    }
}
```

Call `properties.getInstance().validate()` from `DdcRuntimeCoordinator.validateScope()`. Keep the existing
TLS file validation in `DdcClientTransportSecurity` and Redis mode/node validation in `DdcRedisTopology`;
do not duplicate them.

- [ ] **Step 4: Add transport and offline-mode assertions**

Add to `HttpDdcAdminClientTest`:

```java
@Test
void rejectsMissingEndpointBeforeCreatingTransport() {
    DdcProperties properties = new DdcProperties();

    assertThatThrownBy(() -> new HttpDdcAdminClient(properties))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("egon.cola.component.ddc.admin.endpoint is required");
}
```

In `DdcAutoConfig`, when DDC is enabled with `redis.enabled=false`, expose one `SmartInitializingSingleton` that logs exactly:

```text
DDC remote lifecycle is disabled because egon.cola.component.ddc.redis.enabled=false; no registration, pull, subscription, heartbeat, or ACK will run
```

Guard that bean with the existing top-level DDC condition plus:

```java
private static final Logger LOGGER = LoggerFactory.getLogger(DdcAutoConfig.class);

@Bean
@ConditionalOnProperty(
        prefix = "egon.cola.component.ddc.redis",
        name = "enabled",
        havingValue = "false"
)
public SmartInitializingSingleton ddcOfflineModeWarning() {
    return () -> LOGGER.warn(
            "DDC remote lifecycle is disabled because "
                    + "egon.cola.component.ddc.redis.enabled=false; "
                    + "no registration, pull, subscription, heartbeat, or ACK will run"
    );
}
```

Do not introduce a runtime-mode strategy or new interface.

- [ ] **Step 5: Run all Starter tests**

Run:

```bash
./mvnw -B -ntp \
  -pl egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-starter \
  -am test
```

Expected: `BUILD SUCCESS`; config-client beans are absent without the explicit flag, while registry-only enablement remains valid with an explicit Endpoint.

- [ ] **Step 6: Commit Task 1**

```bash
git add egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-starter
git commit -m "fix(ddc): require explicit runtime endpoints"
```

---

### Task 2: Consolidate management-client into Starter and rewire consumers atomically

**Files:**
- Move: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-management-client/src/main/java/top/egon/cola/component/ddc/management/**` to `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-starter/src/main/java/top/egon/cola/component/ddc/management/**`.
- Move: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-management-client/src/main/java/top/egon/cola/component/ddc/security/DdcCanonicalRequest.java` to `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-starter/src/main/java/top/egon/cola/component/ddc/security/DdcCanonicalRequest.java`.
- Move: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-management-client/src/main/java/top/egon/cola/component/ddc/security/DdcRequestSigner.java` to `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-starter/src/main/java/top/egon/cola/component/ddc/security/DdcRequestSigner.java`.
- Move: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-management-client/src/test/java/top/egon/cola/component/ddc/management/**` to `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-starter/src/test/java/top/egon/cola/component/ddc/management/**`.
- Delete: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-management-client/pom.xml` and its empty module directory.
- Modify: `egon-cola-components/egon-cola-component-dynamic-config-center/pom.xml`.
- Modify: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-starter/pom.xml`.
- Modify: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-admin/pom.xml`.
- Modify: `egon-cola-components/egon-cola-component-gateway/egon-cola-component-gateway-admin/pom.xml`.
- Modify: `egon-cola-components/egon-cola-component-gateway/egon-cola-component-gateway-starter/pom.xml`.
- Modify: `egon-cola-components/egon-cola-components-bom/pom.xml`.
- Modify: `egon-cola-components/egon-cola-component-gateway/egon-cola-component-gateway-engine/src/main/resources/application.yml`.
- Modify: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-starter/src/test/java/top/egon/cola/component/ddc/DdcComponentBoundaryTest.java`.

**Interfaces:**
- Consumes: all existing management Client, DTO, signer, and metadata signatures.
- Produces: identical Java packages from the Starter artifact; the old Maven coordinate is absent.

- [ ] **Step 1: Add a failing artifact-ownership test**

Extend `DdcComponentBoundaryTest`:

```java
@Test
void managementContractsArePackagedByStarter() {
    String location = top.egon.cola.component.ddc.management.DdcManagementClient.class
            .getProtectionDomain()
            .getCodeSource()
            .getLocation()
            .toString();

    assertThat(location)
            .contains("egon-cola-component-dynamic-config-center-starter")
            .doesNotContain("management-client");
}
```

- [ ] **Step 2: Run it and verify red**

```bash
./mvnw -B -ntp \
  -pl egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-starter \
  -am -Dtest=DdcComponentBoundaryTest \
  -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: FAIL because the class is loaded from management-client output.

- [ ] **Step 3: Move sources without renaming packages**

```bash
ddc_root=egon-cola-components/egon-cola-component-dynamic-config-center
client_root=$ddc_root/egon-cola-component-dynamic-config-center-management-client
starter_root=$ddc_root/egon-cola-component-dynamic-config-center-starter

git mv \
  "$client_root/src/main/java/top/egon/cola/component/ddc/management" \
  "$starter_root/src/main/java/top/egon/cola/component/ddc/management"

mkdir -p "$starter_root/src/main/java/top/egon/cola/component/ddc/security"
git mv \
  "$client_root/src/main/java/top/egon/cola/component/ddc/security/DdcCanonicalRequest.java" \
  "$starter_root/src/main/java/top/egon/cola/component/ddc/security/DdcCanonicalRequest.java"
git mv \
  "$client_root/src/main/java/top/egon/cola/component/ddc/security/DdcRequestSigner.java" \
  "$starter_root/src/main/java/top/egon/cola/component/ddc/security/DdcRequestSigner.java"

git mv \
  "$client_root/src/test/java/top/egon/cola/component/ddc/management" \
  "$starter_root/src/test/java/top/egon/cola/component/ddc/management"
```

Keep package declarations unchanged.

- [ ] **Step 4: Rewire Maven coordinates in one patch**

The DDC root modules must be:

```xml
<modules>
    <module>egon-cola-component-dynamic-config-center-starter</module>
    <module>egon-cola-component-dynamic-config-center-admin</module>
    <module>egon-cola-component-dynamic-config-center-test</module>
</modules>
```

Remove management-client from DDC dependency management, Starter, Admin, and the components BOM. Add `jackson-databind` directly to Starter. Replace the old dependency in Gateway Admin and Gateway Starter with:

```xml
<dependency>
    <groupId>top.egon</groupId>
    <artifactId>egon-cola-component-dynamic-config-center-starter</artifactId>
    <version>${project.version}</version>
</dependency>
```

Keep Gateway Starter's direct dependency even though optional RPC may also reference Starter. Delete the old module POM only after all consumers are rewired.

- [ ] **Step 5: Make Gateway Engine runtime configuration explicit**

Use this DDC block:

```yaml
ddc:
  enabled: true
  app-code: egon-cola-gateway-engine
  admin:
    endpoint: ${DDC_ADMIN_ENDPOINT:http://127.0.0.1:18080}
    signature-enabled: ${DDC_OPENAPI_SIGNATURE_ENABLED:true}
    access-key: ${DDC_OPENAPI_ACCESS_KEY:}
    secret-key: ${DDC_OPENAPI_SECRET:}
    tls:
      enabled: false
      development-plaintext: true
  redis:
    enabled: true
    mode: ${DDC_REDIS_MODE:SINGLE}
    host: ${DDC_REDIS_HOST:127.0.0.1}
    port: ${DDC_REDIS_PORT:6379}
    password: ${DDC_REDIS_PASSWORD:}
    database: ${DDC_REDIS_DATABASE:0}
  registry:
    enabled: true
```

The local Admin and Redis addresses remain visible at the application boundary, not hidden inside Starter.

- [ ] **Step 6: Run DDC/Gateway/RPC tests**

```bash
./mvnw -B -ntp \
  -pl egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-starter,egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-admin,egon-cola-components/egon-cola-component-gateway/egon-cola-component-gateway-admin,egon-cola-components/egon-cola-component-gateway/egon-cola-component-gateway-starter,egon-cola-components/egon-cola-component-gateway/egon-cola-component-gateway-engine,egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter \
  -am test
```

Expected: `BUILD SUCCESS`; moved tests run under Starter and Gateway/RPC imports compile unchanged.

- [ ] **Step 7: Prove the old Maven coordinate is absent**

```bash
if rg -n "egon-cola-component-dynamic-config-center-management-client" \
  --glob 'pom.xml' egon-cola-components; then
  exit 1
fi
```

Expected: no output, exit 0.

- [ ] **Step 8: Commit Task 2**

```bash
git add \
  egon-cola-components/egon-cola-component-dynamic-config-center \
  egon-cola-components/egon-cola-component-gateway/egon-cola-component-gateway-admin/pom.xml \
  egon-cola-components/egon-cola-component-gateway/egon-cola-component-gateway-starter/pom.xml \
  egon-cola-components/egon-cola-component-gateway/egon-cola-component-gateway-engine/src/main/resources/application.yml \
  egon-cola-components/egon-cola-components-bom/pom.xml
git commit -m "refactor(ddc): consolidate clients into starter"
```

---

### Task 3: Return Admin implementation and Redis topology tests to Admin

**Files:**
- Move: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-test/src/test/java/top/egon/cola/component/ddc/test/DdcSyncPublishFlowTest.java` to `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-admin/src/test/java/top/egon/cola/component/ddc/admin/integration/DdcSyncPublishFlowTest.java`.
- Move: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-test/src/test/java/top/egon/cola/component/ddc/test/DdcRedisSentinelIT.java` to `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-admin/src/test/java/top/egon/cola/component/ddc/admin/integration/DdcRedisSentinelIT.java`.
- Move: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-test/src/test/java/top/egon/cola/component/ddc/test/DdcRedisClusterIT.java` to `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-admin/src/test/java/top/egon/cola/component/ddc/admin/integration/DdcRedisClusterIT.java`.
- Create: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-admin/src/test/java/top/egon/cola/component/ddc/admin/repository/DdcServiceRegistryPersistenceBoundaryTest.java`.
- Modify: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-test/src/test/java/top/egon/cola/component/ddc/test/DdcRegistryLifecycleTest.java`.
- Modify: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-admin/pom.xml`.
- Modify: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-test/pom.xml`.
- Create: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-test/src/test/java/top/egon/cola/component/ddc/test/DdcTestDependencyBoundaryTest.java`.

**Interfaces:**
- Consumes: existing Admin Repository/Service classes and profile names.
- Produces: Admin-free DDC Test classpath; unchanged Sentinel/Cluster profile names under Admin.

- [ ] **Step 1: Add a failing dependency-boundary test**

```java
package top.egon.cola.component.ddc.test;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DdcTestDependencyBoundaryTest {

    @Test
    void adminImplementationIsNotOnTheTestClasspath() {
        assertThatThrownBy(() -> Class.forName(
                "top.egon.cola.component.ddc.admin.service.DdcConfigService"
        )).isInstanceOf(ClassNotFoundException.class);
    }
}
```

- [ ] **Step 2: Run it and verify red**

```bash
./mvnw -B -ntp \
  -pl egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-test \
  -am -Dtest=DdcTestDependencyBoundaryTest \
  -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: FAIL because Admin is on the test classpath.

- [ ] **Step 3: Move the Admin integration tests**

Move to:

```text
egon-cola-component-dynamic-config-center-admin/src/test/java/top/egon/cola/component/ddc/admin/integration/DdcSyncPublishFlowTest.java
egon-cola-component-dynamic-config-center-admin/src/test/java/top/egon/cola/component/ddc/admin/integration/DdcRedisSentinelIT.java
egon-cola-component-dynamic-config-center-admin/src/test/java/top/egon/cola/component/ddc/admin/integration/DdcRedisClusterIT.java
```

Change each package declaration to:

```java
package top.egon.cola.component.ddc.admin.integration;
```

Do not change assertions, topology setup, publish timing, or class names.

- [ ] **Step 4: Split the persistence assertion**

Remove the JPA/SQL assertion and its Admin imports from `DdcRegistryLifecycleTest`. Create Admin `DdcServiceRegistryPersistenceBoundaryTest`:

```java
package top.egon.cola.component.ddc.admin.repository;

import jakarta.persistence.Entity;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import top.egon.cola.component.ddc.model.registry.DdcServiceInstance;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DdcServiceRegistryPersistenceBoundaryTest {

    @Test
    void serviceRegistryIsRedisOnlyAndDoesNotIntroduceJpaTables() throws IOException {
        assertThat(DdcServiceInstance.class.isAnnotationPresent(Entity.class)).isFalse();
        assertThat(DdcServiceRegistryRedisRepository.class.isAnnotationPresent(Entity.class))
                .isFalse();

        for (String dialect : List.of("postgresql", "sqlite")) {
            String schema = resource("db/" + dialect + "/V1__create_ddc_schema.sql")
                    + resource("db/" + dialect + "/V2__add_lease_and_sync_publish.sql");
            assertThat(schema)
                    .doesNotContain("ddc_service_instance")
                    .doesNotContain("ddc_service_registry");
        }
    }

    private String resource(String path) throws IOException {
        return new ClassPathResource(path).getContentAsString(StandardCharsets.UTF_8);
    }
}
```

- [ ] **Step 5: Move Testcontainers and profiles to Admin POM**

Move exactly:

```xml
<properties>
    <testcontainers.version>1.21.4</testcontainers.version>
</properties>

<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>testcontainers</artifactId>
    <version>${testcontainers.version}</version>
    <scope>test</scope>
</dependency>
```

Move complete `ddc-redis-sentinel` and `ddc-redis-cluster` profiles unchanged to Admin POM. Remove Admin test-scope dependency, Testcontainers, version property, and profiles from DDC Test POM.

- [ ] **Step 6: Run Admin and consumer tests**

```bash
./mvnw -B -ntp \
  -pl egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-admin,egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-test \
  -am test
```

Expected: `BUILD SUCCESS`; synchronous-publish tests run under Admin and the classpath boundary passes.

- [ ] **Step 7: Verify profile discovery without starting containers**

```bash
./mvnw -B -ntp \
  -pl egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-admin \
  help:effective-pom -Pddc-redis-sentinel,ddc-redis-cluster \
  -Doutput=target/ddc-effective-pom.xml

rg -n "DdcRedisSentinelIT|DdcRedisClusterIT" \
  egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-admin/target/ddc-effective-pom.xml
```

Expected: both includes appear. Do not execute these profiles without separately available Docker/Testcontainers validation.

- [ ] **Step 8: Commit Task 3**

```bash
git add \
  egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-admin/pom.xml \
  egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-admin/src/test \
  egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-test/pom.xml \
  egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-test/src/test
git commit -m "test(ddc): restore starter-only consumer module"
```

---

### Task 4: Prove Starter-only registration and configuration read

**Files:**
- Modify: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-starter/src/main/java/top/egon/cola/component/ddc/config/DdcAutoConfig.java`.
- Modify: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-starter/src/test/java/top/egon/cola/component/ddc/config/DdcAutoConfigTest.java`.
- Create: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-test/src/test/java/top/egon/cola/component/ddc/test/DdcStarterRuntimeFlowTest.java`.
- Modify: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-test/src/test/java/top/egon/cola/component/ddc/test/DdcSampleInjectionTest.java`.
- Modify: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-test/src/main/resources/application.yml`.

**Interfaces:**
- Consumes: `DdcAdminClient`, the real `DdcRedisChangeSubscription`, a mocked named `RedissonClient`, `DdcRuntimeCoordinator`, and `SampleConfigService`.
- Produces: overrideable Admin adapter and a Starter-only Spring Boot lifecycle proof.

- [ ] **Step 1: Add a failing adapter-override test**

Add to `DdcAutoConfigTest`:

```java
@Test
void retainsUserProvidedAdminClient() {
    DdcAdminClient client = mock(DdcAdminClient.class);

    contextRunner
            .withBean(DdcAdminClient.class, () -> client)
            .withPropertyValues(
                    "egon.cola.component.ddc.enabled=true",
                    "egon.cola.component.ddc.redis.enabled=false",
                    "egon.cola.component.ddc.admin.endpoint=http://ddc.test"
            )
            .run(context -> assertThat(context.getBean(DdcAdminClient.class))
                    .isSameAs(client));
}
```

- [ ] **Step 2: Run it and verify red**

```bash
./mvnw -B -ntp \
  -pl egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-starter \
  -am -Dtest=DdcAutoConfigTest#retainsUserProvidedAdminClient \
  -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: FAIL because auto-configuration creates another `DdcAdminClient`.

- [ ] **Step 3: Make the HTTP adapter overrideable**

```java
@Bean
@ConditionalOnMissingBean(DdcAdminClient.class)
public DdcAdminClient ddcAdminClient(DdcProperties properties) {
    return new HttpDdcAdminClient(properties);
}
```

Reuse the existing port; do not create another interface.

- [ ] **Step 4: Add the Starter-only runtime flow test**

Use:

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, properties = {
        "egon.cola.component.ddc.enabled=true",
        "egon.cola.component.ddc.app-code=demo-app",
        "egon.cola.component.ddc.env=dev",
        "egon.cola.component.ddc.namespace=default",
        "egon.cola.component.ddc.admin.endpoint=http://ddc.test",
        "egon.cola.component.ddc.admin.tls.development-plaintext=true",
        "egon.cola.component.ddc.redis.enabled=true",
        "egon.cola.component.ddc.consistency.fail-fast=true",
        "egon.cola.component.ddc.instance.lease-seconds=7200",
        "egon.cola.component.ddc.instance.heartbeat-interval-seconds=3600"
})
@Import(DdcStarterRuntimeFlowTest.RuntimeTestConfiguration.class)
class DdcStarterRuntimeFlowTest {

    @Autowired
    private DdcRuntimeCoordinator runtimeCoordinator;

    @Autowired
    private RecordingDdcAdminClient adminClient;

    @Autowired
    private SampleConfigService sampleConfigService;

    @Test
    void starterRegistersPullsAppliesAndGoesOfflineWithoutAdminClasses() {
        assertThat(runtimeCoordinator.state()).isEqualTo(DdcRuntimeState.READY);
        assertThat(adminClient.events()).containsExactly("register", "defaults", "pull");
        assertThat(sampleConfigService.getRateLimit()).isEqualTo(250);
        assertThatThrownBy(() -> Class.forName(
                "top.egon.cola.component.ddc.admin.service.DdcConfigService"
        )).isInstanceOf(ClassNotFoundException.class);

        runtimeCoordinator.stop();

        assertThat(adminClient.events())
                .containsExactly("register", "defaults", "pull", "offline");
    }
}
```

Nested configuration:

```java
@TestConfiguration(proxyBeanMethods = false)
static class RuntimeTestConfiguration {

    @Bean
    RecordingDdcAdminClient recordingDdcAdminClient() {
        return new RecordingDdcAdminClient();
    }

    @Bean(name = "ddcRedissonClient", destroyMethod = "")
    RedissonClient ddcRedissonClient() {
        RedissonClient client = mock(RedissonClient.class);
        RTopic topic = mock(RTopic.class);
        when(client.getTopic(anyString())).thenReturn(topic);
        when(topic.addListener(eq(DdcPublishMessage.class), any()))
                .thenReturn(1);
        return client;
    }
}
```

This keeps the production subscription bean in the graph while replacing only its external Redis transport.
`RecordingDdcAdminClient` records `register`, `defaults`, `pull`, and `offline`. Declare it in the
test as:

```java
static final class RecordingDdcAdminClient implements DdcAdminClient {

    private final List<String> events = new ArrayList<>();

@Override
public DdcLeaseSession register(DdcInstanceRegisterRequest request) {
    events.add("register");
    Instant registeredAt = Instant.now();
    return new DdcLeaseSession(
            request.getInstanceId(),
            "lease-1",
            DdcLeaseRole.CONFIG_CLIENT,
            request.getLeaseSeconds(),
            request.getHeartbeatIntervalSeconds(),
            registeredAt,
            registeredAt.plusSeconds(request.getLeaseSeconds())
    );
}

@Override
public DdcLeaseOperationResult heartbeat(DdcHeartbeatRequest request) {
    return new DdcLeaseOperationResult(
            DdcLeaseOperationStatus.RENEWED,
            Instant.now().plusSeconds(7200)
    );
}

@Override
public DdcLeaseOperationResult offline(DdcHeartbeatRequest request) {
    events.add("offline");
    return new DdcLeaseOperationResult(DdcLeaseOperationStatus.DELETED, null);
}

@Override
public List<DdcConfigValue> pull() {
    events.add("pull");
    DdcConfigValue value = new DdcConfigValue();
    value.setConfigKey("rateLimit");
    value.setConfigValue("250");
    value.setValueType(Integer.class.getName());
    value.setVersion(1L);
    return List.of(value);
}

@Override
public void reportDefaults(DdcDefaultReportRequest request) {
    events.add("defaults");
}

@Override
public void ack(DdcAckRequest request) {
}

List<String> events() {
    return List.copyOf(events);
}
}
```

Import the concrete request/result types, `ArrayList`, `Instant`, and the Mockito matchers used by the
Redis transport stub.

- [ ] **Step 5: Run the lifecycle test**

```bash
./mvnw -B -ntp \
  -pl egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-test \
  -am -Dtest=DdcStarterRuntimeFlowTest \
  -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: `BUILD SUCCESS` after the adapter override is implemented; the value is 250 and event order is exact.

- [ ] **Step 6: Make offline-default behavior explicit**

Add Endpoint configuration to `DdcSampleInjectionTest`, autowire `ObjectProvider<DdcRuntimeCoordinator>`, rename the method to `offlineModeKeepsAnnotationDefaultsWithoutRegisteringOrPulling`, and assert:

```java
assertThat(runtimeCoordinator.getIfAvailable()).isNull();
assertThat(sampleConfigService.getDowngradeSwitch()).isFalse();
assertThat(sampleConfigService.getRateLimit()).isEqualTo(100);
```

- [ ] **Step 7: Replace the runnable sample DDC block**

```yaml
egon:
  cola:
    component:
      ddc:
        enabled: ${DDC_ENABLED:false}
        app-code: ${DDC_APP_CODE:demo-app}
        env: ${DDC_ENV:dev}
        namespace: ${DDC_NAMESPACE:default}
        admin:
          endpoint: ${DDC_ADMIN_ENDPOINT:}
          signature-enabled: ${DDC_SIGNATURE_ENABLED:true}
          access-key: ${DDC_ACCESS_KEY:}
          secret-key: ${DDC_SECRET_KEY:}
          tls:
            enabled: ${DDC_TLS_ENABLED:false}
            development-plaintext: ${DDC_DEVELOPMENT_PLAINTEXT:false}
        redis:
          enabled: ${DDC_REDIS_ENABLED:true}
          mode: ${DDC_REDIS_MODE:SINGLE}
          host: ${DDC_REDIS_HOST:127.0.0.1}
          port: ${DDC_REDIS_PORT:6379}
          password: ${DDC_REDIS_PASSWORD:}
          database: ${DDC_REDIS_DATABASE:0}
        instance:
          lease-seconds: ${DDC_LEASE_SECONDS:30}
          heartbeat-interval-seconds: ${DDC_HEARTBEAT_INTERVAL_SECONDS:10}
        registry:
          enabled: ${DDC_REGISTRY_ENABLED:false}
        consistency:
          fail-fast: ${DDC_FAIL_FAST:true}
```

- [ ] **Step 8: Run Starter and DDC Test**

```bash
./mvnw -B -ntp \
  -pl egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-starter,egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-test \
  -am test
```

Expected: `BUILD SUCCESS`; one test proves the full Starter orchestration, the other proves Redis-disabled offline mode has no coordinator.

- [ ] **Step 9: Commit Task 4**

```bash
git add \
  egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-starter/src/main/java/top/egon/cola/component/ddc/config/DdcAutoConfig.java \
  egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-starter/src/test/java/top/egon/cola/component/ddc/config/DdcAutoConfigTest.java \
  egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-test/src
git commit -m "test(ddc): prove starter runtime lifecycle"
```

---

### Task 5: Update current documentation and complete reactor verification

**Files:**
- Modify: `egon-cola-components/egon-cola-component-dynamic-config-center/README.md`.
- Modify: `egon-cola-components/egon-cola-component-dynamic-config-center/README.zh-CN.md`.
- Modify: `egon-cola-components/egon-cola-components-bom/README.md`.
- Modify: `egon-cola-components/egon-cola-components-bom/README.zh-CN.md`.
- Do not modify historical files under `docs/superpowers/specs` or `docs/superpowers/plans`.

**Interfaces:**
- Consumes: final module tree and explicit configuration from Tasks 1–4.
- Produces: current documentation, dependency-tree proof, and full reactor evidence.

- [ ] **Step 1: Capture stale documentation matches**

```bash
rg -n "egon-cola-component-dynamic-config-center-management-client" \
  egon-cola-components/egon-cola-component-dynamic-config-center/README.md \
  egon-cola-components/egon-cola-component-dynamic-config-center/README.zh-CN.md \
  egon-cola-components/egon-cola-components-bom/README.md \
  egon-cola-components/egon-cola-components-bom/README.zh-CN.md
```

Expected: stale module-table matches.

- [ ] **Step 2: Rewrite current dependency and configuration documentation**

Replace the English DDC module table with:

```markdown
| Module | Responsibility |
|---|---|
| `egon-cola-component-dynamic-config-center-starter` | The only consumer SDK: `@DdcValue`, typed management APIs, startup synchronization, refresh, ACK, CONFIG_CLIENT leases, HMAC, and service-registry contracts |
| `egon-cola-component-dynamic-config-center-admin` | Standalone REST Admin, PostgreSQL persistence, Redis cache and leases, registry APIs, and synchronous publish state machine |
| `egon-cola-component-dynamic-config-center-test` | Starter-only sample and black-box consumer verification; it has no Admin dependency |
```

Immediately after it add:

```markdown
Applications add only the Starter. `egon.cola.component.ddc.enabled=true` explicitly
starts the `CONFIG_CLIENT` registration, default-report, pull, Redis subscription,
heartbeat, and shutdown-offline lifecycle. `egon.cola.component.ddc.registry.enabled=true`
independently enables RPC/Gateway service registration; those `RPC_PROVIDER`,
`HTTP_PROVIDER`, and `INTERNAL_GATEWAY` leases are not configuration-client registrations. Every enabled
remote path must explicitly configure the Admin Endpoint, matching HMAC credentials,
and Redis topology. With `redis.enabled=false`, no registration, pull, subscription,
heartbeat, or ACK runs. Production multi-Admin access must use an external DNS name,
VIP, or load balancer; Starter does not discover Admin processes.
```

Follow it with this single consumer dependency example:

```xml
<dependency>
    <groupId>top.egon</groupId>
    <artifactId>egon-cola-component-dynamic-config-center-starter</artifactId>
</dependency>
```

Replace the Chinese DDC module table with:

```markdown
| 模块 | 职责 |
|---|---|
| `egon-cola-component-dynamic-config-center-starter` | 唯一业务侧 SDK：`@DdcValue`、类型化管理 API、启动同步、刷新、ACK、CONFIG_CLIENT 租约、HMAC 和服务注册契约 |
| `egon-cola-component-dynamic-config-center-admin` | 独立 REST Admin、PostgreSQL 持久化、Redis 缓存与租约、注册中心 API 和同步发布状态机 |
| `egon-cola-component-dynamic-config-center-test` | 仅依赖 Starter 的样例与黑盒消费端验证，不依赖 Admin |
```

Immediately after it add:

```markdown
业务应用只引入 Starter。`egon.cola.component.ddc.enabled=true` 会显式启动
`CONFIG_CLIENT` 注册、默认值上报、配置拉取、Redis 订阅、心跳和停机下线闭环；
`egon.cola.component.ddc.registry.enabled=true` 独立启用 RPC/Gateway 服务注册，
其中 `RPC_PROVIDER`、`HTTP_PROVIDER` 和 `INTERNAL_GATEWAY` 租约不是配置客户端注册。启用任一远程
路径时都必须显式配置 Admin Endpoint、匹配的 HMAC 凭据和 Redis 拓扑。
`redis.enabled=false` 时不会执行注册、拉取、订阅、心跳或 ACK。生产多 Admin
入口由外部 DNS、VIP 或负载均衡提供，Starter 不负责发现 Admin 进程。
```

随后给出唯一的业务侧依赖示例：

```xml
<dependency>
    <groupId>top.egon</groupId>
    <artifactId>egon-cola-component-dynamic-config-center-starter</artifactId>
</dependency>
```

In the English and Chinese Scope paragraphs, remove wording that describes a separately published
typed management client. In both BOM export tables delete only the management-client row. Replace BOM
design principle 3 with these exact sentences:

```markdown
3. Regular business components export only their starter, keeping the Spring Boot auto-configuration entry point explicit. DDC includes its typed management API in Starter, Gateway exposes its Provider Runtime separately, and the bytecode component manages its public API, bridge, runtime, Agent, and starter boundaries separately.

3. 常规业务组件只导出 starter，保持 Spring Boot 自动配置入口明确；DDC 的类型化管理 API 合并在 Starter 中，Gateway 单独导出 Provider Runtime，字节码组件按公开的 API、桥接、运行时、Agent 和 starter 边界分别管理版本。
```

- [ ] **Step 3: Prove active files contain no stale coordinate**

```bash
rg -n "egon-cola-component-dynamic-config-center-management-client" \
  --glob '!docs/superpowers/specs/**' \
  --glob '!docs/superpowers/plans/**' \
  --glob '!**/target/**' .
```

Expected: no output. Historical design chronology is intentionally excluded.

- [ ] **Step 4: Verify DDC Test dependency tree**

```bash
./mvnw -B -ntp \
  -pl egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-test \
  -am dependency:tree '-Dincludes=top.egon:*'
```

Expected: Starter and common modules only; no DDC Admin and no management-client.

- [ ] **Step 5: Run targeted DDC/Gateway/RPC tests**

```bash
./mvnw -B -ntp \
  -pl egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-starter,egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-admin,egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-test,egon-cola-components/egon-cola-component-gateway/egon-cola-component-gateway-admin,egon-cola-components/egon-cola-component-gateway/egon-cola-component-gateway-starter,egon-cola-components/egon-cola-component-gateway/egon-cola-component-gateway-engine,egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter \
  -am clean test
```

Expected: `BUILD SUCCESS` with zero failures.

- [ ] **Step 6: Run full repository verification**

```bash
./mvnw -B -ntp clean integration-test
```

Expected: `BUILD SUCCESS`. This proves default-profile Maven wiring, not external Redis, PostgreSQL, DNS/VIP, or multi-Admin deployment.

- [ ] **Step 7: Inspect final scope**

```bash
git diff --check
git status --short
git diff --stat HEAD
```

Expected: only DDC, Gateway dependency/configuration, BOM, and current documentation files from this plan are changed.

- [ ] **Step 8: Commit Task 5**

```bash
git add \
  egon-cola-components/egon-cola-component-dynamic-config-center/README.md \
  egon-cola-components/egon-cola-component-dynamic-config-center/README.zh-CN.md \
  egon-cola-components/egon-cola-components-bom/README.md \
  egon-cola-components/egon-cola-components-bom/README.zh-CN.md
git commit -m "docs(ddc): document starter-only integration"
```

- [ ] **Step 9: Perform post-commit verification**

```bash
git status --short --branch
git log -7 --oneline
./mvnw -B -ntp \
  -pl egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-starter,egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-admin,egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-test \
  -am test
```

Expected: clean implementation worktree, five task commits after the design/plan commits, and final DDC `BUILD SUCCESS`.
