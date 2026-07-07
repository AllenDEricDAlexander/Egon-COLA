# Egon COLA Component Common Enterprise Restructure Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Restructure `egon-cola-component-common` from one utility-heavy Jar into a Maven aggregator with focused enterprise semantic common modules.

**Architecture:** `egon-cola-component-common` becomes a `pom` aggregator under `egon-cola-components`. Concrete Jar modules provide isolated capabilities: `core`, `model`, `trace`, `result`, `id`, `crypto`, `mask`, `structure`, and `test`. The BOM exports only concrete Jar artifacts, and ordinary utility APIs are removed rather than preserved as compatibility facades.

**Tech Stack:** Java 21, Maven multi-module, JUnit Jupiter, SLF4J MDC for trace, JDK crypto/codec APIs, optional `com.github.f4b6a3:uuid-creator` for UUIDv7.

---

## Source Design

Use this approved design as the source of truth:

- `docs/superpowers/specs/2026-07-07-egon-cola-component-common-enterprise-restructure-design.md`

## Execution Rules

- Do not start the project runtime.
- Do not open a browser or use computer UI.
- Commit once per task after the task-specific validation passes.
- Preserve existing behavior only where the approved design keeps it; this plan intentionally removes old common utility APIs.
- Do not create `common-all`, `common-starter`, `common-legacy`, or deprecated wrappers unless the user changes the design.
- Do not modify any Flyway migration files; this task does not require database changes.
- Before starting execution, check for uncommitted user changes with `git status --short` and avoid overwriting unrelated work.

## Target File Structure

Create or modify the following files during the plan:

```text
egon-cola-components/egon-cola-component-common/pom.xml
egon-cola-components/egon-cola-component-common/README.md
egon-cola-components/egon-cola-component-common/egon-cola-component-common-core/pom.xml
egon-cola-components/egon-cola-component-common/egon-cola-component-common-core/src/main/java/top/egon/cola/component/common/core/code/ErrorStatus.java
egon-cola-components/egon-cola-component-common/egon-cola-component-common-core/src/main/java/top/egon/cola/component/common/core/code/CommonStatus.java
egon-cola-components/egon-cola-component-common/egon-cola-component-common-core/src/main/java/top/egon/cola/component/common/core/exception/EgonException.java
egon-cola-components/egon-cola-component-common/egon-cola-component-common-core/src/main/java/top/egon/cola/component/common/core/exception/EgonBusinessException.java
egon-cola-components/egon-cola-component-common/egon-cola-component-common-core/src/main/java/top/egon/cola/component/common/core/exception/EgonSystemException.java
egon-cola-components/egon-cola-component-common/egon-cola-component-common-core/src/main/java/top/egon/cola/component/common/core/exception/EgonIllegalStateException.java
egon-cola-components/egon-cola-component-common/egon-cola-component-common-core/src/main/java/top/egon/cola/component/common/core/enums/CodeEnum.java
egon-cola-components/egon-cola-component-common/egon-cola-component-common-core/src/test/java/top/egon/cola/component/common/core/CoreBoundaryTest.java
egon-cola-components/egon-cola-component-common/egon-cola-component-common-core/src/test/java/top/egon/cola/component/common/core/exception/EgonExceptionTest.java
egon-cola-components/egon-cola-component-common/egon-cola-component-common-model/pom.xml
egon-cola-components/egon-cola-component-common/egon-cola-component-common-model/src/main/java/top/egon/cola/component/common/model/request/BaseRequest.java
egon-cola-components/egon-cola-component-common/egon-cola-component-common-model/src/main/java/top/egon/cola/component/common/model/request/OperatorContext.java
egon-cola-components/egon-cola-component-common/egon-cola-component-common-model/src/main/java/top/egon/cola/component/common/model/query/PageQuery.java
egon-cola-components/egon-cola-component-common/egon-cola-component-common-model/src/main/java/top/egon/cola/component/common/model/query/SortQuery.java
egon-cola-components/egon-cola-component-common/egon-cola-component-common-model/src/main/java/top/egon/cola/component/common/model/query/TimeRangeQuery.java
egon-cola-components/egon-cola-component-common/egon-cola-component-common-model/src/main/java/top/egon/cola/component/common/model/page/PageMeta.java
egon-cola-components/egon-cola-component-common/egon-cola-component-common-model/src/main/java/top/egon/cola/component/common/model/page/PageModel.java
egon-cola-components/egon-cola-component-common/egon-cola-component-common-model/src/main/java/top/egon/cola/component/common/model/page/PageSlice.java
egon-cola-components/egon-cola-component-common/egon-cola-component-common-model/src/test/java/top/egon/cola/component/common/model/query/PageQueryTest.java
egon-cola-components/egon-cola-component-common/egon-cola-component-common-model/src/test/java/top/egon/cola/component/common/model/page/PageModelTest.java
egon-cola-components/egon-cola-component-common/egon-cola-component-common-trace/pom.xml
egon-cola-components/egon-cola-component-common/egon-cola-component-common-trace/src/main/java/top/egon/cola/component/common/trace/TraceContext.java
egon-cola-components/egon-cola-component-common/egon-cola-component-common-trace/src/main/java/top/egon/cola/component/common/trace/TraceSnapshot.java
egon-cola-components/egon-cola-component-common/egon-cola-component-common-trace/src/test/java/top/egon/cola/component/common/trace/TraceContextTest.java
egon-cola-components/egon-cola-component-common/egon-cola-component-common-result/pom.xml
egon-cola-components/egon-cola-component-common/egon-cola-component-common-result/src/main/java/top/egon/cola/component/common/result/dto/ResultDto.java
egon-cola-components/egon-cola-component-common/egon-cola-component-common-result/src/main/java/top/egon/cola/component/common/result/dto/PageResultDto.java
egon-cola-components/egon-cola-component-common/egon-cola-component-common-result/src/main/java/top/egon/cola/component/common/result/dto/ErrorResultDto.java
egon-cola-components/egon-cola-component-common/egon-cola-component-common-result/src/main/java/top/egon/cola/component/common/result/model/ResultModel.java
egon-cola-components/egon-cola-component-common/egon-cola-component-common-result/src/main/java/top/egon/cola/component/common/result/model/PageResultModel.java
egon-cola-components/egon-cola-component-common/egon-cola-component-common-result/src/main/java/top/egon/cola/component/common/result/model/ErrorResultModel.java
egon-cola-components/egon-cola-component-common/egon-cola-component-common-result/src/main/java/top/egon/cola/component/common/result/factory/ResultDtos.java
egon-cola-components/egon-cola-component-common/egon-cola-component-common-result/src/main/java/top/egon/cola/component/common/result/factory/ResultModels.java
egon-cola-components/egon-cola-component-common/egon-cola-component-common-result/src/test/java/top/egon/cola/component/common/result/factory/ResultDtosTest.java
egon-cola-components/egon-cola-component-common/egon-cola-component-common-result/src/test/java/top/egon/cola/component/common/result/factory/ResultModelsTest.java
egon-cola-components/egon-cola-component-common/egon-cola-component-common-id/pom.xml
egon-cola-components/egon-cola-component-common/egon-cola-component-common-id/src/main/java/top/egon/cola/component/common/id/generator/IdGenerator.java
egon-cola-components/egon-cola-component-common/egon-cola-component-common-id/src/main/java/top/egon/cola/component/common/id/generator/UuidV7Generator.java
egon-cola-components/egon-cola-component-common/egon-cola-component-common-id/src/main/java/top/egon/cola/component/common/id/uuid/UuidV7.java
egon-cola-components/egon-cola-component-common/egon-cola-component-common-id/src/test/java/top/egon/cola/component/common/id/uuid/UuidV7Test.java
egon-cola-components/egon-cola-component-common/egon-cola-component-common-crypto/pom.xml
egon-cola-components/egon-cola-component-common/egon-cola-component-common-crypto/src/main/java/top/egon/cola/component/common/crypto/digest/Digests.java
egon-cola-components/egon-cola-component-common/egon-cola-component-common-crypto/src/main/java/top/egon/cola/component/common/crypto/hmac/Hmacs.java
egon-cola-components/egon-cola-component-common/egon-cola-component-common-crypto/src/main/java/top/egon/cola/component/common/crypto/codec/Base64s.java
egon-cola-components/egon-cola-component-common/egon-cola-component-common-crypto/src/main/java/top/egon/cola/component/common/crypto/codec/Hexes.java
egon-cola-components/egon-cola-component-common/egon-cola-component-common-crypto/src/test/java/top/egon/cola/component/common/crypto/CryptoTest.java
egon-cola-components/egon-cola-component-common/egon-cola-component-common-mask/pom.xml
egon-cola-components/egon-cola-component-common/egon-cola-component-common-mask/src/main/java/top/egon/cola/component/common/mask/MaskRule.java
egon-cola-components/egon-cola-component-common/egon-cola-component-common-mask/src/main/java/top/egon/cola/component/common/mask/Masking.java
egon-cola-components/egon-cola-component-common/egon-cola-component-common-mask/src/test/java/top/egon/cola/component/common/mask/MaskingTest.java
egon-cola-components/egon-cola-component-common/egon-cola-component-common-structure/pom.xml
egon-cola-components/egon-cola-component-common/egon-cola-component-common-structure/src/main/java/top/egon/cola/component/common/structure/tree/TreeNode.java
egon-cola-components/egon-cola-component-common/egon-cola-component-common-structure/src/main/java/top/egon/cola/component/common/structure/tree/TreeOptions.java
egon-cola-components/egon-cola-component-common/egon-cola-component-common-structure/src/main/java/top/egon/cola/component/common/structure/tree/TreeBuilder.java
egon-cola-components/egon-cola-component-common/egon-cola-component-common-structure/src/test/java/top/egon/cola/component/common/structure/tree/TreeBuilderTest.java
egon-cola-components/egon-cola-component-common/egon-cola-component-common-test/pom.xml
egon-cola-components/egon-cola-component-common/egon-cola-component-common-test/src/main/java/top/egon/cola/component/common/test/SourceBoundaryAssert.java
egon-cola-components/egon-cola-component-common/egon-cola-component-common-test/src/test/java/top/egon/cola/component/common/test/SourceBoundaryAssertTest.java
egon-cola-components/egon-cola-components-bom/pom.xml
egon-cola-components/egon-cola-components-architecture.md
```

Delete these old source roots after migration:

```text
egon-cola-components/egon-cola-component-common/src/main/java
egon-cola-components/egon-cola-component-common/src/test/java
```

## Design Pattern Decisions

- Use **Factory Method** through `ResultDtos` and `ResultModels`; result defaults and trace injection should be centralized.
- Use **Strategy** through `IdGenerator`; UUIDv7 is the default implementation and future ID strategies can replace it without changing callers.
- Use a small **Builder / Options** style for tree building through `TreeBuilder` and `TreeOptions`; tree building has real variation points such as root handling and ordering.
- Do not introduce Strategy for masking yet; direct `Masking` methods are clearer until configurable masking rules become necessary.
- Do not introduce Template Method for exceptions; simple immutable exception fields and constructors are enough.

---

### Task 1: Convert common into Maven aggregator skeleton

**Files:**
- Modify: `egon-cola-components/egon-cola-component-common/pom.xml`
- Create: `egon-cola-components/egon-cola-component-common/egon-cola-component-common-core/pom.xml`
- Create: `egon-cola-components/egon-cola-component-common/egon-cola-component-common-model/pom.xml`
- Create: `egon-cola-components/egon-cola-component-common/egon-cola-component-common-trace/pom.xml`
- Create: `egon-cola-components/egon-cola-component-common/egon-cola-component-common-result/pom.xml`
- Create: `egon-cola-components/egon-cola-component-common/egon-cola-component-common-id/pom.xml`
- Create: `egon-cola-components/egon-cola-component-common/egon-cola-component-common-crypto/pom.xml`
- Create: `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mask/pom.xml`
- Create: `egon-cola-components/egon-cola-component-common/egon-cola-component-common-structure/pom.xml`
- Create: `egon-cola-components/egon-cola-component-common/egon-cola-component-common-test/pom.xml`

- [ ] **Step 1: Check workspace cleanliness**

Run:

```bash
git status --short
```

Expected: only the approved design and this plan may be untracked or modified. Stop and ask the user before touching unrelated changed files.

- [ ] **Step 2: Replace common root POM with aggregator POM**

Write `egon-cola-components/egon-cola-component-common/pom.xml` with:

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
    <packaging>pom</packaging>
    <name>egon-cola-component-common</name>
    <description>Enterprise common component aggregator for Egon COLA.</description>

    <modules>
        <module>egon-cola-component-common-core</module>
        <module>egon-cola-component-common-model</module>
        <module>egon-cola-component-common-trace</module>
        <module>egon-cola-component-common-result</module>
        <module>egon-cola-component-common-id</module>
        <module>egon-cola-component-common-crypto</module>
        <module>egon-cola-component-common-mask</module>
        <module>egon-cola-component-common-structure</module>
        <module>egon-cola-component-common-test</module>
    </modules>
</project>
```

- [ ] **Step 3: Create core module POM**

Write `egon-cola-components/egon-cola-component-common/egon-cola-component-common-core/pom.xml` with:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>top.egon</groupId>
        <artifactId>egon-cola-component-common</artifactId>
        <version>5.2.0-SNAPSHOT</version>
        <relativePath>../pom.xml</relativePath>
    </parent>

    <artifactId>egon-cola-component-common-core</artifactId>
    <packaging>jar</packaging>
    <name>egon-cola-component-common-core</name>
    <description>Core error status, exception, enum, and marker contracts for Egon COLA.</description>
</project>
```

- [ ] **Step 4: Create model module POM**

Write `egon-cola-components/egon-cola-component-common/egon-cola-component-common-model/pom.xml` with:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>top.egon</groupId>
        <artifactId>egon-cola-component-common</artifactId>
        <version>5.2.0-SNAPSHOT</version>
        <relativePath>../pom.xml</relativePath>
    </parent>

    <artifactId>egon-cola-component-common-model</artifactId>
    <packaging>jar</packaging>
    <name>egon-cola-component-common-model</name>
    <description>Common request, query, and pagination models for Egon COLA.</description>

    <dependencies>
        <dependency>
            <groupId>top.egon</groupId>
            <artifactId>egon-cola-component-common-core</artifactId>
            <version>${project.version}</version>
        </dependency>
    </dependencies>
</project>
```

- [ ] **Step 5: Create trace module POM**

Write `egon-cola-components/egon-cola-component-common/egon-cola-component-common-trace/pom.xml` with:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>top.egon</groupId>
        <artifactId>egon-cola-component-common</artifactId>
        <version>5.2.0-SNAPSHOT</version>
        <relativePath>../pom.xml</relativePath>
    </parent>

    <artifactId>egon-cola-component-common-trace</artifactId>
    <packaging>jar</packaging>
    <name>egon-cola-component-common-trace</name>
    <description>Trace context contracts for Egon COLA.</description>

    <dependencies>
        <dependency>
            <groupId>org.slf4j</groupId>
            <artifactId>slf4j-api</artifactId>
        </dependency>
        <dependency>
            <groupId>ch.qos.logback</groupId>
            <artifactId>logback-classic</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>
</project>
```

- [ ] **Step 6: Create result module POM**

Write `egon-cola-components/egon-cola-component-common/egon-cola-component-common-result/pom.xml` with:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>top.egon</groupId>
        <artifactId>egon-cola-component-common</artifactId>
        <version>5.2.0-SNAPSHOT</version>
        <relativePath>../pom.xml</relativePath>
    </parent>

    <artifactId>egon-cola-component-common-result</artifactId>
    <packaging>jar</packaging>
    <name>egon-cola-component-common-result</name>
    <description>External DTO and internal result models for Egon COLA.</description>

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
    </dependencies>
</project>
```

- [ ] **Step 7: Create id module POM**

Write `egon-cola-components/egon-cola-component-common/egon-cola-component-common-id/pom.xml` with:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>top.egon</groupId>
        <artifactId>egon-cola-component-common</artifactId>
        <version>5.2.0-SNAPSHOT</version>
        <relativePath>../pom.xml</relativePath>
    </parent>

    <artifactId>egon-cola-component-common-id</artifactId>
    <packaging>jar</packaging>
    <name>egon-cola-component-common-id</name>
    <description>UUIDv7 and ID generation contracts for Egon COLA.</description>

    <dependencies>
        <dependency>
            <groupId>top.egon</groupId>
            <artifactId>egon-cola-component-common-core</artifactId>
            <version>${project.version}</version>
        </dependency>
        <dependency>
            <groupId>com.github.f4b6a3</groupId>
            <artifactId>uuid-creator</artifactId>
            <version>6.1.1</version>
        </dependency>
    </dependencies>
</project>
```

- [ ] **Step 8: Create crypto module POM**

Write `egon-cola-components/egon-cola-component-common/egon-cola-component-common-crypto/pom.xml` with:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>top.egon</groupId>
        <artifactId>egon-cola-component-common</artifactId>
        <version>5.2.0-SNAPSHOT</version>
        <relativePath>../pom.xml</relativePath>
    </parent>

    <artifactId>egon-cola-component-common-crypto</artifactId>
    <packaging>jar</packaging>
    <name>egon-cola-component-common-crypto</name>
    <description>Digest, HMAC, and codec utilities with enterprise common semantics.</description>

    <dependencies>
        <dependency>
            <groupId>top.egon</groupId>
            <artifactId>egon-cola-component-common-core</artifactId>
            <version>${project.version}</version>
        </dependency>
    </dependencies>
</project>
```

- [ ] **Step 9: Create mask module POM**

Write `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mask/pom.xml` with:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>top.egon</groupId>
        <artifactId>egon-cola-component-common</artifactId>
        <version>5.2.0-SNAPSHOT</version>
        <relativePath>../pom.xml</relativePath>
    </parent>

    <artifactId>egon-cola-component-common-mask</artifactId>
    <packaging>jar</packaging>
    <name>egon-cola-component-common-mask</name>
    <description>Data masking rules for Egon COLA.</description>

    <dependencies>
        <dependency>
            <groupId>top.egon</groupId>
            <artifactId>egon-cola-component-common-core</artifactId>
            <version>${project.version}</version>
        </dependency>
    </dependencies>
</project>
```

- [ ] **Step 10: Create structure module POM**

Write `egon-cola-components/egon-cola-component-common/egon-cola-component-common-structure/pom.xml` with:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>top.egon</groupId>
        <artifactId>egon-cola-component-common</artifactId>
        <version>5.2.0-SNAPSHOT</version>
        <relativePath>../pom.xml</relativePath>
    </parent>

    <artifactId>egon-cola-component-common-structure</artifactId>
    <packaging>jar</packaging>
    <name>egon-cola-component-common-structure</name>
    <description>Common tree and structure helpers for Egon COLA.</description>

    <dependencies>
        <dependency>
            <groupId>top.egon</groupId>
            <artifactId>egon-cola-component-common-core</artifactId>
            <version>${project.version}</version>
        </dependency>
    </dependencies>
</project>
```

- [ ] **Step 11: Create test support module POM**

Write `egon-cola-components/egon-cola-component-common/egon-cola-component-common-test/pom.xml` with:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>top.egon</groupId>
        <artifactId>egon-cola-component-common</artifactId>
        <version>5.2.0-SNAPSHOT</version>
        <relativePath>../pom.xml</relativePath>
    </parent>

    <artifactId>egon-cola-component-common-test</artifactId>
    <packaging>jar</packaging>
    <name>egon-cola-component-common-test</name>
    <description>Boundary assertion helpers for Egon COLA common modules.</description>
</project>
```

- [ ] **Step 12: Run aggregator validation**

Run:

```bash
mvn -pl egon-cola-components/egon-cola-component-common -am test
```

Expected: PASS. Empty modules should compile and run with no tests.

- [ ] **Step 13: Commit skeleton**

Run:

```bash
git add egon-cola-components/egon-cola-component-common/pom.xml \
  egon-cola-components/egon-cola-component-common/egon-cola-component-common-core/pom.xml \
  egon-cola-components/egon-cola-component-common/egon-cola-component-common-model/pom.xml \
  egon-cola-components/egon-cola-component-common/egon-cola-component-common-trace/pom.xml \
  egon-cola-components/egon-cola-component-common/egon-cola-component-common-result/pom.xml \
  egon-cola-components/egon-cola-component-common/egon-cola-component-common-id/pom.xml \
  egon-cola-components/egon-cola-component-common/egon-cola-component-common-crypto/pom.xml \
  egon-cola-components/egon-cola-component-common/egon-cola-component-common-mask/pom.xml \
  egon-cola-components/egon-cola-component-common/egon-cola-component-common-structure/pom.xml \
  egon-cola-components/egon-cola-component-common/egon-cola-component-common-test/pom.xml
git commit -m "refactor: split common component aggregator"
```

Expected: commit succeeds.

---

### Task 2: Implement common-core contracts

**Files:**
- Create: `egon-cola-components/egon-cola-component-common/egon-cola-component-common-core/src/main/java/top/egon/cola/component/common/core/code/ErrorStatus.java`
- Create: `egon-cola-components/egon-cola-component-common/egon-cola-component-common-core/src/main/java/top/egon/cola/component/common/core/code/CommonStatus.java`
- Create: `egon-cola-components/egon-cola-component-common/egon-cola-component-common-core/src/main/java/top/egon/cola/component/common/core/exception/EgonException.java`
- Create: `egon-cola-components/egon-cola-component-common/egon-cola-component-common-core/src/main/java/top/egon/cola/component/common/core/exception/EgonBusinessException.java`
- Create: `egon-cola-components/egon-cola-component-common/egon-cola-component-common-core/src/main/java/top/egon/cola/component/common/core/exception/EgonSystemException.java`
- Create: `egon-cola-components/egon-cola-component-common/egon-cola-component-common-core/src/main/java/top/egon/cola/component/common/core/exception/EgonIllegalStateException.java`
- Create: `egon-cola-components/egon-cola-component-common/egon-cola-component-common-core/src/main/java/top/egon/cola/component/common/core/enums/CodeEnum.java`
- Create: `egon-cola-components/egon-cola-component-common/egon-cola-component-common-core/src/test/java/top/egon/cola/component/common/core/CoreBoundaryTest.java`
- Create: `egon-cola-components/egon-cola-component-common/egon-cola-component-common-core/src/test/java/top/egon/cola/component/common/core/exception/EgonExceptionTest.java`

- [ ] **Step 1: Write core exception tests first**

Write `EgonExceptionTest.java` with:

```java
package top.egon.cola.component.common.core.exception;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.common.core.code.CommonStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class EgonExceptionTest {

    @Test
    void businessExceptionCarriesStatusCodeAndMessage() {
        EgonBusinessException exception = new EgonBusinessException(CommonStatus.BAD_REQUEST);

        assertEquals(CommonStatus.BAD_REQUEST.getCode(), exception.getCode());
        assertEquals(CommonStatus.BAD_REQUEST.getStatus(), exception.getStatus());
        assertEquals(CommonStatus.BAD_REQUEST.getMessage(), exception.getMessage());
    }

    @Test
    void systemExceptionCarriesCause() {
        RuntimeException cause = new RuntimeException("boom");

        EgonSystemException exception = new EgonSystemException(CommonStatus.SYSTEM_ERROR, cause);

        assertEquals(CommonStatus.SYSTEM_ERROR.getCode(), exception.getCode());
        assertEquals(CommonStatus.SYSTEM_ERROR.getStatus(), exception.getStatus());
        assertEquals(CommonStatus.SYSTEM_ERROR.getMessage(), exception.getMessage());
        assertSame(cause, exception.getCause());
    }

    @Test
    void illegalStateExceptionSupportsCustomMessage() {
        EgonIllegalStateException exception = new EgonIllegalStateException(409001, "STATE_CONFLICT", "state conflict");

        assertEquals(409001, exception.getCode());
        assertEquals("STATE_CONFLICT", exception.getStatus());
        assertEquals("state conflict", exception.getMessage());
    }
}
```

- [ ] **Step 2: Write core boundary test first**

Write `CoreBoundaryTest.java` with:

```java
package top.egon.cola.component.common.core;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CoreBoundaryTest {

    @Test
    void coreSourceDoesNotImportRuntimeFrameworksOrOtherCommonModules() throws Exception {
        Path sourceRoot = Path.of("src/main/java/top/egon/cola/component/common/core");
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
                            || line.startsWith("import javax.servlet.")
                            || line.startsWith("import org.redisson.")
                            || line.startsWith("import redis.")
                            || line.startsWith("import com.alibaba.cola.")
                            || line.startsWith("import top.egon.cola.component.common.model.")
                            || line.startsWith("import top.egon.cola.component.common.result.")
                            || line.startsWith("import top.egon.cola.component.common.trace."))
                    .toList();
            assertEquals(List.of(), badImports);
        }
    }
}
```

- [ ] **Step 3: Run tests and verify they fail**

Run:

```bash
mvn -pl egon-cola-components/egon-cola-component-common/egon-cola-component-common-core -am test
```

Expected: FAIL because `CommonStatus`, `EgonBusinessException`, `EgonSystemException`, and `EgonIllegalStateException` do not exist yet.

- [ ] **Step 4: Implement ErrorStatus**

Write `ErrorStatus.java` with:

```java
package top.egon.cola.component.common.core.code;

/**
 * Error status contract used by common result and exception models.
 */
public interface ErrorStatus {

    int getCode();

    String getStatus();

    String getMessage();
}
```

- [ ] **Step 5: Implement CommonStatus**

Write `CommonStatus.java` with:

```java
package top.egon.cola.component.common.core.code;

/**
 * Common enterprise status definitions shared by Egon COLA components.
 */
public enum CommonStatus implements ErrorStatus {

    SUCCESS(0, "SUCCESS", "success"),
    BAD_REQUEST(400000, "BAD_REQUEST", "bad request"),
    UNAUTHORIZED(401000, "UNAUTHORIZED", "unauthorized"),
    FORBIDDEN(403000, "FORBIDDEN", "forbidden"),
    NOT_FOUND(404000, "NOT_FOUND", "not found"),
    BUSINESS_ERROR(500001, "BUSINESS_ERROR", "business error"),
    SYSTEM_ERROR(500000, "SYSTEM_ERROR", "system error");

    private final int code;

    private final String status;

    private final String message;

    CommonStatus(int code, String status, String message) {
        this.code = code;
        this.status = status;
        this.message = message;
    }

    @Override
    public int getCode() {
        return code;
    }

    @Override
    public String getStatus() {
        return status;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
```

- [ ] **Step 6: Implement EgonException**

Write `EgonException.java` with:

```java
package top.egon.cola.component.common.core.exception;

import top.egon.cola.component.common.core.code.ErrorStatus;

import java.io.Serial;

/**
 * Base runtime exception carrying stable enterprise error status fields.
 */
public class EgonException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final int code;

    private final String status;

    public EgonException(ErrorStatus errorStatus) {
        this(errorStatus.getCode(), errorStatus.getStatus(), errorStatus.getMessage(), null);
    }

    public EgonException(ErrorStatus errorStatus, Throwable cause) {
        this(errorStatus.getCode(), errorStatus.getStatus(), errorStatus.getMessage(), cause);
    }

    public EgonException(int code, String status, String message) {
        this(code, status, message, null);
    }

    public EgonException(int code, String status, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.status = status;
    }

    public int getCode() {
        return code;
    }

    public String getStatus() {
        return status;
    }
}
```

- [ ] **Step 7: Implement concrete exception classes**

Write `EgonBusinessException.java` with:

```java
package top.egon.cola.component.common.core.exception;

import top.egon.cola.component.common.core.code.ErrorStatus;

import java.io.Serial;

/**
 * Exception for expected business rule failures.
 */
public class EgonBusinessException extends EgonException {

    @Serial
    private static final long serialVersionUID = 1L;

    public EgonBusinessException(ErrorStatus errorStatus) {
        super(errorStatus);
    }

    public EgonBusinessException(ErrorStatus errorStatus, Throwable cause) {
        super(errorStatus, cause);
    }

    public EgonBusinessException(int code, String status, String message) {
        super(code, status, message);
    }
}
```

Write `EgonSystemException.java` with:

```java
package top.egon.cola.component.common.core.exception;

import top.egon.cola.component.common.core.code.ErrorStatus;

import java.io.Serial;

/**
 * Exception for unexpected system failures.
 */
public class EgonSystemException extends EgonException {

    @Serial
    private static final long serialVersionUID = 1L;

    public EgonSystemException(ErrorStatus errorStatus) {
        super(errorStatus);
    }

    public EgonSystemException(ErrorStatus errorStatus, Throwable cause) {
        super(errorStatus, cause);
    }

    public EgonSystemException(int code, String status, String message) {
        super(code, status, message);
    }

    public EgonSystemException(int code, String status, String message, Throwable cause) {
        super(code, status, message, cause);
    }
}
```

Write `EgonIllegalStateException.java` with:

```java
package top.egon.cola.component.common.core.exception;

import top.egon.cola.component.common.core.code.ErrorStatus;

import java.io.Serial;

/**
 * Exception for invalid component or domain state transitions.
 */
public class EgonIllegalStateException extends EgonException {

    @Serial
    private static final long serialVersionUID = 1L;

    public EgonIllegalStateException(ErrorStatus errorStatus) {
        super(errorStatus);
    }

    public EgonIllegalStateException(ErrorStatus errorStatus, Throwable cause) {
        super(errorStatus, cause);
    }

    public EgonIllegalStateException(int code, String status, String message) {
        super(code, status, message);
    }
}
```

- [ ] **Step 8: Implement CodeEnum**

Write `CodeEnum.java` with:

```java
package top.egon.cola.component.common.core.enums;

/**
 * Base contract for enums that expose a stable code value.
 */
public interface CodeEnum<C> {

    C getCode();
}
```

- [ ] **Step 9: Run core validation**

Run:

```bash
mvn -pl egon-cola-components/egon-cola-component-common/egon-cola-component-common-core -am test
```

Expected: PASS.

- [ ] **Step 10: Commit core contracts**

Run:

```bash
git add egon-cola-components/egon-cola-component-common/egon-cola-component-common-core
git commit -m "feat: add common core contracts"
```

Expected: commit succeeds.

---

### Task 3: Implement model module

**Files:**
- Create: `egon-cola-components/egon-cola-component-common/egon-cola-component-common-model/src/main/java/top/egon/cola/component/common/model/request/BaseRequest.java`
- Create: `egon-cola-components/egon-cola-component-common/egon-cola-component-common-model/src/main/java/top/egon/cola/component/common/model/request/OperatorContext.java`
- Create: `egon-cola-components/egon-cola-component-common/egon-cola-component-common-model/src/main/java/top/egon/cola/component/common/model/query/PageQuery.java`
- Create: `egon-cola-components/egon-cola-component-common/egon-cola-component-common-model/src/main/java/top/egon/cola/component/common/model/query/SortQuery.java`
- Create: `egon-cola-components/egon-cola-component-common/egon-cola-component-common-model/src/main/java/top/egon/cola/component/common/model/query/TimeRangeQuery.java`
- Create: `egon-cola-components/egon-cola-component-common/egon-cola-component-common-model/src/main/java/top/egon/cola/component/common/model/page/PageMeta.java`
- Create: `egon-cola-components/egon-cola-component-common/egon-cola-component-common-model/src/main/java/top/egon/cola/component/common/model/page/PageModel.java`
- Create: `egon-cola-components/egon-cola-component-common/egon-cola-component-common-model/src/main/java/top/egon/cola/component/common/model/page/PageSlice.java`
- Create: `egon-cola-components/egon-cola-component-common/egon-cola-component-common-model/src/test/java/top/egon/cola/component/common/model/query/PageQueryTest.java`
- Create: `egon-cola-components/egon-cola-component-common/egon-cola-component-common-model/src/test/java/top/egon/cola/component/common/model/page/PageModelTest.java`

- [ ] **Step 1: Write PageQuery tests first**

Write `PageQueryTest.java` with:

```java
package top.egon.cola.component.common.model.query;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PageQueryTest {

    @Test
    void pageNoAndPageSizeAreNormalized() {
        PageQuery query = new PageQuery();
        query.setPageNo(0);
        query.setPageSize(0);

        assertEquals(1, query.getPageNo());
        assertEquals(10, query.getPageSize());
        assertEquals(0, query.offset());
    }

    @Test
    void offsetUsesNormalizedValues() {
        PageQuery query = new PageQuery();
        query.setPageNo(3);
        query.setPageSize(20);

        assertEquals(40, query.offset());
    }
}
```

- [ ] **Step 2: Write PageModel tests first**

Write `PageModelTest.java` with:

```java
package top.egon.cola.component.common.model.page;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PageModelTest {

    @Test
    void pageModelCalculatesMetadata() {
        PageModel<String> page = PageModel.of(List.of("a", "b"), 21, 2, 10);

        assertEquals(List.of("a", "b"), page.getRecords());
        assertEquals(21, page.getTotal());
        assertEquals(2, page.getPageNo());
        assertEquals(10, page.getPageSize());
        assertEquals(3, page.getPages());
        assertTrue(page.isHasPrevious());
        assertTrue(page.isHasNext());
    }

    @Test
    void emptyPageModelUsesSafeDefaults() {
        PageModel<String> page = PageModel.of(null, -1, -1, 0);

        assertEquals(List.of(), page.getRecords());
        assertEquals(0, page.getTotal());
        assertEquals(1, page.getPageNo());
        assertEquals(10, page.getPageSize());
        assertEquals(0, page.getPages());
        assertFalse(page.isHasPrevious());
        assertFalse(page.isHasNext());
    }
}
```

- [ ] **Step 3: Run model tests and verify they fail**

Run:

```bash
mvn -pl egon-cola-components/egon-cola-component-common/egon-cola-component-common-model -am test
```

Expected: FAIL because model classes do not exist yet.

- [ ] **Step 4: Implement BaseRequest and OperatorContext**

Write `BaseRequest.java` with:

```java
package top.egon.cola.component.common.model.request;

import java.io.Serial;
import java.io.Serializable;

/**
 * Base request carrying optional operator context.
 */
public class BaseRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private OperatorContext operator;

    public OperatorContext getOperator() {
        return operator;
    }

    public void setOperator(OperatorContext operator) {
        this.operator = operator;
    }
}
```

Write `OperatorContext.java` with:

```java
package top.egon.cola.component.common.model.request;

import java.io.Serial;
import java.io.Serializable;

/**
 * Operator identity carried by application requests.
 */
public class OperatorContext implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String operatorId;

    private String operatorName;

    private String tenantId;

    public String getOperatorId() {
        return operatorId;
    }

    public void setOperatorId(String operatorId) {
        this.operatorId = operatorId;
    }

    public String getOperatorName() {
        return operatorName;
    }

    public void setOperatorName(String operatorName) {
        this.operatorName = operatorName;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }
}
```

- [ ] **Step 5: Implement query models**

Write `PageQuery.java` with:

```java
package top.egon.cola.component.common.model.query;

import java.io.Serial;
import java.io.Serializable;

/**
 * Common page query with safe page number and page size normalization.
 */
public class PageQuery implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private int pageNo = 1;

    private int pageSize = 10;

    public int getPageNo() {
        return pageNo;
    }

    public void setPageNo(int pageNo) {
        this.pageNo = Math.max(pageNo, 1);
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize <= 0 ? 10 : pageSize;
    }

    public int offset() {
        return (pageNo - 1) * pageSize;
    }
}
```

Write `SortQuery.java` with:

```java
package top.egon.cola.component.common.model.query;

import java.io.Serial;
import java.io.Serializable;

/**
 * Common sorting query fragment.
 */
public class SortQuery implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String sortBy;

    private String sortDirection;

    public String getSortBy() {
        return sortBy;
    }

    public void setSortBy(String sortBy) {
        this.sortBy = sortBy;
    }

    public String getSortDirection() {
        return sortDirection;
    }

    public void setSortDirection(String sortDirection) {
        this.sortDirection = sortDirection;
    }
}
```

Write `TimeRangeQuery.java` with:

```java
package top.egon.cola.component.common.model.query;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Common time range query fragment.
 */
public class TimeRangeQuery implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }
}
```

- [ ] **Step 6: Implement page models**

Write `PageMeta.java` with:

```java
package top.egon.cola.component.common.model.page;

import java.io.Serial;
import java.io.Serializable;

/**
 * Pagination metadata shared by page result models.
 */
public class PageMeta implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private long total;

    private int pageNo;

    private int pageSize;

    private long pages;

    private boolean hasNext;

    private boolean hasPrevious;

    public static PageMeta of(long total, int pageNo, int pageSize) {
        int normalizedPageNo = Math.max(pageNo, 1);
        int normalizedPageSize = pageSize <= 0 ? 10 : pageSize;
        long normalizedTotal = Math.max(total, 0);
        long totalPages = normalizedTotal == 0 ? 0 : (normalizedTotal + normalizedPageSize - 1) / normalizedPageSize;

        PageMeta meta = new PageMeta();
        meta.total = normalizedTotal;
        meta.pageNo = normalizedPageNo;
        meta.pageSize = normalizedPageSize;
        meta.pages = totalPages;
        meta.hasPrevious = normalizedPageNo > 1 && totalPages > 0;
        meta.hasNext = totalPages > normalizedPageNo;
        return meta;
    }

    public long getTotal() {
        return total;
    }

    public int getPageNo() {
        return pageNo;
    }

    public int getPageSize() {
        return pageSize;
    }

    public long getPages() {
        return pages;
    }

    public boolean isHasNext() {
        return hasNext;
    }

    public boolean isHasPrevious() {
        return hasPrevious;
    }
}
```

Write `PageModel.java` with:

```java
package top.egon.cola.component.common.model.page;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Common page model for internal application data.
 */
public class PageModel<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private List<T> records = Collections.emptyList();

    private PageMeta meta;

    public static <T> PageModel<T> of(List<T> records, long total, int pageNo, int pageSize) {
        PageModel<T> page = new PageModel<>();
        page.records = records == null ? Collections.emptyList() : new ArrayList<>(records);
        page.meta = PageMeta.of(total, pageNo, pageSize);
        return page;
    }

    public List<T> getRecords() {
        return records;
    }

    public PageMeta getMeta() {
        return meta;
    }

    public long getTotal() {
        return meta.getTotal();
    }

    public int getPageNo() {
        return meta.getPageNo();
    }

    public int getPageSize() {
        return meta.getPageSize();
    }

    public long getPages() {
        return meta.getPages();
    }

    public boolean isHasNext() {
        return meta.isHasNext();
    }

    public boolean isHasPrevious() {
        return meta.isHasPrevious();
    }
}
```

Write `PageSlice.java` with:

```java
package top.egon.cola.component.common.model.page;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Slice model for pagination scenarios where total count is not available.
 */
public class PageSlice<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private List<T> records = Collections.emptyList();

    private boolean hasNext;

    public static <T> PageSlice<T> of(List<T> records, boolean hasNext) {
        PageSlice<T> slice = new PageSlice<>();
        slice.records = records == null ? Collections.emptyList() : new ArrayList<>(records);
        slice.hasNext = hasNext;
        return slice;
    }

    public List<T> getRecords() {
        return records;
    }

    public boolean isHasNext() {
        return hasNext;
    }
}
```

- [ ] **Step 7: Run model validation**

Run:

```bash
mvn -pl egon-cola-components/egon-cola-component-common/egon-cola-component-common-model -am test
```

Expected: PASS.

- [ ] **Step 8: Commit model module**

Run:

```bash
git add egon-cola-components/egon-cola-component-common/egon-cola-component-common-model
git commit -m "feat: add common model module"
```

Expected: commit succeeds.

---

### Task 4: Implement trace and result modules

**Files:**
- Create: `egon-cola-components/egon-cola-component-common/egon-cola-component-common-trace/src/main/java/top/egon/cola/component/common/trace/TraceContext.java`
- Create: `egon-cola-components/egon-cola-component-common/egon-cola-component-common-trace/src/main/java/top/egon/cola/component/common/trace/TraceSnapshot.java`
- Create: `egon-cola-components/egon-cola-component-common/egon-cola-component-common-trace/src/test/java/top/egon/cola/component/common/trace/TraceContextTest.java`
- Create: `egon-cola-components/egon-cola-component-common/egon-cola-component-common-result/src/main/java/top/egon/cola/component/common/result/dto/ResultDto.java`
- Create: `egon-cola-components/egon-cola-component-common/egon-cola-component-common-result/src/main/java/top/egon/cola/component/common/result/dto/PageResultDto.java`
- Create: `egon-cola-components/egon-cola-component-common/egon-cola-component-common-result/src/main/java/top/egon/cola/component/common/result/dto/ErrorResultDto.java`
- Create: `egon-cola-components/egon-cola-component-common/egon-cola-component-common-result/src/main/java/top/egon/cola/component/common/result/model/ResultModel.java`
- Create: `egon-cola-components/egon-cola-component-common/egon-cola-component-common-result/src/main/java/top/egon/cola/component/common/result/model/PageResultModel.java`
- Create: `egon-cola-components/egon-cola-component-common/egon-cola-component-common-result/src/main/java/top/egon/cola/component/common/result/model/ErrorResultModel.java`
- Create: `egon-cola-components/egon-cola-component-common/egon-cola-component-common-result/src/main/java/top/egon/cola/component/common/result/factory/ResultDtos.java`
- Create: `egon-cola-components/egon-cola-component-common/egon-cola-component-common-result/src/main/java/top/egon/cola/component/common/result/factory/ResultModels.java`
- Create: `egon-cola-components/egon-cola-component-common/egon-cola-component-common-result/src/test/java/top/egon/cola/component/common/result/factory/ResultDtosTest.java`
- Create: `egon-cola-components/egon-cola-component-common/egon-cola-component-common-result/src/test/java/top/egon/cola/component/common/result/factory/ResultModelsTest.java`

- [ ] **Step 1: Write trace tests first**

Write `TraceContextTest.java` with:

```java
package top.egon.cola.component.common.trace;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class TraceContextTest {

    @AfterEach
    void tearDown() {
        TraceContext.clearTraceId();
    }

    @Test
    void traceContextStoresTraceIdInMdc() {
        TraceContext.setTraceId("trace-1");

        assertEquals("trace-1", TraceContext.getTraceId());
    }

    @Test
    void blankTraceIdClearsContext() {
        TraceContext.setTraceId("trace-1");
        TraceContext.setTraceId(" ");

        assertNull(TraceContext.getTraceId());
    }

    @Test
    void snapshotCapturesCurrentTraceId() {
        TraceContext.setTraceId("trace-2");

        TraceSnapshot snapshot = TraceContext.snapshot();

        assertEquals("trace-2", snapshot.getTraceId());
    }
}
```

- [ ] **Step 2: Write result tests first**

Write `ResultDtosTest.java` with:

```java
package top.egon.cola.component.common.result.factory;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import top.egon.cola.component.common.core.code.CommonStatus;
import top.egon.cola.component.common.core.exception.EgonBusinessException;
import top.egon.cola.component.common.result.dto.PageResultDto;
import top.egon.cola.component.common.result.dto.ResultDto;
import top.egon.cola.component.common.trace.TraceContext;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResultDtosTest {

    @AfterEach
    void tearDown() {
        TraceContext.clearTraceId();
    }

    @Test
    void successDtoCarriesTraceIdAndTimestamp() {
        TraceContext.setTraceId("trace-1");

        ResultDto<String> result = ResultDtos.success("ok");

        assertTrue(result.isSuccess());
        assertEquals(CommonStatus.SUCCESS.getCode(), result.getCode());
        assertEquals(CommonStatus.SUCCESS.getStatus(), result.getStatus());
        assertEquals("ok", result.getData());
        assertEquals("trace-1", result.getTraceId());
        assertNotNull(result.getTimestamp());
    }

    @Test
    void failureDtoMapsEgonException() {
        ResultDto<Void> result = ResultDtos.failure(new EgonBusinessException(CommonStatus.BAD_REQUEST));

        assertFalse(result.isSuccess());
        assertEquals(CommonStatus.BAD_REQUEST.getCode(), result.getCode());
        assertEquals(CommonStatus.BAD_REQUEST.getStatus(), result.getStatus());
        assertEquals(CommonStatus.BAD_REQUEST.getMessage(), result.getMessage());
    }

    @Test
    void pageDtoCalculatesPageMetadata() {
        PageResultDto<String> result = ResultDtos.page(List.of("a"), 11, 2, 10);

        assertTrue(result.isSuccess());
        assertEquals(List.of("a"), result.getRecords());
        assertEquals(11, result.getTotal());
        assertEquals(2, result.getPageNo());
        assertEquals(10, result.getPageSize());
        assertEquals(2, result.getPages());
        assertTrue(result.isHasPrevious());
        assertFalse(result.isHasNext());
    }
}
```

Write `ResultModelsTest.java` with:

```java
package top.egon.cola.component.common.result.factory;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.common.core.code.CommonStatus;
import top.egon.cola.component.common.result.model.PageResultModel;
import top.egon.cola.component.common.result.model.ResultModel;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResultModelsTest {

    @Test
    void successModelDoesNotCarryTraceFields() {
        ResultModel<String> result = ResultModels.success("ok");

        assertTrue(result.isSuccess());
        assertEquals(CommonStatus.SUCCESS.getCode(), result.getCode());
        assertEquals(CommonStatus.SUCCESS.getStatus(), result.getStatus());
        assertEquals("ok", result.getData());
    }

    @Test
    void failureModelUsesStatus() {
        ResultModel<Void> result = ResultModels.failure(CommonStatus.BUSINESS_ERROR);

        assertFalse(result.isSuccess());
        assertEquals(CommonStatus.BUSINESS_ERROR.getCode(), result.getCode());
        assertEquals(CommonStatus.BUSINESS_ERROR.getStatus(), result.getStatus());
        assertEquals(CommonStatus.BUSINESS_ERROR.getMessage(), result.getMessage());
    }

    @Test
    void pageModelCalculatesMetadata() {
        PageResultModel<String> result = ResultModels.page(List.of("a"), 1, 1, 10);

        assertTrue(result.isSuccess());
        assertEquals(List.of("a"), result.getRecords());
        assertEquals(1, result.getTotal());
        assertEquals(1, result.getPageNo());
        assertEquals(10, result.getPageSize());
        assertEquals(1, result.getPages());
    }
}
```

- [ ] **Step 3: Run trace/result tests and verify they fail**

Run:

```bash
mvn -pl egon-cola-components/egon-cola-component-common/egon-cola-component-common-trace,egon-cola-components/egon-cola-component-common/egon-cola-component-common-result -am test
```

Expected: FAIL because trace and result classes do not exist yet.

- [ ] **Step 4: Implement trace module**

Write `TraceSnapshot.java` with:

```java
package top.egon.cola.component.common.trace;

import java.io.Serial;
import java.io.Serializable;

/**
 * Immutable snapshot of the current trace context.
 */
public class TraceSnapshot implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String traceId;

    public TraceSnapshot(String traceId) {
        this.traceId = traceId;
    }

    public String getTraceId() {
        return traceId;
    }
}
```

Write `TraceContext.java` with:

```java
package top.egon.cola.component.common.trace;

import org.slf4j.MDC;

/**
 * TraceId context helper based on the MDC key traceId.
 */
public final class TraceContext {

    public static final String TRACE_ID = "traceId";

    private TraceContext() {
    }

    public static String getTraceId() {
        return MDC.get(TRACE_ID);
    }

    public static void setTraceId(String traceId) {
        if (traceId == null || traceId.isBlank()) {
            MDC.remove(TRACE_ID);
            return;
        }
        MDC.put(TRACE_ID, traceId);
    }

    public static void clearTraceId() {
        MDC.remove(TRACE_ID);
    }

    public static TraceSnapshot snapshot() {
        return new TraceSnapshot(getTraceId());
    }
}
```

- [ ] **Step 5: Implement result DTO classes**

Write `ResultDto.java` with:

```java
package top.egon.cola.component.common.result.dto;

import java.io.Serial;
import java.io.Serializable;

/**
 * External single-object response DTO.
 */
public class ResultDto<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private boolean success;

    private int code;

    private String status;

    private String message;

    private T data;

    private String traceId;

    private Long timestamp;

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }
}
```

Write `PageResultDto.java` with:

```java
package top.egon.cola.component.common.result.dto;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * External page response DTO.
 */
public class PageResultDto<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private boolean success;

    private int code;

    private String status;

    private String message;

    private List<T> records = Collections.emptyList();

    private long total;

    private int pageNo;

    private int pageSize;

    private long pages;

    private boolean hasNext;

    private boolean hasPrevious;

    private String traceId;

    private Long timestamp;

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<T> getRecords() {
        return records;
    }

    public void setRecords(List<T> records) {
        this.records = records == null ? Collections.emptyList() : new ArrayList<>(records);
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public int getPageNo() {
        return pageNo;
    }

    public void setPageNo(int pageNo) {
        this.pageNo = pageNo;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public long getPages() {
        return pages;
    }

    public void setPages(long pages) {
        this.pages = pages;
    }

    public boolean isHasNext() {
        return hasNext;
    }

    public void setHasNext(boolean hasNext) {
        this.hasNext = hasNext;
    }

    public boolean isHasPrevious() {
        return hasPrevious;
    }

    public void setHasPrevious(boolean hasPrevious) {
        this.hasPrevious = hasPrevious;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }
}
```

Write `ErrorResultDto.java` with:

```java
package top.egon.cola.component.common.result.dto;

import java.io.Serial;
import java.io.Serializable;

/**
 * External error response DTO.
 */
public class ErrorResultDto implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private int code;

    private String status;

    private String message;

    private String traceId;

    private Long timestamp;

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }
}
```

- [ ] **Step 6: Implement result model classes**

Write `ResultModel.java` with:

```java
package top.egon.cola.component.common.result.model;

import java.io.Serial;
import java.io.Serializable;

/**
 * Internal single-object operation result model.
 */
public class ResultModel<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private boolean success;

    private int code;

    private String status;

    private String message;

    private T data;

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}
```

Write `PageResultModel.java` with:

```java
package top.egon.cola.component.common.result.model;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Internal page operation result model.
 */
public class PageResultModel<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private boolean success;

    private int code;

    private String status;

    private String message;

    private List<T> records = Collections.emptyList();

    private long total;

    private int pageNo;

    private int pageSize;

    private long pages;

    private boolean hasNext;

    private boolean hasPrevious;

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<T> getRecords() {
        return records;
    }

    public void setRecords(List<T> records) {
        this.records = records == null ? Collections.emptyList() : new ArrayList<>(records);
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public int getPageNo() {
        return pageNo;
    }

    public void setPageNo(int pageNo) {
        this.pageNo = pageNo;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public long getPages() {
        return pages;
    }

    public void setPages(long pages) {
        this.pages = pages;
    }

    public boolean isHasNext() {
        return hasNext;
    }

    public void setHasNext(boolean hasNext) {
        this.hasNext = hasNext;
    }

    public boolean isHasPrevious() {
        return hasPrevious;
    }

    public void setHasPrevious(boolean hasPrevious) {
        this.hasPrevious = hasPrevious;
    }
}
```

Write `ErrorResultModel.java` with:

```java
package top.egon.cola.component.common.result.model;

import java.io.Serial;
import java.io.Serializable;

/**
 * Internal error description model.
 */
public class ErrorResultModel implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private int code;

    private String status;

    private String message;

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
```

- [ ] **Step 7: Implement result factories**

Write `ResultDtos.java` with:

```java
package top.egon.cola.component.common.result.factory;

import top.egon.cola.component.common.core.code.CommonStatus;
import top.egon.cola.component.common.core.code.ErrorStatus;
import top.egon.cola.component.common.core.exception.EgonException;
import top.egon.cola.component.common.result.dto.PageResultDto;
import top.egon.cola.component.common.result.dto.ResultDto;
import top.egon.cola.component.common.trace.TraceContext;

import java.time.Instant;
import java.util.List;

/**
 * Factory methods for external response DTOs.
 */
public final class ResultDtos {

    private ResultDtos() {
    }

    public static <T> ResultDto<T> success() {
        return success(null);
    }

    public static <T> ResultDto<T> success(T data) {
        ResultDto<T> result = new ResultDto<>();
        fill(result, true, CommonStatus.SUCCESS);
        result.setData(data);
        return result;
    }

    public static <T> ResultDto<T> failure(ErrorStatus status) {
        ResultDto<T> result = new ResultDto<>();
        fill(result, false, status);
        return result;
    }

    public static <T> ResultDto<T> failure(Throwable throwable) {
        if (throwable instanceof EgonException exception) {
            return failure(exception.getCode(), exception.getStatus(), exception.getMessage());
        }
        String message = throwable == null ? CommonStatus.SYSTEM_ERROR.getMessage() : throwable.getMessage();
        return failure(CommonStatus.SYSTEM_ERROR.getCode(), CommonStatus.SYSTEM_ERROR.getStatus(), message);
    }

    public static <T> ResultDto<T> failure(int code, String status, String message) {
        ResultDto<T> result = new ResultDto<>();
        result.setSuccess(false);
        result.setCode(code);
        result.setStatus(status);
        result.setMessage(message);
        result.setTraceId(TraceContext.getTraceId());
        result.setTimestamp(Instant.now().toEpochMilli());
        return result;
    }

    public static <T> PageResultDto<T> page(List<T> records, long total, int pageNo, int pageSize) {
        int normalizedPageNo = Math.max(pageNo, 1);
        int normalizedPageSize = pageSize <= 0 ? 10 : pageSize;
        long normalizedTotal = Math.max(total, 0);
        long totalPages = normalizedTotal == 0 ? 0 : (normalizedTotal + normalizedPageSize - 1) / normalizedPageSize;

        PageResultDto<T> result = new PageResultDto<>();
        result.setSuccess(true);
        result.setCode(CommonStatus.SUCCESS.getCode());
        result.setStatus(CommonStatus.SUCCESS.getStatus());
        result.setMessage(CommonStatus.SUCCESS.getMessage());
        result.setRecords(records);
        result.setTotal(normalizedTotal);
        result.setPageNo(normalizedPageNo);
        result.setPageSize(normalizedPageSize);
        result.setPages(totalPages);
        result.setHasPrevious(normalizedPageNo > 1 && totalPages > 0);
        result.setHasNext(totalPages > normalizedPageNo);
        result.setTraceId(TraceContext.getTraceId());
        result.setTimestamp(Instant.now().toEpochMilli());
        return result;
    }

    private static <T> void fill(ResultDto<T> result, boolean success, ErrorStatus status) {
        result.setSuccess(success);
        result.setCode(status.getCode());
        result.setStatus(status.getStatus());
        result.setMessage(status.getMessage());
        result.setTraceId(TraceContext.getTraceId());
        result.setTimestamp(Instant.now().toEpochMilli());
    }
}
```

Write `ResultModels.java` with:

```java
package top.egon.cola.component.common.result.factory;

import top.egon.cola.component.common.core.code.CommonStatus;
import top.egon.cola.component.common.core.code.ErrorStatus;
import top.egon.cola.component.common.result.model.PageResultModel;
import top.egon.cola.component.common.result.model.ResultModel;

import java.util.List;

/**
 * Factory methods for internal result models.
 */
public final class ResultModels {

    private ResultModels() {
    }

    public static <T> ResultModel<T> success() {
        return success(null);
    }

    public static <T> ResultModel<T> success(T data) {
        ResultModel<T> result = new ResultModel<>();
        fill(result, true, CommonStatus.SUCCESS);
        result.setData(data);
        return result;
    }

    public static <T> ResultModel<T> failure(ErrorStatus status) {
        ResultModel<T> result = new ResultModel<>();
        fill(result, false, status);
        return result;
    }

    public static <T> PageResultModel<T> page(List<T> records, long total, int pageNo, int pageSize) {
        int normalizedPageNo = Math.max(pageNo, 1);
        int normalizedPageSize = pageSize <= 0 ? 10 : pageSize;
        long normalizedTotal = Math.max(total, 0);
        long totalPages = normalizedTotal == 0 ? 0 : (normalizedTotal + normalizedPageSize - 1) / normalizedPageSize;

        PageResultModel<T> result = new PageResultModel<>();
        result.setSuccess(true);
        result.setCode(CommonStatus.SUCCESS.getCode());
        result.setStatus(CommonStatus.SUCCESS.getStatus());
        result.setMessage(CommonStatus.SUCCESS.getMessage());
        result.setRecords(records);
        result.setTotal(normalizedTotal);
        result.setPageNo(normalizedPageNo);
        result.setPageSize(normalizedPageSize);
        result.setPages(totalPages);
        result.setHasPrevious(normalizedPageNo > 1 && totalPages > 0);
        result.setHasNext(totalPages > normalizedPageNo);
        return result;
    }

    private static <T> void fill(ResultModel<T> result, boolean success, ErrorStatus status) {
        result.setSuccess(success);
        result.setCode(status.getCode());
        result.setStatus(status.getStatus());
        result.setMessage(status.getMessage());
    }
}
```

- [ ] **Step 8: Run trace/result validation**

Run:

```bash
mvn -pl egon-cola-components/egon-cola-component-common/egon-cola-component-common-trace,egon-cola-components/egon-cola-component-common/egon-cola-component-common-result -am test
```

Expected: PASS.

- [ ] **Step 9: Commit trace and result modules**

Run:

```bash
git add egon-cola-components/egon-cola-component-common/egon-cola-component-common-trace \
  egon-cola-components/egon-cola-component-common/egon-cola-component-common-result
git commit -m "feat: add trace and result modules"
```

Expected: commit succeeds.

---

### Task 5: Implement id, crypto, mask, and structure modules

**Files:**
- Create: all files listed for `common-id`, `common-crypto`, `common-mask`, and `common-structure` in Target File Structure.

- [ ] **Step 1: Write id tests first**

Write `UuidV7Test.java` with:

```java
package top.egon.cola.component.common.id.uuid;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.common.id.generator.IdGenerator;
import top.egon.cola.component.common.id.generator.UuidV7Generator;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class UuidV7Test {

    @Test
    void uuidV7GeneratesVersion7Uuid() {
        UUID uuid = UuidV7.generate();

        assertEquals(7, uuid.version());
    }

    @Test
    void generatorReturnsUuidString() {
        IdGenerator generator = new UuidV7Generator();

        String first = generator.nextId();
        String second = generator.nextId();

        assertFalse(first.isBlank());
        assertNotEquals(first, second);
        assertEquals(36, first.length());
    }

    @Test
    void simpleStringRemovesHyphen() {
        String id = UuidV7.simpleString();

        assertEquals(32, id.length());
        assertFalse(id.contains("-"));
    }
}
```

- [ ] **Step 2: Write crypto tests first**

Write `CryptoTest.java` with:

```java
package top.egon.cola.component.common.crypto;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.common.crypto.codec.Base64s;
import top.egon.cola.component.common.crypto.codec.Hexes;
import top.egon.cola.component.common.crypto.digest.Digests;
import top.egon.cola.component.common.crypto.hmac.Hmacs;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CryptoTest {

    @Test
    void sha256HexUsesStableLowercaseOutput() {
        assertEquals("2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824", Digests.sha256Hex("hello"));
    }

    @Test
    void hmacSha256HexUsesStableLowercaseOutput() {
        assertEquals("9307b3b915efb5171ff14d8cb55fbcc798c6c0ef1456d66ded1a6aa723a58b7b", Hmacs.sha256Hex("hello", "key"));
    }

    @Test
    void base64RoundTripWorks() {
        String encoded = Base64s.encodeToString("hello");

        assertEquals("hello", Base64s.decodeToString(encoded));
    }

    @Test
    void hexRoundTripWorks() {
        String encoded = Hexes.encodeToString("hello");

        assertEquals("68656x6c6f".replace("x", "c"), encoded);
        assertEquals("hello", Hexes.decodeToString(encoded));
    }
}
```

- [ ] **Step 3: Write mask tests first**

Write `MaskingTest.java` with:

```java
package top.egon.cola.component.common.mask;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class MaskingTest {

    @Test
    void mobileMasksMiddleDigits() {
        assertEquals("138****8000", Masking.mobile("13812348000"));
    }

    @Test
    void emailMasksLocalName() {
        assertEquals("m***o@example.com", Masking.email("mario@example.com"));
    }

    @Test
    void keepAroundMasksByRule() {
        assertEquals("ab****gh", Masking.keepAround("abcdefgh", MaskRule.keepAround(2, 2)));
    }

    @Test
    void nullInputReturnsNull() {
        assertNull(Masking.mobile(null));
    }
}
```

- [ ] **Step 4: Write structure tests first**

Write `TreeBuilderTest.java` with:

```java
package top.egon.cola.component.common.structure.tree;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TreeBuilderTest {

    @Test
    void buildsTreeFromFlatNodes() {
        TreeNode<Long, String> root = new TreeNode<>(1L, null, "root");
        TreeNode<Long, String> child = new TreeNode<>(2L, 1L, "child");
        TreeNode<Long, String> another = new TreeNode<>(3L, null, "another");

        List<TreeNode<Long, String>> roots = TreeBuilder.build(List.of(child, root, another));

        assertEquals(List.of(root, another), roots);
        assertEquals(List.of(child), root.getChildren());
    }

    @Test
    void keepsOrphansAsRootsByDefault() {
        TreeNode<Long, String> orphan = new TreeNode<>(2L, 99L, "orphan");

        List<TreeNode<Long, String>> roots = TreeBuilder.build(List.of(orphan));

        assertEquals(List.of(orphan), roots);
    }
}
```

- [ ] **Step 5: Run tests and verify they fail**

Run:

```bash
mvn -pl egon-cola-components/egon-cola-component-common/egon-cola-component-common-id,egon-cola-components/egon-cola-component-common/egon-cola-component-common-crypto,egon-cola-components/egon-cola-component-common/egon-cola-component-common-mask,egon-cola-components/egon-cola-component-common/egon-cola-component-common-structure -am test
```

Expected: FAIL because implementation classes do not exist yet.

- [ ] **Step 6: Implement id module**

Write `IdGenerator.java` with:

```java
package top.egon.cola.component.common.id.generator;

/**
 * Strategy contract for string ID generation.
 */
public interface IdGenerator {

    String nextId();
}
```

Write `UuidV7Generator.java` with:

```java
package top.egon.cola.component.common.id.generator;

import top.egon.cola.component.common.id.uuid.UuidV7;

/**
 * UUIDv7 string ID generator.
 */
public class UuidV7Generator implements IdGenerator {

    @Override
    public String nextId() {
        return UuidV7.string();
    }
}
```

Write `UuidV7.java` with:

```java
package top.egon.cola.component.common.id.uuid;

import com.github.f4b6a3.uuid.UuidCreator;

import java.util.UUID;

/**
 * UUIDv7 generation helper.
 */
public final class UuidV7 {

    private UuidV7() {
    }

    public static UUID generate() {
        return UuidCreator.getTimeOrderedEpoch();
    }

    public static String string() {
        return generate().toString();
    }

    public static String simpleString() {
        return string().replace("-", "");
    }
}
```

- [ ] **Step 7: Implement crypto module**

Write `Digests.java` with:

```java
package top.egon.cola.component.common.crypto.digest;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Message digest helpers.
 */
public final class Digests {

    private static final String SHA_256 = "SHA-256";

    private Digests() {
    }

    public static String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance(SHA_256);
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm is unavailable", e);
        }
    }
}
```

Write `Hmacs.java` with:

```java
package top.egon.cola.component.common.crypto.hmac;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

/**
 * HMAC helpers.
 */
public final class Hmacs {

    private static final String HMAC_SHA_256 = "HmacSHA256";

    private Hmacs() {
    }

    public static String sha256Hex(String value, String key) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA_256);
            mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), HMAC_SHA_256));
            return HexFormat.of().formatHex(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("HMAC-SHA256 calculation failed", e);
        }
    }
}
```

Write `Base64s.java` with:

```java
package top.egon.cola.component.common.crypto.codec;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Base64 UTF-8 codec helpers.
 */
public final class Base64s {

    private Base64s() {
    }

    public static String encodeToString(String value) {
        return Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    public static String decodeToString(String value) {
        return new String(Base64.getDecoder().decode(value), StandardCharsets.UTF_8);
    }
}
```

Write `Hexes.java` with:

```java
package top.egon.cola.component.common.crypto.codec;

import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

/**
 * Hex UTF-8 codec helpers.
 */
public final class Hexes {

    private Hexes() {
    }

    public static String encodeToString(String value) {
        return HexFormat.of().formatHex(value.getBytes(StandardCharsets.UTF_8));
    }

    public static String decodeToString(String value) {
        return new String(HexFormat.of().parseHex(value), StandardCharsets.UTF_8);
    }
}
```

- [ ] **Step 8: Implement mask module**

Write `MaskRule.java` with:

```java
package top.egon.cola.component.common.mask;

/**
 * Rule describing how many characters to keep around masked content.
 */
public class MaskRule {

    private final int keepStart;

    private final int keepEnd;

    private final char maskChar;

    private MaskRule(int keepStart, int keepEnd, char maskChar) {
        this.keepStart = Math.max(keepStart, 0);
        this.keepEnd = Math.max(keepEnd, 0);
        this.maskChar = maskChar;
    }

    public static MaskRule keepAround(int keepStart, int keepEnd) {
        return new MaskRule(keepStart, keepEnd, '*');
    }

    public int getKeepStart() {
        return keepStart;
    }

    public int getKeepEnd() {
        return keepEnd;
    }

    public char getMaskChar() {
        return maskChar;
    }
}
```

Write `Masking.java` with:

```java
package top.egon.cola.component.common.mask;

/**
 * Common data masking helpers.
 */
public final class Masking {

    private Masking() {
    }

    public static String mobile(String value) {
        if (value == null) {
            return null;
        }
        if (value.length() < 7) {
            return keepAround(value, MaskRule.keepAround(1, 1));
        }
        return value.substring(0, 3) + "****" + value.substring(value.length() - 4);
    }

    public static String email(String value) {
        if (value == null) {
            return null;
        }
        int atIndex = value.indexOf('@');
        if (atIndex <= 0) {
            return keepAround(value, MaskRule.keepAround(1, 1));
        }
        String local = value.substring(0, atIndex);
        return keepAround(local, MaskRule.keepAround(1, 1)) + value.substring(atIndex);
    }

    public static String keepAround(String value, MaskRule rule) {
        if (value == null) {
            return null;
        }
        int keepStart = rule.getKeepStart();
        int keepEnd = rule.getKeepEnd();
        if (value.length() <= keepStart + keepEnd) {
            return value;
        }
        int maskLength = value.length() - keepStart - keepEnd;
        return value.substring(0, keepStart)
                + String.valueOf(rule.getMaskChar()).repeat(maskLength)
                + value.substring(value.length() - keepEnd);
    }
}
```

- [ ] **Step 9: Implement structure module**

Write `TreeNode.java` with:

```java
package top.egon.cola.component.common.structure.tree;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Generic mutable tree node used by TreeBuilder.
 */
public class TreeNode<ID, V> {

    private final ID id;

    private final ID parentId;

    private final V value;

    private final List<TreeNode<ID, V>> children = new ArrayList<>();

    public TreeNode(ID id, ID parentId, V value) {
        this.id = id;
        this.parentId = parentId;
        this.value = value;
    }

    public ID getId() {
        return id;
    }

    public ID getParentId() {
        return parentId;
    }

    public V getValue() {
        return value;
    }

    public List<TreeNode<ID, V>> getChildren() {
        return children;
    }

    public void addChild(TreeNode<ID, V> child) {
        children.add(child);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof TreeNode<?, ?> treeNode)) {
            return false;
        }
        return Objects.equals(id, treeNode.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
```

Write `TreeOptions.java` with:

```java
package top.egon.cola.component.common.structure.tree;

/**
 * Options for tree building.
 */
public class TreeOptions {

    private boolean keepOrphansAsRoots = true;

    public boolean isKeepOrphansAsRoots() {
        return keepOrphansAsRoots;
    }

    public void setKeepOrphansAsRoots(boolean keepOrphansAsRoots) {
        this.keepOrphansAsRoots = keepOrphansAsRoots;
    }
}
```

Write `TreeBuilder.java` with:

```java
package top.egon.cola.component.common.structure.tree;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Builder for parent-child tree structures.
 */
public final class TreeBuilder {

    private TreeBuilder() {
    }

    public static <ID, V> List<TreeNode<ID, V>> build(List<TreeNode<ID, V>> nodes) {
        return build(nodes, new TreeOptions());
    }

    public static <ID, V> List<TreeNode<ID, V>> build(List<TreeNode<ID, V>> nodes, TreeOptions options) {
        if (nodes == null || nodes.isEmpty()) {
            return List.of();
        }
        Map<ID, TreeNode<ID, V>> nodeMap = new LinkedHashMap<>();
        for (TreeNode<ID, V> node : nodes) {
            nodeMap.put(node.getId(), node);
        }

        List<TreeNode<ID, V>> roots = new ArrayList<>();
        for (TreeNode<ID, V> node : nodes) {
            ID parentId = node.getParentId();
            TreeNode<ID, V> parent = parentId == null ? null : nodeMap.get(parentId);
            if (parent == null) {
                if (parentId == null || options.isKeepOrphansAsRoots()) {
                    roots.add(node);
                }
                continue;
            }
            parent.addChild(node);
        }
        return roots;
    }
}
```

- [ ] **Step 10: Run capability module validation**

Run:

```bash
mvn -pl egon-cola-components/egon-cola-component-common/egon-cola-component-common-id,egon-cola-components/egon-cola-component-common/egon-cola-component-common-crypto,egon-cola-components/egon-cola-component-common/egon-cola-component-common-mask,egon-cola-components/egon-cola-component-common/egon-cola-component-common-structure -am test
```

Expected: PASS.

- [ ] **Step 11: Commit capability modules**

Run:

```bash
git add egon-cola-components/egon-cola-component-common/egon-cola-component-common-id \
  egon-cola-components/egon-cola-component-common/egon-cola-component-common-crypto \
  egon-cola-components/egon-cola-component-common/egon-cola-component-common-mask \
  egon-cola-components/egon-cola-component-common/egon-cola-component-common-structure
git commit -m "feat: add common enterprise capability modules"
```

Expected: commit succeeds.

---

### Task 6: Add reusable boundary test support and remove old source tree

**Files:**
- Create: `egon-cola-components/egon-cola-component-common/egon-cola-component-common-test/src/main/java/top/egon/cola/component/common/test/SourceBoundaryAssert.java`
- Create: `egon-cola-components/egon-cola-component-common/egon-cola-component-common-test/src/test/java/top/egon/cola/component/common/test/SourceBoundaryAssertTest.java`
- Delete: `egon-cola-components/egon-cola-component-common/src/main/java`
- Delete: `egon-cola-components/egon-cola-component-common/src/test/java`

- [ ] **Step 1: Write SourceBoundaryAssert tests first**

Write `SourceBoundaryAssertTest.java` with:

```java
package top.egon.cola.component.common.test;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SourceBoundaryAssertTest {

    @Test
    void acceptsSourceWithoutForbiddenImports() throws Exception {
        Path tempDir = Files.createTempDirectory("common-boundary-ok");
        Path source = tempDir.resolve("Sample.java");
        Files.writeString(source, "package sample;\nimport java.util.List;\nclass Sample {}\n");

        assertDoesNotThrow(() -> SourceBoundaryAssert.assertNoForbiddenImports(tempDir));
    }

    @Test
    void rejectsSourceWithRuntimeFrameworkImport() throws Exception {
        Path tempDir = Files.createTempDirectory("common-boundary-bad");
        Path source = tempDir.resolve("Sample.java");
        Files.writeString(source, "package sample;\nimport org.springframework.context.ApplicationContext;\nclass Sample {}\n");

        assertThrows(AssertionError.class, () -> SourceBoundaryAssert.assertNoForbiddenImports(tempDir));
    }
}
```

- [ ] **Step 2: Run test support tests and verify they fail**

Run:

```bash
mvn -pl egon-cola-components/egon-cola-component-common/egon-cola-component-common-test -am test
```

Expected: FAIL because `SourceBoundaryAssert` does not exist yet.

- [ ] **Step 3: Implement SourceBoundaryAssert**

Write `SourceBoundaryAssert.java` with:

```java
package top.egon.cola.component.common.test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

/**
 * Source-level boundary assertions for common modules.
 */
public final class SourceBoundaryAssert {

    private static final List<String> FORBIDDEN_IMPORT_PREFIXES = List.of(
            "import org.springframework.",
            "import jakarta.",
            "import javax.servlet.",
            "import org.redisson.",
            "import redis.",
            "import com.alibaba.cola."
    );

    private SourceBoundaryAssert() {
    }

    public static void assertNoForbiddenImports(Path sourceRoot) {
        if (!Files.exists(sourceRoot)) {
            return;
        }
        List<String> badImports;
        try (Stream<Path> files = Files.walk(sourceRoot)) {
            badImports = files
                    .filter(path -> path.toString().endsWith(".java"))
                    .flatMap(SourceBoundaryAssert::readLines)
                    .filter(SourceBoundaryAssert::isForbiddenImport)
                    .toList();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to scan source root " + sourceRoot, e);
        }
        if (!badImports.isEmpty()) {
            throw new AssertionError("Forbidden imports found: " + badImports);
        }
    }

    private static Stream<String> readLines(Path path) {
        try {
            return Files.readAllLines(path).stream();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read source file " + path, e);
        }
    }

    private static boolean isForbiddenImport(String line) {
        return FORBIDDEN_IMPORT_PREFIXES.stream().anyMatch(line::startsWith);
    }
}
```

- [ ] **Step 4: Run test support validation**

Run:

```bash
mvn -pl egon-cola-components/egon-cola-component-common/egon-cola-component-common-test -am test
```

Expected: PASS.

- [ ] **Step 5: Remove old single-Jar source tree**

Run:

```bash
rm -rf egon-cola-components/egon-cola-component-common/src/main/java \
  egon-cola-components/egon-cola-component-common/src/test/java
```

Expected: old source tree is removed. Do not remove `target` by hand unless it is tracked, which it should not be.

- [ ] **Step 6: Confirm removed APIs are gone from tracked source**

Run:

```bash
rg -n "package top\.egon\.cola\.component\.common\.util|class StringUtils|class CollectionUtils|class DateTimeUtils|class JsonUtils|class BaseEntity|class AuditableModel" egon-cola-components/egon-cola-component-common || true
```

Expected: no matches in `src/main/java`; matches in the design/plan docs are acceptable.

- [ ] **Step 7: Run full common validation**

Run:

```bash
mvn -pl egon-cola-components/egon-cola-component-common -am test
```

Expected: PASS.

- [ ] **Step 8: Commit boundary support and old tree removal**

Run:

```bash
git add egon-cola-components/egon-cola-component-common
git commit -m "refactor: remove legacy common utility api"
```

Expected: commit succeeds.

---

### Task 7: Update BOM and documentation

**Files:**
- Modify: `egon-cola-components/egon-cola-components-bom/pom.xml`
- Create: `egon-cola-components/egon-cola-component-common/README.md`
- Modify: `egon-cola-components/egon-cola-components-architecture.md`

- [ ] **Step 1: Update BOM to export concrete common modules**

In `egon-cola-components/egon-cola-components-bom/pom.xml`, replace the existing dependencyManagement entry for `egon-cola-component-common` with these dependencies:

```xml
            <dependency>
                <groupId>top.egon</groupId>
                <artifactId>egon-cola-component-common-core</artifactId>
                <version>${project.version}</version>
            </dependency>
            <dependency>
                <groupId>top.egon</groupId>
                <artifactId>egon-cola-component-common-model</artifactId>
                <version>${project.version}</version>
            </dependency>
            <dependency>
                <groupId>top.egon</groupId>
                <artifactId>egon-cola-component-common-trace</artifactId>
                <version>${project.version}</version>
            </dependency>
            <dependency>
                <groupId>top.egon</groupId>
                <artifactId>egon-cola-component-common-result</artifactId>
                <version>${project.version}</version>
            </dependency>
            <dependency>
                <groupId>top.egon</groupId>
                <artifactId>egon-cola-component-common-id</artifactId>
                <version>${project.version}</version>
            </dependency>
            <dependency>
                <groupId>top.egon</groupId>
                <artifactId>egon-cola-component-common-crypto</artifactId>
                <version>${project.version}</version>
            </dependency>
            <dependency>
                <groupId>top.egon</groupId>
                <artifactId>egon-cola-component-common-mask</artifactId>
                <version>${project.version}</version>
            </dependency>
            <dependency>
                <groupId>top.egon</groupId>
                <artifactId>egon-cola-component-common-structure</artifactId>
                <version>${project.version}</version>
            </dependency>
```

Do not export `egon-cola-component-common-test` as a default business dependency.

- [ ] **Step 2: Write common README**

Write `egon-cola-components/egon-cola-component-common/README.md` with:

````markdown
# egon-cola-component-common

`egon-cola-component-common` is the aggregator for Egon COLA enterprise common modules.

It is not a business dependency Jar. Business applications should depend on the concrete modules they need.

## Modules

| Module | Responsibility |
|---|---|
| `egon-cola-component-common-core` | Error status, exceptions, enum contracts |
| `egon-cola-component-common-model` | Request, query, and pagination models |
| `egon-cola-component-common-trace` | Trace context based on MDC key `traceId` |
| `egon-cola-component-common-result` | External result DTOs and internal result models |
| `egon-cola-component-common-id` | UUIDv7 and ID generator contracts |
| `egon-cola-component-common-crypto` | SHA-256, HMAC-SHA256, Base64, Hex helpers |
| `egon-cola-component-common-mask` | Stable data masking helpers |
| `egon-cola-component-common-structure` | Tree structure helpers |
| `egon-cola-component-common-test` | Internal common boundary test support |

## Dependency Examples

Use the components BOM:

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
```

Then import only the module you need:

```xml
<dependency>
    <groupId>top.egon</groupId>
    <artifactId>egon-cola-component-common-result</artifactId>
</dependency>
```

## Migration Notes

| Old API | New API |
|---|---|
| `top.egon.cola.component.common.result.Result` | `top.egon.cola.component.common.result.dto.ResultDto` or `top.egon.cola.component.common.result.model.ResultModel` |
| `top.egon.cola.component.common.result.PageResult` | `PageResultDto` or `PageResultModel` |
| `top.egon.cola.component.common.exception.BusinessException` | `EgonBusinessException` |
| `top.egon.cola.component.common.exception.SystemException` | `EgonSystemException` |
| `top.egon.cola.component.common.exception.ErrorCodes` | `CommonStatus` |
| `top.egon.cola.component.common.util.IdUtils` | `UuidV7` or `UuidV7Generator` |
| `top.egon.cola.component.common.util.CryptoUtils` | `Digests`, `Hmacs`, `Base64s`, `Hexes` |
| `top.egon.cola.component.common.util.MaskingUtils` | `Masking` |
| `top.egon.cola.component.common.util.TreeUtils` | `TreeBuilder` |

The old `util` package, `BaseEntity`, and `AuditableModel` are intentionally removed.
````

- [ ] **Step 3: Update architecture document common exception paragraph**

In `egon-cola-components/egon-cola-components-architecture.md`, replace the existing sentence:

```markdown
`egon-cola-component-common` 是明确的纯 Jar 例外，它不需要拆出 starter / admin / test，业务系统可以直接依赖。
```

with:

```markdown
`egon-cola-component-common` 是明确的纯基础组件例外，它不采用 starter / admin / test 结构，而是作为 common 聚合 POM 管理多个可按需依赖的基础语义 Jar。业务系统不直接依赖 `egon-cola-component-common` 聚合 POM，而是按需依赖 `egon-cola-component-common-core`、`egon-cola-component-common-result`、`egon-cola-component-common-id` 等具体模块。
```

- [ ] **Step 4: Run BOM and common validation**

Run:

```bash
mvn -pl egon-cola-components/egon-cola-components-bom,egon-cola-components/egon-cola-component-common -am test
```

Expected: PASS.

- [ ] **Step 5: Commit BOM and docs**

Run:

```bash
git add egon-cola-components/egon-cola-components-bom/pom.xml \
  egon-cola-components/egon-cola-component-common/README.md \
  egon-cola-components/egon-cola-components-architecture.md
git commit -m "docs: document common module split"
```

Expected: commit succeeds.

---

### Task 8: Run final validation and record completion

**Files:**
- Modify: `docs/superpowers/plans/2026-07-07-egon-cola-component-common-enterprise-restructure-implementation-plan.md`

- [ ] **Step 1: Run common module validation**

Run:

```bash
mvn -pl egon-cola-components/egon-cola-component-common -am test
```

Expected: PASS.

- [ ] **Step 2: Run components-level validation**

Run:

```bash
mvn -pl egon-cola-components -am clean test
```

Expected: PASS. If failures are unrelated to common restructuring, record the exact failing command and error before deciding whether to stop or continue.

- [ ] **Step 3: Run archetype contract validation**

Run:

```bash
mvn clean integration-test
```

Expected: PASS. This is the repository-level contract for generated archetype verification.

- [ ] **Step 4: Confirm removed APIs do not exist**

Run:

```bash
rg -n "package top\.egon\.cola\.component\.common\.util|class StringUtils|class CollectionUtils|class DateTimeUtils|class JsonUtils|class BaseEntity|class AuditableModel" egon-cola-components/egon-cola-component-common || true
```

Expected: no matches in implementation source. Matches in Markdown docs are acceptable if they are migration notes or design records.

- [ ] **Step 5: Confirm BOM does not export common aggregator**

Run:

```bash
rg -n "<artifactId>egon-cola-component-common</artifactId>" egon-cola-components/egon-cola-components-bom/pom.xml || true
```

Expected: no match for the aggregator artifact in the BOM.

- [ ] **Step 6: Mark execution progress in this plan**

Append this section to this plan file after validation succeeds:

````markdown
## Execution Progress

- Task 1: completed and committed.
- Task 2: completed and committed.
- Task 3: completed and committed.
- Task 4: completed and committed.
- Task 5: completed and committed.
- Task 6: completed and committed.
- Task 7: completed and committed.
- Final validation: completed.

Validation commands:

```bash
mvn -pl egon-cola-components/egon-cola-component-common -am test
mvn -pl egon-cola-components -am clean test
mvn clean integration-test
```
````

- [ ] **Step 7: Commit validation record**

Run:

```bash
git add docs/superpowers/plans/2026-07-07-egon-cola-component-common-enterprise-restructure-implementation-plan.md
git commit -m "docs: record common restructure validation"
```

Expected: commit succeeds.

---

## Self-Review Checklist

- [ ] Spec coverage: every approved design section maps to one or more implementation tasks.
- [ ] Module scope: every new common module has one clear responsibility.
- [ ] Dependency direction: `core` is bottom-level and no module depends on `result` except callers.
- [ ] Placeholder scan: plan contains no vague or incomplete implementation instruction.
- [ ] Validation: each task has targeted validation before commit.
- [ ] Final proof: final task includes common validation, components validation, and `clean integration-test`.

## Execution Progress

- Task 1: completed and committed as `96c597e refactor: split common component aggregator`.
- Task 2: completed and committed as `9e4ff7e feat: add common core contracts`.
- Task 3: completed and committed as `e1853c1 feat: add common model module`.
- Task 4: completed and committed as `1fe4f72 feat: add trace and result modules`.
- Task 5: completed and committed as `9ff3e08 feat: add common enterprise capability modules`.
- Task 6: completed and committed as `3719e73 refactor: remove legacy common utility api`.
- Task 7: completed and committed as `7ca2d84 docs: document common module split`.
- Downstream component migration: completed and committed as `cdacbe3 refactor: migrate components to split common modules`.
- Final validation: completed.

Validation commands:

```bash
mvn -f egon-cola-components/egon-cola-component-common/pom.xml test
mvn -pl :egon-cola-component-dynamic-config-center-starter,:egon-cola-component-dynamic-config-center-admin,:egon-cola-component-dynamic-config-center-test,:egon-cola-component-dynamic-thread-pool-starter -am test
mvn clean integration-test
rg -n "package top\\.egon\\.cola\\.component\\.common\\.util|class StringUtils|class CollectionUtils|class DateTimeUtils|class JsonUtils|class BaseEntity|class AuditableModel" egon-cola-components/egon-cola-component-common || true
rg -n "<artifactId>egon-cola-component-common</artifactId>" egon-cola-components/egon-cola-components-bom/pom.xml || true
```
