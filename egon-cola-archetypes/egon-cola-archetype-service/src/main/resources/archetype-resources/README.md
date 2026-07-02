#set( $symbol_pound = '#' )
# ${rootArtifactId}

${rootArtifactId} is a student-management evaluation service generated from the Egon COLA service archetype. It demonstrates course creation and exam-result recording through application orchestration, domain services, repository ports, Spring Data JPA adapters, a facade-style RPC boundary, and a framework-neutral message-consumer boundary.

${symbol_pound}${symbol_pound} Modules

- `${rootArtifactId}-common`: shared response objects, business exceptions, error codes, constants, and id utilities.
- `${rootArtifactId}-facade`: service contracts and request/response DTO records for course and exam-result operations.
- `${rootArtifactId}-domain`: course and exam-result entities, status enums, domain services, and repository ports.
- `${rootArtifactId}-application`: use-case orchestration for course management and exam-result recording.
- `${rootArtifactId}-infrastructure`: Spring Data JPA persistence adapters, entity mappings, repository implementations, and Flyway migrations.
- `${rootArtifactId}-adapter`: facade implementations, conversion helpers, message DTOs, message-consumer entry points, and service exception handling.
- `${rootArtifactId}-starter`: Spring Boot application, runtime configuration, and generated verification tests.

${symbol_pound}${symbol_pound} Pure Service Rule

This generated project is intentionally service-only. Do not add HTTP Controller classes, Web Filter classes, GraphQL endpoints, Web VO packages, <code>spring-boot-starter-&#119;eb</code>, or <code>spring-boot-starter-&#119;ebflux</code> unless the project is deliberately converted into a web-facing service.

${symbol_pound}${symbol_pound} Extension Points

RPC: after selecting Dubbo or gRPC, add the concrete RPC annotations and dependencies in the `adapter` module and keep application/domain modules framework-neutral.

MQ: after selecting Kafka, RocketMQ, or RabbitMQ, add concrete consumer annotations and dependencies in the `adapter` module and keep message handling delegated to application services.

${symbol_pound}${symbol_pound} Commands

```bash
bash ./mvnw test
bash ./mvnw -DskipTests package
bash ./mvnw -pl ${rootArtifactId}-starter spring-boot:run
```
