# Egon-COLA Agent Flow Starter

[English](README.md) | [中文](README.zh-CN.md)

## Overview

`egon-cola-component-agent-flow-starter` is a flat Spring Boot component for compiling and running configuration-driven Agent Flow trees. It combines Spring AI's host-provided `ChatModel` with Google ADK `LlmAgent`, `SequentialAgent`, `ParallelAgent`, `LoopAgent`, and `InMemoryRunner` without introducing the reference project's DDD packages.

The component owns configuration validation, ADK graph compilation, an immutable runtime registry, process-local sessions, tuple-level execution exclusion, synchronous event collection, streaming cancellation, and bounded close. The host application owns the provider implementation, credentials, tools, MCP, HTTP, authentication, and persistence decisions.

## Version matrix

| Requirement | Version or boundary |
|---|---|
| Java | 21 |
| Spring Boot | 3.5.16 |
| Spring AI | 1.1.8 |
| Google ADK | 0.7.0 |
| Session storage | ADK `InMemorySessionService`, process-local and in-memory |

Spring AI provider starters and provider credentials are deliberately not included. A host supplies one or more named `ChatModel` beans.

## Dependency setup

Import the Components BOM and add the single Agent Flow starter:

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>top.egon</groupId>
            <artifactId>egon-cola-components-bom</artifactId>
            <version>${egon-cola.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>

<dependencies>
    <dependency>
        <groupId>top.egon</groupId>
        <artifactId>egon-cola-component-agent-flow-starter</artifactId>
    </dependency>
</dependencies>
```

The starter is default-off. Adding the dependency alone creates no Agent Flow runtime beans.

## Host-owned ChatModel

The provider-specific application configuration stays in the host. Only the bean name is referenced by Agent Flow configuration:

```java
@Bean("researchChatModel")
ChatModel researchChatModel(ChatModel hostProviderChatModel) {
    return hostProviderChatModel;
}
```

The example intentionally contains no provider URL, API key, or vendor starter. The configured `chat-model-bean-name` must resolve to this named bean.

## Configuration

The only component prefix is `egon.cola.component.agent-flow`. Binding is strict: unknown keys, invalid types, invalid durations, missing model beans, duplicate nodes, cycles, multiple parents, unreachable nodes, and invalid roots fail application startup before a partial registry is published.

```yaml
egon:
  cola:
    component:
      agent-flow:
        enabled: true
        execution-timeout: 2m
        shutdown-timeout: 10s
        flows:
          research-flow:
            chat-model-bean-name: researchChatModel
            model-name: research-model
            root-agent-name: planner
            agents:
              - name: planner
                description: Plans the research work
                instruction: Produce a concise research plan.
                output-key: plan
              - name: writer
                description: Writes the final answer
                instruction: Turn the collected findings into a cited answer.
                output-key: answer
            workflows:
              - type: SEQUENTIAL
                name: research-sequence
                description: Plan then write
                sub-agent-names: [planner, writer]
```

Supported root shapes are represented by `root-agent-name`:

| Shape | Configuration |
|---|---|
| Single leaf | Root names one `agents` entry; `workflows` is empty. |
| Sequential | Root names a `SEQUENTIAL` workflow with ordered `sub-agent-names`. |
| Parallel | Root names a `PARALLEL` workflow with independent children. |
| Loop | Root names a `LOOP` workflow and sets `max-iterations` from 1 to 100. |

Each node has exactly one parent except the root. A workflow cannot refer to itself, an unknown node, or the same child twice.

## Java API

`AgentFlowService` is an internal Java API; it is not an HTTP controller or an MCP endpoint. The five operations are `listFlows`, `createSession`, `deleteSession`, `execute`, and `executeStream`:

```java
AgentFlowSessionResult session = agentFlowService.createSession(
        new AgentFlowSessionCommand("research-flow", "tenant-a:user-42", null));

List<Event> events = agentFlowService.execute(new AgentFlowExecutionCommand(
        "research-flow",
        "tenant-a:user-42",
        session.sessionId(),
        Content.fromParts(Part.fromText("Research the selected topic."))));

Flowable<Event> stream = agentFlowService.executeStream(new AgentFlowExecutionCommand(
        "research-flow",
        "tenant-a:user-42",
        session.sessionId(),
        Content.fromParts(Part.fromText("Continue with the source comparison."))));

agentFlowService.deleteSession(new AgentFlowSessionCommand(
        "research-flow", "tenant-a:user-42", session.sessionId()));
```

`execute` reuses the streaming pipeline and returns an immutable `List<Event>` in ADK emission order. `executeStream` is lazy: no session lookup, guard acquisition, or model call happens until subscription, and every subscription represents one execution.

## Execution and lifecycle semantics

- The execution guard key is `(flowId, userId, sessionId)`. The same tuple is exclusive; different tuples may run in parallel.
- `AgentFlowSessionBusyException` is fail-fast. Delete and execute use the same guard, so deletion cannot pass a check-then-act gap.
- `execution-timeout` is one total wall-clock deadline. A timeout raises `AgentFlowExecutionTimeoutException`, disposes the upstream subscription, and releases the tuple lease.
- Caller cancellation disposes the stream and releases the lease without fabricating an error. An upstream failure becomes `AgentFlowExecutionException` with its cause retained.
- There is no retry in the component (`no retry`); the host owns retry policy and idempotency decisions.
- Sessions are process-local and in-memory. They are not durable, replicated, or recoverable after process restart. A timeout or cancel does not roll back events already accepted by ADK.
- `close` changes the guard to closing, rejects new operations, waits up to `shutdown-timeout`, then attempts every Runner close. It is idempotent and does not claim graceful completion after the bound expires.

## Boundaries and logging

The flat starter owns only the Agent Flow component capability:

```text
configuration -> validation -> ADK graph compilation -> registry -> session execution
```

It does not provide a provider integration, HTTP, MCP, database, UI, durable session store, or remote recovery protocol. The host owns those boundaries and supplies trusted `userId` values. Component logs contain only safe stage/outcome/error-type and event-count fields; they never record message content, instructions, credentials, `userId`, or `sessionId`. Provider/ADK debug logging must be reviewed or disabled in production because third-party observability may have broader content visibility.

## Upgrade and validation gate

The tested compatibility line is Java 21 + Spring Boot 3.5.16 + Spring AI 1.1.8 + Google ADK 0.7.0. Any Spring AI or ADK upgrade requires re-running the focused tests, the runtime dependency tree check, and the Components reactor test. Do not add `google-adk-dev`, provider starters, Web, MCP, JDBC, or Flyway dependencies to this starter without a new specification.

```bash
./mvnw -B -ntp -f egon-cola-component-agent-flow-starter/pom.xml test
```
