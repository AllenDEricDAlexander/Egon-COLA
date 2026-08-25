# User-mandated Java Rules and Execution Gates

This reference is normative before coding, during every Step, before every Step commit, and during the final audit. The source block is preserved byte-for-byte. A successful test suite cannot waive a literal rule.

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

Literal spellings stay unchanged. During execution, `@qualify` means Spring `@Qualifier`, `convertor` means the Egon `BaseConverter<S,T>` system, and `SpringBoot-JackSon` means Spring Boot managed Jackson.

## Hard stop rule

The exact source and the approved Spec/Plan decide the implementation. Repository style cannot weaken the mandate. If current code, compiler behavior, framework requirements, dependencies, or the Step scope prevent literal compliance:

1. do not silently choose a reduced implementation;
2. keep the Step `In Progress` or mark it `Blocked`;
3. record exact path/symbol/compiler/dependency evidence;
4. report the smallest compliant correction and whether Spec/Plan/user approval is required;
5. do not commit the Step as complete and do not advance.

At Step lock, create the ten-row Literal Rule Gate from `references/step-gate-checklist.md`. Refresh it after the final diff. All applicable rows must PASS; N/A needs positive scope evidence.

## Rule 1 — semantic name inspection

For every Java type created, renamed, or materially changed, inspect the final declaration and consumers. POJOs must end in semantic `PO`, `BO`, `VO`, `DTO`, `Query`, `Command`, `Event`, `Request`, `Response`, `PageQuery`, `PageResult`, or justified `Entity`; access components use `DAO`; behavior types use their exact role suffix.

Reject new/touched ambiguous `Data`, `Info`, `Param`, `Bean`, or suffixless carrier names. Verify a distinct boundary/lifecycle reason for every new type to prevent class explosion. Record the type inventory and compile evidence.

## Rule 2 — validate each affected layer handoff

Do not stop at Controller validation. Inspect and test each affected external -> Controller/Adapter, Controller/Adapter -> Service/Application, Service/Application -> Domain Service/Component, Service/Application -> DAO/Repository/Gateway, and Event/Job/internal re-entry handoff.

Verify actual Jakarta annotations, `@Valid`, `@Validated`, selected Validation Group, `ValidationUtils` invocation when proxy validation is absent, normalization order, error mapping, and positive/negative tests. Reused objects must use scenario groups at every applicable call site.

Telephone input must use the approved libphonenumber path with region, E.164 normalization, validity, errors, and tests. Reject an unapproved custom Validator or duplicate parsing.

## Rule 3 — inspect exact model and Converter code

- Simple object must be a Java `record`; normalization uses a compact constructor. One-method temporary state may be a local `record`.
- Immutable non-record must use Lombok `@Value`.
- Complex object must be a normal class with the complete baseline `@Data`, `@NoArgsConstructor(access = AccessLevel.PROTECTED)`, `@AllArgsConstructor`, `@RequiredArgsConstructor`, `@Builder`, `@Accessors(chain = true)`.

Compile to prove the complete Lombok combination. A duplicate constructor or framework conflict blocks; do not delete an annotation without user-approved design correction.

Every new affected Converter must use MapStruct/MapStructPlus and implement/inherit `BaseConverter<S,T>`. Inspect exact generics, mappings, qualifiers, null/default/enum/time/sensitive fields, generated implementation, Bean name, and tests. Reject manual setters, `BeanUtils.copyProperties`, reflection, JSON round trips, or local/one-way Converter bypasses.

## Rule 4 — inspect every business class and Bean

For each touched Service/ServiceImpl/Component/Domain Service/Application Service/Handler/Strategy/Factory/Orchestrator/Schedule/Message Processor:

- require `@Slf4j` and safe useful logs;
- require an explicit stable Spring Bean name when managed;
- require final dependency fields and Lombok `@RequiredArgsConstructor`;
- require `@Qualifier("exactBeanName")` on every injected field, even with one implementation;
- reject field `@Autowired` and handwritten injection constructors;
- inspect `lombok.config` Qualifier copy configuration and compile/wiring evidence.

## Rule 5 — enforce the utility allowlist

Inspect touched imports, build manifests, and new helper files. Only JDK, the named Apache Commons modules, Guava, and evidenced Tika are allowed. Reject another utility library or duplicate `*Utils`. Commons BeanUtils remains forbidden for Rule 3 mapping.

## Rule 6 — enforce Jackson contracts

Inspect every touched external DTO/VO/Request/Response/Command/Query/Event. Confirm necessary Jackson annotations or field-by-field evidence that defaults exactly match the protocol. Run focused serialization/compatibility tests. Reject Gson/Fastjson imports, mixed ObjectMappers, or JSON conversion tricks.

## Rule 7 — compare all environment profiles

When any configuration key changes, inspect the base file and every environment file after the diff. Compare normalized key paths and require identical structure/core key sets; only values may differ. Verify `@ConfigurationProperties`, defaults, validation, metadata, and tests where applicable. A one-profile-only change fails.

## Rule 9 — complex business logic must contain the approved pattern

Reconfirm the Plan's Simple/Complex classification for each touched flow. A Complex flow must implement the approved Strategy, Template Method, Factory, Chain of Responsibility, State, Specification, Domain Event, or other pattern with actual participant, selection/wiring, orchestration, failure, and test code.

Reject long `if/else`, `switch`, type/string/reflection dispatch, or direct orchestration replacing the approved pattern. Simple logic remains direct; do not invent ceremonial pattern classes.

## Rule 10 — reject java.util date/time

Inspect touched imports, fields, APIs, persistence mappings, JSON, calculations, durations, timeouts, and clocks. Require `java.time` with exact zone, precision, Clock, database, and JSON semantics. New `java.util.Date`, `Calendar`, or `SimpleDateFormat` fails even when tests pass.

## Rule 11 — verify architecture before and after each Step

Before editing, compare every Step path with the selected traditional layered or exact Egon Archetype tree. After the diff, confirm no third/hybrid structure, renamed Archetype module, invented layer, or dependency-direction violation exists. Rule 11 is applicable to every coding Step and the final audit.

## Required evidence record

| Literal rule | Applicability | Status | Diff/path/symbol evidence | Test/static evidence | Finding | Required action/exception |
| --- | --- | --- | --- | --- | --- | --- |
| Rule 1 | Applicable / Not applicable | PASS / N/A / FAIL / BLOCKED | `<exact evidence>` | `<command/result>` | `<conclusion>` | `None / action and owner` |

Use original order `1, 2, 3, 4, 5, 6, 7, 9, 10, 11`. No merged row, missing evidence, FAIL, BLOCKED, UNKNOWN, or silent exception permits Step verification, a completion commit, or final PASS.
