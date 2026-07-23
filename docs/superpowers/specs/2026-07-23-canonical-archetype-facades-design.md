# Canonical Archetype Facades Design

Date: 2026-07-23

Status: User-approved direction A. Implementation planning remains gated until
the user reviews this written specification.

## Goal

Make the web and service archetypes generate successfully with only the standard
Maven archetype inputs, while preserving real Dubbo provider/consumer
compatibility.

Two published Facade artifacts become the canonical cross-project contracts:

```text
top.egon:egon-cola-organization-facade:${egon-cola.version}
top.egon:egon-cola-evaluation-facade:${egon-cola.version}
```

The organization Facade contains the public contracts implemented by the
generated web project. The evaluation Facade contains the public contracts
implemented by the generated service project. Both generated projects use these
same artifacts when acting as providers or consumers.

## Confirmed Decisions

The user approved these decisions on 2026-07-23:

1. Use canonical shared Facade artifacts rather than compile-only placeholder
   JARs.
2. Install the Facade artifacts locally first and publish them to Maven Central
   in a later release step.
3. Generation must not require users to enter the current
   `organizationFacade*` or `evaluationFacade*` custom properties.
4. The dependency design must eliminate Maven cycles instead of relying on a
   special build order between generated web and service projects.
5. The generated providers and consumers must use the same contract classes and
   fully qualified class names.
6. Validation must not start generated applications or external services.

## Root Cause

The current archetypes mix two contract models:

- each generated project owns a `${rootArtifactId}-facade` module;
- the opposite project is also expected to be supplied as an external Facade
  artifact through four required archetype properties.

Consequently, batch or IDE generation fails before files are created when those
four custom properties are absent. Supplying arbitrary placeholder coordinates
would make generation pass, but would not make Dubbo compatible: the provider
would implement `${package}.facade.*`, while the consumer could import a
different package from the placeholder JAR.

## Architecture

### Canonical Facade Modules

Add these two JAR modules to the `egon-cola-archetypes` reactor before the three
archetype modules:

```text
egon-cola-archetypes/
├── egon-cola-organization-facade
├── egon-cola-evaluation-facade
├── egon-cola-archetype-light
├── egon-cola-archetype-service
└── egon-cola-archetype-web
```

Their stable Java package roots are:

```text
top.egon.cola.organization.facade
top.egon.cola.evaluation.facade
```

The modules contain interfaces, serializable request/response DTOs, protocol
enums, protocol exceptions, and contract-local assertion helpers only. They
must not depend on generated web/service implementations, Spring Boot
applications, repositories, databases, messaging brokers, or infrastructure
adapters.

### Generated Web Project

The web archetype no longer generates its own Facade contract module.

Its adapter module implements interfaces from:

```text
top.egon:egon-cola-organization-facade
```

Its evaluation client imports interfaces and DTOs from:

```text
top.egon:egon-cola-evaluation-facade
```

The generated root reactor becomes:

```text
common -> domain -> application -> infrastructure -> adapter -> starter
```

Dependencies continue to follow the existing layer rules. Removing the local
Facade module does not merge Facade implementation into the contract artifact;
implementations remain in the adapter module.

### Generated Service Project

The service archetype no longer generates its own Facade contract module.

Its adapter module implements interfaces from:

```text
top.egon:egon-cola-evaluation-facade
```

Its organization client imports interfaces and DTOs from:

```text
top.egon:egon-cola-organization-facade
```

Its generated root reactor uses the same six-module layer sequence as the web
project.

## Why This Has No Circular Dependency

The resulting Maven dependency graph is:

```text
egon-cola-organization-facade ─┐
                               ├─> generated web project
egon-cola-evaluation-facade ───┘

egon-cola-organization-facade ─┐
                               ├─> generated service project
egon-cola-evaluation-facade ───┘
```

Neither Facade artifact depends on web or service. The generated web project
does not depend on the generated service artifact, and the generated service
project does not depend on the generated web artifact. Both depend only on the
two already-built contract JARs.

This is a directed acyclic graph. Web and service may therefore be generated,
compiled, tested, installed, and deployed independently and in either order
after the two Facade artifacts are available.

## Generation-Time Injection

The generated root POMs receive fixed canonical group IDs, artifact IDs, and
package roots directly from their archetype templates. Facade versions reference
the existing generated property:

```xml
<egon-cola.version>5.2.3</egon-cola.version>
```

and use:

```xml
<version>${egon-cola.version}</version>
```

This removes the eight required custom archetype properties and avoids adding a
second mutable version literal. The existing version-bump script already updates
the generated `egon-cola.version`, so future Facade versions advance with the
Egon-COLA release.

No runtime injection, post-generation artifact installation, reflection, or
custom code generator is introduced.

## Source-of-Truth Rule

The two canonical modules are the only source of truth for their public Java
contracts. The equivalent generated Facade source trees and the archetype-local
minimal fixture source trees are removed after the canonical modules cover their
contracts.

Provider implementations, consumer adapters, tests, documentation, and
`verify.groovy` checks must import the stable canonical packages. A contract type
must not be copied back into a generated project.

## Build and Publication Order

The archetypes reactor lists the canonical Facade modules first. For local use:

```bash
./mvnw -B -ntp -f egon-cola-archetypes/pom.xml clean install
```

This installs both Facades before validating generated projects.

For Maven Central, the Facade artifacts are published in the same release
reactor before the archetypes that reference them.

### Existing 5.2.3 Release Boundary

Local development may rebuild and overwrite the local `5.2.3` artifacts.
Published Maven Central release artifacts are immutable. Therefore:

- local verification may continue with `5.2.3`;
- the existing remote `egon-cola-archetype-web:5.2.3` and
  `egon-cola-archetype-service:5.2.3` cannot be replaced;
- the Central release containing the corrected archetypes must use the next
  repository version, expected to be `5.2.4`;
- both canonical Facades and the corrected archetypes must be published at that
  same new version.

Publishing only the two new `5.2.3` Facade coordinates would not repair the
already-published `5.2.3` archetype descriptors.

## Pattern Decision

The existing infrastructure clients remain Adapters between domain ports and
Dubbo Facades. The new Facade modules form a stable published contract boundary.

No Strategy, Factory, Template Method, or service-locator abstraction is added.
The variation is not behavioral; it is a contract-ownership problem. A single
published contract per business boundary is simpler and safer than parallel
generated and external definitions.

## Error Handling

- Missing canonical artifacts must fail Maven dependency resolution with their
  exact coordinates.
- Unresolved archetype placeholders are forbidden in generated POMs and Java
  imports.
- Provider implementations must fail compilation if they drift from the
  canonical interface.
- Consumer tests must use canonical DTOs and exceptions, so incompatible API
  changes fail during the archetype build.
- Facade artifacts must remain serialization-safe and free of implementation
  dependencies.

## Validation

Validation is layered:

1. Compile and test both canonical Facade modules.
2. Verify their JAR contents and dependency shape.
3. Generate web and service projects with only standard Maven archetype
   parameters.
4. Assert that generated projects:
   - contain no local Facade module;
   - contain no unresolved Facade placeholders;
   - reference both canonical Facade artifacts through
     `${egon-cola.version}`;
   - implement and consume the canonical FQCNs.
5. Run each generated project's `clean verify`.
6. Run the full archetypes reactor through `clean install`.
7. Use an isolated Maven repository to install the reactor, generate both
   archetypes from the local catalog, and verify both generated projects.
8. Run `git diff --check`.

Runtime applications, containers, databases, brokers, and browsers are not
started.

## Scope

Implementation may change:

- `egon-cola-archetypes/pom.xml`;
- the two new canonical Facade modules;
- web/service archetype template POMs, Java sources, tests, metadata, generated
  READMEs, and living architecture documents;
- web/service archetype integration fixtures and `verify.groovy`;
- root generation documentation where commands or module descriptions change;
- release-shape checks that enumerate published artifacts.

## Out of Scope

- Changing Facade method semantics or DTO field meaning.
- Adding HTTP endpoints to the service archetype.
- Changing persistence schemas or Flyway migrations.
- Starting generated applications or infrastructure.
- Deploying artifacts to Maven Central in this implementation task.
- Migrating projects generated by older archetype versions.
- Introducing compatibility aliases for the removed generated Facade modules.
