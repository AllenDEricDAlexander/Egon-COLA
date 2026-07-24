#set( $symbol_pound = '#' )
${symbol_pound} ${rootArtifactId}

[English](README.md) | [中文](README.zh-CN.md)

`${rootArtifactId}` is a service-only COLA sample for Course, Schedule, Exam, Paper, and Score workflows. Business traffic enters through Dubbo Triple RPC or RabbitMQ; HTTP is reserved for Spring Boot Actuator management endpoints.

${symbol_pound}${symbol_pound} Module Ownership

- `${rootArtifactId}-common`: stable errors, constants, enums, and identifier utilities.
- `${rootArtifactId}-domain`: entities, aggregates, value objects, domain services, repository/event ports, and the consumer-owned Organization directory port. It contains no persistence, MQ, Facade, or Dubbo implementation.
- `${rootArtifactId}-application`: commands, queries, use-case managers, application validation, and result models.
- `${rootArtifactId}-infrastructure`: Spring Data JPA repositories, Flyway migrations, RabbitMQ/local publisher implementations, and the `top.egon:egon-cola-organization-facade` anti-corruption adapter.
- `${rootArtifactId}-adapter`: Dubbo providers for `top.egon:egon-cola-evaluation-facade`, facade conversion, validation, exception translation, and the score-command MQ consumer.
- `${rootArtifactId}-starter`: Spring Boot assembly, profiles, management configuration, and architecture/context tests.

${symbol_pound}${symbol_pound} Domain-first package layout

Business-owned code puts the domain before the technical responsibility:

```text
domain/exam/entities
application/course/manage
infrastructure/exam/repo
adapter/course/facade/impl
adapter/exam/mq
```

This remains service-only: business traffic enters through Dubbo Triple or RabbitMQ, with no business Controller, Web Filter, GraphQL, or VO package. The external Organization boundary remains at `domain/client/organization` and `infrastructure/client/organization`.

The allowed internal dependency graph is:

```text
Common <- Domain <- Application <- Adapter <- Canonical Evaluation Facade
          Domain <- Infrastructure <- Canonical Organization Facade
          Adapter <- Starter -> Infrastructure
```

More precisely: Domain depends only on Common; Application and Infrastructure depend only on Domain; Adapter depends only on Application. Adapter implements the external Evaluation Facade contract, Infrastructure consumes the external Organization Facade contract, and neither published Facade depends on this generated project. Starter is the composition root, so there is no Web/Service Maven dependency cycle.

${symbol_pound}${symbol_pound} Example Flows

- Course RPC creates a course with a unique normalized code, reads it, pages it, and schedules a class without overlapping time ranges.
- Exam RPC creates an exam for a course, attaches one paper, and publishes the exam only after its paper is ready.
- Score RPC records and queries validated scores. A RabbitMQ score command enters through `RecordScoreConsumer` and delegates to the same Application use case.
- Domain publisher ports describe course scheduling, exam publication, and score recording. Infrastructure supplies local or RabbitMQ implementations.

RabbitMQ support is intentionally basic transport. The sample does not promise retry, dead-letter queue, idempotent inbox, transactional outbox, or delivery guarantees beyond the configured broker behavior.

${symbol_pound}${symbol_pound} Profiles And Integrations

`dev` is the default profile for workstation development and `feature/*` branch verification. It uses the environment-backed PostgreSQL, Nacos, RabbitMQ, and Dubbo integrations.

`test` is selected automatically by Maven tests and is used by the `dev`, `release/*`, and `hotfix/*` validation pipelines. It uses H2 in PostgreSQL compatibility mode, disables RabbitMQ publishers and listeners, and selects a deterministic `OrganizationDirectoryPort` stub, so it requires no Nacos, RabbitMQ, PostgreSQL, or external Dubbo provider.

The Organization Facade client is an unused infrastructure foundation; no current Application use case calls the Organization port.

`prod` is reserved for runtime builds and deployments from `main`. Both `dev` and `prod` select the real Organization Dubbo client, pin `top.egon:egon-cola-organization-facade` through the generated POM, and fail explicitly when the provider is unavailable. Configure them through environment variables rather than committed secrets:

- Database: `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `DB_DRIVER_CLASS_NAME`.
- Nacos: `NACOS_SERVER_ADDR`, `NACOS_NAMESPACE`, `NACOS_GROUP`, `NACOS_USERNAME`, `NACOS_PASSWORD`.
- Dubbo: `DUBBO_REGISTRY_ADDRESS`, `DUBBO_PORT`, `DUBBO_CONSUMER_TIMEOUT`.
- Organization Facade: `ORGANIZATION_FACADE_ENABLED`, `ORGANIZATION_FACADE_GROUP`, `ORGANIZATION_FACADE_SERVICE_VERSION`.
- RabbitMQ: `RABBITMQ_HOST`, `RABBITMQ_PORT`, `RABBITMQ_USERNAME`, `RABBITMQ_PASSWORD`, `RABBITMQ_ENABLED`, `RABBITMQ_LISTENER_AUTO_STARTUP`.
- Configuration decryption: `CONFIG_DECRYPT_KEY` or the documented config-tree secret source.

${symbol_pound}${symbol_pound} Sharding, Read/Write Splitting, And Flyway

Run the starter in default, sharding, or sharding plus read/write mode:

```bash
SPRING_PROFILES_ACTIVE=dev bash ./mvnw -pl ${rootArtifactId}-starter spring-boot:run
SPRING_PROFILES_ACTIVE=dev,sharding bash ./mvnw -pl ${rootArtifactId}-starter spring-boot:run
SPRING_PROFILES_ACTIVE=dev,sharding,readwrite bash ./mvnw -pl ${rootArtifactId}-starter spring-boot:run
```

Default mode uses Spring Boot's single `DataSource` and Flyway lifecycle.
`sharding` migrates physical primaries before creating the ShardingSphere JDBC
logical `DataSource`. `readwrite` must be combined with `sharding`; ordinary
queries use replicas, writes use primaries, and transaction-bound reads are
forced to primaries.

The table topology is:

- SINGLE table on `single`: `course`.
- SHARDING table `course_schedule`, sharded by `course_id`.
- SHARDING binding tables `exam`, `exam_paper`, and `score`. `exam` is sharded by
  `id`; its paper and scores copy that root into `exam_id`, so the exam aggregate
  is colocated in one physical database and table suffix.

Primary-only sharding uses `EVALUATION_SHARDING_SINGLE_URL`,
`EVALUATION_SHARDING_SHARD_0_URL`, `EVALUATION_SHARDING_SHARD_1_URL`,
`EVALUATION_SHARDING_USERNAME`, `EVALUATION_SHARDING_PASSWORD`, and optionally
`EVALUATION_SHARDING_DRIVER_CLASS_NAME`. Read/write mode provides URL, username,
and password triples for `EVALUATION_SINGLE_PRIMARY`,
`EVALUATION_SINGLE_REPLICA_0`, `EVALUATION_SHARD_0_PRIMARY`,
`EVALUATION_SHARD_0_REPLICA_0`, `EVALUATION_SHARD_1_PRIMARY`, and
`EVALUATION_SHARD_1_REPLICA_0`; for example,
`EVALUATION_SHARD_1_PRIMARY_URL`, `EVALUATION_SHARD_1_PRIMARY_USERNAME`, and
`EVALUATION_SHARD_1_PRIMARY_PASSWORD`.

Flyway owns `db/migration/default`, `db/migration/sharding/single`, and
`db/migration/sharding/shard`. In ShardingSphere modes it runs serially against
configured primary targets before logical-data-source startup. A replica must
be a database-level copy of its primary and is never a migration target.

Surrogate keys are generated by the application as UUIDv7 and persisted as
36-character RFC strings. Migration files use
`VyyyyMMdd_NNN__description.sql`, with their creation date and a three-digit
daily sequence. Each SQL file begins with `变更内容`, `影响范围`, and
`兼容性说明` comments. This is a fresh scaffold with no migration history, so
the generated files directly initialize the final model; after a generated
project has applied a migration, add a new version instead of editing it.

Database count, tables per database, and total physical-node count must be powers
of two. The initial append-only map is `2 databases × 2 tables = 4 nodes` with
`mapping-version: 1`. Expand only from `N` to `2N`; doubling both dimensions
requires two expansions. Preserve all old slots, migrate new primaries first,
append slots `N..2N-1`, move and reconcile only records whose new slot is
`oldSlot + N`, and atomically publish the incremented mapping version. This
stable-slot contract limits remapping to approximately half the keys; it does
not include online dual writes, CDC, or automatic data movement.

Only local transactions within one physical database are supported. Exam,
paper, and score changes in one aggregate must use the same `examId`; schedule
changes must retain their `courseId`. Cross-shard workflows use business
idempotency, explicit states, events, reconciliation, and compensation. No XA,
BASE, Seata, or other distributed transaction coordinator is included.

${symbol_pound}${symbol_pound} Verification And Packaging

```bash
SPRING_PROFILES_ACTIVE=test bash ./mvnw -B -ntp clean test
SPRING_PROFILES_ACTIVE=test bash ./mvnw -B -ntp -DskipTests package
```

The test suite includes Domain rules, Application orchestration, JPA adapters,
date-sequence Flyway migration contracts, broker-free MQ adapters, an actual
Dubbo Triple proxy call, external-free Spring context assembly, and architecture
dependency checks. Building the image does not start the service.

${symbol_pound}${symbol_pound} Container Delivery

The generated project uses one source-building `deploy/container/Dockerfile`:

```bash
docker build --build-arg CONTAINER_ENGINE=docker -f deploy/container/Dockerfile -t ${rootArtifactId}:local .
podman build --build-arg CONTAINER_ENGINE=podman -f deploy/container/Dockerfile -t ${rootArtifactId}:local .
nerdctl build --build-arg CONTAINER_ENGINE=nerdctl -f deploy/container/Dockerfile -t ${rootArtifactId}:local .
```

Start the complete Docker development stack with:

```bash
docker compose --env-file deploy/env/.env.example -f deploy/compose/compose.docker.yaml up -d --build
```

Podman and nerdctl use `compose.podman.yaml` and `compose.nerdctl.yaml`. Production
uses the matching `.prod.yaml` file and an operator-owned `.env.prod`. See
`deploy/container/README.md` for rootless prerequisites, persistence, production
boundaries, and data-deletion warnings.

The root `Jenkinsfile` runs tests and can publish immutable images. Set
`PUBLISH_IMAGE=true` plus registry parameters to publish; it never deploys.

${symbol_pound}${symbol_pound} Scope Boundary

This generated service has no business Controller, Web Filter, GraphQL endpoint, native grpc-java module, or enabled H2 console. Its Organization Facade client is intentionally not wired into current Application behavior.
