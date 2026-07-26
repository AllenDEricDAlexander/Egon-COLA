# Egon-COLA

[English](README.md) | 中文

> 面向 Java 21 的清晰分层架构脚手架与可复用 Spring Boot 组件集合。

[![Fast CI](https://github.com/AllenDEricDAlexander/Egon-COLA/actions/workflows/ci.yaml/badge.svg)](https://github.com/AllenDEricDAlexander/Egon-COLA/actions/workflows/ci.yaml)
[![Strong CI](https://github.com/AllenDEricDAlexander/Egon-COLA/actions/workflows/ci_java_compatibility.yaml/badge.svg)](https://github.com/AllenDEricDAlexander/Egon-COLA/actions/workflows/ci_java_compatibility.yaml)
[![Java](https://img.shields.io/badge/Java-21-blue.svg)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.x-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![License](https://img.shields.io/badge/license-MIT%20%2F%20LGPL--2.1-blue.svg)](#许可证)

Egon-COLA 是面向 Java 21 与 Spring Boot 3.x 的工程脚手架和基础组件集合。它的目标不是替业务写完所有代码，而是把企业级 Java 工程中最容易失控的部分先约束住：工程结构、分层边界、入口适配、组件复用和新项目初始化方式。

一句话：Egon-COLA 负责把工程大方向立住，业务细节仍然交给业务自己完成。

## 项目定位

Egon-COLA 聚焦三类能力：

| 能力 | 说明 |
|---|---|
| 工程脚手架 | 通过 Maven Archetype 生成 light、service、web 三类业务工程骨架。 |
| 分层规范 | 统一 `common / facade / domain / application / infrastructure / adapter / starter` 边界。 |
| 组件体系 | 提供可复用组件、starter、BOM、测试工具与组件开发规范。 |

Egon-COLA 更像工程底座，不是完整业务框架。业务系统可以按需选择组件和技术栈；架构只负责约束方向，不替业务规定所有包名细节，也不强制套用重型 DDD 模板。

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
├── egon-cola-components/     # 可复用组件、starter、BOM 与组件测试
│   ├── egon-cola-components-bom/
│   ├── egon-cola-component-common/
│   ├── egon-cola-component-dynamic-config-center/
│   ├── egon-cola-component-dynamic-thread-pool/
│   ├── egon-cola-component-rpc/
│   ├── egon-cola-component-rule-engine/
│   ├── egon-cola-component-access-guard/
│   ├── egon-cola-component-method-extension/
│   ├── egon-cola-component-transactional-outbox/
│   ├── egon-cola-component-bytecode/
│   └── egon-cola-component-gateway/
├── scripts/                  # 本地验证、版本调整、发布说明
├── mvnw
├── mvnw.cmd
└── pom.xml
```

## 技术版本

| 技术 | 版本 |
|---|---|
| JDK | 21 |
| Maven Wrapper | 3.9.14 |
| Spring Boot | 3.5.16 |
| Dubbo | 3.3.6 |
| MapStruct Plus | 1.5.1 |
| Lombok | 1.18.38 / 1.18.46 |
| JUnit Jupiter | 5.12.2 |

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

Strong CI 的核心构建需要分别使用 JDK 21 与 JDK 25 执行；完整流程还会验证 archetype 生成项目与 Docker 镜像，具体步骤见 `.github/workflows/ci_java_compatibility.yaml`。

```bash
./mvnw -B -ntp clean install
```

针对三类 archetype 的生成验证：

```bash
./mvnw -B -ntp \
  -pl egon-cola-archetypes/egon-cola-archetype-light,egon-cola-archetypes/egon-cola-archetype-service,egon-cola-archetype-web \
  -am clean integration-test
```

## 三类 Archetype 生成方式：远程仓库

Egon-COLA 当前提供三类 Maven Archetype：

| Archetype | 适用场景 | 生成工程 |
|---|---|---|
| `egon-cola-archetype-light` | 单模块轻量工程，适合小型服务、组件测试、快速验证。 | `student-management` 风格单模块工程。 |
| `egon-cola-archetype-service` | 纯后端服务工程，适合只提供 Dubbo3 Triple RPC / MQ 能力、不暴露 HTTP Controller 的服务。 | `student-management-evaluation` 风格多模块工程。 |
| `egon-cola-archetype-web` | Web 业务工程，适合包含 HTTP adapter、Dubbo3 Triple facade、application、domain、infrastructure 的完整业务服务。 | `student-management-organization` 风格多模块工程。 |

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

## 三类 Archetype 生成方式：本地仓库

Egon-COLA 当前提供三类 Maven Archetype：

| Archetype | 适用场景 | 生成工程 |
|---|---|---|
| `egon-cola-archetype-light` | 单模块轻量工程，适合小型服务、组件测试、快速验证。 | `student-management` 风格单模块工程。 |
| `egon-cola-archetype-service` | 纯后端服务工程，适合只提供 Dubbo3 Triple RPC / MQ 能力、不暴露 HTTP Controller 的服务。 | `student-management-evaluation` 风格多模块工程。 |
| `egon-cola-archetype-web` | Web 业务工程，适合包含 HTTP adapter、Dubbo3 Triple facade、application、domain、infrastructure 的完整业务服务。 | `student-management-organization` 风格多模块工程。 |

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
  -DarchetypeVersion='5.2.1' \
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
  -DarchetypeVersion='5.2.1' \
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
  -DarchetypeVersion='5.2.1' \
  -DarchetypeCatalog='local' \
  -DinteractiveMode='false'
```

生成完成后，可以将目标目录直接作为新仓库根目录，使用 IDEA 打开根 `pom.xml`。

## 组件体系

`egon-cola-components` 包含可复用运行时能力、独立控制面应用、测试工程和公共
Components BOM。各组件 README 是对应 API、配置、边界和专项验证命令的事实来源。

| 组件 | 主要入口 | 范围 |
|---|---|---|
| [Common](egon-cola-components/egon-cola-component-common/README.md) | `egon-cola-component-common-*` | 纯 Jar 错误、模型、结果、trace、ID、加密、脱敏和树结构契约。 |
| [Dynamic Config Center](egon-cola-components/egon-cola-component-dynamic-config-center/README.md) | `...-management-client`、`...-starter` | 动态配置、Redis 租约/服务注册、同步发布和独立 Admin。 |
| [Dynamic Thread Pool](egon-cola-components/egon-cola-component-dynamic-thread-pool/README.md) | `...-starter` | 执行器注册、快照、Redis 变更、扩缩容、虚拟线程并发限制和 MDC 传播。 |
| [RPC](egon-cola-components/egon-cola-component-rpc/README.md) | `...-starter` | Protobuf/gRPC Provider、Consumer、DDC 注册发现、Deadline 和 Gateway 通道。 |
| [Rule Engine](egon-cola-components/egon-cola-component-rule-engine/README.md) | `...-starter` | Java 规则链、单例责任链、规则树、trace、限制和监听器。 |
| [Access Guard](egon-cola-components/egon-cola-component-access-guard/README.md) | `...-starter` | 方法级白名单、黑名单、限流、超时和拒绝治理。 |
| [Method Extension](egon-cola-components/egon-cola-component-method-extension/README.md) | `...-starter` | 在注解方法前执行 AOP 或 Agent 业务决策 Handler。 |
| [Transactional Outbox](egon-cola-components/egon-cola-component-transactional-outbox/README.md) | `...-starter` | 基于 PostgreSQL/JDBC 的至少一次 HTTP、RabbitMQ 或自定义 Handler 投递。 |
| [Bytecode](egon-cola-components/egon-cola-component-bytecode/README.md) | API、bridge、runtime、Agent、starter | 构建期架构检查，以及可选的 executor、观测、Method Extension 和 Access Guard 增强。 |
| [Gateway](egon-cola-components/egon-cola-component-gateway/README.md) | Engine、Admin、Starter、Provider Runtime | HTTP/RPC 数据面、规则发布、Provider 发现、安全、可观测和部署资产。 |
| [Components BOM](egon-cola-components/egon-cola-components-bom/README.md) | `egon-cola-components-bom` | 公共组件消费 Artifact 的集中版本管理。 |

运行时 starter-style 组件推荐结构：

```text
egon-cola-component-xxx
├── pom.xml
├── egon-cola-component-xxx-starter   # 业务系统直接引入
├── egon-cola-component-xxx-test      # 测试工程 / 示例工程
└── egon-cola-component-xxx-admin     # 可选，后端管理服务
```

组件约束：

- `egon-cola-component-common` 是 common 聚合 POM，业务系统按需依赖 `egon-cola-component-common-core`、`egon-cola-component-common-result` 等具体 Jar。
- 除 `common` 这类纯 Jar 基础组件外，运行时 starter-style 组件应由业务系统直接引入 `starter`。
- `starter` 不反向依赖 `admin`、`test`、`ui`。
- `test` 只用于组件自测、集成测试和示例启动。
- `admin` 可选，如果存在，应可以独立部署。
- 组件工程不放 UI，UI 放到独立前端仓库统一维护。

Gateway Admin Web 是 Maven 组件布局之外的例外：它是与 Gateway 源码同目录的私有
React 应用，使用 npm 构建。Gateway 的部署、前端和性能说明均从 Gateway README 进入。

## BOM 使用

业务系统可以通过 BOM 统一管理组件版本：

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

然后按需引入组件：

```xml

<dependencies>
    <dependency>
        <groupId>top.egon</groupId>
        <artifactId>egon-cola-component-common-core</artifactId>
    </dependency>
    <dependency>
        <groupId>top.egon</groupId>
        <artifactId>egon-cola-component-transactional-outbox-starter</artifactId>
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

Fast CI 使用 `.github/workflows/ci.yaml`，由 GitHub-hosted Ubuntu runner 调度，并在 Rocky Linux 10 容器内分别使用 JDK 21 与 JDK 25 执行：

```bash
./mvnw -V --no-transfer-progress -DtrimStackTrace=false clean install
```

Strong CI 使用 `.github/workflows/ci_java_compatibility.yaml`，由 GitHub-hosted Ubuntu runner 调度，在 Rocky Linux 10 容器内分别使用 JDK 21 与 JDK 25 执行 `clean install` 并验证三类 archetype 生成项目，最后在宿主 runner 构建 Docker 镜像：

```bash
./mvnw -B -ntp clean install
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

| 文档 | 说明 |
|---|---|
| [egon-cola-archetypes/code-style-abstract.md](egon-cola-archetypes/code-style-abstract.md) | 大型单体轻量领域分层架构代码风格。 |
| [egon-cola-archetypes/architecture-mermaid-diagrams.md](egon-cola-archetypes/architecture-mermaid-diagrams.md) | 分层依赖、调用链路、架构边界 Mermaid 图。 |
| [egon-cola-archetypes/egon-cola-archetype-light/large-monolith-light-domain-architecture.md](egon-cola-archetypes/egon-cola-archetype-light/large-monolith-light-domain-architecture.md) | light archetype 架构说明。 |
| [egon-cola-archetypes/egon-cola-archetype-service/student-management-service-only-rpc-mq-architecture.md](egon-cola-archetypes/egon-cola-archetype-service/student-management-service-only-rpc-mq-architecture.md) | service archetype 架构说明。 |
| [egon-cola-archetypes/egon-cola-archetype-web/multi-project-multi-module-architecture.md](egon-cola-archetypes/egon-cola-archetype-web/multi-project-multi-module-architecture.md) | web archetype 架构说明。 |
| [egon-cola-components/egon-cola-components-architecture.md](egon-cola-components/egon-cola-components-architecture.md) | 多组件工程结构规范。 |
| [egon-cola-components/egon-cola-components-bom/README.md](egon-cola-components/egon-cola-components-bom/README.md) | 公共组件版本和导出边界。 |
| [egon-cola-components/egon-cola-component-dynamic-config-center/README.md](egon-cola-components/egon-cola-component-dynamic-config-center/README.md) | 动态配置、租约、注册发现和发布协议。 |
| [egon-cola-components/egon-cola-component-rpc/README.md](egon-cola-components/egon-cola-component-rpc/README.md) | Protobuf/gRPC Provider 与 Consumer 契约。 |
| [egon-cola-components/egon-cola-component-gateway/README.md](egon-cola-components/egon-cola-component-gateway/README.md) | HTTP/RPC Gateway 平台和部署入口。 |
| [egon-cola-components/egon-cola-component-transactional-outbox/README.md](egon-cola-components/egon-cola-component-transactional-outbox/README.md) | PostgreSQL/JDBC 事务消息使用方式与保证。 |
| [scripts/maven-deploy.md](scripts/maven-deploy.md) | Maven Central 发布操作说明。 |

## 项目来源

Egon-COLA 最初 fork 自 [alibaba/COLA](https://github.com/alibaba/COLA)。

当前仓库作为独立架构项目维护。项目有意解除与原始 fork 的同步关系，避免误同步上游并保持独立的发展方向。

## 许可证

本项目采用 MIT License 和 GNU Lesser General Public License v2.1 双重许可。

你可以选择任一许可证：

- MIT License，见 [LICENSE-MIT](LICENSE-MIT)。
- GNU LGPL v2.1，见 [LICENSE-LGPL-2.1](LICENSE-LGPL-2.1)。
