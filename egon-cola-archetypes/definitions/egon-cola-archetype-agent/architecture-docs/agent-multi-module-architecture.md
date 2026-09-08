# Deep Research Agent Archetype Architecture

This archetype is the Agent-specific application of the Egon-COLA Web six-module
profile. The generated project has one business capability, Deep Research, and no
database, Flyway migration, cache, message broker, RPC, GraphQL, UI, status, history,
resume, or durable recovery surface.

```text
common <- domain <- application <- adapter <- starter
                         ^             ^
                         |             |
                    infrastructure --+
```

The `domain` module owns research records, events, ports, and service contracts.
`application` owns the `DeepResearchManage` facade, validation, process-local capacity
bulkhead, deadline, and terminal cleanup. `infrastructure` adapts the domain gateway to
the shared Agent Flow component and creates one configured MCP SSE tool set. `adapter`
owns the API-key and trace filters, JSON boundary, error contract, and the only
`POST /api/v1/deep-research/runs` SSE command. `starter` owns Spring Boot, Spring AI
model configuration, OpenAPI metadata, and the fixed flow graph.

The request cannot choose a provider, model, tool, prompt, or flow. Each accepted
request gets one isolated Agent Flow session and a process-local run. Events are
ordered by `runId:sequence`; `research.completed` or `research.failed` is the single
public terminal event. A disconnected client cancels the run and releases the session,
subscription, and capacity lease. Model and MCP output is untrusted; report Markdown
must be sanitized before rendering.

The normal source project is the business truth. This definition is only the Maven
packaging contract, and `.generated` is an ignored derived reactor. Generated projects
must be verified offline with fake model/tool dependencies and must not receive
credentials or provider-specific endpoints from the archetype.

## Dependency and runtime ownership

Generated projects inherit the released `top.egon:egon-cola-archetypes-parent` at a concrete version with an empty `relativePath`. The parent imports the Components BOM, manages Common dependencies and ShardingSphere 5.5.3, and keeps Commons Lang at 3.20.0. Consumer modules inherit their own project root. Install the matching parent/BOM and required artifacts locally before validating an unpublished release; a local install does not publish artifacts.

Agent retains its six-module Deep Research application with Spring AI 1.1.8, Agent Flow and Google ADK. It does not gain Egon RPC/DDC, Nacos, Dubbo or ShardingSphere runtime dependencies. Google ADK may independently bring gRPC/Protobuf; BOM management alone is not a runtime dependency.
