# Student Management Evaluation Service Archetype Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace `egon-cola-archetype-service` with a Spring Boot 3.5.16, Java 21, pure Service archetype that generates one `student-management-evaluation` project.

**Architecture:** The generated project is one Maven aggregation root with seven layer modules: `common`, `facade`, `domain`, `application`, `infrastructure`, `adapter`, and `starter`. The sample implements course creation and exam-result recording through application orchestration, domain services, repository ports, Spring Data JPA infrastructure adapters, a facade-style RPC boundary, and a message-consumer boundary.

**Tech Stack:** Maven Archetype Plugin, Spring Boot 3.5.16, Java 21, Spring Context, Spring Transaction, Spring Validation, Spring Data JPA, Flyway, H2, PostgreSQL runtime driver, JUnit 5, ArchUnit, Maven Wrapper 3.9.14.

---

## File Structure

Archetype files to replace or create:

- Modify: `egon-cola-archetypes/egon-cola-archetype-service/src/test/resources/projects/basic/archetype.properties`
- Modify: `egon-cola-archetypes/egon-cola-archetype-service/src/test/resources/projects/basic/goal.txt`
- Create: `egon-cola-archetypes/egon-cola-archetype-service/src/test/resources/projects/basic/verify.groovy`
- Replace: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/META-INF/maven/archetype-metadata.xml`
- Replace: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/pom.xml`
- Replace: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__gitignore__`
- Create: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/.gitattributes`
- Create: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/README.md`
- Create: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/mvnw`
- Create: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/mvnw.cmd`
- Create: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/.mvn/wrapper/maven-wrapper.properties`
- Delete: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-client`
- Delete: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-app`
- Delete: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/start`
- Create: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-common`
- Create: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-facade`
- Rebuild: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-domain`
- Rebuild: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-application`
- Rebuild: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure`
- Create: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-adapter`
- Create: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-starter`

Generated module responsibilities:

```text
${rootArtifactId}-common          internal response, exception, constants, id utility
${rootArtifactId}-facade          CourseFacade, ExamResultFacade, request/response DTOs
${rootArtifactId}-domain          Course and ExamResult domain model, services, repository ports
${rootArtifactId}-application     CourseManage and ExamManage orchestration
${rootArtifactId}-infrastructure  JPA PO/repositories, port implementations, migration
${rootArtifactId}-adapter         facade implementations and message consumer
${rootArtifactId}-starter         Spring Boot application and runtime config
```

Use `${package}` as the generated base package. The basic integration test must use:

```properties
package=it.pkg
groupId=archetype.it
artifactId=student-management-evaluation
rootArtifactId=student-management-evaluation
version=0.1-SNAPSHOT
gitignore=.gitignore
```

## Implementation Contracts

Use these exact public method signatures across modules:

```java
// facade dto records
CreateCourseRequest(String name, int credit)
CourseDTO(String id, String name, int credit, String status)
RecordExamResultRequest(String courseId, String studentId, int score)
ExamResultDTO(String id, String courseId, String studentId, int score, String status)
ExamResultMessage(String courseId, String studentId, int score)

// facade APIs
CourseFacade.createCourse(CreateCourseRequest request)
CourseFacade.getCourse(String courseId)
ExamResultFacade.record(RecordExamResultRequest request)
ExamResultFacade.getResult(String examResultId)

// application APIs
CourseManage.create(String name, int credit)
CourseManage.getById(String courseId)
ExamManage.record(String courseId, String studentId, int score)
ExamManage.getById(String examResultId)

// domain repository ports
CourseRepository.save(Course course)
CourseRepository.findById(String courseId)
CourseRepository.existsByName(String name)
ExamResultRepository.save(ExamResult examResult)
ExamResultRepository.findById(String examResultId)
```

## Design Pattern Decision

The plan uses the existing COLA-style Ports and Adapters layering plus Domain Service classes. The variation points are inbound service boundaries and persistence technology: `adapter` owns facade/message entry points, `application` owns use-case orchestration, `domain` owns rules, and `infrastructure` implements repository ports. No Strategy, Factory Method, Chain of Responsibility, or extra handler registry is introduced because the sample has two direct flows and fixed business rules; extra abstraction would add indirection without reducing duplication.

---

### Task 1: Add Integration Guards and Root Project Defaults

**Files:**
- Modify: `egon-cola-archetypes/egon-cola-archetype-service/src/test/resources/projects/basic/archetype.properties`
- Modify: `egon-cola-archetypes/egon-cola-archetype-service/src/test/resources/projects/basic/goal.txt`
- Create: `egon-cola-archetypes/egon-cola-archetype-service/src/test/resources/projects/basic/verify.groovy`
- Modify: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/pom.xml`
- Modify: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__gitignore__`
- Create: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/.gitattributes`
- Create: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/README.md`
- Create: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/mvnw`
- Create: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/mvnw.cmd`
- Create: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/.mvn/wrapper/maven-wrapper.properties`
- Modify: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/META-INF/maven/archetype-metadata.xml`

- [ ] **Step 1: Write the integration guard**

Replace `archetype.properties` with:

```properties
package=it.pkg
version=0.1-SNAPSHOT
groupId=archetype.it
artifactId=student-management-evaluation
rootArtifactId=student-management-evaluation
gitignore=.gitignore
```

Replace `goal.txt` with:

```text
validate
```

Create `verify.groovy`:

```groovy
def assertFile = { path ->
    def file = new File(basedir, path)
    assert file.isFile(): "Expected file ${path}"
    file
}

def assertDir = { path ->
    def file = new File(basedir, path)
    assert file.isDirectory(): "Expected directory ${path}"
    file
}

assertFile("pom.xml")
assertFile("mvnw")
assertFile("mvnw.cmd")
assertFile(".mvn/wrapper/maven-wrapper.properties")
assertFile(".gitignore")
assertFile(".gitattributes")
assertFile("README.md")

["common", "facade", "domain", "application", "infrastructure", "adapter", "starter"].each {
    assertDir("student-management-evaluation-${it}")
}

assert !new File(basedir, "student-management-evaluation-client").exists()
assert !new File(basedir, "student-management-evaluation-app").exists()
assert !new File(basedir, "start").exists()
assert !new File(basedir, "student-management-organization").exists()

def forbiddenPaths = ["controller", "web", "filter", "graphql", "vo"].collect { "/${it}/" }
def generatedFiles = basedir.traverse(type: groovy.io.FileType.FILES).collect {
    it.absolutePath.replace(File.separatorChar, '/' as char)
}
forbiddenPaths.each { forbidden ->
    assert !generatedFiles.any { it.contains(forbidden) }: "Forbidden path segment ${forbidden}"
}

def pom = assertFile("pom.xml").text
assert pom.contains("<artifactId>spring-boot-starter-parent</artifactId>")
assert pom.contains("<version>3.5.16</version>")
assert pom.contains("<java.version>21</java.version>")
assert pom.contains("<module>student-management-evaluation-common</module>")
assert pom.contains("<module>student-management-evaluation-facade</module>")
assert pom.contains("<module>student-management-evaluation-domain</module>")
assert pom.contains("<module>student-management-evaluation-application</module>")
assert pom.contains("<module>student-management-evaluation-infrastructure</module>")
assert pom.contains("<module>student-management-evaluation-adapter</module>")
assert pom.contains("<module>student-management-evaluation-starter</module>")
assert !pom.contains("spring-boot-starter-web")
assert !pom.contains("spring-boot-starter-webflux")

def wrapper = assertFile(".mvn/wrapper/maven-wrapper.properties").text
assert wrapper.contains("apache-maven/3.9.14/apache-maven-3.9.14-bin.zip")
```

- [ ] **Step 2: Run the guard and verify it fails**

Run:

```bash
bash ./mvnw -B -ntp -pl egon-cola-archetypes/egon-cola-archetype-service -am test
```

Expected: FAIL because the current archetype still generates `client`, `app`, and `start`, and does not generate wrapper, `.gitattributes`, README, `common`, `facade`, `application`, `adapter`, or `starter` module names.

- [ ] **Step 3: Replace generated root POM**

Replace `archetype-resources/pom.xml` with a Spring Boot parent POM that contains:

```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.5.16</version>
    <relativePath/>
</parent>
<groupId>${groupId}</groupId>
<artifactId>${rootArtifactId}-parent</artifactId>
<version>${version}</version>
<packaging>pom</packaging>
<properties>
    <java.version>21</java.version>
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    <project.reporting.outputEncoding>UTF-8</project.reporting.outputEncoding>
    <archunit.version>1.3.0</archunit.version>
</properties>
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

Do not add `spring-boot-starter-web`, `spring-boot-starter-webflux`, Dubbo, gRPC, Kafka, RocketMQ, or RabbitMQ dependencies to the root POM.

- [ ] **Step 4: Add root files**

Copy the repository root Maven Wrapper scripts into the archetype template:

```bash
cp mvnw egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/mvnw
cp mvnw.cmd egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/mvnw.cmd
mkdir -p egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/.mvn/wrapper
cp .mvn/wrapper/maven-wrapper.properties egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/.mvn/wrapper/maven-wrapper.properties
```

After copying, edit the copied wrapper properties so `distributionUrl` points to:

```properties
distributionUrl=https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.14/apache-maven-3.9.14-bin.zip
```

Create `.gitattributes`:

```gitattributes
* text=auto
*.java text eol=lf
*.xml text eol=lf
*.yml text eol=lf
*.yaml text eol=lf
mvnw text eol=lf
mvnw.cmd text eol=crlf
```

Create `__gitignore__` with Java, Maven, IDE, OS, and runtime output exclusions:

```gitignore
target/
*.log
.idea/
*.iml
.vscode/
.DS_Store
.mvn/timing.properties
```

- [ ] **Step 5: Narrow archetype metadata**

Replace `archetype-metadata.xml` so it includes only root files and the seven generated modules. Include `filtered="true"` for Java, XML, YAML, Markdown, and properties files, and `filtered="false"` for `mvnw` and `mvnw.cmd`.

- [ ] **Step 6: Validate and commit**

Run:

```bash
git diff --check
```

Expected: PASS.

Commit:

```bash
git add egon-cola-archetypes/egon-cola-archetype-service/src/test/resources/projects/basic \
        egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/META-INF/maven/archetype-metadata.xml \
        egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/pom.xml \
        egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__gitignore__ \
        egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/.gitattributes \
        egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/README.md \
        egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/mvnw \
        egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/mvnw.cmd \
        egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/.mvn/wrapper/maven-wrapper.properties
git commit -m "feat: add evaluation service archetype project defaults"
```

---

### Task 2: Align Module Skeletons and POM Dependencies

**Files:**
- Delete: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-client`
- Delete: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-app`
- Delete: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/start`
- Create: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-common/pom.xml`
- Create: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-facade/pom.xml`
- Replace: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-domain/pom.xml`
- Replace: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-application/pom.xml`
- Replace: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/pom.xml`
- Create: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-adapter/pom.xml`
- Create: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-starter/pom.xml`
- Modify: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/META-INF/maven/archetype-metadata.xml`

- [ ] **Step 1: Remove old module directories**

Remove the old directories:

```bash
rm -rf egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-client
rm -rf egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-app
rm -rf egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/start
```

- [ ] **Step 2: Create module directories**

Create the seven module directories and their `src/main/java`, `src/main/resources`, and test directories:

```bash
mkdir -p egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-{common,facade,domain,application,infrastructure,adapter,starter}/src/main/java
mkdir -p egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-starter/src/test/java
```

- [ ] **Step 3: Write module POMs**

Use `${rootArtifactId}-parent` as the parent artifact for each module.

`common` POM dependencies: none.

`facade` POM dependencies:

```xml
<dependency>
    <groupId>jakarta.validation</groupId>
    <artifactId>jakarta.validation-api</artifactId>
</dependency>
```

`domain` POM dependencies:

```xml
<dependency>
    <groupId>${groupId}</groupId>
    <artifactId>${rootArtifactId}-common</artifactId>
    <version>${version}</version>
</dependency>
```

`application` POM dependencies:

```xml
<dependency>
    <groupId>${groupId}</groupId>
    <artifactId>${rootArtifactId}-domain</artifactId>
    <version>${version}</version>
</dependency>
<dependency>
    <groupId>${groupId}</groupId>
    <artifactId>${rootArtifactId}-common</artifactId>
    <version>${version}</version>
</dependency>
<dependency>
    <groupId>org.springframework</groupId>
    <artifactId>spring-context</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework</groupId>
    <artifactId>spring-tx</artifactId>
</dependency>
```

`infrastructure` POM dependencies:

```xml
<dependency>
    <groupId>${groupId}</groupId>
    <artifactId>${rootArtifactId}-domain</artifactId>
    <version>${version}</version>
</dependency>
<dependency>
    <groupId>${groupId}</groupId>
    <artifactId>${rootArtifactId}-application</artifactId>
    <version>${version}</version>
</dependency>
<dependency>
    <groupId>${groupId}</groupId>
    <artifactId>${rootArtifactId}-common</artifactId>
    <version>${version}</version>
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
```

`adapter` POM dependencies:

```xml
<dependency>
    <groupId>${groupId}</groupId>
    <artifactId>${rootArtifactId}-application</artifactId>
    <version>${version}</version>
</dependency>
<dependency>
    <groupId>${groupId}</groupId>
    <artifactId>${rootArtifactId}-facade</artifactId>
    <version>${version}</version>
</dependency>
<dependency>
    <groupId>${groupId}</groupId>
    <artifactId>${rootArtifactId}-common</artifactId>
    <version>${version}</version>
</dependency>
<dependency>
    <groupId>org.springframework</groupId>
    <artifactId>spring-context</artifactId>
</dependency>
<dependency>
    <groupId>jakarta.validation</groupId>
    <artifactId>jakarta.validation-api</artifactId>
</dependency>
```

`starter` POM dependencies:

```xml
<dependency>
    <groupId>${groupId}</groupId>
    <artifactId>${rootArtifactId}-adapter</artifactId>
    <version>${version}</version>
</dependency>
<dependency>
    <groupId>${groupId}</groupId>
    <artifactId>${rootArtifactId}-infrastructure</artifactId>
    <version>${version}</version>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
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
```

- [ ] **Step 4: Update archetype metadata for modules**

In `archetype-metadata.xml`, create one module entry for each generated module:

```xml
<module id="${rootArtifactId}-common" dir="__rootArtifactId__-common" name="${rootArtifactId}-common"/>
<module id="${rootArtifactId}-facade" dir="__rootArtifactId__-facade" name="${rootArtifactId}-facade"/>
<module id="${rootArtifactId}-domain" dir="__rootArtifactId__-domain" name="${rootArtifactId}-domain"/>
<module id="${rootArtifactId}-application" dir="__rootArtifactId__-application" name="${rootArtifactId}-application"/>
<module id="${rootArtifactId}-infrastructure" dir="__rootArtifactId__-infrastructure" name="${rootArtifactId}-infrastructure"/>
<module id="${rootArtifactId}-adapter" dir="__rootArtifactId__-adapter" name="${rootArtifactId}-adapter"/>
<module id="${rootArtifactId}-starter" dir="__rootArtifactId__-starter" name="${rootArtifactId}-starter"/>
```

- [ ] **Step 5: Validate and commit**

Run:

```bash
bash ./mvnw -B -ntp -pl egon-cola-archetypes/egon-cola-archetype-service -am test
```

Expected: FAIL because Java source, resources, generated tests, and runtime sample have not been added.

Run:

```bash
find egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources -maxdepth 1 -type d -name '*client' -o -name '*app' -o -name 'start'
```

Expected: no output.

Commit:

```bash
git add egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/META-INF/maven/archetype-metadata.xml \
        egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources
git commit -m "feat: align service archetype modules with evaluation architecture"
```

---

### Task 3: Add Common, Facade, Domain, and Application Code

**Files:**
- Create Java templates under `__rootArtifactId__-common/src/main/java`
- Create Java templates under `__rootArtifactId__-facade/src/main/java`
- Create Java templates under `__rootArtifactId__-domain/src/main/java`
- Create Java templates under `__rootArtifactId__-application/src/main/java`

- [ ] **Step 1: Add common module classes**

Create these classes under `${package}.common`:

```text
constants/ErrorCodes.java
exception/BizException.java
exception/NotFoundException.java
response/Response.java
response/SingleResponse.java
util/IdGenerator.java
```

`ErrorCodes` must define:

```java
COURSE_NAME_DUPLICATED
COURSE_NOT_FOUND
EXAM_RESULT_NOT_FOUND
INVALID_EXAM_SCORE
```

`IdGenerator` must expose:

```java
public static String nextId()
```

- [ ] **Step 2: Add facade contracts**

Create these facade types under `${package}.facade`:

```text
api/CourseFacade.java
api/ExamResultFacade.java
dto/course/CreateCourseRequest.java
dto/course/CourseDTO.java
dto/examing/RecordExamResultRequest.java
dto/examing/ExamResultDTO.java
```

Use `jakarta.validation.constraints.NotBlank`, `Min`, and `Max` on request records:

```java
CreateCourseRequest(@NotBlank String name, @Min(1) int credit)
RecordExamResultRequest(@NotBlank String courseId, @NotBlank String studentId, @Min(0) @Max(100) int score)
```

Facade methods must return `SingleResponse<CourseDTO>` or `SingleResponse<ExamResultDTO>`.

- [ ] **Step 3: Add domain model and ports**

Create these domain types under `${package}.domain`:

```text
entities/course/Course.java
entities/examing/ExamResult.java
enums/CourseStatus.java
enums/ExamResultStatus.java
repos/course/CourseRepository.java
repos/examing/ExamResultRepository.java
service/course/CourseDomainService.java
service/examing/ExamDomainService.java
```

Domain rules:

- `Course.create(String id, String name, int credit)` rejects blank names and credit values lower than 1 with `BizException`.
- `ExamResult.record(String id, String courseId, String studentId, int score)` rejects blank course/student ids and scores outside `0..100` with `BizException`.
- `CourseDomainService.ensureCourseNameAvailable(String name)` calls `CourseRepository.existsByName(name)` and throws `BizException` with `COURSE_NAME_DUPLICATED`.
- `ExamDomainService.record(String courseId, String studentId, int score)` calls `CourseRepository.findById(courseId)` before creating the exam result.

- [ ] **Step 4: Add application orchestration**

Create these application types under `${package}.application`:

```text
manage/course/CourseManage.java
manage/course/impl/CourseManageImpl.java
manage/examing/ExamManage.java
manage/examing/impl/ExamManageImpl.java
view/course/CourseView.java
view/examing/ExamResultView.java
```

`CourseManageImpl`:

- Annotate the class with `@Service`.
- Annotate `create` with `@Transactional`.
- Generate ids through `IdGenerator.nextId()`.
- Call `CourseDomainService.ensureCourseNameAvailable(name)` before saving.

`ExamManageImpl`:

- Annotate the class with `@Service`.
- Annotate `record` with `@Transactional`.
- Generate ids through `IdGenerator.nextId()`.
- Call `ExamDomainService.record(courseId, studentId, score)` before saving.

- [ ] **Step 5: Validate and commit**

Run:

```bash
bash ./mvnw -B -ntp -pl egon-cola-archetypes/egon-cola-archetype-service -am test
```

Expected: FAIL because infrastructure repository implementations, adapter classes, starter application, and generated tests have not been added.

Commit:

```bash
git add egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-common \
        egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-facade \
        egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-domain \
        egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-application
git commit -m "feat: add evaluation service domain flow"
```

---

### Task 4: Add Infrastructure, Adapter, Starter, and Generated Tests

**Files:**
- Create Java templates under `__rootArtifactId__-infrastructure/src/main/java`
- Create SQL migration under `__rootArtifactId__-infrastructure/src/main/resources/db/migration`
- Create Java templates under `__rootArtifactId__-adapter/src/main/java`
- Create Java templates under `__rootArtifactId__-starter/src/main/java`
- Create resources under `__rootArtifactId__-starter/src/main/resources`
- Create generated tests under `__rootArtifactId__-starter/src/test/java`
- Modify: `egon-cola-archetypes/egon-cola-archetype-service/src/test/resources/projects/basic/goal.txt`

- [ ] **Step 1: Add infrastructure persistence**

Create these infrastructure types under `${package}.infrastructure`:

```text
repo/course/po/CoursePo.java
repo/course/jpa/CourseJpaRepository.java
repo/course/converter/CourseConverter.java
repo/course/impl/CourseRepositoryImpl.java
repo/examing/po/ExamResultPo.java
repo/examing/jpa/ExamResultJpaRepository.java
repo/examing/converter/ExamResultConverter.java
repo/examing/impl/ExamResultRepositoryImpl.java
```

`CourseJpaRepository` must extend:

```java
JpaRepository<CoursePo, String>
```

and declare:

```java
boolean existsByName(String name);
```

`ExamResultJpaRepository` must extend:

```java
JpaRepository<ExamResultPo, String>
```

- [ ] **Step 2: Add exactly one Flyway migration**

Create one migration file:

```text
__rootArtifactId__-infrastructure/src/main/resources/db/migration/V1__init_student_management_evaluation.sql
```

The migration must create:

```sql
create table course (
    id varchar(64) primary key,
    name varchar(128) not null,
    credit integer not null,
    status varchar(32) not null,
    created_at timestamp not null,
    updated_at timestamp not null
);

create unique index uk_course_name on course (name);

create table exam_result (
    id varchar(64) primary key,
    course_id varchar(64) not null,
    student_id varchar(64) not null,
    score integer not null,
    status varchar(32) not null,
    created_at timestamp not null,
    updated_at timestamp not null
);

create index idx_exam_result_course_id on exam_result (course_id);
create index idx_exam_result_student_id on exam_result (student_id);
```

Do not create a second migration file in this task.

- [ ] **Step 3: Add adapter service boundaries**

Create these adapter types under `${package}.adapter`:

```text
convertor/CourseAdapterConvertor.java
convertor/ExamResultAdapterConvertor.java
dto/ExamResultMessage.java
facade/impl/CourseFacadeImpl.java
facade/impl/ExamResultFacadeImpl.java
mq/ExamResultMessageConsumer.java
handler/ServiceExceptionHandler.java
```

`CourseFacadeImpl` must implement `CourseFacade`, call `CourseManage`, and convert `CourseView` to `CourseDTO`.

`ExamResultFacadeImpl` must implement `ExamResultFacade`, call `ExamManage`, and convert `ExamResultView` to `ExamResultDTO`.

`ExamResultMessageConsumer` must expose:

```java
public SingleResponse<ExamResultDTO> consume(ExamResultMessage message)
```

It must call `ExamManage.record(message.courseId(), message.studentId(), message.score())`.

`ServiceExceptionHandler` must be a plain component that converts `BizException` and unexpected exceptions to `Response` objects. Do not annotate it with Web exception annotations.

- [ ] **Step 4: Add starter application and runtime config**

Create `${package}.starter.EvaluationServiceApplication`:

```java
@SpringBootApplication(scanBasePackages = "${package}")
public class EvaluationServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(EvaluationServiceApplication.class, args);
    }
}
```

Create `__rootArtifactId__-starter/src/main/resources/application.yml`:

```yaml
spring:
  application:
    name: ${rootArtifactId}
  datasource:
    url: jdbc:h2:mem:${rootArtifactId};MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
    username: sa
    password:
    driver-class-name: org.h2.Driver
  jpa:
    hibernate:
      ddl-auto: validate
    open-in-view: false
  flyway:
    enabled: true
management:
  endpoints:
    web:
      exposure:
        include: health,info
```

The actuator web endpoint configuration is permitted because the generated project uses `spring-boot-starter-actuator` without `spring-boot-starter-web`; it must not add servlet controllers or Web dependencies.

- [ ] **Step 5: Add generated tests**

Create these generated tests under `${package}.starter`:

```text
EvaluationFlowTest.java
ServiceArchitectureDependencyTest.java
```

`EvaluationFlowTest` must:

- Use `@SpringBootTest`.
- Autowire `CourseFacade` and `ExamResultMessageConsumer`.
- Create one course through `CourseFacade.createCourse`.
- Record one exam result through `ExamResultMessageConsumer.consume`.
- Assert course status is `ENABLED`, exam result status is `PASSED` for score `90`, and response success flags are true.

`ServiceArchitectureDependencyTest` must:

- Use ArchUnit `ClassFileImporter`.
- Assert no class package contains `.controller.`, `.web.`, `.filter.`, `.graphql.`, or `.vo.`.
- Assert domain classes do not depend on adapter, application, infrastructure, facade, Spring Data JPA, Spring MVC, MQ framework packages, or RPC framework packages.
- Assert application classes do not depend on adapter, infrastructure, facade, or starter packages.
- Assert adapter classes do not depend on Spring MVC packages.

Replace `goal.txt` with:

```text
test
```

- [ ] **Step 6: Validate and commit**

Run:

```bash
bash ./mvnw -B -ntp -pl egon-cola-archetypes/egon-cola-archetype-service -am test
```

Expected: PASS.

Run:

```bash
generated_dir="$(find egon-cola-archetypes/egon-cola-archetype-service/target -path '*/projects/basic/project/student-management-evaluation/pom.xml' -print -quit | xargs dirname)"
test -n "$generated_dir"
bash "$generated_dir/mvnw" -B -ntp -f "$generated_dir/pom.xml" test
```

Expected: PASS.

Commit:

```bash
git add egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure \
        egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-adapter \
        egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-starter \
        egon-cola-archetypes/egon-cola-archetype-service/src/test/resources/projects/basic/goal.txt
git commit -m "feat: add evaluation service runtime sample"
```

---

### Task 5: Final README, Metadata Cleanup, and Full Verification

**Files:**
- Modify: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/README.md`
- Modify: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/META-INF/maven/archetype-metadata.xml`
- Review: all files under `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources`

- [ ] **Step 1: Finalize generated README**

README must include:

- Generated project name: `${rootArtifactId}`.
- Business domain: student-management evaluation.
- Module descriptions for `common`, `facade`, `domain`, `application`, `infrastructure`, `adapter`, and `starter`.
- Pure Service rule: no HTTP Controller, Web Filter, GraphQL endpoint, Web VO, `spring-boot-starter-web`, or `spring-boot-starter-webflux`.
- RPC extension point: add concrete RPC annotations and dependencies to `adapter` after choosing Dubbo or gRPC.
- MQ extension point: add concrete consumer annotations and dependencies to `adapter` after choosing Kafka, RocketMQ, or RabbitMQ.
- Commands:

```bash
./mvnw test
./mvnw -DskipTests package
./mvnw -pl ${rootArtifactId}-starter spring-boot:run
```

The implementation validation must not run `spring-boot:run`.

- [ ] **Step 2: Verify metadata coverage**

Inspect `archetype-metadata.xml` and confirm each generated Java, XML, YAML, SQL, Markdown, shell, command, gitignore, gitattributes, and wrapper properties file is included in the file sets.

- [ ] **Step 3: Run forbidden-content checks**

Run:

```bash
rg -n "student-management-organization|__rootArtifactId__-client|__rootArtifactId__-app|<module>start</module>|spring-boot-starter-web|spring-boot-starter-webflux" \
  egon-cola-archetypes/egon-cola-archetype-service/src/main/resources \
  egon-cola-archetypes/egon-cola-archetype-service/src/test/resources/projects/basic
```

Expected: no output.

Run:

```bash
rg -n "package .*(controller|web|filter|graphql|vo)|/controller/|/web/|/filter/|/graphql/|/vo/" \
  egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources
```

Expected: no output.

Run:

```bash
find egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/main/resources/db/migration -type f
```

Expected output:

```text
egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/main/resources/db/migration/V1__init_student_management_evaluation.sql
```

- [ ] **Step 4: Run final validation**

Run:

```bash
bash ./mvnw -B -ntp -pl egon-cola-archetypes/egon-cola-archetype-service -am test
```

Expected: PASS.

Run:

```bash
generated_dir="$(find egon-cola-archetypes/egon-cola-archetype-service/target -path '*/projects/basic/project/student-management-evaluation/pom.xml' -print -quit | xargs dirname)"
test -n "$generated_dir"
bash "$generated_dir/mvnw" -B -ntp -f "$generated_dir/pom.xml" test
```

Expected: PASS.

Run:

```bash
git diff --check
```

Expected: PASS.

- [ ] **Step 5: Commit final cleanup**

Commit:

```bash
git add egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/README.md \
        egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/META-INF/maven/archetype-metadata.xml \
        egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources
git commit -m "docs: finalize evaluation service archetype readme"
```

---

## Completion Checklist

- [ ] `egon-cola-archetype-service` generates only `student-management-evaluation`.
- [ ] The generated project inherits `spring-boot-starter-parent` version `3.5.16`.
- [ ] The generated project uses Java 21 and UTF-8 encoding.
- [ ] Maven Wrapper files are generated and point to Maven 3.9.14.
- [ ] `.gitignore`, `.gitattributes`, and `README.md` are generated.
- [ ] Generated modules are exactly `common`, `facade`, `domain`, `application`, `infrastructure`, `adapter`, and `starter`.
- [ ] No `student-management-organization`, `client`, `app`, or `start` project/module is generated.
- [ ] No `controller`, `web`, `filter`, `graphql`, or `vo` package is generated.
- [ ] No `spring-boot-starter-web` or `spring-boot-starter-webflux` dependency is generated.
- [ ] Adapter contains facade-style RPC implementation and message-consumer examples without concrete RPC/MQ framework dependencies.
- [ ] Course creation enforces unique course name.
- [ ] Exam result recording enforces course existence and score range `0..100`.
- [ ] Exactly one Flyway migration exists for the generated service.
- [ ] Generated tests pass through the archetype integration test.
- [ ] The generated project test suite passes through its own Maven Wrapper.
- [ ] No generated application is started during validation.
