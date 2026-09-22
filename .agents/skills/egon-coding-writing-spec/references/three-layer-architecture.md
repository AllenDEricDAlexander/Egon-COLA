# Traditional Three-Layer Java Package Design

This reference standardizes the traditional profile only. The skill also permits the exact COLA structure of the repository-selected `egon-cola-archetypes` variant; use `references/java-spring-egon-coding-standards.md` to select the profile. Never mix this `biz.*` tree into an Archetype COLA project.

## Applicability gate

Use this profile when the affected module already follows a traditional three-layer structure or the user explicitly selects it. Do not silently migrate an existing DDD, COLA, hexagonal, or custom architecture into this structure. If the repository is not three-layer and package architecture must change, treat that as a major decision and ask the user.

## Target package tree

```text
<base-package>/biz
├── controller
├── service
│   └── impl
├── repository
├── dao
├── config
├── utils
└── domain
    └── <repository-consistent POJO files or justified role packages>
```

`service.impl` is nested under `service`; never place `impl` beside `service`. `repository` is the anti-corruption layer
between Service and persistence. `dao` holds Mapper interfaces and XML only. `domain` may remain flat or use
repository-consistent role packages such as `po`, `dto`, `vo`, or `query`. Create only packages and classes justified by
the current change; never generate one package per POJO term by default.

## Responsibilities

| Package            | Responsibility                                                                                                                                              | Prohibited responsibility                                                                    |
|--------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------|----------------------------------------------------------------------------------------------|
| `biz.controller`   | HTTP/API entry, parameter binding, transport validation, authentication context, result/error mapping                                                       | Database access, Mapper calls, or business workflow                                          |
| `biz.service`      | Stable business-use interface consumed by controllers and other callers; domain types only, no PO generics                                                  | Framework-specific controller, repository, or Mapper implementation details                  |
| `biz.service.impl` | Service implementation, business rules, orchestration, transaction boundaries, and composition of repositories/collaborators                                | Direct Mapper/DAO use, PO leakage, or an inheritance hierarchy created only for helper reuse |
| `biz.repository`   | Anti-corruption layer: translate Service Query/Command/BO/DTO/`*Result` to `EgonModel` PO, own guarded persistence commands, and expose named query methods | Business workflow, HTTP mapping, or leaking Mapper/XML/PO to Service                         |
| `biz.dao`          | MyBatis `*Mapper` interfaces and XML; SQL only                                                                                                              | Business decisions, ACL translation, or transport result construction                        |
| `biz.config`       | Module configuration, bean assembly, properties, serializers, and technical wiring                                                                          | Business workflow                                                                            |
| `biz.utils`        | Small stateless, business-neutral utilities that cannot live in an existing shared utility                                                                  | Stateful orchestration or a dumping ground for business rules                                |
| `biz.domain`       | POJO data carriers classified by role                                                                                                                       | Assuming DDD aggregates, domain services, or value objects                                   |

## Dependency direction

```text
controller -> service
service.impl -> service, repository, domain
repository -> dao, domain
dao -> domain
controller -> domain
config -> module wiring
utils -> no reverse dependency on controller/service/repository/dao workflows
```

- Controllers depend on the service interface, not on `service.impl`, repository, or DAO/Mapper.
- Controllers may reference only the query, command, view, and `*Result` objects required by their contracts; they must
  not expose a PO/ORM Entity.
- A class in `service.impl` implements the corresponding service interface, owns the normal transaction boundary, and
  depends on repository types that do not carry PO generics.
- Repository never calls controller or service and never decides business policy.
- DAO/Mapper never calls controller, service, or repository and never decides business policy.
- Configuration may assemble implementations but must not become a service locator used by business code.
- Utilities remain stateless and cohesive; prefer an existing project utility before creating another one.

## Repository anti-corruption and egon-mp-ext

Use the current `egon-cola-component-common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter` repository contract. Do
not invent a local CRUD helper, Query Wrapper facade, or `*Gateway` persistence adapter.

- The Service-facing type is a named `*Repository` interface in `biz.repository` (or beside it when the repository
  already splits api/impl). Methods take and return domain Query/Command/BO/DTO/`*Result` types, not `EgonModel` PO.
- The implementation extends `EgonColaRepository<M extends EgonColaMapper<T>, T extends EgonModel<T>>`, uses qualified
  Lombok constructor injection, and supplies mapper / `modelValidationUtils` / `tenantIdProvider` / properties getters.
- Commands use `save`, versioned `updateById` / `removeById`, and guarded batches; callers check affected rows. Zero
  affected rows is not success.
- Queries use named Mapper XML. Every mapper supplies `selectActiveById`, `selectActiveByIds`, and
  `deleteVersionedById`. ActiveRecord, QueryChain, `lambdaQuery`, generic Query Wrapper, and `.last()` SQL are
  forbidden.
- Custom UPDATE binds `#{MP_OPTLOCK_VERSION_ORIGINAL}` with tenant, active row, and expected business state. Preserve
  id, tenant, creation metadata, and version from the caller or a row loaded in the same transaction.
- PO stays behind repository. MapStruct/`BaseConverter` owns PO ↔ domain conversion inside the repository
  implementation.

## Object placement

Apply `references/pojo-modeling.md` before adding objects under `biz.domain`.

- Use `po`, `entity`, or the repository's existing persistence term; do not create synonymous persistence models without a real boundary.
- Use DTO, VO, BO, Query, Command, Event, Result, PageQuery, or PageResult only when its distinct semantics justify a
  class. Do not introduce ambiguous `Data`, `Info`, `Param`, or `Bean` carriers.
- Inbound parameters have no extra `*Request` suffix. Outbound protocol objects use `*Result` (and `*VO` when
  presentation-shaped), not `*Response`.
- DAO/Mapper and `*Repository` are access/ACL components and never belong in the POJO inventory.
- This profile uses the MP-ext `*Repository` anti-corruption class. Do not introduce Aggregate, Domain Service, DDD
  Repository Port, or DDD Value Object concepts.

## Required Spec evidence

The Spec must show the current and target trees, exact files/symbols, package responsibilities, dependency direction,
service interface-to-implementation mapping, controller consumers, repository ACL methods, Mapper/DAO SQL paths,
transaction ownership, POJO roles, and tests. Any deviation from this tree must cite existing repository evidence or an
explicit user decision.
