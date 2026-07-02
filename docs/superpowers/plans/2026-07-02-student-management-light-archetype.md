# Student Management Light Archetype Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace `egon-cola-archetype-light` with a Spring Boot 3.5.16, Java 21, single-module `student-management` archetype that follows the large-monolith light domain architecture.

**Architecture:** The generated application remains one Maven module. Layering is expressed through Java packages: `start`, `adapter`, `facade`, `application`, `infrastructure`, `common`, and `domain`. The sample implements student registration, course creation, and assigning a course to a student using application orchestration, domain services, domain repository ports, and Spring Data JPA infrastructure adapters.

**Tech Stack:** Maven Archetype Plugin, Spring Boot 3.5.16, Java 21, Spring MVC, Spring Validation, Spring Data JPA, Flyway, H2, PostgreSQL runtime driver, Lombok optional, JUnit 5, ArchUnit, Maven Wrapper 3.9.14.

---

## File Structure

Primary files to create or replace under `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources`:

- Replace: `pom.xml` with Spring Boot parent inheritance and generic student-management dependencies.
- Create: `mvnw`, `mvnw.cmd`, `.mvn/wrapper/maven-wrapper.properties`, `__gitignore__`, `.gitattributes`.
- Replace: `README.md` with generated project usage and package rules.
- Delete: `img.png`, `img_1.png`, all charging Java source, charging tests, charging HTTP files, charging fixtures.
- Create: `src/main/java/start/StudentManagementApplication.java`.
- Create: `src/main/java/adapter/controller/student/StudentController.java`.
- Create: `src/main/java/adapter/controller/teaching/CourseController.java`.
- Create: `src/main/java/adapter/facade/impl/StudentManagementFacadeImpl.java`.
- Create: `src/main/java/adapter/handler/GlobalExceptionHandler.java`.
- Create: `src/main/java/adapter/convertor/StudentAdapterConverter.java`.
- Create: `src/main/java/adapter/convertor/CourseAdapterConverter.java`.
- Create: `src/main/java/facade/api/StudentManagementFacade.java`.
- Create: request/response DTO records under `src/main/java/facade/dto`.
- Create: `src/main/java/facade/enums/StudentStatus.java`.
- Create: `src/main/java/application/manage/student/StudentManage.java`, `StudentView.java`, and `impl/StudentManageImpl.java`.
- Create: `src/main/java/application/manage/teaching/CourseManage.java`, `CourseView.java`, and `impl/CourseManageImpl.java`.
- Create: `src/main/java/application/validators/StudentApplicationValidator.java`.
- Create: `src/main/java/common/constants/ErrorCodes.java`.
- Create: `src/main/java/common/exceptions/BizException.java`.
- Create: `src/main/java/common/exceptions/NotFoundException.java`.
- Create: `src/main/java/common/response/Response.java`.
- Create: `src/main/java/common/response/SingleResponse.java`.
- Create: `src/main/java/common/utils/IdGenerator.java`.
- Create: domain model, service, and repository ports under `src/main/java/domain/student` and `src/main/java/domain/teaching`.
- Create: Spring Data JPA PO, JPA repository, converter, and domain repository implementation files under `src/main/java/infrastructure/repo/student` and `src/main/java/infrastructure/repo/teaching`.
- Create: `src/main/resources/application.yml`.
- Create: `src/main/resources/db/migration/V1__init_student_management.sql`.
- Create: `src/test/java/ArchitectureDependencyTest.java`.
- Create: `src/test/java/application/StudentManagementFlowTest.java`.

Archetype metadata and integration test files:

- Replace: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/META-INF/maven/archetype-metadata.xml`.
- Modify: `egon-cola-archetypes/egon-cola-archetype-light/src/test/resources/projects/basic/goal.txt`.
- Create: `egon-cola-archetypes/egon-cola-archetype-light/src/test/resources/projects/basic/verify.groovy`.

Package dependency contract for generated code:

```text
start -> adapter, infrastructure
adapter -> application, facade, common
facade -> JDK and jakarta validation only
application -> domain, common, Spring stereotype and transaction APIs
domain -> common
infrastructure -> domain, Spring Data JPA
common -> JDK only
```

## Implementation Contracts

Use these exact generated APIs so all tasks and tests agree on type names.

Facade DTO records:

```java
package ${package}.facade.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record RegisterStudentRequest(
        @NotBlank String name,
        @Email @NotBlank String email
) {
}
```

```java
package ${package}.facade.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateCourseRequest(
        @NotBlank String name,
        String description
) {
}
```

```java
package ${package}.facade.dto;

import jakarta.validation.constraints.NotBlank;

public record AssignCourseRequest(
        @NotBlank String studentId,
        @NotBlank String courseId
) {
}
```

```java
package ${package}.facade.dto;

import java.util.List;

public record StudentDTO(
        String id,
        String name,
        String email,
        String status,
        List<String> courseIds
) {
}
```

```java
package ${package}.facade.dto;

public record CourseDTO(
        String id,
        String name,
        String description
) {
}
```

Application APIs:

```java
package ${package}.application.manage.student;

public interface StudentManage {
    StudentView register(String name, String email);

    StudentView getById(String studentId);
}
```

```java
package ${package}.application.manage.student;

import java.util.List;

public record StudentView(
        String id,
        String name,
        String email,
        String status,
        List<String> courseIds
) {
}
```

```java
package ${package}.application.manage.teaching;

public interface CourseManage {
    CourseView create(String name, String description);

    CourseView getById(String courseId);

    void assignCourse(String studentId, String courseId);
}
```

```java
package ${package}.application.manage.teaching;

public record CourseView(
        String id,
        String name,
        String description
) {
}
```

Domain repository ports:

```java
package ${package}.domain.student.repos;

import ${package}.domain.student.model.Student;

import java.util.Optional;

public interface StudentRepository {
    Student save(Student student);

    Optional<Student> findById(String studentId);

    boolean existsByEmail(String email);
}
```

```java
package ${package}.domain.teaching.repos;

import ${package}.domain.teaching.model.Course;

import java.util.Optional;

public interface CourseRepository {
    Course save(Course course);

    Optional<Course> findById(String courseId);
}
```

Domain service methods:

```java
package ${package}.domain.student.service;

import ${package}.common.exceptions.BizException;
import ${package}.domain.student.model.Student;

public class StudentDomainService {
    public Student register(String studentId, String name, String email) {
        return Student.register(studentId, name, email);
    }

    public Student assignCourse(Student student, String courseId) {
        if (student.hasCourse(courseId)) {
            throw new BizException("STUDENT_COURSE_DUPLICATED", "student already assigned to course");
        }
        student.assignCourse(courseId);
        return student;
    }
}
```

```java
package ${package}.domain.teaching.service;

import ${package}.domain.teaching.model.Course;

public class CourseDomainService {
    public Course create(String courseId, String name, String description) {
        return Course.create(courseId, name, description);
    }
}
```

Common response contract:

```java
package ${package}.common.response;

public record Response(boolean success, String code, String message) {
    public static Response success() {
        return new Response(true, "SUCCESS", "success");
    }

    public static Response fail(String code, String message) {
        return new Response(false, code, message);
    }
}
```

```java
package ${package}.common.response;

public record SingleResponse<T>(boolean success, String code, String message, T data) {
    public static <T> SingleResponse<T> of(T data) {
        return new SingleResponse<>(true, "SUCCESS", "success", data);
    }

    public static <T> SingleResponse<T> fail(String code, String message) {
        return new SingleResponse<>(false, code, message, null);
    }
}
```

---

### Task 1: Add Archetype Guards for Root Project Defaults

**Files:**
- Modify: `egon-cola-archetypes/egon-cola-archetype-light/src/test/resources/projects/basic/goal.txt`
- Create: `egon-cola-archetypes/egon-cola-archetype-light/src/test/resources/projects/basic/verify.groovy`
- Modify: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/pom.xml`
- Create: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/__gitignore__`
- Create: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/.gitattributes`
- Create: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/.mvn/wrapper/maven-wrapper.properties`
- Create: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/mvnw`
- Create: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/mvnw.cmd`
- Modify: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/META-INF/maven/archetype-metadata.xml`

- [ ] **Step 1: Write the root-file integration guard**

Replace `goal.txt` with:

```text
validate
```

Create `verify.groovy` with:

```groovy
def assertFile = { path ->
    def file = new File(basedir, path)
    assert file.isFile(): "Expected file ${path}"
    file
}

assertFile("pom.xml")
assertFile("mvnw")
assertFile("mvnw.cmd")
assertFile(".mvn/wrapper/maven-wrapper.properties")
assertFile(".gitignore")
assertFile(".gitattributes")
assertFile("README.md")

def pom = assertFile("pom.xml").text
assert pom.contains("<artifactId>spring-boot-starter-parent</artifactId>")
assert pom.contains("<version>3.5.16</version>")
assert pom.contains("<java.version>21</java.version>")
assert !pom.contains("spring-boot-dependencies")
assert !pom.contains("spring-ai")
assert !pom.contains("drools")
assert !pom.contains("mcp")

def wrapper = assertFile(".mvn/wrapper/maven-wrapper.properties").text
assert wrapper.contains("apache-maven/3.9.14/apache-maven-3.9.14-bin.zip")
```

- [ ] **Step 2: Run the integration guard and verify it fails**

Run:

```bash
bash ./mvnw -B -ntp -pl egon-cola-archetypes/egon-cola-archetype-light -am test
```

Expected: FAIL from `verify.groovy` because wrapper and git default files are not generated yet.

- [ ] **Step 3: Replace the generated POM**

Replace `archetype-resources/pom.xml` with:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.5.16</version>
        <relativePath/>
    </parent>

    <groupId>${groupId}</groupId>
    <artifactId>${artifactId}</artifactId>
    <version>${version}</version>
    <packaging>jar</packaging>
    <name>${artifactId}</name>
    <description>Student management sample based on Egon COLA light domain architecture</description>

    <properties>
        <java.version>21</java.version>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <project.reporting.outputEncoding>UTF-8</project.reporting.outputEncoding>
        <archunit.version>1.3.0</archunit.version>
    </properties>

    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>
        <dependency>
            <groupId>org.flywaydb</groupId>
            <artifactId>flyway-core</artifactId>
        </dependency>
        <dependency>
            <groupId>com.h2database</groupId>
            <artifactId>h2</artifactId>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>org.postgresql</groupId>
            <artifactId>postgresql</artifactId>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>com.tngtech.archunit</groupId>
            <artifactId>archunit-junit5</artifactId>
            <version>${archunit.version}</version>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <configuration>
                    <excludes>
                        <exclude>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                        </exclude>
                    </excludes>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

- [ ] **Step 4: Add Maven Wrapper and git default files**

Copy wrapper scripts from the repository root:

```bash
cp mvnw egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/mvnw
cp mvnw.cmd egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/mvnw.cmd
mkdir -p egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/.mvn/wrapper
cp .mvn/wrapper/maven-wrapper.properties egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/.mvn/wrapper/maven-wrapper.properties
```

Create `__gitignore__` with:

```gitignore
target/

### Java ###
*.class
*.log
*.tmp

### Maven ###
.mvn/timing.properties

### IntelliJ IDEA ###
.idea/
*.iws
*.iml
*.ipr
out/

### Eclipse ###
.apt_generated
.classpath
.factorypath
.project
.settings/
.springBeans

### NetBeans ###
nbproject/private/
build/
nbbuild/
dist/
nbdist/
bin/

### OS ###
.DS_Store
Thumbs.db

### Local env ###
.env
.env.*
application-local.yml
```

Create `.gitattributes` with:

```gitattributes
* text=auto eol=lf

mvnw text eol=lf
mvnw.cmd text eol=crlf

*.png binary
*.jpg binary
*.jpeg binary
*.gif binary
*.ico binary
*.jar binary
```

- [ ] **Step 5: Update archetype metadata for root files**

In `archetype-metadata.xml`, change descriptor `name` to `student-management`, add:

```xml
  <requiredProperties>
    <requiredProperty key="gitignore">
      <defaultValue>.gitignore</defaultValue>
    </requiredProperty>
  </requiredProperties>
```

Add root file sets:

```xml
    <fileSet filtered="true" encoding="UTF-8">
      <directory></directory>
      <includes>
        <include>pom.xml</include>
        <include>README.md</include>
      </includes>
    </fileSet>
    <fileSet encoding="UTF-8">
      <directory></directory>
      <includes>
        <include>__gitignore__</include>
        <include>.gitattributes</include>
        <include>mvnw</include>
        <include>mvnw.cmd</include>
      </includes>
    </fileSet>
    <fileSet encoding="UTF-8">
      <directory>.mvn/wrapper</directory>
      <includes>
        <include>maven-wrapper.properties</include>
      </includes>
    </fileSet>
```

Remove root includes for `img.png`, `img_1.png`, `charge-parent.iml`, `charge.iml`, and `charging-system.iml`.

- [ ] **Step 6: Run the root-file guard and verify it passes**

Run:

```bash
bash ./mvnw -B -ntp -pl egon-cola-archetypes/egon-cola-archetype-light -am test
```

Expected: PASS for root-file assertions and generated project `validate`.

- [ ] **Step 7: Commit Task 1**

```bash
git add egon-cola-archetypes/egon-cola-archetype-light/src/test/resources/projects/basic/goal.txt \
        egon-cola-archetypes/egon-cola-archetype-light/src/test/resources/projects/basic/verify.groovy \
        egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/pom.xml \
        egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/__gitignore__ \
        egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/.gitattributes \
        egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/.mvn/wrapper/maven-wrapper.properties \
        egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/mvnw \
        egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/mvnw.cmd \
        egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/META-INF/maven/archetype-metadata.xml
git commit -m "feat: add light archetype project defaults"
```

---

### Task 2: Replace Charging Source with Student Management Main Code

**Files:**
- Delete: every old charging source under `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java`
- Create: all main Java files listed in the File Structure section
- Create: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/resources/application.yml`
- Create: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/resources/db/migration/V1__init_student_management.sql`
- Modify: `egon-cola-archetypes/egon-cola-archetype-light/src/test/resources/projects/basic/verify.groovy`

- [ ] **Step 1: Extend the generated project guard for student-management source**

Append to `verify.groovy`:

```groovy
assertFile("src/main/java/it/pkg/start/StudentManagementApplication.java")
assertFile("src/main/java/it/pkg/adapter/controller/student/StudentController.java")
assertFile("src/main/java/it/pkg/adapter/controller/teaching/CourseController.java")
assertFile("src/main/java/it/pkg/facade/api/StudentManagementFacade.java")
assertFile("src/main/java/it/pkg/application/manage/student/StudentManage.java")
assertFile("src/main/java/it/pkg/application/manage/teaching/CourseManage.java")
assertFile("src/main/java/it/pkg/domain/student/model/Student.java")
assertFile("src/main/java/it/pkg/domain/teaching/model/Course.java")
assertFile("src/main/java/it/pkg/infrastructure/repo/student/impl/StudentRepositoryImpl.java")
assertFile("src/main/java/it/pkg/infrastructure/repo/teaching/impl/CourseRepositoryImpl.java")
assertFile("src/main/resources/application.yml")
assertFile("src/main/resources/db/migration/V1__init_student_management.sql")

assert !new File(basedir, "src/main/java/it/pkg/adapter/ChargeController.java").exists()
assert !new File(basedir, "src/main/java/it/pkg/domain/charge").exists()
assert !new File(basedir, "src/test/charge.http").exists()

def migrationDir = new File(basedir, "src/main/resources/db/migration")
assert migrationDir.listFiles({ dir, name -> name.endsWith(".sql") } as FilenameFilter).size() == 1
```

- [ ] **Step 2: Run the guard and verify it fails**

Run:

```bash
bash ./mvnw -B -ntp -pl egon-cola-archetypes/egon-cola-archetype-light -am test
```

Expected: FAIL because generated `student-management` source files do not exist yet.

- [ ] **Step 3: Remove old charging template files**

Run:

```bash
rm -rf egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java/*
rm -rf egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/test
rm -f egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/img.png
rm -f egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/img_1.png
```

- [ ] **Step 4: Create common and facade classes**

Create the DTO, response, exception, enum, and facade API files from the Implementation Contracts section. Add this facade interface:

```java
package ${package}.facade.api;

import ${package}.facade.dto.AssignCourseRequest;
import ${package}.facade.dto.CourseDTO;
import ${package}.facade.dto.CreateCourseRequest;
import ${package}.facade.dto.RegisterStudentRequest;
import ${package}.facade.dto.StudentDTO;

public interface StudentManagementFacade {
    StudentDTO registerStudent(RegisterStudentRequest request);

    StudentDTO getStudent(String studentId);

    CourseDTO createCourse(CreateCourseRequest request);

    void assignCourse(AssignCourseRequest request);
}
```

Add `BizException` and `NotFoundException`:

```java
package ${package}.common.exceptions;

public class BizException extends RuntimeException {
    private final String code;

    public BizException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
```

```java
package ${package}.common.exceptions;

public class NotFoundException extends BizException {
    public NotFoundException(String code, String message) {
        super(code, message);
    }
}
```

Add `ErrorCodes` and `IdGenerator`:

```java
package ${package}.common.constants;

public final class ErrorCodes {
    public static final String STUDENT_NOT_FOUND = "STUDENT_NOT_FOUND";
    public static final String COURSE_NOT_FOUND = "COURSE_NOT_FOUND";
    public static final String STUDENT_EMAIL_DUPLICATED = "STUDENT_EMAIL_DUPLICATED";

    private ErrorCodes() {
    }
}
```

```java
package ${package}.common.utils;

import java.util.UUID;

public final class IdGenerator {
    private IdGenerator() {
    }

    public static String nextId() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
```

- [ ] **Step 5: Create domain models, services, and repository ports**

Create `Student.java`:

```java
package ${package}.domain.student.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Student {
    private final String id;
    private final String name;
    private final String email;
    private final StudentStatus status;
    private final List<String> courseIds;

    private Student(String id, String name, String email, StudentStatus status, List<String> courseIds) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.status = status;
        this.courseIds = new ArrayList<>(courseIds);
    }

    public static Student register(String id, String name, String email) {
        return new Student(id, name, email, StudentStatus.ACTIVE, List.of());
    }

    public static Student restore(String id, String name, String email, StudentStatus status, List<String> courseIds) {
        return new Student(id, name, email, status, courseIds);
    }

    public void assignCourse(String courseId) {
        courseIds.add(courseId);
    }

    public boolean hasCourse(String courseId) {
        return courseIds.contains(courseId);
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public StudentStatus getStatus() {
        return status;
    }

    public List<String> getCourseIds() {
        return Collections.unmodifiableList(courseIds);
    }
}
```

Create `StudentStatus.java` in `domain/student/model`:

```java
package ${package}.domain.student.model;

public enum StudentStatus {
    ACTIVE
}
```

Create `StudentStatus.java` in `facade/enums`:

```java
package ${package}.facade.enums;

public enum StudentStatus {
    ACTIVE
}
```

Create `Course.java`:

```java
package ${package}.domain.teaching.model;

public class Course {
    private final String id;
    private final String name;
    private final String description;

    private Course(String id, String name, String description) {
        this.id = id;
        this.name = name;
        this.description = description;
    }

    public static Course create(String id, String name, String description) {
        return new Course(id, name, description);
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }
}
```

Create repository ports and services from the Implementation Contracts section.

- [ ] **Step 6: Create application validators and manage implementations**

Create `StudentApplicationValidator.java`:

```java
package ${package}.application.validators;

import ${package}.common.exceptions.BizException;

public final class StudentApplicationValidator {
    private StudentApplicationValidator() {
    }

    public static void requireText(String fieldName, String value) {
        if (value == null || value.isBlank()) {
            throw new BizException("VALIDATION_ERROR", fieldName + " must not be blank");
        }
    }
}
```

Create `package-info.java` marker files for architecture packages that do not need concrete classes:

```java
package ${package}.start.config;
```

```java
package ${package}.adapter.vo;
```

```java
package ${package}.application.convertor;
```

```java
package ${package}.infrastructure.config;
```

Create `StudentManageImpl.java`:

```java
package ${package}.application.manage.student.impl;

import ${package}.application.manage.student.StudentManage;
import ${package}.application.manage.student.StudentView;
import ${package}.common.constants.ErrorCodes;
import ${package}.common.exceptions.BizException;
import ${package}.common.exceptions.NotFoundException;
import ${package}.common.utils.IdGenerator;
import ${package}.domain.student.model.Student;
import ${package}.domain.student.repos.StudentRepository;
import ${package}.domain.student.service.StudentDomainService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StudentManageImpl implements StudentManage {
    private final StudentRepository studentRepository;
    private final StudentDomainService studentDomainService;

    public StudentManageImpl(StudentRepository studentRepository) {
        this.studentRepository = studentRepository;
        this.studentDomainService = new StudentDomainService();
    }

    @Override
    @Transactional
    public StudentView register(String name, String email) {
        if (studentRepository.existsByEmail(email)) {
            throw new BizException(ErrorCodes.STUDENT_EMAIL_DUPLICATED, "student email already exists");
        }
        Student student = studentDomainService.register(IdGenerator.nextId(), name, email);
        return toView(studentRepository.save(student));
    }

    @Override
    @Transactional(readOnly = true)
    public StudentView getById(String studentId) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new NotFoundException(ErrorCodes.STUDENT_NOT_FOUND, "student not found"));
        return toView(student);
    }

    private StudentView toView(Student student) {
        return new StudentView(student.getId(), student.getName(), student.getEmail(), student.getStatus().name(), student.getCourseIds());
    }
}
```

Create `CourseManageImpl.java`:

```java
package ${package}.application.manage.teaching.impl;

import ${package}.application.manage.teaching.CourseManage;
import ${package}.application.manage.teaching.CourseView;
import ${package}.common.constants.ErrorCodes;
import ${package}.common.exceptions.NotFoundException;
import ${package}.common.utils.IdGenerator;
import ${package}.domain.student.model.Student;
import ${package}.domain.student.repos.StudentRepository;
import ${package}.domain.student.service.StudentDomainService;
import ${package}.domain.teaching.model.Course;
import ${package}.domain.teaching.repos.CourseRepository;
import ${package}.domain.teaching.service.CourseDomainService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CourseManageImpl implements CourseManage {
    private final CourseRepository courseRepository;
    private final StudentRepository studentRepository;
    private final CourseDomainService courseDomainService;
    private final StudentDomainService studentDomainService;

    public CourseManageImpl(CourseRepository courseRepository, StudentRepository studentRepository) {
        this.courseRepository = courseRepository;
        this.studentRepository = studentRepository;
        this.courseDomainService = new CourseDomainService();
        this.studentDomainService = new StudentDomainService();
    }

    @Override
    @Transactional
    public CourseView create(String name, String description) {
        Course course = courseDomainService.create(IdGenerator.nextId(), name, description);
        return toView(courseRepository.save(course));
    }

    @Override
    @Transactional(readOnly = true)
    public CourseView getById(String courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new NotFoundException(ErrorCodes.COURSE_NOT_FOUND, "course not found"));
        return toView(course);
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

    private CourseView toView(Course course) {
        return new CourseView(course.getId(), course.getName(), course.getDescription());
    }
}
```

- [ ] **Step 7: Create adapter controllers, converters, facade implementation, and handler**

Create `StudentAdapterConverter.java`:

```java
package ${package}.adapter.convertor;

import ${package}.application.manage.student.StudentView;
import ${package}.facade.dto.StudentDTO;

public final class StudentAdapterConverter {
    private StudentAdapterConverter() {
    }

    public static StudentDTO toDto(StudentView view) {
        return new StudentDTO(view.id(), view.name(), view.email(), view.status(), view.courseIds());
    }
}
```

Create `CourseAdapterConverter.java`:

```java
package ${package}.adapter.convertor;

import ${package}.application.manage.teaching.CourseView;
import ${package}.facade.dto.CourseDTO;

public final class CourseAdapterConverter {
    private CourseAdapterConverter() {
    }

    public static CourseDTO toDto(CourseView view) {
        return new CourseDTO(view.id(), view.name(), view.description());
    }
}
```

Create controllers with constructor injection:

```java
package ${package}.adapter.controller.student;

import ${package}.adapter.convertor.StudentAdapterConverter;
import ${package}.application.manage.student.StudentManage;
import ${package}.common.response.SingleResponse;
import ${package}.facade.dto.RegisterStudentRequest;
import ${package}.facade.dto.StudentDTO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/students")
public class StudentController {
    private final StudentManage studentManage;

    public StudentController(StudentManage studentManage) {
        this.studentManage = studentManage;
    }

    @PostMapping
    public SingleResponse<StudentDTO> register(@Valid @RequestBody RegisterStudentRequest request) {
        return SingleResponse.of(StudentAdapterConverter.toDto(studentManage.register(request.name(), request.email())));
    }

    @GetMapping("/{studentId}")
    public SingleResponse<StudentDTO> getById(@PathVariable String studentId) {
        return SingleResponse.of(StudentAdapterConverter.toDto(studentManage.getById(studentId)));
    }
}
```

```java
package ${package}.adapter.controller.teaching;

import ${package}.adapter.convertor.CourseAdapterConverter;
import ${package}.application.manage.teaching.CourseManage;
import ${package}.common.response.Response;
import ${package}.common.response.SingleResponse;
import ${package}.facade.dto.CourseDTO;
import ${package}.facade.dto.CreateCourseRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/courses")
public class CourseController {
    private final CourseManage courseManage;

    public CourseController(CourseManage courseManage) {
        this.courseManage = courseManage;
    }

    @PostMapping
    public SingleResponse<CourseDTO> create(@Valid @RequestBody CreateCourseRequest request) {
        return SingleResponse.of(CourseAdapterConverter.toDto(courseManage.create(request.name(), request.description())));
    }

    @PostMapping("/{courseId}/students/{studentId}")
    public Response assignCourse(@PathVariable String courseId, @PathVariable String studentId) {
        courseManage.assignCourse(studentId, courseId);
        return Response.success();
    }
}
```

Create `StudentManagementFacadeImpl.java`:

```java
package ${package}.adapter.facade.impl;

import ${package}.adapter.convertor.CourseAdapterConverter;
import ${package}.adapter.convertor.StudentAdapterConverter;
import ${package}.application.manage.student.StudentManage;
import ${package}.application.manage.teaching.CourseManage;
import ${package}.facade.api.StudentManagementFacade;
import ${package}.facade.dto.AssignCourseRequest;
import ${package}.facade.dto.CourseDTO;
import ${package}.facade.dto.CreateCourseRequest;
import ${package}.facade.dto.RegisterStudentRequest;
import ${package}.facade.dto.StudentDTO;
import org.springframework.stereotype.Service;

@Service
public class StudentManagementFacadeImpl implements StudentManagementFacade {
    private final StudentManage studentManage;
    private final CourseManage courseManage;

    public StudentManagementFacadeImpl(StudentManage studentManage, CourseManage courseManage) {
        this.studentManage = studentManage;
        this.courseManage = courseManage;
    }

    @Override
    public StudentDTO registerStudent(RegisterStudentRequest request) {
        return StudentAdapterConverter.toDto(studentManage.register(request.name(), request.email()));
    }

    @Override
    public StudentDTO getStudent(String studentId) {
        return StudentAdapterConverter.toDto(studentManage.getById(studentId));
    }

    @Override
    public CourseDTO createCourse(CreateCourseRequest request) {
        return CourseAdapterConverter.toDto(courseManage.create(request.name(), request.description()));
    }

    @Override
    public void assignCourse(AssignCourseRequest request) {
        courseManage.assignCourse(request.studentId(), request.courseId());
    }
}
```

Create `GlobalExceptionHandler.java`:

```java
package ${package}.adapter.handler;

import ${package}.common.exceptions.BizException;
import ${package}.common.response.Response;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(BizException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Response handleBizException(BizException exception) {
        return Response.fail(exception.getCode(), exception.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Response handleValidationException(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getField() + " " + error.getDefaultMessage())
                .orElse("request validation failed");
        return Response.fail("VALIDATION_ERROR", message);
    }
}
```

- [ ] **Step 8: Create infrastructure persistence adapters**

Create JPA PO files:

```java
package ${package}.infrastructure.repo.student.po;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "students")
public class StudentPo {
    @Id
    private String id;
    private String name;
    private String email;
    private String status;
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    protected StudentPo() {
    }

    public StudentPo(String id, String name, String email, String status, LocalDateTime createdAt) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.status = status;
        this.createdAt = createdAt;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
```

```java
package ${package}.infrastructure.repo.student.po;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "student_course_assignments")
public class StudentCoursePo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "student_id")
    private String studentId;
    @Column(name = "course_id")
    private String courseId;
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    protected StudentCoursePo() {
    }

    public StudentCoursePo(String studentId, String courseId, LocalDateTime createdAt) {
        this.studentId = studentId;
        this.courseId = courseId;
        this.createdAt = createdAt;
    }

    public String getStudentId() { return studentId; }
    public String getCourseId() { return courseId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
```

```java
package ${package}.infrastructure.repo.teaching.po;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "courses")
public class CoursePo {
    @Id
    private String id;
    private String name;
    private String description;
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    protected CoursePo() {
    }

    public CoursePo(String id, String name, String description, LocalDateTime createdAt) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.createdAt = createdAt;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
```

Create Spring Data repository interfaces:

```java
package ${package}.infrastructure.repo.student.jpa;

import ${package}.infrastructure.repo.student.po.StudentPo;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudentJpaRepository extends JpaRepository<StudentPo, String> {
    boolean existsByEmail(String email);
}
```

```java
package ${package}.infrastructure.repo.student.jpa;

import ${package}.infrastructure.repo.student.po.StudentCoursePo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StudentCourseJpaRepository extends JpaRepository<StudentCoursePo, Long> {
    List<StudentCoursePo> findByStudentId(String studentId);

    boolean existsByStudentIdAndCourseId(String studentId, String courseId);
}
```

```java
package ${package}.infrastructure.repo.teaching.jpa;

import ${package}.infrastructure.repo.teaching.po.CoursePo;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseJpaRepository extends JpaRepository<CoursePo, String> {
}
```

Create `StudentPoConverter.java`:

```java
package ${package}.infrastructure.repo.student.converter;

import ${package}.domain.student.model.Student;
import ${package}.domain.student.model.StudentStatus;
import ${package}.infrastructure.repo.student.po.StudentCoursePo;
import ${package}.infrastructure.repo.student.po.StudentPo;

import java.time.LocalDateTime;
import java.util.List;

public final class StudentPoConverter {
    private StudentPoConverter() {
    }

    public static StudentPo toPo(Student student) {
        return new StudentPo(student.getId(), student.getName(), student.getEmail(), student.getStatus().name(), LocalDateTime.now());
    }

    public static Student toDomain(StudentPo studentPo, List<StudentCoursePo> coursePos) {
        List<String> courseIds = coursePos.stream()
                .map(StudentCoursePo::getCourseId)
                .toList();
        return Student.restore(studentPo.getId(), studentPo.getName(), studentPo.getEmail(), StudentStatus.valueOf(studentPo.getStatus()), courseIds);
    }
}
```

Create `CoursePoConverter.java`:

```java
package ${package}.infrastructure.repo.teaching.converter;

import ${package}.domain.teaching.model.Course;
import ${package}.infrastructure.repo.teaching.po.CoursePo;

import java.time.LocalDateTime;

public final class CoursePoConverter {
    private CoursePoConverter() {
    }

    public static CoursePo toPo(Course course) {
        return new CoursePo(course.getId(), course.getName(), course.getDescription(), LocalDateTime.now());
    }

    public static Course toDomain(CoursePo coursePo) {
        return Course.create(coursePo.getId(), coursePo.getName(), coursePo.getDescription());
    }
}
```

Create `StudentRepositoryImpl.java`:

```java
package ${package}.infrastructure.repo.student.impl;

import ${package}.domain.student.model.Student;
import ${package}.domain.student.repos.StudentRepository;
import ${package}.infrastructure.repo.student.converter.StudentPoConverter;
import ${package}.infrastructure.repo.student.jpa.StudentCourseJpaRepository;
import ${package}.infrastructure.repo.student.jpa.StudentJpaRepository;
import ${package}.infrastructure.repo.student.po.StudentCoursePo;
import ${package}.infrastructure.repo.student.po.StudentPo;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public class StudentRepositoryImpl implements StudentRepository {
    private final StudentJpaRepository studentJpaRepository;
    private final StudentCourseJpaRepository studentCourseJpaRepository;

    public StudentRepositoryImpl(StudentJpaRepository studentJpaRepository,
                                 StudentCourseJpaRepository studentCourseJpaRepository) {
        this.studentJpaRepository = studentJpaRepository;
        this.studentCourseJpaRepository = studentCourseJpaRepository;
    }

    @Override
    public Student save(Student student) {
        StudentPo saved = studentJpaRepository.save(StudentPoConverter.toPo(student));
        student.getCourseIds().forEach(courseId -> {
            if (!studentCourseJpaRepository.existsByStudentIdAndCourseId(student.getId(), courseId)) {
                studentCourseJpaRepository.save(new StudentCoursePo(student.getId(), courseId, LocalDateTime.now()));
            }
        });
        return StudentPoConverter.toDomain(saved, studentCourseJpaRepository.findByStudentId(student.getId()));
    }

    @Override
    public Optional<Student> findById(String studentId) {
        return studentJpaRepository.findById(studentId)
                .map(studentPo -> StudentPoConverter.toDomain(studentPo, studentCourseJpaRepository.findByStudentId(studentId)));
    }

    @Override
    public boolean existsByEmail(String email) {
        return studentJpaRepository.existsByEmail(email);
    }
}
```

Create `CourseRepositoryImpl.java`:

```java
package ${package}.infrastructure.repo.teaching.impl;

import ${package}.domain.teaching.model.Course;
import ${package}.domain.teaching.repos.CourseRepository;
import ${package}.infrastructure.repo.teaching.converter.CoursePoConverter;
import ${package}.infrastructure.repo.teaching.jpa.CourseJpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class CourseRepositoryImpl implements CourseRepository {
    private final CourseJpaRepository courseJpaRepository;

    public CourseRepositoryImpl(CourseJpaRepository courseJpaRepository) {
        this.courseJpaRepository = courseJpaRepository;
    }

    @Override
    public Course save(Course course) {
        return CoursePoConverter.toDomain(courseJpaRepository.save(CoursePoConverter.toPo(course)));
    }

    @Override
    public Optional<Course> findById(String courseId) {
        return courseJpaRepository.findById(courseId).map(CoursePoConverter::toDomain);
    }
}
```

- [ ] **Step 9: Create application configuration and Flyway migration**

Create `application.yml`:

```yaml
spring:
  application:
    name: ${artifactId}
  datasource:
    url: jdbc:h2:mem:${artifactId};MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH
    username: sa
    password:
    driver-class-name: org.h2.Driver
  flyway:
    enabled: true
  jpa:
    hibernate:
      ddl-auto: validate
    open-in-view: false
    properties:
      hibernate:
        format_sql: true
```

Create `V1__init_student_management.sql`:

```sql
CREATE TABLE students (
    id VARCHAR(64) PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    email VARCHAR(160) NOT NULL UNIQUE,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE courses (
    id VARCHAR(64) PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    description VARCHAR(500),
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE student_course_assignments (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    student_id VARCHAR(64) NOT NULL,
    course_id VARCHAR(64) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_student_course UNIQUE (student_id, course_id)
);
```

- [ ] **Step 10: Run the guard and compile check**

Run:

```bash
bash ./mvnw -B -ntp -pl egon-cola-archetypes/egon-cola-archetype-light -am test
```

Expected: PASS for archetype generation and generated project `validate`.

- [ ] **Step 11: Commit Task 2**

```bash
git add egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources \
        egon-cola-archetypes/egon-cola-archetype-light/src/test/resources/projects/basic/verify.groovy
git commit -m "feat: replace light archetype with student management sample"
```

---

### Task 3: Add Generated Tests and Enforce Package Boundaries

**Files:**
- Create: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/test/java/ArchitectureDependencyTest.java`
- Create: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/test/java/application/StudentManagementFlowTest.java`
- Modify: `egon-cola-archetypes/egon-cola-archetype-light/src/test/resources/projects/basic/goal.txt`
- Modify: `egon-cola-archetypes/egon-cola-archetype-light/src/test/resources/projects/basic/verify.groovy`

- [ ] **Step 1: Update generated project goal to run tests**

Replace `goal.txt` with:

```text
test
```

- [ ] **Step 2: Add generated test assertions**

Append to `verify.groovy`:

```groovy
assertFile("src/test/java/it/pkg/ArchitectureDependencyTest.java")
assertFile("src/test/java/it/pkg/application/StudentManagementFlowTest.java")
```

- [ ] **Step 3: Run generated tests and verify they fail**

Run:

```bash
bash ./mvnw -B -ntp -pl egon-cola-archetypes/egon-cola-archetype-light -am test
```

Expected: FAIL because the generated test files do not exist yet.

- [ ] **Step 4: Create ArchUnit boundary test**

Create `ArchitectureDependencyTest.java`:

```java
#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package};

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

class ArchitectureDependencyTest {
    private final JavaClasses classes = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("${package}");

    @Test
    void domain_does_not_depend_on_outer_layers() {
        noClasses().that().resideInAPackage("${package}.domain..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "${package}.adapter..",
                        "${package}.application..",
                        "${package}.facade..",
                        "${package}.infrastructure..",
                        "${package}.start..")
                .check(classes);
    }

    @Test
    void facade_does_not_depend_on_internal_layers() {
        noClasses().that().resideInAPackage("${package}.facade..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "${package}.adapter..",
                        "${package}.application..",
                        "${package}.common..",
                        "${package}.domain..",
                        "${package}.infrastructure..",
                        "${package}.start..")
                .check(classes);
    }

    @Test
    void application_does_not_depend_on_adapter_or_infrastructure() {
        noClasses().that().resideInAPackage("${package}.application..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "${package}.adapter..",
                        "${package}.facade..",
                        "${package}.infrastructure..",
                        "${package}.start..")
                .check(classes);
    }
}
```

The rule intentionally forbids `domain -> facade`; keep domain status in `domain.student.model.StudentStatus` and map to DTO status strings outside the domain layer.

- [ ] **Step 5: Create generated flow test**

Create `StudentManagementFlowTest.java`:

```java
#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.application;

import ${package}.application.manage.student.StudentManage;
import ${package}.application.manage.student.StudentView;
import ${package}.application.manage.teaching.CourseManage;
import ${package}.application.manage.teaching.CourseView;
import ${package}.start.StudentManagementApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = StudentManagementApplication.class)
class StudentManagementFlowTest {
    @Autowired
    private StudentManage studentManage;

    @Autowired
    private CourseManage courseManage;

    @Test
    void register_student_and_assign_course() {
        StudentView student = studentManage.register("Mario", "mario@example.com");
        CourseView course = courseManage.create("Architecture", "Large monolith light domain architecture");

        courseManage.assignCourse(student.id(), course.id());

        StudentView saved = studentManage.getById(student.id());
        assertThat(saved.email()).isEqualTo("mario@example.com");
        assertThat(saved.courseIds()).containsExactly(course.id());
    }
}
```

- [ ] **Step 6: Run generated tests**

Run:

```bash
bash ./mvnw -B -ntp -pl egon-cola-archetypes/egon-cola-archetype-light -am test
```

Expected: PASS. If the generated project test fails due Flyway/Hibernate schema mismatch, fix the PO annotations or migration column definitions before continuing.

- [ ] **Step 7: Commit Task 3**

```bash
git add egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/test \
        egon-cola-archetypes/egon-cola-archetype-light/src/test/resources/projects/basic/goal.txt \
        egon-cola-archetypes/egon-cola-archetype-light/src/test/resources/projects/basic/verify.groovy
git commit -m "test: add student management archetype verification"
```

---

### Task 4: Finalize README, Metadata, and Cleanup

**Files:**
- Replace: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/README.md`
- Modify: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/META-INF/maven/archetype-metadata.xml`
- Modify: `egon-cola-archetypes/egon-cola-archetype-light/src/test/resources/projects/basic/verify.groovy`
- Delete: obsolete generated resources still matching charging names.

- [ ] **Step 1: Add README verification**

Append to `verify.groovy`:

```groovy
def readme = assertFile("README.md").text
assert readme.contains("Student Management")
assert readme.contains("single Maven module")
assert readme.contains("start / adapter / facade / application / infrastructure / common / domain")
assert !readme.contains("计费")
assert !readme.contains("Charge")

def allFiles = []
new File(basedir, "src").eachFileRecurse { file ->
    if (file.isFile()) {
        allFiles << file
    }
}
assert allFiles.every { !it.name.contains("Charge") }
assert !new File(basedir, "img.png").exists()
assert !new File(basedir, "img_1.png").exists()
```

- [ ] **Step 2: Run cleanup guard and verify it fails if README is still old**

Run:

```bash
bash ./mvnw -B -ntp -pl egon-cola-archetypes/egon-cola-archetype-light -am test
```

Expected: FAIL if the old charging README or obsolete image files remain.

- [ ] **Step 3: Replace README**

Replace `README.md` with:

```markdown
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
```

- [ ] **Step 4: Final metadata cleanup**

Ensure `archetype-metadata.xml` contains:

```xml
    <fileSet filtered="true" packaged="true" encoding="UTF-8">
      <directory>src/main/java</directory>
      <includes>
        <include>**/*.java</include>
      </includes>
    </fileSet>
    <fileSet filtered="true" packaged="true" encoding="UTF-8">
      <directory>src/test/java</directory>
      <includes>
        <include>**/*.java</include>
      </includes>
    </fileSet>
    <fileSet filtered="true" encoding="UTF-8">
      <directory>src/main/resources</directory>
      <includes>
        <include>**/*.yml</include>
        <include>**/*.sql</include>
        <include>**/*.xml</include>
      </includes>
    </fileSet>
```

Ensure no metadata fileSet references `.idea`, `*.iml`, `img.png`, `img_1.png`, `charge.http`, `fixture/wiremock`, or old charging JSON fixtures.

- [ ] **Step 5: Run final validation**

Run:

```bash
bash ./mvnw -B -ntp -pl egon-cola-archetypes/egon-cola-archetype-light -am test
rg -n "Charge|charging|计费|img_1|img\\.png|wiremock|charge\\.http" egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/META-INF/maven/archetype-metadata.xml
```

Expected:

- Maven command PASS.
- `rg` returns no matches. Because `rg` exits 1 when no matches are found, exit code 1 is the expected result for the search command.

- [ ] **Step 6: Commit Task 4**

```bash
git add egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/README.md \
        egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/META-INF/maven/archetype-metadata.xml \
        egon-cola-archetypes/egon-cola-archetype-light/src/test/resources/projects/basic/verify.groovy \
        egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources
git commit -m "docs: update student management archetype readme"
```

---

### Task 5: End-to-End Generated Project Inspection

**Files:**
- No planned source changes.
- Use generated output under `egon-cola-archetypes/egon-cola-archetype-light/target/test-classes/projects/basic/project/basic` or the generated project path reported by Maven Archetype Plugin.

- [ ] **Step 1: Run the complete focused validation**

Run:

```bash
bash ./mvnw -B -ntp -pl egon-cola-archetypes/egon-cola-archetype-light -am test
```

Expected: PASS.

- [ ] **Step 2: Inspect generated project files**

Run:

```bash
generated_dir="$(find egon-cola-archetypes/egon-cola-archetype-light/target -path '*/projects/basic/project/basic/pom.xml' -print -quit | xargs dirname)"
test -n "$generated_dir"
test -f "$generated_dir/mvnw"
test -f "$generated_dir/mvnw.cmd"
test -f "$generated_dir/.mvn/wrapper/maven-wrapper.properties"
test -f "$generated_dir/.gitignore"
test -f "$generated_dir/.gitattributes"
test -f "$generated_dir/src/main/java/it/pkg/start/StudentManagementApplication.java"
test -f "$generated_dir/src/main/resources/db/migration/V1__init_student_management.sql"
```

Expected: all `test -f` commands pass.

- [ ] **Step 3: Verify generated project with wrapper**

Run:

```bash
generated_dir="$(find egon-cola-archetypes/egon-cola-archetype-light/target -path '*/projects/basic/project/basic/pom.xml' -print -quit | xargs dirname)"
bash "$generated_dir/mvnw" -B -ntp -f "$generated_dir/pom.xml" test
```

Expected: PASS. This proves generated Maven Wrapper 3.9.14 can run tests without starting the application.

- [ ] **Step 4: Confirm git state is limited to intended files**

Run:

```bash
git status --short
```

Expected: clean working tree after the task commits. If untracked generated output appears under `target/`, do not add it.

- [ ] **Step 5: Commit Task 5 if validation-only notes changed a tracked file**

If Task 5 required a tracked test or metadata fix, commit it:

```bash
git add egon-cola-archetypes/egon-cola-archetype-light
git commit -m "test: finalize light archetype generated project validation"
```

If no tracked file changed, do not create an empty commit.

## Self-Review Notes

Spec coverage:

- Complete student-management replacement is covered by Tasks 2, 3, and 4.
- Spring Boot parent POM and Java 21 are covered by Task 1.
- Maven Wrapper 3.9.14, `.gitignore`, `.gitattributes`, and README are covered by Tasks 1 and 4.
- Archetype metadata is covered by Tasks 1 and 4.
- Generated project tests, ArchUnit boundaries, and Maven validation are covered by Tasks 3 and 5.
- No application startup command is included.

Design pattern decision:

- No extra Strategy, Factory, Abstract Factory, Decorator, State, or Chain of Responsibility implementation is planned.
- The plan uses ports and adapters through domain repository ports and infrastructure repository implementations.
- Cross-domain behavior stays in `application.manage.teaching.CourseManageImpl`, matching the architecture document.
