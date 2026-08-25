# Java, Spring, and Egon-COLA Execution Standards

Read `references/user-mandated-java-rules.md` first, then this reference before the first Java Step, at every Step gate, and during the final audit. The literal rules are absolute; this reference adds execution evidence and cannot weaken them. These rules do not authorize unrelated cleanup.

## Precedence and stop boundary

Apply, in order: the user's latest explicit requirements and applicable `AGENTS.md`; the current project's explicit conventions and selected Egon-COLA Archetype; the effective Specs and approved Plan; this reference; compatible nearby style.

If compliance requires an undeclared contract, module move, dependency, migration, file, or broad refactor, stop the Step as Plan/Spec drift. Never silently waive the rule, invent a directory, or commit a known violation for later cleanup.

## Pre-code architecture and reuse check

Before editing the first Step, and again when a Step touches a new module:

1. inspect the actual tree, build manifests, Java/Spring Boot versions, dependency management, `lombok.config`, generated sources, tests, and all environment profiles;
2. identify exactly one allowed architecture from evidence:
   - traditional `biz.controller`, `biz.service`, nested `biz.service.impl`, `biz.dao`, `biz.config`, `biz.utils`, `biz.domain`; or
   - the exact selected `egon-cola-archetype-{light,service,web}` or open-variant tree, verifier, and dependency direction;
3. search the module and Egon-COLA for existing Entity/PO, BO/DTO/VO, Query/Command/Event, Converter, Validator, Component, Repository/DAO, Gateway, Service/Domain Service, Exception, Result, Enum, utility, Starter, and infrastructure capabilities;
4. verify the Plan's reuse ledger against current paths and resolved dependencies;
5. use JDK/Spring/Spring Boot first, then an existing Starter, Egon-COLA Component/common infrastructure, or module-local abstraction;
6. permit an additional mature dependency or custom implementation only when the effective Spec/Plan records a current capability gap, alternatives, version ownership, maintenance/security/operational impact, and required approval.

Do not hybridize architectures or duplicate existing capability. Repository candidates must be reverified and may include common-core `BaseConverter<S,T>` and `ValidationUtils`, MapStructPlus, starter validation, and archetype Qualifier propagation.

## Touched-code execution rules

### Semantic Java names

New/materially changed carriers use explicit roles: `*PO`, `*BO`, `*DTO`, `*VO`, `*Query`, `*Command`, `*Event`, protocol-specific `*Request`/`*Response`, and established `*PageQuery`/`*PageResult`. `*Entity` requires real identity/lifecycle semantics. `*DAO` is an access component. Behavior types use exact role suffixes.

Do not add ambiguous `Data`, `Info`, `Param`, or `Bean` carrier names. Do not create one object per layer without an actual boundary/lifecycle distinction.

### Validation and normalization

Every affected layer-to-layer input handoff uses Jakarta Bean Validation from `spring-boot-starter-validation`; Controller-only validation fails. Inspect external -> Controller/Adapter, Controller/Adapter -> Service/Application, Service/Application -> Domain Service/Component, Service/Application -> DAO/Repository/Gateway, and Event/Job/internal re-entry separately, including `@Valid`, `@Validated`, exact groups, `ValidationUtils`, errors, and tests.

Telephone semantics use one named boundary and a mature standard such as libphonenumber for region-aware parsing, normalization, and validation. Do not write a duplicate `ConstraintValidator` unless approved evidence proves annotations, composition, groups, `ValidationUtils`, and mature libraries insufficient.

Verify constraint selection, group invocation, normalization order, null/blank behavior, error mapping, and focused negative tests.

### Object model and conversion

- simple immutable carrier: prefer `record`; compact constructors may normalize or enforce deterministic invariants; method-local temporary structures may use a local `record`;
- immutable non-record: prefer Lombok `@Value`;
- complex object: normal class with the complete mandated `@Data`, protected `@NoArgsConstructor`, `@AllArgsConstructor`, `@RequiredArgsConstructor`, `@Builder`, and `@Accessors(chain = true)` baseline;
- compile the complete baseline; constructor/framework conflicts block and cannot be silently solved by deleting annotations.

Use MapStruct/MapStructPlus for cross-layer conversion and require every new affected Converter to implement/inherit the Egon `BaseConverter<S,T>` system. Verify exact generics, null/default/enum/time/sensitive-field mappings, generated implementation, Bean wiring, and tests. Reject setter/getter mapping, `BeanUtils.copyProperties`, reflection, JSON round trips, and one-way/local converter bypasses. A contract mismatch blocks.

### Spring Bean, logging, and injection

- concrete business behavior classes use Lombok `@Slf4j`; logging is parameterized, actionable, and secret-safe;
- every Spring-managed Bean has a stable explicit stereotype/`@Bean` name;
- use final fields and Lombok `@RequiredArgsConstructor`; no field `@Autowired` and no handwritten business injection constructor;
- every injected dependency field has `@Qualifier("stableBeanName")`;
- inspect `lombok.config` to ensure Qualifier is copied to generated constructor parameters; do not assume field annotations are sufficient.

### Utilities, JSON, time, and configuration

- utility order: JDK, already-managed Apache Commons (`commons-lang3`, `commons-collections4`, `commons-io`, `commons-text`, `commons-codec`, `commons-beanutils`), then Guava; Tika only for real content identification; no duplicate utility class or casual library;
- Commons BeanUtils remains prohibited for business conversion;
- JSON uses Spring Boot Jackson only; apply protocol-required Jackson annotations to external DTO/VO/Request/Response/Command/Query/Event; do not mix Gson/Fastjson or map by serialization;
- new/touched time code uses `java.time` and explicit zone/precision/Clock/persistence/JSON semantics; no new `Date`, `Calendar`, or `SimpleDateFormat`;
- prefer validated `@ConfigurationProperties`; whenever a key changes, compare every environment profile and keep the same structure/core key set while allowing different values.

### Complex business behavior

Classify each touched business flow. Every Complex flow must implement the Spec/Plan-selected Strategy, Template Method, Factory, Chain of Responsibility, State, Specification, Domain Event, or other established pattern with actual participants, selection/wiring, orchestration, failure handling, and tests. Reject direct `if/else`, `switch`, type/string/reflection dispatch for Complex logic. Simple flow remains direct; reject ceremonial pattern classes there.

## Per-Step blocking Manual Check

At Step lock, copy the Plan's applicable IDs into a Step Manual Check table. Add an ID when current evidence shows the Step affects that concern; omission is not a waiver. Record `Applicability`, `Status`, `Evidence`, `Finding`, and `Required action/exception` separately for each ID.

Before committing:

- every applicable Step row is `PASS` with concrete diff/path/symbol/command evidence;
- every genuinely inapplicable row is `N/A` with evidence and reason;
- any `FAIL`, `BLOCKED`, `UNKNOWN`, missing ID/evidence, or unresolved exception keeps the Step `In Progress`/`Blocked` and prohibits commit-as-complete;
- `MC-SCOPE-001` and `MC-TEST-001` are applicable to every coding Step;
- `MC-BLOCKER-001` summarizes all unresolved Step rows.

Use the six-column table from `references/step-gate-checklist.md`. Manual inspection is required even when static searches/tests pass.

## Stable Manual Check catalog

| Check ID | Blocking subject |
| --- | --- |
| `MC-ARCH-001` | Exactly one allowed architecture profile is preserved |
| `MC-REUSE-001` | Spring/Starter/Egon/module reuse was inspected before additions |
| `MC-DEP-001` | Added dependency/custom code has a proven approved gap, or none was added |
| `MC-NAME-001` | Touched type names use semantic roles and avoid ambiguous carrier suffixes |
| `MC-VALID-001` | Affected cross-layer inputs use Validation, groups, normalization, and tests |
| `MC-MODEL-001` | Record/`@Value`/complete complex-class Lombok baseline is present, or conflict blocks |
| `MC-CONVERT-001` | MapStruct/MapStructPlus and mandatory Egon `BaseConverter` own every new affected Converter |
| `MC-LOG-001` | Touched concrete business classes use `@Slf4j` and safe logging |
| `MC-BEAN-001` | Touched Beans have names, Lombok constructor injection, Qualifiers, and propagation |
| `MC-UTIL-001` | Only approved utilities are used and no duplicate helper is introduced |
| `MC-JSON-001` | Jackson is the sole JSON stack and contract annotations are correct |
| `MC-TIME-001` | Touched time modeling uses `java.time` with explicit semantics |
| `MC-CONFIG-001` | All environment profiles preserve equivalent configuration keys |
| `MC-PATTERN-001` | Every Complex business flow implements the approved pattern; only Simple flow remains direct |
| `MC-SCOPE-001` | Touched code complies without unrelated broad refactoring |
| `MC-TEST-001` | Focused tests/static checks prove applicable standards and behavior |
| `MC-BLOCKER-001` | All blockers/check failures are closed with no silent exception |

## Final audit rule

After all Step commits, repeat all 17 checks against the final tree and commits; do not merely aggregate prior Step assertions. Final PASS requires all applicable checks PASS and every N/A evidence-backed. Any Manual Check failure, any `Partial`/`Not satisfied` Spec requirement, or required missing runtime evidence produces `PARTIAL`; inability to determine the baseline/evidence produces `BLOCKED`. Report gaps; do not silently implement them during the audit.
