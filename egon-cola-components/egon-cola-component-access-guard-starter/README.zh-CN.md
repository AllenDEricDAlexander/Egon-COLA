# Egon COLA Access Guard Starter

[English](README.md)

`egon-cola-component-access-guard-starter` 是一个面向 Spring Boot 的规则化准入治理与受保护业务执行组件。

它使用同一套运行模型统一支持：

- Spring AOP 方法拦截
- 程序化 Guard 调用
- `CompletionStage` 完整生命周期治理
- Reactor `Mono` / `Flux` 完整生命周期治理
- 可选 Bytecode Agent 增强
- 本地或 Redisson 分布式状态
- 指标、结构化事件、日志与 Actuator 端点

适用场景包括抽奖、优惠券领取、登录防刷、支付提交、风险校验、昂贵查询、热点接口保护，以及需要在业务方法边界统一执行准入规则的场景。

> 本文档对应 Egon COLA `5.3.2`、Java 21+、Spring Boot 3.5.x。

---

## 1. Access Guard 解决什么问题

业务准入逻辑经常分散在 Controller、Service、Redis Lua、过滤器和异常处理器中，最终容易出现：

- 不同入口执行不同规则；
- 限流、黑名单逻辑逐渐漂移；
- 超时和 fallback 行为不一致；
- 敏感业务标识被直接写入 Redis Key 或指标标签；
- 本地模式与分布式模式语义不一致；
- 异步或响应式方法在返回对象时就被误判为“业务已完成”。

Access Guard 通过一个具名规则统一这些能力。

```text
方法 / 构造器 / 程序化请求
              |
              v
            解析规则
              |
              v
          构建 Guard Key
              |
              v
DenyList -> AllowList -> PenaltyBox -> RateLimit
              |
              v
      TimeLimit -> 执行业务方法
              |
              v
        拒绝处理 / fallback / 最终事件
```

策略顺序固定，并属于公共契约。

---

## 2. 核心能力

| 能力              | 说明                                  |
|-----------------|-------------------------------------|
| 统一规则引擎          | AOP、程序化、异步、响应式和 Agent 入口共享同一套规则语义。  |
| DenyList        | 在所有绕过逻辑之前拒绝已封禁身份。                   |
| AllowList       | 可作为准入门禁，也可只跳过指定的下游策略。               |
| PenaltyBox      | 将连续限流违规升级为临时处罚。                     |
| Token Bucket 限流 | 使用本地或 Redisson 原子状态保护热点入口。          |
| TimeLimit       | 通过调用线程、受控线程池或虚拟线程观察或强制执行时限。         |
| 拒绝处理            | 支持抛异常、fallback、JSON 反序列化或返回 `null`。 |
| 隐私安全 Key        | Key 规范化后使用 HMAC-SHA-256 哈希，再进入存储。   |
| 故障策略            | 按故障点配置 fail-closed、fail-open 或本地兜底。 |
| 异步生命周期          | 跟踪 `CompletionStage` 的完成、超时、取消和拒绝。  |
| 响应式生命周期         | 在订阅时惰性执行，并只发布一个终态结果。                |
| Agent 模式        | 支持 private、static、自调用和显式构造器等字节码路径。  |
| 可观测性            | 支持终态/阶段事件、Micrometer 指标、日志和只读端点。    |
| 严格启动校验          | 未知配置与非法规则组合直接使应用启动失败。               |

---

## 3. 环境要求

- Java 21 或更高版本
- Spring Boot 3.x
- 默认 `AOP` 引擎需要 Spring AOP
- 只要配置了规则，就必须提供非空 HMAC Secret
- 选择 `storage: REDISSON` 时必须提供 `RedissonClient`
- 保护 `Mono` / `Flux` 时需要 Reactor
- 使用 `AGENT` 时需要 `egon-cola-component-bytecode-starter` 与 Java Agent

---

## 4. 引入依赖

### 4.1 直接引入

```xml
<dependency>
    <groupId>top.egon</groupId>
    <artifactId>egon-cola-component-access-guard-starter</artifactId>
    <version>5.3.2</version>
</dependency>
```

### 4.2 使用 Egon COLA BOM

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>top.egon</groupId>
            <artifactId>egon-cola-components-bom</artifactId>
            <version>5.3.2</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>

<dependencies>
    <dependency>
        <groupId>top.egon</groupId>
        <artifactId>egon-cola-component-access-guard-starter</artifactId>
    </dependency>
</dependencies>
```

Redisson、Actuator、Micrometer、Reactor 和 Bytecode Agent 等可选能力，在启用时仍需由业务应用提供对应依赖和运行环境。

---

## 5. 五分钟快速开始

下面的示例为业务方法增加“按用户限流”。

### 5.1 配置规则

```yaml
egon:
  cola:
    component:
      access-guard:
        enabled: true
        engine: AOP
        storage: LOCAL

        key:
          contributors:
            - ARGUMENT
          hmac-secret: ${ACCESS_GUARD_HMAC_SECRET}

        rules:
          draw:
            enabled: true

            key:
              contributors:
                - ARGUMENT

            rate-limit:
              enabled: true
              algorithm: TOKEN_BUCKET
              capacity: 10
              refill-tokens: 10
              refill-period: 1s
              requested-tokens: 1

            rejection:
              mode: THROW
```

通过环境变量提供 Secret：

```bash
export ACCESS_GUARD_HMAC_SECRET='replace-with-a-long-random-secret'
```

### 5.2 标注业务方法

```java
import top.egon.cola.component.accessguard.api.AccessGuard;
import top.egon.cola.component.accessguard.api.GuardKey;

@Service
public class DrawApplicationService {

    @AccessGuard("draw")
    public DrawResult draw(@GuardKey("userId") String userId) {
        return executeDraw(userId);
    }

    private DrawResult executeDraw(String userId) {
        return new DrawResult(true, userId);
    }
}
```

注解值就是规则 ID，必须对应：

```text
egon.cola.component.access-guard.rules
```

下的某个键。

### 5.3 处理拒绝结果

当 `rejection.mode: THROW` 时，组件抛出：

```java
top.egon.cola.component.accessguard.api.AccessGuardRejectedException
```

异常中包含结构化 `GuardOutcome`：

```java
try{
    drawApplicationService.draw(userId);
}catch(
AccessGuardRejectedException exception){
GuardOutcome outcome = exception.outcome();

    log.

warn(
        "Access rejected: rule={}, decision={}, retryAfter={}",
        outcome.ruleId(),
        outcome.

decision(),
        outcome.

retryAfter()
    );
        }
```

---

## 6. 注解模型

### 6.1 `@AccessGuard`

聚合注解是默认推荐入口。

```java

@AccessGuard("payment-submit")
public PaymentResult submit(@GuardKey("customer") Long customerId) {
    return paymentService.submit(customerId);
}
```

支持目标：

- 类型
- 方法
- 显式构造器

属性：

| 属性      | 含义                        |
|---------|---------------------------|
| `value` | 必填规则 ID。                  |
| `key`   | 可选显式绑定 Key，会放入 Key 解析上下文。 |

方法注解优先于类型注解：

```java

@AccessGuard("customer-api")
@Service
public class CustomerService {

    public Customer find(Long id) {
        return repository.find(id);
    }

    @AccessGuard("customer-export")
    public byte[] export() {
        return exporter.export();
    }
}
```

### 6.2 专用注解

组件还提供：

```java
@AllowListGuard("partner-api")
@RateLimitGuard("search")
@TimeLimitGuard("report")
```

专用注解要求绑定规则只能启用一个匹配策略。

合法示例：

```yaml
rules:
  search:
    rate-limit:
      enabled: true
```

```java

@RateLimitGuard("search")
public SearchResult search(@GuardKey String keyword) {
    return searchService.search(keyword);
}
```

下面的规则不适用于 `@RateLimitGuard`，因为启用了多个策略：

```yaml
rules:
    search:
        deny-list:
            enabled: true
        rate-limit:
            enabled: true
```

多策略规则应使用：

```java
@AccessGuard("search")
```

### 6.3 一个方法只能有一个绑定

不要在同一个方法上叠加多个 Guard 注解：

```java
// 非法
@AccessGuard("search")
@RateLimitGuard("search")
public Result search(String text) {
    return doSearch(text);
}
```

启动校验会拒绝歧义绑定。

---

## 7. 完整配置结构

```yaml
egon:
  cola:
    component:
      access-guard:
        enabled: true

        # AOP、AGENT、DISABLED
        engine: AOP

        # LOCAL、REDISSON
        storage: LOCAL

        defaults:
          # THROW、FALLBACK、RETURN_JSON、RETURN_NULL
          rejection: THROW

        key:
          # ARGUMENT、CLIENT_IP、PRINCIPAL、HTTP_HEADER、
          # ATTRIBUTE:<name>、GLOBAL
          contributors:
            - ARGUMENT

          # IP 或 CIDR。
          trusted-proxies: []

          # 使用 HTTP_HEADER 时需要读取的 Header。
          headers: []

          hmac-secret: ${ACCESS_GUARD_HMAC_SECRET}
          max-part-length: 1024

        redisson:
          client-bean-name: redissonClient
          key-prefix: egon:access-guard
          application: ${spring.application.name:}

        local:
          max-entries: 100000
          cleanup-interval: 1m
          idle-ttl: 10m

        thread-pool:
          name: access-guard
          core-pool-size: 4
          max-pool-size: 16
          queue-capacity: 1024
          keep-alive: 60s

        rules:
          draw:
            enabled: true

            key:
              contributors:
                - ARGUMENT

            deny-list:
              enabled: false
              data-version: v1

            allow-list:
              enabled: false
              # GATE、BYPASS_RATE_LIMIT、
              # BYPASS_RATE_LIMIT_AND_PENALTY
              mode: GATE
              data-version: v1

            penalty-box:
              enabled: false
              threshold: 5
              violation-ttl: 1m
              penalty-ttl: 10m

            rate-limit:
              enabled: true
              algorithm: TOKEN_BUCKET
              capacity: 100
              refill-tokens: 100
              refill-period: 1s
              requested-tokens: 1

            time-limit:
              enabled: false
              # DISABLED、OBSERVE_ONLY、ENFORCE
              mode: DISABLED
              # CALLER_THREAD、THREAD_POOL、VIRTUAL_THREAD
              executor: CALLER_THREAD
              timeout: 1s
              cancel-running-task: true

            rejection:
              # 为 null 时继承 defaults.rejection。
              mode: THROW
              fallback-method: ""
              return-json: ""

            failure-policies:
              key-resolution: FAIL_CLOSED
              deny-list-store: FAIL_CLOSED
              allow-list-store: FAIL_CLOSED
              penalty-store: LOCAL_FALLBACK
              rate-limit-backend: LOCAL_FALLBACK
              execution: FAIL_CLOSED
              observability: FAIL_OPEN

            observability:
              final-events: true
              stage-events: false
              metrics: true
              logging: true
              endpoint: true
```

配置采用严格绑定：

- 未知字段导致启动失败；
- 非法枚举值导致启动失败；
- 非法 Duration 或数值导致启动失败；
- 非法注解与规则组合导致启动失败；
- 非法 fallback 或 `return-json` 导致启动失败。

这是有意设计。准入治理问题应在部署阶段暴露，而不是等到第一笔生产请求。

---

## 8. 全局配置说明

### 8.1 顶层属性

| 属性                   |     默认值 | 说明                             |
|----------------------|--------:|--------------------------------|
| `enabled`            |  `true` | 是否启用组件。                        |
| `engine`             |   `AOP` | 选择 `AOP`、`AGENT` 或 `DISABLED`。 |
| `storage`            | `LOCAL` | 选择本地或 Redisson 状态。             |
| `defaults.rejection` | `THROW` | 规则未指定时的默认拒绝模式。                 |

### 8.2 Key 属性

| 属性                    |          默认值 | 说明                           |
|-----------------------|-------------:|------------------------------|
| `key.contributors`    | `[ARGUMENT]` | 全局有序 Contributor 列表。         |
| `key.trusted-proxies` |         `[]` | 允许提供转发 IP Header 的直接代理地址。    |
| `key.headers`         |         `[]` | `HTTP_HEADER` 读取的 Header 名称。 |
| `key.hmac-secret`     |            空 | 存在规则时必填。                     |
| `key.max-part-length` |       `1024` | Key Part 名称和值的最大规范化长度。       |

规则级 Contributor 继承逻辑：

- 空列表：继承全局列表；
- 非空列表：完全替换全局列表。

### 8.3 本地状态属性

| 属性                       |      默认值 | 说明           |
|--------------------------|---------:|--------------|
| `local.max-entries`      | `100000` | 本地有界状态最大条目数。 |
| `local.cleanup-interval` |     `1m` | 清理周期。        |
| `local.idle-ttl`         |    `10m` | 空闲淘汰 TTL。    |

### 8.4 线程池属性

| 属性                           |            默认值 | 说明         |
|------------------------------|---------------:|------------|
| `thread-pool.name`           | `access-guard` | 托管执行器名称。   |
| `thread-pool.core-pool-size` |            `4` | 核心线程数。     |
| `thread-pool.max-pool-size`  |           `16` | 最大线程数。     |
| `thread-pool.queue-capacity` |         `1024` | 有界队列容量。    |
| `thread-pool.keep-alive`     |          `60s` | 非核心线程空闲时间。 |

校验规则：

- 线程数必须为正；
- core 不能大于 max；
- 队列容量必须为正且有界。

---

## 9. Guard Key 设计

Guard Key 决定 AllowList、DenyList、PenaltyBox 和 RateLimit 针对谁执行。

Key 设计往往比限流数值更重要。限流值再精确，如果身份维度选错，结果仍然错误。

### 9.1 Key 生命周期

```text
Contributors
    |
    v
有序 GuardKeyPart
    |
    v
规范化与转义
    |
    v
name=value|name=value
    |
    v
HMAC-SHA-256(secret)
    |
    v
Store 与 Policy 使用 keyHash
```

原始业务标识不会直接作为：

- Redis Key；
- 指标标签；
- Actuator 输出；
- 结构化事件字段。

### 9.2 `ARGUMENT`

可以在以下位置使用 `@GuardKey`：

- 方法参数；
- 参数对象字段；
- record component。

参数示例：

```java

@AccessGuard("draw")
public DrawResult draw(
    @GuardKey(value = "tenant", order = 0) String tenantId,
    @GuardKey(value = "user", order = 10) String userId
) {
    return drawService.draw(tenantId, userId);
}
```

Record 示例：

```java
public record DrawCommand(
        @GuardKey(value = "tenant", order = 0) String tenantId,
        @GuardKey(value = "user", order = 10) String userId,
        String activityId
) {
}

@AccessGuard("draw")
public DrawResult draw(DrawCommand command) {
    return drawService.draw(command);
}
```

普通类字段示例：

```java
public class PaymentCommand {

    @GuardKey(value = "merchant", order = 0)
    private String merchantId;

    @GuardKey(value = "customer", order = 10)
    private String customerId;

    private BigDecimal amount;
}
```

`required` 默认为 `true`：

```java

@GuardKey(value = "device", required = false)
String deviceId
```

必填值缺失时会产生 Key 解析失败。

### 9.3 `GLOBAL`

```yaml
key:
    contributors:
        - GLOBAL
```

所有调用共享同一个 Guard 身份。

适用：

- 保护单个昂贵批任务；
- 限制某个下游依赖的全局调用量；
- 限制应用级维护任务。

需要租户或用户公平性时不要使用 `GLOBAL`。

### 9.4 `CLIENT_IP`

```yaml
key:
    contributors:
        - CLIENT_IP
    trusted-proxies:
        - 10.0.0.0/8
        - 192.168.0.0/16
```

解析逻辑：

1. 读取直接远端地址；
2. 判断直接对端是否命中 `trusted-proxies`；
3. 只有命中时才读取 `Forwarded` 或 `X-Forwarded-For`；
4. 否则使用直接对端地址。

绝不能无条件信任转发 Header，否则客户端可以伪造受保护身份。

调用上下文中需要提供：

```text
accessGuard.httpRequest
```

自定义 Adapter 至少要提供兼容的：

```java
getRemoteAddr()

getHeader(String)
```

### 9.5 `HTTP_HEADER`

```yaml
key:
  contributors:
    - HTTP_HEADER
  headers:
    - X-Tenant-Id
    - X-Client-Id
```

配置的 Header 都是必填项。

只能使用由可信认证层写入、已经校验和清洗的 Header。客户端自行传入的 Header 不能天然成为身份边界。

### 9.6 `PRINCIPAL`

```yaml
key:
  contributors:
    - PRINCIPAL
```

调用上下文中需要提供：

```text
accessGuard.principal
```

如果值实现 `java.security.Principal`，使用 `getName()`；否则使用其字符串值。

### 9.7 `ATTRIBUTE:<name>`

该方式特别适合程序化 API：

```yaml
key:
    contributors:
        - ATTRIBUTE:tenantId
        - ATTRIBUTE:userId
```

```java
GuardRequest request = new GuardRequest(
    "draw",
    new Object[0],
    Map.of(
        "tenantId", tenantId,
        "userId", userId
    ),
    DrawResult.class,
    null
);
```

属性缺失会导致 Key 解析失败。

### 9.8 组合 Key

```yaml
key:
  contributors:
    - ARGUMENT
    - CLIENT_IP
```

Key Part 按 `order` 排序，再进行规范化与转义。

适合：

- tenant + user；
- merchant + customer；
- account + device；
- user + IP 登录防刷。

不要加入不能提升治理效果的高基数维度。

---

## 10. 固定策略顺序

准入顺序不能调整：

1. DenyList
2. AllowList
3. PenaltyBox
4. RateLimit
5. TimeLimit 与业务执行
6. 拒绝处理
7. 最终可观测事件

### DenyList 为什么第一

DenyList 永远优先，AllowList 命中也不能绕过 DenyList。

避免出现：

```text
已封禁用户 + 旧白名单记录 -> 被错误放行
```

### PenaltyBox 为什么在 RateLimit 前

活动处罚应立即拒绝，不应继续读取或消耗 Token Bucket 状态。

### TimeLimit 为什么在准入之后

已经被拒绝的请求不应占用执行器资源，也不应开始业务工作。

---

## 11. DenyList

```yaml
deny-list:
    enabled: true
    data-version: v1
```

命中后产生：

```text
GuardDecision.DENY_LIST_HIT
```

`data-version` 是存储命名空间的一部分。需要整体替换数据集、又不希望立即删除旧 Key 时，可以提升版本。

### Store 接口

```java
public interface DenyListStore {

    boolean contains(String ruleId, String dataVersion, String keyHash);

    void add(String ruleId, String dataVersion, String keyHash, Duration ttl);

    void remove(String ruleId, String dataVersion, String keyHash);

    void replace(
        String ruleId,
        String dataVersion,
        Set<String> keyHashes,
        Duration ttl
    );
}
```

本地和 Redisson 实现支持写入。自定义只读实现可以保留默认写方法，调用时会抛 `StoreOperationException`。

### 运维注意

Store 接收的是哈希后的 Key，而不是原始业务标识。管理工具必须复用相同规范化与 HMAC 逻辑，或者通过可信服务完成“业务身份 ->
keyHash”转换。

---

## 12. AllowList

```yaml
allow-list:
    enabled: true
    mode: GATE
    data-version: v1
```

`GATE` 模式未命中时产生：

```text
GuardDecision.ALLOW_LIST_MISS
```

### 模式

| 模式                              | 行为                                           |
|---------------------------------|----------------------------------------------|
| `GATE`                          | 未命中即拒绝；命中后继续执行 PenaltyBox 和 RateLimit。       |
| `BYPASS_RATE_LIMIT`             | 命中后跳过 RateLimit，但绝不跳过 DenyList 或 PenaltyBox。 |
| `BYPASS_RATE_LIMIT_AND_PENALTY` | 命中后跳过 PenaltyBox 与 RateLimit，但绝不跳过 DenyList。 |

示例：

```yaml
rules:
  partner-api:
    deny-list:
      enabled: true

    allow-list:
      enabled: true
      mode: BYPASS_RATE_LIMIT

    penalty-box:
      enabled: true

    rate-limit:
      enabled: true
```

行为：

```text
deny-list 命中        -> 拒绝
allow-list 未命中     -> 继续执行 penalty 和 rate limit
allow-list 命中       -> 只跳过 rate limit
存在活动 penalty      -> 拒绝
```

组件没有“绕过全部治理”的模式。

---

## 13. PenaltyBox

PenaltyBox 将连续限流违规升级为临时处罚。

```yaml
penalty-box:
    enabled: true
    threshold: 5
    violation-ttl: 1m
    penalty-ttl: 10m
```

含义：

- 在 `violation-ttl` 内累计限流违规；
- 达到 `threshold` 后激活处罚；
- 在 `penalty-ttl` 内直接拒绝。

活动处罚产生：

```text
GuardDecision.PENALTY_ACTIVE
```

### 时间线示例

```text
00:00  被限流，违规数=1
00:10  被限流，违规数=2
00:20  被限流，违规数=3
00:30  被限流，违规数=4
00:40  被限流，违规数=5 -> 激活处罚
00:45  被 PenaltyBox 直接拒绝
10:40  处罚过期
```

如果在达到阈值前，违规窗口已经过期，计数会按照 Store 实现重置。

---

## 14. RateLimit

限流算法由 Rule 选择，默认仍为 `TOKEN_BUCKET`。Starter 支持三种算法：

| 算法 | 状态语义 | 参数含义 |
|------|----------|----------|
| `TOKEN_BUCKET` | Token 累积到 `capacity`，准入调用消耗 `requested-tokens`。 | `refill-tokens` 在每个 `refill-period` 增加。 |
| `LEAKY_BUCKET` | 水位按固定速率流出，只有新水位不超过容量才准入。 | `refill-tokens/refill-period` 定义流出速率；`requested-tokens` 是单次水量。 |
| `SLIDING_WINDOW` | 保留窗口内的准入时间戳并精确计数。 | `capacity` 是最大调用数；`refill-period` 是窗口；`requested-tokens` 必须为 `1`。 |

配置：

```yaml
rate-limit:
    enabled: true
    algorithm: TOKEN_BUCKET
    capacity: 100
    refill-tokens: 100
    refill-period: 1s
    requested-tokens: 1
```

### 参数含义

| 属性                 | 含义               |
|--------------------|------------------|
| `capacity`         | 桶最大容量。           |
| `refill-tokens`    | 每个补充周期增加的 Token。 |
| `refill-period`    | 补充周期。            |
| `requested-tokens` | 单次调用消耗的 Token。   |

校验：

- 所有数值必须为正；
- `requested-tokens` 不能大于 `capacity`；
- `refill-period` 必须为正。
- `SLIDING_WINDOW` 要求 `requested-tokens=1` 且 `capacity<=100000`。

Local 存储使用单调时钟和有界内存条目。Redisson 存储使用 Redis Server 时间和单 Key 原子
脚本。已有 Token Bucket 保留旧 HASH Key；漏桶和滑动窗口惰性使用 `:leaky-bucket`、
`:sliding-window` 后缀 Key，并通过 idle TTL 清理。不需要迁移或批量删除。算法参数变化会
按 Rule 版本产生新的规范化状态。存储异常遵循 `failurePolicies.rateLimitBackend`
（`FAIL_OPEN`、`LOCAL_FALLBACK` 或 `FAIL_CLOSED`）。`retryAfter` 只是 `GuardOutcome` 中的
运行提示，Guard 不会排队或 sleep 被拒绝的调用。

Provider 方法接入 RPC 时，必须显式依赖 RPC Starter 和 Access Guard Starter，并将
`@RateLimitGuard(ruleId, key)` 放在实现方法上。RPC 只把 `GuardDecision.RATE_LIMITED` 映射为
Provider-stage gRPC `UNAVAILABLE`，业务方法不会进入；其他 Guard 决策保持原有异常语义。

### 示例

#### 每秒 10 次，突发 10

```yaml
capacity: 10
refill-tokens: 10
refill-period: 1s
requested-tokens: 1
```

#### 每分钟 60 次，突发 20

```yaml
capacity: 20
refill-tokens: 60
refill-period: 1m
requested-tokens: 1
```

#### 昂贵操作单次消耗 5

```yaml
capacity: 100
refill-tokens: 100
refill-period: 1m
requested-tokens: 5
```

被限流时产生：

```text
GuardDecision.RATE_LIMITED
```

结果中可能带有 `retryAfter`。

---

## 15. TimeLimit

配置：

```yaml
time-limit:
    enabled: true
    mode: ENFORCE
    executor: VIRTUAL_THREAD
    timeout: 800ms
    cancel-running-task: true
```

### 支持组合

| 模式             | 执行器              | 行为              |
|----------------|------------------|-----------------|
| `DISABLED`     | `CALLER_THREAD`  | 不执行时限控制。        |
| `OBSERVE_ONLY` | `CALLER_THREAD`  | 在调用线程执行，仅记录耗时。  |
| `ENFORCE`      | `THREAD_POOL`    | 通过有界线程池执行并强制超时。 |
| `ENFORCE`      | `VIRTUAL_THREAD` | 通过虚拟线程执行并强制超时。  |

非法组合会在启动时失败。

### 调用线程观察模式

```yaml
time-limit:
    enabled: true
    mode: OBSERVE_ONLY
    executor: CALLER_THREAD
    timeout: 1s
```

业务仍在原线程执行。这里的 timeout 是观察阈值，不是强制中断边界。

### 有界线程池

```yaml
time-limit:
  enabled: true
  mode: ENFORCE
  executor: THREAD_POOL
  timeout: 500ms
```

适用于需要独立限制并发度的场景。

线程池或队列满时产生：

```text
GuardDecision.EXECUTOR_REJECTED
```

### 虚拟线程

```yaml
time-limit:
    enabled: true
    mode: ENFORCE
    executor: VIRTUAL_THREAD
    timeout: 500ms
```

虚拟线程降低阻塞平台线程的成本，但不会让不可中断 I/O 自动支持取消。

### 超时取消是协作式的

`cancel-running-task: true` 会尝试取消，但以下任务仍可能继续：

- 忽略中断；
- 阻塞在不可中断的 native I/O；
- 已将工作委托给其他系统；
- 已经提交不可逆副作用。

超时保护的是调用方延迟，不是事务回滚机制。

还应配合：

- 幂等；
- 下游 deadline；
- 下游取消；
- 业务补偿。

---

## 16. 拒绝处理

支持：

```text
THROW
FALLBACK
RETURN_JSON
RETURN_NULL
```

### 16.1 `THROW`

```yaml
rejection:
    mode: THROW
```

抛出 `AccessGuardRejectedException`。

适合由全局异常处理器统一生成 API 响应。

### 16.2 `FALLBACK`

```yaml
rejection:
  mode: FALLBACK
  fallback-method: drawFallback
```

原方法：

```java

@AccessGuard("draw")
public DrawResult draw(String userId) {
    return drawService.draw(userId);
}
```

支持的 fallback 签名：

```java
private DrawResult drawFallback(String userId) {
    return DrawResult.busy(userId);
}
```

```java
private DrawResult drawFallback(
    String userId,
    GuardOutcome outcome
) {
    return DrawResult.rejected(userId, outcome.decision().name());
}
```

```java
private DrawResult drawFallback() {
    return DrawResult.busy();
}
```

规则：

- 必须且只能找到一个兼容 fallback；
- 返回类型必须兼容；
- static 原方法要求 static fallback；
- 构造器不支持 fallback；
- 非法或歧义 fallback 在启动时失败。

### 16.3 `RETURN_JSON`

```yaml
rejection:
    mode: RETURN_JSON
    return-json: >
        {"success":false,"code":"TOO_MANY_REQUESTS"}
```

使用 Spring 管理的 `ObjectMapper`，将 JSON 反序列化为原方法返回类型。

```java

@AccessGuard("draw")
public DrawResult draw(String userId) {
    return drawService.draw(userId);
}
```

JSON 必须能够反序列化为 `DrawResult`。对发现的 Guard 方法会在启动期验证。

### 16.4 `RETURN_NULL`

```yaml
rejection:
    mode: RETURN_NULL
```

限制：

- 不支持 primitive 返回类型；
- 不支持构造器。

公开 API 更推荐使用明确的结果对象，而不是 `RETURN_NULL`。

---

## 17. 故障策略

故障策略和拒绝模式解决的是不同问题。

- **策略拒绝**：规则正常执行，并决定不放行。
- **基础设施故障**：规则无法正常评估。
- **拒绝模式**：如何将终态结果返回给调用方。

支持：

```text
FAIL_CLOSED
FAIL_OPEN
LOCAL_FALLBACK
```

### 默认矩阵

| 故障点               | 默认策略             |
|-------------------|------------------|
| Key 解析            | `FAIL_CLOSED`    |
| DenyList Store    | `FAIL_CLOSED`    |
| AllowList Store   | `FAIL_CLOSED`    |
| Penalty Store     | `LOCAL_FALLBACK` |
| RateLimit Backend | `LOCAL_FALLBACK` |
| 业务执行              | `FAIL_CLOSED`    |
| 可观测性              | `FAIL_OPEN`      |

### `FAIL_CLOSED`

无法安全评估时拒绝或失败。

推荐用于：

- Key 解析；
- DenyList；
- 高风险 AllowList Gate；
- 支付提交；
- 账户安全操作。

### `FAIL_OPEN`

继续业务执行，并将结果标记为降级。

示例：

```text
type       = DEGRADED
decision   = STORE_FAILED
resolution = FAIL_OPEN
```

仅在可用性高于治理严格度时使用。

### `LOCAL_FALLBACK`

改用有界本地实现重新评估策略。

主要适用于：

- PenaltyBox；
- RateLimit。

它不是“无条件放行”。本地策略仍会返回通过或拒绝。

### 规则级配置

```yaml
failure-policies:
    key-resolution: FAIL_CLOSED
    deny-list-store: FAIL_CLOSED
    allow-list-store: FAIL_CLOSED
    penalty-store: LOCAL_FALLBACK
    rate-limit-backend: LOCAL_FALLBACK
    execution: FAIL_CLOSED
    observability: FAIL_OPEN
```

---

## 18. Outcome 模型

`GuardOutcome` 包含：

| 字段            | 说明                                          |
|---------------|---------------------------------------------|
| `type`        | `ALLOWED`、`REJECTED`、`DEGRADED` 或 `FAILED`。 |
| `decision`    | 根因决策或失败原因。                                  |
| `resolution`  | 最终如何处理。                                     |
| `ruleId`      | 规则 ID。                                      |
| `policy`      | 产生终态或降级结果的策略。                               |
| `planVersion` | 当前 Plan 版本。                                 |
| `storage`     | 当前存储模式。                                     |
| `engine`      | 当前执行引擎。                                     |
| `elapsed`     | Guard 生命周期耗时。                               |
| `retryAfter`  | 可用时给出的建议重试时间。                               |
| `failure`     | 有界故障分类与代码。                                  |

### Decision

```text
PASS
DENY_LIST_HIT
ALLOW_LIST_MISS
PENALTY_ACTIVE
RATE_LIMITED
KEY_RESOLUTION_FAILED
STORE_FAILED
CONFIG_FAILED
TIME_LIMIT_EXCEEDED
EXECUTOR_REJECTED
BUSINESS_EXCEPTION
CANCELLED
```

### Resolution

```text
NONE
THROWN
FALLBACK
RETURN_JSON
RETURN_NULL
FAIL_OPEN
LOCAL_FALLBACK
```

根因与处理方式刻意分离。

例如：

```text
decision   = STORE_FAILED
resolution = LOCAL_FALLBACK
```

表示分布式 Store 失败，但本地策略完成了处理。

---

## 19. 存储模式

### 19.1 本地模式

```yaml
storage: LOCAL
```

特点：

- 不依赖 Redis；
- 进程内有界状态；
- 适合开发和单实例服务；
- 每个实例状态互相独立；
- 无法实现集群级统一配额。

本地实现包括：

- AllowList
- DenyList
- PenaltyBox
- RateLimit

### 19.2 Redisson 模式

```yaml
storage: REDISSON

redisson:
    client-bean-name: redissonClient
    key-prefix: egon:access-guard
    application: ${spring.application.name}
```

引入 Redisson：

```xml

<dependency>
    <groupId>org.redisson</groupId>
    <artifactId>redisson</artifactId>
</dependency>
```

示例客户端：

```java

@Configuration
public class RedisConfiguration {

    @Bean
    public RedissonClient redissonClient() {
        Config config = new Config();
        config.useSingleServer()
            .setAddress("redis://127.0.0.1:6379");

        return Redisson.create(config);
    }
}
```

选择规则：

- 配置了 `client-bean-name` 时，该 Bean 必须存在且类型为 `RedissonClient`；
- 留空时，容器中必须恰好只有一个 `RedissonClient`；
- 应用命名空间来自 `redisson.application` 或 `spring.application.name`；
- Redisson 集成不完整时启动失败。

Penalty 和 Token Bucket 状态通过原子操作维护。

### 19.3 Key 命名空间

分布式 Key 会按以下维度隔离：

```text
key-prefix
application
rule
policy
data / state version
key hash
```

不同服务必须使用唯一应用名。

---

## 20. 程序化 API

注入：

```java
private final AccessGuardClient accessGuardClient;
```

### 20.1 仅做准入判断

```java
GuardRequest request = new GuardRequest(
    "draw",
    new Object[]{userId},
    Map.of(),
    DrawResult.class,
    null
);

GuardOutcome outcome = accessGuardClient.evaluate(request);

if(outcome.

type() !=GuardOutcomeType.ALLOWED
        &&outcome.

type() !=GuardOutcomeType.DEGRADED){
    throw new

IllegalStateException("Request was not admitted");
}
```

`evaluate` 只执行准入判断，不执行业务，因此不代表完整的超时与拒绝生命周期。

### 20.2 执行业务

```java
DrawResult result = accessGuardClient.execute(
    request,
    () -> drawService.draw(userId)
);
```

需要以下能力时使用 `execute`：

- TimeLimit；
- 业务异常处理；
- fallback；
- 最终生命周期事件。

### 20.3 程序化 fallback

```java
GuardRequest request = new GuardRequest(
    "draw",
    new Object[]{userId},
    Map.of(),
    DrawResult.class,
    () -> DrawResult.busy(userId)
);
```

### 20.4 Attribute Contributor

```yaml
rules:
    draw:
        key:
            contributors:
                - ATTRIBUTE:tenantId
                - ATTRIBUTE:userId
```

```java
GuardRequest request = new GuardRequest(
    "draw",
    new Object[0],
    Map.of(
        "tenantId", tenantId,
        "userId", userId
    ),
    DrawResult.class,
    null
);
```

程序化 API 适合：

- 不经过 Spring Proxy；
- 工作流引擎动态执行操作；
- Adapter 已经掌握执行生命周期；
- 需要在创建昂贵业务对象前先做准入判断。

---

## 21. `CompletionStage` 支持

返回 `CompletionStage` 的方法，不会在返回 Stage 对象时被判定为业务完成。

```java

@AccessGuard("async-report")
public CompletionStage<Report> generate(String reportId) {
    return reportService.generate(reportId);
}
```

组件会跟踪：

- 准入；
- 异步成功；
- 异步失败；
- 超时；
- 取消；
- 拒绝处理；
- 一个最终结果。

开启强制时限后，会对返回 Stage 应用配置的 timeout。

Fallback 可以返回：

- 直接值；
- 兼容的 `CompletionStage`。

取消结果记录为：

```text
GuardDecision.CANCELLED
```

不要为了让 AOP 看到同步结果而手动阻塞 Stage，组件已经管理完整异步生命周期。

---

## 22. Reactor 支持

Reactor 存在时，`Mono` 和 `Flux` 在订阅时惰性执行治理。

```java

@AccessGuard("reactive-order")
public Mono<Order> findOrder(String orderId) {
    return orderRepository.findById(orderId);
}
```

流程：

```text
方法返回 Publisher
        |
        v
订阅发生
        |
        v
解析 Plan 与准入
        |
        v
订阅业务 Publisher
        |
        v
成功 / 错误 / 超时 / 取消
        |
        v
发布一个终态 Guard Outcome
```

这样可以保留 Reactor 冷流语义。

### 支持返回类型

- `Mono`
- `Flux`

Guard 响应式方法要求恰好存在一个响应式 Adapter，否则启动失败。

### Fallback 兼容性

对于 `Mono`：

- fallback 可以返回普通值或 `Mono`；
- 返回 `Flux` 非法。

对于 `Flux`：

- fallback 可以返回普通值或 `Flux`；
- 返回 `Mono` 非法。

### 超时

```yaml
time-limit:
    enabled: true
    mode: ENFORCE
    executor: VIRTUAL_THREAD
    timeout: 1s
```

对 Reactor 方法，timeout 作用于 Publisher 生命周期，而不是返回 Publisher 对象这一瞬间。

---

## 23. 执行引擎

### 23.1 AOP

```yaml
engine: AOP
```

默认模式。

支持：

- Spring Bean 方法；
- 类型级绑定；
- 方法级绑定；
- 同步方法；
- `CompletionStage`；
- Reactor 方法。

限制：

- 不能拦截构造器；
- 不能覆盖绕过 Proxy 的自调用；
- 不能治理没有经过 Spring Proxy 的对象。

自调用陷阱：

```java

@Service
public class OrderService {

    public void outer() {
        inner(); // 绕过 Spring Proxy
    }

    @AccessGuard("inner")
    public void inner() {
    }
}
```

解决方式：

- 将 Guard 方法移到另一个 Bean；
- 通过 Proxy 调用；
- 使用程序化客户端；
- 使用 Agent 模式。

### 23.2 Disabled

```yaml
engine: DISABLED
```

基础设施可以保留，但不启用 AOP 或 Agent 执行。

适合：

- 灰度；
- 本地诊断；
- 只使用部分程序化能力。

### 23.3 Agent

```yaml
engine: AGENT
```

增加：

```xml

<dependency>
    <groupId>top.egon</groupId>
    <artifactId>egon-cola-component-bytecode-starter</artifactId>
    <version>5.3.2</version>
</dependency>
```

启动：

```bash
java \
  "-javaagent:/opt/egon/egon-cola-component-bytecode-agent-5.3.2.jar=enabled=true,features=access-guard,include=com.example.*" \
  -jar application.jar
```

Agent 必须在 JVM 启动时安装。

Agent 模式额外支持：

- private 方法；
- static 方法；
- 同类调用；
- 递归调用；
- final 方法；
- synchronized 方法，但受超时限制；
- 非 Spring 对象；
- 显式构造器。

AOP 和 Agent 互斥。

---

## 24. 构造器治理

构造器拦截必须使用 Agent 模式，并显式标注构造器。

```java
public class SecureClient {

    @AccessGuard("client-construction")
    public SecureClient(@GuardKey("tenant") String tenantId) {
        initialize(tenantId);
    }
}
```

限制：

- 只增强显式标注的构造器；
- 类型注解不会自动保护所有构造器；
- Guard 在第一个 `this(...)` 或 `super(...)` 前执行；
- 此时没有初始化完成的 receiver；
- 只支持准入策略；
- 拒绝模式必须是 `THROW`；
- 不支持 TimeLimit；
- 不支持 fallback；
- 不支持 JSON/null 返回替换。

构造器治理需要谨慎。保护 Spring 尚未准备好之前创建的基础设施对象，可能形成启动循环或 fail-closed。

除非确有必要，优先保护工厂方法或 Application Service 方法。

---

## 25. Agent 模式下的 synchronized 与 static

### Static 方法

```java

@AccessGuard("static-task")
public static Result execute(String id) {
    return doExecute(id);
}
```

使用 fallback 时，fallback 也必须是 static。

### Synchronized 方法

Agent 保留原始 monitor 边界：

```java

@AccessGuard("critical-section")
public synchronized Result update(String id) {
    return doUpdate(id);
}
```

将方法体移动到其他线程的 TimeLimit 会改变同步语义，因此不允许。

---

## 26. 动态规则源

配置文件只是一个 `GuardPlanSource`。

公共接口：

```java
public interface GuardPlanSource {

    String name();

    int priority();

    Optional<GuardPlanSnapshot> current(String ruleId);

    AutoCloseable subscribe(Consumer<GuardPlanSnapshot> listener);
}
```

Resolver 行为：

- Source 名称必须唯一；
- 优先级必须唯一；
- 高优先级优先；
- 每个候选 Snapshot 都会校验；
- 同一 Source/Rule 版本必须单调递增；
- 新配置非法时保留上一版有效 Snapshot；
- 记录有界加载失败；
- 发布 Plan Change 事件。

因此可以接入动态配置中心，而不需要替换 Guard Engine。

自定义 Source 应：

1. 产生不可变 Snapshot；
2. 版本单调递增；
3. 避免在 `toString` 暴露 Secret；
4. 完整解析后再发布；
5. 正确关闭订阅资源。

---

## 27. 扩展点

多数基础设施 Bean 都支持条件覆盖。

| 接口                      | 用途                |
|-------------------------|-------------------|
| `GuardPlanSource`       | 提供静态或动态规则。        |
| `GuardPlanResolver`     | 解析当前有效 Plan。      |
| `GuardKeyContributor`   | 增加新的 Key 维度。      |
| `KeyHasher`             | 替换哈希实现，但必须保留隐私边界。 |
| `DenyListStore`         | 读写 DenyList。      |
| `AllowListStore`        | 读写 AllowList。     |
| `PenaltyStore`          | 存储违规和处罚状态。        |
| `RateLimitBackend`      | 实现限流状态转换。         |
| `FailurePolicyResolver` | 解析基础设施故障策略。       |
| `TimeLimiter`           | 执行受时限保护的业务。       |
| `RejectionHandler`      | 处理终态拒绝。           |
| `FallbackHandler`       | 执行 fallback。      |
| `GuardEventListener`    | 消费终态或阶段事件。        |
| `GuardEventPublisher`   | 发布可观测事件。          |

### 自定义 Key Contributor

```java
@Component
public class RegionKeyContributor implements GuardKeyContributor {

    @Override
    public String id() {
        return "REGION";
    }

    @Override
    public List<GuardKeyPart> contribute(
            GuardInvocation invocation,
            KeyConfig config
    ) {
        Object value = invocation.attributes().get("region");

        if (value == null) {
            throw new GuardKeyResolutionException("REGION_MISSING");
        }

        return List.of(new GuardKeyPart("region", value.toString(), 0));
    }
}
```

配置：

```yaml
key:
    contributors:
        - REGION
```

Contributor ID 不区分大小写，且必须唯一。

---

## 28. 可观测性

### 28.1 终态与阶段事件

```yaml
observability:
    final-events: true
    stage-events: false
    metrics: true
    logging: true
    endpoint: true
```

默认只发布一个最终事件。阶段事件需要显式开启，因为会显著增加事件量。

### 28.2 指标

存在 `MeterRegistry` 时，组件会发布：

```text
egon.access.guard.calls
egon.access.guard.duration
egon.access.guard.store.failures
egon.access.guard.plan.reloads
egon.access.guard.local.entries
```

允许的有界标签：

```text
ruleId
policy
type
decision
resolution
engine
storage
```

刻意排除：

- 原始 Key；
- Key Hash 标签；
- 方法参数；
- Header；
- Principal；
- 异常消息；
- fallback 值；
- HMAC Secret。

### 28.3 日志

默认 Listener 输出：

```text
ruleId
planVersion
policy
type
decision
resolution
engine
storage
elapsed
retryAfter
failureCategory
failureCode
```

普通结果为 debug，带有界 Failure 的结果为 warn。

### 28.4 Actuator 端点

引入：

```xml

<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

暴露：

```yaml
management:
  endpoints:
    web:
      exposure:
        include:
          - health
          - accessguard
```

请求：

```http
GET /actuator/accessguard
```

端点返回：

- 存储模式；
- Plan 健康状态；
- Plan 失败数量；
- 本地 PenaltyBox 条目数；
- 本地 RateLimit 条目数；
- 不含敏感信息的规则摘要。

不会输出原始身份、Key、Secret、Header 或参数。

---

## 29. 安全与隐私建议

### HMAC Secret

要求：

- 存在规则时不能为空；
- 使用足够长的随机值；
- 从 Secret Manager 或环境变量注入；
- 不要提交到 Git；
- 轮换必须有明确迁移方案。

修改 Secret 会改变所有派生 Key Hash。已有 AllowList、DenyList、Penalty 和 RateLimit 状态将无法命中。

因此 Secret 轮换是数据迁移，不是普通配置修改。

### Trusted Proxy

只配置真实反向代理与负载均衡器。

不推荐：

```yaml
trusted-proxies:
    - 0.0.0.0/0
```

更合理：

```yaml
trusted-proxies:
    - 10.10.0.0/16
    - 192.168.50.10
```

### 指标基数

不要把 userId、IP、requestId 或 keyHash 加入自定义指标标签。

### 故障策略选择

安全敏感场景通常应保持 fail-closed：

- DenyList；
- Principal 解析；
- 支付提交；
- 账户恢复；
- 凭证校验。

只读且强调可用性的场景，可以对选定基础设施故障使用 fail-open，但必须监控降级结果。

---

## 30. 推荐规则模式

### 30.1 按用户限流

```yaml
rules:
  user-query:
    key:
      contributors:
        - ARGUMENT

    rate-limit:
      enabled: true
      capacity: 20
      refill-tokens: 20
      refill-period: 1s
      requested-tokens: 1
```

```java

@AccessGuard("user-query")
public Result query(@GuardKey("user") String userId) {
    return service.query(userId);
}
```

### 30.2 下游全局保护

```yaml
rules:
    downstream-call:
        key:
            contributors:
                - GLOBAL

        rate-limit:
            enabled: true
            capacity: 100
            refill-tokens: 100
            refill-period: 1s

        time-limit:
            enabled: true
            mode: ENFORCE
            executor: VIRTUAL_THREAD
            timeout: 800ms
```

### 30.3 合作方白名单门禁

```yaml
rules:
  partner-api:
    key:
      contributors:
        - HTTP_HEADER

    allow-list:
      enabled: true
      mode: GATE
      data-version: v2

key:
  headers:
    - X-Partner-Id
```

仅当 `X-Partner-Id` 由可信认证层写入时使用。

### 30.4 登录防刷

```yaml
rules:
  login:
    key:
      contributors:
        - ARGUMENT
        - CLIENT_IP

    deny-list:
      enabled: true

    penalty-box:
      enabled: true
      threshold: 5
      violation-ttl: 2m
      penalty-ttl: 15m

    rate-limit:
      enabled: true
      capacity: 5
      refill-tokens: 5
      refill-period: 1m

    rejection:
      mode: THROW
```

### 30.5 查询降级

```yaml
rules:
  recommendation:
    key:
      contributors:
        - ARGUMENT

    rate-limit:
      enabled: true
      capacity: 10
      refill-tokens: 10
      refill-period: 1s

    time-limit:
      enabled: true
      mode: ENFORCE
      executor: VIRTUAL_THREAD
      timeout: 300ms

    rejection:
      mode: FALLBACK
      fallback-method: recommendationFallback
```

---

## 31. 常见配置错误

### 缺少 HMAC Secret

报错：

```text
Access Guard key HMAC secret must not be blank when rules are configured
```

修复：

```yaml
key:
  hmac-secret: ${ACCESS_GUARD_HMAC_SECRET}
```

### 规则不存在

报错：

```text
Unknown Access Guard rule: draw
```

检查注解值和 YAML Rule ID。

### 专用注解绑定多个策略

报错：

```text
A dedicated guard annotation must bind a single matching policy
```

改用 `@AccessGuard`，或者简化规则。

### AOP 模式标注构造器

报错：

```text
AOP mode does not support guarded constructor
```

改用 Agent，或保护工厂方法。

### RedissonClient 不存在

报错：

```text
Configured RedissonClient bean 'redissonClient' was not found
```

提供 Bean，或修改 `client-bean-name`。

### Redisson 应用名为空

报错：

```text
REDISSON storage requires spring.application.name
or access-guard.redisson.application
```

设置其中一个。

### Reactor Adapter 缺失

报错：

```text
Reactive Access Guard method requires exactly one Reactor adapter
```

确保 Reactor 可用，并且没有重复 Adapter。

### Fallback 非法

可能原因：

- 方法不存在；
- 参数不兼容；
- 重载歧义；
- 返回类型不兼容；
- static 原方法使用了实例 fallback。

### Primitive 使用 `RETURN_NULL`

报错：

```text
primitive return types do not support RETURN_NULL
```

改用 `THROW`、`FALLBACK` 或 `RETURN_JSON`。

### 超时后下游仍执行

这可能是正常现象，因为下游任务不响应中断。需要额外增加下游 deadline 与幂等控制。

---

## 32. 排障清单

规则似乎没有执行时：

1. 确认 `enabled: true`。
2. 确认 Engine 不是 `DISABLED`。
3. 确认注解 Rule ID 存在。
4. AOP 模式下确认目标是 Spring Bean。
5. 检查是否为自调用。
6. 确认方法只有一个 Guard 注解。
7. 确认 HMAC Secret 已提供。
8. 确认必填 Key Part 非空。
9. HTTP Contributor 场景确认 Request/Principal 上下文已提供。
10. 查看 `/actuator/accessguard`。
11. 打开 Access Guard 包 debug 日志。
12. 分开查看 `decision` 与 `resolution`。

本地与生产行为不一致时：

1. 检查 `storage`。
2. 检查 `spring.application.name`。
3. 检查 Redisson Bean 选择。
4. 检查 Trusted Proxy CIDR。
5. 检查 HMAC Secret 是否一致。
6. 检查 `data-version`。
7. 检查是否多实例却使用 LOCAL。
8. 检查时钟与 Redis 延迟。
9. 检查生产 JVM 是否真的加载了 Agent。

---

## 33. 测试

### 33.1 模块测试

仓库根目录执行：

```bash
./mvnw -B -ntp \
  -pl egon-cola-components/egon-cola-component-access-guard-starter \
  -am test
```

### 33.2 服务级测试

```java
@SpringBootTest(properties = {
    "egon.cola.component.access-guard.key.hmac-secret=test-secret",
    "egon.cola.component.access-guard.rules.draw.rate-limit.enabled=true",
    "egon.cola.component.access-guard.rules.draw.rate-limit.capacity=1",
    "egon.cola.component.access-guard.rules.draw.rate-limit.refill-tokens=1",
    "egon.cola.component.access-guard.rules.draw.rate-limit.refill-period=1h"
})
class DrawGuardTest {

    @Autowired
    private DrawApplicationService service;

    @Test
    void shouldRejectSecondCall() {
        service.draw("user-1");

        assertThatThrownBy(() -> service.draw("user-1"))
            .isInstanceOf(AccessGuardRejectedException.class);
    }
}
```

### 33.3 Redisson 集成测试

至少验证：

- 两个应用实例共享同一配额；
- 并发下原子状态正确；
- Redis 故障符合 Failure Policy；
- 应用命名空间不冲突；
- TTL 行为符合生产预期。

### 33.4 Agent 验证

Fork JVM：

```bash
-Xverify:all
-javaagent:/path/to/egon-cola-component-bytecode-agent-5.3.2.jar=enabled=true,features=access-guard,include=com.example.*
```

验证：

- private 方法；
- 同类调用；
- static 方法与 static fallback；
- synchronized 限制；
- 显式构造器；
- Runtime 未就绪行为；
- 代理场景不会重复治理。

---

## 34. 生产上线检查表

- [ ] 已明确定义受保护身份。
- [ ] HMAC Secret 已安全存储。
- [ ] 已理解 Secret 轮换影响。
- [ ] Trusted Proxy 范围准确。
- [ ] LOCAL/REDISSON 选择符合部署拓扑。
- [ ] Token Bucket 数值来源于真实流量。
- [ ] Penalty TTL 合理。
- [ ] 风险与可用性负责人已审核 Failure Policy。
- [ ] Fallback 无副作用。
- [ ] Timeout 不会掩盖重复副作用。
- [ ] 已配置拒绝率与 Store Failure 告警。
- [ ] Actuator 端点受保护。
- [ ] 已进行多实例压测。
- [ ] 已演练 Redis 降级。
- [ ] Agent 模式已验证真实启动命令。

---

## 35. 从 Access Guard V1 迁移

`5.3.2` 是源码不兼容的 V2 模型，不再提供 V1 兼容门面。

| V1 概念           | V2 替代                                                 |
|-----------------|-------------------------------------------------------|
| 旧通用 Guard 注解    | `top.egon.cola.component.accessguard.api.AccessGuard` |
| `DoWhiteList`   | `AllowListGuard` 或 `AccessGuard`                      |
| `DoRateLimiter` | `RateLimitGuard` 或 `AccessGuard`                      |
| `DoHystrix`     | `TimeLimitGuard` 或 `AccessGuard`                      |
| 注解内限流/超时参数      | 具名 YAML Rule                                          |
| 多套运行路径          | 统一 `GuardEngine`                                      |
| 原始/组合 Store Key | 有序 Contributor + HMAC-SHA-256                         |
| 白名单绕过全部         | 有边界的 `AllowListMode`                                  |
| 临时 fallback     | 启动期校验 fallback 或 JSON                                 |

注解和配置必须一起迁移，不支持 V1/V2 混合运行。

---

## 36. 设计边界

组件提供准入治理与受保护执行，但它不是：

- API Gateway 替代品；
- 分布式事务协调器；
- 具有滚动错误率的完整 Circuit Breaker；
- 身份认证系统；
- 授权策略引擎；
- WAF；
- “超时后业务一定停止”的保证；
- AllowList/DenyList 管理控制台。

它可以与这些系统组合，但职责边界必须明确。

---

## 37. 验证边界

模块测试覆盖：

- 固定策略顺序；
- 故障策略矩阵；
- 配置绑定与校验；
- AOP/程序化一致性；
- fallback 校验；
- `CompletionStage` 生命周期；
- Reactor 生命周期；
- 本地有界状态；
- 受门控的 Redisson 脚本集成；
- 测试级 Java Agent 进程。

Maven 测试通过并不能自动证明：

- 真实多 JVM Redis 行为；
- 生产反向代理拓扑；
- 网络分区行为；
- 时钟偏差；
- 不可中断 I/O 一定被取消；
- 生产 Agent 打包与启动脚本正确。

这些仍需要在目标环境验证。

---

## 38. 最小示例索引

### 仅限流

```yaml
rules:
  search:
    rate-limit:
      enabled: true
```

```java

@RateLimitGuard("search")
public Result search(@GuardKey String keyword) {
    return service.search(keyword);
}
```

### 多策略

```java

@AccessGuard("login")
public LoginResult login(LoginCommand command) {
    return loginService.login(command);
}
```

### 程序化执行

```java
Result result = accessGuardClient.execute(
    new GuardRequest("task", args, attributes, Result.class, fallback),
    operation
);
```

### 全局限流

```yaml
key:
  contributors:
    - GLOBAL
```

### Redisson

```yaml
storage: REDISSON
redisson:
    application: order-service
```

### Agent

```yaml
engine: AGENT
```

```bash
-javaagent:egon-cola-component-bytecode-agent-5.3.2.jar=enabled=true,features=access-guard,include=com.example.*
```

---

## 39. License

许可证信息请查看仓库根目录中的 MIT / LGPL-2.1 文件。
