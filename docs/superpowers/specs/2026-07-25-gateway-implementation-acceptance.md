# Egon-COLA Gateway 全量实现验收追踪

状态：代码已实现，待用户验收

基线：`2026-07-24-gateway-component-design.md`

范围：GWS-01～GWS-13、对应 15 份实施 Plan、Gateway Component 代码、RPC/DDC
必要扩展、Admin Web、真实测试应用与部署资产。

## 1. 文档目的

本文不重新定义 Gateway 需求或技术路线，只负责建立以下闭环：

1. 总览 Spec 与原始 29 章能力的对应关系；
2. GWS-01～GWS-13 与实施 Plan 的对应关系；
3. 每个子 Spec 的代码落点；
4. 每个能力的自动化验证证据；
5. 本次未执行的真实运行验证边界；
6. 用户后续验收时可逐项检查的入口。

“已实现”表示 Spec 中定义的 V1 代码和自动化验证入口已经落库，不表示用户已经完成
验收，也不把未执行的容器、浏览器、性能或高可用测试描述为已经通过。

## 2. 交付物清单

### 2.1 设计与计划

| 交付物 | 数量 | 状态 |
|---|---:|---|
| Gateway 总览 Spec | 1 | 已实现，待用户验收 |
| 子 Spec 索引 | 1 | 已实现，待用户验收 |
| GWS-01～GWS-13 子 Spec | 13 | 已实现，待用户验收 |
| 全量实现验收追踪 | 1 | 已完成，待用户验收 |
| 实施 Plan | 15 | 已执行 |

GWS-02 因同时修改 DDC Runtime、DDC Management 和 RPC Contract，被拆成 GWS-02A、
GWS-02B、GWS-02C 三份 Plan；其余子 Spec 各有一份 Plan。

### 2.2 Gateway 产品模块

| 模块 | 职责 | 对外使用边界 |
|---|---|---|
| `gateway-contract` | 跨进程身份、定义、规则、Trace、调用事件契约 | 稳定契约 |
| `gateway-core` | 框架无关路由、上下文、Provider 与安全 SPI | Engine 内核基础 |
| `gateway-engine` | HTTP/RPC 数据面、发现、治理、安全、规则激活、Kafka | 独立运行，不进 BOM |
| `gateway-admin` | 控制面、持久化、发布、投影、审计、调用事件消费 | 独立运行，不进 BOM |
| `gateway-starter` | 下游 HTTP/RPC 接口定义发现与上报 | BOM 导出 |
| `gateway-provider-runtime` | HTTP Provider DDC 租约注册 | BOM 导出 |
| `gateway-test` | 真实 HTTP/RPC 应用、进程与容器测试套件 | 测试使用 |
| `gateway-admin-web` | React 管理平台 | 独立前端制品 |

根 Gateway 聚合 POM 可以管理全部内部模块，但全局 Components BOM 只导出下游需要的
`gateway-starter` 和 `gateway-provider-runtime`，不把 Engine、Admin 或 Core 暴露成
普通业务依赖。

### 2.3 依赖 Component 扩展

| Component | 扩展 |
|---|---|
| DDC | `HTTP_PROVIDER`、组合式 Config Applier、版本/checksum 约束、周期校准 |
| DDC | 独立 Management Client、HMAC 机器接口、配置容量保护、Registry 投影 |
| RPC | 共享已校验 Provider Registry、只读 Contract Catalog、Descriptor Snapshot |
| RPC | 可控 `gateway.*` Provider Metadata、`INTERNAL_GATEWAY` 单 Slot 发现契约 |

没有引入 Nacos、Dubbo 或 Spring Cloud Gateway；Engine 的 Provider 地址只能来自 DDC
Provider Directory，Admin Route/Rule 不接受静态 Provider URL。

## 3. 最终技术路线

### 3.1 数据面

- HTTP：Reactor Netty 双 Listener，自研不可变 Route Index 与阶段化执行链；
- RPC：Egon RPC 的 gRPC/Protobuf Unary 协议，Engine 提供内部动态 Gateway Slot；
- HTTP→RPC：只使用 Rule 中的标准 Protobuf Descriptor 动态映射，不根据客户端输入
  加载 Java Class；
- PUBLIC/INTERNAL：由物理 Listener 注入可信 `AccessZone`，忽略客户端伪造 Header；
- Provider：Engine 通过 DDC 发现 HTTP/RPC Provider，不读取 Admin 数据库；
- 路由、Provider 筛选、健康和负载均衡全部在 Engine 执行；
- 停机：先停止接受新流量，再有界等待 HTTP 存量请求和 RPC/Channel 排空。

### 3.2 控制面

- Admin 保存三级接口目录、Operation 详情、Gateway Group、Draft、Release、Target、
  Audit 和运行投影；
- Admin 编译不可变 Rule Snapshot，通过 DDC Management Client 发布；
- DDC 自己负责数据库事实、Redis 状态、Pub/Sub、精确 Target ACK；
- Engine 完成校验、编译、Provider 订阅准备、LKG Staging 后才原子激活并 ACK；
- 大规则使用同版本 Activation Manifest + 有界 Chunk，绝不部分激活；
- 回滚生成新 Release，不修改历史 Release。

### 3.3 Provider 接入与调用事件

- Starter 只发现并上报接口定义；
- HTTP Controller 对应接口组，一组 Controller 对应实体域，一组实体域对应业务域；
- RPC Service/Contract 对应接口组，并上报标准 FileDescriptorSet；
- Operation 包含 Method/Path、参数、Schema、响应、标签、Owner、生命周期、
  `externalAccessible`、Provider Service Identity 与幂等语义；
- HTTP Provider Runtime 和 RPC Component 分别注册 `HTTP_PROVIDER`、`RPC_PROVIDER`；
- Starter 不拦截调用、不注册 Provider、不发送 Kafka；
- Engine 每次业务调用完成后发送有界、Best Effort 的 Kafka 调用事件；
- Admin Consumer 幂等投影 Trace 摘要和聚合。

### 3.4 安全边界

- `externalAccessible=false` 默认只允许 INTERNAL Route；
- Gateway 层提供 Credential Extractor、Authentication Provider、
  Authorization Provider、Identity Mapper 四类扩展点；
- Rule 引用缺失能力时拒绝激活，运行时异常、超时和拒绝全部 Fail Closed；
- 本项目不实现业务系统自身的用户、角色、权限数据源；
- 入站保留身份 Header 先清洗，只允许受信 Provider 重新映射；
- Trace 优先采用调用方生成的合法值，缺失或非法时由 Engine 生成。

## 4. GWS 实现追踪

### 4.1 GWS-01 工程与公共契约

实现：

- 建立 Contract、Core、Engine、Admin、Starter、Provider Runtime、Test 聚合结构；
- 公共版本、Operation Key、错误结果、Trace Context 和 Engine 生命周期状态机；
- ArchUnit/依赖边界测试约束 Contract/Core 不依赖 Spring 或运行模块；
- BOM 只导出 Starter 与 Provider Runtime。

主要证据：

- `egon-cola-component-gateway/pom.xml`
- `GatewayOperationKey`、`GatewayResult`、`GatewayContext`
- `GatewayEngineLifecycle`
- `GatewayContractBoundaryTest`、`GatewayCoreBoundaryTest`

### 4.2 GWS-02 RPC/DDC 扩展

实现：

- DDC 服务类型增加 `HTTP_PROVIDER`；
- Config Applier 支持 exact、longest-prefix、fallback，并在初始化后冻结；
- 同版本不同 checksum 拒绝，Pub/Sub 与周期刷新共用原子版本逻辑；
- 独立 DDC Management Client 和 HMAC Canonical Request；
- DDC Admin 提供配置、发布、实例与 Registry 机器接口；
- RPC Provider Scanner 结果共享，暴露只读 Contract Catalog；
- 标准 FileDescriptorSet 传递依赖闭包、确定性排序与 SHA-256；
- `gateway.*` Metadata Contributor 合并时保护稳定字段。

主要证据：

- `dynamic-config-center-management-client`
- `DdcRefreshService`、`DefaultDdcConfigApplierRegistry`
- `DdcManagementOpenApiController`、`DdcRegistryOpenApiController`
- `DefaultRpcContractCatalog`、`RpcProviderMethodRegistry`
- 对应 DDC/RPC 单元、序列化和边界测试。

### 4.3 GWS-03 Engine Core 与 HTTP 数据面

实现：

- Host/Method/Path 标准化与不可变 Route Index；
- PUBLIC/INTERNAL 独立 Reactor Netty Listener；
- Header 数量/大小、默认 2 MiB Body、连接空闲和上游连接池上限；
- Hop-by-Hop、可信身份和 Trace Header 处理；
- 路由、过滤、Provider 选择、上游调用和统一错误映射；
- 停机时拒绝新业务请求，并按 `drainTimeout` 等待存量请求。

主要证据：

- `GatewayHttpServer`、`GatewayHttpListener`
- `HttpRouteCompiler`、`DefaultGatewayHttpDataPlaneHandler`
- `ReactorNettyHttpUpstreamAdapter`
- `GatewayHttpServerTest`、`HttpRequestNormalizerTest`
- `DefaultGatewayHttpDataPlaneHandlerTraceTest`

### 4.4 GWS-04 Egon RPC 数据面

实现：

- INTERNAL gRPC Listener 和动态 `HandlerRegistry`；
- 每次调用捕获不可变 `RpcMethodIndex`；
- Protobuf 原始字节 Unary 转发；
- Provider Channel 按 Lease 身份缓存、替换和有界排空；
- Deadline、Cancellation、Metadata、Status/Trailer 传播；
- HTTP JSON/Path/Query 到 DynamicMessage，再到 RPC Provider；
- 只允许 Rule Descriptor 驱动动态消息转换。

主要证据：

- `RpcGatewayServer`、`RpcGatewayHandlerRegistry`
- `RpcGatewayForwarder`、`RpcProviderChannelCache`
- `HttpRpcUpstreamAdapter`、`HttpRpcDynamicMessageBridge`
- `RpcGatewayServerTest`、`RpcGatewayHandlerRegistryTest`
- `HttpRpcUpstreamAdapterTest`、`HttpRpcDynamicMessageBridgeTest`

### 4.5 GWS-05 Provider 发现、健康与负载均衡

实现：

- 统一 Provider Service Key、Instance、Lease、Registry/Health 状态；
- DDC Registry Adapter 和按引用计数管理订阅的 Provider Directory；
- HTTP Provider Runtime 独立注册、心跳、恢复和注销；
- Metadata/Zone/Tag/协议/健康/过期候选过滤；
- Round Robin、Smooth Weighted Round Robin、Random、Least Inflight；
- 主动与被动健康维度分离；
- Engine Readiness 等待活动规则所需 Provider 可用。

主要证据：

- `ProviderDirectory`、`DdcProviderServiceRegistryAdapter`
- `HttpProviderLeaseRuntime`
- `ProviderCandidateFilter`、`ProviderLoadBalancers`
- `PassiveHealthTracker`
- 对应 Directory、候选、负载、Lease 和健康测试。

### 4.6 GWS-06 Rule 发布与运行态

实现：

- Admin 从 Draft/Catalog 编译完整、Canonical 的 Rule Content；
- 内容 SHA 与 Artifact SHA 分离；
- 小规则 Inline，大规则 Manifest/Chunk；
- Admin 通过 DDC Management Client 发布 Chunk 后发布 Active Pointer；
- Engine exact + prefix Applier 组装同版本规则；
- 校验、编译、Provider 准备、LKG Staging、原子切换、最终 LKG；
- 失败保留旧快照并返回精确 ACK 事实；
- 启动恢复 LKG，运行期周期校准补偿 Pub/Sub 丢失。

主要证据：

- `GatewayRuleCompiler`、`GatewayDdcRulePublisher`
- `GatewayRuleActivationApplier`、`GatewayRuleApplierRegistrar`
- `GatewayRuleJsonCodec`、`EngineGatewayRuleCompiler`
- `GatewayDdcRulePublisherTest`、`GatewayRuleActivationApplierTest`

### 4.7 GWS-07 流量治理

实现：

- 强类型动态 Traffic Policy；
- 总 Deadline 与 Attempt Timeout；
- 本地 Token Bucket 和 Redis Lua 分布式限流；
- 有界 Key、TTL、Fail Closed/Local Fallback 固定语义；
- Operation/Provider Instance Bulkhead；
- Circuit Breaker Open/Half-Open/Close；
- 幂等 Retry，非幂等默认禁止，重试受剩余 Deadline 约束；
- Rule State Epoch/Policy Version 隔离动态状态。

主要证据：

- `GatewayTrafficGovernance`、`GatewayAttemptExecutor`
- `LocalTokenBucketRateLimiter`、`DistributedTokenBucketRateLimiter`
- `GatewayBulkheadRegistry`、`GatewayCircuitBreakerRegistry`
- `GatewayTrafficGovernanceTest`、`GatewayRateLimitTest`
- `GatewayResilienceTest`、`DefaultGatewayHttpDataPlaneHandlerRetryTest`

### 4.8 GWS-08 安全扩展

实现：

- 框架无关安全 Model/SPI；
- Extractor → Authentication → Authorization → Identity Mapper 责任链；
- Engine Rule 编译时能力校验；
- HTTP/RPC 共用 Fail Closed 语义；
- Listener 信任边界、可信代理地址解析、身份 Header 清洗；
- `externalAccessible` 与 Route `AccessZone` 双重约束；
- 不内置下游权限系统。

主要证据：

- `GatewayCredentialExtractor`、`GatewayAuthenticationProvider`
- `GatewayAuthorizationProvider`、`GatewayIdentityMapper`
- `GatewaySecurityCapabilityRegistry`、`GatewaySecurityChain`
- `RuleBackedHttpGatewaySecurityProcessor`
- `RuleBackedRpcGatewaySecurityProcessor`
- 安全契约、能力、责任链、身份清洗和 Listener 测试。

### 4.9 GWS-09 Admin 后端

实现：

- 独立 Spring Boot Admin；
- Gateway Group、Application/Credential、三级目录、Operation Definition；
- Draft Route/Policy 乐观 Revision 与 Idempotency；
- Release、Target、Rollback、Retry 和恢复编排；
- 审计日志和受限管理 API；
- DDC Engine Node/Provider 投影及 stale 标记；
- PostgreSQL Flyway V1、V2；
- Credential 使用 AES-GCM，主密钥由运行配置提供。

主要证据：

- `GatewayAdminApplication`
- Application/Domain/Infrastructure/Interfaces 分层包；
- `V1__create_gateway_admin_schema.sql`
- `V2__add_gateway_observability_projection.sql`
- Admin Domain、Catalog、Draft、Release、Projection、Schema 测试。

### 4.10 GWS-10 Starter 接口定义上报

实现：

- Spring MVC Controller 与 Egon RPC Contract 定义发现；
- 业务域 → 实体域 → 接口组三层目录；
- 每个 Controller/RPC Service 对应一个接口组；
- 完整 Operation 参数、Schema、响应、Descriptor、标签与属性；
- `externalAccessible=false` 默认值；
- `idempotent` 标签规范化为稳定属性；
- Canonical Definition Set、Fingerprint、批量 HMAC 上报；
- 启动时只上报定义，不注册 Provider、不拦截调用、不发 Kafka。

主要证据：

- `GatewayDefinitionReportFactory`
- `MvcGatewayDefinitionContributor`
- `WebFluxGatewayDefinitionContributor`
- `RpcGatewayDefinitionContributor`
- `GatewayReportHttpClient`
- `GatewayOperationSemantics`
- Starter Factory、语义、HTTP Client 和 AutoConfiguration 测试。

### 4.11 GWS-11 Admin Web

实现：

- React、TypeScript、Vite、React Router、TanStack Query；
- Ant Design 管理界面和 Ant Design Charts 聚合图表；
- Dashboard、Gateway Group、Draft、Release、Catalog、Operation、
  Provider、Trace、Audit 页面；
- 统一 API Client、Trace、Idempotency、Revision 和错误处理；
- 投影 stale/source/observedAt 可见；
- 前端不直接访问 DDC、Redis、Kafka 或 Engine；
- 菜单和页面不包含 Nginx、Nacos、Dubbo 管理功能。

主要证据：

- `gateway-admin-web/src/app/App.tsx`
- `gateway-admin-web/src/api`
- `gateway-admin-web/src/features`
- Vitest API/规则/Release/脱敏测试和 Playwright 契约入口。

### 4.12 GWS-12 Trace、可观测性与调用事件

实现：

- W3C `traceparent` → `X-Trace-Id` → Engine 生成的选择顺序；
- HTTP、RPC、HTTP→RPC Trace 传播；
- 每次业务调用形成唯一不可变终态 Observation；
- 低基数指标、结构化日志和敏感字段约束；
- 有界数量/字节 Kafka 队列、专用发送线程、Best Effort；
- Kafka 不可用不改变业务响应；
- Admin Consumer 按事件 ID 幂等投影并提供 Trace/聚合查询。

主要证据：

- `GatewayTraceContext`、`GatewayCallObservation`
- `GatewayCallEventDispatcher`、`KafkaGatewayCallEventSink`
- `GatewayCallEventConsumerHandler`
- `GatewayObservabilityQueryService`
- Dispatcher、Kafka Sink、Consumer、Projection 和 Trace 测试。

### 4.13 GWS-13 Test 与部署

实现：

- 真实 Spring Boot HTTP Provider；
- 真实 Protobuf Contract、Egon RPC Provider 和 RPC Consumer；
- 随机端口、Readiness、日志、脱敏 Manifest、优雅/强制停止的进程 Harness；
- PostgreSQL、DDC Redis、限流 Redis、Kafka Testcontainers；
- 默认快速测试与 `gateway-live-test` Failsafe Profile 分离；
- 真实 HTTP 拓扑覆盖 Starter 上报、DDC 注册、Rule 发布/ACK、双 Provider
  Round Robin、Provider 摘除恢复、PUBLIC/INTERNAL、限流、Trace/Kafka/Admin；
- 真实 RPC 拓扑覆盖 Starter Descriptor、RPC Provider 发现、内部 Gateway Slot、
  RPC→RPC、HTTP→RPC 和 Trace/Kafka/Admin；
- 0/1/2 RPC Gateway Slot 状态机有 RPC Component 自动化测试；
- Engine/Admin/Test App 可执行 Jar；
- 非 Root Java 21 Containerfile、Admin Web 静态镜像、Compose 和部署说明；
- Engine LKG 持久化目录和优雅停机顺序明确。

主要证据：

- `GatewayLiveTopologyIT`
- `GatewayProcessHarness`、`GatewayTestInfrastructure`
- HTTP/RPC Test Applications
- `deployment/compose.yml`、Containerfile 与部署 README
- `gateway-admin-web/e2e/gateway-admin.spec.ts`

## 5. 原始 29 章能力追踪

原始 `gateway.md` 的 29 章逐章主责映射保存在
`2026-07-25-gateway-child-spec-index.md`，本轮没有删除任何原始能力。

边界调整只有：

1. 原文涉及的 Nginx 节点负载、配置生成和动态刷新不属于业务网关；
2. 其动态路由和负载目标由 DDC Provider 发现、Engine 路由/负载和 Rule Snapshot
   承接；
3. Nacos、Dubbo 被 Egon DDC、Egon RPC 替代；
4. 业务权限系统不在 Gateway 内实现，但 Gateway 安全扩展框架保留；
5. Starter 上报接口定义，调用事件只由 Engine 发往 Kafka。

因此“29 章全部实现”指原始业务能力已经由明确的 Gateway/RPC/DDC 模块承接，不包括
被确认排除的 Nginx 管理实现。

## 6. 关键端到端链路

### 6.1 HTTP

`Client → PUBLIC/INTERNAL Listener → Route → Security → Governance →
DDC Provider Directory → Load Balancer → HTTP Provider → Observation →
Kafka → Admin Projection`

### 6.2 RPC

`RPC Consumer → DDC INTERNAL_GATEWAY → Engine Dynamic Handler →
DDC RPC_PROVIDER → RPC Provider → Observation → Kafka → Admin Projection`

### 6.3 HTTP→RPC

`HTTP Client → INTERNAL Route → Rule Descriptor → DynamicMessage →
DDC RPC_PROVIDER → RPC Provider → JSON Response`

### 6.4 规则发布

`Admin Draft → Validate/Diff → Release → DDC DB/Redis/PubSub →
Engine Validate/Compile/Prepare/LKG/Activate → exact instanceId+leaseId ACK →
Admin Target`

### 6.5 接口定义

`HTTP Controller/RPC Contract → Starter Contributor → Canonical Definition Set →
HMAC Batch Report → Admin Catalog/Operation/Definition`

## 7. 设计模式使用与约束

| 模式 | 使用位置 | 解决的问题 |
|---|---|---|
| Strategy | 负载均衡、限流后端、调用 Sink | 动态算法和后端替换 |
| Chain of Responsibility | HTTP Filter、安全链、Config Applier | 有序阶段、短路和扩展 |
| State | Engine、RPC Slot、Release、Lease | 明确生命周期与非法转换 |
| Observer | DDC 订阅、调用完成、Kafka 投影 | 解耦事实产生和消费 |
| Adapter | Reactor Netty、DDC、RPC、Kafka、JPA | 隔离外部技术细节 |
| Compiler | Route、Rule、Policy、RPC Method Index | 写时校验并生成只读热路径 |
| Builder | Process Spec、测试拓扑 | 安全构造多参数不可变对象 |
| Facade | Admin Application Service、DDC Management | 隔离接口与内部领域模型 |

没有为简单 DTO 或直接映射额外引入 Factory/继承层；模式只用于已经存在的变化点和
一致性边界。

## 8. 自动化验证层级

| 层级 | 覆盖 |
|---|---|
| Contract/Boundary | 模块依赖、序列化、版本、错误、身份 |
| Unit | 路由、规则、负载、治理、安全、Trace、上报 |
| Component | Reactor Netty、gRPC Server、Kafka Sink、Admin Service、DDC/RPC |
| Gated Live | 独立 JVM + Testcontainers 的 HTTP/RPC 完整拓扑 |
| Frontend | TypeScript、ESLint、Vitest、Vite Build、Playwright 入口 |
| Deployment | Compose 解析、Containerfile/Jar/静态 Server 结构 |

完整收尾验证命令：

```bash
./mvnw -B -ntp \
  -pl egon-cola-components/egon-cola-component-gateway/\
egon-cola-component-gateway-test/egon-cola-component-gateway-test-suite \
  -am clean verify

cd egon-cola-components/egon-cola-component-gateway/\
egon-cola-component-gateway-admin-web
npm run typecheck
npm test -- --run
npm run lint
npm run build
```

真实运行命令已经写入 GWS-13 和部署 README，但本次按约束不启动产品进程、不启动
Testcontainers Live Profile、不启动 Vite/Playwright 浏览器。

## 9. 本轮验证结果

本轮在新 Worktree 中完成以下验证，均未启动产品进程、容器或浏览器：

| 验证项 | 结果 |
|---|---|
| 27 模块 Maven `clean verify` | `BUILD SUCCESS` |
| Surefire 报告 | 140 份，329 个测试，0 Failure，0 Error，0 Skip |
| Engine→Admin 测试边界修复定向测试 | 2/2 通过 |
| HTTP Listener 排空定向测试 | 3/3 通过 |
| Admin Web TypeScript | 通过 |
| Admin Web Vitest | 5 个文件、10 个测试全部通过 |
| Admin Web ESLint | 通过 |
| Admin Web Vite 生产构建 | 通过 |
| `static-server.mjs` 语法检查 | 通过 |
| Compose `config --quiet` | 通过 |
| Engine/Admin/三个 Test App 可执行 Jar 结构 | 通过 |
| Flyway 变更检查 | 仅新增 Gateway Admin V1、V2，未修改既有迁移 |
| 禁用技术扫描 | 生产代码/POM 无 Spring Cloud Gateway、Nacos、Dubbo |
| 占位实现扫描 | 生产代码无 TODO、FIXME、`UnsupportedOperationException` |

`GatewayLiveTopologyIT` 的 HTTP、RPC 两个真实拓扑入口已通过 Maven
`testCompile`。它们受 `gateway-live-test` Profile 和
`gateway.live.test=true` 双重门禁保护，本轮未启用，因此不包含在上述 329 个已执行
测试中。

验证过程还发现并修复了一处仅在干净 Reactor 中暴露的测试边界问题：Engine
`GatewayRuleActivationApplierTest` 原先借用了 Admin 内部编译器，导致 Admin 被
Spring Boot Repackage 后 Engine `testCompile` 无法读取其内部类。测试现在只使用
Contract/Engine 契约构造 Fixture，Engine POM 不再测试依赖 Admin。修复后完整 27
模块干净构建通过。

非阻断提示：

1. Vite 报告 Dashboard chunk 压缩前约 1.46 MiB，后续可单独做图表依赖和页面级拆包；
2. macOS 单测环境未加载 Netty 原生 DNS Resolver，自动回退系统解析；部署目标为
   Linux 容器，本轮不把该测试环境提示当作生产验证；
3. DDC 既有测试使用的 Mockito 动态 Agent 和 `@MockBean` 有未来版本弃用提示，不是
   Gateway 本轮构建失败。

## 10. 明确未执行或未声称的事项

以下不影响代码交付状态，但必须在生产验收前另行执行：

1. `gateway-live-test` 真实容器/JVM 拓扑本次未运行；
2. Playwright 浏览器 E2E 本次未运行；
3. 生产 PostgreSQL、Redis、Kafka、网络和 TLS 未联调；
4. 性能、容量、长稳、故障注入数据尚未测量；
5. DDC V1 仍是单 Admin + 单 Redis 证据边界，不声称 HA；
6. RPC Gateway V1 仍要求每个 Slot 恰好一个 `INTERNAL_GATEWAY`，不声称多活；
7. RPC Streaming、Nacos、Dubbo、Nginx 管理均不在当前范围；
8. 下游业务权限系统需要业务方提供安全 SPI 实现。

## 11. 用户验收建议

建议按以下顺序审核：

1. 先审核总览 Spec 和本追踪文档的技术边界；
2. 再按 GWS-01～GWS-13 检查需求与代码落点；
3. 确认后运行 GWS-13 的 Live Profile；
4. 启动 Admin Web 执行 Playwright/人工页面验收；
5. 在目标环境补真实基础设施、性能和故障演练。

用户验收前，本分支和 worktree 保留，不自动合并、不推送、不删除。
