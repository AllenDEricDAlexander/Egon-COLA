# Maven Central POM Governance Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:
> executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the remaining OSSRH-era release configuration with a single Sonatype Central Portal publishing flow
using `central`, `-Prelease`, signed artifacts, and manual GitHub Actions publishing.

**Architecture:** Keep the current Maven module boundaries: root aggregator, component reactor, archetype reactor,
standalone BOM, samples, and local Maven settings. Consolidate release behavior into explicit Maven profiles and one
manual workflow; do not add generators, new build systems, or runtime code.

**Tech Stack:** Maven multi-module POMs, Java 17, Sonatype Central Publishing Maven Plugin `0.11.0`, Maven
source/javadoc/gpg plugins, GitHub Actions, `/Users/mario/.m2/settings.xml`.

---

## Scope Check

This plan covers one subsystem: Maven Central publishing governance. It intentionally excludes Java code, dependency
upgrades unrelated to publishing, version changes beyond `5.1.1`, real `deploy`, application startup, and generated
`target/` files.

## File Structure

- Modify: `cola-components/pom.xml`
    - Component reactor parent, Central publishing properties, release profile, snapshot repository, project metadata.
- Modify: `cola-components/cola-components-bom/pom.xml`
    - Standalone BOM publishing profile, Central server id, snapshot repository, project metadata.
- Modify: component module POMs under `cola-components/cola-component-*/pom.xml`
    - Project URL and LGPL license URL metadata only.
- Modify: component dev archetype template POMs under `cola-components/dev-util-archetypes/**/pom.xml`
    - Project URL metadata only where stale.
- Modify: `cola-archetypes/pom.xml`
    - Archetype reactor parent, Central publishing properties, release profile, snapshot repository, removal of OSSRH
      release repository.
- Modify: archetype module POMs under `cola-archetypes/cola-archetype-*/pom.xml`
    - Malformed project URL and LGPL license URL metadata.
- Modify: archetype resource template POMs under
  `cola-archetypes/cola-archetype-*/src/main/resources/archetype-resources/**/pom.xml`
    - Only if scans show stale project-local coordinates or stale release metadata.
- Modify: `/Users/mario/.m2/settings.xml`
    - Add `central` server and move GPG passphrase to environment variable reference. Preserve unrelated Nexus servers.
- Create: `.github/workflows/publish-maven-central.yml`
    - Manual Maven Central publishing workflow.
- Modify: `README.md`
    - Release command examples and stale Maven Central badge/coordinates only. Preserve existing dirty user changes.
- Modify: `scripts/maven-deploy.md`
    - Replace OSSRH instructions with Central Portal instructions.
- Modify: `scripts/integration_test`
    - Switch release validation option from `-DperformRelease -P'!gen-sign'` to `-Prelease -Dgpg.skip=true`.

## Shared Target XML Blocks

Use these exact values throughout the plan:

```xml
<central.server.id>central</central.server.id>
<central.publishing.maven.plugin.version>0.11.0</central.publishing.maven.plugin.version>
<maven.source.plugin.version>3.3.1</maven.source.plugin.version>
<maven.javadoc.plugin.version>3.7.0</maven.javadoc.plugin.version>
<maven.gpg.plugin.version>3.1.0</maven.gpg.plugin.version>
```

Use this exact Central publishing plugin configuration in publishable POMs:

```xml
<plugin>
    <groupId>org.sonatype.central</groupId>
    <artifactId>central-publishing-maven-plugin</artifactId>
    <version>${central.publishing.maven.plugin.version}</version>
    <extensions>true</extensions>
    <configuration>
        <publishingServerId>${central.server.id}</publishingServerId>
        <autoPublish>true</autoPublish>
        <waitUntil>published</waitUntil>
    </configuration>
</plugin>
```

Use this exact snapshot repository where a publishable root declares `distributionManagement`:

```xml
<distributionManagement>
    <snapshotRepository>
        <id>${central.server.id}</id>
        <name>Central Portal Snapshots</name>
        <url>https://central.sonatype.com/repository/maven-snapshots/</url>
    </snapshotRepository>
</distributionManagement>
```

Use this exact SCM metadata block in publishable root POMs:

```xml
<scm>
    <connection>scm:git:https://github.com/AllenDEricDAlexander/Egon-COLA.git</connection>
    <developerConnection>scm:git:ssh://git@github.com/AllenDEricDAlexander/Egon-COLA.git</developerConnection>
    <url>https://github.com/AllenDEricDAlexander/Egon-COLA</url>
    <tag>HEAD</tag>
</scm>
```

Use this exact license URL for the LGPL entry:

```xml
<url>https://github.com/AllenDEricDAlexander/Egon-COLA/blob/master/LICENSE-GPL-2.1</url>
```

## Task 1: Baseline Release Drift Scan

**Files:**

- Inspect: `cola-components/pom.xml`
- Inspect: `cola-components/cola-components-bom/pom.xml`
- Inspect: `cola-archetypes/pom.xml`
- Inspect: `README.md`
- Inspect: `scripts/maven-deploy.md`
- Inspect: `scripts/integration_test`
- Inspect: `/Users/mario/.m2/settings.xml`

- [ ] **Step 1: Confirm worktree state**

Run:

```bash
git status --short
```

Expected: output may include existing dirty `README.md`. Do not reset or discard it.

- [ ] **Step 2: Capture README user changes before edits**

Run:

```bash
git diff -- README.md > /tmp/egon-cola-readme-before-maven-central-plan.diff
wc -l /tmp/egon-cola-readme-before-maven-central-plan.diff
```

Expected: command succeeds. The line count may be `0` if the user later cleaned the file.

- [ ] **Step 3: Scan source POM release drift**

Run:

```bash
rg -n "ossrh|oss\\.sonatype|ossrh-staging-api|performRelease|nexus-staging-maven-plugin|central-publishing-maven-plugin|github.com/alibaba/COLAhttps://github.com|https://github.com/alibaba/COLA" \
  -g 'pom.xml' \
  -g '!**/target/**' \
  cola-components cola-archetypes cola-samples
```

Expected before changes: output includes `publishingServerId>ossrh`, snapshot repository ids `ossrh`, `performRelease`,
Alibaba URLs, and the archetypes OSSRH Staging API release repository.

- [ ] **Step 4: Scan release docs and scripts**

Run:

```bash
rg -n "ossrh|oss\\.sonatype|performRelease|com\\.alibaba\\.cola|central\\.sonatype\\.com/namespace/com\\.alibaba\\.cola|github.com/alibaba/COLA" README.md scripts .github -g '!**/target/**'
```

Expected before changes: output includes README release examples, `scripts/maven-deploy.md`, and
`scripts/integration_test`.

- [ ] **Step 5: Scan local Maven settings without printing secrets**

Run:

```bash
perl -ne 'print "$.:$_" if /<server>|<id>|<profile>|<activeProfile>|<gpg\.passphrase>|<gpg\.homedir>|<gpg\.executable>/' /Users/mario/.m2/settings.xml
```

Expected before changes: output includes `<id>ossrh</id>` under `<servers>`, no `<id>central</id>` server, and a
`gpg.passphrase` property.

- [ ] **Step 6: Commit**

No commit for this inspection-only task.

## Task 2: Component Parent Central Release Profile

**Files:**

- Modify: `cola-components/pom.xml`

- [ ] **Step 1: Add Central publishing properties**

In `cola-components/pom.xml`, extend the existing `<properties>` block after
`<mysql.connector.version>8.0.33</mysql.connector.version>` so it contains:

```xml
        <mysql.connector.version>8.0.33</mysql.connector.version>
        <central.server.id>central</central.server.id>
        <central.publishing.maven.plugin.version>0.11.0</central.publishing.maven.plugin.version>
        <maven.source.plugin.version>3.3.1</maven.source.plugin.version>
        <maven.javadoc.plugin.version>3.7.0</maven.javadoc.plugin.version>
        <maven.gpg.plugin.version>3.1.0</maven.gpg.plugin.version>
```

- [ ] **Step 2: Replace component parent project metadata**

In `cola-components/pom.xml`, replace the root `<url>` and `<scm>` block with:

```xml
    <url>https://github.com/AllenDEricDAlexander/Egon-COLA</url>
```

```xml
    <scm>
        <connection>scm:git:https://github.com/AllenDEricDAlexander/Egon-COLA.git</connection>
        <developerConnection>scm:git:ssh://git@github.com/AllenDEricDAlexander/Egon-COLA.git</developerConnection>
        <url>https://github.com/AllenDEricDAlexander/Egon-COLA</url>
        <tag>HEAD</tag>
    </scm>
```

Replace the LGPL license URL in the same file with:

```xml
            <url>https://github.com/AllenDEricDAlexander/Egon-COLA/blob/master/LICENSE-GPL-2.1</url>
```

- [ ] **Step 3: Use property-backed plugin versions**

In `cola-components/pom.xml`, update plugin-management entries for source, javadoc, gpg, and Central publishing to:

```xml
                <plugin>
                    <artifactId>maven-source-plugin</artifactId>
                    <version>${maven.source.plugin.version}</version>
                </plugin>
                <plugin>
                    <artifactId>maven-javadoc-plugin</artifactId>
                    <version>${maven.javadoc.plugin.version}</version>
                </plugin>
                <plugin>
                    <artifactId>maven-gpg-plugin</artifactId>
                    <version>${maven.gpg.plugin.version}</version>
                </plugin>
```

```xml
                <plugin>
                    <groupId>org.sonatype.central</groupId>
                    <artifactId>central-publishing-maven-plugin</artifactId>
                    <version>${central.publishing.maven.plugin.version}</version>
                    <extensions>true</extensions>
                    <configuration>
                        <publishingServerId>${central.server.id}</publishingServerId>
                        <autoPublish>true</autoPublish>
                        <waitUntil>published</waitUntil>
                    </configuration>
                </plugin>
```

- [ ] **Step 4: Replace component snapshot repository id**

In `cola-components/pom.xml`, replace the full `<distributionManagement>` block with:

```xml
    <distributionManagement>
        <snapshotRepository>
            <id>${central.server.id}</id>
            <name>Central Portal Snapshots</name>
            <url>https://central.sonatype.com/repository/maven-snapshots/</url>
        </snapshotRepository>
    </distributionManagement>
```

- [ ] **Step 5: Replace release-related profiles**

In `cola-components/pom.xml`, keep the existing `gen-code-cov` profile. Delete the current profiles named
`gen-java-src`, `gen-java-doc`, `gen-sign`, `gen-git-properties`, `force-jdk11-when-release`, and `deploy-settings`.

Add this profile under `<profiles>` alongside `gen-code-cov`:

```xml
        <profile>
            <id>release</id>
            <build>
                <plugins>
                    <plugin>
                        <artifactId>maven-source-plugin</artifactId>
                        <executions>
                            <execution>
                                <id>attach-sources</id>
                                <phase>verify</phase>
                                <goals>
                                    <goal>jar-no-fork</goal>
                                </goals>
                            </execution>
                        </executions>
                    </plugin>
                    <plugin>
                        <artifactId>maven-javadoc-plugin</artifactId>
                        <configuration>
                            <source>17</source>
                            <show>protected</show>
                            <charset>UTF-8</charset>
                            <encoding>UTF-8</encoding>
                            <docencoding>UTF-8</docencoding>
                            <doclint>none</doclint>
                            <additionalJOptions>
                                <additionalJOption>-quiet</additionalJOption>
                                <additionalJOption>-J-Duser.language=en</additionalJOption>
                                <additionalJOption>-J-Duser.country=US</additionalJOption>
                                <additionalJOption>-Xdoclint:none</additionalJOption>
                            </additionalJOptions>
                        </configuration>
                        <executions>
                            <execution>
                                <id>attach-javadocs</id>
                                <phase>verify</phase>
                                <goals>
                                    <goal>jar</goal>
                                </goals>
                            </execution>
                        </executions>
                    </plugin>
                    <plugin>
                        <artifactId>maven-gpg-plugin</artifactId>
                        <configuration>
                            <gpgArguments>
                                <arg>--pinentry-mode</arg>
                                <arg>loopback</arg>
                            </gpgArguments>
                        </configuration>
                        <executions>
                            <execution>
                                <id>sign-artifacts</id>
                                <phase>verify</phase>
                                <goals>
                                    <goal>sign</goal>
                                </goals>
                            </execution>
                        </executions>
                    </plugin>
                    <plugin>
                        <artifactId>maven-enforcer-plugin</artifactId>
                        <executions>
                            <execution>
                                <id>enforce-jdk-versions</id>
                                <goals>
                                    <goal>enforce</goal>
                                </goals>
                                <configuration>
                                    <rules>
                                        <requireJavaVersion>
                                            <version>17</version>
                                        </requireJavaVersion>
                                    </rules>
                                </configuration>
                            </execution>
                        </executions>
                    </plugin>
                    <plugin>
                        <groupId>org.sonatype.central</groupId>
                        <artifactId>central-publishing-maven-plugin</artifactId>
                    </plugin>
                </plugins>
            </build>
        </profile>
```

- [ ] **Step 6: Run component parent drift scan**

Run:

```bash
rg -n "ossrh|ossrh-staging-api|oss\\.sonatype|performRelease|nexus-staging-maven-plugin|github.com/alibaba/COLA" cola-components/pom.xml
```

Expected: no output.

- [ ] **Step 7: Validate component model**

Run:

```bash
mvn -B -ntp -f cola-components/pom.xml -DskipTests validate
```

Expected: `BUILD SUCCESS`.

- [ ] **Step 8: Commit component parent changes**

Run:

```bash
git add cola-components/pom.xml
git commit -m "chore(pom): centralize component publishing"
```

Expected: commit succeeds.

## Task 3: Component BOM and Module Metadata

**Files:**

- Modify: `cola-components/cola-components-bom/pom.xml`
- Modify: `cola-components/cola-component-catchlog-starter/pom.xml`
- Modify: `cola-components/cola-component-domain-starter/pom.xml`
- Modify: `cola-components/cola-component-dto/pom.xml`
- Modify: `cola-components/cola-component-exception/pom.xml`
- Modify: `cola-components/cola-component-extension-starter/pom.xml`
- Modify: `cola-components/cola-component-ruleengine/pom.xml`
- Modify: `cola-components/cola-component-statemachine/pom.xml`
- Modify: `cola-components/cola-component-test-container/pom.xml`
- Modify: `cola-components/cola-component-unittest/pom.xml`
- Modify:
  `cola-components/dev-util-archetypes/cola-normal-component-archetype/src/main/resources/archetype-resources/pom.xml`
- Modify:
  `cola-components/dev-util-archetypes/cola-starter-component-archetype/src/main/resources/archetype-resources/pom.xml`

- [ ] **Step 1: Update BOM metadata**

In `cola-components/cola-components-bom/pom.xml`, replace:

```xml
    <url>https://github.com/alibaba/COLA</url>
```

with:

```xml
    <url>https://github.com/AllenDEricDAlexander/Egon-COLA</url>
```

Replace the LGPL license URL with:

```xml
            <url>https://github.com/AllenDEricDAlexander/Egon-COLA/blob/master/LICENSE-GPL-2.1</url>
```

Replace the `<scm>` block with:

```xml
    <scm>
        <connection>scm:git:https://github.com/AllenDEricDAlexander/Egon-COLA.git</connection>
        <developerConnection>scm:git:ssh://git@github.com/AllenDEricDAlexander/Egon-COLA.git</developerConnection>
        <url>https://github.com/AllenDEricDAlexander/Egon-COLA</url>
        <tag>HEAD</tag>
    </scm>
```

- [ ] **Step 2: Add BOM Central publishing properties**

In `cola-components/cola-components-bom/pom.xml`, add this block after `<developers>` and before
`<dependencyManagement>`:

```xml
    <properties>
        <central.server.id>central</central.server.id>
        <central.publishing.maven.plugin.version>0.11.0</central.publishing.maven.plugin.version>
        <maven.source.plugin.version>3.3.1</maven.source.plugin.version>
        <maven.javadoc.plugin.version>3.7.0</maven.javadoc.plugin.version>
        <maven.gpg.plugin.version>3.1.0</maven.gpg.plugin.version>
    </properties>
```

- [ ] **Step 3: Replace BOM distribution management**

In `cola-components/cola-components-bom/pom.xml`, replace the full `<distributionManagement>` block with:

```xml
    <distributionManagement>
        <snapshotRepository>
            <id>${central.server.id}</id>
            <name>Central Portal Snapshots</name>
            <url>https://central.sonatype.com/repository/maven-snapshots/</url>
        </snapshotRepository>
    </distributionManagement>
```

- [ ] **Step 4: Replace BOM plugin management**

In `cola-components/cola-components-bom/pom.xml`, update plugin-management entries to:

```xml
                <plugin>
                    <artifactId>maven-source-plugin</artifactId>
                    <version>${maven.source.plugin.version}</version>
                </plugin>
                <plugin>
                    <artifactId>maven-javadoc-plugin</artifactId>
                    <version>${maven.javadoc.plugin.version}</version>
                </plugin>
                <plugin>
                    <artifactId>maven-gpg-plugin</artifactId>
                    <version>${maven.gpg.plugin.version}</version>
                </plugin>
                <plugin>
                    <artifactId>maven-deploy-plugin</artifactId>
                    <version>3.1.1</version>
                </plugin>
                <plugin>
                    <groupId>org.sonatype.central</groupId>
                    <artifactId>central-publishing-maven-plugin</artifactId>
                    <version>${central.publishing.maven.plugin.version}</version>
                    <extensions>true</extensions>
                    <configuration>
                        <publishingServerId>${central.server.id}</publishingServerId>
                        <autoPublish>true</autoPublish>
                        <waitUntil>published</waitUntil>
                    </configuration>
                </plugin>
```

- [ ] **Step 5: Replace BOM profiles**

In `cola-components/cola-components-bom/pom.xml`, delete profiles named `gen-sign` and `deploy-settings`. Add this
single profile:

```xml
        <profile>
            <id>release</id>
            <build>
                <plugins>
                    <plugin>
                        <artifactId>maven-source-plugin</artifactId>
                        <executions>
                            <execution>
                                <id>attach-sources</id>
                                <phase>verify</phase>
                                <goals>
                                    <goal>jar-no-fork</goal>
                                </goals>
                            </execution>
                        </executions>
                    </plugin>
                    <plugin>
                        <artifactId>maven-javadoc-plugin</artifactId>
                        <configuration>
                            <charset>UTF-8</charset>
                            <encoding>UTF-8</encoding>
                            <docencoding>UTF-8</docencoding>
                            <doclint>none</doclint>
                        </configuration>
                        <executions>
                            <execution>
                                <id>attach-javadocs</id>
                                <phase>verify</phase>
                                <goals>
                                    <goal>jar</goal>
                                </goals>
                            </execution>
                        </executions>
                    </plugin>
                    <plugin>
                        <artifactId>maven-gpg-plugin</artifactId>
                        <configuration>
                            <gpgArguments>
                                <arg>--pinentry-mode</arg>
                                <arg>loopback</arg>
                            </gpgArguments>
                        </configuration>
                        <executions>
                            <execution>
                                <id>sign-artifacts</id>
                                <phase>verify</phase>
                                <goals>
                                    <goal>sign</goal>
                                </goals>
                            </execution>
                        </executions>
                    </plugin>
                    <plugin>
                        <groupId>org.sonatype.central</groupId>
                        <artifactId>central-publishing-maven-plugin</artifactId>
                    </plugin>
                </plugins>
            </build>
        </profile>
```

- [ ] **Step 6: Update component module POM metadata URLs**

In each listed component module POM, replace:

```xml
    <url>https://github.com/alibaba/COLA</url>
```

with:

```xml
    <url>https://github.com/AllenDEricDAlexander/Egon-COLA</url>
```

Replace each LGPL license URL with:

```xml
            <url>https://github.com/AllenDEricDAlexander/Egon-COLA/blob/master/LICENSE-GPL-2.1</url>
```

Apply this only to these files:

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

- [ ] **Step 7: Update dev archetype template POM URLs**

In these files:

```text
cola-components/dev-util-archetypes/cola-normal-component-archetype/src/main/resources/archetype-resources/pom.xml
cola-components/dev-util-archetypes/cola-starter-component-archetype/src/main/resources/archetype-resources/pom.xml
```

replace:

```xml
    <url>https://github.com/alibaba/COLA</url>
```

with:

```xml
    <url>https://github.com/AllenDEricDAlexander/Egon-COLA</url>
```

- [ ] **Step 8: Run component metadata drift scan**

Run:

```bash
rg -n "ossrh|ossrh-staging-api|oss\\.sonatype|performRelease|nexus-staging-maven-plugin|github.com/alibaba/COLA|<groupId>com\\.alibaba\\.cola</groupId>" \
  cola-components \
  -g 'pom.xml' \
  -g '!**/target/**'
```

Expected: no output from source POM files. Matches in README or Java package references are outside this scan.

- [ ] **Step 9: Validate components**

Run:

```bash
mvn -B -ntp -f cola-components/pom.xml -DskipTests validate
```

Expected: `BUILD SUCCESS`.

- [ ] **Step 10: Commit component BOM and metadata changes**

Run:

```bash
git add \
  cola-components/cola-components-bom/pom.xml \
  cola-components/cola-component-catchlog-starter/pom.xml \
  cola-components/cola-component-domain-starter/pom.xml \
  cola-components/cola-component-dto/pom.xml \
  cola-components/cola-component-exception/pom.xml \
  cola-components/cola-component-extension-starter/pom.xml \
  cola-components/cola-component-ruleengine/pom.xml \
  cola-components/cola-component-statemachine/pom.xml \
  cola-components/cola-component-test-container/pom.xml \
  cola-components/cola-component-unittest/pom.xml \
  cola-components/dev-util-archetypes/cola-normal-component-archetype/src/main/resources/archetype-resources/pom.xml \
  cola-components/dev-util-archetypes/cola-starter-component-archetype/src/main/resources/archetype-resources/pom.xml
git commit -m "chore(pom): align component central metadata"
```

Expected: commit succeeds.

## Task 4: Archetype Parent Central Release Profile

**Files:**

- Modify: `cola-archetypes/pom.xml`

- [ ] **Step 1: Add archetype Central publishing properties**

In `cola-archetypes/pom.xml`, extend the existing `<properties>` block so it contains:

```xml
        <java.version>17</java.version>
        <maven.compiler.source>${java.version}</maven.compiler.source>
        <maven.compiler.target>${java.version}</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <central.server.id>central</central.server.id>
        <central.publishing.maven.plugin.version>0.11.0</central.publishing.maven.plugin.version>
        <maven.source.plugin.version>3.3.1</maven.source.plugin.version>
        <maven.javadoc.plugin.version>3.7.0</maven.javadoc.plugin.version>
        <maven.gpg.plugin.version>3.1.0</maven.gpg.plugin.version>
```

- [ ] **Step 2: Replace archetype parent SCM and license metadata**

In `cola-archetypes/pom.xml`, replace the `<scm>` block with:

```xml
    <scm>
        <connection>scm:git:https://github.com/AllenDEricDAlexander/Egon-COLA.git</connection>
        <developerConnection>scm:git:ssh://git@github.com/AllenDEricDAlexander/Egon-COLA.git</developerConnection>
        <url>https://github.com/AllenDEricDAlexander/Egon-COLA</url>
        <tag>HEAD</tag>
    </scm>
```

Replace the LGPL license URL with:

```xml
            <url>https://github.com/AllenDEricDAlexander/Egon-COLA/blob/master/LICENSE-GPL-2.1</url>
```

- [ ] **Step 3: Use property-backed archetype plugin versions**

In `cola-archetypes/pom.xml`, update plugin-management entries for source, javadoc, gpg, and Central publishing to:

```xml
                <plugin>
                    <artifactId>maven-source-plugin</artifactId>
                    <version>${maven.source.plugin.version}</version>
                </plugin>
                <plugin>
                    <artifactId>maven-javadoc-plugin</artifactId>
                    <version>${maven.javadoc.plugin.version}</version>
                </plugin>
                <plugin>
                    <artifactId>maven-gpg-plugin</artifactId>
                    <version>${maven.gpg.plugin.version}</version>
                </plugin>
```

```xml
                <plugin>
                    <groupId>org.sonatype.central</groupId>
                    <artifactId>central-publishing-maven-plugin</artifactId>
                    <version>${central.publishing.maven.plugin.version}</version>
                    <extensions>true</extensions>
                    <configuration>
                        <publishingServerId>${central.server.id}</publishingServerId>
                        <autoPublish>true</autoPublish>
                        <waitUntil>published</waitUntil>
                    </configuration>
                </plugin>
```

- [ ] **Step 4: Replace archetype distribution management**

In `cola-archetypes/pom.xml`, delete the `<repository>` child that points to:

```text
https://ossrh-staging-api.central.sonatype.com/service/local/staging/deploy/maven2/
```

Replace the full `<distributionManagement>` block with:

```xml
    <distributionManagement>
        <snapshotRepository>
            <id>${central.server.id}</id>
            <name>Central Portal Snapshots</name>
            <url>https://central.sonatype.com/repository/maven-snapshots/</url>
        </snapshotRepository>
    </distributionManagement>
```

- [ ] **Step 5: Replace archetype release-related profiles**

In `cola-archetypes/pom.xml`, delete profiles named `gen-java-src`, `gen-java-doc`, `force-jdk17-when-release`, and
`deploy-settings`.

Add this single profile under `<profiles>`:

```xml
        <profile>
            <id>release</id>
            <build>
                <plugins>
                    <plugin>
                        <artifactId>maven-source-plugin</artifactId>
                        <executions>
                            <execution>
                                <id>attach-sources</id>
                                <phase>verify</phase>
                                <goals>
                                    <goal>jar-no-fork</goal>
                                </goals>
                            </execution>
                        </executions>
                    </plugin>
                    <plugin>
                        <artifactId>maven-javadoc-plugin</artifactId>
                        <configuration>
                            <source>17</source>
                            <show>protected</show>
                            <charset>UTF-8</charset>
                            <encoding>UTF-8</encoding>
                            <docencoding>UTF-8</docencoding>
                            <doclint>none</doclint>
                            <additionalJOptions>
                                <additionalJOption>-quiet</additionalJOption>
                                <additionalJOption>-J-Duser.language=en</additionalJOption>
                                <additionalJOption>-J-Duser.country=US</additionalJOption>
                                <additionalJOption>-Xdoclint:none</additionalJOption>
                            </additionalJOptions>
                        </configuration>
                        <executions>
                            <execution>
                                <id>attach-javadocs</id>
                                <phase>verify</phase>
                                <goals>
                                    <goal>jar</goal>
                                </goals>
                            </execution>
                        </executions>
                    </plugin>
                    <plugin>
                        <artifactId>maven-gpg-plugin</artifactId>
                        <configuration>
                            <gpgArguments>
                                <arg>--pinentry-mode</arg>
                                <arg>loopback</arg>
                            </gpgArguments>
                        </configuration>
                        <executions>
                            <execution>
                                <id>sign-artifacts</id>
                                <phase>verify</phase>
                                <goals>
                                    <goal>sign</goal>
                                </goals>
                            </execution>
                        </executions>
                    </plugin>
                    <plugin>
                        <artifactId>maven-enforcer-plugin</artifactId>
                        <executions>
                            <execution>
                                <id>enforce-jdk-versions</id>
                                <goals>
                                    <goal>enforce</goal>
                                </goals>
                                <configuration>
                                    <rules>
                                        <requireJavaVersion>
                                            <version>17</version>
                                        </requireJavaVersion>
                                    </rules>
                                </configuration>
                            </execution>
                        </executions>
                    </plugin>
                    <plugin>
                        <groupId>org.sonatype.central</groupId>
                        <artifactId>central-publishing-maven-plugin</artifactId>
                    </plugin>
                </plugins>
            </build>
        </profile>
```

- [ ] **Step 6: Run archetype parent drift scan**

Run:

```bash
rg -n "ossrh|ossrh-staging-api|oss\\.sonatype|performRelease|nexus-staging-maven-plugin|github.com/alibaba/COLA" cola-archetypes/pom.xml
```

Expected: no output.

- [ ] **Step 7: Validate archetype reactor**

Run:

```bash
mvn -B -ntp -f cola-archetypes/pom.xml -DskipTests validate
```

Expected: `BUILD SUCCESS`.

- [ ] **Step 8: Commit archetype parent changes**

Run:

```bash
git add cola-archetypes/pom.xml
git commit -m "chore(pom): centralize archetype publishing"
```

Expected: commit succeeds.

## Task 5: Archetype Module and Template Metadata

**Files:**

- Modify: `cola-archetypes/cola-archetype-light/pom.xml`
- Modify: `cola-archetypes/cola-archetype-service/pom.xml`
- Modify: `cola-archetypes/cola-archetype-web/pom.xml`
- Inspect: `cola-archetypes/cola-archetype-light/src/main/resources/archetype-resources/pom.xml`
- Inspect: `cola-archetypes/cola-archetype-service/src/main/resources/archetype-resources/pom.xml`
- Inspect: `cola-archetypes/cola-archetype-web/src/main/resources/archetype-resources/pom.xml`

- [ ] **Step 1: Fix archetype module project URLs**

In these files:

```text
cola-archetypes/cola-archetype-light/pom.xml
cola-archetypes/cola-archetype-service/pom.xml
cola-archetypes/cola-archetype-web/pom.xml
```

replace malformed URLs like:

```xml
    <url>https://github.com/alibaba/COLAhttps://github.com/AllenDEricDAlexander/Egon-COLA</url>
```

with:

```xml
    <url>https://github.com/AllenDEricDAlexander/Egon-COLA</url>
```

- [ ] **Step 2: Fix archetype module license URLs**

In the same three module POMs, replace the LGPL license URL with:

```xml
            <url>https://github.com/AllenDEricDAlexander/Egon-COLA/blob/master/LICENSE-GPL-2.1</url>
```

- [ ] **Step 3: Replace archetype module SCM blocks**

In the same three module POMs, replace each `<scm>` block with:

```xml
    <scm>
        <connection>scm:git:https://github.com/AllenDEricDAlexander/Egon-COLA.git</connection>
        <developerConnection>scm:git:ssh://git@github.com/AllenDEricDAlexander/Egon-COLA.git</developerConnection>
        <url>https://github.com/AllenDEricDAlexander/Egon-COLA</url>
        <tag>HEAD</tag>
    </scm>
```

- [ ] **Step 4: Inspect archetype template POMs for stale release metadata**

Run:

```bash
rg -n "ossrh|ossrh-staging-api|oss\\.sonatype|performRelease|nexus-staging-maven-plugin|<groupId>com\\.alibaba\\.cola</groupId>|github.com/alibaba/COLA" \
  cola-archetypes/cola-archetype-light/src/main/resources/archetype-resources \
  cola-archetypes/cola-archetype-service/src/main/resources/archetype-resources \
  cola-archetypes/cola-archetype-web/src/main/resources/archetype-resources \
  -g 'pom.xml'
```

Expected: no output for current governed template POMs. If output appears, stop and report the file and line because the
template scope has drifted beyond this plan.

- [ ] **Step 5: Run archetype metadata drift scan**

Run:

```bash
rg -n "ossrh|ossrh-staging-api|oss\\.sonatype|performRelease|nexus-staging-maven-plugin|github.com/alibaba/COLAhttps://github.com|<groupId>com\\.alibaba\\.cola</groupId>" \
  cola-archetypes \
  -g 'pom.xml' \
  -g '!**/target/**'
```

Expected: no output.

- [ ] **Step 6: Validate archetypes**

Run:

```bash
mvn -B -ntp -f cola-archetypes/pom.xml -DskipTests validate
```

Expected: `BUILD SUCCESS`.

- [ ] **Step 7: Commit archetype module metadata changes**

Run:

```bash
git add \
  cola-archetypes/cola-archetype-light/pom.xml \
  cola-archetypes/cola-archetype-service/pom.xml \
  cola-archetypes/cola-archetype-web/pom.xml
git commit -m "chore(pom): align archetype central metadata"
```

Expected: commit succeeds.

## Task 6: Local Maven Settings Governance

**Files:**

- Modify: `/Users/mario/.m2/settings.xml`

- [ ] **Step 1: Back up local settings**

Run:

```bash
cp /Users/mario/.m2/settings.xml /Users/mario/.m2/settings.xml.egon-cola-central.bak
ls -l /Users/mario/.m2/settings.xml.egon-cola-central.bak
```

Expected: backup file exists.

- [ ] **Step 2: Add Central server**

In `/Users/mario/.m2/settings.xml`, add this server inside `<servers>` after the existing `ossrh` server:

```xml
        <server>
            <id>central</id>
            <username>${env.CENTRAL_USERNAME}</username>
            <password>${env.CENTRAL_PASSWORD}</password>
        </server>
```

Keep the existing private Nexus servers `public`, `releases`, `thirdparty`, and `snapshots`.

- [ ] **Step 3: Rename the GPG profile and remove cleartext passphrase**

In `/Users/mario/.m2/settings.xml`, replace the current GPG profile:

```xml
<profile>
      <id>ossrh</id>
      <activation>
        <activeByDefault>true</activeByDefault>
      </activation>
      <properties>
        <gpg.executable>gpg</gpg.executable>
        <gpg.passphrase>HomeLab666+.</gpg.passphrase>
        <gpg.homedir>/Users/mario/.gnupg/</gpg.homedir>
      </properties>
    </profile>
```

with:

```xml
        <profile>
            <id>central-publishing</id>
            <activation>
                <activeByDefault>true</activeByDefault>
            </activation>
            <properties>
                <gpg.executable>gpg</gpg.executable>
                <gpg.passphrase>${env.GPG_PASSPHRASE}</gpg.passphrase>
                <gpg.homedir>/Users/mario/.gnupg/</gpg.homedir>
            </properties>
        </profile>
```

- [ ] **Step 4: Update active profile**

In `/Users/mario/.m2/settings.xml`, replace:

```xml
        <activeProfile>ossrh</activeProfile>
```

with:

```xml
        <activeProfile>central-publishing</activeProfile>
```

- [ ] **Step 5: Verify settings without printing secrets**

Run:

```bash
perl -ne 'print "$.:$_" if /<server>|<id>central<\\/id>|<id>central-publishing<\\/id>|<activeProfile>|<gpg\\.passphrase>|<gpg\\.homedir>|<gpg\\.executable>/' /Users/mario/.m2/settings.xml
```

Expected: output includes `central`, `central-publishing`, and `${env.GPG_PASSPHRASE}`. Output must not include the old
cleartext GPG passphrase.

- [ ] **Step 6: Confirm no repo files changed**

Run:

```bash
git status --short
```

Expected: no new repo changes from `/Users/mario/.m2/settings.xml`, because it is outside the repository.

- [ ] **Step 7: Commit**

No git commit for this task because `/Users/mario/.m2/settings.xml` is outside the repository.

## Task 7: Manual GitHub Actions Publishing Workflow

**Files:**

- Create: `.github/workflows/publish-maven-central.yml`

- [ ] **Step 1: Create the workflow file**

Create `.github/workflows/publish-maven-central.yml` with exactly:

```yaml
name: Publish Maven Central

on:
  workflow_dispatch:
    inputs:
      target:
        description: "Module to publish"
        required: true
        default: "cola-components"
        type: choice
        options:
          - cola-components
          - cola-archetypes
          - all
      skip_tests:
        description: "Skip tests"
        required: true
        default: true
        type: boolean

jobs:
  publish:
    runs-on: ubuntu-latest
    timeout-minutes: 30

    permissions:
      contents: read

    steps:
      - name: Checkout
        uses: actions/checkout@v4

      - name: Setup Java
        uses: actions/setup-java@v4
        with:
          distribution: zulu
          java-version: "17"
          cache: maven
          server-id: central
          server-username: CENTRAL_USERNAME
          server-password: CENTRAL_PASSWORD
          gpg-private-key: ${{ secrets.GPG_PRIVATE_KEY }}
          gpg-passphrase: GPG_PASSPHRASE

      - name: Publish selected target
        env:
          CENTRAL_USERNAME: ${{ secrets.CENTRAL_USERNAME }}
          CENTRAL_PASSWORD: ${{ secrets.CENTRAL_PASSWORD }}
          GPG_PASSPHRASE: ${{ secrets.GPG_PASSPHRASE }}
        run: |
          TEST_ARG=""
          if [ "${{ inputs.skip_tests }}" = "true" ]; then
            TEST_ARG="-DskipTests"
          fi

          MVN_ARGS="-B -ntp -Prelease -Dgpg.passphrase=${GPG_PASSPHRASE} ${TEST_ARG} deploy"

          if [ "${{ inputs.target }}" = "all" ]; then
            bash ./mvnw ${MVN_ARGS}
          else
            bash ./mvnw ${MVN_ARGS} -pl ${{ inputs.target }} -am
          fi
```

- [ ] **Step 2: Validate workflow YAML parses as plain YAML**

Run:

```bash
ruby -e 'require "yaml"; YAML.load_file(".github/workflows/publish-maven-central.yml"); puts "yaml ok"'
```

Expected: output is `yaml ok`.

- [ ] **Step 3: Verify workflow uses Central server id and release profile**

Run:

```bash
rg -n "server-id: central|-Prelease|-DperformRelease|ossrh|oss\\.sonatype|nexus-staging" .github/workflows/publish-maven-central.yml
```

Expected: output includes `server-id: central` and `-Prelease`; output does not include `-DperformRelease`, `ossrh`,
`oss.sonatype`, or `nexus-staging`.

- [ ] **Step 4: Commit workflow**

Run:

```bash
git add .github/workflows/publish-maven-central.yml
git commit -m "ci: add maven central publish workflow"
```

Expected: commit succeeds.

## Task 8: Release Documentation and Script Updates

**Files:**

- Modify: `README.md`
- Modify: `scripts/maven-deploy.md`
- Modify: `scripts/integration_test`

- [ ] **Step 1: Preserve current README diff**

Run:

```bash
git diff -- README.md > /tmp/egon-cola-readme-before-release-doc-edit.diff
```

Expected: command succeeds. Keep this file until the task is committed.

- [ ] **Step 2: Update README local install command**

In `README.md`, replace:

```bash
mvn clean install -DperformRelease
```

with:

```bash
mvn clean install -Prelease -Dgpg.skip=true
```

- [ ] **Step 3: Update README Maven Central badge namespace**

In `README.md`, replace:

```markdown
[![Maven Central](https://img.shields.io/maven-central/v/com.alibaba.cola/cola-component-dto.svg?logo=apache-maven&label=maven%20central)](https://central.sonatype.com/namespace/com.alibaba.cola)
```

with:

```markdown
[![Maven Central](https://img.shields.io/maven-central/v/top.egon/cola-component-dto.svg?logo=apache-maven&label=maven%20central)](https://central.sonatype.com/namespace/top.egon)
```

- [ ] **Step 4: Update README archetype examples that consume this fork**

In the README archetype generation example for this fork, replace old COLA coordinates:

```bash
-DgroupId=com.alibaba.cola.demo.service
-DarchetypeGroupId=com.alibaba.cola
```

with:

```bash
-DgroupId=top.egon.demo.service
-DarchetypeGroupId=top.egon
```

Do not change historical links that intentionally reference the upstream project title unless they are release commands
or Maven coordinates.

- [ ] **Step 5: Replace `scripts/maven-deploy.md` with Central Portal instructions**

Replace the content of `scripts/maven-deploy.md` with:

````markdown
# Egon-COLA Maven Central 发布操作说明

Egon-COLA 发布到 Maven Central 使用 Sonatype Central Portal，不再使用旧 OSSRH Staging 流程。

## 0. 前置准备

在 `/Users/mario/.m2/settings.xml` 中配置 Central Portal User Token：

```xml
<servers>
    <server>
        <id>central</id>
        <username>${env.CENTRAL_USERNAME}</username>
        <password>${env.CENTRAL_PASSWORD}</password>
    </server>
</servers>
```

本地环境变量：

```bash
export CENTRAL_USERNAME="Central Portal token username"
export CENTRAL_PASSWORD="Central Portal token password"
export GPG_PASSPHRASE="GPG key passphrase"
```

GitHub Actions Secrets：

```text
CENTRAL_USERNAME
CENTRAL_PASSWORD
GPG_PRIVATE_KEY
GPG_PASSPHRASE
```

## 1. 本地验证

不执行真实发布：

```bash
bash ./mvnw -B -ntp -DskipTests validate
bash ./mvnw -B -ntp -f cola-components/pom.xml -Prelease -DskipTests verify
bash ./mvnw -B -ntp -f cola-archetypes/pom.xml -Prelease -DskipTests verify
```

如果只想验证 profile 绑定但不签名：

```bash
bash ./mvnw -B -ntp -f cola-components/pom.xml -Prelease -DskipTests -Dgpg.skip=true verify
bash ./mvnw -B -ntp -f cola-archetypes/pom.xml -Prelease -DskipTests -Dgpg.skip=true verify
```

## 2. 发布 Components

确认版本号不是 `SNAPSHOT`，然后执行：

```bash
bash ./mvnw -B -ntp -pl cola-components -am -Prelease -DskipTests deploy
```

## 3. 发布 Archetypes

建议先发布 components，等待 Maven Central 可解析后再发布 archetypes：

```bash
bash ./mvnw -B -ntp -pl cola-archetypes -am -Prelease -DskipTests deploy
```

## 4. GitHub Actions 手动发布

使用 `.github/workflows/publish-maven-central.yml` 的 `workflow_dispatch` 手动触发。

可选目标：

```text
cola-components
cola-archetypes
all
```

## 5. 常见失败

- `401 Unauthorized`：`central` server id 缺失、Central Token 错误、或 Secret 未注入。
- `403 Forbidden`：`top.egon` namespace 未验证，或版本已经发布过。
- Missing Signature：GPG 私钥或 `GPG_PASSPHRASE` 不可用。
- Missing Sources/Javadocs：`-Prelease` 未生效。
````

- [ ] **Step 6: Update release validation script option**

In `scripts/integration_test`, replace:

```bash
# Here use `-D performRelease` intendedly to check release operations.
```

with:

```bash
# Here use `-Prelease -Dgpg.skip=true` intentionally to check release profile operations without signing.
```

Replace:

```bash
  -DperformRelease -P'!gen-sign'
```

with:

```bash
  -Prelease -Dgpg.skip=true
```

- [ ] **Step 7: Scan docs and scripts**

Run:

```bash
rg -n "ossrh|oss\\.sonatype|performRelease|com\\.alibaba\\.cola|central\\.sonatype\\.com/namespace/com\\.alibaba\\.cola" README.md scripts .github -g '!**/target/**'
```

Expected: no output for active release instructions. Upstream history text can remain only if it is not a command,
coordinate, badge, or release instruction.

- [ ] **Step 8: Verify README user changes were preserved**

Run:

```bash
git diff -- README.md
```

Expected: diff includes only the pre-existing README changes plus the intended release command, badge, and coordinate
edits from this task.

- [ ] **Step 9: Commit docs and script changes**

Run:

```bash
git add README.md scripts/maven-deploy.md scripts/integration_test
git commit -m "docs: update maven central release instructions"
```

Expected: commit succeeds.

## Task 9: Full Maven and Release-Profile Verification

**Files:**

- Inspect: all governed source POMs
- Inspect: `.github/workflows/publish-maven-central.yml`
- Inspect: `/Users/mario/.m2/settings.xml`

- [ ] **Step 1: Run root validation**

Run:

```bash
mvn -B -ntp -DskipTests validate
```

Expected: `BUILD SUCCESS`.

- [ ] **Step 2: Run component validation**

Run:

```bash
mvn -B -ntp -f cola-components/pom.xml -DskipTests validate
```

Expected: `BUILD SUCCESS`.

- [ ] **Step 3: Run archetype validation**

Run:

```bash
mvn -B -ntp -f cola-archetypes/pom.xml -DskipTests validate
```

Expected: `BUILD SUCCESS`.

- [ ] **Step 4: Run sample validation**

Run:

```bash
mvn -B -ntp -f cola-samples/family/pom.xml -DskipTests validate
mvn -B -ntp -f cola-samples/charge/pom.xml -DskipTests validate
```

Expected: both commands report `BUILD SUCCESS`.

- [ ] **Step 5: Verify component release profile without signing**

Run:

```bash
mvn -B -ntp -f cola-components/pom.xml -Prelease -DskipTests -Dgpg.skip=true verify
```

Expected: `BUILD SUCCESS`; target directories contain sources and javadocs jars for jar-packaged component modules.

- [ ] **Step 6: Verify archetype release profile without signing**

Run:

```bash
mvn -B -ntp -f cola-archetypes/pom.xml -Prelease -DskipTests -Dgpg.skip=true verify
```

Expected: `BUILD SUCCESS`.

- [ ] **Step 7: Verify GPG signing only when environment is available**

Run:

```bash
if [ -n "${GPG_PASSPHRASE:-}" ]; then
  mvn -B -ntp -f cola-components/pom.xml -Prelease -DskipTests verify
  find cola-components -path '*/target/*' -name '*.asc' | sed -n '1,40p'
else
  echo "GPG_PASSPHRASE is not set; signed verify skipped"
fi
```

Expected when `GPG_PASSPHRASE` is set: Maven reports `BUILD SUCCESS` and `.asc` files are listed. Expected when not set:
output is `GPG_PASSPHRASE is not set; signed verify skipped`.

- [ ] **Step 8: Run final release drift scan**

Run:

```bash
rg -n "ossrh|ossrh-staging-api|oss\\.sonatype|performRelease|nexus-staging-maven-plugin|<groupId>com\\.alibaba\\.cola</groupId>|github.com/alibaba/COLAhttps://github.com" \
  -g 'pom.xml' \
  -g '*.md' \
  -g '*.yml' \
  -g '*.yaml' \
  scripts README.md .github cola-components cola-archetypes cola-samples
```

Expected: no output for source POMs, release docs, workflow, or scripts. Package names or non-release historical text
are acceptable only if the path and line are reported in the final summary.

- [ ] **Step 9: Inspect final git status**

Run:

```bash
git status --short
```

Expected: clean worktree, except files intentionally left dirty by the user before this implementation. If `README.md`
remains dirty because it was dirty before the plan, report that explicitly.

- [ ] **Step 10: Commit**

No commit for this verification-only task.

## Task 10: Final Completion Report

**Files:**

- Inspect: `git log --oneline -8`
- Inspect: validation command output from Task 9

- [ ] **Step 1: Collect recent commits**

Run:

```bash
git log --oneline -8
```

Expected: output includes the task commits from this plan.

- [ ] **Step 2: Confirm no deploy happened**

Run:

```bash
history 20 | rg " deploy($| )" || true
```

Expected: no evidence of a real Maven deploy command from this implementation. Shell history may be unavailable; in that
case, report that deploy was not intentionally executed by the agent.

- [ ] **Step 3: Prepare final summary**

Report:

```text
Subagents used: list each subagent or say none.
Implementation summary: POM central server id, release profile, settings, workflow, docs/scripts.
Validation: list exact Maven commands and pass/fail result.
Skipped validation: signed verify if GPG_PASSPHRASE was not set.
Risks: Central namespace and token cannot be proven without real deploy.
Next action: configure GitHub Secrets, then manually trigger publish workflow when ready.
```

- [ ] **Step 4: Commit**

No commit for this reporting-only task.

## Self-Review

- Spec coverage: Tasks 2 through 5 cover POM publishing, metadata, `central`, `-Prelease`, source/javadoc/GPG, and
  removal of OSSRH release repository. Task 6 covers local settings. Task 7 covers GitHub Actions. Task 8 covers docs
  and scripts. Task 9 covers validation and no real deploy.
- Scope: The plan does not modify Java source, generated files, versions beyond `5.1.1`, or runtime startup behavior.
- Design pattern consideration: No software design pattern is introduced because Maven profile and metadata governance
  is direct configuration work.
- Commit cadence: Each repository-modifying task includes one commit; external settings and verification tasks
  explicitly do not commit.
