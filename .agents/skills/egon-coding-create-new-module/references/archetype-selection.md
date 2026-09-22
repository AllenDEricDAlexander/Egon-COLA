# Non-open archetype selection

This file is the only selection table for `egon-coding-create-new-module`. Read it completely before `archetype:generate`.

Allowed coordinates, all `top.egon`:

| Archetype | Choose when | Topology in `egon-cola-archetypes/definitions/<artifactId>/archetype.properties` | Codegen `projectType` |
| --- | --- | --- | --- |
| `egon-cola-archetype-light` | One deployable module. HTTP/GraphQL stay in that module. No separate facade module. | `expectedTopology=root` | `light` |
| `egon-cola-archetype-web` | Seven-module HTTP/GraphQL/OpenAPI product. It publishes its own facade and consumes a peer Evaluation facade. | `common,facade,domain,application,infrastructure,adapter,starter` | `web` |
| `egon-cola-archetype-service` | Seven-module RPC/MQ service. No HTTP controllers. It publishes its own facade and consumes a peer Organization facade. | same seven modules | `service` |
| `egon-cola-archetype-agent` | Six-module process-local agent (Agent Flow, one authenticated SSE command). No durable database and no Egon RPC. | `common,domain,application,infrastructure,adapter,starter` | unsupported |

Forbidden for this skill: `egon-cola-archetype-light-open`, `egon-cola-archetype-service-open`, `egon-cola-archetype-web-open`, and any other artifactId containing `-open`. If the user asks for the public Spring/Open baseline, stop. Do not generate it and do not rename an open archetype to a native one.

Select exactly one row. If two rows fit, ask before generate. Do not mix module sets.

## Required generation properties

Always pass `groupId`, `artifactId`, `version`, `package`, and `interactiveMode=false`.

`gitignore` already defaults to `.gitignore`. Do not override it unless the user asks.

Service has no default for the peer contract. Generation must fail closed until the user names all four:

- `organizationFacadeGroupId`
- `organizationFacadeArtifactId`
- `organizationFacadeVersion`
- `organizationFacadePackage`

Web has no default for the peer contract. Require all four:

- `evaluationFacadeGroupId`
- `evaluationFacadeArtifactId`
- `evaluationFacadeVersion`
- `evaluationFacadePackage`

Do not point those properties at `top.egon.internal.archetype.source` or a `source-projects` facade unless the user explicitly names that sample contract.

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

Do not add the new project to a reactor POM unless the user asks.

## Command shape

Run from the Egon-COLA repository root. Replace every angle-bracket field. Omit peer flags that the selected archetype does not require.

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

Service also passes `-DorganizationFacadeGroupId`, `-DorganizationFacadeArtifactId`, `-DorganizationFacadeVersion`, and `-DorganizationFacadePackage`. Web passes the four `evaluationFacade*` properties instead.

## After generate

Check all of these before reporting success:

- The new directory exists at `<parent>/<artifactId>` and was not written into `source-projects`, `definitions`, or `.generated`.
- Root `pom.xml` inherits `top.egon:egon-cola-archetypes-parent` at the version you passed, with an empty `relativePath`.
- Module directories match `expectedTopology` for that archetype. Light is one module (`root`). Agent has no `facade` module.
- The chosen artifactId does not contain `-open`.
- Do not delete the archetype's sample domain in this skill. Replacing it is later Spec work.
- Do not start the application, write SQL, or run `scripts/egon-codegen.sh` while creating the skeleton.

Then stop. The next skill is `egon-coding-writing-spec` against the generated project. For `light`, `web`, and `service`, later SQL changes refresh catalog Java and Mapper XML only through the generator rule in that skill. `agent` has no codegen profile; do not invent persistence templates for it.
