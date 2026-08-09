# DDC Direct RPC Facade Migration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将 DDC 的配置运行时、服务注册中心和管理机器接口从 HTTP OpenAPI 一次性迁移为基于 egon-rpc 的 Direct gRPC Facade，解除 RPC Starter 对 DDC Starter 的反向依赖，并使多个 DDC Admin 实例在共享 PostgreSQL/Redis 与外部负载均衡下安全运行。

**Architecture:** RPC Starter 只提供中立 Registry/Directory Port、Gateway/Direct Channel Strategy、程序化 Direct Client 与可扩展拦截器；新增 `rpc-ddc-adapter` 作为唯一同时依赖 RPC Starter 和 DDC Starter 的 Ports-and-Adapters 集成叶子，拥有 Protobuf 契约、DDC Client Adapter、Registry Bridge、ConfigData Transport 和客户端鉴权。DDC Admin 通过三个 Facade 与三个 RPC Provider 暴露无会话机器协议，自身使用 `registration-mode=DISABLED`，由 DNS/VIP/Kubernetes Service 直连发现；普通业务 RPC 仍经 Gateway，非 DDC 平台服务仍由 DDC 提供配置和服务发现。

**Tech Stack:** Java 21、Spring Boot 3.5.16、Maven、gRPC Java/Netty、Protocol Buffers、Spring ConfigData、Spring Data JPA、PostgreSQL、Redisson/Redis、JUnit 5、Mockito、AssertJ、Testcontainers。

**Exact path roots used below:**

- `RPC_ROOT` = `egon-cola-components/egon-cola-component-rpc`
- `RPC_MAIN` = `RPC_ROOT/egon-cola-component-rpc-starter/src/main/java/top/egon/cola/component/rpc`
- `RPC_TEST` = `RPC_ROOT/egon-cola-component-rpc-starter/src/test/java/top/egon/cola/component/rpc`
- `ADAPTER` = `RPC_ROOT/egon-cola-component-rpc-ddc-adapter`
- `ADAPTER_MAIN` = `ADAPTER/src/main/java/top/egon/cola/component/rpc/ddc`
- `ADAPTER_TEST` = `ADAPTER/src/test/java/top/egon/cola/component/rpc/ddc`
- `DDC_ROOT` = `egon-cola-platforms/egon-cola-platform-dynamic-config-center`
- `DDC_MAIN` = `DDC_ROOT/egon-cola-platform-dynamic-config-center-starter/src/main/java/top/egon/cola/component/ddc`
- `DDC_TEST` = `DDC_ROOT/egon-cola-platform-dynamic-config-center-starter/src/test/java/top/egon/cola/component/ddc`
- `ADMIN_MAIN` = `DDC_ROOT/egon-cola-platform-dynamic-config-center-admin/src/main/java/top/egon/cola/component/ddc/admin`
- `ADMIN_TEST` = `DDC_ROOT/egon-cola-platform-dynamic-config-center-admin/src/test/java/top/egon/cola/component/ddc/admin`
- `GATEWAY_ROOT` = `egon-cola-platforms/egon-cola-platform-gateway`

Every path using one of these labels is relative to the exact repository path declared here; the labels are documentation abbreviations, not shell variables or unresolved implementation placeholders.

## Global Constraints

- 规格来源：`docs/superpowers/specs/2026-08-09-ddc-direct-rpc-facade-migration-design.md`；第 21 节已锁定的决策不得在实施阶段重新开放。
- 这是 monorepo 破坏式迁移：不保留旧机器 HTTP Endpoint、旧 HTTP Property、deprecated wrapper 或长期双栈；各任务中允许短暂的内部过渡态，但最终发布必须只有 RPC 机器协议。
- `rpc-starter` 的 POM、主源码和测试必须对 DDC/adapter 零依赖；`rpc-ddc-adapter` 是唯一允许同时依赖 RPC Starter 与 DDC Starter 的生产模块。
- DDC 是唯一自举例外：Direct RPC target 来自本地 DNS/VIP/Kubernetes Service 配置；不得通过 Gateway 或 DDC Registry 发现 DDC Admin，不得让 DDC Admin 注册自己。
- 普通业务 `@EgonRpcReference` 仍只允许走 Gateway；不得增加公开的 `route=DIRECT` 注解开关。Direct Client 仅供基础设施组合代码使用，并用边界测试限制生产调用点。
- 保持现有 DDC Java Client Port、领域模型、Redis Key/Topic、租约、revision、ConfigData location/优先级、YAML-only、changeId 幂等、ACK 状态机、Admin Web 和人工 REST 语义。
- 不修改数据库 schema、数据或任何现有 Flyway 文件；本次只给 `DdcPublishAckRepository` 增加悲观写锁查询。
- HMAC 必须保留 access key、timestamp、nonce、scope、operation、operator 语义；摘要基于 deterministic Protobuf bytes，Secret 和配置正文不得进入日志或异常。
- gRPC transparent retry 必须关闭；仅在 adapter 应用层按规格允许的幂等方法重试。Publish 的业务 timeout 与 gRPC deadline 必须分开。
- Active-Active 依赖共享 PostgreSQL/Redis 与外部 HTTP/2 LB；不新增 leader election、Raft、自注册、sticky session 或本地权威状态。
- 采用已经批准的模式：Ports and Adapters 隔离依赖，Facade 收敛 Admin 用例，Strategy 区分 Gateway/Direct Channel，Adapter 转换 Protobuf/领域模型，Observer 保留 Redis 实时订阅。不得再增加泛化 Transport Facade、通用 ControlPlane Client、Provider 基类或抽象工厂层级。
- 每个新增 Java 包按仓库规范添加中文在前、英文在后的 `package-info.java` 与 `@NonNullApi`；新增或修改 public 类型和方法补充同风格中英文 Javadoc。
- 每个任务先增加或调整失败测试，再做最小实现，再运行定向验证；所有 Maven `-Dtest=...` 命令使用 `-Dsurefire.failIfNoSpecifiedTests=false`。
- 每个任务只暂存自己列出的文件并独立提交。执行前检查 `git status --short`，保留并绕开用户或其他任务的未提交修改；禁止 reset、checkout 或扫入无关文件。
- 不启动 DDC Admin、Gateway、IdP、RBAC3 或浏览器。只运行 Maven compile/test/integration-test、依赖树和静态残留扫描；真实 DNS/LB、多 JVM、Redis Sentinel/Cluster 和 PostgreSQL HA 留给显式 live topology 验证。

---

### Task 1: Decouple RPC Runtime from DDC Discovery and Identity

**Files:**

- Modify: `RPC_ROOT/egon-cola-component-rpc-starter/pom.xml`
- Modify: `RPC_MAIN/config/EgonRpcAutoConfig.java`
- Modify: `RPC_MAIN/config/EgonRpcProperties.java`
- Create: `RPC_MAIN/provider/RpcProviderRegistry.java`
- Create: `RPC_MAIN/provider/RpcProviderRegistration.java`
- Create: `RPC_MAIN/provider/RpcProviderLease.java`
- Create: `RPC_MAIN/provider/RpcProviderLeaseIdentity.java`
- Create: `RPC_MAIN/provider/RpcLeaseOperationResult.java`
- Create: `RPC_MAIN/provider/RpcProviderRegistrationMode.java`
- Create: `RPC_MAIN/consumer/RpcGatewayDirectory.java`
- Create: `RPC_MAIN/consumer/RpcGatewayQuery.java`
- Create: `RPC_MAIN/consumer/RpcGatewaySnapshot.java`
- Create: `RPC_MAIN/consumer/RpcGatewaySubscription.java`
- Create: `RPC_MAIN/context/RpcProcessIdentityProvider.java`
- Modify: `RPC_MAIN/context/RpcProcessIdentityFactory.java`
- Modify: `RPC_MAIN/provider/RpcProviderLeaseManager.java`
- Modify: `RPC_MAIN/provider/RpcProviderLifecycle.java`
- Modify: `RPC_MAIN/provider/RpcProviderMetadataMerger.java`
- Create: `RPC_TEST/provider/RpcProviderLeaseManagerTest.java`
- Modify: `RPC_TEST/provider/RpcProviderLifecycleTest.java`
- Modify: `RPC_TEST/provider/RpcProviderMetadataMergerTest.java`
- Modify: `RPC_TEST/context/RpcProcessIdentityFactoryTest.java`
- Modify: `RPC_TEST/config/EgonRpcPropertiesTest.java`
- Modify: `RPC_ROOT/egon-cola-component-rpc-test/egon-cola-component-rpc-test-contract/pom.xml`
- Modify under `RPC_ROOT/egon-cola-component-rpc-test/egon-cola-component-rpc-test-contract/src/test/java`: ordinary unit/TCP fixtures returned by the task's DDC import scan, excluding `process/RpcMockGatewayApplication.java` and `process/RpcProcessIT.java` until Task 10.

**Interfaces and behavior:**

- `RpcProviderRegistry` exposes exactly `register(RpcProviderRegistration)`, `heartbeat(RpcProviderLeaseIdentity)` and `deregister(RpcProviderLeaseIdentity)`.
- Provider records contain only RPC service/process identity, advertised endpoint, secure flag, stable metadata and lease timing; they must not expose `DdcServiceKind`, DDC scope types, Redis keys or DDC lease records.
- `RpcGatewayDirectory.subscribe(RpcGatewayQuery, Consumer<RpcGatewaySnapshot>)` owns discovery snapshots only. `RpcGatewayQuery` includes env, optional target biz/app, serviceName, group and version; snapshot contains revision, observedAt and `RpcGatewayEndpoint` values.
- `RpcProcessIdentityFactory` resolves application name, `egon.cola.component.rpc.identity.env/host/instance-id` and PID without `DdcProperties` or `DdcInstanceIdentity`.
- `RpcProviderMetadataMerger` validates neutral keys locally: reject blank key, reject `egon.rpc.*` and conflicting values, return immutable sorted map. DDC/Gateway metadata conventions move to adapter later.
- `registration-mode=REQUIRED` is the default. With Provider enabled and no `RpcProviderRegistry`, startup fails with an error naming the missing SPI; `DISABLED` starts the server, marks local providers available before accepting calls, and performs no register/heartbeat/deregister.

- [ ] **Step 1: Add neutral SPI and registration-mode tests first.** Cover exact register/heartbeat/recover/deregister arguments, availability transitions, default `REQUIRED`, explicit `DISABLED`, missing Registry failure, and identity fallback/override values. Update metadata tests to reject only neutral reserved keys.

- [ ] **Step 2: Run the focused tests and confirm they fail against the DDC-coupled implementation.**

```bash
./mvnw -B -ntp \
  -pl egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter \
  -am \
  -Dtest=RpcProviderLeaseManagerTest,RpcProviderLifecycleTest,RpcProviderMetadataMergerTest,RpcProcessIdentityFactoryTest,EgonRpcPropertiesTest \
  -Dsurefire.failIfNoSpecifiedTests=false \
  test
```

Expected: test compilation or assertions fail because the neutral SPI and registration mode do not yet exist.

- [ ] **Step 3: Introduce the neutral Registry/Directory/identity contracts.** Keep records immutable, validate mandatory identity/endpoint/lease fields at construction, and preserve the current serviceName/group/version/protocol semantics without DDC names.

- [ ] **Step 4: Refactor provider lifecycle.** Convert each `RpcProviderBinding` into `RpcProviderRegistration`, store `RpcProviderLease`, and use complete lease identity for heartbeat/deregister. In `DISABLED`, skip scheduling heartbeat and lease cleanup while keeping graceful server availability/shutdown ordering.

- [ ] **Step 5: Refactor AutoConfiguration and metadata.** Enable only `EgonRpcProperties`; inject `RpcProcessIdentityProvider`, optional `RpcProviderRegistry`, ordered metadata contributors and neutral dependencies. Remove every `top.egon.cola.component.ddc` import.

- [ ] **Step 6: Remove the DDC Starter dependency from RPC Starter POM.** Do not replace it with adapter or any Platforms dependency.

- [ ] **Step 7: Migrate ordinary RPC test fixtures off DDC types.** Use the exact scan:

```bash
rg -l "top\.egon\.cola\.component\.ddc|InMemoryDdc|TestDdcScopes" \
  egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-test \
  --glob '*.java' --glob '*.yml' --glob 'pom.xml'
```

Replace in-memory DDC registry fixtures used by unit/TCP tests with neutral `RpcProviderRegistry` and `RpcGatewayDirectory` fakes. `RpcMockGatewayApplication` and `RpcProcessIT` are compiled even when the `ddc-live-test` profile does not execute, so keep their existing DDC topology compiling through an explicit **test-scoped** DDC Starter dependency in the test-contract POM; do not rely on RPC Starter transitively providing it. Task 10 replaces that final process topology with adapter/Admin RPC and removes this temporary test dependency.

- [ ] **Step 8: Run RPC Starter tests and dependency checks.**

```bash
./mvnw -B -ntp \
  -pl egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter \
  -am test
./mvnw -B -ntp \
  -pl egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter \
  dependency:tree \
  -Dincludes=top.egon:egon-cola-platform-dynamic-config-center-starter,top.egon:egon-cola-component-rpc-ddc-adapter
rg -n "top\.egon\.cola\.component\.ddc" \
  egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/{pom.xml,src}
```

Expected: tests pass; dependency tree and source scan print no DDC/adapter dependency or import.

- [ ] **Step 9: Commit Task 1.**

```bash
git commit -m "refactor(rpc): decouple runtime discovery from ddc"
```

---

### Task 2: Add Gateway and Direct Invocation Strategies

**Files:**

- Create: `RPC_MAIN/consumer/RpcInvocationChannelProvider.java`
- Create: `RPC_MAIN/consumer/GatewayRpcInvocationChannelProvider.java`
- Create: `RPC_MAIN/consumer/DirectRpcInvocationChannelProvider.java`
- Create: `RPC_MAIN/consumer/RpcDirectClientFactory.java`
- Create: `RPC_MAIN/consumer/RpcDirectClientHandle.java`
- Create: `RPC_MAIN/consumer/RpcDirectClientSettings.java`
- Create: `RPC_MAIN/context/RpcClientInvocation.java`
- Create: `RPC_MAIN/context/RpcClientInterceptorFactory.java`
- Create: `RPC_MAIN/provider/RpcProviderExceptionMapper.java`
- Modify: `RPC_MAIN/consumer/RpcConsumerGatewayManager.java`
- Modify: `RPC_MAIN/consumer/RpcConsumerInvocationHandler.java`
- Modify: `RPC_MAIN/consumer/RpcConsumerProxyFactory.java`
- Modify: `RPC_MAIN/provider/RpcProviderServerFactory.java`
- Modify: `RPC_MAIN/provider/RpcProviderLifecycle.java`
- Modify: `RPC_MAIN/provider/RpcServerServiceDefinitionFactory.java`
- Modify: `RPC_MAIN/config/EgonRpcAutoConfig.java`
- Create: `RPC_TEST/consumer/RpcDirectClientFactoryTest.java`
- Create: `RPC_TEST/consumer/DirectRpcInvocationChannelProviderTest.java`
- Modify: `RPC_TEST/consumer/RpcConsumerGatewayManagerTest.java`
- Modify: `RPC_TEST/consumer/RpcConsumerInvocationHandlerTest.java`
- Modify: `RPC_TEST/provider/RpcProviderLifecycleTest.java`
- Create: `RPC_TEST/provider/RpcProviderExceptionMapperTest.java`

**Interfaces and behavior:**

- `RpcInvocationChannelProvider` exposes `currentChannel(excluded)`, `recordFailure(channel)` and `maxAttempts()`; the invocation handler has no Gateway-specific dependency.
- `GatewayRpcInvocationChannelProvider` wraps `RpcConsumerGatewayManager`. `@EgonRpcReference` AutoConfiguration wires only this implementation.
- `DirectRpcInvocationChannelProvider` owns one `NettyChannelBuilder.forTarget(target)` channel, default `round_robin`, no transparent retry, deterministic shutdown/await termination, TLS via `RpcTransportSecurity`, and no ApplicationContext dependency.
- `RpcDirectClientFactory` validates the existing `@EgonRpcService` contract and returns `RpcDirectClientHandle<T>`, an `AutoCloseable` handle containing the typed proxy and owned channel. It accepts target, identity, TLS, deadline, load-balancing policy, max message size and ordered `RpcClientInterceptorFactory` values.
- Request-derived HMAC cannot be implemented by a static gRPC interceptor because headers are started before `sendMessage`. `RpcClientInterceptorFactory.create(RpcClientInvocation)` is therefore evaluated by the invocation handler after it has the Protobuf request and before `blockingUnaryCall`; this is a neutral core extension, not DDC logic.
- `RpcProviderExceptionMapper` is an ordered neutral extension returning an optional `StatusRuntimeException`; it is evaluated before the generic `EgonRpcRejectedException`/INTERNAL fallback so DDC Admin can attach typed trailers without duplicating try/catch in three Providers.
- Server factory and Direct factory accept ordered interceptor collections; core trace/invocation interceptors remain first according to explicit order, and adapter/Admin extensions cannot replace them.

- [ ] **Step 1: Add failing Strategy and Direct Client tests.** Prove Gateway failover behavior remains unchanged; Direct uses the configured target without `RpcConsumerGatewayManager`; request-aware interceptor sees exact method and request; deadline is capped; channel closes on handle close and on factory failure; transparent retry is disabled.

- [ ] **Step 2: Add failing server extension tests.** Verify multiple server interceptors execute in order and a custom provider exception mapper preserves status/trailers while the default mapper still sanitizes unknown exceptions.

- [ ] **Step 3: Run the focused tests and confirm expected failures.**

```bash
./mvnw -B -ntp \
  -pl egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter \
  -am \
  -Dtest=RpcDirectClientFactoryTest,DirectRpcInvocationChannelProviderTest,RpcConsumerGatewayManagerTest,RpcConsumerInvocationHandlerTest,RpcProviderLifecycleTest,RpcProviderExceptionMapperTest \
  -Dsurefire.failIfNoSpecifiedTests=false \
  test
```

- [ ] **Step 4: Refactor consumer invocation around Strategy.** Keep current idempotent Gateway retry rule and failure-stage handling. Direct defaults to one attempt at transport level; application adapters decide whether a domain call is safe to repeat.

- [ ] **Step 5: Implement the programmatic Direct Client.** Reuse `RpcContractValidator`, `RpcConsumerInvocationHandler`, generic invocation metadata, trace and `RpcStatusExceptionMapper`. Return ownership explicitly so ConfigData can use try-with-resources before Spring exists.

- [ ] **Step 6: Generalize server composition.** Change `RpcProviderServerFactory.create` to accept an ordered list, update lifecycle wiring, and add the provider exception-mapper chain without exposing DDC error types in core.

- [ ] **Step 7: Add a boundary test for business references.** Extend `EgonRpcReferenceBeanPostProcessor`/AutoConfiguration tests to assert annotated references receive `GatewayRpcInvocationChannelProvider`; no annotation attribute or property may select Direct.

- [ ] **Step 8: Run all RPC Starter tests.**

```bash
./mvnw -B -ntp \
  -pl egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter \
  -am test
```

- [ ] **Step 9: Commit Task 2.**

```bash
git commit -m "feat(rpc): support direct invocation channels"
```

---

### Task 3: Create the RPC DDC Adapter and Protobuf Contracts

**Files:**

- Modify: `RPC_ROOT/pom.xml`
- Modify: `egon-cola-components/egon-cola-components-bom/pom.xml`
- Create: `ADAPTER/pom.xml`
- Create: `ADAPTER/src/main/proto/egon/ddc/v1/ddc_common.proto`
- Create: `ADAPTER/src/main/proto/egon/ddc/v1/ddc_config_runtime.proto`
- Create: `ADAPTER/src/main/proto/egon/ddc/v1/ddc_service_registry.proto`
- Create: `ADAPTER/src/main/proto/egon/ddc/v1/ddc_management.proto`
- Create: `ADAPTER_MAIN/contract/DdcConfigRuntimeRpc.java`
- Create: `ADAPTER_MAIN/contract/DdcServiceRegistryRpc.java`
- Create: `ADAPTER_MAIN/contract/DdcManagementRpc.java`
- Create: `ADAPTER_MAIN/package-info.java` and package docs for `contract`, `contract/proto/v1`, `client`, `client/config`, `client/registry`, `client/management`, `mapping`, `registry`, `security`, `configdata`, `autoconfigure`.
- Create: `ADAPTER_TEST/contract/DdcRpcGeneratedContractTest.java`
- Create: `ADAPTER_TEST/contract/DdcRpcContractDescriptorTest.java`
- Create: `ADAPTER_TEST/DdcRpcModuleBoundaryTest.java`

**Wire contract:**

- All files use `package egon.ddc.v1`, `java_package = "top.egon.cola.component.rpc.ddc.contract.proto.v1"`, `java_multiple_files = true`.
- `ddc_common.proto` defines `DdcScope`, lease session/result, service key/instance, shared enums and `DdcRpcErrorDetail(code,message,retryable)`. Timestamps use `google.protobuf.Timestamp`; enum zero values end in `_UNSPECIFIED`; nullable scalar fields use `optional`.
- `DdcConfigRuntimeService` has unary `RegisterConfigClient`, `HeartbeatConfigClient`, `OfflineConfigClient`, `PullConfig`, `AcknowledgePublish`.
- `DdcServiceRegistryService` has unary `RegisterService`, `HeartbeatService`, `DeregisterService`, `GetServiceInstances`, `GetServices`.
- `DdcManagementService` has unary `FindConfig`, `UpsertConfig`, `DeleteConfig`, `PublishConfig`, `GetPublishTask`, `RetryPublishTask`, `GetConfigClients`, `GetScopeBindings`, `GetServiceKeys`, `GetInstances`.
- Every lease mutation request carries instanceId and leaseId; every config/registry request carries complete scope/key and never assumes sticky connection state. `FindConfigResponse` uses explicit `found` plus optional config. requestedOperator is audit input only.
- Java contracts accept/return generated Protobuf messages only. `DdcConfigRuntimeRpc` declares `grpcClass=DdcConfigRuntimeServiceGrpc.class`, `DdcServiceRegistryRpc` declares `grpcClass=DdcServiceRegistryServiceGrpc.class`, and `DdcManagementRpc` declares `grpcClass=DdcManagementServiceGrpc.class`; all three use `group="ddc"`, `version="1.0.0"` and existing `@EgonRpcMethod` descriptor names.

- [ ] **Step 1: Add the module to the reactor and BOM, then add failing contract tests.** Clone the existing RPC test-contract protobuf plugin configuration, including OS classifier, protoc and grpc-java generator. Dependencies are exactly RPC Starter, DDC Starter, gRPC/Protobuf and required Spring Boot APIs; no Admin/Gateway dependency.

- [ ] **Step 2: Run test compilation and confirm missing generated/contract types.**

```bash
./mvnw -B -ntp \
  -pl egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-ddc-adapter \
  -am \
  -DskipTests test-compile
```

- [ ] **Step 3: Implement all four proto files.** Mirror every current DDC Java Port field, including config resourceName/format/content/version/checksum, registration endpoint/metadata/lease timing, ACK status/error/time, management expectedVersion/changeId/reason/description/timeout and namespaceCode visibility filters. Enforce stable unique field numbers and reserve removed numbers rather than reusing them.

- [ ] **Step 4: Implement the three Java RPC contracts.** The descriptor test must compare every Java method to the generated gRPC service/method descriptor and fail on missing or extra methods.

- [ ] **Step 5: Add module/package boundary assertions.** Require all planned `package-info.java` files, reject imports from DDC Admin/Gateway, and assert only adapter depends on both RPC Starter and DDC Starter.

- [ ] **Step 6: Run adapter contract tests.**

```bash
./mvnw -B -ntp \
  -pl egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-ddc-adapter \
  -am \
  -Dtest=DdcRpcGeneratedContractTest,DdcRpcContractDescriptorTest,DdcRpcModuleBoundaryTest \
  -Dsurefire.failIfNoSpecifiedTests=false \
  test
```

- [ ] **Step 7: Commit Task 3.**

```bash
git commit -m "feat(rpc-ddc): add adapter wire contracts"
```

---

### Task 4: Implement DDC Mapping, Client Authentication, and Typed Errors

**Files:**

- Create: `ADAPTER_MAIN/mapping/DdcCommonProtoMapper.java`
- Create: `ADAPTER_MAIN/mapping/DdcConfigProtoMapper.java`
- Create: `ADAPTER_MAIN/mapping/DdcRegistryProtoMapper.java`
- Create: `ADAPTER_MAIN/mapping/DdcManagementProtoMapper.java`
- Create: `ADAPTER_MAIN/security/DdcRpcMetadataKeys.java`
- Create: `ADAPTER_MAIN/security/DdcRpcCredential.java`
- Create: `ADAPTER_MAIN/security/DdcRpcCanonicalRequest.java`
- Create: `ADAPTER_MAIN/security/DdcRpcRequestSigner.java`
- Create: `ADAPTER_MAIN/security/DdcRpcClientInterceptorFactory.java`
- Create: `ADAPTER_MAIN/security/DdcRpcOperation.java`
- Create: `ADAPTER_MAIN/security/DdcRpcOperationResolver.java`
- Create: `ADAPTER_MAIN/mapping/DdcRpcStatusExceptionMapper.java`
- Create: `DDC_MAIN/error/DdcClientTransportException.java`
- Modify: `DDC_MAIN/service/lifecycle/DdcAckDelivery.java`
- Create: `ADAPTER_TEST/mapping/DdcConfigProtoMapperTest.java`
- Create: `ADAPTER_TEST/mapping/DdcRegistryProtoMapperTest.java`
- Create: `ADAPTER_TEST/mapping/DdcManagementProtoMapperTest.java`
- Create: `ADAPTER_TEST/security/DdcRpcRequestSignerTest.java`
- Create: `ADAPTER_TEST/security/DdcRpcClientInterceptorFactoryTest.java`
- Create: `ADAPTER_TEST/mapping/DdcRpcStatusExceptionMapperTest.java`
- Modify: `DDC_TEST/service/lifecycle/DdcAckDeliveryTest.java`

**Security and error contract:**

- Metadata keys are exactly `x-egon-ddc-access-key`, `x-egon-ddc-timestamp`, `x-egon-ddc-nonce`, `x-egon-ddc-content-sha256`, `x-egon-ddc-signature`, `x-egon-ddc-contract-version`.
- Canonical HMAC input is exactly five LF-separated lines: `v1`, full gRPC method, epoch-millis timestamp, nonce, lowercase SHA-256 of deterministic Protobuf bytes. No trailing LF. Signature is lowercase HMAC-SHA256 hex.
- Operation resolver implements the exact method-to-operation table from the approved spec, including `PUBLISH_ACK` and all management read/write operations; unknown DDC method fails closed.
- Mapper tests cover every field and enum in both directions, UTC timestamp conversion, optional values, metadata limits, unknown enum rejection and max config/message size.
- Typed error trailer uses binary key `x-egon-ddc-error-bin` and generated `DdcRpcErrorDetail`. Client restores existing DDC business exceptions/error codes; transport-only failures become `DdcClientTransportException` with `retryable`.
- `DdcAckDelivery` detects only transport-neutral and DDC business exceptions. Remove its `RestClientResponseException`/HTTP status inspection now, before deleting Spring Web from Starter.

- [ ] **Step 1: Add mapper round-trip and malformed-input tests.** Include all three Port model families, not only happy-path config pull.

- [ ] **Step 2: Add fixed HMAC vectors.** Use a fixed protobuf request, timestamp, nonce and secret; assert exact deterministic body hash, canonical bytes and signature. Add a different-map-insertion-order case proving deterministic serialization.

- [ ] **Step 3: Add status/trailer and ACK retryability tests.** Cover INVALID_ARGUMENT, NOT_FOUND, FAILED_PRECONDITION, UNAUTHENTICATED, PERMISSION_DENIED, UNAVAILABLE and INTERNAL; malformed/missing binary detail must fall back safely without exposing raw server text.

- [ ] **Step 4: Run the focused tests and confirm expected failures.**

```bash
./mvnw -B -ntp \
  -pl egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-ddc-adapter,egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-starter \
  -am \
  -Dtest=DdcConfigProtoMapperTest,DdcRegistryProtoMapperTest,DdcManagementProtoMapperTest,DdcRpcRequestSignerTest,DdcRpcClientInterceptorFactoryTest,DdcRpcStatusExceptionMapperTest,DdcAckDeliveryTest \
  -Dsurefire.failIfNoSpecifiedTests=false \
  test
```

- [ ] **Step 5: Implement mappers and validators.** Keep conversion code direct and role-specific; do not introduce reflection, Jackson-as-wire-format or a generic object mapper registry.

- [ ] **Step 6: Implement request-aware signing.** Use `RpcClientInterceptorFactory` from Task 2 so metadata is computed after the request exists and before call start. Never accept a caller-provided body hash.

- [ ] **Step 7: Implement typed error restoration and neutral transport exception.** Preserve existing `DdcManagementClientException`; add only the neutral transport exception needed by all three clients and ACK delivery.

- [ ] **Step 8: Run the focused tests and both module test suites.**

```bash
./mvnw -B -ntp \
  -pl egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-ddc-adapter,egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-starter \
  -am test
```

- [ ] **Step 9: Commit Task 4.**

```bash
git commit -m "feat(rpc-ddc): map authenticated ddc calls"
```

---

### Task 5: Implement RPC-backed DDC Clients, Registry Bridges, and AutoConfiguration

**Files:**

- Create: `ADAPTER_MAIN/autoconfigure/DdcRpcProperties.java`
- Create: `ADAPTER_MAIN/autoconfigure/DdcRpcAutoConfiguration.java`
- Create: `ADAPTER_MAIN/client/DdcRpcClientFactory.java`
- Create: `ADAPTER_MAIN/client/DdcRpcClientHandle.java`
- Create: `ADAPTER_MAIN/client/config/RpcDdcConfigClient.java`
- Create: `ADAPTER_MAIN/client/registry/RpcDdcServiceRegistryClient.java`
- Create: `ADAPTER_MAIN/client/management/RpcDdcManagementClient.java`
- Create: `ADAPTER_MAIN/registry/DdcRpcProviderRegistry.java`
- Create: `ADAPTER_MAIN/registry/DdcRpcGatewayDirectory.java`
- Create: `ADAPTER_MAIN/registry/RpcDdcRegistrySnapshotLoader.java`
- Create: `ADAPTER/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
- Modify: `DDC_MAIN/api/client/DdcManagementClient.java`
- Modify: `DDC_TEST/api/client/DdcManagementContractBoundaryTest.java`
- Create: `ADAPTER_TEST/autoconfigure/DdcRpcPropertiesTest.java`
- Create: `ADAPTER_TEST/autoconfigure/DdcRpcAutoConfigurationTest.java`
- Create: `ADAPTER_TEST/client/RpcDdcConfigClientTest.java`
- Create: `ADAPTER_TEST/client/RpcDdcServiceRegistryClientTest.java`
- Create: `ADAPTER_TEST/client/RpcDdcManagementClientTest.java`
- Create: `ADAPTER_TEST/registry/DdcRpcProviderRegistryTest.java`
- Create: `ADAPTER_TEST/registry/DdcRpcGatewayDirectoryTest.java`

**Configuration and runtime behavior:**

- Bind `egon.cola.component.ddc.rpc.target`, connect/default timeout, `round_robin`, TLS and three non-fallback credential profiles (`runtime`, `registry`, `management`). Validate target and only the credential for the capability being created.
- `DdcRpcClientFactory.configClient()`, `registryClient()` and `managementClient()` create separately owned Direct handles. Management is never a global auto-configured bean.
- `ddc.enabled=true` supplies `DdcConfigClient`; `ddc.registry.enabled=true` supplies `DdcServiceRegistryClient`, snapshot loader, `RpcProviderRegistry`, `RpcGatewayDirectory` and higher-priority DDC-backed process identity. All concrete beans use `@ConditionalOnMissingBean`.
- AutoConfiguration is ordered before DDC Starter `DdcAutoConfiguration` and `DdcRegistryAutoConfiguration`. DDC Admin with both DDC switches false creates no client and does not require a target.
- Registry subscriptions retain `initial Direct RPC + Redis Topic + periodic Direct RPC reconciliation`; V1 adds no gRPC streaming.
- Map neutral Provider registration to `DdcServiceKind.RPC_PROVIDER`; map Gateway queries to `INTERNAL_GATEWAY`; validate DDC/Gateway metadata with `ServiceInstanceMetaCodec` only at these adapter boundaries.
- Make `DdcManagementClient.findConfig` and `getScopeBindings` abstract. Update every repository fake/recording implementation returned by the compilation scan; do not keep unsupported defaults.

- [ ] **Step 1: Add failing properties, bean-condition, client and bridge tests.** Include DDC-disabled Admin context, missing target failure, profile isolation, bean override and closing behavior.

- [ ] **Step 2: Start an in-process plaintext Direct gRPC fixture in client tests.** Verify all methods of all three Java Ports reach the expected Protobuf method with complete scope/lease fields and restore response/error models.

- [ ] **Step 3: Run focused tests and confirm failures.**

```bash
./mvnw -B -ntp \
  -pl egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-ddc-adapter \
  -am \
  -Dtest=DdcRpcPropertiesTest,DdcRpcAutoConfigurationTest,RpcDdcConfigClientTest,RpcDdcServiceRegistryClientTest,RpcDdcManagementClientTest,DdcRpcProviderRegistryTest,DdcRpcGatewayDirectoryTest \
  -Dsurefire.failIfNoSpecifiedTests=false \
  test
```

- [ ] **Step 4: Implement properties, factory and three clients.** Keep deadline selection in one factory; method adapters only map, call and restore errors. Explicitly close all owned Direct handles on bean destruction.

- [ ] **Step 5: Implement Registry bridges and DDC identity provider.** Preserve current gateway snapshot revision/drain semantics and DDC instance identity equivalence without leaking DDC imports back into RPC Starter.

- [ ] **Step 6: Implement AutoConfiguration ordering and fail-fast diagnostics.** Missing required Port errors must name the Port and `top.egon:egon-cola-component-rpc-ddc-adapter`; no silent local-only fallback.

- [ ] **Step 7: Make management methods mandatory and repair test doubles.** Use:

```bash
rg -n "implements DdcManagementClient|new DdcManagementClient" \
  --glob '*.java' .
```

Every implementation must implement `findConfig` and `getScopeBindings` with real behavior or an explicit test fixture result; do not throw `UnsupportedOperationException`.

- [ ] **Step 8: Run adapter tests and DDC Starter contract tests.**

```bash
./mvnw -B -ntp \
  -pl egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-ddc-adapter,egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-starter \
  -am \
  -Dtest=DdcRpcPropertiesTest,DdcRpcAutoConfigurationTest,RpcDdcConfigClientTest,RpcDdcServiceRegistryClientTest,RpcDdcManagementClientTest,DdcRpcProviderRegistryTest,DdcRpcGatewayDirectoryTest,DdcManagementContractBoundaryTest,DdcAutoConfigurationTest,DdcRegistryAutoConfigurationTest \
  -Dsurefire.failIfNoSpecifiedTests=false \
  test
```

- [ ] **Step 9: Commit Task 5.**

```bash
git commit -m "feat(rpc-ddc): provide rpc backed ddc ports"
```

---

### Task 6: Move ConfigData Transport into the Adapter

**Files:**

- Move: `DDC_MAIN/configdata/DdcConfigDataLocationResolver.java` → `ADAPTER_MAIN/configdata/DdcConfigDataLocationResolver.java`
- Move: `DDC_MAIN/configdata/DdcConfigDataResource.java` → `ADAPTER_MAIN/configdata/DdcConfigDataResource.java`
- Move: `DDC_MAIN/configdata/DdcConfigDataLoader.java` → `ADAPTER_MAIN/configdata/DdcConfigDataLoader.java`
- Move: `DDC_MAIN/configdata/DdcConfigDataFetcher.java` → `ADAPTER_MAIN/configdata/DdcConfigDataFetcher.java`
- Move the four matching tests plus `DdcConfigDataSpringApplicationTest.java` from `DDC_TEST/configdata` to `ADAPTER_TEST/configdata`.
- Modify: `DDC_ROOT/egon-cola-platform-dynamic-config-center-starter/src/main/resources/META-INF/spring.factories`
- Create: `ADAPTER/src/main/resources/META-INF/spring.factories`
- Modify: `DDC_MAIN/autoconfigure/DdcAutoConfiguration.java`
- Modify: `DDC_MAIN/autoconfigure/DdcRegistryAutoConfiguration.java`
- Modify: `DDC_TEST/autoconfigure/DdcAutoConfigurationTest.java`
- Modify: `DDC_TEST/autoconfigure/DdcRegistryAutoConfigurationTest.java`
- Modify: `DDC_TEST/DdcPackageDocumentationTest.java`
- Modify: `DDC_TEST/DdcPlatformBoundaryTest.java`

**Bootstrap behavior:**

- Resolver/Loader keep `ddc:application.yml` and `optional:ddc:application.yml` syntax and current precedence: remote YAML above local ConfigData and below system/command-line properties.
- Fetcher binds `DdcProperties` plus `DdcRpcProperties` from bootstrap `Environment`, validates only local bootstrap keys, creates a programmatic Direct config client, pulls YAML and closes the channel in success/failure.
- Bootstrap does not require an ApplicationContext or `egon.cola.component.rpc.enabled=true`; it never reads target/TLS/credential/profile from remote DDC content.
- Non-optional connectivity/auth/config errors abort startup; optional locations continue with local configuration. Reserved-key and YAML-only validation remain unchanged.
- DDC Starter consumes Port beans but provides no default transport implementation. Its package/boundary tests no longer claim ownership of ConfigData SPI or HTTP clients.

- [ ] **Step 1: Move tests first and replace HTTP fixtures with an in-process Direct gRPC service.** Add assertions that bootstrap works without Spring beans, closes the channel, preserves optional behavior and rejects remote `egon.cola.component.ddc.rpc.*` keys.

- [ ] **Step 2: Run the moved tests and confirm package/transport failures.**

```bash
./mvnw -B -ntp \
  -pl egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-ddc-adapter \
  -am \
  -Dtest=DdcConfigDataLocationResolverTest,DdcConfigDataFetcherTest,DdcConfigDataLoaderTest,DdcConfigDataSpringApplicationTest \
  -Dsurefire.failIfNoSpecifiedTests=false \
  test
```

- [ ] **Step 3: Move the four production SPI types with `git mv`.** Update package declarations, Javadocs and package documentation. Do not duplicate the Loader/Resolver in Starter.

- [ ] **Step 4: Replace bootstrap HTTP construction with `RpcDirectClientFactory`.** Use the runtime credential profile and bootstrap timeout; use try-with-resources for the owned handle.

- [ ] **Step 5: Move the `spring.factories` registration.** Starter must no longer list ConfigData Resolver/Loader; adapter must be the sole active registration.

- [ ] **Step 6: Remove default HTTP client beans from DDC AutoConfiguration.** Keep Runtime Coordinator/Registry orchestration conditional on supplied Ports, with precise missing-adapter diagnostics. Update package and boundary tests for the new ownership.

- [ ] **Step 7: Run focused and full module tests.**

```bash
./mvnw -B -ntp \
  -pl egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-ddc-adapter,egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-starter \
  -am test
```

- [ ] **Step 8: Commit Task 6.**

```bash
git commit -m "refactor(ddc): move config data transport to rpc adapter"
```

---

### Task 7: Introduce DDC Admin Facades and RPC Providers

**Files:**

- Modify: `DDC_ROOT/egon-cola-platform-dynamic-config-center-admin/pom.xml`
- Create: `ADMIN_MAIN/service/config/DdcConfigFacade.java`
- Create: `ADMIN_MAIN/service/registry/DdcRegistryFacade.java`
- Modify: `ADMIN_MAIN/service/management/DdcManagementFacade.java`
- Create: `ADMIN_MAIN/rpc/provider/DdcConfigRpcProvider.java`
- Create: `ADMIN_MAIN/rpc/provider/DdcRegistryRpcProvider.java`
- Create: `ADMIN_MAIN/rpc/provider/DdcManagementRpcProvider.java`
- Create: `ADMIN_MAIN/rpc/provider/package-info.java`
- Create: `ADMIN_TEST/service/config/DdcConfigFacadeTest.java`
- Create: `ADMIN_TEST/service/registry/DdcRegistryFacadeTest.java`
- Modify: `ADMIN_TEST/service/management/DdcManagementFacadeTest.java`
- Create: `ADMIN_TEST/rpc/provider/DdcConfigRpcProviderTest.java`
- Create: `ADMIN_TEST/rpc/provider/DdcRegistryRpcProviderTest.java`
- Create: `ADMIN_TEST/rpc/provider/DdcManagementRpcProviderTest.java`

**Facade and Provider boundaries:**

- `DdcConfigFacade` is the only Provider-facing orchestration for config-client register/heartbeat/offline, pull and ACK; it composes existing `DdcInstanceAdminService`, `DdcConfigService`, `DdcPublishService`.
- `DdcRegistryFacade` is the only Provider-facing orchestration for service register/heartbeat/deregister and instance/catalog snapshots; it delegates `DdcServiceRegistryService`.
- `DdcManagementFacade` gains scope-binding query currently in `DdcManagementOpenApiController`, and owns all ten management operations.
- Providers implement adapter contracts and only perform Protobuf mapping, authenticated-context lookup and Facade delegation. They do not touch Repository, Redisson, transactions, Admin Entity or `ResultRecord`, and do not repeat service validation.
- This task adds provider beans but does not yet enable Admin gRPC server configuration and does not delete HTTP routes; secure activation is atomic in Task 8.

- [ ] **Step 1: Add failing Facade delegation tests.** Capture every existing Controller-to-Service call and exact trusted operator placement. Verify Facades preserve not-found/validation/error codes.

- [ ] **Step 2: Add failing Provider boundary tests.** For every RPC method, assert exact mapper call and exactly one corresponding Facade call; use reflection/ArchUnit-style source checks to reject repository/Redisson imports in the provider package.

- [ ] **Step 3: Run focused tests and confirm missing types.**

```bash
./mvnw -B -ntp \
  -pl egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin \
  -am \
  -Dtest=DdcConfigFacadeTest,DdcRegistryFacadeTest,DdcManagementFacadeTest,DdcConfigRpcProviderTest,DdcRegistryRpcProviderTest,DdcManagementRpcProviderTest \
  -Dsurefire.failIfNoSpecifiedTests=false \
  test
```

- [ ] **Step 4: Add direct RPC dependencies to Admin.** Declare both RPC Starter and adapter explicitly because Admin owns RPC Provider annotations/contracts; do not enable a DDC client or target.

- [ ] **Step 5: Implement Facades by extracting Controller orchestration.** Preserve current transactions in Services; do not move Repository logic into Facades.

- [ ] **Step 6: Implement Providers.** Use generated messages only at the boundary and adapter mappers for all conversion. requestedOperator remains untrusted until Task 8 supplies the principal.

- [ ] **Step 7: Run focused tests and the Admin suite.**

```bash
./mvnw -B -ntp \
  -pl egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin \
  -am test
```

- [ ] **Step 8: Commit Task 7.**

```bash
git commit -m "feat(ddc-admin): expose control plane facades via rpc"
```

---

### Task 8: Migrate DDC HMAC Security and Activate the RPC Server

**Files:**

- Move reusable security types from `ADMIN_MAIN/security/openapi` to `ADMIN_MAIN/security/rpc`: `DdcHmacCredential`, `DdcHmacCredentialRegistry`, `DdcNonceCache`, `DdcNonceStore`, `RedisDdcNonceStore`, `InMemoryDdcNonceStore`, `DdcServicePrincipal`.
- Create: `ADMIN_MAIN/security/rpc/DdcRpcServerInterceptor.java`
- Create: `ADMIN_MAIN/security/rpc/DdcRpcScopeExtractor.java`
- Create: `ADMIN_MAIN/security/rpc/DdcRpcProviderExceptionMapper.java`
- Create: `ADMIN_MAIN/security/rpc/DdcRpcSecurityConfiguration.java`
- Create: `ADMIN_MAIN/security/rpc/package-info.java`
- Modify temporarily: `ADMIN_MAIN/security/openapi/DdcOpenApiHmacFilter.java` imports moved shared types until Task 11 deletes the filter.
- Modify: `ADMIN_MAIN/config/DdcAdminProperties.java`
- Modify: `ADMIN_MAIN/config/DdcAdminSecurityPropertiesValidator.java`
- Modify: `ADMIN_MAIN/config/DdcAdminRedisConfig.java`
- Modify: `DDC_ROOT/egon-cola-platform-dynamic-config-center-admin/src/main/resources/application.yml`
- Modify: `DDC_ROOT/egon-cola-platform-dynamic-config-center-admin/src/main/resources/application-local.yml`
- Modify: `DDC_ROOT/egon-cola-platform-dynamic-config-center-admin/src/main/resources/application-test.yml`
- Move/update: `ADMIN_TEST/security/openapi/DdcHmacScopeTest.java` → `ADMIN_TEST/security/rpc/DdcRpcHmacScopeTest.java`
- Move/update: `ADMIN_TEST/security/openapi/RedisDdcNonceStoreTest.java` → `ADMIN_TEST/security/rpc/RedisDdcNonceStoreTest.java`
- Create: `ADMIN_TEST/security/rpc/DdcRpcServerInterceptorTest.java`
- Create: `ADMIN_TEST/security/rpc/DdcRpcProviderExceptionMapperTest.java`
- Modify: `ADMIN_TEST/config/DdcAdminSecurityPropertiesTest.java`
- Modify: `ADMIN_TEST/DdcAdminContextSmokeTest.java`

**Server security behavior:**

- Server interceptor buffers the single unary request long enough to compute deterministic bytes, validates metadata format, credential, clock window, Redis nonce, body hash, signature, method operation and extracted scope, then invokes the delegate listener inside a gRPC `Context` containing `DdcServicePrincipal`.
- Authentication failure closes the call before Provider/Facade invocation. Unknown method/scope fails closed. Nonce consumption is shared Redis and atomic across Admin nodes.
- `InMemoryDdcNonceStore` is constructible only through explicit test bean override. Executable Admin with signature enabled and no Redis store fails startup.
- `DdcRpcProviderExceptionMapper` maps DDC categories to the approved gRPC statuses and attaches `DdcRpcErrorDetail` to `x-egon-ddc-error-bin`; unknown errors are logged with invocation metadata but return sanitized INTERNAL.
- Provider operator is always `DdcServicePrincipal.auditOperator(requestedOperator)`. A request field cannot replace credential identity.
- Admin configuration enables RPC provider port 19080, consumer false, registration mode DISABLED, DDC config/registry clients false. It does not configure `ddc.rpc.target`.
- Rename the Admin nested security model from `DdcAdminProperties.Openapi` to `DdcAdminProperties.Rpc`. Active keys are `egon.cola.component.ddc.admin.rpc.signature-enabled`, `allowed-clock-skew-seconds`, `nonce-cache-max-size` and `credentials`; no `admin.openapi.*` key survives Task 11.

- [ ] **Step 1: Port security tests before production moves.** Cover missing/invalid headers, bad hash/signature, expired timestamp, nonce replay, wrong operation, scope mismatch, unknown method, principal context and requestedOperator audit formatting. Assert Facade mocks have zero interactions on every rejection.

- [ ] **Step 2: Add typed server error tests.** Verify exact status/detail code/retryable values and that Secrets/server stack traces never appear in description or trailers.

- [ ] **Step 3: Run focused tests and confirm failures.**

```bash
./mvnw -B -ntp \
  -pl egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin \
  -am \
  -Dtest=DdcRpcHmacScopeTest,DdcRpcServerInterceptorTest,DdcRpcProviderExceptionMapperTest,RedisDdcNonceStoreTest,DdcAdminSecurityPropertiesTest,DdcAdminContextSmokeTest \
  -Dsurefire.failIfNoSpecifiedTests=false \
  test
```

- [ ] **Step 4: Move shared HMAC types and implement RPC authentication.** Reuse credential/scope/nonce semantics, not Servlet canonical-path logic. Configure ordered server interceptors through RPC Starter's extension list.

- [ ] **Step 5: Implement provider error mapping and principal use.** Update all three Providers to read the authenticated principal from gRPC Context and derive operator server-side.

- [ ] **Step 6: Activate Admin RPC server safely.** Add the exact local config from the spec. Context test must assert `RpcProviderLifecycle` exists, no `RpcProviderRegistry`/lease is required in DISABLED mode, and no `DdcConfigClient`, `DdcServiceRegistryClient` or Direct client handle points back to Admin.

- [ ] **Step 7: Run Admin security/provider tests and context suite.**

```bash
./mvnw -B -ntp \
  -pl egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin \
  -am test
```

- [ ] **Step 8: Commit Task 8.**

```bash
git commit -m "feat(ddc-admin): secure direct rpc providers"
```

---

### Task 9: Harden DDC Admin for Active-Active Operation

**Files:**

- Modify: `ADMIN_MAIN/repository/DdcPublishAckRepository.java`
- Modify: `ADMIN_MAIN/service/publish/DdcPublishStateTransitionService.java`
- Modify as required by failing concurrency tests only: `ADMIN_MAIN/service/publish/DdcPublishService.java`, `DdcPendingPublishDispatcher.java`, `PublishStartupRecovery.java`, `PublishTimeoutScanner.java`, `PublishCompletionWaiterRegistry.java`.
- Modify as required by failing lease tests only: `ADMIN_MAIN/service/lease/DdcLeaseExpiryScanner.java` and `ADMIN_MAIN/service/metadata/DdcScopeGate.java`.
- Create: `ADMIN_TEST/integration/DdcActiveActiveAdminIT.java`
- Create: `ADMIN_TEST/repository/DdcPublishAckLockTest.java`
- Modify: `ADMIN_TEST/service/publish/DdcAckServiceTest.java`
- Modify: `ADMIN_TEST/service/publish/DdcPublishDispatchConsistencyTest.java`
- Modify: `ADMIN_TEST/service/publish/PublishStartupRecoveryTest.java`
- Modify: `ADMIN_TEST/service/publish/DdcPublishTimeoutScannerTest.java`
- Modify: `DDC_ROOT/egon-cola-platform-dynamic-config-center-admin/pom.xml` only if the existing Testcontainers dependency needs the managed PostgreSQL test module.

**Distributed correctness:**

- Add `@Lock(PESSIMISTIC_WRITE)` query by changeId, instanceId and leaseId. ACK state transition uses the locked row inside the existing transaction; no new table/column/index/Flyway file.
- Local `PublishResourceLockRegistry` and `PublishCompletionWaiterRegistry` remain optimizations. Publication correctness comes from DB config/task locks and status transitions; waiters poll PostgreSQL so ACK handled by node B completes a wait on node A.
- All nodes may run lease expiry, publish timeout and startup recovery. Repeated scans must be idempotent through Redis locks, leaseId conditional delete, DB conditional update and changeId/fingerprint keys.
- `DdcScopeGate` keeps five-second local TTL. Cross-node invalidation is explicitly eventual; test node B rereads PostgreSQL after the window, with no new Redis key/topic.
- Test two independently created Admin ApplicationContexts/gRPC ports against the same PostgreSQL container/schema and Redis container/namespace. Do not describe a single context or mocks as Active-Active proof.

- [ ] **Step 1: Add the ACK lock repository test.** Start with the exact conflicting ACK row and two transactions; assert one locks/commits before the other observes the final state.

- [ ] **Step 2: Add the two-context Active-Active integration test.** Cover: register A/heartbeat+deregister B; pull A/ACK B/wait A; same-changeId publish idempotency; different-changeId single active task; conflicting ACK determinism; concurrent scheduler scans; nonce A then replay B; scope TTL expiry on B; Direct channel failover from A to B; no sticky session.

- [ ] **Step 3: Run the new tests and record exact failures before changing services.**

```bash
./mvnw -B -ntp \
  -pl egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin \
  -am \
  -Dtest=DdcPublishAckLockTest,DdcAckServiceTest,DdcPublishDispatchConsistencyTest,PublishStartupRecoveryTest,DdcPublishTimeoutScannerTest \
  -Dsurefire.failIfNoSpecifiedTests=false \
  test
```

- [ ] **Step 4: Implement the smallest concurrency fixes.** Add the locked repository method and route ACK mutations through it. Change scheduler/service code only where the new tests expose a real local-state correctness dependency; do not add leader election.

- [ ] **Step 5: Run unit/concurrency tests, then the Active-Active IT.** Configure the IT under an explicit Failsafe profile if its containers make the default unit suite too slow.

```bash
./mvnw -B -ntp \
  -pl egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin \
  -am test
./mvnw -B -ntp \
  -pl egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin \
  -am \
  -Pddc-active-active \
  -Dit.test=DdcActiveActiveAdminIT \
  verify
```

- [ ] **Step 6: Prove no schema migration was added.**

```bash
git diff --name-only HEAD -- \
  egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin/src/main/resources/db
```

Expected: no output.

- [ ] **Step 7: Commit Task 9.**

```bash
git commit -m "fix(ddc-admin): make rpc control plane active active safe"
```

---

### Task 10: Migrate Gateway, DDC Test, IdP, RBAC3, and RPC Process Consumers

**Files:**

- Modify: `DDC_ROOT/egon-cola-platform-dynamic-config-center-test/pom.xml`
- Modify: `DDC_ROOT/egon-cola-platform-dynamic-config-center-test/src/main/resources/application.yml`
- Modify: `GATEWAY_ROOT/egon-cola-platform-gateway-admin/pom.xml`
- Modify: `GATEWAY_ROOT/egon-cola-platform-gateway-admin/src/main/java/top/egon/cola/component/gateway/admin/GatewayAdminConfiguration.java`
- Modify: `GATEWAY_ROOT/egon-cola-platform-gateway-admin/src/main/resources/application.yml`
- Modify: `GATEWAY_ROOT/egon-cola-platform-gateway-admin/src/main/resources/application-local.yml`
- Modify: `GATEWAY_ROOT/egon-cola-platform-gateway-admin/src/test/java/top/egon/cola/component/gateway/admin/GatewayAdminConfigurationTest.java`
- Modify: `GATEWAY_ROOT/egon-cola-platform-gateway-admin/src/test/java/top/egon/cola/component/gateway/admin/GatewayAdminApplicationConfigurationTest.java`
- Modify: `GATEWAY_ROOT/egon-cola-platform-gateway-engine/pom.xml` and `src/main/resources/application.yml`.
- Modify: `GATEWAY_ROOT/egon-cola-platform-gateway-starter/pom.xml` and focused starter AutoConfiguration tests.
- Modify POM/application YAML under `GATEWAY_ROOT/egon-cola-platform-gateway-test` for every test application returned by the DDC-property scan.
- Modify: `egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-admin/pom.xml`, `src/main/resources/application.yml`, `application-local.yml` and DDC context tests.
- Modify: `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/pom.xml`, `src/main/resources/application.yml`, `application-local.yml`, `GatewayDdcConfigurationTest.java`, `Rbac3AdminApplicationContextTest.java`.
- Modify under `RPC_ROOT/egon-cola-component-rpc-test`: provider/consumer application POMs/YAML and `RpcMockGatewayApplication.java`, `RpcProcessIT.java`.

**Composition behavior:**

- Gateway Admin removes manual `HttpDdcManagementClient`, endpoint/access-key/secret/connect/read/TLS fields. With `gateway.admin.ddc.enabled=true`, one composition bean calls `DdcRpcClientFactory.managementClient()` and exposes its `DdcManagementClient`; Gateway publish timeout and target biz/app remain Gateway-owned.
- Gateway Admin management Direct RPC must work when Gateway data plane is unavailable.
- Gateway Engine and executable/test applications include adapter at the composition root. `DdcProviderServiceRegistryAdapter`, `RpcGatewaySlotRuntime`, HTTP_PROVIDER runtime and business code keep depending on DDC Ports, not concrete RPC client classes.
- Gateway Starter keeps its existing RPC optionality by declaring adapter optional. Every executable that enables DDC adds adapter explicitly; do not rely on an optional transitive dependency.
- IdP and RBAC3 keep DDC annotations/appliers/runtime behavior. Adapter is present on enabled paths; `ddc.enabled=false`/`ddc.registry.enabled=false` creates no client and does not require target.
- RPC process topology replaces HTTP registry setup/properties with adapter gRPC target/credentials and keeps tests distinguishing DDC Direct control plane from Gateway business data plane.

- [ ] **Step 1: Add/adjust composition tests before POM/config changes.** Gateway Admin test must assert management client originates from `DdcRpcClientFactory`; DDC-disabled RBAC3 test asserts no client; enabled IdP/Gateway tests assert RPC-backed Ports and no HTTP implementation.

- [ ] **Step 2: Run the focused tests and capture missing adapter/property failures.**

```bash
./mvnw -B -ntp \
  -pl egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin,egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-admin,egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin \
  -am \
  -Dtest=GatewayAdminConfigurationTest,GatewayAdminApplicationConfigurationTest,IdpDdcPolicyConfigurationTest,GatewayDdcConfigurationTest,Rbac3AdminApplicationContextTest \
  -Dsurefire.failIfNoSpecifiedTests=false \
  test
```

- [ ] **Step 3: Migrate Gateway Admin composition.** Retain the `DdcManagementClient` Port injection in `GatewayDdcRulePublisher`, release coordinator and projection services; only replace construction/configuration.

- [ ] **Step 4: Add adapter dependencies at actual composition roots.** Keep Engine/Provider Runtime libraries Port-oriented. For optional Gateway Starter, add explicit adapter dependency to each executable/test app that enables DDC.

- [ ] **Step 5: Replace all active DDC HTTP properties with RPC properties.** Use the exact scan and re-run until only historical specs remain:

```bash
rg -n "ddc\.admin\.endpoint|gateway\.admin\.ddc\.endpoint|/api/v1/ddc/openapi" \
  egon-cola-components egon-cola-platforms \
  --glob 'pom.xml' --glob '*.java' --glob '*.yml' --glob '*.yaml' \
  --glob '*.properties' --glob '!**/target/**'
```

Use `dns:///ddc-admin:19080`, `round_robin`, local TLS values and the correct runtime/registry/management credential profile. Never put credentials in tracked defaults; retain environment-variable placeholders.

- [ ] **Step 6: Migrate RPC process/live test topology.** `RpcMockGatewayApplication` and `RpcProcessIT` use adapter contracts/clients, expose Admin gRPC 19080, and assert DDC direct calls plus Gateway-routed business calls separately.

- [ ] **Step 7: Run affected module suites.**

```bash
./mvnw -B -ntp \
  -pl egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-test,egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin,egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-engine,egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-starter,egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-admin,egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin \
  -am test
```

If RBAC3 fails on a pre-existing unrelated legacy compile issue, record the exact failure and continue only after proving no changed DDC/RPC type is involved; do not repair unrelated RBAC3 code in this task.

- [ ] **Step 8: Commit Task 10.**

```bash
git commit -m "refactor(platform): route ddc consumers through direct rpc"
```

---

### Task 11: Remove the Machine HTTP Stack and Legacy Properties

**Files:**

- Delete: `ADMIN_MAIN/controller/DdcOpenApiController.java`
- Delete: `ADMIN_MAIN/controller/DdcRegistryOpenApiController.java`
- Delete: `ADMIN_MAIN/controller/DdcManagementOpenApiController.java`
- Delete: `ADMIN_MAIN/security/openapi/DdcCachedBodyHttpServletRequest.java`
- Delete: `ADMIN_MAIN/security/openapi/DdcOpenApiHmacFilter.java`
- Delete: `ADMIN_MAIN/security/openapi/DdcSecurityFilterRegistration.java`
- Modify: `ADMIN_MAIN/security/management/DdcAdminSecurityConfiguration.java`
- Delete: `ADMIN_TEST/controller/DdcOpenApiControllerTest.java`
- Delete: `ADMIN_TEST/controller/DdcRegistryOpenApiControllerTest.java`
- Delete: `ADMIN_TEST/controller/DdcManagementOpenApiControllerTest.java`
- Delete: `ADMIN_TEST/security/openapi/DdcOpenApiHmacFilterTest.java`
- Modify: `ADMIN_TEST/security/management/DdcAdminSecurityIntegrationTest.java`
- Delete: `DDC_MAIN/client/config/HttpDdcConfigClient.java`
- Delete: `DDC_MAIN/client/registry/HttpDdcServiceRegistryClient.java`
- Delete: `DDC_MAIN/client/management/HttpDdcManagementClient.java`
- Delete: all Java files in `DDC_MAIN/client/http` and package docs for now-empty `client`, `client/config`, `client/registry`, `client/management`.
- Delete: `DDC_MAIN/model/client/DdcClientTransportSecurity.java`
- Delete: `DDC_MAIN/model/client/DdcManagementClientProperties.java` and package doc if the package becomes empty.
- Delete: `DDC_MAIN/error/http/DdcOpenApiRequestException.java` and package doc.
- Modify: `DDC_MAIN/autoconfigure/properties/DdcProperties.java`
- Modify: `DDC_ROOT/egon-cola-platform-dynamic-config-center-starter/pom.xml`
- Delete HTTP client/signer/factory/property tests under `DDC_TEST/client` and `DDC_TEST/model/client`.
- Modify: `DDC_TEST/DdcPackageDocumentationTest.java`, `DdcPlatformBoundaryTest.java`, `autoconfigure/properties/DdcPropertiesTest.java`.

**Deletion boundary:**

- Delete only machine OpenAPI. Keep all `/api/v1/ddc/**` human Admin REST controllers, JWT/RBAC, login/bootstrap, Actuator and Admin Web resources.
- Move/retain shared HMAC credential/nonce/principal types in `security.rpc`; do not delete them with the Servlet filter.
- Remove `DdcProperties.Admin` and all HTTP URI/connect/read/TLS/access/secret validation. Adapter RPC properties are the only machine client transport configuration.
- Remove `spring-web` from DDC Starter POM after `jdeps`/`rg` proves no main-source use; retain Jackson/JSR310 for Redis/model/YAML behavior.
- Deleted HTTP tests are replaced by RPC tests from Tasks 4, 5, 6 and 8. Scope/nonce/operator/error coverage must not decrease.

- [ ] **Step 1: Change boundary/security tests to expect HTTP absence.** Assert `/api/v1/ddc/openapi/**` is not mapped and human Admin REST still follows existing JWT rules.

- [ ] **Step 2: Run tests and confirm they fail while old controllers/filter remain.**

```bash
./mvnw -B -ntp \
  -pl egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin,egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-starter \
  -am \
  -Dtest=DdcAdminSecurityIntegrationTest,DdcPlatformBoundaryTest,DdcPropertiesTest \
  -Dsurefire.failIfNoSpecifiedTests=false \
  test
```

- [ ] **Step 3: Delete Controllers, Servlet transport and their tests.** Remove the OpenAPI matcher/filter registration from Spring Security without weakening human route authentication.

- [ ] **Step 4: Delete Starter HTTP clients/models/errors and old tests.** Update package docs/boundary list for removed empty packages.

- [ ] **Step 5: Remove legacy properties and Spring Web dependency.** Re-run main-source imports before POM removal:

```bash
rg -n "org\.springframework\.web|RestClient|DdcClientTransportSecurity|DdcManagementClientProperties" \
  egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-starter/src/main/java
```

Expected: no output.

- [ ] **Step 6: Run DDC Starter/Admin/adapter suites and route scan.**

```bash
./mvnw -B -ntp \
  -pl egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-ddc-adapter,egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-starter,egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin \
  -am test
rg -n "HttpDdc|/api/v1/ddc/openapi|DdcOpenApiHmacFilter|DdcCanonicalRequest|ddc\.admin\.endpoint|gateway\.admin\.ddc\.endpoint" \
  egon-cola-components egon-cola-platforms \
  --glob '!**/target/**' --glob '!**/docs/superpowers/specs/**' --glob '!**/docs/superpowers/plans/**'
```

Expected: active production/test/config source scan prints nothing.

- [ ] **Step 7: Commit Task 11.**

```bash
git commit -m "refactor(ddc): remove machine http transport"
```

---

### Task 12: Update Deployment Contracts, Documentation, and Run Final Verification

**Files:**

- Modify: `RPC_ROOT/README.md`
- Modify: `RPC_ROOT/README.zh-CN.md`
- Modify: `DDC_ROOT/README.md`
- Modify: `DDC_ROOT/README.zh-CN.md`
- Modify: `GATEWAY_ROOT/README.md`
- Modify: `GATEWAY_ROOT/README.zh-CN.md`
- Modify: `GATEWAY_ROOT/deployment/README.md`
- Modify: `GATEWAY_ROOT/deployment/README.zh-CN.md`
- Modify: `GATEWAY_ROOT/deployment/compose.yml`
- Modify: `GATEWAY_ROOT/deployment/compose.demo.yml`
- Modify: `GATEWAY_ROOT/deployment/compose.ha.yml`
- Modify: `GATEWAY_ROOT/deployment/compose.ha-mtls.yml`
- Modify: `GATEWAY_ROOT/deployment/compose.mtls.yml`
- Modify: `GATEWAY_ROOT/deployment/haproxy.cfg` only if it currently routes DDC machine traffic; keep HTTP readiness and add an HTTP/2-capable gRPC backend path without conflating the ports.
- Modify: `GATEWAY_ROOT/egon-cola-platform-gateway-test/egon-cola-platform-gateway-test-suite/src/test/java/top/egon/cola/component/gateway/test/deployment/GatewayComposeConfigurationTest.java`
- Modify: `egon-cola-platforms/egon-cola-platform-rbac3/docs/operations-runbook.md`
- Modify any active archetype/example POM/YAML returned by the final adapter/legacy-property scan; historical specs are read-only records and are not rewritten.

**Documentation and deployment contract:**

- Show the final dependency direction: `rpc-starter <- rpc-ddc-adapter -> ddc-starter`, and Admin/Gateway/IdP/RBAC composition roots above adapter.
- Document DDC as the sole bootstrap exception, external logical target, round_robin, RPC 19080, HTTP Admin Web/Actuator port, TLS/credential profiles, registration DISABLED and Active-Active shared-state assumptions.
- Document that non-DDC managed services still require DDC, ordinary RPC still uses Gateway, Redis subscription remains, and no gRPC streaming/sticky session/leader election is introduced.
- Compose/HA examples expose Admin gRPC 19080, use service DNS rather than Pod/container IP in clients, preserve HTTP readiness, and do not commit real Secrets.
- Deployment order is new DDC Admin gRPC endpoint first, then same-version consumers; source contains no old compatibility endpoint.

- [ ] **Step 1: Update deployment contract tests first.** Assert the DDC Admin service exposes 19080, consumers use `dns:///ddc-admin:19080`, no old endpoint property exists, HTTP health remains, and HA examples have multiple Admin backends sharing PostgreSQL/Redis.

- [ ] **Step 2: Run deployment tests and confirm expected configuration failures.**

```bash
./mvnw -B -ntp \
  -pl egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-test/egon-cola-platform-gateway-test-suite \
  -am \
  -Dtest=GatewayComposeConfigurationTest \
  -Dsurefire.failIfNoSpecifiedTests=false \
  test
```

- [ ] **Step 3: Update active README, runbook, compose and examples.** Include a migration table from removed HTTP properties to `egon.cola.component.ddc.rpc.*`; explicitly mark runtime/registry/management credentials as separate and environment-injected.

- [ ] **Step 4: Run dependency and architecture verification.**

```bash
./mvnw -B -ntp \
  -pl egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter \
  dependency:tree \
  -Dincludes=top.egon:egon-cola-platform-dynamic-config-center-starter,top.egon:egon-cola-component-rpc-ddc-adapter
rg -n "top\.egon\.cola\.component\.ddc" \
  egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/{pom.xml,src}
rg -n "egon-cola-component-rpc-ddc-adapter" \
  --glob 'pom.xml' .
```

Expected: first two checks show RPC Starter is clean; adapter consumers are only approved composition roots and adapter itself is the sole dual-dependency module.

- [ ] **Step 5: Run all targeted module suites.**

```bash
./mvnw -B -ntp \
  -pl egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter,egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-ddc-adapter,egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-test,egon-cola-platforms/egon-cola-platform-dynamic-config-center,egon-cola-platforms/egon-cola-platform-gateway,egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-admin,egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin \
  -am test
```

- [ ] **Step 6: Run integration verification without starting real projects.**

```bash
./mvnw -B -ntp clean integration-test
```

Record exact module/test counts and any environment-gated skips. Do not claim external DNS/LB, Redis Sentinel/Cluster or PostgreSQL HA from this Maven result.

- [ ] **Step 7: Run final active-source residual scans.**

```bash
rg -n "HttpDdc|/api/v1/ddc/openapi|ddc\.admin\.endpoint|gateway\.admin\.ddc\.endpoint|DdcOpenApiHmacFilter|DdcCanonicalRequest" \
  egon-cola-components egon-cola-platforms egon-cola-archetypes \
  --glob '!**/target/**' --glob '!**/docs/superpowers/specs/**' --glob '!**/docs/superpowers/plans/**'
rg -n "top\.egon\.cola\.component\.ddc" \
  egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter
git diff --name-only 9f2963f6..HEAD -- \
  '*/src/main/resources/db/*'
git diff --name-only HEAD -- \
  '*/src/main/resources/db/*'
git diff --check
git status --short
```

Expected: legacy/source imports print nothing; no new DB migration exists; diff check passes; status contains only intentional task files before commit.

- [ ] **Step 8: Commit Task 12.**

```bash
git commit -m "docs(ddc): document direct rpc control plane"
```

---

## Completion Checklist

- [ ] RPC Starter compiles/tests with zero DDC or adapter dependency/import.
- [ ] `rpc-ddc-adapter` is present in RPC reactor/BOM and is the only RPC-DDC integration leaf.
- [ ] All three DDC machine contracts are unary Protobuf/gRPC and all Java Ports use RPC adapters.
- [ ] ConfigData works before ApplicationContext and closes its bootstrap channel.
- [ ] DDC Admin exposes three Facade-backed Providers on 19080, uses shared nonce security and does not register/discover itself.
- [ ] Active-Active tests cover cross-node lease, ACK, publish, nonce, scope TTL and backend failover with shared PostgreSQL/Redis.
- [ ] Gateway Admin, Engine/test compositions, IdP, RBAC3, DDC Test and RPC process tests use adapter/local gRPC properties where enabled.
- [ ] Old Controllers, Servlet HMAC filter, HTTP clients, HTTP transport models/properties/tests and routes are deleted.
- [ ] Admin Web, human REST, Redis topics/keys, DDC domain Ports/models and database/Flyway schema remain intact.
- [ ] Ordinary `@EgonRpcReference` still routes through Gateway; Direct cannot be selected from business annotations.
- [ ] Targeted tests, Active-Active IT, root integration-test, dependency checks and residual scans have recorded results.
- [ ] Every implementation task is committed separately with no unrelated worktree changes.
