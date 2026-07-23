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
├── egon-cola-components/     # Base components, starters, BOM, and component scaffolding
│   ├── egon-cola-components-bom/
│   ├── egon-cola-component-common/
│   └── egon-cola-component-dynamic-thread-pool/
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

To verify only the components reactor:

```bash
./mvnw -V --no-transfer-progress -f egon-cola-components/pom.xml test
```

## Local Verification

Quick verification, equivalent to the core Fast CI build:

```bash
./mvnw -V --no-transfer-progress clean install
```

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
  -DarchetypeVersion='5.1.2' \
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
  -DarchetypeVersion='5.1.2' \
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
  -DarchetypeVersion='5.1.2' \
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
  -DarchetypeVersion='5.2.3' \
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
  -DarchetypeVersion='5.2.3' \
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
  -DarchetypeVersion='5.2.3' \
  -DarchetypeCatalog='local' \
  -DinteractiveMode='false'
```

After generation, use the target directory as the root of the new repository and open its root `pom.xml` in IntelliJ IDEA.

## Component Ecosystem

`egon-cola-components` stores reusable foundation capabilities.

| Component | Description |
|---|---|
| `egon-cola-component-common-*` | Optional pure-JAR foundation modules for common errors, models, responses, IDs, cryptography, masking, and related capabilities. |
| `egon-cola-component-dynamic-thread-pool-starter` | Dynamic thread-pool starter for business systems to add when needed. |
| `egon-cola-component-dynamic-thread-pool-admin` | Independently deployable dynamic thread-pool management service; not exported by the BOM. |
| `egon-cola-components-bom` | Exports only the common modules and starter versions that business systems can depend on directly. |

Recommended structure for runtime starter-style components:

```text
egon-cola-component-xxx
├── pom.xml
├── egon-cola-component-xxx-starter   # Direct dependency for business systems
├── egon-cola-component-xxx-test      # Test / example project
└── egon-cola-component-xxx-admin     # Optional backend management service
```

Component constraints:

- `egon-cola-component-common` is an aggregator POM; business systems depend on concrete JARs such as `egon-cola-component-common-core` and `egon-cola-component-common-result` as needed.
- Except for pure-JAR foundations such as `common`, runtime starter-style components are consumed through their `starter` module.
- `starter` must not depend back on `admin`, `test`, or `ui`.
- `test` is reserved for component self-tests, integration tests, and example startup.
- `admin` is optional and must be independently deployable when present.
- Component projects do not contain UI; UI is maintained in a separate frontend repository.

## Using the BOM

Business systems can manage component versions centrally through the BOM:

```xml

<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>top.egon</groupId>
            <artifactId>egon-cola-components-bom</artifactId>
            <version>5.2.1</version>
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

Egon-COLA uses the Sonatype Central Portal release process. Before publishing, verify the release profile locally:

```bash
./mvnw -B -ntp -f egon-cola-components/pom.xml \
  -Prelease -DskipTests verify

./mvnw -B -ntp -f egon-cola-archetypes/pom.xml \
  -Prelease -DskipTests verify
```

Publish the parent POMs:

```bash
./mvnw -B -ntp -N -Prelease -DskipTests clean deploy
./mvnw -B -ntp -N -f egon-cola-components/pom.xml -Prelease -DskipTests clean deploy
./mvnw -B -ntp -N -f egon-cola-archetypes/pom.xml -Prelease -DskipTests clean deploy
```

Publish components:

```bash
./mvnw -B -ntp -f egon-cola-components/pom.xml -Prelease -DskipTests clean deploy
```

Publish archetypes:

```bash
./mvnw -B -ntp -f egon-cola-archetypes/pom.xml -Prelease -DskipTests clean deploy
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
