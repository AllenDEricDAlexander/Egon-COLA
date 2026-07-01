# Egon-COLA JDK 21 POM Governance Design

Date: 2026-07-01

## Goal

Govern the Maven structure for this fork so framework coordinates and module directories belong to `top.egon` and use `egon-cola` as the artifact prefix. Upgrade the build baseline to JDK 21 and Maven Wrapper 3.9.14, while applying conservative dependency and plugin modernization that remains compatible with the existing Java code.

The work should not start or run any application.

## Confirmed Decisions

- Use `top.egon` as the unified Maven `groupId`.
- Keep the existing project version `5.1.1`.
- Use `egon-cola` as the starting prefix for framework, component, BOM, and archetype artifactIds.
- Rename framework/component/archetype directories to match the new `egon-cola-*` coordinates.
- Preserve sample business module names such as `family-*`, `start`, and `charge`.
- Upgrade all governed POM Java settings to JDK 21.
- Upgrade the Maven Wrapper distribution to Apache Maven 3.9.14.
- Use a medium dependency upgrade strategy: upgrade to Java 21 compatible stable lines, but avoid changes that require broad Java source adaptation.

## Scope

In scope:

- Root aggregation POM.
- Maven Wrapper properties.
- Component parent, component modules, and component BOM POMs.
- Component module directories and root module declarations.
- Archetype parent and archetype module POMs.
- Archetype module directories and parent module declarations.
- Archetype resource template POMs under `src/main/resources/archetype-resources`.
- Sample POMs under `cola-samples` where they consume governed component coordinates or define Java/build versions.
- Release profile settings already present in governed POMs, only where needed to keep Java 21 and Central Portal publishing coherent.

Out of scope:

- Java source behavior changes.
- README, scripts, CI, generated `target/` outputs, and non-POM documentation unless validation proves a POM-only change leaves a broken build contract.
- Changing sample business module directory names.
- Starting services or running applications.
- Real Maven Central deployment.
- Editing generated files under `target/`.

## Target Naming Model

The repository-local framework artifacts should move from the old `cola-*` coordinate family to the `egon-cola-*` coordinate family.

Examples:

- Root aggregator: `top.egon:egon-cola-aggregation-parent:5.1.1`.
- Component parent: `top.egon:egon-cola-components-parent:5.1.1`.
- Component BOM: `top.egon:egon-cola-components-bom:5.1.1`.
- Component module: `top.egon:egon-cola-component-dto:5.1.1`.
- Archetype parent: `top.egon:egon-cola-archetypes-parent:5.1.1`.
- Archetype module: `top.egon:egon-cola-archetype-web:5.1.1`.

Physical framework directories should follow the same prefix:

- `cola-components` -> `egon-cola-components`.
- `cola-components/cola-component-*` -> `egon-cola-components/egon-cola-component-*`.
- `cola-components/cola-components-bom` -> `egon-cola-components/egon-cola-components-bom`.
- `cola-archetypes` -> `egon-cola-archetypes`.
- `cola-archetypes/cola-archetype-*` -> `egon-cola-archetypes/egon-cola-archetype-*`.

Sample projects keep their business directory and module names. Their dependencies and BOM imports should consume `top.egon:egon-cola-*` framework artifacts.

## Build Baseline

All governed POMs should use Java 21 consistently:

- `<java.version>21</java.version>`.
- `<maven.compiler.source>${java.version}</maven.compiler.source>` and `<maven.compiler.target>${java.version}</maven.compiler.target>` may remain where the project already uses them.
- `maven-compiler-plugin` should prefer `<release>${java.version}</release>` where plugin configuration is centrally managed.
- Release profile javadoc source settings should be updated from 17 to 21.
- Enforcer rules should require Maven `[3.9.14,)` and Java `[21,)` where those rules already exist or belong in the publishable parent.

The Maven Wrapper should use:

```properties
distributionUrl=https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.14/apache-maven-3.9.14-bin.zip
```

`wrapperVersion` should only change if Maven Wrapper metadata requires it for compatibility; otherwise the current wrapper script model can remain.

## Dependency and Plugin Governance

The component parent should remain the main place for component dependency and plugin management. The implementation should:

- Keep Spring Boot and its managed dependency ecosystem on a Java 21 compatible stable line.
- Align Spring Cloud and related BOMs in samples and archetype templates with the chosen Spring Boot line.
- Upgrade Maven plugins to compatible stable versions where the upgrade is low risk.
- Prefer current coordinates for dependencies whose old artifact coordinates are deprecated or relocated, if the replacement is build-compatible.
- Remove redundant dependency versions from child POMs when they are already managed by the parent or imported BOM.
- Avoid broad dependency jumps that require Java source rewrites.

The BOM should publish repository-local framework artifacts under `top.egon:egon-cola-*`. It should not keep old repository-local `com.alibaba.cola` or `cola-component-*` artifact entries.

## Archetype and Template Governance

Archetype modules should use the new `egon-cola-archetype-*` coordinate family and matching directories.

Template POMs should generate projects that:

- Use Java 21.
- Import `top.egon:egon-cola-components-bom:5.1.1`.
- Reference framework components through `top.egon:egon-cola-component-*`.
- Preserve generated business module names based on archetype variables such as `${rootArtifactId}`, `${artifactId}`, `${groupId}`, and `${version}`.

The implementation must preserve archetype placeholder syntax and avoid replacing generated project variables with hard-coded fork coordinates.

## Publishing Model

The existing Central Portal publishing direction stays in place:

- Use `org.sonatype.central:central-publishing-maven-plugin`.
- Use server id `central`.
- Use explicit `-Prelease` for source jars, javadocs, GPG signatures, and Central Portal publishing.
- Do not reintroduce `nexus-staging-maven-plugin`.
- Do not attempt a real deploy.

Publishing metadata should be adjusted only as required by the new `egon-cola-*` coordinates and Java 21 release profile.

## Validation Plan

Start with targeted validation:

```bash
bash ./mvnw -B -ntp -DskipTests validate
bash ./mvnw -B -ntp -f egon-cola-components/pom.xml -DskipTests validate
bash ./mvnw -B -ntp -f egon-cola-archetypes/pom.xml -DskipTests validate
bash ./mvnw -B -ntp -f cola-samples/family/pom.xml -DskipTests validate
bash ./mvnw -B -ntp -f cola-samples/charge/pom.xml -DskipTests validate
```

Then validate local artifact resolution after coordinate and directory renaming:

```bash
bash ./mvnw -B -ntp -f egon-cola-components/pom.xml -DskipTests install
bash ./mvnw -B -ntp -f egon-cola-archetypes/pom.xml -DskipTests install
bash ./mvnw -B -ntp -f cola-samples/family/pom.xml -DskipTests validate
bash ./mvnw -B -ntp -f cola-samples/charge/pom.xml -DskipTests validate
```

If Java compilation fails because a dependency upgrade removed or changed an API, prefer the smallest compatible version adjustment before changing Java source. Java source changes should only be made if validation proves they are necessary and the change is narrow.

## Design Pattern Consideration

No software design pattern should be introduced. This is Maven and repository structure governance. Parent POM inheritance, BOM dependency management, and plugin management are the existing Maven mechanisms that directly solve the problem. Adding scripts, generators, factories, or extra abstraction would make the change harder to review without improving maintainability.

## Risks

- Directory renaming touches many module references and can break reactor builds if any path is missed.
- Archetype templates contain Maven placeholders, so automated replacement must not corrupt `${groupId}`, `${artifactId}`, `${version}`, or `${rootArtifactId}`.
- Java 21 and dependency upgrades may expose old APIs or annotation processor issues.
- Central Portal publishing can be validated structurally but cannot be proven without credentials and namespace ownership.
- The working tree currently has unrelated changes under `scripts/bash-buddy`; this task must not modify or stage them.

## Acceptance Criteria

- All governed framework POM coordinates use `top.egon` and `egon-cola-*`.
- Framework/component/archetype directories are renamed to match the `egon-cola-*` coordinate family.
- Sample business module names are preserved.
- Sample and archetype template POMs consume `top.egon:egon-cola-*` framework dependencies.
- Java configuration is consistently upgraded to 21.
- Maven Wrapper uses Maven 3.9.14.
- Dependency and plugin versions are upgraded to Java 21 compatible stable versions without broad Java rewrites.
- Central Portal release profile remains coherent and does not use legacy Nexus staging publishing.
- Relevant Maven validation commands are run and reported honestly.
- No application is started.
