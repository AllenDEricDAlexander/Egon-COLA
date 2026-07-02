# Student Management Organization Web Archetype Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace `egon-cola-archetype-web` with a Spring Boot 3.5.16, Java 21, multi-module `student-management-organization` archetype.

**Architecture:** The generated project is one Maven aggregation root with seven layer modules: `common`, `facade`, `domain`, `application`, `infrastructure`, `adapter`, and `starter`. The sample implements user creation and assigning a user to a school class through application orchestration, domain repository ports, and Spring Data JPA infrastructure adapters.

**Tech Stack:** Maven Archetype Plugin, Spring Boot 3.5.16, Java 21, Spring MVC, Spring Validation, Spring Data JPA, Flyway, H2, PostgreSQL runtime driver, JUnit 5, ArchUnit, Maven Wrapper 3.9.14.

---

## File Structure

Archetype files to replace or create:

- Modify: `egon-cola-archetypes/egon-cola-archetype-web/src/test/resources/projects/basic/archetype.properties`
- Modify: `egon-cola-archetypes/egon-cola-archetype-web/src/test/resources/projects/basic/goal.txt`
- Create: `egon-cola-archetypes/egon-cola-archetype-web/src/test/resources/projects/basic/verify.groovy`
- Replace: `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/META-INF/maven/archetype-metadata.xml`
- Replace: `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/pom.xml`
- Replace: `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__gitignore__`
- Create: `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/.gitattributes`
- Create: `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/README.md`
- Create: `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/mvnw`
- Create: `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/mvnw.cmd`
- Create: `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/.mvn/wrapper/maven-wrapper.properties`
- Delete: `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-client`
- Delete: `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-app`
- Delete: `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/start`
- Create: `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-common`
- Create: `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-facade`
- Rebuild: `__rootArtifactId__-domain`, `__rootArtifactId__-application`, `__rootArtifactId__-infrastructure`, `__rootArtifactId__-adapter`
- Create: `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-starter`

Generated module responsibilities:

```text
${rootArtifactId}-common          common response, exception, constants, id utility
${rootArtifactId}-facade          UserFacade, SchoolClassFacade, request/response DTOs
${rootArtifactId}-domain          User and SchoolClass domain model, services, repository ports
${rootArtifactId}-application     UserManage and SchoolClassManage orchestration
${rootArtifactId}-infrastructure  JPA PO/repositories, port implementations, migration
${rootArtifactId}-adapter         HTTP controllers, facade implementations, exception handler
${rootArtifactId}-starter         Spring Boot application and runtime config
```

Use `${package}` as the generated base package. The basic integration test should use:

```properties
package=it.pkg
groupId=archetype.it
artifactId=student-management-organization
rootArtifactId=student-management-organization
version=0.1-SNAPSHOT
gitignore=.gitignore
```

## Implementation Contracts

Use these exact public method signatures across modules:

```java
// facade dto records
CreateUserRequest(String name, String email)
UserDTO(String id, String name, String email, String status, List<String> schoolClassIds)
CreateSchoolClassRequest(String name, String gradeName)
AssignUserToClassRequest(String userId, String schoolClassId)
SchoolClassDTO(String id, String name, String gradeName, List<String> userIds)

// facade APIs
UserFacade.createUser(CreateUserRequest request)
UserFacade.getUser(String userId)
SchoolClassFacade.createSchoolClass(CreateSchoolClassRequest request)
SchoolClassFacade.assignUser(AssignUserToClassRequest request)

// application APIs
UserManage.create(String name, String email)
UserManage.getById(String userId)
SchoolClassManage.create(String name, String gradeName)
SchoolClassManage.assignUser(String userId, String schoolClassId)

// domain repository ports
UserRepository.save(User user)
UserRepository.findById(String userId)
UserRepository.existsByEmail(String email)
SchoolClassRepository.save(SchoolClass schoolClass)
SchoolClassRepository.findById(String schoolClassId)
```

---

### Task 1: Add Integration Guards and Root Project Defaults

**Files:**
- Modify: `egon-cola-archetypes/egon-cola-archetype-web/src/test/resources/projects/basic/archetype.properties`
- Modify: `egon-cola-archetypes/egon-cola-archetype-web/src/test/resources/projects/basic/goal.txt`
- Create: `egon-cola-archetypes/egon-cola-archetype-web/src/test/resources/projects/basic/verify.groovy`
- Modify: `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/pom.xml`
- Modify: `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__gitignore__`
- Create: `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/.gitattributes`
- Create: `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/README.md`
- Create: `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/mvnw`
- Create: `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/mvnw.cmd`
- Create: `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/.mvn/wrapper/maven-wrapper.properties`
- Modify: `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/META-INF/maven/archetype-metadata.xml`

- [ ] **Step 1: Write the integration guard**

Replace `archetype.properties` with:

```properties
package=it.pkg
version=0.1-SNAPSHOT
groupId=archetype.it
artifactId=student-management-organization
rootArtifactId=student-management-organization
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
    assertDir("student-management-organization-${it}")
}

assert !new File(basedir, "student-management-organization-client").exists()
assert !new File(basedir, "student-management-organization-app").exists()
assert !new File(basedir, "start").exists()
assert !new File(basedir, "student-management-evaluation").exists()

def pom = assertFile("pom.xml").text
assert pom.contains("<artifactId>spring-boot-starter-parent</artifactId>")
assert pom.contains("<version>3.5.16</version>")
assert pom.contains("<java.version>21</java.version>")
assert pom.contains("<module>student-management-organization-common</module>")
assert pom.contains("<module>student-management-organization-starter</module>")
assert !pom.contains("spring-ai")
assert !pom.contains("drools")
assert !pom.contains("mcp")

def wrapper = assertFile(".mvn/wrapper/maven-wrapper.properties").text
assert wrapper.contains("apache-maven/3.9.14/apache-maven-3.9.14-bin.zip")
```

- [ ] **Step 2: Run the guard and verify it fails**

Run:

```bash
bash ./mvnw -B -ntp -pl egon-cola-archetypes/egon-cola-archetype-web -am test
```

Expected: FAIL because the current archetype still generates `client`, `app`, and `start`, and does not generate wrapper, `.gitattributes`, README, or `common/facade/application/starter` module names.

- [ ] **Step 3: Replace generated root POM**

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
    <artifactId>${rootArtifactId}-parent</artifactId>
    <version>${version}</version>
    <packaging>pom</packaging>
    <name>${rootArtifactId}</name>
    <description>Student management organization sample based on Egon COLA web architecture</description>

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

    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>${groupId}</groupId>
                <artifactId>${rootArtifactId}-common</artifactId>
                <version>${project.version}</version>
            </dependency>
            <dependency>
                <groupId>${groupId}</groupId>
                <artifactId>${rootArtifactId}-facade</artifactId>
                <version>${project.version}</version>
            </dependency>
            <dependency>
                <groupId>${groupId}</groupId>
                <artifactId>${rootArtifactId}-domain</artifactId>
                <version>${project.version}</version>
            </dependency>
            <dependency>
                <groupId>${groupId}</groupId>
                <artifactId>${rootArtifactId}-application</artifactId>
                <version>${project.version}</version>
            </dependency>
            <dependency>
                <groupId>${groupId}</groupId>
                <artifactId>${rootArtifactId}-infrastructure</artifactId>
                <version>${project.version}</version>
            </dependency>
            <dependency>
                <groupId>${groupId}</groupId>
                <artifactId>${rootArtifactId}-adapter</artifactId>
                <version>${project.version}</version>
            </dependency>
            <dependency>
                <groupId>${groupId}</groupId>
                <artifactId>${rootArtifactId}-starter</artifactId>
                <version>${project.version}</version>
            </dependency>
            <dependency>
                <groupId>com.tngtech.archunit</groupId>
                <artifactId>archunit-junit5</artifactId>
                <version>${archunit.version}</version>
                <scope>test</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>
</project>
```

- [ ] **Step 4: Add wrapper and root metadata files**

Copy wrapper scripts from the repository root:

```bash
cp mvnw egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/mvnw
cp mvnw.cmd egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/mvnw.cmd
mkdir -p egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/.mvn/wrapper
cp .mvn/wrapper/maven-wrapper.properties egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/.mvn/wrapper/maven-wrapper.properties
```

Replace `__gitignore__` with:

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

### OS ###
.DS_Store
Thumbs.db

### Local env ###
.env
.env.*
application-local.yml
```

Create `.gitattributes`:

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

Create initial `README.md`:

```markdown
# Student Management Organization

This project is generated by `egon-cola-archetype-web`.

It creates only the organization Project. It does not generate `student-management-evaluation`.

## Modules

```text
student-management-organization-common
student-management-organization-facade
student-management-organization-domain
student-management-organization-application
student-management-organization-infrastructure
student-management-organization-adapter
student-management-organization-starter
```

## Commands

```bash
./mvnw test
./mvnw -DskipTests package
./mvnw -pl student-management-organization-starter spring-boot:run
```
```

- [ ] **Step 5: Replace archetype metadata for target modules and root files**

Replace `archetype-metadata.xml` with a descriptor that has:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<archetype-descriptor
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://maven.apache.org/plugins/maven-archetype-plugin/archetype-descriptor/1.0.0 http://maven.apache.org/xsd/archetype-descriptor-1.0.0.xsd"
    name="student-management-organization"
    xmlns="http://maven.apache.org/plugins/maven-archetype-plugin/archetype-descriptor/1.0.0">
    <requiredProperties>
        <requiredProperty key="gitignore">
            <defaultValue>.gitignore</defaultValue>
        </requiredProperty>
    </requiredProperties>
    <fileSets>
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
    </fileSets>
    <modules>
        <module id="${rootArtifactId}-common" dir="__rootArtifactId__-common" name="${rootArtifactId}-common"/>
        <module id="${rootArtifactId}-facade" dir="__rootArtifactId__-facade" name="${rootArtifactId}-facade"/>
        <module id="${rootArtifactId}-domain" dir="__rootArtifactId__-domain" name="${rootArtifactId}-domain"/>
        <module id="${rootArtifactId}-application" dir="__rootArtifactId__-application" name="${rootArtifactId}-application"/>
        <module id="${rootArtifactId}-infrastructure" dir="__rootArtifactId__-infrastructure" name="${rootArtifactId}-infrastructure"/>
        <module id="${rootArtifactId}-adapter" dir="__rootArtifactId__-adapter" name="${rootArtifactId}-adapter"/>
        <module id="${rootArtifactId}-starter" dir="__rootArtifactId__-starter" name="${rootArtifactId}-starter"/>
    </modules>
</archetype-descriptor>
```

In later tasks, each module entry will be expanded with module-specific fileSets. Keep this minimal version only long enough to prove root generation.

- [ ] **Step 6: Run root validation**

Run:

```bash
bash ./mvnw -B -ntp -pl egon-cola-archetypes/egon-cola-archetype-web -am test
```

Expected: PASS for root files and module directory checks, with generated project `validate`.

- [ ] **Step 7: Commit Task 1**

```bash
git add egon-cola-archetypes/egon-cola-archetype-web/src/test/resources/projects/basic \
        egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/pom.xml \
        egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__gitignore__ \
        egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/.gitattributes \
        egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/README.md \
        egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/mvnw \
        egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/mvnw.cmd \
        egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/.mvn/wrapper/maven-wrapper.properties \
        egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/META-INF/maven/archetype-metadata.xml
git commit -m "feat: add organization web archetype project defaults"
```

---

### Task 2: Replace Module Skeleton and POMs

**Files:**
- Delete: `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-client`
- Delete: `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-app`
- Delete: `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/start`
- Create: `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-common/pom.xml`
- Create: `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-facade/pom.xml`
- Replace: `__rootArtifactId__-domain/pom.xml`
- Create: `__rootArtifactId__-application/pom.xml`
- Delete: `__rootArtifactId__-app/pom.xml`
- Replace: `__rootArtifactId__-infrastructure/pom.xml`
- Replace: `__rootArtifactId__-adapter/pom.xml`
- Create: `__rootArtifactId__-starter/pom.xml`
- Modify: `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/META-INF/maven/archetype-metadata.xml`
- Modify: `egon-cola-archetypes/egon-cola-archetype-web/src/test/resources/projects/basic/verify.groovy`

- [ ] **Step 1: Extend verify.groovy for POM dependencies**

Append:

```groovy
def modulePom = { module -> assertFile("student-management-organization-${module}/pom.xml").text }

assert modulePom("common").contains("<artifactId>student-management-organization-common</artifactId>")
assert modulePom("facade").contains("<artifactId>student-management-organization-facade</artifactId>")
assert modulePom("domain").contains("<artifactId>student-management-organization-domain</artifactId>")
assert modulePom("application").contains("<artifactId>student-management-organization-application</artifactId>")
assert modulePom("infrastructure").contains("<artifactId>student-management-organization-infrastructure</artifactId>")
assert modulePom("adapter").contains("<artifactId>student-management-organization-adapter</artifactId>")
assert modulePom("starter").contains("<artifactId>student-management-organization-starter</artifactId>")

assert modulePom("domain").contains("<artifactId>student-management-organization-common</artifactId>")
assert modulePom("application").contains("<artifactId>student-management-organization-domain</artifactId>")
assert !modulePom("application").contains("student-management-organization-infrastructure")
assert modulePom("adapter").contains("<artifactId>student-management-organization-application</artifactId>")
assert modulePom("adapter").contains("<artifactId>student-management-organization-facade</artifactId>")
assert !modulePom("adapter").contains("student-management-organization-infrastructure")
assert modulePom("starter").contains("<artifactId>student-management-organization-adapter</artifactId>")
assert modulePom("starter").contains("<artifactId>student-management-organization-infrastructure</artifactId>")
```

- [ ] **Step 2: Run validation and verify it fails**

Run:

```bash
bash ./mvnw -B -ntp -pl egon-cola-archetypes/egon-cola-archetype-web -am test
```

Expected: FAIL because module POMs still use old module names or do not exist.

- [ ] **Step 3: Remove obsolete module directories**

Run:

```bash
rm -rf egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-client
rm -rf egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-app
rm -rf egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/start
```

- [ ] **Step 4: Create common and facade POMs**

Create `__rootArtifactId__-common/pom.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>${groupId}</groupId>
        <artifactId>${rootArtifactId}-parent</artifactId>
        <version>${version}</version>
        <relativePath>../pom.xml</relativePath>
    </parent>
    <artifactId>${rootArtifactId}-common</artifactId>
    <packaging>jar</packaging>
    <name>${rootArtifactId}-common</name>
</project>
```

Create `__rootArtifactId__-facade/pom.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>${groupId}</groupId>
        <artifactId>${rootArtifactId}-parent</artifactId>
        <version>${version}</version>
        <relativePath>../pom.xml</relativePath>
    </parent>
    <artifactId>${rootArtifactId}-facade</artifactId>
    <packaging>jar</packaging>
    <name>${rootArtifactId}-facade</name>
    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>
    </dependencies>
</project>
```

- [ ] **Step 5: Create domain and application POMs**

Replace `__rootArtifactId__-domain/pom.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>${groupId}</groupId>
        <artifactId>${rootArtifactId}-parent</artifactId>
        <version>${version}</version>
        <relativePath>../pom.xml</relativePath>
    </parent>
    <artifactId>${rootArtifactId}-domain</artifactId>
    <packaging>jar</packaging>
    <name>${rootArtifactId}-domain</name>
    <dependencies>
        <dependency>
            <groupId>${groupId}</groupId>
            <artifactId>${rootArtifactId}-common</artifactId>
        </dependency>
    </dependencies>
</project>
```

Create `__rootArtifactId__-application/pom.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>${groupId}</groupId>
        <artifactId>${rootArtifactId}-parent</artifactId>
        <version>${version}</version>
        <relativePath>../pom.xml</relativePath>
    </parent>
    <artifactId>${rootArtifactId}-application</artifactId>
    <packaging>jar</packaging>
    <name>${rootArtifactId}-application</name>
    <dependencies>
        <dependency>
            <groupId>${groupId}</groupId>
            <artifactId>${rootArtifactId}-domain</artifactId>
        </dependency>
        <dependency>
            <groupId>${groupId}</groupId>
            <artifactId>${rootArtifactId}-common</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework</groupId>
            <artifactId>spring-context</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework</groupId>
            <artifactId>spring-tx</artifactId>
        </dependency>
    </dependencies>
</project>
```

- [ ] **Step 6: Create infrastructure, adapter, and starter POMs**

Replace `__rootArtifactId__-infrastructure/pom.xml`, `__rootArtifactId__-adapter/pom.xml`, and create `__rootArtifactId__-starter/pom.xml` with dependencies matching the spec:

```xml
<!-- infrastructure dependencies -->
<dependencies>
    <dependency>
        <groupId>${groupId}</groupId>
        <artifactId>${rootArtifactId}-domain</artifactId>
    </dependency>
    <dependency>
        <groupId>${groupId}</groupId>
        <artifactId>${rootArtifactId}-application</artifactId>
    </dependency>
    <dependency>
        <groupId>${groupId}</groupId>
        <artifactId>${rootArtifactId}-common</artifactId>
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
</dependencies>
```

```xml
<!-- adapter dependencies -->
<dependencies>
    <dependency>
        <groupId>${groupId}</groupId>
        <artifactId>${rootArtifactId}-application</artifactId>
    </dependency>
    <dependency>
        <groupId>${groupId}</groupId>
        <artifactId>${rootArtifactId}-facade</artifactId>
    </dependency>
    <dependency>
        <groupId>${groupId}</groupId>
        <artifactId>${rootArtifactId}-common</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>
</dependencies>
```

```xml
<!-- starter dependencies -->
<dependencies>
    <dependency>
        <groupId>${groupId}</groupId>
        <artifactId>${rootArtifactId}-adapter</artifactId>
    </dependency>
    <dependency>
        <groupId>${groupId}</groupId>
        <artifactId>${rootArtifactId}-infrastructure</artifactId>
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
        <scope>test</scope>
    </dependency>
</dependencies>
```

Each POM must include this parent block and its own module artifactId:

```xml
<parent>
    <groupId>${groupId}</groupId>
    <artifactId>${rootArtifactId}-parent</artifactId>
    <version>${version}</version>
    <relativePath>../pom.xml</relativePath>
</parent>
```

- [ ] **Step 7: Expand metadata module fileSets**

For every module in `archetype-metadata.xml`, add fileSets for `pom.xml`, Java sources, resources, and tests:

```xml
<fileSets>
    <fileSet filtered="true" encoding="UTF-8">
        <directory></directory>
        <includes>
            <include>pom.xml</include>
        </includes>
    </fileSet>
    <fileSet filtered="true" packaged="true" encoding="UTF-8">
        <directory>src/main/java</directory>
        <includes>
            <include>**/*.java</include>
        </includes>
    </fileSet>
    <fileSet filtered="true" encoding="UTF-8">
        <directory>src/main/resources</directory>
        <includes>
            <include>**/*.yml</include>
            <include>**/*.xml</include>
            <include>**/*.sql</include>
        </includes>
    </fileSet>
    <fileSet filtered="true" packaged="true" encoding="UTF-8">
        <directory>src/test/java</directory>
        <includes>
            <include>**/*.java</include>
        </includes>
    </fileSet>
    <fileSet filtered="true" encoding="UTF-8">
        <directory>src/test/resources</directory>
        <includes>
            <include>**/*.yml</include>
            <include>**/*.xml</include>
        </includes>
    </fileSet>
</fileSets>
```

Keep fileSets even when a module has no resources yet; empty directories do not need to be generated.

- [ ] **Step 8: Run module POM validation**

Run:

```bash
bash ./mvnw -B -ntp -pl egon-cola-archetypes/egon-cola-archetype-web -am test
```

Expected: PASS for generated project `validate`.

- [ ] **Step 9: Commit Task 2**

```bash
git add egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources \
        egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/META-INF/maven/archetype-metadata.xml \
        egon-cola-archetypes/egon-cola-archetype-web/src/test/resources/projects/basic/verify.groovy
git commit -m "feat: align web archetype modules with organization architecture"
```

---

### Task 3: Add Common, Facade, Domain, and Application Code

**Files:**
- Create Java source under:
  - `__rootArtifactId__-common/src/main/java`
  - `__rootArtifactId__-facade/src/main/java`
  - `__rootArtifactId__-domain/src/main/java`
  - `__rootArtifactId__-application/src/main/java`
- Modify: `egon-cola-archetypes/egon-cola-archetype-web/src/test/resources/projects/basic/verify.groovy`

- [ ] **Step 1: Add source existence checks**

Append to `verify.groovy`:

```groovy
assertFile("student-management-organization-common/src/main/java/it/pkg/common/response/Response.java")
assertFile("student-management-organization-facade/src/main/java/it/pkg/facade/user/UserFacade.java")
assertFile("student-management-organization-facade/src/main/java/it/pkg/facade/teaching/SchoolClassFacade.java")
assertFile("student-management-organization-domain/src/main/java/it/pkg/domain/entities/user/User.java")
assertFile("student-management-organization-domain/src/main/java/it/pkg/domain/entities/teaching/SchoolClass.java")
assertFile("student-management-organization-application/src/main/java/it/pkg/application/manage/user/UserManage.java")
assertFile("student-management-organization-application/src/main/java/it/pkg/application/manage/teaching/SchoolClassManage.java")
```

- [ ] **Step 2: Run validation and verify it fails**

Run:

```bash
bash ./mvnw -B -ntp -pl egon-cola-archetypes/egon-cola-archetype-web -am test
```

Expected: FAIL because these source files do not exist.

- [ ] **Step 3: Create common module classes**

Create `Response.java`, `SingleResponse.java`, `BizException.java`, `NotFoundException.java`, `ErrorCodes.java`, and `IdGenerator.java` under `${package}.common`.

Use these exact class bodies:

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

```java
package ${package}.common.constants;

public final class ErrorCodes {
    public static final String USER_NOT_FOUND = "USER_NOT_FOUND";
    public static final String USER_EMAIL_DUPLICATED = "USER_EMAIL_DUPLICATED";
    public static final String SCHOOL_CLASS_NOT_FOUND = "SCHOOL_CLASS_NOT_FOUND";
    public static final String SCHOOL_CLASS_USER_DUPLICATED = "SCHOOL_CLASS_USER_DUPLICATED";

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

- [ ] **Step 4: Create facade contracts and DTO records**

Create:

```java
package ${package}.facade.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record CreateUserRequest(@NotBlank String name, @Email @NotBlank String email) {
}
```

```java
package ${package}.facade.dto.user;

import java.util.List;

public record UserDTO(String id, String name, String email, String status, List<String> schoolClassIds) {
}
```

```java
package ${package}.facade.dto.teaching;

import jakarta.validation.constraints.NotBlank;

public record CreateSchoolClassRequest(@NotBlank String name, @NotBlank String gradeName) {
}
```

```java
package ${package}.facade.dto.teaching;

import jakarta.validation.constraints.NotBlank;

public record AssignUserToClassRequest(@NotBlank String userId, @NotBlank String schoolClassId) {
}
```

```java
package ${package}.facade.dto.teaching;

import java.util.List;

public record SchoolClassDTO(String id, String name, String gradeName, List<String> userIds) {
}
```

```java
package ${package}.facade.user;

import ${package}.facade.dto.user.CreateUserRequest;
import ${package}.facade.dto.user.UserDTO;

public interface UserFacade {
    UserDTO createUser(CreateUserRequest request);

    UserDTO getUser(String userId);
}
```

```java
package ${package}.facade.teaching;

import ${package}.facade.dto.teaching.AssignUserToClassRequest;
import ${package}.facade.dto.teaching.CreateSchoolClassRequest;
import ${package}.facade.dto.teaching.SchoolClassDTO;

public interface SchoolClassFacade {
    SchoolClassDTO createSchoolClass(CreateSchoolClassRequest request);

    void assignUser(AssignUserToClassRequest request);
}
```

- [ ] **Step 5: Create domain model, services, and repository ports**

Create `User.java`:

```java
package ${package}.domain.entities.user;

import ${package}.domain.enums.UserStatus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class User {
    private final String id;
    private final String name;
    private final String email;
    private final UserStatus status;
    private final List<String> schoolClassIds;

    private User(String id, String name, String email, UserStatus status, List<String> schoolClassIds) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.status = status;
        this.schoolClassIds = new ArrayList<>(schoolClassIds);
    }

    public static User create(String id, String name, String email) {
        return new User(id, name, email, UserStatus.ACTIVE, List.of());
    }

    public static User restore(String id, String name, String email, UserStatus status, List<String> schoolClassIds) {
        return new User(id, name, email, status, schoolClassIds);
    }

    public void assignToClass(String schoolClassId) {
        schoolClassIds.add(schoolClassId);
    }

    public boolean hasSchoolClass(String schoolClassId) {
        return schoolClassIds.contains(schoolClassId);
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public UserStatus getStatus() { return status; }
    public List<String> getSchoolClassIds() { return Collections.unmodifiableList(schoolClassIds); }
}
```

Create `SchoolClass.java`:

```java
package ${package}.domain.entities.teaching;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SchoolClass {
    private final String id;
    private final String name;
    private final String gradeName;
    private final List<String> userIds;

    private SchoolClass(String id, String name, String gradeName, List<String> userIds) {
        this.id = id;
        this.name = name;
        this.gradeName = gradeName;
        this.userIds = new ArrayList<>(userIds);
    }

    public static SchoolClass create(String id, String name, String gradeName) {
        return new SchoolClass(id, name, gradeName, List.of());
    }

    public static SchoolClass restore(String id, String name, String gradeName, List<String> userIds) {
        return new SchoolClass(id, name, gradeName, userIds);
    }

    public void assignUser(String userId) {
        userIds.add(userId);
    }

    public boolean hasUser(String userId) {
        return userIds.contains(userId);
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getGradeName() { return gradeName; }
    public List<String> getUserIds() { return Collections.unmodifiableList(userIds); }
}
```

Create `UserStatus.java`:

```java
package ${package}.domain.enums;

public enum UserStatus {
    ACTIVE
}
```

Create repository ports and services:

```java
package ${package}.domain.repos.user;

import ${package}.domain.entities.user.User;

import java.util.Optional;

public interface UserRepository {
    User save(User user);
    Optional<User> findById(String userId);
    boolean existsByEmail(String email);
}
```

```java
package ${package}.domain.repos.teaching;

import ${package}.domain.entities.teaching.SchoolClass;

import java.util.Optional;

public interface SchoolClassRepository {
    SchoolClass save(SchoolClass schoolClass);
    Optional<SchoolClass> findById(String schoolClassId);
}
```

```java
package ${package}.domain.service.user;

import ${package}.common.constants.ErrorCodes;
import ${package}.common.exceptions.BizException;
import ${package}.domain.entities.user.User;

public class UserDomainService {
    public User create(String userId, String name, String email) {
        return User.create(userId, name, email);
    }

    public User assignClass(User user, String schoolClassId) {
        if (user.hasSchoolClass(schoolClassId)) {
            throw new BizException(ErrorCodes.SCHOOL_CLASS_USER_DUPLICATED, "user already assigned to school class");
        }
        user.assignToClass(schoolClassId);
        return user;
    }
}
```

```java
package ${package}.domain.service.teaching;

import ${package}.common.constants.ErrorCodes;
import ${package}.common.exceptions.BizException;
import ${package}.domain.entities.teaching.SchoolClass;

public class SchoolClassDomainService {
    public SchoolClass create(String schoolClassId, String name, String gradeName) {
        return SchoolClass.create(schoolClassId, name, gradeName);
    }

    public SchoolClass assignUser(SchoolClass schoolClass, String userId) {
        if (schoolClass.hasUser(userId)) {
            throw new BizException(ErrorCodes.SCHOOL_CLASS_USER_DUPLICATED, "user already assigned to school class");
        }
        schoolClass.assignUser(userId);
        return schoolClass;
    }
}
```

- [ ] **Step 6: Create application APIs and implementations**

Create records and interfaces:

```java
package ${package}.application.manage.user;

import java.util.List;

public record UserView(String id, String name, String email, String status, List<String> schoolClassIds) {
}
```

```java
package ${package}.application.manage.user;

public interface UserManage {
    UserView create(String name, String email);
    UserView getById(String userId);
}
```

```java
package ${package}.application.manage.teaching;

import java.util.List;

public record SchoolClassView(String id, String name, String gradeName, List<String> userIds) {
}
```

```java
package ${package}.application.manage.teaching;

public interface SchoolClassManage {
    SchoolClassView create(String name, String gradeName);
    void assignUser(String userId, String schoolClassId);
}
```

Create implementations:

```java
package ${package}.application.manage.user.impl;

import ${package}.application.manage.user.UserManage;
import ${package}.application.manage.user.UserView;
import ${package}.common.constants.ErrorCodes;
import ${package}.common.exceptions.BizException;
import ${package}.common.exceptions.NotFoundException;
import ${package}.common.utils.IdGenerator;
import ${package}.domain.entities.user.User;
import ${package}.domain.repos.user.UserRepository;
import ${package}.domain.service.user.UserDomainService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserManageImpl implements UserManage {
    private final UserRepository userRepository;
    private final UserDomainService userDomainService = new UserDomainService();

    public UserManageImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public UserView create(String name, String email) {
        if (userRepository.existsByEmail(email)) {
            throw new BizException(ErrorCodes.USER_EMAIL_DUPLICATED, "user email already exists");
        }
        User user = userDomainService.create(IdGenerator.nextId(), name, email);
        return toView(userRepository.save(user));
    }

    @Override
    @Transactional(readOnly = true)
    public UserView getById(String userId) {
        return userRepository.findById(userId)
                .map(this::toView)
                .orElseThrow(() -> new NotFoundException(ErrorCodes.USER_NOT_FOUND, "user not found"));
    }

    private UserView toView(User user) {
        return new UserView(user.getId(), user.getName(), user.getEmail(), user.getStatus().name(), user.getSchoolClassIds());
    }
}
```

```java
package ${package}.application.manage.teaching.impl;

import ${package}.application.manage.teaching.SchoolClassManage;
import ${package}.application.manage.teaching.SchoolClassView;
import ${package}.common.constants.ErrorCodes;
import ${package}.common.exceptions.NotFoundException;
import ${package}.common.utils.IdGenerator;
import ${package}.domain.entities.teaching.SchoolClass;
import ${package}.domain.entities.user.User;
import ${package}.domain.repos.teaching.SchoolClassRepository;
import ${package}.domain.repos.user.UserRepository;
import ${package}.domain.service.teaching.SchoolClassDomainService;
import ${package}.domain.service.user.UserDomainService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SchoolClassManageImpl implements SchoolClassManage {
    private final SchoolClassRepository schoolClassRepository;
    private final UserRepository userRepository;
    private final SchoolClassDomainService schoolClassDomainService = new SchoolClassDomainService();
    private final UserDomainService userDomainService = new UserDomainService();

    public SchoolClassManageImpl(SchoolClassRepository schoolClassRepository, UserRepository userRepository) {
        this.schoolClassRepository = schoolClassRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public SchoolClassView create(String name, String gradeName) {
        SchoolClass schoolClass = schoolClassDomainService.create(IdGenerator.nextId(), name, gradeName);
        return toView(schoolClassRepository.save(schoolClass));
    }

    @Override
    @Transactional
    public void assignUser(String userId, String schoolClassId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(ErrorCodes.USER_NOT_FOUND, "user not found"));
        SchoolClass schoolClass = schoolClassRepository.findById(schoolClassId)
                .orElseThrow(() -> new NotFoundException(ErrorCodes.SCHOOL_CLASS_NOT_FOUND, "school class not found"));
        schoolClassRepository.save(schoolClassDomainService.assignUser(schoolClass, userId));
        userRepository.save(userDomainService.assignClass(user, schoolClassId));
    }

    private SchoolClassView toView(SchoolClass schoolClass) {
        return new SchoolClassView(schoolClass.getId(), schoolClass.getName(), schoolClass.getGradeName(), schoolClass.getUserIds());
    }
}
```

- [ ] **Step 7: Run source validation**

Run:

```bash
bash ./mvnw -B -ntp -pl egon-cola-archetypes/egon-cola-archetype-web -am test
```

Expected: PASS for generated project `validate`.

- [ ] **Step 8: Commit Task 3**

```bash
git add egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-common \
        egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-facade \
        egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-domain \
        egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-application \
        egon-cola-archetypes/egon-cola-archetype-web/src/test/resources/projects/basic/verify.groovy
git commit -m "feat: add organization web archetype domain flow"
```

---

### Task 4: Add Infrastructure, Adapter, Starter, and Generated Tests

**Files:**
- Create Java source under:
  - `__rootArtifactId__-infrastructure/src/main/java`
  - `__rootArtifactId__-adapter/src/main/java`
  - `__rootArtifactId__-starter/src/main/java`
- Create resources:
  - `__rootArtifactId__-infrastructure/src/main/resources/db/migration/V1__init_student_management_organization.sql`
  - `__rootArtifactId__-starter/src/main/resources/application.yml`
- Create tests:
  - `__rootArtifactId__-starter/src/test/java/ArchitectureDependencyTest.java`
  - `__rootArtifactId__-starter/src/test/java/OrganizationFlowTest.java`
- Modify: `egon-cola-archetypes/egon-cola-archetype-web/src/test/resources/projects/basic/goal.txt`
- Modify: `egon-cola-archetypes/egon-cola-archetype-web/src/test/resources/projects/basic/verify.groovy`

- [ ] **Step 1: Switch generated goal to test and extend guards**

Replace `goal.txt` with:

```text
test
```

Append to `verify.groovy`:

```groovy
assertFile("student-management-organization-infrastructure/src/main/resources/db/migration/V1__init_student_management_organization.sql")
assertFile("student-management-organization-adapter/src/main/java/it/pkg/adapter/controller/user/UserController.java")
assertFile("student-management-organization-adapter/src/main/java/it/pkg/adapter/controller/teaching/SchoolClassController.java")
assertFile("student-management-organization-starter/src/main/java/it/pkg/starter/OrganizationApplication.java")
assertFile("student-management-organization-starter/src/test/java/it/pkg/starter/OrganizationFlowTest.java")
assertFile("student-management-organization-starter/src/test/java/it/pkg/starter/ArchitectureDependencyTest.java")

def migrationDir = new File(basedir, "student-management-organization-infrastructure/src/main/resources/db/migration")
assert migrationDir.listFiles({ dir, name -> name.endsWith(".sql") } as FilenameFilter).size() == 1
```

- [ ] **Step 2: Run validation and verify it fails**

Run:

```bash
bash ./mvnw -B -ntp -pl egon-cola-archetypes/egon-cola-archetype-web -am test
```

Expected: FAIL because infrastructure, adapter, starter, and tests do not exist.

- [ ] **Step 3: Create infrastructure persistence classes**

Create PO classes `UserPo`, `SchoolClassPo`, and `SchoolClassUserPo` in `infrastructure.repo.*.po`, Spring Data repositories in `infrastructure.repo.*.jpa`, converters in `infrastructure.repo.*.converter`, and repository implementations in `infrastructure.repo.*.impl`.

Use these persistence rules:

```java
// UserRepositoryImpl.save(User user)
// 1. Save UserPo(id, name, email, status, LocalDateTime.now()).
// 2. For each user.getSchoolClassIds(), insert SchoolClassUserPo if it does not already exist.
// 3. Restore User from saved UserPo and all assignments for that user.

// SchoolClassRepositoryImpl.save(SchoolClass schoolClass)
// 1. Save SchoolClassPo(id, name, gradeName, LocalDateTime.now()).
// 2. For each schoolClass.getUserIds(), insert SchoolClassUserPo if it does not already exist.
// 3. Restore SchoolClass from saved SchoolClassPo and all assignments for that class.
```

The generated implementation must expose these exact Spring Data methods:

```java
boolean UserJpaRepository.existsByEmail(String email);
List<SchoolClassUserPo> SchoolClassUserJpaRepository.findByUserId(String userId);
List<SchoolClassUserPo> SchoolClassUserJpaRepository.findBySchoolClassId(String schoolClassId);
boolean SchoolClassUserJpaRepository.existsByUserIdAndSchoolClassId(String userId, String schoolClassId);
```

Create `V1__init_student_management_organization.sql`:

```sql
CREATE TABLE users (
    id VARCHAR(64) PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    email VARCHAR(160) NOT NULL UNIQUE,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE school_classes (
    id VARCHAR(64) PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    grade_name VARCHAR(120) NOT NULL,
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE school_class_users (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL,
    school_class_id VARCHAR(64) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_school_class_user UNIQUE (user_id, school_class_id)
);
```

- [ ] **Step 4: Create adapter controllers, converters, facade implementations, and handler**

Create `UserAdapterConverter` and `SchoolClassAdapterConverter` that map application views to facade DTOs.

Create `UserController`:

```java
package ${package}.adapter.controller.user;

import ${package}.adapter.convertor.UserAdapterConverter;
import ${package}.application.manage.user.UserManage;
import ${package}.common.response.SingleResponse;
import ${package}.facade.dto.user.CreateUserRequest;
import ${package}.facade.dto.user.UserDTO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
public class UserController {
    private final UserManage userManage;

    public UserController(UserManage userManage) {
        this.userManage = userManage;
    }

    @PostMapping
    public SingleResponse<UserDTO> create(@Valid @RequestBody CreateUserRequest request) {
        return SingleResponse.of(UserAdapterConverter.toDto(userManage.create(request.name(), request.email())));
    }

    @GetMapping("/{userId}")
    public SingleResponse<UserDTO> getById(@PathVariable String userId) {
        return SingleResponse.of(UserAdapterConverter.toDto(userManage.getById(userId)));
    }
}
```

Create `SchoolClassController`:

```java
package ${package}.adapter.controller.teaching;

import ${package}.adapter.convertor.SchoolClassAdapterConverter;
import ${package}.application.manage.teaching.SchoolClassManage;
import ${package}.common.response.Response;
import ${package}.common.response.SingleResponse;
import ${package}.facade.dto.teaching.CreateSchoolClassRequest;
import ${package}.facade.dto.teaching.SchoolClassDTO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/school-classes")
public class SchoolClassController {
    private final SchoolClassManage schoolClassManage;

    public SchoolClassController(SchoolClassManage schoolClassManage) {
        this.schoolClassManage = schoolClassManage;
    }

    @PostMapping
    public SingleResponse<SchoolClassDTO> create(@Valid @RequestBody CreateSchoolClassRequest request) {
        return SingleResponse.of(SchoolClassAdapterConverter.toDto(schoolClassManage.create(request.name(), request.gradeName())));
    }

    @PostMapping("/{schoolClassId}/users/{userId}")
    public Response assignUser(@PathVariable String schoolClassId, @PathVariable String userId) {
        schoolClassManage.assignUser(userId, schoolClassId);
        return Response.success();
    }
}
```

Create `UserFacadeImpl`, `SchoolClassFacadeImpl`, and `GlobalExceptionHandler` with `BizException` mapped to `Response.fail(exception.getCode(), exception.getMessage())`.

- [ ] **Step 5: Create starter application and config**

Create `OrganizationApplication.java`:

```java
package ${package}.starter;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "${package}")
public class OrganizationApplication {
    public static void main(String[] args) {
        SpringApplication.run(OrganizationApplication.class, args);
    }
}
```

Create `application.yml`:

```yaml
spring:
  application:
    name: ${rootArtifactId}
  datasource:
    url: jdbc:h2:mem:${rootArtifactId};MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH
    username: sa
    password:
    driver-class-name: org.h2.Driver
  flyway:
    enabled: true
  jpa:
    hibernate:
      ddl-auto: validate
    open-in-view: false
management:
  endpoints:
    web:
      exposure:
        include: health,info
```

- [ ] **Step 6: Add generated tests**

Create `OrganizationFlowTest.java`:

```java
#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.starter;

import ${package}.application.manage.teaching.SchoolClassManage;
import ${package}.application.manage.user.UserManage;
import ${package}.application.manage.user.UserView;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = OrganizationApplication.class)
class OrganizationFlowTest {
    @Autowired
    private UserManage userManage;

    @Autowired
    private SchoolClassManage schoolClassManage;

    @Test
    void create_user_and_assign_to_school_class() {
        UserView user = userManage.create("Mario", "mario@example.com");
        var schoolClass = schoolClassManage.create("Class One", "Grade One");

        schoolClassManage.assignUser(user.id(), schoolClass.id());

        UserView saved = userManage.getById(user.id());
        assertThat(saved.email()).isEqualTo("mario@example.com");
        assertThat(saved.schoolClassIds()).containsExactly(schoolClass.id());
    }
}
```

Create `ArchitectureDependencyTest.java`:

```java
#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.starter;

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
                        "${package}.application..",
                        "${package}.adapter..",
                        "${package}.facade..",
                        "${package}.infrastructure..",
                        "${package}.starter..")
                .check(classes);
    }

    @Test
    void facade_does_not_depend_on_internal_layers() {
        noClasses().that().resideInAPackage("${package}.facade..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "${package}.common..",
                        "${package}.domain..",
                        "${package}.application..",
                        "${package}.adapter..",
                        "${package}.infrastructure..",
                        "${package}.starter..")
                .check(classes);
    }

    @Test
    void adapter_does_not_depend_on_infrastructure() {
        noClasses().that().resideInAPackage("${package}.adapter..")
                .should().dependOnClassesThat().resideInAPackage("${package}.infrastructure..")
                .check(classes);
    }
}
```

- [ ] **Step 7: Run generated test validation**

Run:

```bash
bash ./mvnw -B -ntp -pl egon-cola-archetypes/egon-cola-archetype-web -am test
```

Expected: PASS. If generated tests fail because the starter test class cannot see test dependencies, add `spring-boot-starter-test` and `archunit-junit5` to the starter module POM.

- [ ] **Step 8: Commit Task 4**

```bash
git add egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure \
        egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-adapter \
        egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-starter \
        egon-cola-archetypes/egon-cola-archetype-web/src/test/resources/projects/basic
git commit -m "feat: add organization web archetype runtime sample"
```

---

### Task 5: Final README, Metadata Cleanup, and End-to-End Validation

**Files:**
- Modify: `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/README.md`
- Modify: `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/META-INF/maven/archetype-metadata.xml`
- Modify: `egon-cola-archetypes/egon-cola-archetype-web/src/test/resources/projects/basic/verify.groovy`

- [ ] **Step 1: Add cleanup checks**

Append to `verify.groovy`:

```groovy
def readme = assertFile("README.md").text
assert readme.contains("Student Management Organization")
assert readme.contains("only the organization Project")
assert readme.contains("user")
assert readme.contains("teaching")
assert !readme.contains("student-management-evaluation")

def files = []
new File(basedir, ".").eachFileRecurse { file -> if (file.isFile()) files << file }
def forbidden = ["__rootArtifactId__-client", "__rootArtifactId__-app", "app1", "app2", "customer", "order", "examing"]
forbidden.each { token ->
    assert files.every { !it.text.contains(token) }
}
```

- [ ] **Step 2: Run cleanup validation and verify it fails if stale content remains**

Run:

```bash
bash ./mvnw -B -ntp -pl egon-cola-archetypes/egon-cola-archetype-web -am test
```

Expected: FAIL if README, metadata, or generated source still has stale `client/app/start/app1/app2/customer/order/examing` content.

- [ ] **Step 3: Replace README with final content**

Replace `README.md`:

```markdown
# Student Management Organization

This project is generated by `egon-cola-archetype-web`.

It creates one independent Project: `student-management-organization`. It does not generate the evaluation Project.

## Modules

```text
student-management-organization-common
student-management-organization-facade
student-management-organization-domain
student-management-organization-application
student-management-organization-infrastructure
student-management-organization-adapter
student-management-organization-starter
```

## Domains

`user` contains user account behavior.

`teaching` contains school class behavior.

The sample flow creates a user, creates a school class, and assigns the user to that class through the application layer.

## Dependency Direction

```text
starter -> adapter, infrastructure
adapter -> application, facade, common
application -> domain, common
domain -> common
infrastructure -> domain, application, common
facade -> validation and JDK only
common -> JDK only
```

`adapter` must not call infrastructure directly. `domain` must not depend on outer layers. `facade` must not depend on internal modules.

## Commands

```bash
./mvnw test
./mvnw -DskipTests package
./mvnw -pl student-management-organization-starter spring-boot:run
```

The default datasource is H2. Tests and packaging do not require an external database.
```

- [ ] **Step 4: Final metadata cleanup**

Ensure `archetype-metadata.xml` has no module id, dir, or name for:

```text
${rootArtifactId}-client
${rootArtifactId}-app
start
```

Ensure it has module entries and fileSets for:

```text
${rootArtifactId}-common
${rootArtifactId}-facade
${rootArtifactId}-domain
${rootArtifactId}-application
${rootArtifactId}-infrastructure
${rootArtifactId}-adapter
${rootArtifactId}-starter
```

- [ ] **Step 5: Run focused and generated-wrapper validation**

Run:

```bash
bash ./mvnw -B -ntp -pl egon-cola-archetypes/egon-cola-archetype-web -am test
generated_dir="$(find egon-cola-archetypes/egon-cola-archetype-web/target -path '*/projects/basic/project/student-management-organization/pom.xml' -print -quit | xargs dirname)"
test -n "$generated_dir"
bash "$generated_dir/mvnw" -B -ntp -f "$generated_dir/pom.xml" test
```

Expected: both Maven commands PASS and no application is started.

- [ ] **Step 6: Search for stale template content**

Run:

```bash
rg -n "rootArtifactId__-client|rootArtifactId__-app|<module>start</module>|app1|app2|customer|order|examing|student-management-evaluation" \
  egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources \
  egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/META-INF/maven/archetype-metadata.xml
```

Expected: no matches. `rg` exits 1 when no matches are found; exit code 1 is expected for this search.

- [ ] **Step 7: Commit Task 5**

```bash
git add egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/README.md \
        egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/META-INF/maven/archetype-metadata.xml \
        egon-cola-archetypes/egon-cola-archetype-web/src/test/resources/projects/basic/verify.groovy
git commit -m "docs: finalize organization web archetype readme"
```

## Self-Review Notes

Spec coverage:

- One generated `student-management-organization` Project is covered by Tasks 1 and 2.
- No generated evaluation Project is guarded in Tasks 1 and 5.
- Module renaming to `common/facade/domain/application/infrastructure/adapter/starter` is covered by Tasks 1 and 2.
- Root Spring Boot parent, Java 21, Maven Wrapper 3.9.14, `.gitignore`, `.gitattributes`, and README are covered by Tasks 1 and 5.
- User and teaching sample behavior is covered by Tasks 3 and 4.
- Flyway migration and H2 default validation are covered by Task 4.
- Generated tests and dependency boundaries are covered by Task 4.
- No `spring-boot:run` command is used in validation.

Design pattern decision:

- No Strategy, Factory Method, Decorator, State, or Chain of Responsibility implementation is planned.
- The design uses Maven module boundaries, domain repository ports, infrastructure adapters, facade interfaces, and application services, matching the architecture document.
