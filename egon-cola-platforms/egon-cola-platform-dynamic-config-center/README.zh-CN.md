# Egon COLA 动态配置中心

[English](README.md) | [中文](README.zh-CN.md)

## 范围

`egon-cola-platform-dynamic-config-center` 提供基于 Spring Boot ConfigData、每个作用域
一份 YAML 业务配置文档的 SDK、类型化管理 API、可独立部署的 Admin 应用，以及面向
RPC Provider 和内部 Gateway 的 Redis 服务注册中心。

Maven 模块统一使用 `egon-cola-platform-*` 前缀。Starter Java API 已按领域重新组织，
不为旧技术分层包保留转发类型；外部 `egon.cola.component.ddc` 配置命名空间保持不变。

V1 支持由共享 PostgreSQL 和 Redis 支撑的一个逻辑控制面。多个 Admin 进程可以服务
同一个控制面：发布准备通过 PostgreSQL 行锁、版本条件更新和持久化发布任务完成，
等待完成时则回退为轮询共享任务状态。不实现 Raft、Leader 选举、共识日志或分布式锁
服务。Admin 和 SDK 的 Redis 客户端通过 Redisson 支持 `SINGLE`、`SENTINEL` 和
`CLUSTER` 拓扑。PostgreSQL 是生产数据库，SQLite 仅保留给测试使用。服务注册信息
是 Redis 中的临时租约状态，不新增 JPA Entity 或数据库表。

## 部署拓扑

```text
配置客户端 ──直连 gRPC/HMAC──┐
RPC Provider ────直连 gRPC/HMAC──┼──> DDC 逻辑目标 ──> Admin 集合 ──> PostgreSQL
内部 Gateway ────直连 gRPC/HMAC──┘                               │
                                                                       └──> 共享 Redis
配置客户端 <──────── Redis Pub/Sub ────────┘
注册订阅方 <──────── Redis Pub/Sub ────────┘
```

Admin 进程是唯一的机器控制面 RPC Provider。客户端通过本地配置的
`dns:///`、VIP 或负载均衡目标直连，不通过 DDC 服务发现引导 DDC 自身。
Admin HTTP 仅保留给人工管理 API 和 Actuator 健康检查。各 Admin 共享 PostgreSQL 和 Redis；已完成
发布的事实不依赖某个 Admin 的本地状态。Redis 保存配置缓存、发布通知、配置客户端
当前租约和服务注册租约。PostgreSQL 保存配置、版本、发布、ACK、操作日志和配置
客户端管理投影数据。

## 模块

| 模块 | 职责 |
|---|---|
| `egon-cola-platform-dynamic-config-center-starter` | 传输无关 SDK 运行时与端口：ConfigData、`@DdcValue`、选择性刷新、ACK、租约、管理和注册契约 |
| `egon-cola-component-rpc-ddc-adapter` | 位于 `components/rpc` 的组装适配器：Protobuf 契约、直连 gRPC Client/Provider、HMAC Metadata 和 Spring Boot 装配 |
| `egon-cola-platform-dynamic-config-center-admin` | 人工 REST Admin 与直连 gRPC Facade、PostgreSQL 持久化、Redis 缓存/租约和同步发布状态机 |
| `egon-cola-platform-dynamic-config-center-admin-web` | 独立管理控制台（React + antd + Vite，纯 Node 工程，不进 Maven reactor）；构建与部署说明见 `egon-cola-platform-dynamic-config-center-admin-web/README.md` |
| `egon-cola-platform-dynamic-config-center-test` | 仅依赖 Starter 的样例与黑盒消费端验证，不依赖 Admin |

Admin 的 webui 已从 jar 中摘出（`/ddc-admin` 不再由 Admin 提供服务）：管理控制台以
独立容器部署，经 `DDC_ADMIN_API_BASE_URL` 指向 Admin，`/api` 请求由 static-server
同源反代，Admin 侧无需 CORS 配置。

## Starter 分包

```text
top.egon.cola.component.ddc
├── annotation
├── autoconfigure
├── configuration
│   ├── binding
│   ├── bootstrap
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
    └── redis
```

`DdcConfigClient`、`DdcServiceRegistryClient`、`DdcManagementClient` 仍是三套独立
领域门面，RPC-DDC Adapter 通过三个 unary gRPC Service 实现它们。Starter 不依赖
RPC，依赖方向固定为 `rpc-starter <- rpc-ddc-adapter -> ddc-starter`。

## 运维端点

DDC Admin 使用 `GET /actuator/health/readiness` 作为启动与就绪探针。
`GET /actuator/info` 在 `app.name` 和 `app.version` 中暴露应用名称与 Maven
过滤后的组件版本。

可执行业务应用引入 RPC-DDC Adapter，并通过 `spring.config.import` 导入 `ddc:application.yml`。
`egon.cola.component.ddc.enabled=true` 会在 ConfigData 阶段加载远端 YAML，随后启动
`CONFIG_CLIENT` 注册、配置拉取、Redis 订阅、心跳和停机下线闭环；
`egon.cola.component.ddc.registry.enabled=true` 独立启用 RPC/Gateway 服务注册，
其中 `RPC_PROVIDER`、`HTTP_PROVIDER` 和 `INTERNAL_GATEWAY` 租约不是配置客户端注册。启用任一远程
路径时都必须在本地显式配置直连 RPC 目标、匹配的最小权限 HMAC 凭据和 Redis 拓扑。
`redis.enabled=false` 时不会执行注册、拉取、订阅、心跳或 ACK。生产多 Admin
入口由外部 DNS、VIP 或支持 HTTP/2 的负载均衡提供，客户端使用
`round_robin`。DDC 不发现自身 Admin，也不保留机器 HTTP 兼容端点。

```xml
<dependency>
    <groupId>top.egon</groupId>
    <artifactId>egon-cola-component-rpc-ddc-adapter</artifactId>
    <version>5.3.3</version>
</dependency>
```

## Trace 传播

Starter 和 RPC-DDC Adapter 的 gRPC Facade 调用、心跳、拉取、ACK 重试、Redis
Topic 回调和租约恢复任务统一使用 `egon-cola-component-common-trace`。业务请求触发的
DDC 调用会继承当前 `traceId` 并创建 child span；后台任务没有上游 Trace 时会为每次
逻辑操作打开新的 `TraceContext`，并在结束后恢复原线程 MDC。出站只写
`traceparent`、`tracestate` 和 `x-egon-request-id`，不写 `x-egon-trace-id`。

Spring MVC Admin 侧直接引入 `egon-cola-component-common-trace-spring-boot-starter`，不再维护
DDC 私有 Trace Filter。

## 配置客户端生命周期

初始远端 YAML 由 Spring Boot ConfigData 在 Bean 绑定前加载。之后
`DdcRuntimeCoordinator` 只在 Redis 订阅已经可用后启动，并严格按以下顺序执行：

1. 注册配置客户端并取得新的 `leaseId`；
2. 拉取通过 ConfigData 导入的 YAML 资源并与启动快照校准；
3. 进入 `READY`；
4. 按配置周期发送心跳；
5. 停止时主动下线当前租约。

每次注册都会替换旧租约。心跳和下线原子匹配
`instanceId + leaseId`，旧租约不能续期或删除替换后的新租约。当前租约丢失或
不匹配时，SDK 会重新注册并重复首次同步。

远端 `application.yml` 可以使用任意层级，例如：

```yaml
order:
  rate-limit:
    permits-per-second: 200
  downgrade:
    enabled: false
```

`@DdcValue` 与 Spring `@Value` 使用相同表达式语义，层级由点路径访问，冒号后的
值只在属性不存在时作为默认值；字段类型由 Spring 转换服务推断：

```java
@DdcValue("${order.rate-limit.permits-per-second:100}")
private volatile Integer permitsPerSecond;

@DdcValue("${order.downgrade.enabled:false}")
private volatile Boolean downgradeEnabled;
```

运行期发布会原子替换 DDC PropertySource，并按 YAML 叶子计算差异。显式注册的
`DdcConfigApplier` 与允许刷新的 `@DdcValue` 字段接收对应叶子；只有标注
`@DdcRefreshable` 且采用 setter 绑定的 `@ConfigurationProperties` 会被重新绑定。
不可变配置和其他变更会被标记为需要重启，但不会重启 ApplicationContext。

## 租约协议

所有角色复用同一注册、心跳和注销语义，由调用方使用各自的默认参数：

| 角色 | 默认租约 | 默认心跳 | 存储 |
|---|---:|---:|---|
| `CONFIG_CLIENT` | 30 秒 | 10 秒 | Redis 租约，加 `ddc_instance` 管理投影 |
| `RPC_PROVIDER` | 30 秒 | 10 秒 | 仅 Redis |
| `INTERNAL_GATEWAY` | 15 秒 | 5 秒 | 仅 Redis |

Admin 接受 5～300 秒租约，心跳周期必须短于租约。每次 Register 都生成新的
`leaseId`。Redis Bucket TTL 是当前租约事实；Heartbeat 不会隐式重建丢失租约。

## 服务注册中心

服务身份为：

```text
env + namespace + serviceKind + serviceName + group + version + protocol
```

支持的 `serviceKind` 是 `RPC_PROVIDER` 和 `INTERNAL_GATEWAY`。注册信息包含
`instanceId`、主机、端口、secure 标志、元数据、租约秒数和心跳周期。
元数据有数量和长度边界，并拒绝保留前缀与敏感字段。

直连 unary gRPC Service：

| Service | 操作 |
|---|---|
| `DdcConfigRuntimeService` | 注册、心跳、下线、拉取、发布 ACK |
| `DdcServiceRegistryService` | 注册、心跳、注销、实例快照、服务目录 |
| `DdcManagementService` | 配置 CRUD、发布/任务、配置客户端、Scope 和注册信息查询 |

`DdcServiceRegistryClient` 同时提供实例快照和服务目录订阅。Redis Revision 和
Pub/Sub 通知会触发校准，过期实例从快照中摘除。Redis 重启后注册状态按设计
丢失，客户端应使用新租约重新注册。

## 同步发布

V1 只有一种发布模式：`SYNC_ALL_ACK`。

调用方传入 UUIDv7 `changeId`。准备事务会对配置行加悲观锁，拒绝同一资源已有的
活跃任务，更新配置版本，并将 Redis 当前配置客户端租约固化为精确的
`instanceId + leaseId` 目标集合。数据库任务和行条件更新负责协调
`bizCode + env + appCode + resourceName` 的并发 Admin；进程内 Waiter 只负责唤醒优化。
完成轮询读取共享任务状态；Admin 启动恢复会把过期的 `PENDING` 或 `PUBLISHING`
任务置为 `UNKNOWN`，之后由其他请求重试。

ACK 只有同时匹配以下内容才会被接受：

- `changeId`；
- 目标 `instanceId + leaseId`；
- 目标配置版本；
- 覆盖 `resourceName + format + content` 的资源 SHA-256 摘要。

发布调用只在所有目标都返回 `SUCCESS`，或进入终态后返回：

| 状态 | 含义 |
|---|---|
| `SUCCESS` | 所有固化目标都成功确认 |
| `FAILED` | 准备、分发、目标校验或目标 ACK 失败 |
| `TIMEOUT` | 截止时间前未收到全部成功 ACK |
| `UNKNOWN` | Admin 在任务活跃期间重启 |

`POST /api/v1/ddc/publish-tasks/{changeId}/retry` 对 `FAILED`、`TIMEOUT`、
`UNKNOWN` 任务执行幂等重试。Retry 固定复用原目标，不重新快照当前在线实例。
任一原目标租约失效时，Retry 保持失败并记录目标租约错误。

发布请求：

```bash
curl -X POST \
  'http://localhost:18080/api/v1/ddc/configs/{configId}/publish?operator=admin' \
  -H 'Content-Type: application/json' \
  -d '{
    "changeId": "019c9f0d-7b9b-7e00-8000-000000000001",
    "content": "order:\n  rate-limit:\n    permits-per-second: 200\n",
    "expectedVersion": 1,
    "timeoutMs": 30000
  }'
```

该调用有意保持同步。调用方丢失响应时，可用同一 `changeId` 请求
`GET /api/v1/ddc/publish-tasks/{changeId}` 查询持久化结果。

## RPC HMAC

`signature-enabled` 为 true 时，HMAC 保护所有已发布 DDC unary 方法。
必需 gRPC Metadata：

| Header | 值 |
|---|---|
| `x-egon-ddc-access-key` | 配置的 access key |
| `x-egon-ddc-timestamp` | Unix 毫秒时间戳 |
| `x-egon-ddc-nonce` | 请求唯一随机数 |
| `x-egon-ddc-content-sha256` | protobuf 确定性序列化字节的小写 SHA-256 |
| `x-egon-ddc-signature` | 规范请求的 HMAC-SHA256 |
| `x-egon-ddc-contract-version` | `v1` |

规范请求由五个换行分隔的字段组成：

```text
v1
完整 gRPC 方法名
timestamp
nonce
content-sha256
```

Admin 会检查已知方法/操作映射、契约版本、时间偏移、Access Key、
确定性请求体摘要、签名、Nonce 重放、客户端类型和 Scope。Runtime、Registry
和 Management 客户端使用独立凭据。

## 配置

从已删除机器 HTTP 传输迁移是有意的破坏性变更：

| 已删除前缀 | 已删除叶子 | 直连 RPC 替代项 |
|---|---|---|
| `egon.cola.component.ddc.admin` | `endpoint` | `egon.cola.component.ddc.rpc.target` |
| `egon.cola.component.ddc.admin` | `tls.*` | `egon.cola.component.ddc.rpc.tls.*` |
| `egon.cola.component.ddc.admin` | `access-key` / `secret-key` | 按能力使用 `egon.cola.component.ddc.rpc.auth.runtime.*` 或 `.registry.*` |
| `gateway.admin.ddc` | `endpoint` 和 HMAC Key | `egon.cola.component.ddc.rpc.target` 与 `.auth.management.*` |
| `egon.cola.component.ddc.admin.openapi` | `signature-enabled` / `credentials` | `egon.cola.component.ddc.admin.rpc.signature-enabled` / `.credentials` |

不提供兼容别名。凭据通过环境注入，Runtime、Registry 和 Management 使用
不同的 Access Key/Secret 对。

业务应用：

```yaml
spring:
  config:
    import: ddc:application.yml

egon:
  cola:
    component:
      ddc:
        enabled: true
        biz-code: orders
        app-code: order-service
        env: dev
        namespace: default
        rpc:
          target: dns:///ddc-admin.example.internal:19080
          load-balancing-policy: round_robin
          tls:
            enabled: true
            development-plaintext: false
            certificate-chain-path: ${DDC_CLIENT_CERTIFICATE}
            private-key-path: ${DDC_CLIENT_PRIVATE_KEY}
            trust-certificate-collection-path: ${DDC_TRUST_CERTIFICATE}
          auth:
            runtime:
              access-key: ${DDC_RUNTIME_ACCESS_KEY}
              secret-key: ${DDC_RUNTIME_SECRET_KEY}
            registry:
              access-key: ${DDC_REGISTRY_ACCESS_KEY}
              secret-key: ${DDC_REGISTRY_SECRET_KEY}
        redis:
          mode: SINGLE
          nodes: []
          master-name:
          enabled: true
          host: 127.0.0.1
          port: 6379
          database: 0
        instance:
          lease-seconds: 30
          heartbeat-interval-seconds: 10
        registry:
          enabled: true
          reconcile-interval-seconds: 10
        consistency:
          fail-fast: true
```

允许远端文档不存在时可使用 `optional:ddc:application.yml`。DDC 只贡献一个
PropertySource，其优先级高于本地 ConfigData、低于 Spring Boot 的命令行参数和系统
属性；本地与远端文档不做自定义合并。Admin 在每个 `bizCode + env + appCode` 下只接受
一份名为 `application.yml` 或 `application.yaml` 的单文档、Map 根节点 YAML；
`spring.config.import` 中的资源名必须与 Admin 保存的 `resourceName` 一致。远端 YAML 一旦包含
`egon.cola.component.ddc.*`、`spring.config.*` 或 Spring Profile 控制键，整份文档都会
被拒绝，因此 DDC 连接和引导参数只能来自本地配置。

配置格式以 `DdcConfigFormatStrategy` 和 `DdcConfigFormatStrategyRegistry` 作为扩展点。
当前内置策略只有 YAML，并由 Spring Boot `YamlPropertySourceLoader` 负责解析；JSON、
Properties、TOML 等格式没有兼容实现，也不会被注册表接受。

生产 Admin：

```yaml
server:
  port: 18080

spring:
  datasource:
    url: jdbc:postgresql://127.0.0.1:5432/egon_ddc
    username: ${DDC_DB_USERNAME}
    password: ${DDC_DB_PASSWORD}
  flyway:
    locations: classpath:db/postgresql

egon:
  cola:
    component:
      ddc:
        enabled: false
        admin:
          transport-security:
            mode: DEVELOPMENT_PLAINTEXT
          redis:
            mode: SINGLE
            nodes: []
            master-name:
            enabled: true
            host: 127.0.0.1
            port: 6379
            database: 0
          lease:
            minimum-seconds: 5
            maximum-seconds: 300
          rpc:
            signature-enabled: true
            credentials:
              - credential-id: runtime
                access-key: ${DDC_RUNTIME_ACCESS_KEY}
                secret: ${DDC_RUNTIME_SECRET_KEY}
                client-type: SDK
                app-code-patterns: [gateway-engine-*]
                env-patterns: [local]
                biz-code-patterns: [infra]
                namespace-patterns: [default]
                allowed-operations: [SDK_REGISTER, SDK_HEARTBEAT,
                  SDK_OFFLINE, CONFIG_PULL, PUBLISH_ACK]
              - credential-id: registry
                access-key: ${DDC_REGISTRY_ACCESS_KEY}
                secret: ${DDC_REGISTRY_SECRET_KEY}
                client-type: REGISTRY
                app-code-patterns: [gateway-*]
                env-patterns: [local]
                biz-code-patterns: [infra]
                namespace-patterns: [default]
                allowed-operations: [REGISTRY_REGISTER,
                  REGISTRY_HEARTBEAT, REGISTRY_DEREGISTER, REGISTRY_READ]
              - credential-id: management
                access-key: ${DDC_MANAGEMENT_ACCESS_KEY}
                secret: ${DDC_MANAGEMENT_SECRET_KEY}
                client-type: MANAGEMENT
                app-code-patterns: [gateway-engine-*]
                env-patterns: [local]
                biz-code-patterns: [infra]
                namespace-patterns: [default]
                allowed-operations: [MANAGEMENT_CONFIG_READ,
                  MANAGEMENT_CONFIG_WRITE, MANAGEMENT_PUBLISH,
                  MANAGEMENT_TASK_READ, MANAGEMENT_TASK_RETRY,
                  MANAGEMENT_INSTANCE_READ, MANAGEMENT_SCOPE_READ,
                  MANAGEMENT_REGISTRY_READ]
          publish:
            dispatch-timeout-ms: 5000
            default-timeout-ms: 30000
            max-timeout-ms: 60000
            scan-interval-ms: 1000
            completion-poll-interval-ms: 100
            recovery-stale-ms: 120000
      rpc:
        enabled: true
        provider:
          enabled: true
          port: 19080
          registration-mode: DISABLED
        consumer:
          enabled: false
        tls:
          enabled: true
          development-plaintext: false
          certificate-chain-path: ${DDC_SERVER_CERTIFICATE}
          private-key-path: ${DDC_SERVER_PRIVATE_KEY}
          trust-certificate-collection-path: ${DDC_TRUST_CERTIFICATE}
```

`test` Profile 使用 SQLite `create-drop`，并关闭 Flyway 和 Admin Redis
连接；这不是生产存储拓扑。

## 构建和验证

```bash
./mvnw -B -ntp \
  -pl :egon-cola-component-rpc-ddc-adapter,:egon-cola-platform-dynamic-config-center-admin,:egon-cola-platform-dynamic-config-center-test \
  -am clean test

./mvnw -B -ntp \
  -pl :egon-cola-platform-dynamic-config-center-admin,:egon-cola-component-rpc-ddc-adapter \
  -am package -DskipTests
```

## 明确边界

完整的 DDC + Gateway + RPC 启动顺序、凭据、租约演练和运行证据见
[开发联调 Runbook](../egon-cola-platform-gateway/docs/developer-integration.zh-CN.md)。

- 不支持 Raft、Leader 选举、共识日志或成员协议；
- 多 Admin 运行要求共享 PostgreSQL 和 Redis；平台不负责提供数据库或 Redis HA；
- DDC 使用直连逻辑 RPC 目标和客户端/外部轮询；不注册或发现自身、不要求粘性会话、不通过 gRPC 流式下发配置；
- 不提供分布式共识或通用分布式锁服务；
- 不内嵌 Redis，不使用数据库持久化服务注册信息；
- 不内嵌 Admin UI 或账号系统；独立 Admin Web 接入平台统一身份与授权，且不以兼容
  MySQL 为目标；
- V1 不支持异步、多数派或部分成功发布模式。
