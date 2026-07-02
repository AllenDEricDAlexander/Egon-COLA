# Student Management Light Archetype Design

Date: 2026-07-02

## Goal

Revise `egon-cola-archetype-light` so it generates a complete single-module `student-management` application that follows the large-monolith light domain architecture documented in `egon-cola-archetypes/egon-cola-archetype-light/large-monolith-light-domain-architecture.md`.

The generated project should use Spring Boot parent inheritance, Java 21, Maven Wrapper 3.9.14, and standard project files. The work should not start or run any generated application.

## Confirmed Decisions

- Use the complete large-monolith light domain package layout, not the smaller current charging sample layout.
- Keep the generated project as one Maven module; logical layers are Java packages, not Maven submodules.
- Replace the charging business sample with `student-management`.
- Include example flows for student registration and assigning a course to a student.
- Generate Maven Wrapper files for Apache Maven 3.9.14, using `mvnw` and Maven Wrapper rather than Maven Daemon `mvnd`.
- Use a Spring Boot parent POM model, based on CyberMario's build baseline but without copying CyberMario-specific AI, MCP, Drools, Redis, Security, Tika, or other business dependencies.
- Include `.gitignore`, `.gitattributes`, and `README.md` in generated projects.
- Write and commit this design before implementation planning.

## Current Context

The current light archetype is still built around a charging system sample. Its generated project contains packages such as:

```text
adapter
application
domain
infrastructure
```

It does not yet model the architecture document's full package set:

```text
start
adapter
facade
application
infrastructure
common
domain
```

The current template POM imports Spring Boot dependencies through dependency management instead of inheriting from `spring-boot-starter-parent`. It also lacks generated Maven Wrapper files, `.gitignore`, and `.gitattributes`.

The archetype metadata currently enumerates Java, resource, test, image, and README file sets. It will need updates so root files and wrapper files are included correctly in generated projects.

## Scope

In scope:

- `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources`.
- `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/META-INF/maven/archetype-metadata.xml`.
- The light archetype basic integration-test inputs under `egon-cola-archetypes/egon-cola-archetype-light/src/test/resources/projects/basic`, if they need adjustment for the new generated output.
- Template Java source, resource files, test source, root POM, README, Maven Wrapper files, `.gitignore`, and `.gitattributes`.

Out of scope:

- Other archetypes such as service and web archetypes.
- Repository root Maven Wrapper governance.
- Java source changes outside `egon-cola-archetype-light`.
- Changing the architecture document unless implementation reveals a direct contradiction that must be corrected separately.
- Starting generated applications.
- Database migration changes in this repository.

## Generated Package Architecture

The generated project should use the archetype-selected `${package}` as the base package and create this package model:

```text
${package}
    start
        StudentManagementApplication.java
        config
    adapter
        controller
            student
            teaching
        facade.impl
        handler
        convertor
        vo
    facade
        api
        dto
        enums
        exceptions
    application
        manage
            student
                StudentManage.java
                impl
                    StudentManageImpl.java
            teaching
                CourseManage.java
                impl
                    CourseManageImpl.java
        convertor
        validators
    infrastructure
        repo
            student
                impl
                po
                converter
            teaching
                impl
                po
                converter
        config
    common
        constants
        exceptions
        response
        utils
    domain
        student
            model
            service
            repos
        teaching
            model
            service
            repos
```

The architecture should demonstrate the documented dependency direction:

```text
start imports adapter and infrastructure
adapter imports application and facade
application imports domain
domain imports common
infrastructure imports application where needed and implements domain ports
```

`facade` should not depend on `common`. `domain` should not depend on `application` or `infrastructure`. `application` should not depend on concrete infrastructure implementations.

## Student Management Sample

The template should provide a small but complete `student-management` example, not only empty packages.

The core flows are:

```text
StudentController
    -> StudentManage
        -> StudentDomainService
        -> StudentRepository

CourseController
    -> CourseManage
        -> StudentDomainService
        -> CourseDomainService
        -> CourseRepository
```

The generated example should include:

- Student registration.
- Course creation or lookup.
- Assigning an existing course to an existing student.
- A cross-domain orchestration path in `application.manage.teaching` that coordinates `domain.student` and `domain.teaching` without direct domain-to-domain dependency.
- Infrastructure repository implementations backed by Spring Data JPA, with H2 defaults so generated tests and local sample use do not require external services.
- Exactly one initial Flyway migration under `src/main/resources/db/migration`, creating the minimal student, course, and assignment tables required by the sample.

The sample should stay small. It should teach package boundaries and call direction without becoming a full student information system.

## POM Design

The generated root `pom.xml` should inherit from Spring Boot directly:

```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.5.16</version>
    <relativePath/>
</parent>
```

It should keep archetype variables for generated coordinates:

```xml
<groupId>${groupId}</groupId>
<artifactId>${artifactId}</artifactId>
<version>${version}</version>
```

The generated POM should include:

- `<java.version>21</java.version>`.
- UTF-8 build and reporting encodings.
- `spring-boot-starter-web`.
- `spring-boot-starter-validation`.
- `spring-boot-starter-data-jpa`.
- `flyway-core`.
- `h2` for local sample execution and tests.
- `org.postgresql:postgresql` with runtime scope as the production database option.
- `org.projectlombok:lombok` with `optional=true`.
- `spring-boot-starter-test`.
- `com.tngtech.archunit:archunit-junit5` for package dependency tests.
- `spring-boot-maven-plugin`.

Do not include CyberMario-specific dependencies by default:

- Spring AI and Spring AI Alibaba.
- MCP SDK.
- Drools.
- Redis.
- Spring Security.
- Tika.
- Guava.
- MapStruct Plus.
- Business search or tool-calling starters.

The generated default configuration should use H2 so `./mvnw test` and local packaging work without external services. PostgreSQL should be present as a runtime dependency and documented as the production database option, but the generated project should not require a running PostgreSQL instance for its default validation path.

## Maven Wrapper and Root Files

The generated project should contain:

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

The implementation should reuse the repository's existing `mvnw`, `mvnw.cmd`, and wrapper property style where practical. Hidden or special root files should be represented according to Maven archetype conventions, such as `__gitignore__` for `.gitignore`, so generated output has the expected file names.

`.gitignore` should ignore target output, IDE files, OS temporary files, logs, and local environment files. It must not ignore wrapper files or source resources that the generated project needs.

`.gitattributes` should define stable line endings:

- Text files use LF by default.
- `mvnw` uses LF.
- `mvnw.cmd` uses CRLF.
- Common binary files are marked binary.

## README Design

The generated `README.md` should describe the generated student-management project rather than the old charging domain.

It should cover:

- The project purpose as a large-monolith light domain architecture sample.
- The fact that the project is a single Maven module.
- The role of each logical package: `start`, `adapter`, `facade`, `application`, `infrastructure`, `common`, and `domain`.
- The dependency direction and forbidden shortcuts.
- The included example flows: student registration and assigning courses.
- Local commands:

```bash
./mvnw test
./mvnw -DskipTests package
./mvnw spring-boot:run
```

The README should be concise and operational. It should not duplicate the full architecture document.

## Archetype Metadata Design

`archetype-metadata.xml` should be updated to include all generated source, resource, test, wrapper, and root project files.

Expected handling:

- Java source and test source remain `filtered=true` and `packaged=true`.
- Root POM, README, `.gitignore`, `.gitattributes`, Maven Wrapper properties, and config files are included with filtering only where archetype variables are required.
- Binary images from the old charging README should be removed unless still referenced by generated documentation. The preferred result is no image dependency in the generated README.
- Old `.iml` or IDE-specific template files should not be generated.

The metadata should preserve archetype variable behavior for `${groupId}`, `${artifactId}`, `${version}`, `${rootArtifactId}`, and `${package}` where applicable.

## Validation Plan

Implementation should start with targeted validation for the archetype module:

```bash
bash ./mvnw -B -ntp -pl egon-cola-archetypes/egon-cola-archetype-light -am test
```

If the full module test is too broad or slow, use the nearest smaller Maven archetype integration validation that still proves project generation.

Generated project validation should confirm:

- `mvnw`, `mvnw.cmd`, and `.mvn/wrapper/maven-wrapper.properties` are present.
- `.gitignore`, `.gitattributes`, and `README.md` are present.
- `maven-wrapper.properties` references Maven 3.9.14.
- Generated package paths use the requested `${package}`.
- No old charging package, class names, README text, or images remain in the generated output.
- Exactly one initial Flyway migration exists for the generated schema.
- The generated project can run tests through Maven.
- ArchUnit tests enforce the intended package dependency boundaries.

Do not start the generated Spring Boot application during validation.

## Design Pattern Consideration

No new business design pattern should be added just to make the template look more advanced.

The generated architecture already uses the relevant structural patterns:

- Ports and adapters style through domain repository interfaces and infrastructure implementations.
- Application service orchestration through `application.manage.*`.
- Domain service placement for business rules that do not belong on a single entity.

The student-management sample does not need additional Strategy, Factory, Abstract Factory, Decorator, State, or Chain of Responsibility abstractions. Direct application orchestration and domain services are clearer and better aligned with the template's teaching goal.

## Risks

- Archetype resource filtering can corrupt Maven placeholders if filtering is applied too broadly.
- Hidden files require archetype-specific naming and metadata handling; otherwise generated `.gitignore` or `.gitattributes` may be missing.
- Wrapper script executable permissions may not survive archetype generation on every platform. The implementation should verify this where possible and report any residual limitation.
- Replacing the charging sample touches many files, so stale class names or documentation references are easy to miss.
- Adding persistence dependencies without a coherent default database setup can make the generated project harder to run. The implementation must keep H2 as the no-external-service default and include only one initial Flyway migration.

## Acceptance Criteria

- `egon-cola-archetype-light` generates a `student-management` sample instead of a charging sample.
- The generated project is one Maven module with the documented package layers.
- The generated POM inherits from `spring-boot-starter-parent:3.5.16`.
- The generated POM uses Java 21 and only generic dependencies needed by the template.
- The generated project includes exactly one initial Flyway migration for the student-management schema.
- The generated project includes Maven Wrapper files for Maven 3.9.14.
- The generated project includes `.gitignore`, `.gitattributes`, and a student-management README.
- Archetype metadata includes the new files and excludes obsolete charging images or IDE files.
- Generated tests validate package boundaries and sample behavior.
- Relevant Maven validation is run and reported honestly.
- No generated application is started.
