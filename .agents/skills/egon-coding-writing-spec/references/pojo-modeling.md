# Three-Layer POJO Role and Object-Model Design

Read this reference before writing Spec Chapter 10 and the service-structure part of Chapter 13. It applies to the traditional three-layer profile defined by `three-layer-architecture.md`. Treat the names as semantic roles, not as a checklist that requires one class per row.

## Canonical role vocabulary

| Term | Full name | Design meaning | Typical use |
| --- | --- | --- | --- |
| POJO | Plain Old Java Object | Umbrella term for ordinary Java objects; not a layer or required suffix | General object category |
| PO | Persistent Object | Persistence representation aligned with a table, row, or stored record | DAO/mapper persistence boundary |
| DO | Data Object / Domain Object | Ambiguous team-specific term | Use only when the repository defines which meaning applies |
| DTO | Data Transfer Object | Data transferred between layers, modules, processes, or services | Boundary transport without persistence ownership |
| VO | View Object | Data shaped for frontend or presentation | API/page display output |
| BO | Business Object | Object used for internal service calculation or orchestration | Intermediate business computation when it has distinct semantics |
| Entity | Entity | Identity-bearing ORM persistence object in this profile | Persistent state and lifecycle with stable identity; state its exact ORM meaning |
| DAO | Data Access Object | Database-access component, not a data carrier | Persistence access operations |
| Query / QO | Query Object | Read-condition carrier | Search and filtering inputs |
| Command / CO | Command Object | Mutation-intent carrier | Create/update/delete or other state-changing use cases |
| Request | Request Object | Controller or API input | Transport validation and request compatibility boundary |
| Response | Response Object | Controller or API output | Stable transport response boundary |
| Form | Form Object | Form-submission input | UI form binding when distinct from the API request |
| Param | Parameter Object | Grouped method or API parameters | Avoid long parameter lists when the group has one coherent meaning |
| PageQuery | Page Query Object | Pagination plus query conditions | Paged read input |
| PageResult | Page Result Object | Paged items plus pagination metadata | Paged read output |

This profile does not define Aggregate, Domain Service, Repository Port, or DDD Value Object roles. Do not introduce them merely to make a traditional three-layer design look domain-driven.

## Repository-first classification and placement

1. Inspect existing suffixes, `biz.domain` packages, framework annotations, serializers, mappers, persistence types, and public contracts before proposing a name.
2. Preserve a consistent repository definition unless it violates an explicit user decision or creates a documented correctness problem.
3. When `DO`, `VO`, or `Entity` is ambiguous, state the selected meaning in the Spec. A materially incompatible naming change is a major design decision.
4. Classify by ownership and boundary semantics, not by the fact that two classes happen to contain the same fields.
5. Place necessary data carriers directly in `biz.domain` or in a repository-consistent child package; do not create every possible child package.
6. Keep DAO/Mapper types out of the POJO inventory. They are behavior-bearing access components under `biz.dao`.
7. Keep concrete Service implementations under `biz.service.impl`; data-object classification must not create another service layer.

## Class-necessity test

Do not create `FooPO`, `FooDO`, `FooEntity`, `FooBO`, `FooDTO`, `FooVO`, `FooRequest`, and `FooResponse` by default.

Create a distinct class only when at least one concrete difference requires it:

- ownership or dependency direction differs;
- public compatibility or serialization shape differs;
- validation, authorization, privacy, or field exposure differs;
- mutability, lifecycle, identity, validation rules, or state transitions differ;
- persistence mapping, lazy loading, generated fields, or database null semantics differ;
- one boundary needs aggregation, projection, denormalization, localization, or pagination not owned by another model;
- independent versioning or change cadence prevents safe reuse.

Reuse an existing class when semantics, lifecycle, validation, exposure, and dependency direction are genuinely the same. Record why reuse is safe.

Do not reuse a PO or ORM Entity as a public Request, Response, DTO, or View Object merely to reduce the class count. Persistence annotations, internal fields, lazy relationships, and schema evolution must not leak across external boundaries.

When two distinct classes are necessary, define the exact conversion owner and field mapping. Avoid chains of no-op mappers and intermediate objects that add no semantic boundary.

## Required Spec evidence

For every proposed object, record:

- exact class name, package/path, and selected role;
- owner and lifecycle;
- consumers and boundary crossings;
- fields, validation, null/default semantics, sensitive-data handling, and state rules;
- persistence or protocol mapping when applicable;
- why a separate class is necessary, or which existing class is safely reused;
- conversion owner when mapping is necessary;
- requirement IDs.

The Spec must include an object-flow diagram or mapping table when data crosses three or more object roles.

## PO and ORM Entity inheritance

PO or ORM Entity inheritance is allowed, not required. Use it only when repository conventions and semantics support either a true substitutable persistence subtype or a stable common persistence base.

A common persistence base may centralize identity, audit timestamps, tenant ownership, optimistic versioning, or other lifecycle mechanics already shared by persistence objects. The Spec must address:

- the `is-a` or common-lifecycle justification;
- inherited fields and validation/state rules;
- ORM mapping strategy, proxies, lazy loading, discriminator/table rules, and migration impact;
- identity and `equals`/`hashCode` behavior;
- serialization and external-contract exposure;
- test implications and compatibility.

Reject inheritance used only to avoid repeating unrelated fields or helper methods. Prefer composition when there is no substitutable persistence relationship.

## Business-service composition

Concrete classes in `biz.service.impl` use composition and delegation by default. Inject or assemble DAOs, policies, strategies, validators, calculators, gateways, and other cohesive collaborators explicitly.

Do not create a business `BaseService`, multi-level service hierarchy, or subclass-based feature variation merely for code reuse. Such inheritance couples lifecycle, hidden state, protected hooks, and unrelated behavior.

Allow service inheritance only when an existing framework requires a stable extension contract or the repository already has a justified Template Method with a real invariant algorithm and narrow variation hooks. Record why composition cannot express the requirement more clearly and how substitutability, testability, and lifecycle safety are preserved.
