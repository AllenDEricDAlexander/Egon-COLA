# Egon-COLA Components

[English](README.md) | [中文](README.zh-CN.md)

`egon-cola-components` 是可复用 Java 库和 Spring Boot Starter 的 Maven Reactor。每项运行时能力都可以独立测试，并通过 [Components BOM](egon-cola-components-bom/README.zh-CN.md) 发布和消费。

## 组件入口

| 组件 | 能力 | 消费文档 |
|---|---|---|
| Agent Flow | 扁平化的 Spring AI + Google ADK 配置驱动 Flow 编译、in-memory Session、同步事件和流式执行 | [Agent Flow Starter](egon-cola-component-agent-flow-starter/README.zh-CN.md) |
| Common | 共享契约和基础工具模块 | [Common](egon-cola-component-common/README.zh-CN.md) |
| Rule Engine | Java 规则链、规则树和执行控制 | [Rule Engine](egon-cola-component-rule-engine-starter/README.zh-CN.md) |
| Dynamic Thread Pool | 运行时执行器管理 | [Dynamic Thread Pool](egon-cola-component-dynamic-thread-pool/README.zh-CN.md) |
| RPC | Protobuf/gRPC 与 DDC 集成 | [RPC](egon-cola-component-rpc/README.zh-CN.md) |

Agent Flow starter 默认关闭，采用扁平组件结构。它负责配置、ADK 图编译、Registry/Session 执行、cancel、timeout 和有界 close；宿主负责 provider `ChatModel`、凭据、工具、MCP、HTTP、认证，以及 database 等持久化边界。

## BOM

导入 [`egon-cola-components-bom`](egon-cola-components-bom/README.zh-CN.md) 后，可以无重复版本地消费 `top.egon:egon-cola-component-agent-flow-starter` 和其他已发布组件入口。BOM 只导出 Egon 运行时 artifact；provider 库、ADK 内部依赖、admin 应用、test 模块和聚合 POM 不进入公共依赖清单。

## 架构

新增模块前请阅读 [Components 架构规范](egon-cola-components-architecture.md)。文档区分常规 starter/admin/test 结构与获批准的扁平 Agent Flow starter，并记录 Java 21、Spring Boot 3.5.16、Spring AI 1.1.8 和 Google ADK 0.7.0 的兼容线。
