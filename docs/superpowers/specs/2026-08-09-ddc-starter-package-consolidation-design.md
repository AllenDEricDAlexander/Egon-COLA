# DDC Starter 角色分包设计

状态：设计已确认，等待书面规格复核

编写日期：2026-08-09

代码基线：`main@9d3daef3`

主要涉及模块：

- `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-starter`
- `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin`
- `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-test`
- 所有直接引用 DDC Starter 类型的 Gateway、RPC、IdP、RBAC3 和测试模块

本文记录用户于 2026-08-09 确认的破坏式重组方案。实现必须一次性迁移仓库内消费者，不保留旧包兼容壳，不修改数据库和 Flyway，不启动应用。

---

## 1. 设计结论

DDC 保持一个可执行代码模块：

```text
egon-cola-platform-dynamic-config-center-starter
```

不新增 `autoconfigure`、`client`、`core` 或 `infrastructure` Maven 模块。业务应用、Gateway、RPC、IdP 和测试模块继续只依赖 Starter。

Starter 内部采用按代码角色划分的顶层包：

- `api`：公共接口和扩展点；
- `model`：跨模块使用的领域数据、请求、响应、事件和值对象；
- `client`：HTTP 客户端实现及其共享 HTTP 能力；
- `service`：绑定、刷新、生命周期和注册编排实现；
- `listener`：配置和注册事件监听实现；
- `state`：明确持有可变运行时状态的对象；
- `redis`：Redisson 客户端、Key 和 Topic 订阅基础设施；
- `configdata`：Spring Boot ConfigData SPI；
- `autoconfigure`：Starter 内部的 Spring Boot 自动装配和配置属性；
- `environment`、`format`、`observability`、`error`：边界清晰的支持能力。

删除当前的 `configuration`、`runtime`、`transport` 和顶层 `lease` 包。它们分别混合了领域、实现阶段或不相关技术，不能从包名判断类的实际角色。

## 2. Maven 模块边界

目标 Reactor 结构保持不扩张：

```text
egon-cola-platform-dynamic-config-center
├── egon-cola-platform-dynamic-config-center-starter
├── egon-cola-platform-dynamic-config-center-admin
└── egon-cola-platform-dynamic-config-center-test
```

Starter 同时承载公共 API、模型、客户端运行时和 Spring Boot 自动装配。`autoconfigure` 只是 Starter 内部包名，不是新的 Maven artifact。

下游依赖规则：

1. 业务侧和平台侧继续依赖 `egon-cola-platform-dynamic-config-center-starter`；
2. Admin 继续通过 Starter 共享配置、租约、注册和 Management 契约；
3. BOM 继续只暴露 Starter；
4. 不新增只转发依赖或只保存少量 DTO 的模块。

## 3. 包职责规则

### 3.1 `api`

只保存调用方可以实现、替换或直接调用的接口：

- 客户端端口：`DdcConfigClient`、`DdcManagementClient`、`DdcServiceRegistryClient`；
- 刷新扩展：`DdcConfigApplier`、`DdcConfigApplierRegistry`；
- 注册订阅：`DdcRegistrySubscription`；
- 实例扩展：`DdcInstanceIdProvider`、`DdcInstanceMetadataContributor`。

`api` 不保存 HTTP、Redis、Spring Bean 生命周期或默认实现。

### 3.2 `model`

保存跨模块传递的领域数据：

- `model.config`：配置值、发布消息、ACK、心跳、配置变更事件；
- `model.instance`：实例身份和运行状态；
- `model.lease`：租约会话、角色和操作结果；
- `model.registry`：服务键、实例、注册、查询和快照；
- `model.management`：管理查询、写入、发布和运维视图；
- `model.client`：需要由调用方显式构造的客户端连接和传输安全配置。

`model` 不使用 `pojo` 子包。`pojo` 只描述 Java 实现形式，不表达领域语义。

并非所有普通 Java 对象都进入 `model`。例如 `DdcFieldBinding` 只属于字段绑定实现细节，应保留在 `service.binding`；内部任务、句柄和缓存节点也留在拥有它们的实现包。

### 3.3 `client`

保存三个客户端端口的 HTTP 实现：

- `client.config.HttpDdcConfigClient`；
- `client.management.HttpDdcManagementClient`；
- `client.registry.HttpDdcServiceRegistryClient`；
- `client.http` 保存上述客户端共享的请求签名、TLS、RestClient 和规范化请求能力。

删除 `transport.http`。HTTP 是客户端实现方式，不是与 Redis 平级的业务传输层。

### 3.4 `service`

保存同步编排和业务运行时实现：

- `service.binding`：字段发现、注册和刷新绑定；
- `service.refresh`：配置应用、配置属性重绑定和刷新编排；
- `service.lifecycle`：实例注册、ACK 投递和 Starter 生命周期；
- `service.registry`：服务键创建和注册快照加载。

公共接口放在 `api`，`service` 只保存实现和内部协作者，从结构上消除接口与实现混放。

### 3.5 `listener`、`state` 和 `redis`

- `listener.config` 只处理配置发布事件；
- `listener.registry` 只处理注册目录和实例事件；
- `state` 只保存本地配置、活动注册和租约会话等可变状态；
- `redis` 只保存 Redisson 连接、Key 规则和通用 Topic 订阅资源句柄。

删除 `transport.redis`。Redis 不再和 HTTP 包装在同一个泛化父包下。

## 4. `package-info.java` 与包级契约

### 4.1 覆盖范围

目标树中的每一个 Java 包都必须包含 `package-info.java`，包括只用于组织子包、当前没有普通 Java 类型的中间包。包注解不会自动继承到子包，因此每个文件都必须独立声明包注解。

完整清单如下：

```text
top/egon/cola/component/ddc
├── package-info.java
├── annotation/package-info.java
├── api/package-info.java
│   ├── client/package-info.java
│   ├── extension/package-info.java
│   ├── refresh/package-info.java
│   └── registry/package-info.java
├── model/package-info.java
│   ├── client/package-info.java
│   ├── config/package-info.java
│   ├── instance/package-info.java
│   ├── lease/package-info.java
│   ├── management/package-info.java
│   └── registry/package-info.java
├── client/package-info.java
│   ├── config/package-info.java
│   ├── http/package-info.java
│   ├── management/package-info.java
│   └── registry/package-info.java
├── service/package-info.java
│   ├── binding/package-info.java
│   ├── lifecycle/package-info.java
│   ├── refresh/package-info.java
│   └── registry/package-info.java
├── listener/package-info.java
│   ├── config/package-info.java
│   └── registry/package-info.java
├── state/package-info.java
├── redis/package-info.java
├── configdata/package-info.java
├── environment/package-info.java
├── format/package-info.java
├── observability/package-info.java
├── error/package-info.java
│   ├── http/package-info.java
│   └── management/package-info.java
└── autoconfigure/package-info.java
    └── properties/package-info.java
```

实施过程中如果产生规格之外的新包，必须先说明其独立职责，并同时创建对应的 `package-info.java`；不得先创建空包或使用 `common`、`util`、`impl` 等泛化名称规避该约束。

### 4.2 双语包文档

每个 `package-info.java` 必须包含中文和英文两部分，中文在前、英文在后，并至少说明：

1. 本包承担的唯一职责；
2. 允许包含的类型或扩展点；
3. 明确不属于本包的职责；
4. 与相邻包之间的依赖方向；
5. 包内存在的关键 package-private 类型或约定（如有）。

统一模板如下：

```java
/**
 * DDC 配置客户端公共端口，定义配置拉取、实例生命周期和发布确认能力。
 * 本包只保存调用方可实现或替换的接口；HTTP、Redis 和默认实现分别位于
 * {@code client}、{@code redis} 和 {@code service} 包。
 *
 * <p>Public DDC configuration-client ports for configuration retrieval,
 * instance lifecycle, and publication acknowledgements. This package contains
 * only interfaces that callers may implement or replace; HTTP, Redis, and
 * default implementations belong to {@code client}, {@code redis}, and
 * {@code service}, respectively.</p>
 */
@NonNullApi
package top.egon.cola.component.ddc.api.client;

import org.springframework.lang.NonNullApi;
```

文档必须描述具体包，不允许仅把包名翻译成一句“XX package”，也不使用与包文档无关的 `@Param`、`@Return` 或日期占位标签。

### 4.3 包级注解与可空性

每个 `package-info.java` 使用 Spring 现有的 `org.springframework.lang.NonNullApi`，将本包方法返回值和参数默认声明为非空。不新增只为标记 DDC 包而存在的自定义注解，也不为此引入 JSpecify 或其他依赖。

`@NonNullApi` 是真实 API 契约，不是装饰。迁移时必须同步审计：

- 可能合法返回 `null` 的方法使用 `org.springframework.lang.Nullable`；
- 允许调用方传入 `null` 的参数使用 `@Nullable`；
- 不允许用 `Optional` 与 `@Nullable` 双重表达同一个返回值；
- 不通过无意义的空值替换改变现有行为；
- package-private 方法也遵守所属包的默认非空约定。

本次只使用 `@NonNullApi`，不使用 `@NonNullFields`，避免在没有完成字段级空值模型审计前扩大非空契约。

### 4.4 包可见常量和类型

Java 不支持直接声明“包级变量”，因此禁止在 `package-info.java` 中伪造常量容器或附带普通类型声明。包级私有实现按以下规则表达：

1. 只在单个类内使用的常量继续作为该类的 `private static final` 字段；
2. 同一包内多个类型共享的常量放入职责明确的 package-private `final` 类，字段使用 `static final`；
3. 只供本包协作的类型使用顶层 package-private 类、record、enum 或接口，并放在独立的同名 `.java` 文件中；
4. 不为每个包机械创建空的 `Constants`、`Support` 或 `Internal` 类型；
5. `package-info.java` 的双语文档应说明重要包可见类型和常量约定，但不承载其实现。

能安全收窄的现有实现类型应从 `public` 调整为 package-private；被自动装配跨包直接构造、被下游模块调用或属于 `api`/`model` 契约的类型继续保持必要可见性。

## 5. 目标包树

```text
egon-cola-platform-dynamic-config-center-starter
├── pom.xml
└── src/main
    ├── java/top/egon/cola/component/ddc
    │   ├── annotation
    │   │   ├── DdcRefreshable.java
    │   │   └── DdcValue.java
    │   ├── api
    │   │   ├── client
    │   │   │   ├── DdcConfigClient.java
    │   │   │   ├── DdcManagementClient.java
    │   │   │   └── DdcServiceRegistryClient.java
    │   │   ├── extension
    │   │   │   ├── DdcInstanceIdProvider.java
    │   │   │   └── DdcInstanceMetadataContributor.java
    │   │   ├── refresh
    │   │   │   ├── DdcConfigApplier.java
    │   │   │   └── DdcConfigApplierRegistry.java
    │   │   └── registry
    │   │       └── DdcRegistrySubscription.java
    │   ├── model
    │   │   ├── client
    │   │   │   ├── DdcClientTransportSecurity.java
    │   │   │   └── DdcManagementClientProperties.java
    │   │   ├── config
    │   │   │   ├── DdcAckRequest.java
    │   │   │   ├── DdcAckStatus.java
    │   │   │   ├── DdcConfigFormat.java
    │   │   │   ├── DdcConfigValue.java
    │   │   │   ├── DdcConfigurationChangedEvent.java
    │   │   │   ├── DdcHeartbeatRequest.java
    │   │   │   ├── DdcInstanceRegisterRequest.java
    │   │   │   ├── DdcPublishMessage.java
    │   │   │   └── DdcPublishTarget.java
    │   │   ├── instance
    │   │   │   ├── DdcInstanceIdentity.java
    │   │   │   └── DdcRuntimeState.java
    │   │   ├── lease
    │   │   │   ├── DdcLeaseOperationResult.java
    │   │   │   ├── DdcLeaseOperationStatus.java
    │   │   │   ├── DdcLeaseRole.java
    │   │   │   └── DdcLeaseSession.java
    │   │   ├── management
    │   │   │   ├── DdcInstanceStatus.java
    │   │   │   ├── DdcManagementConfig.java
    │   │   │   ├── DdcManagementConfigClientInstance.java
    │   │   │   ├── DdcManagementConfigDeleteRequest.java
    │   │   │   ├── DdcManagementConfigQuery.java
    │   │   │   ├── DdcManagementConfigUpsertRequest.java
    │   │   │   ├── DdcManagementInstanceQuery.java
    │   │   │   ├── DdcManagementPublishRequest.java
    │   │   │   ├── DdcManagementPublishResult.java
    │   │   │   ├── DdcManagementPublishStatus.java
    │   │   │   ├── DdcManagementPublishTarget.java
    │   │   │   ├── DdcManagementPublishTask.java
    │   │   │   ├── DdcManagementScopeBinding.java
    │   │   │   ├── DdcManagementScopeQuery.java
    │   │   │   ├── DdcManagementServiceCatalog.java
    │   │   │   ├── DdcManagementServiceInstance.java
    │   │   │   ├── DdcManagementServiceKey.java
    │   │   │   ├── DdcManagementServiceQuery.java
    │   │   │   └── DdcManagementServiceSnapshot.java
    │   │   └── registry
    │   │       ├── DdcRegistryEvent.java
    │   │       ├── DdcServiceCatalogSnapshot.java
    │   │       ├── DdcServiceInstance.java
    │   │       ├── DdcServiceKey.java
    │   │       ├── DdcServiceKind.java
    │   │       ├── DdcServiceLeaseRequest.java
    │   │       ├── DdcServiceQuery.java
    │   │       ├── DdcServiceRegistration.java
    │   │       ├── DdcServiceSnapshot.java
    │   │       ├── InstanceHealthState.java
    │   │       └── ServiceInstanceMeta.java
    │   ├── client
    │   │   ├── config
    │   │   │   └── HttpDdcConfigClient.java
    │   │   ├── http
    │   │   │   ├── DdcCanonicalRequest.java
    │   │   │   ├── DdcOpenApiRequestFactory.java
    │   │   │   ├── DdcRequestSigner.java
    │   │   │   └── DdcRestClientFactory.java
    │   │   ├── management
    │   │   │   └── HttpDdcManagementClient.java
    │   │   └── registry
    │   │       └── HttpDdcServiceRegistryClient.java
    │   ├── service
    │   │   ├── binding
    │   │   │   ├── DdcBeanPostProcessor.java
    │   │   │   ├── DdcFieldBinding.java
    │   │   │   ├── DdcFieldBindingService.java
    │   │   │   └── DdcValueBindingRegistry.java
    │   │   ├── lifecycle
    │   │   │   ├── DdcAckDelivery.java
    │   │   │   ├── DdcInstanceIdentityFactory.java
    │   │   │   ├── DdcInstanceService.java
    │   │   │   └── DdcRuntimeCoordinator.java
    │   │   ├── refresh
    │   │   │   ├── DdcConfigurationPropertiesRebinder.java
    │   │   │   ├── DdcRefreshService.java
    │   │   │   ├── DdcYamlConfigApplier.java
    │   │   │   └── DefaultDdcConfigApplierRegistry.java
    │   │   └── registry
    │   │       ├── DdcRegistrySnapshotLoader.java
    │   │       └── DdcServiceKeyFactory.java
    │   ├── listener
    │   │   ├── config
    │   │   │   └── DdcConfigChangeListener.java
    │   │   └── registry
    │   │       ├── DdcCatalogSubscription.java
    │   │       ├── DdcInstanceSubscription.java
    │   │       ├── DdcManagedRegistrySubscription.java
    │   │       └── DdcRegistrySubscriptionCoordinator.java
    │   ├── state
    │   │   ├── DdcActiveRegistrationIndex.java
    │   │   ├── DdcLeaseSessionHolder.java
    │   │   └── DdcLocalConfigState.java
    │   ├── redis
    │   │   ├── DdcRedisClientFactory.java
    │   │   ├── DdcRedisKeys.java
    │   │   └── DdcRedisTopicSubscription.java
    │   ├── configdata
    │   │   ├── DdcConfigDataFetcher.java
    │   │   ├── DdcConfigDataLoader.java
    │   │   ├── DdcConfigDataLocationResolver.java
    │   │   └── DdcConfigDataResource.java
    │   ├── environment
    │   │   ├── DdcDynamicPropertySource.java
    │   │   └── DdcReservedConfigurationKeys.java
    │   ├── format
    │   │   ├── DdcChecksum.java
    │   │   ├── DdcConfigFormatStrategy.java
    │   │   ├── DdcConfigFormatStrategyRegistry.java
    │   │   ├── DdcYamlConfigFormatStrategy.java
    │   │   └── ServiceInstanceMetaCodec.java
    │   ├── observability
    │   │   └── DdcTraceSupport.java
    │   ├── error
    │   │   ├── DdcErrorStatus.java
    │   │   ├── DdcException.java
    │   │   ├── http
    │   │   │   └── DdcOpenApiRequestException.java
    │   │   └── management
    │   │       ├── DdcManagementClientException.java
    │   │       └── DdcManagementErrorCode.java
    │   └── autoconfigure
    │       ├── properties
    │       │   ├── DdcAckDeliveryProperties.java
    │       │   └── DdcProperties.java
    │       ├── DdcAutoConfiguration.java
    │       ├── DdcRedisAutoConfiguration.java
    │       └── DdcRegistryAutoConfiguration.java
    └── resources/META-INF/spring
        └── org.springframework.boot.autoconfigure.AutoConfiguration.imports
```

## 6. 自动装配规则

`autoconfigure` 包保留在 Starter 内部，并遵循以下约束：

1. `DdcAutoConfig` 重命名为 `DdcAutoConfiguration`；
2. `DdcRedisAutoConfig` 重命名为 `DdcRedisAutoConfiguration`；
3. `DdcRegistryAutoConfig` 重命名为 `DdcRegistryAutoConfiguration`；
4. `AutoConfiguration.imports` 只列出上述自动装配类；
5. 删除 `@ComponentScan` 和 `DdcLocalConfigState` 上的 `@Repository`；
6. 所有 Starter Bean 通过明确的 `@Bean` 或精确 `@Import` 注册；
7. 默认实现继续使用 `@ConditionalOnMissingBean` 允许应用覆盖；
8. Redis 和注册能力继续通过现有属性条件独立启停；
9. `DdcProperties` 和 `DdcAckDeliveryProperties` 统一放入 `autoconfigure.properties`。

## 7. 迁移映射

| 当前包 | 目标包 |
| --- | --- |
| `configuration.model` | `model.config`、`format` |
| `lease` | `model.lease` |
| `management.model` | `model.management`、`model.registry`、`format` |
| `registry.model` | `model.registry` |
| `configuration.client.DdcConfigClient` | `api.client` |
| `management.DdcManagementClient` | `api.client` |
| `registry.DdcServiceRegistryClient` | `api.client` |
| `registry.DdcRegistrySubscription` | `api.registry` |
| `configuration.client.HttpDdcConfigClient` | `client.config` |
| `management.client.HttpDdcManagementClient` | `client.management` |
| `registry.client.HttpDdcServiceRegistryClient` | `client.registry` |
| `transport.http` | `client.http`、`model.client`、`error.http` |
| `transport.redis` | `redis` |
| `configuration.bootstrap` | `configdata` |
| `configuration.binding` | `service.binding` |
| `configuration.refresh` | `api.refresh`、`model.config`、`service.refresh` |
| `configuration.runtime` | `api.extension`、`model.instance`、`state`、`service.lifecycle`、`autoconfigure.properties` |
| `configuration.subscription` | `listener.config` |
| `registry.subscription` | `listener.registry`、`service.registry` |
| `registry.state` | `state` |
| `configuration.environment` | `environment` |
| `configuration.format` | `format` |

用户允许破坏式更新，因此旧包直接删除，不增加 deprecated 转发类、继承壳或双包并存过渡期。

## 8. 行为边界

本次只重组模块内代码位置、可见边界和自动装配注册方式，不改变以下行为：

- `ddc:application.yml` ConfigData 加载和 Spring Boot 属性优先级；
- YAML-only 远程配置格式；
- `@DdcValue` 字段刷新和 `@DdcRefreshable` 配置属性重绑定语义；
- 配置客户端注册、默认值上报、拉取、ACK、心跳和下线顺序；
- 服务注册、目录查询、实例订阅和本地租约过期；
- HMAC、mTLS、Trace Header 和 Redis Topic 行为；
- Admin 数据库结构和 Flyway 历史。

如果迁移暴露现有行为错误，先以测试和实际调用链确认；不把无关行为重写混入包迁移提交。

## 9. 设计模式

- 保留配置格式和配置应用器的 Strategy/Registry；
- 保留 Redis Topic 和注册快照的 Observer；
- 三个 HTTP Client 继续作为 `api.client` 端口的 Adapter；
- 自动装配承担对象创建和条件化装配，不增加统一 God Client、Facade 基类、Template Method 或 Abstract Factory；
- 不新增 `biz`、`pojo`、`common`、`impl` 等无法表达稳定职责的泛化包。

## 10. 测试与验收

实现完成后必须满足：

1. Starter 仍是唯一消费入口，没有新增 Maven 模块；
2. 生产源码中不再存在顶层 `configuration`、`lease`、`transport` 或职责混合的 `runtime` 包；
3. 所有公共接口位于 `api`，所有领域数据位于对应 `model` 子包；
4. HTTP 和 Redis 不再共享泛化父包；
5. 目标清单中的每个包都存在包含中英双语职责说明和 `@NonNullApi` 的 `package-info.java`；
6. 所有真实可空的参数和返回值已明确使用 `@Nullable`，不存在与实现矛盾的非空契约；
7. `package-info.java` 不声明常量或普通类型，package-private 类型位于独立源码文件；
8. 自动装配不使用 `@ComponentScan` 或组件扫描发现 Starter Bean；
9. 仓库内不存在旧包 import、旧自动装配类名或过渡兼容类；
10. 包边界测试自动检查生产源码目录与 `package-info.java` 清单一致；
11. Starter 单元测试通过；
12. DDC Admin/Test 模块测试通过；
13. 受影响 Gateway、RPC、IdP、RBAC3 模块至少完成源码编译，相关测试按影响范围执行；
14. README、包边界测试、示例和 SPI 资源同步更新；
15. 不修改现有 Flyway 文件，不新增数据库迁移；
16. 不启动任何应用进程。

## 11. 提交策略

按可独立验证的迁移任务提交：

1. 先迁移公共 API、模型和所有仓库内消费者；
2. 再迁移 HTTP、Redis、ConfigData、服务、监听和状态实现；
3. 最后重写自动装配注册、补齐逐包 `package-info.java`、删除旧包并更新文档和边界测试；
4. 每个任务使用路径限定提交，保留用户已有未提交修改。

每个提交必须保持当前任务范围内源码可编译；若破坏式包迁移无法在单个提交内编译，则将强耦合移动与消费者更新合并为同一任务提交。
