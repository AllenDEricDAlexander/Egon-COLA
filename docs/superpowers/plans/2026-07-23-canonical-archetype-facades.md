# Canonical Archetype Facades Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Publish two canonical Facade JARs and make the web and service archetypes generate independently without custom Facade parameters or Maven dependency cycles.

**Architecture:** `egon-cola-organization-facade` and `egon-cola-evaluation-facade` become stable contract-only modules in the archetypes reactor. Generated web and service projects both depend on those artifacts: their adapter modules implement the canonical provider contracts and their infrastructure modules consume the opposite contract. The generated local Facade modules and test-only fixture artifacts are removed so every provider and consumer compiles against the same FQCNs.

**Tech Stack:** Java 21, Maven 3.9.x, Maven Archetype Plugin 3.4.1, Jakarta Validation, Lombok, JUnit 5, Groovy archetype verifiers.

## Global Constraints

- Canonical coordinates are `top.egon:egon-cola-organization-facade:${egon-cola.version}` and `top.egon:egon-cola-evaluation-facade:${egon-cola.version}`.
- Stable package roots are `top.egon.cola.organization.facade` and `top.egon.cola.evaluation.facade`.
- Facade modules contain contracts only and must not depend on generated web/service implementations or infrastructure.
- Generated web/service projects contain six modules: `common`, `domain`, `application`, `infrastructure`, `adapter`, and `starter`.
- Generated providers and consumers use the same canonical contract classes and FQCNs.
- Facade versions use the generated `${egon-cola.version}` property; do not add another release literal.
- Remove all eight `organizationFacade*` and `evaluationFacade*` archetype inputs.
- Preserve existing method semantics, DTO fields, error codes, Dubbo metadata, persistence, and runtime profiles.
- Do not edit existing Flyway migrations or add a migration.
- Do not start generated applications, containers, databases, brokers, browsers, or external services.
- Local verification may overwrite local `5.2.3` artifacts; a later Central release must use `5.2.4` because published `5.2.3` archetypes are immutable.
- Preserve the user's existing `README.md` and `cola-samples/` worktree changes.

---

## File Structure

### New canonical modules

```text
egon-cola-archetypes/
├── egon-cola-organization-facade/
│   ├── pom.xml
│   └── src/
│       ├── main/java/top/egon/cola/organization/facade/**
│       └── test/java/top/egon/cola/organization/facade/OrganizationFacadeContractTest.java
└── egon-cola-evaluation-facade/
    ├── pom.xml
    └── src/
        ├── main/java/top/egon/cola/evaluation/facade/**
        └── test/java/top/egon/cola/evaluation/facade/EvaluationFacadeContractTest.java
```

The organization module owns the current web archetype's `facade/user`,
`facade/teaching`, and `facade/exceptions` contracts. The evaluation module owns
the current service archetype's `facade/course`, `facade/exam`, `facade/dto`,
`facade/enums`, `facade/exceptions`, and `facade/utils` contracts.

### Modified archetype surfaces

```text
egon-cola-archetypes/pom.xml
egon-cola-archetypes/egon-cola-archetype-web/
├── pom.xml
├── multi-project-multi-module-architecture.md
└── src/{main,test}/...
egon-cola-archetypes/egon-cola-archetype-service/
├── pom.xml
├── student-management-service-only-rpc-mq-architecture.md
└── src/{main,test}/...
```

The generated `__rootArtifactId__-facade` trees are deleted only after their
canonical replacements compile and their provider/consumer imports are rewired.

---

### Task 1: Add the Canonical Organization Facade Artifact

**Files:**
- Modify: `egon-cola-archetypes/pom.xml`
- Create: `egon-cola-archetypes/egon-cola-organization-facade/pom.xml`
- Create: `egon-cola-archetypes/egon-cola-organization-facade/src/main/java/top/egon/cola/organization/facade/**`
- Create: `egon-cola-archetypes/egon-cola-organization-facade/src/test/java/top/egon/cola/organization/facade/OrganizationFacadeContractTest.java`
- Source reference: `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-facade/src/{main,test}/java/**`

**Interfaces:**
- Consumes: the current organization Facade interfaces, DTO fields, validation annotations, and exception behavior without semantic changes.
- Produces: Maven coordinate `top.egon:egon-cola-organization-facade:5.2.3` and package root `top.egon.cola.organization.facade`.

- [ ] **Step 1: Register the failing reactor module**

Insert the module before all archetype modules:

```xml
<modules>
    <module>egon-cola-organization-facade</module>
    <module>egon-cola-archetype-light</module>
    <module>egon-cola-archetype-service</module>
    <module>egon-cola-archetype-web</module>
</modules>
```

- [ ] **Step 2: Run Maven to verify the new module is absent**

Run:

```bash
./mvnw -B -ntp -f egon-cola-archetypes/pom.xml \
  -pl egon-cola-organization-facade -am test
```

Expected: FAIL because `egon-cola-organization-facade/pom.xml` does not exist.

- [ ] **Step 3: Add the organization Facade POM**

Create this contract-only module:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>top.egon</groupId>
        <artifactId>egon-cola-archetypes-parent</artifactId>
        <version>5.2.3</version>
    </parent>
    <artifactId>egon-cola-organization-facade</artifactId>
    <packaging>jar</packaging>
    <name>egon-cola-organization-facade</name>
    <description>Canonical organization contracts shared by Egon-COLA archetype providers and consumers.</description>
    <dependencies>
        <dependency>
            <groupId>jakarta.validation</groupId>
            <artifactId>jakarta.validation-api</artifactId>
        </dependency>
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <scope>provided</scope>
        </dependency>
        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.hibernate.validator</groupId>
            <artifactId>hibernate-validator</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>com.fasterxml.jackson.core</groupId>
            <artifactId>jackson-databind</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>
</project>
```

- [ ] **Step 4: Copy the organization contracts into the stable package**

Copy every production type from the web Facade template, preserving subpackages
and type bodies, and perform only this package-root transformation:

```text
${package}.facade
-> top.egon.cola.organization.facade
```

Move the existing `OrganizationFacadeContractTest` behavior into:

```java
package top.egon.cola.organization.facade;
```

and update all imports to `top.egon.cola.organization.facade.*`. Do not rename
interfaces, methods, DTOs, fields, enums, or exceptions.

- [ ] **Step 5: Run the focused module test**

Run:

```bash
./mvnw -B -ntp -f egon-cola-archetypes/pom.xml \
  -pl egon-cola-organization-facade -am clean test
```

Expected: BUILD SUCCESS; `OrganizationFacadeContractTest` passes.

- [ ] **Step 6: Audit the artifact boundary**

Run:

```bash
rg -n "org\\.springframework|infrastructure|adapter|repository|datasource" \
  egon-cola-archetypes/egon-cola-organization-facade/src/main/java
jar tf egon-cola-archetypes/egon-cola-organization-facade/target/egon-cola-organization-facade-5.2.3.jar \
  | rg '^top/egon/cola/organization/facade/'
```

Expected: the first command finds no implementation dependency; the second lists
the canonical organization contracts.

- [ ] **Step 7: Commit the organization contract**

```bash
git add egon-cola-archetypes/pom.xml \
  egon-cola-archetypes/egon-cola-organization-facade
git commit -m "feat(archetype): publish organization facade contract"
```

---

### Task 2: Add the Canonical Evaluation Facade Artifact

**Files:**
- Modify: `egon-cola-archetypes/pom.xml`
- Create: `egon-cola-archetypes/egon-cola-evaluation-facade/pom.xml`
- Create: `egon-cola-archetypes/egon-cola-evaluation-facade/src/main/java/top/egon/cola/evaluation/facade/**`
- Create: `egon-cola-archetypes/egon-cola-evaluation-facade/src/test/java/top/egon/cola/evaluation/facade/EvaluationFacadeContractTest.java`
- Source reference: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-facade/src/{main,test}/java/**`

**Interfaces:**
- Consumes: the current evaluation Facade interfaces, DTO fields, protocol enums, assertion behavior, and exception behavior without semantic changes.
- Produces: Maven coordinate `top.egon:egon-cola-evaluation-facade:5.2.3` and package root `top.egon.cola.evaluation.facade`.

- [ ] **Step 1: Register the failing reactor module**

The final leading module order is:

```xml
<modules>
    <module>egon-cola-organization-facade</module>
    <module>egon-cola-evaluation-facade</module>
    <module>egon-cola-archetype-light</module>
    <module>egon-cola-archetype-service</module>
    <module>egon-cola-archetype-web</module>
</modules>
```

- [ ] **Step 2: Run Maven to verify the new module is absent**

Run:

```bash
./mvnw -B -ntp -f egon-cola-archetypes/pom.xml \
  -pl egon-cola-evaluation-facade -am test
```

Expected: FAIL because `egon-cola-evaluation-facade/pom.xml` does not exist.

- [ ] **Step 3: Add the evaluation Facade POM**

Create:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>top.egon</groupId>
        <artifactId>egon-cola-archetypes-parent</artifactId>
        <version>5.2.3</version>
    </parent>
    <artifactId>egon-cola-evaluation-facade</artifactId>
    <packaging>jar</packaging>
    <name>egon-cola-evaluation-facade</name>
    <description>Canonical evaluation contracts shared by Egon-COLA archetype providers and consumers.</description>
    <dependencies>
        <dependency>
            <groupId>jakarta.validation</groupId>
            <artifactId>jakarta.validation-api</artifactId>
        </dependency>
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <scope>provided</scope>
        </dependency>
        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>
</project>
```

- [ ] **Step 4: Copy the evaluation contracts into the stable package**

Copy every production type from the service Facade template, preserving
subpackages and type bodies, and perform only this package-root transformation:

```text
${package}.facade
-> top.egon.cola.evaluation.facade
```

Move the current `EvaluationFacadeContractTest` behavior into:

```java
package top.egon.cola.evaluation.facade;
```

and update its imports to `top.egon.cola.evaluation.facade.*`. Preserve all
course, exam, score, response, error-code, and validation semantics.

- [ ] **Step 5: Run the focused module test**

Run:

```bash
./mvnw -B -ntp -f egon-cola-archetypes/pom.xml \
  -pl egon-cola-evaluation-facade -am clean test
```

Expected: BUILD SUCCESS; `EvaluationFacadeContractTest` passes.

- [ ] **Step 6: Audit the artifact boundary**

Run:

```bash
rg -n "org\\.springframework|infrastructure|adapter|repository|datasource" \
  egon-cola-archetypes/egon-cola-evaluation-facade/src/main/java
jar tf egon-cola-archetypes/egon-cola-evaluation-facade/target/egon-cola-evaluation-facade-5.2.3.jar \
  | rg '^top/egon/cola/evaluation/facade/'
```

Expected: the first command finds no implementation dependency; the second lists
the canonical evaluation contracts.

- [ ] **Step 7: Commit the evaluation contract**

```bash
git add egon-cola-archetypes/pom.xml \
  egon-cola-archetypes/egon-cola-evaluation-facade
git commit -m "feat(archetype): publish evaluation facade contract"
```

---

### Task 3: Rewire the Service Archetype to Canonical Contracts

**Files:**
- Modify: `egon-cola-archetypes/egon-cola-archetype-service/pom.xml`
- Modify: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/META-INF/maven/archetype-metadata.xml`
- Modify: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/pom.xml`
- Modify: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-adapter/pom.xml`
- Modify: service adapter production and test Java files importing `${package}.facade.*`
- Modify: service infrastructure production and test Java files importing `${organizationFacadePackage}.facade.*`
- Delete: `egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-facade/**`
- Delete: `egon-cola-archetypes/egon-cola-archetype-service/src/test/java/fixture/**`
- Modify: `egon-cola-archetypes/egon-cola-archetype-service/src/test/resources/projects/basic/archetype.properties`
- Modify: `egon-cola-archetypes/egon-cola-archetype-service/src/test/resources/projects/basic/verify.groovy`
- Modify: service generated README pair and living architecture document.

**Interfaces:**
- Consumes: both canonical coordinates and stable package roots from Tasks 1-2.
- Produces: a six-module service project whose adapter provides evaluation contracts and whose infrastructure consumes organization contracts.

- [ ] **Step 1: Make the default generation fixture omit custom Facade inputs**

Delete these properties:

```properties
organizationFacadeGroupId=fixture.organization
organizationFacadeArtifactId=student-management-organization-facade
organizationFacadeVersion=1.0.0-fixture
organizationFacadePackage=fixture.organization
```

Extend `verify.groovy` so it expects:

```groovy
assert rootPom.modules.module*.text() == [
        "${artifactId}-common",
        "${artifactId}-domain",
        "${artifactId}-application",
        "${artifactId}-infrastructure",
        "${artifactId}-adapter",
        "${artifactId}-starter"
]
assert !new File(basedir, "${artifactId}-facade").exists()
assert rootPom.properties.'organization-facade.group-id'.text() == "top.egon"
assert rootPom.properties.'organization-facade.artifact-id'.text() == "egon-cola-organization-facade"
assert rootPom.properties.'organization-facade.version'.text() == '${egon-cola.version}'
assert rootPom.properties.'organization-facade.package'.text() == "top.egon.cola.organization"
assert rootPom.properties.'evaluation-facade.group-id'.text() == "top.egon"
assert rootPom.properties.'evaluation-facade.artifact-id'.text() == "egon-cola-evaluation-facade"
assert rootPom.properties.'evaluation-facade.version'.text() == '${egon-cola.version}'
assert rootPom.properties.'evaluation-facade.package'.text() == "top.egon.cola.evaluation"
```

- [ ] **Step 2: Run the service IT and verify the missing-property failure**

Run:

```bash
./mvnw -B -ntp -f egon-cola-archetypes/pom.xml \
  -pl egon-cola-archetype-service -am clean install
```

Expected: FAIL with missing `organizationFacade*` properties or the old
seven-module assertions.

- [ ] **Step 3: Replace service generation parameters with canonical properties**

Remove the four `organizationFacade*` required properties from
`archetype-metadata.xml`. In the generated root POM use:

```xml
<organization-facade.group-id>top.egon</organization-facade.group-id>
<organization-facade.artifact-id>egon-cola-organization-facade</organization-facade.artifact-id>
<organization-facade.version>${egon-cola.version}</organization-facade.version>
<organization-facade.package>top.egon.cola.organization</organization-facade.package>
<evaluation-facade.group-id>top.egon</evaluation-facade.group-id>
<evaluation-facade.artifact-id>egon-cola-evaluation-facade</evaluation-facade.artifact-id>
<evaluation-facade.version>${egon-cola.version}</evaluation-facade.version>
<evaluation-facade.package>top.egon.cola.evaluation</evaluation-facade.package>
```

Remove `${rootArtifactId}-facade` from `<modules>` and dependency management.

- [ ] **Step 4: Rewire the service adapter and infrastructure**

In `__rootArtifactId__-adapter/pom.xml`, replace the generated local Facade
dependency with:

```xml
<dependency>
    <groupId>${evaluation-facade.group-id}</groupId>
    <artifactId>${evaluation-facade.artifact-id}</artifactId>
</dependency>
```

For all service adapter Java sources and tests, apply:

```text
${package}.facade
-> top.egon.cola.evaluation.facade
```

For service infrastructure Java sources and tests, apply:

```text
${organizationFacadePackage}.facade
-> top.egon.cola.organization.facade
```

No Java source may retain `organizationFacadePackage` or
`evaluationFacadePackage` as a Velocity token. The generated POM package
properties document the contract, while source imports use the stable canonical
FQCNs directly.

- [ ] **Step 5: Remove the duplicate service Facade and fixture lifecycle**

Delete the generated `__rootArtifactId__-facade` module from the template and
its `<module>` block from `archetype-metadata.xml`.

In the archetype module POM, remove the compiler/test-JAR/install-file executions
and add test-scoped reactor dependencies:

```xml
<dependencies>
    <dependency>
        <groupId>top.egon</groupId>
        <artifactId>egon-cola-organization-facade</artifactId>
        <version>${project.version}</version>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>top.egon</groupId>
        <artifactId>egon-cola-evaluation-facade</artifactId>
        <version>${project.version}</version>
        <scope>test</scope>
    </dependency>
</dependencies>
```

Delete `src/test/java/fixture/**`.

- [ ] **Step 6: Update service documentation**

Update the generated README pair and living architecture document to describe:

```text
common, domain, application, infrastructure, adapter, starter
```

and identify `top.egon:egon-cola-evaluation-facade` as the provider contract and
`top.egon:egon-cola-organization-facade` as the consumed contract. Remove claims
that the generated project owns a local Facade module.

- [ ] **Step 7: Verify the service archetype**

Run:

```bash
./mvnw -B -ntp -f egon-cola-archetypes/pom.xml \
  -pl egon-cola-archetype-service -am clean install
rg -n "fixture\\.organization|organizationFacade(GroupId|ArtifactId|Version|Package)|\\$\\{rootArtifactId\\}-facade" \
  egon-cola-archetypes/egon-cola-archetype-service
```

Expected: BUILD SUCCESS; the audit returns no obsolete generation parameter,
fixture package, or local Facade module references.

- [ ] **Step 8: Commit the service rewiring**

```bash
git add egon-cola-archetypes/egon-cola-archetype-service
git commit -m "refactor(archetype): share canonical service facades"
```

---

### Task 4: Rewire the Web Archetype to Canonical Contracts

**Files:**
- Modify: `egon-cola-archetypes/egon-cola-archetype-web/pom.xml`
- Modify: `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/META-INF/maven/archetype-metadata.xml`
- Modify: `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/pom.xml`
- Modify: `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-adapter/pom.xml`
- Modify: web adapter production and test Java files importing `${package}.facade.*`
- Modify: web infrastructure production and test Java files importing `${evaluationFacadePackage}.facade.*`
- Delete: `egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-facade/**`
- Delete: `egon-cola-archetypes/egon-cola-archetype-web/src/test/java/fixture/**`
- Modify: `egon-cola-archetypes/egon-cola-archetype-web/src/test/resources/projects/basic/archetype.properties`
- Modify: `egon-cola-archetypes/egon-cola-archetype-web/src/test/resources/projects/basic/verify.groovy`
- Modify: web generated README pair and living architecture document.

**Interfaces:**
- Consumes: both canonical coordinates and stable package roots from Tasks 1-2.
- Produces: a six-module web project whose adapter provides organization contracts and whose infrastructure consumes evaluation contracts.

- [ ] **Step 1: Make the default generation fixture omit custom Facade inputs**

Delete:

```properties
evaluationFacadeGroupId=fixture.evaluation
evaluationFacadeArtifactId=student-management-evaluation-facade
evaluationFacadeVersion=1.0.0-fixture
evaluationFacadePackage=fixture.evaluation
```

Add verifier assertions equivalent to Task 3, with organization as the provider
contract and evaluation as the consumed contract. Require six modules and the
absence of `${artifactId}-facade`.

- [ ] **Step 2: Run the web IT and verify the missing-property failure**

Run:

```bash
./mvnw -B -ntp -f egon-cola-archetypes/pom.xml \
  -pl egon-cola-archetype-web -am clean install
```

Expected: FAIL with missing `evaluationFacade*` properties or old seven-module
assertions.

- [ ] **Step 3: Replace web generation parameters with canonical properties**

Remove the four `evaluationFacade*` required properties from metadata and use
the same canonical property block defined in Task 3. Remove the generated local
Facade module from root modules and dependency management.

- [ ] **Step 4: Rewire the web adapter and infrastructure**

In `__rootArtifactId__-adapter/pom.xml`, use:

```xml
<dependency>
    <groupId>${organization-facade.group-id}</groupId>
    <artifactId>${organization-facade.artifact-id}</artifactId>
</dependency>
```

For all web adapter Java sources and tests, apply:

```text
${package}.facade
-> top.egon.cola.organization.facade
```

For web infrastructure Java sources and tests, apply:

```text
${evaluationFacadePackage}.facade
-> top.egon.cola.evaluation.facade
```

No generated Java source may retain either Facade package Velocity token.

- [ ] **Step 5: Remove the duplicate web Facade and fixture lifecycle**

Delete the generated `__rootArtifactId__-facade` template and its metadata module
block. Remove test-JAR fixture build/install executions and `src/test/java/fixture/**`.
Add the same two test-scoped canonical reactor dependencies shown in Task 3.

- [ ] **Step 6: Update web documentation**

Update the generated README pair and living architecture document to list six
modules and explain that organization provider contracts and evaluation consumer
contracts come from their two canonical artifacts.

- [ ] **Step 7: Verify the web archetype**

Run:

```bash
./mvnw -B -ntp -f egon-cola-archetypes/pom.xml \
  -pl egon-cola-archetype-web -am clean install
rg -n "fixture\\.evaluation|evaluationFacade(GroupId|ArtifactId|Version|Package)|\\$\\{rootArtifactId\\}-facade" \
  egon-cola-archetypes/egon-cola-archetype-web
```

Expected: BUILD SUCCESS; the audit returns no obsolete generation parameter,
fixture package, or local Facade module references.

- [ ] **Step 8: Commit the web rewiring**

```bash
git add egon-cola-archetypes/egon-cola-archetype-web
git commit -m "refactor(archetype): share canonical web facades"
```

---

### Task 5: Prove Clean Generation, No Cycles, and Release Readiness

**Files:**
- Modify if needed: `egon-cola-archetypes/egon-cola-archetype-service/src/test/resources/projects/basic/verify.groovy`
- Modify if needed: `egon-cola-archetypes/egon-cola-archetype-web/src/test/resources/projects/basic/verify.groovy`
- Modify if needed: `.github/workflows/publish-maven-central.yml`
- Preserve without staging: `README.md`, `cola-samples/**`

**Interfaces:**
- Consumes: the completed canonical modules and six-module archetypes.
- Produces: evidence that standard-input generation works from an isolated local repository and that the Maven graph has no web/service cycle.

- [ ] **Step 1: Audit dependency direction**

Run:

```bash
./mvnw -B -ntp -f egon-cola-archetypes/pom.xml \
  -DskipTests dependency:tree \
  -Dincludes=top.egon:egon-cola-organization-facade,top.egon:egon-cola-evaluation-facade
```

Expected: canonical Facades have no web/service implementation dependencies;
archetype test dependencies point toward the Facades only.

- [ ] **Step 2: Run the full archetypes reactor**

Run:

```bash
./mvnw -B -ntp -f egon-cola-archetypes/pom.xml clean install
```

Expected: five-module source reactor BUILD SUCCESS; both generated six-module
projects complete `clean verify`.

- [ ] **Step 3: Install into an isolated Maven repository**

Run:

```bash
isolated_repo="$PWD/target/canonical-facade-m2"
./mvnw -B -ntp -Dmaven.repo.local="$isolated_repo" \
  -f egon-cola-archetypes/pom.xml clean install
```

Expected: BUILD SUCCESS and both canonical Facade JARs plus all three archetypes
exist under `$isolated_repo/top/egon`.

- [ ] **Step 4: Generate service with standard Maven inputs only**

In a temporary directory, run:

```bash
project_root="$(git rev-parse --show-toplevel)"
"$project_root/mvnw" -B -ntp \
  -Dmaven.repo.local="$isolated_repo" \
  archetype:generate \
  -DarchetypeCatalog=local \
  -DarchetypeGroupId=top.egon \
  -DarchetypeArtifactId=egon-cola-archetype-service \
  -DarchetypeVersion=5.2.3 \
  -DgroupId=verify.service \
  -DartifactId=verify-service \
  -Dversion=1.0.0-SNAPSHOT \
  -Dpackage=verify.service \
  -DinteractiveMode=false
```

Then run the generated `./mvnw -B -ntp clean verify` against the same isolated
repository. Expected: generation and verification succeed without any
`organizationFacade*` parameter.

- [ ] **Step 5: Generate web with standard Maven inputs only**

Repeat Step 4 with:

```text
archetypeArtifactId=egon-cola-archetype-web
groupId=verify.web
artifactId=verify-web
package=verify.web
```

Expected: generation and verification succeed without any `evaluationFacade*`
parameter.

- [ ] **Step 6: Verify release version boundaries**

Run:

```bash
rg -n "5\\.2\\.3" \
  egon-cola-archetypes/egon-cola-{organization,evaluation}-facade \
  egon-cola-archetypes/egon-cola-archetype-{web,service} \
  -g 'pom.xml' -g '*.groovy' -g '*.xml'
./scripts/bump_cola_version.sh 5.2.3
```

Expected: module parent versions and generated `egon-cola.version` are the only
release-bound values; the bump script reports a safe no-op and recognizes both
new module POMs.

- [ ] **Step 7: Run repository hygiene checks**

Run:

```bash
git diff --check
git status --short
```

Expected: no whitespace errors; only planned files plus the user's pre-existing
`README.md` and `cola-samples/` changes are present.

- [ ] **Step 8: Commit verification hardening if files changed**

If verifier or workflow corrections were required:

```bash
git add egon-cola-archetypes .github/workflows/publish-maven-central.yml
git commit -m "test(archetype): verify canonical facade generation"
```

If no files changed in this task, do not create an empty commit.

---

## Completion Check

- [ ] Both canonical Facade coordinates install locally at `5.2.3`.
- [ ] Facade artifacts have stable packages and no web/service implementation dependency.
- [ ] Generated web and service providers implement canonical interfaces.
- [ ] Generated web and service consumers import the same canonical interfaces.
- [ ] Neither generated project contains a local Facade module.
- [ ] Standard-input CLI/IDE-equivalent generation needs no custom Facade fields.
- [ ] Both generated projects pass `clean verify`.
- [ ] The full archetypes reactor passes `clean install`.
- [ ] Isolated-repository generation proves the local-catalog workflow.
- [ ] Existing Flyway migrations and runtime behavior remain unchanged.
- [ ] User-owned `README.md` and `cola-samples/` changes remain preserved.
- [ ] No Maven Central deploy is performed; the final report records that remote correction requires `5.2.4`.
