# Egon-COLA Agent Archetype

This classifier documents the `egon-cola-archetype-agent` Maven Archetype distribution.
It generates a Java 21, Spring Boot six-module Deep Research Agent project with a fixed
Agent Flow and one authenticated Server-Sent Events command. The generated project is
process-local and intentionally has no durable state or infrastructure-specific
defaults; model and MCP credentials are supplied by the consuming deployment.

## Dependency and runtime ownership

Generated projects inherit the released `top.egon:egon-cola-archetypes-parent` at a concrete version with an empty `relativePath`. The parent imports the Components BOM, manages Common dependencies and ShardingSphere 5.5.3, and keeps Commons Lang at 3.20.0. Consumer modules inherit their own project root. Install the matching parent/BOM and required artifacts locally before validating an unpublished release; a local install does not publish artifacts.

Agent retains its six-module Deep Research application with Spring AI 1.1.8, Agent Flow and Google ADK. It does not gain Egon RPC/Tianshu, Nacos, Dubbo or ShardingSphere runtime dependencies. Google ADK may independently bring gRPC/Protobuf; BOM management alone is not a runtime dependency.
