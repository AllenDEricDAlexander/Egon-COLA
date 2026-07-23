# Archetype Logback V3 Design

## 1. Goal

Upgrade the Light, Service, and Web archetypes from inconsistent or missing
Logback configuration to one production-ready Spring Boot 3.5 logging contract
based on `JAVA_LOG_V3.md`.

The generated projects must remain dependency-neutral: Spring Boot's managed
SLF4J, Logback, and `StructuredLogEncoder` are sufficient.

## 2. Current Problems

- Light still generates `logback.xml`, so it cannot safely use Spring Boot's
  `springProperty` and `springProfile` extensions.
- Service generates no explicit Logback configuration.
- Web only has a console pattern and no profile-aware routing, rolling files,
  ERROR mirror, capacity budget, or asynchronous main-file output.
- Only Web verifies the presence of a Logback file, and no archetype verifies
  its logging semantics.
- Runtime configuration has no shared logging groups, capacity controls, or
  asynchronous queue controls.

## 3. Selected Design

Each archetype generates the same `logback-spring.xml` behavior:

| Active profile | Output | Purpose |
| --- | --- | --- |
| `local`, `dev`, `test` | Human-readable console | Developer feedback without local file pollution |
| `k8s` | ECS structured JSON console | Container-native collection through stdout |
| Any other profile, including `prod` | Console, asynchronous `application.log`, synchronous `error.log` | Traditional deployment with persistent rolling files |

The production file layout is:

```text
${LOG_HOME}/${spring.application.name}/${INSTANCE_ID-or-HOSTNAME}/
├── application.log
├── error.log
└── archive/
    ├── application.YYYY-MM-DD.N.log.gz
    └── error.YYYY-MM-DD.N.log.gz
```

`application.log` contains every event accepted by the root logger. `error.log`
is an additional exact-ERROR mirror. Main-file writes use `AsyncAppender`;
ERROR writes remain synchronous.

## 4. Log Event Contract

Text logs contain:

- ISO-8601 timestamp with offset, level, PID, thread, and bounded logger name;
- application name, active environment, and instance name;
- MDC keys `traceId`, `spanId`, `requestId`, `tenantId`, `userId`, and `entId`;
- SLF4J 2 fluent key-value pairs through `%kvp`;
- the configured Spring Boot exception conversion word.

The pattern deliberately excludes class, method, line, caller, and file
lookups.

## 5. Runtime Configuration Contract

Every generated `application.yml` defines:

- `app.logging.instance-name`;
- independent ERROR archive capacity;
- async queue size, discard threshold, blocking behavior, and flush timeout;
- `logging.file.path`;
- logging groups for web, persistence, RPC, and messaging stacks;
- root and group levels;
- size-and-time rolling limits and cleanup behavior.

Defaults follow the blog:

```text
queue-size=8192
discarding-threshold=0
never-block=false
max-flush-time=5000
max-file-size=100MB
max-history=30
total-size-cap=10GB
error-total-size-cap=2GB
clean-history-on-start=false
```

All values remain overridable by Spring configuration and environment
variables.

## 6. Archetype Filtering

Archetype XML and YAML resources are Velocity-filtered. Template files must use
the existing `$symbol_dollar` convention so generated projects receive literal
Spring and Logback `${...}` placeholders.

Light's `logback.xml` is replaced, not retained, because two main Logback
configuration files would create ambiguous startup behavior.

## 7. Test Logging

All three archetypes generate a small `logback-test.xml` in the runnable module.
It keeps test output at `WARN` by default while retaining thread, MDC, logger,
key-value, and exception context. It uses only native Logback syntax because
`logback-test.xml` is loaded without Spring Boot's profile extensions.

## 8. Generated-Project Contract

Each `verify.groovy` must assert:

- `logback-spring.xml` and `logback-test.xml` exist in the correct generated
  module;
- Light no longer generates `logback.xml`;
- no `scan` attribute is enabled;
- Spring properties, profile routing, structured JSON, async main file,
  synchronous exact-ERROR mirror, rolling policy, MDC keys, and `%kvp` exist;
- `application.yml` exposes all required logging controls.

The contract checks semantics through stable tokens and XML parsing where
useful; it does not pin incidental whitespace.

## 9. Design Pattern Decision

No application-level Strategy, Factory, Template Method, or new abstraction is
introduced. Logback's declarative profile routing is already the appropriate
policy-selection mechanism. A shared build-time source module would make the
generated archetypes depend on repository internals, so the three self-contained
templates intentionally carry equivalent configuration.

## 10. Validation

Validation is layered:

1. Run the new verifier assertions against the existing generated projects and
   observe the expected failure.
2. Run each affected archetype integration test after implementation.
3. Run the full `egon-cola-archetypes` `clean integration-test` reactor.
4. Inspect generated XML/YAML, run `git diff --check`, and verify the branch is
   clean after commits.

The project is not started automatically.
