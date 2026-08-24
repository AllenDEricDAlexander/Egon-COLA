# Open-source Archetype Code Style

This guide is for contributors extending the `-open` templates and for teams
reviewing a generated project. It is a maintenance guide, not a replacement for
the approved family specification or the generated project's product README.

## Package and module ownership

Use the generated package root (`<your.package>`) and keep the domain-first
shape. The integration fixtures use `it.pkg`; that name is only a test package
example.

| Area | Owns | Must not depend on |
|---|---|---|
| `common` | shared value objects, ID-facing helpers, error/result contracts | Adapter, Infrastructure, Spring web types |
| `facade` | local Proto source and generated contract types | Domain, Application, Infrastructure, Adapter |
| `domain` | aggregates, domain services, repository ports, business invariants | Spring Boot, MyBatis-Plus, Dubbo, gRPC, HTTP |
| `application` | use cases, transaction boundaries, input validation, orchestration | Mapper XML, generated transport stubs, controllers |
| `infrastructure` | MyBatis-Plus adapters, ShardingSphere topology, MQ, Redis, RPC/gRPC clients | HTTP controller concerns and domain rule duplication |
| `adapter` | HTTP/GraphQL endpoints, MQ consumers, Dubbo providers, DTO/VO mapping | direct SQL and persistence implementation |
| `starter` | Spring Boot composition, Nacos/bootstrap, DTP, observability, runtime tests | business rules and a second application executor |

Light is a single Maven project with these responsibilities under packages;
Service and Web use the seven-module layout `common`, `facade`, `domain`,
`application`, `infrastructure`, `adapter`, and `starter`.

The dependency direction is intentionally boring:

```text
adapter -> application -> domain
adapter -> facade
infrastructure -> domain and facade
starter -> adapter and infrastructure
facade -> (nothing in the business project)
```

Do not introduce a new layer, reverse a dependency, or make `facade` a service
locator to solve a local convenience problem. If a boundary genuinely needs to
change, update the specification, ArchUnit rules, generated verifier, and the
three affected README/runbooks together.

## ID and boundary mappings

The Common ID starter is the only ID generator in an Open project. Use `Long`
for domain IDs, persistence IDs, relation keys, and ShardingSphere route keys.
Configure a unique `EGON_ID_MACHINE_ID`; `0` is reserved for deterministic test
contexts. Do not derive a machine ID from host metadata or generate UUIDs.

At protocol boundaries use the type that matches the consumer:

| Boundary | Representation | Rule |
|---|---|---|
| Domain / Application / MyBatis-Plus | `Long` | no string parsing in the core model |
| PostgreSQL / ShardingSphere | `BIGINT` | indexes and unique keys use the same key type |
| Proto / Dubbo Triple / standard gRPC | `int64` | preserve field numbers and reserve removed fields |
| HTTP / GraphQL request and response | decimal `String` | protect JavaScript clients from 64-bit precision loss |
| MQ payload | contract-defined `int64` or decimal string | keep producer/consumer fixtures in sync |

Use explicit DTO/VO mapping in Adapter. A controller must not expose a domain
aggregate or a generated Proto message directly when the HTTP contract has a
different representation.

## Persistence and mapper safety

Use the official MyBatis-Plus Spring Boot 3 starter. Repository ports remain in
Domain; Infrastructure implements them with Mapper interfaces and XML under the
generated `resources/mapper` directory. Keep SQL explicit and readable:

- bind values with MyBatis parameters; never concatenate user input or table
  names;
- keep mapper IDs, result mappings, and PO/record field types aligned;
- make tenant, relation, and route-key predicates visible in the SQL;
- use ShardingSphere route keys in the method contract so the router can choose a
  physical node;
- test mapper behavior against the disposable schema helper and add a focused
  manual-SQL convention test when a new table is introduced.

Do not add Spring Data JPA, `JpaRepository`, `jakarta.persistence`, Hibernate
repositories, dynamic table-name substitution, or a second persistence API.

## Manual SQL and schema review

Open templates do not use Flyway, Liquibase, `schema.sql`, `data.sql`, or an
automatic schema updater. Every schema change is a reviewed, ordered SQL file in
the generated project's manual database directory with a README that states:

1. backup and maintenance-window prerequisites;
2. physical master/shard execution order;
3. expected tables, indexes, key types, and verification queries;
4. rollback/forward-fix ownership for the DBA.

The application profile must keep `spring.sql.init.mode=never`. Tests may parse or
execute the SQL in an isolated H2-compatible fixture where supported, but the
build must never connect to a user's PostgreSQL instance and the generated
application must never execute production DDL.

## Proto and RPC evolution

`facade/src/main/proto` is the single source for generated transport code. Service
and Web local Proto directories must remain byte-identical. Additive evolution is
preferred: reserve removed field numbers/names, do not reuse tags, and update
descriptor contract tests with every method or message change.

Dubbo provider implementations belong in Adapter and use the generated Triple
interfaces. Standard gRPC clients belong in Infrastructure. Keep protocol
conversion and retry/deadline policy at those edges; Domain and Application
should depend on a port or a business result, not on a transport channel.

Service intentionally has no HTTP controller or Springdoc surface. Web and Light
may expose HTTP/GraphQL APIs and therefore use Springdoc; the external Spring
Cloud Gateway is deployed/configured outside the generated project.

## Async and Dynamic Thread Pool

Define one bounded `applicationTaskExecutor` through the Spring Boot builder and
install the Common/DTP task decorator on that executor. The starter may register
the executor with the Dynamic Thread Pool component, but it must not create a
second executor for `@Async` or silently replace the configured bounds.

Keep core size, maximum size, queue capacity, keep-alive, graceful shutdown, and
reporting settings explicit in YAML. Redis-backed registration/reporting is an
external runtime concern; tests disable it or use a local context. New async work
must have a test for executor selection and trace/context propagation.

## Tests and enforcement

Use the smallest proof that covers the change, then run the generated verifier:

| Change | Required proof |
|---|---|
| package/dependency direction | `OpenArchitectureTest` and generated verifier |
| ID/route/schema type | Long ID, algorithm, mapper, and manual SQL tests |
| Proto method/message | descriptor contract plus provider/client contract tests |
| DTP/executor/configuration | context test proving one executor and explicit bounds |
| Web API mapping | controller/GraphQL contract and Springdoc OpenAPI test |
| release shape | archetype reactor `clean verify` and CI matrix |

The verifier also scans generated runtime source and POMs for forbidden tokens:
JPA, Flyway/Liquibase, UUID generators, embedded Gateway, and internal bytecode
architecture plugins. README and runbook mentions of a forbidden practice are
documentation, not runtime dependencies; keep the verifier's scope limited to
generated runtime files.

## Forbidden shortcuts

- Do not copy the original family into an Open template after Step 1; the Open
  family must stay independently consumable from public Spring artifacts.
- Do not add `spring-cloud-starter-gateway` or Gateway route configuration to a
  generated project.
- Do not use UUIDs where the Open contract requires Common Snowflake `Long` IDs.
- Do not add Spring Data JPA, Flyway, Liquibase, automatic table creation, or a
  hidden migration runner.
- Do not place database access in Domain/Application or business rules in
  Infrastructure/Adapter.
- Do not add a new executor when the bounded DTP-managed application executor
  already exists.
- Do not make live Nacos, Redis, PostgreSQL, broker, or Gateway availability a
  prerequisite for unit/generated-project verification.

Any exception needs an updated specification and a new plan step; it must not be
smuggled in as a dependency-only change.
