# Egon-COLA

[English](README.md) | [中文](README.zh-CN.md)

> Java 21 clean layered architecture scaffolding and reusable Spring Boot components.

[![Fast CI](https://github.com/AllenDEricDAlexander/Egon-COLA/actions/workflows/ci.yaml/badge.svg)](https://github.com/AllenDEricDAlexander/Egon-COLA/actions/workflows/ci.yaml)
[![Strong CI](https://github.com/AllenDEricDAlexander/Egon-COLA/actions/workflows/ci_java_compatibility.yaml/badge.svg)](https://github.com/AllenDEricDAlexander/Egon-COLA/actions/workflows/ci_java_compatibility.yaml)
[![Java](https://img.shields.io/badge/Java-21-blue.svg)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.x-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![License](https://img.shields.io/badge/license-MIT%20%2F%20LGPL--2.1-blue.svg)](#license)

Egon-COLA is a Java 21 and Spring Boot 3.x project scaffold and reusable component collection. It does not try to write an entire business system for you. Instead, it constrains the parts of an enterprise Java project that most often drift: project structure, layer boundaries, entry-point adapters, component reuse, and new-project initialization.

In short, Egon-COLA establishes the engineering direction while business teams retain ownership of business details.

## Project Positioning

Egon-COLA focuses on three capabilities:

| Capability | Description |
|---|---|
| Project scaffolding | Generate light, service, and web business project skeletons through Maven Archetypes. |
| Layering conventions | Standardize the boundaries of `common / facade / domain / application / infrastructure / adapter / starter`. |
| Component ecosystem | Provide reusable components, starters, a BOM, test utilities, and component development conventions. |

Egon-COLA is an engineering foundation rather than a complete business framework. Business systems can choose components and technologies as needed; the architecture constrains direction without prescribing every package name or forcing a heavyweight DDD template.

## Repository Layout

```text
Egon-COLA
├── .github/                  # GitHub Actions workflows
├── .mvn/wrapper/             # Maven Wrapper
├── cola-samples/             # Example projects generated from archetypes
│   ├── light/
│   ├── fable/
│   └── fable-web/
├── docs/superpowers/         # Design specifications and execution plans
├── egon-cola-archetypes/     # Maven Archetype projects
│   ├── egon-cola-archetype-light/
│   ├── egon-cola-archetype-service/
│   ├── egon-cola-archetype-web/
│   ├── architecture-mermaid-diagrams.md
│   └── code-style-abstract.md
├── egon-cola-components/     # Reusable components, starters, BOM, and component tests
│   ├── egon-cola-components-bom/
│   ├── egon-cola-component-common/
│   │   └── egon-cola-component-common-id-starter/
│   ├── egon-cola-component-dynamic-thread-pool/
│   ├── egon-cola-component-rpc/
│   ├── egon-cola-component-rule-engine/
│   ├── egon-cola-component-access-guard-starter/
│   ├── egon-cola-component-method-extension/
│   ├── egon-cola-component-transactional-outbox/
│   └── egon-cola-component-bytecode/
├── egon-cola-platforms/      # Enterprise infrastructure platforms and their shared parent POM
│   ├── egon-cola-platform-dynamic-config-center/
│   └── egon-cola-platform-gateway/
├── scripts/                  # Local verification, version updates, and release notes
├── mvnw
├── mvnw.cmd
└── pom.xml
```

## Technology Versions

| Technology      | Version             |
|----------------|-------------------|
| JDK            | 21                |
| Maven Wrapper  | 3.9.14            |
| Spring Boot    | 3.5.16            |
| Dubbo          | 3.3.6             |
| MapStruct Plus | 1.5.1             |
| Lombok         | 1.18.38 / 1.18.46 |
| JUnit Jupiter  | 5.12.2            |

## Quick Start

```bash
git clone https://github.com/AllenDEricDAlexander/Egon-COLA.git
cd Egon-COLA
./mvnw -V --no-transfer-progress clean install
```

To verify the RPC component together with its required DDC platform client:

```bash
./mvnw -B -ntp -pl :egon-cola-component-rpc-test-suite -am test
```

## Local Verification

Quick verification, equivalent to the core Fast CI build:

```bash
./mvnw -V --no-transfer-progress clean install
```

For the host-local unified IdP, RBAC3, DDC, Gateway, and complete MCP topology,
use the [unified identity and MCP local runbook](docs/operations/unified-identity-mcp-local-runbook.md).

The core Strong CI build runs separately on JDK 21 and JDK 25. The complete workflow also verifies generated archetype projects and Docker images; see `.github/workflows/ci_java_compatibility.yaml` for the exact steps.

```bash
./mvnw -B -ntp clean install
```

Generation verification for all three archetypes:

```bash
./mvnw -B -ntp \
  -pl egon-cola-archetypes/egon-cola-archetype-light,egon-cola-archetypes/egon-cola-archetype-service,egon-cola-archetypes/egon-cola-archetype-web \
  -am clean integration-test
```

## Generating the Three Archetypes from a Remote Repository

Egon-COLA currently provides three Maven Archetypes:

| Archetype | Use case | Generated project |
|---|---|---|
| `egon-cola-archetype-light` | A lightweight single-module project for small services, component tests, and quick verification. | A single-module project in the `student-management` style. |
| `egon-cola-archetype-service` | A backend-only service that exposes Dubbo3 Triple RPC / MQ capabilities without HTTP Controllers. | A multi-module project in the `student-management-evaluation` style. |
| `egon-cola-archetype-web` | A complete web business service with HTTP adapters, Dubbo3 Triple facades, application, domain, and infrastructure layers. | A multi-module project in the `student-management-organization` style. |

### Generate a light Project

```bash
mvn -B archetype:generate \
  -DgroupId='top.egon' \
  -DartifactId='light' \
  -Dversion='1.0.0-SNAPSHOT' \
  -Dpackage='top.egon.light' \
  -DarchetypeGroupId='top.egon' \
  -DarchetypeArtifactId='egon-cola-archetype-light' \
  -DarchetypeVersion='5.3.3' \
  -DinteractiveMode='false'
```

### Generate a service Project

```bash
mvn -B archetype:generate \
  -DgroupId='top.egon' \
  -DartifactId='fable' \
  -Dversion='1.0.0-SNAPSHOT' \
  -Dpackage='top.egon.fable' \
  -DarchetypeGroupId='top.egon' \
  -DarchetypeArtifactId='egon-cola-archetype-service' \
  -DarchetypeVersion='5.3.3' \
  -DinteractiveMode='false'
```

### Generate a web Project

```bash
mvn -B archetype:generate \
  -DgroupId='top.egon' \
  -DartifactId='fable-web' \
  -Dversion='1.0.0-SNAPSHOT' \
  -Dpackage='top.egon.fable.web' \
  -DarchetypeGroupId='top.egon' \
  -DarchetypeArtifactId='egon-cola-archetype-web' \
  -DarchetypeVersion='5.3.3' \
  -DinteractiveMode='false'
```

After generation, use the target directory as the root of the new repository and open its root `pom.xml` in IntelliJ IDEA.

## Generating the Three Archetypes Locally

Egon-COLA currently provides three Maven Archetypes:

| Archetype | Use case | Generated project |
|---|---|---|
| `egon-cola-archetype-light` | A lightweight single-module project for small services, component tests, and quick verification. | A single-module project in the `student-management` style. |
| `egon-cola-archetype-service` | A backend-only service that exposes Dubbo3 Triple RPC / MQ capabilities without HTTP Controllers. | A multi-module project in the `student-management-evaluation` style. |
| `egon-cola-archetype-web` | A complete web business service with HTTP adapters, Dubbo3 Triple facades, application, domain, and infrastructure layers. | A multi-module project in the `student-management-organization` style. |

To use the latest archetype from the local repository, run this before generation:

```bash
./mvnw -V --no-transfer-progress clean install
```

### Generate a light Project

```bash
mvn -B archetype:generate \
  -DgroupId='top.egon' \
  -DartifactId='light' \
  -Dversion='1.0.0-SNAPSHOT' \
  -Dpackage='top.egon.light' \
  -DarchetypeGroupId='top.egon' \
  -DarchetypeArtifactId='egon-cola-archetype-light' \
  -DarchetypeVersion='5.3.3' \
  -DarchetypeCatalog='local' \
  -DinteractiveMode='false'
```

### Generate a service Project

```bash
mvn -B archetype:generate \
  -DgroupId='top.egon' \
  -DartifactId='fable' \
  -Dversion='1.0.0-SNAPSHOT' \
  -Dpackage='top.egon.fable' \
  -DarchetypeGroupId='top.egon' \
  -DarchetypeArtifactId='egon-cola-archetype-service' \
  -DarchetypeVersion='5.3.3' \
  -DarchetypeCatalog='local' \
  -DinteractiveMode='false'
```

### Generate a web Project

```bash
mvn -B archetype:generate \
  -DgroupId='top.egon' \
  -DartifactId='fable-web' \
  -Dversion='1.0.0-SNAPSHOT' \
  -Dpackage='top.egon.fable.web' \
  -DarchetypeGroupId='top.egon' \
  -DarchetypeArtifactId='egon-cola-archetype-web' \
  -DarchetypeVersion='5.3.3' \
  -DarchetypeCatalog='local' \
  -DinteractiveMode='false'
```

After generation, use the target directory as the root of the new repository and open its root `pom.xml` in IntelliJ IDEA.

## Component Ecosystem

`egon-cola-components` contains reusable runtime capabilities, standalone control-plane
applications, test projects, and the public Components BOM. The component README files
are the source of truth for each component's API, configuration, boundaries, and focused
verification command.

| Component | Main entry point | Scope |
|---|---|---|
| [Common](egon-cola-components/egon-cola-component-common/README.md) | `egon-cola-component-common-*`, [`...-id-starter`](egon-cola-components/egon-cola-component-common/egon-cola-component-common-id-starter/README.md) | Common contracts plus Snowflake ID generation and Spring Boot auto-configuration. |
| [Dynamic Thread Pool](egon-cola-components/egon-cola-component-dynamic-thread-pool/README.md) | `...-starter` | Executor registration, snapshots, Redis changes, resizing, virtual-thread limits, and MDC propagation. |
| [RPC](egon-cola-components/egon-cola-component-rpc/README.md) | `...-starter` | Protobuf/gRPC Provider and Consumer contracts, DDC registration/discovery, deadlines, and Gateway channels. |
| [Rule Engine](egon-cola-components/egon-cola-component-rule-engine/README.md) | `...-starter` | Java rule chains, singleton chains of responsibility, rule trees, traces, limits, and listeners. |
| [Access Guard](egon-cola-components/egon-cola-component-access-guard-starter/README.md) | `...-starter` | One Starter with unified AOP, programmatic, and optional Agent governance. |
| [Method Extension](egon-cola-components/egon-cola-component-method-extension/README.md) | `...-starter` | AOP or Agent-based business decision handlers before annotated methods. |
| [Transactional Outbox](egon-cola-components/egon-cola-component-transactional-outbox/README.md) | `...-starter` | PostgreSQL/JDBC at-least-once delivery through HTTP, RabbitMQ, or custom handlers. |
| [Bytecode](egon-cola-components/egon-cola-component-bytecode/README.md) | API, bridge, runtime, Agent, and starter | Build-time architecture checks plus optional executor, observation, Method Extension, and Access Guard enhancement. |
| [Components BOM](egon-cola-components/egon-cola-components-bom/README.md) | `egon-cola-components-bom` | Central version management for public component consumption artifacts. |

## Platform Ecosystem

`egon-cola-platforms` contains deployable enterprise infrastructure systems. Its empty
packaging parent POM aggregates only platform systems and centralizes their dependency
imports and plugin versions. Platforms may consume public components, while the Components
BOM does not export platform artifacts.

| Platform | Main entry point | Scope |
|---|---|---|
| [Dynamic Config Center](egon-cola-platforms/egon-cola-platform-dynamic-config-center/README.md) | Starter, Admin | Dynamic configuration, Redis leases/service registry, synchronous publish, and standalone control plane. |
| [Gateway](egon-cola-platforms/egon-cola-platform-gateway/README.md) | Engine, Admin, Starter, Provider Runtime | HTTP/RPC data plane, rule releases, provider discovery, security, observability, and deployment assets. |

Recommended structure for runtime starter-style components:

```text
egon-cola-component-xxx
├── pom.xml
├── egon-cola-component-xxx-starter   # Direct dependency for business systems
├── egon-cola-component-xxx-test      # Test / example project
└── egon-cola-component-xxx-admin     # Optional backend management service
```

Component constraints:

- `egon-cola-component-common` is an aggregator POM; business systems depend on concrete JARs such as `egon-cola-component-common-core`, or on the direct `egon-cola-component-common-id-starter` Spring Boot entry point.
- Except for pure-JAR foundations such as `common`, runtime starter-style components are consumed through their `starter` module.
- `starter` must not depend back on `admin`, `test`, or `ui`.
- `test` is reserved for component self-tests, integration tests, and example startup.
- `admin` is optional and must be independently deployable when present.
- Component projects do not contain UI; UI is maintained in a separate frontend repository.

The Gateway Admin Web is the exception to the Maven component layout: it is a private
React application colocated with the Gateway sources and is built with npm. Gateway
deployment, frontend, and performance instructions are linked from the Gateway README.

## Using the BOM

Business systems can manage component versions centrally through the BOM:

```xml

<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>top.egon</groupId>
            <artifactId>egon-cola-components-bom</artifactId>
            <version>5.2.3</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

Then add only the components you need:

```xml

<dependencies>
    <dependency>
        <groupId>top.egon</groupId>
        <artifactId>egon-cola-component-common-core</artifactId>
    </dependency>
    <dependency>
        <groupId>top.egon</groupId>
        <artifactId>egon-cola-component-common-id-starter</artifactId>
    </dependency>
    <dependency>
        <groupId>top.egon</groupId>
        <artifactId>egon-cola-component-transactional-outbox-starter</artifactId>
    </dependency>
</dependencies>
```

The dynamic thread-pool starter is optional; add it only when the business system needs thread-pool governance:

```xml

<dependencies>
    <dependency>
        <groupId>top.egon</groupId>
        <artifactId>egon-cola-component-dynamic-thread-pool-starter</artifactId>
    </dependency>
</dependencies>
```

If a component has not been published to Maven Central, run `./mvnw clean install` in this repository before using it from a business project.

## CI

Fast CI uses `.github/workflows/ci.yaml`, runs on a GitHub-hosted Ubuntu runner, and executes the following separately on JDK 21 and JDK 25 inside a Rocky Linux 10 container:

```bash
./mvnw -V --no-transfer-progress -DtrimStackTrace=false clean install
```

Strong CI uses `.github/workflows/ci_java_compatibility.yaml`, runs on a GitHub-hosted Ubuntu runner, executes `clean install` separately on JDK 21 and JDK 25 inside a Rocky Linux 10 container, verifies projects generated by all three archetypes, and finally builds a Docker image on the host runner:

```bash
./mvnw -B -ntp clean install
```

## Release

Egon-COLA uses the Sonatype Central Portal release process. DDC is a platform, while the
RPC component consumes its client SDK and Gateway consumes RPC. Maven topologically sorts
that graph in the root reactor, so fresh versions must be verified and published from the
root rather than as separate Components and Platforms releases.

```bash
./mvnw -B -ntp -Prelease -DskipTests verify
./mvnw -B -ntp -Prelease -DskipTests clean deploy
```

See [scripts/maven-deploy.md](scripts/maven-deploy.md) for detailed steps.

## Documentation Guide

| Document | Description |
|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|---------------------------|
| [egon-cola-archetypes/code-style-abstract.md](egon-cola-archetypes/code-style-abstract.md) | Coding style for the large-monolith light domain-layered architecture. |
| [egon-cola-archetypes/architecture-mermaid-diagrams.md](egon-cola-archetypes/architecture-mermaid-diagrams.md) | Mermaid diagrams for layer dependencies, call flows, and architecture boundaries. |
| [egon-cola-archetypes/egon-cola-archetype-light/large-monolith-light-domain-architecture.md](egon-cola-archetypes/egon-cola-archetype-light/large-monolith-light-domain-architecture.md) | light archetype architecture. |
| [egon-cola-archetypes/egon-cola-archetype-service/student-management-service-only-rpc-mq-architecture.md](egon-cola-archetypes/egon-cola-archetype-service/student-management-service-only-rpc-mq-architecture.md) | service archetype architecture. |
| [egon-cola-archetypes/egon-cola-archetype-web/multi-project-multi-module-architecture.md](egon-cola-archetypes/egon-cola-archetype-web/multi-project-multi-module-architecture.md) | web archetype architecture. |
| [egon-cola-components/egon-cola-components-architecture.md](egon-cola-components/egon-cola-components-architecture.md) | Multi-component project structure conventions. |
| [egon-cola-components/egon-cola-components-bom/README.md](egon-cola-components/egon-cola-components-bom/README.md) | Public component versions and export boundaries. |
| [egon-cola-components/egon-cola-component-common/egon-cola-component-common-id-starter/README.md](egon-cola-components/egon-cola-component-common/egon-cola-component-common-id-starter/README.md) | Snowflake ID configuration, guarantees, and operational boundaries. |
| [egon-cola-platforms/egon-cola-platform-dynamic-config-center/README.md](egon-cola-platforms/egon-cola-platform-dynamic-config-center/README.md) | Dynamic configuration, leases, registry, and publish protocol. |
| [egon-cola-components/egon-cola-component-rpc/README.md](egon-cola-components/egon-cola-component-rpc/README.md) | Protobuf/gRPC Provider and Consumer contract. |
| [egon-cola-platforms/egon-cola-platform-gateway/README.md](egon-cola-platforms/egon-cola-platform-gateway/README.md) | HTTP/RPC Gateway platform and deployment links. |
| [egon-cola-components/egon-cola-component-transactional-outbox/README.md](egon-cola-components/egon-cola-component-transactional-outbox/README.md) | PostgreSQL/JDBC transactional outbox usage and guarantees. |
| [scripts/maven-deploy.md](scripts/maven-deploy.md) | Maven Central release instructions. |

## Project Origin

Egon-COLA was originally forked from [alibaba/COLA](https://github.com/alibaba/COLA).

This repository is now maintained as an independent architecture project.
The original fork relationship has been intentionally detached to avoid accidental upstream synchronization and to keep
the project direction independent.

## License

This project is dual-licensed under the MIT License and the GNU Lesser General Public License v2.1.

You may choose either license:

- MIT License, see [LICENSE-MIT](LICENSE-MIT).
- GNU LGPL v2.1, see [LICENSE-LGPL-2.1](LICENSE-LGPL-2.1).
