# User-mandated Java Rules and Spec Enforcement

This reference is normative for every Java Spec. The source block below is preserved byte-for-byte from the user instruction. It must not be translated, renumbered, corrected, shortened, generalized, or rewritten as recommendations.

## Verbatim normative source

```text
1 类名规范，必须以 java的pojo规范命名。以dao po bo vo dto query command event等结尾
2 每层之间必须被 springboot-validation 校验，复用的对象 validation 要分组校验，ValidatorUtils使用 libphonenumber进行规范化校验或者validation原生注解，若非必要，不要自己写。
3 实体类规范：复杂对象使用java类并使用Lombok进行@Data\@NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor\@RequiredArgsConstructor\@Builder\@Accessors(chain = true)修饰。简单对象使用Java Record。使用MapStruct、MapStructPlus 进行转换，egon-cola-component-common-core有通用的convertor，必须继承实现这个。如果是不可变对象，使用@Value注释修饰。record场景Record 构造器很适合做数据规范化，推荐使用紧凑构造器，Record 可以作为局部类，在方法内部定义临时数据结构。
4业务类必须使用@Slf4j注解注入log对象。如果业务类被spring管理，必须指定名称，如果是单例的情况下，参考@Service("userService")。如果需要依赖注入，必须@RequiredArgsConstructor进行修饰，不要代码中写。且属性必须被@qualify修饰。
5 工具类只允许使用jdk原生、Apache Commons(commons-lang3、commons-collections4、commons-io、commons-text、commons-codec、commons-beanutils)、Guava。针对Tika按需引入。
6 json 使用SpringBoot-JackSon 对外交互层的实体类必须按需被jackson注解修饰。
7 springboot 多环境配置文件，必须保持配置一致，但值不一定一致。
9 复杂业务必须引入设计模式，不允许硬编码
10 日期相关的必须使用java.time下的实体类，不允许使用java.util下的
11 plan中必须确认代码结构，分层结构或者egon-cola-archetype，只允许这两种代码结构规范。&#x20;
```

Literal spellings remain untouched above. Operationally, `@qualify` means Spring `@Qualifier`, `convertor` refers to the existing Egon `BaseConverter<S,T>` contract, and `SpringBoot-JackSon` means the Spring Boot managed Jackson stack. These clarifications implement the literal rule; they do not weaken it.

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

| Type category | Mandatory suffix examples | Required meaning |
| --- | --- | --- |
| Persistence carrier | `*PO` | Maps persisted fields/table records |
| Business carrier | `*BO` | Carries Service/business calculation state |
| View carrier | `*VO` | Carries presentation/output state |
| Transfer carrier | `*DTO` | Cross-layer/service/integration transfer |
| Read input | `*Query`, `*PageQuery` | Encapsulates read criteria |
| Write input | `*Command` | Encapsulates state-changing intent |
| Published fact | `*Event` | Represents an occurred fact |
| Protocol boundary | `*Request`, `*Response`, `*PageResult` | Explicit HTTP/RPC boundary when used by the project |
| Persistence identity object | `*Entity` | Only when established ORM/entity identity and lifecycle require it |
| Access component | `*DAO` | Database access behavior, not a carrier |
| Behavior type | `*Controller`, `*Service`, `*Repository`, `*Gateway`, `*Converter`, `*Validator`, `*Component`, `*Factory`, `*Strategy`, `*Exception`, `*Result`, `*Enum` | Exact behavior responsibility |

`UserData`, `UserInfo`, `UserParam`, `UserBean`, and suffixless ambiguous carriers fail the rule. Existing ambiguous types outside the change surface are not a broad-refactor mandate, but a newly created or materially changed type cannot inherit that ambiguity. Class-explosion prevention remains mandatory: a new suffix does not justify a duplicate object without a real boundary or lifecycle distinction.

The Spec must record: exact path, type name, role, suffix, source/target layer, mutability model, converter, validation group, and reuse-versus-new decision.

## Rule 2 — validation at every layer boundary

Validation is not Controller-only. Inventory every affected handoff, including as applicable:

- HTTP/RPC/message/job input -> Controller/Adapter;
- Controller/Adapter -> Service/Application;
- Service/Application -> Domain Service/Component;
- Service/Application -> DAO/Repository/Gateway command/query input;
- event producer -> event consumer;
- any non-Spring-proxied construction path entering the same business operation.

For each crossing, the Spec must name the input type, constraints, `@Valid` cascade points, `@Validated` activation, selected Validation Group, normalization order, `ValidationUtils` invocation for direct/manual validation, error mapping, and focused positive/negative tests. A reusable object used by create/update/import/partial-update or other distinct scenarios must define explicit groups; a default group that weakens all scenarios fails.

Use Jakarta Validation native annotations first. For telephone data, define libphonenumber parsing, default/allowed region, canonical E.164 representation, validity result, invalid-input error, and idempotent normalization. A custom Validator is prohibited unless the Spec proves native annotations, composed constraints, groups, `ValidationUtils`, and the mature library cannot express the rule.

## Rule 3 — object construction and conversion

Classify every affected data object before choosing syntax:

- **Simple object:** use Java `record`. When normalization is needed, design the compact constructor explicitly. A one-method temporary structure may be a local `record` inside the method.
- **Immutable non-record object:** use Lombok `@Value`.
- **Complex object:** use a normal Java class and treat `@Data`, `@NoArgsConstructor(access = AccessLevel.PROTECTED)`, `@AllArgsConstructor`, `@RequiredArgsConstructor`, `@Builder`, and `@Accessors(chain = true)` as the mandated annotation baseline from the user rule.

The Spec must calculate the constructors generated by that exact Lombok combination. If `@RequiredArgsConstructor` and another constructor would generate the same signature, or an ORM/framework forbids the combination, this is a blocker requiring an explicit user decision; do not silently omit an annotation or relabel the object as simple.

All cross-layer conversion uses MapStruct or MapStructPlus. Every new affected Converter must inherit/implement the existing `egon-cola-component-common-core` `BaseConverter<S,T>` system and name the exact generic source/target types, mapper annotations, generated implementation, Bean name, null/default/enum/date/sensitive-field mappings, and conversion tests. If the current `BaseConverter` cannot represent the required direction, report a blocking contract conflict; do not silently bypass it with manual setters, `BeanUtils.copyProperties`, reflection, JSON round trips, or an unrelated converter abstraction.

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

Use the Spring Boot managed Jackson stack. Every affected external-interaction DTO/VO/Request/Response/Command/Query/Event must have an explicit annotation decision for field names, inclusion/ignore, unknown fields, enum values, dates/times, polymorphism, sensitive fields, and compatibility. “No annotation needed” is acceptable only with field-by-field evidence that Jackson defaults exactly match the effective interface contract. Gson, Fastjson, mixed mappers, and serialization-based object conversion are prohibited.

## Rule 7 — configuration-profile structural parity

Inventory every Spring Boot base and environment-specific configuration file. For every added, renamed, or removed key, the Spec must provide a key-parity matrix showing the key in every profile. Values may differ; key hierarchy and the required core-key set may not. Prefer validated `@ConfigurationProperties` and include defaults, metadata, secret handling, compatibility, and profile-parity tests/static checks. Updating only one profile blocks PASS.

## Rule 9 — a design pattern is mandatory for complex business logic

Classify affected business logic independently from the overall document complexity. Business logic is complex when it has a real variation axis, state machine, rule composition, algorithm selection, responsibility pipeline, non-trivial object creation family, event collaboration, or repeated branching that changes by type/status/scenario.

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
