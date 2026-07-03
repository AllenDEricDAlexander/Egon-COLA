# fable

fable is a student-management evaluation service generated from the Egon COLA service archetype. It demonstrates course
creation and exam-result recording through application orchestration, domain services, repository ports, Spring Data JPA
adapters, Dubbo3 RPC facade implementations, and message-consumer entry points.

The service archetype demonstrates Dubbo3 RPC with the Triple protocol by default. It does not generate native grpc-java
services or a separate gRPC module.

## Modules

- `fable-common`: shared response objects, business exceptions, error codes, constants, and id utilities.
- `fable-facade`: Dubbo3 RPC service contracts, request records, and response DTO classes for course and exam-result
  operations.
- `fable-domain`: course and exam-result entities, status enums, domain services, and repository ports.
- `fable-application`: use-case orchestration for course management and exam-result recording.
- `fable-infrastructure`: Spring Data JPA persistence adapters, entity mappings, repository implementations, and Flyway
  migrations.
- `fable-adapter`: Dubbo3 Triple facade implementations, MapStruct Plus adapter convertors, message DTOs,
  message-consumer entry points, and service exception handling.
- `fable-starter`: Spring Boot application, runtime configuration, and generated verification tests.

## Pure Service Rule

This generated project is intentionally service-only. Do not add HTTP Controller classes, Web Filter classes, GraphQL
endpoints, Web VO packages, <code>spring-boot-starter-&#119;eb</code>, or <code>spring-boot-starter-&#119;ebflux</code>
unless the project is deliberately converted into a web-facing service.

## Clean Architecture Boundary Rules

- `application.manage` returns domain models or simple values only.
- `adapter` converts domain models to RPC or MQ-facing objects.
- Application pagination returns `Page<DomainModel>`; adapter converts it to external `PageResponse<DTO>` for Dubbo.
- `facade` defines Dubbo3 RPC contracts.
- `adapter` exposes facade implementations through Dubbo3 Triple.
- Converters use MapStruct Plus for flat model mapping and explicit Java code for semantic mapping.
- Converter wrappers use concrete MapStruct Plus mapper components, not the generic `Converter` bean.
- Spring Beans are named ordinary classes using Lombok `@RequiredArgsConstructor`; injected dependencies use
  `@Qualifier`.
- The generated project does not include native grpc-java services.

## Extension Points

RPC: Dubbo3 Triple is configured by default. Add or adjust Dubbo annotations and dependencies in the `adapter` module
and keep application/domain modules free of RPC framework dependencies.

MQ: after selecting Kafka, RocketMQ, or RabbitMQ, add concrete consumer annotations and dependencies in the `adapter`
module and keep message handling delegated to application services.

## Commands

```bash
bash ./mvnw test
bash ./mvnw -DskipTests package
bash ./mvnw -pl fable-starter spring-boot:run
```
