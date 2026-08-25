# Java、Spring 与 Egon-COLA 编码规范

> 本文件是 `references/java-spring-egon-coding-standards.md` 的全中文审核镜像。每个 Java coding Spec 都必须读取。本规范约束本次新增或修改代码，不授权无关的存量代码大重构。

## 目录

- [优先级与触达代码边界](#优先级与触达代码边界)
- [仓库与能力发现](#仓库与能力发现)
- [允许的架构形态](#允许的架构形态)
- [Java 语义命名](#java-语义命名)
- [跨层参数校验](#跨层参数校验)
- [数据对象建模](#数据对象建模)
- [对象转换](#对象转换)
- [Spring Bean、依赖注入与日志](#spring-bean依赖注入与日志)
- [工具、JSON、时间与配置规则](#工具json时间与配置规则)
- [业务建模与设计模式](#业务建模与设计模式)
- [阻断型 Manual Check 目录](#阻断型-manual-check-目录)

## 优先级与触达代码边界

按以下顺序执行规则：

1. 用户最新明确要求和适用的 `AGENTS.md`；
2. 当前项目明确规范和已选 Egon-COLA Archetype 契约；
3. 本参考；
4. 只有不与上述规则冲突时才沿用附近存量代码风格。

发现存量代码违规时不能启动无关清理。本次新增文件和实质修改的符号必须在获批范围内尽可能符合规范。如果合规需要未声明的公开契约、模块移动、依赖、Migration 或大范围重构，必须记录阻断并申请批准，不能静默绕过。

## 仓库与能力发现

选择实现前必须：

1. 识别仓库根目录、受影响模块、构建清单、Java/Spring Boot 版本、依赖管理、`lombok.config`、多环境配置、测试和生成源码规则；
2. 按下方允许形态判定代码结构；
3. 搜索当前模块和 Egon-COLA 已有 Entity/PO、DTO/VO/BO、Query/Command/Event、Converter、Validator、Component、Repository/DAO、Gateway、Service/Domain Service、Exception、Result、Enum、工具、Starter 和基础设施；
4. 核对当前版本已经提供的 Spring/Spring Boot 官方能力；
5. 建立复用账本，写明候选项、准确路径/依赖、能力、适配或缺口和最终复用决策；
6. 检查现有与拟新增依赖以及其归属的 Starter/Component。

优先顺序固定为：

1. 当前项目已具备的 JDK 和 Spring/Spring Boot 能力；
2. 已有 Spring Boot Starter；
3. 已有 Egon-COLA Component 或公共基础设施；
4. 当前模块已有抽象；
5. 只有证据证明前四项无法满足时，才允许额外成熟依赖或自行实现。

新增依赖或重复抽象在 Spec 写清准确能力缺口、已检查候选及不足、版本/维护/安全/运维影响，并在影响重大时获得用户批准前，属于阻断项。不得本地重复实现 Spring 或 Egon-COLA 已有能力。

当前仓库证据包括：

- `egon-cola-component-common-core` 的 `BaseConverter<S,T>` 公共转换契约；
- `egon-cola-component-common-core` 的 Jakarta Validation 与 Group 工具 `ValidationUtils`；
- Egon-COLA Archetype/common core 中的 MapStructPlus 依赖与 Processor；
- 生成项目边界/Application 模块中的 `spring-boot-starter-validation`；
- Archetype `lombok.config` 中的 `lombok.copyableAnnotations += org.springframework.beans.factory.annotation.Qualifier`；
- `egon-cola-archetypes/egon-cola-archetype-{light,service,web}` 家族和对应 Open 变体。

每次都必须在当前基线重新核实这些路径，不能因本参考提到某组件就假设目标项目已经可用。

## 允许的架构形态

编码前必须从仓库证据中且只选择一种：

### 传统分层架构

沿用项目已有的 `biz.controller`、`biz.service`、嵌套的 `biz.service.impl`、`biz.dao`、`biz.config`、`biz.utils` 和语义化 `biz.domain`，保持 Controller -> Service 接口 -> `service.impl` -> DAO 方向。不得在该形态中加入 DDD/COLA 模块。

### Egon-COLA Archetype 架构

严格使用项目选中的 `egon-cola-archetypes` 模板和生成校验器。Light 使用 Archetype 的单项目包职责；Service/Web 使用 Archetype 规定的 `common`、`facade`、`domain`、`application`、`infrastructure`、`adapter`、`starter` 模块及依赖方向；Open 变体遵循其自身生成契约。不得自行解释通用 DDD/COLA、与 `biz.*` 混搭、重命名模块或新增 Archetype 不存在的层。

当前代码不符合两种形态，或无法确认所选 Archetype 时，必须在目标设计前停止，向用户展示实际 Tree、可选形态、迁移影响和建议，不能静默规范化仓库。

## Java 语义命名

所有类型按真实职责命名：

- 持久化载体使用 `*PO`；只有具备明确身份和生命周期语义的真实 ORM/DDD 实体才使用 `*Entity`。
- 业务计算载体使用 `*BO`；传输/集成载体使用 `*DTO`；展示输出使用 `*VO`。
- 读取意图使用 `*Query`；修改意图使用 `*Command`；已发生事实使用 `*Event`。
- HTTP/RPC 边界可使用明确的 `*Request`、`*Response`；仓库契约需要时分页使用 `*PageQuery`、`*PageResult`。
- 数据访问组件使用 `*DAO`；DAO 是访问组件，不是 POJO。
- 其他行为类型使用准确后缀，例如 `*Controller`、`*Service`、`*Repository`、`*Gateway`、`*Converter`、`*Validator`、`*Component`、`*Factory`、`*Strategy`、`*Exception`、`*Result`、`*Enum`。

禁止使用 `Data`、`Info`、`Param`、`Bean` 等含糊载体后缀来逃避语义选择。没有真实边界差异时，不能重复创建 PO/BO/DTO/VO/Query/Command/Event。范围外存量含糊名称不是重构授权；新增和实质修改类型必须纠正，无法纠正时报告冲突。

## 跨层参数校验

每个受影响的外部或跨层输入边界必须使用 `spring-boot-starter-validation` 提供的 Jakarta Bean Validation：

- 在 Request/DTO/Command/Query/Event 或方法参数上声明约束，嵌套对象使用 `@Valid`；
- 需要方法/边界校验时使用 Spring `@Validated`；
- 复用输入对象必须通过明确 Validation Group 区分操作，不能弱化约束或复制近似类；
- 优先 Jakarta 原生约束；显式/手动校验和 Group 调用使用 Egon-COLA 已有 `ValidationUtils`；
- 标准化数据只在一个命名边界归一化；电话号码相关变更使用 libphonenumber 等成熟方案完成解析、地区处理、规范化和有效性校验；
- 只有原生注解、组合约束、Group、`ValidationUtils` 和已批准成熟库都无法表达时，才允许自定义 `ConstraintValidator`，并说明缺口与测试；
- 写清校验顺序、规范化前后关系、错误映射、Null/Blank 语义、Group 选择和边界测试。

禁止手写重复的空值/范围/格式 Validator，也不能只校验 Controller 而信任其他跨层构造路径。

## 数据对象建模

按语义选择表示方式，不能套用统一注解堆：

- 简单不可变载体优先 Java `record`；需要确定性规范化或不变量时使用紧凑构造器；方法内部一次性结构可使用局部 `record`。
- 不可变对象优先 `record` 或 Lombok `@Value`，只能选择一套一致模型。
- 复杂可变、ORM/Proxy、框架实例化或生命周期丰富对象使用普通 Java 类。
- 可变类按构造和框架语义，从 `@Data`、`@NoArgsConstructor(access = AccessLevel.PROTECTED)`、`@AllArgsConstructor`、`@RequiredArgsConstructor`、`@Builder`、`@Accessors(chain = true)` 中选择必要项；这些是允许集合，不是必须全部叠加。
- 必须说明构造器可见性、必填字段、Builder/默认值、可变性、Equals/Hash、序列化、ORM/Proxy 和校验语义。
- 拒绝冲突/重复构造器注解，禁止把可变 `@Data` 与不可变 `@Value` 语义混用。

## 对象转换

跨层对象转换统一使用 MapStruct 或 MapStructPlus。受影响模块可依赖 `egon-cola-component-common-core` 且双向语义适配时，Converter 必须继承或实现已有 `BaseConverter<S,T>`，并写清准确 Converter 路径、生成实现和 Bean 名称。

规范化、派生字段、枚举/时间转换、敏感字段排除、默认值和空值语义放在命名 Converter 方法、MapStruct Mapping 或 Qualifier Helper 中。禁止在业务 Service 散布手工 `set/get`，禁止使用 `BeanUtils.copyProperties`、反射复制或 JSON 序列化完成映射。允许 Commons BeanUtils 作为工具依赖，不代表允许它承担业务对象转换。

转换确实单向或 `BaseConverter` 语义不兼容时，说明原因并采用仓库最接近的 Egon-COLA Converter 模式，不能伪造反向映射。

## Spring Bean、依赖注入与日志

- 每个具体业务行为类必须使用 Lombok `@Slf4j`，不能手写 Logger 字段。
- 每个 Spring 管理 Bean 都必须通过组件注解 Value 或 `@Bean` 方法声明稳定显式名称，例如 `@Service("userService")`、`@Component("orderComponent")`、`@Repository("userRepository")`、`@RestController("userController")`。
- 使用 Final 字段、构造器注入和 Lombok `@RequiredArgsConstructor`；禁止字段 `@Autowired`，禁止在业务代码中手写注入构造器。
- 每个注入依赖字段都使用 `@Qualifier("stableBeanName")`。必须核对 `lombok.config` 会把 `Qualifier` 复制到生成构造参数；否则补充正确 Copyable Annotation 配置或解决注入契约属于受影响设计，不能假设字段注解自动生效。
- 实现集合/Map 也必须由稳定限定的聚合 Bean 或明确获批 Registry 提供。
- 日志必须可操作、参数化且不包含秘密/敏感载荷；不能为了满足注解要求输出噪声式进出日志。

## 工具、JSON、时间与配置规则

### 工具

优先 JDK，其次使用已管理的 Apache Commons（`commons-lang3`、`commons-collections4`、`commons-io`、`commons-text`、`commons-codec`、`commons-beanutils`）或 Guava。Apache Tika 只有真实文档/内容识别需求时才能引入。不能为了少量方法增加其他工具库或重复 `*Utils`。`commons-beanutils` 仍禁止用于业务对象转换。

### JSON

统一使用 Spring Boot Jackson。外部 DTO/VO/Request/Response/Command/Query/Event 仅按真实协议需要使用 Jackson 注解，明确字段名、Include/Ignore、枚举、时间、多态、兼容和未知字段策略。禁止引入或混用 Gson、Fastjson，也禁止 JSON 往返转换对象。

### 日期时间

新增或实质修改代码使用 `LocalDate`、`LocalDateTime`、`Instant`、`OffsetDateTime`、`ZonedDateTime`、`Duration` 等 `java.time` 类型。禁止新增 `java.util.Date`、`Calendar`、`SimpleDateFormat`。边界必须定义时区、精度、Clock 来源、持久化类型和 JSON 表达。

### 配置

优先类型化 `@ConfigurationProperties`，禁止散落大量 `@Value`。新增/修改配置 Key 时，比较所有 Spring Boot 环境 Profile，保持相同结构和核心 Key 集合，允许 Value 不同。检查默认值、校验、Metadata、测试、秘密处理和兼容性。任一环境私自缺少或增加核心 Key 都是阻断项。

## 业务建模与设计模式

复杂业务不得堆积长 `if/else`、`switch`、类型判断和硬编码编排。识别真实变化维度、状态流转、规则组合、算法切换、责任链、对象创建和事件协作。

Strategy、Template Method、Factory、Chain of Responsibility、State、Specification、Domain Event 或其他仓库支持模式，只有能隔离当前变化/职责并改善测试和演进时才使用。必须写清变化点、模式、参与者、依赖方向、扩展机制和测试。简单逻辑应明确拒绝不必要模式。`Complex` 不是增加类数量的依据，“使用设计模式”也不是过度设计授权。

## 阻断型 Manual Check 目录

每份 Spec、Plan、每个执行 Step 和最终执行审核都必须使用以下稳定 ID。所有适用行必须为带具体证据的 `PASS`；不适用行必须为带证据与原因的 `N/A`。`FAIL`、`BLOCKED`、`UNKNOWN`、缺失证据或缺少 ID 都会阻断整体 PASS。

| Check ID | 阻断主题 |
| --- | --- |
| `MC-ARCH-001` | 已识别并保持唯一允许的架构形态 |
| `MC-REUSE-001` | 新设计前已检查 Spring、Spring Boot Starter、Egon-COLA Component/公共基础设施和模块内复用候选 |
| `MC-DEP-001` | 每个新增依赖或自研替代都有已证明能力缺口和获批影响；否则不新增 |
| `MC-NAME-001` | 新增/修改 Java 类型使用明确语义后缀，避免 `Data`/`Info`/`Param`/`Bean` |
| `MC-VALID-001` | 每个受影响跨层输入边界使用 Jakarta/Spring Validation、复用 Group、获批规范化和测试 |
| `MC-MODEL-001` | Record/Class/Lombok 选择符合可变性、构造、框架和不变量，且无注解冲突 |
| `MC-CONVERT-001` | MapStruct/MapStructPlus 和适用 Egon `BaseConverter` 负责映射，未使用禁止复制方案 |
| `MC-LOG-001` | 每个受影响具体业务类使用 `@Slf4j` 和安全可操作日志 |
| `MC-BEAN-001` | 每个受影响 Spring Bean 有稳定名称、构造器注入、`@RequiredArgsConstructor` 和带 Lombok 传播验证的 Qualifier 依赖 |
| `MC-UTIL-001` | 工具只使用获准 JDK/Commons/Guava/Tika，且不重复已有 Helper |
| `MC-JSON-001` | Jackson 是唯一 JSON 体系，外部契约只携带必要 Jackson 注解 |
| `MC-TIME-001` | 新增/修改时间建模使用 `java.time` 并明确边界语义 |
| `MC-CONFIG-001` | 所有环境配置保持等价 Key 结构，并按需使用类型化配置 |
| `MC-PATTERN-001` | 复杂变化使用有依据的模式，或直接逻辑有明确理由且无硬编码/过度设计 |
| `MC-SCOPE-001` | 触达代码合规且没有无关大重构；不可避免例外已明确并获批 |
| `MC-TEST-001` | 聚焦测试/静态门禁证明适用规范和保持行为 |
| `MC-BLOCKER-001` | 所有阻断和 Manual Check 失败已关闭，没有 `UNKNOWN`、静默例外或缺失证据 |

生成文档使用以下表格：

| Check ID | Applicability | Status | Evidence | Finding | Required action/exception |
| --- | --- | --- | --- | --- | --- |
| `MC-ARCH-001` | Applicable / Not applicable | PASS / N/A / FAIL / BLOCKED | `<路径、符号、依赖或命令证据>` | `<观察结果>` | `None / 准确下一步和负责人` |
