# Egon-COLA JDK 21 POM Governance Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Upgrade Egon-COLA Maven governance to JDK 21, Maven 3.9.14, medium-stable dependency versions, and `top.egon:egon-cola-*` framework coordinates.

**Architecture:** Use three sequential commits. First set the build baseline, then normalize dependency/plugin versions, then rename framework coordinates and directories. Keep the existing Maven parent/BOM/archetype structure and do not change Java source unless validation proves a tiny compatibility fix is unavoidable.

**Tech Stack:** Maven Wrapper, Maven POM, Java 21, Spring Boot/Spring Cloud BOMs, Sonatype Central Portal Maven plugin.

---

## File Structure

- Maven baseline: `.mvn/wrapper/maven-wrapper.properties` and POMs that declare Java, compiler, javadoc, or enforcer settings.
- Dependency governance: root, component parent/BOM, archetype parent, sample parent POMs, and archetype template parent POMs.
- Naming governance: source POMs with `cola-component`, `cola-components`, `cola-archetype`, `cola-archetypes`, `cola-framework`, or `cola-mario` references, plus matching framework directories.
- Out of scope: `scripts/bash-buddy`, generated `target/` files, sample business directory names under `cola-samples`.

### Task 1: Maven And JDK Baseline

**Files:**
- Modify: `.mvn/wrapper/maven-wrapper.properties`
- Modify: `cola-components/pom.xml`
- Modify: `cola-archetypes/pom.xml`
- Modify: `cola-samples/family/pom.xml`
- Modify: `cola-samples/charge/pom.xml`
- Modify: `cola-archetypes/cola-archetype-light/src/main/resources/archetype-resources/pom.xml`
- Modify: `cola-archetypes/cola-archetype-web/src/main/resources/archetype-resources/pom.xml`
- Modify: `cola-archetypes/cola-archetype-service/src/main/resources/archetype-resources/pom.xml`

- [x] **Step 1: Confirm the exact baseline locations**

Run:

```bash
rg -n "<java\.version>|<maven\.compiler\.source>|<maven\.compiler\.target>|<requireJavaVersion>|<requireMavenVersion>|<source>17" -g 'pom.xml' -g '!target/**'
```

Expected: only source POMs appear; no `target/` files are listed.

- [x] **Step 2: Set Maven Wrapper to 3.9.14**

In `.mvn/wrapper/maven-wrapper.properties`, set:

```properties
distributionUrl=https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.14/apache-maven-3.9.14-bin.zip
```

Leave `wrapperVersion=3.3.2` unchanged unless the wrapper refuses to run.

- [x] **Step 3: Set Java settings to 21**

In every POM listed for this task, replace Java 17 settings with:

```xml
<java.version>21</java.version>
<maven.compiler.source>${java.version}</maven.compiler.source>
<maven.compiler.target>${java.version}</maven.compiler.target>
```

Where `maven-compiler-plugin` has central configuration, use:

```xml
<release>${java.version}</release>
```

Where release javadocs still specify Java 17, set:

```xml
<source>21</source>
```

Where enforcer rules exist, set:

```xml
<requireMavenVersion>
    <version>[3.9.14,)</version>
</requireMavenVersion>
<requireJavaVersion>
    <version>[21,)</version>
</requireJavaVersion>
```

- [x] **Step 4: Validate the baseline**

Run:

```bash
bash ./mvnw -v
rg -n "<java\.version>17|<source>17|<target>17|<version>3\.3\.9</version>" -g 'pom.xml' -g '!target/**'
bash ./mvnw -B -ntp -DskipTests validate
```

Expected:

- `bash ./mvnw -v` reports Apache Maven 3.9.14 and Java 21.
- The `rg` command has no output.
- Maven `validate` succeeds. If local `JAVA_HOME` is not Java 21, stop and report that environment blocker.

- [x] **Step 5: Commit task 1**

Run:

```bash
git add .mvn/wrapper/maven-wrapper.properties pom.xml cola-components/pom.xml cola-archetypes/pom.xml cola-samples/family/pom.xml cola-samples/charge/pom.xml cola-archetypes/cola-archetype-light/src/main/resources/archetype-resources/pom.xml cola-archetypes/cola-archetype-web/src/main/resources/archetype-resources/pom.xml cola-archetypes/cola-archetype-service/src/main/resources/archetype-resources/pom.xml
git commit -m "chore: upgrade maven and jdk baseline"
```

Do not stage `scripts/bash-buddy`.

### Task 2: POM Dependency And Plugin Governance

**Files:**
- Modify: `pom.xml`
- Modify: `cola-components/pom.xml`
- Modify: `cola-components/cola-components-bom/pom.xml`
- Modify: `cola-components/cola-component-unittest/pom.xml`
- Modify: `cola-components/cola-component-job/pom.xml`
- Modify: `cola-archetypes/pom.xml`
- Modify: `cola-samples/family/pom.xml`
- Modify: `cola-samples/charge/pom.xml`
- Modify: `cola-archetypes/cola-archetype-light/src/main/resources/archetype-resources/pom.xml`
- Modify: `cola-archetypes/cola-archetype-web/src/main/resources/archetype-resources/pom.xml`
- Modify: `cola-archetypes/cola-archetype-service/src/main/resources/archetype-resources/pom.xml`

- [x] **Step 1: Inspect available updates from Maven**

Run:

```bash
bash ./mvnw -B -ntp -f cola-components/pom.xml org.codehaus.mojo:versions-maven-plugin:2.18.0:display-property-updates -DallowSnapshots=false
bash ./mvnw -B -ntp -f cola-components/pom.xml org.codehaus.mojo:versions-maven-plugin:2.18.0:display-plugin-updates -DallowSnapshots=false
bash ./mvnw -B -ntp -f cola-samples/family/pom.xml org.codehaus.mojo:versions-maven-plugin:2.18.0:display-property-updates -DallowSnapshots=false
```

Expected: Maven prints candidate updates. Use the target versions below unless validation proves a candidate is incompatible.

- [x] **Step 2: Apply medium-stable dependency targets**

Use these targets in component, sample, and archetype template POMs where the properties exist:

```xml
<spring.boot.version>3.5.16</spring.boot.version>
<spring-cloud.version>2025.0.3</spring-cloud.version>
<spring-ai.version>1.1.8</spring-ai.version>
<spring-ai-alibaba.version>1.1.2.3</spring-ai-alibaba.version>
<mybatis-plus.version>3.5.16</mybatis-plus.version>
<lombok.version>1.18.46</lombok.version>
```

For the Spring Boot parent in `cola-samples/family/pom.xml`, set:

```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.5.16</version>
    <relativePath/>
</parent>
```

Keep `spring-ai.version` on the 1.x line to reduce Spring Boot 4 migration risk.

- [x] **Step 3: Replace deprecated MySQL connector coordinates**

Replace every dependency using:

```xml
<groupId>mysql</groupId>
<artifactId>mysql-connector-java</artifactId>
```

with:

```xml
<groupId>com.mysql</groupId>
<artifactId>mysql-connector-j</artifactId>
```

Remove explicit MySQL connector versions where the Spring Boot BOM manages them.

- [x] **Step 4: Normalize low-risk plugin versions**

Use Maven's plugin update output from Step 1 to upgrade only Maven core plugins already present in the POMs. Keep the Central Portal plugin at:

```xml
<central.publishing.maven.plugin.version>0.11.0</central.publishing.maven.plugin.version>
```

Do not add new plugins. Do not reintroduce `nexus-staging-maven-plugin`, `ossrh`, or `performRelease`.

- [x] **Step 5: Validate dependencies**

Run:

```bash
bash ./mvnw -B -ntp -DskipTests validate
bash ./mvnw -B -ntp -f cola-components/pom.xml -DskipTests validate
bash ./mvnw -B -ntp -f cola-archetypes/pom.xml -DskipTests validate
bash ./mvnw -B -ntp -f cola-samples/family/pom.xml -DskipTests validate
bash ./mvnw -B -ntp -f cola-samples/charge/pom.xml -DskipTests validate
```

Expected: all commands succeed. If a dependency upgrade breaks resolution, pin the nearest lower stable version that validates and record it in the commit message body.

- [x] **Step 6: Commit task 2**

Run:

```bash
git add pom.xml cola-components/pom.xml cola-components/cola-components-bom/pom.xml cola-components/cola-component-unittest/pom.xml cola-components/cola-component-job/pom.xml cola-archetypes/pom.xml cola-samples/family/pom.xml cola-samples/charge/pom.xml cola-archetypes/cola-archetype-light/src/main/resources/archetype-resources/pom.xml cola-archetypes/cola-archetype-web/src/main/resources/archetype-resources/pom.xml cola-archetypes/cola-archetype-service/src/main/resources/archetype-resources/pom.xml
git commit -m "chore: govern pom dependency versions"
```

Do not stage `scripts/bash-buddy`.

### Task 3: Egon-COLA Naming And Directory Governance

**Files:**
- Modify: all source POMs returned by:

```bash
rg -l "cola-component|cola-components|cola-archetype|cola-archetypes|cola-framework|cola-mario|com.alibaba.cola" -g 'pom.xml' -g '!target/**'
```

- Rename: framework directories listed in Step 2.

- [x] **Step 1: Rename framework directories**

Run:

```bash
git mv cola-components egon-cola-components
git mv egon-cola-components/cola-component-dto egon-cola-components/egon-cola-component-dto
git mv egon-cola-components/cola-component-exception egon-cola-components/egon-cola-component-exception
git mv egon-cola-components/cola-component-statemachine egon-cola-components/egon-cola-component-statemachine
git mv egon-cola-components/cola-component-domain-starter egon-cola-components/egon-cola-component-domain-starter
git mv egon-cola-components/cola-component-extension-starter egon-cola-components/egon-cola-component-extension-starter
git mv egon-cola-components/cola-component-catchlog-starter egon-cola-components/egon-cola-component-catchlog-starter
git mv egon-cola-components/cola-component-test-container egon-cola-components/egon-cola-component-test-container
git mv egon-cola-components/cola-components-bom egon-cola-components/egon-cola-components-bom
git mv egon-cola-components/cola-component-ruleengine egon-cola-components/egon-cola-component-ruleengine
git mv egon-cola-components/cola-component-unittest egon-cola-components/egon-cola-component-unittest
git mv egon-cola-components/cola-component-job egon-cola-components/egon-cola-component-job
git mv egon-cola-components/dev-util-archetypes egon-cola-components/egon-cola-dev-util-archetypes
git mv egon-cola-components/egon-cola-dev-util-archetypes/cola-normal-component-archetype egon-cola-components/egon-cola-dev-util-archetypes/egon-cola-normal-component-archetype
git mv egon-cola-components/egon-cola-dev-util-archetypes/cola-starter-component-archetype egon-cola-components/egon-cola-dev-util-archetypes/egon-cola-starter-component-archetype
git mv cola-archetypes egon-cola-archetypes
git mv egon-cola-archetypes/cola-archetype-light egon-cola-archetypes/egon-cola-archetype-light
git mv egon-cola-archetypes/cola-archetype-service egon-cola-archetypes/egon-cola-archetype-service
git mv egon-cola-archetypes/cola-archetype-web egon-cola-archetypes/egon-cola-archetype-web
```

Do not rename `cola-samples`.

- [x] **Step 2: Rename framework coordinates and module paths**

Apply these coordinate changes in source POMs:

```text
cola-mario-aggregation-parent -> egon-cola-aggregation-parent
cola-components-parent -> egon-cola-components-parent
cola-components-bom -> egon-cola-components-bom
cola-component-* -> egon-cola-component-*
dev-util-archetypes -> egon-cola-dev-util-archetypes
cola-normal-component-archetype -> egon-cola-normal-component-archetype
cola-starter-component-archetype -> egon-cola-starter-component-archetype
cola-framework-archetypes-parent -> egon-cola-archetypes-parent
cola-framework-archetype-light -> egon-cola-archetype-light
cola-framework-archetype-service -> egon-cola-archetype-service
cola-framework-archetype-web -> egon-cola-archetype-web
```

Update `<module>` entries to the new directory names. Preserve sample business coordinates such as `family-parent`, `family-client`, `family-app`, `family-domain`, `family-infrastructure`, `start`, and `charging-system`.

- [x] **Step 3: Verify no old framework coordinates remain**

Run:

```bash
rg -n "cola-component|cola-components|cola-archetype|cola-archetypes|cola-framework|cola-mario|com.alibaba.cola" -g 'pom.xml' -g '!target/**'
```

Expected: no output, except archetype placeholder variables or README text inside generated resources if they do not affect Maven coordinates. Any remaining source POM coordinate reference must be changed to `egon-cola-*`.

- [x] **Step 4: Validate renamed reactors and samples**

Run:

```bash
bash ./mvnw -B -ntp -DskipTests validate
bash ./mvnw -B -ntp -f egon-cola-components/pom.xml -DskipTests install
bash ./mvnw -B -ntp -f egon-cola-archetypes/pom.xml -DskipTests install
bash ./mvnw -B -ntp -f cola-samples/family/pom.xml -DskipTests validate
bash ./mvnw -B -ntp -f cola-samples/charge/pom.xml -DskipTests validate
```

Expected: all commands succeed. If validation fails due to a stale module path or coordinate, fix only the stale POM reference and rerun the failed command.

- [x] **Step 5: Commit task 3**

Run:

```bash
git add pom.xml egon-cola-components egon-cola-archetypes cola-samples
git status --short
git commit -m "refactor: rename framework coordinates to egon cola"
```

Expected: `git status --short` shows only intended POM and directory rename changes plus the pre-existing `scripts/bash-buddy` dirty entry. Do not stage `scripts/bash-buddy`.

## Self-Review

- Spec coverage: Task 1 covers Maven 3.9.14 and Java 21. Task 2 covers dependency/plugin governance. Task 3 covers `top.egon:egon-cola-*` coordinates, directory renames, sample/template references, and final validation.
- Placeholder scan: no task uses placeholder markers or an undefined implementation step.
- Scope check: no task starts the project, deploys to Maven Central, edits `target/`, or renames sample business modules.
