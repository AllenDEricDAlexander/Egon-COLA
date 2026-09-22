# User-mandated Java Rules and Plan Enforcement

Read `references/egon-java-cqe-contract.md` for the effective 2026-09-22 modeling, enum, MP, validation, uniqueness, CQE and architecture contract.

This reference is normative for every Java Plan. The source block is preserved byte-for-byte. A Plan must turn each rule into exact ordered files, pseudocode, validation, and a commit gate; it must not reduce a mandatory rule to a general coding note.

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

Literal spellings remain unchanged. `@qualify` is implemented as Spring `@Qualifier`, `convertor` as the Egon `BaseConverter<S,T>` / `BaseForwardConverter<S,T>` system, and `SpringBoot-JackSon` as the Spring Boot managed Jackson stack.

## No-weakening gate

The effective Spec must already decide these rules. If it does not, return the Plan to the Spec instead of inventing or relaxing the decision. A compiler/framework/dependency conflict is a blocker with exact evidence and user action. It is never permission to omit an annotation, skip a layer boundary, use a local converter, leave complex branching, or add a third architecture.

Every Java Plan contains the ten-row matrix in Chapter 4 using original numbers `1, 2, 3, 4, 5, 6, 7, 9, 10, 11`. Every Step contains `Literal Rules:` and every affected file contains `Literal rule enforcement:`.

## Rule 1 — exact type inventory and suffix plan

Inventory every created, renamed, or materially changed Java type with path, operation, role, mandatory suffix, consumers, and rename/compatibility impact. POJOs use semantic `PO`, `BO`, `VO`, `DTO`, `Query`, `Command`, `Event`, `Request`, `Response`, `PageQuery`, `PageResult`, or justified `Entity`; access components use `DAO`; behavior classes use exact behavior suffixes.

Plan no `Data`, `Info`, `Param`, `Bean`, or suffixless ambiguous carrier. For each new type, prove a distinct boundary/lifecycle need so suffix compliance does not create class explosion. Pseudocode uses the final exact type name, never a placeholder.

## Rule 2 — one validation design per affected handoff

The Plan must enumerate and order every affected crossing:

1. external input -> Controller/Adapter;
2. Controller/Adapter -> Service/Application;
3. Service/Application -> Domain Service/Component;
4. Service/Application -> DAO/Repository/Gateway command/query input;
5. event/job/internal re-entry paths.

For each crossing, name the exact input type, Jakarta annotations, `@Valid`, `@Validated`, Validation Group, `ValidationUtils` call when proxy validation is not guaranteed, normalization, error mapping, and positive/negative test. A reused object must define group marker types and group-specific constraints/invocations in exact files.

Telephone handling plans libphonenumber dependency evidence, region, parse, E.164 normalization, validity, exception/error contract, and tests. Use custom constraint annotations with `ConstraintValidator` for reusable business rules; reuse native constraints for generic checks. `ValidationUtils` only invokes common metadata when manual validation is needed.

## Rule 3 — exact object and Converter files

- Ordinary POJOs/entities use `class` with `@Data`, `@NoArgsConstructor`, `@AllArgsConstructor`, `@Accessors(chain = true)`, a context-selected `@Builder` or `@SuperBuilder`, and `@EqualsAndHashCode(callSuper = true)` when superclass state participates in equality. Only immutable value objects may use `record`; see `references/egon-java-cqe-contract.md` for compile-safe root-class exceptions and collection immutability.
- An immutable value object may use `record` without mutable class annotations; a small field count does not establish value-object semantics.


Verify construction targets, parent builders and generated signatures. Apply the scenario rules and root/empty-class exceptions in `references/egon-java-cqe-contract.md`; report remaining framework/API conflicts explicitly.

Every new affected Converter file uses MapStruct/MapStructPlus and implements/inherits `BaseConverter<S,T>` / `BaseForwardConverter<S,T>`. Pseudocode names exact generics, Mapper/Adapter annotations, Bean name, field mappings, qualifiers, null/default/enum/time/sensitive-field handling, and tests. Use Common BaseForwardConverter for irreversible projection; report a gap only if neither shared contract fits. Do not plan setters, BeanUtils, reflection, JSON mapping, or a separate converter hierarchy.

## Rule 4 — exact business-class annotations and injection

Each affected Service/ServiceImpl/Component/Domain Service/Application Service/Handler/Strategy/Factory/Orchestrator/scheduled or message business processor file must plan:

- `@Slf4j` and useful/safe log points;
- an explicit stable Spring Bean name when managed;
- final dependency fields;
- `@RequiredArgsConstructor` rather than a handwritten constructor;
- `@Qualifier("exactBeanName")` on every dependency field, even with one implementation;
- the exact `lombok.config` inspection/change proving Qualifier propagation.

The Step validation includes context/wiring tests or a compile gate that proves generated constructor injection.

## Rule 5 — closed utility allowlist

Every utility import/dependency is JDK, the named Apache Commons modules, or Guava. Tika requires an evidenced content-identification use case. Any other utility dependency or new duplicate `*Utils` file returns to the Spec. Commons BeanUtils cannot implement Rule 3 conversion.

## Rule 6 — Jackson contract files

For every affected external DTO/VO/Request/Response/Command/Query/Event, the file block lists exact Jackson annotations or field-by-field evidence that defaults match the protocol. Include serialization/compatibility tests for names, inclusion, enum/time shape, unknown fields, polymorphism, and sensitive data. No Gson/Fastjson or JSON object conversion.

## Rule 7 — all profile files in the change tree

When a configuration key changes, list the base file and every environment file in the same change tree/Step, plus the `@ConfigurationProperties` class, metadata, and tests as applicable. The Plan includes a key-parity comparison command/method and expected identical key set. Different values are allowed; missing/extra core keys are not.

## Rule 9 — pattern files are mandatory for complex business logic

Classify each affected business flow Simple or Complex with evidence. A Complex flow must have an effective-Spec-selected Strategy, Template Method, Factory, Chain of Responsibility, State, Specification, Domain Event, or other appropriate pattern. The Plan lists participant files, interface/abstract contract where the chosen pattern actually requires it, implementations, registry/selection/wiring, orchestration change, and focused tests in dependency order.

Pseudocode may not leave the variation as `if/else`, `switch`, type/string/reflection dispatch, nor say direct logic is sufficient. Simple flows stay direct and must not receive ceremonial pattern classes.

## Rule 10 — java.time file and field mapping

Every affected date/time symbol uses a concrete `java.time` type. File blocks state zone, precision, Clock source, database/JDBC mapping, JSON format, duration/timeout behavior, compatibility conversion, and tests. New `Date`, `Calendar`, or `SimpleDateFormat` imports are forbidden and must have a static-search gate.

## Rule 11 — architecture confirmation before file ordering

Before deriving Steps, show the observed current tree and choose exactly traditional layered or the exact selected Egon Archetype. Record evidence, verifier, dependency direction, target tree, and why every new file belongs there. A hybrid, third structure, renamed Archetype module, or invented layer blocks the Plan. Every Step includes Rule 11 because every file placement must preserve the selected structure.

## Mandatory Plan matrix

| Literal rule | Spec source | Repository evidence | Exact files and order | Pseudocode obligations | Validation gate | Steps | Status/blocker |
| --- | --- | --- | --- | --- | --- | --- | --- |
| Rule 1 | `<Spec section>` | `<paths/symbols>` | `<ordered files>` | `<mandatory implementation details>` | `<exact command/review>` | `<Step N>` | PASS / N/A / BLOCKED |

All ten rows are required in original-number order. A row marked N/A requires positive evidence. A Plan cannot PASS while any literal rule is missing, weakened, contradicted by a Step, or blocked.
