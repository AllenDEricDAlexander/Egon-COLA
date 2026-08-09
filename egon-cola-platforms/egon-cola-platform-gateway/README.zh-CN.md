# Egon COLA Gateway 平台

[English](README.md) | [中文](README.zh-CN.md)

Gateway 平台是 Egon COLA 自研的 HTTP 与 RPC 企业基础设施系统。它包含 Reactor
Netty 数据面、Spring Boot 管理控制面、面向 Provider 的上报 starter，以及 HTTP
Provider 租约运行时。Admin 通过 DDC 发布不可变规则版本；Engine 发现 Provider、
选择健康实例，并转发 HTTP 和 unary RPC 流量。

## 架构

```text
Admin Web ── 鉴权 API ──> Gateway Admin ── 发布 ──> DDC
                                            │
HTTP/RPC 客户端 ──> Gateway Engine <── 规则版本 ─┘
                         │
                         ├── 路由、安全、流量和可观测流水线
                         ├── 基于 DDC 租约的 Provider / Gateway 注册发现
                         └── HTTP 或 unary RPC 上游 ──> Provider
```

数据面保持当前规则版本不可变，并以原子方式切换。Provider 定义、租约、健康状态和
接口上报通过 DDC 校准。DDC 临时不可用时，Engine 可以继续使用有效的内存状态和
last-known-good 规则；但冷启动 Engine 在缺少必要规则和 Provider 状态时不能声称
Ready。

DDC 是引导例外：Gateway Admin、Engine、Provider 和 Consumer 通过本地配置的
`19080` DDC 直连 gRPC 目标与 `round_robin` 访问，DDC 不注册或发现自身。Redis
Pub/Sub 仍承载变更通知。其他普通 RPC 服务仍由 DDC 支撑，业务调用仍经过
已发现的 Gateway 集合，不直连 Provider。

## 模块

| 模块 | 职责 | 是否为业务依赖入口 |
|---|---|---|
| `egon-cola-platform-gateway-contract` | 规则、Provider、发布和事件的跨进程稳定契约 | 否 |
| `egon-cola-platform-gateway-core` | 无框架数据面模型、过滤器、路由、安全和 SPI | 否 |
| `egon-cola-platform-gateway-engine` | 可执行 HTTP/RPC 数据面、监听器、上游客户端、健康检查和遥测 | 否 |
| `egon-cola-platform-gateway-admin` | 可执行管理控制面、持久化、规则编译、鉴权和 OpenAPI | 否 |
| `egon-cola-platform-gateway-starter` | Provider 接口定义上报和下游集成 | 是 |
| `egon-cola-platform-gateway-provider-runtime` | HTTP Provider 的 DDC 注册和租约生命周期 | 是 |
| `egon-cola-platform-gateway-test` | 真实 HTTP/RPC Provider、Consumer 和拓扑验证 | 否 |

Admin Web 是与 Gateway 源码同目录的私有 React 应用，路径为
`egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web`；它不是 Maven
子模块。详见 [前端 README](egon-cola-platform-gateway-admin-web/README.md)。

## 运行能力

- Public 和 Internal HTTP 监听器，提供有界请求体、CORS、安全过滤器、协议重试、
  幂等传递和优雅 Drain。
- 基于 OpenAI HTTP 调用规范的透明传输，支持原始 JSON、逐 Buffer Flush 的 SSE、
  Multipart 大文件流式上传、图片/音频等多模态与二进制请求响应，以及两阶段
  `ws`/`wss` Realtime WebSocket 双向代理。
- 通过不可变路由与 Provider 快照支持 HTTP→HTTP、HTTP→RPC 和 RPC→RPC 转发。
- 基于 DDC 租约的 Provider 发现、主动健康探测、有界 Provider 尝试、负载均衡，
  以及过期/不健康实例摘除。
- Gateway Admin 草稿、接口目录、规则版本编译、规范化摘要、鉴权管理 API 和
  运行时定义校准。
- HTTP、RPC、DDC 直连 RPC 和管理传输的 TLS/mTLS，以及受控证书刷新和监听器 Drain 操作。
- Micrometer Observation / OpenTelemetry Span 和有界 Kafka 调用事件投影；遥测
  故障不得改变业务响应。

## Trace 传播

Gateway 数据面使用 `egon-cola-component-common-trace` 的 W3C Trace Context 能力。
入口只从合法 `traceparent`、`tracestate` 和 `x-egon-request-id` 建立上下文；Gateway
不再读取或写入 `X-Trace-Id`、`x-trace-id` 或 `x-egon-trace-id`。HTTP 上游和 RPC
上游都会为每次 Provider Attempt 创建独立 child span，重试不会复用同一个 attempt
`spanId`，但整个请求保持同一个 `traceId`。

Gateway 已接入 Micrometer Observation / OpenTelemetry。存在有效 Observation Span
时，`GatewayCallEventV1.Trace`、普通日志和下游 `traceparent` 会以当前 Span 为准；
没有 Tracer 时才使用 `common-trace` 的轻量生成逻辑。

## 消费和构建

Components BOM 不再导出 Gateway Artifact。业务系统如需 Provider 侧集成能力，可以
使用仓库统一发布版本显式依赖 `egon-cola-platform-gateway-starter` 或
`egon-cola-platform-gateway-provider-runtime`。Engine、Admin、Contract、Core 和 test
属于平台内部模块，应通过仓库的 Gateway 拓扑构建或部署。

本次迁移保留 `top.egon.cola.component.gateway` Java 包名，以维持源码和二进制兼容；
Maven 坐标与 reactor 归属是 Gateway 的正式 platform 边界。

执行 JVM 专项验证：

```bash
./mvnw -B -ntp \
  -pl egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-test \
  -am test
```

live Profile 会由测试 Harness 启动真实 Provider/Consumer 拓扑。默认使用
Testcontainers；当 `PATH` 中存在 `initdb`、`postgres` 和 `redis-server` 时，也可使用
隔离的本机临时进程：

```bash
./mvnw -B -ntp \
  -pl egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-test/egon-cola-platform-gateway-test-suite \
  -am -Pgateway-live verify

./mvnw -B -ntp \
  -pl egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-test/egon-cola-platform-gateway-test-suite \
  -am -Pgateway-live -Dgateway.live.infrastructure=local verify
```

## 运维文档

| 文档 | 用途 |
|---|---|
| [Gateway + DDC + RPC 联调](docs/developer-integration.zh-CN.md) | 端到端 Demo 命令、成功判据、故障演练和证据边界 |
| [本地部署](deployment/README.md) | Compose 构建、端口、Ready、HA 样例、TLS/mTLS 和启停顺序 |
| [性能和故障演练](performance/README.md) | k6 smoke/baseline、长稳、资源采样和固定故障场景 |
| [Admin Web](egon-cola-platform-gateway-admin-web/README.md) | React 构建、测试、浏览器鉴权和 API Origin 配置 |

## 边界

- Nginx 节点管理、Nginx 动态配置和外部负载均衡不属于 Gateway 平台。多 Engine
  前的入口及 L4/L7 负载均衡由部署环境负责。
- 基础 Compose 拓扑是本地开发依赖集合。HA overlay 只验证多个无状态 Admin 进程和
  代理路由，不会把单节点 PostgreSQL、Redis 或 Kafka 变成生产 HA 服务。
- Gateway 不包含通用账号系统或外部 IAM。Admin Web 提供经过验证的 IAM Bearer
  Token，Gateway Admin 负责鉴权 Actor 和 capability 边界。
- OpenAI Route Profile 只是传输配置预设，不是 AI 业务平台。Gateway 不统计 Token、
  不计费、不管理 Prompt 或会话、不执行 RAG/Agent 编排或 Function Calling，也不做
  业务模型选择；它只识别请求、匹配路由、承载协议并透明转发字节。
- 流式组件测试只证明进程内 Gateway 边界，不证明公网 OpenAI、外部/私有 CA TLS、
  多进程基础设施，或外层 Nginx/Ingress 的 Flush 与缓存行为。
- 实现和部署契约仍在演进；当前版本证据以专项测试和下方运维文档为准。
