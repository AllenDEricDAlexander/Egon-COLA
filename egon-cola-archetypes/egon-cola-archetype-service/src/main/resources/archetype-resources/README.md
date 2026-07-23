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

${symbol_pound}${symbol_pound} Database Policy

Flyway owns schema evolution. `V1__init_student_management_evaluation.sql` is immutable. `V2__align_evaluation_course_exam_domain.sql` is the single alignment migration that adds the Course/Schedule/Exam/Paper/Score model while preserving valid V1 data. Never edit an applied migration; add the next version instead.

${symbol_pound}${symbol_pound} Verification And Packaging

```bash
SPRING_PROFILES_ACTIVE=test bash ./mvnw -B -ntp clean test
SPRING_PROFILES_ACTIVE=test bash ./mvnw -B -ntp -DskipTests package
```

The test suite includes Domain rules, Application orchestration, JPA adapters, V1-to-V2 migration behavior, broker-free MQ adapters, an actual Dubbo Triple proxy call, external-free Spring context assembly, and ArchUnit dependency checks. Building the image does not start the service.

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
