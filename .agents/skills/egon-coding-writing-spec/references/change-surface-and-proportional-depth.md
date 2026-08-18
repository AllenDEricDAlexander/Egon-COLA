# Change Surface and Proportional Design Depth

Read this reference before deciding which Spec chapters need detailed design. Its purpose is to keep a focused coding task focused: repository architecture determines where a change lives, but the existence of adjacent layers does not make those layers part of the change.

## Contents

- [Core rule](#core-rule)
- [Two independent classification axes](#two-independent-classification-axes)
- [Change-surface dispositions](#change-surface-dispositions)
- [Impact-cone discovery](#impact-cone-discovery)
- [Writing depth by disposition](#writing-depth-by-disposition)
- [Chapter routing](#chapter-routing)
- [Scope-expansion gate](#scope-expansion-gate)
- [Worked DAO-only example](#worked-dao-only-example)
- [Review gate](#review-gate)

## Core rule

Design the smallest coherent change surface that satisfies the approved requirements. Fully design what changes; inspect adjacent code only far enough to prove compatibility and safety.

Do not expand a Spec merely because the repository has Controller, Service, DAO, database, and frontend layers. A layer is affected only when the proposed implementation changes its code, public or internal contract, data/schema semantics, configuration, runtime behavior, or required test responsibility.

Reading a file, calling through a layer, or citing an existing contract for evidence does not by itself make that layer affected.

## Two independent classification axes

Keep these decisions separate:

| Axis | Question | Values | Consequence |
| --- | --- | --- | --- |
| Complexity | How risky or interaction-heavy are the decisions? | `Simple` / `Complex` | Controls analysis depth, scenarios, evidence chains, and diagrams |
| Change surface | Which repository areas actually change? | `Affected` / `Context-only` / `Unchanged` / `Not applicable` | Controls which chapters and elements receive detailed design |

A Simple task may cross several layers. A Complex task may still be confined to one subsystem. Never use `Simple` as a shortcut for missing affected details, and never use `Complex` as permission to redesign unaffected layers.

## Change-surface dispositions

Classify every materially relevant repository area with exactly one disposition:

| Disposition | Meaning | Required evidence | Allowed Spec treatment |
| --- | --- | --- | --- |
| `Affected` | Code, contract, data/schema semantics, configuration, runtime behavior, or test responsibility changes | Exact target paths/symbols and the requirement causing the change | Full applicable detailed design |
| `Context-only` | The area is inspected to prove an affected boundary, dependency, call chain, transaction, access path, or compatibility invariant, but it is not modified | Exact caller/callee/contract/schema evidence and the invariant being checked | Concise evidence, preserved behavior, and verification only |
| `Unchanged` | The area exists and is within the surrounding system, but the approved change neither modifies nor relies on a new decision there | Exact existing contract/path plus why the change stops before this area | One concise unchanged record; no target redesign |
| `Not applicable` | The area or concern does not exist in the repository/task | Evidence of absence or irrelevance | Evidence-backed `N/A` |

Do not write `N/A` for an existing Controller, Service, schema, or frontend that is merely unchanged. `N/A` means not applicable; `Unchanged` means applicable context exists but remains outside the change.

## Impact-cone discovery

Build the change surface before target design:

1. Start from the user-named behavior, file, symbol, query, table, route, page, or failure.
2. Identify the exact implementation symbols expected to change.
3. Trace direct callers until an existing input/output/error/transaction invariant proves that no outward contract change is required.
4. Trace direct callees and state effects until ownership, persistence, external effects, and failure behavior are understood.
5. Inspect tests that currently prove the behavior and tests that must change or be added.
6. Classify each inspected area using the four dispositions.
7. Record the affected design chapters in the Spec header and change-surface matrix.
8. Stop expanding when the next boundary remains semantically identical and can be protected by a focused regression assertion.

The impact cone must widen when repository evidence proves that the requested outcome requires a change to any of the following:

- caller-visible input, output, error, ordering, pagination, or timing semantics;
- Service or other internal contract signatures and invariants;
- persistent fields, constraints, relationships, indexes, migrations, transaction or locking behavior;
- permission, tenant, identity, audit, cache, event, external dependency, or configuration behavior;
- frontend routing, fields, state transitions, copy, permission behavior, or cache invalidation;
- deployment, compatibility, rollout, rollback, or operational ownership.

An incidental defect or attractive refactor outside the required impact cone does not widen scope. Record it as an out-of-scope risk or follow-up only when material.

## Writing depth by disposition

### Affected

Write exact paths, symbols, responsibilities, contracts, fields, branches, state effects, failures, compatibility rules, and tests needed for implementation. Apply every chapter-specific reference that governs the changed element.

### Context-only

Write only:

- the exact repository evidence inspected;
- the existing boundary or invariant relied upon;
- why no modification is required;
- the focused verification that prevents accidental change.

Do not restate complete current APIs, models, table definitions, page states, or architecture for context-only areas.

### Unchanged

Use a concise record such as:

```text
Scope disposition: Unchanged
Evidence: <exact path/symbol/current contract>
Preserved invariant: <what remains identical>
Reason: <why the affected path stops before this area>
Verification: <focused regression/static check, or evidence-backed not required>
```

Do not add target files, new fields, new diagrams, new interface IDs, or implementation decisions to an unchanged area.

### Not applicable

Write `N/A` plus repository evidence and the reason. Do not use a placeholder or a generic statement such as “not needed.”

## Chapter routing

Keep all numbered top-level chapters for stable RFC review, but allocate depth by the change-surface matrix:

| Chapter/area | Focused treatment |
| --- | --- |
| Summary, goals, requirements, review | Always describe the exact requested outcome and bounded acceptance |
| Background/current chain | Trace only the chain needed to locate the change and prove where it stops |
| Technology context | Record only technologies and repository rules constraining affected work |
| Architecture | Design affected collaboration and boundaries; cite preserved surrounding architecture without redrawing the whole system |
| Package/file tree | Show exact changed files plus only the parent/context paths needed to locate them; do not emit an empty full-layer skeleton |
| Interfaces | Fully expand only added, removed, or materially changed contracts; otherwise cite the current contract and preserved invariant |
| POJO/models | Fully design only changed/new types, fields, mappings, or lifecycle rules; do not inventory unrelated models |
| Database | Fully design only changed schema/data/constraint/index/transaction semantics; a DAO query-only change records the exact access path and relevant existing index evidence without reproducing the whole table or ER model |
| Frontend | Design pages/components/states only when frontend behavior changes; existing unaffected frontend is `Unchanged`, not a page-redesign task |
| Patterns/principles | Consider patterns only for the affected variation point; do not survey every pattern or restate the whole architecture |
| Tests | Design focused changed-behavior tests and the smallest boundary regressions proving unaffected contracts remain stable |
| Cross-cutting/release | Expand only materially affected concerns; otherwise record the preserved invariant and validation boundary |

Conditional detailed references follow the same rule. Load the interface, database, POJO, or frontend depth rules when that surface is `Affected`; for `Context-only` or `Unchanged`, inspect the authoritative current artifact only far enough to prove the recorded invariant.

## Scope-expansion gate

When discovery shows that the user-named scope cannot produce the requested behavior alone:

1. Record the evidence and the exact additional area that would become `Affected`.
2. Determine whether the expansion changes public behavior, data/schema, permissions, ownership, compatibility, deployment, or another major decision.
3. For a major expansion, stop before designing that area and ask the user to approve the widened scope with options and impact.
4. For a small, local, reversible implementation detail inside an already approved contract, infer the repository-consistent choice and record it if consequential.

Never silently change a DAO-only request into a Controller/API/frontend redesign. Never force a DAO-only solution when repository evidence proves the Service or contract must change; surface that conflict instead.

## Worked DAO-only example

Request: adjust one existing DAO query filter without changing the Controller, Service contract, database schema, or frontend behavior.

| Area/layer | Disposition | Evidence/invariant | Spec treatment |
| --- | --- | --- | --- |
| Controller/API | `Unchanged` | Existing route, request, response, and error contract remain identical | Cite route/symbol and regression boundary; do not reproduce JSON contracts |
| Service interface | `Unchanged` | Method signature and business result semantics remain identical | Cite symbol and preserved invariant |
| Service implementation | `Context-only` | Existing method delegates to the DAO and requires the corrected result set | Show only the relevant call and expected result semantics |
| DAO/Mapper query | `Affected` | Predicate/join/order/paging logic changes | Fully design exact symbol, SQL shape, parameter/null rules, result semantics, and failures |
| Database schema/relationships | `Unchanged` | Existing columns, constraints, and relationships are sufficient | Cite relevant migration/table/index evidence; no migration or ER redesign |
| Tests | `Affected` | Query regression must prove inclusion/exclusion, boundaries, ordering, and existing caller behavior | Define focused DAO/Mapper tests and the smallest Service regression if needed |
| Frontend | `Unchanged` | No contract or visible state changes | One evidence-backed unchanged record; no page/component design |

Expected detailed content is the DAO/Mapper method, SQL predicates and bindings, ordering/pagination, null/empty semantics, relevant existing access path/index, mapped result, exception behavior, and focused tests. Full Controller payloads, page trees, unrelated POJO inventories, full table definitions, and an unchanged ER diagram are scope violations.

## Review gate

Return `REVISE` when any of these occurs:

- a layer is fully redesigned only because it exists in the architecture;
- an existing but unchanged layer is mislabeled `N/A`;
- a `Context-only` or `Unchanged` row introduces target files, contracts, fields, pages, schema, or abstractions;
- the detailed chapters contain elements absent from the `Affected` rows;
- an affected element lacks full applicable design because the task was labeled Simple;
- repository evidence proves scope must widen, but the Spec silently widens or silently keeps an impossible narrow scope;
- tests do not prove both the changed behavior and the stated preserved boundary.
