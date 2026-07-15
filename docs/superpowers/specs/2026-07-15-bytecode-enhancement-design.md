# Egon-COLA Bytecode Enhancement Design

## 1. Context

Egon-COLA needs a bytecode platform that covers five selected capabilities:

1. build-time COLA architecture validation;
2. runtime `Executor` submission enhancement;
3. method observation outside Spring proxy boundaries;
4. an Agent engine for the existing Method Extension component;
5. an Agent engine for the existing Access Guard component.

The source requirement document proposes a broader platform that also includes
dynamic Attach, retransformation, `CompletableFuture` call-site enhancement,
executor-creation discovery, scheduled-task interception, reactive return types,
dynamic transform filters, and additional diagnostic tooling. Those extensions
are not part of this design.

This design establishes one shared bytecode foundation, but each selected
capability is delivered and accepted independently. The implementation must not
be attempted as one large change.

The repository baseline is Java 21, Spring Boot 3.5.x, Maven, and the existing
starter-centric component structure under `egon-cola-components`.

## 2. Confirmed Decisions

The following decisions are authoritative for the implementation.

1. Production deployments may add `-javaagent` and distribute the Agent JAR.
2. Only startup instrumentation through `premain` is supported. Dynamic Attach,
   `agentmain`, class redefinition, and class retransformation are excluded.
3. Delivery is staged in this order:
   - Architecture Maven Plugin;
   - Agent foundation and Executor enhancement;
   - Method Observation;
   - Method Extension Agent;
   - Access Guard Agent.
4. A stage is not started automatically after the previous stage. Each stage is
   implemented, reviewed, validated, committed, and handed back for confirmation.
5. The Architecture Maven Plugin immediately replaces the current ArchUnit tests
   in the light, web, and service archetypes. Only ARCH-001 through ARCH-010 are
   retained; repository-specific ArchUnit rules are intentionally removed.
6. Add a separate JDK-only `bridge` artifact. It is the only type boundary shared
   by the Agent's system class loader and application class loaders.
7. The components BOM manages the public `api`, `bridge`, `runtime`, `agent`, and
   `starter` artifacts. The Maven Plugin always declares an explicit
   `${egon-cola.version}` because dependency management does not manage build
   plugins.
8. Use `top.egon.cola.component.bytecode.*` as the Java package root and
   `egon.cola.component.bytecode.*` as the Spring configuration prefix.
9. Class/method/line locations in architecture findings are optional because not
   every dependency has method-level or debug-line provenance.
10. V1 incremental architecture scanning uses content hashes only. Timestamp,
    Git-diff, and Maven incremental selectors are excluded.
11. A Transformer fatal condition records Agent fatal state. The Spring Starter
    prevents `ApplicationContext` completion. The Agent does not call
    `Runtime.halt` and does not claim JVM-level startup termination.
12. Executor enhancement returns the original `Future`. Exact pre-start
    cancellation metrics are excluded rather than changing Future identity.
13. Existing Method Extension and Access Guard components remain AOP by default.
    Agent mode is explicit, and `enabled=false` always wins.
14. Cross-feature execution order is fixed as:

    ```text
    Method Extension -> Access Guard -> Method Observation -> business body
    ```

15. Method Extension Agent supports concrete instance methods with public,
    protected, package-private, or private visibility. It does not support static
    methods, constructors, abstract methods, or native methods.
16. Method Observation supports concrete instance methods and static methods.
    Constructor observation is explicit and starts after the first successful
    `this(...)` or `super(...)` initialization call. It excludes `<clinit>`,
    abstract methods, and native methods.
17. Access Guard Agent supports only public or private instance methods, public
    or private static methods, and public or private constructors. Protected and
    package-private targets are unsupported.
18. Only the aggregate `@AccessGuard` annotation is extended to constructors.
    Dedicated and compatibility annotations remain method-only.
19. Constructor Access Guard is a pre-initialization guard. It supports access
    key resolution, white list, blacklist, rate limiting, and fail strategy. It
    does not support business-body timeout, fallback methods, or `returnJson`.
20. Constructor runtime-not-ready behavior follows the effective fail strategy:
    fail-open proceeds and fail-closed throws before `this(...)` or `super(...)`.
21. Synchronized methods may use Access Guard only when timeout protection is
    disabled. Synchronized plus timeout is an invalid configuration.
22. A minimal conditional Actuator endpoint is included. The bytecode starter
    does not transitively bring Actuator into applications.
23. Stable external extension points are limited to architecture rules,
    `ContextCarrier`, and event sinks. Arbitrary Transformer plugins are excluded.
24. Design and implementation plans live at repository level. The component may
    have a README but must not create a component-local `docs` directory.
25. Implementation uses an isolated worktree and one reviewable commit per task.
26. No database schema, data migration, Flyway file, UI, browser workflow, or
    long-running application startup is part of this work.

## 3. Goals

This design delivers:

1. one ASM-based metadata and transformation foundation;
2. a Maven Plugin that enforces the ten approved COLA rules without loading
   application classes;
3. a startup Agent with explicit class filters and safe class-loader boundaries;
4. automatic context and metric decoration for the approved Executor submission
   call sites;
5. method observation for calls that Spring AOP cannot see;
6. an Agent engine that reuses existing Method Extension handlers and response
   semantics;
7. an Agent engine that reuses the existing Access Guard rule chain;
8. explicit and testable semantics for private methods, static methods, and
   constructors;
9. Java 21 and Java 25 class-file compatibility evidence;
10. deterministic diagnostics, failure handling, and release artifacts.

## 4. Non-Goals

The following are not included:

1. `agentmain`, Attach API, dynamic Agent loading, redefinition, or
   retransformation;
2. modification of bootstrap or JDK classes;
3. `CompletableFuture.*Async` call-site enhancement;
4. `ScheduledExecutorService`, `ForkJoinPool`, or third-party executor call sites;
5. interception of executor-construction call sites;
6. dynamic registration of newly discovered executors into DTP;
7. exact cancellation observation through a wrapped Future;
8. Reactor `Mono` or `Flux` rejection responses;
9. dynamic changes to include/exclude transformation filters after class load;
10. an arbitrary external Transformer SPI;
11. SARIF or GitHub Annotation architecture reports;
12. preservation of the archetypes' nonstandard ArchUnit rules;
13. Access Guard on protected or package-private methods and constructors;
14. Access Guard on abstract, native, synthetic, bridge, or generated lambda-body
    methods;
15. constructor timeout, constructor fallback, constructor `returnJson`, instance
    state access before initialization, or replacement of a rejected constructor
    with another object;
16. JVM hard termination after a later class-transformation error;
17. transformed-class byte dumps or a production bytecode dump endpoint;
18. direct Dynamic Config Center integration;
19. full compatibility certification for Dubbo, Reactor, WebFlux, or arbitrary
    third-party agents;
20. Docker or long-running business-application startup as a validation step;
21. a standalone admin service, management UI, database, or Flyway migration.

## 5. Target Component Structure

```text
egon-cola-components/
`-- egon-cola-component-bytecode/
    |-- pom.xml
    |-- README.md
    |-- egon-cola-component-bytecode-api/
    |-- egon-cola-component-bytecode-bridge/
    |-- egon-cola-component-bytecode-core/
    |-- egon-cola-component-bytecode-runtime/
    |-- egon-cola-component-bytecode-agent/
    |-- egon-cola-component-bytecode-starter/
    |-- egon-cola-component-bytecode-architecture-maven-plugin/
    |-- egon-cola-component-bytecode-test/
    `-- egon-cola-component-bytecode-benchmark/
```

The component root is added to `egon-cola-components/pom.xml`.

The BOM manages:

```text
top.egon:egon-cola-component-bytecode-api
top.egon:egon-cola-component-bytecode-bridge
top.egon:egon-cola-component-bytecode-runtime
top.egon:egon-cola-component-bytecode-agent
top.egon:egon-cola-component-bytecode-starter
```

The BOM does not manage or export:

1. the component root POM;
2. `bytecode-core`;
3. the Architecture Maven Plugin;
4. the test module;
5. the benchmark module.

The Agent's default artifact is the executable shaded Agent JAR. A second
unshaded Agent artifact is not published.

`bytecode-core` is published as an internal implementation dependency required
by the Maven Plugin, but it is not a supported consumer API and is not placed in
the BOM. The Architecture Maven Plugin is published with `maven-plugin`
packaging and an explicit same-version dependency on core.

The test and benchmark modules set deployment to skip and are never released as
consumer artifacts.

## 6. Module Responsibilities And Dependencies

### 6.1 `bytecode-api`

Public contracts that application code may compile against:

1. `@EgonObserved`;
2. `ContextCarrier` and `ContextScope`;
3. bounded observation and executor event sink contracts;
4. architecture rule extension contracts that do not expose ASM types.

This module depends only on the JDK.

### 6.2 `bytecode-bridge`

The bridge is loaded from the Agent JAR by the system class loader and may also
be present as an application dependency when no Agent is attached. It contains:

1. the static bridge entry points called by transformed classes;
2. JDK-only request, outcome, and status records;
3. the runtime dispatcher interface;
4. ClassLoader-scoped dispatcher registration;
5. protocol version negotiation;
6. weak references and unregister handles.

The bridge depends only on the JDK. It must not reference Spring, SLF4J,
Micrometer, Jackson, ASM, Method Extension, Access Guard, or DTP types.

### 6.3 `bytecode-core`

Internal ASM implementation shared by the Agent and Maven Plugin:

1. class metadata reader;
2. annotation and hierarchy metadata resolver;
3. descriptor and signature scanners;
4. layer and method matchers;
5. architecture dependency graph;
6. enhancement planning;
7. class hierarchy resolution for stack-map frames;
8. deterministic IDs and fingerprints;
9. class writer and validation support.

The core uses ASM 9.9.1 as the initial pinned implementation baseline. ASM types
are never exposed through public Egon-COLA APIs.

### 6.4 `bytecode-runtime`

Application-loader runtime implementation:

1. context capture and restoration;
2. Executor task decorators;
3. call-site and method metadata registries;
4. observation dispatch;
5. Method Extension and Access Guard dispatcher slots;
6. bounded failure storage;
7. runtime status snapshots;
8. event fan-out.

The runtime depends on `api` and `bridge`. It does not depend on Spring.

### 6.5 `bytecode-agent`

Startup instrumentation:

1. `BytecodeAgent.premain`;
2. Agent argument and external YAML parsing;
3. class include/exclude matching;
4. one composite `ClassFileTransformer`;
5. Agent status and fatal-state publication;
6. startup summary logging;
7. the shaded Agent JAR manifest and dependency relocation.

The JAR manifest contains:

```text
Premain-Class: top.egon.cola.component.bytecode.agent.BytecodeAgent
Can-Redefine-Classes: false
Can-Retransform-Classes: false
```

ASM and the Agent-local YAML parser are shaded beneath an Agent-private package.

### 6.6 `bytecode-starter`

Spring integration:

1. runtime dispatcher registration;
2. properties and validation;
3. MDC `ContextCarrier`;
4. Micrometer event sinks;
5. optional DTP adapter;
6. optional Method Extension adapter;
7. optional Access Guard adapter;
8. fatal-state startup validation;
9. conditional Actuator endpoint.

Dependencies on DTP, Method Extension, Access Guard, and Actuator are optional.
Adding the bytecode starter alone must not transitively add Redisson, Spring Web,
Spring AOP, Jackson, DTP, Access Guard, Method Extension, or Actuator.

### 6.7 `architecture-maven-plugin`

The Maven Plugin owns Maven session/project adaptation, rule execution, report
writers, baseline I/O, cache I/O, and plugin goals. It depends on core and Maven
Plugin APIs but not on Spring or the runtime Agent.

### 6.8 Test And Benchmark Modules

The test module contains Maven Invoker fixtures and forked JVM Agent tests. The
benchmark module contains JMH and architecture-scan benchmarks. Neither module is
published.

## 7. Configuration Model

### 7.1 Transform-Time Configuration

Configuration that changes which classes or bytecode locations are transformed
must be available before application classes load.

Priority:

```text
Agent arguments
    > external Agent YAML
    > system properties / environment
    > built-in defaults
```

Example:

```bash
-javaagent:/opt/egon/egon-cola-component-bytecode-agent-<version>.jar=\
enabled=true,\
features=executor,observation,method-extension,access-guard,\
include=top.egon.*,com.company.*,\
config=/opt/egon/bytecode-agent.yml
```

Spring configuration cannot retroactively add or remove transformed classes.

### 7.2 Runtime Configuration

Spring configuration may control runtime behavior that can safely become a
no-op without retransformation:

1. feature enablement after startup;
2. sampling rates;
3. slow thresholds;
4. event sink enablement;
5. metric enablement;
6. Method Extension not-ready policy;
7. Access Guard runtime failure policy.

The status endpoint reports both requested and effective configuration so that a
Spring property cannot appear to enable a class that the Agent did not transform.

### 7.3 Default Exclusions

The Agent never transforms:

```text
java.*
javax.*
jakarta.*
jdk.*
sun.*
com.sun.*
org.objectweb.asm.*
org.springframework.*
org.slf4j.*
ch.qos.logback.*
io.micrometer.*
top.egon.cola.component.bytecode.*
```

Agent-private shaded packages are also hard exclusions. These exclusions cannot
be overridden in V1.

Third-party application packages require explicit includes.

## 8. ClassLoader And Bridge Design

The Agent and bridge are visible from the system class loader. Spring Boot
application classes may be loaded by a child class loader. Transformed business
classes call only bridge types visible from their parent.

The bridge registry is logically:

```text
WeakHashMap<ClassLoader, WeakReference<BytecodeRuntimeDispatcher>>
```

The implementation must additionally be thread-safe and return an unregister
handle. Runtime registration includes:

1. protocol major/minor version;
2. runtime version;
3. application ClassLoader;
4. enabled dispatcher capabilities.

A major-version mismatch is fatal. A minor-version mismatch is accepted only
when both sides advertise compatible capabilities.

Static methods and constructors do not have an instance target. Their bridge
calls pass the declaring `Class<?>`, allowing deterministic ClassLoader lookup.

The bridge must not retain a strong reference to the application ClassLoader,
Spring context, runtime dispatcher, target class, target object, or MethodHandle
after unregister.

## 9. Transformation Pipeline

### 9.1 Fast Filtering

Before constructing an ASM `ClassReader`, the Transformer rejects:

1. hard-excluded package names;
2. names outside all configured includes;
3. bootstrap classes;
4. bytecode platform classes;
5. unsupported class-file versions.

An unmatched class returns `null` immediately.

### 9.2 One Enhancement Plan Per Class

For a matched class, the core parses metadata once and creates one
`ClassEnhancementPlan`. A method plan may contain:

```text
executor call sites
observation policy
method-extension policy
access-guard policy
constructor-guard policy
```

Feature visitors are composed from this plan. They do not independently parse
and rewrite the same class.

### 9.3 Duplicate Prevention

V1 uses both of these mechanisms:

1. fixed bridge call signatures that are detected before transformation;
2. a ClassLoader-scoped Agent record of transformed class name and input digest.

The Agent does not add a marker field or public annotation to business classes.

A different input digest for a class name already recorded in V1 is a fatal
unsupported-redefinition condition because retransformation is excluded.

### 9.4 Stack-Map Frames

Call-site-only rewrites preserve existing control flow and frames where possible.

Control-flow-changing transformations use `COMPUTE_FRAMES` with an internal
metadata-based common-superclass resolver. The resolver reads class resources
through the target loader and never uses `Class.forName` during transformation.

Every transformed fixture is checked with ASM `CheckClassAdapter` and executed in
an isolated ClassLoader or forked JVM.

### 9.5 Cross-Feature Order

For a method matched by multiple features, the logical flow is:

```text
Method Extension evaluation
    rejected -> resolve and return rejection
    allowed
        -> Access Guard chain
            rejected -> publish guard event and return/throw rejection
            allowed
                -> Observation enter
                -> original business body
                -> Observation success/error/exit
```

Policy rejection is not recorded as a successful or failed business-body
observation. Method Extension and Access Guard publish their own events.

## 10. Architecture Maven Plugin

### 10.1 Goals

The plugin provides:

```text
bytecode-architecture:check
bytecode-architecture:check-reactor
bytecode-architecture:generate-baseline
```

`check` scans the current module. `check-reactor` scans configured reactor
projects. `generate-baseline` is the only goal allowed to add current findings to
the baseline.

Normal lifecycle execution is read-only outside `target`.

### 10.2 Scan Inputs

Supported inputs:

1. `target/classes`;
2. optional `target/test-classes`;
3. current Maven module;
4. configured reactor modules;
5. additional class directories;
6. optional dependency JARs;
7. archetype-generated projects.

The plugin reads class files and does not load or initialize application classes.

### 10.3 Layer Resolution

Priority:

```text
explicit module mapping
    > package patterns
    > module-name suffix
    > UNKNOWN
```

`UNKNOWN` is reported. It is never silently treated as an allowed layer.

### 10.4 Approved Rules

Only these rules are implemented and retained in the archetypes:

1. ARCH-001: Domain must not depend on Application, Infrastructure, Adapter,
   Facade, or Starter.
2. ARCH-002: Domain must remain free from configured technical frameworks.
3. ARCH-003: Application must not depend on Infrastructure or Adapter.
4. ARCH-004: Application must not directly access persistence frameworks or
   infrastructure mapper/repository implementations.
5. ARCH-005: Facade must remain a self-contained contract module.
6. ARCH-006: Starter must not contain or directly reference business
   implementations from Domain or Application.
7. ARCH-007: Common must not depend on business modules.
8. ARCH-008: Adapter must not directly call Infrastructure implementations.
9. ARCH-009: Domain may define repository interfaces but must not contain JPA
   entities, mapper implementations, SQL sessions, or infrastructure repository
   implementations.
10. ARCH-010: Facade implementations must reside in configured Adapter packages.

Rules are Specifications evaluated against the same dependency graph. Framework
denylists, package patterns, and allowed Facade implementation packages are rule
configuration, not additional rules.

### 10.5 Dependency Detection

The scanner covers:

1. superclass and interfaces;
2. field types;
3. method parameters, return types, and declared exceptions;
4. generic signatures;
5. annotations and annotation values;
6. local variable types when present;
7. allocation, array, cast, and `instanceof` instructions;
8. field reads and writes;
9. method and constructor calls;
10. method handles;
11. `invokedynamic` bootstrap handles and arguments;
12. Lambda targets;
13. `ConstantDynamic`;
14. constant-pool class references.

### 10.6 Finding Model

Every finding contains:

```text
ruleId
severity
module
sourceLayer
targetLayer
sourceClass
sourceMember
sourceDescriptor
targetClass
targetMember
targetDescriptor
dependencyKind
locationKind
lineNumber
message
suggestion
```

`sourceMember`, descriptors, and `lineNumber` may be absent. Console/Text use
`<class>` and `-1` when a scalar placeholder is required.

Findings are sorted deterministically by rule, module, source, member,
dependency kind, and target.

### 10.7 Reports

Required formats:

1. Console;
2. Text;
3. JSON;
4. HTML.

All formats consume the same immutable result model and must report identical
rule/severity/count totals.

Default output directory:

```text
${project.build.directory}/egon-cola-architecture
```

### 10.8 Failure Policy

```text
FAIL
WARN
REPORT_ONLY
```

Default is `FAIL`.

### 10.9 Baseline

Default baseline path:

```text
${maven.multiModuleProjectDirectory}/.egon-cola/architecture-baseline.json
```

The stable fingerprint hashes:

```text
ruleId
source class/member/descriptor
dependency kind
target class/member/descriptor
```

It excludes line number, display text, suggestion text, and report ordering.

Rules:

1. existing baseline findings may pass;
2. new findings fail according to failure policy;
3. fixed findings are reported as stale baseline entries;
4. normal checks never add new entries;
5. baseline generation requires an explicit goal;
6. replacing existing baseline content requires an explicit overwrite flag.

### 10.10 Content-Hash Cache

Cache key:

```text
class SHA-256
parser schema version
ASM baseline version
effective scan configuration digest
```

The cache stores parsed class metadata and dependency edges. Every run still
re-evaluates all rules against the complete graph. CI uses a clean full scan.

Cache files live beneath `target` and are not committed.

### 10.11 Archetype Integration And ArchUnit Removal

The light archetype binds `check` in its single generated project.

The web and service archetypes bind `check-reactor` in their terminal Starter
modules because those modules are built after their internal dependencies.

The archetype templates receive explicit package/module mappings for their
different layouts, including light's `start` package.

The migration removes:

1. all three generated `ArchitectureDependencyTest` classes;
2. `archunit-junit5` dependencies;
3. `archunit.version` properties;
4. `verify.groovy` assertions that require ArchUnit files or dependencies.

The verifiers instead assert plugin configuration, explicit plugin version,
generated report presence, and failing illegal-dependency fixtures.

The following current bespoke protections are intentionally not preserved:

1. light domain-first package naming and reversed outbound-port package checks;
2. web external evaluation-facade isolation;
3. service forbidden inbound package segments;
4. service project-wide native gRPC prohibition;
5. service provider-facade isolation.

This loss is an approved consequence of replacing ArchUnit with only the ten
standard rules.

## 11. Executor Enhancement

### 11.1 Supported Call Sites

V1 transforms business-code invocations of:

```text
Executor.execute(Runnable)
ExecutorService.submit(Runnable)
ExecutorService.submit(Runnable, result)
ExecutorService.submit(Callable)
```

It does not transform Executor/JDK classes themselves.

### 11.2 Call-Site Rewrite

Logical example:

```java
executor.submit(task);
```

becomes:

```java
EgonExecutorBridge.submit(executor, task, callSiteId);
```

Bridge overloads preserve the original declared return type and directly return
the Future produced by the underlying executor.

The `callSiteId` is a stable 64-bit hash of owner, method, descriptor, instruction
kind, target signature, and source location when available. Metadata is
registered once during transformation. A detected collision in one ClassLoader
is fatal rather than silently merging two call sites.

### 11.3 Context API

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
```

`CompositeContextCarrier` captures carriers in configured order and closes
scopes in reverse order. Partial restore failure closes already-restored scopes.

The runtime supplies no business-specific tenant or user holder. Applications
may register carriers for those contexts.

The starter supplies MDC propagation without exposing MDC types through the API.

### 11.4 Task Decorators

Runtime task types:

```text
EgonContextAwareRunnable
EgonContextAwareCallable
EgonInstrumentedTask
```

Decoration captures context and submission time once. Execution restores the
captured context, records start/end, and always closes scopes in `finally`.

Task detection prevents wrapping:

```text
DtpRunnable
DtpCallable
EgonContextAwareRunnable
EgonContextAwareCallable
EgonInstrumentedTask
```

Additional detection is available through a bounded task-detector SPI internal
to the starter integration. It is not an arbitrary Transformer SPI.

### 11.5 Semantics

1. wrapper failure falls back to submitting the original task;
2. metric failure never changes task execution;
3. rejection throws the original rejection exception;
4. business exceptions are not swallowed or retyped;
5. interrupt state is not cleared by instrumentation;
6. the original Future instance is returned;
7. task completion restores the previous worker-thread context;
8. Agent-disabled behavior is byte-for-byte unmodified at unloaded classes and
   runtime no-op at already transformed classes.

Exact observation of a Future cancelled before its task starts is excluded.

### 11.6 Executor Naming And DTP

Name priority:

```text
Spring Bean name
    > existing ManagedExecutorRegistry name
    > explicit configured name
    > executor class + identity suffix
```

The starter keeps a weak identity mapping for Spring Executor Beans. The DTP
adapter reuses current wrappers, snapshots, registry names, and meter semantics.

V1 does not modify `ManagedExecutorRegistry` to make it mutable and does not
register executors discovered from creation call sites.

DTP pool gauges and bytecode task metrics use distinct metric names to prevent
double counting.

### 11.7 Metrics And Events

Metrics include:

```text
egon.bytecode.executor.submissions
egon.bytecode.executor.rejections
egon.bytecode.executor.queue.wait
egon.bytecode.executor.execution
egon.bytecode.executor.completions
```

Bounded tags:

```text
executor
executor_type
result
exception_group
virtual_thread
```

Trace IDs, request IDs, thread names, raw exception messages, and call-site
strings are event fields or diagnostic metadata, never metric tags.

## 12. Method Observation

### 12.1 Matching

Observation supports:

1. `@EgonObserved`;
2. include package patterns;
3. method-name patterns;
4. explicit exclusions.

The annotation is runtime-retained and targets methods and constructors.

Constructor observation is disabled by default unless the constructor has
`@EgonObserved` or `observe-constructors=true` is explicitly configured.

Synthetic, bridge, and generated lambda-body methods are excluded by default.

### 12.2 Supported Targets

Supported:

1. public, protected, package-private, and private concrete instance methods;
2. static concrete methods;
3. final methods;
4. synchronized methods;
5. constructors under the restricted constructor timing model;
6. same-class calls and non-Spring objects.

Excluded:

1. abstract methods;
2. native methods;
3. `<clinit>`;
4. constructors before their initialization call completes.

### 12.3 Method Semantics

Logical flow:

```java
long token = EgonObservationBridge.enter(methodId);
try {
    Object result = originalBody();
    EgonObservationBridge.success(token);
    return result;
} catch (Throwable throwable) {
    EgonObservationBridge.error(token, throwable);
    throw throwable;
} finally {
    EgonObservationBridge.exit(token);
}
```

The exact original Throwable is rethrown. Return values are not copied or
replaced.

### 12.4 Constructor Timing

The verifier treats constructor `this` as uninitialized until the first valid
`this(...)` or direct `super(...)` invocation completes. Therefore constructor
observation starts after that invocation.

Consequences:

1. parent/delegated constructor duration is not attributed to the observing
   constructor;
2. an exception thrown by the first initialization call is not observed by the
   child constructor;
3. exceptions and returns in the remaining constructor body are observed;
4. when multiple constructors in a `this(...)` chain are observed, each records
   its own post-initialization body.

### 12.5 Data And Privacy

Recorded data:

```text
methodId
class and method metadata
layer
duration
result
exception group
trace ID for events
thread/virtual-thread state for events
static annotation tags
```

The implementation never records method arguments, return values, passwords,
tokens, cookies, authorization headers, identity numbers, bank details, phone
numbers, or arbitrary object `toString()` output.

### 12.6 Reentrancy

Bridge/runtime/logging/metrics packages are hard-excluded. Runtime additionally
uses a depth/token guard so an event sink cannot recursively observe itself.

### 12.7 Metrics

```text
egon.bytecode.method.duration
egon.bytecode.method.errors
egon.bytecode.method.slow
```

Trace IDs are not metric tags. Static tag keys and values are validated and
bounded during startup.

## 13. Method Extension Agent

### 13.1 Existing Component Boundary

The Method Extension component remains independently usable without the bytecode
starter. AOP mode remains its default and does not require Agent artifacts.

The existing AOP orchestration is extracted into a neutral
`MethodExtensionExecutionService` that owns:

1. method and annotation resolution;
2. unique Handler resolution;
3. `MethodExtensionContext` creation;
4. Handler invocation;
5. decision validation;
6. rejection response resolution.

The AOP Advisor and bytecode starter adapter both call this service. They do not
implement separate decision semantics.

### 13.2 Engine Configuration

```yaml
egon:
  cola:
    component:
      method-extension:
        enabled: true
        engine: aop
        not-ready-policy: proceed
```

Engine values:

```text
AOP
AGENT
DISABLED
```

Compatibility rules:

1. missing `engine` means `AOP`;
2. `enabled=false` means disabled regardless of engine;
3. AOP Advisor is not created for `AGENT`;
4. the Agent dispatcher is not registered for `AOP`;
5. `AGENT` without an attached compatible Agent fails Spring startup;
6. the same method cannot execute both engines.

### 13.3 Supported Methods

Agent mode supports annotated concrete instance methods with public, protected,
package-private, or private visibility, including final and synchronized methods.

It excludes static methods, constructors, abstract methods, and native methods.

The existing `MethodExtensionContext` retains a non-null target and a reflective
`Method`; no nullable-target API change is required.

### 13.4 Agent Flow

For a method that does not also require Access Guard structural wrapping, the
Agent inserts an entry decision and branches directly to the original body.

For a method also using Access Guard, one structural wrapper owns both policies.

The bridge result is logically:

```text
PROCEED
RETURN_NULL
RETURN_VALUE(value)
THROW(throwable)
```

Generated bytecode handles void return, reference casts, and primitive unboxing.
Invalid null-to-primitive rejection is a configuration error.

### 13.5 Runtime Not Ready

Policies:

```text
PROCEED
REJECT
FAIL
```

Default is `PROCEED` for backward compatibility. Once the Spring context is
ready, `AGENT` requires a registered Handler and response dispatcher.

### 13.6 Asynchronous Rejection Results

Allow decisions always return the business method's original asynchronous object.

For a reject decision:

1. a direct response already assignable to the declared return type is returned
   unchanged;
2. for `CompletableFuture<T>`, `CompletionStage<T>`, or `Future<T>`, a payload or
   `returnJson` is resolved as `T` and wrapped in
   `CompletableFuture.completedFuture`;
3. raw asynchronous interfaces resolve payload as `Object`;
4. concrete Future subclasses other than `CompletableFuture` do not support
   payload wrapping and fail configuration validation;
5. Reactor types are excluded.

### 13.7 Logging And Privacy

Logs may contain method signature, Handler type, decision state, and bounded
reason text. They never contain argument values, response bodies, or raw
`returnJson`.

## 14. Access Guard Agent

### 14.1 Existing Component Boundary

The Access Guard component remains independently usable in AOP mode and retains
its existing public annotations and service contracts.

Agent mode reuses the existing sequence:

```text
white list
    -> blacklist
    -> rate limiter
    -> timeout protection
    -> business invocation
```

The bytecode platform must not implement a second white-list, blacklist,
rate-limiter, timeout, reject, or event model.

### 14.2 Engine Configuration

```yaml
egon:
  cola:
    component:
      access-guard:
        enabled: true
        engine: aop
```

Engine values are `AOP`, `AGENT`, and `DISABLED` with the same compatibility
rules as Method Extension.

`AGENT` without an attached compatible Agent fails Spring startup.

### 14.3 Supported Target Matrix

| Target | Visibility | White/black/rate | Timeout | Fallback/JSON | Target object |
|---|---|---:|---:|---:|---|
| Instance method | public/private | yes | yes | yes | instance |
| Static method | public/private | yes | yes | yes | null |
| Constructor | public/private | restricted yes | no | no | unavailable |
| Synchronized instance/static method | public/private | yes | no | yes | instance/null |
| Protected/package-private method or constructor | any | no | no | no | unsupported |
| Abstract/native/synthetic/bridge/lambda body | any | no | no | no | unsupported |

An explicitly annotated unsupported target is a configuration error in Agent
mode; it is never silently ignored.

### 14.4 Structural Method Wrapping

Access Guard must be able to execute the business body through the existing
timeout executor. An entry-only branch cannot provide a reliable continuation
for private and static methods. Therefore V1 uses structural wrapping during
initial class definition.

For each matched non-constructor method:

1. retain the original name, descriptor, generic signature, exceptions,
   visibility, annotations, parameter annotations, bridge-facing metadata, and
   synchronization semantics on a wrapper method;
2. move the original code to a private synthetic method with a deterministic
   collision-checked name;
3. remove user governance annotations from the synthetic body;
4. load a direct JDK MethodHandle to the synthetic body;
5. invoke Method Extension first when configured;
6. pass an Agent `ProceedingJoinPoint` adapter and continuation to the existing
   Access Guard execution service;
7. place Method Observation around the actual synthetic body, not around policy
   rejection.

Self-invocation and recursion continue to invoke the original method name and
therefore re-enter the wrapper. Runtime reentrancy tokens prevent only internal
continuation calls from applying the same policy twice; genuine recursive
business calls remain separately governed.

Because structural changes are made only during initial class definition,
retransformation remains unsupported.

### 14.5 Synchronization

For a synchronized method without timeout, the original `ACC_SYNCHRONIZED` flag
stays on the wrapper and is removed from the synthetic body. The monitor covers
policy evaluation and business execution as the original method monitor covered
the full invocation.

Timeout offloads the business body to another thread. Retaining the monitor on
the caller would deadlock the worker, while moving it to the worker would change
the original synchronization boundary. Therefore synchronized plus timeout is
rejected during configuration validation.

### 14.6 Static Methods

Static methods pass a null target and declaring `Class<?>`.

Rules:

1. access keys may use arguments, request headers, IP, or `all`;
2. instance state is unavailable;
3. fallback methods must be static;
4. JSON rejection resolves against the original static method return type;
5. a configured instance fallback is a startup error;
6. timeout executes the direct static synthetic-body MethodHandle.

### 14.7 Constructor Annotation

Only the aggregate annotation changes target:

```java
@Target({ElementType.METHOD, ElementType.CONSTRUCTOR})
public @interface AccessGuard {

    // existing attributes

    FailStrategy failStrategy() default FailStrategy.GLOBAL_DEFAULT;
}
```

Adding `failStrategy` is additive for existing method users. Dedicated
annotations and `Do*` compatibility annotations remain method-only.

### 14.8 Constructor Guard Model

JVM constructors are `<init>` methods operating on an uninitialized `this` until
the required `this(...)` or direct `super(...)` invocation succeeds. The Agent
therefore performs a static pre-initialization check that never passes `this`.

Logical flow:

```text
enter <init>
    -> bridge.guardConstructor(declaringClass, constructorId, arguments, failHint)
        -> allow: continue original this()/super() and constructor body
        -> reject: throw AccessGuardRejectedException before initialization
```

Supported constructor rule data:

1. rule name;
2. `all` key;
3. constructor parameter names and property paths;
4. request header and IP keys;
5. white list;
6. blacklist;
7. rate limiter;
8. fail strategy;
9. events and bounded diagnostics.

Unsupported constructor configuration:

1. `timeoutBreaker=true`;
2. nonblank `fallbackMethod`;
3. nonblank `returnJson`;
4. instance-field or instance-method key resolution;
5. instance fallback;
6. replacement object return.

Static annotation attributes are validated during transformation. Runtime rule
overrides are validated when the Access Guard dispatcher registers.

On rejection, the constructor throws `AccessGuardRejectedException`. A
constructor cannot return fallback data.

If a constructor delegates with `this(...)`, every separately annotated
constructor evaluates its own rule. This is intentional and is not duplicate
enhancement.

### 14.9 Constructor Runtime Not Ready

Effective priority:

```text
explicit annotation failStrategy
    > transform-time Access Guard failure mode
    > built-in FAIL_OPEN
```

Before the Spring dispatcher is ready:

1. fail-open proceeds;
2. fail-closed throws before `this(...)` or `super(...)`;
3. global-default uses the transform-time mode;
4. no Redis-backed rule can be evaluated without a ready dispatcher;
5. using fail-closed on a Spring Bean constructor may intentionally prevent that
   Bean and the application context from starting.

The README must discourage constructor guards on infrastructure needed to build
the Access Guard runtime itself.

### 14.10 Reusing Existing Services

The current AOP orchestration is extracted to `AccessGuardExecutionService`.

For normal methods:

1. AOP passes the real `ProceedingJoinPoint`;
2. Agent mode passes `AgentProceedingJoinPoint` with the original target, method,
   arguments, and MethodHandle continuation;
3. existing `AccessGuardRuleResolver`, `AccessKeyResolver`, white-list service,
   blacklist service, rate limiter, timeout executor, reject invoker, and event
   publisher are reused.

Required additive changes:

1. static-target support in fallback resolution;
2. executable signature support for constructors;
3. constructor parameter-name resolution;
4. constructor-specific limited execution service;
5. engine and readiness properties;
6. Agent adapter auto-configuration.

Existing custom SPIs that assume a real Spring `ProceedingJoinPoint` are
guaranteed only in AOP mode. Agent mode guarantees the repository's default
implementations and explicitly Agent-aware extensions.

### 14.11 Failure Semantics

Runtime service failures follow the effective Access Guard fail strategy.

Transformation failure is separate:

1. fail-open records a high-priority failure and leaves the class unenhanced;
2. fail-closed records Agent fatal state;
3. the Spring Starter aborts context completion after observing fatal state;
4. the design does not claim that throwing from `ClassFileTransformer` aborts
   JVM class loading;
5. no hard JVM halt occurs.

The strongest fail-closed guarantee applies only to successfully transformed
wrappers. A transformation that fails before the Spring startup validator runs
has the explicitly accepted pre-context limitation above.

## 15. Starter And Actuator

Auto-configuration classes:

```text
BytecodeRuntimeAutoConfiguration
BytecodeContextAutoConfiguration
BytecodeMetricsAutoConfiguration
BytecodeMethodExtensionAdapterAutoConfiguration
BytecodeAccessGuardAdapterAutoConfiguration
BytecodeActuatorAutoConfiguration
```

Registration uses:

```text
META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
```

The starter validates:

1. Agent attached state;
2. protocol compatibility;
3. configured versus transformed features;
4. duplicate runtime registration;
5. Method Extension and Access Guard engine handshakes;
6. unsupported method/constructor configurations;
7. Agent fatal state.

### 15.1 Actuator Endpoint

When Actuator is already present, endpoint ID `egonbytecode` exposes at
`/actuator/egonbytecode`:

```text
agent version and state
protocol version
configured/effective features
transformed class and method counts
skip/failure counts by feature
runtime dispatcher state
bounded recent failures
architecture plugin version is not reported at runtime
```

It does not expose raw include/exclude patterns, access keys, method arguments,
return values, full exception messages, or unbounded class lists.

The Actuator dependency is optional and is not transitively added to consumers.

## 16. Diagnostics And Error Handling

Agent states:

```text
DISABLED
STARTING
ACTIVE
DEGRADED
FAILED
```

Transformation policies:

```text
SKIP_CLASS
DISABLE_FEATURE
MARK_FATAL
```

The bounded failure record contains:

```text
timestamp
class name
class-loader description/hash
feature
exception type
bounded sanitized message
policy
```

The Agent logs one startup summary containing:

```text
Agent and protocol versions
Java runtime and class-file capability
requested/effective features
include/exclude rule counts and redacted digests
failure policy
bridge registration state
```

It then logs aggregate counts. It does not log every successful class
transformation.

Architecture Plugin errors are deterministic Maven failures and include report
paths. Runtime event/metric failures are isolated and do not replace business
exceptions.

## 17. Security And Privacy

1. Transform filters are explicit allowlists.
2. JDK, logging, metrics, Spring, ASM, bridge, runtime, and Agent packages are
   hard-excluded.
3. The Agent JAR and external configuration must be treated as trusted deployment
   artifacts.
4. The status endpoint redacts sensitive match patterns and application values.
5. Metrics do not use trace ID, access key, request ID, thread name, or raw method
   signature as unbounded tags.
6. Access Guard logs contain only hashed/masked access-key data as existing
   component policy requires.
7. Observation never serializes arguments or returns.
8. External YAML file permissions and Agent JAR checksum validation are operator
   responsibilities documented in the README.

## 18. Design Pattern Choices

Patterns are used only at real variation points.

1. Specification for architecture rules and match predicates.
2. Strategy for layer resolution, report writers, context carriers, event sinks,
   and failure policies.
3. Composite for context carriers and the per-class enhancement plan.
4. Adapter for Maven, Spring, DTP, AOP/Agent, and `ProceedingJoinPoint` seams.
5. Decorator for Runnable and Callable task enhancement.
6. Registry for ClassLoader-scoped dispatchers and immutable call-site metadata.

The design does not add a factory hierarchy for transformers or business
policies. One explicit enhancement planner and a small visitor pipeline are
clearer than multiple inheritance or a generic handler framework.

## 19. Compatibility

### 19.1 Java And Class Files

1. production sources compile with `--release 21`;
2. Java 21 and Java 25 run the Agent integration suite;
3. a Java 25 CI job compiles a real `--release 25` fixture;
4. parser compatibility is proven with fixture execution, not only an ASM version
   constant;
5. preview class files are excluded unless explicitly added in a future design.

### 19.2 Existing Components

Method Extension and Access Guard changes are additive:

1. existing properties continue to work;
2. missing engine means AOP;
3. existing public annotations remain valid;
4. AOP behavior and order remain the default;
5. direct responses and existing JSON behavior remain supported;
6. Agent-only behavior does not add Agent dependencies to applications using only
   the current starters.

### 19.3 Agent Compatibility

The initial Agent protocol is versioned independently from component semantic
version. Major protocol mismatch is fatal. Minor additions must preserve existing
record fields and capability negotiation.

### 19.4 Release Artifacts And Versioning

All released bytecode artifacts use the same Egon-COLA reactor version. The
release set is:

```text
egon-cola-component-bytecode-api
egon-cola-component-bytecode-bridge
egon-cola-component-bytecode-core
egon-cola-component-bytecode-runtime
egon-cola-component-bytecode-agent
egon-cola-component-bytecode-starter
egon-cola-component-bytecode-architecture-maven-plugin
```

The API, bridge, runtime, Agent, and starter are public BOM-managed artifacts.
Core is a published internal dependency and is not a supported extension API.
The Maven Plugin is invoked with an explicit plugin version and is not added to
application dependency management. Sources and Javadoc artifacts are published
for non-shaded libraries and the Maven Plugin. The Agent publishes only its
shaded executable JAR plus sources; its manifest records `Implementation-Version`
and the bridge protocol version, and it intentionally omits `Agent-Class` because
dynamic attach is unsupported.

Backward-compatible API additions use a minor release. Removing or changing a
public API contract, bridge protocol major version, annotation meaning, or
transformed-call ABI requires a major release. Internal core/ASM changes may use
a patch release when public behavior and the bridge protocol are unchanged.

### 19.5 Framework Compatibility Boundary

The supported Spring baseline is Spring Boot 3.5.x. Agent and core modules do
not link to Spring Boot APIs. Required compatibility fixtures cover:

1. Spring AOP, CGLIB proxies, and JDK dynamic proxies;
2. plain JVM and Spring Boot application ClassLoaders with DevTools disabled;
3. Maven Surefire and Failsafe forked JVM execution;
4. virtual threads and `CompletableFuture` return paths;
5. MyBatis mapper proxies and JPA entity/repository proxy class loading.

The MyBatis and JPA fixtures prove matching, exclusion, class loading, and
business-call semantics without claiming database-vendor certification. DevTools
restart ClassLoader behavior, Dubbo, Reactor, WebFlux, and arbitrary third-party
Agent coexistence remain outside this release.

## 20. Testing Strategy

### 20.1 Unit Tests

Core tests cover:

1. descriptors, signatures, annotations, handles, invokedynamic, and dynamic
   constants;
2. layer and method matchers;
3. rule Specifications;
4. deterministic IDs and baseline fingerprints;
5. content-hash cache invalidation;
6. config precedence and validation;
7. context capture, restore order, partial failure, and cleanup;
8. bridge registration, version negotiation, unregister, and weak references.

### 20.2 Transformer Tests

Every transformed fixture follows:

```text
original class bytes
    -> transform
    -> CheckClassAdapter
    -> isolated ClassLoader
    -> behavior assertion
```

Fixtures cover:

1. primitive/reference/void returns;
2. checked and unchecked exceptions;
3. try/catch/finally;
4. public, protected, package-private, and private methods;
5. static and final methods;
6. synchronized methods;
7. generic bridge methods;
8. records and enums where applicable;
9. constructor `super(...)` and `this(...)` chains;
10. constructor final-field assignments;
11. duplicate-transform detection;
12. unsupported native/abstract/interface targets;
13. combined Method Extension, Access Guard, and Observation order;
14. plain JVM and Spring Boot class loaders;
15. Spring AOP, CGLIB, and JDK dynamic-proxy coexistence.

### 20.3 Architecture Plugin Tests

Maven Invoker projects cover:

1. a clean project;
2. one isolated fixture for every ARCH rule;
3. multiple findings and deterministic ordering;
4. missing debug information;
5. class-level dependency locations;
6. empty modules;
7. dependency JAR scanning;
8. multi-module reactor scanning;
9. baseline generation and comparison;
10. stale baseline entries;
11. cache hit and invalidation;
12. all four report formats;
13. no class loading or static initialization;
14. Java 21 and Java 25 class files.

### 20.4 Executor Tests

1. MDC and custom context propagation;
2. worker context restoration after success and failure;
3. no duplicate DTP/Egon wrapping;
4. original Future identity with `assertSame`;
5. original rejection exception type and instance where applicable;
6. checked/unchecked Callable failure behavior;
7. interrupt preservation;
8. ordinary and virtual-thread executors;
9. 100-way concurrent submission and registry safety;
10. metric sink failure isolation.

### 20.5 Observation Tests

1. same-class invocation;
2. non-Spring object;
3. private and static methods;
4. constructor post-initialization timing;
5. constructor initialization failure boundary;
6. same return value and Throwable;
7. slow event threshold;
8. reentrancy prevention;
9. disabled/no-op behavior;
10. sensitive data absence.

### 20.6 Method Extension Tests

1. AOP regression suite remains green;
2. AOP and Agent decision parity;
3. public/protected/package-private/private instance methods;
4. self-invocation and non-Spring targets;
5. Handler exactly once;
6. all primitive/reference/void rejection forms;
7. Future, CompletionStage, and CompletableFuture rejection payloads;
8. concrete unsupported Future subclass validation;
9. not-ready policies;
10. AOP/Agent duplicate prevention;
11. handler error propagation.

### 20.7 Access Guard Tests

1. AOP regression suite remains green;
2. Agent parity for white list, blacklist, rate limiting, timeout, reject, and
   event results;
3. public/private instance methods;
4. public/private static methods;
5. static fallback validation;
6. synchronized method without timeout;
7. synchronized plus timeout rejection;
8. public/private constructor matching;
9. protected/package-private target rejection;
10. constructor rejection before superclass side effects;
11. constructor `this(...)` chain rule count;
12. constructor parameter key resolution;
13. constructor unsupported property validation;
14. constructor runtime-not-ready fail-open and fail-closed;
15. genuine recursion versus internal continuation bypass;
16. Access Guard fatal-state monitoring.

### 20.8 Forked Agent Tests

The test module launches short-lived child JVMs with:

```text
-Xverify:all
-javaagent:<built-agent.jar>
```

It asserts process exit, stdout/stderr summaries, Agent status, business behavior,
and transformed-class verification on Java 21 and Java 25. These are test
processes, not a started business application.

Separate Maven Invoker fixtures exercise the Agent through Surefire and
Failsafe. Spring compatibility fixtures cover AOP, CGLIB, JDK proxies, MyBatis
mapper proxies, and JPA entity/repository proxy loading with DevTools disabled.

### 20.9 Performance Tests

Controlled benchmarks measure:

1. Architecture Plugin full scan of 1,000 representative classes: target at or
   below 2 seconds;
2. Agent matching and transformation of 1,000 representative business classes:
   target at or below 1 second;
3. unmatched Agent class-filter path;
4. Executor submission overhead: target below 5 microseconds;
5. Method Observation overhead: target below 2 microseconds;
6. bridge registry memory and class-loader cleanup.

Absolute microsecond thresholds run on a controlled benchmark environment.
Shared CI reports relative regressions but does not fail on noisy absolute
microsecond measurements.

## 21. CI And Validation Layers

### 21.1 Focused Validation

Each implementation task runs its exact module tests first.

### 21.2 Component Reactor

After each capability stage:

```bash
./mvnw -B -ntp -f egon-cola-components/pom.xml test
```

Explicit module slices are used when aggregator selection alone would not execute
child tests.

### 21.3 Archetype Integration

After Architecture Plugin wiring changes:

```bash
./mvnw -B -ntp clean integration-test
```

The generated light, web, and service projects must validate the Maven Plugin and
must not contain ArchUnit dependencies or tests.

### 21.4 Dependency Boundary

Dependency-tree checks prove that the bytecode starter does not transitively add
Actuator, Access Guard, Method Extension, DTP, Redisson, Spring Web, database,
Flyway, PostgreSQL, SQLite, or unrelated infrastructure.

### 21.5 Final Validation

Before a stage is reported complete:

1. focused tests pass;
2. component reactor passes;
3. relevant Invoker/forked JVM tests pass;
4. Java 21/25 matrix passes where applicable;
5. `git diff --check` passes;
6. repository status and exact changed files are reviewed;
7. stronger root integration runs for shared reactor/archetype changes.

## 22. Delivery Stages

### Stage 0: Design And Planning

1. commit this approved design;
2. receive user review of the written spec;
3. create a separate implementation plan for every selected stage;
4. do not write implementation code before plan approval.

### Stage 1: Architecture Maven Plugin

Deliver:

1. component foundation required by the plugin;
2. metadata graph and ten rules;
3. reports, baseline, and content-hash cache;
4. Maven goals and Invoker tests;
5. light/web/service integration;
6. immediate ArchUnit removal.

Exit criteria:

1. all ten rule fixtures pass;
2. generated archetypes use the plugin;
3. generated archetypes contain no ArchUnit dependency/test;
4. Java 21/25 fixtures pass;
5. performance target is measured.

### Stage 2: Agent Foundation And Executor

Deliver:

1. api, bridge, core, runtime, agent, starter, test, and benchmark foundations;
2. shaded premain Agent;
3. ClassLoader-safe runtime registration;
4. Executor P0 call sites;
5. context carriers, DTP deduplication, metrics, and minimal status endpoint.

Exit criteria:

1. forked Agent verification passes on Java 21/25;
2. original Future identity and exception semantics pass;
3. DTP tasks are not double wrapped;
4. virtual-thread tests pass;
5. startup and hot-path targets are measured.

### Stage 3: Method Observation

Deliver:

1. annotation and match configuration;
2. ordinary method and constructor transforms;
3. event/metric sinks and privacy controls;
4. reentrancy protection.

Exit criteria:

1. self/private/static/non-Spring cases pass;
2. constructor timing contract passes;
3. return/Throwable identity passes;
4. observation performance is measured.

### Stage 4: Method Extension Agent

Deliver:

1. existing component execution-service extraction;
2. engine and not-ready configuration;
3. Agent adapter and response bridge;
4. asynchronous rejection response support;
5. AOP/Agent parity tests.

Exit criteria:

1. existing AOP behavior remains valid;
2. all approved Agent method visibilities pass;
3. Handler executes once;
4. async response contracts pass;
5. no duplicate engine execution occurs.

### Stage 5: Access Guard Agent

Deliver:

1. existing Access Guard execution-service extraction;
2. structural wrappers for approved instance/static methods;
3. static fallback handling;
4. synchronized validation;
5. aggregate annotation constructor target and fail strategy;
6. pre-initialization constructor guard;
7. fail-open/fail-closed and status integration;
8. AOP/Agent parity and constructor tests.

Exit criteria:

1. existing AOP suite remains valid;
2. approved instance/static matrix passes;
3. constructor rejection precedes superclass side effects;
4. unsupported targets/configurations fail explicitly;
5. security failure is visible in status and startup validation;
6. root integration validation passes.

## 23. Expected Repository Changes

Implementation will affect only these areas:

1. `egon-cola-components/pom.xml`;
2. `egon-cola-components/egon-cola-components-bom/pom.xml`;
3. the new `egon-cola-component-bytecode` tree;
4. focused Method Extension properties, auto-configuration, AOP orchestration,
   context/response support, and tests;
5. focused Access Guard annotations, properties, auto-configuration, AOP
   orchestration, rule/signature/key/reject/timeout support, and tests;
6. the three archetype template POMs, architecture tests, and verifiers;
7. Java compatibility and strong CI workflows;
8. repository-level design and implementation-plan documents.

The first Executor stage does not make `ManagedExecutorRegistry` mutable and does
not require changes to DTP core behavior.

No database, Flyway, application business module, controller, UI, or unrelated
component is modified.

## 24. Risks And Mitigations

### 24.1 VerifyError Or Frame Corruption

Mitigation: metadata-based hierarchy resolution, minimum control-flow changes,
`CheckClassAdapter`, `-Xverify:all`, Java 21/25 fixtures, and constructor/final-field
tests.

### 24.2 Class Loading Recursion

Mitigation: name filtering before ASM parsing, hard package exclusions, no target
class loading during transform, relocated Agent dependencies, and JDK-only bridge
types.

### 24.3 ClassLoader Leak

Mitigation: weak loader/dispatcher mappings, explicit unregister, no application
objects in Agent registries, and cleanup tests.

### 24.4 Duplicate Policy Execution

Mitigation: one enhancement plan, mutually exclusive AOP/Agent registration,
fixed feature order, fixed bridge-call detection, and continuation bypass tokens.

### 24.5 Constructor Semantic Overstatement

Mitigation: constructor Guard is explicitly pre-initialization and restricted;
constructor Observation explicitly starts after initialization. Timeout, fallback,
replacement object, and instance-state access are rejected rather than emulated.

### 24.6 Access Guard Fail-Closed Gap

Mitigation: transformed wrappers enforce fail-closed directly, Agent fatal state
aborts Spring context completion, and the documented pre-context limitation is
not described as JVM-level termination.

### 24.7 Performance Regression

Mitigation: fast name filter, no reflection in Executor hot paths, immutable
call-site metadata, sampling, bounded events, optional metrics, and controlled
benchmarks.

### 24.8 Architecture Coverage Regression

Immediate ArchUnit removal intentionally drops bespoke archetype rules. This is
not an implementation defect to be silently restored. Only ARCH-001 through
ARCH-010 may be implemented unless a later approved design adds more rules.

## 25. Acceptance Summary

The design is satisfied only when:

1. all published artifacts and package names match this document;
2. the Maven Plugin enforces exactly ARCH-001 through ARCH-010;
3. all archetypes use the plugin and no longer contain ArchUnit;
4. Agent installation is explicit and premain-only;
5. bridge/runtime class-loader boundaries pass leak and compatibility tests;
6. Executor preserves Future, exception, interrupt, and rejection semantics;
7. Observation preserves the original return and Throwable and follows the
   constructor timing boundary;
8. Method Extension remains AOP by default and Agent mode covers the approved
   instance methods once only;
9. Access Guard remains AOP by default and Agent mode covers exactly the approved
   public/private instance/static/constructor matrix;
10. constructor Guard remains pre-initialization and rejects unsupported
    timeout/fallback/JSON semantics;
11. synchronized timeout is rejected;
12. fatal Agent state is visible and prevents Spring context completion without
    hard JVM termination;
13. Java 21/25, Invoker, forked Agent, component reactor, archetype integration,
    dependency boundary, hygiene, and relevant performance checks have fresh
    passing evidence;
14. each delivery stage is reviewed and approved before the next stage starts.
