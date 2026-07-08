# Access Guard Component Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the Egon-COLA `egon-cola-component-access-guard` Spring Boot Starter for white list, Redisson rate limiting, blacklist, timeout protection, fallback, `returnJson`, and compatibility annotations.

**Architecture:** Add one component root with a starter module and a test/sample module. The starter uses a single `AccessGuardAop` facade to orchestrate fixed governance order: white list, blacklist, Redisson rate limiter, timeout protection, then business method. Variation points use Spring Bean Strategy interfaces and default implementations backed by properties, Redisson, reflection, and optional Micrometer.

**Tech Stack:** Java 21, Maven, Spring Boot 3.5.16, Spring AOP, Redisson, Jackson, Micrometer optional, JUnit 5, AssertJ, `ApplicationContextRunner`.

---

## Source Spec

Implementation follows:

```text
docs/superpowers/specs/2026-07-08-access-guard-component-design.md
```

Do not start any runtime service and do not open a browser. Use Maven tests and static checks only.

## File Structure

Create:

```text
egon-cola-components/egon-cola-component-access-guard/pom.xml
egon-cola-components/egon-cola-component-access-guard/README.md
egon-cola-components/egon-cola-component-access-guard/egon-cola-component-access-guard-starter/pom.xml
egon-cola-components/egon-cola-component-access-guard/egon-cola-component-access-guard-starter/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
egon-cola-components/egon-cola-component-access-guard/egon-cola-component-access-guard-test/pom.xml
```

Starter source root:

```text
egon-cola-components/egon-cola-component-access-guard/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard
```

Starter test root:

```text
egon-cola-components/egon-cola-component-access-guard/egon-cola-component-access-guard-starter/src/test/java/top/egon/cola/component/accessguard
```

Test module source root:

```text
egon-cola-components/egon-cola-component-access-guard/egon-cola-component-access-guard-test/src/test/java/top/egon/cola/component/accessguard/test
```

Modify:

```text
egon-cola-components/pom.xml
egon-cola-components/egon-cola-components-bom/pom.xml
```

## Task 1: Maven Module Skeleton and BOM Export

**Files:**
- Modify: `egon-cola-components/pom.xml`
- Modify: `egon-cola-components/egon-cola-components-bom/pom.xml`
- Create: `egon-cola-components/egon-cola-component-access-guard/pom.xml`
- Create: `egon-cola-components/egon-cola-component-access-guard/egon-cola-component-access-guard-starter/pom.xml`
- Create: `egon-cola-components/egon-cola-component-access-guard/egon-cola-component-access-guard-test/pom.xml`
- Create: `egon-cola-components/egon-cola-component-access-guard/README.md`

- [ ] **Step 1: Verify the module does not exist**

Run:

```bash
test ! -d egon-cola-components/egon-cola-component-access-guard
```

Expected: exit 0.

- [ ] **Step 2: Create the component parent POM**

Create `egon-cola-components/egon-cola-component-access-guard/pom.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>top.egon</groupId>
        <artifactId>egon-cola-components-parent</artifactId>
        <version>5.2.0-SNAPSHOT</version>
        <relativePath>../pom.xml</relativePath>
    </parent>

    <artifactId>egon-cola-component-access-guard</artifactId>
    <packaging>pom</packaging>
    <name>egon-cola-component-access-guard</name>
    <description>Access guard component for Egon COLA.</description>

    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>top.egon</groupId>
                <artifactId>egon-cola-component-common-core</artifactId>
                <version>${project.version}</version>
            </dependency>
            <dependency>
                <groupId>top.egon</groupId>
                <artifactId>egon-cola-component-common-trace</artifactId>
                <version>${project.version}</version>
            </dependency>
            <dependency>
                <groupId>top.egon</groupId>
                <artifactId>egon-cola-component-access-guard-starter</artifactId>
                <version>${project.version}</version>
            </dependency>
        </dependencies>
    </dependencyManagement>

    <modules>
        <module>egon-cola-component-access-guard-starter</module>
        <module>egon-cola-component-access-guard-test</module>
    </modules>
</project>
```

- [ ] **Step 3: Create the starter POM**

Create `egon-cola-components/egon-cola-component-access-guard/egon-cola-component-access-guard-starter/pom.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>top.egon</groupId>
        <artifactId>egon-cola-component-access-guard</artifactId>
        <version>5.2.0-SNAPSHOT</version>
        <relativePath>../pom.xml</relativePath>
    </parent>

    <artifactId>egon-cola-component-access-guard-starter</artifactId>
    <packaging>jar</packaging>
    <name>egon-cola-component-access-guard-starter</name>
    <description>Spring Boot starter for Egon COLA access guard.</description>

    <dependencies>
        <dependency>
            <groupId>top.egon</groupId>
            <artifactId>egon-cola-component-common-core</artifactId>
        </dependency>
        <dependency>
            <groupId>top.egon</groupId>
            <artifactId>egon-cola-component-common-trace</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-autoconfigure</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-aop</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework</groupId>
            <artifactId>spring-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.redisson</groupId>
            <artifactId>redisson-spring-boot-starter</artifactId>
            <exclusions>
                <exclusion>
                    <groupId>org.springframework.boot</groupId>
                    <artifactId>spring-boot-starter-actuator</artifactId>
                </exclusion>
            </exclusions>
        </dependency>
        <dependency>
            <groupId>io.micrometer</groupId>
            <artifactId>micrometer-core</artifactId>
            <optional>true</optional>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-configuration-processor</artifactId>
            <optional>true</optional>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>
</project>
```

- [ ] **Step 4: Create the test module POM**

Create `egon-cola-components/egon-cola-component-access-guard/egon-cola-component-access-guard-test/pom.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>top.egon</groupId>
        <artifactId>egon-cola-component-access-guard</artifactId>
        <version>5.2.0-SNAPSHOT</version>
        <relativePath>../pom.xml</relativePath>
    </parent>

    <artifactId>egon-cola-component-access-guard-test</artifactId>
    <packaging>jar</packaging>
    <name>egon-cola-component-access-guard-test</name>
    <description>Sample and validation module for Egon COLA access guard.</description>

    <dependencies>
        <dependency>
            <groupId>top.egon</groupId>
            <artifactId>egon-cola-component-access-guard-starter</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>
</project>
```

- [ ] **Step 5: Register the component in the components parent**

Add this module to the `<modules>` section in `egon-cola-components/pom.xml` after `egon-cola-component-rule-engine`:

```xml
<module>egon-cola-component-access-guard</module>
```

- [ ] **Step 6: Export only the starter from the BOM**

Add this dependency to `egon-cola-components/egon-cola-components-bom/pom.xml` under `dependencyManagement.dependencies` after `egon-cola-component-rule-engine-starter`:

```xml
<dependency>
    <groupId>top.egon</groupId>
    <artifactId>egon-cola-component-access-guard-starter</artifactId>
    <version>${project.version}</version>
</dependency>
```

- [ ] **Step 7: Create the initial README**

Create `egon-cola-components/egon-cola-component-access-guard/README.md`:

````markdown
# Egon COLA Access Guard Component

This component provides a Spring Boot starter for application-internal method access governance.

## Modules

| Module | Purpose |
|---|---|
| `egon-cola-component-access-guard-starter` | Business application starter, annotations, AOP, white list, Redisson rate limiter, blacklist, timeout protection, reject responses, events, and metrics. |
| `egon-cola-component-access-guard-test` | Sample and validation module for standard and compatibility annotations. |

## Starter Dependency

```xml
<dependency>
    <groupId>top.egon</groupId>
    <artifactId>egon-cola-component-access-guard-starter</artifactId>
    <version>5.2.0-SNAPSHOT</version>
</dependency>
```

## Configuration Prefix

```yaml
egon:
  cola:
    component:
      access-guard:
        enabled: true
```

## Boundary

V1.0 is an application-internal Spring AOP starter. It is not a gateway limiter, admin platform, database audit system, or full circuit breaker state machine.
````

- [ ] **Step 8: Verify Maven recognizes the new modules**

Run:

```bash
./mvnw -B -ntp -pl egon-cola-components/egon-cola-component-access-guard -am validate
```

Expected: `BUILD SUCCESS`.

- [ ] **Step 9: Commit**

```bash
git add egon-cola-components/pom.xml egon-cola-components/egon-cola-components-bom/pom.xml egon-cola-components/egon-cola-component-access-guard
git commit -m "feat: add access guard component modules"
```

## Task 2: Annotation and Core Contract Model

**Files:**
- Create: `egon-cola-components/egon-cola-component-access-guard/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/annotation/AccessGuard.java`
- Create: `egon-cola-components/egon-cola-component-access-guard/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/annotation/WhiteListAccessInterceptor.java`
- Create: `egon-cola-components/egon-cola-component-access-guard/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/annotation/RateLimiterAccessInterceptor.java`
- Create: `egon-cola-components/egon-cola-component-access-guard/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/annotation/TimeoutCircuitBreaker.java`
- Create: `egon-cola-components/egon-cola-component-access-guard/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/annotation/DoWhiteList.java`
- Create: `egon-cola-components/egon-cola-component-access-guard/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/annotation/DoRateLimiter.java`
- Create: `egon-cola-components/egon-cola-component-access-guard/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/annotation/DoHystrix.java`
- Create: `egon-cola-components/egon-cola-component-access-guard/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/context/AccessGuardContext.java`
- Create: `egon-cola-components/egon-cola-component-access-guard/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/context/AccessGuardDecision.java`
- Create: `egon-cola-components/egon-cola-component-access-guard/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/context/AccessGuardResult.java`
- Create: `egon-cola-components/egon-cola-component-access-guard/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/config/AccessGuardRule.java`
- Create: `egon-cola-components/egon-cola-component-access-guard/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/config/AccessGuardRuleOverride.java`
- Test: `egon-cola-components/egon-cola-component-access-guard/egon-cola-component-access-guard-starter/src/test/java/top/egon/cola/component/accessguard/annotation/AccessGuardAnnotationTest.java`

- [ ] **Step 1: Write the failing annotation contract test**

Create `egon-cola-components/egon-cola-component-access-guard/egon-cola-component-access-guard-starter/src/test/java/top/egon/cola/component/accessguard/annotation/AccessGuardAnnotationTest.java`:

```java
package top.egon.cola.component.accessguard.annotation;

import org.junit.jupiter.api.Test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class AccessGuardAnnotationTest {

    @Test
    void rateLimiterAnnotationShouldExposeCompatibilityDefaults() throws NoSuchMethodException {
        RateLimiterAccessInterceptor annotation = Sample.class.getDeclaredMethod("rateLimited", String.class)
                .getAnnotation(RateLimiterAccessInterceptor.class);

        assertThat(annotation.name()).isEqualTo("draw-api");
        assertThat(annotation.key()).isEqualTo("userId");
        assertThat(annotation.permitsPerSecond()).isEqualTo(1.0d);
        assertThat(annotation.permits()).isEqualTo(1L);
        assertThat(annotation.interval()).isEqualTo(1L);
        assertThat(annotation.intervalUnit()).isEqualTo(TimeUnit.SECONDS);
        assertThat(annotation.blacklistCount()).isEqualTo(3L);
        assertThat(annotation.fallbackMethod()).isEqualTo("fallback");
    }

    @Test
    void timeoutAnnotationShouldBeMethodLevelOnly() {
        Target target = TimeoutCircuitBreaker.class.getAnnotation(Target.class);

        assertThat(target.value()).containsExactly(ElementType.METHOD);
    }

    static class Sample {

        @RateLimiterAccessInterceptor(
                name = "draw-api",
                key = "userId",
                permitsPerSecond = 1.0d,
                blacklistCount = 3,
                fallbackMethod = "fallback"
        )
        String rateLimited(String userId) {
            return userId;
        }
    }
}
```

- [ ] **Step 2: Run the failing test**

Run:

```bash
./mvnw -B -ntp -pl egon-cola-components/egon-cola-component-access-guard/egon-cola-component-access-guard-starter -am -Dtest=AccessGuardAnnotationTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: fail because `RateLimiterAccessInterceptor` and `TimeoutCircuitBreaker` do not exist.

- [ ] **Step 3: Add enums and annotations**

Create these enum files under `annotation`:

```java
package top.egon.cola.component.accessguard.annotation;

public enum FailStrategy {
    GLOBAL_DEFAULT,
    FAIL_OPEN,
    FAIL_CLOSED,
    LOCAL_FALLBACK
}
```

```java
package top.egon.cola.component.accessguard.annotation;

public enum WhiteListMode {
    GATEKEEPER,
    BYPASS_GUARD
}
```

```java
package top.egon.cola.component.accessguard.annotation;

public enum TimeoutExecutorType {
    GLOBAL_DEFAULT,
    THREAD_POOL,
    VIRTUAL_THREAD,
    HYSTRIX_ADAPTER,
    CUSTOM
}
```

Create `RateLimiterAccessInterceptor.java`:

```java
package top.egon.cola.component.accessguard.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimiterAccessInterceptor {

    String name() default "";

    String key() default "all";

    String keyExpression() default "";

    double permitsPerSecond() default -1.0d;

    long permits() default 1L;

    long interval() default 1L;

    TimeUnit intervalUnit() default TimeUnit.SECONDS;

    long blacklistCount() default 0L;

    long blacklistTimeout() default 24L;

    TimeUnit blacklistTimeUnit() default TimeUnit.HOURS;

    String fallbackMethod() default "";

    String returnJson() default "";

    FailStrategy failStrategy() default FailStrategy.GLOBAL_DEFAULT;

    boolean enableBlacklistForAllKey() default false;
}
```

Create `WhiteListAccessInterceptor.java`, `TimeoutCircuitBreaker.java`, `AccessGuard.java`, `DoWhiteList.java`, `DoRateLimiter.java`, and `DoHystrix.java` with the fields from the spec using `@Target(ElementType.METHOD)`, `@Retention(RetentionPolicy.RUNTIME)`, and `@Documented`.

- [ ] **Step 4: Add core context and result records**

Use Java records for immutable results where mutation is not needed:

```java
package top.egon.cola.component.accessguard.context;

public enum AccessGuardDecision {
    PASS,
    WHITELIST_REJECTED,
    RATE_LIMITED,
    BLACKLIST_HIT,
    CIRCUIT_BREAKER_TIMEOUT,
    CIRCUIT_BREAKER_REJECTED,
    REDISSON_ERROR,
    KEY_RESOLVE_FAILED,
    FALLBACK_INVOKED,
    RETURN_JSON_INVOKED
}
```

```java
package top.egon.cola.component.accessguard.context;

public record AccessGuardResult(
        boolean passed,
        AccessGuardDecision decision,
        String ruleName,
        String accessKeyHash,
        String message
) {

    public static AccessGuardResult pass(String ruleName, String accessKeyHash) {
        return new AccessGuardResult(true, AccessGuardDecision.PASS, ruleName, accessKeyHash, "pass");
    }

    public static AccessGuardResult reject(AccessGuardDecision decision, String ruleName, String accessKeyHash, String message) {
        return new AccessGuardResult(false, decision, ruleName, accessKeyHash, message);
    }
}
```

Create `AccessGuardContext` as a mutable request-scoped class with fields `ruleName`, `methodSignature`, `accessKey`, `accessKeyHash`, `startNanos`, `attributes`, and `result`.

- [ ] **Step 5: Add rule records**

Create `AccessGuardRule` as an immutable record containing all effective rule settings used by AOP:

```java
package top.egon.cola.component.accessguard.config;

import top.egon.cola.component.accessguard.annotation.FailStrategy;
import top.egon.cola.component.accessguard.annotation.TimeoutExecutorType;
import top.egon.cola.component.accessguard.annotation.WhiteListMode;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

public record AccessGuardRule(
        String name,
        String key,
        String keyExpression,
        boolean whiteListEnabled,
        List<String> whiteListUsers,
        WhiteListMode whiteListMode,
        boolean rateLimiterEnabled,
        long permits,
        long interval,
        TimeUnit intervalUnit,
        boolean blacklistEnabled,
        long blacklistCount,
        Duration blacklistTimeout,
        boolean enableBlacklistForAllKey,
        boolean timeoutEnabled,
        Duration timeout,
        TimeoutExecutorType timeoutExecutor,
        boolean fallbackOnException,
        boolean cancelRunningTask,
        String fallbackMethod,
        String returnJson,
        FailStrategy failStrategy
) {
}
```

Create `AccessGuardRuleOverride` as a record with nullable boxed fields for method-level overrides.

- [ ] **Step 6: Run the annotation test**

Run:

```bash
./mvnw -B -ntp -pl egon-cola-components/egon-cola-component-access-guard/egon-cola-component-access-guard-starter -am -Dtest=AccessGuardAnnotationTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: `BUILD SUCCESS`.

- [ ] **Step 7: Commit**

```bash
git add egon-cola-components/egon-cola-component-access-guard/egon-cola-component-access-guard-starter/src/main/java egon-cola-components/egon-cola-component-access-guard/egon-cola-component-access-guard-starter/src/test/java
git commit -m "feat: add access guard annotation contracts"
```

## Task 3: Properties and Auto-Configuration

**Files:**
- Create: `egon-cola-components/egon-cola-component-access-guard/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/autoconfigure/AccessGuardProperties.java`
- Create: `egon-cola-components/egon-cola-component-access-guard/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/autoconfigure/AccessGuardAutoConfiguration.java`
- Create: `egon-cola-components/egon-cola-component-access-guard/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/config/AccessGuardConfigProvider.java`
- Create: `egon-cola-components/egon-cola-component-access-guard/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/config/DefaultAccessGuardConfigProvider.java`
- Create: `egon-cola-components/egon-cola-component-access-guard/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/event/AccessGuardEventPublisher.java`
- Create: `egon-cola-components/egon-cola-component-access-guard/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/event/AccessGuardEventListener.java`
- Create: `egon-cola-components/egon-cola-component-access-guard/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/event/LoggingAccessGuardEventListener.java`
- Create: `egon-cola-components/egon-cola-component-access-guard/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/event/NoopAccessGuardEventPublisher.java`
- Create: `src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
- Test: `egon-cola-components/egon-cola-component-access-guard/egon-cola-component-access-guard-starter/src/test/java/top/egon/cola/component/accessguard/autoconfigure/AccessGuardAutoConfigurationTest.java`

- [ ] **Step 1: Write the failing auto-configuration test**

Create `AccessGuardAutoConfigurationTest`:

```java
package top.egon.cola.component.accessguard.autoconfigure;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import top.egon.cola.component.accessguard.aop.AccessGuardAop;
import top.egon.cola.component.accessguard.config.AccessGuardConfigProvider;
import top.egon.cola.component.accessguard.event.AccessGuardEventPublisher;

import static org.assertj.core.api.Assertions.assertThat;

class AccessGuardAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(AccessGuardAutoConfiguration.class));

    @Test
    void shouldCreateCoreBeansWhenEnabled() {
        contextRunner.run(context -> assertThat(context)
                .hasSingleBean(AccessGuardProperties.class)
                .hasSingleBean(AccessGuardConfigProvider.class)
                .hasSingleBean(AccessGuardEventPublisher.class)
                .hasSingleBean(AccessGuardAop.class));
    }

    @Test
    void shouldNotCreateAopWhenDisabled() {
        contextRunner.withPropertyValues("egon.cola.component.access-guard.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(AccessGuardAop.class));
    }
}
```

- [ ] **Step 2: Run the failing auto-configuration test**

Run:

```bash
./mvnw -B -ntp -pl egon-cola-components/egon-cola-component-access-guard/egon-cola-component-access-guard-starter -am -Dtest=AccessGuardAutoConfigurationTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: fail because auto-configuration types do not exist.

- [ ] **Step 3: Add `AccessGuardProperties`**

Create `AccessGuardProperties` with prefix `egon.cola.component.access-guard`, `ignoreInvalidFields = true`, and nested classes:

```text
Aop
Redisson
WhiteList
RateLimiter
Blacklist
CircuitBreaker
ThreadPool
Dynamic
LocalFallback
Rule
RuleWhiteList
RuleRateLimiter
RuleCircuitBreaker
```

Use defaults from the spec:

```text
enabled=true
storage=redisson
keyPrefix=egon:access-guard
failStrategy=FAIL_OPEN
keyResolveFailureStrategy=USE_ALL
aop.order=-100
redisson.clientBeanName=redissonClient
redisson.autoCreateClient=false
whiteList.emptyListStrategy=DENY_ALL
whiteList.mode=GATEKEEPER
rateLimiter.defaultPermits=1
rateLimiter.defaultInterval=1
blacklist.defaultCount=0
circuitBreaker.defaultTimeout=350ms
circuitBreaker.executor=THREAD_POOL
```

- [ ] **Step 4: Add config provider and event interfaces**

Create `AccessGuardConfigProvider`:

```java
package top.egon.cola.component.accessguard.config;

import java.util.Optional;

public interface AccessGuardConfigProvider {

    Optional<AccessGuardRuleOverride> findMethodOverride(String ruleName, String methodSignature);

    Optional<AccessGuardRuleOverride> findGlobalOverride();
}
```

Create `DefaultAccessGuardConfigProvider` that returns `Optional.empty()` for both methods.

Create `AccessGuardEventPublisher` with method `void publish(AccessGuardEvent event)`, `AccessGuardEventListener` with `void onEvent(AccessGuardEvent event)`, and `NoopAccessGuardEventPublisher` that iterates listeners.

- [ ] **Step 5: Add a minimal AOP shell**

Create `AccessGuardAop` with constructor dependencies required later and a method-level pointcut that matches the standard and compatibility annotations. At this task it can call `proceed()` directly. The full flow is implemented in Task 9.

- [ ] **Step 6: Add auto-configuration**

Create `AccessGuardAutoConfiguration`:

```java
package top.egon.cola.component.accessguard.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import top.egon.cola.component.accessguard.aop.AccessGuardAop;
import top.egon.cola.component.accessguard.config.AccessGuardConfigProvider;
import top.egon.cola.component.accessguard.config.DefaultAccessGuardConfigProvider;
import top.egon.cola.component.accessguard.event.AccessGuardEventListener;
import top.egon.cola.component.accessguard.event.AccessGuardEventPublisher;
import top.egon.cola.component.accessguard.event.LoggingAccessGuardEventListener;
import top.egon.cola.component.accessguard.event.NoopAccessGuardEventPublisher;

import java.util.List;

@AutoConfiguration
@EnableConfigurationProperties(AccessGuardProperties.class)
@ConditionalOnProperty(prefix = "egon.cola.component.access-guard", name = "enabled", havingValue = "true", matchIfMissing = true)
public class AccessGuardAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public AccessGuardConfigProvider accessGuardConfigProvider() {
        return new DefaultAccessGuardConfigProvider();
    }

    @Bean
    @ConditionalOnMissingBean
    public LoggingAccessGuardEventListener loggingAccessGuardEventListener() {
        return new LoggingAccessGuardEventListener();
    }

    @Bean
    @ConditionalOnMissingBean
    public AccessGuardEventPublisher accessGuardEventPublisher(List<AccessGuardEventListener> listeners) {
        return new NoopAccessGuardEventPublisher(listeners);
    }

    @Bean
    @ConditionalOnMissingBean
    public AccessGuardAop accessGuardAop() {
        return new AccessGuardAop();
    }
}
```

Create `AutoConfiguration.imports`:

```text
top.egon.cola.component.accessguard.autoconfigure.AccessGuardAutoConfiguration
```

- [ ] **Step 7: Run the auto-configuration test**

Run:

```bash
./mvnw -B -ntp -pl egon-cola-components/egon-cola-component-access-guard/egon-cola-component-access-guard-starter -am -Dtest=AccessGuardAutoConfigurationTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: `BUILD SUCCESS`.

- [ ] **Step 8: Commit**

```bash
git add egon-cola-components/egon-cola-component-access-guard/egon-cola-component-access-guard-starter/src
git commit -m "feat: add access guard auto configuration"
```

## Task 4: Key Resolution, Method Resolution, and Redis Keys

**Files:**
- Create: `egon-cola-components/egon-cola-component-access-guard/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/key/AccessKeyResolver.java`
- Create: `egon-cola-components/egon-cola-component-access-guard/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/key/CompositeAccessKeyResolver.java`
- Create: `egon-cola-components/egon-cola-component-access-guard/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/key/DefaultAccessKeyResolver.java`
- Create: `egon-cola-components/egon-cola-component-access-guard/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/key/AccessGuardKeyGenerator.java`
- Create: `egon-cola-components/egon-cola-component-access-guard/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/support/AopMethodResolver.java`
- Create: `egon-cola-components/egon-cola-component-access-guard/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/support/MethodSignatureKey.java`
- Create: `egon-cola-components/egon-cola-component-access-guard/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/support/SensitiveValueHasher.java`
- Create: `egon-cola-components/egon-cola-component-access-guard/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/support/AccessGuardRedisKeys.java`
- Test: `egon-cola-components/egon-cola-component-access-guard/egon-cola-component-access-guard-starter/src/test/java/top/egon/cola/component/accessguard/key/DefaultAccessKeyResolverTest.java`
- Test: `egon-cola-components/egon-cola-component-access-guard/egon-cola-component-access-guard-starter/src/test/java/top/egon/cola/component/accessguard/support/AccessGuardRedisKeysTest.java`

- [ ] **Step 1: Write failing key resolver tests**

Create tests for:

```java
@Test
void shouldResolveSimpleParameterName()

@Test
void shouldResolveNestedField()

@Test
void shouldResolveAll()

@Test
void shouldRejectBlankKeyWhenConfigured()
```

Use a local record:

```java
record Request(User user) {}
record User(String id) {}
```

Assert that `key="user.id"` resolves `"u-001"` from `new Request(new User("u-001"))`.

- [ ] **Step 2: Run failing key resolver tests**

Run:

```bash
./mvnw -B -ntp -pl egon-cola-components/egon-cola-component-access-guard/egon-cola-component-access-guard-starter -am -Dtest=DefaultAccessKeyResolverTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: fail because resolver classes do not exist.

- [ ] **Step 3: Implement key resolution contracts**

Create `AccessKeyResolver`:

```java
package top.egon.cola.component.accessguard.key;

import org.aspectj.lang.ProceedingJoinPoint;
import top.egon.cola.component.accessguard.config.AccessGuardRule;

public interface AccessKeyResolver {

    AccessKeyResolution resolve(ProceedingJoinPoint joinPoint, AccessGuardRule rule);
}
```

Create `AccessKeyResolution` as a record:

```java
package top.egon.cola.component.accessguard.key;

public record AccessKeyResolution(String rawKey, String normalizedKey, String keyHash) {
}
```

`DefaultAccessKeyResolver` must:

1. return `all` for key `all`;
2. use `DefaultParameterNameDiscoverer` for method parameters;
3. use fields and JavaBean getters for object and nested field access;
4. support `header:` and `ip` using `RequestContextHolder`;
5. apply trim, empty-key strategy, and SHA-256 hash.

- [ ] **Step 4: Implement Redis key generation**

Create `AccessGuardRedisKeys` with methods:

```java
public String whiteList(String ruleName, String accessKeyHash)
public String limiter(String ruleName, String accessKeyHash)
public String blacklist(String ruleName, String accessKeyHash)
public String rejectCount(String ruleName, String accessKeyHash)
public String configVersion(String ruleName)
```

Format:

```text
{prefix}:{app}:{env}:{ruleName}:{accessKeyHash}:{type}
```

- [ ] **Step 5: Run key tests**

Run:

```bash
./mvnw -B -ntp -pl egon-cola-components/egon-cola-component-access-guard/egon-cola-component-access-guard-starter -am -Dtest=DefaultAccessKeyResolverTest,AccessGuardRedisKeysTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: `BUILD SUCCESS`.

- [ ] **Step 6: Commit**

```bash
git add egon-cola-components/egon-cola-component-access-guard/egon-cola-component-access-guard-starter/src
git commit -m "feat: add access guard key resolution"
```

## Task 5: Reject Response Invoker

**Files:**
- Create: `egon-cola-components/egon-cola-component-access-guard/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/reject/RejectResponseInvoker.java`
- Create: `egon-cola-components/egon-cola-component-access-guard/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/reject/ReflectionFallbackInvoker.java`
- Create: `egon-cola-components/egon-cola-component-access-guard/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/reject/JsonRejectResponseParser.java`
- Create: `egon-cola-components/egon-cola-component-access-guard/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/exception/AccessGuardRejectResponseException.java`
- Test: `egon-cola-components/egon-cola-component-access-guard/egon-cola-component-access-guard-starter/src/test/java/top/egon/cola/component/accessguard/reject/RejectResponseInvokerTest.java`

- [ ] **Step 1: Write failing reject response tests**

Create tests proving:

1. same-argument fallback is invoked;
2. fallback with `AccessGuardContext` is invoked;
3. no-argument fallback is invoked;
4. `returnJson` is parsed into the method return type.

Use a sample target:

```java
static class SampleService {
    String fallback(String userId) {
        return "fallback:" + userId;
    }

    String fallbackWithContext(String userId, AccessGuardContext context) {
        return context.ruleName() + ":" + userId;
    }

    String noArgFallback() {
        return "fallback";
    }
}
```

- [ ] **Step 2: Run failing reject response tests**

Run:

```bash
./mvnw -B -ntp -pl egon-cola-components/egon-cola-component-access-guard/egon-cola-component-access-guard-starter -am -Dtest=RejectResponseInvokerTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: fail because reject invoker classes do not exist.

- [ ] **Step 3: Implement `RejectResponseInvoker`**

Create:

```java
package top.egon.cola.component.accessguard.reject;

import org.aspectj.lang.ProceedingJoinPoint;
import top.egon.cola.component.accessguard.config.AccessGuardRule;
import top.egon.cola.component.accessguard.context.AccessGuardContext;

public interface RejectResponseInvoker {

    Object reject(ProceedingJoinPoint joinPoint, AccessGuardRule rule, AccessGuardContext context, Object[] args);
}
```

`ReflectionFallbackInvoker` performs lookup order:

1. same name and original parameter types;
2. same name, original parameters, and `AccessGuardContext`;
3. same name with no parameters;
4. `returnJson`;
5. default string reject body for `String` return types.

`JsonRejectResponseParser` uses Spring `ObjectMapper` and returns the raw configured string when return type is `String`.

- [ ] **Step 4: Run reject response tests**

Run:

```bash
./mvnw -B -ntp -pl egon-cola-components/egon-cola-component-access-guard/egon-cola-component-access-guard-starter -am -Dtest=RejectResponseInvokerTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: `BUILD SUCCESS`.

- [ ] **Step 5: Commit**

```bash
git add egon-cola-components/egon-cola-component-access-guard/egon-cola-component-access-guard-starter/src
git commit -m "feat: add access guard reject responses"
```

## Task 6: White List Service

**Files:**
- Create: `egon-cola-components/egon-cola-component-access-guard/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/whitelist/WhiteListRepository.java`
- Create: `egon-cola-components/egon-cola-component-access-guard/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/whitelist/WhiteListService.java`
- Create: `egon-cola-components/egon-cola-component-access-guard/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/whitelist/DefaultWhiteListService.java`
- Create: `egon-cola-components/egon-cola-component-access-guard/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/whitelist/RedissonWhiteListRepository.java`
- Test: `egon-cola-components/egon-cola-component-access-guard/egon-cola-component-access-guard-starter/src/test/java/top/egon/cola/component/accessguard/whitelist/DefaultWhiteListServiceTest.java`

- [ ] **Step 1: Write failing white list tests**

Create tests for:

1. matching annotation users;
2. rejecting when enabled and empty-list strategy is `DENY_ALL`;
3. passing when white list is disabled;
4. returning `BYPASS_GUARD` when configured.

- [ ] **Step 2: Run failing white list tests**

Run:

```bash
./mvnw -B -ntp -pl egon-cola-components/egon-cola-component-access-guard/egon-cola-component-access-guard-starter -am -Dtest=DefaultWhiteListServiceTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: fail because white list service classes do not exist.

- [ ] **Step 3: Implement white list contracts**

Create `WhiteListDecision` record:

```java
package top.egon.cola.component.accessguard.whitelist;

import top.egon.cola.component.accessguard.annotation.WhiteListMode;

public record WhiteListDecision(boolean passed, boolean bypassGuard, WhiteListMode mode, String reason) {

    public static WhiteListDecision pass(WhiteListMode mode) {
        return new WhiteListDecision(true, mode == WhiteListMode.BYPASS_GUARD, mode, "white list hit");
    }

    public static WhiteListDecision reject(String reason) {
        return new WhiteListDecision(false, false, WhiteListMode.GATEKEEPER, reason);
    }
}
```

`DefaultWhiteListService` checks sources in this order:

```text
rule.whiteListUsers
WhiteListRepository.contains(ruleName, accessKeyHash)
global properties defaultUsers
```

`RedissonWhiteListRepository` uses `RedissonClient.getSet(key)` and checks hashed access keys.

- [ ] **Step 4: Run white list tests**

Run:

```bash
./mvnw -B -ntp -pl egon-cola-components/egon-cola-component-access-guard/egon-cola-component-access-guard-starter -am -Dtest=DefaultWhiteListServiceTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: `BUILD SUCCESS`.

- [ ] **Step 5: Commit**

```bash
git add egon-cola-components/egon-cola-component-access-guard/egon-cola-component-access-guard-starter/src
git commit -m "feat: add access guard white list service"
```

## Task 7: Rate Limiter and Blacklist

**Files:**
- Create: `egon-cola-components/egon-cola-component-access-guard/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/ratelimiter/RateLimiterExecutor.java`
- Create: `egon-cola-components/egon-cola-component-access-guard/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/ratelimiter/RateLimiterDecision.java`
- Create: `egon-cola-components/egon-cola-component-access-guard/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/ratelimiter/RedissonRateLimiterExecutor.java`
- Create: `egon-cola-components/egon-cola-component-access-guard/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/ratelimiter/LocalRateLimiterExecutor.java`
- Create: `egon-cola-components/egon-cola-component-access-guard/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/blacklist/BlacklistService.java`
- Create: `egon-cola-components/egon-cola-component-access-guard/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/blacklist/BlacklistStatus.java`
- Create: `egon-cola-components/egon-cola-component-access-guard/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/blacklist/RedissonBlacklistService.java`
- Test: `egon-cola-components/egon-cola-component-access-guard/egon-cola-component-access-guard-starter/src/test/java/top/egon/cola/component/accessguard/ratelimiter/RateLimiterRuleConversionTest.java`
- Test: `egon-cola-components/egon-cola-component-access-guard/egon-cola-component-access-guard-starter/src/test/java/top/egon/cola/component/accessguard/ratelimiter/LocalRateLimiterExecutorTest.java`
- Test: `egon-cola-components/egon-cola-component-access-guard/egon-cola-component-access-guard-starter/src/test/java/top/egon/cola/component/accessguard/blacklist/RedissonBlacklistServiceTest.java`

- [ ] **Step 1: Write failing conversion and local limiter tests**

Create tests that prove:

1. `permitsPerSecond=0.5` converts to `permits=1`, `interval=2`, `SECONDS`;
2. local limiter allows the first request and rejects the second request when capacity is exhausted;
3. local limiter refills after the configured interval.

- [ ] **Step 2: Run failing limiter tests**

Run:

```bash
./mvnw -B -ntp -pl egon-cola-components/egon-cola-component-access-guard/egon-cola-component-access-guard-starter -am -Dtest=RateLimiterRuleConversionTest,LocalRateLimiterExecutorTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: fail because limiter classes do not exist.

- [ ] **Step 3: Implement limiter contracts**

Create `RateLimiterDecision`:

```java
package top.egon.cola.component.accessguard.ratelimiter;

public record RateLimiterDecision(boolean allowed, long remainingPermits, String reason) {

    public static RateLimiterDecision allow(long remainingPermits) {
        return new RateLimiterDecision(true, remainingPermits, "allowed");
    }

    public static RateLimiterDecision reject(String reason) {
        return new RateLimiterDecision(false, 0L, reason);
    }
}
```

Create `RateLimiterExecutor`:

```java
package top.egon.cola.component.accessguard.ratelimiter;

import top.egon.cola.component.accessguard.config.AccessGuardRule;
import top.egon.cola.component.accessguard.context.AccessGuardContext;

public interface RateLimiterExecutor {

    RateLimiterDecision tryAcquire(AccessGuardRule rule, AccessGuardContext context);
}
```

`RedissonRateLimiterExecutor`:

1. uses `RedissonClient.getRateLimiter(key)`;
2. calls `trySetRate(RateType.OVERALL, permits, interval, intervalUnit)` on first use or version change;
3. calls `tryAcquire()`;
4. returns `FAIL_OPEN`, `FAIL_CLOSED`, or delegates to local fallback on Redisson exceptions.

`LocalRateLimiterExecutor`:

1. uses a `ConcurrentHashMap<String, Bucket>`;
2. stores tokens and next refill time;
3. removes idle buckets after `local-fallback.expire-after-write`;
4. does not claim global consistency.

- [ ] **Step 4: Implement blacklist service**

`BlacklistService` methods:

```java
BlacklistStatus status(AccessGuardRule rule, AccessGuardContext context);

BlacklistStatus incrementRejectAndMaybeBlacklist(AccessGuardRule rule, AccessGuardContext context);

void remove(String ruleName, String accessKeyHash);
```

`RedissonBlacklistService`:

1. reads blacklist from `RBucket<BlacklistStatus>`;
2. increments reject count with `RAtomicLong`;
3. sets reject counter expiry to the configured window;
4. writes blacklist with TTL when threshold is reached;
5. refuses automatic blacklist for `all` unless enabled.

- [ ] **Step 5: Run limiter and blacklist tests**

Run:

```bash
./mvnw -B -ntp -pl egon-cola-components/egon-cola-component-access-guard/egon-cola-component-access-guard-starter -am -Dtest=RateLimiterRuleConversionTest,LocalRateLimiterExecutorTest,RedissonBlacklistServiceTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: `BUILD SUCCESS`.

- [ ] **Step 6: Commit**

```bash
git add egon-cola-components/egon-cola-component-access-guard/egon-cola-component-access-guard-starter/src
git commit -m "feat: add access guard limiting and blacklist"
```

## Task 8: Timeout Circuit Breaker Executor

**Files:**
- Create: `egon-cola-components/egon-cola-component-access-guard/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/circuitbreaker/TimeoutCircuitBreakerExecutor.java`
- Create: `egon-cola-components/egon-cola-component-access-guard/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/circuitbreaker/TimeoutTaskExecutorProvider.java`
- Create: `egon-cola-components/egon-cola-component-access-guard/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/circuitbreaker/ThreadPoolTimeoutCircuitBreakerExecutor.java`
- Create: `egon-cola-components/egon-cola-component-access-guard/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/circuitbreaker/VirtualThreadTimeoutCircuitBreakerExecutor.java`
- Create: `egon-cola-components/egon-cola-component-access-guard/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/circuitbreaker/TimeoutCircuitBreakerException.java`
- Test: `egon-cola-components/egon-cola-component-access-guard/egon-cola-component-access-guard-starter/src/test/java/top/egon/cola/component/accessguard/circuitbreaker/ThreadPoolTimeoutCircuitBreakerExecutorTest.java`

- [ ] **Step 1: Write failing timeout tests**

Create tests for:

1. returning the business result before timeout;
2. returning reject response after timeout;
3. returning reject response when executor rejects;
4. propagating business exceptions when `fallbackOnException=false`.

- [ ] **Step 2: Run failing timeout tests**

Run:

```bash
./mvnw -B -ntp -pl egon-cola-components/egon-cola-component-access-guard/egon-cola-component-access-guard-starter -am -Dtest=ThreadPoolTimeoutCircuitBreakerExecutorTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: fail because timeout executor classes do not exist.

- [ ] **Step 3: Implement timeout contracts**

Create:

```java
package top.egon.cola.component.accessguard.circuitbreaker;

import org.aspectj.lang.ProceedingJoinPoint;
import top.egon.cola.component.accessguard.config.AccessGuardRule;
import top.egon.cola.component.accessguard.context.AccessGuardContext;

public interface TimeoutCircuitBreakerExecutor {

    Object execute(ProceedingJoinPoint joinPoint, AccessGuardRule rule, AccessGuardContext context) throws Throwable;
}
```

`ThreadPoolTimeoutCircuitBreakerExecutor`:

1. submits `joinPoint.proceed()` to an executor;
2. waits for `rule.timeout()`;
3. calls `future.cancel(true)` when configured;
4. returns reject response on timeout or executor rejection;
5. propagates original business exceptions when configured.

`VirtualThreadTimeoutCircuitBreakerExecutor` uses `Executors.newVirtualThreadPerTaskExecutor()` when selected.

- [ ] **Step 4: Run timeout tests**

Run:

```bash
./mvnw -B -ntp -pl egon-cola-components/egon-cola-component-access-guard/egon-cola-component-access-guard-starter -am -Dtest=ThreadPoolTimeoutCircuitBreakerExecutorTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: `BUILD SUCCESS`.

- [ ] **Step 5: Commit**

```bash
git add egon-cola-components/egon-cola-component-access-guard/egon-cola-component-access-guard-starter/src
git commit -m "feat: add access guard timeout protection"
```

## Task 9: AOP Orchestration Flow

**Files:**
- Modify: `egon-cola-components/egon-cola-component-access-guard/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/aop/AccessGuardAop.java`
- Create: `egon-cola-components/egon-cola-component-access-guard/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/config/AccessGuardRuleResolver.java`
- Create: `egon-cola-components/egon-cola-component-access-guard/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/config/AccessGuardAnnotationResolver.java`
- Test: `egon-cola-components/egon-cola-component-access-guard/egon-cola-component-access-guard-starter/src/test/java/top/egon/cola/component/accessguard/aop/AccessGuardAopFlowTest.java`

- [ ] **Step 1: Write failing AOP flow tests**

Create tests proving:

1. non-white-list user returns reject response and business method is not called;
2. white-list hit with `GATEKEEPER` continues into rate limiter;
3. blacklist hit returns reject response before rate limiter;
4. rate limiter rejection increments blacklist service and returns reject response;
5. rate limiter pass executes timeout wrapper;
6. disabled global switch calls `proceed()` and does not call Redisson or timeout services.

Use fake services with counters so each assertion proves call order.

- [ ] **Step 2: Run failing AOP flow tests**

Run:

```bash
./mvnw -B -ntp -pl egon-cola-components/egon-cola-component-access-guard/egon-cola-component-access-guard-starter -am -Dtest=AccessGuardAopFlowTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: fail because current AOP shell proceeds directly.

- [ ] **Step 3: Implement annotation and rule resolver**

`AccessGuardAnnotationResolver` maps these annotations into `AccessGuardRule`:

```text
AccessGuard
WhiteListAccessInterceptor
RateLimiterAccessInterceptor
TimeoutCircuitBreaker
DoWhiteList
DoRateLimiter
DoHystrix
```

`AccessGuardRuleResolver` merges:

```text
dynamic method override
dynamic global override
properties rule
annotation rule
starter defaults
```

`DoRateLimiter` mapping:

```text
key=all
ruleName=method signature hash when name is absent
blacklist disabled
returnJson from annotation
```

`DoHystrix` mapping:

```text
timeoutValue milliseconds
returnJson from annotation
fallbackOnException=false
```

- [ ] **Step 4: Implement full AOP flow**

`AccessGuardAop` must execute this order:

```text
if global disabled -> proceed
resolve rule
resolve key
if white list rejects -> reject response
if white list bypasses -> proceed
if blacklist hit -> reject response
if rate limit rejects -> increment reject count and reject response
if timeout enabled -> timeout executor
else -> proceed
publish event for every decision
```

Business exceptions from `proceed()` must propagate unless timeout executor handles them with `fallbackOnException=true`.

- [ ] **Step 5: Run AOP flow tests**

Run:

```bash
./mvnw -B -ntp -pl egon-cola-components/egon-cola-component-access-guard/egon-cola-component-access-guard-starter -am -Dtest=AccessGuardAopFlowTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: `BUILD SUCCESS`.

- [ ] **Step 6: Run all starter tests**

Run:

```bash
./mvnw -B -ntp -pl egon-cola-components/egon-cola-component-access-guard/egon-cola-component-access-guard-starter -am test
```

Expected: `BUILD SUCCESS`.

- [ ] **Step 7: Commit**

```bash
git add egon-cola-components/egon-cola-component-access-guard/egon-cola-component-access-guard-starter/src
git commit -m "feat: orchestrate access guard aop flow"
```

## Task 10: Test Module Samples and README

**Files:**
- Create: `egon-cola-components/egon-cola-component-access-guard/egon-cola-component-access-guard-test/src/test/java/top/egon/cola/component/accessguard/test/AccessGuardSampleTest.java`
- Modify: `egon-cola-components/egon-cola-component-access-guard/README.md`

- [ ] **Step 1: Write sample tests**

Create `AccessGuardSampleTest` with nested services proving:

1. `@WhiteListAccessInterceptor` accepts a configured user;
2. `@DoWhiteList` rejects through `returnJson`;
3. `@RateLimiterAccessInterceptor` calls fallback on rejection;
4. `@DoRateLimiter` maps to method-level global limiting;
5. `@TimeoutCircuitBreaker` returns timeout fallback;
6. `@DoHystrix` returns `returnJson`;
7. combined white list + rate limiter + timeout executes in fixed order.

- [ ] **Step 2: Run failing sample tests**

Run:

```bash
./mvnw -B -ntp -pl egon-cola-components/egon-cola-component-access-guard/egon-cola-component-access-guard-test -am -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: fail until the sample context imports the starter auto-configuration and test beans.

- [ ] **Step 3: Wire sample context**

Use `ApplicationContextRunner` and `AutoConfigurations.of(AccessGuardAutoConfiguration.class)`. Register fake Redisson-related beans where needed so tests do not require a live Redis server.

- [ ] **Step 4: Expand README**

README must include:

1. starter dependency;
2. configuration prefix;
3. white list annotation sample;
4. `@DoWhiteList` compatibility sample;
5. rate limiter annotation sample;
6. `@DoRateLimiter` compatibility sample;
7. timeout annotation sample;
8. `@DoHystrix` compatibility sample;
9. combined flow order;
10. validation commands.

- [ ] **Step 5: Run test module tests**

Run:

```bash
./mvnw -B -ntp -pl egon-cola-components/egon-cola-component-access-guard/egon-cola-component-access-guard-test -am -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: `BUILD SUCCESS`.

- [ ] **Step 6: Commit**

```bash
git add egon-cola-components/egon-cola-component-access-guard
git commit -m "test: add access guard samples"
```

## Task 11: Final Validation and Dependency Boundary

**Files:**
- Modify only files under `egon-cola-components/egon-cola-component-access-guard`, `egon-cola-components/pom.xml`, and `egon-cola-components/egon-cola-components-bom/pom.xml` if validation reveals gaps.

- [ ] **Step 1: Run starter tests**

Run:

```bash
./mvnw -B -ntp -pl egon-cola-components/egon-cola-component-access-guard/egon-cola-component-access-guard-starter -am test
```

Expected: `BUILD SUCCESS`.

- [ ] **Step 2: Run component tests**

Run:

```bash
./mvnw -B -ntp -pl egon-cola-components/egon-cola-component-access-guard -am test
```

Expected: `BUILD SUCCESS`.

- [ ] **Step 3: Run components reactor tests**

Run:

```bash
./mvnw -B -ntp -f egon-cola-components/pom.xml test
```

Expected: `BUILD SUCCESS`.

- [ ] **Step 4: Verify BOM exports the starter only**

Run:

```bash
rg -n "egon-cola-component-access-guard" egon-cola-components/egon-cola-components-bom/pom.xml
```

Expected output contains exactly one access-guard artifact entry:

```text
egon-cola-component-access-guard-starter
```

- [ ] **Step 5: Verify no runtime admin or database dependency leaked into starter**

Run:

```bash
./mvnw -B -ntp -pl egon-cola-components/egon-cola-component-access-guard/egon-cola-component-access-guard-starter -am dependency:tree -Dincludes=org.flywaydb,org.postgresql,org.xerial,org.springframework.boot:spring-boot-starter-actuator
```

Expected: no access-guard starter dependency path includes Flyway, PostgreSQL, SQLite, or Actuator.

- [ ] **Step 6: Verify no stale rate-limiter component path remains**

Run:

```bash
rg -n "egon-cola-component-rate-limiter|component-rate-limiter" egon-cola-components docs/superpowers/plans docs/superpowers/specs
```

Expected: no matches except historical Git output is not part of this command.

- [ ] **Step 7: Run whitespace check**

Run:

```bash
git diff --check
```

Expected: no output and exit 0.

- [ ] **Step 8: Commit validation documentation if files changed during validation**

If README or tests were changed while closing validation gaps, commit:

```bash
git add egon-cola-components/egon-cola-component-access-guard egon-cola-components/pom.xml egon-cola-components/egon-cola-components-bom/pom.xml
git commit -m "chore: validate access guard component"
```

If no files changed, do not create an empty commit.

## Self-Review Checklist

- [ ] Spec coverage: Tasks cover module structure, annotations, compatibility annotations, AOP flow, white list, Redisson limiting, blacklist, timeout protection, reject responses, dynamic config provider, events/logs, metrics hook, README, samples, BOM, and validation.
- [ ] No admin module is created.
- [ ] No Flyway migration is created or edited.
- [ ] No browser or runtime startup is required.
- [ ] Every task starts with tests or a concrete verification command.
- [ ] Each commit is path-scoped.
- [ ] The final validation commands are Maven-based and repository-local.
