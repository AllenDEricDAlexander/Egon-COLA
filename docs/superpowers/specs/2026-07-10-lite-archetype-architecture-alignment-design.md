# Lite Archetype Architecture Alignment Design

Date: 2026-07-10

## Goal

Optimize `egon-cola-archetypes/egon-cola-archetype-light` so the generated project implements the package structure, naming conventions, and responsibilities described by section 4, "结构示例 + 命名示例", of `large-monolith-light-domain-architecture.md`.

The generated result remains one Spring Boot application, one Maven module, and one deployment unit. `start`, `adapter`, `facade`, `application`, `infrastructure`, `common`, and `domain` are Java package boundaries inside that monolith, not Maven submodules.

## Confirmed Decisions

- Treat the architecture document's section 4 package structure and naming examples as the implementation baseline.
- Replace the current `student` sample with the documented `user / role / permission` and `teaching / school class / course` model.
- Provide working implementations for HTTP, GraphQL, Dubbo RPC, RabbitMQ, Redis, external HTTP clients, JPA, AOP, filters, converters, and all four validator categories.
- Use JPA only. Do not generate MyBatis-Plus mapper or service packages and do not add MyBatis-Plus dependencies.
- Use RabbitMQ for inbound and outbound messaging.
- Keep `local` and `test` free of external service requirements. H2 and local fallback beans are the defaults; Redis, RabbitMQ, Nacos, and external HTTP clients are enabled only by configuration.
- Remove the old `StudentController`, `StudentManagementFacade`, `Student` domain model, and student-course application flow.
- Use Spring Boot Test through `spring-boot-starter-test`, with JUnit 5 and Mockito. Do not add TestNG, PowerMock, or another mocking framework.
- Do not start the generated application during implementation validation.

## Scope

Implementation changes are limited to:

- `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources`
- `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/META-INF/maven/archetype-metadata.xml`
- `egon-cola-archetypes/egon-cola-archetype-light/src/test/resources/projects/basic`
- The light archetype's own `pom.xml` only when its integration-test setup needs an adjustment

Other archetypes, repository-wide runtime behavior, and unrelated components are out of scope.

## Architecture Contract

The generated project's allowed internal layer dependencies are:

```text
start -> adapter / infrastructure
adapter -> application / facade
application -> domain
domain -> common
infrastructure -> domain
facade -> no internal layer dependency
common -> no other business layer dependency
```

The rules have these consequences:

- Adapter code must not import Domain, Common, Infrastructure, or Start types.
- Application code must not import Adapter, Facade, Common, Infrastructure, or Start types.
- Infrastructure code must not import Application, Adapter, Facade, Common, or Start types.
- Domain code may import Common but no other internal layer.
- Facade and Common remain independently consumable.
- Start performs Spring Boot startup and assembly without containing business logic.

ArchUnit must enforce both the allowed directions and the corresponding forbidden directions. Tests must fail when a generated project introduces an undeclared cross-layer dependency.

## Document Overrides Required By The Dependency Contract

Two parts of the document's example tree are superseded by the confirmed dependency direction:

- `application.client` is not generated because an Infrastructure implementation would require the forbidden `infrastructure -> application` dependency. Outbound capability contracts are Domain Service interfaces instead.
- `domain/*/service/impl` is not generated. Domain packages expose service interfaces, while implementations live under `infrastructure/service/*/impl`.

These are deliberate changes, not omissions. All other document structures remain the baseline except for the confirmed JPA-only persistence choice.

## Generated Package Structure

### Start

`start` contains `StudentManagementApplication` and business-neutral configuration only:

```text
start
├── StudentManagementApplication.java
└── config
    ├── JacksonConfig.java
    ├── OpenApiConfig.java
    ├── ActuatorConfig.java
    ├── async
    └── encryption
```

The current async, configuration decryption, executable-jar, Docker, and runtime profile baselines remain intact. Technology-specific JPA, Redis, RabbitMQ, and client configuration belongs to Infrastructure.

### Adapter

```text
adapter
├── controller
│   ├── user
│   └── teaching
├── mq
│   ├── user
│   └── teaching
├── rpc
│   ├── user
│   └── teaching
├── graphql
│   ├── user
│   └── teaching
├── facade/impl
│   ├── user
│   └── teaching
├── dto
│   ├── user
│   └── teaching
├── vo
│   ├── user
│   └── teaching
├── convertor
│   ├── user
│   └── teaching
├── handler
├── filter
└── validators
```

Representative concrete types include:

- `UserController`, `RoleController`, `PermissionController`, `SchoolClassController`, and `CourseController`
- `UserImportedConsumer` and `CourseImportedConsumer`
- `UserRpcProvider` and `CourseRpcProvider`
- `UserResolver` and `CourseResolver`
- `UserFacadeImpl`, `PermissionFacadeImpl`, `SchoolClassFacadeImpl`, and `CourseFacadeImpl`
- `CreateUserRequest`, `AssignRoleRequest`, `GrantPermissionRequest`, `CreateSchoolClassRequest`, `CreateCourseRequest`, and `ScheduleCourseRequest`
- `UserDetailVO`, `PermissionTreeVO`, `SchoolClassDetailVO`, and `CourseDetailVO`
- `GlobalExceptionHandler`, `ResponseWrapperHandler`, `TraceIdFilter`, and `RequestContextFilter`

Every inbound adapter validates and converts its protocol model, then delegates to an Application Manage interface. No inbound adapter may contain core business rules or call a Domain or Infrastructure type directly.

### Facade

```text
facade
├── user
│   ├── UserFacade.java
│   └── PermissionFacade.java
├── teaching
│   ├── SchoolClassFacade.java
│   └── CourseFacade.java
├── dto
│   ├── user
│   └── teaching
├── enums
├── exceptions
└── utils
```

Facade contracts use only Facade-owned DTOs, enums, exceptions, and utilities. Dubbo providers live in Adapter and delegate to `adapter.facade.impl`; Facade has no Spring, Domain, Common, or Application dependency.

### Application

```text
application
├── manage
│   ├── user
│   │   ├── UserManage.java
│   │   ├── RoleManage.java
│   │   ├── PermissionManage.java
│   │   └── impl
│   └── teaching
│       ├── SchoolClassManage.java
│       ├── CourseManage.java
│       └── impl
├── convertor
├── validators
└── assemblers
```

Each `manage.<domain>` package also owns the Commands, Queries, and Results used by Adapter. These boundary models prevent Adapter from importing Domain models and prevent Application from importing Facade DTOs.

Application controls transactions, idempotency, operation-level authorization checks, workflow prerequisites, and cross-domain orchestration. It depends only on Domain entities, aggregates, repositories, validators, and service interfaces.

### Domain

The user domain contains:

```text
domain/user
├── entities
│   ├── User.java
│   ├── Role.java
│   └── Permission.java
├── aggregates
│   ├── UserAggregate.java
│   └── RolePermissionAggregate.java
├── vos
│   ├── UserId.java
│   ├── RoleCode.java
│   └── PermissionCode.java
├── service
├── repos
├── validators
├── enums
└── exceptions
```

The teaching domain contains:

```text
domain/teaching
├── entities
│   ├── SchoolClass.java
│   └── Course.java
├── aggregates
│   ├── SchoolClassAggregate.java
│   └── CourseAggregate.java
├── vos
│   ├── SchoolClassId.java
│   ├── CourseCode.java
│   └── Semester.java
├── service
├── repos
├── validators
├── enums
└── exceptions
```

Domain Service contracts include business services such as `UserDomainService`, `RoleDomainService`, `PermissionDomainService`, `SchoolClassDomainService`, and `CourseDomainService`. User-owned outbound ports are `UserQueryService`, `UserCacheService`, and `UserEventPublisher`; teaching-owned outbound ports are `TeachingQueryService`, `CourseCacheService`, and `TeachingEventPublisher`. Keeping the publishers domain-specific avoids adding an undocumented shared Domain package.

Repository contracts include `UserRepository`, `RoleRepository`, `PermissionRepository`, `SchoolClassRepository`, and `CourseRepository`.

Domain objects own business state and invariants. Domain validators enforce rules that must hold regardless of whether the request arrived through HTTP, GraphQL, RPC, or RabbitMQ.

### Infrastructure

```text
infrastructure
├── repo
│   ├── user
│   │   ├── impl
│   │   ├── po
│   │   ├── jpa
│   │   └── converter
│   └── teaching
│       ├── impl
│       ├── po
│       ├── jpa
│       └── converter
├── service
│   ├── user/impl
│   └── teaching/impl
├── validators
├── client
│   ├── user/impl
│   └── teaching/impl
├── aop
├── mq
│   ├── user
│   └── teaching
├── cache
│   ├── user
│   └── teaching
└── config
```

JPA types use the document's naming style: `UserPO`, `RolePO`, `PermissionPO`, `UserRolePO`, `RolePermissionPO`, `SchoolClassPO`, `CoursePO`, and schedule-association PO types. Spring Data interfaces use names such as `UserJpaRepository` and `CourseJpaRepository`.

Infrastructure Service implementations implement Domain Service interfaces. External HTTP clients, Redis caches, and RabbitMQ publishers also implement Domain-owned outbound ports, preserving `infrastructure -> domain` as the only Infrastructure layer dependency.

### Common

```text
common
├── constants
├── utils
├── enums
└── exceptions
```

Common contains only stable, business-neutral primitives such as base error contracts, generic identifiers, time helpers, and tracing constants. Facade does not reuse Common types.

## Business Flows

The generated sample implements these complete vertical flows:

1. Create a user after request validation and external identity lookup.
2. Assign an active role to an active user through `UserAggregate`.
3. Grant an active permission to a non-archived role through `RolePermissionAggregate`.
4. Create a school class and a course.
5. Schedule a course for a school class after validating class state, course state, semester, and time conflicts.
6. Consume user-imported and course-imported RabbitMQ messages through the same Application use cases.
7. Query users, permissions, classes, and courses through HTTP, GraphQL, and Dubbo without duplicating business logic.
8. Cache supported queries in Redis and invalidate affected entries after successful mutations.
9. Publish user and authorization events through `UserEventPublisher`, and class, course, and scheduling events through `TeachingEventPublisher`, after successful transactions.

The sample does not expand into a complete product. Login UI, full authentication, bulk export, reporting, and administrative workflows not required by the architecture example remain out of scope.

## Validation Responsibilities

The four validator categories are implemented separately:

- Adapter validators check protocol shape, required fields, formats, enum values, and pagination values.
- Application validators check operation permissions, idempotency, workflow prerequisites, and multi-domain preconditions.
- Domain validators enforce entity, aggregate, status-transition, assignment, authorization, and scheduling invariants.
- Infrastructure validators check external responses, cache payloads, JPA constraint failures, and RabbitMQ publication results.

Validation logic must not be copied between layers. A rule that remains true for every entry protocol belongs to Domain, not Adapter.

## Runtime Profiles And External Dependencies

The generated application remains runnable and testable without external infrastructure:

- `local` and `test` use H2.
- `local` and `test` select in-memory cache adapters, local event publishers, and deterministic local external-client adapters.
- RabbitMQ listeners and producers are conditional on an explicit property and disabled by default in `local` and `test`.
- Redis adapters are conditional and disabled by default in `local` and `test`.
- Nacos and real external HTTP clients remain disabled in `local` and `test`.
- `dev` and `prod` can enable Redis, RabbitMQ, Nacos, Dubbo registry, and real HTTP clients through environment-backed configuration.

Fallback implementations live in Infrastructure and implement the same Domain ports. Start does not contain business-aware fallback beans.

The real external clients use Spring `RestClient` with environment-backed base URLs. `UserQueryService` resolves an external user by identifier, while `TeachingQueryService` resolves a source course by course code. Their local implementations return deterministic configured data rather than performing network calls.

RabbitMQ uses one application topic exchange. Separate user-imported and course-imported queues feed Adapter consumers; Domain event routing keys cover user, authorization, class, course, and schedule changes. Retryable failures follow the configured listener retry policy and exhausted messages route to dead-letter queues. Redis keys are namespaced by application and aggregate type, with configurable TTLs and explicit invalidation after mutations.

## Error Handling

- Common supplies the base error representation and business-neutral exception foundation.
- Domain defines domain-facing exception types that may extend the Common foundation.
- Application catches Domain failures at use-case boundaries and exposes Application-owned use-case exceptions to Adapter.
- HTTP maps failures through `GlobalExceptionHandler`.
- GraphQL maps failures to GraphQL errors without exposing stack traces.
- RPC maps failures to Facade-owned error contracts.
- RabbitMQ classifies retryable and non-retryable failures and avoids acknowledging failed messages as successful.
- Infrastructure translates JPA, Redis, RabbitMQ, and external HTTP failures into Domain-facing port failures.

This chain preserves the layer dependency contract while keeping protocol-specific error handling at the edge.

## Persistence And Flyway

The existing `V1__init_student_management.sql` is immutable and must not be edited.

Exactly one new migration, `V2__align_large_monolith_domain.sql`, will:

- Add users, roles, permissions, user-role relations, role-permission relations, school classes, and class-course scheduling structures.
- Extend or reuse the existing course table where compatible.
- Copy compatible student data into the new user structure without deleting source rows.
- Preserve the old student and student-course tables as unused compatibility tables so existing generated-project data is not destroyed.

Generated Java code must not reference the deprecated Student tables. The V2 SQL must remain compatible with the H2 test path and PostgreSQL production path.

## Build Dependencies

The generated POM keeps the current Spring Boot parent, Java 21, Egon COLA BOM, JPA, Flyway, H2, PostgreSQL, Dubbo, Nacos, MapStruct Plus, Actuator, Prometheus, Maven Wrapper, and layered-jar setup.

It adds only dependencies required by the confirmed implementation:

- `spring-boot-starter-graphql`
- `spring-boot-starter-amqp`
- `spring-boot-starter-data-redis`
- `spring-boot-starter-aop`
- Springdoc OpenAPI's WebMVC starter

Testing uses:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>
```

`spring-boot-starter-test` is retained because it aggregates Spring Boot Test, JUnit 5, Mockito, Spring Test, and test auto-configuration. Declaring only `spring-boot-test` would not supply the complete confirmed test stack.

## Test Design

- Domain tests use JUnit 5 without a Spring context and cover aggregate invariants, status transitions, duplicate assignments, and scheduling conflicts.
- Application tests use JUnit 5 and Mockito through `@ExtendWith(MockitoExtension.class)` to verify orchestration, transactions, port calls, and exception translation.
- HTTP tests use Spring Boot's MVC test support and mocked Application Manage interfaces.
- GraphQL behavior is tested through the Spring Boot test HTTP boundary without starting a real server.
- RPC providers and RabbitMQ consumers are tested as adapter components with Mockito-based delegation and failure-path assertions.
- JPA repositories, converters, and constraints use `@DataJpaTest` with H2.
- A small number of `@SpringBootTest` tests validate complete `local/test` assembly with external integrations disabled.
- ArchUnit verifies every allowed and forbidden layer dependency.
- No test requires Redis, RabbitMQ, Nacos, PostgreSQL, or an external HTTP service.

## Archetype Metadata And Verification

`archetype-metadata.xml` must include all new Java packages, `package-info.java` files, GraphQL schemas, configuration, migration, and tests. It must stop generating obsolete Student Java types.

The basic `verify.groovy` must assert:

- The generated project is a single Maven module.
- The documented package and representative class tree exists.
- Domain Service interfaces are under Domain and their implementations are under Infrastructure.
- `application.client` and `domain/*/service/impl` are absent.
- JPA packages exist and MyBatis-Plus packages and dependencies are absent.
- RabbitMQ, Redis, GraphQL, AOP, OpenAPI, and Spring Boot Test dependencies are present.
- Local and test profiles disable external integrations.
- No active Java source references the old Student sample.
- The V1 migration remains unchanged and exactly one V2 migration is added.
- Maven Wrapper and existing cross-platform verification behavior remain intact.

## README And Developer Guidance

The generated README must describe:

- The single-module monolith model.
- Package responsibilities and the exact dependency graph.
- The five primary business use cases.
- Which integrations are real but disabled in `local/test`.
- How to enable RabbitMQ, Redis, Nacos, Dubbo registry, and external clients in `dev/prod`.
- Commands for test, package, configuration encryption, Docker build, and local run.

It must not imply that package layers are Maven modules and must not mention the removed Student sample.

## Design Pattern Consideration

The design uses patterns already justified by the architecture:

- Repository ports isolate Domain persistence contracts from JPA.
- Application Service coordinates transactions and cross-domain use cases.
- Domain Service contracts expose domain capabilities while Infrastructure supplies the confirmed implementations.
- Adapter and Facade patterns isolate HTTP, GraphQL, RPC, and RabbitMQ protocols.
- Observer-style domain event publication decouples successful business changes from RabbitMQ delivery.
- Assemblers and converters separate protocol, use-case, domain, and persistence models.

No persistence Strategy or Factory is introduced because JPA is the only persistence implementation. No additional abstract factories, handler chains, or decorators are added unless the implementation exposes a concrete need.

## Validation Commands

Implementation completion requires, in order:

```bash
bash ./mvnw -B -ntp -pl egon-cola-archetypes/egon-cola-archetype-light -am clean integration-test
```

Then run the generated project's wrapper test command from the archetype IT output:

```bash
bash ./mvnw -B -ntp test
```

Also run `git diff --check` and targeted stale-name searches for `StudentController`, `StudentManagementFacade`, the old Student domain model, and MyBatis-Plus artifacts. Validation must not start the application.

## Risks And Mitigations

- The generated sample is substantially larger. Keep every class attached to one of the confirmed vertical flows and reject placeholder-only types.
- Strict package boundaries can be accidentally bypassed by converter or exception imports. ArchUnit must check each layer explicitly.
- Conditional Infrastructure beans can cause missing or duplicate implementations. Assembly tests must cover both default fallbacks and property selection.
- RabbitMQ and Redis starters may trigger unwanted connections. Listener and adapter creation must be property-gated, not merely documented.
- H2 and PostgreSQL SQL behavior can diverge. Keep V2 to their common SQL subset and validate both paths when available.
- The immutable V1 migration leaves deprecated tables in a fresh schema. V2 preserves them solely for backward-safe migration; application code and documentation treat them as inactive.
- Archetype filtering can corrupt Maven or Spring placeholders. Metadata filtering must be scoped and verified against generated output.

## Acceptance Criteria

- The generated project remains one Maven module and matches the confirmed package dependency graph.
- The section 4 package and naming baseline is represented, subject only to the explicit JPA-only and Domain-Service-implementation overrides.
- User, role, permission, school class, course, and scheduling flows work end to end.
- HTTP, GraphQL, Dubbo, RabbitMQ, Redis, external HTTP, JPA, AOP, filters, converters, and four validator layers have real exercised implementations.
- Domain Service interfaces reside in Domain and Infrastructure provides their implementations without importing Application.
- Local and test validation requires no external services.
- Tests use Spring Boot Test, JUnit 5, Mockito, JPA test slices, and ArchUnit.
- Existing Flyway V1 is unchanged and one V2 migration performs the schema extension.
- The archetype integration test and generated-project Maven tests pass.
- No generated application is started during validation.
