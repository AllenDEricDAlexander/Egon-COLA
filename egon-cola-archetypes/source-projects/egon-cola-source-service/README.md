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

Persistence uses Common MP repositories and explicit Mapper XML; domain service ports do not inherit technical CRUD types.

## Domain-first package layout

Business-owned code puts the domain before the technical responsibility:

```text
domain/exam/entities
application/course/manage
infrastructure/exam/dao
infrastructure/exam/service/impl
adapter/course/facade/impl
adapter/exam/mq
```

This remains service-only: business traffic enters through COLA native unary RPC or RabbitMQ, with no business Controller, Web Filter, GraphQL, or VO package. The external Organization boundary is a Domain capability contract in `domain/course/service` plus its technical client in `infrastructure/client/organization`.
The Evaluation contract is published by this project itself from `top.egon.internal.archetype.source:egon-cola-source-service-facade`, while the external Organization contract stays a separately published artifact that the generated POM resolves through the explicit `organization-facade.group-id`, `organization-facade.artifact-id`, `organization-facade.version` and `organization-facade.package` properties supplied at generation time.

The allowed internal dependency graph is:

```text
Common <- Domain <- Application <- Adapter -> Own Evaluation Facade (protocol only)
          Domain <- Infrastructure -> Published Organization Facade (external artifact)
          Adapter <- Starter -> Infrastructure
```

More precisely: Domain depends only on Common; Facade depends on no internal module; Application and Infrastructure depend only on Domain; Adapter depends on Application plus this project's own Facade. Adapter implements the owned Evaluation Facade contract, Infrastructure consumes the published Organization Facade contract, and neither the peer Facade artifact nor the Organization provider depends on this generated project. Starter is the composition root, so there is no Web/Service Maven dependency cycle.

## Example Flows

- Course RPC creates a course with a unique normalized code, reads it, pages it, and schedules a class without overlapping time ranges.
- Exam RPC creates an exam for a course, attaches one paper, and publishes the exam only after its paper is ready.
- Score RPC records and queries validated scores. A RabbitMQ score command enters through `RecordScoreConsumer` and delegates to the same Application use case.
- Domain event service contracts in `domain/course/service` and `domain/exam/service` describe course scheduling, exam publication, and score recording. Infrastructure supplies the implementations and forwards the message to the domain-agnostic `infrastructure/mq` send boundary, which has local or RabbitMQ implementations.

RabbitMQ support is intentionally basic transport. The sample does not promise retry, dead-letter queue, idempotent inbox, transactional outbox, or delivery guarantees beyond the configured broker behavior.

## Profiles And Integrations

`dev` is the default profile for workstation development and `feature/*` branch verification. It uses the environment-backed PostgreSQL, RabbitMQ, and COLA RPC integrations.

`test` is selected automatically by Maven tests and is used by the `dev`, `release/*`, and `hotfix/*` validation pipelines. It uses H2 in PostgreSQL compatibility mode, disables RabbitMQ publishers and listeners, and selects the deterministic local `OrganizationDirectoryClient` implementation, so it requires no RabbitMQ, PostgreSQL, or external COLA RPC provider.

The Organization Facade client is an unused infrastructure foundation; no current Application use case calls the Organization port.

`prod` is reserved for runtime builds and deployments from `main`. Both `dev` and `prod` select the real Organization COLA RPC client, pin the published Organization contract through the `organization-facade.group-id`, `organization-facade.artifact-id` and `organization-facade.version` properties of the generated POM, and fail explicitly when the provider is unavailable. Configure them through environment variables rather than committed secrets:

- Database: configure the `master_data`, `shard_0`, and `shard_1` physical data sources described below.
- Tianshu: configure `TIANSHU_RPC_TARGET`, `TIANSHU_NAMESPACE`, separate runtime/registry HMAC credentials and Tianquan-Shoubing SERVICE tokens. `TIANSHU_ENABLED` and `TIANSHU_REGISTRY_ENABLED` control configuration and registration. See the native RPC/Tianshu section below for connection settings.
- COLA RPC: `TIANSHU_RPC_TARGET`, `RPC_PORT`, `ORGANIZATION_FACADE_TIMEOUT_MS`.
- Organization Facade: `ORGANIZATION_FACADE_ENABLED`, `ORGANIZATION_FACADE_GROUP`, `ORGANIZATION_FACADE_SERVICE_VERSION`.
- RabbitMQ: connection settings bind through Spring's own names — `SPRING_RABBITMQ_HOST`, `SPRING_RABBITMQ_PORT`, `SPRING_RABBITMQ_USERNAME`, `SPRING_RABBITMQ_PASSWORD` — while `RABBITMQ_ENABLED` and `RABBITMQ_LISTENER_AUTO_STARTUP` are this application's own switches.
- Configuration decryption: `EGON_CONFIG_DECRYPT_KEY`, `EGON_CONFIG_DECRYPT_KEY_FILE`, or the documented config-tree secret source.

## Verification And Packaging

```bash
SPRING_PROFILES_ACTIVE=test bash ./mvnw -B -ntp clean verify
SPRING_PROFILES_ACTIVE=test bash ./mvnw -B -ntp -DskipTests package
```

The test suite includes Domain rules, Application orchestration, MyBatis-Plus DAO
contracts, managed PostgreSQL DDL contracts, broker-free MQ adapters, an actual
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

This project exposes 11 evaluation unary operations from the Protobuf contract published by its own Facade module. Each named `*FacadeImpl` is the single native provider of one contract and delegates to the existing use cases. Remote queries use the existing domain port, MapStruct/BaseConverter and component DIRECT proxy/strategy factories. Configure `app.integrations.organization` with the exact target biz code, app code, group/version and timeout. `ORGANIZATION_FACADE_APP_CODE` must match the peer's registered Tianshu app code. References use the current process environment, version `1.0` by default, a 3000ms default bounded by the component ceiling, zero retries and FAIL_CLOSED.

Supply existing Tianshu RPC/Redis endpoints, registration resource URI, separate runtime/registry HMAC credentials, and an Tianquan-Shoubing SERVICE-token client allowed `tianshu:registration:write`. Complete the `TIANSHU_*`, `TIANQUAN_SHOUBING_*` and advertised-host entries in the environment sample. Compose maps Spring OAuth2 Client registration/provider `tianshuregistration`; direct Java launches must supply the corresponding `spring.security.oauth2.client.registration.tianshuregistration` and `spring.security.oauth2.client.provider.tianshuregistration.token-uri` external properties. Production enables RPC/Tianshu mTLS; configure and mount the certificate-chain, private-key and trust-certificate paths. No Tianshu or Tianquan-Shoubing container is bundled.

The platform OpenAPI MVC starter preserves business HTTP access. Document governance is disabled by default. Enabling it requires the platform document identity, published groups, JWT decoder and `yuheng.openapi.read` scope; published handlers also require explicit `@Operation(operationId = "...")` metadata. Tianquan-Shoubing Servlet filter auto-registration is disabled. HTTP registration follows the effective `server.port`.

The `test` profile disables RPC provider/consumer, Tianshu config/registry/Redis, HTTP registration and remote query clients, retaining H2 and local stubs. Stock Redisson auto-configuration is excluded; Tianshu creates only its explicitly configured Redis client. Existing PostgreSQL, Redis, RabbitMQ and their data volumes remain. Configuration decryption runs after Spring Boot Config Data; explicit imports/configtree replace retired bootstrap loading while encryption and key rules remain unchanged. Static, module and in-process RPC tests do not establish live Tianshu/Tianquan-Shoubing, mTLS or container interoperability.

## Repository, CQE and PostgreSQL

This archetype uses MyBatis-Plus 3.5.16. Business service ports retain domain semantics; concrete repositories extend `EgonColaRepository` and mappers extend `EgonColaMapper`. Queries use explicit XML. `EgonModel` owns id, tenantId, creation/update actors and times, `LocalDateTime deletedAt` and `Long version`: NULL is active, deletion writes a UTC timestamp and increments the version. AR/QueryChain are disabled; technical filling is mandatory.

`APP_DATASOURCE_MODE` supports `SHARDING` and `SHARDING_READWRITE` with LOCAL transactions. Single tables use explicit `!SINGLE group.schema.table`; broadcast tables are read-only. Default legacy tenant routing retains its old addresses. The optional `src/test/resources/sharding/two-level-readwrite.yml` example lives in infrastructure for multi-module projects. It hashes tenant_id into a tenant slot, then a business root ID into a bucket; order.id and item.order_id share the same root policy and physical group.

mix64-v1 is fixed. T/B are powers of two up to 1024, with product at most 4096. Balanced databases also require a balanced slot map and tenant workload. There is no automatic redistribution. Query fanout is bounded; commands require exact keys. Changing topology requires matching DDL and a deliberate data migration/rebuild; the test example is not a drop-in production schema.

The MP-SDJ starter owns datasource/topology setup and the managed `EgonColaPostgreDdlRunner` on physical PRIMARY targets using `db/egon-mp/V20260913_001__initialize_repository_schema.sql` and `repository-manifest.json`. Empty schemas initialize once; managed schemas verify checksums, prefix and route fingerprint. Non-empty unmanaged schemas fail with REBUILD_REQUIRED. Old B/V/manual SQL remains unchanged as an archive and is no longer the runtime entry point.

PostgreSQL owns replication. Ordinary reads use ROUND_ROBIN replicas; transaction/locking/strong reads use PRIMARY. No replica provisioning or promotion is implemented. Business, single and broadcast tables carry tenant_id. Cross-group LOCAL writes are rejected and mark rollback-only.

Set a unique `EGON_ID_MACHINE_ID` for each runtime; production uses the existing Common Snowflake generator. Profiles retain matching MP keys, diagnostics are dev-only and the raw recorder logger is OFF. Dynamic table names require explicit mappings. MybatisBatch runs inside the caller's transaction.

Default tests use isolated H2 and controlled dependencies. Physical routing tests require `-Degon.pg.routing=true` and `EGON_TEST_PG_URL`; read/write tests require `-Degon.pg.readwrite=true`, `EGON_TEST_PG_PRIMARY_URL` and `EGON_TEST_PG_REPLICA_URL`, plus dedicated `EGON_TEST_PG_USER/PASSWORD`. They create/clean only their UUID schemas and do not start databases. Real PG/SS, migration and EXPLAIN acceptance remains manual; a skip is not a pass.

## Two-level cache skeleton (disabled by default)

The generated project includes the cache starter with `enabled: false`. To enable it, provide a `RedissonClient`, set
`egon.cola.component.cache.enabled=true`, and explicitly add `@EnableCaching` to a configuration class. The mp-sd-ext
base repository no longer depends on a cache port: concrete repositories declare Spring Cache annotations. The
repository examples use `findCachedById` / `updateCachedById` (Agent defines its own business methods). Ordinary CRUD no
longer evicts implicitly; annotate every relevant write/delete path. See
the [cache starter README](../../../egon-cola-components/egon-cola-component-common/egon-cola-component-common-cache-spring-boot-starter/README.md)
for keys, conditions, combined operations, transactions and sync limitations.

## 2026-09-22 Java / CQE maintenance requirements

Normal entities use class; record is reserved for immutable value objects. Select @Builder for ordinary construction or compatible @SuperBuilder for inherited fields. Persisted enum codes use @EnumValue; frontend JSON codes use @JsonValue. Reuse Components and Common MP repositories. Validation uses native/custom constraint annotations, @Valid, @Validated and groups; ValidationUtils is a generic manual helper. Soft-delete business uniqueness must combine business columns + deleted_at with active-row NULL enforcement. Event delivery uses the Egon transactional outbox or actual MQ, with explicit transaction/failure and consumer-idempotency semantics. Existing source examples and old SQL must be reviewed against these new requirements; this documentation update does not migrate them.
