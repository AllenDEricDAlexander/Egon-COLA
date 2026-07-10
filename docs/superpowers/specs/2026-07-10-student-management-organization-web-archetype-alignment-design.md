# Student Management Organization Web Archetype Alignment Design

Date: 2026-07-10

## Goal

Align `egon-cola-archetypes/egon-cola-archetype-web` completely with the `student-management-organization` portion of `multi-project-multi-module-architecture.md`, including its layering, package responsibilities, naming conventions, technology boundaries, and both organization domains, while retaining the confirmed organization-only generation model.

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

`student-management-organization` contains both documented domains, `user` and `teaching`. Both domains have complete Facade, Application, Domain, Infrastructure, and Adapter implementations, supported by the shared business-neutral Common and assembled by Starter. The earlier interpretation that only `user` would be concrete and `teaching` would remain marker-only is superseded by the user's explicit correction.

## Confirmed Decisions

- Generate only `student-management-organization`.
- Keep all seven Maven layer modules and use the architecture document's module responsibilities as the baseline.
- Implement both `user` and `teaching` completely in the five business-bearing modules, with shared Common foundations and Starter assembly.
- Enforce `infrastructure -> domain` as Infrastructure's only internal module dependency.
- Update `multi-project-multi-module-architecture.md` to use `infrastructure -> domain`, Domain-owned outbound client ports, `exam`, and `converter` consistently.
- Generate Application-owned Command, Query, and Result models so Adapter does not pass Facade DTOs or protocol models into Application.
- Use JPA only. Do not generate MyBatis-Plus packages, mappers, services, XML, configuration, or dependencies.
- Use the generated Facade artifact as the cross-Project contract. Adapter supplies the Dubbo provider implementation; consumers outside this Project depend on the Facade artifact.
- Provide real HTTP, GraphQL, Dubbo, RabbitMQ, Redis, AOP, and OpenAPI integrations for both `user` and `teaching`.
- Keep the bidirectional organization/evaluation consumer integration deferred until `2026-07-10-evaluation-organization-facade-integration-design.md` passes its separate approval gates.
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
- JPA is the only persistence implementation.
- `EvaluationClient`, `CourseClient`, and `EvaluationClientImpl` are not generated while the separately documented bidirectional Facade integration remains deferred.
- Business-specific states and rules do not move into Common even when an older example filename suggests otherwise.

These are deliberate scope decisions, not accidental omissions. Subject to those four overrides, every organization `user` and `teaching` type in section 4.2 of the architecture document is mandatory unless this spec names an explicit replacement.

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

The organization Facade module is the publishable cross-Project contract. It owns the user, role, permission, school-class, and grade Facades; their request/response DTOs; Facade enums; and Facade errors without depending on another generated module.

Adapter contains the five Facade implementations under `adapter.facade.impl.user` and `adapter.facade.impl.teaching`. They are the only implementations of the Facade interfaces and delegate only to Application Manage interfaces.

`adapter.rpc.UserRpcProvider` exports the user, role, and permission Facade implementation beans. `adapter.rpc.SchoolClassRpcProvider` exports the school-class and grade Facade implementation beans. These RPC provider classes own Dubbo export configuration and lifecycle only; they do not implement the Facade interfaces or duplicate business delegation.

An external evaluation or other Project may depend on the organization Facade artifact and call it through Dubbo. This archetype does not generate that consumer Project and does not add a dependency on an evaluation Facade artifact.

The future outbound direction is governed by `docs/superpowers/specs/2026-07-10-evaluation-organization-facade-integration-design.md`. It will place consumer-owned ports in `domain.client` and technology adapters in `infrastructure.client.impl`, preserving `infrastructure -> domain`. This spec does not authorize placeholder interfaces, fake DTOs, external Facade dependencies, or outbound Dubbo clients before those contracts are approved.

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

Required Common types are `OrganizationCommonConstants`, `OrganizationDateUtils`, `OrganizationTraceUtils`, `OrganizationOperationStatus`, `OrganizationBaseException`, and `OrganizationErrorCode`, exactly as named in architecture section 4.2.2. User status, role status, permission type, Redis business keys, table names, and authorization rules must not live in Common.

Common retains the existing `egon-cola-component-common-core` dependency. Facade does not reuse Common types.

### Facade

Facade contains the public contracts for both organization domains:

```text
facade
├── user
│   ├── UserFacade.java
│   ├── RoleFacade.java
│   └── PermissionFacade.java
├── teaching
│   ├── SchoolClassFacade.java
│   └── GradeFacade.java
├── dto
│   ├── user
│   └── teaching
├── enums
├── exceptions
└── utils
```

Required user DTOs are `CreateUserDTO`, `UserDetailDTO`, `AssignRoleDTO`, `GrantPermissionDTO`, and `PermissionTreeDTO`. Required teaching DTOs are `CreateSchoolClassDTO`, `SchoolClassDetailDTO`, `CreateGradeDTO`, `GradeDetailDTO`, and `AssignUserToClassDTO`.

Facade DTOs are self-contained and may use Jakarta Validation annotations. Facade contains no Spring components and no implementation classes.

### Domain

Both organization domains use the complete Domain structure described by the architecture document:

```text
domain
├── entities
│   ├── user
│   └── teaching
├── aggregates
│   ├── user
│   └── teaching
├── vos
│   ├── user
│   └── teaching
├── service
│   ├── user/impl
│   └── teaching/impl
├── repos
│   ├── user
│   └── teaching
├── client
│   ├── user
│   └── teaching
├── validators
│   ├── user
│   └── teaching
└── enums
    ├── user
    └── teaching
```

Required user types include `User`, `Role`, `Permission`, `UserAggregate`, `RolePermissionAggregate`, `UserId`, `RoleCode`, `PermissionCode`, the documented user service interfaces and implementations, `UserRepository`, `RoleRepository`, `PermissionRepository`, validators, and business enums.

Required teaching types include `SchoolClass`, `Grade`, `SchoolClassAggregate`, `SchoolClassId`, `GradeCode`, `SchoolClassDomainService`, `SchoolClassDomainServiceImpl`, `SchoolClassRepository`, `GradeRepository`, `SchoolClassDomainValidator`, `SchoolClassStatus`, and `GradeStatus`.

The generated enum values are fixed: `UserStatus={ACTIVE,DISABLED}`, `RoleStatus={ACTIVE,ARCHIVED}`, `PermissionType={API,MENU,ACTION}`, `PermissionStatus={ACTIVE,INACTIVE}`, `SchoolClassStatus={ACTIVE,ARCHIVED}`, and `GradeStatus={ACTIVE,ARCHIVED}`.

New emails are trimmed and lowercased with `Locale.ROOT` before uniqueness checks and persistence. New role, permission, and grade codes are trimmed, uppercased with `Locale.ROOT`, and must match `[A-Z][A-Z0-9_]{1,63}`. Names are trimmed, preserve internal whitespace, and compare case-insensitively for the “class name inside one grade” uniqueness rule. Restored legacy Grade codes are the sole exception to the new-code pattern.

The Domain technical ports are narrow and technology-neutral. `UserCachePort`, `SchoolClassCachePort`, and `GradeCachePort` each expose `findById`, `put`, and `evict` using Domain identifiers and models; TTL and serialization do not appear in their signatures. `CommandIdempotencyPort` exposes `claim(operation, requestId)` and `release(operation, requestId)` without Redis types. `OrganizationEventPublisher` exposes `publish(OrganizationDomainEvent)` only. All Domain event records implement `OrganizationDomainEvent(eventId, aggregateId, occurredAt)` and add these fields: `UserChangedEvent(changeType)`, `RoleAssignedEvent(roleCode)`, `PermissionGrantedEvent(roleCode, permissionCode)`, `GradeChangedEvent(changeType)`, `SchoolClassChangedEvent(gradeId, changeType)`, and `SchoolClassMembershipChangedEvent(userId, changeType)`.

Domain rejects invariants through `OrganizationDomainException` and exposes port failures through `OrganizationPortException`; both carry an `OrganizationDomainErrorCode` but no HTTP, GraphQL, Dubbo, Redis, JPA, or RabbitMQ detail. Application maps those failures to `OrganizationApplicationException(failureType, code, message)`, where `OrganizationFailureType={VALIDATION,FORBIDDEN,NOT_FOUND,CONFLICT,DOMAIN_REJECTED,DEPENDENCY_UNAVAILABLE,INTERNAL}` maps one-to-one to the stable `ORG_*` protocol codes. Adapter maps only the Application exception to HTTP/GraphQL/RabbitMQ, while Facade implementations map it to `OrganizationFacadeException`; no inbound adapter imports Domain or Infrastructure exceptions.

Domain owns these invariants:

- User email is normalized and unique.
- Disabled users cannot receive new roles.
- Archived roles cannot be assigned.
- A role cannot be assigned to the same user twice.
- Inactive permissions cannot be granted.
- A permission cannot be granted to the same role twice.
- Grade codes are unique and immutable after creation.
- An archived grade cannot receive a new school class.
- School class names are unique inside one grade.
- A disabled user cannot be assigned to a school class.
- A user cannot be assigned to the same school class twice.
- School class membership changes must validate both aggregates and persist one authoritative membership relationship.

Domain code remains free of Spring MVC, JPA, Redis, RabbitMQ, Dubbo, GraphQL, and protocol DTOs.

### Application

Application owns use cases and transaction boundaries:

```text
application
├── manage
│   ├── user/impl
│   └── teaching/impl
├── command
│   ├── user
│   └── teaching
├── query
│   ├── user
│   └── teaching
├── result
│   ├── user
│   └── teaching
├── converter
│   ├── user
│   └── teaching
├── validators
│   ├── user
│   └── teaching
└── assemblers
    ├── user
    └── teaching
```

Commands, Queries, and Results isolate Adapter protocol models from Domain models. Application validates use-case prerequisites, controls transactions, invokes Domain services and repositories, coordinates cache/event ports defined by Domain, and returns Application Results.

User requires `UserManage`, `RoleManage`, `PermissionManage` and their implementations. Teaching requires `SchoolClassManage`, `GradeManage` and their implementations. The concrete Command, Query, Result, Converter, Validator, and Assembler types mirror the documented user and teaching Facade operations rather than leaving either domain as package markers.

Application owns the cross-domain transaction that assigns a user to a school class. The use case loads the User and SchoolClass aggregates through Domain repositories, validates both states, persists one authoritative `school_class_users` relationship through the teaching repository, and publishes events only after a successful commit.

Application must not pass Facade DTOs into Domain and must not call JPA, Redis, RabbitMQ, Dubbo, or GraphQL APIs.

### Infrastructure

Infrastructure supplies technology implementations of Domain contracts:

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
├── client/impl
├── validators
├── mq
├── cache
├── aop
└── config
```

JPA is the only persistence implementation. MyBatis-Plus and mapper XML directories are absent.

Infrastructure implements:

- User, role, permission, school-class, grade, and membership repositories with Spring Data JPA.
- Redis-backed user, school-class, and grade caches plus deterministic in-memory fallbacks.
- RabbitMQ domain-event publication for both domains plus a deterministic local publisher.
- Infrastructure validation for JPA constraints, cache payloads, and broker results.
- Technical tracing/timing through an AOP aspect.

`RabbitOrganizationEventPublisher` implements the Domain port and delegates serialization/routing to `OrganizationEventProducer`; `LocalOrganizationEventPublisher` implements the same port for local/test. These are outbound components only and never consume messages.

Required teaching persistence types include `SchoolClassRepositoryImpl`, `GradeRepositoryImpl`, `SchoolClassPO`, `GradePO`, the membership PO required by the assignment flow, `SchoolClassJpaRepository`, `GradeJpaRepository`, and their converters. These are concrete, tested implementations, not directory placeholders.

Infrastructure imports Domain-owned ports and models directly. It must not import Application to implement a client or reach Application-owned exceptions.

### Adapter

Adapter supplies real inbound protocols for the same Application use cases:

```text
adapter
├── controller
│   ├── user
│   └── teaching
├── mq
│   ├── user
│   └── teaching
├── rpc
├── graphql
├── facade/impl
│   ├── user
│   └── teaching
├── dto
│   ├── user
│   └── teaching
├── vo
│   ├── user
│   └── teaching
├── converter
├── validators
│   ├── user
│   └── teaching
├── handler
└── filter
```

Both domain flows include:

- HTTP controllers documented by Springdoc OpenAPI.
- GraphQL queries and mutations with a generated schema.
- Dubbo providers implementing the Facade artifact.
- RabbitMQ command consumers.
- Protocol-specific DTO/VO conversion and validation.
- A global HTTP exception handler and GraphQL error resolver.
- Trace and request-context filters.

Every adapter delegates to an Application Manage interface. No adapter calls Domain or Infrastructure directly.

Required teaching adapters include `SchoolClassController`, `GradeController`, `SchoolClassChangedConsumer`, `SchoolClassResolver`, `SchoolClassFacadeImpl`, `GradeFacadeImpl`, teaching request/response models, converters, and validators. `SchoolClassResolver` groups the GraphQL operations for both SchoolClass and Grade, just as `UserResolver` groups the user, role, and permission operations. The Facade implementation beans remain the only contract implementations; the `rpc` package exports those beans through Dubbo rather than reimplementing the same interfaces.

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
- `OrganizationSwaggerConfig`, `OrganizationJacksonConfig`, and `OrganizationActuatorConfig`
- `logback-spring.xml` and `logback-test.xml`

Technology-specific JPA, Redis, and RabbitMQ configuration remains in Infrastructure. Inbound protocol handlers remain in Adapter. Business-neutral OpenAPI, Jackson, Actuator, logging, and runtime assembly configuration remains in Starter as required by the architecture document.

## Complete Teaching Domain

`teaching` is a complete domain vertical, not an extension marker. SchoolClass and Grade contracts, use cases, domain rules, persistence, inbound adapters, caches, messages, Facade implementations, GraphQL resolvers, Dubbo exposure, tests, and documentation are generated alongside the complete `user` domain.

The existing V1 school-class tables remain immutable and are reused. The V2 migration adds the Grade schema and compatible relationships required to complete teaching without deleting existing school-class or membership data.

The required-file manifest and verifier treat missing teaching classes exactly like missing user classes: either omission fails archetype integration testing.

## Canonical Organization File Manifest

Section 4.2 of `multi-project-multi-module-architecture.md` is the base required-file manifest for `student-management-organization`. Every organization file and Java type named there is mandatory for both `user` and `teaching`, subject only to the explicit replacements, additions, and exclusions below. Dotted package nodes such as `facade.impl`, `client.impl`, and `service.impl` mean nested Java package directories.

| Layer | Required baseline and additions | Explicit exclusions or replacements |
| --- | --- | --- |
| Starter | All section 4.2.1 classes and resources, plus the current local profile, bootstrap profiles, AES-GCM configuration/decryption classes, async/virtual-thread configuration, layered-jar packaging, Docker files, and Maven Wrapper | Keep the architecture name `OrganizationSwaggerConfig`; OpenAPI annotations remain in Adapter |
| Common | All section 4.2.2 types and tests | No business enums, business keys, table names, or Facade-shared models |
| Facade | All section 4.2.3 user and teaching Facades, DTOs, enums, exception, utility, and contract test; add `GrantPermissionDTO` and `AssignUserToClassDTO` for the confirmed flows | No implementation and no dependency on another generated module |
| Application | All section 4.2.4 user and teaching Manage, converter, validator, assembler, Command, Query, Result, and test types; add `GrantPermissionCommand`, `CreateGradeCommand`, `AssignUserToClassCommand`, `GradeDetailQuery`, `GradeDetailResult`, `GradeApplicationValidator`, and `GradeAssembler` | No Facade DTO, protocol model, persistence object, or technical client API |
| Domain | All section 4.2.5 user and teaching entity, aggregate, value object, service, repository, validator, enum, and test types; add Domain-owned cache ports, event-publisher port, and user/teaching event models needed by the confirmed flows | Defer `EvaluationClient` and `CourseClient`; no Spring MVC, JPA, Redis, RabbitMQ, Dubbo, or GraphQL types |
| Infrastructure | All JPA-side user and teaching repository implementations, persistent objects, Spring Data repositories, converters, validator, aspect, event producer, caches, JPA/Redis/RabbitMQ configuration, resources, and tests from section 4.2.6; add user-role, role-permission, and school-class-membership persistence types plus real Redis and local fallback port implementations | Remove all `mp` packages, Mapper types/XML, MP services, and `OrganizationMybatisPlusConfig`; defer `EvaluationClientImpl`; the production migration is `V1` plus exactly one `V2`, not a separate test-only schema history |
| Adapter | All section 4.2.7 user and teaching controllers, consumers, RPC providers, resolvers, Facade implementations, DTOs, VOs, converters, validators, handler, filters, and tests; add `GrantPermissionRequest`, `CreateGradeRequest`, `AssignUserToClassRequest`, `GradeDetailVO`, GraphQL error resolution, and `schema.graphqls` | Facade implementations exist only under `adapter.facade.impl.user` and `adapter.facade.impl.teaching`; RPC providers only export those beans |

For the following lists, `<base>` means the generated `${packageInPathFormat}` directory. These are the fixed paths added to the section 4.2 baseline; their names are not implementation-plan choices:

```text
${rootArtifactId}-facade/src/main/java/<base>/facade/dto/user/GrantPermissionDTO.java
${rootArtifactId}-facade/src/main/java/<base>/facade/dto/teaching/AssignUserToClassDTO.java

${rootArtifactId}-application/src/main/java/<base>/application/converter/user/PermissionApplicationConverter.java
${rootArtifactId}-application/src/main/java/<base>/application/config/package-info.java
${rootArtifactId}-application/src/main/java/<base>/application/config/DomainServiceConfiguration.java
${rootArtifactId}-application/src/main/java/<base>/application/context/package-info.java
${rootArtifactId}-application/src/main/java/<base>/application/context/OrganizationRequestContext.java
${rootArtifactId}-application/src/main/java/<base>/application/context/OrganizationRequestContextHolder.java
${rootArtifactId}-application/src/main/java/<base>/application/exceptions/package-info.java
${rootArtifactId}-application/src/main/java/<base>/application/exceptions/OrganizationApplicationException.java
${rootArtifactId}-application/src/main/java/<base>/application/exceptions/OrganizationFailureType.java
${rootArtifactId}-application/src/main/java/<base>/application/validators/teaching/GradeApplicationValidator.java
${rootArtifactId}-application/src/main/java/<base>/application/assemblers/user/PermissionAssembler.java
${rootArtifactId}-application/src/main/java/<base>/application/assemblers/teaching/GradeAssembler.java
${rootArtifactId}-application/src/main/java/<base>/application/command/user/GrantPermissionCommand.java
${rootArtifactId}-application/src/main/java/<base>/application/command/teaching/CreateGradeCommand.java
${rootArtifactId}-application/src/main/java/<base>/application/command/teaching/AssignUserToClassCommand.java
${rootArtifactId}-application/src/main/java/<base>/application/query/user/PermissionTreeQuery.java
${rootArtifactId}-application/src/main/java/<base>/application/query/teaching/GradeDetailQuery.java
${rootArtifactId}-application/src/main/java/<base>/application/result/user/PermissionTreeResult.java
${rootArtifactId}-application/src/main/java/<base>/application/result/teaching/GradeDetailResult.java

${rootArtifactId}-domain/src/main/java/<base>/domain/client/OrganizationEventPublisher.java
${rootArtifactId}-domain/src/main/java/<base>/domain/client/CommandIdempotencyPort.java
${rootArtifactId}-domain/src/main/java/<base>/domain/enums/user/PermissionStatus.java
${rootArtifactId}-domain/src/main/java/<base>/domain/client/user/UserCachePort.java
${rootArtifactId}-domain/src/main/java/<base>/domain/client/teaching/SchoolClassCachePort.java
${rootArtifactId}-domain/src/main/java/<base>/domain/client/teaching/GradeCachePort.java
${rootArtifactId}-domain/src/main/java/<base>/domain/events/package-info.java
${rootArtifactId}-domain/src/main/java/<base>/domain/events/OrganizationDomainEvent.java
${rootArtifactId}-domain/src/main/java/<base>/domain/events/user/package-info.java
${rootArtifactId}-domain/src/main/java/<base>/domain/events/user/UserChangedEvent.java
${rootArtifactId}-domain/src/main/java/<base>/domain/events/user/RoleAssignedEvent.java
${rootArtifactId}-domain/src/main/java/<base>/domain/events/user/PermissionGrantedEvent.java
${rootArtifactId}-domain/src/main/java/<base>/domain/events/teaching/package-info.java
${rootArtifactId}-domain/src/main/java/<base>/domain/events/teaching/GradeChangedEvent.java
${rootArtifactId}-domain/src/main/java/<base>/domain/events/teaching/SchoolClassChangedEvent.java
${rootArtifactId}-domain/src/main/java/<base>/domain/events/teaching/SchoolClassMembershipChangedEvent.java
${rootArtifactId}-domain/src/main/java/<base>/domain/exceptions/package-info.java
${rootArtifactId}-domain/src/main/java/<base>/domain/exceptions/OrganizationDomainException.java
${rootArtifactId}-domain/src/main/java/<base>/domain/exceptions/OrganizationPortException.java
${rootArtifactId}-domain/src/main/java/<base>/domain/exceptions/OrganizationDomainErrorCode.java

${rootArtifactId}-infrastructure/src/main/java/<base>/infrastructure/repo/user/po/UserRolePO.java
${rootArtifactId}-infrastructure/src/main/java/<base>/infrastructure/repo/user/po/RolePermissionPO.java
${rootArtifactId}-infrastructure/src/main/java/<base>/infrastructure/repo/user/jpa/UserRoleJpaRepository.java
${rootArtifactId}-infrastructure/src/main/java/<base>/infrastructure/repo/user/jpa/RolePermissionJpaRepository.java
${rootArtifactId}-infrastructure/src/main/java/<base>/infrastructure/repo/teaching/po/SchoolClassUserPO.java
${rootArtifactId}-infrastructure/src/main/java/<base>/infrastructure/repo/teaching/jpa/SchoolClassUserJpaRepository.java
${rootArtifactId}-infrastructure/src/main/java/<base>/infrastructure/cache/OrganizationCacheKey.java
${rootArtifactId}-infrastructure/src/main/java/<base>/infrastructure/cache/RedisUserCache.java
${rootArtifactId}-infrastructure/src/main/java/<base>/infrastructure/cache/RedisSchoolClassCache.java
${rootArtifactId}-infrastructure/src/main/java/<base>/infrastructure/cache/RedisGradeCache.java
${rootArtifactId}-infrastructure/src/main/java/<base>/infrastructure/cache/InMemoryUserCache.java
${rootArtifactId}-infrastructure/src/main/java/<base>/infrastructure/cache/InMemorySchoolClassCache.java
${rootArtifactId}-infrastructure/src/main/java/<base>/infrastructure/cache/InMemoryGradeCache.java
${rootArtifactId}-infrastructure/src/main/java/<base>/infrastructure/cache/RedisCommandIdempotencyAdapter.java
${rootArtifactId}-infrastructure/src/main/java/<base>/infrastructure/cache/InMemoryCommandIdempotencyAdapter.java
${rootArtifactId}-infrastructure/src/main/java/<base>/infrastructure/mq/OrganizationEventMessage.java
${rootArtifactId}-infrastructure/src/main/java/<base>/infrastructure/mq/RabbitOrganizationEventPublisher.java
${rootArtifactId}-infrastructure/src/main/java/<base>/infrastructure/mq/LocalOrganizationEventPublisher.java
${rootArtifactId}-infrastructure/src/main/java/<base>/infrastructure/config/OrganizationIntegrationProperties.java
${rootArtifactId}-infrastructure/src/main/java/<base>/infrastructure/config/OrganizationLocalFallbackConfig.java
${rootArtifactId}-infrastructure/src/main/resources/db/migration/V2__complete_organization_domains.sql

${rootArtifactId}-adapter/src/main/java/<base>/adapter/dto/user/GrantPermissionRequest.java
${rootArtifactId}-adapter/src/main/java/<base>/adapter/dto/user/CreateUserMessage.java
${rootArtifactId}-adapter/src/main/java/<base>/adapter/dto/teaching/CreateGradeRequest.java
${rootArtifactId}-adapter/src/main/java/<base>/adapter/dto/teaching/AssignUserToClassRequest.java
${rootArtifactId}-adapter/src/main/java/<base>/adapter/dto/teaching/CreateSchoolClassMessage.java
${rootArtifactId}-adapter/src/main/java/<base>/adapter/vo/user/PermissionTreeVO.java
${rootArtifactId}-adapter/src/main/java/<base>/adapter/vo/teaching/GradeDetailVO.java
${rootArtifactId}-adapter/src/main/java/<base>/adapter/converter/RoleAdapterConverter.java
${rootArtifactId}-adapter/src/main/java/<base>/adapter/converter/PermissionAdapterConverter.java
${rootArtifactId}-adapter/src/main/java/<base>/adapter/converter/GradeAdapterConverter.java
${rootArtifactId}-adapter/src/main/java/<base>/adapter/handler/OrganizationErrorResponse.java
${rootArtifactId}-adapter/src/main/java/<base>/adapter/handler/OrganizationGraphQlExceptionResolver.java
${rootArtifactId}-adapter/src/main/resources/graphql/schema.graphqls
```

The fixed additional test paths are:

```text
${rootArtifactId}-domain/src/test/java/<base>/domain/user/RolePermissionAggregateTest.java
${rootArtifactId}-domain/src/test/java/<base>/domain/teaching/GradeDomainServiceTest.java
${rootArtifactId}-application/src/test/java/<base>/application/user/RoleManageImplTest.java
${rootArtifactId}-application/src/test/java/<base>/application/user/PermissionManageImplTest.java
${rootArtifactId}-application/src/test/java/<base>/application/teaching/GradeManageImplTest.java
${rootArtifactId}-application/src/test/java/<base>/application/teaching/AssignUserToClassUseCaseTest.java
${rootArtifactId}-infrastructure/src/test/java/<base>/infrastructure/teaching/GradeRepositoryImplTest.java
${rootArtifactId}-infrastructure/src/test/java/<base>/infrastructure/OrganizationCacheTest.java
${rootArtifactId}-infrastructure/src/test/java/<base>/infrastructure/OrganizationRabbitMqContractTest.java
${rootArtifactId}-infrastructure/src/test/java/<base>/infrastructure/OrganizationFlywayMigrationTest.java
${rootArtifactId}-infrastructure/src/test/java/<base>/infrastructure/OrganizationInfrastructureProfileTest.java
${rootArtifactId}-adapter/src/test/java/<base>/adapter/OrganizationHttpErrorContractTest.java
${rootArtifactId}-adapter/src/test/java/<base>/adapter/OrganizationGraphQlContractTest.java
${rootArtifactId}-adapter/src/test/java/<base>/adapter/OrganizationDubboProviderConfigurationTest.java
${rootArtifactId}-adapter/src/test/java/<base>/adapter/OrganizationRabbitMqConsumerTest.java
${rootArtifactId}-adapter/src/test/java/<base>/adapter/OrganizationFilterTest.java
${rootArtifactId}-starter/src/test/java/<base>/starter/ArchitectureDependencyTest.java
${rootArtifactId}-starter/src/test/java/<base>/starter/OrganizationFlowTest.java
${rootArtifactId}-starter/src/test/java/<base>/starter/OrganizationRollbackTest.java
${rootArtifactId}-starter/src/test/java/<base>/starter/OrganizationOpenApiTest.java
```

The current runtime-baseline additions are also fixed:

```text
${rootArtifactId}-starter/src/main/java/<base>/starter/config/async/AsyncConfiguration.java
${rootArtifactId}-starter/src/main/java/<base>/starter/config/async/package-info.java
${rootArtifactId}-starter/src/main/java/<base>/starter/config/encryption/package-info.java
${rootArtifactId}-starter/src/main/java/<base>/starter/config/encryption/AesGcmConfigDecryptor.java
${rootArtifactId}-starter/src/main/java/<base>/starter/config/encryption/ConfigCipherCli.java
${rootArtifactId}-starter/src/main/java/<base>/starter/config/encryption/ConfigDecryptEnvironmentPostProcessor.java
${rootArtifactId}-starter/src/main/java/<base>/starter/config/encryption/ConfigDecryptException.java
${rootArtifactId}-starter/src/main/java/<base>/starter/config/encryption/ConfigDecryptKeyProvider.java
${rootArtifactId}-starter/src/main/java/<base>/starter/config/encryption/ConfigDecryptor.java
${rootArtifactId}-starter/src/main/resources/META-INF/spring.factories
${rootArtifactId}-starter/src/main/resources/application-local.yml
${rootArtifactId}-starter/src/main/resources/application-dev.yml
${rootArtifactId}-starter/src/main/resources/application-test.yml
${rootArtifactId}-starter/src/main/resources/application-prod.yml
${rootArtifactId}-starter/src/main/resources/bootstrap-local.yml
${rootArtifactId}-starter/src/main/resources/bootstrap-dev.yml
${rootArtifactId}-starter/src/main/resources/bootstrap-test.yml
${rootArtifactId}-starter/src/main/resources/bootstrap-prod.yml
${rootArtifactId}-starter/src/main/resources/logback-spring.xml
${rootArtifactId}-starter/src/test/resources/logback-test.xml
${rootArtifactId}-starter/src/test/java/<base>/starter/config/encryption/AesGcmConfigDecryptorTest.java
${rootArtifactId}-starter/src/test/java/<base>/starter/config/encryption/ConfigDecryptEnvironmentPostProcessorTest.java
${rootArtifactId}-starter/src/test/java/<base>/starter/config/encryption/package-info.java
```

The fixed forbidden-path and stale-token manifest is:

```text
student-management-evaluation/**
**/src/**/java/**/convertor/**
**/src/**/java/**/facade/impl/** outside ${rootArtifactId}-adapter
${rootArtifactId}-adapter/src/main/java/<base>/adapter/facade/user/**
${rootArtifactId}-adapter/src/main/java/<base>/adapter/facade/teaching/**
${rootArtifactId}-adapter/src/main/java/<base>/adapter/validation/**
${rootArtifactId}-facade/src/main/java/<base>/facade/dto/PageResponse.java
${rootArtifactId}-facade/src/main/java/<base>/facade/dto/user/CreateUserRequest.java
${rootArtifactId}-facade/src/main/java/<base>/facade/dto/user/UserDTO.java
${rootArtifactId}-facade/src/main/java/<base>/facade/dto/teaching/CreateSchoolClassRequest.java
${rootArtifactId}-facade/src/main/java/<base>/facade/dto/teaching/AssignUserToClassRequest.java
${rootArtifactId}-facade/src/main/java/<base>/facade/dto/teaching/SchoolClassDTO.java
${rootArtifactId}-common/src/main/java/<base>/common/response/**
${rootArtifactId}-domain/src/main/java/<base>/domain/common/**
${rootArtifactId}-domain/src/main/java/<base>/domain/enums/UserStatus.java
${rootArtifactId}-domain/src/main/java/<base>/domain/client/user/EvaluationClient.java
${rootArtifactId}-domain/src/main/java/<base>/domain/client/teaching/CourseClient.java
${rootArtifactId}-domain/src/main/java/<base>/domain/client/user/UserClient.java
${rootArtifactId}-domain/src/main/java/<base>/domain/client/teaching/SchoolClassClient.java
${rootArtifactId}-infrastructure/src/main/java/<base>/infrastructure/client/impl/EvaluationClientImpl.java
${rootArtifactId}-infrastructure/src/main/java/<base>/infrastructure/client/impl/user/UserClientImpl.java
${rootArtifactId}-infrastructure/src/main/java/<base>/infrastructure/client/impl/teaching/SchoolClassClientImpl.java
${rootArtifactId}-infrastructure/src/main/java/<base>/infrastructure/repo/**/mp/**
${rootArtifactId}-infrastructure/src/main/java/<base>/infrastructure/config/OrganizationMybatisPlusConfig.java
${rootArtifactId}-infrastructure/src/main/resources/mapper/**
${rootArtifactId}-infrastructure/src/test/resources/db/migration/**
${rootArtifactId}-starter/src/main/java/<base>/starter/config/OrganizationOpenApiConfig.java
**/src/**/java/**/*Po.java
stale text tokens: "convertor", "examing"
```

The manifest also requires `package-info.java` in every generated main and test Java package, and requires `src/main/java`, `src/main/resources`, `src/test/java`, and `src/test/resources` in every Maven module. The implementation must encode the required and forbidden paths as fixed canonical lists in `verify.groovy`; it must not derive the expected list from whatever the archetype happens to generate.

Common, Facade, Domain, and Application use a fixed `src/main/resources/.gitkeep` when no runtime resource belongs there, and `archetype-metadata.xml` explicitly includes that filename. Resource-directory existence may not depend on the local filesystem preserving an empty directory.

This reference-plus-delta table is the only allowed interpretation of “complete organization tree.” An implementation plan may split it into task-sized checklists, but may not silently treat any listed user or teaching type as illustrative or optional.

## Organization Business Flows

The generated Project implements these shared use cases across both domains:

1. Create a user after Adapter format validation, Application duplicate-request checks, Domain invariant validation, and JPA uniqueness checks.
2. Query a user by ID through HTTP, GraphQL, and Dubbo while sharing one Application Query path.
3. Assign an active role to an active user through `UserAggregate`.
4. Grant an active permission to a non-archived role through `RolePermissionAggregate`.
5. Consume a RabbitMQ create-user command through the same create-user Application use case.
6. Cache supported user queries through a Domain-owned cache port and invalidate them after successful mutations.
7. Publish user, role-assignment, and permission-grant events after successful transactions through a Domain-owned event-publisher port.
8. Create a grade after validating its code, status, and uniqueness.
9. Create a school class inside an active grade after validating the grade and class-name uniqueness.
10. Query grades and school classes through HTTP, GraphQL, and Dubbo while sharing Application Query paths.
11. Assign an active user to an active school class through one cross-domain Application transaction.
12. Consume a RabbitMQ school-class command through the same SchoolClass Application use case.
13. Cache supported grade and school-class queries and invalidate affected entries after successful mutations.
14. Publish grade, school-class, and membership events only after successful transactions.

The Project does not implement login, token issuance, a full permission engine, user administration UI, bulk import, reporting, course/exam evaluation workflows, or the separately deferred cross-Project Facade integration.

## Frozen Provider And Runtime Contracts

This section freezes the organization-owned contract expected by every adapter and test. It does not freeze or generate the deferred evaluation consumer contract.

### Facade Contract

The five Facades expose exactly these operations:

```text
UserFacade.createUser(CreateUserDTO) -> UserDetailDTO
UserFacade.getUser(String userId) -> UserDetailDTO
RoleFacade.assignRole(AssignRoleDTO) -> void
PermissionFacade.grantPermission(GrantPermissionDTO) -> void
PermissionFacade.getPermissionTree(String userId) -> PermissionTreeDTO
GradeFacade.createGrade(CreateGradeDTO) -> GradeDetailDTO
GradeFacade.getGrade(String gradeId) -> GradeDetailDTO
SchoolClassFacade.createSchoolClass(CreateSchoolClassDTO) -> SchoolClassDetailDTO
SchoolClassFacade.getSchoolClass(String schoolClassId) -> SchoolClassDetailDTO
SchoolClassFacade.assignUser(AssignUserToClassDTO) -> void
```

Facade records have these fields in this order:

| DTO | Fields |
| --- | --- |
| `CreateUserDTO` | `String name`, `String email` |
| `UserDetailDTO` | `String id`, `String name`, `String email`, `String status`, `List<String> roleCodes` |
| `AssignRoleDTO` | `String userId`, `String roleCode` |
| `GrantPermissionDTO` | `String roleCode`, `String permissionCode` |
| `PermissionTreeDTO` | `String userId`, `List<String> permissionCodes` |
| `CreateGradeDTO` | `String code`, `String name` |
| `GradeDetailDTO` | `String id`, `String code`, `String name`, `String status` |
| `CreateSchoolClassDTO` | `String name`, `String gradeCode` |
| `SchoolClassDetailDTO` | `String id`, `String name`, `String gradeCode`, `String gradeName`, `String status`, `List<String> userIds` |
| `AssignUserToClassDTO` | `String userId`, `String schoolClassId` |

Facade input validation requires nonblank identifiers/codes/names, normalized valid email, names no longer than 120 characters, and new command codes no longer than 64 characters. A `GradeDetailDTO` restored from V1 may expose its preserved legacy code up to 120 characters. Facade calls reject contract or provider failures with `OrganizationFacadeException`, whose stable fields are `code`, `message`, and `traceId`; it does not expose Domain or Infrastructure exception classes.

Application models use the same semantic fields but remain independent types. Mutation Commands add a leading nonblank `requestId`; detail Queries contain only their aggregate identifier; Results contain the fields of the corresponding detail DTO. RabbitMQ supplies the mandatory message `requestId`. HTTP/GraphQL use an optional `Idempotency-Key` header and Dubbo uses an optional `idempotency-key` attachment; an adapter generates a new UUID when that protocol value is absent. Trace IDs are never reused as idempotency IDs.

### HTTP Contract

All paths use the `/api/v1` prefix. Mutation requests may carry `Idempotency-Key`; all requests may carry the actor/trace headers defined below. Successful create responses return the Adapter VO directly with `201 Created` and a `Location` header; lookup responses return the VO directly with `200 OK`; relationship mutations return `204 No Content`.

| Method and path | Adapter request | Success response |
| --- | --- | --- |
| `POST /api/v1/users` | `CreateUserRequest(name,email)` | `201 UserDetailVO` |
| `GET /api/v1/users/{userId}` | none | `200 UserDetailVO` |
| `POST /api/v1/users/{userId}/roles` | `AssignRoleRequest(roleCode)` | `204` |
| `POST /api/v1/roles/{roleCode}/permissions` | `GrantPermissionRequest(permissionCode)` | `204` |
| `GET /api/v1/users/{userId}/permissions` | none | `200 PermissionTreeVO` |
| `POST /api/v1/grades` | `CreateGradeRequest(code,name)` | `201 GradeDetailVO` |
| `GET /api/v1/grades/{gradeId}` | none | `200 GradeDetailVO` |
| `POST /api/v1/school-classes` | `CreateSchoolClassRequest(name,gradeCode)` | `201 SchoolClassDetailVO` |
| `GET /api/v1/school-classes/{schoolClassId}` | none | `200 SchoolClassDetailVO` |
| `POST /api/v1/school-classes/{schoolClassId}/users` | `AssignUserToClassRequest(userId)` | `204` |

The error body and non-success status mapping are exactly those in the Error Handling section. Adapter request and VO classes do not reuse Facade DTOs or Common response wrappers.

### GraphQL Contract

`graphql/schema.graphqls` exposes these exact operation names:

```text
Query.user(id: ID!): User!
Query.permissionTree(userId: ID!): PermissionTree!
Query.grade(id: ID!): Grade!
Query.schoolClass(id: ID!): SchoolClass!

Mutation.createUser(input: CreateUserInput!): User!
Mutation.assignRole(input: AssignRoleInput!): Boolean!
Mutation.grantPermission(input: GrantPermissionInput!): Boolean!
Mutation.createGrade(input: CreateGradeInput!): Grade!
Mutation.createSchoolClass(input: CreateSchoolClassInput!): SchoolClass!
Mutation.assignUserToSchoolClass(input: AssignUserToSchoolClassInput!): Boolean!
```

GraphQL object and input fields mirror the frozen HTTP/Facade semantics. `OrganizationGraphQlExceptionResolver` maps failures into extensions named `code`, `traceId`, `timestamp`, and `fieldErrors`.

### Dubbo Provider Contract

Dubbo exports the five Facade interface FQCNs derived from the generated base package with group `student-management-organization` and service version `1.0.0`. `UserRpcProvider` exports `UserFacade`, `RoleFacade`, and `PermissionFacade`; `SchoolClassRpcProvider` exports `SchoolClassFacade` and `GradeFacade`. Trace and request context use invocation attachment names `x-trace-id`, `x-actor-id`, `x-actor-roles`, and `idempotency-key`. Local/test use the `injvm` protocol, `register=false`, and no registry or network listener. Dev/prod use protocol `dubbo`, may use Nacos-backed registry properties, and default the provider port to `50051`.

### RabbitMQ Contract

The runtime property is `organization.integrations.rabbit.enabled`. The fixed topology is:

| Purpose | Name or routing key |
| --- | --- |
| command exchange | `student.organization.command.v1` |
| event exchange | `student.organization.event.v1` |
| dead-letter exchange | `student.organization.dlx.v1` |
| create-user queue/key | `student.organization.user.create.v1` / `organization.command.user.create.v1` |
| create-school-class queue/key | `student.organization.school-class.create.v1` / `organization.command.teaching.school-class.create.v1` |
| command dead-letter queues | each command queue plus `.dlq` |
| user event keys | `organization.event.user.changed.v1`, `organization.event.user.role-assigned.v1`, `organization.event.user.permission-granted.v1` |
| teaching event keys | `organization.event.teaching.grade.changed.v1`, `organization.event.teaching.school-class.changed.v1`, `organization.event.teaching.membership.changed.v1` |

`CreateUserMessage` fields are `requestId`, `name`, and `email`. `CreateSchoolClassMessage` fields are `requestId`, `name`, and `gradeCode`. `OrganizationEventMessage` fields are `eventId`, `eventType`, `aggregateId`, `occurredAt`, and a JSON object payload. All messages use Jackson JSON; consumers delegate to the same Commands used by the other adapters.

Retryable technical failures use three total delivery attempts with 1-second initial backoff, multiplier 2, and 5-second maximum, then dead-letter. Adapter validation, forbidden context, Domain rejection, and duplicate-command outcomes are acknowledged or rejected without requeue according to their permanent classification; they never enter an endless retry loop.

Outbound event publication uses Rabbit publisher confirms and the same bounded technical retry policy after the database transaction commits. A transactional outbox is outside this example; an exhausted post-commit publish failure is logged and metered without pretending that the already committed database mutation rolled back.

### Redis And Profile Contract

The runtime property is `organization.integrations.redis.enabled`. Keys are UTF-8 strings and values use the configured Jackson JSON serializer:

```text
student-management-organization:user:{userId}
student-management-organization:grade:{gradeId}
student-management-organization:school-class:{schoolClassId}
```

TTL properties and defaults are `organization.cache.user-ttl=PT10M`, `organization.cache.grade-ttl=PT30M`, and `organization.cache.school-class-ttl=PT10M`. Mutations evict the affected aggregate keys only after commit. Local/test set Redis and RabbitMQ integration flags to `false`, use H2 and local fallback beans, disable Nacos discovery/config, and export Dubbo only in-process without an external registry. Dev/prod default these integrations to explicit environment-backed configuration rather than silently using local fallbacks.

Command idempotency uses `student-management-organization:command:{operation}:{requestId}` with `organization.command.idempotency-ttl=PT24H`. Operation values are `create-user`, `assign-role`, `grant-permission`, `create-grade`, `create-school-class`, and `assign-user-to-school-class`. Redis uses an atomic set-if-absent claim; local/test use the deterministic in-memory implementation. A duplicate RabbitMQ command is acknowledged without invoking the Application mutation again; a duplicate HTTP, GraphQL, or Facade mutation maps to `ORG_CONFLICT`. If the claimed mutation rolls back, Application releases the claim; after commit the claim remains until TTL expiry.

The generated local/test profiles fix these effective values:

```properties
spring.datasource.url=jdbc:h2:mem:organization;MODE=PostgreSQL;DB_CLOSE_DELAY=-1
spring.flyway.enabled=true
spring.cloud.nacos.discovery.enabled=false
spring.cloud.nacos.config.enabled=false
dubbo.registry.address=N/A
dubbo.protocol.name=injvm
dubbo.protocol.port=-1
organization.integrations.redis.enabled=false
organization.integrations.rabbit.enabled=false
```

Local/test also exclude Redis and RabbitMQ connection auto-configuration; focused configuration tests instantiate the serializer and Rabbit topology beans directly. Dev/prod require explicit datasource, Redis, RabbitMQ, Nacos, and Dubbo registry environment values whenever the corresponding integration is enabled.

### AOP, Filters, And OpenAPI Contract

`OrganizationInfraLogAspect` uses the pointcut `execution(public * ${package}.application.manage..*(..))`, records trace ID, method, outcome, and elapsed time, and never logs request bodies, email addresses, or exception stack traces at info level. It rethrows the original failure unchanged.

`OrganizationTraceFilter` runs before `OrganizationAuthContextFilter`, accepts or generates `X-Trace-Id`, and returns the value in the response. The auth-context filter reads optional `X-Actor-Id` and comma-separated `X-Actor-Roles` into the Application request context for example authorization checks; it does not authenticate credentials. Both filters clear thread-local state in `finally`.

`OrganizationSwaggerConfig` publishes title `Student Management Organization API`, API version `v1`, JSON at `/v3/api-docs`, and UI at `/swagger-ui.html`. Only `/api/v1/**` HTTP operations are part of the generated business API contract.

## Technology Integration

The generated POM retains the existing Spring Boot, Egon COLA BOM, Nacos, Dubbo, JPA, Flyway, H2, PostgreSQL, MapStruct Plus, Lombok, Actuator, Prometheus, ArchUnit, Maven Wrapper, and layered-jar dependencies.

It adds the confirmed real integrations:

- `spring-boot-starter-graphql`
- `spring-boot-starter-amqp` for RabbitMQ
- `spring-boot-starter-data-redis`
- `spring-boot-starter-aop`
- Springdoc OpenAPI's WebMVC API and UI starters, sharing one explicit version property in the generated root POM

Adapter owns inbound Web, GraphQL, RabbitMQ, Dubbo, validation, conversion, and the Springdoc WebMVC API/annotation dependency. Infrastructure owns JPA, Redis, outbound RabbitMQ, AOP, Flyway, database driver, and persistence conversion dependencies. Starter owns the Springdoc UI dependency and `OrganizationSwaggerConfig`; it depends on Adapter and Infrastructure but does not redeclare their business integration libraries.

The generated module POMs use this fixed direct dependency ownership. Scopes shown below are mandatory; `org.projectlombok:lombok` is allowed only with `provided` scope in Facade, Application, Infrastructure, Adapter, and Starter, and MapStruct Plus may be used only in Application, Infrastructure, and Adapter.

| Module | Required direct production/runtime artifacts |
| --- | --- |
| Common | `top.egon:egon-cola-component-common-core` |
| Facade | `jakarta.validation:jakarta.validation-api` |
| Domain | no external production artifact beyond its generated Common dependency |
| Application | `org.springframework:spring-context`, `org.springframework:spring-tx`, `jakarta.validation:jakarta.validation-api`, `io.github.linpeilie:mapstruct-plus-spring-boot-starter` |
| Infrastructure | `org.springframework.boot:spring-boot-starter-data-jpa`, `org.springframework.boot:spring-boot-starter-data-redis`, `org.springframework.boot:spring-boot-starter-amqp`, `org.springframework.boot:spring-boot-starter-aop`, `org.flywaydb:flyway-core`, `com.h2database:h2` with runtime scope, `org.postgresql:postgresql` with runtime scope, `io.github.linpeilie:mapstruct-plus-spring-boot-starter` |
| Adapter | `org.springframework.boot:spring-boot-starter-web`, `org.springframework.boot:spring-boot-starter-validation`, `org.springframework.boot:spring-boot-starter-graphql`, `org.springframework.boot:spring-boot-starter-amqp`, `org.apache.dubbo:dubbo-spring-boot-starter`, `org.springdoc:springdoc-openapi-starter-webmvc-api`, `io.github.linpeilie:mapstruct-plus-spring-boot-starter` |
| Starter | `org.springframework.boot:spring-boot-starter`, `org.springframework.boot:spring-boot-starter-actuator`, `org.springdoc:springdoc-openapi-starter-webmvc-ui`, `org.springframework.cloud:spring-cloud-starter-bootstrap`, `com.alibaba.cloud:spring-cloud-starter-alibaba-nacos-discovery`, `com.alibaba.cloud:spring-cloud-starter-alibaba-nacos-config`, `io.micrometer:micrometer-registry-prometheus` |

Required direct test artifacts are `org.junit.jupiter:junit-jupiter` for Common, Facade, and Domain; `org.hibernate.validator:hibernate-validator` and `com.fasterxml.jackson.core:jackson-databind` with test scope for Facade contract tests; JUnit Jupiter plus `org.mockito:mockito-junit-jupiter` for Application; `org.springframework.boot:spring-boot-starter-test` for Infrastructure, Adapter, and Starter; `org.springframework.graphql:spring-graphql-test` for Adapter; and `com.tngtech.archunit:archunit-junit5` for Starter.

Direct artifact prohibitions are also fixed:

- No generated module may declare MyBatis, MyBatis-Plus, Mapper XML, or an external evaluation Facade artifact.
- Common, Facade, and Domain declare no Spring, JPA, Redis, AMQP, Dubbo, GraphQL, Springdoc, Flyway, H2, or PostgreSQL artifact.
- Application declares no Web, JPA, Redis, AMQP, Dubbo, GraphQL, Springdoc, Flyway, H2, PostgreSQL, or external Facade artifact.
- Infrastructure declares no Web, GraphQL, Dubbo provider, Springdoc, or external Facade artifact in this deferred-integration scope.
- Adapter declares no JPA, Redis, Flyway, H2, PostgreSQL, or outbound-client artifact.
- Starter declares no direct JPA, Redis, AMQP, Dubbo, Flyway, database-driver, MapStruct, or business-contract artifact beyond its two generated assembly dependencies.

The root POM owns version/BOM management only: Spring Boot, Egon COLA, Spring Cloud, Spring Cloud Alibaba, Dubbo, MapStruct Plus, Lombok, ArchUnit, and one explicit Springdoc version property. `verify.groovy` parses every generated POM and enforces both the internal graph and this external artifact matrix.

## Runtime Profiles And Fallbacks

The integrations are real but do not make local development dependent on external services:

- `local` and `test` use H2.
- Redis beans are property-gated; local/test select an in-memory cache adapter.
- RabbitMQ listeners and publishers are property-gated; local/test select a local event publisher and do not connect to a broker.
- Nacos remains disabled in local/test.
- Dubbo uses `injvm` and no registry or network listener in local/test; provider tests exercise the exported Facade beans in-process.
- GraphQL and HTTP remain available for in-process tests.
- `dev` and `prod` may enable Redis, RabbitMQ, Nacos, a Dubbo registry, and PostgreSQL using environment-backed configuration.

Fallback beans live in Infrastructure and implement the same Domain ports as the real adapters. Conditional configuration must prevent duplicate port implementations and accidental startup connections.

RabbitMQ command and event routing keys are distinct for user and teaching so a published event cannot re-enter its originating command consumer. Retryable failures use listener retries and dead-letter routing; validation and permanent business failures are rejected without endless retries.

Redis keys are namespaced by application and aggregate identifier, use configurable TTLs, and are invalidated only after successful mutation transactions.

## Validation Responsibilities

The four validator categories remain separate:

- Adapter validators enforce protocol shape, required fields, lengths, formats, enums, and pagination values.
- Application validators enforce authorization context, idempotency, workflow prerequisites, and use-case preconditions.
- Domain validators enforce user, role, permission, grade, school-class, status, and relationship invariants.
- Infrastructure validators translate database constraints and validate cache, RabbitMQ, and external technical results.

The same rule must not be copied between layers. A protocol-independent business rule belongs to Domain even if it is first observed through an HTTP request.

In this provider-only scope, “external technical results” means JPA, Redis, RabbitMQ, and runtime configuration results. External Facade response validation is generated only with the deferred outbound client and is not represented by a placeholder validator now.

The sample Application authorization rules are fixed but deliberately not an authentication system: user/role/permission mutations require `ORGANIZATION_ADMIN`; grade, school-class, and membership mutations require `TEACHING_ADMIN`; RabbitMQ commands run with the trusted `SYSTEM` context; detail queries require no role. HTTP/GraphQL read `X-Actor-Id` and `X-Actor-Roles`, Dubbo reads equivalent invocation attachments, and missing/insufficient mutation context maps to `ORG_FORBIDDEN` / HTTP 403. Credential verification and token issuance remain out of scope.

Each HTTP/GraphQL filter chain, Dubbo provider invocation, and RabbitMQ consumer establishes exactly one `OrganizationRequestContext` and clears it in `finally`; context must not leak between virtual-thread or pooled-thread executions.

## Error Handling

HTTP uses one Adapter-owned error response:

```text
String code
String message
String traceId
Instant timestamp
Map<String, List<String>> fieldErrors
```

The stable codes are `ORG_VALIDATION_ERROR`, `ORG_FORBIDDEN`, `ORG_NOT_FOUND`, `ORG_CONFLICT`, `ORG_DOMAIN_REJECTED`, `ORG_DEPENDENCY_UNAVAILABLE`, and `ORG_INTERNAL_ERROR`. Non-field failures use an empty `fieldErrors` map.

Status mapping is:

- `400 Bad Request` for malformed input and Adapter validation failures.
- `403 Forbidden` for missing or insufficient Application authorization context.
- `404 Not Found` for missing users, roles, permissions, grades, or school classes.
- `409 Conflict` for duplicate email, duplicate assignment, and optimistic or unique-key conflicts.
- `422 Unprocessable Entity` for a valid request that violates a Domain invariant.
- `503 Service Unavailable` for an enabled Redis, RabbitMQ, Dubbo, or database integration that is unavailable.
- `500 Internal Server Error` for unexpected failures.

HTTP responses do not expose stack traces. GraphQL exposes equivalent error codes in GraphQL extensions. Dubbo uses Facade-owned response/error contracts. RabbitMQ distinguishes retryable technical failures from permanent validation or business failures.

`503` applies only when an enabled dependency is required before the use case can complete. An exhausted post-commit event-publish attempt is observable through metrics/logging but does not change an already committed HTTP, GraphQL, or Facade success into a failure response.

Infrastructure translates JPA, Redis, and RabbitMQ failures into Domain-facing port failures. Adapter performs the final protocol mapping without importing Infrastructure exception types.

## Persistence And Flyway

The existing `V1__init_student_management_organization.sql` is immutable and must not be edited, renamed, reformatted, or deleted.

Its committed SHA-256 is `c5481736a3ffefc45197a767aec26c1462bb338dfccc1d11751a782ac3de6df1`. Both the archetype verifier and the generated migration contract test must pin this value.

Exactly one new migration, `V2__complete_organization_domains.sql`, completes the schema for both organization domains:

- `roles(id VARCHAR(64), code VARCHAR(64), name VARCHAR(120), status VARCHAR(32), created_at TIMESTAMP)` with primary key and unique `code`.
- `permissions(id VARCHAR(64), code VARCHAR(64), name VARCHAR(120), type VARCHAR(32), status VARCHAR(32), created_at TIMESTAMP)` with primary key and unique `code`.
- `user_roles(id BIGINT GENERATED BY DEFAULT AS IDENTITY, user_id VARCHAR(64), role_id VARCHAR(64), created_at TIMESTAMP)` with primary key, foreign keys, and unique `(user_id, role_id)`.
- `role_permissions(id BIGINT GENERATED BY DEFAULT AS IDENTITY, role_id VARCHAR(64), permission_id VARCHAR(64), created_at TIMESTAMP)` with primary key, foreign keys, and unique `(role_id, permission_id)`.
- `grades(id VARCHAR(160), code VARCHAR(160), name VARCHAR(120), status VARCHAR(32), created_at TIMESTAMP)` with primary key and unique `code`.
- Nullable `school_classes.grade_id VARCHAR(160)` followed by a deterministic backfill, then `NOT NULL` and foreign key `fk_school_classes_grade` to `grades(id)`.
- `school_classes.status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE'`.
- Supporting indexes for foreign-key lookup columns; the existing `uk_school_class_user` remains the membership uniqueness contract.

All newly created business, relationship, status, and timestamp columns are `NOT NULL`; `school_classes.grade_id` is nullable only during the statements that perform its backfill.

Legacy backfill is fixed rather than implementation-defined:

1. For each exact distinct V1 `school_classes.grade_name`, insert one Grade with `id = CONCAT('legacy:', grade_name)`, `code = grade_name`, `name = grade_name`, `status = 'ACTIVE'`, and `created_at = MIN(school_classes.created_at)`.
2. Set each existing `school_classes.grade_id` to `CONCAT('legacy:', grade_name)` and its status to `ACTIVE` before adding the non-null/FK constraints.
3. Retain `school_classes.grade_name` as a non-null display snapshot. New class writes set both `grade_id` and the current Grade name; Grade renames are out of this sample, so snapshot synchronization is not required.
4. Restore legacy Grade aggregates from the generated ID/code/name even when the old name does not satisfy the stricter new-grade code format. `CreateGradeCommand` still limits new codes to 64 characters and the confirmed code pattern.
5. Reserve the `legacy:` Grade-ID prefix for migrated rows. New Grade IDs use `grade-` plus a lowercase UUID and can never collide with a migrated ID.
6. Do not add new foreign keys to the pre-existing `school_class_users` table because V1 permitted historical orphan rows. New assignment writes validate both sides in Application/Domain and retain the existing unique key; the migration never deletes orphan rows.
7. Do not add a database unique constraint on legacy `school_classes(grade_id,name)` because V1 permitted duplicates. The Domain rule and repository existence check reject duplicates for all new writes without making V2 fail on preserved history.
8. Seed one active role `(role-student, STUDENT, Student)` and one active permission `(permission-class-read, CLASS_READ, Read school class, API)` so the generated role-assignment and permission-grant flows are executable without an administrative subsystem.

The migration reuses the existing users, school classes, and school-class membership tables. It must not drop, rename, truncate, delete, or destructively rewrite an existing row. SQL stays within the H2/PostgreSQL subset used by this archetype.

A generated Flyway migration contract test must migrate H2 in PostgreSQL mode to V1, insert representative legacy user, duplicate-named school-class, membership, and orphan-membership rows, migrate the same database to V2, call Flyway `validate`, and prove the exact backfill/read semantics, preserved rows, seeded reference records, and all new-table constraints. Tests use the production migrations directly; they do not create a divergent test-only migration history.

The default external-free validation does not execute a real PostgreSQL server. This is an accepted residual risk for this scope: compatibility is bounded to the tested H2 PostgreSQL mode and deliberately restricted common SQL. A real PostgreSQL migration smoke test belongs to a later opt-in infrastructure-validation profile and must not be represented as already proven by `clean verify`.

## Archetype Metadata And Verification

`archetype-metadata.xml` must include all user and teaching Java packages, `package-info.java` files, GraphQL schemas, runtime configuration, V2 migration, and tests. It must preserve wrapper scripts without filtering them incorrectly.

The basic `verify.groovy` contract must assert:

- Exactly one generated `student-management-organization` Project exists.
- The Project contains exactly seven Maven modules.
- Every module contains all four required main/test Java/resource directories.
- Every generated main and test Java package contains `package-info.java`.
- The fixed required-file manifest exists in full for both `user` and `teaching`, and every fixed forbidden path is absent.
- Each module's internal dependency set matches the architecture contract exactly; key external technology artifacts are required only in their owning layer and forbidden from disallowed layers.
- The complete user and teaching trees exist in the five business-bearing modules, and the required shared Common and Starter trees exist.
- Adapter/Application/Domain/Infrastructure validator packages exist and contain exercised validators.
- Application Command, Query, Result, converter, validator, and assembler packages exist for both domains.
- `adapter.facade.impl.user` and `adapter.facade.impl.teaching` are the only Facade implementation locations; RPC provider classes do not reimplement Facades.
- JPA packages and dependencies exist.
- MyBatis-Plus packages, dependencies, mapper XML, and configuration are absent.
- HTTP, GraphQL, Dubbo, RabbitMQ, Redis, AOP, and Springdoc dependencies, configurations, resources, and user/teaching implementations exist.
- Local and test profiles disable external connections and select the H2, in-memory cache, local publisher, and no-registry Dubbo behavior.
- HTTP error tests assert standard status codes rather than a universal 200 response.
- `exam` and `converter` naming is used; the exact stale tokens `examing` and `convertor` are absent from active generated sources and documentation.
- The V1 SHA-256 matches the pinned value and exactly one `V2__complete_organization_domains.sql` exists.
- The generated README contains the dependency graph, both complete domains, integration ownership, fallback behavior, error semantics, deferred cross-Project boundary, and validation commands.
- The archetype IT goal and generated-project CI execute Maven `verify`, not only `test`.
- Maven Wrapper 3.9.14, Docker, and cross-platform post-generation behavior remain intact.

The verifier may be decomposed into focused helper closures but must remain one generation-time contract and must not introduce a new test framework.

## Test Design

- Common tests cover tracing, time, error, and business-neutral helper behavior.
- Facade contract tests cover user and teaching DTO validation and serialization without loading internal modules.
- Domain tests use JUnit 5 without a Spring context and cover user, role, permission, grade, school-class, both aggregates, membership, and status invariants.
- Application tests use JUnit 5 and Mockito to cover both domains, the cross-domain user/class assignment, transaction boundaries, idempotency, port calls, and result assembly.
- HTTP tests use Spring MVC test support and mocked Application interfaces to cover every controller, uniform error-body field, and the full 400/403/404/409/422/503/500 mapping.
- GraphQL tests use `WebGraphQlTester` without a real server to cover the schema, both domain resolvers, and equivalent error extensions.
- Dubbo configuration tests load the Adapter configuration without a registry, prove that all five Facades are exported through the two RPC provider configurations, and prove that no second Facade implementation exists.
- RabbitMQ tests cover both command consumers, outbound event routing, exchange/queue/binding/dead-letter declarations, retry classification, and permanent failure behavior without a broker.
- Redis adapter tests cover both domains' key namespaces, serializer round trips, configured TTL use, post-commit invalidation, and fallback selection without requiring Redis.
- JPA repository and converter tests use `@DataJpaTest` with H2 for all user, role, permission, grade, school-class, and relationship repositories.
- AOP tests verify interception and error propagation without changing business results.
- Filter tests verify trace and request-context propagation and cleanup.
- Springdoc tests request `/v3/api-docs` in-process and prove that both user and teaching HTTP operations are published.
- Starter assembly tests use `@SpringBootTest` with the test profile and assert that external Redis connections, Rabbit listener containers, Nacos discovery, and external Dubbo registry clients are not created or started.
- `OrganizationFlowTest` uses real HTTP adapters, Application services, Domain services, JPA/H2 repositories, and local fallback ports with no business mocks. With an actor holding both sample admin roles, it creates and queries a user, assigns the V2-seeded `STUDENT` role, grants the V2-seeded `CLASS_READ` permission, creates and queries a grade and school class, assigns the user to that class, and verifies the persisted relationships.
- A rollback scenario forces a mutation failure and proves that neither cache invalidation nor event publication occurs before a successful commit.
- The Starter test scope hosts ArchUnit and imports all production classes from the seven generated modules. It verifies both module directions and responsibility rules, including forbidden technical APIs/annotations in Facade, Domain, and Application and forbidden direct persistence/cache/MQ access in Adapter.
- The Flyway contract test pins V1, exercises V1-to-V2 upgrade and data retention, validates V2 constraints, and calls Flyway `validate`.
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

Also run XML validation for the archetype metadata and generated POMs, `git diff --check`, the V1 checksum assertion, required/forbidden manifest checks, and targeted stale-name searches.

The web archetype IT fixture's `goal.txt` must be `verify`. The generated-project CI workflow must execute `clean verify`, package the generated Project, and build its Docker image on Java 21. It must not start the generated application or require external infrastructure.

## README Requirements

The generated README must document:

- The organization-only generation model.
- The seven Maven modules and exact dependency graph.
- The complete `user` and `teaching` domain verticals and their shared cross-domain assignment flow.
- HTTP, GraphQL, Dubbo, RabbitMQ, Redis, JPA, AOP, and OpenAPI responsibilities.
- The Facade artifact contract for external consumers and the separately deferred outbound cross-Project integration.
- Local/test fallback behavior and dev/prod enablement properties.
- Standard HTTP error semantics.
- Test, package, encryption CLI, Docker build, and optional local run commands.

The README must not claim that evaluation is generated, must not document MyBatis-Plus, and must not contain `examing` or `convertor`.

## Design Pattern Consideration

The design uses patterns already justified by the architecture:

- Facade isolates the publishable cross-Project contract.
- Adapter isolates HTTP, GraphQL, Dubbo, and RabbitMQ entry protocols.
- Application Service owns transactions and use-case orchestration.
- Application Service also owns the explicit cross-domain transaction between `user` and `teaching`; neither aggregate reaches into the other domain's repository.
- Repository isolates Domain persistence contracts from JPA.
- Domain Service and Aggregate concentrate business invariants.
- Ports and Adapters lets Redis, RabbitMQ, and local fallbacks implement Domain-owned contracts without reversing dependencies.
- Observer-style event publication decouples committed user and teaching changes from RabbitMQ delivery.
- Converters and Assemblers separate protocol, use-case, domain, and persistence models.

No persistence Strategy is added because JPA is the only persistence choice. No abstract factory, builder hierarchy, responsibility chain, or decorator layer is introduced because the confirmed organization flows have no variation point that justifies them.

## Risks And Mitigations

- Real integrations can accidentally make local/test require external services. Mitigation: property-gate both bean creation and listeners, supply deterministic Domain-port fallbacks, and prove assembly with tests.
- Either organization domain can be accidentally reduced to marker packages. Mitigation: a fixed required-file manifest and real vertical flow make missing user or teaching implementations fail verification.
- Existing code relies on transitive internal module imports. Mitigation: remove those imports and enforce exact POM plus ArchUnit rules.
- Facade DTOs can leak into Application. Mitigation: Application-owned Commands, Queries, and Results are mandatory.
- The user/class assignment crosses domain packages. Mitigation: Application owns one transaction, each Domain keeps its own invariants, and rollback tests prove atomic persistence and after-commit side effects.
- Redis cache invalidation can race with failed transactions. Mitigation: perform invalidation and event publication only after successful mutation completion.
- RabbitMQ command and event routes can loop. Mitigation: use separate command and event routing namespaces and test them explicitly.
- Direct post-commit publication cannot provide outbox-level durability. Mitigation: require publisher confirms, bounded retry, dead-lettering for commands, and explicit metrics/logging; do not claim transactional event delivery.
- Spring starters can auto-connect unexpectedly. Mitigation: conditional bean/listener configuration plus external-free Starter assembly tests.
- JPA schema extension can damage compatibility. Mitigation: pin V1 by SHA-256, add exactly one backward-safe V2, migrate legacy rows in a contract test, and retain the existing teaching columns and data.
- H2 PostgreSQL mode cannot prove behavior on a real PostgreSQL server. Mitigation: restrict V2 to the tested common subset, state the residual risk explicitly, and reserve a real PostgreSQL smoke test for a later opt-in infrastructure profile.
- The generated sample is substantially larger. Mitigation: the canonical manifest limits the work to types attached to the confirmed organization flows and explicit runtime contracts.
- Architecture examples still name deferred outbound clients. Mitigation: the precedence and manifest exclude only those named clients and point to the separate approval-gated integration design.
- Archetype filtering can corrupt Maven and Spring placeholders. Mitigation: scope filtered file sets and assert the generated values.

## Acceptance Criteria

- One archetype invocation generates one independent `student-management-organization` Project and no evaluation Project.
- The generated Project contains all seven Maven modules.
- The exact internal dependency graph is enforced in POMs, generated source, ArchUnit, and `verify.groovy`.
- Infrastructure depends internally only on Domain.
- Both `user` and `teaching` are complete in Facade, Domain, Application, Infrastructure, and Adapter, use the shared business-neutral Common, and are assembled by Starter; neither is marker-only.
- The fixed required/forbidden manifest, every-package `package-info.java` scan, and four-directory-per-module scan pass.
- Application uses its own Commands, Queries, Results, converters, validators, and assemblers for both domains.
- User creation/lookup, role assignment, permission grant, grade creation/lookup, school-class creation/lookup, user/class assignment, caching, command consumption, and event publication work through shared Application use cases.
- HTTP, GraphQL, Dubbo, RabbitMQ, Redis, JPA, AOP, OpenAPI, filters, converters, and all four validator layers have real tested user and teaching implementations.
- JPA is the only persistence implementation and no MyBatis-Plus artifact remains.
- External consumers can use the standalone organization Facade artifact and Dubbo providers for both domains; outbound organization/evaluation clients remain explicitly deferred.
- HTTP errors use the confirmed status mapping and uniform error body.
- Local/test validation requires no external services.
- The existing Flyway V1 matches the pinned checksum and exactly one V2 migration completes both organization domains while preserving legacy rows.
- The no-mock `OrganizationFlowTest`, rollback test, protocol/configuration contract tests, and seven-module ArchUnit checks pass.
- `exam` and `converter` naming is used consistently in active documentation and generated code.
- The archetype IT uses `verify`; generated-project `clean verify`, package validation, Docker build, XML validation, and CI pass without starting the application.
