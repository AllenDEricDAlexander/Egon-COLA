# Rule Engine Component Design

## 1. Context

Egon-COLA is adding a lightweight business rule orchestration component under `egon-cola-components`. The provided requirement document names the original component `atluofu-rule-engine`, but this repository should expose it with Egon-COLA naming and component conventions.

The first version is a Spring Boot Starter for Java 21 and Spring Boot 3.5.x. It standardizes two common business flow models:

1. Chain of Responsibility for linear, ordered processing.
2. Rule Tree for branching, dynamic routing, jump-back, and loop-capable decision flows.

This component is not a full rule platform. It does not provide UI, database-backed rule configuration, runtime topology hot updates, or an expression engine.

## 2. Confirmed Decisions

1. Use Scheme A: one starter module plus one test/sample module.
2. Do not create a component-local `docs` directory.
3. Use Egon-COLA naming:
   - component root: `egon-cola-component-rule-engine`
   - starter artifact: `egon-cola-component-rule-engine-starter`
   - test artifact: `egon-cola-component-rule-engine-test`
   - package root: `top.egon.cola.component.ruleengine`
   - configuration prefix: `egon.cola.component.rule-engine`
4. `RuleResult` uses an `int code` contract, consistent with the current common component style.
5. Examples and validation live in the test module.
6. V1.0 covers the requirement document acceptance scope AC-001 through AC-020, while keeping all declared non-goals out of implementation.
7. The design spec is stored under `docs/superpowers/specs`, not under the new component.

## 3. Goals

This work delivers:

1. A new `egon-cola-component-rule-engine` component under `egon-cola-components`.
2. A Spring Boot auto-configured starter that exposes `RuleEngine`.
3. Java-code assembly for `RuleChain` and `RuleTree`.
4. Linear responsibility-chain execution with active stop, exception wrapping, and trace recording.
5. Rule-tree execution with dynamic routing, default routing, jump-back, loops, max-step protection, and timeout protection.
6. A request-scoped `RuleContext` for attributes, flow control, trace IDs, execution path, step count, deadlines, and errors.
7. A unified `RuleResult<R>` with status, int code, message, data, trace, exception, stopped node, hit node, and total cost.
8. Listener extension points for engine, node, route, stop, timeout, max-steps, and error events.
9. A lightweight `RuleAsyncExecutor` based on `CompletableFuture` and an overridable executor.
10. README usage documentation and component-local tests that prove the main flows without starting long-running services.

## 4. Non-Goals

V1.0 does not include:

1. UI management pages.
2. Admin service.
3. Database tables or Flyway migrations.
4. YAML, JSON, database, or remote topology definition.
5. Runtime rule topology hot updates.
6. Drools, SpEL, Groovy, MVEL, Aviator, or other expression engines.
7. Tenant, permission, grayscale, AB test, or rule version governance.
8. Full dynamic-thread-pool governance for async execution.
9. Compatibility packages under `cn.atluofu` or other old package names.
10. A separate SDK/core artifact outside the starter.

## 5. Target Module Structure

The new component is added as:

```text
egon-cola-components/
└── egon-cola-component-rule-engine/
    ├── pom.xml
    ├── README.md
    ├── egon-cola-component-rule-engine-starter/
    │   ├── pom.xml
    │   └── src/
    └── egon-cola-component-rule-engine-test/
        ├── pom.xml
        └── src/
```

The components parent POM aggregates the component root. The component root aggregates `starter` and `test`. The components BOM exports only:

```text
top.egon:egon-cola-component-rule-engine-starter
```

The BOM must not export the component root POM or the test module.

## 6. Dependency Boundary

The starter may depend on:

1. `spring-boot-starter`
2. `spring-boot-autoconfigure`
3. `spring-boot-configuration-processor` as optional
4. `slf4j-api`
5. selected Egon-COLA common modules when they directly help the rule-engine contract, such as common core or trace

The starter must not depend on:

1. Admin modules.
2. Test modules.
3. Database, JPA, Flyway, Redis, MQ, Nacos, Dubbo, or web UI dependencies.
4. Dynamic config center or dynamic thread pool components.

Async execution should use JDK concurrency by default. A later integration with the dynamic thread pool component can be added through an adapter, but V1.0 must not require that component.

## 7. Starter Package Layout

The starter package root is:

```text
top.egon.cola.component.ruleengine
```

Target layout:

```text
top.egon.cola.component.ruleengine
├── autoconfigure
│   ├── RuleEngineAutoConfiguration
│   └── RuleEngineProperties
├── engine
│   ├── RuleEngine
│   ├── DefaultRuleEngine
│   ├── RuleChainExecutor
│   ├── DefaultRuleChainExecutor
│   ├── RuleTreeExecutor
│   └── DefaultRuleTreeExecutor
├── context
│   └── RuleContext
├── result
│   ├── RuleResult
│   └── RuleStatus
├── trace
│   ├── RuleTrace
│   ├── NodeTrace
│   └── RuleTraceRecorder
├── chain
│   ├── RuleChain
│   ├── ChainHandler
│   ├── SingletonRuleLink
│   └── AbstractSingletonRuleLink
├── tree
│   ├── RuleTree
│   ├── RuleNode
│   ├── RuleRouter
│   ├── RouteDecision
│   ├── AbstractRuleNode
│   ├── AbstractAsyncRuleNode
│   └── NodeType
├── async
│   ├── RuleAsyncExecutor
│   └── DefaultRuleAsyncExecutor
├── listener
│   ├── RuleExecutionListener
│   ├── RuleExecutionListenerComposite
│   └── LoggingRuleExecutionListener
└── exception
    ├── RuleEngineException
    ├── RuleConfigException
    ├── RuleNodeException
    ├── RuleRouteException
    ├── RuleTimeoutException
    ├── RuleMaxStepsExceededException
    ├── RuleEmptyChainException
    └── RuleEmptyTreeException
```

This layout keeps V1.0 understandable and avoids adding a separate internal domain layer.

## 8. Public API Shape

`RuleEngine` is the preferred business entry point:

```java
<T, R> RuleResult<R> executeChain(RuleChain<T, R> ruleChain, T request);

<T, R> RuleResult<R> executeChain(RuleChain<T, R> ruleChain, T request, RuleContext context);

<T, R> RuleResult<R> executeTree(RuleTree<T, R> ruleTree, T request);

<T, R> RuleResult<R> executeTree(RuleTree<T, R> ruleTree, T request, RuleContext context);
```

Business code may also inject specific executors:

```java
RuleChainExecutor
RuleTreeExecutor
```

The API accepts Java-assembled rule definitions only. No external topology parser is provided in V1.0.

## 9. RuleContext

`RuleContext` is request-scoped and must not be reused across executions. It owns dynamic state for one rule execution:

```text
requestId
traceId
proceed
stopped
currentNode
previousNode
executionPath
stepCount
maxSteps
startTime
deadline
attributes
errors
```

Attribute API:

```java
RuleContext set(String key, Object value);
Object get(String key);
<T> T get(String key, Class<T> type);
boolean contains(String key);
Object remove(String key);
```

Flow-control API:

```java
void proceed();
void stop();
boolean isProceed();
boolean isStopped();
int incrementStep();
boolean isExceededMaxSteps();
boolean isTimeout();
```

`attributes` should use a concurrency-safe structure because async resource loading can write values back into the context. Business governance values such as tenant, permission, grayscale, and rule version are not first-class fields. Business code can store them as attributes when needed.

## 10. RuleResult And Status

Every execution returns `RuleResult<R>`.

Fields:

```text
success
status
int code
message
data
trace
exception
stoppedNode
hitNode
costMillis
```

`RuleStatus` includes:

```text
SUCCESS
STOPPED
FAILED
TIMEOUT
MAX_STEPS_EXCEEDED
NO_ROUTE
EMPTY_CHAIN
EMPTY_TREE
NODE_ERROR
CONFIG_ERROR
```

Convenience factories:

```java
RuleResult.success(data)
RuleResult.stop(code, message, data)
RuleResult.fail(code, message, exception)
RuleResult.timeout(message)
RuleResult.maxStepsExceeded(message)
RuleResult.noRoute(message)
```

The default success code is `0`. Non-success codes are integers and should follow the Egon-COLA common status convention, with rule-engine-specific codes mapped by status where needed. The public contract must not use string codes.

## 11. Responsibility Chain Model

Responsibility chains handle linear, ordered business flows.

`ChainHandler<T, R>` receives:

```text
request
RuleContext
```

and returns `RuleResult<R>`.

The recommended chain model is multi-instance `RuleChain`:

1. Chain definitions are immutable after assembly.
2. One handler can be reused by multiple chains.
3. Chain execution state lives in `RuleContext`, not in handlers.
4. Empty chains fail with `EMPTY_CHAIN`.
5. Stop never depends on returning `null`.

Stop mechanisms:

1. handler returns `RuleResult.stop(...)`
2. handler calls `context.stop()`
3. handler throws an exception and the framework wraps it
4. timeout or max-steps protection terminates execution

The starter also keeps singleton chain support:

```java
appendNext(next)
next(request, context)
```

Singleton chains must be assembled at startup. Runtime mutation of the `next` pointer is prohibited. The README must warn that multi-chain reuse should prefer multi-instance `RuleChain` rather than sharing mutable singleton links.

## 12. Rule Tree Model

`RuleTree` is exposed as a tree to business developers, but its execution semantics are closer to a decision graph. It supports:

1. root node execution
2. business nodes
3. switch nodes
4. end nodes
5. custom node types
6. dynamic route decisions
7. default route decisions
8. jump-back to already executed nodes
9. business loops

`RuleNode<T, R>` executes business logic and returns a node result or `RuleResult<R>`. Routing is handled through `RuleRouter<T, R>` or a node-provided route decision. `RouteDecision` should support:

```text
target node instance
target node code
route reason
end
no route
```

The executor must not reject cycles during startup validation because loops are an explicit V1.0 capability. Runtime protection is mandatory:

1. increment step before or during every node execution
2. record every node visit
3. terminate with `MAX_STEPS_EXCEEDED` when max steps are exceeded
4. terminate with `TIMEOUT` when deadline is exceeded
5. include full execution path in the trace

Default routing priority:

1. current node default target
2. rule tree default end node
3. `NO_ROUTE` result

## 13. Trace Model

`RuleTrace` records one execution:

```text
ruleCode
ruleName
modelType
requestId
traceId
startTime
endTime
costMillis
status
nodeTraces
error
```

`NodeTrace` records one node visit:

```text
nodeCode
nodeName
nodeType
order
visitCount
startTime
endTime
costMillis
routeTo
routeReason
status
error
```

Trace recording is enabled by default and can be disabled through properties. Even when detailed trace recording is disabled, the result should still expose basic status and cost fields.

## 14. Listener Model

`RuleExecutionListener` provides lifecycle hooks:

```text
beforeEngineExecute
afterEngineExecute
beforeNodeExecute
afterNodeExecute
beforeRoute
afterRoute
onNodeError
onEngineError
onStop
onTimeout
onMaxStepsExceeded
```

The auto-configuration collects all Spring `RuleExecutionListener` beans, sorts them by `@Order` or `Ordered`, and delegates through `RuleExecutionListenerComposite`.

Listener failures are ignored by default and logged. A property can make listener failures strict, but the default should protect the main business execution.

`LoggingRuleExecutionListener` is provided as a default listener. It logs the execution summary at INFO, node path details at DEBUG, route or assembly warnings at WARN, and execution failures at ERROR.

## 15. Async Resource Loading

`RuleAsyncExecutor` is a lightweight helper for node preloading:

1. uses `CompletableFuture`
2. supports timeout
3. captures async exceptions and writes them to `RuleContext.errors`
4. supports writing named results into `RuleContext`
5. uses a default JDK executor
6. can be replaced by a user-provided `RuleAsyncExecutor` bean

This module does not implement thread-pool governance. Future integration with the dynamic thread pool component should be an optional adapter, not a V1.0 dependency.

## 16. Auto-Configuration And Properties

Auto-configuration registers:

```text
RuleEngine
RuleChainExecutor
RuleTreeExecutor
RuleExecutionListenerComposite
RuleAsyncExecutor
RuleTraceRecorder
RuleEngineProperties
LoggingRuleExecutionListener
```

The Spring Boot auto-configuration import file is:

```text
META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
```

Configuration prefix:

```text
egon.cola.component.rule-engine
```

Default properties:

```yaml
egon:
  cola:
    component:
      rule-engine:
        enabled: true
        default-max-steps: 100
        default-timeout-millis: 3000
        async-core-pool-size: 4
        async-max-pool-size: 16
        trace-enabled: true
        listener-error-ignore: true
        throw-exception: false
```

These properties are framework runtime settings only. They must not define chain order, tree routing, node conditions, rule versions, or grayscale behavior.

## 17. Validation And Assembly Rules

Startup validation for `RuleChain`:

1. rule code is not blank
2. handlers are not empty
3. no handler is null
4. assembled chain is immutable

Startup validation for `RuleTree`:

1. rule code is not blank
2. root node exists
3. node codes are unique
4. default node references are legal
5. cycles are allowed
6. obviously unreachable nodes may log warnings but must not block startup

Runtime validation:

1. each execution creates or receives an independent `RuleContext`
2. null context is replaced by a new context
3. null request handling is left to business handlers and nodes unless a specific rule definition forbids it
4. max steps and deadline are applied for both chains and trees

## 18. README Contract

The component README should include:

1. dependency snippet
2. configuration prefix and defaults
3. multi-instance chain example
4. singleton chain example and mutation warning
5. rule tree example with jump-back and max-step protection
6. async loading example
7. listener extension example
8. common errors and troubleshooting
9. explicit non-goals: no UI, no DB topology, no YAML/JSON topology, no hot update

No component-local `docs` directory is created in V1.0.

## 19. Test Module

`egon-cola-component-rule-engine-test` proves the V1.0 contract.

Required test coverage:

1. Spring Boot auto-configuration injects `RuleEngine`.
2. Multi-instance chain executes handlers in order and records the path.
3. Any chain handler can stop execution through `RuleResult.stop(...)`.
4. Any chain handler exception returns a unified failed result.
5. Multiple chains can reuse one handler without state pollution.
6. Singleton chain executes with `appendNext`.
7. Rule tree starts from root node.
8. Rule tree routes to different nodes based on context.
9. Rule tree can route back to a previously visited node.
10. Rule tree terminates with `MAX_STEPS_EXCEEDED`.
11. Rule tree terminates with `TIMEOUT`.
12. No route uses default node, default end node, or `NO_ROUTE`.
13. Result contains complete trace and execution path.
14. Multiple listeners execute in order.
15. Listener exceptions are ignored by default.
16. Async resource loading writes results into `RuleContext`.

Validation command:

```bash
./mvnw -B -ntp -pl egon-cola-components/egon-cola-component-rule-engine -am test
```

Long-running services must not be started for delivery validation.

## 20. Acceptance Mapping

The design covers the requirement acceptance list:

| Requirement | Design response |
|---|---|
| AC-001 | Starter auto-configuration injects `RuleEngine`. |
| AC-002 | `RuleChain` is Java-assembled. |
| AC-003 | `RuleTree` is Java-assembled. |
| AC-004 | Chain returns `RuleResult`. |
| AC-005 | Tree returns `RuleResult`. |
| AC-006 | `RuleContext` supports cross-node attributes. |
| AC-007 | Chain nodes continue or stop explicitly. |
| AC-008 | Tree nodes route dynamically. |
| AC-009 | Tree jump-back and loops are allowed. |
| AC-010 | Max steps return `MAX_STEPS_EXCEEDED`. |
| AC-011 | Timeout returns `TIMEOUT`. |
| AC-012 | Node exceptions return failed results and fire listeners. |
| AC-013 | Results include trace. |
| AC-014 | Listener beans execute in order. |
| AC-015 | Async loading writes to context. |
| AC-016 | Multi-instance chain reuse avoids pollution. |
| AC-017 | Singleton chain is retained and tested. |
| AC-018 | YAML, JSON, and DB topology are excluded. |
| AC-019 | Tenant, permission, and grayscale governance are excluded. |
| AC-020 | UI admin is excluded. |

## 21. Design Pattern Consideration

Patterns used:

1. Chain of Responsibility: fits the linear chain model and is explicitly required by the business flow.
2. Strategy: `RuleRouter` isolates variable route decisions without embedding large `if...else` blocks in the executor.
3. Composite: `RuleExecutionListenerComposite` allows ordered listener fan-out while keeping the executor independent of listener count.
4. Builder: optional builders for `RuleChain` and `RuleTree` make Java assembly readable and allow immutable definitions after construction.

Patterns intentionally not used:

1. Abstract Factory is unnecessary because V1.0 does not create families of interchangeable engines.
2. State pattern is unnecessary because runtime state lives clearly in `RuleContext`.
3. Decorator is unnecessary for V1.0 because listener and trace hooks cover cross-cutting behavior without wrapping every node.
4. Specification is unnecessary because expression/rule-condition evaluation is out of scope.

This keeps the design extensible at real variation points without turning a lightweight starter into a heavy framework.

## 22. Risks And Guardrails

1. Rule tree cycles are intentional, so tests must prove max-step and timeout protection.
2. Singleton chain mutation is risky, so README and tests must make startup-only assembly explicit.
3. Listener failures can hide observability bugs, so ignored listener exceptions must still be logged.
4. Async context writes can race, so context attributes must use a concurrency-safe structure.
5. Rule topology configuration can easily creep into properties, so V1.0 properties must remain runtime-only.
6. The starter should remain independent from DDC, DTP, DB, Redis, and admin modules.

## 23. Implementation Sequence Preview

Implementation should proceed in small commits:

1. Add component POMs, aggregation, BOM export, and empty module skeleton.
2. Add core contracts: context, result, status, trace, exceptions.
3. Add chain contracts and executor.
4. Add tree contracts and executor.
5. Add listener, async executor, and auto-configuration.
6. Add README and test-module examples.
7. Run focused component validation.

The detailed implementation plan will be written only after this spec is reviewed and approved.
