# Egon COLA Bytecode Component

The bytecode component checks compiled classes against the standard Egon COLA architecture rules without loading or initializing application classes. Its public API is JDK-only; ASM, Maven, and JSON serialization remain implementation details.

## Runtime Agent Installation

The runtime enhancement has two independently installed parts. Add the Spring starter to the application and pass the separately published shaded Agent JAR to the JVM before the application main class:

```xml
<dependency>
    <groupId>top.egon</groupId>
    <artifactId>egon-cola-component-bytecode-starter</artifactId>
    <version>${egon-cola.version}</version>
</dependency>
```

```bash
java -Xverify:all \
  "-javaagent:/opt/egon/egon-cola-component-bytecode-agent-5.2.3.jar=enabled=true,features=executor;observation,include=com.example.*,observation-include=com.example.*" \
  -jar application.jar
```

The published Agent JAR is the main `egon-cola-component-bytecode-agent-${version}.jar`; it already contains relocated ASM and SnakeYAML classes. Do not put an unshaded Agent classifier on the command line. The Agent supports `premain` only.

The Agent is disabled by default and an enabled Agent requires at least one explicit `include` pattern. Supported keys are `enabled`, `features`, `include`, `exclude`, `observation-include`, `observation-method`, `observation-exclude`, `observe-constructors`, `observation-slow-threshold-millis`, `failure-policy`, `failure-capacity`, and `config`. Configuration precedence from lowest to highest is defaults, environment, JVM system properties, YAML, and `-javaagent` arguments. The `config` path itself is selected from environment, system property, then Agent argument.

```yaml
enabled: true
features:
  - executor
  - observation
include:
  - com.example.*
exclude:
  - com.example.generated.*
observation-include:
  - com.example.application.*
observation-method:
  - handle*
observation-exclude:
  - com.example.application.internal.*
observe-constructors: false
observation-slow-threshold-millis: 500
failure-policy: skip-class
failure-capacity: 32
```

Environment keys use `EGON_COLA_BYTECODE_`, such as `EGON_COLA_BYTECODE_INCLUDE`; system properties use `egon.cola.bytecode.`, such as `-Degon.cola.bytecode.config=/opt/egon/bytecode.yaml`. List values accept commas or semicolons. Failure policies are `skip-class`, `disable-feature`, and `mark-fatal`.

Includes can never override the immutable exclusions for bootstrap classes and the `java`, `javax`, `jakarta`, `jdk`, `sun`, `com.sun`, ASM, Spring, logging, Micrometer, and `top.egon.cola.component.bytecode` packages. Only class-file versions 65 through 69 (Java 21 through Java 25) are eligible.

## Executor Enhancement Semantics

The Agent rewrites exactly these interface call sites in included application classes:

- `Executor.execute(Runnable)`
- `ExecutorService.submit(Runnable)`
- `ExecutorService.submit(Runnable, Object)`
- `ExecutorService.submit(Callable)`

Calls to scheduler APIs, concrete executor-owner methods, JDK classes, and other overloads are left unchanged. Each rewritten site has a stable ID derived from its owner, enclosing method, target signature, and instruction position. A conflicting ID is a hard registration failure rather than an ambiguous metric.

The underlying executor API is invoked exactly once. `submit` returns the exact `Future` created by that executor, while business exceptions, `RejectedExecutionException`, interruption, and cancellation behavior retain their original identities and timing. The wrapper restores captured context around task execution and cleans worker-thread state in `finally`; cancellation cannot prevent a carrier that already performed capture from doing that capture work.

MDC propagation is enabled when SLF4J is present. Additional carriers implement the JDK-only `ContextCarrier` API. Egon-wrapped tasks and exact DTP wrapper types `DtpRunnable` and `DtpCallable` are not wrapped again; the dynamic-thread-pool registry is neither modified nor used for discovery.

Spring runtime settings use the `egon.cola.component.bytecode` prefix:

```yaml
egon:
  cola:
    component:
      bytecode:
        enabled: true
        executor:
          enabled: true
          propagate-mdc: true
          metrics: true
          sampling-rate: 1.0
          names:
            applicationTaskExecutor: application
        runtime:
          failure-capacity: 32
        endpoint:
          enabled: true
```

Agent inclusion is decided before Spring starts. Runtime `executor.include` and `executor.exclude` values are reserved requested settings and cannot widen the Agent's effective transformation scope.

## Method Observation Semantics

Method Observation matches in this order: immutable/hard package exclusions and explicit `observation-exclude` patterns win first; `@EgonObserved` then enables an otherwise eligible method; finally `observation-include` plus `observation-method` enables configured methods. The annotation accepts at most eight static `key=value` tags and an optional slow threshold. Static tags are validated and bounded; dynamic `${...}` and `#{...}` values are rejected.

Supported targets are concrete instance methods of every visibility, static, final, and synchronized methods, same-class calls, recursive calls, and non-Spring objects. Abstract, native, synthetic, bridge, generated lambda-body methods, and `<clinit>` are excluded. Constructors are observed only when annotated or when `observe-constructors=true`; timing starts after the first successful `this(...)` or direct `super(...)` call. A failure from that initialization call is not attributed to the child constructor, while each observed constructor in a successful `this(...)` chain records its own remaining body.

The observation wrapper preserves the exact return value and rethrows the exact original `Throwable`. It records method identity metadata, inferred layer, duration, result, exception class group, virtual-thread state, trace ID for event sinks when available, and validated static tags. It never captures arguments, return payloads, exception messages, arbitrary object text, credentials, cookies, authorization headers, or other request payloads. Timing covers synchronous method execution only: returning a `Future`, `CompletionStage`, reactive publisher, or other asynchronous value ends observation at the return and does not track later completion.

Spring runtime controls are independent from transform-time matching:

```yaml
egon:
  cola:
    component:
      bytecode:
        observation:
          enabled: true
          sampling-rate: 1.0
          slow-threshold-millis: 500
          metrics-enabled: true
```

Sampling and runtime disablement become no-ops without retransformation. Runtime Sink failures are isolated into bounded diagnostics, and a depth guard suppresses observations recursively triggered by event publication. The Agent also hard-excludes the bridge, runtime, logging, and metrics packages.

## Metrics, Status, And Privacy

When a `MeterRegistry` is present, the starter emits only these bounded metrics:

- `egon.cola.bytecode.executor.tasks.submitted`
- `egon.cola.bytecode.executor.tasks.started`
- `egon.cola.bytecode.executor.tasks.finished`
- `egon.cola.bytecode.executor.queue.wait`
- `egon.cola.bytecode.executor.execution`
- `egon.bytecode.method.duration`
- `egon.bytecode.method.errors`
- `egon.bytecode.method.slow`

Executor tags are `executor`, `executor_type`, `result`, `exception_group`, and `virtual_thread`. Observation tags are bounded class, method, layer, virtual-thread, exception-group where applicable, and validated static annotation tags. Trace IDs, request IDs, thread names, raw descriptors, arguments, and return values are never metric tags. Unknown or identity-derived executor names collapse to `unmanaged`, values are sanitized and length-bounded, and each `sampling-rate` must be between `0.0` and `1.0`.

Actuator is optional and is not pulled transitively by the starter. If Actuator is already installed and the endpoint is exposed, `GET /actuator/egonbytecode` reports Agent/runtime versions, protocol, state, requested/effective features, bounded counts and recent failures, dispatcher registration, metadata counts, and aggregate observation counts. It never reports raw include/exclude patterns, Agent arguments, method descriptors, class owners, arguments, returns, exception messages, tasks, `Future` objects, request IDs, trace IDs, or captured context. Agent startup output likewise prints only pattern counts and SHA-256 digests.

States are `DISABLED`, `STARTING`, `ACTIVE`, `DEGRADED`, and `FAILED`; a missing Agent is reported by the starter as `AGENT_UNAVAILABLE`. A running Agent with a different protocol major fails Spring startup. The Agent does not support Attach, `agentmain`, redefinition, retransformation, bootstrap/JDK transformation, or transformed-class dumps.

## Maven Plugin

Always declare the plugin version explicitly. Bind `check` in a single-module project or bind `check-reactor` in the terminal module of a multi-module reactor:

```xml
<plugin>
    <groupId>top.egon</groupId>
    <artifactId>egon-cola-component-bytecode-architecture-maven-plugin</artifactId>
    <version>${egon-cola.version}</version>
    <configuration>
        <packageMappings>
            <com.example.domain..>DOMAIN</com.example.domain..>
            <com.example.application..>APPLICATION</com.example.application..>
            <com.example.infrastructure..>INFRASTRUCTURE</com.example.infrastructure..>
            <com.example.adapter..>ADAPTER</com.example.adapter..>
            <com.example.facade..>FACADE</com.example.facade..>
            <com.example.starter..>STARTER</com.example.starter..>
            <com.example.common..>COMMON</com.example.common..>
        </packageMappings>
    </configuration>
    <executions>
        <execution>
            <id>cola-architecture-check</id>
            <phase>verify</phase>
            <goals>
                <goal>check-reactor</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

The goals are:

- `bytecode-architecture:check`: scan the current module.
- `bytecode-architecture:check-reactor`: scan compiled classes in the current Maven reactor.
- `bytecode-architecture:generate-baseline`: explicitly write the current finding fingerprints to the baseline.

Normal checks only write beneath `target` and never add findings to the baseline.

## Configuration

Maven user properties override matching XML configuration, XML overrides plugin defaults, and rule-specific lists replace their defaults when supplied. Layer resolution then uses this independent precedence:

```text
explicit module mapping > package mapping > module-name suffix > UNKNOWN
```

Supported layers are `DOMAIN`, `APPLICATION`, `INFRASTRUCTURE`, `ADAPTER`, `FACADE`, `STARTER`, `COMMON`, and `UNKNOWN`. Important options are:

| Option | Default | Purpose |
| --- | --- | --- |
| `moduleMappings` | empty | Map an exact Maven artifact ID to a layer. |
| `packageMappings` | empty | Map package patterns such as `com.example.domain..` to a layer. |
| `scanTests` / `egonArchitecture.scanTests` | `false` | Include `target/test-classes`. |
| `scanDependencies` / `egonArchitecture.scanDependencies` | `false` | Include dependency JARs. |
| `additionalClassDirectories` | empty | Include additional compiled-class directories. |
| `frameworkDenylist` | built-in Spring/Jakarta technical prefixes | Replace the Domain technical-framework denylist. |
| `frameworkAllowlist` | empty | Allow specific technical prefixes before applying the denylist. |
| `facadeImplementationPackages` | `..adapter..` | Define allowed packages for Facade implementations. |
| `failurePolicy` / `egonArchitecture.failurePolicy` | `FAIL` | Choose `FAIL`, `WARN`, or `REPORT_ONLY`. |
| `unknownLayerPolicy` / `egonArchitecture.unknownLayerPolicy` | `WARN` | Choose `FAIL`, `WARN`, or `IGNORE`. |
| `cacheEnabled` / `egonArchitecture.cache.enabled` | `true` | Enable parsed-class metadata caching. |

## Standard Rules

The built-in registry contains exactly these ten Specifications:

1. `ARCH-001`: Domain must not depend on Application, Infrastructure, Adapter, Facade, or Starter.
2. `ARCH-002`: Domain must remain free from configured technical frameworks.
3. `ARCH-003`: Application must not depend on Infrastructure or Adapter.
4. `ARCH-004`: Application must not directly access persistence frameworks or infrastructure mapper/repository implementations.
5. `ARCH-005`: Facade must remain a self-contained contract module.
6. `ARCH-006`: Starter must not contain or directly reference Domain or Application business implementations.
7. `ARCH-007`: Common must not depend on business modules.
8. `ARCH-008`: Adapter must not directly call Infrastructure implementations.
9. `ARCH-009`: Domain may define repository interfaces but must not contain JPA entities, mapper implementations, SQL sessions, or infrastructure repository implementations.
10. `ARCH-010`: Facade implementations must reside in configured Adapter packages.

The scanner covers inheritance, fields, parameters, return and exception types, generic signatures, annotations and values, local types, allocations, arrays, casts, `instanceof`, field access, method and constructor calls, method handles, `invokedynamic`, Lambda targets, `ConstantDynamic`, and constant-pool class references.

## Reports And Failure Policies

Every check writes deterministically ordered Text, JSON, and HTML reports from the same immutable result model and prints the same totals to the console. The default directory is:

```text
${project.build.directory}/egon-cola-architecture
```

Files are `architecture-report.txt`, `architecture-report.json`, and `architecture-report.html`.

`FAIL` fails the build for new error findings, `WARN` logs them without failing, and `REPORT_ONLY` only emits reports. Existing baseline findings do not count as new. `unknownLayerPolicy` is evaluated separately so unmapped classes are never silently accepted.

## Baseline Workflow

The default baseline is `${maven.multiModuleProjectDirectory}/.egon-cola/architecture-baseline.json`.

1. Review the current findings.
2. Run `./mvnw bytecode-architecture:generate-baseline` to create a baseline.
3. Commit the reviewed baseline if it represents accepted debt.
4. Keep running `check` or `check-reactor`; only new findings are subject to the failure policy and fixed entries are reported as stale.
5. Pass `-DegonArchitecture.overwrite=true` only when intentionally replacing an existing baseline.

The stable fingerprint includes rule ID, source class/member/descriptor, dependency kind, and target class/member/descriptor. It excludes line numbers and presentation text.

## Content-Hash Cache

Parsed metadata is cached below `target/egon-cola-architecture/cache`. The cache key contains the class SHA-256, parser schema version, ASM baseline version, and effective scan-configuration digest. Every run still rebuilds the complete graph and evaluates every rule. CI disables the cache with:

```bash
./mvnw -DegonArchitecture.cache.enabled=false verify
```

## ArchUnit Migration Boundary

The light, web, and service archetypes replace their generated ArchUnit test with this plugin. The approved standard-rule scope intentionally does not preserve five bespoke checks: light domain-first/reversed outbound-port package naming, web external evaluation-facade isolation, service forbidden inbound package segments, service project-wide native gRPC prohibition, and service provider-facade isolation.

## Compatibility And Benchmark

Production artifacts compile with `--release 21`. Maven Invoker verifies real Java 21 classes locally and compiles a real `--release 25` record fixture on JDK 25 before the plugin scans it. Forked tests also start real Java 21/25 processes with `-Xverify:all` and the published `-javaagent` JAR, including Surefire and Failsafe fixtures.

Build and list the JMH benchmark with:

```bash
./mvnw -B -ntp -pl egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-benchmark -am -DskipTests package
java -jar egon-cola-components/egon-cola-component-bytecode/egon-cola-component-bytecode-benchmark/target/egon-cola-component-bytecode-benchmark-benchmarks.jar -l
```

`ArchitectureScanBenchmark.scanOneThousandClasses` generates its 1,000 deterministic class byte arrays before measurement, then measures parsing, graph construction, all ten rules, and result creation. The controlled target is at or below two seconds; shared CI records performance evidence without applying a noisy absolute threshold.

`ExecutorEnhancementBenchmark` separately records unmatched filtering, 1,000 transformations, direct submission, context-only submission, and context-plus-Micrometer submission. The controlled targets are at most one second for 1,000 transformations and less than five microseconds of submission overhead; shared CI lists and records these benchmarks but does not enforce hardware-sensitive absolute numbers.

`MethodObservationBenchmark` records the direct baseline, disabled bridge, enabled success, enabled exception, and slow-event paths without argument capture. The controlled enabled-success target is below two microseconds; shared CI records the result without applying a hardware-sensitive absolute threshold.
