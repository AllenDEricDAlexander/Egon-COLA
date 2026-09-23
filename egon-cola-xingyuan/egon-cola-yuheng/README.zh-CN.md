# Egon COLA Yuheng 平台

[English](README.md) | [中文](README.zh-CN.md)

Yuheng 平台是 Egon COLA 自研的 HTTP 与 RPC 企业基础设施系统。它包含 Reactor
Netty 数据面、Spring Boot 管理控制面、面向 Provider 的上报 starter，以及 HTTP
Provider 租约运行时。Admin 通过 Tianshu 发布不可变规则版本；Engine 发现 Provider、
选择健康实例，并转发 HTTP 和 unary RPC 流量。

## 架构

```text
Admin Web ── 鉴权 API ──> Yuheng Admin ── 发布 ──> Tianshu
                                            │
HTTP/RPC 客户端 ──> Yuheng Engine <── 规则版本 ─┘
                         │
                         ├── 路由、安全、流量和可观测流水线
                         ├── 基于 Tianshu 租约的 Provider / Yuheng 注册发现
                         └── HTTP 或 unary RPC 上游 ──> Provider
```

数据面保持当前规则版本不可变，并以原子方式切换。Provider 定义、租约、健康状态和
接口上报通过 Tianshu 校准。Tianshu 临时不可用时，Engine 可以继续使用有效的内存状态和
last-known-good 规则；但冷启动 Engine 在缺少必要规则和 Provider 状态时不能声称
Ready。

Tianshu 是引导例外：Yuheng Admin、Engine、Provider 和 Consumer 通过本地配置的
`19080` Tianshu 直连 gRPC 目标与 `round_robin` 访问，Tianshu 不注册或发现自身。Redis
Pub/Sub 仍承载变更通知。其他普通 RPC 服务仍由 Tianshu 支撑，业务调用仍经过
已发现的 Yuheng 集合，不直连 Provider。

## 模块

| 模块 | 职责 | 是否为业务依赖入口 |
|---|---|---|
| `yuheng-contract` | 规则、Provider、发布和事件的跨进程稳定契约 | 否 |
| `yuheng-core` | 无框架数据面模型、过滤器、路由、安全和 SPI | 否 |
| `yuheng-mcp-core` | Admin 与 Engine 共享的 MCP 协议、任务、会话、制品和订阅运行时原语 | 否 |
| `yuheng-runtime-core` | 两种 Engine 角色共享的非可执行运行时能力：流量整形、弹性治理和已编译操作处理 | 否 |
| `yuheng-biz-gateway` | 可执行 HTTP/RPC 数据面、监听器、上游客户端、健康检查和遥测 | 否 |
| `yuheng-mcp-gateway` | 可执行的第二种 Engine 角色（`McpGatewayEngineApplication`），承载 MCP 数据面 | 否 |
| `yuheng-admin` | 可执行管理控制面、持久化、规则编译、鉴权和 OpenAPI | 否 |
| `yuheng-starter` | Provider 接口定义上报，并向 Tianshu HTTP 注册贡献 Yuheng 元数据 | 是 |
| `yuheng-starter-openapi` | 与框架无关的 Provider OpenAPI 治理扩展 | 是 |
| `yuheng-starter-openapi-webmvc` | OpenAPI 治理扩展的 Spring MVC 适配器 | 是 |
| `yuheng-starter-openapi-webflux` | OpenAPI 治理扩展的 Spring WebFlux 适配器 | 是 |
| `yuheng-test` | 真实 HTTP/RPC Provider、Consumer 和拓扑验证 | 否 |

Admin Web 是与 Yuheng 源码同目录的私有 React 应用，路径为
`egon-cola-yuheng/yuheng-admin-web`；它不是 Maven
子模块。详见 [前端 README](yuheng-admin-web/README.zh-CN.md)。

## 运行能力

- Public 和 Internal HTTP 监听器，提供有界请求体、CORS、安全过滤器、协议重试、
  幂等传递和优雅 Drain。
- 基于 OpenAI HTTP 调用规范的透明传输，支持原始 JSON、逐 Buffer Flush 的 SSE、
  Multipart 大文件流式上传、图片/音频等多模态与二进制请求响应，以及两阶段
  `ws`/`wss` Realtime WebSocket 双向代理。
- 通过不可变路由与 Provider 快照支持 HTTP→HTTP、HTTP→RPC 和 RPC→RPC 转发。
- 基于 Tianshu 租约的 Provider 发现、主动健康探测、有界 Provider 尝试、负载均衡，
  以及过期/不健康实例摘除。
- Yuheng Admin 草稿、接口目录、规则版本编译、规范化摘要、鉴权管理 API 和
  运行时定义校准。
- HTTP、RPC、Tianshu 直连 RPC 和管理传输的 TLS/mTLS，以及受控证书刷新和监听器 Drain 操作。
- Micrometer Observation / OpenTelemetry Span 和有界 Kafka 调用事件投影；遥测
  故障不得改变业务响应。

## MCP 扩展边界

现有持久化 Tasks 是 Egon 扩展，不是 MCP 2025-11-25 标准 Tasks。
Stable 初始化通过 `capabilities.experimental["top.egon/tasks"]` 声明任务扩展，
UI 资源扩展通过 `capabilities.experimental["top.egon/apps"]` 声明。
保留现有任务策略以及 `tasks/get`、`tasks/update`、`tasks/cancel`，
未实现标准任务增强协商、`tasks/list` 和 `tasks/result`。
RC 发现仍保留既有方言描述；Resource、Prompt 回归通过不代表标准 Tasks 或 Apps 兼容性。

## OAuth Resource 绑定

Yuheng 从可信的路由目标解析期望的 Resource Server，而不是从调用方提供的 Header 或请求
参数解析。一条路由指向唯一的 `bizCode + appCode + environment` 三元组，对应一个绝对
Resource URI，例如
`https://api.egon.internal/prod/permission/tianquan-shoubing`。Tianquan-Shoubing 适配器
只接受单一 Audience 的 Access Token，且其 `aud` 与 `resource_version` 必须匹配该路由
Resource，principal 类型为 `USER` 或 `SERVICE`。

Yuheng 负责认证、精确 Resource 绑定、可信身份 Header 替换和路由。它不判定用户角色，也不
判定接口/数据/字段权限，更不会询问 Tianquan-Jianshen 某个服务能否调用另一个服务。下游
服务会重复精确 Resource 校验：`USER` 继续进入 Tianquan-Jianshen 授权，`SERVICE` 则在本地
与该操作要求的 Tianquan-Shoubing Scope 比对。

Yuheng Admin 和两种 Engine 角色本身也是 Resource Server。它们通过标准 Spring OAuth2
Client `client_credentials` 流程，为每次 Tianshu 注册和心跳取得一枚新的 Tianquan-Shoubing
SERVICE Token（Scope 为 `tianshu:registration:write`）；不存在独立的注册票据。Tianshu 把
Token Claims 记录为实例上的准入投影（`resourceServerId`、`resourceVersion`、
`credentialId`、`admissionExpiresAt`）。客户端凭据配置在
`egon.cola.platform.tianquan.shoubing.service-client.*`，并用
`egon.cola.component.tianshu.registration-resource-uri` 指向 Tianshu Resource；各项
`private-key-path` 配置是 TLS 证书私钥，不是 OAuth 凭据。

禁用 Resource 会摘除它所准入的租约——配置客户端和 Provider 都包括在内——并阻止新签发
Token。恢复 Resource、凭据、Grant 和路由定义后，实例取得新的 SERVICE Token 即可正常校准。
Schema 顺序为：Tianquan-Shoubing 到 V6、Tianshu 到 V9、Yuheng Admin 到 V13。Yuheng V11
把 `gateway_mcp_server.oauth_audience` 重命名为 `resource_uri`；该重命名无法回退到仍读取
旧列的二进制，但 V11 自身不删除任何内容。

## Trace 传播

Yuheng 数据面使用 `egon-cola-component-common-trace` 的 W3C Trace Context 能力。
入口只从合法 `traceparent`、`tracestate` 和 `x-egon-request-id` 建立上下文；Yuheng
不再读取或写入 `X-Trace-Id`、`x-trace-id` 或 `x-egon-trace-id`。HTTP 上游和 RPC
上游都会为每次 Provider Attempt 创建独立 child span，重试不会复用同一个 attempt
`spanId`，但整个请求保持同一个 `traceId`。

Yuheng 已接入 Micrometer Observation / OpenTelemetry。存在有效 Observation Span
时，`GatewayCallEventV1.Trace`、普通日志和下游 `traceparent` 会以当前 Span 为准；
没有 Tracer 时才使用 `common-trace` 的轻量生成逻辑。

## 消费和构建

Components BOM 不再导出 Yuheng Artifact。需要上报 Yuheng 接口定义的业务系统只需
依赖 `yuheng-starter`，它会组装 Tianshu HTTP 注册 Starter；希望 OpenAPI 文档一并被
Admin 治理的 Provider 应用，再追加 `yuheng-starter-openapi` 与对应的
`yuheng-starter-openapi-webmvc` 或 `yuheng-starter-openapi-webflux` 适配器。不需要
Yuheng 接口定义上报、只需注册 HTTP 服务的应用，才直接依赖
`egon-cola-tianshu-http-registration-starter`。Engine、Admin、Contract、Core 和 test
属于平台内部模块，应通过仓库的 Yuheng 拓扑构建或部署。

HTTP 注册 Java 类型已迁入 `top.egon.cola.component.tianshu.http.registration`，Yuheng
类型继续位于 `top.egon.cola.component.yuheng`。

执行 JVM 专项验证：

```bash
./mvnw -B -ntp \
  -pl egon-cola-xingyuan/egon-cola-yuheng/yuheng-test \
  -am test
```

live Profile 会由测试 Harness 启动真实 Provider/Consumer 拓扑。默认使用
Testcontainers；当 `PATH` 中存在 `initdb`、`postgres` 和 `redis-server` 时，也可使用
隔离的本机临时进程：

```bash
./mvnw -B -ntp \
  -pl egon-cola-xingyuan/egon-cola-yuheng/yuheng-test/yuheng-test-suite \
  -am -Pyuheng-live verify

./mvnw -B -ntp \
  -pl egon-cola-xingyuan/egon-cola-yuheng/yuheng-test/yuheng-test-suite \
  -am -Pyuheng-live -Dgateway.live.infrastructure=local verify
```

## 运维文档

| 文档 | 用途 |
|---|---|
| [Yuheng + Tianshu + RPC 联调](docs/developer-integration.zh-CN.md) | 端到端 Demo 命令、成功判据、故障演练和证据边界 |
| [本地部署](deployment/README.zh-CN.md) | Compose 构建、端口、Ready、HA 样例、TLS/mTLS 和启停顺序 |
| [性能和故障演练](performance/README.zh-CN.md) | k6 smoke/baseline、长稳、资源采样和固定故障场景 |
| [Admin Web](yuheng-admin-web/README.zh-CN.md) | React 构建、测试、浏览器鉴权和 API Origin 配置 |

## 边界

- Nginx 节点管理、Nginx 动态配置和外部负载均衡不属于 Yuheng 平台。多 Engine
  前的入口及 L4/L7 负载均衡由部署环境负责。
- 基础 Compose 拓扑是本地开发依赖集合。HA overlay 只验证多个无状态 Admin 进程和
  代理路由，不会把单节点 PostgreSQL、Redis 或 Kafka 变成生产 HA 服务。
- Yuheng 不包含通用账号系统或外部 IAM。Admin Web 经平台统一身份以 CSRF 保护的 Cookie
  会话登录，Yuheng Admin 依据 `GET /api/v1/auth/bootstrap` 返回的 Actor 和 capability
  边界执行鉴权。
- OpenAI Route Profile 只是传输配置预设，不是 AI 业务平台。Yuheng 不统计 Token、
  不计费、不管理 Prompt 或会话、不执行 RAG/Agent 编排或 Function Calling，也不做
  业务模型选择；它只识别请求、匹配路由、承载协议并透明转发字节。
- 流式组件测试只证明进程内 Yuheng 边界，不证明公网 OpenAI、外部/私有 CA TLS、
  多进程基础设施，或外层 Nginx/Ingress 的 Flush 与缓存行为。
- 实现和部署契约仍在演进；当前版本证据以专项测试和下方运维文档为准。
