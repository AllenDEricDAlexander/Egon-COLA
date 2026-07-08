# Rule Engine Component Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add an Egon-COLA rule-engine Spring Boot Starter component that supports Java-assembled responsibility chains and rule trees with unified context, result, trace, listener, async, and auto-configuration contracts.

**Architecture:** The component uses Scheme A from the approved design: one business-facing starter module plus one test/sample module under `egon-cola-components`. Runtime contracts and implementations live in the starter under `top.egon.cola.component.ruleengine`; the test module proves real usage flows without starting long-running services. The starter stays independent from DB, Redis, DDC, DTP, admin, and UI dependencies.

**Tech Stack:** Java 21, Maven, Spring Boot 3.5.x auto-configuration, JUnit Jupiter, AssertJ, SLF4J, Egon-COLA common core/trace where useful.

---

## File Structure

Create these files:

```text
egon-cola-components/egon-cola-component-rule-engine/pom.xml
egon-cola-components/egon-cola-component-rule-engine/README.md
egon-cola-components/egon-cola-component-rule-engine/egon-cola-component-rule-engine-starter/pom.xml
egon-cola-components/egon-cola-component-rule-engine/egon-cola-component-rule-engine-starter/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
egon-cola-components/egon-cola-component-rule-engine/egon-cola-component-rule-engine-test/pom.xml
```

Starter source files:

```text
egon-cola-components/egon-cola-component-rule-engine/egon-cola-component-rule-engine-starter/src/main/java/top/egon/cola/component/ruleengine/autoconfigure/RuleEngineAutoConfiguration.java
egon-cola-components/egon-cola-component-rule-engine/egon-cola-component-rule-engine-starter/src/main/java/top/egon/cola/component/ruleengine/autoconfigure/RuleEngineProperties.java
egon-cola-components/egon-cola-component-rule-engine/egon-cola-component-rule-engine-starter/src/main/java/top/egon/cola/component/ruleengine/async/DefaultRuleAsyncExecutor.java
egon-cola-components/egon-cola-component-rule-engine/egon-cola-component-rule-engine-starter/src/main/java/top/egon/cola/component/ruleengine/async/RuleAsyncExecutor.java
egon-cola-components/egon-cola-component-rule-engine/egon-cola-component-rule-engine-starter/src/main/java/top/egon/cola/component/ruleengine/chain/AbstractSingletonRuleLink.java
egon-cola-components/egon-cola-component-rule-engine/egon-cola-component-rule-engine-starter/src/main/java/top/egon/cola/component/ruleengine/chain/ChainHandler.java
egon-cola-components/egon-cola-component-rule-engine/egon-cola-component-rule-engine-starter/src/main/java/top/egon/cola/component/ruleengine/chain/RuleChain.java
egon-cola-components/egon-cola-component-rule-engine/egon-cola-component-rule-engine-starter/src/main/java/top/egon/cola/component/ruleengine/chain/SingletonRuleLink.java
egon-cola-components/egon-cola-component-rule-engine/egon-cola-component-rule-engine-starter/src/main/java/top/egon/cola/component/ruleengine/context/RuleContext.java
egon-cola-components/egon-cola-component-rule-engine/egon-cola-component-rule-engine-starter/src/main/java/top/egon/cola/component/ruleengine/engine/DefaultRuleChainExecutor.java
egon-cola-components/egon-cola-component-rule-engine/egon-cola-component-rule-engine-starter/src/main/java/top/egon/cola/component/ruleengine/engine/DefaultRuleEngine.java
egon-cola-components/egon-cola-component-rule-engine/egon-cola-component-rule-engine-starter/src/main/java/top/egon/cola/component/ruleengine/engine/DefaultRuleTreeExecutor.java
egon-cola-components/egon-cola-component-rule-engine/egon-cola-component-rule-engine-starter/src/main/java/top/egon/cola/component/ruleengine/engine/RuleChainExecutor.java
egon-cola-components/egon-cola-component-rule-engine/egon-cola-component-rule-engine-starter/src/main/java/top/egon/cola/component/ruleengine/engine/RuleEngine.java
egon-cola-components/egon-cola-component-rule-engine/egon-cola-component-rule-engine-starter/src/main/java/top/egon/cola/component/ruleengine/engine/RuleTreeExecutor.java
egon-cola-components/egon-cola-component-rule-engine/egon-cola-component-rule-engine-starter/src/main/java/top/egon/cola/component/ruleengine/exception/RuleConfigException.java
egon-cola-components/egon-cola-component-rule-engine/egon-cola-component-rule-engine-starter/src/main/java/top/egon/cola/component/ruleengine/exception/RuleEmptyChainException.java
egon-cola-components/egon-cola-component-rule-engine/egon-cola-component-rule-engine-starter/src/main/java/top/egon/cola/component/ruleengine/exception/RuleEmptyTreeException.java
egon-cola-components/egon-cola-component-rule-engine/egon-cola-component-rule-engine-starter/src/main/java/top/egon/cola/component/ruleengine/exception/RuleEngineException.java
egon-cola-components/egon-cola-component-rule-engine/egon-cola-component-rule-engine-starter/src/main/java/top/egon/cola/component/ruleengine/exception/RuleMaxStepsExceededException.java
egon-cola-components/egon-cola-component-rule-engine/egon-cola-component-rule-engine-starter/src/main/java/top/egon/cola/component/ruleengine/exception/RuleNodeException.java
egon-cola-components/egon-cola-component-rule-engine/egon-cola-component-rule-engine-starter/src/main/java/top/egon/cola/component/ruleengine/exception/RuleRouteException.java
egon-cola-components/egon-cola-component-rule-engine/egon-cola-component-rule-engine-starter/src/main/java/top/egon/cola/component/ruleengine/exception/RuleTimeoutException.java
egon-cola-components/egon-cola-component-rule-engine/egon-cola-component-rule-engine-starter/src/main/java/top/egon/cola/component/ruleengine/listener/LoggingRuleExecutionListener.java
egon-cola-components/egon-cola-component-rule-engine/egon-cola-component-rule-engine-starter/src/main/java/top/egon/cola/component/ruleengine/listener/RuleExecutionListener.java
egon-cola-components/egon-cola-component-rule-engine/egon-cola-component-rule-engine-starter/src/main/java/top/egon/cola/component/ruleengine/listener/RuleExecutionListenerComposite.java
egon-cola-components/egon-cola-component-rule-engine/egon-cola-component-rule-engine-starter/src/main/java/top/egon/cola/component/ruleengine/result/RuleResult.java
egon-cola-components/egon-cola-component-rule-engine/egon-cola-component-rule-engine-starter/src/main/java/top/egon/cola/component/ruleengine/result/RuleStatus.java
egon-cola-components/egon-cola-component-rule-engine/egon-cola-component-rule-engine-starter/src/main/java/top/egon/cola/component/ruleengine/trace/NodeTrace.java
egon-cola-components/egon-cola-component-rule-engine/egon-cola-component-rule-engine-starter/src/main/java/top/egon/cola/component/ruleengine/trace/RuleTrace.java
egon-cola-components/egon-cola-component-rule-engine/egon-cola-component-rule-engine-starter/src/main/java/top/egon/cola/component/ruleengine/trace/RuleTraceRecorder.java
egon-cola-components/egon-cola-component-rule-engine/egon-cola-component-rule-engine-starter/src/main/java/top/egon/cola/component/ruleengine/tree/AbstractAsyncRuleNode.java
egon-cola-components/egon-cola-component-rule-engine/egon-cola-component-rule-engine-starter/src/main/java/top/egon/cola/component/ruleengine/tree/AbstractRuleNode.java
egon-cola-components/egon-cola-component-rule-engine/egon-cola-component-rule-engine-starter/src/main/java/top/egon/cola/component/ruleengine/tree/NodeType.java
egon-cola-components/egon-cola-component-rule-engine/egon-cola-component-rule-engine-starter/src/main/java/top/egon/cola/component/ruleengine/tree/RouteDecision.java
egon-cola-components/egon-cola-component-rule-engine/egon-cola-component-rule-engine-starter/src/main/java/top/egon/cola/component/ruleengine/tree/RuleNode.java
egon-cola-components/egon-cola-component-rule-engine/egon-cola-component-rule-engine-starter/src/main/java/top/egon/cola/component/ruleengine/tree/RuleRouter.java
egon-cola-components/egon-cola-component-rule-engine/egon-cola-component-rule-engine-starter/src/main/java/top/egon/cola/component/ruleengine/tree/RuleTree.java
```

Starter tests:

```text
egon-cola-components/egon-cola-component-rule-engine/egon-cola-component-rule-engine-starter/src/test/java/top/egon/cola/component/ruleengine/autoconfigure/RuleEngineAutoConfigurationTest.java
egon-cola-components/egon-cola-component-rule-engine/egon-cola-component-rule-engine-starter/src/test/java/top/egon/cola/component/ruleengine/async/DefaultRuleAsyncExecutorTest.java
egon-cola-components/egon-cola-component-rule-engine/egon-cola-component-rule-engine-starter/src/test/java/top/egon/cola/component/ruleengine/chain/DefaultRuleChainExecutorTest.java
egon-cola-components/egon-cola-component-rule-engine/egon-cola-component-rule-engine-starter/src/test/java/top/egon/cola/component/ruleengine/context/RuleContextTest.java
egon-cola-components/egon-cola-component-rule-engine/egon-cola-component-rule-engine-starter/src/test/java/top/egon/cola/component/ruleengine/engine/DefaultRuleEngineTest.java
egon-cola-components/egon-cola-component-rule-engine/egon-cola-component-rule-engine-starter/src/test/java/top/egon/cola/component/ruleengine/listener/RuleExecutionListenerCompositeTest.java
egon-cola-components/egon-cola-component-rule-engine/egon-cola-component-rule-engine-starter/src/test/java/top/egon/cola/component/ruleengine/result/RuleResultTest.java
egon-cola-components/egon-cola-component-rule-engine/egon-cola-component-rule-engine-starter/src/test/java/top/egon/cola/component/ruleengine/tree/DefaultRuleTreeExecutorTest.java
```

Test module sample tests:

```text
egon-cola-components/egon-cola-component-rule-engine/egon-cola-component-rule-engine-test/src/test/java/top/egon/cola/component/ruleengine/test/RuleEngineAutoConfigurationSampleTest.java
egon-cola-components/egon-cola-component-rule-engine/egon-cola-component-rule-engine-test/src/test/java/top/egon/cola/component/ruleengine/test/RuleEngineOrderChainSampleTest.java
egon-cola-components/egon-cola-component-rule-engine/egon-cola-component-rule-engine-test/src/test/java/top/egon/cola/component/ruleengine/test/RuleEngineLoginSingletonChainSampleTest.java
egon-cola-components/egon-cola-component-rule-engine/egon-cola-component-rule-engine-test/src/test/java/top/egon/cola/component/ruleengine/test/RuleEngineMemberBenefitTreeSampleTest.java
```

Modify these existing files:

```text
egon-cola-components/pom.xml
egon-cola-components/egon-cola-components-bom/pom.xml
```

## Task 1: Add Maven Module Skeleton

**Files:**
- Modify: `egon-cola-components/pom.xml`
- Modify: `egon-cola-components/egon-cola-components-bom/pom.xml`
- Create: `egon-cola-components/egon-cola-component-rule-engine/pom.xml`
- Create: `egon-cola-components/egon-cola-component-rule-engine/egon-cola-component-rule-engine-starter/pom.xml`
- Create: `egon-cola-components/egon-cola-component-rule-engine/egon-cola-component-rule-engine-test/pom.xml`

- [ ] **Step 1: Add component module to the components parent**

Add this module entry after `egon-cola-component-dynamic-config-center` in `egon-cola-components/pom.xml`:

```xml
<module>egon-cola-component-rule-engine</module>
```

- [ ] **Step 2: Create the component root POM**

Create `egon-cola-components/egon-cola-component-rule-engine/pom.xml` with:

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

    <artifactId>egon-cola-component-rule-engine</artifactId>
    <packaging>pom</packaging>
    <name>egon-cola-component-rule-engine</name>
    <description>Rule engine component for Egon COLA.</description>

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
                <artifactId>egon-cola-component-rule-engine-starter</artifactId>
                <version>${project.version}</version>
            </dependency>
        </dependencies>
    </dependencyManagement>

    <modules>
        <module>egon-cola-component-rule-engine-starter</module>
        <module>egon-cola-component-rule-engine-test</module>
    </modules>
</project>
```

- [ ] **Step 3: Create the starter POM**

Create `egon-cola-components/egon-cola-component-rule-engine/egon-cola-component-rule-engine-starter/pom.xml` with:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>top.egon</groupId>
        <artifactId>egon-cola-component-rule-engine</artifactId>
        <version>5.2.0-SNAPSHOT</version>
        <relativePath>../pom.xml</relativePath>
    </parent>

    <artifactId>egon-cola-component-rule-engine-starter</artifactId>
    <packaging>jar</packaging>
    <name>egon-cola-component-rule-engine-starter</name>
    <description>Spring Boot starter for Egon COLA rule engine.</description>

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

Create `egon-cola-components/egon-cola-component-rule-engine/egon-cola-component-rule-engine-test/pom.xml` with:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>top.egon</groupId>
        <artifactId>egon-cola-component-rule-engine</artifactId>
        <version>5.2.0-SNAPSHOT</version>
        <relativePath>../pom.xml</relativePath>
    </parent>

    <artifactId>egon-cola-component-rule-engine-test</artifactId>
    <packaging>jar</packaging>
    <name>egon-cola-component-rule-engine-test</name>
    <description>Sample and validation module for Egon COLA rule engine.</description>

    <dependencies>
        <dependency>
            <groupId>top.egon</groupId>
            <artifactId>egon-cola-component-rule-engine-starter</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>
</project>
```

- [ ] **Step 5: Add starter to the components BOM**

Add this dependency inside `egon-cola-components/egon-cola-components-bom/pom.xml` dependency management after the dynamic-config-center starter:

```xml
<dependency>
    <groupId>top.egon</groupId>
    <artifactId>egon-cola-component-rule-engine-starter</artifactId>
    <version>${project.version}</version>
</dependency>
```

- [ ] **Step 6: Run Maven module discovery**

Run:

```bash
./mvnw -B -ntp -pl egon-cola-components/egon-cola-component-rule-engine -am validate
```

Expected: Maven resolves the new module reactor and exits with code 0.

- [ ] **Step 7: Commit module skeleton**

```bash
git add egon-cola-components/pom.xml \
  egon-cola-components/egon-cola-components-bom/pom.xml \
  egon-cola-components/egon-cola-component-rule-engine/pom.xml \
  egon-cola-components/egon-cola-component-rule-engine/egon-cola-component-rule-engine-starter/pom.xml \
  egon-cola-components/egon-cola-component-rule-engine/egon-cola-component-rule-engine-test/pom.xml
git commit -m "build: add rule engine component modules"
```

## Task 2: Add Core Contracts

**Files:**
- Create: `egon-cola-components/egon-cola-component-rule-engine/egon-cola-component-rule-engine-starter/src/test/java/top/egon/cola/component/ruleengine/context/RuleContextTest.java`
- Create: `egon-cola-components/egon-cola-component-rule-engine/egon-cola-component-rule-engine-starter/src/test/java/top/egon/cola/component/ruleengine/result/RuleResultTest.java`
- Create: `egon-cola-components/egon-cola-component-rule-engine/egon-cola-component-rule-engine-starter/src/main/java/top/egon/cola/component/ruleengine/context/RuleContext.java`
- Create: `egon-cola-components/egon-cola-component-rule-engine/egon-cola-component-rule-engine-starter/src/main/java/top/egon/cola/component/ruleengine/result/RuleResult.java`
- Create: `egon-cola-components/egon-cola-component-rule-engine/egon-cola-component-rule-engine-starter/src/main/java/top/egon/cola/component/ruleengine/result/RuleStatus.java`
- Create: all exception files listed in File Structure
- Create: `NodeTrace.java`, `RuleTrace.java`, `RuleTraceRecorder.java`

- [ ] **Step 1: Write failing tests for context and result contracts**

Create `RuleContextTest.java`:

```java
package top.egon.cola.component.ruleengine.context;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class RuleContextTest {

    @Test
    void shouldReadWriteAttributesAndControlFlow() {
        RuleContext context = RuleContext.create("req-1", "trace-1")
                .maxSteps(2)
                .timeout(Duration.ofMillis(500));

        context.set("amount", 100);

        assertThat(context.get("amount", Integer.class)).isEqualTo(100);
        assertThat(context.contains("amount")).isTrue();
        assertThat(context.isProceed()).isTrue();

        context.incrementStep();
        context.incrementStep();
        context.incrementStep();

        assertThat(context.isExceededMaxSteps()).isTrue();

        context.stop();

        assertThat(context.isStopped()).isTrue();
        assertThat(context.isProceed()).isFalse();
    }
}
```

Create `RuleResultTest.java`:

```java
package top.egon.cola.component.ruleengine.result;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RuleResultTest {

    @Test
    void shouldBuildSuccessStopAndFailureResultsWithIntCode() {
        RuleResult<String> success = RuleResult.success("ok");
        RuleResult<String> stopped = RuleResult.stop(600100, "blocked", "reject");
        IllegalStateException exception = new IllegalStateException("boom");
        RuleResult<String> failed = RuleResult.fail(500100, "failed", exception);

        assertThat(success.isSuccess()).isTrue();
        assertThat(success.getStatus()).isEqualTo(RuleStatus.SUCCESS);
        assertThat(success.getCode()).isZero();
        assertThat(success.getData()).isEqualTo("ok");

        assertThat(stopped.isSuccess()).isFalse();
        assertThat(stopped.getStatus()).isEqualTo(RuleStatus.STOPPED);
        assertThat(stopped.getCode()).isEqualTo(600100);
        assertThat(stopped.getData()).isEqualTo("reject");

        assertThat(failed.isSuccess()).isFalse();
        assertThat(failed.getStatus()).isEqualTo(RuleStatus.FAILED);
        assertThat(failed.getException()).isSameAs(exception);
    }
}
```

- [ ] **Step 2: Run tests and verify they fail**

Run:

```bash
./mvnw -B -ntp -pl egon-cola-components/egon-cola-component-rule-engine/egon-cola-component-rule-engine-starter -am -Dtest=RuleContextTest,RuleResultTest test
```

Expected: compilation fails because `RuleContext`, `RuleResult`, and `RuleStatus` do not exist.

- [ ] **Step 3: Implement `RuleStatus`**

Create `RuleStatus.java`:

```java
package top.egon.cola.component.ruleengine.result;

public enum RuleStatus {

    SUCCESS(0, "success"),
    STOPPED(600100, "rule stopped"),
    FAILED(500100, "rule failed"),
    TIMEOUT(500101, "rule execution timeout"),
    MAX_STEPS_EXCEEDED(500102, "rule max steps exceeded"),
    NO_ROUTE(500103, "rule tree no route"),
    EMPTY_CHAIN(500104, "rule chain is empty"),
    EMPTY_TREE(500105, "rule tree is empty"),
    NODE_ERROR(500106, "rule node error"),
    CONFIG_ERROR(500107, "rule config error");

    private final int code;

    private final String message;

    RuleStatus(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
```

- [ ] **Step 4: Implement `RuleResult`**

Create `RuleResult.java` with JavaBean getters so Spring/Jackson consumers have a stable contract:

```java
package top.egon.cola.component.ruleengine.result;

import top.egon.cola.component.ruleengine.trace.RuleTrace;

import java.io.Serial;
import java.io.Serializable;

public class RuleResult<R> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final boolean success;
    private final RuleStatus status;
    private final int code;
    private final String message;
    private final R data;
    private final RuleTrace trace;
    private final Throwable exception;
    private final String stoppedNode;
    private final String hitNode;
    private final long costMillis;

    private RuleResult(boolean success, RuleStatus status, int code, String message, R data,
                       RuleTrace trace, Throwable exception, String stoppedNode, String hitNode,
                       long costMillis) {
        this.success = success;
        this.status = status;
        this.code = code;
        this.message = message;
        this.data = data;
        this.trace = trace;
        this.exception = exception;
        this.stoppedNode = stoppedNode;
        this.hitNode = hitNode;
        this.costMillis = costMillis;
    }

    public static <R> RuleResult<R> success(R data) {
        return new RuleResult<>(true, RuleStatus.SUCCESS, RuleStatus.SUCCESS.getCode(),
                RuleStatus.SUCCESS.getMessage(), data, null, null, null, null, 0L);
    }

    public static <R> RuleResult<R> stop(int code, String message, R data) {
        return new RuleResult<>(false, RuleStatus.STOPPED, code, message, data, null, null, null, null, 0L);
    }

    public static <R> RuleResult<R> fail(int code, String message, Throwable exception) {
        return new RuleResult<>(false, RuleStatus.FAILED, code, message, null, null, exception, null, null, 0L);
    }

    public static <R> RuleResult<R> failure(RuleStatus status, String message, Throwable exception) {
        return new RuleResult<>(false, status, status.getCode(), message, null, null, exception, null, null, 0L);
    }

    public static <R> RuleResult<R> timeout(String message) {
        return new RuleResult<>(false, RuleStatus.TIMEOUT, RuleStatus.TIMEOUT.getCode(), message, null, null, null, null, null, 0L);
    }

    public static <R> RuleResult<R> maxStepsExceeded(String message) {
        return new RuleResult<>(false, RuleStatus.MAX_STEPS_EXCEEDED, RuleStatus.MAX_STEPS_EXCEEDED.getCode(), message, null, null, null, null, null, 0L);
    }

    public static <R> RuleResult<R> noRoute(String message) {
        return new RuleResult<>(false, RuleStatus.NO_ROUTE, RuleStatus.NO_ROUTE.getCode(), message, null, null, null, null, null, 0L);
    }

    public RuleResult<R> withTrace(RuleTrace trace) {
        return new RuleResult<>(success, status, code, message, data, trace, exception, stoppedNode, hitNode, costMillis);
    }

    public RuleResult<R> withStoppedNode(String stoppedNode) {
        return new RuleResult<>(success, status, code, message, data, trace, exception, stoppedNode, hitNode, costMillis);
    }

    public RuleResult<R> withHitNode(String hitNode) {
        return new RuleResult<>(success, status, code, message, data, trace, exception, stoppedNode, hitNode, costMillis);
    }

    public RuleResult<R> withCostMillis(long costMillis) {
        return new RuleResult<>(success, status, code, message, data, trace, exception, stoppedNode, hitNode, costMillis);
    }

    public boolean isSuccess() {
        return success;
    }

    public RuleStatus getStatus() {
        return status;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public R getData() {
        return data;
    }

    public RuleTrace getTrace() {
        return trace;
    }

    public Throwable getException() {
        return exception;
    }

    public String getStoppedNode() {
        return stoppedNode;
    }

    public String getHitNode() {
        return hitNode;
    }

    public long getCostMillis() {
        return costMillis;
    }
}
```

- [ ] **Step 5: Implement `RuleContext`**

Create `RuleContext.java`:

```java
package top.egon.cola.component.ruleengine.context;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class RuleContext {

    private final String requestId;
    private final String traceId;
    private final Instant startTime;
    private final Map<String, Object> attributes = new ConcurrentHashMap<>();
    private final List<String> executionPath = new CopyOnWriteArrayList<>();
    private final List<Throwable> errors = new CopyOnWriteArrayList<>();
    private final AtomicInteger stepCount = new AtomicInteger();
    private volatile boolean proceed = true;
    private volatile boolean stopped;
    private volatile String currentNode;
    private volatile String previousNode;
    private volatile int maxSteps = 100;
    private volatile Instant deadline;

    private RuleContext(String requestId, String traceId) {
        this.requestId = normalize(requestId, "req-");
        this.traceId = normalize(traceId, "trace-");
        this.startTime = Instant.now();
    }

    public static RuleContext create() {
        return new RuleContext(null, null);
    }

    public static RuleContext create(String requestId, String traceId) {
        return new RuleContext(requestId, traceId);
    }

    public RuleContext maxSteps(int maxSteps) {
        this.maxSteps = maxSteps;
        return this;
    }

    public RuleContext timeout(Duration timeout) {
        this.deadline = timeout == null ? null : Instant.now().plus(timeout);
        return this;
    }

    public RuleContext set(String key, Object value) {
        if (value == null) {
            attributes.remove(key);
            return this;
        }
        attributes.put(key, value);
        return this;
    }

    public Object get(String key) {
        return attributes.get(key);
    }

    public <T> T get(String key, Class<T> type) {
        Object value = attributes.get(key);
        return type.isInstance(value) ? type.cast(value) : null;
    }

    public boolean contains(String key) {
        return attributes.containsKey(key);
    }

    public Object remove(String key) {
        return attributes.remove(key);
    }

    public void proceed() {
        this.proceed = true;
        this.stopped = false;
    }

    public void stop() {
        this.proceed = false;
        this.stopped = true;
    }

    public int incrementStep() {
        return stepCount.incrementAndGet();
    }

    public boolean isExceededMaxSteps() {
        return stepCount.get() > maxSteps;
    }

    public boolean isTimeout() {
        return deadline != null && !Instant.now().isBefore(deadline);
    }

    public void enterNode(String nodeCode) {
        this.previousNode = currentNode;
        this.currentNode = nodeCode;
        this.executionPath.add(nodeCode);
    }

    public void addError(Throwable throwable) {
        if (throwable != null) {
            errors.add(throwable);
        }
    }

    public String getRequestId() {
        return requestId;
    }

    public String getTraceId() {
        return traceId;
    }

    public boolean isProceed() {
        return proceed;
    }

    public boolean isStopped() {
        return stopped;
    }

    public String getCurrentNode() {
        return currentNode;
    }

    public String getPreviousNode() {
        return previousNode;
    }

    public List<String> getExecutionPath() {
        return Collections.unmodifiableList(new ArrayList<>(executionPath));
    }

    public int getStepCount() {
        return stepCount.get();
    }

    public int getMaxSteps() {
        return maxSteps;
    }

    public Instant getStartTime() {
        return startTime;
    }

    public Instant getDeadline() {
        return deadline;
    }

    public List<Throwable> getErrors() {
        return Collections.unmodifiableList(new ArrayList<>(errors));
    }

    private static String normalize(String value, String prefix) {
        if (value != null && !value.isBlank()) {
            return value;
        }
        return prefix + UUID.randomUUID();
    }
}
```

- [ ] **Step 6: Implement trace records**

Create `NodeTrace.java`:

```java
package top.egon.cola.component.ruleengine.trace;

import top.egon.cola.component.ruleengine.result.RuleStatus;
import top.egon.cola.component.ruleengine.tree.NodeType;

import java.time.Instant;

public record NodeTrace(
        String nodeCode,
        String nodeName,
        NodeType nodeType,
        int order,
        int visitCount,
        Instant startTime,
        Instant endTime,
        long costMillis,
        String routeTo,
        String routeReason,
        RuleStatus status,
        String error
) {
}
```

Create `RuleTrace.java`:

```java
package top.egon.cola.component.ruleengine.trace;

import top.egon.cola.component.ruleengine.result.RuleStatus;

import java.time.Instant;
import java.util.List;

public record RuleTrace(
        String ruleCode,
        String ruleName,
        String modelType,
        String requestId,
        String traceId,
        Instant startTime,
        Instant endTime,
        long costMillis,
        RuleStatus status,
        List<NodeTrace> nodeTraces,
        String error
) {
}
```

Create `RuleTraceRecorder.java`:

```java
package top.egon.cola.component.ruleengine.trace;

import top.egon.cola.component.ruleengine.context.RuleContext;
import top.egon.cola.component.ruleengine.result.RuleStatus;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class RuleTraceRecorder {

    private final boolean enabled;
    private final List<NodeTrace> nodeTraces = new ArrayList<>();

    public RuleTraceRecorder(boolean enabled) {
        this.enabled = enabled;
    }

    public void addNodeTrace(NodeTrace nodeTrace) {
        if (enabled && nodeTrace != null) {
            nodeTraces.add(nodeTrace);
        }
    }

    public RuleTrace finish(String ruleCode, String ruleName, String modelType, RuleContext context,
                            RuleStatus status, Throwable error) {
        Instant endTime = Instant.now();
        String errorMessage = error == null ? null : error.getMessage();
        return new RuleTrace(ruleCode, ruleName, modelType, context.getRequestId(), context.getTraceId(),
                context.getStartTime(), endTime, Duration.between(context.getStartTime(), endTime).toMillis(),
                status, enabled ? List.copyOf(nodeTraces) : List.of(), errorMessage);
    }
}
```

- [ ] **Step 7: Implement exception hierarchy**

Create `RuleEngineException.java`:

```java
package top.egon.cola.component.ruleengine.exception;

import java.io.Serial;

public class RuleEngineException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public RuleEngineException(String message) {
        super(message);
    }

    public RuleEngineException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

Create `RuleConfigException.java`:

```java
package top.egon.cola.component.ruleengine.exception;

import java.io.Serial;

public class RuleConfigException extends RuleEngineException {

    @Serial
    private static final long serialVersionUID = 1L;

    public RuleConfigException(String message) {
        super(message);
    }

    public RuleConfigException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

Create `RuleEmptyChainException.java`:

```java
package top.egon.cola.component.ruleengine.exception;

import java.io.Serial;

public class RuleEmptyChainException extends RuleEngineException {

    @Serial
    private static final long serialVersionUID = 1L;

    public RuleEmptyChainException(String message) {
        super(message);
    }

    public RuleEmptyChainException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

Create `RuleEmptyTreeException.java`:

```java
package top.egon.cola.component.ruleengine.exception;

import java.io.Serial;

public class RuleEmptyTreeException extends RuleEngineException {

    @Serial
    private static final long serialVersionUID = 1L;

    public RuleEmptyTreeException(String message) {
        super(message);
    }

    public RuleEmptyTreeException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

Create `RuleMaxStepsExceededException.java`:

```java
package top.egon.cola.component.ruleengine.exception;

import java.io.Serial;

public class RuleMaxStepsExceededException extends RuleEngineException {

    @Serial
    private static final long serialVersionUID = 1L;

    public RuleMaxStepsExceededException(String message) {
        super(message);
    }

    public RuleMaxStepsExceededException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

Create `RuleNodeException.java`:

```java
package top.egon.cola.component.ruleengine.exception;

import java.io.Serial;

public class RuleNodeException extends RuleEngineException {

    @Serial
    private static final long serialVersionUID = 1L;

    public RuleNodeException(String message) {
        super(message);
    }

    public RuleNodeException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

Create `RuleRouteException.java`:

```java
package top.egon.cola.component.ruleengine.exception;

import java.io.Serial;

public class RuleRouteException extends RuleEngineException {

    @Serial
    private static final long serialVersionUID = 1L;

    public RuleRouteException(String message) {
        super(message);
    }

    public RuleRouteException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

Create `RuleTimeoutException.java`:

```java
package top.egon.cola.component.ruleengine.exception;

import java.io.Serial;

public class RuleTimeoutException extends RuleEngineException {

    @Serial
    private static final long serialVersionUID = 1L;

    public RuleTimeoutException(String message) {
        super(message);
    }

    public RuleTimeoutException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

- [ ] **Step 8: Run core contract tests**

Run:

```bash
./mvnw -B -ntp -pl egon-cola-components/egon-cola-component-rule-engine/egon-cola-component-rule-engine-starter -am -Dtest=RuleContextTest,RuleResultTest test
```

Expected: both tests pass.

- [ ] **Step 9: Commit core contracts**

```bash
git add egon-cola-components/egon-cola-component-rule-engine/egon-cola-component-rule-engine-starter/src/main/java \
  egon-cola-components/egon-cola-component-rule-engine/egon-cola-component-rule-engine-starter/src/test/java
git commit -m "feat: add rule engine core contracts"
```

## Task 3: Add Responsibility Chain Execution

**Files:**
- Create: `DefaultRuleChainExecutorTest.java`
- Create: `ChainHandler.java`
- Create: `RuleChain.java`
- Create: `SingletonRuleLink.java`
- Create: `AbstractSingletonRuleLink.java`
- Create: `RuleChainExecutor.java`
- Create: `DefaultRuleChainExecutor.java`

- [ ] **Step 1: Write failing chain executor tests**

Create `DefaultRuleChainExecutorTest.java`:

```java
package top.egon.cola.component.ruleengine.chain;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.ruleengine.context.RuleContext;
import top.egon.cola.component.ruleengine.engine.DefaultRuleChainExecutor;
import top.egon.cola.component.ruleengine.result.RuleResult;
import top.egon.cola.component.ruleengine.result.RuleStatus;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultRuleChainExecutorTest {

    private final DefaultRuleChainExecutor executor = new DefaultRuleChainExecutor(true, false);

    @Test
    void shouldExecuteHandlersInOrder() {
        RuleChain<String, String> chain = RuleChain.<String, String>builder("order-check")
                .name("Order Check")
                .handler((request, context) -> {
                    context.set("first", request);
                    return RuleResult.success(null);
                })
                .handler((request, context) -> RuleResult.success(context.get("first", String.class) + "-ok"))
                .build();

        RuleResult<String> result = executor.execute(chain, "req", RuleContext.create("r1", "t1"));

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).isEqualTo("req-ok");
        assertThat(result.getTrace().nodeTraces()).hasSize(2);
    }

    @Test
    void shouldStopWhenHandlerReturnsStop() {
        AtomicInteger count = new AtomicInteger();
        RuleChain<String, String> chain = RuleChain.<String, String>builder("risk-check")
                .handler((request, context) -> {
                    count.incrementAndGet();
                    return RuleResult.stop(600101, "risk blocked", "blocked");
                })
                .handler((request, context) -> {
                    count.incrementAndGet();
                    return RuleResult.success("unreachable");
                })
                .build();

        RuleResult<String> result = executor.execute(chain, "req", RuleContext.create());

        assertThat(result.getStatus()).isEqualTo(RuleStatus.STOPPED);
        assertThat(result.getCode()).isEqualTo(600101);
        assertThat(result.getData()).isEqualTo("blocked");
        assertThat(count).hasValue(1);
    }

    @Test
    void shouldWrapHandlerException() {
        RuleChain<String, String> chain = RuleChain.<String, String>builder("exception-chain")
                .handler((request, context) -> {
                    throw new IllegalStateException("boom");
                })
                .build();

        RuleResult<String> result = executor.execute(chain, "req", RuleContext.create());

        assertThat(result.getStatus()).isEqualTo(RuleStatus.NODE_ERROR);
        assertThat(result.getException()).isInstanceOf(IllegalStateException.class);
    }
}
```

- [ ] **Step 2: Run tests and verify they fail**

Run:

```bash
./mvnw -B -ntp -pl egon-cola-components/egon-cola-component-rule-engine/egon-cola-component-rule-engine-starter -am -Dtest=DefaultRuleChainExecutorTest test
```

Expected: compilation fails because chain contracts and executor do not exist.

- [ ] **Step 3: Implement chain contracts**

Create `ChainHandler.java`:

```java
package top.egon.cola.component.ruleengine.chain;

import top.egon.cola.component.ruleengine.context.RuleContext;
import top.egon.cola.component.ruleengine.result.RuleResult;

@FunctionalInterface
public interface ChainHandler<T, R> {

    RuleResult<R> handle(T request, RuleContext context);
}
```

Create `RuleChain.java`:

```java
package top.egon.cola.component.ruleengine.chain;

import java.util.ArrayList;
import java.util.List;

public record RuleChain<T, R>(
        String code,
        String name,
        List<ChainHandler<T, R>> handlers,
        int maxSteps,
        long timeoutMillis
) {

    public RuleChain {
        handlers = List.copyOf(handlers);
    }

    public static <T, R> Builder<T, R> builder(String code) {
        return new Builder<>(code);
    }

    public static final class Builder<T, R> {

        private final String code;
        private final List<ChainHandler<T, R>> handlers = new ArrayList<>();
        private String name;
        private int maxSteps = 100;
        private long timeoutMillis = 3000L;

        private Builder(String code) {
            this.code = code;
            this.name = code;
        }

        public Builder<T, R> name(String name) {
            this.name = name;
            return this;
        }

        public Builder<T, R> handler(ChainHandler<T, R> handler) {
            this.handlers.add(handler);
            return this;
        }

        public Builder<T, R> maxSteps(int maxSteps) {
            this.maxSteps = maxSteps;
            return this;
        }

        public Builder<T, R> timeoutMillis(long timeoutMillis) {
            this.timeoutMillis = timeoutMillis;
            return this;
        }

        public RuleChain<T, R> build() {
            return new RuleChain<>(code, name, handlers, maxSteps, timeoutMillis);
        }
    }
}
```

- [ ] **Step 4: Implement singleton chain contracts**

Create `SingletonRuleLink.java`:

```java
package top.egon.cola.component.ruleengine.chain;

import top.egon.cola.component.ruleengine.context.RuleContext;
import top.egon.cola.component.ruleengine.result.RuleResult;

public interface SingletonRuleLink<T, R> {

    SingletonRuleLink<T, R> appendNext(SingletonRuleLink<T, R> next);

    RuleResult<R> handle(T request, RuleContext context);
}
```

Create `AbstractSingletonRuleLink.java`:

```java
package top.egon.cola.component.ruleengine.chain;

import top.egon.cola.component.ruleengine.context.RuleContext;
import top.egon.cola.component.ruleengine.result.RuleResult;

public abstract class AbstractSingletonRuleLink<T, R> implements SingletonRuleLink<T, R> {

    private volatile SingletonRuleLink<T, R> next;

    @Override
    public SingletonRuleLink<T, R> appendNext(SingletonRuleLink<T, R> next) {
        this.next = next;
        return next;
    }

    @Override
    public final RuleResult<R> handle(T request, RuleContext context) {
        RuleResult<R> result = apply(request, context);
        if (!result.isSuccess() || context.isStopped() || next == null) {
            return result;
        }
        return next.handle(request, context);
    }

    protected abstract RuleResult<R> apply(T request, RuleContext context);
}
```

- [ ] **Step 5: Implement chain executor contracts**

Create `RuleChainExecutor.java`:

```java
package top.egon.cola.component.ruleengine.engine;

import top.egon.cola.component.ruleengine.chain.RuleChain;
import top.egon.cola.component.ruleengine.context.RuleContext;
import top.egon.cola.component.ruleengine.result.RuleResult;

public interface RuleChainExecutor {

    <T, R> RuleResult<R> execute(RuleChain<T, R> ruleChain, T request, RuleContext context);
}
```

Create `DefaultRuleChainExecutor.java`:

```java
package top.egon.cola.component.ruleengine.engine;

import top.egon.cola.component.ruleengine.chain.ChainHandler;
import top.egon.cola.component.ruleengine.chain.RuleChain;
import top.egon.cola.component.ruleengine.context.RuleContext;
import top.egon.cola.component.ruleengine.result.RuleResult;
import top.egon.cola.component.ruleengine.result.RuleStatus;
import top.egon.cola.component.ruleengine.trace.NodeTrace;
import top.egon.cola.component.ruleengine.trace.RuleTrace;
import top.egon.cola.component.ruleengine.trace.RuleTraceRecorder;
import top.egon.cola.component.ruleengine.tree.NodeType;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

public class DefaultRuleChainExecutor implements RuleChainExecutor {

    private final boolean traceEnabled;
    private final boolean throwException;

    public DefaultRuleChainExecutor(boolean traceEnabled, boolean throwException) {
        this.traceEnabled = traceEnabled;
        this.throwException = throwException;
    }

    @Override
    public <T, R> RuleResult<R> execute(RuleChain<T, R> ruleChain, T request, RuleContext context) {
        RuleContext actualContext = context == null ? RuleContext.create() : context;
        RuleTraceRecorder recorder = new RuleTraceRecorder(traceEnabled);
        Instant start = Instant.now();
        if (ruleChain == null || ruleChain.handlers().isEmpty()) {
            RuleTrace trace = recorder.finish("empty", "empty", "CHAIN", actualContext, RuleStatus.EMPTY_CHAIN, null);
            return RuleResult.<R>failure(RuleStatus.EMPTY_CHAIN, RuleStatus.EMPTY_CHAIN.getMessage(), null)
                    .withTrace(trace)
                    .withCostMillis(Duration.between(start, Instant.now()).toMillis());
        }
        try {
            return runHandlers(ruleChain, request, actualContext, recorder, start);
        } catch (RuntimeException ex) {
            actualContext.addError(ex);
            if (throwException) {
                throw ex;
            }
            RuleTrace trace = recorder.finish(ruleChain.code(), ruleChain.name(), "CHAIN", actualContext, RuleStatus.NODE_ERROR, ex);
            return RuleResult.<R>failure(RuleStatus.NODE_ERROR, ex.getMessage(), ex)
                    .withTrace(trace)
                    .withCostMillis(Duration.between(start, Instant.now()).toMillis());
        }
    }

    private <T, R> RuleResult<R> runHandlers(RuleChain<T, R> ruleChain, T request, RuleContext context,
                                             RuleTraceRecorder recorder, Instant start) {
        RuleResult<R> last = RuleResult.success(null);
        List<ChainHandler<T, R>> handlers = ruleChain.handlers();
        for (int i = 0; i < handlers.size(); i++) {
            if (context.isTimeout()) {
                RuleTrace trace = recorder.finish(ruleChain.code(), ruleChain.name(), "CHAIN", context, RuleStatus.TIMEOUT, null);
                return RuleResult.<R>timeout(RuleStatus.TIMEOUT.getMessage()).withTrace(trace);
            }
            context.incrementStep();
            if (context.isExceededMaxSteps()) {
                RuleTrace trace = recorder.finish(ruleChain.code(), ruleChain.name(), "CHAIN", context, RuleStatus.MAX_STEPS_EXCEEDED, null);
                return RuleResult.<R>maxStepsExceeded(RuleStatus.MAX_STEPS_EXCEEDED.getMessage()).withTrace(trace);
            }
            String nodeCode = "handler-" + (i + 1);
            Instant nodeStart = Instant.now();
            context.enterNode(nodeCode);
            last = handlers.get(i).handle(request, context);
            Instant nodeEnd = Instant.now();
            RuleStatus status = last.getStatus();
            recorder.addNodeTrace(new NodeTrace(nodeCode, nodeCode, NodeType.BIZ, i + 1, 1, nodeStart, nodeEnd,
                    Duration.between(nodeStart, nodeEnd).toMillis(), null, null, status, null));
            if (!last.isSuccess() || context.isStopped() || !context.isProceed()) {
                RuleTrace trace = recorder.finish(ruleChain.code(), ruleChain.name(), "CHAIN", context, last.getStatus(), null);
                return last.withTrace(trace)
                        .withStoppedNode(nodeCode)
                        .withCostMillis(Duration.between(start, Instant.now()).toMillis());
            }
        }
        RuleTrace trace = recorder.finish(ruleChain.code(), ruleChain.name(), "CHAIN", context, last.getStatus(), null);
        return last.withTrace(trace).withCostMillis(Duration.between(start, Instant.now()).toMillis());
    }
}
```

- [ ] **Step 6: Run chain tests**

Run:

```bash
./mvnw -B -ntp -pl egon-cola-components/egon-cola-component-rule-engine/egon-cola-component-rule-engine-starter -am -Dtest=DefaultRuleChainExecutorTest test
```

Expected: all chain tests pass.

- [ ] **Step 7: Commit chain execution**

```bash
git add egon-cola-components/egon-cola-component-rule-engine/egon-cola-component-rule-engine-starter/src/main/java/top/egon/cola/component/ruleengine/chain \
  egon-cola-components/egon-cola-component-rule-engine/egon-cola-component-rule-engine-starter/src/main/java/top/egon/cola/component/ruleengine/engine/RuleChainExecutor.java \
  egon-cola-components/egon-cola-component-rule-engine/egon-cola-component-rule-engine-starter/src/main/java/top/egon/cola/component/ruleengine/engine/DefaultRuleChainExecutor.java \
  egon-cola-components/egon-cola-component-rule-engine/egon-cola-component-rule-engine-starter/src/test/java/top/egon/cola/component/ruleengine/chain/DefaultRuleChainExecutorTest.java
git commit -m "feat: add rule chain executor"
```

## Task 4: Add Rule Tree Execution

**Files:**
- Create: `DefaultRuleTreeExecutorTest.java`
- Create: `NodeType.java`
- Create: `RouteDecision.java`
- Create: `RuleNode.java`
- Create: `RuleRouter.java`
- Create: `RuleTree.java`
- Create: `AbstractRuleNode.java`
- Create: `AbstractAsyncRuleNode.java`
- Create: `RuleTreeExecutor.java`
- Create: `DefaultRuleTreeExecutor.java`

- [ ] **Step 1: Write failing rule tree tests**

Create `DefaultRuleTreeExecutorTest.java`:

```java
package top.egon.cola.component.ruleengine.tree;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.ruleengine.context.RuleContext;
import top.egon.cola.component.ruleengine.engine.DefaultRuleTreeExecutor;
import top.egon.cola.component.ruleengine.result.RuleResult;
import top.egon.cola.component.ruleengine.result.RuleStatus;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultRuleTreeExecutorTest {

    private final DefaultRuleTreeExecutor executor = new DefaultRuleTreeExecutor(true, false);

    @Test
    void shouldRouteByContextAndReturnHitNode() {
        RuleNode<String, String> root = new TestNode("root", RouteDecision.toCode("level"));
        RuleNode<String, String> level = new TestNode("level", RouteDecision.end("gold"));
        RuleTree<String, String> tree = RuleTree.<String, String>builder("member-benefit", root)
                .node(level)
                .build();

        RuleResult<String> result = executor.execute(tree, "req", RuleContext.create());

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).isEqualTo("gold");
        assertThat(result.getHitNode()).isEqualTo("level");
        assertThat(result.getTrace().nodeTraces()).hasSize(2);
    }

    @Test
    void shouldStopLoopWhenMaxStepsExceeded() {
        RuleNode<String, String> loop = new TestNode("loop", RouteDecision.toCode("loop"));
        RuleTree<String, String> tree = RuleTree.<String, String>builder("loop-tree", loop)
                .maxSteps(3)
                .build();
        RuleContext context = RuleContext.create().maxSteps(3);

        RuleResult<String> result = executor.execute(tree, "req", context);

        assertThat(result.getStatus()).isEqualTo(RuleStatus.MAX_STEPS_EXCEEDED);
        assertThat(result.getTrace().nodeTraces()).hasSize(3);
    }

    @Test
    void shouldReturnNoRouteWhenNoDefaultExists() {
        RuleNode<String, String> root = new TestNode("root", RouteDecision.noRoute("missing branch"));
        RuleTree<String, String> tree = RuleTree.<String, String>builder("no-route-tree", root).build();

        RuleResult<String> result = executor.execute(tree, "req", RuleContext.create());

        assertThat(result.getStatus()).isEqualTo(RuleStatus.NO_ROUTE);
        assertThat(result.getMessage()).contains("missing branch");
    }

    private static final class TestNode implements RuleNode<String, String> {

        private final String code;
        private final RouteDecision routeDecision;

        private TestNode(String code, RouteDecision routeDecision) {
            this.code = code;
            this.routeDecision = routeDecision;
        }

        @Override
        public String code() {
            return code;
        }

        @Override
        public String name() {
            return code;
        }

        @Override
        public NodeType type() {
            return NodeType.BIZ;
        }

        @Override
        public RuleResult<String> execute(String request, RuleContext context) {
            return routeDecision.isEnd() ? RuleResult.success(routeDecision.endData(String.class)) : RuleResult.success(null);
        }

        @Override
        public RouteDecision route(String request, RuleContext context) {
            return routeDecision;
        }
    }
}
```

- [ ] **Step 2: Run tests and verify they fail**

Run:

```bash
./mvnw -B -ntp -pl egon-cola-components/egon-cola-component-rule-engine/egon-cola-component-rule-engine-starter -am -Dtest=DefaultRuleTreeExecutorTest test
```

Expected: compilation fails because tree contracts and executor do not exist.

- [ ] **Step 3: Implement tree node contracts**

Create `NodeType.java`:

```java
package top.egon.cola.component.ruleengine.tree;

public enum NodeType {
    ROOT,
    SWITCH,
    BIZ,
    END,
    OTHER
}
```

Create `RuleNode.java`:

```java
package top.egon.cola.component.ruleengine.tree;

import top.egon.cola.component.ruleengine.context.RuleContext;
import top.egon.cola.component.ruleengine.result.RuleResult;

public interface RuleNode<T, R> {

    String code();

    String name();

    NodeType type();

    RuleResult<R> execute(T request, RuleContext context);

    default RouteDecision route(T request, RuleContext context) {
        return RouteDecision.noRoute("node has no route");
    }
}
```

Create `RuleRouter.java`:

```java
package top.egon.cola.component.ruleengine.tree;

import top.egon.cola.component.ruleengine.context.RuleContext;

@FunctionalInterface
public interface RuleRouter<T, R> {

    RouteDecision route(T request, RuleContext context);
}
```

- [ ] **Step 4: Implement route decision and tree definition**

Create `RouteDecision.java`:

```java
package top.egon.cola.component.ruleengine.tree;

public final class RouteDecision {

    private final String targetCode;
    private final RuleNode<?, ?> targetNode;
    private final String reason;
    private final boolean end;
    private final boolean noRoute;
    private final Object endData;

    private RouteDecision(String targetCode, RuleNode<?, ?> targetNode, String reason,
                          boolean end, boolean noRoute, Object endData) {
        this.targetCode = targetCode;
        this.targetNode = targetNode;
        this.reason = reason;
        this.end = end;
        this.noRoute = noRoute;
        this.endData = endData;
    }

    public static RouteDecision toCode(String targetCode) {
        return new RouteDecision(targetCode, null, null, false, false, null);
    }

    public static RouteDecision toCode(String targetCode, String reason) {
        return new RouteDecision(targetCode, null, reason, false, false, null);
    }

    public static RouteDecision toNode(RuleNode<?, ?> targetNode, String reason) {
        return new RouteDecision(null, targetNode, reason, false, false, null);
    }

    public static RouteDecision end(Object data) {
        return new RouteDecision(null, null, null, true, false, data);
    }

    public static RouteDecision noRoute(String reason) {
        return new RouteDecision(null, null, reason, false, true, null);
    }

    public <R> R endData(Class<R> type) {
        return type.isInstance(endData) ? type.cast(endData) : null;
    }

    public String getTargetCode() {
        return targetCode;
    }

    public RuleNode<?, ?> getTargetNode() {
        return targetNode;
    }

    public String getReason() {
        return reason;
    }

    public boolean isEnd() {
        return end;
    }

    public boolean isNoRoute() {
        return noRoute;
    }
}
```

Create `RuleTree.java`:

```java
package top.egon.cola.component.ruleengine.tree;

import java.util.LinkedHashMap;
import java.util.Map;

public record RuleTree<T, R>(
        String code,
        String name,
        RuleNode<T, R> root,
        Map<String, RuleNode<T, R>> nodes,
        String defaultEndNodeCode,
        int maxSteps,
        long timeoutMillis
) {

    public RuleTree {
        nodes = Map.copyOf(nodes);
    }

    public static <T, R> Builder<T, R> builder(String code, RuleNode<T, R> root) {
        return new Builder<>(code, root);
    }

    public static final class Builder<T, R> {

        private final String code;
        private final RuleNode<T, R> root;
        private final Map<String, RuleNode<T, R>> nodes = new LinkedHashMap<>();
        private String name;
        private String defaultEndNodeCode;
        private int maxSteps = 100;
        private long timeoutMillis = 3000L;

        private Builder(String code, RuleNode<T, R> root) {
            this.code = code;
            this.root = root;
            this.name = code;
            if (root != null) {
                this.nodes.put(root.code(), root);
            }
        }

        public Builder<T, R> name(String name) {
            this.name = name;
            return this;
        }

        public Builder<T, R> node(RuleNode<T, R> node) {
            this.nodes.put(node.code(), node);
            return this;
        }

        public Builder<T, R> defaultEndNodeCode(String defaultEndNodeCode) {
            this.defaultEndNodeCode = defaultEndNodeCode;
            return this;
        }

        public Builder<T, R> maxSteps(int maxSteps) {
            this.maxSteps = maxSteps;
            return this;
        }

        public Builder<T, R> timeoutMillis(long timeoutMillis) {
            this.timeoutMillis = timeoutMillis;
            return this;
        }

        public RuleTree<T, R> build() {
            return new RuleTree<>(code, name, root, nodes, defaultEndNodeCode, maxSteps, timeoutMillis);
        }
    }
}
```

- [ ] **Step 5: Implement abstract node helpers**

Create `AbstractRuleNode.java`:

```java
package top.egon.cola.component.ruleengine.tree;

import top.egon.cola.component.ruleengine.context.RuleContext;
import top.egon.cola.component.ruleengine.result.RuleResult;

public abstract class AbstractRuleNode<T, R> implements RuleNode<T, R> {

    private final String code;
    private final String name;
    private final NodeType type;

    protected AbstractRuleNode(String code, String name, NodeType type) {
        this.code = code;
        this.name = name;
        this.type = type;
    }

    @Override
    public String code() {
        return code;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public NodeType type() {
        return type;
    }

    @Override
    public abstract RuleResult<R> execute(T request, RuleContext context);
}
```

Create `AbstractAsyncRuleNode.java`:

```java
package top.egon.cola.component.ruleengine.tree;

import top.egon.cola.component.ruleengine.async.RuleAsyncExecutor;

public abstract class AbstractAsyncRuleNode<T, R> extends AbstractRuleNode<T, R> {

    private final RuleAsyncExecutor asyncExecutor;

    protected AbstractAsyncRuleNode(String code, String name, NodeType type, RuleAsyncExecutor asyncExecutor) {
        super(code, name, type);
        this.asyncExecutor = asyncExecutor;
    }

    protected RuleAsyncExecutor asyncExecutor() {
        return asyncExecutor;
    }
}
```

- [ ] **Step 6: Implement tree executor**

Create `RuleTreeExecutor.java`:

```java
package top.egon.cola.component.ruleengine.engine;

import top.egon.cola.component.ruleengine.context.RuleContext;
import top.egon.cola.component.ruleengine.result.RuleResult;
import top.egon.cola.component.ruleengine.tree.RuleTree;

public interface RuleTreeExecutor {

    <T, R> RuleResult<R> execute(RuleTree<T, R> ruleTree, T request, RuleContext context);
}
```

Create `DefaultRuleTreeExecutor.java`:

```java
package top.egon.cola.component.ruleengine.engine;

import top.egon.cola.component.ruleengine.context.RuleContext;
import top.egon.cola.component.ruleengine.result.RuleResult;
import top.egon.cola.component.ruleengine.result.RuleStatus;
import top.egon.cola.component.ruleengine.trace.NodeTrace;
import top.egon.cola.component.ruleengine.trace.RuleTrace;
import top.egon.cola.component.ruleengine.trace.RuleTraceRecorder;
import top.egon.cola.component.ruleengine.tree.RouteDecision;
import top.egon.cola.component.ruleengine.tree.RuleNode;
import top.egon.cola.component.ruleengine.tree.RuleTree;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

public class DefaultRuleTreeExecutor implements RuleTreeExecutor {

    private final boolean traceEnabled;
    private final boolean throwException;

    public DefaultRuleTreeExecutor(boolean traceEnabled, boolean throwException) {
        this.traceEnabled = traceEnabled;
        this.throwException = throwException;
    }

    @Override
    public <T, R> RuleResult<R> execute(RuleTree<T, R> ruleTree, T request, RuleContext context) {
        RuleContext actualContext = context == null ? RuleContext.create() : context;
        RuleTraceRecorder recorder = new RuleTraceRecorder(traceEnabled);
        Instant start = Instant.now();
        if (ruleTree == null || ruleTree.root() == null) {
            RuleTrace trace = recorder.finish("empty", "empty", "TREE", actualContext, RuleStatus.EMPTY_TREE, null);
            return RuleResult.<R>failure(RuleStatus.EMPTY_TREE, RuleStatus.EMPTY_TREE.getMessage(), null).withTrace(trace);
        }
        try {
            return runTree(ruleTree, request, actualContext, recorder, start);
        } catch (RuntimeException ex) {
            actualContext.addError(ex);
            if (throwException) {
                throw ex;
            }
            RuleTrace trace = recorder.finish(ruleTree.code(), ruleTree.name(), "TREE", actualContext, RuleStatus.NODE_ERROR, ex);
            return RuleResult.<R>failure(RuleStatus.NODE_ERROR, ex.getMessage(), ex).withTrace(trace);
        }
    }

    private <T, R> RuleResult<R> runTree(RuleTree<T, R> tree, T request, RuleContext context,
                                         RuleTraceRecorder recorder, Instant start) {
        RuleNode<T, R> current = tree.root();
        Map<String, Integer> visits = new HashMap<>();
        RuleResult<R> last = RuleResult.success(null);
        while (current != null) {
            if (context.isTimeout()) {
                RuleTrace trace = recorder.finish(tree.code(), tree.name(), "TREE", context, RuleStatus.TIMEOUT, null);
                return RuleResult.<R>timeout(RuleStatus.TIMEOUT.getMessage()).withTrace(trace);
            }
            context.incrementStep();
            if (context.isExceededMaxSteps()) {
                RuleTrace trace = recorder.finish(tree.code(), tree.name(), "TREE", context, RuleStatus.MAX_STEPS_EXCEEDED, null);
                return RuleResult.<R>maxStepsExceeded(RuleStatus.MAX_STEPS_EXCEEDED.getMessage()).withTrace(trace);
            }
            int order = context.getStepCount();
            int visitCount = visits.merge(current.code(), 1, Integer::sum);
            Instant nodeStart = Instant.now();
            context.enterNode(current.code());
            last = current.execute(request, context);
            RouteDecision route = current.route(request, context);
            Instant nodeEnd = Instant.now();
            recorder.addNodeTrace(new NodeTrace(current.code(), current.name(), current.type(), order, visitCount,
                    nodeStart, nodeEnd, Duration.between(nodeStart, nodeEnd).toMillis(), route.getTargetCode(),
                    route.getReason(), last.getStatus(), null));
            if (!last.isSuccess() || context.isStopped() || route.isEnd()) {
                RuleTrace trace = recorder.finish(tree.code(), tree.name(), "TREE", context, last.getStatus(), null);
                return last.withTrace(trace)
                        .withHitNode(current.code())
                        .withCostMillis(Duration.between(start, Instant.now()).toMillis());
            }
            if (route.isNoRoute()) {
                RuleNode<T, R> defaultNode = resolveDefault(tree);
                if (defaultNode == null) {
                    RuleTrace trace = recorder.finish(tree.code(), tree.name(), "TREE", context, RuleStatus.NO_ROUTE, null);
                    return RuleResult.<R>noRoute(route.getReason()).withTrace(trace);
                }
                current = defaultNode;
            } else {
                current = resolveRoute(tree, route);
            }
        }
        RuleTrace trace = recorder.finish(tree.code(), tree.name(), "TREE", context, RuleStatus.NO_ROUTE, null);
        return RuleResult.<R>noRoute(RuleStatus.NO_ROUTE.getMessage()).withTrace(trace);
    }

    @SuppressWarnings("unchecked")
    private <T, R> RuleNode<T, R> resolveRoute(RuleTree<T, R> tree, RouteDecision route) {
        if (route.getTargetNode() != null) {
            return (RuleNode<T, R>) route.getTargetNode();
        }
        return tree.nodes().get(route.getTargetCode());
    }

    private <T, R> RuleNode<T, R> resolveDefault(RuleTree<T, R> tree) {
        if (tree.defaultEndNodeCode() == null || tree.defaultEndNodeCode().isBlank()) {
            return null;
        }
        return tree.nodes().get(tree.defaultEndNodeCode());
    }
}
```

- [ ] **Step 7: Run rule tree tests**

Run:

```bash
./mvnw -B -ntp -pl egon-cola-components/egon-cola-component-rule-engine/egon-cola-component-rule-engine-starter -am -Dtest=DefaultRuleTreeExecutorTest test
```

Expected: all rule tree tests pass.

- [ ] **Step 8: Commit rule tree execution**

```bash
git add egon-cola-components/egon-cola-component-rule-engine/egon-cola-component-rule-engine-starter/src/main/java/top/egon/cola/component/ruleengine/tree \
  egon-cola-components/egon-cola-component-rule-engine/egon-cola-component-rule-engine-starter/src/main/java/top/egon/cola/component/ruleengine/engine/RuleTreeExecutor.java \
  egon-cola-components/egon-cola-component-rule-engine/egon-cola-component-rule-engine-starter/src/main/java/top/egon/cola/component/ruleengine/engine/DefaultRuleTreeExecutor.java \
  egon-cola-components/egon-cola-component-rule-engine/egon-cola-component-rule-engine-starter/src/test/java/top/egon/cola/component/ruleengine/tree/DefaultRuleTreeExecutorTest.java
git commit -m "feat: add rule tree executor"
```

## Task 5: Add Engine Facade, Listeners, And Async Executor

**Files:**
- Create: `DefaultRuleEngineTest.java`
- Create: `RuleExecutionListenerCompositeTest.java`
- Create: `DefaultRuleAsyncExecutorTest.java`
- Create: `RuleEngine.java`
- Create: `DefaultRuleEngine.java`
- Create: `RuleExecutionListener.java`
- Create: `RuleExecutionListenerComposite.java`
- Create: `LoggingRuleExecutionListener.java`
- Create: `RuleAsyncExecutor.java`
- Create: `DefaultRuleAsyncExecutor.java`

- [ ] **Step 1: Write failing facade, listener, and async tests**

Create `DefaultRuleEngineTest.java`:

```java
package top.egon.cola.component.ruleengine.engine;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.ruleengine.chain.RuleChain;
import top.egon.cola.component.ruleengine.context.RuleContext;
import top.egon.cola.component.ruleengine.result.RuleResult;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultRuleEngineTest {

    @Test
    void shouldDelegateChainExecution() {
        RuleChainExecutor chainExecutor = (ruleChain, request, context) -> RuleResult.success("chain-ok");
        RuleTreeExecutor treeExecutor = (ruleTree, request, context) -> RuleResult.success("tree-ok");
        DefaultRuleEngine engine = new DefaultRuleEngine(chainExecutor, treeExecutor);

        RuleResult<String> result = engine.executeChain(RuleChain.<String, String>builder("chain").build(), "req", RuleContext.create());

        assertThat(result.getData()).isEqualTo("chain-ok");
    }
}
```

Create `RuleExecutionListenerCompositeTest.java`:

```java
package top.egon.cola.component.ruleengine.listener;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.ruleengine.context.RuleContext;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RuleExecutionListenerCompositeTest {

    @Test
    void shouldInvokeListenersInOrderAndIgnoreFailureByDefault() {
        List<String> calls = new ArrayList<>();
        RuleExecutionListener first = new RecordingListener("first", calls);
        RuleExecutionListener broken = new BrokenListener();
        RuleExecutionListener last = new RecordingListener("last", calls);
        RuleExecutionListenerComposite composite = new RuleExecutionListenerComposite(List.of(first, broken, last), true);

        composite.beforeEngineExecute("chain", "rule", RuleContext.create());

        assertThat(calls).containsExactly("first", "last");
    }

    private record RecordingListener(String name, List<String> calls) implements RuleExecutionListener {
        @Override
        public void beforeEngineExecute(String modelType, String ruleCode, RuleContext context) {
            calls.add(name);
        }
    }

    private static final class BrokenListener implements RuleExecutionListener {
        @Override
        public void beforeEngineExecute(String modelType, String ruleCode, RuleContext context) {
            throw new IllegalStateException("listener failed");
        }
    }
}
```

Create `DefaultRuleAsyncExecutorTest.java`:

```java
package top.egon.cola.component.ruleengine.async;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.ruleengine.context.RuleContext;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultRuleAsyncExecutorTest {

    @Test
    void shouldLoadValueIntoContext() {
        RuleContext context = RuleContext.create();
        DefaultRuleAsyncExecutor executor = new DefaultRuleAsyncExecutor(2, 4);

        executor.loadToContext("user", () -> "alice", context, Duration.ofSeconds(1));

        assertThat(context.get("user", String.class)).isEqualTo("alice");
        executor.shutdown();
    }
}
```

- [ ] **Step 2: Run tests and verify they fail**

Run:

```bash
./mvnw -B -ntp -pl egon-cola-components/egon-cola-component-rule-engine/egon-cola-component-rule-engine-starter -am -Dtest=DefaultRuleEngineTest,RuleExecutionListenerCompositeTest,DefaultRuleAsyncExecutorTest test
```

Expected: compilation fails because facade, listener, and async classes do not exist.

- [ ] **Step 3: Implement engine facade**

Create `RuleEngine.java`:

```java
package top.egon.cola.component.ruleengine.engine;

import top.egon.cola.component.ruleengine.chain.RuleChain;
import top.egon.cola.component.ruleengine.context.RuleContext;
import top.egon.cola.component.ruleengine.result.RuleResult;
import top.egon.cola.component.ruleengine.tree.RuleTree;

public interface RuleEngine {

    <T, R> RuleResult<R> executeChain(RuleChain<T, R> ruleChain, T request);

    <T, R> RuleResult<R> executeChain(RuleChain<T, R> ruleChain, T request, RuleContext context);

    <T, R> RuleResult<R> executeTree(RuleTree<T, R> ruleTree, T request);

    <T, R> RuleResult<R> executeTree(RuleTree<T, R> ruleTree, T request, RuleContext context);
}
```

Create `DefaultRuleEngine.java`:

```java
package top.egon.cola.component.ruleengine.engine;

import top.egon.cola.component.ruleengine.chain.RuleChain;
import top.egon.cola.component.ruleengine.context.RuleContext;
import top.egon.cola.component.ruleengine.result.RuleResult;
import top.egon.cola.component.ruleengine.tree.RuleTree;

public class DefaultRuleEngine implements RuleEngine {

    private final RuleChainExecutor chainExecutor;
    private final RuleTreeExecutor treeExecutor;

    public DefaultRuleEngine(RuleChainExecutor chainExecutor, RuleTreeExecutor treeExecutor) {
        this.chainExecutor = chainExecutor;
        this.treeExecutor = treeExecutor;
    }

    @Override
    public <T, R> RuleResult<R> executeChain(RuleChain<T, R> ruleChain, T request) {
        return executeChain(ruleChain, request, RuleContext.create());
    }

    @Override
    public <T, R> RuleResult<R> executeChain(RuleChain<T, R> ruleChain, T request, RuleContext context) {
        return chainExecutor.execute(ruleChain, request, context);
    }

    @Override
    public <T, R> RuleResult<R> executeTree(RuleTree<T, R> ruleTree, T request) {
        return executeTree(ruleTree, request, RuleContext.create());
    }

    @Override
    public <T, R> RuleResult<R> executeTree(RuleTree<T, R> ruleTree, T request, RuleContext context) {
        return treeExecutor.execute(ruleTree, request, context);
    }
}
```

- [ ] **Step 4: Implement listener contracts**

Create `RuleExecutionListener.java`:

```java
package top.egon.cola.component.ruleengine.listener;

import top.egon.cola.component.ruleengine.context.RuleContext;
import top.egon.cola.component.ruleengine.result.RuleResult;
import top.egon.cola.component.ruleengine.tree.RouteDecision;

public interface RuleExecutionListener {

    default void beforeEngineExecute(String modelType, String ruleCode, RuleContext context) {
    }

    default void afterEngineExecute(String modelType, String ruleCode, RuleContext context, RuleResult<?> result) {
    }

    default void beforeNodeExecute(String nodeCode, RuleContext context) {
    }

    default void afterNodeExecute(String nodeCode, RuleContext context, RuleResult<?> result) {
    }

    default void beforeRoute(String nodeCode, RuleContext context) {
    }

    default void afterRoute(String nodeCode, RuleContext context, RouteDecision decision) {
    }

    default void onNodeError(String nodeCode, RuleContext context, Throwable error) {
    }

    default void onEngineError(String ruleCode, RuleContext context, Throwable error) {
    }

    default void onStop(String nodeCode, RuleContext context, RuleResult<?> result) {
    }

    default void onTimeout(String ruleCode, RuleContext context) {
    }

    default void onMaxStepsExceeded(String ruleCode, RuleContext context) {
    }
}
```

Create `RuleExecutionListenerComposite.java`:

```java
package top.egon.cola.component.ruleengine.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.egon.cola.component.ruleengine.context.RuleContext;

import java.util.List;
import java.util.function.Consumer;

public class RuleExecutionListenerComposite implements RuleExecutionListener {

    private static final Logger log = LoggerFactory.getLogger(RuleExecutionListenerComposite.class);

    private final List<RuleExecutionListener> listeners;
    private final boolean ignoreErrors;

    public RuleExecutionListenerComposite(List<RuleExecutionListener> listeners, boolean ignoreErrors) {
        this.listeners = List.copyOf(listeners);
        this.ignoreErrors = ignoreErrors;
    }

    @Override
    public void beforeEngineExecute(String modelType, String ruleCode, RuleContext context) {
        each(listener -> listener.beforeEngineExecute(modelType, ruleCode, context));
    }

    private void each(Consumer<RuleExecutionListener> consumer) {
        for (RuleExecutionListener listener : listeners) {
            try {
                consumer.accept(listener);
            } catch (RuntimeException ex) {
                if (!ignoreErrors) {
                    throw ex;
                }
                log.warn("Rule execution listener failed: {}", ex.getMessage(), ex);
            }
        }
    }
}
```

Create `LoggingRuleExecutionListener.java`:

```java
package top.egon.cola.component.ruleengine.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.egon.cola.component.ruleengine.context.RuleContext;
import top.egon.cola.component.ruleengine.result.RuleResult;

public class LoggingRuleExecutionListener implements RuleExecutionListener {

    private static final Logger log = LoggerFactory.getLogger(LoggingRuleExecutionListener.class);

    @Override
    public void afterEngineExecute(String modelType, String ruleCode, RuleContext context, RuleResult<?> result) {
        log.info("rule engine executed modelType={} ruleCode={} traceId={} requestId={} status={} costMillis={}",
                modelType, ruleCode, context.getTraceId(), context.getRequestId(), result.getStatus(), result.getCostMillis());
    }
}
```

- [ ] **Step 5: Wire listeners into chain and tree executors**

Update `DefaultRuleChainExecutor.java` imports:

```java
import top.egon.cola.component.ruleengine.listener.RuleExecutionListener;
import top.egon.cola.component.ruleengine.listener.RuleExecutionListenerComposite;
```

Replace the two-field constructor block with:

```java
private final boolean traceEnabled;
private final boolean throwException;
private final RuleExecutionListener listener;

public DefaultRuleChainExecutor(boolean traceEnabled, boolean throwException) {
    this(traceEnabled, throwException, new RuleExecutionListenerComposite(List.of(), true));
}

public DefaultRuleChainExecutor(boolean traceEnabled, boolean throwException, RuleExecutionListener listener) {
    this.traceEnabled = traceEnabled;
    this.throwException = throwException;
    this.listener = listener;
}
```

At the start of `execute(...)`, after `RuleTraceRecorder recorder = new RuleTraceRecorder(traceEnabled);`, add:

```java
String ruleCode = ruleChain == null ? "empty" : ruleChain.code();
listener.beforeEngineExecute("CHAIN", ruleCode, actualContext);
```

Before each return of a `RuleResult<R>` in `execute(...)` and `runHandlers(...)`, assign the result to a local variable, call `listener.afterEngineExecute("CHAIN", ruleChain.code(), context, result)`, and return that local variable. Use this exact pattern for the normal completion path:

```java
RuleTrace trace = recorder.finish(ruleChain.code(), ruleChain.name(), "CHAIN", context, last.getStatus(), null);
RuleResult<R> result = last.withTrace(trace).withCostMillis(Duration.between(start, Instant.now()).toMillis());
listener.afterEngineExecute("CHAIN", ruleChain.code(), context, result);
return result;
```

Inside the handler loop, immediately before `last = handlers.get(i).handle(request, context);`, add:

```java
listener.beforeNodeExecute(nodeCode, context);
```

Immediately after the handler returns, add:

```java
listener.afterNodeExecute(nodeCode, context, last);
```

Inside the `catch (RuntimeException ex)` block, before the `throwException` check, add:

```java
listener.onEngineError(ruleChain.code(), actualContext, ex);
```

Update `DefaultRuleTreeExecutor.java` imports:

```java
import top.egon.cola.component.ruleengine.listener.RuleExecutionListener;
import top.egon.cola.component.ruleengine.listener.RuleExecutionListenerComposite;
import java.util.List;
```

Replace the tree executor constructor block with:

```java
private final boolean traceEnabled;
private final boolean throwException;
private final RuleExecutionListener listener;

public DefaultRuleTreeExecutor(boolean traceEnabled, boolean throwException) {
    this(traceEnabled, throwException, new RuleExecutionListenerComposite(List.of(), true));
}

public DefaultRuleTreeExecutor(boolean traceEnabled, boolean throwException, RuleExecutionListener listener) {
    this.traceEnabled = traceEnabled;
    this.throwException = throwException;
    this.listener = listener;
}
```

At the start of tree `execute(...)`, after `RuleTraceRecorder recorder = new RuleTraceRecorder(traceEnabled);`, add:

```java
String ruleCode = ruleTree == null ? "empty" : ruleTree.code();
listener.beforeEngineExecute("TREE", ruleCode, actualContext);
```

Inside the tree loop, immediately before `last = current.execute(request, context);`, add:

```java
listener.beforeNodeExecute(current.code(), context);
```

Immediately after node execution, before route calculation, add:

```java
listener.afterNodeExecute(current.code(), context, last);
listener.beforeRoute(current.code(), context);
```

Immediately after `RouteDecision route = current.route(request, context);`, add:

```java
listener.afterRoute(current.code(), context, route);
```

Inside the tree executor `catch (RuntimeException ex)` block, before the `throwException` check, add:

```java
listener.onEngineError(ruleTree.code(), actualContext, ex);
```

Before the normal tree completion return, use this return pattern:

```java
RuleTrace trace = recorder.finish(tree.code(), tree.name(), "TREE", context, last.getStatus(), null);
RuleResult<R> result = last.withTrace(trace)
        .withHitNode(current.code())
        .withCostMillis(Duration.between(start, Instant.now()).toMillis());
listener.afterEngineExecute("TREE", tree.code(), context, result);
return result;
```

- [ ] **Step 6: Implement async executor**

Create `RuleAsyncExecutor.java`:

```java
package top.egon.cola.component.ruleengine.async;

import top.egon.cola.component.ruleengine.context.RuleContext;

import java.time.Duration;
import java.util.concurrent.Callable;

public interface RuleAsyncExecutor {

    <T> T load(Callable<T> loader, RuleContext context, Duration timeout);

    <T> void loadToContext(String key, Callable<T> loader, RuleContext context, Duration timeout);
}
```

Create `DefaultRuleAsyncExecutor.java`:

```java
package top.egon.cola.component.ruleengine.async;

import top.egon.cola.component.ruleengine.context.RuleContext;

import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class DefaultRuleAsyncExecutor implements RuleAsyncExecutor {

    private final ExecutorService executorService;

    public DefaultRuleAsyncExecutor(int corePoolSize, int maxPoolSize) {
        int size = Math.max(corePoolSize, maxPoolSize);
        this.executorService = Executors.newFixedThreadPool(Math.max(1, size));
    }

    @Override
    public <T> T load(Callable<T> loader, RuleContext context, Duration timeout) {
        Future<T> future = executorService.submit(loader);
        try {
            if (timeout == null) {
                return future.get();
            }
            return future.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (Exception ex) {
            future.cancel(true);
            context.addError(ex);
            throw new IllegalStateException("rule async load failed", ex);
        }
    }

    @Override
    public <T> void loadToContext(String key, Callable<T> loader, RuleContext context, Duration timeout) {
        T value = load(loader, context, timeout);
        context.set(key, value);
    }

    public void shutdown() {
        executorService.shutdownNow();
    }
}
```

- [ ] **Step 7: Run facade, listener, and async tests**

Run:

```bash
./mvnw -B -ntp -pl egon-cola-components/egon-cola-component-rule-engine/egon-cola-component-rule-engine-starter -am -Dtest=DefaultRuleEngineTest,RuleExecutionListenerCompositeTest,DefaultRuleAsyncExecutorTest test
```

Expected: all tests pass.

- [ ] **Step 8: Commit facade, listener, and async support**

```bash
git add egon-cola-components/egon-cola-component-rule-engine/egon-cola-component-rule-engine-starter/src/main/java/top/egon/cola/component/ruleengine/engine/RuleEngine.java \
  egon-cola-components/egon-cola-component-rule-engine/egon-cola-component-rule-engine-starter/src/main/java/top/egon/cola/component/ruleengine/engine/DefaultRuleEngine.java \
  egon-cola-components/egon-cola-component-rule-engine/egon-cola-component-rule-engine-starter/src/main/java/top/egon/cola/component/ruleengine/listener \
  egon-cola-components/egon-cola-component-rule-engine/egon-cola-component-rule-engine-starter/src/main/java/top/egon/cola/component/ruleengine/async \
  egon-cola-components/egon-cola-component-rule-engine/egon-cola-component-rule-engine-starter/src/test/java/top/egon/cola/component/ruleengine/engine/DefaultRuleEngineTest.java \
  egon-cola-components/egon-cola-component-rule-engine/egon-cola-component-rule-engine-starter/src/test/java/top/egon/cola/component/ruleengine/listener/RuleExecutionListenerCompositeTest.java \
  egon-cola-components/egon-cola-component-rule-engine/egon-cola-component-rule-engine-starter/src/test/java/top/egon/cola/component/ruleengine/async/DefaultRuleAsyncExecutorTest.java
git commit -m "feat: add rule engine facade and extensions"
```

## Task 6: Add Spring Boot Auto-Configuration

**Files:**
- Create: `RuleEngineAutoConfigurationTest.java`
- Create: `RuleEngineAutoConfiguration.java`
- Create: `RuleEngineProperties.java`
- Create: `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`

- [ ] **Step 1: Write failing auto-configuration test**

Create `RuleEngineAutoConfigurationTest.java`:

```java
package top.egon.cola.component.ruleengine.autoconfigure;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import top.egon.cola.component.ruleengine.async.RuleAsyncExecutor;
import top.egon.cola.component.ruleengine.engine.RuleChainExecutor;
import top.egon.cola.component.ruleengine.engine.RuleEngine;
import top.egon.cola.component.ruleengine.engine.RuleTreeExecutor;
import top.egon.cola.component.ruleengine.listener.RuleExecutionListenerComposite;

import static org.assertj.core.api.Assertions.assertThat;

class RuleEngineAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(org.springframework.boot.autoconfigure.AutoConfigurations.of(RuleEngineAutoConfiguration.class));

    @Test
    void shouldCreateDefaultBeansWhenEnabled() {
        contextRunner.run(context -> assertThat(context)
                .hasSingleBean(RuleEngine.class)
                .hasSingleBean(RuleChainExecutor.class)
                .hasSingleBean(RuleTreeExecutor.class)
                .hasSingleBean(RuleAsyncExecutor.class)
                .hasSingleBean(RuleExecutionListenerComposite.class));
    }

    @Test
    void shouldDisableAutoConfigurationByProperty() {
        contextRunner.withPropertyValues("egon.cola.component.rule-engine.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(RuleEngine.class));
    }
}
```

- [ ] **Step 2: Run test and verify it fails**

Run:

```bash
./mvnw -B -ntp -pl egon-cola-components/egon-cola-component-rule-engine/egon-cola-component-rule-engine-starter -am -Dtest=RuleEngineAutoConfigurationTest test
```

Expected: compilation fails because auto-configuration classes do not exist.

- [ ] **Step 3: Implement properties**

Create `RuleEngineProperties.java`:

```java
package top.egon.cola.component.ruleengine.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "egon.cola.component.rule-engine", ignoreInvalidFields = true)
public class RuleEngineProperties {

    private boolean enabled = true;
    private int defaultMaxSteps = 100;
    private long defaultTimeoutMillis = 3000L;
    private int asyncCorePoolSize = 4;
    private int asyncMaxPoolSize = 16;
    private boolean traceEnabled = true;
    private boolean listenerErrorIgnore = true;
    private boolean throwException = false;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getDefaultMaxSteps() {
        return defaultMaxSteps;
    }

    public void setDefaultMaxSteps(int defaultMaxSteps) {
        this.defaultMaxSteps = defaultMaxSteps;
    }

    public long getDefaultTimeoutMillis() {
        return defaultTimeoutMillis;
    }

    public void setDefaultTimeoutMillis(long defaultTimeoutMillis) {
        this.defaultTimeoutMillis = defaultTimeoutMillis;
    }

    public int getAsyncCorePoolSize() {
        return asyncCorePoolSize;
    }

    public void setAsyncCorePoolSize(int asyncCorePoolSize) {
        this.asyncCorePoolSize = asyncCorePoolSize;
    }

    public int getAsyncMaxPoolSize() {
        return asyncMaxPoolSize;
    }

    public void setAsyncMaxPoolSize(int asyncMaxPoolSize) {
        this.asyncMaxPoolSize = asyncMaxPoolSize;
    }

    public boolean isTraceEnabled() {
        return traceEnabled;
    }

    public void setTraceEnabled(boolean traceEnabled) {
        this.traceEnabled = traceEnabled;
    }

    public boolean isListenerErrorIgnore() {
        return listenerErrorIgnore;
    }

    public void setListenerErrorIgnore(boolean listenerErrorIgnore) {
        this.listenerErrorIgnore = listenerErrorIgnore;
    }

    public boolean isThrowException() {
        return throwException;
    }

    public void setThrowException(boolean throwException) {
        this.throwException = throwException;
    }
}
```

- [ ] **Step 4: Implement auto-configuration**

Create `RuleEngineAutoConfiguration.java`:

```java
package top.egon.cola.component.ruleengine.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import top.egon.cola.component.ruleengine.async.DefaultRuleAsyncExecutor;
import top.egon.cola.component.ruleengine.async.RuleAsyncExecutor;
import top.egon.cola.component.ruleengine.engine.DefaultRuleChainExecutor;
import top.egon.cola.component.ruleengine.engine.DefaultRuleEngine;
import top.egon.cola.component.ruleengine.engine.DefaultRuleTreeExecutor;
import top.egon.cola.component.ruleengine.engine.RuleChainExecutor;
import top.egon.cola.component.ruleengine.engine.RuleEngine;
import top.egon.cola.component.ruleengine.engine.RuleTreeExecutor;
import top.egon.cola.component.ruleengine.listener.LoggingRuleExecutionListener;
import top.egon.cola.component.ruleengine.listener.RuleExecutionListener;
import top.egon.cola.component.ruleengine.listener.RuleExecutionListenerComposite;

import java.util.ArrayList;
import java.util.List;

@AutoConfiguration
@EnableConfigurationProperties(RuleEngineProperties.class)
@ConditionalOnProperty(prefix = "egon.cola.component.rule-engine", name = "enabled", havingValue = "true", matchIfMissing = true)
public class RuleEngineAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public RuleChainExecutor ruleChainExecutor(RuleEngineProperties properties,
                                               RuleExecutionListenerComposite listeners) {
        return new DefaultRuleChainExecutor(properties.isTraceEnabled(), properties.isThrowException(), listeners);
    }

    @Bean
    @ConditionalOnMissingBean
    public RuleTreeExecutor ruleTreeExecutor(RuleEngineProperties properties,
                                             RuleExecutionListenerComposite listeners) {
        return new DefaultRuleTreeExecutor(properties.isTraceEnabled(), properties.isThrowException(), listeners);
    }

    @Bean
    @ConditionalOnMissingBean
    public RuleEngine ruleEngine(RuleChainExecutor chainExecutor, RuleTreeExecutor treeExecutor) {
        return new DefaultRuleEngine(chainExecutor, treeExecutor);
    }

    @Bean
    @ConditionalOnMissingBean
    public RuleAsyncExecutor ruleAsyncExecutor(RuleEngineProperties properties) {
        return new DefaultRuleAsyncExecutor(properties.getAsyncCorePoolSize(), properties.getAsyncMaxPoolSize());
    }

    @Bean
    @ConditionalOnMissingBean(LoggingRuleExecutionListener.class)
    public LoggingRuleExecutionListener loggingRuleExecutionListener() {
        return new LoggingRuleExecutionListener();
    }

    @Bean
    @ConditionalOnMissingBean
    public RuleExecutionListenerComposite ruleExecutionListenerComposite(List<RuleExecutionListener> listeners,
                                                                        RuleEngineProperties properties) {
        List<RuleExecutionListener> ordered = new ArrayList<>(listeners);
        ordered.sort(AnnotationAwareOrderComparator.INSTANCE);
        return new RuleExecutionListenerComposite(ordered, properties.isListenerErrorIgnore());
    }
}
```

- [ ] **Step 5: Add Spring Boot auto-configuration imports file**

Create `egon-cola-component-rule-engine-starter/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`:

```text
top.egon.cola.component.ruleengine.autoconfigure.RuleEngineAutoConfiguration
```

- [ ] **Step 6: Run auto-configuration tests**

Run:

```bash
./mvnw -B -ntp -pl egon-cola-components/egon-cola-component-rule-engine/egon-cola-component-rule-engine-starter -am -Dtest=RuleEngineAutoConfigurationTest test
```

Expected: both auto-configuration tests pass.

- [ ] **Step 7: Commit auto-configuration**

```bash
git add egon-cola-components/egon-cola-component-rule-engine/egon-cola-component-rule-engine-starter/src/main/java/top/egon/cola/component/ruleengine/autoconfigure \
  egon-cola-components/egon-cola-component-rule-engine/egon-cola-component-rule-engine-starter/src/main/resources \
  egon-cola-components/egon-cola-component-rule-engine/egon-cola-component-rule-engine-starter/src/test/java/top/egon/cola/component/ruleengine/autoconfigure/RuleEngineAutoConfigurationTest.java
git commit -m "feat: add rule engine auto configuration"
```

## Task 7: Add Samples, README, And Final Validation

**Files:**
- Create: `egon-cola-components/egon-cola-component-rule-engine/README.md`
- Create: all four sample tests listed in File Structure

- [ ] **Step 1: Write sample tests in the test module**

Create `RuleEngineOrderChainSampleTest.java` with an order pre-check chain:

```java
package top.egon.cola.component.ruleengine.test;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.ruleengine.chain.RuleChain;
import top.egon.cola.component.ruleengine.context.RuleContext;
import top.egon.cola.component.ruleengine.engine.DefaultRuleChainExecutor;
import top.egon.cola.component.ruleengine.result.RuleResult;

import static org.assertj.core.api.Assertions.assertThat;

class RuleEngineOrderChainSampleTest {

    @Test
    void shouldRunOrderPreCheckChain() {
        RuleChain<OrderRequest, String> chain = RuleChain.<OrderRequest, String>builder("order-pre-check")
                .handler((request, context) -> {
                    context.set("paramChecked", request.orderId() != null);
                    return RuleResult.success(null);
                })
                .handler((request, context) -> request.stock() > 0
                        ? RuleResult.success("allowed")
                        : RuleResult.stop(600201, "stock unavailable", "blocked"))
                .build();

        RuleResult<String> result = new DefaultRuleChainExecutor(true, false)
                .execute(chain, new OrderRequest("O-1", 3), RuleContext.create());

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).isEqualTo("allowed");
        assertThat(result.getTrace().nodeTraces()).hasSize(2);
    }

    private record OrderRequest(String orderId, int stock) {
    }
}
```

Create `RuleEngineLoginSingletonChainSampleTest.java`:

```java
package top.egon.cola.component.ruleengine.test;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.ruleengine.chain.AbstractSingletonRuleLink;
import top.egon.cola.component.ruleengine.context.RuleContext;
import top.egon.cola.component.ruleengine.result.RuleResult;

import static org.assertj.core.api.Assertions.assertThat;

class RuleEngineLoginSingletonChainSampleTest {

    @Test
    void shouldRunLoginSingletonChain() {
        AccountCheck accountCheck = new AccountCheck();
        PasswordCheck passwordCheck = new PasswordCheck();
        StatusCheck statusCheck = new StatusCheck();
        accountCheck.appendNext(passwordCheck).appendNext(statusCheck);

        RuleResult<String> result = accountCheck.handle(new LoginRequest("egon", "secret", true), RuleContext.create());

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).isEqualTo("login-allowed");
    }

    private static final class AccountCheck extends AbstractSingletonRuleLink<LoginRequest, String> {
        @Override
        protected RuleResult<String> apply(LoginRequest request, RuleContext context) {
            return request.account() == null || request.account().isBlank()
                    ? RuleResult.stop(600301, "account required", "login-blocked")
                    : RuleResult.success(null);
        }
    }

    private static final class PasswordCheck extends AbstractSingletonRuleLink<LoginRequest, String> {
        @Override
        protected RuleResult<String> apply(LoginRequest request, RuleContext context) {
            return request.password() == null || request.password().isBlank()
                    ? RuleResult.stop(600302, "password required", "login-blocked")
                    : RuleResult.success(null);
        }
    }

    private static final class StatusCheck extends AbstractSingletonRuleLink<LoginRequest, String> {
        @Override
        protected RuleResult<String> apply(LoginRequest request, RuleContext context) {
            return request.active()
                    ? RuleResult.success("login-allowed")
                    : RuleResult.stop(600303, "account disabled", "login-blocked");
        }
    }

    private record LoginRequest(String account, String password, boolean active) {
    }
}
```

Create `RuleEngineMemberBenefitTreeSampleTest.java`:

```java
package top.egon.cola.component.ruleengine.test;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.ruleengine.context.RuleContext;
import top.egon.cola.component.ruleengine.engine.DefaultRuleTreeExecutor;
import top.egon.cola.component.ruleengine.result.RuleResult;
import top.egon.cola.component.ruleengine.result.RuleStatus;
import top.egon.cola.component.ruleengine.tree.NodeType;
import top.egon.cola.component.ruleengine.tree.RouteDecision;
import top.egon.cola.component.ruleengine.tree.RuleNode;
import top.egon.cola.component.ruleengine.tree.RuleTree;

import static org.assertj.core.api.Assertions.assertThat;

class RuleEngineMemberBenefitTreeSampleTest {

    @Test
    void shouldStopMemberBenefitLoopByMaxSteps() {
        RuleNode<MemberRequest, String> root = new StaticNode("root", NodeType.ROOT, RouteDecision.toCode("account"));
        RuleNode<MemberRequest, String> account = new StaticNode("account", NodeType.BIZ, RouteDecision.toCode("level"));
        RuleNode<MemberRequest, String> level = new StaticNode("level", NodeType.SWITCH, RouteDecision.toCode("coupon"));
        RuleNode<MemberRequest, String> coupon = new StaticNode("coupon", NodeType.BIZ, RouteDecision.toCode("level", "recheck level after coupon"));

        RuleTree<MemberRequest, String> tree = RuleTree.<MemberRequest, String>builder("member-benefit", root)
                .node(account)
                .node(level)
                .node(coupon)
                .maxSteps(5)
                .build();

        RuleResult<String> result = new DefaultRuleTreeExecutor(true, false)
                .execute(tree, new MemberRequest("U-1"), RuleContext.create().maxSteps(5));

        assertThat(result.getStatus()).isEqualTo(RuleStatus.MAX_STEPS_EXCEEDED);
        assertThat(result.getTrace().nodeTraces()).hasSize(5);
    }

    private record StaticNode(String code, NodeType type, RouteDecision decision) implements RuleNode<MemberRequest, String> {

        @Override
        public String name() {
            return code;
        }

        @Override
        public RuleResult<String> execute(MemberRequest request, RuleContext context) {
            return RuleResult.success(null);
        }

        @Override
        public RouteDecision route(MemberRequest request, RuleContext context) {
            return decision;
        }
    }

    private record MemberRequest(String userId) {
    }
}
```

Create `RuleEngineAutoConfigurationSampleTest.java`:

```java
package top.egon.cola.component.ruleengine.test;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import top.egon.cola.component.ruleengine.autoconfigure.RuleEngineAutoConfiguration;
import top.egon.cola.component.ruleengine.engine.RuleEngine;

import static org.assertj.core.api.Assertions.assertThat;

class RuleEngineAutoConfigurationSampleTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(org.springframework.boot.autoconfigure.AutoConfigurations.of(RuleEngineAutoConfiguration.class));

    @Test
    void shouldInjectRuleEngineFromStarter() {
        contextRunner.run(context -> assertThat(context).hasSingleBean(RuleEngine.class));
    }
}
```

- [ ] **Step 2: Run sample tests**

Run:

```bash
./mvnw -B -ntp -pl egon-cola-components/egon-cola-component-rule-engine/egon-cola-component-rule-engine-test -am test
```

Expected: sample tests compile and pass when Tasks 1 through 6 are complete.

- [ ] **Step 3: Write component README**

Create `egon-cola-components/egon-cola-component-rule-engine/README.md`:

```markdown
# Egon COLA Rule Engine Component

This component provides a lightweight Spring Boot Starter for Java-assembled business rule orchestration.

## Modules

| Module | Purpose |
|---|---|
| `egon-cola-component-rule-engine-starter` | Business application starter, rule engine API, chain executor, tree executor, trace, listener, and async loading. |
| `egon-cola-component-rule-engine-test` | Sample and validation module for chain, singleton chain, rule tree, and auto-configuration flows. |

## Starter Dependency

```xml
<dependency>
    <groupId>top.egon</groupId>
    <artifactId>egon-cola-component-rule-engine-starter</artifactId>
    <version>5.2.0-SNAPSHOT</version>
</dependency>
```

## Configuration

```yaml
egon:
  cola:
    component:
      rule-engine:
        enabled: true
        default-max-steps: 100
        default-timeout-millis: 3000
        async-core-pool-size: 4
        async-max-pool-size: 16
        trace-enabled: true
        listener-error-ignore: true
        throw-exception: false
```

## Boundaries

Rules are assembled with Java code. YAML, JSON, database, remote configuration, UI management, hot update, tenant binding, permission binding, grayscale binding, and expression engines are outside V1.0.

## Validation

```bash
./mvnw -B -ntp -pl egon-cola-components/egon-cola-component-rule-engine -am test
```
```

- [ ] **Step 4: Run focused starter and test-module validation**

Run:

```bash
./mvnw -B -ntp -pl egon-cola-components/egon-cola-component-rule-engine -am test
```

Expected: all starter unit tests and test-module sample tests pass.

- [ ] **Step 5: Run source boundary checks**

Run:

```bash
rg -n "org\\.springframework\\.data|jakarta\\.persistence|org\\.flywaydb|org\\.redisson|top\\.egon\\.cola\\.component\\.ddc|top\\.egon\\.cola\\.component\\.dtp" egon-cola-components/egon-cola-component-rule-engine/egon-cola-component-rule-engine-starter/src/main/java
```

Expected: no matches.

Run:

```bash
rg -n "YAML|JSON|database topology|hot update|UI management" egon-cola-components/egon-cola-component-rule-engine/README.md
```

Expected: README explicitly lists these as V1.0 boundaries.

- [ ] **Step 6: Run whitespace check**

Run:

```bash
git diff --check
```

Expected: no whitespace errors.

- [ ] **Step 7: Commit samples, README, and validation-facing docs**

```bash
git add egon-cola-components/egon-cola-component-rule-engine/README.md \
  egon-cola-components/egon-cola-component-rule-engine/egon-cola-component-rule-engine-test/src/test/java
git commit -m "test: add rule engine usage samples"
```

## Final Completion Checklist

- [ ] `./mvnw -B -ntp -pl egon-cola-components/egon-cola-component-rule-engine -am test` passes.
- [ ] `git diff --check` passes.
- [ ] `egon-cola-components/pom.xml` aggregates `egon-cola-component-rule-engine`.
- [ ] `egon-cola-components/egon-cola-components-bom/pom.xml` exports only `egon-cola-component-rule-engine-starter`, not the test module.
- [ ] No component-local `docs` directory exists under `egon-cola-component-rule-engine`.
- [ ] Starter source has no DB, Flyway, Redis, DDC, DTP, admin, or UI dependency.
- [ ] README documents Java-only topology assembly and V1.0 non-goals.
- [ ] No long-running service was started during validation.
