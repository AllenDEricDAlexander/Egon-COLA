# Components Dynamic Thread Pool Migration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Rebuild `egon-cola-components` around `common` plus a copied-and-adapted dynamic-thread-pool starter/admin/test component.

**Architecture:** Copy the working implementation from `/Users/mario/SelfProject/atluofu-dynamic-thread-pool`, then rename Maven coordinates, package roots, configuration prefix, and docs to Egon-COLA conventions. Use Scheme A: admin may depend on starter for shared models and Redis contracts, but only common and starter are exported by BOM.

**Tech Stack:** Java 21, Maven 3.9.14+, Spring Boot 3.5.16, Redisson 3.26.0, Micrometer, JUnit 5, Docker.

---

## Global Constraints

- Do not open a browser or start any project runtime server.
- Keep the implementation copy-first; do not rewrite the dynamic thread pool from scratch.
- Keep `egon-cola-components` on the 5.x line and align this breaking component migration to `5.2.0-SNAPSHOT`.
- Do not preserve old component compatibility modules or old `com.alibaba.cola` packages.
- Do not migrate `atluofu-dynamic-thread-pool-ui` into this repository.
- Do not export admin, test, component root POMs, docs, or UI artifacts from `egon-cola-components-bom`.
- Use path-scoped staging and one commit per task.
- If Redis-backed tests need a live Redis instance, gate them behind `DTP_REDIS_TESTS=true` so the default Maven test path remains deterministic.

## File Structure

Create and maintain this target structure:

```text
egon-cola-components/
├── pom.xml
├── egon-cola-components-bom/pom.xml
├── egon-cola-component-common/
│   ├── pom.xml
│   └── src/
├── egon-cola-component-dynamic-thread-pool/
│   ├── pom.xml
│   ├── README.md
│   ├── docs/
│   ├── egon-cola-component-dynamic-thread-pool-starter/
│   ├── egon-cola-component-dynamic-thread-pool-admin/
│   └── egon-cola-component-dynamic-thread-pool-test/
└── egon-cola-components-architecture.md
```

Source project mapping:

```text
/Users/mario/SelfProject/atluofu-dynamic-thread-pool/atluofu-dynamic-thread-pool-spring-boot-starter
  -> egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter

/Users/mario/SelfProject/atluofu-dynamic-thread-pool/dynamic-thread-pool-admin
  -> egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-admin

/Users/mario/SelfProject/atluofu-dynamic-thread-pool/atluofu-dynamic-thread-pool-test
  -> egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-test
```

Use these naming conversions everywhere under the migrated dynamic-thread-pool component:

```text
top.atluofu.middleware.dynamic.thread.pool.sdk -> top.egon.cola.component.dtp
top.atluofu.middleware.dynamic.thread.pool     -> top.egon.cola.component.dtp.admin
atluofu.dynamic.thread-pool                    -> egon.cola.component.dtp
atluofu-dynamic-thread-pool-spring-boot-starter -> egon-cola-component-dynamic-thread-pool-starter
dynamic-thread-pool-admin                      -> egon-cola-component-dynamic-thread-pool-admin
atluofu-dynamic-thread-pool-test               -> egon-cola-component-dynamic-thread-pool-test
```

## Task 1: Components Parent, Version, BOM, and Common Jar

**Files:**
- Modify: `pom.xml`
- Modify: `egon-cola-components/pom.xml`
- Modify: `egon-cola-components/egon-cola-components-bom/pom.xml`
- Create: `egon-cola-components/egon-cola-component-common/pom.xml`
- Create: `egon-cola-components/egon-cola-component-common/src/main/java/top/egon/cola/component/common/model/DTO.java`
- Create: `egon-cola-components/egon-cola-component-common/src/main/java/top/egon/cola/component/common/model/ClientObject.java`
- Create: `egon-cola-components/egon-cola-component-common/src/main/java/top/egon/cola/component/common/model/Command.java`
- Create: `egon-cola-components/egon-cola-component-common/src/main/java/top/egon/cola/component/common/model/Query.java`
- Create: `egon-cola-components/egon-cola-component-common/src/main/java/top/egon/cola/component/common/result/Response.java`
- Create: `egon-cola-components/egon-cola-component-common/src/main/java/top/egon/cola/component/common/result/SingleResponse.java`
- Create: `egon-cola-components/egon-cola-component-common/src/main/java/top/egon/cola/component/common/result/MultiResponse.java`
- Create: `egon-cola-components/egon-cola-component-common/src/main/java/top/egon/cola/component/common/page/PageQuery.java`
- Create: `egon-cola-components/egon-cola-component-common/src/main/java/top/egon/cola/component/common/page/PageResponse.java`
- Create: `egon-cola-components/egon-cola-component-common/src/main/java/top/egon/cola/component/common/exception/BaseException.java`
- Create: `egon-cola-components/egon-cola-component-common/src/main/java/top/egon/cola/component/common/exception/BizException.java`
- Create: `egon-cola-components/egon-cola-component-common/src/main/java/top/egon/cola/component/common/exception/SysException.java`
- Create: `egon-cola-components/egon-cola-component-common/src/main/java/top/egon/cola/component/common/validation/Assert.java`
- Create: `egon-cola-components/egon-cola-component-common/src/test/java/top/egon/cola/component/common/CommonComponentBoundaryTest.java`
- Delete: legacy component directories listed in Step 4

**Interfaces:**
- Produces Maven artifact `top.egon:egon-cola-component-common:${project.version}`.
- Produces BOM entries for `egon-cola-component-common` and `egon-cola-component-dynamic-thread-pool-starter`.

- [ ] **Step 1: Write the common boundary test**

Create `egon-cola-components/egon-cola-component-common/src/test/java/top/egon/cola/component/common/CommonComponentBoundaryTest.java`:

```java
package top.egon.cola.component.common;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.common.exception.BizException;
import top.egon.cola.component.common.page.PageQuery;
import top.egon.cola.component.common.page.PageResponse;
import top.egon.cola.component.common.result.Response;
import top.egon.cola.component.common.result.SingleResponse;
import top.egon.cola.component.common.validation.Assert;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommonComponentBoundaryTest {

    @Test
    void responseFactoriesKeepColaStyleContract() {
        Response success = Response.buildSuccess();
        assertTrue(success.isSuccess());

        Response failure = Response.buildFailure("BIZ_ERROR", "bad request");
        assertFalse(failure.isSuccess());
        assertEquals("BIZ_ERROR", failure.getErrCode());
        assertEquals("bad request", failure.getErrMessage());

        SingleResponse<String> single = SingleResponse.of("ok");
        assertTrue(single.isSuccess());
        assertEquals("ok", single.getData());
    }

    @Test
    void pageModelsNormalizeInvalidPageValues() {
        PageQuery query = new PageQuery();
        query.setPageIndex(0);
        query.setPageSize(0);

        assertEquals(1, query.getPageIndex());
        assertEquals(1, query.getPageSize());

        PageResponse<String> page = PageResponse.of(List.of("a", "b"), 5, 2, 2);
        assertTrue(page.isSuccess());
        assertEquals(3, page.getTotalPages());
        assertEquals(List.of("a", "b"), page.getData());
    }

    @Test
    void assertThrowsBizException() {
        BizException exception = assertThrows(BizException.class, () -> Assert.notNull(null, "missing value"));
        assertEquals("BIZ_ERROR", exception.getErrCode());
        assertEquals("missing value", exception.getErrMessage());
    }

    @Test
    void commonSourceDoesNotImportRuntimeFrameworks() throws Exception {
        Path sourceRoot = Path.of("src/main/java/top/egon/cola/component/common");
        try (Stream<Path> files = Files.walk(sourceRoot)) {
            List<String> badImports = files
                    .filter(path -> path.toString().endsWith(".java"))
                    .flatMap(path -> {
                        try {
                            return Files.readAllLines(path).stream();
                        } catch (Exception e) {
                            throw new IllegalStateException(e);
                        }
                    })
                    .filter(line -> line.startsWith("import org.springframework.")
                            || line.startsWith("import jakarta.")
                            || line.startsWith("import javax.")
                            || line.startsWith("import org.redisson.")
                            || line.startsWith("import redis."))
                    .toList();
            assertEquals(List.of(), badImports);
        }
    }
}
```

- [ ] **Step 2: Run the common test to verify it fails before the module exists**

Run:

```bash
bash ./mvnw -B -ntp -pl egon-cola-components/egon-cola-component-common -am -Dtest=CommonComponentBoundaryTest test
```

Expected: FAIL because Maven cannot find `egon-cola-components/egon-cola-component-common`.

- [ ] **Step 3: Update repository and components versions to `5.2.0-SNAPSHOT`**

In root `pom.xml`, change:

```xml
<version>5.1.2</version>
```

to:

```xml
<version>5.2.0-SNAPSHOT</version>
```

In `egon-cola-components/pom.xml`, update the parent version:

```xml
<parent>
    <groupId>top.egon</groupId>
    <artifactId>egon-cola-aggregation-parent</artifactId>
    <version>5.2.0-SNAPSHOT</version>
</parent>
```

In `egon-cola-archetypes/pom.xml`, update the parent version to `5.2.0-SNAPSHOT` so the reactor remains coherent.

- [ ] **Step 4: Replace the components parent modules**

In `egon-cola-components/pom.xml`, replace the full `<modules>` block with:

```xml
<modules>
    <module>egon-cola-components-bom</module>
    <module>egon-cola-component-common</module>
    <module>egon-cola-component-dynamic-thread-pool</module>
</modules>
```

Remove old component module entries from `<dependencyManagement>`. Keep `spring-boot-dependencies`, test libraries, Redisson, Micrometer, Lombok, plugin management, central publishing, and release profile configuration.

Delete these legacy module directories:

```bash
git rm -r \
  egon-cola-components/egon-cola-component-dto \
  egon-cola-components/egon-cola-component-exception \
  egon-cola-components/egon-cola-component-statemachine \
  egon-cola-components/egon-cola-component-domain-starter \
  egon-cola-components/egon-cola-component-extension-starter \
  egon-cola-components/egon-cola-component-catchlog-starter \
  egon-cola-components/egon-cola-component-test-container \
  egon-cola-components/egon-cola-component-ruleengine \
  egon-cola-components/egon-cola-component-unittest \
  egon-cola-components/egon-cola-component-job \
  egon-cola-components/egon-cola-dev-util-archetypes
```

- [ ] **Step 5: Create the common POM**

Create `egon-cola-components/egon-cola-component-common/pom.xml`:

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

    <artifactId>egon-cola-component-common</artifactId>
    <packaging>jar</packaging>
    <name>egon-cola-component-common</name>
    <description>Pure common component for Egon COLA.</description>
</project>
```

- [ ] **Step 6: Copy the stable DTO and exception classes into common**

Run these copy commands before deleting old modules if Step 4 has not run. If Step 4 already deleted them, restore only the needed file content from Git using `git show HEAD~1:path`.

```bash
mkdir -p \
  egon-cola-components/egon-cola-component-common/src/main/java/top/egon/cola/component/common/model \
  egon-cola-components/egon-cola-component-common/src/main/java/top/egon/cola/component/common/result \
  egon-cola-components/egon-cola-component-common/src/main/java/top/egon/cola/component/common/page \
  egon-cola-components/egon-cola-component-common/src/main/java/top/egon/cola/component/common/exception \
  egon-cola-components/egon-cola-component-common/src/main/java/top/egon/cola/component/common/validation

cp egon-cola-components/egon-cola-component-dto/src/main/java/com/alibaba/cola/dto/DTO.java \
  egon-cola-components/egon-cola-component-common/src/main/java/top/egon/cola/component/common/model/DTO.java
cp egon-cola-components/egon-cola-component-dto/src/main/java/com/alibaba/cola/dto/ClientObject.java \
  egon-cola-components/egon-cola-component-common/src/main/java/top/egon/cola/component/common/model/ClientObject.java
cp egon-cola-components/egon-cola-component-dto/src/main/java/com/alibaba/cola/dto/Command.java \
  egon-cola-components/egon-cola-component-common/src/main/java/top/egon/cola/component/common/model/Command.java
cp egon-cola-components/egon-cola-component-dto/src/main/java/com/alibaba/cola/dto/Query.java \
  egon-cola-components/egon-cola-component-common/src/main/java/top/egon/cola/component/common/model/Query.java
cp egon-cola-components/egon-cola-component-dto/src/main/java/com/alibaba/cola/dto/Response.java \
  egon-cola-components/egon-cola-component-common/src/main/java/top/egon/cola/component/common/result/Response.java
cp egon-cola-components/egon-cola-component-dto/src/main/java/com/alibaba/cola/dto/SingleResponse.java \
  egon-cola-components/egon-cola-component-common/src/main/java/top/egon/cola/component/common/result/SingleResponse.java
cp egon-cola-components/egon-cola-component-dto/src/main/java/com/alibaba/cola/dto/MultiResponse.java \
  egon-cola-components/egon-cola-component-common/src/main/java/top/egon/cola/component/common/result/MultiResponse.java
cp egon-cola-components/egon-cola-component-dto/src/main/java/com/alibaba/cola/dto/PageQuery.java \
  egon-cola-components/egon-cola-component-common/src/main/java/top/egon/cola/component/common/page/PageQuery.java
cp egon-cola-components/egon-cola-component-dto/src/main/java/com/alibaba/cola/dto/PageResponse.java \
  egon-cola-components/egon-cola-component-common/src/main/java/top/egon/cola/component/common/page/PageResponse.java
cp egon-cola-components/egon-cola-component-exception/src/main/java/com/alibaba/cola/exception/BaseException.java \
  egon-cola-components/egon-cola-component-common/src/main/java/top/egon/cola/component/common/exception/BaseException.java
cp egon-cola-components/egon-cola-component-exception/src/main/java/com/alibaba/cola/exception/BizException.java \
  egon-cola-components/egon-cola-component-common/src/main/java/top/egon/cola/component/common/exception/BizException.java
cp egon-cola-components/egon-cola-component-exception/src/main/java/com/alibaba/cola/exception/SysException.java \
  egon-cola-components/egon-cola-component-common/src/main/java/top/egon/cola/component/common/exception/SysException.java
cp egon-cola-components/egon-cola-component-exception/src/main/java/com/alibaba/cola/exception/Assert.java \
  egon-cola-components/egon-cola-component-common/src/main/java/top/egon/cola/component/common/validation/Assert.java
```

Apply package/import updates:

```bash
perl -pi -e 's/package com\.alibaba\.cola\.dto;/package top.egon.cola.component.common.model;/' \
  egon-cola-components/egon-cola-component-common/src/main/java/top/egon/cola/component/common/model/*.java

perl -pi -e 's/package com\.alibaba\.cola\.dto;/package top.egon.cola.component.common.result;/' \
  egon-cola-components/egon-cola-component-common/src/main/java/top/egon/cola/component/common/result/*.java

perl -pi -e 's/package com\.alibaba\.cola\.dto;/package top.egon.cola.component.common.page;/' \
  egon-cola-components/egon-cola-component-common/src/main/java/top/egon/cola/component/common/page/*.java

perl -pi -e 's/package com\.alibaba\.cola\.exception;/package top.egon.cola.component.common.exception;/' \
  egon-cola-components/egon-cola-component-common/src/main/java/top/egon/cola/component/common/exception/*.java

perl -pi -e 's/package com\.alibaba\.cola\.exception;/package top.egon.cola.component.common.validation;/' \
  egon-cola-components/egon-cola-component-common/src/main/java/top/egon/cola/component/common/validation/Assert.java
```

Then add these imports where compilation requires them:

```java
import top.egon.cola.component.common.model.Query;
import top.egon.cola.component.common.result.Response;
```

`PageQuery.java` needs the `Query` import. `PageResponse.java` needs the `Response` import. `Assert.java` needs `top.egon.cola.component.common.exception.BizException`.

- [ ] **Step 7: Replace the BOM dependency management**

In `egon-cola-components/egon-cola-components-bom/pom.xml`, set version to `5.2.0-SNAPSHOT` and replace the `<dependencyManagement>` contents with:

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>top.egon</groupId>
            <artifactId>egon-cola-component-common</artifactId>
            <version>${project.version}</version>
        </dependency>
        <dependency>
            <groupId>top.egon</groupId>
            <artifactId>egon-cola-component-dynamic-thread-pool-starter</artifactId>
            <version>${project.version}</version>
        </dependency>
    </dependencies>
</dependencyManagement>
```

- [ ] **Step 8: Run common validation**

Run:

```bash
bash ./mvnw -B -ntp -pl egon-cola-components/egon-cola-component-common -am -Dtest=CommonComponentBoundaryTest test
```

Expected: PASS.

Run:

```bash
bash ./mvnw -B -ntp -f egon-cola-components/pom.xml -DskipTests validate
```

Expected: FAIL only because `egon-cola-component-dynamic-thread-pool` has not been created yet.

- [ ] **Step 9: Commit Task 1**

```bash
git status --short
git add -- pom.xml egon-cola-archetypes/pom.xml egon-cola-components
git diff --cached --stat
git commit -m "feat(components): rebuild common module baseline"
```

## Task 2: Dynamic Thread Pool Component Shell and Starter Copy

**Files:**
- Create: `egon-cola-components/egon-cola-component-dynamic-thread-pool/pom.xml`
- Create: `egon-cola-components/egon-cola-component-dynamic-thread-pool/README.md`
- Create: `egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/pom.xml`
- Copy and modify: starter files from `/Users/mario/SelfProject/atluofu-dynamic-thread-pool/atluofu-dynamic-thread-pool-spring-boot-starter`
- Test: copied starter tests under `egon-cola-component-dynamic-thread-pool-starter/src/test/java`

**Interfaces:**
- Produces Maven artifact `top.egon:egon-cola-component-dynamic-thread-pool-starter:${project.version}`.
- Produces auto-configuration import `top.egon.cola.component.dtp.config.DynamicThreadPoolAutoConfig`.
- Produces configuration prefix `egon.cola.component.dtp`.

- [ ] **Step 1: Write the starter migration guard test**

Create `egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/test/java/top/egon/cola/component/dtp/config/DynamicThreadPoolPrefixMigrationTest.java`:

```java
package top.egon.cola.component.dtp.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DynamicThreadPoolPrefixMigrationTest {

    @Test
    void bindsEgonColaDtpPrefix() {
        MapConfigurationPropertySource source = new MapConfigurationPropertySource(Map.of(
                "egon.cola.component.dtp.enabled", "true",
                "egon.cola.component.dtp.app-name", "student-management",
                "egon.cola.component.dtp.instance-id", "student-management-8080",
                "egon.cola.component.dtp.registry.redis.host", "127.0.0.1",
                "egon.cola.component.dtp.report.interval", "30s"
        ));

        DynamicThreadPoolAutoProperties properties = new Binder(source)
                .bind("egon.cola.component.dtp", DynamicThreadPoolAutoProperties.class)
                .orElseThrow();

        assertTrue(properties.isEnabled());
        assertEquals("student-management", properties.getAppName());
        assertEquals("student-management-8080", properties.getInstanceId());
        assertEquals("127.0.0.1", properties.getRegistry().getRedis().getHost());
        assertEquals(30, properties.getReport().getInterval().toSeconds());
    }
}
```

- [ ] **Step 2: Run the starter guard to verify it fails before copy**

Run:

```bash
bash ./mvnw -B -ntp -pl egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter -am -Dtest=DynamicThreadPoolPrefixMigrationTest test
```

Expected: FAIL because the dynamic-thread-pool module does not exist yet.

- [ ] **Step 3: Create the dynamic-thread-pool root POM**

Create `egon-cola-components/egon-cola-component-dynamic-thread-pool/pom.xml`:

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

    <artifactId>egon-cola-component-dynamic-thread-pool</artifactId>
    <packaging>pom</packaging>
    <name>egon-cola-component-dynamic-thread-pool</name>
    <description>Dynamic thread pool component for Egon COLA.</description>

    <modules>
        <module>egon-cola-component-dynamic-thread-pool-starter</module>
    </modules>
</project>
```

- [ ] **Step 4: Copy the starter module**

Run:

```bash
mkdir -p egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter
rsync -a --exclude target \
  /Users/mario/SelfProject/atluofu-dynamic-thread-pool/atluofu-dynamic-thread-pool-spring-boot-starter/ \
  egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/
```

Move Java package directories:

```bash
mkdir -p egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/main/java/top/egon/cola/component
mv egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/main/java/top/atluofu/middleware/dynamic/thread/pool/sdk \
  egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/main/java/top/egon/cola/component/dtp
rm -rf egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/main/java/top/atluofu

mkdir -p egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/test/java/top/egon/cola/component
mv egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/test/java/top/atluofu/middleware/dynamic/thread/pool/sdk \
  egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/test/java/top/egon/cola/component/dtp
rm -rf egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/test/java/top/atluofu
```

Move the copied top-level starter test `ApiTest.java` if it exists:

```bash
if [ -f egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/test/java/top/atluofu/middleware/dynamic/thread/pool/ApiTest.java ]; then
  mkdir -p egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/test/java/top/egon/cola/component/dtp
  mv egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/test/java/top/atluofu/middleware/dynamic/thread/pool/ApiTest.java \
    egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/test/java/top/egon/cola/component/dtp/ApiTest.java
  rm -rf egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/test/java/top/atluofu
fi
```

- [ ] **Step 5: Rewrite starter package names and configuration prefix**

Run:

```bash
find egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src -type f \
  \( -name '*.java' -o -name '*.imports' -o -name '*.yml' -o -name '*.yaml' -o -name '*.properties' -o -name '*.json' \) \
  -exec perl -pi -e 's/top\.atluofu\.middleware\.dynamic\.thread\.pool\.sdk/top.egon.cola.component.dtp/g; s/top\.atluofu\.middleware\.dynamic\.thread\.pool/top.egon.cola.component.dtp/g; s/atluofu\.dynamic\.thread-pool/egon.cola.component.dtp/g' {} +
```

Ensure `src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` contains exactly:

```text
top.egon.cola.component.dtp.config.DynamicThreadPoolAutoConfig
```

In `DynamicThreadPoolAutoProperties.java`, ensure:

```java
@ConfigurationProperties(prefix = "egon.cola.component.dtp", ignoreInvalidFields = true)
```

In `DynamicThreadPoolAutoConfig.java`, ensure:

```java
@ConditionalOnProperty(prefix = "egon.cola.component.dtp", name = "enabled", havingValue = "true", matchIfMissing = true)
```

In `ThreadPoolDataReportJob.java`, ensure:

```java
@Scheduled(fixedDelayString = "${egon.cola.component.dtp.report.interval:20s}")
```

- [ ] **Step 6: Replace the starter POM**

Replace `egon-cola-component-dynamic-thread-pool-starter/pom.xml` with:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>top.egon</groupId>
        <artifactId>egon-cola-component-dynamic-thread-pool</artifactId>
        <version>5.2.0-SNAPSHOT</version>
        <relativePath>../pom.xml</relativePath>
    </parent>

    <artifactId>egon-cola-component-dynamic-thread-pool-starter</artifactId>
    <packaging>jar</packaging>
    <name>egon-cola-component-dynamic-thread-pool-starter</name>
    <description>Spring Boot starter for Egon COLA dynamic thread pool.</description>

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
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-aop</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-configuration-processor</artifactId>
            <optional>true</optional>
        </dependency>
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <scope>provided</scope>
        </dependency>
        <dependency>
            <groupId>commons-lang</groupId>
            <artifactId>commons-lang</artifactId>
        </dependency>
        <dependency>
            <groupId>com.alibaba.fastjson2</groupId>
            <artifactId>fastjson2</artifactId>
        </dependency>
        <dependency>
            <groupId>com.fasterxml.jackson.datatype</groupId>
            <artifactId>jackson-datatype-jsr310</artifactId>
        </dependency>
        <dependency>
            <groupId>io.micrometer</groupId>
            <artifactId>micrometer-core</artifactId>
            <optional>true</optional>
        </dependency>
        <dependency>
            <groupId>org.redisson</groupId>
            <artifactId>redisson-spring-boot-starter</artifactId>
            <version>3.26.0</version>
            <exclusions>
                <exclusion>
                    <groupId>org.springframework.boot</groupId>
                    <artifactId>spring-boot-starter-actuator</artifactId>
                </exclusion>
            </exclusions>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>
</project>
```

- [ ] **Step 7: Run starter tests**

Run:

```bash
bash ./mvnw -B -ntp -pl egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter -am test
```

Expected: PASS. If copied Redis tests require a live Redis instance, mark those tests with:

```java
@EnabledIfEnvironmentVariable(named = "DTP_REDIS_TESTS", matches = "true")
```

Then run the same command again and expect PASS without Redis.

- [ ] **Step 8: Run stale-name checks**

Run:

```bash
rg -n "top\\.atluofu|atluofu\\.dynamic\\.thread-pool|atluofu-dynamic-thread-pool|dynamic.thread.pool.config" \
  egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter
```

Expected: no output.

- [ ] **Step 9: Commit Task 2**

```bash
git status --short
git add -- egon-cola-components/egon-cola-component-dynamic-thread-pool egon-cola-components/pom.xml egon-cola-components/egon-cola-components-bom/pom.xml
git diff --cached --stat
git commit -m "feat(components): migrate dynamic thread pool starter"
```

## Task 3: Admin Copy, Manifest, and Dockerfile

**Files:**
- Modify: `egon-cola-components/egon-cola-component-dynamic-thread-pool/pom.xml`
- Create: `egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-admin/pom.xml`
- Copy and modify: admin files from `/Users/mario/SelfProject/atluofu-dynamic-thread-pool/dynamic-thread-pool-admin`
- Create: `egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-admin/Dockerfile`
- Create: `egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-admin/src/main/java/top/egon/cola/component/dtp/admin/manifest/DtpComponentManifest.java`
- Create: `egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-admin/src/main/java/top/egon/cola/component/dtp/admin/manifest/DtpManifestController.java`
- Test: admin tests under `egon-cola-component-dynamic-thread-pool-admin/src/test/java`

**Interfaces:**
- Produces executable admin jar `egon-cola-component-dynamic-thread-pool-admin`.
- Produces REST API base `/api/v1/dtp`.
- Produces manifest endpoint `GET /api/v1/dtp/manifest`.

- [ ] **Step 1: Write the manifest controller test**

Create `egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-admin/src/test/java/top/egon/cola/component/dtp/admin/manifest/DtpManifestControllerTest.java`:

```java
package top.egon.cola.component.dtp.admin.manifest;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DtpManifestControllerTest {

    @Test
    void manifestDescribesDynamicThreadPoolComponent() {
        DtpManifestController controller = new DtpManifestController();

        DtpComponentManifest manifest = controller.manifest().getData();

        assertEquals("dynamic-thread-pool", manifest.getComponent());
        assertEquals("Dynamic Thread Pool", manifest.getName());
        assertTrue(manifest.isEnabled());
        assertEquals("/api/v1/dtp", manifest.getBaseApi());
        assertEquals("dynamic-thread-pool", manifest.getFrontend().getModule());
        assertEquals("/components/dynamic-thread-pool", manifest.getFrontend().getRouteBase());
        assertFalse(manifest.getFrontend().getMenus().isEmpty());
        assertFalse(manifest.getPermissions().isEmpty());
    }
}
```

- [ ] **Step 2: Run the manifest test to verify it fails before admin copy**

Run:

```bash
bash ./mvnw -B -ntp -pl egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-admin -am -Dtest=DtpManifestControllerTest test
```

Expected: FAIL because the admin module does not exist yet.

- [ ] **Step 3: Add admin module to the dynamic-thread-pool root**

In `egon-cola-component-dynamic-thread-pool/pom.xml`, replace the modules block with:

```xml
<modules>
    <module>egon-cola-component-dynamic-thread-pool-starter</module>
    <module>egon-cola-component-dynamic-thread-pool-admin</module>
</modules>
```

- [ ] **Step 4: Copy the admin module**

Run:

```bash
mkdir -p egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-admin
rsync -a --exclude target \
  /Users/mario/SelfProject/atluofu-dynamic-thread-pool/dynamic-thread-pool-admin/ \
  egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-admin/

mkdir -p egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-admin/src/main/java/top/egon/cola/component/dtp
mv egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-admin/src/main/java/top/atluofu/middleware/dynamic/thread/pool \
  egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-admin/src/main/java/top/egon/cola/component/dtp/admin
rm -rf egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-admin/src/main/java/top/atluofu

mkdir -p egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-admin/src/test/java/top/egon/cola/component/dtp
mv egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-admin/src/test/java/top/atluofu/middleware/dynamic/thread/pool \
  egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-admin/src/test/java/top/egon/cola/component/dtp/admin
rm -rf egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-admin/src/test/java/top/atluofu
```

- [ ] **Step 5: Rewrite admin package names and imports**

Run:

```bash
find egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-admin/src -type f \
  \( -name '*.java' -o -name '*.yml' -o -name '*.yaml' -o -name '*.properties' \) \
  -exec perl -pi -e 's/top\.atluofu\.middleware\.dynamic\.thread\.pool\.sdk/top.egon.cola.component.dtp/g; s/top\.atluofu\.middleware\.dynamic\.thread\.pool/top.egon.cola.component.dtp.admin/g; s/atluofu\.dynamic\.thread-pool/egon.cola.component.dtp/g' {} +
```

In the copied `AdminApplication.java`, ensure:

```java
@SpringBootApplication(
        exclude = DynamicThreadPoolAutoConfig.class,
        scanBasePackages = {
                "top.egon.cola.component.dtp.admin.config",
                "top.egon.cola.component.dtp.admin.trigger",
                "top.egon.cola.component.dtp.admin.manifest"
        }
)
```

Also ensure the admin Redis properties use:

```java
@ConfigurationProperties(prefix = "egon.cola.component.dtp.registry.redis", ignoreInvalidFields = true)
```

- [ ] **Step 6: Replace the admin POM**

Replace `egon-cola-component-dynamic-thread-pool-admin/pom.xml` with:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>top.egon</groupId>
        <artifactId>egon-cola-component-dynamic-thread-pool</artifactId>
        <version>5.2.0-SNAPSHOT</version>
        <relativePath>../pom.xml</relativePath>
    </parent>

    <artifactId>egon-cola-component-dynamic-thread-pool-admin</artifactId>
    <packaging>jar</packaging>
    <name>egon-cola-component-dynamic-thread-pool-admin</name>
    <description>Admin service for Egon COLA dynamic thread pool.</description>

    <dependencies>
        <dependency>
            <groupId>top.egon</groupId>
            <artifactId>egon-cola-component-dynamic-thread-pool-starter</artifactId>
            <version>${project.version}</version>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-configuration-processor</artifactId>
        </dependency>
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <scope>provided</scope>
        </dependency>
        <dependency>
            <groupId>com.alibaba.fastjson2</groupId>
            <artifactId>fastjson2</artifactId>
        </dependency>
        <dependency>
            <groupId>org.apache.commons</groupId>
            <artifactId>commons-lang3</artifactId>
        </dependency>
        <dependency>
            <groupId>commons-codec</groupId>
            <artifactId>commons-codec</artifactId>
        </dependency>
        <dependency>
            <groupId>org.redisson</groupId>
            <artifactId>redisson-spring-boot-starter</artifactId>
            <version>3.26.0</version>
            <exclusions>
                <exclusion>
                    <groupId>org.springframework.boot</groupId>
                    <artifactId>spring-boot-starter-actuator</artifactId>
                </exclusion>
            </exclusions>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.mockito</groupId>
            <artifactId>mockito-core</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <finalName>egon-cola-component-dynamic-thread-pool-admin</finalName>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <configuration>
                    <mainClass>top.egon.cola.component.dtp.admin.AdminApplication</mainClass>
                    <layout>JAR</layout>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

- [ ] **Step 7: Add manifest model and controller**

Create `DtpComponentManifest.java`:

```java
package top.egon.cola.component.dtp.admin.manifest;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DtpComponentManifest {

    private String component;

    private String name;

    private String version;

    private boolean enabled;

    private String baseApi;

    private Frontend frontend;

    private List<Permission> permissions;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Frontend {

        private String module;

        private String routeBase;

        private List<Menu> menus;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Menu {

        private String key;

        private String title;

        private String path;

        private String permission;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Permission {

        private String code;

        private String name;
    }
}
```

Create `DtpManifestController.java`:

```java
package top.egon.cola.component.dtp.admin.manifest;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.egon.cola.component.dtp.admin.types.Response;

import java.util.List;

@RestController
@RequestMapping("/api/v1/dtp")
public class DtpManifestController {

    @Value("${egon.cola.component.dtp.manifest.version:5.2.0-SNAPSHOT}")
    private String version = "5.2.0-SNAPSHOT";

    @GetMapping("/manifest")
    public Response<DtpComponentManifest> manifest() {
        return Response.success(DtpComponentManifest.builder()
                .component("dynamic-thread-pool")
                .name("Dynamic Thread Pool")
                .version(version)
                .enabled(true)
                .baseApi("/api/v1/dtp")
                .frontend(DtpComponentManifest.Frontend.builder()
                        .module("dynamic-thread-pool")
                        .routeBase("/components/dynamic-thread-pool")
                        .menus(List.of(
                                menu("dynamic-thread-pool.apps", "Applications", "/components/dynamic-thread-pool/apps", "dtp:apps:read"),
                                menu("dynamic-thread-pool.events", "Audit Events", "/components/dynamic-thread-pool/events", "dtp:events:read")
                        ))
                        .build())
                .permissions(List.of(
                        permission("dtp:apps:read", "View dynamic thread pool applications"),
                        permission("dtp:executors:read", "View dynamic thread pool executors"),
                        permission("dtp:executors:resize", "Resize platform thread pools"),
                        permission("dtp:executors:virtual-limit", "Update virtual thread concurrency limit"),
                        permission("dtp:events:read", "View dynamic thread pool audit events")
                ))
                .build());
    }

    private DtpComponentManifest.Menu menu(String key, String title, String path, String permission) {
        return DtpComponentManifest.Menu.builder()
                .key(key)
                .title(title)
                .path(path)
                .permission(permission)
                .build();
    }

    private DtpComponentManifest.Permission permission(String code, String name) {
        return DtpComponentManifest.Permission.builder()
                .code(code)
                .name(name)
                .build();
    }
}
```

- [ ] **Step 8: Add Dockerfile**

Create `egon-cola-component-dynamic-thread-pool-admin/Dockerfile`:

```dockerfile
FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

ARG JAR_FILE=target/egon-cola-component-dynamic-thread-pool-admin.jar
COPY ${JAR_FILE} /app/app.jar

ENV JAVA_OPTS="-Xms512m -Xmx512m"
ENV SPRING_PROFILES_ACTIVE="prod"

EXPOSE 8089

ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -jar /app/app.jar --spring.profiles.active=${SPRING_PROFILES_ACTIVE}"]
```

- [ ] **Step 9: Run admin tests**

Run:

```bash
bash ./mvnw -B -ntp -pl egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-admin -am test
```

Expected: PASS.

- [ ] **Step 10: Build admin jar without starting it**

Run:

```bash
bash ./mvnw -B -ntp -pl egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-admin -am -DskipTests package
```

Expected: PASS and jar exists at:

```text
egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-admin/target/egon-cola-component-dynamic-thread-pool-admin.jar
```

- [ ] **Step 11: Run admin stale-name checks**

Run:

```bash
rg -n "top\\.atluofu|atluofu\\.dynamic\\.thread-pool|atluofu-dynamic-thread-pool|dynamic.thread.pool.config|atluofu-dynamic-thread-pool-ui" \
  egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-admin
```

Expected: no output.

- [ ] **Step 12: Commit Task 3**

```bash
git status --short
git add -- egon-cola-components/egon-cola-component-dynamic-thread-pool
git diff --cached --stat
git commit -m "feat(components): migrate dynamic thread pool admin"
```

## Task 4: Consolidated Dynamic Thread Pool Test Module

**Files:**
- Modify: `egon-cola-components/egon-cola-component-dynamic-thread-pool/pom.xml`
- Create: `egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-test/pom.xml`
- Copy and modify: sample/test files from `/Users/mario/SelfProject/atluofu-dynamic-thread-pool/atluofu-dynamic-thread-pool-test`
- Test: `egon-cola-component-dynamic-thread-pool-test/src/test/java/top/egon/cola/component/dtp/test/smoke/DtpSampleSmokeTest.java`

**Interfaces:**
- Produces non-BOM module `egon-cola-component-dynamic-thread-pool-test`.
- Verifies starter integration without requiring default live Redis.

- [ ] **Step 1: Write a sample smoke test**

Create `egon-cola-component-dynamic-thread-pool-test/src/test/java/top/egon/cola/component/dtp/test/smoke/DtpSampleSmokeTest.java`:

```java
package top.egon.cola.component.dtp.test.smoke;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import top.egon.cola.component.dtp.config.DynamicThreadPoolAutoConfig;
import top.egon.cola.component.dtp.executor.ManagedExecutorRegistry;
import top.egon.cola.component.dtp.executor.virtual.BoundedVirtualThreadExecutor;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class DtpSampleSmokeTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(SampleExecutorConfiguration.class)
            .withPropertyValues(
                    "egon.cola.component.dtp.enabled=true",
                    "egon.cola.component.dtp.app-name=dtp-sample",
                    "egon.cola.component.dtp.instance-id=dtp-sample-1",
                    "egon.cola.component.dtp.report.enabled=false"
            )
            .withConfiguration(org.springframework.boot.autoconfigure.AutoConfigurations.of(DynamicThreadPoolAutoConfig.class));

    @Test
    void starterRegistersSampleExecutors() {
        contextRunner.run(context -> {
            ManagedExecutorRegistry registry = context.getBean(ManagedExecutorRegistry.class);
            assertNotNull(registry.find("samplePlatformExecutor"));
            assertNotNull(registry.find("sampleVirtualExecutor"));
        });
    }

    @Configuration
    static class SampleExecutorConfiguration {

        @Bean
        ThreadPoolExecutor samplePlatformExecutor() {
            return new ThreadPoolExecutor(2, 4, 60, TimeUnit.SECONDS, new LinkedBlockingQueue<>(32));
        }

        @Bean
        BoundedVirtualThreadExecutor sampleVirtualExecutor() {
            return new BoundedVirtualThreadExecutor("sample-virtual", 16);
        }
    }
}
```

- [ ] **Step 2: Run the smoke test to verify it fails before the module exists**

Run:

```bash
bash ./mvnw -B -ntp -pl egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-test -am -Dtest=DtpSampleSmokeTest test
```

Expected: FAIL because the test module does not exist yet.

- [ ] **Step 3: Add the test module to the dynamic-thread-pool root**

In `egon-cola-component-dynamic-thread-pool/pom.xml`, replace the modules block with:

```xml
<modules>
    <module>egon-cola-component-dynamic-thread-pool-starter</module>
    <module>egon-cola-component-dynamic-thread-pool-admin</module>
    <module>egon-cola-component-dynamic-thread-pool-test</module>
</modules>
```

- [ ] **Step 4: Copy the sample module**

Run:

```bash
mkdir -p egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-test
rsync -a --exclude target \
  /Users/mario/SelfProject/atluofu-dynamic-thread-pool/atluofu-dynamic-thread-pool-test/ \
  egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-test/
```

Move Java packages:

```bash
mkdir -p egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-test/src/main/java/top/egon/cola/component/dtp/test
mv egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-test/src/main/java/top/atluofu/* \
  egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-test/src/main/java/top/egon/cola/component/dtp/test/
rm -rf egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-test/src/main/java/top/atluofu

mkdir -p egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-test/src/test/java/top/egon/cola/component/dtp/test
if [ -d egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-test/src/test/java/top/atluofu ]; then
  mv egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-test/src/test/java/top/atluofu/* \
    egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-test/src/test/java/top/egon/cola/component/dtp/test/
  rm -rf egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-test/src/test/java/top/atluofu
fi
```

- [ ] **Step 5: Rewrite sample package names and config prefix**

Run:

```bash
find egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-test/src -type f \
  \( -name '*.java' -o -name '*.yml' -o -name '*.yaml' -o -name '*.properties' -o -name '*.xml' \) \
  -exec perl -pi -e 's/top\.atluofu\.middleware\.dynamic\.thread\.pool\.sdk/top.egon.cola.component.dtp/g; s/top\.atluofu/top.egon.cola.component.dtp.test/g; s/atluofu\.dynamic\.thread-pool/egon.cola.component.dtp/g' {} +
```

- [ ] **Step 6: Replace the test module POM**

Replace `egon-cola-component-dynamic-thread-pool-test/pom.xml` with:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>top.egon</groupId>
        <artifactId>egon-cola-component-dynamic-thread-pool</artifactId>
        <version>5.2.0-SNAPSHOT</version>
        <relativePath>../pom.xml</relativePath>
    </parent>

    <artifactId>egon-cola-component-dynamic-thread-pool-test</artifactId>
    <packaging>jar</packaging>
    <name>egon-cola-component-dynamic-thread-pool-test</name>
    <description>Sample and integration tests for Egon COLA dynamic thread pool.</description>

    <dependencies>
        <dependency>
            <groupId>top.egon</groupId>
            <artifactId>egon-cola-component-dynamic-thread-pool-starter</artifactId>
            <version>${project.version}</version>
        </dependency>
        <dependency>
            <groupId>top.egon</groupId>
            <artifactId>egon-cola-component-dynamic-thread-pool-admin</artifactId>
            <version>${project.version}</version>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <scope>provided</scope>
        </dependency>
    </dependencies>

    <build>
        <finalName>egon-cola-component-dynamic-thread-pool-test</finalName>
    </build>
</project>
```

- [ ] **Step 7: Gate Redis integration tests**

For copied tests that connect to Redis, add:

```java
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

@EnabledIfEnvironmentVariable(named = "DTP_REDIS_TESTS", matches = "true")
```

Apply the annotation at class level on Redis-backed test classes. Keep pure unit and Spring context tests enabled by default.

- [ ] **Step 8: Run test module validation**

Run:

```bash
bash ./mvnw -B -ntp -pl egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-test -am test
```

Expected: PASS without live Redis.

Run optional Redis validation only when Redis is intentionally available:

```bash
DTP_REDIS_TESTS=true bash ./mvnw -B -ntp -pl egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-test -am test
```

Expected: PASS when Redis config is available.

- [ ] **Step 9: Run test-module stale-name checks**

Run:

```bash
rg -n "top\\.atluofu|atluofu\\.dynamic\\.thread-pool|atluofu-dynamic-thread-pool|atluofu-dynamic-thread-pool-ui|/Users/mario/SelfProject/be" \
  egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-test
```

Expected: no output.

- [ ] **Step 10: Commit Task 4**

```bash
git status --short
git add -- egon-cola-components/egon-cola-component-dynamic-thread-pool
git diff --cached --stat
git commit -m "test(components): add dynamic thread pool sample module"
```

## Task 5: Documentation, Archetype Defaults, and Optional Starter Guidance

**Files:**
- Modify: `README.md`
- Modify: `egon-cola-components/egon-cola-components-architecture.md`
- Create or modify: `egon-cola-components/egon-cola-component-dynamic-thread-pool/README.md`
- Create: `egon-cola-components/egon-cola-component-dynamic-thread-pool/docs/manifest.md`
- Modify: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/pom.xml`
- Modify: `egon-cola-archetypes/egon-cola-archetype-light/src/test/resources/projects/basic/verify.groovy`
- Modify: `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/pom.xml`
- Modify: `egon-cola-archetypes/egon-cola-archetype-web/src/test/resources/projects/basic/verify.groovy`
- Modify: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/pom.xml`
- Modify: `egon-cola-archetypes/egon-cola-archetype-service/src/test/resources/projects/basic/verify.groovy`

**Interfaces:**
- Generated projects import `egon-cola-components-bom`.
- Generated projects depend on `egon-cola-component-common` by default.
- Generated projects do not depend on `egon-cola-component-dynamic-thread-pool-starter` by default.

- [ ] **Step 1: Write archetype dependency guard updates**

In each active `verify.groovy`, add these assertions near the root POM assertions:

```groovy
assert rootPomText.contains("<artifactId>egon-cola-components-bom</artifactId>")
assert rootPomText.contains("<egon-cola.version>5.2.0-SNAPSHOT</egon-cola.version>")
assert rootPomText.contains("<artifactId>egon-cola-component-common</artifactId>")
assert !rootPomText.contains("<artifactId>egon-cola-component-dynamic-thread-pool-starter</artifactId>")
assert !rootPomText.contains("<artifactId>egon-cola-component-dynamic-thread-pool-admin</artifactId>")
assert !rootPomText.contains("<artifactId>egon-cola-component-dynamic-thread-pool-test</artifactId>")
```

For light archetype, the variable is named `pom`; use:

```groovy
assert pom.contains("<artifactId>egon-cola-components-bom</artifactId>")
assert pom.contains("<egon-cola.version>5.2.0-SNAPSHOT</egon-cola.version>")
assert pom.contains("<artifactId>egon-cola-component-common</artifactId>")
assert !pom.contains("<artifactId>egon-cola-component-dynamic-thread-pool-starter</artifactId>")
assert !pom.contains("<artifactId>egon-cola-component-dynamic-thread-pool-admin</artifactId>")
assert !pom.contains("<artifactId>egon-cola-component-dynamic-thread-pool-test</artifactId>")
```

- [ ] **Step 2: Run archetype ITs to verify they fail before POM updates**

Run:

```bash
bash ./mvnw -B -ntp -pl egon-cola-archetypes/egon-cola-archetype-light -am clean integration-test
bash ./mvnw -B -ntp -pl egon-cola-archetypes/egon-cola-archetype-web -am clean integration-test
bash ./mvnw -B -ntp -pl egon-cola-archetypes/egon-cola-archetype-service -am clean integration-test
```

Expected: FAIL because generated POMs do not yet import the components BOM or common component.

- [ ] **Step 3: Add common dependency to light archetype**

In `egon-cola-archetype-light/src/main/resources/archetype-resources/pom.xml`, add this property:

```xml
<egon-cola.version>5.2.0-SNAPSHOT</egon-cola.version>
```

Inside `<dependencyManagement><dependencies>`, add:

```xml
<dependency>
    <groupId>top.egon</groupId>
    <artifactId>egon-cola-components-bom</artifactId>
    <version>${egon-cola.version}</version>
    <type>pom</type>
    <scope>import</scope>
</dependency>
```

Inside `<dependencies>`, add:

```xml
<dependency>
    <groupId>top.egon</groupId>
    <artifactId>egon-cola-component-common</artifactId>
</dependency>
```

- [ ] **Step 4: Add common dependency to web and service archetype root POMs**

In both root archetype POM templates:

```text
egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/pom.xml
egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/pom.xml
```

Add this property:

```xml
<egon-cola.version>5.2.0-SNAPSHOT</egon-cola.version>
```

Inside `<dependencyManagement><dependencies>`, add:

```xml
<dependency>
    <groupId>top.egon</groupId>
    <artifactId>egon-cola-components-bom</artifactId>
    <version>${egon-cola.version}</version>
    <type>pom</type>
    <scope>import</scope>
</dependency>
<dependency>
    <groupId>top.egon</groupId>
    <artifactId>egon-cola-component-common</artifactId>
    <version>${egon-cola.version}</version>
</dependency>
```

In both generated common module POM templates:

```text
egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-common/pom.xml
egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-common/pom.xml
```

Add:

```xml
<dependencies>
    <dependency>
        <groupId>top.egon</groupId>
        <artifactId>egon-cola-component-common</artifactId>
    </dependency>
</dependencies>
```

- [ ] **Step 5: Refresh component documentation**

In `README.md`, replace the old component table with:

```markdown
| 组件 | 说明 |
|---|---|
| `egon-cola-component-common` | 纯 Jar 基础组件，提供通用响应、分页、异常、断言等稳定能力。 |
| `egon-cola-component-dynamic-thread-pool-starter` | 业务系统按需引入的动态线程池 starter。 |
| `egon-cola-component-dynamic-thread-pool-admin` | 独立部署的动态线程池管理服务，不进入 BOM。 |
| `egon-cola-components-bom` | 只导出业务系统可直接依赖的 common 与 starter 版本。 |
```

In the BOM usage example, use:

```xml
<dependency>
    <groupId>top.egon</groupId>
    <artifactId>egon-cola-components-bom</artifactId>
    <version>5.2.0-SNAPSHOT</version>
    <type>pom</type>
    <scope>import</scope>
</dependency>
```

Use `egon-cola-component-common` as the default dependency example. Add `egon-cola-component-dynamic-thread-pool-starter` as an optional runtime component example.

- [ ] **Step 6: Create dynamic thread pool README**

Create `egon-cola-components/egon-cola-component-dynamic-thread-pool/README.md`:

```markdown
# Egon COLA Dynamic Thread Pool Component

This component provides Spring Boot starter based executor governance and an independently deployable admin service.

## Modules

| Module | Purpose |
|---|---|
| `egon-cola-component-dynamic-thread-pool-starter` | Business application integration, executor registration, snapshots, Redis change listening, MDC propagation, audit events, and Micrometer metrics. |
| `egon-cola-component-dynamic-thread-pool-admin` | REST management API, Redis-backed queries and change publishing, manifest endpoint, and Docker packaging. |
| `egon-cola-component-dynamic-thread-pool-test` | Component sample and validation module. |

## Configuration Prefix

```yaml
egon:
  cola:
    component:
      dtp:
        enabled: true
        app-name: ${spring.application.name}
        instance-id: ${spring.application.name}-${server.port}
        registry:
          type: redis
          redis:
            host: 127.0.0.1
            port: 6379
            password:
            database: 0
        report:
          enabled: true
          interval: 20s
        trace:
          enabled: true
          mdc-enabled: true
          trace-id-key: traceId
          request-id-key: requestId
        virtual:
          enabled: true
          default-concurrency-limit: 500
```

## Admin API

The admin API base path is `/api/v1/dtp`.

## Manifest

The admin exposes `GET /api/v1/dtp/manifest` for dynamic frontend discovery.

## UI Boundary

UI code is not stored in `egon-cola-components`.
```

- [ ] **Step 7: Create manifest protocol doc**

Create `egon-cola-components/egon-cola-component-dynamic-thread-pool/docs/manifest.md`:

```markdown
# Dynamic Thread Pool Manifest

The dynamic thread pool admin exposes a manifest for management UI discovery.

Endpoint:

```text
GET /api/v1/dtp/manifest
```

Required fields:

| Field | Meaning |
|---|---|
| `component` | Stable component key, `dynamic-thread-pool`. |
| `name` | Human-readable component name. |
| `version` | Component version. |
| `enabled` | Whether the admin exposes this component as enabled. |
| `baseApi` | Admin API base path. |
| `frontend.module` | Frontend module key. |
| `frontend.routeBase` | Frontend route base. |
| `frontend.menus` | Menu definitions. |
| `permissions` | Permission definitions. |
```

- [ ] **Step 8: Run documentation stale-reference checks**

Run:

```bash
rg -n "egon-cola-component-dto|egon-cola-component-exception|egon-cola-component-statemachine|egon-cola-component-ruleengine|egon-cola-component-job|egon-cola-dev-util-archetypes|atluofu-dynamic-thread-pool-ui" \
  README.md egon-cola-components egon-cola-archetypes
```

Expected: no output except historical docs under `docs/superpowers` if included by the search. Do not edit historical plans/specs under `docs/superpowers`.

- [ ] **Step 9: Run archetype validation**

Install components first so generated archetypes can resolve `egon-cola-components-bom`:

```bash
bash ./mvnw -B -ntp -f egon-cola-components/pom.xml -DskipTests install
```

Then run:

```bash
bash ./mvnw -B -ntp -pl egon-cola-archetypes/egon-cola-archetype-light -am clean integration-test
bash ./mvnw -B -ntp -pl egon-cola-archetypes/egon-cola-archetype-web -am clean integration-test
bash ./mvnw -B -ntp -pl egon-cola-archetypes/egon-cola-archetype-service -am clean integration-test
```

Expected: PASS.

- [ ] **Step 10: Commit Task 5**

```bash
git status --short
git add -- README.md egon-cola-components egon-cola-archetypes
git diff --cached --stat
git commit -m "docs(components): align docs and archetypes with common baseline"
```

## Task 6: Full Build, Docker Build, and Final Cleanup

**Files:**
- Modify only files needed to fix validation failures from Tasks 1-5.

**Interfaces:**
- Confirms the entire components migration builds.
- Confirms admin Docker image can be built from the packaged jar.

- [ ] **Step 1: Run full components test**

Run:

```bash
bash ./mvnw -B -ntp -f egon-cola-components/pom.xml test
```

Expected: PASS.

- [ ] **Step 2: Run full components package**

Run:

```bash
bash ./mvnw -B -ntp -f egon-cola-components/pom.xml clean package
```

Expected: PASS.

- [ ] **Step 3: Build admin Docker image**

Run:

```bash
docker build \
  -f egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-admin/Dockerfile \
  -t egon-cola-dtp-admin:local \
  egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-admin
```

Expected: PASS. If Docker is unavailable in the environment, record the exact Docker error in the final implementation summary and keep the successful Maven package output as jar proof.

- [ ] **Step 4: Run repository stale-reference checks**

Run:

```bash
rg -n "top\\.atluofu|atluofu\\.dynamic\\.thread-pool|atluofu-dynamic-thread-pool|atluofu-dynamic-thread-pool-ui|dynamic.thread.pool.config|egon-cola-component-dto|egon-cola-component-exception|egon-cola-component-statemachine|egon-cola-component-ruleengine|egon-cola-component-job|egon-cola-dev-util-archetypes" \
  README.md egon-cola-components egon-cola-archetypes
```

Expected: no output except intentional mentions in migration docs under `egon-cola-components/egon-cola-component-dynamic-thread-pool/docs` if those docs explicitly compare old and new names. Prefer removing old names from active docs.

- [ ] **Step 5: Verify BOM does not export admin or test**

Run:

```bash
rg -n "egon-cola-component-dynamic-thread-pool-admin|egon-cola-component-dynamic-thread-pool-test|egon-cola-component-dynamic-thread-pool</artifactId>" \
  egon-cola-components/egon-cola-components-bom/pom.xml
```

Expected: no output.

Run:

```bash
rg -n "egon-cola-component-common|egon-cola-component-dynamic-thread-pool-starter" \
  egon-cola-components/egon-cola-components-bom/pom.xml
```

Expected: two matching BOM dependency entries.

- [ ] **Step 6: Inspect final changed file scope**

Run:

```bash
git status --short
git diff --stat HEAD
```

Expected: changes are limited to `pom.xml`, `README.md`, `egon-cola-components`, and archetype files needed for common dependency guidance.

- [ ] **Step 7: Commit Task 6**

If validation fixes changed files:

```bash
git add -- pom.xml README.md egon-cola-components egon-cola-archetypes
git diff --cached --stat
git commit -m "chore(components): verify dynamic thread pool migration"
```

If no files changed after validation, do not create an empty commit.

## Self-Review Checklist

Spec coverage:

- Old components removed: Task 1.
- Common pure Jar: Task 1.
- Dynamic thread pool starter copied and renamed: Task 2.
- Dynamic thread pool admin copied, manifest, Dockerfile: Task 3.
- Test module consolidated: Task 4.
- BOM contract: Tasks 1 and 6.
- Archetype default common dependency and optional starter: Task 5.
- No UI migration: Tasks 3, 4, 5, and 6 stale checks.
- Validation without starting services: Tasks 1-6.

Type consistency:

- Configuration prefix is consistently `egon.cola.component.dtp`.
- Starter package root is consistently `top.egon.cola.component.dtp`.
- Admin package root is consistently `top.egon.cola.component.dtp.admin`.
- API base path is consistently `/api/v1/dtp`.
- Manifest endpoint is consistently `/api/v1/dtp/manifest`.

Execution boundary:

- Each task has one commit.
- Redis tests are opt-in through `DTP_REDIS_TESTS=true`.
- No application server is started by any validation command.
