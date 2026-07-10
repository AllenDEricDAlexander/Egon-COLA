# Student Management Organization Web Archetype Alignment Design

Date: 2026-07-10

## Goal

Align `egon-cola-archetypes/egon-cola-archetype-web` with the layering, package responsibilities, naming conventions, and technology boundaries in `multi-project-multi-module-architecture.md` while retaining the confirmed organization-only generation model.

One archetype invocation continues to generate one independent `student-management-organization` Project. It does not generate `student-management-evaluation`, a workspace wrapper, or a Maven root that aggregates multiple Projects.

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

The user's phrase "complete one module as the example" means one complete business module, `user`, implemented vertically across all seven Maven layer modules. It does not mean that only one Maven layer module is generated. The `teaching` boundary remains visible through documented extension packages, but it does not receive a second runnable business flow in this iteration.

## Confirmed Decisions

- Generate only `student-management-organization`.
- Keep all seven Maven layer modules and use the architecture document's module responsibilities as the baseline.
- Implement one complete `user` business module as the representative vertical slice.
- Keep `teaching` as a documented extension boundary with `package-info.java` markers rather than a second complete implementation.
- Enforce `infrastructure -> domain` as Infrastructure's only internal module dependency.
- Update `multi-project-multi-module-architecture.md` to use `infrastructure -> domain`, Domain-owned outbound client ports, `exam`, and `converter` consistently.
- Generate Application-owned Command, Query, and Result models so Adapter does not pass Facade DTOs or protocol models into Application.
- Use JPA only. Do not generate MyBatis-Plus packages, mappers, services, XML, configuration, or dependencies.
- Use the generated Facade artifact as the cross-Project contract. Adapter supplies the Dubbo provider implementation; consumers outside this Project depend on the Facade artifact.
- Provide real HTTP, GraphQL, Dubbo, RabbitMQ, Redis, AOP, and OpenAPI integrations for the `user` example.
- Keep `local` and `test` independent of Redis, RabbitMQ, Nacos, a Dubbo registry, PostgreSQL, and other external services.
- Treat normative responsibility and dependency rules as authoritative when an example tree conflicts with them.
- Use the corrected `exam` and `converter` / `Converter` naming consistently.
- Use standard HTTP status semantics and a uniform error body instead of returning HTTP 200 for every business or validation failure.
- Permit changes to the web archetype, its generated-project CI coverage, and the architecture/spec documentation required by this design.
- Do not start the generated application during implementation or validation.

## Design Authority And Precedence

The implementation must resolve requirements in this order:

1. The decisions confirmed in this spec.
2. The dependency and responsibility constraints in `multi-project-multi-module-architecture.md` after the accompanying documentation corrections.
3. The organization package and naming examples in that document.
4. Existing web archetype behavior that does not conflict with the first three items.

This precedence produces four explicit document-example overrides:

- Only the organization Project is generated in this archetype.
- Only the `user` business module is implemented end to end; `teaching` remains an extension boundary.
- JPA is the only persistence implementation.
- Business-specific states and rules do not move into Common even when an older example filename suggests otherwise.

These are deliberate scope decisions, not accidental omissions.

## Scope

Implementation changes may touch:

- `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources`
- `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/META-INF`
- `egon-cola-archetypes/egon-cola-archetype-web/src/test/resources/projects/basic`
- `egon-cola-archetypes/egon-cola-archetype-web/pom.xml` only if the archetype IT requires a local plugin adjustment
- `egon-cola-archetypes/egon-cola-archetype-web/multi-project-multi-module-architecture.md`
- `.github/workflows/ci_by_multiply_java_versions.yaml` only where generated web-project verification must change
- The generated README, Docker, Maven Wrapper, runtime configuration, schema migration, and tests owned by the web archetype

Out of scope:

- `egon-cola-archetype-light`
- `egon-cola-archetype-service`
- Generating or modifying `student-management-evaluation`
- A complete `teaching` implementation
- A production authentication or authorization system
- User interfaces
- Reporting, import/export, and administrative products
- Changes to Egon COLA component implementations or their BOM
- Running external infrastructure or starting the generated application

## Generated Project Shape

The integration-test fixture continues to use `rootArtifactId=student-management-organization` and a configurable base package.

```text
student-management-organization
├── pom.xml
├── README.md
├── Dockerfile
├── .dockerignore
├── .gitignore
├── .gitattributes
├── lombok.config
├── mvnw
├── mvnw.cmd
├── .mvn/wrapper/maven-wrapper.properties
├── student-management-organization-common
├── student-management-organization-facade
├── student-management-organization-domain
├── student-management-organization-application
├── student-management-organization-infrastructure
├── student-management-organization-adapter
└── student-management-organization-starter
```

The generated root POM retains Spring Boot 3.5.16, Java 21, Egon COLA 5.2.1, Maven Wrapper 3.9.14, dependency management, annotation processing, and layered-jar packaging.

Every Java package defined by the generated example contains `package-info.java`. Each module contains `src/main/java`, `src/main/resources`, `src/test/java`, and `src/test/resources`; a minimal tracked marker is allowed only for an otherwise empty resource directory.

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

- Common must not import any other generated layer.
- Facade must not import Common, Domain, Application, Infrastructure, Adapter, or Starter.
- Domain may import Common but no other generated layer.
- Application may import Domain but not Common, Facade, Infrastructure, Adapter, or Starter.
- Infrastructure may import Domain but not Common, Facade, Application, Adapter, or Starter.
- Adapter may import Application and Facade but not Common, Domain, Infrastructure, or Starter.
- Starter may assemble Adapter and Infrastructure but may not contain business code or reach into Domain directly.
- No module may rely on an undeclared transitive generated-module dependency.

`verify.groovy` must assert the exact internal dependency set for every module. ArchUnit must assert the same allowed and forbidden source directions.

## Cross-Project Contract

The organization Facade module is the publishable cross-Project contract. It owns `UserFacade`, request/response DTOs, Facade enums, and Facade errors without depending on another generated module.

Adapter contains `adapter.facade.impl.user.UserFacadeImpl` and exposes it as a Dubbo provider. The implementation delegates only to Application Manage interfaces.

An external evaluation or other Project may depend on the organization Facade artifact and call it through Dubbo. This archetype does not generate that consumer Project and does not add a dependency on an evaluation Facade artifact.

When this Project needs a future outbound Project capability, the port belongs to `domain.client` and its technology adapter belongs to `infrastructure.client.impl`. This placement preserves `infrastructure -> domain` and replaces the architecture document's former `application.client` model.

## Module Design

### Common

Common contains stable, Project-local, business-neutral foundations:

```text
common
├── constants
├── utils
├── enums
└── exceptions
```

Representative types are tracing constants, time utilities, a generic operation status, a base error contract, and a base exception. User status, role status, permission type, Redis business keys, table names, and authorization rules must not live in Common.

Common retains the existing `egon-cola-component-common-core` dependency. Facade does not reuse Common types.

### Facade

Facade contains the public user contract:

```text
facade
├── user
│   ├── UserFacade.java
│   ├── RoleFacade.java
│   └── PermissionFacade.java
├── dto/user
├── enums
├── exceptions
└── utils
```

Facade DTOs are self-contained and may use Jakarta Validation annotations. Facade contains no Spring components and no implementation classes.

### Domain

The complete example lives under the user packages described by the architecture document:

```text
domain
├── entities/user
├── aggregates/user
├── vos/user
├── service/user
│   └── impl
├── repos/user
├── client/user
├── validators/user
└── enums/user
```

Representative types include `User`, `Role`, `Permission`, `UserAggregate`, `RolePermissionAggregate`, `UserId`, `RoleCode`, `PermissionCode`, domain service interfaces and implementations, repositories, outbound ports, validators, and business enums.

Domain owns these invariants:

- User email is normalized and unique.
- Disabled users cannot receive new roles.
- Archived roles cannot be assigned.
- A role cannot be assigned to the same user twice.
- Inactive permissions cannot be granted.
- A permission cannot be granted to the same role twice.

Domain code remains free of Spring MVC, JPA, Redis, RabbitMQ, Dubbo, GraphQL, and protocol DTOs.

### Application

Application owns use cases and transaction boundaries:

```text
application
├── manage/user
│   └── impl
├── command/user
├── query/user
├── result/user
├── converter/user
├── validators/user
└── assemblers/user
```

Commands, Queries, and Results isolate Adapter protocol models from Domain models. Application validates use-case prerequisites, controls transactions, invokes Domain services and repositories, coordinates cache/event ports defined by Domain, and returns Application Results.

Application must not pass Facade DTOs into Domain and must not call JPA, Redis, RabbitMQ, Dubbo, or GraphQL APIs.

### Infrastructure

Infrastructure supplies technology implementations of Domain contracts:

```text
infrastructure
├── repo/user
│   ├── impl
│   ├── po
│   ├── jpa
│   └── converter
├── client/impl
├── validators
├── mq
├── cache
├── aop
└── config
```

JPA is the only persistence implementation. MyBatis-Plus and mapper XML directories are absent.

Infrastructure implements:

- User, role, and permission repositories with Spring Data JPA.
- A Redis-backed user cache and a deterministic in-memory fallback.
- RabbitMQ domain-event publication and a deterministic local publisher.
- Infrastructure validation for JPA constraints, cache payloads, and broker results.
- Technical tracing/timing through an AOP aspect.

Infrastructure imports Domain-owned ports and models directly. It must not import Application to implement a client or reach Application-owned exceptions.

### Adapter

Adapter supplies real inbound protocols for the same Application use cases:

```text
adapter
├── controller/user
├── mq/user
├── rpc
├── graphql
├── facade/impl/user
├── dto/user
├── vo/user
├── converter
├── validators/user
├── handler
└── filter
```

The user flow includes:

- HTTP controllers documented by Springdoc OpenAPI.
- GraphQL queries and mutations with a generated schema.
- Dubbo providers implementing the Facade artifact.
- RabbitMQ command consumers.
- Protocol-specific DTO/VO conversion and validation.
- A global HTTP exception handler and GraphQL error resolver.
- Trace and request-context filters.

Every adapter delegates to an Application Manage interface. No adapter calls Domain or Infrastructure directly.

### Starter

Starter contains only the Spring Boot entry point, component assembly, business-neutral runtime configuration, and resources.

It retains the current runtime baseline:

- `bootstrap*.yml` and `application*.yml`
- local/test/dev/prod profiles
- AES-256-GCM configuration decryption
- virtual threads and async execution
- Tomcat tuning and graceful shutdown
- Jackson defaults
- Actuator and Prometheus
- executable layered jar
- Docker packaging

Technology-specific JPA, Redis, RabbitMQ, and protocol configuration remains in Infrastructure or Adapter rather than moving business-aware beans into Starter.

## Teaching Extension Boundary

The generated Project retains `teaching` extension packages where the architecture document defines the second organization domain. These packages contain `package-info.java` documentation describing SchoolClass and Grade ownership and the expected layer responsibilities.

This iteration does not generate teaching controllers, Facades, Application implementations, repositories, Redis caches, RabbitMQ routes, GraphQL fields, or Dubbo providers. The existing teaching schema in the immutable V1 migration remains untouched for backward compatibility.

The verifier must distinguish this deliberate extension boundary from an accidental missing user implementation: all required user types are concrete and tested, while teaching is intentionally marker-only.

## Representative Business Flows

The complete `user` example implements these shared use cases:

1. Create a user after Adapter format validation, Application duplicate-request checks, Domain invariant validation, and JPA uniqueness checks.
2. Query a user by ID through HTTP, GraphQL, and Dubbo while sharing one Application Query path.
3. Assign an active role to an active user through `UserAggregate`.
4. Grant an active permission to a non-archived role through `RolePermissionAggregate`.
5. Consume a RabbitMQ create-user command through the same create-user Application use case.
6. Cache supported user queries through a Domain-owned cache port and invalidate them after successful mutations.
7. Publish user, role-assignment, and permission-grant events after successful transactions through a Domain-owned event-publisher port.

The example does not implement login, token issuance, a full permission engine, user administration UI, bulk import, reporting, or evaluation workflows.

## Technology Integration

The generated POM retains the existing Spring Boot, Egon COLA BOM, Nacos, Dubbo, JPA, Flyway, H2, PostgreSQL, MapStruct Plus, Lombok, Actuator, Prometheus, ArchUnit, Maven Wrapper, and layered-jar dependencies.

It adds the confirmed real integrations:

- `spring-boot-starter-graphql`
- `spring-boot-starter-amqp` for RabbitMQ
- `spring-boot-starter-data-redis`
- `spring-boot-starter-aop`
- Springdoc OpenAPI's WebMVC UI starter, with one explicit version property in the generated root POM

Adapter owns inbound Web, GraphQL, RabbitMQ, Dubbo, validation, conversion, and OpenAPI dependencies. Infrastructure owns JPA, Redis, outbound RabbitMQ, AOP, Flyway, database driver, and persistence conversion dependencies. Starter depends on Adapter and Infrastructure rather than redeclaring business integration libraries.

## Runtime Profiles And Fallbacks

The integrations are real but do not make local development dependent on external services:

- `local` and `test` use H2.
- Redis beans are property-gated; local/test select an in-memory cache adapter.
- RabbitMQ listeners and publishers are property-gated; local/test select a local event publisher and do not connect to a broker.
- Nacos remains disabled in local/test.
- Dubbo uses no external registry in local/test; provider tests invoke the Adapter component directly.
- GraphQL and HTTP remain available for in-process tests.
- `dev` and `prod` may enable Redis, RabbitMQ, Nacos, a Dubbo registry, and PostgreSQL using environment-backed configuration.

Fallback beans live in Infrastructure and implement the same Domain ports as the real adapters. Conditional configuration must prevent duplicate port implementations and accidental startup connections.

RabbitMQ command and event routing keys are distinct so a published user-created event cannot re-enter the create-user command consumer. Retryable failures use listener retries and dead-letter routing; validation and permanent business failures are rejected without endless retries.

Redis keys are namespaced by application and aggregate identifier, use configurable TTLs, and are invalidated only after successful mutation transactions.

## Validation Responsibilities

The four validator categories remain separate:

- Adapter validators enforce protocol shape, required fields, lengths, formats, enums, and pagination values.
- Application validators enforce authorization context, idempotency, workflow prerequisites, and use-case preconditions.
- Domain validators enforce user, role, permission, status, and relationship invariants.
- Infrastructure validators translate database constraints and validate cache, RabbitMQ, and external technical results.

The same rule must not be copied between layers. A protocol-independent business rule belongs to Domain even if it is first observed through an HTTP request.

## Error Handling

HTTP uses one Adapter-owned error response:

```text
code
message
traceId
timestamp
fieldErrors
```

Status mapping is:

- `400 Bad Request` for malformed input and Adapter validation failures.
- `404 Not Found` for missing users, roles, or permissions.
- `409 Conflict` for duplicate email, duplicate assignment, and optimistic or unique-key conflicts.
- `422 Unprocessable Entity` for a valid request that violates a Domain invariant.
- `503 Service Unavailable` for an enabled Redis, RabbitMQ, Dubbo, or database integration that is unavailable.
- `500 Internal Server Error` for unexpected failures.

HTTP responses do not expose stack traces. GraphQL exposes equivalent error codes in GraphQL extensions. Dubbo uses Facade-owned response/error contracts. RabbitMQ distinguishes retryable technical failures from permanent validation or business failures.

Infrastructure translates JPA, Redis, and RabbitMQ failures into Domain-facing port failures. Adapter performs the final protocol mapping without importing Infrastructure exception types.

## Persistence And Flyway

The existing `V1__init_student_management_organization.sql` is immutable and must not be edited, renamed, reformatted, or deleted.

Exactly one new migration, `V2__extend_organization_user_domain.sql`, adds the schema required by the complete user example:

- roles
- permissions
- user-role relationships
- role-permission relationships
- required unique constraints and indexes

The migration reuses the existing users table and preserves the existing school-class tables. It does not delete or rewrite existing data. SQL must stay within the common H2/PostgreSQL subset used by this archetype.

No second migration is added for teaching because teaching is not implemented in this scope.

## Archetype Metadata And Verification

`archetype-metadata.xml` must include all user Java packages, `package-info.java` files, GraphQL schemas, runtime configuration, V2 migration, and tests. It must preserve wrapper scripts without filtering them incorrectly.

The basic `verify.groovy` contract must assert:

- Exactly one generated `student-management-organization` Project exists.
- The Project contains exactly seven Maven modules.
- Each module's internal dependency set matches the architecture contract exactly.
- The complete user class/package tree exists across all seven modules.
- Teaching contains only the confirmed extension markers, not a second runnable implementation.
- Adapter/Application/Domain/Infrastructure validator packages exist and contain exercised validators.
- Application Command, Query, Result, converter, validator, and assembler packages exist.
- `adapter.facade.impl.user` is the only Facade implementation location.
- JPA packages and dependencies exist.
- MyBatis-Plus packages, dependencies, mapper XML, and configuration are absent.
- HTTP, GraphQL, Dubbo, RabbitMQ, Redis, AOP, and Springdoc dependencies and representative implementations exist.
- Local and test profiles disable external connections and select fallback ports.
- HTTP error tests assert standard status codes rather than a universal 200 response.
- `exam` and `converter` naming is used; the legacy misspellings are absent from active generated sources and documentation.
- The existing V1 migration remains unchanged and exactly one V2 migration exists.
- Maven Wrapper 3.9.14, Docker, and cross-platform post-generation behavior remain intact.

The verifier may be decomposed into focused helper closures but must remain one generation-time contract and must not introduce a new test framework.

## Test Design

- Common tests cover tracing, time, error, and business-neutral helper behavior.
- Facade contract tests cover DTO validation and serialization without loading internal modules.
- Domain tests use JUnit 5 without a Spring context and cover user, role, permission, aggregate, and status invariants.
- Application tests use JUnit 5 and Mockito to verify orchestration, transaction boundaries, idempotency, port calls, and result assembly.
- HTTP tests use Spring MVC test support and mocked Application interfaces.
- GraphQL tests use Spring GraphQL test support without starting a real server.
- Dubbo provider tests invoke Adapter providers with mocked Application interfaces.
- RabbitMQ consumer and publisher tests verify delegation, routing, retry classification, and failure behavior without a broker.
- Redis adapter tests verify keying, TTL, serialization, invalidation, and fallback selection without requiring Redis.
- JPA repository and converter tests use `@DataJpaTest` with H2.
- AOP tests verify interception and error propagation without changing business results.
- Starter assembly tests use `@SpringBootTest` with the test profile and all external services disabled.
- ArchUnit verifies every allowed and forbidden layer dependency.
- Existing AES-GCM and runtime configuration tests remain covered.

No test requires Redis, RabbitMQ, Nacos, PostgreSQL, a Dubbo registry, or a running generated application.

## CI And Validation Commands

Implementation completion requires:

```bash
bash ./mvnw -B -ntp -pl egon-cola-archetypes/egon-cola-archetype-web -am clean integration-test
```

Then validate the generated Project directly from the archetype IT output:

```bash
bash ./mvnw -B -ntp clean verify
bash ./mvnw -B -ntp -DskipTests package
docker build -t egon-generated-web:java-21 .
```

Also run XML validation for the archetype metadata and generated POMs, `git diff --check`, and targeted stale-name searches.

The generated-project CI workflow must continue verifying web generation on Java 21. It may be updated to assert the revised user flow and integration dependencies, but it must not start the generated application or require external infrastructure.

## README Requirements

The generated README must document:

- The organization-only generation model.
- The seven Maven modules and exact dependency graph.
- The complete user example and marker-only teaching boundary.
- HTTP, GraphQL, Dubbo, RabbitMQ, Redis, JPA, AOP, and OpenAPI responsibilities.
- The Facade artifact contract for external consumers.
- Local/test fallback behavior and dev/prod enablement properties.
- Standard HTTP error semantics.
- Test, package, encryption CLI, Docker build, and optional local run commands.

The README must not claim that evaluation is generated, must not document MyBatis-Plus, and must not use the legacy exam or converter misspellings.

## Design Pattern Consideration

The design uses patterns already justified by the architecture:

- Facade isolates the publishable cross-Project contract.
- Adapter isolates HTTP, GraphQL, Dubbo, and RabbitMQ entry protocols.
- Application Service owns transactions and use-case orchestration.
- Repository isolates Domain persistence contracts from JPA.
- Domain Service and Aggregate concentrate business invariants.
- Ports and Adapters lets Redis, RabbitMQ, and local fallbacks implement Domain-owned contracts without reversing dependencies.
- Observer-style event publication decouples committed user changes from RabbitMQ delivery.
- Converters and Assemblers separate protocol, use-case, domain, and persistence models.

No persistence Strategy is added because JPA is the only persistence choice. No abstract factory, builder hierarchy, responsibility chain, or decorator layer is introduced because the confirmed user flow has no variation point that justifies it.

## Risks And Mitigations

- Real integrations can accidentally make local/test require external services. Mitigation: property-gate both bean creation and listeners, supply deterministic Domain-port fallbacks, and prove assembly with tests.
- The one-business-module scope can be mistaken for one Maven module. Mitigation: verifier and README explicitly require all seven Maven modules and label `user` as the representative business module.
- Existing code relies on transitive internal module imports. Mitigation: remove those imports and enforce exact POM plus ArchUnit rules.
- Facade DTOs can leak into Application. Mitigation: Application-owned Commands, Queries, and Results are mandatory.
- Redis cache invalidation can race with failed transactions. Mitigation: perform invalidation and event publication only after successful mutation completion.
- RabbitMQ command and event routes can loop. Mitigation: use separate command and event routing namespaces and test them explicitly.
- Spring starters can auto-connect unexpectedly. Mitigation: conditional bean/listener configuration plus external-free Starter assembly tests.
- JPA schema extension can damage compatibility. Mitigation: preserve V1, add exactly one backward-safe V2, and keep existing teaching tables untouched.
- The generated sample is larger. Mitigation: reject concrete classes that are not attached to the user flow; teaching remains package documentation only.
- Archetype filtering can corrupt Maven and Spring placeholders. Mitigation: scope filtered file sets and assert the generated values.

## Acceptance Criteria

- One archetype invocation generates one independent `student-management-organization` Project and no evaluation Project.
- The generated Project contains all seven Maven modules.
- The exact internal dependency graph is enforced in POMs, generated source, ArchUnit, and `verify.groovy`.
- Infrastructure depends internally only on Domain.
- The user business module is complete across Common, Facade, Domain, Application, Infrastructure, Adapter, and Starter.
- Teaching remains a clearly documented marker-only extension boundary.
- Application uses its own Commands, Queries, Results, converters, validators, and assemblers.
- User creation, lookup, role assignment, permission grant, caching, command consumption, and event publication work through shared Application use cases.
- HTTP, GraphQL, Dubbo, RabbitMQ, Redis, JPA, AOP, OpenAPI, filters, converters, and all four validator layers have real tested implementations.
- JPA is the only persistence implementation and no MyBatis-Plus artifact remains.
- External consumers use the standalone organization Facade artifact and Dubbo provider.
- HTTP errors use the confirmed status mapping and uniform error body.
- Local/test validation requires no external services.
- The existing Flyway V1 is unchanged and exactly one V2 migration extends the user schema.
- `exam` and `converter` naming is used consistently in active documentation and generated code.
- The archetype IT, generated-project tests, package validation, Docker build, XML validation, and CI path pass without starting the application.
