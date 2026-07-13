# Method Extension Component Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build an independent `egon-cola-component-method-extension` Spring Boot Starter that runs one typed business handler before an annotated method and either proceeds or returns a validated rejection response.

**Architecture:** The component follows the repository-standard `root + starter + test` shape. `@MethodExtension` selects a Spring-managed Strategy implementation, `MethodExtensionAop` orchestrates the call, focused resolvers handle proxy methods, handler Beans, and rejection responses, and auto-configuration wires the feature without Access Guard, common, Web, Redis, or database dependencies.

**Tech Stack:** Java 21, Spring Boot 3.5.16, Spring AOP/AspectJ, Jackson Databind, JUnit 5, AssertJ, Maven Wrapper.

---

## Implementation Scope and File Map

Create the component reactor and public documentation:

```text
egon-cola-components/egon-cola-component-method-extension/pom.xml
egon-cola-components/egon-cola-component-method-extension/README.md
egon-cola-components/egon-cola-component-method-extension/egon-cola-component-method-extension-starter/pom.xml
egon-cola-components/egon-cola-component-method-extension/egon-cola-component-method-extension-test/pom.xml
```

Create the starter production files:

```text
egon-cola-components/egon-cola-component-method-extension/egon-cola-component-method-extension-starter/src/main/java/top/egon/cola/component/methodextension/annotation/MethodExtension.java
egon-cola-components/egon-cola-component-method-extension/egon-cola-component-method-extension-starter/src/main/java/top/egon/cola/component/methodextension/aop/MethodExtensionAop.java
egon-cola-components/egon-cola-component-method-extension/egon-cola-component-method-extension-starter/src/main/java/top/egon/cola/component/methodextension/autoconfigure/MethodExtensionAutoConfiguration.java
egon-cola-components/egon-cola-component-method-extension/egon-cola-component-method-extension-starter/src/main/java/top/egon/cola/component/methodextension/autoconfigure/MethodExtensionProperties.java
egon-cola-components/egon-cola-component-method-extension/egon-cola-component-method-extension-starter/src/main/java/top/egon/cola/component/methodextension/context/MethodExtensionContext.java
egon-cola-components/egon-cola-component-method-extension/egon-cola-component-method-extension-starter/src/main/java/top/egon/cola/component/methodextension/handler/MethodExtensionDecision.java
egon-cola-components/egon-cola-component-method-extension/egon-cola-component-method-extension-starter/src/main/java/top/egon/cola/component/methodextension/handler/MethodExtensionHandler.java
egon-cola-components/egon-cola-component-method-extension/egon-cola-component-method-extension-starter/src/main/java/top/egon/cola/component/methodextension/handler/MethodExtensionHandlerResolver.java
egon-cola-components/egon-cola-component-method-extension/egon-cola-component-method-extension-starter/src/main/java/top/egon/cola/component/methodextension/response/MethodExtensionResponseResolver.java
egon-cola-components/egon-cola-component-method-extension/egon-cola-component-method-extension-starter/src/main/java/top/egon/cola/component/methodextension/support/MethodExtensionMethodResolver.java
egon-cola-components/egon-cola-component-method-extension/egon-cola-component-method-extension-starter/src/main/java/top/egon/cola/component/methodextension/exception/MethodExtensionException.java
egon-cola-components/egon-cola-component-method-extension/egon-cola-component-method-extension-starter/src/main/java/top/egon/cola/component/methodextension/exception/MethodExtensionConfigurationException.java
egon-cola-components/egon-cola-component-method-extension/egon-cola-component-method-extension-starter/src/main/java/top/egon/cola/component/methodextension/exception/MethodExtensionResponseException.java
egon-cola-components/egon-cola-component-method-extension/egon-cola-component-method-extension-starter/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
```

Create focused tests beside the starter and samples in the test module. Each task below names its exact test file. Do not add a component-local `docs/` directory and do not modify any existing Flyway migration.

### Task 1: Register the Component Reactor and Maven Boundaries

**Files:**
- Create: `egon-cola-components/egon-cola-component-method-extension/pom.xml`
- Create: `egon-cola-components/egon-cola-component-method-extension/egon-cola-component-method-extension-starter/pom.xml`
- Create: `egon-cola-components/egon-cola-component-method-extension/egon-cola-component-method-extension-test/pom.xml`
- Modify: `egon-cola-components/pom.xml:55-62`
- Modify: `egon-cola-components/egon-cola-components-bom/pom.xml:116-125`

- [ ] **Step 1: Create the component root POM**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>top.egon</groupId>
        <artifactId>egon-cola-components-parent</artifactId>
        <version>5.2.2</version>
        <relativePath>../pom.xml</relativePath>
    </parent>

    <artifactId>egon-cola-component-method-extension</artifactId>
    <packaging>pom</packaging>
    <name>egon-cola-component-method-extension</name>
    <description>Method extension component for Egon COLA.</description>

    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>top.egon</groupId>
                <artifactId>egon-cola-component-method-extension-starter</artifactId>
                <version>${project.version}</version>
            </dependency>
        </dependencies>
    </dependencyManagement>

    <modules>
        <module>egon-cola-component-method-extension-starter</module>
        <module>egon-cola-component-method-extension-test</module>
    </modules>
</project>
```

- [ ] **Step 2: Create the starter POM with the approved lightweight dependency set**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>top.egon</groupId>
        <artifactId>egon-cola-component-method-extension</artifactId>
        <version>5.2.2</version>
        <relativePath>../pom.xml</relativePath>
    </parent>

    <artifactId>egon-cola-component-method-extension-starter</artifactId>
    <packaging>jar</packaging>
    <name>egon-cola-component-method-extension-starter</name>
    <description>Spring Boot starter for Egon COLA method extension.</description>

    <dependencies>
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
            <groupId>com.fasterxml.jackson.core</groupId>
            <artifactId>jackson-databind</artifactId>
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

- [ ] **Step 3: Create the test-module POM**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>top.egon</groupId>
        <artifactId>egon-cola-component-method-extension</artifactId>
        <version>5.2.2</version>
        <relativePath>../pom.xml</relativePath>
    </parent>

    <artifactId>egon-cola-component-method-extension-test</artifactId>
    <packaging>jar</packaging>
    <name>egon-cola-component-method-extension-test</name>
    <description>Sample and validation module for Egon COLA method extension.</description>

    <dependencies>
        <dependency>
            <groupId>top.egon</groupId>
            <artifactId>egon-cola-component-method-extension-starter</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>
</project>
```

- [ ] **Step 4: Add the component root to the components parent**

Replace the existing `<modules>` block in `egon-cola-components/pom.xml` with:

```xml
    <modules>
        <module>egon-cola-components-bom</module>
        <module>egon-cola-component-common</module>
        <module>egon-cola-component-dynamic-thread-pool</module>
        <module>egon-cola-component-dynamic-config-center</module>
        <module>egon-cola-component-rule-engine</module>
        <module>egon-cola-component-access-guard</module>
        <module>egon-cola-component-method-extension</module>
    </modules>
```

- [ ] **Step 5: Export only the starter from the components BOM**

Insert this dependency after the Access Guard starter entry in `egon-cola-components/egon-cola-components-bom/pom.xml`:

```xml
            <dependency>
                <groupId>top.egon</groupId>
                <artifactId>egon-cola-component-method-extension-starter</artifactId>
                <version>${project.version}</version>
            </dependency>
```

- [ ] **Step 6: Validate all three new Maven projects**

Run:

```bash
./mvnw -B -ntp -f egon-cola-components/egon-cola-component-method-extension/pom.xml validate
```

Expected: `BUILD SUCCESS` with the root, starter, and test modules in the reactor summary.

- [ ] **Step 7: Commit the Maven skeleton**

```bash
git add egon-cola-components/pom.xml \
  egon-cola-components/egon-cola-components-bom/pom.xml \
  egon-cola-components/egon-cola-component-method-extension
git commit -m "feat: register method extension component"
```

### Task 2: Define the Annotation, Context, Handler, and Decision Contracts

**Files:**
- Create: `egon-cola-components/egon-cola-component-method-extension/egon-cola-component-method-extension-starter/src/test/java/top/egon/cola/component/methodextension/annotation/MethodExtensionAnnotationTest.java`
- Create: `egon-cola-components/egon-cola-component-method-extension/egon-cola-component-method-extension-starter/src/test/java/top/egon/cola/component/methodextension/context/MethodExtensionContextTest.java`
- Create: `egon-cola-components/egon-cola-component-method-extension/egon-cola-component-method-extension-starter/src/test/java/top/egon/cola/component/methodextension/handler/MethodExtensionDecisionTest.java`
- Create: `egon-cola-components/egon-cola-component-method-extension/egon-cola-component-method-extension-starter/src/main/java/top/egon/cola/component/methodextension/annotation/MethodExtension.java`
- Create: `egon-cola-components/egon-cola-component-method-extension/egon-cola-component-method-extension-starter/src/main/java/top/egon/cola/component/methodextension/context/MethodExtensionContext.java`
- Create: `egon-cola-components/egon-cola-component-method-extension/egon-cola-component-method-extension-starter/src/main/java/top/egon/cola/component/methodextension/handler/MethodExtensionHandler.java`
- Create: `egon-cola-components/egon-cola-component-method-extension/egon-cola-component-method-extension-starter/src/main/java/top/egon/cola/component/methodextension/handler/MethodExtensionDecision.java`

- [ ] **Step 1: Write failing tests for the public contracts**

Create `MethodExtensionAnnotationTest.java`:

```java
package top.egon.cola.component.methodextension.annotation;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.methodextension.context.MethodExtensionContext;
import top.egon.cola.component.methodextension.handler.MethodExtensionDecision;
import top.egon.cola.component.methodextension.handler.MethodExtensionHandler;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class MethodExtensionAnnotationTest {

    @Test
    void shouldExposeRuntimeMethodAnnotationContract() throws NoSuchMethodException {
        Target target = MethodExtension.class.getAnnotation(Target.class);
        Retention retention = MethodExtension.class.getAnnotation(Retention.class);
        Method method = SampleService.class.getDeclaredMethod("query", String.class);
        MethodExtension annotation = method.getAnnotation(MethodExtension.class);

        assertThat(Arrays.asList(target.value())).containsExactly(ElementType.METHOD);
        assertThat(retention.value()).isEqualTo(RetentionPolicy.RUNTIME);
        assertThat(annotation.handler()).isEqualTo(AllowHandler.class);
        assertThat(annotation.returnJson()).isEqualTo("{\"code\":1111}");
    }

    static class SampleService {

        @MethodExtension(handler = AllowHandler.class, returnJson = "{\"code\":1111}")
        String query(String userId) {
            return userId;
        }
    }

    static class AllowHandler implements MethodExtensionHandler {

        @Override
        public MethodExtensionDecision evaluate(MethodExtensionContext context) {
            return MethodExtensionDecision.allow();
        }
    }
}
```

Create `MethodExtensionContextTest.java`:

```java
package top.egon.cola.component.methodextension.context;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

class MethodExtensionContextTest {

    @Test
    void shouldExposeTargetMethodAndDefensiveArgumentCopies() throws NoSuchMethodException {
        SampleService target = new SampleService();
        Method method = SampleService.class.getDeclaredMethod("query", String.class);
        Object[] source = {"u-001"};

        MethodExtensionContext context = new MethodExtensionContext(target, method, source);
        source[0] = "changed-at-source";
        Object[] returned = context.arguments();
        returned[0] = "changed-at-caller";

        assertThat(context.target()).isSameAs(target);
        assertThat(context.method()).isEqualTo(method);
        assertThat(context.arguments()).containsExactly("u-001");
    }

    static class SampleService {

        String query(String userId) {
            return userId;
        }
    }
}
```

Create `MethodExtensionDecisionTest.java`:

```java
package top.egon.cola.component.methodextension.handler;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class MethodExtensionDecisionTest {

    @Test
    void shouldCreateAllowDecision() {
        MethodExtensionDecision decision = MethodExtensionDecision.allow();

        assertThat(decision.allowed()).isTrue();
        assertThat(decision.responseProvided()).isFalse();
        assertThat(decision.response()).isNull();
        assertThat(decision.reason()).isEmpty();
    }

    @Test
    void shouldCreateFallbackRejectionWithReason() {
        MethodExtensionDecision decision = MethodExtensionDecision.rejectWithReason("blacklist hit");

        assertThat(decision.allowed()).isFalse();
        assertThat(decision.responseProvided()).isFalse();
        assertThat(decision.response()).isNull();
        assertThat(decision.reason()).isEqualTo("blacklist hit");
    }

    @Test
    void shouldCreateDirectResponseRejection() {
        MethodExtensionDecision decision = MethodExtensionDecision.reject("blocked", "policy rejected");

        assertThat(decision.allowed()).isFalse();
        assertThat(decision.responseProvided()).isTrue();
        assertThat(decision.response()).isEqualTo("blocked");
        assertThat(decision.reason()).isEqualTo("policy rejected");
    }

    @Test
    void shouldRejectNullDirectResponse() {
        assertThatNullPointerException()
                .isThrownBy(() -> MethodExtensionDecision.reject(null))
                .withMessage("response must not be null");
    }
}
```

- [ ] **Step 2: Run the tests to verify they fail before the contracts exist**

Run:

```bash
./mvnw -B -ntp \
  -f egon-cola-components/egon-cola-component-method-extension/egon-cola-component-method-extension-starter/pom.xml \
  -Dtest=MethodExtensionAnnotationTest,MethodExtensionContextTest,MethodExtensionDecisionTest test
```

Expected: `testCompile` fails because `MethodExtension`, `MethodExtensionContext`, `MethodExtensionHandler`, and `MethodExtensionDecision` do not exist.

- [ ] **Step 3: Implement the annotation and handler interface**

Create `MethodExtension.java`:

```java
package top.egon.cola.component.methodextension.annotation;

import top.egon.cola.component.methodextension.handler.MethodExtensionHandler;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface MethodExtension {

    Class<? extends MethodExtensionHandler> handler();

    String returnJson() default "";
}
```

Create `MethodExtensionHandler.java`:

```java
package top.egon.cola.component.methodextension.handler;

import top.egon.cola.component.methodextension.context.MethodExtensionContext;

public interface MethodExtensionHandler {

    MethodExtensionDecision evaluate(MethodExtensionContext context) throws Exception;
}
```

- [ ] **Step 4: Implement the immutable context**

```java
package top.egon.cola.component.methodextension.context;

import java.lang.reflect.Method;
import java.util.Objects;

public final class MethodExtensionContext {

    private final Object target;

    private final Method method;

    private final Object[] arguments;

    public MethodExtensionContext(Object target, Method method, Object[] arguments) {
        this.target = Objects.requireNonNull(target, "target must not be null");
        this.method = Objects.requireNonNull(method, "method must not be null");
        this.arguments = arguments == null ? new Object[0] : arguments.clone();
    }

    public Object target() {
        return target;
    }

    public Method method() {
        return method;
    }

    public Object[] arguments() {
        return arguments.clone();
    }
}
```

- [ ] **Step 5: Implement the immutable decision factories**

```java
package top.egon.cola.component.methodextension.handler;

import java.util.Objects;

public final class MethodExtensionDecision {

    private final boolean allowed;

    private final boolean responseProvided;

    private final Object response;

    private final String reason;

    private MethodExtensionDecision(boolean allowed, boolean responseProvided, Object response, String reason) {
        this.allowed = allowed;
        this.responseProvided = responseProvided;
        this.response = response;
        this.reason = reason == null ? "" : reason;
    }

    public static MethodExtensionDecision allow() {
        return new MethodExtensionDecision(true, false, null, "");
    }

    public static MethodExtensionDecision reject() {
        return new MethodExtensionDecision(false, false, null, "");
    }

    public static MethodExtensionDecision rejectWithReason(String reason) {
        return new MethodExtensionDecision(false, false, null, reason);
    }

    public static MethodExtensionDecision reject(Object response) {
        return reject(response, "");
    }

    public static MethodExtensionDecision reject(Object response, String reason) {
        return new MethodExtensionDecision(
                false,
                true,
                Objects.requireNonNull(response, "response must not be null"),
                reason
        );
    }

    public boolean allowed() {
        return allowed;
    }

    public boolean responseProvided() {
        return responseProvided;
    }

    public Object response() {
        return response;
    }

    public String reason() {
        return reason;
    }
}
```

- [ ] **Step 6: Run the contract tests**

Run the command from Step 2 again.

Expected: all contract tests pass.

- [ ] **Step 7: Commit the public contracts**

```bash
git add egon-cola-components/egon-cola-component-method-extension/egon-cola-component-method-extension-starter/src/main/java/top/egon/cola/component/methodextension/annotation \
  egon-cola-components/egon-cola-component-method-extension/egon-cola-component-method-extension-starter/src/main/java/top/egon/cola/component/methodextension/context \
  egon-cola-components/egon-cola-component-method-extension/egon-cola-component-method-extension-starter/src/main/java/top/egon/cola/component/methodextension/handler/MethodExtensionDecision.java \
  egon-cola-components/egon-cola-component-method-extension/egon-cola-component-method-extension-starter/src/main/java/top/egon/cola/component/methodextension/handler/MethodExtensionHandler.java \
  egon-cola-components/egon-cola-component-method-extension/egon-cola-component-method-extension-starter/src/test
git commit -m "feat: add method extension contracts"
```

### Task 3: Resolve Exactly One Spring-Managed Handler

**Files:**
- Create: `egon-cola-components/egon-cola-component-method-extension/egon-cola-component-method-extension-starter/src/test/java/top/egon/cola/component/methodextension/handler/MethodExtensionHandlerResolverTest.java`
- Create: `egon-cola-components/egon-cola-component-method-extension/egon-cola-component-method-extension-starter/src/main/java/top/egon/cola/component/methodextension/exception/MethodExtensionException.java`
- Create: `egon-cola-components/egon-cola-component-method-extension/egon-cola-component-method-extension-starter/src/main/java/top/egon/cola/component/methodextension/exception/MethodExtensionConfigurationException.java`
- Create: `egon-cola-components/egon-cola-component-method-extension/egon-cola-component-method-extension-starter/src/main/java/top/egon/cola/component/methodextension/exception/MethodExtensionResponseException.java`
- Create: `egon-cola-components/egon-cola-component-method-extension/egon-cola-component-method-extension-starter/src/main/java/top/egon/cola/component/methodextension/handler/MethodExtensionHandlerResolver.java`

- [ ] **Step 1: Write the failing handler-resolution tests**

```java
package top.egon.cola.component.methodextension.handler;

import org.junit.jupiter.api.Test;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import top.egon.cola.component.methodextension.context.MethodExtensionContext;
import top.egon.cola.component.methodextension.exception.MethodExtensionConfigurationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MethodExtensionHandlerResolverTest {

    @Test
    void shouldResolveUniqueHandlerBean() {
        DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
        AllowHandler handler = new AllowHandler();
        beanFactory.registerSingleton("allowHandler", handler);

        MethodExtensionHandler result = new MethodExtensionHandlerResolver(beanFactory)
                .resolve(AllowHandler.class);

        assertThat(result).isSameAs(handler);
    }

    @Test
    void shouldResolveHandlerByItsTargetClassWhenBeanUsesJdkProxy() {
        DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
        ProxyFactory proxyFactory = new ProxyFactory(new AllowHandler());
        proxyFactory.setProxyTargetClass(false);
        MethodExtensionHandler proxy = (MethodExtensionHandler) proxyFactory.getProxy();
        beanFactory.registerSingleton("proxiedAllowHandler", proxy);

        MethodExtensionHandler result = new MethodExtensionHandlerResolver(beanFactory)
                .resolve(AllowHandler.class);

        assertThat(result).isSameAs(proxy);
    }

    @Test
    void shouldRejectMissingHandlerBean() {
        DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();

        assertThatThrownBy(() -> new MethodExtensionHandlerResolver(beanFactory).resolve(AllowHandler.class))
                .isInstanceOf(MethodExtensionConfigurationException.class)
                .hasMessageContaining("No MethodExtensionHandler bean found")
                .hasMessageContaining(AllowHandler.class.getName());
    }

    @Test
    void shouldRejectAmbiguousHandlerBeans() {
        DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
        beanFactory.registerSingleton("allowHandlerOne", new AllowHandler());
        beanFactory.registerSingleton("allowHandlerTwo", new AllowHandler());

        assertThatThrownBy(() -> new MethodExtensionHandlerResolver(beanFactory).resolve(AllowHandler.class))
                .isInstanceOf(MethodExtensionConfigurationException.class)
                .hasMessageContaining("Multiple MethodExtensionHandler beans found")
                .hasMessageContaining("allowHandlerOne")
                .hasMessageContaining("allowHandlerTwo");
    }

    static class AllowHandler implements MethodExtensionHandler {

        @Override
        public MethodExtensionDecision evaluate(MethodExtensionContext context) {
            return MethodExtensionDecision.allow();
        }
    }
}
```

- [ ] **Step 2: Run the test to verify the resolver and exceptions are missing**

```bash
./mvnw -B -ntp \
  -f egon-cola-components/egon-cola-component-method-extension/egon-cola-component-method-extension-starter/pom.xml \
  -Dtest=MethodExtensionHandlerResolverTest test
```

Expected: `testCompile` fails because the resolver and exception types do not exist.

- [ ] **Step 3: Add the component exception hierarchy**

Create `MethodExtensionException.java`:

```java
package top.egon.cola.component.methodextension.exception;

public class MethodExtensionException extends RuntimeException {

    public MethodExtensionException(String message) {
        super(message);
    }

    public MethodExtensionException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

Create `MethodExtensionConfigurationException.java`:

```java
package top.egon.cola.component.methodextension.exception;

public class MethodExtensionConfigurationException extends MethodExtensionException {

    public MethodExtensionConfigurationException(String message) {
        super(message);
    }

    public MethodExtensionConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

Create `MethodExtensionResponseException.java`:

```java
package top.egon.cola.component.methodextension.exception;

public class MethodExtensionResponseException extends MethodExtensionException {

    public MethodExtensionResponseException(String message) {
        super(message);
    }

    public MethodExtensionResponseException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

- [ ] **Step 4: Implement proxy-aware handler resolution**

```java
package top.egon.cola.component.methodextension.handler;

import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.ListableBeanFactory;
import top.egon.cola.component.methodextension.exception.MethodExtensionConfigurationException;

import java.util.List;
import java.util.Map;

public class MethodExtensionHandlerResolver {

    private final ListableBeanFactory beanFactory;

    public MethodExtensionHandlerResolver(ListableBeanFactory beanFactory) {
        this.beanFactory = beanFactory;
    }

    public MethodExtensionHandler resolve(Class<? extends MethodExtensionHandler> handlerType) {
        Map<String, MethodExtensionHandler> handlerBeans = beanFactory.getBeansOfType(MethodExtensionHandler.class);
        List<Map.Entry<String, MethodExtensionHandler>> matches = handlerBeans.entrySet().stream()
                .filter(entry -> matches(handlerType, entry.getValue()))
                .toList();
        if (matches.isEmpty()) {
            throw new MethodExtensionConfigurationException(
                    "No MethodExtensionHandler bean found for type " + handlerType.getName()
            );
        }
        if (matches.size() > 1) {
            String beanNames = matches.stream().map(Map.Entry::getKey).sorted().toList().toString();
            throw new MethodExtensionConfigurationException(
                    "Multiple MethodExtensionHandler beans found for type " + handlerType.getName()
                            + ": " + beanNames
            );
        }
        return matches.getFirst().getValue();
    }

    private boolean matches(Class<? extends MethodExtensionHandler> handlerType, MethodExtensionHandler handler) {
        return handlerType.isInstance(handler)
                || handlerType.isAssignableFrom(AopUtils.getTargetClass(handler));
    }
}
```

- [ ] **Step 5: Run the handler-resolution tests**

Run the command from Step 2 again.

Expected: all four tests pass, including the JDK-proxied handler case.

- [ ] **Step 6: Commit handler resolution**

```bash
git add egon-cola-components/egon-cola-component-method-extension/egon-cola-component-method-extension-starter/src/main/java/top/egon/cola/component/methodextension/exception \
  egon-cola-components/egon-cola-component-method-extension/egon-cola-component-method-extension-starter/src/main/java/top/egon/cola/component/methodextension/handler/MethodExtensionHandlerResolver.java \
  egon-cola-components/egon-cola-component-method-extension/egon-cola-component-method-extension-starter/src/test/java/top/egon/cola/component/methodextension/handler/MethodExtensionHandlerResolverTest.java
git commit -m "feat: resolve method extension handlers"
```

### Task 4: Resolve Direct and JSON Rejection Responses

**Files:**
- Create: `egon-cola-components/egon-cola-component-method-extension/egon-cola-component-method-extension-starter/src/test/java/top/egon/cola/component/methodextension/response/MethodExtensionResponseResolverTest.java`
- Create: `egon-cola-components/egon-cola-component-method-extension/egon-cola-component-method-extension-starter/src/main/java/top/egon/cola/component/methodextension/response/MethodExtensionResponseResolver.java`

- [ ] **Step 1: Write failing response-resolution tests**

```java
package top.egon.cola.component.methodextension.response;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import top.egon.cola.component.methodextension.exception.MethodExtensionConfigurationException;
import top.egon.cola.component.methodextension.exception.MethodExtensionResponseException;
import top.egon.cola.component.methodextension.handler.MethodExtensionDecision;

import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MethodExtensionResponseResolverTest {

    @Test
    void shouldPreferCompatibleDirectResponseWithoutObjectMapper() throws NoSuchMethodException {
        Method method = SampleMethods.class.getDeclaredMethod("payload");
        Payload response = new Payload("direct");

        Object result = resolver().resolve(method, MethodExtensionDecision.reject(response), "{\"code\":\"json\"}");

        assertThat(result).isSameAs(response);
    }

    @Test
    void shouldAcceptBoxedValueForPrimitiveReturnType() throws NoSuchMethodException {
        Method method = SampleMethods.class.getDeclaredMethod("count");

        Object result = resolver().resolve(method, MethodExtensionDecision.reject(7), "");

        assertThat(result).isEqualTo(7);
    }

    @Test
    void shouldReturnCompatibleAsyncWrapperProvidedByHandler() throws NoSuchMethodException {
        Method method = SampleMethods.class.getDeclaredMethod("asyncPayload");
        CompletableFuture<Payload> response = CompletableFuture.completedFuture(new Payload("direct"));

        Object result = resolver().resolve(method, MethodExtensionDecision.reject(response), "");

        assertThat(result).isSameAs(response);
    }

    @Test
    void shouldConvertJsonWithGenericReturnType() throws NoSuchMethodException {
        Method method = SampleMethods.class.getDeclaredMethod("payloads");

        Object result = resolver(new ObjectMapper()).resolve(
                method,
                MethodExtensionDecision.reject(),
                "[{\"code\":\"limited\"}]"
        );

        assertThat(result).isEqualTo(List.of(new Payload("limited")));
    }

    @Test
    void shouldReturnRawJsonForStringWithoutObjectMapper() throws NoSuchMethodException {
        Method method = SampleMethods.class.getDeclaredMethod("text");

        Object result = resolver().resolve(method, MethodExtensionDecision.reject(), "{\"code\":1111}");

        assertThat(result).isEqualTo("{\"code\":1111}");
    }

    @Test
    void shouldReturnNullForVoidRejectionWithoutJson() throws NoSuchMethodException {
        Method method = SampleMethods.class.getDeclaredMethod("nothing");

        Object result = resolver().resolve(method, MethodExtensionDecision.reject(), "");

        assertThat(result).isNull();
    }

    @Test
    void shouldRejectJsonForVoidReturnType() throws NoSuchMethodException {
        Method method = SampleMethods.class.getDeclaredMethod("nothing");

        assertThatThrownBy(() -> resolver().resolve(method, MethodExtensionDecision.reject(), "{}"))
                .isInstanceOf(MethodExtensionConfigurationException.class)
                .hasMessageContaining("void method");
    }

    @Test
    void shouldRequireObjectMapperOnlyForObjectJsonConversion() throws NoSuchMethodException {
        Method method = SampleMethods.class.getDeclaredMethod("payload");

        assertThatThrownBy(() -> resolver().resolve(method, MethodExtensionDecision.reject(), "{\"code\":\"x\"}"))
                .isInstanceOf(MethodExtensionConfigurationException.class)
                .hasMessageContaining("No ObjectMapper bean found");
    }

    @Test
    void shouldRejectMissingFallbackForNonVoidMethod() throws NoSuchMethodException {
        Method method = SampleMethods.class.getDeclaredMethod("payload");

        assertThatThrownBy(() -> resolver().resolve(method, MethodExtensionDecision.reject(), " "))
                .isInstanceOf(MethodExtensionConfigurationException.class)
                .hasMessageContaining("rejected without a response or returnJson");
    }

    @Test
    void shouldRejectAmbiguousObjectMappers() throws NoSuchMethodException {
        Method method = SampleMethods.class.getDeclaredMethod("payload");

        assertThatThrownBy(() -> resolver(new ObjectMapper(), new ObjectMapper())
                .resolve(method, MethodExtensionDecision.reject(), "{\"code\":\"x\"}"))
                .isInstanceOf(MethodExtensionConfigurationException.class)
                .hasMessageContaining("Multiple ObjectMapper beans found");
    }

    @Test
    void shouldRejectIncompatibleDirectResponse() throws NoSuchMethodException {
        Method method = SampleMethods.class.getDeclaredMethod("payload");

        assertThatThrownBy(() -> resolver().resolve(method, MethodExtensionDecision.reject("wrong"), ""))
                .isInstanceOf(MethodExtensionResponseException.class)
                .hasMessageContaining(String.class.getName())
                .hasMessageContaining(Payload.class.getName());
    }

    @Test
    void shouldRejectMalformedJson() throws NoSuchMethodException {
        Method method = SampleMethods.class.getDeclaredMethod("payload");

        assertThatThrownBy(() -> resolver(new ObjectMapper())
                .resolve(method, MethodExtensionDecision.reject(), "not-json"))
                .isInstanceOf(MethodExtensionResponseException.class)
                .hasMessageContaining("Failed to convert returnJson");
    }

    @Test
    void shouldRejectUnresolvedTypeVariable() throws NoSuchMethodException {
        Method method = SampleMethods.class.getDeclaredMethod("generic");

        assertThatThrownBy(() -> resolver(new ObjectMapper())
                .resolve(method, MethodExtensionDecision.reject(), "{}"))
                .isInstanceOf(MethodExtensionResponseException.class)
                .hasMessageContaining("unresolved type variable");
    }

    private MethodExtensionResponseResolver resolver(ObjectMapper... objectMappers) {
        DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
        for (int index = 0; index < objectMappers.length; index++) {
            beanFactory.registerSingleton("objectMapper" + index, objectMappers[index]);
        }
        return new MethodExtensionResponseResolver(beanFactory.getBeanProvider(ObjectMapper.class));
    }

    static class SampleMethods {

        Payload payload() {
            return null;
        }

        int count() {
            return 0;
        }

        List<Payload> payloads() {
            return List.of();
        }

        CompletableFuture<Payload> asyncPayload() {
            return CompletableFuture.completedFuture(new Payload("business"));
        }

        String text() {
            return "";
        }

        void nothing() {
        }

        <T> T generic() {
            return null;
        }
    }

    record Payload(String code) {
    }
}
```

- [ ] **Step 2: Run the response test to verify it fails**

```bash
./mvnw -B -ntp \
  -f egon-cola-components/egon-cola-component-method-extension/egon-cola-component-method-extension-starter/pom.xml \
  -Dtest=MethodExtensionResponseResolverTest test
```

Expected: `testCompile` fails because `MethodExtensionResponseResolver` does not exist.

- [ ] **Step 3: Implement response priority, compatibility, and generic JSON conversion**

```java
package top.egon.cola.component.methodextension.response;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;
import top.egon.cola.component.methodextension.exception.MethodExtensionConfigurationException;
import top.egon.cola.component.methodextension.exception.MethodExtensionResponseException;
import top.egon.cola.component.methodextension.handler.MethodExtensionDecision;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.List;

public class MethodExtensionResponseResolver {

    private final ObjectProvider<ObjectMapper> objectMappers;

    public MethodExtensionResponseResolver(ObjectProvider<ObjectMapper> objectMappers) {
        this.objectMappers = objectMappers;
    }

    public Object resolve(Method method, MethodExtensionDecision decision, String returnJson) {
        if (decision.allowed()) {
            throw new MethodExtensionConfigurationException(
                    "Cannot resolve a rejection response from an allow decision for " + method.toGenericString()
            );
        }
        if (decision.responseProvided()) {
            return validateDirectResponse(method, decision.response());
        }
        if (method.getReturnType() == Void.TYPE || method.getReturnType() == Void.class) {
            if (StringUtils.hasText(returnJson)) {
                throw new MethodExtensionConfigurationException(
                        "returnJson must be empty for void method " + method.toGenericString()
                );
            }
            return null;
        }
        if (!StringUtils.hasText(returnJson)) {
            throw new MethodExtensionConfigurationException(
                    "Method extension rejected without a response or returnJson for " + method.toGenericString()
            );
        }
        if (method.getReturnType() == String.class) {
            return returnJson;
        }
        Type genericReturnType = method.getGenericReturnType();
        if (containsTypeVariable(genericReturnType)) {
            throw new MethodExtensionResponseException(
                    "Cannot convert returnJson for unresolved type variable on " + method.toGenericString()
            );
        }
        ObjectMapper objectMapper = requireUniqueObjectMapper(method);
        try {
            JavaType javaType = objectMapper.getTypeFactory().constructType(genericReturnType);
            return objectMapper.readerFor(javaType).readValue(returnJson);
        } catch (JsonProcessingException | IllegalArgumentException exception) {
            throw new MethodExtensionResponseException(
                    "Failed to convert returnJson for " + method.toGenericString(),
                    exception
            );
        }
    }

    private Object validateDirectResponse(Method method, Object response) {
        if (!ClassUtils.isAssignableValue(method.getReturnType(), response)) {
            throw new MethodExtensionResponseException(
                    "Method extension response type " + response.getClass().getName()
                            + " is not assignable to " + method.getReturnType().getName()
                            + " for " + method.toGenericString()
            );
        }
        return response;
    }

    private ObjectMapper requireUniqueObjectMapper(Method method) {
        List<ObjectMapper> candidates = objectMappers.orderedStream().toList();
        if (candidates.isEmpty()) {
            throw new MethodExtensionConfigurationException(
                    "No ObjectMapper bean found for returnJson conversion on " + method.toGenericString()
            );
        }
        if (candidates.size() > 1) {
            throw new MethodExtensionConfigurationException(
                    "Multiple ObjectMapper beans found for returnJson conversion on " + method.toGenericString()
            );
        }
        return candidates.getFirst();
    }

    private boolean containsTypeVariable(Type type) {
        if (type instanceof TypeVariable<?>) {
            return true;
        }
        if (type instanceof ParameterizedType parameterizedType) {
            if (containsTypeVariable(parameterizedType.getRawType())) {
                return true;
            }
            for (Type argument : parameterizedType.getActualTypeArguments()) {
                if (containsTypeVariable(argument)) {
                    return true;
                }
            }
        }
        if (type instanceof GenericArrayType genericArrayType) {
            return containsTypeVariable(genericArrayType.getGenericComponentType());
        }
        if (type instanceof WildcardType wildcardType) {
            for (Type upperBound : wildcardType.getUpperBounds()) {
                if (containsTypeVariable(upperBound)) {
                    return true;
                }
            }
            for (Type lowerBound : wildcardType.getLowerBounds()) {
                if (containsTypeVariable(lowerBound)) {
                    return true;
                }
            }
        }
        return false;
    }
}
```

- [ ] **Step 4: Run the response-resolution tests**

Run the command from Step 2 again.

Expected: all response tests pass.

- [ ] **Step 5: Run all starter tests accumulated so far**

```bash
./mvnw -B -ntp \
  -f egon-cola-components/egon-cola-component-method-extension/egon-cola-component-method-extension-starter/pom.xml test
```

Expected: `BUILD SUCCESS` with all contract, handler, and response tests passing.

- [ ] **Step 6: Commit response resolution**

```bash
git add egon-cola-components/egon-cola-component-method-extension/egon-cola-component-method-extension-starter/src/main/java/top/egon/cola/component/methodextension/response \
  egon-cola-components/egon-cola-component-method-extension/egon-cola-component-method-extension-starter/src/test/java/top/egon/cola/component/methodextension/response
git commit -m "feat: resolve method extension responses"
```

### Task 5: Resolve Proxy, Interface, Bridge, and Inherited Methods

**Files:**
- Create: `egon-cola-components/egon-cola-component-method-extension/egon-cola-component-method-extension-starter/src/test/java/top/egon/cola/component/methodextension/support/MethodExtensionMethodResolverTest.java`
- Create: `egon-cola-components/egon-cola-component-method-extension/egon-cola-component-method-extension-starter/src/main/java/top/egon/cola/component/methodextension/support/MethodExtensionMethodResolver.java`

- [ ] **Step 1: Write failing method-resolution tests**

```java
package top.egon.cola.component.methodextension.support;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.methodextension.annotation.MethodExtension;
import top.egon.cola.component.methodextension.context.MethodExtensionContext;
import top.egon.cola.component.methodextension.exception.MethodExtensionConfigurationException;
import top.egon.cola.component.methodextension.handler.MethodExtensionDecision;
import top.egon.cola.component.methodextension.handler.MethodExtensionHandler;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MethodExtensionMethodResolverTest {

    private final MethodExtensionMethodResolver resolver = new MethodExtensionMethodResolver();

    @Test
    void shouldPreferImplementationAnnotation() throws NoSuchMethodException {
        Method invoked = ServiceContract.class.getMethod("call", String.class);

        MethodExtensionMethodResolver.ResolvedMethodExtension resolved = resolver.resolve(invoked, new ServiceImpl());

        assertThat(resolved.method().getDeclaringClass()).isEqualTo(ServiceImpl.class);
        assertThat(resolved.annotation().handler()).isEqualTo(ImplementationHandler.class);
    }

    @Test
    void shouldFallBackToInterfaceAnnotation() throws NoSuchMethodException {
        Method invoked = InterfaceOnlyContract.class.getMethod("call", String.class);

        MethodExtensionMethodResolver.ResolvedMethodExtension resolved = resolver.resolve(
                invoked,
                new InterfaceOnlyImpl()
        );

        assertThat(resolved.method().getDeclaringClass()).isEqualTo(InterfaceOnlyImpl.class);
        assertThat(resolved.annotation().handler()).isEqualTo(InterfaceHandler.class);
    }

    @Test
    void shouldResolveGenericBridgeMethod() throws NoSuchMethodException {
        Method invoked = GenericContract.class.getMethod("convert", Object.class);

        MethodExtensionMethodResolver.ResolvedMethodExtension resolved = resolver.resolve(invoked, new StringService());

        assertThat(resolved.method().getParameterTypes()).containsExactly(String.class);
        assertThat(resolved.annotation().handler()).isEqualTo(ImplementationHandler.class);
    }

    @Test
    void shouldResolveInheritedAnnotation() throws NoSuchMethodException {
        Method invoked = BaseService.class.getMethod("inherited", String.class);

        MethodExtensionMethodResolver.ResolvedMethodExtension resolved = resolver.resolve(invoked, new ChildService());

        assertThat(resolved.method()).isEqualTo(invoked);
        assertThat(resolved.annotation().handler()).isEqualTo(InterfaceHandler.class);
    }

    @Test
    void shouldRejectMethodWithoutAnnotation() throws NoSuchMethodException {
        Method invoked = PlainService.class.getMethod("call", String.class);

        assertThatThrownBy(() -> resolver.resolve(invoked, new PlainService()))
                .isInstanceOf(MethodExtensionConfigurationException.class)
                .hasMessageContaining("No @MethodExtension found");
    }

    interface ServiceContract {

        @MethodExtension(handler = InterfaceHandler.class)
        String call(String value);
    }

    static class ServiceImpl implements ServiceContract {

        @Override
        @MethodExtension(handler = ImplementationHandler.class)
        public String call(String value) {
            return value;
        }
    }

    interface InterfaceOnlyContract {

        @MethodExtension(handler = InterfaceHandler.class)
        String call(String value);
    }

    static class InterfaceOnlyImpl implements InterfaceOnlyContract {

        @Override
        public String call(String value) {
            return value;
        }
    }

    interface GenericContract<T> {

        T convert(T value);
    }

    static class StringService implements GenericContract<String> {

        @Override
        @MethodExtension(handler = ImplementationHandler.class)
        public String convert(String value) {
            return value;
        }
    }

    static class BaseService {

        @MethodExtension(handler = InterfaceHandler.class)
        public String inherited(String value) {
            return value;
        }
    }

    static class ChildService extends BaseService {
    }

    static class PlainService {

        public String call(String value) {
            return value;
        }
    }

    static class InterfaceHandler implements MethodExtensionHandler {

        @Override
        public MethodExtensionDecision evaluate(MethodExtensionContext context) {
            return MethodExtensionDecision.allow();
        }
    }

    static class ImplementationHandler implements MethodExtensionHandler {

        @Override
        public MethodExtensionDecision evaluate(MethodExtensionContext context) {
            return MethodExtensionDecision.allow();
        }
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

```bash
./mvnw -B -ntp \
  -f egon-cola-components/egon-cola-component-method-extension/egon-cola-component-method-extension-starter/pom.xml \
  -Dtest=MethodExtensionMethodResolverTest test
```

Expected: `testCompile` fails because `MethodExtensionMethodResolver` does not exist.

- [ ] **Step 3: Implement most-specific method and merged-annotation resolution**

```java
package top.egon.cola.component.methodextension.support;

import org.springframework.aop.support.AopUtils;
import org.springframework.core.BridgeMethodResolver;
import org.springframework.core.annotation.AnnotatedElementUtils;
import top.egon.cola.component.methodextension.annotation.MethodExtension;
import top.egon.cola.component.methodextension.exception.MethodExtensionConfigurationException;

import java.lang.reflect.Method;
import java.util.Objects;

public class MethodExtensionMethodResolver {

    public ResolvedMethodExtension resolve(Method invokedMethod, Object target) {
        Objects.requireNonNull(invokedMethod, "invokedMethod must not be null");
        Objects.requireNonNull(target, "target must not be null");
        Class<?> targetClass = AopUtils.getTargetClass(target);
        Method specificMethod = AopUtils.getMostSpecificMethod(invokedMethod, targetClass);
        Method userMethod = BridgeMethodResolver.findBridgedMethod(specificMethod);
        MethodExtension annotation = AnnotatedElementUtils.findMergedAnnotation(userMethod, MethodExtension.class);
        if (annotation == null) {
            Method interfaceMethod = BridgeMethodResolver.findBridgedMethod(invokedMethod);
            annotation = AnnotatedElementUtils.findMergedAnnotation(interfaceMethod, MethodExtension.class);
        }
        if (annotation == null) {
            throw new MethodExtensionConfigurationException(
                    "No @MethodExtension found for " + userMethod.toGenericString()
            );
        }
        return new ResolvedMethodExtension(userMethod, annotation);
    }

    public record ResolvedMethodExtension(Method method, MethodExtension annotation) {
    }
}
```

- [ ] **Step 4: Run the method-resolution tests**

Run the command from Step 2 again.

Expected: all five method-resolution tests pass.

- [ ] **Step 5: Commit method resolution**

```bash
git add egon-cola-components/egon-cola-component-method-extension/egon-cola-component-method-extension-starter/src/main/java/top/egon/cola/component/methodextension/support \
  egon-cola-components/egon-cola-component-method-extension/egon-cola-component-method-extension-starter/src/test/java/top/egon/cola/component/methodextension/support
git commit -m "feat: resolve annotated proxy methods"
```

### Task 6: Implement the AOP Allow, Reject, and Failure Flow

**Files:**
- Create: `egon-cola-components/egon-cola-component-method-extension/egon-cola-component-method-extension-starter/src/test/java/top/egon/cola/component/methodextension/aop/MethodExtensionAopTest.java`
- Create: `egon-cola-components/egon-cola-component-method-extension/egon-cola-component-method-extension-starter/src/main/java/top/egon/cola/component/methodextension/autoconfigure/MethodExtensionProperties.java`
- Create: `egon-cola-components/egon-cola-component-method-extension/egon-cola-component-method-extension-starter/src/main/java/top/egon/cola/component/methodextension/aop/MethodExtensionAop.java`

- [ ] **Step 1: Write failing AOP flow tests**

```java
package top.egon.cola.component.methodextension.aop;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import top.egon.cola.component.methodextension.annotation.MethodExtension;
import top.egon.cola.component.methodextension.autoconfigure.MethodExtensionProperties;
import top.egon.cola.component.methodextension.context.MethodExtensionContext;
import top.egon.cola.component.methodextension.exception.MethodExtensionConfigurationException;
import top.egon.cola.component.methodextension.handler.MethodExtensionDecision;
import top.egon.cola.component.methodextension.handler.MethodExtensionHandler;
import top.egon.cola.component.methodextension.handler.MethodExtensionHandlerResolver;
import top.egon.cola.component.methodextension.response.MethodExtensionResponseResolver;
import top.egon.cola.component.methodextension.support.MethodExtensionMethodResolver;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(OutputCaptureExtension.class)
class MethodExtensionAopTest {

    private final HandlerFailure handlerFailure = new HandlerFailure("handler failed");

    private final DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();

    @BeforeEach
    void setUp() {
        beanFactory.registerSingleton("objectMapper", new ObjectMapper());
        beanFactory.registerSingleton("allowHandler", new AllowHandler());
        beanFactory.registerSingleton("directRejectHandler", new DirectRejectHandler());
        beanFactory.registerSingleton("jsonRejectHandler", new JsonRejectHandler());
        beanFactory.registerSingleton("nullDecisionHandler", new NullDecisionHandler());
        beanFactory.registerSingleton("throwingHandler", new ThrowingHandler(handlerFailure));
    }

    @Test
    void shouldRunHandlerThenInvokeBusinessExactlyOnceWhenAllowed() {
        SampleService target = new SampleService();
        SampleService proxy = proxy(target, -100);

        String result = proxy.allowed("u-001");

        assertThat(result).isEqualTo("allowed:u-001");
        assertThat(target.businessCalls).hasValue(1);
    }

    @Test
    void shouldSkipBusinessAndReturnDirectResponseWhenRejected() {
        SampleService target = new SampleService();
        SampleService proxy = proxy(target, -100);

        Payload result = proxy.direct();

        assertThat(result).isEqualTo(new Payload("direct"));
        assertThat(target.businessCalls).hasValue(0);
    }

    @Test
    void shouldSkipBusinessAndConvertReturnJsonWhenRejected() {
        SampleService target = new SampleService();
        SampleService proxy = proxy(target, -100);

        Payload result = proxy.json("u-001");

        assertThat(result).isEqualTo(new Payload("json"));
        assertThat(target.businessCalls).hasValue(0);
    }

    @Test
    void shouldLogRejectionMetadataWithoutArgumentsOrReturnJson(CapturedOutput output) {
        SampleService target = new SampleService();
        SampleService proxy = proxy(target, -100);

        proxy.json("sensitive-user");

        assertThat(output.getOut())
                .contains("Method extension rejected")
                .contains(JsonRejectHandler.class.getName())
                .doesNotContain("sensitive-user")
                .doesNotContain("{\"code\":\"json\"}");
    }

    @Test
    void shouldRejectNullDecisionWithoutCallingBusiness() {
        SampleService target = new SampleService();
        SampleService proxy = proxy(target, -100);

        assertThatThrownBy(proxy::nullDecision)
                .isInstanceOf(MethodExtensionConfigurationException.class)
                .hasMessageContaining("returned null");
        assertThat(target.businessCalls).hasValue(0);
    }

    @Test
    void shouldPropagateOriginalHandlerFailureWithoutCallingBusiness() {
        SampleService target = new SampleService();
        SampleService proxy = proxy(target, -100);

        assertThatThrownBy(proxy::failure).isSameAs(handlerFailure);
        assertThat(target.businessCalls).hasValue(0);
    }

    @Test
    void shouldExposeConfiguredAspectOrder() {
        MethodExtensionAop aop = aop(-77);

        assertThat(aop.getOrder()).isEqualTo(-77);
    }

    private SampleService proxy(SampleService target, int order) {
        AspectJProxyFactory proxyFactory = new AspectJProxyFactory(target);
        proxyFactory.addAspect(aop(order));
        return proxyFactory.getProxy();
    }

    private MethodExtensionAop aop(int order) {
        MethodExtensionProperties properties = new MethodExtensionProperties();
        properties.setOrder(order);
        return new MethodExtensionAop(
                properties,
                new MethodExtensionMethodResolver(),
                new MethodExtensionHandlerResolver(beanFactory),
                new MethodExtensionResponseResolver(beanFactory.getBeanProvider(ObjectMapper.class))
        );
    }

    public static class SampleService {

        private final AtomicInteger businessCalls = new AtomicInteger();

        @MethodExtension(handler = AllowHandler.class)
        public String allowed(String userId) {
            businessCalls.incrementAndGet();
            return "allowed:" + userId;
        }

        @MethodExtension(handler = DirectRejectHandler.class)
        public Payload direct() {
            businessCalls.incrementAndGet();
            return new Payload("business");
        }

        @MethodExtension(handler = JsonRejectHandler.class, returnJson = "{\"code\":\"json\"}")
        public Payload json(String userId) {
            businessCalls.incrementAndGet();
            return new Payload("business:" + userId);
        }

        @MethodExtension(handler = NullDecisionHandler.class)
        public String nullDecision() {
            businessCalls.incrementAndGet();
            return "business";
        }

        @MethodExtension(handler = ThrowingHandler.class)
        public String failure() {
            businessCalls.incrementAndGet();
            return "business";
        }
    }

    public static class AllowHandler implements MethodExtensionHandler {

        @Override
        public MethodExtensionDecision evaluate(MethodExtensionContext context) {
            return MethodExtensionDecision.allow();
        }
    }

    public static class DirectRejectHandler implements MethodExtensionHandler {

        @Override
        public MethodExtensionDecision evaluate(MethodExtensionContext context) {
            return MethodExtensionDecision.reject(new Payload("direct"), "direct rejection");
        }
    }

    public static class JsonRejectHandler implements MethodExtensionHandler {

        @Override
        public MethodExtensionDecision evaluate(MethodExtensionContext context) {
            return MethodExtensionDecision.rejectWithReason("json rejection");
        }
    }

    public static class NullDecisionHandler implements MethodExtensionHandler {

        @Override
        public MethodExtensionDecision evaluate(MethodExtensionContext context) {
            return null;
        }
    }

    public static class ThrowingHandler implements MethodExtensionHandler {

        private final HandlerFailure failure;

        ThrowingHandler(HandlerFailure failure) {
            this.failure = failure;
        }

        @Override
        public MethodExtensionDecision evaluate(MethodExtensionContext context) {
            throw failure;
        }
    }

    public record Payload(String code) {
    }

    static class HandlerFailure extends RuntimeException {

        HandlerFailure(String message) {
            super(message);
        }
    }
}
```

- [ ] **Step 2: Run the AOP test to verify production types are missing**

```bash
./mvnw -B -ntp \
  -f egon-cola-components/egon-cola-component-method-extension/egon-cola-component-method-extension-starter/pom.xml \
  -Dtest=MethodExtensionAopTest test
```

Expected: `testCompile` fails because `MethodExtensionAop` and `MethodExtensionProperties` do not exist.

- [ ] **Step 3: Implement the component properties**

```java
package top.egon.cola.component.methodextension.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.Ordered;

@ConfigurationProperties(prefix = "egon.cola.component.method-extension", ignoreInvalidFields = true)
public class MethodExtensionProperties {

    private boolean enabled = true;

    private int order = Ordered.HIGHEST_PRECEDENCE + 100;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }
}
```

- [ ] **Step 4: Implement the aspect orchestration and logging**

```java
package top.egon.cola.component.methodextension.aop;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import top.egon.cola.component.methodextension.autoconfigure.MethodExtensionProperties;
import top.egon.cola.component.methodextension.context.MethodExtensionContext;
import top.egon.cola.component.methodextension.exception.MethodExtensionConfigurationException;
import top.egon.cola.component.methodextension.exception.MethodExtensionException;
import top.egon.cola.component.methodextension.handler.MethodExtensionDecision;
import top.egon.cola.component.methodextension.handler.MethodExtensionHandler;
import top.egon.cola.component.methodextension.handler.MethodExtensionHandlerResolver;
import top.egon.cola.component.methodextension.response.MethodExtensionResponseResolver;
import top.egon.cola.component.methodextension.support.MethodExtensionMethodResolver;

import java.lang.reflect.Method;

@Aspect
public class MethodExtensionAop implements Ordered {

    private static final Logger LOGGER = LoggerFactory.getLogger(MethodExtensionAop.class);

    private final MethodExtensionProperties properties;

    private final MethodExtensionMethodResolver methodResolver;

    private final MethodExtensionHandlerResolver handlerResolver;

    private final MethodExtensionResponseResolver responseResolver;

    public MethodExtensionAop(
            MethodExtensionProperties properties,
            MethodExtensionMethodResolver methodResolver,
            MethodExtensionHandlerResolver handlerResolver,
            MethodExtensionResponseResolver responseResolver
    ) {
        this.properties = properties;
        this.methodResolver = methodResolver;
        this.handlerResolver = handlerResolver;
        this.responseResolver = responseResolver;
    }

    @Around("execution(public * *(..)) && @annotation(top.egon.cola.component.methodextension.annotation.MethodExtension)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        Method invokedMethod = ((MethodSignature) joinPoint.getSignature()).getMethod();
        MethodExtensionMethodResolver.ResolvedMethodExtension resolved;
        MethodExtensionHandler handler;
        try {
            resolved = methodResolver.resolve(invokedMethod, joinPoint.getTarget());
            handler = handlerResolver.resolve(resolved.annotation().handler());
        } catch (MethodExtensionException exception) {
            LOGGER.error("Invalid method extension configuration for {}", invokedMethod.toGenericString(), exception);
            throw exception;
        }

        Method method = resolved.method();
        Class<? extends MethodExtensionHandler> handlerType = resolved.annotation().handler();
        LOGGER.debug("Matched method extension {} with handler {}", method.toGenericString(), handlerType.getName());
        MethodExtensionDecision decision;
        try {
            LOGGER.debug("Executing method extension handler {}", handlerType.getName());
            decision = handler.evaluate(new MethodExtensionContext(joinPoint.getTarget(), method, joinPoint.getArgs()));
        } catch (Throwable exception) {
            LOGGER.error(
                    "Method extension handler {} failed for {}",
                    handlerType.getName(),
                    method.toGenericString(),
                    exception
            );
            throw exception;
        }
        if (decision == null) {
            MethodExtensionConfigurationException exception = new MethodExtensionConfigurationException(
                    "MethodExtensionHandler " + handlerType.getName() + " returned null for " + method.toGenericString()
            );
            LOGGER.error("Invalid method extension decision for {}", method.toGenericString(), exception);
            throw exception;
        }
        if (decision.allowed()) {
            LOGGER.debug("Method extension allowed {}", method.toGenericString());
            return joinPoint.proceed();
        }

        LOGGER.info(
                "Method extension rejected {} with handler {} and reason [{}]",
                method.toGenericString(),
                handlerType.getName(),
                decision.reason()
        );
        try {
            return responseResolver.resolve(method, decision, resolved.annotation().returnJson());
        } catch (MethodExtensionException exception) {
            LOGGER.error(
                    "Invalid method extension response for {}: {}",
                    method.toGenericString(),
                    exception.getMessage()
            );
            throw exception;
        }
    }

    @Override
    public int getOrder() {
        return properties.getOrder();
    }
}
```

- [ ] **Step 5: Run the AOP flow tests**

Run the command from Step 2 again.

Expected: all seven AOP tests pass; the handler exception assertion proves the original exception instance is propagated, and captured output contains no argument or JSON body.

- [ ] **Step 6: Run all starter tests**

```bash
./mvnw -B -ntp \
  -f egon-cola-components/egon-cola-component-method-extension/egon-cola-component-method-extension-starter/pom.xml test
```

Expected: `BUILD SUCCESS` with all accumulated starter tests passing.

- [ ] **Step 7: Commit the AOP flow**

```bash
git add egon-cola-components/egon-cola-component-method-extension/egon-cola-component-method-extension-starter/src/main/java/top/egon/cola/component/methodextension/aop \
  egon-cola-components/egon-cola-component-method-extension/egon-cola-component-method-extension-starter/src/main/java/top/egon/cola/component/methodextension/autoconfigure/MethodExtensionProperties.java \
  egon-cola-components/egon-cola-component-method-extension/egon-cola-component-method-extension-starter/src/test/java/top/egon/cola/component/methodextension/aop
git commit -m "feat: intercept method extension decisions"
```

### Task 7: Add Spring Boot Auto-Configuration

**Files:**
- Create: `egon-cola-components/egon-cola-component-method-extension/egon-cola-component-method-extension-starter/src/test/java/top/egon/cola/component/methodextension/autoconfigure/MethodExtensionAutoConfigurationTest.java`
- Create: `egon-cola-components/egon-cola-component-method-extension/egon-cola-component-method-extension-starter/src/main/java/top/egon/cola/component/methodextension/autoconfigure/MethodExtensionAutoConfiguration.java`
- Create: `egon-cola-components/egon-cola-component-method-extension/egon-cola-component-method-extension-starter/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`

- [ ] **Step 1: Write failing auto-configuration tests**

```java
package top.egon.cola.component.methodextension.autoconfigure;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.Ordered;
import top.egon.cola.component.methodextension.aop.MethodExtensionAop;
import top.egon.cola.component.methodextension.handler.MethodExtensionHandlerResolver;
import top.egon.cola.component.methodextension.response.MethodExtensionResponseResolver;
import top.egon.cola.component.methodextension.support.MethodExtensionMethodResolver;

import static org.assertj.core.api.Assertions.assertThat;

class MethodExtensionAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(MethodExtensionAutoConfiguration.class));

    @Test
    void shouldCreateCoreBeansWithoutObjectMapper() {
        contextRunner.run(context -> {
            assertThat(context)
                    .hasSingleBean(MethodExtensionProperties.class)
                    .hasSingleBean(MethodExtensionMethodResolver.class)
                    .hasSingleBean(MethodExtensionHandlerResolver.class)
                    .hasSingleBean(MethodExtensionResponseResolver.class)
                    .hasSingleBean(MethodExtensionAop.class);
            MethodExtensionProperties properties = context.getBean(MethodExtensionProperties.class);
            assertThat(properties.isEnabled()).isTrue();
            assertThat(properties.getOrder()).isEqualTo(Ordered.HIGHEST_PRECEDENCE + 100);
        });
    }

    @Test
    void shouldDisableAutoConfigurationByProperty() {
        contextRunner.withPropertyValues("egon.cola.component.method-extension.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(MethodExtensionAop.class));
    }

    @Test
    void shouldBindConfiguredAspectOrder() {
        contextRunner.withPropertyValues("egon.cola.component.method-extension.order=-77")
                .run(context -> {
                    assertThat(context.getBean(MethodExtensionProperties.class).getOrder()).isEqualTo(-77);
                    assertThat(context.getBean(MethodExtensionAop.class).getOrder()).isEqualTo(-77);
                });
    }
}
```

- [ ] **Step 2: Run the test to verify auto-configuration is missing**

```bash
./mvnw -B -ntp \
  -f egon-cola-components/egon-cola-component-method-extension/egon-cola-component-method-extension-starter/pom.xml \
  -Dtest=MethodExtensionAutoConfigurationTest test
```

Expected: `testCompile` fails because `MethodExtensionAutoConfiguration` does not exist.

- [ ] **Step 3: Implement conditional, override-friendly auto-configuration**

```java
package top.egon.cola.component.methodextension.autoconfigure;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import top.egon.cola.component.methodextension.aop.MethodExtensionAop;
import top.egon.cola.component.methodextension.handler.MethodExtensionHandlerResolver;
import top.egon.cola.component.methodextension.response.MethodExtensionResponseResolver;
import top.egon.cola.component.methodextension.support.MethodExtensionMethodResolver;

@AutoConfiguration
@EnableConfigurationProperties(MethodExtensionProperties.class)
@ConditionalOnProperty(
        prefix = "egon.cola.component.method-extension",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class MethodExtensionAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public MethodExtensionMethodResolver methodExtensionMethodResolver() {
        return new MethodExtensionMethodResolver();
    }

    @Bean
    @ConditionalOnMissingBean
    public MethodExtensionHandlerResolver methodExtensionHandlerResolver(ListableBeanFactory beanFactory) {
        return new MethodExtensionHandlerResolver(beanFactory);
    }

    @Bean
    @ConditionalOnMissingBean
    public MethodExtensionResponseResolver methodExtensionResponseResolver(ObjectProvider<ObjectMapper> objectMappers) {
        return new MethodExtensionResponseResolver(objectMappers);
    }

    @Bean
    @ConditionalOnMissingBean
    public MethodExtensionAop methodExtensionAop(
            MethodExtensionProperties properties,
            MethodExtensionMethodResolver methodResolver,
            MethodExtensionHandlerResolver handlerResolver,
            MethodExtensionResponseResolver responseResolver
    ) {
        return new MethodExtensionAop(properties, methodResolver, handlerResolver, responseResolver);
    }
}
```

- [ ] **Step 4: Register the auto-configuration import**

Create `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` with exactly:

```text
top.egon.cola.component.methodextension.autoconfigure.MethodExtensionAutoConfiguration
```

- [ ] **Step 5: Run the auto-configuration tests**

Run the command from Step 2 again.

Expected: all three tests pass, including context creation without an `ObjectMapper` Bean.

- [ ] **Step 6: Run the starter test suite**

```bash
./mvnw -B -ntp \
  -f egon-cola-components/egon-cola-component-method-extension/egon-cola-component-method-extension-starter/pom.xml test
```

Expected: `BUILD SUCCESS`.

- [ ] **Step 7: Commit auto-configuration**

```bash
git add egon-cola-components/egon-cola-component-method-extension/egon-cola-component-method-extension-starter/src/main/java/top/egon/cola/component/methodextension/autoconfigure \
  egon-cola-components/egon-cola-component-method-extension/egon-cola-component-method-extension-starter/src/main/resources \
  egon-cola-components/egon-cola-component-method-extension/egon-cola-component-method-extension-starter/src/test/java/top/egon/cola/component/methodextension/autoconfigure
git commit -m "feat: auto-configure method extension"
```

### Task 8: Prove JDK/CGLIB Proxy Behavior and Self-Invocation Limits

**Files:**
- Create: `egon-cola-components/egon-cola-component-method-extension/egon-cola-component-method-extension-starter/src/test/java/top/egon/cola/component/methodextension/aop/MethodExtensionProxyIntegrationTest.java`

- [ ] **Step 1: Write real Spring-proxy integration tests**

```java
package top.egon.cola.component.methodextension.aop;

import org.junit.jupiter.api.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.aop.AopAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import top.egon.cola.component.methodextension.annotation.MethodExtension;
import top.egon.cola.component.methodextension.autoconfigure.MethodExtensionAutoConfiguration;
import top.egon.cola.component.methodextension.context.MethodExtensionContext;
import top.egon.cola.component.methodextension.handler.MethodExtensionDecision;
import top.egon.cola.component.methodextension.handler.MethodExtensionHandler;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class MethodExtensionProxyIntegrationTest {

    private final ApplicationContextRunner jdkRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(AopAutoConfiguration.class, MethodExtensionAutoConfiguration.class))
            .withPropertyValues("spring.aop.proxy-target-class=false")
            .withUserConfiguration(JdkProxyConfiguration.class);

    private final ApplicationContextRunner cglibRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(AopAutoConfiguration.class, MethodExtensionAutoConfiguration.class))
            .withPropertyValues("spring.aop.proxy-target-class=true")
            .withUserConfiguration(CglibProxyConfiguration.class);

    @Test
    void shouldInterceptAnnotationDeclaredOnJdkProxyInterface() {
        jdkRunner.run(context -> {
            InterfaceAnnotatedService service = context.getBean(InterfaceAnnotatedService.class);
            CountingHandler handler = context.getBean(CountingHandler.class);

            assertThat(AopUtils.isJdkDynamicProxy(service)).isTrue();
            assertThat(service.call("value")).isEqualTo("interface:value");
            assertThat(handler.calls).hasValue(1);
        });
    }

    @Test
    void shouldInterceptImplementationAnnotationBehindJdkProxy() {
        jdkRunner.run(context -> {
            ImplementationAnnotatedService service = context.getBean(ImplementationAnnotatedService.class);
            CountingHandler handler = context.getBean(CountingHandler.class);

            assertThat(AopUtils.isJdkDynamicProxy(service)).isTrue();
            assertThat(service.call("value")).isEqualTo("implementation:value");
            assertThat(handler.calls).hasValue(1);
        });
    }

    @Test
    void shouldLeaveUnannotatedMethodUnaffected() {
        jdkRunner.run(context -> {
            InterfaceAnnotatedService service = context.getBean(InterfaceAnnotatedService.class);
            CountingHandler handler = context.getBean(CountingHandler.class);

            assertThat(service.plain("value")).isEqualTo("plain:value");
            assertThat(handler.calls).hasValue(0);
        });
    }

    @Test
    void shouldBypassHandlerWhenComponentIsDisabled() {
        jdkRunner.withPropertyValues("egon.cola.component.method-extension.enabled=false")
                .run(context -> {
                    InterfaceAnnotatedService service = context.getBean(InterfaceAnnotatedService.class);
                    CountingHandler handler = context.getBean(CountingHandler.class);

                    assertThat(AopUtils.isAopProxy(service)).isFalse();
                    assertThat(service.call("value")).isEqualTo("interface:value");
                    assertThat(handler.calls).hasValue(0);
                });
    }

    @Test
    void shouldInterceptInheritedMethodOnCglibProxy() {
        cglibRunner.run(context -> {
            InheritedService service = context.getBean(InheritedService.class);
            CountingHandler handler = context.getBean(CountingHandler.class);

            assertThat(AopUtils.isCglibProxy(service)).isTrue();
            assertThat(service.inherited("value")).isEqualTo("inherited:value");
            assertThat(handler.calls).hasValue(1);
        });
    }

    @Test
    void shouldLeaveSelfInvocationUnintercepted() {
        cglibRunner.run(context -> {
            SelfInvokingService service = context.getBean(SelfInvokingService.class);
            CountingHandler handler = context.getBean(CountingHandler.class);

            assertThat(AopUtils.isCglibProxy(service)).isTrue();
            assertThat(service.outer("value")).isEqualTo("inner:value");
            assertThat(handler.calls).hasValue(0);
        });
    }

    @Configuration(proxyBeanMethods = false)
    static class JdkProxyConfiguration {

        @Bean
        CountingHandler countingHandler() {
            return new CountingHandler();
        }

        @Bean
        InterfaceAnnotatedService interfaceAnnotatedService() {
            return new InterfaceAnnotatedServiceImpl();
        }

        @Bean
        ImplementationAnnotatedService implementationAnnotatedService() {
            return new ImplementationAnnotatedServiceImpl();
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class CglibProxyConfiguration {

        @Bean
        CountingHandler countingHandler() {
            return new CountingHandler();
        }

        @Bean
        InheritedService inheritedService() {
            return new InheritedService();
        }

        @Bean
        SelfInvokingService selfInvokingService() {
            return new SelfInvokingService();
        }
    }

    interface InterfaceAnnotatedService {

        @MethodExtension(handler = CountingHandler.class)
        String call(String value);

        String plain(String value);
    }

    static class InterfaceAnnotatedServiceImpl implements InterfaceAnnotatedService {

        @Override
        public String call(String value) {
            return "interface:" + value;
        }

        @Override
        public String plain(String value) {
            return "plain:" + value;
        }
    }

    interface ImplementationAnnotatedService {

        String call(String value);
    }

    static class ImplementationAnnotatedServiceImpl implements ImplementationAnnotatedService {

        @Override
        @MethodExtension(handler = CountingHandler.class)
        public String call(String value) {
            return "implementation:" + value;
        }
    }

    static class BaseService {

        @MethodExtension(handler = CountingHandler.class)
        public String inherited(String value) {
            return "inherited:" + value;
        }
    }

    static class InheritedService extends BaseService {
    }

    static class SelfInvokingService {

        public String outer(String value) {
            return inner(value);
        }

        @MethodExtension(handler = CountingHandler.class)
        public String inner(String value) {
            return "inner:" + value;
        }
    }

    static class CountingHandler implements MethodExtensionHandler {

        private final AtomicInteger calls = new AtomicInteger();

        @Override
        public MethodExtensionDecision evaluate(MethodExtensionContext context) {
            calls.incrementAndGet();
            return MethodExtensionDecision.allow();
        }
    }
}
```

- [ ] **Step 2: Run the proxy integration tests**

```bash
./mvnw -B -ntp \
  -f egon-cola-components/egon-cola-component-method-extension/egon-cola-component-method-extension-starter/pom.xml \
  -Dtest=MethodExtensionProxyIntegrationTest test
```

Expected: all six tests pass, proving both annotation locations, both Spring proxy mechanisms, unannotated behavior, the disabled switch, and the documented self-invocation boundary.

- [ ] **Step 3: Run all starter tests after the proxy coverage is added**

```bash
./mvnw -B -ntp \
  -f egon-cola-components/egon-cola-component-method-extension/egon-cola-component-method-extension-starter/pom.xml test
```

Expected: `BUILD SUCCESS`; existing allow, reject, response, and error tests remain green.

- [ ] **Step 4: Commit the proxy compatibility tests**

```bash
git add egon-cola-components/egon-cola-component-method-extension/egon-cola-component-method-extension-starter/src/test/java/top/egon/cola/component/methodextension/aop/MethodExtensionProxyIntegrationTest.java \
  egon-cola-components/egon-cola-component-method-extension/egon-cola-component-method-extension-starter/src/main/java/top/egon/cola/component/methodextension/aop/MethodExtensionAop.java
git commit -m "test: cover method extension proxies"
```

### Task 9: Add Business Samples and the Component README

**Files:**
- Create: `egon-cola-components/egon-cola-component-method-extension/egon-cola-component-method-extension-test/src/test/java/top/egon/cola/component/methodextension/test/MethodExtensionSampleTest.java`
- Create: `egon-cola-components/egon-cola-component-method-extension/README.md`

- [ ] **Step 1: Write black-list, gray-release, and temporary-validation samples**

```java
package top.egon.cola.component.methodextension.test;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.aop.AopAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import top.egon.cola.component.methodextension.annotation.MethodExtension;
import top.egon.cola.component.methodextension.autoconfigure.MethodExtensionAutoConfiguration;
import top.egon.cola.component.methodextension.context.MethodExtensionContext;
import top.egon.cola.component.methodextension.handler.MethodExtensionDecision;
import top.egon.cola.component.methodextension.handler.MethodExtensionHandler;

import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MethodExtensionSampleTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(AopAutoConfiguration.class, MethodExtensionAutoConfiguration.class))
            .withPropertyValues("spring.aop.proxy-target-class=true")
            .withUserConfiguration(SampleConfiguration.class);

    @Test
    void shouldUseReturnJsonForBlacklistedUserAndAllowOtherUsers() {
        contextRunner.run(context -> {
            UserQueryService service = context.getBean(UserQueryService.class);

            assertThat(service.query("bbb"))
                    .isEqualTo(new UserResponse(1111, "access rejected", null));
            assertThat(service.query("aaa"))
                    .isEqualTo(new UserResponse(0, "success", "user:aaa"));
            assertThat(service.calls()).isEqualTo(1);
        });
    }

    @Test
    void shouldUseDirectResponseOutsideGrayCohort() {
        contextRunner.run(context -> {
            FeatureService service = context.getBean(FeatureService.class);

            assertThat(service.feature("legacy-user"))
                    .isEqualTo(new FeatureResponse(false, "legacy path"));
            assertThat(service.feature("gray-001"))
                    .isEqualTo(new FeatureResponse(true, "new path"));
            assertThat(service.calls()).isEqualTo(1);
        });
    }

    @Test
    void shouldPropagateTemporaryValidationFailure() {
        contextRunner.run(context -> {
            TemporaryService service = context.getBean(TemporaryService.class);

            assertThatThrownBy(() -> service.submit(" "))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("request must not be blank");
            assertThat(service.calls()).isZero();
        });
    }

    @Configuration(proxyBeanMethods = false)
    static class SampleConfiguration {

        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }

        @Bean
        BlacklistHandler blacklistHandler() {
            return new BlacklistHandler();
        }

        @Bean
        GrayReleaseHandler grayReleaseHandler() {
            return new GrayReleaseHandler();
        }

        @Bean
        TemporaryValidationHandler temporaryValidationHandler() {
            return new TemporaryValidationHandler();
        }

        @Bean
        UserQueryService userQueryService() {
            return new UserQueryService();
        }

        @Bean
        FeatureService featureService() {
            return new FeatureService();
        }

        @Bean
        TemporaryService temporaryService() {
            return new TemporaryService();
        }
    }

    static class BlacklistHandler implements MethodExtensionHandler {

        private static final Set<String> BLOCKED_USERS = Set.of("bbb", "222");

        @Override
        public MethodExtensionDecision evaluate(MethodExtensionContext context) {
            String userId = (String) context.arguments()[0];
            return BLOCKED_USERS.contains(userId)
                    ? MethodExtensionDecision.rejectWithReason("blacklist hit")
                    : MethodExtensionDecision.allow();
        }
    }

    static class GrayReleaseHandler implements MethodExtensionHandler {

        @Override
        public MethodExtensionDecision evaluate(MethodExtensionContext context) {
            String userId = (String) context.arguments()[0];
            return userId.startsWith("gray-")
                    ? MethodExtensionDecision.allow()
                    : MethodExtensionDecision.reject(new FeatureResponse(false, "legacy path"), "outside gray cohort");
        }
    }

    static class TemporaryValidationHandler implements MethodExtensionHandler {

        @Override
        public MethodExtensionDecision evaluate(MethodExtensionContext context) {
            String request = (String) context.arguments()[0];
            if (request.isBlank()) {
                throw new IllegalArgumentException("request must not be blank");
            }
            return MethodExtensionDecision.allow();
        }
    }

    static class UserQueryService {

        private final AtomicInteger calls = new AtomicInteger();

        @MethodExtension(
                handler = BlacklistHandler.class,
                returnJson = "{\"code\":1111,\"message\":\"access rejected\",\"name\":null}"
        )
        public UserResponse query(String userId) {
            calls.incrementAndGet();
            return new UserResponse(0, "success", "user:" + userId);
        }

        int calls() {
            return calls.get();
        }
    }

    static class FeatureService {

        private final AtomicInteger calls = new AtomicInteger();

        @MethodExtension(handler = GrayReleaseHandler.class)
        public FeatureResponse feature(String userId) {
            calls.incrementAndGet();
            return new FeatureResponse(true, "new path");
        }

        int calls() {
            return calls.get();
        }
    }

    static class TemporaryService {

        private final AtomicInteger calls = new AtomicInteger();

        @MethodExtension(handler = TemporaryValidationHandler.class)
        public String submit(String request) {
            calls.incrementAndGet();
            return request;
        }

        int calls() {
            return calls.get();
        }
    }

    record UserResponse(int code, String message, String name) {
    }

    record FeatureResponse(boolean enabled, String path) {
    }
}
```

- [ ] **Step 2: Run the sample test**

```bash
./mvnw -B -ntp \
  -f egon-cola-components/egon-cola-component-method-extension/pom.xml \
  -pl egon-cola-component-method-extension-test -am \
  -Dtest=MethodExtensionSampleTest \
  -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: all three sample tests pass. They prove JSON fallback, direct rejection, normal allow, and unchanged handler-exception propagation without starting an application.

- [ ] **Step 3: Create the component README**

````markdown
# Egon-COLA Method Extension

`egon-cola-component-method-extension` is a lightweight Spring Boot Starter for
running one business-defined decision handler before an annotated method.

## Modules

| Module | Purpose |
|---|---|
| `egon-cola-component-method-extension-starter` | Annotation, handler contract, AOP, response conversion, and auto-configuration |
| `egon-cola-component-method-extension-test` | Black-list, gray-release, and temporary-validation samples |

## Dependency

Import `top.egon:egon-cola-components-bom:5.2.2`, then add:

```xml
<dependency>
    <groupId>top.egon</groupId>
    <artifactId>egon-cola-component-method-extension-starter</artifactId>
</dependency>
```

## Configuration

```yaml
egon:
  cola:
    component:
      method-extension:
        enabled: true
        order: -2147483548
```

## Handler

Register every handler as a Spring Bean. Return `allow()` to invoke the original
method, `reject(response)` to return a direct object, or `reject()` to use the
annotation's `returnJson`.

```java
@Component
public class BlacklistHandler implements MethodExtensionHandler {

    @Override
    public MethodExtensionDecision evaluate(MethodExtensionContext context) {
        String userId = (String) context.arguments()[0];
        return Set.of("bbb", "222").contains(userId)
                ? MethodExtensionDecision.rejectWithReason("blacklist hit")
                : MethodExtensionDecision.allow();
    }
}
```

```java
@MethodExtension(
        handler = BlacklistHandler.class,
        returnJson = "{\"code\":1111,\"message\":\"access rejected\"}"
)
public UserResponse query(String userId) {
    return new UserResponse(0, "success");
}
```

## Response Rules

1. A direct handler response has first priority and must match the method return type.
2. A non-blank `returnJson` is second priority.
3. `String` methods receive `returnJson` unchanged.
4. Object and generic JSON conversion requires exactly one application `ObjectMapper` Bean.
5. `void` methods reject with `null` and must not configure `returnJson`.
6. Missing responses and incompatible types produce explicit component exceptions.

## Error Behavior

Missing or ambiguous handlers, null decisions, and invalid response configuration
raise `MethodExtensionConfigurationException`. JSON and response type failures
raise `MethodExtensionResponseException`. Exceptions from a handler are logged
and propagated unchanged; the original method is never executed after a handler
failure.

## Spring AOP Limits

Only public methods on Spring-managed proxy Beans are intercepted. Self-invocation
through `this`, private methods, static methods, and non-proxyable final methods
are not supported. V1 executes one synchronous handler and does not provide a
handler chain, reactive adapter, Web layer, Redis integration, or database state.
````

- [ ] **Step 4: Run the component test reactor**

```bash
./mvnw -B -ntp \
  -f egon-cola-components/egon-cola-component-method-extension/pom.xml test
```

Expected: `BUILD SUCCESS` for the root, starter, and test modules.

- [ ] **Step 5: Commit samples and documentation**

```bash
git add egon-cola-components/egon-cola-component-method-extension/README.md \
  egon-cola-components/egon-cola-component-method-extension/egon-cola-component-method-extension-test/src/test
git commit -m "docs: add method extension samples"
```

### Task 10: Verify the Full Reactor and Dependency Boundary

**Files:**
- Verify: `egon-cola-components/egon-cola-component-method-extension`
- Verify: `egon-cola-components/pom.xml`
- Verify: `egon-cola-components/egon-cola-components-bom/pom.xml`
- Verify: repository root `pom.xml`

- [ ] **Step 1: Run the focused starter and test-module slice**

```bash
./mvnw -B -ntp \
  -pl egon-cola-components/egon-cola-component-method-extension/egon-cola-component-method-extension-starter,egon-cola-components/egon-cola-component-method-extension/egon-cola-component-method-extension-test \
  -am test
```

Expected: `BUILD SUCCESS` with every Method Extension test passing.

- [ ] **Step 2: Run the complete components reactor**

```bash
./mvnw -B -ntp -f egon-cola-components/pom.xml test
```

Expected: `BUILD SUCCESS` for all existing components plus Method Extension.

- [ ] **Step 3: Prove the starter has no forbidden runtime dependencies**

```bash
mkdir -p target
./mvnw -B -ntp \
  -pl egon-cola-components/egon-cola-component-method-extension/egon-cola-component-method-extension-starter \
  -am dependency:tree \
  -DoutputFile="$PWD/target/method-extension-dependency-tree.txt"
if rg -n 'top\.egon:egon-cola-component-(access-guard|common)|org\.redisson|org\.springframework:spring-web|org\.flywaydb|org\.postgresql|org\.xerial' \
  target/method-extension-dependency-tree.txt; then
  exit 1
fi
```

Expected: dependency tree generation succeeds and the `rg` check prints no forbidden dependency.

- [ ] **Step 4: Verify that the BOM exports only the starter**

```bash
test "$(rg -c '<artifactId>egon-cola-component-method-extension-starter</artifactId>' \
  egon-cola-components/egon-cola-components-bom/pom.xml)" -eq 1
if rg -n '<artifactId>egon-cola-component-method-extension(-test)?</artifactId>' \
  egon-cola-components/egon-cola-components-bom/pom.xml; then
  exit 1
fi
```

Expected: both checks exit successfully; the BOM contains one starter entry and no root/test entry.

- [ ] **Step 5: Run repository-level integration verification**

```bash
./mvnw -B -ntp clean integration-test
```

Expected: `BUILD SUCCESS`, including downstream modules and archetype verification.

- [ ] **Step 6: Check repository hygiene and commit history**

```bash
git diff --check
git status --short
git log -12 --oneline
```

Expected: `git diff --check` succeeds, `git status --short` prints nothing, and the log shows one intentional commit for each implementation task.

Task 10 is verification-only. Do not create an empty commit when every check passes and the worktree is clean.

No service process, browser, database, Redis instance, or other external runtime is started during verification.

## Spec Coverage Check

| Approved design area | Implementation tasks |
|---|---|
| independent root/starter/test structure and BOM boundary | Tasks 1 and 10 |
| annotation, context, Strategy handler, and decision model | Tasks 2 and 3 |
| direct response, JSON fallback, generic types, and errors | Tasks 4 and 6 |
| implementation/interface/bridge/inherited method resolution | Tasks 5 and 8 |
| AOP order, allow/reject flow, logging, and fail-closed errors | Task 6 |
| enable switch and `AutoConfiguration.imports` wiring | Tasks 7 and 8 |
| JDK/CGLIB behavior and self-invocation limitation | Task 8 |
| black-list, gray-release, and temporary-validation examples | Task 9 |
| README, focused tests, full reactors, and dependency hygiene | Tasks 9 and 10 |

All approved V1 requirements have a concrete implementation task and validation command. The non-goals require no scaffolding or placeholder modules.
