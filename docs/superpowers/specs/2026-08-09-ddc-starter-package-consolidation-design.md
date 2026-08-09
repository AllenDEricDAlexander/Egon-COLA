# DDC Starter 领域分包与基础设施收敛设计

状态：已确认，进入实施

编写日期：2026-08-09

代码基线：`main@84c908c4`

主要涉及模块：

- `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-starter`
- `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin`
- `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-test`
- 所有直接依赖 DDC Starter 公共类型的 Gateway、RPC、IdP、RBAC3 和测试模块

本文记录用户于 2026-08-09 确认的破坏式重组方案。实现必须一次性迁移仓库内消费者，不保留旧包兼容壳，不修改数据库和 Flyway，不启动应用。

---

## 1. 目标结论

DDC Starter 保持单一 Maven 模块和消费侧唯一入口，不再增加独立 client 或 infrastructure 模块。本次重构按领域能力组织 Java 包：

1. `configuration` 承载远程配置的 ConfigData、运行时、刷新、绑定、格式和订阅；
2. `registry` 承载服务注册、发现和服务快照订阅；
3. `management` 承载配置写入、发布和运维查询契约；
4. `transport` 只承载多个领域共同使用的 HTTP、HMAC、TLS 和 Redis Topic 基础设施；
5. `lease` 承载配置客户端租约和服务实例租约共享的值对象；
6. `autoconfigure` 只承载 Spring Boot 自动装配与属性；
7. 删除没有生产调用方的 `DdcRedisConfigRepository`；
8. 删除旧的顶层 `bootstrap`、`client`、`common`、`config`、`environment`、`format`、`listener`、`model`、`refresh`、`repository`、`service`、`trace` 包。

目标不是减少领域契约数量，而是消除技术基础设施重复和无语义的顶层分层包。

## 2. 客户端边界

保留三套领域客户端：

- `configuration.client.DdcConfigClient`：配置客户端注册、心跳、下线、拉取和 ACK；原 `DdcAdminClient` 改名，避免与管理接口混淆；
- `registry.DdcServiceRegistryClient`：服务实例注册、心跳、注销、发现和订阅；公共包名保持不变以减少 Gateway/RPC 迁移噪声；
- `management.DdcManagementClient`：配置写入、发布、任务和运维查询；保持独立错误模型和权限边界。

原 `DdcBootstrapClient` 改为 `configuration.bootstrap.DdcConfigDataFetcher`。它仍由 Spring Boot BootstrapContext 管理，不依赖 ApplicationContext Bean，但复用共享 HTTP 请求基础设施。

不创建统一 `DdcClient`，也不使用继承式 HTTP 客户端基类。

## 3. HTTP 收敛

新增 `transport.http.DdcOpenApiRequestFactory`，统一：

- JSON 请求序列化；
- 规范查询字符串和 URI；
- 时间戳、nonce、正文摘要和 HMAC Header；
- 可选签名；
- Trace Header 注入。

保留 `DdcRestClientFactory` 和 `DdcClientTransportSecurity`，移动到 `transport.http`。领域客户端继续拥有端点路径、查询参数、响应是否允许空数据以及领域异常映射，避免通用 Transport 理解配置、注册或管理业务码。

## 4. Redis 收敛

配置变更订阅和服务注册订阅当前使用同一份 `DdcProperties.Redis`，因此只创建一个名为 `ddcRedissonClient` 的 RedissonClient。自动装配必须在以下任一条件成立时创建它：

- DDC 配置运行时启用且 `redis.enabled=true`；
- `registry.enabled=true`。

配置运行时和注册中心继续独立启停。共享连接不允许把 `registry.enabled` 与配置客户端 `enabled` 绑定为同一个业务开关。

新增泛型 `transport.redis.DdcRedisTopicSubscription<T>`，统一 Topic 监听器注册、部分失败回滚、活动状态和幂等关闭。配置消息校验留在 `configuration.subscription.DdcConfigChangeListener`；注册事件解析、合并刷新和周期对账留在 `registry.subscription`。

## 5. 注册订阅拆分

原 `DdcRegistrySubscriptionManager` 同时处理 Topic、调度、实例快照、目录快照和本地过期。本次拆成：

- `DdcRegistrySubscriptionCoordinator`：持有调度器、订阅集合和关闭生命周期；
- `DdcManagedRegistrySubscription<T>`：共享事件刷新合并、定时对账和监听器隔离；
- `DdcInstanceSubscription`：单服务实例快照和本地租约过期；
- `DdcCatalogSubscription`：服务目录快照；
- `DdcRegistrySnapshotLoader`：只读 HTTP 查询端口，避免 Subscription 依赖包含自身的完整 Registry Facade。

公共 `DdcRegistrySubscription` 和 `DdcServiceRegistryClient` 契约不变。

## 6. 目标包结构

```text
top.egon.cola.component.ddc
├── annotation
├── autoconfigure
├── configuration
│   ├── bootstrap
│   ├── binding
│   ├── client
│   ├── environment
│   ├── format
│   ├── model
│   ├── refresh
│   ├── runtime
│   └── subscription
├── error
├── lease
├── management
│   ├── client
│   └── model
├── observability
├── registry
│   ├── client
│   ├── model
│   ├── state
│   └── subscription
└── transport
    ├── http
    └── redis
```

公共扩展点位于对应领域根包或明确子包，不新增无语义的 `common`、`service`、`repository` 或全局 `model`。

## 7. 设计模式

- 保留配置格式和配置应用器的 Strategy/Registry；
- 领域客户端使用 Facade；
- Redis 通知继续使用 Observer；
- HTTP 与 Redis 基础设施使用组合式 Adapter；
- 不引入 Template Method、Abstract Factory、统一 God Client 或新 Maven 模块。

## 8. 兼容与迁移

用户允许破坏式更新，因此旧类名和旧包名直接删除，不提供 deprecated 转发类。所有仓库内 Java import、Spring SPI 文件、AutoConfiguration imports、测试、README 和示例配置必须同步更新。

`registry.DdcServiceRegistryClient`、`registry.DdcRegistrySubscription` 和 `management.DdcManagementClient` 的领域包保持稳定。`service.DdcConfigApplier*`、`service.DdcRuntimeCoordinator`、`client.DdcAdminClient` 等旧路径一次性迁往 `configuration` 领域。

## 9. 验收

1. Starter 生产源码不再包含旧顶层技术包；
2. 仓库内不存在旧包 import 或旧类名 `DdcAdminClient`、`DdcBootstrapClient`；
3. 配置运行时和服务注册共用一个 RedissonClient；
4. 两类 Redis Topic 订阅复用同一个泛型资源句柄；
5. `DdcRedisConfigRepository` 被删除；
6. ConfigData 启动、配置刷新、ACK、服务注册和订阅行为保持现有测试语义；
7. DDC Starter/Admin/Test 以及受影响 Gateway、RPC、IdP、RBAC3 模块完成源码/Maven 验证；
8. 不运行服务器、浏览器或数据库迁移。
