# Java, Spring, and Egon-COLA Planning Standards

Read `references/user-mandated-java-rules.md` first, then this reference completely for every Java Plan. The literal rules are absolute; this reference adds repository planning evidence and cannot weaken them. It does not authorize redesign or unrelated refactoring.

## Rule precedence and touched-code scope

Apply, in order: the user's latest explicit requirements and applicable `AGENTS.md`; the current project's explicit conventions and selected Egon-COLA Archetype; the effective Specs; this reference; nearby legacy style only where compatible.

Every new file and every materially changed symbol must comply as far as the approved scope permits. Existing violations outside the write scope are not cleanup work. If compliance requires a new public contract, module move, dependency, migration, or broad refactor absent from the Spec, record a blocker and return to the user/Spec. Never silently waive a rule.

## Mandatory pre-plan discovery

Before assigning files or Steps:

1. identify repository root, affected modules, build manifests, Java/Spring Boot versions, dependency management, `lombok.config`, generated sources, environment profiles, and test commands;
2. classify the actual tree as exactly one allowed architecture profile;
3. search the affected module and Egon-COLA for existing Entity/PO, BO/DTO/VO, Query/Command/Event, Converter, Validator, Component, Repository/DAO, Gateway, Service/Domain Service, Exception, Result, Enum, utility, Starter, and infrastructure capabilities;
4. inspect current Spring/Spring Boot capabilities and resolved dependencies;
5. create a reuse ledger with candidate, exact path/dependency, capability, fit/gap, decision, and owning Step;
6. prove the need, version ownership, maintenance/security/operational impact, and approval for every additional dependency or custom replacement.

Use this preference order: existing JDK/Spring/Spring Boot capability; existing Spring Boot Starter; existing Egon-COLA Component/common infrastructure; module-local abstraction; only then an approved mature dependency or custom implementation. Do not reproduce Spring or Egon-COLA capability.

Reverify repository evidence rather than assuming availability. Current Egon-COLA candidates include `egon-cola-component-common-core` `BaseConverter<S,T>` and `ValidationUtils`, MapStructPlus integration, `spring-boot-starter-validation`, archetype `lombok.config` Qualifier propagation, and the `egon-cola-archetype-{light,service,web}` families and open variants.

## Allowed architecture profiles

Select one, and only one, from current repository evidence:

- **Traditional layered**: preserve `biz.controller`, `biz.service`, nested `biz.service.impl`, `biz.dao`, `biz.config`, `biz.utils`, and semantically grouped `biz.domain`; preserve Controller -> Service interface -> `service.impl` -> DAO direction.
- **Exact Egon-COLA Archetype**: follow the selected archetype's generated tree, verifier, and dependencies. Light uses its single-project responsibilities. Service/Web use the archetype-defined `common`, `facade`, `domain`, `application`, `infrastructure`, `adapter`, and `starter` modules. Open variants follow their own contract.

Do not invent a third architecture, generic DDD interpretation, hybrid `biz.*`/COLA structure, renamed module, or extra layer. If the current tree matches neither profile or selection evidence conflicts, planning is blocked pending a user decision.

## Per-file planning rules

Every affected Java file block must name its applicable `MC-*` IDs in `Standards impact`, its original `Rule N` values in `Literal rule enforcement`, show repository evidence, and make these decisions explicit in pseudocode and validation:

### Semantic naming

- persistence carrier `*PO`; real lifecycle/identity entity `*Entity` only with established semantics;
- business carrier `*BO`, transport `*DTO`, presentation `*VO`;
- read intent `*Query`, write intent `*Command`, published fact `*Event`;
- protocol-specific `*Request`/`*Response`, and established `*PageQuery`/`*PageResult` where applicable;
- access component `*DAO`; other behavior types use their exact role suffix.

Do not introduce ambiguous carrier suffixes `Data`, `Info`, `Param`, or `Bean`. Do not create a type per layer unless a real boundary or lifecycle difference requires it.

### Cross-layer validation and normalization

Every affected layer-to-layer input handoff must use Jakarta Bean Validation from `spring-boot-starter-validation`; Controller-only validation is insufficient. Plan each external -> Controller/Adapter, Controller/Adapter -> Service/Application, Service/Application -> Domain Service/Component, Service/Application -> DAO/Repository/Gateway, and event/job/internal re-entry handoff separately. Name `@Valid`, `@Validated`, the exact group, `ValidationUtils` invocation, normalization, errors, and tests. A custom `ConstraintValidator` is prohibited unless the effective Spec approves the proven annotations/groups/library gap.

For telephone semantics, plan one named normalization boundary and a mature solution such as libphonenumber, including region, canonical form, validity, error mapping, and tests. If the dependency is absent, the Spec/dependency gate must approve it before the Plan can PASS.

### Object modeling and conversion

- simple immutable carrier: prefer `record`; use a compact constructor for deterministic normalization/invariants; a method-local temporary structure may be a local `record`;
- immutable non-record: prefer Lombok `@Value`;
- complex object: normal class with the complete mandated baseline `@Data`, protected `@NoArgsConstructor`, `@AllArgsConstructor`, `@RequiredArgsConstructor`, `@Builder`, and `@Accessors(chain = true)`;
- calculate all generated constructor signatures; any duplicate/framework conflict blocks the Plan and cannot be silently solved by removing an annotation.

Use MapStruct or MapStructPlus for cross-layer conversion. Every new affected Converter must implement/extend the Egon `BaseConverter<S,T>` system. Plan exact generic types, mapping, null/default/enum/time/sensitive-field rules, generated implementation, Bean name, and tests. Prohibit manual `set/get`, `BeanUtils.copyProperties`, reflection, JSON round trips, and one-way/local converter bypasses. If `BaseConverter` cannot express the mapping, block and return to the Spec/user.

### Spring Beans, injection, and logging

- every affected concrete business class uses `@Slf4j` and safe parameterized logging;
- every Spring-managed Bean has an explicit stable name in its stereotype or `@Bean` method;
- injection uses final fields plus Lombok `@RequiredArgsConstructor`; no field `@Autowired` and no handwritten business injection constructor;
- every injected dependency is annotated with `@Qualifier("stableBeanName")`;
- verify `lombok.config` copies `Qualifier` to generated constructor parameters, and plan that configuration change when absent and in scope.

### Utilities, JSON, time, and configuration

- utilities: JDK first, then already managed Apache Commons (`commons-lang3`, `commons-collections4`, `commons-io`, `commons-text`, `commons-codec`, `commons-beanutils`) or Guava; Tika only for real content identification; no duplicate helper or extra utility library;
- `commons-beanutils` is not permitted for business conversion;
- JSON: Spring Boot Jackson only; external DTO/VO/Request/Response/Command/Query/Event uses only protocol-required Jackson annotations; no Gson/Fastjson or JSON mapping trick;
- time: `java.time` only for new/touched modeling, with time zone, precision, Clock, persistence, and JSON semantics; no new `Date`, `Calendar`, or `SimpleDateFormat`;
- configuration: prefer validated `@ConfigurationProperties`; every added/changed key must be represented with the same structure/core key set in all environment profiles, while values may differ.

### Business modeling and patterns

Classify affected business flows. For every Complex flow, plan an actual repository-consistent Strategy, Template Method, Factory, Chain of Responsibility, State, Specification, Domain Event, or other appropriate pattern. Name participants, files, dependency order, registration/selection, orchestration, failures, and tests. Complex logic cannot remain direct `if/else`, `switch`, type/string/reflection dispatch. Only Simple logic may stay direct; do not create ceremonial patterns for it.

## Step Manual Check contract

Each Step lists `Manual Checks: <IDs>` and every file lists `Standards impact: <IDs and exact consequence>`. Before proposing the Step commit, the Plan must define evidence that will make every applicable Step check PASS. No Step may defer a known violation to an unspecified later cleanup.

The final Plan repeats every ID exactly once in the blocking table:

| Check ID | Blocking subject |
| --- | --- |
| `MC-ARCH-001` | Exactly one allowed architecture profile is identified and preserved |
| `MC-REUSE-001` | Spring, Starter, Egon-COLA, and module-local reuse candidates were inspected first |
| `MC-DEP-001` | Added dependency/custom code has a proven gap and approved impact, or none is added |
| `MC-NAME-001` | New/changed Java names use semantic roles and avoid ambiguous carrier suffixes |
| `MC-VALID-001` | Cross-layer inputs use Jakarta/Spring Validation, groups, approved normalization, and tests |
| `MC-MODEL-001` | Record/`@Value`/complete complex-class Lombok baseline is planned exactly, or constructor/framework conflict blocks |
| `MC-CONVERT-001` | MapStruct/MapStructPlus and mandatory Egon `BaseConverter` own every new affected Converter |
| `MC-LOG-001` | Affected concrete business classes use `@Slf4j` and safe logging |
| `MC-BEAN-001` | Affected Beans have stable names, Lombok constructor injection, Qualifiers, and propagation |
| `MC-UTIL-001` | Only approved utilities are used and no duplicate helper is introduced |
| `MC-JSON-001` | Jackson is the sole JSON stack and annotations match the protocol |
| `MC-TIME-001` | New/touched time modeling uses `java.time` with explicit boundary semantics |
| `MC-CONFIG-001` | All environment profiles retain equivalent configuration key structure |
| `MC-PATTERN-001` | Every Complex business flow has a concrete pattern; only Simple logic remains direct |
| `MC-SCOPE-001` | Touched code complies without unrelated broad refactoring; exceptions are explicit |
| `MC-TEST-001` | Planned focused tests/static gates prove every applicable standard |
| `MC-BLOCKER-001` | All blockers/check failures are closed; no unknown or silent exception remains |

Use this exact table shape:

| Check ID | Applicability | Status | Evidence | Finding | Required action/exception |
| --- | --- | --- | --- | --- | --- |
| `MC-ARCH-001` | Applicable / Not applicable | PASS / N/A / FAIL / BLOCKED | `<paths, symbols, dependency or planned command evidence>` | `<observed conclusion>` | `None / exact action and owner` |

An overall PASS requires every applicable row to be `PASS` and every inapplicable row to be evidence-backed `N/A`. Any missing ID/evidence, `FAIL`, `BLOCKED`, `UNKNOWN`, or unresolved exception prohibits PASS.
