# Rate Limiter Component Design

## 1. Context

Egon-COLA is adding a distributed global rate limiting component under `egon-cola-components`. The supplied requirement document describes a Spring Boot Starter based on method annotations, Spring AOP, Redis global state, Redis Lua scripts, dynamic blacklisting, fallback method invocation, runtime rule overrides, and observability.

The original requirement document uses `atluofu-starter-rate-limiter` as a suggested module name. This repository should expose the component with Egon-COLA naming and component conventions, matching the existing starter-type components such as dynamic thread pool, dynamic config center, and rule engine.

The first version focuses on application-internal method-level rate limiting. It is not a gateway limiter, a management platform, or a permanent blacklist system.

## 2. Confirmed Decisions

1. Use Scheme 1: an Egon-COLA native starter component.
2. Add a new component root under `egon-cola-components`.
3. Use Egon-COLA naming:
   - component root: `egon-cola-component-rate-limiter`
   - starter artifact: `egon-cola-component-rate-limiter-starter`
   - test artifact: `egon-cola-component-rate-limiter-test`
   - package root: `top.egon.cola.component.ratelimiter`
   - configuration prefix: `egon.cola.component.rate-limiter`
4. Keep the public annotation name from the requirement document:
   - `@RateLimiterAccessInterceptor`
5. Default production mode is Redis global rate limiting.
6. Local in-memory limiting is supported only as an explicit mode or Redis failure fallback.
7. The starter uses Spring Boot 3.x auto-configuration through `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`.
8. The BOM exports only the starter module, not the test module or component root POM.
9. No browser, running application, or long-lived runtime process is needed for implementation validation.

## 3. Goals

This work delivers:

1. A new `egon-cola-component-rate-limiter` component under `egon-cola-components`.
2. A Spring Boot starter for method-level rate limiting.
3. `@RateLimiterAccessInterceptor` for business methods.
4. AOP interception for annotated Controller and Service methods.
5. Redis Token Bucket rate limiting with Lua-based atomic state updates.
6. Redis TTL blacklist state shared by all application instances.
7. Reject-count based blacklist triggering.
8. Fallback method invocation for rate-limited and blacklisted requests.
9. A proxy-aware fallback method resolver with method caching.
10. Key resolution from method parameters, nested object fields, `all`, and basic web request sources when a request context is available.
11. Rule resolution from annotations, application properties, and overridable Spring beans.
12. Redis failure strategies:
    - `FAIL_OPEN`
    - `FAIL_CLOSED`
    - `LOCAL_FALLBACK`
13. SPI extension interfaces for key resolution, rule resolution, algorithms, storage, blacklist management, fallback invocation, and metrics.
14. Structured logs for allow, limited, blacklist, fallback, key resolution failure, and Redis failure events.
15. Optional Micrometer metrics when a `MeterRegistry` is present.
16. Component README with dependency, configuration, annotation, fallback, and validation examples.
17. A test/sample module proving starter auto-configuration and typical annotation use cases.

## 4. Non-Goals

V1.0 does not include:

1. A complete management UI.
2. A standalone admin service.
3. Gateway-level rate limiting.
4. Cross-region strong consistency.
5. Permanent blacklist persistence.
6. Database tables or Flyway migrations.
7. Complex rule orchestration or rule expression platforms.
8. RBAC, tenant permission, or grayscale governance.
9. Reactive `Mono` or `Flux` fallback specialization.
10. Compatibility packages under `com.atluofu`, `cn.atluofu`, or `xfg-wrench`.
11. Automatic integration with the dynamic config center component.
12. Runtime browser-based or app-start based validation.

## 5. Target Module Structure

The component is added as:

```text
egon-cola-components/
`-- egon-cola-component-rate-limiter/
    |-- pom.xml
    |-- README.md
    |-- egon-cola-component-rate-limiter-starter/
    |   |-- pom.xml
    |   `-- src/
    `-- egon-cola-component-rate-limiter-test/
        |-- pom.xml
        `-- src/
```

The components parent POM aggregates the new component root. The component root aggregates `starter` and `test`. The components BOM exports only:

```text
top.egon:egon-cola-component-rate-limiter-starter
```

The BOM must not export:

1. `egon-cola-component-rate-limiter`
2. `egon-cola-component-rate-limiter-test`
3. test-only dependencies
4. generated sample artifacts

## 6. Dependency Boundary

The starter may depend on:

1. `egon-cola-component-common-core`
2. `egon-cola-component-common-trace`
3. `spring-boot-starter`
4. `spring-boot-autoconfigure`
5. `spring-boot-starter-aop`
6. `spring-boot-configuration-processor` as optional
7. `spring-web` for request header and IP key resolution
8. `redisson-spring-boot-starter` for Redis global state
9. `micrometer-core` as optional
10. `slf4j-api`

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

The test module may depend on the starter and Spring Boot test libraries. Redis behavior should be tested with a focused embedded Redis or test fixture only where the repository already supports that test dependency shape.

## 7. Starter Package Layout

Package root:

```text
top.egon.cola.component.ratelimiter
```

Target layout:

```text
top.egon.cola.component.ratelimiter
|-- annotation
|   `-- RateLimiterAccessInterceptor
|-- aop
|   `-- RateLimiterAop
|-- algorithm
|   |-- RateLimitAlgorithm
|   |-- LocalTokenBucketRateLimitAlgorithm
|   `-- RedisTokenBucketRateLimitAlgorithm
|-- blacklist
|   |-- BlacklistManager
|   |-- BlacklistStatus
|   `-- RedisBlacklistManager
|-- config
|   |-- RateLimiterAutoConfiguration
|   `-- RateLimiterProperties
|-- exception
|   |-- RateLimiterException
|   |-- RateLimiterFallbackException
|   `-- RateLimiterKeyResolveException
|-- fallback
|   |-- FallbackMethodInvoker
|   `-- DefaultFallbackMethodInvoker
|-- metrics
|   |-- RateLimiterMetrics
|   `-- DefaultRateLimiterMetrics
|-- resolver
|   |-- RateLimitKeyResolver
|   |-- CompositeRateLimitKeyResolver
|   `-- DefaultRateLimitKeyResolver
|-- rule
|   |-- RateLimitRule
|   |-- RateLimitRuleOverride
|   `-- RateLimitRuleResolver
|-- storage
|   |-- RateLimitStorage
|   |-- RedisRateLimitStorage
|   `-- LocalRateLimitStorage
`-- support
    |-- AopMethodResolver
    |-- MethodSignatureKey
    |-- RateLimitResult
    |-- RateLimiterConstants
    |-- RateLimiterRedisKeys
    `-- RateLimiterScripts
```

This layout follows the requirement document while using the repository package style and avoiding unnecessary internal layers.

## 8. Public Annotation

The annotation is method-level only:

```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimiterAccessInterceptor {

    String key() default "all";

    double permitsPerSecond();

    long blacklistCount() default 0L;

    String fallbackMethod();

    RateLimitScope scope() default RateLimitScope.GLOBAL;

    RateLimitAlgorithmType algorithm() default RateLimitAlgorithmType.TOKEN_BUCKET;

    long blacklistTtlSeconds() default 86400L;

    long rejectCountWindowSeconds() default 60L;

    long timeoutMillis() default 0L;

    String message() default "";

    String ruleId() default "";

    FailStrategy failStrategy() default FailStrategy.FAIL_OPEN;
}
```

Supporting enums:

```text
RateLimitScope: GLOBAL, LOCAL
RateLimitAlgorithmType: TOKEN_BUCKET
FailStrategy: FAIL_OPEN, FAIL_CLOSED, LOCAL_FALLBACK
EmptyKeyStrategy: REJECT, PASS, USE_ALL
```

`permitsPerSecond` must be greater than zero. `blacklistCount` must be greater than or equal to zero. `fallbackMethod` must resolve unless a global fallback handler bean is configured.

## 9. Runtime Flow

The AOP flow is:

1. Intercept a method annotated with `@RateLimiterAccessInterceptor`.
2. If the component is disabled, call `ProceedingJoinPoint.proceed()` immediately.
3. Resolve the most specific method and annotation from the Spring proxy context.
4. Build a `RateLimitRule` from annotation values, application properties, and rule overrides.
5. Validate the effective rule.
6. Resolve the identity key.
7. Build Redis keys using a common cluster hash tag.
8. Execute the selected algorithm.
9. If allowed, call the business method and never swallow business exceptions.
10. If limited or blacklisted, invoke the configured fallback.
11. Record structured logs and metrics.
12. For Redis errors, apply the effective failure strategy.

The rate limiter must distinguish component failures from business method failures. Business exceptions from `proceed()` are propagated as-is.

## 10. Rule Resolution

Rule merge priority from high to low:

```text
runtime Spring Bean overrides > application properties > annotation > component defaults
```

V1.0 implements the override seam with a Spring bean interface rather than coupling directly to the dynamic config center:

```java
public interface RateLimitRuleOverrideProvider {

    Optional<RateLimitRuleOverride> findOverride(String ruleId, String methodSignature);
}
```

The default provider returns `Optional.empty()`. A business application or later component integration can provide a bean that reads DCC, Nacos, local memory, database, or other configuration sources.

The rule resolver must reject invalid override values and keep the lower-priority rule rather than polluting the current execution with an invalid rule.

## 11. Key Resolution

V1.0 key support:

1. `all`
2. method parameter name, such as `userId`
3. nested object field, such as `user.id`
4. `header:<name>` when a web request is available
5. `ip` when a web request is available
6. custom `RateLimitKeyResolver` beans

SpEL can be reserved for a later version unless it can be added with a small Spring Expression dependency that does not broaden the starter shape. V1.0 should not block the core Redis limiter on SpEL support.

Identity normalization:

1. Trim blank input.
2. Apply configured empty-key strategy.
3. Limit raw key length before hashing.
4. Hash identity by default.
5. Do not log raw sensitive key values.

The default empty-key strategy is `REJECT`, because creating a shared `null` or blank Redis key can incorrectly blacklist unrelated callers.

## 12. Redis Key Contract

Redis keys use this logical shape:

```text
{prefix}:{app}:{env}:{resource}:{scope}:{identity}:{suffix}
```

For Redis Cluster, the slot-stable portion must be wrapped in a hash tag:

```text
egon:rl:{app:env:resource:scope:identity}:bucket
egon:rl:{app:env:resource:scope:identity}:reject
egon:rl:{app:env:resource:scope:identity}:blacklist
```

Defaults:

```text
prefix = egon:rl
app = spring.application.name or default-app
env = spring.profiles.active or default
resource = ruleId when present, otherwise method signature hash
identity = hash(normalized key) when key hashing is enabled
```

All keys must have TTL. The starter must not create permanent Redis keys.

## 13. Redis Lua Algorithm

The default algorithm is Redis Token Bucket.

The Lua script handles these operations atomically:

1. Read blacklist status.
2. Refill token bucket by elapsed time.
3. Check whether one requested permit is available.
4. Deduct one permit when allowed.
5. Increment reject count when limited.
6. Set reject count TTL when needed.
7. Add blacklist when `rejectCount >= blacklistCount`.
8. Set bucket TTL, reject TTL, and blacklist TTL.
9. Return a structured result array.

The script result maps to:

```text
allowed
limited
blacklisted
remainingPermits
retryAfterMillis
rejectCount
blacklistTtlSeconds
reason
```

Script SHA caching is enabled by default. If Redis reports a missing script, the executor reloads and retries once.

## 14. Local Fallback Algorithm

Local fallback is not the default production mode. It exists for:

1. explicit local scope
2. unit tests
3. Redis failure strategy `LOCAL_FALLBACK`

The implementation uses an in-memory token bucket per resource and identity. It must have eviction or TTL cleanup to avoid unbounded memory growth. Local mode logs with `mode=LOCAL`.

Guava is not required for V1.0 if a small JDK-based token bucket is enough. This avoids adding a new dependency for a small fallback path.

## 15. Blacklist Management

The blacklist is based on rejected requests, not total requests.

Trigger rule:

```text
same identity within rejectCountWindowSeconds
limited reject count >= blacklistCount
then write blacklist key for blacklistTtlSeconds
```

`blacklistCount = 0` disables automatic blacklisting.

`key = all` is protected by default. If `blacklist.protect-all-key=true`, global keys cannot be blacklisted automatically even when `blacklistCount` is configured.

The starter provides a Java service interface:

```java
boolean isBlacklisted(String resource, String identity);

void addBlacklist(String resource, String identity, long ttlSeconds);

void removeBlacklist(String resource, String identity);

long getBlacklistTtl(String resource, String identity);

long getRejectCount(String resource, String identity);
```

No UI or permission layer is provided. Business systems own access control around manual blacklist operations.

## 16. Fallback Invocation

Fallback method lookup priority:

1. method name plus original parameter types
2. method name plus original parameter types plus `RateLimitResult`
3. method name with no parameters
4. global fallback handler bean

The fallback resolver must:

1. work with Spring AOP proxies
2. resolve the most specific target class method
3. support inherited fallback methods
4. cache successful and failed lookup results when method cache is enabled
5. verify return type compatibility where possible
6. wrap fallback failures in `RateLimiterFallbackException`

Fallback must not recursively trigger the same rate limiter flow. The default implementation should invoke the method reflectively on the target object rather than calling through the proxied method path.

## 17. Auto-Configuration

`RateLimiterAutoConfiguration` is registered in:

```text
META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
```

Auto-configuration rules:

1. `@EnableConfigurationProperties(RateLimiterProperties.class)`
2. Enabled by `egon.cola.component.rate-limiter.enabled=true`, default true.
3. AOP aspect is not registered when disabled.
4. Default beans use `@ConditionalOnMissingBean`.
5. Redis beans are conditional on Redisson being available and Redis mode being selected.
6. Metrics binder is conditional on Micrometer and `MeterRegistry`.
7. No package scanning requirement is imposed on business applications.

The starter should reuse an application-provided `RedissonClient` when possible. It should not create a second client unless explicitly configured to do so.

## 18. Configuration Properties

Configuration prefix:

```yaml
egon:
  cola:
    component:
      rate-limiter:
        enabled: true
        mode: redis
        namespace: egon:rl
        app-name: ${spring.application.name:default-app}
        env: ${spring.profiles.active:default}
        default-fail-strategy: fail-open
        local-fallback-enabled: false
        key-hash-enabled: true
        empty-key-strategy: reject
        method-cache-enabled: true
        aop-order: -100
        redis:
          script-cache-enabled: true
          key-ttl-seconds: 300
          command-timeout-millis: 50
        blacklist:
          enabled: true
          default-ttl-seconds: 86400
          default-reject-window-seconds: 60
          protect-all-key: true
        metrics:
          enabled: true
          log-enabled: true
          slow-cost-millis: 20
        rules:
          drawLimiter:
            enabled: true
            permits-per-second: 1
            capacity: 1
            blacklist-count: 3
            blacklist-ttl-seconds: 86400
            reject-count-window-seconds: 60
            fail-strategy: fail-open
```

The implementation should use Java property objects rather than maps of raw strings wherever possible.

## 19. Observability

Structured logs include:

```text
traceId
app
env
module=rate-limiter
scene
method
ruleId
resource
scope
identityHash
permitsPerSecond
remainingPermits
rejectCount
blacklistTtlSeconds
failStrategy
costMs
message
```

Log levels:

1. allow: debug
2. limited: info
3. blacklist: warn
4. fallback failure: error
5. Redis failure: error with basic rate limiting
6. key resolution failure: warn

Metrics:

```text
rate_limiter_requests_total
rate_limiter_allowed_total
rate_limiter_limited_total
rate_limiter_blacklisted_total
rate_limiter_fallback_total
rate_limiter_redis_error_total
rate_limiter_cost_millis
```

Metrics are optional and must not force Actuator into business applications.

## 20. Design Pattern Consideration

The implementation should use the Strategy pattern for real variation points:

1. `RateLimitAlgorithm` isolates Redis Token Bucket from Local Token Bucket.
2. `RateLimitKeyResolver` isolates parameter, web request, and custom identity resolution.
3. `RateLimitRuleOverrideProvider` isolates dynamic configuration sources from the core starter.
4. `FallbackMethodInvoker` isolates fallback invocation policy.
5. `RateLimiterMetrics` isolates logging and Micrometer recording.

This pattern fits because the requirement document explicitly calls out replaceable algorithms, key parsing, storage, fallback, and metrics.

The implementation should not introduce a broad factory layer. Spring auto-configuration and `@ConditionalOnMissingBean` are enough for object selection. A Chain of Responsibility is not needed for the AOP flow because the runtime path is short and fixed. Template Method is not needed unless the Redis and local algorithms later share meaningful skeleton logic.

## 21. Testing Strategy

Starter module tests:

1. `RateLimiterAutoConfigurationTest`
   - creates default beans when enabled
   - creates no aspect when disabled
   - backs off when custom SPI beans are provided
2. `DefaultRateLimitKeyResolverTest`
   - resolves `all`
   - resolves parameter names
   - resolves nested object fields
   - rejects empty keys by default
3. `RateLimiterRedisKeysTest`
   - produces cluster-slot-safe hash tags
   - hashes identity when enabled
   - includes app and env
4. `DefaultFallbackMethodInvokerTest`
   - invokes same-argument fallback
   - invokes fallback with `RateLimitResult`
   - invokes no-arg fallback
   - wraps missing fallback
5. `LocalTokenBucketRateLimitAlgorithmTest`
   - allows within capacity
   - limits above capacity
   - recovers after refill
6. `RedisTokenBucketRateLimitAlgorithmTest`
   - proves Lua result mapping
   - proves blacklist is based on reject count
   - proves script reload retry on missing SHA

Test module samples:

1. annotated service fallback sample
2. auto-configuration sample
3. local mode sample
4. Redis failure strategy sample with fake storage or controlled failing bean

Validation commands should start targeted:

```bash
./mvnw -B -ntp -pl egon-cola-components/egon-cola-component-rate-limiter/egon-cola-component-rate-limiter-starter -am test
./mvnw -B -ntp -pl egon-cola-components/egon-cola-component-rate-limiter/egon-cola-component-rate-limiter-test -am test
```

Full component validation:

```bash
./mvnw -B -ntp -f egon-cola-components/pom.xml test
```

## 22. Acceptance Mapping

The design covers the requirement document acceptance criteria as follows:

1. Starter access: module, BOM export, auto-configuration.
2. Annotation limiting: `@RateLimiterAccessInterceptor` and AOP aspect.
3. Redis global limiting: Redisson-backed Lua Token Bucket.
4. Shared blacklist: Redis blacklist key with TTL.
5. Auto-expiring blacklist: Redis TTL.
6. Fallback return: proxy-aware fallback invoker.
7. Dynamic configuration: rule override provider plus application properties.
8. Proxy compatibility: method resolver and fallback resolver.
9. Redis degradation: `FAIL_OPEN`, `FAIL_CLOSED`, `LOCAL_FALLBACK`.
10. Logs and metrics: structured logs and optional Micrometer.
11. Stress stability: Lua atomic update, SHA cache, method cache, targeted concurrency tests.
12. Redis key cleanup: TTL required for bucket, reject, and blacklist keys.

## 23. Implementation Constraints

1. Make the smallest safe changes under `egon-cola-components`.
2. Do not modify existing Flyway migration files.
3. Do not start the project automatically.
4. Do not open a browser.
5. Add tests before production code for new behavior.
6. Keep commits scoped to task boundaries.
7. Do not broaden existing components or refactor unrelated modules.
8. Keep existing module naming, POM style, auto-configuration style, and test style consistent with sibling components.
9. Use path-scoped staging before commits.
10. Validate with targeted Maven commands before claiming completion.
