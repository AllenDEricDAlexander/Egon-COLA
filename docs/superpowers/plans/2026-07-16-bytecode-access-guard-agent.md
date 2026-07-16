# Bytecode Access Guard Agent Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add an explicit Access Guard Agent engine for public/private instance methods, public/private static methods, and public/private constructors, with restricted pre-initialization constructor governance and no loss of existing AOP behavior.

**Architecture:** Extract current Access Guard orchestration into one execution service that accepts a `ProceedingJoinPoint`; AOP passes the real join point and Agent mode passes an adapter backed by a direct synthetic-body MethodHandle. Initial class definition structurally wraps approved non-constructor methods so the existing timeout executor can invoke the continuation, while constructors use a separate static pre-initialization guard that supports only key/white/black/rate/fail semantics. A single policy dispatcher enforces Method Extension → Access Guard → Observation → business body.

**Tech Stack:** Java 21 production, Java 21/25 verification, existing Access Guard Spring AOP/Redisson component, AspectJ interfaces, existing bytecode platform/ASM 9.9.1, Spring Boot 3.5.16, JUnit 5.

## Global Constraints

- Complete and merge all four earlier stage plans first.
- Follow `docs/superpowers/specs/2026-07-15-bytecode-enhancement-design.md` exactly.
- AOP remains default. Agent mode is explicit; `enabled=false` always wins.
- Agent supports only public/private instance methods, public/private static methods, and public/private constructors.
- Reject protected/package-private targets and abstract/native/synthetic/bridge/lambda bodies explicitly when annotated.
- Only aggregate `@AccessGuard` gains constructor target and `failStrategy`; all dedicated/compatibility annotations remain method-only.
- Constructors support key resolution, white list, blacklist, rate limiter, fail strategy, events, and bounded diagnostics only.
- Constructors reject timeout, fallback, `returnJson`, instance state, local fallback, and replacement-object semantics.
- Constructor rejection throws `AccessGuardRejectedException` before `this(...)`/`super(...)`.
- Before the dispatcher is ready, constructor failure priority is explicit annotation `failStrategy` > transform-time Access Guard failure mode > built-in `FAIL_OPEN`; `GLOBAL_DEFAULT` selects the transform-time mode and `LOCAL_FALLBACK` is rejected.
- Synchronized method plus effective timeout is invalid. Synchronized without timeout remains supported.
- Reuse existing rule/key/white/black/rate/timeout/reject/event services; do not create parallel business semantics in bytecode modules.
- Existing custom SPIs requiring a real Spring join point remain guaranteed only in AOP; Agent guarantees repository defaults and explicitly Agent-aware extensions.
- Do not add Attach/retransform, database/Flyway changes, UI, browser, Docker, JVM halt, or long-running app startup.
- Use an isolated worktree and one path-scoped commit per task.

## Pattern Boundary

Use Adapter for Agent-backed `ProceedingJoinPoint`, Strategy only for the existing rule/failure services, and Facade-style execution services for normal and constructor paths. Keep the approved rule order explicit; do not introduce a new Chain of Responsibility, handler factory, or inheritance tree because that would duplicate established Access Guard semantics.

## Stable Method And Constructor Interfaces

```java
package top.egon.cola.component.accessguard.execution;

public class AccessGuardExecutionService {
    public Object execute(org.aspectj.lang.ProceedingJoinPoint joinPoint) throws Throwable;
}

public class ConstructorAccessGuardExecutionService {
    public ConstructorGuardResult evaluate(
            java.lang.reflect.Constructor<?> constructor,
            Object[] arguments,
            top.egon.cola.component.accessguard.annotation.FailStrategy transformTimeFailHint
    );
}

public record ConstructorGuardResult(boolean allowed) {
    public static ConstructorGuardResult allow() { return new ConstructorGuardResult(true); }
}
```

Properties add:

```java
public enum AccessGuardEngine { AOP, AGENT, DISABLED }
```

The new rejection exception is:

```java
public class AccessGuardRejectedException extends RuntimeException {
    public AccessGuardRejectedException(String message) { super(message); }
    public AccessGuardRejectedException(String message, Throwable cause) { super(message, cause); }
}
```

## Task 1: Extract Access Guard Execution Service And Preserve AOP Chain

**Files:**
- Create: `egon-cola-components/egon-cola-component-access-guard/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/execution/AccessGuardExecutionService.java`
- Create: `egon-cola-components/egon-cola-component-access-guard/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/execution/AccessGuardFailureHandler.java`
- Modify: `egon-cola-components/egon-cola-component-access-guard/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/aop/AccessGuardAop.java`
- Modify: `egon-cola-components/egon-cola-component-access-guard/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/autoconfigure/AccessGuardAutoConfiguration.java`
- Test: `egon-cola-components/egon-cola-component-access-guard/egon-cola-component-access-guard-starter/src/test/java/top/egon/cola/component/accessguard/execution/AccessGuardExecutionServiceTest.java`
- Modify: `egon-cola-components/egon-cola-component-access-guard/egon-cola-component-access-guard-starter/src/test/java/top/egon/cola/component/accessguard/aop/AccessGuardAopFlowTest.java`

**Interfaces:**
- Consumes current properties/rule/key/white/black/rate/timeout/reject/event services.
- Produces one shared `execute(ProceedingJoinPoint)` orchestration.

- [ ] **Step 1: Write failing service parity tests**

Cover disabled, white-list reject/bypass, blacklist, rate reject/increment, timeout/no-timeout, fallback, pass events, rejection events, fail-open/fail-closed/local-fallback infrastructure failures, business Throwable identity, and business invocation exactly once.

- [ ] **Step 2: Run existing and new flow tests**

```bash
./mvnw -B -ntp -pl egon-cola-components/egon-cola-component-access-guard/egon-cola-component-access-guard-starter -am -Dsurefire.failIfNoSpecifiedTests=false -Dtest=AccessGuardExecutionServiceTest,AccessGuardAopFlowTest test
```

Expected: FAIL because the shared service is absent.

- [ ] **Step 3: Move the exact current chain into the service**

Preserve order:

```text
white list -> blacklist -> rate limiter -> timeout protection -> business invocation
```

Move `reject(...)` and `publish(...)` unchanged in observable behavior. Keep `AccessGuardAop.getOrder()` in the aspect.

`AccessGuardFailureHandler` distinguishes guard-infrastructure failures from business exceptions. Effective fail-open proceeds to the next safe stage, fail-closed throws `AccessGuardRejectedException`, and local-fallback remains delegated to the existing service implementation. Business exceptions from `joinPoint.proceed()` are never reclassified as guard failures.

- [ ] **Step 4: Reduce aspect to delegation and rerun**

Keep the exact existing pointcut expression and reduce the advice body to `return executionService.execute(joinPoint);`.

Run Step 2. Expected: PASS with all existing annotation compatibility tests unchanged.

- [ ] **Step 5: Commit**

```bash
git add egon-cola-components/egon-cola-component-access-guard
git commit -m "refactor(access-guard): share execution chain"
```

## Task 2: Engine Selection And Agent ProceedingJoinPoint Adapter

**Files:**
- Create: `egon-cola-components/egon-cola-component-access-guard/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/autoconfigure/AccessGuardEngine.java`
- Modify: `egon-cola-components/egon-cola-component-access-guard/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/autoconfigure/AccessGuardProperties.java`
- Modify: `egon-cola-components/egon-cola-component-access-guard/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/autoconfigure/AccessGuardAutoConfiguration.java`
- Create: `egon-cola-components/egon-cola-component-access-guard/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/agent/AgentProceedingJoinPoint.java`
- Create: `egon-cola-components/egon-cola-component-access-guard/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/agent/AgentMethodSignature.java`
- Test: `egon-cola-components/egon-cola-component-access-guard/egon-cola-component-access-guard-starter/src/test/java/top/egon/cola/component/accessguard/autoconfigure/AccessGuardAutoConfigurationTest.java`
- Test: `egon-cola-components/egon-cola-component-access-guard/egon-cola-component-access-guard-starter/src/test/java/top/egon/cola/component/accessguard/agent/AgentProceedingJoinPointTest.java`

**Interfaces:**
- Missing engine maps to AOP; disabled wins; aspect exists only for effective AOP.
- Adapter implements `ProceedingJoinPoint` and invokes one direct MethodHandle continuation.

- [ ] **Step 1: Write failing engine and adapter tests**

Assert default/AOP, AGENT, DISABLED, enabled=false precedence, service availability, aspect exclusivity, instance/static targets, `proceed()` and `proceed(newArgs)`, argument cloning, method signature fields, and exact continuation Throwable.

- [ ] **Step 2: Run focused tests**

```bash
./mvnw -B -ntp -pl egon-cola-components/egon-cola-component-access-guard/egon-cola-component-access-guard-starter -am -Dsurefire.failIfNoSpecifiedTests=false -Dtest=AccessGuardAutoConfigurationTest,AgentProceedingJoinPointTest test
```

Expected: FAIL because engine/adapter are absent.

- [ ] **Step 3: Implement effective engine and conditional aspect**

Defaults: `enabled=true`, `engine=AOP`; existing AOP order remains. Execution service and supporting services exist in AOP/AGENT, but Aspect exists only for AOP.

- [ ] **Step 4: Implement complete join-point adapter**

`getThis()`/`getTarget()` return instance or null for static; `getArgs()` clones; `getSignature()` returns `AgentMethodSignature`; `toShortString`/`toLongString` are deterministic; `getKind()` is `METHOD_EXECUTION`; source/static-part values contain no application objects beyond the ephemeral invocation. `proceed` calls the direct handle once with receiver prepended only for instance methods.

Run Step 2. Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add egon-cola-components/egon-cola-component-access-guard
git commit -m "feat(access-guard): add agent engine boundary"
```

## Task 3: Static Method Rule, Key, Timeout, And Fallback Support

**Files:**
- Modify: `egon-cola-components/egon-cola-component-access-guard/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/support/AopMethodResolver.java`
- Modify: `egon-cola-components/egon-cola-component-access-guard/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/key/DefaultAccessKeyResolver.java`
- Modify: `egon-cola-components/egon-cola-component-access-guard/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/reject/ReflectionFallbackInvoker.java`
- Modify if adapter assumptions require correction: `egon-cola-components/egon-cola-component-access-guard/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/circuitbreaker/ThreadPoolTimeoutCircuitBreakerExecutor.java`
- Test: `egon-cola-components/egon-cola-component-access-guard/egon-cola-component-access-guard-starter/src/test/java/top/egon/cola/component/accessguard/reject/RejectResponseInvokerTest.java`
- Test: `egon-cola-components/egon-cola-component-access-guard/egon-cola-component-access-guard-starter/src/test/java/top/egon/cola/component/accessguard/key/DefaultAccessKeyResolverTest.java`
- Create test: `egon-cola-components/egon-cola-component-access-guard/egon-cola-component-access-guard-starter/src/test/java/top/egon/cola/component/accessguard/execution/StaticAccessGuardExecutionTest.java`

**Interfaces:**
- Static method target is null and declaring class comes from `Method`.
- Static fallback must be static; instance fallback is a configuration error.

- [ ] **Step 1: Write failing static flow tests**

Cover argument/header/IP/all keys, white/black/rate, pass/reject, JSON direct return, static fallback with same args/context/no args, instance fallback rejection, timeout continuation, and exact static business Throwable.

- [ ] **Step 2: Run focused tests**

```bash
./mvnw -B -ntp -pl egon-cola-components/egon-cola-component-access-guard/egon-cola-component-access-guard-starter -am -Dsurefire.failIfNoSpecifiedTests=false -Dtest=StaticAccessGuardExecutionTest,RejectResponseInvokerTest,DefaultAccessKeyResolverTest test
```

Expected: new static cases FAIL because fallback assumes `target.getClass()`.

- [ ] **Step 3: Make default services target-null safe**

Use `target != null ? target.getClass() : method.getDeclaringClass()`. For null target, require `Modifier.isStatic(fallback.getModifiers())` and invoke with null. Keep AOP instance behavior unchanged.

- [ ] **Step 4: Rerun full Access Guard starter suite**

```bash
./mvnw -B -ntp -pl egon-cola-components/egon-cola-component-access-guard/egon-cola-component-access-guard-starter -am test
```

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add egon-cola-components/egon-cola-component-access-guard
git commit -m "feat(access-guard): support static agent targets"
```

## Task 4: Constructor Annotation, Executable Metadata, And Validation

**Files:**
- Modify: `egon-cola-components/egon-cola-component-access-guard/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/annotation/AccessGuard.java`
- Modify: `egon-cola-components/egon-cola-component-access-guard/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/config/AccessGuardAnnotationResolver.java`
- Modify: `egon-cola-components/egon-cola-component-access-guard/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/config/AccessGuardRuleResolver.java`
- Modify: `egon-cola-components/egon-cola-component-access-guard/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/support/MethodSignatureKey.java`
- Create: `egon-cola-components/egon-cola-component-access-guard/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/support/ExecutableSignatureKey.java`
- Create: `egon-cola-components/egon-cola-component-access-guard/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/exception/AccessGuardRejectedException.java`
- Create: `egon-cola-components/egon-cola-component-access-guard/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/execution/ConstructorAccessGuardValidator.java`
- Test: `egon-cola-components/egon-cola-component-access-guard/egon-cola-component-access-guard-starter/src/test/java/top/egon/cola/component/accessguard/annotation/AccessGuardAnnotationTest.java`
- Test: `egon-cola-components/egon-cola-component-access-guard/egon-cola-component-access-guard-starter/src/test/java/top/egon/cola/component/accessguard/execution/ConstructorAccessGuardValidatorTest.java`

**Interfaces:**
- `AccessGuardAnnotationResolver.resolve(Executable)` accepts Method or Constructor.
- Only Constructor + aggregate `@AccessGuard` is valid.

- [ ] **Step 1: Write failing annotation/validation tests**

Assert aggregate target METHOD+CONSTRUCTOR and `failStrategy=GLOBAL_DEFAULT`; every dedicated/Do annotation remains METHOD-only. Validate public/private constructors and reject protected/package, timeout, fallback, returnJson, LOCAL_FALLBACK, and instance-state key expressions.

- [ ] **Step 2: Run focused tests**

```bash
./mvnw -B -ntp -pl egon-cola-components/egon-cola-component-access-guard/egon-cola-component-access-guard-starter -am -Dsurefire.failIfNoSpecifiedTests=false -Dtest=AccessGuardAnnotationTest,ConstructorAccessGuardValidatorTest test
```

Expected: FAIL because constructor support is absent.

- [ ] **Step 3: Extend aggregate annotation additively**

```java
@Target({ElementType.METHOD, ElementType.CONSTRUCTOR})
public @interface AccessGuard {
    // existing attributes unchanged
    FailStrategy failStrategy() default FailStrategy.GLOBAL_DEFAULT;
}
```

Apply `failStrategy` to method rules too without changing current default behavior.

- [ ] **Step 4: Add Executable rule/signature resolution and rerun**

Constructor default names hash declaring class + `<init>` + descriptor. Parameter names use `DefaultParameterNameDiscoverer`; property paths inspect parameter objects only. Header/IP/all remain valid. `AccessGuardRuleResolver` converts `GLOBAL_DEFAULT` to `AccessGuardProperties.failStrategy` before services execute. Dynamic overrides are validated again when dispatcher registers.

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add egon-cola-components/egon-cola-component-access-guard
git commit -m "feat(access-guard): define constructor guard contract"
```

## Task 5: Structural Wrappers For Public/Private Instance And Static Methods

**Files:**
- Create: `egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-core/src/main/java/top/egon/cola/component/bytecode/core/enhance/accessguard/AccessGuardMatcher.java`
- Create: `egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-core/src/main/java/top/egon/cola/component/bytecode/core/enhance/accessguard/AccessGuardMethodWrapper.java`
- Create: `egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-core/src/main/java/top/egon/cola/component/bytecode/core/enhance/accessguard/SyntheticBodyName.java`
- Create: `egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-core/src/main/java/top/egon/cola/component/bytecode/core/enhance/accessguard/GovernanceAnnotationFilter.java`
- Modify: `egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-core/src/main/java/top/egon/cola/component/bytecode/core/enhance/ClassEnhancementPlanner.java`
- Modify: `egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-core/src/main/java/top/egon/cola/component/bytecode/core/enhance/MethodEnhancementPlan.java`
- Modify: `egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-agent/src/main/java/top/egon/cola/component/bytecode/agent/AgentConfiguration.java`
- Test: `egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-core/src/test/java/top/egon/cola/component/bytecode/core/enhance/accessguard/AccessGuardMatcherTest.java`
- Test: `egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-core/src/test/java/top/egon/cola/component/bytecode/core/enhance/accessguard/AccessGuardMethodWrapperTest.java`

**Interfaces:**
- Wrapper keeps original public API/method identity; private synthetic body has deterministic collision-checked name.
- Wrapper calls `EgonPolicyBridge.invokeGuarded` with a direct MethodHandle constant.

- [ ] **Step 1: Write failing target/wrapper fixtures**

Cover public/private instance/static, final, synchronized, generic metadata, annotations/parameter annotations, checked exceptions, primitive/reference/void returns, recursion/self-call, existing name collision, and explicit protected/package/abstract/native/synthetic/bridge rejection.

- [ ] **Step 2: Run core tests**

```bash
./mvnw -B -ntp -pl egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-core -am -Dsurefire.failIfNoSpecifiedTests=false -Dtest=AccessGuardMatcherTest,AccessGuardMethodWrapperTest test
```

Expected: FAIL because matcher/wrapper is absent.

- [ ] **Step 3: Implement structural split**

Keep original name/descriptor/signature/exceptions/visibility/annotations/parameter annotations on wrapper. Move code to private synthetic `egon$guard$<hash>`, remove `ACC_SYNCHRONIZED` and governance annotations from body, and load a `CONSTANT_MethodHandle` using `REF_invokeSpecial` for instance or `REF_invokeStatic` for static.

- [ ] **Step 4: Enforce combined feature order and rerun**

One class plan creates one wrapper. Runtime metadata feature bits determine whether Method Extension evaluates first. Observation instrumentation applies to the synthetic business body, not wrapper rejection. Internal continuation bypass token prevents duplicate policy; real recursive calls re-enter wrapper.

Run Step 2 with `CheckClassAdapter` and `-Xverify:all`. Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-core \
  egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-agent
git commit -m "feat(bytecode): wrap access guarded methods"
```

## Task 6: Agent Runtime Adapter And Cross-Feature Policy Dispatcher

**Files:**
- Modify: `egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-starter/pom.xml` to add optional Access Guard starter dependency
- Create: `egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-starter/src/main/java/top/egon/cola/component/bytecode/starter/accessguard/AccessGuardAgentAutoConfiguration.java`
- Create: `egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-starter/src/main/java/top/egon/cola/component/bytecode/starter/accessguard/AccessGuardRuntimeAdapter.java`
- Create: `egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-starter/src/main/java/top/egon/cola/component/bytecode/starter/accessguard/CombinedPolicyDispatcher.java`
- Modify: `egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-starter/src/main/java/top/egon/cola/component/bytecode/starter/BytecodeStartupValidator.java`
- Modify: `egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-starter/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
- Test: `egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-starter/src/test/java/top/egon/cola/component/bytecode/starter/accessguard/AccessGuardRuntimeAdapterTest.java`
- Test: `egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-starter/src/test/java/top/egon/cola/component/bytecode/starter/accessguard/CombinedPolicyDispatcherTest.java`

**Interfaces:**
- Resolves Method from application-loader metadata and creates `AgentProceedingJoinPoint`.
- Combined order is Method Extension then Access Guard; Observation remains inside continuation.

- [ ] **Step 1: Write failing adapter/order tests**

Cover AOP/AGENT/DISABLED wiring, Agent absent/mismatch, instance/static invocation, all rule outcomes, timeout, fallback, Handler rejection before guard, guard rejection before observation, allowed observation around body, recursion vs continuation, and event counts.

- [ ] **Step 2: Run starter adapter tests**

```bash
./mvnw -B -ntp -pl egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-starter -am -Dsurefire.failIfNoSpecifiedTests=false -Dtest=AccessGuardRuntimeAdapterTest,CombinedPolicyDispatcherTest test
```

Expected: FAIL because adapters are absent.

- [ ] **Step 3: Implement runtime adapter**

Resolve method through application `ClassValue`, create ephemeral PJP with target/args/handle, and call the shared Access Guard service. Map neither arguments nor result into status/events. Runtime service failures use effective Access Guard fail strategy.

- [ ] **Step 4: Implement startup/effective configuration validation**

Require attached Agent/protocol/ACCESS_GUARD capability in Agent mode. Reject unsupported transformed targets, instance fallback for static, and synchronized + effective timeout. Revalidate runtime rule overrides on registration and before invoking a changed rule.

Run Step 2. Expected: PASS; optional Access Guard is absent from starter-only consumer dependency tree.

- [ ] **Step 5: Commit**

```bash
git add egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-starter
git commit -m "feat(bytecode): bridge access guard runtime"
```

## Task 7: Pre-Initialization Constructor Guard And Limited Execution Service

**Files:**
- Create: `egon-cola-components/egon-cola-component-access-guard/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/execution/ConstructorAccessGuardExecutionService.java`
- Create: `egon-cola-components/egon-cola-component-access-guard/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/execution/ConstructorGuardResult.java`
- Create: `egon-cola-components/egon-cola-component-access-guard/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/key/ExecutableAccessKeyResolver.java`
- Modify: `egon-cola-components/egon-cola-component-access-guard/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/key/DefaultAccessKeyResolver.java`
- Modify: `egon-cola-components/egon-cola-component-access-guard/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard/autoconfigure/AccessGuardAutoConfiguration.java`
- Create: `egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-core/src/main/java/top/egon/cola/component/bytecode/core/enhance/accessguard/ConstructorGuardEnhancer.java`
- Modify: `egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-starter/src/main/java/top/egon/cola/component/bytecode/starter/accessguard/AccessGuardRuntimeAdapter.java`
- Test: `egon-cola-components/egon-cola-component-access-guard/egon-cola-component-access-guard-starter/src/test/java/top/egon/cola/component/accessguard/execution/ConstructorAccessGuardExecutionServiceTest.java`
- Test: `egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-core/src/test/java/top/egon/cola/component/bytecode/core/enhance/accessguard/ConstructorGuardEnhancerTest.java`

**Interfaces:**
- Generated constructor calls `EgonPolicyBridge.guardConstructor(DeclaringClass.class, methodId, boxedArgs, failHint)` before any use of `this`.
- Reject throws `AccessGuardRejectedException`; allow continues original bytecode.

- [ ] **Step 1: Write failing constructor service and verifier fixtures**

Cover public/private, protected/package rejection, args/property/header/IP/all keys, white/black/rate, rejection before superclass side effect, `this(...)` chains, parent/delegated failure, final fields, fail-open/fail-closed before runtime ready, runtime service failure, events, and unsupported configuration.

- [ ] **Step 2: Run focused tests with verifier**

```bash
./mvnw -B -ntp -pl egon-cola-components/egon-cola-component-access-guard/egon-cola-component-access-guard-starter -am -Dsurefire.failIfNoSpecifiedTests=false -Dtest=ConstructorAccessGuardExecutionServiceTest test
./mvnw -B -ntp -pl egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-core -am -Dsurefire.failIfNoSpecifiedTests=false -Dtest=ConstructorGuardEnhancerTest -DargLine=-Xverify:all test
```

Expected: FAIL because constructor service/enhancer is absent.

- [ ] **Step 3: Implement limited constructor chain**

Resolve aggregate rule, key, white list, blacklist, and rate limiter. Publish pass/reject events. Never call timeout/reject-response/fallback services. On reject or fail-closed service failure throw `AccessGuardRejectedException`; fail-open returns allow.

- [ ] **Step 4: Insert legal pre-init bytecode and rerun**

At `<init>` entry load only declaring Class literal, method ID, boxed parameters, and enum fail hint. Do not load/pass `this`, access fields, or create a replacement result. Each separately annotated constructor in a `this(...)` chain evaluates once.

At transform time map explicit `FAIL_OPEN`/`FAIL_CLOSED` directly to `BridgeFailHint`; map `GLOBAL_DEFAULT` to the configured transform-time Access Guard mode, falling back to `FAIL_OPEN` when absent. Reject `LOCAL_FALLBACK` before bytecode emission. The runtime-ready path resolves dynamic/global rule overrides; the bridge hint governs only the no-dispatcher path.

Expected: PASS under `CheckClassAdapter` and `-Xverify:all`.

- [ ] **Step 5: Commit**

```bash
git add egon-cola-components/egon-cola-component-access-guard \
  egon-cola-components/egon-cola-component-bytecode
git commit -m "feat(bytecode): guard constructor entry"
```

## Task 8: Synchronization, Fatal State, Forked Matrix, Documentation, And Final Gate

**Files:**
- Modify: `egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-core/src/main/java/top/egon/cola/component/bytecode/core/enhance/accessguard/AccessGuardMethodWrapper.java`
- Modify: `egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-agent/src/main/java/top/egon/cola/component/bytecode/agent/AgentStateStore.java`
- Modify: `egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-starter/src/main/java/top/egon/cola/component/bytecode/starter/BytecodeStartupValidator.java`
- Modify: `egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-starter/src/main/java/top/egon/cola/component/bytecode/starter/actuator/EgonBytecodeEndpoint.java`
- Add: `egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-test/src/test/java/top/egon/cola/component/bytecode/test/agent/AccessGuardAgentIntegrationTest.java`
- Add fixture: `egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-test/src/it/access-guard-spring/`
- Modify: `egon-cola-components/egon-cola-component-access-guard/README.md`
- Modify: `egon-cola-components/egon-cola-component-bytecode/README.md`
- Modify: `.github/workflows/ci.yaml`
- Modify: `.github/workflows/ci_java_compatibility.yaml`

**Interfaces:**
- Synchronized wrapper retains `ACC_SYNCHRONIZED`; synthetic body removes it; timeout must be false.
- Fail-closed transform failure marks Agent fatal and starter prevents context completion without halting JVM.

- [ ] **Step 1: Add failing full matrix**

Cover approved method/constructor matrix, unsupported explicit targets, synchronized no-timeout/timeout rejection, static fallback, normal timeout, constructor limits, fail-open/closed, fatal state endpoint/startup, AOP regression/parity, Method Extension/Observation order, recursion, proxy coexistence, MyBatis/JPA class loading, Java 21/25, and privacy.

- [ ] **Step 2: Run Access Guard and forked suites**

```bash
./mvnw -B -ntp -pl egon-cola-components/egon-cola-component-access-guard -am test
./mvnw -B -ntp -pl egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-test -am verify
```

Expected before final integration: new cases FAIL.

- [ ] **Step 3: Complete fatal/synchronization semantics and docs**

Transform failure policy: fail-open leaves class unchanged and records high-priority failure; fail-closed marks fatal. Transformer exceptions are never described as class-load termination. README documents exact matrix, constructor pre-init semantics, `this(...)` chain behavior, unsupported properties, infrastructure-constructor warning, synchronized restriction, SPI boundary, and AOP default.

- [ ] **Step 4: Run final validation**

```bash
./mvnw -B -ntp -pl egon-cola-components/egon-cola-component-access-guard -am test
./mvnw -B -ntp -pl egon-cola-components/egon-cola-component-method-extension -am test
./mvnw -B -ntp -pl egon-cola-components/egon-cola-component-bytecode -am test
./mvnw -B -ntp -pl egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-test -am verify
./mvnw -B -ntp -f egon-cola-components/pom.xml test
./mvnw -B -ntp -pl egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-starter -am dependency:tree -Dincludes=top.egon:egon-cola-component-access-guard-starter,org.redisson,org.springframework:spring-web
./mvnw -B -ntp clean integration-test
git diff --check
```

Expected: all suites PASS; starter-only dependency check contains no Access Guard/Redisson/Spring Web unless explicitly requested. Do not start the project.

- [ ] **Step 5: Commit**

```bash
git add .github/workflows \
  egon-cola-components/egon-cola-component-access-guard \
  egon-cola-components/egon-cola-component-bytecode
git commit -m "test(bytecode): verify access guard agent"
```

## Stage Completion Checklist

- [ ] Existing Access Guard AOP behavior and tests remain valid; AOP is default.
- [ ] Agent/AOP are mutually exclusive and disabled wins.
- [ ] Normal method matrix is exactly public/private instance/static; protected/package and unsupported bytecode targets fail explicitly.
- [ ] Existing rule chain/services are reused through real or Agent PJP.
- [ ] Static target/fallback/timeout behavior passes and instance fallback is rejected.
- [ ] Synchronized no-timeout preserves monitor semantics; timeout is rejected.
- [ ] Only aggregate annotation supports public/private constructors.
- [ ] Constructor guard runs before initialization, never passes `this`, and supports only key/white/black/rate/fail/events.
- [ ] Constructor timeout/fallback/JSON/local fallback/replacement/instance state are rejected.
- [ ] Method Extension → Access Guard → Observation → business order and event counts pass.
- [ ] Fail-closed fatal state prevents Spring context completion but never calls JVM halt.
- [ ] Java 21/25, `-Xverify:all`, forked Agent, AOP parity, proxies/framework loading, dependency, reactor, privacy, and hygiene evidence is recorded.
- [ ] Stop and hand the completed selected scope back to the user.
