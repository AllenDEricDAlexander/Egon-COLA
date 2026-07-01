# Maven Central POM Governance Design

Date: 2026-07-01

## Goal

Finish the Maven Central publishing governance for Egon-COLA by replacing the remaining OSSRH-era release configuration with a single Sonatype Central Portal flow.

The governed result should let the project build locally as it does today, prepare release artifacts with sources, javadocs, and GPG signatures through an explicit `release` profile, and publish manually through GitHub Actions or a local `mvn deploy` command using a `central` server id.

## Confirmed Decisions

- Use `top.egon` as the project namespace.
- Keep the already governed release version `5.1.1`.
- Use `org.sonatype.central:central-publishing-maven-plugin:0.11.0`.
- Use `central` as the Maven server id everywhere for Central Portal publishing.
- Use `-Prelease` as the explicit release profile.
- Stop using `-DperformRelease` for the Central release flow.
- Do not execute a real `deploy` during this task.
- Do not start or run any application.

## Current Context

The repository already has a partially governed Maven model:

- The root POM is a non-publishing development aggregator for `cola-components` and `cola-archetypes`.
- `cola-components` and `cola-archetypes` use `top.egon:5.1.1`.
- `cola-component-job` is already part of the component reactor and BOM.
- Basic Maven validation passes for the root reactor, component reactor, archetype reactor, and samples.
- The Maven wrapper file is not executable through `./mvnw`, but `bash ./mvnw ...` works.

Remaining drift:

- Publishing plugin configuration still uses `publishingServerId` value `ossrh`.
- Snapshot repository ids still use `ossrh`.
- `cola-archetypes/pom.xml` still has a release repository that points to the OSSRH Staging API compatibility endpoint.
- Release behavior is split across `gen-java-src`, `gen-java-doc`, `gen-sign`, `deploy-settings`, and Java-enforcer profiles activated by `performRelease`.
- Several POM metadata URLs still point at Alibaba COLA or contain malformed combined URLs.
- Local `/Users/mario/.m2/settings.xml` has an `ossrh` server but no `central` server.
- Local GPG passphrase is currently stored as a clear Maven property in settings.
- README and release scripts still mention old release commands and coordinates.

## Scope

In scope:

- Root-level release documentation where it directly prevents old publishing commands from being reused.
- `cola-components/pom.xml`.
- `cola-components/cola-components-bom/pom.xml`.
- Component module POM metadata where Maven Central metadata is stale or malformed.
- `cola-archetypes/pom.xml`.
- Archetype module POM metadata.
- Archetype template POMs only where generated projects still point to stale publishing metadata or old component coordinates.
- Sample POMs only where they must remain valid consumers of `top.egon:5.1.1`.
- `.github/workflows/publish-maven-central.yml`.
- `/Users/mario/.m2/settings.xml` server/profile governance, without exposing secrets.

Out of scope:

- Java source changes.
- Dependency upgrades unrelated to publishing.
- Version bumping beyond the existing `5.1.1`.
- Automatic version calculation or release tagging.
- GitHub Release creation.
- Real upload to Maven Central.
- Editing generated `target/` files.
- Starting local services or applications.

## Target Publishing Model

The project should use one release path:

```text
mvn deploy
  -> release profile
  -> sources jar
  -> javadocs jar
  -> GPG signatures
  -> central-publishing-maven-plugin
  -> Sonatype Central Portal
  -> Maven Central
```

Snapshot publishing should keep using the Central Portal snapshot repository and the same `central` server credentials:

```text
mvn deploy with x.y.z-SNAPSHOT
  -> Central Portal snapshot endpoint
```

No source POM should keep the old Nexus staging plugin or an OSSRH release repository as part of the normal release path.

## POM Design

### Shared Publishing Properties

`cola-components/pom.xml`, `cola-archetypes/pom.xml`, and any standalone publishable BOM POM should expose release plugin values through properties where the current file structure makes that practical:

```xml
<central.server.id>central</central.server.id>
<central.publishing.maven.plugin.version>0.11.0</central.publishing.maven.plugin.version>
<maven.source.plugin.version>3.3.1</maven.source.plugin.version>
<maven.javadoc.plugin.version>3.7.0</maven.javadoc.plugin.version>
<maven.gpg.plugin.version>3.1.0</maven.gpg.plugin.version>
```

If a file already manages plugin versions directly and adding properties would make the POM noisier, the implementation may keep explicit versions. The invariant is that the same versions are used consistently.

### Distribution Management

Release repositories should be removed from publishable parents because release upload is handled by `central-publishing-maven-plugin`.

Snapshot repositories should remain:

```xml
<distributionManagement>
    <snapshotRepository>
        <id>${central.server.id}</id>
        <name>Central Portal Snapshots</name>
        <url>https://central.sonatype.com/repository/maven-snapshots/</url>
    </snapshotRepository>
</distributionManagement>
```

If a child POM duplicates the same snapshot repository without needing to override anything, remove the duplicate and inherit from the parent.

### Release Profile

Each publishable Maven root should have one explicit profile:

```bash
-Prelease
```

The profile should attach:

- `maven-source-plugin` using `jar-no-fork` or the nearest existing project style.
- `maven-javadoc-plugin` using UTF-8 and doclint suppression.
- `maven-gpg-plugin` bound to `verify`.
- `central-publishing-maven-plugin` with `publishingServerId` set to `${central.server.id}`.
- A Java version enforcer only where the existing POM already uses one for release builds.

The old property activation:

```xml
<name>performRelease</name>
```

should be removed from the governed release path.

### Component Reactor

`cola-components/pom.xml` remains the component parent and keeps all existing component modules. Its release profile should be the source of publishing behavior for component artifacts where possible.

`cola-components/cola-components-bom/pom.xml` should remain publishable as `top.egon:cola-components-bom:5.1.1`. It should keep only `top.egon` repository-local component entries and should not reintroduce `com.alibaba.cola` component coordinates.

### Archetype Reactor

`cola-archetypes/pom.xml` remains the archetype parent. It should remove the OSSRH Staging API release repository and publish through the Central Portal plugin with server id `central`.

Archetype module POMs should have valid Maven Central metadata:

- Project URL should be `https://github.com/AllenDEricDAlexander/Egon-COLA`.
- SCM URLs should point to the Egon-COLA repository.
- Issue management should point to the Egon-COLA issue tracker.
- License metadata should stay consistent with the repository license files.

Malformed combined URLs such as an Alibaba URL concatenated with the Egon-COLA URL must be corrected.

### Samples and Templates

Samples and archetype templates should stay aligned with the governed component coordinates:

- Component dependencies use `top.egon`.
- Component BOM version is `5.1.1`.
- Generated projects continue using their existing module layout.

Samples are not release artifacts and should not be added to the root reactor.

## Settings Design

`/Users/mario/.m2/settings.xml` should preserve unrelated private Nexus configuration and add Central Portal credentials without exposing secrets in committed files or command output.

Target server:

```xml
<server>
    <id>central</id>
    <username>${env.CENTRAL_USERNAME}</username>
    <password>${env.CENTRAL_PASSWORD}</password>
</server>
```

The existing `ossrh` server can be left in place for compatibility with other local work unless the user explicitly asks to remove it.

GPG properties should avoid cleartext secrets:

```xml
<gpg.executable>gpg</gpg.executable>
<gpg.passphrase>${env.GPG_PASSPHRASE}</gpg.passphrase>
<gpg.homedir>/Users/mario/.gnupg/</gpg.homedir>
```

If changing the active profile would affect unrelated local builds, add a new Central-specific profile and activate it only for this project through command-line profile activation or documented local setup.

## GitHub Actions Design

Add `.github/workflows/publish-maven-central.yml` as a manual workflow using `workflow_dispatch`.

Inputs:

- `target`: `cola-components`, `cola-archetypes`, or `all`.
- `skip_tests`: boolean, default true.

Secrets:

- `CENTRAL_USERNAME`
- `CENTRAL_PASSWORD`
- `GPG_PRIVATE_KEY`
- `GPG_PASSPHRASE`

The workflow should use `actions/setup-java@v4` with Java 17, Maven cache, `server-id: central`, and GPG import. The publish command should use `-Prelease`, not `-DperformRelease`.

## Documentation and Script Design

Documentation and scripts that instruct release behavior should stop pointing at OSSRH and `-DperformRelease`.

Update only the parts directly related to publishing and validation:

- README local archetype/install command examples.
- `scripts/maven-deploy.md`.
- `scripts/integration_test` Maven options if it still exists as release-profile validation.

If any of these files are dirty before implementation starts, inspect the existing changes and merge with them instead of overwriting user work.

## Error Handling

Expected failure classes should be documented or reported honestly:

- `401 Unauthorized`: `central` server id missing, wrong Central Portal token, or secrets not injected.
- `403 Forbidden`: namespace `top.egon` is not verified for the Central account, or the version already exists.
- Missing signature: GPG key or passphrase not available.
- Missing sources or javadocs: release profile did not attach required artifacts.
- Maven wrapper permission error: use `bash ./mvnw ...` unless executable permission is intentionally restored later.

No implementation should hide these errors by disabling publishing checks.

## Validation Plan

After implementation, run the smallest relevant checks first:

```bash
mvn -B -ntp -DskipTests validate
mvn -B -ntp -f cola-components/pom.xml -DskipTests validate
mvn -B -ntp -f cola-archetypes/pom.xml -DskipTests validate
mvn -B -ntp -f cola-samples/family/pom.xml -DskipTests validate
mvn -B -ntp -f cola-samples/charge/pom.xml -DskipTests validate
```

Then validate release artifact preparation without deploying:

```bash
mvn -B -ntp -f cola-components/pom.xml -Prelease -DskipTests verify
mvn -B -ntp -f cola-archetypes/pom.xml -Prelease -DskipTests verify
```

Expected release verification evidence:

- Sources jars exist for jar-packaged artifacts.
- Javadocs jars exist for jar-packaged artifacts.
- `.asc` signature files are generated when GPG credentials are available.
- No `nexus-staging-maven-plugin` remains in source POMs.
- No `oss.sonatype.org` release path remains in source POMs.
- No governed release profile depends on `performRelease`.

If GPG credentials are not available in the environment, report the exact failing command and stop before any real deploy.

## Design Pattern Consideration

No software design pattern should be introduced. This is Maven configuration and release-pipeline governance. Direct profile consolidation, parent/BOM inheritance, and one manual workflow are clearer and safer than introducing scripts, code generators, factories, or extra abstraction layers.

## Risks

- Central Portal publishing cannot be proven without valid namespace ownership and credentials.
- Replacing cleartext GPG settings with environment variables may require local shell setup before a release verify can sign artifacts.
- `maven-gpg-plugin` behavior can differ between local GPG installations and GitHub Actions imported keys.
- Release versions on Maven Central are immutable; a failed published version may require the next patch version.
- README is currently dirty in the working tree, so implementation must not discard those existing changes.

## Acceptance Criteria

- Source POMs use `central` for Central Portal publishing server ids.
- Source POMs do not use `nexus-staging-maven-plugin`.
- Source POMs do not use OSSRH release repository URLs.
- Publishable roots expose a single `release` profile for sources, javadocs, signing, and Central Portal publishing.
- `performRelease` is removed from the governed release path.
- Maven Central metadata URLs are valid and point to Egon-COLA where appropriate.
- `/Users/mario/.m2/settings.xml` has a `central` server that reads credentials from environment variables.
- Local GPG passphrase is not kept as cleartext in the governed settings profile.
- Manual GitHub Actions publishing workflow exists and uses `central` credentials.
- Targeted Maven validation is run and results are reported honestly.
- No real deploy is attempted without explicit user approval.
