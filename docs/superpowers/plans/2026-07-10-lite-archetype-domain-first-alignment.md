# Lite Archetype Domain-First Alignment Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Rebuild `egon-cola-archetype-light` as a single-module, domain-first `user` and `teaching` monolith whose generated project implements the approved HTTP, GraphQL, Dubbo, RabbitMQ, Redis, external-client, JPA, validation, and architecture contracts.

**Architecture:** Keep one Spring Boot application and organize each layer by domain first: `adapter.user.controller`, `application.user.manage`, `domain.user`, and `infrastructure.user.repo`, with matching `teaching` trees. Application depends only on Domain interfaces; Infrastructure implements Domain services and outbound ports without importing Application. Build the replacement alongside the old Student sample, then remove the old paths only after the new vertical flows compile and pass tests.

**Tech Stack:** Java 21, Spring Boot 3.5.16, Spring MVC, Spring GraphQL, Dubbo 3.3.6, Spring AMQP/RabbitMQ, Spring Data Redis, Spring Data JPA, H2, PostgreSQL, Flyway, MapStruct Plus, Springdoc OpenAPI 2.8.17, JUnit 5, Mockito, Spring Boot Test, ArchUnit, Maven Archetype Plugin.

---

## Execution Constraints

- Execute in an isolated worktree created with `superpowers:using-git-worktrees`.
- Restrict implementation changes to `egon-cola-archetypes/egon-cola-archetype-light`; the committed spec and this plan are already outside that implementation boundary.
- Do not modify `src/main/resources/archetype-resources/src/main/resources/db/migration/V1__init_student_management.sql`.
- Add exactly one migration: `V2__align_large_monolith_domain.sql`.
- Do not start the generated Spring Boot application.
- Keep `local` and `test` independent of Redis, RabbitMQ, Nacos, PostgreSQL, and external HTTP services.
- Commit each task separately with path-scoped staging.

## Generated File Map

The final generated source tree must use these domain-first roots:

```text
src/main/java/${package}
├── start
├── adapter
│   ├── user/{controller,mq,rpc,graphql,facade/impl,dto,vo,convertor,validators}
│   ├── teaching/{controller,mq,rpc,graphql,facade/impl,dto,vo,convertor,validators}
│   ├── handler
│   └── filter
├── facade
│   ├── user/{dto,enums,exceptions,utils}
│   └── teaching/{dto,enums,exceptions,utils}
├── application
│   ├── user/{manage,manage/impl,command,query,result,convertor,validators,assemblers}
│   └── teaching/{manage,manage/impl,command,query,result,convertor,validators,assemblers}
├── domain
│   ├── user/{entities,aggregates,vos,service,repos,validators,enums,exceptions}
│   └── teaching/{entities,aggregates,vos,service,repos,validators,enums,exceptions}
├── infrastructure
│   ├── user/{repo/impl,repo/po,repo/jpa,repo/converter,service/impl,validators,client/impl,mq,cache}
│   ├── teaching/{repo/impl,repo/po,repo/jpa,repo/converter,service/impl,validators,client/impl,mq,cache}
│   ├── aop
│   └── config
└── common/{constants,utils,enums,exceptions}
```

Every directory in this map receives a `package-info.java` when it contains more than one responsibility-bearing type. Root-level shared packages remain limited to `adapter.handler`, `adapter.filter`, `infrastructure.aop`, and `infrastructure.config`.

## Canonical Boundary Signatures

Use these signatures consistently across tasks:

```java
// UserDomainService
User createUser(String externalId, String name, String email);

// RoleDomainService
UserAggregate assignRole(UserAggregate user, Role role);

// PermissionDomainService
RolePermissionAggregate grantPermission(RolePermissionAggregate role, Permission permission);

// UserQueryService, UserCacheService, UserEventPublisher
Optional<ExternalUser> findExternalUser(String externalId);
Optional<UserSnapshot> getUser(String userId);
void putUser(UserSnapshot user);
void evictUser(String userId);
void publish(UserEvent event);

// Domain user repositories
User save(User user);
Optional<User> findById(UserId userId);
void saveRoles(UserAggregate aggregate);
Optional<Role> findByCode(RoleCode roleCode);
Role save(Role role);
Optional<Permission> findByCode(PermissionCode permissionCode);
Permission save(Permission permission);
void savePermissions(RolePermissionAggregate aggregate);

// SchoolClassDomainService
SchoolClass createSchoolClass(String name, Semester semester);
SchoolClassAggregate schedule(SchoolClassAggregate schoolClass, Course course, CourseSchedule schedule);

// CourseDomainService
Course createCourse(CourseCode code, String name);

// TeachingQueryService, CourseCacheService, TeachingEventPublisher
Optional<ExternalCourse> findExternalCourse(CourseCode code);
Optional<CourseSnapshot> getCourse(String courseId);
void putCourse(CourseSnapshot course);
void evictCourse(String courseId);
void publish(TeachingEvent event);

// Domain teaching repositories
SchoolClass save(SchoolClass schoolClass);
Optional<SchoolClassAggregate> findAggregateById(SchoolClassId schoolClassId);
void saveAggregate(SchoolClassAggregate aggregate);
Course save(Course course);
Optional<Course> findById(String courseId);
Optional<Course> findByCode(CourseCode courseCode);

// Application user manages
UserResult create(CreateUserCommand command);
UserResult assignRole(AssignRoleCommand command);
PermissionResult grantPermission(GrantPermissionCommand command);
UserResult get(GetUserQuery query);

// Application teaching manages
SchoolClassResult create(CreateSchoolClassCommand command);
CourseResult create(CreateCourseCommand command);
SchoolClassResult schedule(ScheduleCourseCommand command);
CourseResult get(GetCourseQuery query);
```

Use `String` for database identifiers, Java records for value objects and boundary models, UTC `Instant` for persisted timestamps, and `LocalDateTime` for classroom schedule intervals.

### Task 1: Extend The Generated Build And Archetype Resource Contract

**Files:**
- Modify: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/pom.xml`
- Modify: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/META-INF/maven/archetype-metadata.xml`
- Modify: `egon-cola-archetypes/egon-cola-archetype-light/src/test/resources/projects/basic/verify.groovy`

- [ ] **Step 1: Add failing dependency assertions to the archetype verifier**

Add assertions for the exact new artifacts and JPA-only exclusion:

```groovy
assert pom.contains("<springdoc.version>2.8.17</springdoc.version>")
[
    "spring-boot-starter-graphql",
    "spring-boot-starter-amqp",
    "spring-boot-starter-data-redis",
    "spring-boot-starter-aop",
    "springdoc-openapi-starter-webmvc-ui",
    "spring-boot-starter-test"
].each { artifactId ->
    assert pom.contains("<artifactId>${artifactId}</artifactId>")
}
assert !pom.contains("mybatis-plus")
assert !pom.contains("mybatis-spring")
```

- [ ] **Step 2: Run the archetype integration test and verify the new assertion fails**

Run:

```bash
bash ./mvnw -B -ntp -pl egon-cola-archetypes/egon-cola-archetype-light -am clean integration-test
```

Expected: FAIL in `verify.groovy` because the GraphQL, AMQP, Redis, AOP, and Springdoc artifacts are absent.

- [ ] **Step 3: Add the build dependencies and GraphQL resource inclusion**

Add this property:

```xml
<springdoc.version>2.8.17</springdoc.version>
```

Add these generated-project dependencies while retaining `spring-boot-starter-test` as the single general test starter:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-graphql</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-amqp</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-aop</artifactId>
</dependency>
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>${springdoc.version}</version>
</dependency>
```

Extend the resources file set with:

```xml
<include>**/*.graphqls</include>
```

- [ ] **Step 4: Run the archetype integration test**

Run the command from Step 2.

Expected: BUILD SUCCESS; the existing generated-project tests still pass without external services.

- [ ] **Step 5: Commit the build contract**

```bash
git add -- egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/pom.xml egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/META-INF/maven/archetype-metadata.xml egon-cola-archetypes/egon-cola-archetype-light/src/test/resources/projects/basic/verify.groovy
git commit -m "build(archetype): add lite integration dependencies"
```

### Task 2: Add Common Foundations And User Domain Contracts

**Files:**
- Create: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java/common/exceptions/BaseBusinessException.java`
- Create: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java/common/constants/TraceConstants.java`
- Create: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java/common/utils/IdUtils.java`
- Create: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java/common/enums/DeletedStatus.java`
- Create: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java/domain/user/entities/{User,Role,Permission}.java`
- Create: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java/domain/user/aggregates/{UserAggregate,RolePermissionAggregate}.java`
- Create: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java/domain/user/vos/{UserId,RoleCode,PermissionCode,ExternalUser,UserSnapshot,UserEvent}.java`
- Create: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java/domain/user/enums/{UserStatus,RoleStatus,PermissionStatus}.java`
- Create: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java/domain/user/exceptions/UserDomainException.java`
- Create: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java/domain/user/validators/UserDomainValidator.java`
- Create: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java/domain/user/service/{UserDomainService,RoleDomainService,PermissionDomainService,UserQueryService,UserCacheService,UserEventPublisher}.java`
- Create: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java/domain/user/repos/{UserRepository,RoleRepository,PermissionRepository}.java`
- Create: corresponding `package-info.java` files
- Create: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/test/java/domain/user/aggregates/{UserAggregateTest,RolePermissionAggregateTest}.java`
- Modify: `egon-cola-archetypes/egon-cola-archetype-light/src/test/resources/projects/basic/verify.groovy`

- [ ] **Step 1: Write failing aggregate tests**

```java
package ${package}.domain.user.aggregates;

import ${package}.domain.user.entities.Permission;
import ${package}.domain.user.entities.Role;
import ${package}.domain.user.entities.User;
import ${package}.domain.user.enums.PermissionStatus;
import ${package}.domain.user.enums.RoleStatus;
import ${package}.domain.user.enums.UserStatus;
import ${package}.domain.user.exceptions.UserDomainException;
import ${package}.domain.user.vos.PermissionCode;
import ${package}.domain.user.vos.RoleCode;
import ${package}.domain.user.vos.UserId;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UserAggregateTest {
    @Test
    void assigns_active_role_to_active_user() {
        UserAggregate aggregate = new UserAggregate(
                new User(new UserId("u-1"), "Mario", "mario@example.com", UserStatus.ACTIVE));
        aggregate.assign(new Role(new RoleCode("teacher"), "Teacher", RoleStatus.ACTIVE));
        assertEquals(1, aggregate.roles().size());
    }

    @Test
    void rejects_role_assignment_for_disabled_user() {
        UserAggregate aggregate = new UserAggregate(
                new User(new UserId("u-1"), "Mario", "mario@example.com", UserStatus.DISABLED));
        Role role = new Role(new RoleCode("teacher"), "Teacher", RoleStatus.ACTIVE);
        assertThrows(UserDomainException.class, () -> aggregate.assign(role));
    }
}
```

`RolePermissionAggregateTest` uses an active role and permission to assert duplicate grants collapse to one code, then separately asserts inactive permission and archived role each throw `UserDomainException`.

- [ ] **Step 2: Add verifier assertions and run the failing integration test**

Assert `domain/user/aggregates/UserAggregate.java`, all six Domain service interfaces, all three repositories, and both tests exist. Run the Task 1 integration command.

Expected: FAIL because the new domain files do not exist.

- [ ] **Step 3: Implement the Common base and user domain**

Use these complete aggregate behaviors:

```java
public final class UserAggregate {
    private final User user;
    private final Set<RoleCode> roles = new LinkedHashSet<>();

    public UserAggregate(User user) { this.user = Objects.requireNonNull(user); }
    public User user() { return user; }
    public Set<RoleCode> roles() { return Set.copyOf(roles); }

    public void assign(Role role) {
        UserDomainValidator.requireActive(user);
        UserDomainValidator.requireActive(role);
        roles.add(role.code());
    }
}

public final class RolePermissionAggregate {
    private final Role role;
    private final Set<PermissionCode> permissions = new LinkedHashSet<>();

    public RolePermissionAggregate(Role role) { this.role = Objects.requireNonNull(role); }
    public Role role() { return role; }
    public Set<PermissionCode> permissions() { return Set.copyOf(permissions); }

    public void grant(Permission permission) {
        UserDomainValidator.requireAssignable(role, permission);
        permissions.add(permission.code());
    }
}
```

Define the repository and port contracts with the canonical signatures. Domain exception types extend `BaseBusinessException`; Domain code imports no Spring or persistence types.

- [ ] **Step 4: Run the focused generated-project tests**

Run the integration command, then:

```bash
bash egon-cola-archetypes/egon-cola-archetype-light/target/test-classes/projects/basic/project/basic/mvnw -B -ntp -f egon-cola-archetypes/egon-cola-archetype-light/target/test-classes/projects/basic/project/basic/pom.xml -Dtest=UserAggregateTest,RolePermissionAggregateTest test
```

Expected: BUILD SUCCESS; four aggregate behaviors pass.

- [ ] **Step 5: Commit the user domain**

```bash
git add -- egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java/common egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java/domain/user egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/test/java/domain/user egon-cola-archetypes/egon-cola-archetype-light/src/test/resources/projects/basic/verify.groovy
git commit -m "feat(archetype): add user domain contracts"
```

### Task 3: Add The Teaching Domain And Scheduling Invariant

**Files:**
- Create: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java/domain/teaching/entities/{SchoolClass,Course}.java`
- Create: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java/domain/teaching/aggregates/{SchoolClassAggregate,CourseAggregate}.java`
- Create: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java/domain/teaching/vos/{SchoolClassId,CourseCode,Semester,CourseSchedule,ExternalCourse,CourseSnapshot,TeachingEvent}.java`
- Create: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java/domain/teaching/enums/{SchoolClassStatus,CourseStatus}.java`
- Create: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java/domain/teaching/exceptions/TeachingDomainException.java`
- Create: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java/domain/teaching/validators/TeachingDomainValidator.java`
- Create: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java/domain/teaching/service/{SchoolClassDomainService,CourseDomainService,TeachingQueryService,CourseCacheService,TeachingEventPublisher}.java`
- Create: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java/domain/teaching/repos/{SchoolClassRepository,CourseRepository}.java`
- Create: corresponding `package-info.java` files
- Create: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/test/java/domain/teaching/aggregates/SchoolClassAggregateTest.java`
- Modify: `egon-cola-archetypes/egon-cola-archetype-light/src/test/resources/projects/basic/verify.groovy`

- [ ] **Step 1: Write the failing scheduling tests**

```java
@Test
void schedules_non_overlapping_courses() {
    SchoolClassAggregate aggregate = activeClass();
    aggregate.schedule(activeCourse("math"), schedule("math", 9, 10));
    aggregate.schedule(activeCourse("english"), schedule("english", 10, 11));
    assertEquals(2, aggregate.schedules().size());
}

@Test
void rejects_overlapping_course_times() {
    SchoolClassAggregate aggregate = activeClass();
    aggregate.schedule(activeCourse("math"), schedule("math", 9, 11));
    assertThrows(TeachingDomainException.class,
            () -> aggregate.schedule(activeCourse("english"), schedule("english", 10, 12)));
}

private static SchoolClassAggregate activeClass() {
    SchoolClass schoolClass = new SchoolClass(
            new SchoolClassId("class-1"), "Class One", new Semester("2026-FALL"), SchoolClassStatus.ACTIVE);
    return new SchoolClassAggregate(schoolClass);
}

private static Course activeCourse(String code) {
    return new Course("course-" + code, new CourseCode(code), code, CourseStatus.ACTIVE);
}

private static CourseSchedule schedule(String code, int startsAtHour, int endsAtHour) {
    LocalDate date = LocalDate.of(2026, 9, 1);
    return new CourseSchedule(
            new CourseCode(code),
            date.atTime(startsAtHour, 0),
            date.atTime(endsAtHour, 0));
}
```

Also test that inactive classes and disabled courses cannot be scheduled.

- [ ] **Step 2: Add verifier assertions and run the failing integration test**

Assert the exact `domain/teaching` files and `SchoolClassAggregateTest` exist. Run the integration command.

Expected: FAIL on the first absent teaching file.

- [ ] **Step 3: Implement teaching entities, values, services, repositories, and validator**

Use an immutable schedule value:

```java
public record CourseSchedule(CourseCode courseCode, LocalDateTime startsAt, LocalDateTime endsAt) {
    public CourseSchedule {
        Objects.requireNonNull(courseCode);
        Objects.requireNonNull(startsAt);
        Objects.requireNonNull(endsAt);
        if (!startsAt.isBefore(endsAt)) {
            throw new TeachingDomainException("INVALID_SCHEDULE", "startsAt must be before endsAt");
        }
    }

    public boolean overlaps(CourseSchedule other) {
        return startsAt.isBefore(other.endsAt()) && other.startsAt().isBefore(endsAt);
    }
}
```

`SchoolClassAggregate.schedule` must validate active class/course state, reject overlap, and retain a stable insertion order.

- [ ] **Step 4: Run focused teaching tests**

Run integration, then:

```bash
bash egon-cola-archetypes/egon-cola-archetype-light/target/test-classes/projects/basic/project/basic/mvnw -B -ntp -f egon-cola-archetypes/egon-cola-archetype-light/target/test-classes/projects/basic/project/basic/pom.xml -Dtest=SchoolClassAggregateTest test
```

Expected: BUILD SUCCESS; valid, overlapping, inactive-class, and disabled-course cases pass.

- [ ] **Step 5: Commit the teaching domain**

```bash
git add -- egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java/domain/teaching egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/test/java/domain/teaching egon-cola-archetypes/egon-cola-archetype-light/src/test/resources/projects/basic/verify.groovy
git commit -m "feat(archetype): add teaching domain contracts"
```

### Task 4: Implement User Application Use Cases

**Files:**
- Create: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java/application/user/command/{CreateUserCommand,AssignRoleCommand,GrantPermissionCommand}.java`
- Create: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java/application/user/query/GetUserQuery.java`
- Create: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java/application/user/result/{UserResult,PermissionResult}.java`
- Create: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java/application/user/manage/{UserManage,RoleManage,PermissionManage}.java`
- Create: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java/application/user/manage/UserUseCaseException.java`
- Create: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java/application/user/manage/impl/{UserManageImpl,RoleManageImpl,PermissionManageImpl}.java`
- Create: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java/application/user/{convertor/UserApplicationConvertor,validators/UserApplicationValidator,assemblers/UserAssembler}.java`
- Create: corresponding `package-info.java` files
- Create: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/test/java/application/user/manage/{UserManageTest,RoleManageTest,PermissionManageTest}.java`
- Modify: `egon-cola-archetypes/egon-cola-archetype-light/src/test/resources/projects/basic/verify.groovy`

- [ ] **Step 1: Write failing Mockito orchestration tests**

```java
@ExtendWith(MockitoExtension.class)
class UserManageTest {
    @Mock UserDomainService userDomainService;
    @Mock UserRepository userRepository;
    @Mock UserQueryService userQueryService;
    @Mock UserCacheService userCacheService;
    @Mock UserEventPublisher userEventPublisher;
    @Mock UserApplicationValidator applicationValidator;
    @Mock UserApplicationConvertor convertor;
    @InjectMocks UserManageImpl manage;

    @Test
    void creates_user_through_domain_ports() {
        CreateUserCommand command = new CreateUserCommand("ext-1", "Mario", "mario@example.com");
        when(userQueryService.findExternalUser("ext-1"))
                .thenReturn(Optional.of(new ExternalUser("ext-1", "Mario")));
        when(userDomainService.createUser("ext-1", "Mario", "mario@example.com"))
                .thenReturn(activeUser());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(convertor.toResult(any(User.class)))
                .thenReturn(new UserResult("u-1", "Mario", "mario@example.com", "ACTIVE"));

        UserResult result = manage.create(command);

        assertEquals("Mario", result.name());
        verify(userCacheService).evictUser(result.id());
        verify(userEventPublisher).publish(any(UserEvent.class));
    }
}
```

`RoleManageTest` verifies role lookup, assignment, and persistence. `PermissionManageTest` verifies permission lookup, grant, and event publication. Add failure tests for missing external user, disabled user, archived role, and inactive permission.

- [ ] **Step 2: Run the verifier and confirm application files are absent**

Add exact path assertions, then run integration.

Expected: FAIL for `application/user/manage/UserManage.java`.

- [ ] **Step 3: Implement boundary records and Manage classes**

Use records with constructor validation for commands and results. Apply `@Service`, Lombok `@RequiredArgsConstructor`, explicit `@Qualifier`, and `@Transactional` to Manage implementations. Convert Domain objects to Application Results inside `UserApplicationConvertor`; do not return Domain objects to Adapter.

The create flow is exactly:

```java
applicationValidator.validate(command);
userQueryService.findExternalUser(command.externalId())
        .orElseThrow(() -> new UserUseCaseException("EXTERNAL_USER_NOT_FOUND", "external user not found"));
User saved = userRepository.save(userDomainService.createUser(
        command.externalId(), command.name(), command.email()));
userCacheService.evictUser(saved.id().value());
userEventPublisher.publish(UserEvent.created(saved.id().value()));
return convertor.toResult(saved);
```

- [ ] **Step 4: Run user Application tests**

Run integration, then:

```bash
bash egon-cola-archetypes/egon-cola-archetype-light/target/test-classes/projects/basic/project/basic/mvnw -B -ntp -f egon-cola-archetypes/egon-cola-archetype-light/target/test-classes/projects/basic/project/basic/pom.xml -Dtest=UserManageTest,RoleManageTest,PermissionManageTest test
```

Expected: BUILD SUCCESS; happy paths and declared failure paths pass using Mockito without Spring context.

- [ ] **Step 5: Commit user Application**

```bash
git add -- egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java/application/user egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/test/java/application/user egon-cola-archetypes/egon-cola-archetype-light/src/test/resources/projects/basic/verify.groovy
git commit -m "feat(archetype): add user application flows"
```

### Task 5: Implement Teaching Application Use Cases

**Files:**
- Create: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java/application/teaching/command/{CreateSchoolClassCommand,CreateCourseCommand,ScheduleCourseCommand}.java`
- Create: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java/application/teaching/query/GetCourseQuery.java`
- Create: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java/application/teaching/result/{SchoolClassResult,CourseResult}.java`
- Create: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java/application/teaching/manage/{SchoolClassManage,CourseManage}.java`
- Create: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java/application/teaching/manage/TeachingUseCaseException.java`
- Create: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java/application/teaching/manage/impl/{SchoolClassManageImpl,CourseManageImpl}.java`
- Create: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java/application/teaching/{convertor/TeachingApplicationConvertor,validators/TeachingApplicationValidator,assemblers/TeachingAssembler}.java`
- Create: corresponding `package-info.java` files
- Create: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/test/java/application/teaching/manage/{SchoolClassManageTest,CourseManageTest}.java`
- Modify: `egon-cola-archetypes/egon-cola-archetype-light/src/test/resources/projects/basic/verify.groovy`

- [ ] **Step 1: Write failing course and scheduling orchestration tests**

`CourseManageTest` must verify external course lookup, Domain creation, repository save, cache eviction, and teaching event publication. `SchoolClassManageTest` must verify the Manage loads both class and course, delegates conflict enforcement to `SchoolClassDomainService`, persists the aggregate, and publishes a schedule event.

```java
verify(schoolClassDomainService).schedule(same(aggregate), same(course), same(schedule));
verify(schoolClassRepository).save(aggregate);
verify(teachingEventPublisher).publish(any(TeachingEvent.class));
```

- [ ] **Step 2: Add verifier assertions and run integration**

Expected: FAIL for the first missing `application/teaching` class.

- [ ] **Step 3: Implement teaching boundary models and Manage classes**

Use `LocalDateTime` in `ScheduleCourseCommand`, validate `startsAt < endsAt`, convert to `CourseSchedule`, and keep cross-domain behavior out of Adapter. `CourseManage.create` must consult `TeachingQueryService`; local/test implementations make this deterministic.

- [ ] **Step 4: Run teaching Application tests**

Run integration, then:

```bash
bash egon-cola-archetypes/egon-cola-archetype-light/target/test-classes/projects/basic/project/basic/mvnw -B -ntp -f egon-cola-archetypes/egon-cola-archetype-light/target/test-classes/projects/basic/project/basic/pom.xml -Dtest=SchoolClassManageTest,CourseManageTest test
```

Expected: BUILD SUCCESS.

- [ ] **Step 5: Commit teaching Application**

```bash
git add -- egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java/application/teaching egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/test/java/application/teaching egon-cola-archetypes/egon-cola-archetype-light/src/test/resources/projects/basic/verify.groovy
git commit -m "feat(archetype): add teaching application flows"
```

### Task 6: Add V2 And Domain-First JPA Repositories

**Files:**
- Create: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/resources/db/migration/V2__align_large_monolith_domain.sql`
- Create: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java/infrastructure/user/repo/po/{UserPO,RolePO,PermissionPO,UserRolePO,RolePermissionPO}.java`
- Create: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java/infrastructure/user/repo/jpa/{UserJpaRepository,RoleJpaRepository,PermissionJpaRepository,UserRoleJpaRepository,RolePermissionJpaRepository}.java`
- Create: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java/infrastructure/user/repo/converter/{UserPOConverter,RolePOConverter,PermissionPOConverter}.java`
- Create: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java/infrastructure/user/repo/impl/{UserRepositoryImpl,RoleRepositoryImpl,PermissionRepositoryImpl}.java`
- Create: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java/infrastructure/teaching/repo/po/{SchoolClassPO,CoursePO,ClassCourseSchedulePO}.java`
- Create: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java/infrastructure/teaching/repo/jpa/{SchoolClassJpaRepository,CourseJpaRepository,ClassCourseScheduleJpaRepository}.java`
- Create: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java/infrastructure/teaching/repo/converter/{SchoolClassPOConverter,CoursePOConverter}.java`
- Create: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java/infrastructure/teaching/repo/impl/{SchoolClassRepositoryImpl,CourseRepositoryImpl}.java`
- Create: corresponding `package-info.java` files
- Create: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/test/java/infrastructure/user/repo/UserRepositoryImplTest.java`
- Create: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/test/java/infrastructure/teaching/repo/SchoolClassRepositoryImplTest.java`
- Modify: `egon-cola-archetypes/egon-cola-archetype-light/src/test/resources/projects/basic/verify.groovy`

- [ ] **Step 1: Record and verify the immutable V1 checksum**

Run:

```bash
shasum -a 256 egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/resources/db/migration/V1__init_student_management.sql
```

Save the output in the task notes and compare it again before Task 12 completes.

- [ ] **Step 2: Write failing JPA slice tests**

Use `@DataJpaTest`, import only the relevant repository implementation and converters, save an aggregate, clear the persistence context, and assert roles, permissions, and schedules reconstruct correctly. Add a unique-email failure assertion and a schedule uniqueness assertion.

- [ ] **Step 3: Add verifier assertions and run integration**

Assert V1 and V2 both exist, the migration SQL file count is exactly two, and domain-first JPA paths exist. Assert `infrastructure/repo/user` and `infrastructure/repo/teaching` do not exist.

Expected: FAIL because V2 and JPA files are absent.

- [ ] **Step 4: Add the single V2 migration**

Create the migration with this operation order so existing course rows are backfilled before constraints are tightened:

```sql
CREATE TABLE users (
    id VARCHAR(64) PRIMARY KEY,
    external_id VARCHAR(64) NOT NULL,
    name VARCHAR(120) NOT NULL,
    email VARCHAR(160) NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_users_external_id UNIQUE (external_id),
    CONSTRAINT uk_users_email UNIQUE (email)
);

CREATE TABLE roles (
    code VARCHAR(64) PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE permissions (
    code VARCHAR(128) PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE user_roles (
    user_id VARCHAR(64) NOT NULL,
    role_code VARCHAR(64) NOT NULL,
    assigned_at TIMESTAMP NOT NULL,
    PRIMARY KEY (user_id, role_code),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_user_roles_role FOREIGN KEY (role_code) REFERENCES roles (code)
);

CREATE TABLE role_permissions (
    role_code VARCHAR(64) NOT NULL,
    permission_code VARCHAR(128) NOT NULL,
    granted_at TIMESTAMP NOT NULL,
    PRIMARY KEY (role_code, permission_code),
    CONSTRAINT fk_role_permissions_role FOREIGN KEY (role_code) REFERENCES roles (code),
    CONSTRAINT fk_role_permissions_permission FOREIGN KEY (permission_code) REFERENCES permissions (code)
);

CREATE TABLE school_classes (
    id VARCHAR(64) PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    semester VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NOT NULL
);

ALTER TABLE courses ADD COLUMN course_code VARCHAR(64);
ALTER TABLE courses ADD COLUMN status VARCHAR(32) DEFAULT 'ACTIVE';
UPDATE courses SET course_code = id WHERE course_code IS NULL;
UPDATE courses SET status = 'ACTIVE' WHERE status IS NULL;
ALTER TABLE courses ALTER COLUMN course_code SET NOT NULL;
ALTER TABLE courses ALTER COLUMN status SET NOT NULL;
ALTER TABLE courses ADD CONSTRAINT uk_courses_code UNIQUE (course_code);

CREATE TABLE class_course_schedules (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    school_class_id VARCHAR(64) NOT NULL,
    course_id VARCHAR(64) NOT NULL,
    starts_at TIMESTAMP NOT NULL,
    ends_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_class_course_start UNIQUE (school_class_id, course_id, starts_at),
    CONSTRAINT fk_schedule_class FOREIGN KEY (school_class_id) REFERENCES school_classes (id),
    CONSTRAINT fk_schedule_course FOREIGN KEY (course_id) REFERENCES courses (id)
);

INSERT INTO users (id, external_id, name, email, status, created_at)
SELECT id, CONCAT('legacy-', id), name, email, status, created_at
FROM students;

INSERT INTO roles (code, name, status, created_at)
VALUES ('teacher', 'Teacher', 'ACTIVE', CURRENT_TIMESTAMP);

INSERT INTO permissions (code, name, status, created_at)
VALUES ('course:read', 'Read courses', 'ACTIVE', CURRENT_TIMESTAMP);
```

The migration leaves `students` and `student_course_assignments` intact.

Use portable DDL for H2 and PostgreSQL: `VARCHAR`, `TIMESTAMP`, explicit primary keys, named unique constraints, and no vendor-specific functions.

- [ ] **Step 5: Implement PO, Spring Data, converter, and repository types**

Repository implementations must implement Domain Repository interfaces, translate PO/domain models through dedicated converters, and never import Application. Use explicit join PO types rather than JPA bidirectional entity graphs so aggregate reconstruction remains deterministic.

- [ ] **Step 6: Run JPA tests and verify V1 checksum**

Run integration, then:

```bash
bash egon-cola-archetypes/egon-cola-archetype-light/target/test-classes/projects/basic/project/basic/mvnw -B -ntp -f egon-cola-archetypes/egon-cola-archetype-light/target/test-classes/projects/basic/project/basic/pom.xml -Dtest=UserRepositoryImplTest,SchoolClassRepositoryImplTest test
```

Re-run the Step 1 checksum and require an exact match.

Expected: BUILD SUCCESS; both repository tests and Flyway startup pass.

- [ ] **Step 7: Commit persistence**

```bash
git add -- egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java/infrastructure/user/repo egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java/infrastructure/teaching/repo egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/resources/db/migration/V2__align_large_monolith_domain.sql egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/test/java/infrastructure egon-cola-archetypes/egon-cola-archetype-light/src/test/resources/projects/basic/verify.groovy
git commit -m "feat(archetype): add domain-first jpa persistence"
```

### Task 7: Implement Domain Services And Local Fallback Adapters

**Files:**
- Create: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java/infrastructure/user/service/impl/{UserDomainServiceImpl,RoleDomainServiceImpl,PermissionDomainServiceImpl}.java`
- Create: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java/infrastructure/teaching/service/impl/{SchoolClassDomainServiceImpl,CourseDomainServiceImpl}.java`
- Create: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java/infrastructure/user/client/impl/LocalUserQueryService.java`
- Create: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java/infrastructure/teaching/client/impl/LocalTeachingQueryService.java`
- Create: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java/infrastructure/user/cache/InMemoryUserCacheService.java`
- Create: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java/infrastructure/teaching/cache/InMemoryCourseCacheService.java`
- Create: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java/infrastructure/user/mq/LocalUserEventPublisher.java`
- Create: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java/infrastructure/teaching/mq/LocalTeachingEventPublisher.java`
- Create: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java/infrastructure/config/LocalAdapterConfiguration.java`
- Create: corresponding `package-info.java` files
- Create: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/test/java/infrastructure/config/LocalAdapterConfigurationTest.java`
- Modify: `egon-cola-archetypes/egon-cola-archetype-light/src/test/resources/projects/basic/verify.groovy`

- [ ] **Step 1: Write the failing local assembly test**

Use `ApplicationContextRunner` with `app.integrations.rabbitmq.enabled=false`, `app.integrations.redis.enabled=false`, and `app.integrations.external-http.enabled=false`. Assert exactly one bean exists for every Domain Service and outbound port and all beans come from Infrastructure domain-first packages.

- [ ] **Step 2: Add verifier assertions and run integration**

Expected: FAIL for missing Infrastructure service implementations.

- [ ] **Step 3: Implement Domain services and fallback ports**

Domain Service implementations delegate invariant enforcement to Domain entities, aggregates, and validators. Local query services use deterministic properties (`ext-1`, `COURSE-001`) rather than network calls. In-memory caches use `ConcurrentHashMap`. Local event publishers retain published events in a thread-safe list for assembly tests and never connect to RabbitMQ.

Use complementary conditional properties:

```java
@ConditionalOnProperty(name = "app.integrations.redis.enabled", havingValue = "false", matchIfMissing = true)
@ConditionalOnProperty(name = "app.integrations.rabbitmq.enabled", havingValue = "false", matchIfMissing = true)
@ConditionalOnProperty(name = "app.integrations.external-http.enabled", havingValue = "false", matchIfMissing = true)
```

- [ ] **Step 4: Run the local assembly test**

Run integration, then:

```bash
bash egon-cola-archetypes/egon-cola-archetype-light/target/test-classes/projects/basic/project/basic/mvnw -B -ntp -f egon-cola-archetypes/egon-cola-archetype-light/target/test-classes/projects/basic/project/basic/pom.xml -Dtest=LocalAdapterConfigurationTest test
```

Expected: BUILD SUCCESS and no attempted socket connection.

- [ ] **Step 5: Commit fallback Infrastructure**

```bash
git add -- egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java/infrastructure egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/test/java/infrastructure/config egon-cola-archetypes/egon-cola-archetype-light/src/test/resources/projects/basic/verify.groovy
git commit -m "feat(archetype): add local domain adapters"
```

### Task 8: Implement RabbitMQ, Redis, HTTP Clients, Validators, And AOP

**Files:**
- Create: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java/infrastructure/user/client/impl/RestUserQueryService.java`
- Create: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java/infrastructure/teaching/client/impl/RestTeachingQueryService.java`
- Create: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java/infrastructure/user/cache/RedisUserCacheService.java`
- Create: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java/infrastructure/teaching/cache/RedisCourseCacheService.java`
- Create: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java/infrastructure/user/mq/RabbitUserEventPublisher.java`
- Create: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java/infrastructure/teaching/mq/RabbitTeachingEventPublisher.java`
- Create: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java/infrastructure/user/validators/UserInfrastructureValidator.java`
- Create: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java/infrastructure/teaching/validators/TeachingInfrastructureValidator.java`
- Create: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java/infrastructure/aop/{RepositoryMonitorAspect,InfrastructureLogAspect}.java`
- Create: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java/infrastructure/config/{ExternalClientConfig,RabbitMqConfig,RedisConfig}.java`
- Modify: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/resources/{application,application-local,application-test,application-dev,application-prod}.yml`
- Create: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/test/java/infrastructure/user/client/RestUserQueryServiceTest.java`
- Create: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/test/java/infrastructure/teaching/client/RestTeachingQueryServiceTest.java`
- Create: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/test/java/infrastructure/user/mq/RabbitUserEventPublisherTest.java`
- Create: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/test/java/infrastructure/teaching/cache/RedisCourseCacheServiceTest.java`
- Modify: `egon-cola-archetypes/egon-cola-archetype-light/src/test/resources/projects/basic/verify.groovy`

- [ ] **Step 1: Write failing component tests with mocks**

Mock `RabbitTemplate` and verify routing keys `user.changed`, `authorization.changed`, `class.changed`, `course.changed`, and `schedule.changed`. Mock `StringRedisTemplate` and verify namespaced keys plus TTL. Bind `RestClient` to `MockRestServiceServer` and verify `/users/{externalId}` and `/courses/{courseCode}` response validation.

- [ ] **Step 2: Add verifier assertions and run integration**

Expected: FAIL because the real integration adapters and config classes are absent.

- [ ] **Step 3: Implement property-gated real adapters**

Use `havingValue = "true"` on each real adapter. RabbitMQ config declares one topic exchange, user/course imported queues, matching dead-letter queues, bindings, JSON conversion, and retry properties. Redis uses application/domain-prefixed keys and a configurable `Duration`. HTTP clients use `RestClient.Builder` and environment-backed base URLs.

Set defaults in `application.yml`:

```yaml
app:
  integrations:
    rabbitmq:
      enabled: false
      exchange: ${spring.application.name}.domain
    redis:
      enabled: false
      ttl: 10m
    external-http:
      enabled: false
      user-base-url: http://localhost:18081
      teaching-base-url: http://localhost:18082
```

Override all three flags to `false` in local/test and allow environment-backed `true` values in dev/prod.

- [ ] **Step 4: Run focused Infrastructure tests**

Run integration, then:

```bash
bash egon-cola-archetypes/egon-cola-archetype-light/target/test-classes/projects/basic/project/basic/mvnw -B -ntp -f egon-cola-archetypes/egon-cola-archetype-light/target/test-classes/projects/basic/project/basic/pom.xml -Dtest=RestUserQueryServiceTest,RestTeachingQueryServiceTest,RabbitUserEventPublisherTest,RedisCourseCacheServiceTest test
```

Expected: BUILD SUCCESS; tests use mocks only.

- [ ] **Step 5: Commit real Infrastructure adapters**

```bash
git add -- egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java/infrastructure egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/resources/application*.yml egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/test/java/infrastructure egon-cola-archetypes/egon-cola-archetype-light/src/test/resources/projects/basic/verify.groovy
git commit -m "feat(archetype): add external infrastructure adapters"
```

### Task 9: Add User HTTP, GraphQL, Facade, RPC, And MQ Adapters

**Files:**
- Create: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java/adapter/user/controller/{UserController,RoleController,PermissionController}.java`
- Create: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java/adapter/user/dto/{CreateUserRequest,AssignRoleRequest,GrantPermissionRequest}.java`
- Create: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java/adapter/user/vo/{UserDetailVO,PermissionTreeVO}.java`
- Create: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java/adapter/user/convertor/UserAdapterConvertor.java`
- Create: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java/adapter/user/validators/UserRequestValidator.java`
- Create: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java/adapter/user/graphql/UserResolver.java`
- Create: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java/adapter/user/facade/impl/{UserFacadeImpl,PermissionFacadeImpl}.java`
- Create: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java/adapter/user/rpc/UserRpcProvider.java`
- Create: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java/adapter/user/mq/UserImportedConsumer.java`
- Create: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java/facade/user/{UserFacade,PermissionFacade}.java`
- Create: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java/facade/user/dto/{CreateUserDTO,UserDetailDTO,PermissionDTO}.java`
- Create: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java/facade/user/enums/UserFacadeStatus.java`
- Create: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java/facade/user/exceptions/UserFacadeException.java`
- Create: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java/facade/user/utils/UserFacadeAssert.java`
- Create: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/resources/graphql/user.graphqls`
- Create: corresponding `package-info.java` files
- Create: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/test/java/adapter/user/{controller/UserControllerTest,graphql/UserResolverTest,rpc/UserRpcProviderTest,mq/UserImportedConsumerTest}.java`
- Modify: `egon-cola-archetypes/egon-cola-archetype-light/src/test/resources/projects/basic/verify.groovy`

- [ ] **Step 1: Write failing Adapter tests**

Use `@WebMvcTest(UserController.class)` and `@MockitoBean UserManage`. Assert POST `/api/users` maps request to `CreateUserCommand` and returns `UserDetailVO`. Test GraphQL via a mocked Application Manage. Test RPC and Consumer as plain JUnit 5 + Mockito classes and verify they call Application, not Domain.

- [ ] **Step 2: Add verifier assertions and run integration**

Assert `adapter/user/controller`, `adapter/user/mq`, and `adapter/user/rpc` exist while `adapter/controller/user`, `adapter/mq/user`, and `adapter/rpc/user` are absent.

Expected: FAIL on domain-first Adapter files.

- [ ] **Step 3: Implement user protocol models and adapters**

Controllers accept Adapter DTOs and return Adapter VOs. Facade implementations accept Facade DTOs and convert Application Results to Facade DTOs. `UserRpcProvider` is the Dubbo exposure class and delegates to `UserFacadeImpl`; it contains no business rule. `UserImportedConsumer` validates message shape, creates a command, and delegates to `UserManage`.

The GraphQL schema exposes:

```graphql
type Query {
  user(id: ID!): User
  permissions(userId: ID!): [Permission!]!
}
```

- [ ] **Step 4: Run focused user Adapter tests**

Run integration, then:

```bash
bash egon-cola-archetypes/egon-cola-archetype-light/target/test-classes/projects/basic/project/basic/mvnw -B -ntp -f egon-cola-archetypes/egon-cola-archetype-light/target/test-classes/projects/basic/project/basic/pom.xml -Dtest=UserControllerTest,UserResolverTest,UserRpcProviderTest,UserImportedConsumerTest test
```

Expected: BUILD SUCCESS.

- [ ] **Step 5: Commit user adapters**

```bash
git add -- egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java/adapter/user egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java/facade/user egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/resources/graphql/user.graphqls egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/test/java/adapter/user egon-cola-archetypes/egon-cola-archetype-light/src/test/resources/projects/basic/verify.groovy
git commit -m "feat(archetype): add domain-first user adapters"
```

### Task 10: Add Teaching And Shared Inbound Adapters

**Files:**
- Create: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java/adapter/teaching/controller/{SchoolClassController,CourseController}.java`
- Create: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java/adapter/teaching/dto/{CreateSchoolClassRequest,CreateCourseRequest,ScheduleCourseRequest}.java`
- Create: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java/adapter/teaching/vo/{SchoolClassDetailVO,CourseDetailVO}.java`
- Create: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java/adapter/teaching/convertor/TeachingAdapterConvertor.java`
- Create: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java/adapter/teaching/validators/TeachingRequestValidator.java`
- Create: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java/adapter/teaching/graphql/CourseResolver.java`
- Create: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java/adapter/teaching/facade/impl/{SchoolClassFacadeImpl,CourseFacadeImpl}.java`
- Create: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java/adapter/teaching/rpc/CourseRpcProvider.java`
- Create: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java/adapter/teaching/mq/CourseImportedConsumer.java`
- Create: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java/adapter/handler/{GlobalExceptionHandler,ResponseWrapperHandler}.java`
- Create: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java/adapter/filter/{TraceIdFilter,RequestContextFilter}.java`
- Create: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java/facade/teaching/{SchoolClassFacade,CourseFacade}.java`
- Create: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java/facade/teaching/dto/{CreateSchoolClassDTO,SchoolClassDetailDTO,CourseDTO}.java`
- Create: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java/facade/teaching/enums/CourseFacadeStatus.java`
- Create: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java/facade/teaching/exceptions/TeachingFacadeException.java`
- Create: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java/facade/teaching/utils/TeachingFacadeAssert.java`
- Create: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/resources/graphql/teaching.graphqls`
- Create: corresponding `package-info.java` files
- Create: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/test/java/adapter/teaching/{controller/CourseControllerTest,graphql/CourseResolverTest,rpc/CourseRpcProviderTest,mq/CourseImportedConsumerTest}.java`
- Create: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/test/java/adapter/filter/RequestContextFilterTest.java`
- Modify: `egon-cola-archetypes/egon-cola-archetype-light/src/test/resources/projects/basic/verify.groovy`

- [ ] **Step 1: Write failing teaching and shared Adapter tests**

Mirror user Adapter test boundaries with teaching-specific commands and results. Filter tests assert `X-Trace-Id`, `X-Operator-Id`, and `X-Tenant-Id` are stored for the request and cleared in `finally` after the chain completes.

- [ ] **Step 2: Add domain-first verifier assertions and run integration**

Expected: FAIL for missing `adapter/teaching/controller/CourseController.java`.

- [ ] **Step 3: Implement teaching and shared adapters**

The GraphQL schema exposes `schoolClass(id: ID!)` and `course(id: ID!)`. Controllers and Facade implementations convert only Application boundary models. `GlobalExceptionHandler` catches Application exceptions; it must not import Domain or Common.

- [ ] **Step 4: Run focused Adapter tests**

Run integration, then:

```bash
bash egon-cola-archetypes/egon-cola-archetype-light/target/test-classes/projects/basic/project/basic/mvnw -B -ntp -f egon-cola-archetypes/egon-cola-archetype-light/target/test-classes/projects/basic/project/basic/pom.xml -Dtest=CourseControllerTest,CourseResolverTest,CourseRpcProviderTest,CourseImportedConsumerTest,RequestContextFilterTest test
```

Expected: BUILD SUCCESS.

- [ ] **Step 5: Commit teaching and shared adapters**

```bash
git add -- egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java/adapter/teaching egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java/adapter/handler egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java/adapter/filter egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java/facade/teaching egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/resources/graphql/teaching.graphqls egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/test/java/adapter egon-cola-archetypes/egon-cola-archetype-light/src/test/resources/projects/basic/verify.groovy
git commit -m "feat(archetype): add teaching and shared adapters"
```

### Task 11: Cut Over Startup, Enforce Architecture, And Remove Student Code

**Files:**
- Modify: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java/start/StudentManagementApplication.java`
- Create: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java/start/config/{JacksonConfig,OpenApiConfig,ActuatorConfig}.java`
- Modify: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/test/java/ArchitectureDependencyTest.java`
- Create: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/test/java/start/StudentManagementApplicationTest.java`
- Delete: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java/adapter/controller`
- Delete: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java/adapter/convertor`
- Delete: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java/adapter/facade`
- Delete: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java/adapter/validation`
- Delete: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java/adapter/vo`
- Delete: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java/application/config`
- Delete: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java/application/convertor`
- Delete: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java/application/manage`
- Delete: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java/application/validators`
- Delete: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java/domain/common`
- Delete: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java/domain/student`
- Delete: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java/domain/teaching/model`
- Delete: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java/domain/teaching/repos`
- Delete: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java/domain/teaching/service`
- Delete: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java/facade/api`
- Delete: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java/facade/dto`
- Delete: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java/facade/enums`
- Delete: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java/infrastructure/repo`
- Delete: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java/common/constants/ErrorCodes.java`
- Delete: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java/common/exceptions/BizException.java`
- Delete: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java/common/exceptions/NotFoundException.java`
- Delete: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java/common/response`
- Delete: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java/common/utils/IdGenerator.java`
- Delete: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/test/java/application/StudentManagementFlowTest.java`
- Modify: `egon-cola-archetypes/egon-cola-archetype-light/src/test/resources/projects/basic/verify.groovy`

- [ ] **Step 1: Replace ArchitectureDependencyTest with failing exact rules**

Create one rule per confirmed edge. The Adapter rule forbids Domain/Common/Infrastructure/Start; Application forbids Adapter/Facade/Common/Infrastructure/Start; Infrastructure forbids Application/Adapter/Facade/Common/Start; Domain forbids Adapter/Application/Facade/Infrastructure/Start; Facade and Common forbid every other internal layer.

Add domain-first placement checks:

```java
import com.tngtech.archunit.core.domain.JavaClass;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

Set<String> packageNames = classes.stream()
        .map(JavaClass::getPackageName)
        .collect(Collectors.toSet());
assertThat(packageNames).noneMatch(name -> name.startsWith("${package}.adapter.controller"));
assertThat(packageNames).noneMatch(name -> name.startsWith("${package}.application.manage"));
assertThat(packageNames).noneMatch(name -> name.startsWith("${package}.infrastructure.repo"));
```

- [ ] **Step 2: Run integration and capture violations from old code**

Expected: FAIL because the old technology-first Student sample violates the final rules.

- [ ] **Step 3: Cut over startup and remove old Java paths**

Update component and Dubbo scans to the new domain-first Adapter and Infrastructure packages. Preserve async/encryption runtime config. Add Jackson, OpenAPI, and Actuator configuration without business imports. Delete only the listed obsolete Java paths; keep Flyway V1 unchanged.

- [ ] **Step 4: Add a no-external-service Spring Boot assembly test**

```java
@SpringBootTest(properties = {
        "spring.profiles.active=test",
        "app.integrations.rabbitmq.enabled=false",
        "app.integrations.redis.enabled=false",
        "app.integrations.external-http.enabled=false",
        "dubbo.protocol.port=-1"
})
class StudentManagementApplicationTest {
    @Test
    void contextLoads() {
    }
}
```

- [ ] **Step 5: Run architecture and assembly tests**

Run integration, then:

```bash
bash egon-cola-archetypes/egon-cola-archetype-light/target/test-classes/projects/basic/project/basic/mvnw -B -ntp -f egon-cola-archetypes/egon-cola-archetype-light/target/test-classes/projects/basic/project/basic/pom.xml -Dtest=ArchitectureDependencyTest,StudentManagementApplicationTest test
```

Expected: BUILD SUCCESS; no external connection is attempted.

- [ ] **Step 6: Commit the cutover**

```bash
git add -A -- egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/test/java egon-cola-archetypes/egon-cola-archetype-light/src/test/resources/projects/basic/verify.groovy
git commit -m "refactor(archetype): cut over lite domain architecture"
```

### Task 12: Harden Verification, Documentation, And Final Generated Output

**Files:**
- Modify: `egon-cola-archetypes/egon-cola-archetype-light/src/test/resources/projects/basic/verify.groovy`
- Modify: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/README.md`
- Modify: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/META-INF/maven/archetype-metadata.xml`
- Modify: package documentation files found incomplete during generated-tree review

- [ ] **Step 1: Add final generated-tree assertions**

Verify all representative files, both GraphQL schemas, V1 + one V2, local/test integration flags, Maven Wrapper, Docker files, and Spring Boot Test. Assert all reversed roots are absent:

```groovy
[
    "src/main/java/it/pkg/adapter/controller/user",
    "src/main/java/it/pkg/adapter/mq/user",
    "src/main/java/it/pkg/application/manage/user",
    "src/main/java/it/pkg/infrastructure/repo/user",
    "src/main/java/it/pkg/domain/student"
].each { path ->
    assert !new File(generatedProjectDir, path).exists(): "Unexpected reversed or stale path ${path}"
}
assert !new File(generatedProjectDir, "src/main/java/it/pkg/application/client").exists()
assert !new File(generatedProjectDir, "src/main/java/it/pkg/domain/user/service/impl").exists()
assert !new File(generatedProjectDir, "src/main/java/it/pkg/domain/teaching/service/impl").exists()
```

Scan generated Java imports and fail if Infrastructure imports Application, Adapter imports Domain/Common, or Facade imports any internal layer.

- [ ] **Step 2: Run integration and use failures as the documentation/file completeness checklist**

Run the light archetype integration command.

Expected: BUILD SUCCESS. A missing-file assertion means a prior task is incomplete and must be resolved before continuing.

- [ ] **Step 3: Rewrite the generated README**

Document the single-module monolith, domain-first package tree, exact dependency graph, five primary workflows, local/test fallbacks, dev/prod integration flags, Maven commands, Docker build, and configuration encryption. Explain that `user` and `teaching` can later be extracted without reversing package order.

- [ ] **Step 4: Run complete verification**

Run:

```bash
bash ./mvnw -B -ntp -pl egon-cola-archetypes/egon-cola-archetype-light -am clean integration-test
```

Then run:

```bash
bash egon-cola-archetypes/egon-cola-archetype-light/target/test-classes/projects/basic/project/basic/mvnw -B -ntp -f egon-cola-archetypes/egon-cola-archetype-light/target/test-classes/projects/basic/project/basic/pom.xml test
```

Expected: both commands report BUILD SUCCESS.

- [ ] **Step 5: Run static final checks**

```bash
git diff --check
rg -n "StudentController|StudentManagementFacade|domain\.student|adapter\.controller\.user|application\.manage\.user|infrastructure\.repo\.user|mybatis-plus" egon-cola-archetypes/egon-cola-archetype-light/src/main/resources egon-cola-archetypes/egon-cola-archetype-light/src/test/resources
```

Expected: `git diff --check` succeeds. The stale-name search returns only intentional negative assertions in `verify.groovy`; no generated source, POM, README, or active test uses those names.

Re-run the V1 SHA-256 command from Task 6 and require the same checksum.

- [ ] **Step 6: Review scope and commit final hardening**

Run `git status --short` and confirm every changed implementation file is under `egon-cola-archetypes/egon-cola-archetype-light`.

```bash
git add -- egon-cola-archetypes/egon-cola-archetype-light
git commit -m "test(archetype): harden lite generation contract"
```

## Completion Checklist

- [ ] Twelve task commits exist and each commit is limited to its declared scope.
- [ ] The generated project has one POM and no Maven child modules.
- [ ] User and teaching packages are domain-first across Adapter, Facade, Application, Domain, and Infrastructure.
- [ ] The only internal layer edges are the seven confirmed directions.
- [ ] Domain Service interfaces are in Domain and implementations are in the matching Infrastructure domain subtree.
- [ ] `application.client` and Domain-local `service.impl` packages are absent.
- [ ] JPA is the only persistence technology.
- [ ] RabbitMQ, Redis, GraphQL, Dubbo, external HTTP, AOP, filters, converters, and validators are exercised by tests.
- [ ] `spring-boot-starter-test` supplies JUnit 5 and Mockito.
- [ ] Local/test requires no external service.
- [ ] V1 is unchanged and exactly one V2 migration exists.
- [ ] Archetype integration and generated-project tests pass.
- [ ] No application process was started.
