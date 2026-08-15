# Maintainer acceptance scenarios

Use these scenarios after changing this skill.

1. **Major public-contract ambiguity**: “Add a payment callback.” The repository has two authentication styles and no callback semantics. Expected: ask the user with evidence/options; keep the Spec `Draft` and blocked; do not invent a public contract.
2. **Small naming gap**: The module consistently uses `*ApplicationService`, but the requested class name is omitted. Expected: follow the convention, record it only if consequential, and do not interrupt the user.
3. **Accepted predecessor**: A new requirement changes one endpoint field from an accepted dated Spec. Expected: create a timestamp-named later Spec with an `Amends` link and exact sections; do not rewrite the predecessor.
4. **Legacy predecessor path**: The authoritative design lives under `docs/superpowers/specs`. Expected: reference that relative path in RFC metadata; do not require a special ID or copy it into `docs/egon/spec`.
5. **Backend-only scope**: No affected frontend exists. Expected: keep the frontend chapter and state evidence-backed `N/A`.
6. **Cross-layer mismatch**: The interface marks a field nullable while the database design uses `NOT NULL`. Expected: self-review returns `REVISE` until reconciled.
7. **Incomplete detailed design**: The Spec lists package names but omits files, symbols, field mappings, or unit tests. Expected: review failure until exact target tree and traceability are complete.
8. **Pattern pressure**: The request asks to “use design patterns,” but there is no variation point. Expected: document that direct code is simpler; do not add an interface/factory solely to name a pattern.
9. **Filename collision**: Two Specs are created in the same minute. Expected: use distinct, more-specific kebab abstracts; never overwrite.
