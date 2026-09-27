# Non-open archetype selection

This file is the only selection table for `egon-coding-create-new-module`. Read it completely before `archetype:generate`.

Allowed coordinates, all `top.egon`:

| Archetype | Choose when | Topology in `egon-cola-archetypes/definitions/<artifactId>/archetype.properties` | Codegen `projectType` |
| --- | --- | --- | --- |
| `egon-cola-archetype-light` | One deployable module. HTTP/GraphQL stay in that module. No separate facade module. | `expectedTopology=root` | `light` |
| `egon-cola-archetype-web` | Seven-module HTTP/GraphQL/OpenAPI product. It publishes its own facade; an optional peer Evaluation facade adds a client integration. | `common,facade,domain,application,infrastructure,adapter,starter` | `web` |
| `egon-cola-archetype-service` | Seven-module RPC/MQ service. No HTTP controllers. It publishes its own facade; an optional peer Organization facade adds a client integration. | same seven modules | `service` |
| `egon-cola-archetype-agent` | Six-module process-local agent (Agent Flow, one authenticated SSE command). No durable database and no Egon RPC. | `common,domain,application,infrastructure,adapter,starter` | unsupported |

Forbidden for this skill: `egon-cola-archetype-light-open`, `egon-cola-archetype-service-open`, `egon-cola-archetype-web-open`, and any other artifactId containing `-open`. If the user asks for the public Spring/Open baseline, stop. Do not generate it and do not rename an open archetype to a native one.

Select exactly one row. If two rows fit, ask before generate. Do not mix module sets.

One generated project already has a root POM. For an explicitly requested outer reactor containing several generated projects, read `references/multi-project-parent.md` before deciding whether its POM needs inheritance; the normal solution is an aggregation-only POM with no Maven `<parent>`. The generated project's root keeps its direct published Egon parent.

For Egon Component/Platform capability questions during project creation, the same reference explains the separation of the outer `<modules>`, inherited BOM/dependency management, and the actual `<dependencies>` of the generated inner module. Do not add Egon source repositories to the business reactor or introduce an unapproved dependency merely because the archetype parent manages its version.

## Required generation properties

Always pass `groupId`, `artifactId`, `version`, `package`, and `interactiveMode=false`.

`gitignore` already defaults to `.gitignore`. Do not override it unless the user asks.

Service accepts an optional peer Organization contract. If requested, pass all four properties together:

- `organizationFacadeGroupId`
- `organizationFacadeArtifactId`
- `organizationFacadeVersion`
- `organizationFacadePackage`

Web accepts an optional peer Evaluation contract. If requested, pass all four properties together:

- `evaluationFacadeGroupId`
- `evaluationFacadeArtifactId`
- `evaluationFacadeVersion`
- `evaluationFacadePackage`

Omit all four properties when the new project has no peer Facade yet. The generated project then contains no peer dependency, client, domain service/value objects, or peer configuration. Passing only some properties is invalid. When all four are supplied, the existing peer integration is generated. Do not point those properties at `top.egon.internal.archetype.source` or a `source-projects` facade unless the user explicitly names that sample contract.

Light and agent have no peer-facade properties.

## Version and catalog

Read the archetype version from `egon-cola-archetypes/pom.xml` (`parent` / `version`) at generation time. Do not invent a version.

Generate from this checkout with `./mvnw`. Add `-DarchetypeCatalog=local` only after that version is already installed locally. If Maven cannot resolve the archetype, stop with the error and ask before any install. Do not publish, and do not run `scripts/generate_archetypes.sh`. That script packages archetypes; it does not create a business project.

## Where the project is written

Ask for the output parent directory when the user has not named one. Do not create the project inside the Egon-COLA repository unless the user explicitly says it belongs there.

Never write under:

- `egon-cola-archetypes/source-projects`
- `egon-cola-archetypes/definitions`
- `egon-cola-archetypes/.generated`

Do not add the new project to an outer reactor POM unless the user asks. If requested, use `references/multi-project-parent.md`; include the generated project root path once, without rewriting its `<parent>` or listing its internal modules again.

## Command shape

Run from the Egon-COLA repository root. Replace every angle-bracket field. Add the selected archetype's four peer flags only when the user supplied a real peer Facade.

```bash
./mvnw -B -ntp archetype:generate \
  -DarchetypeGroupId=top.egon \
  -DarchetypeArtifactId=<egon-cola-archetype-light|service|web|agent> \
  -DarchetypeVersion=<version from egon-cola-archetypes/pom.xml> \
  -DgroupId=<groupId> \
  -DartifactId=<artifactId> \
  -Dversion=<version> \
  -Dpackage=<package> \
  -DoutputDirectory=<parent directory> \
  -DinteractiveMode=false
```

When a Service peer is supplied, also pass `-DorganizationFacadeGroupId`, `-DorganizationFacadeArtifactId`, `-DorganizationFacadeVersion`, and `-DorganizationFacadePackage`. When a Web peer is supplied, pass the four `evaluationFacade*` properties instead. With no peer, pass none of them.

## After generate

Check all of these before reporting success:

- The new directory exists at `<parent>/<artifactId>` and was not written into `source-projects`, `definitions`, or `.generated`.
- Root `pom.xml` inherits `top.egon:egon-cola-archetypes-parent` at the version you passed, with an empty `relativePath`.
- If an outer aggregation POM was requested, verify that it lists this generated root exactly once, its business GAV is user-specified, and any other listed child paths actually exist. An aggregation POM does not replace this generated root parent.
- Module directories match `expectedTopology` for that archetype. Light is one module (`root`). Agent has no `facade` module.
- For Web or Service without peer coordinates, the root and infrastructure POMs contain no peer-facade properties or dependency, and the peer client/source/configuration files are absent. With all four coordinates, verify the existing peer dependency and client remain.
- The chosen artifactId does not contain `-open`.
- Do not delete the archetype's sample domain during creation. Treat it as reference material; the later real-business Spec/Plan must name its removal or replacement paths, and execution must complete that cleanup in the owning Step.
- Do not start the application, write SQL, or run `scripts/egon-codegen.sh` while creating the skeleton.

Then stop. The next skill is `egon-coding-writing-spec` against the generated project. For `light`, `web`, and `service`, only initial CREATE scaffolding uses the generator rule in that skill; later ALTER-driven code changes are agent-owned. `agent` has no codegen profile; do not invent persistence templates for it.
