# Egon-COLA

> Java 21 clean layered architecture scaffolding and reusable Spring Boot components.

[![Fast CI](https://github.com/AllenDEricDAlexander/Egon-COLA/actions/workflows/ci.yaml/badge.svg)](https://github.com/AllenDEricDAlexander/Egon-COLA/actions/workflows/ci.yaml)
[![Strong CI](https://github.com/AllenDEricDAlexander/Egon-COLA/actions/workflows/ci_by_multiply_java_versions.yaml/badge.svg)](https://github.com/AllenDEricDAlexander/Egon-COLA/actions/workflows/ci_by_multiply_java_versions.yaml)
[![Java](https://img.shields.io/badge/Java-21-blue.svg)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.x-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![License](https://img.shields.io/badge/license-MIT%20%2F%20LGPL--2.1-blue.svg)](#license)

Egon-COLA 是面向 Java 21 与 Spring Boot 3.x 的工程脚手架和基础组件集合。它的目标不是替业务写完所有代码，而是把企业级 Java
工程中最容易失控的部分先约束住：工程结构、分层边界、入口适配、组件复用和新项目初始化方式。

一句话：Egon-COLA 负责把工程大方向立住，业务细节仍然交给业务自己完成。

## 项目定位

Egon-COLA 聚焦三类能力：

| 能力    | 说明                                                                                   |
|-------|--------------------------------------------------------------------------------------|
| 工程脚手架 | 通过 Maven Archetype 生成 light、service、web 三类业务工程骨架。                                    |
| 分层规范  | 统一 `common / facade / domain / application / infrastructure / adapter / starter` 边界。 |
| 组件体系  | 提供可复用组件、starter、BOM、测试工具与组件开发规范。                                                     |

Egon-COLA 更像工程底座，不是完整业务框架。业务系统可以按需选择组件和技术栈；架构只负责约束方向，不替业务规定所有包名细节，也不强制套用重型
DDD 模板。

## 仓库结构

```text
Egon-COLA
├── .github/                  # GitHub Actions 工作流
├── .mvn/wrapper/             # Maven Wrapper
├── cola-samples/             # archetype 生成的示例工程
│   ├── light/
│   ├── fable/
│   └── fable-web/
├── docs/superpowers/         # 设计规格与执行计划
├── egon-cola-archetypes/     # Maven Archetype 工程
│   ├── egon-cola-archetype-light/
│   ├── egon-cola-archetype-service/
│   ├── egon-cola-archetype-web/
│   ├── architecture-mermaid-diagrams.md
│   └── code-style-abstract.md
├── egon-cola-components/     # 基础组件、starter、BOM 与组件开发脚手架
│   ├── egon-cola-components-bom/
│   ├── egon-cola-component-common/
│   └── egon-cola-component-dynamic-thread-pool/
├── scripts/                  # 本地验证、版本调整、发布说明
├── mvnw
├── mvnw.cmd
└── pom.xml
```

## 技术版本

| 技术             | 版本                |
|----------------|-------------------|
| JDK            | 21                |
| Maven Wrapper  | 3.9.14            |
| Spring Boot    | 3.5.16            |
| Dubbo          | 3.3.6             |
| MapStruct Plus | 1.5.1             |
| Lombok         | 1.18.38 / 1.18.46 |
| JUnit Jupiter  | 5.12.2            |

## 快速开始

```bash
git clone https://github.com/AllenDEricDAlexander/Egon-COLA.git
cd Egon-COLA
./mvnw -V --no-transfer-progress clean install
```

只验证 components 工程：

```bash
./mvnw -V --no-transfer-progress -f egon-cola-components/pom.xml test
```

## 本地验证

快速验证，等同 Fast CI 的核心构建：

```bash
./mvnw -V --no-transfer-progress clean install
```

强验证，等同 Strong CI：默认使用 Java 21 完整构建，再使用 Java 22 执行 `surefire:test`，并验证 archetype 生成工程。

```bash
JAVA21_HOME=/path/to/jdk-21 \
JAVA22_HOME=/path/to/jdk-22 \
scripts/integration_test
```

针对三类 archetype 的生成验证：

```bash
./mvnw -B -ntp \
  -pl egon-cola-archetypes/egon-cola-archetype-light,egon-cola-archetypes/egon-cola-archetype-service,egon-cola-archetypes/egon-cola-archetype-web \
  -am clean integration-test
```

## 三类 Archetype 生成方式 远程

Egon-COLA 当前提供三类 Maven Archetype：

| Archetype                     | 适用场景                                                                                       | 生成工程                                       |
|-------------------------------|--------------------------------------------------------------------------------------------|--------------------------------------------|
| `egon-cola-archetype-light`   | 单模块轻量工程，适合小型服务、组件测试、快速验证。                                                                  | `student-management` 风格单模块工程。              |
| `egon-cola-archetype-service` | 纯后端服务工程，适合只提供 Dubbo3 Triple RPC / MQ 能力、不暴露 HTTP Controller 的服务。                           | `student-management-evaluation` 风格多模块工程。   |
| `egon-cola-archetype-web`     | Web 业务工程，适合包含 HTTP adapter、Dubbo3 Triple facade、application、domain、infrastructure 的完整业务服务。 | `student-management-organization` 风格多模块工程。 |

### 生成 light 工程

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

### 生成 service 工程

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

### 生成 web 工程

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

生成完成后，可以将目标目录直接作为新仓库根目录，使用 IDEA 打开根 `pom.xml`。

## 三类 Archetype 生成方式 本地

Egon-COLA 当前提供三类 Maven Archetype：

| Archetype                     | 适用场景                                                                                       | 生成工程                                       |
|-------------------------------|--------------------------------------------------------------------------------------------|--------------------------------------------|
| `egon-cola-archetype-light`   | 单模块轻量工程，适合小型服务、组件测试、快速验证。                                                                  | `student-management` 风格单模块工程。              |
| `egon-cola-archetype-service` | 纯后端服务工程，适合只提供 Dubbo3 Triple RPC / MQ 能力、不暴露 HTTP Controller 的服务。                           | `student-management-evaluation` 风格多模块工程。   |
| `egon-cola-archetype-web`     | Web 业务工程，适合包含 HTTP adapter、Dubbo3 Triple facade、application、domain、infrastructure 的完整业务服务。 | `student-management-organization` 风格多模块工程。 |

生成前如果要使用本地仓库中的最新 archetype，先执行：

```bash
./mvnw -V --no-transfer-progress clean install
```

### 生成 light 工程

```bash
mvn -B archetype:generate \
  -DgroupId='top.egon' \
  -DartifactId='light' \
  -Dversion='1.0.0-SNAPSHOT' \
  -Dpackage='top.egon.light' \
  -DarchetypeGroupId='top.egon' \
  -DarchetypeArtifactId='egon-cola-archetype-light' \
  -DarchetypeVersion='5.2.0-SNAPSHOT' \
  -DarchetypeCatalog='local' \
  -DinteractiveMode='false'
```

### 生成 service 工程

```bash
mvn -B archetype:generate \
  -DgroupId='top.egon' \
  -DartifactId='fable' \
  -Dversion='1.0.0-SNAPSHOT' \
  -Dpackage='top.egon.fable' \
  -DarchetypeGroupId='top.egon' \
  -DarchetypeArtifactId='egon-cola-archetype-service' \
  -DarchetypeVersion='5.2.0-SNAPSHOT' \
  -DarchetypeCatalog='local' \
  -DinteractiveMode='false'
```

### 生成 web 工程

```bash
mvn -B archetype:generate \
  -DgroupId='top.egon' \
  -DartifactId='fable-web' \
  -Dversion='1.0.0-SNAPSHOT' \
  -Dpackage='top.egon.fable.web' \
  -DarchetypeGroupId='top.egon' \
  -DarchetypeArtifactId='egon-cola-archetype-web' \
  -DarchetypeVersion='5.2.0-SNAPSHOT' \
  -DarchetypeCatalog='local' \
  -DinteractiveMode='false'
```

生成完成后，可以将目标目录直接作为新仓库根目录，使用 IDEA 打开根 `pom.xml`。

## 组件体系

`egon-cola-components` 用于沉淀可复用基础能力。

| 组件 | 说明 |
|---|---|
| `egon-cola-component-common` | 纯 Jar 基础组件，提供通用响应、分页、异常、断言等稳定能力。 |
| `egon-cola-component-dynamic-thread-pool-starter` | 业务系统按需引入的动态线程池 starter。 |
| `egon-cola-component-dynamic-thread-pool-admin` | 独立部署的动态线程池管理服务，不进入 BOM。 |
| `egon-cola-components-bom` | 只导出业务系统可直接依赖的 common 与 starter 版本。 |

运行时 starter-style 组件推荐结构：

```text
egon-cola-component-xxx
├── pom.xml
├── egon-cola-component-xxx-starter   # 业务系统直接引入
├── egon-cola-component-xxx-test      # 测试工程 / 示例工程
└── egon-cola-component-xxx-admin     # 可选，后端管理服务
```

组件约束：

- `egon-cola-component-common` 是明确的纯 Jar 例外，业务系统可以直接依赖。
- 除 `common` 这类纯 Jar 基础组件外，运行时 starter-style 组件应由业务系统直接引入 `starter`。
- `starter` 不反向依赖 `admin`、`test`、`ui`。
- `test` 只用于组件自测、集成测试和示例启动。
- `admin` 可选，如果存在，应可以独立部署。
- 组件工程不放 UI，UI 放到独立前端仓库统一维护。

## BOM 使用

业务系统可以通过 BOM 统一管理组件版本：

```xml

<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>top.egon</groupId>
            <artifactId>egon-cola-components-bom</artifactId>
            <version>5.2.0-SNAPSHOT</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

然后按需引入组件：

```xml

<dependencies>
    <dependency>
        <groupId>top.egon</groupId>
        <artifactId>egon-cola-component-common</artifactId>
    </dependency>
</dependencies>
```

动态线程池 starter 是可选运行时组件，业务系统需要线程池治理时再引入：

```xml

<dependencies>
    <dependency>
        <groupId>top.egon</groupId>
        <artifactId>egon-cola-component-dynamic-thread-pool-starter</artifactId>
    </dependency>
</dependencies>
```

如果组件还没有发布到 Maven Central，请先在本仓库执行 `./mvnw clean install`，再在业务工程中使用。

## CI

Fast CI 使用 `.github/workflows/ci.yaml`，在 Ubuntu 和 Windows 的 Java 21 上执行：

```bash
./mvnw -V --no-transfer-progress -DtrimStackTrace=false clean install
```

Strong CI 使用 `.github/workflows/ci_by_multiply_java_versions.yaml`，在 Ubuntu 上安装 Java 21 / 22 并执行：

```bash
scripts/integration_test
```

## 发布

Egon-COLA 使用 Sonatype Central Portal 发布流程。发布前建议先本地验证 release profile：

```bash
./mvnw -B -ntp -f egon-cola-components/pom.xml \
  -Prelease -DskipTests verify

./mvnw -B -ntp -f egon-cola-archetypes/pom.xml \
  -Prelease -DskipTests verify
```

发布父 POM：

```bash
./mvnw -B -ntp -N -Prelease -DskipTests clean deploy
./mvnw -B -ntp -N -f egon-cola-components/pom.xml -Prelease -DskipTests clean deploy
./mvnw -B -ntp -N -f egon-cola-archetypes/pom.xml -Prelease -DskipTests clean deploy
```

发布 components：

```bash
./mvnw -B -ntp -f egon-cola-components/pom.xml -Prelease -DskipTests clean deploy
```

发布 archetypes：

```bash
./mvnw -B -ntp -f egon-cola-archetypes/pom.xml -Prelease -DskipTests clean deploy
```

详细步骤见 [scripts/maven-deploy.md](scripts/maven-deploy.md)。

## 文档导航

| 文档                                                                                                                                                                                                                 | 说明                        |
|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|---------------------------|
| [egon-cola-archetypes/code-style-abstract.md](egon-cola-archetypes/code-style-abstract.md)                                                                                                                         | 大型单体轻量领域分层架构代码风格。         |
| [egon-cola-archetypes/architecture-mermaid-diagrams.md](egon-cola-archetypes/architecture-mermaid-diagrams.md)                                                                                                     | 分层依赖、调用链路、架构边界 Mermaid 图。 |
| [egon-cola-archetypes/egon-cola-archetype-light/large-monolith-light-domain-architecture.md](egon-cola-archetypes/egon-cola-archetype-light/large-monolith-light-domain-architecture.md)                           | light archetype 架构说明。     |
| [egon-cola-archetypes/egon-cola-archetype-service/student-management-service-only-rpc-mq-architecture.md](egon-cola-archetypes/egon-cola-archetype-service/student-management-service-only-rpc-mq-architecture.md) | service archetype 架构说明。   |
| [egon-cola-archetypes/egon-cola-archetype-web/multi-project-multi-module-architecture.md](egon-cola-archetypes/egon-cola-archetype-web/multi-project-multi-module-architecture.md)                                 | web archetype 架构说明。       |
| [egon-cola-components/egon-cola-components-architecture.md](egon-cola-components/egon-cola-components-architecture.md)                                                                                             | 多组件工程结构规范。                |
| [scripts/maven-deploy.md](scripts/maven-deploy.md)                                                                                                                                                                 | Maven Central 发布操作说明。     |

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
