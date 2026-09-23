# Egon-COLA Agent Archetype

This classifier documents the `egon-cola-archetype-agent` Maven Archetype distribution.
It generates a Java 21, Spring Boot six-module Agent project with two business domains. `research` runs a fixed Agent Flow behind one authenticated Server-Sent Events command and keeps its sessions process-local. `knowledge` is durable: it stores documents in PostgreSQL, embeds them into a pgvector store, and answers questions from the retrieved chunks through twelve additional authenticated operations. Model, MCP, embedding and database credentials are supplied by the consuming deployment; the archetype commits none.

## Dependency and runtime ownership

Generated projects inherit the released `top.egon:egon-cola-archetypes-parent` at a concrete version with an empty `relativePath`. The parent imports the Components BOM, manages Common dependencies and ShardingSphere 5.5.3, and keeps Commons Lang at 3.20.0. Consumer modules inherit their own project root. Install the matching parent/BOM and required artifacts locally before validating an unpublished release; a local install does not publish artifacts.

Agent retains its six-module application with Spring AI 1.1.8, Agent Flow and Google ADK. Its runtime libraries include the Agent Flow, RAG, transactional-outbox and two-level cache starters, `spring-ai-pgvector-store`, the Common MyBatis-Plus ShardingSphere-JDBC extension starter (which brings ShardingSphere transitively, and is the only permitted route to it), Flyway, and the PostgreSQL driver. What Agent does not gain is Egon RPC/Tianshu, Nacos, Dubbo, or the organization/evaluation facades; the generated project runs as a single HTTP service. Google ADK may independently bring gRPC/Protobuf; BOM management alone is not a runtime dependency.
