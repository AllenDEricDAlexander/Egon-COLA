# Java、Spring 与 Egon-COLA 执行规范

> 本文件是 `references/java-spring-egon-coding-standards.md` 的全中文审核镜像。第一个 Java Step 前必须先读取 `references/user-mandated-java-rules.zh-CN.md`，再读取本文；每个 Step 门禁和最终审核都要应用。逐字规则是绝对约束，本文只能补执行证据，不能弱化，也不授权无关清理。

## 优先级与停止边界

依次执行：用户最新明确要求和适用 `AGENTS.md`；当前项目明确约定与已选 Archetype；有效 Specs 与已批准 Plan；本参考；最后才是兼容的附近旧风格。

合规如果需要未声明契约、模块移动、依赖、Migration、文件或大范围重构，应把 Step 停为 Plan/Spec 漂移。禁止静默豁免、发明目录，或先提交已知违规再说以后清理。

## 编码前架构与复用检查

第一个 Step 前以及 Step 首次触达新模块时：

1. 检查真实 Tree、Build Manifest、Java/Spring Boot 版本、依赖管理、`lombok.config`、生成源码、测试和全部环境 Profile；
2. 根据证据且只识别一种允许架构：传统 `biz.controller`、`biz.service`、嵌套 `biz.service.impl`、`biz.dao`、`biz.config`、`biz.utils`、`biz.domain`；或准确的 light/service/web/Open Archetype Tree、Verifier 与依赖方向；
3. 搜索模块和 Egon-COLA 已有 Entity/PO、BO/DTO/VO、Query/Command/Event、Converter、Validator、Component、Repository/DAO、Gateway、Service/Domain Service、Exception、Result、Enum、工具、Starter 和基础设施；
4. 对照当前路径和解析依赖重新验证 Plan 复用账本；
5. 优先 JDK/Spring/Spring Boot，再选已有 Starter、Egon-COLA Component/公共设施或模块内抽象；
6. 只有有效 Spec/Plan 已记录当前能力缺口、候选、版本归属、维护/安全/运维影响和必要批准时，才允许额外成熟依赖或自研。

禁止混合架构或重复能力。必须重新核实 common core `BaseConverter<S,T>`、`ValidationUtils`、MapStructPlus、Starter Validation 和 Archetype Qualifier 传播等候选。

## 触达代码执行规则

### Java 语义命名

新增/实质修改载体使用准确 `*PO`、`*BO`、`*DTO`、`*VO`、`*Query`、`*Command`、`*Event`、协议 `*Request`/`*Response` 和既有 `*PageQuery`/`*PageResult`；只有真实身份/生命周期才用 `*Entity`。`*DAO` 是访问组件；行为类型使用准确职责后缀。

禁止新增 `Data`、`Info`、`Param`、`Bean` 含糊载体。没有真实边界/生命周期差异时不能每层造类。

### 校验与规范化

每个受影响层间输入交接使用 `spring-boot-starter-validation` 的 Jakarta Bean Validation；只校验 Controller 失败。分别检查外部 -> Controller/Adapter、Controller/Adapter -> Service/Application、Service/Application -> Domain Service/Component、Service/Application -> DAO/Repository/Gateway、Event/Job/内部重入，包含 `@Valid`、`@Validated`、准确 Group、`ValidationUtils`、错误和测试。

电话号码在一个命名边界使用 libphonenumber 等成熟方案处理地区、规范化和有效性。只有证据证明注解、组合、Group、`ValidationUtils` 和成熟库都无法满足时，才允许获批自定义 `ConstraintValidator`。

必须验证约束/Group 选择、规范化顺序、Null/Blank、错误映射和聚焦负例。

### 对象模型与转换

- 简单不可变载体优先 `record`；紧凑构造器可做确定性规范化/不变量；方法内临时结构可用局部 `record`；
- 其他不可变对象优先 Lombok `@Value`；
- 复杂对象使用普通类并完整使用强制 `@Data`、Protected `@NoArgsConstructor`、`@AllArgsConstructor`、`@RequiredArgsConstructor`、`@Builder`、`@Accessors(chain = true)` 基线；
- 必须编译完整基线；构造器/框架冲突会阻断，不能静默删除注解。

跨层转换用 MapStruct/MapStructPlus，每个新增受影响 Converter 必须实现/继承 Egon `BaseConverter<S,T>`。核对泛型、空值、默认、枚举、时间、敏感字段、生成实现、Bean 和测试。禁止 Setter/Getter、`BeanUtils.copyProperties`、反射、JSON 和单向/本地 Converter 绕过；契约冲突会阻断。

### Spring Bean、日志与注入

- 具体业务类使用 Lombok `@Slf4j`，日志参数化、可操作、不泄密；
- 每个 Spring Bean 有稳定显式组件/`@Bean` 名；
- Final 字段配合 Lombok `@RequiredArgsConstructor`，禁止字段 `@Autowired` 和业务手写注入构造器；
- 每个依赖字段使用 `@Qualifier("stableBeanName")`；
- 检查 `lombok.config` 会把 Qualifier 复制到生成构造参数，不能假设字段注解自动生效。

### 工具、JSON、时间与配置

- 工具顺序：JDK、已管理 Commons（lang3、collections4、io、text、codec、beanutils）、Guava；Tika 仅真实内容识别；禁止重复 Utils 和随意增加库；BeanUtils 不得用于业务转换；
- JSON 只用 Spring Boot Jackson；外部 DTO/VO/Request/Response/Command/Query/Event 按协议使用必要 Jackson 注解；禁止 Gson/Fastjson 或序列化映射；
- 新增/触达时间只用 `java.time`，明确时区、精度、Clock、持久化和 JSON；禁止新增 `Date`、`Calendar`、`SimpleDateFormat`；
- 配置优先带校验 `@ConfigurationProperties`；Key 变化时比较全部环境 Profile，保持相同结构/核心 Key，Value 可不同。

### 复杂业务行为

分类每个触达业务 Flow。每个 Complex Flow 必须实现 Spec/Plan 已选 Strategy、Template Method、Factory、Chain of Responsibility、State、Specification、Domain Event 等模式的实际参与者、选择/接线、编排、失败和测试。Complex 逻辑禁止直接 `if/else`、`switch`、类型/字符串/反射分发。Simple Flow 保持直接，禁止仪式性模式类。

## 每 Step 阻断型 Manual Check

锁定 Step 时，把 Plan 适用 ID 抄入 Step Manual Check 表；当前证据显示影响新关注点时必须补 ID，遗漏不代表豁免。每项分别记录 Applicability、Status、Evidence、Finding、Required action/exception。

提交前：

- 每个适用行必须有具体 Diff/路径/符号/命令证据且为 `PASS`；
- 真实不适用行必须有证据与原因且为 `N/A`；
- `FAIL`、`BLOCKED`、`UNKNOWN`、缺失 ID/证据或未关闭例外会让 Step 保持 `In Progress`/`Blocked`，禁止按完成提交；
- 每个 Coding Step 的 `MC-SCOPE-001` 与 `MC-TEST-001` 都适用；
- `MC-BLOCKER-001` 汇总全部未关闭行。

使用 `references/step-gate-checklist.md` 的六列表。Static Search/Test 通过也不能替代人工逐项复核。

## 稳定 Manual Check 目录

| Check ID | 阻断主题 |
| --- | --- |
| `MC-ARCH-001` | 保持唯一允许架构 |
| `MC-REUSE-001` | 新增前已检查 Spring/Starter/Egon/模块复用 |
| `MC-DEP-001` | 新依赖/自研有已证明获批缺口，否则未新增 |
| `MC-NAME-001` | 触达类型语义明确且无含糊载体后缀 |
| `MC-VALID-001` | 跨层输入使用 Validation、Group、规范化和测试 |
| `MC-MODEL-001` | 已使用 Record/`@Value`/复杂类完整 Lombok 基线，否则冲突阻断 |
| `MC-CONVERT-001` | MapStruct/MapStructPlus 与强制 Egon `BaseConverter` 负责每个新增受影响 Converter |
| `MC-LOG-001` | 触达业务类使用 `@Slf4j` 和安全日志 |
| `MC-BEAN-001` | Bean 有名称、Lombok 构造注入、Qualifier 和传播校验 |
| `MC-UTIL-001` | 只用获准工具且无重复 Helper |
| `MC-JSON-001` | Jackson 是唯一 JSON 体系且契约注解正确 |
| `MC-TIME-001` | 时间使用 `java.time` 并明确语义 |
| `MC-CONFIG-001` | 所有环境 Profile 保持等价配置 Key |
| `MC-PATTERN-001` | 每个 Complex 业务 Flow 实现获批模式；只有 Simple Flow 保持直接 |
| `MC-SCOPE-001` | 触达代码合规且无无关大重构 |
| `MC-TEST-001` | 聚焦测试/静态检查证明适用规范与行为 |
| `MC-BLOCKER-001` | 所有阻断/失败已关闭且无静默例外 |

## 最终审核规则

全部 Step 提交后，针对最终 Tree 与 Commit 重新逐项执行 17 项检查，不能只汇总 Step 断言。最终 PASS 要求所有适用项 PASS、所有 N/A 有证据。任何 Manual Check 失败、任何 `Partial`/`Not satisfied` Spec 要求或缺失强制运行时证据都产生 `PARTIAL`；无法确定基线/证据则 `BLOCKED`。只报告缺口，最终审核中不能静默实施。
