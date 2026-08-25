# Java、Spring 与 Egon-COLA Plan 规范

> 本文件是 `references/java-spring-egon-coding-standards.md` 的全中文审核镜像。每个 Java Plan 必须先读取 `references/user-mandated-java-rules.zh-CN.md`，再完整读取本文。逐字规则是绝对约束；本文增加仓库规划证据，不能弱化规则，也不授权重设计或无关重构。

## 规则优先级与触达范围

依次执行：用户最新明确要求和适用 `AGENTS.md`；当前项目明确约定与已选 Archetype；有效 Specs；本参考；最后才是与前述不冲突的附近旧代码风格。

每个新增文件和实质修改符号都要在获批范围内尽可能合规。范围外旧违规不是本次清理任务。合规若需要 Spec 未声明的公开契约、模块移动、依赖、Migration 或大范围重构，必须记录阻断并返回用户/Spec，不能静默豁免。

## Plan 前强制发现

分配文件和 Step 前必须：

1. 识别仓库根、受影响模块、Build Manifest、Java/Spring Boot 版本、依赖管理、`lombok.config`、生成源码、多环境配置和测试命令；
2. 把真实 Tree 判定为且只判定为一种允许架构；
3. 搜索当前模块和 Egon-COLA 已有 Entity/PO、BO/DTO/VO、Query/Command/Event、Converter、Validator、Component、Repository/DAO、Gateway、Service/Domain Service、Exception、Result、Enum、工具、Starter 和基础设施；
4. 核对当前 Spring/Spring Boot 版本已经提供的能力和解析后的依赖；
5. 建立复用账本，逐项记录候选、准确路径/依赖、能力、适配/缺口、决策和所属 Step；
6. 对每个额外依赖或自研替代证明需要、版本归属、维护/安全/运维影响和批准情况。

固定优先级：现有 JDK/Spring/Spring Boot；已有 Starter；已有 Egon-COLA Component/公共基础设施；模块内抽象；最后才是获批成熟依赖或自研。不得重复实现 Spring/Egon-COLA 已有能力。

必须在当前基线重新核实。候选包括 common core 的 `BaseConverter<S,T>`、`ValidationUtils`、MapStructPlus、`spring-boot-starter-validation`、Archetype `lombok.config` 的 Qualifier 传播以及 light/service/web 与 Open Archetype。

## 只允许两种架构

- **传统分层**：保持 `biz.controller`、`biz.service`、嵌套 `biz.service.impl`、`biz.dao`、`biz.config`、`biz.utils` 和语义化 `biz.domain`，依赖方向为 Controller -> Service 接口 -> `service.impl` -> DAO。
- **精确 Egon-COLA Archetype**：严格遵循已选 Archetype 的生成 Tree、Verifier 和依赖；Light 使用单工程职责，Service/Web 使用 `common`、`facade`、`domain`、`application`、`infrastructure`、`adapter`、`starter`，Open 变体遵循自身契约。

禁止第三种架构、通用 DDD 猜测、`biz.*` 与 COLA 混搭、模块改名或新增层。真实 Tree 不符合两者或证据冲突时，Plan 必须阻断并找用户决策。

## 逐文件规划规范

每个受影响 Java 文件块都必须在 `Standards impact` 列适用 `MC-*`，在 `Literal rule enforcement` 列原始 `Rule N`，并用仓库证据、伪代码和验证落实。

### 语义命名

持久化载体 `*PO`；只有真实身份/生命周期语义才用 `*Entity`；业务载体 `*BO`；传输 `*DTO`；展示 `*VO`；读意图 `*Query`；写意图 `*Command`；事实 `*Event`；协议边界可用 `*Request`/`*Response`；既有分页契约可用 `*PageQuery`/`*PageResult`；访问组件为 `*DAO`。行为类型使用准确职责后缀。

禁止新增载体使用 `Data`、`Info`、`Param`、`Bean`。没有真实边界或生命周期差异时不能每层造一个类。

### 跨层校验与规范化

每个受影响层间输入交接都使用 `spring-boot-starter-validation` 的 Jakarta Bean Validation；只规划 Controller 校验不合格。分别规划外部 -> Controller/Adapter、Controller/Adapter -> Service/Application、Service/Application -> Domain Service/Component、Service/Application -> DAO/Repository/Gateway、Event/Job/内部重入，写 `@Valid`、`@Validated`、准确 Group、`ValidationUtils`、规范化、错误和测试。只有有效 Spec 批准已证明注解/Group/成熟库缺口时，才允许自定义 `ConstraintValidator`。

电话号码在一个命名边界使用 libphonenumber 等成熟方案，明确地区、规范格式、有效性、错误映射和测试；依赖缺失时必须先通过 Spec/依赖门禁。

### 对象建模与转换

- 简单不可变载体优先 `record`，规范化/不变量用紧凑构造器，方法内临时结构可用局部 `record`；
- 其他不可变对象优先 Lombok `@Value`；
- 复杂对象使用普通类，并完整使用 `@Data`、Protected `@NoArgsConstructor`、`@AllArgsConstructor`、`@RequiredArgsConstructor`、`@Builder`、`@Accessors(chain = true)` 强制基线；
- 计算全部生成构造器；重复签名/框架冲突阻断 Plan，不能静默删除注解。

跨层转换使用 MapStruct/MapStructPlus；每个新增受影响 Converter 必须实现/继承 Egon `BaseConverter<S,T>`。规划准确泛型、字段、空值、默认、枚举、时间、敏感字段、生成实现、Bean 名和测试。禁止手工 `set/get`、`BeanUtils.copyProperties`、反射、JSON 往返和单向/本地 Converter 绕过。`BaseConverter` 无法表达时阻断并返回 Spec/用户。

### Spring Bean、注入与日志

- 受影响具体业务类使用 `@Slf4j`，日志参数化且不泄密；
- 每个 Spring Bean 在组件注解或 `@Bean` 中声明稳定显式名称；
- Final 字段加 Lombok `@RequiredArgsConstructor`，禁止字段 `@Autowired` 和业务手写注入构造器；
- 每个依赖字段使用 `@Qualifier("stableBeanName")`；
- 核对 `lombok.config` 会把 Qualifier 复制到生成构造参数，不具备时在范围内规划修复。

### 工具、JSON、时间与配置

- 工具优先 JDK，其次已管理 Commons（lang3、collections4、io、text、codec、beanutils）或 Guava；Tika 仅真实内容识别；禁止重复 Helper 和额外工具库；BeanUtils 不得用于业务转换；
- JSON 只用 Spring Boot Jackson；外部 DTO/VO/Request/Response/Command/Query/Event 只按协议需要使用 Jackson 注解；禁止 Gson/Fastjson 与 JSON 映射技巧；
- 新增/触达时间建模只用 `java.time`，明确时区、精度、Clock、持久化和 JSON；禁止新增 `Date`、`Calendar`、`SimpleDateFormat`；
- 配置优先带校验的 `@ConfigurationProperties`；新增/修改 Key 必须同步所有环境 Profile 的结构和核心 Key 集合，Value 可不同。

### 业务建模与模式

分类受影响业务 Flow。每个 Complex Flow 必须规划实际且符合仓库的 Strategy、Template Method、Factory、Chain of Responsibility、State、Specification、Domain Event 或其他合适模式，写参与者、文件、依赖顺序、注册/选择、编排、失败和测试。Complex 逻辑不能保留直接 `if/else`、`switch`、类型/字符串/反射分发。只有 Simple 逻辑可直接实现，且不能制造仪式性模式。

## Step Manual Check 契约

每个 Step 写 `Manual Checks: <IDs>`，每个文件写 `Standards impact: <IDs 与准确影响>`。建议 Step commit 前必须规划出让所有适用项 PASS 的证据；已知违规不能推给未指定的后续清理。

最终 Plan 必须逐项包含以下 ID：

| Check ID | 阻断主题 |
| --- | --- |
| `MC-ARCH-001` | 只识别并保持一种允许架构 |
| `MC-REUSE-001` | 已先检查 Spring、Starter、Egon-COLA 和模块内复用 |
| `MC-DEP-001` | 新依赖/自研有已证明缺口和批准，否则不新增 |
| `MC-NAME-001` | 类型名语义明确且无含糊载体后缀 |
| `MC-VALID-001` | 跨层输入使用 Validation、Group、获批规范化和测试 |
| `MC-MODEL-001` | 准确规划 Record/`@Value`/复杂类完整 Lombok 基线，否则构造器/框架冲突阻断 |
| `MC-CONVERT-001` | MapStruct/MapStructPlus 与强制 Egon `BaseConverter` 负责每个新增受影响 Converter |
| `MC-LOG-001` | 业务类使用 `@Slf4j` 和安全日志 |
| `MC-BEAN-001` | Bean 有稳定名称、Lombok 构造注入、Qualifier 和传播校验 |
| `MC-UTIL-001` | 只用获准工具且不新增重复 Helper |
| `MC-JSON-001` | Jackson 是唯一 JSON 体系且注解符合协议 |
| `MC-TIME-001` | 时间使用 `java.time` 并明确边界语义 |
| `MC-CONFIG-001` | 所有环境保持等价配置 Key 结构 |
| `MC-PATTERN-001` | 每个 Complex 业务 Flow 有具体模式；只有 Simple 逻辑保持直接 |
| `MC-SCOPE-001` | 触达代码合规且无无关大重构，例外明确 |
| `MC-TEST-001` | 聚焦测试/静态门禁证明每项适用规范 |
| `MC-BLOCKER-001` | 所有阻断/失败已关闭，无未知或静默例外 |

生成文档使用准确六列表：

| Check ID | Applicability | Status | Evidence | Finding | Required action/exception |
| --- | --- | --- | --- | --- | --- |
| `MC-ARCH-001` | Applicable / Not applicable | PASS / N/A / FAIL / BLOCKED | `<路径、符号、依赖或规划命令证据>` | `<结论>` | `None / 准确动作和负责人` |

整体 PASS 要求所有适用项 `PASS`，所有不适用项有证据地 `N/A`。缺 ID/证据、`FAIL`、`BLOCKED`、`UNKNOWN` 或未关闭例外一律禁止 PASS。
