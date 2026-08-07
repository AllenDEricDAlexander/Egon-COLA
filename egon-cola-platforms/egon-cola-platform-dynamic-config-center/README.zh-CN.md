# Egon COLA 动态配置中心

[English](README.md) | [中文](README.zh-CN.md)

## 范围

`egon-cola-platform-dynamic-config-center` 提供包含类型化管理 API 的动态配置 SDK、
可独立部署的 Admin 应用，以及面向 RPC Provider 和内部 Gateway 的 Redis 服务注册中心。

Maven 模块统一使用 `egon-cola-platform-*` 前缀。为保持源码和配置兼容，现有 Java 包名
以及 `egon.cola.component.ddc` 配置命名空间保持不变。

V1 支持由共享 PostgreSQL 和 Redis 支撑的一个逻辑控制面。多个 Admin 进程可以服务
同一个控制面：发布准备通过 PostgreSQL 行锁、版本条件更新和持久化发布任务完成，
等待完成时则回退为轮询共享任务状态。不实现 Raft、Leader 选举、共识日志或分布式锁
服务。Admin 和 SDK 的 Redis 客户端通过 Redisson 支持 `SINGLE`、`SENTINEL` 和
`CLUSTER` 拓扑。PostgreSQL 是生产数据库，SQLite 仅保留给测试使用。服务注册信息
是 Redis 中的临时租约状态，不新增 JPA Entity 或数据库表。

## 部署拓扑

```text
配置客户端 ─────────HTTP/HMAC──┐
RPC Provider ──────HTTP/HMAC──┼──> 一个或多个 DDC Admin ──> PostgreSQL
内部 Gateway ──────HTTP/HMAC──┘             │
                                             └──> 共享 Redis
配置客户端 <──────── Redis Pub/Sub ────────┘
注册订阅方 <──────── Redis Pub/Sub ────────┘
```

Admin 进程是唯一的管理和租约 API 入口。各 Admin 共享 PostgreSQL 和 Redis；已完成
发布的事实不依赖某个 Admin 的本地状态。Redis 保存配置缓存、发布通知、配置客户端
当前租约和服务注册租约。PostgreSQL 保存配置、版本、发布、ACK、操作日志和配置
客户端管理投影数据。

## 模块

| 模块 | 职责 |
|---|---|
| `egon-cola-platform-dynamic-config-center-starter` | 唯一业务侧 SDK：`@DdcValue`、类型化管理 API、启动同步、刷新、ACK、CONFIG_CLIENT 租约、HMAC 和服务注册契约 |
| `egon-cola-platform-dynamic-config-center-admin` | 独立 REST Admin、PostgreSQL 持久化、Redis 缓存与租约、注册中心 API 和同步发布状态机 |
| `egon-cola-platform-dynamic-config-center-admin-web` | 独立管理控制台（React + antd + Vite，纯 Node 工程，不进 Maven reactor）；构建与部署说明见 `egon-cola-platform-dynamic-config-center-admin-web/README.md` |
| `egon-cola-platform-dynamic-config-center-test` | 仅依赖 Starter 的样例与黑盒消费端验证，不依赖 Admin |

Admin 的 webui 已从 jar 中摘出（`/ddc-admin` 不再由 Admin 提供服务）：管理控制台以
独立容器部署，经 `DDC_ADMIN_API_BASE_URL` 指向 Admin，`/api` 请求由 static-server
同源反代，Admin 侧无需 CORS 配置。

## 运维端点

DDC Admin 使用 `GET /actuator/health/readiness` 作为启动与就绪探针。
`GET /actuator/info` 在 `app.name` 和 `app.version` 中暴露应用名称与 Maven
过滤后的组件版本。

业务应用只引入 Starter。`egon.cola.component.ddc.enabled=true` 会显式启动
`CONFIG_CLIENT` 注册、默认值上报、配置拉取、Redis 订阅、心跳和停机下线闭环；
`egon.cola.component.ddc.registry.enabled=true` 独立启用 RPC/Gateway 服务注册，
其中 `RPC_PROVIDER`、`HTTP_PROVIDER` 和 `INTERNAL_GATEWAY` 租约不是配置客户端注册。启用任一远程
路径时都必须显式配置 Admin Endpoint、匹配的 HMAC 凭据和 Redis 拓扑。
`redis.enabled=false` 时不会执行注册、拉取、订阅、心跳或 ACK。生产多 Admin
入口由外部 DNS、VIP 或负载均衡提供，Starter 不负责发现 Admin 进程。

```xml
<dependency>
    <groupId>top.egon</groupId>
    <artifactId>egon-cola-platform-dynamic-config-center-starter</artifactId>
    <version>5.3.3</version>
</dependency>
```

## Trace 传播

Starter 的 `HttpDdcAdminClient`、OpenAPI 注册客户端、心跳、拉取、ACK 重试、Redis
Topic 回调和租约恢复任务统一使用 `egon-cola-component-common-trace`。业务请求触发的
DDC 调用会继承当前 `traceId` 并创建 child span；后台任务没有上游 Trace 时会为每次
逻辑操作打开新的 `TraceContext`，并在结束后恢复原线程 MDC。出站只写
`traceparent`、`tracestate` 和 `x-egon-request-id`，不写 `x-egon-trace-id`。

Spring MVC Admin 侧直接引入 `egon-cola-component-common-trace-spring-boot-starter`，不再维护
DDC 私有 Trace Filter。

## 配置客户端生命周期

`DdcRuntimeCoordinator` 只在 Redis 订阅已经可用后启动，并严格按以下顺序执行：

1. 注册配置客户端并取得新的 `leaseId`；
2. 上报注解默认值；
3. 拉取并应用完整配置快照；
4. 进入 `READY`；
5. 按配置周期发送心跳；
6. 停止时主动下线当前租约。

每次注册都会替换旧租约。心跳和下线原子匹配
`instanceId + leaseId`，旧租约不能续期或删除替换后的新租约。当前租约丢失或
不匹配时，SDK 会重新注册并重复首次同步。

```java
@DdcValue("rateLimit:100")
private volatile Integer rateLimit;

@DdcValue(value = "", key = "downgradeSwitch",
        defaultValue = "false", type = Boolean.class)
private volatile Boolean downgradeSwitch;
```

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

OpenAPI：

| 方法和路径 | 用途 |
|---|---|
| `POST /api/v1/ddc/openapi/registry/instances/register` | 注册并取得新租约 |
| `POST /api/v1/ddc/openapi/registry/instances/heartbeat` | 仅续期匹配的当前租约 |
| `POST /api/v1/ddc/openapi/registry/instances/deregister` | 仅删除匹配的当前租约 |
| `GET /api/v1/ddc/openapi/registry/instances` | 查询一个服务 Key 的稳定有效实例快照 |
| `GET /api/v1/ddc/openapi/registry/services` | 查询有效服务目录 |

`DdcServiceRegistryClient` 同时提供实例快照和服务目录订阅。Redis Revision 和
Pub/Sub 通知会触发校准，过期实例从快照中摘除。Redis 重启后注册状态按设计
丢失，客户端应使用新租约重新注册。

## 同步发布

V1 只有一种发布模式：`SYNC_ALL_ACK`。

调用方传入 UUIDv7 `changeId`。准备事务会对配置行加悲观锁，拒绝同一资源已有的
活跃任务，更新配置版本，并将 Redis 当前配置客户端租约固化为精确的
`instanceId + leaseId` 目标集合。数据库任务和行条件更新负责协调
`appCode + env + namespace + configKey` 的并发 Admin；进程内 Waiter 只负责唤醒优化。
完成轮询读取共享任务状态；Admin 启动恢复会把过期的 `PENDING` 或 `PUBLISHING`
任务置为 `UNKNOWN`，之后由其他请求重试。

ACK 只有同时匹配以下内容才会被接受：

- `changeId`；
- 目标 `instanceId + leaseId`；
- 目标配置版本；
- 内容 SHA-256 摘要。

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
    "configValue": "200",
    "expectedVersion": 1,
    "timeoutMs": 30000
  }'
```

该调用有意保持同步。调用方丢失响应时，可用同一 `changeId` 请求
`GET /api/v1/ddc/publish-tasks/{changeId}` 查询持久化结果。

## OpenAPI HMAC

`signature-enabled` 为 true 时，HMAC 保护
`/api/v1/ddc/openapi/` 下的所有路径。必需请求头：

| Header | 值 |
|---|---|
| `X-DDC-Access-Key` | 配置的 access key |
| `X-DDC-Timestamp` | Unix 毫秒时间戳 |
| `X-DDC-Nonce` | 请求唯一随机数 |
| `X-DDC-Content-SHA256` | 精确请求体的小写 SHA-256 十六进制 |
| `X-DDC-Signature` | 规范请求的 HMAC-SHA256 十六进制 |

规范请求由六个换行分隔的字段组成：

```text
大写 HTTP_METHOD
请求路径
规范查询串
timestamp
nonce
content-sha256
```

规范查询串按 UTF-8 和 RFC 3986 非保留字符进行百分号编码，再按编码后的 Key
和值排序，并保留重复参数。Admin 会检查时间偏移、Access Key、请求体摘要、
签名和 Nonce 重放。

## 配置

业务应用：

```yaml
egon:
  cola:
    component:
      ddc:
        enabled: true
        app-code: order-service
        env: dev
        namespace: default
        admin:
          endpoint: http://localhost:18080
          signature-enabled: true
          access-key: ${DDC_ACCESS_KEY}
          secret-key: ${DDC_SECRET_KEY}
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
          openapi:
            signature-enabled: true
            credentials:
              - credential-id: gateway-engine
                access-key: ${DDC_ACCESS_KEY}
                secret: ${DDC_SECRET_KEY}
                client-type: "*"
                app-code-patterns: [gateway-engine-*]
                env-patterns: [local]
                namespace-patterns: [default]
                allowed-operations: [SDK_REGISTER, SDK_HEARTBEAT,
                  SDK_OFFLINE, CONFIG_PULL, CONFIG_VALUE, PUBLISH_ACK,
                  DEFAULTS_REPORT, REGISTRY_REGISTER, REGISTRY_HEARTBEAT,
                  REGISTRY_DEREGISTER, REGISTRY_READ]
          publish:
            dispatch-timeout-ms: 5000
            default-timeout-ms: 30000
            max-timeout-ms: 60000
            scan-interval-ms: 1000
            completion-poll-interval-ms: 100
            recovery-stale-ms: 120000
```

`test` Profile 使用 SQLite `create-drop`，并关闭 Flyway 和 Admin Redis
连接；这不是生产存储拓扑。

## 构建和验证

```bash
./mvnw -B -ntp \
  -pl :egon-cola-platform-dynamic-config-center-starter,:egon-cola-platform-dynamic-config-center-admin,:egon-cola-platform-dynamic-config-center-test \
  -am clean test

./mvnw -B -ntp \
  -pl :egon-cola-platform-dynamic-config-center-admin,:egon-cola-platform-dynamic-config-center-starter \
  -am package -DskipTests
```

## 明确边界

完整的 DDC + Gateway + RPC 启动顺序、凭据、租约演练和运行证据见
[开发联调 Runbook](../egon-cola-platform-gateway/docs/developer-integration.zh-CN.md)。

- 不支持 Raft、Leader 选举、共识日志或成员协议；
- 多 Admin 运行要求共享 PostgreSQL 和 Redis；平台不负责提供数据库或 Redis HA；
- 不提供分布式共识或通用分布式锁服务；
- 不内嵌 Redis，不使用数据库持久化服务注册信息；
- 不包含 UI、账号系统、RBAC 或 MySQL 兼容目标；
- V1 不支持异步、多数派或部分成功发布模式。
