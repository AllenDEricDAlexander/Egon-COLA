# 用户强制 Java 规则与 Plan 执行细则

本参考是每份 Java Plan 的强制规范。下方源区块逐字保留。Plan 必须把每条规则转成准确文件顺序、伪代码、验证和提交门禁，不能把强制规则缩成一般编码提示。

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

原文拼写保持不变。`@qualify` 落地为 Spring `@Qualifier`，`convertor` 落地为 Egon `BaseConverter<S,T>` 体系，`SpringBoot-JackSon` 落地为 Spring Boot 管理的 Jackson。

## 禁止弱化门禁

有效 Spec 必须已定案这些规则。Spec 缺失时，Plan 返回 Spec，不能自行放松或发明。编译/框架/依赖冲突必须带准确证据阻断；不能借此删注解、跳过层间边界、使用本地 Converter、保留复杂分支或新增第三种架构。

每份 Java Plan 在第 4 章按原编号包含规则 `1、2、3、4、5、6、7、9、10、11` 十行矩阵。每个 Step 写 `Literal Rules:`，每个受影响文件写 `Literal rule enforcement:`。

## 规则 1——准确类型清单与后缀计划

清单每个 Create/Rename/实质修改 Java 类型的路径、操作、职责、强制后缀、消费者和兼容影响。POJO 使用 PO、BO、VO、DTO、Query、Command、Event、Request、Response、PageQuery、PageResult 或有依据 Entity；访问组件使用 DAO；行为类使用准确行为后缀。

禁止规划 `Data`、`Info`、`Param`、`Bean` 或无后缀含糊载体。每个新类型必须证明独立边界/生命周期需要，避免后缀合规导致类爆炸。伪代码必须使用最终准确类型名。

## 规则 2——每个受影响交接独立校验

Plan 必须分别枚举并排序：外部输入 -> Controller/Adapter；Controller/Adapter -> Service/Application；Service/Application -> Domain Service/Component；Service/Application -> DAO/Repository/Gateway；Event/Job/内部重入。

每个交接写准确输入、Jakarta 注解、`@Valid`、`@Validated`、Validation Group、非 Proxy 场景 `ValidationUtils`、规范化、错误和正反测试。复用对象必须在准确文件定义 Group Marker、Group Constraint 和调用。电话号码写 libphonenumber 依赖证据、地区、Parse、E.164、Validity、错误和测试。Spec 未批准能力缺口时禁止自定义 Validator 文件。

## 规则 3——准确对象与 Converter 文件

- 简单对象：Java `record`，伪代码写 Component 与紧凑构造器；方法内临时状态可用局部 `record`，不得单独建文件。
- 不可变非 Record：Lombok `@Value`。
- 复杂对象：普通类，完整使用 `@Data`、`@NoArgsConstructor(access = AccessLevel.PROTECTED)`、`@AllArgsConstructor`、`@RequiredArgsConstructor`、`@Builder`、`@Accessors(chain = true)`。

Plan 必须计算构造器签名和编译/框架后果；冲突即 `BLOCKED`，不能规划缩减注解。

每个新增受影响 Converter 使用 MapStruct/MapStructPlus 并继承/实现 `BaseConverter<S,T>`，写准确泛型、Mapper 注解、Bean 名、字段/Qualifier/空值/默认/枚举/时间/敏感字段和测试。`BaseConverter` 无法表达时返回 Spec/用户；禁止 Setter、BeanUtils、反射、JSON 或另建转换体系。

## 规则 4——业务类注解与注入

每个受影响 Service/ServiceImpl/Component/Domain/Application Service/Handler/Strategy/Factory/Orchestrator/Schedule/Message Processor 文件都规划：`@Slf4j` 与安全有效日志；显式稳定 Bean 名；Final 依赖；`@RequiredArgsConstructor`；每个依赖字段 `@Qualifier("exactBeanName")`；准确 `lombok.config` 检查/修改。Step 必须用 Wiring Test 或 Compile Gate 证明生成构造注入。

## 规则 5——封闭工具白名单

每个工具 Import/Dependency 只能是 JDK、指定 Apache Commons 模块或 Guava；Tika 必须有内容识别需求。其他工具依赖或重复 `*Utils` 返回 Spec。Commons BeanUtils 不能承担规则 3 转换。

## 规则 6——Jackson 契约文件

每个受影响外部 DTO/VO/Request/Response/Command/Query/Event 文件写准确 Jackson 注解，或逐字段证明默认行为符合协议。增加字段名、Include、Enum/Time、Unknown、Polymorphism、Sensitive 与兼容序列化测试。禁止 Gson/Fastjson 与 JSON 对象转换。

## 规则 7——所有 Profile 文件进入 Tree

配置 Key 变化时，同一 Tree/Step 列出 Base 与每个环境文件，以及适用的 `@ConfigurationProperties`、Metadata 和 Test。Plan 写 Key 对齐命令/方法及“Key 集合完全相同”的预期；Value 可不同，核心 Key 缺失/多出不允许。

## 规则 9——复杂业务强制模式文件

每个受影响业务 Flow 用证据分类 Simple/Complex。Complex Flow 必须使用有效 Spec 已选 Strategy、Template Method、Factory、Chain of Responsibility、State、Specification、Domain Event 等模式，按依赖顺序列参与者、必要接口/抽象、实现、Registry/Selection/Wiring、Orchestration 和测试。

伪代码禁止把变化保留为 `if/else`、`switch`、类型/字符串/反射分发，也不能写直接逻辑足够。Simple Flow 保持直接，禁止仪式性模式类。

## 规则 10——java.time 文件与字段映射

每个受影响日期时间符号使用准确 `java.time` 类型。文件块写时区、精度、Clock、DB/JDBC、JSON、Duration/Timeout、兼容转换和测试。静态门禁禁止新增 `Date`、`Calendar`、`SimpleDateFormat` Import。

## 规则 11——文件排序前确认架构

推导 Step 前展示当前 Tree，并只选择传统分层或准确 Egon Archetype。记录证据、Verifier、依赖方向、目标 Tree 和每个新文件归属理由。混合/第三种结构、Archetype 模块改名或自创层会阻断。每个 Step 都列 Rule 11，因为每个文件位置都必须保持所选结构。

## Plan 强制矩阵

| 原文规则 | Spec 来源 | 仓库证据 | 准确文件与顺序 | 伪代码义务 | 验证门禁 | Steps | 状态/阻断 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| Rule 1 | `<Spec 章节>` | `<路径/符号>` | `<有序文件>` | `<强制实施细节>` | `<准确命令/审核>` | `<Step N>` | PASS / N/A / BLOCKED |

十行必须按原编号顺序出现。N/A 必须有正向证据。任何规则缺失、弱化、与 Step 矛盾或阻断时，Plan 禁止 PASS。
