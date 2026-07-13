# Evaluation-Organization Facade Integration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make the web and service archetypes generate bidirectional, read-only Facade client foundations without wiring them into current Application use cases.

**Architecture:** Each generated Domain module owns a narrow query port and consumer projection records. Only Infrastructure depends on the provider Facade artifact and Dubbo, where a dev/prod real adapter and local/test deterministic stub form an Anti-Corruption Layer. Archetype integration tests rebuild a minimal fixture artifact and install it under a dedicated `fixture.*` coordinate in the active Maven local repository before compiling the generated consumer.

**Tech Stack:** Java 21, Maven Archetype Plugin 3.4.1, Spring Boot 3.5.16, Apache Dubbo 3.3.6, JUnit 5, Mockito, AssertJ, ArchUnit, Groovy archetype verifier.

---

## File Structure

### Service archetype: Evaluation consumes Organization

- `egon-cola-archetypes/egon-cola-archetype-service/pom.xml`: build and install the Organization Facade fixture before archetype IT.
- `.../src/test/java/fixture/organization/facade/**`: minimal provider-owned contract subset used only by archetype IT.
- `.../src/main/resources/META-INF/maven/archetype-metadata.xml`: require Organization coordinates and Java base package.
- `.../src/main/resources/archetype-resources/pom.xml`: pin the Organization Facade artifact in dependency management.
- `.../__rootArtifactId__-domain/src/main/java/domain/client/organization/**`: `OrganizationDirectoryPort` and consumer projections.
- `.../__rootArtifactId__-infrastructure/src/main/java/infrastructure/client/organization/**`: Dubbo adapter, deterministic stub, failure mapper.
- `.../__rootArtifactId__-infrastructure/src/test/java/infrastructure/client/organization/**`: conversion, error, and stub tests.
- `.../__rootArtifactId__-starter/src/main/resources/application*.yml`: local/test stub and dev/prod real-client configuration.
- `.../__rootArtifactId__-starter/src/test/java/starter/ServiceArchitectureDependencyTest.java`: external contract import boundary.
- `.../src/test/resources/projects/basic/{archetype.properties,verify.groovy}`: fixture coordinates and generated contract assertions.
- `.../src/main/resources/archetype-resources/README.md`: publication and profile behavior.

### Web archetype: Organization consumes Evaluation

- `egon-cola-archetypes/egon-cola-archetype-web/pom.xml`: build and install the Evaluation Facade fixture before archetype IT.
- `.../src/test/java/fixture/evaluation/facade/**`: minimal Evaluation response-wrapper and query-contract subset.
- `.../src/main/resources/META-INF/maven/archetype-metadata.xml`: require Evaluation coordinates and Java base package.
- `.../src/main/resources/archetype-resources/pom.xml`: pin the Evaluation Facade artifact in dependency management.
- `.../__rootArtifactId__-domain/src/main/java/domain/client/evaluation/**`: `EvaluationQueryPort` and consumer projections.
- `.../__rootArtifactId__-infrastructure/src/main/java/infrastructure/client/evaluation/**`: Dubbo adapter, deterministic stub, response/failure mapper.
- `.../__rootArtifactId__-infrastructure/src/test/java/infrastructure/client/evaluation/**`: conversion, response, error, and stub tests.
- `.../__rootArtifactId__-starter/src/main/resources/application*.yml`: local/test stub and dev/prod real-client configuration.
- `.../__rootArtifactId__-starter/src/test/java/starter/ArchitectureDependencyTest.java`: external contract import boundary.
- `.../src/test/resources/projects/basic/{archetype.properties,verify.groovy}`: fixture coordinates and generated contract assertions.
- `.../src/main/resources/archetype-resources/README.md`: publication and profile behavior.

No Flyway migration is created or modified.

## Shared Consumer Failure Contract

Both generated Domain modules define the same consumer-owned failure categories, under their own base package:

```java
public enum ExternalDependencyFailure {
    NOT_FOUND,
    BUSINESS_REJECTED,
    VALIDATION_FAILED,
    UNAVAILABLE,
    TIMEOUT,
    CONTRACT_INCOMPATIBLE,
    SERVICE_FAILURE
}

public final class ExternalDependencyException extends RuntimeException {
    private final String dependency;
    private final ExternalDependencyFailure failure;
    private final String externalCode;

    public ExternalDependencyException(
            String dependency,
            ExternalDependencyFailure failure,
            String externalCode,
            String message,
            Throwable cause) {
        super(message, cause);
        this.dependency = dependency;
        this.failure = failure;
        this.externalCode = externalCode;
    }

    public String dependency() { return dependency; }
    public ExternalDependencyFailure failure() { return failure; }
    public String externalCode() { return externalCode; }
}
```

The Infrastructure adapters map provider or Dubbo failures into these types. Domain and Application never import provider Facade or Dubbo classes.

---

### Task 1: Add the service-archetype Organization fixture and failing generation contract

**Files:**
- Modify: `egon-cola-archetypes/egon-cola-archetype-service/pom.xml`
- Create: `egon-cola-archetypes/egon-cola-archetype-service/src/test/java/fixture/organization/facade/user/UserFacade.java`
- Create: `egon-cola-archetypes/egon-cola-archetype-service/src/test/java/fixture/organization/facade/teaching/SchoolClassFacade.java`
- Create: `egon-cola-archetypes/egon-cola-archetype-service/src/test/java/fixture/organization/facade/dto/user/UserDetailDTO.java`
- Create: `egon-cola-archetypes/egon-cola-archetype-service/src/test/java/fixture/organization/facade/dto/teaching/SchoolClassDetailDTO.java`
- Create: `egon-cola-archetypes/egon-cola-archetype-service/src/test/java/fixture/organization/facade/exceptions/OrganizationFacadeException.java`
- Modify: `egon-cola-archetypes/egon-cola-archetype-service/src/test/resources/projects/basic/archetype.properties`
- Modify: `egon-cola-archetypes/egon-cola-archetype-service/src/test/resources/projects/basic/verify.groovy`

- [ ] **Step 1: Add the exact read-only Organization fixture surface**

```java
package fixture.organization.facade.user;

import fixture.organization.facade.dto.user.UserDetailDTO;

public interface UserFacade {
    UserDetailDTO getUser(String userId);
}
```

```java
package fixture.organization.facade.teaching;

import fixture.organization.facade.dto.teaching.SchoolClassDetailDTO;

public interface SchoolClassFacade {
    SchoolClassDetailDTO getSchoolClass(String schoolClassId);
}
```

The fixture DTOs exactly match the provider fields used by the client:

```java
public record UserDetailDTO(
        String id, String name, String email, String status, List<String> roleCodes)
        implements Serializable {
    public UserDetailDTO { roleCodes = List.copyOf(roleCodes); }
}

public record SchoolClassDetailDTO(
        String id, String name, String gradeCode, String gradeName,
        String status, List<String> userIds) {
    public SchoolClassDetailDTO { userIds = List.copyOf(userIds); }
}
```

`OrganizationFacadeException` must expose `code()` and `traceId()` exactly like the current web contract.

- [ ] **Step 2: Package and install the fixture for archetype IT**

In the service archetype POM, bind `maven-jar-plugin:test-jar` at `package`, then bind `maven-install-plugin:install-file` at `pre-integration-test` with:

```xml
<groupId>fixture.organization</groupId>
<artifactId>student-management-organization-facade</artifactId>
<version>1.0.0-fixture</version>
<packaging>jar</packaging>
<generatePom>true</generatePom>
<localRepositoryPath>${project.build.directory}/local-repo</localRepositoryPath>
```

Use `${project.build.directory}/${project.build.finalName}-tests.jar` as the file and configure `maven-archetype-plugin` to use the same `${project.build.directory}/local-repo`.

- [ ] **Step 3: Add fixture generation parameters**

Append to `archetype.properties`:

```properties
organizationFacadeGroupId=fixture.organization
organizationFacadeArtifactId=student-management-organization-facade
organizationFacadeVersion=1.0.0-fixture
organizationFacadePackage=fixture.organization
```

- [ ] **Step 4: Write the failing generated-project assertions**

Update `verify.groovy` to require:

```groovy
assert rootPom.properties.'organization-facade.group-id'.text() == "fixture.organization"
assert rootPom.properties.'organization-facade.artifact-id'.text() ==
        "student-management-organization-facade"
assert rootPom.properties.'organization-facade.version'.text() == "1.0.0-fixture"
assert rootPom.properties.'organization-facade.package'.text() == "fixture.organization"
assert dependencyArtifacts("infrastructure").contains("student-management-organization-facade")
```

Require the new Domain port, projections, Infrastructure clients, client tests, and profile configuration. Remove the old assertion that `domain/client` and `infrastructure/client` are absent.

- [ ] **Step 5: Run the red test**

Run:

```bash
bash ./mvnw -B -ntp -pl egon-cola-archetypes/egon-cola-archetype-service -am clean integration-test
```

Expected: FAIL in `verify.groovy` because the generated root POM and client files do not yet exist. The fixture itself must compile and install successfully before that failure.

- [ ] **Step 6: Commit the fixture and red contract**

```bash
git add -- egon-cola-archetypes/egon-cola-archetype-service
git commit -m "test(archetype): define organization facade client contract"
```

---

### Task 2: Generate the Evaluation-side Domain boundary and external dependency

**Files:**
- Modify: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/META-INF/maven/archetype-metadata.xml`
- Modify: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/pom.xml`
- Modify: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/pom.xml`
- Create: `.../__rootArtifactId__-domain/src/main/java/domain/client/ExternalDependencyFailure.java`
- Create: `.../__rootArtifactId__-domain/src/main/java/domain/client/ExternalDependencyException.java`
- Create: `.../__rootArtifactId__-domain/src/main/java/domain/client/organization/OrganizationDirectoryPort.java`
- Create: `.../__rootArtifactId__-domain/src/main/java/domain/client/organization/OrganizationUser.java`
- Create: `.../__rootArtifactId__-domain/src/main/java/domain/client/organization/OrganizationSchoolClass.java`
- Create: `.../__rootArtifactId__-domain/src/main/java/domain/client/organization/package-info.java`
- Modify: `.../__rootArtifactId__-domain/src/main/java/domain/client/package-info.java`

- [ ] **Step 1: Require the four external contract properties**

Add required properties with no inferred consumer-coordinate defaults:

```xml
<requiredProperty key="organizationFacadeGroupId"/>
<requiredProperty key="organizationFacadeArtifactId"/>
<requiredProperty key="organizationFacadeVersion"/>
<requiredProperty key="organizationFacadePackage"/>
```

- [ ] **Step 2: Add root dependency-management properties and the managed artifact**

```xml
<organization-facade.group-id>${organizationFacadeGroupId}</organization-facade.group-id>
<organization-facade.artifact-id>${organizationFacadeArtifactId}</organization-facade.artifact-id>
<organization-facade.version>${organizationFacadeVersion}</organization-facade.version>
<organization-facade.package>${organizationFacadePackage}</organization-facade.package>
```

```xml
<dependency>
    <groupId>${organization-facade.group-id}</groupId>
    <artifactId>${organization-facade.artifact-id}</artifactId>
    <version>${organization-facade.version}</version>
</dependency>
```

Only the Infrastructure POM declares that dependency. It also declares `org.apache.dubbo:dubbo-spring-boot-starter`; no other generated module gains the external artifact or Dubbo client dependency.

- [ ] **Step 3: Add the consumer-owned port and projections**

```java
public interface OrganizationDirectoryPort {
    OrganizationUser getUser(String userId);
    OrganizationSchoolClass getSchoolClass(String schoolClassId);
}

public record OrganizationUser(String id, String name, String status) {}

public record OrganizationSchoolClass(
        String id, String name, String gradeCode, String status, List<String> userIds) {
    public OrganizationSchoolClass { userIds = List.copyOf(userIds); }
}
```

Create `ExternalDependencyFailure` and `ExternalDependencyException` with exactly the definitions in the plan's **Shared Consumer Failure Contract** section. Keep every Domain file free of Spring, Dubbo, `${organizationFacadePackage}`, and provider DTO types.

- [ ] **Step 4: Run the focused generated build**

Run the service archetype integration test again.

Expected: FAIL because the verifier now finds the parameters, POM, and Domain files but not the Infrastructure adapter and profile tests.

- [ ] **Step 5: Commit**

```bash
git add -- egon-cola-archetypes/egon-cola-archetype-service
git commit -m "feat(archetype): add organization facade domain boundary"
```

---

### Task 3: Generate the Evaluation-side Dubbo adapter, stub, profiles, and tests

**Files:**
- Create: `.../__rootArtifactId__-infrastructure/src/main/java/infrastructure/client/organization/DubboOrganizationDirectoryClient.java`
- Create: `.../__rootArtifactId__-infrastructure/src/main/java/infrastructure/client/organization/LocalOrganizationDirectoryStub.java`
- Create: `.../__rootArtifactId__-infrastructure/src/main/java/infrastructure/client/organization/OrganizationClientFailureMapper.java`
- Create: `.../__rootArtifactId__-infrastructure/src/main/java/infrastructure/client/organization/package-info.java`
- Create: `.../__rootArtifactId__-infrastructure/src/main/java/infrastructure/client/package-info.java`
- Create: `.../__rootArtifactId__-infrastructure/src/test/java/infrastructure/client/organization/DubboOrganizationDirectoryClientTest.java`
- Create: `.../__rootArtifactId__-infrastructure/src/test/java/infrastructure/client/organization/LocalOrganizationDirectoryStubTest.java`
- Modify: `.../__rootArtifactId__-starter/src/main/resources/application.yml`
- Modify: `.../__rootArtifactId__-starter/src/main/resources/application-local.yml`
- Modify: `.../__rootArtifactId__-starter/src/main/resources/application-test.yml`
- Modify: `.../__rootArtifactId__-starter/src/main/resources/application-dev.yml`
- Modify: `.../__rootArtifactId__-starter/src/main/resources/application-prod.yml`
- Modify: `.../__rootArtifactId__-starter/src/test/java/starter/EvaluationExternalFreeContextTest.java`
- Modify: `.../__rootArtifactId__-starter/src/test/java/starter/ServiceArchitectureDependencyTest.java`
- Modify: `.../src/main/resources/archetype-resources/README.md`

- [ ] **Step 1: Write the adapter tests before the implementation**

Use Mockito Facade references. Assert exact mappings:

```java
when(userFacade.getUser("user-1"))
        .thenReturn(new UserDetailDTO("user-1", "Mario", "m@example.com", "ACTIVE", List.of("STUDENT")));
assertThat(client.getUser("user-1"))
        .isEqualTo(new OrganizationUser("user-1", "Mario", "ACTIVE"));

when(schoolClassFacade.getSchoolClass("class-1"))
        .thenReturn(new SchoolClassDetailDTO(
                "class-1", "Class One", "G1", "Grade One", "ACTIVE", List.of("user-1")));
assertThat(client.getSchoolClass("class-1").userIds()).containsExactly("user-1");
```

Also assert:

- Provider code containing `NOT_FOUND` maps to `NOT_FOUND`.
- Provider code containing `VALIDATION` or `INVALID` maps to `VALIDATION_FAILED`.
- Dubbo timeout maps to `TIMEOUT`; other `RpcException` maps to `UNAVAILABLE`.
- A null DTO maps to `CONTRACT_INCOMPATIBLE`.
- An unknown provider failure maps to `SERVICE_FAILURE` without exposing the remote stack trace in the consumer message.

- [ ] **Step 2: Run the generated Infrastructure test and observe failure**

Run the service archetype integration test.

Expected: FAIL compilation because `DubboOrganizationDirectoryClient` and the stub do not exist.

- [ ] **Step 3: Implement the real adapter**

```java
@Component
@Profile({"dev", "prod"})
public class DubboOrganizationDirectoryClient implements OrganizationDirectoryPort {

    @DubboReference(
            group = "${app.integrations.organization.group:student-management-organization}",
            version = "${app.integrations.organization.version:1.0.0}", retries = 0, check = true)
    private UserFacade userFacade;

    @DubboReference(
            group = "${app.integrations.organization.group:student-management-organization}",
            version = "${app.integrations.organization.version:1.0.0}", retries = 0, check = true)
    private SchoolClassFacade schoolClassFacade;

    DubboOrganizationDirectoryClient(UserFacade userFacade, SchoolClassFacade schoolClassFacade) {
        this.userFacade = userFacade;
        this.schoolClassFacade = schoolClassFacade;
    }

    public DubboOrganizationDirectoryClient() {}

    // getUser/getSchoolClass invoke provider, reject null, map provider DTO to Domain projection,
    // and delegate every external exception to OrganizationClientFailureMapper.
}
```

Use `${organizationFacadePackage}.facade...` imports. Do not add any Facade import outside Infrastructure.

- [ ] **Step 4: Implement the local/test stub**

```java
@Component
@Profile({"local", "test"})
public class LocalOrganizationDirectoryStub implements OrganizationDirectoryPort {
    @Override
    public OrganizationUser getUser(String userId) {
        rejectMissing(userId);
        return new OrganizationUser(userId, "Local User " + userId, "ACTIVE");
    }

    @Override
    public OrganizationSchoolClass getSchoolClass(String schoolClassId) {
        rejectMissing(schoolClassId);
        return new OrganizationSchoolClass(
                schoolClassId, "Local Class " + schoolClassId, "LOCAL", "ACTIVE", List.of("local-user"));
    }
}
```

Blank identifiers and identifiers starting with `missing-` throw `ExternalDependencyException` with `NOT_FOUND`. The stub performs no network or registry work.

- [ ] **Step 5: Add runtime configuration**

In `application.yml` add a finite global consumer baseline:

```yaml
dubbo:
  consumer:
    timeout: ${DUBBO_CONSUMER_TIMEOUT:3000}
    retries: 0
    check: true
```

Add `app.integrations.organization` in all profiles. Local/test set `enabled: false`; dev/prod set `enabled: true` and expose `group`, `version`, and timeout environment variables. The bean profiles are the enforcement mechanism; the `enabled` property documents and validates intended configuration.

- [ ] **Step 6: Strengthen architecture and context tests**

Assert the test-profile bean is `LocalOrganizationDirectoryStub`, no `DubboOrganizationDirectoryClient` bean exists, Domain and Application do not depend on `fixture.organization..`, and only `infrastructure.client.organization..` may depend on it.

- [ ] **Step 7: Run the green service archetype test**

Run:

```bash
bash ./mvnw -B -ntp -pl egon-cola-archetypes/egon-cola-archetype-service -am clean integration-test
```

Expected: `BUILD SUCCESS`; generated project tests pass without Nacos, an Organization provider, RabbitMQ, or PostgreSQL.

- [ ] **Step 8: Commit**

```bash
git add -- egon-cola-archetypes/egon-cola-archetype-service
git commit -m "feat(archetype): integrate organization facade client"
```

---

### Task 4: Add the web-archetype Evaluation fixture and failing generation contract

**Files:**
- Modify: `egon-cola-archetypes/egon-cola-archetype-web/pom.xml`
- Create: `egon-cola-archetypes/egon-cola-archetype-web/src/test/java/fixture/evaluation/facade/api/{CourseFacade,ExamFacade,ScoreFacade}.java`
- Create: `.../src/test/java/fixture/evaluation/facade/dto/SingleResponse.java`
- Create: `.../src/test/java/fixture/evaluation/facade/dto/course/{GetCourseRequest,CourseResponse}.java`
- Create: `.../src/test/java/fixture/evaluation/facade/dto/exam/{GetExamRequest,ExamResponse,GetScoreRequest,ScoreResponse}.java`
- Modify: `egon-cola-archetypes/egon-cola-archetype-web/src/test/resources/projects/basic/archetype.properties`
- Modify: `egon-cola-archetypes/egon-cola-archetype-web/src/test/resources/projects/basic/verify.groovy`

- [ ] **Step 1: Add the exact read-only Evaluation fixture surface**

```java
public interface CourseFacade {
    SingleResponse<CourseResponse> getCourse(GetCourseRequest request);
}

public interface ExamFacade {
    SingleResponse<ExamResponse> getExam(GetExamRequest request);
}

public interface ScoreFacade {
    SingleResponse<ScoreResponse> getScore(GetScoreRequest request);
}
```

`SingleResponse<T>` exposes `isSuccess()`, `getCode()`, `getMessage()`, `getData()`, plus `of(T)` and `fail(String,String)` factories. Fixture request/response record fields must match the current service Facade exactly.

- [ ] **Step 2: Package and install the fixture**

Bind `maven-jar-plugin:test-jar` at `package`, then bind `maven-install-plugin:install-file` at `pre-integration-test`. Use `${project.build.directory}/${project.build.finalName}-tests.jar`, enable generated POM creation, install into `${project.build.directory}/local-repo`, and configure `maven-archetype-plugin` to use that same repository. The installed coordinates are:

```xml
<groupId>fixture.evaluation</groupId>
<artifactId>student-management-evaluation-facade</artifactId>
<version>1.0.0-fixture</version>
```

Install into `${project.build.directory}/local-repo` and configure the archetype integration test to use it.

- [ ] **Step 3: Add fixture generation parameters**

```properties
evaluationFacadeGroupId=fixture.evaluation
evaluationFacadeArtifactId=student-management-evaluation-facade
evaluationFacadeVersion=1.0.0-fixture
evaluationFacadePackage=fixture.evaluation
```

- [ ] **Step 4: Add failing verifier assertions**

Require the four root properties, the external dependency only in Infrastructure, the Domain query port/projections, both Infrastructure implementations, tests, and all profile settings.

- [ ] **Step 5: Run the red web archetype test**

```bash
bash ./mvnw -B -ntp -pl egon-cola-archetypes/egon-cola-archetype-web -am clean integration-test
```

Expected: FAIL in `verify.groovy` after the fixture compiles and installs.

- [ ] **Step 6: Commit**

```bash
git add -- egon-cola-archetypes/egon-cola-archetype-web
git commit -m "test(archetype): define evaluation facade client contract"
```

---

### Task 5: Generate the Organization-side Domain boundary and external dependency

**Files:**
- Modify: `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/META-INF/maven/archetype-metadata.xml`
- Modify: `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/pom.xml`
- Modify: `.../__rootArtifactId__-infrastructure/pom.xml`
- Create: `.../__rootArtifactId__-domain/src/main/java/domain/client/ExternalDependencyFailure.java`
- Create: `.../__rootArtifactId__-domain/src/main/java/domain/client/ExternalDependencyException.java`
- Create: `.../__rootArtifactId__-domain/src/main/java/domain/client/evaluation/EvaluationQueryPort.java`
- Create: `.../__rootArtifactId__-domain/src/main/java/domain/client/evaluation/EvaluationCourse.java`
- Create: `.../__rootArtifactId__-domain/src/main/java/domain/client/evaluation/EvaluationExam.java`
- Create: `.../__rootArtifactId__-domain/src/main/java/domain/client/evaluation/EvaluationScore.java`
- Create: `.../__rootArtifactId__-domain/src/main/java/domain/client/evaluation/package-info.java`

- [ ] **Step 1: Add required properties and managed dependency**

Add four required properties with no inferred defaults:

```xml
<requiredProperty key="evaluationFacadeGroupId"/>
<requiredProperty key="evaluationFacadeArtifactId"/>
<requiredProperty key="evaluationFacadeVersion"/>
<requiredProperty key="evaluationFacadePackage"/>
```

Add these generated root-POM properties and manage the resulting artifact:

```xml
<evaluation-facade.group-id>${evaluationFacadeGroupId}</evaluation-facade.group-id>
<evaluation-facade.artifact-id>${evaluationFacadeArtifactId}</evaluation-facade.artifact-id>
<evaluation-facade.version>${evaluationFacadeVersion}</evaluation-facade.version>
<evaluation-facade.package>${evaluationFacadePackage}</evaluation-facade.package>
```

```xml
<dependency>
    <groupId>${evaluation-facade.group-id}</groupId>
    <artifactId>${evaluation-facade.artifact-id}</artifactId>
    <version>${evaluation-facade.version}</version>
</dependency>
```

Only Infrastructure declares the external artifact and `org.apache.dubbo:dubbo-spring-boot-starter`.

- [ ] **Step 2: Add the consumer query port and projections**

```java
public interface EvaluationQueryPort {
    EvaluationCourse getCourse(String courseId);
    EvaluationExam getExam(String examId);
    EvaluationScore getScore(String scoreId);
}

public record EvaluationCourse(String id, String code, String name, int credit, String status) {}

public record EvaluationExam(
        String id, String courseId, String title, Instant startsAt, Instant endsAt, String status) {}

public record EvaluationScore(
        String id, String examId, String courseId, String studentId, int points, String status) {}
```

Create `ExternalDependencyFailure` and `ExternalDependencyException` with exactly the definitions in the plan's **Shared Consumer Failure Contract** section. Do not wire `EvaluationQueryPort` into a Manage class, controller, GraphQL resolver, Facade implementation, or MQ consumer.

- [ ] **Step 3: Run the focused red test**

Run the web archetype integration test.

Expected: FAIL only for missing Infrastructure clients/profile coverage.

- [ ] **Step 4: Commit**

```bash
git add -- egon-cola-archetypes/egon-cola-archetype-web
git commit -m "feat(archetype): add evaluation facade domain boundary"
```

---

### Task 6: Generate the Organization-side Dubbo adapter, stub, profiles, and tests

**Files:**
- Create: `.../__rootArtifactId__-infrastructure/src/main/java/infrastructure/client/evaluation/DubboEvaluationQueryClient.java`
- Create: `.../__rootArtifactId__-infrastructure/src/main/java/infrastructure/client/evaluation/LocalEvaluationQueryStub.java`
- Create: `.../__rootArtifactId__-infrastructure/src/main/java/infrastructure/client/evaluation/EvaluationClientFailureMapper.java`
- Create: `.../__rootArtifactId__-infrastructure/src/main/java/infrastructure/client/evaluation/package-info.java`
- Create: `.../__rootArtifactId__-infrastructure/src/main/java/infrastructure/client/package-info.java`
- Create: `.../__rootArtifactId__-infrastructure/src/test/java/infrastructure/client/evaluation/DubboEvaluationQueryClientTest.java`
- Create: `.../__rootArtifactId__-infrastructure/src/test/java/infrastructure/client/evaluation/LocalEvaluationQueryStubTest.java`
- Modify: `.../__rootArtifactId__-starter/src/main/resources/application.yml`
- Modify: `.../__rootArtifactId__-starter/src/main/resources/application-{local,test,dev,prod}.yml`
- Modify: `.../__rootArtifactId__-starter/src/test/java/starter/OrganizationApplicationTest.java`
- Modify: `.../__rootArtifactId__-starter/src/test/java/starter/ArchitectureDependencyTest.java`
- Modify: `.../src/main/resources/archetype-resources/README.md`

- [ ] **Step 1: Write the failing adapter tests**

Assert request construction and exact response mapping for all three methods. Also assert:

```java
when(courseFacade.getCourse(new GetCourseRequest("course-1")))
        .thenReturn(SingleResponse.of(new CourseResponse(
                "course-1", "C-1", "Course One", 3, "ACTIVE")));
assertThat(client.getCourse("course-1"))
        .isEqualTo(new EvaluationCourse("course-1", "C-1", "Course One", 3, "ACTIVE"));
```

- `success=false`, code `NOT_FOUND` maps to `NOT_FOUND`.
- `VALIDATION_FAILED` maps to `VALIDATION_FAILED`.
- `CONFLICT` maps to `BUSINESS_REJECTED`.
- `INTERNAL_ERROR` or an unknown code maps to `SERVICE_FAILURE`.
- A successful wrapper with null data maps to `CONTRACT_INCOMPATIBLE`.
- Dubbo timeout and availability failures map to `TIMEOUT` and `UNAVAILABLE`.

- [ ] **Step 2: Run the red test**

Run the web archetype integration test and expect compilation failure for the missing client classes.

- [ ] **Step 3: Implement the dev/prod adapter**

```java
@Component
@Profile({"dev", "prod"})
public class DubboEvaluationQueryClient implements EvaluationQueryPort {
    @DubboReference(group = "${organization.integrations.evaluation.course-group:course}",
            version = "${organization.integrations.evaluation.version:1.0.0}", retries = 0, check = true)
    private CourseFacade courseFacade;

    @DubboReference(group = "${organization.integrations.evaluation.exam-group:exam}",
            version = "${organization.integrations.evaluation.version:1.0.0}", retries = 0, check = true)
    private ExamFacade examFacade;

    @DubboReference(group = "${organization.integrations.evaluation.score-group:score}",
            version = "${organization.integrations.evaluation.version:1.0.0}", retries = 0, check = true)
    private ScoreFacade scoreFacade;

    // public no-arg constructor for Dubbo field injection and package-private constructor for tests.
    // Each method constructs the provider request, unwraps the response, and maps to Domain.
}
```

All provider imports use `${evaluationFacadePackage}.facade...` and remain under Infrastructure.

- [ ] **Step 4: Implement the local/test stub**

Return deterministic projections based on the requested ID. Blank or `missing-` IDs map to `NOT_FOUND`. The stub imports no provider or Dubbo type.

- [ ] **Step 5: Add profile configuration and checks**

Add the global `dubbo.consumer` timeout/retry/check baseline. Under `organization.integrations.evaluation`, local/test set `enabled: false`; dev/prod set `enabled: true`, `course-group`, `exam-group`, `score-group`, `version`, and the 3000 ms environment-backed timeout.

Update the test-profile context assertion to require `LocalEvaluationQueryStub` and reject the real client. Extend ArchUnit so Domain and Application cannot depend on `fixture.evaluation..`, while only `infrastructure.client.evaluation..` may import it.

- [ ] **Step 6: Run the green web archetype test**

```bash
bash ./mvnw -B -ntp -pl egon-cola-archetypes/egon-cola-archetype-web -am clean integration-test
```

Expected: `BUILD SUCCESS`; generated web project tests pass without Nacos, an Evaluation provider, RabbitMQ, Redis, or PostgreSQL.

- [ ] **Step 7: Commit**

```bash
git add -- egon-cola-archetypes/egon-cola-archetype-web
git commit -m "feat(archetype): integrate evaluation facade client"
```

---

### Task 7: Harden bidirectional boundaries and run final verification

**Files:**
- Modify: `egon-cola-archetypes/egon-cola-archetype-service/src/test/resources/projects/basic/verify.groovy`
- Modify: `egon-cola-archetypes/egon-cola-archetype-web/src/test/resources/projects/basic/verify.groovy`
- Modify: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/README.md`
- Modify: `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/README.md`

- [ ] **Step 1: Assert exact direct dependencies**

In each verifier, preserve the existing internal dependency lists and additionally assert:

```groovy
assert externalFacadeDependencies("infrastructure") == [expectedExternalArtifact]
modules.findAll { it != "infrastructure" }.each {
    assert externalFacadeDependencies(it).isEmpty()
}
```

Starter may receive the artifact transitively but must not directly declare or source-import it.

- [ ] **Step 2: Assert source boundaries and intentional non-use**

Scan generated Java sources and prove:

- Provider package imports occur only below `infrastructure/client/<provider>`.
- Dubbo reference imports occur only in Adapter provider code or Infrastructure client code.
- No Application, Adapter inbound, Facade, Common, Domain, or Starter source imports the external contract.
- No current Application Manage implementation mentions `OrganizationDirectoryPort` or `EvaluationQueryPort`.
- Local/test stubs import neither provider contract nor Dubbo.

- [ ] **Step 3: Validate archetype metadata and XML**

Run:

```bash
xmllint --noout \
  egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/META-INF/maven/archetype-metadata.xml \
  egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/META-INF/maven/archetype-metadata.xml
```

Expected: exit code `0` with no output.

- [ ] **Step 4: Run both targeted archetype integration tests sequentially**

```bash
bash ./mvnw -B -ntp -pl egon-cola-archetypes/egon-cola-archetype-service -am clean integration-test
bash ./mvnw -B -ntp -pl egon-cola-archetypes/egon-cola-archetype-web -am clean integration-test
```

Expected: both commands end with `BUILD SUCCESS`. Run sequentially to avoid Dubbo QoS or test-repository contention.

- [ ] **Step 5: Run the archetype reactor proof**

```bash
bash ./mvnw -B -ntp -f egon-cola-archetypes/pom.xml clean integration-test
```

Expected: `BUILD SUCCESS` for all archetypes. Do not use plain `test` as the final proof because it does not execute generated-project guards.

- [ ] **Step 6: Review the final diff and workspace scope**

```bash
git status --short
git diff --check
git diff --stat "$(git merge-base HEAD origin/main)"..HEAD
```

Expected: only the design/plan and web/service archetype paths changed. Preserve the unrelated untracked `cola-samples/` directory.

- [ ] **Step 7: Commit the final verifier and documentation hardening**

```bash
git add -- \
  egon-cola-archetypes/egon-cola-archetype-service \
  egon-cola-archetypes/egon-cola-archetype-web
git commit -m "test(archetype): enforce facade client boundaries"
```

## Completion Checklist

- [ ] Both archetypes require explicit external Facade coordinates and Java base package.
- [ ] Only Infrastructure directly depends on and imports the provider Facade artifact.
- [ ] Domain owns the ports, projections, and stable failure categories without framework types.
- [ ] local/test selects deterministic stubs and starts no external client.
- [ ] dev/prod selects real Dubbo clients with exact groups/version, 3000 ms timeout, zero retries, and explicit failures.
- [ ] No current Application use case calls either port.
- [ ] Both fixture-backed generated projects compile and test.
- [ ] Existing Facade contracts and Flyway migrations remain unchanged.
- [ ] Both targeted archetype IT commands and the archetype reactor `clean integration-test` pass.
- [ ] No runtime application was started.
