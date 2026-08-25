# 用户强制 Java 规则与执行门禁

本参考在编码前、每个 Step、每次提交前和最终审核中都具有强制性。下方源区块逐字保留。测试全绿不能豁免任何逐字规则。

## 逐字规范源

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

原始拼写保持不动。执行中，`@qualify` 对应 Spring `@Qualifier`，`convertor` 对应 Egon `BaseConverter<S,T>` 体系，`SpringBoot-JackSon` 对应 Spring Boot Jackson。

## 硬停止规则

逐字源和已批准 Spec/Plan 决定实现。仓库旧风格不能弱化规则。当前代码、编译器、框架、依赖或 Step 范围妨碍合规时：不能静默选择缩减实现；Step 保持 `In Progress` 或 `Blocked`；记录准确路径/符号/编译/依赖证据；报告最小合规修正和所需审批；禁止按完成提交和进入下一 Step。

锁定 Step 时，从 `references/step-gate-checklist.md` 建立十行 Literal Rule Gate；最终 Diff 后刷新。适用行全部 PASS，N/A 必须有正向范围证据。

## 规则 1——语义命名检查

检查每个新增、重命名或实质修改 Java 类型最终声明和消费者。POJO 使用 PO、BO、VO、DTO、Query、Command、Event、Request、Response、PageQuery、PageResult 或有依据 Entity；访问组件用 DAO；行为类型用准确职责后缀。

拒绝新增/触达 `Data`、`Info`、`Param`、`Bean` 和无后缀载体。每个新类型要有真实边界/生命周期依据，避免类爆炸。记录类型清单和编译证据。

## 规则 2——检查每个受影响层间交接

不能停在 Controller。分别检查/测试外部 -> Controller/Adapter、Controller/Adapter -> Service/Application、Service/Application -> Domain Service/Component、Service/Application -> DAO/Repository/Gateway，以及 Event/Job/内部重入。

核对 Jakarta 注解、`@Valid`、`@Validated`、Validation Group、非 Proxy 场景 `ValidationUtils`、规范化顺序、错误和正反测试。复用对象必须在每个调用点使用场景 Group。电话输入必须走已批准 libphonenumber，包含 Region、E.164、Validity、错误和测试；拒绝未批准自定义 Validator 或重复解析。

## 规则 3——检查准确模型与 Converter

- 简单对象必须 Java `record`，规范化使用紧凑构造器；方法内临时状态可用局部 `record`。
- 不可变非 Record 必须 Lombok `@Value`。
- 复杂对象必须普通类并完整使用 `@Data`、`@NoArgsConstructor(access = AccessLevel.PROTECTED)`、`@AllArgsConstructor`、`@RequiredArgsConstructor`、`@Builder`、`@Accessors(chain = true)`。

必须编译证明完整 Lombok 组合。重复构造器/框架冲突会阻断，未经用户批准不能删除注解。

每个新增受影响 Converter 必须 MapStruct/MapStructPlus 且继承/实现 `BaseConverter<S,T>`。检查泛型、Mapping、Qualifier、空值/默认/枚举/时间/敏感字段、生成实现、Bean 名和测试。拒绝 Setter、`BeanUtils.copyProperties`、反射、JSON 或本地/单向 Converter 绕过。

## 规则 4——检查每个业务类与 Bean

逐个检查触达 Service/ServiceImpl/Component/Domain/Application Service/Handler/Strategy/Factory/Orchestrator/Schedule/Message Processor：必须 `@Slf4j` 和安全有效日志；Spring 管理时有显式稳定 Bean 名；依赖字段 Final；类有 `@RequiredArgsConstructor`；每个依赖字段都有 `@Qualifier("exactBeanName")`；禁止字段 `@Autowired` 和手写构造器；检查 `lombok.config` Qualifier Copy 和编译/Wiring 证据。

## 规则 5——执行工具白名单

检查触达 Import、Build Manifest 和新 Helper。只允许 JDK、指定 Apache Commons、Guava 和有证据的 Tika。拒绝其他工具库或重复 `*Utils`。Commons BeanUtils 仍不能做规则 3 映射。

## 规则 6——执行 Jackson 契约

检查每个触达外部 DTO/VO/Request/Response/Command/Query/Event。核对必要 Jackson 注解，或逐字段证明默认行为等于协议；执行序列化/兼容测试。拒绝 Gson/Fastjson、混合 ObjectMapper 和 JSON 转换技巧。

## 规则 7——比较全部环境 Profile

配置 Key 变化时，在 Diff 后比较 Base 与每个环境文件的规范化 Key Path，结构/核心 Key 集合必须完全一致，只有 Value 可不同。核对 `@ConfigurationProperties`、默认、Validation、Metadata 和测试。只改一个 Profile 失败。

## 规则 9——复杂业务必须包含获批模式

重新确认每个触达 Flow 的 Simple/Complex 分类。Complex Flow 必须实现获批 Strategy、Template Method、Factory、Chain of Responsibility、State、Specification、Domain Event 等模式的实际参与者、选择/接线、编排、失败和测试代码。

拒绝长 `if/else`、`switch`、类型/字符串/反射分发或用直接编排替代模式。Simple 逻辑保持直接，不能新增仪式性模式类。

## 规则 10——拒绝 java.util 日期时间

检查触达 Import、字段、API、持久化、JSON、计算、Duration、Timeout 和 Clock。必须使用 `java.time` 并明确时区、精度、Clock、数据库和 JSON。新增 `java.util.Date`、`Calendar`、`SimpleDateFormat` 即使测试通过也失败。

## 规则 11——每 Step 前后检查架构

编辑前把每个 Step 路径与所选传统分层或准确 Egon Archetype Tree 对比；Diff 后确认没有第三/混合结构、Archetype 模块改名、自创层或依赖方向违规。Rule 11 对每个 Coding Step 和最终审核都适用。

## 强制证据记录

| 原文规则 | 适用性 | 状态 | Diff/路径/符号证据 | 测试/静态证据 | 结论 | 必需动作/例外 |
| --- | --- | --- | --- | --- | --- | --- |
| Rule 1 | Applicable / Not applicable | PASS / N/A / FAIL / BLOCKED | `<准确证据>` | `<命令/结果>` | `<结论>` | `None / 动作和负责人` |

按原顺序 `1、2、3、4、5、6、7、9、10、11`。合并行、缺证据、FAIL、BLOCKED、UNKNOWN 或静默例外都禁止 Step 验证、完成提交和最终 PASS。
