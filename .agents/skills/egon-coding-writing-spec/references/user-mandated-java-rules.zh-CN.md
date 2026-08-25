# 用户强制 Java 规则与 Spec 执行细则

本参考是每份 Java Spec 的强制规范。下方源区块逐字保留用户指令，禁止翻译、重新编号、纠错、缩写、概括或改写成建议。

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

原文拼写保持不动。执行时，`@qualify` 对应 Spring `@Qualifier`，`convertor` 对应 Egon 已有 `BaseConverter<S,T>` 契约，`SpringBoot-JackSon` 对应 Spring Boot 管理的 Jackson 体系。这些是准确落地原文，不是放宽规则。

## 绝对解释规则

原文中的“必须”“只允许”“不允许”都是阻断要求。禁止替换成“优先”“建议”“视情况”“语义兼容时”或宽泛的“遵循仓库风格”。仓库证据只能决定强制规则内部的准确实现，不能取消强制规则。

由于编译冲突、框架限制、缺少已批准依赖、不可变公开契约或范围边界而无法满足时，Spec 必须：

1. 把对应 `MC-*` 标为 `BLOCKED`；
2. 引用准确路径、符号、依赖或生成代码证据；
3. 写出最小合规替代方案和影响；
4. 请求用户明确批准例外或设计决策；
5. 问题关闭前禁止 PASS。

## 规则 1——Java 类型强制语义命名

完整清单本 Spec 新增、重命名或实质修改的所有 Java 类型。

| 类型 | 强制后缀示例 | 准确含义 |
| --- | --- | --- |
| 持久化载体 | `*PO` | 表/持久化字段映射 |
| 业务载体 | `*BO` | Service 内业务计算状态 |
| 展示载体 | `*VO` | 展示/输出状态 |
| 传输载体 | `*DTO` | 跨层、服务或集成传输 |
| 查询输入 | `*Query`、`*PageQuery` | 读取条件 |
| 修改输入 | `*Command` | 状态修改意图 |
| 已发生事实 | `*Event` | 已发生事件 |
| 协议边界 | `*Request`、`*Response`、`*PageResult` | 项目采用的 HTTP/RPC 边界 |
| 持久化身份对象 | `*Entity` | 只有既有 ORM 身份/生命周期明确时使用 |
| 访问组件 | `*DAO` | 数据库访问行为，不是载体 |
| 行为类型 | `*Controller`、`*Service`、`*Repository`、`*Gateway`、`*Converter`、`*Validator`、`*Component`、`*Factory`、`*Strategy`、`*Exception`、`*Result`、`*Enum` | 准确行为职责 |

`UserData`、`UserInfo`、`UserParam`、`UserBean` 和无后缀含糊载体不合格。范围外旧命名不授权大重构，但新增或实质修改类型不能继承这种含糊。防止类爆炸仍是强制要求：没有真实边界/生命周期差异时，换一个后缀不能成为重复建类理由。

Spec 必须记录准确路径、类型、职责、后缀、来源/目标层、可变性、Converter、Validation Group 和复用/新增决策。

## 规则 2——每个层间边界都校验

校验不能只放 Controller。分别清单并设计适用的：

- HTTP/RPC/消息/Job 输入 -> Controller/Adapter；
- Controller/Adapter -> Service/Application；
- Service/Application -> Domain Service/Component；
- Service/Application -> DAO/Repository/Gateway 的 Command/Query 输入；
- Event Producer -> Event Consumer；
- 进入同一业务操作的非 Spring Proxy 构造路径。

每个交接都要写输入类型、约束、`@Valid` 级联、`@Validated` 激活、Validation Group、规范化顺序、非 Proxy/手工校验的 `ValidationUtils` 调用、错误映射和正反测试。Create/Update/Import/Partial-update 等复用对象必须有明确 Group，不能用弱化 Default Group 混过所有场景。

优先 Jakarta 原生注解。电话号码必须写 libphonenumber 解析、默认/允许地区、E.164 规范形式、有效性、非法错误和幂等规范化。只有证据证明原生注解、组合约束、Group、`ValidationUtils` 和成熟库都不能表达时，才允许自定义 Validator。

## 规则 3——对象构造与转换

- **简单对象：** 使用 Java `record`；需要规范化时明确设计紧凑构造器；方法内一次性结构可使用局部 `record`。
- **不可变非 Record：** 使用 Lombok `@Value`。
- **复杂对象：** 使用普通 Java 类，并把 `@Data`、`@NoArgsConstructor(access = AccessLevel.PROTECTED)`、`@AllArgsConstructor`、`@RequiredArgsConstructor`、`@Builder`、`@Accessors(chain = true)` 作为用户规定的强制完整基线。

Spec 必须计算该完整 Lombok 组合生成的构造器。`@RequiredArgsConstructor` 与其他构造器产生相同签名，或 ORM/Framework 禁止该组合时，必须阻断并找用户明确决策；禁止静默删除某个注解，禁止把复杂对象改名为简单对象绕过。

所有跨层转换使用 MapStruct/MapStructPlus。每个新增受影响 Converter 必须继承/实现 `egon-cola-component-common-core` 的 `BaseConverter<S,T>` 体系，写清泛型源/目标、Mapper 注解、生成实现、Bean 名、空值/默认/枚举/时间/敏感字段和测试。`BaseConverter` 无法表达时属于阻断型契约冲突；禁止用 Setter、`BeanUtils.copyProperties`、反射、JSON 往返或另造 Converter 抽象绕过。

## 规则 4——业务日志与 Spring 注入

清单所有受影响业务行为类，包括 Service、ServiceImpl、Component、Domain/Application Service、Handler、Strategy、Factory、Orchestrator、Schedule/Message Processor。

每个具体业务类必须 `@Slf4j`。每个 Spring Bean 必须在组件注解或 `@Bean` 中声明稳定显式名称。每个依赖字段必须 Final，类必须 `@RequiredArgsConstructor`，每个依赖字段必须 `@Qualifier("exactBeanName")`，即使当前只有一个实现也不能省略。禁止字段 `@Autowired`，禁止手写注入构造器。

Spec 必须核对适用 `lombok.config` 会把 `org.springframework.beans.factory.annotation.Qualifier` 复制到生成构造参数；同时设计日志事件、级别、稳定标识、失败上下文和敏感字段排除。

## 规则 5——封闭工具白名单

新增/触达代码只能使用：JDK 原生；`commons-lang3`、`commons-collections4`、`commons-io`、`commons-text`、`commons-codec`、`commons-beanutils`；Guava；有真实文档/内容识别需求时的 Apache Tika。其他工具依赖或重复 `*Utils` 阻断。允许 Commons BeanUtils 不代表允许其承担规则 3 的对象转换。

## 规则 6——外部 JSON 只用 Jackson

使用 Spring Boot 管理的 Jackson。每个受影响外部 DTO/VO/Request/Response/Command/Query/Event 都要逐项决定字段名、Include/Ignore、Unknown Field、枚举、日期时间、多态、敏感字段和兼容注解。“不需要注解”只有在逐字段证明 Jackson 默认行为与有效接口契约完全相同时才成立。禁止 Gson、Fastjson、混合 Mapper 和序列化对象转换。

## 规则 7——多环境配置结构一致

清单 Spring Boot Base 和全部环境配置文件。每个新增、重命名、删除 Key 都要提供 Profile Key 对齐矩阵。Value 可以不同，Key 层级和核心 Key 集合不能不同。优先带校验 `@ConfigurationProperties`，包含默认值、Metadata、秘密处理、兼容和 Profile 对齐测试/静态检查。只更新一个环境文件禁止 PASS。

## 规则 9——复杂业务强制设计模式

受影响业务逻辑复杂度与整份 Spec 的 Complexity 分开判定。存在真实变化轴、状态机、规则组合、算法选择、责任传递、非平凡对象族创建、事件协作或按类型/状态/场景重复变化分支时，就是复杂业务。

一旦判定复杂，必须完整设计至少一个 Strategy、Template Method、Factory、Chain of Responsibility、State、Specification、Domain Event 等合适模式，写清变化点、参与者、包/文件、依赖方向、注册/选择机制、扩展步骤、失败和测试。长 `if/else`、`switch`、类型/字符串判断、反射分发，或“直接实现更简单”都不能替代复杂业务模式。Simple 逻辑保持直接，不能为满足规则造模式类。

## 规则 10——只用 java.time

所有新增/实质修改日期时间字段、参数、持久化映射、JSON 字段、计算、Duration、Timeout 和 Clock 都使用 `java.time`。写清具体类型、时区、精度、Clock 来源、持久化表示、JSON 表达和转换边界。新增 `java.util.Date`、`Calendar`、`SimpleDateFormat` 不合格。

## 规则 11——只允许两种代码结构

分包/文件设计前必须证明项目准确属于：传统分层；或准确已选 `egon-cola-archetype` 与 Verifier。Spec 必须给出当前 Tree、所选形态、证据路径、允许依赖方向、目标 Tree，以及“不引入第三种/混合/改名/自创层模块”的明确结论。不符合两者就停止找用户，不能静默规范化。后续 Plan 必须在文件排序前再次确认。

## Spec 强制证据矩阵

每份 Java Spec 必须逐行包含规则 1、2、3、4、5、6、7、9、10、11，不能合并：

| 原文规则 | 是否受影响 | 仓库证据 | 准确设计决策 | 文件/类型/接口 | 验证/测试证据 | 状态/阻断 |
| --- | --- | --- | --- | --- | --- | --- |
| Rule 1 | Yes / No | `<路径与符号>` | `<强制命名决策>` | `<准确目标>` | `<搜索/编译/审核门禁>` | PASS / N/A / BLOCKED |

`No`/`N/A` 必须有正向仓库证据证明该关注点不在受影响面。
