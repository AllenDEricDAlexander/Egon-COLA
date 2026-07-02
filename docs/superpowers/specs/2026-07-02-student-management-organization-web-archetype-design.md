# Student Management Organization Web Archetype Design

Date: 2026-07-02

## Goal

Revise `egon-cola-archetype-web` so it generates one independent `student-management-organization` multi-module Project that follows `egon-cola-archetypes/egon-cola-archetype-web/multi-project-multi-module-architecture.md`.

The generated project should use Spring Boot parent inheritance, Java 21, Maven Wrapper 3.9.14, and standard root project files. The work should not generate `student-management-evaluation` and should not start any generated application.

## Confirmed Decisions

- Use approach A from the design discussion.
- Generate only one Project: `student-management-organization`.
- Do not generate `student-management-evaluation`.
- Replace the current generic `client / app / start` module naming with architecture-aligned `facade / application / starter`.
- Add the missing `common` module.
- Generate a Maven aggregation root plus seven layer modules:
  - `student-management-organization-starter`
  - `student-management-organization-common`
  - `student-management-organization-facade`
  - `student-management-organization-application`
  - `student-management-organization-domain`
  - `student-management-organization-infrastructure`
  - `student-management-organization-adapter`
- Use the organization business domains from the architecture document: `user` and `teaching`.
- Include a small, complete sample that demonstrates user creation and assigning a user to a school class.
- Generate Maven Wrapper files for Apache Maven 3.9.14.
- Include `.gitignore`, `.gitattributes`, and `README.md` in generated projects.
- Write and commit this design before implementation planning.

## Current Context

The current web archetype already generates a Maven aggregation project, but its module model does not match the architecture document:

```text
${rootArtifactId}-client
${rootArtifactId}-adapter
${rootArtifactId}-app
${rootArtifactId}-domain
${rootArtifactId}-infrastructure
start
```

The architecture document describes an independent Project named `student-management-organization` with layered modules:

```text
starter
adapter
facade
application
domain
infrastructure
common
```

The current template is mostly empty `.gitkeep` scaffolding and contains package-info files with inaccurate package declarations. Its generated root POM already uses `spring-boot-starter-parent:3.5.16`, but it also carries broad BOMs and dependencies that are not needed for a focused organization sample. The generated project currently has `.gitignore`, but not Maven Wrapper files, `.gitattributes`, or a generated README.

## Scope

In scope:

- `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources`.
- `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/META-INF/maven/archetype-metadata.xml`.
- `egon-cola-archetypes/egon-cola-archetype-web/src/test/resources/projects/basic`, if integration-test expectations need to change.
- Generated root POM, module POMs, Java source, resources, tests, README, Maven Wrapper files, `.gitignore`, and `.gitattributes`.

Out of scope:

- `egon-cola-archetype-light`.
- `egon-cola-archetype-service`.
- Repository root Maven Wrapper governance.
- Generating `student-management-evaluation`.
- Producing two independent generated Projects from one archetype run.
- Starting generated applications.
- Real database setup outside the generated default H2 validation path.

## Generated Project Shape

The generated project should use `${rootArtifactId}` as the Project prefix. For the basic integration test, `rootArtifactId` should be set to `student-management-organization`.

Target generated structure:

```text
student-management-organization
├── pom.xml
├── mvnw
├── mvnw.cmd
├── .mvn/wrapper/maven-wrapper.properties
├── .gitignore
├── .gitattributes
├── README.md
├── student-management-organization-starter
├── student-management-organization-common
├── student-management-organization-facade
├── student-management-organization-application
├── student-management-organization-domain
├── student-management-organization-infrastructure
└── student-management-organization-adapter
```

The generated root package should be based on the archetype-selected `${package}`. For the organization sample, each module should use subpackages that make the layer and domain explicit:

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
user        用户、角色、权限
teaching    班级、年级、教学组织
```

The generated project should not contain `evaluation`, `course`, or `examing` modules or packages unless a future task explicitly adds them.

## Module Responsibilities

### starter

`student-management-organization-starter` contains the Spring Boot entry point, `application.yml`, and boot-level configuration.

It may depend on:

```text
adapter
infrastructure
```

It must not contain controllers, application services, domain services, repository implementations, mappers, or business rules.

### common

`student-management-organization-common` contains project-local basic utilities and shared internal types:

```text
constants
exceptions
response
utils
```

It should not depend on other generated modules. It is internal to this Project and must not be used by `facade`.

### facade

`student-management-organization-facade` contains external contracts:

```text
api
dto.user
dto.teaching
enums
exceptions
utils
```

It should define APIs such as `UserFacade` and `SchoolClassFacade`, plus request/response DTOs. It must not implement facade interfaces or depend on `common`, `application`, `domain`, `infrastructure`, `adapter`, or `starter`.

### application

`student-management-organization-application` coordinates use cases and transaction boundaries. It contains:

```text
manage.user
manage.user.impl
manage.teaching
manage.teaching.impl
convertor
validators
assemblers
client
```

It depends on `domain` and `common`. It must not depend on `adapter`, `facade`, `infrastructure`, or `starter`.

### domain

`student-management-organization-domain` contains the core domain model:

```text
entities.user
entities.teaching
service.user
service.teaching
repos.user
repos.teaching
validators
enums
vos
```

It depends on `common` only. It must not depend on Spring MVC, JPA, MyBatis, Redis, MQ, `application`, `adapter`, `facade`, `infrastructure`, or `starter`.

### infrastructure

`student-management-organization-infrastructure` implements technology details:

```text
repo.user.impl
repo.user.po
repo.user.jpa
repo.user.converter
repo.teaching.impl
repo.teaching.po
repo.teaching.jpa
repo.teaching.converter
config
```

It implements domain repository ports and may use Spring Data JPA, Flyway, H2, and PostgreSQL runtime support. It must not expose controllers or facade implementations.

### adapter

`student-management-organization-adapter` contains inbound adapters:

```text
controller.user
controller.teaching
facade.impl
convertor
handler
vo
filter
```

It handles HTTP requests and facade implementations, converts external DTOs to application inputs, and calls `application`. It must not directly call JPA repositories, mappers, Redis clients, or infrastructure implementations.

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

The architecture document states that infrastructure may import application in this multi-module web archetype. The generated template may keep that dependency if needed for application client implementations, but default repository implementations should still implement domain ports and avoid unnecessary application coupling.

Forbidden dependencies:

```text
domain -> application / infrastructure / adapter / facade / starter
application -> adapter / facade / infrastructure / starter
facade -> common / application / domain / infrastructure / adapter / starter
adapter -> infrastructure
starter -> domain directly for business work
```

## Student Management Organization Sample

The sample should be small but complete enough to prove module boundaries.

Core flow 1: create a user.

```text
UserController
    -> UserManage
        -> UserDomainService
        -> UserRepository
            -> UserRepositoryImpl
                -> UserJpaRepository
```

Core flow 2: create a school class and assign a user to it.

```text
SchoolClassController
    -> SchoolClassManage
        -> UserDomainService
        -> SchoolClassDomainService
        -> UserRepository
        -> SchoolClassRepository
```

The sample should include:

- User entity with `id`, `name`, `email`, and status.
- School class entity with `id`, `name`, `gradeName`, and assigned user ids.
- Domain service rules:
  - User email must be unique at application/repository boundary.
  - A user cannot be assigned to the same class twice.
- Application orchestration:
  - `UserManage` creates and reads users.
  - `SchoolClassManage` creates classes and assigns users.
- Infrastructure persistence:
  - JPA-backed PO and repository implementations.
  - H2 default datasource.
  - Exactly one initial Flyway migration for user, school class, and assignment tables.

The sample should not attempt to model roles, permissions, grades, or evaluation workflows beyond package markers. Those can remain documented extension points.

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
- `adapter` depends on `application`, `facade`, `common`, `spring-boot-starter-web`, and `spring-boot-starter-validation`.
- `starter` depends on `adapter`, `infrastructure`, `spring-boot-starter`, `spring-boot-starter-actuator`, and test dependencies.
- Test dependencies include `spring-boot-starter-test` and ArchUnit where dependency rules are validated.

Do not include these by default:

- Spring AI or Spring AI Alibaba.
- MCP SDK.
- Drools.
- Redis.
- Spring Security.
- Tika.
- Search/tool-calling starters.
- Evaluation-project dependencies.

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

- The Project as `student-management-organization`.
- The fact that this archetype generates only the organization Project, not evaluation.
- The Maven module layout and responsibilities.
- The `user` and `teaching` domains.
- The dependency direction and forbidden shortcuts.
- Local commands:

```bash
./mvnw test
./mvnw -DskipTests package
./mvnw -pl student-management-organization-starter spring-boot:run
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

The descriptor name should reflect `student-management-organization`.

The metadata should include:

- Root POM and README with filtering.
- Root wrapper and git files.
- Java source and test source with `packaged=true` where package expansion is required.
- Resource files such as `application.yml`, `logback-spring.xml`, and SQL migrations.

The metadata should remove or replace current references to:

- `__rootArtifactId__-client`.
- `__rootArtifactId__-app`.
- `start`.
- Current empty `app1/app2/customer/order` package markers that do not match the organization sample.

## Validation Plan

Implementation should run focused archetype validation:

```bash
bash ./mvnw -B -ntp -pl egon-cola-archetypes/egon-cola-archetype-web -am test
```

The generated basic project should run its tests, not start the application.

Generated project validation should confirm:

- `student-management-organization-evaluation` is not generated.
- `student-management-evaluation` is not generated.
- `student-management-organization-client`, `student-management-organization-app`, and `start` are not generated as modules.
- The seven target modules are generated.
- Maven Wrapper files exist and reference Maven 3.9.14.
- `.gitignore`, `.gitattributes`, and README exist.
- The generated root POM inherits `spring-boot-starter-parent:3.5.16`.
- Java 21 is configured.
- Exactly one initial Flyway migration exists.
- Generated tests pass.
- ArchUnit or equivalent tests enforce cross-module or package dependency boundaries.

Do not run `spring-boot:run` during validation.

## Design Pattern Consideration

No extra business design pattern should be introduced solely for the archetype.

The generated project already uses the structural patterns appropriate to the architecture:

- Maven module boundaries for coarse layer separation.
- Repository ports in domain and adapters in infrastructure.
- Application services for orchestration and transaction boundaries.
- Facade interfaces for external contracts.

Strategy, Factory Method, Decorator, State, and Chain of Responsibility are unnecessary for the small organization sample. Direct application services and domain services are clearer and easier to verify.

## Risks

- Renaming generated modules from `client/app/start` to `facade/application/starter` touches the archetype metadata, root POM, module POMs, and generated package paths.
- Archetype filtering can corrupt Maven variables if filtering is applied too broadly.
- Hidden files and wrapper files need careful archetype metadata handling.
- Cross-module dependency rules can become circular if application, domain, and infrastructure responsibilities are mixed.
- Adding JPA and Flyway requires a coherent default H2 configuration so generated tests do not need external services.
- Maven Wrapper executable permissions may not survive generation on every platform; this should be verified and reported.

## Acceptance Criteria

- `egon-cola-archetype-web` generates one `student-management-organization` Project.
- The generated project does not generate `student-management-evaluation`.
- Generated modules are `common`, `facade`, `domain`, `application`, `infrastructure`, `adapter`, and `starter` with the `student-management-organization-*` prefix.
- The generated root POM inherits `spring-boot-starter-parent:3.5.16`.
- Java 21 is configured.
- Generated project includes Maven Wrapper files for Maven 3.9.14.
- Generated project includes `.gitignore`, `.gitattributes`, and README.
- The organization sample includes user creation and assigning a user to a school class.
- Exactly one initial Flyway migration creates the sample schema.
- Generated tests validate sample behavior and dependency boundaries.
- Relevant Maven validation is run and reported honestly.
- No generated application is started.
