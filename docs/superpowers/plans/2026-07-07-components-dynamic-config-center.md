# Components Dynamic Config Center Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the Egon-COLA Dynamic Config Center component with a Spring Boot starter, admin backend, PostgreSQL/SQLite persistence, Redis publish/subscribe refresh, ACK tracking, and a test/sample module.

**Architecture:** Add one new starter-style component under `egon-cola-components` with `starter`, `admin`, and `test` submodules. Keep the admin flat with Controller -> Service -> Repository and keep the starter focused around annotation scanning, local binding, Admin OpenAPI calls, Redis listener refresh, heartbeat, and ACK reporting.

**Tech Stack:** Java 21, Spring Boot 3.5, Spring Data JPA, Flyway, Redisson, Jackson via `JsonUtils`, JUnit 5, Spring Boot Test, SQLite JDBC, PostgreSQL JDBC.

---

## Execution Rules

1. Do not start the admin service or the sample app as a long-running process.
2. Commit once per task after the task validation passes.
3. Keep changes scoped to `egon-cola-components`, `docs/superpowers/plans`, and docs for the new DDC component.
4. Do not modify existing Flyway migrations. DDC is a new component, so create only new DDC migration files.
5. Do not add UI, login, account, role, permission, MySQL, Spring Boot 2.7, or JDK 17 support.
6. Earlier DDC code may be read for reference, but write this implementation in Egon-COLA style.

## File Structure Map

### Reactor and BOM

- Modify: `egon-cola-components/pom.xml`  
  Add `egon-cola-component-dynamic-config-center` to modules and add dependency versions needed by DDC.
- Modify: `egon-cola-components/egon-cola-components-bom/pom.xml`  
  Export only `egon-cola-component-dynamic-config-center-starter`.
- Create: `egon-cola-components/egon-cola-component-dynamic-config-center/pom.xml`  
  Aggregates starter, admin, and test modules.
- Create: `egon-cola-components/egon-cola-component-dynamic-config-center/README.md`  
  Documents module purpose, configuration prefix, API base path, and validation commands.
- Create: `egon-cola-components/egon-cola-component-dynamic-config-center/docs/manifest.md`  
  Documents component manifest and Admin API groups.

### Starter Module

- Create: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-starter/pom.xml`
- Create: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-starter/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
- Create: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-starter/src/main/java/top/egon/cola/component/ddc/annotation/DdcValue.java`
- Create: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-starter/src/main/java/top/egon/cola/component/ddc/config/DdcAutoConfig.java`
- Create: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-starter/src/main/java/top/egon/cola/component/ddc/config/DdcProperties.java`
- Create: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-starter/src/main/java/top/egon/cola/component/ddc/common/DdcKeys.java`
- Create: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-starter/src/main/java/top/egon/cola/component/ddc/common/DdcValueConverter.java`
- Create: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-starter/src/main/java/top/egon/cola/component/ddc/common/DdcValueDefinition.java`
- Create: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-starter/src/main/java/top/egon/cola/component/ddc/common/DdcValueParser.java`
- Create: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-starter/src/main/java/top/egon/cola/component/ddc/common/DdcChecksum.java`
- Create: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-starter/src/main/java/top/egon/cola/component/ddc/common/DdcException.java`
- Create: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-starter/src/main/java/top/egon/cola/component/ddc/model/enums/DdcAckStatus.java`
- Create: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-starter/src/main/java/top/egon/cola/component/ddc/model/enums/DdcValueType.java`
- Create: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-starter/src/main/java/top/egon/cola/component/ddc/model/dto/DdcAckRequest.java`
- Create: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-starter/src/main/java/top/egon/cola/component/ddc/model/dto/DdcDefaultReportRequest.java`
- Create: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-starter/src/main/java/top/egon/cola/component/ddc/model/dto/DdcHeartbeatRequest.java`
- Create: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-starter/src/main/java/top/egon/cola/component/ddc/model/dto/DdcInstanceRegisterRequest.java`
- Create: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-starter/src/main/java/top/egon/cola/component/ddc/model/dto/DdcPublishMessage.java`
- Create: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-starter/src/main/java/top/egon/cola/component/ddc/model/vo/DdcConfigValue.java`
- Create: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-starter/src/main/java/top/egon/cola/component/ddc/model/vo/DdcFieldBinding.java`
- Create: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-starter/src/main/java/top/egon/cola/component/ddc/client/DdcAdminClient.java`
- Create: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-starter/src/main/java/top/egon/cola/component/ddc/client/HttpDdcAdminClient.java`
- Create: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-starter/src/main/java/top/egon/cola/component/ddc/repository/DdcLocalConfigRepository.java`
- Create: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-starter/src/main/java/top/egon/cola/component/ddc/repository/DdcRedisConfigRepository.java`
- Create: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-starter/src/main/java/top/egon/cola/component/ddc/service/DdcFieldBindingService.java`
- Create: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-starter/src/main/java/top/egon/cola/component/ddc/service/DdcInstanceService.java`
- Create: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-starter/src/main/java/top/egon/cola/component/ddc/service/DdcRefreshService.java`
- Create: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-starter/src/main/java/top/egon/cola/component/ddc/processor/DdcBeanPostProcessor.java`
- Create: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-starter/src/main/java/top/egon/cola/component/ddc/listener/DdcRedisChangeListener.java`
- Create tests under `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-starter/src/test/java/top/egon/cola/component/ddc/`.

### Admin Module

- Create: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-admin/pom.xml`
- Create: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-admin/Dockerfile`
- Create: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-admin/src/main/java/top/egon/cola/component/ddc/admin/DynamicConfigCenterAdminApplication.java`
- Create: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-admin/src/main/resources/application.yml`
- Create: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-admin/src/main/resources/application-test.yml`
- Create: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-admin/src/main/resources/db/postgresql/V1__create_ddc_schema.sql`
- Create: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-admin/src/main/resources/db/sqlite/V1__create_ddc_schema.sql`
- Create entity classes under `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-admin/src/main/java/top/egon/cola/component/ddc/admin/model/entity/`.
- Create DTO, VO, enum, repository, service, controller, config, and common classes under the admin package.
- Create tests under `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-admin/src/test/java/top/egon/cola/component/ddc/admin/`.

### Test Module

- Create: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-test/pom.xml`
- Create: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-test/src/main/java/top/egon/cola/component/ddc/test/DynamicConfigCenterTestApplication.java`
- Create: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-test/src/main/java/top/egon/cola/component/ddc/test/service/SampleConfigService.java`
- Create: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-test/src/main/resources/application.yml`
- Create integration-style tests under `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-test/src/test/java/top/egon/cola/component/ddc/test/`.

---

### Task 1: Maven Reactor and Component Skeleton

**Files:**
- Modify: `egon-cola-components/pom.xml`
- Modify: `egon-cola-components/egon-cola-components-bom/pom.xml`
- Create: `egon-cola-components/egon-cola-component-dynamic-config-center/pom.xml`
- Create: `egon-cola-components/egon-cola-component-dynamic-config-center/README.md`
- Create: `egon-cola-components/egon-cola-component-dynamic-config-center/docs/manifest.md`
- Create: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-starter/pom.xml`
- Create: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-admin/pom.xml`
- Create: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-test/pom.xml`

- [ ] **Step 1: Add the component module to the components parent**

In `egon-cola-components/pom.xml`, add the module:

```xml
<module>egon-cola-component-dynamic-config-center</module>
```

Add dependency version properties:

```xml
<flyway.version>11.15.0</flyway.version>
<postgresql.version>42.7.8</postgresql.version>
<sqlite.jdbc.version>3.50.3.0</sqlite.jdbc.version>
```

Add dependency management entries:

```xml
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-core</artifactId>
    <version>${flyway.version}</version>
</dependency>
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-database-postgresql</artifactId>
    <version>${flyway.version}</version>
</dependency>
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <version>${postgresql.version}</version>
</dependency>
<dependency>
    <groupId>org.xerial</groupId>
    <artifactId>sqlite-jdbc</artifactId>
    <version>${sqlite.jdbc.version}</version>
</dependency>
```

Use Spring Boot's imported dependency management for `hibernate-community-dialects` in the admin module dependency.

- [ ] **Step 2: Create the DDC component root POM**

Create `egon-cola-components/egon-cola-component-dynamic-config-center/pom.xml`:

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

    <artifactId>egon-cola-component-dynamic-config-center</artifactId>
    <packaging>pom</packaging>
    <name>egon-cola-component-dynamic-config-center</name>
    <description>Dynamic config center component for Egon COLA.</description>

    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>top.egon</groupId>
                <artifactId>egon-cola-component-common</artifactId>
                <version>${project.version}</version>
            </dependency>
            <dependency>
                <groupId>top.egon</groupId>
                <artifactId>egon-cola-component-dynamic-config-center-starter</artifactId>
                <version>${project.version}</version>
            </dependency>
        </dependencies>
    </dependencyManagement>

    <modules>
        <module>egon-cola-component-dynamic-config-center-starter</module>
        <module>egon-cola-component-dynamic-config-center-admin</module>
        <module>egon-cola-component-dynamic-config-center-test</module>
    </modules>
</project>
```

- [ ] **Step 3: Create starter/admin/test POMs**

Create the starter POM with dependencies:

```xml
<dependencies>
    <dependency>
        <groupId>top.egon</groupId>
        <artifactId>egon-cola-component-common</artifactId>
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
        <groupId>org.springframework</groupId>
        <artifactId>spring-web</artifactId>
    </dependency>
    <dependency>
        <groupId>org.redisson</groupId>
        <artifactId>redisson-spring-boot-starter</artifactId>
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
```

Create the admin POM with dependencies:

```xml
<dependencies>
    <dependency>
        <groupId>top.egon</groupId>
        <artifactId>egon-cola-component-common</artifactId>
    </dependency>
    <dependency>
        <groupId>top.egon</groupId>
        <artifactId>egon-cola-component-dynamic-config-center-starter</artifactId>
        <version>${project.version}</version>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>
    <dependency>
        <groupId>org.flywaydb</groupId>
        <artifactId>flyway-core</artifactId>
    </dependency>
    <dependency>
        <groupId>org.flywaydb</groupId>
        <artifactId>flyway-database-postgresql</artifactId>
    </dependency>
    <dependency>
        <groupId>org.postgresql</groupId>
        <artifactId>postgresql</artifactId>
    </dependency>
    <dependency>
        <groupId>org.xerial</groupId>
        <artifactId>sqlite-jdbc</artifactId>
    </dependency>
    <dependency>
        <groupId>org.hibernate.orm</groupId>
        <artifactId>hibernate-community-dialects</artifactId>
    </dependency>
    <dependency>
        <groupId>org.redisson</groupId>
        <artifactId>redisson-spring-boot-starter</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

Create the test POM with dependencies on starter, admin in test scope, and Spring Boot Web.

- [ ] **Step 4: Export only the starter in the BOM**

In `egon-cola-components/egon-cola-components-bom/pom.xml`, add:

```xml
<dependency>
    <groupId>top.egon</groupId>
    <artifactId>egon-cola-component-dynamic-config-center-starter</artifactId>
    <version>${project.version}</version>
</dependency>
```

Do not add admin or test modules to the BOM.

- [ ] **Step 5: Add component docs**

Create `README.md` with sections:

```markdown
# Egon COLA Dynamic Config Center Component

This component provides Spring Boot dynamic configuration through a business-facing starter and an independently deployable admin backend.

## Modules

| Module | Purpose |
|---|---|
| `egon-cola-component-dynamic-config-center-starter` | Business application SDK, `@DdcValue`, runtime refresh, heartbeat, ACK. |
| `egon-cola-component-dynamic-config-center-admin` | Backend APIs, persistence, Redis cache, publish task management, instance state. |
| `egon-cola-component-dynamic-config-center-test` | Sample application and integration-style validation. |

## Configuration Prefix

`egon.cola.component.ddc`

## Admin API

The admin API base path is `/api/v1/ddc`.
```

- [ ] **Step 6: Validate the skeleton**

Run:

```bash
mvn -pl egon-cola-components/egon-cola-component-dynamic-config-center -am -DskipTests validate
```

Expected: Maven recognizes the new component and completes `BUILD SUCCESS`.

- [ ] **Step 7: Commit**

```bash
git add egon-cola-components/pom.xml \
  egon-cola-components/egon-cola-components-bom/pom.xml \
  egon-cola-components/egon-cola-component-dynamic-config-center
git commit -m "feat: add dynamic config center component skeleton"
```

---

### Task 2: Starter Annotation, Properties, Value Parsing, and Conversion

**Files:**
- Create: starter annotation/config/common/model files listed in the Starter Module map.
- Test: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-starter/src/test/java/top/egon/cola/component/ddc/common/DdcValueParserTest.java`
- Test: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-starter/src/test/java/top/egon/cola/component/ddc/common/DdcValueConverterTest.java`
- Test: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-starter/src/test/java/top/egon/cola/component/ddc/common/DdcKeysTest.java`
- Test: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-starter/src/test/java/top/egon/cola/component/ddc/common/DdcChecksumTest.java`

- [ ] **Step 1: Write parser tests**

Create `DdcValueParserTest`:

```java
class DdcValueParserTest {

    @Test
    void parsesExpressionWithDefaultValue() {
        DdcValueDefinition definition = DdcValueParser.parse("downgradeSwitch:0", "", "", Integer.class);

        assertThat(definition.getKey()).isEqualTo("downgradeSwitch");
        assertThat(definition.getDefaultValue()).isEqualTo("0");
        assertThat(definition.getType()).isEqualTo(Integer.class);
    }

    @Test
    void explicitKeyAndDefaultOverrideExpression() {
        DdcValueDefinition definition = DdcValueParser.parse("ignored:1", "realKey", "2", String.class);

        assertThat(definition.getKey()).isEqualTo("realKey");
        assertThat(definition.getDefaultValue()).isEqualTo("2");
    }

    @Test
    void rejectsBlankKey() {
        assertThatThrownBy(() -> DdcValueParser.parse(":1", "", "", String.class))
                .isInstanceOf(DdcException.class)
                .hasMessageContaining("config key must not be blank");
    }
}
```

- [ ] **Step 2: Implement annotation and parser**

Create `DdcValue.java`:

```java
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DdcValue {
    String value();
    String key() default "";
    String defaultValue() default "";
    Class<?> type() default Object.class;
    boolean required() default false;
    boolean refreshable() default true;
}
```

Create `DdcValueDefinition` with `key`, `defaultValue`, and `type` fields plus getters.

Create `DdcValueParser.parse(String expression, String explicitKey, String explicitDefaultValue, Class<?> explicitType)`:

```java
public static DdcValueDefinition parse(String expression, String explicitKey, String explicitDefaultValue, Class<?> explicitType) {
    String key = hasText(explicitKey) ? explicitKey.trim() : expressionKey(expression);
    if (!hasText(key)) {
        throw new DdcException("config key must not be blank");
    }
    String defaultValue = hasText(explicitDefaultValue) ? explicitDefaultValue : expressionDefault(expression);
    Class<?> type = explicitType == null || explicitType == Object.class ? String.class : explicitType;
    return new DdcValueDefinition(key, defaultValue, type);
}
```

- [ ] **Step 3: Run parser tests**

Run:

```bash
mvn -pl egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-starter -am -Dtest=DdcValueParserTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: `Tests run: 3, Failures: 0, Errors: 0`.

- [ ] **Step 4: Write converter tests**

Create `DdcValueConverterTest` with tests for `String`, `Integer`, primitive `int`, `Long`, `Boolean`, `Double`, `BigDecimal`, enum, `List<String>`, and JSON object:

```java
class DdcValueConverterTest {

    enum Mode {
        ON, OFF
    }

    static class JsonConfig {
        private String name;
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
    }

    private final DdcValueConverter converter = new DdcValueConverter();

    @Test
    void convertsScalarTypes() {
        assertThat(converter.convert("1", Integer.class)).isEqualTo(1);
        assertThat(converter.convert("2", int.class)).isEqualTo(2);
        assertThat(converter.convert("3", Long.class)).isEqualTo(3L);
        assertThat(converter.convert("true", Boolean.class)).isEqualTo(true);
        assertThat(converter.convert("1.5", Double.class)).isEqualTo(1.5D);
        assertThat(converter.convert("9.99", BigDecimal.class)).isEqualByComparingTo("9.99");
    }

    @Test
    void convertsEnumListAndJsonObject() {
        assertThat(converter.convert("ON", Mode.class)).isEqualTo(Mode.ON);
        assertThat(converter.convert("[\"a\",\"b\"]", List.class)).containsExactly("a", "b");
        JsonConfig config = converter.convert("{\"name\":\"demo\"}", JsonConfig.class);
        assertThat(config.getName()).isEqualTo("demo");
    }
}
```

- [ ] **Step 5: Implement converter**

Create `DdcValueConverter.convert(String value, Class<T> targetType)`:

```java
@SuppressWarnings({"unchecked", "rawtypes"})
public <T> T convert(String value, Class<T> targetType) {
    if (targetType == String.class) {
        return (T) value;
    }
    if (targetType == Integer.class || targetType == int.class) {
        return (T) Integer.valueOf(value);
    }
    if (targetType == Long.class || targetType == long.class) {
        return (T) Long.valueOf(value);
    }
    if (targetType == Boolean.class || targetType == boolean.class) {
        return (T) Boolean.valueOf(value);
    }
    if (targetType == Double.class || targetType == double.class) {
        return (T) Double.valueOf(value);
    }
    if (targetType == BigDecimal.class) {
        return (T) new BigDecimal(value);
    }
    if (targetType.isEnum()) {
        return (T) Enum.valueOf((Class<? extends Enum>) targetType.asSubclass(Enum.class), value);
    }
    if (targetType == List.class) {
        return (T) JsonUtils.fromJsonList(value, String.class);
    }
    return JsonUtils.fromJson(value, targetType);
}
```

Wrap conversion failures in `DdcException` with message `convert config value failed`.

- [ ] **Step 6: Implement properties, keys, checksum, and enums**

Create `DdcProperties` with nested classes:

```text
enabled
appCode
env
namespace
admin.endpoint
admin.accessKey
admin.secretKey
admin.signatureEnabled
redis.host
redis.port
redis.password
redis.database
instance.heartbeatIntervalSeconds
instance.heartbeatTimeoutSeconds
consistency.ackEnabled
consistency.failFast
```

Create `DdcKeys` methods:

```java
config(appCode, env, namespace, key)
version(appCode, env, namespace, key)
instance(appCode, env, namespace, instanceId)
instances(appCode, env, namespace)
publish(changeId)
publishAck(changeId)
topic(appCode, env, namespace)
```

Create `DdcChecksum.sha256(DdcPublishMessage message)` using `CryptoUtils.sha256Hex` over `changeId|appCode|env|namespace|configKey|configValue|targetVersion`.

- [ ] **Step 7: Run starter common tests**

Run:

```bash
mvn -pl egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-starter -am -Dtest='DdcValueParserTest,DdcValueConverterTest,DdcKeysTest,DdcChecksumTest' -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: all selected tests pass.

- [ ] **Step 8: Commit**

```bash
git add egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-starter
git commit -m "feat: add ddc starter value parsing"
```

---

### Task 3: Starter Binding, Admin Client, Refresh, Heartbeat, and Auto-Configuration

**Files:**
- Create starter client/repository/service/processor/listener files listed in the Starter Module map.
- Modify: `egon-cola-component-dynamic-config-center-starter/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
- Test: `DdcFieldBindingServiceTest.java`
- Test: `DdcRefreshServiceTest.java`
- Test: `DdcAutoConfigTest.java`
- Test: `HttpDdcAdminClientTest.java`

- [ ] **Step 1: Write binding service tests**

Create `DdcFieldBindingServiceTest`:

```java
class DdcFieldBindingServiceTest {

    static class SampleBean {
        @DdcValue("limit:1")
        private volatile Integer limit;
    }

    @Test
    void bindsAndAssignsAnnotatedField() {
        DdcLocalConfigRepository repository = new DdcLocalConfigRepository();
        DdcFieldBindingService service = new DdcFieldBindingService(repository, new DdcValueConverter());
        SampleBean bean = new SampleBean();

        service.bind(bean, SampleBean.class);
        service.apply("limit", "5", 2L);

        assertThat(bean.limit).isEqualTo(5);
        assertThat(repository.version("limit")).isEqualTo(2L);
        assertThat(repository.bindings("limit")).hasSize(1);
    }
}
```

- [ ] **Step 2: Implement local repository and binding service**

`DdcLocalConfigRepository` keeps:

```java
private final ConcurrentMap<String, CopyOnWriteArrayList<DdcFieldBinding>> bindings = new ConcurrentHashMap<>();
private final ConcurrentMap<String, Long> versions = new ConcurrentHashMap<>();
```

`DdcFieldBindingService.bind(Object bean, Class<?> targetClass)` scans fields with `@DdcValue`, creates `DdcFieldBinding`, and stores by config key.

`DdcFieldBindingService.apply(String key, String value, long version)` converts before field assignment and writes the version after every binding for the key is updated.

- [ ] **Step 3: Write refresh tests**

Create `DdcRefreshServiceTest`:

```java
class DdcRefreshServiceTest {

    @Test
    void ignoresLowerVersionAndReportsIgnoredAck() {
        RecordingAdminClient client = new RecordingAdminClient();
        DdcLocalConfigRepository repository = new DdcLocalConfigRepository();
        repository.updateVersion("switch", 3L);
        DdcRefreshService service = new DdcRefreshService(repository, key -> true, client);

        service.refresh(message("switch", "1", 2L));

        assertThat(client.lastAck().getStatus()).isEqualTo(DdcAckStatus.IGNORED);
    }

    @Test
    void reportsFailedAckWhenApplyFails() {
        RecordingAdminClient client = new RecordingAdminClient();
        DdcRefreshService service = new DdcRefreshService(new DdcLocalConfigRepository(), key -> {
            throw new DdcException("convert config value failed");
        }, client);

        service.refresh(message("switch", "bad", 4L));

        assertThat(client.lastAck().getStatus()).isEqualTo(DdcAckStatus.FAILED);
        assertThat(client.lastAck().getErrorMessage()).contains("convert config value failed");
    }
}
```

- [ ] **Step 4: Implement refresh service and listener**

`DdcRefreshService.refresh(DdcPublishMessage message)`:

```java
Long localVersion = repository.version(message.getConfigKey());
if (localVersion != null && message.getTargetVersion() <= localVersion) {
    adminClient.ack(ack(message, DdcAckStatus.IGNORED, localVersion, null));
    return;
}
try {
    applyFunction.apply(message.getConfigKey(), message.getConfigValue(), message.getTargetVersion());
    adminClient.ack(ack(message, DdcAckStatus.SUCCESS, message.getTargetVersion(), null));
} catch (Exception e) {
    adminClient.ack(ack(message, DdcAckStatus.FAILED, localVersion, e.getMessage()));
}
```

`DdcRedisChangeListener` validates namespace-level messages and delegates to `DdcRefreshService`.

- [ ] **Step 5: Implement Admin client**

Create `DdcAdminClient` methods:

```java
void register(DdcInstanceRegisterRequest request);
void heartbeat(DdcHeartbeatRequest request);
void offline(DdcHeartbeatRequest request);
List<DdcConfigValue> pull();
void reportDefaults(DdcDefaultReportRequest request);
void ack(DdcAckRequest request);
```

`HttpDdcAdminClient` uses Spring `RestClient` and posts to `/api/v1/ddc/openapi/*`. When `signatureEnabled` is true, add headers:

```text
X-DDC-Access-Key
X-DDC-Timestamp
X-DDC-Signature
```

Signature body is `CryptoUtils.hmacSha256Hex(accessKey + "|" + timestamp + "|" + path, secretKey)`.

- [ ] **Step 6: Implement auto-configuration**

`DdcAutoConfig` creates:

```text
DdcProperties
RedissonClient named ddcRedissonClient
DdcLocalConfigRepository
DdcRedisConfigRepository
DdcAdminClient
DdcFieldBindingService
DdcRefreshService
DdcBeanPostProcessor
DdcRedisChangeListener
heartbeat scheduled task through DdcInstanceService
```

Auto configuration is conditional on:

```text
egon.cola.component.ddc.enabled=true
```

`AutoConfiguration.imports` contains:

```text
top.egon.cola.component.ddc.config.DdcAutoConfig
```

- [ ] **Step 7: Run starter tests**

Run:

```bash
mvn -pl egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-starter -am test
```

Expected: starter tests pass.

- [ ] **Step 8: Commit**

```bash
git add egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-starter
git commit -m "feat: add ddc starter runtime refresh"
```

---

### Task 4: Admin Schema, Entities, Repositories, and Failure Recording Foundation

**Files:**
- Create admin resources, schema scripts, entities, repositories, common exception handling.
- Test: `DdcSchemaScriptTest.java`
- Test: `DdcRepositoryTest.java`
- Test: `PublishFailureRecorderTest.java`

- [ ] **Step 1: Write schema contract tests**

Create `DdcSchemaScriptTest` that loads both scripts as resources and asserts every required table name exists:

```java
class DdcSchemaScriptTest {

    @Test
    void postgresqlScriptContainsRequiredTables() throws Exception {
        String sql = script("db/postgresql/V1__create_ddc_schema.sql");
        assertThat(sql).contains("create table ddc_app");
        assertThat(sql).contains("create table ddc_namespace");
        assertThat(sql).contains("create table ddc_config_item");
        assertThat(sql).contains("create table ddc_config_version");
        assertThat(sql).contains("create table ddc_publish_task");
        assertThat(sql).contains("create table ddc_publish_ack");
        assertThat(sql).contains("create table ddc_instance");
        assertThat(sql).contains("create table ddc_operation_log");
    }

    @Test
    void sqliteScriptContainsRequiredTables() throws Exception {
        String sql = script("db/sqlite/V1__create_ddc_schema.sql");
        assertThat(sql).contains("create table ddc_app");
        assertThat(sql).contains("create table ddc_namespace");
        assertThat(sql).contains("create table ddc_config_item");
        assertThat(sql).contains("create table ddc_config_version");
        assertThat(sql).contains("create table ddc_publish_task");
        assertThat(sql).contains("create table ddc_publish_ack");
        assertThat(sql).contains("create table ddc_instance");
        assertThat(sql).contains("create table ddc_operation_log");
    }
}
```

- [ ] **Step 2: Create PostgreSQL and SQLite scripts**

Both scripts create the eight required tables and indexes. Use `varchar(64)` string IDs, `timestamp` for PostgreSQL, and `datetime` for SQLite. Add unique indexes:

```sql
create unique index uk_ddc_config_item_key on ddc_config_item(app_code, env, namespace, config_key);
create unique index uk_ddc_publish_ack_instance on ddc_publish_ack(change_id, instance_id);
create unique index uk_ddc_instance_id on ddc_instance(instance_id);
```

Do not create user, role, or permission tables.

- [ ] **Step 3: Create entity and repository classes**

Create entities:

```text
DdcAppEntity
DdcNamespaceEntity
DdcConfigItemEntity
DdcConfigVersionEntity
DdcPublishTaskEntity
DdcPublishAckEntity
DdcInstanceEntity
DdcOperationLogEntity
```

Each entity has `@Entity`, `@Table(name = "...")`, string `id`, and `createdAt`/`updatedAt` when the table has both columns.

Create repositories:

```text
DdcAppRepository extends JpaRepository<DdcAppEntity, String>
DdcNamespaceRepository extends JpaRepository<DdcNamespaceEntity, String>
DdcConfigItemRepository extends JpaRepository<DdcConfigItemEntity, String>
DdcConfigVersionRepository extends JpaRepository<DdcConfigVersionEntity, String>
DdcPublishTaskRepository extends JpaRepository<DdcPublishTaskEntity, String>
DdcPublishAckRepository extends JpaRepository<DdcPublishAckEntity, String>
DdcInstanceRepository extends JpaRepository<DdcInstanceEntity, String>
DdcOperationLogRepository extends JpaRepository<DdcOperationLogEntity, String>
```

Add query methods for:

```text
findByAppCodeAndEnvAndNamespaceAndConfigKey
findByChangeId
findByChangeIdAndInstanceId
findByAppCodeAndEnvAndNamespace
```

- [ ] **Step 4: Add repository tests**

Create `DdcRepositoryTest` using `@DataJpaTest` and SQLite in-memory URL:

```java
@DataJpaTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:sqlite::memory:",
        "spring.datasource.driver-class-name=org.sqlite.JDBC",
        "spring.jpa.database-platform=org.hibernate.community.dialect.SQLiteDialect",
        "spring.flyway.enabled=false"
})
class DdcRepositoryTest {

    @Autowired
    private DdcConfigItemRepository configItemRepository;

    @Test
    void savesAndFindsConfigItemByNaturalKey() {
        DdcConfigItemEntity entity = new DdcConfigItemEntity();
        entity.setId(IdUtils.simpleUuid());
        entity.setAppCode("demo");
        entity.setEnv("dev");
        entity.setNamespace("default");
        entity.setConfigKey("switch");
        entity.setConfigValue("true");
        entity.setValueType("BOOLEAN");
        entity.setCurrentVersion(1L);
        entity.setEnabled(true);
        entity.setDeleted(false);
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());

        configItemRepository.save(entity);

        assertThat(configItemRepository.findByAppCodeAndEnvAndNamespaceAndConfigKey("demo", "dev", "default", "switch"))
                .isPresent();
    }
}
```

- [ ] **Step 5: Implement independent publish failure recorder**

Create `PublishFailureRecorder` with `@Transactional(propagation = Propagation.REQUIRES_NEW)`:

```java
public void recordFailure(String changeId, String appCode, String env, String namespace, String configKey, String errorMessage) {
    DdcPublishTaskEntity task = publishTaskRepository.findByChangeId(changeId)
            .orElseGet(() -> newFailedTask(changeId, appCode, env, namespace, configKey));
    task.setStatus(PublishStatus.FAILED.name());
    task.setErrorMessage(errorMessage);
    task.setUpdatedAt(LocalDateTime.now());
    publishTaskRepository.save(task);
}
```

- [ ] **Step 6: Run admin persistence tests**

Run:

```bash
mvn -pl egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-admin -am -Dtest='DdcSchemaScriptTest,DdcRepositoryTest,PublishFailureRecorderTest' -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: selected admin tests pass.

- [ ] **Step 7: Commit**

```bash
git add egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-admin
git commit -m "feat: add ddc admin persistence model"
```

---

### Task 5: Admin Services, Publish Consistency Policy, Redis Cache, and ACK State

**Files:**
- Create admin service classes:
  - `DdcAppService.java`
  - `DdcNamespaceService.java`
  - `DdcConfigService.java`
  - `DdcPublishService.java`
  - `DdcInstanceAdminService.java`
  - `DdcCacheService.java`
  - `PublishFailureRecorder.java`
- Create policy classes:
  - `PublishConsistencyPolicy.java`
  - `AsyncPublishConsistencyPolicy.java`
  - `StrongAllAckPublishConsistencyPolicy.java`
  - `StrongQuorumAckPublishConsistencyPolicy.java`
  - `PublishConsistencyPolicyFactory.java`
- Create admin repository:
  - `DdcRedisRepository.java`
- Create tests:
  - `PublishConsistencyPolicyTest.java`
  - `DdcConfigServiceTest.java`
  - `DdcPublishServiceFailureTest.java`
  - `DdcAckServiceTest.java`
  - `DdcCacheServiceTest.java`

- [ ] **Step 1: Write publish policy tests**

Create `PublishConsistencyPolicyTest`:

```java
class PublishConsistencyPolicyTest {

    @Test
    void allAckRequiresEveryTargetSuccess() {
        PublishConsistencyPolicy policy = new StrongAllAckPublishConsistencyPolicy();

        PublishDecision decision = policy.decide(3, 3, 0, 0);

        assertThat(decision.completed()).isTrue();
        assertThat(decision.status()).isEqualTo(PublishStatus.SUCCESS);
    }

    @Test
    void quorumAckCompletesWhenMajoritySucceeded() {
        PublishConsistencyPolicy policy = new StrongQuorumAckPublishConsistencyPolicy();

        PublishDecision decision = policy.decide(5, 3, 1, 0);

        assertThat(decision.completed()).isTrue();
        assertThat(decision.status()).isEqualTo(PublishStatus.SUCCESS);
    }

    @Test
    void asyncCompletesAfterMessagePublish() {
        PublishConsistencyPolicy policy = new AsyncPublishConsistencyPolicy();

        PublishDecision decision = policy.afterMessagePublished();

        assertThat(decision.completed()).isTrue();
        assertThat(decision.status()).isEqualTo(PublishStatus.SUCCESS);
    }
}
```

- [ ] **Step 2: Implement publish policy classes**

`PublishConsistencyPolicy`:

```java
public interface PublishConsistencyPolicy {
    PublishDecision afterMessagePublished();
    PublishDecision decide(int targetCount, int ackCount, int failedCount, int timeoutCount);
}
```

`StrongAllAckPublishConsistencyPolicy` returns success only when `targetCount > 0 && ackCount == targetCount`.  
`StrongQuorumAckPublishConsistencyPolicy` returns success when `ackCount >= targetCount / 2 + 1`.  
`AsyncPublishConsistencyPolicy.afterMessagePublished()` returns completed success.

- [ ] **Step 3: Write config service tests**

Create `DdcConfigServiceTest` for create/update/delete/rollback:

```java
@SpringBootTest
@ActiveProfiles("test")
class DdcConfigServiceTest {

    @Autowired
    private DdcConfigService configService;
    @Autowired
    private DdcConfigVersionRepository versionRepository;

    @Test
    void updateCreatesNewVersion() {
        DdcConfigCreateRequest create = new DdcConfigCreateRequest("demo", "dev", "default", "switch", "false", "false", "BOOLEAN", "switch");
        DdcConfigVO created = configService.create(create, "tester");

        DdcConfigUpdateRequest update = new DdcConfigUpdateRequest(created.getId(), "true", "enable switch", created.getCurrentVersion());
        DdcConfigVO updated = configService.update(update, "tester");

        assertThat(updated.getCurrentVersion()).isEqualTo(2L);
        assertThat(versionRepository.findByConfigIdOrderByVersionDesc(created.getId())).hasSize(2);
    }
}
```

- [ ] **Step 4: Implement config service**

`DdcConfigService` methods:

```text
create(DdcConfigCreateRequest request, String operator)
update(DdcConfigUpdateRequest request, String operator)
delete(String configId, String operator, String reason)
rollback(DdcConfigRollbackRequest request, String operator)
list(DdcConfigQueryRequest request)
versions(String configId)
```

Each mutating method updates `ddc_config_item`, writes `ddc_config_version`, and writes `ddc_operation_log`.

- [ ] **Step 5: Implement Redis repository and cache service**

`DdcRedisRepository` wraps Redisson:

```text
writeConfig(appCode, env, namespace, key, value, version)
publish(DdcPublishMessage message)
writeInstanceHeartbeat(...)
removeInstance(...)
rebuildNamespace(...)
checkConfig(...)
```

`DdcCacheService.rebuild(appCode, env, namespace)` reads enabled non-deleted configs from DB and writes Redis value/version keys.

`DdcCacheService.check(appCode, env, namespace)` compares database and Redis value/version and returns mismatch rows.

- [ ] **Step 6: Write publish failure tests**

Create `DdcPublishServiceFailureTest`:

```java
@SpringBootTest
@ActiveProfiles("test")
class DdcPublishServiceFailureTest {

    @Autowired
    private DdcPublishService publishService;
    @Autowired
    private DdcPublishTaskRepository publishTaskRepository;

    @Test
    void transactionFailureCreatesFailedRecordAndThrows() {
        DdcPublishRequest request = DdcPublishRequest.invalidForTest("demo", "dev", "default", "switch");

        assertThatThrownBy(() -> publishService.publish(request, "tester"))
                .isInstanceOf(DdcAdminException.class);

        assertThat(publishTaskRepository.findAll())
                .anyMatch(task -> "FAILED".equals(task.getStatus()) && task.getErrorMessage() != null);
    }
}
```

- [ ] **Step 7: Implement publish service**

`DdcPublishService.publish(DdcPublishRequest request, String operator)`:

```java
String changeId = IdUtils.simpleUuid();
try {
    DdcPublishTaskEntity task = transactionalPublishPrepare(changeId, request, operator);
    redisRepository.writeConfig(...);
    redisRepository.publish(message);
    return waitOrReturn(task, request.getPublishMode());
} catch (Exception e) {
    failureRecorder.recordFailure(changeId, request.getAppCode(), request.getEnv(), request.getNamespace(), request.getConfigKey(), e.getMessage());
    throw new DdcAdminException("publish config failed", e);
}
```

`transactionalPublishPrepare` uses `@Transactional` and performs config item update, version insert, publish task insert, target ACK initialization, and operation log insert.

- [ ] **Step 8: Implement ACK update**

`DdcPublishService.ack(DdcAckRequest request)`:

```text
find publish ack by changeId + instanceId
update status, errorMessage, ackAt, currentVersion
recount SUCCESS, FAILED, IGNORED
apply PublishConsistencyPolicy
update ddc_publish_task status and counts
```

Duplicate ACK updates the same row rather than creating another row.

- [ ] **Step 9: Run service tests**

Run:

```bash
mvn -pl egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-admin -am -Dtest='PublishConsistencyPolicyTest,DdcConfigServiceTest,DdcPublishServiceFailureTest,DdcAckServiceTest,DdcCacheServiceTest' -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: selected service tests pass.

- [ ] **Step 10: Commit**

```bash
git add egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-admin
git commit -m "feat: add ddc admin publish services"
```

---

### Task 6: Admin Controllers, SDK OpenAPI, Configuration, and Dockerfile

**Files:**
- Create admin controllers:
  - `DdcAppController.java`
  - `DdcNamespaceController.java`
  - `DdcConfigController.java`
  - `DdcPublishTaskController.java`
  - `DdcInstanceController.java`
  - `DdcCacheController.java`
  - `DdcOpenApiController.java`
  - `DdcManifestController.java`
- Create admin config:
  - `DdcAdminProperties.java`
  - `DdcAdminRedisConfig.java`
  - `DdcTraceIdFilter.java`
  - `DdcGlobalExceptionHandler.java`
- Create model DTO/VO classes used by controllers.
- Test: `DdcOpenApiControllerTest.java`
- Test: `DdcConfigControllerTest.java`
- Test: `DdcManifestControllerTest.java`

- [ ] **Step 1: Write controller tests**

Create `DdcOpenApiControllerTest`:

```java
@WebMvcTest(DdcOpenApiController.class)
class DdcOpenApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DdcInstanceAdminService instanceAdminService;

    @MockBean
    private DdcConfigService configService;

    @MockBean
    private DdcPublishService publishService;

    @Test
    void ackReturnsSuccessResult() throws Exception {
        mockMvc.perform(post("/api/v1/ddc/openapi/publish/ack")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"changeId":"c1","instanceId":"i1","appCode":"demo","env":"dev","namespace":"default","configKey":"switch","targetVersion":2,"currentVersion":2,"status":"SUCCESS"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
```

- [ ] **Step 2: Implement controllers using common Result**

Every controller returns `Result<T>` or `PageResult<T>`.

`DdcOpenApiController` paths:

```text
POST /api/v1/ddc/openapi/instances/register
POST /api/v1/ddc/openapi/instances/heartbeat
POST /api/v1/ddc/openapi/instances/offline
GET  /api/v1/ddc/openapi/configs/pull
GET  /api/v1/ddc/openapi/configs/{key}
POST /api/v1/ddc/openapi/publish/ack
POST /api/v1/ddc/openapi/defaults/report
```

`DdcConfigController` paths:

```text
GET    /api/v1/ddc/configs
POST   /api/v1/ddc/configs
PUT    /api/v1/ddc/configs/{id}
DELETE /api/v1/ddc/configs/{id}
POST   /api/v1/ddc/configs/{id}/publish
GET    /api/v1/ddc/configs/{id}/versions
POST   /api/v1/ddc/configs/{id}/rollback
```

- [ ] **Step 3: Implement manifest endpoint**

`DdcManifestController` path:

```text
GET /api/v1/ddc/manifest
```

Return:

```json
{
  "component": "dynamic-config-center",
  "displayName": "Dynamic Config Center",
  "version": "5.2.0-SNAPSHOT",
  "enabled": true,
  "baseApiPath": "/api/v1/ddc",
  "frontendModuleKey": "dynamic-config-center",
  "routeBase": "/components/dynamic-config-center"
}
```

- [ ] **Step 4: Add admin configuration files**

`application.yml` contains:

```yaml
server:
  port: 18080
spring:
  application:
    name: egon-cola-ddc-admin
  flyway:
    locations: classpath:db/postgresql
egon:
  cola:
    component:
      ddc:
        admin:
          redis:
            host: 127.0.0.1
            port: 6379
            database: 0
```

`application-test.yml` uses SQLite in-memory and disables Redis-dependent startup paths in controller slice tests.

- [ ] **Step 5: Add Dockerfile**

Use the existing dynamic-thread-pool admin Dockerfile style:

```dockerfile
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY target/egon-cola-component-dynamic-config-center-admin.jar app.jar
EXPOSE 18080
ENTRYPOINT ["java","-jar","/app/app.jar"]
```

- [ ] **Step 6: Run controller tests**

Run:

```bash
mvn -pl egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-admin -am -Dtest='DdcOpenApiControllerTest,DdcConfigControllerTest,DdcManifestControllerTest' -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: selected controller tests pass.

- [ ] **Step 7: Commit**

```bash
git add egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-admin
git commit -m "feat: add ddc admin api"
```

---

### Task 7: Test Module, End-to-End Main Flow Tests, Docs, and Full Verification

**Files:**
- Create all files listed in the Test Module map.
- Modify: `egon-cola-components/egon-cola-component-dynamic-config-center/README.md`
- Modify: `egon-cola-components/egon-cola-component-dynamic-config-center/docs/manifest.md`
- Test: `DdcSampleInjectionTest.java`
- Test: `DdcSampleRefreshFlowTest.java`
- Test: `DdcComponentBoundaryTest.java`

- [ ] **Step 1: Create sample service**

Create `SampleConfigService`:

```java
@Service
public class SampleConfigService {

    @DdcValue("downgradeSwitch:false")
    private volatile Boolean downgradeSwitch;

    @DdcValue("rateLimit:100")
    private volatile Integer rateLimit;

    public Boolean getDowngradeSwitch() {
        return downgradeSwitch;
    }

    public Integer getRateLimit() {
        return rateLimit;
    }
}
```

- [ ] **Step 2: Write sample injection test**

Create `DdcSampleInjectionTest`:

```java
@SpringBootTest(properties = {
        "egon.cola.component.ddc.enabled=true",
        "egon.cola.component.ddc.app-code=demo-app",
        "egon.cola.component.ddc.env=dev",
        "egon.cola.component.ddc.namespace=default",
        "egon.cola.component.ddc.consistency.fail-fast=false"
})
class DdcSampleInjectionTest {

    @Autowired
    private SampleConfigService sampleConfigService;

    @Test
    void sampleBeanKeepsAnnotationDefaultsWhenAdminUnavailableAndFailFastDisabled() {
        assertThat(sampleConfigService.getDowngradeSwitch()).isFalse();
        assertThat(sampleConfigService.getRateLimit()).isEqualTo(100);
    }
}
```

- [ ] **Step 3: Write refresh flow test**

Create `DdcSampleRefreshFlowTest` using starter services directly instead of starting Redis:

```java
class DdcSampleRefreshFlowTest {

    @Test
    void refreshUpdatesBoundFieldAndReportsSuccessAck() {
        RecordingAdminClient adminClient = new RecordingAdminClient();
        DdcLocalConfigRepository repository = new DdcLocalConfigRepository();
        DdcFieldBindingService bindingService = new DdcFieldBindingService(repository, new DdcValueConverter());
        SampleConfigService sample = new SampleConfigService();
        bindingService.bind(sample, SampleConfigService.class);
        DdcRefreshService refreshService = new DdcRefreshService(repository, bindingService::apply, adminClient);

        refreshService.refresh(message("rateLimit", "200", 2L));

        assertThat(sample.getRateLimit()).isEqualTo(200);
        assertThat(adminClient.lastAck().getStatus()).isEqualTo(DdcAckStatus.SUCCESS);
    }
}
```

- [ ] **Step 4: Add boundary test**

Create `DdcComponentBoundaryTest` in the starter module:

```java
class DdcComponentBoundaryTest {

    @Test
    void starterDoesNotDependOnAdminOrTestPackages() throws Exception {
        List<String> classFiles = Files.walk(Path.of("target/classes"))
                .filter(path -> path.toString().endsWith(".class"))
                .map(Path::toString)
                .toList();

        assertThat(classFiles).noneMatch(path -> path.contains("/admin/"));
        assertThat(classFiles).noneMatch(path -> path.contains("/test/"));
    }
}
```

- [ ] **Step 5: Update docs**

Update component README with:

```text
Starter dependency coordinates.
Configuration prefix and example YAML.
Admin API base path.
PostgreSQL and SQLite Flyway location examples.
Validation commands.
Explicit non-goals: UI, account, RBAC, MySQL.
```

Update `docs/manifest.md` with manifest response fields and API groups.

- [ ] **Step 6: Run module verification**

Run:

```bash
mvn -pl egon-cola-components/egon-cola-component-dynamic-config-center -am test
```

Expected: DDC component tests and required upstream module tests pass.

- [ ] **Step 7: Run package verification without starting services**

Run:

```bash
mvn -pl egon-cola-components/egon-cola-component-dynamic-config-center -am package -DskipTests
```

Expected: starter, admin, and test modules package successfully. Admin jar is created; no long-running service is started.

- [ ] **Step 8: Commit**

```bash
git add egon-cola-components/egon-cola-component-dynamic-config-center
git commit -m "test: add ddc component sample flow"
```

---

## Final Verification

After all tasks are complete, run:

```bash
git status --short --untracked-files=all
mvn -pl egon-cola-components/egon-cola-component-dynamic-config-center -am test
mvn -pl egon-cola-components/egon-cola-component-dynamic-config-center -am package -DskipTests
```

Expected:

```text
git status shows no unstaged implementation changes.
The DDC test command ends with BUILD SUCCESS.
The DDC package command ends with BUILD SUCCESS.
No browser is opened.
No long-running admin or sample app is started.
```

## Self-Review Notes

Spec coverage:

1. Module naming, Maven structure, BOM export, and no UI are covered by Task 1.
2. Starter annotation, parsing, conversion, binding, refresh, heartbeat, ACK, and auto-configuration are covered by Tasks 2 and 3.
3. PostgreSQL and SQLite independent scripts, JPA entities, repositories, and failure recording are covered by Task 4.
4. Admin config/version/publish/cache/instance services, three publish modes, transaction failure marking, Redis failure marking, and ACK idempotency are covered by Task 5.
5. Admin REST APIs, SDK OpenAPI, manifest endpoint, configuration, and Dockerfile are covered by Task 6.
6. Sample/test module and full Maven validation are covered by Task 7.

Scope checks:

1. The plan excludes UI, accounts, RBAC, MySQL, Spring Boot 2.7, and JDK 17 support.
2. The plan does not modify existing dynamic-thread-pool or common component behavior.
3. The plan uses frequent commits, one per task.
