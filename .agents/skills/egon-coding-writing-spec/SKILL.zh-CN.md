---
name: egon-coding-writing-spec
description: 当编码任务在实施计划之前需要基于仓库现状编写需求与用例分析、系统架构、概要设计、详细设计、复杂跨模块分析、完整展开的接口契约、Mermaid ER/数据库设计、RFC 风格规格或获批设计基线时使用。当前 Java 分包设计只支持传统三层结构：biz.controller、biz.service、嵌套的 biz.service.impl、biz.dao、biz.config、biz.utils 和 biz.domain。
---

# EGON 编码 Spec 编写

> 本文件是 `SKILL.md` 的全中文审核镜像，不是 Codex 的运行入口。实际开发使用英文 `SKILL.md`；修改任一版本时必须同步另一版本，确保语义一致。

## 目的

把编码需求和当前仓库状态转化为可审核的系统架构、概要设计或详细设计 Spec。在编写实施 Plan 或修改代码之前，将文档保存到 `docs/egon/spec`。

Spec 定义的是**必须构建什么，以及该设计为什么自洽**。它不是实施顺序，不能开始编码。

## 适用范围与输出契约

- 仅用于已有仓库或已初始化仓库中的编码工作。
- 每一项重要技术结论都必须来自当前仓库、用户明确决策或被引用的前置 Spec。
- 只生成或修订 Spec 及关系元数据；不能生成 Plan、修改生产代码、执行迁移或启动项目。
- Spec 使用用户指定的自然语言；用户未指定时，跟随用户和仓库附近文档使用的语言。源码标识符、符号、路径、schema 和协议名保持原样。
- 新 Spec 固定保存为 `docs/egon/spec/YYYY-MM-DD-HH-MM-ABSTRACT.md`。
  - 创建时间使用用户/仓库本地时区。
  - `ABSTRACT` 替换为简洁的小写 ASCII kebab-case 摘要，通常为 3–8 个单词。
  - 示例：`docs/egon/spec/2026-08-15-14-30-account-lockout-design.md`。
  - 同一分钟且摘要相同时不得覆盖旧文档，应使用更具体的摘要。
- 必须以 `assets/spec-template.md` 中的 Template Version 2 为模板，保留所有编号章节。不适用的章节写 `N/A`，并给出仓库证据和原因。

## 不可违反的规则

1. 先定位仓库根目录，并读取所有适用的 `AGENTS.md`。
2. 检查相关构建清单、模块、源码包、迁移、测试、前端、配置、文档和已有 `docs/egon/spec`。如果旧设计目录可能包含有效决策，也必须搜索。
3. 识别真实的编程语言、版本、框架、模块边界、分包习惯、架构风格、错误模型、持久化策略、迁移机制、前端栈和测试工具。除非 Spec 明确论证变更理由，否则沿用已有模式。
4. 把原始需求拆成稳定且原子的 `REQ-001`、`REQ-002`……，每项都包含可观察的验收标准和来源。
5. 不能擅自决定重大歧义，也不能静默修补重大需求或设计缺陷。应给出仓库证据、影响、可选方案和推荐意见，在定案受影响章节前请用户决策。
6. 小型、本地、可逆缺口采用最小且符合仓库惯例的推断。对有实际影响的推断，以 `ASM-*` 记录证据和错误影响；普通命名、放置和格式问题不要打断用户。
7. 所有 Spec 使用相同元数据 Header。通过 RFC 风格的 `Amends`、`Supersedes`、`Depends On` 和 `Related Specs` 链接保证有效设计可追溯。
8. 不得静默修改已获批前置 Spec 的规范正文。应新建后续 Spec，指出被修改的文档和精确章节；仓库规范允许时，只为旧文档补充反向关系元数据。
9. 后续 Spec 可以补齐或纠正旧设计，但必须说明它只修订哪些章节，或在哪个范围内完整替代旧 Spec；未被修改的旧章节继续有效。
10. 未经用户或决策负责人明确批准，不能标记为 `Accepted`。内部完整、等待审核时为 `Review`；仍有重大未决项时必须为 `Draft`，结论为阻塞。
11. 所有适用层次都要达到详细设计深度：精确路径/包、符号、契约、字段、状态规则、schema、页面状态、测试用例、兼容与失败语义。不要编写完整生产实现。
12. 交付前必须对照原始用户需求和当前仓库复核。内部缺陷自行修复，只把仍未解决的重大决策交给用户。
13. Java 对象必须按照真实边界和生命周期职责分类。遵循 `references/pojo-modeling.zh-CN.md`；不能把 POJO/PO/DO/DTO/VO/BO/Entity/Query/Command/Request/Response/Form/Param/PageQuery/PageResult 当成必须并列创建的类清单。
14. 防止类爆炸。每个独立对象和 Mapper 都必须有具体语义依据。PO/ORM Entity 只有在仓库惯例和生命周期依据充分时才能继承；具体业务 Service 默认采用组合与委托，不采用继承。
15. Java 分包设计必须读取 `references/three-layer-architecture.zh-CN.md`，并只使用当前已规范的传统三层结构：`biz.controller`、`biz.service`、`biz.service.impl`、`biz.dao`、`biz.config`、`biz.utils` 和 `biz.domain`。本 skill 明确扩展前，不得设计 DDD 或 COLA 分包。现有仓库采用其他架构时，应保持现状，并在提出结构迁移前询问用户。
16. 必须使用 `references/complex-scenario-analysis.zh-CN.md` 把 Spec 分类为 `Simple` 或 `Complex`。Complex Spec 在选择架构前必须完成证据图谱、场景矩阵、所有权/一致性分析、质量约束和“证据到决策”结论链；Simple Spec 不得为了形式套用重型分析。
17. 第 7 章必须分为系统架构设计、概要设计和详细设计。Complex Spec 必须包含架构 Mermaid Flowchart、独立的关键业务/控制 Flowchart，以及覆盖主要参与者和重要失败行为的 Mermaid 泳道图/Sequence Diagram。
18. 存在接口时必须读取 `references/interface-contract-design.zh-CN.md`。一个原子 Method + URL 或协议操作分配一个接口 ID，禁止把 CRUD 接口族合并为一个 ID；先列清单，再逐个展开，写清准确 URL 或协议符号、完整入参规则、真实成功/错误载荷、面向前端的逻辑、兼容与验证。HTTP JSON 示例使用仅供文档的 `jsonc`，每个字段必须有行尾含义注释。
19. 使用或修改持久化数据时必须读取 `references/database-design.zh-CN.md`。每张受影响表和每个索引都要先列清单再展开，包括完整字段语义、真实查询/访问路径、索引顺序依据、Migration/历史数据、事务、锁、兼容、验证和回滚/Forward Fix 边界。
20. 每份 Spec 都必须读取 `references/requirements-use-case-analysis.zh-CN.md`。需求分析必须识别真实参与者和稳定 `UC-*` 用例，写清触发、前置条件、主要结果、分支/失败、后置条件和追踪。可使用完整表格或 Mermaid `flowchart`；复杂或多参与者行为优先使用带系统边界的 Mermaid 视图。不能把 Controller 方法或架构调用链当作用例。
21. 读取、新建或修改关系型表时，必须增加 Mermaid `erDiagram`，覆盖清单中的每张表、直接相关邻表、真实基数、关系标签和重要 PK/FK/UK 字段。渲染安全 Entity 名称要映射到准确物理表。ER 图用于补充而不是替代逐表、逐字段、逐索引、Migration 和事务设计。

## 强制参考资料加载与分阶段写作

不能根据 `SKILL.md` 或生成 Spec 的行数判断是否完整。本文件是工作流入口，详细操作规则位于 references 和输出模板中。应根据证据密度、契约完整度、跨章节一致性，以及另一位工程师能否在不自行发明设计决策的情况下实施来判断质量。

编写对应章节前，必须**完整读取**所有适用参考资料，不能只依赖本文件中的简述：

| 场景 | 必须完整读取的参考资料 |
| --- | --- |
| 所有 Spec | `references/ambiguity-policy.md`、`references/rfc-governance.md`、`references/complex-scenario-analysis.zh-CN.md`、`references/requirements-use-case-analysis.zh-CN.md`、`references/review-checklist.md` 和 `assets/spec-template.md` |
| Java 设计 | `references/three-layer-architecture.zh-CN.md` 和 `references/pojo-modeling.zh-CN.md` |
| 任意 HTTP/RPC/事件/Job/内部契约 | `references/interface-contract-design.zh-CN.md` |
| 任意持久化读写或 Schema 依赖 | `references/database-design.zh-CN.md` |

Complex Spec 必须显式执行四轮工作，并把分析结果保留在 Spec 中，不能压缩成摘要：

1. **发现轮**——收集准确仓库证据、现有调用链、消费者、数据存储、配置、测试和前置决策；区分证据、推断和未验证运行时行为。
2. **场景轮**——枚举实际适用的成功、分支、校验、权限、空数据、重复、并发、超时、部分失败、回滚、重试和恢复路径。
3. **设计轮**——确定所有权、契约、数据流、事务/一致性边界、失败语义、兼容、文件、模型、Schema、前端状态和测试；每项重要决策都从证据推导。
4. **一致性轮**——逐字段、逐状态对比需求、图示、接口、POJO、表/索引、页面、测试、发布和追踪矩阵；校验前修复矛盾。

最低深度是结构要求，不是凑字数：

- Complex Spec 至少包含两条证据/现有链路、三个实质不同场景、三个适用质量/约束项和两条“证据到决策”结论链；如果真实项目确实更少，必须在对应小节写 `Depth exception:` 并给出证明元素更少的仓库依据；
- 每份 Spec 必须使用完整表格或 Mermaid 用例视图表达有证据的参与者和稳定 `UC-*` 目标，并写清条件、结果、后置条件和向后追踪；
- 每个接口清单项必须包含全部逐接口子章节、真实协议身份、完整参数规则、成功与错误结果、有序调用方逻辑和验证；
- 每张表清单项必须包含全部逐表子章节、完整受影响字段表、绑定真实访问路径的逐索引论证、Migration/历史数据处理和一致性/恢复规则；
- 每张关系型清单表必须出现在 Mermaid `erDiagram` 中，物理名称映射、关系与 Key 语义必须和详细设计一致；
- `N/A`、“沿用现有”、“框架处理”、类名或链接只有在 Spec 引用准确权威实现/契约，并说明无需新决策的原因时，才能代替详细内容。

## 歧义与决策边界

提问前读取 `references/ambiguity-policy.md`。

如果错误选择会显著改变业务行为、范围、公共 API/RPC/事件契约、职责边界、持久化数据、迁移、安全/权限/租户、财务正确性、一致性/并发、技术选型、部署拓扑、外部依赖、兼容性或不可逆操作，必须找用户确认。

一次检查发现多个相关重大问题时，应把问题、选项和后果合并成一组，让用户一次完成一致决策。仓库能回答的问题不要反复打断用户。

只有当缺口是本地的、可逆的、不改变已决定契约的外部可见语义，而且有明确仓库惯例支撑时，才可自行推断。

## RFC 风格元数据与关系

所有 Spec 使用以下完全一致的 Header 字段：

| 字段 | 必需含义 |
| --- | --- |
| Document | 当前文件名，使用仓库相对链接或代码值 |
| Template Version | 当前模板固定为 `2` |
| Status | `Draft`、`Review`、`Accepted`、`Implemented`、`Superseded` 或 `Rejected` |
| Type | `Feature`、`Refactor`、`Bugfix`、`Architecture` 或清晰定义的其他编码类型 |
| Complexity | `Simple` 或 `Complex` |
| Complexity Drivers | 实质交互/决策风险驱动因素；Simple Spec 写 `None` |
| Created | `YYYY-MM-DD HH:mm ZONE` |
| Updated | `YYYY-MM-DD HH:mm ZONE` |
| Owner | 用户、团队或决策负责人 |
| Repository | 仓库名 |
| Scope | 受影响模块或限界上下文 |
| Source Requirement | 用户请求、Issue、工单或已链接的需求简述 |
| Baseline Revision | Git commit/branch，或明确的未提交工作树快照 |
| Amends | 被部分修改的旧 Spec 链接和精确章节，或 `None` |
| Supersedes | 被替代的旧 Spec 链接和替代范围，或 `None` |
| Depends On | 规范性依赖文档链接和精确章节，或 `None` |
| Related Specs | 非规范性上下文链接，或 `None` |
| Related Plans | 实施本 Spec 的 Plan 链接，或 `None` |

关系目标可以是仓库中的任意设计文档；仍有权威性的旧文档即使不在 `docs/egon/spec` 下也可以引用。必须使用相对 Markdown 链接和精确章节号/锚点。有效设计按以下顺序解析：

1. 从被引用的基础设计开始。
2. 排除当前范围内已被替代的内容。
3. 按时间顺序应用已接受的修订。
4. 纳入规范性依赖。
5. 如果已接受文档发生冲突且没有治理关系，停止并请用户决策。

生命周期和反向链接规则见 `references/rfc-governance.md`。

## 必需工作流

1. **还原需求并判定复杂度**
   - 准确引用或转述原始目标、约束、排除项和成功标准。
   - 建立 `REQ-*` 清单并识别缺失决策。
   - 执行 `references/requirements-use-case-analysis.zh-CN.md`；识别有证据的参与者，把行为需求映射为 `UC-*` 目标、流程、结果与后置条件。
   - 执行 `references/complex-scenario-analysis.zh-CN.md`，记录 `Simple`/`Complex` 和具体驱动因素。
2. **检查仓库**
   - 记录真实文件、符号、消费者、调用链、schema、页面、测试和构建命令。
   - 区分仓库/静态证据、推断和未经验证的运行时结论。
3. **解析设计历史**
   - 搜索当前和旧 Spec 位置。
   - 判断本 Spec 是全新、修订、替代、依赖还是仅相关。
4. **处理歧义**
   - 受影响章节定案前询问重大决策。
   - 推断小缺口并记录有影响的假设。
5. **分析复杂场景**
   - Complex Spec 在选择架构前必须完成证据/现有链路图谱、场景矩阵、边界/数据所有权、质量属性、关键失败和结论证据链。
   - Simple Spec 说明轻量路径足够的原因，不得虚构复杂度。
6. **设计方案**
   - 至少评估一个直接遵循仓库惯例的方案，以及任何实质不同且可行的替代方案。
   - 明确考虑 Strategy、Template Method、Factory、Adapter、Facade、State、Observer、Command、Specification 等合适模式。
   - 只有模式确实解决变化点、耦合、生命周期、编排或可测试性问题时才采用；否则记录为什么直接设计更清晰且避免过度设计。
   - Java 工作必须阅读 `references/three-layer-architecture.zh-CN.md` 和 `references/pojo-modeling.zh-CN.md`。确认传统三层架构适用门禁，保持 `impl` 位于 `service` 下，按语义职责分类每个拟议对象，执行类必要性检验，并分别评估持久化继承与 Service 组合。
   - 对应章节适用时读取 `references/interface-contract-design.zh-CN.md` 和 `references/database-design.zh-CN.md`。
   - 存在关系型持久化时，先根据已有表所有权、Key 和基数推导 Mermaid `erDiagram`，再定案逐表详情。
7. **编写 Spec**
   - 复制 `assets/spec-template.md`，用仓库真实内容填满所有章节。
   - 可使用精确签名、字段表、状态转换、文件树和伪代码阐明设计，但不能写生产级完整方法体。
   - 包含选定的用例产物；关系型持久化适用时必须包含 Mermaid ER 图。
   - 每个接口、每张受影响表和每个索引都必须展开，不能停留在清单层面。
8. **复核并修复**
   - 执行 `references/review-checklist.md`。
   - 修复遗漏、矛盾、过期路径、含糊占位、追踪断点和无依据的范围膨胀。
9. **校验并交付**
   - 运行 `scripts/validate_spec.py <spec-path> --strict`。
   - 报告路径、状态、前置关系、假设和未决用户决策。
   - 停止并等待用户审核。只有用户明确要求针对该 Spec 编写 Plan 且必要审批门禁已满足后，才进入 Plan。

## 必需章节与设计深度

模板具有规范性。Spec 至少包含：

1. **摘要**——问题、选定方向、影响范围和预期结果。
2. **背景与现状**——真实行为、调用链、现有消费者、仓库证据和差距。
3. **目标与非目标**——明确控制范围。
4. **需求、验收标准与用例分析**——原子化 `REQ-*`、可观察结果以及有证据的参与者和 `UC-*` 目标。使用完整用例表或 Mermaid `flowchart`，说明触发、前置条件、主/分支/失败结果、后置条件、接口/页面和测试。
5. **约束、假设与决策**——已确认约束、`ASM-*`、已决事项和开放阻塞项。
6. **项目技术上下文**——当前语言/框架/构建/模块/持久化/前端/测试事实。
7. **架构设计**——明确分为系统架构设计、概要设计和详细设计，覆盖边界、职责、依赖、数据/控制流、事务、并发、一致性、失败处理和可观测性。Complex Spec 必须包含架构 Flowchart、关键流程 Flowchart 和泳道/Sequence Diagram。对 Java 三层结构，必须定义 Controller、Service 接口、`service.impl`、DAO、Config、Utils 和 POJO 的职责及允许依赖。
8. **分包结构与代码文件树**——现有相关树、选定的三层目标树、精确新增/修改/删除路径、符号、职责和需求映射。`biz.service.impl` 必须嵌套在 `biz.service` 下。这是目标设计，不是实施顺序。
9. **接口定义**——先给出完整 HTTP/RPC/事件/内部接口清单，再逐个展开接口 ID。HTTP 必须写经仓库验证的 Method 与 URL、全部 Path/Query/Header/Body 规则、成功/错误完整 `jsonc`（每字段行尾注释）、面向前端的接口逻辑、鉴权/租户/幂等、版本、兼容与测试；非 HTTP 契约使用对应协议达到同等深度。
10. **POJO 与数据模型设计**——仓库定义的 POJO 职责、对象所有权/边界、类必要性决策、持久化对象或 ORM Entity、DTO/Command/Query/View Object/BO、字段类型、可空性、校验、状态转换、映射、继承和安全复用。关系型模型关系必须与 ER 图一致。不得要求 DDD 聚合、领域服务、仓储端口或值对象。
11. **数据库设计**——先列出所有受影响表，再绘制覆盖全部清单表和直接相关邻表的 Mermaid `erDiagram`，然后逐表、逐个保留/新增/修改/删除的索引展开。说明基数、PK/FK/UK 字段、相关字段、数据库原生类型、可空/默认/约束、关系、真实查询/访问模式、索引字段顺序/选择性/成本、Migration 与历史数据、事务/锁/审计、验证、回滚与兼容。如果仓库规范要求新增 Migration，绝不能修改旧文件。
12. **前端页面设计**——路由、导航、权限、布局/组件树、用户流程、表单规则、API/状态映射、loading/empty/error/disabled/denied 状态、可访问性、响应式和关键文案。
13. **设计模式与架构理念**——采用/拒绝的模式、变化点、简洁性检验和与三层架构的一致性；按需说明 Controller 到 Service 的依赖、Service 到 DAO 的编排、内聚、耦合、信息隐藏、SOLID、YAGNI、持久化继承安全性和 `service.impl` 类的组合优于继承。
14. **测试设计**——行为与不变量的单元测试，以及适用的集成、契约、Mapper/Repository、组件和端到端测试；定义测试数据、边界、失败场景、预期断言、工具和需求映射。
15. **非功能与横切设计**——安全、租户、隐私、性能、容量、缓存、审计、日志、指标、追踪、运维和可维护性。
16. **兼容、迁移、发布与回滚**。
17. **替代方案与决策**——基于证据的权衡和被拒方案。
18. **风险与开放问题**。
19. **追踪矩阵**——每个 `REQ-*` 映射到设计、适用的契约/模型/页面、测试和验收证据；每个设计元素映射回需求或必要基础设施理由。
20. **复核与验收**——原始需求符合性、仓库符合性、跨章节一致性、引用关系正确性和最终结论。

## 完成结论

只能使用以下之一：

- `PASS — Ready for user review`
- `BLOCKED — User decision required`
- `REVISE — Internal inconsistency found`

`PASS` 只表示文档内部完整，不表示用户已经接受。`BLOCKED` 必须指出所需决策。仅编写 Spec 时，不能声称已经完成实现或运行时验证。

## 常见失败

| 失败 | 必需修正 |
| --- | --- |
| 只复述需求，没有仓库证据 | 先检查真实代码、契约、数据、UI、测试和消费者 |
| 复杂场景只根据一条主调用链形成薄弱结论 | 补齐场景、所有权、失败、一致性和质量属性分析；每个结论通过“证据 -> 约束 -> 决策 -> 后果 -> 验证”推导 |
| 只有需求清单，没有参与者目标与用例 | 增加有证据的 `ACTOR-*` 和 `UC-*` 分析，使用完整表格或 Mermaid 系统边界视图，包含触发、条件、结果、失败、后置条件和追踪 |
| 因某个重大语义看似明显就直接选择 | 给出证据/选项并询问用户 |
| 为命名或可逆本地细节打断用户 | 采用最小且符合仓库惯例的选择 |
| 静默修改已获批前置文档 | 新建修订或替代 Spec，并精确链接章节 |
| 只列包名，不列文件树与职责 | 增加精确目标路径、操作、符号、职责和 `REQ-*` 映射 |
| 接口、实体、schema、UI 和测试不一致 | 通过字段/状态/需求追踪修复 |
| 只有接口清单，没有逐个展开 | 每个 ID 增加独立详情，写清路由/符号、入参规则、成功/错误载荷、逻辑、消费者、兼容与测试 |
| 用响应类名或省略 JSON 代替响应结构 | 展示真实完整 `jsonc` 传输结构，每个字段添加行尾含义注释 |
| 只列出表或索引，没有逐项设计 | 每张表和每个索引展开字段、语义、查询/访问证据、索引依据、Migration、锁、验证与回滚 |
| 关系型数据没有 ER 图，或 ER 图遗漏清单表 | 增加 Mermaid `erDiagram`，包含物理名称映射、真实基数/标签和重要 PK/FK/UK 字段，并与每张表详情对齐 |
| 复杂架构只画一张装饰图 | 分别提供与契约、数据、失败和依赖规则一致的架构图、关键流程图和泳道 Mermaid 图 |
| 默认在每层都创建 PO/DO/Entity/BO/DTO/VO/Request/Response | 执行类必要性检验；语义完全相同且复用安全时复用，只保留有依据的边界类型 |
| 使用 `DO`、`VO` 或 `Entity` 却不说明仓库语义 | 明确准确职责；当前规范中 `VO` 表示 View Object，`Entity` 必须说明持久化/ORM 语义 |
| 为减少类数量而把持久化对象直接作为公开契约 | 持久化关注点留在边界内部，只创建确有必要的传输/展示类型 |
| 为复用代码给业务 Service 设计基类继承树 | 组合显式协作者；只有合理的现有框架扩展契约才允许继承 |
| 把 `impl` 与 `service` 平级 | 把实现移动到 `biz.service.impl` |
| 在当前规范中引入聚合、领域服务、仓储端口或 COLA 分层 | 删除暂缓的 DDD/COLA 结构，使用已批准的传统三层分包 |
| Controller 直接访问 DAO 或 `service.impl` | 依赖 Service 接口，并把持久化隐藏在实现内部 |
| 只写模式名，没有变化点 | 拒绝该模式，或说明它解决的具体问题 |
| 把集成测试当作单测设计 | 定义隔离的单元行为，并分开更高层测试 |
| 删除不适用章节 | 保留章节并写有证据的 `N/A` |
| 编写实施顺序或代码 | 停在设计阶段；审核后使用 `egon-coding-writing-plan` |

## Skill 维护

修改本 skill 时，必须用 `references/acceptance-scenarios.md` 进行场景复核，并保持 `SKILL.md` 和所有 `*.zh-CN.md` 审核镜像与英文运行契约同步。
