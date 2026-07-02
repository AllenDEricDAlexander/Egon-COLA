# fable

fable is a student-management evaluation service generated from the Egon COLA service archetype. It demonstrates course
creation and exam-result recording through application orchestration, domain services, repository ports, Spring Data JPA
adapters, a facade-style RPC boundary, and a framework-neutral message-consumer boundary.

## Modules

- `fable-common`: shared response objects, business exceptions, error codes, constants, and id utilities.
- `fable-facade`: service contracts and request/response DTO records for course and exam-result operations.
- `fable-domain`: course and exam-result entities, status enums, domain services, and repository ports.
- `fable-application`: use-case orchestration for course management and exam-result recording.
- `fable-infrastructure`: Spring Data JPA persistence adapters, entity mappings, repository implementations, and Flyway
  migrations.
- `fable-adapter`: facade implementations, conversion helpers, message DTOs, message-consumer entry points, and service
  exception handling.
- `fable-starter`: Spring Boot application, runtime configuration, and generated verification tests.

## Pure Service Rule

This generated project is intentionally service-only. Do not add HTTP Controller classes, Web Filter classes, GraphQL
endpoints, Web VO packages, <code>spring-boot-starter-&#119;eb</code>, or <code>spring-boot-starter-&#119;ebflux</code>
unless the project is deliberately converted into a web-facing service.

## Extension Points

RPC: after selecting Dubbo or gRPC, add the concrete RPC annotations and dependencies in the `adapter` module and keep
application/domain modules framework-neutral.

MQ: after selecting Kafka, RocketMQ, or RabbitMQ, add concrete consumer annotations and dependencies in the `adapter`
module and keep message handling delegated to application services.

## Commands

```bash
bash ./mvnw test
bash ./mvnw -DskipTests package
bash ./mvnw -pl fable-starter spring-boot:run
```
