# Archetype Clean Architecture Optimization Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Optimize the three current archetypes so generated projects use domain-returning application services, adapter-owned conversion, named constructor-injected Spring Beans, MapStruct Plus converters, and Dubbo3 Triple facade examples.

**Architecture:** Keep each archetype's current shape: light stays single-module, web stays the seven-module organization sample, and service stays the seven-module pure-service evaluation sample. Apply the rules in place: application services return domain models, adapter/facade/controller/MQ classes convert those models outward, infrastructure converts PO/domain with MapStruct Plus, and Dubbo providers live in adapter facade implementation packages.

**Tech Stack:** Java 21, Spring Boot 3.5.16, Maven Archetype Plugin, Lombok `@RequiredArgsConstructor`, MapStruct Plus `1.5.1`, Apache Dubbo `3.3.6`, Spring Data JPA, Flyway, H2, PostgreSQL runtime driver, ArchUnit.

---

## Constraints For Every Task

- Do not start any generated Spring Boot application.
- Do not change existing Flyway migration files outside archetype template resources.
- Do not rename `application.manage`.
- Do not add `command`, `query`, `usecase`, aggregate, factory, or strategy packages.
- Do not convert the light archetype into a multi-module project.
- Do not add native grpc-java services, `.proto` service files, or a standalone gRPC module.
- Commit exactly once at the end of each task after its validation passes.
- Ignore generated `target/` output in git; never stage `target/`.

## File Structure

The implementation modifies only archetype templates, archetype metadata when required for newly generated files, basic archetype verify scripts, and generated README/application configuration text.

### Shared Template Files

- Modify `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/pom.xml`: add MapStruct Plus, Dubbo, Lombok version properties, dependencies, and compiler annotation processors.
- Modify `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/pom.xml`: add shared dependency management and inherited compiler annotation processor configuration.
- Modify `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/pom.xml`: add shared dependency management and inherited compiler annotation processor configuration.
- Create generated `lombok.config` files in all three archetype roots:
  - `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/lombok.config`
  - `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/lombok.config`
  - `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/lombok.config`
- Modify generated `application.yml` files for Dubbo Triple:
  - `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/resources/application.yml`
  - `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-starter/src/main/resources/application.yml`
  - `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-starter/src/main/resources/application.yml`

### Light Archetype Files

- Delete generated view records after usages are removed:
  - `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java/application/manage/student/StudentView.java`
  - `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java/application/manage/teaching/CourseView.java`
- Modify application service contracts and implementations:
  - `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java/application/manage/student/StudentManage.java`
  - `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java/application/manage/student/impl/StudentManageImpl.java`
  - `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java/application/manage/teaching/CourseManage.java`
  - `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java/application/manage/teaching/impl/CourseManageImpl.java`
- Create domain service configuration:
  - `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java/application/config/DomainServiceConfiguration.java`
- Modify adapter entry points and converters:
  - `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java/adapter/controller/student/StudentController.java`
  - `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java/adapter/controller/teaching/CourseController.java`
  - `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java/adapter/facade/impl/StudentManagementFacadeImpl.java`
  - `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java/adapter/convertor/StudentAdapterConverter.java`
  - `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java/adapter/convertor/CourseAdapterConverter.java`
- Modify infrastructure converters and repositories:
  - `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java/infrastructure/repo/student/converter/StudentPoConverter.java`
  - `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java/infrastructure/repo/teaching/converter/CoursePoConverter.java`
  - `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java/infrastructure/repo/student/impl/StudentRepositoryImpl.java`
  - `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java/infrastructure/repo/teaching/impl/CourseRepositoryImpl.java`
- Modify DTO classes if MapStruct Plus needs setters:
  - `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java/facade/dto/StudentDTO.java`
  - `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java/facade/dto/CourseDTO.java`
- Modify tests and metadata:
  - `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/META-INF/maven/archetype-metadata.xml`
  - `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/test/java/application/StudentManagementFlowTest.java`
  - `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/test/java/ArchitectureDependencyTest.java`
  - `egon-cola-archetypes/egon-cola-archetype-light/src/test/resources/projects/basic/verify.groovy`

### Web Archetype Files

- Delete generated view records after usages are removed:
  - `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-application/src/main/java/application/manage/user/UserView.java`
  - `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-application/src/main/java/application/manage/teaching/SchoolClassView.java`
- Modify application contracts and implementations:
  - `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-application/src/main/java/application/manage/user/UserManage.java`
  - `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-application/src/main/java/application/manage/user/impl/UserManageImpl.java`
  - `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-application/src/main/java/application/manage/teaching/SchoolClassManage.java`
  - `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-application/src/main/java/application/manage/teaching/impl/SchoolClassManageImpl.java`
- Create domain service configuration:
  - `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-application/src/main/java/application/config/DomainServiceConfiguration.java`
- Modify adapter controllers, facades, converters, handler:
  - `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-adapter/src/main/java/adapter/controller/user/UserController.java`
  - `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-adapter/src/main/java/adapter/controller/teaching/SchoolClassController.java`
  - `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-adapter/src/main/java/adapter/facade/user/UserFacadeImpl.java`
  - `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-adapter/src/main/java/adapter/facade/teaching/SchoolClassFacadeImpl.java`
  - `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-adapter/src/main/java/adapter/convertor/UserAdapterConverter.java`
  - `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-adapter/src/main/java/adapter/convertor/SchoolClassAdapterConverter.java`
  - `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-adapter/src/main/java/adapter/handler/GlobalExceptionHandler.java`
- Modify infrastructure converters and repositories:
  - `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/main/java/infrastructure/repo/user/converter/UserPoConverter.java`
  - `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/main/java/infrastructure/repo/teaching/converter/SchoolClassPoConverter.java`
  - `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/main/java/infrastructure/repo/user/impl/UserRepositoryImpl.java`
  - `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/main/java/infrastructure/repo/teaching/impl/SchoolClassRepositoryImpl.java`
- Modify DTO classes if MapStruct Plus needs setters:
  - `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-facade/src/main/java/facade/dto/user/UserDTO.java`
  - `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-facade/src/main/java/facade/dto/teaching/SchoolClassDTO.java`
- Modify module POMs, tests, and metadata:
  - `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-adapter/pom.xml`
  - `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-application/pom.xml`
  - `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/pom.xml`
  - `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-starter/pom.xml`
  - `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/META-INF/maven/archetype-metadata.xml`
  - `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-starter/src/test/java/starter/OrganizationFlowTest.java`
  - `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-starter/src/test/java/starter/ArchitectureDependencyTest.java`
  - `egon-cola-archetypes/egon-cola-archetype-web/src/test/resources/projects/basic/verify.groovy`

### Service Archetype Files

- Delete generated view records after usages are removed:
  - `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-application/src/main/java/application/view/course/CourseView.java`
  - `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-application/src/main/java/application/view/examing/ExamResultView.java`
- Modify application contracts and implementations:
  - `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-application/src/main/java/application/manage/course/CourseManage.java`
  - `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-application/src/main/java/application/manage/course/impl/CourseManageImpl.java`
  - `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-application/src/main/java/application/manage/examing/ExamManage.java`
  - `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-application/src/main/java/application/manage/examing/impl/ExamManageImpl.java`
- Create domain service configuration:
  - `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-application/src/main/java/application/config/DomainServiceConfiguration.java`
- Modify adapter facades, MQ consumer, converters, handler:
  - `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-adapter/src/main/java/adapter/facade/impl/CourseFacadeImpl.java`
  - `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-adapter/src/main/java/adapter/facade/impl/ExamResultFacadeImpl.java`
  - `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-adapter/src/main/java/adapter/mq/ExamResultMessageConsumer.java`
  - `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-adapter/src/main/java/adapter/convertor/CourseAdapterConvertor.java`
  - `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-adapter/src/main/java/adapter/convertor/ExamResultAdapterConvertor.java`
  - `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-adapter/src/main/java/adapter/handler/ServiceExceptionHandler.java`
- Modify infrastructure converters and repositories:
  - `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/main/java/infrastructure/repo/course/converter/CourseConverter.java`
  - `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/main/java/infrastructure/repo/examing/converter/ExamResultConverter.java`
  - `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/main/java/infrastructure/repo/course/impl/CourseRepositoryImpl.java`
  - `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/main/java/infrastructure/repo/examing/impl/ExamResultRepositoryImpl.java`
- Modify DTO/message classes if MapStruct Plus needs setters:
  - `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-facade/src/main/java/facade/dto/course/CourseDTO.java`
  - `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-facade/src/main/java/facade/dto/examing/ExamResultDTO.java`
  - `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-adapter/src/main/java/adapter/dto/ExamResultMessage.java`
- Modify module POMs, tests, and metadata:
  - `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-adapter/pom.xml`
  - `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-application/pom.xml`
  - `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/pom.xml`
  - `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-starter/pom.xml`
  - `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/META-INF/maven/archetype-metadata.xml`
  - `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-starter/src/test/java/starter/EvaluationFlowTest.java`
  - `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-starter/src/test/java/starter/ServiceArchitectureDependencyTest.java`
  - `egon-cola-archetypes/egon-cola-archetype-service/src/test/resources/projects/basic/verify.groovy`

## Task 1: Shared Dependency, Compiler, Lombok, Dubbo Config, And Verify Gates

**Files:**
- Modify: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/pom.xml`
- Modify: `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/pom.xml`
- Modify: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/pom.xml`
- Modify: `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-adapter/pom.xml`
- Modify: `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-application/pom.xml`
- Modify: `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/pom.xml`
- Modify: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-adapter/pom.xml`
- Modify: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-application/pom.xml`
- Modify: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/pom.xml`
- Create: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/lombok.config`
- Create: `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/lombok.config`
- Create: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/lombok.config`
- Modify: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/resources/application.yml`
- Modify: `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-starter/src/main/resources/application.yml`
- Modify: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-starter/src/main/resources/application.yml`
- Modify: `egon-cola-archetypes/egon-cola-archetype-light/src/test/resources/projects/basic/verify.groovy`
- Modify: `egon-cola-archetypes/egon-cola-archetype-web/src/test/resources/projects/basic/verify.groovy`
- Modify: `egon-cola-archetypes/egon-cola-archetype-service/src/test/resources/projects/basic/verify.groovy`

- [ ] **Step 1: Add failing generated-output assertions**

Add these assertions to the light `verify.groovy` after the existing POM assertions:

```groovy
assert pom.contains("<mapstruct-plus.version>1.5.1</mapstruct-plus.version>")
assert pom.contains("<dubbo.version>3.3.6</dubbo.version>")
assert pom.contains("<artifactId>mapstruct-plus-spring-boot-starter</artifactId>")
assert pom.contains("<artifactId>dubbo-spring-boot-starter</artifactId>")
assert pom.contains("<artifactId>mapstruct-plus-processor</artifactId>")
assertFile("lombok.config").text.contains("lombok.copyableAnnotations += org.springframework.beans.factory.annotation.Qualifier")

def applicationYaml = assertFile("src/main/resources/application.yml").text
assert applicationYaml.contains("dubbo:")
assert applicationYaml.contains("name: tri")
assert applicationYaml.contains('${DUBBO_REGISTRY_ADDRESS:N/A}')
```

Add these helpers and assertions near the dependency assertions in the web `verify.groovy`:

```groovy
def assertRootPomContains = { token ->
    assert rootPomText.contains(token): "Expected root pom to contain ${token}"
}

assertRootPomContains("<mapstruct-plus.version>1.5.1</mapstruct-plus.version>")
assertRootPomContains("<dubbo.version>3.3.6</dubbo.version>")
assertRootPomContains("<artifactId>dubbo-bom</artifactId>")
assertRootPomContains("<artifactId>mapstruct-plus-spring-boot-starter</artifactId>")
assertRootPomContains("<artifactId>mapstruct-plus-processor</artifactId>")
assertFile("lombok.config").text.contains("lombok.copyableAnnotations += org.springframework.beans.factory.annotation.Qualifier")

assertDependency(adapterDependencies, "dubbo-spring-boot-starter")
assertDependency(adapterDependencies, "mapstruct-plus-spring-boot-starter")
assertDependency(applicationDependencies, "lombok")
assertDependency(infrastructureDependencies, "mapstruct-plus-spring-boot-starter")

def webApplicationYaml = assertFile("student-management-organization-starter/src/main/resources/application.yml").text
assert webApplicationYaml.contains("dubbo:")
assert webApplicationYaml.contains("name: tri")
assert webApplicationYaml.contains('${DUBBO_REGISTRY_ADDRESS:N/A}')
```

Add these assertions to the service `verify.groovy` after the root POM assertions:

```groovy
assert pom.contains("<mapstruct-plus.version>1.5.1</mapstruct-plus.version>")
assert pom.contains("<dubbo.version>3.3.6</dubbo.version>")
assert pom.contains("<artifactId>dubbo-bom</artifactId>")
assert pom.contains("<artifactId>mapstruct-plus-spring-boot-starter</artifactId>")
assert pom.contains("<artifactId>mapstruct-plus-processor</artifactId>")
assertFile("lombok.config").text.contains("lombok.copyableAnnotations += org.springframework.beans.factory.annotation.Qualifier")

def adapterPomText = assertFile("student-management-evaluation-adapter/pom.xml").text
assert adapterPomText.contains("<artifactId>dubbo-spring-boot-starter</artifactId>")
assert adapterPomText.contains("<artifactId>mapstruct-plus-spring-boot-starter</artifactId>")

def applicationPomText = assertFile("student-management-evaluation-application/pom.xml").text
assert applicationPomText.contains("<artifactId>lombok</artifactId>")

def infrastructurePomText = assertFile("student-management-evaluation-infrastructure/pom.xml").text
assert infrastructurePomText.contains("<artifactId>mapstruct-plus-spring-boot-starter</artifactId>")

def serviceApplicationYaml = assertFile("student-management-evaluation-starter/src/main/resources/application.yml").text
assert serviceApplicationYaml.contains("dubbo:")
assert serviceApplicationYaml.contains("name: tri")
assert serviceApplicationYaml.contains('${DUBBO_REGISTRY_ADDRESS:N/A}')
```

- [ ] **Step 2: Run one archetype test to verify the new checks fail**

Run:

```bash
./mvnw -pl egon-cola-archetypes/egon-cola-archetype-light -am test
```

Expected: FAIL in `verify.groovy` with an assertion mentioning `mapstruct-plus.version`, `dubbo.version`, or `lombok.config`.

- [ ] **Step 3: Add shared dependency management and compiler processors**

In the light generated `pom.xml`, add these properties under `<archunit.version>`:

```xml
        <lombok.version>1.18.38</lombok.version>
        <mapstruct-plus.version>1.5.1</mapstruct-plus.version>
        <dubbo.version>3.3.6</dubbo.version>
```

Add these dependencies to the light generated POM dependencies:

```xml
        <dependency>
            <groupId>io.github.linpeilie</groupId>
            <artifactId>mapstruct-plus-spring-boot-starter</artifactId>
            <version>${mapstruct-plus.version}</version>
        </dependency>
        <dependency>
            <groupId>org.apache.dubbo</groupId>
            <artifactId>dubbo-spring-boot-starter</artifactId>
            <version>${dubbo.version}</version>
        </dependency>
```

Add this compiler plugin before the existing Spring Boot Maven plugin in the light generated POM:

```xml
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <configuration>
                    <parameters>true</parameters>
                    <annotationProcessorPaths>
                        <path>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                            <version>${lombok.version}</version>
                        </path>
                        <path>
                            <groupId>io.github.linpeilie</groupId>
                            <artifactId>mapstruct-plus-processor</artifactId>
                            <version>${mapstruct-plus.version}</version>
                        </path>
                    </annotationProcessorPaths>
                </configuration>
            </plugin>
```

In the web and service generated root POMs, add these properties:

```xml
        <lombok.version>1.18.38</lombok.version>
        <mapstruct-plus.version>1.5.1</mapstruct-plus.version>
        <dubbo.version>3.3.6</dubbo.version>
```

Add these dependency-management entries inside the generated web and service root POM `<dependencyManagement><dependencies>` sections:

```xml
            <dependency>
                <groupId>org.apache.dubbo</groupId>
                <artifactId>dubbo-bom</artifactId>
                <version>${dubbo.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
            <dependency>
                <groupId>io.github.linpeilie</groupId>
                <artifactId>mapstruct-plus-spring-boot-starter</artifactId>
                <version>${mapstruct-plus.version}</version>
            </dependency>
            <dependency>
                <groupId>org.projectlombok</groupId>
                <artifactId>lombok</artifactId>
                <version>${lombok.version}</version>
            </dependency>
```

Add this inherited build plugin block to the generated web and service root POMs:

```xml
    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <configuration>
                    <parameters>true</parameters>
                    <annotationProcessorPaths>
                        <path>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                            <version>${lombok.version}</version>
                        </path>
                        <path>
                            <groupId>io.github.linpeilie</groupId>
                            <artifactId>mapstruct-plus-processor</artifactId>
                            <version>${mapstruct-plus.version}</version>
                        </path>
                    </annotationProcessorPaths>
                </configuration>
            </plugin>
        </plugins>
    </build>
```

- [ ] **Step 4: Add generated module dependencies**

Add this dependency to each generated adapter module POM in web and service:

```xml
        <dependency>
            <groupId>org.apache.dubbo</groupId>
            <artifactId>dubbo-spring-boot-starter</artifactId>
        </dependency>
        <dependency>
            <groupId>io.github.linpeilie</groupId>
            <artifactId>mapstruct-plus-spring-boot-starter</artifactId>
        </dependency>
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <scope>provided</scope>
        </dependency>
```

Add this dependency to each generated application module POM in web and service:

```xml
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <scope>provided</scope>
        </dependency>
```

Add these dependencies to each generated infrastructure module POM in web and service:

```xml
        <dependency>
            <groupId>io.github.linpeilie</groupId>
            <artifactId>mapstruct-plus-spring-boot-starter</artifactId>
        </dependency>
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <scope>provided</scope>
        </dependency>
```

- [ ] **Step 5: Add generated Lombok config and Dubbo Triple YAML**

Create all three generated `lombok.config` files with this exact content:

```properties
config.stopBubbling = true
lombok.copyableAnnotations += org.springframework.beans.factory.annotation.Qualifier
lombok.addLombokGeneratedAnnotation = true
```

Add this YAML block to each generated `application.yml` under the existing Spring application config:

```yaml
dubbo:
  application:
    name: ${spring.application.name}
  registry:
    address: ${DUBBO_REGISTRY_ADDRESS:N/A}
  protocol:
    name: tri
    port: ${DUBBO_PORT:50051}
  provider:
    timeout: 3000
    retries: 0
```

- [ ] **Step 6: Run dependency/config verification**

Run:

```bash
./mvnw -pl egon-cola-archetypes/egon-cola-archetype-light -am test
./mvnw -pl egon-cola-archetypes/egon-cola-archetype-web -am test
./mvnw -pl egon-cola-archetypes/egon-cola-archetype-service -am test
```

Expected: `BUILD SUCCESS` for all three commands. If dependency resolution fails because a pinned Dubbo or MapStruct Plus version is unavailable, stop and replace only the unavailable version with the nearest available Maven Central version, then rerun the same three commands.

- [ ] **Step 7: Commit**

```bash
git add \
  egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/pom.xml \
  egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/pom.xml \
  egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/pom.xml \
  egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-adapter/pom.xml \
  egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-application/pom.xml \
  egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/pom.xml \
  egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-adapter/pom.xml \
  egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-application/pom.xml \
  egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/pom.xml \
  egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/lombok.config \
  egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/lombok.config \
  egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/lombok.config \
  egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/resources/application.yml \
  egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-starter/src/main/resources/application.yml \
  egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-starter/src/main/resources/application.yml \
  egon-cola-archetypes/egon-cola-archetype-light/src/test/resources/projects/basic/verify.groovy \
  egon-cola-archetypes/egon-cola-archetype-web/src/test/resources/projects/basic/verify.groovy \
  egon-cola-archetypes/egon-cola-archetype-service/src/test/resources/projects/basic/verify.groovy
git commit -m "feat(archetype): add clean architecture dependency baseline"
```

## Task 2: Light Archetype Boundary, Bean, MapStruct Plus, And Dubbo Provider

**Files:**
- Modify: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java/application/manage/student/StudentManage.java`
- Modify: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java/application/manage/student/impl/StudentManageImpl.java`
- Modify: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java/application/manage/teaching/CourseManage.java`
- Modify: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java/application/manage/teaching/impl/CourseManageImpl.java`
- Create: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java/application/config/DomainServiceConfiguration.java`
- Delete: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java/application/manage/student/StudentView.java`
- Delete: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java/application/manage/teaching/CourseView.java`
- Modify: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java/adapter/controller/student/StudentController.java`
- Modify: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java/adapter/controller/teaching/CourseController.java`
- Modify: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java/adapter/facade/impl/StudentManagementFacadeImpl.java`
- Modify: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java/adapter/convertor/StudentAdapterConverter.java`
- Modify: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java/adapter/convertor/CourseAdapterConverter.java`
- Modify: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java/infrastructure/repo/student/converter/StudentPoConverter.java`
- Modify: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java/infrastructure/repo/teaching/converter/CoursePoConverter.java`
- Modify: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java/infrastructure/repo/student/impl/StudentRepositoryImpl.java`
- Modify: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java/infrastructure/repo/teaching/impl/CourseRepositoryImpl.java`
- Modify: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java/adapter/handler/GlobalExceptionHandler.java`
- Modify: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java/facade/dto/StudentDTO.java`
- Modify: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java/facade/dto/CourseDTO.java`
- Modify: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/META-INF/maven/archetype-metadata.xml`
- Modify: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/test/java/application/StudentManagementFlowTest.java`
- Modify: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/test/java/ArchitectureDependencyTest.java`
- Modify: `egon-cola-archetypes/egon-cola-archetype-light/src/test/resources/projects/basic/verify.groovy`

- [ ] **Step 1: Add failing light verify assertions**

Add these assertions to the light `verify.groovy` after existing Java file assertions:

```groovy
assertFile("src/main/java/it/pkg/application/config/DomainServiceConfiguration.java")
assert !new File(generatedProjectDir, "src/main/java/it/pkg/application/manage/student/StudentView.java").exists()
assert !new File(generatedProjectDir, "src/main/java/it/pkg/application/manage/teaching/CourseView.java").exists()

def studentManageText = assertFile("src/main/java/it/pkg/application/manage/student/StudentManage.java").text
assert studentManageText.contains("Student register(String name, String email)")
assert studentManageText.contains("Student getById(String studentId)")
assert !studentManageText.contains("StudentView")

def courseManageText = assertFile("src/main/java/it/pkg/application/manage/teaching/CourseManage.java").text
assert courseManageText.contains("Course create(String name, String description)")
assert courseManageText.contains("Course getById(String courseId)")
assert !courseManageText.contains("CourseView")

def lightFacadeText = assertFile("src/main/java/it/pkg/adapter/facade/impl/StudentManagementFacadeImpl.java").text
assert lightFacadeText.contains("@DubboService")
assert lightFacadeText.contains("@RequiredArgsConstructor")
assert lightFacadeText.contains("@Qualifier(\"studentManage\")")
assert lightFacadeText.contains("@Qualifier(\"studentAdapterConverter\")")

def lightConverterText = assertFile("src/main/java/it/pkg/adapter/convertor/StudentAdapterConverter.java").text
assert lightConverterText.contains("io.github.linpeilie.Converter")
assert lightConverterText.contains("@Component(\"studentAdapterConverter\")")
assert !lightConverterText.contains("static StudentDTO")

def allApplicationJava = []
new File(generatedProjectDir, "src/main/java/it/pkg/application").eachFileRecurse { file ->
    if (file.isFile() && file.name.endsWith(".java")) {
        allApplicationJava << file
    }
}
assert allApplicationJava.every { !it.text.contains("View") }
assert allApplicationJava.every { !it.text.contains("facade.dto") }
assert allApplicationJava.every { !it.text.contains("common.response") }
```

- [ ] **Step 2: Run the light archetype test and verify it fails on light boundary assertions**

Run:

```bash
./mvnw -pl egon-cola-archetypes/egon-cola-archetype-light -am test
```

Expected: FAIL with an assertion mentioning `StudentView`, `CourseView`, `@DubboService`, `studentAdapterConverter`, or a domain return signature.

- [ ] **Step 3: Change light application services to return domain models**

Replace `StudentManage.java` with:

```java
package ${package}.application.manage.student;

import ${package}.domain.student.model.Student;

public interface StudentManage {
    Student register(String name, String email);

    Student getById(String studentId);
}
```

Replace `CourseManage.java` with:

```java
package ${package}.application.manage.teaching;

import ${package}.domain.teaching.model.Course;

public interface CourseManage {
    Course create(String name, String description);

    Course getById(String courseId);

    void assignCourse(String studentId, String courseId);
}
```

Update `StudentManageImpl.java` to this dependency and return shape:

```java
@Service("studentManage")
@RequiredArgsConstructor
public class StudentManageImpl implements StudentManage {

    @Qualifier("studentRepositoryImpl")
    private final StudentRepository studentRepository;

    @Qualifier("studentDomainService")
    private final StudentDomainService studentDomainService;

    @Override
    @Transactional
    public Student register(String name, String email) {
        if (studentRepository.existsByEmail(email)) {
            throw new BizException(ErrorCodes.STUDENT_EMAIL_DUPLICATED, "student email already exists");
        }
        Student student = studentDomainService.register(IdGenerator.nextId(), name, email);
        try {
            return studentRepository.save(student);
        } catch (DataIntegrityViolationException exception) {
            throw new BizException(ErrorCodes.STUDENT_EMAIL_DUPLICATED, "student email already exists");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Student getById(String studentId) {
        return studentRepository.findById(studentId)
                .orElseThrow(() -> new NotFoundException(ErrorCodes.STUDENT_NOT_FOUND, "student not found"));
    }
}
```

Update `CourseManageImpl.java` to this dependency and return shape:

```java
@Service("courseManage")
@RequiredArgsConstructor
public class CourseManageImpl implements CourseManage {

    @Qualifier("courseRepositoryImpl")
    private final CourseRepository courseRepository;

    @Qualifier("studentRepositoryImpl")
    private final StudentRepository studentRepository;

    @Qualifier("courseDomainService")
    private final CourseDomainService courseDomainService;

    @Qualifier("studentDomainService")
    private final StudentDomainService studentDomainService;

    @Override
    @Transactional
    public Course create(String name, String description) {
        Course course = courseDomainService.create(IdGenerator.nextId(), name, description);
        return courseRepository.save(course);
    }

    @Override
    @Transactional(readOnly = true)
    public Course getById(String courseId) {
        return courseRepository.findById(courseId)
                .orElseThrow(() -> new NotFoundException(ErrorCodes.COURSE_NOT_FOUND, "course not found"));
    }

    @Override
    @Transactional
    public void assignCourse(String studentId, String courseId) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new NotFoundException(ErrorCodes.STUDENT_NOT_FOUND, "student not found"));
        courseRepository.findById(courseId)
                .orElseThrow(() -> new NotFoundException(ErrorCodes.COURSE_NOT_FOUND, "course not found"));
        studentRepository.save(studentDomainService.assignCourse(student, courseId));
    }
}
```

Add `DomainServiceConfiguration.java`:

```java
package ${package}.application.config;

import ${package}.domain.student.service.StudentDomainService;
import ${package}.domain.teaching.service.CourseDomainService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DomainServiceConfiguration {

    @Bean("studentDomainService")
    public StudentDomainService studentDomainService() {
        return new StudentDomainService();
    }

    @Bean("courseDomainService")
    public CourseDomainService courseDomainService() {
        return new CourseDomainService();
    }
}
```

- [ ] **Step 4: Convert light adapter and infrastructure converters to MapStruct Plus Spring Beans**

Replace static adapter converter usage with bean methods. `StudentAdapterConverter.java` must use this shape:

```java
@Component("studentAdapterConverter")
@RequiredArgsConstructor
public class StudentAdapterConverter {

    @Qualifier("converter")
    private final Converter converter;

    public StudentDTO toDto(Student student) {
        StudentDTO dto = converter.convert(student, StudentDTO.class);
        dto.setStatus(student.getStatus().name());
        return dto;
    }
}
```

`CourseAdapterConverter.java` must use this shape:

```java
@Component("courseAdapterConverter")
@RequiredArgsConstructor
public class CourseAdapterConverter {

    @Qualifier("converter")
    private final Converter converter;

    public CourseDTO toDto(Course course) {
        return converter.convert(course, CourseDTO.class);
    }
}
```

Convert `StudentDTO.java` and `CourseDTO.java` from records to classes with no-arg constructors and setters:

```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@AutoMapper(target = Student.class, reverseConvertGenerate = false)
public class StudentDTO implements Serializable {
    private String id;
    private String name;
    private String email;
    private String status;
    private List<String> courseIds;
}
```

```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@AutoMapper(target = Course.class, reverseConvertGenerate = false)
public class CourseDTO implements Serializable {
    private String id;
    private String name;
    private String description;
}
```

Use the same MapStruct Plus pattern for `StudentPoConverter.java` and `CoursePoConverter.java`, keeping existing manual relationship handling for `StudentCoursePo` because that mapping is not a flat PO/domain copy.

- [ ] **Step 5: Convert light entry points and repositories to named constructor-injected Beans**

Update controllers to use `@RequiredArgsConstructor`, named stereotypes, final fields, and `@Qualifier`:

```java
@RestController("studentController")
@RequestMapping("/students")
@RequiredArgsConstructor
public class StudentController {

    @Qualifier("studentManage")
    private final StudentManage studentManage;

    @Qualifier("studentAdapterConverter")
    private final StudentAdapterConverter studentAdapterConverter;
}
```

```java
@RestController("courseController")
@RequestMapping("/courses")
@RequiredArgsConstructor
public class CourseController {

    @Qualifier("courseManage")
    private final CourseManage courseManage;

    @Qualifier("courseAdapterConverter")
    private final CourseAdapterConverter courseAdapterConverter;
}
```

Update `StudentManagementFacadeImpl.java` to expose Dubbo:

```java
@DubboService(
        interfaceClass = StudentManagementFacade.class,
        version = "1.0.0",
        group = "student-management"
)
@RequiredArgsConstructor
public class StudentManagementFacadeImpl implements StudentManagementFacade {

    @Qualifier("studentManage")
    private final StudentManage studentManage;

    @Qualifier("courseManage")
    private final CourseManage courseManage;

    @Qualifier("studentAdapterConverter")
    private final StudentAdapterConverter studentAdapterConverter;

    @Qualifier("courseAdapterConverter")
    private final CourseAdapterConverter courseAdapterConverter;
}
```

Update repository implementations and handler stereotypes:

```java
@Repository("studentRepositoryImpl")
@RequiredArgsConstructor
public class StudentRepositoryImpl implements StudentRepository {
    @Qualifier("studentJpaRepository")
    private final StudentJpaRepository studentJpaRepository;
    @Qualifier("studentCourseJpaRepository")
    private final StudentCourseJpaRepository studentCourseJpaRepository;
    @Qualifier("studentPoConverter")
    private final StudentPoConverter studentPoConverter;
}
```

```java
@Repository("courseRepositoryImpl")
@RequiredArgsConstructor
public class CourseRepositoryImpl implements CourseRepository {
    @Qualifier("courseJpaRepository")
    private final CourseJpaRepository courseJpaRepository;
    @Qualifier("coursePoConverter")
    private final CoursePoConverter coursePoConverter;
}
```

```java
@RestControllerAdvice("globalExceptionHandler")
public class GlobalExceptionHandler {
}
```

- [ ] **Step 6: Remove light View files and update metadata/tests**

Delete:

```text
egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java/application/manage/student/StudentView.java
egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java/application/manage/teaching/CourseView.java
```

Remove both deleted files from `archetype-metadata.xml` if they are listed explicitly.

Update `StudentManagementFlowTest.java` imports and variables:

```java
import ${package}.domain.student.model.Student;
import ${package}.domain.teaching.model.Course;
```

Use domain getters in assertions:

```java
Student student = studentManage.register("Mario", "mario@example.com");
Course course = courseManage.create("Architecture", "Large monolith light domain architecture");
courseManage.assignCourse(student.getId(), course.getId());
Student saved = studentManage.getById(student.getId());
assertThat(saved.getCourseIds()).contains(course.getId());
```

Update `ArchitectureDependencyTest.java` so `application` is still forbidden from depending on `..facade..`, `..adapter..`, `..infrastructure..`, and `..common.response..`.

- [ ] **Step 7: Run light validation**

Run:

```bash
./mvnw -pl egon-cola-archetypes/egon-cola-archetype-light -am test
rg -n "StudentView|CourseView|application\\.view|import .*facade\\.dto|import .*common\\.response" egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java/application || true
rg -n "@Autowired|public void set[A-Z].*\\(" egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java || true
```

Expected:

```text
./mvnw command: BUILD SUCCESS
rg commands: no matches
```

- [ ] **Step 8: Commit**

```bash
git add egon-cola-archetypes/egon-cola-archetype-light
git commit -m "feat(archetype-light): align clean architecture details"
```

## Task 3: Web Archetype Boundary, Bean, MapStruct Plus, And Dubbo Providers

**Files:** Use the web files listed in the File Structure section.

- [ ] **Step 1: Add failing web verify assertions**

Add these assertions to web `verify.groovy` after existing generated source assertions:

```groovy
assertFile("student-management-organization-application/src/main/java/it/pkg/application/config/DomainServiceConfiguration.java")
assert !new File(projectDir, "student-management-organization-application/src/main/java/it/pkg/application/manage/user/UserView.java").exists()
assert !new File(projectDir, "student-management-organization-application/src/main/java/it/pkg/application/manage/teaching/SchoolClassView.java").exists()

def userManageText = assertFile("student-management-organization-application/src/main/java/it/pkg/application/manage/user/UserManage.java").text
assert userManageText.contains("User create(String name, String email)")
assert userManageText.contains("User getById(String userId)")
assert !userManageText.contains("UserView")

def schoolClassManageText = assertFile("student-management-organization-application/src/main/java/it/pkg/application/manage/teaching/SchoolClassManage.java").text
assert schoolClassManageText.contains("SchoolClass create(String name, String gradeName)")
assert !schoolClassManageText.contains("SchoolClassView")

def userFacadeText = assertFile("student-management-organization-adapter/src/main/java/it/pkg/adapter/facade/user/UserFacadeImpl.java").text
assert userFacadeText.contains("@DubboService")
assert userFacadeText.contains("@Qualifier(\"userManage\")")
assert userFacadeText.contains("@Qualifier(\"userAdapterConverter\")")

def schoolClassFacadeText = assertFile("student-management-organization-adapter/src/main/java/it/pkg/adapter/facade/teaching/SchoolClassFacadeImpl.java").text
assert schoolClassFacadeText.contains("@DubboService")
assert schoolClassFacadeText.contains("@Qualifier(\"schoolClassManage\")")
assert schoolClassFacadeText.contains("@Qualifier(\"schoolClassAdapterConverter\")")

def applicationJava = []
new File(projectDir, "student-management-organization-application/src/main/java").eachFileRecurse { file ->
    if (file.isFile() && file.name.endsWith(".java")) {
        applicationJava << file
    }
}
assert applicationJava.every { !it.text.contains("View") }
assert applicationJava.every { !it.text.contains("facade.dto") }
assert applicationJava.every { !it.text.contains("common.response") }
```

- [ ] **Step 2: Run the web archetype test and verify it fails on web boundary assertions**

Run:

```bash
./mvnw -pl egon-cola-archetypes/egon-cola-archetype-web -am test
```

Expected: FAIL with an assertion mentioning `UserView`, `SchoolClassView`, `@DubboService`, or a domain return signature.

- [ ] **Step 3: Change web application services to return domain models**

Replace `UserManage.java` with:

```java
package ${package}.application.manage.user;

import ${package}.domain.entities.user.User;

public interface UserManage {
    User create(String name, String email);

    User getById(String userId);
}
```

Replace `SchoolClassManage.java` with:

```java
package ${package}.application.manage.teaching;

import ${package}.domain.entities.teaching.SchoolClass;

public interface SchoolClassManage {
    SchoolClass create(String name, String gradeName);

    void assignUser(String userId, String schoolClassId);
}
```

Update `UserManageImpl.java` to use named constructor injection and return `User`:

```java
@Service("userManage")
@RequiredArgsConstructor
public class UserManageImpl implements UserManage {

    @Qualifier("userRepositoryImpl")
    private final UserRepository userRepository;

    @Qualifier("userDomainService")
    private final UserDomainService userDomainService;

    @Override
    @Transactional
    public User create(String name, String email) {
        if (userRepository.existsByEmail(email)) {
            throw new BizException(ErrorCodes.USER_EMAIL_DUPLICATED, "user email already exists");
        }
        User user = userDomainService.create(IdGenerator.nextId(), name, email);
        return userRepository.save(user);
    }

    @Override
    @Transactional(readOnly = true)
    public User getById(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(ErrorCodes.USER_NOT_FOUND, "user not found"));
    }
}
```

Update `SchoolClassManageImpl.java` to use named constructor injection and return `SchoolClass`:

```java
@Service("schoolClassManage")
@RequiredArgsConstructor
public class SchoolClassManageImpl implements SchoolClassManage {

    @Qualifier("schoolClassRepositoryImpl")
    private final SchoolClassRepository schoolClassRepository;

    @Qualifier("userRepositoryImpl")
    private final UserRepository userRepository;

    @Qualifier("schoolClassDomainService")
    private final SchoolClassDomainService schoolClassDomainService;

    @Qualifier("userDomainService")
    private final UserDomainService userDomainService;

    @Override
    @Transactional
    public SchoolClass create(String name, String gradeName) {
        SchoolClass schoolClass = schoolClassDomainService.create(IdGenerator.nextId(), name, gradeName);
        return schoolClassRepository.save(schoolClass);
    }
}
```

Add `DomainServiceConfiguration.java`:

```java
package ${package}.application.config;

import ${package}.domain.service.teaching.SchoolClassDomainService;
import ${package}.domain.service.user.UserDomainService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DomainServiceConfiguration {

    @Bean("userDomainService")
    public UserDomainService userDomainService() {
        return new UserDomainService();
    }

    @Bean("schoolClassDomainService")
    public SchoolClassDomainService schoolClassDomainService() {
        return new SchoolClassDomainService();
    }
}
```

- [ ] **Step 4: Convert web adapter/infrastructure converters and DTO targets**

Use this bean shape for `UserAdapterConverter.java`:

```java
@Component("userAdapterConverter")
@RequiredArgsConstructor
public class UserAdapterConverter {

    @Qualifier("converter")
    private final Converter converter;

    public UserDTO toDto(User user) {
        UserDTO dto = converter.convert(user, UserDTO.class);
        dto.setStatus(user.getStatus().name());
        return dto;
    }
}
```

Use this bean shape for `SchoolClassAdapterConverter.java`:

```java
@Component("schoolClassAdapterConverter")
@RequiredArgsConstructor
public class SchoolClassAdapterConverter {

    @Qualifier("converter")
    private final Converter converter;

    public SchoolClassDTO toDto(SchoolClass schoolClass) {
        return converter.convert(schoolClass, SchoolClassDTO.class);
    }
}
```

Convert response DTO records into MapStruct Plus targets:

```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@AutoMapper(target = User.class, reverseConvertGenerate = false)
public class UserDTO implements Serializable {
    private String id;
    private String name;
    private String email;
    private String status;
    private List<String> schoolClassIds;
}
```

```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@AutoMapper(target = SchoolClass.class, reverseConvertGenerate = false)
public class SchoolClassDTO implements Serializable {
    private String id;
    private String name;
    private String gradeName;
    private List<String> userIds;
}
```

Convert `UserPoConverter.java` and `SchoolClassPoConverter.java` to named Spring Beans using `Converter converter`, while keeping manual code for `SchoolClassUserPo` collection mapping.

- [ ] **Step 5: Convert web entry points, facades, repositories, and handler to named Beans**

Use `@RestController("userController")`, `@RestController("schoolClassController")`, `@RestControllerAdvice("globalExceptionHandler")`, `@Repository("userRepositoryImpl")`, `@Repository("schoolClassRepositoryImpl")`, `@Component("userPoConverter")`, and `@Component("schoolClassPoConverter")`.

Update `UserFacadeImpl.java` to Dubbo:

```java
@DubboService(
        interfaceClass = UserFacade.class,
        version = "1.0.0",
        group = "user"
)
@Validated
@RequiredArgsConstructor
public class UserFacadeImpl implements UserFacade {

    @Qualifier("userManage")
    private final UserManage userManage;

    @Qualifier("userAdapterConverter")
    private final UserAdapterConverter userAdapterConverter;
}
```

Update `SchoolClassFacadeImpl.java` to Dubbo:

```java
@DubboService(
        interfaceClass = SchoolClassFacade.class,
        version = "1.0.0",
        group = "school-class"
)
@Validated
@RequiredArgsConstructor
public class SchoolClassFacadeImpl implements SchoolClassFacade {

    @Qualifier("schoolClassManage")
    private final SchoolClassManage schoolClassManage;

    @Qualifier("schoolClassAdapterConverter")
    private final SchoolClassAdapterConverter schoolClassAdapterConverter;
}
```

- [ ] **Step 6: Remove web View files and update tests/metadata**

Delete:

```text
egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-application/src/main/java/application/manage/user/UserView.java
egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-application/src/main/java/application/manage/teaching/SchoolClassView.java
```

Remove both files from `archetype-metadata.xml` if listed explicitly.

Update generated `OrganizationFlowTest.java` to import and assert domain models:

```java
import ${package}.domain.entities.user.User;
import ${package}.domain.entities.teaching.SchoolClass;
```

Use getters:

```java
User user = userManage.create("Mario", "mario@example.com");
SchoolClass schoolClass = schoolClassManage.create("Class One", "Grade One");
schoolClassManage.assignUser(user.getId(), schoolClass.getId());
User saved = userManage.getById(user.getId());
assertThat(saved.getSchoolClassIds()).contains(schoolClass.getId());
```

Update generated test assertions that read `UserDTO` or `SchoolClassDTO` records to use getters, for example `userDTO.getStatus()`.

- [ ] **Step 7: Run web validation**

Run:

```bash
./mvnw -pl egon-cola-archetypes/egon-cola-archetype-web -am test
rg -n "UserView|SchoolClassView|application\\.view|import .*facade\\.dto|import .*common\\.response" egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-application/src/main/java || true
rg -n "@Autowired|public void set[A-Z].*\\(" egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources || true
```

Expected:

```text
./mvnw command: BUILD SUCCESS
rg commands: no matches
```

- [ ] **Step 8: Commit**

```bash
git add egon-cola-archetypes/egon-cola-archetype-web
git commit -m "feat(archetype-web): align clean architecture details"
```

## Task 4: Service Archetype Boundary, Bean, MapStruct Plus, Dubbo Providers, And Pure-Service Guard

**Files:** Use the service files listed in the File Structure section.

- [ ] **Step 1: Add failing service verify assertions**

Add these assertions to service `verify.groovy` after the generated source assertions:

```groovy
assertFile("student-management-evaluation-application/src/main/java/it/pkg/application/config/DomainServiceConfiguration.java")
assert !new File(projectDir, "student-management-evaluation-application/src/main/java/it/pkg/application/view/course/CourseView.java").exists()
assert !new File(projectDir, "student-management-evaluation-application/src/main/java/it/pkg/application/view/examing/ExamResultView.java").exists()

def courseManageText = assertFile("student-management-evaluation-application/src/main/java/it/pkg/application/manage/course/CourseManage.java").text
assert courseManageText.contains("Course create(String name, int credit)")
assert courseManageText.contains("Course getById(String courseId)")
assert !courseManageText.contains("CourseView")

def examManageText = assertFile("student-management-evaluation-application/src/main/java/it/pkg/application/manage/examing/ExamManage.java").text
assert examManageText.contains("ExamResult record(String courseId, String studentId, int score)")
assert examManageText.contains("ExamResult getById(String examResultId)")
assert !examManageText.contains("ExamResultView")

def courseFacadeText = assertFile("student-management-evaluation-adapter/src/main/java/it/pkg/adapter/facade/impl/CourseFacadeImpl.java").text
assert courseFacadeText.contains("@DubboService")
assert courseFacadeText.contains("@Qualifier(\"courseManage\")")
assert courseFacadeText.contains("@Qualifier(\"courseAdapterConvertor\")")

def examFacadeText = assertFile("student-management-evaluation-adapter/src/main/java/it/pkg/adapter/facade/impl/ExamResultFacadeImpl.java").text
assert examFacadeText.contains("@DubboService")
assert examFacadeText.contains("@Qualifier(\"examManage\")")
assert examFacadeText.contains("@Qualifier(\"examResultAdapterConvertor\")")

def serviceApplicationJava = []
new File(projectDir, "student-management-evaluation-application/src/main/java").eachFileRecurse { file ->
    if (file.isFile() && file.name.endsWith(".java")) {
        serviceApplicationJava << file
    }
}
assert serviceApplicationJava.every { !it.text.contains("View") }
assert serviceApplicationJava.every { !it.text.contains("facade.dto") }
assert serviceApplicationJava.every { !it.text.contains("common.response") }

assert !generatedFiles.any { it.endsWith(".proto") }
assert !generatedFiles.any { it.contains("/grpc/") || it.contains("/grpcjava/") }
```

- [ ] **Step 2: Run the service archetype test and verify it fails on service boundary assertions**

Run:

```bash
./mvnw -pl egon-cola-archetypes/egon-cola-archetype-service -am test
```

Expected: FAIL with an assertion mentioning `CourseView`, `ExamResultView`, `@DubboService`, or a domain return signature.

- [ ] **Step 3: Change service application services to return domain models**

Replace `CourseManage.java` with:

```java
#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.application.manage.course;

import ${package}.domain.entities.course.Course;

public interface CourseManage {

    Course create(String name, int credit);

    Course getById(String courseId);
}
```

Replace `ExamManage.java` with:

```java
#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.application.manage.examing;

import ${package}.domain.entities.examing.ExamResult;

public interface ExamManage {

    ExamResult record(String courseId, String studentId, int score);

    ExamResult getById(String examResultId);
}
```

Update `CourseManageImpl.java` to use named constructor injection and return `Course`:

```java
@Service("courseManage")
@RequiredArgsConstructor
public class CourseManageImpl implements CourseManage {

    @Qualifier("courseRepositoryImpl")
    private final CourseRepository courseRepository;

    @Qualifier("courseDomainService")
    private final CourseDomainService courseDomainService;

    @Override
    @Transactional
    public Course create(String name, int credit) {
        Course course = Course.create(IdGenerator.nextId(), name, credit);
        courseDomainService.ensureCourseNameAvailable(name);
        return courseRepository.save(course);
    }

    @Override
    public Course getById(String courseId) {
        return courseRepository.findById(courseId)
                .orElseThrow(() -> new NotFoundException(ErrorCodes.COURSE_NOT_FOUND, "course not found"));
    }
}
```

Update `ExamManageImpl.java` to use named constructor injection and return `ExamResult`:

```java
@Service("examManage")
@RequiredArgsConstructor
public class ExamManageImpl implements ExamManage {

    @Qualifier("examResultRepositoryImpl")
    private final ExamResultRepository examResultRepository;

    @Qualifier("examDomainService")
    private final ExamDomainService examDomainService;

    @Override
    @Transactional
    public ExamResult record(String courseId, String studentId, int score) {
        examDomainService.record(courseId, studentId, score);
        ExamResult examResult = ExamResult.record(IdGenerator.nextId(), courseId, studentId, score);
        return examResultRepository.save(examResult);
    }

    @Override
    public ExamResult getById(String examResultId) {
        return examResultRepository.findById(examResultId)
                .orElseThrow(() -> new NotFoundException(ErrorCodes.EXAM_RESULT_NOT_FOUND, "exam result not found"));
    }
}
```

Add `DomainServiceConfiguration.java`:

```java
#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.application.config;

import ${package}.domain.repos.course.CourseRepository;
import ${package}.domain.service.course.CourseDomainService;
import ${package}.domain.service.examing.ExamDomainService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DomainServiceConfiguration {

    @Bean("courseDomainService")
    public CourseDomainService courseDomainService(@Qualifier("courseRepositoryImpl") CourseRepository courseRepository) {
        return new CourseDomainService(courseRepository);
    }

    @Bean("examDomainService")
    public ExamDomainService examDomainService(@Qualifier("courseRepositoryImpl") CourseRepository courseRepository) {
        return new ExamDomainService(courseRepository);
    }
}
```

- [ ] **Step 4: Convert service adapter/infrastructure converters and DTO targets**

Use this bean shape for `CourseAdapterConvertor.java`:

```java
@Component("courseAdapterConvertor")
@RequiredArgsConstructor
public class CourseAdapterConvertor {

    @Qualifier("converter")
    private final Converter converter;

    public CourseDTO toDTO(Course course) {
        CourseDTO dto = converter.convert(course, CourseDTO.class);
        dto.setStatus(course.getStatus().name());
        return dto;
    }
}
```

Use this bean shape for `ExamResultAdapterConvertor.java`:

```java
@Component("examResultAdapterConvertor")
@RequiredArgsConstructor
public class ExamResultAdapterConvertor {

    @Qualifier("converter")
    private final Converter converter;

    public ExamResultDTO toDTO(ExamResult examResult) {
        ExamResultDTO dto = converter.convert(examResult, ExamResultDTO.class);
        dto.setStatus(examResult.getStatus().name());
        return dto;
    }
}
```

Convert response DTO records into MapStruct Plus targets:

```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@AutoMapper(target = Course.class, reverseConvertGenerate = false)
public class CourseDTO implements Serializable {
    private String id;
    private String name;
    private int credit;
    private String status;
}
```

```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@AutoMapper(target = ExamResult.class, reverseConvertGenerate = false)
public class ExamResultDTO implements Serializable {
    private String id;
    private String courseId;
    private String studentId;
    private int score;
    private String status;
}
```

Convert `CourseConverter.java` and `ExamResultConverter.java` to named Spring Beans using `Converter converter`, while keeping explicit timestamp arguments in `toPo` methods.

- [ ] **Step 5: Convert service facades, MQ consumer, repositories, and handler to named Beans**

Update `CourseFacadeImpl.java` to Dubbo:

```java
@DubboService(
        interfaceClass = CourseFacade.class,
        version = "1.0.0",
        group = "course"
)
@RequiredArgsConstructor
public class CourseFacadeImpl implements CourseFacade {

    @Qualifier("courseManage")
    private final CourseManage courseManage;

    @Qualifier("courseAdapterConvertor")
    private final CourseAdapterConvertor courseAdapterConvertor;

    @Qualifier("serviceExceptionHandler")
    private final ServiceExceptionHandler serviceExceptionHandler;
}
```

Update `ExamResultFacadeImpl.java` to Dubbo:

```java
@DubboService(
        interfaceClass = ExamResultFacade.class,
        version = "1.0.0",
        group = "exam-result"
)
@RequiredArgsConstructor
public class ExamResultFacadeImpl implements ExamResultFacade {

    @Qualifier("examManage")
    private final ExamManage examManage;

    @Qualifier("examResultAdapterConvertor")
    private final ExamResultAdapterConvertor examResultAdapterConvertor;

    @Qualifier("serviceExceptionHandler")
    private final ServiceExceptionHandler serviceExceptionHandler;
}
```

Use `@Component("examResultMessageConsumer")`, `@Component("serviceExceptionHandler")`, `@Repository("courseRepositoryImpl")`, and `@Repository("examResultRepositoryImpl")`. Add `@RequiredArgsConstructor`, final fields, and `@Qualifier` to each injected dependency.

- [ ] **Step 6: Remove service View files and update tests/metadata**

Delete:

```text
egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-application/src/main/java/application/view/course/CourseView.java
egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-application/src/main/java/application/view/examing/ExamResultView.java
```

Remove both deleted files from `archetype-metadata.xml` if listed explicitly.

Update generated `EvaluationFlowTest.java` to use getters on DTO classes:

```java
assertThat(courseResponse.getData().getStatus()).isEqualTo("ENABLED");
CourseDTO course = courseResponse.getData();
assertThat(examResultResponse.getData().getStatus()).isEqualTo("PASSED");
```

Update application-level expectations in the same test to use domain imports:

```java
import ${package}.domain.entities.course.Course;
import ${package}.domain.entities.examing.ExamResult;
```

Use `course.getId()` and `examResult.getStatus()` where generated tests previously used record accessors or view accessors.

- [ ] **Step 7: Run service validation**

Run:

```bash
./mvnw -pl egon-cola-archetypes/egon-cola-archetype-service -am test
rg -n "CourseView|ExamResultView|application\\.view|import .*facade\\.dto|import .*common\\.response" egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-application/src/main/java || true
rg -n "spring-boot-starter-web|spring-boot-starter-webflux|/controller/|/web/|/filter/|/graphql/|/vo/" egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources || true
rg -n "@Autowired|public void set[A-Z].*\\(" egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources || true
```

Expected:

```text
./mvnw command: BUILD SUCCESS
rg commands: no matches
```

- [ ] **Step 8: Commit**

```bash
git add egon-cola-archetypes/egon-cola-archetype-service
git commit -m "feat(archetype-service): align clean architecture details"
```

## Task 5: README, Architecture Guardrails, And Final Verification

**Files:**
- Modify: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/README.md`
- Modify: `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/README.md`
- Modify: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/README.md`
- Modify: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/test/java/ArchitectureDependencyTest.java`
- Modify: `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-starter/src/test/java/starter/ArchitectureDependencyTest.java`
- Modify: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-starter/src/test/java/starter/ServiceArchitectureDependencyTest.java`

- [ ] **Step 1: Update generated README wording**

In all generated README files, add this boundary summary:

```markdown
## Clean Architecture Boundary Rules

- `application.manage` returns domain models or simple values only.
- `adapter` converts domain models to HTTP, RPC, or MQ-facing objects.
- `facade` defines Dubbo3 RPC contracts.
- `adapter` exposes facade implementations through Dubbo3 Triple.
- Converters use MapStruct Plus for flat model mapping and explicit Java code for semantic mapping.
- Spring Beans are named ordinary classes using Lombok `@RequiredArgsConstructor`; injected dependencies use `@Qualifier`.
- The generated project does not include native grpc-java services.
```

In the service README, replace any "technology-neutral RPC" wording with:

```markdown
The service archetype demonstrates Dubbo3 RPC with the Triple protocol by default. It does not generate native grpc-java services or a separate gRPC module.
```

- [ ] **Step 2: Strengthen generated architecture tests**

In each generated architecture test, add these exact ArchUnit checks, adjusting only the field name from `classes` to `importedClasses` where that file already uses `importedClasses`:

```java
@Test
void application_should_not_depend_on_external_models() {
    noClasses()
            .that().resideInAPackage("..application..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "..adapter..",
                    "..facade.dto..",
                    "..common.response..",
                    "org.springframework.web..")
            .check(importedClasses);
}

@Test
void service_template_should_not_generate_native_grpc() {
    noClasses()
            .should().dependOnClassesThat().resideInAnyPackage(
                    "io.grpc..",
                    "..grpc..")
            .check(importedClasses);
}
```

For light and web, keep existing HTTP adapter rules and only add the `application_should_not_depend_on_external_models` rule. For service, keep the pure-service Web ban and add the native gRPC dependency ban.

- [ ] **Step 3: Run all archetype tests**

Run:

```bash
./mvnw -pl egon-cola-archetypes/egon-cola-archetype-light -am test
./mvnw -pl egon-cola-archetypes/egon-cola-archetype-web -am test
./mvnw -pl egon-cola-archetypes/egon-cola-archetype-service -am test
```

Expected:

```text
BUILD SUCCESS
BUILD SUCCESS
BUILD SUCCESS
```

- [ ] **Step 4: Run repository-wide generated template searches**

Run:

```bash
rg -n "application\\.view|StudentView|CourseView|UserView|SchoolClassView|ExamResultView" egon-cola-archetypes/egon-cola-archetype-{light,web,service}/src/main/resources/archetype-resources || true
rg -n "@Autowired|public void set[A-Z].*\\(" egon-cola-archetypes/egon-cola-archetype-{light,web,service}/src/main/resources/archetype-resources || true
rg -n "grpc-java|\\.proto|/grpc/|io\\.grpc" egon-cola-archetypes/egon-cola-archetype-{light,web,service}/src/main/resources/archetype-resources || true
rg -n "technology-neutral RPC|technology neutral RPC" egon-cola-archetypes/egon-cola-archetype-{light,web,service}/src/main/resources/archetype-resources egon-cola-archetypes/egon-cola-archetype-service/student-management-service-only-rpc-mq-architecture.md || true
```

Expected:

```text
No matches from every rg command.
```

- [ ] **Step 5: Inspect generated git diff for unrelated scope**

Run:

```bash
git status --short
git diff --stat
git diff --check
```

Expected:

```text
git status: only archetype template, verify, README, architecture-test, and plan-related files are modified
git diff --stat: no target/ files
git diff --check: no output
```

- [ ] **Step 6: Commit**

```bash
git add egon-cola-archetypes/egon-cola-archetype-light egon-cola-archetypes/egon-cola-archetype-web egon-cola-archetypes/egon-cola-archetype-service
git commit -m "test(archetype): enforce clean architecture guardrails"
```

## Final Review Checklist

- [ ] The light archetype remains a single-module generated project.
- [ ] The web archetype remains a seven-module generated project.
- [ ] The service archetype remains a seven-module pure-service generated project.
- [ ] No `application.manage` contract returns `View`, `DTO`, or `Response`.
- [ ] No application package imports `facade.dto`, `adapter.dto`, `common.response`, or Spring Web response types.
- [ ] All Spring Beans added or modified by this work are ordinary classes with explicit Bean names.
- [ ] All injected dependency fields added or modified by this work are `private final` and have `@Qualifier`.
- [ ] All modified Bean classes use `@RequiredArgsConstructor`.
- [ ] Adapter and infrastructure converters use MapStruct Plus.
- [ ] All three generated projects include Dubbo3 Triple facade examples.
- [ ] No native grpc-java service, `.proto` service definition, or standalone gRPC module is generated.
- [ ] The service archetype still has no Web controller, Web filter, WebFlux, GraphQL, or Web VO.
- [ ] No generated application was started during validation.

## Self-Review Notes

- Spec coverage: Tasks 1-5 cover dependencies, Lombok config, Dubbo Triple config, light/web/service application-domain boundaries, adapter conversion, MapStruct Plus conversion, named Bean injection, pure-service restrictions, README wording, architecture tests, and final validation.
- Placeholder scan: This plan contains no unresolved placeholder markers.
- Type consistency: Domain return types are `Student`, `Course`, `User`, `SchoolClass`, and `ExamResult`; adapter DTO methods use `toDto` in light/web and current `toDTO` spelling in service to preserve existing service naming.
