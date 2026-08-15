# Traditional Three-Layer Java Package Design

This is the only Java package-design profile currently standardized by this skill. DDD and COLA profiles are intentionally deferred.

## Applicability gate

Use this profile when the affected module already follows a traditional three-layer structure or the user explicitly selects it. Do not silently migrate an existing DDD, COLA, hexagonal, or custom architecture into this structure. If the repository is not three-layer and package architecture must change, treat that as a major decision and ask the user.

## Target package tree

```text
<base-package>/biz
├── controller
├── service
│   └── impl
├── dao
├── config
├── utils
└── domain
    └── <repository-consistent POJO files or justified role packages>
```

`service.impl` is nested under `service`; never place `impl` beside `service`. `domain` may remain flat or use repository-consistent role packages such as `po`, `dto`, `vo`, or `query`. Create only packages and classes justified by the current change; never generate one package per POJO term by default.

## Responsibilities

| Package | Responsibility | Prohibited responsibility |
| --- | --- | --- |
| `biz.controller` | HTTP/API entry, request binding, transport validation, authentication context, response/error mapping | Database access or business workflow |
| `biz.service` | Stable business-use interface consumed by controllers and other callers | Framework-specific controller or DAO implementation details |
| `biz.service.impl` | Service implementation, business rules, orchestration, transaction boundaries, and composition of DAOs/collaborators | Inheritance hierarchy created only for helper reuse |
| `biz.dao` | Database access component, mapper/repository queries, and persistence operations | Business decisions or transport response construction |
| `biz.config` | Module configuration, bean assembly, properties, serializers, and technical wiring | Business workflow |
| `biz.utils` | Small stateless, business-neutral utilities that cannot live in an existing shared utility | Stateful orchestration or a dumping ground for business rules |
| `biz.domain` | POJO data carriers classified by role | Assuming DDD aggregates, domain services, or value objects |

## Dependency direction

```text
controller -> service
service.impl -> service, dao, domain
dao -> domain
controller -> domain
config -> module wiring
utils -> no reverse dependency on controller/service/dao workflows
```

- Controllers depend on the service interface, not on `service.impl` or DAO.
- Controllers may reference only the transport, query, and view objects required by their contracts; they must not expose a PO/ORM Entity.
- A class in `service.impl` implements the corresponding service interface and owns the normal transaction boundary.
- DAO never calls controller or service and never decides business policy.
- Configuration may assemble implementations but must not become a service locator used by business code.
- Utilities remain stateless and cohesive; prefer an existing project utility before creating another one.

## Object placement

Apply `pojo-modeling.md` before adding objects under `biz.domain`.

- Use `po`, `entity`, or the repository's existing persistence term; do not create synonymous persistence models without a real boundary.
- Use DTO, VO, BO, Query, Command, Request, Response, Form, Param, PageQuery, or PageResult only when its distinct semantics justify a class.
- Request/Response may remain under `biz.domain` for this profile unless the repository consistently places them below `controller`.
- DAO is an access component and never belongs in the POJO inventory.
- Do not introduce Aggregate, Domain Service, Repository Port, or DDD Value Object concepts in this profile.

## Required Spec evidence

The Spec must show the current and target trees, exact files/symbols, package responsibilities, dependency direction, service interface-to-implementation mapping, controller consumers, DAO access paths, transaction ownership, POJO roles, and tests. Any deviation from this tree must cite existing repository evidence or an explicit user decision.
