# Open-source Archetype Code Style

Updated: 2026-09-22. The native and Open products share the [Java/component/CQE standard](code-style-abstract.md). Apply its class/value-object distinction, scenario-based Builder choice, enum mapping, annotation-driven validation, MP repository contract and soft-delete business uniqueness rules.

## Source ownership

Edit normal projects under `source-projects/egon-cola-source-*-open` and curated packaging documents under `definitions`. `.generated` is derived and ignored; do not edit generated Java or package resources. Regenerate only through the repository generator when packaging needs updated inputs.

## Product boundaries

Light Open is one module with domain-first packages and `start`. Service/Web Open contain seven modules: common, facade, domain, application, infrastructure, adapter, starter. Follow the [source-derived module tables](open-source-archetype-architecture.md), each product's POM and architecture tests.

Domain owns business service ports and value semantics; it does not extend `EgonColaIService` or technical PO-generic CRUD. Infrastructure implements ports and owns sibling `dao`, `po`, `converter`, `repo` packages, explicit Mapper XML, ShardingSphere, cache and outbound clients. Adapter owns HTTP/GraphQL where supported, RPC providers and MQ consumers. Facade owns this product's local Proto contract; consumed peer contracts are separate artifacts, not duplicate local protocol trees.

## Persistence and database lifecycle

Use `egon-cola-component-common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter`, `EgonModel`, `EgonColaRepository` and `EgonColaMapper`; dependency versions come from current POMs. Never restore the retired IService/ServiceImpl persistence baseline or require only @Builder on every PO.

The six Light/Service/Web products initialize and verify schemas through `EgonColaShardingDataSourceBootstrapper`, the Common managed DDL runner, `db/egon-mp` and `repository-manifest.json`. The runner targets physical primaries, initializes empty schemas and validates managed state; a nonempty unmanaged schema fails with REBUILD_REQUIRED. Legacy B/V/manual SQL is an archive, not the runtime entrypoint. `spring.sql.init.mode=never` disables Boot SQL initialization, not this managed runner. Do not describe startup as incapable of DDL.

Business unique keys include business columns plus deleted_at, with explicit active-row NULL enforcement. Existing SQL is not automatically compliant with this new requirement. Corrective schema work is a separate authorized change; applied SQL/history remains immutable; no new Flyway integration is allowed.

## Integration and proof

Retain the selected Open product's Nacos/Dubbo/gRPC and managed component configuration. Do not introduce native RPC/Tianshu solely by copying another product. Check current POMs for exact protocol ownership and versions. Keep one bounded application executor and profile key parity.

Event delivery is CQE: actual Egon Outbox or MQ, with schema/routing, validation, consumer idempotency, confirmation/retry and failure semantics. Direct after-commit MQ has a dual-write window. Local test publishers and in-process protocol tests are not broker delivery proof.

Validate source, packaging and deterministic generation with the repository's applicable commands. Starting services, containers, browsers, publishing and applying production DDL remain user-controlled actions.
