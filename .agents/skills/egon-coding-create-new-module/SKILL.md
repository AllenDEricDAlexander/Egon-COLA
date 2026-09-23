---
name: egon-coding-create-new-module
description: >
  Create one real business project from exactly one non-open Egon-COLA Maven
  archetype (light, service, web, or agent), or design/create an explicitly
  requested outer parent/aggregator POM for several generated projects. Use
  when the user asks to generate a non-open archetype, runs
  /egon-coding-create-new-module, or asks which parent a generated project or
  multi-project reactor should use, including how that generated project
  initially obtains Egon-COLA Component and Platform dependencies.
  Do not select -open artifacts, copy source-projects, or handwrite a skeleton.
  This skill does not replace write-spec, write-plan, or exec-plan.
---

# Create a business project from a non-open archetype

Generate one Maven project with `archetype:generate` when requested, then stop for review. When the request concerns only the outer parent POM, give the repository-grounded parent/aggregation answer without generating a business project. Do not write a Spec, a Plan, business code, SQL, or codegen output in this skill.

## When this skill applies

Use it when the user wants a new business project/module or asks about the outer POM that aggregates such projects.

If the work changes existing business code, stop and use `egon-coding-writing-spec`. A narrowly requested outer aggregator POM may be created or updated through `references/multi-project-parent.md`; do not generate a second business tree beside an existing one.

## Steps

1. Read `references/archetype-selection.md` completely for any generation. When an outer parent, multiple independent projects, Maven parent inheritance, or Egon Component/Platform dependencies in a generated project are in question, also read `references/multi-project-parent.md` completely before choosing a POM relationship.
2. Decide whether the request is for one standalone generated project, a pure outer reactor, or explicitly shared parent inheritance. A standalone generated project already has a root POM; the default outer reactor has no Maven `<parent>` and preserves each generated root's direct `egon-cola-archetypes-parent` inheritance.
3. For generation, pick exactly one allowed archetype from the selection table. If two fit, ask before generate. If the user already named one allowed archetype, use it. For a parent-only request, skip generation.
4. If required identities for the requested action are missing, ask once. For generation these are `groupId`, `artifactId`, `version`, `package`, output parent, and any required peer-facade coordinates. For an outer reactor these are its business GAV and the exact child paths. Never invent a peer facade or default it to a `source-projects` sample.
5. Read the archetype version from `egon-cola-archetypes/pom.xml`. For generation run the command in `references/archetype-selection.md` with `./mvnw` from the Egon-COLA repository root. A parent-only answer does not run Maven generation.
6. If the archetype cannot be resolved, stop and show the Maven error. Ask before any local install. Do not publish or run `scripts/generate_archetypes.sh`.
7. Run the applicable generation and optional outer-reactor checks in the references. A failure is reported with evidence; do not patch the generated tree to force a pass.
8. Stop. Report the selected inheritance/aggregation relationship, and for generation the archetype, version, output path, topology, and whether codegen later applies (`light`, `web`, `service`) or does not (`agent`).

## Egon modules in a new business project

Use the capability placement table in `references/multi-project-parent.md`. The outer reactor lists business project roots; it does not list `egon-cola-components` or `egon-cola-xingyuan` source modules. The generated root inherits the archetypes parent's dependency management, including the Components BOM and some named Platform artifacts. Each inner module declares only the Egon dependency it actually uses. A managed version does not automatically put that dependency on the classpath, and the outer aggregator's dependency management does not flow into generated roots that do not inherit it. Do not add or upgrade a dependency in an existing project under this scaffold skill; use the Spec/Plan/Execute dependency approval workflow.

## What the model does not write

- Do not copy `egon-cola-archetypes/source-projects`.
- Do not edit `definitions` or `.generated`.
- Do not handwrite the archetype project's `pom.xml`, packages, or module skeleton when generate fails. An outer aggregation-only `pom.xml` is a separate, explicit user-requested artifact governed by `references/multi-project-parent.md`.
- Do not select or rename an `-open` archetype.
- Do not add the project to an outer reactor POM unless the user asks. Never change a generated root's Maven parent merely to add it to a reactor.
- Do not start the process, database, or browser.

Later SQL changes on native `light`, `web`, and `service` refresh catalog Java and Mapper XML only through `scripts/egon-codegen.sh`, as required by `egon-coding-writing-spec`. This skill does not restate that catalog. `agent` is a legal non-open archetype and is outside the generator; do not invent its persistence templates.

## Handoff

After a successful generate, the next skill is `egon-coding-writing-spec` on the generated project. Do not start it unless the user asks.
