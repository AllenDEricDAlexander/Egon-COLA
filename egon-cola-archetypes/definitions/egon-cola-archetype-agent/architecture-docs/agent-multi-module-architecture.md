# Deep Research Agent Archetype Architecture

This archetype is the Agent-specific application of the Egon-COLA Web six-module
profile. The generated project has two business capabilities: Deep Research, which runs
entirely in process, and Knowledge, which stores documents, embeds them into PostgreSQL
with the `vector` extension, and answers questions from what it stored. Beyond the
knowledge tables the project has no cache, message broker, RPC, GraphQL, UI, status,
history, resume, or durable recovery surface.

```text
common <- domain <- application <- adapter <- starter
                         ^             ^
                         |             |
                    infrastructure --+
```

The `domain` module owns research records, events, ports, and service contracts, and the
knowledge vocabulary: bases, documents, chunks, the ingest lifecycle, and the retrieval
and answer ports. `application` owns the `DeepResearchManage` facade, validation, the
process-local capacity bulkheads, deadline and terminal cleanup, and the knowledge use
cases. `infrastructure` adapts the domain gateway to the shared Agent Flow component,
persists the knowledge tables, and assembles the embedding model, the vector store and
the ingest delivery handler. `adapter` owns the API-key, trace and tenant filters, JSON
boundary, error contract, the only `POST /api/v1/deep-research/runs` SSE command, and the
twelve knowledge operations of `/api/v1/knowledge-bases` and `/api/v1/knowledge-documents`.
`starter` owns Spring Boot, Spring AI model configuration, OpenAPI metadata, the fixed
flow graph, the datasource and the migrations.

The research request cannot choose a provider, model, tool, prompt, or flow; the
knowledge request cannot choose a collection, an embedding model or a tenant. Each
accepted research request gets one isolated Agent Flow session and a process-local run.
Events are ordered by `runId:sequence`; `research.completed` or `research.failed` is the
single public terminal event, and a knowledge answer stream ends with
`knowledge.completed` or `knowledge.failed` the same way. A disconnected client cancels
the run or the answer and releases the session, subscription and capacity lease, and the
answer's generation with it. Model, MCP and document output is untrusted; report Markdown
and retrieved text must be sanitized before rendering.

The knowledge domain owns three tables — `knowledge_base`, `knowledge_document` and the
outbox queue `egon_cola_outbox_message` — all created by this project's Flyway
migrations. The vector table is not one of them: the `knowledgeRagVectorStore` bean
creates it on first start-up with the dimension the component validates the embedding
model against, so no migration can disagree with the store. Both knowledge tables carry a
tenant column, and the tenant currently comes from `agent.knowledge.tenant.default-id`
through the request MDC; it is a fixed value rather than an isolation boundary, and
authentication is out of scope. Deployment therefore needs a PostgreSQL database where
the application role may run `CREATE EXTENSION vector`, and the `spring.datasource` keys
the four profiles declare.

The normal source project is the business truth. This definition is only the Maven
packaging contract, and `.generated` is an ignored derived reactor. Generated projects
must be verified offline with fake model/tool dependencies and must not receive
credentials or provider-specific endpoints from the archetype.

## Dependency and runtime ownership

Generated projects inherit the released `top.egon:egon-cola-archetypes-parent` at a concrete version with an empty `relativePath`. The parent imports the Components BOM, manages Common dependencies and ShardingSphere 5.5.3, and keeps Commons Lang at 3.20.0. Consumer modules inherit their own project root. Install the matching parent/BOM and required artifacts locally before validating an unpublished release; a local install does not publish artifacts.

Agent retains its six-module Deep Research application with Spring AI 1.1.8, Agent Flow and Google ADK, and consumes the RAG, outbox and MyBatis-Plus components for the knowledge domain. It does not gain Egon RPC/Tianshu, Nacos, Dubbo or ShardingSphere runtime dependencies. Google ADK may independently bring gRPC/Protobuf; BOM management alone is not a runtime dependency.
