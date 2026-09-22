---
name: egon-coding-create-new-module
description: >
  Create one real business project from exactly one non-open Egon-COLA Maven
  archetype (egon-cola-archetype-light, service, web, or agent). Use when the
  user asks to create a new module or project from egon-cola-archetypes, runs
  /egon-coding-create-new-module, or asks to generate a non-open archetype.
  Do not select -open artifacts, copy source-projects, or handwrite a skeleton.
  This skill does not replace write-spec, write-plan, or exec-plan.
---

# Create a business project from a non-open archetype

Generate one Maven project with `archetype:generate`, then stop for review. Do not write a Spec, a Plan, business code, SQL, or codegen output in this skill.

## When this skill applies

Use it only when the user wants a new business project or module that does not exist yet.

If the work changes a project that already exists, stop and use `egon-coding-writing-spec`. Do not generate a second tree beside it.

## Steps

1. Read `references/archetype-selection.md` completely. It owns the allowed artifactIds, forbidden `-open` artifactIds, required properties, version source, output rules, command shape, and post-checks.
2. Pick exactly one allowed archetype from that table. If two fit, ask before generate. If the user already named one allowed archetype, use it.
3. If `groupId`, `artifactId`, `version`, `package`, output parent, or a required peer-facade coordinate is missing, ask for the missing fields once. Do not invent a peer facade. Do not default a peer to a `source-projects` sample.
4. Read the archetype version from `egon-cola-archetypes/pom.xml`. Run the command in `references/archetype-selection.md` with `./mvnw` from the Egon-COLA repository root.
5. If the archetype cannot be resolved, stop and show the Maven error. Ask before any local install. Do not publish. Do not run `scripts/generate_archetypes.sh`.
6. Run the post-checks in `references/archetype-selection.md`. On failure, report the mismatch and do not patch the tree by hand.
7. Stop. Report the archetype, version, output path, topology, and whether codegen later applies (`light`, `web`, `service`) or does not (`agent`).

## What the model does not write

- Do not copy `egon-cola-archetypes/source-projects`.
- Do not edit `definitions` or `.generated`.
- Do not handwrite `pom.xml`, packages, or a module skeleton when generate fails.
- Do not select or rename an `-open` archetype.
- Do not add the project to a reactor POM unless the user asks.
- Do not start the process, database, or browser.

Later SQL changes on native `light`, `web`, and `service` refresh catalog Java and Mapper XML only through `scripts/egon-codegen.sh`, as required by `egon-coding-writing-spec`. This skill does not restate that catalog. `agent` is a legal non-open archetype and is outside the generator; do not invent its persistence templates.

## Handoff

After a successful generate, the next skill is `egon-coding-writing-spec` on the generated project. Do not start it unless the user asks.
