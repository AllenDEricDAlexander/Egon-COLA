# EGON Coding Spec review checklist

## Original-request fidelity

- [ ] Every explicit user requirement maps to a `REQ-*` or an explicit non-goal.
- [ ] Requirements are atomic and acceptance criteria are observable.
- [ ] User constraints and exclusions remain literal; none were weakened or reconstructed from a different source.
- [ ] Major ambiguities/defects were not silently decided.
- [ ] Small assumptions are minimal, reversible, and supported by repository evidence.

## Repository fidelity

- [ ] Applicable `AGENTS.md` instructions and repository status were checked.
- [ ] Languages, versions, frameworks, module boundaries, build/test commands, and migration rules come from current files.
- [ ] Existing call chains, consumers, reusable code, schemas, pages, and historical Specs are cited with exact paths/symbols/sections.
- [ ] Proposed packages, files, names, dependencies, comments, and annotations follow current project style.
- [ ] Static/source evidence is not presented as live-runtime proof.
- [ ] No unrelated refactor, new dependency, or architecture layer was smuggled into scope.

## Design completeness and consistency

- [ ] Architecture, target file tree, interfaces, fields, models, schema, pages, tests, rollout, and failure behavior describe the same system.
- [ ] The target tree names exact Create/Modify/Delete paths, symbols, responsibilities, ownership, consumers, and requirement mapping.
- [ ] Interface fields map through DTO/domain/persistence/frontend layers where applicable.
- [ ] Entity invariants, state transitions, database constraints, transaction boundaries, locks, idempotency, and error semantics agree.
- [ ] Frontend routes, permissions, components, user flows, states, validation, and copy agree with contracts.
- [ ] Unit tests target isolated production behavior; higher-level tests have separate responsibilities.
- [ ] Security, tenancy, compatibility, migration, observability, rollback, and operational concerns are covered or evidence-backed `N/A`.

## Design-pattern and architecture review

- [ ] Each selected pattern names the concrete variation point/problem, placement, and repository precedent.
- [ ] Direct implementation was considered and rejected only for a concrete reason.
- [ ] Rejected patterns and YAGNI trade-offs prevent needless interfaces, factories, handlers, or inheritance.
- [ ] Dependency direction, cohesion, coupling, information hiding, and testability match the stated architecture principles.

## RFC governance

- [ ] Filename matches `YYYY-MM-DD-HH-MM-abstract.md`; header document/timestamps/status match it.
- [ ] `Amends`, `Supersedes`, `Depends On`, `Related Specs`, and `Related Plans` use valid relative links or `None`.
- [ ] Amendment/supersession scope names exact predecessor sections and unchanged content remains effective.
- [ ] Approved predecessor normative text was not silently rewritten.
- [ ] Legacy authoritative design paths are linked directly rather than hidden because they lack a new naming convention.
- [ ] `Accepted` is backed by explicit user/owner approval.

## Traceability and final gate

- [ ] Every `REQ-*` maps to design, tests, and acceptance evidence.
- [ ] Every proposed interface, model, file, migration, page, and test maps to a requirement or necessary infrastructure rationale.
- [ ] Happy path, boundaries, invalid input, permissions, tenancy, concurrency, failure, migration, compatibility, and regression cases are considered as applicable.
- [ ] No unresolved `TBD`, `TODO`, `FIXME`, vague placeholder, or internal contradiction remains in a `Review`/`Accepted` document.
- [ ] Every non-applicable mandatory chapter says `N/A` with evidence and reason.
- [ ] Final verdict is exactly `PASS`, `BLOCKED`, or `REVISE` and matches reality.
- [ ] No Plan, production code, migration execution, service start, or runtime claim was produced as a side effect.
