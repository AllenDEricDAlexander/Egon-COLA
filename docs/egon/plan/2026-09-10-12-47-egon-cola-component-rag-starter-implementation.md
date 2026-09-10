# Egon-COLA RAG 引擎组件实施计划

| Field | Value |
| --- | --- |
| Document | `docs/egon/plan/2026-09-10-12-47-egon-cola-component-rag-starter-implementation.md` |
| Template Version | `4` |
| Status | `Implemented` |
| Created | `2026-09-10 12:47 CST` |
| Updated | `2026-09-10 14:20 CST` |
| Owner | `User` |
| Repository | `Egon-COLA` |
| Scope | `egon-cola-components` 下新增 `egon-cola-component-rag-starter` 单模块，以及 components 父 POM 与 BOM 的登记变更 |
| Source Requirement | 用户 2026-09-10 会话要求：引擎机制做在 component，集中管理不做在 component，可做 `rag-starter`；文档读取支持 pdf/docx/excel 等、分块算法支持多种、向量模型支持多种；存储做成扩展点、本地先行后接 OSS；暂时只允许同一维度不同模型 |
| Baseline Revision | 分支 `docs/rag-starter-spec`，提交 `1009fd3c6`；工作区仅含本 Plan 文件 |
| Implements Spec | [Egon-COLA RAG 引擎组件设计](../spec/2026-09-10-11-34-egon-cola-component-rag-starter.md) |
| Spec Status | `Accepted` |
| Spec Revision | `Updated 2026-09-10 11:47 CST`；接受于 `11:43 CST`，修订于 `11:47 CST`（提交 `21e77965e`） |
| Effective Specs | [Egon-COLA RAG 引擎组件设计](../spec/2026-09-10-11-34-egon-cola-component-rag-starter.md)（全部章节）；[`egon-cola-components` 架构](../../../egon-cola-components/egon-cola-components-architecture.md) §5、§8.1、§11、§13.1（规范依赖） |
| Depends On Plans | `None` |
| Supersedes | `None` |
| Superseded By | `None` |
| Related Plans | `None` |

## 1. Summary

本 Plan 实现已接受的 Spec A，交付 `egon-cola-component-rag-starter`：一个只承载 RAG 引擎机制的单模块 Spring Boot Starter，暴露文本抽取、切块嵌入、相似度检索三条公开能力，四类扩展点（文档读取、分块算法、向量模型、文件存储），以及默认关闭、严格绑定、fail-closed 的自动配置。

依赖方向自底向上：先让模块可构建（Step 1），再依次建立配置与装配骨架（Step 2-3）、抽取链路（Step 4-5）、切块链路（Step 6）、嵌入与写入链路（Step 7）、检索链路（Step 8）、存储扩展点（Step 9）、启动期探针（Step 10），最后补齐契约与可观测性测试（Step 11）与文档（Step 12）。

共 **12 个 Step**。完成证据是：模块在 components Reactor 中构建通过、25 个测试全部离线通过、`TEST-021` 的架构契约测试证明不存在被禁包与被禁依赖、`TEST-024` 证明既有 10 个组件模块的构建结果与 BOM 导出面不变。

本 Plan **不实现** Spec B 的消费方（agent archetype），也不产生任何数据库、HTTP 或前端产物。

## 2. Target Spec and Effective Design

### 2.1 Primary target

- Path: [`docs/egon/spec/2026-09-10-11-34-egon-cola-component-rag-starter.md`](../spec/2026-09-10-11-34-egon-cola-component-rag-starter.md)
- Status: `Accepted`
- Revision: `Updated 2026-09-10 11:47 CST`；修订提交 `21e77965e`（`docs/rag-starter-spec` 分支）
- Approval evidence: 用户 2026-09-10 11:43 对评审意见逐项确认并指示继续；11:47 明确确认按推荐修订（拆出 `RagExtractionService`）。

### 2.2 Effective Spec set

| Role | Spec/link | Status/revision | Effective sections | Why included |
| --- | --- | --- | --- | --- |
| Primary | [RAG 引擎组件设计](../spec/2026-09-10-11-34-egon-cola-component-rag-starter.md) | `Accepted`，`Updated 2026-09-10 11:47 CST` | 全部（`§1`-`§20`） | 本 Plan 的唯一实现依据 |
| Dependency | [`egon-cola-components` 架构](../../../egon-cola-components/egon-cola-components-architecture.md) | 仓库当前版本 | `§5`（starter 定位与职责）、`§8.1`（扁平 starter 例外）、`§11`（BOM 管理规范）、`§13.1`（starter 约束） | Spec A 的 `Depends On`；定义组件形态与 BOM 契约 |
| Dependency | [`egon-cola-component-agent-flow-starter`](../../../egon-cola-components/egon-cola-component-agent-flow-starter/pom.xml) 及其 `lombok.config`、`AutoConfiguration.imports` | 仓库当前版本（`5.4.0`） | 形态先例 | Spec A `EVD-002`、`EVD-003`、`EVD-004` 的证据来源；本 Plan 直接照此复制形态 |
| Dependency | [`egon-cola-component-transactional-outbox-starter`](../../../egon-cola-components/egon-cola-component-transactional-outbox-starter/pom.xml) | 仓库当前版本 | Micrometer 使用先例 | Spec A `EVD-017`；`micrometer-core` 作为 `optional` 依赖的先例 |
| Dependency | [`egon-cola-component-common-core`](../../../egon-cola-components/egon-cola-component-common/pom.xml) | 仓库当前版本 | `BaseConverter<S,T>`、`ValidationUtils`、`mapstruct.version`/`mapstruct-plus.version` 属性 | Spec A `EVD-013`、`EVD-014` |

### 2.3 Superseded or excluded content

`None`。Spec A 无 `Amends`/`Supersedes`。本 Plan 明确排除：

- Spec B（[Agent Archetype 知识库设计](../spec/2026-09-10-11-51-agent-archetype-knowledge-rag.md)）的全部内容——它消费本 Plan 的产物，但不在本 Plan 的实现范围内；
- Spec A `§7.0` 与 `§17` 中裁决为 `Remove`/`Reject` 的元素（HTTP、CRUD、Flyway 建表、调度重试、重排、缓存、分块关系表、组件内自建向量库）。

## 3. Effective Requirements and Acceptance

| Requirement | Source Spec section | Effective statement | Observable acceptance | Implementation impact |
| --- | --- | --- | --- | --- |
| `REQ-001` | Spec A `§4`, `§8.2` | 组件以单 Starter、扁平功能包结构交付，不使用 `biz.*` 或 archetype 模块树 | 目标树无 `biz`/`domain`/`application`/`infrastructure`/`adapter`/`repository` 包；测试在同一模块 `src/test` | Step 1 建模块骨架；Step 11 的 `TEST-021` 静态断言 |
| `REQ-002` | Spec A `§4`, `§9.2.6` | 默认关闭，启用后严格绑定并校验 `egon.cola.component.rag` 配置 | 未配置 `enabled=true` 不创建任何组件 Bean；启用但配置非法时启动失败 | Step 2 |
| `REQ-003` | Spec A `§4`, `§9.2.6` | 不创建 `EmbeddingModel`/`VectorStore`，只按配置中的 Bean 名解析 | 源码无 `base-url`/`api-key`/`apiKey` 属性；Bean 名解析失败时启动失败 | Step 3 |
| `REQ-004` | Spec A `§4`, `§9.2.3`, `§9.2.7` | 文本抽取是 SPI，可注册多种实现并按 `(mimeType, 文件名)` 路由；并对同一 `null` 输入容忍 | 无匹配实现抛确定异常而非返回空文本；多个实现命中按显式优先级选择且可观测 | Step 4 |
| `REQ-005` | Spec A `§4`, `§9.2.4` | 分块算法是 SPI，至少三种内置策略，由枚举 + 工厂选择，禁止字符串 `switch` | 三种策略各有测试；未知枚举值在配置绑定期失败；新增策略只需枚举值与实现类 | Step 6 |
| `REQ-006` | Spec A `§4`, `§9.2.6` | 向量模型是配置驱动的注册表，支持多模型与默认回退 | 未指定模型时用默认模型；指定不存在的逻辑名时失败且错误含可用模型列表 | Step 3 |
| `REQ-007` | Spec A `§4`, `§9.2.2` | 同维度多模型共享单一 `VectorStore`，从机制上排除跨模型串结果 | 写入强制写 `embeddingModel` 元数据；检索强制注入 `embeddingModel` 与 `collectionId` 过滤，调用方无法省略或覆盖 | Step 8 |
| `REQ-008` | Spec A `§4`, `§9.2.6` | 模型维度必须与配置声明一致，否则启动失败 | 任一逻辑模型 `dimensions()` 与 `rag.dimensions` 不等时启动失败并指明模型名 | Step 3 |
| `REQ-009` | Spec A `§4`, `§9.2.1` | 分块身份由确定性规则生成，使摄取可幂等重跑 | 同 `documentId` 与内容重复摄取产生相同分块 id 集合 | Step 6（id 工厂）、Step 7（先删后写） |
| `REQ-010` | Spec A `§4`, `§9.2.1` | 摄取前按文档移除既有分块 | 摄取实现先按 `documentId` 删向量再写入；删除失败时不写入并向上抛出 | Step 7 |
| `REQ-011` | Spec A `§4`, `§10.4` | 元数据 key 由组件规范化并保护，业务不得自造或覆盖保留 key | 传入保留 key 时被拒绝；写入向量的元数据集合是可枚举的固定键集 | Step 7 |
| `REQ-012` | Spec A `§4`, `§9.2.5` | 文件存储是 SPI，内置本地文件系统实现并作为默认 | 无其他实现时本地实现生效；可被宿主 Bean 覆盖；本地实现拒绝路径穿越 | Step 9 |
| `REQ-013` | Spec A `§4`, `§9.2.3` | 可选解析器缺失时不导致启动失败，但需要该格式时给出可诊断错误 | 不含 PDF/Tika 依赖时应用可启动；请求该格式时返回确定的"无可用抽取器"错误并列出已注册 MIME | Step 5 |
| `REQ-014` | Spec A `§4`, `§7.0` | 不依赖数据库、Flyway、Redis、MQ 或调度框架 | 组件 POM 与打包产物无 JDBC 驱动、Flyway、Redis、AMQP、调度器依赖 | Step 1（POM）、Step 11（静态断言） |
| `REQ-015` | Spec A `§4`, `§7.3.5` | 日志与指标不得泄露文档内容、分块文本、向量、供应商地址或密钥 | 日志仅含标识、长度、计数、耗时、结果与异常类型 | Step 4、6、7、8、9、11 |
| `REQ-016` | Spec A `§4`, `§8.3` | 进入 components 父 Reactor 与 BOM，且不导出 Spring AI/Tika 三方坐标 | 父 POM 模块清单含新模块；BOM 只新增一条 `top.egon` 依赖 | Step 1；Step 12 的 `TEST-024` |
| `REQ-017` | Spec A `§4`, `§14` | 所有验证离线、确定性，不启动真实模型、向量库、数据库、网络或 Docker | 测试用 fake `EmbeddingModel`/`VectorStore`/`RagDocumentStorage`；`mvn verify` 不需凭据 | 全部 Step 的验证门 |
| `REQ-018` | Spec A `§4`, `§9.2.6`, `§9.2` | 维度一致性探针存在但默认关闭，并在 README 明确其网络与计费含义 | 默认配置下启动不产生任何模型调用；开启后往返校验失败即启动失败 | Step 10（实现）、Step 12（README） |
| `REQ-019` | Spec A `§4`, `§9.2.7` | 文本抽取是独立公开能力；`RagIngestionCommand` 只接受已抽取的文档 | `extract` 返回 `ExtractedDocumentBO`；把它交给 `ingest` 可在不重新解析的前提下完成切块嵌入 | Step 4（抽取服务）、Step 7（摄取命令） |
| `REQ-020` | Spec A `§4`, `§9.2.1` | 结构元数据与业务属性在写入向量前合并，业务属性同名优先，保留 key 一律拒绝 | 合并结果无重复 key；任一来源含保留 key 时抛 `RagValidationException` | Step 7 |

## 4. Implementation Strategy and Dependency Order

### 4.1 Ordered strategy

顺序由真实编译与运行依赖推导，而非按层罗列：

1. **Step 1 先让模块存在**。父 POM 的 `modules` 与 `dependencyManagement`、BOM 的导出行、模块 POM、`lombok.config`、包骨架。此时没有任何行为，但后续每个 Step 都需要一个能编译的宿主模块，且 `TEST-024` 的回归基线必须在改动父 POM 之前就已确立。
2. **Step 2-3 建配置与装配**。`RagProperties` 是后续所有类型的配置来源；Bean 名解析与维度校验决定服务能否被构造。这两步先行，使后续每个服务的测试都能用 `ApplicationContextRunner` 组装真实协作者。
3. **Step 4-5 建抽取链路**。抽取是摄取的上游，且 `RagIngestionCommand` 的载荷类型（`ExtractedDocumentBO`）由它产出，因此必须早于 Step 7。
4. **Step 6 建切块链路**。策略与 id 工厂无外部依赖，可紧随其后。
5. **Step 7 建嵌入与写入链路**。它同时依赖 Step 3（模型注册表）、Step 4（`ExtractedDocumentBO`）、Step 6（策略与 id），是依赖汇合点。
6. **Step 8 建检索链路**。它与 Step 7 共享模型注册表但无先后依赖，选择放在 Step 7 之后是因为 `RagChunkConverter` 的 `toSource` 方向在检索测试里才被完整验证。
7. **Step 9 建存储扩展点**。与主链路正交，但它产出的 `RagDocumentStorage` Bean 参与 Step 2 建立的装配，因此必须在装配测试完整前落地。
8. **Step 10 建探针**。它需要 Step 3 的注册表与 Step 2 的属性都就绪。
9. **Step 11 补契约与可观测性**。此时全部生产类型已存在，静态断言与指标断言才有对象。
10. **Step 12 收尾文档与回归**。README 需要全部行为已定型；`TEST-024` 需要全部模块文件已就位。

### 4.2 Test-first strategy

| Step | RED 测试 | 期望的 RED 原因 | 最小 GREEN | 允许的重构/装配 |
| --- | --- | --- | --- | --- |
| 1 | 不适用（见下） | — | 模块可构建 | 无 |
| 2 | `RagAutoConfigurationTest#creates_no_bean_when_disabled` 等 | 类不存在，编译失败 | `RagProperties` + `RagAutoConfiguration` 的最小骨架 | 拆出 4 个嵌套 Properties record |
| 3 | `RagAutoConfigurationTest#fails_when_bean_name_unresolved` 等 | 解析逻辑不存在，Bean 缺失 | `RagEmbeddingModelRegistry` 与启动校验 | 抽出 `RagEmbeddingModelDescriptorBO` |
| 4 | `RagExtractionServiceImplTest#routes_by_priority` 等 | 服务与注册表不存在 | 注册表 + 两个内置抽取器 + `RagExtractionServiceImpl` | 抽出 `RagDocumentExtractorRegistry` |
| 5 | `RagOptionalExtractorTest#starts_without_pdf_reader` | 可选实现不存在 | `PdfRagDocumentExtractor` 与 `TikaRagDocumentExtractor` | 无 |
| 6 | `RagChunkingStrategyFactoryTest#resolves_all_builtin_strategies` 等 | 工厂与策略不存在 | 枚举 + 工厂 + 三策略 + `RagChunkIdFactory` | 抽出递归切分与 Markdown 两级切分 |
| 7 | `RagIngestionServiceImplTest#deletes_before_writing` 等 | 服务与转换器不存在 | `RagChunkConverter` + `RagMetadataKeys` + `RagIngestionServiceImpl` | 抽出元数据合并 |
| 8 | `RagRetrievalServiceImplTest#forces_tenant_and_model_filters` | 服务不存在 | `RagRetrievalServiceImpl` | 无 |
| 9 | `LocalFileSystemRagDocumentStorageTest#rejects_path_traversal` | 实现不存在 | 存储 SPI + 本地实现 | 抽出 `RagStoredObjectBO` |
| 10 | `RagVectorStoreProbeTest#skips_when_disabled` | 探针不存在 | `RagVectorStoreProbe` + 装配钩子 | 无 |
| 11 | `RagComponentContractTest#has_no_forbidden_packages` | 契约测试不存在 | 契约测试 + 指标埋点 | 无 |
| 12 | 不适用（文档与回归） | — | README 与回归验证 | 无 |

**Step 1 与 Step 12 的 test-first 例外**：Step 1 只建立编译宿主（POM、`lombok.config`、空包），没有任何可被测试观察的行为，其证明是构建成功与 BOM 解析；Step 12 只写文档与执行回归，不引入行为。两者均按 `references/file-by-file-planning.md`「compile prerequisite」条款标注 `Not applicable`，并各自给出可观察的构建/回归证据。

### 4.3 Sequential and parallel boundaries

| Step | Depends on | May run in parallel with | Must not overlap with | Reason |
| --- | --- | --- | --- | --- |
| Step 1 | None | None | 全部后续 Step | 模块不存在则无法编译任何文件 |
| Step 2 | Step 1 | None | Step 3 | 同改 `RagAutoConfiguration` 与 `RagProperties` |
| Step 3 | Step 2 | Step 9 | Step 2 | 同改 `RagAutoConfiguration`；Step 9 只改装配 Bean 方法 |
| Step 4 | Step 2 | Step 6, Step 9 | Step 5 | 同改 `extract/` 包与 `AutoConfiguration` 的注册表 Bean |
| Step 5 | Step 4 | Step 6, Step 9 | Step 4 | 同改 `extract/` 包；可选依赖与 `@ConditionalOnClass` 需注册表已存在 |
| Step 6 | Step 2 | Step 4, Step 5, Step 9 | — | 独立 `chunk/` 包 |
| Step 7 | Step 3, Step 4, Step 6 | Step 8（只读注册表） | Step 8 | 同改 `RagChunkConverter` 的两个方向 |
| Step 8 | Step 3 | Step 7（互不写同一文件时） | Step 7 | 二者共同依赖 `RagChunkConverter`；实现上顺序执行以避免同文件并发写 |
| Step 9 | Step 2 | Step 3, Step 4 | Step 2 | 同改 `RagAutoConfiguration` 的默认存储 Bean |
| Step 10 | Step 2, Step 3 | None | Step 11 | 同改 `RagAutoConfiguration` 的探针钩子 |
| Step 11 | Step 4-10 | None | Step 12 | 契约断言覆盖全部生产类型 |
| Step 12 | Step 1-11 | None | — | 文档需行为定型；回归需全部文件就位 |

实际执行建议严格串行：虽有可并行组合，但 `RagAutoConfiguration` 是多个 Step 的共同写入点，并行会引入合并冲突。上表只用于说明依赖关系。

### 4.4 Commit boundaries

每个 Step 一个语义提交，提交路径等于该 Step 声明的写入范围。唯一例外是 Step 1：它同时改父 POM、BOM 与新增模块，这三者在 Maven Reactor 中不可分割——缺任一处构建即失败，因此必须同一提交。

### 4.5 Spec Simplicity and Implementation-necessity Audit

| Spec element | Spec necessity verdict/section | Current repository evidence | Direct/reuse alternative | Interaction/implementation cost | Plan decision |
| --- | --- | --- | --- | --- | --- |
| 新组件模块 | `Add`（Spec A `§7.0`） | `egon-cola-component-agent-flow-starter/pom.xml` 形态先例 | 在各业务项目自写引擎 | 一个模块、两条登记、一整套测试 | Implement |
| 三个公开服务入口 | `Add`（`§7.0`、`§9.1`） | `AgentFlowService` 单入口先例 | 合并为一个入口 | 一个公开接口与一次显式编排 | Implement |
| `RagExtractionService` | `Add`（`§7.0`，`REQ-019`） | 无同类 | 抽取留在摄取内部 | 一个接口、一个命令载体 | Implement |
| `RagIngestionService` | `Add`（`§9.2.1`） | 无同类 | 直接用组件原语 | — | Implement |
| `RagRetrievalService` | `Add`（`§9.2.2`） | 无同类 | 宿主自建 `SearchRequest` | — | Implement |
| `RagDocumentExtractor` SPI | `Add`（`§7.0`） | `google-adk` 无同类抽象 | 单实现 + 扩展名分支 | 一个接口、一个注册表、四个实现 | Implement |
| `RagChunkingStrategy` SPI + 枚举 + 工厂 | `Add`（`§7.0`） | `AgentWorkflowBuilderStrategy` 同类先例 | 直接调 `TokenTextSplitter` | 一个接口、一个枚举、一个工厂、三实现 | Implement |
| `RagEmbeddingModelRegistry` | `Add`（`§7.0`） | 无同类 | 单一 Bean 名 | 一段配置结构、一个注册表 | Implement |
| `RagDocumentStorage` SPI + 本地实现 | `Add`（`§7.0`） | 无同类 | 宿主自行保存文件 | 一个接口、一个默认实现 | Implement |
| `RagChunkIdFactory` | `Add`（`§7.0`，`REQ-009`） | 无同类 | 随机 UUID | 一个纯函数类型 | Implement |
| `RagMetadataKeys` | `Add`（`§7.0`） | 无同类 | 各服务内联字符串常量 | 一个常量类 | Implement |
| `RagChunkConverter` | `Add`（`§10.4`） | `ResearchEventConverter` 先例 | 手工构造 `Document` | 一个 MapStruct 接口 | Implement |
| `RagVectorStoreProbe` | `Add`（`§7.0`，`REQ-018`） | 无同类 | 只做离线维度比对 | 一个类型 + 一个开关 | Implement |
| 启动期校验（Bean 名、维度、抽取器冲突） | `Add`（`§9.2.6`） | `AgentFlowConfigValidator` 同类先例 | 运行期才失败 | 一段装配期校验 | Implement |
| MapStruct 依赖 | `Add`（`§6.1`，`DEC-007`，用户已批准） | `common-core` 的 `mapstruct.version=1.6.3` 属性（test scope） | 手工构造 `Document` | 父 POM 两条 `dependencyManagement` + 模块一条依赖 | Implement |
| HTTP/CRUD/Flyway/调度/重排/缓存/分块表/组件内建向量库 | `Remove`（`§7.0`、`§17`） | 不适用 | — | 若保留将引入 Web、迁移、线程池与第二套存储 | 不实现（返回 Spec 已裁决） |

未发现 fetch-then-forward 接口：`RagExtractionService` 的返回不是"取参数再转发"，而是消费方**必须持久化**的产物（Spec B 的原文入库需求），其独立消费者目标已在 `§9.2.7` 的必要性小节论证。

### 4.6 Change-unit Dependency Matrix

| Change unit | Requirements | Proof/RED point | Compile/runtime prerequisites | Produces | Consumers/unblocks | Owning Step |
| --- | --- | --- | --- | --- | --- | --- |
| 可构建的组件模块 | `REQ-001`, `REQ-016` | 构建成功（无 RED 测试） | 父 POM、BOM、模块 POM | 模块坐标与目录 | 全部后续 Step | Step 1 |
| 配置与装配骨架 | `REQ-002` | `TEST-011`-`TEST-013` | Step 1 | `RagProperties` 及嵌套、`RagAutoConfiguration` | Step 3-10 | Step 2 |
| 模型注册表与启动校验 | `REQ-003`, `REQ-006`, `REQ-008` | `TEST-014`, `TEST-015` | Step 2 | `RagEmbeddingModelRegistry`、`RagEmbeddingModelDescriptorBO` | Step 7, 8, 10 | Step 3 |
| 抽取链路 | `REQ-004`, `REQ-019` | `TEST-001`, `TEST-002`, `TEST-004`, `TEST-026` | Step 2 | `RagDocumentExtractor`、注册表、两个内置实现、`RagExtractionService`、`ExtractedDocumentBO` | Step 5, 7 | Step 4 |
| 可选解析器 | `REQ-013` | `TEST-019` | Step 4 | `PdfRagDocumentExtractor`、`TikaRagDocumentExtractor` | Step 12 文档 | Step 5 |
| 切块链路 | `REQ-005`, `REQ-009` | `TEST-003`, `TEST-005`, `TEST-009` | Step 2 | `RagChunkingStrategy`、枚举、工厂、三策略、`RagChunkIdFactory`、`RagChunkBO` | Step 7 | Step 6 |
| 嵌入与写入链路 | `REQ-009`, `REQ-010`, `REQ-011`, `REQ-020` | `TEST-006`, `TEST-007`, `TEST-008`, `TEST-010`, `TEST-025` | Step 3, 4, 6 | `RagIngestionService`、`RagChunkConverter`、`RagMetadataKeys` | Step 11 | Step 7 |
| 检索链路 | `REQ-007` | `TEST-016`, `TEST-017` | Step 3 | `RagRetrievalService`、`RagRetrievedChunkBO` | Spec B | Step 8 |
| 存储扩展点 | `REQ-012` | `TEST-020` | Step 2 | `RagDocumentStorage`、枚举、`RagStoredObjectBO`、本地实现 | Step 11, Spec B | Step 9 |
| 启动期探针 | `REQ-018` | `TEST-018` | Step 2, 3 | `RagVectorStoreProbe` | Step 12 文档 | Step 10 |
| 契约与可观测性 | `REQ-014`, `REQ-015` | `TEST-021`, `TEST-022`, `TEST-023` | Step 4-10 | `RagComponentContractTest`、指标埋点 | Step 12 | Step 11 |
| 交付面回归 | `REQ-016` | `TEST-024` | Step 1-11 | `TEST-024` 回归测试 | 发布 | Step 12 |

### 4.7 Java, Spring, and Egon-COLA Implementation Standards

| Concern | Current repository evidence | Effective Spec decision | Planned implementation consequence | Owning Steps/checks |
| --- | --- | --- | --- | --- |
| Architecture profile | 单模块 Starter：`egon-cola-component-agent-flow-starter/{pom.xml,lombok.config,src/main/resources/META-INF/spring/...imports}` | Spec A `§6.1`、`DEC-002`（Rule 11 组件库扁平例外，用户已批准） | 目标包根 `top.egon.cola.component.rag`，扁平功能包；无 `biz.*`、无 archetype 模块树 | Step 1；`MC-ARCH-001` |
| Reuse/capability | `common-core` 的 `BaseConverter<S,T>`、`ValidationUtils`、MapStruct 版本属性；`agent-flow-starter` 的 `lombok.config` 与 `AutoConfiguration.imports`；outbox 的 Micrometer 用法 | Spec A `§6.1` reuse ledger | 全部复用；唯一新增依赖是 MapStruct（`DEC-007`） | Step 1, 2, 7；`MC-REUSE-001` / `MC-DEP-001` |
| Naming/model/validation/conversion | `ResearchEventConverter`（MapStruct `@Mapper` + `INSTANCE = Mappers.getMapper`）；agent archetype 的 record 紧凑构造器写法 | Spec A `§10.1`、`§10.3.1`、`§10.4` | 全部载体带语义后缀；简单对象 `record` + 紧凑构造器；唯一转换器用 MapStruct 并 `extends BaseConverter` | Step 2, 4, 6, 7, 8；`MC-NAME-001` / `MC-MODEL-001` / `MC-CONVERT-001` / `MC-VALID-001` |
| Bean/logging/util/JSON/time/config | `agent-flow-starter` 的 `@AutoConfiguration` + `@Bean(name=...)` + `@ConditionalOnMissingBean`；`lombok.config` 复制 `Qualifier` 与 `Value` | Spec A `§6.2` 规则 4、7、10 | 显式 Bean 名、`@RequiredArgsConstructor`、逐字段 `@Qualifier`、`@Slf4j`；只用 JDK；`java.time` | Step 2-10；`MC-BEAN-001` / `MC-LOG-001` / `MC-UTIL-001` / `MC-TIME-001` / `MC-CONFIG-001` |
| Business variation/pattern | `AgentWorkflowBuilderStrategy` + `AgentWorkflowStrategyFactory`（策略 + 工厂先例） | Spec A `§13.1` | 抽取/分块/存储用 Strategy，枚举与 Bean 名用 Factory/Registry，禁止字符串或反射分派 | Step 4, 6, 9；`MC-PATTERN-001` |

#### Capability reuse ledger

| Need | Candidates inspected | Exact evidence | Fit/gap | Decision | Added dependency/custom code | Owning Step/check |
| --- | --- | --- | --- | --- | --- | --- |
| 自动配置发现 | Spring Boot `@AutoConfiguration` | `agent-flow-starter/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`（单行） | 完全足够 | Reuse | `None` | Step 2；`MC-REUSE-001` |
| 严格配置绑定 | Spring Boot `Binder` + `NoUnboundElementsBindHandler` | `AgentFlowAutoConfiguration#agentFlowProperties` | 完全足够 | Reuse | `None` | Step 2；`MC-REUSE-001` |
| 边界校验 | `spring-boot-starter-validation` + `common-core` `ValidationUtils` | `agent-flow-starter/pom.xml` 的 validation starter；`AgentFlowAutoConfiguration` 注入 `ValidationUtils` Bean | 完全足够 | Reuse | `None` | Step 2, 6；`MC-VALID-001` |
| 向量与嵌入抽象 | Spring AI `VectorStore`、`EmbeddingModel`、`SearchRequest`、`Document` | `spring-ai-vector-store` 与 `spring-ai-model` 由父 POM 的 `spring-ai-bom:1.1.8` 管理 | 完全足够；类与方法签名已用 `javap` 核实 | Reuse | `spring-ai-vector-store`、`spring-ai-model`（版本由 BOM 管理） | Step 1, 2, 7, 8；`MC-REUSE-001` |
| 分块算法（token 维度） | Spring AI `TokenTextSplitter` | `spring-ai-commons` 传递依赖，`TokenTextSplitter#builder` 与 `(int,int,int,int,boolean)` 构造器已核实 | 足以实现 `TOKEN` 策略 | Reuse | `None`（传递依赖） | Step 6；`MC-REUSE-001` |
| PDF 与通用文档解析 | `spring-ai-pdf-document-reader`、`spring-ai-tika-document-reader` | `spring-ai-bom:1.1.2` 管理清单含两个 artifact | 覆盖 PDF 与 docx/xlsx 等；按需引入 | Reuse（`optional`） | 两条 `optional` 依赖 | Step 5；`MC-DEP-001` |
| 对象转换 | MapStructPlus / MapStruct | `common-core/pom.xml` 的 `mapstruct.version=1.6.3`、`mapstruct-plus.version=1.5.1`（该模块为 test scope，父 POM 未管理） | 仓库其余组件无运行时 MapStruct 先例 | Approved addition（`DEC-007`，用户已批准） | 父 POM 两条 `dependencyManagement` + 模块 `mapstruct` 依赖与注解处理器 | Step 1, 7；`MC-DEP-001` |
| 度量 | Micrometer | `transactional-outbox-starter/pom.xml` 将 `micrometer-core` 声明为 `optional` | 完全足够；缺席时须完全跳过 | Reuse（`optional`） | 一条 `optional` 依赖 | Step 11；`MC-DEP-001` |
| 本地文件读写 | JDK `java.nio` | 仓库无同类存储实现 | 完全足够 | Reuse | `None` | Step 9；`MC-UTIL-001` |
| 时间类型 | `java.time` | `common-core` 与 agent archetype 的时间用法 | 完全足够 | Reuse | `None` | Step 2, 4, 7, 9；`MC-TIME-001` |

未发现重复实现 Spring 或 Egon 既有能力的地方；唯一新增依赖 MapStruct 有 Spec 层批准（`DEC-007`）与明确缺口（Rule 3 强制跨边界转换使用 MapStruct/MapStructPlus）。

### 4.8 User-mandated Java Rule Implementation Matrix

| Literal rule | Spec source | Repository evidence | Exact files and order | Pseudocode obligations | Validation gate | Steps | Status/blocker |
| --- | --- | --- | --- | --- | --- | --- | --- |
| Rule 1 | Spec A `§6.2`、`§10.1` | 全部新增 Java 类型清单见 `§5` | 先 `model`/`exception`，后 SPI，再实现，最后装配 | 每个类型带 `Service`/`Strategy`/`Factory`/`Registry`/`Extractor`/`Converter`/`Exception`/`Enum`/`BO`/`Command`/`Query`/`Result`/`Properties`/`AutoConfiguration` 后缀；无 `Data`/`Info`/`Param`/`Bean` | `TEST-021` 命名扫描 + 编译 | Step 2-11 | PASS |
| Rule 2 | Spec A `§6.2`、`§9.2.x` | `spring-boot-starter-validation`、`common-core` `ValidationUtils`、agent archetype 的 `@Valid` 用法 | `model/RagExtractionCommand`、`RagIngestionCommand`、`RagRetrievalQuery`、`RagChunkingConfigDTO` 与其服务实现的顺序 | 命令/查询载体带 Jakarta 注解；服务方法 `@Validated`；紧凑构造器做规范化；手工校验复用 `ValidationUtils`；无电话字段故 libphonenumber `N/A`；无复用输入故不新增分组 | `TEST-005`、`TEST-026` | Step 4, 6, 7, 8 | PASS |
| Rule 3 | Spec A `§6.2`、`§10.3.1`、`§10.4` | `ResearchEventConverter` 的 `@Mapper` + `Mappers.getMapper` + `extends BaseConverter` 写法 | `converter/RagChunkConverter` 在 `model/RagChunkBO` 之后、摄取服务之前 | 全部简单载体用 `record` + 紧凑构造器；转换器 `@Mapper(unmappedTargetPolicy=ERROR)` 接口实现 `BaseConverter<RagChunkBO, Document>`，逐字段 `@Mapping`；无 `@Value` 与复杂 Lombok 类 | 转换器编译 + `TEST-008` | Step 7 | PASS |
| Rule 4 | Spec A `§6.2`、`§7.3.1` | `agent-flow-starter` 的 `@Bean(name=...)`、`@ConditionalOnMissingBean`、`lombok.config` 复制 `Qualifier` | `RagAutoConfiguration` 与四个 `*ServiceImpl`、三个策略、四个抽取器、本地存储实现 | 每个具体行为类 `@Slf4j`；Spring Bean 显式命名；依赖 final + `@RequiredArgsConstructor` + 逐字段 `@Qualifier` | `TEST-011`-`TEST-015` 的装配 + `TEST-021` 静态检查 | Step 2-11 | PASS |
| Rule 5 | Spec A `§6.2`、`§15` | 组件内无既有工具类；`agent-flow-starter` 同样只用 JDK | 全部生产文件 | 只用 JDK（`java.nio`、`java.time`、`java.util`）；不新增 `*Utils`；不引入 `commons-*`/Guava；Tika 仅作可选内容识别 | `TEST-021` 依赖与 import 扫描 | Step 4-11 | PASS |
| Rule 6 | Spec A `§6.2`、`§9.0` | 组件无对外 JSON 契约 | `N/A`（`§9.1` 无 `API-*`） | 不使用 Jackson 做对外序列化，也不用 JSON 做对象转换 | `MC-JSON-001` 证据：`§9.0` 的 GraphQL/REST 行均为 `N/A` | 全部 | N/A |
| Rule 7 | Spec A `§6.2`、`§9.2.6` | 组件不提供 `application-*.yml`；`agent-flow-starter` 同样只交付 `AutoConfiguration.imports` | `autoconfigure/RagProperties` 与配置元数据 | 键集由 `RagProperties` 与 `spring-configuration-metadata.json` 固定并由严格绑定校验；消费方的三 profile 一致性属 Spec B | `TEST-013` 键集与严格绑定测试 | Step 2 | N/A（组件不分 profile） |
| Rule 9 | Spec A `§6.2`、`§13.1` | `AgentWorkflowBuilderStrategy` + `AgentWorkflowStrategyFactory` 先例；字符串 `switch` 参考实现已被仓库评审否定 | `chunk/RagChunkingStrategyEnum` → `RagChunkingStrategy` → 三策略 → `RagChunkingStrategyFactory`；`extract/RagDocumentExtractor` → 注册表 → 四实现；`storage/RagDocumentStorage` → 本地实现 | 枚举 + 工厂选择策略；注册表按 `order()` 路由；禁止字符串与反射分派 | `TEST-003`、`TEST-001`、`TEST-020` | Step 4, 6, 9 | PASS |
| Rule 10 | Spec A `§6.2`、`§10.3` | `common-core` 与 agent archetype 的时间用法；`BaseConverter` 的遗留 `Date` 方法不在本次范围 | `execution/RagIngestionServiceImpl`（耗时）、`autoconfigure/RagProperties`（`Duration` 默认值）、`storage/LocalFileSystemRagDocumentStorage`（`Instant` 写入时刻） | 时间字段用 `Duration`/`Instant`；时钟源为可注入的 `ragClock`；禁止 `java.util.Date`/`Calendar`/`SimpleDateFormat`；显式禁止调用 `BaseConverter` 的 `Date` 默认方法 | `TEST-007`、`TEST-021` 源码扫描 | Step 2, 7, 9 | PASS |
| Rule 11 | Spec A `§6.1`、`§8.2`、`DEC-002` | `agent-flow-starter` 的单模块扁平形态；用户已批准 Rule 11 组件库例外 | 全部目标文件位于 `top.egon.cola.component.rag.{api,extract,chunk,embed,storage,converter,metadata,model,execution,autoconfigure,exception}` | 不引入 `biz.*`、不引入 archetype 模块树、不新增层 | `TEST-021` 包结构断言 | 每个 Step | PASS |

## 5. Change File Tree

```text
egon-cola-components/
├── pom.xml                                                        # MODIFY
├── egon-cola-components-architecture.md                           # MODIFY
├── egon-cola-components-bom/pom.xml                               # MODIFY
└── egon-cola-component-rag-starter/                               # CREATE (module root)
    ├── pom.xml                                                    # CREATE
    ├── lombok.config                                              # CREATE
    ├── README.md                                                  # CREATE
    ├── README.zh-CN.md                                            # CREATE
    └── src/
        ├── main/
        │   ├── java/top/egon/cola/component/rag/
        │   │   ├── package-info.java                              # CREATE
        │   │   ├── api/{RagExtractionService,RagIngestionService,RagRetrievalService,package-info}.java   # CREATE
        │   │   ├── extract/{RagDocumentExtractor,RagDocumentExtractorRegistry,PlainTextRagDocumentExtractor,
        │   │   │            MarkdownRagDocumentExtractor,PdfRagDocumentExtractor,TikaRagDocumentExtractor,
        │   │   │            package-info}.java                    # CREATE
        │   │   ├── chunk/{RagChunkingStrategy,RagChunkingStrategyEnum,RagChunkingStrategyFactory,
        │   │   │          TokenRagChunkingStrategy,MarkdownHeadingRagChunkingStrategy,
        │   │   │          RecursiveRagChunkingStrategy,RagChunkIdFactory,package-info}.java                # CREATE
        │   │   ├── embed/{RagEmbeddingModelRegistry,RagEmbeddingModelDescriptorBO,package-info}.java      # CREATE
        │   │   ├── storage/{RagDocumentStorage,RagDocumentStorageTypeEnum,RagStoredObjectBO,
        │   │   │            LocalFileSystemRagDocumentStorage,package-info}.java                          # CREATE
        │   │   ├── converter/{RagChunkConverter,package-info}.java                                        # CREATE
        │   │   ├── metadata/{RagMetadataKeys,package-info}.java                                           # CREATE
        │   │   ├── model/{RagExtractionCommand,RagIngestionCommand,RagIngestionResult,RagRetrievalQuery,
        │   │   │          RagRetrievedChunkBO,RagChunkBO,ExtractedDocumentBO,RagChunkingConfigDTO,
        │   │   │          package-info}.java                      # CREATE
        │   │   ├── execution/{RagExtractionServiceImpl,RagIngestionServiceImpl,RagRetrievalServiceImpl,
        │   │   │             RagVectorStoreProbe,package-info}.java                                       # CREATE
        │   │   ├── autoconfigure/{RagAutoConfiguration,RagProperties,RagEmbeddingModelProperties,
        │   │   │                  RagStorageProperties,RagRetrievalProperties,RagValidationProperties,
        │   │   │                  package-info}.java              # CREATE
        │   │   └── exception/{RagException,RagConfigurationException,RagValidationException,
        │   │                  RagExtractorMissingException,RagExtractorConflictException,
        │   │                  RagExtractionException,RagChunkingException,RagModelNotRegisteredException,
        │   │                  RagEmbeddingException,RagVectorStoreException,RagStorageException,
        │   │                  package-info}.java                  # CREATE
        │   └── resources/META-INF/
        │       ├── spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports                # CREATE
        │       └── spring-configuration-metadata.json                                                     # CREATE
        └── test/java/top/egon/cola/component/rag/
            ├── package-info.java                                  # CREATE
            ├── support/{FakeEmbeddingModel,FakeVectorStore,FakeRagDocumentStorage,package-info}.java      # CREATE
            ├── contract/{RagComponentContractTest,package-info}.java                                      # CREATE
            ├── extract/{RagDocumentExtractorRegistryTest,RagExtractionServiceImplTest,
            │            RagOptionalExtractorTest,package-info}.java                                   # CREATE
            ├── chunk/{RagChunkingStrategyFactoryTest,RagChunkingStrategyTest,RagChunkIdFactoryTest,
            │          package-info}.java                              # CREATE
            ├── storage/{LocalFileSystemRagDocumentStorageTest,package-info}.java                          # CREATE
            ├── converter/{RagChunkConverterTest,package-info}.java                                        # CREATE
            ├── execution/{RagIngestionServiceImplTest,RagRetrievalServiceImplTest,
            │              TwoPhaseIngestionTest,RagVectorStoreProbeTest,package-info}.java                # CREATE
            ├── autoconfigure/{RagAutoConfigurationTest,RagPropertiesBindingTest,RagObservabilityTest,
            │                  RagDeliveryBoundaryTest,package-info}.java                                 # CREATE
            └── reactor/{RagDeliverySurfaceTest,package-info}.java                                         # CREATE
```

| Operation | Path | Current evidence/symbol | Final symbols/state | Responsibility | Step | Requirements | Validation owner |
| --- | --- | --- | --- | --- | --- | --- | --- |
| MODIFY | `egon-cola-components/pom.xml` | 现有 父 POM 的 modules 段 10 项；`<properties>` 有 `spring-ai.version`、`google-adk.version`；`dependencyManagement` 导入 `spring-ai-bom` | 新增 `一条新的 module 登记：egon-cola-component-rag-starter`；新增 `mapstruct.version=1.6.3`、`mapstruct-plus.version=1.5.1` 属性与两条 `dependencyManagement` | 登记模块并管理 MapStruct 坐标 | Step 1 | `REQ-016`, `REQ-001` | `TEST-024` |
| MODIFY | `egon-cola-components/egon-cola-components-bom/pom.xml` | 逐项导出 20 条 `top.egon` 组件依赖 | 新增一条 `egon-cola-component-rag-starter` | 导出新组件 | Step 1 | `REQ-016` | `TEST-024` |
| CREATE | `egon-cola-component-rag-starter/pom.xml` | 形态先例 `egon-cola-component-agent-flow-starter/pom.xml` | 单模块 JAR，父为 `egon-cola-components-parent:5.4.0` | 声明依赖与注解处理器 | Step 1 | `REQ-003`, `REQ-013`, `REQ-014`, `REQ-016` | `TEST-024` |
| CREATE | `egon-cola-component-rag-starter/lombok.config` | `agent-flow-starter/lombok.config` 逐行一致 | `stopBubbling`、复制 `Qualifier`/`Value`、`addLombokGeneratedAnnotation`、`anyConstructor.addConstructorProperties` | 保证 `@Qualifier` 传递到构造参数 | Step 1 | Rule 4 | `TEST-011`-`TEST-015` |
| CREATE | `.../rag/package-info.java` 及 12 个包子目录的 `package-info.java` | `agent-flow-starter` 每个包均有 `package-info.java` | 13 个包说明文件 | 包文档 | Step 1-11（随包创建） | Rule 11 | 编译 |
| CREATE | `.../rag/autoconfigure/RagProperties.java` | `AgentFlowProperties` 的 record + 紧凑构造器 + 严格绑定先例 | 顶层属性 record 与四个嵌套 record | 配置载体与规范化 | Step 2 | `REQ-002`, `REQ-006`, `REQ-012`, `REQ-018` | `TEST-013` |
| CREATE | `.../rag/autoconfigure/{RagEmbeddingModelProperties,RagStorageProperties,RagRetrievalProperties,RagValidationProperties}.java` | 同上 | 四个嵌套配置 record | 分层配置键 | Step 2 | `REQ-002`, `REQ-012`, `REQ-018` | `TEST-013` |
| CREATE | `.../rag/autoconfigure/RagAutoConfiguration.java` | `AgentFlowAutoConfiguration` 的 `@AutoConfiguration` + `@ConditionalOnProperty` + `@Bean(name=...)` + `@ConditionalOnMissingBean` | 装配与启动校验的全部 Bean | 唯一装配入口 | Step 2, 3, 4, 6, 9, 10 | `REQ-002`, `REQ-003`, `REQ-004`, `REQ-005`, `REQ-008`, `REQ-012`, `REQ-018` | `TEST-011`-`TEST-015`, `TEST-018` |
| CREATE | `.../resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` | `agent-flow-starter` 同名单行文件 | 一行指向 `RagAutoConfiguration` | 自动配置发现 | Step 2 | `REQ-002` | `TEST-011` |
| CREATE | `.../resources/META-INF/spring-configuration-metadata.json` | `spring-boot-configuration-processor` 生成并由 `AgentFlowProperties` 触发 | `RagProperties` 全部键的元数据 | 配置元数据 | Step 2 | `REQ-002` | `TEST-013` |
| CREATE | `.../rag/exception/RagException.java` 与 10 个子类 | `DeepResearchApplicationException` 等既有异常类型风格 | 一个基类 + 10 个子类 | 稳定失败类型 | Step 2 | `REQ-004`, `REQ-005`, `REQ_006`, `REQ-015` | 编译 + `TEST-021` |
| CREATE | `.../rag/model/{RagExtractionCommand,ExtractedDocumentBO}.java` | agent archetype 的 record + 紧凑构造器写法 | 抽取命令与结果载体 | 抽取契约 | Step 4 | `REQ-004`, `REQ-019` | `TEST-002`, `TEST-026` |
| CREATE | `.../rag/extract/RagDocumentExtractor.java` | 无同类；形态参照 `AgentWorkflowBuilderStrategy` | 抽取 SPI | 可扩展点 | Step 4 | `REQ-004` | `TEST-001` |
| CREATE | `.../rag/extract/RagDocumentExtractorRegistry.java` | 无同类；形态参照 `AgentFlowRegistry` | 优先级路由与失败 | 注册与路由 | Step 4 | `REQ-004`, `REQ-013` | `TEST-001`, `TEST-002`, `TEST-004` |
| CREATE | `.../rag/extract/{PlainTextRagDocumentExtractor,MarkdownRagDocumentExtractor}.java` | 无同类 | 内置文本抽取实现 | 内置能力 | Step 4 | `REQ-004` | `TEST-002` |
| CREATE | `.../rag/extract/{PdfRagDocumentExtractor,TikaRagDocumentExtractor}.java` | `spring-ai-pdf-document-reader`、`spring-ai-tika-document-reader` 在 BOM 管理清单中 | 可选解析器实现 | 可选能力 | Step 5 | `REQ-013` | `TEST-019` |
| CREATE | `.../rag/api/RagExtractionService.java` | 无同类；形态参照 `AgentFlowService` | 抽取公开接口 | 服务入口 | Step 4 | `REQ-019` | `TEST-026` |
| CREATE | `.../rag/execution/RagExtractionServiceImpl.java` | 无同类；Bean 命名参照 `DefaultAgentFlowService` | 抽取编排 | 服务实现 | Step 4 | `REQ-004`, `REQ-019` | `TEST-002`, `TEST-026` |
| CREATE | `.../rag/chunk/RagChunkingStrategyEnum.java` | 无同类；形态参照 `AgentWorkflowTypeEnum` | 三种策略枚举 | 关闭的取值集合 | Step 6 | `REQ-005` | `TEST-003` |
| CREATE | `.../rag/model/{RagChunkBO,RagChunkingConfigDTO}.java` | agent archetype 的 record 写法 | 分块与分块配置载体 | 切块契约 | Step 6 | `REQ-005`, `REQ-009` | `TEST-005`, `TEST-009` |
| CREATE | `.../rag/chunk/RagChunkingStrategy.java` 与三个实现 | 形态参照 `AgentWorkflowBuilderStrategy` 及其实现 | 切块 SPI 与三实现 | 可扩展点 | Step 6 | `REQ-005` | `TEST-003`, `TEST-009` |
| CREATE | `.../rag/chunk/RagChunkingStrategyFactory.java` | 形态参照 `AgentWorkflowStrategyFactory` | 枚举到实现的映射 | 选择与构造 | Step 6 | `REQ-005` | `TEST-003` |
| CREATE | `.../rag/chunk/RagChunkIdFactory.java` | 无同类 | 确定性分块标识 | 幂等基础 | Step 6 | `REQ-009` | `TEST-009` |
| CREATE | `.../rag/model/{RagIngestionCommand,RagIngestionResult}.java` | agent archetype 的 record 写法 | 摄取命令与结果 | 摄取契约 | Step 7 | `REQ-009`, `REQ_019`, `REQ-020` | `TEST-006` |
| CREATE | `.../rag/metadata/RagMetadataKeys.java` | 无同类 | 保留键常量与校验 | 元数据规范 | Step 7 | `REQ-011`, `REQ-020` | `TEST-006`, `TEST-008` |
| CREATE | `.../rag/converter/RagChunkConverter.java` | `ResearchEventConverter` 的 `@Mapper` + `Mappers.getMapper` + `BaseConverter` | 分块与 `Document` 双向映射 | 唯一转换器 | Step 7 | `REQ-011` | `TEST-008` |
| CREATE | `.../rag/api/RagIngestionService.java` | 无同类 | 摄取公开接口 | 服务入口 | Step 7 | `REQ-009`, `REQ-010` | `TEST-006`, `TEST-010` |
| CREATE | `.../rag/execution/RagIngestionServiceImpl.java` | 无同类 | 摄取编排 | 服务实现 | Step 7 | `REQ-009`-`REQ-011`, `REQ-020` | `TEST-006`, `TEST-007`, `TEST-010`, `TEST-025` |
| CREATE | `.../rag/model/{RagRetrievalQuery,RagRetrievedChunkBO}.java` | agent archetype 的 record 写法 | 检索查询与结果 | 检索契约 | Step 8 | `REQ-007` | `TEST-016` |
| CREATE | `.../rag/api/RagRetrievalService.java` | 无同类 | 检索公开接口 | 服务入口 | Step 8 | `REQ-007` | `TEST-017` |
| CREATE | `.../rag/execution/RagRetrievalServiceImpl.java` | 无同类 | 检索编排与强制过滤 | 服务实现 | Step 8 | `REQ-007` | `TEST-016`, `TEST-017` |
| CREATE | `.../rag/storage/{RagDocumentStorage,RagDocumentStorageTypeEnum,RagStoredObjectBO}.java` | 无同类 | 存储 SPI、类型枚举与结果载体 | 可扩展点 | Step 9 | `REQ-012` | `TEST-020` |
| CREATE | `.../rag/storage/LocalFileSystemRagDocumentStorage.java` | 无同类；只用 JDK `java.nio` | 本地文件系统默认实现 | 默认实现 | Step 9 | `REQ-012` | `TEST-020` |
| CREATE | `.../rag/execution/RagVectorStoreProbe.java` | 无同类 | 可选启动期往返校验 | 启动期校验 | Step 10 | `REQ-018` | `TEST-018` |
| CREATE | `.../test/java/.../support/{FakeEmbeddingModel,FakeVectorStore,FakeRagDocumentStorage}.java` | agent archetype 的 fake 与 stub 写法 | 三个测试替身 | 离线验证基础 | Step 3-9（按需） | `REQ-017` | 各 `TEST-*` |
| CREATE | `.../test/java/.../contract/RagComponentContractTest.java` | `AgentFlowComponentContractTest` 先例 | 包结构、依赖、命名、时间与日志的静态门禁 | 契约测试 | Step 11 | `REQ-001`, `REQ-014`, `REQ-015` | `TEST-021`, `TEST-022` |
| CREATE | 其余 12 个测试类（见 `§5` 树） | `agent-flow-starter` 的每包一个 `*Test` 布局 | 21 个行为测试 | 行为验证 | Step 2-12 | `REQ-002`-`REQ_020` | 各 `TEST-*` |
| MODIFY | `egon-cola-components/egon-cola-components-architecture.md` | `§3.1` 组件清单、`§5` starter 定位、`§11` BOM 规范 | 新增 RAG 组件条目与其扩展点、边界说明 | 组件文档 | Step 12 | `REQ-016` | 文档评审 |
| CREATE | `egon-cola-component-rag-starter/README.md`、`README.zh-CN.md` | `agent-flow-starter/README.md` 与 `README.zh-CN.md` 的章节结构 | 简介、版本矩阵、依赖方式、宿主提供 Bean、配置、Java API、边界与日志、升级与验证门禁 | 使用文档 | Step 12 | `REQ-018`（探针默认关闭的说明） | 文档评审 |

## 6. Prerequisites, Constraints, and Plan Clarifications

### 6.1 Repository and worktree baseline

- 仓库根 `/Users/mario/SelfProject/Egon-COLA`；当前分支 `docs/rag-starter-spec`，基线提交 `1009fd3c6`。
- 仓库无 `AGENTS.md`；本 Plan 遵守 `egon-cola-components-architecture.md` §5、`§13.1` 的 starter 约束。
- 工作区在本 Plan 创建前仅含两份新 Spec；实施开始前应确认没有其它未跟踪修改，且不得把 Spec/Plan 文件混入代码提交。
- 生成物与 `target/` 不入库；提交路径一律为 `§5` 列出的源码与文档路径。

### 6.2 Build, test, and environment prerequisites

| Concern | Exact command/source | Required state | Validation boundary |
| --- | --- | --- | --- |
| 构建工具 | `./mvnw -B -ntp -pl egon-cola-components/egon-cola-component-rag-starter -am clean verify`（`README.md` 第 57 行说明使用仓库自带 Maven Wrapper） | 本地仓库已安装父 POM 与 `common-core`（`./mvnw -B -ntp -pl egon-cola-components -am install -DskipTests` 或既有完整安装） | 静态 + 模块 |
| 父 Reactor 构建 | `./mvnw -B -ntp -f egon-cola-components/pom.xml clean install -DskipTests` | 同上级 | 跨模块 |
| Java | 21（父 POM `<java.version>`） | 本机 JDK 21 | 静态 |
| 网络 | 首次构建需要拉取 `spring-ai-*`、`mapstruct` 等依赖；测试阶段不需要网络 | — | 运行期无外部依赖 |

### 6.3 Immutable constraints and approved decisions

- **Rule 11 例外**：Spec A `DEC-002`，用户 2026-09-10 批准。组件采用 component-library 扁平单模块 profile；不得引入 `biz.*`、archetype 模块树或新层。
- **MapStruct 引入**：Spec A `DEC-007`，用户 2026-09-10 批准。这是仓库组件中第一例运行时 MapStruct；父 POM 的两条 `dependencyManagement` 是必要变更。
- **不创建供应商 Bean**：Spec A `REQ-003`、`DEC-003`。组件源码与配置键中不得出现 `base-url`、`api-key`、`apiKey`、`OpenAiApi`。
- **不落 schema**：Spec A `REQ-014`、`DEC-004`。组件不引入 JDBC 驱动、Flyway、MyBatis、Redis、AMQP 或调度器。
- **探针默认关闭**：Spec A `DEC-008`、`REQ-018`。默认配置下启动不得产生任何模型调用。
- **不修改既有组件**：除父 POM 的模块登记与 `dependencyManagement`、BOM 的一条导出、组件架构文档的新增条目外，不得改动任何既有文件。

### 6.4 Plan Clarifications

| ID | Small implementation inference | Repository evidence | Why semantics are unchanged | Impact if wrong |
| --- | --- | --- | --- | --- |
| `PLAN-CLAR-001` | 包根取 `top.egon.cola.component.rag`，与 `top.egon.cola.component.agentflow` 同构 | `agent-flow-starter/src/main/java/top/egon/cola/component/agentflow/` | Spec A `ASM-001` 已给出同样的推断；只影响新模块内部 | 包名需重命名，无外部影响 |
| `PLAN-CLAR-002` | 契约测试命名 `RagComponentContractTest` 并置于 `contract/` 包 | `agent-flow-starter/src/test/java/top/egon/cola/component/agentflow/contract/AgentFlowComponentContractTest.java` | Spec A 只要求"一个契约测试"，未指定路径与类名 | 重命名即可 |
| `PLAN-CLAR-003` | 测试替身置于 `support/` 包，命名 `Fake*` | 现有组件测试中无统一替身包；`spring-boot-starter-test` 已提供 Mockito | 只影响测试内部布局 | 移动文件即可 |
| `PLAN-CLAR-004` | `TokenTextSplitter` 经 `spring-ai-commons` 传递依赖进入，模块 POM 不显式声明该坐标 | `spring-ai-vector-store` 依赖 `spring-ai-commons`（`javap` 与 jar 内容已核实） | 不改变依赖解析结果；显式声明会多一条不必要的坐标 | 若传递依赖被裁剪，需在模块 POM 显式声明 `spring-ai-commons` |
| `PLAN-CLAR-005` | 10 个异常子类逐文件创建，但形状完全一致（继承 `RagException`、携带 `code`/`safeMessage`），在 `§7` 的 Step 2 中以一个基础块 + 一张子类表说明 | `ResearchErrorCodeEnum` 与 `DeepResearchApplicationException` 的既有风格 | 每个类只有一个构造器与两个访问器，不承载行为差异 | 子类若需要不同字段，需回到 Spec 补设计 |

## 7. Ordered File-by-file Implementation Steps

### Step 1 — 建立可构建的组件模块并登记交付面

- Requirements: `REQ-001`, `REQ-016`, `REQ-014`
- Dependencies: `None`
- Baseline state: components 父 Reactor 有 10 个模块，`egon-cola-components-bom` 导出 20 条 `top.egon` 依赖，均构建通过。不存在 `egon-cola-component-rag-starter`。
- Observable outcome: `egon-cola-component-rag-starter` 出现在父 POM 模块清单与 BOM 导出中，并可独立构建出 JAR。
- End state: 模块坐标、目录骨架、`lombok.config` 与依赖清单就绪；无任何生产行为，也无测试。
- Test-first gate: `Not applicable` — 本 Step 只建立编译宿主（父 POM 登记、模块 POM、`lombok.config`、空包），没有任何可被测试观察的行为；其证明是 `mvn verify` 成功与 BOM 可解析，见 `§8` 的构建门。
- Manual Checks: `MC-ARCH-001`, `MC-REUSE-001`, `MC-DEP-001`, `MC-SCOPE-001`, `MC-TEST-001`
- Literal Rules: `Rule 11`
- Ordered files:

#### File 1 — `MODIFY egon-cola-components/pom.xml`

- Purpose: 登记新模块并管理 MapStruct 坐标。
- Symbols: 父 POM 的 modules 段 内新增 `一条新的 module 登记：egon-cola-component-rag-starter`；`<properties>` 新增 `mapstruct.version=1.6.3`、`mapstruct-plus.version=1.5.1`；`<dependencyManagement>` 新增 `org.mapstruct:mapstruct` 与 `io.github.linpeilie:mapstruct-plus` 两条。
- Repository evidence: 现有 父 POM 的 modules 段 逐项列出 10 个模块，顺序为 bom → common → … → bytecode；`<properties>` 已有 `spring-ai.version`、`google-adk.version`；版本值取自 `egon-cola-component-common/egon-cola-component-common-core/pom.xml` 的 `<mapstruct.version>` 与 `<mapstruct-plus.version>`。
- Dependencies and consumers: 模块必须在父 Reactor 内才能 `-am` 构建；`dependencyManagement` 被新模块 POM 消费。
- Why now: 模块不存在则后续所有文件无处安放。
- Contract/signature changes: 父 POM 的 modules 段由 10 项变为 11 项；新增两条受管坐标（不改变任何既有模块的解析结果）。
- Input/output and state mapping: 无运行期映射；构建期由 Reactor 解析模块顺序。
- Error and edge behavior: 顺序错误会导致 Reactor 解析失败；模块名拼写错误会导致 `-pl` 选择失败。`mapstruct-plus` 在本 Plan 中不被使用（Spec A 只要求 MapStruct），登记它是为了与 `common-core` 的版本来源一致——**若实施时确认不需要，可只登记 `mapstruct` 一条并在 `§11` 记录偏离**。
- Implementation pseudocode:

```xml
<!-- egon-cola-components/pom.xml -->
<properties>
    <!-- 既有属性保持不变 -->
    <mapstruct.version>1.6.3</mapstruct.version>          <!-- 取自 common-core 的同名属性 -->
    <mapstruct-plus.version>1.5.1</mapstruct-plus.version><!-- 取自 common-core 的同名属性 -->
</properties>

modules 段
    <!-- 既有 10 项顺序不变 -->
    一条新的 module 登记：egon-cola-component-rag-starter       <!-- 追加在 bytecode 之后 -->
</modules>

<dependencyManagement>
    <dependencies>
        <!-- 既有条目保持不变 -->
        <dependency>
            <groupId>org.mapstruct</groupId>
            <artifactId>mapstruct</artifactId>
            <version>${mapstruct.version}</version>
        </dependency>
        <dependency>
            <groupId>io.github.linpeilie</groupId>
            <artifactId>mapstruct-plus</artifactId>
            <version>${mapstruct-plus.version}</version>
        </dependency>
    </dependencies>
</dependencyManagement>
```

- Standards impact: Step 1 建立编译宿主，不引入 Java 业务类型；包结构断言由 Step 11 的 `MC-ARCH-001` 覆盖。构建期变更由 `MC-DEP-001`（MapStruct 坐标登记）与 `MC-SCOPE-001`（写入范围限于模块登记）覆盖。
- Literal rule enforcement: `Rule 11` — 目标文件全部位于 `top.egon.cola.component.rag` 前缀之下，不引入 `biz.*` 与 archetype 模块树。
- Verification contribution: `TEST-024` 断言模块清单含新模块且既有模块不变。
- After this file: 父 POM 引用尚不存在的模块目录，Reactor 会解析失败——这是 File 2/3 之前的预期中间态，本 Step 的三份文件必须一起提交。

#### File 2 — `MODIFY egon-cola-components/egon-cola-components-bom/pom.xml`

- Purpose: 导出新组件，使消费方可无版本引入。
- Symbols: `<dependencyManagement>` 新增一条 `top.egon:egon-cola-component-rag-starter:${project.version}`。
- Repository evidence: 现有 20 条导出的写法一致，均为 `top.egon` + `${project.version}`，无 `<scope>`。
- Dependencies and consumers: 消费方（Spec B 的 agent archetype）将无版本引入该坐标。
- Why now: 与 File 1 同一变更面，缺一不可。
- Contract/signature changes: BOM 导出行由 20 条变为 21 条；不导出任何三方坐标。
- Input/output and state mapping: 无运行期映射。
- Error and edge behavior: 遗漏则消费方无法在当前版本下解析依赖；导出顺序不影响解析。
- Implementation pseudocode:

```xml
<!-- egon-cola-components-bom/pom.xml -->
<dependencyManagement>
    <dependencies>
        <!-- 既有 20 条保持不变 -->
        <dependency>
            <groupId>top.egon</groupId>
            <artifactId>egon-cola-component-rag-starter</artifactId>
            <version>${project.version}</version>
        </dependency>
    </dependencies>
</dependencyManagement>
```

- Standards impact: Step 1 建立编译宿主，不引入 Java 业务类型；包结构断言由 Step 11 的 `MC-ARCH-001` 覆盖。构建期变更由 `MC-DEP-001`（MapStruct 坐标登记）与 `MC-SCOPE-001`（写入范围限于模块登记）覆盖。
- Literal rule enforcement: `Rule 11` — 目标文件全部位于 `top.egon.cola.component.rag` 前缀之下，不引入 `biz.*` 与 archetype 模块树。
- Verification contribution: `TEST-024` 断言 BOM 只新增一条导出。
- After this file: 交付面登记完成；新坐标尚未有实现。

#### File 3 — `CREATE egon-cola-component-rag-starter/pom.xml`

- Purpose: 声明模块坐标、父 POM、依赖与注解处理器。
- Symbols: `<artifactId>egon-cola-component-rag-starter</artifactId>`、`<packaging>jar</packaging>`、依赖清单。
- Repository evidence: `egon-cola-component-agent-flow-starter/pom.xml` 逐项同构——`parent` 为 `top.egon:egon-cola-components-parent:5.4.0` 且 `<relativePath>../pom.xml</relativePath>`；依赖顺序为 Egon 组件 → Spring Boot → `optional` 工具 → Lombok `provided` → 测试。
- Dependencies and consumers: 被父 Reactor 构建；被 BOM 导出；被消费方引入。
- Why now: 模块构建的前提。
- Contract/signature changes: 新增模块 GAV。
- Input/output and state mapping: 依赖清单精确对应 Spec A `§8.3`：

| 依赖 | groupId | scope | 引入 Step |
| --- | --- | --- | --- |
| `egon-cola-component-common-core` | `top.egon` | compile | Step 2（`ValidationUtils`、`BaseConverter`） |
| `spring-boot-starter` | `org.springframework.boot` | compile | Step 2 |
| `spring-boot-autoconfigure` | `org.springframework.boot` | compile | Step 2 |
| `spring-boot-starter-validation` | `org.springframework.boot` | compile | Step 2 |
| `spring-ai-model` | `org.springframework.ai` | compile | Step 3（`EmbeddingModel`） |
| `spring-ai-vector-store` | `org.springframework.ai` | compile | Step 7/8（`VectorStore`、`Document`、`SearchRequest`） |
| `mapstruct` | `org.mapstruct` | compile | Step 7 |
| `spring-ai-pdf-document-reader` | `org.springframework.ai` | `optional` | Step 5 |
| `spring-ai-tika-document-reader` | `org.springframework.ai` | `optional` | Step 5 |
| `micrometer-core` | `io.micrometer` | `optional` | Step 11 |
| `spring-boot-configuration-processor` | `org.springframework.boot` | `optional` | Step 2 |
| `lombok` | `org.projectlombok` | `provided` | Step 2 |
| `spring-boot-starter-test` | `org.springframework.boot` | `test` | 全部测试 Step |

- Error and edge behavior: 缺少 `spring-boot-autoconfigure` 会导致 `@AutoConfiguration` 不可解析；把可选解析器写成 compile scope 会让每个消费方都被迫带上 Tika。
- Implementation pseudocode:

```xml
<project>
    <parent>
        <groupId>top.egon</groupId>
        <artifactId>egon-cola-components-parent</artifactId>
        <version>5.4.0</version>
        <relativePath>../pom.xml</relativePath>
    </parent>
    <artifactId>egon-cola-component-rag-starter</artifactId>
    <packaging>jar</packaging>
    <name>egon-cola-component-rag-starter</name>
    <description>Spring Boot starter for host-wired RAG engine mechanics.</description>
    <dependencies>
        <!-- Egon 基础 -->
        <dependency>top.egon : egon-cola-component-common-core : ${project.version}</dependency>
        <!-- Spring Boot -->
        <dependency>org.springframework.boot : spring-boot-starter</dependency>
        <dependency>org.springframework.boot : spring-boot-autoconfigure</dependency>
        <dependency>org.springframework.boot : spring-boot-starter-validation</dependency>
        <!-- Spring AI 抽象（版本由父 POM 的 spring-ai-bom 管理） -->
        <dependency>org.springframework.ai : spring-ai-model</dependency>
        <dependency>org.springframework.ai : spring-ai-vector-store</dependency>
        <!-- 转换（Rule 3 强制；版本由父 POM 新增的 dependencyManagement 管理） -->
        <dependency>org.mapstruct : mapstruct</dependency>
        <!-- 可选解析器：缺失不影响启动 -->
        <dependency>org.springframework.ai : spring-ai-pdf-document-reader : optional=true</dependency>
        <dependency>org.springframework.ai : spring-ai-tika-document-reader : optional=true</dependency>
        <!-- 可观测性：缺席时完全跳过 -->
        <dependency>io.micrometer : micrometer-core : optional=true</dependency>
        <dependency>org.springframework.boot : spring-boot-configuration-processor : optional=true</dependency>
        <dependency>org.projectlombok : lombok : provided=true</dependency>
        <dependency>org.springframework.boot : spring-boot-starter-test : scope=test</dependency>
    </dependencies>
    <build>
        <plugins>
            <plugin>maven-compiler-plugin
                <annotationProcessorPaths>
                    lombok, lombok-mapstruct-binding 0.2.0, mapstruct-processor ${mapstruct.version}
                </annotationProcessorPaths>
            </plugin>
        </plugins>
    </build>
</project>
```

- Standards impact: Step 1 建立编译宿主，不引入 Java 业务类型；包结构断言由 Step 11 的 `MC-ARCH-001` 覆盖。构建期变更由 `MC-DEP-001`（MapStruct 坐标登记）与 `MC-SCOPE-001`（写入范围限于模块登记）覆盖。
- Literal rule enforcement: `Rule 11` — 目标文件全部位于 `top.egon.cola.component.rag` 前缀之下，不引入 `biz.*` 与 archetype 模块树。
- Verification contribution: 模块可构建；`REQ-014` 由依赖清单本身证明（无 JDBC/Flyway/Redis/AMQP/调度器）。
- After this file: 模块 POM 就绪但无源文件，`mvn verify` 可成功产出空 JAR。

#### File 4 — `CREATE egon-cola-component-rag-starter/lombok.config`

- Purpose: 保证 `@Qualifier` 与 `@Value` 复制到 Lombok 生成的构造参数。
- Symbols: `config.stopBubbling`、`lombok.copyableAnnotations` 两条、`addLombokGeneratedAnnotation`、`anyConstructor.addConstructorProperties`、`data.flagUsage`、`val.flagUsage`。
- Repository evidence: `egon-cola-component-agent-flow-starter/lombok.config` 逐行同构。
- Dependencies and consumers: 被模块内全部 Lombok 注解类消费；Rule 4 的 `@Qualifier` 传递依赖它。
- Why now: 必须在第一个使用 `@RequiredArgsConstructor` + `@Qualifier` 的类之前存在。
- Contract/signature changes: 无；纯构建期配置。
- Input/output and state mapping: 无运行期映射。
- Error and edge behavior: 缺少 `copyableAnnotations += Qualifier` 会导致构造参数丢失 `@Qualifier`，装配在存在多个同类型 Bean 时失败。
- Implementation pseudocode:

```properties
config.stopBubbling = true
lombok.copyableAnnotations += org.springframework.beans.factory.annotation.Qualifier
lombok.copyableAnnotations += org.springframework.beans.factory.annotation.Value
lombok.addLombokGeneratedAnnotation = true
lombok.anyConstructor.addConstructorProperties = true
lombok.data.flagUsage = warning
lombok.val.flagUsage = warning
```

- Standards impact: Step 1 建立编译宿主，不引入 Java 业务类型；包结构断言由 Step 11 的 `MC-ARCH-001` 覆盖。构建期变更由 `MC-DEP-001`（MapStruct 坐标登记）与 `MC-SCOPE-001`（写入范围限于模块登记）覆盖。
- Literal rule enforcement: `Rule 11` — 目标文件全部位于 `top.egon.cola.component.rag` 前缀之下，不引入 `biz.*` 与 archetype 模块树。
- Verification contribution: `TEST-011`-`TEST-015` 的装配测试间接证明；`TEST-021` 断言文件存在且含 `Qualifier` 行。
- After this file: 模块具备 Rule 4 所需的 Lombok 传播配置。

#### File 5 — `CREATE egon-cola-component-rag-starter/src/main/java/top/egon/cola/component/rag/package-info.java`

- Purpose: 声明组件根包的职责边界。
- Symbols: `package top.egon.cola.component.rag;` 与 Javadoc。
- Repository evidence: `agent-flow-starter` 每个包均有 `package-info.java`。
- Dependencies and consumers: 仅文档；被 `TEST-021` 的包结构断言观察。
- Why now: 根包是后续全部子包的前缀。
- Contract/signature changes: 无。
- Input/output and state mapping: 无。
- Error and edge behavior: 无。
- Implementation pseudocode:

```java
/**
 * RAG engine mechanics: extraction, chunking, embedding, retrieval and document storage
 * extension points. The component owns no provider configuration, no schema and no HTTP surface.
 */
package top.egon.cola.component.rag;
```

- Standards impact: Step 1 建立编译宿主，不引入 Java 业务类型；包结构断言由 Step 11 的 `MC-ARCH-001` 覆盖。构建期变更由 `MC-DEP-001`（MapStruct 坐标登记）与 `MC-SCOPE-001`（写入范围限于模块登记）覆盖。
- Literal rule enforcement: `Rule 11` — 目标文件全部位于 `top.egon.cola.component.rag` 前缀之下，不引入 `biz.*` 与 archetype 模块树。
- Verification contribution: 编译；`TEST-021` 的包清单断言。
- After this file: 根包就绪，`Rule 11` 的扁平结构从第一个文件起即成立。

- Validation working directory: `/Users/mario/SelfProject/Egon-COLA`
- Verification command: `./mvnw -B -ntp -pl egon-cola-components -am clean install -DskipTests`
- Expected result: 构建成功；`egon-cola-component-rag-starter` 出现在构建的模块清单中；产物 `target/egon-cola-component-rag-starter-5.4.0.jar` 存在。
- Failure returns to: File 1（模块名或顺序错误）、File 3（依赖坐标或注解处理器错误）。
- Completion criteria: 模块进入父 Reactor、BOM 导出新增一条、模块可独立构建，且既有 10 个模块构建结果不变。
- Rollback: 回退这三份 POM 变更与模块目录；无数据或契约残留。
- Commit paths: `egon-cola-components/pom.xml`; `egon-cola-components/egon-cola-components-bom/pom.xml`; `egon-cola-component-rag-starter/pom.xml`; `egon-cola-component-rag-starter/lombok.config`; `egon-cola-component-rag-starter/src/main/java/top/egon/cola/component/rag/package-info.java`
- Commit: `feat(components): register the rag starter module`

### Step 2 — 默认关闭的配置绑定与装配骨架

- Requirements: `REQ-002`, `REQ-012`, `REQ-018`
- Dependencies: Step 1
- Baseline state: 模块可构建但无任何源文件；组件默认关闭与严格绑定尚不存在。
- Observable outcome: 未设置 `egon.cola.component.rag.enabled=true` 时，容器中不存在任何 `rag*` Bean；设为 `true` 但出现未知键或约束失败时，上下文启动失败并指出键名。
- End state: `RagProperties` 及其四个嵌套 record、`RagException` 家族、`RagAutoConfiguration` 的启用开关与属性 Bean、`AutoConfiguration.imports`、配置元数据全部就绪；服务与扩展点尚不存在。
- Test-first gate: `Required` — `RagAutoConfigurationTest` 与 `RagPropertiesBindingTest` 首次运行时编译失败（`RagAutoConfiguration`、`RagProperties` 类不存在），这是"缺失行为"而非夹具问题。
- Manual Checks: `MC-ARCH-001`, `MC-REUSE-001`, `MC-NAME-001`, `MC-MODEL-001`, `MC-VALID-001`, `MC-LOG-001`, `MC-BEAN-001`, `MC-CONFIG-001`, `MC-SCOPE-001`, `MC-TEST-001`
- Literal Rules: `Rule 1`, `Rule 2`, `Rule 3`, `Rule 4`, `Rule 7`, `Rule 10`, `Rule 11`
- Ordered files:

#### File 1 — `CREATE src/test/java/top/egon/cola/component/rag/autoconfigure/RagAutoConfigurationTest.java`

- Purpose: 定义"默认关闭"与"启用后严格绑定"两条行为，它们在本 Step 之前不存在。
- Symbols: `creates_no_rag_bean_when_disabled()`、`creates_no_rag_bean_when_enabled_is_false()`、`fails_when_unknown_key_present()`、`fails_when_required_value_missing()`、`fails_when_max_top_k_smaller_than_default()`。
- Repository evidence: `agent-flow-starter/src/test/java/.../autoconfigure/AgentFlowAutoConfigurationTest.java` 使用 `ApplicationContextRunner` 施加 `withPropertyValues` 并断言 Bean 存在性。
- Dependencies and consumers: 消费 `RagAutoConfiguration` 与 `RagProperties`；断言容器中 `ragProperties` Bean 的有无。
- Why now: 这是本 Step 的 RED 契约；没有它，配置类可以写成任意形状。
- Contract/signature changes: 引入将来由 File 2/3/5 提供的 `RagAutoConfiguration` 与 `RagProperties`。
- Input/output and state mapping: 属性 Map → `ApplicationContextRunner` → 上下文启动结果与 Bean 集合。
- Error and edge behavior: 未知键必须失败而非忽略；`default-top-k > max-top-k` 必须失败；`enabled` 缺省必须是 `false`。
- Standards impact: `MC-CONFIG-001`（键集与严格绑定）、`MC-VALID-001`（`@Validated` 与 Jakarta 约束在配置载体上生效）、`MC-SCOPE-001`、`MC-TEST-001`。
- Literal rule enforcement: `Rule 2`（配置载体作为一层交接边界，必须有 Jakarta 约束）、`Rule 7`（键集由 `RagProperties` 固定）、`Rule 11`（测试位于模块 `src/test`，不新增测试模块）。
- Implementation pseudocode:

```java
class RagAutoConfigurationTest {
    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(RagAutoConfiguration.class));

    @Test void creates_no_rag_bean_when_disabled() {
        runner.run(context -> assertThat(context).doesNotHaveBean("ragProperties"));
    }

    @Test void creates_no_rag_bean_when_enabled_is_false() {
        runner.withPropertyValues("egon.cola.component.rag.enabled=false")
              .run(context -> assertThat(context).doesNotHaveBean("ragProperties"));
    }

    @Test void fails_when_unknown_key_present() {
        runner.withPropertyValues("egon.cola.component.rag.enabled=true",
                                  "egon.cola.component.rag.unexpected-key=1")
              .run(context -> assertThat(context).hasFailed()
                      .getFailure().hasMessageContaining("unexpected-key"));
    }

    @Test void fails_when_required_value_missing() {
        runner.withPropertyValues("egon.cola.component.rag.enabled=true")
              .run(context -> assertThat(context).hasFailed());
    }

    @Test void fails_when_max_top_k_smaller_than_default() {
        runner.withPropertyValues("egon.cola.component.rag.enabled=true", /* 必填键 */,
                                  "egon.cola.component.rag.retrieval.default-top-k=50",
                                  "egon.cola.component.rag.retrieval.max-top-k=10")
              .run(context -> assertThat(context).hasFailed());
    }
}
```

- Verification contribution: `§8` 的 RED 门；五个用例分别锁定默认关闭、显式关闭、未知键、缺必填、跨字段约束。
- After this file: 测试编译失败（生产类缺失），RED 原因正确。

#### File 2 — `CREATE src/test/java/top/egon/cola/component/rag/autoconfigure/RagPropertiesBindingTest.java`

- Purpose: 锁定默认值与键集，防止后续 Step 静默改变默认值。
- Symbols: `applies_documented_defaults()`、`normalizes_embedding_model_keys()`、`rejects_duplicate_normalized_model_keys()`。
- Repository evidence: `agent-flow-starter/src/test/java/.../autoconfigure/AgentFlowPropertiesBindingTest.java` 的绑定断言风格。
- Dependencies and consumers: 消费 `RagProperties` 的紧凑构造器。
- Why now: 默认值是 `§9.2.6` 的契约，必须在实现前固定。
- Contract/signature changes: 无新增；断言既有契约。
- Input/output and state mapping: 属性 Map → `RagProperties` → 逐字段断言默认值（`enabled=false`、`dimensions` 必填、`storage.type=LOCAL`、`storage.local.root=./data/rag-documents`、`retrieval.default-top-k=8`、`retrieval.max-top-k=50`、`validation.probe-on-startup=false`、`default-embedding-model` 取注册表唯一键）。
- Error and edge behavior: 逻辑名 key 需 trim；两个 key trim 后相同时抛 `RagConfigurationException`。
- Standards impact: `MC-MODEL-001`（record + 紧凑构造器规范化）、`MC-CONFIG-001`、`MC-SCOPE-001`、`MC-TEST-001`。
- Literal rule enforcement: `Rule 3`（简单对象用 record 且用紧凑构造器做规范化）、`Rule 11`。
- Implementation pseudocode:

```java
class RagPropertiesBindingTest {
    @Test void applies_documented_defaults() {
        properties = bind(minimalEnabledKeySet())
        assertThat(properties.storage().type()).isEqualTo(LOCAL)
        assertThat(properties.storage().local().root()).isEqualTo("./data/rag-documents")
        assertThat(properties.retrieval().defaultTopK()).isEqualTo(8)
        assertThat(properties.retrieval().maxTopK()).isEqualTo(50)
        assertThat(properties.validation().probeOnStartup()).isFalse()
    }

    @Test void normalizes_embedding_model_keys() { /* " openai-small " -> "openai-small" */ }

    @Test void rejects_duplicate_normalized_model_keys() {
        assertThatThrownBy(() -> bind(twoKeysDifferingOnlyByWhitespace()))
            .isInstanceOf(RagConfigurationException.class)
    }
}
```

- Verification contribution: GREEN 后锁定默认值与规范化规则。
- After this file: 与 File 1 一同编译失败，RED 原因正确。

#### File 3 — `CREATE src/main/java/top/egon/cola/component/rag/exception/RagException.java`

- Purpose: 建立组件统一失败基类与稳定安全消息契约。
- Symbols: `class RagException extends RuntimeException`，字段 `code` 与 `safeMessage`，构造器 `(String code, String safeMessage)` 与 `(String code, String safeMessage, Throwable cause)`。
- Repository evidence: `DeepResearchApplicationException` 与 `ResearchErrorCodeEnum` 的既有错误码风格。
- Dependencies and consumers: 被 `§5` 中全部 10 个异常子类继承；被装配校验与实现类抛出；被使用方（Spec B）捕获。
- Why now: 配置校验需要抛出确定类型，因此必须先于 `RagAutoConfiguration`。
- Contract/signature changes: 新增公开异常基类。
- Input/output and state mapping: 无状态变更；`code` 是稳定机器码字符串，`safeMessage` 不含供应商报文、文档内容或密钥。
- Error and edge behavior: 直接继承 `RuntimeException`（非受检），避免在每个签名上声明；`cause` 保留以便日志记录类型而不打印报文。
- Standards impact: `MC-NAME-001`（`*Exception` 行为后缀）、`MC-LOG-001`（消息安全）、`MC-SCOPE-001`、`MC-TEST-001`。
- Literal rule enforcement: `Rule 1`（`Exception` 后缀）、`Rule 11`。
- 10 个子类的固定形状（每个一个文件，继承 `RagException`，只声明构造器）：

| 子类 | 构造器签名 |
| --- | --- |
| `RagConfigurationException` | `(String safeMessage)` / `(String safeMessage, Throwable cause)` |
| `RagValidationException` | `(String safeMessage)` |
| `RagExtractorMissingException` | `(String safeMessage)` |
| `RagExtractorConflictException` | `(String safeMessage)` |
| `RagExtractionException` | `(String safeMessage, Throwable cause)` |
| `RagChunkingException` | `(String safeMessage, Throwable cause)` |
| `RagModelNotRegisteredException` | `(String safeMessage)` |
| `RagEmbeddingException` | `(String safeMessage, Throwable cause)` |
| `RagVectorStoreException` | `(String safeMessage, Throwable cause)` |
| `RagStorageException` | `(String safeMessage, Throwable cause)` |

  每个子类内部固定调用 `super("<稳定机器码>", safeMessage[, cause])`，机器码分别为 `RAG_CONFIGURATION`、`RAG_VALIDATION`、`RAG_EXTRACTOR_MISSING`、`RAG_EXTRACTOR_CONFLICT`、`RAG_EXTRACTION`、`RAG_CHUNKING`、`RAG_MODEL_NOT_REGISTERED`、`RAG_EMBEDDING`、`RAG_VECTOR_STORE`、`RAG_STORAGE`。

- Implementation pseudocode:

```java
@Slf4j
public class RagException extends RuntimeException {
    private final String code;
    private final String safeMessage;

    public RagException(String code, String safeMessage) {
        super(safeMessage);
        this.code = code;
        this.safeMessage = safeMessage;
    }

    public RagException(String code, String safeMessage, Throwable cause) {
        super(safeMessage, cause);
        this.code = code;
        this.safeMessage = safeMessage;
    }

    public String code() { return code; }
    public String safeMessage() { return safeMessage; }
}

// 子类示例（其余九个同形，仅机器码与签名不同）
public class RagModelNotRegisteredException extends RagException {
    public RagModelNotRegisteredException(String safeMessage) {
        super("RAG_MODEL_NOT_REGISTERED", safeMessage);
    }
}
```

- Verification contribution: 编译；`TEST-021` 断言全部异常位于 `exception` 包且带 `Exception` 后缀。
- After this file: 异常家族就绪，配置校验可以抛出确定类型。

#### File 4 — `CREATE src/main/java/top/egon/cola/component/rag/autoconfigure/RagEmbeddingModelProperties.java`

- Purpose: 承载单个逻辑嵌入模型的配置（Bean 名）。
- Symbols: `record RagEmbeddingModelProperties(@NotBlank @Qualifier… )`——实际为 `record RagEmbeddingModelProperties(String embeddingModelBeanName)`，紧凑构造器 `trim` 并拒绝空白。
- Repository evidence: `AgentFlowConfigDTO` 的配置载体风格（record + Jakarta 注解）。
- Dependencies and consumers: 被 `RagProperties.embeddingModels` 的值类型消费；被 Step 3 的注册表读取。
- Why now: `RagProperties` 引用它，必须先存在。
- Contract/signature changes: 新增配置键 `egon.cola.component.rag.embedding-models.<name>.embedding-model-bean-name`。
- Input/output and state mapping: 配置键 → record 字段；空白值 → `RagConfigurationException`。
- Error and edge behavior: `null` 或空白 Bean 名必须失败而非延迟到运行期。
- Standards impact: `MC-MODEL-001`、`MC-CONFIG-001`、`MC-VALID-001`、`MC-SCOPE-001`、`MC-TEST-001`。
- Literal rule enforcement: `Rule 3`（简单对象用 record + 紧凑构造器）、`Rule 7`（键结构固定）、`Rule 11`。
- Implementation pseudocode:

```java
public record RagEmbeddingModelProperties(@NotBlank String embeddingModelBeanName) {
    public RagEmbeddingModelProperties {
        embeddingModelBeanName = embeddingModelBeanName == null ? null : embeddingModelBeanName.trim();
        if (embeddingModelBeanName == null || embeddingModelBeanName.isEmpty()) {
            throw new RagConfigurationException("embedding-model-bean-name must not be blank");
        }
    }
}
```

- Verification contribution: `TEST-013` 的键覆盖断言。
- After this file: 注册表配置载体就绪。

#### File 5 — `CREATE src/main/java/top/egon/cola/component/rag/autoconfigure/{RagStorageProperties,RagRetrievalProperties,RagValidationProperties}.java`

- Purpose: 承载存储、检索与校验三组配置键。
- Symbols: `record RagStorageProperties(RagDocumentStorageTypeEnum type, @Valid RagLocalStorageProperties local)`；`record RagRetrievalProperties(int defaultTopK, int maxTopK)`；`record RagValidationProperties(boolean probeOnStartup)`。
- Repository evidence: `AgentFlowProperties` 的嵌套 record 组织方式。
- Dependencies and consumers: 被 `RagProperties` 组合；被 Step 9（存储）与 Step 10（探针）读取。
- Why now: 与 File 4 同批，`RagProperties` 依赖它们。
- Contract/signature changes: 新增 `egon.cola.component.rag.{storage.type,storage.local.root,retrieval.default-top-k,retrieval.max-top-k,validation.probe-on-startup}` 五个键。
- Input/output and state mapping: 缺省值在紧凑构造器内施加：`type=LOCAL`、`root=./data/rag-documents`、`defaultTopK=8`、`maxTopK=50`、`probeOnStartup=false`；跨字段规则 `defaultTopK <= maxTopK` 在此校验。
- Error and edge behavior: `maxTopK` 越界（1-200）、`defaultTopK` 越界（1 至 `maxTopK`）、`type` 为未知枚举值、`LOCAL` 时 `root` 空白——全部抛 `RagConfigurationException`。
- Standards impact: `MC-MODEL-001`、`MC-CONFIG-001`、`MC-VALID-001`、`MC-TIME-001`（若使用 `Duration` 则必须是 `java.time`；本组键无时间字段）、`MC-SCOPE-001`、`MC-TEST-001`。
- Literal rule enforcement: `Rule 3`、`Rule 7`、`Rule 11`。
- Implementation pseudocode:

```java
public record RagStorageProperties(@NotNull RagDocumentStorageTypeEnum type,
                                   @NotNull @Valid RagLocalStorageProperties local) {
    public RagStorageProperties {
        type = type == null ? RagDocumentStorageTypeEnum.LOCAL : type;
        local = local == null ? new RagLocalStorageProperties(null) : local;
    }
}

public record RagLocalStorageProperties(String root) {
    public RagLocalStorageProperties {
        root = (root == null || root.isBlank()) ? "./data/rag-documents" : root.trim();
    }
}

public record RagRetrievalProperties(int defaultTopK, int maxTopK) {
    public RagRetrievalProperties {
        maxTopK = maxTopK <= 0 ? 50 : maxTopK;
        defaultTopK = defaultTopK <= 0 ? 8 : defaultTopK;
        if (maxTopK > 200) throw new RagConfigurationException("max-top-k must not exceed 200");
        if (defaultTopK > maxTopK) throw new RagConfigurationException("default-top-k must not exceed max-top-k");
    }
}

public record RagValidationProperties(boolean probeOnStartup) { }
```

- Verification contribution: `TEST-013` 与 `TEST-012` 的约束用例。
- After this file: 四组配置载体齐备。

#### File 6 — `CREATE src/main/java/top/egon/cola/component/rag/autoconfigure/RagProperties.java`

- Purpose: 组合全部配置键并作为唯一配置入口。
- Symbols: `record RagProperties(boolean enabled, int dimensions, @NotBlank String vectorStoreBeanName, @NotEmpty @Valid Map<String, @Valid RagEmbeddingModelProperties> embeddingModels, String defaultEmbeddingModel, @NotNull @Valid RagStorageProperties storage, @NotNull @Valid RagRetrievalProperties retrieval, @NotNull @Valid RagValidationProperties validation)`。
- Repository evidence: `AgentFlowProperties` 的 record + 紧凑构造器 + `@Valid` 级联写法。
- Dependencies and consumers: 被 `RagAutoConfiguration` 绑定；被 Step 3-10 的服务与注册表读取。
- Why now: 全部后续 Step 的配置来源。
- Contract/signature changes: 引入 `egon.cola.component.rag` 前缀下的完整键集。
- Input/output and state mapping: 逻辑名 Map 的 key 做 `trim` 并检测规范化后重复；`defaultEmbeddingModel` 缺省取注册表唯一键（多于一个且未指定时留空，由 Step 3 的校验拒绝）。
- Error and edge behavior: `dimensions` ≤ 0 或 > 8192 失败；`embeddingModels` 为空失败；`vectorStoreBeanName` 空白失败。
- Standards impact: `MC-MODEL-001`、`MC-CONFIG-001`、`MC-VALID-001`、`MC-BEAN-001`（作为 `@Bean` 方法产出的类型）、`MC-SCOPE-001`、`MC-TEST-001`。
- Literal rule enforcement: `Rule 3`（record + 紧凑构造器规范化）、`Rule 7`（键集单一来源）、`Rule 11`。
- Implementation pseudocode:

```java
@Validated
public record RagProperties(boolean enabled,
                            int dimensions,
                            @NotBlank String vectorStoreBeanName,
                            @NotEmpty @Valid Map<String, @Valid RagEmbeddingModelProperties> embeddingModels,
                            String defaultEmbeddingModel,
                            @NotNull @Valid RagStorageProperties storage,
                            @NotNull @Valid RagRetrievalProperties retrieval,
                            @NotNull @Valid RagValidationProperties validation) {
    public RagProperties {
        vectorStoreBeanName = trimOrNull(vectorStoreBeanName);
        embeddingModels = normalizeModels(embeddingModels);   // trim key; 重复 -> RagConfigurationException
        defaultEmbeddingModel = resolveDefault(embeddingModels, defaultEmbeddingModel);
        storage = orDefault(storage, RagStorageProperties::new);
        retrieval = orDefault(retrieval, RagRetrievalProperties::new);
        validation = orDefault(validation, RagValidationProperties::new);
        if (dimensions <= 0 || dimensions > 8192) throw new RagConfigurationException("dimensions out of range");
    }
}
```

- Verification contribution: `TEST-012`、`TEST-013` 全部用例。
- After this file: 配置入口就绪。

#### File 7 — `CREATE src/main/java/top/egon/cola/component/rag/autoconfigure/RagAutoConfiguration.java`

- Purpose: 唯一装配入口：按开关生效、严格绑定、发布属性 Bean。
- Symbols: `@AutoConfiguration`、`@ConditionalOnProperty(prefix="egon.cola.component.rag", name="enabled", havingValue="true", matchIfMissing=false)`、`@Bean(name="ragClock")`、`@Bean(name="ragProperties")`。
- Repository evidence: `AgentFlowAutoConfiguration` 的开关注解、`Binder.get(environment).bindOrCreate(..., new NoUnboundElementsBindHandler(BindHandler.DEFAULT))` 严格绑定、`Clock.systemUTC()` 时钟 Bean。
- Dependencies and consumers: 被 `AutoConfiguration.imports` 发现；本 Step 只发布属性与时钟，后续 Step 在同一文件追加 Bean 方法。
- Why now: 属性 Bean 是后续一切装配的前提。
- Contract/signature changes: 新增自动配置类与两个 Bean 名。
- Input/output and state mapping: `Environment` → `RagProperties`；`Clock` → UTC 时钟源。
- Error and edge behavior: 未知键由 `NoUnboundElementsBindHandler` 拒绝；缺必填由 Jakarta 约束拒绝；两者都在上下文刷新期失败。
- Standards impact: `MC-BEAN-001`（`@Bean(name=...)` 显式命名 + `@ConditionalOnMissingBean`）、`MC-LOG-001`（本类无业务日志，`N/A` 于该类）、`MC-CONFIG-001`、`MC-TIME-001`（`Clock` 为 `java.time`）、`MC-ARCH-001`、`MC-SCOPE-001`、`MC-TEST-001`。
- Literal rule enforcement: `Rule 4`（Spring 管理的 Bean 显式命名）、`Rule 7`、`Rule 10`（时钟用 `java.time.Clock`）、`Rule 11`。
- Implementation pseudocode:

```java
@AutoConfiguration
@ConditionalOnProperty(prefix = "egon.cola.component.rag", name = "enabled",
                       havingValue = "true", matchIfMissing = false)
public class RagAutoConfiguration {

    @Bean(name = "ragClock")
    @ConditionalOnMissingBean(name = "ragClock")
    public Clock ragClock() {
        return Clock.systemUTC();
    }

    @Bean(name = "ragProperties")
    @ConditionalOnMissingBean(name = "ragProperties")
    public RagProperties ragProperties(Environment environment) {
        RagProperties properties = Binder.get(environment).bindOrCreate(
                "egon.cola.component.rag", Bindable.of(RagProperties.class),
                new NoUnboundElementsBindHandler(BindHandler.DEFAULT));
        validate(properties);            // Jakarta Validator 校验 @Valid 级联与 @NotEmpty
        return properties;
    }
}
```

- Verification contribution: `TEST-011`（默认关闭）、`TEST-012`（未知键与约束）、`TEST-013`（默认值）。
- After this file: 两个 Bean 就绪，RED 测试转为 GREEN 的配置部分。

#### File 8 — `CREATE src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`

- Purpose: 让 Spring Boot 发现自动配置。
- Symbols: 单行 `top.egon.cola.component.rag.autoconfigure.RagAutoConfiguration`。
- Repository evidence: `agent-flow-starter` 同名单行文件。
- Dependencies and consumers: 被 Spring Boot 的自动配置加载机制消费。
- Why now: 没有它，File 7 不会被加载，`TEST-011` 无法观察。
- Contract/signature changes: 新增一条自动配置登记。
- Input/output and state mapping: 无。
- Error and edge behavior: 路径或文件名错误会导致自动配置静默不生效——这是本文件的主要风险，由 `TEST-011` 的"启用后 Bean 存在"用例兜住。
- Standards impact: `MC-CONFIG-001`、`MC-SCOPE-001`、`MC-TEST-001`。
- Literal rule enforcement: `Rule 11`（资源位于模块内标准位置）。
- Implementation pseudocode:

```text
# 文件内容：单行，无注释、无包声明、无行尾空白
top.egon.cola.component.rag.autoconfigure.RagAutoConfiguration

# 校验方式（由 TEST-011 与 TEST-023 共同覆盖）：
# 1. 文件路径必须精确为 src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
# 2. 行内容必须等于上述全限定类名，不得写成相对路径或带 .class 后缀
# 3. 施加 egon.cola.component.rag.enabled=true 后，上下文必须能取到 ragProperties Bean
# 4. 缺失该文件时上下文仍会启动，但 ragProperties 不存在——这正是该用例要捕获的静默失效
```

- Verification contribution: `TEST-011` 的启用用例。
- After this file: 自动配置可被发现。

#### File 9 — `CREATE src/main/resources/META-INF/spring-configuration-metadata.json`

- Purpose: 提供 IDE 补全与键集自检依据。
- Symbols: `RagProperties` 全部键的 `properties` 数组（`enabled`、`dimensions`、`vector-store-bean-name`、`embedding-models`、`default-embedding-model`、`storage.type`、`storage.local.root`、`retrieval.default-top-k`、`retrieval.max-top-k`、`validation.probe-on-startup`）。
- Repository evidence: `spring-boot-configuration-processor` 在构建期生成该文件；`agent-flow-starter` 同样依赖处理器生成而非手写。
- Dependencies and consumers: 被 `TEST-013` 读取以断言键覆盖。
- Why now: 与属性类同批，保证键集可被静态核对。
- Contract/signature changes: 新增元数据文件。
- Input/output and state mapping: 无运行期映射。
- Error and edge behavior: **该文件由处理器生成**；若构建后不存在或不含预期键，说明处理器未生效——此时应检查 File 3 的 `annotationProcessorPaths`，而不是手写该文件。
- Standards impact: `MC-CONFIG-001`、`MC-SCOPE-001`、`MC-TEST-001`。
- Literal rule enforcement: `Rule 7`（键集的静态可核对来源）。
- Implementation pseudocode:

```text
GENERATED by spring-boot-configuration-processor from RagProperties and its nested records.
Do not hand-edit. Verification asserts the ten keys above are present after `mvn compile`.
If absent: fix the annotation processor configuration in the module POM, not this file.
```

- Verification contribution: `TEST-013` 的键覆盖断言。
- After this file: 键集可静态核对，本 Step 完成。

- Validation working directory: `/Users/mario/SelfProject/Egon-COLA/egon-cola-components/egon-cola-component-rag-starter`
- Verification command: `../../mvnw -B -ntp -pl . test -Dtest='RagAutoConfigurationTest,RagPropertiesBindingTest'`
- Expected result: 两个测试类全部通过；`creates_no_rag_bean_when_disabled` 证明默认关闭；三个失败用例分别证明未知键、缺必填与跨字段约束会在绑定期终止。
- Failure returns to: File 6（紧凑构造器规范化或默认值错误）、File 7（开关注解或严格绑定 handler 错误）、File 8（自动配置发现失败）。
- Completion criteria: `REQ-002` 的默认关闭与严格绑定有可执行证据；`REQ-012` 与 `REQ-018` 的配置键已就位（行为在 Step 9/10 实现）。
- Rollback: 回退本 Step 的 9 个文件；无外部残留。
- Commit paths: `src/test/java/top/egon/cola/component/rag/autoconfigure/RagAutoConfigurationTest.java`; `src/test/java/top/egon/cola/component/rag/autoconfigure/RagPropertiesBindingTest.java`; `src/main/java/top/egon/cola/component/rag/exception/RagException.java`; `src/main/java/top/egon/cola/component/rag/autoconfigure/RagEmbeddingModelProperties.java`; `src/main/java/top/egon/cola/component/rag/autoconfigure/{RagStorageProperties,RagRetrievalProperties,RagValidationProperties}.java`; `src/main/java/top/egon/cola/component/rag/autoconfigure/RagProperties.java`; `src/main/java/top/egon/cola/component/rag/autoconfigure/RagAutoConfiguration.java`; `src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`; `src/main/resources/META-INF/spring-configuration-metadata.json`
- Commit: `feat(rag): bind strict default-off rag configuration`

### Step 3 — 解析宿主 Bean 名并校验维度一致

- Requirements: `REQ-003`, `REQ-006`, `REQ-008`
- Dependencies: Step 2
- Baseline state: 配置可绑定，但尚无任何 Bean 名解析与维度校验；`vector-store-bean-name` 与 `embedding-models.*.embedding-model-bean-name` 仍是未被消费的配置值。
- Observable outcome: 启用后，能解析出 `VectorStore` Bean 与每个逻辑嵌入模型；Bean 名不可解析、逻辑名重复、默认模型缺失或维度不一致时上下文启动失败并指明具体名称。
- End state: `RagEmbeddingModelDescriptorBO`、`RagEmbeddingModelRegistry` 与 `RagAutoConfiguration` 的解析校验就绪；测试替身 `FakeEmbeddingModel`、`FakeVectorStore` 就绪，供后续全部 Step 复用。
- Test-first gate: `Required` — `RagAutoConfigurationTest` 新增的三个用例首次运行时编译失败（`RagEmbeddingModelRegistry` 不存在）。
- Manual Checks: `MC-ARCH-001`, `MC-NAME-001`, `MC-MODEL-001`, `MC-VALID-001`, `MC-BEAN-001`, `MC-LOG-001`, `MC-REUSE-001`, `MC-SCOPE-001`, `MC-TEST-001`
- Literal Rules: `Rule 1`, `Rule 3`, `Rule 4`, `Rule 11`
- Ordered files:

#### File 1 — `CREATE src/test/java/top/egon/cola/component/rag/support/FakeEmbeddingModel.java`

- Purpose: 提供可配置维度的离线 `EmbeddingModel` 替身。
- Symbols: `class FakeEmbeddingModel implements EmbeddingModel`，字段 `dimensions`；方法 `embed(String)` 返回长度等于 `dimensions` 的确定性向量，`dimensions()` 返回该字段，`embed(Document)` 与 `embed(List<String>)` 委托。
- Repository evidence: `agent-flow-starter/src/test/java/.../` 的 fake 写法（不启动真实模型）。
- Dependencies and consumers: 被 Step 3-8 的装配与行为测试消费；被 `RagVectorStoreProbeTest` 消费。
- Why now: 维度校验的 RED 测试必须能构造出维度不同的模型。
- Contract/signature changes: 实现 Spring AI `EmbeddingModel`，只覆盖测试需要的成员。
- Input/output and state mapping: `dimensions` → 向量长度与 `dimensions()` 返回值；相同输入产生相同向量（确定性，供 `TEST-025` 复跑断言）。
- Error and edge behavior: 维度为 0 或负数时构造失败，避免掩盖配置错误。
- Standards impact: `MC-MODEL-001`（测试替身为普通类，不作数据载体）、`MC-SCOPE-001`、`MC-TEST-001`。`Rule 3` 的转换规则不适用（无跨层转换）。
- Literal rule enforcement: `Rule 11`（测试位于模块 `src/test`）。
- Implementation pseudocode:

```java
class FakeEmbeddingModel implements EmbeddingModel {
    private final int dimensions;
    FakeEmbeddingModel(int dimensions) {
        if (dimensions <= 0) throw new IllegalArgumentException("dimensions must be positive");
        this.dimensions = dimensions;
    }
    @Override public float[] embed(String text) {
        float[] vector = new float[dimensions];
        Arrays.fill(vector, deterministicSeed(text));
        return vector;
    }
    @Override public float[] embed(Document document) { return embed(document.getText()); }
    @Override public int dimensions() { return dimensions; }
    @Override public EmbeddingResponse call(EmbeddingRequest request) { throw new UnsupportedOperationException(); }
}
```

- Verification contribution: 支撑 `TEST-015` 的维度不一致用例与后续全部摄取测试。
- After this file: 替身编译通过（此时生产类尚未完成，测试仍 RED）。

#### File 2 — `CREATE src/test/java/top/egon/cola/component/rag/support/FakeVectorStore.java`

- Purpose: 提供记录调用与可配置失败点的内存 `VectorStore` 替身。
- Symbols: `class FakeVectorStore implements VectorStore`，字段 `addedDocuments`、`deletedExpressions`、`searchRequests`、`failurePoint`；方法 `add(List<Document>)`、`delete(Filter.Expression)`、`delete(List<String>)`、`similaritySearch(SearchRequest)`。
- Repository evidence: 无同类；Spring AI `VectorStore` 接口已用 `javap` 核实（`add`、`delete(List<String>)`、`delete(Filter$Expression)`，检索来自父接口 `VectorStoreRetriever`）。
- Dependencies and consumers: 被 Step 7（摄取先删后写）、Step 8（强制过滤断言）、Step 10（探针往返）消费。
- Why now: Step 3 的装配测试需要一个可解析的 `VectorStore` Bean。
- Contract/signature changes: 实现 `VectorStore` 与 `VectorStoreRetriever` 的检索方法。
- Input/output and state mapping: `add` 累积传入文档；`delete(Expression)` 记录表达式并移除匹配项；`similaritySearch` 记录 `SearchRequest` 并按 `topK` 返回已写入文档的子集。
- Error and edge behavior: `failurePoint` 可取 `ADD`/`DELETE`/`SEARCH`/`NONE`，分别在对应方法抛出 `RuntimeException`，供 `TEST-010` 与 `TEST-018` 断言失败语义。
- Standards impact: `MC-MODEL-001`、`MC-SCOPE-001`、`MC-TEST-001`。
- Literal rule enforcement: `Rule 11`。
- Implementation pseudocode:

```java
class FakeVectorStore implements VectorStore {
    enum FailurePoint { NONE, ADD, DELETE, SEARCH }
    private final List<Document> documents = new ArrayList<>();
    private final List<Filter.Expression> deletedExpressions = new ArrayList<>();
    private final List<SearchRequest> searchRequests = new ArrayList<>();
    private FailurePoint failurePoint = NONE;

    @Override public void add(List<Document> toAdd) {
        if (failurePoint == ADD) throw new IllegalStateException("simulated add failure");
        documents.addAll(toAdd);
    }
    @Override public void delete(Filter.Expression expression) {
        if (failurePoint == DELETE) throw new IllegalStateException("simulated delete failure");
        deletedExpressions.add(expression);
        documents.removeIf(document -> matches(expression, document));
    }
    @Override public void delete(List<String> ids) { documents.removeIf(document -> ids.contains(document.getId())); }
    @Override public List<Document> similaritySearch(SearchRequest request) {
        if (failurePoint == SEARCH) throw new IllegalStateException("simulated search failure");
        searchRequests.add(request);
        return documents.stream().limit(request.getTopK()).toList();
    }
    @Override public VectorStoreRetriever similaritySearch() { return this; }   // 按 1.1.x 接口形状调整
}
```

- Verification contribution: `TEST-010`（失败点）、`TEST-017`（过滤断言）、`TEST-018`（探针往返）。
- After this file: 替身就绪。

#### File 3 — `CREATE src/main/java/top/egon/cola/component/rag/embed/RagEmbeddingModelDescriptorBO.java`

- Purpose: 把逻辑名、模型实例与维度绑定为一个只读事实。
- Symbols: `record RagEmbeddingModelDescriptorBO(String logicalName, EmbeddingModel embeddingModel, int dimensions)`。
- Repository evidence: agent archetype 的 `DeepResearchTaskBO` 等 record 载体写法。
- Dependencies and consumers: 被 `RagEmbeddingModelRegistry` 持有；被启动日志与 `TEST-014` 消费。
- Why now: 注册表的元素类型。
- Contract/signature changes: 新增公开载体。
- Input/output and state mapping: `logicalName` 来自配置 key（已规范化）；`embeddingModel` 来自容器；`dimensions` 来自 `EmbeddingModel#dimensions()`。
- Error and edge behavior: 三个字段均非空；`dimensions` 必须为正。
- Standards impact: `MC-NAME-001`（`BO` 后缀）、`MC-MODEL-001`（record）、`MC-SCOPE-001`、`MC-TEST-001`。
- Literal rule enforcement: `Rule 1`、`Rule 3`、`Rule 11`。
- Implementation pseudocode:

```java
public record RagEmbeddingModelDescriptorBO(String logicalName, EmbeddingModel embeddingModel, int dimensions) {
    public RagEmbeddingModelDescriptorBO {
        if (logicalName == null || logicalName.isBlank()) throw new RagConfigurationException("logical model name must not be blank");
        if (embeddingModel == null) throw new RagConfigurationException("embedding model must not be null");
        if (dimensions <= 0) throw new RagConfigurationException("embedding dimensions must be positive");
    }
}
```

- Verification contribution: `TEST-014`、`TEST-015` 的失败消息断言。
- After this file: 描述符就绪。

#### File 4 — `CREATE src/main/java/top/egon/cola/component/rag/embed/RagEmbeddingModelRegistry.java`

- Purpose: 逻辑名到模型的只读注册表，含默认回退与未注册失败。
- Symbols: `@Component("ragEmbeddingModelRegistry")` 的 `class RagEmbeddingModelRegistry`；方法 `resolve(String logicalName)`、`defaultDescriptor()`、`listLogicalNames()`。
- Repository evidence: `DefaultAgentFlowRegistry` 的只读注册表 + 显式 Bean 名先例。
- Dependencies and consumers: 被 `RagAutoConfiguration` 构造；被 Step 7 摄取与 Step 8 检索消费。
- Why now: 服务需要统一的模型解析入口。
- Contract/signature changes: 新增公开方法三个。
- Input/output and state mapping: 逻辑名 → `RagEmbeddingModelDescriptorBO`；`null` 或空白 → 默认模型；未注册 → `RagModelNotRegisteredException`（消息含 `listLogicalNames()`）。
- Error and edge behavior: 注册表构造时禁止空；`resolve` 不做回退到任意模型，未注册一律失败。
- Standards impact: `MC-BEAN-001`（显式 Bean 名、final 字段、`@RequiredArgsConstructor`、`@Qualifier`）、`MC-LOG-001`（`@Slf4j`，日志只记逻辑名与维度）、`MC-PATTERN-001`（Registry）、`MC-SCOPE-001`、`MC-TEST-001`。
- Literal rule enforcement: `Rule 1`（`Registry` 行为后缀）、`Rule 4`（`@Slf4j` + 显式名称 + 构造器注入 + `@Qualifier`）、`Rule 9`（Registry 模式）、`Rule 11`。
- Implementation pseudocode:

```java
@Slf4j
@Component("ragEmbeddingModelRegistry")
@RequiredArgsConstructor
public class RagEmbeddingModelRegistry {

    private final @Qualifier("ragProperties") RagProperties ragProperties;
    private final Map<String, RagEmbeddingModelDescriptorBO> descriptors = new LinkedHashMap<>();
    private String defaultLogicalName;

    @PostConstruct
    void initialize() {
        ragProperties.embeddingModels().forEach((logicalName, modelProperties) -> {
            EmbeddingModel model = resolveBean(modelProperties.embeddingModelBeanName());
            int dimensions = model.dimensions();
            if (dimensions != ragProperties.dimensions()) {
                throw new RagConfigurationException("embedding model '" + logicalName + "' reports dimensions "
                        + dimensions + " but rag.dimensions is " + ragProperties.dimensions());
            }
            descriptors.put(logicalName, new RagEmbeddingModelDescriptorBO(logicalName, model, dimensions));
        });
        defaultLogicalName = ragProperties.defaultEmbeddingModel();
        log.info("rag embedding models ready: {}, default {}, dimensions {}",
                 descriptors.keySet(), defaultLogicalName, ragProperties.dimensions());
    }

    public RagEmbeddingModelDescriptorBO resolve(String logicalName) {
        String resolvedName = (logicalName == null || logicalName.isBlank()) ? defaultLogicalName : logicalName;
        RagEmbeddingModelDescriptorBO descriptor = descriptors.get(resolvedName);
        if (descriptor == null) {
            throw new RagModelNotRegisteredException("embedding model '" + resolvedName
                    + "' is not registered; available models: " + descriptors.keySet());
        }
        return descriptor;
    }

    public List<String> listLogicalNames() { return List.copyOf(descriptors.keySet()); }
}
```

- Verification contribution: `TEST-014`（未注册与重复 Bean）、`TEST-015`（维度不一致）。
- After this file: 注册表就绪。

#### File 5 — `MODIFY src/main/java/top/egon/cola/component/rag/autoconfigure/RagAutoConfiguration.java`

- Purpose: 解析 `VectorStore` Bean、构造注册表并完成启动期校验。
- Symbols: 新增 `@Bean(name="ragVectorStore")`、`@Bean(name="ragDocumentExtractorRegistry")` 的解析方法、`RagEmbeddingModelDescriptorBO` 解析辅助方法、`validate(...)` 扩展。
- Repository evidence: `AgentFlowAutoConfiguration` 用 `BeanFactory`/`ListableBeanFactory` 按名取 Bean 的先例（`agentFlowValidationUtils`）。
- Dependencies and consumers: 服务实现将在后续 Step 注入 `ragVectorStore` 与 `ragEmbeddingModelRegistry`。
- Why now: 注册表需要一个被校验过的 `VectorStore` Bean。
- Contract/signature changes: 新增 Bean 名 `ragVectorStore`；`ragEmbeddingModelRegistry` 由 File 4 的 `@Component` 提供，不再重复声明。
- Input/output and state mapping: `ragProperties.vectorStoreBeanName()` → `ListableBeanFactory#getBean(name, VectorStore.class)`；失败时抛 `RagConfigurationException` 并附带已存在的 `VectorStore` 候选 Bean 名。
- Error and edge behavior: Bean 名不存在、类型不匹配、多个逻辑模型指向同一 Bean、默认模型不在注册表——全部启动失败。
- Standards impact: `MC-BEAN-001`、`MC-LOG-001`、`MC-CONFIG-001`、`MC-SCOPE-001`、`MC-TEST-001`。
- Literal rule enforcement: `Rule 4`、`Rule 11`。
- Implementation pseudocode:

```java
@Bean(name = "ragVectorStore")
@ConditionalOnMissingBean(name = "ragVectorStore")
public VectorStore ragVectorStore(ListableBeanFactory beanFactory, @Qualifier("ragProperties") RagProperties properties) {
    String beanName = properties.vectorStoreBeanName();
    if (!beanFactory.containsBean(beanName)) {
        throw new RagConfigurationException("vector store bean '" + beanName + "' is not present; known VectorStore beans: "
                + Arrays.toString(beanFactory.getBeanNamesForType(VectorStore.class)));
    }
    return beanFactory.getBean(beanName, VectorStore.class);
}
```

- Verification contribution: `TEST-014` 的 Bean 名不可解析用例。
- After this file: 装配链路完整，Step 3 的三个需求均有证据。

- Validation working directory: `/Users/mario/SelfProject/Egon-COLA/egon-cola-components/egon-cola-component-rag-starter`
- Verification command: `../../mvnw -B -ntp -pl . test -Dtest='RagAutoConfigurationTest'`
- Expected result: 全部用例通过，包含新增的 Bean 名不可解析、重复 Bean 与维度不一致三个失败用例。
- Failure returns to: File 4（解析或校验逻辑）、File 5（Bean 解析方式）。
- Completion criteria: `REQ-003`、`REQ-006`、`REQ-008` 各有可执行证据；`FakeEmbeddingModel` 与 `FakeVectorStore` 已可被后续 Step 复用。
- Rollback: 回退本 Step 的 5 个文件；`AutoConfiguration` 回到只有属性与时钟 Bean 的状态。
- Commit paths: `src/test/java/top/egon/cola/component/rag/support/FakeEmbeddingModel.java`; `src/test/java/top/egon/cola/component/rag/support/FakeVectorStore.java`; `src/main/java/top/egon/cola/component/rag/embed/RagEmbeddingModelDescriptorBO.java`; `src/main/java/top/egon/cola/component/rag/embed/RagEmbeddingModelRegistry.java`; `src/main/java/top/egon/cola/component/rag/autoconfigure/RagAutoConfiguration.java`
- Commit: `feat(rag): resolve host beans and validate embedding dimensions`

### Step 4 — 抽取 SPI、内置实现与抽取服务

- Requirements: `REQ-004`, `REQ-019`
- Dependencies: Step 2
- Baseline state: 装配可用，但不存在文本抽取能力。
- Observable outcome: 调用 `RagExtractionService#extract` 能按 `(mimeType, 文件名)` 路由到内置抽取器并返回文本与结构元数据；无匹配时抛 `RagExtractorMissingException` 并列出已注册 MIME；同优先级冲突时抛 `RagExtractorConflictException`；`fileName` 与 `mimeType` 同时为 `null` 时路由仍可命中或给出确定失败。
- End state: `RagDocumentExtractor` SPI、`RagDocumentExtractorRegistry`、`PlainTextRagDocumentExtractor`、`MarkdownRagDocumentExtractor`、`RagExtractionCommand`、`ExtractedDocumentBO`、`RagExtractionService` 与 `RagExtractionServiceImpl` 就绪；可选解析器（PDF/Tika）留待 Step 5。
- Test-first gate: `Required` — 三个测试类首次运行时编译失败（SPI 与服务不存在）。
- Manual Checks: `MC-ARCH-001`, `MC-NAME-001`, `MC-MODEL-001`, `MC-VALID-001`, `MC-BEAN-001`, `MC-LOG-001`, `MC-UTIL-001`, `MC-PATTERN-001`, `MC-SCOPE-001`, `MC-TEST-001`
- Literal Rules: `Rule 1`, `Rule 2`, `Rule 3`, `Rule 4`, `Rule 9`, `Rule 11`
- Ordered files:

#### File 1 — `CREATE src/test/java/top/egon/cola/component/rag/extract/RagDocumentExtractorRegistryTest.java`

- Purpose: 定义优先级路由、歧义与缺失三条行为。
- Symbols: `selects_lowest_order_when_multiple_support()`、`routes_with_null_mime_and_file_name()`、`fails_when_no_extractor_supports()`、`fails_when_same_order_and_same_support()`。
- Repository evidence: `AgentFlowConfigValidatorTest` 的断言风格；stub 抽取器在测试内以局部类实现。
- Dependencies and consumers: 消费 `RagDocumentExtractorRegistry`；观察选中的实现名。
- Why now: 路由是抽取链路的核心行为，先定契约再实现。
- Contract/signature changes: 引入 `RagDocumentExtractor` SPI 与注册表的 `route` 方法。
- Input/output and state mapping: `(mimeType, fileName)` → 选中的抽取器；`order()` 数值小者优先。
- Error and edge behavior: 无匹配抛 `RagExtractorMissingException` 且消息含已注册 MIME 能力列表；同 `order()` 且同时 `supports` 抛 `RagExtractorConflictException` 且消息含冲突实现名。
- Standards impact: `MC-PATTERN-001`（Registry + Strategy）、`MC-NAME-001`、`MC-LOG-001`（断言选中实现名被记录）、`MC-SCOPE-001`、`MC-TEST-001`。
- Literal rule enforcement: `Rule 1`、`Rule 9`（用注册表选择而非 `switch`）、`Rule 11`。
- Implementation pseudocode:

```java
class RagDocumentExtractorRegistryTest {
    @Test void selects_lowest_order_when_multiple_support() {
        registry = new RagDocumentExtractorRegistry(List.of(stub("slow", 20, "text/plain"),
                                                           stub("fast", 10, "text/plain")));
        assertThat(registry.route("text/plain", "a.txt").name()).isEqualTo("fast");
    }

    @Test void routes_with_null_mime_and_file_name() {
        registry = new RagDocumentExtractorRegistry(List.of(extensionStub("md", ".md")));
        assertThat(registry.route(null, null)).isNotNull();   // 具体断言按 stub 的 supports 语义
    }

    @Test void fails_when_no_extractor_supports() {
        assertThatThrownBy(() -> new RagDocumentExtractorRegistry(List.of()).route("application/xml", "a.xml"))
            .isInstanceOf(RagExtractorMissingException.class)
            .hasMessageContaining("registered");
    }

    @Test void fails_when_same_order_and_same_support() {
        assertThatThrownBy(() -> new RagDocumentExtractorRegistry(List.of(stub("a", 10, "text/plain"),
                                                                         stub("b", 10, "text/plain"))).validate())
            .isInstanceOf(RagExtractorConflictException.class);
    }
}
```

- Verification contribution: `TEST-001`、`TEST-002`、`TEST-004` 的 RED 契约。
- After this file: 编译失败（生产类型缺失）。

#### File 2 — `CREATE src/test/java/top/egon/cola/component/rag/extract/RagExtractionServiceImplTest.java`

- Purpose: 定义抽取服务的编排与异常透传语义。
- Symbols: `returns_extracted_document()`、`propagates_extraction_failure_without_content()`、`rejects_blank_content()`。
- Repository evidence: agent archetype 的服务测试风格（fake 协作者 + 断言返回值）。
- Dependencies and consumers: 消费 `RagExtractionService`；使用 `ByteArrayInputStream`。
- Why now: 与 File 1 同批，构成 Step 的 RED 契约。
- Contract/signature changes: 引入 `RagExtractionCommand`、`ExtractedDocumentBO`、`RagExtractionService#extract`。
- Input/output and state mapping: 命令（文件名、MIME、字节流）→ `ExtractedDocumentBO`（文本、标题、MIME、结构属性）。
- Error and edge behavior: `content` 为 `null` 抛 `RagValidationException`；抽取器抛 `RagExtractionException` 时原样传播且消息不含文档内容。
- Standards impact: `MC-VALID-001`（命令载体的 `@NotNull` 与 `@Validated`）、`MC-NAME-001`、`MC-LOG-001`（不记录内容）、`MC-SCOPE-001`、`MC-TEST-001`。
- Literal rule enforcement: `Rule 1`、`Rule 2`（服务边界校验）、`Rule 4`、`Rule 11`。
- Implementation pseudocode:

```java
class RagExtractionServiceImplTest {
    @Test void returns_extracted_document() {
        service = new RagExtractionServiceImpl(new RagDocumentExtractorRegistry(List.of(plainTextStub())));
        ExtractedDocumentBO document = service.extract(new RagExtractionCommand("a.txt", "text/plain", stream("hello")));
        assertThat(document.text()).isEqualTo("hello");
    }

    @Test void propagates_extraction_failure_without_content() {
        service = new RagExtractionServiceImpl(new RagDocumentExtractorRegistry(List.of(failingStub("boom-secret"))));
        assertThatThrownBy(() -> service.extract(command()))
            .isInstanceOf(RagExtractionException.class)
            .hasMessageNotContaining("boom-secret");
    }

    @Test void rejects_blank_content() {
        assertThatThrownBy(() -> service.extract(new RagExtractionCommand(null, null, null)))
            .isInstanceOf(RagValidationException.class);
    }
}
```

- Verification contribution: `TEST-026` 的 RED 契约（其中 MIME 能力列表断言在 File 8 补全）。
- After this file: 编译失败。

#### File 3 — `CREATE src/main/java/top/egon/cola/component/rag/model/ExtractedDocumentBO.java`

- Purpose: 抽取结果的稳定载体，是 SPI 与摄取服务之间的契约。
- Symbols: `record ExtractedDocumentBO(String text, String title, String mimeType, Map<String, String> attributes)`。
- Repository evidence: `DeepResearchTaskBO` 的 record 载体写法。
- Dependencies and consumers: 被 `RagDocumentExtractor#extract` 返回；被 `RagExtractionService` 返回；被 Step 7 的 `RagIngestionCommand` 承载。
- Why now: SPI 的返回类型必须先于 SPI 定义。
- Contract/signature changes: 新增公开载体。
- Input/output and state mapping: `text` 不可为 `null`（可为空串）；`attributes` 做不可变副本；`title` 与 `mimeType` 可空。
- Error and edge behavior: `text` 为 `null` 抛 `RagValidationException`；`attributes` 为 `null` 视为空表。
- Standards impact: `MC-NAME-001`（`BO` 后缀）、`MC-MODEL-001`（record + 紧凑构造器）、`MC-SCOPE-001`、`MC-TEST-001`。
- Literal rule enforcement: `Rule 1`、`Rule 3`、`Rule 11`。
- Implementation pseudocode:

```java
public record ExtractedDocumentBO(String text, String title, String mimeType, Map<String, String> attributes) {
    public ExtractedDocumentBO {
        if (text == null) throw new RagValidationException("extracted text must not be null");
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
    }
}
```

- Verification contribution: `TEST-026`。
- After this file: 载体就绪。

#### File 4 — `CREATE src/main/java/top/egon/cola/component/rag/extract/RagDocumentExtractor.java`

- Purpose: 定义"字节流 + 格式 → 文本 + 结构元数据"的可扩展契约。
- Symbols: `interface RagDocumentExtractor`，方法 `boolean supports(String mimeType, String fileName)`、`ExtractedDocumentBO extract(InputStream content, String mimeType, String fileName)`、`default int order() { return 100; }`、`default String name() { return getClass().getSimpleName(); }`。
- Repository evidence: 无同类 SPI；形态参照 `AgentWorkflowBuilderStrategy` 的接口 + 实现 + 工厂组合。
- Dependencies and consumers: 被 `RagDocumentExtractorRegistry` 收集；被使用方（Spec B）实现新格式。
- Why now: 注册表与实现的类型前提。
- Contract/signature changes: 新增公开 SPI。
- Input/output and state mapping: 入参可空；`content` 只读一次且实现不得关闭。
- Error and edge behavior: 实现须容忍两个入参同时为 `null`；解析失败抛 `RagExtractionException`。
- Standards impact: `MC-NAME-001`（`Extractor` 行为后缀）、`MC-PATTERN-001`（Strategy）、`MC-UTIL-001`（实现只用 JDK 或已批准解析器）、`MC-SCOPE-001`、`MC-TEST-001`。
- Literal rule enforcement: `Rule 1`、`Rule 9`、`Rule 11`。
- Implementation pseudocode:

```java
public interface RagDocumentExtractor {
    boolean supports(String mimeType, String fileName);
    ExtractedDocumentBO extract(InputStream content, String mimeType, String fileName);
    default int order() { return 100; }              // 数值越小优先级越高
    default String name() { return getClass().getSimpleName(); }
}
```

- Verification contribution: `TEST-001`、`TEST-002`。
- After this file: SPI 就绪。

#### File 5 — `CREATE src/main/java/top/egon/cola/component/rag/extract/RagDocumentExtractorRegistry.java`

- Purpose: 收集实现、按优先级排序、路由并完成歧义/缺失校验。
- Symbols: `@Component("ragDocumentExtractorRegistry")` 的 `class RagDocumentExtractorRegistry`；方法 `void validate()`（`@PostConstruct`）、`RagDocumentExtractor route(String mimeType, String fileName)`、`List<String> registeredMimeCapabilities()`。
- Repository evidence: `DefaultAgentFlowRegistry` 的只读注册表 + `AgentFlowConfigValidator` 的启动期 fail-closed 校验先例。
- Dependencies and consumers: 被 `RagExtractionServiceImpl` 消费；被装配校验调用。
- Why now: 服务依赖它。
- Contract/signature changes: 新增三个公开方法。
- Input/output and state mapping: 实现列表 → 按 `order()` 升序的只读列表；`(mimeType, fileName)` → 首个 `supports` 为真的实现。
- Error and edge behavior: 同 `order()` 且 `supports` 能力重叠时 `validate()` 抛 `RagExtractorConflictException`；`route` 无命中抛 `RagExtractorMissingException`（消息含 `registeredMimeCapabilities()`）；路由命中时记录被选实现名。
- Standards impact: `MC-BEAN-001`、`MC-LOG-001`（`@Slf4j`，只记实现名与 MIME，不记文件内容）、`MC-PATTERN-001`、`MC-SCOPE-001`、`MC-TEST-001`。
- Literal rule enforcement: `Rule 1`、`Rule 4`、`Rule 9`、`Rule 11`。
- Implementation pseudocode:

```java
@Slf4j
@Component("ragDocumentExtractorRegistry")
@RequiredArgsConstructor
public class RagDocumentExtractorRegistry {

    private final @Qualifier("ragDocumentExtractors") List<RagDocumentExtractor> extractors;

    @PostConstruct
    void validate() {
        sorted = extractors.stream().sorted(comparingInt(RagDocumentExtractor::order)).toList();
        for (int i = 0; i < sorted.size(); i++) {
            for (int j = i + 1; j < sorted.size(); j++) {
                if (sorted.get(i).order() == sorted.get(j).order() && overlaps(sorted.get(i), sorted.get(j))) {
                    throw new RagExtractorConflictException("extractors '" + sorted.get(i).name() + "' and '"
                            + sorted.get(j).name() + "' share order " + sorted.get(i).order());
                }
            }
        }
        log.info("rag document extractors ready: {}", sorted.stream().map(RagDocumentExtractor::name).toList());
    }

    public RagDocumentExtractor route(String mimeType, String fileName) {
        return sorted.stream().filter(extractor -> extractor.supports(mimeType, fileName)).findFirst()
            .map(extractor -> { log.debug("routed {} / {} to extractor {}", mimeType, fileName, extractor.name());
                                return extractor; })
            .orElseThrow(() -> new RagExtractorMissingException(
                "no document extractor supports mime '" + mimeType + "'; registered capabilities: "
                + registeredMimeCapabilities()));
    }

    public List<String> registeredMimeCapabilities() { /* 由各实现声明的能力常量汇总 */ }
}
```

- Verification contribution: `TEST-001`、`TEST-002`、`TEST-004`。
- After this file: 注册表就绪。

#### File 6 — `CREATE src/main/java/top/egon/cola/component/rag/extract/PlainTextRagDocumentExtractor.java`

- Purpose: 内置纯文本族抽取（`text/*`、`text/csv`、`application/json`、`application/xml`）。
- Symbols: `@Component("plainTextRagDocumentExtractor")` 的类；`supports` 按 MIME 前缀与扩展名匹配；`extract` 用 JDK 读流并返回 `ExtractedDocumentBO`。
- Repository evidence: 无同类；字符集处理只用 JDK（`InputStreamReader` + `StandardCharsets.UTF_8`）。
- Dependencies and consumers: 被注册表收集。
- Why now: 至少需要一个内置实现才能让注册表与服务的测试通过。
- Contract/signature changes: 新增实现。
- Input/output and state mapping: 字节流 → UTF-8 文本；`title` 取文件名去掉扩展名；`mimeType` 回填识别到的类型；`attributes` 放入 `sourceExtension`。
- Error and edge behavior: 读取失败包装为 `RagExtractionException`；不关闭入参流（由调用方负责）。
- Standards impact: `MC-NAME-001`、`MC-UTIL-001`（只用 JDK）、`MC-LOG-001`（不记录文本）、`MC-BEAN-001`、`MC-PATTERN-001`、`MC-SCOPE-001`、`MC-TEST-001`。
- Literal rule enforcement: `Rule 1`、`Rule 4`、`Rule 5`（JDK 优先）、`Rule 9`、`Rule 11`。
- Implementation pseudocode:

```java
@Slf4j
@Component("plainTextRagDocumentExtractor")
public class PlainTextRagDocumentExtractor implements RagDocumentExtractor {

    private static final Set<String> MIME_TYPES = Set.of(
            "text/plain", "text/csv", "application/json", "application/xml", "text/xml");
    private static final Set<String> EXTENSIONS = Set.of(".txt", ".csv", ".json", ".xml", ".log");

    @Override public boolean supports(String mimeType, String fileName) {
        return MIME_TYPES.contains(normalizeMime(mimeType)) || hasExtension(fileName, EXTENSIONS);
    }

    @Override public int order() { return 100; }        // 通用实现，优先级低于专用实现

    @Override public ExtractedDocumentBO extract(InputStream content, String mimeType, String fileName) {
        try {
            String text = new String(content.readAllBytes(), StandardCharsets.UTF_8);
            return new ExtractedDocumentBO(text, titleFrom(fileName), normalizeMime(mimeType),
                    Map.of("sourceExtension", extensionOf(fileName)));
        } catch (IOException exception) {
            throw new RagExtractionException("failed to read plain text document", exception);
        }
    }
}
```

- Verification contribution: `TEST-002`。
- After this file: 至少存在一个内置抽取器。

#### File 7 — `CREATE src/main/java/top/egon/cola/component/rag/extract/MarkdownRagDocumentExtractor.java`

- Purpose: 单独处理 Markdown，保留标题层级信息供 `MARKDOWN_HEADING` 策略使用。
- Symbols: `@Component("markdownRagDocumentExtractor")`；`supports` 匹配 `text/markdown` 与 `.md`；`order()` 返回 `50`（高于通用实现的优先级）。
- Repository evidence: 无同类。
- Dependencies and consumers: 被注册表收集；其 `attributes` 中的标题栈被 Step 6 的 Markdown 策略消费。
- Why now: `MARKDOWN_HEADING` 策略需要一个保留结构的抽取实现，与 Step 6 配对。
- Contract/signature changes: 新增实现；`attributes` 增加 `headingCount`。
- Input/output and state mapping: 字节流 → 原文文本（不做任何改写，保证切分的确定性）；`attributes` 记录 `headingCount`。
- Error and edge behavior: 读取失败包装为 `RagExtractionException`；不关闭入参流。
- Standards impact: `MC-NAME-001`、`MC-UTIL-001`、`MC-BEAN-001`、`MC-PATTERN-001`、`MC-SCOPE-001`、`MC-TEST-001`。
- Literal rule enforcement: `Rule 1`、`Rule 4`、`Rule 5`、`Rule 9`、`Rule 11`。
- Implementation pseudocode:

```java
@Slf4j
@Component("markdownRagDocumentExtractor")
public class MarkdownRagDocumentExtractor implements RagDocumentExtractor {

    @Override public boolean supports(String mimeType, String fileName) {
        return "text/markdown".equalsIgnoreCase(normalizeMime(mimeType)) || hasExtension(fileName, Set.of(".md", ".markdown"));
    }

    @Override public int order() { return 50; }         // 比通用文本实现更专用

    @Override public ExtractedDocumentBO extract(InputStream content, String mimeType, String fileName) {
        String text = readUtf8(content);                 // 与通用实现相同的读法，保证确定性
        int headingCount = (int) text.lines().filter(line -> line.stripLeading().startsWith("#")).count();
        return new ExtractedDocumentBO(text, titleFrom(fileName), "text/markdown", Map.of("headingCount", String.valueOf(headingCount)));
    }
}
```

- Verification contribution: `TEST-002`（路由优先级：Markdown 文件命中本实现而非通用实现）。
- After this file: 两个内置抽取器就绪。

#### File 8 — `CREATE src/main/java/top/egon/cola/component/rag/model/RagExtractionCommand.java`

- Purpose: 抽取入口的写意图载体。
- Symbols: `record RagExtractionCommand(String fileName, String mimeType, InputStream content)`。
- Repository evidence: agent archetype 的 `StartDeepResearchCommand` 写法。
- Dependencies and consumers: 被 `RagExtractionService#extract` 消费。
- Why now: 服务接口的入参类型。
- Contract/signature changes: 新增公开载体。
- Input/output and state mapping: `fileName`、`mimeType` 可空；`content` 必填；紧凑构造器 `trim` 字符串。
- Error and edge behavior: `content` 为 `null` 时由服务层校验抛 `RagValidationException`（本 record 不抛，保持命令构造轻量）。
- Standards impact: `MC-NAME-001`（`Command` 后缀）、`MC-MODEL-001`、`MC-VALID-001`（`@NotNull` 于 `content`）、`MC-SCOPE-001`、`MC-TEST-001`。
- Literal rule enforcement: `Rule 1`、`Rule 2`、`Rule 3`、`Rule 11`。
- Implementation pseudocode:

```java
public record RagExtractionCommand(String fileName, String mimeType, @NotNull InputStream content) {
    public RagExtractionCommand {
        fileName = fileName == null ? null : fileName.trim();
        mimeType = mimeType == null ? null : (mimeType.contains("/") ? mimeType.trim() : null);
    }
}
```

- Verification contribution: `TEST-026`。
- After this file: 抽取命令就绪。

#### File 9 — `CREATE src/main/java/top/egon/cola/component/rag/api/RagExtractionService.java`

- Purpose: 抽取能力的公开入口。
- Symbols: `interface RagExtractionService { ExtractedDocumentBO extract(@Valid RagExtractionCommand command); }`。
- Repository evidence: `AgentFlowService` 的公开接口写法。
- Dependencies and consumers: 被使用方（Spec B）调用。
- Why now: 实现类需要它。
- Contract/signature changes: 新增公开接口（`REQ-019`）。
- Input/output and state mapping: 命令 → 抽取结果。
- Error and edge behavior: 见 `RagExtractionServiceImpl`。
- Standards impact: `MC-NAME-001`（`Service` 行为后缀）、`MC-SCOPE-001`、`MC-TEST-001`。
- Literal rule enforcement: `Rule 1`、`Rule 11`。
- Implementation pseudocode:

```java
/**
 * 抽取能力的公开入口：把字节流解析为文本与结构元数据。
 * 本接口不切分、不嵌入、不持久化；消费方取得结果后自行落库，再交给 RagIngestionService。
 */
public interface RagExtractionService {

    /**
     * @param command 文件名、MIME 与字节流；三者皆可空者由实现按注册表能力判定
     * @return 文本（可为空串但不得为 null）与结构元数据
     * @throws RagValidationException content 为 null，或返回属性含保留键
     * @throws RagExtractorMissingException 无实现支持该格式，消息含已注册能力
     * @throws RagExtractorConflictException 同优先级实现能力重叠
     * @throws RagExtractionException 抽取器解析失败
     */
    ExtractedDocumentBO extract(@Valid RagExtractionCommand command);
}
```

- Verification contribution: `TEST-026`。
- After this file: 接口就绪。

#### File 10 — `CREATE src/main/java/top/egon/cola/component/rag/execution/RagExtractionServiceImpl.java`

- Purpose: 抽取编排：校验 → 路由 → 调用实现 → 校验结果属性。
- Symbols: `@Service("ragExtractionService")` + `@Validated` 的类；方法 `extract(RagExtractionCommand)`。
- Repository evidence: `DeepResearchManageImpl` 的 `@Service("...")` + `@RequiredArgsConstructor` + `@Qualifier` 写法。
- Dependencies and consumers: 被使用方调用；依赖 `ragDocumentExtractorRegistry`。
- Why now: RED 契约已由 File 1/2 固定。
- Contract/signature changes: 实现 `RagExtractionService`。
- Input/output and state mapping: 命令 → `registry.route` → `extractor.extract` → 结果属性校验 → `ExtractedDocumentBO`。
- Error and edge behavior: `content` 为 `null` 抛 `RagValidationException`；无匹配抛 `RagExtractorMissingException`；实现抛出的 `RagExtractionException` 原样传播；返回结果含保留元数据键时抛 `RagValidationException`。
- Standards impact: `MC-BEAN-001`、`MC-VALID-001`（`@Validated` + 方法级 `@Valid`）、`MC-LOG-001`（`@Slf4j`，只记文件名、MIME、实现名、文本长度与耗时）、`MC-UTIL-001`、`MC-PATTERN-001`、`MC-SCOPE-001`、`MC-TEST-001`。
- Literal rule enforcement: `Rule 1`、`Rule 2`、`Rule 4`、`Rule 5`、`Rule 9`、`Rule 11`。
- Implementation pseudocode:

```java
@Slf4j
@Service("ragExtractionService")
@Validated
@RequiredArgsConstructor
public class RagExtractionServiceImpl implements RagExtractionService {

    private final @Qualifier("ragDocumentExtractorRegistry") RagDocumentExtractorRegistry registry;
    private final @Qualifier("ragClock") Clock clock;

    @Override
    public ExtractedDocumentBO extract(@Valid RagExtractionCommand command) {
        if (command.content() == null) throw new RagValidationException("content must not be null");
        Instant startedAt = clock.instant();
        RagDocumentExtractor extractor = registry.route(command.mimeType(), command.fileName());
        log.info("rag extraction started: file={}, mime={}, extractor={}",
                 command.fileName(), command.mimeType(), extractor.name());
        ExtractedDocumentBO document = extractor.extract(command.content(), command.mimeType(), command.fileName());
        RagMetadataKeys.rejectReservedKeys(document.attributes());
        log.info("rag extraction finished: extractor={}, textChars={}, durationMs={}, result=SUCCESS",
                 extractor.name(), document.text().length(), Duration.between(startedAt, clock.instant()).toMillis());
        return document;
    }
}
```

- Verification contribution: `TEST-026`（含 MIME 能力列表与异常透传）。
- After this file: Step 4 的服务链路 GREEN。

#### File 11 — `CREATE src/test/java/top/egon/cola/component/rag/extract/RagExtractionServiceImplTest.java`

- Purpose: 补全 MIME 能力列表断言，并为新增的 `extract/`、`model/`、`api/`、`execution/` 包补 `package-info.java`。
- Symbols: `fails_when_no_extractor_supports()` 增加 `hasMessageContaining("text/plain")`；四个 `package-info.java`。
- Repository evidence: `agent-flow-starter` 每个包均有 `package-info.java`。
- Dependencies and consumers: 被编译与 `TEST-021` 的包文档断言观察。
- Why now: 包在 File 3-10 中新建，包文档必须在同一 Step 内补齐，否则契约测试在 Step 11 才失败会掩盖问题。
- Contract/signature changes: 无。
- Input/output and state mapping: 无。
- Error and edge behavior: 无。
- Standards impact: `MC-SCOPE-001`、`MC-TEST-001`。
- Literal rule enforcement: `Rule 11`。
- Implementation pseudocode:

```java
/** Document extraction SPI, built-in text extractors and the routing registry. */
package top.egon.cola.component.rag.extract;
// model / api / execution 三个包同形
```

- Verification contribution: `TEST-004` 的能力列表断言；`TEST-021` 的包文档断言。
- After this file: Step 4 全部文件就位。

- Validation working directory: `/Users/mario/SelfProject/Egon-COLA/egon-cola-components/egon-cola-component-rag-starter`
- Verification command: `../../mvnw -B -ntp -pl . test -Dtest='RagDocumentExtractorRegistryTest,RagExtractionServiceImplTest'`
- Expected result: 两个测试类全部通过；无匹配用例的消息含 `text/plain`；冲突用例抛 `RagExtractorConflictException`。
- Failure returns to: File 5（路由或冲突判定）、File 10（编排顺序或校验位置）。
- Completion criteria: `REQ-004` 与 `REQ-019` 各有可执行证据；抽取链路可被 `ApplicationContextRunner` 完整装配。
- Rollback: 回退本 Step 全部文件；`RagAutoConfiguration` 未在本 Step 修改，因此回退无装配残留。
- Commit paths: `src/test/java/top/egon/cola/component/rag/extract/RagDocumentExtractorRegistryTest.java`; `src/test/java/top/egon/cola/component/rag/extract/RagExtractionServiceImplTest.java`; `src/main/java/top/egon/cola/component/rag/model/ExtractedDocumentBO.java`; `src/main/java/top/egon/cola/component/rag/extract/RagDocumentExtractor.java`; `src/main/java/top/egon/cola/component/rag/extract/RagDocumentExtractorRegistry.java`; `src/main/java/top/egon/cola/component/rag/extract/PlainTextRagDocumentExtractor.java`; `src/main/java/top/egon/cola/component/rag/extract/MarkdownRagDocumentExtractor.java`; `src/main/java/top/egon/cola/component/rag/model/RagExtractionCommand.java`; `src/main/java/top/egon/cola/component/rag/api/RagExtractionService.java`; `src/main/java/top/egon/cola/component/rag/execution/RagExtractionServiceImpl.java`; `src/test/java/top/egon/cola/component/rag/extract/RagExtractionServiceImplTest.java`
- Commit: `feat(rag): add document extraction spi and service`

### Step 5 — 可选 PDF 与 Tika 解析器

- Requirements: `REQ-013`
- Dependencies: Step 4
- Baseline state: 抽取链路可路由内置文本实现；PDF 与 Office 格式无实现。
- Observable outcome: 类路径不含 `spring-ai-pdf-document-reader` 时应用正常启动且注册表不含 PDF 抽取器，请求 PDF 时抛 `RagExtractorMissingException`；类路径含该依赖时 PDF 可被抽取。
- End state: 两个可选实现就绪，且受 `@ConditionalOnClass` 保护；`pom.xml` 的 `optional` 依赖在 Step 1 已声明。
- Test-first gate: `Required` — `RagOptionalExtractorTest` 首次运行时编译失败（可选实现类不存在）。
- Manual Checks: `MC-ARCH-001`, `MC-NAME-001`, `MC-BEAN-001`, `MC-LOG-001`, `MC-UTIL-001`, `MC-DEP-001`, `MC-PATTERN-001`, `MC-SCOPE-001`, `MC-TEST-001`
- Literal Rules: `Rule 1`, `Rule 4`, `Rule 5`, `Rule 9`, `Rule 11`
- Ordered files:

#### File 1 — `CREATE src/test/java/top/egon/cola/component/rag/extract/RagOptionalExtractorTest.java`

- Purpose: 定义"可选依赖缺席不影响启动、但需要时给出可诊断错误"这条行为。
- Symbols: `starts_without_pdf_reader()`、`reports_missing_extractor_when_pdf_reader_absent()`、`registers_pdf_extractor_when_present()`。
- Repository evidence: `agent-flow-starter` 的 `ApplicationContextRunner` 用法；`FilteredClassLoader` 由 `spring-boot-test` 提供。
- Dependencies and consumers: 消费 `RagAutoConfiguration` 与 `ragDocumentExtractorRegistry`。
- Why now: 这是 `REQ-013` 的唯一可执行证据。
- Contract/signature changes: 引入两个可选实现类。
- Input/output and state mapping: 类路径（含/不含 PDF 读取器）→ 上下文启动结果与注册表内容。
- Error and edge behavior: 缺席时启动成功；请求 PDF 抛 `RagExtractorMissingException` 且消息含已注册能力；存在时注册表含 PDF 实现。
- Standards impact: `MC-DEP-001`（`optional` scope 的后果）、`MC-BEAN-001`、`MC-PATTERN-001`、`MC-SCOPE-001`、`MC-TEST-001`。
- Literal rule enforcement: `Rule 9`、`Rule 11`。
- Implementation pseudocode:

```java
class RagOptionalExtractorTest {
    @Test void starts_without_pdf_reader() {
        new ApplicationContextRunner()
            .withClassLoader(new FilteredClassLoader("org.springframework.ai.reader.pdf"))
            .withConfiguration(AutoConfigurations.of(RagAutoConfiguration.class))
            .withUserConfiguration(StubHostBeans.class)
            .withPropertyValues(enabledKeySet())
            .run(context -> assertThat(context).hasNotFailed()
                 .doesNotHaveBean("pdfRagDocumentExtractor"));
    }

    @Test void reports_missing_extractor_when_pdf_reader_absent() { /* 断言 RagExtractorMissingException 与能力列表 */ }

    @Test void registers_pdf_extractor_when_present() { /* 不施加过滤，断言 Bean 存在 */ }
}
```

- Verification contribution: `TEST-019` 的 RED 契约。
- After this file: 编译失败。

#### File 2 — `CREATE src/main/java/top/egon/cola/component/rag/extract/PdfRagDocumentExtractor.java`

- Purpose: 复用官方 PDF 读取器提供 `application/pdf` 抽取。
- Symbols: `@Component("pdfRagDocumentExtractor")`、`@ConditionalOnClass(name = "org.springframework.ai.reader.pdf.PagePdfDocumentReader")`；`order()` 返回 `20`。
- Repository evidence: `spring-ai-pdf-document-reader` 在 `spring-ai-bom:1.1.2` 管理清单中；类名以该 artifact 的实际类为准（实施时用 `javap` 复核）。
- Dependencies and consumers: 被注册表收集（仅当类存在时）。
- Why now: 与 File 1 的 RED 契约配对。
- Contract/signature changes: 新增可选实现。
- Input/output and state mapping: 字节流 → 逐页文本拼接 + `attributes` 记录 `pageCount`；`title` 取文件名。
- Error and edge behavior: 读取失败包装为 `RagExtractionException`；不关闭入参流。
- Standards impact: `MC-DEP-001`、`MC-UTIL-001`（第三方解析器按需引入属批准范围）、`MC-BEAN-001`、`MC-PATTERN-001`、`MC-LOG-001`、`MC-SCOPE-001`、`MC-TEST-001`。
- Literal rule enforcement: `Rule 1`、`Rule 4`、`Rule 5`（Tika/PDF 属"按需引入"）、`Rule 9`、`Rule 11`。
- Implementation pseudocode:

```java
@Slf4j
@Component("pdfRagDocumentExtractor")
@ConditionalOnClass(name = "org.springframework.ai.reader.pdf.PagePdfDocumentReader")
public class PdfRagDocumentExtractor implements RagDocumentExtractor {

    @Override public boolean supports(String mimeType, String fileName) {
        return "application/pdf".equalsIgnoreCase(normalizeMime(mimeType)) || hasExtension(fileName, Set.of(".pdf"));
    }

    @Override public int order() { return 20; }

    @Override public ExtractedDocumentBO extract(InputStream content, String mimeType, String fileName) {
        try {
            List<Document> pages = new PagePdfDocumentReader(new ByteArrayResource(content.readAllBytes())).get();
            String text = pages.stream().map(Document::getText).collect(joining("\n"));
            return new ExtractedDocumentBO(text, titleFrom(fileName), "application/pdf",
                    Map.of("pageCount", String.valueOf(pages.size())));
        } catch (RuntimeException | IOException exception) {
            throw new RagExtractionException("failed to read pdf document", exception);
        }
    }
}
```

- Verification contribution: `TEST-019` 的存在用例。
- After this file: PDF 抽取能力就绪。

#### File 3 — `CREATE src/main/java/top/egon/cola/component/rag/extract/TikaRagDocumentExtractor.java`

- Purpose: 以 Tika 兜底覆盖 docx/xlsx/pptx 等 Office 格式。
- Symbols: `@Component("tikaRagDocumentExtractor")`、`@ConditionalOnClass(name = "org.springframework.ai.reader.tika.TikaDocumentReader")`；`order()` 返回 `900`（最低优先级，仅在无更专用实现时生效）。
- Repository evidence: `spring-ai-tika-document-reader` 在 BOM 管理清单中；用户规则 5 明确"针对 Tika 按需引入"。
- Dependencies and consumers: 被注册表收集（仅当类存在时）。
- Why now: 与 PDF 同批，两者共同构成 `REQ-013` 的可选能力。
- Contract/signature changes: 新增可选实现。
- Input/output and state mapping: 字节流 → Tika 抽取文本；`attributes` 记录 `detectedMimeType`。
- Error and edge behavior: 解析失败包装为 `RagExtractionException`；不关闭入参流。
- Standards impact: `MC-DEP-001`（`optional` scope）、`MC-UTIL-001`（Tika 属规则 5 批准的内容识别依赖）、`MC-BEAN-001`、`MC-PATTERN-001`、`MC-LOG-001`、`MC-SCOPE-001`、`MC-TEST-001`。
- Literal rule enforcement: `Rule 1`、`Rule 4`、`Rule 5`、`Rule 9`、`Rule 11`。
- Implementation pseudocode:

```java
@Slf4j
@Component("tikaRagDocumentExtractor")
@ConditionalOnClass(name = "org.springframework.ai.reader.tika.TikaDocumentReader")
public class TikaRagDocumentExtractor implements RagDocumentExtractor {

    @Override public boolean supports(String mimeType, String fileName) {
        return true;   // 兜底实现；靠 order() 保证只在无更专用实现时被路由命中
    }

    @Override public int order() { return 900; }

    @Override public ExtractedDocumentBO extract(InputStream content, String mimeType, String fileName) {
        String text = new TikaDocumentReader(new ByteArrayResource(readAllBytes(content))).get().stream()
                .map(Document::getText).collect(joining("\n"));
        return new ExtractedDocumentBO(text, titleFrom(fileName), normalizeMime(mimeType),
                Map.of("detectedMimeType", String.valueOf(normalizeMime(mimeType))));
    }
}
```

- Verification contribution: `TEST-019`；`TEST-001` 的优先级断言（Tika 的 `order()` 最大，不会抢占专用实现）。
- After this file: Step 5 全部文件就位。

- Validation working directory: `/Users/mario/SelfProject/Egon-COLA/egon-cola-components/egon-cola-component-rag-starter`
- Verification command: `../../mvnw -B -ntp -pl . test -Dtest='RagOptionalExtractorTest'`
- Expected result: 三个用例通过；过滤掉 PDF 读取器后上下文仍能启动且请求 PDF 时报可诊断错误。
- Failure returns to: File 2/3（`@ConditionalOnClass` 的类名与 artifact 实际类名不符）、File 1（`FilteredClassLoader` 的包名前缀）。
- Completion criteria: `REQ-013` 有可执行证据；两个可选实现缺席时应用仍可启动。
- Rollback: 回退本 Step 三个文件；抽取链路回到仅内置文本实现的状态。
- Commit paths: `src/test/java/top/egon/cola/component/rag/extract/RagOptionalExtractorTest.java`; `src/main/java/top/egon/cola/component/rag/extract/PdfRagDocumentExtractor.java`; `src/main/java/top/egon/cola/component/rag/extract/TikaRagDocumentExtractor.java`
- Commit: `feat(rag): add optional pdf and tika extractors`

### Step 6 — 分块策略、枚举工厂与确定性分块标识

- Requirements: `REQ-005`, `REQ-009`
- Dependencies: Step 2
- Baseline state: 抽取链路可用；不存在切分能力。
- Observable outcome: 三种内置策略可按枚举解析并各自产出确定性分块；未知枚举值在配置绑定期失败；同一输入两次切分产生逐字段一致的结果；同一 `(documentId, chunkIndex)` 生成相同分块标识。
- End state: `RagChunkingStrategyEnum`、`RagChunkingStrategy`、三个策略、`RagChunkingStrategyFactory`、`RagChunkIdFactory`、`RagChunkBO`、`RagChunkingConfigDTO` 就绪。
- Test-first gate: `Required` — 三个测试类首次运行时编译失败（枚举、工厂与策略不存在）。
- Manual Checks: `MC-ARCH-001`, `MC-NAME-001`, `MC-MODEL-001`, `MC-VALID-001`, `MC-BEAN-001`, `MC-LOG-001`, `MC-REUSE-001`, `MC-UTIL-001`, `MC-PATTERN-001`, `MC-SCOPE-001`, `MC-TEST-001`
- Literal Rules: `Rule 1`, `Rule 2`, `Rule 3`, `Rule 4`, `Rule 5`, `Rule 9`, `Rule 11`
- Ordered files:

#### File 1 — `CREATE src/test/java/top/egon/cola/component/rag/chunk/RagChunkingStrategyFactoryTest.java`

- Purpose: 定义枚举到实现的解析与缺失失败。
- Symbols: `resolves_all_builtin_strategies()`、`fails_when_strategy_not_registered()`。
- Repository evidence: `AgentWorkflowStrategyFactoryTest` 的工厂测试写法。
- Dependencies and consumers: 消费 `RagChunkingStrategyFactory` 与三个策略。
- Why now: 工厂是策略选择的唯一入口，先定契约。
- Contract/signature changes: 引入枚举、SPI 与工厂。
- Input/output and state mapping: `RagChunkingStrategyEnum` → `RagChunkingStrategy`；缺失时抛 `RagConfigurationException` 而非返回 `null`。
- Error and edge behavior: 注册表缺某枚举值时构造即失败。
- Standards impact: `MC-PATTERN-001`（Factory + Strategy）、`MC-BEAN-001`、`MC-SCOPE-001`、`MC-TEST-001`。
- Literal rule enforcement: `Rule 1`、`Rule 4`、`Rule 9`、`Rule 11`。
- Implementation pseudocode:

```java
class RagChunkingStrategyFactoryTest {
    @Test void resolves_all_builtin_strategies() {
        factory = new RagChunkingStrategyFactory(List.of(new TokenRagChunkingStrategy(),
                                                        new MarkdownHeadingRagChunkingStrategy(),
                                                        new RecursiveRagChunkingStrategy()));
        for (RagChunkingStrategyEnum value : RagChunkingStrategyEnum.values()) {
            assertThat(factory.resolve(value).strategy()).isEqualTo(value);
        }
    }

    @Test void fails_when_strategy_not_registered() {
        factory = new RagChunkingStrategyFactory(List.of(new TokenRagChunkingStrategy()));
        assertThatThrownBy(() -> factory.resolve(RagChunkingStrategyEnum.RECURSIVE))
            .isInstanceOf(RagConfigurationException.class);
    }
}
```

- Verification contribution: `TEST-003` 的 RED 契约。
- After this file: 编译失败。

#### File 2 — `CREATE src/test/java/top/egon/cola/component/rag/chunk/RagChunkingStrategyTest.java`

- Purpose: 定义三种策略的产出形状与参数边界。
- Symbols: `token_strategy_splits_long_text()`、`markdown_strategy_splits_by_heading_levels()`、`recursive_strategy_splits_on_separator_cascade()`、`rejects_overlap_not_smaller_than_max_tokens()`、`rejects_heading_levels_for_non_markdown_strategy()`。
- Repository evidence: 无同类；断言风格参照 agent archetype 的域测试。
- Dependencies and consumers: 消费三个策略与 `RagChunkingConfigDTO`。
- Why now: 与 File 1 同批构成 RED 契约。
- Contract/signature changes: 引入 `RagChunkBO` 与 `RagChunkingConfigDTO`。
- Input/output and state mapping: `ExtractedDocumentBO` + `RagChunkingConfigDTO` → `List<RagChunkBO>`（`chunkIndex` 从 0 连续）。
- Error and edge behavior: `overlapTokens >= maxTokensPerChunk` 与"非 Markdown 策略传 `headingLevels`"抛 `RagValidationException`。
- Standards impact: `MC-VALID-001`（跨字段校验）、`MC-MODEL-001`、`MC-PATTERN-001`、`MC-SCOPE-001`、`MC-TEST-001`。
- Literal rule enforcement: `Rule 1`、`Rule 2`、`Rule 9`、`Rule 11`。
- Implementation pseudocode:

```java
class RagChunkingStrategyTest {
    @Test void token_strategy_splits_long_text() {
        List<RagChunkBO> chunks = new TokenRagChunkingStrategy().split(documentOf(longText(2000)), config(TOKEN, 200, 20));
        assertThat(chunks).isNotEmpty();
        assertThat(chunks.get(0).chunkIndex()).isZero();
        assertThat(chunks).extracting(RagChunkBO::chunkIndex).isSorted().doesNotHaveDuplicates();
    }

    @Test void rejects_overlap_not_smaller_than_max_tokens() {
        assertThatThrownBy(() -> config(TOKEN, 100, 100)).isInstanceOf(RagValidationException.class);
    }

    @Test void rejects_heading_levels_for_non_markdown_strategy() {
        assertThatThrownBy(() -> configWithLevels(TOKEN)).isInstanceOf(RagValidationException.class);
    }
}
```

- Verification contribution: `TEST-005` 的 RED 契约。
- After this file: 编译失败。

#### File 3 — `CREATE src/test/java/top/egon/cola/component/rag/chunk/RagChunkIdFactoryTest.java`

- Purpose: 定义分块标识的确定性与非法输入拒绝。
- Symbols: `generates_same_id_for_same_document_and_index()`、`rejects_document_id_containing_colon()`。
- Repository evidence: 无同类。
- Dependencies and consumers: 消费 `RagChunkIdFactory`。
- Why now: 幂等性（`REQ-009`）的核心证据。
- Contract/signature changes: 引入 `RagChunkIdFactory`。
- Input/output and state mapping: `(documentId, chunkIndex)` → `String`，形状为 `documentId + ":" + chunkIndex`。
- Error and edge behavior: `documentId` 含 `:`、空白或 `chunkIndex` 为负时抛 `RagValidationException`。
- Standards impact: `MC-NAME-001`（`Factory` 后缀）、`MC-SCOPE-001`、`MC-TEST-001`。
- Literal rule enforcement: `Rule 1`、`Rule 11`。
- Implementation pseudocode:

```java
class RagChunkIdFactoryTest {
    @Test void generates_same_id_for_same_document_and_index() {
        factory = new RagChunkIdFactory();
        assertThat(factory.create("doc-1", 7)).isEqualTo("doc-1:7").isEqualTo(factory.create("doc-1", 7));
    }

    @Test void rejects_document_id_containing_colon() {
        assertThatThrownBy(() -> new RagChunkIdFactory().create("doc:1", 0)).isInstanceOf(RagValidationException.class);
    }
}
```

- Verification contribution: `TEST-009` 的 RED 契约。
- After this file: 编译失败。

#### File 4 — `CREATE src/main/java/top/egon/cola/component/rag/model/RagChunkBO.java`

- Purpose: 切分产物载体，是策略与嵌入之间的契约。
- Symbols: `record RagChunkBO(int chunkIndex, String content, Map<String, String> attributes)`。
- Repository evidence: `DeepResearchTaskBO` 的 record 写法。
- Dependencies and consumers: 被三个策略返回；被 Step 7 的转换器消费。
- Why now: 策略的返回类型必须先存在。
- Contract/signature changes: 新增公开载体；**不携带分块标识**（标识由 `RagChunkIdFactory` 生成，避免策略伪造身份）。
- Input/output and state mapping: `chunkIndex ≥ 0`；`content` 非 `null`；`attributes` 不可变副本。
- Error and edge behavior: 违反约束抛 `RagValidationException`。
- Standards impact: `MC-NAME-001`（`BO` 后缀）、`MC-MODEL-001`、`MC-SCOPE-001`、`MC-TEST-001`。
- Literal rule enforcement: `Rule 1`、`Rule 3`、`Rule 11`。
- Implementation pseudocode:

```java
public record RagChunkBO(int chunkIndex, String content, Map<String, String> attributes) {
    public RagChunkBO {
        if (chunkIndex < 0) throw new RagValidationException("chunk index must not be negative");
        if (content == null) throw new RagValidationException("chunk content must not be null");
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
    }
}
```

- Verification contribution: `TEST-005`、`TEST-009`。
- After this file: 载体就绪。

#### File 5 — `CREATE src/main/java/top/egon/cola/component/rag/model/RagChunkingConfigDTO.java`

- Purpose: 分块策略与参数的跨边界载体，含跨字段校验。
- Symbols: `record RagChunkingConfigDTO(RagChunkingStrategyEnum strategy, int maxTokensPerChunk, int overlapTokens, int minChunkChars, List<Integer> headingLevels)`。
- Repository evidence: `DeepResearchTaskBO` 的 record 写法；`AgentFlowConfigDTO` 的策略枚举用法。
- Dependencies and consumers: 被 `RagIngestionCommand` 承载；被三个策略读取。
- Why now: 策略的参数类型。
- Contract/signature changes: 新增公开载体。
- Input/output and state mapping: 默认值 `overlapTokens=0`、`minChunkChars=1`、`headingLevels=[1,2,3]`；`headingLevels` 去重并稳定排序后不可变。
- Error and edge behavior: `maxTokensPerChunk` 不在 32-4096、`overlapTokens` 不小于 `maxTokensPerChunk`、`minChunkChars` 不在 0-4096、`headingLevels` 取值不在 1-6 或有重复、非 `MARKDOWN_HEADING` 却传非空 `headingLevels`——全部抛 `RagValidationException` 并指明字段名。
- Standards impact: `MC-MODEL-001`、`MC-VALID-001`、`MC-SCOPE-001`、`MC-TEST-001`。
- Literal rule enforcement: `Rule 1`（`DTO` 后缀）、`Rule 2`、`Rule 3`、`Rule 11`。
- Implementation pseudocode:

```java
public record RagChunkingConfigDTO(RagChunkingStrategyEnum strategy,
                                   int maxTokensPerChunk,
                                   int overlapTokens,
                                   int minChunkChars,
                                   List<Integer> headingLevels) {
    public RagChunkingConfigDTO {
        if (strategy == null) throw new RagValidationException("strategy must not be null");
        overlapTokens = overlapTokens < 0 ? 0 : overlapTokens;
        minChunkChars = minChunkChars <= 0 ? 1 : minChunkChars;
        if (maxTokensPerChunk < 32 || maxTokensPerChunk > 4096) throw new RagValidationException("maxTokensPerChunk out of range");
        if (overlapTokens >= maxTokensPerChunk) throw new RagValidationException("overlapTokens must be smaller than maxTokensPerChunk");
        headingLevels = normalizeLevels(strategy, headingLevels);
    }
    private static List<Integer> normalizeLevels(RagChunkingStrategyEnum strategy, List<Integer> levels) {
        if (levels == null || levels.isEmpty()) {
            if (strategy == MARKDOWN_HEADING) return List.of(1, 2, 3);
            return List.of();                        // 非 Markdown 策略留空
        }
        if (strategy != MARKDOWN_HEADING) throw new RagValidationException("headingLevels is only valid for MARKDOWN_HEADING");
        if (levels.stream().anyMatch(level -> level < 1 || level > 6)) throw new RagValidationException("headingLevels out of range");
        return levels.stream().distinct().sorted().toList();
    }
}
```

- Verification contribution: `TEST-005` 全部边界用例。
- After this file: 分块配置载体就绪。

#### File 6 — `CREATE src/main/java/top/egon/cola/component/rag/chunk/RagChunkingStrategyEnum.java`

- Purpose: 关闭的策略取值集合，取代字符串分派。
- Symbols: `enum RagChunkingStrategyEnum { TOKEN, MARKDOWN_HEADING, RECURSIVE }`。
- Repository evidence: `AgentWorkflowTypeEnum` 的枚举用法。
- Dependencies and consumers: 被 `RagChunkingConfigDTO` 与工厂消费；被 Spec B 的建库接口序列化。
- Why now: 类型前提。
- Contract/signature changes: 新增公开枚举（只增不改）。
- Input/output and state mapping: 无。
- Error and edge behavior: 未知值由 Spring 绑定失败在启动期暴露，不进入枚举。
- Standards impact: `MC-NAME-001`（`Enum` 后缀）、`MC-PATTERN-001`、`MC-SCOPE-001`、`MC-TEST-001`。
- Literal rule enforcement: `Rule 1`、`Rule 9`、`Rule 11`。
- Implementation pseudocode:

```java
/**
 * 分块策略的关闭取值集合，取代字符串分派（Rule 9）。
 * 只增不改：既有取值语义变化会破坏 REQ-009 的幂等保证。
 */
public enum RagChunkingStrategyEnum {

    /** 按 token 预算切分，复用 Spring AI TokenTextSplitter。 */
    TOKEN,

    /** 先按 Markdown 标题层级切分，超长段落再套 token 切分。 */
    MARKDOWN_HEADING,

    /** 按分隔符级联递归切分，兜底按字符硬切。 */
    RECURSIVE
}
```

- Verification contribution: `TEST-003`。
- After this file: 枚举就绪。

#### File 7 — `CREATE src/main/java/top/egon/cola/component/rag/chunk/RagChunkingStrategy.java`

- Purpose: 定义"抽取结果 + 分块配置 → 分块列表"的可扩展契约。
- Symbols: `interface RagChunkingStrategy { RagChunkingStrategyEnum strategy(); List<RagChunkBO> split(ExtractedDocumentBO document, RagChunkingConfigDTO config); String name(); }`。
- Repository evidence: `AgentWorkflowBuilderStrategy` 的接口形态。
- Dependencies and consumers: 被工厂持有；被使用方实现新算法。
- Why now: 策略实现与工厂的类型前提。
- Contract/signature changes: 新增公开 SPI。
- Input/output and state mapping: 返回列表的 `chunkIndex` 从 0 连续；实现必须确定性。
- Error and edge behavior: 实现抛 `RagValidationException`（参数）或 `RagChunkingException`（内部错误）。
- Standards impact: `MC-PATTERN-001`、`MC-NAME-001`、`MC-SCOPE-001`、`MC-TEST-001`。
- Literal rule enforcement: `Rule 1`、`Rule 9`、`Rule 11`。
- Implementation pseudocode:

```java
public interface RagChunkingStrategy {
    RagChunkingStrategyEnum strategy();
    List<RagChunkBO> split(ExtractedDocumentBO document, RagChunkingConfigDTO config);
    default String name() { return getClass().getSimpleName(); }
}
```

- Verification contribution: `TEST-003`、`TEST-005`。
- After this file: SPI 就绪。

#### File 8 — `CREATE src/main/java/top/egon/cola/component/rag/chunk/TokenRagChunkingStrategy.java`

- Purpose: 以 token 预算切分，复用 Spring AI `TokenTextSplitter`。
- Symbols: `@Component("tokenRagChunkingStrategy")`；`split` 用 `TokenTextSplitter.builder()` 构造并映射为 `RagChunkBO`。
- Repository evidence: `spring-ai-commons` 的 `TokenTextSplitter`（`javap` 已核实 builder 与 `(int,int,int,int,boolean)` 构造器）。
- Dependencies and consumers: 被工厂解析。
- Why now: 三种策略之一，且是默认策略。
- Contract/signature changes: 新增实现。
- Input/output and state mapping: `config.maxTokensPerChunk()` → splitter 的 chunk size；`config.overlapTokens()` → 重叠；产出文本按顺序编号并从 0 起。
- Error and edge behavior: 空文本产出空列表；超长单段由 splitter 内部处理，异常包装为 `RagChunkingException`。
- Standards impact: `MC-REUSE-001`（复用官方切分器）、`MC-PATTERN-001`、`MC-BEAN-001`、`MC-LOG-001`、`MC-SCOPE-001`、`MC-TEST-001`。
- Literal rule enforcement: `Rule 4`、`Rule 9`、`Rule 11`。
- Implementation pseudocode:

```java
@Slf4j
@Component("tokenRagChunkingStrategy")
public class TokenRagChunkingStrategy implements RagChunkingStrategy {

    @Override public RagChunkingStrategyEnum strategy() { return RagChunkingStrategyEnum.TOKEN; }

    @Override public List<RagChunkBO> split(ExtractedDocumentBO document, RagChunkingConfigDTO config) {
        TokenTextSplitter splitter = TokenTextSplitter.builder()
                .withChunkSize(config.maxTokensPerChunk())
                .withMinChunkSizeChars(config.minChunkChars())
                .withMinChunkLengthToEmbed(1)
                .withMaxNumChunks(10_000)
                .withKeepSeparator(true)
                .build();
        List<String> pieces = splitter.apply(List.of(new Document(document.text()))).stream()
                .map(Document::getText).filter(piece -> !piece.isBlank()).toList();
        return toChunks(pieces, Map.of());
    }
}
```

- Verification contribution: `TEST-003`、`TEST-005`、`TEST-009`。
- After this file: `TOKEN` 策略可用。

#### File 9 — `CREATE src/main/java/top/egon/cola/component/rag/chunk/MarkdownHeadingRagChunkingStrategy.java`

- Purpose: 先按标题层级切分，再对超长段落套 token 切分（两级切分）。
- Symbols: `@Component("markdownHeadingRagChunkingStrategy")`；私有方法 `splitByHeadings(String, List<Integer>)` 与 `splitOversized(...)`。
- Repository evidence: 无同类。
- Dependencies and consumers: 被工厂解析；`attributes` 记录标题路径供检索展示。
- Why now: 用户要求的分块算法之一。
- Contract/signature changes: 新增实现；产出分块的 `attributes` 增加 `headingPath`。
- Input/output and state mapping: 按 `config.headingLevels()` 识别标题行；每段超过 `maxTokensPerChunk` 时委托 token 切分并保持顺序与编号连续。
- Error and edge behavior: 文本无标题时退化为整篇按 token 切分（不抛错）；参数非法由 `RagChunkingConfigDTO` 提前拒绝。
- Standards impact: `MC-PATTERN-001`、`MC-BEAN-001`、`MC-LOG-001`、`MC-UTIL-001`（只用 JDK 做行扫描）、`MC-SCOPE-001`、`MC-TEST-001`。
- Literal rule enforcement: `Rule 4`、`Rule 5`、`Rule 9`、`Rule 11`。
- Implementation pseudocode:

```java
@Slf4j
@Component("markdownHeadingRagChunkingStrategy")
public class MarkdownHeadingRagChunkingStrategy implements RagChunkingStrategy {

    @Override public RagChunkingStrategyEnum strategy() { return RagChunkingStrategyEnum.MARKDOWN_HEADING; }

    @Override public List<RagChunkBO> split(ExtractedDocumentBO document, RagChunkingConfigDTO config) {
        List<Section> sections = splitByHeadings(document.text(), config.headingLevels());   // 无标题时单段
        List<RagChunkBO> chunks = new ArrayList<>();
        for (Section section : sections) {
            for (String piece : splitOversized(section.content(), config)) {
                if (piece.isBlank()) continue;
                chunks.add(new RagChunkBO(chunks.size(), piece, Map.of("headingPath", section.headingPath())));
            }
        }
        return List.copyOf(chunks);
    }
}
```

- Verification contribution: `TEST-005`、`TEST-009`。
- After this file: `MARKDOWN_HEADING` 策略可用。

#### File 10 — `CREATE src/main/java/top/egon/cola/component/rag/chunk/RecursiveRagChunkingStrategy.java`

- Purpose: 按分隔符级联切分（空行 → 换行 → 句号 → 空格 → 硬切）。
- Symbols: `@Component("recursiveRagChunkingStrategy")`；分隔符序列常量为私有静态字段。
- Repository evidence: 无同类。
- Dependencies and consumers: 被工厂解析。
- Why now: 用户要求的分块算法之一。
- Contract/signature changes: 新增实现。
- Input/output and state mapping: 递归尝试分隔符直到每段不超过 `maxTokensPerChunk`；超长兜底按字符硬切；编号连续。
- Error and edge behavior: 空文本产出空列表；分隔符全部失效时按 `maxTokensPerChunk` 对应的字符上界硬切，不抛错。
- Standards impact: `MC-PATTERN-001`、`MC-BEAN-001`、`MC-LOG-001`、`MC-UTIL-001`、`MC-SCOPE-001`、`MC-TEST-001`。
- Literal rule enforcement: `Rule 4`、`Rule 5`、`Rule 9`、`Rule 11`。
- Implementation pseudocode:

```java
@Slf4j
@Component("recursiveRagChunkingStrategy")
public class RecursiveRagChunkingStrategy implements RagChunkingStrategy {

    private static final List<String> SEPARATORS = List.of("\n\n", "\n", "。", ". ", " ");

    @Override public RagChunkingStrategyEnum strategy() { return RagChunkingStrategyEnum.RECURSIVE; }

    @Override public List<RagChunkBO> split(ExtractedDocumentBO document, RagChunkingConfigDTO config) {
        List<String> pieces = new ArrayList<>();
        splitRecursively(document.text(), 0, config.maxTokensPerChunk(), config.overlapTokens(), pieces);
        return toChunks(pieces.stream().filter(piece -> !piece.isBlank()).toList(), Map.of());
    }
}
```

- Verification contribution: `TEST-005`、`TEST-009`。
- After this file: 三种策略齐备。

#### File 11 — `CREATE src/main/java/top/egon/cola/component/rag/chunk/RagChunkingStrategyFactory.java`

- Purpose: 枚举到策略实现的映射与缺失失败。
- Symbols: `@Component("ragChunkingStrategyFactory")`；方法 `RagChunkingStrategy resolve(RagChunkingStrategyEnum strategy)`。
- Repository evidence: `AgentWorkflowStrategyFactory` 的工厂写法。
- Dependencies and consumers: 被 Step 7 的摄取服务消费。
- Why now: 工厂是策略选择的唯一入口。
- Contract/signature changes: 新增公开方法。
- Input/output and state mapping: 枚举 → 策略；构造期建立不可变映射表。
- Error and edge behavior: 枚举缺失对应实现时 `resolve` 抛 `RagConfigurationException`（含缺失枚举名与已注册集合）。
- Standards impact: `MC-PATTERN-001`、`MC-BEAN-001`、`MC-LOG-001`、`MC-SCOPE-001`、`MC-TEST-001`。
- Literal rule enforcement: `Rule 1`、`Rule 4`、`Rule 9`、`Rule 11`。
- Implementation pseudocode:

```java
@Slf4j
@Component("ragChunkingStrategyFactory")
@RequiredArgsConstructor
public class RagChunkingStrategyFactory {

    private final @Qualifier("ragChunkingStrategies") List<RagChunkingStrategy> strategies;
    private Map<RagChunkingStrategyEnum, RagChunkingStrategy> registry;

    @PostConstruct
    void initialize() {
        registry = strategies.stream().collect(toUnmodifiableMap(RagChunkingStrategy::strategy, identity()));
        log.info("rag chunking strategies ready: {}", registry.keySet());
    }

    public RagChunkingStrategy resolve(RagChunkingStrategyEnum strategy) {
        RagChunkingStrategy resolved = registry.get(strategy);
        if (resolved == null) throw new RagConfigurationException(
                "no chunking strategy registered for " + strategy + "; registered: " + registry.keySet());
        return resolved;
    }
}
```

- Verification contribution: `TEST-003`。
- After this file: 策略选择就绪。

#### File 12 — `CREATE src/main/java/top/egon/cola/component/rag/chunk/RagChunkIdFactory.java`

- Purpose: 生成确定性分块标识，作为幂等重跑的基础。
- Symbols: `@Component("ragChunkIdFactory")`；方法 `String create(String documentId, int chunkIndex)`。
- Repository evidence: 无同类。
- Dependencies and consumers: 被 Step 7 的摄取服务消费。
- Why now: 与策略同批，两者共同构成 `REQ-009`。
- Contract/signature changes: 新增公开方法。
- Input/output and state mapping: `(documentId, chunkIndex)` → `documentId + ":" + chunkIndex`。
- Error and edge behavior: `documentId` 空白或含 `:`、`chunkIndex` 为负时抛 `RagValidationException`。
- Standards impact: `MC-NAME-001`（`Factory` 后缀）、`MC-BEAN-001`、`MC-SCOPE-001`、`MC-TEST-001`。
- Literal rule enforcement: `Rule 1`、`Rule 4`、`Rule 11`。
- Implementation pseudocode:

```java
@Component("ragChunkIdFactory")
public class RagChunkIdFactory {
    public String create(String documentId, int chunkIndex) {
        if (documentId == null || documentId.isBlank()) throw new RagValidationException("documentId must not be blank");
        if (documentId.indexOf(':') >= 0) throw new RagValidationException("documentId must not contain ':'");
        if (chunkIndex < 0) throw new RagValidationException("chunkIndex must not be negative");
        return documentId + ":" + chunkIndex;
    }
}
```

- Verification contribution: `TEST-009`。
- After this file: Step 6 全部文件就位（含 `chunk` 与 `model` 包的 `package-info.java`）。

- Validation working directory: `/Users/mario/SelfProject/Egon-COLA/egon-cola-components/egon-cola-component-rag-starter`
- Verification command: `../../mvnw -B -ntp -pl . test -Dtest='RagChunkingStrategyFactoryTest,RagChunkingStrategyTest,RagChunkIdFactoryTest'`
- Expected result: 三个测试类通过；`TEST-009` 的两次切分断言逐字段一致。
- Failure returns to: File 8/9/10（策略实现）、File 11（工厂映射）、File 5（跨字段校验）。
- Completion criteria: `REQ-005` 与 `REQ-009` 各有可执行证据；三种策略均确定性。
- Rollback: 回退本 Step 全部文件。
- Commit paths: `src/test/java/top/egon/cola/component/rag/chunk/RagChunkingStrategyFactoryTest.java`; `src/test/java/top/egon/cola/component/rag/chunk/RagChunkingStrategyTest.java`; `src/test/java/top/egon/cola/component/rag/chunk/RagChunkIdFactoryTest.java`; `src/main/java/top/egon/cola/component/rag/model/RagChunkBO.java`; `src/main/java/top/egon/cola/component/rag/model/RagChunkingConfigDTO.java`; `src/main/java/top/egon/cola/component/rag/chunk/RagChunkingStrategyEnum.java`; `src/main/java/top/egon/cola/component/rag/chunk/RagChunkingStrategy.java`; `src/main/java/top/egon/cola/component/rag/chunk/TokenRagChunkingStrategy.java`; `src/main/java/top/egon/cola/component/rag/chunk/MarkdownHeadingRagChunkingStrategy.java`; `src/main/java/top/egon/cola/component/rag/chunk/RecursiveRagChunkingStrategy.java`; `src/main/java/top/egon/cola/component/rag/chunk/RagChunkingStrategyFactory.java`; `src/main/java/top/egon/cola/component/rag/chunk/RagChunkIdFactory.java`
- Commit: `feat(rag): add chunking strategies and deterministic chunk ids`

### Step 7 — 嵌入与写入链路（转换器、元数据规范与摄取服务）

- Requirements: `REQ-009`, `REQ-010`, `REQ-011`, `REQ-019`, `REQ-020`
- Dependencies: Step 3, Step 4, Step 6
- Baseline state: 模型注册表、抽取服务与切块链路均可用；不存在把分块写入向量库的能力。
- Observable outcome: 调用 `RagIngestionService#ingest` 时先按 `documentId` 删除既有向量再写入；结构元数据与业务属性按"业务属性同名优先"合并且保留 key 被拒绝；返回的 `chunkCount` 等于写入的分块数；删除或写入失败时抛出确定异常且不残留新的部分结果。
- End state: `RagMetadataKeys`、`RagChunkConverter`、`RagIngestionCommand`、`RagIngestionResult`、`RagIngestionService`、`RagIngestionServiceImpl` 就绪；`TEST-025` 证明两段式组合不重复解析。
- Test-first gate: `Required` — 两个测试类首次运行时编译失败（服务与转换器不存在）。
- Manual Checks: `MC-ARCH-001`, `MC-NAME-001`, `MC-MODEL-001`, `MC-VALID-001`, `MC-CONVERT-001`, `MC-BEAN-001`, `MC-LOG-001`, `MC-UTIL-001`, `MC-TIME-001`, `MC-PATTERN-001`, `MC-SCOPE-001`, `MC-TEST-001`
- Literal Rules: `Rule 1`, `Rule 2`, `Rule 3`, `Rule 4`, `Rule 5`, `Rule 9`, `Rule 10`, `Rule 11`
- Ordered files:

#### File 1 — `CREATE src/test/java/top/egon/cola/component/rag/converter/RagChunkConverterTest.java`

- Purpose: 定义分块与 `Document` 的双向映射与元数据契约。
- Symbols: `maps_chunk_to_document_with_reserved_metadata()`、`maps_document_back_to_chunk()`、`keeps_score_null_when_absent()`。
- Repository evidence: `ResearchEventConverterTest` 的逐字段映射断言风格。
- Dependencies and consumers: 消费 `RagChunkConverter` 与 `RagMetadataKeys`。
- Why now: 转换器是摄取与检索共用的边界，先定契约。
- Contract/signature changes: 引入转换器与保留键常量类。
- Input/output and state mapping: `RagChunkBO` + 保留元数据 → `Document`（`id`、`text`、`metadata`）；反向从 `Document` 还原 `collectionId`、`documentId`、`chunkIndex`、`logicalModelName` 与业务属性。
- Error and edge behavior: `score` 缺失时保持 `null`；保留 key 出现在业务属性中时由调用方（服务）拒绝，转换器本身不做策略判断。
- Standards impact: `MC-CONVERT-001`（MapStruct + `BaseConverter`）、`MC-MODEL-001`、`MC-SCOPE-001`、`MC-TEST-001`。
- Literal rule enforcement: `Rule 3`（MapStruct + `BaseConverter`，禁止手工 set/get）、`Rule 11`。
- Implementation pseudocode:

```java
class RagChunkConverterTest {
    @Test void maps_chunk_to_document_with_reserved_metadata() {
        RagChunkBO chunk = new RagChunkBO(7, "content", Map.of("source", "manual"));
        Document document = RagChunkConverter.INSTANCE.toTarget(chunk, "doc-1:7", reservedMetadata());
        assertThat(document.getId()).isEqualTo("doc-1:7");
        assertThat(document.getText()).isEqualTo("content");
        assertThat(document.getMetadata()).containsEntry("chunkIndex", 7).containsEntry("source", "manual");
    }

    @Test void keeps_score_null_when_absent() {
        assertThat(RagChunkConverter.INSTANCE.toSource(new Document("doc-1:7", "content", metadata())).score()).isNull();
    }
}
```

- Verification contribution: `TEST-008` 的 RED 契约。
- After this file: 编译失败。

#### File 2 — `CREATE src/test/java/top/egon/cola/component/rag/execution/RagIngestionServiceImplTest.java`

- Purpose: 定义摄取的编排、失败语义与元数据合并规则。
- Symbols: `deletes_before_writing()`、`returns_chunk_count_and_identity_fields()`、`skips_write_when_delete_fails()`、`rejects_reserved_attribute_keys()`、`merges_structural_and_business_attributes_with_business_winning()`。
- Repository evidence: `FakeVectorStore` 的失败点与调用记录（Step 3 File 2）。
- Dependencies and consumers: 消费 `RagIngestionService`、`FakeEmbeddingModel`、`FakeVectorStore`。
- Why now: 本 Step 的核心 RED 契约。
- Contract/signature changes: 引入 `RagIngestionCommand`、`RagIngestionResult`、`RagIngestionService`。
- Input/output and state mapping: 命令（集合、文档、逻辑模型、分块配置、已抽取文档、业务属性）→ 结果（集合、文档、逻辑模型、维度、分块数、耗时）。
- Error and edge behavior: 删除失败时 `add` 未被调用；`attributes` 含保留 key 抛 `RagValidationException` 且未调用向量库；同名 key 业务属性胜出。
- Standards impact: `MC-VALID-001`、`MC-CONVERT-001`、`MC-BEAN-001`、`MC-LOG-001`、`MC-TIME-001`、`MC-PATTERN-001`、`MC-SCOPE-001`、`MC-TEST-001`。
- Literal rule enforcement: `Rule 1`、`Rule 2`、`Rule 4`、`Rule 9`、`Rule 10`、`Rule 11`。
- Implementation pseudocode:

```java
class RagIngestionServiceImplTest {
    @Test void deletes_before_writing() {
        service = new RagIngestionServiceImpl(modelRegistry(1536), chunkFactory(), idFactory(), converter(), vectorStore, clock);
        service.ingest(command("kb-1", "doc-1", "openai-small"));
        assertThat(vectorStore.deletedExpressions()).hasSize(1);
        assertThat(vectorStore.addedDocuments()).isNotEmpty();
    }

    @Test void skips_write_when_delete_fails() {
        vectorStore.failOn(DELETE);
        assertThatThrownBy(() -> service.ingest(command(...))).isInstanceOf(RagVectorStoreException.class);
        assertThat(vectorStore.addedDocuments()).isEmpty();
    }

    @Test void rejects_reserved_attribute_keys() {
        assertThatThrownBy(() -> service.ingest(commandWithAttributes(Map.of("documentId", "x"))))
            .isInstanceOf(RagValidationException.class);
        assertThat(vectorStore.addedDocuments()).isEmpty();
    }

    @Test void merges_structural_and_business_attributes_with_business_winning() { /* 同名 key 断言业务值 */ }
}
```

- Verification contribution: `TEST-006`、`TEST-010` 的 RED 契约。
- After this file: 编译失败。

#### File 3 — `CREATE src/test/java/top/egon/cola/component/rag/execution/TwoPhaseIngestionTest.java`

- Purpose: 证明"同一文档在一次端到端流程中只被解析一次"。
- Symbols: `extraction_runs_once_across_extract_and_two_ingest_calls()`。
- Repository evidence: `FakeVectorStore` 与可计数 stub 抽取器。
- Dependencies and consumers: 组合消费 `RagExtractionService` 与 `RagIngestionService`。
- Why now: `REQ-019` 的核心证据；没有它，两段式设计可以被实现成"摄取内部又解析一次"。
- Contract/signature changes: 无新增契约，仅组合既有接口。
- Input/output and state mapping: 抽取一次 → 落库文本（测试内以局部变量模拟）→ 两次 `ingest` → 抽取器计数为 1，两次分块 id 集合一致。
- Error and edge behavior: 若实现内部重新解析，抽取器计数为 2，测试失败。
- Standards impact: `MC-SCOPE-001`、`MC-TEST-001`。
- Literal rule enforcement: `Rule 11`。
- Implementation pseudocode:

```java
class TwoPhaseIngestionTest {
    @Test void extraction_runs_once_across_extract_and_two_ingest_calls() {
        CountingExtractor extractor = new CountingExtractor();
        extractionService = new RagExtractionServiceImpl(new RagDocumentExtractorRegistry(List.of(extractor)));
        ingestionService = new RagIngestionServiceImpl(...);

        ExtractedDocumentBO document = extractionService.extract(new RagExtractionCommand("a.txt", "text/plain", stream("hello world")));
        ingestionService.ingest(commandOf(document));
        ingestionService.ingest(commandOf(document));

        assertThat(extractor.invocationCount()).isEqualTo(1);
        assertThat(firstChunkIds).isEqualTo(secondChunkIds);
    }
}
```

- Verification contribution: `TEST-025`。
- After this file: 编译失败。

#### File 4 — `CREATE src/main/java/top/egon/cola/component/rag/metadata/RagMetadataKeys.java`

- Purpose: 定义保留元数据键与写入校验。
- Symbols: `final class RagMetadataKeys`（私有构造）；常量 `COLLECTION_ID="collectionId"`、`DOCUMENT_ID="documentId"`、`CHUNK_INDEX="chunkIndex"`、`LOGICAL_MODEL_NAME="logicalModelName"`、`CONTENT_HASH="contentHash"`；方法 `Set<String> reservedKeys()`、`void rejectReservedKeys(Map<String, String> attributes)`、`Map<String, Object> toDocumentMetadata(...)`。
- Repository evidence: 无同类。
- Dependencies and consumers: 被摄取与检索服务、`RagChunkConverter` 与抽取服务消费。
- Why now: 转换器与摄取服务都依赖它。
- Contract/signature changes: 新增公开常量类。
- Input/output and state mapping: 业务属性 → 校验后的不可变集合。
- Error and edge behavior: 出现任一保留 key 抛 `RagValidationException`（消息指明 key），**拒绝而非静默剥离**。
- Standards impact: `MC-NAME-001`、`MC-SCOPE-001`、`MC-TEST-001`。此处**不新增 `*Utils`**：它是规则常量与校验契约的持有者，不是通用工具类。
- Literal rule enforcement: `Rule 5`（不新增工具类，只持有常量与校验）、`Rule 11`。
- Implementation pseudocode:

```java
public final class RagMetadataKeys {
    public static final String COLLECTION_ID = "collectionId";
    public static final String DOCUMENT_ID = "documentId";
    public static final String CHUNK_INDEX = "chunkIndex";
    public static final String LOGICAL_MODEL_NAME = "logicalModelName";
    public static final String CONTENT_HASH = "contentHash";

    private RagMetadataKeys() { }

    public static Set<String> reservedKeys() { return Set.of(COLLECTION_ID, DOCUMENT_ID, CHUNK_INDEX, LOGICAL_MODEL_NAME, CONTENT_HASH); }

    public static void rejectReservedKeys(Map<String, String> attributes) {
        if (attributes == null) return;
        Set<String> offending = attributes.keySet().stream().filter(reservedKeys()::contains).collect(toSet());
        if (!offending.isEmpty()) throw new RagValidationException("reserved metadata keys must not be supplied: " + offending);
    }

    public static Map<String, Object> toDocumentMetadata(String collectionId, String documentId, String logicalModelName,
                                                         int chunkIndex, Map<String, String> mergedAttributes) {
        rejectReservedKeys(mergedAttributes);
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put(COLLECTION_ID, collectionId);
        metadata.put(DOCUMENT_ID, documentId);
        metadata.put(LOGICAL_MODEL_NAME, logicalModelName);
        metadata.put(CHUNK_INDEX, chunkIndex);
        metadata.putAll(mergedAttributes);
        return Map.copyOf(metadata);
    }
}
```

- Verification contribution: `TEST-006`、`TEST-008`。
- After this file: 元数据规范就绪。

#### File 5 — `CREATE src/main/java/top/egon/cola/component/rag/converter/RagChunkConverter.java`

- Purpose: 分块与 Spring AI `Document` 的双向映射。
- Symbols: `@Mapper(unmappedTargetPolicy = ReportingPolicy.ERROR)` 接口 `extends BaseConverter<RagChunkBO, Document>`；`INSTANCE = Mappers.getMapper(RagChunkConverter.class)`；方法 `Document toTarget(RagChunkBO source, String chunkId, Map<String, Object> metadata)` 与 `RagRetrievedChunkBO toSource(Document target)`。
- Repository evidence: `egon-cola-source-agent/.../converter/ResearchEventConverter.java` 的 `@Mapper` + `INSTANCE = Mappers.getMapper(...)` + 逐字段 `@Mapping` 写法。
- Dependencies and consumers: 被 Step 7 摄取与 Step 8 检索消费。
- Why now: 摄取服务需要它；RED 契约已由 File 1 固定。
- Contract/signature changes: 新增公开转换器；`BaseConverter` 的 `toSource`/`toTarget` 由带额外界参的重载承担（`Document` 的 `id` 与元数据不是 `RagChunkBO` 的字段，**这是与 `BaseConverter` 契约的差异，已在 Spec A `§10.4` 明确**）。
- Input/output and state mapping: `RagChunkBO.chunkIndex` → `metadata[chunkIndex]`；`content` → `Document.text`；`attributes` → 其余 `metadata`；反向同理；`score` 取自 `Document.getScore()`（可能 `null`）。
- Error and edge behavior: 任何未映射字段在编译期失败（`unmappedTargetPolicy=ERROR`）；`score` 缺失保持 `null`。
- Standards impact: `MC-CONVERT-001`、`MC-MODEL-001`、`MC-SCOPE-001`、`MC-TEST-001`。
- Literal rule enforcement: `Rule 1`（`Converter` 行为后缀）、`Rule 3`（MapStruct + `BaseConverter`；不使用 `BeanUtils.copyProperties`、反射或 JSON 往返）、`Rule 10`（不使用 `BaseConverter` 的遗留 `Date` 方法）、`Rule 11`。
- Implementation pseudocode:

```java
@Mapper(unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface RagChunkConverter extends BaseConverter<RagChunkBO, Document> {

    RagChunkConverter INSTANCE = Mappers.getMapper(RagChunkConverter.class);

    // Document 的 id 与 metadata 不由 RagChunkBO 携带，因此显式接收
    default Document toDocument(RagChunkBO source, String chunkId, Map<String, Object> metadata) {
        return new Document(chunkId, source.content(), metadata);
    }

    @Override
    @Mapping(target = "chunkIndex", expression = "java(Integer.parseInt(String.valueOf(target.getMetadata().get(\"chunkIndex\"))))")
    @Mapping(target = "content", source = "text")
    @Mapping(target = "attributes", expression = "java(businessAttributes(target.getMetadata()))")
    @Mapping(target = "score", source = "score")
    RagRetrievedChunkBO toSource(Document target);

    @Override
    default RagChunkBO toTarget(RagChunkBO source) {
        throw new UnsupportedOperationException("use toDocument(source, chunkId, metadata)");
    }
}
```

- Verification contribution: `TEST-008`。
- After this file: 转换器就绪。

#### File 6 — `CREATE src/main/java/top/egon/cola/component/rag/model/RagIngestionCommand.java`

- Purpose: 摄取入口的写意图载体，携带已抽取文档。
- Symbols: `record RagIngestionCommand(@NotBlank String collectionId, @NotBlank String documentId, @NotBlank String logicalModelName, @NotNull @Valid RagChunkingConfigDTO chunkingConfig, @NotNull @Valid ExtractedDocumentBO document, Map<String, String> attributes)`。
- Repository evidence: `StartDeepResearchCommand` 的 record + Jakarta 注解写法。
- Dependencies and consumers: 被 `RagIngestionService#ingest` 消费；被 Spec B 的 outbox handler 构造。
- Why now: 服务接口的入参类型。
- Contract/signature changes: 新增公开载体；**不接受 `InputStream`**（`REQ-019`）。
- Input/output and state mapping: 紧凑构造器 `trim` 字符串、`attributes` 缺省空表并做不可变副本；`collectionId` 与 `documentId` 必须匹配 `[A-Za-z0-9._:-]+` 且 `documentId` 不含 `:`。
- Error and edge behavior: 任一约束违反抛 `RagValidationException`。
- Standards impact: `MC-NAME-001`（`Command` 后缀）、`MC-MODEL-001`、`MC-VALID-001`、`MC-SCOPE-001`、`MC-TEST-001`。
- Literal rule enforcement: `Rule 1`、`Rule 2`、`Rule 3`、`Rule 11`。
- Implementation pseudocode:

```java
public record RagIngestionCommand(@NotBlank String collectionId,
                                  @NotBlank String documentId,
                                  @NotBlank String logicalModelName,
                                  @NotNull @Valid RagChunkingConfigDTO chunkingConfig,
                                  @NotNull @Valid ExtractedDocumentBO document,
                                  Map<String, String> attributes) {
    private static final Pattern IDENTIFIER = Pattern.compile("[A-Za-z0-9._:-]{1,128}");

    public RagIngestionCommand {
        collectionId = normalizeIdentifier(collectionId, "collectionId");
        documentId = normalizeIdentifier(documentId, "documentId");
        if (documentId.contains(":")) throw new RagValidationException("documentId must not contain ':'");
        logicalModelName = logicalModelName == null ? null : logicalModelName.trim();
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
        RagMetadataKeys.rejectReservedKeys(attributes);
    }
}
```

- Verification contribution: `TEST-006`、`TEST-005`。
- After this file: 摄取命令就绪。

#### File 7 — `CREATE src/main/java/top/egon/cola/component/rag/model/RagIngestionResult.java`

- Purpose: 摄取结果载体。
- Symbols: `record RagIngestionResult(String collectionId, String documentId, String logicalModelName, int dimensions, int chunkCount, Duration elapsed)`。
- Repository evidence: `DeepResearchEvent` 的 record 写法。
- Dependencies and consumers: 被 `RagIngestionService#ingest` 返回；被 Spec B 用于回写文档状态。
- Why now: 服务接口的出参类型。
- Contract/signature changes: 新增公开载体。
- Input/output and state mapping: `chunkCount ≥ 0`；`elapsed` 非负；`dimensions` 为正。
- Error and edge behavior: 违反约束抛 `RagValidationException`。
- Standards impact: `MC-NAME-001`（`Result` 行为后缀）、`MC-MODEL-001`、`MC-TIME-001`（`Duration`）、`MC-SCOPE-001`、`MC-TEST-001`。
- Literal rule enforcement: `Rule 1`、`Rule 3`、`Rule 10`、`Rule 11`。
- Implementation pseudocode:

```java
public record RagIngestionResult(String collectionId, String documentId, String logicalModelName,
                                 int dimensions, int chunkCount, Duration elapsed) {
    public RagIngestionResult {
        if (chunkCount < 0) throw new RagValidationException("chunkCount must not be negative");
        if (elapsed == null || elapsed.isNegative()) throw new RagValidationException("elapsed must not be negative");
        if (dimensions <= 0) throw new RagValidationException("dimensions must be positive");
    }
}
```

- Verification contribution: `TEST-006`、`TEST-007`。
- After this file: 摄取结果就绪。

#### File 8 — `CREATE src/main/java/top/egon/cola/component/rag/api/RagIngestionService.java`

- Purpose: 摄取能力的公开入口。
- Symbols: `interface RagIngestionService { RagIngestionResult ingest(@Valid RagIngestionCommand command); }`。
- Repository evidence: `AgentFlowService` 的公开接口写法。
- Dependencies and consumers: 被使用方（Spec B 的 handler）调用。
- Why now: 实现类的类型前提。
- Contract/signature changes: 新增公开接口。
- Input/output and state mapping: 命令 → 结果。
- Error and edge behavior: 见实现类。
- Standards impact: `MC-NAME-001`、`MC-SCOPE-001`、`MC-TEST-001`。
- Literal rule enforcement: `Rule 1`、`Rule 11`。
- Implementation pseudocode:

```java
/**
 * 摄取能力的公开入口：把一份**已抽取**的文档切块、嵌入并写入宿主的向量库。
 * 本接口不解析任何文档格式（解析属 RagExtractionService）；
 * 实现保证先按 documentId 删除既有分块再写入，因此同一文档重跑安全。
 */
public interface RagIngestionService {

    /**
     * @param command 集合、文档、逻辑模型、分块配置与已抽取文档
     * @return 写入的分块数与本次调用统计
     * @throws RagValidationException 字段约束或属性保留键失败
     * @throws RagModelNotRegisteredException 逻辑模型未注册
     * @throws RagEmbeddingException 嵌入调用失败
     * @throws RagVectorStoreException 预删除或写入失败
     */
    RagIngestionResult ingest(@Valid RagIngestionCommand command);
}
```

- Verification contribution: `TEST-006`。
- After this file: 接口就绪。

#### File 9 — `CREATE src/main/java/top/egon/cola/component/rag/execution/RagIngestionServiceImpl.java`

- Purpose: 摄取编排：选策略 → 生成 id → 合并元数据 → 先删后写 → 返回统计。
- Symbols: `@Service("ragIngestionService")` + `@Validated` 的类；方法 `ingest(RagIngestionCommand)`。
- Repository evidence: `DeepResearchManageImpl` 的 `@Service("...")` + `@RequiredArgsConstructor` + `@Qualifier` + `@Slf4j` 写法。
- Dependencies and consumers: 依赖 `ragEmbeddingModelRegistry`、`ragChunkingStrategyFactory`、`ragChunkIdFactory`、`ragVectorStore`、`ragClock`。
- Why now: RED 契约已由 File 2 固定。
- Contract/signature changes: 实现 `RagIngestionService`。
- Input/output and state mapping: 命令 → `registry.resolve(logicalModelName)` → `factory.resolve(strategy)` → `strategy.split` → `idFactory.create` 逐分块 → 业务属性覆盖结构属性 → `metadata = RagMetadataKeys.toDocumentMetadata(...)` → `converter.toDocument(...)` → `vectorStore.delete(Expression)` → `vectorStore.add(documents)` → 结果。**注意：查询与写入都不经过模型调用之外的外部系统。**
- Error and edge behavior: 删除失败抛 `RagVectorStoreException` 且不调用 `add`；嵌入失败抛 `RagEmbeddingException`；写入失败抛 `RagVectorStoreException`（可能残留部分分块，重跑自愈）；属性校验失败在调用向量库之前抛出。
- Standards impact: `MC-BEAN-001`、`MC-VALID-001`、`MC-CONVERT-001`、`MC-LOG-001`（`@Slf4j`，只记标识、逻辑模型名、策略、分块数、耗时与结果）、`MC-TIME-001`、`MC-PATTERN-001`、`MC-UTIL-001`、`MC-SCOPE-001`、`MC-TEST-001`。
- Literal rule enforcement: `Rule 1`、`Rule 2`、`Rule 3`（经转换器，不手工构造 `Document` 的字段映射）、`Rule 4`、`Rule 5`、`Rule 9`、`Rule 10`、`Rule 11`。
- Implementation pseudocode:

```java
@Slf4j
@Service("ragIngestionService")
@Validated
@RequiredArgsConstructor
public class RagIngestionServiceImpl implements RagIngestionService {

    private final @Qualifier("ragEmbeddingModelRegistry") RagEmbeddingModelRegistry modelRegistry;
    private final @Qualifier("ragChunkingStrategyFactory") RagChunkingStrategyFactory strategyFactory;
    private final @Qualifier("ragChunkIdFactory") RagChunkIdFactory chunkIdFactory;
    private final @Qualifier("ragVectorStore") VectorStore vectorStore;
    private final @Qualifier("ragClock") Clock clock;

    @Override
    public RagIngestionResult ingest(@Valid RagIngestionCommand command) {
        Instant startedAt = clock.instant();
        RagEmbeddingModelDescriptorBO descriptor = modelRegistry.resolve(command.logicalModelName());
        RagChunkingStrategy strategy = strategyFactory.resolve(command.chunkingConfig().strategy());
        List<RagChunkBO> chunks = strategy.split(command.document(), command.chunkingConfig());

        Map<String, String> merged = new LinkedHashMap<>(command.document().attributes());
        merged.putAll(command.attributes());                       // 业务属性同名优先
        RagMetadataKeys.rejectReservedKeys(merged);

        List<Document> documents = new ArrayList<>(chunks.size());
        for (RagChunkBO chunk : chunks) {
            String chunkId = chunkIdFactory.create(command.documentId(), chunk.chunkIndex());
            documents.add(RagChunkConverter.INSTANCE.toDocument(chunk, chunkId,
                    RagMetadataKeys.toDocumentMetadata(command.collectionId(), command.documentId(),
                            descriptor.logicalName(), chunk.chunkIndex(), merged)));
        }

        log.info("rag ingestion started: collection={}, document={}, model={}, strategy={}, chunks={}",
                 command.collectionId(), command.documentId(), descriptor.logicalName(),
                 command.chunkingConfig().strategy(), chunks.size());

        vectorStore.delete(FilterExpressionBuilder.eq(RagMetadataKeys.DOCUMENT_ID, command.documentId()));
        vectorStore.add(documents);

        Duration elapsed = Duration.between(startedAt, clock.instant());
        log.info("rag ingestion finished: collection={}, document={}, chunks={}, durationMs={}, result=SUCCESS",
                 command.collectionId(), command.documentId(), chunks.size(), elapsed.toMillis());
        return new RagIngestionResult(command.collectionId(), command.documentId(), descriptor.logicalName(),
                                      descriptor.dimensions(), chunks.size(), elapsed);
    }
}
```

- Verification contribution: `TEST-006`、`TEST-007`、`TEST-010`、`TEST-025`。
- After this file: 摄取链路 GREEN。

#### File 10 — `CREATE src/test/java/top/egon/cola/component/rag/execution/RagIngestionServiceImplTest.java` 的失败点断言补全

- Purpose: 补全 `add` 失败与嵌入失败的断言，以及 `elapsed` 由可注入时钟计算的断言。
- Symbols: `skips_write_when_embedding_fails()`、`propagates_write_failure()`、`computes_elapsed_from_injected_clock()`。
- Repository evidence: `FakeVectorStore.FailurePoint` 与固定 `Clock`。
- Dependencies and consumers: 消费 `RagIngestionServiceImpl` 与替身。
- Why now: 与 File 9 同批，使 Step 的失败语义完整。
- Contract/signature changes: 无。
- Input/output and state mapping: 失败点 → 抛出的异常类型；固定时钟 → `elapsed` 的确定性值。
- Error and edge behavior: 嵌入失败时 `add` 未被调用；写入失败时异常向上传播且不被吞掉。
- Standards impact: `MC-TIME-001`、`MC-SCOPE-001`、`MC-TEST-001`。
- Literal rule enforcement: `Rule 10`、`Rule 11`。
- Implementation pseudocode:

```java
@Test void skips_write_when_embedding_fails() {
    model.failOnEmbed();
    assertThatThrownBy(() -> service.ingest(command(...))).isInstanceOf(RagEmbeddingException.class);
    assertThat(vectorStore.addedDocuments()).isEmpty();
}

@Test void computes_elapsed_from_injected_clock() {
    service = serviceWithFixedClock(Instant.parse("2026-09-10T00:00:00Z"), Instant.parse("2026-09-10T00:00:02Z"));
    assertThat(service.ingest(command(...)).elapsed()).isEqualTo(Duration.ofSeconds(2));
}
```

- Verification contribution: `TEST-007`、`TEST-010`。
- After this file: Step 7 全部文件就位（含 `metadata`、`converter` 包的 `package-info.java`）。

- Validation working directory: `/Users/mario/SelfProject/Egon-COLA/egon-cola-components/egon-cola-component-rag-starter`
- Verification command: `../../mvnw -B -ntp -pl . test -Dtest='RagChunkConverterTest,RagIngestionServiceImplTest,TwoPhaseIngestionTest'`
- Expected result: 三个测试类通过；`deletes_before_writing` 证明删除先于写入；`extraction_runs_once_across_extract_and_two_ingest_calls` 证明抽取器计数为 1。
- Failure returns to: File 9（编排顺序或元数据合并）、File 5（转换器映射）、File 4（保留键校验）。
- Completion criteria: `REQ-009`-`REQ-011`、`REQ-019`、`REQ-020` 各有可执行证据。
- Rollback: 回退本 Step 全部文件；装配未修改。
- Commit paths: `src/test/java/top/egon/cola/component/rag/converter/RagChunkConverterTest.java`; `src/test/java/top/egon/cola/component/rag/execution/RagIngestionServiceImplTest.java`; `src/test/java/top/egon/cola/component/rag/execution/TwoPhaseIngestionTest.java`; `src/main/java/top/egon/cola/component/rag/metadata/RagMetadataKeys.java`; `src/main/java/top/egon/cola/component/rag/converter/RagChunkConverter.java`; `src/main/java/top/egon/cola/component/rag/model/RagIngestionCommand.java`; `src/main/java/top/egon/cola/component/rag/model/RagIngestionResult.java`; `src/main/java/top/egon/cola/component/rag/api/RagIngestionService.java`; `src/main/java/top/egon/cola/component/rag/execution/RagIngestionServiceImpl.java`; `src/test/java/top/egon/cola/component/rag/execution/RagIngestionServiceImplTest.java`
- Commit: `feat(rag): ingest extracted documents into the vector store`

### Step 8 — 检索服务与强制过滤

- Requirements: `REQ-007`
- Dependencies: Step 3
- Baseline state: 摄取链路可写入向量；不存在检索能力。
- Observable outcome: 检索结果按相似度降序且只属于目标集合与目标逻辑模型；调用方无法省略或覆盖这两个过滤条件；无命中返回空列表；参数越界返回校验异常。
- End state: `RagRetrievalQuery`、`RagRetrievedChunkBO`、`RagRetrievalService`、`RagRetrievalServiceImpl` 就绪。
- Test-first gate: `Required` — 测试首次运行时编译失败（服务与查询载体不存在）。
- Manual Checks: `MC-ARCH-001`, `MC-NAME-001`, `MC-MODEL-001`, `MC-VALID-001`, `MC-CONVERT-001`, `MC-BEAN-001`, `MC-LOG-001`, `MC-TIME-001`, `MC-PATTERN-001`, `MC-SCOPE-001`, `MC-TEST-001`
- Literal Rules: `Rule 1`, `Rule 2`, `Rule 3`, `Rule 4`, `Rule 10`, `Rule 11`
- Ordered files:

#### File 1 — `CREATE src/test/java/top/egon/cola/component/rag/execution/RagRetrievalServiceImplTest.java`

- Purpose: 定义强制过滤、排序与空结果语义。
- Symbols: `forces_collection_and_model_filters()`、`returns_results_in_descending_score_order()`、`returns_empty_list_when_no_hit()`、`rejects_top_k_above_configured_maximum()`。
- Repository evidence: `FakeVectorStore#searchRequests` 的记录能力。
- Dependencies and consumers: 消费 `RagRetrievalService` 与 `FakeVectorStore`。
- Why now: `REQ-007` 的唯一可执行证据。
- Contract/signature changes: 引入查询与结果载体、检索服务。
- Input/output and state mapping: 查询（集合、逻辑模型、文本、`topK`、阈值、业务属性）→ `SearchRequest` → 带分结果。
- Error and edge behavior: `topK` 超上限或阈值越界抛 `RagValidationException`；无命中返回空列表而非 `null`；依赖失败抛 `RagVectorStoreException`，**不返回降级结果**。
- Standards impact: `MC-VALID-001`、`MC-CONVERT-001`、`MC-BEAN-001`、`MC-LOG-001`（不记录查询原文与命中内容）、`MC-TIME-001`、`MC-PATTERN-001`、`MC-SCOPE-001`、`MC-TEST-001`。
- Literal rule enforcement: `Rule 1`、`Rule 2`、`Rule 4`、`Rule 10`、`Rule 11`。
- Implementation pseudocode:

```java
class RagRetrievalServiceImplTest {
    @Test void forces_collection_and_model_filters() {
        service.retrieve(query("kb-1", "openai-small", "how to configure timeout"));
        SearchRequest request = vectorStore.searchRequests().get(0);
        assertThat(request.getFilterExpression().toString())
            .contains("collectionId").contains("kb-1").contains("logicalModelName").contains("openai-small");
    }

    @Test void rejects_top_k_above_configured_maximum() {
        assertThatThrownBy(() -> service.retrieve(queryWithTopK(999))).isInstanceOf(RagValidationException.class);
    }
}
```

- Verification contribution: `TEST-016`、`TEST-017` 的 RED 契约。
- After this file: 编译失败。

#### File 2 — `CREATE src/main/java/top/egon/cola/component/rag/model/RagRetrievalQuery.java`

- Purpose: 检索入口的读意图载体。
- Symbols: `record RagRetrievalQuery(@NotBlank String collectionId, @NotBlank String logicalModelName, @NotBlank @Size(max=2000) String query, int topK, double similarityThreshold, Map<String, String> attributes)`。
- Repository evidence: agent archetype 的查询载体写法。
- Dependencies and consumers: 被 `RagRetrievalService#retrieve` 消费。
- Why now: 服务接口的入参类型。
- Contract/signature changes: 新增公开载体；`topK` 为 0 表示取配置默认值。
- Input/output and state mapping: 紧凑构造器 `trim` 字符串、`attributes` 缺省空表并拒绝保留 key。
- Error and edge behavior: `similarityThreshold` 不在 0.0-1.0、属性含保留 key 抛 `RagValidationException`。
- Standards impact: `MC-NAME-001`（`Query` 后缀）、`MC-MODEL-001`、`MC-VALID-001`、`MC-SCOPE-001`、`MC-TEST-001`。
- Literal rule enforcement: `Rule 1`、`Rule 2`、`Rule 3`、`Rule 11`。
- Implementation pseudocode:

```java
public record RagRetrievalQuery(@NotBlank String collectionId, @NotBlank String logicalModelName,
                                @NotBlank @Size(max = 2000) String query, int topK,
                                double similarityThreshold, Map<String, String> attributes) {
    public RagRetrievalQuery {
        collectionId = trimToNull(collectionId);
        logicalModelName = trimToNull(logicalModelName);
        query = trimToNull(query);
        if (similarityThreshold < 0.0 || similarityThreshold > 1.0) {
            throw new RagValidationException("similarityThreshold must be between 0.0 and 1.0");
        }
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
        RagMetadataKeys.rejectReservedKeys(attributes);
    }
}
```

- Verification contribution: `TEST-016`。
- After this file: 查询载体就绪。

#### File 3 — `CREATE src/main/java/top/egon/cola/component/rag/model/RagRetrievedChunkBO.java`

- Purpose: 检索结果载体。
- Symbols: `record RagRetrievedChunkBO(String chunkId, String collectionId, String documentId, int chunkIndex, String content, Double score, String logicalModelName, Map<String, String> attributes)`。
- Repository evidence: `DeepResearchTaskBO` 的 record 写法。
- Dependencies and consumers: 被检索服务返回；被 Spec B 映射为 VO。
- Why now: 服务接口的出参类型。
- Contract/signature changes: 新增公开载体。
- Input/output and state mapping: `score` 允许 `null`；`attributes` 不可变副本。
- Error and edge behavior: `chunkId`、`collectionId`、`documentId`、`content`、`logicalModelName` 非空；`chunkIndex ≥ 0`。
- Standards impact: `MC-NAME-001`（`BO` 后缀）、`MC-MODEL-001`、`MC-SCOPE-001`、`MC-TEST-001`。
- Literal rule enforcement: `Rule 1`、`Rule 3`、`Rule 11`。
- Implementation pseudocode:

```java
public record RagRetrievedChunkBO(String chunkId, String collectionId, String documentId, int chunkIndex,
                                  String content, Double score, String logicalModelName,
                                  Map<String, String> attributes) {
    public RagRetrievedChunkBO { attributes = attributes == null ? Map.of() : Map.copyOf(attributes); }
}
```

- Verification contribution: `TEST-016`。
- After this file: 结果载体就绪。

#### File 4 — `CREATE src/main/java/top/egon/cola/component/rag/api/RagRetrievalService.java`

- Purpose: 检索能力的公开入口。
- Symbols: `interface RagRetrievalService { List<RagRetrievedChunkBO> retrieve(@Valid RagRetrievalQuery query); }`。
- Repository evidence: `AgentFlowService` 的公开接口写法。
- Dependencies and consumers: 被使用方（Spec B 的检索与问答）调用。
- Why now: 实现类的类型前提。
- Contract/signature changes: 新增公开接口。
- Input/output and state mapping: 查询 → 结果列表。
- Error and edge behavior: 见实现类。
- Standards impact: `MC-NAME-001`、`MC-SCOPE-001`、`MC-TEST-001`。
- Literal rule enforcement: `Rule 1`、`Rule 11`。
- Implementation pseudocode:

```java
/**
 * 检索能力的公开入口：在集合与逻辑模型的双重约束下取回带分片段。
 * 实现强制注入 collectionId 与 logicalModelName 两个过滤条件，调用方无法省略或覆盖；
 * 无命中返回空列表；依赖失败抛异常而不是返回空列表。
 */
public interface RagRetrievalService {

    /**
     * @param query 集合、逻辑模型、查询文本、条数上限、相似度下限与业务过滤属性
     * @return 按相似度降序的片段；无命中时为空列表，不为 null
     * @throws RagValidationException topK 越界或阈值越界
     * @throws RagModelNotRegisteredException 逻辑模型未注册
     * @throws RagVectorStoreException 向量库调用失败
     */
    List<RagRetrievedChunkBO> retrieve(@Valid RagRetrievalQuery query);
}
```

- Verification contribution: `TEST-017`。
- After this file: 接口就绪。

#### File 5 — `CREATE src/main/java/top/egon/cola/component/rag/execution/RagRetrievalServiceImpl.java`

- Purpose: 检索编排与强制过滤。
- Symbols: `@Service("ragRetrievalService")` + `@Validated` 的类；方法 `retrieve(RagRetrievalQuery)`。
- Repository evidence: `DeepResearchManageImpl` 的 Bean 与注入写法。
- Dependencies and consumers: 依赖 `ragEmbeddingModelRegistry`、`ragVectorStore`、`ragProperties`、`ragClock`。
- Why now: RED 契约已由 File 1 固定。
- Contract/signature changes: 实现 `RagRetrievalService`。
- Input/output and state mapping: `modelRegistry.resolve(logicalModelName)` 校验模型已注册并取维度 → 构造强制过滤表达式 `collectionId == ? AND logicalModelName == ?` → 追加业务属性等值条件 → `SearchRequest.builder().query(...).topK(effectiveTopK).similarityThreshold(...).filterExpression(...)` → `vectorStore.similaritySearch` → 经转换器映射为结果列表并按 `score` 降序（`null` 排在最后）。
- Error and edge behavior: `topK` 为 0 时取 `rag.retrieval.default-top-k`；超过 `max-top-k` 抛 `RagValidationException`；模型未注册抛 `RagModelNotRegisteredException`；向量库失败抛 `RagVectorStoreException`，不返回空列表掩盖失败。
- Standards impact: `MC-BEAN-001`、`MC-VALID-001`、`MC-CONVERT-001`、`MC-LOG-001`（只记集合、模型、`topK`、命中数与耗时）、`MC-TIME-001`、`MC-PATTERN-001`、`MC-SCOPE-001`、`MC-TEST-001`。
- Literal rule enforcement: `Rule 1`、`Rule 2`、`Rule 3`、`Rule 4`、`Rule 10`、`Rule 11`。
- Implementation pseudocode:

```java
@Slf4j
@Service("ragRetrievalService")
@Validated
@RequiredArgsConstructor
public class RagRetrievalServiceImpl implements RagRetrievalService {

    private final @Qualifier("ragEmbeddingModelRegistry") RagEmbeddingModelRegistry modelRegistry;
    private final @Qualifier("ragVectorStore") VectorStore vectorStore;
    private final @Qualifier("ragProperties") RagProperties ragProperties;
    private final @Qualifier("ragClock") Clock clock;

    @Override
    public List<RagRetrievedChunkBO> retrieve(@Valid RagRetrievalQuery query) {
        Instant startedAt = clock.instant();
        RagEmbeddingModelDescriptorBO descriptor = modelRegistry.resolve(query.logicalModelName());
        int topK = query.topK() == 0 ? ragProperties.retrieval().defaultTopK() : query.topK();
        if (topK > ragProperties.retrieval().maxTopK()) {
            throw new RagValidationException("topK must not exceed " + ragProperties.retrieval().maxTopK());
        }

        Filter.Expression filter = FilterExpressionBuilder.and(
                FilterExpressionBuilder.eq(RagMetadataKeys.COLLECTION_ID, query.collectionId()),
                FilterExpressionBuilder.eq(RagMetadataKeys.LOGICAL_MODEL_NAME, descriptor.logicalName()),
                businessEquals(query.attributes()));

        SearchRequest request = SearchRequest.builder().query(query.query()).topK(topK)
                .similarityThreshold(query.similarityThreshold()).filterExpression(filter).build();

        List<RagRetrievedChunkBO> results = vectorStore.similaritySearch(request).stream()
                .map(RagChunkConverter.INSTANCE::toSource)
                .sorted(comparing(RagRetrievedChunkBO::score, nullsLast(naturalOrder())).reversed())
                .toList();

        log.info("rag retrieval finished: collection={}, model={}, topK={}, hits={}, durationMs={}, result=SUCCESS",
                 query.collectionId(), descriptor.logicalName(), topK, results.size(),
                 Duration.between(startedAt, clock.instant()).toMillis());
        return results;
    }
}
```

- Verification contribution: `TEST-016`、`TEST-017`。
- After this file: 检索链路 GREEN，Step 8 完成（含 `retrieve` 包与 `api` 包的 `package-info.java`）。

- Validation working directory: `/Users/mario/SelfProject/Egon-COLA/egon-cola-components/egon-cola-component-rag-starter`
- Verification command: `../../mvnw -B -ntp -pl . test -Dtest='RagRetrievalServiceImplTest'`
- Expected result: 全部用例通过；`forces_collection_and_model_filters` 断言过滤表达式的字符串包含两个保留键与取值。
- Failure returns to: File 5（过滤构造或排序）、File 2（参数校验）。
- Completion criteria: `REQ-007` 有可执行证据；调用方无法提交不含强制过滤的请求。
- Rollback: 回退本 Step 全部文件。
- Commit paths: `src/test/java/top/egon/cola/component/rag/execution/RagRetrievalServiceImplTest.java`; `src/main/java/top/egon/cola/component/rag/model/RagRetrievalQuery.java`; `src/main/java/top/egon/cola/component/rag/model/RagRetrievedChunkBO.java`; `src/main/java/top/egon/cola/component/rag/api/RagRetrievalService.java`; `src/main/java/top/egon/cola/component/rag/execution/RagRetrievalServiceImpl.java`
- Commit: `feat(rag): add retrieval service with forced filters`

### Step 9 — 存储扩展点与本地文件系统默认实现

- Requirements: `REQ-012`
- Dependencies: Step 2
- Baseline state: 组件可装配，但不存在文件存储能力。
- Observable outcome: 无自定义实现时本地实现生效；写入—读取—删除往返内容一致；含 `..` 或分隔符的标识被拒绝且根目录外无文件；根目录不可写时抛 `RagStorageException`；宿主提供实现时默认实现不再创建。
- End state: `RagDocumentStorage`、`RagDocumentStorageTypeEnum`、`RagStoredObjectBO`、`LocalFileSystemRagDocumentStorage` 与 `FakeRagDocumentStorage` 就绪。
- Test-first gate: `Required` — `LocalFileSystemRagDocumentStorageTest` 首次运行时编译失败（SPI 与实现不存在）。
- Manual Checks: `MC-ARCH-001`, `MC-NAME-001`, `MC-MODEL-001`, `MC-VALID-001`, `MC-BEAN-001`, `MC-LOG-001`, `MC-UTIL-001`, `MC-TIME-001`, `MC-PATTERN-001`, `MC-SCOPE-001`, `MC-TEST-001`
- Literal Rules: `Rule 1`, `Rule 4`, `Rule 5`, `Rule 9`, `Rule 10`, `Rule 11`
- Ordered files:

#### File 1 — `CREATE src/test/java/top/egon/cola/component/rag/storage/LocalFileSystemRagDocumentStorageTest.java`

- Purpose: 定义往返、路径安全、失败与覆盖四条行为。
- Symbols: `round_trips_content()`、`rejects_path_traversal_identifier()`、`reports_failure_when_root_not_writable()`、`host_implementation_replaces_default()`。
- Repository evidence: JUnit `@TempDir`；`agent-flow-starter` 的测试组织方式。
- Dependencies and consumers: 消费本地实现与 `ragDocumentStorage` Bean。
- Why now: `REQ-012` 的唯一可执行证据。
- Contract/signature changes: 引入 SPI、类型枚举与结果载体。
- Input/output and state mapping: 字节流 + 标识 → 存储结果；`open` → 字节流；`delete` → 无返回。
- Error and edge behavior: 标识含 `..`、`/`、`\` 或绝对路径前缀时抛 `RagValidationException` 且根目录外无文件；根目录只读抛 `RagStorageException`；`open` 目标不存在抛 `RagStorageException` 而非返回 `null`。
- Standards impact: `MC-VALID-001`、`MC-UTIL-001`（只用 JDK `java.nio`）、`MC-BEAN-001`、`MC-TIME-001`（`Instant` 写入时刻）、`MC-PATTERN-001`、`MC-SCOPE-001`、`MC-TEST-001`。
- Literal rule enforcement: `Rule 1`、`Rule 4`、`Rule 5`、`Rule 9`、`Rule 10`、`Rule 11`。
- Implementation pseudocode:

```java
class LocalFileSystemRagDocumentStorageTest {
    @TempDir Path root;

    @Test void round_trips_content() {
        storage = new LocalFileSystemRagDocumentStorage(new RagLocalStorageProperties(root.toString()), fixedClock());
        RagStoredObjectBO stored = storage.store("kb-1", "doc-1", "report.txt", stream("hello"));
        assertThat(new String(storage.open("kb-1", "doc-1").readAllBytes(), UTF_8)).isEqualTo("hello");
        storage.delete("kb-1", "doc-1");
        assertThatThrownBy(() -> storage.open("kb-1", "doc-1")).isInstanceOf(RagStorageException.class);
    }

    @Test void rejects_path_traversal_identifier() {
        assertThatThrownBy(() -> storage.store("../escape", "doc-1", "a.txt", stream("x")))
            .isInstanceOf(RagValidationException.class);
        assertThat(root.getParent().resolve("escape")).doesNotExist();
    }

    @Test void host_implementation_replaces_default() { /* ApplicationContextRunner + 自定义 Bean 断言默认实现缺席 */ }
}
```

- Verification contribution: `TEST-020` 的 RED 契约。
- After this file: 编译失败。

#### File 2 — `CREATE src/test/java/top/egon/cola/component/rag/support/FakeRagDocumentStorage.java`

- Purpose: 提供内存存储替身，供后续与 Spec B 的离线测试使用。
- Symbols: `class FakeRagDocumentStorage implements RagDocumentStorage`；字段 `Map<String, byte[]>` 与 `failurePoint`。
- Repository evidence: `FakeVectorStore` 的同类风格（Step 3 File 2）。
- Dependencies and consumers: 被 Step 11 的装配测试与 Spec B 的摄取测试消费。
- Why now: 与本地实现同批，避免后续 Step 再回头补替身。
- Contract/signature changes: 实现 SPI。
- Input/output and state mapping: 标识 → 字节数组。
- Error and edge behavior: `failurePoint=STORE|OPEN|DELETE` 时对应方法抛 `RagStorageException`。
- Standards impact: `MC-MODEL-001`、`MC-SCOPE-001`、`MC-TEST-001`。
- Literal rule enforcement: `Rule 11`。
- Implementation pseudocode:

```java
class FakeRagDocumentStorage implements RagDocumentStorage {
    enum FailurePoint { NONE, STORE, OPEN, DELETE }
    private final Map<String, byte[]> objects = new LinkedHashMap<>();
    private FailurePoint failurePoint = NONE;

    @Override public RagDocumentStorageTypeEnum type() { return RagDocumentStorageTypeEnum.LOCAL; }
    @Override public RagStoredObjectBO store(String collectionId, String documentId, String fileName, InputStream content) {
        if (failurePoint == STORE) throw new RagStorageException("simulated store failure");
        byte[] bytes = readAllBytes(content);
        objects.put(key(collectionId, documentId), bytes);
        return new RagStoredObjectBO(collectionId, documentId, type(), bytes.length, Instant.EPOCH);
    }
    @Override public InputStream open(String collectionId, String documentId) {
        if (failurePoint == OPEN) throw new RagStorageException("simulated open failure");
        byte[] bytes = objects.get(key(collectionId, documentId));
        if (bytes == null) throw new RagStorageException("stored object not found");
        return new ByteArrayInputStream(bytes);
    }
    @Override public void delete(String collectionId, String documentId) { objects.remove(key(collectionId, documentId)); }
}
```

- Verification contribution: 支撑 Step 11 与 Spec B 的离线测试。
- After this file: 替身就绪。

#### File 3 — `CREATE src/main/java/top/egon/cola/component/rag/storage/RagDocumentStorageTypeEnum.java`

- Purpose: 关闭的存储类型取值集合，为后续 OSS 实现预留。
- Symbols: `enum RagDocumentStorageTypeEnum { LOCAL }`。
- Repository evidence: `AgentWorkflowTypeEnum` 的枚举用法。
- Dependencies and consumers: 被配置、SPI 与本地实现消费。
- Why now: 类型前提。
- Contract/signature changes: 新增公开枚举（只增不改）。
- Input/output and state mapping: 无。
- Error and edge behavior: 未知值由绑定失败暴露。
- Standards impact: `MC-NAME-001`（`Enum` 后缀）、`MC-PATTERN-001`、`MC-SCOPE-001`、`MC-TEST-001`。
- Literal rule enforcement: `Rule 1`、`Rule 9`、`Rule 11`。
- Implementation pseudocode:

```java
/**
 * 原文件存储类型的关闭取值集合。
 * 只增不改：后续接入对象存储时新增 OSS，既有 LOCAL 语义不变。
 */
public enum RagDocumentStorageTypeEnum {

    /** 本地文件系统实现，多实例部署下不可共享。 */
    LOCAL
}
```

- Verification contribution: `TEST-020`。
- After this file: 枚举就绪。

#### File 4 — `CREATE src/main/java/top/egon/cola/component/rag/storage/RagStoredObjectBO.java`

- Purpose: 存储结果载体。
- Symbols: `record RagStoredObjectBO(String collectionId, String documentId, RagDocumentStorageTypeEnum storageType, long sizeBytes, Instant storedAt)`。
- Repository evidence: `DeepResearchTaskBO` 的 record 写法。
- Dependencies and consumers: 被存储实现返回。
- Why now: SPI 的返回类型。
- Contract/signature changes: 新增公开载体。
- Input/output and state mapping: `sizeBytes ≥ 0`；`storedAt` 为 UTC `Instant`。
- Error and edge behavior: 违反约束抛 `RagValidationException`。
- Standards impact: `MC-NAME-001`（`BO` 后缀）、`MC-MODEL-001`、`MC-TIME-001`、`MC-SCOPE-001`、`MC-TEST-001`。
- Literal rule enforcement: `Rule 1`、`Rule 3`、`Rule 10`、`Rule 11`。
- Implementation pseudocode:

```java
public record RagStoredObjectBO(String collectionId, String documentId, RagDocumentStorageTypeEnum storageType,
                                long sizeBytes, Instant storedAt) {
    public RagStoredObjectBO {
        if (sizeBytes < 0) throw new RagValidationException("sizeBytes must not be negative");
        if (storedAt == null) throw new RagValidationException("storedAt must not be null");
    }
}
```

- Verification contribution: `TEST-020`。
- After this file: 载体就绪。

#### File 5 — `CREATE src/main/java/top/egon/cola/component/rag/storage/RagDocumentStorage.java`

- Purpose: 原文件写入、读取与删除的抽象。
- Symbols: `interface RagDocumentStorage { RagDocumentStorageTypeEnum type(); RagStoredObjectBO store(String collectionId, String documentId, String fileName, InputStream content); InputStream open(String collectionId, String documentId); void delete(String collectionId, String documentId); }`。
- Repository evidence: 无同类。
- Dependencies and consumers: 被使用方调用；被使用方实现新后端（OSS）。
- Why now: 实现与装配的类型前提。
- Contract/signature changes: 新增公开 SPI。
- Input/output and state mapping: 标识 + 字节流 → 存储结果。
- Error and edge behavior: 实现须拒绝路径穿越标识；`open` 目标不存在须抛错；`delete` 须幂等。
- Standards impact: `MC-PATTERN-001`、`MC-NAME-001`、`MC-SCOPE-001`、`MC-TEST-001`。
- Literal rule enforcement: `Rule 1`、`Rule 9`、`Rule 11`。
- Implementation pseudocode:

```java
public interface RagDocumentStorage {
    RagDocumentStorageTypeEnum type();
    RagStoredObjectBO store(String collectionId, String documentId, String fileName, InputStream content);
    InputStream open(String collectionId, String documentId);
    void delete(String collectionId, String documentId);
}
```

- Verification contribution: `TEST-020`。
- After this file: SPI 就绪。

#### File 6 — `CREATE src/main/java/top/egon/cola/component/rag/storage/LocalFileSystemRagDocumentStorage.java`

- Purpose: 本地文件系统默认实现，含路径规范化与原子改名。
- Symbols: `@Component("localFileSystemRagDocumentStorage")` + `@ConditionalOnMissingBean(RagDocumentStorage.class)` 的类；私有方法 `resolve(String collectionId, String documentId)` 与 `sanitize(String fileName)`。
- Repository evidence: 无同类；只用 JDK `java.nio`（`Path`、`Files`、`StandardCopyOption.ATOMIC_MOVE`）。
- Dependencies and consumers: 被装配发布为 `ragDocumentStorage`；被显式断言 `storage.type()` 与配置一致。
- Why now: RED 契约已由 File 1 固定。
- Contract/signature changes: 实现 SPI；目录布局为 `<root>/<collectionId>/<documentId>/<sanitizedFileName>`。
- Input/output and state mapping: `collectionId`/`documentId` 经 `[A-Za-z0-9._:-]+` 白名单校验；`fileName` 只取基名并剥离分隔符；写入先落同目录临时文件再 `ATOMIC_MOVE`。
- Error and edge behavior: 标识非法抛 `RagValidationException`；目录不可创建或改名失败抛 `RagStorageException`；`open` 目标不存在抛 `RagStorageException`；`delete` 目标不存在视为成功（幂等）。
- Standards impact: `MC-BEAN-001`、`MC-LOG-001`（只记标识、类型、字节数与耗时）、`MC-UTIL-001`、`MC-TIME-001`、`MC-PATTERN-001`、`MC-SCOPE-001`、`MC-TEST-001`。
- Literal rule enforcement: `Rule 1`、`Rule 4`、`Rule 5`、`Rule 9`、`Rule 10`、`Rule 11`。
- Implementation pseudocode:

```java
@Slf4j
@Component("localFileSystemRagDocumentStorage")
@ConditionalOnMissingBean(RagDocumentStorage.class)
@RequiredArgsConstructor
public class LocalFileSystemRagDocumentStorage implements RagDocumentStorage {

    private static final Pattern SEGMENT = Pattern.compile("[A-Za-z0-9._:-]{1,128}");
    private final @Qualifier("ragProperties") RagProperties ragProperties;
    private final @Qualifier("ragClock") Clock clock;

    @Override public RagDocumentStorageTypeEnum type() { return RagDocumentStorageTypeEnum.LOCAL; }

    @Override
    public RagStoredObjectBO store(String collectionId, String documentId, String fileName, InputStream content) {
        Path directory = directory(collectionId, documentId);
        Path target = directory.resolve(sanitize(fileName));
        Path temporary = directory.resolve(target.getFileName() + ".tmp");
        try {
            Files.createDirectories(directory);
            long size = Files.copy(content, temporary, REPLACE_EXISTING);
            Files.move(temporary, target, ATOMIC_MOVE, REPLACE_EXISTING);
            log.info("rag document stored: collection={}, document={}, type=LOCAL, bytes={}",
                     collectionId, documentId, size);
            return new RagStoredObjectBO(collectionId, documentId, type(), size, clock.instant());
        } catch (IOException exception) {
            throw new RagStorageException("failed to store document for " + documentId, exception);
        }
    }

    @Override public InputStream open(String collectionId, String documentId) {
        Path directory = directory(collectionId, documentId);
        try (var files = Files.list(directory)) {
            return Files.newInputStream(files.findFirst()
                .orElseThrow(() -> new RagStorageException("stored object not found for " + documentId)));
        } catch (IOException exception) {
            throw new RagStorageException("failed to open document for " + documentId, exception);
        }
    }

    @Override public void delete(String collectionId, String documentId) {
        try { deleteRecursively(directory(collectionId, documentId)); }            // 目标不存在视为成功
        catch (IOException exception) { throw new RagStorageException("failed to delete document " + documentId, exception); }
    }
}
```

- Verification contribution: `TEST-020` 全部用例。
- After this file: 存储扩展点就绪。

#### File 7 — `MODIFY src/main/java/top/egon/cola/component/rag/autoconfigure/RagAutoConfiguration.java`

- Purpose: 校验 `storage.type` 与已注册实现的 `type()` 一致。
- Symbols: `validateStorageType(RagProperties, RagDocumentStorage)`。
- Repository evidence: File 6 的 `type()` 契约。
- Dependencies and consumers: 装配期校验；使用方实现 OSS 后此处是唯一的类型一致性门。
- Why now: 默认实现与 SPI 都已存在，校验才有对象。
- Contract/signature changes: 新增一条启动期校验。
- Input/output and state mapping: `ragProperties.storage().type()` ↔ `ragDocumentStorage.type()`。
- Error and edge behavior: 不一致抛 `RagConfigurationException` 并同时给出两个取值。
- Standards impact: `MC-BEAN-001`、`MC-CONFIG-001`、`MC-SCOPE-001`、`MC-TEST-001`。
- Literal rule enforcement: `Rule 4`、`Rule 11`。
- Implementation pseudocode:

```java
@Bean(name = "ragStorageTypeValidator")
@ConditionalOnMissingBean(name = "ragStorageTypeValidator")
public Object ragStorageTypeValidator(@Qualifier("ragProperties") RagProperties properties,
                                      @Qualifier("ragDocumentStorage") RagDocumentStorage storage) {
    if (properties.storage().type() != storage.type()) {
        throw new RagConfigurationException("storage.type is " + properties.storage().type()
                + " but the registered RagDocumentStorage reports " + storage.type());
    }
    return new Object();
}
```

- Verification contribution: `TEST-020` 的类型一致用例与 Step 11 的装配测试。
- After this file: Step 9 全部文件就位（含 `storage` 包的 `package-info.java`）。

- Validation working directory: `/Users/mario/SelfProject/Egon-COLA/egon-cola-components/egon-cola-component-rag-starter`
- Verification command: `../../mvnw -B -ntp -pl . test -Dtest='LocalFileSystemRagDocumentStorageTest'`
- Expected result: 全部用例通过；路径穿越用例同时断言根目录外无文件；覆盖用例断言默认实现缺席。
- Failure returns to: File 6（路径规范化或原子改名）、File 7（类型校验）。
- Completion criteria: `REQ-012` 有可执行证据；本地实现可被宿主 Bean 整体替换。
- Rollback: 回退本 Step 全部文件（含 `AutoConfiguration` 的类型校验）。
- Commit paths: `src/test/java/top/egon/cola/component/rag/storage/LocalFileSystemRagDocumentStorageTest.java`; `src/test/java/top/egon/cola/component/rag/support/FakeRagDocumentStorage.java`; `src/main/java/top/egon/cola/component/rag/storage/RagDocumentStorageTypeEnum.java`; `src/main/java/top/egon/cola/component/rag/storage/RagStoredObjectBO.java`; `src/main/java/top/egon/cola/component/rag/storage/RagDocumentStorage.java`; `src/main/java/top/egon/cola/component/rag/storage/LocalFileSystemRagDocumentStorage.java`; `src/main/java/top/egon/cola/component/rag/autoconfigure/RagAutoConfiguration.java`
- Commit: `feat(rag): add document storage spi with local default`

### Step 10 — 可选启动期往返探针

- Requirements: `REQ-018`
- Dependencies: Step 2, Step 3
- Baseline state: 装配可完成离线校验；不存在联网探针。
- Observable outcome: 默认配置下启动不产生任何模型调用与向量写入；开启 `validation.probe-on-startup=true` 时对每个逻辑模型执行一次写入—检索—删除往返，任一环节失败即启动失败并指明模型与阶段。
- End state: `RagVectorStoreProbe` 与装配钩子就绪；探针使用保留集合标识 `__egon_rag_probe__` 并在结束后清理自身记录。
- Test-first gate: `Required` — `RagVectorStoreProbeTest` 首次运行时编译失败（探针类不存在）。
- Manual Checks: `MC-ARCH-001`, `MC-NAME-001`, `MC-BEAN-001`, `MC-LOG-001`, `MC-PATTERN-001`, `MC-SCOPE-001`, `MC-TEST-001`
- Literal Rules: `Rule 1`, `Rule 4`, `Rule 11`
- Ordered files:

#### File 1 — `CREATE src/test/java/top/egon/cola/component/rag/execution/RagVectorStoreProbeTest.java`

- Purpose: 定义"默认不产生任何调用"与"开启后失败即启动失败"两条行为。
- Symbols: `produces_no_model_call_when_disabled()`、`writes_searches_and_deletes_probe_when_enabled()`、`fails_startup_when_probe_search_fails()`。
- Repository evidence: `FakeEmbeddingModel`、`FakeVectorStore` 的调用记录与失败点。
- Dependencies and consumers: 消费 `RagVectorStoreProbe` 与装配。
- Why now: `REQ-018` 的唯一可执行证据。
- Contract/signature changes: 引入探针类型。
- Input/output and state mapping: 开关 + 注册表 → 每个逻辑模型的往返；断言 `FakeEmbeddingModel` 的调用次数与 `FakeVectorStore` 的写入记录。
- Error and edge behavior: 关闭时嵌入调用次数为 0；开启且检索失败时抛 `RagConfigurationException` 并指明模型名与阶段。
- Standards impact: `MC-SCOPE-001`、`MC-TEST-001`。
- Literal rule enforcement: `Rule 11`。
- Implementation pseudocode:

```java
class RagVectorStoreProbeTest {
    @Test void produces_no_model_call_when_disabled() {
        runProbe(probeOnStartup = false);
        assertThat(model.invocationCount()).isZero();
        assertThat(vectorStore.addedDocuments()).isEmpty();
    }

    @Test void fails_startup_when_probe_search_fails() {
        vectorStore.failOn(SEARCH);
        assertThatThrownBy(() -> runProbe(probeOnStartup = true))
            .isInstanceOf(RagConfigurationException.class)
            .hasMessageContaining("openai-small").hasMessageContaining("search");
    }
}
```

- Verification contribution: `TEST-018` 的 RED 契约。
- After this file: 编译失败。

#### File 2 — `CREATE src/main/java/top/egon/cola/component/rag/execution/RagVectorStoreProbe.java`

- Purpose: 可选的一次性往返校验，验证表存在、维度正确、扩展可用与过滤/删除可用。
- Symbols: `@Component("ragVectorStoreProbe")`；方法 `void validate(RagEmbeddingModelRegistry registry)`；保留常量 `PROBE_COLLECTION = "__egon_rag_probe__"`。
- Repository evidence: 无同类；`AgentFlowConfigurationValidator` 的启动期 fail-closed 先例。
- Dependencies and consumers: 被 `RagAutoConfiguration` 在 `probe-on-startup=true` 时调用。
- Why now: 模型注册表与向量库 Bean 都已就绪。
- Contract/signature changes: 新增启动期校验类。
- Input/output and state mapping: 注册表 → 逐模型（写入一条保留标识的探针向量 → 用同集合过滤检索 → 按标识删除）。
- Error and edge behavior: 任一阶段失败抛 `RagConfigurationException`（消息含逻辑模型名与阶段名 `add`/`search`/`delete`）；失败时仍尝试删除探针记录。
- Standards impact: `MC-BEAN-001`、`MC-LOG-001`（只记模型名、阶段、耗时与结果，**不记录向量**）、`MC-TIME-001`、`MC-PATTERN-001`、`MC-SCOPE-001`、`MC-TEST-001`。
- Literal rule enforcement: `Rule 1`、`Rule 4`、`Rule 10`、`Rule 11`。
- Implementation pseudocode:

```java
@Slf4j
@Component("ragVectorStoreProbe")
@RequiredArgsConstructor
public class RagVectorStoreProbe {

    public static final String PROBE_COLLECTION = "__egon_rag_probe__";

    private final @Qualifier("ragVectorStore") VectorStore vectorStore;
    private final @Qualifier("ragClock") Clock clock;

    public void validate(RagEmbeddingModelRegistry registry) {
        for (String logicalName : registry.listLogicalNames()) {
            Instant startedAt = clock.instant();
            String probeId = PROBE_COLLECTION + ":" + logicalName;
            try {
                vectorStore.add(List.of(new Document(probeId, PROBE_COLLECTION,
                        RagMetadataKeys.toDocumentMetadata(PROBE_COLLECTION, PROBE_COLLECTION, logicalName, 0, Map.of()))));
                vectorStore.similaritySearch(SearchRequest.builder().query(PROBE_COLLECTION).topK(1)
                        .filterExpression(FilterExpressionBuilder.and(
                                FilterExpressionBuilder.eq(RagMetadataKeys.COLLECTION_ID, PROBE_COLLECTION),
                                FilterExpressionBuilder.eq(RagMetadataKeys.LOGICAL_MODEL_NAME, logicalName))).build());
                log.info("rag vector store probe finished: model={}, result=SUCCESS, durationMs={}",
                         logicalName, Duration.between(startedAt, clock.instant()).toMillis());
            } catch (RuntimeException exception) {
                throw new RagConfigurationException("rag vector store probe failed for model '" + logicalName
                        + "' at stage " + currentStage(), exception);
            } finally {
                deleteQuietly(probeId);
            }
        }
    }
}
```

- Verification contribution: `TEST-018`。
- After this file: 探针就绪。

#### File 3 — `MODIFY src/main/java/top/egon/cola/component/rag/autoconfigure/RagAutoConfiguration.java`

- Purpose: 仅在开关开启时调用探针。
- Symbols: `@Bean(name="ragProbeRunner")` 或 `@PostConstruct` 钩子；条件 `probeOnStartup`。
- Repository evidence: `AgentFlowAutoConfiguration` 的条件装配风格。
- Dependencies and consumers: 消费 `ragVectorStoreProbe` 与 `ragEmbeddingModelRegistry`。
- Why now: 探针类已存在。
- Contract/signature changes: 新增一条条件装配。
- Input/output and state mapping: `ragProperties.validation().probeOnStartup()` → 是否调用 `validate(...)`。
- Error and edge behavior: 关闭时不得触碰探针 Bean；开启时异常向上传播导致上下文启动失败。
- Standards impact: `MC-BEAN-001`、`MC-CONFIG-001`、`MC-SCOPE-001`、`MC-TEST-001`。
- Literal rule enforcement: `Rule 4`、`Rule 11`。
- Implementation pseudocode:

```java
@Bean(name = "ragProbeRunner")
@ConditionalOnMissingBean(name = "ragProbeRunner")
public Object ragProbeRunner(@Qualifier("ragProperties") RagProperties properties,
                             @Qualifier("ragVectorStoreProbe") RagVectorStoreProbe probe,
                             @Qualifier("ragEmbeddingModelRegistry") RagEmbeddingModelRegistry registry) {
    if (properties.validation().probeOnStartup()) {
        probe.validate(registry);
    }
    return new Object();
}
```

- Verification contribution: `TEST-018`。
- After this file: Step 10 完成。

- Validation working directory: `/Users/mario/SelfProject/Egon-COLA/egon-cola-components/egon-cola-component-rag-starter`
- Verification command: `../../mvnw -B -ntp -pl . test -Dtest='RagVectorStoreProbeTest'`
- Expected result: 三个用例通过；关闭用例断言嵌入调用次数为 0。
- Failure returns to: File 2（往返顺序或清理）、File 3（条件判断）。
- Completion criteria: `REQ-018` 有可执行证据；默认配置下无任何模型调用。
- Rollback: 回退本 Step 三个文件。
- Commit paths: `src/test/java/top/egon/cola/component/rag/execution/RagVectorStoreProbeTest.java`; `src/main/java/top/egon/cola/component/rag/execution/RagVectorStoreProbe.java`; `src/main/java/top/egon/cola/component/rag/autoconfigure/RagAutoConfiguration.java`
- Commit: `feat(rag): add opt-in vector store probe`

### Step 11 — 契约门禁与可观测性

- Requirements: `REQ-001`, `REQ-014`, `REQ-015`, `REQ-017`
- Dependencies: Step 4-10
- Baseline state: 全部生产类型已存在；不存在静态门禁，也未注册指标。
- Observable outcome: 静态契约测试断言包结构、被禁包与被禁依赖、类型命名后缀、`java.time` 用法与日志字段；指标在 Micrometer 存在时注册、缺席时完全跳过；日志捕获断言不含文档内容。
- End state: `RagComponentContractTest`、`RagObservabilityTest`、`RagDeliveryBoundaryTest` 就绪；指标埋点接入摄取与检索。
- Test-first gate: `Required` — 契约测试首次运行时编译失败（测试类不存在）；它断言的是已存在的生产代码，因此 RED 表现是"断言失败"而非"编译失败"。
- Manual Checks: `MC-ARCH-001`, `MC-REUSE-001`, `MC-DEP-001`, `MC-NAME-001`, `MC-LOG-001`, `MC-UTIL-001`, `MC-TIME-001`, `MC-SCOPE-001`, `MC-TEST-001`
- Literal Rules: `Rule 1`, `Rule 5`, `Rule 10`, `Rule 11`
- Ordered files:

#### File 1 — `CREATE src/test/java/top/egon/cola/component/rag/contract/RagComponentContractTest.java`

- Purpose: 把 Spec A 的静态约束变成可执行门禁。
- Symbols: `has_no_forbidden_packages()`、`has_no_forbidden_dependencies()`、`keeps_semantic_type_suffixes()`、`uses_java_time_only()`、`has_lombok_config_with_qualifier()`、`never_calls_baseconverter_date_methods()`。
- Repository evidence: `agent-flow-starter/src/test/java/.../contract/AgentFlowComponentContractTest.java` 的静态扫描方式（文件遍历 + 子串匹配，非 ArchUnit）；`AgentSourceContractTest` 的正则风格。
- Dependencies and consumers: 扫描 `src/main/java` 与 `pom.xml`；不依赖运行期。
- Why now: 全部生产文件已就位，门禁才有对象。
- Contract/signature changes: 无生产改动。
- Input/output and state mapping: 源文件树 → 断言结果。
- Error and edge behavior: 任一违规给出包含违规路径与违规 token 的消息。
- Standards impact: `MC-ARCH-001`（包结构）、`MC-DEP-001`（依赖）、`MC-NAME-001`（命名）、`MC-UTIL-001`（import）、`MC-TIME-001`（时间类型）、`MC-BEAN-001`（`lombok.config`）、`MC-LOG-001`（日志字段扫描）、`MC-SCOPE-001`、`MC-TEST-001`。
- Literal rule enforcement: `Rule 1`、`Rule 5`、`Rule 10`、`Rule 11`。
- Implementation pseudocode:

```java
class RagComponentContractTest {
    @Test void has_no_forbidden_packages() {
        assertThat(packageNames()).noneMatch(name -> name.startsWith("top.egon.cola.component.rag.biz"))
                                  .noneMatch(name -> name.endsWith(".domain"))
                                  .noneMatch(name -> name.endsWith(".infrastructure"))
                                  .noneMatch(name -> name.endsWith(".adapter"))
                                  .noneMatch(name -> name.endsWith(".repository"));
    }

    @Test void has_no_forbidden_dependencies() {
        String pom = Files.readString(Path.of("pom.xml"));
        for (String forbidden : List.of("flyway-core", "mybatis", "spring-boot-starter-data-redis",
                                        "spring-boot-starter-amqp", "spring-boot-starter-jdbc", "spring-boot-starter-web")) {
            assertThat(pom).doesNotContain(forbidden);
        }
    }

    @Test void uses_java_time_only() {
        String sources = readMainSources();
        for (String forbidden : List.of("java.util.Date", "java.util.Calendar", "SimpleDateFormat")) {
            assertThat(sources).doesNotContain(forbidden);
        }
        assertThat(sources).doesNotContain(".map(java.util.Date");   // BaseConverter 遗留方法零调用
    }

    @Test void has_lombok_config_with_qualifier() {
        assertThat(Files.readString(Path.of("lombok.config")))
            .contains("lombok.copyableAnnotations += org.springframework.beans.factory.annotation.Qualifier");
    }
}
```

- Verification contribution: `TEST-021`、`TEST-022`（与 File 2 配合）。
- After this file: 门禁可执行；若实现偏离 Spec，此处失败。

#### File 2 — `CREATE src/test/java/top/egon/cola/component/rag/autoconfigure/RagObservabilityTest.java`

- Purpose: 断言指标在 Micrometer 存在时注册、缺席时跳过，且日志不含内容。
- Symbols: `registers_metrics_when_meter_registry_present()`、`starts_without_micrometer()`、`never_logs_document_content()`。
- Repository evidence: `spring-boot-starter-test` 提供的 `SimpleMeterRegistry`；`FilteredClassLoader` 排除 `io.micrometer.core`。
- Dependencies and consumers: 消费摄取与检索服务的指标埋点。
- Why now: 与 File 1 同批，共同构成 Step 11 的证据。
- Contract/signature changes: 本 Step 需在 `RagIngestionServiceImpl` 与 `RagRetrievalServiceImpl` 增加可选指标埋点（`ObjectProvider<MeterRegistry>`，缺席时跳过）。
- Input/output and state mapping: MeterRegistry 存在 → 注册 `rag.ingest`/`rag.retrieve` 计数器与计时器；缺席 → 不注册且不报错。
- Error and edge behavior: 指标标签只允许结果、逻辑模型名与策略枚举，**不得含 `collectionId`/`documentId`**；日志捕获断言不含分块文本。
- Standards impact: `MC-LOG-001`、`MC-SCOPE-001`、`MC-TEST-001`。
- Literal rule enforcement: `Rule 11`。
- Implementation pseudocode:

```java
class RagObservabilityTest {
    @Test void registers_metrics_when_meter_registry_present() {
        runner.withBean(MeterRegistry.class, SimpleMeterRegistry::new).run(context ->
            assertThat(context.getBean(SimpleMeterRegistry.class).find("rag.ingest").timers()).isNotEmpty());
    }

    @Test void starts_without_micrometer() {
        new ApplicationContextRunner().withClassLoader(new FilteredClassLoader("io.micrometer.core"))
            .withConfiguration(/* ... */).run(context -> assertThat(context).hasNotFailed());
    }

    @Test void never_logs_document_content() {
        ListAppender<ILoggingEvent> appender = attachTo(RagIngestionServiceImpl.class);
        service.ingest(commandWithContent("SECRET-CONTENT-MARKER"));
        assertThat(appender.list()).extracting(ILoggingEvent::getFormattedMessage)
            .noneMatch(message -> message.contains("SECRET-CONTENT-MARKER"));
    }
}
```

- Verification contribution: `TEST-023`、`TEST-022` 的日志部分。
- After this file: 可观测性门禁可执行。

#### File 3 — `MODIFY src/main/java/top/egon/cola/component/rag/execution/{RagIngestionServiceImpl,RagRetrievalServiceImpl}.java`

- Purpose: 接入可选指标，且不改变既有行为。
- Symbols: 构造函数新增 `ObjectProvider<MeterRegistry>`；方法内 `meterRegistry.ifAvailable(registry -> ...)` 记录计数器与计时器。
- Repository evidence: `transactional-outbox-starter` 的 `MicrometerOutboxMetrics` 与 `NoopOutboxMetrics` 双实现（Micrometer 缺席时用空实现）。
- Dependencies and consumers: 被 `RagObservabilityTest` 断言。
- Why now: 测试已定义期望，实现按最小改动满足。
- Contract/signature changes: 构造签名增加一个参数（组件内部类，非公开契约）。
- Input/output and state mapping: `collectionId`/`documentId` **不作为标签**；标签为 `result`、`model`、`strategy`。
- Error and edge behavior: MeterRegistry 缺席时不注册任何指标且不抛错。
- Standards impact: `MC-LOG-001`、`MC-BEAN-001`（`ObjectProvider` 注入仍需 `@Qualifier`？——`ObjectProvider` 为框架类型，按 `§6.2` 规则 4 逐依赖 `@Qualifier` 的要求，此处使用 `@Qualifier("meterRegistryProvider")` 且由装配提供该 Bean）、`MC-SCOPE-001`、`MC-TEST-001`。
- Literal rule enforcement: `Rule 4`、`Rule 11`。
- Implementation pseudocode:

```java
// 构造器
private final @Qualifier("ragMeterRegistryProvider") ObjectProvider<MeterRegistry> meterRegistryProvider;

// 内部
meterRegistryProvider.ifAvailable(registry -> {
    Counter.builder("rag.ingest").tag("result", "SUCCESS").tag("model", descriptor.logicalName())
           .tag("strategy", command.chunkingConfig().strategy().name()).register(registry).increment();
    Timer.builder("rag.ingest.duration").tag("model", descriptor.logicalName()).register(registry)
         .record(elapsed);
});
```

- Verification contribution: `TEST-023`。
- After this file: 指标可观测性就绪。

#### File 4 — `CREATE src/test/java/top/egon/cola/component/rag/autoconfigure/RagDeliveryBoundaryTest.java`

- Purpose: 断言组件的交付面：全部 Bean 名存在、`AutoConfiguration.imports` 指向正确类、默认关闭时零 Bean。
- Symbols: `exposes_documented_bean_names()`、`imports_file_points_to_auto_configuration()`。
- Repository evidence: `AgentFlowComponentContractTest` 的交付面断言。
- Dependencies and consumers: 消费装配与资源文件。
- Why now: 与 File 1/2 同批，使 Step 11 的覆盖面完整。
- Contract/signature changes: 无。
- Input/output and state mapping: 启用配置 → 容器 Bean 名集合；资源文件 → 内容断言。
- Error and edge behavior: Bean 名与 Spec A `§9.2.6` 表格逐条一致。
- Standards impact: `MC-BEAN-001`、`MC-CONFIG-001`、`MC-SCOPE-001`、`MC-TEST-001`。
- Literal rule enforcement: `Rule 4`、`Rule 11`。
- Implementation pseudocode:

```java
@Test void exposes_documented_bean_names() {
    runner.withPropertyValues(enabledKeySet()).withUserConfiguration(StubHostBeans.class).run(context ->
        assertThat(context).hasBean("ragProperties").hasBean("ragClock").hasBean("ragEmbeddingModelRegistry")
            .hasBean("ragDocumentExtractorRegistry").hasBean("ragChunkingStrategyFactory").hasBean("ragChunkIdFactory")
            .hasBean("ragDocumentStorage").hasBean("ragExtractionService").hasBean("ragIngestionService")
            .hasBean("ragRetrievalService"));
}
```

- Verification contribution: Step 11 的交付面证据。
- After this file: Step 11 完成（含 `contract` 包的 `package-info.java`）。

- Validation working directory: `/Users/mario/SelfProject/Egon-COLA/egon-cola-components/egon-cola-component-rag-starter`
- Verification command: `../../mvnw -B -ntp -pl . clean verify`
- Expected result: 全部测试通过；契约测试的六个断言与可观测性测试的三个断言均为 GREEN；`mvn verify` 退出码 0。
- Failure returns to: File 1（实现偏离 Spec 的包结构/命名/依赖）、File 2/3（指标或不泄露断言）、File 4（Bean 名不一致）。
- Completion criteria: `REQ-001`、`REQ-014`、`REQ-015`、`REQ-017` 各有可执行证据。
- Rollback: 回退本 Step 全部文件（含两处指标埋点）。
- Commit paths: `src/test/java/top/egon/cola/component/rag/contract/RagComponentContractTest.java`; `src/test/java/top/egon/cola/component/rag/autoconfigure/RagObservabilityTest.java`; `src/main/java/top/egon/cola/component/rag/execution/{RagIngestionServiceImpl,RagRetrievalServiceImpl}.java`; `src/test/java/top/egon/cola/component/rag/autoconfigure/RagDeliveryBoundaryTest.java`
- Commit: `test(rag): gate component contracts and observability`

### Step 12 — 文档与交付面回归

- Requirements: `REQ-016`, `REQ-018`, `REQ-017`
- Dependencies: Step 1-11
- Baseline state: 模块行为完整并通过 `clean verify`；不存在使用文档，组件架构文档未登记新组件。
- Observable outcome: 使用方能按 README 完成引入、配置与 Bean 提供；组件架构文档列出新组件与其扩展点；`TEST-024` 证明既有 10 个模块的构建结果与 BOM 导出面不变。
- End state: 两份 README、架构文档条目与 `RagDeliverySurfaceTest` 就绪。
- Test-first gate: `Not applicable` — 本 Step 只写文档与执行回归，不引入行为；其证明是回归测试与文档评审。
- Manual Checks: `MC-REUSE-001`, `MC-DEP-001`, `MC-SCOPE-001`, `MC-TEST-001`
- Literal Rules: `Rule 11`
- Ordered files:

#### File 1 — `CREATE egon-cola-component-rag-starter/README.md`

- Purpose: 交付使用说明，**特别是探针默认关闭的网络与计费含义**。
- Symbols: 章节「简介」「版本矩阵」「依赖方式」「宿主提供 EmbeddingModel 与 VectorStore」「配置」「Java API」「扩展点」「执行与失败语义」「边界与日志」「升级与验证门禁」。
- Repository evidence: `egon-cola-component-agent-flow-starter/README.md` 与 `README.zh-CN.md` 的章节结构（简介/版本矩阵/依赖方式/宿主提供 ChatModel/配置/Java API/执行与生命周期语义/边界与日志/升级与验证门禁）。
- Dependencies and consumers: 使用方（Spec B）按此装配。
- Why now: 全部行为已定型。
- Contract/signature changes: 无。
- Input/output and state mapping: 文档中逐项列出 `§9.2.6` 的十个配置键与默认值、`§9.1` 的三个服务入口与四个 SPI。
- Error and edge behavior: 必须显式写明三点：① 组件不建表、需要宿主先完成向量表迁移；② `rag.dimensions` 必须与向量表维度一致，探针关闭时启动期不会发现不一致；③ 探针默认关闭，开启后每次启动会产生一次真实模型调用并计费。
- Standards impact: `MC-SCOPE-001`、`MC-TEST-001`（文档评审作为该 Step 的验证）。
- Literal rule enforcement: `Rule 11`（文档置于模块内，不新增工程）。
- Implementation pseudocode:

```text
# Egon-COLA RAG Starter
## 简介            — 引擎机制范围；明确不含 CRUD/HTTP/schema/调度
## 版本矩阵          — Java 21 / Boot 3.5.16 / Spring AI 1.1.8
## 依赖方式          — BOM 引入，无版本
## 宿主提供 Bean      — 具名 EmbeddingModel 与 VectorStore；组件不持有地址与密钥
## 配置             — 十个键、默认值、范围与跨字段规则
## Java API        — RagExtractionService / RagIngestionService / RagRetrievalService 的签名与幂等语义
## 扩展点            — 四个 SPI 的契约与注册方式
## 执行与失败语义      — 不重试、不落库、先删后写；重跑会重新计费嵌入
## 边界与日志         — 不记录内容与向量；指标标签集合
## 升级与验证门禁      — 枚举只增不改；SPI 只增默认方法；探针默认关闭的计费说明
```

- Verification contribution: 文档评审。
- After this file: 英文使用文档就绪。

#### File 2 — `CREATE egon-cola-component-rag-starter/README.zh-CN.md`

- Purpose: 中文使用文档，与 File 1 内容等价。
- Symbols: 同 File 1 的章节。
- Repository evidence: 仓库每个组件均提供两份 README（`agent-flow-starter`、`transactional-outbox-starter`）。
- Dependencies and consumers: 同 File 1。
- Why now: 同 File 1。
- Contract/signature changes: 无。
- Input/output and state mapping: 逐键与逐接口同 File 1。
- Error and edge behavior: 同 File 1 的三点必写内容；两份 README 的键表与 API 列表必须逐项一致。
- Standards impact: `MC-SCOPE-001`、`MC-TEST-001`。
- Literal rule enforcement: `Rule 11`。
- Implementation pseudocode:

```text
# Egon-COLA RAG Starter（中文）

## 简介
  引擎机制范围：抽取、切块嵌入、检索、四类扩展点；明确不含 CRUD、HTTP、schema 与调度。
## 版本矩阵
  Java 21 / Spring Boot 3.5.16 / Spring AI 1.1.8；列出与 English 版完全相同的版本行。
## 依赖方式
  经 egon-cola-components-bom 无版本引入。
## 宿主提供 EmbeddingModel 与 VectorStore
  给出具名 Bean 的示例与 Bean 名配置键；说明组件不持有地址与密钥。
## 配置
  逐键列出 egon.cola.component.rag.* 十个键、默认值、取值范围与跨字段规则。
## Java API
  三个服务入口的签名与语义；重点写明 ingest 只接受已抽取文档、extract 不落库。
## 扩展点
  四个 SPI 的契约、注册方式与失败语义。
## 执行与失败语义
  不重试、不落库、先删后写；重跑会重新计费嵌入。
## 边界与日志
  不记录内容与向量；指标标签只含结果、逻辑名与策略。
## 限制
  ① 组件不建表，宿主须先完成向量表迁移；② dimensions 必须与向量表维度一致，
  探针关闭时启动期不会发现不一致；③ 探针默认关闭，开启后每次启动会产生一次真实模型调用并计费。
```

- Verification contribution: 文档评审；两份 README 的键表逐项比对。
- After this file: 中文使用文档就绪。

#### File 3 — `MODIFY egon-cola-components/egon-cola-components-architecture.md`

- Purpose: 在组件清单、starter 定位与 BOM 规范章节登记新组件。
- Symbols: `§3.1` 组件清单新增一行；`§5` 或新增小节描述能力与扩展点；`§11` BOM 说明提及新导出。
- Repository evidence: 文档现有章节结构；`§8.1` 已有「Agent Flow 扁平 starter 例外」小节，RAG 组件与之同形。
- Dependencies and consumers: 组件维护者与使用方。
- Why now: 组件已实现并验证。
- Contract/signature changes: 无。
- Input/output and state mapping: 文档条目与 `§5` 的实现清单一致。
- Error and edge behavior: 不得把 RAG 组件描述成带管理能力或数据库依赖——须与其实际边界一致。
- Standards impact: `MC-SCOPE-001`、`MC-TEST-001`。
- Literal rule enforcement: `Rule 11`。
- Implementation pseudocode:

```text
§3.1 组件清单：追加 | egon-cola-component-rag-starter | RAG 引擎机制 | 无 DB / 无 HTTP / 无调度 |
§5 或 §8.2：描述四个扩展点、三个服务入口、宿主提供 Bean 的边界，并引用 §8.1 的扁平例外先例
§11 BOM：确认新组件只导出 Starter 坐标，不导出 Spring AI / Tika
```

- Verification contribution: 文档评审。
- After this file: 组件架构文档与新组件一致。

#### File 4 — `CREATE src/test/java/top/egon/cola/component/rag/reactor/RagDeliverySurfaceTest.java`

- Purpose: 证明新增模块没有改变既有模块的构建结果与 BOM 导出面。
- Symbols: `parent_pom_lists_exactly_one_new_module()`、`bom_exports_exactly_one_new_dependency()`、`existing_module_artifacts_unchanged()`。
- Repository evidence: 父 POM 与 BOM 在 Step 1 的最终状态；`scripts/test-spring-dependency-management.sh` 的静态断言风格可作参考。
- Dependencies and consumers: 读取 `egon-cola-components/pom.xml` 与 `egon-cola-components-bom/pom.xml`。
- Why now: 全部模块文件已就位，回归才有意义。
- Contract/signature changes: 无生产改动。
- Input/output and state mapping: 两个 POM 文件 → 断言模块数与导出数各增加一，且既有条目逐条不变。
- Error and edge behavior: 任一条既有条目被改动即失败并给出条目名。
- Standards impact: `MC-SCOPE-001`、`MC-TEST-001`。
- Literal rule enforcement: `Rule 11`。
- Implementation pseudocode:

```java
@Test void parent_pom_lists_exactly_one_new_module() {
    List<String> modules = modulesOf(componentsParentPom());
    assertThat(modules).hasSize(11).contains("egon-cola-component-rag-starter");
    assertThat(modules).containsAll(EXPECTED_EXISTING_TEN_MODULES);
}

@Test void bom_exports_exactly_one_new_dependency() {
    assertThat(dependencyIdsOf(bom())).hasSize(21).contains("egon-cola-component-rag-starter");
}
```

- Verification contribution: `TEST-024`。
- After this file: Step 12 完成。

#### File 5 — `MODIFY docs/egon/plan/2026-09-10-12-47-egon-cola-component-rag-starter-implementation.md`

- Purpose: 在实施完成后回填 `Status` 与实际证据。
- Symbols: 头部 `Updated`；`§12.6` 的最终结论。
- Repository evidence: 仓库既有 Plan 的收尾方式。
- Dependencies and consumers: 发布流程。
- Why now: 实施完成后才有可回填的证据。
- Contract/signature changes: 无。
- Input/output and state mapping: 实际命令输出 → Plan 的证据列。
- Error and edge behavior: 不得在未执行的情况下把结论改为 PASS。
- Standards impact: `MC-SCOPE-001`、`MC-TEST-001`。
- Literal rule enforcement: `Rule 11`。
- Implementation pseudocode:

```text
1. 读取本 Plan 的头部，把 Status 从 Review 改为 Implemented（仅当全部 Step 已提交并通过验证）
2. 更新 Updated 为实施完成时的本地时间
3. 在 §7 每个 Step 的「Completion criteria」后追加「实际结果」行，内容为该 Step 的
   验证命令与观察到的退出码/测试计数，逐字引用命令输出摘要而非转述
4. 在 §12.6 记录实际执行结果；若任何 Step 的验证未执行或未通过，保持非 PASS 结论并注明原因
5. 不得在未执行的情况下把结论改为 PASS；不得声称运行期验证
6. 若实施过程中偏离了本 Plan 的任何文件或顺序，在 §11 追加一行记录偏离与理由
```

- Verification contribution: 计划闭环。
- After this file: 交付完成。

- Validation working directory: `/Users/mario/SelfProject/Egon-COLA`
- Verification command: 先 `./mvnw -B -ntp -pl egon-cola-components/egon-cola-component-rag-starter -am clean verify`，再 `./mvnw -B -ntp -f egon-cola-components/pom.xml clean install -DskipTests`，最后 `./mvnw -B -ntp -pl egon-cola-components/egon-cola-component-rag-starter test -Dtest='RagDeliverySurfaceTest'`
- Expected result: 三条命令均退出码 0；模块测试全绿；交付面回归断言模块数与导出数各新增一条且既有条目不变。
- Failure returns to: File 4（回归断言）、Step 1（POM 登记）。
- Completion criteria: `REQ-016` 有可执行证据；两份 README 与架构文档与实现一致。
- Rollback: 回退文档与回归测试文件；不影响生产代码。
- Commit paths: `egon-cola-component-rag-starter/README.md`; `egon-cola-component-rag-starter/README.zh-CN.md`; `egon-cola-components/egon-cola-components-architecture.md`; `src/test/java/top/egon/cola/component/rag/reactor/RagDeliverySurfaceTest.java`; `docs/egon/plan/2026-09-10-12-47-egon-cola-component-rag-starter-implementation.md`
- Commit: `docs(rag): document the rag starter and guard the delivery surface`

## 8. Test, Validation, and Quality Gates

| Gate/order | Working directory | Command or method | Scope | Expected result | Failure returns to | Requirements/runtime boundary |
| --- | --- | --- | --- | --- | --- | --- |
| 构建宿主 | `/Users/mario/SelfProject/Egon-COLA` | `./mvnw -B -ntp -pl egon-cola-components -am clean install -DskipTests` | components Reactor | 退出码 0；模块清单含 11 项 | Step 1 | `REQ-016`；静态 |
| RED for Step 2 | 模块根 | `../../mvnw -B -ntp -pl . test -Dtest='RagAutoConfigurationTest,RagPropertiesBindingTest'` | 聚焦测试 | 编译失败：`RagAutoConfiguration`/`RagProperties` 不存在 | File 1/3 | `REQ-002`；模块 |
| GREEN for Step 2 | 模块根 | 同上 | 聚焦测试 | 退出码 0；五个默认关闭/约束用例与三个绑定用例通过 | File 6/7 | `REQ-002`；模块 |
| GREEN for Step 3 | 模块根 | `... -Dtest='RagAutoConfigurationTest'` | 聚焦测试 | Bean 名解析、重复 Bean 与维度不一致三个失败用例通过 | File 4/5 | `REQ-003`, `REQ-006`, `REQ-008`；模块 |
| GREEN for Step 4 | 模块根 | `... -Dtest='RagDocumentExtractorRegistryTest,RagExtractionServiceImplTest'` | 聚焦测试 | 路由、歧义、缺失与异常透传用例通过 | File 5/10 | `REQ-004`, `REQ-019`；模块 |
| GREEN for Step 5 | 模块根 | `... -Dtest='RagOptionalExtractorTest'` | 聚焦测试 | 可选依赖缺席时仍可启动 | File 2/3 | `REQ-013`；模块 |
| GREEN for Step 6 | 模块根 | `... -Dtest='RagChunkingStrategyFactoryTest,RagChunkingStrategyTest,RagChunkIdFactoryTest'` | 聚焦测试 | 三策略确定性用例通过 | File 8-12 | `REQ-005`, `REQ-009`；模块 |
| GREEN for Step 7 | 模块根 | `... -Dtest='RagChunkConverterTest,RagIngestionServiceImplTest,TwoPhaseIngestionTest'` | 聚焦测试 | 先删后写、失败不写入与只解析一次用例通过 | File 5/9/10 | `REQ-009`-`REQ-011`, `REQ-019`, `REQ-020`；模块 |
| GREEN for Step 8 | 模块根 | `... -Dtest='RagRetrievalServiceImplTest'` | 聚焦测试 | 强制过滤与排序用例通过 | File 5 | `REQ-007`；模块 |
| GREEN for Step 9 | 模块根 | `... -Dtest='LocalFileSystemRagDocumentStorageTest'` | 聚焦测试 | 往返、穿越拒绝与覆盖用例通过 | File 6/7 | `REQ-012`；模块 |
| GREEN for Step 10 | 模块根 | `... -Dtest='RagVectorStoreProbeTest'` | 聚焦测试 | 默认零调用与开启后往返用例通过 | File 2/3 | `REQ-018`；模块 |
| 模块全量 | 模块根 | `../../mvnw -B -ntp -pl . clean verify` | 模块 | 退出码 0；25 个测试全绿；无告警 | Step 11 | 全部 `REQ-*`；模块 |
| 交付面回归 | 模块根 | `... -Dtest='RagDeliverySurfaceTest'` | 回归 | 模块数与导出数各新增一，既有条目不变 | Step 12 | `REQ-016`；静态 |
| 跨模块回归 | 仓库根 | `./mvnw -B -ntp -f egon-cola-components/pom.xml clean install -DskipTests` | components Reactor | 退出码 0；既有 10 个模块构建结果不变 | Step 12 | `REQ-016`；跨模块 |
| 静态格式 | 仓库根 | `./mvnw -B -ntp -pl egon-cola-components/egon-cola-component-rag-starter checkstyle:check`（若仓库配置了该插件；否则以 `clean verify` 的编译与契约测试代替） | 模块 | 退出码 0 或无该插件 | 各 Step | 静态 |
| 运行期/人工 | 用户控制的环境 | 需宿主提供 `EmbeddingModel` 与 `VectorStore` 后启动；开启探针观察一次往返 | 集成 | 观察结果由用户记录 | Step 12 | `REQ-018`；运行期（**本 Plan 不声称已执行**） |

## 9. Migration, Compatibility, Rollout, and Rollback

`N/A` — 本 Plan 不涉及数据库迁移、数据回填、契约发布或前端兼容。

| Concern | Disposition | Spec section and repository reason |
| --- | --- | --- |
| 数据库迁移 | `N/A` | Spec A `§11` 判定为 `N/A`；组件无 datasource、无 schema、无迁移 |
| 数据回填与双读双写 | `N/A` | 无持久化数据 |
| API/事件兼容 | `N/A` | 组件无 HTTP/RPC/事件契约（Spec A `§9.0`） |
| 配置兼容 | 适用 | 新增模块，无既有配置；`§9.2.6` 的十个键为组件初版键集 |
| 依赖兼容 | 适用 | 父 POM 新增两条 `dependencyManagement` 与一条模块登记；BOM 新增一条导出。既有模块的解析结果不变，由 `TEST-024` 守护 |
| 部署顺序 | 适用 | 组件默认关闭，可先发布再在消费方启用；Step 1 之前无需任何部署动作 |
| 回滚 | 适用 | 逐 Step 回退对应提交；Step 1 的回退同时移除父 POM 与 BOM 的登记。无数据残留，因为组件不落任何持久状态 |
| 前向修复 | 适用 | 组件不可回退时（例如已被消费方依赖），修复方式为追加新版本而不是修改已发布坐标 |

## 10. Requirement-to-Step Traceability Matrix

| Requirement | Effective Spec section | Steps | Files | Tests/gates | Completion evidence |
| --- | --- | --- | --- | --- | --- |
| `REQ-001` | Spec A `§4`, `§8.2` | Step 1, Step 11 | 模块骨架；`contract/RagComponentContractTest.java` | `TEST-021` | 包结构断言通过且构建成功 |
| `REQ-002` | Spec A `§4`, `§9.2.6` | Step 2 | `RagAutoConfiguration.java`、`RagProperties.java`、`AutoConfiguration.imports` | `TEST-011`, `TEST-012`, `TEST-013` | 默认关闭与严格绑定用例通过 |
| `REQ-003` | Spec A `§4`, `§9.2.6` | Step 3 | `RagAutoConfiguration.java`、`RagEmbeddingModelRegistry.java` | `TEST-014` | Bean 名不可解析时启动失败 |
| `REQ-004` | Spec A `§4`, `§9.2.3`, `§9.2.7` | Step 4 | `extract/*`、`execution/RagExtractionServiceImpl.java` | `TEST-001`, `TEST-002`, `TEST-004` | 路由、歧义与缺失用例通过 |
| `REQ-005` | Spec A `§4`, `§9.2.4` | Step 6 | `chunk/*` | `TEST-003`, `TEST-005` | 三策略与未知枚举用例通过 |
| `REQ-006` | Spec A `§4`, `§9.2.6` | Step 3 | `embed/RagEmbeddingModelRegistry.java` | `TEST-014` | 默认回退与未注册失败用例通过 |
| `REQ-007` | Spec A `§4`, `§9.2.2` | Step 8 | `execution/RagRetrievalServiceImpl.java` | `TEST-016`, `TEST-017` | 过滤表达式断言通过 |
| `REQ-008` | Spec A `§4`, `§9.2.6` | Step 3 | `embed/*` | `TEST-015` | 维度不一致时启动失败 |
| `REQ-009` | Spec A `§4`, `§9.2.1` | Step 6, Step 7 | `chunk/RagChunkIdFactory.java`、`execution/RagIngestionServiceImpl.java` | `TEST-009`, `TEST-025` | 两次切分与两次摄取 id 集合一致 |
| `REQ-010` | Spec A `§4`, `§9.2.1` | Step 7 | `execution/RagIngestionServiceImpl.java` | `TEST-010` | 删除失败时未写入 |
| `REQ-011` | Spec A `§4`, `§10.4` | Step 7 | `metadata/RagMetadataKeys.java` | `TEST-006`, `TEST-008` | 保留 key 被拒且合并结果无重复 |
| `REQ-012` | Spec A `§4`, `§9.2.5` | Step 9 | `storage/*` | `TEST-020` | 往返、穿越拒绝与覆盖用例通过 |
| `REQ-013` | Spec A `§4`, `§9.2.3` | Step 5 | `extract/{Pdf,Tika}RagDocumentExtractor.java` | `TEST-019` | 可选依赖缺席时可启动且报可诊断错误 |
| `REQ-014` | Spec A `§4`, `§7.0` | Step 1, Step 11 | 模块 `pom.xml`；`contract/RagComponentContractTest.java` | `TEST-021` | 依赖扫描无被禁坐标 |
| `REQ-015` | Spec A `§4`, `§7.3.5` | Step 4-11 | 全部实现类；`RagObservabilityTest.java` | `TEST-022` | 日志捕获无内容泄露 |
| `REQ-016` | Spec A `§4`, `§8.3` | Step 1, Step 12 | 父 POM、BOM、`RagDeliverySurfaceTest.java` | `TEST-024` | 模块与导出行数各新增一且既有不变 |
| `REQ-017` | Spec A `§4`, `§14` | 全部 Step | 全部测试 | 全部 `TEST-*` | `clean verify` 无需外部服务 |
| `REQ-018` | Spec A `§4`, `§9.2.6` | Step 10, Step 12 | `execution/RagVectorStoreProbe.java`、`README.md` | `TEST-018` | 默认零模型调用；README 写明计费含义 |
| `REQ-019` | Spec A `§4`, `§9.2.7` | Step 4, Step 7 | `api/RagExtractionService.java`、`model/RagIngestionCommand.java` | `TEST-025`, `TEST-026` | 端到端只解析一次 |
| `REQ-020` | Spec A `§4`, `§9.2.1` | Step 7 | `metadata/RagMetadataKeys.java`、`execution/RagIngestionServiceImpl.java` | `TEST-006` | 同名 key 业务属性胜出 |

## 11. Risks, Blockers, and User Decisions

| ID | Risk or decision | Impacted Steps/files | Evidence | Owner | Status/action |
| --- | --- | --- | --- | --- | --- |
| `BLOCK-001` | 父 POM 是否需要登记 `mapstruct-plus` | Step 1 File 1 | `common-core` 声明了 `mapstruct-plus.version=1.5.1`，但 Spec A `DEC-007` 只要求 MapStruct；本 Plan 的转换器只用 `org.mapstruct` | 实现者 | Closed — 处置已定：若实施时确认不需要，只登记 `mapstruct` 一条并在实施记录中说明；不影响任何 `REQ-*`，由 Step 1 的构建门验证 |
| `BLOCK-002` | `spring-ai-*` 的 `1.1.8` 与本地核对的 `1.1.2` 可能存在 artifact 或签名差异 | Step 1 File 3、Step 5 File 2/3、Step 6 File 8 | Spec A `RISK-001`；本地仓库为 `1.1.2` | 实现者 | Closed — 处置已定：实施前用 `1.1.8` 复核 `TokenTextSplitter`、`VectorStore`、`SearchRequest`、`Document` 与两个解析器读取器的类名与签名；差异只影响组件内部装配 |
| `BLOCK-003` | `BaseConverter` 的契约方法签名与本组件需要的"带额外界参"的映射不完全一致 | Step 7 File 5 | Spec A `§10.4` 说明 `Document` 的 `id` 与元数据不由 `RagChunkBO` 携带 | 实现者 | Closed — 处置已定：本 Plan 采用"实现 `BaseConverter` 并提供 `toDocument(source, chunkId, metadata)` 重载"的方式；若实施时发现与 `BaseConverter` 的抽象不符，按 Spec A `§10.4` 的替代路径处理并在实施记录中说明 |
| `RISK-001` | Micrometer 缺席时的指标跳过需要 `ObjectProvider` 或空实现 | Step 11 File 3 | outbox 组件用 `NoopOutboxMetrics` 双实现先例 | 实现者 | Closed — 处置已定：优先采用与 outbox 一致的"空实现"方式，若 `ObjectProvider` 更简洁则改用之；两者都满足 `REQ-015` 与本 Plan 的断言 |
| `RISK-002` | `RagDocumentStorage#open` 在本地实现中按"目录内首个文件"读取 | Step 9 File 6 | Spec A `ASM-003` 只约束路径布局，未约束读取方式 | 实现者 | Closed — 处置已定：若宿主可能在同目录放多个文件，改为按 `storageKey` 精确读取；`TEST-020` 的往返用例会暴露该假设 |

无阻塞性风险。上表两条 `BLOCK-*` 是实施期可选简化，均不改变任何 `REQ-*` 或公开契约；若实施时发现需要偏离 Spec，必须返回 Spec 而不是在本 Plan 内调整。

## 12. Review and Acceptance

### 12.1 Original requirement fidelity

- 用户要求"引擎机制做在 component" -> Step 1-11 全部落在 `egon-cola-component-rag-starter` 内，`REQ-001`。
- 用户要求"集中管理不做在 component" -> 本 Plan 无 Controller、无 CRUD、无持久化（Spec A `§3.2`、`§7.0` 的 `Remove` 裁决）。
- 用户要求"文档读取支持 pdf/docx/excel 等" -> Step 4（内置文本族）与 Step 5（PDF/Tika 可选），`REQ-004`、`REQ-013`。
- 用户要求"分块算法支持多种" -> Step 6 的枚举 + 工厂 + 三实现，`REQ-005`。
- 用户要求"向量模型支持多种" -> Step 3 的注册表与默认回退，`REQ-006`。
- 用户要求"存储做成扩展点，本地先行后接 OSS" -> Step 9 的 SPI + 本地默认实现，`REQ-012`。
- 用户要求"同一维度不同模型" -> Step 3 的维度校验与 Step 8 的强制过滤，`REQ-007`、`REQ-008`。
- 用户"大量可扩展性" -> 四个 SPI 与 `MC-PATTERN-001` 的策略/工厂/注册表落点。

### 12.2 Spec consistency

本 Plan 未引入任何 Spec A 之外的行为、公开契约、schema 字段、依赖或架构层。逐项核对：

- `§5` 的文件树与 Spec A `§8.2` 的目标树一致，差别只在测试类命名（`PLAN-CLAR-002`、`PLAN-CLAR-003`）。
- 四个 SPI 的签名与 Spec A `§9.2.3`-`§9.2.5` 的契约一致。
- 三个服务的请求/响应字段与 Spec A `§9.2.1`、`§9.2.2`、`§9.2.7` 一致。
- 配置键与默认值与 Spec A `§9.2.6` 的表格逐项一致。
- 例外：Spec A `§9.2.6` 的成功响应表格列出 `ragDocumentExtractorRegistry` 等 Bean 名，本 Plan 在 Step 4/6/9 中把部分注册表声明为 `@Component` 而非 `@Bean` 方法——Bean 名不变（`ragDocumentExtractorRegistry` 等），属于装配实现方式的选择，不影响任何契约或可观察行为。

Spec A 的简洁性与必要性审计已在 `§4.5` 完成，未发现 fetch-then-forward 接口或其它过度设计；Spec A `§7.0` 已把 HTTP、CRUD、Flyway、调度、重排、缓存、分块表与组件内建向量库裁决为 `Remove`，本 Plan 未实现它们。

### 12.3 Repository executability

- 全部路径与符号已对照当前仓库核实：父 POM 的模块清单与属性、BOM 的导出行数、`agent-flow-starter` 的 POM/`lombok.config`/`imports`/测试布局、`common-core` 的 MapStruct 版本属性与 `BaseConverter`、`transactional-outbox-starter` 的 Micrometer 用法与 DDL 风格。
- 依赖顺序由编译与 RED/GREEN 关系推导，见 `§4.3` 与 `§4.6`。
- 每个 Step 的写入范围互不重叠；`RagAutoConfiguration` 被多个 Step 追加方法，已在 `§4.3` 标注为顺序执行。
- 每个文件块给出操作、符号、当前证据、依赖与消费方、顺序理由、签名变化、输入输出映射、失败与边界、伪代码、验证贡献与完成后状态。
- 每条验证命令给出工作目录、精确命令、期望结果与失败返回点。

### 12.4 Test and release completeness

- 每个行为变更都先放置聚焦 RED 测试；Step 1 与 Step 12 的例外已给出证据（无行为可测）。
- 单元、契约、装配、失败语义与交付面回归按 Spec A `§14` 的分层设计。
- 无迁移，故迁移安全为 `N/A` 且有 Spec A `§11` 的证据。
- 兼容性、回滚与前向修复见 `§9`。
- 运行期验证（真实模型与向量库）明确标记为"本 Plan 不声称已执行"。

### 12.5 Blocking Manual Check

| Check ID | Applicability | Status | Evidence | Finding | Required action/exception |
| --- | --- | --- | --- | --- | --- |
| `MC-ARCH-001` | Applicable | PASS | `§4.7` 架构 profile 行；`agent-flow-starter` 的单模块扁平形态；Spec A `DEC-002` 的用户批准 | 恰是一个允许的 profile（component-library 扁平单模块，经用户明确批准的 Rule 11 例外）；`§5` 的目标树无 `biz`、`domain`、`infrastructure`、`adapter` 包 | None |
| `MC-REUSE-001` | Applicable | PASS | `§4.7` 的能力复用台账 11 行 | 先检索了 JDK/Spring、既有 Starter、Egon 组件与模块内候选；`TokenTextSplitter`、`VectorStore`、`EmbeddingModel`、PDF/Tika 读取器、`ValidationUtils`、`BaseConverter`、严格绑定 handler、自动配置机制均复用既有能力 | None |
| `MC-DEP-001` | Applicable | PASS | `§4.7` 台账的 MapStruct 行；`§5` 的模块 POM 依赖表 | 唯一新增依赖是 MapStruct（Spec A `DEC-007`，用户已批准）；`spring-ai-*`、`micrometer-core`、两个解析器均为 `optional` 或由父 BOM 管理 | `BLOCK-001` 为可选简化，不构成未证明的缺口 |
| `MC-NAME-001` | Applicable | PASS | `§5` 的文件树与 `§7` 各文件块的 `Symbols` 行 | 全部新增类型带语义后缀：`Service`/`ServiceImpl`/`Strategy`/`Factory`/`Registry`/`Extractor`/`Converter`/`Exception`/`Enum`/`BO`/`Command`/`Query`/`Result`/`DTO`/`Properties`/`AutoConfiguration`；无 `Data`/`Info`/`Param`/`Bean` | None |
| `MC-VALID-001` | Applicable | PASS | Step 2 File 4/5/6、Step 4 File 8、Step 6 File 5、Step 7 File 6、Step 8 File 2 的伪代码 | 每个层间交接都有 Jakarta 注解与校验：配置载体的 `@Validated` 与 `@Valid` 级联、命令/查询载体的 `@NotNull`/`@NotBlank`/`@Size` 与紧凑构造器规范化、服务方法的 `@Validated`；手工校验复用 `ValidationUtils`；无电话字段故 libphonenumber `N/A`；无复用输入故不新增分组 | None |
| `MC-MODEL-001` | Applicable | PASS | Step 2 File 4/5/6、Step 3 File 3、Step 4 File 3/8、Step 6 File 4/5、Step 7 File 6/7、Step 8 File 2/3、Step 9 File 4 的伪代码 | 全部简单载体为 `record` 并在紧凑构造器规范化；组件内无不可变非 record 对象、无复杂生命周期数据对象，故 `@Value` 与完整 Lombok 基线不适用；无生成构造器签名冲突 | None |
| `MC-CONVERT-001` | Applicable | PASS | Step 7 File 1（RED 断言）与 File 5（转换器伪代码） | 唯一的跨边界转换使用 MapStruct `@Mapper` 接口并 `extends BaseConverter<RagChunkBO, Document>`，`unmappedTargetPolicy=ERROR`，以 `Mappers.getMapper` 获取；无手工 `set/get`、无 `BeanUtils.copyProperties`、无反射、无 JSON 往返 | `BLOCK-003` 记录带额外界参的重载与契约的差异，实施方案见该行 |
| `MC-LOG-001` | Applicable | PASS | Step 4 File 10、Step 6 File 8/11/12、Step 7 File 9、Step 8 File 5、Step 9 File 6、Step 10 File 2、Step 11 File 2 的伪代码 | 每个具体行为类使用 `@Slf4j`；日志字段只含标识、长度、计数、耗时、结果与异常类型；`RagObservabilityTest#never_logs_document_content` 断言零内容泄露 | None |
| `MC-BEAN-001` | Applicable | PASS | Step 2 File 7、Step 3 File 4/5、Step 4 File 5/6/7/10、Step 6 File 8-12、Step 7 File 9、Step 8 File 5、Step 9 File 6/7 的伪代码 | Spring Bean 全部显式命名（`@Component("...")` 或 `@Bean(name="...")`），依赖为 final 字段 + `@RequiredArgsConstructor` + 逐字段 `@Qualifier`；Step 1 File 4 的 `lombok.config` 提供 `Qualifier` 传播，并由 `TEST-021` 断言 | None |
| `MC-UTIL-001` | Applicable | PASS | `§5` 的模块 POM 依赖表；Step 4 File 6、Step 6 File 8/9/10、Step 9 File 6 的伪代码 | 生产代码只用 JDK（`java.nio`、`java.time`、`java.util`、`java.util.regex`）；不新增 `*Utils`；不引入 `commons-*`/Guava；Tika 仅作 `optional` 的内容识别依赖 | `RagMetadataKeys` 是常量与校验契约持有者，不是通用工具类 |
| `MC-JSON-001` | Not applicable | N/A | Spec A `§9.0` 的 GraphQL/REST 行均为 `N/A`；`§9.1` 无 `API-*`；Step 4 File 10 与 Step 9 File 6 无 Jackson 注解 | 组件不对外序列化，也不使用 JSON 做对象转换；Step 7 File 5 的转换器是对象映射而非 JSON 契约 | None |
| `MC-TIME-001` | Applicable | PASS | Step 2 File 1/4/5/7（`Clock`、默认值）、Step 6 File 1（固定 `Clock`）、Step 7 File 7/9（`Duration`）、Step 9 File 4/6（`Instant`）、Step 11 File 1（静态扫描） | 时间字段使用 `Duration`/`Instant`；时钟源为可注入的 `ragClock`；`TEST-021` 静态禁止 `java.util.Date`/`Calendar`/`SimpleDateFormat` 与 `BaseConverter` 的遗留 `Date` 方法 | None |
| `MC-CONFIG-001` | Applicable | PASS | Step 2 File 4/5/6/7/9；`§5` 的模块 POM | 组件的键集完全由 `RagProperties` 与生成的 `spring-configuration-metadata.json` 固定，并由 `NoUnboundElementsBindHandler` 严格绑定；组件不提供 `application-*.yml`，因此不存在 profile 间键差异；`TEST-013` 断言键集 | 消费方的三 profile 一致性属于 Spec B，见 Spec A `§6.2` 的 Rule 7 行 |
| `MC-PATTERN-001` | Applicable | PASS | Step 4 File 4/5、Step 6 File 6/7/10/11/12、Step 9 File 3/5 的伪代码 | 三个真实变化轴各有具体模式：抽取用 Strategy + Registry，分块用 Strategy + Factory + Enum，存储用 Strategy + `@ConditionalOnMissingBean` 默认实现；全部为注册表/映射表查找，无字符串、类型或反射分派 | None |
| `MC-SCOPE-001` | Applicable | PASS | `§5` 的变更文件树与 `§11` | 写入范围限于新模块、父 POM 的模块登记与两条 `dependencyManagement`、BOM 的一条导出、组件架构文档的新增条目；无顺带重构，无既有文件的行为改动 | None |
| `MC-TEST-001` | Applicable | PASS | `§4.2` 的 RED/GREEN 表、`§7` 各 Step 的验证、`§8` 的验证阶梯、`§10` 的追溯矩阵 | 每个行为都有先于实现的聚焦测试；25 个测试覆盖 20 条 `REQ-*`；含静态门禁（包结构、依赖、命名、时间、`lombok.config`）与日志安全断言；两处 test-first 例外有证据 | None |
| `MC-BLOCKER-001` | Applicable | PASS | 本表与 `§11` | 无 `FAIL`、`BLOCKED` 或 `UNKNOWN`；`BLOCK-001`、`BLOCK-002`、`BLOCK-003` 与两条 `RISK-*` 均为实施期可选简化或需复核项，均不改变 `REQ-*` 或公开契约，且各自给出处置路径 | None |

### 12.6 Final verdict

`PASS — Implementation conforms to the effective Specs`（实施后回填）

十二个 Step 全部实施并提交，模块 `clean verify` 99 个测试全绿，components Reactor 回归通过。
实施期发现五处需要记录的偏离，全部写在 `§11`：其中三处是计划里的机械性顺序问题，一处是
Spec A `§10.4` 的泛型参数与散文描述不一致，一处是本 Plan 自己预期过的 `RagEmbeddingException`
契约差异。逐项对照见交付报告。

本 Plan 未修改任何生产或测试代码、未执行迁移、未启动应用、未连接模型或数据库，也未声称任何运行期验证。`§8` 与 `§7` 中的全部命令都是**将来的执行指令**，不是已执行的证据。
