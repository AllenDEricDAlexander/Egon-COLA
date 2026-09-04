# Egon-COLA Agent Flow Starter

[English](README.md) | [中文](README.zh-CN.md)

## 简介

`egon-cola-component-agent-flow-starter` 是一个扁平化的 Spring Boot 组件，用于按配置编译和运行 Agent Flow 树。它把宿主提供的 Spring AI `ChatModel` 与 Google ADK 的 `LlmAgent`、`SequentialAgent`、`ParallelAgent`、`LoopAgent` 和 `InMemoryRunner` 组合起来，不引入参考项目的 DDD 包结构。

组件负责配置校验、ADK 图编译、不可变运行时 Registry、进程内 Session、按 tuple 的执行互斥、同步事件收集、流式 cancel 和有界 close。宿主应用负责 provider 实现、凭据、工具、MCP、HTTP、认证和持久化决策。

## 版本矩阵

| 要求 | 版本或边界 |
|---|---|
| Java | 21 |
| Spring Boot | 3.5.16 |
| Spring AI | 1.1.8 |
| Google ADK | 0.7.0 |
| Session 存储 | ADK `InMemorySessionService`，进程内、in-memory |

组件不会带入 Spring AI provider starter 或 provider 凭据。宿主需要提供一个或多个具名 `ChatModel` Bean。

## 依赖方式

导入 Components BOM，再加入唯一的 Agent Flow starter：

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

Starter 默认为关闭状态。仅添加依赖不会创建 Agent Flow 运行时 Bean。

## 宿主提供 ChatModel

provider 相关的应用配置必须留在宿主中，Agent Flow 配置只引用 Bean 名称：

```java
@Bean("researchChatModel")
ChatModel researchChatModel(ChatModel hostProviderChatModel) {
    return hostProviderChatModel;
}
```

示例故意不包含 provider URL、API key 或厂商 starter。配置中的 `chat-model-bean-name` 必须能解析到这个具名 Bean。

## 配置

组件唯一配置前缀是 `egon.cola.component.agent-flow`。Binder 使用严格模式：未知键、类型错误、非法 duration、缺失模型 Bean、重复节点、环、多个父节点、不可达节点和非法 root 都会在应用启动时失败，不发布部分 Registry。

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
                description: 规划研究工作
                instruction: Produce a concise research plan.
                output-key: plan
              - name: writer
                description: 撰写最终答案
                instruction: Turn the collected findings into a cited answer.
                output-key: answer
            workflows:
              - type: SEQUENTIAL
                name: research-sequence
                description: 先规划再写作
                sub-agent-names: [planner, writer]
```

通过 `root-agent-name` 可以选择以下四种形状：

| 形状 | 配置方式 |
|---|---|
| 单叶子 | Root 指向一个 `agents` 项，`workflows` 为空。 |
| Sequential | Root 指向 `SEQUENTIAL` workflow，并按顺序填写 `sub-agent-names`。 |
| Parallel | Root 指向 `PARALLEL` workflow，子 Agent 可并行执行。 |
| Loop | Root 指向 `LOOP` workflow，并将 `max-iterations` 设置为 1 到 100。 |

除 root 外，每个节点必须恰好有一个父节点。Workflow 不能引用自身、未知节点或重复的子节点。

## Java API

`AgentFlowService` 是内部 Java API，不是 HTTP Controller，也不是 MCP endpoint。它提供五个操作：`listFlows`、`createSession`、`deleteSession`、`execute` 和 `executeStream`。

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

`execute` 复用流式管线，返回保持 ADK emission 顺序的不可变 `List<Event>`。`executeStream` 是惰性的：返回时不会查询 Session、获取 guard 或调用模型，只有订阅时才开始；每次订阅都代表一次执行。

## 执行与生命周期语义

- 执行 guard 的 key 是 `(flowId, userId, sessionId)`。相同 tuple 互斥，不同 tuple 可以并行。
- `AgentFlowSessionBusyException` 立即失败。delete 和 execute 使用同一个 guard，避免检查后再操作的竞态。
- `execution-timeout` 是一次执行的总 wall-clock deadline。超时抛出 `AgentFlowExecutionTimeoutException`，dispose 上游订阅并释放 tuple lease。
- 调用方 cancel 会 dispose 流并释放 lease，不伪造 error。上游失败会包装为保留 cause 的 `AgentFlowExecutionException`。
- 组件没有自动重试（`no retry`），重试策略和幂等决策由宿主负责。
- Session 是进程内的 in-memory 状态，不持久化、不复制，进程重启后不能恢复。timeout 或 cancel 不会回滚 ADK 已接受的事件。
- `close` 把 guard 切换到 closing，拒绝新操作，在 `shutdown-timeout` 内等待，然后尝试关闭全部 Runner。它是幂等的，超时后不宣称优雅完成。

## 边界与日志

扁平 starter 只拥有 Agent Flow 能力：

```text
configuration -> validation -> ADK graph compilation -> registry -> session execution
```

它不提供 provider 集成、HTTP、MCP、database、UI、持久化 Session 或远程恢复协议。宿主拥有这些边界，并负责提供可信的 `userId`。组件日志只记录安全的 stage/outcome/error-type 和 event-count，不记录消息内容、instruction、凭据、`userId` 或 `sessionId`。生产环境应审查或关闭 provider/ADK debug 日志，因为第三方 observability 可能看到更宽的内容范围。

## 升级与验证门禁

当前验证的兼容线是 Java 21 + Spring Boot 3.5.16 + Spring AI 1.1.8 + Google ADK 0.7.0。升级 Spring AI 或 ADK 后必须重新执行 focused tests、runtime dependency tree 检查和 Components reactor 测试。没有新的 Spec，不要向 starter 加入 `google-adk-dev`、provider starter、Web、MCP、JDBC 或 Flyway 依赖。

```bash
./mvnw -B -ntp -f egon-cola-component-agent-flow-starter/pom.xml test
```
