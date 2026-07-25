# GWS-12 Gateway Trace、可观测性与调用事件 Spec

状态：草案，等待审核

父文档：`2026-07-24-gateway-component-design.md`

索引：`2026-07-25-gateway-child-spec-index.md`

依赖：GWS-01、GWS-03、GWS-04

主模块：

- `egon-cola-component-gateway-engine`
- `egon-cola-component-gateway-contract`
- `egon-cola-component-gateway-admin`
- `egon-cola-component-gateway-test`

## 1. 目标

本 Spec 定义一次 Gateway 调用从入口到 Provider 的 Trace、结构化日志、指标和 Kafka
调用事件闭环。

核心原则：

1. Trace ID 由前端/调用方优先生成，缺失或非法时 Engine 生成；
2. Trace 贯穿 HTTP、RPC、日志、响应和 Kafka 事件；
3. 每次进入 Engine 的调用由 Engine 生成一条调用完成事件；
4. Starter 不拦截调用、不生成调用事件、不持有 Kafka Producer；
5. Kafka 投递异步、有界、可观测，故障不能改变业务响应；
6. 默认不记录请求/响应 Body、Credential 或敏感身份信息；
7. 指标保持低基数，Trace/Operation 明细通过日志或事件查询。

## 2. Trace Context

### 2.1 接受顺序

Engine 按以下顺序选择 Trace：

1. 合法 W3C `traceparent`；
2. 合法约定 Header `X-Trace-Id`；
3. Engine 生成。

合法 Trace ID：

- 32 个十六进制字符；
- 大小写输入规范化为小写；
- 不能全为 0；
- 不接受任意长度字符串；
- 不从 Query/Cookie/Body 读取。

若 `traceparent` 与 `X-Trace-Id` 都合法但不一致，以 `traceparent` 为准，并增加冲突
指标，不记录两个原始值。

Gateway Admin 的管理 API 和 Starter OpenAPI 复用同一 Trace ID 校验器：接受前端或
Starter 的合法 Trace ID，缺失或非法时由 Admin 生成。Kafka 调用事件仍只能由 Engine
生成。

### 2.2 Engine Span

每次请求生成新的 Engine Span ID：

```text
traceId
parentSpanId
engineSpanId
traceFlags
tracestate
```

- 接受的调用方 Span 是 Parent；
- Engine 调用 Provider 创建 Child Span；
- 每次重试 Attempt 创建独立 Child Span；
- 不允许复用客户端 Span ID 作为 Engine Span；
- Sampling Flag 遵循 OpenTelemetry/W3C 语义，但调用事件不依赖 Trace Sampling。

### 2.3 传播

| 边界 | 传播 |
|---|---|
| HTTP 入站 | `traceparent`、受控 `tracestate`、兼容 `X-Trace-Id` |
| HTTP Provider | 新 Child `traceparent`、受控 `tracestate`、`X-Trace-Id` |
| RPC 入站/出站 | Egon RPC Metadata 中的 W3C Trace 字段 |
| HTTP Response | 始终返回 `X-Trace-Id`；需要时追加服务端 `traceparent` |
| gRPC Response | Trailer/Metadata 中的 Trace ID，遵循 RPC 契约 |
| 日志 | MDC/Reactive Context 中 `traceId`、`spanId` |
| Kafka Event | `traceId`、`engineSpanId` 和 Attempt Span |

Provider 返回的冲突 Trace Header 不覆盖 Engine 当前 Trace。

### 2.4 Reactor/MDC

复用 `egon-cola-component-common-trace` 的 MDC 能力，但必须增加 Reactor Context
桥接：

- 在请求订阅时安装 Trace Context；
- 每个异步回调恢复必要 MDC；
- 回调结束清理，防止 EventLoop 线程串号；
- 不依赖 ThreadLocal 在整个 Reactive Chain 中天然传播；
- 阻塞 Adapter 切换线程池时使用 Context Snapshot。

## 3. 请求生命周期观测

每次进入 Listener 都创建 `GatewayCallObservation`，即使最终没有匹配 Route：

```text
ACCEPTED
→ ROUTED | REJECTED_BEFORE_ROUTE
→ GOVERNED | REJECTED_BY_POLICY
→ PROVIDER_ATTEMPTED | REJECTED_BEFORE_PROVIDER
→ COMPLETED | CANCELLED | FAILED
```

Observation 在请求终止钩子中只完成一次，覆盖：

- 正常响应；
- Route 不存在；
- 外部暴露拒绝；
- 认证/授权拒绝；
- 限流/熔断/隔离拒绝；
- 参数绑定失败；
- Provider 成功/失败；
- 客户端取消；
- Deadline；
- Engine 内部错误。

使用观察者/生命周期回调模式，把日志、指标和事件订阅到统一完成事实，避免每个 Filter
重复编写且产生不同结果。

## 4. 结构化日志

### 4.1 字段

请求完成日志至少包含：

```text
timestamp
level
event = gateway_call_completed
traceId
spanId
requestId
gatewayGroupId
engineNodeId
releaseId
protocol
accessZone
operationId
routeId
providerServiceIdentity
providerInstanceId
attemptCount
resultCategory
gatewayErrorCode
httpStatus/grpcStatus
durationMs
requestBytes
responseBytes
```

未路由时相关 ID 为空，不使用虚假占位值。

### 4.2 脱敏

复用 `egon-cola-component-common-mask` 并设置固定敏感字段：

- Authorization；
- Cookie/Set-Cookie；
- Token/Secret/API Key；
- Credential；
- 可信身份 Header；
- DDC/Kafka 配置；
- Query 中配置标记的敏感字段。

默认不记录：

- 请求/响应 Body；
- 完整 Header/Metadata；
- 完整 Query；
- Principal ID；
- Provider 返回业务数据；
- Protobuf 原始字节。

任何诊断采样扩展都需独立 Spec，不允许通过打开 DEBUG 绕过。

### 4.3 日志级别

- 正常成功：INFO 或受控 Access Log；
- 客户端参数/限流拒绝：INFO；
- Provider 业务错误：INFO/WARN 按映射；
- Provider 不可用、超时：WARN；
- Engine 内部异常、规则损坏：ERROR；
- 客户端取消不默认 ERROR。

## 5. 指标

使用 Micrometer，支持对接 OpenTelemetry/Prometheus，但 Core 不依赖具体 Registry。

### 5.1 请求

```text
gateway_requests_total{
  gatewayGroup,protocol,accessZone,resultCategory
}

gateway_request_duration_seconds{
  gatewayGroup,protocol,resultCategory
}

gateway_requests_inflight{
  gatewayGroup,protocol
}
```

### 5.2 Route/Provider

```text
gateway_operation_requests_total{gatewayGroup,operationCode,resultCategory}
gateway_provider_attempts_total{gatewayGroup,serviceCode,resultCategory}
gateway_provider_duration_seconds{gatewayGroup,serviceCode,resultCategory}
gateway_provider_selected_total{gatewayGroup,serviceCode,zone}
gateway_retry_attempts_total{gatewayGroup,reason}
```

Operation Code/Service Code 只有在数量受平台上限治理时才允许作为 Label；超出预算时
降级到聚合指标并发出 Cardinality 告警。

### 5.3 治理和运行态

```text
gateway_rate_limit_rejected_total{scope,mode}
gateway_circuit_breaker_state{policy}
gateway_concurrency_rejected_total{policy}
gateway_rule_active_info{gatewayGroup,release}
gateway_rule_apply_total{stage,status}
gateway_provider_directory_instances{gatewayGroup,service,state}
```

### 5.4 禁止的 Label

- Trace/Request/Release UUID 明细；
- 原始 Path；
- Provider Instance ID；
- Principal/Tenant/User；
- 客户端 IP；
- 错误 Message；
- 任意 Header/Query 值。

Release ID 用于日志/事件；`gateway_rule_active_info` 仅作为低频 Info Metric，并限制每
Group 一条。

## 6. Kafka 调用事件

### 6.1 Topic

推荐：

```text
topic = egon.gateway.call-events.v1
partitionKey(match)   = gatewayGroupId + ":" + operationId
partitionKey(unmatch) = gatewayGroupId + ":" + eventId
```

未路由请求使用 Event ID 打散，避免探测流量集中到单一 Partition。Topic 名支持部署
配置，但 `eventSchemaVersion=v1` 不随 Topic 改名。

### 6.2 事件 Envelope

```text
GatewayCallEventV1
├── eventSchemaVersion = "v1"
├── eventId
├── occurredAt
├── completedAt
├── trace
│   ├── traceId
│   ├── engineSpanId
│   └── sampled
├── request
│   ├── requestId
│   ├── protocol
│   ├── accessZone
│   ├── normalizedMethod
│   ├── normalizedRouteTemplate
│   ├── requestBytes
│   └── clientNetworkClass
├── routing
│   ├── gatewayGroupId
│   ├── engineNodeId
│   ├── releaseId
│   ├── operationId
│   ├── routeId
│   └── providerServiceIdentity
├── governance
│   ├── terminalStage
│   ├── rateLimitDecision
│   ├── circuitDecision
│   ├── securityDecision
│   └── retryCount
├── result
│   ├── category
│   ├── gatewayErrorCode
│   ├── httpStatus
│   ├── grpcStatus
│   ├── responseBytes
│   └── durationMs
└── attempts[]
    ├── attempt
    ├── spanId
    ├── providerInstanceId
    ├── startedAt
    ├── durationMs
    ├── resultCategory
    └── retryReason
```

`eventId` 使用 UUIDv7，每个 Engine 请求只生成一次。一次请求的所有 Provider Retry
放在同一事件中，不为每个 Attempt 再生产一条顶层调用事件。

### 6.3 不允许出现

- Request/Response Body；
- Authorization、Cookie、Token、Credential；
- 原始 Header/Metadata；
- 未脱敏 Query；
- Principal ID/业务身份属性；
- Java Exception Stack；
- DDC/Kafka Secret；
- Protobuf Payload；
- Provider 业务响应文本。

错误只保存标准 Category、Gateway Error Code 和受控诊断码。

### 6.4 序列化

首期使用 UTF-8 JSON：

- Kafka Key 是上述 UTF-8 Partition Key；
- Value 是 `GatewayCallEventV1`；
- Header 包含 `content-type=application/json`、`event-schema-version=v1`、
  `event-id`；
- 不写 Java Class 名或多态类型提示；
- 字段顺序不作为消费者契约；
- 时间统一为 UTC epoch milliseconds；
- 单事件默认硬上限 64 KiB；
- Attempts 数受 GWS-07 `maxAttempts` 硬上限控制；
- Producer 压缩使用 Kafka 批次压缩，不在 Value 中嵌套自定义压缩。

选择 JSON 是为了首期无需额外 Schema Registry 即可让 Admin/Test Consumer 使用。
事件演进仍按显式 Version 和兼容测试治理；如果后续切换 Protobuf/Avro，使用新
Content Type 和独立兼容方案，不能在同一 `v1` 中静默替换。

## 7. 事件生成与投递

### 7.1 生成

调用完成钩子把 `GatewayCallObservation` 转为不可变事件。转换发生在响应业务结果已经
确定之后，序列化或 Kafka 异常不能修改响应。

每个进入 Engine 的请求都尝试生成事件，包括未路由和治理拒绝。健康检查、Engine
管理端点等非业务 Listener 可以通过固定类型排除，不能由普通 Route 任意关闭。

### 7.2 有界异步链路

```text
Request Completion
→ Event Builder
→ Bounded In-Memory Queue
→ Serializer
→ Kafka Producer
→ Callback Metric
```

约束：

- EventLoop 只执行有界对象构建和非阻塞入队；
- 队列同时限制事件数和估算字节数；
- Producer 工作在独立线程；
- 不在请求线程等待 Kafka ACK；
- 不允许无界重试或无界内存；
- Engine 关闭时在配置的短超时内 Drain，超时后记录丢弃数量；
- 事件 Schema 校验失败进入本地错误计数，不发送非法消息。

### 7.3 Producer

推荐基线：

```text
acks = all
enable.idempotence = true
retries = bounded by delivery.timeout.ms
compression.type = lz4
```

Kafka Producer 的内部缓冲也必须有上限。Producer ID 幂等减少重试重复，但消费者仍
必须按 `eventId` 幂等，因为进程重启和未知 ACK 仍可能重复。

### 7.4 可靠性级别

首期采用：

```text
BEST_EFFORT_NON_BLOCKING
```

语义：

- 正常情况下每次调用异步发送一条事件；
- Kafka 暂时失败由 Producer 在 Delivery Timeout 内重试；
- 队列满、持续 Kafka 故障或 Engine 崩溃可能丢失事件；
- 事件丢失不改变已经确定的业务响应；
- 队列满采用丢弃新事件并计数/告警，不阻塞 EventLoop；
- 不声称端到端 Exactly Once 或持久化 At Least Once。

不使用 Transactional Outbox：调用热路径没有可复用业务数据库事务，先落本地数据库
会放大延迟和故障面。若未来要求审计级不丢失，应单独设计磁盘 Spool/WAL，而不是修改
本期语义。

## 8. Kafka 故障与背压

需要区分：

```text
queue_rejected
serialization_failed
producer_send_failed
producer_timeout
callback_failed
shutdown_dropped
```

指标：

```text
gateway_call_event_enqueued_total
gateway_call_event_sent_total
gateway_call_event_failed_total{reason}
gateway_call_event_dropped_total{reason}
gateway_call_event_queue_depth
gateway_call_event_queue_bytes
gateway_call_event_send_duration_seconds
```

阈值告警关注丢弃率、队列利用率和最长未成功时间。禁止将事件 Payload 写入失败日志。

## 9. Admin 消费与展示投影

Admin 可启用独立 Consumer Group 消费调用事件：

```text
group.id = egon-gateway-admin-observability-v1
```

职责：

- 按 `eventId` 幂等；
- 写入分钟级低基数聚合；
- 保存受控、短保留期的调用摘要用于 Trace 查询；
- 更新 Operation/Route/Provider 维度统计；
- 提交 Offset 前完成本批次持久化；
- 毒消息进入隔离 Topic/记录，不阻塞整个 Partition。

建议逻辑表：

```text
gateway_call_event_summary
gateway_call_metric_minute
gateway_call_event_consume_failure
```

默认保留建议：

- 调用摘要 7 天；
- 分钟聚合 90 天；
- 消费失败 14 天；
- 具体值由部署配置决定并在页面显示。

该 PostgreSQL 投影适合首期验证和中等规模。大规模生产可通过
`GatewayObservabilityStore` Port 接入专用时序/OLAP 存储，但不能改变 Kafka
Schema 或 Engine 投递语义。

## 10. OpenTelemetry

- Engine Request、Provider Attempt、DDC Apply、Kafka Send 形成 Span；
- Metric 使用 Micrometer Observation；
- 没有配置 Collector 时仍保留日志、指标和 Kafka 事件；
- Exporter 故障不能阻塞请求；
- Attribute 遵守低基数和敏感信息规则；
- Trace Sampling 只影响 Span Export，不影响调用事件生成；
- Engine 不要求下游 Provider 必须部署 OpenTelemetry 才能转发 Trace。

## 11. 配置

```yaml
egon:
  cola:
    component:
      gateway:
        observability:
          trace-header: X-Trace-Id
          call-event:
            enabled: true
            topic: egon.gateway.call-events.v1
            queue-capacity: 10000
            queue-max-bytes: 67108864
            shutdown-drain-timeout: 5s
```

边界值实施时按性能测试确定，配置必须校验：

- 容量大于 0 且有上限；
- Topic 非空且符合 Kafka 命名；
- Drain Timeout 有上限；
- 生产环境 `enabled=false` 时启动警告并暴露健康状态；
- 不在普通配置 Endpoint 返回 Kafka Secret。

## 12. 错误与兼容

- 未知 Event Schema 由消费者隔离，不猜测；
- 新字段只做向后兼容追加；
- 删除/改变字段语义需要 `v2` Topic/Schema；
- 时间使用 UTC epoch milliseconds；
- Duration 使用整数毫秒并校验非负；
- Consumer 写入失败不回退 Offset；
- 重复 Event ID 幂等成功；
- Admin 页面必须区分“无流量”和“Consumer/数据源异常”。

## 13. 测试设计

### 13.1 Trace

1. 合法 `traceparent` 优先；
2. 合法 `X-Trace-Id` 回退；
3. 缺失、全 0、长度错误、非法字符时生成；
4. 两 Header 冲突；
5. HTTP、RPC、HTTP→RPC 传播；
6. Retry Attempt Span；
7. Reactive 线程切换不串 Trace；
8. Response 带最终 Trace ID；
9. 前端生成 Trace 贯穿 Admin 和业务调用。

### 13.2 日志与指标

1. 全部终止路径只记录一次完成日志；
2. Body/Credential/Header 脱敏；
3. 客户端取消分类；
4. 指标 Label 白名单；
5. Operation 基数预算降级；
6. Kafka/Exporter 故障不改变业务响应。

### 13.3 Kafka

1. 正常调用、未路由、限流、鉴权拒绝、超时、取消各一条事件；
2. 多次 Retry 仍是一条顶层事件；
3. Schema 字段、时间和 SHA/ID 格式；
4. 事件不包含 Body/Credential；
5. Producer ACK 成功；
6. Kafka 暂时故障后有界重试；
7. 队列满丢弃并计数，不阻塞 EventLoop；
8. Shutdown Drain；
9. Consumer Event ID 幂等；
10. 毒消息隔离且后续消息继续消费。

### 13.4 真实集成

使用 Testcontainers Kafka：

1. 启动真实 Engine、Provider、Consumer；
2. 发起 HTTP/RPC 调用；
3. 从 Kafka 消费并断言 Trace/Route/Operation/Node/结果；
4. 验证 Starter 进程没有 Producer/事件；
5. 停止 Kafka 后业务调用仍返回原结果；
6. 恢复 Kafka 后新事件恢复发送；
7. Admin 能查询聚合和受控 Trace 摘要。

## 14. 验收标准

1. 调用方合法 Trace ID 被接受，缺失或非法时 Engine 生成；
2. Trace 贯穿 HTTP、RPC、日志、响应和 Kafka；
3. 每次 Engine 业务调用都尝试异步发送一条完成事件；
4. Retry 以 Attempt 数组表达，不重复顶层事件；
5. Starter 完全不参与调用事件；
6. Kafka 故障、队列满或序列化错误不改变业务响应；
7. 投递链路有界并具备丢弃/失败指标；
8. 事件和日志不含 Body、Credential 或敏感身份；
9. Admin Consumer 幂等并可展示聚合与 Trace 摘要；
10. 不声明首期具备审计级不丢失或 Exactly Once。

## 15. 本轮审核项

1. 认可 Trace 选择优先级为 `traceparent` → `X-Trace-Id` → Engine 生成；
2. 认可每个请求一条顶层事件、Retry 放入 Attempt 数组；
3. 认可首期 Kafka 可靠性为有界异步 Best Effort；
4. 认可 Kafka 故障永不改变业务响应；
5. 认可 Admin 首期保存短期摘要和分钟聚合，大规模可替换 Store；
6. 认可默认不采集请求/响应 Body、Credential 和完整 Header。
