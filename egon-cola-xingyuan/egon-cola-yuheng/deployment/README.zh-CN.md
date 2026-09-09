# Yuheng 本地部署与运行边界

[English](README.md) | [Yuheng 概览](../README.md)

该目录提供 Yuheng Engine、Yuheng Admin、Admin Web 以及本地依赖的部署样例。
它不是生产 HA 方案，也不负责 Nginx 节点负载或动态配置。Admin 保持一个逻辑控制面，API_RPC 与 MCP 使用各自固定职责的可执行制品。

## 构建前置

先在仓库根目录生成四个可执行制品：

```bash
./mvnw -B -ntp \
  -pl egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin,egon-cola-xingyuan/egon-cola-yuheng/yuheng-admin,egon-cola-xingyuan/egon-cola-yuheng/yuheng-biz-gateway,egon-cola-xingyuan/egon-cola-yuheng/yuheng-mcp-gateway \
  -am package -DskipTests
```

复制 `.env.example` 为 `.env`，仅在本机填入独立的 Tianshu Runtime、Registry、Management
随机凭据和 32 字节主密钥的 Base64。不要在不同能力之间复用 Tianshu Secret。
不得提交 `.env`。随后可由操作者自行执行：

```bash
docker compose --env-file .env -f compose.yml build
docker compose --env-file .env -f compose.yml up -d
```

本次代码交付不会自动执行上述启动命令。

包含 MVC、WebFlux、RPC Provider、RPC Consumer 和双 Engine 的完整 Demo 见
[开发联调 Runbook](../docs/developer-integration.zh-CN.md)。命令入口为
`./scripts/demo.sh`；`down` 保留数据，显式破坏性的 `purge` 只允许用于带本地标记的
Demo project。

## 端口与持久化

| 服务 | 本机端口 | 用途 |
|---|---:|---|
| Tianshu Admin HTTP | 18070 | 人工管理 API 与 Actuator Readiness |
| Tianshu Admin gRPC | 19080 | 直连 Runtime、Registry 与 Management Facade |
| Yuheng Admin | 18080 | 管理 API 与健康端点 |
| API_RPC 1 PUBLIC (direct) | 18091 | 外部 HTTP 数据面 |
| Engine 1 INTERNAL | 18082 | 内部 HTTP 数据面 |
| Engine 1 Management | 18083 | Actuator |
| Engine 1 RPC Slot | 19090 | Egon RPC 内部网关 |
| API_RPC 2 PUBLIC | 18181 | 第二个外部 HTTP 数据面 |
| Engine 2 INTERNAL | 18182 | 第二个内部 HTTP 数据面 |
| Engine 2 Management | 18183 | 第二个 Actuator |
| Engine 2 RPC Slot | 19190 | 第二个 Egon RPC 内部网关 |
| 统一数据面代理 | 18081 | 原有 API/MCP Host/Path 入口 |
| MCP 1 数据 / Management | 18084 / 18085 | 本机诊断，独立就绪探测 |
| MCP 2 数据 / Management | 18184 / 18185 | 第二个 MCP 副本 |
| Demo MVC / WebFlux | 18094 / 18095 | 避免占用 MCP 默认端口 |
| Admin Web | 18090 | React 管理页面 |

每个 Engine 的 LKG 目录必须独立持久化；Tianshu Redis 与分布式限流 Redis
使用不同实例和数据卷。
PostgreSQL 初始化两个 Database，避免 Tianshu 与 Yuheng Admin 的 Flyway 历史互相污染。

## 健康与发布顺序

推荐检查：

```text
Tianshu Admin  GET /actuator/health/readiness
Admin      GET /actuator/health/liveness
Admin      GET /actuator/health/readiness
Engine     GET :18083/actuator/health/liveness
Engine     GET :18083/actuator/health/readiness
Admin Web  GET /healthz
```

Engine 进程存活不代表业务 Ready。首次部署必须先部署并验证 Tianshu Admin
`19080` gRPC Provider 和 HTTP Readiness，再启动依赖 Tianshu 的 Admin/Provider/Engine，
等待 Engine 注册 Config Client，最后由 Admin 发布首个有效 Rule Release。Engine
只有在 Listener、有效规则及必要 Provider 就绪后才应接流量。

## 自动化验收

快速门禁不启动外部进程，覆盖 Java 单元/组件测试、Admin Web 类型检查、Vitest、
ESLint 与生产构建。真实拓扑门禁由进程 Harness 启动真实 Tianshu、Admin、两个 API_RPC 和两个 MCP 副本、
HTTP Provider、RPC Provider 与 RPC Consumer；基础设施默认使用 Testcontainers：

```bash
./mvnw -B -ntp \
  -pl egon-cola-xingyuan/egon-cola-yuheng/yuheng-test/yuheng-test-suite \
  -am -Pyuheng-live verify
```

如需完全使用本机进程，请确保 `PATH` 中存在 `initdb`、`postgres` 和 `redis-server`，
并选择 local 后端。该模式会在随机端口启动测试专属的临时 PostgreSQL、两个 Redis，
以及单节点内嵌 KRaft，不会使用现有数据库或 Redis 数据：

```bash
./mvnw -B -ntp \
  -pl egon-cola-xingyuan/egon-cola-yuheng/yuheng-test/yuheng-test-suite \
  -am -Pyuheng-live -Dgateway.live.infrastructure=local verify
```

测试会验证接口定义上报、规则发布、两个角色的全部实例注册与
Ready、HTTP/RPC 转发、双 Provider 负载均衡、Provider 摘除、限流和 Kafka Trace
投影；日志及脱敏后的进程参数写入 `target/yuheng-process-it`。

发布与停机顺序：

```text
启动：PostgreSQL/Redis/Kafka → Tianshu → Admin → Provider → Engine → Admin Web
停机：摘除 Engine 流量 → Engine 有界 Drain → Provider → Admin → Tianshu → 基础设施
```

Compose 为进程预留 30 秒优雅停止时间。Kafka 故障不应改变业务响应，但必须通过指标
暴露丢弃/失败；Tianshu 暂时不可用时，已运行 Engine 只能继续使用有效内存状态与 LKG，
重启节点有经过校验的 LKG 时可降级服务，但未 ACK 当前版本前不得宣称全局一致。

## 控制面 HA

`compose.ha.yml` 在共享 PostgreSQL、Tianshu Redis 和 Kafka 之上增加第二个 Tianshu Admin、
第二个 Yuheng Admin，以及分别代理 Tianshu 人工 HTTP（`18070`）、Tianshu HTTP/2 gRPC
（`19080`）和 Yuheng Admin HTTP（`18080`）的 HAProxy TCP Frontend。它不引入 Raft，也不改变业务
网关边界：Tianshu 的发布一致性仍由 PostgreSQL 行锁、版本条件更新和持久化发布任务保证，
Redis 负责缓存、Registry 与消息通知。Tianshu Admin 可通过以下属性接入生产 Redis：

```text
EGON_COLA_COMPONENT_TIANSHU_ADMIN_REDIS_MODE=SENTINEL|CLUSTER
EGON_COLA_COMPONENT_TIANSHU_ADMIN_REDIS_NODES[0]=redis://redis-1:26379
EGON_COLA_COMPONENT_TIANSHU_ADMIN_REDIS_MASTER_NAME=tianshu-master
```

HA 样例由操作者自行启动：

```bash
docker compose --env-file .env \
  -f compose.yml -f compose.ha.yml --profile ha config
docker compose --env-file .env \
  -f compose.yml -f compose.ha.yml --profile ha up -d
```

代理端口 `18270`、`19280` 和 `18280` 分别暴露 Tianshu 人工 HTTP、Tianshu gRPC
和 Yuheng Admin HTTP。Tianshu 客户端使用 `dns:///control-plane-proxy:19080` 和
`round_robin`，不通过 Tianshu 发现 Tianshu。移除任一 Admin 容器后，TCP 健康检查会摘除
故障节点；另一实例继续读取共享 PostgreSQL 发布任务与 Redis 状态。不需要
粘性会话、Leader 选举或 Admin 本地权威状态。

RPC Yuheng Slot 同样按 Tianshu `INTERNAL_GATEWAY` 实例集合工作。Consumer 增量保留未变化
通道、为新增 Engine 建立通道、对下线 Engine 有界 Drain，使用 Round Robin 选点；
只有 Yuheng 连接阶段的 `UNAVAILABLE` 才会在总 Deadline 内换节点重试，Provider 阶段
失败不会被重复调用。

## TLS 与 mTLS

生产模式不接受隐式明文。PUBLIC HTTP 可配置单向 TLS；INTERNAL HTTP、RPC Slot、Tianshu
直连 RPC 和 Yuheng Admin Management 可强制 mTLS。证书、私钥和信任链仅通过只读
文件路径注入，不提供跳过 SAN/Authority 校验或 Trust-All 开关。本地明文必须显式设置
`development-plaintext=true` 或 `transport-security.mode=DEVELOPMENT_PLAINTEXT`。

`compose.mtls.yml` 是 PEM 文件注入样例。`${YUHENG_TLS_DIRECTORY}` 至少包含：

```text
ca.crt
tianshu-admin.crt / tianshu-admin.key
tianshu-admin-2.crt / tianshu-admin-2.key
yuheng-admin.crt / yuheng-admin.key
yuheng-admin-2.crt / yuheng-admin-2.key
yuheng-admin-web.crt / yuheng-admin-web.key
yuheng-biz-gateway.crt / yuheng-biz-gateway.key
yuheng-biz-gateway-2.crt / yuheng-biz-gateway-2.key
yuheng-mcp-gateway.crt / yuheng-mcp-gateway.key
yuheng-mcp-gateway-2.crt / yuheng-mcp-gateway-2.key
yuheng-data-plane-proxy.pem (certificate chain and private key)
```

私钥必须为未加密 PKCS#8 PEM。证书 SAN 必须覆盖实际连接名；组合 HA 与 mTLS 时，
HAProxy 是 TCP Passthrough，因此两张 Tianshu gRPC 服务端证书都须覆盖逻辑 Authority
`control-plane-proxy`。验证配置或启动命令为：

```bash
docker compose --env-file .env \
  -f compose.yml -f compose.mtls.yml config
docker compose --env-file .env \
  -f compose.yml -f compose.ha.yml -f compose.mtls.yml \
  -f compose.ha-mtls.yml --profile ha config
```

Spring SSL Bundle 设置 `reload-on-update=true`，Tianshu Admin 与 Yuheng Admin 会监听 PEM
文件更新；Actuator 暴露 `ssl.chain.expiry` 指标和 SSL 健康信息。Engine 暴露
`yuheng.tls.certificate.expiry.epoch.seconds`，证书原子替换后可由受保护的
`POST /actuator/gatewayTls` 入口执行有界 Drain 并重建 HTTP/RPC Listener。该入口在本 Compose mTLS 样例中显式关闭。Management 仅暴露 health/info/metrics 给容器网络和本机回环映射，便于数据面代理按角色就绪摘流；代理不转发 Management 端口。需要热重载时，应另行通过受控容器内管理通道启用，不得直接对外开放。

## OpenTelemetry

Engine 已通过 Micrometer Observation 和 OTel Bridge 记录 Request、Provider Attempt、
Tianshu Apply 与 Kafka Send Span。默认不连接 Collector；启用 OTLP 时显式注入：

```text
MANAGEMENT_OTLP_TRACING_EXPORT_ENABLED=true
MANAGEMENT_OTLP_TRACING_ENDPOINT=https://otel-collector.example/v1/traces
MANAGEMENT_TRACING_SAMPLING_PROBABILITY=0.1
```

合法的上游 W3C `traceparent` 采样标志优先；只有调用方未提供 W3C Parent 时才使用本地
采样概率。Operation、Route、Provider Instance、Event ID 等高基字段只进入 Span，
不会进入低基数指标 Tag。Collector 不可用不影响 Yuheng 业务响应。

## 已知部署边界

- 基础 `compose.yml` 的 PostgreSQL、Redis、Kafka 和 Admin 仍为单节点开发依赖；
- `compose.ha.yml` 只验证无状态双 Admin，不宣称单节点 PostgreSQL/Redis/Kafka 已 HA；
- Provider 只通过 Tianshu Registry 发现，规则只通过 Tianshu DB/Redis/PubSub 下发；
- Tianshu 引导使用本地配置的逻辑目标直连 unary gRPC；不保留机器 HTTP Fallback、不自注册、不流式下发配置、不要求粘性会话；
- Nacos、Dubbo 与 Nginx 管理不属于该部署；
- 独立数据面 HAProxy 按路径选择 API_RPC/MCP 池；外部 DNS、证书与生产入口仍由部署平台管理；
- Secret Manager、NetworkPolicy 和外部可观测平台由部署平台负责。

## 双角色状态、凭据与切换

- Tianshu 的 `biz/env/appCode/namespace` 相同，appCode 为 `ge`；每个副本使用唯一 Config Client/Registry/Node ID。角色由制品固定，不增加 mode 开关。
- `tianshu-rpc-credentials.yml` 显式保留 Runtime、Registry、Management 三类现有凭据，并增加 MCP Runtime/Registry。Spring 的高优先级 list 会整体替换，因此不能仅配置下标3/4。示例 scope 保留原通配行为；生产按实际 Provider 访问需求收紧。
- API/MCP 各自填写 Tianquan-Shoubing Resource ID/URI，不能复用进程身份。共享业务 MCP Server Resource、原有 Token/audience/权限契约不变。示例占位符必须替换为实际已登记身份；本目录不部署 Tianquan-Shoubing。
- MCP Session/Subscription 使用共享 Redis；Task/Approval 使用现有 gateway_admin 表，Flyway 仍由 Admin 负责。API 没有 MCP 数据库配置。两个 MCP 副本共享 `YUHENG_MCP_ARTIFACT_DIRECTORY`，启动前由操作者准备 UID/GID10001 可访问的现有目录；不自动 chown 或清除用户文件。
- 四个 Engine 使用四个独立 LKG 卷。先启动 Tianshu/Admin/Provider，再启动 Engine，发布已有合法规则后执行 `./scripts/wait-ready.sh --engines`；Admin 必须看到两个角色全部在线实例的相同 Release/Version/Checksum 和 ACK_SUCCESS。
- 数据面代理保留18081，按 `/mcp/`、`/legacy/mcp/`、`/.well-known/oauth-protected-resource/mcp/` 分流；其余路径进入 API_RPC，保留 Host、认证和协议头。INTERNAL HTTP/gRPC 保持原独立入口。
- mTLS 数据面使用专用 `haproxy.data-plane.mtls.cfg` 终止外部 TLS，再校验各后端证书与主机名；控制面 `haproxy.cfg` 仍是 TCP 透传。代理 PEM 需具备服务端/客户端用途，SAN 覆盖稳定外部域名；各 Engine 证书 SAN 覆盖对应服务名，不使用 verify-none。
- 一角色更新失败保留旧快照，Admin 必须显示不一致。混合旧 Combined/新 split 制品切换期间禁止发布新 Release；先暗启 MCP 并验证，切 MCP 路由，再替换 API_RPC，最后退役旧实例。回滚仅切目标角色路由/制品，保留数据库和 LKG。
- `run-mcp-conformance.sh` 默认指向 MCP 的 `/mcp/commerce`，需先发布该 Server 并满足其鉴权条件；可显式传入官方 SDK fixture URL，后者不能作为 Engine 验收证据。安全脚本覆盖 MCP Context 与单一制品兼容性。
- 当前静态渲染/单元测试不等于 Tianquan-Shoubing 联调、mTLS 握手、浏览器或真实 HA 验收。生产上线前必须完成相应运行验收。

代理健康检查与 TLS 参数参考 [HAProxy 3.1 官方配置说明](https://docs.haproxy.org/3.1/configuration.html)。
