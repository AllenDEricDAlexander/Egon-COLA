# Java, Spring, and Egon-COLA Coding Standards

Read this reference for every Java coding Spec. These are blocking design constraints for newly added or modified code, not permission to refactor unrelated legacy code.

## Contents

- [Precedence and touched-code boundary](#precedence-and-touched-code-boundary)
- [Repository and capability discovery](#repository-and-capability-discovery)
- [Allowed architecture profiles](#allowed-architecture-profiles)
- [Semantic Java naming](#semantic-java-naming)
- [Cross-layer validation](#cross-layer-validation)
- [Data-object modeling](#data-object-modeling)
- [Object conversion](#object-conversion)
- [Spring beans, injection, and logging](#spring-beans-injection-and-logging)
- [Utility, JSON, time, and configuration rules](#utility-json-time-and-configuration-rules)
- [Business modeling and design patterns](#business-modeling-and-design-patterns)
- [Blocking Manual Check catalog](#blocking-manual-check-catalog)

## Precedence and touched-code boundary

Apply rules in this order:

1. the user's latest explicit instruction and applicable `AGENTS.md` rules;
2. the current project's explicit conventions and its selected Egon-COLA Archetype contract;
3. this reference;
4. nearby legacy style only where it does not conflict with the preceding rules.

Do not launch an unrelated cleanup when existing code violates a rule. Every new file and every symbol materially modified by the current task must comply as far as the approved scope permits. When compliance requires an undeclared public contract, module move, dependency, migration, or broad refactor, record the conflict as a blocker and ask for approval instead of silently bypassing the rule.

## Repository and capability discovery

Before selecting an implementation:

1. identify the repository root, affected module, build manifests, Java/Spring Boot versions, dependency management, `lombok.config`, configuration profiles, tests, and generated-source rules;
2. classify the code structure using the allowed profiles below;
3. search the affected module and Egon-COLA for existing Entity/PO, DTO/VO/BO, Query/Command/Event, Converter, Validator, Component, Repository/DAO, Gateway, Service/Domain Service, Exception, Result, Enum, utilities, starters, and infrastructure;
4. inspect Spring/Spring Boot capabilities already available in the resolved project version;
5. build a reuse ledger naming each candidate, exact path or dependency, capability, fit/gap, and final reuse decision;
6. inspect all current and proposed dependencies and identify their owning starter/component.

Use this preference order:

1. JDK and the Spring/Spring Boot ecosystem already present in the project;
2. an existing Spring Boot Starter;
3. an existing Egon-COLA Component or common infrastructure;
4. an existing module-local abstraction;
5. an additional mature dependency or new implementation only after evidence proves the preceding options insufficient.

An additional dependency or duplicate abstraction is a blocking design decision until the Spec states the exact missing capability, candidates inspected, why each is insufficient, dependency/version/maintenance/security/operational impact, and user approval when the impact is material. Do not reproduce Spring or Egon-COLA functionality locally.

Repository evidence currently includes:

- `egon-cola-component-common-core` `BaseConverter<S,T>` for shared converter contracts;
- `egon-cola-component-common-core` `ValidationUtils` for Jakarta Validation and groups;
- MapStructPlus dependencies and processors in Egon-COLA archetypes/common core;
- `spring-boot-starter-validation` in the generated boundary/application modules;
- `lombok.copyableAnnotations += org.springframework.beans.factory.annotation.Qualifier` in archetype `lombok.config` files;
- the `egon-cola-archetypes/egon-cola-archetype-{light,service,web}` families and their selected open variants.

Reverify these paths in the current baseline; do not assume a component is available merely because this reference names it.

## Allowed architecture profiles

Before coding, select exactly one profile from repository evidence:

### Traditional layered profile

Use the repository's established structure with `biz.controller`, `biz.service`, nested `biz.service.impl`, `biz.dao`, `biz.config`, `biz.utils`, and semantically organized `biz.domain`. Preserve Controller -> Service interface -> `service.impl` -> DAO direction. Do not add DDD/COLA modules inside this profile.

### Egon-COLA Archetype profile

Use the exact selected `egon-cola-archetypes` template and generated verifier. Light uses the archetype's single-project package responsibilities. Service/Web use the archetype-defined `common`, `facade`, `domain`, `application`, `infrastructure`, `adapter`, and `starter` modules and dependency direction. Open variants follow their own generated contract. Do not invent a generic DDD/COLA interpretation, hybridize it with `biz.*`, rename modules, or add a layer absent from the selected archetype.

If the current code matches neither profile, or evidence conflicts about the selected archetype, stop before target design. Present the observed tree, allowed options, migration impact, and recommendation to the user. Do not normalize the repository silently.

## Semantic Java naming

Name every type by its actual role.

- Persistence carrier: `*PO`; use `*Entity` only for a real ORM/DDD entity with explicit identity and lifecycle semantics.
- Business calculation carrier: `*BO`.
- Transport/integration carrier: `*DTO`.
- Presentation/output carrier: `*VO`.
- Read intent: `*Query`; write intent: `*Command`; published fact: `*Event`.
- HTTP/RPC-specific boundaries may use explicit `*Request` and `*Response`; pagination may use `*PageQuery` and `*PageResult` when those are the repository contracts.
- Data access component: `*DAO`; it is an access component, not a POJO.
- Other behavior types use exact role suffixes such as `*Controller`, `*Service`, `*Repository`, `*Gateway`, `*Converter`, `*Validator`, `*Component`, `*Factory`, `*Strategy`, `*Exception`, `*Result`, or `*Enum`.

Do not use ambiguous carrier suffixes such as `Data`, `Info`, `Param`, or `Bean` to avoid choosing a semantic role. Do not create duplicate PO/BO/DTO/VO/Query/Command/Event types when no real boundary difference exists. Existing ambiguous names outside the touched scope are not a refactoring mandate; new and materially changed types must be corrected or the incompatibility reported.

## Cross-layer validation

Every affected external or cross-layer input boundary must use Jakarta Bean Validation supplied by `spring-boot-starter-validation`.

- Put constraints on the consumed Request/DTO/Command/Query/Event or method parameters and use `@Valid` for cascaded objects.
- Enable method/boundary validation with Spring `@Validated` where required.
- Reusable input types must use explicit Validation Groups for different operations rather than weakening constraints or duplicating near-identical classes.
- Prefer Jakarta's native constraints. Use the existing Egon-COLA `ValidationUtils` for explicit/manual validation and group invocation.
- Normalize standardized data at one named boundary. Telephone numbers use a mature standard such as libphonenumber for parsing, region handling, normalization, and validity when telephone semantics are affected.
- Create a custom `ConstraintValidator` only when native annotations, composition, groups, `ValidationUtils`, and an approved mature library cannot express the rule. Document the gap and tests.
- Define validation order, normalization-before/after behavior, error mapping, null/blank semantics, group selection, and boundary tests.

Do not handwrite repeated null/range/format validators or validate only at the Controller while trusting a second cross-layer construction path.

## Data-object modeling

Choose the representation from semantics, not a blanket annotation list.

- Prefer a Java `record` for a simple immutable carrier. Use a compact constructor for deterministic normalization and invariant checks when appropriate. A one-method temporary structure may be a local `record`.
- Prefer `record` or Lombok `@Value` for immutable objects; choose one coherent model.
- Use a normal Java class for complex mutable, ORM/proxy, framework-instantiated, or lifecycle-rich objects.
- For mutable classes, select only the Lombok annotations required by construction and framework semantics: `@Data`, `@NoArgsConstructor(access = AccessLevel.PROTECTED)`, `@AllArgsConstructor`, `@RequiredArgsConstructor`, `@Builder`, and `@Accessors(chain = true)` are an allowed palette, not a mandatory stack.
- Explain constructor visibility, required fields, builder/default behavior, mutation, equality/hash, serialization, ORM/proxy requirements, and validation.
- Reject conflicting or redundant constructor annotations and do not combine mutable `@Data` semantics with immutable `@Value` semantics.

## Object conversion

Use MapStruct or MapStructPlus for cross-layer object conversion. When the affected module can depend on `egon-cola-component-common-core`, the converter must extend or implement the existing `BaseConverter<S,T>` contract where its two-way semantics fit. Cite the exact converter path and generated implementation/bean name.

Keep normalization, derived fields, enum/time conversion, sensitive-field exclusion, defaults, and null semantics in named converter methods, MapStruct mappings, or qualified helpers. Do not scatter manual `set/get` mapping through business services. Do not use `BeanUtils.copyProperties`, reflection copying, or JSON serialization as an object mapper. The presence of `commons-beanutils` in the allowed utility set does not permit it for business object conversion.

If conversion is intentionally one-way or `BaseConverter` is semantically incompatible, document the reason and use the closest established Egon-COLA converter pattern; do not force a false reverse mapping.

## Spring beans, injection, and logging

- Every concrete business behavior class must use Lombok `@Slf4j`; do not handwrite a logger field.
- Every Spring-managed Bean must have a stable explicit name through its stereotype value or `@Bean` method, for example `@Service("userService")`, `@Component("orderComponent")`, `@Repository("userRepository")`, or `@RestController("userController")`.
- Use constructor injection with final fields and Lombok `@RequiredArgsConstructor`. Do not use field `@Autowired` and do not handwrite an injection constructor in business code.
- Annotate every injected dependency field with `@Qualifier("stableBeanName")`. Verify `lombok.config` copies `Qualifier` to generated constructor parameters. If it does not, adding the correct copyable-annotation configuration or resolving the injection contract is part of the affected design; do not assume field annotations are sufficient.
- A collection/map of implementations also requires a stable qualified aggregate Bean or an explicit approved registry.
- Logs must be actionable, parameterized, and free of secrets/sensitive payloads. Do not add a logger merely to emit noisy enter/exit messages.

## Utility, JSON, time, and configuration rules

### Utilities

Use JDK APIs first, then the already managed Apache Commons libraries (`commons-lang3`, `commons-collections4`, `commons-io`, `commons-text`, `commons-codec`, `commons-beanutils`) or Guava. Apache Tika is allowed only for a real document/content detection requirement. Do not add another utility library or a duplicate `*Utils` class for a few simple methods. `commons-beanutils` remains prohibited for business object mapping.

### JSON

Use Spring Boot's Jackson stack. Apply Jackson annotations to external DTO/VO/Request/Response/Command/Query/Event types only as required by the actual protocol: field names, inclusion/ignore rules, enum values, date/time format, polymorphism, compatibility, and unknown-field behavior. Do not introduce or mix Gson, Fastjson, or JSON round-trip conversion.

### Date and time

New or materially changed code uses `java.time` types such as `LocalDate`, `LocalDateTime`, `Instant`, `OffsetDateTime`, `ZonedDateTime`, and `Duration`. Do not introduce `java.util.Date`, `Calendar`, or `SimpleDateFormat`. Define time zone, precision, clock source, persistence type, and JSON representation at boundaries.

### Configuration

Prefer typed `@ConfigurationProperties` over scattered `@Value`. When adding or changing a configuration key, compare every Spring Boot environment profile and keep the same key structure/core key set; values may differ. Verify defaults, validation, metadata, tests, secret handling, and backward compatibility. An environment-specific missing or extra core key is a blocker.

## Business modeling and design patterns

Do not accumulate complex business behavior in long `if/else`, `switch`, type checks, or hard-coded orchestration. Identify real variation dimensions, state transitions, rule combinations, algorithm choices, responsibility chains, object creation, and event collaboration.

Use Strategy, Template Method, Factory, Chain of Responsibility, State, Specification, Domain Event, or another repository-supported pattern only when it isolates a present variation or responsibility and improves testing/evolution. Name the variation point, selected pattern, participants, dependency direction, extension mechanism, and tests. For simple direct logic, explicitly reject unnecessary patterns. “Complex” is not evidence that more classes are needed, and “use a pattern” is not permission to overdesign.

## Blocking Manual Check catalog

Every Spec, Plan, execution Step, and final execution audit must use these stable IDs. All applicable rows require `PASS` with concrete evidence. An inapplicable row requires `N/A` with evidence and reason. `FAIL`, `BLOCKED`, `UNKNOWN`, missing evidence, or a missing ID blocks an overall PASS.

| Check ID | Blocking subject |
| --- | --- |
| `MC-ARCH-001` | Exactly one allowed architecture profile is identified and preserved |
| `MC-REUSE-001` | Spring, Spring Boot Starter, Egon-COLA Component/common infrastructure, and module-local reuse candidates were inspected before new design |
| `MC-DEP-001` | Every added dependency or custom replacement has a proven capability gap and approved impact; otherwise none is added |
| `MC-NAME-001` | New/changed Java type names use explicit semantic role suffixes and avoid `Data`/`Info`/`Param`/`Bean` ambiguity |
| `MC-VALID-001` | Every affected cross-layer input boundary uses Jakarta/Spring Validation, groups where reused, approved normalization, and tests |
| `MC-MODEL-001` | Record/class/Lombok choice matches mutability, construction, framework, and invariant semantics without annotation conflicts |
| `MC-CONVERT-001` | MapStruct/MapStructPlus and the applicable Egon `BaseConverter` contract own object mapping; prohibited copying is absent |
| `MC-LOG-001` | Every affected concrete business class uses `@Slf4j` and safe actionable logging |
| `MC-BEAN-001` | Every affected Spring Bean has a stable explicit name, constructor injection, `@RequiredArgsConstructor`, and qualified dependencies with Lombok propagation verified |
| `MC-UTIL-001` | Utilities use only the approved JDK/Commons/Guava/Tika policy and do not duplicate existing helpers |
| `MC-JSON-001` | Jackson is the only JSON stack and external contracts carry only necessary Jackson annotations |
| `MC-TIME-001` | New/changed time modeling uses `java.time` with explicit boundary semantics |
| `MC-CONFIG-001` | All environment configuration files retain equivalent key structure and use typed properties where applicable |
| `MC-PATTERN-001` | Complex variation is modeled with a justified pattern, or direct logic is explicitly justified without hard coding/overdesign |
| `MC-SCOPE-001` | Touched code complies without unrelated broad refactoring; any unavoidable exception is explicit and approved |
| `MC-TEST-001` | Focused tests/static gates prove the applicable standards and preserved behavior |
| `MC-BLOCKER-001` | Every blocker and manual-check failure is closed; no `UNKNOWN`, silent exception, or missing evidence remains |

Use this table shape in generated artifacts:

| Check ID | Applicability | Status | Evidence | Finding | Required action/exception |
| --- | --- | --- | --- | --- | --- |
| `MC-ARCH-001` | Applicable / Not applicable | PASS / N/A / FAIL / BLOCKED | `<paths, symbols, dependency or command evidence>` | `<observed result>` | `None / exact next action and owner` |
