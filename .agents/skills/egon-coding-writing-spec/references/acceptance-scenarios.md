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
10. **Simple internal CRUD model**: PO, DTO, BO, Entity, and VO would have identical semantics and never cross an external boundary. Expected: apply the class-necessity test and reuse the repository-standard type where safe; do not generate a class and mapper per layer.
11. **Persistence/public boundary**: An ORM Entity contains audit, tenant, lazy relation, and internal status fields, while the endpoint exposes a stable subset. Expected: keep the Entity internal and design only the necessary Request/Response or DTO/View Object boundary types.
12. **Ambiguous DO/VO vocabulary**: Existing modules use `DO` inconsistently and use `VO` for frontend views. Expected: state the chosen local meaning, avoid introducing a conflicting `DO` definition, and keep `VO` as View Object.
13. **Persistence base class**: The repository already has a stable `BaseEntity` for ID, audit timestamps, tenant ID, and optimistic versioning. Expected: PO/ORM Entity inheritance may be selected after documenting ORM, equality, serialization, migration, compatibility, and test implications; it is not mandatory for unrelated persistence objects.
14. **Business-service reuse**: Two classes in `biz.service.impl` share calculation logic. Expected: extract and compose a calculator/policy/strategy collaborator; do not introduce `BaseBusinessService` solely for protected helper reuse.
15. **Service implementation placement**: A proposed tree places `biz.impl` beside `biz.service`. Expected: review fails until the tree uses `biz.service.impl` and maps each implementation to its Service interface.
16. **Controller bypasses Service**: A Controller injects DAO or a concrete implementation directly. Expected: review fails; the Controller must depend on the Service interface and persistence stays behind `service.impl`.
17. **Existing non-three-layer module**: The affected module follows DDD, COLA, hexagonal, or a custom architecture. Expected: preserve the existing structure and ask before a structural migration; do not silently force the three-layer profile.
18. **Simple three-layer CRUD**: The task is ordinary CRUD with no real domain abstraction need. Expected: use Controller, Service, `service.impl`, DAO, and only justified POJO roles; do not add aggregates, domain services, or repository ports.
