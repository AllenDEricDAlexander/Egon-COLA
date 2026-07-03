#set( $symbol_pound = '#' )
# Student Management

Student Management is a single Maven module sample generated from `egon-cola-archetype-light`.

The project demonstrates a large-monolith light domain architecture. The layers are Java packages, not Maven submodules:

```text
start / adapter / facade / application / infrastructure / common / domain
```

${symbol_pound}${symbol_pound} Package Roles

`start` contains the Spring Boot entry point and boot-level configuration.

`adapter` handles inbound HTTP requests and facade implementations.

`facade` defines external API contracts and DTOs.

`application` coordinates use cases and transaction boundaries.

`domain` contains student and teaching models, domain services, and repository ports.

`infrastructure` implements repository ports with Spring Data JPA.

`common` contains project-local response, exception, constant, and utility types.

${symbol_pound}${symbol_pound} Dependency Direction

The main call direction is:

```text
adapter -> application -> domain -> common
```

Infrastructure implements domain repository ports. Domain code must not depend on infrastructure, adapter, or application code. Facade code must stay independent from internal layers.

${symbol_pound}${symbol_pound} Clean Architecture Boundary Rules

- `application.manage` returns domain models or simple values only.
- `adapter` converts domain models to HTTP, RPC, or MQ-facing objects.
- `facade` defines Dubbo3 RPC contracts.
- `adapter` exposes facade implementations through Dubbo3 Triple.
- Converters use MapStruct Plus for flat model mapping and explicit Java code for semantic mapping.
- Spring Beans are named ordinary classes using Lombok `@RequiredArgsConstructor`; injected dependencies use `@Qualifier`.
- The generated project does not include native grpc-java services.

${symbol_pound}${symbol_pound} Sample Use Cases

- Register a student.
- Create a course.
- Assign a course to a student through application-layer orchestration.

${symbol_pound}${symbol_pound} Local Commands

```bash
./mvnw test
./mvnw -DskipTests package
./mvnw spring-boot:run
```

The default datasource is H2, so tests and local packaging do not require an external database. PostgreSQL is included as the runtime production database driver.
