# egon-cola-source-agent

[English](README.md) | [中文](README.zh-CN.md)

This internal source reactor is the Deep Research reference project for `egon-cola-archetype-agent`. It follows the six-module non-open Web profile while keeping two business domains: `research`, which runs in process, and `knowledge`, which stores documents in PostgreSQL, embeds them and answers questions from them. The source project is a build input, not a public Central artifact.

```text
common -> domain -> application -> adapter -> starter
             \-> infrastructure -> starter
```

The project consumes `egon-cola-component-agent-flow-starter` for Agent Flow execution, and the RAG, transactional-outbox and MyBatis-Plus components for the knowledge domain. Provider configuration, MCP credentials, database credentials, and API secrets are supplied by environment-backed Starter configuration. It has no cache, MQ, RPC, GraphQL, UI, history, or recovery API beyond the knowledge tables.

The source reactor is verified with Java 21, Spring Boot 3.5.16, Spring AI 1.1.8, Springdoc 2.8.17, and Google ADK 0.7.0 through the Components starter. Only the generated `egon-cola-archetype-agent` definition becomes a public Archetype family.

## Maven Profiles And External Launch Arguments

Use one environment profile at a time: `dev`, `test`, or `prod`; omitted profiles use
`dev` defaults. These profiles configure application startup, while Surefire keeps
its existing `test` profile. JVM heap sizes are examples to tune for the deployment.

From the project root (install sibling modules first for a multi-module project):

```bash
mvn install
mvn -pl egon-cola-source-agent-starter -Pdev spring-boot:run
mvn -Pprod -Drun.jvm-args="-Xms1g -Xmx2g" \
  -Drun.server-port=8080 \
  -Drun.config-location=file:/etc/myapp/override.yml package
```

Maven generates `egon-cola-source-agent-starter/target/launch.args` during `process-resources`, including when
packaging. Switching profiles or `-Drun.*` values rewrites it even without `clean`.
Only `src/main/launch/launch.args` is filtered by this execution using `@...@`;
existing YAML placeholders remain runtime values. The argument file stays outside the JAR.

Deploy the executable JAR and `launch.args` together, optionally renaming the JAR to
`app.jar`, then launch from the deployment directory:

```bash
java @launch.args -jar app.jar
java @launch.args -Xmx3g -jar app.jar --server.port=9080
```

The file records JVM arguments, the Spring profile, port, and additional configuration
location. These explicit system properties take precedence over corresponding environment
variables; `-Drun.*` overrides Maven defaults, JVM options go before `-jar`, and Spring
command-line overrides go after the JAR. Plain `java -jar` and IDE main-class runs do not
read this file automatically. Relative configuration paths use the launch working directory,
not the argument file directory; use an absolute `file:` path for deployment and forward
slashes for Windows paths. Extra configuration supplements `application.yml` and the
selected `application-{profile}.yml`; omit `optional:` when the file must exist. Keep
credentials in the existing environment/secrets mechanism, never in `run.*` properties.

## Runtime contract

The only public operation is `POST /api/v1/deep-research/runs`. It consumes JSON and produces `text/event-stream`. The request body accepts `topic`, optional `reportLanguage` (`ZH_CN` or `EN_US`, default `ZH_CN`), and optional `maxSources` (default `8`, bounded to `3..20`). The `X-Research-Api-Key` header is required; `X-Trace-Id` is optional and is generated when absent.

Each accepted request creates one process-local Agent Flow session and one non-idempotent run. Events are ordered by `runId:sequence` and use `research.started`, `research.progress`, `research.completed`, or `research.failed`. A completed or failed event is the sole public terminal event. Reconnecting creates a new run; there is no status, history, resume, or durable recovery API. A disconnected client cancels the run and releases the session and capacity lease.

The fixed flow is `Planner -> ParallelResearch -> Writer`; the parallel node contains `EvidenceResearcher`, `CounterpointResearcher`, and `FreshnessResearcher`. Search callbacks come from one configured MCP SSE service. Model and MCP clients are never selected by the request.

## Knowledge contract

The knowledge domain adds twelve operations under `/api/v1/knowledge-bases` and `/api/v1/knowledge-documents`. They use the same `X-Research-Api-Key` header and the same optional `X-Trace-Id` as research, and they answer their failures with the same JSON error body — including the answer endpoint, whose success is an event stream.

| Method and path | Purpose |
| --- | --- |
| `POST /api/v1/knowledge-bases` | Create a base: code, name, embedding model, chunking |
| `GET /api/v1/knowledge-bases` | Page the bases |
| `GET /api/v1/knowledge-bases/{knowledgeBaseId}` | Read one base |
| `PUT /api/v1/knowledge-bases/{knowledgeBaseId}` | Update name, description or chunking |
| `DELETE /api/v1/knowledge-bases/{knowledgeBaseId}` | Delete the base with its documents and chunks |
| `POST /api/v1/knowledge-bases/{knowledgeBaseId}/documents` | Upload one file as `multipart/form-data` (`file`, optional `displayName`) |
| `GET /api/v1/knowledge-bases/{knowledgeBaseId}/documents` | Page and filter the documents of a base |
| `GET /api/v1/knowledge-documents/{documentId}` | Read the status and diagnostics of one document |
| `DELETE /api/v1/knowledge-documents/{documentId}` | Delete one document with its stored text and chunks |
| `POST /api/v1/knowledge-documents/{documentId}/reingest` | Queue another attempt from the stored text |
| `POST /api/v1/knowledge-bases/{knowledgeBaseId}/retrieve` | Embed a query and return the scored chunks; no chat model is called |
| `POST /api/v1/knowledge-bases/{knowledgeBaseId}/chat` | Stream one answer as server-sent events |

`GET /v3/api-docs` publishes each operation with its request and response schemas and is the reference for field constraints. Two behaviours are worth stating here because no schema shows them: an answer stream requires `Accept: text/event-stream` and is answered `406` otherwise, and no request names a collection, an embedding model or a tenant — the base's stored model decides what is read and written.

Retrieval takes `query` (required, trimmed, at most 2000 characters), `topK` (1 to 50, defaulting to the component's configured default of 8) and `similarityThreshold` (0.0 to 1.0). The answer stream takes `question` (required, trimmed, at most 2000 characters, no control characters) and `topK`, and emits `knowledge.started` with the retrieved references, zero or more `knowledge.progress` increments carrying `delta`, and exactly one `knowledge.completed` or `knowledge.failed` as its last event. An upload is observed by polling the document.

## Knowledge ingest states

`DocumentIngestStatusEnum` mirrors the outbox states of a queued ingest; the table below is the state machine, and a transition that is not in it is rejected.

| Current | Event | Next | Guard |
| --- | --- | --- | --- |
| (none) | upload committed | `PENDING` | file stored, text in the database, outbox row added |
| `PENDING` | claimed by a delivery | `PROCESSING` | a pending record exists |
| `PROCESSING` | ingest succeeded | `SUCCEEDED` | vectors written, `chunkCount` written back |
| `PROCESSING` | ingest failed, attempts left | `PENDING` | `attempt_count < max_attempts`; the outbox message waits as `RETRY_WAIT` |
| `PROCESSING` | ingest failed, attempts exhausted | `DEAD` | no attempts left; the message becomes a dead letter |
| `SUCCEEDED`, `FAILED`, `DEAD` | reprocessed through `reingest` | `PENDING` | stored text is non-empty; failure fields cleared, attempts reset |

A document in a non-terminal state is refused a reprocess with `409`. A retried delivery never re-parses the original: it re-chunks and re-embeds the text already stored in `knowledge_document.content`.

## Knowledge configuration

| Key | Environment variable | Meaning |
| --- | --- | --- |
| `spring.datasource.url` / `username` / `password` | `AGENT_DB_URL`, `AGENT_DB_USERNAME`, `AGENT_DB_PASSWORD` | The PostgreSQL database; `dev` and `prod` require them |
| `spring.flyway.enabled`, `spring.flyway.locations` | `AGENT_FLYWAY_ENABLED` | Migrations in `classpath:db/migration`; the test profile disables them |
| `spring.servlet.multipart.max-file-size` / `max-request-size` | `AGENT_MULTIPART_MAX_FILE_SIZE`, `AGENT_MULTIPART_MAX_REQUEST_SIZE` | Container guard above the API limit (25 MB / 26 MB by default) |
| `egon.cola.component.rag.*` | `AGENT_RAG_STORAGE_ROOT` | Dimensions, the named vector store and embedding model, the storage root, retrieval limits (default 8, maximum 50) |
| `egon.cola.component.transactional-outbox.enabled` | — | The ingest queue and its delivery |
| `egon.cola.component.mybatis-plus.tenant-id` | — | The tenant column, filled from the MDC on every statement |
| `agent.knowledge.embedding.base-url` / `api-key` / `model-name` | `AGENT_KNOWLEDGE_EMBEDDING_BASE_URL`, `AGENT_KNOWLEDGE_EMBEDDING_API_KEY`, `AGENT_KNOWLEDGE_EMBEDDING_MODEL` | The embedding provider; `dev` and `prod` require them |
| `agent.knowledge.tenant.default-id` | `AGENT_KNOWLEDGE_TENANT_ID` | The tenant every call runs as, `0` by default |
| `agent.knowledge.runtime.qa-max-concurrent` | `AGENT_KNOWLEDGE_QA_MAX_CONCURRENT` | Answer streams in flight at once (4 by default) |
| `agent.knowledge.runtime.max-upload-bytes` | `AGENT_KNOWLEDGE_MAX_UPLOAD_BYTES` | The upload limit the API enforces (20 MB by default) |
| `agent.knowledge.runtime.max-documents-per-base` | `AGENT_KNOWLEDGE_MAX_DOCUMENTS_PER_BASE` | Documents one base may hold (10000 by default) |
| `agent.knowledge.runtime.qa-max-duration` | `AGENT_KNOWLEDGE_QA_MAX_DURATION` | How long one answer may stream (PT2M by default) |

A deployment of the knowledge domain has four prerequisites that no configuration key can satisfy:

1. The database must be PostgreSQL with the `vector` extension available to the application role — the first migration runs `CREATE EXTENSION vector`.
2. The vector table is created by the `knowledgeRagVectorStore` bean on first start-up, not by a migration, so the table's dimension cannot drift from the embedding model the component validates.
3. The tenant is a fixed value (`agent.knowledge.tenant.default-id`, `0` by default); it is not an isolation boundary, and authentication and tenant resolution are out of scope for this archetype.
4. Knowledge answers and research runs share one API key and the same process-local capacity model, each with its own pool: saturating one does not refuse the other, and neither survives a restart.

## Environment contract

Development and production require externally supplied values for `DEEP_RESEARCH_MODEL_BASE_URL`, `DEEP_RESEARCH_MODEL_API_KEY`, `DEEP_RESEARCH_MODEL_NAME`, `DEEP_RESEARCH_API_KEY`, `DEEP_RESEARCH_MCP_BASE_URI`, `DEEP_RESEARCH_MCP_SSE_ENDPOINT`, and `DEEP_RESEARCH_MCP_API_KEY`, and for the knowledge domain the `AGENT_DB_*`, `AGENT_FLYWAY_ENABLED` and `AGENT_KNOWLEDGE_EMBEDDING_*` values listed above. Runtime limits use the `DEEP_RESEARCH_MAX_*`, `DEEP_RESEARCH_HEARTBEAT_INTERVAL` and `AGENT_KNOWLEDGE_*` variables. No credential, provider URL, or usable secret is committed in this source project. The test profile uses a fake `ChatModel` and fake `ToolCallback`, a closed RAG component and no Flyway run, so it never opens a network connection or touches PostgreSQL.

Example request (the shell expands the externally managed key):

```bash
curl --fail-with-body --no-buffer \
  -H "X-Research-Api-Key: ${DEEP_RESEARCH_API_KEY}" \
  -H "Accept: text/event-stream" \
  -H "Content-Type: application/json" \
  -d '{"topic":"agent architecture","reportLanguage":"EN_US","maxSources":8}' \
  http://localhost:8080/api/v1/deep-research/runs
```

`reportMarkdown` is untrusted model output and must be sanitized before rendering. The server does not retry paid model or MCP calls; a retry is a new billable run. The in-memory session and report disappear on process restart. Production API documentation is disabled; TLS, proxy headers, and secret delivery belong to deployment.

## Verification

Run the source checks without starting a service:

```bash
./mvnw -B -ntp -f egon-cola-source-agent/pom.xml clean verify
```

The generated family is validated separately by the repository archetype scripts. The source project contains no cache, message broker, RPC, GraphQL endpoint, or UI; its only durable surface is the knowledge schema with its migrations and the outbox queue.
