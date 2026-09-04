# egon-cola-source-agent

[English](README.md) | [中文](README.zh-CN.md)

This internal source reactor is the Deep Research reference project for `egon-cola-archetype-agent`. It follows the six-module non-open Web profile while keeping one `research` business domain. The source project is a build input, not a public Central artifact.

```text
common -> domain -> application -> adapter -> starter
             \-> infrastructure -> starter
```

The project consumes `egon-cola-component-agent-flow-starter` for Agent Flow execution. Provider configuration, MCP credentials, and API secrets are supplied by environment-backed Starter configuration. It intentionally has no database, Flyway, Redis, MQ, RPC, GraphQL, UI, history, or recovery API.

The source reactor is verified with Java 21, Spring Boot 3.5.16, Spring AI 1.1.8, Springdoc 2.8.17, and Google ADK 0.7.0 through the Components starter. Only the generated `egon-cola-archetype-agent` definition becomes a public Archetype family.
