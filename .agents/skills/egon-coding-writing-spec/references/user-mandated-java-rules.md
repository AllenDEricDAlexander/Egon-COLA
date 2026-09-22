# User-mandated Java Rules and Spec Enforcement

Read `references/egon-java-cqe-contract.md` for the effective 2026-09-22 modeling, enum, MP, validation, uniqueness, CQE and architecture contract.

This reference is normative for every Java Spec. The source block below is consolidated from the user instructions, updated on 2026-09-22. It must not be translated, renumbered, corrected, shortened, generalized, or rewritten as recommendations.

## Effective normative source

```text
1 类名规范，必须以 java的pojo规范命名。以dao po bo vo dto query command event等结尾
2 每层之间必须被 springboot-validation 校验，复用对象使用分组校验；基于原生和自定义约束注解、@Valid、@Validated 及 ConstraintValidator 扩展实现。ValidationUtils 只承担通用手工校验，不承载全部业务校验；电话号码复用 libphonenumber。
3 Java POJO 默认使用 class，注解为 @Data @NoArgsConstructor @AllArgsConstructor @Accessors(chain = true)，按构造目标选择 @Builder 或 @SuperBuilder；父类状态参与相等性时使用 @EqualsAndHashCode(callSuper = true)。只有不可变 value object 可以使用 record，并豁免 class 的 Lombok 规范；普通实体不能因简单而使用 record。使用 MapStruct、MapStructPlus 和 egon-cola-component-common-core 的 BaseConverter（双向）或 BaseForwardConverter（不可逆投影）进行转换。
4业务类必须使用@Slf4j注解注入log对象。如果业务类被spring管理，必须指定名称，如果是单例的情况下，参考@Service("userService")。如果需要依赖注入，必须@RequiredArgsConstructor进行修饰，不要代码中写。且属性必须被@qualify修饰。
5 工具类只允许使用jdk原生、Apache Commons(commons-lang3、commons-collections4、commons-io、commons-text、commons-codec、commons-beanutils)、Guava。针对Tika按需引入。
6 JSON 使用 Spring Boot Jackson；持久化枚举值使用 @EnumValue，向前端输出的枚举值使用 @JsonValue，禁止 ordinal 作为业务编码。
7 springboot 多环境配置文件，必须保持配置一致，但值不一定一致。
9 复杂业务必须引入设计模式，不允许硬编码
10 日期相关的必须使用java.time下的实体类，不允许使用java.util下的
11 只允许现有三层结构或当前 egon-cola-archetypes 的 DDD 结构，三层结构保持现状。必须尽量复用 egon-cola-components；持久化模块必须使用 Egon COLA MP Starter，DDL 统一由 egon-mp-sdj-ext-starter 分布式管理。业务唯一键必须组合业务列与 deletedAt（数据库 deleted_at），并验证 NULL、租户及重复软删除语义。采用 CQE（Command Query Event）；Event 必须经 egon-cola-component-transactional-outbox-starter 或 MQ 中间件投递。
```

Literal spellings remain untouched above. Operationally, `@qualify` means Spring `@Qualifier`, `convertor` refers to the existing Egon `BaseConverter<S,T>` / `BaseForwardConverter<S,T>` contract, and `SpringBoot-JackSon` means the Spring Boot managed Jackson stack. These clarifications implement the literal rule; they do not weaken it.

## Absolute interpretation rule

Words such as **must**, **only**, and **not allowed** are blocking requirements. Do not replace them with “prefer,” “consider,” “when convenient,” “where semantically compatible,” or a broad repository-style exception. Current-project evidence selects the exact implementation form inside the mandate; it does not cancel the mandate.

When a literal requirement cannot be satisfied because of a compiler conflict, framework restriction, missing approved dependency, immutable public contract, or scope boundary, the Spec must:

1. mark the corresponding `MC-*` row `BLOCKED`;
2. cite the exact path, symbol, dependency, or generated-code evidence;
3. state the smallest compliant alternative and its impact;
4. ask the user for an explicit exception or design decision;
5. prohibit a PASS verdict until resolved.

## Rule 1 — mandatory semantic Java type naming

Create a complete inventory of every Java type added, renamed, or materially changed by the Spec.

| Type category               | Mandatory suffix examples                                                                                           | Required meaning                                                        |
|-----------------------------|---------------------------------------------------------------------------------------------------------------------|-------------------------------------------------------------------------|
| Persistence carrier         | `*PO`                                                                                                               | Maps persisted fields/table records                                     |
| Business carrier            | `*BO`                                                                                                               | Carries Service/business calculation state                              |
| View carrier                | `*VO`                                                                                                               | Carries presentation/output state                                       |
| Transfer carrier            | `*DTO`                                                                                                              | Cross-layer/service/integration transfer                                |
| Read input                  | `*Query`, `*PageQuery`                                                                                              | Encapsulates read criteria when a dedicated read object exists          |
| Write input                 | `*Command`                                                                                                          | Encapsulates state-changing intent when a dedicated write object exists |
| Published fact              | `*Event`                                                                                                            | Represents an occurred fact                                             |
| Protocol/output result      | `*Result`, `*PageResult`                                                                                            | HTTP/RPC/service output; paged output uses `*PageResult`                |
| Persistence identity object | `*Entity`                                                                                                           | Only when established ORM/entity identity and lifecycle require it      |
| Mapper access               | `*DAO`, `*Mapper`                                                                                                   | Database SQL access, not a carrier                                      |
| Anti-corruption access      | `*Repository`                                                                                                       | Service-facing persistence/ACL; replaces the former Gateway slot        |
| Behavior type               | `*Controller`, `*Service`, `*Converter`, `*Validator`, `*Component`, `*Factory`, `*Strategy`, `*Exception`, `*Enum` | Exact behavior responsibility                                           |

Inbound parameter types have no extra suffix requirement: do not add `*Request`. Output uses `*Result`, not `*Response`.
Persistence and outbound isolation use `*Repository`, not `*Gateway`. `UserData`, `UserInfo`, `UserParam`, `UserBean`,
and suffixless ambiguous carriers fail the rule. Existing ambiguous types outside the change surface are not a
broad-refactor mandate, but a newly created or materially changed type cannot inherit that ambiguity. Class-explosion
prevention remains mandatory: a new suffix does not justify a duplicate object without a real boundary or lifecycle
distinction.

The Spec must record: exact path, type name, role, suffix, source/target layer, mutability model, converter, validation group, and reuse-versus-new decision.

## Rule 2 — validation at every layer boundary

Validation is not Controller-only. Inventory every affected handoff, including as applicable:

- HTTP/RPC/message/job input -> Controller/Adapter;
- Controller/Adapter -> Service/Application;
- Service/Application -> Domain Service/Component;
- Service/Application -> Repository command/query input;
- event producer -> event consumer;
- any non-Spring-proxied construction path entering the same business operation.

For each crossing, the Spec must name the input type, constraints, `@Valid` cascade points, `@Validated` activation, selected Validation Group, normalization order, `ValidationUtils` invocation for direct/manual validation, error mapping, and focused positive/negative tests. A reusable object used by create/update/import/partial-update or other distinct scenarios must define explicit groups; a default group that weakens all scenarios fails.

Use Jakarta Validation native annotations first. For telephone data, define libphonenumber parsing, default/allowed region, canonical E.164 representation, validity result, invalid-input error, and idempotent normalization. Use custom constraint annotations with `ConstraintValidator` for reusable business rules; reuse native constraints for generic checks. `ValidationUtils` only invokes common metadata when manual validation is needed.

## Rule 3 — object construction and conversion

Classify every affected data object before choosing syntax:

- Ordinary POJOs/entities use `class` with `@Data`, `@NoArgsConstructor`, `@AllArgsConstructor`, `@Accessors(chain = true)`, a context-selected `@Builder` or `@SuperBuilder`, and `@EqualsAndHashCode(callSuper = true)` when superclass state participates in equality. Only immutable value objects may use `record`; see `references/egon-java-cqe-contract.md` for compile-safe root-class exceptions and collection immutability.
- An immutable value object may use `record` without mutable class annotations; a small field count does not establish value-object semantics.


Check `@SuperBuilder` across the inheritance chain and generated constructor signatures. Do not add `@RequiredArgsConstructor` to data classes; it remains the business Bean injection convention. Apply the explicit root/empty-class exceptions in `references/egon-java-cqe-contract.md`.

All cross-layer conversion uses MapStruct or MapStructPlus. Every new affected Converter must inherit/implement the existing `egon-cola-component-common-core` `BaseConverter<S,T>` / `BaseForwardConverter<S,T>` system and name the exact generic source/target types, mapper annotations, generated implementation, Bean name, null/default/enum/date/sensitive-field mappings, and conversion tests. Select the Common BaseForwardConverter for irreversible projections; if neither shared contract fits, report the gap; do not silently bypass it with manual setters, `BeanUtils.copyProperties`, reflection, JSON round trips, or an unrelated converter abstraction.

## Rule 4 — business logging and Spring dependency injection

Inventory every affected business behavior class, including Services, Service implementations, Components, domain/application services, handlers, strategies, factories, orchestrators, and scheduled/message business processors.

Each concrete business class must use `@Slf4j`. Each Spring-managed Bean must declare a stable explicit name in its stereotype or `@Bean` method. Every injected dependency must be a final field, the class must use Lombok `@RequiredArgsConstructor`, and every injected field must carry Spring `@Qualifier("exactBeanName")`, even when only one current candidate exists. Field `@Autowired` and handwritten injection constructors are prohibited.

The Spec must verify the applicable `lombok.config` copies `org.springframework.beans.factory.annotation.Qualifier` to generated constructor parameters. It must also define log event, level, stable identifiers, failure context, and sensitive-field exclusion; `@Slf4j` without useful/safe log design is incomplete.

## Rule 5 — closed utility allowlist

The only utility sources permitted for new/touched code are:

1. JDK native APIs;
2. `commons-lang3`, `commons-collections4`, `commons-io`, `commons-text`, `commons-codec`, `commons-beanutils`;
3. Guava;
4. Apache Tika only for an evidenced document/content-recognition requirement.

Any other utility dependency or new duplicate `*Utils` class is blocked. `commons-beanutils` being allowed as a utility does not permit it for Rule 3 object conversion.

## Rule 6 — Jackson-only external JSON contracts

Use the Spring Boot managed Jackson stack. Every affected external-interaction DTO/VO/Result/Command/Query/Event must
have an explicit annotation decision for field names, inclusion/ignore, unknown fields, enum values, dates/times,
polymorphism, sensitive fields, and compatibility. “No annotation needed” is acceptable only with field-by-field
evidence that Jackson defaults exactly match the effective interface contract. Gson, Fastjson, mixed mappers, and
serialization-based object conversion are prohibited.

## Rule 7 — configuration-profile structural parity

Inventory every Spring Boot base and environment-specific configuration file. For every added, renamed, or removed key, the Spec must provide a key-parity matrix showing the key in every profile. Values may differ; key hierarchy and the required core-key set may not. Prefer validated `@ConfigurationProperties` and include defaults, metadata, secret handling, compatibility, and profile-parity tests/static checks. Updating only one profile blocks PASS.

## Rule 9 — a design pattern is mandatory for complex business logic

Classify affected business logic independently from the overall document complexity. Business logic is complex when it has a real variation axis, state machine, rule composition, algorithm selection, responsibility pipeline, non-trivial object creation variant, event collaboration, or repeated branching that changes by type/status/scenario.

Once classified complex, the Spec must select and fully design at least one appropriate pattern such as Strategy, Template Method, Factory, Chain of Responsibility, State, Specification, or Domain Event. Name the variation point, pattern, participants, package/file placement, dependency direction, selection/registration mechanism, extension procedure, failure behavior, and tests. A long `if/else`, `switch`, type test, string dispatch, reflection dispatch, or “direct implementation is simpler” is not an acceptable substitute for complex business logic. Simple logic must remain direct and must not be inflated only to satisfy this rule.

## Rule 10 — java.time only

Every new or materially changed date/time field, parameter, persistence mapping, JSON field, calculation, duration, timeout, or clock source must use `java.time`. The Spec must name the concrete type (`LocalDate`, `LocalDateTime`, `Instant`, `OffsetDateTime`, `ZonedDateTime`, `Duration`, etc.), timezone, precision, clock source, persistence representation, JSON representation, and conversion boundary. New uses of `java.util.Date`, `Calendar`, or `SimpleDateFormat` fail the rule.

## Rule 11 — only two code-structure profiles

Before any package/file design, prove the current project is exactly one of:

1. the established traditional layered structure; or
2. the exact selected `egon-cola-archetype` structure and verifier contract.

The Spec must include the observed current tree, selected profile, evidence path, allowed dependency direction, target tree, and a statement that no third, hybrid, renamed, or invented layer/module is introduced. If neither profile fits, stop and ask the user; do not normalize the tree. The later Plan must repeat this confirmation before ordering files.

## Mandatory Spec evidence matrix

Every Java Spec must include a rule-by-rule matrix. No rows may be merged:

| Literal rule | Affected? | Repository evidence | Exact design decision | Files/types/interfaces | Validation/test evidence | Status/blocker |
| --- | --- | --- | --- | --- | --- | --- |
| Rule 1 | Yes / No | `<paths and symbols>` | `<mandatory naming decision>` | `<exact targets>` | `<search/compile/review gate>` | PASS / N/A / BLOCKED |

Rules 1, 2, 3, 4, 5, 6, 7, 9, 10, and 11 must each appear exactly once. A `No`/`N/A` row requires positive repository evidence proving the concern is absent from the affected surface.
