# DDC Namespace Visibility and Gateway Integration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking. This plan must be executed inline in the current workspace because the user explicitly prohibited subagents.

**Goal:** 将 DDC 改成 `biz -> namespace -> env -> app` 管控可见性模型，同时让物理配置和服务注册只保留 `biz + env + app` 身份，统一副本实例 ID，并完成 Gateway Admin、Gateway Engine 与两个订单服务副本的本机联调。

**Architecture:** namespace 是 biz 下的管理实体，`DdcNamespaceEnvAppBindingEntity` 作为 Association Object 保存环境级应用可见性；配置、服务、实例租约不复制。服务注册采用 V3 canonical/serviceId、V3 Redis scope/global catalog，配置采用 V3 Redis scope；`DdcInstanceIdProvider` Strategy 统一实例身份。DDC Admin 提供可选筛选和 binding 级联，两个 Web UI 只消费这些稳定接口。

**Tech Stack:** Java 21, Spring Boot 3.5, Spring Data JPA, Flyway, PostgreSQL, SQLite, Redis/Redisson, Maven, JUnit 5, React 19, TypeScript, Ant Design 6, Vite, Vitest.

## Global Constraints

- 在当前 `main` checkout 内联执行；不创建子代理，不创建 worktree。
- 保留所有无关本地修改和本地数据；暂存与提交只包含当前任务列出的路径。
- 只新增 DDC `V7` 迁移版本；PostgreSQL 与 SQLite 各一份同版本方言文件，绝不修改 V1-V6。
- 应用唯一键为 `(biz_code, app_code)`；namespace 唯一键为 `(biz_code, namespace_code)`；binding 唯一键为 `(namespace_id, env_code, app_id)`。
- 物理配置键为 `(biz_code, env, app_code, config_key)`；namespace 只作为管理访问上下文和历史快照。
- 物理服务键为 `bizCode, env, appCode, serviceKind, protocol, serviceName, group, version`；namespace 不进入注册、心跳、发现或路由身份。
- `serviceId = SHA-256(serviceKey.canonicalValue())`；canonical 首行固定为 `ddc-service-key-v3`，字段禁止 CR/LF。
- `instanceId` 优先级为显式 `egon.cola.component.ddc.instance.id`、自定义 `DdcInstanceIdProvider`、完整 UUIDv7；不使用 MAC/IP/PID Hash。
- `leaseId` 继续由 DDC Admin 为每次成功注册生成 UUIDv7，并保持 fencing 语义。
- Redis V2 数据只按 TTL 自然过期；新实现只读写 V3 key，不使用常规 Redis `SCAN`。
- 本机联调使用已有 PostgreSQL、Redis 和本机多 JVM；不使用 Docker 或 Testcontainers 代替本机拓扑证据。
- Access Token、Refresh Token、Redis 密码、数据库密码和签名密钥不得打印到终端、日志、文档或最终回复。
- 每个行为修改先写失败测试，再做最小实现；每个任务验证通过后提交一次。
- 设计模式只使用已确认的 Association Object、Strategy 和既有 Adapter；不增加额外 Factory/Facade/Handler 链。
- 最终验收后保持 DDC Admin/Web、Gateway Admin/Web/Engine 和两个订单 Provider 运行，供用户测试。

---

### Task 1: 建立 V3 服务身份和统一实例 ID Strategy

**Files:**
- Create: `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-starter/src/main/java/top/egon/cola/component/ddc/service/DdcInstanceIdProvider.java`
- Modify: `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-starter/src/main/java/top/egon/cola/component/ddc/model/registry/DdcServiceKey.java`
- Modify: `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-starter/src/main/java/top/egon/cola/component/ddc/model/registry/DdcServiceQuery.java`
- Modify: `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-starter/src/main/java/top/egon/cola/component/ddc/registry/DdcServiceKeyFactory.java`
- Modify: `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-starter/src/main/java/top/egon/cola/component/ddc/config/DdcProperties.java`
- Modify: `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-starter/src/main/java/top/egon/cola/component/ddc/config/DdcAutoConfig.java`
- Modify: `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-starter/src/main/java/top/egon/cola/component/ddc/service/DdcInstanceIdentityFactory.java`
- Modify: `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-starter/src/main/java/top/egon/cola/component/ddc/common/DdcKeys.java`
- Test: `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-starter/src/test/java/top/egon/cola/component/ddc/model/registry/DdcServiceRegistrationTest.java`
- Test: `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-starter/src/test/java/top/egon/cola/component/ddc/registry/DdcServiceKeyFactoryTest.java`
- Create test: `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-starter/src/test/java/top/egon/cola/component/ddc/service/DdcInstanceIdentityFactoryTest.java`
- Modify test: `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-starter/src/test/java/top/egon/cola/component/ddc/common/DdcKeysTest.java`
- Modify test: `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-starter/src/test/java/top/egon/cola/component/ddc/config/DdcAutoConfigTest.java`

**Interfaces:**
- Produces `DdcServiceKey(String bizCode, String env, String appCode, DdcServiceKind serviceKind, String serviceName, String group, String version, String protocol)`.
- Produces `String DdcServiceKey.serviceId()` using the exact canonical SHA-256.
- Produces `DdcServiceQuery` with nullable physical filters and `boolean hasExactCatalogScope()`.
- Produces `@FunctionalInterface DdcInstanceIdProvider { String getInstanceId(); }`.
- Produces `DdcInstanceIdentityFactory(DdcProperties properties, DdcInstanceIdProvider provider)`; `provider` may be null and is used only when no explicit ID is configured.
- Produces `DdcProperties.Instance.id`; `DdcProperties.namespace` remains deprecated for one release and is not used by V3 identity.
- Produces V3 key methods `v3Config`, `v3Version`, `v3Topic`, `v3ConfigLeaseInstance`, `v3RegistryInstance`, `v3RegistryService`, `v3RegistryRevision`, `v3RegistryCatalog`, `v3RegistryCatalogRevision`, `v3RegistryTopic`, `v3GlobalRegistryCatalog`, and `v3GlobalRegistryCatalogRevision`.

- [ ] **Step 1: 写 V3 canonical/serviceId 和实例 ID 优先级失败测试**

```java
@Test
void derivesOneServiceIdForReplicasWithoutNamespace() {
    DdcServiceKey key = new DdcServiceKey(
            "retail", "local", "order",
            DdcServiceKind.HTTP_PROVIDER,
            "order-service", "default", "1.0.0", "http");

    assertThat(key.canonicalValue()).isEqualTo(String.join("\n",
            "ddc-service-key-v3", "retail", "local", "order",
            "HTTP_PROVIDER", "http", "order-service", "default", "1.0.0"));
    assertThat(key.serviceId()).hasSize(64);
    assertThat(DdcServiceKey.parse(key.canonicalValue())).isEqualTo(key);
}

@Test
void resolvesConfiguredThenCustomThenUuidV7InstanceId() {
    properties.getInstance().setId("pod-uid-1");
    assertThat(new DdcInstanceIdentityFactory(
            properties, () -> "custom-1").create().instanceId())
            .isEqualTo("pod-uid-1");
    properties.getInstance().setId(null);
    assertThat(new DdcInstanceIdentityFactory(
            properties, () -> "custom-1").create().instanceId())
            .isEqualTo("custom-1");
    assertThat(UUID.fromString(new DdcInstanceIdentityFactory(
            properties, null).create().instanceId()).version())
            .isEqualTo(7);
}
```

- [ ] **Step 2: 运行 RED 测试**

```bash
./mvnw -B -ntp -f egon-cola-platforms/egon-cola-platform-dynamic-config-center/pom.xml \
  -pl egon-cola-platform-dynamic-config-center-starter -am \
  -Dtest=DdcServiceRegistrationTest,DdcServiceKeyFactoryTest,DdcInstanceIdentityFactoryTest,DdcKeysTest \
  -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: FAIL because V2 key contains namespace, `serviceId()` and `DdcInstanceIdProvider` do not exist, and the default ID is host/PID plus truncated UUID.

- [ ] **Step 3: 实现 V3 canonical、兼容构造器和 Strategy**

```java
public record DdcServiceKey(
        String bizCode,
        String env,
        String appCode,
        DdcServiceKind serviceKind,
        String serviceName,
        String group,
        String version,
        String protocol
) implements Comparable<DdcServiceKey> {

    public String canonicalValue() {
        return String.join("\n", "ddc-service-key-v3", bizCode, env, appCode,
                serviceKind.name(), protocol, serviceName, group, version);
    }

    public String serviceId() {
        return Digests.sha256Hex(canonicalValue());
    }

    @Deprecated(forRemoval = true)
    public DdcServiceKey(String bizCode, String appCode, String env, String ignoredNamespace,
                         DdcServiceKind serviceKind, String serviceName, String group,
                         String version, String protocol) {
        this(bizCode, env, appCode, serviceKind, serviceName, group, version, protocol);
    }

    @Deprecated(forRemoval = true)
    public String namespace() {
        return "";
    }
}

@FunctionalInterface
public interface DdcInstanceIdProvider {
    String getInstanceId();
}
```

`DdcInstanceIdentityFactory` 必须先读取 `properties.getInstance().getId()`，再调用自定义 Provider，最后调用 `UuidV7.string()`；每个结果都 trim 并拒绝空白。保留旧九参数 service key 构造器和旧 factory overload 一版，只忽略 namespace，不把它写回 canonical。

- [ ] **Step 4: 实现并测试 V3 Redis key 口径**

```java
public static String v3GlobalRegistryCatalog() {
    return PREFIX + ":v3:{registry-catalog}:services";
}

public static String v3GlobalRegistryCatalogRevision() {
    return PREFIX + ":v3:{registry-catalog}:revision";
}

private static String v3ConfigTag(String bizCode, String env, String appCode) {
    return tag(String.join("\n", bizCode, env, appCode));
}

private static String v3RegistryTag(DdcServiceKey key) {
    return tag(String.join("\n", key.bizCode(), key.env(), key.appCode(),
            key.serviceKind().name()));
}
```

断言 V3 config key 对 namespace 变化保持一致、不同 biz/env/app 不同；V3 registry service key 对相同 serviceKey 的两个 instanceId 保持一致。

- [ ] **Step 5: 运行 GREEN Starter 测试并提交**

```bash
./mvnw -B -ntp -f egon-cola-platforms/egon-cola-platform-dynamic-config-center/pom.xml \
  -pl egon-cola-platform-dynamic-config-center-starter -am clean test
git add egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-starter
git commit -m "feat(ddc): introduce v3 service and instance identity"
```

### Task 2: 将服务注册、目录和所有运行时迁移到 V3

**Files:**
- Modify: `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-starter/src/main/java/top/egon/cola/component/ddc/model/dto/DdcServiceLeaseRequest.java`
- Modify: `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-starter/src/main/java/top/egon/cola/component/ddc/registry/DdcOpenApiServiceRegistryClient.java`
- Modify: `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-starter/src/main/java/top/egon/cola/component/ddc/registry/DdcRegistrySubscriptionManager.java`
- Modify: `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin/src/main/java/top/egon/cola/component/ddc/admin/repository/DdcServiceRegistryRedisRepository.java`
- Modify: `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin/src/main/java/top/egon/cola/component/ddc/admin/service/DdcServiceRegistryService.java`
- Modify: `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin/src/main/java/top/egon/cola/component/ddc/admin/controller/DdcRegistryOpenApiController.java`
- Modify: `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin/src/main/resources/redis/ddc_service_register.lua`
- Modify: `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin/src/main/resources/redis/ddc_service_heartbeat.lua`
- Modify: `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin/src/main/resources/redis/ddc_service_deregister.lua`
- Modify: `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin/src/main/resources/redis/ddc_service_expire.lua`
- Modify: `egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/main/java/top/egon/cola/component/rpc/context/RpcProcessIdentity.java`
- Modify: `egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/main/java/top/egon/cola/component/rpc/context/RpcProcessIdentityFactory.java`
- Modify: `egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/main/java/top/egon/cola/component/rpc/config/EgonRpcAutoConfig.java`
- Modify: `egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/main/java/top/egon/cola/component/rpc/provider/RpcProviderLeaseManager.java`
- Modify: `egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/main/java/top/egon/cola/component/rpc/consumer/RpcConsumerGatewayManager.java`
- Modify: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-provider-runtime/src/main/java/top/egon/cola/component/gateway/provider/GatewayHttpProviderProperties.java`
- Modify: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-provider-runtime/src/main/java/top/egon/cola/component/gateway/provider/HttpProviderRuntimeProperties.java`
- Modify: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-provider-runtime/src/main/java/top/egon/cola/component/gateway/provider/GatewayHttpProviderAutoConfiguration.java`
- Modify: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-provider-runtime/src/main/java/top/egon/cola/component/gateway/provider/HttpProviderLeaseRuntime.java`
- Modify: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-engine/src/main/java/top/egon/cola/component/gateway/engine/rpc/RpcGatewaySlotProperties.java`
- Modify: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-engine/src/main/java/top/egon/cola/component/gateway/engine/rpc/RpcGatewaySlotRuntime.java`
- Modify: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-engine/src/main/java/top/egon/cola/component/gateway/engine/GatewayEngineConfiguration.java`
- Test: `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin/src/test/java/top/egon/cola/component/ddc/admin/repository/DdcServiceRegistryRedisRepositoryTest.java`
- Test: `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin/src/test/java/top/egon/cola/component/ddc/admin/service/DdcServiceRegistryServiceTest.java`
- Test: `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin/src/test/java/top/egon/cola/component/ddc/admin/controller/DdcRegistryOpenApiControllerTest.java`
- Test: `egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/test/java/top/egon/cola/component/rpc/consumer/RpcConsumerGatewayManagerTest.java`
- Test: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-provider-runtime/src/test/java/top/egon/cola/component/gateway/provider/HttpProviderLeaseRuntimeTest.java`
- Test: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-engine/src/test/java/top/egon/cola/component/gateway/engine/rpc/RpcGatewaySlotRuntimeTest.java`

**Interfaces:**
- `DdcServiceLeaseRequest` contains only `serviceKey`, `instanceId`, and `leaseId`.
- `DdcOpenApiServiceRegistryClient` tracks active registrations by `leaseId`, allowing one runtime `instanceId` to register multiple service keys.
- Exact catalog scope is `bizCode + env + appCode + serviceKind + protocol`; partial query reads the global V3 catalog and applies `DdcServiceQuery.matches`.
- Register and successful heartbeat add the canonical key to the global catalog; deregister/expiry removes it only after the service has no live instances.
- Subscription requires `hasExactCatalogScope() == true`; one-shot management queries may be partial.
- RPC Provider, HTTP Provider and Gateway Engine use the shared DDC instance ID and no longer pass namespace to `DdcServiceKeyFactory`.

- [ ] **Step 1: 写两个副本、全局目录和共享运行时 ID 失败测试**

```java
@Test
void returnsTwoReplicasFromOnePhysicalServiceAndGlobalCatalog() {
    repository.register(instance("order-a", SERVICE_KEY, 18084));
    repository.register(instance("order-b", SERVICE_KEY, 18085));

    assertThat(repository.getServiceKeys(new DdcServiceQuery(
            null, null, null, null, null, null, null, null), NOW).serviceKeys())
            .containsExactly(SERVICE_KEY);
    assertThat(repository.getInstances(SERVICE_KEY, NOW).instances())
            .extracting(DdcServiceInstance::instanceId)
            .containsExactlyInAnyOrder("order-a", "order-b");
}

@Test
void registryClientKeepsMultipleLeasesForOneRuntimeInstance() {
    DdcLeaseSession first = client.register(registration("runtime-1", SERVICE_A));
    DdcLeaseSession second = client.register(registration("runtime-1", SERVICE_B));
    assertThat(client.heartbeat("runtime-1", first.leaseId()).renewed()).isTrue();
    assertThat(client.heartbeat("runtime-1", second.leaseId()).renewed()).isTrue();
}
```

- [ ] **Step 2: 运行 RED 注册测试**

```bash
./mvnw -B -ntp -f egon-cola-platforms/egon-cola-platform-dynamic-config-center/pom.xml \
  -pl egon-cola-platform-dynamic-config-center-starter,egon-cola-platform-dynamic-config-center-admin -am \
  -Dtest=DdcServiceRegistryRedisRepositoryTest,DdcServiceRegistryServiceTest,DdcRegistryOpenApiControllerTest \
  -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: FAIL because current repository reads only a complete V2 scope catalog and the client overwrites registrations sharing an instance ID.

- [ ] **Step 3: 实现 V3 scope/global catalog 与 leaseId 会话索引**

```java
public DdcServiceCatalogSnapshot getServiceKeys(DdcServiceQuery query, Instant now) {
    String catalogKey = query.hasExactCatalogScope()
            ? DdcKeys.v3RegistryCatalog(query.bizCode(), query.env(), query.appCode(),
                    query.serviceKind(), query.protocol())
            : DdcKeys.v3GlobalRegistryCatalog();
    Set<String> members = redissonClient.<String>getSet(catalogKey, StringCodec.INSTANCE)
            .readAll();
    List<DdcServiceKey> keys = members.stream()
            .map(DdcServiceKey::parse)
            .filter(query::matches)
            .filter(key -> !getInstances(key, now).instances().isEmpty())
            .sorted()
            .toList();
    return new DdcServiceCatalogSnapshot(query, revision(query), keys, now);
}
```

全局 catalog 写入必须在现有 scope Lua 成功后执行独立 Redis 命令，避免 Redis Cluster `CROSSSLOT`。heartbeat 成功也执行幂等 `add`；最后实例消失时执行 `remove` 和 revision increment。Lua 继续只操作同一 V3 scope Hash Tag 内的 keys。

- [ ] **Step 4: 迁移 RPC、HTTP Provider 和 Gateway Engine 的调用点**

```java
serviceKeyFactory.fromScope(
        DdcServiceKind.HTTP_PROVIDER,
        properties.serviceName(), properties.group(),
        properties.version(), properties.protocol());

new RpcGatewaySlotProperties(
        rpc.isEnabled(), ddcIdentity.instanceId(), rpc.getAdvertisedHost(),
        rpc.getServiceName(), rpc.getGroup(), rpc.getVersion(),
        properties.getGatewayGroupCode(), "5.3.2", "5.3.2",
        rpc.getTls().isEnabled(), rpc.getLeaseSeconds(),
        rpc.getHeartbeatIntervalSeconds());
```

`GatewayHttpProviderAutoConfiguration` 在 provider `instance-id` 为空时使用 `DdcInstanceIdentity.instanceId()`；RPC `RpcProcessIdentityFactory` 使用同一 `DdcInstanceIdentity`，`RpcProviderLeaseManager` 不再追加 `registrySuffix()`。

- [ ] **Step 5: 运行 DDC/RPC/Gateway GREEN 测试并提交**

```bash
./mvnw -B -ntp -f egon-cola-platforms/egon-cola-platform-dynamic-config-center/pom.xml clean test
./mvnw -B -ntp -f egon-cola-components/egon-cola-component-rpc/pom.xml clean test
./mvnw -B -ntp -f egon-cola-platforms/egon-cola-platform-gateway/pom.xml \
  -pl egon-cola-platform-gateway-provider-runtime,egon-cola-platform-gateway-engine -am clean test
git add egon-cola-platforms/egon-cola-platform-dynamic-config-center \
  egon-cola-components/egon-cola-component-rpc \
  egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-provider-runtime \
  egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-engine
git commit -m "feat(ddc): migrate service registry runtimes to v3"
```

### Task 3: 新增 V7 namespace-env-app Association Object

**Files:**
- Create: `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin/src/main/resources/db/postgresql/V7__add_namespace_env_app_visibility.sql`
- Create: `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin/src/main/resources/db/sqlite/V7__add_namespace_env_app_visibility.sql`
- Create: `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin/src/test/java/top/egon/cola/component/ddc/admin/repository/DdcV7MigrationTest.java`
- Modify: `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin/src/main/java/top/egon/cola/component/ddc/admin/model/entity/DdcAppEntity.java`
- Modify: `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin/src/main/java/top/egon/cola/component/ddc/admin/model/entity/DdcNamespaceEntity.java`
- Create: `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin/src/main/java/top/egon/cola/component/ddc/admin/model/entity/DdcNamespaceEnvAppBindingEntity.java`
- Modify: `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin/src/main/java/top/egon/cola/component/ddc/admin/repository/DdcAppRepository.java`
- Modify: `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin/src/main/java/top/egon/cola/component/ddc/admin/repository/DdcNamespaceRepository.java`
- Create: `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin/src/main/java/top/egon/cola/component/ddc/admin/repository/DdcNamespaceEnvAppBindingRepository.java`
- Create: `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin/src/main/java/top/egon/cola/component/ddc/admin/model/dto/DdcNamespaceEnvAppBindingRequest.java`
- Create: `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin/src/main/java/top/egon/cola/component/ddc/admin/model/vo/DdcNamespaceEnvAppBindingVO.java`
- Modify: `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin/src/main/java/top/egon/cola/component/ddc/admin/service/DdcAppService.java`
- Modify: `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin/src/main/java/top/egon/cola/component/ddc/admin/service/DdcNamespaceService.java`
- Create: `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin/src/main/java/top/egon/cola/component/ddc/admin/service/DdcNamespaceEnvAppBindingService.java`
- Modify: `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin/src/main/java/top/egon/cola/component/ddc/admin/service/DdcEnvService.java`
- Modify: `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin/src/main/java/top/egon/cola/component/ddc/admin/controller/DdcAppController.java`
- Modify: `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin/src/main/java/top/egon/cola/component/ddc/admin/controller/DdcNamespaceController.java`
- Create: `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin/src/main/java/top/egon/cola/component/ddc/admin/controller/DdcNamespaceEnvAppBindingController.java`
- Modify: `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin/src/main/java/top/egon/cola/component/ddc/admin/controller/DdcEnvController.java`
- Modify: `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-starter/src/main/java/top/egon/cola/component/ddc/common/DdcErrorStatus.java`
- Test: `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin/src/test/java/top/egon/cola/component/ddc/admin/controller/DdcAppControllerTest.java`
- Test: `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin/src/test/java/top/egon/cola/component/ddc/admin/controller/DdcNamespaceControllerTest.java`
- Create test: `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin/src/test/java/top/egon/cola/component/ddc/admin/controller/DdcNamespaceEnvAppBindingControllerTest.java`

**Interfaces:**
- `DdcNamespaceEntity` contains `bizCode`, `namespaceCode`, `namespace`; it no longer contains `appCode`.
- `DdcAppEntity.appCode` is not globally unique; repository lookups use `bizCode + appCode` or immutable `id`.
- Binding request is `(bizCode, namespaceCode, env, appCode, enabled)`; response adds binding ID, namespace/app IDs and app name.
- `GET /api/v1/ddc/namespaces?bizCode=&keyword=` lists namespaces by biz.
- `GET /api/v1/ddc/envs?bizCode=&namespaceCode=&keyword=` returns bound envs when biz+namespace are present and global envs otherwise.
- `GET /api/v1/ddc/apps?bizCode=&namespaceCode=&env=&keyword=` returns bound apps when namespace+env are present and biz apps otherwise.
- Binding CRUD path is `/api/v1/ddc/namespace-env-app-bindings`; app/namespace mutation paths use immutable entity ID.

- [ ] **Step 1: 写 V7 迁移、重复物理配置拒绝和 binding CRUD 失败测试**

```java
@Test
void sqliteV7BuildsBizNamespaceAndEnvironmentBindings() throws Exception {
    migrateThroughV6(connection);
    seedLegacyAppNamespaceAndConfig(connection);
    execute(connection, script("db/sqlite/V7__add_namespace_env_app_visibility.sql"));

    assertThat(queryLong(connection, "select count(*) from ddc_namespace_env_app")).isEqualTo(5L);
    assertThat(queryObject(connection,
            "select biz_code from ddc_namespace where namespace_code='default'"))
            .isEqualTo("default");
    assertThat(uniqueIndexColumns(connection, "uk_ddc_namespace_env_app"))
            .containsExactly("namespace_id", "env_code", "app_id");
}

@Test
void v7RejectsDuplicatePhysicalConfigAcrossLegacyNamespaces() {
    assertThatThrownBy(() -> migrateSeedWithDuplicateConfig(connection))
            .isInstanceOf(SQLException.class);
}
```

- [ ] **Step 2: 运行 RED V7 和 controller 测试**

```bash
./mvnw -B -ntp -f egon-cola-platforms/egon-cola-platform-dynamic-config-center/pom.xml \
  -pl egon-cola-platform-dynamic-config-center-admin -am \
  -Dtest=DdcV7MigrationTest,DdcAppControllerTest,DdcNamespaceControllerTest,DdcNamespaceEnvAppBindingControllerTest \
  -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: FAIL because V7 files, binding types and biz-owned namespace API do not exist.

- [ ] **Step 3: 实现唯一的 V7 方言迁移**

```sql
-- PostgreSQL 核心形状；SQLite 使用临时表重建表达同一最终 schema。
create table ddc_namespace_env_app (
    id varchar(64) primary key,
    namespace_id varchar(64) not null,
    env_code varchar(32) not null,
    app_id varchar(64) not null,
    enabled boolean not null default true,
    created_at timestamp not null,
    updated_at timestamp not null
);
create unique index uk_ddc_namespace_env_app
    on ddc_namespace_env_app(namespace_id, env_code, app_id);
```

迁移先以 app 回填 namespace.bizCode，再按 `(bizCode, namespaceCode)` 合并 namespace，并对每个旧 app-namespace 与现有 `ddc_env` 生成 binding。配置表在删除旧唯一键前先建立 `(biz_code, env, app_code, config_key)` 唯一约束；任何旧 namespace 下的物理重复键由数据库约束直接中止整个 V7 事务。历史表的 namespace 列保留并改为 nullable。

- [ ] **Step 4: 实现 Association Object 和级联查询**

```java
public record DdcNamespaceEnvAppBindingRequest(
        String bizCode,
        String namespaceCode,
        String env,
        String appCode,
        Boolean enabled
) {
}

@Transactional
public DdcNamespaceEnvAppBindingVO create(DdcNamespaceEnvAppBindingRequest request) {
    DdcNamespaceEntity namespace = requireNamespace(request.bizCode(), request.namespaceCode());
    DdcAppEntity app = requireApp(request.bizCode(), request.appCode());
    requireEnv(request.env());
    if (repository.existsByNamespaceIdAndEnvCodeAndAppId(namespace.getId(), request.env(), app.getId())) {
        throw new CommonException(DdcErrorStatus.NAMESPACE_BINDING_EXISTS);
    }
    return save(namespace, app, request);
}
```

新增状态码 `56043/DDC_NAMESPACE_BINDING_EXISTS`、`56044/DDC_NAMESPACE_BINDING_NOT_FOUND`。删除 namespace 时只检查 binding；删除 binding 不删除 app、配置、实例或 Redis 注册。

- [ ] **Step 5: 运行 GREEN V7/binding 测试并提交**

```bash
./mvnw -B -ntp -f egon-cola-platforms/egon-cola-platform-dynamic-config-center/pom.xml \
  -pl egon-cola-platform-dynamic-config-center-admin -am \
  -Dtest=DdcV7MigrationTest,DdcAppControllerTest,DdcNamespaceControllerTest,DdcNamespaceEnvAppBindingControllerTest \
  -Dsurefire.failIfNoSpecifiedTests=false test
git add egon-cola-platforms/egon-cola-platform-dynamic-config-center
git commit -m "feat(ddc-admin): add namespace environment app bindings"
```

### Task 4: 将 Starter 配置协议迁移到 biz-env-app V3

**Files:**
- Modify: `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-starter/src/main/java/top/egon/cola/component/ddc/model/vo/DdcInstanceIdentity.java`
- Modify: `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-starter/src/main/java/top/egon/cola/component/ddc/model/dto/DdcDefaultReportRequest.java`
- Modify: `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-starter/src/main/java/top/egon/cola/component/ddc/model/dto/DdcInstanceRegisterRequest.java`
- Modify: `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-starter/src/main/java/top/egon/cola/component/ddc/model/dto/DdcHeartbeatRequest.java`
- Modify: `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-starter/src/main/java/top/egon/cola/component/ddc/model/dto/DdcAckRequest.java`
- Modify: `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-starter/src/main/java/top/egon/cola/component/ddc/model/dto/DdcPublishMessage.java`
- Modify: `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-starter/src/main/java/top/egon/cola/component/ddc/client/HttpDdcAdminClient.java`
- Modify: `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-starter/src/main/java/top/egon/cola/component/ddc/repository/DdcRedisConfigRepository.java`
- Modify: `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-starter/src/main/java/top/egon/cola/component/ddc/listener/DdcRedisChangeListener.java`
- Modify: `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-starter/src/main/java/top/egon/cola/component/ddc/config/DdcAutoConfig.java`
- Modify: `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-starter/src/main/java/top/egon/cola/component/ddc/service/DdcInstanceService.java`
- Modify: `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-starter/src/main/java/top/egon/cola/component/ddc/service/DdcRuntimeCoordinator.java`
- Modify: `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-starter/src/main/java/top/egon/cola/component/ddc/service/DdcAckDelivery.java`
- Modify: `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-starter/src/main/java/top/egon/cola/component/ddc/management/model/DdcManagementConfigQuery.java`
- Modify: `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-starter/src/main/java/top/egon/cola/component/ddc/management/model/DdcManagementConfig.java`
- Modify: `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-starter/src/main/java/top/egon/cola/component/ddc/management/model/DdcManagementConfigUpsertRequest.java`
- Modify: `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-starter/src/main/java/top/egon/cola/component/ddc/management/model/DdcManagementConfigDeleteRequest.java`
- Modify: `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-starter/src/main/java/top/egon/cola/component/ddc/management/model/DdcManagementPublishRequest.java`
- Modify: `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-starter/src/main/java/top/egon/cola/component/ddc/management/model/DdcManagementInstanceQuery.java`
- Modify: `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-starter/src/main/java/top/egon/cola/component/ddc/management/model/DdcManagementConfigClientInstance.java`
- Modify: `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-starter/src/main/java/top/egon/cola/component/ddc/management/client/HttpDdcManagementClient.java`
- Test: `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-starter/src/test/java/top/egon/cola/component/ddc/client/HttpDdcAdminClientTest.java`
- Test: `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-starter/src/test/java/top/egon/cola/component/ddc/repository/DdcRedisConfigRepositoryTest.java`
- Test: `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-starter/src/test/java/top/egon/cola/component/ddc/service/DdcRuntimeCoordinatorTest.java`
- Test: `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-starter/src/test/java/top/egon/cola/component/ddc/management/client/HttpDdcManagementClientTest.java`

**Interfaces:**
- Runtime DTO scope is `bizCode + env + appCode`; deprecated namespace getters/setters remain no-op compatibility shims for one release.
- Config management models use `bizCode + env + appCode + configKey`; list-only namespace context is handled by Admin REST, not Starter runtime contracts.
- OpenAPI config routes become `/configs/{bizCode}/{env}/{appCode}/{configKey}` and pull query becomes `bizCode, env, appCode`.
- Starter reads, subscribes and ACKs only V3 Redis keys; it does not fall back to V2 values.

- [ ] **Step 1: 写 namespace 变化不影响配置读取和客户端请求失败测试**

```java
@Test
void readsOnePhysicalConfigRegardlessOfDeprecatedNamespace() {
    properties.setBizCode("retail");
    properties.setEnv("local");
    properties.setAppCode("order");
    redisson.getBucket(DdcKeys.v3Config("retail", "local", "order", "feature.x"))
            .set("true");

    properties.setNamespace("namespace-a");
    assertThat(repository.readValue("feature.x")).isEqualTo("true");
    properties.setNamespace("namespace-b");
    assertThat(repository.readValue("feature.x")).isEqualTo("true");
}
```

- [ ] **Step 2: 运行 RED Starter 配置测试**

```bash
./mvnw -B -ntp -f egon-cola-platforms/egon-cola-platform-dynamic-config-center/pom.xml \
  -pl egon-cola-platform-dynamic-config-center-starter -am \
  -Dtest=HttpDdcAdminClientTest,DdcRedisConfigRepositoryTest,DdcRuntimeCoordinatorTest,HttpDdcManagementClientTest \
  -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: FAIL because pull、Redis key、DTO 和管理路径仍包含 namespace。

- [ ] **Step 3: 实现 V3 DTO、HTTP 和订阅口径**

```java
Map<String, List<String>> query = new LinkedHashMap<>();
query.put("bizCode", List.of(require(properties.getBizCode(), "bizCode")));
query.put("env", List.of(properties.getEnv()));
query.put("appCode", List.of(properties.getAppCode()));

return redissonClient.<String>getBucket(DdcKeys.v3Config(
        properties.getBizCode(), properties.getEnv(), properties.getAppCode(), key)).get();
```

`DdcAutoConfig` 只订阅一个 `ddcRedisV3Topic`。如果 deprecated namespace 有值，启动日志只打印属性名和迁移提示，不打印配置值或凭据。

- [ ] **Step 4: 运行 GREEN Starter 测试并提交**

```bash
./mvnw -B -ntp -f egon-cola-platforms/egon-cola-platform-dynamic-config-center/pom.xml \
  -pl egon-cola-platform-dynamic-config-center-starter -am clean test
git add egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-starter
git commit -m "refactor(ddc): use physical biz env app config scope"
```

### Task 5: 迁移 DDC Admin 配置、发布、租约和可选筛选

**Files:**
- Modify entities: `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin/src/main/java/top/egon/cola/component/ddc/admin/model/entity/DdcConfigItemEntity.java`, `DdcConfigVersionEntity.java`, `DdcPublishTaskEntity.java`, `DdcPublishAckEntity.java`, `DdcInstanceEntity.java`, `DdcOperationLogEntity.java`
- Modify DTO/VO: `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin/src/main/java/top/egon/cola/component/ddc/admin/model/dto/DdcConfigCreateRequest.java`, `DdcConfigQueryRequest.java`, `DdcPublishRequest.java`, `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin/src/main/java/top/egon/cola/component/ddc/admin/model/vo/DdcConfigResourceKey.java`, `DdcAtomicPublishCommand.java`, `DdcConfigVO.java`
- Modify repositories: `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin/src/main/java/top/egon/cola/component/ddc/admin/repository/DdcConfigItemRepository.java`, `DdcConfigVersionRepository.java`, `DdcInstanceRepository.java`, `DdcConfigLeaseRedisRepository.java`, `DdcRedisRepository.java`, `DdcPublishTaskRepository.java`
- Modify services: `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin/src/main/java/top/egon/cola/component/ddc/admin/service/DdcConfigService.java`, `DdcConfigLeaseService.java`, `DdcInstanceAdminService.java`, `DdcScopeGate.java`, `DdcCacheService.java`, `DdcPublishService.java`, `DdcPendingPublishDispatcher.java`, `DdcPublishStateTransitionService.java`, `PublishFailureRecorder.java`
- Modify controllers/facade: `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin/src/main/java/top/egon/cola/component/ddc/admin/controller/DdcConfigController.java`, `DdcOpenApiController.java`, `DdcManagementOpenApiController.java`, `DdcRegistryAdminController.java`, `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin/src/main/java/top/egon/cola/component/ddc/admin/service/DdcManagementFacade.java`
- Modify error handling: `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin/src/main/java/top/egon/cola/component/ddc/admin/config/DdcGlobalExceptionHandler.java`
- Modify management service models: `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-starter/src/main/java/top/egon/cola/component/ddc/management/model/DdcManagementServiceKey.java`, `DdcManagementServiceQuery.java`
- Tests: `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin/src/test/java/top/egon/cola/component/ddc/admin/service/DdcConfigServiceTest.java`, `DdcScopeGateTest.java`, `DdcPublishPreparationTest.java`, `DdcPublishRetryTest.java`, `DdcAckServiceTest.java`
- Tests: `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin/src/test/java/top/egon/cola/component/ddc/admin/controller/DdcConfigControllerTest.java`, `DdcRegistryAdminControllerTest.java`, `DdcManagementOpenApiControllerTest.java`
- Test: `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin/src/test/java/top/egon/cola/component/ddc/admin/config/DdcGlobalExceptionHandlerTest.java`
- Test: `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin/src/test/java/top/egon/cola/component/ddc/admin/integration/DdcSyncPublishFlowTest.java`

**Interfaces:**
- Config current state and config-client instance entities contain `bizCode, env, appCode` and no active namespace field.
- Historical version/task/ACK/log entities contain `bizCode` and retain nullable legacy namespace snapshot; all new writes set legacy namespace to null.
- `DdcConfigItemRepository.search(bizCode, namespaceCode, env, appCode, configKey, includeDeleted)` uses one SQL `EXISTS` binding predicate and returns distinct physical configs.
- `DdcConfigVO.visibleNamespaces` lists enabled binding namespace codes for its biz/env/app.
- `DdcScopeGate.assertEnabled(bizCode, env, appCode)` checks only biz/env/app enabled state.
- Admin service list and registry service list accept every filter as optional; instance lookup remains exact and excludes namespace.
- `DdcManagementServiceKey` adds `serviceId` and removes namespace; `DdcManagementServiceQuery` uses optional `namespaceCode` only as a management visibility filter.
- MVC binding/enum/body errors map to `DDC_INVALID_REQUEST`, not `DDC_INTERNAL_FAILURE`.

- [ ] **Step 1: 写配置空筛选、namespace 去重、注册部分筛选和 56000 错误测试**

```java
@Test
void listsAllConfigsWhenEveryFilterIsMissing() {
    DdcConfigQueryRequest query = new DdcConfigQueryRequest();
    when(repository.search(null, null, null, null, null, false))
            .thenReturn(List.of(config("infra", "local", "ge", "gateway.rules.active")));
    assertThat(service.list(query)).hasSize(1);
}

@Test
void sameConfigVisibleInTwoNamespacesIsReturnedOnce() {
    when(bindingRepository.findVisibleNamespaceCodes("infra", "local", "ge"))
            .thenReturn(List.of("default", "ops"));
    assertThat(service.list(query).getFirst().getVisibleNamespaces())
            .containsExactly("default", "ops");
}

@Test
void missingRegistryFiltersReturnSuccessInsteadOfInternalFailure() throws Exception {
    mockMvc.perform(get("/api/v1/ddc/registry/services"))
            .andExpect(jsonPath("$.success").value(true));
}

@Test
void missingExactInstanceIdentityReturnsInvalidRequest() throws Exception {
    mockMvc.perform(get("/api/v1/ddc/registry/instances"))
            .andExpect(jsonPath("$.status").value("DDC_INVALID_REQUEST"));
}
```

- [ ] **Step 2: 运行 RED Admin 测试**

```bash
./mvnw -B -ntp -f egon-cola-platforms/egon-cola-platform-dynamic-config-center/pom.xml \
  -pl egon-cola-platform-dynamic-config-center-admin -am \
  -Dtest=DdcConfigServiceTest,DdcConfigControllerTest,DdcRegistryAdminControllerTest,DdcGlobalExceptionHandlerTest \
  -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: FAIL because config list performs an exact null lookup, registry parameters are required, and MVC binding exceptions fall through to 56999.

- [ ] **Step 3: 实现物理配置查询和 visibleNamespaces**

```java
@Query(value = """
        select distinct c.*
          from ddc_config_item c
         where (:bizCode is null or c.biz_code = :bizCode)
           and (:env is null or c.env = :env)
           and (:appCode is null or c.app_code = :appCode)
           and (:configKey is null or c.config_key like ('%' || :configKey || '%'))
           and (:includeDeleted = true or c.deleted = false)
           and (:namespaceCode is null or exists (
               select 1
                 from ddc_namespace_env_app b
                 join ddc_namespace n on n.id = b.namespace_id
                 join ddc_app a on a.id = b.app_id
                where b.enabled = true
                  and n.enabled = true
                  and n.namespace_code = :namespaceCode
                  and n.biz_code = c.biz_code
                  and a.biz_code = c.biz_code
                  and a.app_code = c.app_code
                  and b.env_code = c.env))
         order by c.biz_code, c.env, c.app_code, c.config_key
        """, nativeQuery = true)
List<DdcConfigItemEntity> search(
        @Param("bizCode") String bizCode,
        @Param("namespaceCode") String namespaceCode,
        @Param("env") String env,
        @Param("appCode") String appCode,
        @Param("configKey") String configKey,
        @Param("includeDeleted") boolean includeDeleted);
```

所有 create/upsert/find/pull/value/publish/ACK/cache/lease resource key 都改为 `bizCode, env, appCode, configKey`。V7 legacy namespace 字段只在读取旧历史记录时保留，新增记录写 null。

- [ ] **Step 4: 实现管理目录 optional filter 与错误映射**

```java
@GetMapping("/services")
public ResultRecord<DdcManagementServiceCatalog> services(
        @RequestParam(required = false) String bizCode,
        @RequestParam(required = false) String namespaceCode,
        @RequestParam(required = false) String env,
        @RequestParam(required = false) String appCode,
        @RequestParam(required = false) String serviceKind,
        @RequestParam(required = false) String protocol,
        @RequestParam(required = false) String serviceName,
        @RequestParam(required = false) String group,
        @RequestParam(required = false) String version) {
    return ResultRecord.success(facade.getServiceKeys(new DdcManagementServiceQuery(
            bizCode, namespaceCode, env, appCode, serviceKind,
            protocol, serviceName, group, version)));
}

@ExceptionHandler({MissingServletRequestParameterException.class,
        MethodArgumentTypeMismatchException.class,
        HttpMessageNotReadableException.class,
        IllegalArgumentException.class})
public ResultRecord<Void> handleInvalidRequest(Exception exception) {
    log.debug("Invalid DDC request", exception);
    return ResultRecord.failure(DdcErrorStatus.INVALID_REQUEST);
}
```

namespaceCode 过滤先从 binding repository 得到允许的物理 `(biz,env,app)` 集合，再与 Redis V3 服务目录求交；未知值返回空 catalog。`/registry/instances` 必须使用完整物理 key，缺少字段时抛 `DdcAdminException(DdcErrorStatus.INVALID_REQUEST)`。

- [ ] **Step 5: 运行 Admin 全量 GREEN 和 V7 SQLite 测试**

```bash
./mvnw -B -ntp -f egon-cola-platforms/egon-cola-platform-dynamic-config-center/pom.xml clean test
```

检查 reactor summary 确认 Starter、Admin 和 Test 子模块实际执行测试，不把只构建父 POM 的 `BUILD SUCCESS` 当成证据。

- [ ] **Step 6: 提交物理配置与筛选修复**

```bash
git add egon-cola-platforms/egon-cola-platform-dynamic-config-center
git commit -m "fix(ddc-admin): use physical scopes and optional filters"
```

### Task 6: 重构 DDC Web 为 biz-namespace-env-app 浏览

**Files:**
- Modify: `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin-web/src/api/types.ts`
- Modify: `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin-web/src/components/scope/ScopeSelects.tsx`
- Modify: `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin-web/src/components/scope/useScopeOptions.ts`
- Modify: `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin-web/src/components/scope/NamespaceSelect.tsx`
- Modify: `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin-web/src/components/scope/EnvSelect.tsx`
- Modify: `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin-web/src/components/scope/AppSelect.tsx`
- Modify: `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin-web/src/lib/scopeDefaults.ts`
- Modify: `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin-web/src/pages/NamespacesPage.tsx`
- Modify: `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin-web/src/pages/AppsPage.tsx`
- Modify: `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin-web/src/pages/RegistryPage.tsx`
- Modify: `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin-web/src/pages/ConfigsPage.tsx`
- Modify: `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin-web/src/pages/ConfigEditorDialog.tsx`
- Tests: `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin-web/src/components/scope/ScopeSelects.test.tsx`, `useScopeOptions.test.ts`
- Tests: `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin-web/src/pages/NamespacesPage.test.tsx`, `AppsPage.test.tsx`, `RegistryPage.test.tsx`, `ConfigsPage.test.tsx`

**Interfaces:**
- `ScopeValue` field order is `bizCode, namespaceCode, env, appCode`.
- Empty filter values stay empty on initial list pages; they are not replaced with default biz/app/env/namespace.
- Namespace page owns binding editing grouped by env with app multi-select.
- Registry service type contains `serviceId`, `bizCode`, `env`, `appCode` and physical service fields; instance Drawer uses the service row's exact physical key.
- Config type contains `bizCode`, no physical namespace, and `visibleNamespaces: string[]`.

- [ ] **Step 1: 写级联顺序、空筛选、多 namespace 同实例和配置去重失败测试**

```tsx
it('loads biz then namespace then env then app', async () => {
  render(<ScopeSelects value={emptyScope} onChange={onChange} />)
  expect(screen.getAllByRole('combobox')).toHaveLength(4)
  expect(fetchMock).toHaveFetched('/api/v1/ddc/namespaces?bizCode=infra')
  expect(fetchMock).toHaveFetched('/api/v1/ddc/envs?bizCode=infra&namespaceCode=default')
  expect(fetchMock).toHaveFetched('/api/v1/ddc/apps?bizCode=infra&namespaceCode=default&env=local')
})

it('opens the same physical instances from either namespace', async () => {
  server.use(serviceCatalogFor('default', service), serviceCatalogFor('ops', service))
  expect(await openInstances('default')).toEqual(await openInstances('ops'))
})
```

- [ ] **Step 2: 运行 RED Web 测试**

```bash
npm --prefix egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin-web test -- --run
```

Expected: FAIL because current order is biz-app-namespace-env, namespace belongs to app, and list pages inject default scope.

- [ ] **Step 3: 实现级联 hook 与 binding 管理**

```ts
export type ScopeValue = {
  bizCode: string
  namespaceCode: string
  env: string
  appCode: string
}

const namespacePath = buildPath('/api/v1/ddc/namespaces', { bizCode })
const envPath = buildPath('/api/v1/ddc/envs', { bizCode, namespaceCode })
const appPath = buildPath('/api/v1/ddc/apps', { bizCode, namespaceCode, env })
```

上级变化清空所有下级：biz 清 namespace/env/app，namespace 清 env/app，env 清 app。列表页初始 scope 为四个空字符串；ConfigEditor 新建时校验四项非空并要求 binding 已存在，不再自动创建 app/namespace。

- [ ] **Step 4: 实现 Registry/Configs 展示**

```ts
const exactInstanceQuery = buildQuery({
  bizCode: service.bizCode,
  env: service.env,
  appCode: service.appCode,
  serviceKind: service.serviceKind,
  protocol: service.protocol,
  serviceName: service.serviceName,
  group: service.group,
  version: service.version,
})
```

Registry 主表按 appId/`bizCode|appCode` 去重统计；serviceId 显示前 12 位并提供复制完整值。Configs 用 `visibleNamespaces` Tag 展示，同一 configId 永远只渲染一行。

- [ ] **Step 5: 运行 Web 全量验证并提交**

```bash
npm --prefix egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin-web test -- --run
npm --prefix egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin-web run typecheck
npm --prefix egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin-web run lint
npm --prefix egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin-web run build
git add egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin-web
git commit -m "feat(ddc-web): browse namespace visibility bindings"
```

### Task 7: 简化 Gateway Web 本地 Access Token 登录

**Files:**
- Modify: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web/src/auth/LoginPage.tsx`
- Modify: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web/src/auth/tokenStore.ts`
- Create: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web/src/auth/LoginPage.test.tsx`
- Modify: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web/src/auth/tokenStore.test.ts`

**Interfaces:**
- Produces `oauthRefreshEnabled = Boolean(VITE_GATEWAY_ADMIN_TOKEN_URL && VITE_GATEWAY_ADMIN_CLIENT_ID)`.
- Local mode hides Refresh Token and submits only accessToken.
- OAuth mode preserves optional Refresh Token and refresh behavior.

- [ ] **Step 1: 写本地隐藏 Refresh Token、OAuth 显示字段失败测试**

```tsx
it('asks only for access token in local mode', () => {
  renderLogin()
  expect(screen.getByLabelText('Access Token')).toBeInTheDocument()
  expect(screen.queryByLabelText(/Refresh Token/)).not.toBeInTheDocument()
  expect(screen.getByText('本地模式仅需 Access Token')).toBeInTheDocument()
})
```

- [ ] **Step 2: 运行 RED 测试**

```bash
npm --prefix egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web test -- --run
```

Expected: FAIL because the Refresh Token form item is always rendered.

- [ ] **Step 3: 实现环境能力开关**

```ts
export const oauthRefreshEnabled = Boolean(
  import.meta.env.VITE_GATEWAY_ADMIN_TOKEN_URL
  && import.meta.env.VITE_GATEWAY_ADMIN_CLIENT_ID,
)
```

只有 `oauthRefreshEnabled` 为 true 时渲染 Refresh Token Form.Item；否则显示本地说明并调用 `auth.login({ accessToken }, remember)`。

- [ ] **Step 4: 运行 Gateway Web 全量验证并提交**

```bash
npm --prefix egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web test -- --run
npm --prefix egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web run typecheck
npm --prefix egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web run lint
npm --prefix egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web run build
git add egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web
git commit -m "fix(gateway-web): simplify local token login"
```

### Task 8: 注册 Gateway Admin=infra/ga 与 Engine=infra/ge

**Files:**
- Modify: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin/pom.xml`
- Modify: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin/src/main/resources/application.yml`
- Modify: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin/src/main/java/top/egon/cola/component/gateway/admin/GatewayAdminConfiguration.java`
- Modify: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin/src/main/java/top/egon/cola/component/gateway/admin/rule/GatewayDdcPublicationCommand.java`
- Modify: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin/src/main/java/top/egon/cola/component/gateway/admin/rule/GatewayDdcRulePublisher.java`
- Modify: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin/src/main/java/top/egon/cola/component/gateway/admin/application/release/GatewayReleasePublicationCoordinator.java`
- Modify: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-engine/src/main/resources/application.yml`
- Modify: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-test/egon-cola-platform-gateway-test-http-provider/src/main/resources/application.yml`
- Test: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin/src/test/java/top/egon/cola/component/gateway/admin/GatewayAdminApplicationConfigurationTest.java`
- Test: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin/src/test/java/top/egon/cola/component/gateway/admin/rule/GatewayDdcRulePublisherTest.java`
- Test: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-engine/src/test/java/top/egon/cola/component/gateway/engine/GatewayEngineConfigurationTest.java`
- Test: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-test/egon-cola-platform-gateway-test-suite/src/test/java/top/egon/cola/component/gateway/test/live/GatewayLiveTopologyEngineLeaseTest.java`

**Interfaces:**
- Gateway Admin adds the existing `egon-cola-platform-gateway-provider-runtime` module dependency; no new external dependency.
- Gateway Admin local DDC identity is `infra/local/ga`; HTTP service is `egon-cola-gateway-admin`, kind `HTTP_PROVIDER`, metadata `gateway.component=admin`.
- Gateway Engine local DDC identity is `infra/local/ge`; RPC service is `egon-gateway-rpc`, kind `INTERNAL_GATEWAY`, metadata includes `gateway.component=engine`.
- Gateway Admin release publication target is configured separately as `gateway.admin.ddc.target-biz-code=infra`, `target-app-code=ge`; namespace from Gateway rule content is not used as DDC config identity.

- [ ] **Step 1: 写 Admin HTTP lease 和 Engine INTERNAL_GATEWAY 物理身份失败测试**

```java
@Test
void gatewayAdminRegistersAsInfraGaHttpProvider() {
    contextRunner.withPropertyValues(
            "egon.cola.component.ddc.enabled=true",
            "egon.cola.component.ddc.registry.enabled=true",
            "egon.cola.component.ddc.biz-code=infra",
            "egon.cola.component.ddc.app-code=ga",
            "egon.cola.component.ddc.env=local",
            "egon.cola.component.gateway.provider.http.enabled=true")
        .run(context -> assertThat(context).hasSingleBean(HttpProviderLeaseRuntime.class));
}

@Test
void engineRegistrationUsesInfraLocalGeWithoutNamespace() {
    assertThat(captured.serviceKey()).extracting(
            DdcServiceKey::bizCode, DdcServiceKey::env, DdcServiceKey::appCode)
            .containsExactly("infra", "local", "ge");
}
```

- [ ] **Step 2: 运行 RED Gateway 测试**

```bash
./mvnw -B -ntp -f egon-cola-platforms/egon-cola-platform-gateway/pom.xml \
  -pl egon-cola-platform-gateway-admin,egon-cola-platform-gateway-engine -am \
  -Dtest=GatewayAdminApplicationConfigurationTest,GatewayDdcRulePublisherTest,GatewayEngineConfigurationTest,GatewayLiveTopologyEngineLeaseTest \
  -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: FAIL because Admin lacks provider-runtime self-registration and publication still derives app/namespace from Gateway group scope.

- [ ] **Step 3: 配置 ga/ge 并分离 Admin 自身作用域与 Engine 发布目标**

```yaml
egon:
  cola:
    component:
      ddc:
        biz-code: ${DDC_BIZ_CODE:infra}
        env: ${DDC_ENV:local}
        app-code: ${DDC_APP_CODE:ga}
        registry:
          enabled: ${DDC_REGISTRY_ENABLED:false}
      gateway:
        provider:
          http:
            enabled: ${GATEWAY_ADMIN_DDC_REGISTRATION_ENABLED:false}
            service-name: egon-cola-gateway-admin
            metadata:
              gateway.component: admin
```

Engine application.yml 使用 `DDC_APP_CODE:ge`；本机运行命令显式启用 DDC 和 registry。`GatewayReleasePublicationCoordinator.Scope` 改为 `(bizCode, env, appCode)`，值来自 target properties 和 compiled env。

- [ ] **Step 4: 运行 Gateway 全 reactor GREEN 并提交**

```bash
./mvnw -B -ntp -f egon-cola-platforms/egon-cola-platform-gateway/pom.xml clean test
git add egon-cola-platforms/egon-cola-platform-gateway
git commit -m "feat(gateway): register admin and engine with ddc"
```

### Task 9: 全量回归、本机迁移、启动和验收

**Files:**
- Runtime only: `target/local-biz-app-run/` for generated JWT, PID files and logs; all artifacts remain ignored.
- Read/update only if source verification exposes a real defect: the exact file covered by a new failing test in Tasks 1-8; each isolated repair uses a `fix:` commit message naming the observed failing boundary and root cause.

**Interfaces:**
- DDC Admin readiness: `http://127.0.0.1:18080/actuator/health/readiness`.
- Gateway Admin readiness: `http://127.0.0.1:8080/actuator/health/readiness`.
- Gateway Engine public/internal/management/RPC ports: `18081/18082/18083/19090`.
- Gateway Web: `http://127.0.0.1:5173`; DDC Web: `http://127.0.0.1:5174`.
- Order Providers: same `retail/local/order/order-service` physical service on `18084` and `18085`, each using a different default UUIDv7 instance ID.
- Required bindings: `infra/default/local/ga`, `infra/default/local/ge`, `retail/default/local/order`; add a second namespace binding for order to prove no re-registration is needed.

- [ ] **Step 1: 停止旧项目进程但保留用户 PostgreSQL/Redis**

```bash
lsof -nP -iTCP:18080 -iTCP:8080 -iTCP:18081 -iTCP:18082 -iTCP:18083 \
  -iTCP:19090 -iTCP:18084 -iTCP:18085 -iTCP:5173 -iTCP:5174 -sTCP:LISTEN
```

只终止通过 PID 文件、命令行 main class 和端口三项共同确认的项目进程。单独验证 PostgreSQL 和 Redis 仍可用，不执行清库、flush 或删除无关 key。

- [ ] **Step 2: 执行全量静态回归**

```bash
./mvnw -B -ntp -f egon-cola-platforms/egon-cola-platform-dynamic-config-center/pom.xml clean test
./mvnw -B -ntp -f egon-cola-components/egon-cola-component-rpc/pom.xml clean test
./mvnw -B -ntp -f egon-cola-platforms/egon-cola-platform-gateway/pom.xml clean test
npm --prefix egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin-web test -- --run
npm --prefix egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin-web run typecheck
npm --prefix egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin-web run lint
npm --prefix egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin-web run build
npm --prefix egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web test -- --run
npm --prefix egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web run typecheck
npm --prefix egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web run lint
npm --prefix egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web run build
```

每个 Maven 命令检查 reactor summary 和实际测试数；任何失败先定位根因、增加最小失败测试、修复、重新执行受影响命令。

- [ ] **Step 3: 在本机 PostgreSQL 运行 V7 冲突预检查和真实 Flyway 迁移**

```sql
select a.biz_code, c.env, c.app_code, c.config_key, count(*)
  from ddc_config_item c
  join ddc_app a on a.app_code = c.app_code
 group by a.biz_code, c.env, c.app_code, c.config_key
having count(*) > 1;
```

预检查必须返回 0 行才启动新 DDC Admin。使用已有受控本机连接配置执行 Flyway；不在命令行回显密码。迁移后查询 Flyway history、V7 表/索引和旧数据行数。

- [ ] **Step 4: 构建并按依赖顺序启动服务**

```bash
./mvnw -B -ntp -f egon-cola-platforms/egon-cola-platform-dynamic-config-center/pom.xml \
  -pl egon-cola-platform-dynamic-config-center-admin -am package -DskipTests
./mvnw -B -ntp -f egon-cola-platforms/egon-cola-platform-gateway/pom.xml \
  -pl egon-cola-platform-gateway-admin,egon-cola-platform-gateway-engine,egon-cola-platform-gateway-test/egon-cola-platform-gateway-test-http-provider \
  -am package -DskipTests
```

依次启动 DDC Admin、创建 biz/env/app/namespace/binding、Gateway Admin、Gateway Engine、两个订单 Provider、Gateway Web、DDC Web。使用 readiness 轮询；不使用固定长 sleep。生成新的 `target/local-biz-app-run/admin.jwt`，权限设为仅当前用户可读，不打印内容。

- [ ] **Step 5: 验证 optional filter、可见性和实例身份**

```text
GET /api/v1/ddc/registry/services
GET /api/v1/ddc/registry/services?serviceKind=INTERNAL_GATEWAY&protocol=grpc
GET /api/v1/ddc/registry/services?bizCode=infra&namespaceCode=default&env=local
GET /api/v1/ddc/configs?includeDeleted=false
GET /api/v1/ddc/configs?bizCode=infra&namespaceCode=default&env=local&appCode=ge
```

断言所有请求 `success=true`；未知筛选返回空数组。`infra/default/local` 能看到 ga/ge；order catalog 只有一个 serviceId，instance Drawer 有两个 ONLINE 实例，instanceId/leaseId/port 均不同。新增 `retail/ops/local/order` binding 后不重启 Provider，再查询应返回相同 serviceId 和实例集合。

- [ ] **Step 6: 验证 Gateway 路由和 Web 静态入口**

```text
GET http://127.0.0.1:5173
GET http://127.0.0.1:5174
GET http://127.0.0.1:18081/api/providers/probe-1
```

Gateway Web 登录页本地模式不显示 Refresh Token；DDC Web 首次空筛选能加载列表；重复请求已发布订单路由，响应中的非敏感 instance 标识覆盖 18084 和 18085 两个副本。不得使用浏览器或 Computer Use，使用 HTTP、前端测试和构建产物完成验证。

- [ ] **Step 7: 最终仓库、进程与安全检查**

```bash
git diff --check
git status --short --branch
lsof -nP -iTCP:18080 -iTCP:8080 -iTCP:18081 -iTCP:18082 -iTCP:18083 \
  -iTCP:19090 -iTCP:18084 -iTCP:18085 -iTCP:5173 -iTCP:5174 -sTCP:LISTEN
```

确认所有源代码已提交、没有无关改动、日志没有 Token/密码明文、目标服务保持运行。最终回复列出 URL、JWT 文件路径、serviceId/instance 数量、验证命令结果和任何未验证边界，不输出凭据。

## Plan Self-Review

- Spec coverage: V7 关系模型、环境级多对多、配置共享、V3 服务目录、serviceId、instanceId Strategy、optional filters、DDC Web、Gateway 登录、ga/ge 注册、两个订单副本和最终保持运行分别由 Tasks 1-9 覆盖。
- Migration safety: 只创建 PostgreSQL/SQLite 两个 V7 方言文件；重复物理配置键在迁移事务内失败，V1-V6 不改。
- Type consistency: 所有物理配置接口统一 `bizCode, env, appCode, configKey`；所有物理服务接口统一 `bizCode, env, appCode, serviceKind, protocol, serviceName, group, version`；管理 namespace 字段统一命名 `namespaceCode`。
- Identity consistency: serviceId 是同一 canonical 的 SHA-256；同一副本跨 DDC Config/HTTP/RPC/Gateway runtime 使用同一基础 instanceId；每个注册会话使用独立 leaseId。
- Query consistency: services/configs 可空或部分筛选；instances 精确查询；未知值成功返回空结果。
- Pattern decision: binding 使用 Association Object，实例 ID 使用 Strategy，Gateway 继续使用既有 Adapter/lease runtime；未增加无收益的抽象层。
- Validation boundary: Maven/Vitest/build 证明源码和模块行为；Task 9 的本机多进程、PostgreSQL、Redis 与 HTTP 证据才证明本机联调，不扩张为生产 HA、Redis Cluster/Sentinel 或多主机证明。
