# Deep Research Agent Archetype Architecture

This archetype is the Agent-specific application of the Egon-COLA Agent six-module
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
cases. `infrastructure` implements the domain service ports against the shared Agent Flow
component,
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
outbox queue `egon_cola_outbox_message` — historically created by this project's legacy migrations. The required target is now
MP-SDJ distributed managed DDL; existing POM/config still needs an implementation migration. The vector table is not one of them: the `knowledgeRagVectorStore` bean
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

## Java / CQE maintenance standard

Ordinary entities use class; only immutable value objects may use record. Choose @Builder for ordinary construction and @SuperBuilder for compatible inheritance construction, never mechanically replace one with the other. Persistence enums use @EnumValue and frontend enums use @JsonValue. Reuse Components, Common MP repositories, annotation-driven validation and the existing Knowledge transactional outbox. Research/answer SSE is a response stream, not proof of durable CQE business-event delivery. Soft-deletable business uniqueness must include business columns + deleted_at and active-row NULL enforcement; current SQL must be reviewed separately before any corrective migration. Applied SQL/history stays immutable; no new Flyway integration is allowed.

## 分布式 DDL 管理约束

统一复用 `egon-mp-sdj-ext-starter` 对应的 `egon-cola-component-common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter`，不再选用 Flyway。使用组件 `EgonColaPostgreDdlRunner`、显式物理 PRIMARY/schema/role 目标、SQL 版本与 SHA-256 Manifest；数据源与拓扑由 Starter 管理，业务项目不得复制本地 Bootstrapper 或并行使用 MP 默认 IDdl/DdlApplicationRunner。

schema advisory transaction lock 协调多实例；每份 SQL 与 ddl_history 同连接同事务提交。跨物理目标不是全局原子事务，后续目标失败不撤销前面已提交目标；必须设计续跑与幂等。未知提交结果先用新连接核实再决定重试。检查锁/语句/拓扑超时、脚本前缀、checksum 与路由指纹；非空未受管库或漂移不能自动 DROP、repair 或接管历史。

一个逻辑变更新增一个下一版本 SQL 和 Manifest 条目，不改写已应用 SQL/history。配置基于 `egon.cola.component.mybatis-plus.ddl` 与现有 profile；实际接线及真实 PostgreSQL 并发/失败恢复需要独立验证，文档不声称已完成运行时迁移。
