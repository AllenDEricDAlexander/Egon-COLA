# egon-cola-source-agent

[English](README.md) | [中文](README.zh-CN.md)

This internal source reactor is the Deep Research reference project for `egon-cola-archetype-agent`. It follows the six-module non-open Web profile while keeping one `research` business domain. The source project is a build input, not a public Central artifact.

```text
common -> domain -> application -> adapter -> starter
             \-> infrastructure -> starter
```

The project consumes `egon-cola-component-agent-flow-starter` for Agent Flow execution. Provider configuration, MCP credentials, and API secrets are supplied by environment-backed Starter configuration. It intentionally has no database, Flyway, Redis, MQ, RPC, GraphQL, UI, history, or recovery API.

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

## Environment contract

Development and production require externally supplied values for `DEEP_RESEARCH_MODEL_BASE_URL`, `DEEP_RESEARCH_MODEL_API_KEY`, `DEEP_RESEARCH_MODEL_NAME`, `DEEP_RESEARCH_API_KEY`, `DEEP_RESEARCH_MCP_BASE_URI`, `DEEP_RESEARCH_MCP_SSE_ENDPOINT`, and `DEEP_RESEARCH_MCP_API_KEY`. Runtime limits use the `DEEP_RESEARCH_MAX_*` and `DEEP_RESEARCH_HEARTBEAT_INTERVAL` variables. No credential, provider URL, or usable secret is committed in this source project. The test profile uses a fake `ChatModel` and fake `ToolCallback` and never opens a network connection.

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

The generated family is validated separately by the repository archetype scripts. The source project contains no database, Flyway migration, cache, message broker, RPC, GraphQL endpoint, or UI.
