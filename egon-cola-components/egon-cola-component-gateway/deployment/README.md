# Gateway 本地部署与运行边界

该目录提供 Gateway Engine、Gateway Admin、Admin Web 以及本地依赖的部署样例。
它不是生产 HA 方案，也不负责 Nginx 节点负载或动态配置。

## 构建前置

先在仓库根目录生成三个可执行制品：

```bash
./mvnw -B -ntp \
  -pl egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-admin,egon-cola-components/egon-cola-component-gateway/egon-cola-component-gateway-admin,egon-cola-components/egon-cola-component-gateway/egon-cola-component-gateway-engine \
  -am clean package -DskipTests
```

复制 `.env.example` 为 `.env`，仅在本机填入随机凭据和 32 字节主密钥的 Base64。
不得提交 `.env`。随后可由操作者自行执行：

```bash
docker compose --env-file .env -f compose.yml build
docker compose --env-file .env -f compose.yml up -d
```

本次代码交付不会自动执行上述启动命令。

## 端口与持久化

| 服务 | 本机端口 | 用途 |
|---|---:|---|
| DDC Admin | 18070 | DDC OpenAPI/Management |
| Gateway Admin | 18080 | 管理 API 与健康端点 |
| Engine 1 PUBLIC | 18081 | 外部 HTTP 数据面 |
| Engine 1 INTERNAL | 18082 | 内部 HTTP 数据面 |
| Engine 1 Management | 18083 | Actuator |
| Engine 1 RPC Slot | 19090 | Egon RPC 内部网关 |
| Engine 2 PUBLIC | 18181 | 第二个外部 HTTP 数据面 |
| Engine 2 INTERNAL | 18182 | 第二个内部 HTTP 数据面 |
| Engine 2 Management | 18183 | 第二个 Actuator |
| Engine 2 RPC Slot | 19190 | 第二个 Egon RPC 内部网关 |
| Admin Web | 18090 | React 管理页面 |

每个 Engine 的 LKG 目录必须独立持久化；DDC Redis 与分布式限流 Redis
使用不同实例和数据卷。
PostgreSQL 初始化两个 Database，避免 DDC 与 Gateway Admin 的 Flyway 历史互相污染。

## 健康与发布顺序

推荐检查：

```text
DDC Admin  GET /api/v1/ddc/manifest
Admin      GET /actuator/health/liveness
Admin      GET /actuator/health/readiness
Engine     GET :18083/actuator/health/liveness
Engine     GET :18083/actuator/health/readiness
Admin Web  GET /healthz
```

Engine 进程存活不代表业务 Ready。首次部署必须先启动 DDC/Admin，再启动 Engine，
等待 Engine 注册 Config Client，最后由 Admin 发布首个有效 Rule Release。Engine
只有在 Listener、有效规则及必要 Provider 就绪后才应接流量。

## 自动化验收

快速门禁不启动外部进程，覆盖 Java 单元/组件测试、Admin Web 类型检查、Vitest、
ESLint 与生产构建。真实拓扑门禁通过 Testcontainers 启动 PostgreSQL、两个 Redis
和 Kafka，并由进程 Harness 启动真实 DDC、Admin、两个 Engine、HTTP Provider、
RPC Provider 与 RPC Consumer：

```bash
./mvnw -B -ntp \
  -pl egon-cola-components/egon-cola-component-gateway/egon-cola-component-gateway-test/egon-cola-component-gateway-test-suite \
  -am -Pgateway-live verify
```

该命令要求本机 Docker 可用。测试会验证接口定义上报、规则发布、双 Engine 注册与
Ready、HTTP/RPC 转发、双 Provider 负载均衡、Provider 摘除、限流和 Kafka Trace
投影；日志及脱敏后的进程参数写入 `target/gateway-process-it`。

发布与停机顺序：

```text
启动：PostgreSQL/Redis/Kafka → DDC → Admin → Provider → Engine → Admin Web
停机：摘除 Engine 流量 → Engine 有界 Drain → Provider → Admin → DDC → 基础设施
```

Compose 为进程预留 30 秒优雅停止时间。Kafka 故障不应改变业务响应，但必须通过指标
暴露丢弃/失败；DDC 暂时不可用时，已运行 Engine 只能继续使用有效内存状态与 LKG，
冷启动节点不能据此声称 Ready。

## 控制面 HA

`compose.ha.yml` 在共享 PostgreSQL、DDC Redis 和 Kafka 之上增加第二个 DDC Admin、
第二个 Gateway Admin，以及只做 TCP 转发的 HAProxy。它不引入 Raft，也不改变业务
网关边界：DDC 的发布一致性仍由 PostgreSQL 行锁、版本条件更新和持久化发布任务保证，
Redis 负责缓存、Registry 与消息通知。DDC Admin 可通过以下属性接入生产 Redis：

```text
EGON_COLA_COMPONENT_DDC_ADMIN_REDIS_MODE=SENTINEL|CLUSTER
EGON_COLA_COMPONENT_DDC_ADMIN_REDIS_NODES[0]=redis://redis-1:26379
EGON_COLA_COMPONENT_DDC_ADMIN_REDIS_MASTER_NAME=ddc-master
```

HA 样例由操作者自行启动：

```bash
docker compose --env-file .env \
  -f compose.yml -f compose.ha.yml --profile ha config
docker compose --env-file .env \
  -f compose.yml -f compose.ha.yml --profile ha up -d
```

代理端口 `18270` 和 `18280` 分别指向两个 DDC Admin 和两个 Gateway Admin。移除任一
Admin 容器后，TCP 健康检查会摘除故障节点；发布请求在另一实例继续读取同一发布任务，
不会由第二实例启动恢复逻辑误判为失败。

RPC Gateway Slot 同样按 DDC `INTERNAL_GATEWAY` 实例集合工作。Consumer 增量保留未变化
通道、为新增 Engine 建立通道、对下线 Engine 有界 Drain，使用 Round Robin 选点；
只有 Gateway 连接阶段的 `UNAVAILABLE` 才会在总 Deadline 内换节点重试，Provider 阶段
失败不会被重复调用。

## TLS 与 mTLS

生产模式不接受隐式明文。PUBLIC HTTP 可配置单向 TLS；INTERNAL HTTP、RPC Slot、DDC
Management 和 Gateway Admin Management 可强制 mTLS。证书、私钥和信任链仅通过只读
文件路径注入，不提供跳过 SAN/Authority 校验或 Trust-All 开关。本地明文必须显式设置
`development-plaintext=true` 或 `transport-security.mode=DEVELOPMENT_PLAINTEXT`。

`compose.mtls.yml` 是 PEM 文件注入样例。`${GATEWAY_TLS_DIRECTORY}` 至少包含：

```text
ca.crt
ddc-admin.crt / ddc-admin.key
ddc-admin-2.crt / ddc-admin-2.key
gateway-admin.crt / gateway-admin.key
gateway-admin-2.crt / gateway-admin-2.key
gateway-admin-web.crt / gateway-admin-web.key
gateway-engine.crt / gateway-engine.key
gateway-engine-2.crt / gateway-engine-2.key
```

私钥必须为未加密 PKCS#8 PEM。证书 SAN 必须覆盖实际连接名；组合 HA 与 mTLS 时，DDC
和 Gateway Admin 服务端证书还须覆盖 `control-plane-proxy`。验证配置或启动命令为：

```bash
docker compose --env-file .env \
  -f compose.yml -f compose.mtls.yml config
docker compose --env-file .env \
  -f compose.yml -f compose.ha.yml -f compose.mtls.yml \
  -f compose.ha-mtls.yml --profile ha config
```

Spring SSL Bundle 设置 `reload-on-update=true`，DDC Admin 与 Gateway Admin 会监听 PEM
文件更新；Actuator 暴露 `ssl.chain.expiry` 指标和 SSL 健康信息。Engine 暴露
`gateway.tls.certificate.expiry.epoch.seconds`，证书原子替换后可由受保护的
`POST /actuator/gatewayTls` 入口执行有界 Drain 并重建 HTTP/RPC Listener。该写操作必须
由部署平台限制在管理网络内。

## OpenTelemetry

Engine 已通过 Micrometer Observation 和 OTel Bridge 记录 Request、Provider Attempt、
DDC Apply 与 Kafka Send Span。默认不连接 Collector；启用 OTLP 时显式注入：

```text
MANAGEMENT_OTLP_TRACING_EXPORT_ENABLED=true
MANAGEMENT_OTLP_TRACING_ENDPOINT=https://otel-collector.example/v1/traces
MANAGEMENT_TRACING_SAMPLING_PROBABILITY=0.1
```

合法的上游 W3C `traceparent` 采样标志优先；只有调用方未提供 W3C Parent 时才使用本地
采样概率。Operation、Route、Provider Instance、Event ID 等高基数字段只进入 Span，
不会进入低基数指标 Tag。Collector 不可用不影响 Gateway 业务响应。

## 已知部署边界

- 基础 `compose.yml` 的 PostgreSQL、Redis、Kafka 和 Admin 仍为单节点开发依赖；
- `compose.ha.yml` 只验证无状态双 Admin，不宣称单节点 PostgreSQL/Redis/Kafka 已 HA；
- Provider 只通过 DDC Registry 发现，规则只通过 DDC DB/Redis/PubSub 下发；
- Nacos、Dubbo 与 Nginx 管理不属于该部署；
- Compose 暴露两个 Engine 端口，但入口四层/七层负载均衡仍由部署平台负责；
- Secret Manager、NetworkPolicy 和外部可观测平台由部署平台负责。
