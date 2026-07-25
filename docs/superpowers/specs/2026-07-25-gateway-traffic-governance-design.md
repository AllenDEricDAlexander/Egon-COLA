# GWS-07 Gateway 流量治理 Spec

状态：已实现，待用户验收

父文档：`2026-07-24-gateway-component-design.md`

索引：`2026-07-25-gateway-child-spec-index.md`

依赖：GWS-03、GWS-05、GWS-06

主模块：

- `egon-cola-component-gateway-core`
- `egon-cola-component-gateway-engine`
- `egon-cola-component-gateway-admin`

## 1. 目标

在 Engine 请求热路径实现可配置、可版本化的治理能力：

- 端到端超时；
- 本地与分布式限流；
- 并发隔离；
- 熔断；
- 有条件重试；
- 请求资源保护；
- Provider 健康联动。

Admin 只配置和发布 Policy，不进入业务请求热路径。

## 2. 治理阶段

```text
Request Guard
→ Rate Limit
→ Operation/Service Concurrency Acquire
→ Provider Select
→ Provider Instance Circuit/Bulkhead Permission
→ Attempt / Retry
→ Record Result
→ Release Instance and Operation/Service Permits
```

规则：

- Exposure/Security 在治理前完成；
- 全局保护性限流可以在完整参数绑定前执行；
- 需要 Caller/业务 Key 的限流在身份和必要参数提取后执行；
- Operation/Service Permit 在 Provider 选择前获取，Instance Permit 在选定实例后获取；
- Concurrency Permit 在所有成功、失败、超时、取消路径释放；
- Observation 不改变治理结果。

## 3. Policy 公共结构

```text
RuntimeTrafficPolicy
├── policyId
├── policyType
├── scope
├── enabled
├── priority
├── keyExpression
├── failureMode
├── parameters
├── stateEpoch
└── policyVersion
```

### 3.1 作用域

支持：

- GLOBAL；
- GATEWAY_GROUP；
- APPLICATION；
- BUSINESS_DOMAIN；
- ENTITY_DOMAIN；
- INTERFACE_GROUP；
- OPERATION；
- ROUTE；
- CALLER；
- PROVIDER_SERVICE；
- PROVIDER_INSTANCE。

多个非互斥 Policy 可以叠加，例如 Global + Operation 限流。互斥 Policy 按 GWS-06
作用域和优先级在发布时解析为确定结果。

### 3.2 Key Expression

允许从受控字段构造：

- operationId；
- routeId；
- applicationCode；
- principal/caller ID；
- Client IP；
- Header 白名单；
- Path/Query 中已声明字段；
- Provider Service/Instance。

不执行任意脚本、SpEL、Groovy 或 JavaScript。Expression 在发布时编译为受限
Key Extractor。

## 4. 超时

### 4.1 Timeout Budget

```text
effectiveDeadline =
min(
  inboundDeadline,
  routeTimeout,
  operationTimeout,
  engineMaximumTimeout
)
```

预算分为：

- 网关前置处理；
- Provider Acquire/Connect；
- Provider Response；
- 可选 Retry；
- 响应写回。

### 4.2 规则

- 默认 Operation Timeout 必须配置系统值；
- Engine 最大超时防止错误配置无限请求；
- 剩余预算小于最小 Attempt 预算时不发起新 Attempt；
- 客户端取消优先于 Timeout；
- HTTP 映射 504，gRPC 映射 `DEADLINE_EXCEEDED`；
- Timeout 必须取消上游调用并释放 Permit；
- 不通过延长 Deadline 掩盖网关处理耗时。

## 5. 本地限流

### 5.1 算法

首期使用 Token Bucket：

```text
capacity
refillTokens
refillPeriod
initialTokens
```

- 单 Engine 内存状态；
- 原子扣减；
- 使用单调时间；
- Policy State 按 `policyId + stateEpoch + keyHash`；
- Key 数量有上限与空闲 TTL；
- 不为每个原始 Path 创建状态。

### 5.2 适用

- 单节点保护；
- 分布式限流前的快速削峰；
- DDC/Redis 降级期间的本地保护；
- Provider Instance 局部保护。

本地限流不宣称跨 Engine 共享配额。

## 6. 分布式限流

### 6.1 Redis

使用独立 Gateway Redis Key Space，不使用 DDC 私有 Key：

```text
gateway:ratelimit:{env}:{namespace}:{policyId}:{stateEpoch}:{keyHash}
```

Key 中不保存原始凭据、手机号、Token 或长 URL。

### 6.2 原子算法

使用 Redis Lua 实现 Token Bucket：

- 当前 Token；
- 最后补充时间；
- 当前请求消耗；
- TTL；
- Server Time 或校正后的统一时间策略。

一次脚本调用返回：

```text
allowed
remaining
retryAfterMillis
resetAt
```

不能通过 GET/SET 多命令实现非原子扣减。

### 6.3 Failure Mode

每个 Policy 必须明确：

| Mode | Redis 不可用 |
|---|---|
| `DENY` | 拒绝请求，适合安全/合同硬配额 |
| `LOCAL_FALLBACK` | 使用独立本地桶，适合保护性限流 |

不支持静默 `ALLOW_ALL`。默认保护型 Policy 使用 `LOCAL_FALLBACK`，明确的外部调用
配额使用 `DENY`。

### 6.4 响应

- HTTP：429，带受控 `Retry-After`；
- gRPC：`RESOURCE_EXHAUSTED`；
- 不暴露 Redis 状态；
- 记录 policyId、scope、decision，不记录原始敏感 Key。

## 7. 并发隔离

### 7.1 Semaphore Bulkhead

首期使用非阻塞 Semaphore：

```text
maxConcurrent
maxWait = 0 by default
```

- EventLoop 不阻塞等待 Permit；
- 无 Permit 立即拒绝；
- 可以按 Operation/Provider Service/Instance 设置；
- Operation/Service Permit 覆盖整个请求及其全部 Retry，Instance Permit 只覆盖单个
  Attempt；
- Permit 在取消和异常时释放；
- 动态缩小上限不取消既有请求，只影响新 Acquire；
- 不创建无界等待队列。

### 7.2 线程隔离

仅对明确的阻塞扩展点使用有界 Worker Pool。HTTP/gRPC 非阻塞调用不切换到通用线程池。
`dynamic-thread-pool` 可以在后续用于这些 Worker Pool，但不管理 Netty EventLoop。

## 8. 熔断

### 8.1 State

```text
CLOSED → OPEN → HALF_OPEN → CLOSED
```

State Key：

```text
policyId + stateEpoch + providerServiceKey + instanceId + leaseId
```

新 leaseId 代表新运行实例，不继承旧进程的熔断窗口。

### 8.2 计入失败

计入：

- Connect Failure；
- Reset；
- Timeout；
- gRPC `UNAVAILABLE`；
- 配置的可重试 5xx。

不计入：

- 业务 4xx；
- `INVALID_ARGUMENT`；
- `UNAUTHENTICATED`；
- `PERMISSION_DENIED`；
- 客户端主动取消。

### 8.3 行为

- OPEN 实例在 Candidate Filter 中排除；
- HALF_OPEN 允许有界探测；
- 所有实例 OPEN 时返回 Provider Unavailable/Circuit Open；
- 熔断状态是 Engine 本地状态；
- 不写入 DDC；
- Rule 不兼容变更使用新 stateEpoch，兼容阈值调整可保留历史窗口。

## 9. 重试

### 9.1 默认

默认不重试。Retry Policy 必须显式配置。

### 9.2 允许条件

同时满足：

1. Operation 标记幂等；
2. Method/业务契约允许；
3. 错误属于明确可重试集合；
4. 剩余 Deadline 足够；
5. 未超过 `maxAttempts`；
6. 请求 Body 可重放且未超缓存限制；
7. 没有收到可证明业务成功的响应。

### 9.3 禁止

- 非幂等写请求默认禁止；
- 认证/授权/参数错误不重试；
- Rate Limit 拒绝不重试；
- Provider 返回业务失败不重试；
- 客户端取消不重试；
- Streaming Body 不可重放时不重试。

### 9.4 Provider 选择

- 每个 Attempt 重新执行候选过滤；
- 默认排除上一失败实例；
- 没有其他实例时可按 Policy 决定是否复用；
- Invocation ID 保持，Attempt ID 变化；
- 总 Attempt 数包含首次调用。

### 9.5 Backoff

- 支持固定/指数 Backoff；
- 必须有最大值；
- 可添加 Jitter；
- Backoff 占用总 Deadline；
- 使用非阻塞 Scheduler。

## 10. 请求资源保护

### 10.1 固定保护

- Request Line；
- Header 数量/单值/总大小；
- Body 大小；
- Query 参数数量；
- Path Segment 数量；
- Metadata 大小；
- RPC Message 大小；
- 连接空闲时间。

### 10.2 Route 覆盖

Route 只能在系统允许范围内收紧或受控放宽。超过 Engine 硬上限的发布配置失败。

### 10.3 拒绝顺序

尽可能在读取完整 Body、执行鉴权 Provider 或查询 Redis 前拒绝明显超限请求。

## 11. Policy 动态更新

### 11.1 兼容更新

以下可以保留 State：

- Token Bucket 阈值调整；
- Circuit 窗口阈值调整；
- Concurrency 上限调整；
- Retry Backoff 调整。

### 11.2 不兼容更新

以下必须增加 `stateEpoch`：

- Key Expression 改变；
- Policy Scope 改变；
- 算法改变；
- Redis Key 语义改变；
- Provider Service 维度改变。

旧 State 通过 TTL/Retention 自然清理，不在 Rule 激活热路径批量删除。

## 12. 组件复用

首期选用 Resilience4j Core/Reactor Adapter，复用：

- Circuit Breaker；
- Retry 状态机；
- Bulkhead；

不使用其 Spring Annotation 或 Spring Cloud CircuitBreaker 把 Policy 隐式绑定到业务
方法；Gateway Infrastructure 根据编译后的 Policy 显式创建/释放实例。版本进入仓库
Dependency Management。

必须自研/明确实现：

- 本地 Token Bucket；
- Redis Lua 分布式 Token Bucket；
- Gateway Policy Scope/Key Compiler；
- Rule Snapshot 到治理实例的生命周期；
- HTTP/gRPC 错误映射。

不使用 `access-guard` 作为 Netty 热路径治理链，也不使用现有静态 `rule-engine`
解释动态 Gateway Policy。

选择成熟库是因为 Circuit/Retry/Bulkhead 涉及并发状态和取消边界，直接重复实现风险
高；Gateway 自有 Adapter 仍负责 `stateEpoch`、Provider Lease 隔离、Reactive
Cancellation 和规则原子切换。

## 13. 可观测性

每个决策记录：

- policyId/type/scope；
- allowed/rejected；
- fallback mode；
- retry attempt；
- circuit state；
- concurrency remaining；
- timeout stage。

指标标签不使用原始 Caller ID、IP、Path 或 instanceId。详细诊断进入受控日志/Trace。

## 14. 错误代码

```text
GATEWAY_RATE_LIMITED
GATEWAY_RATE_LIMIT_BACKEND_UNAVAILABLE
GATEWAY_CONCURRENCY_REJECTED
GATEWAY_CIRCUIT_OPEN
GATEWAY_RETRY_EXHAUSTED
GATEWAY_TIMEOUT
GATEWAY_REQUEST_LIMIT_EXCEEDED
```

## 15. 测试设计

### 15.1 Rate Limit

- 本地 Token 补充与并发扣减；
- Redis Lua 多 Engine 共享额度；
- Key Hash 不泄漏原值；
- DENY/LOCAL_FALLBACK；
- Policy State Epoch；
- Retry-After。

### 15.2 Concurrency/Circuit

- Permit 所有路径释放；
- 动态缩小；
- OPEN/HALF_OPEN/CLOSED；
- 业务错误不计入失败；
- Provider 摘除与熔断协同。

### 15.3 Retry/Timeout

- 幂等/非幂等；
- 多 Provider Attempt；
- Body 不可重放；
- Deadline 预算；
- Backoff 取消；
- 客户端取消不重试。

### 15.4 压力与故障

- Redis 延迟/中断；
- 大量动态 Key；
- 高并发 Permit；
- Rule 切换时旧 State 清理；
- EventLoop 无阻塞。

## 16. 验收标准

1. Admin 不进入治理热路径；
2. 本地与分布式限流语义清晰区分；
3. Redis 扣减使用单次 Lua 原子操作；
4. Redis 故障没有隐式 Allow All；
5. 并发隔离不阻塞 EventLoop；
6. 业务错误不错误熔断 Provider；
7. Retry 默认关闭且只用于显式幂等 Operation；
8. 所有 Attempt 共享总 Deadline；
9. 动态规则更新不会产生无界状态；
10. HTTP/gRPC 使用统一治理决策和协议状态映射。

## 17. 本轮审核项

1. 认可 Token Bucket 作为首期本地/分布式限流算法；
2. 认可 Redis 故障只有 DENY 或 LOCAL_FALLBACK；
3. 认可非阻塞 Semaphore Bulkhead；
4. 认可熔断以 Engine 本地 Provider Instance 为维度；
5. 认可 Retry 默认关闭、幂等显式开启；
6. 认可 `stateEpoch` 管理不兼容 Policy 变更；
7. 认可 Resilience4j 只复用算法，不接管 Gateway Policy 模型。
