# Student Management Organization Web Archetype Alignment Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Rebuild `egon-cola-archetype-web` so one invocation generates the complete organization-only `user + teaching` sample defined by the approved architecture spec.

**Architecture:** Keep one generated Maven reactor with seven modules and the exact dependency graph `common`, `facade`, `domain -> common`, `application -> domain`, `infrastructure -> domain`, `adapter -> application + facade`, and `starter -> adapter + infrastructure`. Build the sample in vertical increments so every commit still generates and verifies a usable project; remove legacy compatibility classes only after all consumers have moved to the approved Command/Query/Result and JPA-only contracts.

**Tech Stack:** Maven Archetype Plugin, Java 21, Spring Boot 3.5.16, Spring MVC, Spring GraphQL, Apache Dubbo 3.3.6, Spring AMQP, Spring Data Redis, Spring Data JPA, Flyway, H2 PostgreSQL mode, PostgreSQL runtime driver, Springdoc OpenAPI, MapStruct Plus, JUnit 5, Mockito, ArchUnit, Maven Wrapper 3.9.14, Docker.

---

## Scope And Source Of Truth

This is one implementation plan because the Facade contracts, both domain verticals, runtime profiles, and archetype verification all ship as one generated Project and share one acceptance path. `student-management-evaluation` and the deferred bidirectional consumer integration remain outside this plan.

Use these documents in this order:

1. `docs/superpowers/specs/2026-07-10-student-management-organization-web-archetype-alignment-design.md`
2. `egon-cola-archetypes/egon-cola-archetype-web/multi-project-multi-module-architecture.md`
3. `docs/superpowers/specs/2026-07-10-evaluation-organization-facade-integration-design.md` only to enforce the deferred boundary

Run every command from `/Users/mario/SelfProject/Egon-COLA`. Never start the generated application. The generated integration-test project is located at:

```text
egon-cola-archetypes/egon-cola-archetype-web/target/test-classes/projects/basic/project/student-management-organization
```

The standard red/green command is:

```bash
bash ./mvnw -B -ntp -pl egon-cola-archetypes/egon-cola-archetype-web -am clean integration-test
```

For a focused generated-project test after generation succeeds, use:

```bash
bash ./mvnw -B -ntp \
  -f egon-cola-archetypes/egon-cola-archetype-web/target/test-classes/projects/basic/project/student-management-organization/pom.xml \
  -pl student-management-organization-starter -am \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest=it.pkg.starter.OrganizationFlowTest test
```

## Locked File Map

The approved spec's `Canonical Organization File Manifest` is the exhaustive required/forbidden file list. The implementation tasks below group those files by behavior; they do not weaken the manifest.

Core build and generation files:

```text
egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/pom.xml
egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-{common,facade,domain,application,infrastructure,adapter,starter}/pom.xml
egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/META-INF/maven/archetype-metadata.xml
egon-cola-archetypes/egon-cola-archetype-web/src/test/resources/projects/basic/{archetype.properties,goal.txt,verify.groovy}
```

Generated business packages:

```text
common/{constants,utils,enums,exceptions}
facade/{user,teaching,dto/user,dto/teaching,enums,exceptions,utils}
domain/{entities,aggregates,vos,service,repos,client,events,exceptions,validators,enums}
application/{config,context,exceptions,manage,command,query,result,converter,validators,assemblers}
infrastructure/{repo,cache,mq,aop,config,validators}
adapter/{controller,mq,rpc,graphql,facade/impl,dto,vo,converter,validators,handler,filter}
starter/config
```

Required generated tests are distributed across all seven modules. `starter/ArchitectureDependencyTest` imports production classes from the complete reactor; `starter/OrganizationFlowTest` and `starter/OrganizationRollbackTest` are the final no-mock acceptance tests.

## Design Pattern Decisions

- Use Ports and Adapters for repositories, cache, idempotency, events, local fallbacks, and inbound protocols.
- Use Application Service for transaction boundaries, authorization, idempotency, and the cross-domain membership operation.
- Use Domain Service and Aggregate for invariant-heavy user, role/permission, grade, and school-class behavior.
- Use Repository for JPA isolation and Observer-style Domain events for after-commit publication.
- Use an Anti-Corruption Layer only when the separately deferred external Facade client is approved; do not generate it here.
- Do not add persistence Strategy, factory hierarchies, chains, decorators, or an outbox because the approved scope has one persistence choice and no justified runtime variant beyond conditional real/fallback adapters.

### Task 1: Establish Verify Lifecycle, Dependency Baseline, And Resource Generation

**Files:**
- Modify: `egon-cola-archetypes/egon-cola-archetype-web/src/test/resources/projects/basic/goal.txt`
- Modify: `egon-cola-archetypes/egon-cola-archetype-web/src/test/resources/projects/basic/verify.groovy`
- Modify: `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/pom.xml`
- Modify: all seven generated module `pom.xml` files under `archetype-resources`
- Modify: `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/META-INF/maven/archetype-metadata.xml`
- Create: `src/main/resources/.gitkeep` under generated Common, Facade, Domain, and Application modules

- [ ] **Step 1: Strengthen the generation guard before changing POMs**

Change `goal.txt` to:

```text
verify
```

Add these assertions to `verify.groovy` while retaining the existing wrapper, Docker, profile, and version checks:

```groovy
def modules = ["common", "facade", "domain", "application", "infrastructure", "adapter", "starter"]
modules.each { module ->
    def root = "student-management-organization-${module}"
    assertDir(root)
    ["src/main/java", "src/main/resources", "src/test/java", "src/test/resources"].each {
        assertDir("${root}/${it}")
    }
}

def pomText = { module -> assertFile("student-management-organization-${module}/pom.xml").text }
assert pomText("adapter").contains("spring-boot-starter-graphql")
assert pomText("adapter").contains("spring-boot-starter-amqp")
assert pomText("adapter").contains("springdoc-openapi-starter-webmvc-api")
assert pomText("infrastructure").contains("spring-boot-starter-data-redis")
assert pomText("infrastructure").contains("spring-boot-starter-amqp")
assert pomText("infrastructure").contains("spring-boot-starter-aop")
assert pomText("starter").contains("springdoc-openapi-starter-webmvc-ui")
```

- [ ] **Step 2: Run the archetype IT and verify the new guard fails**

Run:

```bash
bash ./mvnw -B -ntp -pl egon-cola-archetypes/egon-cola-archetype-web -am clean integration-test
```

Expected: FAIL in `verify.groovy` because the integration dependencies and empty resource directories are not generated yet.

- [ ] **Step 3: Add the root version properties and module dependencies**

Add one root property and dependency-management entry for Springdoc:

```xml
<springdoc.version>2.8.13</springdoc.version>

<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>${springdoc.version}</version>
</dependency>
```

Add these exact direct dependencies without changing the internal module graph:

```xml
<!-- adapter -->
<dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-graphql</artifactId></dependency>
<dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-amqp</artifactId></dependency>
<dependency><groupId>org.springdoc</groupId><artifactId>springdoc-openapi-starter-webmvc-api</artifactId></dependency>

<!-- infrastructure -->
<dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-data-redis</artifactId></dependency>
<dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-amqp</artifactId></dependency>
<dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-aop</artifactId></dependency>

<!-- starter -->
<dependency><groupId>org.springdoc</groupId><artifactId>springdoc-openapi-starter-webmvc-ui</artifactId></dependency>
```

Add the test dependencies required by the approved matrix:

```xml
<!-- common, facade, domain -->
<dependency><groupId>org.junit.jupiter</groupId><artifactId>junit-jupiter</artifactId><scope>test</scope></dependency>

<!-- facade only -->
<dependency><groupId>org.hibernate.validator</groupId><artifactId>hibernate-validator</artifactId><scope>test</scope></dependency>
<dependency><groupId>com.fasterxml.jackson.core</groupId><artifactId>jackson-databind</artifactId><scope>test</scope></dependency>

<!-- application only -->
<dependency><groupId>org.junit.jupiter</groupId><artifactId>junit-jupiter</artifactId><scope>test</scope></dependency>
<dependency><groupId>org.mockito</groupId><artifactId>mockito-junit-jupiter</artifactId><scope>test</scope></dependency>

<!-- infrastructure and adapter -->
<dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-test</artifactId><scope>test</scope></dependency>

<!-- adapter only -->
<dependency><groupId>org.springframework.graphql</groupId><artifactId>spring-graphql-test</artifactId><scope>test</scope></dependency>
```

- [ ] **Step 4: Make empty resource directories reproducible**

Create `.gitkeep` in Common, Facade, Domain, and Application `src/main/resources`, then add this include to each matching metadata file set:

```xml
<include>.gitkeep</include>
```

Keep Java file sets `filtered="true" packaged="true"`; keep wrapper scripts unfiltered; keep `META-INF/**`, GraphQL, SQL, YAML, XML, and properties resources filtered according to their current ownership.

- [ ] **Step 5: Run the archetype IT and verify the build baseline passes**

Run the standard red/green command.

Expected: PASS; the generated project executes `verify`, all four directories exist in each module, and no external service is contacted.

- [ ] **Step 6: Commit the baseline**

```bash
git add egon-cola-archetypes/egon-cola-archetype-web/src/main/resources \
  egon-cola-archetypes/egon-cola-archetype-web/src/test/resources/projects/basic
git commit -m "build(archetype): prepare organization integration baseline"
```

### Task 2: Add The Backward-Safe V2 Flyway Migration

**Files:**
- Create: `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/main/resources/db/migration/V2__complete_organization_domains.sql`
- Create: `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/test/java/infrastructure/OrganizationFlywayMigrationTest.java`
- Create: corresponding `package-info.java`
- Modify: `verify.groovy`

- [ ] **Step 1: Write the migration contract test**

Create a JUnit test that migrates only V1, inserts legacy duplicates and an orphan membership, then migrates V2:

```java
package ${package}.infrastructure;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;

import static org.assertj.core.api.Assertions.assertThat;

class OrganizationFlywayMigrationTest {
    private final DataSource dataSource = TestDataSources.h2PostgreSqlMode("migration-contract");

    @Test
    void migratesLegacyTeachingRowsWithoutDeletingDuplicatesOrOrphans() throws Exception {
        Flyway.configure().dataSource(dataSource).target("1").load().migrate();
        try (Connection c = dataSource.getConnection()) {
            c.createStatement().executeUpdate("INSERT INTO users VALUES ('u-1','Mario','mario@example.com','ACTIVE',CURRENT_TIMESTAMP)");
            c.createStatement().executeUpdate("INSERT INTO school_classes VALUES ('c-1','Class A','Grade One',CURRENT_TIMESTAMP)");
            c.createStatement().executeUpdate("INSERT INTO school_classes VALUES ('c-2','Class A','Grade One',CURRENT_TIMESTAMP)");
            c.createStatement().executeUpdate("INSERT INTO school_class_users(user_id,school_class_id,created_at) VALUES ('u-1','c-1',CURRENT_TIMESTAMP)");
            c.createStatement().executeUpdate("INSERT INTO school_class_users(user_id,school_class_id,created_at) VALUES ('missing-user','missing-class',CURRENT_TIMESTAMP)");
        }

        Flyway flyway = Flyway.configure().dataSource(dataSource).load();
        flyway.migrate();
        flyway.validate();

        try (Connection c = dataSource.getConnection()) {
            ResultSet grade = c.createStatement().executeQuery("SELECT id,code,status FROM grades WHERE id='legacy:Grade One'");
            assertThat(grade.next()).isTrue();
            assertThat(grade.getString("code")).isEqualTo("Grade One");
            assertThat(grade.getString("status")).isEqualTo("ACTIVE");
            assertThat(count(c, "school_classes")).isEqualTo(2);
            assertThat(count(c, "school_class_users")).isEqualTo(2);
            assertThat(count(c, "roles")).isEqualTo(1);
            assertThat(count(c, "permissions")).isEqualTo(1);
        }
    }

    private static int count(Connection c, String table) throws Exception {
        ResultSet rs = c.createStatement().executeQuery("SELECT COUNT(*) FROM " + table);
        rs.next();
        return rs.getInt(1);
    }
}
```

Define this package-private helper in the same test source:

```java
final class TestDataSources {
    private TestDataSources() {}

    static DataSource h2PostgreSqlMode(String name) {
        org.h2.jdbcx.JdbcDataSource dataSource = new org.h2.jdbcx.JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:" + name + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1");
        dataSource.setUser("sa");
        dataSource.setPassword("");
        return dataSource;
    }
}
```

- [ ] **Step 2: Run the test and verify it fails**

Run the standard red/green command.

Expected: FAIL because `grades`, `roles`, `permissions`, `user_roles`, and `role_permissions` do not exist.

- [ ] **Step 3: Create the exact V2 migration**

Use one migration only. Its core SQL must be:

```sql
CREATE TABLE roles (
    id VARCHAR(64) PRIMARY KEY,
    code VARCHAR(64) NOT NULL UNIQUE,
    name VARCHAR(120) NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE permissions (
    id VARCHAR(64) PRIMARY KEY,
    code VARCHAR(64) NOT NULL UNIQUE,
    name VARCHAR(120) NOT NULL,
    type VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE user_roles (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL,
    role_id VARCHAR(64) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_user_role UNIQUE (user_id, role_id),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_user_roles_role FOREIGN KEY (role_id) REFERENCES roles(id)
);

CREATE TABLE role_permissions (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    role_id VARCHAR(64) NOT NULL,
    permission_id VARCHAR(64) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_role_permission UNIQUE (role_id, permission_id),
    CONSTRAINT fk_role_permissions_role FOREIGN KEY (role_id) REFERENCES roles(id),
    CONSTRAINT fk_role_permissions_permission FOREIGN KEY (permission_id) REFERENCES permissions(id)
);

CREATE TABLE grades (
    id VARCHAR(160) PRIMARY KEY,
    code VARCHAR(160) NOT NULL UNIQUE,
    name VARCHAR(120) NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NOT NULL
);

INSERT INTO grades(id, code, name, status, created_at)
SELECT CONCAT('legacy:', grade_name), grade_name, grade_name, 'ACTIVE', MIN(created_at)
FROM school_classes
GROUP BY grade_name;

ALTER TABLE school_classes ADD COLUMN grade_id VARCHAR(160);
ALTER TABLE school_classes ADD COLUMN status VARCHAR(32) DEFAULT 'ACTIVE' NOT NULL;
UPDATE school_classes SET grade_id = CONCAT('legacy:', grade_name);
ALTER TABLE school_classes ALTER COLUMN grade_id SET NOT NULL;
ALTER TABLE school_classes ADD CONSTRAINT fk_school_classes_grade FOREIGN KEY (grade_id) REFERENCES grades(id);
CREATE INDEX idx_school_classes_grade_id ON school_classes(grade_id);
CREATE INDEX idx_user_roles_role_id ON user_roles(role_id);
CREATE INDEX idx_role_permissions_permission_id ON role_permissions(permission_id);

INSERT INTO roles(id, code, name, status, created_at)
VALUES ('role-student', 'STUDENT', 'Student', 'ACTIVE', CURRENT_TIMESTAMP);

INSERT INTO permissions(id, code, name, type, status, created_at)
VALUES ('permission-class-read', 'CLASS_READ', 'Read school class', 'API', 'ACTIVE', CURRENT_TIMESTAMP);
```

Do not alter V1, do not add a foreign key to `school_class_users`, and do not add a unique key to `(grade_id, name)`.

- [ ] **Step 4: Pin V1 and V2 in the archetype verifier**

Add:

```groovy
import java.security.MessageDigest

def sha256 = { File file ->
    MessageDigest.getInstance("SHA-256").digest(file.bytes).encodeHex().toString()
}
def migrationDir = "student-management-organization-infrastructure/src/main/resources/db/migration"
assert sha256(assertFile("${migrationDir}/V1__init_student_management_organization.sql")) ==
        "c5481736a3ffefc45197a767aec26c1462bb338dfccc1d11751a782ac3de6df1"
assertFile("${migrationDir}/V2__complete_organization_domains.sql")
assert new File(projectDir, migrationDir).listFiles().findAll { it.name.startsWith("V2__") }.size() == 1
assertMissing("student-management-organization-infrastructure/src/test/resources/db/migration")
```

- [ ] **Step 5: Run the migration contract and full IT**

Run the standard red/green command.

Expected: PASS; Flyway validates V1 -> V2, duplicates and orphan membership survive, and only the two production migrations exist.

- [ ] **Step 6: Commit the migration**

```bash
git add egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure \
  egon-cola-archetypes/egon-cola-archetype-web/src/test/resources/projects/basic/verify.groovy
git commit -m "feat(archetype): add organization V2 migration"
```

### Task 3: Implement The User Create And Lookup Vertical

**Files:**
- Create/modify Common types named in architecture section 4.2.2
- Create/modify Facade: `UserFacade`, `CreateUserDTO`, `UserDetailDTO`, Facade enums/error/utility, contract test
- Create/modify Domain user entity, aggregate, value objects, service, repository, validator, enum, and error types
- Create/modify Application user Command, Query, Result, Manage, converter, validator, assembler, context, exception
- Create/modify Infrastructure user PO, JPA repository, repository implementation, converter, repository test
- Create/modify Adapter `UserController`, request/VO/converter/validator, `UserFacadeImpl`, controller test
- Keep legacy teaching files compiling until Task 5

- [ ] **Step 1: Write failing Common, Facade, Domain, Application, repository, and controller tests**

The tests must lock these contracts:

```java
Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
assertThat(new CreateUserDTO("Mario", "MARIO@EXAMPLE.COM").email()).isEqualTo("MARIO@EXAMPLE.COM");
assertThat(validator.validate(new CreateUserDTO("", "bad"))).isNotEmpty();

User user = userDomainService.create(new UserId("u-1"), " Mario ", "MARIO@EXAMPLE.COM");
assertThat(user.email()).isEqualTo("mario@example.com");
assertThat(user.status()).isEqualTo(UserStatus.ACTIVE);

when(userRepository.existsByEmail("mario@example.com")).thenReturn(false);
UserDetailResult result = userManage.createUser(new CreateUserCommand("req-1", "Mario", "MARIO@EXAMPLE.COM"));
assertThat(result.email()).isEqualTo("mario@example.com");
verify(userRepository).save(any(User.class));

mockMvc.perform(post("/api/v1/users")
        .header("X-Actor-Id", "admin-1")
        .header("X-Actor-Roles", "ORGANIZATION_ADMIN")
        .contentType(APPLICATION_JSON)
        .content("{\"name\":\"Mario\",\"email\":\"mario@example.com\"}"))
    .andExpect(status().isCreated())
    .andExpect(header().string("Location", startsWith("/api/v1/users/")))
    .andExpect(jsonPath("$.email").value("mario@example.com"));
```

- [ ] **Step 2: Run the standard command and verify the tests fail**

Expected: FAIL because the new DTOs, `UserAggregate`, Command/Query/Result path, direct HTTP VO, and normalized JPA model do not exist.

- [ ] **Step 3: Create the User Domain contract**

Use these signatures and normalization rules:

```java
public record UserId(String value) {
    public UserId {
        if (value == null || value.isBlank()) throw new OrganizationDomainException(OrganizationDomainErrorCode.INVALID_USER_ID, "userId must not be blank");
    }
}

public record User(UserId id, String name, String email, UserStatus status, List<RoleCode> roleCodes) {
    public User {
        name = name.trim();
        email = email.trim().toLowerCase(Locale.ROOT);
        roleCodes = List.copyOf(roleCodes);
    }
}

public interface UserRepository {
    User save(User user);
    Optional<User> findById(UserId userId);
    boolean existsByEmail(String normalizedEmail);
}

public interface UserDomainService {
    User create(UserId userId, String name, String email);
}
```

`UserDomainServiceImpl.create` validates nonblank trimmed name, a normalized email, and returns an ACTIVE user with no roles. `UserDomainValidator` owns these invariant checks; neither class imports Spring or Jakarta persistence.

- [ ] **Step 4: Create the Application API and minimal implementation**

Use:

```java
public record CreateUserCommand(String requestId, String name, String email) {}
public record UserDetailQuery(String userId) {}
public record UserDetailResult(String id, String name, String email, String status, List<String> roleCodes) {}

public interface UserManage {
    UserDetailResult createUser(CreateUserCommand command);
    UserDetailResult getUser(UserDetailQuery query);
}
```

`UserManageImpl` must require `ORGANIZATION_ADMIN` for create, reject normalized duplicate email as `CONFLICT`, save through `UserRepository`, and use `UserAssembler` for the Result. `getUser` has no role requirement and throws `NOT_FOUND` when the repository returns empty. Task 7 adds the approved idempotency claim/release around this mutation after all mutation APIs exist.

- [ ] **Step 5: Replace the user Facade and HTTP boundary**

Use exactly:

```java
public interface UserFacade {
    UserDetailDTO createUser(@Valid @NotNull CreateUserDTO request);
    UserDetailDTO getUser(@NotBlank String userId);
}

@PostMapping("/api/v1/users")
public ResponseEntity<UserDetailVO> create(
        @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
        @Valid @RequestBody CreateUserRequest request) {
    String requestId = idempotencyKey == null ? UUID.randomUUID().toString() : idempotencyKey;
    UserDetailVO body = converter.toVo(userManage.createUser(converter.toCommand(requestId, request)));
    return ResponseEntity.created(URI.create("/api/v1/users/" + body.id())).body(body);
}
```

`UserFacadeImpl` converts Facade DTO -> Application Command and Application Result -> Facade DTO. It catches only `OrganizationApplicationException` and translates it to `OrganizationFacadeException`.

- [ ] **Step 6: Implement JPA persistence for user create/lookup**

`UserPO` maps `users`; `UserJpaRepository` adds `boolean existsByEmail(String email)`; `UserRepositoryImpl` implements only Domain `UserRepository`. Store normalized email and status string, and restore `UserStatus` explicitly. Do not use the legacy `domain.client.user.UserClient` path in new code.

- [ ] **Step 7: Run focused tests and full IT**

Run the standard command, then:

```bash
bash ./mvnw -B -ntp \
  -f egon-cola-archetypes/egon-cola-archetype-web/target/test-classes/projects/basic/project/student-management-organization/pom.xml \
  -pl student-management-organization-starter -am \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest=UserDomainServiceTest,UserManageImplTest,UserRepositoryImplTest,UserControllerTest test
```

Expected: PASS; no Redis, RabbitMQ, Nacos, PostgreSQL, or Dubbo registry connection occurs.

- [ ] **Step 8: Commit the user vertical**

```bash
git add egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources \
  egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/META-INF/maven/archetype-metadata.xml
git commit -m "feat(archetype): add organization user vertical"
```

### Task 4: Complete Role And Permission Behavior

**Files:**
- Create/modify Facade: `RoleFacade`, `PermissionFacade`, `AssignRoleDTO`, `GrantPermissionDTO`, `PermissionTreeDTO`
- Create/modify Domain: `Role`, `Permission`, `UserAggregate`, `RolePermissionAggregate`, `RoleCode`, `PermissionCode`, `PermissionDomainService`, `PermissionDomainServiceImpl`, user repositories, validators, enums
- Create: `domain/validators/OrganizationCodeValidator.java`
- Create/modify Application: role/permission Commands, Query, Result, Manage interfaces/implementations, converters, validators, assemblers, tests
- Create/modify Infrastructure: role/permission/relation PO, JPA repositories, converters, repository implementations, tests
- Create/modify Adapter: `RoleController`, `PermissionController`, requests, `PermissionTreeVO`, converters, Facade implementations, tests

- [ ] **Step 1: Write failing aggregate and use-case tests**

Create `RolePermissionAggregateTest`, `RoleManageImplTest`, and `PermissionManageImplTest` with these assertions:

```java
UserAggregate user = new UserAggregate(activeUser("u-1"));
user.assignRole(activeRole("STUDENT"));
assertThat(user.user().roleCodes()).containsExactly(new RoleCode("STUDENT"));
assertThatThrownBy(() -> user.assignRole(activeRole("STUDENT")))
        .isInstanceOf(OrganizationDomainException.class)
        .extracting(e -> ((OrganizationDomainException) e).code())
        .isEqualTo(OrganizationDomainErrorCode.DUPLICATE_ROLE_ASSIGNMENT);

RolePermissionAggregate role = new RolePermissionAggregate(activeRole("STUDENT"), List.of());
role.grant(activePermission("CLASS_READ"));
assertThat(role.permissionCodes()).containsExactly(new PermissionCode("CLASS_READ"));

OrganizationRequestContextHolder.set(new OrganizationRequestContext("admin-1", Set.of("ORGANIZATION_ADMIN"), "trace-1"));
roleManage.assignRole(new AssignRoleCommand("req-role", "u-1", "student"));
verify(userRepository).save(argThat(saved -> saved.roleCodes().contains(new RoleCode("STUDENT"))));
```

Define these fixture methods in the same test class:

```java
private static User activeUser(String id) {
    return new User(new UserId(id), "Mario", "mario@example.com", UserStatus.ACTIVE, List.of());
}

private static Role activeRole(String code) {
    return new Role("role-" + code.toLowerCase(Locale.ROOT), new RoleCode(code), code, RoleStatus.ACTIVE);
}

private static Permission activePermission(String code) {
    return new Permission("permission-" + code.toLowerCase(Locale.ROOT),
            new PermissionCode(code), code, PermissionType.API, PermissionStatus.ACTIVE);
}
```

Add controller assertions for `204` on assign/grant and `200` for permission-tree lookup.

- [ ] **Step 2: Run the standard command and verify failure**

Expected: FAIL because role/permission aggregates, repositories, Application services, and inbound contracts are missing.

- [ ] **Step 3: Implement role and permission invariants**

Use these final enum values and code normalization:

```java
public enum RoleStatus { ACTIVE, ARCHIVED }
public enum PermissionType { API, MENU, ACTION }
public enum PermissionStatus { ACTIVE, INACTIVE }

public record RoleCode(String value) {
    public RoleCode { value = OrganizationCodeValidator.normalize(value); }
}

public record PermissionCode(String value) {
    public PermissionCode { value = OrganizationCodeValidator.normalize(value); }
}
```

Create the Domain-only validator:

```java
public final class OrganizationCodeValidator {
    private static final Pattern CODE = Pattern.compile("[A-Z][A-Z0-9_]{1,63}");

    private OrganizationCodeValidator() {}

    public static String normalize(String raw) {
        String normalized = raw == null ? "" : raw.trim().toUpperCase(Locale.ROOT);
        if (!CODE.matcher(normalized).matches()) {
            throw new OrganizationDomainException(
                    OrganizationDomainErrorCode.INVALID_CODE,
                    "code must match [A-Z][A-Z0-9_]{1,63}");
        }
        return normalized;
    }
}
```

`UserAggregate.assignRole` rejects disabled user, archived role, and duplicate role. `RolePermissionAggregate.grant` rejects archived role, inactive permission, and duplicate permission.

Repository APIs are:

```java
public interface RoleRepository {
    Optional<Role> findByCode(RoleCode code);
    Role save(Role role);
}

public interface PermissionRepository {
    Optional<Permission> findByCode(PermissionCode code);
    List<Permission> findByUserId(UserId userId);
    Permission save(Permission permission);
}
```

- [ ] **Step 4: Implement Application role/permission APIs**

Use:

```java
public record AssignRoleCommand(String requestId, String userId, String roleCode) {}
public record GrantPermissionCommand(String requestId, String roleCode, String permissionCode) {}
public record PermissionTreeQuery(String userId) {}
public record PermissionTreeResult(String userId, List<String> permissionCodes) {}

public interface RoleManage {
    void assignRole(AssignRoleCommand command);
}

public interface PermissionManage {
    void grantPermission(GrantPermissionCommand command);
    PermissionTreeResult getPermissionTree(PermissionTreeQuery query);
}
```

Both mutations require `ORGANIZATION_ADMIN`, execute in one transaction, and save only through Domain repositories. Task 7 wraps them with idempotency operations `assign-role` and `grant-permission` and rollback release.

- [ ] **Step 5: Implement JPA relation persistence**

Create exact uppercase-PO types `RolePO`, `PermissionPO`, `UserRolePO`, and `RolePermissionPO`. Create `RoleJpaRepository`, `PermissionJpaRepository`, `UserRoleJpaRepository`, and `RolePermissionJpaRepository`. The Domain repository implementations load relation rows when restoring aggregates and insert only missing relation rows; database unique-key violations translate to `OrganizationPortException(CONFLICT)`.

- [ ] **Step 6: Implement HTTP and Facade operations**

Use the approved signatures:

```java
public interface RoleFacade { void assignRole(@Valid @NotNull AssignRoleDTO request); }

public interface PermissionFacade {
    void grantPermission(@Valid @NotNull GrantPermissionDTO request);
    PermissionTreeDTO getPermissionTree(@NotBlank String userId);
}

@PostMapping("/api/v1/users/{userId}/roles")
public ResponseEntity<Void> assign(@PathVariable String userId,
        @Valid @RequestBody AssignRoleRequest request,
        @RequestHeader(value = "Idempotency-Key", required = false) String key) {
    String requestId = key == null ? UUID.randomUUID().toString() : key;
    roleManage.assignRole(converter.toCommand(requestId, userId, request));
    return ResponseEntity.noContent().build();
}
```

Implement the matching permission POST and permission-tree GET paths from the spec. Return direct Adapter VOs, never Common or Facade response wrappers.

- [ ] **Step 7: Run focused tests and full IT**

Run the standard command and the generated tests:

```bash
bash ./mvnw -B -ntp \
  -f egon-cola-archetypes/egon-cola-archetype-web/target/test-classes/projects/basic/project/student-management-organization/pom.xml \
  -pl student-management-organization-starter -am \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest=RolePermissionAggregateTest,RoleManageImplTest,PermissionManageImplTest test
```

Expected: PASS, including normalization, authorization, duplicate assignment, archived/inactive state, and permission-tree cases.

- [ ] **Step 8: Commit role and permission behavior**

```bash
git add egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources
git commit -m "feat(archetype): complete user role permission flow"
```

### Task 5: Implement Grade And School-Class Verticals

**Files:**
- Create/modify Facade teaching interfaces and all teaching DTOs
- Create/modify Domain teaching entities, aggregate, value objects, service, repositories, validators, enums, tests
- Create/modify Application teaching Commands, Queries, Results, Manage services, converters, validators, assemblers, tests
- Create/modify Infrastructure Grade/SchoolClass PO, JPA, converters, repositories, tests
- Create/modify Adapter Grade/SchoolClass HTTP, Facade, request/VO/converter/validator, tests

- [ ] **Step 1: Write failing Grade and SchoolClass domain tests**

Create `GradeDomainServiceTest` and extend `SchoolClassDomainServiceTest`:

```java
Grade grade = gradeService.create("grade-" + UUID.randomUUID(), "grade_one", " Grade One ");
assertThat(grade.code()).isEqualTo(GradeCode.create("GRADE_ONE"));
assertThat(grade.status()).isEqualTo(GradeStatus.ACTIVE);

SchoolClass schoolClass = schoolClassService.create(
        new SchoolClassId("class-1"), " Class A ", grade);
assertThat(schoolClass.gradeId()).isEqualTo(grade.id());
assertThat(schoolClass.gradeName()).isEqualTo("Grade One");

assertThatThrownBy(() -> schoolClassService.create(
        new SchoolClassId("class-2"), "Class A", archivedGrade()))
        .isInstanceOf(OrganizationDomainException.class);

private static Grade archivedGrade() {
    return new Grade(
            "grade-archived",
            GradeCode.create("ARCHIVED_GRADE"),
            "Archived Grade",
            GradeStatus.ARCHIVED);
}
```

Add Application tests proving duplicate Grade code and case-insensitive class name inside one Grade are rejected.

- [ ] **Step 2: Run the standard command and verify failure**

Expected: FAIL because the Grade aggregate path and the approved SchoolClass/Grade contracts do not exist.

- [ ] **Step 3: Implement teaching Domain types**

Use:

```java
public record GradeCode(String value) {
    public GradeCode {
        if (value == null || value.isBlank() || value.length() > 120) {
            throw new OrganizationDomainException(
                    OrganizationDomainErrorCode.INVALID_CODE,
                    "grade code must not be blank or exceed 120 characters");
        }
    }

    public static GradeCode create(String raw) {
        return new GradeCode(OrganizationCodeValidator.normalize(raw));
    }

    public static GradeCode restoreLegacy(String raw) {
        return new GradeCode(raw);
    }
}

public record Grade(String id, GradeCode code, String name, GradeStatus status) {}

public record SchoolClass(
        SchoolClassId id,
        String name,
        String gradeId,
        GradeCode gradeCode,
        String gradeName,
        SchoolClassStatus status,
        List<UserId> userIds) {}
```

Implement `GradeRepository.findById`, `findByCode`, `existsByCode`, `save`; implement `SchoolClassRepository.findById`, `existsByGradeIdAndNameIgnoreCase`, `save`, and `addUser`. Reserve `legacy:` Grade IDs; new IDs use `grade-` plus lowercase UUID.

- [ ] **Step 4: Implement teaching Application APIs**

Use:

```java
public record CreateGradeCommand(String requestId, String code, String name) {}
public record GradeDetailQuery(String gradeId) {}
public record GradeDetailResult(String id, String code, String name, String status) {}
public record CreateSchoolClassCommand(String requestId, String name, String gradeCode) {}
public record SchoolClassDetailQuery(String schoolClassId) {}
public record SchoolClassDetailResult(
        String id, String name, String gradeCode, String gradeName, String status, List<String> userIds) {}

public interface GradeManage {
    GradeDetailResult createGrade(CreateGradeCommand command);
    GradeDetailResult getGrade(GradeDetailQuery query);
}

public interface SchoolClassManage {
    SchoolClassDetailResult createSchoolClass(CreateSchoolClassCommand command);
    SchoolClassDetailResult getSchoolClass(SchoolClassDetailQuery query);
}
```

Teaching mutations require `TEACHING_ADMIN`; queries require no role. Keep these mutations direct in this commit; Task 7 wraps them with the `create-grade` and `create-school-class` idempotency operations after the idempotency port exists.

- [ ] **Step 5: Implement teaching JPA mappings with legacy restore semantics**

`GradePO` maps `grades`. `SchoolClassPO` keeps both `grade_id` and `grade_name`, plus status. `GradePOConverter` uses `GradeCode.restoreLegacy` when ID starts with `legacy:` and `GradeCode.create` otherwise. `SchoolClassRepositoryImpl` enforces new-write uniqueness through `existsByGradeIdAndNameIgnoreCase` without adding a V2 unique constraint.

- [ ] **Step 6: Replace the teaching Facade and HTTP contracts**

Use the exact five methods and DTO fields from the spec:

```java
public interface GradeFacade {
    GradeDetailDTO createGrade(@Valid @NotNull CreateGradeDTO request);
    GradeDetailDTO getGrade(@NotBlank String gradeId);
}

public interface SchoolClassFacade {
    SchoolClassDetailDTO createSchoolClass(@Valid @NotNull CreateSchoolClassDTO request);
    SchoolClassDetailDTO getSchoolClass(@NotBlank String schoolClassId);
    void assignUser(@Valid @NotNull AssignUserToClassDTO request);
}
```

Keep the existing working assignment implementation during this commit; only Grade and SchoolClass create/get paths move to the new teaching models. Task 6 replaces assignment atomically. Do not add a stub or throw `UnsupportedOperationException`; the generated public Facade method must continue to work throughout the cutover.

Create `POST/GET /api/v1/grades` and `POST/GET /api/v1/school-classes` with `201/200` and direct `GradeDetailVO`/`SchoolClassDetailVO` responses.

- [ ] **Step 7: Run focused tests and full IT**

Run the standard command, then run `GradeDomainServiceTest`, `SchoolClassDomainServiceTest`, `GradeManageImplTest`, `SchoolClassManageImplTest`, `GradeRepositoryImplTest`, and `SchoolClassRepositoryImplTest` in the generated reactor.

Expected: PASS; V1 legacy classes restore through the generated Grade, while new writes require active Grade and case-insensitive class-name uniqueness.

- [ ] **Step 8: Commit teaching core**

```bash
git add egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources
git commit -m "feat(archetype): add grade and school class verticals"
```

### Task 6: Implement Cross-Domain User Membership

**Files:**
- Modify: Domain `SchoolClassAggregate`, `SchoolClassRepository`, user and teaching validators
- Create: Application `AssignUserToClassCommand` and test
- Modify: `SchoolClassManage` and `SchoolClassManageImpl`
- Create/modify: Infrastructure `SchoolClassUserPO`, `SchoolClassUserJpaRepository`, `SchoolClassRepositoryImpl`
- Modify: Adapter assignment request, controller, converter, Facade implementation, tests
- Delete after cutover: legacy Domain `UserClient`/`SchoolClassClient` and Infrastructure client implementations

- [ ] **Step 1: Write the failing cross-domain Application test**

```java
@Test
void assignsActiveUserToActiveSchoolClassInOneTransaction() {
    when(userRepository.findById(new UserId("u-1"))).thenReturn(Optional.of(activeUser("u-1")));
    when(schoolClassRepository.findById(new SchoolClassId("c-1"))).thenReturn(Optional.of(activeClass("c-1")));

    schoolClassManage.assignUser(new AssignUserToClassCommand("req-1", "u-1", "c-1"));

    verify(schoolClassRepository).addUser(new SchoolClassId("c-1"), new UserId("u-1"));
}

@Test
void rejectsDuplicateMembershipWithoutPublishing() {
    when(schoolClassRepository.hasUser(new SchoolClassId("c-1"), new UserId("u-1"))).thenReturn(true);
    assertThatThrownBy(() -> schoolClassManage.assignUser(new AssignUserToClassCommand("req-2", "u-1", "c-1")))
            .isInstanceOf(OrganizationApplicationException.class);
    verify(schoolClassRepository, never()).addUser(any(), any());
}

private static User activeUser(String id) {
    return new User(new UserId(id), "Mario", "mario@example.com", UserStatus.ACTIVE);
}

private static SchoolClass activeClass(String id) {
    return new SchoolClass(
            new SchoolClassId(id),
            "Class A",
            "grade-1",
            GradeCode.create("GRADE_ONE"),
            "Grade One",
            SchoolClassStatus.ACTIVE,
            List.of());
}
```

- [ ] **Step 2: Run the standard command and verify failure**

Expected: FAIL because the final Command, authoritative membership repository methods, and event are absent.

- [ ] **Step 3: Implement the authoritative membership transaction**

Add to `SchoolClassManage`:

```java
void assignUser(AssignUserToClassCommand command);
```

The implementation requires `TEACHING_ADMIN`, loads User and SchoolClass through Domain repositories, rejects disabled/archived/duplicate state, and calls `SchoolClassRepository.addUser`. It does not save a second membership copy in User. Task 7 adds the `assign-user-to-school-class` idempotency claim; Task 8 adds its after-commit `SchoolClassMembershipChangedEvent` after those shared ports exist.

- [ ] **Step 4: Implement membership JPA persistence**

Map `SchoolClassUserPO` to the existing `school_class_users` table and preserve `uk_school_class_user`. Add `existsBySchoolClassIdAndUserId` to `SchoolClassUserJpaRepository`; translate its unique-key failure into Domain `CONFLICT`. Do not add foreign keys or delete orphan rows.

- [ ] **Step 5: Cut over the HTTP and Facade assignment paths**

Use:

```java
@PostMapping("/api/v1/school-classes/{schoolClassId}/users")
public ResponseEntity<Void> assignUser(@PathVariable String schoolClassId,
        @Valid @RequestBody AssignUserToClassRequest request,
        @RequestHeader(value = "Idempotency-Key", required = false) String key) {
    String requestId = key == null ? UUID.randomUUID().toString() : key;
    schoolClassManage.assignUser(converter.toCommand(requestId, schoolClassId, request.userId()));
    return ResponseEntity.noContent().build();
}
```

`SchoolClassFacadeImpl.assignUser` maps `AssignUserToClassDTO` to the same Command.

- [ ] **Step 6: Remove the obsolete local-client pattern**

Delete these exact paths after verifying no imports remain:

```text
domain/client/user/UserClient.java
domain/client/teaching/SchoolClassClient.java
infrastructure/client/impl/user/UserClientImpl.java
infrastructure/client/impl/teaching/SchoolClassClientImpl.java
```

Run:

```bash
rg -n "UserClient|SchoolClassClient" egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources
```

Expected: no matches.

- [ ] **Step 7: Run focused tests and full IT**

Run the standard command and `AssignUserToClassUseCaseTest`, `SchoolClassRepositoryImplTest`, `SchoolClassControllerTest`.

Expected: PASS; one membership row exists, duplicate assignment is a conflict, and no obsolete client path is generated.

- [ ] **Step 8: Commit membership**

```bash
git add -A egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources
git commit -m "feat(archetype): add cross-domain class membership"
```

### Task 7: Add Cache-Aside Queries And Command Idempotency

**Files:**
- Create/modify Domain: `UserCachePort`, `GradeCachePort`, `SchoolClassCachePort`, `CommandIdempotencyPort`
- Create Infrastructure: Redis and in-memory cache/idempotency adapters, `OrganizationCacheKey`, Redis config, local fallback config, integration properties
- Modify Application Manage implementations for cache-aside lookup, post-commit eviction, claim/release
- Create: `OrganizationCacheTest`, `OrganizationInfrastructureProfileTest`

- [ ] **Step 1: Write failing cache and idempotency tests**

```java
@Test
void usesNamespacedKeysAndConfiguredTtl() {
    RedisUserCache cache = new RedisUserCache(redisTemplate, properties, converter);
    cache.put(new User(new UserId("u-1"), "Mario", "mario@example.com", UserStatus.ACTIVE));
    verify(redisTemplate.opsForValue()).set(
            eq("student-management-organization:user:u-1"),
            any(),
            eq(Duration.ofMinutes(10)));
}

@Test
void claimsCommandAtomicallyAndReleasesAfterRollback() {
    when(valueOperations.setIfAbsent(
            "student-management-organization:command:create-user:req-1",
            "1", Duration.ofHours(24))).thenReturn(true);
    assertThat(adapter.claim("create-user", "req-1")).isTrue();
    adapter.release("create-user", "req-1");
    verify(redisTemplate).delete("student-management-organization:command:create-user:req-1");
}

@Test
void localProfileCreatesOnlyInMemoryPorts() {
    contextRunner.withPropertyValues(
            "organization.integrations.redis.enabled=false",
            "organization.integrations.rabbit.enabled=false")
        .run(context -> {
            assertThat(context).hasSingleBean(InMemoryUserCache.class);
            assertThat(context).hasSingleBean(InMemoryCommandIdempotencyAdapter.class);
            assertThat(context).doesNotHaveBean(RedisConnectionFactory.class);
        });
}
```

- [ ] **Step 2: Run the standard command and verify failure**

Expected: FAIL because the cache key, TTL, serializers, and conditional adapters are missing.

- [ ] **Step 3: Implement Domain ports without technical types**

Use:

```java
public interface UserCachePort {
    Optional<User> findById(UserId id);
    void put(User user);
    void evict(UserId id);
}

public interface CommandIdempotencyPort {
    boolean claim(String operation, String requestId);
    void release(String operation, String requestId);
}
```

Create equivalent Grade and SchoolClass cache ports using their Domain IDs/models. Do not expose `Duration`, Redis serializers, or Spring classes in Domain.

- [ ] **Step 4: Implement keys, properties, Redis adapters, and local fallbacks**

Use these exact defaults in `OrganizationIntegrationProperties`:

```java
private Duration userTtl = Duration.ofMinutes(10);
private Duration gradeTtl = Duration.ofMinutes(30);
private Duration schoolClassTtl = Duration.ofMinutes(10);
private Duration commandIdempotencyTtl = Duration.ofHours(24);
```

`OrganizationCacheKey` exposes pure static builders for:

```text
student-management-organization:user:{id}
student-management-organization:grade:{id}
student-management-organization:school-class:{id}
student-management-organization:command:{operation}:{requestId}
```

Use `StringRedisSerializer` for keys and a Jackson JSON serializer configured for explicit cache-value records. Redis beans require `organization.integrations.redis.enabled=true`. `OrganizationLocalFallbackConfig` activates the in-memory implementations when the flag is false.

Keep the Redis wire model private to each adapter, for example:

```java
private record UserCacheValue(String id, String name, String email, String status) {}
```

- [ ] **Step 5: Add cache-aside and after-commit behavior to Application**

Each lookup checks its cache port, loads the repository on a miss, stores the restored Domain model, and returns a Result. Each successful mutation registers eviction through Spring transaction synchronization:

```java
TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
    @Override
    public void afterCommit() {
        userCache.evict(user.id());
    }
});
```

If a mutation throws, release the idempotency claim and do not evict. Duplicate non-MQ requests throw `new OrganizationApplicationException(OrganizationFailureType.CONFLICT, "ORG_CONFLICT", "Duplicate command request")`.

- [ ] **Step 6: Run focused tests and full IT**

Run the standard command and generated `OrganizationCacheTest`, `OrganizationInfrastructureProfileTest`, plus Application tests for cache hit, miss, duplicate claim, and rollback release.

Expected: PASS; test/local creates no external Redis connection factory.

- [ ] **Step 7: Commit cache and idempotency**

```bash
git add egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources
git commit -m "feat(archetype): add cache and idempotency ports"
```

### Task 8: Add After-Commit Events, RabbitMQ Topology, And Infrastructure AOP

**Files:**
- Create Domain `OrganizationEventPublisher`, event interface, and six approved event records
- Create Infrastructure `OrganizationEventMessage`, `OrganizationEventProducer`, Rabbit/local publisher, Rabbit config, validator, log aspect
- Create tests: `OrganizationRabbitMqContractTest` and AOP test
- Modify Application mutation flows to register after-commit publication

- [ ] **Step 1: Write failing event and topology tests**

```java
@Test
void declaresDistinctCommandEventAndDeadLetterTopology() {
    assertThat(config.commandExchange().getName()).isEqualTo("student.organization.command.v1");
    assertThat(config.eventExchange().getName()).isEqualTo("student.organization.event.v1");
    assertThat(config.deadLetterExchange().getName()).isEqualTo("student.organization.dlx.v1");
    assertThat(config.createUserQueue().getName()).isEqualTo("student.organization.user.create.v1");
    assertThat(config.createSchoolClassQueue().getName()).isEqualTo("student.organization.school-class.create.v1");
}

@Test
void publishesMappedEventWithConfirmRetryPolicy() {
    Instant occurredAt = Instant.parse("2026-07-11T00:00:00Z");
    publisher.publish(new RoleAssignedEvent("e-1", "u-1", occurredAt, "STUDENT"));
    verify(producer).send(
            "student.organization.event.v1",
            "organization.event.user.role-assigned.v1",
            new OrganizationEventMessage(
                    "e-1", "ROLE_ASSIGNED", "u-1", occurredAt, Map.of("roleCode", "STUDENT")));
}

@Test
void aspectRethrowsOriginalFailure() {
    IllegalStateException failure = new IllegalStateException("boom");
    when(joinPoint.proceed()).thenThrow(failure);
    assertThatThrownBy(() -> aspect.log(joinPoint)).isSameAs(failure);
}
```

- [ ] **Step 2: Run the standard command and verify failure**

Expected: FAIL because the topology, event publisher, message envelope, and aspect are absent.

- [ ] **Step 3: Implement Domain events**

Use:

```java
public interface OrganizationDomainEvent {
    String eventId();
    String aggregateId();
    Instant occurredAt();
}

public interface OrganizationEventPublisher {
    void publish(OrganizationDomainEvent event);
}

public record RoleAssignedEvent(
        String eventId, String aggregateId, Instant occurredAt, String roleCode)
        implements OrganizationDomainEvent {}
```

Create the same explicit base fields plus approved extra fields for `UserChangedEvent`, `PermissionGrantedEvent`, `GradeChangedEvent`, `SchoolClassChangedEvent`, and `SchoolClassMembershipChangedEvent`.

- [ ] **Step 4: Implement outbound publication**

`RabbitOrganizationEventPublisher` maps every Domain event to `OrganizationEventMessage(eventId,eventType,aggregateId,occurredAt,payload)`. `OrganizationEventProducer` uses `RabbitTemplate` publisher confirms, three total attempts, one-second initial backoff, multiplier two, and five-second maximum. `LocalOrganizationEventPublisher` stores immutable published-event snapshots for deterministic tests.

Use the exact routing keys from the approved spec. No event routing key may match either command consumer key.

- [ ] **Step 5: Publish only after commit**

Register `OrganizationEventPublisher.publish` in `afterCommit`. Exhausted post-commit Rabbit failures log and increment a Micrometer counter; they do not change an already committed use-case response into `503`. Do not add an outbox table.

- [ ] **Step 6: Implement the AOP contract**

Use:

```java
@Around("execution(public * ${package}.application.manage..*(..))")
public Object log(ProceedingJoinPoint point) throws Throwable {
    long started = System.nanoTime();
    try {
        return point.proceed();
    } finally {
        long elapsedNanos = System.nanoTime() - started;
        log.info("traceId={} method={} elapsedNanos={}", traceId(), point.getSignature().toShortString(), elapsedNanos);
    }
}
```

Never log arguments, emails, DTO payloads, or stack traces at info level.

- [ ] **Step 7: Run focused tests and full IT**

Run the standard command and `OrganizationRabbitMqContractTest` plus the AOP test.

Expected: PASS without a broker; the test profile uses local publisher and directly instantiates topology beans.

- [ ] **Step 8: Commit outbound integration**

```bash
git add egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources
git commit -m "feat(archetype): add organization events and aop"
```

### Task 9: Finalize HTTP Errors, Validation, Trace, And Request Context

**Files:**
- Create/modify Adapter validators for user and teaching
- Create `OrganizationErrorResponse`, `OrganizationGlobalExceptionHandler`, trace/auth filters
- Create/modify Application request context and failure types
- Create tests: `OrganizationHttpErrorContractTest`, `OrganizationFilterTest`
- Delete legacy Common response wrappers and Adapter validation/handler paths after all callers are cut over

- [ ] **Step 1: Write parameterized HTTP error tests**

```java
@ParameterizedTest
@MethodSource("failures")
void mapsApplicationFailures(OrganizationFailureType type, HttpStatus status, String code) throws Exception {
    when(userManage.getUser(any())).thenThrow(new OrganizationApplicationException(type, code, "failure"));
    mockMvc.perform(get("/api/v1/users/u-1").header("X-Trace-Id", "trace-1"))
            .andExpect(status().is(status.value()))
            .andExpect(jsonPath("$.code").value(code))
            .andExpect(jsonPath("$.traceId").value("trace-1"))
            .andExpect(jsonPath("$.timestamp").exists())
            .andExpect(jsonPath("$.fieldErrors").isMap());
}

static Stream<Arguments> failures() {
    return Stream.of(
        Arguments.of(OrganizationFailureType.VALIDATION, HttpStatus.BAD_REQUEST, "ORG_VALIDATION_ERROR"),
        Arguments.of(OrganizationFailureType.FORBIDDEN, HttpStatus.FORBIDDEN, "ORG_FORBIDDEN"),
        Arguments.of(OrganizationFailureType.NOT_FOUND, HttpStatus.NOT_FOUND, "ORG_NOT_FOUND"),
        Arguments.of(OrganizationFailureType.CONFLICT, HttpStatus.CONFLICT, "ORG_CONFLICT"),
        Arguments.of(OrganizationFailureType.DOMAIN_REJECTED, HttpStatus.UNPROCESSABLE_ENTITY, "ORG_DOMAIN_REJECTED"),
        Arguments.of(OrganizationFailureType.DEPENDENCY_UNAVAILABLE, HttpStatus.SERVICE_UNAVAILABLE, "ORG_DEPENDENCY_UNAVAILABLE"),
        Arguments.of(OrganizationFailureType.INTERNAL, HttpStatus.INTERNAL_SERVER_ERROR, "ORG_INTERNAL_ERROR"));
}
```

Add filter tests proving generated/propagated `X-Trace-Id`, actor roles, and `finally` cleanup after both success and exception.

- [ ] **Step 2: Run the standard command and verify failure**

Expected: FAIL because the uniform error response, 403 mapping, and filter cleanup are incomplete.

- [ ] **Step 3: Implement Application request context and errors**

Use:

```java
public record OrganizationRequestContext(String actorId, Set<String> actorRoles, String traceId) {
    public boolean hasRole(String role) { return actorRoles.contains(role) || actorRoles.contains("SYSTEM"); }
}

public enum OrganizationFailureType {
    VALIDATION, FORBIDDEN, NOT_FOUND, CONFLICT, DOMAIN_REJECTED, DEPENDENCY_UNAVAILABLE, INTERNAL
}
```

`OrganizationRequestContextHolder` uses one ThreadLocal, rejects nested replacement, and always exposes `clear()`.

```java
public final class OrganizationRequestContextHolder {
    private static final ThreadLocal<OrganizationRequestContext> CURRENT = new ThreadLocal<>();

    private OrganizationRequestContextHolder() {}

    public static Optional<OrganizationRequestContext> current() {
        return Optional.ofNullable(CURRENT.get());
    }

    public static void set(OrganizationRequestContext context) {
        if (CURRENT.get() != null) {
            throw new IllegalStateException("Organization request context is already set");
        }
        CURRENT.set(Objects.requireNonNull(context));
    }

    public static void clear() {
        CURRENT.remove();
    }
}
```

- [ ] **Step 4: Implement the HTTP error body and status map**

Use:

```java
public record OrganizationErrorResponse(
        String code,
        String message,
        String traceId,
        Instant timestamp,
        Map<String, List<String>> fieldErrors) {}
```

Map the seven stable codes/statuses exactly. Bean-validation field errors preserve field name -> list of messages. Unexpected errors use `ORG_INTERNAL_ERROR`, omit stack traces, and keep details only in error-level logs.

- [ ] **Step 5: Implement filter ordering and cleanup**

`OrganizationTraceFilter` uses highest precedence, accepts/generates `X-Trace-Id`, and returns it in the response. `OrganizationAuthContextFilter` runs next, reads `X-Actor-Id` and comma-separated `X-Actor-Roles`, then clears context in `finally`.

- [ ] **Step 6: Remove legacy response and typo paths**

Delete:

```text
common/response/Response.java
common/response/SingleResponse.java
adapter/validation/**
adapter/handler/GlobalExceptionHandler.java
```

Move every `adapter/convertor` file to `adapter/converter`, rename classes/imports to `Converter`, and run:

```bash
rg -n "convertor|SingleResponse|common.response|GlobalExceptionHandler" \
  egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources
```

Expected: no matches.

- [ ] **Step 7: Run HTTP/filter tests and full IT**

Expected: PASS for every controller, status, error-body field, trace propagation, authorization, and cleanup path.

- [ ] **Step 8: Commit the HTTP boundary**

```bash
git add -A egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources
git commit -m "feat(archetype): standardize organization http boundary"
```

### Task 10: Add GraphQL Queries, Mutations, And Error Extensions

**Files:**
- Create: `adapter/src/main/resources/graphql/schema.graphqls`
- Create: `adapter/graphql/UserResolver.java`, `SchoolClassResolver.java`, package docs
- Create: `adapter/handler/OrganizationGraphQlExceptionResolver.java`
- Create: `adapter/OrganizationGraphQlContractTest.java`

- [ ] **Step 1: Write the failing GraphQL contract test**

```java
@Test
void exposesBothDomainQueriesAndMutations() {
    graphQlTester.document("mutation { createGrade(input:{code:\"GRADE_ONE\",name:\"Grade One\"}) { code name status } }")
        .header("X-Actor-Id", "admin-1")
        .header("X-Actor-Roles", "TEACHING_ADMIN")
        .execute()
        .path("createGrade.code").entity(String.class).isEqualTo("GRADE_ONE");

    graphQlTester.document("query { user(id:\"u-1\") { id email status roleCodes } }")
        .execute()
        .path("user.id").entity(String.class).isEqualTo("u-1");
}

@Test
void exposesStableErrorExtensions() {
    graphQlTester.document("query { user(id:\"missing\") { id } }")
        .execute()
        .errors().satisfy(errors -> assertThat(errors.getFirst().getExtensions())
            .containsKeys("code", "traceId", "timestamp", "fieldErrors"));
}
```

- [ ] **Step 2: Run the standard command and verify failure**

Expected: FAIL because no GraphQL schema or resolver is generated.

- [ ] **Step 3: Create the exact schema**

Define the four approved Queries and six Mutations verbatim from the spec. Define `User`, `PermissionTree`, `Grade`, and `SchoolClass` object fields to match the frozen DTO fields; define the six input types with the frozen command fields excluding generated request IDs.

The schema root must be:

```graphql
type Query {
  user(id: ID!): User!
  permissionTree(userId: ID!): PermissionTree!
  grade(id: ID!): Grade!
  schoolClass(id: ID!): SchoolClass!
}

type Mutation {
  createUser(input: CreateUserInput!): User!
  assignRole(input: AssignRoleInput!): Boolean!
  grantPermission(input: GrantPermissionInput!): Boolean!
  createGrade(input: CreateGradeInput!): Grade!
  createSchoolClass(input: CreateSchoolClassInput!): SchoolClass!
  assignUserToSchoolClass(input: AssignUserToSchoolClassInput!): Boolean!
}
```

- [ ] **Step 4: Implement resolvers through Application only**

`UserResolver` owns user, role, permission operations. `SchoolClassResolver` owns Grade and SchoolClass operations. Each mutation resolves/generates `Idempotency-Key`, creates the same Application Command as HTTP, and returns Application Result-derived GraphQL objects. Neither resolver imports Domain, Infrastructure, Facade DTOs, or JPA.

- [ ] **Step 5: Implement GraphQL error extensions**

Map `OrganizationApplicationException` to the same stable `ORG_*` code, trace ID, timestamp, and field-error map. Unexpected errors become `ORG_INTERNAL_ERROR` without stack-trace disclosure.

- [ ] **Step 6: Run GraphQL tests and full IT**

Expected: PASS in-process through `WebGraphQlTester`; no real server starts.

- [ ] **Step 7: Commit GraphQL**

```bash
git add egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-adapter
git commit -m "feat(archetype): add organization graphql contract"
```

### Task 11: Implement Facade Providers And Dubbo Export Configuration

**Files:**
- Create/modify all five `adapter/facade/impl/{user,teaching}/*FacadeImpl.java`
- Create: `adapter/rpc/UserRpcProvider.java`, `SchoolClassRpcProvider.java`
- Create: `adapter/OrganizationDubboProviderConfigurationTest.java`
- Modify: profile configuration for injvm test/local and Dubbo dev/prod
- Modify: Facade contract test

- [ ] **Step 1: Write the failing provider configuration test**

```java
@SpringBootTest(properties = {
    "dubbo.protocol.name=injvm",
    "dubbo.protocol.port=-1",
    "dubbo.registry.address=N/A"
})
class OrganizationDubboProviderConfigurationTest {
    @Autowired Map<String, ServiceBean<?>> services;
    @Autowired UserFacade userFacade;
    @Autowired RoleFacade roleFacade;
    @Autowired PermissionFacade permissionFacade;
    @Autowired GradeFacade gradeFacade;
    @Autowired SchoolClassFacade schoolClassFacade;

    @Test
    void exportsFiveFacadeBeansWithoutDuplicateImplementations() {
        assertThat(services).hasSize(5);
        assertThat(services.values()).allSatisfy(service -> {
            assertThat(service.getGroup()).isEqualTo("student-management-organization");
            assertThat(service.getVersion()).isEqualTo("1.0.0");
        });
        assertThat(userFacade).isInstanceOf(UserFacadeImpl.class);
        assertThat(roleFacade).isInstanceOf(RoleFacadeImpl.class);
        assertThat(permissionFacade).isInstanceOf(PermissionFacadeImpl.class);
        assertThat(gradeFacade).isInstanceOf(GradeFacadeImpl.class);
        assertThat(schoolClassFacade).isInstanceOf(SchoolClassFacadeImpl.class);
    }
}
```

- [ ] **Step 2: Run the standard command and verify failure**

Expected: FAIL because the final Facade implementations and `ServiceBean` exports are incomplete.

- [ ] **Step 3: Implement all Facade adapters**

Each `*FacadeImpl` is the sole implementation of its Facade interface, converts Facade DTOs to Application Commands/Queries, delegates once, and converts Results back. For mutations, read the `idempotency-key` Dubbo attachment or generate a UUID. Establish and clear Application request context using `x-trace-id`, `x-actor-id`, and `x-actor-roles` attachments.

Use this error translation:

```java
private OrganizationFacadeException translate(OrganizationApplicationException failure) {
    return new OrganizationFacadeException(failure.code(), failure.getMessage(), currentTraceId());
}
```

Do not annotate Facade implementations with `@DubboService`; they are ordinary Spring components.

- [ ] **Step 4: Export Facade implementations from RPC provider configuration**

In each provider class, declare typed `ServiceBean<T>` beans:

```java
private static <T> ServiceBean<T> service(Class<T> type, T implementation) {
    ServiceBean<T> bean = new ServiceBean<>();
    bean.setInterface(type);
    bean.setRef(implementation);
    bean.setGroup("student-management-organization");
    bean.setVersion("1.0.0");
    return bean;
}

@Bean
ServiceBean<UserFacade> userFacadeService(UserFacade implementation) {
    return service(UserFacade.class, implementation);
}
```

`UserRpcProvider` declares User, Role, Permission beans. `SchoolClassRpcProvider` declares Grade and SchoolClass beans. The provider classes do not implement Facade interfaces.

- [ ] **Step 5: Configure profile-specific protocol behavior**

Local/test:

```yaml
dubbo:
  registry:
    address: N/A
  protocol:
    name: injvm
    port: -1
```

Dev/prod use protocol `dubbo`, port `${DUBBO_PROTOCOL_PORT:50051}`, group/version from the provider beans, and environment-backed registry address.

- [ ] **Step 6: Run provider and Facade contract tests**

Expected: PASS; five services, two provider configuration classes, five sole Facade implementation beans, no registry, and no network listener under test.

- [ ] **Step 7: Commit Facade/Dubbo providers**

```bash
git add egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/{__rootArtifactId__-facade,__rootArtifactId__-adapter,__rootArtifactId__-starter}
git commit -m "feat(archetype): export organization dubbo facades"
```

### Task 12: Add RabbitMQ Command Consumers

**Files:**
- Create: `adapter/dto/user/CreateUserMessage.java`
- Create: `adapter/dto/teaching/CreateSchoolClassMessage.java`
- Create/modify: `adapter/mq/user/UserCreatedConsumer.java`
- Create/modify: `adapter/mq/teaching/SchoolClassChangedConsumer.java`
- Create: `adapter/mq/RetryableOrganizationMessageException.java`
- Create: `adapter/OrganizationRabbitMqConsumerTest.java`
- Modify: Infrastructure Rabbit topology/retry configuration

- [ ] **Step 1: Write failing consumer tests**

```java
@Test
void createUserMessageDelegatesToTheSharedCommand() {
    consumer.consume(new CreateUserMessage("req-1", "Mario", "mario@example.com"));
    verify(userManage).createUser(new CreateUserCommand("req-1", "Mario", "mario@example.com"));
    assertThat(OrganizationRequestContextHolder.current()).isEmpty();
}

@Test
void duplicateCommandIsAcknowledgedWithoutSecondMutation() {
    when(userManage.createUser(any())).thenThrow(new OrganizationApplicationException(CONFLICT, "ORG_CONFLICT", "duplicate"));
    assertThatCode(() -> consumer.consume(new CreateUserMessage("req-1", "Mario", "mario@example.com")))
            .doesNotThrowAnyException();
    verify(userManage, times(1)).createUser(any());
}

@Test
void dependencyFailureRemainsRetryable() {
    when(userManage.createUser(any())).thenThrow(new OrganizationApplicationException(DEPENDENCY_UNAVAILABLE, "ORG_DEPENDENCY_UNAVAILABLE", "db"));
    CreateUserMessage message = new CreateUserMessage("req-1", "Mario", "mario@example.com");
    assertThatThrownBy(() -> consumer.consume(message)).isInstanceOf(RetryableOrganizationMessageException.class);
}
```

- [ ] **Step 2: Run the standard command and verify failure**

Expected: FAIL because message DTOs and consumers are absent.

- [ ] **Step 3: Implement exact message records and listener bindings**

```java
public record CreateUserMessage(String requestId, String name, String email) {}
public record CreateSchoolClassMessage(String requestId, String name, String gradeCode) {}

@RabbitListener(
    queues = "student.organization.user.create.v1",
    autoStartup = "${organization.integrations.rabbit.enabled:false}")
public void consume(CreateUserMessage message) {
    withSystemContext(() -> userManage.createUser(
            new CreateUserCommand(message.requestId(), message.name(), message.email())));
}

private void withSystemContext(Runnable action) {
    OrganizationRequestContextHolder.set(new OrganizationRequestContext(
            "rabbit-system", Set.of("SYSTEM"), UUID.randomUUID().toString()));
    try {
        action.run();
    } finally {
        OrganizationRequestContextHolder.clear();
    }
}
```

Bind the teaching listener to `student.organization.school-class.create.v1` and delegate to `CreateSchoolClassCommand`.

- [ ] **Step 4: Implement permanent/retryable classification**

Validation, forbidden, Domain rejection, and duplicate conflict are permanent; acknowledge duplicate idempotency conflicts and reject other permanent failures without requeue. Dependency unavailable and unexpected technical exceptions throw the retryable wrapper. Configure three total attempts with 1s initial backoff, multiplier 2, 5s max, then route to each queue's `.dlq` through `student.organization.dlx.v1`.

```java
public final class RetryableOrganizationMessageException extends RuntimeException {
    public RetryableOrganizationMessageException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

- [ ] **Step 5: Prove listener conditions do not start a broker connection in test**

Extend profile tests to assert the listener containers are absent or not running when `organization.integrations.rabbit.enabled=false`, while direct consumer unit tests still execute the Java components.

- [ ] **Step 6: Run consumer/topology tests and full IT**

Expected: PASS without RabbitMQ; both consumers delegate to shared Application Commands and clear SYSTEM context.

- [ ] **Step 7: Commit Rabbit consumers**

```bash
git add egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/{__rootArtifactId__-adapter,__rootArtifactId__-infrastructure}
git commit -m "feat(archetype): add organization rabbit consumers"
```

### Task 13: Complete Starter Runtime, OpenAPI, Logging, And External-Free Profiles

**Files:**
- Create: `starter/config/OrganizationSwaggerConfig.java`, `OrganizationJacksonConfig.java`, `OrganizationActuatorConfig.java`, package docs
- Create: `starter/src/main/resources/logback-spring.xml`
- Create: `starter/src/test/resources/logback-test.xml`
- Modify: all `application*.yml` and `bootstrap*.yml`
- Retain and verify: AES-GCM classes, async/virtual-thread config, Spring factories, Docker layering
- Create: `starter/OrganizationOpenApiTest.java`
- Modify: `starter/OrganizationApplicationTest.java`

- [ ] **Step 1: Write failing Starter assembly and OpenAPI tests**

```java
@SpringBootTest(properties = "spring.profiles.active=test")
@AutoConfigureMockMvc
class OrganizationOpenApiTest {
    @Autowired MockMvc mockMvc;

    @Test
    void publishesBothDomainOperations() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.info.title").value("Student Management Organization API"))
            .andExpect(jsonPath("$.info.version").value("v1"))
            .andExpect(jsonPath("$.paths['/api/v1/users']").exists())
            .andExpect(jsonPath("$.paths['/api/v1/grades']").exists())
            .andExpect(jsonPath("$.paths['/api/v1/school-classes']").exists());
    }
}
```

Extend `OrganizationApplicationTest` to assert no `RedisConnectionFactory`, running Rabbit listener container, Nacos discovery client, or external Dubbo registry client exists in test profile.

- [ ] **Step 2: Run the standard command and verify failure**

Expected: FAIL because the OpenAPI config/logging/resources and strict external-free profile behavior are absent.

- [ ] **Step 3: Implement business-neutral Starter config**

`OrganizationSwaggerConfig` returns an OpenAPI bean with title `Student Management Organization API` and version `v1`. `OrganizationJacksonConfig` registers Java Time support and disables timestamp date serialization. `OrganizationActuatorConfig` contributes only business-neutral actuator tags.

- [ ] **Step 4: Freeze local/test integration values**

Both local and test profiles must produce these effective values:

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:organization;MODE=PostgreSQL;DB_CLOSE_DELAY=-1
  flyway:
    enabled: true
  cloud:
    nacos:
      discovery:
        enabled: false
      config:
        enabled: false
organization:
  integrations:
    redis:
      enabled: false
    rabbit:
      enabled: false
  cache:
    user-ttl: PT10M
    grade-ttl: PT30M
    school-class-ttl: PT10M
  command:
    idempotency-ttl: PT24H
dubbo:
  registry:
    address: N/A
  protocol:
    name: injvm
    port: -1
```

Exclude Redis and Rabbit auto-configuration in local/test. Dev/prod must use environment-backed datasource, Redis, RabbitMQ, Nacos, and Dubbo values when their flags are enabled.

- [ ] **Step 5: Add logging resources and retain runtime baseline**

Configure console logging with trace ID, no secrets, and no payloads. Keep AES-256-GCM, Config CLI, virtual threads, async executor, graceful shutdown, Tomcat tuning, Actuator, Prometheus, layered jar, Dockerfile, wrapper, `spring.factories`, and current config-decryption tests.

- [ ] **Step 6: Run Starter and OpenAPI tests**

Expected: PASS; `/v3/api-docs` is available in-process, both domains appear, and no external connection is created.

- [ ] **Step 7: Commit Starter runtime**

```bash
git add egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-starter \
  egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/Dockerfile
git commit -m "feat(archetype): complete organization runtime profiles"
```

### Task 14: Add Cross-Layer Architecture, No-Mock Flow, And Rollback Tests

**Files:**
- Replace/extend: `starter/ArchitectureDependencyTest.java`
- Replace/extend: `starter/OrganizationFlowTest.java`
- Create: `starter/OrganizationRollbackTest.java`
- Modify: Starter test dependencies only if the reactor classes are not imported transitively

- [ ] **Step 1: Write failing ArchUnit rules**

```java
@Test
void enforcesLayerDependenciesAndTechnicalOwnership() {
    JavaClasses classes = new ClassFileImporter().importPackages("${package}");

    noClasses().that().resideInAPackage("..domain..")
        .should().dependOnClassesThat().resideInAnyPackage(
            "org.springframework..", "jakarta.persistence..", "org.apache.dubbo..",
            "org.springframework.data.redis..", "org.springframework.amqp..")
        .check(classes);

    noClasses().that().resideInAPackage("..adapter..")
        .should().dependOnClassesThat().resideInAnyPackage(
            "..domain..", "..infrastructure..", "jakarta.persistence..", "org.springframework.data.redis..")
        .check(classes);

    classes().that().haveSimpleNameEndingWith("FacadeImpl")
        .should().resideInAnyPackage(
            "..adapter.facade.impl.user..", "..adapter.facade.impl.teaching..")
        .check(classes);
}
```

Add exact package-direction rules for all seven modules and rules forbidding Spring/JPA/Dubbo annotations in Facade, technical APIs in Application, and business classes in Starter.

- [ ] **Step 2: Write the no-mock organization flow test**

Use `@SpringBootTest`, `@AutoConfigureMockMvc`, test profile, and real H2/Flyway/local ports. Do not use `@MockBean`, `@MockitoBean`, `@SpyBean`, or `@MockitoSpyBean`.

```java
MvcResult createdUser = mockMvc.perform(post("/api/v1/users")
        .headers(adminHeaders())
        .contentType(APPLICATION_JSON)
        .content("{\"name\":\"Mario\",\"email\":\"mario@example.com\"}"))
    .andExpect(status().isCreated())
    .andReturn();
String userId = JsonPath.read(createdUser.getResponse().getContentAsString(), "$.id");

mockMvc.perform(post("/api/v1/users/{id}/roles", userId)
        .headers(adminHeaders())
        .contentType(APPLICATION_JSON)
        .content("{\"roleCode\":\"STUDENT\"}"))
    .andExpect(status().isNoContent());

mockMvc.perform(post("/api/v1/roles/STUDENT/permissions")
        .headers(adminHeaders())
        .contentType(APPLICATION_JSON)
        .content("{\"permissionCode\":\"CLASS_READ\"}"))
    .andExpect(status().isNoContent());

MvcResult createdGrade = mockMvc.perform(post("/api/v1/grades")
        .headers(adminHeaders())
        .contentType(APPLICATION_JSON)
        .content("{\"code\":\"GRADE_ONE\",\"name\":\"Grade One\"}"))
    .andExpect(status().isCreated())
    .andReturn();
String gradeId = JsonPath.read(createdGrade.getResponse().getContentAsString(), "$.id");

mockMvc.perform(get("/api/v1/grades/{id}", gradeId))
    .andExpect(status().isOk())
    .andExpect(jsonPath("$.code").value("GRADE_ONE"));

MvcResult createdClass = mockMvc.perform(post("/api/v1/school-classes")
        .headers(adminHeaders())
        .contentType(APPLICATION_JSON)
        .content("{\"name\":\"Class A\",\"gradeCode\":\"GRADE_ONE\"}"))
    .andExpect(status().isCreated())
    .andReturn();
String schoolClassId = JsonPath.read(createdClass.getResponse().getContentAsString(), "$.id");

mockMvc.perform(get("/api/v1/school-classes/{id}", schoolClassId))
    .andExpect(status().isOk())
    .andExpect(jsonPath("$.gradeCode").value("GRADE_ONE"));

mockMvc.perform(post("/api/v1/school-classes/{id}/users", schoolClassId)
        .headers(adminHeaders())
        .contentType(APPLICATION_JSON)
        .content("{\"userId\":\"" + userId + "\"}"))
    .andExpect(status().isNoContent());

mockMvc.perform(get("/api/v1/users/{id}", userId))
    .andExpect(status().isOk())
    .andExpect(jsonPath("$.roleCodes[0]").value("STUDENT"));

assertThat(jdbcTemplate.queryForObject(
        "select count(*) from user_roles where user_id = ?", Integer.class, userId)).isEqualTo(1);
assertThat(jdbcTemplate.queryForObject(
        "select count(*) from role_permissions rp join roles r on r.id = rp.role_id where r.code = ?",
        Integer.class, "STUDENT")).isEqualTo(1);
assertThat(jdbcTemplate.queryForObject(
        "select count(*) from school_class_users where user_id = ? and school_class_id = ?",
        Integer.class, userId, schoolClassId)).isEqualTo(1);

private static HttpHeaders adminHeaders() {
    HttpHeaders headers = new HttpHeaders();
    headers.set("X-Actor-Id", "admin-1");
    headers.set("X-Actor-Roles", "ORGANIZATION_ADMIN,TEACHING_ADMIN");
    headers.set("X-Trace-Id", "flow-test");
    return headers;
}
```

The complete test above is one method: no request may bypass the real filters, Application services, JPA adapters, or Flyway-created schema.

- [ ] **Step 3: Write the rollback side-effect test**

Configure deterministic local cache/event adapters with observation methods. Create a real Grade and SchoolClass, insert a disabled User through `JdbcTemplate`, clear the adapters' observation history, and call `assignUser` with request ID `rollback-1`. The Domain rejection must unwind the real transaction, release the claim, and leave the membership table unchanged without a mocked repository. Assert:

```java
assertThatThrownBy(() -> schoolClassManage.assignUser(command))
        .isInstanceOf(OrganizationApplicationException.class);
assertThat(localPublisher.events()).isEmpty();
assertThat(inMemorySchoolClassCache.evictedKeys()).isEmpty();
assertThat(inMemoryIdempotency.contains("assign-user-to-school-class", "rollback-1")).isFalse();
assertThat(jdbcTemplate.queryForObject(
        "select count(*) from school_class_users where user_id = ? and school_class_id = ?",
        Integer.class, disabledUserId, schoolClassId)).isZero();
```

- [ ] **Step 4: Run tests and verify failure**

Run the standard command.

Expected: FAIL until all boundaries, after-commit hooks, and architecture rules are correct.

- [ ] **Step 5: Make the minimum corrections exposed by the tests**

Move imports or responsibilities to the owning module rather than weakening ArchUnit. Fix transaction synchronization so publication/eviction happens only in `afterCommit`; release idempotency in rollback handling; keep the database relationship authoritative.

- [ ] **Step 6: Run the three acceptance tests and full IT**

```bash
bash ./mvnw -B -ntp \
  -f egon-cola-archetypes/egon-cola-archetype-web/target/test-classes/projects/basic/project/student-management-organization/pom.xml \
  -pl student-management-organization-starter -am \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest=ArchitectureDependencyTest,OrganizationFlowTest,OrganizationRollbackTest test
```

Expected: PASS with zero business mocks and zero external services.

- [ ] **Step 7: Commit acceptance tests**

```bash
git add egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-starter
git commit -m "test(archetype): verify organization vertical flows"
```

### Task 15: Enforce The Canonical Manifest, Tighten POMs, Update README And CI

**Files:**
- Replace/extend: `verify.groovy`
- Modify: all generated POMs to the final exact dependency matrix
- Modify: `archetype-metadata.xml`
- Create all remaining `package-info.java` and required test/resource directories
- Delete every forbidden path from the approved spec
- Replace: generated `README.md`
- Modify: `.github/workflows/ci_by_multiply_java_versions.yaml`

- [ ] **Step 1: Add the final required and forbidden manifests to `verify.groovy`**

Encode two fixed canonical lists, not lists inferred from generated output:

1. Transcribe every section 4.2 organization path from `multi-project-multi-module-architecture.md`, then append every path in the approved spec's `Canonical Organization File Manifest` blocks: added production paths, added tests, and runtime-baseline additions. Resolve `${rootArtifactId}` to `student-management-organization` and `<base>` to `it/pkg` in `verify.groovy`.
2. Transcribe every entry in the approved spec's `fixed forbidden-path and stale-token manifest`. For literal files/directories call `assertMissing`; for the three wildcard rules traverse the generated tree and fail on matching `convertor`, out-of-Adapter `facade/impl`, Infrastructure `mp`, Mapper, test-migration, or `*Po.java` paths.

After transcription, sort and de-duplicate each fixed list, assert every required path with `assertFile`/`assertDir`, and assert `requiredFiles.size() == requiredFiles.toSet().size()`. Do not replace either manifest with glob-based discovery: traversal is allowed only to detect forbidden patterns and missing package documentation.

- [ ] **Step 2: Add dynamic package-info and directory checks**

For every folder containing a Java source other than `package-info.java`, assert a sibling `package-info.java`. Scan both main and test Java. Assert all four source/resource directories in all seven modules.

Use:

```groovy
def assertPackageDocs = { String sourceRoot ->
    def root = assertDir(sourceRoot)
    def javaDirs = [] as Set
    root.traverse(type: groovy.io.FileType.FILES) { file ->
        if (file.name.endsWith(".java") && file.name != "package-info.java") javaDirs << file.parentFile
    }
    javaDirs.each { dir ->
        assert new File(dir, "package-info.java").isFile(): "Missing package-info.java in ${relativePath(dir)}"
    }
}
```

- [ ] **Step 3: Enforce final POM ownership**

Parse each module POM and assert the exact internal dependency set plus the external matrix from the spec. Remove Domain's Spring validation starter; remove Infrastructure's Spring validation starter if no direct import remains; remove every JPA/Redis/Flyway/driver dependency from Adapter; keep Springdoc API only in Adapter and UI only in Starter. Assert no MyBatis/MyBatis-Plus or evaluation Facade artifact appears in any generated POM.

- [ ] **Step 4: Delete all stale classes and names**

Delete obsolete Facade Request/DTO names, legacy client implementations, root Domain enum path, old `*Po.java` casing, test-only migrations, Mapper paths, and `OrganizationOpenApiConfig`. Run:

```bash
rg -n -i "convertor|examing|mybatis|EvaluationClient|CourseClient|UserClient|SchoolClassClient|OrganizationOpenApiConfig" \
  egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources \
  egon-cola-archetypes/egon-cola-archetype-web/src/test/resources/projects/basic
```

Expected: only negative assertion strings inside source `verify.groovy`; no generated source/resource/document match.

- [ ] **Step 5: Complete archetype metadata**

Include all Java, SQL, GraphQL, YAML, XML, properties, `META-INF/**`, `.gitkeep`, test resources, wrapper, Docker, and README paths. Preserve the escaped `__rootArtifactId__-&#97;pplication` directory workaround if required by current archetype filtering. Validate the XML with:

```bash
xmllint --noout egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/META-INF/maven/archetype-metadata.xml
```

Expected: no output, exit 0.

- [ ] **Step 6: Replace the generated README with the approved operational contract**

Document organization-only generation, seven modules and exact graph, complete user + teaching flows, HTTP/GraphQL/Dubbo/RabbitMQ/Redis/JPA/AOP/OpenAPI ownership, local/test fallbacks, dev/prod properties, 400/403/404/409/422/503/500 semantics, deferred outbound cross-Project integration, encryption CLI, test/package/Docker commands, and optional local run command. Do not claim evaluation is generated and do not mention MyBatis-Plus.

- [ ] **Step 7: Change generated-project CI from test to verify**

In `.github/workflows/ci_by_multiply_java_versions.yaml`, select `clean verify` for the web archetype while preserving existing behavior for unrelated archetypes, then package and Docker-build without starting the application:

```bash
if [ "${SHORT_NAME}" = "web" ]; then
  ./mvnw -V --no-transfer-progress clean verify
else
  ./mvnw -V --no-transfer-progress clean test
fi
./mvnw -V --no-transfer-progress -DskipTests package
docker build -t "egon-generated-${SHORT_NAME}:java-${JAVA_VERSION}" .
```

- [ ] **Step 8: Run the full archetype IT**

Run the standard command.

Expected: PASS; fixed manifest, package docs, POM ownership, README checks, V1 checksum, one V2, both domains, and all generated tests pass.

- [ ] **Step 9: Commit final conformance**

```bash
git add -A egon-cola-archetypes/egon-cola-archetype-web .github/workflows/ci_by_multiply_java_versions.yaml
git commit -m "feat(archetype): enforce organization architecture contract"
```

### Task 16: Run Final Package And Docker Verification

**Files:**
- No planned source changes; fix only a failure proven by these commands and commit that focused fix separately

- [ ] **Step 1: Run the clean archetype integration test**

```bash
bash ./mvnw -B -ntp -pl egon-cola-archetypes/egon-cola-archetype-web -am clean integration-test
```

Expected: BUILD SUCCESS with the basic archetype IT executing generated `verify`.

- [ ] **Step 2: Run generated-project verify directly**

```bash
bash ./mvnw -B -ntp \
  -f egon-cola-archetypes/egon-cola-archetype-web/target/test-classes/projects/basic/project/student-management-organization/pom.xml \
  clean verify
```

Expected: BUILD SUCCESS, zero test failures, zero external service connection attempts.

- [ ] **Step 3: Package the executable layered jar**

```bash
bash ./mvnw -B -ntp \
  -f egon-cola-archetypes/egon-cola-archetype-web/target/test-classes/projects/basic/project/student-management-organization/pom.xml \
  -DskipTests package
```

Expected: BUILD SUCCESS and a starter jar under `student-management-organization-starter/target`.

- [ ] **Step 4: Build the generated Docker image**

```bash
docker build \
  -t egon-generated-web:java-21 \
  egon-cola-archetypes/egon-cola-archetype-web/target/test-classes/projects/basic/project/student-management-organization
```

Expected: Docker build succeeds; do not run the image.

- [ ] **Step 5: Run static final checks**

```bash
xmllint --noout egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/META-INF/maven/archetype-metadata.xml
git diff --check
git status --short
```

Expected: XML valid, no whitespace errors, and only intentional implementation changes or a clean tree.

- [ ] **Step 6: Record the accepted residual risk**

Confirm the delivery note states that H2 PostgreSQL mode passed but a real PostgreSQL migration server was not run, and that transactional outbox delivery is not claimed. Do not mark either as verified.

---

## Completion Checklist

The self-review maps the approved spec to executable work as follows: canonical files and dependencies are Tasks 1 and 15; Flyway compatibility is Task 2; complete user behavior is Tasks 3-4; complete teaching behavior and authoritative membership are Tasks 5-6; Redis/idempotency, after-commit events/RabbitMQ/AOP, and inbound Rabbit commands are Tasks 7-8 and 12; HTTP, GraphQL, and Dubbo contracts are Tasks 9-11; runtime profiles/OpenAPI/logging are Task 13; architecture/no-mock/rollback acceptance is Task 14; README/CI/archetype verification and package/Docker gates are Tasks 15-16. Evaluation generation and outbound cross-Project clients are asserted absent in Tasks 6 and 15.

- [ ] One archetype invocation generates only `student-management-organization`.
- [ ] Seven modules and exact internal/external dependency ownership pass mechanical checks.
- [ ] User and teaching are complete in Facade, Domain, Application, Infrastructure, and Adapter.
- [ ] Common remains business-neutral and Starter remains assembly-only.
- [ ] JPA is the only persistence implementation; V1 checksum and one V2 pass.
- [ ] HTTP, GraphQL, Dubbo, RabbitMQ, Redis, AOP, OpenAPI, filters, converters, and four validator layers are tested.
- [ ] Local/test require no Redis, RabbitMQ, Nacos, PostgreSQL, registry, or running application.
- [ ] No-mock vertical and rollback tests pass.
- [ ] Required/forbidden manifest, every-package docs, README, CI verify, package, and Docker checks pass.
- [ ] Evaluation generation and outbound cross-Project clients remain deferred.
