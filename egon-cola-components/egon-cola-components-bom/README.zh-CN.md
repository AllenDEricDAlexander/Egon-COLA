# egon-cola-components-bom

[English](README.md) | 中文

## 简要介绍

`egon-cola-components-bom` 是 Egon COLA 组件体系的 Maven BOM。它不提供运行时代码，只负责统一管理 `egon-cola-components` 下可被业务应用直接依赖的组件版本，避免业务工程在每个组件依赖上重复写版本号。

BOM 当前管理 22 个 `top.egon` 运行时 artifact：common core 与工具模块、各业务组件的 starter（含 RAG 和扁平化 Agent Flow starter）、RPC 的 Tianshu 适配器，以及字节码组件的公开 API、桥接层、运行时、Agent 和 starter。除此之外，它还固定组件 API 暴露给消费者的第三方版本：`org.apache.commons:commons-lang3` 和 Redisson artifact。平台、admin、test、构建期工具和聚合 POM 不作为业务依赖入口导出。

## 功能说明

### 统一版本管理

业务应用通过 `dependencyManagement` import BOM 后，后续声明组件依赖时不需要再写 `<version>`。所有 Egon 组件版本跟随 BOM 的 `project.version`，当前为 `5.4.1`。三个第三方条目使用各自的固定版本，而不是 `${project.version}`：`commons-lang3` 为 `3.20.0`，两个 Redisson artifact 为 `3.26.0`。

### 导出的依赖清单

| Artifact | 用途 |
|---|---|
| `egon-cola-component-common-core` | 错误状态、异常、枚举契约、请求/结果模型和树结构构建 |
| `egon-cola-component-common-trace` | 纯 JDK + SLF4J `TraceContext`、MDC 投影、W3C `traceparent` 解析和本地线程任务模板 |
| `egon-cola-component-common-trace-spring-boot-starter` | common 聚合内的 Spring Boot 日志关联和 Trace 传播 Starter |
| `egon-cola-component-common-id-starter` | 纯 JDK Snowflake 契约与算法，以及面向数据库 `BIGINT` ID 的 Spring Boot 自动配置 |
| `egon-cola-component-common-crypto` | 摘要、HMAC、Base64、Hex |
| `egon-cola-component-common-data-desensitize-spring-boot-starter` | 基于共享策略的 Jackson 响应与 Logback 消息脱敏 |
| `egon-cola-component-common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter` | MyBatis-Plus 与 ShardingSphere-JDBC 契约：`EgonModel`、Mapper XML 语句、本地写入守卫和分片拓扑校验 |
| `egon-cola-component-common-cache-spring-boot-starter` | Guava L1 + Redisson `RMapCache` L2 两级缓存，支持租户维度 Key 和跨节点失效 |
| `egon-cola-component-dynamic-thread-pool-starter` | 动态线程池业务侧 starter |
| `egon-cola-component-rpc-starter` | Protobuf/gRPC Provider 与 Consumer starter |
| `egon-cola-component-rpc-tianshu-adapter` | RPC 组件使用的 Tianshu（DDC）Protobuf 契约、请求签名和注册快照客户端 |
| `egon-cola-component-rule-engine-starter` | 规则引擎 starter |
| `egon-cola-component-agent-flow-starter` | 扁平化 Spring AI + Google ADK Agent Flow starter，提供 in-memory Session 和流式执行 |
| `egon-cola-component-access-guard-starter` | 方法访问治理 starter |
| `egon-cola-component-method-extension-starter` | 方法扩展 starter |
| `egon-cola-component-transactional-outbox-starter` | PostgreSQL/JDBC 事务消息 starter |
| `egon-cola-component-rag-starter` | 文档抽取、切片、向量化和相似度检索 starter |
| `egon-cola-component-bytecode-api` | 字节码能力公共 API |
| `egon-cola-component-bytecode-bridge` | 业务应用与 Agent 之间的桥接层 |
| `egon-cola-component-bytecode-runtime` | 字节码运行时实现 |
| `egon-cola-component-bytecode-agent` | Java Agent 入口 |
| `egon-cola-component-bytecode-starter` | 字节码能力 Spring Boot starter |

### 不导出的模块

| Module | 不导出原因 |
|---|---|
| `egon-cola-component-common` / `-dynamic-thread-pool` / `-rpc` / `-method-extension` / `-bytecode` | 组件聚合 POM，不是业务依赖入口。规则引擎、访问治理、事务消息、Agent Flow 和 RAG 是扁平 starter，没有聚合 POM |
| `egon-cola-component-common-test` / `-dynamic-thread-pool-test` / `-rpc-test` / `-bytecode-test` | 组件样例、生成工程验证和校验模块，不应进入业务运行时 |
| `egon-cola-component-dynamic-thread-pool-admin` | 本 Reactor 唯一的 admin 模块：独立 Spring Boot 服务，应按应用部署，不作为业务依赖 |
| `egon-cola-component-bytecode-core` | ASM 转换和规则引擎实现，通过导出的 API、桥接、运行时和 Agent artifact 使用 |
| `egon-cola-component-bytecode-architecture-maven-plugin` | 构建期 Maven 插件，在 `<build>` 中显式声明 `<version>`，不作为依赖 import |
| `egon-cola-component-bytecode-benchmark` | JMH 基准模块 |
| `egon-cola-component-code-generator` | 离线构建期代码生成工具，没有运行时 artifact |
| `egon-cola-tianshu-*` / `egon-cola-yuheng-*` / `egon-cola-tianquan-*` | 企业级基础设施平台模块归属独立的 `egon-cola-xingyuan` Reactor，它 import 本 BOM，而不是由本 BOM 管理版本 |

## 完整的使用示例

### 1. 在业务工程中导入 BOM

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

### 2. 按需引入组件

```xml
<dependencies>
    <dependency>
        <groupId>top.egon</groupId>
        <artifactId>egon-cola-component-common-core</artifactId>
    </dependency>
    <dependency>
        <groupId>top.egon</groupId>
        <artifactId>egon-cola-component-common-trace</artifactId>
    </dependency>
    <dependency>
        <groupId>top.egon</groupId>
        <artifactId>egon-cola-component-common-trace-spring-boot-starter</artifactId>
    </dependency>
    <dependency>
        <groupId>top.egon</groupId>
        <artifactId>egon-cola-component-common-id-starter</artifactId>
    </dependency>
    <dependency>
        <groupId>top.egon</groupId>
        <artifactId>egon-cola-component-rule-engine-starter</artifactId>
    </dependency>
    <dependency>
        <groupId>top.egon</groupId>
        <artifactId>egon-cola-component-agent-flow-starter</artifactId>
    </dependency>
    <dependency>
        <groupId>top.egon</groupId>
        <artifactId>egon-cola-component-access-guard-starter</artifactId>
    </dependency>
    <dependency>
        <groupId>top.egon</groupId>
        <artifactId>egon-cola-component-rpc-starter</artifactId>
    </dependency>
</dependencies>
```

### 3. 在多模块业务项目中集中声明版本

```xml
<properties>
    <egon-cola.version>5.4.1</egon-cola.version>
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

子模块只声明 artifact：

```xml
<dependency>
    <groupId>top.egon</groupId>
    <artifactId>egon-cola-component-transactional-outbox-starter</artifactId>
</dependency>
```

## 设计思想和实现细节

### 设计思想

1. BOM 只管理消费者真正需要的运行时入口，避免 admin/test/聚合模块被业务误依赖。
2. 稳定 common 契约通过 `common-core` 导出；Trace 核心通过 `common-trace` 导出，Spring 场景通过独立 Trace Starter 接入。
3. 常规业务组件只导出 starter，保持 Spring Boot 自动配置入口明确；规则引擎、访问治理、事务消息、Agent Flow 和 RAG 只存在这一个 starter。字节码组件按公开的 API、桥接、运行时、Agent 和 starter 边界分别管理版本，RPC 因为需要宿主显式装配而额外导出 Tianshu 适配器。
4. Egon 组件版本统一跟随 BOM 自身版本，降低组件组合使用时的版本漂移风险；组件 API 暴露给消费者的第三方坐标保留各自的固定版本。
5. Agent Flow 是扁平化 starter 入口；宿主负责 ChatModel/provider、MCP、HTTP 和 database 边界，BOM 不额外导出这些依赖。

### 实现细节

- `packaging` 为 `pom`，没有源码和运行时类。
- 所有导出组件都在 `<dependencyManagement>` 中声明，版本使用 `${project.version}`；三个第三方条目使用各自的版本属性，因此消费者可以依赖 BOM 提供 cache 和 common 模块暴露的 Redisson、`commons-lang3` 版本。
- release profile 负责源码包、javadoc、GPG 签名和 Central Portal 发布配置。
- `maven-deploy-plugin` 默认 `skip=true`，发布路径由 Central Publishing profile 控制。

## 边界和注意事项

- 业务应用不能只依赖 BOM；BOM 只能放在 `dependencyManagement` 中 import。
- admin 模块需要按独立 Spring Boot 应用构建部署，不通过 BOM 作为业务依赖使用。
- 新增组件时，应优先导出 starter，而不是导出组件聚合 POM 或 test 模块；只有存在明确、独立的消费边界时才导出额外模块。
- Agent Flow 默认关闭且是进程内 `in-memory` 能力；其 README 说明 `executeStream` 的 cancel、timeout 和 `close` 契约。provider 凭据和运行恢复由宿主应用负责。
- 若 common 新增子模块，需要明确它是否是业务运行时稳定入口，再决定是否加入 BOM。
- 构建期工具不是依赖入口。`egon-cola-component-code-generator` 和字节码架构校验 Maven 插件都不进入 BOM，插件版本需要在 `<build>` 中显式声明。

## 验证命令

```bash
./mvnw -B -ntp -pl egon-cola-components/egon-cola-components-bom -am test
```
