# egon-cola-source-service

[English](README.md) | [中文](README.zh-CN.md)

`egon-cola-source-service` is a service-only COLA sample for Course, Schedule, Exam, Paper, and Score workflows. Business traffic enters through COLA native unary RPC RPC or RabbitMQ; HTTP is reserved for Spring Boot Actuator management endpoints.

## Maven Profiles And External Launch Arguments

Use one environment profile at a time: `dev`, `test`, or `prod`; omitted profiles use
`dev` defaults. These profiles configure application startup, while Surefire keeps
its existing `test` profile. JVM heap sizes are examples to tune for the deployment.

From the project root (install sibling modules first for a multi-module project):

```bash
mvn install
mvn -pl egon-cola-source-service-starter -Pdev spring-boot:run
mvn -Pprod -Drun.jvm-args="-Xms1g -Xmx2g" \
  -Drun.server-port=8081 \
  -Drun.config-location=file:/etc/myapp/override.yml package
```

Maven generates `egon-cola-source-service-starter/target/launch.args` during `process-resources`, including when
packaging. Switching profiles or `-Drun.*` values rewrites it even without `clean`.
Only `src/main/launch/launch.args` is filtered by this execution using `@...@`;
existing YAML placeholders remain runtime values. The argument file stays outside the JAR.

Deploy the executable JAR and `launch.args` together, optionally renaming the JAR to
`app.jar`, then launch from the deployment directory:

```bash
java @launch.args -jar app.jar
java @launch.args -Xmx3g -jar app.jar --server.port=9081
```

The file records JVM arguments, the Spring profile, port, and additional configuration
location. These explicit system properties take precedence over corresponding environment
variables; `-Drun.*` overrides Maven defaults, JVM options go before `-jar`, and Spring
command-line overrides go after the JAR. Plain `java -jar` and IDE main-class runs do not
read this file automatically. Relative configuration paths use the launch working directory,
not the argument file directory; use an absolute `file:` path for deployment and forward
slashes for Windows paths. Extra configuration supplements `application.yml` and the
selected `application-{profile}.yml`; omit `optional:` when the file must exist. Keep
credentials in the existing environment/secrets mechanism, never in `run.*` properties.

## Module Ownership

- `egon-cola-source-service-common`: stable errors, constants, enums, and identifier utilities.
- `egon-cola-source-service-domain`: entities, aggregates, value objects, generic `EgonColaIService` contracts, event ports, and the consumer-owned Organization directory port. It depends on the Common MyBatis-Plus starter only for the shared service/model contract; it contains no DAO, PO, persistence, MQ, Facade, or COLA RPC implementation.
- `egon-cola-source-service-application`: commands, queries, use-case managers, application validation, and result models.
- `egon-cola-source-service-infrastructure`: MyBatis-Plus `*PO`/`*DAO` persistence, generic domain-service implementations, Flyway migrations, RabbitMQ/local publisher implementations, and the `top.egon:egon-cola-organization-facade` anti-corruption adapter.
- `egon-cola-source-service-adapter`: COLA RPC providers for `top.egon:egon-cola-evaluation-facade`, facade conversion, validation, exception translation, and the score-command MQ consumer.
- `egon-cola-source-service-starter`: Spring Boot assembly, profiles, management configuration, and architecture/context tests.

## Domain-first package layout

Business-owned code puts the domain before the technical responsibility:

```text
domain/exam/entities
application/course/manage
infrastructure/exam/repo/dao
infrastructure/exam/service/impl
adapter/course/facade/impl
adapter/exam/mq
```

This remains service-only: business traffic enters through COLA native unary RPC or RabbitMQ, with no business Controller, Web Filter, GraphQL, or VO package. The external Organization boundary remains at `domain/client/organization` and `infrastructure/client/organization`.

The allowed internal dependency graph is:

```text
Common <- Domain <- Application <- Adapter <- Canonical Evaluation Facade
          Domain <- Infrastructure <- Canonical Organization Facade
          Adapter <- Starter -> Infrastructure
```

More precisely: Domain depends only on Common; Application and Infrastructure depend only on Domain; Adapter depends only on Application. Adapter implements the external Evaluation Facade contract, Infrastructure consumes the external Organization Facade contract, and neither published Facade depends on this generated project. Starter is the composition root, so there is no Web/Service Maven dependency cycle.

## Example Flows

- Course RPC creates a course with a unique normalized code, reads it, pages it, and schedules a class without overlapping time ranges.
- Exam RPC creates an exam for a course, attaches one paper, and publishes the exam only after its paper is ready.
- Score RPC records and queries validated scores. A RabbitMQ score command enters through `RecordScoreConsumer` and delegates to the same Application use case.
- Domain publisher ports describe course scheduling, exam publication, and score recording. Infrastructure supplies local or RabbitMQ implementations.

RabbitMQ support is intentionally basic transport. The sample does not promise retry, dead-letter queue, idempotent inbox, transactional outbox, or delivery guarantees beyond the configured broker behavior.

## Profiles And Integrations

`dev` is the default profile for workstation development and `feature/*` branch verification. It uses the environment-backed PostgreSQL, RabbitMQ, and COLA RPC integrations.

`test` is selected automatically by Maven tests and is used by the `dev`, `release/*`, and `hotfix/*` validation pipelines. It uses H2 in PostgreSQL compatibility mode, disables RabbitMQ publishers and listeners, and selects a deterministic `OrganizationDirectoryPort` stub, so it requires no RabbitMQ, PostgreSQL, or external COLA RPC provider.

The Organization Facade client is an unused infrastructure foundation; no current Application use case calls the Organization port.

`prod` is reserved for runtime builds and deployments from `main`. Both `dev` and `prod` select the real Organization COLA RPC client, pin `top.egon:egon-cola-organization-facade` through the generated POM, and fail explicitly when the provider is unavailable. Configure them through environment variables rather than committed secrets:

- Database: configure the `master_data`, `shard_0`, and `shard_1` physical data sources described below.
- Tianshu: configure `TIANSHU_RPC_TARGET`, `TIANSHU_NAMESPACE`, separate runtime/registry HMAC credentials and Tianquan-Shoubing SERVICE tokens. `TIANSHU_ENABLED` and `TIANSHU_REGISTRY_ENABLED` control configuration and registration. See the native RPC/Tianshu section below for connection settings.
- COLA RPC: `TIANSHU_RPC_TARGET`, `RPC_PORT`, `ORGANIZATION_FACADE_TIMEOUT_MS`.
- Organization Facade: `ORGANIZATION_FACADE_ENABLED`, `ORGANIZATION_FACADE_GROUP`, `ORGANIZATION_FACADE_SERVICE_VERSION`.
- RabbitMQ: connection settings bind through Spring's own names — `SPRING_RABBITMQ_HOST`, `SPRING_RABBITMQ_PORT`, `SPRING_RABBITMQ_USERNAME`, `SPRING_RABBITMQ_PASSWORD` — while `RABBITMQ_ENABLED` and `RABBITMQ_LISTENER_AUTO_STARTUP` are this application's own switches.
- Configuration decryption: `EGON_CONFIG_DECRYPT_KEY`, `EGON_CONFIG_DECRYPT_KEY_FILE`, or the documented config-tree secret source.

## Sharding, Read/Write Splitting, And Flyway

The generated application always uses a ShardingSphere logical data source and
supports two routing modes:

```bash
SPRING_PROFILES_ACTIVE=dev APP_DATASOURCE_MODE=SHARDING bash ./mvnw -pl egon-cola-source-service-starter spring-boot:run
SPRING_PROFILES_ACTIVE=dev APP_DATASOURCE_MODE=SHARDING_READWRITE bash ./mvnw -pl egon-cola-source-service-starter spring-boot:run
```

Environment profiles are limited to `dev`, `test`, and `prod`.
`APP_DATASOURCE_MODE` accepts `SHARDING` (the default) or
`SHARDING_READWRITE`. Both modes migrate each configured physical primary
before creating the logical `DataSource`; replicas and the logical data source
are never Flyway targets. Read/write mode sends ordinary reads to replicas,
writes to primaries, and transaction-bound reads to primaries. Bundled Compose
does not emulate replicas and defaults to `SHARDING`.

The table topology is:

- Master table `evaluation_course` stays on `master_data` through explicit
  `databaseStrategy.none` and `tableStrategy.none` rules inside
  `!SHARDING.tables`. Neither `!SINGLE` nor an application-wide single data
  source mode is used.
- `evaluation_course_schedule`, `evaluation_exam`, `evaluation_exam_paper`, and
  `evaluation_score` are sharded by positive `tenant_id` for both database and
  table selection. One tenant therefore uses one stable physical database/table
  slot for the whole evaluation family.
- All four sharded tables enable `DML_SHARDING_CONDITIONS`; DML without a
  sharding condition is rejected and `allowHintDisable=false` prevents bypass.

Primary-only sharding uses `EVALUATION_SHARDING_MASTER_DATA_URL`,
`EVALUATION_SHARDING_SHARD_0_URL`, `EVALUATION_SHARDING_SHARD_1_URL`,
`EVALUATION_SHARDING_USERNAME`, `EVALUATION_SHARDING_PASSWORD`, and optionally
`EVALUATION_SHARDING_DRIVER_CLASS_NAME`. Read/write splitting uses URL,
username, and password triples for `EVALUATION_MASTER_DATA_PRIMARY`,
`EVALUATION_MASTER_DATA_REPLICA_0`, `EVALUATION_SHARD_0_PRIMARY`,
`EVALUATION_SHARD_0_REPLICA_0`, `EVALUATION_SHARD_1_PRIMARY`, and
`EVALUATION_SHARD_1_REPLICA_0`.

Flyway uses only `db/migration/sharding/master-data` and
`db/migration/sharding/shard`. It runs serially against physical primaries
before the logical data source is created. Spring Boot Flyway auto-configuration
is excluded, so replicas and the logical data source are never migrated.
`FLYWAY_ENABLED=false` skips physical migrations.

Application-generated surrogate keys use positive `Long` values supplied by the
Common ID starter. Migration files follow `VyyyyMMdd_NNN__description.sql` and begin with
`变更内容`, `影响范围`, and `兼容性说明` comments.

Database count, table count per database, and total physical-node count must all
be powers of two. The initial map is `2 databases × 2 tables = 4 nodes`, held in
`EVALUATION_SHARDING_NODE_COUNT` (default `4`) and `EVALUATION_SHARDING_NODE_MAP`
(default `0=shard_0:0,1=shard_0:1,2=shard_1:0,3=shard_1:1`);
`EVALUATION_SHARDING_DATABASE_NAME` names the logical database.
Capacity follows the 2N rule: change one dimension from `N` to `2N` at a time
and publish the complete `node-count` and `node-map` together. This unused
scaffold has no historical data and provides no online migration, dual-write,
CDC, or automatic data movement mechanism.

Transactions are local to one physical database only. An exam, its paper, and
scores must use the same `examId`; schedules retain their `courseId`.
Cross-shard workflows use business idempotency, explicit states, events,
reconciliation, and compensation. No XA, BASE, Seata, or other distributed
transaction coordinator is included.

## Verification And Packaging

```bash
SPRING_PROFILES_ACTIVE=test bash ./mvnw -B -ntp clean verify
SPRING_PROFILES_ACTIVE=test bash ./mvnw -B -ntp -DskipTests package
```

The test suite includes Domain rules, Application orchestration, MyBatis-Plus DAO
contracts, date-sequence Flyway migration contracts, broker-free MQ adapters, an actual
COLA native unary RPC proxy call, external-free Spring context assembly, and architecture
dependency checks. Building the image does not start the service.

Use `verify`, not `test`. The architecture-governance plugin is bound to the `verify`
phase and runs with `unknownLayerPolicy=FAIL`, so `clean test` would run every unit test
while performing no layer check at all. The generated `.github/workflows/ci.yml` and the
root `Jenkinsfile` both use `verify` for this reason.

Encrypt a configuration value with a 32-byte key supplied through
`EGON_CONFIG_DECRYPT_KEY` or `EGON_CONFIG_DECRYPT_KEY_FILE`:

```bash
printf '%s' 'plain-text' | EGON_CONFIG_DECRYPT_KEY='replace-with-32-byte-secret-key' \
  bash ./mvnw -q -pl egon-cola-source-service-starter -am -DskipTests compile exec:java \
  -Dexec.mainClass=top.egon.cola.archetype.source.service.starter.config.encryption.ConfigCipherCli
```

`ConfigCipherCli` takes no arguments and reads the plaintext from standard input.
Use the emitted `ENC(v1:...)` value in configuration.

## Container Delivery

The generated project uses one source-building `deploy/container/Dockerfile`:

```bash
docker build --build-arg CONTAINER_ENGINE=docker -f deploy/container/Dockerfile -t egon-cola-source-service:local .
podman build --build-arg CONTAINER_ENGINE=podman -f deploy/container/Dockerfile -t egon-cola-source-service:local .
nerdctl build --build-arg CONTAINER_ENGINE=nerdctl -f deploy/container/Dockerfile -t egon-cola-source-service:local .
```

Start the complete Docker development stack with:

```bash
docker compose --env-file deploy/env/.env.example -f deploy/compose/compose.docker.yaml up -d --build
```

Bundled Compose defaults to `APP_DATASOURCE_MODE=SHARDING` and provisions three
PostgreSQL primaries: `postgres-master-data`, `postgres-shard-0`, and
`postgres-shard-1`. It does not create replicas; `SHARDING_READWRITE` is code and
configuration support for environments that supply matching primary/replica endpoints.

Podman and nerdctl use `compose.podman.yaml` and `compose.nerdctl.yaml`. Production
uses the matching `.prod.yaml` file and an operator-owned `.env.prod`. See
`deploy/container/README.md` for rootless prerequisites, persistence, production
boundaries, and data-deletion warnings.

The root `Jenkinsfile` runs tests and can publish immutable images. Set
`PUBLISH_IMAGE=true` plus registry parameters to publish; it never deploys.

## Scope Boundary

This generated service has no business Controller, Web Filter, GraphQL endpoint, native grpc-java module, or enabled H2 console. Its Organization Facade client is intentionally not wired into current Application behavior.

## Native RPC, Tianshu and remote queries

This project exposes 11 evaluation unary operations from the shared Protobuf contract. Providers delegate to the existing facades. Remote queries use the existing domain port, MapStruct/BaseConverter and component DIRECT proxy/strategy factories. Configure `app.integrations.organization` with the exact target biz code, app code, group/version and timeout. `ORGANIZATION_FACADE_APP_CODE` must match the peer's registered Tianshu app code. References use the current process environment, version `1.0` by default, a 3000ms default bounded by the component ceiling, zero retries and FAIL_CLOSED.

Supply existing Tianshu RPC/Redis endpoints, registration resource URI, separate runtime/registry HMAC credentials, and an Tianquan-Shoubing SERVICE-token client allowed `tianshu:registration:write`. Complete the `TIANSHU_*`, `TIANQUAN_SHOUBING_*` and advertised-host entries in the environment sample. Compose maps Spring OAuth2 Client registration/provider `tianshuregistration`; direct Java launches must supply the corresponding `spring.security.oauth2.client.registration.tianshuregistration` and `spring.security.oauth2.client.provider.tianshuregistration.token-uri` external properties. Production enables RPC/Tianshu mTLS; configure and mount the certificate-chain, private-key and trust-certificate paths. No Tianshu or Tianquan-Shoubing container is bundled.

The platform OpenAPI MVC starter preserves business HTTP access. Document governance is disabled by default. Enabling it requires the platform document identity, published groups, JWT decoder and `yuheng.openapi.read` scope; published handlers also require explicit `@Operation(operationId = "...")` metadata. Tianquan-Shoubing Servlet filter auto-registration is disabled. HTTP registration follows the effective `server.port`.

The `test` profile disables RPC provider/consumer, Tianshu config/registry/Redis, HTTP registration and remote query clients, retaining H2 and local stubs. Stock Redisson auto-configuration is excluded; Tianshu creates only its explicitly configured Redis client. Existing PostgreSQL, Redis, RabbitMQ and their data volumes remain. Configuration decryption runs after Spring Boot Config Data; explicit imports/configtree replace retired bootstrap loading while encryption and key rules remain unchanged. Static, module and in-process RPC tests do not establish live Tianshu/Tianquan-Shoubing, mTLS or container interoperability.
