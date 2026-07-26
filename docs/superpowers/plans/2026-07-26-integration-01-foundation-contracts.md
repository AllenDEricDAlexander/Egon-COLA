# Integration 01 Public Contracts and Startup Baseline Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 统一服务状态、Redisson Bean、DDC 可执行产物、版本和 Redis/advertised endpoint 配置，使真实进程具备可启动的共同基线。

**Architecture:** 状态归一化放入 DDC management-client 公共模型，Starter、Gateway、RPC 只消费 typed accessor。所有 Redisson Client 按稳定 Bean 名称装配，部署配置显式传递真实基础设施地址。

**Tech Stack:** Java 21、Spring Boot AutoConfiguration、Jackson、Redisson、Docker Compose、JUnit 5、AssertJ。

## Global Constraints

- 保留 `DdcServiceInstance.status` 和 `DdcManagementServiceInstance.status` 的 String wire component。
- legacy `REGISTERED`/`UP` 兼容为 ONLINE；未知或空值不能视为在线。
- 不修改数据库或 Flyway。
- DDC thin JAR 继续是 Maven 主产物，Docker 只使用 `exec` classifier。
- 每个任务单独提交。

---

### Task 1: 统一服务实例状态合同

**Files:**
- Create: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-management-client/src/main/java/top/egon/cola/component/ddc/management/model/DdcInstanceStatus.java`
- Create: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-management-client/src/test/java/top/egon/cola/component/ddc/management/model/DdcInstanceStatusTest.java`
- Modify: `.../management/model/DdcManagementServiceInstance.java`
- Modify: `.../management/model/DdcManagementConfigClientInstance.java`
- Modify: `.../starter/src/main/java/top/egon/cola/component/ddc/model/registry/DdcServiceInstance.java`
- Modify: `.../gateway-engine/src/main/java/top/egon/cola/component/gateway/engine/discovery/DdcProviderServiceRegistryAdapter.java`
- Modify: `.../rpc-starter/src/main/java/top/egon/cola/component/rpc/consumer/RpcConsumerGatewayManager.java`
- Test: `.../gateway-engine/src/test/java/top/egon/cola/component/gateway/engine/discovery/DdcProviderServiceRegistryAdapterTest.java`
- Test: `.../rpc-starter/src/test/java/top/egon/cola/component/rpc/consumer/RpcConsumerGatewayManagerTest.java`

**Interfaces:**
- Produces: `DdcInstanceStatus.fromWire(String)` and `isOnline(Instant, Instant)`.
- Produces: `DdcServiceInstance.normalizedStatus()`,
  `DdcManagementServiceInstance.normalizedStatus()` and
  `DdcManagementConfigClientInstance.normalizedStatus()`.
- Consumes: existing String JSON values without record component changes.

- [ ] **Step 1: Write failing normalization tests**

```java
@ParameterizedTest
@CsvSource({
        "ONLINE,ONLINE", "REGISTERED,ONLINE", "UP,ONLINE",
        "OFFLINE,OFFLINE", "EXPIRED,OFFLINE", "DOWN,OFFLINE",
        "unexpected,UNKNOWN"
})
void normalizesWireValues(String wire, DdcInstanceStatus expected) {
    assertThat(DdcInstanceStatus.fromWire(wire)).isEqualTo(expected);
}

@Test
void nullStatusIsUnknownAndExpiredLeaseIsNotOnline() {
    assertThat(DdcInstanceStatus.fromWire(null)).isEqualTo(UNKNOWN);
    assertThat(ONLINE.isAvailable(
            Instant.parse("2026-07-26T00:00:00Z"),
            Instant.parse("2026-07-25T23:59:59Z")
    )).isFalse();
}
```

- [ ] **Step 2: Run tests and confirm current string comparisons fail**

Run:

```bash
./mvnw -B -ntp \
  -pl egon-cola-components/egon-cola-component-dynamic-config-center/\
egon-cola-component-dynamic-config-center-management-client,\
egon-cola-components/egon-cola-component-gateway/\
egon-cola-component-gateway-engine,\
egon-cola-components/egon-cola-component-rpc/\
egon-cola-component-rpc-starter -am test
```

Expected: 新 enum 尚不存在或 ONLINE Provider/RPC Slot 用例失败。

- [ ] **Step 3: Implement the public status type and accessors**

```java
public enum DdcInstanceStatus {
    ONLINE, OFFLINE, UNKNOWN;

    public static DdcInstanceStatus fromWire(String value) {
        if (value == null || value.isBlank()) {
            return UNKNOWN;
        }
        return switch (value.trim().toUpperCase(Locale.ROOT)) {
            case "ONLINE", "REGISTERED", "UP" -> ONLINE;
            case "OFFLINE", "EXPIRED", "DOWN" -> OFFLINE;
            default -> UNKNOWN;
        };
    }

    public boolean isAvailable(Instant now, Instant leaseExpireAt) {
        return this == ONLINE && leaseExpireAt != null
                && leaseExpireAt.isAfter(now);
    }
}
```

Each String-based record adds:

```java
public DdcInstanceStatus normalizedStatus() {
    return DdcInstanceStatus.fromWire(status);
}
```

Gateway and RPC must call `normalizedStatus().isAvailable(now, leaseExpireAt)` rather than compare strings.

- [ ] **Step 4: Run focused tests**

Run the command from Step 2. Expected: PASS with ONLINE, legacy and expired lease cases.

- [ ] **Step 5: Commit**

```bash
git add egon-cola-components/egon-cola-component-dynamic-config-center \
        egon-cola-components/egon-cola-component-gateway/egon-cola-component-gateway-engine \
        egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter
git commit -m "fix: unify ddc service availability status"
```

### Task 2: 按名称隔离 Redisson Client

**Files:**
- Modify: `.../dynamic-config-center-starter/src/main/java/top/egon/cola/component/ddc/config/DdcAutoConfig.java`
- Modify: `.../dynamic-config-center-starter/src/main/java/top/egon/cola/component/ddc/config/DdcRegistryAutoConfig.java`
- Modify: `.../gateway-engine/src/main/java/top/egon/cola/component/gateway/engine/GatewayEngineConfiguration.java`
- Test: `.../dynamic-config-center-starter/src/test/java/top/egon/cola/component/ddc/config/DdcAutoConfigTest.java`
- Test: `.../dynamic-config-center-starter/src/test/java/top/egon/cola/component/ddc/config/DdcRegistryAutoConfigTest.java`
- Test: `.../gateway-engine/src/test/java/top/egon/cola/component/gateway/engine/GatewayEngineConfigurationTest.java`

**Interfaces:**
- Produces exact beans `ddcRedissonClient`, `ddcRegistryRedissonClient`, `gatewayRateLimitRedissonClient`.
- Consumes `DdcRedisTopology.create(...)` for Registry topology.

- [ ] **Step 1: Add failing context tests with an unrelated RedissonClient**

```java
contextRunner.withBean("applicationRedissonClient", RedissonClient.class, () -> unrelated)
        .withPropertyValues(
                "egon.cola.component.ddc.registry.enabled=true",
                "egon.cola.component.ddc.redis.enabled=true"
        )
        .run(context -> {
            assertThat(context).hasBean("ddcRedissonClient");
            assertThat(context).hasBean("ddcRegistryRedissonClient");
            assertThat(context.getBean("applicationRedissonClient"))
                    .isSameAs(unrelated);
        });
```

Add an override case where a user-supplied bean with the exact DDC name is retained.

- [ ] **Step 2: Run the three context tests**

```bash
./mvnw -B -ntp \
  -pl egon-cola-components/egon-cola-component-dynamic-config-center/\
egon-cola-component-dynamic-config-center-starter,\
egon-cola-components/egon-cola-component-gateway/\
egon-cola-component-gateway-engine -am test \
  -Dtest=DdcAutoConfigTest,DdcRegistryAutoConfigTest,GatewayEngineConfigurationTest
```

Expected: DDC named bean assertions fail with the current type-based conditions.

- [ ] **Step 3: Change conditions and qualifiers**

Use exact conditions:

```java
@Bean(name = "ddcRegistryRedissonClient", destroyMethod = "shutdown")
@ConditionalOnMissingBean(name = "ddcRegistryRedissonClient")
RedissonClient ddcRegistryRedissonClient(DdcProperties properties) {
    DdcProperties.Redis redis = properties.getRedis();
    return Redisson.create(DdcRedisTopology.create(
            redis.getMode(), redis.getNodes(), redis.getMasterName(),
            redis.getHost(), redis.getPort(), redis.getPassword(),
            redis.getDatabase()
    ));
}
```

Every consumer uses `@Qualifier("ddcRegistryRedissonClient")`; every condition names the bean. Apply the
same rule to `ddcRedissonClient` and `gatewayRateLimitRedissonClient`.

- [ ] **Step 4: Run focused tests and starter/gateway module tests**

Run Step 2, then:

```bash
./mvnw -B -ntp \
  -pl egon-cola-components/egon-cola-component-dynamic-config-center/\
egon-cola-component-dynamic-config-center-starter,\
egon-cola-components/egon-cola-component-gateway/\
egon-cola-component-gateway-engine -am test
```

Expected: PASS and no ambiguous/missing bean failure.

- [ ] **Step 5: Commit**

```bash
git add egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-starter \
        egon-cola-components/egon-cola-component-gateway/egon-cola-component-gateway-engine
git commit -m "fix: isolate ddc and gateway redis clients"
```

### Task 3: 修复 DDC 可执行镜像和版本来源

**Files:**
- Modify: `.../dynamic-config-center-admin/Dockerfile`
- Modify: `.../dynamic-config-center-admin/src/main/java/top/egon/cola/component/ddc/admin/config/DdcAdminProperties.java`
- Modify: `.../dynamic-config-center-admin/src/main/java/top/egon/cola/component/ddc/admin/controller/DdcManifestController.java`
- Modify: `.../dynamic-config-center-admin/src/main/resources/application.yml`
- Modify: `.../dynamic-config-center/docs/manifest.md`
- Test: `.../dynamic-config-center-admin/src/test/java/top/egon/cola/component/ddc/admin/controller/DdcManifestControllerTest.java`
- Test: `.../dynamic-config-center-admin/src/test/java/top/egon/cola/component/ddc/admin/DdcExecutableJarContractTest.java`

**Interfaces:**
- Produces executable artifact `egon-cola-component-dynamic-config-center-admin-exec.jar` for Docker.
- Produces manifest version from `${project.version}` filtered metadata, not source literals.

- [ ] **Step 1: Add failing artifact/version contract tests**

```java
@Test
void manifestUsesFilteredComponentVersion() {
    assertThat(controller.manifest().getData().getVersion()).isEqualTo("5.2.3");
}
```

The artifact test opens `target/*-exec.jar` and asserts `Main-Class` exists. Do not assert Dockerfile source
text: the container contract is verified by actually building the image, which fails if the runtime artifact
path is wrong.

- [ ] **Step 2: Run admin tests and package**

```bash
./mvnw -B -ntp \
  -pl egon-cola-components/egon-cola-component-dynamic-config-center/\
egon-cola-component-dynamic-config-center-admin -am clean package

docker build \
  -f egon-cola-components/egon-cola-component-dynamic-config-center/\
egon-cola-component-dynamic-config-center-admin/Dockerfile \
  -t egon-cola-ddc-admin:contract-test .
```

Expected: the new manifest/version contract or the real image build fails before the Dockerfile/version fix.

- [ ] **Step 3: Use the exec artifact and filtered version metadata**

Dockerfile runtime copy must be:

```dockerfile
COPY --from=builder /workspace/egon-cola-components/\
egon-cola-component-dynamic-config-center/\
egon-cola-component-dynamic-config-center-admin/target/\
egon-cola-component-dynamic-config-center-admin-exec.jar app.jar
```

Use the already filtered `META-INF/egon-cola-ddc.properties` or a `BuildProperties`/package implementation
version fallback. Remove all `5.2.1` source and documentation defaults.

- [ ] **Step 4: Re-run package and inspect both manifests**

```bash
./mvnw -B -ntp \
  -pl egon-cola-components/egon-cola-component-dynamic-config-center/\
egon-cola-component-dynamic-config-center-admin -am clean package
unzip -p egon-cola-components/egon-cola-component-dynamic-config-center/\
egon-cola-component-dynamic-config-center-admin/target/\
egon-cola-component-dynamic-config-center-admin-exec.jar META-INF/MANIFEST.MF

docker build \
  -f egon-cola-components/egon-cola-component-dynamic-config-center/\
egon-cola-component-dynamic-config-center-admin/Dockerfile \
  -t egon-cola-ddc-admin:contract-test .
docker image inspect egon-cola-ddc-admin:contract-test --format '{{json .Config.Entrypoint}} {{json .Config.Cmd}}'
```

Expected: package PASS; exec JAR contains Boot launcher Main-Class.

- [ ] **Step 5: Commit**

```bash
git add egon-cola-components/egon-cola-component-dynamic-config-center
git commit -m "fix: package executable ddc admin image"
```

### Task 4: 对齐 Compose/live 的基础设施和服务身份

**Files:**
- Modify: `.../gateway/deployment/compose.yml`
- Modify: `.../gateway/deployment/.env.example`
- Modify: `.../gateway-test-suite/src/test/java/top/egon/cola/component/gateway/test/live/GatewayLiveTopologyIT.java`
- Modify: `.../gateway-test-http-provider/src/main/resources/application.yml`
- Modify: `.../rpc-starter/src/main/java/top/egon/cola/component/rpc/config/EgonRpcProperties.java`
- Test: `.../gateway-test-suite/src/test/java/top/egon/cola/component/gateway/test/deployment/GatewayComposeConfigurationTest.java`

**Interfaces:**
- Produces default RPC Gateway service name `egon-gateway-rpc`.
- Produces explicit DDC Redis host/port for every Engine/Provider/Consumer process.
- Produces matching Reporting artifact version and Provider service version.

- [ ] **Step 1: Add failing configuration assertions**

```java
assertThat(engineEnvironment)
        .containsEntry("EGON_COLA_COMPONENT_DDC_REDIS_HOST", "ddc-redis")
        .containsEntry("EGON_COLA_COMPONENT_GATEWAY_ENGINE_RPC_ADVERTISED_HOST",
                "gateway-engine");
assertThat(new EgonRpcProperties().getConsumer().getGatewayServiceName())
        .isEqualTo("egon-gateway-rpc");
```

Add a live fixture assertion that every child process receives the Testcontainers DDC Redis mapped port.

- [ ] **Step 2: Run deployment/default tests**

```bash
./mvnw -B -ntp \
  -pl egon-cola-components/egon-cola-component-gateway/\
egon-cola-component-gateway-test/egon-cola-component-gateway-test-suite,\
egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter \
  -am test -Dtest=GatewayComposeConfigurationTest,EgonRpcPropertiesTest
```

Expected: DDC Redis, advertised host, or default service name assertions fail.

- [ ] **Step 3: Align all properties**

Set explicit values in Compose and harness:

```yaml
EGON_COLA_COMPONENT_DDC_REDIS_HOST: ddc-redis
EGON_COLA_COMPONENT_DDC_REDIS_PORT: 6379
EGON_COLA_COMPONENT_GATEWAY_ENGINE_RPC_ADVERTISED_HOST: gateway-engine
```

Use the same artifact/service version value in Reporting and `HttpProviderRuntimeProperties`; set the RPC
Consumer default to `egon-gateway-rpc`.

- [ ] **Step 4: Verify static Compose and focused tests**

```bash
docker compose --env-file \
  egon-cola-components/egon-cola-component-gateway/deployment/.env.example \
  -f egon-cola-components/egon-cola-component-gateway/deployment/compose.yml \
  config --quiet

./mvnw -B -ntp \
  -pl egon-cola-components/egon-cola-component-gateway/\
egon-cola-component-gateway-test/egon-cola-component-gateway-test-suite,\
egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter \
  -am test
```

Expected: both commands PASS. This does not yet claim real processes started.

- [ ] **Step 5: Commit**

```bash
git add egon-cola-components/egon-cola-component-gateway \
        egon-cola-components/egon-cola-component-rpc
git commit -m "fix: align gateway ddc runtime coordinates"
```
