# POJO Role and Object-Model Design

Read this reference before writing Spec Chapter 10 and the service-structure part of Chapter 13. Treat the names as semantic roles, not as a checklist that requires one class per row.

## Canonical role vocabulary

| Term | Full name | Design meaning | Typical use |
| --- | --- | --- | --- |
| POJO | Plain Old Java Object | Umbrella term for ordinary Java objects; not a layer or required suffix | General object category |
| PO | Persistent Object | Persistence representation aligned with a table, row, or stored record | Repository/mapper persistence boundary |
| DO | Data Object / Domain Object | Ambiguous team-specific term | Use only when the repository defines which meaning applies |
| DTO | Data Transfer Object | Data transferred between layers, modules, processes, or services | Boundary transport without business ownership |
| VO | View Object | Data shaped for frontend or presentation | API/page display output; do not use `VO` to abbreviate a DDD Value Object when the repository uses `VO` for views |
| BO | Business Object | Object used for internal service calculation or orchestration | Intermediate business computation when it has distinct semantics |
| Entity | Entity | Identity-bearing ORM or DDD object | State and lifecycle with stable identity; state whether it is a persistence entity, domain entity, or both |
| DAO | Data Access Object | Database-access component, not a data carrier | Persistence access operations |
| Query / QO | Query Object | Read-condition carrier | Search and filtering inputs |
| Command / CO | Command Object | Mutation-intent carrier | Create/update/delete or other state-changing use cases |
| Request | Request Object | Controller or API input | Transport validation and request compatibility boundary |
| Response | Response Object | Controller or API output | Stable transport response boundary |
| Form | Form Object | Form-submission input | UI form binding when distinct from the API request |
| Param | Parameter Object | Grouped method or API parameters | Avoid long parameter lists when the group has one coherent meaning |
| PageQuery | Page Query Object | Pagination plus query conditions | Paged read input |
| PageResult | Page Result Object | Paged items plus pagination metadata | Paged read output |

A DDD Value Object is a domain concept defined by value equality and invariants. Name it by the domain concept, such as `Money` or `EmailAddress`, and do not conflate it with the `VO` View Object suffix.

## Repository-first classification

1. Inspect existing suffixes, package placement, framework annotations, serializers, mappers, persistence types, and public contracts before proposing a name.
2. Preserve a consistent repository definition unless it violates an explicit user decision or creates a documented correctness problem.
3. When `DO`, `VO`, or `Entity` is ambiguous in the repository, state the selected meaning in the Spec. A materially incompatible naming change is a major design decision.
4. Classify by ownership and boundary semantics, not by the fact that two classes happen to contain the same fields.
5. Keep DAO/Repository/Gateway/Mapper types out of the data-object inventory; they are behavior-bearing access components.

## Class-necessity test

Do not create `FooPO`, `FooDO`, `FooEntity`, `FooBO`, `FooDTO`, `FooVO`, `FooRequest`, and `FooResponse` by default.

Create a distinct class only when at least one concrete difference requires it:

- ownership or dependency direction differs;
- public compatibility or serialization shape differs;
- validation, authorization, privacy, or field exposure differs;
- mutability, lifecycle, identity, invariants, or state transitions differ;
- persistence mapping, lazy loading, generated fields, or database null semantics differ;
- one boundary needs aggregation, projection, denormalization, localization, or pagination not owned by another model;
- independent versioning or change cadence prevents safe reuse.

Reuse an existing class when semantics, lifecycle, validation, exposure, and dependency direction are genuinely the same. Record the reason reuse is safe.

Do not reuse a PO or ORM Entity as a public Request, Response, DTO, or View Object merely to reduce the class count. Persistence annotations, internal fields, lazy relationships, and schema evolution must not leak across external boundaries.

When two distinct classes are necessary, define the exact conversion owner and field mapping. Avoid chains of no-op mappers and intermediate objects that add no semantic boundary.

## Required Spec evidence

For every proposed object, record:

- exact class name, package/path, and selected role;
- owner and lifecycle;
- consumers and boundary crossings;
- fields, validation, null/default semantics, sensitive-data handling, and invariants;
- persistence or protocol mapping when applicable;
- why a separate class is necessary, or which existing class is safely reused;
- conversion owner when mapping is necessary;
- requirement IDs.

The Spec must include an object-flow diagram or mapping table when data crosses three or more object roles.

## Entity inheritance

Entity inheritance is allowed, not required. Use it only when repository conventions and semantics support either a true substitutable subtype or a stable common entity base.

A common entity base may centralize identity, audit timestamps, tenant ownership, optimistic versioning, or other lifecycle mechanics already shared by repository entities. The Spec must address:

- the `is-a` or common-lifecycle justification;
- inherited fields and invariants;
- ORM mapping strategy, proxies, lazy loading, discriminator/table rules, and migration impact;
- identity and `equals`/`hashCode` behavior;
- serialization and external-contract exposure;
- test implications and compatibility.

Reject inheritance used only to avoid repeating unrelated fields or helper methods. Prefer embedded value objects or composed collaborators when there is no substitutable entity relationship.

## Business-service composition

Design concrete application, domain, and business services with composition and delegation as the default. Inject or assemble repositories, policies, strategies, validators, calculators, gateways, and domain collaborators explicitly.

Do not create a business `BaseService`, multi-level service hierarchy, or subclass-based feature variation merely for code reuse. Such inheritance couples lifecycle, hidden state, protected hooks, and unrelated behavior.

Allow service inheritance only when an existing framework requires a stable extension contract or the repository already has a justified Template Method with a real invariant algorithm and narrow variation hooks. Record why composition cannot express the requirement more clearly and how substitutability, testability, and lifecycle safety are preserved.
