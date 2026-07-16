# Egon COLA Access Guard

[English](README.md) | [中文](README.zh-CN.md)

## Overview

`egon-cola-component-access-guard` is a method-level access-governance starter for Egon COLA. Through the default Spring AOP engine or the optional Bytecode Agent engine, it applies allow-list, deny-list, rate-limiting, timeout protection, and rejection handling at annotated entry points. It fits scenarios such as lotteries, coupon claims, login, risk checks, and hot-endpoint protection where governance should be applied consistently at the business-method boundary.

The component provides local implementations and replaceable extension points by default. It also keeps the compatible `DoWhiteList`, `DoRateLimiter`, and `DoHystrix` annotations so applications can migrate from legacy annotations gradually.

## Module Structure

| Module | Description |
|---|---|
| `egon-cola-component-access-guard-starter` | Spring Boot starter providing annotations, AOP, rule resolution, key resolution, allow-list, rate limiting, deny-list, timeout protection, rejection responses, and event extension points |
| `egon-cola-component-access-guard-test` | Component examples and integration verification |

## Execution Engines

`egon.cola.component.access-guard.engine` supports three mutually exclusive modes:

| Mode | Behavior |
|---|---|
| `AOP` | Default; registers a Spring AOP advisor and is suitable for ordinary Spring Bean methods |
| `AGENT` | Does not register Access Guard AOP; the Bytecode Agent governs methods and constructors |
| `DISABLED` | Keeps the base Beans but registers neither AOP nor Agent integration |

Agent mode requires the additional `egon-cola-component-bytecode-starter` dependency and the published Agent JAR installed at JVM startup:

```yaml
egon:
  cola:
    component:
      access-guard:
        engine: AGENT
```

```bash
java "-javaagent:/opt/egon/egon-cola-component-bytecode-agent-${egon-cola.version}.jar=enabled=true,features=access-guard,include=com.example.*" \
  -jar application.jar
```

In Agent mode, methods may be public or private instance methods or static methods. Protected, package-private, abstract, native, synthetic, and bridge methods fail explicitly during transformation. When a static method configures a fallback, the fallback must also be static; the original-arguments, original-arguments-plus-`AccessGuardContext`, and no-argument signatures remain supported. Synchronized methods retain their original monitor semantics, but timeout governance cannot be enabled for them.

Constructors support only public/private constructors with the aggregate `@AccessGuard` annotation. Governance runs before `this(...)`/`super(...)`, so no receiver is available. Only parameters, Header, IP, `all` keys, allow-list, deny-list, rate limiting, failure strategy, and events are supported; timeout, fallback, `returnJson`, `LOCAL_FALLBACK`, return replacement, and instance fields are not. Every annotated constructor in a `this(...)` chain is governed once. Use constructor governance cautiously for infrastructure classes to avoid startup dependency cycles before Spring is ready.

When Agent constructor mode uses a custom `AccessKeyResolver`, that implementation must also implement `ExecutableAccessKeyResolver`. Agent events and diagnostics never record arguments, return values, credentials, cookies, Authorization headers, or exception messages.

## Features

### Annotation Model

| Annotation | Description |
|---|---|
| `@AccessGuard` | Composite annotation that can enable allow-list, rate limiting, deny-list, and timeout protection together |
| `@WhiteListAccessInterceptor` | Allow-list check |
| `@RateLimiterAccessInterceptor` | Method rate limiting, with optional cumulative deny-listing after rate-limit failures |
| `@TimeoutCircuitBreaker` | Method timeout protection with fallback and `returnJson` support |
| `@DoWhiteList` | Compatible legacy allow-list annotation |
| `@DoRateLimiter` | Compatible legacy rate-limit annotation |
| `@DoHystrix` | Compatible legacy timeout/circuit-breaker annotation |

### Fixed Execution Order

`AccessGuardAop` applies governance to a matched method in this fixed order:

```text
white list -> blacklist -> rate limiter -> timeout protection -> business method
```

There are two allow-list modes:

| Mode | Behavior |
|---|---|
| `GATEKEEPER` | The allow-list acts as an admission gate; after it passes, deny-list, rate limiting, and timeout protection continue |
| `BYPASS_GUARD` | A matching allow-list entry invokes the business method directly and skips the remaining governance steps |

### Key Resolution

The default `DefaultAccessKeyResolver` supports:

| Key syntax | Description |
|---|---|
| `all` | One globally shared key |
| `userId` | Matches a method parameter name |
| `request.userId` | Reads an object property path |
| `header:X-User-Id` | Reads a Header from the current HTTP request |
| `ip` | Reads the IP from `X-Forwarded-For` or `remoteAddr` |

The raw key is passed through `AccessGuardKeyGenerator` to produce `normalizedKey` and `keyHash`, so sensitive raw values do not appear directly in events or storage.

### Rejection Responses

Rejected calls return results in this order:

1. If `fallbackMethod` is configured, invoke the fallback reflectively first. It may have the original arguments, the original arguments plus `AccessGuardContext`, or no arguments.
2. If `returnJson` is configured, parse JSON according to the original method's return type.
3. If the original method returns `String`, return the default string `access rejected`.
4. Other return types default to `null`.

### Extension Points

Most Beans in auto-configuration use `@ConditionalOnMissingBean`, so applications can provide their own implementations:

| Extension point | Purpose |
|---|---|
| `AccessGuardConfigProvider` | Provide global or method-level dynamic rule overrides |
| `AccessKeyResolver` | Customize access-key resolution |
| `WhiteListRepository` / `WhiteListService` | Customize the allow-list source |
| `RateLimiterExecutor` | Customize rate limiting |
| `BlacklistService` | Customize deny-list storage and accumulation policy |
| `TimeoutCircuitBreakerExecutor` | Customize timeout execution |
| `RejectResponseInvoker` | Customize rejection response generation |
| `AccessGuardEventListener` | Subscribe to governance events |

The starter includes `RedissonRateLimiterExecutor`, `RedissonBlacklistService`, `RedissonWhiteListRepository`, and `AccessGuardRedisKeys`. Provide custom Beans when Redis/Redisson semantics are required.

## Dependency

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>top.egon</groupId>
            <artifactId>egon-cola-components-bom</artifactId>
            <version>${egon-cola.version}</version>
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

The starter already includes `spring-boot-starter-aop` and `spring-web`; the business application only needs to enable Spring Boot auto-configuration.

## Configuration

The configuration prefix is `egon.cola.component.access-guard`:

```yaml
egon:
  cola:
    component:
      access-guard:
        enabled: true
        engine: AOP
        storage: REDISSON
        key-prefix: egon:access-guard
        fail-strategy: FAIL_OPEN
        key-resolve-failure-strategy: USE_ALL
        aop:
          order: -100
        redisson:
          client-bean-name: redissonClient
          auto-create-client: false
        white-list:
          empty-list-strategy: DENY_ALL
          mode: GATEKEEPER
          default-users: []
        rate-limiter:
          default-permits: 1
          default-interval: 1
          default-interval-unit: SECONDS
        blacklist:
          default-count: 0
          default-timeout: 24h
        circuit-breaker:
          default-timeout: 350ms
          executor: THREAD_POOL
          fallback-on-exception: false
          cancel-running-task: true
        thread-pool:
          name: access-guard
          core-pool-size: 4
          max-pool-size: 16
          queue-capacity: 1024
        dynamic:
          enabled: false
          provider-bean-name:
        local-fallback:
          enabled: true
          expire-after-write: 10m
```

Specific rules can be overridden through configuration:

```yaml
egon:
  cola:
    component:
      access-guard:
        rules:
          - name: draw-api
            key: userId
            white-list:
              enabled: true
              users:
                - hash001
              mode: GATEKEEPER
            rate-limiter:
              enabled: true
              permits: 1
              interval: 1
              interval-unit: SECONDS
              blacklist-enabled: true
              blacklist-count: 3
              blacklist-timeout: 24h
            circuit-breaker:
              enabled: true
              timeout: 350ms
            fallback-method: fallback
            fail-strategy: FAIL_OPEN
```

## Complete Examples

### 1. Composite Governance

```java
package demo.draw;

import org.springframework.stereotype.Service;
import top.egon.cola.component.accessguard.annotation.AccessGuard;
import top.egon.cola.component.accessguard.context.AccessGuardContext;

@Service
public class DrawService {

    @AccessGuard(
            name = "draw-api",
            key = "request.userId",
            whitelist = true,
            rateLimiter = true,
            blacklist = true,
            timeoutBreaker = true,
            fallbackMethod = "fallback"
    )
    public DrawResult draw(DrawRequest request) {
        return new DrawResult(true, "draw accepted");
    }

    private DrawResult fallback(DrawRequest request, AccessGuardContext context) {
        return new DrawResult(false, "access rejected by " + context.result().decision());
    }

    public record DrawRequest(String userId, String activityId) {
    }

    public record DrawResult(boolean success, String message) {
    }
}
```

### 2. Individual Allow-List, Rate-Limit, and Timeout Annotations

```java
package demo.draw;

import org.springframework.stereotype.Service;
import top.egon.cola.component.accessguard.annotation.RateLimiterAccessInterceptor;
import top.egon.cola.component.accessguard.annotation.TimeoutCircuitBreaker;
import top.egon.cola.component.accessguard.annotation.WhiteListAccessInterceptor;
import top.egon.cola.component.accessguard.annotation.WhiteListMode;

@Service
public class CouponService {

    @WhiteListAccessInterceptor(
            name = "coupon-white-list",
            key = "userId",
            users = {"hash001"},
            mode = WhiteListMode.GATEKEEPER,
            returnJson = "{\"success\":false,\"message\":\"not in white list\"}"
    )
    public CouponResult whiteListOnly(String userId) {
        return new CouponResult(true, "ok");
    }

    @RateLimiterAccessInterceptor(
            name = "coupon-rate-limit",
            key = "userId",
            permits = 1,
            interval = 1,
            blacklistCount = 3,
            fallbackMethod = "rateLimitFallback"
    )
    public CouponResult rateLimited(String userId) {
        return new CouponResult(true, "ok");
    }

    @TimeoutCircuitBreaker(
            name = "coupon-timeout",
            timeoutValue = 350,
            fallbackMethod = "timeoutFallback"
    )
    public CouponResult timeoutProtected(String userId) {
        return new CouponResult(true, "ok");
    }

    private CouponResult rateLimitFallback(String userId) {
        return new CouponResult(false, "too many requests");
    }

    private CouponResult timeoutFallback(String userId) {
        return new CouponResult(false, "timeout");
    }

    public record CouponResult(boolean success, String message) {
    }
}
```

### 3. Compatible Annotations

```java
package demo.draw;

import org.springframework.stereotype.Service;
import top.egon.cola.component.accessguard.annotation.DoHystrix;
import top.egon.cola.component.accessguard.annotation.DoRateLimiter;
import top.egon.cola.component.accessguard.annotation.DoWhiteList;

@Service
public class LegacyDrawService {

    @DoWhiteList(key = "userId", returnJson = "{\"success\":false,\"message\":\"deny\"}")
    public String whiteList(String userId) {
        return "ok";
    }

    @DoRateLimiter(permitsPerSecond = 0.5d, returnJson = "{\"success\":false,\"message\":\"limited\"}")
    public String rateLimit(String userId) {
        return "ok";
    }

    @DoHystrix(timeoutValue = 350, returnJson = "{\"success\":false,\"message\":\"timeout\"}")
    public String timeout(String userId) {
        return "ok";
    }
}
```

### 4. Using Redisson for Rate Limiting, Allow-Lists, and Deny-Lists

```java
package demo.draw.config;

import org.redisson.api.RedissonClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import top.egon.cola.component.accessguard.blacklist.BlacklistService;
import top.egon.cola.component.accessguard.blacklist.RedissonBlacklistService;
import top.egon.cola.component.accessguard.ratelimiter.LocalRateLimiterExecutor;
import top.egon.cola.component.accessguard.ratelimiter.RateLimiterExecutor;
import top.egon.cola.component.accessguard.ratelimiter.RedissonRateLimiterExecutor;
import top.egon.cola.component.accessguard.support.AccessGuardRedisKeys;
import top.egon.cola.component.accessguard.whitelist.RedissonWhiteListRepository;
import top.egon.cola.component.accessguard.whitelist.WhiteListRepository;

@Configuration
public class AccessGuardRedisConfig {

    @Bean
    public AccessGuardRedisKeys accessGuardRedisKeys() {
        return new AccessGuardRedisKeys("egon:access-guard", "draw-service", "prod");
    }

    @Bean
    public RateLimiterExecutor rateLimiterExecutor(RedissonClient redissonClient,
                                                   AccessGuardRedisKeys redisKeys) {
        return new RedissonRateLimiterExecutor(redissonClient, redisKeys, new LocalRateLimiterExecutor());
    }

    @Bean
    public WhiteListRepository whiteListRepository(RedissonClient redissonClient,
                                                   AccessGuardRedisKeys redisKeys) {
        return new RedissonWhiteListRepository(redissonClient, redisKeys);
    }

    @Bean
    public BlacklistService blacklistService(RedissonClient redissonClient,
                                             AccessGuardRedisKeys redisKeys) {
        return new RedissonBlacklistService(redissonClient, redisKeys);
    }
}
```

## Design And Implementation Details

### Design Principles

1. Governance runs at the method boundary, keeping business methods focused on business logic.
2. Annotations declare static rules while `AccessGuardConfigProvider` enables runtime overrides, balancing convenience with dynamic governance.
3. The AOP flow is fixed so composing multiple governance actions never produces an ambiguous order.
4. Key resolution, rule resolution, rate limiting, deny-listing, rejection responses, and event publication are interfaces that applications can replace independently.
5. Local implementations make the starter usable out of the box; Redis/Redisson capabilities are supplied through custom Beans instead of binding the component to one infrastructure choice.

### Implementation Details

- `AccessGuardAutoConfiguration` is registered through `AutoConfiguration.imports`; `enabled` defaults to `true`.
- `AccessGuardAnnotationResolver` normalizes the supported annotations into `AccessGuardRule`.
- `AccessGuardRuleResolver` precedence is annotation rule -> same-name property rule -> global dynamic override -> method-level dynamic override.
- `AccessGuardAop` resolves the method and key first, then runs allow-list, deny-list, rate limiting, and timeout protection, and finally publishes `AccessGuardEvent`.
- `LocalRateLimiterExecutor` uses an in-memory token bucket with the key `{ruleName}:{accessKeyHash}`.
- `RedissonRateLimiterExecutor` uses Redisson `RRateLimiter`; Redis keys are generated by `AccessGuardRedisKeys`.
- `ThreadPoolTimeoutCircuitBreakerExecutor` runs the protected method on a fixed thread pool and uses fallback or `returnJson` after a timeout.
- `ReflectionFallbackInvoker` searches the current class and its superclasses for a fallback and supports original arguments, original arguments plus `AccessGuardContext`, and no arguments.
- `NoopAccessGuardEventPublisher` actually iterates over every `AccessGuardEventListener`; the default `LoggingAccessGuardEventListener` writes governance-event logs.

## Boundaries And Notes

- `@AccessGuard` and the related annotations apply only to methods.
- The default `WhiteListRepository` always reports a miss. If the allow-list is enabled without configured `users` or a repository, the default policy denies access.
- The default `BlacklistService` does not persist deny-list entries. Use `RedissonBlacklistService` or a custom implementation for cross-instance deny-listing.
- The default `RateLimiterExecutor` is an in-memory limiter. Use the Redisson implementation for global multi-instance rate limiting.
- Fallback return values must be compatible with the original method return type.
- `returnJson` is parsed according to the original return type; string return values may be ordinary strings or JSON strings.

## Verification

```bash
./mvnw -B -ntp -pl egon-cola-components/egon-cola-component-access-guard/egon-cola-component-access-guard-starter -am test
./mvnw -B -ntp -pl egon-cola-components/egon-cola-component-access-guard/egon-cola-component-access-guard-test -am -Dsurefire.failIfNoSpecifiedTests=false test
```
