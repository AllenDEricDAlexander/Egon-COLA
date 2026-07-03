# Archetype Mapper and Pagination Follow-up Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Update all three archetypes so generated converter wrappers use concrete MapStruct Plus mapper components instead of the generic `Converter`, and add externally exposed application pagination examples returning `Page<DomainModel>`.

**Architecture:** Keep the existing archetype structures and converter wrapper classes. Move field mapping into top-level `@Mapper(componentModel = "spring")` interfaces, keep wrappers for semantic conversion and list/page composition, and add a small internal `domain.common.Page<T>` model returned by application services. Adapter HTTP/Dubbo contracts expose external page DTOs converted from application `Page<DomainModel>`.

**Tech Stack:** Java 21, Spring Boot 3.5.16, Maven archetype integration tests, Lombok `@RequiredArgsConstructor`, Spring `@Qualifier`, MapStruct Plus `BaseMapper`, Dubbo3 Triple, Spring Data JPA `PageRequest`.

---

## File Structure

### Cross-archetype rules

- Keep existing `*Converter` / `*Convertor` wrapper classes as named Spring Beans.
- Create top-level mapper interfaces beside each wrapper, for example `StudentAdapterMapper.java` and `StudentPoMapper.java`.
- Remove all `import io.github.linpeilie.Converter;` and `private final Converter converter;` from converter wrappers.
- Add `domain/common/Page.java` as the internal application/domain page model.
- Add external `PageResponse<T>` DTOs in facade DTO packages, not application packages.
- Add one page example per archetype: light `Student`, web `User`, service `Course`.
- Update archetype metadata whenever a new template file is added.
- Update `verify.groovy` guardrails before implementation to make the current templates fail for the intended reasons.

### Light archetype responsibilities

- `domain/common/Page.java`: internal page model.
- `domain/student/repos/StudentRepository.java`: adds `Page<Student> findPage(int currentPage, int pageSize)`.
- `infrastructure/repo/student/impl/StudentRepositoryImpl.java`: uses `PageRequest` and `StudentJpaRepository.findAll(pageable)`.
- `application/manage/student/StudentManage.java` and `application/manage/student/impl/StudentManageImpl.java`: expose and implement `getPage` returning `Page<Student>`.
- `facade/dto/PageResponse.java`: external page DTO.
- `facade/api/StudentManagementFacade.java`: adds `PageResponse<StudentDTO> getStudents(int currentPage, int pageSize)`.
- `adapter/controller/student/StudentController.java`: adds HTTP `GET /students?currentPage=&pageSize=` example.
- `adapter/facade/impl/StudentManagementFacadeImpl.java`: adds Dubbo page method.
- `adapter/convertor/*`: wrappers inject `StudentAdapterMapper` and `CourseAdapterMapper`, provide `toPageResponse` where needed.
- `infrastructure/repo/**/converter/*`: wrappers inject concrete PO mappers.
- `src/test/java/application/StudentManagementFlowTest.java`: asserts application page returns domain models and facade page returns external DTOs.
- `src/test/resources/projects/basic/verify.groovy`: enforces no generic `Converter`, new mappers, page API, metadata.

### Web archetype responsibilities

- `__rootArtifactId__-domain/src/main/java/domain/common/Page.java`: internal page model.
- `domain/repos/user/UserRepository.java` and `infrastructure/repo/user/impl/UserRepositoryImpl.java`: add page repository method.
- `application/manage/user/UserManage.java` and `application/manage/user/impl/UserManageImpl.java`: expose and implement `getPage` returning `Page<User>`.
- `facade/dto/PageResponse.java`: external page DTO shared by user facade DTOs.
- `facade/user/UserFacade.java`: adds `PageResponse<UserDTO> getUsers(int currentPage, int pageSize)`.
- `adapter/controller/user/UserController.java`: adds HTTP page endpoint.
- `adapter/facade/user/UserFacadeImpl.java`: adds Dubbo page method.
- Adapter and infrastructure converters: split concrete mapper interfaces into top-level files and remove generic converter injection.
- `starter/src/test/java/starter/OrganizationFlowTest.java`: asserts application and facade page behavior.
- `src/test/resources/projects/basic/verify.groovy`: enforces mapper/page rules.

### Service archetype responsibilities

- `__rootArtifactId__-domain/src/main/java/domain/common/Page.java`: internal page model.
- `domain/repos/course/CourseRepository.java` and `infrastructure/repo/course/impl/CourseRepositoryImpl.java`: add page repository method.
- `application/manage/course/CourseManage.java` and `application/manage/course/impl/CourseManageImpl.java`: expose and implement `getPage` returning `Page<Course>`.
- `facade/dto/PageResponse.java`: external page DTO.
- `facade/api/CourseFacade.java`: adds `SingleResponse<PageResponse<CourseDTO>> getCourses(int currentPage, int pageSize)` to match the existing service facade response style.
- `adapter/facade/impl/CourseFacadeImpl.java`: adds Dubbo page method.
- Adapter and infrastructure converters: split concrete mapper interfaces into top-level files and remove generic converter injection.
- `starter/src/test/java/starter/EvaluationFlowTest.java`: asserts application and facade page behavior.
- `src/test/resources/projects/basic/verify.groovy`: enforces mapper/page rules and pure-service constraints.

---

### Task 1: Add Failing Guardrails for Mapper and Pagination Rules

**Files:**
- Modify: `egon-cola-archetypes/egon-cola-archetype-light/src/test/resources/projects/basic/verify.groovy`
- Modify: `egon-cola-archetypes/egon-cola-archetype-web/src/test/resources/projects/basic/verify.groovy`
- Modify: `egon-cola-archetypes/egon-cola-archetype-service/src/test/resources/projects/basic/verify.groovy`

- [ ] **Step 1: Add reusable Java source helpers to each `verify.groovy`**

In the light verify file, add this helper after `assertFile`:

```groovy
def javaFileTexts = { path ->
    def dir = new File(generatedProjectDir, path)
    assert dir.isDirectory(): "Expected directory ${path}"
    def files = []
    dir.traverse(type: groovy.io.FileType.FILES) { file ->
        if (file.name.endsWith(".java")) {
            files << file
        }
    }
    files.collect { it.text }
}

def assertNoGenericMapStructConverterInjection = { path ->
    javaFileTexts(path).each { text ->
        assert !text.contains("import io.github.linpeilie.Converter;")
        assert !text.contains("private final Converter converter;")
        assert !text.contains('@Qualifier("converter")')
    }
}
```

In the web and service verify files, add this helper after `assertDir`:

```groovy
def javaFileTexts = { path ->
    def dir = new File(projectDir, path)
    assert dir.isDirectory(): "Expected directory ${path}"
    def files = []
    dir.traverse(type: groovy.io.FileType.FILES) { file ->
        if (file.name.endsWith(".java")) {
            files << file
        }
    }
    files.collect { it.text }
}

def assertNoGenericMapStructConverterInjection = { path ->
    javaFileTexts(path).each { text ->
        assert !text.contains("import io.github.linpeilie.Converter;")
        assert !text.contains("private final Converter converter;")
        assert !text.contains('@Qualifier("converter")')
    }
}
```

- [ ] **Step 2: Add light guard assertions**

In `egon-cola-archetypes/egon-cola-archetype-light/src/test/resources/projects/basic/verify.groovy`, replace the current assertions that require `io.github.linpeilie.Converter` in adapter converter files with these assertions:

```groovy
assertNoGenericMapStructConverterInjection("src/main/java/it/pkg/adapter/convertor")
assertNoGenericMapStructConverterInjection("src/main/java/it/pkg/infrastructure/repo")

assertFile("src/main/java/it/pkg/adapter/convertor/StudentAdapterMapper.java")
assertFile("src/main/java/it/pkg/adapter/convertor/CourseAdapterMapper.java")
assertFile("src/main/java/it/pkg/infrastructure/repo/student/converter/StudentPoMapper.java")
assertFile("src/main/java/it/pkg/infrastructure/repo/student/converter/StudentDomainMapper.java")
assertFile("src/main/java/it/pkg/infrastructure/repo/student/converter/StudentDomainFactory.java")
assertFile("src/main/java/it/pkg/infrastructure/repo/teaching/converter/CoursePoMapper.java")
assertFile("src/main/java/it/pkg/infrastructure/repo/teaching/converter/CourseDomainMapper.java")
assertFile("src/main/java/it/pkg/infrastructure/repo/teaching/converter/CourseDomainFactory.java")
assertFile("src/main/java/it/pkg/domain/common/Page.java")
assertFile("src/main/java/it/pkg/facade/dto/PageResponse.java")

assertFile("src/main/java/it/pkg/adapter/convertor/StudentAdapterMapper.java").text.contains("BaseMapper<Student, StudentDTO>")
assertFile("src/main/java/it/pkg/adapter/convertor/CourseAdapterMapper.java").text.contains("BaseMapper<Course, CourseDTO>")
assertFile("src/main/java/it/pkg/infrastructure/repo/student/converter/StudentPoMapper.java").text.contains("BaseMapper<Student, StudentPo>")
assertFile("src/main/java/it/pkg/infrastructure/repo/teaching/converter/CoursePoMapper.java").text.contains("BaseMapper<Course, CoursePo>")

studentManageText = assertFile("src/main/java/it/pkg/application/manage/student/StudentManage.java").text
assert studentManageText.contains("Page<Student> getPage(int currentPage, int pageSize)")
assert studentManageText.contains("import it.pkg.domain.common.Page;")

def studentRepositoryText = assertFile("src/main/java/it/pkg/domain/student/repos/StudentRepository.java").text
assert studentRepositoryText.contains("Page<Student> findPage(int currentPage, int pageSize)")
assert studentRepositoryText.contains("import it.pkg.domain.common.Page;")

def studentControllerText = assertFile("src/main/java/it/pkg/adapter/controller/student/StudentController.java").text
assert studentControllerText.contains("PageResponse<StudentDTO> getPage")
assert studentControllerText.contains("studentAdapterConverter.toPageResponse(studentManage.getPage(currentPage, pageSize))")

def studentFacadeText = assertFile("src/main/java/it/pkg/facade/api/StudentManagementFacade.java").text
assert studentFacadeText.contains("PageResponse<StudentDTO> getStudents(int currentPage, int pageSize)")

def lightFacadeImplTextAfterPage = assertFile("src/main/java/it/pkg/adapter/facade/impl/StudentManagementFacadeImpl.java").text
assert lightFacadeImplTextAfterPage.contains("PageResponse<StudentDTO> getStudents")
assert lightFacadeImplTextAfterPage.contains("studentAdapterConverter.toPageResponse(studentManage.getPage(currentPage, pageSize))")
```

Keep the existing assertions that verify no application view files exist and no application package imports facade DTOs.

- [ ] **Step 3: Add web guard assertions**

In `egon-cola-archetypes/egon-cola-archetype-web/src/test/resources/projects/basic/verify.groovy`, add these assertions near the existing converter/application/facade assertions:

```groovy
assertNoGenericMapStructConverterInjection("student-management-organization-adapter/src/main/java/it/pkg/adapter/convertor")
assertNoGenericMapStructConverterInjection("student-management-organization-infrastructure/src/main/java/it/pkg/infrastructure/repo")

assertFile("student-management-organization-adapter/src/main/java/it/pkg/adapter/convertor/UserAdapterMapper.java")
assertFile("student-management-organization-adapter/src/main/java/it/pkg/adapter/convertor/SchoolClassAdapterMapper.java")
assertFile("student-management-organization-infrastructure/src/main/java/it/pkg/infrastructure/repo/user/converter/UserPoMapper.java")
assertFile("student-management-organization-infrastructure/src/main/java/it/pkg/infrastructure/repo/user/converter/UserDomainMapper.java")
assertFile("student-management-organization-infrastructure/src/main/java/it/pkg/infrastructure/repo/user/converter/UserDomainFactory.java")
assertFile("student-management-organization-infrastructure/src/main/java/it/pkg/infrastructure/repo/teaching/converter/SchoolClassPoMapper.java")
assertFile("student-management-organization-infrastructure/src/main/java/it/pkg/infrastructure/repo/teaching/converter/SchoolClassDomainMapper.java")
assertFile("student-management-organization-infrastructure/src/main/java/it/pkg/infrastructure/repo/teaching/converter/SchoolClassDomainFactory.java")
assertFile("student-management-organization-domain/src/main/java/it/pkg/domain/common/Page.java")
assertFile("student-management-organization-facade/src/main/java/it/pkg/facade/dto/PageResponse.java")

assertFile("student-management-organization-adapter/src/main/java/it/pkg/adapter/convertor/UserAdapterMapper.java").text.contains("BaseMapper<User, UserDTO>")
assertFile("student-management-organization-adapter/src/main/java/it/pkg/adapter/convertor/SchoolClassAdapterMapper.java").text.contains("BaseMapper<SchoolClass, SchoolClassDTO>")
assertFile("student-management-organization-infrastructure/src/main/java/it/pkg/infrastructure/repo/user/converter/UserPoMapper.java").text.contains("BaseMapper<User, UserPo>")
assertFile("student-management-organization-infrastructure/src/main/java/it/pkg/infrastructure/repo/teaching/converter/SchoolClassPoMapper.java").text.contains("BaseMapper<SchoolClass, SchoolClassPo>")

def userManageTextAfterPage = assertFile("student-management-organization-application/src/main/java/it/pkg/application/manage/user/UserManage.java").text
assert userManageTextAfterPage.contains("Page<User> getPage(int currentPage, int pageSize)")
assert userManageTextAfterPage.contains("import it.pkg.domain.common.Page;")

def userRepositoryText = assertFile("student-management-organization-domain/src/main/java/it/pkg/domain/repos/user/UserRepository.java").text
assert userRepositoryText.contains("Page<User> findPage(int currentPage, int pageSize)")
assert userRepositoryText.contains("import it.pkg.domain.common.Page;")

def userControllerText = assertFile("student-management-organization-adapter/src/main/java/it/pkg/adapter/controller/user/UserController.java").text
assert userControllerText.contains("PageResponse<UserDTO> getPage")
assert userControllerText.contains("userAdapterConverter.toPageResponse(userManage.getPage(currentPage, pageSize))")

def userFacadeText = assertFile("student-management-organization-facade/src/main/java/it/pkg/facade/user/UserFacade.java").text
assert userFacadeText.contains("PageResponse<UserDTO> getUsers(int currentPage, int pageSize)")

def userFacadeImplTextAfterPage = assertFile("student-management-organization-adapter/src/main/java/it/pkg/adapter/facade/user/UserFacadeImpl.java").text
assert userFacadeImplTextAfterPage.contains("PageResponse<UserDTO> getUsers")
assert userFacadeImplTextAfterPage.contains("userAdapterConverter.toPageResponse(userManage.getPage(currentPage, pageSize))")
```

- [ ] **Step 4: Add service guard assertions**

In `egon-cola-archetypes/egon-cola-archetype-service/src/test/resources/projects/basic/verify.groovy`, add these assertions near the existing converter/application/facade assertions:

```groovy
assertNoGenericMapStructConverterInjection("student-management-evaluation-adapter/src/main/java/it/pkg/adapter/convertor")
assertNoGenericMapStructConverterInjection("student-management-evaluation-infrastructure/src/main/java/it/pkg/infrastructure/repo")

assertFile("student-management-evaluation-adapter/src/main/java/it/pkg/adapter/convertor/CourseAdapterMapper.java")
assertFile("student-management-evaluation-adapter/src/main/java/it/pkg/adapter/convertor/ExamResultAdapterMapper.java")
assertFile("student-management-evaluation-infrastructure/src/main/java/it/pkg/infrastructure/repo/course/converter/CoursePoMapper.java")
assertFile("student-management-evaluation-infrastructure/src/main/java/it/pkg/infrastructure/repo/examing/converter/ExamResultPoMapper.java")
assertFile("student-management-evaluation-domain/src/main/java/it/pkg/domain/common/Page.java")
assertFile("student-management-evaluation-facade/src/main/java/it/pkg/facade/dto/PageResponse.java")

assertFile("student-management-evaluation-adapter/src/main/java/it/pkg/adapter/convertor/CourseAdapterMapper.java").text.contains("BaseMapper<Course, CourseDTO>")
assertFile("student-management-evaluation-adapter/src/main/java/it/pkg/adapter/convertor/ExamResultAdapterMapper.java").text.contains("BaseMapper<ExamResult, ExamResultDTO>")
assertFile("student-management-evaluation-infrastructure/src/main/java/it/pkg/infrastructure/repo/course/converter/CoursePoMapper.java").text.contains("BaseMapper<Course, CoursePo>")
assertFile("student-management-evaluation-infrastructure/src/main/java/it/pkg/infrastructure/repo/examing/converter/ExamResultPoMapper.java").text.contains("BaseMapper<ExamResult, ExamResultPo>")

def courseManageTextAfterPage = assertFile("student-management-evaluation-application/src/main/java/it/pkg/application/manage/course/CourseManage.java").text
assert courseManageTextAfterPage.contains("Page<Course> getPage(int currentPage, int pageSize)")
assert courseManageTextAfterPage.contains("import it.pkg.domain.common.Page;")

def courseRepositoryText = assertFile("student-management-evaluation-domain/src/main/java/it/pkg/domain/repos/course/CourseRepository.java").text
assert courseRepositoryText.contains("Page<Course> findPage(int currentPage, int pageSize)")
assert courseRepositoryText.contains("import it.pkg.domain.common.Page;")

def courseFacadeTextAfterPage = assertFile("student-management-evaluation-facade/src/main/java/it/pkg/facade/api/CourseFacade.java").text
assert courseFacadeTextAfterPage.contains("SingleResponse<PageResponse<CourseDTO>> getCourses(int currentPage, int pageSize)")

def courseFacadeImplTextAfterPage = assertFile("student-management-evaluation-adapter/src/main/java/it/pkg/adapter/facade/impl/CourseFacadeImpl.java").text
assert courseFacadeImplTextAfterPage.contains("SingleResponse<PageResponse<CourseDTO>> getCourses")
assert courseFacadeImplTextAfterPage.contains("SingleResponse.of(courseAdapterConvertor.toPageResponse(courseManage.getPage(currentPage, pageSize)))")
```

Keep the existing service pure-service assertions for forbidden paths and missing web starters.

- [ ] **Step 5: Run light integration test and confirm guard fails**

Run:

```bash
./mvnw -B -ntp -pl egon-cola-archetypes/egon-cola-archetype-light -am clean integration-test
```

Expected: `BUILD FAILURE` in `verify.groovy`, with failures about missing mapper files or generic `Converter` injection still present.

- [ ] **Step 6: Run web integration test and confirm guard fails**

Run:

```bash
./mvnw -B -ntp -pl egon-cola-archetypes/egon-cola-archetype-web -am clean integration-test
```

Expected: `BUILD FAILURE` in `verify.groovy`, with failures about missing mapper/page files or generic `Converter` injection still present.

- [ ] **Step 7: Run service integration test and confirm guard fails**

Run:

```bash
./mvnw -B -ntp -pl egon-cola-archetypes/egon-cola-archetype-service -am clean integration-test
```

Expected: `BUILD FAILURE` in `verify.groovy`, with failures about missing mapper/page files or generic `Converter` injection still present.

- [ ] **Step 8: Commit failing guardrails**

```bash
git add -- \
  egon-cola-archetypes/egon-cola-archetype-light/src/test/resources/projects/basic/verify.groovy \
  egon-cola-archetypes/egon-cola-archetype-web/src/test/resources/projects/basic/verify.groovy \
  egon-cola-archetypes/egon-cola-archetype-service/src/test/resources/projects/basic/verify.groovy
git commit -m "test(archetype): guard mapper pagination followup"
```

---

### Task 2: Implement Light Archetype Mapper Components and Student Pagination

**Files:**
- Create: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java/domain/common/Page.java`
- Create: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java/facade/dto/PageResponse.java`
- Create: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java/adapter/convertor/StudentAdapterMapper.java`
- Create: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java/adapter/convertor/CourseAdapterMapper.java`
- Create: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java/infrastructure/repo/student/converter/StudentPoMapper.java`
- Create: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java/infrastructure/repo/student/converter/StudentDomainMapper.java`
- Create: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java/infrastructure/repo/student/converter/StudentDomainFactory.java`
- Create: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java/infrastructure/repo/teaching/converter/CoursePoMapper.java`
- Create: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java/infrastructure/repo/teaching/converter/CourseDomainMapper.java`
- Create: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java/infrastructure/repo/teaching/converter/CourseDomainFactory.java`
- Modify: `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/META-INF/maven/archetype-metadata.xml`
- Modify: light adapter converters, infrastructure converters, student repository, student manage, controller, facade, tests, README, verify.

- [ ] **Step 1: Create the light internal page model**

Create `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java/domain/common/Page.java`:

```java
package ${package}.domain.common;

import java.util.List;

public record Page<T>(
        List<T> records,
        int currentPage,
        int totalPages,
        int pageSize,
        long totalCount
) {
    public static <T> Page<T> of(List<T> records, int currentPage, int totalPages, int pageSize, long totalCount) {
        return new Page<>(records, currentPage, totalPages, pageSize, totalCount);
    }
}
```

- [ ] **Step 2: Create the light external page response DTO**

Create `egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java/facade/dto/PageResponse.java`:

```java
package ${package}.facade.dto;

import java.io.Serializable;
import java.util.List;

public record PageResponse<T>(
        List<T> records,
        int currentPage,
        int totalPages,
        int pageSize,
        long totalCount
) implements Serializable {
    public static <T> PageResponse<T> of(List<T> records, int currentPage, int totalPages, int pageSize, long totalCount) {
        return new PageResponse<>(records, currentPage, totalPages, pageSize, totalCount);
    }
}
```

- [ ] **Step 3: Create light adapter mapper files**

Create `StudentAdapterMapper.java`:

```java
package ${package}.adapter.convertor;

import ${package}.domain.student.model.Student;
import ${package}.facade.dto.StudentDTO;
import io.github.linpeilie.BaseMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface StudentAdapterMapper extends BaseMapper<Student, StudentDTO> {
    @Override
    @Mapping(target = "status", ignore = true)
    StudentDTO convert(Student student);
}
```

Create `CourseAdapterMapper.java`:

```java
package ${package}.adapter.convertor;

import ${package}.domain.teaching.model.Course;
import ${package}.facade.dto.CourseDTO;
import io.github.linpeilie.BaseMapper;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CourseAdapterMapper extends BaseMapper<Course, CourseDTO> {
}
```

- [ ] **Step 4: Update light adapter converter wrappers**

Replace `StudentAdapterConverter.java` with:

```java
package ${package}.adapter.convertor;

import ${package}.domain.common.Page;
import ${package}.domain.student.model.Student;
import ${package}.facade.dto.PageResponse;
import ${package}.facade.dto.StudentDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component("studentAdapterConverter")
@RequiredArgsConstructor
public class StudentAdapterConverter {
    @Qualifier("studentAdapterMapperImpl")
    private final StudentAdapterMapper studentAdapterMapper;

    public StudentDTO toDto(Student student) {
        StudentDTO dto = studentAdapterMapper.convert(student);
        dto.setStatus(student.getStatus().name());
        return dto;
    }

    public PageResponse<StudentDTO> toPageResponse(Page<Student> page) {
        return PageResponse.of(
                page.records().stream().map(this::toDto).toList(),
                page.currentPage(),
                page.totalPages(),
                page.pageSize(),
                page.totalCount());
    }
}
```

Replace `CourseAdapterConverter.java` with:

```java
package ${package}.adapter.convertor;

import ${package}.domain.teaching.model.Course;
import ${package}.facade.dto.CourseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component("courseAdapterConverter")
@RequiredArgsConstructor
public class CourseAdapterConverter {
    @Qualifier("courseAdapterMapperImpl")
    private final CourseAdapterMapper courseAdapterMapper;

    public CourseDTO toDto(Course course) {
        return courseAdapterMapper.convert(course);
    }
}
```

- [ ] **Step 5: Create light infrastructure mapper files**

Create `StudentPoMapper.java`:

```java
package ${package}.infrastructure.repo.student.converter;

import ${package}.domain.student.model.Student;
import ${package}.infrastructure.repo.student.po.StudentPo;
import io.github.linpeilie.BaseMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface StudentPoMapper extends BaseMapper<Student, StudentPo> {
    @Override
    @Mapping(target = "createdAt", ignore = true)
    StudentPo convert(Student student);
}
```

Create `StudentDomainMapper.java`:

```java
package ${package}.infrastructure.repo.student.converter;

import ${package}.domain.student.model.Student;
import ${package}.infrastructure.repo.student.po.StudentPo;
import io.github.linpeilie.BaseMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = StudentDomainFactory.class)
public interface StudentDomainMapper extends BaseMapper<StudentPo, Student> {
    @Override
    @Mapping(target = "courseIds", ignore = true)
    Student convert(StudentPo studentPo);
}
```

Create `StudentDomainFactory.java`:

```java
package ${package}.infrastructure.repo.student.converter;

import ${package}.domain.student.model.Student;
import ${package}.domain.student.model.StudentStatus;
import ${package}.infrastructure.repo.student.po.StudentPo;
import org.mapstruct.ObjectFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component("studentDomainFactory")
public class StudentDomainFactory {
    @ObjectFactory
    public Student create(StudentPo studentPo) {
        return Student.restore(studentPo.getId(), studentPo.getName(), studentPo.getEmail(), StudentStatus.valueOf(studentPo.getStatus()), List.of());
    }
}
```

Create `CoursePoMapper.java`:

```java
package ${package}.infrastructure.repo.teaching.converter;

import ${package}.domain.teaching.model.Course;
import ${package}.infrastructure.repo.teaching.po.CoursePo;
import io.github.linpeilie.BaseMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CoursePoMapper extends BaseMapper<Course, CoursePo> {
    @Override
    @Mapping(target = "createdAt", ignore = true)
    CoursePo convert(Course course);
}
```

Create `CourseDomainMapper.java`:

```java
package ${package}.infrastructure.repo.teaching.converter;

import ${package}.domain.teaching.model.Course;
import ${package}.infrastructure.repo.teaching.po.CoursePo;
import io.github.linpeilie.BaseMapper;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = CourseDomainFactory.class)
public interface CourseDomainMapper extends BaseMapper<CoursePo, Course> {
}
```

Create `CourseDomainFactory.java`:

```java
package ${package}.infrastructure.repo.teaching.converter;

import ${package}.domain.teaching.model.Course;
import ${package}.infrastructure.repo.teaching.po.CoursePo;
import org.mapstruct.ObjectFactory;
import org.springframework.stereotype.Component;

@Component("courseDomainFactory")
public class CourseDomainFactory {
    @ObjectFactory
    public Course create(CoursePo coursePo) {
        return Course.create(coursePo.getId(), coursePo.getName(), coursePo.getDescription());
    }
}
```

- [ ] **Step 6: Update light infrastructure converter wrappers**

Replace `StudentPoConverter.java` with:

```java
package ${package}.infrastructure.repo.student.converter;

import ${package}.domain.student.model.Student;
import ${package}.domain.student.model.StudentStatus;
import ${package}.infrastructure.repo.student.po.StudentCoursePo;
import ${package}.infrastructure.repo.student.po.StudentPo;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component("studentPoConverter")
@RequiredArgsConstructor
public class StudentPoConverter {
    @Qualifier("studentPoMapperImpl")
    private final StudentPoMapper studentPoMapper;

    @Qualifier("studentDomainMapperImpl")
    private final StudentDomainMapper studentDomainMapper;

    public StudentPo toPo(Student student) {
        StudentPo studentPo = studentPoMapper.convert(student);
        return new StudentPo(studentPo.getId(), studentPo.getName(), studentPo.getEmail(), student.getStatus().name(), LocalDateTime.now());
    }

    public Student toDomain(StudentPo studentPo, List<StudentCoursePo> coursePos) {
        List<String> courseIds = coursePos.stream()
                .map(StudentCoursePo::getCourseId)
                .toList();
        Student student = studentDomainMapper.convert(studentPo);
        return Student.restore(student.getId(), student.getName(), student.getEmail(), student.getStatus(), courseIds);
    }
}
```

Replace `CoursePoConverter.java` with:

```java
package ${package}.infrastructure.repo.teaching.converter;

import ${package}.domain.teaching.model.Course;
import ${package}.infrastructure.repo.teaching.po.CoursePo;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component("coursePoConverter")
@RequiredArgsConstructor
public class CoursePoConverter {
    @Qualifier("coursePoMapperImpl")
    private final CoursePoMapper coursePoMapper;

    @Qualifier("courseDomainMapperImpl")
    private final CourseDomainMapper courseDomainMapper;

    public CoursePo toPo(Course course) {
        CoursePo coursePo = coursePoMapper.convert(course);
        return new CoursePo(coursePo.getId(), coursePo.getName(), coursePo.getDescription(), LocalDateTime.now());
    }

    public Course toDomain(CoursePo coursePo) {
        return courseDomainMapper.convert(coursePo);
    }
}
```

- [ ] **Step 7: Add light repository pagination**

Modify `StudentRepository.java`:

```java
package ${package}.domain.student.repos;

import ${package}.domain.common.Page;
import ${package}.domain.student.model.Student;

import java.util.Optional;

public interface StudentRepository {
    Student save(Student student);

    Optional<Student> findById(String studentId);

    Page<Student> findPage(int currentPage, int pageSize);

    boolean existsByEmail(String email);
}
```

Modify `StudentRepositoryImpl.java` to import `Page`, `PageRequest`, and `Pageable`, then add this method:

```java
@Override
public Page<Student> findPage(int currentPage, int pageSize) {
    Pageable pageable = PageRequest.of(Math.max(currentPage, 1) - 1, pageSize);
    org.springframework.data.domain.Page<StudentPo> page = studentJpaRepository.findAll(pageable);
    return Page.of(
            page.getContent().stream()
                    .map(studentPo -> studentPoConverter.toDomain(
                            studentPo,
                            studentCourseJpaRepository.findByStudentId(studentPo.getId())))
                    .toList(),
            currentPage,
            page.getTotalPages(),
            pageSize,
            page.getTotalElements());
}
```

- [ ] **Step 8: Add light application pagination**

Modify `StudentManage.java`:

```java
package ${package}.application.manage.student;

import ${package}.domain.common.Page;
import ${package}.domain.student.model.Student;

public interface StudentManage {
    Student register(String name, String email);

    Student getById(String studentId);

    Page<Student> getPage(int currentPage, int pageSize);
}
```

Add this method to `application/manage/student/impl/StudentManageImpl.java`:

```java
@Override
@Transactional(readOnly = true)
public Page<Student> getPage(int currentPage, int pageSize) {
    return studentRepository.findPage(currentPage, pageSize);
}
```

Also add `import ${package}.domain.common.Page;`.

- [ ] **Step 9: Expose light pagination through HTTP and Dubbo**

Modify `StudentController.java` imports:

```java
import ${package}.facade.dto.PageResponse;
import org.springframework.web.bind.annotation.RequestParam;
```

Add this method:

```java
@GetMapping
public SingleResponse<PageResponse<StudentDTO>> getPage(
        @RequestParam(defaultValue = "1") int currentPage,
        @RequestParam(defaultValue = "10") int pageSize) {
    return SingleResponse.of(studentAdapterConverter.toPageResponse(studentManage.getPage(currentPage, pageSize)));
}
```

Modify `StudentManagementFacade.java` imports and methods:

```java
import ${package}.facade.dto.PageResponse;

PageResponse<StudentDTO> getStudents(int currentPage, int pageSize);
```

Add this method to `StudentManagementFacadeImpl.java`:

```java
@Override
public PageResponse<StudentDTO> getStudents(int currentPage, int pageSize) {
    return studentAdapterConverter.toPageResponse(studentManage.getPage(currentPage, pageSize));
}
```

Also import `PageResponse`.

- [ ] **Step 10: Update light tests**

Modify `StudentManagementFlowTest.java` imports:

```java
import ${package}.domain.common.Page;
import ${package}.facade.dto.PageResponse;

import java.util.UUID;
```

Add a new test method named `get_student_page_returns_domain_page_and_facade_page`:

```java
@Test
void get_student_page_returns_domain_page_and_facade_page() {
    String suffix = UUID.randomUUID().toString();
    String marioEmail = "mario-" + suffix + "@example.com";
    String luigiEmail = "luigi-" + suffix + "@example.com";

    studentManage.register("Mario", marioEmail);
    studentManagementFacade.registerStudent(new RegisterStudentRequest("Luigi", luigiEmail));

    Page<Student> studentPage = studentManage.getPage(1, 10);
    assertThat(studentPage.records()).extracting(Student::getEmail)
            .contains(marioEmail, luigiEmail);
    assertThat(studentPage.currentPage()).isEqualTo(1);
    assertThat(studentPage.pageSize()).isEqualTo(10);
    assertThat(studentPage.totalCount()).isGreaterThanOrEqualTo(2);

    PageResponse<StudentDTO> facadePage = studentManagementFacade.getStudents(1, 10);
    assertThat(facadePage.records()).extracting(StudentDTO::getEmail)
            .contains(marioEmail, luigiEmail);
    assertThat(facadePage.currentPage()).isEqualTo(1);
    assertThat(facadePage.pageSize()).isEqualTo(10);
    assertThat(facadePage.totalCount()).isGreaterThanOrEqualTo(2);
}
```

- [ ] **Step 11: Update light archetype metadata and README**

The light archetype metadata already includes `src/main/java/**/*.java`, so no explicit mapper include is needed. Do not add narrow includes for individual Java files.

In light `README.md`, add one concise bullet under architecture or guardrail wording:

```markdown
- Application pagination returns `Page<DomainModel>`; adapter converts it to external `PageResponse<DTO>` for HTTP and Dubbo.
- Converter wrappers use concrete MapStruct Plus mapper components, not the generic `Converter` bean.
```

- [ ] **Step 12: Run light validation**

Run:

```bash
./mvnw -B -ntp -pl egon-cola-archetypes/egon-cola-archetype-light -am clean integration-test
```

Expected: `BUILD SUCCESS`, generated light project tests pass, and `verify.groovy` passes.

- [ ] **Step 13: Commit light implementation**

```bash
git add -- \
  egon-cola-archetypes/egon-cola-archetype-light/src/main/resources \
  egon-cola-archetypes/egon-cola-archetype-light/src/test/resources/projects/basic
git commit -m "feat(archetype-light): add mapper pagination followup"
```

---

### Task 3: Implement Web Archetype Mapper Components and User Pagination

**Files:**
- Create: `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-domain/src/main/java/domain/common/Page.java`
- Create: `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-facade/src/main/java/facade/dto/PageResponse.java`
- Create: `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-adapter/src/main/java/adapter/convertor/UserAdapterMapper.java`
- Create: `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-adapter/src/main/java/adapter/convertor/SchoolClassAdapterMapper.java`
- Create: `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/main/java/infrastructure/repo/user/converter/UserPoMapper.java`
- Create: `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/main/java/infrastructure/repo/user/converter/UserDomainMapper.java`
- Create: `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/main/java/infrastructure/repo/user/converter/UserDomainFactory.java`
- Create: `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/main/java/infrastructure/repo/teaching/converter/SchoolClassPoMapper.java`
- Create: `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/main/java/infrastructure/repo/teaching/converter/SchoolClassDomainMapper.java`
- Create: `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/main/java/infrastructure/repo/teaching/converter/SchoolClassDomainFactory.java`
- Modify: `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-adapter/src/main/java/adapter/convertor/UserAdapterConverter.java`
- Modify: `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-adapter/src/main/java/adapter/convertor/SchoolClassAdapterConverter.java`
- Modify: `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-domain/src/main/java/domain/repos/user/UserRepository.java`
- Modify: `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-application/src/main/java/application/manage/user/UserManage.java`
- Modify: `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-application/src/main/java/application/manage/user/impl/UserManageImpl.java`
- Modify: `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/main/java/infrastructure/repo/user/impl/UserRepositoryImpl.java`
- Modify: `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-adapter/src/main/java/adapter/controller/user/UserController.java`
- Modify: `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-facade/src/main/java/facade/user/UserFacade.java`
- Modify: `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-adapter/src/main/java/adapter/facade/user/UserFacadeImpl.java`
- Modify: `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-starter/src/test/java/starter/OrganizationFlowTest.java`
- Modify: `egon-cola-archetypes/egon-cola-archetype-web/src/test/resources/projects/basic/verify.groovy`
- Modify: `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/README.md`

- [ ] **Step 1: Create web internal page model and external page response**

Create `__rootArtifactId__-domain/src/main/java/domain/common/Page.java`:

```java
package ${package}.domain.common;

import java.util.List;

public record Page<T>(
        List<T> records,
        int currentPage,
        int totalPages,
        int pageSize,
        long totalCount
) {
    public static <T> Page<T> of(List<T> records, int currentPage, int totalPages, int pageSize, long totalCount) {
        return new Page<>(records, currentPage, totalPages, pageSize, totalCount);
    }
}
```

Create `__rootArtifactId__-facade/src/main/java/facade/dto/PageResponse.java`:

```java
package ${package}.facade.dto;

import java.io.Serializable;
import java.util.List;

public record PageResponse<T>(
        List<T> records,
        int currentPage,
        int totalPages,
        int pageSize,
        long totalCount
) implements Serializable {
    public static <T> PageResponse<T> of(List<T> records, int currentPage, int totalPages, int pageSize, long totalCount) {
        return new PageResponse<>(records, currentPage, totalPages, pageSize, totalCount);
    }
}
```

- [ ] **Step 2: Create web adapter mapper files**

Create `UserAdapterMapper.java`:

```java
package ${package}.adapter.convertor;

import ${package}.domain.entities.user.User;
import ${package}.facade.dto.user.UserDTO;
import io.github.linpeilie.BaseMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserAdapterMapper extends BaseMapper<User, UserDTO> {
    @Override
    @Mapping(target = "status", ignore = true)
    UserDTO convert(User user);
}
```

Create `SchoolClassAdapterMapper.java`:

```java
package ${package}.adapter.convertor;

import ${package}.domain.entities.teaching.SchoolClass;
import ${package}.facade.dto.teaching.SchoolClassDTO;
import io.github.linpeilie.BaseMapper;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface SchoolClassAdapterMapper extends BaseMapper<SchoolClass, SchoolClassDTO> {
}
```

- [ ] **Step 3: Update web adapter converter wrappers**

Replace `UserAdapterConverter.java` with this complete wrapper.

```java
package ${package}.adapter.convertor;

import ${package}.domain.common.Page;
import ${package}.domain.entities.user.User;
import ${package}.facade.dto.PageResponse;
import ${package}.facade.dto.user.UserDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component("userAdapterConverter")
@RequiredArgsConstructor
public class UserAdapterConverter {
    @Qualifier("userAdapterMapperImpl")
    private final UserAdapterMapper userAdapterMapper;

    public UserDTO toDto(User user) {
        UserDTO dto = userAdapterMapper.convert(user);
        dto.setStatus(user.getStatus().name());
        return dto;
    }

    public PageResponse<UserDTO> toPageResponse(Page<User> page) {
        return PageResponse.of(
                page.records().stream().map(this::toDto).toList(),
                page.currentPage(),
                page.totalPages(),
                page.pageSize(),
                page.totalCount());
    }
}
```

Replace `SchoolClassAdapterConverter.java` with:

```java
package ${package}.adapter.convertor;

import ${package}.domain.entities.teaching.SchoolClass;
import ${package}.facade.dto.teaching.SchoolClassDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component("schoolClassAdapterConverter")
@RequiredArgsConstructor
public class SchoolClassAdapterConverter {
    @Qualifier("schoolClassAdapterMapperImpl")
    private final SchoolClassAdapterMapper schoolClassAdapterMapper;

    public SchoolClassDTO toDto(SchoolClass schoolClass) {
        return schoolClassAdapterMapper.convert(schoolClass);
    }
}
```

- [ ] **Step 4: Create web infrastructure mapper files and update wrappers**

Create `UserPoMapper.java` beside `UserPoConverter.java`:

```java
package ${package}.infrastructure.repo.user.converter;

import ${package}.domain.entities.user.User;
import ${package}.infrastructure.repo.user.po.UserPo;
import io.github.linpeilie.BaseMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserPoMapper extends BaseMapper<User, UserPo> {
    @Override
    @Mapping(target = "createdAt", ignore = true)
    UserPo convert(User user);
}
```

Create `UserDomainMapper.java`:

```java
package ${package}.infrastructure.repo.user.converter;

import ${package}.domain.entities.user.User;
import ${package}.infrastructure.repo.user.po.UserPo;
import io.github.linpeilie.BaseMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = UserDomainFactory.class)
public interface UserDomainMapper extends BaseMapper<UserPo, User> {
    @Override
    @Mapping(target = "schoolClassIds", ignore = true)
    User convert(UserPo userPo);
}
```

Create `UserDomainFactory.java`:

```java
package ${package}.infrastructure.repo.user.converter;

import ${package}.domain.entities.user.User;
import ${package}.domain.enums.UserStatus;
import ${package}.infrastructure.repo.user.po.UserPo;
import org.mapstruct.ObjectFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component("userDomainFactory")
public class UserDomainFactory {
    @ObjectFactory
    public User create(UserPo userPo) {
        return User.restore(userPo.getId(), userPo.getName(), userPo.getEmail(), UserStatus.valueOf(userPo.getStatus()), List.of());
    }
}
```

Create `SchoolClassPoMapper.java`:

```java
package ${package}.infrastructure.repo.teaching.converter;

import ${package}.domain.entities.teaching.SchoolClass;
import ${package}.infrastructure.repo.teaching.po.SchoolClassPo;
import io.github.linpeilie.BaseMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface SchoolClassPoMapper extends BaseMapper<SchoolClass, SchoolClassPo> {
    @Override
    @Mapping(target = "createdAt", ignore = true)
    SchoolClassPo convert(SchoolClass schoolClass);
}
```

Create `SchoolClassDomainMapper.java`:

```java
package ${package}.infrastructure.repo.teaching.converter;

import ${package}.domain.entities.teaching.SchoolClass;
import ${package}.infrastructure.repo.teaching.po.SchoolClassPo;
import io.github.linpeilie.BaseMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = SchoolClassDomainFactory.class)
public interface SchoolClassDomainMapper extends BaseMapper<SchoolClassPo, SchoolClass> {
    @Override
    @Mapping(target = "userIds", ignore = true)
    SchoolClass convert(SchoolClassPo schoolClassPo);
}
```

Create `SchoolClassDomainFactory.java`:

```java
package ${package}.infrastructure.repo.teaching.converter;

import ${package}.domain.entities.teaching.SchoolClass;
import ${package}.infrastructure.repo.teaching.po.SchoolClassPo;
import org.mapstruct.ObjectFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component("schoolClassDomainFactory")
public class SchoolClassDomainFactory {
    @ObjectFactory
    public SchoolClass create(SchoolClassPo schoolClassPo) {
        return SchoolClass.restore(schoolClassPo.getId(), schoolClassPo.getName(), schoolClassPo.getGradeName(), List.of());
    }
}
```

Replace `UserPoConverter.java` with:

```java
package ${package}.infrastructure.repo.user.converter;

import ${package}.domain.entities.user.User;
import ${package}.domain.enums.UserStatus;
import ${package}.infrastructure.repo.user.po.UserPo;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component("userPoConverter")
@RequiredArgsConstructor
public class UserPoConverter {
    @Qualifier("userPoMapperImpl")
    private final UserPoMapper userPoMapper;

    @Qualifier("userDomainMapperImpl")
    private final UserDomainMapper userDomainMapper;

    public UserPo toPo(User user) {
        UserPo userPo = userPoMapper.convert(user);
        return new UserPo(userPo.getId(), userPo.getName(), userPo.getEmail(), user.getStatus().name(), LocalDateTime.now());
    }

    public User toEntity(UserPo userPo, List<String> schoolClassIds) {
        User user = userDomainMapper.convert(userPo);
        return User.restore(user.getId(), user.getName(), user.getEmail(), user.getStatus(), schoolClassIds);
    }
}
```

Replace `SchoolClassPoConverter.java` with:

```java
package ${package}.infrastructure.repo.teaching.converter;

import ${package}.domain.entities.teaching.SchoolClass;
import ${package}.infrastructure.repo.teaching.po.SchoolClassPo;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component("schoolClassPoConverter")
@RequiredArgsConstructor
public class SchoolClassPoConverter {
    @Qualifier("schoolClassPoMapperImpl")
    private final SchoolClassPoMapper schoolClassPoMapper;

    @Qualifier("schoolClassDomainMapperImpl")
    private final SchoolClassDomainMapper schoolClassDomainMapper;

    public SchoolClassPo toPo(SchoolClass schoolClass) {
        SchoolClassPo schoolClassPo = schoolClassPoMapper.convert(schoolClass);
        return new SchoolClassPo(
                schoolClassPo.getId(),
                schoolClassPo.getName(),
                schoolClassPo.getGradeName(),
                LocalDateTime.now());
    }

    public SchoolClass toEntity(SchoolClassPo schoolClassPo, List<String> userIds) {
        SchoolClass schoolClass = schoolClassDomainMapper.convert(schoolClassPo);
        return SchoolClass.restore(schoolClass.getId(), schoolClass.getName(), schoolClass.getGradeName(), userIds);
    }
}
```

- [ ] **Step 5: Add web user repository pagination**

Modify `domain/repos/user/UserRepository.java` to include:

```java
import ${package}.domain.common.Page;

Page<User> findPage(int currentPage, int pageSize);
```

Modify `infrastructure/repo/user/impl/UserRepositoryImpl.java` to import:

```java
import ${package}.domain.common.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
```

Add this method:

```java
@Override
public Page<User> findPage(int currentPage, int pageSize) {
    Pageable pageable = PageRequest.of(Math.max(currentPage, 1) - 1, pageSize);
    org.springframework.data.domain.Page<UserPo> page = userJpaRepository.findAll(pageable);
    return Page.of(
            page.getContent().stream()
                    .map(this::restore)
                    .toList(),
            currentPage,
            page.getTotalPages(),
            pageSize,
            page.getTotalElements());
}
```

- [ ] **Step 6: Add web application pagination**

Modify `application/manage/user/UserManage.java`:

```java
import ${package}.domain.common.Page;

Page<User> getPage(int currentPage, int pageSize);
```

Add to `application/manage/user/impl/UserManageImpl.java`:

```java
@Override
@Transactional(readOnly = true)
public Page<User> getPage(int currentPage, int pageSize) {
    return userRepository.findPage(currentPage, pageSize);
}
```

- [ ] **Step 7: Expose web pagination through HTTP and Dubbo**

Modify `UserController.java` to import `PageResponse` and `RequestParam`, then add:

```java
@GetMapping
public SingleResponse<PageResponse<UserDTO>> getPage(
        @RequestParam(defaultValue = "1") int currentPage,
        @RequestParam(defaultValue = "10") int pageSize) {
    return SingleResponse.of(userAdapterConverter.toPageResponse(userManage.getPage(currentPage, pageSize)));
}
```

Modify `UserFacade.java`:

```java
import ${package}.facade.dto.PageResponse;

PageResponse<UserDTO> getUsers(int currentPage, int pageSize);
```

Modify `UserFacadeImpl.java`:

```java
@Override
public PageResponse<UserDTO> getUsers(int currentPage, int pageSize) {
    return userAdapterConverter.toPageResponse(userManage.getPage(currentPage, pageSize));
}
```

- [ ] **Step 8: Update web tests**

Modify `OrganizationFlowTest.java` to import:

```java
import ${package}.domain.common.Page;
import ${package}.facade.dto.PageResponse;
import ${package}.facade.dto.user.CreateUserRequest;
import ${package}.facade.dto.user.UserDTO;

import java.util.UUID;
```

Add a new test method named `get_user_page_returns_domain_page_and_facade_page`:

```java
@Test
void get_user_page_returns_domain_page_and_facade_page() {
    String suffix = UUID.randomUUID().toString();
    String marioEmail = "mario-" + suffix + "@example.com";
    String luigiEmail = "luigi-" + suffix + "@example.com";

    userManage.create("Mario", marioEmail);
    userFacade.createUser(new CreateUserRequest("Luigi", luigiEmail));

    Page<User> userPage = userManage.getPage(1, 10);
    assertThat(userPage.records()).extracting(User::getEmail)
            .contains(marioEmail, luigiEmail);
    assertThat(userPage.currentPage()).isEqualTo(1);
    assertThat(userPage.pageSize()).isEqualTo(10);
    assertThat(userPage.totalCount()).isGreaterThanOrEqualTo(2);

    PageResponse<UserDTO> facadePage = userFacade.getUsers(1, 10);
    assertThat(facadePage.records()).extracting(UserDTO::getEmail)
            .contains(marioEmail, luigiEmail);
    assertThat(facadePage.currentPage()).isEqualTo(1);
    assertThat(facadePage.pageSize()).isEqualTo(10);
    assertThat(facadePage.totalCount()).isGreaterThanOrEqualTo(2);
}
```

- [ ] **Step 9: Update web metadata and README**

The web archetype metadata already includes `src/main/java/**/*.java` inside each module, so no explicit mapper include is needed. Do not add narrow includes for individual Java files.

Update web generated README with:

```markdown
- Application pagination returns `Page<DomainModel>`; adapter converts it to external `PageResponse<DTO>` for HTTP and Dubbo.
- Converter wrappers use concrete MapStruct Plus mapper components, not the generic `Converter` bean.
```

- [ ] **Step 10: Run web validation**

Run:

```bash
./mvnw -B -ntp -pl egon-cola-archetypes/egon-cola-archetype-web -am clean integration-test
```

Expected: `BUILD SUCCESS`, generated web project tests pass, and `verify.groovy` passes.

- [ ] **Step 11: Commit web implementation**

```bash
git add -- \
  egon-cola-archetypes/egon-cola-archetype-web/src/main/resources \
  egon-cola-archetypes/egon-cola-archetype-web/src/test/resources/projects/basic
git commit -m "feat(archetype-web): add mapper pagination followup"
```

---

### Task 4: Implement Service Archetype Mapper Components and Course Pagination

**Files:**
- Create: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-domain/src/main/java/domain/common/Page.java`
- Create: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-facade/src/main/java/facade/dto/PageResponse.java`
- Create: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-adapter/src/main/java/adapter/convertor/CourseAdapterMapper.java`
- Create: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-adapter/src/main/java/adapter/convertor/ExamResultAdapterMapper.java`
- Create: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/main/java/infrastructure/repo/course/converter/CoursePoMapper.java`
- Create: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/main/java/infrastructure/repo/course/converter/CourseDomainMapper.java`
- Create: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/main/java/infrastructure/repo/examing/converter/ExamResultPoMapper.java`
- Create: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/main/java/infrastructure/repo/examing/converter/ExamResultDomainMapper.java`
- Modify: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-adapter/src/main/java/adapter/convertor/CourseAdapterConvertor.java`
- Modify: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-adapter/src/main/java/adapter/convertor/ExamResultAdapterConvertor.java`
- Modify: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-domain/src/main/java/domain/repos/course/CourseRepository.java`
- Modify: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-application/src/main/java/application/manage/course/CourseManage.java`
- Modify: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-application/src/main/java/application/manage/course/impl/CourseManageImpl.java`
- Modify: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/main/java/infrastructure/repo/course/impl/CourseRepositoryImpl.java`
- Modify: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-facade/src/main/java/facade/api/CourseFacade.java`
- Modify: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-adapter/src/main/java/adapter/facade/impl/CourseFacadeImpl.java`
- Modify: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-starter/src/test/java/starter/EvaluationFlowTest.java`
- Modify: `egon-cola-archetypes/egon-cola-archetype-service/src/test/resources/projects/basic/verify.groovy`
- Modify: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/README.md`

- [ ] **Step 1: Create service internal page model and external page response**

Create `__rootArtifactId__-domain/src/main/java/domain/common/Page.java`:

```java
#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.domain.common;

import java.util.List;

public record Page<T>(
        List<T> records,
        int currentPage,
        int totalPages,
        int pageSize,
        long totalCount
) {
    public static <T> Page<T> of(List<T> records, int currentPage, int totalPages, int pageSize, long totalCount) {
        return new Page<>(records, currentPage, totalPages, pageSize, totalCount);
    }
}
```

Create `__rootArtifactId__-facade/src/main/java/facade/dto/PageResponse.java`:

```java
#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.facade.dto;

import java.io.Serializable;
import java.util.List;

public record PageResponse<T>(
        List<T> records,
        int currentPage,
        int totalPages,
        int pageSize,
        long totalCount
) implements Serializable {
    public static <T> PageResponse<T> of(List<T> records, int currentPage, int totalPages, int pageSize, long totalCount) {
        return new PageResponse<>(records, currentPage, totalPages, pageSize, totalCount);
    }
}
```

- [ ] **Step 2: Create service adapter mapper files**

Create `CourseAdapterMapper.java`:

```java
#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.adapter.convertor;

import ${package}.domain.entities.course.Course;
import ${package}.facade.dto.course.CourseDTO;
import io.github.linpeilie.BaseMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CourseAdapterMapper extends BaseMapper<Course, CourseDTO> {
    @Override
    @Mapping(target = "status", ignore = true)
    CourseDTO convert(Course course);
}
```

Create `ExamResultAdapterMapper.java`:

```java
#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.adapter.convertor;

import ${package}.domain.entities.examing.ExamResult;
import ${package}.facade.dto.examing.ExamResultDTO;
import io.github.linpeilie.BaseMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ExamResultAdapterMapper extends BaseMapper<ExamResult, ExamResultDTO> {
    @Override
    @Mapping(target = "status", ignore = true)
    ExamResultDTO convert(ExamResult examResult);
}
```

- [ ] **Step 3: Update service adapter converter wrappers**

Replace `CourseAdapterConvertor.java` with:

```java
#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.adapter.convertor;

import ${package}.domain.common.Page;
import ${package}.domain.entities.course.Course;
import ${package}.domain.enums.CourseStatus;
import ${package}.facade.dto.PageResponse;
import ${package}.facade.dto.course.CourseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component("courseAdapterConvertor")
@RequiredArgsConstructor
public class CourseAdapterConvertor {
    @Qualifier("courseAdapterMapperImpl")
    private final CourseAdapterMapper courseAdapterMapper;

    public CourseDTO toDTO(Course course) {
        CourseDTO dto = courseAdapterMapper.convert(course);
        dto.setStatus(toFacadeStatus(course.getStatus()));
        return dto;
    }

    public PageResponse<CourseDTO> toPageResponse(Page<Course> page) {
        return PageResponse.of(
                page.records().stream().map(this::toDTO).toList(),
                page.currentPage(),
                page.totalPages(),
                page.pageSize(),
                page.totalCount());
    }

    private String toFacadeStatus(CourseStatus status) {
        if (CourseStatus.ACTIVE == status) {
            return "ENABLED";
        }
        return status.name();
    }
}
```

Replace `ExamResultAdapterConvertor.java` with:

```java
#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.adapter.convertor;

import ${package}.domain.entities.examing.ExamResult;
import ${package}.domain.enums.ExamResultStatus;
import ${package}.facade.dto.examing.ExamResultDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component("examResultAdapterConvertor")
@RequiredArgsConstructor
public class ExamResultAdapterConvertor {
    @Qualifier("examResultAdapterMapperImpl")
    private final ExamResultAdapterMapper examResultAdapterMapper;

    public ExamResultDTO toDTO(ExamResult examResult) {
        ExamResultDTO dto = examResultAdapterMapper.convert(examResult);
        dto.setStatus(toFacadeStatus(examResult.getStatus(), examResult.getScore()));
        return dto;
    }

    private String toFacadeStatus(ExamResultStatus status, int score) {
        if (ExamResultStatus.RECORDED == status) {
            return score >= 60 ? "PASSED" : "FAILED";
        }
        return status.name();
    }
}
```

- [ ] **Step 4: Create service infrastructure mapper files and update wrappers**

Create `CoursePoMapper.java`:

```java
#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.infrastructure.repo.course.converter;

import ${package}.domain.entities.course.Course;
import ${package}.infrastructure.repo.course.po.CoursePo;
import io.github.linpeilie.BaseMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface CoursePoMapper extends BaseMapper<Course, CoursePo> {

    @Override
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "status", ignore = true)
    CoursePo convert(Course course);

    @Override
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "status", ignore = true)
    CoursePo convert(Course course, @MappingTarget CoursePo coursePo);
}
```

Create `CourseDomainMapper.java`:

```java
#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.infrastructure.repo.course.converter;

import ${package}.domain.entities.course.Course;
import ${package}.infrastructure.repo.course.po.CoursePo;
import io.github.linpeilie.BaseMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface CourseDomainMapper extends BaseMapper<CoursePo, Course> {

    @Override
    @Mapping(target = "status", ignore = true)
    Course convert(CoursePo coursePo);

    @Override
    @Mapping(target = "status", ignore = true)
    Course convert(CoursePo coursePo, @MappingTarget Course course);
}
```

Create `ExamResultPoMapper.java`:

```java
#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.infrastructure.repo.examing.converter;

import ${package}.domain.entities.examing.ExamResult;
import ${package}.infrastructure.repo.examing.po.ExamResultPo;
import io.github.linpeilie.BaseMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface ExamResultPoMapper extends BaseMapper<ExamResult, ExamResultPo> {

    @Override
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "status", ignore = true)
    ExamResultPo convert(ExamResult examResult);

    @Override
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "status", ignore = true)
    ExamResultPo convert(ExamResult examResult, @MappingTarget ExamResultPo examResultPo);
}
```

Create `ExamResultDomainMapper.java`:

```java
#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.infrastructure.repo.examing.converter;

import ${package}.domain.entities.examing.ExamResult;
import ${package}.infrastructure.repo.examing.po.ExamResultPo;
import io.github.linpeilie.BaseMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface ExamResultDomainMapper extends BaseMapper<ExamResultPo, ExamResult> {

    @Override
    @Mapping(target = "status", ignore = true)
    ExamResult convert(ExamResultPo examResultPo);

    @Override
    @Mapping(target = "status", ignore = true)
    ExamResult convert(ExamResultPo examResultPo, @MappingTarget ExamResult examResult);
}
```

Replace `CourseConverter.java` with:

```java
#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.infrastructure.repo.course.converter;

import ${package}.domain.entities.course.Course;
import ${package}.domain.enums.CourseStatus;
import ${package}.infrastructure.repo.course.po.CoursePo;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component("courseConverter")
@RequiredArgsConstructor
public class CourseConverter {

    @Qualifier("coursePoMapperImpl")
    private final CoursePoMapper coursePoMapper;

    @Qualifier("courseDomainMapperImpl")
    private final CourseDomainMapper courseDomainMapper;

    public CoursePo toPo(Course course, LocalDateTime createdAt, LocalDateTime updatedAt) {
        CoursePo coursePo = coursePoMapper.convert(course);
        return new CoursePo(
                coursePo.getId(),
                coursePo.getName(),
                coursePo.getCredit(),
                course.getStatus().name(),
                createdAt,
                updatedAt);
    }

    public Course toDomain(CoursePo coursePo) {
        Course course = courseDomainMapper.convert(coursePo);
        course.setStatus(CourseStatus.valueOf(coursePo.getStatus()));
        return course;
    }
}
```

Replace `ExamResultConverter.java` with:

```java
#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.infrastructure.repo.examing.converter;

import ${package}.domain.entities.examing.ExamResult;
import ${package}.domain.enums.ExamResultStatus;
import ${package}.infrastructure.repo.examing.po.ExamResultPo;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component("examResultConverter")
@RequiredArgsConstructor
public class ExamResultConverter {

    @Qualifier("examResultPoMapperImpl")
    private final ExamResultPoMapper examResultPoMapper;

    @Qualifier("examResultDomainMapperImpl")
    private final ExamResultDomainMapper examResultDomainMapper;

    public ExamResultPo toPo(ExamResult examResult, LocalDateTime createdAt, LocalDateTime updatedAt) {
        ExamResultPo examResultPo = examResultPoMapper.convert(examResult);
        return new ExamResultPo(
                examResultPo.getId(),
                examResultPo.getCourseId(),
                examResultPo.getStudentId(),
                examResultPo.getScore(),
                examResult.getStatus().name(),
                createdAt,
                updatedAt);
    }

    public ExamResult toDomain(ExamResultPo examResultPo) {
        ExamResult examResult = examResultDomainMapper.convert(examResultPo);
        examResult.setStatus(ExamResultStatus.valueOf(examResultPo.getStatus()));
        return examResult;
    }
}
```

- [ ] **Step 5: Add service course repository pagination**

Modify `domain/repos/course/CourseRepository.java`:

```java
import ${package}.domain.common.Page;

Page<Course> findPage(int currentPage, int pageSize);
```

Modify `infrastructure/repo/course/impl/CourseRepositoryImpl.java` to import:

```java
import ${package}.domain.common.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
```

Add this method:

```java
@Override
public Page<Course> findPage(int currentPage, int pageSize) {
    Pageable pageable = PageRequest.of(Math.max(currentPage, 1) - 1, pageSize);
    org.springframework.data.domain.Page<CoursePo> page = courseJpaRepository.findAll(pageable);
    return Page.of(
            page.getContent().stream()
                    .map(courseConverter::toDomain)
                    .toList(),
            currentPage,
            page.getTotalPages(),
            pageSize,
            page.getTotalElements());
}
```

- [ ] **Step 6: Add service application pagination**

Modify `application/manage/course/CourseManage.java`:

```java
import ${package}.domain.common.Page;

Page<Course> getPage(int currentPage, int pageSize);
```

Add to `application/manage/course/impl/CourseManageImpl.java`:

```java
@Override
@Transactional(readOnly = true)
public Page<Course> getPage(int currentPage, int pageSize) {
    return courseRepository.findPage(currentPage, pageSize);
}
```

- [ ] **Step 7: Expose service pagination through Dubbo only**

Modify `facade/api/CourseFacade.java`:

```java
import ${package}.facade.dto.PageResponse;
import ${package}.facade.dto.SingleResponse;

SingleResponse<PageResponse<CourseDTO>> getCourses(int currentPage, int pageSize);
```

Modify `adapter/facade/impl/CourseFacadeImpl.java` imports:

```java
import ${package}.common.exception.BizException;
import ${package}.facade.dto.PageResponse;
import ${package}.facade.dto.SingleResponse;
```

Then add this method:

```java
@Override
public SingleResponse<PageResponse<CourseDTO>> getCourses(int currentPage, int pageSize) {
    try {
        return SingleResponse.of(courseAdapterConvertor.toPageResponse(courseManage.getPage(currentPage, pageSize)));
    } catch (BizException exception) {
        return serviceExceptionHandler.handleSingle(exception);
    } catch (Exception exception) {
        return serviceExceptionHandler.handleSingle(exception);
    }
}
```

Do not add HTTP controllers, Web dependencies, filters, GraphQL, or VO packages.

- [ ] **Step 8: Update service tests**

Modify `EvaluationFlowTest.java` to import:

```java
import ${package}.application.manage.course.CourseManage;
import ${package}.domain.common.Page;
import ${package}.facade.dto.PageResponse;
import ${package}.facade.dto.SingleResponse;
```

Add this field to `EvaluationFlowTest`:

```java
@Autowired
private CourseManage courseManage;
```

Add a new test method named `shouldReturnCoursePageFromApplicationAndFacade`:

```java
@Test
void shouldReturnCoursePageFromApplicationAndFacade() {
    Course firstCourse = courseManage.create("Course-" + UUID.randomUUID(), 2);
    Course secondCourse = courseManage.create("Course-" + UUID.randomUUID(), 3);

    Page<Course> coursePage = courseManage.getPage(1, 10);
    assertThat(coursePage.records()).extracting(Course::getName)
            .contains(firstCourse.getName(), secondCourse.getName());
    assertThat(coursePage.currentPage()).isEqualTo(1);
    assertThat(coursePage.pageSize()).isEqualTo(10);
    assertThat(coursePage.totalCount()).isGreaterThanOrEqualTo(2);

    SingleResponse<PageResponse<CourseDTO>> facadeResponse = courseFacade.getCourses(1, 10);
    assertThat(facadeResponse.isSuccess()).isTrue();

    PageResponse<CourseDTO> facadePage = facadeResponse.getData();
    assertThat(facadePage.records()).extracting(CourseDTO::getName)
            .contains(firstCourse.getName(), secondCourse.getName());
    assertThat(facadePage.currentPage()).isEqualTo(1);
    assertThat(facadePage.pageSize()).isEqualTo(10);
    assertThat(facadePage.totalCount()).isGreaterThanOrEqualTo(2);
}
```

- [ ] **Step 9: Update service metadata and README**

The service archetype metadata already includes `src/main/java/**/*.java` and `.gitkeep` inside each module, so no explicit mapper include is needed. Do not add narrow includes for individual Java files.

Update service generated README with:

```markdown
- Application pagination returns `Page<DomainModel>`; adapter converts it to external `PageResponse<DTO>` for Dubbo.
- Converter wrappers use concrete MapStruct Plus mapper components, not the generic `Converter` bean.
```

- [ ] **Step 10: Run service validation**

Run:

```bash
./mvnw -B -ntp -pl egon-cola-archetypes/egon-cola-archetype-service -am clean integration-test
```

Expected: `BUILD SUCCESS`, generated service project tests pass, `verify.groovy` passes, and pure-service guardrails still pass.

- [ ] **Step 11: Commit service implementation**

```bash
git add -- \
  egon-cola-archetypes/egon-cola-archetype-service/src/main/resources \
  egon-cola-archetypes/egon-cola-archetype-service/src/test/resources/projects/basic
git commit -m "feat(archetype-service): add mapper pagination followup"
```

---

### Task 5: Run Full Cross-Archetype Verification and Fix Empty Folder Metadata

**Files:**
- Modify only files needed to fix final verification, empty folders, README wording, metadata includes, or guard drift.

- [ ] **Step 1: Check for empty template directories**

Run:

```bash
find egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources \
     egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources \
     egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources \
     -type d -empty | sort
```

Expected: no output. If output appears under a Java package that should remain as an intentional package, add `package-info.java` with the correct package declaration. If output appears under a non-Java placeholder directory that must remain, add `.gitkeep`. If output appears for an obsolete directory, remove it from the archetype resources and metadata.

Example `package-info.java` shape:

```java
package ${package}.facade.enums;
```

- [ ] **Step 2: Run full archetype integration verification**

Run:

```bash
./mvnw -B -ntp -pl egon-cola-archetypes/egon-cola-archetype-light,egon-cola-archetypes/egon-cola-archetype-web,egon-cola-archetypes/egon-cola-archetype-service -am clean integration-test
```

Expected: `BUILD SUCCESS`. The generated light, web, and service projects compile, run tests, and pass their `verify.groovy` scripts.

- [ ] **Step 3: Run static source searches**

Run:

```bash
rg -n "private final Converter converter|import io\.github\.linpeilie\.Converter|@Qualifier\(\"converter\"\)" \
  egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources \
  egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources \
  egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources
```

Expected: no output.

Run:

```bash
rg -n "Page<.*DTO|Page<.*Response|PageResponse<.*> getPage" \
  egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java/application \
  egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-application/src/main/java \
  egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-application/src/main/java
```

Expected: no output.

Run:

```bash
rg -n "Page<.*Student>|Page<.*User>|Page<.*Course>" \
  egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/java/application \
  egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-application/src/main/java \
  egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-application/src/main/java
```

Expected: at least one match in each archetype application source.

Run:

```bash
rg -n "spring-boot-starter-web|spring-boot-starter-webflux|/controller/|/web/|/filter/|/graphql/|/vo/" \
  egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources
```

Expected: no forbidden generated service dependency or package matches. README guardrail text that says the service does not include those items is acceptable only if it does not create a generated package path.

- [ ] **Step 4: Run git hygiene checks**

Run:

```bash
git diff --check
git status --short --branch
git ls-files | rg '/target/'
```

Expected:

- `git diff --check`: no output and exit code 0.
- `git status --short --branch`: only intended files modified if final fixes are not yet committed.
- `git ls-files | rg '/target/'`: no output.

- [ ] **Step 5: Commit final verification fixes if any**

If Step 1-4 required changes, commit them:

```bash
git add -- \
  egon-cola-archetypes/egon-cola-archetype-light \
  egon-cola-archetypes/egon-cola-archetype-web \
  egon-cola-archetypes/egon-cola-archetype-service
git commit -m "test(archetype): finalize mapper pagination guardrails"
```

If no files changed, do not create an empty commit.

- [ ] **Step 6: Final status report**

Run:

```bash
git log --oneline --decorate -8
git status --short --branch
```

Expected: branch contains the task commits, and the working tree is clean.

Report:

- Full integration-test command and `BUILD SUCCESS` result.
- Static search commands and whether each produced the expected output.
- Any warnings that were present but did not fail tests, such as Maven plugin compatibility warnings or Mockito dynamic agent warnings.
- Whether any empty-folder placeholder files were added.
