# Gateway、DDC 与 RPC 开发联调 Runbook

[English](developer-integration.md) | [Gateway 概览](../README.zh-CN.md)

本文给出本地完整链路：Gateway Admin 通过 DDC 发布规则，两个 Gateway Engine
订阅规则和 Provider 租约，MVC/WebFlux/RPC Provider 注册并上报接口，RPC Consumer
只发现内部 Gateway。Nginx、生产 HA、外部 IAM 与生产 TLS 证书不属于该 Demo。

## 前置与证据边界

需要 JDK 21、Docker Compose、`curl`、`jq`、`openssl`，以及可用的 Maven Wrapper。
复制环境文件并替换所有示例密钥：

```bash
cd egon-cola-platforms/egon-cola-platform-gateway/deployment
cp .env.example .env
chmod 600 .env
./scripts/demo.sh doctor
```

本仓库默认测试不会启动 Docker。`demo.sh --help`、Shell 语法、伪 Docker 安全测试和
`docker compose config --quiet` 可以作为静态证据；只有操作者实际完成下方生命周期，
才能声称 PostgreSQL、双 Redis、Kafka、双 Engine 和真实 Provider 链路通过。

## 拓扑与端口

```text
Admin Web :18090 -> Gateway Admin :18080 -> DDC Admin :18070
                                      |          |
                                      |          +-> DDC Redis
                                      +-> PostgreSQL + Kafka
Engine 1 :18081/:18082 RPC :19090 ----+----> Provider registry/rules
Engine 2 :18181/:18182 RPC :19190 ----+
MVC :18084  WebFlux :18085  RPC Provider :18086/:19091
RPC Consumer :18087 -> DDC discovers Engine RPC slots -> RPC Provider
```

DDC Redis 与分布式限流 Redis 是不同服务和数据卷；两个 Engine 使用不同 LKG 卷。

## 一条命令化生命周期

严格按顺序执行：

```bash
./scripts/demo.sh build
./scripts/demo.sh up-control
./scripts/demo.sh init
./scripts/demo.sh up-providers
./scripts/demo.sh publish
./scripts/demo.sh up-consumer
./scripts/demo.sh verify
./scripts/demo.sh logs
./scripts/demo.sh down
```

`init` 生成 12 小时本地 JWT，创建 HTTP/RPC Application、上报凭据和 Gateway Group。
JWT、凭据与对象 ID 只写入忽略目录 `.demo/`，文件权限为 0600。`publish` 根据实际上报
的 `methodIdentity` 解析 operationId，发布 HTTP Provider route、HTTP→RPC route 和
分布式限流策略，再等待双 Engine 一致。不要手工把 operationId 写入 fixture。

`down` 不删卷。确需删除本 Demo 的 PostgreSQL/Redis/Kafka/LKG 数据时才执行：

```bash
./scripts/demo.sh purge
```

`purge` 需要 `.demo/.local-demo-marker` 且 Compose project name 必须以
`egon-cola-gateway-demo-` 开头。它不可恢复。

## OpenAI 兼容传输 Route

OpenAI 兼容 Route 仍是绑定 HTTP Operation、通过租约注册中心发现 Provider 的普通
Gateway Route，不包含静态上游 URL，也不包含模型选择。以下 JSON 是 Draft Route API
中的规范 `content`；外围请求的 `operationId` 必须来自接口目录。

普通 JSON、SSE 自动识别、Multipart 上传和多模态载荷可以共用一个
`OPENAI_HTTP` Streaming Route：

```json
{
  "host": "ai.example.com",
  "httpMethod": "POST",
  "pathPattern": "/v1/**",
  "accessZones": ["PUBLIC"],
  "priority": 0,
  "transportPolicy": {
    "profile": "OPENAI_HTTP",
    "transportProtocol": "HTTP",
    "requestBodyMode": "STREAMING",
    "responseMode": "AUTO_STREAM",
    "maxRequestBodyBytes": 536870912,
    "connectTimeoutMs": 10000,
    "responseHeaderTimeoutMs": 120000,
    "streamIdleTimeoutMs": 90000,
    "totalTimeoutMs": 1800000,
    "bodyLogEnabled": false,
    "retryEnabled": false
  }
}
```

契约更窄时可显式指定响应模式。Multipart 和大文件上传应保持
`requestBodyMode=STREAMING`；Gateway 不运行 Multipart Parser。

| Route 用途 | `requestBodyMode` | `responseMode` |
|---|---|---|
| 普通 JSON 或混合 OpenAI Endpoint | `STREAMING` | `AUTO_STREAM` |
| Responses/Chat SSE Endpoint | `STREAMING` | `SSE` |
| Multipart 音频/文件上传 | `STREAMING` | `STANDARD` 或 `AUTO_STREAM` |
| 图片/音频二进制下载 | `STREAMING` | `BINARY_STREAM` |

Realtime WebSocket 使用单独的 GET Route，并复用同一接口目录与 Provider 边界：

```json
{
  "host": "ai.example.com",
  "httpMethod": "GET",
  "pathPattern": "/v1/realtime",
  "accessZones": ["PUBLIC"],
  "priority": 0,
  "transportPolicy": {
    "profile": "OPENAI_HTTP",
    "transportProtocol": "WEBSOCKET",
    "websocketIdleTimeoutMs": 300000,
    "websocketMaxFrameBytes": 16777216,
    "bodyLogEnabled": false,
    "retryEnabled": false
  }
}
```

Engine 根据已选 Provider 的 secure 元数据使用 `ws` 或 `wss`，转发 Text、Binary、
Continuation、Ping/Pong 和合法 Close Frame，并从客户端候选中协商 Subprotocol；
默认关闭 WebSocket Extension。上游握手拒绝仍返回普通 HTTP 错误，因为只有上游握手
成功后才向下游发送 `101`。

`OPENAI_HTTP` 默认请求上限为 512 MiB，Connect Timeout 10 秒、Response Header
Timeout 120 秒、Stream Idle Timeout 90 秒、Total Timeout 30 分钟，并关闭 Body 日志
和重试；WebSocket 默认 Idle 为 5 分钟、单 Frame 为 16 MiB。Route 显式值优先，但
始终受 Engine 安全上限约束；同一 Gateway Group 的全部 Engine 必须使用一致的安全上限。

| 字段 | 语义 |
|---|---|
| `maxRequestBodyBytes` | 流式字节上限；已声明超限在连接前拒绝，Chunked 超限在中途终止。 |
| `connectTimeoutMs` | TCP/TLS 建连预算。 |
| `responseHeaderTimeoutMs` | 请求开始发送后等待上游响应头的预算。 |
| `streamIdleTimeoutMs` | 请求或响应流相邻 Buffer 之间允许的最大无活动时间。 |
| `totalTimeoutMs` | HTTP 端到端总预算；持续 SSE 数据不会重置。 |
| `websocketIdleTimeoutMs` | 双向共享 Frame 空闲预算；Ping/Pong 会刷新活动时间。 |
| `websocketMaxFrameBytes` | 单 Frame 上限；超限以 1009 关闭。 |
| `bodyLogEnabled` | 只控制既有的有界 Body Sample；OpenAI Profile 默认关闭。 |
| `retryEnabled` | 仍受安全门禁；Streaming、OpenAI POST、WebSocket 与任何已提交交换都不可重试。 |

在 Route 安全配置允许时，`Content-Type`、`Authorization`、
`OpenAI-Organization`、`OpenAI-Project`、`Idempotency-Key`、`traceparent` 和
`tracestate` 等端到端 Header 会保留；固定及 `Connection` 声明的 Hop-by-Hop Header
会移除。Body 以 `DataBuffer` 流透传，不解析再序列化。SSE 会移除 `Content-Length`，
设置 `Cache-Control: no-cache, no-transform` 与 `X-Accel-Buffering: no`，并逐 Buffer
Flush。二进制 Body 不经过 String 或 JSON 转换。

HTTP→RPC 与 RPC Route 保持既有的聚合 unary 行为，不能启用 WebSocket、Streaming
Request、SSE、Binary Stream 或 `OPENAI_HTTP` 默认值。OpenAI 传输一旦收到上游响应头、
提交下游响应头、发送 SSE Buffer、完成 WebSocket `101` 或转发 Frame，就绝不重试。
下游断开会立即取消当前上游 Body 或 Session。

### 兼容与发布顺序

新 Engine 仍可读取不含 `transportPolicy` 的旧 v1 Release，并维持旧的 HTTP/RPC 聚合
行为；旧 Engine 不应接收包含新 Transport 字段的 Release。必须按以下顺序发布：

1. 升级同一 Gateway Group 的全部 Engine，并等待所有节点 Ready。
2. 确认 Engine Transport 安全上限同构且 runtime consistency 正常。
3. 升级 Gateway Admin 与 Admin Web。
4. 最后才创建并发布包含 `transportPolicy` 的 Route。

混部窗口继续使用旧规则；激活失败沿用现有 last-known-good Release。历史 UI Draft
缺少 `host` 时必须人工补录，Admin 不会自动生成通配符 `*`。

该能力严格止于传输：Gateway 不统计 Token、不计费、不管理 Prompt 或会话、不执行
RAG/Agent 编排或 Function Calling，也不做业务模型选择。

## 手工成功判据

```bash
curl -fsS http://127.0.0.1:18070/api/v1/ddc/manifest
curl -fsS http://127.0.0.1:18080/actuator/health/readiness
curl -fsS -H 'Host: providers.gateway.demo' \
  http://127.0.0.1:18081/api/providers/manual-1 | jq
curl -fsS 'http://127.0.0.1:18087/test/rpc/echo?message=manual-rpc' | jq
```

重复第一个数据面请求应能观察到 `framework=mvc` 和 `framework=webflux`。RPC 返回应
包含消息、traceId 和 `rpc-provider-demo`。`.demo/admin.jwt` 可用于查询：

```bash
TOKEN="$(cat .demo/admin.jwt)"
GROUP_ID="$(cat .demo/group.id)"
curl -fsS -H "Authorization: Bearer ${TOKEN}" \
  "http://127.0.0.1:18080/api/v1/gateway/admin/gateway-groups/${GROUP_ID}/runtime-consistency" | jq
```

成功状态要求 `consistent=true`、`readyEngineNodeCount=2`，Provider 投影包含两个 HTTP
实例和 RPC 实例，Trace 投影包含 protocol、providerService 与 engineInstanceId。

## 故障演练

- `docker compose ... stop http-provider-mvc`：优雅注销后 WebFlux 应继续服务；重启后
  同一 instanceId 获得新 leaseId。
- `docker compose ... kill http-provider-webflux`：在租约 TTL 后摘除；MVC 继续服务。
- 停止一个 Engine：RPC Consumer 应在 DDC 更新后选择另一个 Gateway Slot；重启节点
  后获取新租约并重新进入轮转。
- 暂停 DDC：已 Ready Engine 只能继续有效内存规则/LKG；冷启动 Engine 不得 Ready。
- 暂停 Kafka：业务响应不得被改变，但 Kafka 发送失败/丢弃指标必须增加。

故障后重新执行 `publish` 或 `verify` 前，先看 `.demo/logs/compose.log`、Admin 的
runtime-consistency、Provider/Engine 投影和 Actuator readiness。不要用固定 sleep
代替状态检查。

## 自动化与未验证边界

默认门禁：

```bash
./mvnw -B -ntp -f pom.xml \
  -pl egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-test/egon-cola-platform-gateway-test-suite \
  -am test
```

真实 Testcontainers 门禁（会启动容器与多个 JVM）：

```bash
./mvnw -B -ntp -f pom.xml \
  -pl egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-test/egon-cola-platform-gateway-test-suite \
  -am -Pgateway-live verify
```

纯本机真实拓扑门禁（要求 `PATH` 中存在 `initdb`、`postgres` 和 `redis-server`；
会启动隔离的临时基础设施与多个 JVM）：

```bash
./mvnw -B -ntp -f pom.xml \
  -pl egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-test/egon-cola-platform-gateway-test-suite \
  -am -Pgateway-live -Dgateway.live.infrastructure=local verify
```

基础 Demo 与进程内流式组件测试不验证 Redis Sentinel/Cluster、PostgreSQL/Kafka HA、
控制面多实例故障转移、生产 TLS/mTLS、证书轮换、公网 OpenAI、私有 CA、外部负载均衡
或 Kubernetes。Gateway 也不能强制外层 Nginx/Ingress Flush 或关闭缓存。配置可渲染与
组件 Fixture 都不等于真实运行证据，仍需在目标环境验证。
