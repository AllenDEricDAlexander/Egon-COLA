# Ambiguity and decision policy

## Major ambiguity or defect: ask the user

A question is major when a wrong answer would materially change any of these:

- business semantics, scope, acceptance criteria, or user-visible behavior;
- public API, RPC, event, error, versioning, or compatibility contracts;
- aggregate boundaries, module ownership, dependency direction, or architecture style;
- persistent schema, retention, migration, backfill, rollback, or irreversible data changes;
- authentication, authorization, tenancy, privacy, security, or audit obligations;
- technology/framework selection, external dependencies, deployment topology, or operational cost;
- concurrency, transactions, consistency, idempotency, or financial correctness;
- destructive actions, rollout strategy, cross-team ownership, or legal/compliance obligations.

Do not decide silently. State repository evidence, the impact of leaving the issue unresolved, 2–3 viable options when useful, and a clearly labelled recommendation. Ask the smallest coherent set of blocking questions. A recommendation is not a decision.

If a major defect means the requested result contradicts current architecture, existing contracts, or accepted Specs, explain the conflict and require a decision before finalizing the affected design.

## Small gap: infer and record when consequential

A gap is small only when the choice is local, reversible, and cannot change external semantics already decided. Examples:

- internal helper/class naming that follows a dominant convention;
- placement inside an established package boundary;
- test fixture names, local variable names, formatting, and deterministic ordering;
- reuse of an existing project utility rather than creating an equivalent helper;
- exact test file placement where nearby tests establish one clear convention.

Use the smallest repository-consistent inference. Record it as `ASM-*` when another implementer needs to know it or when a wrong inference would cause rework; include evidence and impact.

## Red flags

Stop and ask when reasoning contains statements like:

- “the business probably wants ...”;
- “we can just change the table/API/event ...”;
- “security is unclear, so assume ...”;
- “this refactor should also move unrelated modules ...”;
- “the predecessor conflicts, but the new approach is cleaner ...”;
- “we can select a framework now and revisit compatibility later ...”.

These are user or architecture decisions, not harmless fill-in-the-blanks.
