# egon-cola-source-agent

[English](README.md) | [中文](README.zh-CN.md)

这是 `egon-cola-archetype-agent` 的内部 Deep Research source reactor。它采用 Web non-open 六模块结构，但只保留一个 `research` 业务域。source project 只是构建输入，不是发布到 Central 的公共 artifact。

```text
common -> domain -> application -> adapter -> starter
             \-> infrastructure -> starter
```

项目通过 `egon-cola-component-agent-flow-starter` 消费通用 Agent Flow 能力。provider 配置、MCP 凭据和 API secret 由 Starter 的环境配置提供。项目明确不包含 database、Flyway、Redis、MQ、RPC、GraphQL、UI、历史、恢复 API。

source reactor 使用 Java 21、Spring Boot 3.5.16、Spring AI 1.1.8、Springdoc 2.8.17，并通过 Components starter 使用 Google ADK 0.7.0。只有生成后的 `egon-cola-archetype-agent` definition 会成为公共 Archetype family。
