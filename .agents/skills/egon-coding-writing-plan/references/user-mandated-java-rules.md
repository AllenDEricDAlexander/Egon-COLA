# User-mandated Java Rules and Plan Enforcement

This reference is normative for every Java Plan. The source block is preserved byte-for-byte. A Plan must turn each rule into exact ordered files, pseudocode, validation, and a commit gate; it must not reduce a mandatory rule to a general coding note.

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

Literal spellings remain unchanged. `@qualify` is implemented as Spring `@Qualifier`, `convertor` as the Egon `BaseConverter<S,T>` system, and `SpringBoot-JackSon` as the Spring Boot managed Jackson stack.

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

Telephone handling plans libphonenumber dependency evidence, region, parse, E.164 normalization, validity, exception/error contract, and tests. A custom Validator file is forbidden unless the effective Spec explicitly approves the proven native/group/library gap.

## Rule 3 — exact object and Converter files

- Simple object file: Java `record`; pseudocode shows components and compact constructor normalization. One-method temporary state may be a local `record` in the owning method and must not become a separate file.
- Immutable non-record file: Lombok `@Value`.
- Complex object file: normal class with the complete baseline `@Data`, `@NoArgsConstructor(access = AccessLevel.PROTECTED)`, `@AllArgsConstructor`, `@RequiredArgsConstructor`, `@Builder`, `@Accessors(chain = true)`.

The Plan must calculate generated constructor signatures and compilation/framework consequences. A conflict returns `BLOCKED`; it cannot plan a reduced annotation set.

Every new affected Converter file uses MapStruct/MapStructPlus and implements/inherits `BaseConverter<S,T>`. Pseudocode names exact generics, Mapper/Adapter annotations, Bean name, field mappings, qualifiers, null/default/enum/time/sensitive-field handling, and tests. If `BaseConverter` cannot express the mapping, return to the Spec/user. Do not plan setters, BeanUtils, reflection, JSON mapping, or a separate converter hierarchy.

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
