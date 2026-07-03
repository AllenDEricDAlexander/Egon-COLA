# Runtime Engineering Archetype Baseline Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Upgrade the Web, Service, and Light archetypes so generated projects include the approved Spring Boot runtime engineering baseline and CI proves the generated projects can test, package, and build Docker images.

**Architecture:** Keep the three existing generated project shapes: Web and Service remain seven-module generated projects with a runnable `starter` module, and Light remains one Maven module with package layers. Runtime capabilities live in generated starter/start boot packages and profile resources; contracts stay in facade modules, business boundaries stay in adapter/application/domain/infrastructure modules, and Service keeps business Web code out of adapter/business modules while allowing management-only Actuator HTTP in starter.

**Tech Stack:** Java 21, Spring Boot 3.5.16, Spring Cloud 2025.0.3, Spring Cloud Alibaba 2025.0.0.0, Apache Dubbo 3.3.6, Maven Archetype Plugin, Flyway, HikariCP, Bean Validation, Actuator, Micrometer Prometheus, Docker multi-stage builds, GitHub Actions.

---

## Execution Constraints

- Do not start generated Spring Boot applications.
- Do not run `docker run`; Docker verification is build-only.
- Do not modify `cola-samples`.
- Do not extract `egon-cola-component-config-starter`.
- Do not change existing Flyway migration files outside archetype templates.
- Do not add business Web controllers, Web filters, `web` packages, or `spring-boot-starter-web` to the service adapter.
- A management-only Web runtime dependency may exist in the service starter if required to expose Actuator over HTTP.
- Keep `local` and `test` profiles free of external Nacos by default.
- Keep JSON `Long` values as numbers.
- Commit exactly once at the end of each task after validation passes.
- Stage only files listed in each task. Never stage `target/`.

## File Structure

### Shared Design And Validation Files

- Already written spec: `docs/superpowers/specs/2026-07-03-runtime-engineering-archetype-baseline-design.md`.
- Create this implementation plan: `docs/superpowers/plans/2026-07-03-runtime-engineering-archetype-baseline.md`.
- Modify CI: `.github/workflows/ci_by_multiply_java_versions.yaml`.

### Web Archetype Template

- Modify root generated POM: `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/pom.xml`.
- Create top-level generated Docker files:
  - `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/Dockerfile`
  - `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/.dockerignore`
- Modify top-level generated docs:
  - `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/README.md`
  - `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__gitignore__`
- Modify generated module POMs:
  - `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-facade/pom.xml`
  - `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-domain/pom.xml`
  - `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-application/pom.xml`
  - `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/pom.xml`
  - `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-adapter/pom.xml`
  - `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-starter/pom.xml`
- Replace starter runtime resources:
  - `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-starter/src/main/resources/application.yml`
  - Create the nine missing profile files beside it: `application-local.yml`, `application-dev.yml`, `application-test.yml`, `application-prod.yml`, `bootstrap.yml`, `bootstrap-local.yml`, `bootstrap-dev.yml`, `bootstrap-test.yml`, `bootstrap-prod.yml`.
- Create Web starter runtime classes:
  - `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-starter/src/main/java/starter/config/async/AsyncConfiguration.java`
  - `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-starter/src/main/java/starter/config/encryption/ConfigDecryptor.java`
  - `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-starter/src/main/java/starter/config/encryption/AesGcmConfigDecryptor.java`
  - `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-starter/src/main/java/starter/config/encryption/ConfigDecryptEnvironmentPostProcessor.java`
  - `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-starter/src/main/java/starter/config/encryption/ConfigDecryptException.java`
  - `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-starter/src/main/java/starter/config/encryption/ConfigDecryptKeyProvider.java`
  - `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-starter/src/main/java/starter/config/encryption/ConfigCipherCli.java`
  - `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-starter/src/main/resources/META-INF/spring.factories`
- Create Web validation utility:
  - `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-adapter/src/main/java/adapter/validation/ValidatorUtils.java`
- Modify Web request DTOs and handlers:
  - `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-facade/src/main/java/facade/dto/user/CreateUserRequest.java`
  - `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-facade/src/main/java/facade/dto/teaching/CreateSchoolClassRequest.java`
  - `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-facade/src/main/java/facade/dto/teaching/AssignUserToClassRequest.java`
  - `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-adapter/src/main/java/adapter/controller/user/UserController.java`
  - `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-adapter/src/main/java/adapter/controller/teaching/SchoolClassController.java`
  - `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-adapter/src/main/java/adapter/facade/user/UserFacadeImpl.java`
  - `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-adapter/src/main/java/adapter/facade/teaching/SchoolClassFacadeImpl.java`
  - `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-adapter/src/main/java/adapter/handler/GlobalExceptionHandler.java`
- Modify Web archetype metadata and verification:
  - `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/META-INF/maven/archetype-metadata.xml`
  - `egon-cola-archetypes/egon-cola-archetype-web/src/test/resources/projects/basic/verify.groovy`

### Service Archetype Template

- Modify the same root/module file categories as Web under `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources`.
- Service starter runtime classes use package `starter.config.*`.
- Service adapter validation utility path:
  - `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-adapter/src/main/java/adapter/validation/ValidatorUtils.java`
- Service DTOs and adapter entry points:
  - `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-facade/src/main/java/facade/dto/course/CreateCourseRequest.java`
  - `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-facade/src/main/java/facade/dto/examing/RecordExamResultRequest.java`
  - `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-adapter/src/main/java/adapter/facade/impl/CourseFacadeImpl.java`
  - `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-adapter/src/main/java/adapter/facade/impl/ExamResultFacadeImpl.java`
  - `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-adapter/src/main/java/adapter/handler/ServiceExceptionHandler.java`
- Modify Service metadata and verification:
  - `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/META-INF/maven/archetype-metadata.xml`
  - `egon-cola-archetypes/egon-cola-archetype-service/src/test/resources/projects/basic/verify.groovy`

### Light Archetype Template

- Modify generated POM: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/pom.xml`.
- Create top-level generated Docker files:
  - `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/Dockerfile`
  - `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/.dockerignore`
- Modify generated docs and ignore:
  - `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/README.md`
  - `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/__gitignore__`
- Replace runtime resources under `src/main/resources`:
  - `application.yml`
  - Create `application-local.yml`, `application-dev.yml`, `application-test.yml`, `application-prod.yml`, `bootstrap.yml`, `bootstrap-local.yml`, `bootstrap-dev.yml`, `bootstrap-test.yml`, `bootstrap-prod.yml`.
- Create Light runtime classes:
  - `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java/start/config/async/AsyncConfiguration.java`
  - `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java/start/config/encryption/ConfigDecryptor.java`
  - `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java/start/config/encryption/AesGcmConfigDecryptor.java`
  - `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java/start/config/encryption/ConfigDecryptEnvironmentPostProcessor.java`
  - `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java/start/config/encryption/ConfigDecryptException.java`
  - `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java/start/config/encryption/ConfigDecryptKeyProvider.java`
  - `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java/start/config/encryption/ConfigCipherCli.java`
  - `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/resources/META-INF/spring.factories`
- Create Light validation utility:
  - `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java/adapter/validation/ValidatorUtils.java`
- Modify Light request DTOs and handlers:
  - `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java/facade/dto/RegisterStudentRequest.java`
  - `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java/facade/dto/CreateCourseRequest.java`
  - `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java/facade/dto/AssignCourseRequest.java`
  - `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java/adapter/controller/student/StudentController.java`
  - `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java/adapter/controller/teaching/CourseController.java`
  - `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java/adapter/facade/impl/StudentManagementFacadeImpl.java`
  - `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java/adapter/handler/GlobalExceptionHandler.java`
- Modify Light metadata and verification:
  - `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/META-INF/maven/archetype-metadata.xml`
  - `egon-cola-archetypes/egon-cola-archetype-light/src/test/resources/projects/basic/verify.groovy`

## Task 1: Dependency Baseline, Runtime Resources, And Metadata

**Files:**
- Modify all root and module POM paths listed in File Structure.
- Create/modify all runtime profile YAML paths listed in File Structure.
- Modify all three `archetype-metadata.xml` files.
- Modify all three `verify.groovy` files.

- [ ] **Step 1: Add failing verification assertions**

In each `verify.groovy`, add a reusable config assertion block. For Light, use `src/main/resources`; for Web use `student-management-organization-starter/src/main/resources`; for Service use `student-management-evaluation-starter/src/main/resources`.

```groovy
def assertRuntimeConfigFiles = { resourcesDir ->
    [
        "bootstrap.yml",
        "bootstrap-local.yml",
        "bootstrap-dev.yml",
        "bootstrap-test.yml",
        "bootstrap-prod.yml",
        "application.yml",
        "application-local.yml",
        "application-dev.yml",
        "application-test.yml",
        "application-prod.yml"
    ].each {
        assertFile("${resourcesDir}/${it}")
    }
}
```

Call the helper in each script:

```groovy
assertRuntimeConfigFiles("src/main/resources")
assertRuntimeConfigFiles("student-management-organization-starter/src/main/resources")
assertRuntimeConfigFiles("student-management-evaluation-starter/src/main/resources")
```

Use only the call that matches that archetype's generated project.

Add these dependency checks:

```groovy
assert pom.contains("<spring-cloud.version>2025.0.3</spring-cloud.version>")
assert pom.contains("<spring-cloud-alibaba.version>2025.0.0.0</spring-cloud-alibaba.version>")
assert pom.contains("<artifactId>spring-cloud-dependencies</artifactId>")
assert pom.contains("<artifactId>spring-cloud-alibaba-dependencies</artifactId>")
```

For runnable module POM text, assert:

```groovy
assert starterPomText.contains("<artifactId>spring-cloud-starter-bootstrap</artifactId>")
assert starterPomText.contains("<artifactId>spring-cloud-starter-alibaba-nacos-discovery</artifactId>")
assert starterPomText.contains("<artifactId>spring-cloud-starter-alibaba-nacos-config</artifactId>")
assert starterPomText.contains("<artifactId>micrometer-registry-prometheus</artifactId>")
```

For Light, use `pom` as the runnable POM text.

- [ ] **Step 2: Run verification and confirm it fails**

Run:

```bash
bash ./mvnw -B -ntp -pl egon-cola-archetypes/egon-cola-archetype-light -am clean integration-test
```

Expected: `BUILD FAILURE` with an assertion mentioning a missing `bootstrap.yml` or Spring Cloud dependency.

Run:

```bash
bash ./mvnw -B -ntp -pl egon-cola-archetypes/egon-cola-archetype-web -am clean integration-test
```

Expected: `BUILD FAILURE` with an assertion mentioning a missing `bootstrap.yml` or Spring Cloud dependency.

Run:

```bash
bash ./mvnw -B -ntp -pl egon-cola-archetypes/egon-cola-archetype-service -am clean integration-test
```

Expected: `BUILD FAILURE` with an assertion mentioning a missing `bootstrap.yml` or Spring Cloud dependency.

- [ ] **Step 3: Add Spring Cloud dependency management**

In the root generated POM for Web and Service, add these properties beside the existing `dubbo.version` property:

```xml
<spring-cloud.version>2025.0.3</spring-cloud.version>
<spring-cloud-alibaba.version>2025.0.0.0</spring-cloud-alibaba.version>
```

In their `<dependencyManagement><dependencies>` blocks, add these BOMs after `spring-boot-dependencies`:

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-dependencies</artifactId>
    <version>${spring-cloud.version}</version>
    <type>pom</type>
    <scope>import</scope>
</dependency>
<dependency>
    <groupId>com.alibaba.cloud</groupId>
    <artifactId>spring-cloud-alibaba-dependencies</artifactId>
    <version>${spring-cloud-alibaba.version}</version>
    <type>pom</type>
    <scope>import</scope>
</dependency>
```

In the Light generated POM, add the same properties and the same BOMs in a new `<dependencyManagement>` block before `<dependencies>`.

- [ ] **Step 4: Add runnable module dependencies**

In Web and Service starter POMs, add these dependencies:

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-bootstrap</artifactId>
</dependency>
<dependency>
    <groupId>com.alibaba.cloud</groupId>
    <artifactId>spring-cloud-starter-alibaba-nacos-discovery</artifactId>
</dependency>
<dependency>
    <groupId>com.alibaba.cloud</groupId>
    <artifactId>spring-cloud-starter-alibaba-nacos-config</artifactId>
</dependency>
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
```

In the Service starter POM, add this dependency and keep it out of the service adapter:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>
```

In the Light POM, add these dependencies:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-bootstrap</artifactId>
</dependency>
<dependency>
    <groupId>com.alibaba.cloud</groupId>
    <artifactId>spring-cloud-starter-alibaba-nacos-discovery</artifactId>
</dependency>
<dependency>
    <groupId>com.alibaba.cloud</groupId>
    <artifactId>spring-cloud-starter-alibaba-nacos-config</artifactId>
</dependency>
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
```

- [ ] **Step 5: Create bootstrap profile resources**

For Web and Service, create these files under each starter `src/main/resources`. For Light, create the same files under `src/main/resources`. In Web/Service use `${rootArtifactId}`. In Light use `${artifactId}`.

`bootstrap.yml`:

```yaml
#set( $symbol_dollar = '$' )
spring:
  application:
    name: ${symbol_dollar}{APP_NAME:${rootArtifactId}}
  profiles:
    default: local
  cloud:
    discovery:
      enabled: ${symbol_dollar}{DISCOVERY_ENABLED:false}
    nacos:
      username: ${symbol_dollar}{NACOS_USERNAME:}
      password: ${symbol_dollar}{NACOS_PASSWORD:}
      config:
        enabled: ${symbol_dollar}{NACOS_CONFIG_ENABLED:false}
        server-addr: ${symbol_dollar}{NACOS_SERVER_ADDR:127.0.0.1:8848}
        namespace: ${symbol_dollar}{NACOS_NAMESPACE:local}
        group: ${symbol_dollar}{NACOS_CONFIG_GROUP:DEFAULT_GROUP}
        file-extension: yaml
        refresh-enabled: ${symbol_dollar}{NACOS_CONFIG_REFRESH_ENABLED:false}
      discovery:
        enabled: ${symbol_dollar}{NACOS_DISCOVERY_ENABLED:false}
        server-addr: ${symbol_dollar}{NACOS_SERVER_ADDR:127.0.0.1:8848}
        namespace: ${symbol_dollar}{NACOS_NAMESPACE:local}
        group: ${symbol_dollar}{NACOS_DISCOVERY_GROUP:DEFAULT_GROUP}

dubbo:
  application:
    name: ${symbol_dollar}{spring.application.name}
  registry:
    address: ${symbol_dollar}{DUBBO_REGISTRY_ADDRESS:N/A}
```

For Light, change the `APP_NAME` default line to:

```yaml
    name: ${symbol_dollar}{APP_NAME:${artifactId}}
```

`bootstrap-local.yml`:

```yaml
spring:
  cloud:
    discovery:
      enabled: false
    nacos:
      config:
        enabled: false
      discovery:
        enabled: false

dubbo:
  registry:
    address: N/A
```

`bootstrap-dev.yml`:

```yaml
#set( $symbol_dollar = '$' )
spring:
  cloud:
    discovery:
      enabled: ${symbol_dollar}{DISCOVERY_ENABLED:true}
    nacos:
      username: ${symbol_dollar}{NACOS_USERNAME:}
      password: ${symbol_dollar}{NACOS_PASSWORD:}
      config:
        enabled: ${symbol_dollar}{NACOS_CONFIG_ENABLED:true}
        server-addr: ${symbol_dollar}{NACOS_SERVER_ADDR}
        namespace: ${symbol_dollar}{NACOS_NAMESPACE:dev}
        group: ${symbol_dollar}{NACOS_CONFIG_GROUP:DEV_GROUP}
        refresh-enabled: ${symbol_dollar}{NACOS_CONFIG_REFRESH_ENABLED:true}
      discovery:
        enabled: ${symbol_dollar}{NACOS_DISCOVERY_ENABLED:true}
        server-addr: ${symbol_dollar}{NACOS_SERVER_ADDR}
        namespace: ${symbol_dollar}{NACOS_NAMESPACE:dev}
        group: ${symbol_dollar}{NACOS_DISCOVERY_GROUP:DEV_GROUP}

dubbo:
  registry:
    address: ${symbol_dollar}{DUBBO_REGISTRY_ADDRESS:nacos://${symbol_dollar}{NACOS_SERVER_ADDR}}
```

`bootstrap-test.yml`:

```yaml
spring:
  cloud:
    discovery:
      enabled: false
    nacos:
      config:
        enabled: false
      discovery:
        enabled: false

dubbo:
  registry:
    address: N/A
```

`bootstrap-prod.yml`:

```yaml
#set( $symbol_dollar = '$' )
spring:
  cloud:
    discovery:
      enabled: ${symbol_dollar}{DISCOVERY_ENABLED:true}
    nacos:
      username: ${symbol_dollar}{NACOS_USERNAME}
      password: ${symbol_dollar}{NACOS_PASSWORD}
      config:
        enabled: ${symbol_dollar}{NACOS_CONFIG_ENABLED:true}
        server-addr: ${symbol_dollar}{NACOS_SERVER_ADDR}
        namespace: ${symbol_dollar}{NACOS_NAMESPACE}
        group: ${symbol_dollar}{NACOS_CONFIG_GROUP:PROD_GROUP}
        refresh-enabled: ${symbol_dollar}{NACOS_CONFIG_REFRESH_ENABLED:false}
      discovery:
        enabled: ${symbol_dollar}{NACOS_DISCOVERY_ENABLED:true}
        server-addr: ${symbol_dollar}{NACOS_SERVER_ADDR}
        namespace: ${symbol_dollar}{NACOS_NAMESPACE}
        group: ${symbol_dollar}{NACOS_DISCOVERY_GROUP:PROD_GROUP}

dubbo:
  registry:
    address: ${symbol_dollar}{DUBBO_REGISTRY_ADDRESS:nacos://${symbol_dollar}{NACOS_SERVER_ADDR}}
```

- [ ] **Step 6: Split application profile resources**

Replace Web/Service starter `application.yml` and Light `application.yml` with this common baseline. Use `${rootArtifactId}` for Web/Service and `${artifactId}` for Light.

```yaml
#set( $symbol_dollar = '$' )
spring:
  profiles:
    default: local
  config:
    import:
      - optional:file:./config/application-secrets.yml
      - optional:configtree:/run/secrets/
  threads:
    virtual:
      enabled: ${symbol_dollar}{SPRING_THREADS_VIRTUAL_ENABLED:true}
  lifecycle:
    timeout-per-shutdown-phase: ${symbol_dollar}{SHUTDOWN_TIMEOUT:30s}
  task:
    execution:
      thread-name-prefix: ${symbol_dollar}{ASYNC_THREAD_NAME_PREFIX:egon-async-}
      shutdown:
        await-termination: true
        await-termination-period: ${symbol_dollar}{ASYNC_AWAIT_TERMINATION:30s}
      pool:
        core-size: ${symbol_dollar}{ASYNC_CORE_SIZE:8}
        max-size: ${symbol_dollar}{ASYNC_MAX_SIZE:32}
        queue-capacity: ${symbol_dollar}{ASYNC_QUEUE_CAPACITY:500}
  jackson:
    date-format: yyyy-MM-dd HH:mm:ss
    time-zone: ${symbol_dollar}{APP_TIME_ZONE:Asia/Shanghai}
    default-property-inclusion: non_null
    serialization:
      write-dates-as-timestamps: false
      fail-on-empty-beans: false
    deserialization:
      fail-on-unknown-properties: false

server:
  port: ${symbol_dollar}{SERVER_PORT:8080}
  shutdown: graceful
  tomcat:
    threads:
      max: ${symbol_dollar}{TOMCAT_THREADS_MAX:200}
      min-spare: ${symbol_dollar}{TOMCAT_THREADS_MIN_SPARE:20}
    accept-count: ${symbol_dollar}{TOMCAT_ACCEPT_COUNT:100}
    max-connections: ${symbol_dollar}{TOMCAT_MAX_CONNECTIONS:8192}
    connection-timeout: ${symbol_dollar}{TOMCAT_CONNECTION_TIMEOUT:20s}
    keep-alive-timeout: ${symbol_dollar}{TOMCAT_KEEP_ALIVE_TIMEOUT:20s}
    max-keep-alive-requests: ${symbol_dollar}{TOMCAT_MAX_KEEP_ALIVE_REQUESTS:100}

management:
  endpoints:
    web:
      exposure:
        include: ${symbol_dollar}{MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE:health,info,metrics,prometheus}
  endpoint:
    health:
      probes:
        enabled: true
      show-details: ${symbol_dollar}{MANAGEMENT_HEALTH_SHOW_DETAILS:never}
  health:
    readinessstate:
      enabled: true
    livenessstate:
      enabled: true

dubbo:
  application:
    name: ${symbol_dollar}{spring.application.name}
  registry:
    address: ${symbol_dollar}{DUBBO_REGISTRY_ADDRESS:N/A}
  protocol:
    name: tri
    port: ${symbol_dollar}{DUBBO_PORT:50051}
  provider:
    timeout: 3000
    retries: 0
```

For Service only, override the server block in `application.yml` with:

```yaml
server:
  port: ${symbol_dollar}{MANAGEMENT_SERVER_PORT:8081}
  shutdown: graceful
```

Do not include `server.tomcat` in Service.

Create `application-local.yml`:

```yaml
#set( $symbol_dollar = '$' )
spring:
  datasource:
    url: ${symbol_dollar}{DB_URL:jdbc:h2:mem:${rootArtifactId};MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH}
    username: ${symbol_dollar}{DB_USERNAME:sa}
    password: ${symbol_dollar}{DB_PASSWORD:}
    driver-class-name: org.h2.Driver
    hikari:
      pool-name: ${symbol_dollar}{HIKARI_POOL_NAME:${symbol_dollar}{spring.application.name}-hikari}
      maximum-pool-size: ${symbol_dollar}{HIKARI_MAX_POOL_SIZE:10}
      minimum-idle: ${symbol_dollar}{HIKARI_MIN_IDLE:2}
      connection-timeout: ${symbol_dollar}{HIKARI_CONNECTION_TIMEOUT:30000}
      idle-timeout: ${symbol_dollar}{HIKARI_IDLE_TIMEOUT:600000}
      max-lifetime: ${symbol_dollar}{HIKARI_MAX_LIFETIME:1800000}
      keepalive-time: ${symbol_dollar}{HIKARI_KEEPALIVE_TIME:300000}
      validation-timeout: ${symbol_dollar}{HIKARI_VALIDATION_TIMEOUT:5000}
  h2:
    console:
      enabled: true
  flyway:
    enabled: true
    locations: classpath:db/migration
    clean-disabled: false
    validate-on-migrate: true
  jpa:
    hibernate:
      ddl-auto: validate
    open-in-view: false
    show-sql: true
```

For Light, use `${artifactId}` in the H2 database name. For Web/Service, use `${rootArtifactId}`.

Create `application-dev.yml`:

```yaml
#set( $symbol_dollar = '$' )
spring:
  datasource:
    url: ${symbol_dollar}{DB_URL}
    username: ${symbol_dollar}{DB_USERNAME}
    password: ${symbol_dollar}{DB_PASSWORD}
    hikari:
      pool-name: ${symbol_dollar}{HIKARI_POOL_NAME:${symbol_dollar}{spring.application.name}-hikari}
      maximum-pool-size: ${symbol_dollar}{HIKARI_MAX_POOL_SIZE:20}
      minimum-idle: ${symbol_dollar}{HIKARI_MIN_IDLE:5}
      connection-timeout: ${symbol_dollar}{HIKARI_CONNECTION_TIMEOUT:30000}
      idle-timeout: ${symbol_dollar}{HIKARI_IDLE_TIMEOUT:600000}
      max-lifetime: ${symbol_dollar}{HIKARI_MAX_LIFETIME:1800000}
      keepalive-time: ${symbol_dollar}{HIKARI_KEEPALIVE_TIME:300000}
      validation-timeout: ${symbol_dollar}{HIKARI_VALIDATION_TIMEOUT:5000}
  flyway:
    enabled: ${symbol_dollar}{FLYWAY_ENABLED:true}
    locations: classpath:db/migration
    baseline-on-migrate: ${symbol_dollar}{FLYWAY_BASELINE_ON_MIGRATE:false}
    validate-on-migrate: true
    clean-disabled: true
  jpa:
    hibernate:
      ddl-auto: validate
    open-in-view: false
    show-sql: false
```

Create `application-test.yml`:

```yaml
#set( $symbol_dollar = '$' )
spring:
  datasource:
    url: ${symbol_dollar}{DB_URL:jdbc:h2:mem:${rootArtifactId}-test;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH}
    username: ${symbol_dollar}{DB_USERNAME:sa}
    password: ${symbol_dollar}{DB_PASSWORD:}
    driver-class-name: org.h2.Driver
    hikari:
      pool-name: ${symbol_dollar}{HIKARI_POOL_NAME:${symbol_dollar}{spring.application.name}-hikari}
      maximum-pool-size: ${symbol_dollar}{HIKARI_MAX_POOL_SIZE:8}
      minimum-idle: ${symbol_dollar}{HIKARI_MIN_IDLE:1}
  flyway:
    enabled: true
    locations: classpath:db/migration
    clean-disabled: false
    validate-on-migrate: true
  jpa:
    hibernate:
      ddl-auto: validate
    open-in-view: false
    show-sql: false
```

For Light, use `${artifactId}-test` in the H2 database name.

Create `application-prod.yml`:

```yaml
#set( $symbol_dollar = '$' )
spring:
  datasource:
    url: ${symbol_dollar}{DB_URL}
    username: ${symbol_dollar}{DB_USERNAME}
    password: ${symbol_dollar}{DB_PASSWORD}
    hikari:
      pool-name: ${symbol_dollar}{HIKARI_POOL_NAME:${symbol_dollar}{spring.application.name}-hikari}
      maximum-pool-size: ${symbol_dollar}{HIKARI_MAX_POOL_SIZE:20}
      minimum-idle: ${symbol_dollar}{HIKARI_MIN_IDLE:5}
      connection-timeout: ${symbol_dollar}{HIKARI_CONNECTION_TIMEOUT:30000}
      idle-timeout: ${symbol_dollar}{HIKARI_IDLE_TIMEOUT:600000}
      max-lifetime: ${symbol_dollar}{HIKARI_MAX_LIFETIME:1800000}
      keepalive-time: ${symbol_dollar}{HIKARI_KEEPALIVE_TIME:300000}
      validation-timeout: ${symbol_dollar}{HIKARI_VALIDATION_TIMEOUT:5000}
  flyway:
    enabled: ${symbol_dollar}{FLYWAY_ENABLED:true}
    locations: classpath:db/migration
    baseline-on-migrate: ${symbol_dollar}{FLYWAY_BASELINE_ON_MIGRATE:false}
    validate-on-migrate: true
    clean-disabled: true
  jpa:
    hibernate:
      ddl-auto: validate
    open-in-view: false
    show-sql: false

management:
  endpoint:
    health:
      show-details: never
```

- [ ] **Step 7: Update archetype metadata for new resource files**

In Light metadata, add these includes to the root file set:

```xml
<include>Dockerfile</include>
<include>.dockerignore</include>
```

In Light `src/main/resources` file set, include properties and nested metadata:

```xml
<include>**/*.properties</include>
<include>META-INF/**</include>
```

In Web and Service metadata root file sets, include:

```xml
<include>Dockerfile</include>
<include>.dockerignore</include>
```

In Web starter and Service starter resource file sets, include:

```xml
<include>**/*.properties</include>
<include>META-INF/**</include>
```

- [ ] **Step 8: Run validation**

Run:

```bash
bash ./mvnw -B -ntp -pl egon-cola-archetypes/egon-cola-archetype-light -am clean integration-test
bash ./mvnw -B -ntp -pl egon-cola-archetypes/egon-cola-archetype-web -am clean integration-test
bash ./mvnw -B -ntp -pl egon-cola-archetypes/egon-cola-archetype-service -am clean integration-test
```

Expected: each command ends with `BUILD SUCCESS`.

- [ ] **Step 9: Commit**

```bash
git status --short
git add -- \
  egon-cola-archetypes/egon-cola-archetype-light \
  egon-cola-archetypes/egon-cola-archetype-web \
  egon-cola-archetypes/egon-cola-archetype-service
git diff --cached --check
git commit -m "feat(archetype): add runtime config baseline"
```

Expected: commit succeeds and no `target/` files are staged.

## Task 2: Configuration Encryption Support

**Files:**
- Create the encryption class files and `spring.factories` files listed in File Structure for Web, Service, and Light.
- Modify all three `verify.groovy` files.
- Add generated tests:
  - Web: `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-starter/src/test/java/starter/config/encryption/AesGcmConfigDecryptorTest.java`
  - Service: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-starter/src/test/java/starter/config/encryption/AesGcmConfigDecryptorTest.java`
  - Light: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/test/java/start/config/encryption/AesGcmConfigDecryptorTest.java`

- [ ] **Step 1: Add failing encryption assertions**

In each `verify.groovy`, assert the generated files exist.

Light:

```groovy
assertFile("src/main/java/it/pkg/start/config/encryption/ConfigDecryptor.java")
assertFile("src/main/java/it/pkg/start/config/encryption/AesGcmConfigDecryptor.java")
assertFile("src/main/java/it/pkg/start/config/encryption/ConfigDecryptEnvironmentPostProcessor.java")
assertFile("src/main/java/it/pkg/start/config/encryption/ConfigCipherCli.java")
assertFile("src/main/resources/META-INF/spring.factories")
assertFile("src/test/java/it/pkg/start/config/encryption/AesGcmConfigDecryptorTest.java")
```

Web:

```groovy
assertFile("student-management-organization-starter/src/main/java/it/pkg/starter/config/encryption/ConfigDecryptor.java")
assertFile("student-management-organization-starter/src/main/java/it/pkg/starter/config/encryption/AesGcmConfigDecryptor.java")
assertFile("student-management-organization-starter/src/main/java/it/pkg/starter/config/encryption/ConfigDecryptEnvironmentPostProcessor.java")
assertFile("student-management-organization-starter/src/main/java/it/pkg/starter/config/encryption/ConfigCipherCli.java")
assertFile("student-management-organization-starter/src/main/resources/META-INF/spring.factories")
assertFile("student-management-organization-starter/src/test/java/it/pkg/starter/config/encryption/AesGcmConfigDecryptorTest.java")
```

Service:

```groovy
assertFile("student-management-evaluation-starter/src/main/java/it/pkg/starter/config/encryption/ConfigDecryptor.java")
assertFile("student-management-evaluation-starter/src/main/java/it/pkg/starter/config/encryption/AesGcmConfigDecryptor.java")
assertFile("student-management-evaluation-starter/src/main/java/it/pkg/starter/config/encryption/ConfigDecryptEnvironmentPostProcessor.java")
assertFile("student-management-evaluation-starter/src/main/java/it/pkg/starter/config/encryption/ConfigCipherCli.java")
assertFile("student-management-evaluation-starter/src/main/resources/META-INF/spring.factories")
assertFile("student-management-evaluation-starter/src/test/java/it/pkg/starter/config/encryption/AesGcmConfigDecryptorTest.java")
```

- [ ] **Step 2: Run one archetype IT and confirm it fails**

Run:

```bash
bash ./mvnw -B -ntp -pl egon-cola-archetypes/egon-cola-archetype-light -am clean integration-test
```

Expected: `BUILD FAILURE` with a missing encryption class assertion.

- [ ] **Step 3: Create encryption interfaces and exceptions**

For Web/Service use `package starter.config.encryption;`. For Light use `package start.config.encryption;`.

`ConfigDecryptor.java`:

```java
package starter.config.encryption;

public interface ConfigDecryptor {

    boolean supports(String value);

    String decrypt(String value, char[] key);
}
```

`ConfigDecryptException.java`:

```java
package starter.config.encryption;

public class ConfigDecryptException extends RuntimeException {

    public ConfigDecryptException(String message) {
        super(message);
    }

    public ConfigDecryptException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

- [ ] **Step 4: Create AES-GCM decryptor**

Create `AesGcmConfigDecryptor.java` with this content, changing only the package line for Light:

```java
package starter.config.encryption;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.Base64;
import java.util.Objects;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class AesGcmConfigDecryptor implements ConfigDecryptor {

    private static final String PREFIX = "ENC(";
    private static final String SUFFIX = ")";
    private static final String VERSION = "v1";
    private static final int GCM_TAG_BITS = 128;

    @Override
    public boolean supports(String value) {
        return value != null && value.startsWith(PREFIX) && value.endsWith(SUFFIX);
    }

    @Override
    public String decrypt(String value, char[] key) {
        if (!supports(value)) {
            return value;
        }
        Objects.requireNonNull(key, "key must not be null");
        String[] parts = value.substring(PREFIX.length(), value.length() - SUFFIX.length()).split(":");
        if (parts.length != 4 || !VERSION.equals(parts[0])) {
            throw new ConfigDecryptException("Invalid encrypted configuration value format");
        }
        try {
            byte[] iv = Base64.getDecoder().decode(parts[1]);
            byte[] cipherText = Base64.getDecoder().decode(parts[2]);
            byte[] tag = Base64.getDecoder().decode(parts[3]);
            byte[] encrypted = new byte[cipherText.length + tag.length];
            System.arraycopy(cipherText, 0, encrypted, 0, cipherText.length);
            System.arraycopy(tag, 0, encrypted, cipherText.length, tag.length);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, secretKey(key), new GCMParameterSpec(GCM_TAG_BITS, iv));
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException | GeneralSecurityException ex) {
            throw new ConfigDecryptException("Failed to decrypt encrypted configuration value", ex);
        }
    }

    private SecretKey secretKey(char[] key) {
        byte[] bytes = new String(key).getBytes(StandardCharsets.UTF_8);
        if (bytes.length != 32) {
            Arrays.fill(bytes, (byte) 0);
            throw new ConfigDecryptException("EGON_CONFIG_DECRYPT_KEY must be 32 UTF-8 bytes for AES-256-GCM");
        }
        return new SecretKeySpec(bytes, "AES");
    }
}
```

- [ ] **Step 5: Create key provider**

Create `ConfigDecryptKeyProvider.java` with this content:

```java
package starter.config.encryption;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

public class ConfigDecryptKeyProvider {

    private static final String KEY_ENV = "EGON_CONFIG_DECRYPT_KEY";
    private static final String KEY_FILE_ENV = "EGON_CONFIG_DECRYPT_KEY_FILE";
    private static final Path DEFAULT_KEY_FILE = Path.of("/run/secrets/egon_config_decrypt_key");

    public Optional<char[]> resolveKey() {
        String key = System.getenv(KEY_ENV);
        if (key != null && !key.isBlank()) {
            return Optional.of(key.toCharArray());
        }
        String keyFile = System.getenv(KEY_FILE_ENV);
        if (keyFile != null && !keyFile.isBlank()) {
            return Optional.of(readKey(Path.of(keyFile)));
        }
        if (Files.isRegularFile(DEFAULT_KEY_FILE)) {
            return Optional.of(readKey(DEFAULT_KEY_FILE));
        }
        return Optional.empty();
    }

    private char[] readKey(Path path) {
        try {
            return Files.readString(path, StandardCharsets.UTF_8).trim().toCharArray();
        } catch (IOException ex) {
            throw new ConfigDecryptException("Failed to read configuration decrypt key file", ex);
        }
    }
}
```

- [ ] **Step 6: Create environment post processor**

Create `ConfigDecryptEnvironmentPostProcessor.java` with this structure:

```java
package starter.config.encryption;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;

public class ConfigDecryptEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    private final ConfigDecryptor decryptor = new AesGcmConfigDecryptor();
    private final ConfigDecryptKeyProvider keyProvider = new ConfigDecryptKeyProvider();

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        Optional<char[]> key = keyProvider.resolveKey();
        for (PropertySource<?> propertySource : environment.getPropertySources()) {
            if (!(propertySource instanceof EnumerablePropertySource<?> enumerablePropertySource)) {
                continue;
            }
            Map<String, Object> decrypted = new LinkedHashMap<>();
            for (String propertyName : enumerablePropertySource.getPropertyNames()) {
                Object value = enumerablePropertySource.getProperty(propertyName);
                if (value instanceof String text && decryptor.supports(text)) {
                    char[] resolvedKey = key.orElseThrow(() ->
                        new ConfigDecryptException("Encrypted configuration value requires EGON_CONFIG_DECRYPT_KEY"));
                    decrypted.put(propertyName, decryptor.decrypt(text, resolvedKey));
                }
            }
            if (!decrypted.isEmpty()) {
                environment.getPropertySources().addBefore(
                    propertySource.getName(),
                    new MapPropertySource(propertySource.getName() + "-decrypted", decrypted)
                );
            }
        }
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 10;
    }
}
```

- [ ] **Step 7: Register the post processor**

For Web/Service `spring.factories`:

```properties
org.springframework.boot.env.EnvironmentPostProcessor=\
${package}.starter.config.encryption.ConfigDecryptEnvironmentPostProcessor
```

For Light `spring.factories`:

```properties
org.springframework.boot.env.EnvironmentPostProcessor=\
${package}.start.config.encryption.ConfigDecryptEnvironmentPostProcessor
```

- [ ] **Step 8: Add CLI helper and unit tests**

Create `ConfigCipherCli.java` with package adjusted per archetype:

```java
package starter.config.encryption;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public final class ConfigCipherCli {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private ConfigCipherCli() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            throw new IllegalArgumentException("Usage: ConfigCipherCli <32-byte-key> <plain-text>");
        }
        System.out.println(encrypt(args[0].toCharArray(), args[1]));
    }

    static String encrypt(char[] key, String plainText) throws Exception {
        byte[] keyBytes = new String(key).getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length != 32) {
            throw new IllegalArgumentException("key must be 32 UTF-8 bytes");
        }
        byte[] iv = new byte[12];
        SECURE_RANDOM.nextBytes(iv);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(keyBytes, "AES"), new GCMParameterSpec(128, iv));
        byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
        byte[] cipherText = Arrays.copyOf(encrypted, encrypted.length - 16);
        byte[] tag = Arrays.copyOfRange(encrypted, encrypted.length - 16, encrypted.length);
        return "ENC(v1:%s:%s:%s)".formatted(
            Base64.getEncoder().encodeToString(iv),
            Base64.getEncoder().encodeToString(cipherText),
            Base64.getEncoder().encodeToString(tag)
        );
    }
}
```

Create `AesGcmConfigDecryptorTest.java` with package adjusted per archetype:

```java
package starter.config.encryption;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class AesGcmConfigDecryptorTest {

    private final AesGcmConfigDecryptor decryptor = new AesGcmConfigDecryptor();

    @Test
    void decryptsValueCreatedByCli() throws Exception {
        char[] key = "12345678901234567890123456789012".toCharArray();
        String encrypted = ConfigCipherCli.encrypt(key, "secret-value");
        assertThat(encrypted).startsWith("ENC(v1:");
        assertThat(decryptor.decrypt(encrypted, key)).isEqualTo("secret-value");
    }

    @Test
    void encryptsSamePlainTextWithDifferentIv() throws Exception {
        char[] key = "12345678901234567890123456789012".toCharArray();
        String first = ConfigCipherCli.encrypt(key, "same-value");
        String second = ConfigCipherCli.encrypt(key, "same-value");
        assertThat(first).isNotEqualTo(second);
    }

    @Test
    void rejectsInvalidKeyLength() {
        assertThatThrownBy(() -> decryptor.decrypt("ENC(v1:a:b:c)", "short".toCharArray()))
            .isInstanceOf(ConfigDecryptException.class);
    }
}
```

- [ ] **Step 9: Run validation**

Run the three archetype integration tests:

```bash
bash ./mvnw -B -ntp -pl egon-cola-archetypes/egon-cola-archetype-light -am clean integration-test
bash ./mvnw -B -ntp -pl egon-cola-archetypes/egon-cola-archetype-web -am clean integration-test
bash ./mvnw -B -ntp -pl egon-cola-archetypes/egon-cola-archetype-service -am clean integration-test
```

Expected: each command ends with `BUILD SUCCESS`.

- [ ] **Step 10: Commit**

```bash
git status --short
git add -- \
  egon-cola-archetypes/egon-cola-archetype-light \
  egon-cola-archetypes/egon-cola-archetype-web \
  egon-cola-archetypes/egon-cola-archetype-service
git diff --cached --check
git commit -m "feat(archetype): add config encryption baseline"
```

## Task 3: Validation Dependencies, DTO Annotations, And Boundary Utilities

**Files:**
- Modify all facade, domain, application, infrastructure, adapter POMs listed in File Structure.
- Create all `ValidatorUtils.java` files listed in File Structure.
- Modify all request DTOs, controllers, facade implementations, and exception handlers listed in File Structure.
- Modify all three `verify.groovy` files.

- [ ] **Step 1: Add failing validation assertions**

For Web and Service facade POMs, assert `jakarta.validation-api` exists and `spring-boot-starter-validation` does not:

```groovy
assertDependency(facadeDependencies, "jakarta.validation-api")
assertNoDependency(facadeDependencies, "spring-boot-starter-validation")
```

For Web and Service domain, application, infrastructure, and adapter POMs, assert:

```groovy
assertDependency(domainDependencies, "spring-boot-starter-validation")
assertDependency(applicationDependencies, "spring-boot-starter-validation")
assertDependency(infrastructureDependencies, "spring-boot-starter-validation")
assertDependency(adapterDependencies, "spring-boot-starter-validation")
```

For Service adapter, keep:

```groovy
assertNoDependency(adapterDependencies, "spring-boot-starter-web")
```

For generated files, add matching assertions:

```groovy
assertFile("student-management-organization-adapter/src/main/java/it/pkg/adapter/validation/ValidatorUtils.java")
assertFile("student-management-evaluation-adapter/src/main/java/it/pkg/adapter/validation/ValidatorUtils.java")
assertFile("src/main/java/it/pkg/adapter/validation/ValidatorUtils.java")
```

Use only the matching path in each archetype verify script.

- [ ] **Step 2: Run one validation and confirm it fails**

Run:

```bash
bash ./mvnw -B -ntp -pl egon-cola-archetypes/egon-cola-archetype-web -am clean integration-test
```

Expected: `BUILD FAILURE` with a facade validation dependency or `ValidatorUtils` assertion.

- [ ] **Step 3: Fix validation dependencies**

In Web and Service facade POMs, replace `spring-boot-starter-validation` with:

```xml
<dependency>
    <groupId>jakarta.validation</groupId>
    <artifactId>jakarta.validation-api</artifactId>
</dependency>
```

In Web and Service domain/application/infrastructure/adapter POMs, add:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>
```

In Light POM, keep `spring-boot-starter-validation`.

- [ ] **Step 4: Create `ValidatorUtils`**

For Web/Service:

```java
package adapter.validation;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ValidationException;
import jakarta.validation.Validator;
import java.util.Comparator;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component("validatorUtils")
@RequiredArgsConstructor
public class ValidatorUtils {

    private final Validator validator;

    public <T> void validate(T target, Class<?>... groups) {
        Set<ConstraintViolation<T>> violations = validator.validate(target, groups);
        if (!violations.isEmpty()) {
            throw new ValidationException(toMessage(violations));
        }
    }

    private <T> String toMessage(Set<ConstraintViolation<T>> violations) {
        return violations.stream()
            .sorted(Comparator.comparing(violation -> violation.getPropertyPath().toString()))
            .map(violation -> violation.getPropertyPath() + " " + violation.getMessage())
            .collect(Collectors.joining("; "));
    }
}
```

For Light, use the same class with `package adapter.validation;`.

- [ ] **Step 5: Add Bean Validation annotations to facade DTOs**

For create request DTOs, add imports and field annotations. Use these patterns:

```java
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
```

Name fields:

```java
@NotBlank(message = "name must not be blank")
@Size(max = 64, message = "name length must be less than or equal to 64")
private String name;
```

Email fields:

```java
@NotBlank(message = "email must not be blank")
@Email(message = "email format is invalid")
private String email;
```

Credit or score fields:

```java
@Positive(message = "credit must be positive")
private int credit;
```

ID fields:

```java
@NotBlank(message = "id must not be blank")
private String userId;
```

Do this in the request DTOs listed in File Structure.

- [ ] **Step 6: Enable validation at adapter boundaries**

For Web/Light controllers, add `@Validated` on the class and `@Valid` on request bodies:

```java
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;

@Validated
public class UserController {

    public SingleResponse<UserDTO> create(@Valid @RequestBody CreateUserRequest request) {
        return SingleResponse.of(userAdapterConverter.toDto(userManage.create(request.name(), request.email())));
    }
}
```

For Service/Web/Light facade implementations, add `@Validated` on the class and `@Valid` on request parameters:

```java
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;

@Validated
public class CourseFacadeImpl implements CourseFacade {

    @Override
    public SingleResponse<CourseDTO> createCourse(@Valid CreateCourseRequest request) {
        try {
            Course course = courseManage.create(request.name(), request.credit());
            return SingleResponse.of(courseAdapterConvertor.toDTO(course));
        } catch (BizException exception) {
            return serviceExceptionHandler.handleSingle(exception);
        } catch (ValidationException exception) {
            return serviceExceptionHandler.handleSingle(exception);
        } catch (Exception exception) {
            return serviceExceptionHandler.handleSingle(exception);
        }
    }
}
```

- [ ] **Step 7: Add validation exception handling**

In Web and Light `GlobalExceptionHandler`, add a method for `jakarta.validation.ValidationException`:

```java
@ExceptionHandler(ValidationException.class)
public Response handleValidationException(ValidationException ex) {
    return Response.fail(ErrorCodes.VALIDATION_ERROR, ex.getMessage());
}
```

If Light lacks `VALIDATION_ERROR`, add this constant to `common.constants.ErrorCodes`:

```java
public static final String VALIDATION_ERROR = "VALIDATION_ERROR";
```

In Service `ServiceExceptionHandler`, do not add `@ExceptionHandler` because it is not a web advice class. Add overloads that preserve the existing facade response style:

```java
private static final String VALIDATION_ERROR = "VALIDATION_ERROR";

public Response handle(ValidationException exception) {
    return Response.failure(VALIDATION_ERROR, exception.getMessage());
}

public <T> SingleResponse<T> handleSingle(ValidationException exception) {
    return SingleResponse.fail(VALIDATION_ERROR, exception.getMessage());
}
```

Then update service facade implementations to catch `ValidationException` before the generic `Exception` catch:

```java
} catch (ValidationException exception) {
    return serviceExceptionHandler.handleSingle(exception);
}
```

- [ ] **Step 8: Run validation**

```bash
bash ./mvnw -B -ntp -pl egon-cola-archetypes/egon-cola-archetype-light -am clean integration-test
bash ./mvnw -B -ntp -pl egon-cola-archetypes/egon-cola-archetype-web -am clean integration-test
bash ./mvnw -B -ntp -pl egon-cola-archetypes/egon-cola-archetype-service -am clean integration-test
```

Expected: each command ends with `BUILD SUCCESS`.

- [ ] **Step 9: Commit**

```bash
git status --short
git add -- \
  egon-cola-archetypes/egon-cola-archetype-light \
  egon-cola-archetypes/egon-cola-archetype-web \
  egon-cola-archetypes/egon-cola-archetype-service
git diff --cached --check
git commit -m "feat(archetype): align validation baseline"
```

## Task 4: Async, Virtual Threads, Graceful Shutdown, Tomcat, Actuator, Jackson, Flyway, And Hikari

**Files:**
- Create the `AsyncConfiguration.java` files listed in File Structure.
- Modify all application profile YAML files created in Task 1.
- Modify all three `verify.groovy` files.

- [ ] **Step 1: Add failing runtime assertions**

In each verify script, read the generated `application.yml` and assert:

```groovy
assert applicationYaml.contains("threads:")
assert applicationYaml.contains("virtual:")
assert applicationYaml.contains('${SPRING_THREADS_VIRTUAL_ENABLED:true}')
assert applicationYaml.contains("timeout-per-shutdown-phase")
assert applicationYaml.contains("write-dates-as-timestamps: false")
assert applicationYaml.contains("prometheus")
```

For Web/Light assert:

```groovy
assert applicationYaml.contains("tomcat:")
assert applicationYaml.contains('${TOMCAT_MAX_CONNECTIONS:8192}')
```

For Service assert:

```groovy
assert applicationYaml.contains('${MANAGEMENT_SERVER_PORT:8081}')
assert !applicationYaml.contains("tomcat:")
```

Assert async config file exists:

```groovy
assertFile("student-management-organization-starter/src/main/java/it/pkg/starter/config/async/AsyncConfiguration.java")
assertFile("student-management-evaluation-starter/src/main/java/it/pkg/starter/config/async/AsyncConfiguration.java")
assertFile("src/main/java/it/pkg/start/config/async/AsyncConfiguration.java")
```

Use only the matching path in each archetype verify script.

- [ ] **Step 2: Run one archetype IT and confirm it fails**

```bash
bash ./mvnw -B -ntp -pl egon-cola-archetypes/egon-cola-archetype-service -am clean integration-test
```

Expected: `BUILD FAILURE` with a missing async config or management port assertion.

- [ ] **Step 3: Add async configuration**

For Web/Service use `package starter.config.async;`. For Light use `package start.config.async;`.

```java
package starter.config.async;

import java.lang.reflect.Method;
import java.util.concurrent.Executor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.boot.task.ThreadPoolTaskExecutorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;

@Slf4j
@EnableAsync
@Configuration
public class AsyncConfiguration implements AsyncConfigurer {

    @Bean("applicationTaskExecutor")
    public AsyncTaskExecutor applicationTaskExecutor(ThreadPoolTaskExecutorBuilder builder) {
        return builder.build();
    }

    @Override
    public Executor getAsyncExecutor() {
        return null;
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (Throwable ex, Method method, Object... params) ->
            log.error("Async method failed: {}", method.toGenericString(), ex);
    }
}
```

This lets Spring Boot provide virtual-thread execution when `spring.threads.virtual.enabled=true` and uses configured bounded pool settings when virtual threads are disabled.

- [ ] **Step 4: Verify YAML contains required runtime settings**

Check the generated YAML files contain:

```bash
rg -n "SPRING_THREADS_VIRTUAL_ENABLED|ASYNC_THREAD_NAME_PREFIX|SHUTDOWN_TIMEOUT|TOMCAT_MAX_CONNECTIONS|MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE|HIKARI_MAX_POOL_SIZE|write-dates-as-timestamps" \
  egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources \
  egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources \
  egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources
```

Expected: output includes the new application YAML files for all three archetypes.

- [ ] **Step 5: Run validation**

```bash
bash ./mvnw -B -ntp -pl egon-cola-archetypes/egon-cola-archetype-light -am clean integration-test
bash ./mvnw -B -ntp -pl egon-cola-archetypes/egon-cola-archetype-web -am clean integration-test
bash ./mvnw -B -ntp -pl egon-cola-archetypes/egon-cola-archetype-service -am clean integration-test
```

Expected: each command ends with `BUILD SUCCESS`.

- [ ] **Step 6: Commit**

```bash
git status --short
git add -- \
  egon-cola-archetypes/egon-cola-archetype-light \
  egon-cola-archetypes/egon-cola-archetype-web \
  egon-cola-archetypes/egon-cola-archetype-service
git diff --cached --check
git commit -m "feat(archetype): add runtime operations config"
```

## Task 5: Layered Jar Packaging, Dockerfile, Dockerignore, And README

**Files:**
- Create all `Dockerfile` and `.dockerignore` paths listed in File Structure.
- Modify Light POM and Web/Service starter POMs.
- Modify all three generated `README.md` files.
- Modify all three `__gitignore__` files.
- Modify all three `verify.groovy` files.

- [ ] **Step 1: Add failing packaging and Docker assertions**

In each verify script, assert:

```groovy
assertFile("Dockerfile")
assertFile(".dockerignore")
assertFile("README.md").text.contains("Docker")
assertFile(".gitignore").text.contains(".env")
```

For runnable POM text, assert:

```groovy
assert runnablePomText.contains("<artifactId>spring-boot-maven-plugin</artifactId>")
assert runnablePomText.contains("<layers>")
assert runnablePomText.contains("<enabled>true</enabled>")
assert runnablePomText.contains("<artifactId>lombok</artifactId>")
```

Use `pom` for Light. Use starter POM text for Web/Service.

- [ ] **Step 2: Run one archetype IT and confirm it fails**

```bash
bash ./mvnw -B -ntp -pl egon-cola-archetypes/egon-cola-archetype-web -am clean integration-test
```

Expected: `BUILD FAILURE` with a missing `Dockerfile` assertion.

- [ ] **Step 3: Add Spring Boot layered jar plugin**

In Web and Service starter POMs and in Light POM, ensure the Spring Boot plugin contains:

```xml
<plugin>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-maven-plugin</artifactId>
    <configuration>
        <layers>
            <enabled>true</enabled>
        </layers>
        <excludes>
            <exclude>
                <groupId>org.projectlombok</groupId>
                <artifactId>lombok</artifactId>
            </exclude>
        </excludes>
    </configuration>
</plugin>
```

Do not add this plugin to non-runnable Web/Service modules.

- [ ] **Step 4: Create Web/Service Dockerfile**

Create this file for Web and Service:

```dockerfile
# syntax=docker/dockerfile:1.7

FROM eclipse-temurin:21-jdk-jammy AS builder
WORKDIR /workspace

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
COPY */pom.xml ./

RUN --mount=type=cache,target=/root/.m2 \
    ./mvnw -B -ntp -DskipTests dependency:go-offline

COPY . .

RUN --mount=type=cache,target=/root/.m2 \
    ./mvnw -B -ntp -DskipTests package

FROM eclipse-temurin:21-jre-jammy AS extractor
WORKDIR /workspace

ARG STARTER_MODULE=${rootArtifactId}-starter
ARG JAR_FILE=${STARTER_MODULE}/target/*.jar

COPY --from=builder /workspace/${JAR_FILE} app.jar
RUN java -Djarmode=tools -jar app.jar extract --layers --destination extracted

FROM eclipse-temurin:21-jre-jammy AS runtime
WORKDIR /app

ENV TZ=Asia/Shanghai \
    SPRING_PROFILES_ACTIVE=prod \
    JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError -Djava.security.egd=file:/dev/./urandom"

RUN groupadd -r app && useradd -r -g app app

COPY --from=extractor /workspace/extracted/dependencies/ ./
COPY --from=extractor /workspace/extracted/spring-boot-loader/ ./
COPY --from=extractor /workspace/extracted/snapshot-dependencies/ ./
COPY --from=extractor /workspace/extracted/application/ ./

USER app

EXPOSE 8080 50051

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS org.springframework.boot.loader.launch.JarLauncher"]
```

For Service, use:

```dockerfile
EXPOSE 8081 50051
```

- [ ] **Step 5: Create Light Dockerfile**

```dockerfile
# syntax=docker/dockerfile:1.7

FROM eclipse-temurin:21-jdk-jammy AS builder
WORKDIR /workspace

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./

RUN --mount=type=cache,target=/root/.m2 \
    ./mvnw -B -ntp -DskipTests dependency:go-offline

COPY . .

RUN --mount=type=cache,target=/root/.m2 \
    ./mvnw -B -ntp -DskipTests package

FROM eclipse-temurin:21-jre-jammy AS extractor
WORKDIR /workspace

ARG JAR_FILE=target/*.jar
COPY --from=builder /workspace/${JAR_FILE} app.jar
RUN java -Djarmode=tools -jar app.jar extract --layers --destination extracted

FROM eclipse-temurin:21-jre-jammy AS runtime
WORKDIR /app

ENV TZ=Asia/Shanghai \
    SPRING_PROFILES_ACTIVE=prod \
    JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError -Djava.security.egd=file:/dev/./urandom"

RUN groupadd -r app && useradd -r -g app app

COPY --from=extractor /workspace/extracted/dependencies/ ./
COPY --from=extractor /workspace/extracted/spring-boot-loader/ ./
COPY --from=extractor /workspace/extracted/snapshot-dependencies/ ./
COPY --from=extractor /workspace/extracted/application/ ./

USER app

EXPOSE 8080 50051

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS org.springframework.boot.loader.launch.JarLauncher"]
```

- [ ] **Step 6: Create `.dockerignore`**

Use this exact content in all three archetypes:

```dockerignore
.git
.gitignore
.github
.idea
.vscode
*.iml
.DS_Store

**/target
**/build
**/.mvn/wrapper/maven-wrapper.jar

logs
*.log

.env
.env.*
config/*secret*
secrets
*.pem
*.key
```

- [ ] **Step 7: Update generated `.gitignore`**

Add these lines to each `__gitignore__` template:

```gitignore
.env
.env.*
config/application-secrets.yml
secrets/
*.pem
*.key
```

- [ ] **Step 8: Update README runtime section**

Add a concise section to each generated README:

```markdown
## Runtime Baseline

The generated project defaults to `local` profile. `local` and `test` do not require Nacos. `dev` and `prod` can connect to Nacos and Dubbo registry through environment variables.

Build:

```bash
./mvnw -V --no-transfer-progress clean test
./mvnw -V --no-transfer-progress -DskipTests package
```

Docker build:

```bash
docker build -t ${rootArtifactId}:local .
```

Sensitive configuration must be provided through environment variables, mounted files, `config/application-secrets.yml`, or `configtree:/run/secrets/`. Do not commit real credentials or decryption keys.
```

For Light, use `${artifactId}` in the Docker tag example.

- [ ] **Step 9: Run validation**

```bash
bash ./mvnw -B -ntp -pl egon-cola-archetypes/egon-cola-archetype-light -am clean integration-test
bash ./mvnw -B -ntp -pl egon-cola-archetypes/egon-cola-archetype-web -am clean integration-test
bash ./mvnw -B -ntp -pl egon-cola-archetypes/egon-cola-archetype-service -am clean integration-test
```

Expected: each command ends with `BUILD SUCCESS`.

- [ ] **Step 10: Commit**

```bash
git status --short
git add -- \
  egon-cola-archetypes/egon-cola-archetype-light \
  egon-cola-archetypes/egon-cola-archetype-web \
  egon-cola-archetypes/egon-cola-archetype-service
git diff --cached --check
git commit -m "feat(archetype): add layered jar docker packaging"
```

## Task 6: Generated Project Test, Package, And Docker Verification

**Files:**
- Modify all three `verify.groovy` files.
- No source template files unless this task exposes a compile, test, or packaging defect from prior tasks.

- [ ] **Step 1: Harden generated project guards**

Add service-specific guards in `egon-cola-archetype-service/src/test/resources/projects/basic/verify.groovy`:

```groovy
def serviceAdapterPomText = assertFile("student-management-evaluation-adapter/pom.xml").text
assert !serviceAdapterPomText.contains("spring-boot-starter-web")
assert !serviceAdapterPomText.contains("spring-boot-starter-webflux")

def serviceStarterPomText = assertFile("student-management-evaluation-starter/pom.xml").text
assert serviceStarterPomText.contains("spring-boot-starter-web")
assert serviceStarterPomText.contains("spring-boot-starter-actuator")

def allServiceJavaPaths = []
new File(projectDir, "student-management-evaluation-adapter/src/main/java").eachFileRecurse { file ->
    if (file.isFile() && file.name.endsWith(".java")) {
        allServiceJavaPaths << projectDir.toPath().relativize(file.toPath()).toString().replace(File.separator, "/")
    }
}
assert allServiceJavaPaths.every { !it.contains("/controller/") }
assert allServiceJavaPaths.every { !it.contains("/web/") }
assert allServiceJavaPaths.every { !it.contains("/filter/") }
```

Add Docker and config guards to all three scripts:

```groovy
def dockerfile = assertFile("Dockerfile").text
assert dockerfile.contains("FROM eclipse-temurin:21-jdk-jammy AS builder")
assert dockerfile.contains("FROM eclipse-temurin:21-jre-jammy AS runtime")
assert dockerfile.contains("USER app")
assert dockerfile.contains("JarLauncher")
assert assertFile(".dockerignore").text.contains("**/target")
```

- [ ] **Step 2: Run archetype integration tests**

```bash
bash ./mvnw -B -ntp -pl egon-cola-archetypes/egon-cola-archetype-light -am clean integration-test
bash ./mvnw -B -ntp -pl egon-cola-archetypes/egon-cola-archetype-web -am clean integration-test
bash ./mvnw -B -ntp -pl egon-cola-archetypes/egon-cola-archetype-service -am clean integration-test
```

Expected: each command ends with `BUILD SUCCESS`.

- [ ] **Step 3: Run generated project test and package commands**

Run:

```bash
for artifact in light web service; do
  case "$artifact" in
    light)
      module="egon-cola-archetypes/egon-cola-archetype-light"
      generated_dir="$(find "$module/target" -path '*/project/basic/pom.xml' -print -quit | xargs dirname)"
      ;;
    web)
      module="egon-cola-archetypes/egon-cola-archetype-web"
      generated_dir="$(find "$module/target" -path '*/student-management-organization/pom.xml' -print -quit | xargs dirname)"
      ;;
    service)
      module="egon-cola-archetypes/egon-cola-archetype-service"
      generated_dir="$(find "$module/target" -path '*/student-management-evaluation/pom.xml' -print -quit | xargs dirname)"
      ;;
  esac
  echo "Generated dir for $artifact: $generated_dir"
  test -n "$generated_dir"
  chmod +x "$generated_dir/mvnw"
  SPRING_PROFILES_ACTIVE=test bash "$generated_dir/mvnw" -V --no-transfer-progress -f "$generated_dir/pom.xml" clean test
  SPRING_PROFILES_ACTIVE=test bash "$generated_dir/mvnw" -V --no-transfer-progress -f "$generated_dir/pom.xml" -DskipTests package
done
```

Expected: each generated project test and package command ends with `BUILD SUCCESS`.

- [ ] **Step 4: Run local Docker build only if Docker is available**

Run:

```bash
if command -v docker >/dev/null 2>&1 && docker info >/dev/null 2>&1; then
  for artifact in light web service; do
    case "$artifact" in
      light)
        generated_dir="$(find egon-cola-archetypes/egon-cola-archetype-light/target -path '*/project/basic/pom.xml' -print -quit | xargs dirname)"
        ;;
      web)
        generated_dir="$(find egon-cola-archetypes/egon-cola-archetype-web/target -path '*/student-management-organization/pom.xml' -print -quit | xargs dirname)"
        ;;
      service)
        generated_dir="$(find egon-cola-archetypes/egon-cola-archetype-service/target -path '*/student-management-evaluation/pom.xml' -print -quit | xargs dirname)"
        ;;
    esac
    docker build -t "egon-generated-${artifact}:local" "$generated_dir"
  done
else
  echo "Docker is not available locally; GitHub Actions will run docker build."
fi
```

Expected if Docker is available: all three `docker build` commands succeed. If Docker is unavailable: the command prints `Docker is not available locally; GitHub Actions will run docker build.`

- [ ] **Step 5: Commit**

```bash
git status --short
git add -- \
  egon-cola-archetypes/egon-cola-archetype-light/src/test/resources/projects/basic/verify.groovy \
  egon-cola-archetypes/egon-cola-archetype-web/src/test/resources/projects/basic/verify.groovy \
  egon-cola-archetypes/egon-cola-archetype-service/src/test/resources/projects/basic/verify.groovy
git diff --cached --check
git commit -m "test(archetype): harden runtime baseline verification"
```

## Task 7: GitHub Actions Generated Project Verification

**Files:**
- Modify: `.github/workflows/ci_by_multiply_java_versions.yaml`

- [ ] **Step 1: Add failing workflow expectations by inspection**

Before editing, run:

```bash
rg -n "docker build|SPRING_PROFILES_ACTIVE=test|-DskipTests package|clean test" .github/workflows/ci_by_multiply_java_versions.yaml
```

Expected: output does not contain all four required patterns.

- [ ] **Step 2: Update generated archetype project workflow**

In `.github/workflows/ci_by_multiply_java_versions.yaml`, replace the generated-project build block inside `Verify generated archetype projects` with:

```bash
pushd "${WORK_DIR}/${APP_NAME}"

if [ -f "./mvnw" ]; then
  chmod +x ./mvnw
  export SPRING_PROFILES_ACTIVE=test
  ./mvnw -V --no-transfer-progress clean test
  ./mvnw -V --no-transfer-progress -DskipTests package
  docker build -t "egon-generated-${SHORT_NAME}:java-${JAVA_VERSION}" .
else
  export SPRING_PROFILES_ACTIVE=test
  mvn -V --no-transfer-progress clean test
  mvn -V --no-transfer-progress -DskipTests package
  docker build -t "egon-generated-${SHORT_NAME}:java-${JAVA_VERSION}" .
fi

popd
```

Keep the existing archetype generation loop and Maven cache setup.

- [ ] **Step 3: Limit Docker build to Ubuntu if required**

If the matrix ever adds a non-Ubuntu runner, guard Docker build with:

```yaml
if: runner.os == 'Linux'
```

The current workflow already uses `ubuntu-latest`, so no guard is needed unless the matrix changes.

- [ ] **Step 4: Validate workflow syntax by grep**

Run:

```bash
rg -n "docker build|SPRING_PROFILES_ACTIVE=test|-DskipTests package|clean test" .github/workflows/ci_by_multiply_java_versions.yaml
```

Expected: output includes all four patterns.

- [ ] **Step 5: Run local YAML-safe check**

Run:

```bash
python3 - <<'PY'
from pathlib import Path
path = Path(".github/workflows/ci_by_multiply_java_versions.yaml")
text = path.read_text()
required = [
    "SPRING_PROFILES_ACTIVE=test",
    "-DskipTests package",
    "docker build",
    "clean test",
]
missing = [token for token in required if token not in text]
if missing:
    raise SystemExit(f"missing required workflow tokens: {missing}")
print("workflow token check passed")
PY
```

Expected: `workflow token check passed`.

- [ ] **Step 6: Commit**

```bash
git status --short
git add -- .github/workflows/ci_by_multiply_java_versions.yaml
git diff --cached --check
git commit -m "ci: verify generated archetype docker builds"
```

## Task 8: Final Full Validation

**Files:**
- No source edits expected.
- If validation exposes a defect, fix only the failing task's scoped files and commit with a focused message.

- [ ] **Step 1: Run full archetype verification**

```bash
bash ./mvnw -B -ntp -pl egon-cola-archetypes/egon-cola-archetype-light -am clean integration-test
bash ./mvnw -B -ntp -pl egon-cola-archetypes/egon-cola-archetype-web -am clean integration-test
bash ./mvnw -B -ntp -pl egon-cola-archetypes/egon-cola-archetype-service -am clean integration-test
```

Expected: all three commands end with `BUILD SUCCESS`.

- [ ] **Step 2: Run generated project test/package proof**

```bash
for module in \
  egon-cola-archetypes/egon-cola-archetype-light \
  egon-cola-archetypes/egon-cola-archetype-web \
  egon-cola-archetypes/egon-cola-archetype-service
do
  generated_pom="$(find "$module/target" -path '*/pom.xml' -not -path '*/target/classes/*' -not -path '*/maven-archiver/*' | rg '/(project/basic|student-management-organization|student-management-evaluation)/pom.xml$' | head -n 1)"
  generated_dir="$(dirname "$generated_pom")"
  echo "Testing generated project: $generated_dir"
  chmod +x "$generated_dir/mvnw"
  SPRING_PROFILES_ACTIVE=test bash "$generated_dir/mvnw" -V --no-transfer-progress -f "$generated_dir/pom.xml" clean test
  SPRING_PROFILES_ACTIVE=test bash "$generated_dir/mvnw" -V --no-transfer-progress -f "$generated_dir/pom.xml" -DskipTests package
done
```

Expected: all generated project commands end with `BUILD SUCCESS`.

- [ ] **Step 3: Run source guard checks**

```bash
rg -n "spring-boot-starter-web|spring-boot-starter-webflux" \
  egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-adapter \
  egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-application \
  egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-domain \
  egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure
```

Expected: no output.

Run:

```bash
rg -n "ENC\\(|EGON_CONFIG_DECRYPT_KEY|NACOS_PASSWORD|DB_PASSWORD" \
  egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources \
  egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources \
  egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources
```

Expected: output shows environment-variable references and code paths only; no real secret value is present.

- [ ] **Step 4: Inspect final git state**

```bash
git status --short
```

Expected: clean working tree after all implementation commits. If untracked `target/` files appear, do not stage them.

## Self-Review Checklist

- Spec coverage:
  - Configuration files and profile behavior: Task 1.
  - Bootstrap and Nacos support: Task 1.
  - Configuration encryption: Task 2.
  - Validation dependency and usage rules: Task 3.
  - Virtual threads, async, graceful shutdown, Tomcat, management, Actuator, Jackson, Flyway, Hikari: Task 4.
  - Layered jar, Dockerfile, `.dockerignore`, README: Task 5.
  - Archetype and generated project verification: Task 6.
  - GitHub Actions generated test/package/docker build: Task 7.
  - Final validation and service no-business-Web guard: Task 8.
- Plan red-flag scan:
  - No unresolved markers are intentionally left in this plan.
  - Each task includes concrete files, commands, expected results, and a commit step.
- Type consistency:
  - Encryption packages are `starter.config.encryption` for Web/Service and `start.config.encryption` for Light.
  - Async packages are `starter.config.async` for Web/Service and `start.config.async` for Light.
  - `ConfigDecryptor`, `AesGcmConfigDecryptor`, `ConfigDecryptEnvironmentPostProcessor`, `ConfigDecryptException`, `ConfigDecryptKeyProvider`, and `ConfigCipherCli` names are used consistently across tasks.

## Execution Handoff

Plan complete and saved to `docs/superpowers/plans/2026-07-03-runtime-engineering-archetype-baseline.md`. Two execution options:

**1. Subagent-Driven (recommended)** - I dispatch a fresh subagent per task, review between tasks, fast iteration

**2. Inline Execution** - Execute tasks in this session using executing-plans, batch execution with checkpoints

Which approach?
