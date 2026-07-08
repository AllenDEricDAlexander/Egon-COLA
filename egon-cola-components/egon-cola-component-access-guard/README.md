# Egon COLA Access Guard

`egon-cola-component-access-guard` provides a Spring Boot starter for method access governance.

Capabilities:

- White list short-circuiting
- Redisson global rate limiting
- Temporary blacklist after repeated limit hits
- Timeout protection with fallback and `returnJson`
- Compatibility annotations for existing `DoWhiteList`, `DoRateLimiter`, and `DoHystrix` style usage
- Dynamic configuration provider extension points

## Dependency

The runtime starter artifact exported by `egon-cola-components-bom` is:

```xml
<dependency>
    <groupId>top.egon</groupId>
    <artifactId>egon-cola-component-access-guard-starter</artifactId>
</dependency>
```

## Configuration

Configuration prefix:

```properties
egon.cola.component.access-guard.enabled=true
egon.cola.component.access-guard.key-prefix=egon:access-guard
egon.cola.component.access-guard.fail-strategy=FAIL_OPEN
egon.cola.component.access-guard.white-list.empty-list-strategy=DENY_ALL
egon.cola.component.access-guard.circuit-breaker.default-timeout=350ms
```

## White List

```java
@WhiteListAccessInterceptor(name = "draw-api", key = "userId", users = "hash001")
public String draw(String userId) {
    return userId;
}
```

Compatibility:

```java
@DoWhiteList(key = "userId", returnJson = "{\"code\":\"deny\"}")
public String draw(String userId) {
    return userId;
}
```

## Rate Limiter

```java
@RateLimiterAccessInterceptor(
        name = "draw-api",
        key = "userId",
        permits = 1,
        interval = 1,
        fallbackMethod = "fallback"
)
public String draw(String userId) {
    return userId;
}
```

Compatibility:

```java
@DoRateLimiter(permitsPerSecond = 0.5d, returnJson = "{\"code\":\"limited\"}")
public String draw(String userId) {
    return userId;
}
```

## Timeout

```java
@TimeoutCircuitBreaker(name = "draw-api", timeoutValue = 350, fallbackMethod = "timeoutFallback")
public String draw(String userId) {
    return userId;
}
```

Compatibility:

```java
@DoHystrix(timeoutValue = 350, returnJson = "{\"code\":\"timeout\"}")
public String draw(String userId) {
    return userId;
}
```

## Combined Flow

`AccessGuardAop` executes a fixed method governance order:

```text
white list -> blacklist -> rate limiter -> timeout protection -> business method
```

`WhiteListMode.BYPASS_GUARD` skips the remaining guard steps after a white-list hit.

## Validation

```bash
./mvnw -B -ntp -pl egon-cola-components/egon-cola-component-access-guard/egon-cola-component-access-guard-starter -am test
./mvnw -B -ntp -pl egon-cola-components/egon-cola-component-access-guard/egon-cola-component-access-guard-test -am -Dsurefire.failIfNoSpecifiedTests=false test
```
