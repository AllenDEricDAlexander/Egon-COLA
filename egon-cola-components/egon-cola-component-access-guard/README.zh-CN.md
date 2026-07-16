# Egon COLA Access Guard

[English](README.md) | 中文

## 简要介绍

`egon-cola-component-access-guard` 是 Egon COLA 的方法级访问治理 starter。它可以通过默认的 Spring AOP 引擎或可选的 Bytecode Agent 引擎，对标注入口执行白名单、黑名单、限流、超时保护和拒绝响应处理，适合抽奖、优惠券领取、登录、风控校验、热点接口保护等需要在业务方法入口统一治理的场景。

组件默认提供本地实现和可覆盖扩展点，同时保留 `DoWhiteList`、`DoRateLimiter`、`DoHystrix` 兼容注解，便于从旧式注解平滑迁移。

## 模块结构

| Module | 说明 |
|---|---|
| `egon-cola-component-access-guard-starter` | Spring Boot starter，提供注解、AOP、规则解析、key 解析、白名单、限流、黑名单、超时保护、拒绝响应和事件扩展点 |
| `egon-cola-component-access-guard-test` | 组件样例和集成验证模块 |

## 执行引擎

`egon.cola.component.access-guard.engine` 支持三种互斥模式：

| 模式 | 行为 |
|---|---|
| `AOP` | 默认值，注册 Spring AOP Advisor，适合普通 Spring Bean 方法 |
| `AGENT` | 不注册 Access Guard AOP，由 Bytecode Agent 治理方法和构造器 |
| `DISABLED` | 保留基础 Bean，但不注册 AOP 或 Agent 集成 |

Agent 模式需要额外引入 `egon-cola-component-bytecode-starter`，并在 JVM 启动时安装发布的 Agent JAR：

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

Agent 模式的方法支持边界是 public/private 实例方法和 static 方法；protected、package-private、abstract、native、synthetic、bridge 方法会在转换阶段明确失败。static 方法配置 fallback 时，fallback 也必须为 static，并继续支持原方法同参、同参追加 `AccessGuardContext` 或无参三种签名。synchronized 方法保留原监视器语义，但不允许开启超时治理。

构造器仅支持 public/private 且只接受组合式 `@AccessGuard`。治理发生在 `this(...)`/`super(...)` 之前，因此没有可用的 `this`，只支持参数、Header、IP、`all` key，以及白名单、黑名单、限流、失败策略和事件；不支持超时、fallback、`returnJson`、`LOCAL_FALLBACK`、返回值替换或实例字段。`this(...)` 链上每个标注构造器各治理一次。基础设施类构造器应谨慎使用，避免在 Spring 运行时就绪前形成启动依赖环。

如果 Agent 构造器模式自定义 `AccessKeyResolver`，该实现还必须实现 `ExecutableAccessKeyResolver`。Agent 事件和诊断不会记录参数、返回值、凭证、Cookie、Authorization 或异常消息。

## 功能说明

### 注解模型

| 注解 | 说明 |
|---|---|
| `@AccessGuard` | 组合式注解，可同时启用白名单、限流、黑名单、超时保护 |
| `@WhiteListAccessInterceptor` | 白名单校验 |
| `@RateLimiterAccessInterceptor` | 方法限流，支持限流失败后累计黑名单 |
| `@TimeoutCircuitBreaker` | 方法超时保护，支持 fallback 和 `returnJson` |
| `@DoWhiteList` | 兼容旧白名单注解 |
| `@DoRateLimiter` | 兼容旧限流注解 |
| `@DoHystrix` | 兼容旧超时/熔断注解 |

### 固定执行顺序

`AccessGuardAop` 对命中的方法按固定顺序执行治理逻辑：

```text
white list -> blacklist -> rate limiter -> timeout protection -> business method
```

白名单命中时有两种模式：

| 模式 | 行为 |
|---|---|
| `GATEKEEPER` | 白名单作为准入门禁，通过后继续执行黑名单、限流、超时保护 |
| `BYPASS_GUARD` | 白名单命中后直接执行业务方法，跳过后续治理 |

### key 解析

默认 `DefaultAccessKeyResolver` 支持：

| key 写法 | 说明 |
|---|---|
| `all` | 全局共享 key |
| `userId` | 匹配方法参数名 |
| `request.userId` | 从对象属性路径读取 |
| `header:X-User-Id` | 从当前 HTTP 请求 Header 读取 |
| `ip` | 从 `X-Forwarded-For` 或 `remoteAddr` 读取 IP |

解析出的原始 key 会经过 `AccessGuardKeyGenerator` 生成 `normalizedKey` 和 `keyHash`，避免把敏感原值直接用于事件和存储。

### 拒绝响应

拒绝时按以下顺序返回结果：

1. 如果配置了 `fallbackMethod`，优先反射调用 fallback。fallback 可以与原方法同参，也可以额外追加 `AccessGuardContext`，也可以无参。
2. 如果配置了 `returnJson`，按原方法返回类型解析 JSON。
3. 如果原方法返回 `String`，返回默认字符串 `access rejected`。
4. 其他返回类型默认返回 `null`。

### 扩展点

AutoConfiguration 中大部分 Bean 都使用 `@ConditionalOnMissingBean`，业务侧可以提供自己的实现：

| 扩展点 | 用途 |
|---|---|
| `AccessGuardConfigProvider` | 提供全局或方法级动态规则覆盖 |
| `AccessKeyResolver` | 自定义访问 key 解析 |
| `WhiteListRepository` / `WhiteListService` | 自定义白名单来源 |
| `RateLimiterExecutor` | 自定义限流实现 |
| `BlacklistService` | 自定义黑名单存储和累计策略 |
| `TimeoutCircuitBreakerExecutor` | 自定义超时执行器 |
| `RejectResponseInvoker` | 自定义拒绝响应生成 |
| `AccessGuardEventListener` | 订阅治理事件 |

starter 内置 `RedissonRateLimiterExecutor`、`RedissonBlacklistService`、`RedissonWhiteListRepository` 和 `AccessGuardRedisKeys`，需要 Redis/Redisson 语义时可以通过自定义 Bean 接入。

## 依赖方式

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

starter 已包含 `spring-boot-starter-aop` 和 `spring-web`，业务应用只需要启用 Spring Boot 自动配置即可。

## 配置说明

配置前缀为 `egon.cola.component.access-guard`：

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

可以通过配置覆盖指定规则：

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

## 完整的使用示例

### 1. 组合式治理

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

### 2. 单独使用白名单、限流和超时注解

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

### 3. 使用兼容注解

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

### 4. 接入 Redisson 限流、白名单和黑名单

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

## 设计思想和实现细节

### 设计思想

1. 治理逻辑放在方法入口，业务方法保持纯业务代码。
2. 注解声明静态规则，`AccessGuardConfigProvider` 允许运行时覆盖，兼顾易用性和动态治理。
3. AOP 流程固定，避免多个治理动作组合后顺序不确定。
4. key 解析、规则解析、限流、黑名单、拒绝响应和事件发布都拆成接口，业务可以按需替换。
5. 默认本地实现保证 starter 开箱可用，Redis/Redisson 能力通过自定义 Bean 接入，避免强绑具体基础设施。

### 实现细节

- `AccessGuardAutoConfiguration` 通过 `AutoConfiguration.imports` 注册，`enabled` 缺省为 `true`。
- `AccessGuardAnnotationResolver` 把各种注解统一解析为 `AccessGuardRule`。
- `AccessGuardRuleResolver` 的优先级是注解规则 -> properties 中同名规则 -> 全局动态覆盖 -> 方法级动态覆盖。
- `AccessGuardAop` 先解析方法和 key，再执行白名单、黑名单、限流、超时保护，最后发布 `AccessGuardEvent`。
- `LocalRateLimiterExecutor` 使用内存 token bucket，key 为 `{ruleName}:{accessKeyHash}`。
- `RedissonRateLimiterExecutor` 使用 Redisson `RRateLimiter`，Redis key 由 `AccessGuardRedisKeys` 生成。
- `ThreadPoolTimeoutCircuitBreakerExecutor` 使用固定线程池执行受保护方法，超时后走 fallback 或 `returnJson`。
- `ReflectionFallbackInvoker` 会在当前类及父类中查找 fallback 方法，支持同参、追加 `AccessGuardContext` 和无参方法。
- `NoopAccessGuardEventPublisher` 实际会遍历所有 `AccessGuardEventListener`，默认 `LoggingAccessGuardEventListener` 输出治理事件日志。

## 边界和注意事项

- `@AccessGuard` 等注解只作用于方法。
- 默认 `WhiteListRepository` 总是返回未命中，若启用白名单且没有配置 `users` 或仓储，默认策略会拒绝访问。
- 默认 `BlacklistService` 不持久化黑名单；需要跨实例黑名单时应接入 `RedissonBlacklistService` 或自定义实现。
- 默认 `RateLimiterExecutor` 是本地内存限流；多实例全局限流应接入 Redisson 实现。
- fallback 返回值应与原方法返回类型兼容。
- `returnJson` 会按原方法返回类型解析，字符串返回值可以直接使用普通字符串或 JSON 字符串。

## 验证命令

```bash
./mvnw -B -ntp -pl egon-cola-components/egon-cola-component-access-guard/egon-cola-component-access-guard-starter -am test
./mvnw -B -ntp -pl egon-cola-components/egon-cola-component-access-guard/egon-cola-component-access-guard-test -am -Dsurefire.failIfNoSpecifiedTests=false test
```
