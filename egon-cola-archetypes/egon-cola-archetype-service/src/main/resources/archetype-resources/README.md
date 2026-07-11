#set( $symbol_pound = '#' )
${symbol_pound} ${rootArtifactId}

`${rootArtifactId}` is a service-only COLA sample for Course, Schedule, Exam, Paper, and Score workflows. Business traffic enters through Dubbo Triple RPC or RabbitMQ; HTTP is reserved for Spring Boot Actuator management endpoints.

${symbol_pound}${symbol_pound} Module Ownership

- `${rootArtifactId}-common`: stable errors, constants, enums, and identifier utilities.
- `${rootArtifactId}-facade`: serializable Dubbo request/response contracts and public facade APIs.
- `${rootArtifactId}-domain`: entities, aggregates, value objects, domain services, repository ports, and event publisher ports. It contains no persistence or MQ implementation.
- `${rootArtifactId}-application`: commands, queries, use-case managers, application validation, and result models.
- `${rootArtifactId}-infrastructure`: Spring Data JPA repositories, Flyway migrations, and RabbitMQ/local publisher implementations.
- `${rootArtifactId}-adapter`: Dubbo providers, facade conversion, validation, exception translation, and the score-command MQ consumer.
- `${rootArtifactId}-starter`: Spring Boot assembly, profiles, management configuration, and architecture/context tests.

The allowed internal dependency graph is:

```text
Common <- Domain <- Application <- Adapter
Facade <------------------------- Adapter
          Domain <- Infrastructure
          Adapter <- Starter -> Infrastructure
```

More precisely: Domain depends only on Common; Application and Infrastructure depend only on Domain; Adapter depends only on Application and Facade. Facade and Common have no inward dependency. Starter is the composition root.

${symbol_pound}${symbol_pound} Example Flows

- Course RPC creates a course with a unique normalized code, reads it, pages it, and schedules a class without overlapping time ranges.
- Exam RPC creates an exam for a course, attaches one paper, and publishes the exam only after its paper is ready.
- Score RPC records and queries validated scores. A RabbitMQ score command enters through `RecordScoreConsumer` and delegates to the same Application use case.
- Domain publisher ports describe course scheduling, exam publication, and score recording. Infrastructure supplies local or RabbitMQ implementations.

RabbitMQ support is intentionally basic transport. The sample does not promise retry, dead-letter queue, idempotent inbox, transactional outbox, or delivery guarantees beyond the configured broker behavior.

${symbol_pound}${symbol_pound} Profiles And Integrations

`local` is the default profile. Both `local` and `test` use H2 in PostgreSQL compatibility mode and require no Nacos, RabbitMQ, or PostgreSQL service. RabbitMQ publishers and listeners are disabled in `test`; `local` uses the local publisher implementation unless explicitly enabled.

`dev` and `prod` are external-integration profiles. Configure them through environment variables rather than committed secrets:

- Database: `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `DB_DRIVER_CLASS_NAME`.
- Nacos: `NACOS_SERVER_ADDR`, `NACOS_NAMESPACE`, `NACOS_GROUP`, `NACOS_USERNAME`, `NACOS_PASSWORD`.
- Dubbo: `DUBBO_REGISTRY_ADDRESS`, `DUBBO_PORT`.
- RabbitMQ: `RABBITMQ_HOST`, `RABBITMQ_PORT`, `RABBITMQ_USERNAME`, `RABBITMQ_PASSWORD`, `RABBITMQ_ENABLED`, `RABBITMQ_LISTENER_AUTO_STARTUP`.
- Configuration decryption: `CONFIG_DECRYPT_KEY` or the documented config-tree secret source.

${symbol_pound}${symbol_pound} Database Policy

Flyway owns schema evolution. `V1__init_student_management_evaluation.sql` is immutable. `V2__align_evaluation_course_exam_domain.sql` is the single alignment migration that adds the Course/Schedule/Exam/Paper/Score model while preserving valid V1 data. Never edit an applied migration; add the next version instead.

${symbol_pound}${symbol_pound} Verification And Packaging

```bash
SPRING_PROFILES_ACTIVE=test bash ./mvnw -B -ntp clean test
SPRING_PROFILES_ACTIVE=test bash ./mvnw -B -ntp -DskipTests package
docker build -t ${rootArtifactId}:local .
```

The test suite includes Domain rules, Application orchestration, JPA adapters, V1-to-V2 migration behavior, broker-free MQ adapters, an actual Dubbo Triple proxy call, external-free Spring context assembly, and ArchUnit dependency checks. Building the image does not start the service.

${symbol_pound}${symbol_pound} Scope Boundary

This generated service has no business Controller, Web Filter, GraphQL endpoint, native grpc-java module, or enabled H2 console. Organization dual-domain Facade integration is deferred to a separate specification and is not part of this service sample.
