# egon-cola-components-bom

[English](README.md) | [中文](README.zh-CN.md)

## Overview

`egon-cola-components-bom` is the Maven BOM for the Egon COLA component ecosystem. It provides no runtime code. Its only responsibility is to manage versions consistently for components under `egon-cola-components` that business applications can consume directly, avoiding repeated version declarations on every component dependency.

The BOM exports stable consumption entry points: specific common submodules, business component starters, and the bytecode component's public API, bridge, runtime, Agent, and starter. Admin, test, and aggregator POM modules are not exported as business dependency entry points.

## Features

### Unified Version Management

After a business application imports the BOM through `dependencyManagement`, subsequent component dependencies do not need their own `<version>`. All component versions follow the BOM's `project.version`, currently `5.2.3`.

### Exported Dependencies

| Artifact | Purpose |
|---|---|
| `egon-cola-component-common-core` | Error statuses, exceptions, and enum contracts |
| `egon-cola-component-common-model` | Request, query, and pagination models |
| `egon-cola-component-common-trace` | MDC `traceId` context |
| `egon-cola-component-common-result` | External response DTOs and internal result Models |
| `egon-cola-component-common-id` | UUIDv7 and ID generation |
| `egon-cola-component-common-crypto` | Digests, HMAC, Base64, and Hex |
| `egon-cola-component-common-mask` | Data masking |
| `egon-cola-component-common-structure` | Tree construction |
| `egon-cola-component-dynamic-thread-pool-starter` | Business-side dynamic thread-pool starter |
| `egon-cola-component-dynamic-config-center-starter` | Business-side dynamic configuration center starter |
| `egon-cola-component-rule-engine-starter` | Rule engine starter |
| `egon-cola-component-access-guard-starter` | Method access governance starter |
| `egon-cola-component-method-extension-starter` | Method extension starter |
| `egon-cola-component-bytecode-api` | Public bytecode capability API |
| `egon-cola-component-bytecode-bridge` | Bridge between business applications and the Agent |
| `egon-cola-component-bytecode-runtime` | Bytecode runtime implementation |
| `egon-cola-component-bytecode-agent` | Java Agent entry point |
| `egon-cola-component-bytecode-starter` | Spring Boot starter for bytecode capabilities |

### Modules Not Exported

| Module | Reason |
|---|---|
| `egon-cola-component-common` | Aggregator POM, not a runtime JAR |
| `*-admin` | Standalone services that should be deployed as applications, not used as business dependencies |
| `*-test` | Component samples and verification modules that should not enter the business runtime |
| `egon-cola-component-dynamic-thread-pool` / `dynamic-config-center` / `rule-engine` / `access-guard` / `method-extension` / `bytecode` | Component aggregator POMs, not business dependency entry points |

## Complete Usage Example

### 1. Import the BOM in a Business Project

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

### 2. Include Components as Needed

```xml
<dependencies>
    <dependency>
        <groupId>top.egon</groupId>
        <artifactId>egon-cola-component-common-result</artifactId>
    </dependency>
    <dependency>
        <groupId>top.egon</groupId>
        <artifactId>egon-cola-component-common-model</artifactId>
    </dependency>
    <dependency>
        <groupId>top.egon</groupId>
        <artifactId>egon-cola-component-rule-engine-starter</artifactId>
    </dependency>
    <dependency>
        <groupId>top.egon</groupId>
        <artifactId>egon-cola-component-access-guard-starter</artifactId>
    </dependency>
</dependencies>
```

### 3. Centralize the Version in a Multi-Module Business Project

```xml
<properties>
    <egon-cola.version>5.2.3</egon-cola.version>
</properties>

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

Child modules declare only the artifact:

```xml
<dependency>
    <groupId>top.egon</groupId>
    <artifactId>egon-cola-component-dynamic-config-center-starter</artifactId>
</dependency>
```

## Design Principles and Implementation Details

### Design Principles

1. The BOM manages only runtime entry points that consumers actually need, preventing accidental business dependencies on admin, test, or aggregator modules.
2. Common capabilities are exported at fine granularity so applications can choose only what they need instead of receiving a large transitive common package.
3. Regular business components export only their starter, keeping the Spring Boot auto-configuration entry point explicit. The bytecode component manages its public API, bridge, runtime, Agent, and starter boundaries separately.
4. Every managed version follows the BOM's own version, reducing version drift when components are combined.

### Implementation Details

- Its `packaging` is `pom`; it has no source code or runtime classes.
- Every exported component is declared in `<dependencyManagement>` with `${project.version}` as its version.
- The release profile configures source archives, Javadoc, GPG signing, and Central Portal publishing.
- `maven-deploy-plugin` defaults to `skip=true`; the Central Publishing profile controls the publication path.

## Boundaries and Operational Notes

- A business application cannot depend only on the BOM. The BOM must be imported in `dependencyManagement`.
- Admin modules must be built and deployed as standalone Spring Boot applications, not consumed as business dependencies through the BOM.
- When adding a component, export its starter instead of its aggregator POM or test module. Export additional modules only when they have an explicit, independent consumption boundary.
- When common gains a submodule, decide whether it is a stable business runtime entry point before adding it to the BOM.

## Validation Command

```bash
./mvnw -B -ntp -pl egon-cola-components/egon-cola-components-bom -am test
```
