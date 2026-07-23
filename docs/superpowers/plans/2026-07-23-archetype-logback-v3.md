# Archetype Logback V3 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make every Egon-COLA archetype generate the same profile-aware, observable, capacity-bounded Logback V3 configuration.

**Architecture:** Keep output routing in a self-contained `logback-spring.xml` per generated runnable module and keep environment, level, queue, and capacity values in `application.yml`. Protect the generated result with each archetype's existing `verify.groovy` post-generation contract.

**Tech Stack:** Maven Archetype 3.4, Groovy verifier scripts, Spring Boot 3.5, SLF4J 2, Logback 1.5, Velocity-filtered XML/YAML.

## Global Constraints

- Change only Light, Service, and Web archetype logging resources, generated-project verifiers, and this task's design/plan documents.
- Do not add a logging or JSON dependency; use Spring Boot dependency management and `StructuredLogEncoder`.
- Do not enable Logback `scan`.
- Do not add class, method, line, file, location, or caller conversions.
- Keep each generated project self-contained.
- Do not start an application.
- Commit each completed task once.

---

### Task 1: Record The Approved Design And Execution Contract

**Files:**
- Create: `docs/superpowers/specs/2026-07-23-archetype-logback-v3-design.md`
- Create: `docs/superpowers/plans/2026-07-23-archetype-logback-v3.md`

**Interfaces:**
- Consumes: the logging guidance in `/Users/mario/SelfProject/blog/source/_posts/java/spring/JAVA_LOG_V3.md`
- Produces: the exact runtime modes, defaults, generated-file paths, and validation contract used by Task 2

- [ ] **Step 1: Check the design for ambiguity**

Run:

```bash
rg -n 'T[B]D|T[O]DO|implement [l]ater|fill [i]n' \
  docs/superpowers/specs/2026-07-23-archetype-logback-v3-design.md \
  docs/superpowers/plans/2026-07-23-archetype-logback-v3.md
```

Expected: no output.

- [ ] **Step 2: Check document whitespace**

Run:

```bash
git diff --check -- \
  docs/superpowers/specs/2026-07-23-archetype-logback-v3-design.md \
  docs/superpowers/plans/2026-07-23-archetype-logback-v3.md
```

Expected: exit code 0.

- [ ] **Step 3: Commit the design task**

```bash
git add \
  docs/superpowers/specs/2026-07-23-archetype-logback-v3-design.md \
  docs/superpowers/plans/2026-07-23-archetype-logback-v3.md
git commit -m "docs(archetype): design Logback V3 templates"
```

### Task 2: Add Contract-First Logging Verification

**Files:**
- Modify: `egon-cola-archetypes/egon-cola-archetype-light/src/test/resources/projects/basic/verify.groovy`
- Modify: `egon-cola-archetypes/egon-cola-archetype-service/src/test/resources/projects/basic/verify.groovy`
- Modify: `egon-cola-archetypes/egon-cola-archetype-web/src/test/resources/projects/basic/verify.groovy`

**Interfaces:**
- Consumes: generated project root and each verifier's existing `assertFile` closure
- Produces: a stable generated logging contract that fails until Task 3 supplies the resources

- [ ] **Step 1: Add a logging assertion helper to each verifier**

Each verifier must parse the generated `logback-spring.xml` with
`XmlSlurper(false, false)` and assert these stable tokens:

```groovy
def requiredLogbackTokens = [
    "org/springframework/boot/logging/logback/defaults.xml",
    'source="spring.application.name"',
    'source="spring.profiles.active"',
    'source="logging.file.path"',
    'source="app.logging.instance-name"',
    'source="logging.level.root"',
    'traceId=%X{traceId:-}',
    'spanId=%X{spanId:-}',
    'requestId=%X{requestId:-}',
    'tenantId=%X{tenantId:-}',
    'userId=%X{userId:-}',
    'entId=%X{entId:-}',
    "%kvp",
    'name="k8s"',
    "StructuredLogEncoder",
    'name="ASYNC_APP_FILE"',
    "<discardingThreshold>\${ASYNC_DISCARDING_THRESHOLD}</discardingThreshold>",
    "<neverBlock>\${ASYNC_NEVER_BLOCK}</neverBlock>",
    'name="ERROR_FILE"',
    "LevelFilter",
    "SizeAndTimeBasedRollingPolicy",
    ".%i.log.gz"
]
```

Also assert the XML root has no `scan` attribute, the root never references
both `APP_FILE` and `ASYNC_APP_FILE`, and the test config contains MDC, `%kvp`,
UTF-8, and root `WARN`.

- [ ] **Step 2: Assert application-level controls**

Each generated `application.yml` must contain:

```groovy
[
    "logging:",
    "instance-name:",
    "error-total-size-cap:",
    "queue-size:",
    "discarding-threshold:",
    "never-block:",
    "max-flush-time:",
    "web-stack:",
    "persistence:",
    "rpc:",
    "messaging:",
    "max-file-size:",
    "max-history:",
    "total-size-cap:",
    "clean-history-on-start:"
]
```

- [ ] **Step 3: Run the contract against the current generated outputs**

Run the three copied post-build verifiers through Maven Invoker by running the
three archetype modules independently or the reactor until each failure is
observed.

Expected: each verifier fails because Light uses `logback.xml`, Service has no
main logging XML, and Web lacks the V3 tokens.

### Task 3: Generate The Logback V3 Runtime Configuration

**Files:**
- Delete: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/resources/logback.xml`
- Modify: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/META-INF/maven/archetype-metadata.xml`
- Create: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/resources/logback-spring.xml`
- Create: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/test/resources/logback-test.xml`
- Create: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-starter/src/main/resources/logback-spring.xml`
- Create: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-starter/src/test/resources/logback-test.xml`
- Modify: `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-starter/src/main/resources/logback-spring.xml`
- Modify: `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-starter/src/test/resources/logback-test.xml`
- Modify: all three generated `application.yml` files

**Interfaces:**
- Consumes: Spring Environment properties and the current active Spring profiles
- Produces: text console, ECS JSON console, asynchronous rolling application file, and synchronous exact-ERROR file outputs

- [ ] **Step 1: Add the Velocity-safe main XML**

Each source template starts with:

```xml
#set( $symbol_dollar = '$' )
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <include resource="org/springframework/boot/logging/logback/defaults.xml"/>
```

It defines Spring properties for application, environment, instance, root
level, rolling limits, ERROR capacity, async queue behavior, and structured
format. Every literal generated placeholder uses
`${symbol_dollar}{PROPERTY}` in the archetype source.

The local profile references only `CONSOLE`; the K8s profile references only
`JSON_CONSOLE`; the production profile references `CONSOLE`,
`ASYNC_APP_FILE`, and `ERROR_FILE`.

- [ ] **Step 2: Add the native test XML**

Each runnable module receives:

```xml
#set( $symbol_dollar = '$' )
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <include resource="org/springframework/boot/logging/logback/defaults.xml"/>
    <property name="TEST_LOG_PATTERN"
              value="%d{HH:mm:ss.SSS} %-5level [%thread] traceId=%X{traceId:-} spanId=%X{spanId:-} requestId=%X{requestId:-} %logger{40} %kvp - %msg%n${symbol_dollar}{LOG_EXCEPTION_CONVERSION_WORD:-%wEx}"/>
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder class="ch.qos.logback.classic.encoder.PatternLayoutEncoder">
            <pattern>${symbol_dollar}{TEST_LOG_PATTERN}</pattern>
            <charset>UTF-8</charset>
        </encoder>
    </appender>
    <root level="WARN">
        <appender-ref ref="CONSOLE"/>
    </root>
</configuration>
```

- [ ] **Step 3: Include Light test resources in generated projects**

Add a filtered UTF-8 `src/test/resources` file set to Light's
`archetype-metadata.xml`, including `**/*.yml`, `**/*.properties`, and
`**/*.xml`. Service and Web already have the equivalent starter-module file
set.

- [ ] **Step 4: Add common runtime values**

Merge this shape into each existing `application.yml` without duplicating an
existing top-level `app` key:

```yaml
app:
  logging:
    instance-name: ${INSTANCE_ID:${HOSTNAME:local}}
    error-total-size-cap: ${LOG_ERROR_TOTAL_SIZE_CAP:2GB}
    async:
      queue-size: ${LOG_ASYNC_QUEUE_SIZE:8192}
      discarding-threshold: ${LOG_ASYNC_DISCARDING_THRESHOLD:0}
      never-block: ${LOG_ASYNC_NEVER_BLOCK:false}
      max-flush-time: ${LOG_ASYNC_MAX_FLUSH_TIME:5000}

logging:
  file:
    path: ${LOG_HOME:./logs}
  group:
    web-stack: org.springframework.web,org.springframework.http,org.apache.catalina,org.apache.coyote
    persistence: org.mybatis,org.mybatis.spring,com.baomidou.mybatisplus,org.springframework.jdbc,org.hibernate.SQL
    rpc: org.apache.dubbo,io.grpc
    messaging: org.springframework.kafka,org.springframework.amqp
  level:
    root: info
    web-stack: info
    persistence: warn
    rpc: info
    messaging: info
    ${groupId}: info
  logback:
    rollingpolicy:
      max-file-size: ${LOG_MAX_FILE_SIZE:100MB}
      max-history: ${LOG_MAX_HISTORY:30}
      total-size-cap: ${LOG_TOTAL_SIZE_CAP:10GB}
      clean-history-on-start: ${LOG_CLEAN_HISTORY_ON_START:false}
```

In archetype sources all `${...}` expressions use `$symbol_dollar`.

- [ ] **Step 5: Run focused archetype verification**

Run:

```bash
bash ./mvnw -B -ntp -f egon-cola-archetypes/pom.xml \
  -pl egon-cola-archetype-light,egon-cola-archetype-service,egon-cola-archetype-web \
  -am clean integration-test
```

Expected: all selected modules and generated-project verifiers succeed.

- [ ] **Step 6: Commit the implementation task**

```bash
git add egon-cola-archetypes
git commit -m "feat(archetype): standardize Logback V3 logging"
```

### Task 4: Verify The Complete Archetype Delivery

**Files:**
- Verify only: all files changed by Tasks 1 through 3

**Interfaces:**
- Consumes: committed archetype templates and verifiers
- Produces: fresh evidence that the complete generated-project reactor is green

- [ ] **Step 1: Run the authoritative reactor**

Run:

```bash
bash ./mvnw -B -ntp -f egon-cola-archetypes/pom.xml clean integration-test
```

Expected: six reactor modules report `SUCCESS`.

- [ ] **Step 2: Inspect generated logging resources**

Run:

```bash
find egon-cola-archetypes/egon-cola-archetype-*/target/test-classes/projects/basic/project \
  \( -name logback-spring.xml -o -name logback-test.xml -o -name logback.xml \) \
  -type f -print
```

Expected: exactly one main `logback-spring.xml` and one runnable-module
`logback-test.xml` per generated project, and no generated main `logback.xml`.

- [ ] **Step 3: Run repository hygiene checks**

Run:

```bash
git diff --check HEAD^
git status --short --branch
```

Expected: no whitespace errors and no uncommitted source changes.
