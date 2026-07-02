# Student Management

Student Management is a single Maven module sample generated from `egon-cola-archetype-light`.

The project demonstrates a large-monolith light domain architecture. The layers are Java packages, not Maven submodules:

```text
start / adapter / facade / application / infrastructure / common / domain
```

## Package Roles

`start` contains the Spring Boot entry point and boot-level configuration.

`adapter` handles inbound HTTP requests and facade implementations.

`facade` defines external API contracts and DTOs.

`application` coordinates use cases and transaction boundaries.

`domain` contains student and teaching models, domain services, and repository ports.

`infrastructure` implements repository ports with Spring Data JPA.

`common` contains project-local response, exception, constant, and utility types.

## Dependency Direction

The main call direction is:

```text
adapter -> application -> domain -> common
```

Infrastructure implements domain repository ports. Domain code must not depend on infrastructure, adapter, or application code. Facade code must stay independent from internal layers.

## Sample Use Cases

- Register a student.
- Create a course.
- Assign a course to a student through application-layer orchestration.

## Local Commands

```bash
./mvnw test
./mvnw -DskipTests package
./mvnw spring-boot:run
```

The default datasource is H2, so tests and local packaging do not require an external database. PostgreSQL is included as the runtime production database driver.
