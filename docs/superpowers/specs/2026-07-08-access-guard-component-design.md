# Access Guard Component Design

## 1. Context

Egon-COLA is adding an application-internal access governance component under `egon-cola-components`. The updated v2 requirement expands the earlier rate-limiter-only design into a unified Spring Boot Starter for:

1. method-level white list access control;
2. Redisson-based distributed rate limiting;
3. Redis-backed dynamic blacklisting;
4. method-level timeout protection;
5. fallback method and static `returnJson` reject responses;
6. dynamic switches and method-level rule overrides;
7. structured events, logs, and metrics.

This means the component should no longer be modeled as only `rate-limiter`. In this repository it should be an Egon-COLA native access guard component, while retaining the old rate limiter annotation semantics for migration.

## 2. Confirmed Decisions

1. Use Scheme 1: an Egon-COLA native starter component.
2. Rename the design target from rate limiter to access guard because v2 covers white list, rate limiting, blacklist, timeout protection, and reject responses.
3. Use Egon-COLA naming:
   - component root: `egon-cola-component-access-guard`
   - starter artifact: `egon-cola-component-access-guard-starter`
   - test artifact: `egon-cola-component-access-guard-test`
   - package root: `top.egon.cola.component.accessguard`
   - configuration prefix: `egon.cola.component.access-guard`
4. Provide a new standard annotation `@AccessGuard` for combined governance.
5. Provide dedicated standard annotations:
   - `@WhiteListAccessInterceptor`
   - `@RateLimiterAccessInterceptor`
   - `@TimeoutCircuitBreaker`
6. Retain compatibility annotations and semantics:
   - `@DoWhiteList`
   - `@DoRateLimiter`
   - `@DoHystrix`
7. Use Redisson as the default Redis access path. Do not use Jedis or `StringRedisTemplate` as the core state path.
8. Use Redisson `RRateLimiter` as the default distributed limiter, not a hand-rolled Lua script in V1.0.
9. Keep Hystrix as an optional adapter concept. The core starter must not hard-bind to Hystrix.
10. The BOM exports only the starter module, not the component root or test module.
11. No browser, app startup, or long-lived runtime process is needed for implementation validation.

## 3. Goals

This work delivers:

1. A new `egon-cola-component-access-guard` component under `egon-cola-components`.
2. A Spring Boot starter that auto-registers the access guard AOP flow.
3. Unified method interception for white list, blacklist, rate limiting, timeout protection, fallback, and `returnJson`.
4. Static and dynamic white list support through annotation values, properties, Redisson state, and extension providers.
5. Redisson global rate limiting shared across application instances.
6. Reject-count based blacklist triggering with Redis TTL auto-release.
7. Method-level timeout protection using a replaceable timeout executor.
8. `fallbackMethod` and `returnJson` reject responses for white list rejection, blacklist hit, rate limit rejection, timeout, Redisson failure with `FAIL_CLOSED`, and thread-pool rejection.
9. Dynamic global, capability-level, and method-level switches through a config-provider abstraction.
10. Key resolution from method parameters, object fields, nested fields, SpEL, request headers, IP, and custom resolvers.
11. Backward compatibility for original white list, Guava-style rate limiter, and Hystrix-style timeout annotations.
12. Spring proxy compatibility for annotation resolution, fallback lookup, and return type resolution.
13. SPI extension points for key resolution, white list source, rate limiting, blacklist storage, timeout execution, reject response invocation, config providers, key generation, events, and errors.
14. Structured events, logs, and optional Micrometer metrics.
15. README and test-module samples covering direct annotations and compatibility annotations.

## 4. Non-Goals

V1.0 does not include:

1. A complete management UI.
2. A standalone admin service.
3. Gateway-level rate limiting.
4. Cross-region strongly consistent limiting.
5. A full circuit breaker state machine with open, half-open, and closed states.
6. Exception-rate or slow-call-rate automatic circuit breaking.
7. A risk scoring platform, device fingerprinting, or anti-fraud model.
8. A permanent blacklist database.
9. Database tables or Flyway migrations.
10. A rule orchestration platform.
11. Mandatory Hystrix dependency.
12. Compatibility packages under old `com.atluofu`, `cn.atluofu`, or `xfg-wrench` package roots.
13. Direct compile-time dependency on the dynamic config center component. DCC is supported through `AccessGuardConfigProvider` so the starter remains independently usable.

## 5. Target Module Structure

The component is added as:

```text
egon-cola-components/
`-- egon-cola-component-access-guard/
    |-- pom.xml
    |-- README.md
    |-- egon-cola-component-access-guard-starter/
    |   |-- pom.xml
    |   `-- src/
    `-- egon-cola-component-access-guard-test/
        |-- pom.xml
        `-- src/
```

The components parent POM aggregates the new component root. The component root aggregates `starter` and `test`. The components BOM exports only:

```text
top.egon:egon-cola-component-access-guard-starter
```

The BOM must not export:

1. `egon-cola-component-access-guard`
2. `egon-cola-component-access-guard-test`
3. optional compatibility adapter modules
4. test-only dependencies

## 6. Dependency Boundary

The starter may depend on:

1. `egon-cola-component-common-core`
2. `egon-cola-component-common-trace`
3. `spring-boot-starter`
4. `spring-boot-autoconfigure`
5. `spring-boot-starter-aop`
6. `spring-boot-configuration-processor` as optional
7. `spring-web` for request header and IP key resolution
8. `redisson-spring-boot-starter` for Redis and Redisson objects
9. Jackson through the Spring Boot dependency set for `returnJson`
10. `micrometer-core` as optional
11. `slf4j-api`

The starter must not depend on:

1. admin modules
2. test modules
3. JPA or database libraries
4. Flyway
5. MQ
6. Nacos
7. Dubbo
8. UI build tooling
9. dynamic thread pool
10. dynamic config center
11. Hystrix in the core starter

The test module may depend on the starter and Spring Boot test libraries. Redisson behavior should be tested with focused fixtures and should not require starting the business application.

## 7. Starter Package Layout

Package root:

```text
top.egon.cola.component.accessguard
```

Target layout:

```text
top.egon.cola.component.accessguard
|-- annotation
|   |-- AccessGuard
|   |-- WhiteListAccessInterceptor
|   |-- RateLimiterAccessInterceptor
|   |-- TimeoutCircuitBreaker
|   |-- DoWhiteList
|   |-- DoRateLimiter
|   `-- DoHystrix
|-- aop
|   `-- AccessGuardAop
|-- autoconfigure
|   |-- AccessGuardAutoConfiguration
|   `-- AccessGuardProperties
|-- blacklist
|   |-- BlacklistService
|   |-- BlacklistStatus
|   `-- RedissonBlacklistService
|-- circuitbreaker
|   |-- TimeoutCircuitBreakerExecutor
|   |-- ThreadPoolTimeoutCircuitBreakerExecutor
|   |-- VirtualThreadTimeoutCircuitBreakerExecutor
|   |-- TimeoutTaskExecutorProvider
|   `-- TimeoutCircuitBreakerException
|-- config
|   |-- AccessGuardConfigProvider
|   |-- DefaultAccessGuardConfigProvider
|   |-- AccessGuardRule
|   `-- AccessGuardRuleOverride
|-- context
|   |-- AccessGuardContext
|   |-- AccessGuardDecision
|   `-- AccessGuardResult
|-- event
|   |-- AccessGuardEvent
|   |-- AccessGuardEventListener
|   |-- AccessGuardEventPublisher
|   `-- LoggingAccessGuardEventListener
|-- exception
|   |-- AccessGuardException
|   |-- AccessGuardKeyResolveException
|   `-- AccessGuardRejectResponseException
|-- key
|   |-- AccessKeyResolver
|   |-- AccessGuardKeyGenerator
|   |-- CompositeAccessKeyResolver
|   `-- DefaultAccessKeyResolver
|-- ratelimiter
|   |-- RateLimiterExecutor
|   |-- RedissonRateLimiterExecutor
|   `-- LocalRateLimiterExecutor
|-- reject
|   |-- RejectResponseInvoker
|   |-- ReflectionFallbackInvoker
|   `-- JsonRejectResponseParser
|-- support
|   |-- AopMethodResolver
|   |-- AccessGuardRedisKeys
|   |-- MethodSignatureKey
|   `-- SensitiveValueHasher
`-- whitelist
    |-- WhiteListRepository
    |-- WhiteListService
    |-- DefaultWhiteListService
    `-- RedissonWhiteListRepository
```

This layout follows the v2 requirement while using the repository package style and avoiding an admin module in V1.0.

## 8. Core Concepts

### 8.1 Access Key

An access key identifies the caller or traffic dimension used by white list, blacklist, and rate limiting.

Supported dimensions:

1. `all`
2. method parameter names, such as `userId`
3. object fields and nested fields, such as `request.userId`
4. `keyExpression` SpEL, such as `#request.userId`
5. request headers, such as `#header['X-App-Id']`
6. IP, such as `#ip`
7. combined keys, such as `#tenantId + ':' + #userId`
8. custom `AccessKeyResolver` beans

The implementation should hash or mask sensitive values before using them in Redis keys or logs.

### 8.2 Rule Name

Every governed method needs a stable rule name. Resolution priority:

```text
annotation name > config-provider method-level name > class + method + parameter signature
```

Business code should set `name` explicitly to avoid key and metric changes after refactoring Java class names.

### 8.3 White List

White list controls who may access a new or restricted method.

Supported modes:

```text
GATEKEEPER
BYPASS_GUARD
```

Default mode is `GATEKEEPER`, where a white list hit continues through blacklist, rate limiting, timeout protection, and business execution. `BYPASS_GUARD` directly proceeds to the business method and must be used carefully.

### 8.4 Blacklist

Blacklist blocks abnormal high-frequency access keys. It is triggered by rate-limit rejections, not total requests.

`blacklistCount = 0` disables automatic blacklisting. `key = all` is protected by default because blacklisting an `all` key can block the whole method.

### 8.5 Rate Limiting

Rate limiting controls how frequently an access key may enter the business method.

The default implementation uses Redisson `RRateLimiter`. Compatibility `permitsPerSecond` values are converted to Redisson `permits + interval` rules.

### 8.6 Timeout Protection

Timeout protection wraps business method execution after access control and rate limiting pass.

If execution exceeds `timeoutValue`, the component returns fallback or `returnJson`. A timeout return does not guarantee the underlying Java task has stopped immediately; the executor attempts cancellation and logs the outcome.

### 8.7 Reject Response

Reject responses are produced by:

1. `fallbackMethod`
2. `returnJson`
3. global default reject handler

Priority:

```text
fallbackMethod > returnJson > global default reject handler
```

## 9. Annotations

### 9.1 Unified Annotation

`@AccessGuard` is the recommended new annotation when a method uses more than one governance capability.

Fields:

```text
name
key
keyExpression
whitelist
rateLimiter
blacklist
timeoutBreaker
fallbackMethod
returnJson
```

The annotation is not required for all users. Dedicated annotations and compatibility annotations remain supported.

### 9.2 Rate Limiter Annotation

`@RateLimiterAccessInterceptor` supports old and new semantics.

Fields:

```text
name
key
keyExpression
permitsPerSecond
permits
interval
intervalUnit
blacklistCount
blacklistTimeout
blacklistTimeUnit
fallbackMethod
returnJson
failStrategy
enableBlacklistForAllKey
```

Conversion rules:

```text
permitsPerSecond=1.0 -> permits=1, interval=1, intervalUnit=SECONDS
permitsPerSecond=5.0 -> permits=5, interval=1, intervalUnit=SECONDS
permitsPerSecond=0.5 -> permits=1, interval=2, intervalUnit=SECONDS
```

If `permits/interval` and `permitsPerSecond` are both configured, `permits/interval` wins.

### 9.3 White List Annotation

`@WhiteListAccessInterceptor` fields:

```text
name
key
keyExpression
users
returnJson
fallbackMethod
mode
failStrategy
enabled
```

Annotation `users` is suitable for small test lists. Production lists should come from properties, Redisson, or a config provider.

### 9.4 Timeout Annotation

`@TimeoutCircuitBreaker` fields:

```text
name
timeoutValue
timeoutUnit
fallbackMethod
returnJson
executor
threadPoolName
corePoolSize
maxPoolSize
queueCapacity
fallbackOnException
cancelRunningTask
enabled
```

Default behavior:

1. `timeoutValue <= 0` disables timeout protection.
2. `fallbackOnException=false` preserves business exception semantics.
3. `cancelRunningTask=true` attempts to cancel timed-out tasks.

### 9.5 Compatibility Annotations

`@DoWhiteList` maps to `WhiteListAccessInterceptor.key` and `returnJson`.

`@DoRateLimiter` maps to `RateLimiterAccessInterceptor.permitsPerSecond` and `returnJson`. Missing key means `key = all`, with method identity used for rule isolation. Production mode upgrades from local Guava-style limiting to Redisson method-level global limiting.

`@DoHystrix` maps to `TimeoutCircuitBreaker.timeoutValue` and `returnJson`, with milliseconds as the default unit.

The compatibility layer should live in the Egon-COLA package root. It should not expose old package roots as public APIs.

## 10. Runtime Flow

The access guard AOP flow is:

```text
request enters Spring bean method
-> AccessGuardAop intercepts supported annotations
-> resolve trace and method metadata
-> read global and capability switches
-> if globally disabled, proceed directly
-> merge annotation, application properties, and dynamic overrides
-> resolve access key
-> evaluate white list
-> evaluate blacklist
-> evaluate Redisson rate limiter
-> wrap business method with timeout protection when enabled
-> return business result, fallback result, returnJson result, or propagated business exception
-> publish event, log, and metrics
```

Execution order is fixed:

```text
white list -> blacklist -> rate limiter -> timeout protection -> business method
```

Business exceptions from the protected method are propagated by default. They are sent to fallback only when `fallbackOnException=true`.

## 11. Dynamic Configuration

Configuration priority from high to low:

```text
config-provider method-level override
> config-provider global override
> Redisson dynamic config snapshot
> application properties
> annotation
> starter defaults
```

The starter exposes:

```java
public interface AccessGuardConfigProvider {

    Optional<AccessGuardRuleOverride> findMethodOverride(String ruleName, String methodSignature);

    Optional<AccessGuardRuleOverride> findGlobalOverride();
}
```

The default provider reads application properties and returns no external overrides. Business systems or a later DDC adapter can provide a Spring bean that reads DCC, Nacos, database, Redisson config snapshots, or another dynamic source.

Invalid dynamic overrides must be rejected with an event and log entry. They must not poison the active rule.

## 12. Key Resolution

Resolution support:

1. default `all`
2. simple parameter name
3. object field
4. nested object field
5. SpEL expression
6. request header
7. IP address
8. combined key expression
9. custom `AccessKeyResolver`

Failure strategies:

```text
USE_ALL
REJECT
IGNORE
```

Default is `USE_ALL` for compatibility with the v2 requirement. Production critical methods should configure `REJECT` to avoid accidentally converting user-level governance into method-level global governance.

## 13. White List

White list sources from high to low:

```text
method-level dynamic white list
> method-level Redisson white list
> method-level properties white list
> annotation users
> global dynamic white list
> global Redisson white list
> global properties white list
```

When white list is enabled but empty:

```text
DENY_ALL
ALLOW_ALL
DISABLE_WHITELIST
```

Default is `DENY_ALL`, because an enabled empty white list usually means the rollout list is missing.

White list storage:

1. properties list for local configuration
2. Redisson `RSet`
3. optional Redisson `RSetCache` for TTL entries
4. custom `WhiteListRepository`

## 14. Redisson Rate Limiting

Redis access is centralized through `RedissonClient`.

Redisson object choices:

1. `RRateLimiter` for distributed rate limiting
2. `RSet` or `RSetCache` for white lists
3. `RBucket` or `RMapCache` for blacklists
4. `RAtomicLong` or `RMapCache` for reject counts
5. `RBucket` for rule versions or config snapshots
6. `RLock` only for rare idempotent limiter initialization or rebuilds

Limiter initialization requirements:

1. first use initializes the limiter automatically
2. initialization is idempotent under concurrent instances
3. local rule version cache avoids repeated reconfiguration on every request
4. config changes refresh Redisson limiter state
5. Redisson exceptions use the effective `failStrategy`

Failure strategies:

```text
FAIL_OPEN
FAIL_CLOSED
LOCAL_FALLBACK
GLOBAL_DEFAULT
```

Default is `FAIL_OPEN`. Security-sensitive methods may use `FAIL_CLOSED`.

## 15. Redis Key Contract

Default key prefix:

```text
egon:access-guard
```

Logical format:

```text
{prefix}:{app}:{env}:{ruleName}:{accessKey}:{type}
```

Types:

```text
whitelist
limiter
blacklist
reject-count
timeout-config
config-version
rule-config
```

Requirements:

1. include app name
2. include environment
3. include stable rule name
4. include normalized access key when the state is identity-scoped
5. hash or mask sensitive values
6. summarize long keys
7. set TTL for blacklist and reject-count keys
8. use cleanup strategy for limiter keys

## 16. Blacklist

Blacklist is triggered only by rate-limit rejections.

Flow:

```text
rate limit rejected
-> reject count + 1
-> if count >= blacklistCount, write blacklist with TTL
-> return fallback or returnJson
```

Blacklist record fields:

```text
ruleName
accessKeyHash
blacklistTime
expireAt
reason
rejectCount
```

When `key = all`, automatic blacklist is disabled unless `enableBlacklistForAllKey=true`.

## 17. Timeout Protection

Timeout protection wraps the business method only after white list, blacklist, and rate limiting pass.

Supported executor modes:

```text
THREAD_POOL
VIRTUAL_THREAD
HYSTRIX_ADAPTER
CUSTOM
GLOBAL_DEFAULT
```

V1.0 default is `THREAD_POOL`. `VIRTUAL_THREAD` may be provided because the repository targets Java 21. `HYSTRIX_ADAPTER` should stay optional and out of the core starter dependency graph.

Thread-pool configuration:

```text
threadPoolName
corePoolSize
maxPoolSize
queueCapacity
keepAliveTime
rejectedPolicy
```

Thread-pool rejection is treated as a circuit-breaker rejection and returns fallback or `returnJson`.

Timeout cancellation:

1. attempt cancellation when `cancelRunningTask=true`
2. log when cancellation cannot stop the task
3. release context and MDC after task completion
4. never promise hard thread termination

## 18. Reject Response Invocation

`RejectResponseInvoker` owns both fallback and `returnJson`.

Fallback lookup order:

1. same method name and original parameter types
2. same method name and original parameters plus `AccessGuardContext`
3. same method name with no parameters
4. `returnJson`
5. global default reject handler

Return compatibility:

1. `String`
2. common result wrappers
3. `ResponseEntity<T>`
4. `CompletableFuture<T>` as a reserved adapter path
5. `Mono<T>` as a reserved adapter path

`returnJson` parsing should use the Spring `ObjectMapper` when available. It should not rely on `returnType.newInstance()`.

Fallback lookup must be proxy-aware and cache method resolution results when method caching is enabled.

## 19. Auto-Configuration

`AccessGuardAutoConfiguration` is registered in:

```text
META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
```

Auto-configured beans:

1. `AccessGuardProperties`
2. `AccessGuardAop`
3. `AccessGuardConfigProvider`
4. `AccessKeyResolver`
5. `WhiteListService`
6. `WhiteListRepository`
7. `BlacklistService`
8. `RateLimiterExecutor`
9. `TimeoutCircuitBreakerExecutor`
10. `TimeoutTaskExecutorProvider`
11. `RejectResponseInvoker`
12. `AccessGuardEventPublisher`
13. `AccessGuardEventListener`

Auto-configuration rules:

1. enabled by `egon.cola.component.access-guard.enabled=true`, default true
2. AOP aspect is not registered when disabled
3. default beans use `@ConditionalOnMissingBean`
4. Redisson beans reuse an application-provided `RedissonClient` by default
5. if no `RedissonClient` exists, auto-creation occurs only when explicitly enabled
6. metrics bind only when Micrometer and `MeterRegistry` are present
7. no business package scanning is required

Spring Boot 3 uses `AutoConfiguration.imports`. Spring Boot 2 `spring.factories` compatibility can be considered only if the repository decides to support Boot 2, but it is not required for the current Java 21 / Boot 3 line.

## 20. Configuration Properties

Configuration prefix:

```yaml
egon:
  cola:
    component:
      access-guard:
        enabled: true
        storage: redisson
        key-prefix: egon:access-guard
        app-name: ${spring.application.name:default-app}
        env: ${spring.profiles.active:default}
        fail-strategy: fail-open
        key-resolve-failure-strategy: use-all
        aop:
          order: -100
        redisson:
          enabled: true
          client-bean-name: redissonClient
          auto-create-client: false
        whitelist:
          enabled: true
          source: properties,redisson,config-provider
          default-users: []
          empty-list-strategy: deny-all
          mode: gatekeeper
        rate-limiter:
          enabled: true
          default-permits: 1
          default-interval: 1
          default-interval-unit: seconds
        blacklist:
          enabled: true
          default-count: 0
          default-timeout: 24h
          reject-count-window: 1m
          enable-all-key-blacklist: false
        circuit-breaker:
          enabled: true
          default-timeout: 350ms
          executor: thread-pool
          fallback-on-exception: false
          cancel-running-task: true
          thread-pool:
            core-pool-size: 10
            max-pool-size: 50
            queue-capacity: 200
            keep-alive-time: 60s
            rejected-policy: fallback
        dynamic:
          enabled: true
          global-switch-key: accessGuardSwitch
          rate-limiter-switch-key: rateLimiterSwitch
          whitelist-switch-key: whiteListSwitch
          circuit-breaker-switch-key: circuitBreakerSwitch
        local-fallback:
          enabled: false
          expire-after-write: 1m
        rules:
          draw-api:
            whitelist:
              enabled: true
              key: userId
              users: []
              mode: gatekeeper
              return-json: '{"code":"403","info":"not in white list"}'
            rate-limiter:
              enabled: true
              key: userId
              permits: 1
              interval: 1
              interval-unit: seconds
              blacklist-count: 3
              blacklist-timeout: 24h
              fallback-method: drawRateLimitFallback
            circuit-breaker:
              enabled: true
              timeout-value: 350
              timeout-unit: milliseconds
              fallback-method: drawTimeoutFallback
```

Legacy configuration compatibility:

```text
bugstack.whitelist.users -> egon.cola.component.access-guard.whitelist.default-users
rateLimiterSwitch -> egon.cola.component.access-guard.dynamic.rate-limiter-switch-key
```

## 21. Events, Logs, and Metrics

Event types:

```text
PASS
WHITELIST_HIT
WHITELIST_REJECTED
RATE_LIMITED
BLACKLIST_ADDED
BLACKLIST_HIT
CIRCUIT_BREAKER_PASS
CIRCUIT_BREAKER_TIMEOUT
CIRCUIT_BREAKER_REJECTED
CIRCUIT_BREAKER_ERROR
FALLBACK_INVOKED
RETURN_JSON_INVOKED
CONFIG_REFRESHED
REDISSON_ERROR
KEY_RESOLVE_FAILED
```

Structured log fields:

```text
traceId
appName
env
ruleName
method
accessKeyHash
guardType
action
whiteListMode
permits
interval
blacklistCount
timeoutValue
executor
costMs
failStrategy
message
```

Metrics:

```text
access_guard_request_total
access_guard_pass_total
access_guard_whitelist_hit_total
access_guard_whitelist_reject_total
access_guard_rate_limiter_reject_total
access_guard_blacklist_hit_total
access_guard_blacklist_added_total
access_guard_circuit_breaker_timeout_total
access_guard_circuit_breaker_reject_total
access_guard_fallback_total
access_guard_return_json_total
access_guard_redisson_error_total
access_guard_cost_seconds
access_guard_business_cost_seconds
```

Metrics are optional and must not force Actuator into business applications.

## 22. Design Pattern Consideration

Use patterns only where they reduce real complexity:

1. Strategy for `AccessKeyResolver`, `WhiteListRepository`, `RateLimiterExecutor`, `BlacklistService`, `TimeoutCircuitBreakerExecutor`, `RejectResponseInvoker`, and `AccessGuardConfigProvider`.
2. Facade for `AccessGuardAop`, which orchestrates the fixed governance flow without exposing every internal service to business code.
3. Adapter for compatibility annotations and optional Hystrix integration.
4. Observer for `AccessGuardEventListener`.

Do not add a broad factory hierarchy. Spring auto-configuration and `@ConditionalOnMissingBean` are enough for implementation selection. Do not split the AOP flow into multiple aspects unless a later requirement proves the single orchestrator is too large.

## 23. Testing Strategy

Starter module tests:

1. `AccessGuardAutoConfigurationTest`
   - default beans are created when enabled
   - AOP bean is absent when disabled
   - custom SPI beans make defaults back off
2. `AccessGuardAnnotationResolverTest`
   - parses `@AccessGuard`
   - parses white list, rate limiter, and timeout annotations
   - maps `@DoWhiteList`, `@DoRateLimiter`, and `@DoHystrix`
3. `DefaultAccessKeyResolverTest`
   - resolves `all`
   - resolves parameter names
   - resolves object and nested fields
   - resolves SpEL, header, and IP
   - applies failure strategy
4. `DefaultWhiteListServiceTest`
   - combines annotation, properties, Redisson repository, and config-provider sources
   - applies empty-list strategy
   - applies `GATEKEEPER` and `BYPASS_GUARD`
5. `RedissonRateLimiterExecutorTest`
   - converts `permitsPerSecond`
   - initializes limiter idempotently
   - handles config refresh
   - applies `FAIL_OPEN`, `FAIL_CLOSED`, and `LOCAL_FALLBACK`
6. `RedissonBlacklistServiceTest`
   - increments reject count
   - adds blacklist when threshold is reached
   - respects TTL
   - protects `all` by default
7. `TimeoutCircuitBreakerExecutorTest`
   - returns business result before timeout
   - returns fallback after timeout
   - handles executor rejection
   - preserves business exceptions by default
8. `RejectResponseInvokerTest`
   - invokes fallback with same args
   - invokes fallback with `AccessGuardContext`
   - invokes no-arg fallback
   - parses `returnJson`
   - handles proxy target methods
9. `AccessGuardAopFlowTest`
   - proves fixed order: white list, blacklist, rate limiter, timeout
   - proves disabled switch avoids Redisson and timeout wrapping
   - proves event publication for each reject path

Test module samples:

1. white list direct annotation sample
2. `@DoWhiteList` compatibility sample
3. rate limiter direct annotation sample
4. `@DoRateLimiter` compatibility sample
5. timeout direct annotation sample
6. `@DoHystrix` compatibility sample
7. combined white list + rate limiter + timeout sample

Validation commands should start targeted:

```bash
./mvnw -B -ntp -pl egon-cola-components/egon-cola-component-access-guard/egon-cola-component-access-guard-starter -am test
./mvnw -B -ntp -pl egon-cola-components/egon-cola-component-access-guard/egon-cola-component-access-guard-test -am test
```

Full component validation:

```bash
./mvnw -B -ntp -f egon-cola-components/pom.xml test
```

## 24. Acceptance Mapping

The design covers v2 acceptance as follows:

1. Starter access: module, BOM export, and auto-configuration.
2. White list annotation: `@WhiteListAccessInterceptor`.
3. Old white list compatibility: `@DoWhiteList`.
4. White list control: source composition and empty-list strategy.
5. Dynamic white list changes: config provider and Redisson repository.
6. Rate limiter annotation: `@RateLimiterAccessInterceptor`.
7. Redisson limiting: `RRateLimiter` executor.
8. Fallback return: `RejectResponseInvoker`.
9. `returnJson`: JSON parser backed by Spring `ObjectMapper`.
10. Blacklist: Redisson-backed TTL blacklist.
11. Blacklist expiry: Redis TTL.
12. Timeout annotation: `@TimeoutCircuitBreaker`.
13. Old rate limiter compatibility: `@DoRateLimiter`.
14. Old Hystrix compatibility: `@DoHystrix`.
15. Normal timeout path: timeout executor returns business result within threshold.
16. Timeout fallback path: timeout returns fallback or `returnJson`.
17. Executor rejection protection: rejection returns fallback or `returnJson`.
18. Dynamic switch: global, capability, and method-level config provider switches.
19. Dynamic override: method-level rule overrides.
20. Proxy compatibility: AOP method resolver and reject response invoker.
21. Redisson failure handling: `FAIL_OPEN`, `FAIL_CLOSED`, and `LOCAL_FALLBACK`.
22. Structured logs: event-driven log listener.
23. Key boundary: app, env, ruleName, accessKey, type, TTL where required.
24. Metrics: optional Micrometer metrics.

## 25. Implementation Constraints

1. Make the smallest safe changes under `egon-cola-components`.
2. Do not modify existing Flyway migration files.
3. Do not start the project automatically.
4. Do not open a browser.
5. Add tests before production code for new behavior.
6. Keep commits scoped to task boundaries.
7. Do not broaden existing components or refactor unrelated modules.
8. Keep POM style, auto-configuration style, and test style consistent with sibling components.
9. Use path-scoped staging before commits.
10. Validate with targeted Maven commands before claiming implementation completion.
