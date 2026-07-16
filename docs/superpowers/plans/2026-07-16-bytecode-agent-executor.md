# Bytecode Agent Foundation And Executor Enhancement Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add the premain-only Agent, JDK-only ClassLoader bridge, application runtime, Spring starter, and P0 Executor call-site enhancement while preserving original Future, exception, rejection, interrupt, and context semantics.

**Architecture:** The system-loaded Agent and bridge are isolated from the Spring Boot application ClassLoader. Transformed business call sites invoke JDK-only static bridge methods with the caller class and stable call-site ID; the bridge finds a weakly registered application dispatcher, decorates Runnable/Callable values, and invokes the original Executor API exactly once. The runtime owns context/event logic, while the starter supplies Spring, MDC, Micrometer, DTP, validation, and optional Actuator adapters.

**Tech Stack:** Java 21 production, Java 21/25 forked verification, ASM 9.9.1, Java Instrumentation API, SnakeYAML Engine 2.9 shaded inside Agent, Spring Boot 3.5.16, Micrometer 1.15.12, JUnit 5, Maven Shade Plugin 3.6.1, Maven Invoker Plugin 3.9.1, JMH 1.37.

## Global Constraints

- Complete and merge `2026-07-16-bytecode-architecture-maven-plugin.md` first.
- Treat `docs/superpowers/specs/2026-07-15-bytecode-enhancement-design.md` as authoritative.
- Compile production code with `--release 21`; verify the Agent on Java 21 and Java 25.
- Support only `premain`. Do not add `agentmain`, Attach, redefinition, retransformation, bootstrap/JDK transformation, or transformed-class dumps.
- Transform only `Executor.execute(Runnable)` and the three approved `ExecutorService.submit` overloads.
- Return the exact Future produced by the underlying executor; do not wrap Future.
- Never transform JDK, Spring, logging, metrics, ASM, bridge, runtime, or Agent packages.
- Use `top.egon.cola.component.bytecode.*` and `egon.cola.component.bytecode.*`.
- Bridge depends on JDK only and must not retain strong application ClassLoader/dispatcher/object references after unregister.
- Runtime depends only on API and bridge; it must not depend on Spring, SLF4J, Micrometer, DTP, Access Guard, or Method Extension.
- Starter dependencies on Actuator and DTP are optional; adding starter alone must not transitively add either.
- Do not make `ManagedExecutorRegistry` mutable and do not register discovered executors into DTP.
- No database, Flyway, UI, browser, Docker, or long-running business application startup.
- At execution time use an isolated worktree and one path-scoped commit per task.

## Pattern Boundary

Use Composite for captured carriers and the per-class enhancement plan, Decorator for Runnable/Callable wrapping, Registry for ClassLoader dispatchers/metadata, and narrow Adapters for Spring/DTP. These patterns isolate actual runtime variation; do not add a transformer factory hierarchy or generic middleware chain because one composite planner and explicit bridge calls are clearer.

## Prerequisite Artifact State

The component already contains API, core, Architecture Maven Plugin, test, and benchmark modules. This plan adds:

```text
egon-cola-component-bytecode-bridge
egon-cola-component-bytecode-runtime
egon-cola-component-bytecode-agent
egon-cola-component-bytecode-starter
```

The final component module order is API, bridge, core, runtime, Agent, starter, Architecture Maven Plugin, test, benchmark.

## Stable Cross-Stage Bridge ABI

Define the future feature slots now so Observation, Method Extension, and Access Guard do not break the bridge protocol later. Default methods are no-ops until their stages implement them.

```java
package top.egon.cola.component.bytecode.bridge;

public interface BytecodeRuntimeDispatcher {
    int protocolMajor();
    int protocolMinor();
    java.util.Set<BridgeCapability> capabilities();

    Runnable decorateRunnable(Class<?> callerClass, java.util.concurrent.Executor executor,
            Runnable task, long callSiteId);

    <V> java.util.concurrent.Callable<V> decorateCallable(Class<?> callerClass,
            java.util.concurrent.Executor executor, java.util.concurrent.Callable<V> task,
            long callSiteId);

    default void executorRejected(Class<?> callerClass, long callSiteId,
            java.util.concurrent.Executor executor,
            java.util.concurrent.RejectedExecutionException exception) { }

    default ObservationToken enterObservation(Class<?> declaringClass, long methodId) {
        return ObservationToken.noop();
    }

    default void observationSuccess(ObservationToken token) { }
    default void observationError(ObservationToken token, Throwable throwable) { }
    default void observationExit(ObservationToken token) { }

    default InvocationDecision evaluateMethodExtension(BridgeMethodInvocation invocation) {
        return InvocationDecision.proceed();
    }

    default Object invokeGuarded(BridgeGuardedInvocation invocation) throws Throwable {
        return invocation.proceed();
    }

    default ConstructorGuardDecision guardConstructor(BridgeConstructorInvocation invocation) {
        return ConstructorGuardDecision.allow();
    }
}
```

`BridgeCapability` contains `EXECUTOR`, `OBSERVATION`, `METHOD_EXTENSION`, and `ACCESS_GUARD`. Bridge records may contain JDK types only. The protocol starts at major `1`, minor `0`.

The bridge records are fixed before later stages:

```java
public record CallSiteMetadata(
        long callSiteId, String owner, String methodName, String methodDescriptor,
        String targetOwner, String targetName, String targetDescriptor, Integer lineNumber
) { }

public record MethodMetadata(
        long methodId, String owner, String methodName, String methodDescriptor,
        int access, boolean constructor, java.util.Set<BridgeCapability> features
) {
    public MethodMetadata {
        features = java.util.Set.copyOf(features);
    }
}

public record BridgeMethodInvocation(
        Object target, Class<?> declaringClass, long methodId, Object[] arguments
) {
    public BridgeMethodInvocation {
        arguments = arguments == null ? new Object[0] : arguments.clone();
        java.util.Objects.requireNonNull(declaringClass, "declaringClass");
    }

    @Override public Object[] arguments() { return arguments.clone(); }
}

public record BridgeGuardedInvocation(
        Object target, Class<?> declaringClass, long methodId, Object[] arguments,
        java.lang.invoke.MethodHandle continuation
) {
    public BridgeGuardedInvocation {
        arguments = arguments == null ? new Object[0] : arguments.clone();
        java.util.Objects.requireNonNull(declaringClass, "declaringClass");
        java.util.Objects.requireNonNull(continuation, "continuation");
    }

    public Object proceed() throws Throwable {
        java.util.List<Object> invocationArguments = new java.util.ArrayList<>();
        if (target != null) invocationArguments.add(target);
        java.util.Collections.addAll(invocationArguments, arguments);
        return continuation.invokeWithArguments(invocationArguments);
    }

    @Override public Object[] arguments() { return arguments.clone(); }
}

public enum BridgeFailHint { GLOBAL_DEFAULT, FAIL_OPEN, FAIL_CLOSED }

public record BridgeConstructorInvocation(
        Class<?> declaringClass, long methodId, Object[] arguments, BridgeFailHint failHint
) {
    public BridgeConstructorInvocation {
        arguments = arguments == null ? new Object[0] : arguments.clone();
        java.util.Objects.requireNonNull(declaringClass, "declaringClass");
        java.util.Objects.requireNonNull(failHint, "failHint");
    }

    @Override public Object[] arguments() { return arguments.clone(); }
}
```

`InvocationDecision` has `DecisionKind` values `PROCEED`, `RETURN_NULL`,
`RETURN_VALUE`, and `THROW`, with static factories that validate which of
`value` or `throwable` may be non-null. `ConstructorGuardDecision` has `ALLOW`
and `THROW` factories; the THROW form carries the exact application-loader
exception. Method IDs hash owner + method name + descriptor only; the immutable
`MethodMetadata.features` set records which policies apply to that one method.

## Task 1: Add Modules, Dependency Boundaries, And Executor API Contracts

**Files:**
- Modify: `egon-cola-components/egon-cola-component-bytecode/pom.xml`
- Modify: `egon-cola-components/egon-cola-components-bom/pom.xml`
- Create: `egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-bridge/pom.xml`
- Create: `egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-runtime/pom.xml`
- Create: `egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-agent/pom.xml`
- Create: `egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-starter/pom.xml`
- Create: `egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-api/src/main/java/top/egon/cola/component/bytecode/api/executor/ContextCarrier.java`
- Create: `egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-api/src/main/java/top/egon/cola/component/bytecode/api/executor/ContextScope.java`
- Create: `egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-api/src/main/java/top/egon/cola/component/bytecode/api/executor/ExecutorEvent.java`
- Create: `egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-api/src/main/java/top/egon/cola/component/bytecode/api/executor/ExecutorEventSink.java`
- Test: `egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-api/src/test/java/top/egon/cola/component/bytecode/api/executor/ExecutorApiBoundaryTest.java`

**Interfaces:**
- Produces `ContextCarrier`, `ContextScope`, `ExecutorEvent`, and `ExecutorEventSink`.
- BOM manages API, bridge, runtime, Agent, and starter; it does not manage core/plugin/test/benchmark/root.

- [ ] **Step 1: Write the failing API boundary test**

```java
class ExecutorApiBoundaryTest {
    @Test
    void carrierContractCapturesAndRestoresOpaqueState() {
        assertArrayEquals(new String[]{"name", "capture", "restore"},
                Arrays.stream(ContextCarrier.class.getDeclaredMethods())
                        .map(Method::getName).sorted().toArray(String[]::new));
        assertTrue(AutoCloseable.class.isAssignableFrom(ContextScope.class));
    }
}
```

- [ ] **Step 2: Run and verify missing contracts**

```bash
./mvnw -B -ntp -pl egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-api -am -Dsurefire.failIfNoSpecifiedTests=false -Dtest=ExecutorApiBoundaryTest test
```

Expected: FAIL because executor API contracts are missing.

- [ ] **Step 3: Add modules and API contracts**

```java
public interface ContextCarrier {
    String name();
    Object capture();
    ContextScope restore(Object snapshot);
}

public interface ContextScope extends AutoCloseable {
    @Override
    void close();
}

public interface ExecutorEventSink {
    void publish(ExecutorEvent event);
}
```

`ExecutorEvent` is a record containing `callSiteId`, `executorName`, `executorType`, `phase`, `result`, `exceptionGroup`, `virtualThread`, `submittedNanos`, `startedNanos`, and `completedNanos`; it contains no task, Future, Throwable, request ID, or trace ID object.

- [ ] **Step 4: Verify module boundaries**

Run Step 2 and these dependency checks:

```bash
./mvnw -B -ntp -pl egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-bridge -am dependency:tree
./mvnw -B -ntp -pl egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-runtime -am dependency:tree
```

Expected: bridge has JDK-only production dependencies; runtime has only API/bridge production dependencies.

- [ ] **Step 5: Commit**

```bash
git add egon-cola-components/egon-cola-component-bytecode \
  egon-cola-components/egon-cola-components-bom/pom.xml
git commit -m "feat(bytecode): add agent runtime module boundaries"
```

## Task 2: JDK-Only Bridge Protocol And Weak Dispatcher Registry

**Files:**
- Create: `egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-bridge/src/main/java/top/egon/cola/component/bytecode/bridge/BridgeProtocol.java`
- Create: `egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-bridge/src/main/java/top/egon/cola/component/bytecode/bridge/BridgeCapability.java`
- Create: `egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-bridge/src/main/java/top/egon/cola/component/bytecode/bridge/BridgeFailHint.java`
- Create: `egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-bridge/src/main/java/top/egon/cola/component/bytecode/bridge/BytecodeRuntimeDispatcher.java`
- Create: `egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-bridge/src/main/java/top/egon/cola/component/bytecode/bridge/DispatcherRegistry.java`
- Create: `egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-bridge/src/main/java/top/egon/cola/component/bytecode/bridge/DispatcherRegistration.java`
- Create: `egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-bridge/src/main/java/top/egon/cola/component/bytecode/bridge/BridgeStatus.java`
- Create: `egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-bridge/src/main/java/top/egon/cola/component/bytecode/bridge/CallSiteMetadata.java`
- Create: `egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-bridge/src/main/java/top/egon/cola/component/bytecode/bridge/MethodMetadata.java`
- Create: `egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-bridge/src/main/java/top/egon/cola/component/bytecode/bridge/ObservationToken.java`
- Create: `egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-bridge/src/main/java/top/egon/cola/component/bytecode/bridge/InvocationDecision.java`
- Create: `egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-bridge/src/main/java/top/egon/cola/component/bytecode/bridge/DecisionKind.java`
- Create: `egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-bridge/src/main/java/top/egon/cola/component/bytecode/bridge/BridgeMethodInvocation.java`
- Create: `egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-bridge/src/main/java/top/egon/cola/component/bytecode/bridge/BridgeGuardedInvocation.java`
- Create: `egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-bridge/src/main/java/top/egon/cola/component/bytecode/bridge/BridgeConstructorInvocation.java`
- Create: `egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-bridge/src/main/java/top/egon/cola/component/bytecode/bridge/ConstructorGuardDecision.java`
- Create: `egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-bridge/src/main/java/top/egon/cola/component/bytecode/bridge/EgonExecutorBridge.java`
- Create: `egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-bridge/src/main/java/top/egon/cola/component/bytecode/bridge/EgonObservationBridge.java`
- Create: `egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-bridge/src/main/java/top/egon/cola/component/bytecode/bridge/EgonPolicyBridge.java`
- Test: `egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-bridge/src/test/java/top/egon/cola/component/bytecode/bridge/DispatcherRegistryTest.java`
- Test: `egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-bridge/src/test/java/top/egon/cola/component/bytecode/bridge/EgonExecutorBridgeTest.java`

**Interfaces:**
- Consumes only JDK types.
- Produces the stable cross-stage ABI and four exact Executor bridge overloads.

- [ ] **Step 1: Write failing weak-registration and Future-identity tests**

Test duplicate registration rejection, major mismatch rejection, minor capability negotiation, unregister, weak loader collection, dispatcher failure fallback, original `RejectedExecutionException`, and:

```java
Future<String> actual = EgonExecutorBridge.submit(executor, () -> "ok", Caller.class, 42L);
assertSame(recordingExecutor.lastFuture(), actual);
```

- [ ] **Step 2: Run bridge tests**

```bash
./mvnw -B -ntp -pl egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-bridge -am test
```

Expected: FAIL because bridge types are absent.

- [ ] **Step 3: Implement registry and protocol**

`DispatcherRegistry` uses synchronized access around a `WeakHashMap<ClassLoader, Entry>`; each entry contains only weak dispatcher reference, version/capability scalars, and string metadata. `register(...)` returns an idempotent `DispatcherRegistration.close()` and rejects two live dispatchers for the same ClassLoader.

- [ ] **Step 4: Implement exact Executor bridge calls**

```java
public static void execute(Executor executor, Runnable task, Class<?> caller, long id) {
    executor.execute(decorateRunnable(caller, executor, task, id));
}

public static Future<?> submit(ExecutorService executor, Runnable task, Class<?> caller, long id) {
    return executor.submit(decorateRunnable(caller, executor, task, id));
}

public static <T> Future<T> submit(ExecutorService executor, Runnable task, T result,
        Class<?> caller, long id) {
    return executor.submit(decorateRunnable(caller, executor, task, id), result);
}

public static <T> Future<T> submit(ExecutorService executor, Callable<T> task,
        Class<?> caller, long id) {
    return executor.submit(decorateCallable(caller, executor, task, id));
}
```

If no dispatcher exists or decoration throws, submit the original task. The bridge catches only `RejectedExecutionException`, publishes a best-effort rejection callback, and rethrows the exact same exception instance; it does not catch any other exception from the actual executor call. Run Step 2; expected PASS.

- [ ] **Step 5: Commit**

```bash
git add egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-bridge
git commit -m "feat(bytecode): add classloader-safe bridge"
```

## Task 3: Runtime Context Capture, Task Decoration, And Events

**Files:**
- Create: `egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-runtime/src/main/java/top/egon/cola/component/bytecode/runtime/context/CompositeContextCarrier.java`
- Create: `egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-runtime/src/main/java/top/egon/cola/component/bytecode/runtime/context/ContextSnapshot.java`
- Create: `egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-runtime/src/main/java/top/egon/cola/component/bytecode/runtime/executor/EgonInstrumentedTask.java`
- Create: `egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-runtime/src/main/java/top/egon/cola/component/bytecode/runtime/executor/EgonContextAwareRunnable.java`
- Create: `egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-runtime/src/main/java/top/egon/cola/component/bytecode/runtime/executor/EgonContextAwareCallable.java`
- Create: `egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-runtime/src/main/java/top/egon/cola/component/bytecode/runtime/executor/RuntimeTaskDetector.java`
- Create: `egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-runtime/src/main/java/top/egon/cola/component/bytecode/runtime/executor/ExecutorTaskDecorator.java`
- Create: `egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-runtime/src/main/java/top/egon/cola/component/bytecode/runtime/executor/ExecutorNameResolver.java`
- Create: `egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-runtime/src/main/java/top/egon/cola/component/bytecode/runtime/event/RuntimeEventFanout.java`
- Create: `egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-runtime/src/main/java/top/egon/cola/component/bytecode/runtime/event/BoundedFailureStore.java`
- Create: `egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-runtime/src/main/java/top/egon/cola/component/bytecode/runtime/DefaultBytecodeRuntimeDispatcher.java`
- Test: `egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-runtime/src/test/java/top/egon/cola/component/bytecode/runtime/context/CompositeContextCarrierTest.java`
- Test: `egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-runtime/src/test/java/top/egon/cola/component/bytecode/runtime/executor/ExecutorTaskDecoratorTest.java`

**Interfaces:**
- Consumes API carriers/sinks and bridge dispatcher.
- Produces task wrappers implementing `EgonInstrumentedTask` and a dispatcher advertising `EXECUTOR`.

- [ ] **Step 1: Write failing context and task semantic tests**

Cover carrier capture order, reverse close order, partial restore cleanup, success/failure cleanup, sink failure isolation, nested submission, 100 concurrent submissions, interrupt preservation, and detection of Egon/DTP wrapper types.

- [ ] **Step 2: Run focused runtime tests**

```bash
./mvnw -B -ntp -pl egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-runtime -am test
```

Expected: FAIL because runtime types are missing.

- [ ] **Step 3: Implement context lifecycle**

```java
public ContextSnapshot capture() {
    List<CapturedContext> values = carriers.stream()
            .map(carrier -> new CapturedContext(carrier, carrier.capture()))
            .toList();
    return new ContextSnapshot(values);
}

public ContextScope restore() {
    Deque<ContextScope> scopes = new ArrayDeque<>();
    try {
        captured.forEach(value -> scopes.push(value.carrier().restore(value.snapshot())));
        return () -> closeAll(scopes);
    } catch (Throwable failure) {
        closeAll(scopes);
        throw failure;
    }
}
```

- [ ] **Step 4: Implement decoration and rerun**

Capture once at submission. Restore before business execution and close in `finally`. Publish submission, start, completion, failure, and rejection-safe metadata without changing task exceptions. `RuntimeTaskDetector` rejects `EgonInstrumentedTask` plus exact DTP class names `top.egon.cola.component.dtp.context.DtpRunnable` and `DtpCallable` without linking DTP. `ExecutorNameResolver` uses Spring Bean name, existing `ManagedExecutorRegistry` name, explicit configured name, then executor class plus identity suffix; Spring/DTP values arrive through starter adapters, not runtime dependencies.

Run Step 2. Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-runtime
git commit -m "feat(bytecode): decorate executor tasks"
```

## Task 4: Agent Configuration, Filtering, State, And Shaded Premain JAR

**Files:**
- Create: `egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-agent/src/main/java/top/egon/cola/component/bytecode/agent/BytecodeAgent.java`
- Create: `egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-agent/src/main/java/top/egon/cola/component/bytecode/agent/AgentArguments.java`
- Create: `egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-agent/src/main/java/top/egon/cola/component/bytecode/agent/AgentConfiguration.java`
- Create: `egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-agent/src/main/java/top/egon/cola/component/bytecode/agent/AgentConfigurationLoader.java`
- Create: `egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-agent/src/main/java/top/egon/cola/component/bytecode/agent/AgentYamlParser.java`
- Create: `egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-agent/src/main/java/top/egon/cola/component/bytecode/agent/ClassNameFilter.java`
- Create: `egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-agent/src/main/java/top/egon/cola/component/bytecode/agent/AgentState.java`
- Create: `egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-agent/src/main/java/top/egon/cola/component/bytecode/agent/AgentStateStore.java`
- Create: `egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-agent/src/main/java/top/egon/cola/component/bytecode/agent/AgentFailure.java`
- Create: `egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-agent/src/main/java/top/egon/cola/component/bytecode/agent/AgentStartupReporter.java`
- Create: `egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-agent/src/main/java/top/egon/cola/component/bytecode/agent/transform/CompositeBytecodeTransformer.java`
- Test: `egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-agent/src/test/java/top/egon/cola/component/bytecode/agent/AgentConfigurationLoaderTest.java`
- Test: `egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-agent/src/test/java/top/egon/cola/component/bytecode/agent/ClassNameFilterTest.java`
- Test: `egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-agent/src/test/java/top/egon/cola/component/bytecode/agent/AgentJarManifestTest.java`

**Interfaces:**
- Produces `BytecodeAgent.premain(String, Instrumentation)` only.
- Precedence: Agent args > external YAML > system/env > defaults.

- [ ] **Step 1: Write failing configuration/filter/manifest tests**

Assert precedence, malformed YAML failure policy, redacted digest output, explicit include requirement, immutable hard exclusions, unmatched-filter path without constructing `ClassReader`, and manifest values:

```text
Premain-Class: top.egon.cola.component.bytecode.agent.BytecodeAgent
Can-Redefine-Classes: false
Can-Retransform-Classes: false
```

Assert `Agent-Class` is absent and ASM/SnakeYAML packages are relocated.

- [ ] **Step 2: Run Agent tests**

```bash
./mvnw -B -ntp -pl egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-agent -am test
```

Expected: FAIL because Agent foundation is absent.

- [ ] **Step 3: Implement config/filter/state**

`AgentStateStore` allows `DISABLED -> STARTING -> ACTIVE|DEGRADED|FAILED`, keeps aggregate counters and a bounded recent-failure deque, and publishes status through bridge. Hard exclusions include the exact packages in the design plus the relocated Agent package.

- [ ] **Step 4: Configure the shaded JAR and premain**

Shade ASM and SnakeYAML beneath `top.egon.cola.component.bytecode.agent.shaded`. Attach only the shaded executable artifact as the main Agent JAR. `premain` loads config, registers one transformer with `canRetransform=false`, transitions state, and logs one sanitized startup summary. Run Step 2; expected PASS.

- [ ] **Step 5: Commit**

```bash
git add egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-agent
git commit -m "feat(bytecode): add premain agent foundation"
```

## Task 5: Enhancement Planner, Frame Resolver, And Executor Call-Site Rewrite

**Files:**
- Create: `egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-core/src/main/java/top/egon/cola/component/bytecode/core/enhance/ClassEnhancementPlanner.java`
- Create: `egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-core/src/main/java/top/egon/cola/component/bytecode/core/enhance/ClassEnhancementPlan.java`
- Create: `egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-core/src/main/java/top/egon/cola/component/bytecode/core/enhance/MethodEnhancementPlan.java`
- Create: `egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-core/src/main/java/top/egon/cola/component/bytecode/core/enhance/EnhancementFeature.java`
- Create: `egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-core/src/main/java/top/egon/cola/component/bytecode/core/enhance/DuplicateEnhancementDetector.java`
- Create: `egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-core/src/main/java/top/egon/cola/component/bytecode/core/enhance/executor/ExecutorCallSiteEnhancer.java`
- Create: `egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-core/src/main/java/top/egon/cola/component/bytecode/core/hierarchy/ClassHierarchyResolver.java`
- Create: `egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-core/src/main/java/top/egon/cola/component/bytecode/core/hierarchy/EgonClassWriter.java`
- Modify: `egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-agent/src/main/java/top/egon/cola/component/bytecode/agent/transform/CompositeBytecodeTransformer.java`
- Test: `egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-core/src/test/java/top/egon/cola/component/bytecode/core/enhance/executor/ExecutorCallSiteEnhancerTest.java`
- Test: `egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-agent/src/test/java/top/egon/cola/component/bytecode/agent/transform/CompositeBytecodeTransformerTest.java`

**Interfaces:**
- Produces one `ClassEnhancementPlan` per class and stable 64-bit call-site IDs.
- Rewrites only four exact JVM descriptors to `EgonExecutorBridge`.

- [ ] **Step 1: Write failing bytecode fixture tests**

Compile fixtures for all four call sites, non-interface owners, unrelated overloads, try/catch, lambda submissions, virtual-thread executors, duplicate input, and ID collision injection. Transform, run `CheckClassAdapter`, load in an isolated ClassLoader, and assert behavior/Future identity.

- [ ] **Step 2: Run focused transformer tests**

```bash
./mvnw -B -ntp -pl egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-core -am -Dsurefire.failIfNoSpecifiedTests=false -Dtest=ExecutorCallSiteEnhancerTest test
```

Expected: FAIL because enhancer/planner types are absent.

- [ ] **Step 3: Implement planning and call-site IDs**

Hash owner class, enclosing method name/descriptor, opcode, target owner/name/descriptor, and instruction ordinal with SHA-256; truncate to signed 64 bits. Register `CallSiteMetadata` against the transform ClassLoader and fail the class on an in-loader collision.

- [ ] **Step 4: Implement stack rewrite and duplicate prevention**

The enhancer preserves receiver/arguments, pushes `Owner.class` and ID in the bridge descriptor's required order, then replaces the interface invocation with the corresponding static bridge call. Detect existing fixed bridge signatures before rewrite. `EgonClassWriter` resolves common superclasses from target-loader class resources and never calls `Class.forName`.

Run Step 2 plus Agent transformer test. Expected: PASS with `-Xverify:all` fixture loading.

- [ ] **Step 5: Commit**

```bash
git add egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-core \
  egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-agent
git commit -m "feat(bytecode): rewrite executor submissions"
```

## Task 6: Spring Starter, MDC, Micrometer, DTP Deduplication, And Status Endpoint

**Files:**
- Create: `egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-starter/src/main/java/top/egon/cola/component/bytecode/starter/BytecodeAutoConfiguration.java`
- Create: `egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-starter/src/main/java/top/egon/cola/component/bytecode/starter/BytecodeProperties.java`
- Create: `egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-starter/src/main/java/top/egon/cola/component/bytecode/starter/BytecodeRuntimeRegistrar.java`
- Create: `egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-starter/src/main/java/top/egon/cola/component/bytecode/starter/BytecodeStartupValidator.java`
- Create: `egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-starter/src/main/java/top/egon/cola/component/bytecode/starter/context/MdcContextCarrier.java`
- Create: `egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-starter/src/main/java/top/egon/cola/component/bytecode/starter/metrics/MicrometerExecutorEventSink.java`
- Create: `egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-starter/src/main/java/top/egon/cola/component/bytecode/starter/dtp/DtpTaskDetector.java`
- Create: `egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-starter/src/main/java/top/egon/cola/component/bytecode/starter/actuator/EgonBytecodeEndpoint.java`
- Create: `egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-starter/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
- Test: `egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-starter/src/test/java/top/egon/cola/component/bytecode/starter/BytecodeAutoConfigurationTest.java`
- Test: `egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-starter/src/test/java/top/egon/cola/component/bytecode/starter/BytecodeDependencyBoundaryTest.java`
- Test: `egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-starter/src/test/java/top/egon/cola/component/bytecode/starter/context/MdcContextCarrierTest.java`
- Test: `egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-starter/src/test/java/top/egon/cola/component/bytecode/starter/actuator/EgonBytecodeEndpointTest.java`

**Interfaces:**
- Registers the runtime dispatcher for the application ClassLoader and unregisters on context close.
- Endpoint ID is `egonbytecode` and exists only when Actuator is already present.

- [ ] **Step 1: Write failing ApplicationContextRunner tests**

Assert enabled/disabled behavior, missing Agent handling, protocol mismatch failure, duplicate registration failure, MDC restoration, bounded Micrometer tags, DTP no-double-wrap, endpoint conditionality, and requested/effective status values.

- [ ] **Step 2: Run starter tests**

```bash
./mvnw -B -ntp -pl egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-starter -am test
```

Expected: FAIL because starter wiring is absent.

- [ ] **Step 3: Implement properties and registration**

`BytecodeProperties` uses prefix `egon.cola.component.bytecode`, contains executor enabled/sampling/metrics, runtime failure capacity, and endpoint enabled. Transform-time include/exclude properties are status-only in Spring and cannot claim to alter already loaded classes.

- [ ] **Step 4: Implement optional adapters and endpoint**

Use `@ConditionalOnClass` and optional Maven dependencies. Metrics names are the five names from the design and bounded tags are `executor`, `executor_type`, `result`, `exception_group`, `virtual_thread`. Endpoint returns versions/state/features/counts/failures/dispatcher status and no raw patterns/arguments/values.

Run Step 2 and dependency-tree exclusion checks. Expected: PASS; starter alone does not pull Actuator/DTP/Redisson/Spring Web/Access Guard/Method Extension.

- [ ] **Step 5: Commit**

```bash
git add egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-starter
git commit -m "feat(bytecode): auto-configure executor runtime"
```

## Task 7: Forked Agent, Surefire/Failsafe, Concurrency, And Performance Tests

**Files:**
- Add: `egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-test/src/test/java/top/egon/cola/component/bytecode/test/agent/ForkedAgentProcess.java`
- Add: `egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-test/src/test/java/top/egon/cola/component/bytecode/test/agent/ExecutorAgentIntegrationTest.java`
- Add: `egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-test/src/test/java/top/egon/cola/component/bytecode/test/agent/AgentClassLoaderLeakTest.java`
- Add: `egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-test/src/test/java/top/egon/cola/component/bytecode/test/agent/AgentJava25CompatibilityTest.java`
- Create Invoker fixture: `egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-test/src/it/agent-surefire/`
- Create Invoker fixture: `egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-test/src/it/agent-failsafe/`
- Create benchmark: `egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-benchmark/src/main/java/top/egon/cola/component/bytecode/benchmark/ExecutorEnhancementBenchmark.java`

**Interfaces:**
- Every forked process uses `-Xverify:all -javaagent:<built-agent.jar>` and exits itself.
- Performance targets: 1,000 representative transforms <=1 second; Executor submission overhead <5 microseconds on controlled hardware.

- [ ] **Step 1: Add failing fork harness and assertions**

Cover plain JVM and Spring Boot loader, all four call sites, success/failure/rejection, original Future `assertSame`, 100-way concurrency, nested tasks, interrupt/cancel behavior, virtual threads, MDC/custom carriers, DTP wrappers, dispatcher unavailable, dispatcher removed, and weak ClassLoader cleanup.

- [ ] **Step 2: Run integration tests**

```bash
./mvnw -B -ntp -pl egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-test -am verify
```

Expected before fixtures/harness are complete: FAIL.

- [ ] **Step 3: Implement process isolation and Invoker fixtures**

Build command arguments as a `List<String>`; do not invoke a shell. Capture bounded stdout/stderr, enforce a 60-second timeout, destroy on timeout, and assert the Agent startup summary contains version/protocol/effective features without raw include patterns.

- [ ] **Step 4: Implement JMH smoke and rerun**

Benchmark unmatched filter, 1,000 transformations, direct submit baseline, context-only, and context+metrics. Shared CI records relative results; absolute limits run only under the controlled profile.

Expected: forked/Invoker tests PASS on the available JDK; Java-25-only test skips with an explicit assumption when JDK 25 is not installed locally.

- [ ] **Step 5: Commit**

```bash
git add egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-test \
  egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-benchmark
git commit -m "test(bytecode): verify executor agent semantics"
```

## Task 8: README, CI Matrix, Release Shape, And Final Gate

**Files:**
- Modify: `egon-cola-components/egon-cola-component-bytecode/README.md`
- Modify: `.github/workflows/ci.yaml`
- Modify: `.github/workflows/ci_java_compatibility.yaml`
- Modify: Agent/starter POM release plugin configuration if source/Javadoc/shaded artifact checks require it.

**Interfaces:**
- Agent manifest records implementation and bridge protocol versions.
- Release publishes API/bridge/core/runtime/Agent/starter/plugin; test/benchmark are skipped.

- [ ] **Step 1: Add release/dependency verification assertions**

Add tests/scripts that inspect Agent manifest, one shaded Agent artifact, source artifacts, BOM entries, and absence of forbidden starter transitive dependencies.

- [ ] **Step 2: Run pre-documentation final suite**

```bash
./mvnw -B -ntp -pl egon-cola-components/egon-cola-component-bytecode -am test
./mvnw -B -ntp -pl egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-test -am verify
```

Expected: implementation tests PASS; README/CI contract assertion fails until updated.

- [ ] **Step 3: Document exact installation and limitations**

README covers shaded Agent path, premain arguments/YAML precedence, required includes, immutable exclusions, requested/effective runtime settings, four supported call sites, original Future guarantee, cancellation limit, DTP behavior, metrics/tags, endpoint, error states, privacy, and no Attach/retransform.

- [ ] **Step 4: Run final validation**

```bash
./mvnw -B -ntp -pl egon-cola-components/egon-cola-component-bytecode -am test
./mvnw -B -ntp -pl egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-test -am verify
./mvnw -B -ntp -f egon-cola-components/pom.xml test
./mvnw -B -ntp -pl egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-starter -am dependency:tree -Dincludes=org.springframework.boot:spring-boot-starter-actuator,top.egon:egon-cola-component-dynamic-thread-pool-starter,org.redisson,org.springframework:spring-web,top.egon:egon-cola-component-access-guard-starter,top.egon:egon-cola-component-method-extension-starter
./mvnw -B -ntp clean integration-test
git diff --check
```

Expected: all tests PASS; filtered dependency tree contains none of the forbidden artifacts for starter-only resolution. Do not run an application or Docker.

- [ ] **Step 5: Commit**

```bash
git add .github/workflows \
  egon-cola-components/egon-cola-component-bytecode
git commit -m "docs(bytecode): document executor agent runtime"
```

## Stage Completion Checklist

- [ ] Only premain is available; manifest omits Agent-Class and retransformation capability.
- [ ] Bridge is JDK-only and passes duplicate/version/weak-reference tests.
- [ ] Runtime is Spring-free and task/context cleanup is exception-safe.
- [ ] Only four approved Executor call sites are rewritten.
- [ ] Underlying Executor is called exactly once and original Future/exception/rejection identities are preserved.
- [ ] DTP tasks are not double wrapped; registry remains immutable.
- [ ] Starter does not transitively add Actuator, DTP, Redisson, Spring Web, Method Extension, or Access Guard.
- [ ] Agent status/startup summary is bounded and sanitized.
- [ ] Java 21/25 forked verification, Surefire/Failsafe, concurrency, virtual-thread, leak, performance, reactor, dependency, and hygiene evidence is recorded.
- [ ] Stop and request review before Method Observation.
