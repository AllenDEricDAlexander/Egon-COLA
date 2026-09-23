# Egon-COLA Components

[English](README.md) | [中文](README.zh-CN.md)

`egon-cola-components` 是可复用 Java 库和 Spring Boot Starter 的 Maven Reactor。每项运行时能力都可以独立测试，并通过 [Components BOM](egon-cola-components-bom/README.zh-CN.md) 发布和消费。

## 组件入口

| 组件 | 能力 | 消费文档 |
|---|---|---|
| Access Guard | 基于规则的准入控制，通过 Spring AOP 保护业务方法执行 | [Access Guard Starter](egon-cola-component-access-guard-starter/README.zh-CN.md) |
| Agent Flow | 扁平化的 Spring AI + Google ADK 配置驱动 Flow 编译、in-memory Session、同步事件和流式执行 | [Agent Flow Starter](egon-cola-component-agent-flow-starter/README.zh-CN.md) |
| Bytecode | ASM 架构校验，以及运行时 executor 增强、方法观测和方法扩展增强 | [Bytecode](egon-cola-component-bytecode/README.zh-CN.md) |
| Code Generator | 离线开发工具，读取 PostgreSQL DDL 或 MyBatis-Plus/Sharding-JDBC manifest 生成后端 CRUD | [Code Generator](egon-cola-component-code-generator/README.zh-CN.md) |
| Common | 共享契约和基础工具模块 | [Common](egon-cola-component-common/README.zh-CN.md) |
| Dynamic Thread Pool | 运行时执行器管理 | [Dynamic Thread Pool](egon-cola-component-dynamic-thread-pool/README.zh-CN.md) |
| Method Extension | 在注解方法执行前运行一个业务自定义决策 Handler | [Method Extension](egon-cola-component-method-extension/README.zh-CN.md) |
| RAG | 文档抽取、切片、向量化和相似度检索机制 | [RAG Starter](egon-cola-component-rag-starter/README.zh-CN.md) |
| RPC | Protobuf/gRPC 与 Tianshu 集成 | [RPC](egon-cola-component-rpc/README.zh-CN.md) |
| Rule Engine | Java 规则链、规则树和执行控制 | [Rule Engine](egon-cola-component-rule-engine-starter/README.zh-CN.md) |
| Transactional Outbox | 在本地 PostgreSQL 事务内存储出站消息，异步投递并通过轮询恢复 | [Transactional Outbox Starter](egon-cola-component-transactional-outbox-starter/README.zh-CN.md) |

上述模块都是 Reactor 模块，但并非每个都是依赖入口。Access Guard、Agent Flow、Bytecode、Common、Dynamic Thread Pool、Method Extension、RAG、RPC、Rule Engine、Transactional Outbox 都提供由 BOM 管理版本的运行时 artifact；Code Generator 是构建期工具，没有运行时 artifact。

Agent Flow starter 默认关闭，采用扁平组件结构。它负责配置、ADK 图编译、Registry/Session 执行、cancel、timeout 和有界 close；宿主负责 provider `ChatModel`、凭据、工具、MCP、HTTP、认证，以及 database 等持久化边界。

## BOM

导入 [`egon-cola-components-bom`](egon-cola-components-bom/README.zh-CN.md) 后，可以无重复版本地消费 `top.egon:egon-cola-component-agent-flow-starter` 和其他已发布组件入口。BOM 管理 22 个 Egon 运行时 artifact，并固定这些 artifact 暴露给消费者的第三方版本（`org.apache.commons:commons-lang3` 和 Redisson artifact）。provider 库、ADK 内部依赖、admin 应用、test 模块、聚合 POM 和离线的 Code Generator 不进入公共依赖清单。

## 架构

新增模块前请阅读 [Components 架构规范](egon-cola-components-architecture.md)。文档区分常规 starter/admin/test 结构与获批准的扁平 Agent Flow starter，并记录 Java 21、Spring Boot 3.5.16、Spring AI 1.1.8 和 Google ADK 0.7.0 的兼容线。
