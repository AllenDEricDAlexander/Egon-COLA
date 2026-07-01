# POM Governance Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Govern all source POM files so this fork consistently uses `top.egon:5.1.1`, includes `cola-component-job` as a first-class component, and validates through components, archetypes, and samples.

**Architecture:** Keep the root POM as a development aggregator, make `cola-components/pom.xml` the component parent, keep `cola-components-bom` as the public dependency-management contract, and keep samples as independent validation consumers. Do not change Java source, generated `target/` files, README, scripts, or runtime behavior.

**Tech Stack:** Maven multi-module build, Java 17, Spring Boot dependency BOM, Maven Central publishing plugin, COLA component/archetype/sample POMs.

---

## File Structure

Modify these POM files:

- `pom.xml`: keep as non-publishing development aggregator; no sample module addition.
- `cola-components/pom.xml`: component parent, module list, shared dependency and plugin management, Central publishing.
- `cola-components/cola-components-bom/pom.xml`: public BOM with all `top.egon` component artifacts at `5.1.1`.
- `cola-components/cola-component-dto/pom.xml`
- `cola-components/cola-component-exception/pom.xml`
- `cola-components/cola-component-statemachine/pom.xml`
- `cola-components/cola-component-domain-starter/pom.xml`
- `cola-components/cola-component-extension-starter/pom.xml`
- `cola-components/cola-component-catchlog-starter/pom.xml`
- `cola-components/cola-component-test-container/pom.xml`
- `cola-components/cola-component-ruleengine/pom.xml`
- `cola-components/cola-component-unittest/pom.xml`
- `cola-components/cola-component-job/pom.xml`: convert from standalone project to component child module.
- `cola-components/dev-util-archetypes/cola-normal-component-archetype/pom.xml`
- `cola-components/dev-util-archetypes/cola-normal-component-archetype/src/main/resources/archetype-resources/pom.xml`
- `cola-components/dev-util-archetypes/cola-starter-component-archetype/pom.xml`
- `cola-components/dev-util-archetypes/cola-starter-component-archetype/src/main/resources/archetype-resources/pom.xml`
- `cola-archetypes/pom.xml`: archetype parent publishing plugin management.
- `cola-archetypes/cola-archetype-light/pom.xml`
- `cola-archetypes/cola-archetype-service/pom.xml`
- `cola-archetypes/cola-archetype-web/pom.xml`
- `cola-archetypes/cola-archetype-light/src/main/resources/archetype-resources/pom.xml`
- `cola-archetypes/cola-archetype-service/src/main/resources/archetype-resources/pom.xml`
- `cola-archetypes/cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-app/pom.xml`
- `cola-archetypes/cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-client/pom.xml`
- `cola-archetypes/cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-domain/pom.xml`
- `cola-archetypes/cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/pom.xml`
- `cola-archetypes/cola-archetype-service/src/main/resources/archetype-resources/start/pom.xml`
- `cola-archetypes/cola-archetype-web/src/main/resources/archetype-resources/pom.xml`
- `cola-archetypes/cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-adapter/pom.xml`
- `cola-archetypes/cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-app/pom.xml`
- `cola-archetypes/cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-client/pom.xml`
- `cola-archetypes/cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-domain/pom.xml`
- `cola-archetypes/cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/pom.xml`
- `cola-archetypes/cola-archetype-web/src/main/resources/archetype-resources/start/pom.xml`
- `cola-samples/family/pom.xml`
- `cola-samples/family/family-adapter/pom.xml`
- `cola-samples/family/family-app/pom.xml`
- `cola-samples/family/family-client/pom.xml`
- `cola-samples/family/family-domain/pom.xml`
- `cola-samples/family/family-infrastructure/pom.xml`
- `cola-samples/family/start/pom.xml`
- `cola-samples/charge/pom.xml`

Do not modify:

- Any Java file.
- Any file under `target/`.
- `README.md`, `scripts/`, `.mvn/`, `mvnw`, or generated archetype test projects.

## Task 1: Component Parent Governance

**Files:**
- Modify: `cola-components/pom.xml`

- [ ] **Step 1: Capture current component-parent drift**

Run:

```bash
rg -n "5\\.x-SNAPSHOT|3\\.3\\.0|nexus-staging-maven-plugin|central-publishing-maven-plugin|cola-component-job" cola-components/pom.xml
```

Expected before changes: output includes `5.x-SNAPSHOT`, `spring.boot.version>3.3.0`, `nexus-staging-maven-plugin`, and no active `cola-component-job` module.

- [ ] **Step 2: Update component parent identity and modules**

Edit `cola-components/pom.xml` so the identity and module section are:

```xml
    <groupId>top.egon</groupId>
    <artifactId>cola-components-parent</artifactId>
    <version>5.1.1</version>
    <packaging>pom</packaging>
```

```xml
    <modules>
        <module>cola-component-dto</module>
        <module>cola-component-exception</module>
        <module>cola-component-statemachine</module>
        <module>cola-component-domain-starter</module>
        <module>cola-component-extension-starter</module>
        <module>cola-component-catchlog-starter</module>
        <module>cola-component-test-container</module>
        <module>cola-components-bom</module>
        <module>cola-component-ruleengine</module>
        <module>cola-component-unittest</module>
        <module>cola-component-job</module>
    </modules>
```

- [ ] **Step 3: Centralize component versions**

In `cola-components/pom.xml`, replace the existing `<properties>` block with:

```xml
    <properties>
        <java.version>17</java.version>
        <maven.compiler.source>${java.version}</maven.compiler.source>
        <maven.compiler.target>${java.version}</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <spring.boot.version>3.2.10</spring.boot.version>
        <lombok.version>1.18.46</lombok.version>
        <junit.platform.version>1.9.3</junit.platform.version>
        <junit.jupiter.version>5.10.2</junit.jupiter.version>
        <embedded.redis.version>0.7.3</embedded.redis.version>
        <jedis.version>5.1.0</jedis.version>
        <spring.test.dbunit.version>5.2.0</spring.test.dbunit.version>
        <dbunit.version>2.7.0</dbunit.version>
        <wiremock.version>3.5.4</wiremock.version>
        <awaitility.version>4.2.0</awaitility.version>
        <mysql.connector.version>8.0.33</mysql.connector.version>
    </properties>
```

- [ ] **Step 4: Extend component dependency management**

In `cola-components/pom.xml`, keep the Spring Boot BOM import and make the managed direct dependencies include this exact block after `commons-cli`:

```xml
            <dependency>
                <groupId>org.junit.platform</groupId>
                <artifactId>junit-platform-launcher</artifactId>
                <version>${junit.platform.version}</version>
            </dependency>
            <dependency>
                <groupId>org.junit.jupiter</groupId>
                <artifactId>junit-jupiter-engine</artifactId>
                <version>${junit.jupiter.version}</version>
            </dependency>
            <dependency>
                <groupId>it.ozimov</groupId>
                <artifactId>embedded-redis</artifactId>
                <version>${embedded.redis.version}</version>
            </dependency>
            <dependency>
                <groupId>redis.clients</groupId>
                <artifactId>jedis</artifactId>
                <version>${jedis.version}</version>
            </dependency>
            <dependency>
                <groupId>com.github.ppodgorsek</groupId>
                <artifactId>spring-test-dbunit-core</artifactId>
                <version>${spring.test.dbunit.version}</version>
            </dependency>
            <dependency>
                <groupId>org.dbunit</groupId>
                <artifactId>dbunit</artifactId>
                <version>${dbunit.version}</version>
            </dependency>
            <dependency>
                <groupId>org.wiremock</groupId>
                <artifactId>wiremock-standalone</artifactId>
                <version>${wiremock.version}</version>
            </dependency>
            <dependency>
                <groupId>org.awaitility</groupId>
                <artifactId>awaitility</artifactId>
                <version>${awaitility.version}</version>
            </dependency>
            <dependency>
                <groupId>mysql</groupId>
                <artifactId>mysql-connector-java</artifactId>
                <version>${mysql.connector.version}</version>
            </dependency>
```

- [ ] **Step 5: Replace legacy publishing plugin management**

In `cola-components/pom.xml`, remove the plugin-management entry for `org.sonatype.plugins:nexus-staging-maven-plugin` and add this plugin-management entry:

```xml
                <plugin>
                    <groupId>org.sonatype.central</groupId>
                    <artifactId>central-publishing-maven-plugin</artifactId>
                    <version>0.11.0</version>
                    <extensions>true</extensions>
                    <configuration>
                        <publishingServerId>ossrh</publishingServerId>
                        <autoPublish>true</autoPublish>
                        <waitUntil>published</waitUntil>
                    </configuration>
                </plugin>
```

- [ ] **Step 6: Replace deploy profile plugin**

In `cola-components/pom.xml`, replace the `deploy-settings` profile plugin with:

```xml
                    <plugin>
                        <groupId>org.sonatype.central</groupId>
                        <artifactId>central-publishing-maven-plugin</artifactId>
                        <extensions>true</extensions>
                        <configuration>
                            <publishingServerId>ossrh</publishingServerId>
                            <autoPublish>true</autoPublish>
                            <waitUntil>published</waitUntil>
                        </configuration>
                    </plugin>
```

- [ ] **Step 7: Verify component parent scan**

Run:

```bash
rg -n "5\\.x-SNAPSHOT|3\\.3\\.0|nexus-staging-maven-plugin" cola-components/pom.xml
```

Expected: no output.

- [ ] **Step 8: Validate component parent model**

Run:

```bash
mvn -f cola-components/pom.xml validate -DskipTests
```

Expected: the command reaches Maven project validation. It may fail because `cola-component-job` has not been converted to a child module yet; if it fails, the error should point to `cola-component-job` parent/module model issues only.

- [ ] **Step 9: Commit**

```bash
git add cola-components/pom.xml
git commit -m "chore(pom): govern component parent"
```

## Task 2: Component Module and BOM Governance

**Files:**
- Modify: all component module POMs listed in File Structure.
- Modify: `cola-components/cola-components-bom/pom.xml`

- [ ] **Step 1: Capture current component module drift**

Run:

```bash
rg -n "5\\.x-SNAPSHOT|5\\.0\\.0|com\\.alibaba\\.cola|<version>RELEASE</version>|<name>cola-component-job</name>|<description>cola-component-job</description>" cola-components -g 'pom.xml' -g '!**/target/**'
```

Expected before changes: output includes the old component versions, the old BOM groupId entry, and incorrect names in unrelated modules.

- [ ] **Step 2: Update child parent versions**

For each component child module that already has this parent:

```xml
    <parent>
        <groupId>top.egon</groupId>
        <artifactId>cola-components-parent</artifactId>
        <version>5.x-SNAPSHOT</version>
    </parent>
```

replace only the parent version with:

```xml
        <version>5.1.1</version>
```

Affected files:

```text
cola-components/cola-component-catchlog-starter/pom.xml
cola-components/cola-component-domain-starter/pom.xml
cola-components/cola-component-dto/pom.xml
cola-components/cola-component-exception/pom.xml
cola-components/cola-component-extension-starter/pom.xml
cola-components/cola-component-ruleengine/pom.xml
cola-components/cola-component-statemachine/pom.xml
cola-components/cola-component-test-container/pom.xml
cola-components/cola-component-unittest/pom.xml
```

- [ ] **Step 3: Convert `cola-component-job` to a child module**

Replace the top-level identity block in `cola-components/cola-component-job/pom.xml` with:

```xml
    <parent>
        <groupId>top.egon</groupId>
        <artifactId>cola-components-parent</artifactId>
        <version>5.1.1</version>
    </parent>

    <artifactId>cola-component-job</artifactId>
    <packaging>jar</packaging>
    <name>cola-component-job</name>
    <description>cola-component-job</description>
```

Remove the standalone `<properties>` block from `cola-components/cola-component-job/pom.xml`; the parent now supplies Java, encoding, Spring Boot, Lombok, JUnit, MySQL, and plugin versions.

- [ ] **Step 4: Remove duplicate managed dependency versions in `cola-component-job`**

In `cola-components/cola-component-job/pom.xml`, remove these version elements:

```xml
            <version>1.18.34</version>
```

```xml
            <version>${spring.boot.version}</version>
```

```xml
            <version>8.0.33</version>
```

```xml
            <version>5.10.2</version>
```

```xml
            <version>5.0.0</version>
```

The `cola-component-unittest` and `cola-component-test-container` dependencies should become:

```xml
        <dependency>
            <groupId>top.egon</groupId>
            <artifactId>cola-component-unittest</artifactId>
            <exclusions>
                <exclusion>
                    <groupId>org.springframework.kafka</groupId>
                    <artifactId>spring-kafka</artifactId>
                </exclusion>
                <exclusion>
                    <groupId>org.springframework.kafka</groupId>
                    <artifactId>spring-kafka-test</artifactId>
                </exclusion>
                <exclusion>
                    <groupId>org.postgresql</groupId>
                    <artifactId>postgresql</artifactId>
                </exclusion>
            </exclusions>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>top.egon</groupId>
            <artifactId>cola-component-test-container</artifactId>
            <scope>test</scope>
        </dependency>
```

Remove the local `<build>` section from `cola-components/cola-component-job/pom.xml`; the parent compiler plugin supplies Java 17.

- [ ] **Step 5: Fix statemachine POM order, dependency version, and metadata**

In `cola-components/cola-component-statemachine/pom.xml`, place the parent before dependencies, use parent version `5.1.1`, remove `<version>RELEASE</version>` from `junit-jupiter`, and set metadata to:

```xml
    <artifactId>cola-component-statemachine</artifactId>
    <packaging>jar</packaging>
    <name>cola-component-statemachine</name>
    <description>cola-component-statemachine</description>
```

The dependency should be:

```xml
    <dependencies>
        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>
```

- [ ] **Step 6: Fix ruleengine metadata**

In `cola-components/cola-component-ruleengine/pom.xml`, set:

```xml
    <artifactId>cola-component-ruleengine</artifactId>
    <packaging>jar</packaging>
    <name>cola-component-ruleengine</name>
    <description>cola-component-ruleengine</description>
```

- [ ] **Step 7: Remove duplicate managed versions from component modules**

In these files, remove `<version>${project.version}</version>` from dependencies whose `groupId` is `top.egon` and whose version is now managed by the reactor/BOM:

```text
cola-components/cola-component-catchlog-starter/pom.xml
cola-components/cola-component-extension-starter/pom.xml
cola-components/cola-component-unittest/pom.xml
```

Keep scopes and exclusions unchanged.

- [ ] **Step 8: Govern the component BOM identity**

In `cola-components/cola-components-bom/pom.xml`, set:

```xml
    <groupId>top.egon</groupId>
    <artifactId>cola-components-bom</artifactId>
    <version>5.1.1</version>
    <packaging>pom</packaging>
```

- [ ] **Step 9: Govern the component BOM dependencyManagement**

Replace the BOM dependency-management list with:

```xml
    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>top.egon</groupId>
                <artifactId>cola-component-dto</artifactId>
                <version>${project.version}</version>
            </dependency>
            <dependency>
                <groupId>top.egon</groupId>
                <artifactId>cola-component-exception</artifactId>
                <version>${project.version}</version>
            </dependency>
            <dependency>
                <groupId>top.egon</groupId>
                <artifactId>cola-component-statemachine</artifactId>
                <version>${project.version}</version>
            </dependency>
            <dependency>
                <groupId>top.egon</groupId>
                <artifactId>cola-component-domain-starter</artifactId>
                <version>${project.version}</version>
            </dependency>
            <dependency>
                <groupId>top.egon</groupId>
                <artifactId>cola-component-extension-starter</artifactId>
                <version>${project.version}</version>
            </dependency>
            <dependency>
                <groupId>top.egon</groupId>
                <artifactId>cola-component-catchlog-starter</artifactId>
                <version>${project.version}</version>
            </dependency>
            <dependency>
                <groupId>top.egon</groupId>
                <artifactId>cola-component-test-container</artifactId>
                <version>${project.version}</version>
            </dependency>
            <dependency>
                <groupId>top.egon</groupId>
                <artifactId>cola-component-ruleengine</artifactId>
                <version>${project.version}</version>
            </dependency>
            <dependency>
                <groupId>top.egon</groupId>
                <artifactId>cola-component-unittest</artifactId>
                <version>${project.version}</version>
            </dependency>
            <dependency>
                <groupId>top.egon</groupId>
                <artifactId>cola-component-job</artifactId>
                <version>${project.version}</version>
            </dependency>
        </dependencies>
    </dependencyManagement>
```

- [ ] **Step 10: Govern BOM publishing plugin**

In `cola-components/cola-components-bom/pom.xml`, keep `maven-source-plugin`, `maven-javadoc-plugin`, and `maven-deploy-plugin` plugin-management entries. Add `central-publishing-maven-plugin` to plugin management with:

```xml
                <plugin>
                    <groupId>org.sonatype.central</groupId>
                    <artifactId>central-publishing-maven-plugin</artifactId>
                    <version>0.11.0</version>
                    <extensions>true</extensions>
                    <configuration>
                        <publishingServerId>ossrh</publishingServerId>
                        <autoPublish>true</autoPublish>
                        <waitUntil>published</waitUntil>
                    </configuration>
                </plugin>
```

Replace the `deploy-settings` profile plugin with the same `central-publishing-maven-plugin` block without the `<version>` element, relying on plugin management.

- [ ] **Step 11: Verify component scan**

Run:

```bash
rg -n "5\\.x-SNAPSHOT|5\\.0\\.0|com\\.alibaba\\.cola|<version>RELEASE</version>|<name>cola-component-job</name>|<description>cola-component-job</description>|nexus-staging-maven-plugin" cola-components -g 'pom.xml' -g '!**/target/**'
```

Expected: output may include `cola-component-job/pom.xml` for its own `name` and `description` only. There must be no old versions, no `com.alibaba.cola`, no `RELEASE`, and no `nexus-staging-maven-plugin`.

- [ ] **Step 12: Validate component reactor**

Run:

```bash
mvn -f cola-components/pom.xml validate -DskipTests
```

Expected: build success for the component reactor.

- [ ] **Step 13: Commit**

```bash
git add cola-components
git commit -m "chore(pom): govern component modules and bom"
```

## Task 3: Component Development Archetype POM Governance

**Files:**
- Modify: `cola-components/dev-util-archetypes/cola-normal-component-archetype/pom.xml`
- Modify: `cola-components/dev-util-archetypes/cola-normal-component-archetype/src/main/resources/archetype-resources/pom.xml`
- Modify: `cola-components/dev-util-archetypes/cola-starter-component-archetype/pom.xml`
- Modify: `cola-components/dev-util-archetypes/cola-starter-component-archetype/src/main/resources/archetype-resources/pom.xml`

- [ ] **Step 1: Capture current dev-util archetype drift**

Run:

```bash
rg -n "1\\.0\\.0-SNAPSHOT|3\\.0\\.1|3\\.2\\.0|5\\.x-SNAPSHOT|com\\.alibaba\\.cola" cola-components/dev-util-archetypes -g 'pom.xml' -g '!**/target/**'
```

Expected before changes: output includes `1.0.0-SNAPSHOT` in the dev-util archetype project POMs and older archetype/resources plugin versions.

- [ ] **Step 2: Govern normal component archetype project POM**

In `cola-components/dev-util-archetypes/cola-normal-component-archetype/pom.xml`, set:

```xml
    <groupId>top.egon</groupId>
    <artifactId>cola-normal-component-archetype</artifactId>
    <version>5.1.1</version>
```

Use these plugin versions:

```xml
                <plugin>
                    <artifactId>maven-source-plugin</artifactId>
                    <version>3.3.1</version>
                </plugin>
                <plugin>
                    <artifactId>maven-archetype-plugin</artifactId>
                    <version>3.2.1</version>
                </plugin>
                <plugin>
                    <artifactId>maven-resources-plugin</artifactId>
                    <version>3.3.1</version>
                </plugin>
```

- [ ] **Step 3: Govern starter component archetype project POM**

In `cola-components/dev-util-archetypes/cola-starter-component-archetype/pom.xml`, set:

```xml
    <groupId>top.egon</groupId>
    <artifactId>cola-starter-component-archetype</artifactId>
    <version>5.1.1</version>
```

Use this plugin-management entry:

```xml
                <plugin>
                    <artifactId>maven-archetype-plugin</artifactId>
                    <version>3.2.1</version>
                </plugin>
```

- [ ] **Step 4: Verify dev-util archetype resource POMs keep generated component parent contract**

In both resource POMs, keep this generated parent contract unchanged:

```xml
    <parent>
        <groupId>${groupId}</groupId>
        <artifactId>cola-components-parent</artifactId>
        <version>${version}</version>
    </parent>
```

Affected files:

```text
cola-components/dev-util-archetypes/cola-normal-component-archetype/src/main/resources/archetype-resources/pom.xml
cola-components/dev-util-archetypes/cola-starter-component-archetype/src/main/resources/archetype-resources/pom.xml
```

This preserves archetype placeholders while allowing generated component projects to target `top.egon:cola-components-parent:5.1.1` when invoked with the governed version.

- [ ] **Step 5: Verify dev-util archetype scan**

Run:

```bash
rg -n "1\\.0\\.0-SNAPSHOT|3\\.0\\.1|3\\.2\\.0|5\\.x-SNAPSHOT|com\\.alibaba\\.cola" cola-components/dev-util-archetypes -g 'pom.xml' -g '!**/target/**'
```

Expected: no output.

- [ ] **Step 6: Validate dev-util archetype POMs**

Run:

```bash
mvn -f cola-components/dev-util-archetypes/cola-normal-component-archetype/pom.xml validate -DskipTests
mvn -f cola-components/dev-util-archetypes/cola-starter-component-archetype/pom.xml validate -DskipTests
```

Expected: both commands succeed.

- [ ] **Step 7: Commit**

```bash
git add cola-components/dev-util-archetypes
git commit -m "chore(pom): govern component dev archetypes"
```

## Task 4: Archetype Parent Governance

**Files:**
- Modify: `cola-archetypes/pom.xml`
- Modify: `cola-archetypes/cola-archetype-light/pom.xml`
- Modify: `cola-archetypes/cola-archetype-service/pom.xml`
- Modify: `cola-archetypes/cola-archetype-web/pom.xml`

- [ ] **Step 1: Capture current archetype-parent drift**

Run:

```bash
rg -n "nexus-staging-maven-plugin|central-publishing-maven-plugin|maven\\.compiler\\.source>|<version>5\\.1\\.1</version>" cola-archetypes/pom.xml cola-archetypes/cola-archetype-*/pom.xml
```

Expected before changes: parent already uses `5.1.1`, output includes legacy Nexus staging plugin management, and child modules inherit `5.1.1`.

- [ ] **Step 2: Keep archetype coordinates and set Java 17 compiler properties**

In `cola-archetypes/pom.xml`, keep:

```xml
    <groupId>top.egon</groupId>
    <artifactId>cola-framework-archetypes-parent</artifactId>
    <version>5.1.1</version>
    <packaging>pom</packaging>
```

Replace the properties block with:

```xml
    <properties>
        <java.version>17</java.version>
        <maven.compiler.source>${java.version}</maven.compiler.source>
        <maven.compiler.target>${java.version}</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    </properties>
```

- [ ] **Step 3: Remove legacy Nexus staging plugin management**

In `cola-archetypes/pom.xml`, remove the plugin-management entry:

```xml
                <plugin>
                    <groupId>org.sonatype.plugins</groupId>
                    <artifactId>nexus-staging-maven-plugin</artifactId>
                    <version>1.6.13</version>
                </plugin>
```

Keep the existing `org.sonatype.central:central-publishing-maven-plugin` plugin-management entry.

- [ ] **Step 4: Remove explicit Central plugin version from deploy profile**

In `cola-archetypes/pom.xml` under profile `deploy-settings`, keep the `central-publishing-maven-plugin` plugin and remove this child:

```xml
                        <version>0.11.0</version>
```

The deploy profile plugin should rely on plugin management:

```xml
                    <plugin>
                        <groupId>org.sonatype.central</groupId>
                        <artifactId>central-publishing-maven-plugin</artifactId>
                        <extensions>true</extensions>
                        <configuration>
                            <publishingServerId>ossrh</publishingServerId>
                            <autoPublish>true</autoPublish>
                            <waitUntil>published</waitUntil>
                        </configuration>
                    </plugin>
```

- [ ] **Step 5: Ensure child archetype parent versions are stable**

In each child archetype POM, ensure the parent block is:

```xml
    <parent>
        <groupId>top.egon</groupId>
        <artifactId>cola-framework-archetypes-parent</artifactId>
        <version>5.1.1</version>
    </parent>
```

Affected files:

```text
cola-archetypes/cola-archetype-light/pom.xml
cola-archetypes/cola-archetype-service/pom.xml
cola-archetypes/cola-archetype-web/pom.xml
```

- [ ] **Step 6: Verify archetype-parent scan**

Run:

```bash
rg -n "nexus-staging-maven-plugin|maven\\.compiler\\.source>1\\.8" cola-archetypes/pom.xml cola-archetypes/cola-archetype-*/pom.xml
```

Expected: no output.

- [ ] **Step 7: Validate archetype reactor**

Run:

```bash
mvn -f cola-archetypes/pom.xml validate -DskipTests
```

Expected: build success for archetype project model validation.

- [ ] **Step 8: Commit**

```bash
git add cola-archetypes/pom.xml cola-archetypes/cola-archetype-light/pom.xml cola-archetypes/cola-archetype-service/pom.xml cola-archetypes/cola-archetype-web/pom.xml
git commit -m "chore(pom): govern archetype parent"
```

## Task 5: Archetype Template POM Governance

**Files:**
- Modify: all archetype resource POMs listed in File Structure.

- [ ] **Step 1: Capture current template drift**

Run:

```bash
rg -n "com\\.alibaba\\.cola|5\\.x-SNAPSHOT|4\\.4\\.0-SNAPSHOT|3\\.2\\.0|maven\\.compiler\\.source>1\\.8|maven-resources-plugin</artifactId>\\n\\s*<version>3\\.3\\.0|maven-compiler-plugin</artifactId>\\n\\s*<version>3\\.10\\.1|maven-javadoc-plugin</artifactId>\\n\\s*<version>3\\.4\\.0|maven-deploy-plugin</artifactId>\\n\\s*<version>3\\.0\\.0" cola-archetypes -g 'pom.xml' -g '!**/target/**'
```

Expected before changes: output includes old component groupIds, `5.x-SNAPSHOT`, `4.4.0-SNAPSHOT`, Boot `3.2.0`, Java `1.8`, and older plugin versions inside template POMs.

- [ ] **Step 2: Update service and web root template compiler and component versions**

In both files below, change the properties to Java 17 and component version `5.1.1`:

```text
cola-archetypes/cola-archetype-service/src/main/resources/archetype-resources/pom.xml
cola-archetypes/cola-archetype-web/src/main/resources/archetype-resources/pom.xml
```

Use this properties block:

```xml
    <properties>
        <java.version>17</java.version>
        <maven.compiler.source>${java.version}</maven.compiler.source>
        <maven.compiler.target>${java.version}</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <project.reporting.outputEncoding>UTF-8</project.reporting.outputEncoding>
        <maven.deploy.skip>true</maven.deploy.skip>

        <cola.components.version>5.1.1</cola.components.version>

        <spring-cloud.version>2023.0.0</spring-cloud.version>
        <spring-ai.version>1.0.0</spring-ai.version>
        <spring-ai-alibaba.version>1.0.0.2</spring-ai-alibaba.version>
        <spring-cloud-alibaba.version>2022.0.0.0-RC2</spring-cloud-alibaba.version>

        <mybatis-plus.version>3.5.12</mybatis-plus.version>
        <guava.version>33.4.8-jre</guava.version>
    </properties>
```

- [ ] **Step 3: Update service and web root template BOM groupId**

In both root template POMs, change the component BOM import to:

```xml
            <dependency>
                <groupId>top.egon</groupId>
                <artifactId>cola-components-bom</artifactId>
                <version>${cola.components.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
```

- [ ] **Step 4: Update service and web root template plugin versions**

In both root template POMs, the plugin management versions should be:

```xml
                <plugin>
                    <artifactId>maven-resources-plugin</artifactId>
                    <version>3.3.1</version>
                </plugin>
                <plugin>
                    <artifactId>maven-compiler-plugin</artifactId>
                    <version>3.13.0</version>
                </plugin>
                <plugin>
                    <artifactId>maven-source-plugin</artifactId>
                    <version>3.3.1</version>
                </plugin>
                <plugin>
                    <artifactId>maven-javadoc-plugin</artifactId>
                    <version>3.7.0</version>
                </plugin>
                <plugin>
                    <artifactId>maven-deploy-plugin</artifactId>
                    <version>3.1.1</version>
                </plugin>
```

Keep the existing Spring Boot Maven plugin entry.

- [ ] **Step 5: Update service template component dependencies**

In these service archetype template POMs, replace every repository-local component dependency groupId `com.alibaba.cola` with `top.egon`:

```text
cola-archetypes/cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-app/pom.xml
cola-archetypes/cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-client/pom.xml
cola-archetypes/cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-domain/pom.xml
```

The dependency entries should keep their artifactIds:

```xml
            <groupId>top.egon</groupId>
            <artifactId>cola-component-catchlog-starter</artifactId>
```

```xml
            <groupId>top.egon</groupId>
            <artifactId>cola-component-dto</artifactId>
```

```xml
            <groupId>top.egon</groupId>
            <artifactId>cola-component-domain-starter</artifactId>
```

```xml
            <groupId>top.egon</groupId>
            <artifactId>cola-component-exception</artifactId>
```

- [ ] **Step 6: Update web template component dependencies**

In these web archetype template POMs, replace every repository-local component dependency groupId `com.alibaba.cola` with `top.egon`:

```text
cola-archetypes/cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-app/pom.xml
cola-archetypes/cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-client/pom.xml
cola-archetypes/cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-domain/pom.xml
```

The dependency entries should keep their artifactIds:

```xml
            <groupId>top.egon</groupId>
            <artifactId>cola-component-catchlog-starter</artifactId>
```

```xml
            <groupId>top.egon</groupId>
            <artifactId>cola-component-dto</artifactId>
```

```xml
            <groupId>top.egon</groupId>
            <artifactId>cola-component-domain-starter</artifactId>
```

```xml
            <groupId>top.egon</groupId>
            <artifactId>cola-component-exception</artifactId>
```

- [ ] **Step 7: Update light archetype template POM**

In `cola-archetypes/cola-archetype-light/src/main/resources/archetype-resources/pom.xml`, set:

```xml
        <spring.boot.version>3.2.10</spring.boot.version>
        <cola.components.version>5.1.1</cola.components.version>
        <lombok.version>1.18.46</lombok.version>
```

Use the version properties for Lombok and test container:

```xml
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <version>${lombok.version}</version>
        </dependency>
```

```xml
        <dependency>
            <groupId>top.egon</groupId>
            <artifactId>cola-component-test-container</artifactId>
            <version>${cola.components.version}</version>
        </dependency>
```

- [ ] **Step 8: Verify template scan**

Run:

```bash
rg -n "com\\.alibaba\\.cola|5\\.x-SNAPSHOT|4\\.4\\.0-SNAPSHOT|3\\.2\\.0|maven\\.compiler\\.source>1\\.8|maven-resources-plugin</artifactId>\\n\\s*<version>3\\.3\\.0|maven-compiler-plugin</artifactId>\\n\\s*<version>3\\.10\\.1|maven-javadoc-plugin</artifactId>\\n\\s*<version>3\\.4\\.0|maven-deploy-plugin</artifactId>\\n\\s*<version>3\\.0\\.0" cola-archetypes -g 'pom.xml' -g '!**/target/**'
```

Expected: no output from source archetype POMs.

- [ ] **Step 9: Validate archetype reactor**

Run:

```bash
mvn -f cola-archetypes/pom.xml validate -DskipTests
```

Expected: build success.

- [ ] **Step 10: Commit**

```bash
git add cola-archetypes
git commit -m "chore(pom): govern archetype templates"
```

## Task 6: Sample POM Governance

**Files:**
- Modify: all sample POMs listed in File Structure.

- [ ] **Step 1: Capture current sample drift**

Run:

```bash
rg -n "com\\.alibaba\\.cola|5\\.x-SNAPSHOT|4\\.4\\.0-SNAPSHOT|3\\.2\\.0|maven\\.compiler\\.source>1\\.8|maven-resources-plugin</artifactId>\\n\\s*<version>3\\.3\\.0|maven-compiler-plugin</artifactId>\\n\\s*<version>3\\.10\\.1|maven-javadoc-plugin</artifactId>\\n\\s*<version>3\\.4\\.0|maven-deploy-plugin</artifactId>\\n\\s*<version>3\\.0\\.0" cola-samples -g 'pom.xml' -g '!**/target/**'
```

Expected before changes: output includes old component groupIds and versions in `family` and `charge`.

- [ ] **Step 2: Update family root POM properties**

In `cola-samples/family/pom.xml`, replace the properties block with:

```xml
    <properties>
        <java.version>17</java.version>
        <maven.compiler.source>${java.version}</maven.compiler.source>
        <maven.compiler.target>${java.version}</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <project.reporting.outputEncoding>UTF-8</project.reporting.outputEncoding>
        <maven.deploy.skip>true</maven.deploy.skip>

        <cola.components.version>5.1.1</cola.components.version>

        <spring-cloud.version>2023.0.0</spring-cloud.version>
        <spring-ai.version>1.0.0</spring-ai.version>
        <spring-ai-alibaba.version>1.0.0.2</spring-ai-alibaba.version>
        <spring-cloud-alibaba.version>2022.0.0.0-RC2</spring-cloud-alibaba.version>

        <mybatis-plus.version>3.5.12</mybatis-plus.version>
        <guava.version>33.4.8-jre</guava.version>
    </properties>
```

- [ ] **Step 3: Update family root BOM groupId and plugin versions**

In `cola-samples/family/pom.xml`, change the component BOM import to:

```xml
            <dependency>
                <groupId>top.egon</groupId>
                <artifactId>cola-components-bom</artifactId>
                <version>${cola.components.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
```

Update plugin management versions to:

```xml
                <plugin>
                    <artifactId>maven-resources-plugin</artifactId>
                    <version>3.3.1</version>
                </plugin>
                <plugin>
                    <artifactId>maven-compiler-plugin</artifactId>
                    <version>3.13.0</version>
                </plugin>
                <plugin>
                    <artifactId>maven-source-plugin</artifactId>
                    <version>3.3.1</version>
                </plugin>
                <plugin>
                    <artifactId>maven-javadoc-plugin</artifactId>
                    <version>3.7.0</version>
                </plugin>
                <plugin>
                    <artifactId>maven-deploy-plugin</artifactId>
                    <version>3.1.1</version>
                </plugin>
```

Keep the existing Spring Boot Maven plugin entry.

- [ ] **Step 4: Update family module component dependencies**

In these files, replace every repository-local component dependency groupId `com.alibaba.cola` with `top.egon`:

```text
cola-samples/family/family-app/pom.xml
cola-samples/family/family-client/pom.xml
cola-samples/family/family-domain/pom.xml
```

The dependency entries should keep their artifactIds:

```xml
            <groupId>top.egon</groupId>
            <artifactId>cola-component-catchlog-starter</artifactId>
```

```xml
            <groupId>top.egon</groupId>
            <artifactId>cola-component-dto</artifactId>
```

```xml
            <groupId>top.egon</groupId>
            <artifactId>cola-component-domain-starter</artifactId>
```

```xml
            <groupId>top.egon</groupId>
            <artifactId>cola-component-exception</artifactId>
```

- [ ] **Step 5: Update charge sample POM**

In `cola-samples/charge/pom.xml`, keep project identity unchanged and set properties:

```xml
        <spring.boot.version>3.2.10</spring.boot.version>
        <cola.components.version>5.1.1</cola.components.version>
        <lombok.version>1.18.46</lombok.version>
```

Use these dependency entries:

```xml
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <version>${lombok.version}</version>
        </dependency>
```

```xml
        <dependency>
            <groupId>top.egon</groupId>
            <artifactId>cola-component-test-container</artifactId>
            <version>${cola.components.version}</version>
        </dependency>
```

- [ ] **Step 6: Verify sample scan**

Run:

```bash
rg -n "com\\.alibaba\\.cola|5\\.x-SNAPSHOT|4\\.4\\.0-SNAPSHOT|3\\.2\\.0|maven\\.compiler\\.source>1\\.8|maven-resources-plugin</artifactId>\\n\\s*<version>3\\.3\\.0|maven-compiler-plugin</artifactId>\\n\\s*<version>3\\.10\\.1|maven-javadoc-plugin</artifactId>\\n\\s*<version>3\\.4\\.0|maven-deploy-plugin</artifactId>\\n\\s*<version>3\\.0\\.0" cola-samples -g 'pom.xml' -g '!**/target/**'
```

Expected: no output.

- [ ] **Step 7: Validate samples before local install**

Run:

```bash
mvn -f cola-samples/family/pom.xml validate -DskipTests
```

Expected: this may fail until `top.egon:cola-components-bom:5.1.1` is installed locally. If it fails, the only acceptable failure at this step is unresolved `top.egon:cola-components-bom:5.1.1`.

Run:

```bash
mvn -f cola-samples/charge/pom.xml validate -DskipTests
```

Expected: this may fail until `top.egon:cola-component-test-container:5.1.1` is installed locally. If it fails, the only acceptable failure at this step is unresolved `top.egon` component artifact version `5.1.1`.

- [ ] **Step 8: Commit**

```bash
git add cola-samples
git commit -m "chore(pom): govern sample projects"
```

## Task 7: Root Aggregator and Cross-POM Consistency

**Files:**
- Modify: `pom.xml` only if scan proves it needs version or publishing cleanup.

- [ ] **Step 1: Inspect root aggregator**

Run:

```bash
sed -n '1,120p' pom.xml
```

Expected: root POM aggregates `cola-components` and `cola-archetypes`, skips deploy, and does not include active `cola-samples` modules.

- [ ] **Step 2: Keep root deployment disabled**

If `pom.xml` already has this deploy plugin configuration, leave it unchanged:

```xml
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-deploy-plugin</artifactId>
                <version>3.1.1</version>
                <inherited>false</inherited>
                <configuration>
                    <skip>true</skip>
                </configuration>
            </plugin>
```

If the version differs, set it to `3.1.1`.

- [ ] **Step 3: Verify no governed source POM has old repository-local coordinates**

Run:

```bash
rg -n "com\\.alibaba\\.cola" -g 'pom.xml' -g '!**/target/**'
```

Expected: no output.

- [ ] **Step 4: Verify no governed source POM has old component versions**

Run:

```bash
rg -n "5\\.x-SNAPSHOT|4\\.4\\.0-SNAPSHOT|<version>5\\.0\\.0</version>|<version>RELEASE</version>" -g 'pom.xml' -g '!**/target/**'
```

Expected: no output from governed source POMs. Project versions such as generated sample `1.0.0-SNAPSHOT` are allowed because they are consumer project versions, not repository component versions.

- [ ] **Step 5: Verify no source POM keeps legacy Nexus staging plugin**

Run:

```bash
rg -n "nexus-staging-maven-plugin" -g 'pom.xml' -g '!**/target/**'
```

Expected: no output.

- [ ] **Step 6: Validate root aggregator**

Run:

```bash
mvn validate -DskipTests
```

Expected: build success for the root reactor.

- [ ] **Step 7: Commit root cleanup if root changed**

If `pom.xml` changed in this task:

```bash
git add pom.xml
git commit -m "chore(pom): align root aggregator"
```

If `pom.xml` did not change:

```bash
git status --short
```

Expected: no unstaged root POM change from this task.

## Task 8: Install-Level Validation and Final Audit

**Files:**
- No planned file edits.

- [ ] **Step 1: Validate component reactor**

Run:

```bash
mvn -f cola-components/pom.xml validate -DskipTests
```

Expected: build success.

- [ ] **Step 2: Install component artifacts locally**

Run:

```bash
mvn -f cola-components/pom.xml install -DskipTests
```

Expected: build success and local installation of `top.egon` component artifacts and `top.egon:cola-components-bom:5.1.1`.

- [ ] **Step 3: Validate archetype reactor**

Run:

```bash
mvn -f cola-archetypes/pom.xml validate -DskipTests
```

Expected: build success.

- [ ] **Step 4: Validate family sample after local install**

Run:

```bash
mvn -f cola-samples/family/pom.xml validate -DskipTests
```

Expected: build success.

- [ ] **Step 5: Validate charge sample after local install**

Run:

```bash
mvn -f cola-samples/charge/pom.xml validate -DskipTests
```

Expected: build success.

- [ ] **Step 6: Run final source-POM audit**

Run:

```bash
find . -path '*/target' -prune -o -name pom.xml -print | sort
```

Expected: source POM list only; generated `target/` POMs are not part of the audit.

Run:

```bash
rg -n "com\\.alibaba\\.cola|5\\.x-SNAPSHOT|4\\.4\\.0-SNAPSHOT|<version>RELEASE</version>|nexus-staging-maven-plugin" -g 'pom.xml' -g '!**/target/**'
```

Expected: no output from governed POMs. If output appears under `cola-components/dev-util-archetypes`, decide whether it is a source POM that should be governed by this task before finalizing.

- [ ] **Step 7: Inspect changed files**

Run:

```bash
git status --short
git diff --stat HEAD
```

Expected: no Java files, generated `target/` files, README, scripts, or runtime files changed.

- [ ] **Step 8: Commit any final POM-only cleanup**

If Step 6 or Step 7 required final POM-only cleanup:

```bash
git add pom.xml cola-components cola-archetypes cola-samples
git commit -m "chore(pom): complete pom governance audit"
```

If no cleanup was needed:

```bash
git status --short
```

Expected: clean working tree.

## Final Completion Checklist

- [ ] Root reactor validates with `mvn validate -DskipTests`.
- [ ] Component reactor validates with `mvn -f cola-components/pom.xml validate -DskipTests`.
- [ ] Component reactor installs with `mvn -f cola-components/pom.xml install -DskipTests`.
- [ ] Archetype reactor validates with `mvn -f cola-archetypes/pom.xml validate -DskipTests`.
- [ ] Family sample validates with `mvn -f cola-samples/family/pom.xml validate -DskipTests`.
- [ ] Charge sample validates with `mvn -f cola-samples/charge/pom.xml validate -DskipTests`.
- [ ] No governed source POM contains `com.alibaba.cola`.
- [ ] No governed source POM contains `5.x-SNAPSHOT`, `4.4.0-SNAPSHOT`, or `<version>RELEASE</version>`.
- [ ] No governed source POM contains `nexus-staging-maven-plugin`.
- [ ] No Java source, README, script, generated `target/` file, or runtime config was changed.
