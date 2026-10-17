# Egon-COLA Archetype Architecture

Updated: 2026-09-22. This overview is grounded in normal source-project POMs and Java, with Native and Open as the named variants. Exact dependency versions are owned by the root POM, Components BOM and archetypes parent; resolve those files rather than copying old version tables.

## Source of truth

- `source-projects/egon-cola-source-*`: normal Java/resources/tests and module POMs.
- `definitions/egon-cola-archetype-*`: packaging descriptors, `verify.groovy` integration tests and curated `architecture-docs/architecture.md` (read by the tests, not shipped to generated projects).
- `.generated`: derived ignored packaging inputs; never hand-edit them.
- The [coding standard](coding-standard.md) specifies target Java/MP/CQE rules. Structure follows current code; any older source behavior conflicting with the newly requested standard is an explicit migration gap, not proof of compliance.

## Product selection

| Product | Shape | Integration boundary |
| --- | --- | --- |
| light / light-open | One Maven module; domain-first packages; `start` | HTTP/GraphQL/MQ as declared in the selected source; no extra facade module |
| service / service-open | Seven modules, including own facade | Evaluation RPC/MQ; no business HTTP Controller |
| web / web-open | Seven modules, including own facade | Organization HTTP/GraphQL/RPC/MQ and Evaluation client |
| agent | Six modules, no facade | Deep Research SSE and Knowledge APIs; Agent Flow/RAG/outbox |

Native uses its Egon platform integrations; Open retains its public-stack integrations. Agent is its own source profile. Do not inject protocol dependencies into another product just because they exist in the BOM.

## Actual direct module dependencies

Arrows below mean consumer depends on provider. These are direct, non-test sibling Maven edges; external component and peer-facade dependencies remain in each POM.

### agent

Source: [egon-cola-source-agent](../source-projects/egon-cola-source-agent/pom.xml).

| Module | Direct sibling dependencies |
| --- | --- |
| common | None |
| domain | common |
| application | domain |
| infrastructure | domain |
| adapter | application |
| starter | adapter, infrastructure |

### light

Source: [egon-cola-source-light](../source-projects/egon-cola-source-light/pom.xml).

Single Maven module; package direction is enforced by its architecture configuration/tests.

### light-open

Source: [egon-cola-source-light-open](../source-projects/egon-cola-source-light-open/pom.xml).

Single Maven module; package direction is enforced by its architecture configuration/tests.

### service

Source: [egon-cola-source-service](../source-projects/egon-cola-source-service/pom.xml).

| Module | Direct sibling dependencies |
| --- | --- |
| common | None |
| facade | None |
| domain | common |
| application | domain |
| infrastructure | domain |
| adapter | application, facade |
| starter | adapter, infrastructure |

### service-open

Source: [egon-cola-source-service-open](../source-projects/egon-cola-source-service-open/pom.xml).

| Module | Direct sibling dependencies |
| --- | --- |
| common | None |
| facade | None |
| domain | common |
| application | domain |
| infrastructure | domain, facade |
| adapter | application, facade |
| starter | adapter, infrastructure |

### web

Source: [egon-cola-source-web](../source-projects/egon-cola-source-web/pom.xml).

| Module | Direct sibling dependencies |
| --- | --- |
| common | None |
| facade | None |
| domain | common |
| application | domain |
| infrastructure | domain |
| adapter | application, facade |
| starter | adapter, infrastructure |

### web-open

Source: [egon-cola-source-web-open](../source-projects/egon-cola-source-web-open/pom.xml).

| Module | Direct sibling dependencies |
| --- | --- |
| common | None |
| domain | common |
| application | domain |
| facade | None |
| infrastructure | domain, facade |
| adapter | application, facade |
| starter | adapter, infrastructure |

## Responsibilities

Domain owns business entities/value objects, rules and service ports. Application owns use-case coordination and transactions. Infrastructure implements Domain ports and owns persistence adapters, external clients, cache and MQ. Adapter owns transport binding, validation, mapping and providers/consumers. Starter/start owns composition. Common carries shared contracts; facade is this product's protocol leaf, not a business service locator.

Use domain-first placement such as `application/user/pojo/command`, `domain/teaching/service`, `infrastructure/user/{dao,po,converter,repo}`, `adapter/user/facade/impl`. Root-level shared concerns and explicit cross-project client packages follow current source. Do not introduce a generic Gateway layer or technical CRUD domain service.

The three-layer skill profile stays unchanged; this DDD map is not a migration instruction for existing `biz.*` projects.

## Persistence, schema and events

The six Light/Service/Web products use the Common MP repository starter, nullable deletedAt/version, explicit Mapper XML, Common ID and managed PostgreSQL DDL through EgonColaShardingDataSourceBootstrapper. `db/egon-mp` and repository-manifest.json are current runtime schema inputs; legacy manual/B/V SQL is archival. The required DDL standard also applies to Agent. Its remaining Flyway dependency/configuration is legacy source drift to be migrated separately; do not document it as an allowed alternative. Vector-store ownership must be reconciled in that implementation change.

CQE means Command, Query, Event. Event uses Egon transactional outbox or actual MQ. Source Web's after-commit Rabbit publication logs/counts failure without an atomic outbox guarantee; Agent Knowledge uses the outbox. See the coding standard for the required transaction, retry and idempotency design and known annotation/uniqueness gaps.

## Verification and runtime ownership

Use source module architecture tests/plugin configuration and packaging verifier checks for the selected variant. No universal graph overrides an observed product POM. Source tests, H2 fixtures and in-process RPC checks do not prove live PostgreSQL replication/routing, Nacos/Tianshu registration, broker delivery, remote RPC/TLS or container deployment. Generator checks prove deterministic packaging, not runtime acceptance.

## Distributed DDL management

The managed DDL rules are specified once, in the [coding standard](coding-standard.md#分布式-ddl-管理约束): the `egon-mp-sdj-ext-starter` runner, explicit physical targets, versioned SQL with a SHA-256 manifest, no Flyway, and no rewriting of applied SQL or history. They apply to all six Light/Service/Web products and to Agent; Agent's remaining Flyway configuration is the migration gap described above, not an allowed alternative.
