# Student Management Evaluation Service Archetype Alignment Design

Date: 2026-07-10

## Goal

Align `egon-cola-archetypes/egon-cola-archetype-service` with the evaluation
Project structure, package responsibilities, naming rules, RPC/MQ boundaries,
and validation model in
`egon-cola-archetypes/egon-cola-archetype-service/student-management-service-only-rpc-mq-architecture.md`.

One archetype invocation continues to generate one independent
`student-management-evaluation` Project. It does not generate
`student-management-organization`, a workspace wrapper, or a parent that
aggregates multiple independent Projects.

The generated Project keeps seven Maven modules:

```text
common
facade
domain
application
infrastructure
adapter
starter
```

This design replaces the current thin `course / exam-result` example with
complete, minimal vertical flows for course, course schedule, exam, exam paper,
and score. Cross-Project Organization Facade integration is deliberately
separated into
`docs/superpowers/specs/2026-07-10-evaluation-organization-facade-integration-design.md`
and is not part of the implementation plan produced from this spec.

## Confirmed Decisions

- Optimize only the existing evaluation service archetype.
- Do not add a second organization archetype or generate an organization Project.
- Keep all seven Maven layer modules.
- Use the architecture document's evaluation responsibilities and naming examples
  as the baseline, subject to the explicit overrides in this spec.
- Enforce `infrastructure -> domain` as Infrastructure's only internal module
  dependency.
- Put Repository and MQ Publisher method contracts in Domain as interfaces only.
  Infrastructure supplies JPA and RabbitMQ implementations. The cross-Project
  outbound Client port and Dubbo client are deferred to the separate Facade
  integration spec.
- Keep business Domain Service implementations in Domain. The confirmed
  interface-only rule applies to technical outbound ports, not to core domain
  behavior.
- Add Application-owned Command, Query, and Result models so Adapter never passes
  Facade DTOs or RabbitMQ messages into Domain.
- Keep a management-only HTTP runtime in Starter for Actuator. Do not generate a
  business HTTP API, Controller, Web Filter, GraphQL endpoint, or Web VO.
- Use Dubbo 3 Triple for RPC, RabbitMQ for MQ, and Spring Data JPA with Flyway for
  persistence.
- Implement all evaluation concepts named by the document as minimal working
  flows: course, course schedule, exam, exam paper, and score.
- Use `exam` consistently. Remove the current `examing` package and type naming.
- Keep RabbitMQ delivery basic: working consumers and publishers without a
  generated idempotency store, custom retry policy, dead-letter topology,
  transactional outbox, or exactly-once claim.
- Preserve the non-conflicting runtime engineering baseline and add a generated
  project CI workflow.
- Keep `local` and `test` independent of Nacos, RabbitMQ, PostgreSQL, and other
  running external services.
- Do not start the generated application or a container during implementation
  validation.

## Design Authority And Precedence

Implementation resolves requirements in this order:

1. The decisions confirmed in this spec.
2. The dependency and responsibility contract in this spec.
3. The evaluation Project sections of
   `student-management-service-only-rpc-mq-architecture.md`.
4. The existing runtime engineering baseline where it does not conflict with the
   first three items.
5. Existing service archetype behavior that does not conflict with the preceding
   rules.

The source architecture document remains unchanged in this work. It also covers
the organization Project and cross-Project Facade integration, both of which are
moving independently. This spec is the authoritative, scoped resolution for the
evaluation archetype until the deferred Facade integration spec is separately
approved.

## Explicit Document Overrides

The following differences from the source document are intentional:

- Generate only `student-management-evaluation`.
- Use `exam`, not `examing`.
- Use JPA only. Do not generate MyBatis-Plus packages or dependencies.
- Use RabbitMQ as the concrete MQ adapter.
- Keep a management-only HTTP runtime for Actuator in Starter.
- Keep `infrastructure -> domain`; do not use the conflicting final-summary
  direction `infrastructure -> application`.
- Place technical outbound port interfaces in Domain and their implementations in
  Infrastructure. Do not generate `application.client`.
- Add Application Command, Query, and Result models that are absent from the
  document tree but required by its Adapter/Application boundary.
- Defer Organization Facade dependencies, coordinates, Dubbo references, and
  cross-Project contract tests to the separate integration spec.
- Generate only working packages and types. Do not create empty technology
  packages merely because the document lists possible infrastructure choices.

These are confirmed scope decisions, not omissions.

## Scope

Implementation changes produced from this spec may touch:

- `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources`
- `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/META-INF`
- `egon-cola-archetypes/egon-cola-archetype-service/src/test/resources/projects/basic`
- `egon-cola-archetypes/egon-cola-archetype-service/pom.xml` only if its own
  archetype integration-test configuration requires adjustment
- `.github/workflows/ci_by_multiply_java_versions.yaml` only where repository CI
  must validate the revised generated service project
- Generated POMs, source, tests, resources, README, Docker assets, Maven Wrapper,
  migration, and generated `.github/workflows/ci.yml`

Out of scope:

- `egon-cola-archetype-web`
- `egon-cola-archetype-light`
- `cola-samples`
- Generating or modifying `student-management-organization`
- Depending on `student-management-organization-facade`
- Implementing an Organization Dubbo client
- Cross-Project Facade coordinate parameters or compatibility tests
- Modifying the source architecture document
- MyBatis-Plus, Redis, GraphQL, business HTTP endpoints, or native grpc-java
- MQ idempotency persistence, custom retry/DLQ topology, transactional outbox, or
  broker-backed integration tests
- Starting the generated application, RabbitMQ, PostgreSQL, Nacos, or containers

## Current Gaps To Remove

The current archetype already generates the seven Maven modules, but it does not
yet meet the confirmed contract:

- It models only Course and ExamResult instead of the complete evaluation sample.
- It uses `examing` in package names and verification assertions.
- Adapter imports Domain and Common types through transitive dependencies.
- Application imports Common types even though its only allowed internal
  dependency is Domain.
- Application methods accept primitives and return Domain models instead of
  owning protocol-neutral use-case inputs and results.
- Domain `Client` interfaces duplicate Repository operations, while Infrastructure
  Client implementations merely delegate to Repository implementations.
- The MQ consumer is a plain Spring component rather than a RabbitMQ listener.
- No outbound MQ publisher exists.
- Facade contains placeholder-only enum, exception, and utility packages.
- Common and Facade duplicate response types, and the Common copies are unused.
- Generated tests invoke provider beans directly and do not prove a Dubbo Triple
  Provider/Reference path.
- The PostgreSQL profile lacks the Flyway PostgreSQL database module.
- Pagination does not define stable ordering.
- The generated project does not contain its own CI workflow.

The redesign removes these inconsistencies rather than preserving them for
compatibility.

## Generated Project Shape

The basic archetype IT keeps `rootArtifactId=student-management-evaluation` and a
configurable base package.

```text
student-management-evaluation
|-- pom.xml
|-- README.md
|-- Dockerfile
|-- .dockerignore
|-- .gitignore
|-- .gitattributes
|-- lombok.config
|-- mvnw
|-- mvnw.cmd
|-- .mvn/wrapper/maven-wrapper.properties
|-- .github/workflows/ci.yml
|-- student-management-evaluation-common
|-- student-management-evaluation-facade
|-- student-management-evaluation-domain
|-- student-management-evaluation-application
|-- student-management-evaluation-infrastructure
|-- student-management-evaluation-adapter
`-- student-management-evaluation-starter
```

The root POM retains Spring Boot 3.5.16, Java 21, Egon COLA 5.2.1, Maven
Wrapper 3.9.14, Dubbo 3.3.6, dependency management, annotation processing, and
layered-jar packaging.

Every generated Java package contains `package-info.java`. Empty resource
directories may use a tracked marker only when Maven or the archetype descriptor
requires the directory. Empty Java package scaffolding is not generated.

## Architecture Contract

The exact internal Maven dependency graph is:

```text
common         -> no generated module
facade         -> no generated module
domain         -> common
application    -> domain
infrastructure -> domain
adapter        -> application, facade
starter        -> adapter, infrastructure
```

The graph has these source-level consequences:

- Common imports no generated layer.
- Facade imports no generated layer and owns its full RPC contract.
- Domain may import Common but no other generated layer.
- Application may import Domain but not Common, Facade, Infrastructure, Adapter,
  or Starter.
- Infrastructure may import Domain but not Common, Facade, Application, Adapter,
  or Starter.
- Adapter may import Application and Facade but not Common, Domain,
  Infrastructure, or Starter.
- Starter assembles Adapter and Infrastructure without containing business code
  or importing Domain for business work.
- No source may rely on an undeclared transitive generated-module dependency.

`verify.groovy` must assert the exact internal dependency set for every module.
ArchUnit must assert the same allowed and forbidden source directions.

## Module Design

### Common

Common contains stable, Project-local foundations used by Domain:

```text
common
|-- constants
|-- utils
|-- enums
`-- exceptions
```

Representative types include evaluation-wide constants, identifier utilities,
time utilities, a business-neutral base error contract, and a base exception.
Course status, exam status, score status, table names, queue names, and RabbitMQ
routing keys do not belong in Common.

Common retains `egon-cola-component-common-core` only when generated source uses
it directly. Otherwise the unused dependency is removed. Common does not own RPC
response types; those belong exclusively to Facade.

### Facade

Facade is the independently publishable Evaluation RPC contract:

```text
facade
|-- api
|   |-- CourseFacade.java
|   |-- ExamFacade.java
|   `-- ScoreFacade.java
|-- dto/course
|-- dto/exam
|-- enums
|-- exceptions
`-- utils
```

The three Facades cover course and schedule operations, exam and paper
operations, and score operations. Facade owns request/response DTOs, pagination
DTOs, external enums, external error codes, and assertion helpers. Its types use
Jakarta Validation APIs where appropriate but do not use Spring components or
internal generated-module types.

Facade implementations do not live in this module.

### Domain

Domain contains the evaluation model and business rules:

```text
domain
|-- entities
|   |-- course
|   |   |-- Course.java
|   |   `-- CourseSchedule.java
|   `-- exam
|       |-- Exam.java
|       |-- ExamPaper.java
|       `-- Score.java
|-- aggregates
|   |-- course/CourseAggregate.java
|   `-- exam
|       |-- ExamAggregate.java
|       `-- ScoreAggregate.java
|-- vos
|   |-- course
|   `-- exam
|-- service
|   |-- course
|   |   |-- CourseDomainService.java
|   |   `-- impl/CourseDomainServiceImpl.java
|   `-- exam
|       |-- ExamDomainService.java
|       |-- ScoreDomainService.java
|       `-- impl
|-- repos
|   |-- course
|   `-- exam
|-- event
|   |-- course/CourseEventPublisher.java
|   `-- exam/ExamEventPublisher.java
|-- validators
|   |-- course
|   `-- exam
`-- enums
    |-- course
    `-- exam
```

Repository and Publisher packages contain interfaces only. Publisher methods use
Domain entities, value objects, identifiers, and primitive metadata. Domain does
not define RabbitMQ message records, exchanges, queues, routing keys, listener
settings, or `RabbitTemplate` calls.

Infrastructure implements these technical ports:

- `CourseRepository`
- `CourseScheduleRepository`
- `ExamRepository`
- `ExamPaperRepository`
- `ScoreRepository`
- `CourseEventPublisher`
- `ExamEventPublisher`

Core Domain Service implementations remain in Domain because they express
business rules rather than technology. Domain owns these invariants:

- Course code is normalized and unique.
- Only an active course may be scheduled.
- A course schedule has a valid time range and may not overlap another schedule
  for the same course and class context.
- An exam references an active course and has a valid start/end window.
- A published exam has a paper.
- Paper total points are positive and consistent with its questions or sections.
- A score is between zero and the exam paper's total points.
- A student may not have two active scores for the same exam.
- Score status transitions are controlled by the Score aggregate.

Cross-Project Organization membership or student lookup is not enforced in this
spec. That rule enters only when the deferred Organization Facade integration is
approved. CourseSchedule may carry an opaque class or organization identifier,
but this iteration neither resolves nor validates it through another Project.

### Application

Application owns use cases, transaction boundaries, and protocol-neutral models:

```text
application
|-- manage
|   |-- course
|   |   `-- impl
|   `-- exam
|       `-- impl
|-- command
|   |-- course
|   `-- exam
|-- query
|   |-- course
|   `-- exam
|-- result
|   |-- course
|   `-- exam
|-- converter
|   |-- course
|   `-- exam
|-- validators
|   |-- course
|   `-- exam
|-- config
|   `-- DomainServiceConfiguration.java
`-- assemblers
    |-- course
    `-- exam
```

Package order follows the source document where it is explicit:

```text
application/manage/course/impl/CourseManageImpl.java
application/manage/exam/impl/ExamManageImpl.java
```

Commands, Queries, and Results prevent Adapter from importing Domain models and
prevent Application from importing Facade or RabbitMQ DTOs. Application maps
Domain failures to Application-owned use-case failures that Adapter can consume
without importing Common or Domain exception types.

Application directly invokes Domain Repository, Domain Service, and Domain
Publisher interfaces. It does not use the current persistence-shaped
`CourseClient -> CourseRepository` or `ExamResultClient -> ExamResultRepository`
pass-through layers.

Domain Service implementations are plain Java classes with no Spring annotation.
Application retains an explicit `DomainServiceConfiguration` that creates the
required Domain Service beans. This keeps Spring assembly out of Domain and keeps
Starter free of business-aware bean definitions.

### Infrastructure

Infrastructure implements Domain-owned technical contracts:

```text
infrastructure
|-- repo
|   |-- course
|   |   |-- impl
|   |   |-- po
|   |   |-- jpa
|   |   `-- converter
|   `-- exam
|       |-- impl
|       |-- po
|       |-- jpa
|       `-- converter
|-- mq
|   |-- course
|   |-- exam
|   `-- message
|-- validators
|-- aop
`-- config
```

Package order follows the source document:

```text
infrastructure/repo/course/impl/CourseRepositoryImpl.java
infrastructure/repo/exam/impl/ExamRepositoryImpl.java
```

JPA is the only persistence implementation. MyBatis-Plus packages, mapper XML,
and dependencies are absent.

RabbitMQ publishers implement the Domain Publisher methods. They map Domain
values to Infrastructure-owned message records immediately before calling
`RabbitTemplate`. They do not contain business state transitions or duplicate
Domain validation.

This spec deliberately generates no external Organization client implementation.
Infrastructure gains that adapter only through the deferred Facade integration
work.

### Adapter

Adapter contains RPC and MQ inbound protocols only:

```text
adapter
|-- facade
|   `-- impl
|       |-- course
|       `-- exam
|-- mq
|   |-- course
|   `-- exam
|-- dto
|   |-- course
|   `-- exam
|-- converter
|   |-- course
|   `-- exam
|-- validators
|   |-- course
|   `-- exam
`-- handler
```

Facade implementation paths follow the source document:

```text
adapter/facade/impl/course/CourseFacadeImpl.java
adapter/facade/impl/exam/ExamFacadeImpl.java
adapter/facade/impl/exam/ScoreFacadeImpl.java
```

Each Facade implementation is itself the Dubbo Triple Provider through
`@DubboService`. A second wrapper class named `RpcProvider` is not generated.

RabbitMQ consumers use real `@RabbitListener` bindings and map
Infrastructure-neutral Adapter message DTOs to Application Commands. Consumers
call Application Manage interfaces only. Adapter does not publish MQ messages,
call repositories, or import Domain types.

The following packages and types remain forbidden:

```text
controller
web
filter
graphql
vo
WebMvcConfigurer
RouterFunction
```

### Starter

Starter contains only the Spring Boot entry point, layer assembly,
business-neutral runtime configuration, and resources.

It retains:

- `bootstrap*.yml` and `application*.yml`
- local/test/dev/prod profiles
- AES-256-GCM configuration decryption
- virtual threads and async execution
- graceful shutdown
- Jackson defaults
- Actuator and Prometheus
- executable layered jar
- Docker packaging
- Dubbo Provider configuration
- RabbitMQ listener enablement configuration

`spring-boot-starter-web` is allowed only in Starter to expose the confirmed
management HTTP surface. No other generated module declares Web or WebFlux.

## Representative Business Flows

The sample implements these complete vertical flows:

1. Create and query a course through `CourseFacade`.
2. Schedule an active course after validating time range and schedule conflicts.
3. Create an exam for an active course.
4. Attach and publish an exam paper after validating total points.
5. Record and query a score through `ScoreFacade`.
6. Consume a RabbitMQ score-recording command through the same Application score
   use case.
7. Publish basic course-scheduled, exam-published, and score-recorded messages
   through Domain Publisher methods implemented by Infrastructure.
8. Page course, exam, and score results with explicit stable ordering.

The sample does not expand into a complete education product. Question banks,
grading workflows, appeals, reports, ranking, bulk import, authentication,
authorization, and Organization membership remain out of scope.

## RPC Design

Dubbo 3 Triple remains the concrete RPC implementation.

- Facade owns the wire contract and validation annotations.
- Adapter owns `@DubboService` Provider implementations.
- Providers convert Facade DTOs to Application Commands/Queries and Application
  Results back to Facade responses.
- Application and Domain contain no Dubbo types.
- At least one generated integration test must invoke a Provider through a Dubbo
  Reference with registry `N/A` and a collision-safe test protocol port.
- Provider group and version are centralized in generated configuration rather
  than copied as unrelated literals across classes.

Evaluation Facades are ready to be consumed by another Project, but this spec
does not add a dependency on an Organization Facade or validate cross-Project
compatibility.

## RabbitMQ Design

RabbitMQ is a real but optional runtime integration.

- Adapter owns inbound listeners and inbound message DTOs.
- Infrastructure owns outbound message DTOs, publishers, exchange/routing
  configuration, and `RabbitTemplate` usage.
- Domain owns publisher interfaces containing methods only.
- Application calls Domain publisher methods after the corresponding business
  mutation succeeds.
- Command routing keys and outbound event routing keys are distinct so a
  published event cannot re-enter its command consumer.
- `local` and `test` disable listeners and real publishers by default.
- A deterministic local publisher implements the same Domain interface for
  local/test assembly without contacting RabbitMQ.

The generated sample intentionally does not provide a custom retry interceptor,
dead-letter exchange, dead-letter queue, idempotency record, outbox table, or
delivery guarantee beyond the broker/client defaults. README must state this
limitation plainly. Consumer failures propagate to the listener container rather
than being converted into a false success response.

## Management-Only HTTP Boundary

The generated service has no business HTTP API. Starter's Web runtime exists only
for Actuator management endpoints.

- The configured HTTP port is named `MANAGEMENT_SERVER_PORT` and defaults to the
  existing management port value.
- Exposed endpoints are limited to health, probes, info, metrics, and Prometheus.
- No Controller, Web Filter, Web Interceptor, GraphQL Resolver, business route,
  or Adapter/Web `vo` package is generated. Domain `vos` value-object packages
  remain part of the business model.
- The H2 browser console is disabled in every profile and is not part of the
  management surface.
- Adapter and all business modules must not depend on Web or WebFlux.
- README must explain that adding a business Controller changes the archetype's
  pure-Service boundary and is not a normal extension of this sample.
- `verify.groovy` and ArchUnit guard the difference between Starter's
  management-only runtime and a business Web surface.

## Validation Responsibilities

The four validator categories are separate:

- Adapter validators check RPC and RabbitMQ protocol shape, required fields,
  formats, enum values, and pagination bounds.
- Application validators check use-case prerequisites and workflow ordering.
- Domain validators enforce course, schedule, exam, paper, and score invariants.
- Infrastructure validators translate database constraints and validate
  persistence plus synchronous `RabbitTemplate` publication failures. Publisher
  confirms, retries, and asynchronous delivery acknowledgements are not implied.

Rules are not copied between layers. A protocol-independent rule belongs to
Domain even when first observed through an RPC request or RabbitMQ message.

## Error Handling

- Common provides the Domain's business-neutral base error foundation.
- Domain defines domain failures and port failure contracts.
- Application translates Domain failures to Application-owned use-case failures.
- Adapter maps Application failures to Facade-owned response/error contracts or
  RabbitMQ listener failures.
- Facade responses do not expose internal exception classes or stack traces.
- Infrastructure translates JPA and RabbitMQ exceptions to Domain-facing port
  failures and never leaks JPA or AMQP exception types upward.
- Unexpected failures retain a trace identifier in logs while external responses
  remain sanitized.

No generated error handler claims a RabbitMQ message succeeded when Application
failed.

## Runtime Profiles And External Dependencies

The generated service remains testable without running external infrastructure:

- `local` and `test` use H2.
- RabbitMQ listeners and real publishers are property-gated and disabled in
  `local` and `test`.
- Nacos discovery/config is disabled in `local` and `test`.
- Dubbo registry is `N/A` in `local` and `test`.
- Generated tests use a collision-safe Dubbo protocol port.
- `dev` and `prod` may enable RabbitMQ, Nacos, Dubbo registry, and PostgreSQL
  through environment-backed configuration.
- Production secrets remain external and configuration decryption remains
  fail-fast when encrypted values require a missing or invalid key.

The local publisher and other fallback implementations live in Infrastructure
and implement Domain interfaces. Starter does not contain business-aware fallback
beans.

## Persistence And Flyway

The existing
`V1__init_student_management_evaluation.sql` is immutable. It must not be edited,
renamed, reformatted, deleted, or moved.

Exactly one new migration,
`V2__align_evaluation_course_exam_domain.sql`, performs this deterministic,
additive transition:

1. Add a nullable course code, backfill each V1 course with the deterministic
   value `LEGACY-<course-id>`, make the column required, and add the unique index.
   New Application writes use normal business course codes; legacy rows remain
   queryable through their generated code.
2. Before copying legacy results, guard that every V1 `exam_result.course_id`
   resolves to a course and every score is between zero and 100. If incompatible
   manually inserted data exists, migration fails without deleting, clamping, or
   silently excluding it. The generated migration test covers this failure case.
3. Add course schedule storage and conflict-query indexes.
4. Add exam and exam paper storage plus a new score table.
5. For each valid V1 exam-result row, create a deterministic closed legacy exam
   and 100-point legacy paper keyed by that result ID, then copy the row into the
   new score table using the same score ID, course ID, student ID, score, status,
   and timestamps. One legacy exam per result avoids inventing a uniqueness
   relationship that V1 never enforced.
6. Preserve the original `exam_result` table and rows as read-only compatibility
   data. New generated Java code reads and writes the new score table only; every
   V1 row accepted by the V1 application remains visible through the new Score
   query path after migration.
7. Add uniqueness and lookup indexes for new schedules, exams, papers, and scores.

The migration must use the H2/PostgreSQL common subset used by the archetype and
must not delete or rewrite V1 business values. Deterministic synthetic identifiers
must be collision-safe and covered by migration tests with both empty and
populated V1 schemas.

The Infrastructure POM adds `flyway-database-postgresql` so the dev/prod
PostgreSQL path matches the declared Flyway baseline.

## Build Dependencies

The generated root POM retains the current Spring Boot, Egon COLA BOM, Dubbo,
Nacos, validation, JPA, Flyway, H2, PostgreSQL, MapStruct Plus, Lombok, Actuator,
Prometheus, ArchUnit, Maven Wrapper, and layered-jar dependencies.

It adds or corrects only the confirmed dependencies:

- `spring-boot-starter-amqp` in Adapter for RabbitMQ listeners
- `spring-boot-starter-amqp` in Infrastructure for RabbitMQ publishers
- `flyway-database-postgresql` as PostgreSQL runtime support
- Generated-project CI support files, without adding a build-time framework

Do not add MyBatis-Plus, Redis, GraphQL, native grpc-java, Spring Security,
Spring AI, or Testcontainers.

## Test Design

- Common tests cover generated utility behavior actually used by Domain.
- Facade contract tests cover DTO validation, enum/error contracts, and
  serialization-safe construction.
- Domain tests run without Spring and cover aggregate invariants, schedule
  conflicts, exam publication, paper totals, score ranges, and state transitions.
- Application tests use JUnit 5 and Mockito to verify orchestration, transaction
  boundaries, Repository calls, Publisher calls, and exception translation.
- Adapter tests verify Facade and RabbitMQ consumer conversion, delegation, and
  failure propagation without importing Domain types.
- One Adapter-module Dubbo integration test exercises a real Triple
  Provider/Reference path with no external registry and a mocked Application
  boundary.
- RabbitMQ publisher tests mock `RabbitTemplate`; listener tests invoke the
  listener component without a broker.
- An external-free AMQP configuration test enables the RabbitMQ configuration
  while keeping listener auto-startup disabled, then verifies exchange, queue,
  binding, routing-key, listener-container, publisher, and conditional-bean
  wiring. It does not connect to a broker.
- JPA tests use H2 and cover repositories, converters, constraints, and stable
  pagination ordering.
- A focused Starter `@SpringBootTest` validates complete `test` profile assembly
  with external integrations disabled without directly importing transitive
  business-layer types.
- ArchUnit verifies every allowed and forbidden layer direction and the absence
  of business Web packages.
- No test requires RabbitMQ, Nacos, PostgreSQL, or a running Organization service.

## Archetype Metadata And Verification

`archetype-metadata.xml` must include all new source, resource, test, migration,
README, wrapper, Docker, and generated CI files. It must stop generating obsolete
`ExamResult`, `examing`, duplicate Response, pass-through Client, and empty Java
package artifacts.

The basic `verify.groovy` must assert:

- The generated Project contains exactly the seven confirmed modules.
- Every module has the exact internal dependency set defined by this spec.
- Source imports do not bypass the direct dependency graph through transitive
  modules.
- Course, CourseSchedule, Exam, ExamPaper, and Score structures and representative
  implementations exist.
- `exam` paths exist and `examing`, `ExamResult`, and stale status names are absent
  from active generated Java and documentation.
- Application Command, Query, and Result models exist.
- Adapter imports only Application and Facade generated-layer types.
- Domain Publisher interfaces contain no RabbitMQ imports or implementation code.
- Infrastructure contains RabbitMQ publisher implementations and JPA repository
  implementations.
- Real RabbitMQ listener annotations exist in Adapter.
- No Organization Facade dependency or Organization client implementation is
  generated by this spec.
- Starter alone contains the management Web runtime dependency.
- No Controller, Web Filter, GraphQL, Adapter/Web `vo`, WebFlux, H2 console, or
  business route is generated. Domain `vos` remain allowed.
- Local and test profiles disable RabbitMQ and Nacos.
- The existing V1 migration is unchanged and exactly one V2 migration is added.
- `flyway-database-postgresql` is present.
- The generated CI workflow, Maven Wrapper, Docker assets, and cross-platform
  wrapper behavior are present.
- The generated project still uses parameterized package and artifact coordinates
  rather than hard-coded basic-IT values.

## Generated CI

The generated `.github/workflows/ci.yml` runs on Ubuntu with Java 21 and uses the
generated Maven Wrapper to:

1. Run `clean test` with the `test` profile.
2. Run `-DskipTests package`.
3. Build the Docker image from the already-packaged layered jar.

It does not start the application, a container, RabbitMQ, Nacos, PostgreSQL, or
an Organization service. Repository Strong CI continues to generate the service
project with arbitrary coordinates and runs equivalent test/package/Docker-build
checks.

## README And Developer Guidance

The generated README describes:

- The evaluation-only generation model.
- The seven module responsibilities and exact dependency graph.
- The course and exam business boundaries.
- Application Command/Query/Result ownership.
- Dubbo Triple Provider usage.
- RabbitMQ consumers, publishers, property gates, and the intentionally basic
  delivery semantics.
- The management-only HTTP exception and forbidden business Web surface.
- Local/test fallbacks and dev/prod integration switches.
- The immutable V1 and additive V2 migration model.
- The separate deferred Organization Facade integration spec as future work.
- Commands for test, package, configuration encryption, and Docker build.

README must not describe the current `examing`, ExamResult-only, technology-neutral
MQ, or pass-through Client design as active behavior.

## Design Pattern Consideration

The design uses patterns justified by the architecture:

- Facade isolates the publishable Dubbo contract.
- Adapter isolates Dubbo and RabbitMQ inbound protocols.
- Application Service owns transactions and use-case orchestration.
- Repository ports isolate Domain persistence contracts from JPA.
- Domain Publisher ports isolate business intent from RabbitMQ publication.
- Domain Services and Aggregates own reusable business rules and invariants.
- Converters separate Facade, RabbitMQ, Application, Domain, and persistence
  models.

No persistence Strategy or Factory is introduced because JPA is the only
persistence implementation. No Chain of Responsibility is added for RabbitMQ
because custom retry/DLQ behavior is explicitly out of scope. Transactional
Outbox is not introduced because the user selected basic MQ delivery for this
iteration.

## Validation Commands

Implementation completion requires:

```bash
bash ./mvnw -B -ntp \
  -pl egon-cola-archetypes/egon-cola-archetype-service \
  -am clean integration-test
```

Then run the generated project's wrapper from the archetype IT output:

```bash
SPRING_PROFILES_ACTIVE=test bash ./mvnw -B -ntp clean test
SPRING_PROFILES_ACTIVE=test bash ./mvnw -B -ntp -DskipTests package
```

Also run:

```bash
git diff --check
```

Run targeted stale-name and forbidden-boundary searches for `examing`,
`ExamResult`, `application.client`, business Web packages, Organization Facade
dependencies, and MyBatis-Plus artifacts. Docker build verification may run when
Docker is available, but no application or container is started.

## Risks And Mitigations

- The full evaluation sample is larger than the current two-flow example. Every
  concrete type must belong to a confirmed vertical flow; reject placeholder-only
  classes.
- Renaming `examing` affects metadata, source, tests, verifier assertions, README,
  and persistence mapping. Stale-name searches guard the complete replacement.
- The unchanged V1 uses `exam_result`. V2 preserves that table, migrates every
  compatible row into the new score model, and fails rather than hiding
  incompatible legacy data.
- Management Web can accidentally become a business Web surface. Exact POM,
  package, and ArchUnit guards keep Web isolated to Starter operations.
- Real AMQP dependencies may attempt broker connections. Conditional listeners
  and publisher implementations must be disabled by default in local/test.
- Basic MQ delivery can duplicate or lose messages around failures. README must
  document the limitation rather than imply production-grade delivery semantics.
- Dubbo test ports can collide in parallel CI. Tests must prove their effective
  collision-safe configuration rather than only set an ignored property.
- Archetype filtering can corrupt Maven, YAML, shell, or GitHub Actions
  placeholders. Generated output verification must inspect the rendered files.
- Web Facade work is moving concurrently. Keeping all Organization integration in
  the deferred spec prevents unstable contract details from entering this plan.

## Acceptance Criteria

- `egon-cola-archetype-service` generates one independent
  `student-management-evaluation` Project and no organization Project.
- The generated Project contains exactly the seven confirmed Maven modules.
- POM and source dependencies match the exact architecture contract.
- Adapter does not import Domain or Common, Application does not import Common,
  and Infrastructure does not import Application.
- Course, CourseSchedule, Exam, ExamPaper, and Score have working persistence,
  Domain, Application, RPC, and relevant MQ flows.
- All active Java packages and types use `exam`; `examing` and ExamResult naming
  are removed from active generated artifacts.
- Domain owns technical port method interfaces only, while Infrastructure owns
  JPA and RabbitMQ implementations.
- Dubbo Triple and RabbitMQ are real integrations, with local/test requiring no
  running registry or broker.
- RabbitMQ remains intentionally basic and contains none of the deferred
  reliability mechanisms.
- Starter exposes only management Actuator HTTP; no business Web surface exists.
- The non-conflicting runtime baseline and generated project CI are present.
- Existing Flyway V1 is unchanged and one V2 migration performs the additive
  schema alignment.
- Populated-V1 migration tests prove deterministic legacy course codes,
  synthetic exam/paper linkage, complete score copying, original-row
  preservation, and explicit failure for incompatible V1 data.
- The H2 browser console is disabled in every generated profile.
- No Organization Facade dependency or client implementation is included.
- Archetype integration tests, generated-project tests, package verification,
  and available Docker build verification pass without starting the application.
