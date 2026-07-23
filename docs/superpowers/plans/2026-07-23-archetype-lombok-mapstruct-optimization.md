# Archetype Lombok and Mapping Optimization Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make the light, service, and web archetypes use Lombok and compile-time mappers for structural boilerplate while preserving explicit domain semantics.

**Architecture:** Apply role-based source transformations inside the existing archetype templates. Lombok owns dependency and JPA constructors, MapStruct owns flat record/DTO mappings, and concrete MapStruct Plus `BaseMapper` types own flat persistence mapping; wrappers retain value-object, enum, timestamp, and restoration logic.

**Tech Stack:** Java 21, Spring Boot 3.5.16, Lombok 1.18.38, MapStruct Plus 1.5.1, MapStruct, Maven Compiler Plugin, Maven Archetype Plugin, Groovy verifier scripts, Maven Wrapper 3.9.14.

## Global Constraints

- Do not change generated public APIs, endpoints, facade contracts, database schemas, or package boundaries.
- Do not inject `io.github.linpeilie.Converter`; use concrete mapper types.
- Do not replace exception, validation, normalization, restoration, defensive-copy, or framework bootstrap constructors.
- Do not add `@Data` to persistence or domain objects.
- Do not modify an existing Flyway migration.
- Do not edit generated `target/` files.
- Do not start generated applications manually.
- Preserve the user's uncommitted root `README.md` in the main checkout.

---

### Task 1: Annotation Processor and Lombok Contract

**Files:**

- Modify: `egon-cola-archetypes/egon-cola-archetype-light/src/test/resources/projects/basic/verify.groovy`
- Modify: `egon-cola-archetypes/egon-cola-archetype-service/src/test/resources/projects/basic/verify.groovy`
- Modify: `egon-cola-archetypes/egon-cola-archetype-web/src/test/resources/projects/basic/verify.groovy`
- Modify: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/pom.xml`
- Modify: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/pom.xml`
- Modify: `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/pom.xml`
- Modify: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/lombok.config`
- Modify: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/lombok.config`
- Modify: `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/lombok.config`

**Interfaces:**

- Consumes: existing Maven compiler annotation processor configuration.
- Produces: `${lombok.mapstruct.binding.version}` and consistent Lombok configuration in every generated project.

- [ ] **Step 1: Add failing generated-project contract assertions**

Add the following assertions to each verifier, adapting only the existing local
POM variable name:

```groovy
assert pom.contains("<lombok.mapstruct.binding.version>0.2.0</lombok.mapstruct.binding.version>")
assert pom.contains("<artifactId>lombok-mapstruct-binding</artifactId>")

def lombokConfig = assertFile("lombok.config").text
[
    "config.stopBubbling = true",
    "lombok.copyableAnnotations += org.springframework.beans.factory.annotation.Qualifier",
    "lombok.copyableAnnotations += org.springframework.beans.factory.annotation.Value",
    "lombok.addLombokGeneratedAnnotation = true",
    "lombok.anyConstructor.addConstructorProperties = true",
    "lombok.data.flagUsage = warning",
    "lombok.val.flagUsage = warning"
].each { expected -> assert lombokConfig.contains(expected) }
```

- [ ] **Step 2: Run each focused archetype integration test and observe RED**

Run:

```bash
./mvnw -B -ntp -f egon-cola-archetypes/pom.xml \
  -pl egon-cola-archetype-light -am clean integration-test
```

Expected: failure in `verify.groovy` because the binding property or Lombok
configuration is absent.

Repeat with `-pl egon-cola-archetype-service` and
`-pl egon-cola-archetype-web`; each must fail for the same missing contract.

- [ ] **Step 3: Add binding properties and processor paths**

Add to each generated root POM:

```xml
<lombok.mapstruct.binding.version>0.2.0</lombok.mapstruct.binding.version>
```

Add after the Lombok annotation processor and before the MapStruct Plus
processor:

```xml
<path>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok-mapstruct-binding</artifactId>
    <version>${lombok.mapstruct.binding.version}</version>
</path>
```

- [ ] **Step 4: Harden all generated Lombok configurations**

Make each `lombok.config` exactly:

```properties
config.stopBubbling = true
lombok.copyableAnnotations += org.springframework.beans.factory.annotation.Qualifier
lombok.copyableAnnotations += org.springframework.beans.factory.annotation.Value
lombok.addLombokGeneratedAnnotation = true
lombok.anyConstructor.addConstructorProperties = true
lombok.data.flagUsage = warning
lombok.val.flagUsage = warning
```

- [ ] **Step 5: Run focused GREEN verification**

Run the three commands from Step 2.

Expected: all three commands end with `BUILD SUCCESS`.

- [ ] **Step 6: Commit**

```bash
git add egon-cola-archetypes
git commit -m "build(archetype): align lombok mapstruct processing"
```

### Task 2: Light Archetype Source Optimization

**Files:**

- Modify the eight persistence classes under:
  - `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java/infrastructure/teaching/repo/po/`
  - `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java/infrastructure/user/repo/po/`
- Modify:
  - `adapter/teaching/convertor/TeachingAdapterConvertor.java`
  - `adapter/user/convertor/UserAdapterConvertor.java`
  - their controller and GraphQL callers under `adapter/teaching` and `adapter/user`
  - `infrastructure/teaching/repo/converter/CoursePOConverter.java`
- Create:
  - `infrastructure/teaching/repo/converter/CoursePOMapper.java`
- Modify the twelve structural dependency-injection classes identified by the
  verifier under light adapter and infrastructure packages.
- Modify: `egon-cola-archetypes/egon-cola-archetype-light/src/test/resources/projects/basic/verify.groovy`

**Interfaces:**

- Consumes: `Course`, `CoursePO`, result records, and adapter VO records.
- Produces: Spring mappers `TeachingAdapterConvertor`,
  `UserAdapterConvertor`, and `coursePOMapperImpl`.

- [ ] **Step 1: Add failing light source-contract assertions**

Add:

```groovy
def teachingMapper = assertFile(
    "src/main/java/it/pkg/adapter/teaching/convertor/TeachingAdapterConvertor.java").text
assert teachingMapper.contains("@Mapper(")
assert teachingMapper.contains("ReportingPolicy.ERROR")

def userMapper = assertFile(
    "src/main/java/it/pkg/adapter/user/convertor/UserAdapterConvertor.java").text
assert userMapper.contains("@Mapper(")
assert userMapper.contains("ReportingPolicy.ERROR")

def coursePoMapper = assertFile(
    "src/main/java/it/pkg/infrastructure/teaching/repo/converter/CoursePOMapper.java").text
assert coursePoMapper.contains("extends BaseMapper<Course, CoursePO>")

def coursePo = assertFile(
    "src/main/java/it/pkg/infrastructure/teaching/repo/po/CoursePO.java").text
assert coursePo.contains("@NoArgsConstructor(access = AccessLevel.PROTECTED)")
assert coursePo.contains("@AllArgsConstructor")
assert !coursePo.contains("protected CoursePO()")

def rabbitConfig = assertFile(
    "src/main/java/it/pkg/infrastructure/config/RabbitMqConfig.java").text
assert rabbitConfig.contains("@RequiredArgsConstructor")
assert !rabbitConfig.contains("public RabbitMqConfig(")
```

- [ ] **Step 2: Run light integration test and observe RED**

Run:

```bash
./mvnw -B -ntp -f egon-cola-archetypes/pom.xml \
  -pl egon-cola-archetype-light -am clean integration-test
```

Expected: failure because the mapper file and Lombok annotations are absent.

- [ ] **Step 3: Replace light JPA constructor boilerplate**

For each persistence class, add:

```java
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
```

Remove only the matching protected no-argument and field-for-field full
constructors. Keep accessors and JPA annotations unchanged. Apply the same
annotations to embedded key classes whose two constructors are purely
structural.

- [ ] **Step 4: Convert light adapter helpers to strict MapStruct mappers**

`TeachingAdapterConvertor` becomes:

```java
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface TeachingAdapterConvertor {
    CourseDetailVO toCourse(CourseResult result);
    SchoolClassDetailVO toSchoolClass(SchoolClassResult result);
}
```

`UserAdapterConvertor` becomes:

```java
@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.ERROR,
        imports = List.class)
public interface UserAdapterConvertor {
    UserDetailVO toUserDetail(UserResult result);
    List<PermissionTreeVO> toPermissionTree(List<PermissionDetailResult> results);

    @Mapping(target = "children", expression = "java(List.of())")
    PermissionTreeVO toPermissionTreeItem(PermissionDetailResult result);
}
```

Inject these mapper interfaces into existing controllers and resolvers with
final fields and `@RequiredArgsConstructor`; replace static calls with instance
calls.

- [ ] **Step 5: Add a concrete MapStruct Plus persistence mapper**

Create:

```java
@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.ERROR,
        imports = Instant.class)
public interface CoursePOMapper extends BaseMapper<Course, CoursePO> {
    @Override
    @Mapping(target = "courseCode", source = "code.value")
    @Mapping(target = "status", source = "status")
    @Mapping(target = "createdAt", expression = "java(Instant.now())")
    CoursePO convert(Course source);
}
```

Inject `CoursePOMapper` into `CoursePOConverter` with
`@Qualifier("coursePOMapperImpl")` and delegate `toPO` to `convert`. Keep the
explicit PO-to-domain value-object restoration.

- [ ] **Step 6: Convert structural injection constructors**

Add `@RequiredArgsConstructor` and remove assignment-only constructors from
the twelve light classes enumerated by:

```bash
for f in $(rg -l '@(Component|Configuration|Aspect|DubboService)' \
  egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources \
  -g '*.java'); do
  rg -q '@RequiredArgsConstructor' "$f" || echo "$f"
done
```

Move `@Qualifier` and `@Value` annotations to final fields. Preserve bootstrap,
exception, domain, and test-fixture constructors.

- [ ] **Step 7: Run light GREEN verification**

Run the command from Step 2.

Expected: `BUILD SUCCESS`, including generated mapper compilation and all
generated light tests.

- [ ] **Step 8: Commit**

```bash
git add egon-cola-archetypes/egon-cola-archetype-light
git commit -m "refactor(archetype): optimize light lombok mappings"
```

### Task 3: Service Archetype Source Optimization

**Files:**

- Modify the five persistence classes under:
  - `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/main/java/infrastructure/course/repo/po/`
  - `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/main/java/infrastructure/exam/repo/po/`
- Modify:
  - `adapter/course/converter/CourseFacadeConverter.java`
  - `adapter/exam/converter/ExamFacadeConverter.java`
  - `adapter/exam/converter/ScoreFacadeConverter.java`
  - `adapter/exam/mq/RecordScoreConsumer.java`
  - `infrastructure/course/mq/RabbitCourseEventPublisher.java`
  - `infrastructure/exam/mq/RabbitExamEventPublisher.java`
- Modify: `egon-cola-archetypes/egon-cola-archetype-service/src/test/resources/projects/basic/verify.groovy`

**Interfaces:**

- Consumes: facade requests, application commands/results, and persistence
  classes.
- Produces: strict Spring mapper implementations for the three facade
  converters and Lombok-generated structural constructors.

- [ ] **Step 1: Add failing service assertions**

Assert each facade converter contains `@Mapper(` and
`ReportingPolicy.ERROR`, representative PO classes contain protected
`@NoArgsConstructor` plus `@AllArgsConstructor`, and the three assignment-only
Spring constructors contain `@RequiredArgsConstructor` instead.

- [ ] **Step 2: Run service integration test and observe RED**

```bash
./mvnw -B -ntp -f egon-cola-archetypes/pom.xml \
  -pl egon-cola-archetype-service -am clean integration-test
```

Expected: verifier failure for missing mapper and Lombok contracts.

- [ ] **Step 3: Replace service JPA constructor boilerplate**

Apply:

```java
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
```

to `CoursePo`, `CourseSchedulePo`, `ExamPo`, `ExamPaperPo`, and `ScorePo`.
Remove only their structural constructors.

- [ ] **Step 4: Convert facade converters to strict MapStruct interfaces**

Use:

```java
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface CourseFacadeConverter {
    CreateCourseCommand toCommand(CreateCourseRequest request);
    ScheduleCourseCommand toCommand(ScheduleCourseRequest request);
    CourseResponse toResponse(CourseResult result);
    CourseScheduleResponse toResponse(CourseScheduleResult result);
}
```

Use the same annotation on `ExamFacadeConverter` and
`ScoreFacadeConverter`, retaining their existing method signatures.

- [ ] **Step 5: Convert service structural constructors**

Use `@RequiredArgsConstructor` for `RecordScoreConsumer`,
`RabbitCourseEventPublisher`, and `RabbitExamEventPublisher`. Move each Spring
`@Value` annotation to its final field. Preserve the two-constructor Dubbo
client because its no-argument constructor supports field injection and its
package constructor supports tests.

- [ ] **Step 6: Run service GREEN verification**

Run the command from Step 2.

Expected: `BUILD SUCCESS`.

- [ ] **Step 7: Commit**

```bash
git add egon-cola-archetypes/egon-cola-archetype-service
git commit -m "refactor(archetype): optimize service lombok mappings"
```

### Task 4: Web Archetype Source Optimization

**Files:**

- Modify the eight persistence classes under:
  - `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/main/java/infrastructure/teaching/repo/po/`
  - `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/main/java/infrastructure/user/repo/po/`
- Modify the five converter files under:
  - `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-adapter/src/main/java/adapter/teaching/converter/`
  - `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-adapter/src/main/java/adapter/user/converter/`
- Create:
  - `infrastructure/teaching/repo/converter/GradePOMapper.java`
- Modify:
  - `infrastructure/teaching/repo/converter/GradePOConverter.java`
  - assignment-only Spring classes in web application, adapter, and
    infrastructure modules.
- Modify: `egon-cola-archetypes/egon-cola-archetype-web/src/test/resources/projects/basic/verify.groovy`

**Interfaces:**

- Consumes: adapter request records, application command/result records,
  `Grade`, and `GradePO`.
- Produces: five strict adapter mapper interfaces, `gradePOMapperImpl`, and
  Lombok-generated dependency/JPA constructors.

- [ ] **Step 1: Add failing web assertions**

Assert the five adapter converter files contain `@Mapper(` and
`ReportingPolicy.ERROR`; `GradePOMapper` extends
`BaseMapper<Grade, GradePO>`; `GradePO` uses Lombok constructors; and
representative controller, application service, repository, cache, and event
publisher classes use `@RequiredArgsConstructor`.

- [ ] **Step 2: Run web integration test and observe RED**

```bash
./mvnw -B -ntp -f egon-cola-archetypes/pom.xml \
  -pl egon-cola-archetype-web -am clean integration-test
```

Expected: verifier failure for missing mapper and Lombok contracts.

- [ ] **Step 3: Replace web JPA constructor boilerplate**

Apply protected no-argument and all-argument Lombok annotations to `GradePO`,
`SchoolClassPO`, `SchoolClassUserPO`, `PermissionPO`, `RolePO`,
`RolePermissionPO`, `UserPO`, and `UserRolePO`. Remove only structural
constructors.

- [ ] **Step 4: Convert adapter converters to strict MapStruct interfaces**

Use:

```java
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
```

on all five converter interfaces. Add explicit `@Mapping` annotations for
multi-source command methods, for example:

```java
@Mapping(target = "requestId", source = "requestId")
@Mapping(target = "name", source = "request.name")
@Mapping(target = "email", source = "request.email")
CreateUserCommand toCommand(String requestId, CreateUserRequest request);
```

Keep every current converter method signature so callers remain unchanged.

- [ ] **Step 5: Add a concrete MapStruct Plus grade mapper**

Create:

```java
@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.ERROR,
        imports = LocalDateTime.class)
public interface GradePOMapper extends BaseMapper<Grade, GradePO> {
    @Override
    @Mapping(target = "code", source = "code.value")
    @Mapping(target = "status", source = "status")
    @Mapping(target = "createdAt", expression = "java(LocalDateTime.now())")
    GradePO convert(Grade source);
}
```

Inject it into `GradePOConverter` with
`@Qualifier("gradePOMapperImpl")`; delegate domain-to-PO conversion and retain
the explicit legacy-aware PO-to-domain restoration.

- [ ] **Step 6: Convert web structural injection constructors**

Add `@RequiredArgsConstructor` and remove assignment-only constructors from
controllers, facade implementations, resolvers, consumers, application
services, repositories, caches, idempotency adapter, event producer, and event
publisher. Use `@Slf4j` in `RabbitOrganizationEventPublisher`.

Preserve `DubboEvaluationQueryClient` because its no-argument constructor is
needed for `@DubboReference` field injection and its package constructor is a
test seam.

- [ ] **Step 7: Run web GREEN verification**

Run the command from Step 2.

Expected: `BUILD SUCCESS`.

- [ ] **Step 8: Commit**

```bash
git add egon-cola-archetypes/egon-cola-archetype-web
git commit -m "refactor(archetype): optimize web lombok mappings"
```

### Task 5: Full Contract Verification and Documentation Closure

**Files:**

- Modify if assertions need path corrections:
  - the three `verify.groovy` files already changed in Tasks 1-4.
- Review:
  - `docs/superpowers/specs/2026-07-23-archetype-lombok-mapstruct-optimization-design.md`
  - `docs/superpowers/plans/2026-07-23-archetype-lombok-mapstruct-optimization.md`

**Interfaces:**

- Consumes: all changes from Tasks 1-4.
- Produces: a verified archetype reactor and a clean feature branch.

- [ ] **Step 1: Audit remaining explicit constructors**

Run:

```bash
rg -n "^[[:space:]]*(public|protected|private)[[:space:]]+[A-Z][A-Za-z0-9_]*\\(" \
  egon-cola-archetypes/egon-cola-archetype-{light,service,web}/src/main/resources/archetype-resources \
  -g '*.java'
```

Classify every remaining constructor as exception, domain invariant,
normalization, restore/copy, framework bootstrap, test seam, or intentional
overload. Remove any remaining assignment-only dependency/JPA constructor.

- [ ] **Step 2: Audit mapper boundaries**

Run:

```bash
rg -n "io.github.linpeilie.Converter|private final Converter converter" \
  egon-cola-archetypes/egon-cola-archetype-{light,service,web}/src/main/resources/archetype-resources \
  -g '*.java'
```

Expected: no matches.

Run:

```bash
rg -n "extends BaseMapper|@Mapper\\(" \
  egon-cola-archetypes/egon-cola-archetype-{light,service,web}/src/main/resources/archetype-resources \
  -g '*.java'
```

Expected: concrete mapper coverage in all three archetypes.

- [ ] **Step 3: Run full reactor verification**

Run:

```bash
./mvnw -B -ntp -f egon-cola-archetypes/pom.xml clean integration-test
```

Expected: all six archetype reactor modules and all three generated projects
end with `BUILD SUCCESS`.

- [ ] **Step 4: Run repository hygiene checks**

Run:

```bash
git diff --check
git status --short
git log --oneline --decorate -6
```

Expected: no whitespace errors, only intended files are changed before the
final commit, and task commits are visible.

- [ ] **Step 5: Commit verifier corrections only if required**

If Step 3 exposed a verifier path mismatch and the templates already satisfy
the design, correct only that assertion and commit:

```bash
git add egon-cola-archetypes/*/src/test/resources/projects/basic/verify.groovy
git commit -m "test(archetype): finalize lombok mapper contracts"
```

If no correction is required, do not create an empty commit.
