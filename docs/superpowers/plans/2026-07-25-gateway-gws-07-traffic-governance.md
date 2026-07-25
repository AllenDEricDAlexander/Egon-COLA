# Gateway GWS-07 流量治理实现计划

状态：已执行

**Goal:** 在统一 HTTP/RPC Attempt 执行模型上实现有界、可动态更新、可观测的超时、限流、
隔离、熔断、重试和资源保护。

**Architecture:** Rule Compiler 把通用 Policy 编译为强类型 `RuntimeTrafficPolicy`；
Engine 以责任链执行 Request Guard、Rate Limit、Operation Bulkhead、Provider
Selection、Instance Circuit/Bulkhead、Attempt/Retry。状态统一以
`policyId + stateEpoch + boundedKeyHash` 隔离。

## 设计模式判断

- Strategy：本地/Redis Token Bucket 与不同 Failure Mode 独立替换。
- State：Circuit CLOSED/OPEN/HALF_OPEN 的并发转换必须显式。
- Decorator/Template：Attempt Executor 统一超时、重试、Permit 和结果记录。
- 不引入 Spring Annotation 治理，避免规则与业务方法静态绑定。

## Task 1: 强类型 Policy 与受限 Key Compiler

- 定义作用域、Failure Mode、stateEpoch、版本、参数边界。
- Key Expression 只支持已声明字段，不执行脚本或 SpEL。
- 原始 Key 只做 SHA-256，不进入 Redis Key、指标或普通日志。

**Commit:** `feat(gateway): compile traffic governance policies`

## Task 2: 本地/分布式限流

- 实现有界 Token Bucket、空闲 TTL 和最大 Key 数。
- 定义 Redis Lua Executor，单次脚本返回 allowed/remaining/retryAfter。
- Redis 失败只允许 DENY 或 LOCAL_FALLBACK。

**Commit:** `feat(gateway): enforce gateway rate limits`

## Task 3: Bulkhead 与 Circuit

- 非阻塞 Permit，动态缩容不取消在途请求。
- Provider runtime identity 维度 Circuit，业务错误与取消不计失败。
- OPEN 排除候选，HALF_OPEN 限制探测数。

**Commit:** `feat(gateway): isolate and circuit break providers`

## Task 4: Deadline 与 Retry Attempt

- 所有 Attempt 共用单一 Deadline Budget。
- 仅显式幂等、可重放 Body、可重试错误允许 Retry。
- 每次 Attempt 重新选择 Provider，并正确释放 Selection/Permit。

**Commit:** `feat(gateway): execute bounded retry attempts`

## Task 5: 资源保护和协议映射

- 固化请求行、Header、Body、Query、Path、Metadata/RPC Message 硬上限。
- HTTP/gRPC 统一治理错误代码和协议状态。
- 确认 EventLoop 不阻塞、状态存储有界。

**Commit:** `feat(gateway): guard gateway request resources`

## Task 6: 验收

```bash
./mvnw -B -ntp -f egon-cola-components/pom.xml \
  -pl :egon-cola-component-gateway-engine -am clean test
```

检查 Redis 限流只有一次 Lua 调用，任何错误/取消路径都释放 Permit，Retry 默认关闭。
