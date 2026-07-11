# Student Management Evaluation Service Archetype Alignment Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Rebuild `egon-cola-archetype-service` so it generates one evaluation-only, seven-module Service Project with complete course, schedule, exam, paper, score, Dubbo Triple, RabbitMQ, JPA/Flyway, management HTTP, validation, CI, and architecture enforcement.

**Architecture:** Preserve the exact Maven graph `domain -> common`, `application -> domain`, `infrastructure -> domain`, `adapter -> application,facade`, and `starter -> adapter,infrastructure`. Application owns Commands, Queries, and Results; Domain owns business behavior plus Repository and Publisher interfaces; Infrastructure implements JPA and RabbitMQ; Adapter implements Dubbo Facades and RabbitMQ consumers. Build the replacement beside the current `examing` sample, keep the archetype green after each task, and remove the old sample only after the new vertical flows are covered.

**Tech Stack:** Java 21, Spring Boot 3.5.16, Dubbo 3.3.6 Triple, Spring AMQP/RabbitMQ, Spring Data JPA, Flyway, H2, PostgreSQL, MapStruct Plus 1.5.1, Jakarta Validation, JUnit 5, Mockito, Spring Boot Test, ArchUnit 1.3.0, Maven 3.9.14 Wrapper, Maven Archetype Plugin, GitHub Actions.

---

## Execution Constraints

- Execute in an isolated worktree created at implementation time with `superpowers:using-git-worktrees`.
- Restrict implementation changes to `egon-cola-archetypes/egon-cola-archetype-service`, the generated-service section of `.github/workflows/ci_by_multiply_java_versions.yaml`, and files explicitly listed in this plan.
- Do not modify `egon-cola-archetypes/egon-cola-archetype-web`, `egon-cola-archetype-light`, `cola-samples`, or either approved spec.
- Do not add `student-management-organization-facade`, Organization Domain ports, Organization stubs, Organization Dubbo clients, or cross-Project fixtures. Those remain deferred to `docs/superpowers/specs/2026-07-10-evaluation-organization-facade-integration-design.md`.
- Never edit, rename, move, reformat, or delete `V1__init_student_management_evaluation.sql`.
- Add exactly one migration file: `V2__align_evaluation_course_exam_domain.sql`.
- Keep RabbitMQ semantics basic: no generated idempotency store, custom retry interceptor, dead-letter topology, transactional outbox, or exactly-once claim.
- Keep `local` and `test` independent of RabbitMQ, Nacos, PostgreSQL, and an external Dubbo registry.
- Keep HTTP management-only. Do not generate Controller, Web Filter, GraphQL, Adapter `vo`, WebFlux, or H2 browser-console behavior.
- Do not start the generated Spring Boot application or run a container. Spring test contexts and Docker image builds are allowed.
- Use path-scoped staging and one commit per task. Never stage unrelated workspace changes.

## Generated File Map

The final generated Project keeps these module roots:

```text
student-management-evaluation
|-- pom.xml
|-- README.md
|-- Dockerfile
|-- .dockerignore
|-- .gitignore
|-- .gitattributes
|-- lombok.config
|-- mvnw
|-- mvnw.cmd
|-- .mvn/wrapper/maven-wrapper.properties
|-- .github/workflows/ci.yml
|-- student-management-evaluation-common
|-- student-management-evaluation-facade
|-- student-management-evaluation-domain
|-- student-management-evaluation-application
|-- student-management-evaluation-infrastructure
|-- student-management-evaluation-adapter
`-- student-management-evaluation-starter
```

The Java source roots use this final structure:

```text
common
|-- constants
|-- enums
|-- exceptions
`-- utils

facade
|-- api
|-- dto
|   |-- course
|   `-- exam
|-- enums
|-- exceptions
`-- utils

domain
|-- common
|-- entities/{course,exam}
|-- aggregates/{course,exam}
|-- vos/{course,exam}
|-- service/{course,course/impl,exam,exam/impl}
|-- repos/{course,exam}
|-- event/{course,exam}
|-- validators/{course,exam}
`-- enums/{course,exam}

application
|-- manage/{course,course/impl,exam,exam/impl}
|-- command/{course,exam}
|-- query/{course,exam}
|-- result/{course,exam}
|-- converter/{course,exam}
|-- validators/{course,exam}
|-- assemblers/{course,exam}
|-- exceptions
`-- config

infrastructure
|-- repo/course/{impl,po,jpa,converter}
|-- repo/exam/{impl,po,jpa,converter}
|-- mq/{course,exam,message}
|-- validators
|-- aop
`-- config

adapter
|-- facade/impl/{course,exam}
|-- mq/{course,exam}
|-- dto/{course,exam}
|-- converter/{course,exam}
|-- validators/{course,exam}
`-- handler

starter
`-- config/{async,encryption}
```

Do not generate Java `.gitkeep` files. Use `package-info.java` for intentional packages and `.gitkeep` only for an otherwise empty resource directory that Maven or the archetype descriptor must preserve.

## Required Package Documentation Manifest

`verify.groovy` must require `package-info.java` in every package in this exact list:

```text
common
common/constants
common/enums
common/exceptions
common/utils
facade
facade/api
facade/dto
facade/dto/course
facade/dto/exam
facade/enums
facade/exceptions
facade/utils
domain
domain/common
domain/entities
domain/entities/course
domain/entities/exam
domain/aggregates
domain/aggregates/course
domain/aggregates/exam
domain/vos
domain/vos/course
domain/vos/exam
domain/service
domain/service/course
domain/service/course/impl
domain/service/exam
domain/service/exam/impl
domain/repos
domain/repos/course
domain/repos/exam
domain/event
domain/event/course
domain/event/exam
domain/validators
domain/validators/course
domain/validators/exam
domain/enums
domain/enums/course
domain/enums/exam
application
application/manage
application/manage/course
application/manage/course/impl
application/manage/exam
application/manage/exam/impl
application/command
application/command/course
application/command/exam
application/query
application/query/course
application/query/exam
application/result
application/result/course
application/result/exam
application/converter
application/converter/course
application/converter/exam
application/validators
application/validators/course
application/validators/exam
application/assemblers
application/assemblers/course
application/assemblers/exam
application/exceptions
application/config
infrastructure
infrastructure/repo
infrastructure/repo/course
infrastructure/repo/course/impl
infrastructure/repo/course/po
infrastructure/repo/course/jpa
infrastructure/repo/course/converter
infrastructure/repo/exam
infrastructure/repo/exam/impl
infrastructure/repo/exam/po
infrastructure/repo/exam/jpa
infrastructure/repo/exam/converter
infrastructure/mq
infrastructure/mq/course
infrastructure/mq/exam
infrastructure/mq/message
infrastructure/validators
infrastructure/aop
infrastructure/config
adapter
adapter/facade
adapter/facade/impl
adapter/facade/impl/course
adapter/facade/impl/exam
adapter/mq
adapter/mq/course
adapter/mq/exam
adapter/dto
adapter/dto/course
adapter/dto/exam
adapter/converter
adapter/converter/course
adapter/converter/exam
adapter/validators
adapter/validators/course
adapter/validators/exam
adapter/handler
starter
starter/config
starter/config/async
starter/config/encryption
```

Each file contains short package Javadoc that states responsibility and allowed generated-module dependencies. Tasks 2 through 11 add package docs with their source changes; Task 12 verifies the manifest explicitly.

## Canonical Boundary Signatures

Use these signatures consistently. Do not reintroduce `ExamResult`, `examing`, `application.client`, Domain-to-Infrastructure imports, or Adapter-to-Domain imports.

```java
// Domain course repositories and publisher
Course save(Course course);
Optional<Course> findById(CourseId courseId);
Optional<Course> findByCode(CourseCode courseCode);
boolean existsByCode(CourseCode courseCode);
Page<Course> findPage(int currentPage, int pageSize);

CourseSchedule save(CourseSchedule schedule);
List<CourseSchedule> findOverlapping(
        CourseId courseId, String classId, Instant startsAt, Instant endsAt);

void courseScheduled(CourseSchedule schedule);

// Domain exam repositories and publisher
Exam save(Exam exam);
Optional<Exam> findById(ExamId examId);

ExamPaper save(ExamPaper paper);
Optional<ExamPaper> findByExamId(ExamId examId);

Score save(Score score);
Optional<Score> findById(String scoreId);
boolean existsByExamIdAndStudentId(ExamId examId, String studentId);
Page<Score> findPageByExamId(ExamId examId, int currentPage, int pageSize);

void examPublished(Exam exam, ExamPaper paper);
void scoreRecorded(Score score);
```

Domain Service interfaces and implementations use these methods:

```java
// CourseDomainService
Course createCourse(String id, CourseCode code, String name, int credit);
CourseSchedule scheduleCourse(
        String id,
        Course course,
        String classId,
        Instant startsAt,
        Instant endsAt,
        List<CourseSchedule> overlaps);

// ExamDomainService
Exam createExam(
        String id,
        Course course,
        String title,
        Instant startsAt,
        Instant endsAt);
ExamPaper attachPaper(String id, Exam exam, String title, int totalPoints);
Exam publishExam(Exam exam, ExamPaper paper);

// ScoreDomainService
Score recordScore(
        String id,
        Exam exam,
        ExamPaper paper,
        String studentId,
        int points,
        boolean duplicate);
```

Application boundaries use Java records and never expose Domain objects:

```java
// Course commands and queries
record CreateCourseCommand(String code, String name, int credit) {}
record ScheduleCourseCommand(
        String courseId,
        String classId,
        Instant startsAt,
        Instant endsAt) {}
record GetCourseQuery(String courseId) {}
record PageCourseQuery(int currentPage, int pageSize) {}

// Exam commands and queries
record CreateExamCommand(
        String courseId,
        String title,
        Instant startsAt,
        Instant endsAt) {}
record AttachExamPaperCommand(String examId, String title, int totalPoints) {}
record PublishExamCommand(String examId) {}
record RecordScoreCommand(String examId, String studentId, int points) {}
record GetExamQuery(String examId) {}
record GetScoreQuery(String scoreId) {}
record PageScoreQuery(String examId, int currentPage, int pageSize) {}

// Application results
record CourseResult(String id, String code, String name, int credit, String status) {}
record CourseScheduleResult(
        String id,
        String courseId,
        String classId,
        Instant startsAt,
        Instant endsAt,
        String status) {}
record ExamDetailResult(
        String id,
        String courseId,
        String title,
        Instant startsAt,
        Instant endsAt,
        String status) {}
record ExamPaperResult(String id, String examId, String title, int totalPoints, String status) {}
record ScoreResult(
        String id,
        String examId,
        String courseId,
        String studentId,
        int points,
        String status) {}
```

Application Manage interfaces are:

```java
// CourseManage
CourseResult create(CreateCourseCommand command);
CourseScheduleResult schedule(ScheduleCourseCommand command);
CourseResult get(GetCourseQuery query);
Page<CourseResult> page(PageCourseQuery query);

// ExamManage
ExamDetailResult create(CreateExamCommand command);
ExamPaperResult attachPaper(AttachExamPaperCommand command);
ExamDetailResult publish(PublishExamCommand command);
ExamDetailResult get(GetExamQuery query);

// ScoreManage
ScoreResult record(RecordScoreCommand command);
ScoreResult get(GetScoreQuery query);
Page<ScoreResult> page(PageScoreQuery query);
```

Facade interfaces use Facade-owned models only:

```java
// CourseFacade
SingleResponse<CourseResponse> createCourse(CreateCourseRequest request);
SingleResponse<CourseScheduleResponse> scheduleCourse(ScheduleCourseRequest request);
SingleResponse<CourseResponse> getCourse(GetCourseRequest request);
SingleResponse<PageResponse<CourseResponse>> pageCourses(PageCourseRequest request);

// ExamFacade
SingleResponse<ExamResponse> createExam(CreateExamRequest request);
SingleResponse<ExamPaperResponse> attachPaper(AttachExamPaperRequest request);
SingleResponse<ExamResponse> publishExam(PublishExamRequest request);
SingleResponse<ExamResponse> getExam(GetExamRequest request);

// ScoreFacade
SingleResponse<ScoreResponse> recordScore(RecordScoreRequest request);
SingleResponse<ScoreResponse> getScore(GetScoreRequest request);
SingleResponse<PageResponse<ScoreResponse>> pageScores(PageScoreRequest request);
```

RabbitMQ boundaries are intentionally small:

```java
// Adapter inbound message
record RecordScoreMessage(
        String messageId,
        String examId,
        String studentId,
        int points,
        Instant occurredAt) {}

// Infrastructure outbound messages
record CourseScheduledMessage(
        String scheduleId, String courseId, String classId, Instant startsAt, Instant endsAt) {}
record ExamPublishedMessage(String examId, String courseId, String paperId, Instant publishedAt) {}
record ScoreRecordedMessage(
        String scoreId, String examId, String courseId, String studentId, int points) {}
```

Use `String` database identifiers, `Instant` persisted timestamps, `Instant` schedule/exam windows, one-based page numbers, and stable ordering by `createdAt DESC, id ASC`.

## Incremental Compatibility Rule

Tasks 2 through 10 add the replacement beside the current sample so every task can end with a green archetype integration test. Temporary compatibility is limited to:

- The old `examing` source remains until Task 11.
- The old persistence-shaped Domain Client interfaces remain until Task 11.
- Existing Course methods may keep deprecated overloads used only by old code until Task 11.
- New verifiers are added task-by-task only when the matching files exist.

Task 11 removes every temporary compatibility path in one cutover. No deprecated type remains in the final generated Project.

### Task 1: Prepare Module Dependencies And Test Harnesses

**Files:**
- Modify: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-facade/pom.xml`
- Modify: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-domain/pom.xml`
- Modify: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-application/pom.xml`
- Modify: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/pom.xml`
- Modify: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-adapter/pom.xml`
- Modify: `egon-cola-archetypes/egon-cola-archetype-service/src/test/resources/projects/basic/verify.groovy`

- [ ] **Step 1: Add failing dependency assertions**

Extend the existing parsed-POM assertions:

```groovy
assertDependency(adapterDependencies, "spring-boot-starter-amqp")
assertDependency(adapterDependencies, "spring-boot-starter-test")
assertDependency(infrastructureDependencies, "spring-boot-starter-amqp")
assertDependency(infrastructureDependencies, "flyway-database-postgresql")
assertDependency(infrastructureDependencies, "spring-boot-starter-test")
assertDependency(applicationDependencies, "spring-boot-starter-test")
assertDependency(domainDependencies, "junit-jupiter")
assertDependency(facadeDependencies, "junit-jupiter")

assert modulePomDependencies("facade") == []
assert modulePomDependencies("domain") == ["common"]
assert modulePomDependencies("application") == ["domain"]
assert modulePomDependencies("infrastructure") == ["domain"]
assert modulePomDependencies("adapter") == ["application", "facade"]
assert modulePomDependencies("starter") == ["adapter", "infrastructure"]
```

Do not weaken the exact generated-module graph while adding external libraries.

- [ ] **Step 2: Run the archetype integration test and confirm the assertions fail**

Run:

```bash
bash ./mvnw -B -ntp \
  -pl egon-cola-archetypes/egon-cola-archetype-service \
  -am clean integration-test
```

Expected: FAIL in `verify.groovy` because AMQP, Flyway PostgreSQL, and per-module test dependencies are absent.

- [ ] **Step 3: Add the minimal dependencies to their owning modules**

Facade and Domain receive JUnit only:

```xml
<dependency>
    <groupId>org.junit.jupiter</groupId>
    <artifactId>junit-jupiter</artifactId>
    <scope>test</scope>
</dependency>
```

Application receives Spring Boot Test for Mockito and Spring transaction tests:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>
```

Adapter receives Spring AMQP and Spring Boot Test:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-amqp</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>
```

Infrastructure receives Spring AMQP, Flyway PostgreSQL, and Spring Boot Test:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-amqp</artifactId>
</dependency>
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-database-postgresql</artifactId>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>
```

Do not add AMQP to Domain, Application, Facade, or Starter directly.

- [ ] **Step 4: Run the integration test and confirm the generated Project still passes**

Run the Task 1 Step 2 command again.

Expected: `BUILD SUCCESS`; the current generated sample still passes while the replacement dependencies are available.

- [ ] **Step 5: Commit the dependency baseline**

```bash
git add -- \
  egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-facade/pom.xml \
  egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-domain/pom.xml \
  egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-application/pom.xml \
  egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/pom.xml \
  egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-adapter/pom.xml \
  egon-cola-archetypes/egon-cola-archetype-service/src/test/resources/projects/basic/verify.groovy
git diff --cached --check
git commit -m "build(archetype): prepare evaluation alignment dependencies"
```

### Task 2: Add Common Foundations And Facade Contracts

**Files:**
- Create: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-common/src/main/java/common/constants/EvaluationConstants.java`
- Create: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-common/src/main/java/common/enums/YesNoEnum.java`
- Create: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-common/src/main/java/common/exceptions/EvaluationError.java`
- Create: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-common/src/main/java/common/exceptions/EvaluationErrorCode.java`
- Create: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-common/src/main/java/common/exceptions/EvaluationBizException.java`
- Create: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-common/src/main/java/common/exceptions/EvaluationNotFoundException.java`
- Create: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-common/src/main/java/common/utils/EvaluationIdUtils.java`
- Modify: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-common/pom.xml`
- Create: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-facade/src/main/java/facade/api/ExamFacade.java`
- Create: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-facade/src/main/java/facade/api/ScoreFacade.java`
- Create: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-facade/src/main/java/facade/dto/course/{GetCourseRequest,PageCourseRequest,ScheduleCourseRequest,CourseResponse,CourseScheduleResponse}.java`
- Create: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-facade/src/main/java/facade/dto/exam/{CreateExamRequest,AttachExamPaperRequest,PublishExamRequest,GetExamRequest,RecordScoreRequest,GetScoreRequest,PageScoreRequest,ExamResponse,ExamPaperResponse,ScoreResponse}.java`
- Create: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-facade/src/main/java/facade/enums/{EvaluationFacadeStatus,EvaluationFacadeErrorCode}.java`
- Create: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-facade/src/main/java/facade/exceptions/EvaluationFacadeException.java`
- Create: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-facade/src/main/java/facade/utils/EvaluationFacadeAssert.java`
- Create: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-facade/src/test/java/facade/EvaluationFacadeContractTest.java`
- Modify: `egon-cola-archetypes/egon-cola-archetype-service/src/test/resources/projects/basic/verify.groovy`

- [ ] **Step 1: Write failing Common and Facade contract tests**

Create `EvaluationFacadeContractTest` with explicit reflection checks:

```java
package ${package}.facade;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import ${package}.facade.dto.course.ScheduleCourseRequest;
import ${package}.facade.dto.exam.RecordScoreRequest;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class EvaluationFacadeContractTest {
    @Test
    void shouldExposeSerializableProtocolOwnedRequests() throws Exception {
        ScheduleCourseRequest schedule = new ScheduleCourseRequest(
                "course-1", "class-1", Instant.parse("2026-09-01T01:00:00Z"),
                Instant.parse("2026-09-01T02:00:00Z"));
        assertEquals("course-1", schedule.courseId());
        assertNotNull(ScheduleCourseRequest.class.getRecordComponents());
    }

    @Test
    void shouldKeepScoreValidationOnFacadeRequest() throws Exception {
        var studentId = RecordScoreRequest.class.getRecordComponents()[1];
        var points = RecordScoreRequest.class.getRecordComponents()[2];
        assertNotNull(studentId.getAnnotation(NotBlank.class));
        assertEquals(0, points.getAnnotation(Min.class).value());
        assertEquals(100, points.getAnnotation(Max.class).value());
    }
}
```

Add verifier assertions for concrete Facade enum, exception, utility, request, and response files. Assert no new Facade source imports `${package}.common`, `${package}.domain`, Spring, or Dubbo.

- [ ] **Step 2: Run the integration test and verify the generated test fails**

Run the Task 1 integration-test command.

Expected: FAIL because the new Facade contracts and Common foundations do not exist.

- [ ] **Step 3: Implement Common without an unused component dependency**

Remove `egon-cola-component-common-core` from the generated Common POM and implement the Common API with JDK types:

```java
package ${package}.common.exceptions;

public enum EvaluationErrorCode implements EvaluationError {
    COURSE_NOT_FOUND,
    COURSE_CODE_DUPLICATED,
    SCHEDULE_CONFLICT,
    EXAM_NOT_FOUND,
    EXAM_PAPER_NOT_FOUND,
    EXAM_NOT_PUBLISHABLE,
    SCORE_NOT_FOUND,
    SCORE_DUPLICATED,
    VALIDATION_FAILED,
    INFRASTRUCTURE_FAILURE
}
```

```java
package ${package}.common.exceptions;

public class EvaluationBizException extends RuntimeException {
    private final EvaluationError code;

    public EvaluationBizException(EvaluationError code, String message) {
        super(message);
        this.code = code;
    }

    public EvaluationError getCode() {
        return code;
    }
}
```

`EvaluationNotFoundException` extends `EvaluationBizException`, and `EvaluationIdUtils.nextId()` returns `UUID.randomUUID().toString()`. `EvaluationConstants` owns the course-code and display-name length limits used by Domain validators. `YesNoEnum` is retained only as the shared two-state value used by aggregate activation methods; do not generate an unused marker enum.

- [ ] **Step 4: Implement the self-contained Facade contract**

Use records for requests and normal serializable response classes. The score request is exactly:

```java
package ${package}.facade.dto.exam;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record RecordScoreRequest(
        @NotBlank String examId,
        @NotBlank String studentId,
        @Min(0) @Max(100) int points) {
}
```

Implement `ExamFacade` and `ScoreFacade` using the canonical signatures. Keep the existing `CourseFacade` unchanged until Task 9 so the current provider still compiles.

`EvaluationFacadeAssert` throws `EvaluationFacadeException` with `EvaluationFacadeErrorCode.VALIDATION_FAILED`; it must not import Common.

- [ ] **Step 5: Run the integration test and verify the new contract passes**

Run the Task 1 integration-test command.

Expected: `BUILD SUCCESS`; `EvaluationFacadeContractTest` passes and current RPC providers still compile.

- [ ] **Step 6: Commit Common and Facade contracts**

```bash
git add -- \
  egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-common \
  egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-facade \
  egon-cola-archetypes/egon-cola-archetype-service/src/test/resources/projects/basic/verify.groovy
git diff --cached --check
git commit -m "feat(archetype): add evaluation facade contracts"
```

### Task 3: Implement The Course And Schedule Domain

**Files:**
- Modify: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-domain/src/main/java/domain/entities/course/Course.java`
- Create: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-domain/src/main/java/domain/entities/course/CourseSchedule.java`
- Create: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-domain/src/main/java/domain/aggregates/course/CourseAggregate.java`
- Create: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-domain/src/main/java/domain/common/{EvaluationDomainErrorCode,EvaluationDomainException,EvaluationPortException}.java`
- Create: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-domain/src/main/java/domain/vos/course/{CourseId,CourseCode}.java`
- Create: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-domain/src/main/java/domain/enums/course/{CourseStatus,CourseScheduleStatus}.java`
- Create: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-domain/src/main/java/domain/validators/course/CourseDomainValidator.java`
- Create: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-domain/src/main/java/domain/service/course/CourseDomainService.java`
- Create: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-domain/src/main/java/domain/service/course/impl/CourseDomainServiceImpl.java`
- Modify: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-domain/src/main/java/domain/repos/course/CourseRepository.java`
- Create: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-domain/src/main/java/domain/repos/course/CourseScheduleRepository.java`
- Create: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-domain/src/main/java/domain/event/course/CourseEventPublisher.java`
- Create: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-domain/src/test/java/domain/course/{CourseDomainServiceTest,CourseAggregateTest}.java`
- Modify: `egon-cola-archetypes/egon-cola-archetype-service/src/test/resources/projects/basic/verify.groovy`

- [ ] **Step 1: Write failing pure Domain tests**

Use fixed timestamps and no Spring context:

```java
package ${package}.domain.course;

import static org.junit.jupiter.api.Assertions.assertThrows;

import ${package}.domain.common.EvaluationDomainException;
import ${package}.domain.entities.course.Course;
import ${package}.domain.entities.course.CourseSchedule;
import ${package}.domain.service.course.impl.CourseDomainServiceImpl;
import ${package}.domain.vos.course.CourseCode;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class CourseDomainServiceTest {
    private final CourseDomainServiceImpl service = new CourseDomainServiceImpl();

    @Test
    void shouldRejectOverlappingSchedule() {
        Course course = service.createCourse("course-1", new CourseCode("MATH-101"), "Math", 3);
        CourseSchedule existing = service.scheduleCourse(
                "schedule-1", course, "class-1",
                Instant.parse("2026-09-01T01:00:00Z"),
                Instant.parse("2026-09-01T02:00:00Z"), List.of());

        assertThrows(EvaluationDomainException.class, () -> service.scheduleCourse(
                "schedule-2", course, "class-1",
                Instant.parse("2026-09-01T01:30:00Z"),
                Instant.parse("2026-09-01T02:30:00Z"), List.of(existing)));
    }
}
```

`CourseAggregateTest` covers inactive-course scheduling, blank class ID, end-before-start, and non-overlapping schedules.

- [ ] **Step 2: Run the integration test and confirm Domain tests fail**

Run the Task 1 integration-test command.

Expected: FAIL because CourseSchedule, CourseCode, the aggregate, and Domain Service do not exist.

- [ ] **Step 3: Implement Course value objects, entities, aggregate, and validator**

`CourseCode` normalizes to uppercase and rejects blank values:

```java
public record CourseCode(String value) {
    public CourseCode {
        if (value == null || value.isBlank()) {
            throw new EvaluationDomainException(
                    EvaluationDomainErrorCode.VALIDATION_FAILED,
                    "course code must not be blank");
        }
        value = value.trim().toUpperCase(Locale.ROOT);
    }
}
```

`CourseSchedule.overlaps` uses the half-open rule:

```java
public boolean overlaps(Instant candidateStart, Instant candidateEnd) {
    return startsAt.isBefore(candidateEnd) && candidateStart.isBefore(endsAt);
}
```

Keep the old `Course.create(String id, String name, int credit)` overload only until Task 11. The new constructor path requires `CourseCode` and creates an `ACTIVE` course.

`CourseId.newId()` delegates to Common's `EvaluationIdUtils.nextId()`. `EvaluationDomainErrorCode` implements Common's `EvaluationError`; `EvaluationDomainException` extends `EvaluationBizException`. `EvaluationPortException` is a technology-neutral Domain exception with an operation name and cause, so Infrastructure can translate JPA/AMQP failures without leaking those exception types upward. Application catches only these Domain-owned exception types and never imports Common.

- [ ] **Step 4: Implement Domain Services and technical port interfaces**

`CourseDomainService` and `CourseDomainServiceImpl` use the canonical signatures. `CourseRepository`, `CourseScheduleRepository`, and `CourseEventPublisher` contain method declarations only and import no Spring, JPA, AMQP, Dubbo, or Infrastructure type.

Add verifier checks that Publisher and Repository source contains no `@Component`, `RabbitTemplate`, `JpaRepository`, or implementation method body.

- [ ] **Step 5: Run the integration test and verify the Course Domain passes**

Run the Task 1 integration-test command.

Expected: `BUILD SUCCESS`; new Domain tests pass and the old sample remains temporarily compatible.

- [ ] **Step 6: Commit the Course Domain**

```bash
git add -- \
  egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-domain \
  egon-cola-archetypes/egon-cola-archetype-service/src/test/resources/projects/basic/verify.groovy
git diff --cached --check
git commit -m "feat(archetype): add course scheduling domain"
```

### Task 4: Implement The Exam, Paper, And Score Domain

**Files:**
- Create: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-domain/src/main/java/domain/entities/exam/{Exam,ExamPaper,Score}.java`
- Create: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-domain/src/main/java/domain/aggregates/exam/{ExamAggregate,ScoreAggregate}.java`
- Create: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-domain/src/main/java/domain/vos/exam/{ExamId,ScoreValue}.java`
- Create: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-domain/src/main/java/domain/enums/exam/{ExamStatus,ExamPaperStatus,ScoreStatus}.java`
- Create: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-domain/src/main/java/domain/validators/exam/{ExamDomainValidator,ScoreDomainValidator}.java`
- Create: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-domain/src/main/java/domain/service/exam/{ExamDomainService,ScoreDomainService}.java`
- Create: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-domain/src/main/java/domain/service/exam/impl/{ExamDomainServiceImpl,ScoreDomainServiceImpl}.java`
- Create: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-domain/src/main/java/domain/repos/exam/{ExamRepository,ExamPaperRepository,ScoreRepository}.java`
- Create: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-domain/src/main/java/domain/event/exam/ExamEventPublisher.java`
- Create: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-domain/src/test/java/domain/exam/{ExamDomainServiceTest,ExamAggregateTest,ScoreDomainServiceTest}.java`
- Modify: `egon-cola-archetypes/egon-cola-archetype-service/src/test/resources/projects/basic/verify.groovy`

- [ ] **Step 1: Write failing Exam and Score Domain tests**

Create tests that cover the approved rules:

```java
@Test
void shouldRequirePaperBeforePublishingExam() {
    Course course = Course.create(
            "course-1", new CourseCode("MATH-101"), "Math", 3, Instant.EPOCH);
    Exam exam = examService.createExam(
            "exam-1", course, "Midterm",
            Instant.parse("2026-10-01T01:00:00Z"),
            Instant.parse("2026-10-01T03:00:00Z"));

    assertThrows(EvaluationDomainException.class, () -> examService.publishExam(exam, null));
}

@Test
void shouldRejectDuplicateOrOutOfRangeScore() {
    assertThrows(EvaluationDomainException.class, () -> scoreService.recordScore(
            "score-1", publishedExam, paper, "student-1", 101, false));
    assertThrows(EvaluationDomainException.class, () -> scoreService.recordScore(
            "score-2", publishedExam, paper, "student-1", 90, true));
}
```

Also test invalid exam windows, non-positive paper totals, score above paper total, and Score status creation.

- [ ] **Step 2: Run the integration test and verify the new Domain tests fail**

Run the Task 1 integration-test command.

Expected: FAIL because the `domain.exam` model does not exist.

- [ ] **Step 3: Implement the Exam aggregate and publication rule**

`ExamStatus` values are `DRAFT`, `PUBLISHED`, and `CLOSED`. `ExamPaperStatus` values are `DRAFT` and `PUBLISHED`. `ExamAggregate.publish()` requires a paper and returns an Exam in `PUBLISHED` state plus a published paper.

Implement `ExamDomainService` and `ExamDomainServiceImpl` with the canonical signatures. Domain code contains no Spring annotation.

- [ ] **Step 4: Implement Score creation and technical ports**

`ScoreValue` validates `points >= 0`. `ScoreDomainServiceImpl` additionally enforces `points <= paper.totalPoints()` and rejects the `duplicate` flag.

`ExamId.newId()` delegates to Common's `EvaluationIdUtils.nextId()`. Exam and Score rules throw `EvaluationDomainException` with `EvaluationDomainErrorCode`, not raw Common exceptions.

Create Repository and Event Publisher interfaces with the canonical methods. `ExamEventPublisher` contains only:

```java
public interface ExamEventPublisher {
    void examPublished(Exam exam, ExamPaper paper);
    void scoreRecorded(Score score);
}
```

- [ ] **Step 5: Run the integration test and verify all Domain tests pass**

Run the Task 1 integration-test command.

Expected: `BUILD SUCCESS`; both `domain.course` and `domain.exam` tests pass without a Spring context.

- [ ] **Step 6: Commit the Exam Domain**

```bash
git add -- \
  egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-domain \
  egon-cola-archetypes/egon-cola-archetype-service/src/test/resources/projects/basic/verify.groovy
git diff --cached --check
git commit -m "feat(archetype): add exam paper and score domain"
```

### Task 5: Implement Application Commands, Results, And Use Cases

**Files:**
- Modify: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-application/src/main/java/application/manage/course/CourseManage.java`
- Modify: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-application/src/main/java/application/manage/course/impl/CourseManageImpl.java`
- Create: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-application/src/main/java/application/manage/exam/{ExamManage,ScoreManage}.java`
- Create: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-application/src/main/java/application/manage/exam/impl/{ExamManageImpl,ScoreManageImpl}.java`
- Create: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-application/src/main/java/application/command/course/{CreateCourseCommand,ScheduleCourseCommand}.java`
- Create: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-application/src/main/java/application/command/exam/{CreateExamCommand,AttachExamPaperCommand,PublishExamCommand,RecordScoreCommand}.java`
- Create: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-application/src/main/java/application/query/course/{GetCourseQuery,PageCourseQuery}.java`
- Create: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-application/src/main/java/application/query/exam/{GetExamQuery,GetScoreQuery,PageScoreQuery}.java`
- Create: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-application/src/main/java/application/result/course/{CourseResult,CourseScheduleResult}.java`
- Create: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-application/src/main/java/application/result/exam/{ExamDetailResult,ExamPaperResult,ScoreResult}.java`
- Create: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-application/src/main/java/application/converter/course/CourseApplicationConverter.java`
- Create: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-application/src/main/java/application/converter/exam/ExamApplicationConverter.java`
- Create: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-application/src/main/java/application/validators/course/CourseApplicationValidator.java`
- Create: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-application/src/main/java/application/validators/exam/ExamApplicationValidator.java`
- Create: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-application/src/main/java/application/assemblers/{course/CourseAssembler,exam/ExamAssembler}.java`
- Create: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-application/src/main/java/application/exceptions/{ApplicationErrorCode,ApplicationException}.java`
- Modify: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-application/src/main/java/application/config/DomainServiceConfiguration.java`
- Create: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-application/src/test/java/application/{course/CourseManageTest,exam/ExamManageTest,exam/ScoreManageTest}.java`
- Modify: `egon-cola-archetypes/egon-cola-archetype-service/src/test/resources/projects/basic/verify.groovy`

- [ ] **Step 1: Write failing Application orchestration tests**

Use Mockito and verify Repository and Publisher calls through Domain interfaces:

```java
@ExtendWith(MockitoExtension.class)
class ScoreManageTest {
    @Mock ExamRepository examRepository;
    @Mock ExamPaperRepository paperRepository;
    @Mock ScoreRepository scoreRepository;
    @Mock ExamEventPublisher eventPublisher;

    @Test
    void shouldPersistAndPublishRecordedScore() {
        when(examRepository.findById(new ExamId("exam-1"))).thenReturn(Optional.of(publishedExam));
        when(paperRepository.findByExamId(new ExamId("exam-1"))).thenReturn(Optional.of(paper));
        when(scoreRepository.existsByExamIdAndStudentId(new ExamId("exam-1"), "student-1"))
                .thenReturn(false);
        when(scoreRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ScoreResult result = manage.record(new RecordScoreCommand("exam-1", "student-1", 90));

        assertEquals(90, result.points());
        verify(scoreRepository).save(any(Score.class));
        verify(eventPublisher).scoreRecorded(any(Score.class));
    }
}
```

`CourseManageTest` covers duplicate code, stable page request forwarding, schedule overlap lookup, persistence, and `courseScheduled`. `ExamManageTest` covers course lookup, paper lookup, publish ordering, and `examPublished`.

- [ ] **Step 2: Run the integration test and verify Application tests fail**

Run the Task 1 integration-test command.

Expected: FAIL because Commands, Results, converters, and new Manage implementations are absent.

- [ ] **Step 3: Implement Application-owned boundaries and errors**

Create every record in the canonical signatures. Application error types are independent of Common:

```java
public enum ApplicationErrorCode {
    COURSE_NOT_FOUND,
    COURSE_CODE_DUPLICATED,
    EXAM_NOT_FOUND,
    EXAM_PAPER_NOT_FOUND,
    SCORE_NOT_FOUND,
    VALIDATION_FAILED,
    BUSINESS_REJECTED,
    INFRASTRUCTURE_FAILURE
}

public final class ApplicationException extends RuntimeException {
    private final ApplicationErrorCode code;

    public ApplicationException(ApplicationErrorCode code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public ApplicationErrorCode code() {
        return code;
    }
}
```

Converters map Domain objects to Application Results with explicit constructors. Do not use Facade DTOs or import Common.

- [ ] **Step 4: Implement Course, Exam, And Score Manage classes**

Use direct Domain ports and Domain Services. The Score mutation sequence is exact:

```java
@Transactional
public ScoreResult record(RecordScoreCommand command) {
    validator.validate(command);
    ExamId examId = new ExamId(command.examId());
    Exam exam = examRepository.findById(examId)
            .orElseThrow(() -> new ApplicationException(
                    ApplicationErrorCode.EXAM_NOT_FOUND, "exam not found", null));
    ExamPaper paper = paperRepository.findByExamId(examId)
            .orElseThrow(() -> new ApplicationException(
                    ApplicationErrorCode.EXAM_PAPER_NOT_FOUND, "exam paper not found", null));
    boolean duplicate = scoreRepository.existsByExamIdAndStudentId(examId, command.studentId());
    Score score = scoreDomainService.recordScore(
            UUID.randomUUID().toString(), exam, paper,
            command.studentId(), command.points(), duplicate);
    Score saved = scoreRepository.save(score);
    eventPublisher.scoreRecorded(saved);
    return converter.toResult(saved);
}
```

Course and Exam Manage implementations follow the canonical methods and map Domain failures to `ApplicationException`. Keep old Course and old `application.manage.examing` methods only for temporary compilation until Task 11.

- [ ] **Step 5: Assemble plain Domain Service implementations in Application**

Update `DomainServiceConfiguration`:

```java
@Configuration(proxyBeanMethods = false)
public class DomainServiceConfiguration {
    @Bean
    CourseDomainService courseDomainService() {
        return new CourseDomainServiceImpl();
    }

    @Bean
    ExamDomainService examDomainService() {
        return new ExamDomainServiceImpl();
    }

    @Bean
    ScoreDomainService scoreDomainService() {
        return new ScoreDomainServiceImpl();
    }
}
```

Domain implementations remain free of Spring annotations.

- [ ] **Step 6: Run the integration test and verify Application tests pass**

Run the Task 1 integration-test command.

Expected: `BUILD SUCCESS`; new Application tests pass and source scans show no `facade`, `common`, `infrastructure`, AMQP, JPA, or Dubbo imports under Application.

- [ ] **Step 7: Commit the Application layer**

```bash
git add -- \
  egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-application \
  egon-cola-archetypes/egon-cola-archetype-service/src/test/resources/projects/basic/verify.groovy
git diff --cached --check
git commit -m "feat(archetype): add evaluation application use cases"
```

### Task 6: Add The Immutable-V1 Migration Path And Course JPA

**Files:**
- Create: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/main/resources/db/migration/V2__align_evaluation_course_exam_domain.sql`
- Modify: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/main/java/infrastructure/repo/course/po/CoursePo.java`
- Create: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/main/java/infrastructure/repo/course/po/CourseSchedulePo.java`
- Modify: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/main/java/infrastructure/repo/course/jpa/CourseJpaRepository.java`
- Create: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/main/java/infrastructure/repo/course/jpa/CourseScheduleJpaRepository.java`
- Modify: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/main/java/infrastructure/repo/course/impl/CourseRepositoryImpl.java`
- Create: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/main/java/infrastructure/repo/course/impl/CourseScheduleRepositoryImpl.java`
- Replace: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/main/java/infrastructure/repo/course/converter/{CourseConverter,CourseDomainMapper,CoursePoMapper}.java`
- Create: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/main/java/infrastructure/repo/course/converter/CourseScheduleConverter.java`
- Create: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/test/java/infrastructure/migration/EvaluationMigrationTest.java`
- Create: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/test/java/infrastructure/repo/course/{CourseRepositoryTest,CourseScheduleRepositoryTest}.java`
- Modify: `egon-cola-archetypes/egon-cola-archetype-service/src/test/resources/projects/basic/verify.groovy`

- [ ] **Step 1: Record V1 checksum and write failing migration tests**

Run and record in the implementation notes:

```bash
shasum -a 256 \
  egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/main/resources/db/migration/V1__init_student_management_evaluation.sql
```

Create `EvaluationMigrationTest` that migrates to V1, seeds data, then migrates to V2:

```java
@Test
void shouldMigrateValidV1ResultsWithoutChangingLegacyRows() throws Exception {
    String url = "jdbc:h2:mem:migration-valid;MODE=PostgreSQL;DB_CLOSE_DELAY=-1";
    Flyway.configure().dataSource(url, "sa", "").target("1").load().migrate();
    try (Connection connection = DriverManager.getConnection(url, "sa", "");
         Statement statement = connection.createStatement()) {
        statement.executeUpdate("insert into course values "
                + "('course-1','Math',3,'ACTIVE',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)");
        statement.executeUpdate("insert into exam_result values "
                + "('result-1','course-1','student-1',90,'RECORDED',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)");
    }

    Flyway.configure().dataSource(url, "sa", "").load().migrate();

    assertEquals(1, count(url, "select count(*) from exam_result where id='result-1'"));
    assertEquals(1, count(url, "select count(*) from score where id='result-1'"));
    assertEquals(1, count(url, "select count(*) from exam where id='legacy-exam-result-1'"));
}
```

Add a second test that inserts score `101` and expects V2 migration failure without deleting the V1 row.

- [ ] **Step 2: Run the integration test and verify migration tests fail**

Run the Task 1 integration-test command.

Expected: FAIL because V2 and the new schema do not exist.

- [ ] **Step 3: Create the single additive V2 migration**

Use one SQL file. Its key statements are:

```sql
alter table course add column code varchar(96);
update course set code = 'LEGACY-' || id where code is null;
alter table course alter column code set not null;
create unique index uk_course_code on course (code);

create table course_schedule (
    id varchar(64) primary key,
    course_id varchar(64) not null,
    class_id varchar(64) not null,
    starts_at timestamp not null,
    ends_at timestamp not null,
    status varchar(32) not null,
    created_at timestamp not null,
    updated_at timestamp not null,
    constraint fk_schedule_course foreign key (course_id) references course (id),
    constraint ck_schedule_window check (starts_at < ends_at)
);
create index idx_schedule_overlap
    on course_schedule (course_id, class_id, starts_at, ends_at);

create table exam (
    id varchar(160) primary key,
    course_id varchar(64) not null,
    title varchar(128) not null,
    starts_at timestamp not null,
    ends_at timestamp not null,
    status varchar(32) not null,
    created_at timestamp not null,
    updated_at timestamp not null,
    constraint fk_exam_course foreign key (course_id) references course (id),
    constraint ck_exam_window check (starts_at < ends_at)
);
create index idx_exam_course_created
    on exam (course_id, created_at, id);

create table exam_paper (
    id varchar(160) primary key,
    exam_id varchar(160) not null,
    title varchar(128) not null,
    total_points integer not null,
    status varchar(32) not null,
    created_at timestamp not null,
    updated_at timestamp not null,
    constraint fk_paper_exam foreign key (exam_id) references exam (id),
    constraint ck_paper_points check (total_points > 0)
);
create unique index uk_exam_paper_exam
    on exam_paper (exam_id);

create table score (
    id varchar(64) primary key,
    exam_id varchar(160) not null,
    course_id varchar(64) not null,
    student_id varchar(64) not null,
    points integer not null,
    status varchar(32) not null,
    created_at timestamp not null,
    updated_at timestamp not null,
    constraint fk_score_exam foreign key (exam_id) references exam (id),
    constraint fk_score_course foreign key (course_id) references course (id),
    constraint ck_score_points check (points between 0 and 100),
    constraint uk_score_exam_student unique (exam_id, student_id)
);
create index idx_score_exam_created
    on score (exam_id, created_at, id);
create index idx_score_course
    on score (course_id);
create index idx_score_student
    on score (student_id);

insert into exam (id, course_id, title, starts_at, ends_at, status, created_at, updated_at)
select 'legacy-exam-' || id, course_id, 'Legacy exam ' || id,
       created_at,
       case when updated_at > created_at
            then updated_at
            else created_at + interval '1 second'
       end,
       'CLOSED', created_at, updated_at
from exam_result;

insert into exam_paper (id, exam_id, title, total_points, status, created_at, updated_at)
select 'legacy-paper-' || id, 'legacy-exam-' || id, 'Legacy paper ' || id,
       100, 'PUBLISHED', created_at, updated_at
from exam_result;

insert into score (id, exam_id, course_id, student_id, points, status, created_at, updated_at)
select id, 'legacy-exam-' || id, course_id, student_id, score, status, created_at, updated_at
from exam_result;
```

The foreign keys reject orphan course IDs; the score check rejects invalid legacy points. Do not add another migration file.

- [ ] **Step 4: Implement Course and Schedule JPA adapters with stable ordering**

Use `CoursePo` fields matching the extended table and map `CourseCode` explicitly. Pagination is exact:

```java
Pageable pageable = PageRequest.of(
        currentPage - 1,
        pageSize,
        Sort.by(Sort.Order.desc("createdAt"), Sort.Order.asc("id")));
```

`CourseScheduleJpaRepository` declares an overlap query using:

```java
List<CourseSchedulePo> findByCourseIdAndClassIdAndStartsAtLessThanAndEndsAtGreaterThan(
        String courseId, String classId, Instant endsAt, Instant startsAt);
```

Repository implementations catch only recognized unique constraints and translate them to `EvaluationPortException`; unrelated `DataIntegrityViolationException` instances are wrapped as a generic persistence `EvaluationPortException`, never misreported as a duplicate.

- [ ] **Step 5: Run migration and JPA tests**

Run the Task 1 integration-test command.

Expected: `BUILD SUCCESS`; V1-to-V2 migration, incompatible-row failure, stable course pagination, and schedule overlap tests pass.

- [ ] **Step 6: Recheck V1 checksum and commit**

Run the Step 1 checksum command and confirm it matches the recorded value, then:

```bash
git add -- \
  egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure \
  egon-cola-archetypes/egon-cola-archetype-service/src/test/resources/projects/basic/verify.groovy
git diff --cached --check
git commit -m "feat(archetype): add evaluation schema and course persistence"
```

### Task 7: Implement Exam, Paper, And Score JPA Adapters

**Files:**
- Create: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/main/java/infrastructure/repo/exam/po/{ExamPo,ExamPaperPo,ScorePo}.java`
- Create: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/main/java/infrastructure/repo/exam/jpa/{ExamJpaRepository,ExamPaperJpaRepository,ScoreJpaRepository}.java`
- Create: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/main/java/infrastructure/repo/exam/converter/{ExamConverter,ExamPaperConverter,ScoreConverter}.java`
- Create: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/main/java/infrastructure/repo/exam/impl/{ExamRepositoryImpl,ExamPaperRepositoryImpl,ScoreRepositoryImpl}.java`
- Create: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/main/java/infrastructure/validators/EvaluationPersistenceValidator.java`
- Create: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/test/java/infrastructure/repo/exam/{ExamRepositoryTest,ExamPaperRepositoryTest,ScoreRepositoryTest}.java`
- Modify: `egon-cola-archetypes/egon-cola-archetype-service/src/test/resources/projects/basic/verify.groovy`

- [ ] **Step 1: Write failing JPA adapter tests**

Use `@DataJpaTest` plus `@Import` for converters and repository implementations. Cover:

```java
@Test
void shouldPageScoresInStableOrder() {
    repository.save(score("score-b", "exam-1", "student-2", 80, EARLIER));
    repository.save(score("score-a", "exam-1", "student-1", 90, LATER));

    Page<Score> page = repository.findPageByExamId(new ExamId("exam-1"), 1, 10);

    assertEquals(List.of("score-a", "score-b"),
            page.records().stream().map(Score::getId).toList());
}
```

Also test `findByExamId`, duplicate `(exam_id, student_id)` translation, missing rows, and Domain/JPA round trips.

- [ ] **Step 2: Run the integration test and verify JPA tests fail**

Run the Task 1 integration-test command.

Expected: FAIL because the new Exam, Paper, and Score adapters are absent.

- [ ] **Step 3: Implement persistence objects and converters**

Map every persisted timestamp to `Instant`. Use explicit semantic conversion for `ExamId`, enums, and Score points. Do not inject the generic MapStruct Plus `Converter` bean.

`ScorePo` maps to table `score`, not `exam_result`. The old table remains untouched and has no active JPA entity after Task 11.

- [ ] **Step 4: Implement repository adapters and constraint translation**

`ScoreJpaRepository` declares:

```java
boolean existsByExamIdAndStudentId(String examId, String studentId);
Page<ScorePo> findByExamId(String examId, Pageable pageable);
```

Use the same stable sort as Course pagination. `EvaluationPersistenceValidator` recognizes named constraints such as `uk_score_exam_student`; it does not convert every integrity violation into a duplicate-score error. Every JPA exception crossing the Infrastructure boundary becomes a technology-neutral `EvaluationPortException` with the original cause retained.

- [ ] **Step 5: Run the integration test and verify all JPA tests pass**

Run the Task 1 integration-test command.

Expected: `BUILD SUCCESS`; Course, Schedule, Exam, Paper, Score, and migration tests pass on H2 PostgreSQL mode.

- [ ] **Step 6: Commit Exam persistence**

```bash
git add -- \
  egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure \
  egon-cola-archetypes/egon-cola-archetype-service/src/test/resources/projects/basic/verify.groovy
git diff --cached --check
git commit -m "feat(archetype): add exam and score persistence"
```

### Task 8: Implement Basic RabbitMQ Publication And Local Fallbacks

**Files:**
- Create: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/main/java/infrastructure/mq/message/{CourseScheduledMessage,ExamPublishedMessage,ScoreRecordedMessage}.java`
- Create: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/main/java/infrastructure/mq/course/{RabbitCourseEventPublisher,LocalCourseEventPublisher}.java`
- Create: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/main/java/infrastructure/mq/exam/{RabbitExamEventPublisher,LocalExamEventPublisher}.java`
- Create: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/main/java/infrastructure/config/RabbitMqConfiguration.java`
- Create: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/test/java/infrastructure/mq/{RabbitCourseEventPublisherTest,RabbitExamEventPublisherTest,RabbitMqConfigurationTest}.java`
- Modify: `egon-cola-archetypes/egon-cola-archetype-service/src/test/resources/projects/basic/verify.groovy`

- [ ] **Step 1: Write failing publisher and configuration tests**

Publisher tests mock `RabbitTemplate`:

```java
@Test
void shouldPublishScoreRecordedMessage() {
    RabbitTemplate template = mock(RabbitTemplate.class);
    RabbitExamEventPublisher publisher = new RabbitExamEventPublisher(
            template, "evaluation.events", "score.recorded");

    publisher.scoreRecorded(score);

    verify(template).convertAndSend(
            eq("evaluation.events"), eq("score.recorded"),
            argThat(message -> ((ScoreRecordedMessage) message).scoreId().equals(score.getId())));
}
```

`RabbitMqConfigurationTest` starts `ApplicationContextRunner` with
`app.integrations.rabbitmq.enabled=true` and a mocked `ConnectionFactory`. Assert one TopicExchange, one score-command Queue, one Binding, one JSON converter, and real publisher beans. Keep listener auto-startup false; do not open a broker connection.

- [ ] **Step 2: Run the integration test and confirm AMQP tests fail**

Run the Task 1 integration-test command.

Expected: FAIL because Rabbit publishers and configuration are absent.

- [ ] **Step 3: Implement real and local Publisher adapters**

Real publishers are conditional on `app.integrations.rabbitmq.enabled=true` and use only `RabbitTemplate`. Local publishers are selected when the property is false or missing and return normally without broker access. Synchronous `AmqpException` failures are wrapped in `EvaluationPortException`; AMQP exception types never cross the Infrastructure boundary.

The real score method is:

```java
@Override
public void scoreRecorded(Score score) {
    ScoreRecordedMessage message = new ScoreRecordedMessage(
            score.getId(), score.getExamId().value(), score.getCourseId().value(),
            score.getStudentId(), score.getPoints().value());
    rabbitTemplate.convertAndSend(exchange, scoreRecordedRoutingKey, message);
}
```

Do not add retry, confirm, outbox, dead-letter, or idempotency code.

- [ ] **Step 4: Implement basic RabbitMQ topology**

`RabbitMqConfiguration` defines only:

```java
@Bean
TopicExchange evaluationExchange(
        @Value("${symbol_dollar}{app.integrations.rabbitmq.exchange}") String name) {
    return new TopicExchange(name, true, false);
}

@Bean
Queue recordScoreCommandQueue(
        @Value("${symbol_dollar}{app.integrations.rabbitmq.score-command-queue}") String name) {
    return QueueBuilder.durable(name).build();
}

@Bean
Binding recordScoreCommandBinding(
        Queue recordScoreCommandQueue,
        TopicExchange evaluationExchange,
        @Value("${symbol_dollar}{app.integrations.rabbitmq.score-command-routing-key}") String routingKey) {
    return BindingBuilder.bind(recordScoreCommandQueue).to(evaluationExchange).with(routingKey);
}
```

No dead-letter arguments or listener retry configuration are permitted.

- [ ] **Step 5: Run the integration test and verify AMQP tests pass externally free**

Run the Task 1 integration-test command.

Expected: `BUILD SUCCESS`; publisher and enabled-configuration tests pass without RabbitMQ.

- [ ] **Step 6: Commit RabbitMQ Infrastructure**

```bash
git add -- \
  egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure \
  egon-cola-archetypes/egon-cola-archetype-service/src/test/resources/projects/basic/verify.groovy
git diff --cached --check
git commit -m "feat(archetype): add basic evaluation rabbitmq adapters"
```

### Task 9: Implement Dubbo Facades And The RabbitMQ Command Consumer

**Files:**
- Modify: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-facade/src/main/java/facade/api/CourseFacade.java`
- Create: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-adapter/src/main/java/adapter/facade/impl/course/CourseFacadeImpl.java`
- Create: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-adapter/src/main/java/adapter/facade/impl/exam/{ExamFacadeImpl,ScoreFacadeImpl}.java`
- Create: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-adapter/src/main/java/adapter/converter/course/CourseFacadeConverter.java`
- Create: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-adapter/src/main/java/adapter/converter/exam/{ExamFacadeConverter,ScoreFacadeConverter}.java`
- Create: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-adapter/src/main/java/adapter/validators/course/CourseFacadeValidator.java`
- Create: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-adapter/src/main/java/adapter/validators/exam/{ExamFacadeValidator,ScoreFacadeValidator}.java`
- Create: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-adapter/src/main/java/adapter/dto/exam/RecordScoreMessage.java`
- Create: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-adapter/src/main/java/adapter/mq/exam/RecordScoreConsumer.java`
- Create: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-adapter/src/main/java/adapter/handler/GlobalFacadeExceptionHandler.java`
- Create: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-adapter/src/test/java/adapter/facade/impl/{CourseFacadeImplTest,ExamFacadeImplTest,ScoreFacadeImplTest}.java`
- Create: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-adapter/src/test/java/adapter/mq/exam/RecordScoreConsumerTest.java`
- Create: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-adapter/src/test/java/adapter/rpc/EvaluationDubboTripleIntegrationTest.java`
- Modify: `egon-cola-archetypes/egon-cola-archetype-service/src/test/resources/projects/basic/verify.groovy`

- [ ] **Step 1: Write failing facade and consumer unit tests**

Each facade test mocks only its Application manager and verifies validation, conversion, delegation, and response conversion. The consumer test verifies that one incoming command becomes exactly one `RecordScoreCommand` and that an Application exception is propagated so the broker can apply its delivery policy.

```java
@Test
void shouldDelegateRecordScoreCommand() {
    RecordScoreConsumer consumer = new RecordScoreConsumer(scoreManage);
    RecordScoreMessage message = new RecordScoreMessage(
            "message-1", "exam-1", "student-1", 92, Instant.EPOCH);

    consumer.consume(message);

    verify(scoreManage).record(new RecordScoreCommand(
            "exam-1", "student-1", 92));
}
```

- [ ] **Step 2: Write a failing in-process Dubbo Triple transport test**

`EvaluationDubboTripleIntegrationTest` allocates a free TCP port with `ServerSocket(0)`, starts a provider with registry `N/A`, and injects a consumer reference using `@DubboReference(url = "${symbol_dollar}{test.dubbo.url}")`. Invoke one Course method and one Exam method through the proxy and assert DTO values. Do not satisfy this acceptance test by directly calling the implementation class.

```java
@DynamicPropertySource
static void dubboProperties(DynamicPropertyRegistry registry) {
    registry.add("dubbo.protocol.name", () -> "tri");
    registry.add("dubbo.protocol.port", EvaluationDubboTripleIntegrationTest::freePort);
    registry.add("dubbo.registry.address", () -> "N/A");
    registry.add("test.dubbo.url", () -> "tri://127.0.0.1:" + freePort());
}
```

Capture the selected port once in a static field; both suppliers must return the same value. The test must not contact Nacos.

- [ ] **Step 3: Run the integration test and confirm adapter tests fail**

Run the Task 1 integration-test command.

Expected: FAIL because the facade implementations, MQ consumer, and Triple providers do not exist.

- [ ] **Step 4: Implement Dubbo providers as thin adapters**

Each provider is a Dubbo service, validates its Facade request, converts it into an Application command/query, invokes one manager, and converts the Application result into a Facade response.

```java
@DubboService
public class ExamFacadeImpl implements ExamFacade {

    private final ExamManage examManage;
    private final ExamFacadeConverter converter;
    private final ExamFacadeValidator validator;
    private final GlobalFacadeExceptionHandler exceptionHandler;

    @Override
    public SingleResponse<ExamResponse> publishExam(PublishExamRequest request) {
        validator.validate(request);
        try {
            return SingleResponse.of(
                    converter.toResponse(examManage.publish(converter.toCommand(request))));
        } catch (ApplicationException exception) {
            return exceptionHandler.toFailure(exception);
        }
    }
}
```

Do not import Domain or Infrastructure types. `GlobalFacadeExceptionHandler` maps Application validation/not-found/conflict failures to the approved Facade error codes without exposing internal exceptions.

- [ ] **Step 5: Implement the RabbitMQ command consumer**

```java
@Component
@ConditionalOnProperty(
        prefix = "app.integrations.rabbitmq", name = "enabled", havingValue = "true")
public class RecordScoreConsumer {

    @RabbitListener(
            queues = "${symbol_dollar}{app.integrations.rabbitmq.score-command-queue}",
            autoStartup = "${symbol_dollar}{app.integrations.rabbitmq.listener-auto-startup:false}")
    public void consume(RecordScoreMessage message) {
        scoreManage.record(new RecordScoreCommand(
                message.examId(), message.studentId(), message.points()));
    }
}
```

The consumer returns `void`, does not swallow exceptions, and contains no retry, dead-letter, deduplication, or outbox behavior.

- [ ] **Step 6: Run adapter tests and the actual Triple transport test**

Run the Task 1 integration-test command.

Expected: `BUILD SUCCESS`; unit tests pass and `EvaluationDubboTripleIntegrationTest` performs real in-process Triple calls on a collision-safe port without Nacos or RabbitMQ.

- [ ] **Step 7: Commit the Adapter implementation**

```bash
git add -- \
  egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-facade \
  egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-adapter \
  egon-cola-archetypes/egon-cola-archetype-service/src/test/resources/projects/basic/verify.groovy
git diff --cached --check
git commit -m "feat(archetype): add evaluation rpc and mq adapters"
```

### Task 10: Harden Starter Runtime Profiles And Architecture Rules

**Files:**
- Modify: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-starter/src/main/resources/{application.yml,application-local.yml,application-test.yml,application-dev.yml,application-prod.yml,bootstrap.yml}`
- Modify: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-starter/src/main/java/starter/StudentManagementEvaluationApplication.java`
- Create: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-starter/src/test/java/starter/EvaluationExternalFreeContextTest.java`
- Modify: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-starter/src/test/java/starter/architecture/ModuleArchitectureTest.java`
- Delete: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-starter/src/test/java/starter/EvaluationFlowTest.java`
- Modify: `egon-cola-archetypes/egon-cola-archetype-service/src/test/resources/projects/basic/verify.groovy`

- [ ] **Step 1: Write failing external-free context and architecture tests**

`EvaluationExternalFreeContextTest` starts the complete generated ApplicationContext with profile `test` and `webEnvironment = NONE`. It verifies assembly by bean name so Starter tests do not import transitive business-layer types:

```java
assertThat(context.containsBean("courseFacadeImpl")).isTrue();
assertThat(context.containsBean("examFacadeImpl")).isTrue();
assertThat(context.containsBean("scoreFacadeImpl")).isTrue();
assertThat(context.containsBean("rabbitCourseEventPublisher")).isFalse();
assertThat(context.containsBean("rabbitExamEventPublisher")).isFalse();
```

It must not start an HTTP server, Nacos client, Rabbit listener, or broker connection.

Add ArchUnit assertions that initially fail on the legacy paths:

```java
noClasses().that().resideInAPackage("..adapter..")
        .should().dependOnClassesThat()
        .resideInAnyPackage("..domain..", "..infrastructure..", "..starter..");

noClasses().that().resideInAPackage("..application..")
        .should().dependOnClassesThat()
        .resideInAnyPackage("..common..", "..facade..", "..infrastructure..", "..starter..");

noClasses().that().resideInAPackage("..infrastructure..")
        .should().dependOnClassesThat()
        .resideInAnyPackage("..application..", "..common..", "..facade..", "..adapter..", "..starter..");
```

Also assert Domain does not depend on Facade/Application/Infrastructure/Adapter/Starter, and Common/Facade do not depend inward. Allow Domain package `..domain..vos..`; prohibit the singular Adapter package segment `..adapter..vo..`.

- [ ] **Step 2: Run the integration test and confirm runtime rules fail**

Run the Task 1 integration-test command.

Expected: FAIL because the final profile contract and strict boundary rules are not yet established.

- [ ] **Step 3: Configure the final profile matrix**

Base `application.yml` contains only shared defaults and environment substitutions:

```yaml
server:
  port: ${symbol_dollar}{MANAGEMENT_SERVER_PORT:8081}

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    health:
      probes:
        enabled: true

app:
  integrations:
    rabbitmq:
      enabled: ${symbol_dollar}{RABBITMQ_ENABLED:false}
      listener-auto-startup: ${symbol_dollar}{RABBITMQ_LISTENER_AUTO_STARTUP:false}
      exchange: ${symbol_dollar}{RABBITMQ_EXCHANGE:evaluation.events}
      score-command-queue: ${symbol_dollar}{RABBITMQ_SCORE_COMMAND_QUEUE:evaluation.score.command}
      score-command-routing-key: ${symbol_dollar}{RABBITMQ_SCORE_COMMAND_ROUTING_KEY:score.record.command}
      course-scheduled-routing-key: ${symbol_dollar}{RABBITMQ_COURSE_SCHEDULED_ROUTING_KEY:course.scheduled}
      exam-published-routing-key: ${symbol_dollar}{RABBITMQ_EXAM_PUBLISHED_ROUTING_KEY:exam.published}
      score-recorded-routing-key: ${symbol_dollar}{RABBITMQ_SCORE_RECORDED_ROUTING_KEY:score.recorded}
```

`local` and `test` use H2 PostgreSQL mode, disable RabbitMQ/Nacos/listeners, and disable the H2 console. `dev` and `prod` read PostgreSQL, Nacos, Dubbo, and RabbitMQ endpoints from environment variables and opt in to integrations explicitly. No profile enables a business HTTP endpoint.

- [ ] **Step 4: Align bootstrap and starter scanning**

Keep the starter as the only bootstrap module. Its root-package component scan discovers Application's Domain-service configuration and Infrastructure adapters at runtime without Starter source importing Application or Domain types. Enable Dubbo provider scanning only for Adapter facade implementations, and do not expose Domain/Application objects as web controllers.

- [ ] **Step 5: Run the external-free context and architecture tests**

Run the Task 1 integration-test command.

Expected: `BUILD SUCCESS`; the test profile creates the complete service graph without external services and all layer rules pass.

- [ ] **Step 6: Commit runtime hardening**

```bash
git add -- \
  egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-starter \
  egon-cola-archetypes/egon-cola-archetype-service/src/test/resources/projects/basic/verify.groovy
git diff --cached --check
git commit -m "feat(archetype): harden service runtime boundaries"
```

### Task 11: Cut Over From The Legacy Course And ExamResult Sample

**Files:**
- Delete: all generated Java files below `**/examing/**`
- Delete: legacy `domain/client/**` and `infrastructure/client/**`
- Delete: legacy `facade/api/ExamResultFacade.java`, `facade/dto/examing/**`, and obsolete Course Facade DTOs replaced by Task 2/9 contracts
- Delete: legacy `domain/exam/ExamResult.java`, legacy `infrastructure/persistence/exam/ExamResultPo.java`, and their repositories/converters
- Delete: obsolete pass-through converters, duplicate response wrappers, and Java `.gitkeep` files
- Modify: temporary compatibility overloads in Course/Application types introduced before Tasks 3-5
- Modify: `egon-cola-archetypes/egon-cola-archetype-service/src/test/resources/projects/basic/verify.groovy`

- [ ] **Step 1: Add failing stale-path assertions to the verifier**

The verifier recursively scans generated source paths and rejects active references to:

```groovy
['examing', 'ExamResultFacade', 'ExamResultPo', 'domain.client',
 'infrastructure.client', 'CourseClient', 'ExamClient']
```

Allow the literal database table name `exam_result` only inside immutable `V1__init_student_management_evaluation.sql` and the V2 migration-copy statement. Do not exempt Java, YAML, POM, README, or generated workflow files.

- [ ] **Step 2: Run the integration test and confirm legacy assertions fail**

Run the Task 1 integration-test command.

Expected: FAIL and list the obsolete files still generated.

- [ ] **Step 3: Remove the old paths and compatibility overloads**

Delete only after Tasks 2-10 have green replacement tests. Remove temporary overloads, imports, beans, and tests in one cutover. Final active code uses `course`, `exam`, `paper`, and `score`; it never uses the misspelled `examing` package or the old pass-through client abstraction.

- [ ] **Step 4: Verify the final dependency and type vocabulary**

```bash
rg -n 'examing|ExamResultFacade|ExamResultPo|domain\.client|infrastructure\.client|CourseClient|ExamClient' \
  egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources \
  --glob '!**/V1__init_student_management_evaluation.sql' \
  --glob '!**/V2__align_evaluation_course_exam_domain.sql'
```

Expected: no output.

Run the Task 1 integration-test command.

Expected: `BUILD SUCCESS`; no test relies on removed compatibility APIs.

- [ ] **Step 5: Commit the cutover**

```bash
git add -A -- \
  egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources \
  egon-cola-archetypes/egon-cola-archetype-service/src/test/resources/projects/basic/verify.groovy
git diff --cached --check
git commit -m "refactor(archetype): cut over evaluation service sample"
```

### Task 12: Lock Generation Metadata, Generated CI, Documentation, And Acceptance

**Files:**
- Modify: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/META-INF/maven/archetype-metadata.xml`
- Modify: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/README.md`
- Create: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/.github/workflows/ci.yml`
- Modify: `egon-cola-archetypes/egon-cola-archetype-service/src/test/resources/projects/basic/verify.groovy`
- Modify only if the service row needs stronger assertions: `.github/workflows/ci_by_multiply_java_versions.yaml`

- [ ] **Step 1: Write failing final-generation assertions**

Extend `verify.groovy` to assert:

- the exact seven-module reactor and approved dependency direction;
- the full required package/file manifest from this plan;
- no Java `.gitkeep`, `examing`, pass-through client, business Controller, or H2 console configuration;
- one immutable V1 plus exactly one `V2__align_evaluation_course_exam_domain.sql`;
- `spring-amqp` and `flyway-database-postgresql` are present only where approved;
- local/test external integrations default off;
- generated `.github/workflows/ci.yml`, Maven wrapper, Dockerfile, README, and all profile files exist;
- Adapter tests include an actual Dubbo Triple proxy call and MQ consumer tests;
- Domain tests, JPA tests, migration tests, context tests, and ArchUnit tests are generated.

- [ ] **Step 2: Run the integration test and confirm metadata assertions fail**

Run the Task 1 integration-test command.

Expected: FAIL because the workflow, metadata, and final README assertions are not complete.

- [ ] **Step 3: Align archetype metadata with the generated file set**

Add a filtered file set for `.github/workflows/ci.yml`. Keep Java/resources/tests filtered according to the existing archetype convention. Remove metadata entries whose only purpose was to preserve empty Java directories. Do not add a new module or dependency.

- [ ] **Step 4: Add a generated-project CI workflow**

```yaml
name: CI

on:
  push:
  pull_request:

permissions:
  contents: read

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: '21'
          cache: maven
      - run: SPRING_PROFILES_ACTIVE=test bash ./mvnw -B -ntp clean test
      - run: SPRING_PROFILES_ACTIVE=test bash ./mvnw -B -ntp -DskipTests package
      - run: docker build -t student-management-evaluation:ci .
```

Use `bash ./mvnw` so the generated contract matches Linux and Windows GitHub runners. Do not start the built service.

- [ ] **Step 5: Rewrite the generated README as an operations contract**

Document the module ownership, allowed dependency graph, Course/Exam/Paper/Score example flows, Facade and MQ entry points, environment variables, profiles, migration policy, and exact test/package/Docker commands. State explicitly:

- only management HTTP endpoints are exposed;
- local/test require no Nacos, RabbitMQ, or PostgreSQL;
- V1 is immutable and V2 is the alignment migration;
- RabbitMQ is basic transport without retry/DLQ/outbox guarantees;
- Organization dual-domain Facade integration is deferred and not part of this generated service.

- [ ] **Step 6: Keep the repository strong CI service scenario authoritative**

Inspect `.github/workflows/ci_by_multiply_java_versions.yaml`. Its service-archetype scenario must generate arbitrary coordinates, assert generated `.github/workflows/ci.yml`, run generated `clean test`, run generated `package`, and Docker-build the generated service. Change only that service scenario if any assertion is absent; preserve all other archetype jobs.

- [ ] **Step 7: Run the authoritative archetype acceptance command**

```bash
bash ./mvnw -B -ntp \
  -pl egon-cola-archetypes/egon-cola-archetype-service -am \
  clean integration-test
```

Expected: `BUILD SUCCESS`; the Invoker generates the default project and `verify.groovy` accepts its exact structure and tests.

- [ ] **Step 8: Test and package the generated project directly**

```bash
generated_dir="egon-cola-archetypes/egon-cola-archetype-service/target/test-classes/projects/basic/project/student-management-evaluation"
SPRING_PROFILES_ACTIVE=test bash "$generated_dir/mvnw" -B -ntp -f "$generated_dir/pom.xml" clean test
SPRING_PROFILES_ACTIVE=test bash "$generated_dir/mvnw" -B -ntp -f "$generated_dir/pom.xml" -DskipTests package
```

Expected: both commands end in `BUILD SUCCESS`; tests use H2 and do not require Nacos, RabbitMQ, or PostgreSQL.

- [ ] **Step 9: Docker-build without starting a container**

```bash
if command -v docker >/dev/null 2>&1; then
  docker build -t student-management-evaluation:plan-verify "$generated_dir"
else
  echo "Docker unavailable; record this acceptance item as unverified"
fi
```

Expected when Docker is available: image build succeeds. Do not run the image.

- [ ] **Step 10: Run final scope and formatting checks**

```bash
git diff --check
git status --short
rg -n 'examing|ExamResultFacade|ExamResultPo|domain\.client|infrastructure\.client|CourseClient|ExamClient' \
  egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources \
  --glob '!**/V1__init_student_management_evaluation.sql' \
  --glob '!**/V2__align_evaluation_course_exam_domain.sql'
```

Expected: no whitespace errors; only service-archetype, its service CI scenario, and this plan/spec scope are changed; stale-source scan has no output. Do not include or modify the separately evolving Web archetype work.

- [ ] **Step 11: Commit the generation contract**

```bash
git add -- \
  egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/META-INF/maven/archetype-metadata.xml \
  egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/README.md \
  egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/.github/workflows/ci.yml \
  egon-cola-archetypes/egon-cola-archetype-service/src/test/resources/projects/basic/verify.groovy \
  .github/workflows/ci_by_multiply_java_versions.yaml
git diff --cached --check
git commit -m "test(archetype): harden evaluation generation contract"
```

If `.github/workflows/ci_by_multiply_java_versions.yaml` did not require modification, omit it from `git add`.

## Spec Coverage Map

| Approved spec area | Implemented and verified by |
|---|---|
| Scope, seven-module topology, dependency direction | Tasks 1, 10, 12 |
| Common stable foundations | Task 2 |
| Facade DTO/RPC contracts | Tasks 2, 9 |
| Course and Schedule Domain | Task 3 |
| Exam, Paper, and Score Domain | Task 4 |
| Application commands, queries, managers, and assembly | Task 5 |
| Immutable V1 and one V2 migration | Task 6 |
| Course/Schedule persistence | Task 6 |
| Exam/Paper/Score persistence and stable pagination | Task 7 |
| Domain publisher ports and basic RabbitMQ adapters | Tasks 3, 4, 8 |
| RabbitMQ score command consumer | Task 9 |
| Actual Dubbo Triple provider/consumer call | Task 9 |
| External-free local/test profiles and management-only HTTP | Task 10 |
| Strict ArchUnit dependency rules | Tasks 5, 9, 10, 11 |
| Legacy `examing`/client cutover | Task 11 |
| Archetype metadata, verifier, README, generated CI, strong CI | Task 12 |
| Deferred Organization dual-domain Facade integration | Explicitly excluded from every implementation task |

## Completion Checklist

- [ ] All twelve task commits exist in order and contain only their declared path scope.
- [ ] The generated reactor has exactly Common, Facade, Domain, Application, Infrastructure, Adapter, and Starter modules.
- [ ] Domain owns methods and ports; Infrastructure owns JPA and RabbitMQ implementations.
- [ ] Adapter owns Dubbo providers and the MQ consumer; it does not import Domain or Infrastructure.
- [ ] Course scheduling, exam publication, paper constraints, and score uniqueness/validation have Domain tests.
- [ ] V1 checksum is unchanged and exactly one V2 migration exists.
- [ ] H2 PostgreSQL-mode migration tests prove V1 to V2 upgrade and invalid legacy score failure.
- [ ] The actual Dubbo Triple transport test uses a collision-safe port and no registry.
- [ ] RabbitMQ tests need no broker; local/test start with RabbitMQ disabled.
- [ ] No business HTTP controller or enabled H2 console is generated.
- [ ] No active `examing`, `ExamResultFacade`, pass-through client, or Java `.gitkeep` remains.
- [ ] Archetype Invoker `clean integration-test` succeeds.
- [ ] The generated project passes `clean test` and `package` with the test profile.
- [ ] Docker build succeeds when Docker is available; no container or application is started.
- [ ] The separate Organization Facade integration spec remains untouched and deferred.
- [ ] The concurrent Web archetype changes remain untouched and are not staged with this work.
