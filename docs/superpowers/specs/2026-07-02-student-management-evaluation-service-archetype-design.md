# Student Management Evaluation Service Archetype Design

Date: 2026-07-02

## Goal

Revise `egon-cola-archetype-service` so it generates one independent pure Service Project named `student-management-evaluation`, following `egon-cola-archetypes/egon-cola-archetype-service/student-management-service-only-rpc-mq-architecture.md`.

The generated project should use Spring Boot parent inheritance, Java 21, Maven Wrapper 3.9.14, and standard root project files. It should not generate `student-management-organization`, should not expose HTTP/Web entry points, and should not start any generated application during validation.

## Confirmed Decisions

- Use approach A from the design discussion.
- Generate only one Project: `student-management-evaluation`.
- Do not generate `student-management-organization`.
- Treat this archetype as pure Service: no HTTP Controller, no Web Filter, no GraphQL, no Web VO, no direct Web API.
- Replace the current generic `client / app / start` module naming with architecture-aligned `facade / application / starter`.
- Add the missing `adapter` and `common` module concepts explicitly.
- Generate a Maven aggregation root plus seven layer modules:
  - `student-management-evaluation-starter`
  - `student-management-evaluation-common`
  - `student-management-evaluation-facade`
  - `student-management-evaluation-application`
  - `student-management-evaluation-domain`
  - `student-management-evaluation-infrastructure`
  - `student-management-evaluation-adapter`
- Use the evaluation business domains from the architecture document: `course` and `examing`.
- Include a small complete sample that demonstrates course creation/query through a Facade/RPC-style entry and recording an exam result through an MQ-style consumer.
- Generate Maven Wrapper files for Apache Maven 3.9.14.
- Include `.gitignore`, `.gitattributes`, and `README.md` in generated projects.
- Write and commit this design before implementation planning.

## Current Context

The current service archetype already generates a Maven aggregation project, but its module model does not match the pure Service architecture document:

```text
${rootArtifactId}-client
${rootArtifactId}-app
${rootArtifactId}-domain
${rootArtifactId}-infrastructure
start
```

The architecture document describes an independent Project with layered modules:

```text
starter
adapter
facade
application
domain
infrastructure
common
```

The current template is mostly empty `.gitkeep` scaffolding and uses old sample domains such as `customer` and `order`. Its generated project currently has `.gitignore`, but not Maven Wrapper files, `.gitattributes`, or a generated README. It also lacks pure Service guards that prevent HTTP/Web packages from creeping into the template.

## Scope

In scope:

- `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources`.
- `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/META-INF/maven/archetype-metadata.xml`.
- `egon-cola-archetypes/egon-cola-archetype-service/src/test/resources/projects/basic`, if integration-test expectations need to change.
- Generated root POM, module POMs, Java source, resources, tests, README, Maven Wrapper files, `.gitignore`, and `.gitattributes`.

Out of scope:

- `egon-cola-archetype-light`.
- `egon-cola-archetype-web`.
- Repository root Maven Wrapper governance.
- Generating `student-management-organization`.
- Producing two independent generated Projects from one archetype run.
- Adding concrete Dubbo, gRPC, Kafka, RocketMQ, or RabbitMQ dependencies by default.
- Starting generated applications.
- Real database setup outside the generated default H2 validation path.

## Generated Project Shape

The generated project should use `${rootArtifactId}` as the Project prefix. For the basic integration test, `rootArtifactId` should be set to `student-management-evaluation`.

Target generated structure:

```text
student-management-evaluation
├── pom.xml
├── mvnw
├── mvnw.cmd
├── .mvn/wrapper/maven-wrapper.properties
├── .gitignore
├── .gitattributes
├── README.md
├── student-management-evaluation-starter
├── student-management-evaluation-common
├── student-management-evaluation-facade
├── student-management-evaluation-application
├── student-management-evaluation-domain
├── student-management-evaluation-infrastructure
└── student-management-evaluation-adapter
```

The generated root package should be based on the archetype-selected `${package}`. Each module should use subpackages that make the layer explicit:

```text
${package}.starter
${package}.adapter
${package}.facade
${package}.application
${package}.domain
${package}.infrastructure
${package}.common
```

The business domains are:

```text
course      课程、课程安排、课程资源
examing     考试、成绩、评价
```

The generated project should not contain `organization`, `user`, or `teaching` modules or packages unless a future task explicitly adds cross-Project integration examples.

## Pure Service Constraints

This archetype must not generate:

```text
controller
web
filter
graphql
vo
spring-boot-starter-web
spring-boot-starter-webflux
```

The generated `adapter` module should only contain:

```text
rpc
mq
facade.impl
convertor
dto
handler
```

Default RPC and MQ examples should be technology-neutral Spring components:

- Facade implementations demonstrate the RPC-provider boundary without binding the template to Dubbo or gRPC.
- MQ consumer classes consume plain message DTOs through ordinary methods, demonstrating the consumer boundary without binding the template to Kafka, RocketMQ, or RabbitMQ.
- README explains where teams can attach concrete RPC/MQ framework annotations and dependencies.

## Module Responsibilities

### starter

`student-management-evaluation-starter` contains the Spring Boot entry point, `application.yml`, and service-level boot configuration.

It may depend on:

```text
adapter
infrastructure
```

It must not contain controllers, Web filters, application services, domain services, repository implementations, RPC provider business logic, MQ consumer business logic, mappers, or business rules.

### common

`student-management-evaluation-common` contains internal reusable types:

```text
constants
exceptions
response
utils
```

It should not depend on other generated modules. It is internal to this Project and must not be used by `facade`.

### facade

`student-management-evaluation-facade` contains RPC-facing contracts:

```text
api
dto.course
dto.examing
enums
exceptions
utils
```

It should define APIs such as `CourseFacade` and `ExamResultFacade`, plus request/response DTOs. It must not implement facade interfaces or depend on `common`, `application`, `domain`, `infrastructure`, `adapter`, or `starter`.

### application

`student-management-evaluation-application` coordinates use cases and transaction boundaries. It contains:

```text
manage.course
manage.course.impl
manage.examing
manage.examing.impl
convertor
validators
assemblers
client
```

It depends on `domain` and `common`. It must not depend on `adapter`, `facade`, `infrastructure`, or `starter`.

### domain

`student-management-evaluation-domain` contains the core domain model:

```text
entities.course
entities.examing
service.course
service.examing
repos.course
repos.examing
validators
enums
vos
```

It depends on `common` only. It must not depend on Spring MVC, JPA, MyBatis, Redis, MQ, Dubbo, gRPC, `application`, `adapter`, `facade`, `infrastructure`, or `starter`.

### infrastructure

`student-management-evaluation-infrastructure` implements technology details:

```text
repo.course.impl
repo.course.po
repo.course.jpa
repo.course.converter
repo.examing.impl
repo.examing.po
repo.examing.jpa
repo.examing.converter
client
mq
cache
config
```

It implements domain repository ports and may use Spring Data JPA, Flyway, H2, and PostgreSQL runtime support. It must not consume inbound MQ messages, expose RPC providers, expose facade implementations, or handle Web requests.

### adapter

`student-management-evaluation-adapter` contains inbound service adapters:

```text
rpc
mq
facade.impl
convertor
dto
handler
```

It calls `application` and converts between facade/message DTOs and application inputs. It must not directly call JPA repositories, mappers, Redis clients, MQ templates, or infrastructure implementations.

## Dependency Direction

Generated Maven module dependencies should enforce this direction:

```text
starter -> adapter, infrastructure
adapter -> application, facade, common
application -> domain, common
domain -> common
infrastructure -> domain, application, common
facade -> JDK / validation only
common -> JDK only
```

The architecture document states that infrastructure may import application in this pure Service archetype. The generated template may keep that dependency for future application client implementations, but default repository implementations should still implement domain ports and avoid unnecessary application coupling.

Forbidden dependencies:

```text
domain -> application / infrastructure / adapter / facade / starter
application -> adapter / facade / infrastructure / starter
facade -> common / application / domain / infrastructure / adapter / starter
adapter -> infrastructure
starter -> domain directly for business work
adapter -> Spring Web / WebFlux
```

## Student Management Evaluation Sample

The sample should be small but complete enough to prove pure Service boundaries.

Core flow 1: create and query a course through a Facade/RPC-style entry.

```text
CourseFacadeImpl
    -> CourseManage
        -> CourseDomainService
        -> CourseRepository
            -> CourseRepositoryImpl
                -> CourseJpaRepository
```

Core flow 2: record an exam result through an MQ-style consumer.

```text
ExamResultMessageConsumer
    -> ExamManage
        -> CourseDomainService
        -> ExamDomainService
        -> CourseRepository
        -> ExamResultRepository
```

The sample should include:

- Course entity with `id`, `name`, `credit`, and status.
- Exam result entity with `id`, `courseId`, `studentId`, `score`, and result status.
- Domain service rules:
  - Course name must be unique at application/repository boundary.
  - Exam score must be between 0 and 100.
  - Exam result recording must verify the course exists.
- Application orchestration:
  - `CourseManage` creates and reads courses.
  - `ExamManage` records exam results after course validation.
- Adapter examples:
  - `CourseFacadeImpl` implements `CourseFacade`.
  - `ExamResultFacadeImpl` implements `ExamResultFacade` for RPC-style direct calls.
  - `ExamResultMessageConsumer` accepts an `ExamResultMessage` and calls `ExamManage`.
- Infrastructure persistence:
  - JPA-backed PO and repository implementations.
  - H2 default datasource.
  - Exactly one initial Flyway migration for course and exam result tables.

The sample should not attempt to model organization, user, teaching, or cross-Project RPC integration beyond documented extension points.

## POM Design

The generated root `pom.xml` should inherit directly from Spring Boot:

```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.5.16</version>
    <relativePath/>
</parent>
```

Generated coordinates should preserve archetype variables:

```xml
<groupId>${groupId}</groupId>
<artifactId>${rootArtifactId}-parent</artifactId>
<version>${version}</version>
```

Root modules should be:

```xml
<modules>
    <module>${rootArtifactId}-common</module>
    <module>${rootArtifactId}-facade</module>
    <module>${rootArtifactId}-domain</module>
    <module>${rootArtifactId}-application</module>
    <module>${rootArtifactId}-infrastructure</module>
    <module>${rootArtifactId}-adapter</module>
    <module>${rootArtifactId}-starter</module>
</modules>
```

Default dependency model:

- Root POM manages generated module versions.
- Root POM sets Java 21 and UTF-8 encodings.
- `common` has no Spring dependency by default.
- `facade` uses validation annotations only where needed.
- `domain` depends on `common`.
- `application` depends on `domain` and `common`, plus Spring transaction/stereotype APIs as needed.
- `infrastructure` depends on `domain`, `application` if needed by the documented architecture, `common`, `spring-boot-starter-data-jpa`, `flyway-core`, H2, and PostgreSQL runtime.
- `adapter` depends on `application`, `facade`, and `common`.
- `starter` depends on `adapter`, `infrastructure`, `spring-boot-starter`, `spring-boot-starter-actuator`, and test dependencies.
- Test dependencies include `spring-boot-starter-test` and ArchUnit where dependency rules are validated.

Do not include these by default:

- `spring-boot-starter-web`.
- `spring-boot-starter-webflux`.
- Spring AI or Spring AI Alibaba.
- MCP SDK.
- Drools.
- Redis.
- Spring Security.
- Tika.
- Concrete Dubbo, gRPC, Kafka, RocketMQ, or RabbitMQ dependencies.
- Organization-project dependencies.

## Maven Wrapper and Root Files

Generated root files:

```text
mvnw
mvnw.cmd
.mvn/wrapper/maven-wrapper.properties
.gitignore
.gitattributes
README.md
```

The wrapper properties should target Apache Maven 3.9.14:

```properties
distributionUrl=https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.14/apache-maven-3.9.14-bin.zip
```

`.gitignore` should ignore target output, IDE files, OS temporary files, logs, and local env files. It must not ignore Maven Wrapper files or generated migration files.

`.gitattributes` should define stable line endings, with LF for `mvnw` and CRLF for `mvnw.cmd`.

## README Design

The generated README should describe:

- The Project as `student-management-evaluation`.
- The fact that this archetype generates only the evaluation Project, not organization.
- The pure Service rule: no HTTP Controller or Web API by default.
- The Maven module layout and responsibilities.
- The `course` and `examing` domains.
- The RPC/Façade and MQ-style sample flows.
- The dependency direction and forbidden shortcuts.
- Local commands:

```bash
./mvnw test
./mvnw -DskipTests package
```

The README may include the Spring Boot run command as a manual option, but validation must not execute it:

```bash
./mvnw -pl student-management-evaluation-starter spring-boot:run
```

The README should be concise and operational. It should not duplicate the whole architecture document.

## Archetype Metadata Design

`archetype-metadata.xml` should be updated so generated module ids and directories match the target module names:

```text
__rootArtifactId__-common
__rootArtifactId__-facade
__rootArtifactId__-domain
__rootArtifactId__-application
__rootArtifactId__-infrastructure
__rootArtifactId__-adapter
__rootArtifactId__-starter
```

The descriptor name should reflect `student-management-evaluation`.

The metadata should include:

- Root POM and README with filtering.
- Root wrapper and git files.
- Java source and test source with `packaged=true` where package expansion is required.
- Resource files such as `application.yml`, `logback-spring.xml`, and SQL migrations.

The metadata should remove or replace current references to:

- `__rootArtifactId__-client`.
- `__rootArtifactId__-app`.
- `start`.
- Current empty `app1/app2/customer/order` package markers.

## Validation Plan

Implementation should run focused archetype validation:

```bash
bash ./mvnw -B -ntp -pl egon-cola-archetypes/egon-cola-archetype-service -am test
```

The generated basic project should run its tests, not start the application.

Generated project validation should confirm:

- `student-management-organization` is not generated.
- `student-management-evaluation-client`, `student-management-evaluation-app`, and `start` are not generated as modules.
- The seven target modules are generated.
- Maven Wrapper files exist and reference Maven 3.9.14.
- `.gitignore`, `.gitattributes`, and README exist.
- The generated root POM inherits `spring-boot-starter-parent:3.5.16`.
- Java 21 is configured.
- `spring-boot-starter-web` and `spring-boot-starter-webflux` are absent.
- No generated package contains `controller`, `web`, `filter`, `graphql`, or `vo`.
- Exactly one initial Flyway migration exists.
- Generated tests pass.
- ArchUnit or equivalent tests enforce dependency boundaries and pure Service restrictions.

Do not run `spring-boot:run` during validation.

## Design Pattern Consideration

No extra business design pattern should be introduced solely for the archetype.

The generated project already uses the structural patterns appropriate to the architecture:

- Maven module boundaries for coarse layer separation.
- Facade interfaces for RPC contracts.
- Technology-neutral MQ consumer adapter classes for inbound event handling.
- Repository ports in domain and adapters in infrastructure.
- Application services for orchestration and transaction boundaries.

Strategy, Factory Method, Decorator, State, and Chain of Responsibility are unnecessary for the small evaluation sample. Direct application services and domain services are clearer and easier to verify.

## Risks

- Renaming generated modules from `client/app/start` to `facade/application/starter` touches the archetype metadata, root POM, module POMs, and generated package paths.
- Pure Service restrictions can regress if a Web starter or controller package is accidentally copied from the web archetype.
- Archetype filtering can corrupt Maven variables if filtering is applied too broadly.
- Hidden files and wrapper files need careful archetype metadata handling.
- Cross-module dependency rules can become circular if application, domain, and infrastructure responsibilities are mixed.
- Adding JPA and Flyway requires a coherent default H2 configuration so generated tests do not need external services.
- Maven Wrapper executable permissions may not survive generation on every platform; this should be verified and reported.

## Acceptance Criteria

- `egon-cola-archetype-service` generates one `student-management-evaluation` Project.
- The generated project does not generate `student-management-organization`.
- Generated modules are `common`, `facade`, `domain`, `application`, `infrastructure`, `adapter`, and `starter` with the `student-management-evaluation-*` prefix.
- The generated root POM inherits `spring-boot-starter-parent:3.5.16`.
- Java 21 is configured.
- Generated project includes Maven Wrapper files for Maven 3.9.14.
- Generated project includes `.gitignore`, `.gitattributes`, and README.
- No HTTP Controller, Web Filter, GraphQL, Web VO, `spring-boot-starter-web`, or `spring-boot-starter-webflux` is generated.
- The evaluation sample includes course creation/query and recording an exam result through MQ-style input.
- Exactly one initial Flyway migration creates the sample schema.
- Generated tests validate sample behavior, dependency boundaries, and pure Service restrictions.
- Relevant Maven validation is run and reported honestly.
- No generated application is started.
