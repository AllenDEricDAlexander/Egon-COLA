# Egon COLA Access Guard Starter

[English](README.md)

Access Guard 是一个基于规则执行准入治理与受保护业务调用的单体 Spring Boot Starter。5.3.2 版本由 Spring AOP、程序化客户端和可选 Bytecode Agent 共用一套公共运行模型；不再拆分 Access Guard Core、API 或测试制品。

## 依赖

```xml
<dependency>
    <groupId>top.egon</groupId>
    <artifactId>egon-cola-component-access-guard-starter</artifactId>
    <version>5.3.2</version>
</dependency>
```

## 基本用法

先配置规则，再把规则 ID 绑定到方法。Key 在进入存储前会统一规范化并执行 HMAC-SHA-256，不会把原始参数直接用于 Redis Key、指标标签或端点输出。

```java
import top.egon.cola.component.accessguard.api.AccessGuard;
import top.egon.cola.component.accessguard.api.GuardKey;

@AccessGuard("draw")
public DrawResult draw(@GuardKey("user") String userId) {
    return drawService.execute(userId);
}
```

`@AccessGuard` 是通用入口。当一条规则只启用一个对应策略时，也可以使用以下专用方法注解：

- `@AllowListGuard("partner-api")`
- `@RateLimitGuard("search")`
- `@TimeLimitGuard("report")`

`@AccessGuard` 还可以标注类型或显式构造器；方法注解优先于类型注解。构造器拦截必须使用 Agent 模式，并受后文的更严格限制。

## 程序化 API

`AccessGuardClient` 复用相同的 `GuardEngine` 和规则语义：

```java
GuardRequest request = new GuardRequest(
        "draw",
        new Object[]{userId},
        Map.of(),
        DrawResult.class,
        null);

DrawResult result = accessGuardClient.execute(
        request,
        () -> drawService.execute(userId));
```

`evaluate(request)` 只做准入判断；如果需要把超时、业务异常、拒绝处理和最终事件纳入同一次执行，应使用 `execute(request, operation)`。

## 完整配置结构

Spring 配置采用严格绑定：未知字段或非法字段会使启动失败。`rules` 是以规则 ID 为键的 Map。

```yaml
egon:
  cola:
    component:
      access-guard:
        enabled: true
        engine: AOP                 # AOP、AGENT、DISABLED
        storage: LOCAL              # LOCAL、REDISSON
        defaults:
          rejection: THROW          # THROW、FALLBACK、RETURN_JSON、RETURN_NULL
        key:
          contributors: [ARGUMENT]  # ARGUMENT、CLIENT_IP、PRINCIPAL、HTTP_HEADER、ATTRIBUTE、GLOBAL
          trusted-proxies: []       # IP/CIDR；仅信任这些代理传递的转发头
          headers: []               # 使用 HTTP_HEADER 时必填
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
              contributors: [ARGUMENT]
            deny-list:
              enabled: true
              data-version: v1
            allow-list:
              enabled: false
              mode: GATE
              data-version: v1
            penalty-box:
              enabled: true
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
              enabled: true
              mode: ENFORCE
              executor: VIRTUAL_THREAD
              timeout: 1s
              cancel-running-task: true
            rejection:
              mode: FALLBACK
              fallback-method: drawFallback
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

规则级 contributor 列表为空时继承全局列表；非空时直接替换全局列表。

## 固定策略顺序

准入顺序固定且不可配置：

1. DenyList
2. AllowList
3. PenaltyBox
4. RateLimit
5. TimeLimit、业务调用和终态拒绝处理

DenyList 始终优先。AllowList 模式被刻意限制在以下范围：

| 模式 | 语义 |
| --- | --- |
| `GATE` | 未命中即拒绝；命中后仍执行 PenaltyBox 和 RateLimit。 |
| `BYPASS_RATE_LIMIT` | 命中后可跳过 RateLimit，但绝不跳过 DenyList 或 PenaltyBox。 |
| `BYPASS_RATE_LIMIT_AND_PENALTY` | 命中后可跳过 PenaltyBox 和 RateLimit，但绝不跳过 DenyList。 |

PenaltyBox 记录违规次数和处罚状态；RateLimit 使用原子 Token Bucket 后端。固定顺序属于公共契约，不是可由用户任意重排的 Handler 列表。

## 故障策略矩阵

`GuardDecision` 表示根因，`GuardResolution` 表示处理结果。因此，被允许继续的存储故障仍会明确记录为 `STORE_FAILED/FAIL_OPEN` 或 `STORE_FAILED/LOCAL_FALLBACK`。

| 故障点 | 默认策略 | 行为 |
| --- | --- | --- |
| Key 解析 | `FAIL_CLOSED` | Key 缺失、不安全或非法时拒绝。 |
| DenyList 存储 | `FAIL_CLOSED` | 默认拒绝；显式配置 `FAIL_OPEN` 或本地兜底后才可继续。 |
| AllowList 存储 | `FAIL_CLOSED` | 默认拒绝；可显式配置 `FAIL_OPEN`/`LOCAL_FALLBACK`。 |
| Penalty 存储 | `LOCAL_FALLBACK` | 使用有界本地策略重试；兜底不存在或失败时拒绝。 |
| 限流后端 | `LOCAL_FALLBACK` | 使用有界本地 Token Bucket 重试；兜底不存在或失败时拒绝。 |
| 业务执行 | `FAIL_CLOSED` | 超时、执行器拒绝或业务异常进入终态拒绝处理。 |
| 可观测性 | `FAIL_OPEN` | Listener 失败不改变业务结果。 |

`LOCAL_FALLBACK` 不等于隐式放行：对应本地策略仍必须给出明确的策略结果。

## 存储

默认使用 `LOCAL`。Penalty 和 RateLimit 状态受 `local.max-entries` 限制，并按清理周期与空闲 TTL 回收。本地状态只在当前进程内有效，不能提供集群级配额。

配置 `storage: REDISSON` 后，DenyList、AllowList、PenaltyBox 和 RateLimit 使用 Redisson 原子存储。`client-bean-name` 必须精确解析到一个 `RedissonClient`；若留空，容器中必须恰好只有一个客户端。`redisson.application` 默认回退到 `spring.application.name`，两者都为空时启动失败。Key 按 `key-prefix`、应用、规则、策略和状态/数据版本建立命名空间。

选择 REDISSON 却缺少对应集成或客户端会直接启动失败。Lua 脚本保证处罚与 Token Bucket 状态转换的原子性，但本机 Maven 通过不能证明生产 Redis 拓扑或跨节点行为。

## Key 与隐私边界

内置 contributor 包括 `ARGUMENT`、`CLIENT_IP`、`PRINCIPAL`、`HTTP_HEADER`、`ATTRIBUTE` 和 `GLOBAL`。可以在参数、record component 或字段上使用 `@GuardKey` 选择参数内容；必需部分缺失时 Key 解析失败。

只有直接对端命中 `trusted-proxies` 时才读取 `Forwarded` 或 `X-Forwarded-For`，否则使用直接远端地址。只配置业务确实信任的 Header。存在规则时 `hmac-secret` 必填，其对象输出会脱敏，建议从密钥管理系统或环境变量注入。指标与 Actuator 端点不会输出原始 Key、参数、Header、Principal 或 HMAC 材料。

## 超时与拒绝处理

TimeLimit 模式与执行器组合会在启动期校验：

| 模式 | 执行器 | 契约 |
| --- | --- | --- |
| `DISABLED` | `CALLER_THREAD` | 不执行超时控制。 |
| `OBSERVE_ONLY` | `CALLER_THREAD` | 原线程记录耗时，不搬移业务任务。 |
| `ENFORCE` | `THREAD_POOL` 或 `VIRTUAL_THREAD` | 通过托管执行器运行，并强制正数超时。 |

线程池参数必须为正，`core-pool-size <= max-pool-size`，队列容量必须有界。超时取消是协作式的：不可中断的阻塞 I/O 可能在调用方收到超时后继续执行。

拒绝模式包括 `THROW`、`FALLBACK`、`RETURN_JSON` 和 `RETURN_NULL`。fallback 方法与 JSON 返回类型会在启动期验证。构造器只允许 `THROW`。

## CompletionStage 与 Reactor

`CompletionStage` 方法把准入、异步完成、超时、取消、拒绝处理和最终事件放在同一生命周期内；不会把“返回了一个 Stage”误判为业务已经完成。

Reactor 在 classpath 中时，`Mono` 和 `Flux` 在订阅时惰性执行治理，并针对成功、错误、超时、拒绝或取消只发出一个终态事件。Reactor 不存在时 Starter 没有强依赖。受保护的响应式方法若不能精确找到一个 Reactor 适配器，会在启动期失败。

## AOP 与 Bytecode Agent

`engine: AOP` 为默认模式，通过 Spring Proxy 支持方法/类型绑定；它不能拦截构造器，也不能覆盖绕过代理的自调用。

`engine: AGENT` 要求 `egon-cola-component-bytecode-starter` 恰好提供一个 Access Guard 集成，并以该模块的 Java Agent 参数启动测试或生产 JVM。Agent 只识别四个 V2 API 注解，复用同一个 `GuardEngine`，并覆盖 private、static、synchronized、recursive 和显式构造器字节码路径。

运行时未就绪前，构造器规则固定 fail-closed。只有显式标注的构造器会被增强；类型注解不会隐式保护所有构造器。构造器只支持准入策略与 `THROW`，不支持 TimeLimit、fallback、JSON 或 null 返回。

## 可观测性

Starter 默认只发布一个最终事件，阶段事件需显式开启。指标包括：

- `egon.access.guard.calls`
- `egon.access.guard.duration`
- `egon.access.guard.store.failures`
- `egon.access.guard.plan.reloads`
- `egon.access.guard.local.entries`

指标标签严格限定为 `ruleId`、`policy`、`type`、`decision`、`resolution`、`engine` 和 `storage`。Key、参数、Header、Principal、异常消息与 fallback 值都不能成为标签。

使用 Spring Boot Actuator 时按需暴露只读端点：

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,accessguard
```

`GET /actuator/accessguard` 返回存储类型、规则计划健康度、有界本地条目数和非敏感规则摘要，不提供规则写操作，也不暴露原始 Key。

## 从 V1 迁移

5.3.2 是有意进行的源码不兼容替换，不打包任何 V1 兼容门面。

| V1 概念 | V2 替代方式 |
| --- | --- |
| 旧通用治理注解 | `top.egon.cola.component.accessguard.api.AccessGuard` |
| 白名单、限流、超时/熔断三种旧 `Do*` 注解 | `AllowListGuard`、`RateLimitGuard`、`TimeLimitGuard` 或通用 `AccessGuard`；三者均无兼容制品。 |
| 注解内嵌的限流、超时与 fallback 参数 | `egon.cola.component.access-guard.rules.<rule-id>` |
| 独立 AOP、执行、构造器与 Agent 路径 | 由 `engine` 选择、所有入口共用的单一 `GuardEngine`。 |
| 原始/拼接访问 Key | 有序 contributor 与 HMAC-SHA-256 存储 Key。 |
| 旧白名单全链路绕过 | 三种受限 `AllowListMode` 之一；DenyList 永远不可绕过。 |

升级前必须同时迁移配置与注解，不存在 V1/V2 混合运行模式。

## 验证边界

模块测试覆盖固定策略链、故障矩阵、配置绑定与消费、AOP/程序化入口一致性、CompletionStage/Reactor 行为、受门控的 Redisson 脚本集成，以及 fork 出的测试级 `-javaagent` 进程。Maven 通过本身不能证明真实多 JVM Redis 部署、生产反向代理信任拓扑、时钟/网络行为，也不能证明不可中断 I/O 一定被取消；这些能力需要在目标环境继续验证。
