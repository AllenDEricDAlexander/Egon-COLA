# DDC 直连 RPC Facade、RPC 解耦与分布式 Admin 设计

状态：设计已确认，等待书面规格复核

编写日期：2026-08-09

代码基线：`main@0fd1b3c8`

主要涉及模块：

- `egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter`
- `egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-ddc-adapter`（新增）
- `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-starter`
- `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin`
- `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-test`
- `egon-cola-platforms/egon-cola-platform-gateway` 下的 Admin、Engine、Provider Runtime、Starter 与测试模块
- 启用 DDC 的 IdP、RBAC3、样例、部署和 Archetype
- `egon-cola-components-bom` 与 `egon-cola-platforms` dependency management

本文固化用户于 2026-08-09 确认的破坏式迁移设计。本文只定义目标架构、契约、边界和验收条件，不是实施 Plan。书面规格经用户审核通过后，才允许编写逐任务实施 Plan；在此之前不得开始代码改造。

本文在冲突范围内取代以下历史规格的既有决定，历史文件保留为当时事实，不回写：

- `2026-07-25-gateway-rpc-ddc-extension-design.md` 中 RPC Starter 直接使用 DDC 类型的设计；
- `2026-07-27-ddc-module-boundary-runtime-closure-design.md` 中 DDC 机器客户端使用 HTTP Endpoint 的设计；
- `2026-08-07-ddc-spring-boot-config-data-design.md` 中 ConfigData 直接创建 `HttpDdcConfigClient` 的传输实现；
- `2026-08-09-ddc-starter-package-consolidation-design.md` 中 HTTP Client 继续归属 DDC Starter、且不增加集成模块的设计。

---

## 1. 设计结论

本次迁移采用以下不可分割的架构结论：

1. `egon-cola-component-rpc-starter` 必须完全移除对 DDC Starter 的 Maven 和源码依赖；
2. 在 `egon-cola-components/egon-cola-component-rpc` 下新增 `egon-cola-component-rpc-ddc-adapter`；
3. adapter 是唯一同时理解 RPC 模型和 DDC 模型的集成叶子模块；
4. DDC Starter 继续拥有 `DdcConfigClient`、`DdcServiceRegistryClient`、`DdcManagementClient` 公共端口和领域模型；
5. adapter 用 egon-rpc 实现这三个端口，并承载 DDC ConfigData 的早期 RPC 引导；
6. DDC Admin 删除 `DdcOpenApiController`、`DdcRegistryOpenApiController`、`DdcManagementOpenApiController`；
7. 三组机器接口改为薄 RPC Provider，分别委托 `DdcConfigFacade`、`DdcRegistryFacade`、`DdcManagementFacade`；
8. DDC Admin 的人工管理页面、人工管理 REST、Spring Security/JWT 和 Actuator HTTP 能力继续保留；
9. 原三个 Controller 承载的 DDC request/response 机器操作全部使用 `DIRECT` gRPC 通道，不经过 Gateway，也不通过 DDC 自身的服务注册目录发现 DDC Admin；异步通知继续使用 Redis Topic；
10. DDC Admin 是平台唯一的服务发现自举例外，通过本地配置的 DNS、VIP、Kubernetes Service 或 gRPC 负载均衡地址访问；
11. 除 DDC 外的平台托管 RPC/HTTP Provider、Gateway 和动态配置消费者默认由 DDC 提供注册、租约、目录和配置控制面；
12. 普通业务 RPC 继续经过 Gateway，不因新增 Direct Channel 而允许业务调用绕过 Gateway 治理；
13. DDC Redis 租约、目录修订、配置发布 Topic、注册变更 Topic 和发布 ACK 状态机保持现有语义；
14. 本次不把 Redis Pub/Sub 订阅改成 gRPC Streaming；
15. 本次不保留机器 HTTP OpenAPI 兼容层，不提供 HTTP/RPC 双栈长期切换开关；
16. 本次不修改数据库表结构，不修改任何已有 Flyway 文件，也不新增 Flyway 迁移；
17. DDC Admin 支持共享 PostgreSQL 与 Redis 的 Active-Active 多实例部署，但不实现 Raft、选主或存储复制。

最终控制面关系如下：

```text
DNS / VIP / Kubernetes Service / PostgreSQL / Redis
                         |
                         v
                DDC Admin gRPC 集群
                         |
        +----------------+----------------+
        |                |                |
        v                v                v
  ConfigData       Provider 注册      Gateway 管理发布

非 DDC Provider ----注册/心跳----> DDC
Gateway ------------发现 Provider-> DDC
RPC Consumer --------发现 Gateway--> DDC
RPC Consumer --------业务调用------> Gateway ------> Provider
```

DDC 是控制面依赖，不是普通业务请求的数据面中转节点。

---

## 2. 当前问题与源码边界

### 2.1 RPC Starter 被 DDC 平台语义污染

当前 `egon-cola-component-rpc-starter` 直接依赖 `egon-cola-platform-dynamic-config-center-starter`，并在通用 RPC 自动装配中直接引用：

- `DdcProperties`；
- `DdcInstanceIdentity`；
- `DdcServiceRegistryClient`；
- `DdcServiceKeyFactory`；
- `ServiceInstanceMetaCodec`。

具体耦合包括：

- `RpcProviderLeaseManager` 直接注册、心跳、注销 `RPC_PROVIDER`；
- `RpcConsumerGatewayManager` 直接查询和订阅 `INTERNAL_GATEWAY`；
- `RpcProcessIdentityFactory` 直接从 DDC 属性和身份生成 RPC 进程身份；
- `RpcProviderMetadataMerger` 直接使用 DDC 元数据编码规范。

结果是基础 RPC 组件反向依赖平台 DDC，RPC Starter 无法独立构建、测试或替换注册目录实现。

### 2.2 三个 OpenAPI Controller 是机器协议入口

当前映射关系为：

| Controller | Starter 端口 | 主要调用方 |
|---|---|---|
| `DdcOpenApiController` | `DdcConfigClient` | ConfigData、DDC Runtime、CONFIG_CLIENT 租约和 ACK |
| `DdcRegistryOpenApiController` | `DdcServiceRegistryClient` | RPC Provider、RPC Consumer、Gateway Engine、Gateway Provider Runtime |
| `DdcManagementOpenApiController` | `DdcManagementClient` | Gateway Admin 与管理投影 |

三个 Controller、三个 `HttpDdc*Client`、Servlet HMAC Filter、请求规范化和 HTTP TLS 配置共同形成一套只服务内部机器调用的重复传输栈。

### 2.3 ConfigData 比 Spring Bean 更早执行

当前 `DdcConfigDataFetcher` 在 ConfigData 阶段直接 `new HttpDdcConfigClient(...)`。该阶段 ApplicationContext 和普通自动装配尚未建立，因此不能仅把运行时 Bean 换成 RPC Bean；必须提供一个不依赖 Spring Bean 的程序化 Direct RPC Client Factory。

### 2.4 DDC 不能依赖自己完成发现

如果 DDC RPC 也通过 Gateway 或 DDC Registry 发现，将形成以下闭环：

```text
Gateway 启动需要 DDC 目录
RPC Provider 注册需要 DDC
DDC 访问又需要 Gateway 或 DDC 目录
```

Gateway 故障时，Gateway Admin 也无法通过 DDC 发布恢复规则。DDC 必须作为自举根服务，从更底层的本地配置和外部网络地址解析启动。

---

## 3. 目标与非目标

### 3.1 目标

1. 恢复 RPC Starter 的通用组件边界；
2. 让 DDC 的机器接口统一使用 Protobuf unary RPC；
3. 保留 DDC Starter 的稳定 Java 端口，使 Gateway、IdP、RBAC3 的业务层不感知传输替换；
4. 让 ConfigData、运行时配置、服务注册和管理发布共享同一套 DDC RPC 契约、安全和错误语义；
5. 用 Facade 固定 Admin 应用边界，禁止协议 Provider 直接编排 Repository 或跨域 Service；
6. 切断 DDC 对 Gateway 和 DDC Registry 的自举依赖；
7. 支持 DDC Admin 通过 DNS/VIP/Kubernetes Service 和共享存储进行 Active-Active 部署；
8. 完整保留当前 HMAC 权限、scope、nonce 防重放和可信 operator 审计语义；
9. 保留 Spring Boot ConfigData 优先级、YAML-only 远程配置和保留键保护；
10. 一次性迁移仓库内所有生产消费者、测试、文档、示例和部署配置。

### 3.2 非目标

- 不删除 DDC Admin Web 或人工管理 REST；
- 不把普通业务 RPC 改为 Provider 直连；
- 不让 `@EgonRpcReference` 默认或可随意选择 Direct 路由；
- 不替换 PostgreSQL、Redis、JPA、Flyway 或现有发布状态机；
- 不把 Registry Redis Topic 改为 gRPC Server Streaming；
- 不实现 DDC Admin 节点选举、Raft、共识日志或内置节点发现；
- 不引入新的配置合并规则；
- 不允许远程 DDC YAML 覆盖 DDC 自身 target、TLS、Credential、Redis 或 Spring ConfigData/Profile 设置；
- 不在本规格阶段编写实施 Plan 或修改生产代码；
- 不自动启动 DDC、Gateway、Redis、PostgreSQL、IdP 或 RBAC3 进程。

---

## 4. Maven 模块与依赖方向

### 4.1 目标 Reactor 结构

```text
egon-cola-components/egon-cola-component-rpc
├── egon-cola-component-rpc-starter
├── egon-cola-component-rpc-ddc-adapter
└── egon-cola-component-rpc-test

egon-cola-platforms/egon-cola-platform-dynamic-config-center
├── egon-cola-platform-dynamic-config-center-starter
├── egon-cola-platform-dynamic-config-center-admin
└── egon-cola-platform-dynamic-config-center-test
```

### 4.2 依赖图

```text
rpc-starter                 ddc-starter
     ^                           ^
     |                           |
     +------ rpc-ddc-adapter ----+
                    ^
                    |
          +---------+---------+
          |                   |
      ddc-admin        Gateway / IdP / RBAC3
```

强制规则：

1. `rpc-starter` 不得依赖 DDC Starter 或 adapter；
2. `ddc-starter` 不得依赖 RPC Starter 或 adapter；
3. adapter 直接依赖 RPC Starter 和 DDC Starter；
4. DDC Admin 直接声明它实际使用的 DDC Starter、RPC Starter 和 adapter，不依赖传递偶然性；
5. 直接使用 DDC Java 端口的 Gateway 模块继续直接依赖 DDC Starter；
6. 需要默认 RPC 实现或自动装配的可运行组合模块增加 adapter；
7. adapter 进入 `egon-cola-component-rpc/pom.xml` 的 modules 和 dependency management；
8. adapter 进入 `egon-cola-components-bom` 只做版本管理，不由 RPC Starter 传递暴露；
9. `egon-cola-platforms` 已导入 Components BOM，平台模块不得重复写 adapter 版本；
10. 任何生产模块不得依赖 DDC Admin 作为库。

### 4.3 adapter 位于 Components 的边界说明

adapter 物理位于 `components/rpc`，但逻辑上是集成叶子模块，不是无条件通用基础组件。它形成唯一受控的：

```text
components/rpc-ddc-adapter -> platforms/ddc-starter
```

因此：

- `rpc-starter` 可以脱离 Platforms 独立构建；
- 完整根 Reactor 由 Maven 按 artifact 依赖排序；
- 单独从 RPC 聚合目录构建包含 adapter 时，需要 DDC Starter 已在同一 Reactor 或本地仓库；
- 不允许 adapter 的依赖反向渗透到其他无关 Components；
- 架构扫描必须把该依赖视为显式批准的唯一例外。

---

## 5. RPC Starter 去 DDC 化

### 5.1 中立 Provider Registry SPI

RPC Starter 新增中立端口：

```java
public interface RpcProviderRegistry {
    RpcProviderLease register(RpcProviderRegistration registration);

    RpcLeaseOperationResult heartbeat(RpcProviderLeaseIdentity lease);

    RpcLeaseOperationResult deregister(RpcProviderLeaseIdentity lease);
}
```

配套模型只表达 RPC 语义：

- `RpcProviderRegistration`：RPC 服务身份、进程身份、host、port、secure、metadata 和租约参数；
- `RpcProviderLease`：instanceId、leaseId、registeredAt、leaseExpireAt；
- `RpcProviderLeaseIdentity`：instanceId、leaseId 和 RPC 服务身份；
- `RpcLeaseOperationResult`：中立状态和可选 leaseExpireAt。

这些类型不得出现 `Ddc` 前缀、`DdcServiceKind` 或 Redis Key。

`RpcProviderLeaseManager` 改为只依赖 `RpcProviderRegistry`。adapter 负责把中立注册转换为 `DdcServiceRegistration`，并固定映射为 `RPC_PROVIDER`。

### 5.2 中立 Gateway Directory SPI

RPC Starter 新增：

```java
public interface RpcGatewayDirectory {
    RpcGatewaySubscription subscribe(
            RpcGatewayQuery query,
            Consumer<RpcGatewaySnapshot> listener
    );
}
```

配套模型：

- `RpcGatewayQuery`：env、目标 biz/app、serviceName、group、version；
- `RpcGatewaySnapshot`：revision、observedAt 和 `RpcGatewayEndpoint` 列表；
- `RpcGatewaySubscription`：关闭订阅的中立句柄。

`RpcConsumerGatewayManager` 只依赖 `RpcGatewayDirectory`，不再构造 `DdcServiceKey`。adapter 把查询映射为 `INTERNAL_GATEWAY` DDC 查询。

### 5.3 RPC 进程身份

RPC Starter 新增 `RpcProcessIdentityProvider`。默认实现只读取：

- `spring.application.name`；
- `egon.cola.component.rpc.identity.env`；
- `egon.cola.component.rpc.identity.host`；
- `egon.cola.component.rpc.identity.instance-id`；
- 当前进程 PID。

host 和 instanceId 未显式配置时使用 RPC 自己的默认解析器，不引用 `DdcInstanceIdentity`。

adapter 在 DDC 启用时提供优先级更高的 `DdcRpcProcessIdentityProvider`，复用 `DdcProperties` 与 `DdcInstanceIdentity`，保证 RPC 注册和 DDC 配置客户端使用同一物理实例身份。RPC Starter 本身仍不知道该实现。

### 5.4 Provider 元数据

`RpcProviderMetadataMerger` 只负责：

- 合并配置元数据和 ordered contributor；
- 拒绝空 key；
- 拒绝 `egon.rpc.*` 内部保留 key；
- 检测冲突值；
- 生成不可变、稳定顺序的 Map。

`ServiceInstanceMetaCodec` 的 DDC/Gateway 元数据规范校验移动到 adapter 的注册映射边界。RPC Starter 不再引用 DDC format 包。

### 5.5 Provider 注册模式

`EgonRpcProperties.Provider` 增加：

```text
egon.cola.component.rpc.provider.registration-mode=REQUIRED|DISABLED
```

语义：

- `REQUIRED` 为默认值；Provider 启用但没有 `RpcProviderRegistry` 时启动失败；
- `DISABLED` 只启动 gRPC Server，不注册、不心跳、不注销；
- `DISABLED` 时本地 Provider 在 Server 接受请求前进入 available；
- 停止时先拒绝新调用，再执行现有 graceful shutdown；
- DDC Admin 必须显式使用 `DISABLED`；
- 普通平台 Provider 不得使用 `DISABLED` 绕过 DDC，除非属于明确批准的基础设施或测试。

### 5.6 Gateway 与 Direct 两种 Channel Strategy

新增中立策略：

```java
public interface RpcInvocationChannelProvider {
    ManagedChannel currentChannel(Set<ManagedChannel> excluded);

    void recordFailure(ManagedChannel channel);

    int maxAttempts();
}
```

实现：

- `GatewayRpcInvocationChannelProvider`：包装现有 Gateway Manager，用于普通业务 RPC；
- `DirectRpcInvocationChannelProvider`：包装按 target 创建的 gRPC Channel，用于基础设施直连。

`RpcConsumerInvocationHandler` 和 `RpcConsumerProxyFactory` 改为依赖该策略，而不是硬编码 `RpcConsumerGatewayManager`。

`@EgonRpcReference` 继续只走 Gateway。不得增加面向业务代码的 `route=DIRECT` 注解参数。Direct 仅通过程序化 `RpcDirectClientFactory` 使用，并通过架构测试限制生产调用点为 `rpc-ddc-adapter`。

### 5.7 程序化 Direct Client

RPC Starter 提供：

```java
RpcDirectClientFactory
```

它接受：

- `@EgonRpcService` 契约类型；
- gRPC target；
- `RpcProcessIdentity`；
- TLS 配置；
- deadline；
- load-balancing policy；
- ordered client interceptors。

要求：

- 使用 `NettyChannelBuilder.forTarget(target)`；
- 默认 `round_robin`；
- 禁止 gRPC transparent retry；
- 支持显式关闭并等待 Channel 终止；
- 不依赖 ApplicationContext；
- 复用现有 contract validator、调用元数据、trace、deadline 和 status mapper；
- 不经过 `RpcConsumerGatewayManager`。

### 5.8 Interceptor 扩展

RPC Server Factory 从单一固定 interceptor 改为 ordered interceptor 列表；Direct Client Factory 同样接受 ordered client interceptors。

RPC Starter 只提供通用 trace、source、invocation metadata 扩展，不包含 DDC access key、operation 或 scope 规则。DDC 安全由 adapter 和 DDC Admin 实现。

---

## 6. `rpc-ddc-adapter` 模块

### 6.1 模块职责

新模块 artifact：

```text
top.egon:egon-cola-component-rpc-ddc-adapter
```

目标包树：

```text
top.egon.cola.component.rpc.ddc
├── contract
│   └── proto.v1
├── client
│   ├── config
│   ├── management
│   └── registry
├── mapping
├── registry
├── security
├── configdata
└── autoconfigure
```

每个新 Java 包必须按当前仓库规范提供中英文 `package-info.java`，说明允许类型、禁止职责和依赖方向。

### 6.2 契约文件

adapter 是 DDC RPC 传输契约的唯一源码归属，包含：

```text
src/main/proto/egon/ddc/v1/ddc_common.proto
src/main/proto/egon/ddc/v1/ddc_config_runtime.proto
src/main/proto/egon/ddc/v1/ddc_service_registry.proto
src/main/proto/egon/ddc/v1/ddc_management.proto
```

统一设置：

```proto
package egon.ddc.v1;
option java_package = "top.egon.cola.component.rpc.ddc.contract.proto.v1";
option java_multiple_files = true;
```

Protobuf 是 RPC wire schema 的唯一事实来源。禁止同时维护 JSON Schema、手写二进制 DTO 或把 Java DDC Model 直接作为 RPC 请求体。

### 6.3 Java RPC 契约

adapter 提供三个 `@EgonRpcService` Java 接口：

```text
DdcConfigRuntimeRpc
DdcServiceRegistryRpc
DdcManagementRpc
```

统一使用：

```text
group = "ddc"
version = "1.0.0"
```

这些接口只接受和返回生成的 Protobuf 类型，不返回 `ResultRecord`，不暴露 Admin Entity、Repository 或 Service。

### 6.4 三个 DDC Client Adapter

adapter 提供：

- `RpcDdcConfigClient implements DdcConfigClient`；
- `RpcDdcServiceRegistryClient implements DdcServiceRegistryClient`；
- `RpcDdcManagementClient implements DdcManagementClient`；
- `DdcRpcClientFactory`，供 Spring 组合模块按业务开关创建指定端口的 Direct RPC 实现。

上层 DDC Runtime、Gateway 和业务服务继续面向 DDC Starter 的三个端口。传输、Protobuf 转换、deadline、认证和错误还原全部封装在 adapter。

`DdcManagementClient.findConfig` 和 `getScopeBindings` 从默认抛出实现改为抽象方法。仓库内实现和 Test Double 一次性补齐，不保留“不支持该 RPC 能力”的兼容分支。

### 6.5 RPC Registry Bridge

adapter 提供：

- `DdcRpcProviderRegistry implements RpcProviderRegistry`；
- `DdcRpcGatewayDirectory implements RpcGatewayDirectory`。

前者把中立 RPC Provider Registration 映射为 DDC `RPC_PROVIDER` 注册；后者把 Gateway Query 映射为 `INTERNAL_GATEWAY` 查询和订阅。

DDC Registry 订阅继续采用：

```text
初始快照：Direct RPC
周期对账：Direct RPC
实时变更：Redis Topic
```

现有 `DdcRegistrySubscriptionCoordinator` 可继续复用，但快照加载器改为 RPC-backed 实现。

### 6.6 ConfigData 归属

以下 transport-dependent 类型从 DDC Starter 移入 adapter，并使用 adapter 包名：

- `DdcConfigDataLocationResolver`；
- `DdcConfigDataResource`；
- `DdcConfigDataLoader`；
- `DdcConfigDataFetcher`。

adapter 的 `META-INF/spring.factories` 注册 Resolver 和 Loader。DDC Starter 不再注册 ConfigData SPI。

ConfigData Fetcher 必须：

1. 从 Bootstrap Environment 绑定 `DdcProperties` 和 `DdcRpcProperties`；
2. 校验 target、runtime credential、TLS 和 timeout；
3. 程序化构建 `RpcDirectClientFactory`；
4. 调用 `DdcConfigRuntimeRpc.PullConfig`；
5. 转换为现有 YAML PropertySource；
6. 在完成或失败时可靠关闭 bootstrap Channel；
7. 不要求 `egon.cola.component.rpc.enabled=true`；
8. 不读取任何远程 DDC 自举属性。

`spring.config.import=ddc:application.yml` 与 `optional:ddc:application.yml` 语义保持不变。非 optional 连接失败时终止启动；optional 连接失败时按现有 ConfigData 语义继续本地配置。

### 6.7 自动装配顺序

adapter 提供 `DdcRpcAutoConfiguration`，并显式早于 DDC Starter 的 `DdcAutoConfiguration` 和 `DdcRegistryAutoConfiguration`：

- `ddc.enabled=true` 时，在 DDC Runtime Coordinator 装配前提供 `DdcConfigClient`；
- `ddc.registry.enabled=true` 且 Redis subscription 条件满足时，提供 `DdcServiceRegistryClient`、Registry snapshot loader 和 RPC bridge；
- `DdcRpcClientFactory` 只在存在合法 RPC target、TLS 和 credential 配置时允许创建具体 client；
- 所有具体 client Bean 使用 `@ConditionalOnMissingBean`，保留测试和应用显式替换端口的能力；
- DDC Starter 不再提供任何默认传输实现，只消费三个端口；
- 缺少 adapter 或缺少必需 client Bean 时必须以包含缺失端口和 adapter artifact 名称的错误快速失败，不能静默降级为无远端 DDC；
- DDC Admin 因 `ddc.enabled=false`、`ddc.registry.enabled=false` 且不请求 management client，不创建任何指向自己的 Direct Client。

---

## 7. RPC 契约边界

### 7.1 公共消息规则

`ddc_common.proto` 至少定义：

- `DdcScope`：`biz_code`、`env`、`app_code`；
- `DdcLeaseSession`：instanceId、leaseId、registeredAt、leaseExpireAt；
- `DdcLeaseOperationResult`：状态和可选 leaseExpireAt；
- `DdcServiceKey`；
- `DdcServiceInstance`；
- `DdcRpcErrorDetail`：code、message、retryable；
- 所有跨服务复用的 enum。

规则：

- 时间使用 `google.protobuf.Timestamp` 或明确的 duration 字段，不用本地时区字符串；
- 每个 enum 的 0 值必须是 `*_UNSPECIFIED`；
- 可空 scalar 使用 Proto3 `optional`，不以空字符串偷偷表达所有 null；
- 字段号发布后永不复用；删除字段必须 `reserved`；
- physical scope 不包含已废弃 namespace；
- management scope query 可以携带 `namespace_code` 作为可见性过滤条件；
- map metadata 必须经过大小、key 和保留前缀校验；
- 所有请求限制总消息大小，沿用当前 DDC `maxConfigBytes` 约束并增加 RPC message limit。

### 7.2 Config Runtime Service

```proto
service DdcConfigRuntimeService {
  rpc RegisterConfigClient(RegisterConfigClientRequest)
      returns (RegisterConfigClientResponse);
  rpc HeartbeatConfigClient(HeartbeatConfigClientRequest)
      returns (HeartbeatConfigClientResponse);
  rpc OfflineConfigClient(OfflineConfigClientRequest)
      returns (OfflineConfigClientResponse);
  rpc PullConfig(PullConfigRequest)
      returns (PullConfigResponse);
  rpc AcknowledgePublish(AcknowledgePublishRequest)
      returns (AcknowledgePublishResponse);
}
```

字段语义必须覆盖当前：

- instanceId、bizCode、env、appCode；
- host、可选 port、pid、sdkVersion、metadata；
- leaseSeconds、heartbeatIntervalSeconds；
- leaseId；
- 配置 resourceName、format、content、version、checksum；
- ACK changeId、instanceId、leaseId、targetVersion、currentVersion、status、errorMessage、ackTime。

`PullConfigRequest` 必须显式携带 scope，不能依赖服务端 session 或 sticky connection。

### 7.3 Service Registry Service

```proto
service DdcServiceRegistryService {
  rpc RegisterService(RegisterServiceRequest)
      returns (RegisterServiceResponse);
  rpc HeartbeatService(HeartbeatServiceRequest)
      returns (HeartbeatServiceResponse);
  rpc DeregisterService(DeregisterServiceRequest)
      returns (DeregisterServiceResponse);
  rpc GetServiceInstances(GetServiceInstancesRequest)
      returns (GetServiceInstancesResponse);
  rpc GetServices(GetServicesRequest)
      returns (GetServicesResponse);
}
```

Heartbeat 和 Deregister 请求必须显式携带完整 service key、instanceId、leaseId，不能依赖客户端到某个 Admin 实例的会话状态。

V1 只使用 unary RPC。`subscribe` 和 `subscribeServices` 由 adapter 组合初始/对账 unary RPC 与现有 Redis Topic，不进入 Proto streaming contract。

### 7.4 Management Service

```proto
service DdcManagementService {
  rpc FindConfig(FindConfigRequest) returns (FindConfigResponse);
  rpc UpsertConfig(UpsertConfigRequest) returns (UpsertConfigResponse);
  rpc DeleteConfig(DeleteConfigRequest) returns (DeleteConfigResponse);
  rpc PublishConfig(PublishConfigRequest) returns (PublishConfigResponse);
  rpc GetPublishTask(GetPublishTaskRequest) returns (GetPublishTaskResponse);
  rpc RetryPublishTask(RetryPublishTaskRequest) returns (RetryPublishTaskResponse);
  rpc GetConfigClients(GetConfigClientsRequest) returns (GetConfigClientsResponse);
  rpc GetScopeBindings(GetScopeBindingsRequest) returns (GetScopeBindingsResponse);
  rpc GetServiceKeys(GetServiceKeysRequest) returns (GetServiceKeysResponse);
  rpc GetInstances(GetInstancesRequest) returns (GetInstancesResponse);
}
```

`FindConfigResponse` 使用显式 `found` 与可选 config 表达未找到，不把 NOT_FOUND 当普通空查询异常。写入、删除、发布请求保留 expectedVersion、changeId、reason、description、format 和 timeout 语义。

请求中的 operator 不是可信身份，只允许作为 requestedOperator 审计备注；最终 operator 必须由服务端已认证 principal 生成。

---

## 8. DDC Admin Facade 与 RPC Provider

### 8.1 Config Facade

新增 `DdcConfigFacade`，统一封装：

- CONFIG_CLIENT 注册；
- CONFIG_CLIENT 心跳；
- CONFIG_CLIENT 下线；
- 配置快照拉取；
- 发布 ACK。

它组合现有 `DdcInstanceAdminService`、`DdcConfigService` 和 `DdcPublishService`。`DdcConfigRpcProvider` 只负责 Protobuf 转换、调用认证上下文和委托 Facade。

### 8.2 Registry Facade

新增 `DdcRegistryFacade`，统一封装：

- 服务实例注册；
- 服务实例心跳；
- 服务实例注销；
- 服务实例快照；
- 服务目录快照。

它委托现有 `DdcServiceRegistryService`。`DdcRegistryRpcProvider` 不直接访问 Redis Repository。

### 8.3 Management Facade

保留并扩展现有 `DdcManagementFacade`：

- `findConfig`；
- `upsert`；
- `delete`；
- `publish`；
- `getPublishTask`；
- `retry`；
- `getConfigClients`；
- `getScopeBindings`；
- `getServiceKeys`；
- `getInstances`。

当前 Controller 直接使用 `DdcNamespaceEnvAppBindingService` 的 scope binding 查询必须迁入 Facade。`DdcManagementRpcProvider` 只能调用 Facade。

### 8.4 Provider 类

Admin 新增：

```text
top.egon.cola.component.ddc.admin.rpc.provider.DdcConfigRpcProvider
top.egon.cola.component.ddc.admin.rpc.provider.DdcRegistryRpcProvider
top.egon.cola.component.ddc.admin.rpc.provider.DdcManagementRpcProvider
```

三者使用 `@EgonRpcProvider`，分别实现 adapter 中的三个 `@EgonRpcService` 接口。

Provider 禁止：

- 直接访问 JPA Repository；
- 直接访问 Redisson；
- 自己管理事务；
- 自己生成 operator；
- 返回 Admin Entity；
- 吞掉 DDC Error Code；
- 在 Provider 中复制现有 Service 业务校验。

### 8.5 HTTP 边界

最终删除机器路由：

```text
/api/v1/ddc/openapi/**
/api/v1/ddc/openapi/registry/**
/api/v1/ddc/openapi/management/**
```

保留：

- DDC Admin Web 使用的 `/api/v1/ddc/**` 人工管理 REST；
- 登录、JWT、RBAC、安全配置；
- Actuator 和人工运维 HTTP 接口；
- 静态 Admin Web 资源。

---

## 9. DDC 自举与平台服务规则

### 9.1 DDC 是唯一自举例外

所有 DDC 机器调用固定为：

```text
调用方 -> 本地 DDC RPC target -> DNS/VIP/Kubernetes Service -> DDC Admin gRPC
```

不得：

- 从 DDC Registry 查询 DDC Admin；
- 把 DDC Admin 注册为 `RPC_PROVIDER`；
- 通过 `INTERNAL_GATEWAY` 转发 DDC RPC；
- 从远程 DDC YAML 获取 DDC RPC target；
- 在未配置 target 时隐式回退 localhost。

### 9.2 非 DDC 平台服务默认由 DDC 支撑

平台默认规则：

1. RPC Provider 通过 adapter 注册 `RPC_PROVIDER`；
2. HTTP Provider 通过 Gateway Provider Runtime 注册 `HTTP_PROVIDER`；
3. Gateway Engine 从 DDC 查询和订阅 Provider；
4. RPC Consumer 从 DDC 查询和订阅 `INTERNAL_GATEWAY`；
5. 普通业务 RPC 继续 `Consumer -> Gateway -> Provider`；
6. 启用动态配置的应用通过 DDC 拉取和刷新配置。

RPC Starter 只依赖中立 SPI，因此该规则是 Egon-COLA 默认平台装配，不是 RPC 核心对 DDC 的硬编码。未来允许其他 adapter 或显式 standalone，但不属于本次实现。

### 9.3 DDC 故障语义

DDC 不在每个业务 RPC 请求路径上。DDC 短时不可用时：

- 已应用的本地配置继续有效；
- 已建立的 Gateway/Provider Channel 和最近目录快照可继续服务；
- 新启动、首次 ConfigData、Provider 注册、租约续期、新目录发现和配置发布受影响；
- 租约到期并被目录移除后，数据面会逐步降级。

不得把“已建立数据面暂时可用”描述为 DDC 故障完全无影响。

---

## 10. 配置模型

### 10.1 DDC RPC Client 配置

adapter 新增 `DdcRpcProperties`：

```yaml
egon:
  cola:
    component:
      ddc:
        rpc:
          target: dns:///ddc-admin:19080
          connect-timeout: 3s
          default-timeout: 5s
          load-balancing-policy: round_robin
          tls:
            enabled: true
            development-plaintext: false
            certificate-chain-path: ${DDC_CLIENT_CERT:}
            private-key-path: ${DDC_CLIENT_KEY:}
            trust-certificate-collection-path: ${DDC_CA_CERT:}
          auth:
            enabled: true
            runtime:
              access-key: ${DDC_RUNTIME_ACCESS_KEY:}
              secret-key: ${DDC_RUNTIME_SECRET_KEY:}
            registry:
              access-key: ${DDC_REGISTRY_ACCESS_KEY:}
              secret-key: ${DDC_REGISTRY_SECRET_KEY:}
            management:
              access-key: ${DDC_MANAGEMENT_ACCESS_KEY:}
              secret-key: ${DDC_MANAGEMENT_SECRET_KEY:}
```

启用条件：

- `ddc.enabled=true` 创建 config runtime client；
- `ddc.registry.enabled=true` 创建 registry client 和 RPC bridge；
- management client 不全局自动创建，由 Gateway Admin 等组合模块按自己的业务开关调用 `DdcRpcClientFactory.managementClient()` 创建；
- `spring.config.import` 含 `ddc:` 时在 bootstrap 阶段创建一次性 config client；
- 任一能力启用时 target 必须存在并合法；
- 不启用任何 client 的 DDC Admin 服务端不要求配置 target。

runtime、registry、management credential 分离以支持最小权限。部署方可以显式配置为同一 credential，但不提供隐式 profile 回退。

### 10.2 删除旧 HTTP 配置

删除：

- `egon.cola.component.ddc.admin.endpoint`；
- `egon.cola.component.ddc.admin.connect-timeout`；
- `egon.cola.component.ddc.admin.read-timeout`；
- `egon.cola.component.ddc.admin.tls.*`；
- `egon.cola.component.ddc.admin.access-key`；
- `egon.cola.component.ddc.admin.secret-key`；
- `gateway.admin.ddc.endpoint`；
- `gateway.admin.ddc.access-key`；
- `gateway.admin.ddc.secret-key`；
- `gateway.admin.ddc.connect-timeout`；
- `gateway.admin.ddc.read-timeout`；
- `gateway.admin.ddc.tls.*`。

Gateway Admin 保留：

- `gateway.admin.ddc.enabled`，用于条件创建基于 `DdcRpcClientFactory` 的 management client；
- `gateway.admin.ddc.publish-timeout`；
- target bizCode/appCode 等发布业务范围配置。

### 10.3 DDC Admin RPC Server 配置

DDC Admin 使用：

```yaml
egon:
  cola:
    component:
      ddc:
        enabled: false
        registry:
          enabled: false
      rpc:
        enabled: true
        provider:
          enabled: true
          port: 19080
          registration-mode: DISABLED
        consumer:
          enabled: false
```

Admin HTTP 默认端口和 Admin Web 不由本配置修改。外部负载均衡使用现有 HTTP Actuator readiness 检查 Admin 实例，并将机器流量转发到 gRPC 19080；本次不为此额外引入 gRPC Health Service 依赖。

### 10.4 本地保留键

`DdcReservedConfigurationKeys` 已保护整个 `egon.cola.component.ddc` 前缀。新增 `ddc.rpc.*` 自动属于本地保留配置，远程 YAML 出现时必须拒绝。

DDC Admin 的 PostgreSQL、Redis、RPC Server、Credential、TLS 和 profile 设置同样必须来自本地配置或 Secret 注入。

---

## 11. RPC 鉴权与审计

### 11.1 保留当前安全语义

迁移后必须保留：

- access key / secret key；
- timestamp 窗口；
- nonce 防重放；
- 请求内容 SHA-256；
- client type；
- allowed operations；
- bizCode/env/appCode pattern；
- 服务端可信 principal；
- operator 审计；
- nonce 存储异常时写请求 fail-closed；
- Secret 不进入日志、异常或 `toString()`。

### 11.2 gRPC Metadata

客户端发送以下 lowercase ASCII metadata：

```text
x-egon-ddc-access-key
x-egon-ddc-timestamp
x-egon-ddc-nonce
x-egon-ddc-content-sha256
x-egon-ddc-signature
x-egon-ddc-contract-version
```

签名规范固定为 UTF-8：

```text
v1
<fullGrpcMethodName>
<timestampEpochMillis>
<nonce>
<lowercaseHexSha256OfDeterministicProtobufRequest>
```

五行之间以及最后一行之后不附加额外字符，前四行使用单个 LF（`0x0A`）连接。使用 HMAC-SHA256，输出 lowercase hex。服务端必须对 unary request 使用 deterministic Protobuf 序列化重新计算摘要，不能信任客户端提交的 content hash。

### 11.3 服务端认证顺序

DDC RPC Server Interceptor 必须：

1. 读取 metadata 并检查格式；
2. 收到完整 unary request 后计算 deterministic bytes；
3. 校验 credential、timestamp、nonce、hash 和 signature；
4. 从 request 提取 scope；
5. 校验 operation 与 scope pattern；
6. 把 `DdcServicePrincipal` 放入 gRPC Context；
7. 认证成功后才把 request 传给 Provider。

认证失败不得执行 Facade，也不得写入发布、租约或 ACK 状态。

### 11.4 Operation 映射

| RPC 方法 | Operation |
|---|---|
| RegisterConfigClient | `SDK_REGISTER` |
| HeartbeatConfigClient | `SDK_HEARTBEAT` |
| OfflineConfigClient | `SDK_OFFLINE` |
| PullConfig | `CONFIG_PULL` |
| AcknowledgePublish | `PUBLISH_ACK` |
| RegisterService | `REGISTRY_REGISTER` |
| HeartbeatService | `REGISTRY_HEARTBEAT` |
| DeregisterService | `REGISTRY_DEREGISTER` |
| GetServiceInstances / GetServices | `REGISTRY_READ` |
| FindConfig | `MANAGEMENT_CONFIG_READ` |
| UpsertConfig / DeleteConfig | `MANAGEMENT_CONFIG_WRITE` |
| PublishConfig | `MANAGEMENT_PUBLISH` |
| GetPublishTask | `MANAGEMENT_TASK_READ` |
| RetryPublishTask | `MANAGEMENT_TASK_RETRY` |
| GetConfigClients | `MANAGEMENT_INSTANCE_READ` |
| GetScopeBindings | `MANAGEMENT_SCOPE_READ` |
| GetServiceKeys / GetInstances | `MANAGEMENT_REGISTRY_READ` |

### 11.5 Nonce Store

`DdcNonceStore`、`RedisDdcNonceStore`、credential registry 和 principal 语义迁移到 Admin 的 RPC security 包。删除 Servlet body wrapper、Filter 和 Filter Registration。

启用 DDC RPC 认证的 Admin 必须使用共享 `RedisDdcNonceStore`；没有该 Bean 时启动失败。`InMemoryDdcNonceStore` 只允许测试通过显式 Bean 覆盖使用，不作为可执行 Admin 的运行回退。

### 11.6 operator

服务端 operator 格式继续由 `DdcServicePrincipal.auditOperator(requestedOperator)` 生成。RPC request 中的 requestedOperator 不能覆盖 credential identity。

---

## 12. 错误、Deadline 与重试

### 12.1 错误映射

Admin 把 DDC 错误映射为 gRPC status：

| DDC 错误类别 | gRPC Status |
|---|---|
| 参数、格式、scope 缺失 | `INVALID_ARGUMENT` |
| 配置或任务不存在 | `NOT_FOUND` |
| 版本冲突、租约冲突、发布进行中 | `FAILED_PRECONDITION` |
| Credential、operation、scope 拒绝 | `PERMISSION_DENIED` 或 `UNAUTHENTICATED` |
| Redis、数据库、Admin 暂不可用 | `UNAVAILABLE` |
| 未分类服务端错误 | `INTERNAL` |

同时使用 binary trailer：

```text
x-egon-ddc-error-bin
```

内容为 `DdcRpcErrorDetail`。Client Adapter 根据 detail 还原现有 `DdcException`、`DdcManagementClientException` 和错误码，避免上层 Gateway/Runtime 依赖 gRPC Status。

### 12.2 中立传输异常

DDC Starter 增加 transport-neutral：

```text
DdcClientTransportException
```

至少包含 `retryable`。`DdcAckDelivery` 不再识别 Spring HTTP 异常或状态码，只识别该中立异常和 DDC 业务错误。

HTTP 专用 `DdcOpenApiRequestException` 在无生产引用后删除；`DdcManagementClientException` 作为业务端口异常继续保留。

### 12.3 Deadline

- ConfigData 使用 bootstrap timeout；
- runtime/registry/management 使用各自方法默认 deadline；
- publish request 的业务 timeout 仍控制 ACK 状态机，不等同于 gRPC deadline；
- Client deadline 必须小于或等于调用方剩余预算；
- timeout 日志不得输出 Secret 或完整配置内容。

### 12.4 重试

gRPC transparent retry 全部关闭。adapter 只允许在应用层重试明确幂等调用：

- Pull、Find、Get；
- heartbeat；
- ACK；
- 相同 instanceId/leaseId 的目录对账。

不透明重试：

- Upsert；
- Delete；
- Register；
- Deregister；
- RetryPublishTask。

Publish 已有 changeId 幂等和 UNKNOWN 恢复状态机，不使用 Channel 自动重试制造不确定重复写。

---

## 13. 分布式 DDC Admin

### 13.1 部署模型

DDC Admin 支持：

```text
多个无状态 Admin 实例
       + 共享 PostgreSQL
       + 共享 Redis
       + 外部 DNS/VIP/Kubernetes Service/gRPC LB
```

它不提供：

- 自建节点列表；
- Admin 自注册；
- leader election；
- Raft；
- PostgreSQL/Redis 数据复制。

Admin 多实例不能替代 PostgreSQL 和 Redis 自身的 HA。

### 13.2 gRPC 负载均衡

客户端 target 使用逻辑地址，例如：

```text
dns:///ddc-admin:19080
```

Direct Channel 默认 `round_robin`。外部 LB 必须支持 HTTP/2、连接摘除和后端 readiness。不得把单个 Pod IP 或单机 IP 作为生产 target。

DDC RPC 是无会话协议，每个请求携带完整 scope、service key、instanceId 和 leaseId，不要求 sticky session。

### 13.3 共享状态权威

| 状态 | 权威存储 |
|---|---|
| 配置、版本、发布任务、ACK、操作日志 | PostgreSQL |
| CONFIG_CLIENT 持久投影 | PostgreSQL |
| 活跃配置租约、服务实例、目录和 revision | Redis |
| 配置值、发布幂等键和通知 | Redis |
| HMAC nonce | Redis |
| 本地 waiter、短期 scope cache、known service keys | 非权威优化 |

任何本地 Map 不得成为跨 Admin 正确性的唯一依据。

### 13.4 Publish 并发

现有数据库配置行悲观锁、changeId 唯一约束、条件状态迁移、Redis 配置锁和 publish idempotency key 继续作为跨节点正确性基础。

明确要求：

- `PublishResourceLockRegistry` 只作为本 JVM 快速拒绝优化；
- 相同配置的跨节点发布由数据库锁和 active task 检查裁决；
- `DdcPublishAckRepository` 增加按 changeId、instanceId、leaseId 的悲观写锁查询，避免冲突 ACK 在两个 Admin 上同时覆盖；
- `PublishCompletionWaiterRegistry` 继续本地唤醒，等待方必须轮询 PostgreSQL，因此 ACK 落到其他 Admin 仍能完成；
- Redis publish idempotency key 保证多节点恢复扫描不会重复广播同一 changeId/fingerprint；
- 不新增数据库列或 Flyway 文件。

### 13.5 Scheduler

Lease Expiry、Publish Timeout 和 Startup Recovery 可以在每个 Admin 实例运行，不引入 leader election。正确性依赖现有：

- Redis 分布式锁；
- leaseId 条件删除；
- database status 条件更新；
- publish changeId 幂等。

允许重复扫描，不允许重复产生不同业务结果。多实例测试必须覆盖两个节点同时 scan/recover。

### 13.6 Scope Cache

`DdcScopeGate` 的 5 秒本地缓存继续作为读取优化，本次不增加新的 Redis Key 或 Topic。metadata write 只清除处理该写请求的 Admin 本地 cache；其他 Admin 最多在 TTL 内继续读取旧值。

本规格明确接受最多 5 秒的有界最终一致，不宣称 scope disable 跨节点强一致。Active-Active 测试必须证明其他节点在该窗口结束后重新读取 PostgreSQL。如果未来要求立即强一致，应独立评审为数据库强校验或带 revision 的共享缓存，不在本次扩大。

### 13.7 多实例运行约束

- 所有节点使用相同 Credential 规则和 TLS trust；
- 所有节点连接相同 Redis namespace；
- 所有节点连接相同 PostgreSQL schema；
- 所有节点时钟通过 NTP 同步；
- JVM 时区统一为 UTC；
- Protobuf 契约升级遵守向后兼容，支持滚动发布期间新旧节点共存；
- 无 session affinity 依赖。

---

## 14. DDC Starter 调整

### 14.1 保留

DDC Starter 继续拥有：

- `@DdcValue`、`@DdcRefreshable`；
- `DdcConfigClient`、`DdcServiceRegistryClient`、`DdcManagementClient`；
- `model.config`、`model.registry`、`model.management`、lease 和 instance 模型；
- Runtime Coordinator、默认值上报、pull/apply、heartbeat、offline、ACK Delivery；
- Redis Change Subscription 和 Registry Subscription Coordinator；
- ConfigData YAML 应用、优先级、保留键校验所需的通用环境能力；
- `DdcServiceKeyFactory` 和 DDC 领域元数据格式能力。

### 14.2 移出或删除

移入 adapter：

- ConfigData Resolver、Resource、Loader、Fetcher；
- 三个 RPC DDC Client 实现；
- Direct RPC security client 能力。

删除：

- `HttpDdcConfigClient`；
- `HttpDdcManagementClient`；
- `HttpDdcServiceRegistryClient`；
- `client.http.DdcCanonicalRequest`；
- `client.http.DdcOpenApiRequestFactory`；
- `client.http.DdcRequestSigner`；
- `client.http.DdcRestClientFactory`；
- `DdcClientTransportSecurity`；
- `DdcManagementClientProperties`；
- `error.http` 包；
- `DdcProperties.Admin` 和 HTTP URI 校验。

删除后重新审计依赖；若 Starter 主代码不再使用 Spring Web，则从 Starter POM 删除 `spring-web`。Jackson 因 Redis 消息、模型和 YAML 处理继续保留。

### 14.3 公共端口稳定性

三个 Java Client 接口的方法语义保持不变。实现由 HTTP 换成 RPC，但上层 DDC Runtime、Gateway Service 和测试 Double 仍面向端口。

允许的破坏变化仅包括：

- `DdcManagementClient` 两个 default unsupported 方法变为必实现；
- HTTP transport-only model 和 exception 删除；
- Javadoc 从 OpenAPI/HTTP 改为 DDC Control Plane/RPC，不保留错误术语。

---

## 15. 关联模块影响

### 15.1 DDC Admin

- 增加 RPC Starter 与 adapter 直接依赖；
- 增加三个 Facade/Provider 和 RPC Security；
- 删除三个 OpenAPI Controller；
- 删除 Servlet HMAC Filter 和机器 OpenAPI Filter Registration；
- Spring Security 删除 `/api/v1/ddc/openapi/**` 规则；
- Admin Web 和人工 REST 不变；
- DDC Admin 不创建 DDC Client，不配置 DDC target，不自注册。

### 15.2 DDC Test

- 增加 adapter；
- 端口编排单测继续使用 Recording Client；
- HTTP Client、request signer、request factory 测试删除；
- ConfigData 黑盒改为进程内 Direct gRPC Server；
- 增加两个 Admin Context 共享 PostgreSQL/Redis 的 Active-Active 测试；
- 不把单 Context 测试描述成分布式证明。

### 15.3 Gateway Admin

当前 `GatewayAdminConfiguration` 手工创建 `HttpDdcManagementClient`。迁移后：

- 删除 endpoint/access-key/secret/TLS 字段和 HTTP Client 构造逻辑；
- 保留 `gateway.admin.ddc.enabled` 条件，由一个只负责组合的 Bean 方法调用 `DdcRpcClientFactory.managementClient()`；
- 继续注入 `DdcManagementClient`；
- target、TLS、deadline 和 management credential 全部由 adapter 的 `DdcRpcProperties` 提供；
- `GatewayDdcRulePublisher`、Release Coordinator 和 projection 业务逻辑不修改端口；
- 保留 Gateway 自己的发布 timeout 与 target scope 配置；
- Gateway 数据面故障时，Gateway Admin 仍能 Direct RPC 调用 DDC。

### 15.4 Gateway Engine

- 继续编译依赖 DDC Starter 与 RPC Starter；
- `DdcProviderServiceRegistryAdapter`、`RpcGatewaySlotRuntime` 继续面向 `DdcServiceRegistryClient`；
- 运行组合增加 adapter，获得 RPC-backed Registry Client；
- Provider 发现仍使用 DDC，网关转发逻辑不变。

### 15.5 Gateway Provider Runtime

- HTTP_PROVIDER 注册端口不变；
- 运行时由 adapter 提供 Registry Client；
- 注册恢复、心跳和注销语义不变；
- 不直接依赖 RPC DDC 具体 Client 类。

### 15.6 Gateway Starter 与测试部署

- Starter 作为组合根引入 adapter；
- RPC 依赖保持现有 optional 语义时，adapter 条件必须与 RPC/DDC feature 一致；
- Gateway live、测试应用和部署 YAML 把 DDC HTTP endpoint 改为 gRPC target；
- 暴露 DDC Admin gRPC 19080；
- DDC Admin HTTP 端口继续供 Admin Web/Actuator；
- 测试分别验证 DDC direct control plane 与 Gateway business data plane，不能用一个 root HTTP 200 代替。

### 15.7 IdP Admin

- 继续直接依赖 DDC Starter 的注解、模型和运行时；
- 增加 adapter 作为运行时集成；
- `@DdcValue`、`@DdcRefreshable`、Yaml Applier 和业务配置逻辑不变；
- 删除旧 DDC Admin HTTP 属性。

### 15.8 RBAC3

- 默认关闭 DDC 的路径不强制创建 adapter client；
- 启用 DDC 的应用、测试和生产配置确保 adapter 在 classpath；
- Gateway/DDC 生产配置迁移为 gRPC target；
- 不借本次迁移修复无关 RBAC3 legacy 编译问题。

### 15.9 BOM、文档与 Archetype

- Components RPC 聚合 POM 增加 adapter module；
- Components BOM 增加 adapter version；
- 平台模块通过已导入 BOM 获取版本；
- DDC、RPC、Gateway 中英文 README 更新依赖、配置和拓扑；
- Runbook、部署 YAML、Docker/Kubernetes 端口更新；
- Archetype 和示例中启用 DDC 的应用增加 adapter；
- 历史 specs 不做全量文本替换，但活跃 README、Runbook 和配置不得残留旧 HTTP 说明。

---

## 16. 删除清单

最终生产源码中删除：

### DDC Admin

- `DdcOpenApiController`；
- `DdcRegistryOpenApiController`；
- `DdcManagementOpenApiController`；
- `DdcCachedBodyHttpServletRequest`；
- `DdcOpenApiHmacFilter`；
- `DdcSecurityFilterRegistration`；
- 仅为 Servlet Filter 存在的 request attribute 和 path canonicalization；
- `/api/v1/ddc/openapi/**` Security 配置。

保留但迁移包或职责：

- `DdcHmacCredential`；
- `DdcHmacCredentialRegistry`；
- `DdcNonceStore`；
- `RedisDdcNonceStore`；
- `InMemoryDdcNonceStore`；
- `DdcServicePrincipal`。

### DDC Starter

- 三个 `HttpDdc*Client`；
- `client.http` 整包；
- HTTP client transport models；
- HTTP request exception；
- HTTP ConfigData fetch 创建逻辑；
- `DdcProperties.Admin`。

### 测试

- 三个 OpenAPI Controller Test；
- Servlet HMAC Filter Test；
- 三个 HTTP Client Test；
- HTTP request factory/signer/canonical request tests；
- 只断言旧 route 或旧 property 的测试。

安全、scope、nonce、错误和 ConfigData 语义测试必须迁移到 RPC，不得以删除 HTTP 为理由降低覆盖率。

---

## 17. 兼容性与发布边界

本次是一次 monorepo 破坏式迁移：

- 不保留旧 HTTP Endpoint；
- 不保留旧 HTTP Properties；
- 不提供旧 Client deprecated wrapper；
- 不让旧客户端和新 Admin 跨版本互通；
- 仓库内所有生产消费者必须在同一发布版本迁移；
- 部署必须先保证 DDC Admin 新 gRPC Endpoint 可用，再升级调用方；
- 若需要生产滚动迁移，只允许在部署层安排同版本 Admin 先行，不在源码中保留长期双栈。

保持兼容的业务语义：

- DDC Java Client 端口；
- DDC model 的业务含义；
- ConfigData location 与 Spring Boot precedence；
- Redis Key、Topic、租约和 revision；
- changeId 幂等；
- ACK 状态机；
- Admin Web 和人工管理 REST；
- 数据库 Schema。

---

## 18. 设计模式选择

### 18.1 Ports and Adapters

`DdcConfigClient`、`DdcServiceRegistryClient`、`DdcManagementClient` 继续作为业务端口，HTTP Adapter 被 RPC Adapter 替换。该模式隔离上层业务与传输，直接解决 Gateway、Runtime 和 Admin Client 的耦合。

### 18.2 Facade

三个 Admin Facade 统一应用操作和 Service 编排，使 RPC Provider 保持薄层。Controller 直接调用多个 Service 的现状不能简单平移到 RPC Provider。

### 18.3 Strategy

`RpcInvocationChannelProvider` 区分 Gateway 与 Direct。普通业务 RPC 使用 Gateway Strategy，DDC 自举使用 Direct Strategy，避免在 Invocation Handler 中继续硬编码路由。

### 18.4 Adapter

`DdcRpcProviderRegistry` 和 `DdcRpcGatewayDirectory` 把 DDC 领域端口适配为 RPC 中立 SPI，让 RPC Starter 不再理解 `RPC_PROVIDER` 和 `INTERNAL_GATEWAY`。

### 18.5 Observer

Registry 与配置发布继续通过现有 Redis Topic 传播变更，快照 RPC 负责初始状态和对账。该模式与现有 DDC Redis 通知体系一致；本次不新增 scope invalidation Topic。

不采用额外 Command、Factory Method 层级或大量 Handler 链。现有业务复杂度由 Provider -> Facade -> Service 足够表达，避免无收益抽象。

---

## 19. 验证矩阵

### 19.1 RPC Starter

- RPC Starter compile/test 不需要 DDC artifact；
- `dependency:tree` 不出现 DDC Starter、DDC Admin 或 adapter；
- 主源码和测试不出现 `top.egon.cola.component.ddc` import；
- Provider Registry SPI 使用内存 fake 验证 register/heartbeat/deregister；
- Gateway Directory SPI 使用内存 fake 验证 READY、drain、round robin 和 failure；
- Direct Channel 验证 target、TLS、deadline、interceptor、shutdown；
- `@EgonRpcReference` 仍只能走 Gateway。

### 19.2 RPC DDC Adapter

- Proto descriptor 和 `@EgonRpcService` method 一致；
- 每个 DDC model/proto mapper 双向测试；
- 三个 Client Adapter 方法覆盖；
- Registry initial RPC、Redis update、periodic reconciliation 覆盖；
- ConfigData 在无 ApplicationContext 时拉取并关闭 Channel；
- optional/non-optional ConfigData 失败语义覆盖；
- HMAC deterministic body、timestamp、nonce、scope、operation 覆盖；
- gRPC error detail 正确还原 DDC exception；
- DNS target round robin 与 backend failure 覆盖。

### 19.3 DDC Admin

- 三个 Provider 只调用对应 Facade；
- Facade 保持现有 Service 语义；
- 未认证、签名错误、过期 timestamp、nonce 重放、scope 拒绝均不进入 Facade；
- operator 来自 principal；
- Admin 使用 registration-mode=DISABLED，不创建 Provider Lease；
- 旧 OpenAPI route 不存在；
- 人工 Admin REST 与 Web 安全测试继续通过。

### 19.4 Active-Active

使用两个 Admin ApplicationContext 或两个测试实例共享同一 PostgreSQL/Redis，至少验证：

- Register 到 Admin A、Heartbeat/Deregister 到 Admin B；
- Pull 到 A、ACK 到 B、等待方在 A 收敛；
- 相同 changeId 并发发布幂等；
- 不同 changeId 对同一配置并发时只有一个 active publish；
- 冲突 ACK 通过行锁得到确定结果；
- 两个 Scheduler 同时 scan/recover 不产生重复业务结果；
- nonce 在 A 使用后，B 拒绝重放；
- scope 在 A 禁用后，B 在约定的 5 秒窗口内失效；
- A 下线后 Direct Channel 切换到 B；
- 无 sticky session 依赖。

### 19.5 关联模块

- DDC Starter、Admin、Test 定向 test；
- RPC Starter、adapter、RPC Test 定向 test；
- Gateway Admin、Engine、Provider Runtime、Starter 和 RPC fixtures 定向 test；
- IdP Admin 定向 compile/test；
- RBAC3 启用/关闭 DDC 配置路径 compile/test；
- 根 Reactor `clean integration-test`；
- 不自动启动真实项目进程。

外部 Redis Sentinel/Cluster、PostgreSQL HA、DNS/VIP、多 JVM 和真实 LB 仍需要显式 live topology 验证，不能由 Maven 测试替代。

---

## 20. 残留扫描与验收条件

实现完成后必须满足：

1. RPC Starter POM 和源码对 DDC 零依赖；
2. adapter 是唯一同时依赖 RPC Starter 与 DDC Starter 的模块；
3. 三个机器 OpenAPI Controller 不存在；
4. 三个 `HttpDdc*Client` 和 `client.http` 不存在；
5. 活跃源码、配置、README 和 Runbook 不出现 `/api/v1/ddc/openapi`；
6. 活跃配置不出现 `ddc.admin.endpoint` 或 `gateway.admin.ddc.endpoint`；
7. `spring.config.import=ddc:` 只由 adapter ConfigData SPI 处理；
8. DDC Admin 不配置 DDC target、不注册自己、不依赖 Gateway；
9. DDC Direct RPC 使用外部 target 和 round robin；
10. 普通 `@EgonRpcReference` 继续通过 Gateway；
11. RPC Provider 默认需要 Registry，DDC Admin 唯一显式使用 registration disabled；
12. Gateway Admin 在 Gateway 数据面不可用时仍能直连 DDC；
13. 所有当前 HMAC operation、scope、nonce 和 operator 语义均有 RPC 测试；
14. Redis Key、Topic、数据库 Schema 和 Flyway 文件无变化；
15. Multi-Admin 测试证明跨节点租约、ACK、nonce、发布和故障切换；
16. 无 Secret 被写入日志或异常；
17. Git diff 不包含无关重构或用户已有修改。

残留检查至少包括：

```text
HttpDdc
/api/v1/ddc/openapi
ddc.admin.endpoint
gateway.admin.ddc.endpoint
DdcOpenApiHmacFilter
DdcCanonicalRequest
top.egon.cola.component.ddc（限定 rpc-starter）
```

历史 specs 中的旧名称属于历史记录，不作为活跃残留失败；生产源码、测试、README、Runbook、样例和部署配置属于失败范围。

---

## 21. 已锁定决策

以下事项已经确认，不在实施 Plan 阶段重新开放：

- adapter 放在 `components/rpc` 下；
- RPC Starter 去 DDC 化；
- 三个 DDC 机器 Controller 全部删除；
- DDC 机器协议只使用 Direct egon-rpc；
- DDC 不使用 Gateway 和 DDC Registry 发现自己；
- DDC 是平台唯一自举例外；
- 非 DDC 平台托管服务默认由 DDC 支撑；
- 普通业务 RPC 不允许 Direct 绕过 Gateway；
- Admin Web/人工 REST 保留；
- Redis Subscription 保留，不做 gRPC Streaming；
- HMAC scope、nonce、operator 语义保留；
- 支持共享存储的 Active-Active Admin；
- 不保留机器 HTTP 兼容层；
- 不修改数据库或 Flyway；
- 先审核本规格，审核通过后再写实施 Plan。
