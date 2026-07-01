# POM Governance Design

Date: 2026-07-01

## Goal

Govern all source POM files in this repository without changing Java code, runtime behavior, generated `target/` outputs, scripts, or documentation. The result should make Maven coordinates, versions, dependency management, plugin management, archetype templates, samples, and publishing configuration consistent enough for local validation and Maven Central release preparation.

## Confirmed Decisions

- Use `top.egon` as the unified Maven groupId for this fork.
- Treat `cola-component-job` as a first-class component.
- Use `5.1.1` as the unified release version.
- Use the Maven Central publishing flow based on `central-publishing-maven-plugin`.
- Synchronize archetype template POMs and sample POMs to the governed `top.egon:5.1.1` component coordinates.
- Modify POM files only.

## Scope

In scope:

- Root development aggregation POM.
- Component parent, component modules, and component BOM POMs.
- `cola-component-job` POM integration into the component reactor.
- Archetype parent and archetype module POMs.
- Archetype resource template POMs under `src/main/resources/archetype-resources`.
- Sample POMs under `cola-samples`.

Out of scope:

- Java source code changes.
- README, scripts, CI, generated `target/` POMs, and other non-POM files.
- Runtime test execution or project startup.
- Changing repository history or generated artifacts.

## Architecture

The repository keeps three Maven roles:

- Root aggregator: `pom.xml` remains a non-publishing development aggregator for local IDE import and broad validation. It aggregates `cola-components` and `cola-archetypes`; samples remain independently validated.
- Component reactor: `cola-components/pom.xml` becomes the authoritative parent for all `cola-component-*` modules, including `cola-component-job`.
- Archetype reactor: `cola-archetypes/pom.xml` remains the parent for archetype packaging and publishing.

Samples are validation consumers, not release modules. They should consume the governed BOM and component coordinates exactly as generated projects are expected to do.

## Component POM Design

`cola-components/pom.xml` should manage:

- `groupId`: `top.egon`
- `version`: `5.1.1`
- Java 17 compiler configuration.
- Spring Boot dependency BOM version.
- Shared dependency versions such as Lombok, test libraries, Redis/JPA/test helper dependencies when used by multiple component modules.
- Plugin versions for resources, compiler, surefire, source, javadoc, gpg, deploy, Central publishing, jacoco, and git metadata where still needed.

Every component module should inherit from `cola-components-parent`. Module POMs should avoid declaring versions already managed by the parent or imported BOM. `cola-component-job` should be added to the component module list and converted from an independent project into a child module.

Obvious metadata errors should be corrected, including module `name` and `description` values that currently reference `cola-component-job` in unrelated modules. Dynamic dependency versions such as `RELEASE` should be replaced with managed or explicit stable versions.

## BOM Design

`cola-components/cola-components-bom/pom.xml` should publish as:

- `top.egon:cola-components-bom:5.1.1`

The BOM should manage all externally consumable component artifacts from this repository:

- `cola-component-dto`
- `cola-component-exception`
- `cola-component-statemachine`
- `cola-component-domain-starter`
- `cola-component-extension-starter`
- `cola-component-catchlog-starter`
- `cola-component-test-container`
- `cola-component-ruleengine`
- `cola-component-unittest`
- `cola-component-job`

No repository-local component entry in the BOM should keep the old `com.alibaba.cola` groupId.

## Archetype POM Design

`cola-archetypes/pom.xml` remains version `5.1.1` and uses `top.egon` coordinates. Publishing plugin configuration should use the Central publishing plugin and should not keep the old Nexus staging plugin path.

Archetype template POMs under `src/main/resources/archetype-resources` should generate projects that:

- Use Java 17 consistently.
- Reference `top.egon:cola-components-bom:5.1.1`.
- Reference repository component dependencies through `top.egon`.
- Keep plugin and dependency versions aligned with the governed component and sample POMs.

The generated project structure and module names should not change.

## Sample POM Design

`cola-samples/family` should mirror the governed web archetype output and validate as a generated project using `top.egon:cola-components-bom:5.1.1`.

`cola-samples/charge` should mirror the governed light archetype output where applicable, including the `top.egon` component coordinates and consistent Java/Spring dependency versions.

Samples should not be added to the root reactor unless later requested. They remain separate validation targets.

## Publishing Design

Release-related POM configuration should use the Maven Central publishing plugin:

- `org.sonatype.central:central-publishing-maven-plugin`
- publishing server id: `ossrh`
- auto publish enabled where the current release profile already publishes.

Old `org.sonatype.plugins:nexus-staging-maven-plugin` configuration should be removed from governed POMs when it is only present for the legacy OSSRH flow.

Deployment skip behavior should remain in places that are explicitly non-publishing, such as the root development aggregation POM and sample projects.

## Validation Design

Targeted validation should be run after POM changes:

1. `mvn validate -DskipTests`
2. `mvn -f cola-components/pom.xml validate -DskipTests`
3. `mvn -f cola-archetypes/pom.xml validate -DskipTests`
4. `mvn -f cola-samples/family/pom.xml validate -DskipTests`
5. `mvn -f cola-samples/charge/pom.xml validate -DskipTests`

If these pass, run an install-level validation to prove local BOM and sample resolution are coherent:

1. `mvn -f cola-components/pom.xml install -DskipTests`
2. `mvn -f cola-samples/family/pom.xml validate -DskipTests`
3. `mvn -f cola-samples/charge/pom.xml validate -DskipTests`

No project startup is part of this task.

## Design Pattern Consideration

No software design pattern should be introduced. This is Maven configuration governance, and direct parent/BOM/plugin-management consolidation is simpler and clearer than adding scripts, generators, inheritance tricks, or new build abstractions.

## Risks

- Full `5.1.1` release coordinates may require all referenced artifacts to be installed locally before standalone sample validation can pass.
- Archetype template POMs contain Maven placeholder syntax, so edits must preserve archetype variables such as `${groupId}`, `${artifactId}`, `${version}`, and `${rootArtifactId}`.
- Central publishing settings can be validated syntactically, but actual publishing depends on external credentials and should not be attempted as part of this task.
- Existing local Maven mirror settings may affect dependency resolution during validation.

## Acceptance Criteria

- All source POMs use consistent `top.egon` repository-local coordinates.
- Component, BOM, archetype, template, and sample versions align on `5.1.1`.
- `cola-component-job` is part of the component parent reactor and BOM.
- Old repository-local `com.alibaba.cola` references are removed from governed source POMs.
- Legacy Nexus staging publishing configuration is removed from governed release profiles in favor of the Central publishing plugin.
- Targeted Maven validation commands are run and reported honestly.
