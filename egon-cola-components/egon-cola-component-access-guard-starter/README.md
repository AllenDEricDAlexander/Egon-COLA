# Egon COLA Access Guard Starter

[中文](README.zh-CN.md)

Access Guard is a single Spring Boot Starter for rule-based admission control and guarded execution. Version 5.3.2 has one public runtime model shared by Spring AOP, the programmatic client, and the optional Bytecode Agent. It does not ship a separate Access Guard core, API, or test artifact.

## Dependency

```xml
<dependency>
    <groupId>top.egon</groupId>
    <artifactId>egon-cola-component-access-guard-starter</artifactId>
    <version>5.3.2</version>
</dependency>
```

## Basic usage

Configure a rule and bind the rule id to a method. Key parts are normalized and HMAC-SHA-256 hashed before reaching a store; raw arguments are not used as Redis keys, metrics tags, or endpoint values.

```java
import top.egon.cola.component.accessguard.api.AccessGuard;
import top.egon.cola.component.accessguard.api.GuardKey;

@AccessGuard("draw")
public DrawResult draw(@GuardKey("user") String userId) {
    return drawService.execute(userId);
}
```

`@AccessGuard` is the general entry. The dedicated method annotations are available when a rule enables exactly one matching policy:

- `@AllowListGuard("partner-api")`
- `@RateLimitGuard("search")`
- `@TimeLimitGuard("report")`

`@AccessGuard` may also annotate a type or an explicit constructor. Method annotations override a type annotation. Constructor interception requires Agent mode and has stricter limits described below.

## Programmatic API

The same `GuardEngine` and rule semantics are exposed through `AccessGuardClient`:

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

Use `evaluate(request)` only for admission evaluation. Use `execute(request, operation)` when time limits, business failures, rejection handling, and the final event must be part of the same execution.

## Complete configuration shape

Spring binding is strict: unknown and invalid fields fail startup. Rules are a map keyed by rule id.

```yaml
egon:
  cola:
    component:
      access-guard:
        enabled: true
        engine: AOP                 # AOP, AGENT, DISABLED
        storage: LOCAL              # LOCAL, REDISSON
        defaults:
          rejection: THROW          # THROW, FALLBACK, RETURN_JSON, RETURN_NULL
        key:
          contributors: [ARGUMENT]  # ARGUMENT, CLIENT_IP, PRINCIPAL, HTTP_HEADER, ATTRIBUTE, GLOBAL
          trusted-proxies: []       # IP/CIDR; forwarded headers are trusted only through these proxies
          headers: []               # required when HTTP_HEADER is selected
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

When a rule-level contributor list is empty, the global list is used. A non-empty rule list replaces the global list.

## Fixed policy order

The admission order is fixed and cannot be configured:

1. DenyList
2. AllowList
3. PenaltyBox
4. RateLimit
5. TimeLimit, business invocation, and terminal rejection handling

DenyList always wins. Allow-list modes are intentionally narrow:

| Mode | Meaning |
| --- | --- |
| `GATE` | A miss rejects; a hit continues through PenaltyBox and RateLimit. |
| `BYPASS_RATE_LIMIT` | A hit may skip RateLimit, but never DenyList or PenaltyBox. |
| `BYPASS_RATE_LIMIT_AND_PENALTY` | A hit may skip PenaltyBox and RateLimit, but never DenyList. |

PenaltyBox tracks violations and active penalties. RateLimit uses an atomic token-bucket backend. The chain order is part of the public contract, not a list of user-pluggable handlers.

## Failure policy matrix

`GuardDecision` records the root cause; `GuardResolution` records how it was handled. A store failure that is allowed to continue therefore remains observable as `STORE_FAILED/FAIL_OPEN` or `STORE_FAILED/LOCAL_FALLBACK`.

| Failure point | Default | Supported behavior |
| --- | --- | --- |
| Key resolution | `FAIL_CLOSED` | Reject when the key is missing, unsafe, or invalid. |
| DenyList store | `FAIL_CLOSED` | Default rejects; `FAIL_OPEN` or a configured local fallback may continue. |
| AllowList store | `FAIL_CLOSED` | Default rejects; optional `FAIL_OPEN`/`LOCAL_FALLBACK` is explicit. |
| Penalty store | `LOCAL_FALLBACK` | Retry with the bounded local policy; reject if fallback is unavailable or fails. |
| Rate-limit backend | `LOCAL_FALLBACK` | Retry with the bounded local token bucket; reject if fallback is unavailable or fails. |
| Execution | `FAIL_CLOSED` | Timeout, executor rejection, or business failure enters terminal rejection handling. |
| Observability | `FAIL_OPEN` | Listener failure does not change the business result. |

`LOCAL_FALLBACK` is not an implicit open decision: the corresponding local policy must return a policy result.

## Storage

`LOCAL` is the default. Penalty and rate-limit state is bounded by `local.max-entries`, cleaned periodically, and evicted by idle TTL. Local state is process-local and cannot enforce a cluster-wide quota.

Set `storage: REDISSON` to use atomic Redisson stores for DenyList, AllowList, PenaltyBox, and RateLimit. The configured `client-bean-name` must resolve to exactly one `RedissonClient`; if it is blank, exactly one client must exist. `redisson.application` falls back to `spring.application.name` and may not remain blank. Keys are namespaced by `key-prefix`, application, rule, policy, and state/data version.

Selecting REDISSON without the matching integration or client fails startup. Store scripts preserve atomic penalty and token-bucket transitions, but a local Maven run is not evidence of production Redis topology or cross-node behavior.

## Key and privacy boundary

Available contributors are `ARGUMENT`, `CLIENT_IP`, `PRINCIPAL`, `HTTP_HEADER`, `ATTRIBUTE`, and `GLOBAL`. Use `@GuardKey` on parameters, record components, or fields to select argument material. Required parts fail key resolution when absent.

`Forwarded` and `X-Forwarded-For` are accepted only when the direct peer matches `trusted-proxies`; otherwise the direct remote address is used. Configure only headers you intend to trust. `hmac-secret` is mandatory when rules exist, is redacted from object rendering, and should be supplied through a secret manager or environment variable. Raw keys, arguments, headers, principals, and HMAC material are excluded from metrics and the Actuator endpoint.

## Time limits and rejection

Time-limit modes and executors have validated combinations:

| Mode | Executor | Contract |
| --- | --- | --- |
| `DISABLED` | `CALLER_THREAD` | No timeout enforcement. |
| `OBSERVE_ONLY` | `CALLER_THREAD` | Measure elapsed time without moving work to another thread. |
| `ENFORCE` | `THREAD_POOL` or `VIRTUAL_THREAD` | Execute through a managed executor and enforce a positive timeout. |

Thread-pool bounds must be positive, `core-pool-size <= max-pool-size`, and queue capacity must be bounded. Timeout cancellation is cooperative: blocking or uninterruptible I/O can continue after the caller receives a timeout.

Rejection modes are `THROW`, `FALLBACK`, `RETURN_JSON`, and `RETURN_NULL`. Fallback methods and JSON return types are validated at startup. Constructors allow only admission checks and `THROW`.

## CompletionStage and Reactor

`CompletionStage` methods keep admission, asynchronous completion, timeout, cancellation, rejection resolution, and the final event in one lifecycle. The returned stage is not treated as a completed business result.

When Reactor is on the classpath, `Mono` and `Flux` are guarded lazily at subscription time and emit one terminal final event for success, error, timeout, rejection, or cancellation. When Reactor is absent, the Starter has no hard Reactor dependency. A guarded reactive method fails startup unless exactly one Reactor adapter is available.

## AOP and Bytecode Agent

`engine: AOP` is the default and supports method/type bindings through Spring proxies. It cannot intercept constructors or self-invocation that bypasses the proxy.

`engine: AGENT` requires exactly one Access Guard integration from `egon-cola-component-bytecode-starter` and the test/production JVM to be launched with that module's Java Agent. The Agent recognizes only the four V2 API annotations, uses the same `GuardEngine`, and supports private, static, synchronized, recursive, and explicit-constructor bytecode paths.

Constructor rules are deliberately fail-closed before the runtime is ready. Only explicitly annotated constructors are transformed; a type annotation does not implicitly guard every constructor. Constructors support admission policies and `THROW` rejection only—no time limit, fallback, JSON, or null return mode.

## Observability

The Starter publishes one final event by default. Stage events are opt-in. Metrics are:

- `egon.access.guard.calls`
- `egon.access.guard.duration`
- `egon.access.guard.store.failures`
- `egon.access.guard.plan.reloads`
- `egon.access.guard.local.entries`

Allowed metric tags are exactly `ruleId`, `policy`, `type`, `decision`, `resolution`, `engine`, and `storage`. No key, argument, header, principal, exception message, or fallback value is a tag.

With Spring Boot Actuator, expose the read-only endpoint as needed:

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,accessguard
```

`GET /actuator/accessguard` returns storage, plan health, bounded local entry counts, and non-sensitive rule summaries. It does not mutate rules or expose raw keys.

## Migration from V1

Version 5.3.2 is an intentional source-breaking replacement; no V1 compatibility facade is packaged.

| V1 concept | V2 replacement |
| --- | --- |
| Legacy general guard annotation | `top.egon.cola.component.accessguard.api.AccessGuard` |
| Three legacy `Do*` annotations for allow-list, rate-limit, and timeout/circuit-breaker | `AllowListGuard`, `RateLimitGuard`, `TimeLimitGuard`, or the general `AccessGuard`; no compatibility artifact exists. |
| Annotation-embedded rate/timeout/fallback settings | `egon.cola.component.access-guard.rules.<rule-id>` |
| Separate AOP, execution, constructor, and Agent paths | One `GuardEngine` selected through `engine` and shared by every entry. |
| Raw/composite access keys | Ordered contributors plus HMAC-SHA-256 store keys. |
| Legacy whitelist bypass-all behavior | One of the three bounded `AllowListMode` values; DenyList is never bypassed. |

Migrate configuration and annotations together before upgrading. There is no mixed V1/V2 runtime mode.

## Verification boundary

The module suite covers the fixed chain, failure matrix, configuration binding/consumption, AOP/programmatic parity, CompletionStage/Reactor behavior, Redisson scripts with gated integration tests, and a forked test-scoped `-javaagent` process. Passing Maven tests does not by itself prove a real multi-JVM Redis deployment, production reverse-proxy trust topology, clock/network behavior, or cancellation of uninterruptible I/O. Validate those properties in the target environment.
