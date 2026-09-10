# Agent Archetype 知识库实施计划

| Field | Value |
| --- | --- |
| Document | `docs/egon/plan/2026-09-10-14-35-agent-archetype-knowledge-rag-implementation.md` |
| Template Version | `4` |
| Status | `Review` |
| Created | `2026-09-10 14:35 CST` |
| Updated | `2026-09-10 15:10 CST` |
| Owner | `User` |
| Repository | `Egon-COLA` |
| Scope | `egon-cola-archetypes/source-projects/egon-cola-source-agent` 新增 `knowledge` 业务域与全部持久化、装配、接口；`definitions/egon-cola-archetype-agent` 的门禁与打包清单同步修订 |
| Source Requirement | 用户要求 agent archetype 引入 RAG：引擎用 `rag-starter`、管理留在 archetype、无前端、原文入库不重复解析、同维度多模型、存储可扩展、持久化用 MyBatis Plus、向量表由 `PgVectorStore` 自建、租户字段先加上但来源后置 |
| Baseline Revision | `main` 分支之外的 `docs/rag-starter-spec` 分支，提交 `139c75772`；工作区干净 |
| Implements Spec | [Agent Archetype 知识库（RAG）设计](../spec/2026-09-10-11-51-agent-archetype-knowledge-rag.md) |
| Spec Status | `Accepted` |
| Spec Revision | `Updated 2026-09-10 12:47 CST`；接受于 `12:33 CST` |
| Effective Specs | [Agent Archetype 知识库设计](../spec/2026-09-10-11-51-agent-archetype-knowledge-rag.md)（全部章节）；[Agent Deep Research Archetype 设计](../spec/2026-09-04-16-32-agent-deep-research-archetype.md)（被本 Spec `Amends`，其余章节继续有效）；[RAG 引擎组件设计](../spec/2026-09-10-11-34-egon-cola-component-rag-starter.md)（规范依赖，已实施）；`egon-cola-component-transactional-outbox-starter` README 的 `PostgreSQL Migration` 与 `Direct API Example` 两节 |
| Depends On Plans | [RAG 引擎组件实施计划](2026-09-10-12-47-egon-cola-component-rag-starter-implementation.md)（已实施，其产物是本计划的编译前提） |
| Supersedes | `None` |
| Superseded By | `None` |
| Related Plans | `None` |

## 1. Summary

本 Plan 实现已接受的 Spec B：让 `egon-cola-source-agent` 生成的项目成为自己的 RAG 管理后端。依赖方向自底向上：先放宽四道门禁与打包清单（否则任何持久化代码都进不来），再登记依赖与配置，然后建立 Flyway 迁移与数据模型，接着是持久化与向量装配、outbox 异步摄取、应用用例，最后是三组 REST/SSE 接口与契约测试。

共 **12 个 Step**。完成证据是：源码项目 `clean verify` 通过、生成项目 `clean verify` 通过、生成产物含两条迁移、研究域既有测试不改动即通过。

本 Plan **不实现**租户身份解析与鉴权：租户列与显式作用域落地，来源仍是配置的默认值。

## 2. Target Spec and Effective Design

### 2.1 Primary target

- Path: [`docs/egon/spec/2026-09-10-11-51-agent-archetype-knowledge-rag.md`](../spec/2026-09-10-11-51-agent-archetype-knowledge-rag.md)
- Status: `Accepted`
- Revision: `Updated 2026-09-10 12:47 CST`
- Approval evidence: 用户 2026-09-10 12:33 确认接受并转入实现计划。

### 2.2 Effective Spec set

| Role | Spec/link | Status/revision | Effective sections | Why included |
| --- | --- | --- | --- | --- |
| Primary | [Agent Archetype 知识库设计](../spec/2026-09-10-11-51-agent-archetype-knowledge-rag.md) | `Accepted` | 全部（`§1`-`§20`） | 本 Plan 的唯一实现依据 |
| Amended | [Agent Deep Research Archetype 设计](../spec/2026-09-04-16-32-agent-deep-research-archetype.md) | `Accepted` | 除 `§4`（`REQ-003`、`REQ-013`）、`§7.1.2`、`§8.2`、`§11` 之外全部继续有效 | 本 Spec 的 `Amends` 目标；研究域接口、SSE 事件、容量边界与发布数量合同仍由它管辖 |
| Dependency | [RAG 引擎组件设计](../spec/2026-09-10-11-34-egon-cola-component-rag-starter.md) | `Accepted` 且 `Implemented`（提交 `139c75772`） | `§7`、`§8`、`§9`、`§10`、`§16` | 三个服务入口、四个 SPI 与配置键的实际契约；本 Plan 消费其已交付产物 |
| Dependency | `egon-cola-component-transactional-outbox-starter` README | 仓库当前版本 | `PostgreSQL Migration`、`Direct API Example`、`Delivery Guarantee` | 迁移重编号要求、`enqueue` 事务要求与至少一次语义 |

### 2.3 Superseded or excluded content

- Spec B `§3.2` 明确排除的能力全部不实现：前端页面、分块预览接口、知识库级权限与租户鉴权、问答会话持久化、重排与混合检索、对象存储实现、不同维度模型共存。
- 本 Plan 不触及 `research` 域的任何生产文件；只有 `AgentArchitectureTest` 与 `AgentSourceContractTest` 的禁项列表因本 Spec 的 `Amends` 而修改。

## 3. Effective Requirements and Acceptance

| Requirement | Source Spec section | Effective statement | Observable acceptance | Implementation impact |
| --- | --- | --- | --- | --- |
| `REQ-001` | Spec B `§4` | 新增 `knowledge` 业务域，六模块与依赖方向不变 | 模块集合不变；业务包为 `research` 与 `knowledge`；`check-reactor` 通过 | 六模块各增一棵子树 |
| `REQ-002` | Spec B `§9.2.1`-`§9.2.5` | 知识库建/查/改/删四类操作 | 四个端点各有契约；删除级联清文档、文件与向量分块 | `KnowledgeBaseManage` + `KnowledgeBaseController` |
| `REQ-003` | Spec B `§9.2.6`-`§9.2.10` | 文档上传、列表、详情、重新处理、删除 | 上传返回 202；列表与详情暴露状态与分块数；删除清除行、文件与分块 | `KnowledgeDocumentManage` |
| `REQ-004` | Spec B `§7.3.3` | 上传在一个本地事务内写文档行与 outbox 行 | 成功时两行同时存在；任一失败都不存在 | `@Transactional` 上传路径 |
| `REQ-005` | Spec B `§7.3.4`, `§10.6` | 摄取异步执行且状态可查询 | 状态枚举与 outbox 状态一一映射；上传立即返回 | outbox `DeliveryHandler` |
| `REQ-006` | Spec B `§7.3.1` | 摄取幂等，且重跑不重新上传、下载或解析原文件 | 重跑分块 id 集合一致；抽取只发生一次 | handler 从 `content` 重建文档 |
| `REQ-007` | Spec B `§9.2.6`, `§11.2.2` | 原文件按存储扩展点保存，抽取文本入库 | 原文件可经 `RagDocumentStorage` 取回；`knowledge_document.content` 非空 | `storage_key` 与 `content` 列 |
| `REQ-008` | Spec B `§7.1.2` | 嵌入与向量存储由宿主装配 | 生成项目提供具名 `EmbeddingModel` 与 `VectorStore` | `KnowledgeVectorConfiguration` |
| `REQ-009` | Spec B `§9.2.11` | 支持多个同维度嵌入模型且不允许串结果 | 跨模型检索互不返回；维度不一致启动失败 | 注册表 + 强制过滤 |
| `REQ-010` | Spec B `§11.1` | 向量表由 `PgVectorStore` 创建，Flyway 只建扩展与业务表 | 迁移不含 `vector_store` 建表；启动后表存在 | `initializeSchema(true)` |
| `REQ-011` | Spec B `§9.2.11` | 检索调试接口返回片段与分数且不调用对话模型 | 有序片段与分数；无持久化写入 | `KnowledgeQaController#retrieveKnowledgeChunks` |
| `REQ-012` | Spec B `§9.2.12` | 知识库问答 SSE | 单流返回 started/progress/completed 或 failed 之一并终止；含引用 | SSE 事件契约 |
| `REQ-013` | Spec B `§9.0` | 全部新接口复用现有 API Key，接口不接受租户头 | 无 key 返回既有 401；不新增鉴权配置 | 复用过滤器 |
| `REQ-014` | Spec B `§9.2.x` | 全部新接口复用既有错误体与 trace 约定 | 错误体形状与 `X-Trace-Id` 一致 | 复用 `DeepResearchErrorResponse` |
| `REQ-015` | Spec B `§11.2.1` | 迁移位于 infrastructure 模块并被 archetype 正确打包 | 两条迁移存在且随生成产出 | 迁移文件 + `archetype-metadata.xml` |
| `REQ-016` | Spec B `§11.2.3` | outbox 表由本项目复制并重编号创建 | 迁移含建表语句；启动期 schema 校验通过 | 复制组件 DDL |
| `REQ-017` | Spec B `§16` 清单 1-3 | 两份测试与验证器的禁项只放开 `flyway` 与 `mybatis` | 修订后源码项目构建通过；其余禁项仍生效 | Step 1 |
| `REQ-018` | Spec B `§16` 清单 4 | `archetype-metadata.xml` 为 infrastructure 新增资源文件集 | 生成产物含两条迁移 | Step 1 |
| `REQ-019` | Spec B `§16` 清单 3 | `verify.groovy` 新增 RAG/outbox/pgvector/mybatis 存在性断言 | 依赖与运行时库断言通过 | Step 1 与 Step 12 |
| `REQ-020` | Spec B `§16` 清单 5 | 架构文档不再声明"无数据库/无迁移" | 文档描述 knowledge 域与迁移位置 | Step 12 |
| `REQ-021` | Spec B `§4` | 生成项目可独立 `clean verify` 通过 | 生成后 `clean verify` 成功且无 source sentinel | Step 12 |
| `REQ-022` | Spec B `§7.2.2` | 问答受独立并发与超时约束，不与研究互斥 | 饱和 429 + `Retry-After`；无跨用例共享互斥 | `KnowledgeQaCapacityService` |
| `REQ-023` | Spec B `§4` | 全部验证离线、确定性 | `clean verify` 不需凭据或外部服务 | 全部 Step 的验证门 |
| `REQ-024` | Spec B `§4` | 研究域接口、事件与错误码不变 | 既有研究测试不改动即通过 | Step 12 回归 |
| `REQ-025` | Spec B `§11.2.1` | 两张业务表保留租户列，经 MDC 由拦截器自动作用域 | `tenant_id` 非空且为 MDC 租户；业务键租户内唯一；生成 SQL 含租户条件；Service/DAO 无租户参数 | Step 3 列、Step 11 过滤器 |
| `REQ-026` | Spec B `§7.3.1` | outbox 载荷携带租户，handler 重建并清理 MDC | 投递线程起始无 MDC 时仍按正确租户读写，结束后已清理 | Step 7 |
| `REQ-027` | Spec B `§11.2.4` | 向量元数据携带租户且检索强制过滤 | 过滤恒含 `tenantId`；跨租户不串结果 | Step 6 与 Step 11 |

## 4. Implementation Strategy and Dependency Order

### 4.1 Ordered strategy

1. **Step 1 先放宽门禁**。`AgentArchitectureTest`、`AgentSourceContractTest`、`verify.groovy` 与 `archetype-metadata.xml` 目前禁止 `flyway`/`mybatis` 并断言基础设施模块没有 `db` 目录。不先改，后面每一步的构建都会失败。此步只放宽，不加任何业务代码，因此验证是"源码项目仍然通过"。
2. **Step 2-3 建立构建与持久化基线**。依赖登记与四 profile 配置先行，迁移随其后；迁移是后续所有 PO/DAO 的 schema 前提。
3. **Step 4 建立领域词汇**。BO、枚举与端口不依赖任何框架，是 application 与 infrastructure 的共同下游。
4. **Step 5-6 建立基础设施**。先持久化（PO/DAO/Converter），再向量与嵌入装配，最后接 outbox 摄取。摄取 handler 同时依赖前两者，因此顺序固定。
5. **Step 7-8 建立应用与接口**。用例只依赖 domain 端口；接口只依赖 application。
6. **Step 11 建立租户作用域**。MDC 过滤器与 handler 的租户恢复必须在全部数据访问路径就位后统一接入，否则会散落。
7. **Step 12 收口**。契约测试、文档与生成回归。

### 4.2 Test-first strategy

| Step | RED 测试 | 期望的 RED 原因 | 最小 GREEN |
| --- | --- | --- | --- |
| 1 | 不适用（只改门禁与清单，无行为） | — | 放开禁项与新增文件集 |
| 2 | 不适用（只改 POM） | — | 依赖登记 |
| 3 | `KnowledgeSchemaMigrationTest` | 迁移文件不存在 | 两条迁移 |
| 4 | `KnowledgeDomainTest` | 域类型不存在 | 领域词汇与端口 |
| 5 | `KnowledgeBaseManageImplTest`（持久化部分） | DAO/PO 不存在 | PO/DAO/Converter |
| 6 | `KnowledgeVectorConfigurationTest` | 装配不存在 | 向量与嵌入 Bean |
| 7 | `KnowledgeIngestDeliveryHandlerTest` | handler 不存在 | handler 与 MDC 恢复 |
| 8 | `KnowledgeManageTest` | 用例不存在 | 三个 Manage |
| 9 | `KnowledgeBaseControllerTest` | 控制器不存在 | API-001..005 |
| 10 | `KnowledgeDocumentControllerTest` | 控制器不存在 | API-006..010 |
| 11 | `KnowledgeQaControllerTest` | 控制器不存在 | API-011、012 与租户过滤器 |
| 12 | 不适用（契约与文档） | — | 契约测试与文档 |

### 4.3 Sequential and parallel boundaries

| Step | Depends on | May run in parallel with | Must not overlap with | Reason |
| --- | --- | --- | --- | --- |
| Step 1 | None | None | 全部后续 Step | 门禁未放宽则后续构建失败 |
| Step 2 | Step 1 | None | Step 3 | 同改根 `pom.xml` |
| Step 3 | Step 2 | None | Step 2 | 迁移与配置同批 |
| Step 4 | Step 2 | Step 5 | — | 独立包 |
| Step 5 | Step 3, Step 4 | Step 6（不共写文件时） | Step 6 | Step 6 依赖 Step 5 的仓储 |
| Step 6 | Step 5 | None | Step 7 | 同改 starter 配置与 infrastructure 装配 |
| Step 7 | Step 4, Step 6 | None | Step 8 | 同改 application 包 |
| Step 8 | Step 7 | Step 9 | Step 9 | 同改 application 包 |
| Step 9 | Step 8 | Step 10 | Step 10 | 同改 adapter 包 |
| Step 10 | Step 9 | None | Step 11 | 同改 adapter 包 |
| Step 11 | Step 9, Step 10 | None | Step 12 | 同改 adapter 包 |
| Step 12 | Step 1-11 | None | — | 契约覆盖全部生产类型 |

建议严格串行：`adapter` 与 `application` 包是多个 Step 的共同写入点。

### 4.4 Commit boundaries

每 Step 一个语义提交。唯一例外是 Step 1：四处门禁修订不可分割——只改一部分会让源码项目既不能通过也不能构建，因此同一提交。

### 4.5 Spec Simplicity and Implementation-necessity Audit

| Spec element | Spec necessity verdict/section | Current repository evidence | Direct/reuse alternative | Interaction/implementation cost | Plan decision |
| --- | --- | --- | --- | --- | --- |
| `knowledge` 业务域包 | `Add`（Spec B `§7.0`） | `source-agent` 六模块与 `research` 包布局 | 把知识库并入 `research` 包 | 每模块新增子树与 `package-info` | Implement |
| 两张业务表 | `Add`（`§7.0`, `§11.1`） | `VectorStore` 无列举 API（组件 `EVD-011`） | 只把元数据放进向量 metadata | 两张表、一套迁移、一组 PO/DAO | Implement |
| outbox 异步摄取 | `Add`（`§7.0`, 用户已选方案 C） | `egon-cola-component-transactional-outbox-starter` 已在 BOM | 上传内同步嵌入 | 表副本、同事务入队、handler、轮询运维面 | Implement |
| 本地文件存储 | `Add`（`REQ-007`） | 组件的 `RagDocumentStorage` SPI 与其本地实现已交付 | 只存文本不留原文件 | 本地目录与清理语义 | Implement |
| `PgVectorStore` 自建表 | `Add`（`REQ-010`, 用户选择） | 组件的配置只引用 Bean 名 | Flyway 手写向量表 DDL | 一个宿主 Bean | Implement |
| `ragVectorStore` Bean 命名 | Spec B `Context-only` | 组件 `§9.2.6` 的 Bean 名 | — | 无 | 复用组件 |
| 12 个 REST/SSE 接口 | `Add`（`§9.1`） | 研究域已有 Controller/错误体/过滤器形状 | 复用研究域端点 | 三组 Controller 与边界载体 | Implement |
| 分块预览接口 | `Remove`（`DEC-007`） | 组件不暴露分块枚举能力 | — | — | 不实现 |
| 知识库级权限与租户鉴权 | `Remove`（`§3.2`） | 单一 API Key（`EVD-021`） | — | — | 不实现 |
| 问答会话持久化 | `Remove`（`§3.2`） | — | — | — | 不实现 |
| 重排与混合检索 | `Remove`（`§3.2`） | — | — | — | 不实现 |
| `research` 域改动 | `Remove`（`REQ-024`） | 既有研究包与测试 | — | — | 不实现 |

未发现 fetch-then-forward 接口：`API-011` 的检索调试有免计费的独立诊断目标（`§9.2.11` 已论证），其余 11 个端点各自对应独立的用户目标。

### 4.6 Change-unit Dependency Matrix

| Change unit | Requirements | Proof/RED point | Compile/runtime prerequisites | Produces | Consumers/unblocks | Owning Step |
| --- | --- | --- | --- | --- | --- | --- |
| 放开的门禁与打包清单 | `REQ-017`-`REQ-019` | 源码项目 `clean verify` | 无 | 可持久化的构建环境 | 全部后续 Step | Step 1 |
| 依赖登记 | `REQ-008`-`REQ-010`, `REQ-015` | 构建成功 | Step 1 | 可解析的 rag/outbox/pgvector/mybatis/flyway | Step 3-6 | Step 2 |
| 迁移与四 profile 配置 | `REQ-015`, `REQ-016`, `REQ-025` | `KnowledgeSchemaMigrationTest` | Step 2 | 两张业务表、outbox 表、扩展、配置键 | Step 5-7 | Step 3 |
| 领域词汇与端口 | `REQ-001`, `REQ-002`, `REQ-006` | `KnowledgeDomainTest` | Step 2 | BO、枚举、gateway、仓储契约 | Step 5-8 | Step 4 |
| 持久化实现 | `REQ-002`, `REQ-003`, `REQ-025` | `KnowledgeBaseManageImplTest` | Step 3, Step 4 | PO、DAO、Converter | Step 7, Step 8 | Step 5 |
| 向量与嵌入装配 | `REQ-008`, `REQ-009`, `REQ-010`, `REQ-027` | `KnowledgeVectorConfigurationTest` | Step 2 | `VectorStore`、`EmbeddingModel`、向量 gateway | Step 7, Step 11 | Step 6 |
| 异步摄取 | `REQ-004`-`REQ_007`, `REQ_016`, `REQ_026` | `KnowledgeIngestDeliveryHandlerTest` | Step 5, Step 6 | handler、状态机、租户恢复 | Step 8 | Step 7 |
| 应用用例 | `REQ-002`, `REQ-003`, `REQ_005`, `REQ_008`, `REQ_022` | `KnowledgeManageTest` | Step 4, Step 7 | 三个 Manage 与容量服务 | Step 9-11 | Step 8 |
| 知识库接口 | `REQ-002` | `KnowledgeBaseControllerTest` | Step 8 | API-001..005 | Step 12 | Step 9 |
| 文档接口 | `REQ-003`, `REQ-005`, `REQ_007` | `KnowledgeDocumentControllerTest` | Step 8 | API-006..010 | Step 12 | Step 10 |
| 检索、问答与租户 | `REQ_011`-`REQ_014`, `REQ_022`, `REQ_025`, `REQ_027` | `KnowledgeQaControllerTest` | Step 8, Step 9 | API-011、012、租户过滤器 | Step 12 | Step 11 |
| 契约与回归 | `REQ-013`, `REQ-018`-`REQ-024` | 全部测试 | Step 1-11 | 契约测试与文档 | 发布 | Step 12 |

### 4.7 Java, Spring, and Egon-COLA Implementation Standards

| Concern | Current repository evidence | Effective Spec decision | Planned implementation consequence | Owning Steps/checks |
| --- | --- | --- | --- | --- |
| Architecture profile | `source-agent` 六模块 Web non-open；`AgentArchitectureTest`、`verify.groovy` | Spec B `§6.1` | 保留六模块与依赖方向；`knowledge` 只是业务域包 | 每个 Step；`MC-ARCH-001` |
| Reuse/capability | 组件 `rag-starter` 三个服务入口；`common-core` 的 `BaseConverter`/`ValidationUtils`；`common-mybatis-plus-starter` | Spec B `§6.1` ledger | 全部复用；不新增第三方依赖 | Step 2, 5, 6；`MC-REUSE-001` / `MC-DEP-001` |
| Naming/model/validation/conversion | `ResearchEventConverter` 的 MapStruct 写法；`RolePO` 的 PO 组合 | Spec B `§10.1`-`§10.4` | 载体带语义后缀；简单对象 `record`；PO 用仓库既有组合（Rule 3 例外，用户已批准） | Step 4, 5, 8；`MC-NAME-001` / `MC-MODEL-001` / `MC-CONVERT-001` |
| Bean/logging/util/JSON/time/config | `DeepResearchManageImpl` 的注解风格；`lombok.config` | Spec B `§6.2` | 显式 Bean 名、构造器注入、`@Slf4j`；只用 JDK 与既有依赖；`java.time` | Step 4-11；`MC-BEAN-001` / `MC-LOG-001` / `MC-UTIL-001` / `MC-TIME-001` / `MC-CONFIG-001` |
| Business variation/pattern | outbox `DeliveryHandler`；`KnowledgeVectorGateway` 端口 | Spec B `§13.1` | 端口 + 适配器、outbox 策略、显式状态机；无业务分支枚举 | Step 4, 6, 7；`MC-PATTERN-001` |
| 租户语义 | `EgonModel.tenantId`；`tenant-id.ignored-tables` | Spec B `DEC-012`-`DEC-015` | MDC 通道 + 请求过滤器 + handler 恢复 | Step 3, 7, 11；`REQ-025`-`REQ-027` |

#### Capability reuse ledger

| Need | Candidates inspected | Exact evidence | Fit/gap | Decision | Added dependency/custom code | Owning Step/check |
| --- | --- | --- | --- | --- | --- | --- |
| RAG 引擎 | `egon-cola-component-rag-starter`（已交付） | 提交 `139c75772`；三个服务入口与四个 SPI | 完全足够 | Reuse | `None` | Step 2；`MC-REUSE-001` |
| 持久任务与重试 | `egon-cola-component-transactional-outbox-starter` | 组件 README 与自带 DDL | 完全足够；表需自建 | Reuse | `None` | Step 2, 3, 7 |
| 关系型持久化 | 组件 `common-mybatis-plus-spring-boot-starter` | `EgonModel`、`EgonColaMapper`、`tenant-id.ignored-tables` | 完全足够 | Reuse | `None` | Step 3, 5 |
| 表结构管理 | Flyway（既有 source 项目使用） | `source-web` 的 `db/migration` 布局与日期命名 | 完全足够 | Reuse | `None` | Step 1, 3 |
| 向量存储 | `spring-ai-pgvector-store` | 组件 `EVD-018` 的表结构与 builder | 完全足够 | Reuse | 模块依赖一条 | Step 2, 6 |
| 嵌入模型 | `spring-ai-openai`（既有） | `source-agent-starter` 已声明 | 完全足够 | Reuse | `None` | Step 2, 6 |
| 边界校验 | `spring-boot-starter-validation` + `ValidationUtils` | 既有依赖与 `common-core` | 完全足够 | Reuse | `None` | Step 4, 8 |
| 对象转换 | MapStructPlus（既有） | `source-agent` 已声明 `mapstruct-plus-spring-boot-starter` | 完全足够 | Reuse | `None` | Step 5, 8 |
| 错误体与 trace | `DeepResearchErrorResponse`、`ResearchTraceFilter` | 既有 adapter 包 | 完全足够 | Reuse | `None` | Step 9-11 |
| 鉴权 | `ResearchApiKeyFilter` | 未限定 URL 模式，覆盖全部路径 | 完全足够；接口不接受租户头 | Reuse | `None` | Step 9-11 |

未发现重复实现，也未新增第三方依赖。

### 4.8 User-mandated Java Rule Implementation Matrix

| Literal rule | Spec source | Repository evidence | Exact files and order | Pseudocode obligations | Validation gate | Steps | Status/blocker |
| --- | --- | --- | --- | --- | --- | --- | --- |
| Rule 1 | Spec B `§6.2`、`§10.1` | 全部新增类型见 `§5` | domain 模型 → 端口 → 仓储契约 → PO/DAO/Converter → Manage → Controller/DTO/VO | 后缀：`BO`/`PO`/`DAO`/`Command`/`Query`/`VO`/`Request`/`Enum`/`Manage`/`ServiceImpl`/`Controller`/`Converter`/`Gateway`/`Handler`/`Properties`/`Configuration` | `KnowledgeContractTest` 命名扫描 | Step 4-11 | PASS |
| Rule 2 | Spec B `§6.2`、`§9.2.x` | `ValidationUtils`、`spring-boot-starter-validation` | 命令、查询与 Request 载体；三个 Manage 实现 | 每层交接 Jakarta 注解 + `@Valid` 级联；Manage 方法 `@Validated`；手工校验复用 `ValidationUtils`；无电话字段故 libphonenumber `N/A` | `KnowledgeValidationTest` | Step 4, 8-11 | PASS |
| Rule 3 | Spec B `§6.2`、`§6.2.1`、`§10.3.1` | `RolePO` 的 PO 注解组合；`ResearchEventConverter` | PO 与 Converter 文件 | 简单载体 `record` + 紧凑构造器；**PO 使用仓库既有约定（无 `@RequiredArgsConstructor`），用户已批准为规则 3 例外**；转换器用 MapStructPlus 并 `extends BaseConverter` | 编译 + 转换器测试 | Step 5, 8 | PASS（含已批准例外） |
| Rule 4 | Spec B `§6.2`、`§7.3.1` | `DeepResearchManageImpl` 的 `@Service("...")` + `@RequiredArgsConstructor` + `@Qualifier` | 三个 Manage 实现、gateway 实现、handler、过滤器、配置类 | 每类 `@Slf4j`；显式 Bean 名；final 字段 + `@RequiredArgsConstructor` + 逐字段 `@Qualifier` | 上下文装配测试 | Step 5-11 | PASS |
| Rule 5 | Spec B `§6.2`、`§15` | `source-agent` 既有依赖清单 | 全部生产文件 | 只用 JDK 与仓库既有依赖；不新增 `*Utils` | 依赖与 import 扫描 | Step 4-11 | PASS |
| Rule 6 | Spec B `§6.2`、`§9.2.x` | `DeepResearchErrorResponse` 的 Jackson 用法 | Request、VO 与错误体 | `@JsonInclude(NON_NULL)`；枚举与 `Instant` 的 wire 语义显式声明；不引入第二个 JSON 库 | 序列化与 OpenAPI 测试 | Step 9-11 | PASS |
| Rule 7 | Spec B `§6.2`、`§15` | `application.yml` 与三个 profile | 四个配置文件同批修改 | 新增键在四个文件中同构；值可不同 | `KnowledgeConfigParityTest` | Step 3 | PASS |
| Rule 9 | Spec B `§6.2`、`§13.1` | outbox 组件示例；`KnowledgeVectorGateway` 端口 | 端口 → 适配器；策略注册表 | 端口/适配器、Strategy、显式状态机；禁止硬编码分派 | 行为与分支测试 | Step 4, 6, 7 | PASS |
| Rule 10 | Spec B `§6.2`、`§10.3` | `EgonModel` 的 `Instant` 审计列；既有 `agentClock` | 全部时间字段 | `Instant`/`Duration`；复用既有 `agentClock`；禁止 `java.util.Date` | `KnowledgeContractTest` 静态扫描 | Step 4, 5, 8 | PASS |
| Rule 11 | Spec B `§6.1`、`§8.2` | `source-agent` 六模块与 `check-reactor` 包映射 | 全部目标文件位于六个既有包前缀之下 | 不新增模块或层；`knowledge` 只是业务域包 | `check-reactor` + `AgentArchitectureTest` | 每个 Step | PASS |

## 5. Change File Tree

```text
egon-cola-archetypes/source-projects/egon-cola-source-agent/
├── pom.xml                                                          # MODIFY
├── egon-cola-source-agent-common/
│   ├── pom.xml                                                      # MODIFY
│   └── src/main/java/.../common/error/KnowledgeErrorCodeEnum.java    # CREATE
├── egon-cola-source-agent-domain/
│   ├── pom.xml                                                      # MODIFY
│   └── src/main/java/.../domain/knowledge/
│       ├── model/{KnowledgeBaseBO,KnowledgeDocumentBO,KnowledgeChunkBO,
│       │          KnowledgeChunkConfigBO,DocumentIngestStatusEnum,
│       │          KnowledgeBaseStatusEnum,ChunkingStrategyEnum}.java # CREATE
│       ├── gateway/KnowledgeVectorGateway.java                       # CREATE
│       ├── repository/{KnowledgeBaseRepository,KnowledgeDocumentRepository}.java  # CREATE
│       └── package-info.java                                         # CREATE
├── egon-cola-source-agent-application/
│   ├── pom.xml                                                      # MODIFY
│   └── src/main/java/.../application/knowledge/
│       ├── command/{CreateKnowledgeBaseCommand,UpdateKnowledgeBaseCommand,
│       │            UploadKnowledgeDocumentCommand,RetrieveKnowledgeCommand,
│       │            AskKnowledgeBaseCommand}.java                    # CREATE
│       ├── config/{KnowledgeRuntimeProperties,KnowledgeApplicationConfiguration}.java  # CREATE
│       ├── manage/{KnowledgeBaseManage,KnowledgeDocumentManage,KnowledgeQaManage}.java # CREATE
│       ├── manage/impl/{KnowledgeBaseManageImpl,KnowledgeDocumentManageImpl,
│       │                  KnowledgeQaManageImpl}.java                # CREATE
│       ├── service/KnowledgeQaCapacityService.java                   # CREATE
│       ├── exception/KnowledgeApplicationException.java              # CREATE
│       └── package-info.java                                         # CREATE
├── egon-cola-source-agent-infrastructure/
│   ├── pom.xml                                                      # MODIFY
│   └── src/main/
│       ├── java/.../infrastructure/knowledge/
│       │   ├── config/{KnowledgeVectorConfiguration,KnowledgeEmbeddingConfiguration}.java  # CREATE
│       │   ├── gateway/RagKnowledgeVectorGateway.java                # CREATE
│       │   ├── handler/KnowledgeIngestDeliveryHandler.java           # CREATE
│       │   ├── repo/po/{KnowledgeBasePO,KnowledgeDocumentPO}.java    # CREATE
│       │   ├── repo/dao/{KnowledgeBaseDAO,KnowledgeDocumentDAO}.java # CREATE
│       │   ├── repo/converter/{KnowledgeBasePOConverter,KnowledgeDocumentPOConverter}.java # CREATE
│       │   ├── service/{KnowledgeBaseRepositoryImpl,KnowledgeDocumentRepositoryImpl}.java  # CREATE
│       │   └── package-info.java                                     # CREATE
│       └── resources/db/migration/
│           ├── V20260910_001__create_knowledge_schema.sql            # CREATE
│           └── V20260910_002__create_transactional_outbox_schema.sql # CREATE
├── egon-cola-source-agent-adapter/
│   ├── pom.xml                                                      # MODIFY
│   └── src/main/java/.../adapter/knowledge/
│       ├── controller/{KnowledgeBaseController,KnowledgeDocumentController,
│       │              KnowledgeQaController}.java                    # CREATE
│       ├── converter/{KnowledgeCommandConverter,KnowledgeVoConverter,
│       │              KnowledgeEventConverter}.java                  # CREATE
│       ├── dto/{CreateKnowledgeBaseRequest,UpdateKnowledgeBaseRequest,
│       │         RetrieveKnowledgeRequest,AskKnowledgeBaseRequest}.java  # CREATE
│       ├── filter/KnowledgeTenantMdcFilter.java                      # CREATE
│       ├── vo/{KnowledgeBaseVO,KnowledgeDocumentVO,KnowledgeRetrievedChunkVO,
│       │        KnowledgeQaEventVO}.java                             # CREATE
│       └── package-info.java                                         # CREATE
└── egon-cola-source-agent-starter/
    ├── pom.xml                                                      # MODIFY
    └── src/main/resources/{application.yml,application-dev.yml,
                            application-test.yml,application-prod.yml}  # MODIFY

egon-cola-archetypes/definitions/egon-cola-archetype-agent/
├── src/main/resources/META-INF/maven/archetype-metadata.xml          # MODIFY
├── src/test/resources/projects/basic/verify.groovy                   # MODIFY
└── architecture-docs/agent-multi-module-architecture.md              # MODIFY

（测试文件见 §7 各 Step 的 Ordered files）
```

| Operation | Path | Current evidence/symbol | Final symbols/state | Responsibility | Step | Requirements | Validation owner |
| --- | --- | --- | --- | --- | --- | --- | --- |
| MODIFY | `...-starter/src/test/java/.../architecture/AgentArchitectureTest.java` | 第 70-72 行的禁项列表含 `flyway`、`mybatis` | 移除两个 token；新增 knowledge 依赖方向断言 | 放开禁项 | Step 1 | `REQ-017` | `TEST-001` |
| MODIFY | `...-starter/src/test/java/.../starter/AgentSourceContractTest.java` | 第 38 行正则 `(fastjson\ | flyway\ | mybatis\ | redis\ | graphql\ | dubbo)` | 改为 `(fastjson\ | redis\ | graphql\ | dubbo)` | 放开禁项 | Step 1 | `REQ-017` | `TEST-001` |
| MODIFY | `definitions/.../verify.groovy` | 第 78-80 行 `forbiddenDependencies`；第 129 行 `missing(db)`；第 193 行 forbiddenToken | 移除 `flyway-core`、`mybatis-plus-spring-boot3-starter`、`flyway` token 与 `missing(db)`；新增依赖与迁移断言 | 放开禁项并新增断言 | Step 1 | `REQ-017`-`REQ-019` | `TEST-002` |
| MODIFY | `definitions/.../archetype-metadata.xml` | infrastructure 模块无 resources 文件集 | 新增 `src/main/resources` 文件集含 `**/*.sql` | 打包迁移 | Step 1 | `REQ-018` | `TEST-003` |
| MODIFY | `source-agent/pom.xml` 与模块 POM | 现有依赖管理与六模块 | 新增 rag/outbox/pgvector/mybatis/flyway 依赖 | 依赖登记 | Step 2 | `REQ-008`-`REQ-010` | 构建 |
| CREATE | `infrastructure/src/main/resources/db/migration/*.sql` | 无 `db` 目录 | 两条迁移 | schema | Step 3 | `REQ-015`, `REQ-016`, `REQ-025` | `TEST-004` |
| MODIFY | `starter/src/main/resources/application*.yml` | 无 datasource/flyway/rag/outbox 键 | 四 profile 同构新增键 | 配置 | Step 3 | `REQ-008`-`REQ-010`, `REQ-025` | `TEST-005` |
| CREATE | `domain/knowledge/**` | 只有 `domain/research` | BO、枚举、端口、仓储与服务契约 | 领域词汇 | Step 4 | `REQ-001`, `REQ-006` | `TEST-006` |
| CREATE | `infrastructure/knowledge/repo/**` | 只有 `infrastructure/research` | PO、DAO、Converter、仓储实现 | 持久化 | Step 5 | `REQ-002`, `REQ-003`, `REQ-025` | `TEST-007` |
| CREATE | `infrastructure/knowledge/{config,gateway}/**` | 无同类 | 向量/嵌入装配与 gateway 实现 | 向量接入 | Step 6 | `REQ-008`-`REQ-010`, `REQ-027` | `TEST-008` |
| CREATE | `infrastructure/knowledge/handler/KnowledgeIngestDeliveryHandler.java` | 无同类 | outbox handler 与租户恢复 | 异步摄取 | Step 7 | `REQ-004`-`REQ-007`, `REQ-016`, `REQ-026` | `TEST-009` |
| CREATE | `application/knowledge/**` | 只有 `application/research` | Command、Manage、容量服务、属性 | 用例 | Step 8 | `REQ-002`, `REQ-003`, `REQ-005`, `REQ_008`, `REQ-022` | `TEST-010` |
| CREATE | `adapter/knowledge/controller/KnowledgeBaseController.java` 与边界载体 | 只有 `adapter/research` | API-001..005 | 知识库接口 | Step 9 | `REQ-002` | `TEST-011` |
| CREATE | `adapter/knowledge/controller/KnowledgeDocumentController.java` 与边界载体 | 同上 | API-006..010 | 文档接口 | Step 10 | `REQ-003`, `REQ-005`, `REQ-007` | `TEST-012` |
| CREATE | `adapter/knowledge/controller/KnowledgeQaController.java`、`filter/KnowledgeTenantMdcFilter.java` 与边界载体 | 同上 | API-011、012 与租户过滤器 | 检索问答 | Step 11 | `REQ-011`-`REQ-014`, `REQ-022`, `REQ-025`, `REQ-027` | `TEST-013` |
| MODIFY | `definitions/.../architecture-docs/agent-multi-module-architecture.md` | 声明无数据库与迁移 | 描述 knowledge 域与迁移位置 | 文档 | Step 12 | `REQ-020` | `TEST-014` |

## 6. Prerequisites, Constraints, and Plan Clarifications

### 6.1 Repository and worktree baseline

- 分支 `docs/rag-starter-spec`，基线提交 `139c75772`，工作区干净。
- 仓库无 `AGENTS.md`。
- 组件 `egon-cola-component-rag-starter` 已实施并安装到本地仓库，其 artifact 可解析。
- 不得改动 `research` 域的任何生产文件；不得改动任何既有 Flyway 迁移（本项目此前没有迁移）。

### 6.2 Build, test, and environment prerequisites

| Concern | Exact command/source | Required state | Validation boundary |
| --- | --- | --- | --- |
| 组件已安装 | `./mvnw -B -ntp -f egon-cola-components/pom.xml install -DskipTests` | 本地仓库含 `egon-cola-component-rag-starter:5.4.0` | 跨模块 |
| 源码项目 | `./mvnw -B -ntp -f egon-cola-archetypes/source-projects/pom.xml clean install` | 六模块构建通过 | 模块 |
| 生成 | `./scripts/generate_archetypes.sh generate` | `.generated` 产出 agent archetype | 跨模块 |
| 生成校验 | `./scripts/check_archetypes.sh` | 生成物与源码一致 | 静态 |
| 生成 IT | `./mvnw -B -ntp -f egon-cola-archetypes/pom.xml -Pgenerated-archetypes clean verify` | 七个 archetype 的 verifier 全部通过 | 跨模块 |

### 6.3 Immutable constraints and approved decisions

- Rule 3 的 PO 例外：Spec B `§6.2.1`，用户已批准。
- 组件不建表、不跑 Flyway（组件 README 明示）：迁移由本项目创建并重编号。
- `enqueue` 必须在活跃事务中调用（组件契约）：上传路径必须 `@Transactional`。
- 向量表由 `PgVectorStore` 创建：Flyway 不得包含 `vector_store` 建表语句。
- 租户走 MDC 通道且不接入身份：`DEC-012`-`DEC-015`，来源推迟。

### 6.4 Plan Clarifications

| ID | Small implementation inference | Repository evidence | Why semantics are unchanged | Impact if wrong |
| --- | --- | --- | --- | --- |
| `PLAN-CLAR-001` | 迁移命名取 `V20260910_001__create_knowledge_schema.sql` 与 `V20260910_002__create_transactional_outbox_schema.sql` | `source-web` 的日期命名风格 | Spec B `ASM-001` 已给出同样的推断 | 重命名即可，未发布 |
| `PLAN-CLAR-002` | 仓储端口放在 `domain/knowledge/repository`，实现放在 `infrastructure/knowledge/service` | `source-web` 的 `repo/dao` 布局与 `AgentArchitectureTest` 的依赖方向 | 端口与实现分离是既有模式的具体落点 | 移动包即可 |
| `PLAN-CLAR-003` | 租户过滤器命名 `KnowledgeTenantMdcFilter`，与既有 `ResearchTraceFilter` 同包 | `adapter/filter` 既有两个过滤器 | 只影响命名 | 重命名即可 |
| `PLAN-CLAR-004` | 知识库状态枚举取 `ACTIVE`/`DELETED`，文档状态取 `PENDING`/`PROCESSING`/`SUCCEEDED`/`FAILED`/`DEAD` | Spec B `ASM-005` 与 `§10.6` | 与 Spec B 的表格一致 | 调整枚举值 |

## 7. Ordered File-by-file Implementation Steps

### Step 1 — 放开四道门禁并扩展打包清单

- Requirements: `REQ-017`, `REQ-018`, `REQ-019`
- Dependencies: `None`
- Baseline state: 源码项目 `clean verify` 通过；四条禁项分别禁止 `flyway`/`mybatis` 与基础设施模块的 `db` 目录；infrastructure 模块没有任何 resources 文件集。
- Observable outcome: 修订后的源码项目仍然 `clean verify` 通过，且 `flyway`/`mybatis` 不再被禁；生成产物包含 infrastructure 的 `src/main/resources` 内容。
- End state: 四处门禁修订完成，可容纳持久化代码与迁移资源；业务代码尚未新增。
- Test-first gate: `Not applicable` — 本 Step 只放宽既有断言与新增文件集，不引入行为；其证明是源码项目与生成 IT 仍然通过。
- Manual Checks: `MC-ARCH-001`, `MC-SCOPE-001`, `MC-TEST-001`
- Literal Rules: `Rule 11`
- Ordered files:

#### File 1 — `MODIFY egon-cola-source-agent-starter/src/test/java/.../architecture/AgentArchitectureTest.java`

- Purpose: 让 `flyway` 与 `mybatis` 在受控范围内可用，其余禁项保持不变。
- Symbols: `has_no_forbidden_integrations_or_source_web_business()` 的 `List.of(...)`。
- Repository evidence: 第 70-72 行的列表；本条断言扫描 `.xml`/`.java`（排除 `src/test`）。
- Dependencies and consumers: 每个 Step 的 `clean verify` 都会执行它。
- Why now: 不禁用则 Step 3 的迁移与 Step 5 的 MyBatis 依赖会立刻失败。
- Contract/signature changes: 列表由 9 项减为 7 项；新增一条 knowledge 依赖方向断言（`adapter` 仍不得依赖 `infrastructure`）。
- Input/output and state mapping: 无运行期映射。
- Error and edge behavior: 保留 `spring-boot-starter-jdbc`、`spring-boot-starter-data-redis`、`spring-boot-starter-amqp`、`spring-boot-starter-graphql`、`dubbo-spring-boot-starter`、`shardingsphere`、`egon-cola-source-web`。
- Standards impact: `MC-ARCH-001`（依赖方向不变）、`MC-SCOPE-001`、`MC-TEST-001`。
- Literal rule enforcement: `Rule 11` — 只改断言，不改包结构或模块集合。
- Implementation pseudocode:

```java
// 移除的两个 token
for (String forbidden : List.of("spring-boot-starter-jdbc", "spring-boot-starter-data-redis",
        "spring-boot-starter-amqp", "spring-boot-starter-graphql", "dubbo-spring-boot-starter",
        "shardingsphere", "egon-cola-source-web")) {   // flyway 与 mybatis 已移除
    assertFalse(content.toLowerCase().contains(forbidden), path + " contains " + forbidden);
}

// 新增：knowledge 域也遵守既有依赖方向
@Test
void keeps_knowledge_dependency_direction() {
    assertFalse(modulePom("adapter").contains("-infrastructure"));
    assertFalse(modulePom("application").contains("-infrastructure"));
}
```

- Verification contribution: `TEST-001`。
- After this file: `flyway`/`mybatis` 在该测试中可用；其余禁项仍生效。

#### File 2 — `MODIFY egon-cola-source-agent-starter/src/test/java/.../starter/AgentSourceContractTest.java`

- Purpose: 让 `flyway` 与 `mybatis` 在源码文本扫描中可用，其余 token 与全部结构断言保持不变。
- Symbols: 第 38 行的正则 `(?s).*\\b(fastjson|flyway|mybatis|redis|graphql|dubbo)\\b.*`。
- Repository evidence: 该正则扫全部 `src/main` 与 `pom.xml`；`redis` 仍须禁止（outbox 不需要 Redis）。
- Dependencies and consumers: 同 File 1。
- Why now: 同 File 1。
- Contract/signature changes: 正则改为 `(?s).*\\b(fastjson|redis|graphql|dubbo)\\b.*`；`PROFILE_KEYS`、`package-info` 与 sentinel 断言不变。
- Input/output and state mapping: 无。
- Error and edge behavior: `redis` 保留禁止，作为 outbox 不需要 Redis 的回归保护。
- Standards impact: `MC-ARCH-001`、`MC-SCOPE-001`、`MC-TEST-001`。
- Literal rule enforcement: `Rule 11`。
- Implementation pseudocode:

```java
// 原断言
assertFalse(allSourceText().matches("(?s).*\\b(fastjson|flyway|mybatis|redis|graphql|dubbo)\\b.*"));
// 修订后：移除 flyway 与 mybatis，保留其余四个以及全部结构断言
assertFalse(allSourceText().matches("(?s).*\\b(fastjson|redis|graphql|dubbo)\\b.*"));
assertFalse(allSourceText().contains("egon-cola-source-web"));
assertFalse(allSourceText().contains("BEGIN GENERATED"));
// PROFILE_KEYS 与 package-info 断言保持不变
assertEquals(PROFILE_KEYS, yamlKeys(Path.of("egon-cola-source-agent-starter/src/main/resources/application.yml")));
```

- Verification contribution: `TEST-001`。
- After this file: 文本扫描允许持久化依赖，其余不变。

#### File 3 — `MODIFY definitions/egon-cola-archetype-agent/src/test/resources/projects/basic/verify.groovy`

- Purpose: 让生成产物可以包含 Flyway、MyBatis 与 `db/migration` 资源。
- Symbols: `forbiddenDependencies`、`missing("${prefix}-infrastructure/src/main/resources/db")`、forbiddenToken 列表。
- Repository evidence: 第 78-80 行的禁项、第 129 行的 `missing(db)`、第 193 行的 token 列表。
- Dependencies and consumers: 生成项目 `clean verify` 时执行。
- Why now: 同 File 1。
- Contract/signature changes: **只做放开**——移除 `flyway-core`、`mybatis-plus-spring-boot3-starter`、`flyway` token 与 `missing(db)`。新增断言按"谁创建产物谁加断言"的原则放到后续 Step（依赖断言在 Step 2、迁移文件断言在 Step 3），否则 Step 1 之后生成 IT 会因为断言指向尚不存在的产物而变红，中间态不再可用。
- Input/output and state mapping: 无。
- Error and edge behavior: 断言失败即阻止发布，保持 fail-closed。
- Standards impact: `MC-ARCH-001`、`MC-DEP-001`、`MC-SCOPE-001`、`MC-TEST-001`。
- Literal rule enforcement: `Rule 11`。
- Implementation pseudocode:

```groovy
def forbiddenDependencies = [
    "spring-boot-starter-data-jpa", "spring-boot-starter-data-redis",
    "spring-boot-starter-amqp", "dubbo-spring-boot-starter",
    "spring-boot-starter-graphql", "egon-cola-organization-facade", "egon-cola-evaluation-facade"
]

// 迁移文件与依赖的存在性断言分别由 Step 3 与 Step 2 加入；本 Step 只放开禁项。

["spring-boot-starter-data-jpa", "jakarta.persistence", "jparepository", "redis", "graphql", "dubbo", "fastjson"].each { forbiddenToken ->
    assert !generatedSourceText.contains(forbiddenToken)
}
```

- Verification contribution: `TEST-002`。
- After this file: 生成侧放开就位；新增断言留待其产物出现的 Step。

#### File 4 — `MODIFY definitions/egon-cola-archetype-agent/src/main/resources/META-INF/maven/archetype-metadata.xml`

- Purpose: 让 infrastructure 模块的 `src/main/resources`（含 `.sql`）随生成打包。
- Symbols: infrastructure 模块的 `<fileSets>`。
- Repository evidence: 现有 infrastructure 模块只有 `src/main/java`、`pom.xml`、`src/test/java` 三个文件集；starter 模块有 `src/main/resources` 但只含 `**/*.yml`。
- Dependencies and consumers: 生成器与生成 IT。
- Why now: 同 File 1；不加则迁移文件根本不进生成产物。
- Contract/signature changes: infrastructure 模块新增一个 `filtered="true" packaged="true"` 文件集，`directory=src/main/resources`，`includes` 含 `**/*.sql` 与 `**/*.yml`。
- Input/output and state mapping: 无运行期映射。
- Error and edge behavior: 文件集缺失会让 File 3 的存在性断言失败，fail-closed。
- Standards impact: `MC-SCOPE-001`、`MC-TEST-001`。
- Literal rule enforcement: `Rule 11`。
- Implementation pseudocode:

```xml
<!-- infrastructure 模块块；id/dir/name 沿用其它模块的同一写法 -->
    <fileSets>
        <!-- 既有三个文件集保持不变 -->
        <fileSet filtered="true" packaged="true" encoding="UTF-8">
            <directory>src/main/resources</directory>
            <includes>
                <include>**/*.sql</include>
                <include>**/*.yml</include>
            </includes>
        </fileSet>
    </fileSets>
</module>
```

- Verification contribution: `TEST-003`。
- After this file: 生成产物可承载迁移资源。

- Validation working directory: `/Users/mario/SelfProject/Egon-COLA`
- Verification command: `./mvnw -B -ntp -f egon-cola-archetypes/source-projects/pom.xml clean install` 然后 `./scripts/generate_archetypes.sh generate && ./scripts/check_archetypes.sh` 然后 `./mvnw -B -ntp -f egon-cola-archetypes/pom.xml -Pgenerated-archetypes clean verify`
- Expected result: 三条命令均退出码 0；源码项目与七个生成产物的 verifier 全部通过（本 Step 只放开禁项，不新增指向未创建产物的断言，因此中间态保持全绿）。
- Failure returns to: File 3（断言与实现不一致）或 File 4（文件集路径）。
- Completion criteria: `REQ-017`-`REQ-019` 的放开部分有证据；其余禁项仍生效。
- Rollback: 回退本 Step 四个文件。
- Commit paths: `egon-cola-source-agent-starter/src/test/java/.../architecture/AgentArchitectureTest.java`; `egon-cola-source-agent-starter/src/test/java/.../starter/AgentSourceContractTest.java`; `definitions/egon-cola-archetype-agent/src/test/resources/projects/basic/verify.groovy`; `definitions/egon-cola-archetype-agent/src/main/resources/META-INF/maven/archetype-metadata.xml`
- Commit: `test(agent-archetype): allow flyway and mybatis behind the existing guards`

### Step 2 — 登记依赖

- Requirements: `REQ-008`, `REQ-009`, `REQ-010`, `REQ-015`, `REQ-023`
- Dependencies: Step 1
- Baseline state: 六模块可构建；不存在 rag、outbox、pgvector、mybatis、flyway 任何坐标。
- Observable outcome: 六模块解析全部新依赖并构建通过；依赖出现在正确模块而非补齐到根。
- End state: 依赖登记完成，无生产代码变化。
- Test-first gate: `Not applicable` — 只改 POM，无可测行为；证明是构建与 `verify.groovy` 的依赖断言。
- Manual Checks: `MC-DEP-001`, `MC-REUSE-001`, `MC-SCOPE-001`, `MC-TEST-001`
- Literal Rules: `Rule 11`
- Ordered files:

#### File 1 — `MODIFY egon-cola-source-agent/pom.xml`

- Purpose: 声明新依赖的版本来源与模块管理。
- Symbols: `<dependencyManagement>` 与 `<properties>`。
- Repository evidence: 根 POM 已导入 `spring-ai-bom:${spring-ai.version}` 并管理六个内部模块。
- Dependencies and consumers: 六个模块 POM 消费。
- Why now: 模块 POM 引用前必须可解析。
- Contract/signature changes: `spring-ai-bom` 已覆盖 `spring-ai-pgvector-store`，无需新增版本属性。
- Input/output and state mapping: 无运行期映射。
- Error and edge behavior: 版本缺失导致解析失败。
- Standards impact: `MC-DEP-001`、`MC-SCOPE-001`、`MC-TEST-001`。
- Literal rule enforcement: `Rule 11`。
- Implementation pseudocode:

```xml
<!-- 本 Step 只在确有需要时才改；spring-ai-bom 已覆盖 spring-ai-pgvector-store，通常无需新增属性 -->
<dependencyManagement>
    <dependencies>
        <!-- 既有：spring-ai-bom、六个内部模块 -->
        <!-- 仅当某个新坐标不被任一既有 BOM 管理时才追加一条 -->
    </dependencies>
</dependencyManagement>
<!-- 判定方式：先构建；若依赖解析失败且报 missing version，再补管理条目 -->
```

- Verification contribution: 构建门。
- After this file: 无变化时保持原样。

#### File 2 — `MODIFY egon-cola-source-agent-domain/pom.xml`

- Purpose: 让 `knowledge` 域获得 `EgonModel`/`EgonColaIService` 基线。
- Symbols: `<dependencies>` 新增 `egon-cola-component-common-mybatis-plus-spring-boot-starter`。
- Repository evidence: `source-web-domain/pom.xml` 第 19-22 行以 `top.egon` 无版本声明同一坐标（`EVD-008`）。
- Dependencies and consumers: domain 的 BO 与仓储契约。
- Why now: PO 与仓储实现依赖其基类。
- Contract/signature changes: 新增一条依赖。
- Input/output and state mapping: 无。
- Error and edge behavior: 缺依赖则 PO 无法继承 `EgonModel`。
- Standards impact: `MC-DEP-001`、`MC-REUSE-001`。
- Literal rule enforcement: `Rule 11`。
- Implementation pseudocode:

```xml
<dependency>
    <groupId>top.egon</groupId>
    <artifactId>egon-cola-component-common-mybatis-plus-spring-boot-starter</artifactId>
</dependency>
```

- Verification contribution: `verify.groovy` 的 domain 依赖断言。
- After this file: domain 具备持久化基线类型。

#### File 3 — `MODIFY egon-cola-source-agent-infrastructure/pom.xml`

- Purpose: 让基础设施获得引擎、outbox、向量库与迁移能力。
- Symbols: `<dependencies>` 新增四条。
- Repository evidence: 现有 infra 依赖 `egon-cola-component-agent-flow-starter`、`spring-ai-mcp`、`mapstruct-plus-spring-boot-starter`。
- Dependencies and consumers: gateway、handler、装配与迁移。
- Why now: 与 File 2 同批。
- Contract/signature changes: 新增 `egon-cola-component-rag-starter`、`egon-cola-component-transactional-outbox-starter`、`spring-ai-pgvector-store`、`flyway-core` 与 `org.postgresql:postgresql`。
- Input/output and state mapping: 无。
- Error and edge behavior: 缺 `spring-ai-pgvector-store` 则向量装配无法编译。
- Standards impact: `MC-DEP-001`、`MC-REUSE-001`。
- Literal rule enforcement: `Rule 11`。
- Implementation pseudocode:

```xml
<dependency><groupId>top.egon</groupId><artifactId>egon-cola-component-rag-starter</artifactId></dependency>
<dependency><groupId>top.egon</groupId><artifactId>egon-cola-component-transactional-outbox-starter</artifactId></dependency>
<dependency><groupId>org.springframework.ai</groupId><artifactId>spring-ai-pgvector-store</artifactId></dependency>
<dependency><groupId>org.flywaydb</groupId><artifactId>flyway-core</artifactId></dependency>
<dependency><groupId>org.postgresql</groupId><artifactId>postgresql</artifactId><scope>runtime</scope></dependency>
```

- Verification contribution: `verify.groovy` 的 infra 依赖与运行时库断言。
- After this file: infrastructure 具备全部能力依赖。

#### File 4 — `MODIFY egon-cola-source-agent-application/pom.xml` 与 `egon-cola-source-agent-adapter/pom.xml` 与 `egon-cola-source-agent-starter/pom.xml`

- Purpose: 确认三份 POM 均无需新增；数据源能力由 MyBatis Plus 组件传递，不显式声明 `spring-boot-starter-jdbc`。
- Symbols: application、adapter、starter 三份 POM 均无变化。
- Repository evidence: `mybatis-plus-spring-boot3-starter:3.5.17` 的 POM 以 `compile` scope 声明 `spring-boot-starter-jdbc`，DataSource 自动配置随之生效；Spec `§6.1` 明确 `spring-boot-starter-jdbc` 被 `EVD-003` 禁止、关系型持久化选择 `egon-cola-component-common-mybatis-plus-spring-boot-starter`；`egon-cola-archetypes/source-projects` 下无任何 POM 声明该坐标。
- Dependencies and consumers: Flyway 与 MyBatis Plus 的自动配置经由传递依赖获得 DataSource。
- Why now: 与前三份同批。
- Contract/signature changes: 无。
- Input/output and state mapping: 无。
- Error and edge behavior: 若显式声明 `spring-boot-starter-jdbc`，`AgentArchitectureTest` 的 `EVD-003` 断言与 Spec 决策会立即冲突。
- Standards impact: `MC-DEP-001`。
- Literal rule enforcement: `Rule 11`。
- Implementation pseudocode:

```xml
<!-- 三份 POM 均无变化：jdbc 能力由 mybatis-plus 组件传递，且被 EVD-003 禁止显式声明 -->
```

- Verification contribution: 依赖解析与 `AgentArchitectureTest` 的禁项断言仍通过。
- After this file: Step 2 完成。

- Validation working directory: `/Users/mario/SelfProject/Egon-COLA`
- Verification command: `./mvnw -B -ntp -f egon-cola-archetypes/source-projects/pom.xml clean install`；全反应堆命令受 `egon-cola-source-light` 的 `NativeRpcConfigurationTest` 预先存在的红灯（`c7f09dd5a` 的 `tianquan.shoubing` 前缀与 yml 的 `tianquan-shoubing` 键不一致，与本 Step 无关）阻塞，实施期以 `./mvnw -B -ntp -f egon-cola-archetypes/source-projects/egon-cola-source-agent/pom.xml clean install` 作为等价门禁，并在 Step 记录中标注偏差。
- Expected result: 退出码 0；六模块构建通过；`verify.groovy` 新增的四条依赖存在性断言在生成 IT 中通过。
- Failure returns to: File 2-3 的坐标或 scope。
- Completion criteria: 依赖可解析且构建通过；`research` 域未改动。
- Rollback: 回退本 Step 的 POM 与 `verify.groovy` 断言。
- Commit paths: `egon-cola-source-agent-domain/pom.xml`; `egon-cola-source-agent-infrastructure/pom.xml`; `definitions/egon-cola-archetype-agent/src/test/resources/projects/basic/verify.groovy`（根 POM 与 application/adapter/starter 三份 POM 预期无变化，不进入提交）
- Commit: `build(agent-archetype): add knowledge persistence and rag dependencies`

在实施期修正（方言插件）：infrastructure 另需 `org.flywaydb:flyway-database-postgresql`，与 `flyway-core` 成对声明，版本由 Boot BOM 管理。Flyway 10 起数据库支持按方言拆包，`flyway-core:11.15.0` 的 jar 内不含任何 PostgreSQL `DatabaseType`（只有 `PgpassFileReader`），缺该插件时 PostgreSQL 迁移在启动期报 `Unsupported Database`；仓库内 `egon-cola-source-light` 与 `egon-cola-source-service` 两个 PostgreSQL 源码项目也是成对声明。Spec B `§11`/`§16` 的依赖清单未列该坐标，属清单遗漏。

实施期前置修正（共享组件）：依赖一挂上 classpath，全部上下文测试即在 `EgonColaMybatisPlusAutoConfiguration` 上失败。该组件的 `egonColaModelValidationUtils` 与 `egonColaMetaObjectHandler` 按类型注入 `ValidationUtils` 与 `java.time.Clock`，而 agent-flow 组件提供 `agentFlowValidationUtils`/`agentFlowClock`、本项目提供 `agentValidationUtils`/`agentClock`，双双构成 `NoUniqueBeanDefinitionException`——即该组件与 agent-flow 无法共存，而 Spec B 正要求二者同时存在。已在组件侧修复（提交 `0ad56b21b`）：`ValidationUtils` 改为具名解析（与 agent-flow 组件的写法一致），`Clock` 改经 `ObjectProvider` 取唯一（或 `@Primary`）候选、多候选时退回 `systemUTC`，保持"宿主时钟可覆盖"的既有语义；组件模块 49 个测试通过（新增 3 个多 Bean 回归用例）。该修复是 Step 2 起所有上下文测试的前提，不属于本项目 six-module 的源码改动。

### Step 3 — 迁移与四 profile 配置

- Requirements: `REQ-010`, `REQ-015`, `REQ-016`, `REQ-023`, `REQ-025`
- Dependencies: Step 2
- Baseline state: 无 `db` 目录；四个 profile 无 datasource、Flyway、RAG、outbox、MyBatis Plus 键。
- Observable outcome: 迁移可被 Flyway 执行；四个 profile 键集同构；`vector_store` 不在迁移中。
- End state: 两张业务表、outbox 表、`vector` 扩展与全部配置就位。
- Test-first gate: `Required` — `KnowledgeSchemaMigrationTest` 首次运行时因迁移文件不存在而失败。
- Manual Checks: `MC-CONFIG-001`, `MC-DEP-001`, `MC-SCOPE-001`, `MC-TEST-001`, `MC-TIME-001`
- Literal Rules: `Rule 7`, `Rule 10`, `Rule 11`
- Ordered files:

#### File 1 — `CREATE ...-infrastructure/src/test/java/.../infrastructure/knowledge/KnowledgeSchemaMigrationTest.java`

- Purpose: 定义"两条迁移存在且向量表不在其中"这一契约。
- Symbols: `declares_the_knowledge_and_outbox_migrations()`、`leaves_the_vector_table_to_the_vector_store()`、`indexes_the_tenant_scoped_access_paths()`。
- Repository evidence: 既有 source 项目无迁移测试；断言风格取自 `AgentSourceContractTest`。
- Dependencies and consumers: 读取 `src/main/resources/db/migration`。
- Why now: 迁移是本 Step 的产物，先定契约。
- Contract/signature changes: 无生产契约。
- Input/output and state mapping: 迁移目录 → 文件与内容断言。
- Error and edge behavior: 文件缺失或含 `vector_store` 建表即失败。
- Standards impact: `MC-CONFIG-001`、`MC-SCOPE-001`、`MC-TEST-001`。
- Literal rule enforcement: `Rule 11`。
- Implementation pseudocode:

```java
@Test void declares_the_knowledge_and_outbox_migrations() {
    assertThat(migrationFiles()).containsExactly(
        "V20260910_001__create_knowledge_schema.sql", "V20260910_002__create_transactional_outbox_schema.sql");
}
@Test void leaves_the_vector_table_to_the_vector_store() {
    assertThat(read("V20260910_001__create_knowledge_schema.sql"))
        .contains("CREATE EXTENSION IF NOT EXISTS vector").doesNotContain("vector_store");
}
```

- Verification contribution: GREEN 后锁定迁移契约。
- After this file: 测试因文件缺失而 RED。

#### File 2 — `CREATE ...-infrastructure/src/main/resources/db/migration/V20260910_001__create_knowledge_schema.sql`

- Purpose: 创建扩展与两张业务表。
- Symbols: `CREATE EXTENSION`、`knowledge_base`、`knowledge_document`、两个索引与一个唯一索引、三个 check 约束。
- Repository evidence: 组件自带 DDL 的小写风格（`EVD-016`）；`EgonModel` 提供的列名与类型（`EVD-010`）。
- Dependencies and consumers: 全部 PO/DAO。
- Why now: schema 是持久化实现的前提。
- Contract/signature changes: 新增两张表与索引。
- Input/output and state mapping: 见 Spec B `§11.2.1` 与 `§11.2.2` 的列设计表。
- Error and edge behavior: 迁移失败即启动失败。
- Standards impact: `MC-TIME-001`（`timestamptz`）`、`MC-SCOPE-001`。
- Literal rule enforcement: `Rule 10`（时间列用 `timestamp with time zone`）、`Rule 11`。
- Implementation pseudocode:

```sql
create extension if not exists vector;

create table knowledge_base (
    id bigint generated by default as identity primary key,
    tenant_id bigint not null default 0,
    create_user_id varchar(64), create_time timestamptz not null,
    update_user_id varchar(64), update_time timestamptz not null,
    is_deleted boolean not null default false,
    code varchar(64) not null, name varchar(128) not null, description varchar(512),
    embedding_model varchar(32) not null, chunk_strategy varchar(32) not null, chunk_config jsonb not null,
    status varchar(16) not null default 'ACTIVE',
    constraint ck_knowledge_base_chunk_strategy check (chunk_strategy in ('TOKEN','MARKDOWN_HEADING','RECURSIVE')),
    constraint ck_knowledge_base_status check (status in ('ACTIVE','DELETED'))
);
create unique index uk_knowledge_base_tenant_code on knowledge_base(tenant_id, lower(code)) where is_deleted = false;
create index idx_knowledge_base_tenant_created on knowledge_base(tenant_id, create_time desc, id desc) where is_deleted = false;

create table knowledge_document (
    /* 同结构的审计列与 is_deleted */
    id bigint generated by default as identity primary key,
    knowledge_base_id bigint not null,
    display_name varchar(255) not null, file_name varchar(255), mime_type varchar(128),
    size_bytes bigint not null, content_hash char(64) not null,
    storage_type varchar(16) not null, storage_key varchar(512) not null,
    content text,
    status varchar(16) not null default 'PENDING',
    chunk_count integer not null default 0, attempt_count integer not null default 0,
    error_code varchar(64), error_message varchar(512),
    constraint ck_knowledge_document_status check (status in ('PENDING','PROCESSING','SUCCEEDED','FAILED','DEAD'))
);
create index idx_knowledge_document_tenant_base_created
    on knowledge_document(tenant_id, knowledge_base_id, create_time desc, id desc) where is_deleted = false;
```

- Verification contribution: `KnowledgeSchemaMigrationTest` 的 GREEN。
- After this file: 表结构就位。

#### File 3 — `CREATE ...-infrastructure/src/main/resources/db/migration/V20260910_002__create_transactional_outbox_schema.sql`

- Purpose: 创建 outbox 表，结构必须与组件期望逐字一致。
- Symbols: `egon_cola_outbox_message` 与五个索引。
- Repository evidence: 组件自带 `V1__create_transactional_outbox_schema.sql`（`EVD-016`）；其 README 要求复制到消费方序列并重编号（`EVD-017`）。
- Dependencies and consumers: outbox 组件的存储与启动期 schema 校验。
- Why now: handler 可用前必须能入队。
- Contract/signature changes: 新增一张表；**逐字副本，不新增、不修改任何列**。
- Input/output and state mapping: 无。
- Error and edge behavior: 结构不符则启动期 fail-fast。
- Standards impact: `MC-SCOPE-001`、`MC-DEP-001`。
- Literal rule enforcement: `Rule 11`。
- Implementation pseudocode:

```sql
-- 组件自带 DDL 的逐字副本，仅文件版本号按本项目 db/migration 序列重编号
create table egon_cola_outbox_message ( /* 与组件 V1 完全一致 */ );
create unique index uk_outbox_message_id on egon_cola_outbox_message(message_id);
create unique index uk_outbox_idempotency_key on egon_cola_outbox_message(idempotency_key) where idempotency_key is not null;
create index idx_outbox_claim on egon_cola_outbox_message(next_attempt_at, id) where status in ('PENDING','RETRY_WAIT');
create index idx_outbox_reclaim on egon_cola_outbox_message(locked_until, id) where status = 'PROCESSING';
create index idx_outbox_cleanup on egon_cola_outbox_message(completed_at, id) where status = 'SUCCEEDED';
```

- Verification contribution: 启动期 schema 校验与 `KnowledgeIngestDeliveryHandlerTest`。
- After this file: 建表迁移齐备。

#### File 4 — `MODIFY ...-starter/src/main/resources/{application.yml,application-dev.yml,application-test.yml,application-prod.yml}`

- Purpose: 四 profile 同构新增数据源、Flyway、RAG、outbox、MyBatis Plus 与运行时键。
- Symbols: `spring.datasource.*`、`spring.flyway.*`、`egon.cola.component.rag.*`、`egon.cola.component.transactional-outbox.*`、`egon.cola.component.mybatis-plus.*`、`agent.knowledge.*`。
- Repository evidence: 四个文件当前键集合相同（`EVD-014`）；`AgentSourceContractTest` 断言 `PROFILE_KEYS` 一致。
- Dependencies and consumers: 全部自动配置。
- Why now: 迁移执行需要 datasource 与 Flyway locations。
- Contract/signature changes: 新增键；值随 profile 不同但键结构一致。
- Input/output and state mapping: 环境变量 → 数据源与模型凭据。
- Error and edge behavior: 缺键或键不一致即测试失败。
- Standards impact: `MC-CONFIG-001`、`MC-DEP-001`、`MC-SCOPE-001`。
- Literal rule enforcement: `Rule 7`（四文件同构）、`Rule 11`。
- Implementation pseudocode:

```yaml
spring:
  datasource:
    url: ${AGENT_DB_URL:}
    username: ${AGENT_DB_USERNAME:}
    password: ${AGENT_DB_PASSWORD:}
  flyway:
    enabled: ${AGENT_FLYWAY_ENABLED:true}
    locations: classpath:db/migration
egon:
  cola:
    component:
      rag:
        enabled: true
        dimensions: 1536
        vector-store-bean-name: knowledgeRagVectorStore
        embedding-models:
          openai-compatible: { embedding-model-bean-name: knowledgeEmbeddingModel }
      transactional-outbox:
        enabled: true
      mybatis-plus:
        tenant-id:
          ignored-tables: []   # 实施期修正：知识表必须留在拦截器作用域内（见下方租户作用域修正）
agent:
  knowledge:
    tenant: { default-id: 0 }
    runtime: { qa-max-concurrent: 4, max-upload-bytes: 20971520, max-documents-per-base: 10000, qa-max-duration: PT2M }
```

- Verification contribution: Step 3 的配置断言与 Step 12 的键一致性测试。
- After this file: 配置就位，Step 3 完成。

- Validation working directory: `/Users/mario/SelfProject/Egon-COLA`
- Verification command: `./mvnw -B -ntp -f egon-cola-archetypes/source-projects/egon-cola-source-agent/pom.xml clean verify` 然后 `./scripts/generate_archetypes.sh generate && ./scripts/check_archetypes.sh`
- Expected result: 退出码 0；`KnowledgeSchemaMigrationTest` 通过；生成产物含两条迁移。
- Failure returns to: File 2（DDL 与测试断言不符）、File 4（键不一致）。
- Completion criteria: `REQ-015`、`REQ-016`、`REQ-025` 的列与键部分有证据。
- Rollback: 回退四个文件；表尚未在任何环境创建。
- Commit paths: `...-infrastructure/src/test/java/.../infrastructure/knowledge/KnowledgeSchemaMigrationTest.java`; `...-infrastructure/src/main/resources/db/migration/V20260910_001__create_knowledge_schema.sql`; `...-infrastructure/src/main/resources/db/migration/V20260910_002__create_transactional_outbox_schema.sql`; `...-starter/src/main/resources/{application.yml,application-dev.yml,application-test.yml,application-prod.yml}`；实施期增补：`...-starter/pom.xml`（H2 测试替身）与 `definitions/.../verify.groovy`（迁移文件与 `BOOT-INF/lib` 断言）
- Commit: `feat(agent-archetype): create the knowledge schema and configuration`

在实施期修正：本 Step 同时补上 Step 1 尚未加入的 `verify.groovy` 迁移文件断言与 `BOOT-INF/lib` 运行时库断言中依赖迁移的部分；依赖存在性断言在 Step 2 加入。

在实施期修正（租户作用域）：本 Step 原伪代码把两张 knowledge 表写进 `tenant-id.ignored-tables`，取自 Spec B `EVD-010` 的"排除出 MDC 驱动的租户拦截，改用显式租户参数"。该句是 Spec B 反转前的陈旧文字：Spec B 的 `§20.6`（第 3261 行）记录了用户 2026-09-10 12:25 "走 MDC 不走显式参数"的澄清，`DEC-012` 据此反转为"MDC 通道 + 租户拦截器自动作用域，业务代码不传租户参数"，随后 `ASM-006` 明确要求拦截器"对两张 knowledge 表**生效**（不使用 `tenant-id.ignored-tables` 排除）"，`§11.2.1` 的索引理由也写明租户谓词"由 MyBatis Plus 的租户拦截器自动注入（`DEC-012`），应用代码里不存在它"，本 Plan 的 Step 5 同样要求"查询不写租户谓词（拦截器注入）"。因此四 profile 一律取 `ignored-tables: []`（与仓库内 light/light-open/web-open/service-open 四个源码项目一致）；被排除在拦截器外反而会让 `scopes_every_query_by_tenant()` 失去实现基础。**待办（用户侧）**：Spec B 里同属反转前陈旧文字的还有第 58 行（`EVD-010` 证据行的"排除……改用显式租户参数"）、第 111 行（"租户列与显式租户参数在本版落地"）、第 2645 行（`§11.2.2` 的"租户来自显式参数而非 MDC"）与第 3225 行（"建立了显式租户作用域"）；它们需按 `DEC-012`/`REQ-025` 改写，属 Spec B 自身的修订，不在本 Plan 的提交范围内。

在实施期修正（离线数据源）：Step 2 的依赖一落地，缺少连接串就让 `DeepResearchApplicationTest` 等三个上下文测试变红，而数据源要到本 Step 才配置。为使 Step 2 与 Step 3 作为同一个绿灯单元收口，`...-starter/pom.xml` 新增 `com.h2database:h2`（`test` 作用域，版本由 Boot BOM 管理，dev/prod 仍因缺连接串而启动失败），`application-test.yml` 以 `jdbc:h2:mem:agent_knowledge;MODE=PostgreSQL` 顶替真实库，并在该 profile 关闭 Flyway、RAG 与 outbox。实测 H2 无法执行 `create extension if not exists vector`、`jsonb`、部分索引与 `lower(...)` 表达式索引，故离线环境不能真实建表：Spec B `TEST-026`/`TEST-032` 中"运行真实迁移并观察建表/维度"的部分不可离线验证，迁移契约改由 `KnowledgeSchemaMigrationTest` 的文本断言守住，真实建表与维度校验留待有 PostgreSQL 的运行期验收。因此本 Step 的提交路径另含 `...-starter/pom.xml`（H2 替身）与 `verify.groovy`（迁移文件与 `BOOT-INF/lib` 断言）。

### Step 4 — domain：knowledge 领域词汇与端口

- Requirements: `REQ-001`, `REQ-006`, `REQ-023`
- Dependencies: Step 2
- Baseline state: `domain` 只有 `research` 包。
- Observable outcome: 领域类型具备完整词汇且不含任何框架导入。
- End state: BO、枚举、端口与仓储契约就位；无实现。
- Test-first gate: `Required` — 域类型不存在。
- Manual Checks: `MC-ARCH-001`, `MC-NAME-001`, `MC-MODEL-001`, `MC-SCOPE-001`, `MC-TEST-001`
- Literal Rules: `Rule 1`, `Rule 3`, `Rule 11`
- Ordered files:

#### File 1 — `CREATE ...-domain/src/test/java/.../domain/knowledge/KnowledgeDomainTest.java`

- Purpose: 锁定租户一致性、状态机合法迁移与 BO 归一化。
- Symbols: `keeps_document_status_transitions_legal()`、`normalizes_identifiers()`、`rejects_unknown_status()`。
- Repository evidence: `DeepResearchDomainTest` 的断言风格。
- Dependencies and consumers: 消费 domain 的 BO 与枚举。
- Why now: 领域契约先定。
- Contract/signature changes: 引入全部 domain 类型。
- Input/output and state mapping: 见 Spec B `§10.6` 的状态机表。
- Error and edge behavior: 非法迁移抛 `KnowledgeApplicationException`。
- Standards impact: `MC-NAME-001`、`MC-MODEL-001`、`MC-PATTERN-001`、`MC-SCOPE-001`、`MC-TEST-001`。
- Literal rule enforcement: `Rule 1`、`Rule 3`、`Rule 9`、`Rule 11`。
- Implementation pseudocode:

```java
@Test void keeps_document_status_transitions_legal() {
    assertThat(DocumentIngestStatusEnum.PENDING.canTransitionTo(SUCCEEDED)).isFalse();
    assertThat(DocumentIngestStatusEnum.PROCESSING.canTransitionTo(SUCCEEDED)).isTrue();
}
@Test void normalizes_identifiers() {
    assertThat(KnowledgeBaseBO.create(" product-docs ", "名称", null, "openai", config(), 0L).code())
        .isEqualTo("product-docs");
}
```

- Verification contribution: `TEST-006`。
- After this file: 编译失败（域类型缺失）。

#### File 2 — `CREATE .../domain/knowledge/model/{KnowledgeBaseBO,KnowledgeDocumentBO,KnowledgeChunkBO,KnowledgeChunkConfigBO}.java`

- Purpose: 领域载体，含冻结配置与状态机入口。
- Symbols: 四个 record 与紧凑构造器。
- Repository evidence: `DeepResearchTaskBO` 的 record 写法。
- Dependencies and consumers: application 与 infrastructure。
- Why now: 全部下游的类型前提。
- Contract/signature changes: 新增公开载体；**不携带审计列**（属 PO）。
- Input/output and state mapping: 字段见 Spec B `§10.3`。
- Error and edge behavior: 空白标识、负计数与越界参数抛 `IllegalArgumentException`；非法状态迁移由 `canTransitionTo` 返回 `false` 表达（见下方实施期修正）。
- Standards impact: `MC-NAME-001`（`BO` 后缀）、`MC-MODEL-001`（record + 紧凑构造器）、`MC-SCOPE-001`。
- Literal rule enforcement: `Rule 1`、`Rule 3`、`Rule 10`（`Instant` 时间字段）、`Rule 11`。
- Implementation pseudocode:

```java
public record KnowledgeBaseBO(Long knowledgeBaseId, Long tenantId, String code, String name,
                              String description, String embeddingModel,
                              ChunkingStrategyEnum chunkStrategy, KnowledgeChunkConfigBO chunkConfig,
                              KnowledgeBaseStatusEnum status, Instant createdAt, Instant updatedAt) {
    public KnowledgeBaseBO { /* trim、非空、范围与策略—参数配合校验 */ }
    public static KnowledgeBaseBO create(String code, String name, String description,
                                         String embeddingModel, ChunkingStrategyEnum chunkStrategy,
                                         KnowledgeChunkConfigBO chunkConfig, Long tenantId) { /* 未落库形态 */ }
}

public record KnowledgeChunkConfigBO(int maxTokensPerChunk, int overlapTokens, int minChunkChars,
                                     List<Integer> headingLevels) { /* 仅参数，不含策略 */ }
```

- Verification contribution: `TEST-006`。
- After this file: 载体就绪。

#### File 3 — `CREATE .../domain/knowledge/model/{DocumentIngestStatusEnum,KnowledgeBaseStatusEnum,ChunkingStrategyEnum}.java`

- Purpose: 关闭的取值集合，取代字符串状态与策略分派。
- Symbols: 三个枚举，文档状态含 `canTransitionTo`。
- Repository evidence: `ResearchStageEnum` 的既有枚举用法。
- Dependencies and consumers: BO、Manage、handler。
- Why now: 与载体同批。
- Contract/signature changes: 只增不改。
- Input/output and state mapping: 文档状态与 outbox 状态一一映射（Spec B `ASM-005`）。
- Error and edge behavior: 未知值由反序列化与管理层拒绝。
- Standards impact: `MC-NAME-001`（`Enum` 后缀）、`MC-PATTERN-001`、`MC-SCOPE-001`。
- Literal rule enforcement: `Rule 1`、`Rule 9`、`Rule 11`。
- Implementation pseudocode:

```java
public enum DocumentIngestStatusEnum {
    PENDING, PROCESSING, SUCCEEDED, FAILED, DEAD;
    public boolean isTerminal() { return this == SUCCEEDED || this == FAILED || this == DEAD; }
    public boolean canTransitionTo(DocumentIngestStatusEnum next) { /* §10.6 的合法迁移 */ }
}
```

- Verification contribution: `TEST-006`。
- After this file: 枚举就绪。

#### File 4 — `CREATE .../domain/knowledge/gateway/KnowledgeVectorGateway.java`

- Purpose: 技术中立的向量检索与写入端口。
- Symbols: `List<KnowledgeChunkBO> retrieve(String collectionId, String logicalModelName, String query, int topK, Map<String,String> attributes)`。
- Repository evidence: `DeepResearchAgentGateway` 的端口写法；`AgentArchitectureTest` 禁止 domain 出现 `org.springframework.ai`（`EVD-002`）。
- Dependencies and consumers: 由 infrastructure 实现；由 application 消费。
- Why now: 与仓储契约同批。
- Contract/signature changes: 新增端口。
- Input/output and state mapping: 参数 → 带分分块。
- Error and edge behavior: 依赖失败抛 `KnowledgeApplicationException`。
- Standards impact: `MC-ARCH-001`（domain 无框架）、`MC-NAME-001`（`Gateway`）、`MC-PATTERN-001`（端口与适配器）。
- Literal rule enforcement: `Rule 1`、`Rule 9`、`Rule 11`。
- Implementation pseudocode:

```java
public interface KnowledgeVectorGateway {
    List<KnowledgeChunkBO> retrieve(String collectionId, String logicalModelName, String query, int topK,
                                    Map<String, String> attributes);
}
```

- Verification contribution: `TEST-008`。
- After this file: 端口就绪。

#### File 5 — `CREATE .../domain/knowledge/repository/{KnowledgeBaseRepository,KnowledgeDocumentRepository}.java`

- Purpose: 持久化端口，隔离 MyBatis 类型。
- Symbols: 查询、插入、更新、软删与分页方法。
- Repository evidence: `source-web` 的 `repo/dao` 布局；application 不得依赖 MyBatis。
- Dependencies and consumers: 由 infrastructure 的仓储实现实现。
- Why now: 与载体同批。
- Contract/signature changes: 新增端口；方法**不含租户参数**（`REQ-025`）。
- Input/output and state mapping: 标识 → BO。
- Error and edge behavior: 缺失返回空 `Optional`。
- Standards impact: `MC-NAME-001`（`Repository`）、`MC-PATTERN-001`、`MC-SCOPE-001`。
- Literal rule enforcement: `Rule 1`、`Rule 9`、`Rule 11`。
- Implementation pseudocode:

```java
public interface KnowledgeBaseRepository {
    KnowledgeBaseBO insert(KnowledgeBaseBO knowledgeBase);
    Optional<KnowledgeBaseBO> findById(Long knowledgeBaseId);
    List<KnowledgeBaseBO> page(int offset, int size, String keyword, String embeddingModel);
    long count(String keyword, String embeddingModel);
    void updateNameAndDescription(Long knowledgeBaseId, String name, String description);
    void softDelete(Long knowledgeBaseId);
}

public interface KnowledgeDocumentRepository {
    KnowledgeDocumentBO insert(KnowledgeDocumentBO document);
    Optional<KnowledgeDocumentBO> findById(Long documentId);
    List<KnowledgeDocumentBO> findByIds(Collection<Long> documentIds);      // 引用解析 displayName
    List<KnowledgeDocumentBO> page(Long knowledgeBaseId, int offset, int size,
                                   DocumentIngestStatusEnum status, String keyword);
    long count(Long knowledgeBaseId, DocumentIngestStatusEnum status, String keyword);
    long countByKnowledgeBaseId(Long knowledgeBaseId);
    boolean markProcessing(Long documentId, int attemptCount);               // PENDING -> PROCESSING
    boolean markSucceeded(Long documentId, int chunkCount);                  // PROCESSING -> SUCCEEDED
    boolean markRetryPending(Long documentId, int attemptCount, String errorCode, String errorMessage);
    boolean markDead(Long documentId, String errorCode, String errorMessage); // PROCESSING -> DEAD
    boolean resetForReingest(Long documentId);                               // 终态 -> PENDING
    void softDelete(Long documentId);
    void softDeleteByKnowledgeBaseId(Long knowledgeBaseId);
}
```

- Verification contribution: `TEST-007`。
- After this file: 仓储契约就绪。

#### File 6 — `CREATE .../domain/knowledge/package-info.java` 与各子包 `package-info.java`

- Purpose: 包文档（`AgentSourceContractTest` 要求每个含 `.java` 的目录都有 `package-info.java`，含测试源集）。
- Symbols: `domain/knowledge`、`model`、`gateway`、`repository` 与测试包各一份。
- Repository evidence: `domain/research` 各包的 `package-info.java`。
- Dependencies and consumers: 无。
- Why now: 与端口同批。
- Contract/signature changes: 无。
- Input/output and state mapping: 无。
- Error and edge behavior: 缺文件则契约测试失败。
- Standards impact: `MC-NAME-001`、`MC-SCOPE-001`。
- Literal rule enforcement: `Rule 1`、`Rule 11`。
- Implementation pseudocode:

```java
/** Domain-owned knowledge base vocabulary and ports. */
package top.egon.cola.archetype.source.agent.domain.knowledge;
```

- Verification contribution: `TEST-006`。
- After this file: Step 4 全部文件就位。

- Validation working directory: `/Users/mario/SelfProject/Egon-COLA`
- Verification command: `./mvnw -B -ntp -f egon-cola-archetypes/source-projects/egon-cola-source-agent/pom.xml clean verify`
- Expected result: 退出码 0；`KnowledgeDomainTest` 通过；`AgentArchitectureTest` 的 domain 无框架断言仍通过。
- Failure returns to: File 2/3 的校验或 File 4/5 的包位置。
- Completion criteria: `REQ-001` 与 `REQ-006` 的领域部分有证据。
- Rollback: 回退 domain 的 knowledge 子树。
- Commit paths: `...-domain/src/test/java/.../domain/knowledge/{KnowledgeDomainTest,package-info}.java`; `.../domain/knowledge/model/{KnowledgeBaseBO,KnowledgeDocumentBO,KnowledgeChunkBO,KnowledgeChunkConfigBO,DocumentIngestStatusEnum,KnowledgeBaseStatusEnum,ChunkingStrategyEnum,package-info}.java`; `.../domain/knowledge/gateway/{KnowledgeVectorGateway,package-info}.java`; `.../domain/knowledge/repository/{KnowledgeBaseRepository,KnowledgeDocumentRepository,package-info}.java`; `.../domain/knowledge/package-info.java`
- Commit: `feat(agent-archetype): add the knowledge domain model and ports`

在实施期修正（领域载体与词汇）：

- **枚举命名按 Spec B**：计划原写 `KnowledgeChunkingStrategyEnum`，Spec B `§8.2` 目标树、`§10.1` 与 `§10.3` 字段表都写 `ChunkingStrategyEnum`，实现取后者；计划中 Step 8/9 的引用已一并改名，语义不变。
- **`chunkConfig` 用领域自有载体**：计划原写 `RagChunkingConfigBO`，组件里没有这个类型（组件是 `RagChunkingConfigDTO`，内含 `strategy`）。实现改为领域自有的 `KnowledgeChunkConfigBO`，只承载参数（`maxTokensPerChunk`、`overlapTokens`、`minChunkChars`、`headingLevels`），不含策略。理由：Spec B `§8.3` 规定 domain 的依赖只有 `common` 与 Jakarta Validation，本 Step 也不改 domain POM；把组件载体放进 domain 会把 `rag-starter`（连同 Spring AI）拉进 domain 的编译类路径，与 `EVD-002` 的意图相反。策略与参数的配合规则（`headingLevels` 仅 `MARKDOWN_HEADING` 可用）由 `KnowledgeBaseBO` 的紧凑构造器校验，组件边界上的 `RagChunkingConfigDTO` 由 Step 6/7 的适配器组装。
- **不创建 `KnowledgeIngestService`**：计划 File 6 原要求该领域服务端口，但计划 Step 7 的 handler 实际直接注入组件的 `RagIngestionService`（handler 本身即适配器），该端口既无实现者也无调用方；Spec B `§10.1` 的领域服务契约是问答流的观察者（`§2976`），归属 Step 11。按 `MC-MODEL-001` 的必要性审计，本 Step 不引入空端口。
- **BO 标识字段名按 Spec B `§10.3`**：`KnowledgeBaseBO.knowledgeBaseId`、`KnowledgeDocumentBO.documentId`（计划伪代码写 `id`），`BO → VO` 因此可按名直连映射。
- **非法迁移的表达**：计划 File 1 写「非法迁移抛 `KnowledgeApplicationException`」，但该异常属 application 层（Spec B `§10.1`），domain 不依赖 application；domain 侧以 `DocumentIngestStatusEnum#canTransitionTo` 返回 `false` 表达非法迁移，409 映射留给 Step 8 的用例。
- **仓储端口的增补**：`KnowledgeDocumentRepository` 增加 `findByIds`（检索结果的 `displayName` 需要按文档标识批量解析，Spec B `§9.2.11` 明示「避免调用方二次查询」）与 `countByKnowledgeBaseId`（知识库详情的 `documentCount` 与单库容量判定）；状态回写按 Spec B `§10.6` 的每一行给出显式方法并以「影响 0 或 1 行」的布尔返回表达并发保护（Spec B `§11.2.2`）。

### Step 5 — infrastructure：持久化

- Requirements: `REQ-002`, `REQ-003`, `REQ-023`, `REQ-025`
- Dependencies: Step 3, Step 4
- Baseline state: 两张业务表已迁移；domain 契约已就位；无 PO/DAO。
- Observable outcome: 仓储端口的实现可完成插入、查询、分页、更新与软删，且生成 SQL 含租户条件。
- End state: PO、Converter、DAO 与仓储实现就位。
- Test-first gate: `Required` — PO/DAO 不存在。
- Manual Checks: `MC-NAME-001`, `MC-MODEL-001`, `MC-CONVERT-001`, `MC-BEAN-001`, `MC-LOG-001`, `MC-SCOPE-001`, `MC-TEST-001`
- Literal Rules: `Rule 1`, `Rule 2`, `Rule 3`, `Rule 4`, `Rule 9`, `Rule 10`, `Rule 11`
- Ordered files:

#### File 1 — `CREATE ...-infrastructure/src/test/java/.../infrastructure/knowledge/KnowledgeRepositoryTest.java`

- Purpose: 锁定租户作用域、软删过滤与分页排序。
- Symbols: `scopes_every_query_by_tenant()`、`filters_soft_deleted_rows()`、`orders_by_creation_descending()`。
- Repository evidence: MyBatis Plus 的语句拦截可用于断言生成 SQL；`source-web` 的仓储测试风格。
- Dependencies and consumers: 消费仓储实现与 DAO。
- Why now: 持久化行为先定。
- Contract/signature changes: 引入 PO/DAO/仓储实现。
- Input/output and state mapping: 租户上下文 → 结果集。
- Error and edge behavior: 软删行不可见；跨租户不可见。
- Standards impact: `MC-VALID-001`、`MC-CONVERT-001`、`MC-SCOPE-001`、`MC-TEST-001`。
- Literal rule enforcement: `Rule 2`、`Rule 11`。
- Implementation pseudocode:

```java
@Test void scopes_every_query_by_tenant() {
    List<String> sql = capturedStatements(() -> repository.page(0, 20, null, null));
    assertThat(sql).allSatisfy(statement -> assertThat(statement).contains("tenant_id"));
}
```

- Verification contribution: `TEST-007` 与 `REQ-025`。
- After this file: 编译失败。

#### File 2 — `CREATE .../infrastructure/knowledge/repo/po/{KnowledgeBasePO,KnowledgeDocumentPO}.java`

- Purpose: 持久化载体，含业务列与 `@TableName`。
- Symbols: 两个类，继承 `EgonModel<M>`。
- Repository evidence: `RolePO` 的注解组合（`EVD-011`）；`EgonModel` 提供 `id`/`tenantId`/审计/`isDeleted`（`EVD-010`）。
- Dependencies and consumers: DAO 与 Converter。
- Why now: DAO 与 Converter 的类型前提。
- Contract/signature changes: 新增 PO；**使用仓库既有注解组合，不含 `@RequiredArgsConstructor`**（Rule 3 例外，用户已批准）。
- Input/output and state mapping: 列 → 字段，见 Spec B `§11.2`。
- Error and edge behavior: 无。
- Standards impact: `MC-MODEL-001`（含已批准例外）、`MC-NAME-001`（`PO`）、`MC-SCOPE-001`。
- Literal rule enforcement: `Rule 1`、`Rule 3`（含已批准例外）、`Rule 11`。
- Implementation pseudocode:

```java
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Accessors(chain = true)
@TableName("knowledge_base")
public class KnowledgeBasePO extends EgonModel<KnowledgeBasePO> {
    @TableField("code") private String code;
    @TableField("name") private String name;
    // description / embedding_model / chunk_strategy / chunk_config / status
}
```

- Verification contribution: `TEST-007`。
- After this file: PO 就绪。

#### File 3 — `CREATE .../infrastructure/knowledge/repo/converter/{KnowledgeBasePOConverter,KnowledgeDocumentPOConverter}.java`

- Purpose: PO 与 BO 的双向映射。
- Symbols: 两个 `@Mapper` 接口，`extends BaseConverter<PO, BO>`，`unmappedTargetPolicy = ERROR`。
- Repository evidence: `ResearchEventConverter` 的写法（`EVD-015`）。
- Dependencies and consumers: 仓储实现。
- Why now: 仓储实现依赖它们。
- Contract/signature changes: 新增转换器；`chunk_config` 的 jsonb 文本与组件配置载体互转写在此处。
- Input/output and state mapping: 逐字段显式 `@Mapping`。
- Error and edge behavior: 未映射字段编译期失败。
- Standards impact: `MC-CONVERT-001`、`MC-JSON-001`、`MC-SCOPE-001`。
- Literal rule enforcement: `Rule 1`、`Rule 3`、`Rule 6`、`Rule 11`。
- Implementation pseudocode:

```java
@Mapper(unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface KnowledgeBasePOConverter extends BaseConverter<KnowledgeBasePO, KnowledgeBaseBO> {
    KnowledgeBasePOConverter INSTANCE = Mappers.getMapper(KnowledgeBasePOConverter.class);
    @Mapping(target = "id", source = "id") /* 逐字段 */ KnowledgeBaseBO toTarget(KnowledgeBasePO source);
    @Mapping(target = "id", source = "id") /* 逐字段 */ KnowledgeBasePO toSource(KnowledgeBaseBO target);
}
```

- Verification contribution: `TEST-007`。
- After this file: 转换器就绪。

#### File 4 — `CREATE .../infrastructure/knowledge/repo/dao/{KnowledgeBaseDAO,KnowledgeDocumentDAO}.java`

- Purpose: 数据访问组件。
- Symbols: 两个接口，`extends EgonColaMapper<PO>`，加自定义分页与更新方法。
- Repository evidence: `EVD-009` 的 `repo/dao` 布局与 `EVD-010` 的 `EgonColaMapper`。
- Dependencies and consumers: 仓储实现。
- Why now: 仓储实现的类型前提。
- Contract/signature changes: 新增访问组件；**查询不写租户谓词**（拦截器注入）。
- Input/output and state mapping: 条件 → 行。
- Error and edge behavior: 影响行数为 0 表示目标不存在或不可见。
- Standards impact: `MC-NAME-001`（`DAO`）、`MC-SCOPE-001`。
- Literal rule enforcement: `Rule 1`、`Rule 11`。
- Implementation pseudocode:

```java
public interface KnowledgeBaseDAO extends EgonColaMapper<KnowledgeBasePO> {
    List<KnowledgeBasePO> selectPage(@Param("keyword") String keyword, @Param("embeddingModel") String embeddingModel,
                                     @Param("offset") int offset, @Param("size") int size);
    long countActive(@Param("keyword") String keyword, @Param("embeddingModel") String embeddingModel);
}
```

- Verification contribution: `TEST-007`。
- After this file: DAO 就绪。

#### File 5 — `CREATE .../infrastructure/knowledge/service/{KnowledgeBaseRepositoryImpl,KnowledgeDocumentRepositoryImpl}.java`

- Purpose: 实现 domain 仓储端口。
- Symbols: 两个 `@Repository("...")` 类，`@RequiredArgsConstructor` + `@Qualifier` + `@Slf4j`。
- Repository evidence: `DeepResearchManageImpl` 的 Bean 与注入写法。
- Dependencies and consumers: application 的 Manage。
- Why now: 端口与 DAO 都已就位。
- Contract/signature changes: 实现全部端口方法；分页排序 `create_time desc, id desc`。
- Input/output and state mapping: PO ↔ BO 经转换器。
- Error and edge behavior: 软删用 `is_deleted = true`；计数只统计未删除行。
- Standards impact: `MC-BEAN-001`、`MC-LOG-001`、`MC-SCOPE-001`。
- Literal rule enforcement: `Rule 1`、`Rule 4`、`Rule 11`。
- Implementation pseudocode:

```java
@Slf4j
@Repository("knowledgeBaseRepository")
@RequiredArgsConstructor
public class KnowledgeBaseRepositoryImpl implements KnowledgeBaseRepository {
    private final @Qualifier("knowledgeBaseDAO") KnowledgeBaseDAO knowledgeBaseDAO;
    @Override public KnowledgeBaseBO insert(KnowledgeBaseBO knowledgeBase) {
        KnowledgeBasePO po = KnowledgeBasePOConverter.INSTANCE.toSource(knowledgeBase);
        knowledgeBaseDAO.insert(po);
        return KnowledgeBasePOConverter.INSTANCE.toTarget(po);
    }
}
```

- Verification contribution: `TEST-007`。
- After this file: Step 5 完成（含 `package-info.java`）。

- Validation working directory: `/Users/mario/SelfProject/Egon-COLA`
- Verification command: `./mvnw -B -ntp -f egon-cola-archetypes/source-projects/egon-cola-source-agent/pom.xml clean verify`
- Expected result: 退出码 0；`KnowledgeRepositoryTest` 通过；生成 SQL 含租户条件。
- Failure returns to: File 3（映射）、File 5（排序或软删条件）。
- Completion criteria: `REQ-002`、`REQ-003`、`REQ-025` 的持久化部分有证据。
- Rollback: 回退 infrastructure 的 repo 与 service 子树。
- Commit paths: `...-infrastructure/src/test/java/.../infrastructure/knowledge/KnowledgeRepositoryTest.java`; `.../infrastructure/knowledge/repo/po/{KnowledgeBasePO,KnowledgeDocumentPO}.java`; `.../infrastructure/knowledge/repo/converter/{KnowledgeBasePOConverter,KnowledgeDocumentPOConverter}.java`; `.../infrastructure/knowledge/repo/dao/{KnowledgeBaseDAO,KnowledgeDocumentDAO}.java`; `.../infrastructure/knowledge/service/{KnowledgeBaseRepositoryImpl,KnowledgeDocumentRepositoryImpl}.java`；实施期增补：`...-infrastructure/pom.xml`（H2 `test` 作用域）、`...-infrastructure/knowledge/repo/po/JsonbStringTypeHandler.java`、`...-starter/DeepResearchApplication.java`（`@MapperScan`）
- Commit: `feat(agent-archetype): persist knowledge bases and documents`

在实施期修正（DAO 形态）：Spec B 第 2596 行已定调"文档用途，非生产 SQL；实际实现使用 MyBatis Plus 的条件构造器"，故本 Step 的 File 4 伪代码中的手写 SQL 不落地：两个 DAO 只是 `EgonColaMapper<PO>` 的裸扩展，列表/计数/软删/条件更新全部由 `LambdaQueryWrapper`/`LambdaUpdateWrapper` 组装，排序恒为 `create_time desc, id desc`，窗口由 `.last("limit ? offset ?")` 渲染并在入参处守卫（负 offset / 非正 size 抛 `IllegalArgumentException`）。

在实施期修正（jsonb 写入）：迁移里的 `chunk_config` 是 `jsonb`，且迁移文件不可改，pgjdbc 默认按 `varchar` 发送会报类型不匹配，故新增 `JsonbStringTypeHandler`（`setObject(index, value, Types.OTHER)`）承载 BO 与 jsonb 文本的互转；这也符合 Spec B §11.2 对转换器与类型处理的分工。

在实施期修正（状态写入的整行前提）：`EgonColaModelValidationInterceptor` 对 update/insert 校验的是完整持久化行（`id`、`tenantId`、`createTime`、`createUserId`、`isDeleted` 必须非空），只带变更列的"部分实体"会被拒。故状态迁移实现为读改写：`selectById` → 状态前置校验 → `carrierOf(current)` 复制五个持久化列 → `update(change, ...)` 并把期望状态留在 WHERE 里作为 CAS；`error_code`/`error_message` 用 `FieldStrategy.ALWAYS`，否则默认的 `NOT_NULL` 策略会让"清空上一次失败原因"永远无法落库。`resetForReingest` 的终态集合由 `DocumentIngestStatusEnum::isTerminal` 派生，不手写字面量。

在实施期修正（离线可证的模糊匹配）：Spec B 的说明性 SQL 用 `ILIKE`，H2 不支持，故实现改用 `lower(col) like ?`（大小写不敏感语义等价，参数绑定而非拼接）；`TEST-026`/`TEST-032` 中依赖真实 PostgreSQL 的部分（`vector` 扩展、jsonb 与表达式索引的真实建表、维度校验）离线不可执行，已在本 Step 与 Step 3 的记述中留痕，留待有 PostgreSQL 的运行期验收。

在实施期修正（宿主扫描）：infrastructure 的 `...knowledge.repo.dao` 不在 starter 自动扫描的基础包内，Step 5 的仓储 Bean 一经组件扫描就因缺 `KnowledgeBaseDAO` 而让 starter 的三个上下文测试变红，故 `DeepResearchApplication` 按 service archetype 的先例增补 `@MapperScan(basePackages = "...knowledge.repo.dao")`——这是计划文件树未列出的实施期增补，随本 Step 提交。

本 Step 实际交付 6 个 `package-info.java`（`knowledge`、`knowledge/repo`、`knowledge/repo/po`、`knowledge/repo/converter`、`knowledge/repo/dao`、`knowledge/service`）与 5 个测试方法（多出的是租户隔离、软删可见性与 CAS 失败路径），均按 `AgentSourceContractTest` 的包文档规约与 CSS 边界的证据要求扩展。

### Step 6 — infrastructure：向量与嵌入装配

- Requirements: `REQ-008`, `REQ-009`, `REQ-010`, `REQ-023`, `REQ-027`
- Dependencies: Step 2
- Baseline state: 组件已交付；无宿主 Bean。
- Observable outcome: 启动后存在具名 `EmbeddingModel` 与 `VectorStore`；向量表由 `PgVectorStore` 创建；检索过滤恒含租户。
- End state: 两个配置类与向量 gateway 就位。
- Test-first gate: `Required` — 装配类不存在。
- Manual Checks: `MC-BEAN-001`, `MC-DEP-001`, `MC-CONFIG-001`, `MC-ARCH-001`, `MC-SCOPE-001`, `MC-TEST-001`
- Literal Rules: `Rule 4`, `Rule 11`
- Ordered files:

#### File 1 — `CREATE ...-infrastructure/src/test/java/.../infrastructure/knowledge/KnowledgeVectorConfigurationTest.java`

- Purpose: 锁定 Bean 名、维度一致与租户强制过滤。
- Symbols: `exposes_named_embedding_model_and_vector_store()`、`fails_when_dimensions_mismatch()`、`forces_the_tenant_filter()`。
- Repository evidence: 组件 `§9.2.6` 的 Bean 名契约与 `§9.2.11` 的强制过滤语义。
- Dependencies and consumers: 消费两个配置类与 gateway。
- Why now: 装配契约先定。
- Contract/signature changes: 引入配置类与 gateway。
- Input/output and state mapping: 配置 → Bean。
- Error and edge behavior: 维度不符时上下文启动失败。
- Standards impact: `MC-CONFIG-001`、`MC-BEAN-001`、`MC-SCOPE-001`、`MC-TEST-001`。
- Literal rule enforcement: `Rule 11`。
- Implementation pseudocode:

```java
@Test void exposes_named_embedding_model_and_vector_store() {
    contextRunner.run(context -> assertThat(context).hasNotFailed()
        .hasBean("knowledgeEmbeddingModel")
        .hasBean("knowledgeRagVectorStore"));
}

@Test void forces_the_tenant_filter() {
    gateway.retrieve("kb-1", "openai-compatible", "q", 8, Map.of());
    String expression = fakeVectorStore.searchRequests().get(0).getFilterExpression().toString();
    assertThat(expression).contains("tenantId").contains("kb-1").contains("openai-compatible");
}

@Test void fails_when_dimensions_mismatch() {
    contextRunner.withPropertyValues("egon.cola.component.rag.dimensions=768")
        .run(context -> assertThat(context).hasFailed().getFailure().hasStackTraceContaining("1536"));
}
```

- Verification contribution: `TEST-008`。
- After this file: 编译失败。

#### File 2 — `CREATE .../infrastructure/knowledge/config/KnowledgeEmbeddingConfiguration.java`

- Purpose: 提供具名 OpenAI-compatible `EmbeddingModel`。
- Symbols: `@Configuration(proxyBeanMethods = false)` + `@Bean("knowledgeEmbeddingModel")`。
- Repository evidence: `DeepResearchAiConfiguration` 的 `ChatModel` Bean 写法。
- Dependencies and consumers: 被 `VectorStore` 与组件按名解析。
- Why now: `VectorStore` 依赖它。
- Contract/signature changes: 新增 Bean 名 `knowledgeEmbeddingModel`。
- Input/output and state mapping: `spring.ai.openai.embedding.*` 配置 → Bean。
- Error and edge behavior: 凭据缺失时启动失败。
- Standards impact: `MC-BEAN-001`、`MC-CONFIG-001`、`MC-DEP-001`、`MC-SCOPE-001`。
- Literal rule enforcement: `Rule 4`、`Rule 11`。
- Implementation pseudocode:

```java
@Configuration(proxyBeanMethods = false)
public class KnowledgeEmbeddingConfiguration {
    @Bean("knowledgeEmbeddingModel")
    @ConditionalOnMissingBean(name = "knowledgeEmbeddingModel")
    public EmbeddingModel knowledgeEmbeddingModel(OpenAiEmbeddingModel model) { return model; }
}
```

- Verification contribution: `TEST-008`。
- After this file: 嵌入 Bean 就绪。

#### File 3 — `CREATE .../infrastructure/knowledge/config/KnowledgeVectorConfiguration.java`

- Purpose: 提供具名 `VectorStore`，表由它创建。
- Symbols: `@Bean("knowledgeRagVectorStore")`。
- Repository evidence: 组件 `EVD-018` 的 builder 方法；`initializeSchema` 默认 `false` 因此必须显式开启。
- Dependencies and consumers: 被组件按 `vector-store-bean-name` 解析。
- Why now: gateway 与 handler 依赖它。
- Contract/signature changes: 新增 Bean 名；`initializeSchema(true)` 使向量表由本 Bean 创建（`REQ-010`）。
- Input/output and state mapping: `JdbcTemplate` + `EmbeddingModel` + 配置维度 → `VectorStore`。
- Error and edge behavior: 维度与表不符时首次写入失败。
- Standards impact: `MC-BEAN-001`、`MC-CONFIG-001`、`MC-DEP-001`、`MC-SCOPE-001`。
- Literal rule enforcement: `Rule 4`、`Rule 11`。
- Implementation pseudocode:

```java
@Bean("knowledgeRagVectorStore")
@ConditionalOnMissingBean(name = "knowledgeRagVectorStore")
public VectorStore knowledgeRagVectorStore(JdbcTemplate jdbcTemplate,
                                           @Qualifier("knowledgeEmbeddingModel") EmbeddingModel embeddingModel,
                                           @Qualifier("knowledgeRuntimeProperties") KnowledgeRuntimeProperties properties) {
    return PgVectorStore.builder(jdbcTemplate, embeddingModel)
            .dimensions(properties.dimensions())
            .initializeSchema(true)
            .build();
}
```

- Verification contribution: `TEST-008` 与 `REQ-010`。
- After this file: 向量 Bean 就绪。

#### File 4 — `CREATE .../infrastructure/knowledge/gateway/RagKnowledgeVectorGateway.java`

- Purpose: 实现 domain 端口，调用组件的检索服务。
- Symbols: `@Component("knowledgeVectorGateway")` 或 `@Service("...")`。
- Repository evidence: `AgentFlowDeepResearchAgentGateway` 的适配器写法。
- Dependencies and consumers: application 的问答用例。
- Why now: 端口与 Bean 都已就位。
- Contract/signature changes: 实现 `KnowledgeVectorGateway`；检索时把租户作为业务属性强制叠加（`REQ-027`）。
- Input/output and state mapping: 组件 `RagRetrievedChunkBO` → domain `KnowledgeChunkBO`。
- Error and edge behavior: 组件异常映射为 `KnowledgeApplicationException`。
- Standards impact: `MC-BEAN-001`、`MC-LOG-001`、`MC-PATTERN-001`、`MC-CONVERT-001`、`MC-SCOPE-001`。
- Literal rule enforcement: `Rule 1`、`Rule 3`、`Rule 4`、`Rule 9`、`Rule 11`。
- Implementation pseudocode:

```java
@Service("knowledgeVectorGateway")
@RequiredArgsConstructor
public class RagKnowledgeVectorGateway implements KnowledgeVectorGateway {
    private final @Qualifier("ragRetrievalService") RagRetrievalService ragRetrievalService;
    @Override public List<KnowledgeChunkBO> retrieve(String collectionId, String logicalModelName, String query,
                                                     int topK, Map<String, String> attributes) {
        Map<String, String> filters = new LinkedHashMap<>(attributes);
        filters.put(RagMetadataKeys.TENANT_ID_METADATA_KEY, currentTenantId());
        return ragRetrievalService.retrieve(new RagRetrievalQuery(collectionId, logicalModelName, query, topK, 0.0,
                filters)).stream().map(this::toChunkBO).toList();
    }
}
```

- Verification contribution: `TEST-008`。
- After this file: Step 6 完成。

- Validation working directory: `/Users/mario/SelfProject/Egon-COLA`
- Verification command: `./mvnw -B -ntp -f egon-cola-archetypes/source-projects/egon-cola-source-agent/pom.xml clean verify`
- Expected result: 退出码 0；`KnowledgeVectorConfigurationTest` 通过。
- Failure returns to: File 3（builder 方法名与 `1.1.8` 不符，见 `BLOCK-002`）。
- Completion criteria: `REQ-008`-`REQ-010`、`REQ-027` 的装配部分有证据。
- Rollback: 回退 infrastructure 的 config 与 gateway。
- Commit paths: `...-infrastructure/src/test/java/.../infrastructure/knowledge/KnowledgeVectorConfigurationTest.java`; `.../infrastructure/knowledge/config/KnowledgeEmbeddingConfiguration.java`; `.../infrastructure/knowledge/config/KnowledgeVectorConfiguration.java`; `.../infrastructure/knowledge/gateway/RagKnowledgeVectorGateway.java`
- Commit: `feat(agent-archetype): wire the embedding model and vector store`

在实施期修正（嵌入模型的构造入口）：plan File 2 假定宿主已从自动配置拿到 `OpenAiEmbeddingModel`，但 starter 的 classpath 上没有 `spring-ai-autoconfigure-model-openai`，`spring.ai.openai.*` 键始终是惰性的（research 域也是手工构造模型）。因此 infrastructure 另建两个类：`KnowledgeEmbeddingProperties`（`@Validated` 记录，前缀 `agent.knowledge.embedding`，`ignoreUnknownFields = false`，`base-url`/`api-key`/`model-name` 三键 `@NotBlank`，紧凑构造器 trim；不设 `dimensions` 分量）与 `KnowledgeEmbeddingConfiguration`（`@ConditionalOnProperty(egon.cola.component.rag.enabled=true)` + `@Bean("knowledgeEmbeddingModel")` + `@ConditionalOnMissingBean`）。模型按 `new OpenAiEmbeddingModel(api, MetadataMode.NONE, options)` 构造——`1.1.8` 只有构造器没有 builder，`MetadataMode.NONE` 避免把正文当元数据外发。infrastructure 的 POM 相应新增 `org.springframework.ai:spring-ai-openai`（版本由 Boot BOM 管理）。

在实施期修正（向量宽度只有一个来源）：plan File 3 伪代码写成 `KnowledgeRuntimeProperties.dimensions()`，但该类是 application 层 `agent.knowledge.runtime` 的宿主属性（Step 8），既无 `dimensions` 分量，也不能被 infrastructure 引用。改为 `@Qualifier("ragProperties") RagProperties.dimensions()`：宽度只由 `egon.cola.component.rag.dimensions` 一处给出，组件在启动期还会拿它与模型自报维度对账。

在实施期修正（租户键与异常边界）：plan File 4 伪代码引用 `RagMetadataKeys.TENANT_ID_METADATA_KEY`，但组件的保留键只有 collectionId/documentId/chunkIndex/logicalModelName/contentHash——按 `ASM-008`，`tenantId` 是业务属性而非保留键，故新增 `KnowledgeVectorMetadata.TENANT_ID`（`infrastructure.knowledge.metadata` 包）承载该常量。同一伪代码要求"组件异常映射为 `KnowledgeApplicationException`"，而该类属 application 层（Spec B 行 2429），infrastructure 不能引用；网关原样上抛组件异常，映射留给 Step 8 的用例边界（`KnowledgeQaManageImpl`）。网关另为每个检索拷贝调用方属性再强制覆写租户键，保证调用方无法绕过租户作用域。

在实施期修正（离线测试装配）：`fails_when_dimensions_mismatch` 必须同时注册两个配置类——只注册 `KnowledgeEmbeddingConfiguration` 时先失败的是"具名向量存储不存在"，断言 `1536` 会落空。测试用被 mock 的 `JdbcTemplate` 驱动真实 `PgVectorStore`（init 期只执行 DDL，可以离线跑），断言语句含 `CREATE EXTENSION IF NOT EXISTS vector` 与 `CREATE TABLE IF NOT EXISTS public.vector_store … embedding vector(1536)`；用内存 `RecordingVectorStore` 承接检索路径，断言强制过滤式同时含 `tenantId`、`42`、`1001`、`openai-compatible`；模型名取框架内置维度表里已有条目的 `text-embedding-ada-002`，使 `dimensions()` 不必访问网络。共 4 个测试方法（plan 写 3 个），另加 `config`/`gateway`/`metadata` 三个 `package-info.java`。

在实施期修正（离线 profile 的组件侧假件）：`test` profile 关掉 rag 组件后，具名依赖 `ragRetrievalService` 不存在，网关会让 starter 的三个上下文测试全部失败（`NoSuchBeanDefinitionException`）。按既有 `FakeAgentDependencies` 的写法在 `DeepResearchApplicationTest` 内补一个 `ragRetrievalService` 假件（与 `deepResearchChatModel` 同类：外部依赖在离线 profile 里为假件，其余 profile 由组件装配真实件）。Step 8 的三个 `@Service` 还要注入 `ragDocumentStorage`、`ragExtractionService`、`transactionalOutbox`，同样需要在离线 profile 补假件，否则上下文起不来——已记入 Step 8 的实施前提。

在实施期修正（配置键）：四份 profile 新增 `agent.knowledge.embedding.{base-url,api-key,model-name}`：`application.yml` 用 `${VAR:}` 留空、dev/prod 用 `${VAR}` 无默认值（缺失即启动失败，fail-closed）、test 用哑值占位；四份键集合同构，Step 12 的 `KnowledgeConfigParityTest` 可直接全量比对。切记 Step 8 的 `KnowledgeRuntimeProperties` 绑定 `agent.knowledge` 时不要开启 `ignoreUnknownFields = false`（或须显式声明 `embedding` 子块），否则严格绑定会把这三个键判为未知键。

- Commit paths 补充：`...-infrastructure/src/main/java/.../infrastructure/knowledge/config/KnowledgeEmbeddingProperties.java`; `.../infrastructure/knowledge/config/package-info.java`; `.../infrastructure/knowledge/gateway/package-info.java`; `.../infrastructure/knowledge/metadata/{KnowledgeVectorMetadata,package-info}.java`; `...-infrastructure/pom.xml`; `...-starter/src/main/resources/{application.yml,application-dev.yml,application-test.yml,application-prod.yml}`; `...-starter/src/test/java/.../starter/DeepResearchApplicationTest.java`

### Step 7 — outbox 异步摄取与租户恢复

- Requirements: `REQ-004`, `REQ-005`, `REQ-006`, `REQ-007`, `REQ-016`, `REQ-023`, `REQ-026`
- Dependencies: Step 5, Step 6
- Baseline state: 仓储与向量装配就位；无 outbox handler。
- Observable outcome: 投递线程从载荷恢复租户、按正确租户读写、结束后清理 MDC；文档状态随投递推进。
- End state: handler 与 outbox 配置就位。
- Test-first gate: `Required` — handler 不存在。
- Manual Checks: `MC-BEAN-001`, `MC-LOG-001`, `MC-TIME-001`, `MC-PATTERN-001`, `MC-SCOPE-001`, `MC-TEST-001`
- Literal Rules: `Rule 1`, `Rule 4`, `Rule 9`, `Rule 10`, `Rule 11`
- Ordered files:

#### File 1 — `CREATE ...-infrastructure/src/test/java/.../infrastructure/knowledge/KnowledgeIngestDeliveryHandlerTest.java`

- Purpose: 锁定两段式重建、状态映射、幂等与 MDC 生命周期。
- Symbols: `rebuilds_the_document_without_reparsing()`、`maps_delivery_failures_to_document_status()`、`restores_and_clears_the_tenant_mdc()`、`treats_a_missing_document_as_success()`。
- Repository evidence: 组件 `§9.2.1` 的摄取契约与 `§7.3.4` 的失败语义。
- Dependencies and consumers: 消费 handler 与仓储。
- Why now: handler 行为先定。
- Contract/signature changes: 引入 handler。
- Input/output and state mapping: 载荷 → 文档状态。
- Error and edge behavior: 载荷缺租户时投递失败而非空租户执行。
- Standards impact: `MC-PATTERN-001`、`MC-BEAN-001`、`MC-LOG-001`、`MC-SCOPE-001`、`MC-TEST-001`。
- Literal rule enforcement: `Rule 9`、`Rule 11`。
- Implementation pseudocode:

```java
@Test void restores_and_clears_the_tenant_mdc() {
    handler.handle(contextWith(payload(tenantId = 42L, documentId = 1L)));
    assertThat(MDC.get(TENANT_MDC_KEY)).isNull();
}
```

- Verification contribution: `TEST-009` 与 `REQ-026`。
- After this file: 编译失败。

#### File 2 — `CREATE .../infrastructure/knowledge/handler/KnowledgeIngestDeliveryHandler.java`

- Purpose: outbox 投递处理器：恢复租户、重建文档、调用摄取、回写状态。
- Symbols: `@Component("knowledgeIngestDeliveryHandler")`，`implements DeliveryHandler`，`channel() = "rag-ingest"`。
- Repository evidence: 组件的 `DeliveryHandler` SPI 与 `EVD-017` 的至少一次语义。
- Dependencies and consumers: outbox 轮询器。
- Why now: 仓储与向量都已就位。
- Contract/signature changes: 新增 handler；`finally` 清理 MDC。
- Input/output and state mapping: 载荷 `{tenantId, documentId}` → 文档行状态与分块数。
- Error and edge behavior: 失败抛出由 outbox 重试；记录不可见视为成功；文本缺失抛失败。
- Standards impact: `MC-BEAN-001`、`MC-LOG-001`、`MC-TIME-001`、`MC-PATTERN-001`、`MC-SCOPE-001`。
- Literal rule enforcement: `Rule 1`、`Rule 4`、`Rule 9`、`Rule 10`、`Rule 11`。
- Implementation pseudocode:

```java
@Slf4j
@Component("knowledgeIngestDeliveryHandler")
@RequiredArgsConstructor
public class KnowledgeIngestDeliveryHandler implements DeliveryHandler {
    private final @Qualifier("knowledgeDocumentRepository") KnowledgeDocumentRepository documentRepository;
    private final @Qualifier("ragIngestionService") RagIngestionService ragIngestionService;
    @Override public String channel() { return "rag-ingest"; }
    @Override public DeliveryResult handle(DeliveryContext context) {
        Long tenantId = context.requireLong("tenantId");
        Long documentId = context.requireLong("documentId");
        MDC.put(TENANT_MDC_KEY, String.valueOf(tenantId));
        try {
            Optional<KnowledgeDocumentBO> document = documentRepository.findById(documentId);
            if (document.isEmpty()) return DeliveryResult.success();          // 幂等
            // 从 content 重建 ExtractedDocumentBO，调用 ingest，回写状态与分块数
        } finally { MDC.remove(TENANT_MDC_KEY); }
    }
}
```

- Verification contribution: `TEST-009`。
- After this file: Step 7 完成（含 `package-info.java`）。

- Validation working directory: `/Users/mario/SelfProject/Egon-COLA`
- Verification command: `./mvnw -B -ntp -f egon-cola-archetypes/source-projects/egon-cola-source-agent/pom.xml clean verify`
- Expected result: 退出码 0；`KnowledgeIngestDeliveryHandlerTest` 通过。
- Failure returns to: File 2（状态映射或 MDC 清理）。
- Completion criteria: `REQ-004`-`REQ-007`、`REQ-016`、`REQ-026` 有证据。
- Rollback: 回退 handler 与其测试。
- Commit paths: `...-infrastructure/src/test/java/.../infrastructure/knowledge/KnowledgeIngestDeliveryHandlerTest.java`; `.../infrastructure/knowledge/handler/KnowledgeIngestDeliveryHandler.java`
- Commit: `feat(agent-archetype): ingest documents through the transactional outbox`

在实施期修正（载荷契约与解析）：Spec B 行 2753 的载荷样例只有 `schemaVersion` 与 `documentId`，`tenantId` 由本 Step 依 `REQ-026`/`DEC-013` 补入——写方是 Step 8 的上传用例，读方是本 handler。实际形状为 `{"schemaVersion":"1","documentId":2001,"tenantId":42}`：`schemaVersion` 必须是文本 `"1"`（数字或缺失即 `IllegalArgumentException`），`documentId`/`tenantId` 同时接受 JSON 整数与数字串；解析在设置 MDC 之前完成，载荷缺租户时"投递失败而非以空租户执行"（Spec B 行 185、619、`TEST-033`），`finally` 只负责清理。

在实施期修正（失败即抛出）：Spec B 行 543 定调"抛出 -> 重试/死信"，故 handler 在写明文档状态后原样抛出异常（`RagException` 直接抛、其余不变），不在此处返回 `retryableFailure`——outbox 因此按组件分类器记录 `OUTBOX_DELIVERY_EXCEPTION` + 异常类名，而文档行上落的是本项目的稳定码，两处码各司其职。`markSucceeded` 自身失败也走同一路径：本次投递失败并重试（Spec B 行 546），摄取幂等（组件先删旧分块）使重试安全。

在实施期修正（文档错误码只用规范已有码）：plan 的 File 2 伪代码只给"调用摄取、回写状态"，未给码表。实际映射为 `RAG_VECTOR_STORE -> KNOWLEDGE_EMBEDDING_FAILED`（行 1781/2672 的样例码；组件把嵌入调用放在向量存储内，外部无法区分二者，`RagIngestionService` 的 javadoc 亦如此说明）、`RAG_MODEL_NOT_REGISTERED -> KNOWLEDGE_MODEL_NOT_REGISTERED`（行 913）、`RAG_VALIDATION -> KNOWLEDGE_VALIDATION_ERROR`（行 912）、原文缺失 -> `KNOWLEDGE_CONTENT_MISSING`（行 1928）、知识库已删 -> `KNOWLEDGE_BASE_NOT_FOUND`（行 1175）、其余 -> `KNOWLEDGE_INTERNAL_ERROR`（行 917）。未分类异常的 `errorMessage` 写固定文案而非原始异常文本：`errorCode`/`errorMessage` 会随 `API-008` 渲染给客户端（行 1532、1668）。组件异常的 `safeMessage()` 按契约本就不含正文、向量与供应商报文，故可直接落库。

在实施期修正（知识库已删属终局）：plan 的边界只列了"记录不可见视为成功／文本缺失抛失败"，未覆盖"文档可见但其知识库已不在"。若按"视为不可见"返回成功，文档会永远停在 `PROCESSING`（后续尝试的 `markProcessing` 只能从 `PENDING` 出发，`resetForReingest` 又只接受终态），故实现取 `markDead` + `permanentFailure`：重试不会让知识库回来，直接了结消息，文档落到可观测、可"重新处理"的终态。

在实施期修正（摄取属性携带租户）：Spec B 行 2953 要求业务行与 `VECTOR_STORE.metadata->>'tenantId'` 由同一次调用同时提供，检索网关的强制过滤也读该键（Step 6），故 `RagIngestionCommand.attributes` 写入 `{tenantId: document.tenantId()}`——值取自读到的行而非载荷，两者本由租户拦截器保证一致。`tenantId` 不是组件的保留键（`ASM-008`），因此该键可写入。

在实施期修正（一次投递一条日志）：计时起点在 `deliver` 内、MDC 设置之后取一次，成功与失败两条日志都带 `elapsedMs`；成功行给 `documentId、knowledgeBaseId、logicalModelName、chunkCount、attemptCount、outcome、elapsedMs`，失败行给同一组字段加 `code`（Spec B 行 650 的字段表）；跳过路径没有分块数，记为 `outcome=SKIPPED reason=…`。日志文案用英文 `key=value` 结构，与既有 infrastructure 日志一致，且不含正文、分块与向量。

在实施期修正（分块配置重建的落点）：《13.1》的模式表给出两个落点（`KnowledgeBasePOConverter` 内的具名工厂方法，或 `KnowledgeIngestionFactory`）。`POConverter` 在 Step 5 已负责 PO↔BO 的持久化转换，域 BO → 组件 `RagChunkingConfigDTO` 的映射故落在 handler 的具名私有工厂方法 `chunkingConfig(KnowledgeBaseBO)` 与 `assemble(...)`，不新建类型（Rule 9：本项目不新增模式与载体）。

在实施期修正（测试与离线假件）：plan 列 4 个方法，实际 11 个——补上三条组件码到项目码的映射、未分类异常的固定文案、载荷版本不符、抛异常路径的 MDC 清理、原文缺失、知识库已删，以及"他租户/已删除记录"与"已被接管"两条跳过路径。测试用 `new EgonColaMybatisPlusProperties()` 取默认 MDC 键，不写死 `tenantId` 字面量（Step 12 的 `signs_no_tenant_parameter_in_services()` 会把 `tenantId, `／`tenantId)` 当作租户参数签名）。`handler/package-info.java` 为包文档门禁所需；`DeepResearchApplicationTest` 的 `FakeAgentDependencies` 另补 `ragIngestionService` 假件（与 Step 6 的 `ragRetrievalService` 同理：离线 profile 关了 rag 与 outbox，handler 的组件依赖必须由假件补齐）。

在实施期修正（留给最终审计的规范缺口）：Spec B §10.6 的状态机没有 `PROCESSING -> PROCESSING` 行，也没有崩溃恢复行。投递在 `markProcessing` 成功之后、终态写入之前死掉（进程被杀、库不可用）会把文档留在 `PROCESSING`；后续尝试因守卫失败而按"已被接管"返回成功，`resetForReingest` 又只接受终态，该文档即无法通过 API 恢复。本 Step 依规范的守卫实现，不在 Step 7 内改动 Step 4/5 已提交的 domain 与仓储语义；该缺口连同 `TEST-026`/`TEST-032` 的离线不可行性一并留给最终审计与规格修订决定。

- Commit paths 补充：除 plan 列出的两个文件外，本次提交还含 `.../infrastructure/knowledge/handler/package-info.java` 与 `...-starter/src/test/java/.../starter/DeepResearchApplicationTest.java`（`ragIngestionService` 假件）。

### Step 8 — application：用例

- Requirements: `REQ-002`, `REQ-003`, `REQ-005`, `REQ-008`, `REQ-022`, `REQ-023`
- Dependencies: Step 4, Step 7
- Baseline state: domain 与 infrastructure 就位；无用例。
- Observable outcome: 建库、上传、重新处理、删除与问答编排可用，且上传的文档行与 outbox 行同事务。
- End state: 三个 Manage、容量服务与属性就位。
- Test-first gate: `Required` — 用例不存在。
- Manual Checks: `MC-NAME-001`, `MC-VALID-001`, `MC-MODEL-001`, `MC-BEAN-001`, `MC-LOG-001`, `MC-PATTERN-001`, `MC-TIME-001`, `MC-SCOPE-001`, `MC-TEST-001`
- Literal Rules: `Rule 1`, `Rule 2`, `Rule 3`, `Rule 4`, `Rule 9`, `Rule 10`, `Rule 11`
- Ordered files:

#### File 1 — `CREATE ...-application/src/test/java/.../application/knowledge/KnowledgeManageTest.java`

- Purpose: 锁定上传的事务边界、补偿删除、状态与容量。
- Symbols: `writes_document_and_outbox_in_one_transaction()`、`compensates_the_stored_file_when_the_transaction_fails()`、`rejects_reingest_while_processing()`、`rejects_upload_when_capacity_is_reached()`。
- Repository evidence: `DeepResearchManageImplTest` 的用例测试风格。
- Dependencies and consumers: 消费三个 Manage 与 fake 协作者。
- Why now: 用例行为先定。
- Contract/signature changes: 引入 Command 与 Manage。
- Input/output and state mapping: Command → BO 或状态。
- Error and edge behavior: 见 Spec B `§7.3.4`。
- Standards impact: `MC-VALID-001`、`MC-BEAN-001`、`MC-LOG-001`、`MC-SCOPE-001`、`MC-TEST-001`。
- Literal rule enforcement: `Rule 2`、`Rule 9`、`Rule 11`。
- Implementation pseudocode:

```java
@Test void writes_document_and_outbox_in_one_transaction() {
    manage.upload(command());
    verify(documentRepository).insert(any());
    verify(transactionalOutbox).enqueue(argThat(payload -> payload.contains("rag-ingest")));
}
```

- Verification contribution: `TEST-010` 与 `REQ-004`。
- After this file: 编译失败。

#### File 2 — `CREATE .../application/knowledge/command/*.java`

- Purpose: 五个写意图与读意图载体。
- Symbols: `CreateKnowledgeBaseCommand`、`UpdateKnowledgeBaseCommand`、`UploadKnowledgeDocumentCommand`、`RetrieveKnowledgeCommand`、`AskKnowledgeBaseCommand`。
- Repository evidence: `StartDeepResearchCommand` 的 record + Jakarta 注解写法。
- Dependencies and consumers: adapter 与 Manage。
- Why now: Manage 的入参类型。
- Contract/signature changes: 新增载体；**不含租户字段**（`REQ-025`）。
- Input/output and state mapping: 见 Spec B `§9.2.x` 的请求契约。
- Error and edge behavior: 违反约束抛 `KnowledgeApplicationException`。
- Standards impact: `MC-NAME-001`（`Command`）、`MC-MODEL-001`、`MC-VALID-001`、`MC-SCOPE-001`。
- Literal rule enforcement: `Rule 1`、`Rule 2`、`Rule 3`、`Rule 11`。
- Implementation pseudocode:

```java
public record CreateKnowledgeBaseCommand(@NotBlank @Pattern(regexp = "[A-Za-z0-9._-]{2,64}") String code,
                                         @NotBlank @Size(max = 128) String name,
                                         @Size(max = 512) String description,
                                         @NotBlank String embeddingModel,
                                         @NotNull ChunkingStrategyEnum chunkStrategy,
                                         @NotNull @Valid ChunkingConfigCommand chunkConfig) { }
```

- Verification contribution: `TEST-010`。
- After this file: 命令就绪。

#### File 3 — `CREATE .../application/knowledge/config/{KnowledgeRuntimeProperties,KnowledgeApplicationConfiguration}.java`

- Purpose: 类型化运行时属性与用例 Bean 装配。
- Symbols: `KnowledgeRuntimeProperties`（`agent.knowledge.runtime.*` 与 `tenant.default-id`）与 `@Configuration` 的容量服务 Bean。
- Repository evidence: `DeepResearchConfigurationProperties` 与 `DeepResearchApplicationConfiguration`。
- Dependencies and consumers: Manage 与过滤器。
- Why now: Manage 的配置来源。
- Contract/signature changes: 新增键与 Bean 名 `knowledgeRuntimeProperties`。
- Input/output and state mapping: 配置 → 属性。
- Error and edge behavior: 越界抛 `KnowledgeApplicationException`。
- Standards impact: `MC-CONFIG-001`、`MC-BEAN-001`、`MC-MODEL-001`、`MC-SCOPE-001`。
- Literal rule enforcement: `Rule 3`、`Rule 4`、`Rule 7`、`Rule 10`（`Duration`）、`Rule 11`。
- Implementation pseudocode:

```java
@ConfigurationProperties(prefix = "agent.knowledge")
@Validated
public record KnowledgeRuntimeProperties(@Valid Runtime runtime, @Valid Tenant tenant) {
    public record Runtime(@Min(1) @Max(32) int qaMaxConcurrent, @Positive long maxUploadBytes,
                          @Min(1) int maxDocumentsPerBase, @NotNull Duration qaMaxDuration) { }
    public record Tenant(@PositiveOrZero long defaultId) { }
}
```

- Verification contribution: `TEST-010` 与 Step 12 的键一致性。
- After this file: 属性就绪。

#### File 4 — `CREATE .../application/knowledge/service/KnowledgeQaCapacityService.java`

- Purpose: 问答的独立进程内并发限额。
- Symbols: 公平 `Semaphore` 与 `Lease implements AutoCloseable`。
- Repository evidence: `ResearchCapacityService` 的既有写法。
- Dependencies and consumers: 问答 Manage。
- Why now: 与用例同批。
- Contract/signature changes: 新增 `KnowledgeQaCapacityService`，其 `Lease` 是可关闭的许可凭据。
- Input/output and state mapping: 配置的并发上限 → `Semaphore` 的许可数；获取成功返回 `Lease`，失败返回空 `Optional`。
- Error and edge behavior: 获取失败立即返回而不是排队；`Lease` 重复关闭不产生副作用（由 `Semaphore` 的 release 语义保证）。
- Standards impact: `MC-NAME-001`（`Service`）、`MC-BEAN-001`、`MC-SCOPE-001`。
- Literal rule enforcement: `Rule 1`、`Rule 4`、`Rule 11`。
- Implementation pseudocode:

```java
public class KnowledgeQaCapacityService {
    private final Semaphore permits;
    public Optional<Lease> tryAcquire() { return permits.tryAcquire() ? Optional.of(new Lease(permits)) : Optional.empty(); }
    public record Lease(Semaphore permits) implements AutoCloseable {
        @Override public void close() { permits.release(); }
    }
}
```

- Verification contribution: `TEST-013` 与 `REQ-022`。
- After this file: 容量服务就绪。

#### File 5 — `CREATE .../application/knowledge/manage/{KnowledgeBaseManage,KnowledgeDocumentManage,KnowledgeQaManage}.java`

- Purpose: 三个用例入口接口。
- Symbols: 三个接口，方法签名与 Spec B `§9.2.x` 一致。
- Repository evidence: `DeepResearchManage` 的接口写法。
- Dependencies and consumers: adapter 的 Controller。
- Why now: 实现类的类型前提。
- Contract/signature changes: 新增接口。
- Input/output and state mapping: Command → BO 或事件。
- Error and edge behavior: 见实现类。
- Standards impact: `MC-NAME-001`（`Manage`）、`MC-SCOPE-001`。
- Literal rule enforcement: `Rule 1`、`Rule 11`。
- Implementation pseudocode:

```java
public interface KnowledgeDocumentManage {
    KnowledgeDocumentBO upload(@Valid UploadKnowledgeDocumentCommand command);
    KnowledgeDocumentBO get(Long documentId);
    PageResult<KnowledgeDocumentBO> page(Long knowledgeBaseId, int page, int size, DocumentIngestStatusEnum status, String keyword);
    KnowledgeDocumentBO reingest(Long documentId);
    void delete(Long documentId);
}
```

- Verification contribution: `TEST-010`。
- After this file: 接口就绪。

#### File 6 — `CREATE .../application/knowledge/manage/impl/{KnowledgeBaseManageImpl,KnowledgeDocumentManageImpl,KnowledgeQaManageImpl}.java`

- Purpose: 用例编排：校验、事务、补偿、状态与容量。
- Symbols: 三个 `@Service("...")` + `@Validated` + `@RequiredArgsConstructor` + `@Slf4j` 类。
- Repository evidence: `DeepResearchManageImpl` 的注解与编排写法。
- Dependencies and consumers: Controller。
- Why now: 接口与全部下游就位。
- Contract/signature changes: 实现三个接口；上传方法带 `@Transactional`。
- Input/output and state mapping: 文件写入与抽取在事务外，文档行与 outbox 行在事务内；事务失败补偿删除文件。
- Error and edge behavior: 见 Spec B `§7.3.4`；`reingest` 非终态抛冲突；删除时非终态抛 409。
- Standards impact: `MC-VALID-001`、`MC-BEAN-001`、`MC-LOG-001`、`MC-PATTERN-001`、`MC-CONVERT-001`、`MC-TIME-001`、`MC-SCOPE-001`。
- Literal rule enforcement: `Rule 1`、`Rule 2`、`Rule 3`、`Rule 4`、`Rule 9`、`Rule 10`、`Rule 11`。
- Implementation pseudocode:

```java
@Slf4j
@Service("knowledgeDocumentManage")
@Validated
@RequiredArgsConstructor
public class KnowledgeDocumentManageImpl implements KnowledgeDocumentManage {
    private final @Qualifier("ragDocumentStorage") RagDocumentStorage storage;
    private final @Qualifier("ragExtractionService") RagExtractionService extractionService;
    private final @Qualifier("knowledgeDocumentRepository") KnowledgeDocumentRepository documentRepository;
    private final @Qualifier("transactionalOutbox") TransactionalOutbox outbox;
    private final @Qualifier("knowledgeRuntimeProperties") KnowledgeRuntimeProperties properties;

    @Override public KnowledgeDocumentBO upload(@Valid UploadKnowledgeDocumentCommand command) {
        // 1. 事务外：存储原文件，抽取文本
        // 2. 事务内：insert 文档行 + enqueue(channel=rag-ingest, payload={tenantId, documentId})
        // 3. 事务失败：补偿删除已写文件
    }
}
```

- Verification contribution: `TEST-010`。
- After this file: Step 8 完成（含异常类与 `package-info.java`）。

- Validation working directory: `/Users/mario/SelfProject/Egon-COLA`
- Verification command: `./mvnw -B -ntp -f egon-cola-archetypes/source-projects/egon-cola-source-agent/pom.xml clean verify`
- Expected result: 退出码 0；`KnowledgeManageTest` 通过。
- Failure returns to: File 6（事务边界或补偿）。
- Completion criteria: `REQ-002`、`REQ-003`、`REQ-005`、`REQ-008`、`REQ-022` 的用例部分有证据。
- Rollback: 回退 application 的 knowledge 子树。
- Commit paths: `...-application/src/test/java/.../application/knowledge/KnowledgeManageTest.java`; `.../application/knowledge/command/*.java`; `.../application/knowledge/config/{KnowledgeRuntimeProperties,KnowledgeApplicationConfiguration}.java`; `.../application/knowledge/service/KnowledgeQaCapacityService.java`; `.../application/knowledge/manage/{KnowledgeBaseManage,KnowledgeDocumentManage,KnowledgeQaManage}.java`; `.../application/knowledge/manage/impl/{KnowledgeBaseManageImpl,KnowledgeDocumentManageImpl,KnowledgeQaManageImpl}.java`
- Commit: `feat(agent-archetype): add the knowledge use cases`

在实施期修正（事务边界的落点与两个协作者）：plan File 6 把 `@Transactional` 直接写在 `KnowledgeDocumentManageImpl#upload` 上并注入 `TransactionalOutbox`。实际实现另建两个协作者 Bean——`service/KnowledgeIngestQueueService`（`storeAndEnqueue(document, traceId)`：insert 文档行 + 入队，同一事务；`resetAndEnqueue(documentId, traceId) → boolean`：把"终态重置为 PENDING"与入队放进同一事务，返回 `false` 表达 CAS 失败）与 `service/KnowledgeRemovalService`（`deleteStoredFile` 尽力而为、`removeDocument`、`removeBase` 各自成事务）。原因有二：`@Transactional` 由代理施加，用例自调用自身的注解方法不产生新事务，而组件契约要求 `enqueue` 处于活跃事务中；文件落盘与文本解析留在事务外，避免事务跨越慢 IO（plan File 6 的"文件写入与抽取在事务外"保持不变）。`KnowledgeRemovalService` 的级联删除按"先删存储文件与向量、后删数据库行"排列，使失败时留下的是可见的、可重试的孤儿而非不可见的残留。

在实施期修正（共享的通道常量回改 Step 7 已提交文件）：`common/knowledge/KnowledgeIngestChannel` 承载通道名 `rag-ingest`、信封版本 `"1"` 与三个载荷字段名（`schemaVersion`、`documentId`、`tenantId`）。生产者是本 Step 的 application 用例、消费者是 Step 7 已提交的 infrastructure handler，两者互不可见，字面量各写一份会在改名后只以"投递失败"的形式暴露。故 `KnowledgeIngestDeliveryHandler` 改为引用同一常量（`CHANNEL` 与两个字段名改引用、私有 `SCHEMA_VERSION` 常量删除），行为与载荷形状不变——本 Step 因此修改 Step 7 已提交的文件，且该文件不在 plan 的 Commit paths 之内。

在实施期修正（载荷断言）：plan File 1 的伪代码用 `verify(transactionalOutbox).enqueue(argThat(payload -> payload.contains("rag-ingest")))`；实际载荷里不含通道名（通道在信封上），断言改为按 `KnowledgeIngestChannel` 的字段名比对整个 payload 映射 `{schemaVersion:"1", documentId, tenantId}`（`tenantId` 由 `REQ-026`/`DEC-013` 补入，见 Step 7 记述）。

在实施期修正（命令载体与分页）：`UploadKnowledgeDocumentCommand.content` 是 `byte[]` 而非 plan 的流——原文件要先落存储、再解析一次，流只能读一次；`upload` 在两次读取之前按 `maxUploadBytes` 限长，缓冲因此有界。分页返回用仓库既有的 `PageResultRecord`（Step 5 已记为偏差），命令一律带 `@NotBlank` 的 `traceId` 以支撑 Spec B `§9.2` 的错误体关联。plan 未列而本 Step 一并落地的域载体：`KnowledgeQaEvent`/`KnowledgeQaEventTypeEnum`/`KnowledgeRetrievedChunkBO`/`KnowledgeAnswerTaskBO`/`KnowledgeAnswerGateway`/`KnowledgeAnswerRunService` 与 `KnowledgeVectorGateway#deleteDocument`（原因见 Step 4/6 的记述：Spec B 行 549 与 `AgentArchitectureTest` 的门禁冲突，用户已批准"加 domain 端口"方案）；`KnowledgeQaStageEnum` 未新建，事件阶段由 `KnowledgeQaEventTypeEnum` 表达。

在实施期修正（`@Validated` 不落在 Manage 实现上）：plan File 6 要求实现类带 `@Validated`。实际实现改为"接口方法参数 `@Valid` + 实现内显式 `ValidationUtils.validate(...)`"：两者并存时方法级校验异常由容器抛出，绕过实现里把违规映射为 `KnowledgeApplicationException(KNOWLEDGE_VALIDATION_ERROR)` 的分支，规范要求的错误码与字段表将不可达。校验仍在边界完成，`Rule 2` 的实现方式不变。

在实施期修正（提取路由前置检查，`TEST-010`）：Spec B `TEST-010` 要求"无可用提取器 → 400 `KNOWLEDGE_EXTRACTOR_MISSING`，且存储与仓储不被调用"，故 `upload` 在 `storeOriginal` 之前新增 `requireExtractor`：用组件导出的具名 Bean `ragDocumentExtractorRegistry` 按同一 `mimeType`/`fileName` 询问，命中结果与 `RagExtractionService` 的判定一致，只是提前到"尚未写入任何东西"之前；`extract` 中原用于捕获 `RagExtractorMissingException` 的分支因此不可达而被删除（避免 `801c85949` 已修过的"不可达分支"缺陷类）。

在实施期修正（失败补偿与"无部分持久化"）：解析失败 → 补偿删除已写入的原文件 → 500 `KNOWLEDGE_INTERNAL_ERROR`；行/消息事务失败 → 同一次补偿删除 → 500（Spec B "无部分持久化"）；基座级联删除失败 → 500 `KNOWLEDGE_INTERNAL_ERROR` 且状态未变。三处都落在实现里。补偿删除自身失败不覆盖原始失败：`KnowledgeRemovalService.deleteStoredFile` 捕获存储异常并记 `outcome=ORPHANED reason=<异常类名>`；Spec B 行 633 的"补偿记录"实体在规范中没有定义，实现只落日志（**留待审计**：规范未定义的补偿记录载体）。

在实施期修正（阈值后置过滤，留待审计）：冻结的域端口 `KnowledgeVectorGateway#retrieve` 不带阈值参数，用例在组件返回后按 `similarityThreshold` 后置过滤（`score == null` 仅在阈值 ≤ 0 时通过）；问答路径取 `ACCEPT_EVERY_SCORE = 0.0`（`API-012` 无阈值参数），`topK` 为 null 时传 0 让组件的默认值（8）生效。**留待审计**：后置过滤与组件的 top-K 截断相互作用，"先截断、后过滤导致结果不足 topK"是规范未定义的行为。

在实施期修正（`KNOWLEDGE_MODEL_NOT_REGISTERED` 的时机）：plan 未含建库时的模型注册校验，`API-011`/`API-012` 的错误表也没有该码。实现按"检索时才可能失败"处理：`KnowledgeQaManageImpl` 把 `RagModelNotRegisteredException` 映射为 `KNOWLEDGE_MODEL_NOT_REGISTERED`，其余 `RagException` 映射为 503 `KNOWLEDGE_DEPENDENCY_UNAVAILABLE`；流建立之后才发生的失败由网关以流内 `knowledge.failed` 表达（`API-012` 的语义：建立前 503、建立后流内终态）。**留待审计**（错误表缺口）。

在实施期修正（容量许可只归还一次）：plan File 4 的伪代码把 `Lease` 写成裸 record，每次 `close()` 都会 `release()`，重复关闭会把并发上限悄悄抬高。实现改为 `AtomicBoolean` 守卫的 `close()`；`KnowledgeQaManageImpl.ManagedRunService#cancel()` 与之配套，用同一个 `terminal` 标志与 `cancelled` 标志保证"终态事件已释放"与"取消释放"只发生一次。

在实施期修正（尚未消费的配置键）：`agent.knowledge.runtime.qa-max-duration` 已绑定并在 `KnowledgeRuntimeProperties` 的紧凑构造器里校验，但用例层不设超时；按计划由 Step 11 的 SSE 发射器消费。**留待 Step 11**。

在实施期修正（基座删除的并发缺口，留待审计）：`KnowledgeBaseManageImpl#delete` 先守卫"无在途文档"再级联删除，但 `KnowledgeBaseRepository#softDelete` 返回 `void`，无法在数据库侧对"守卫与删除之间新插入的文档"做 CAS，该窗口留给最终审计（`softDelete` 的形态是 Step 5 的既有偏差）。

在实施期修正（组件的联合过滤写法）：`FilterExpressionBuilder.Op` 只提供 `build()`，`eq/and` 都在 `FilterExpressionBuilder` 上，故 `RagKnowledgeVectorGateway#deleteDocument` 的"文档键 ∧ 集合键"必须写成 `filters.and(filters.eq(DOCUMENT_ID, …), filters.eq(COLLECTION_ID, …)).build()`；组件自身的删除只用单条件，没有可照抄的联合写法。集合条件不是装饰：它保证一个基座的标识符不会触及另一个基座的分块。

在实施期修正（测试与离线假件）：plan File 1 列 4 个方法，实际 19 个——四个 plan 命名方法保留，其余覆盖路由前置检查、大小上限、解析补偿、重新入队的两条路径、文档与基座级联删除、不可变字段拒绝、检索的阈值与未知分数、问答的许可生命周期与流建立前的 503。假件全部手写（`DeepResearchManageImplTest` 的风格），不用 Mockito；`RoutingExtractionService` 复刻组件的路由语义。`application/pom.xml` 另新增三条依赖：组件 rag starter、组件 outbox starter（用例直接引用它们的类型）与 `spring-tx`（`@Transactional` 的来源）——前两条是 plan 的"依赖登记"Step 未列的使用点。starter 的 `DeepResearchApplicationTest#FakeAgentDependencies` 另补五个假件（`knowledgeRagVectorStore`、`ragDocumentStorage`、`ragExtractionService`、`ragDocumentExtractorRegistry`、`transactionalOutbox`）：离线 profile 关了 rag 与 outbox，本 Step 的用例依赖必须由假件补齐（Step 6/7 为同类依赖已留痕）。

- Commit paths 补充：除 plan 列出的文件外，本次提交还含 `...-application/pom.xml`、`...-application/src/main/java/.../application/knowledge/{exception,service}/*.java`、`...-application/src/main/java/.../application/knowledge/package-info.java` 及六个子包的 `package-info.java`、`...-application/src/test/java/.../application/knowledge/package-info.java`、`...-common/src/main/java/.../common/error/KnowledgeErrorCodeEnum.java`、`...-common/src/main/java/.../common/knowledge/KnowledgeIngestChannel.java`、domain 的 `KnowledgeAnswerGateway`/`KnowledgeAnswerTaskBO`/`KnowledgeQaEvent`/`KnowledgeQaEventTypeEnum`/`KnowledgeRetrievalBO`/`KnowledgeRetrievedChunkBO`/`knowledge/service/*` 与 `KnowledgeVectorGateway#deleteDocument`、infrastructure 的 `KnowledgeAnswerChatModelGateway` 与 `RagKnowledgeVectorGateway#deleteDocument`、以及 `KnowledgeIngestDeliveryHandler` 对共享通道常量的改引用。

### Step 9 — adapter：知识库接口

- Requirements: `REQ-002`, `REQ-023`
- Dependencies: Step 8
- Baseline state: 用例就位；adapter 只有 `research` 与既有过滤器/处理器。
- Observable outcome: API-001 至 API-005 各自可用并带完整 OpenAPI 注解。
- End state: 知识库的三个边界载体组与一个 Controller 就位。
- Test-first gate: `Required` — 控制器不存在。
- Manual Checks: `MC-NAME-001`, `MC-VALID-001`, `MC-CONVERT-001`, `MC-JSON-001`, `MC-BEAN-001`, `MC-LOG-001`, `MC-SCOPE-001`, `MC-TEST-001`
- Literal Rules: `Rule 1`, `Rule 2`, `Rule 3`, `Rule 4`, `Rule 6`, `Rule 11`
- Ordered files:

#### File 1 — `CREATE ...-adapter/src/test/java/.../adapter/knowledge/KnowledgeBaseControllerTest.java`

- Purpose: 锁定 API-001..005 的状态码、校验与不可修改字段拒绝。
- Symbols: `creates_a_knowledge_base()`、`rejects_unknown_keys_on_update()`、`returns_conflict_on_duplicate_code()`、`returns_not_found_after_deletion()`。
- Repository evidence: `DeepResearchControllerTest` 的 MockMvc 风格。
- Dependencies and consumers: 消费 Controller 与 Manage fake。
- Why now: 接口契约先定。
- Contract/signature changes: 引入 Controller 与边界载体。
- Input/output and state mapping: 请求 → 状态码与响应体。
- Error and edge behavior: 400/401/404/409 各自断言。
- Standards impact: `MC-VALID-001`、`MC-JSON-001`、`MC-SCOPE-001`、`MC-TEST-001`。
- Literal rule enforcement: `Rule 2`、`Rule 6`、`Rule 11`。
- Implementation pseudocode:

```java
@Test void rejects_unknown_keys_on_update() throws Exception {
    mockMvc.perform(put("/api/v1/knowledge-bases/{id}", 1L).contentType(APPLICATION_JSON)
            .header(API_KEY_HEADER, KEY).content("{\"name\":\"n\",\"embeddingModel\":\"x\"}"))
        .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("KNOWLEDGE_IMMUTABLE_FIELD"));
}
```

- Verification contribution: `TEST-011`。
- After this file: 编译失败。

#### File 2 — `CREATE .../adapter/knowledge/dto/{CreateKnowledgeBaseRequest,UpdateKnowledgeBaseRequest}.java`

- Purpose: HTTP 边界载体。
- Symbols: 两个 record，含 Jakarta 注解与 `@Schema`。
- Repository evidence: `StartDeepResearchRequest` 的写法。
- Dependencies and consumers: Controller 与 Converter。
- Why now: Controller 的入参类型。
- Contract/signature changes: 新增载体；未知属性拒绝。
- Input/output and state mapping: 见 Spec B `§9.2.1` 的请求体。
- Error and edge behavior: 违反约束 → 400。
- Standards impact: `MC-NAME-001`（`Request`）、`MC-MODEL-001`、`MC-JSON-001`、`MC-VALID-001`。
- Literal rule enforcement: `Rule 1`、`Rule 2`、`Rule 3`、`Rule 6`、`Rule 11`。
- Implementation pseudocode:

```java
@JsonIgnoreProperties(ignoreUnknown = false)
public record CreateKnowledgeBaseRequest(@NotBlank @Pattern(regexp = "[A-Za-z0-9._-]{2,64}") String code,
                                         @NotBlank @Size(max = 128) String name,
                                         @Size(max = 512) String description,
                                         @NotBlank String embeddingModel,
                                         @NotNull ChunkingStrategyEnum chunkStrategy,
                                         @NotNull @Valid ChunkingConfigRequest chunkConfig) { }
```

- Verification contribution: `TEST-011`。
- After this file: DTO 就绪。

#### File 3 — `CREATE .../adapter/knowledge/vo/KnowledgeBaseVO.java`

- Purpose: HTTP 出口载体。
- Symbols: record，含冻结配置与计数。
- Repository evidence: `DeepResearchEventVO` 的 `@JsonInclude(NON_NULL)` 用法。
- Dependencies and consumers: Controller 与 VoConverter。
- Why now: Controller 的出参类型。
- Contract/signature changes: 新增载体。
- Input/output and state mapping: 见 Spec B `§9.2.1` 的成功响应。
- Error and edge behavior: 无。
- Standards impact: `MC-NAME-001`（`VO`）、`MC-MODEL-001`、`MC-JSON-001`、`MC-TIME-001`。
- Literal rule enforcement: `Rule 1`、`Rule 3`、`Rule 6`、`Rule 10`、`Rule 11`。
- Implementation pseudocode:

```java
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(name = "KnowledgeBaseVO")
public record KnowledgeBaseVO(Long knowledgeBaseId, String code, String name, String description,
                              String embeddingModel, ChunkingStrategyEnum chunkStrategy,
                              Map<String, Object> chunkConfig, long documentCount, String status,
                              Instant createdAt, Instant updatedAt) { }
```

- Verification contribution: `TEST-011`。
- After this file: VO 就绪。

#### File 4 — `CREATE .../adapter/knowledge/converter/{KnowledgeCommandConverter,KnowledgeVoConverter}.java`

- Purpose: 边界到用例、用例到边界的转换。
- Symbols: 两个 `@Mapper` 接口，`extends BaseConverter<S,T>`。
- Repository evidence: `ResearchEventConverter` 的 `INSTANCE = Mappers.getMapper` 写法。
- Dependencies and consumers: Controller。
- Why now: Controller 依赖它们。
- Contract/signature changes: 新增转换器；逐字段 `@Mapping`。
- Input/output and state mapping: Request → Command；BO → VO。
- Error and edge behavior: 未映射字段编译期失败。
- Standards impact: `MC-CONVERT-001`、`MC-SCOPE-001`。
- Literal rule enforcement: `Rule 1`、`Rule 3`、`Rule 11`。
- Implementation pseudocode:

```java
@Mapper(unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface KnowledgeCommandConverter extends BaseConverter<CreateKnowledgeBaseRequest, CreateKnowledgeBaseCommand> {
    KnowledgeCommandConverter INSTANCE = Mappers.getMapper(KnowledgeCommandConverter.class);
    @Mapping(target = "code", source = "code") /* 逐字段 */ CreateKnowledgeBaseCommand toTarget(CreateKnowledgeBaseRequest source);
}
```

- Verification contribution: `TEST-011`。
- After this file: 转换器就绪。

#### File 5 — `CREATE .../adapter/knowledge/controller/KnowledgeBaseController.java`

- Purpose: API-001 至 API-005。
- Symbols: `@RestController("knowledgeBaseController")` + `@RequestMapping("/api/v1/knowledge-bases")`；五个方法与 OpenAPI 注解。
- Repository evidence: `DeepResearchController` 的映射、注解与 `SseEmitter` 之外的 MVC 写法。
- Dependencies and consumers: API client。
- Why now: 全部边界载体与用例就位。
- Contract/signature changes: 新增五个端点，`operationId` 与 Spec B `§9.2.x` 一致。
- Input/output and state mapping: 见 Spec B `§9.2.1`-`§9.2.5`。
- Error and edge behavior: 400/401/404/409 由既有错误体与全局处理器映射。
- Standards impact: `MC-VALID-001`、`MC-JSON-001`、`MC-BEAN-001`、`MC-LOG-001`、`MC-SCOPE-001`。
- Literal rule enforcement: `Rule 1`、`Rule 2`、`Rule 4`、`Rule 6`、`Rule 11`。
- Implementation pseudocode:

```java
@Tag(name = "Knowledge Base")
@RestController("knowledgeBaseController")
@RequestMapping("/api/v1/knowledge-bases")
@RequiredArgsConstructor
public class KnowledgeBaseController {
    private final @Qualifier("knowledgeBaseManage") KnowledgeBaseManage knowledgeBaseManage;

    @Operation(operationId = "createKnowledgeBase", summary = "Create a knowledge base")
    @SecurityRequirement(name = "researchApiKey")
    @PostMapping
    public KnowledgeBaseVO create(@Valid @RequestBody CreateKnowledgeBaseRequest request) { /* 转换 + 调用 */ }
    // list / get / update / delete 同形
}
```

- Verification contribution: `TEST-011`。
- After this file: Step 9 完成（含错误码与 `package-info.java`）。

- Validation working directory: `/Users/mario/SelfProject/Egon-COLA`
- Verification command: `./mvnw -B -ntp -f egon-cola-archetypes/source-projects/egon-cola-source-agent/pom.xml clean verify`
- Expected result: 退出码 0；`KnowledgeBaseControllerTest` 通过。
- Failure returns to: File 5（映射或注解）。
- Completion criteria: `REQ-002` 的接口部分有证据。
- Rollback: 回退 adapter 的知识库接口文件。
- Commit paths: `...-adapter/src/test/java/.../adapter/knowledge/KnowledgeBaseControllerTest.java`; `.../adapter/knowledge/dto/{CreateKnowledgeBaseRequest,UpdateKnowledgeBaseRequest}.java`; `.../adapter/knowledge/vo/KnowledgeBaseVO.java`; `.../adapter/knowledge/converter/{KnowledgeCommandConverter,KnowledgeVoConverter}.java`; `.../adapter/knowledge/controller/KnowledgeBaseController.java`
- Commit: `feat(agent-archetype): expose the knowledge base api`

在实施期修正（未知字段的拒绝必须落在类型上）：Spec B 行 1329 要求"探测不可依赖序列化层静默丢弃字段"，行 1349 给 `UpdateKnowledgeBaseRequest` 定的验证是"序列化与 400 断言"。类级 `@JsonIgnoreProperties(ignoreUnknown = false)` 做不到这件事——"未知属性是否抛错"由 mapper 的 `FAIL_ON_UNKNOWN_PROPERTIES` 特性决定，Spring Boot 的应用 mapper 与 standalone MockMvc 的 mapper 都默认关闭它，故该注解只表达意图。两个 Request 因此各带一个 `StdDeserializer`（白名单 + 逐字段类型检查），Edit 体复用 Create 体的解析入口（两者字段集完全相同），与仓库既有的同类解法一致（`StartDeepResearchRequest.Deserializer` 处理的正是同一问题；research 的 400 断言也靠它而不是注解）。契约的第二个后果：显式 `null` 与缺省在 `name`/`description` 上同义（`PUT` 整替换），在四个探针上也同义（都表示"未提供"）。

在实施期修正（知识边界自己的错误处理器）：`ExceptionHandlerExceptionResolver` 按 `@Order` 取第一个"适用且有匹配方法"的 advice，而不受限的 `DeepResearchGlobalExceptionHandler` 不能被知识域复用——它对 `MethodArgumentNotValidException`、`Exception` 等共有异常只认 research 的错误码。故新增 `adapter/knowledge/handler/KnowledgeGlobalExceptionHandler`：`@RestControllerAdvice(name = "knowledgeGlobalExceptionHandler", basePackages = "…adapter.knowledge")` + `@Order(Ordered.HIGHEST_PRECEDENCE)`，把 `KnowledgeErrorCodeEnum` 映射到 Spec B `§9.2` 的状态码（400/404/406/409/413/429/503/500），`KNOWLEDGE_CAPACITY_EXHAUSTED` 附 `Retry-After: 5`，错误体复用既有 `DeepResearchErrorResponse`（`REQ-014` 的"复用既有错误体"）。plan 未列该文件；顺序限定是必需的，否则知识端点的非法请求体会落到 research 的处理器上并渲染成 `RESEARCH_*` 错误码。

在实施期修正（`documentCount` 端口方法）：`KnowledgeBaseVO` 的 `documentCount` 既不在 `KnowledgeBaseBO` 上，adapter 也够不到仓储（不得依赖 infrastructure）。实现改为在 `KnowledgeBaseManage` 上新增 `long documentCount(Long)`，由 `KnowledgeBaseManageImpl` 用既有 `KnowledgeDocumentRepository#countByKnowledgeBaseId` 实现，控制器把它交给 VO 转换器。该文件不在 plan 的 Commit paths 之内。列表因此按行逐个计数（页大小上限 100，N+1 有界）；规范没有批量计数的端口，**留待审计**。建库与更新也走同一条渲染路径，故新库返回 `documentCount=0` 而不是省略该字段——同一 VO 的同一个字段在任何响应里都同义。

在实施期修正（`minChunkChars` 的下界）：plan 的 File 2 伪代码与 Step 8 的 `CreateKnowledgeBaseCommand.ChunkingConfigCommand` 把它定成 `@Min(1)`，Spec B `§9.2.1` 的字段表给的是 0–4096（域在 `KnowledgeChunkConfigBO` 里把 ≤0 归一为 1）。故命令载体放宽为 `@Min(0)`，与请求载体一致：否则 0（契约允许）会被边界拒绝成 400。

在实施期修正（分页外壳与 spec 的列表字段名不同）：`API-002` 的响应在 spec 里形如 `{items, page, size, totalElements, totalPages, hasNext}`，实现用的是组件既有的 `PageResultRecord`（`{success, code, status, message, records, page{total, pageNo, pageSize, pages, hasNext, hasPrevious}, traceId, timestamp}`，Step 5 已记同一偏差）。列表行复用 `KnowledgeBaseVO`（plan 的 File 3 只定义一个 VO），因此列表行比 spec 的列表字段表多出 `description`/`chunkConfig`/`embeddingModel` 等字段——是超集，不是冲突。**留待审计**（列表外壳与字段名）。

在实施期修正（`traceId`、标识类型与单一 VO）：`KnowledgeBaseVO` 带 `traceId`（Spec B 的成功响应字段表列了它，plan 的 File 3 伪代码漏了），值取 `ResearchTraceFilter` 的请求属性（同一过滤器已把它写进响应头与 MDC）。标识在实现里是 `Long`（`EgonModel.id` 的 bigint），而 Spec B 把 `knowledgeBaseId` 声明为 `String` 且示例是 `kb-01J5K9`：接口输出数字、路径也只接受能表示正数的数字标识，能通过路径模式但不能表示正数的 token（如 `kb-01J5K9`）按 400 `KNOWLEDGE_VALIDATION_ERROR` 的 `fieldErrors.knowledgeBaseId` 拒绝，而不是用 404 谎称资源不存在。**留待审计**（声明类型与示例）。

在实施期修正（查询参数校验与字段名）：页面参数用 Spring 6.1+ 的内建方法校验（`@RequestParam` 上的 `@Min`/`@Max`/`@Size`，无需在控制器类上加 `@Validated`），失败是 `HandlerMethodValidationException`；处理器从 `getParameterValidationResults()` 取每个参数的 `getResolvableErrors()`，字段名优先取 `@PathVariable`/`@RequestParam` 的显式名字（`MethodParameter#getParameterName` 依赖编译期 `-parameters`，不作为唯一依据）。处理器另映射 `MethodArgumentTypeMismatchException`（查询参数类型不符 → 400）与不支持的媒体类型（415），避免它们落到 `Exception` 兜底变成 500。

在实施期修正（`KnowledgeBaseControllerTest` 的规模）：plan 列 4 个方法名，实际 13 个——四个保留，其余覆盖列表外壳、详情、可编辑字段与冻结配置的分离、不可定义字段的 400、字段约束的 400、分页范围、格式非法的标识、基座繁忙的 409、删除后的 204 与随后 404、无 API Key 的 401（带 `WWW-Authenticate: ApiKey`）。用例是手写假件（`KnowledgeManageTest` 与研究测试的风格），不使用 Mockito；组装用 `standaloneSetup` + `LocalValidatorFactoryBean` + 两个既有过滤器。

- Commit paths 补充：除 plan 列出的文件外，本次提交还含 `...-adapter/src/main/java/.../adapter/knowledge/{package-info.java,dto/package-info.java,vo/package-info.java,converter/package-info.java,controller/package-info.java}`、`.../adapter/knowledge/handler/{KnowledgeGlobalExceptionHandler,package-info}.java`、`...-adapter/src/test/java/.../adapter/knowledge/package-info.java`、以及 application 的 `.../knowledge/manage/KnowledgeBaseManage.java`、`.../knowledge/manage/impl/KnowledgeBaseManageImpl.java`、`.../knowledge/command/CreateKnowledgeBaseCommand.java`。

### Step 10 — adapter：文档接口

- Requirements: `REQ-003`, `REQ-005`, `REQ-007`, `REQ-023`
- Dependencies: Step 9
- Baseline state: 知识库接口就位；文档接口不存在。
- Observable outcome: API-006 至 API-010 可用，上传返回 202，详情只返回文本长度。
- End state: 文档边界载体与 Controller 就位。
- Test-first gate: `Required` — 控制器不存在。
- Manual Checks: `MC-NAME-001`, `MC-VALID-001`, `MC-CONVERT-001`, `MC-JSON-001`, `MC-BEAN-001`, `MC-LOG-001`, `MC-SCOPE-001`, `MC-TEST-001`
- Literal Rules: `Rule 1`, `Rule 2`, `Rule 3`, `Rule 4`, `Rule 6`, `Rule 11`
- Ordered files:

#### File 1 — `CREATE ...-adapter/src/test/java/.../adapter/knowledge/KnowledgeDocumentControllerTest.java`

- Purpose: 锁定上传 202、详情不返回文本、重处理冲突与删除。
- Symbols: `accepts_a_multipart_upload()`、`returns_content_length_not_content()`、`rejects_reingest_while_processing()`、`deletes_the_document()`。
- Repository evidence: 同 Step 9 File 1。
- Dependencies and consumers: 消费 Controller 与 Manage fake。
- Why now: 接口契约先定。
- Contract/signature changes: 引入文档 Controller 与载体。
- Input/output and state mapping: multipart → 202。
- Error and edge behavior: 400/404/409/413 各自断言。
- Standards impact: `MC-VALID-001`、`MC-JSON-001`、`MC-SCOPE-001`、`MC-TEST-001`。
- Literal rule enforcement: `Rule 2`、`Rule 6`、`Rule 11`。
- Implementation pseudocode:

```java
@Test void returns_content_length_not_content() throws Exception {
    mockMvc.perform(get("/api/v1/knowledge-documents/{id}", 1L).header(API_KEY_HEADER, KEY))
        .andExpect(status().isOk()).andExpect(jsonPath("$.contentChars").exists())
        .andExpect(jsonPath("$.content").doesNotExist());
}
```

- Verification contribution: `TEST-012`。
- After this file: 编译失败。

#### File 2 — `CREATE .../adapter/knowledge/vo/KnowledgeDocumentVO.java`

- Purpose: 文档出口载体。
- Symbols: record，含状态、分块数、失败码与 `contentChars`。
- Repository evidence: 同 Step 9 File 3。
- Dependencies and consumers: Controller。
- Why now: 出参类型。
- Contract/signature changes: 新增载体；**不包含 `content`**。
- Input/output and state mapping: 见 Spec B `§9.2.6` 与 `§9.2.8`。
- Error and edge behavior: 无。
- Standards impact: `MC-NAME-001`、`MC-MODEL-001`、`MC-JSON-001`、`MC-TIME-001`。
- Literal rule enforcement: `Rule 1`、`Rule 3`、`Rule 6`、`Rule 10`、`Rule 11`。
- Implementation pseudocode:

```java
@JsonInclude(JsonInclude.Include.NON_NULL)
public record KnowledgeDocumentVO(Long documentId, Long knowledgeBaseId, String displayName, String fileName,
                                  String mimeType, Long sizeBytes, DocumentIngestStatusEnum status, int chunkCount,
                                  Integer attemptCount, Integer contentChars, String errorCode, String errorMessage,
                                  Instant createdAt, Instant updatedAt) { }
```

- Verification contribution: `TEST-012`。
- After this file: VO 就绪。

#### File 3 — `CREATE .../adapter/knowledge/converter/KnowledgeDocumentVoConverter.java`

- Purpose: 文档 BO 到 VO 的转换。
- Symbols: `@Mapper` 接口，`extends BaseConverter<KnowledgeDocumentBO, KnowledgeDocumentVO>`。
- Repository evidence: 同 Step 9 File 4。
- Dependencies and consumers: Controller。
- Why now: Controller 依赖它。
- Contract/signature changes: 新增转换器；`contentChars` 由 `content` 长度派生，**不映射文本本身**。
- Input/output and state mapping: 逐字段显式 `@Mapping`。
- Error and edge behavior: 未映射字段编译期失败。
- Standards impact: `MC-CONVERT-001`、`MC-SCOPE-001`。
- Literal rule enforcement: `Rule 1`、`Rule 3`、`Rule 11`。
- Implementation pseudocode:

```java
@Mapper(unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface KnowledgeDocumentVoConverter extends BaseConverter<KnowledgeDocumentBO, KnowledgeDocumentVO> {
    @Mapping(target = "contentChars", expression = "java(target.content() == null ? null : target.content().length())")
    @Mapping(target = "content", ignore = true) /* 逐字段 */
    KnowledgeDocumentVO toTarget(KnowledgeDocumentBO source);
    @Mapping(target = "content", ignore = true) KnowledgeDocumentBO toSource(KnowledgeDocumentVO target);
}
```

- Verification contribution: `TEST-012`。
- After this file: 转换器就绪。

#### File 4 — `CREATE .../adapter/knowledge/controller/KnowledgeDocumentController.java`

- Purpose: API-006 至 API-010。
- Symbols: `@RestController("knowledgeDocumentController")`；上传为 `@PostMapping(consumes = MULTIPART_FORM_DATA_VALUE)`，返回 `ResponseEntity` 202。
- Repository evidence: 同 Step 9 File 5。
- Dependencies and consumers: API client。
- Why now: 载体与用例就位。
- Contract/signature changes: 新增五个端点；上传使用 `@RequestPart`。
- Input/output and state mapping: 见 Spec B `§9.2.6`-`§9.2.10`。
- Error and edge behavior: 400/404/409/413 由既有错误体映射。
- Standards impact: `MC-VALID-001`、`MC-JSON-001`、`MC-BEAN-001`、`MC-LOG-001`、`MC-SCOPE-001`。
- Literal rule enforcement: `Rule 1`、`Rule 2`、`Rule 4`、`Rule 6`、`Rule 11`。
- Implementation pseudocode:

```java
@Tag(name = "Knowledge Document")
@RestController("knowledgeDocumentController")
@RequiredArgsConstructor
public class KnowledgeDocumentController {
    @Operation(operationId = "uploadKnowledgeDocument") @SecurityRequirement(name = "researchApiKey")
    @PostMapping(path = "/api/v1/knowledge-bases/{knowledgeBaseId}/documents", consumes = MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<KnowledgeDocumentVO> upload(@PathVariable Long knowledgeBaseId,
            @RequestPart("file") MultipartFile file, @RequestPart(value = "displayName", required = false) String displayName) {
        return ResponseEntity.accepted().body(/* 转换 + 调用 */);
    }
}
```

- Verification contribution: `TEST-012`。
- After this file: Step 10 完成。

- Validation working directory: `/Users/mario/SelfProject/Egon-COLA`
- Verification command: `./mvnw -B -ntp -f egon-cola-archetypes/source-projects/egon-cola-source-agent/pom.xml clean verify`
- Expected result: 退出码 0；`KnowledgeDocumentControllerTest` 通过。
- Failure returns to: File 4（multipart 映射或状态码）。
- Completion criteria: `REQ-003`、`REQ-005`、`REQ-007` 的接口部分有证据。
- Rollback: 回退 adapter 的文档接口文件。
- Commit paths: `...-adapter/src/test/java/.../adapter/knowledge/KnowledgeDocumentControllerTest.java`; `.../adapter/knowledge/vo/KnowledgeDocumentVO.java`; `.../adapter/knowledge/converter/KnowledgeDocumentVoConverter.java`; `.../adapter/knowledge/controller/KnowledgeDocumentController.java`
- Commit: `feat(agent-archetype): expose the knowledge document api`

在实施期修正（表单字段不是 part）：plan 的 File 4 伪代码把 `displayName` 绑成 `@RequestPart(value = "displayName", required = false) String`。Spring 的 `RequestPartMethodArgumentResolver` 对非文件参数不做请求参数回退——`resolveName` 只在 part 与 `MultipartFile`/`Part` 家族之间查找，找不到就返回 `null`（`required = false` 时静默丢弃），因此这样绑定会永远得到 `null`，契约里的展示名会被无声忽略（测试先以此失败）。实现改为 `@RequestParam(name = "displayName", required = false)`：真正的 multipart 文本字段在容器层就是请求参数，这正是它与 MockMvc 的 `.param(...)` 同形的依据。文件仍用 `@RequestPart`。

在实施期修正（上传边界自己校验 part）：File 4 伪代码只说"转换 + 调用"。缺 `file`、空文件或无名文件若交给容器/用例，得到的是容器错误或约束违例文案；契约给 API-006 的行是"multipart 缺 `file` 或字段非法 → 400 `KNOWLEDGE_VALIDATION_ERROR` + `fieldErrors`"。故两个 part 都按 `required = false` 绑定，在控制器里逐项检查并用 `KnowledgeApplicationException.onField` 报出 `fieldErrors.file` 或 `fieldErrors.displayName`（展示名按契约"trim 后 1-255"，空白即非法而不是回落成文件名）。读 part 失败的 `IOException` 是本服务端的失败，报 500 `KNOWLEDGE_INTERNAL_ERROR`。文件大小与抽取路由仍在用例里（`max-upload-bytes`、`RagExtractorMissingException`），边界不拥有这两条规则。

在实施期修正（容器级 multipart 阈值与延迟解析）：契约允许 20MB 的上传，而 Spring Boot 默认的 `spring.servlet.multipart.max-file-size` 是 1MB——默认值下合规上传会先被容器拒掉。新增三个 `application.yml` 键：`max-file-size: 25MB`、`max-request-size: 26MB`、`resolve-lazily: true`。前两者只作滥用防线并高于契约上限，判定权仍归 `agent.knowledge.runtime.max-upload-bytes`（20MB ≤ 文件 ≤ 25MB 由用例给出契约里的 413）。`resolve-lazily` 是必需的：`DispatcherServlet.checkMultipart` 在处理器映射之前同步执行，立即解析的超限异常发生在"处理器未知"的时刻——按包限定的 knowledge advice 此时不被选中，响应会落到不限定的 research 处理器上变成 `RESEARCH_INTERNAL_ERROR`/500；延迟解析把异常挪到参数解析时，处理器已知，本域 advice 才可能渲染契约的 413。为此 `KnowledgeGlobalExceptionHandler` 补两个映射：`MaxUploadSizeExceededException` → 413 `KNOWLEDGE_FILE_TOO_LARGE`（`fieldErrors.file`）、`MultipartException` → 400 `KNOWLEDGE_VALIDATION_ERROR`。该文件与 `application.yml` 均不在 plan 的 Commit paths 之内；容器行为的最终确认需要一次真实上传（运行期由用户执行）。

在实施期修正（文档 VO 的反向映射无法成立）：`BaseConverter<S,T>` 两个方向都要有，而 `KnowledgeDocumentVO` 按契约**不带** `content`、`contentHash`、`storageType`、`storageKey`（§9.2.8 明说"不返回文本本身"，正文只以 `contentChars` 出现）。由 VO 重建的 `KnowledgeDocumentBO` 必须为内容指纹与存储位置编造取值，且构造器会拒绝空指纹；Step 9 的 `WIRE_TENANT_ID` 式常量在此没有对应物。故 `toSource` 手写并抛 `UnsupportedOperationException`：宁可让"表示不含正文"这件事在编译期之外仍然响亮，也不落一个看起来可持久化、实际什么都没描述的载体。方向始终是 BO → VO。

在实施期修正（一个文档 VO 服务三个接口，字段是超集）：`KnowledgeDocumentVO` 同时服务 API-006、API-008、API-009（契约明说 008 是 006 的表示加诊断字段、009 与 008 同形）。因此上传响应也带 `attemptCount`/`contentChars`，列表行也带 `fileName`/`knowledgeBaseId`/`errorMessage`——都是超集而非冲突（Step 9 已记同一取舍）。因为 `@JsonInclude(NON_NULL)`，契约示例里的 `"errorCode": null` 渲染为字段缺席而不是显式 `null`（Step 9 的 `description` 同理）。列表分页外壳沿用 `PageResultRecord`，与 spec 的 `items/page/size/totalElements/totalPages/hasNext` 字段名不同（既有偏差，Step 5 已记）。

在实施期修正（reingest 的空请求体与标识解析）：API-009 的参数表要求"请求体必须为空"并给了 400 行，故 `@RequestBody(required = false) String` 读入后非空白即报 `fieldErrors.body`；API-010 同样声明空请求体，但其错误表没有对应行、也没有既定错误码，故删除端点不检查（容器会忽略 DELETE 的请求体）。标识解析沿用 Step 9 的形状，但抽出 `field` 参数：同一控制器同时解析 `knowledgeBaseId` 与 `documentId`，字段名必须如实出现在 `fieldErrors` 里（契约把非数字标识定为 400 而不是 404）。两个控制器各持一份私有 `identifier(...)`：`MC-UTIL-001` 禁止新增 `*Utils`，复制的是 12 行边界规则；Step 11 出现第三个控制器时重新评估是否值得提为共享类型。

在实施期修正（`KnowledgeDocumentControllerTest` 的规模）：plan 列 4 个方法名，实际 18 个——四个保留，其余覆盖默认展示名、缺文件/空文件/空白展示名、知识库不存在、容器级与用例级两种 413、列表外壳与筛选透传、非法状态筛选、分页范围、非法标识、重处理成功与繁忙、重处理带体、删除后 404、无 API Key 的 401。用例同样是手写假件，不使用 Mockito。

- Commit paths 补充：除 plan 列出的四个文件外，本次提交还含 `.../adapter/knowledge/handler/KnowledgeGlobalExceptionHandler.java`（补两个 multipart 映射）与 `...-starter/src/main/resources/application.yml`（新增 `spring.servlet.multipart` 三键）。四个新文件都落在 Step 9 已建并已带 `package-info.java` 的包里，故本次没有新增包文档。

### Step 11 — adapter：检索、问答与租户作用域

- Requirements: `REQ-011`, `REQ-012`, `REQ-013`, `REQ-014`, `REQ-022`, `REQ-025`, `REQ-027`
- Dependencies: Step 9, Step 10
- Baseline state: 前两组接口就位；无检索问答端点，无租户过滤器。
- Observable outcome: 检索调试与问答 SSE 可用；请求线程与投递线程的租户都来自显式来源且被清理。
- End state: 检索问答端点、SSE 事件 VO 与租户过滤器就位。
- Test-first gate: `Required` — 控制器与过滤器不存在。
- Manual Checks: `MC-NAME-001`, `MC-VALID-001`, `MC-CONVERT-001`, `MC-JSON-001`, `MC-BEAN-001`, `MC-LOG-001`, `MC-TIME-001`, `MC-SCOPE-001`, `MC-TEST-001`
- Literal Rules: `Rule 1`, `Rule 2`, `Rule 3`, `Rule 4`, `Rule 6`, `Rule 10`, `Rule 11`
- Ordered files:

#### File 1 — `CREATE ...-adapter/src/test/java/.../adapter/knowledge/KnowledgeQaControllerTest.java`

- Purpose: 锁定检索不调用模型、SSE 事件顺序与终态唯一、容量与断连。
- Symbols: `retrieves_without_calling_the_chat_model()`、`emits_started_progress_completed_in_order()`、`emits_one_failed_on_dependency_failure()`、`returns_429_when_saturated()`、`releases_the_permit_on_disconnect()`。
- Repository evidence: `DeepResearchControllerTest` 的 SSE 测试写法。
- Dependencies and consumers: 消费 Controller 与 Manage fake。
- Why now: SSE 契约先定。
- Contract/signature changes: 引入检索问答 Controller 与事件 VO。
- Input/output and state mapping: 见 Spec B `§9.2.11` 与 `§9.2.12`。
- Error and edge behavior: 429 带 `Retry-After`；流内失败一个终态。
- Standards impact: `MC-VALID-001`、`MC-JSON-001`、`MC-SCOPE-001`、`MC-TEST-001`。
- Literal rule enforcement: `Rule 2`、`Rule 6`、`Rule 11`。
- Implementation pseudocode:

```java
@Test void emits_started_progress_completed_in_order() throws Exception {
    mockMvc.perform(post("/api/v1/knowledge-bases/{id}/chat", 1L).header(API_KEY_HEADER, KEY)
            .accept(TEXT_EVENT_STREAM).contentType(APPLICATION_JSON).content("{\"question\":\"q\"}"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("event:knowledge.started")))
        .andExpect(content().string(containsString("event:knowledge.completed")));
}
```

- Verification contribution: `TEST-013`。
- After this file: 编译失败。

#### File 2 — `CREATE .../adapter/knowledge/filter/KnowledgeTenantMdcFilter.java`

- Purpose: 在请求线程写入默认租户并在结束时清理。
- Symbols: `@Component("knowledgeTenantMdcFilter")` + `OncePerRequestFilter`。
- Repository evidence: `ResearchTraceFilter` 写 MDC 的既有写法。
- Dependencies and consumers: 全部 knowledge 请求。
- Why now: 数据访问路径全部就位后统一接入。
- Contract/signature changes: 新增过滤器；MDC 键与 `tenant-id.mdc-key` 配置对齐。
- Input/output and state mapping: 配置默认租户 → MDC。
- Error and edge behavior: `finally` 清理，避免线程池复用导致串租户。
- Standards impact: `MC-BEAN-001`、`MC-CONFIG-001`、`MC-SCOPE-001`。
- Literal rule enforcement: `Rule 4`、`Rule 11`。
- Implementation pseudocode:

```java
@Slf4j
@Component("knowledgeTenantMdcFilter")
@Order(Ordered.HIGHEST_PRECEDENCE + 2)
@RequiredArgsConstructor
public class KnowledgeTenantMdcFilter extends OncePerRequestFilter {
    private final @Qualifier("knowledgeRuntimeProperties") KnowledgeRuntimeProperties properties;
    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) {
        MDC.put(TENANT_MDC_KEY, String.valueOf(properties.tenant().defaultId()));
        try { chain.doFilter(request, response); } finally { MDC.remove(TENANT_MDC_KEY); }
    }
}
```

- Verification contribution: `TEST-013` 与 `REQ-025`。
- After this file: 租户作用域就绪。

#### File 3 — `CREATE .../adapter/knowledge/dto/{RetrieveKnowledgeRequest,AskKnowledgeBaseRequest}.java`

- Purpose: 检索与问答的入参载体。
- Symbols: 两个 record，含 Jakarta 注解与 `@Schema`。
- Repository evidence: 同 Step 9 File 2。
- Dependencies and consumers: Controller。
- Why now: Controller 的入参类型。
- Contract/signature changes: 新增载体；不含模型或集合字段（由知识库派生）。
- Input/output and state mapping: 见 Spec B `§9.2.11` 与 `§9.2.12`。
- Error and edge behavior: 违反约束 → 400。
- Standards impact: `MC-NAME-001`、`MC-MODEL-001`、`MC-JSON-001`、`MC-VALID-001`。
- Literal rule enforcement: `Rule 1`、`Rule 2`、`Rule 3`、`Rule 6`、`Rule 11`。
- Implementation pseudocode:

```java
@JsonIgnoreProperties(ignoreUnknown = false)
public record RetrieveKnowledgeRequest(@NotBlank @Size(max = 2000) String query,
                                       @Min(1) @Max(200) Integer topK,
                                       @DecimalMin("0.0") @DecimalMax("1.0") Double similarityThreshold) { }

@JsonIgnoreProperties(ignoreUnknown = false)
public record AskKnowledgeBaseRequest(@NotBlank @Size(max = 2000) String question,
                                      @Min(1) @Max(200) Integer topK) {
    public AskKnowledgeBaseRequest {
        question = question == null ? null : question.trim();
    }
}
// 两个载体都不含 embeddingModel 或 collectionId：由知识库派生，不接受请求覆盖
```

- Verification contribution: `TEST-013`。
- After this file: DTO 就绪。

#### File 4 — `CREATE .../adapter/knowledge/vo/{KnowledgeRetrievedChunkVO,KnowledgeQaEventVO}.java`

- Purpose: 检索片段与 SSE 事件载体。
- Symbols: 两个 record；事件 VO 含 type/stage/retrievals/delta/answer/code。
- Repository evidence: `DeepResearchEventVO` 的 SSE 事件形状。
- Dependencies and consumers: Controller 与 EventConverter。
- Why now: 出参类型。
- Contract/signature changes: 新增载体；`retrievals` 为不可变集合。
- Input/output and state mapping: 见 Spec B `§9.2.11` 与 `§9.2.12` 的事件表。
- Error and edge behavior: 无。
- Standards impact: `MC-NAME-001`、`MC-MODEL-001`、`MC-JSON-001`、`MC-TIME-001`。
- Literal rule enforcement: `Rule 1`、`Rule 3`、`Rule 6`、`Rule 10`、`Rule 11`。
- Implementation pseudocode:

```java
@JsonInclude(JsonInclude.Include.NON_NULL)
public record KnowledgeQaEventVO(String answerId, long sequence, KnowledgeQaEventTypeEnum type,
                                 KnowledgeQaStageEnum stage, List<KnowledgeRetrievedChunkVO> retrievals,
                                 String delta, String answer, String code, String message, Boolean retryable,
                                 Instant occurredAt, String traceId) { }
```

- Verification contribution: `TEST-013`。
- After this file: VO 就绪。

#### File 5 — `CREATE .../adapter/knowledge/converter/KnowledgeEventConverter.java`

- Purpose: 内部问答事件到 SSE 事件 VO 的转换。
- Symbols: `@Mapper` 接口，`extends BaseConverter<KnowledgeQaEvent, KnowledgeQaEventVO>`。
- Repository evidence: `ResearchEventConverter`。
- Dependencies and consumers: Controller。
- Why now: Controller 依赖它。
- Contract/signature changes: 新增转换器；`retrievals` 不可变。
- Input/output and state mapping: 逐字段显式 `@Mapping`。
- Error and edge behavior: 未映射字段编译期失败。
- Standards impact: `MC-CONVERT-001`、`MC-SCOPE-001`。
- Literal rule enforcement: `Rule 1`、`Rule 3`、`Rule 11`。
- Implementation pseudocode:

```java
@Mapper(unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface KnowledgeEventConverter extends BaseConverter<KnowledgeQaEvent, KnowledgeQaEventVO> {
    KnowledgeEventConverter INSTANCE = Mappers.getMapper(KnowledgeEventConverter.class);
    @Mapping(target = "answerId", source = "answerId") /* 逐字段 */ KnowledgeQaEventVO toTarget(KnowledgeQaEvent source);
}
```

- Verification contribution: `TEST-013`。
- After this file: 转换器就绪。

#### File 6 — `CREATE .../adapter/knowledge/controller/KnowledgeQaController.java`

- Purpose: API-011 与 API-012。
- Symbols: `@RestController("knowledgeQaController")`；检索为普通 `@PostMapping`，问答返回 `SseEmitter`。
- Repository evidence: `DeepResearchController#chatStream` 之外的既有 SSE 写法。
- Dependencies and consumers: API client。
- Why now: 全部载体与用例就位。
- Contract/signature changes: 新增两个端点；`operationId` 为 `retrieveKnowledgeChunks` 与 `chatWithKnowledgeBase`；问答 `produces = TEXT_EVENT_STREAM`。
- Input/output and state mapping: 见 Spec B `§9.2.11` 与 `§9.2.12`。
- Error and edge behavior: 容量饱和 429 带 `Retry-After`；流内失败一个终态；断连取消并释放许可。
- Standards impact: `MC-VALID-001`、`MC-JSON-001`、`MC-BEAN-001`、`MC-LOG-001`、`MC-TIME-001`、`MC-SCOPE-001`。
- Literal rule enforcement: `Rule 1`、`Rule 2`、`Rule 4`、`Rule 6`、`Rule 10`、`Rule 11`。
- Implementation pseudocode:

```java
@Operation(operationId = "chatWithKnowledgeBase") @SecurityRequirement(name = "researchApiKey")
@PostMapping(path = "/api/v1/knowledge-bases/{knowledgeBaseId}/chat", produces = TEXT_EVENT_STREAM_VALUE)
public SseEmitter chat(@PathVariable Long knowledgeBaseId, @Valid @RequestBody AskKnowledgeBaseRequest request) {
    SseEmitter emitter = new SseEmitter(properties.runtime().qaMaxDuration().toMillis());
    // 建立流前取许可；观察者映射为 SSE 事件；终态与断连释放许可
    return emitter;
}
```

- Verification contribution: `TEST-013`。
- After this file: Step 11 完成。

- Validation working directory: `/Users/mario/SelfProject/Egon-COLA`
- Verification command: `./mvnw -B -ntp -f egon-cola-archetypes/source-projects/egon-cola-source-agent/pom.xml clean verify`
- Expected result: 退出码 0；`KnowledgeQaControllerTest` 通过。
- Failure returns to: File 6（SSE 事件或容量）、File 2（MDC 清理）。
- Completion criteria: `REQ-011`-`REQ-014`、`REQ-022`、`REQ-025`、`REQ-027` 的接口部分有证据。
- Rollback: 回退 adapter 的问答接口与租户过滤器。
- Commit paths: `...-adapter/src/test/java/.../adapter/knowledge/KnowledgeQaControllerTest.java`; `.../adapter/knowledge/filter/KnowledgeTenantMdcFilter.java`; `.../adapter/knowledge/dto/{RetrieveKnowledgeRequest,AskKnowledgeBaseRequest}.java`; `.../adapter/knowledge/vo/{KnowledgeRetrievedChunkVO,KnowledgeQaEventVO}.java`; `.../adapter/knowledge/converter/KnowledgeEventConverter.java`; `.../adapter/knowledge/controller/KnowledgeQaController.java`
- Commit: `feat(agent-archetype): expose retrieval, qa and tenant scoping`

在实施期修正（校验失败的字段名有两种来源）：File 6 伪代码只写 `@Valid @RequestBody`。当同一处理器方法还带自己的参数约束时（本控制器的 `@Pattern` 路径变量），Spring 不在参数解析器里校验请求体，而是改由 `InvocableHandlerMethod` 的方法校验统一处理（`HandlerMethodValidationException`），advice 原按参数名报成 `fieldErrors.request`——而契约的 `fieldErrors` 键是调用方字段名（`query`/`topK`/`similarityThreshold`/`question`）。已用最小探针确认这条分叉只由"方法上是否有参数约束"决定（无路径变量的同形方法仍走 `MethodArgumentNotValidException`）。修正：`KnowledgeGlobalExceptionHandler.handleParameterValidation` 在可解析错误是 `FieldError` 时用它的属性路径作键，`@RequestParam`/`@PathVariable` 这类简单值的错误实为 `ViolationMessageSourceResolvable`、仍按注解里的线名报出（既有断言不变）。同一修正顺带消除了 Step 9 `PUT /api/v1/knowledge-bases/{id}`（路径变量 + 校验体）潜伏的同类误报。

在实施期修正（错误体必须显式声明 JSON 内容类型）：契约要求问答调用方声明 `Accept: text/event-stream`，而 4xx/5xx 的错误体是 JSON。响应体写入走 `AbstractMessageConverterMethodProcessor.writeWithMessageConverters`：只有响应未预设具体内容类型时才按 `Accept` 协商，而 `MappingJackson2HttpMessageConverter.canWrite(DeepResearchErrorResponse.class, text/event-stream)` 为 false，于是本域 advice 的处理器在发送时抛 `HttpMediaTypeNotAcceptableException`——`ExceptionHandlerExceptionResolver` 记为 `Failure in @ExceptionHandler` 并放弃该处理器，异常最终落成容器 500（`ServletException`）。修正：`KnowledgeGlobalExceptionHandler.response(...)` 显式 `.contentType(APPLICATION_JSON)`，`isContentTypePreset` 分支使协商被跳过（spring-web 6.2.18 源码 L246-253），错误体在任何 `Accept` 下都可写。research 域的等价测试没暴露这点，是因为它直接调用 advice 方法（`DeepResearchControllerTest.maps_capacity_failure_before_stream_to_429`）而不经过 MVC 栈；本 Step 的测试走完整栈，因此先发现。该文件不在 plan 的 Commit paths 之内。

在实施期修正（406 的落地方式与 `Accept` 的取值）：媒体协商失败发生在"处理器未知"的时刻，按包限定的 advice 此时不可选，而 research 域的不限定 advice 会用研究域词汇回答同一失败。故 File 6 之外增加一个 `@Hidden` 兜底映射（`consumes = application/json`、不声明 `produces`），并显式声明 `headers = "Accept!=text/event-stream"`：声明了事件流的调用方不可能落到它，其余调用方只能被拒，406 带 `KNOWLEDGE_NOT_ACCEPTABLE`。plan §13.3.2 记录了"缺 `Accept` 时 406 还是 200 未明确"，本实现把"未声明"与"声明了别的类型"同等处理为 406，并在 `@Parameter` 说明里写明"必须声明 `text/event-stream`"。另一处顺序偏差：契约 §9.2.12 逻辑第 2 条把媒体协商排在"知识库存在"之后，本实现把它排在更前——不可接受的表示连 404 也带不回去。

在实施期修正（引用为空仍要发出数组）：原实现"引用为空就不发该字段"。契约 §9.2.12 的字段表把 `data.retrievals` 定为 STARTED/COMPLETED 必填、可为空数组，故改为按**事件类型**而非条数决定：STARTED/COMPLETED 始终带数组（无命中即 `[]`），PROGRESS/FAILED 不带该字段。

在实施期修正（增量不得被规范化）：领域 `KnowledgeQaEvent` 的 `optional(delta)` 会 trim 并折叠空串，增量首尾空格被吃掉——"超时通过 " 与 "max-duration 配置。" 拼接会黏在一起。契约把 `data.delta` 定义为"增量回答 → 追加展示"，故新增 `optionalVerbatim`：空白仍视为缺失（变体校验照旧拒绝），非空白原样保留。**这是对 Step 4 工件（`...-domain/.../KnowledgeQaEvent.java`）的修改**，一并提交以便审计。

在实施期修正（超时、断连与终态语义）：File 6 只说"终态与断连释放许可"。实现：`onCompletion`/`onError` 经 `AtomicBoolean` 至多取消一次，`onTimeout` 取消并 `complete()` 而不补发 `knowledge.failed`——契约 §9.2.12 逻辑第 6 条"EOF 前没有终态事件视为未知失败"；`qaMaxDuration` 直接作为 `SseEmitter` 的超时。`ask` 返回后若流已在观察者回调里终结，补一次 `run.cancel()` 归还许可。事件发送失败（`IOException`/`IllegalStateException`）视为流本身失败：取消并让异常上抛交用例记账，不在流内伪造终态。

在实施期修正（入参与出参载体超出 plan 清单）：除 plan 列的类型外，新增 `KnowledgeRetrievalVO`（§9.2.11 信封：items/query/embeddingModel/traceId）、`KnowledgeQaReferenceVO`（事件引用不带片段文本：契约字段表只有 documentId/chunkIndex/displayName/score，而域的 `KnowledgeRetrievedChunkBO` 带正文）与 `KnowledgeQaCommandConverter`（两个入参载体 → 命令，含 `@Context` 标识与 traceId）。`KnowledgeQaEventVO` 去掉伪代码里的 `stage`（契约无该字段；`KnowledgeQaStageEnum` 未按 Step 8 笔记实现）。`KnowledgeRetrievalVoConverter` 用普通参数而非两个 `@Context`：MapStruct 要求 `@Context` 类型唯一，而 query 与 traceId 同为文本；多源参数下嵌套目标必须写成 `source.items`。`topK` 上界取契约的 `rag.retrieval.max-top-k`（50）而不是伪代码的 `@Max(200)`；控制字符只在 `question` 上拒绝（契约的 `query` 行只有 trim 后 1-2000）；两个载体都用自定义反序列化器拒绝未声明字段（契约逻辑第 3 条禁止 `embeddingModel`/`collectionId`）。两个 VO 转换器都可逆且不抛：反向只重建表示里确实存在的字段（引用按契约无正文，沿用 `withoutContent()` 的空文本写法）。

在实施期修正（测试规模与流的解码）：plan 列 5 个方法名，实际 18 个——检索不调用对话模型、空结果、空查询/越界 `topK`/越界阈值/未声明字段、404/503、事件顺序与终态唯一、流内失败一个终态、迟到事件被忽略、429 带 `Retry-After`、406、断连取消、空问题与带控制字符、问答的未声明字段、无 API Key 的 401。SSE 断言按契约"每个 `data` 是单行 UTF-8 JSON"用 `getContentAsString(UTF_8)` 解码：事件流响应不带 charset 参数（按规范即 UTF-8），默认会按容器编码读成乱码，非 ASCII 断言会假失败。

- Commit paths 补充：除 plan 列出的六个文件外，本次提交还含 `.../adapter/knowledge/converter/{KnowledgeRetrievalVoConverter,KnowledgeQaCommandConverter}.java`、`.../adapter/knowledge/vo/{KnowledgeRetrievalVO,KnowledgeQaReferenceVO}.java`、`.../adapter/knowledge/filter/package-info.java`（新包的文档）、`.../adapter/knowledge/handler/KnowledgeGlobalExceptionHandler.java`（错误体内容类型 + 校验字段名）与 `...-domain/.../domain/knowledge/model/KnowledgeQaEvent.java`（增量原样保留）。

### Step 12 — 契约测试、文档与生成回归

- Requirements: `REQ-013`, `REQ-018`, `REQ-019`, `REQ-020`, `REQ-021`, `REQ-022`, `REQ-023`, `REQ-024`
- Dependencies: Step 1-11
- Baseline state: 全部行为已实现并通过模块测试。
- Observable outcome: 契约测试断言包结构、命名、时间与日志；OpenAPI 测试断言 12 个 operation 且研究域未变；生成项目 `clean verify` 通过。
- End state: 契约测试与文档就位。
- Test-first gate: `Not applicable` — 契约与文档，无新行为。
- Manual Checks: `MC-ARCH-001`, `MC-REUSE-001`, `MC-NAME-001`, `MC-LOG-001`, `MC-CONFIG-001`, `MC-SCOPE-001`, `MC-TEST-001`, `MC-BLOCKER-001`
- Literal Rules: `Rule 1`, `Rule 7`, `Rule 10`, `Rule 11`
- Ordered files:

#### File 1 — `CREATE ...-starter/src/test/java/.../starter/KnowledgeContractTest.java`

- Purpose: 把 Spec B 的静态约束变成可执行门禁。
- Symbols: `keeps_semantic_type_suffixes()`、`uses_java_time_only()`、`never_logs_document_content()`、`signs_no_tenant_parameter_in_services()`、`documents_every_new_package()`。
- Repository evidence: `AgentSourceContractTest` 的文件遍历与正则风格。
- Dependencies and consumers: 扫描 `src/main/java`。
- Why now: 全部生产类型就位。
- Contract/signature changes: 无生产改动。
- Input/output and state mapping: 源码树 → 断言。
- Error and edge behavior: 任一违规给出违规路径与 token。
- Standards impact: `MC-NAME-001`、`MC-LOG-001`、`MC-TIME-001`、`MC-SCOPE-001`、`MC-TEST-001`。
- Literal rule enforcement: `Rule 1`、`Rule 10`、`Rule 11`。
- Implementation pseudocode:

```java
@Test void signs_no_tenant_parameter_in_services() {
    assertThat(allSourceText()).doesNotContain("tenantId, ", "tenantId)");   // 租户经 MDC 而非参数
}
@Test void uses_java_time_only() {
    assertThat(allSourceText()).doesNotContain("java.util.Date", "SimpleDateFormat");
}
```

- Verification contribution: `TEST-014`。
- After this file: 契约门禁可执行。

#### File 2 — `CREATE ...-starter/src/test/java/.../starter/KnowledgeConfigParityTest.java`

- Purpose: 断言四个 profile 的键集合同构。
- Symbols: `keeps_profile_key_sets_equal()`。
- Repository evidence: `AgentSourceContractTest#yamlKeys` 的既有实现。
- Dependencies and consumers: 四个 `application*.yml`。
- Why now: 与 File 1 同批。
- Contract/signature changes: 无。
- Input/output and state mapping: 文件 → 键集合。
- Error and edge behavior: 缺键即失败。
- Standards impact: `MC-CONFIG-001`、`MC-SCOPE-001`、`MC-TEST-001`。
- Literal rule enforcement: `Rule 7`、`Rule 11`。
- Implementation pseudocode:

```java
@Test void keeps_profile_key_sets_equal() {
    Set<String> baseline = yamlKeys("application.yml");
    for (String profile : List.of("dev", "test", "prod")) {
        assertThat(yamlKeys("application-" + profile + ".yml")).isEqualTo(baseline);
    }
}
```

- Verification contribution: `REQ-... ` 的配置部分与 Step 3。
- After this file: 键一致性受门禁保护。

#### File 3 — `CREATE ...-starter/src/test/java/.../starter/KnowledgeOpenApiTest.java`

- Purpose: 断言 12 个 operation 与开放 security，并断言研究域未变。
- Symbols: `exposes_the_twelve_knowledge_operations()`、`keeps_the_research_operation_unchanged()`。
- Repository evidence: `DeepResearchOpenApiTest` 的 JSONPath 断言风格。
- Dependencies and consumers: `/v3/api-docs`。
- Why now: 接口全部就位。
- Contract/signature changes: 无。
- Input/output and state mapping: 文档 → JSONPath 断言。
- Error and edge behavior: operation 缺失或研究域变化即失败。
- Standards impact: `MC-JSON-001`、`MC-SCOPE-001`、`MC-TEST-001`。
- Literal rule enforcement: `Rule 6`、`Rule 11`。
- Implementation pseudocode:

```java
@Test void exposes_the_twelve_knowledge_operations() throws Exception {
    mockMvc.perform(get("/v3/api-docs")).andExpect(jsonPath("$.paths.length()").value(13))
        .andExpect(jsonPath("$.paths['/api/v1/knowledge-bases'].post.operationId").value("createKnowledgeBase"));
}
```

- Verification contribution: `REQ-014`、`REQ-024`。
- After this file: 契约与回归门禁就位。

#### File 4 — `MODIFY definitions/egon-cola-archetype-agent/architecture-docs/agent-multi-module-architecture.md`

- Purpose: 移除"无数据库/无迁移"表述并描述 knowledge 域。
- Symbols: 依赖图与边界说明。
- Repository evidence: 该文档当前声明无 DB/Flyway/cache/broker/RPC/GraphQL/UI。
- Dependencies and consumers: 生成项目读者。
- Why now: 实现已定型。
- Contract/signature changes: 更新表述；新增 knowledge 域与迁移位置说明。
- Input/output and state mapping: 无。
- Error and edge behavior: 不得声称组件会建表或跑 Flyway。
- Standards impact: `MC-SCOPE-001`、`MC-TEST-001`。
- Literal rule enforcement: `Rule 11`。
- Implementation pseudocode:

```text
依赖图：保留六模块与方向，新增 knowledge 域标注
边界：说明本项目拥有 knowledge 表、Flyway 迁移与 outbox 表；向量表由宿主 Bean 创建
限制：说明租户当前恒为默认值，不构成隔离边界（Spec B RISK-009）
```

- Verification contribution: `REQ-020`。
- After this file: 文档与生成结果一致。

#### File 5 — `MODIFY egon-cola-source-agent/{README.md,README.zh-CN.md}`

- Purpose: 说明新接口、配置键与运维前提。
- Symbols: 接口清单、配置键表与运维须知。
- Repository evidence: 现有两份 README 的章节结构。
- Dependencies and consumers: 生成项目读者。
- Why now: 同 File 4。
- Contract/signature changes: 文档更新。
- Input/output and state mapping: 无。
- Error and edge behavior: 必须写明四件事：数据库与 `vector` 扩展前提、向量表由 Bean 创建、租户恒为默认值、问答与研究共用同一 API Key。
- Standards impact: `MC-SCOPE-001`、`MC-TEST-001`。
- Literal rule enforcement: `Rule 11`。
- Implementation pseudocode:

```text
新增章节：
  知识库接口     — 12 个端点与 API Key 说明，引用 OpenAPI 文档
  知识库配置     — 数据源、Flyway locations、RAG 组件、outbox、MyBatis Plus 与 agent.knowledge 键表
  摄取状态机     — PENDING/PROCESSING/SUCCEEDED/FAILED/DEAD 与 outbox 状态的一一映射
  运维前提       — ① 数据库需具备 CREATE EXTENSION vector 权限；
                   ② 向量表由 knowledgeRagVectorStore 创建，不由 Flyway 管理；
                   ③ 租户当前恒为默认值，不构成隔离边界；
                   ④ 问答与研究共用同一 API Key 与同一进程容量模型
更新章节：
  模块依赖       — 在依赖图与包边界表中加入 knowledge 域与 infrastructure 的新依赖
  运行前提       — 移除"无数据库/无迁移"表述，改为列出迁移位置与向量表归属
```

- Verification contribution: `REQ-020`。
- After this file: Step 12 完成。

- Validation working directory: `/Users/mario/SelfProject/Egon-COLA`
- Verification command: `./mvnw -B -ntp -f egon-cola-archetypes/source-projects/egon-cola-source-agent/pom.xml clean verify` 然后 `./scripts/generate_archetypes.sh generate && ./scripts/check_archetypes.sh` 然后 `./mvnw -B -ntp -f egon-cola-archetypes/pom.xml -Pgenerated-archetypes clean verify`
- Expected result: 三条命令均退出码 0；全部契约测试通过；研究域既有测试未改动即通过。
- Failure returns to: File 1-3（契约断言）、Step 1（打包清单）。
- Completion criteria: `REQ-013`、`REQ-018`-`REQ-024` 有证据。
- Rollback: 回退契约测试与文档。
- Commit paths: `...-starter/src/test/java/.../starter/KnowledgeContractTest.java`; `...-starter/src/test/java/.../starter/KnowledgeConfigParityTest.java`; `...-starter/src/test/java/.../starter/KnowledgeOpenApiTest.java`; `definitions/egon-cola-archetype-agent/architecture-docs/agent-multi-module-architecture.md`; `egon-cola-source-agent/{README.md,README.zh-CN.md}`
- Commit: `test(agent-archetype): gate the knowledge contracts and update the docs`

在实施期修正（`$.paths.length()` 是路径数不是操作数）：File 3 伪代码断言 13，实际为 8——`/v3/api-docs` 把同一路径上的 `post`/`get`/`put`/`delete` 收进一个 path 对象，13 是操作数（12 个 knowledge + 1 个 research）。断言因此拆成两段：`$.paths.length() = 8` 钉住表面规模，12 个 knowledge 操作逐个按"路径 + 方法"断言 `operationId`——改名、掉方法、丢路径都会失败，比单一计数更能说明问题。

在实施期修正（配置键比对的粒度与范围）：既有 `AgentSourceContractTest#yamlKeys` 只比较白名单里的**叶子键名**（研究域那 12 个），四个文件因此"看起来"集合相等；File 2 要的是**完整点分路径**，而这个粒度上"四个文件同集合"不成立——`application.yml` 合法地多出 `spring.application.name`、`spring.config.import`、`spring.servlet.multipart.*`，profile 文件也各自持有数据源、Flyway 与供应商键而不必回写 base。落地为两条属性：三个 profile 文件互为同一集合（完整路径），且每个 profile 键路径都出现在 base 中。`keyPaths(...)` 用缩进栈把 `key:` 行还原成点分路径，避免把 `spring:` 之下不同层级的同名叶子混为一谈。

在实施期修正（包文档门禁的强度）：`documents_every_new_package()` 除"存在 `package-info.java`"外还要求其中 javadoc 非空白——存在性已由 `AgentSourceContractTest#documents_every_populated_java_package_and_keeps_profile_key_sets_equal` 覆盖（它遍历 main 与 test 两个源集），本门禁补的是"有内容"，防止空壳文件骗过前者。

在实施期修正（问答错误体在文档里也必须是 JSON）：写 File 3 的断言时用临时探针 dump 了 chat 操作，发现六个错误响应只声明 schema，于是继承操作的 `produces`，在文档里被渲染为 `text/event-stream`——据此生成的客户端会拿流解析器读 JSON 错误体。修正：`KnowledgeQaController` 的六个 `@ApiResponse` 显式 `mediaType = application/json`，并把 `406`/`429` 的 `content['application/json'].schema.$ref` 写进断言。该文件不在 plan 的 Commit paths 之内。

在实施期修正（`@Hidden` 不是这份文档的守卫）：兜底映射带 `@Hidden`，注释里一度写成"它使该映射不出现在文档中"。A/B 探针（带/不带注解各 dump 一次 chat 操作）得到的两份 JSON **逐字节相同**：springdoc 把同一"路径 + 方法"的两个处理器合并为一个操作，流式的那个胜出。`@Hidden` 表达的是意图，文档规模由第一段的断言钉住；测试 javadoc 已按事实改写，探针与 dump 已删除。

在实施期修正（静态门禁的变异验证）：File 1 的三条扫描门禁各人为种过一次违规——新增 `KnowledgeChunkData` 类、日志标签写成 `text={}`、服务方法加 `Long tenantId` 形参——每次都恰好是对应那一条测试失败并打印违规路径与 token，随后回退并确认工作树干净。这三条断言不是"从未红过"的正则。

在实施期修正（starter 测试规模）：本 Step 新增 10 个测试（契约 5、配置 2、OpenAPI 3），starter 模块从 10 升到 20；第一条门禁命令的 surefire 汇总为 136 个测试（common 0、domain 6、application 22、infrastructure 32、adapter 56、starter 20），零失败零错误。

在实施期修正（文档改写的落点）：File 4 除删除"无数据库/无迁移"表述外，重写了依赖与运行时归属小节——点出 RAG、transactional-outbox、MyBatis-Plus 三个组件，并写明向量表由 Bean 创建、故不可能与组件校验过的嵌入模型维度不一致。两份 README 同步新增四节：知识库接口（12 操作表 + `Accept`/406 + "请求不能指定集合、模型或租户"这两条 schema 表达不出的行为）、摄取状态机（Spec B §10.6 的迁移表）、知识库配置（键 → 环境变量 → 含义，含 `spring.servlet.multipart` 三键）、以及四条运维前提（`CREATE EXTENSION vector` 权限、向量表归属、租户不构成隔离边界、问答与研究共用 Key 但容量池独立）；环境合同与校验节里"不访问数据库/不跑 Flyway"的旧表述一并删除。

在实施期修正（生成器校验脚本的过期断言，由第三条门禁发现）：`.../src/test/resources/projects/basic/verify.groovy` 第 195 行仍断言 `readme.contains("no database")`——那是 knowledge 域之前的事实。第三条门禁第一次运行即在 `egon-cola-archetype-agent` 的 IT 上失败并打印 README 全文；这正是该门禁存在的意义。修正：断言取反（`!readme.contains("no database")`）并改为钉住新的事实——两份 README 都必须写出问答端点、`PostgreSQL` 与 `CREATE EXTENSION vector`，从而把"中英同步"也变成可执行条件。该文件不在 plan 的 Commit paths 之内，但它是 Step 1 打包清单的组成部分，故与本次一并提交。

在实施期修正（第三条门禁的残留红灯与本 plan 无关）：修正后重跑，`egon-cola-archetype-agent` 的 IT 通过（生成项目的六个模块与 `verify.groovy` 全绿），反应堆继续前进并在 `egon-cola-archetype-light` 上以非零码停下——`NativeRpcConfigurationTest` 有两个既有失败（`egon.cola.platform.tianquan.shoubing.enabled` 读不到、整个前缀 bind 不上）。根因不在本次改动：light 的四个 YAML 把它写成单段键 `tianquan-shoubing:`，而组件 `IdpStarterProperties` 的 `@ConfigurationProperties("egon.cola.platform.tianquan.shoubing")` 需要嵌套层级，Spring 的宽松绑定不把 `-` 当层级分隔符。该漂移在 HEAD 上即存在（`git log` 显示 light 的 YAML 与测试最后同出于 `07f495535`），与 knowledge 域无关，按"不夹带无关改动"的约束未在此修复，留待单独处理。

- Commit paths 补充：除 plan 列出的六个文件外，本次提交还含 `...-adapter/knowledge/controller/KnowledgeQaController.java`（六个错误响应显式声明 JSON 内容类型）与 `definitions/egon-cola-archetype-agent/src/test/resources/projects/basic/verify.groovy`（README 断言更新）。

## 8. Test, Validation, and Quality Gates

| Gate/order | Working directory | Command or method | Scope | Expected result | Failure returns to | Requirements/runtime boundary |
| --- | --- | --- | --- | --- | --- | --- |
| 组件前置 | `/Users/mario/SelfProject/Egon-COLA` | `./mvnw -B -ntp -f egon-cola-components/pom.xml install -DskipTests` | components | 退出码 0；rag-starter 安装 | 组件 Plan | 跨模块 |
| 源码项目 | 同上 | `./mvnw -B -ntp -f egon-cola-archetypes/source-projects/egon-cola-source-agent/pom.xml clean verify` | 六模块 | 退出码 0；全部测试通过 | 当前 Step | 模块 |
| 生成 | 同上 | `./scripts/generate_archetypes.sh generate && ./scripts/check_archetypes.sh` | 生成器 | 退出码 0；生成物与源码一致 | Step 1 | 跨模块 |
| 生成 IT | 同上 | `./mvnw -B -ntp -f egon-cola-archetypes/pom.xml -Pgenerated-archetypes clean verify` | 七个 archetype | 退出码 0；全部 verifier 通过 | Step 1 / 12 | 跨模块 |
| 运行期/人工 | 用户控制的环境 | 需 PostgreSQL（含 `vector` 扩展）与 OpenAI-compatible 模型；启动后手工走一遍上传—轮询—检索—问答 | 集成 | 观察结果由用户记录 | Step 12 | **本 Plan 不声称已执行** |

## 9. Migration, Compatibility, Rollout, and Rollback

| Concern | Disposition | Spec section and repository reason |
| --- | --- | --- |
| 新增迁移 | 适用 | 两条新迁移，全部为创建操作，无历史数据、无回填、无 `NOT NULL` 收紧；不修改任何既有迁移（本项目此前没有迁移） |
| 向量表 | 适用 | 不在迁移内，由 `PgVectorStore` 首次启动创建（`REQ-010`） |
| 兼容面 | 适用 | `research` 域的接口、事件、错误码与容量语义不变（`REQ-024`）；六模块与包前缀不变（`REQ-001`） |
| 部署顺序 | 适用 | 数据库需先具备 `CREATE EXTENSION vector` 权限；Flyway 建表后由 `PgVectorStore` 建向量表，顺序由 Bean 依赖保证 |
| 回滚 | 适用 | 逐 Step 回退对应提交；迁移可 `DROP TABLE`，但会丢失全部知识库数据 |
| 前向修复 | 适用 | 迁移不可修改；修正需追加新版本迁移 |

## 10. Requirement-to-Step Traceability Matrix

| Requirement | Effective Spec section | Steps | Tests/gates | Completion evidence |
| --- | --- | --- | --- | --- |
| `REQ-001` | Spec B `§4`, `§8.2` | Step 4-11 | `KnowledgeContractTest` | 六模块与包前缀不变；`check-reactor` 通过 |
| `REQ-002` | Spec B `§9.2.1`-`§9.2.5` | Step 5, 8, 9 | `KnowledgeBaseControllerTest` | 四个端点可用且级联正确 |
| `REQ-003` | Spec B `§9.2.6`-`§9.2.10` | Step 5, 8, 10 | `KnowledgeDocumentControllerTest` | 五条文档路径可用 |
| `REQ-004` | Spec B `§7.3.3` | Step 8 | `KnowledgeManageTest` | 文档行与 outbox 行同事务 |
| `REQ-005` | Spec B `§7.3.4` | Step 7, 8 | `KnowledgeIngestDeliveryHandlerTest` | 状态可查询且非法迁移被拒 |
| `REQ-006` | Spec B `§7.3.1` | Step 7 | `KnowledgeIngestDeliveryHandlerTest` | 重跑不重解析且分块 id 一致 |
| `REQ-007` | Spec B `§9.2.6` | Step 8, 10 | `KnowledgeManageTest` | 原文件可取回且文本已落库 |
| `REQ-008` | Spec B `§7.1.2` | Step 2, 6 | `KnowledgeVectorConfigurationTest` | 具名 Bean 存在且被组件解析 |
| `REQ-009` | Spec B `§9.2.11` | Step 6, 11 | `KnowledgeVectorConfigurationTest` | 跨模型不串结果 |
| `REQ-010` | Spec B `§11.1` | Step 3, 6 | `KnowledgeSchemaMigrationTest` | 迁移不含向量表；启动后表存在 |
| `REQ-011` | Spec B `§9.2.11` | Step 11 | `KnowledgeQaControllerTest` | 返回片段与分数 |
| `REQ-012` | Spec B `§9.2.12` | Step 11 | `KnowledgeQaControllerTest` | 事件有序、终态唯一 |
| `REQ-013` | Spec B `§9.0` | Step 9-11 | `KnowledgeBaseControllerTest` | 无 key 一律 401 |
| `REQ-014` | Spec B `§9.2.x` | Step 9-11 | `KnowledgeOpenApiTest` | 错误体与 trace 一致 |
| `REQ-015` | Spec B `§11.2.1` | Step 3, 12 | `KnowledgeSchemaMigrationTest` | 两条迁移存在且被生成 |
| `REQ-016` | Spec B `§11.2.3` | Step 3, 7 | `KnowledgeSchemaMigrationTest` | 表结构与组件期望一致 |
| `REQ-017` | Spec B `§16` 清单 1-3 | Step 1 | `TEST-001` | 修订后源码项目构建通过 |
| `REQ-018` | Spec B `§16` 清单 4 | Step 1 | `TEST-003` | 生成产物含迁移 |
| `REQ-019` | Spec B `§16` 清单 3 | Step 1, 12 | `TEST-002` | 依赖与运行时库断言通过 |
| `REQ-020` | Spec B `§16` 清单 5 | Step 12 | 文档评审 | 文档与生成结果一致 |
| `REQ-021` | Spec B `§4` | Step 12 | 生成 IT | 生成项目 `clean verify` 通过 |
| `REQ-022` | Spec B `§7.2.2` | Step 8, 11 | `KnowledgeQaControllerTest` | 饱和 429 且不与研究互斥 |
| `REQ-023` | Spec B `§4` | 全部 Step | 全部测试 | `clean verify` 无需外部服务 |
| `REQ-024` | Spec B `§4` | Step 12 | 既有研究测试 | 不改动即通过 |
| `REQ-025` | Spec B `§11.2.1` | Step 3, 5, 11 | `KnowledgeTenantScopeTest` | 列非空；生成 SQL 含租户条件；签名无租户参数 |
| `REQ-026` | Spec B `§7.3.1` | Step 7 | `KnowledgeIngestDeliveryHandlerTest` | 投递线程无 MDC 时仍按正确租户读写 |
| `REQ-027` | Spec B `§11.2.4` | Step 6, 11 | `KnowledgeVectorConfigurationTest` | 过滤恒含租户 |

## 11. Risks, Blockers, and User Decisions

| ID | Risk or decision | Impacted Steps/files | Evidence | Owner | Status/action |
| --- | --- | --- | --- | --- | --- |
| `BLOCK-001` | MyBatis Plus 租户拦截器的实际行为未从源码验证：插入时是否自动填 `tenant_id`、`tenant-id.mdc-key` 的默认键名、MDC 无值时的行为 | Step 3, 5, 11 | Spec B `RISK-002`；组件配置元数据 | 实施者 | Closed — 处置已定：`KnowledgeTenantScopeTest` 在完整读写路径上通过语句拦截断言注入的条件与写入值；不符时按 `EgonColaMetaObjectHandler` 的既有实现调整 |
| `BLOCK-002` | Spring AI `1.1.8` 的 `PgVectorStore` builder 方法名与本地核对的 `1.1.2` 可能不同 | Step 6 | 组件 Plan 的 `RISK-001` | 实施者 | Closed — 处置已定：实施 Step 6 前用 `1.1.8` 复核 builder 方法签名 |
| `BLOCK-003` | 组件侧 `RagEmbeddingException` 的处置 | Step 7 | 组件交付报告 → 用户 2026-09-10 15:10 选择按推荐修订 | 用户 | Closed — 已按推荐处置：该异常不可达，已从组件删除，Spec A 出第二次显式修订；`RagVectorStoreException` 覆盖嵌入失败。本 Plan 的 Step 7 错误映射**已同步修改**，全部 `REQ-*` 不变 |
| `RISK-001` | 全套生成回归需要先安装 components 与 source-projects，且 components Reactor 有既有的 xingyuan 循环依赖 | Step 1, 12 | 组件实施期的基线观察 | 实施者 | Closed — 处置已定：按 `README.md` Quick Start 从根 Reactor 构建，或在已安装的本地仓库上跑 |

`BLOCK-003` 已按推荐关闭：组件删除了不可达的 `RagEmbeddingException`，`RagVectorStoreException` 覆盖嵌入失败；本 Plan 的 Step 7 错误映射已同步为 `RagVectorStoreException`，全部 `REQ-*` 不变。本 Plan 无开放项。

## 12. Review and Acceptance

### 12.1 Original requirement fidelity

- 用户要求"引擎用 rag-starter" -> Step 2 依赖登记、Step 6 装配、Step 7 摄取。
- 用户要求"管理留在 archetype，agent archetype 就是 rag-admin" -> Step 5、8、9、10。
- 用户要求"无前端" -> 本 Plan 无前端文件。
- 用户要求"原文入库且不重复解析" -> Step 3 的 `content` 列、Step 7 的重建逻辑。
- 用户要求"同维度多模型" -> Step 6 装配与 Step 11 强制过滤。
- 用户要求"租户字段先加上、来源后置" -> Step 3 列、Step 7 恢复、Step 11 过滤器；不实现鉴权。

### 12.2 Spec consistency

本 Plan 未引入 Spec B 之外的行为、契约、字段、schema 或依赖。逐项核对：`§5` 的文件树与 Spec B `§8.2` 一致（差量见 `§6.4` 的四个 Clarification）；12 个接口与 `§9.1` 一一对应；两张表与 `§11.2` 的列设计一致；租户语义与 `REQ-025`-`REQ-027` 一致，且**接口契约中不出现租户字段**。

简洁性与必要性审计见 `§4.5`，未发现 fetch-then-forward 接口或过度设计。

### 12.3 Repository executability

全部路径与符号已对照当前仓库核实：`AgentArchitectureTest` 第 70-72 行、`AgentSourceContractTest` 第 38 行、`verify.groovy` 第 78-80/129/193 行、`archetype-metadata.xml` 的 starter 文件集形状、六模块的既有依赖。命令取自 `README.md` 的 Quick Start 与既有脚本。

### 12.4 Test and release completeness

每个行为 Step 都有 RED 门；Step 1/2/12 的例外给出证据。全套离线。运行期验证明确标记为未执行。

### 12.5 Blocking Manual Check

| Check ID | Applicability | Status | Evidence | Finding | Required action/exception |
| --- | --- | --- | --- | --- | --- |
| `MC-ARCH-001` | Applicable | PASS | `§4.7`；`AgentArchitectureTest`、`verify.groovy`；Spec B `§6.1` | 严格保留六模块 Web non-open profile；`knowledge` 只是业务域包；Step 1 的修订只放宽禁项不改结构 | None |
| `MC-REUSE-001` | Applicable | PASS | `§4.7` 的能力复用台账 10 行 | 组件、outbox、MyBatis Plus、Flyway、pgvector、校验、转换、错误体与过滤器全部复用 | None |
| `MC-DEP-001` | Applicable | PASS | `§4.7` 台账与 Step 2 | 未新增任何第三方依赖；全部坐标由既有 BOM 管理 | None |
| `MC-NAME-001` | Applicable | PASS | `§5` 文件树与各 Step 的文件块 | 全部新增类型带语义后缀；无 `Data`/`Info`/`Param`/`Bean` | None |
| `MC-VALID-001` | Applicable | PASS | Step 4、8-11 的伪代码要点 | 每层交接 Jakarta 注解与 `@Valid` 级联；Manage 方法 `@Validated`；无电话字段故 libphonenumber `N/A` | None |
| `MC-MODEL-001` | Applicable | PASS | Spec B `§6.2.1`；Step 5 | 简单载体为 `record`；PO 使用仓库既有约定且**用户已批准为规则 3 例外**；无构造器签名冲突 | None |
| `MC-CONVERT-001` | Applicable | PASS | Step 5、8、11 | 全部转换器用 MapStructPlus 并 `extends BaseConverter`；无手工拷贝 | None |
| `MC-LOG-001` | Applicable | PASS | Step 5、7、8、9-11 | 每个具体行为类 `@Slf4j`；日志字段白名单；契约测试断言零内容泄露 | None |
| `MC-BEAN-001` | Applicable | PASS | Step 5-11 | Spring Bean 显式命名；构造器注入 + `@RequiredArgsConstructor` + `@Qualifier`；`lombok.config` 既有 | None |
| `MC-UTIL-001` | Applicable | PASS | Step 4-11 | 只用 JDK 与既有依赖；不新增 `*Utils` | None |
| `MC-JSON-001` | Applicable | PASS | Step 9-11 | 只用 Spring Boot Jackson；错误体复用；枚举与 `Instant` wire 语义显式 | None |
| `MC-TIME-001` | Applicable | PASS | Step 3-11 | 全部时间为 `Instant`/`Duration`；复用既有 `agentClock`；契约测试静态扫描 | None |
| `MC-CONFIG-001` | Applicable | PASS | Step 3 | 四个 profile 同构新增键；类型化 `@ConfigurationProperties`；凭据全部来自环境变量 | None |
| `MC-PATTERN-001` | Applicable | PASS | Spec B `§13.1`；Step 4、6、7 | 端口与适配器、outbox 策略、显式状态机；无硬编码业务分支 | None |
| `MC-SCOPE-001` | Applicable | PASS | `§5` 与 `§11` | 变更限于 agent source 与 definition；`research` 域零改动 | None |
| `MC-TEST-001` | Applicable | PASS | `§8` 与 `§10` | 每个行为 Step 有 RED 与 GREEN；含 OpenAPI 与生成产物断言 | None |
| `MC-BLOCKER-001` | Applicable | PASS | 本表与 `§11` | 无 `FAIL`/`BLOCKED`/`UNKNOWN`；`BLOCK-003` 为开放式用户决策，不改变本 Plan 的任何 `REQ-*` | None |

### 12.6 Final verdict

`PASS — Ready for user review`

本 Plan 内部完整、无未决占位符，全部阻塞 Manual Check 为 `PASS` 或证据化 `N/A`。`BLOCK-003` 已关闭：组件侧不可达的异常已删除，本 Plan 的错误映射已同步。

本 Plan 未修改任何生产或测试代码、未执行迁移、未启动应用，也未声称任何运行期验证。

---

## 13. 附录：Spec B §9.2 接口契约逐字摘录

> 本附录由两个只读子代理从 Spec B `docs/egon/spec/2026-09-10-11-51-agent-archetype-knowledge-rag.md` 的第 809–2388 行（`§9.2.1`–`§9.2.12`，共 12 个接口）逐字摘出，目的是让 `§7` 的每个 Step 能在同一份文件里读到它即将实现的确切契约，并作为 `§10.5` 实施期修正的判据来源。
>
> 摘录约定：每段以其在 Spec B 中的行号区间 `[L<起>-<止>]` 开头；收录各接口的定位与用途、身份与所有权、请求参数与请求体字段表、成功响应字段表与 jsonc、完整失败表、编号处理步骤，以及排序/分页/幂等/事务/顺序等段落；未收录前端页面实现、注解与测试小节。除把运输过程中产生的 HTML 实体还原为字面字符（`&gt;`→`>`、`&lt;`→`<`、`&amp;`→`&`）外，未做任何改写、删节、摘要或压缩——包括子代理重复引用的行号段在内，均按返回原样保留。
>
> 冲突时以 Spec B 原文为准；本附录不构成新的契约来源，只做定位。文末 `OBSERVATIONS` 是子代理的观察，**不是 spec 原文**。

### 13.1 API-001 – API-006（逐字摘录）

[L811]
#### 9.2.1 API-001 — 建知识库

[L813-823]
##### Necessity and interaction-cost decision

| Concern | Decision |
| --- | --- |
| Change classification | `New` |
| Independent consumer goal | 管理员建立一个可供后续上传与检索的知识库，并选定其嵌入模型与分块策略 |
| Parameter ownership and derivation | 名称、描述、逻辑模型名与分块策略由管理员拥有；标识、审计字段、初始状态与时间戳由服务端派生 |
| Direct/no-new-interface alternative | 复用研究域端点或在首次上传时隐式建库。前者语义不符；后者把"选模型"这一不可变更的决策藏在副作用里，无法在错误时给出明确契约 |
| Caller use of result | 管理员据返回的标识进入文档管理；结果不被原样转发给另一个端点 |
| Round trips and failure points | 一次 RTT；失败点为校验、逻辑模型未注册、名称重复与写库失败 |
| Verdict | `Add`，覆盖 `REQ-002` 与 `REQ-009` |

[L825-834]
##### API style and CQRS semantics

| Concern | Decision/evidence |
| --- | --- |
| Protocol style | `REST Command` |
| CQRS role | 创建型 Command，拥有 `knowledge_base` 的权威写入 |
| Resource/task semantics | `POST /api/v1/knowledge-bases` 在集合下创建从属资源，返回其表示 |
| Read/write and side effects | 校验逻辑模型已注册后写入一行；无事件、无外部调用 |
| Consistency and idempotency | 单表插入在一个本地事务内；`(tenant_scope, code)` 唯一约束兜底重复；非幂等，重复提交返回 409 |
| Why this style | 资源语义清晰、无独立任务语义，无需 GraphQL 或任务资源 |

[L836-845]
##### Identity and purpose

| Concern | Definition |
| --- | --- |
| Purpose/owner/consumer | 建立一个知识库；由 adapter 拥有，由知识库管理员调用 |
| Protocol and endpoint | `HTTP POST /api/v1/knowledge-bases` |
| Content type/version | 请求与响应 `application/json`；版本 v1 在路径中 |
| Auth/permission/tenant | `X-Research-Api-Key`；无租户与用户身份 |
| Timeout/retry/rate limit | 无独立超时；无独立限流；服务端不重试 |
| Idempotency/concurrency | 非幂等；并发同名创建由唯一约束拒绝其中一个 |

[L847-874]
##### Request parameters

| Name | Location | Type/format | Required/null | Default | Validation/range/enum | Meaning | Example | Source |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| `X-Research-Api-Key` | Header | String | 必填、非空 | 无 | 常量时间比较 | 服务凭据 | `demo-key`（仅测试） | 既有过滤器 |
| `X-Trace-Id` | Header | String | 可选 | 服务端生成 | trim；1-128；`[A-Za-z0-9._:-]+` | 请求关联标识 | `4e9d6938` | 既有过滤器 |
| `code` | Body | String | 必填、非空 | 无 | trim 后 2-64；`[A-Za-z0-9._-]+` | 稳定业务键 | `product-docs` | 管理员 |
| `name` | Body | String | 必填、非空 | 无 | trim 后 1-128 | 展示名称 | `产品文档库` | 管理员 |
| `description` | Body | String | 可选、允许缺省 | `null` | trim 后 0-512 | 描述 | `内部产品手册` | 管理员 |
| `embeddingModel` | Body | String | 必填、非空 | 无 | 必须存在于逻辑模型注册表 | 逻辑嵌入模型名 | `openai-small` | 管理员 |
| `chunkStrategy` | Body | String enum | 必填 | 无 | `TOKEN` / `MARKDOWN_HEADING` / `RECURSIVE` | 分块策略 | `TOKEN` | 管理员 |
| `chunkConfig` | Body | Object | 必填、非空 | 无 | 按策略校验；`maxTokensPerChunk` 32-4096；`overlapTokens` 0 至 `maxTokensPerChunk-1`；`minChunkChars` 0-4096；`headingLevels` 仅 `MARKDOWN_HEADING` 可用 | 分块参数 | 见下 | 管理员 |

```jsonc
{
  "code": "product-docs", // 必填。稳定业务键，trim 后 2-64 位，仅允许字母、数字、点、下划线与连字符；创建后不可修改，重复返回 409。
  "name": "产品文档库", // 必填。展示名称，trim 后 1-128 字符。
  "description": "内部产品手册", // 可选。缺省为 null；trim 后最长 512 字符。
  "embeddingModel": "openai-small", // 必填。逻辑嵌入模型名，必须已在本部署注册；创建后不可修改。
  "chunkStrategy": "TOKEN", // 必填。分块策略枚举，取值 TOKEN、MARKDOWN_HEADING 或 RECURSIVE；创建后不可修改。
  "chunkConfig": { // 必填。分块参数对象，字段集合必须与 chunkStrategy 匹配；创建后不可修改。
    "maxTokensPerChunk": 512, // 必填。单分块 token 上限，范围 32-4096。
    "overlapTokens": 64, // 可选。相邻分块重叠 token 数，缺省 0；必须严格小于 maxTokensPerChunk。
    "minChunkChars": 1, // 可选。丢弃短于该字符数的分块，缺省 1；范围 0-4096。
    "headingLevels": [1, 2, 3] // 可选。仅 MARKDOWN_HEADING 允许非空，取值 1-6 且不重复；其它策略传入非空值返回 400。
  }
}
```

[L876-906]
##### Success response

HTTP `200 OK`。

```jsonc
{
  "knowledgeBaseId": "kb-01J5K9", // 服务端生成的知识库标识；后续所有路径参数使用它。
  "code": "product-docs", // 回显业务键，创建后不可修改。
  "name": "产品文档库", // 当前展示名称。
  "description": "内部产品手册", // 当前描述；无描述时为 null。
  "embeddingModel": "openai-small", // 已冻结的逻辑嵌入模型名。
  "chunkStrategy": "TOKEN", // 已冻结的分块策略。
  "chunkConfig": { // 已冻结的分块参数。
    "maxTokensPerChunk": 512, // 单分块 token 上限。
    "overlapTokens": 64, // 相邻分块重叠 token 数。
    "minChunkChars": 1 // 最短分块字符数。
  },
  "documentCount": 0, // 当前未删除文档数；新建时为 0。
  "status": "ACTIVE", // 知识库状态；当前只有 ACTIVE 与 DELETED，DELETED 不再对查询可见。
  "createdAt": "2026-09-10T03:51:00.000Z", // 创建时间，UTC Instant，毫秒精度。
  "traceId": "4e9d6938" // 与响应头一致的关联标识。
}
```

| Field path | Type/format | Required/null/default | Validation/enum/precision | Meaning and source | Frontend use |
| --- | --- | --- | --- | --- | --- |
| `knowledgeBaseId` | String | 必填、非空 | 服务端生成 | 稳定标识 | 路径参数与列表行键 |
| `embeddingModel` | String | 必填、非空 | 注册表中的逻辑名 | 冻结配置 | 展示；不可编辑 |
| `documentCount` | int | 必填 | ≥ 0 | 未删除文档计数 | 列表与详情展示 |
| `status` | String enum | 必填 | `ACTIVE` / `DELETED` | 生命周期 | 决定是否可上传 |
| `createdAt` | RFC 3339 UTC | 必填 | 毫秒精度 | 服务端 `agentClock` | 展示 |

[L908-927]
##### Error responses

| Condition | HTTP status | Business code | Response shape | Retryable | Frontend handling |
| --- | --- | --- | --- | --- | --- |
| 字段校验失败 | 400 | `KNOWLEDGE_VALIDATION_ERROR` | 既有错误体 + `fieldErrors` | 修正后 | 标红字段 |
| 逻辑模型未注册 | 400 | `KNOWLEDGE_MODEL_NOT_REGISTERED` | 既有错误体 | 修正后 | 展示可用模型列表 |
| 分块参数与策略不匹配 | 400 | `KNOWLEDGE_VALIDATION_ERROR` | 既有错误体 + `fieldErrors` | 修正后 | 标红字段 |
| API Key 缺失或错误 | 401 | `RESEARCH_UNAUTHORIZED`（复用） | 既有错误体 + `WWW-Authenticate: ApiKey` | 修正凭据后 | 提示重新配置 key |
| 业务键已存在 | 409 | `KNOWLEDGE_BASE_CODE_CONFLICT` | 既有错误体 | 换名后 | 提示更换业务键 |
| 未预期失败 | 500 | `KNOWLEDGE_INTERNAL_ERROR` | 既有错误体（无堆栈） | 携带 traceId 上报 | 通用错误提示 |

```jsonc
{
  "code": "KNOWLEDGE_BASE_CODE_CONFLICT", // 稳定机器码；前端据此分支，不解析 message。
  "message": "knowledge base code already exists", // 面向调用方的安全摘要。
  "traceId": "4e9d6938", // 与响应头一致的关联标识。
  "timestamp": "2026-09-10T03:51:00.000Z", // 服务端 UTC Instant。
  "fieldErrors": {} // 非字段错误时为空对象；字段错误时为 字段名 -> 消息列表。
}
```

[L929-938]
##### Interface logic for frontend and consumers

1. 调用方必须携带有效 `X-Research-Api-Key`；缺失时请求不会进入控制器。
2. 校验顺序：字段约束与规范化 -> 逻辑模型在注册表中存在 -> 分块参数与所选策略匹配 -> 业务键唯一。
3. 服务端派生 `knowledgeBaseId`、初始 `status=ACTIVE`、审计字段与 `createdAt`；请求不得提供这些字段。
4. 一次本地事务插入一行；无外部调用、无事件、无缓存。
5. 重复业务键由唯一约束拒绝，映射为 409；并发同名创建只有一个成功。
6. 一旦创建成功，`embeddingModel`、`chunkStrategy` 与 `chunkConfig` 即为冻结；`API-004` 不接受它们的修改，客户端不可提供。
7. 调用方成功后应使用返回的 `knowledgeBaseId` 进入文档管理；失败时按 `code` 分支，`fieldErrors` 用于定位字段，不要解析 `message` 文本。
8. 本接口不产生异步工作；上传不会在此时发生，也不会有任何摄取任务被创建。

[L833]
| Consistency and idempotency | 单表插入在一个本地事务内；`(tenant_scope, code)` 唯一约束兜底重复；非幂等，重复提交返回 409 |

[L845]
| Idempotency/concurrency | 非幂等；并发同名创建由唯一约束拒绝其中一个 |

[L934]
4. 一次本地事务插入一行；无外部调用、无事件、无缓存。

[L811]
#### 9.2.1 API-001 — 建知识库

[L964]
#### 9.2.2 API-002 — 知识库分页列表

[L966-976]
##### Necessity and interaction-cost decision

| Concern | Decision |
| --- | --- |
| Change classification | `New` |
| Independent consumer goal | 管理员在多个知识库之间浏览、选择并翻页；这是独立于任何命令的展示目标 |
| Parameter ownership and derivation | 分页与筛选由调用方拥有；总数与页元数据由服务端派生 |
| Direct/no-new-interface alternative | 让调用方缓存创建时的返回值。不足：无跨会话持久、无并发变更可见性、无翻页，且管理员可能并非创建者 |
| Caller use of result | 展示列表并选择其中一个进入详情或文档管理 |
| Round trips and failure points | 每页一次 RTT；失败点为分页参数越界与查询失败 |
| Verdict | `Add`，覆盖 `REQ-002` |

[L978-987]
##### API style and CQRS semantics

| Concern | Decision/evidence |
| --- | --- |
| Protocol style | `REST Query` |
| CQRS role | 只读查询，拥有零业务写入 |
| Resource/task semantics | `GET /api/v1/knowledge-bases` 返回集合的一页 |
| Read/write and side effects | 只读 `knowledge_base`；无审计以外的副作用 |
| Consistency and idempotency | 读已提交；页间可能出现并发新增或删除（见 `§9.2.2` 消费方逻辑） |
| Why this style | 集合浏览是标准 HTTP 资源查询；无需游标或独立读模型 |

[L989-998]
##### Identity and purpose

| Concern | Definition |
| --- | --- |
| Purpose/owner/consumer | 分页浏览未删除的知识库；由 adapter 拥有，由管理员调用 |
| Protocol and endpoint | `HTTP GET /api/v1/knowledge-bases` |
| Content type/version | `application/json`；v1 |
| Auth/permission/tenant | `X-Research-Api-Key`；无租户 |
| Timeout/retry/rate limit | 无独立设置 |
| Idempotency/concurrency | 只读且幂等；并发变更可能影响后续页 |

[L1000-1008]
##### Request parameters

| Name | Location | Type/format | Required/null | Default | Validation/range/enum | Meaning | Example | Source |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| `X-Research-Api-Key` | Header | String | 必填 | 无 | 常量时间比较 | 服务凭据 | `demo-key` | 既有过滤器 |
| `page` | Query | int | 可选 | `1` | ≥ 1 | 页码（1 基准） | `1` | 调用方 |
| `size` | Query | int | 可选 | `20` | 1-100 | 每页条数 | `20` | 调用方 |
| `keyword` | Query | String | 可选 | 无 | trim 后 0-64；对 `code` 与 `name` 做包含匹配 | 关键字 | `docs` | 调用方 |
| `embeddingModel` | Query | String | 可选 | 无 | 已注册逻辑名 | 按模型筛选 | `openai-small` | 调用方 |

[L1010-1040]
##### Success response

HTTP `200 OK`。空结果返回空数组而非 `null`；排序为 `createdAt DESC, knowledgeBaseId DESC`，保证稳定分页。

```jsonc
{
  "items": [ // 必填。当前页数据；无匹配时为空数组，不为 null。
    {
      "knowledgeBaseId": "kb-01J5K9", // 稳定标识，用于详情与文档路径。
      "code": "product-docs", // 业务键。
      "name": "产品文档库", // 展示名称。
      "embeddingModel": "openai-small", // 已冻结的逻辑模型名。
      "documentCount": 12, // 未删除文档数。
      "status": "ACTIVE", // 生命周期状态。
      "createdAt": "2026-09-10T03:51:00.000Z", // 创建时间，UTC Instant。
      "updatedAt": "2026-09-10T04:10:00.000Z" // 最后更新时间，用于展示与缓存失效判断。
    }
  ],
  "page": 1, // 当前页码，1 基准。
  "size": 20, // 生效的每页条数。
  "totalElements": 1, // 满足筛选条件的总行数。
  "totalPages": 1, // 由 totalElements 与 size 派生。
  "hasNext": false // 是否存在下一页。
}
```

| Field path | Type/format | Required/null/default | Validation/enum/precision | Meaning and source | Frontend use |
| --- | --- | --- | --- | --- | --- |
| `items[].documentCount` | int | 必填 | ≥ 0 | 聚合计数 | 列表展示 |
| `totalPages` | int | 必填 | ≥ 0 | 派生 | 分页控件 |
| `hasNext` | boolean | 必填 | — | 派生 | 下一页按钮 |

[L1042-1060]
##### Error responses

| Condition | HTTP status | Business code | Response shape | Retryable | Frontend handling |
| --- | --- | --- | --- | --- | --- |
| 分页参数越界或类型错误 | 400 | `KNOWLEDGE_VALIDATION_ERROR` | 既有错误体 + `fieldErrors` | 修正后 | 重置分页控件 |
| API Key 缺失或错误 | 401 | `RESEARCH_UNAUTHORIZED` | 既有错误体 | 修正凭据后 | 提示配置 key |
| 未预期失败 | 500 | `KNOWLEDGE_INTERNAL_ERROR` | 既有错误体 | 携带 traceId 上报 | 通用错误提示 |

```jsonc
{
  "code": "KNOWLEDGE_VALIDATION_ERROR", // 稳定机器码。
  "message": "validation failed", // 安全摘要。
  "traceId": "4e9d6938", // 关联标识。
  "timestamp": "2026-09-10T03:51:00.000Z", // 服务端 UTC Instant。
  "fieldErrors": { // 字段名 -> 消息列表；本接口只可能出现 page 或 size。
    "size": ["must be between 1 and 100"] // 越界消息。
  }
}
```

[L1062-1071]
##### Interface logic for frontend and consumers

1. 调用方必须携带有效 API Key。
2. 校验顺序：分页与筛选参数约束 -> 逻辑模型名（若提供）已注册。
3. 查询只返回 `is_deleted = false` 的行；`keyword` 按 `code` 与 `name` 做包含匹配。
4. 排序固定为 `created_at DESC, id DESC`，`id` 是确定性次序打破者；`totalElements` 与 `items` 在同一查询快照内。
5. 无持久化写入、无缓存、无外部调用。
6. 页间并发新增可能使后续页出现重复或遗漏条目；本契约不承诺快照一致性，前端应整表刷新而非增量拼接。
7. 前端应保持筛选状态与页码同步到查询参数，便于分享与返回；空结果展示空态而非错误。
8. `size` 上限 100；超出直接 400，服务端不静默截断。

[L986]
| Consistency and idempotency | 读已提交；页间可能出现并发新增或删除（见 `§9.2.2` 消费方逻辑） |

[L998]
| Idempotency/concurrency | 只读且幂等；并发变更可能影响后续页 |

[L1012]
HTTP `200 OK`。空结果返回空数组而非 `null`；排序为 `createdAt DESC, knowledgeBaseId DESC`，保证稳定分页。

[L1067-1069]
4. 排序固定为 `created_at DESC, id DESC`，`id` 是确定性次序打破者；`totalElements` 与 `items` 在同一查询快照内。
5. 无持久化写入、无缓存、无外部调用。
6. 页间并发新增可能使后续页出现重复或遗漏条目；本契约不承诺快照一致性，前端应整表刷新而非增量拼接。

[L1071]
8. `size` 上限 100；超出直接 400，服务端不静默截断。

[L964]
#### 9.2.2 API-002 — 知识库分页列表

[L1095]
#### 9.2.3 API-003 — 知识库详情

[L1097-1107]
##### Necessity and interaction-cost decision

| Concern | Decision |
| --- | --- |
| Change classification | `New` |
| Independent consumer goal | 管理页面需要完整配置（含不可改的模型与分块参数）与文档计数；列表为避免膨胀不返回 `chunkConfig` |
| Parameter ownership and derivation | 标识来自路径；全部字段由服务端派生 |
| Direct/no-new-interface alternative | 让列表返回全部字段。不足：列表会携带大对象并放大分页成本，而详情只在打开一个知识库时需要 |
| Caller use of result | 展示配置、判断是否可上传、作为删除前的确认依据 |
| Round trips and failure points | 打开详情一次 RTT；失败点为不存在与查询失败 |
| Verdict | `Add`，覆盖 `REQ-002` |

[L1109-1118]
##### API style and CQRS semantics

| Concern | Decision/evidence |
| --- | --- |
| Protocol style | `REST Query` |
| CQRS role | 只读查询 |
| Resource/task semantics | `GET /api/v1/knowledge-bases/{knowledgeBaseId}` 读取单个资源表示 |
| Read/write and side effects | 只读 |
| Consistency and idempotency | 读已提交；重复读取结果一致（除计数随上传变化） |
| Why this style | 单资源读取是标准 HTTP 语义 |

[L1120-1129]
##### Identity and purpose

| Concern | Definition |
| --- | --- |
| Purpose/owner/consumer | 读取一个知识库的完整配置与计数；由 adapter 拥有 |
| Protocol and endpoint | `HTTP GET /api/v1/knowledge-bases/{knowledgeBaseId}` |
| Content type/version | `application/json`；v1 |
| Auth/permission/tenant | `X-Research-Api-Key`；无租户 |
| Timeout/retry/rate limit | 无独立设置 |
| Idempotency/concurrency | 只读且幂等 |

[L1131-1136]
##### Request parameters

| Name | Location | Type/format | Required/null | Default | Validation/range/enum | Meaning | Example | Source |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| `X-Research-Api-Key` | Header | String | 必填 | 无 | 常量时间比较 | 服务凭据 | `demo-key` | 既有过滤器 |
| `knowledgeBaseId` | Path | String | 必填、非空 | 无 | `[A-Za-z0-9._-]{1,64}` | 知识库标识 | `kb-01J5K9` | 上游接口 |

[L1138-1167]
##### Success response

HTTP `200 OK`；响应体与 `API-001` 的成功响应同形（同一 VO），其中 `chunkConfig` 按创建时的策略返回对应字段集合。

| Field path | Type/format | Required/null/default | Validation/enum/precision | Meaning and source | Frontend use |
| --- | --- | --- | --- | --- | --- |
| `chunkConfig` | Object | 必填、非空 | 字段集合随 `chunkStrategy` 变化 | 冻结参数 | 展示；当 `chunkStrategy=MARKDOWN_HEADING` 时含 `headingLevels` |
| `documentCount` | int | 必填 | ≥ 0 | 未删除文档计数 | 判断是否为空库 |

```jsonc
{
  "knowledgeBaseId": "kb-01J5K9", // 稳定标识。
  "code": "product-docs", // 业务键。
  "name": "产品文档库", // 展示名称。
  "description": "内部产品手册", // 描述；无则为 null。
  "embeddingModel": "openai-small", // 冻结的逻辑模型名。
  "chunkStrategy": "MARKDOWN_HEADING", // 冻结的策略；决定 chunkConfig 的字段集合。
  "chunkConfig": { // 冻结的分块参数。
    "maxTokensPerChunk": 512, // 单分块 token 上限。
    "overlapTokens": 64, // 相邻重叠 token 数。
    "minChunkChars": 1, // 最短分块字符数。
    "headingLevels": [1, 2, 3] // 仅 MARKDOWN_HEADING 返回；参与切分的标题层级。
  },
  "documentCount": 12, // 未删除文档数。
  "status": "ACTIVE", // 生命周期状态。
  "createdAt": "2026-09-10T03:51:00.000Z", // 创建时间。
  "updatedAt": "2026-09-10T04:10:00.000Z", // 最后更新时间。
  "traceId": "4e9d6938" // 关联标识。
}
```

[L1169-1186]
##### Error responses

| Condition | HTTP status | Business code | Response shape | Retryable | Frontend handling |
| --- | --- | --- | --- | --- | --- |
| 标识格式非法 | 400 | `KNOWLEDGE_VALIDATION_ERROR` | 既有错误体 | 修正后 | 返回列表 |
| API Key 缺失或错误 | 401 | `RESEARCH_UNAUTHORIZED` | 既有错误体 | 修正凭据后 | 提示配置 key |
| 不存在或已删除 | 404 | `KNOWLEDGE_BASE_NOT_FOUND` | 既有错误体 | 否 | 返回列表并提示已删除 |
| 未预期失败 | 500 | `KNOWLEDGE_INTERNAL_ERROR` | 既有错误体 | 携带 traceId 上报 | 通用错误提示 |

```jsonc
{
  "code": "KNOWLEDGE_BASE_NOT_FOUND", // 稳定机器码；已删除与不存在共用同一码，不泄露存在性差异。
  "message": "knowledge base not found", // 安全摘要。
  "traceId": "4e9d6938", // 关联标识。
  "timestamp": "2026-09-10T03:51:00.000Z", // 服务端 UTC Instant。
  "fieldErrors": {} // 本接口无字段错误，恒为空对象。
}
```

[L1188-1197]
##### Interface logic for frontend and consumers

1. 调用方必须携带有效 API Key；标识来自路径。
2. 校验顺序：路径标识格式 -> 资源存在且未删除。
3. 查询单行并按 `chunkStrategy` 组装 `chunkConfig` 的字段子集；不同策略返回的字段不同，前端必须按策略渲染。
4. `documentCount` 由未删除文档计数派生，与文档列表接口在同一筛选条件下口径一致。
5. 无写入、无缓存、无外部调用。
6. 已删除与不存在返回同一个 404，不区分存在性。
7. 前端在详情页进入时拉取本接口；上传完成后应重新拉取以刷新 `documentCount`。
8. 若页面已在编辑名称，需在保存前用本接口的 `updatedAt` 做提示性校验，避免覆盖他人修改（服务端不做乐观锁）。

[L1117]
| Consistency and idempotency | 读已提交；重复读取结果一致（除计数随上传变化） |

[L1129]
| Idempotency/concurrency | 只读且幂等 |

[L1193]
3. 查询单行并按 `chunkStrategy` 组装 `chunkConfig` 的字段子集；不同策略返回的字段不同，前端必须按策略渲染。

[L1197]
8. 若页面已在编辑名称，需在保存前用本接口的 `updatedAt` 做提示性校验，避免覆盖他人修改（服务端不做乐观锁）。

[L1095]
#### 9.2.3 API-003 — 知识库详情

[L1220]
#### 9.2.4 API-004 — 改知识库名称与描述

[L1222-1232]
##### Necessity and interaction-cost decision

| Concern | Decision |
| --- | --- |
| Change classification | `New` |
| Independent consumer goal | 管理员修正展示名称与描述；这是唯一允许变更的字段集合 |
| Parameter ownership and derivation | 新名称与描述由管理员拥有；`updatedAt` 与审计字段由服务端派生 |
| Direct/no-new-interface alternative | 无直接替代；创建后改名是真实且不可省略的管理动作 |
| Caller use of result | 更新本地展示并刷新详情 |
| Round trips and failure points | 一次 RTT；失败点为校验、不存在与写库失败；不涉及的配置字段被显式拒绝 |
| Verdict | `Add`，覆盖 `REQ-002` |

[L1234-1243]
##### API style and CQRS semantics

| Concern | Decision/evidence |
| --- | --- |
| Protocol style | `REST Command` |
| CQRS role | 状态变更 Command；只允许改可变字段 |
| Resource/task semantics | `PUT` 是对已知 URI 的完整替换语义；本接口的"完整表示"就是 `name` 与 `description` 两个可写字段，其余字段为只读 |
| Read/write and side effects | 更新一行；无事件、无外部调用 |
| Consistency and idempotency | 单行更新在本地事务内；相同请求重复提交结果一致，视为幂等 |
| Why this style | 可写字段集合固定且小，`PUT` 语义成立；用 `PATCH` 会引入缺席/空值歧义 |

[L1245-1254]
##### Identity and purpose

| Concern | Definition |
| --- | --- |
| Purpose/owner/consumer | 修改知识库的展示名称与描述；由 adapter 拥有 |
| Protocol and endpoint | `HTTP PUT /api/v1/knowledge-bases/{knowledgeBaseId}` |
| Content type/version | `application/json`；v1 |
| Auth/permission/tenant | `X-Research-Api-Key`；无租户 |
| Timeout/retry/rate limit | 无独立设置 |
| Idempotency/concurrency | 幂等；并发写以后提交者为准，无乐观锁 |

[L1256-1272]
##### Request parameters

| Name | Location | Type/format | Required/null | Default | Validation/range/enum | Meaning | Example | Source |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| `X-Research-Api-Key` | Header | String | 必填 | 无 | 常量时间比较 | 服务凭据 | `demo-key` | 既有过滤器 |
| `knowledgeBaseId` | Path | String | 必填、非空 | 无 | `[A-Za-z0-9._-]{1,64}` | 知识库标识 | `kb-01J5K9` | 上游接口 |
| `name` | Body | String | 必填、非空 | 无 | trim 后 1-128 | 新名称 | `产品文档库（内网）` | 管理员 |
| `description` | Body | String | 可选、允许 `null` | `null` | trim 后 0-512；显式 `null` 表示清空 | 新描述 | `内网产品手册` | 管理员 |

```jsonc
{
  "name": "产品文档库（内网）", // 必填。新展示名称，trim 后 1-128 字符。
  "description": "内网产品手册" // 可选。显式 null 表示清空描述；缺省时也按清空处理（PUT 完整替换语义）。
}
```

请求体中出现 `embeddingModel`、`chunkStrategy`、`chunkConfig`、`code` 或任何其它字段时返回 400；这些字段在创建后不可修改，静默忽略会让调用方误以为修改生效。

[L1274-1301]
##### Success response

HTTP `200 OK`，返回与 `API-003` 同形的完整知识库表示（含更新后的 `name`、`description` 与 `updatedAt`）。

| Field path | Type/format | Required/null/default | Validation/enum/precision | Meaning and source | Frontend use |
| --- | --- | --- | --- | --- | --- |
| `updatedAt` | RFC 3339 UTC | 必填 | 毫秒精度 | 本次更新的提交时间 | 刷新展示 |

```jsonc
{
  "knowledgeBaseId": "kb-01J5K9", // 稳定标识。
  "code": "product-docs", // 业务键，本次未变。
  "name": "产品文档库（内网）", // 更新后的名称。
  "description": "内网产品手册", // 更新后的描述；清空时为 null。
  "embeddingModel": "openai-small", // 冻结字段，未变。
  "chunkStrategy": "TOKEN", // 冻结字段，未变。
  "chunkConfig": { // 冻结字段，未变。
    "maxTokensPerChunk": 512, // 单分块 token 上限。
    "overlapTokens": 64, // 相邻重叠 token 数。
    "minChunkChars": 1 // 最短分块字符数。
  },
  "documentCount": 12, // 未删除文档数。
  "status": "ACTIVE", // 生命周期状态。
  "createdAt": "2026-09-10T03:51:00.000Z", // 创建时间。
  "updatedAt": "2026-09-10T05:02:00.000Z", // 本次更新的提交时间。
  "traceId": "4e9d6938" // 关联标识。
}
```

[L1303-1323]
##### Error responses

| Condition | HTTP status | Business code | Response shape | Retryable | Frontend handling |
| --- | --- | --- | --- | --- | --- |
| 字段校验失败 | 400 | `KNOWLEDGE_VALIDATION_ERROR` | 既有错误体 + `fieldErrors` | 修正后 | 标红字段 |
| 提交了不可修改字段 | 400 | `KNOWLEDGE_IMMUTABLE_FIELD` | 既有错误体 + `fieldErrors` | 修正后 | 移除该字段并提示 |
| API Key 缺失或错误 | 401 | `RESEARCH_UNAUTHORIZED` | 既有错误体 | 修正凭据后 | 提示配置 key |
| 不存在或已删除 | 404 | `KNOWLEDGE_BASE_NOT_FOUND` | 既有错误体 | 否 | 返回列表 |
| 未预期失败 | 500 | `KNOWLEDGE_INTERNAL_ERROR` | 既有错误体 | 携带 traceId 上报 | 通用错误提示 |

```jsonc
{
  "code": "KNOWLEDGE_IMMUTABLE_FIELD", // 稳定机器码；表示请求含创建后不可修改的字段。
  "message": "immutable field cannot be updated", // 安全摘要。
  "traceId": "4e9d6938", // 关联标识。
  "timestamp": "2026-09-10T05:02:00.000Z", // 服务端 UTC Instant。
  "fieldErrors": { // 字段名 -> 消息列表。
    "embeddingModel": ["immutable after creation"] // 被拒绝的字段与原因。
  }
}
```

[L1325-1334]
##### Interface logic for frontend and consumers

1. 调用方必须携带有效 API Key；标识来自路径。
2. 校验顺序：标识格式 -> 字段约束 -> 不可修改字段探测 -> 资源存在且未删除。
3. 不可修改字段的探测必须在写库之前完成，且不能依赖序列化层静默丢弃字段（本接口关闭未知字段忽略）。
4. 更新单行的 `name`、`description`、`updated_at` 与 `update_user_id`；其余列不变。
5. 无事件、无缓存失效、无外部调用；向量与文档不受影响。
6. 重复提交同一请求结果一致，视为幂等；并发修改后提交者覆盖先前提交，服务端不做乐观锁并在文档中说明。
7. 前端应以服务端返回的完整表示刷新本地状态，而不是仅本地改写 `name`。
8. 描述字段的"缺省"与"显式 `null`"在本接口都表示清空（`PUT` 完整替换语义），前端必须发送明确的意图。

[L1242]
| Consistency and idempotency | 单行更新在本地事务内；相同请求重复提交结果一致，视为幂等 |

[L1254]
| Idempotency/concurrency | 幂等；并发写以后提交者为准，无乐观锁 |

[L1330]
4. 更新单行的 `name`、`description`、`updated_at` 与 `update_user_id`；其余列不变。

[L1332]
6. 重复提交同一请求结果一致，视为幂等；并发修改后提交者覆盖先前提交，服务端不做乐观锁并在文档中说明。

[L1220]
#### 9.2.4 API-004 — 改知识库名称与描述

[L1357]
#### 9.2.5 API-005 — 删知识库

[L1359-1369]
##### Necessity and interaction-cost decision

| Concern | Decision |
| --- | --- |
| Change classification | `New` |
| Independent consumer goal | 管理员彻底撤销一个知识库，包括其文件、文本与向量；这是不可逆的独立目标 |
| Parameter ownership and derivation | 标识来自路径；级联范围由服务端按归属关系派生 |
| Direct/no-new-interface alternative | 无；没有替代的撤销路径 |
| Caller use of result | 从列表移除并返回列表页 |
| Round trips and failure points | 一次 RTT；失败点为不存在与存在处理中文档 |
| Verdict | `Add`，覆盖 `REQ-002` |

[L1371-1380]
##### API style and CQRS semantics

| Concern | Decision/evidence |
| --- | --- |
| Protocol style | `REST Command` |
| CQRS role | 删除型 Command，拥有级联删除的权威 |
| Resource/task semantics | `DELETE` 删除该资源及其从属数据；语义上幂等 |
| Read/write and side effects | 删除本地文件、删除向量分块、软删知识库行与文档行 |
| Consistency and idempotency | 级联在一个本地事务内对数据库生效；文件与向量的删除在事务内完成，失败则整体回滚 |
| Why this style | 标准资源删除语义；无需任务资源 |

[L1382-1391]
##### Identity and purpose

| Concern | Definition |
| --- | --- |
| Purpose/owner/consumer | 删除知识库及其全部从属数据；由 adapter 拥有 |
| Protocol and endpoint | `HTTP DELETE /api/v1/knowledge-bases/{knowledgeBaseId}` |
| Content type/version | 无请求体；成功无响应体 |
| Auth/permission/tenant | `X-Research-Api-Key`；无租户 |
| Timeout/retry/rate limit | 大知识库的级联删除可能耗时；由容器超时与文档说明约束 |
| Idempotency/concurrency | 已删除再删返回 404；并发删除只有一个成功 |

[L1393-1399]
##### Request parameters

| Name | Location | Type/format | Required/null | Default | Validation/range/enum | Meaning | Example | Source |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| `X-Research-Api-Key` | Header | String | 必填 | 无 | 常量时间比较 | 服务凭据 | `demo-key` | 既有过滤器 |
| `knowledgeBaseId` | Path | String | 必填、非空 | 无 | `[A-Za-z0-9._-]{1,64}` | 知识库标识 | `kb-01J5K9` | 上游接口 |
| Body | Body | — | 无 | 无 | 请求体必须为空 | 不适用 | 无 | 不适用 |

[L1401-1407]
##### Success response

HTTP `204 No Content`，无响应体。

| Field path | Type/format | Required/null/default | Validation/enum/precision | Meaning and source | Frontend use |
| --- | --- | --- | --- | --- | --- |
| Body | — | 无 | — | `204` 无内容 | 前端据状态码判定成功 |

[L1409-1427]
##### Error responses

| Condition | HTTP status | Business code | Response shape | Retryable | Frontend handling |
| --- | --- | --- | --- | --- | --- |
| 标识格式非法 | 400 | `KNOWLEDGE_VALIDATION_ERROR` | 既有错误体 | 修正后 | 返回列表 |
| API Key 缺失或错误 | 401 | `RESEARCH_UNAUTHORIZED` | 既有错误体 | 修正凭据后 | 提示配置 key |
| 不存在或已删除 | 404 | `KNOWLEDGE_BASE_NOT_FOUND` | 既有错误体 | 否 | 从本地列表移除 |
| 仍有处理中的文档 | 409 | `KNOWLEDGE_BASE_BUSY` | 既有错误体 | 稍后重试 | 提示等待处理完成 |
| 级联删除失败 | 500 | `KNOWLEDGE_INTERNAL_ERROR` | 既有错误体 | 携带 traceId 上报 | 通用错误提示；状态未变 |

```jsonc
{
  "code": "KNOWLEDGE_BASE_BUSY", // 稳定机器码；表示仍有非终态文档。
  "message": "knowledge base still has documents in progress", // 安全摘要。
  "traceId": "4e9d6938", // 关联标识。
  "timestamp": "2026-09-10T05:20:00.000Z", // 服务端 UTC Instant。
  "fieldErrors": {} // 本接口无字段错误。
}
```

[L1429-1438]
##### Interface logic for frontend and consumers

1. 调用方必须携带有效 API Key；请求体必须为空。
2. 校验顺序：标识格式 -> 资源存在且未删除 -> 不存在非终态文档。
3. 若存在处理中的文档，返回 409 并要求调用方先等待其进入终态；服务端不会自动取消正在执行的 outbox 投递。
4. 级联顺序：逐个文档删除本地文件与向量分块，然后软删文档行；最后软删知识库行。全部在一个本地事务内对数据库生效。
5. 文件删除发生在事务内会导致长事务；因此实现必须先收集待删标识，再在事务内执行数据库软删，文件删除失败时以补偿记录处理并在响应前完成。
6. 重复删除返回 404；并发删除只有一个成功，另一个得到 404。
7. 前端必须在删除前要求显式确认，不得把删除做成一次点击；成功后可返回列表并整表刷新。
8. 删除不产生任何异步任务，也不提供撤销；文档与向量在成功响应后不可恢复。

[L1379]
| Consistency and idempotency | 级联在一个本地事务内对数据库生效；文件与向量的删除在事务内完成，失败则整体回滚 |

[L1391]
| Idempotency/concurrency | 已删除再删返回 404；并发删除只有一个成功 |

[L1434-1436]
4. 级联顺序：逐个文档删除本地文件与向量分块，然后软删文档行；最后软删知识库行。全部在一个本地事务内对数据库生效。
5. 文件删除发生在事务内会导致长事务；因此实现必须先收集待删标识，再在事务内执行数据库软删，文件删除失败时以补偿记录处理并在响应前完成。
6. 重复删除返回 404；并发删除只有一个成功，另一个得到 404。

[L1438]
8. 删除不产生任何异步任务，也不提供撤销；文档与向量在成功响应后不可恢复。

[L1357]
#### 9.2.5 API-005 — 删知识库

[L1460]
#### 9.2.6 API-006 — 上传文档

[L1462-1472]
##### Necessity and interaction-cost decision

| Concern | Decision |
| --- | --- |
| Change classification | `New` |
| Independent consumer goal | 管理员把一份文件交给知识库处理；这是全部知识库能力的入口目标 |
| Parameter ownership and derivation | 文件与目标知识库由管理员拥有；文档标识、状态、分块数、存储标识与任务记录由服务端派生；`embeddingModel` 与分块参数由知识库派生，不接受请求覆盖 |
| Direct/no-new-interface alternative | 无；没有替代的文档入口 |
| Caller use of result | 用返回的文档标识轮询 `API-008` 直到终态；结果不被转发给另一个命令 |
| Round trips and failure points | 一次 multipart RTT，随后由后台执行嵌入；失败点为校验、无抽取器、存储失败与事务失败 |
| Verdict | `Add`，覆盖 `REQ-003`, `REQ-004`, `REQ-007` |

[L1474-1483]
##### API style and CQRS semantics

| Concern | Decision/evidence |
| --- | --- |
| Protocol style | `REST Command` |
| CQRS role | 任务型 Command：它启动一条异步处理链并立即返回 |
| Resource/task semantics | `POST /api/v1/knowledge-bases/{knowledgeBaseId}/documents` 创建从属文档资源，同时入队一条后台任务 |
| Read/write and side effects | 写入本地文件、`knowledge_document` 行与 outbox 行；不直接调用模型 |
| Consistency and idempotency | 文档行与 outbox 行在同一本地事务内；文件在事务之外；非幂等，同一文件重复上传产生两个文档 |
| Why this style | 嵌入耗时不可预测，同步会超时；`202` + 状态轮询是最小且诚实的表达 |

[L1485-1494]
##### Identity and purpose

| Concern | Definition |
| --- | --- |
| Purpose/owner/consumer | 上传一份文件并启动其摄取；由 adapter 拥有 |
| Protocol and endpoint | `HTTP POST /api/v1/knowledge-bases/{knowledgeBaseId}/documents` |
| Content type/version | 请求 `multipart/form-data`；响应 `application/json`；v1 |
| Auth/permission/tenant | `X-Research-Api-Key`；无租户 |
| Timeout/retry/rate limit | 无独立超时；服务端不重试上传；重复上传是新文档 |
| Idempotency/concurrency | 非幂等；并发上传互不影响 |

[L1496-1505]
##### Request parameters

| Name | Location | Type/format | Required/null | Default | Validation/range/enum | Meaning | Example | Source |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| `X-Research-Api-Key` | Header | String | 必填 | 无 | 常量时间比较 | 服务凭据 | `demo-key` | 既有过滤器 |
| `knowledgeBaseId` | Path | String | 必填、非空 | 无 | `[A-Za-z0-9._-]{1,64}` | 目标知识库 | `kb-01J5K9` | 上游接口 |
| `file` | Multipart | File | 必填、非空 | 无 | 非空；≤ 20MB；MIME 或扩展名须被某个已注册抽取器支持 | 待处理文件 | `report.pdf` | 管理员 |
| `displayName` | Multipart | String | 可选 | 原始文件名 | trim 后 1-255 | 展示名 | `2026 手册` | 管理员 |

请求体为 multipart，不使用 JSON 形状；无 JSON 请求体注释块。

[L1507-1532]
##### Success response

HTTP `202 Accepted`。

```jsonc
{
  "documentId": "doc-01J5K9", // 服务端生成的文档标识；用于详情、重新处理与删除。
  "knowledgeBaseId": "kb-01J5K9", // 所属知识库标识。
  "displayName": "report.pdf", // 展示名；未提供时取原始文件名。
  "fileName": "report.pdf", // 原始文件名，用于诊断与重新解析。
  "mimeType": "application/pdf", // 实际识别的内容类型；未知时为 null。
  "sizeBytes": 182734, // 文件字节数。
  "status": "PENDING", // 处理状态；上传后固定为 PENDING，随后由后台推进。
  "chunkCount": 0, // 已写入的分块数；未完成时为 0。
  "errorCode": null, // 失败时的稳定错误码；非失败状态为 null。
  "errorMessage": null, // 失败时的安全摘要；非失败状态为 null。
  "createdAt": "2026-09-10T05:30:00.000Z", // 上传时间，UTC Instant。
  "traceId": "4e9d6938" // 关联标识。
}
```

| Field path | Type/format | Required/null/default | Validation/enum/precision | Meaning and source | Frontend use |
| --- | --- | --- | --- | --- | --- |
| `status` | String enum | 必填 | `PENDING`/`PROCESSING`/`SUCCEEDED`/`FAILED`/`DEAD` | 文档状态机 | 轮询与展示 |
| `chunkCount` | int | 必填 | ≥ 0 | 成功写入的分块数 | 展示 |
| `errorCode` | String | 失败时必填，否则 `null` | 稳定机器码，不含供应商细节 | 失败原因 | 展示与重试决策 |

[L1534-1556]
##### Error responses

| Condition | HTTP status | Business code | Response shape | Retryable | Frontend handling |
| --- | --- | --- | --- | --- | --- |
| multipart 缺 `file` 或字段非法 | 400 | `KNOWLEDGE_VALIDATION_ERROR` | 既有错误体 + `fieldErrors` | 修正后 | 标红字段 |
| 无可用抽取器 | 400 | `KNOWLEDGE_EXTRACTOR_MISSING` | 既有错误体 | 换格式或引入依赖 | 展示已支持格式 |
| 文件超过大小上限 | 413 | `KNOWLEDGE_FILE_TOO_LARGE` | 既有错误体 | 否 | 提示上限 |
| API Key 缺失或错误 | 401 | `RESEARCH_UNAUTHORIZED` | 既有错误体 | 修正凭据后 | 提示配置 key |
| 知识库不存在或已删除 | 404 | `KNOWLEDGE_BASE_NOT_FOUND` | 既有错误体 | 否 | 返回列表 |
| 文档数达到上限 | 429 | `KNOWLEDGE_CAPACITY_EXHAUSTED` | 既有错误体 + `Retry-After` | 稍后重试 | 提示清理旧文档 |
| 存储或摄取前置失败 | 500 | `KNOWLEDGE_INTERNAL_ERROR` | 既有错误体 | 携带 traceId 上报 | 通用错误提示；无部分持久化 |

```jsonc
{
  "code": "KNOWLEDGE_EXTRACTOR_MISSING", // 稳定机器码；表示该格式当前无可用抽取器。
  "message": "no document extractor available for the uploaded format", // 安全摘要。
  "traceId": "4e9d6938", // 关联标识。
  "timestamp": "2026-09-10T05:30:00.000Z", // 服务端 UTC Instant。
  "fieldErrors": { // 字段名 -> 消息列表。
    "file": ["registered mime types: text/plain, text/markdown, application/json"] // 已注册能力，用于引导调用方换格式或引入依赖。
  }
}
```

[L1558-1567]
##### Interface logic for frontend and consumers

1. 调用方必须携带有效 API Key 与 multipart 请求；`file` 必填。
2. 校验顺序：知识库存在且未删除 -> 文档数未达上限 -> 文件大小与字段约束 -> 抽取路由命中。
3. 服务端先把文件写入 `RagDocumentStorage`，再调用 `RagExtractionService#extract` 取得文本与结构元数据；这两步都在数据库事务之外。
4. 随后在**一个本地事务内**插入 `knowledge_document` 行（含 `content` 文本）并入队一条 `channel=rag-ingest` 的 outbox 记录；该事务是"文档存在即会被处理"的保证。
5. 事务失败时补偿删除已写入的文件；补偿失败时记录孤儿文件告警但不改变响应语义。
6. 返回 `202` 与 `PENDING` 状态；嵌入由后台 outbox 投递异步完成，调用方应轮询 `API-008`。
7. 前端应禁用重复提交按钮直至拿到响应，并在拿到 `documentId` 后以指数退避轮询详情直到终态；不要在轮询期间阻塞其它操作。
8. 重复上传同一文件会产生两个独立文档并各自计费嵌入；本接口不做内容去重，也不接受客户端提供 `embeddingModel` 或分块参数覆盖知识库配置。

[L1482]
| Consistency and idempotency | 文档行与 outbox 行在同一本地事务内；文件在事务之外；非幂等，同一文件重复上传产生两个文档 |

[L1494]
| Idempotency/concurrency | 非幂等；并发上传互不影响 |

[L1563-1564]
4. 随后在**一个本地事务内**插入 `knowledge_document` 行（含 `content` 文本）并入队一条 `channel=rag-ingest` 的 outbox 记录；该事务是"文档存在即会被处理"的保证。
5. 事务失败时补偿删除已写入的文件；补偿失败时记录孤儿文件告警但不改变响应语义。

[L1567]
8. 重复上传同一文件会产生两个独立文档并各自计费嵌入；本接口不做内容去重，也不接受客户端提供 `embeddingModel` 或分块参数覆盖知识库配置。

### 13.2 API-007 – API-012（逐字摘录）

[L1591-1591]
#### 9.2.7 API-007 — 文档分页列表

[L1593-1625]
##### Necessity and interaction-cost decision

| Concern | Decision |
| --- | --- |
| Change classification | `New` |
| Independent consumer goal | 管理员在一个知识库内浏览文档并观察各自状态与失败 |
| Parameter ownership and derivation | 筛选与分页由调用方拥有；计数由服务端派生 |
| Direct/no-new-interface alternative | 复用知识库详情中的 `documentCount`。不足：无法逐条查看状态、失败原因与标识 |
| Caller use of result | 展示列表、定位失败文档并触发重新处理 |
| Round trips and failure points | 每页一次 RTT |
| Verdict | `Add`，覆盖 `REQ-003`, `REQ-005` |

##### API style and CQRS semantics

| Concern | Decision/evidence |
| --- | --- |
| Protocol style | `REST Query` |
| CQRS role | 只读查询；状态由后台任务写入，本接口不修改 |
| Resource/task semantics | `GET` 返回知识库下文档集合的一页 |
| Read/write and side effects | 只读 `knowledge_document` |
| Consistency and idempotency | 读已提交；状态可能被后台并发推进 |
| Why this style | 标准从属集合查询 |

##### Identity and purpose

| Concern | Definition |
| --- | --- |
| Purpose/owner/consumer | 分页浏览某知识库下未删除的文档；由 adapter 拥有 |
| Protocol and endpoint | `HTTP GET /api/v1/knowledge-bases/{knowledgeBaseId}/documents` |
| Content type/version | `application/json`；v1 |
| Auth/permission/tenant | `X-Research-Api-Key`；无租户 |
| Timeout/retry/rate limit | 无独立设置 |
| Idempotency/concurrency | 只读且幂等 |

[L1627-1636]
##### Request parameters

| Name | Location | Type/format | Required/null | Default | Validation/range/enum | Meaning | Example | Source |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| `X-Research-Api-Key` | Header | String | 必填 | 无 | 常量时间比较 | 服务凭据 | `demo-key` | 既有过滤器 |
| `knowledgeBaseId` | Path | String | 必填、非空 | 无 | `[A-Za-z0-9._-]{1,64}` | 所属知识库 | `kb-01J5K9` | 上游接口 |
| `page` | Query | int | 可选 | `1` | ≥ 1 | 页码（1 基准） | `1` | 调用方 |
| `size` | Query | int | 可选 | `20` | 1-100 | 每页条数 | `20` | 调用方 |
| `status` | Query | String enum | 可选 | 无 | `PENDING`/`PROCESSING`/`SUCCEEDED`/`FAILED`/`DEAD` | 按状态筛选 | `FAILED` | 调用方 |
| `keyword` | Query | String | 可选 | 无 | trim 后 0-64；对 `display_name` 做包含匹配 | 文件名关键字 | `report` | 调用方 |

[L1638-1668]
##### Success response

HTTP `200 OK`；排序 `created_at DESC, id DESC`。

```jsonc
{
  "items": [ // 必填。当前页文档；无匹配时为空数组。
    {
      "documentId": "doc-01J5K9", // 文档标识。
      "displayName": "report.pdf", // 展示名。
      "mimeType": "application/pdf", // 内容类型；未知时为 null。
      "sizeBytes": 182734, // 文件字节数。
      "status": "SUCCEEDED", // 处理状态。
      "chunkCount": 42, // 已写入的分块数；未成功时为 0。
      "errorCode": null, // 失败时的稳定错误码；否则为 null。
      "createdAt": "2026-09-10T05:30:00.000Z", // 上传时间。
      "updatedAt": "2026-09-10T05:32:00.000Z" // 最近一次状态变更时间。
    }
  ],
  "page": 1, // 当前页码，1 基准。
  "size": 20, // 生效的每页条数。
  "totalElements": 1, // 满足筛选条件的总行数。
  "totalPages": 1, // 派生页数。
  "hasNext": false // 是否存在下一页。
}
```

| Field path | Type/format | Required/null/default | Validation/enum/precision | Meaning and source | Frontend use |
| --- | --- | --- | --- | --- | --- |
| `items[].status` | String enum | 必填 | 五值状态机 | 处理进度 | 决定是否轮询或展示重试按钮 |
| `items[].errorCode` | String | 失败时为稳定码，否则 `null` | 不含供应商细节 | 失败原因 | 展示与重试决策 |

[L1670-1689]
##### Error responses

| Condition | HTTP status | Business code | Response shape | Retryable | Frontend handling |
| --- | --- | --- | --- | --- | --- |
| 参数越界或枚举非法 | 400 | `KNOWLEDGE_VALIDATION_ERROR` | 既有错误体 + `fieldErrors` | 修正后 | 重置筛选 |
| API Key 缺失或错误 | 401 | `RESEARCH_UNAUTHORIZED` | 既有错误体 | 修正凭据后 | 提示配置 key |
| 知识库不存在或已删除 | 404 | `KNOWLEDGE_BASE_NOT_FOUND` | 既有错误体 | 否 | 返回列表 |
| 未预期失败 | 500 | `KNOWLEDGE_INTERNAL_ERROR` | 既有错误体 | 携带 traceId 上报 | 通用错误提示 |

```jsonc
{
  "code": "KNOWLEDGE_VALIDATION_ERROR", // 稳定机器码。
  "message": "validation failed", // 安全摘要。
  "traceId": "4e9d6938", // 关联标识。
  "timestamp": "2026-09-10T05:35:00.000Z", // 服务端 UTC Instant。
  "fieldErrors": { // 字段名 -> 消息列表。
    "status": ["unsupported status value"] // 非法枚举值。
  }
}
```

[L1691-1700]
##### Interface logic for frontend and consumers

1. 调用方必须携带有效 API Key。
2. 校验顺序：知识库存在且未删除 -> 分页与筛选约束。
3. 只返回 `knowledge_base_id` 匹配且 `is_deleted = false` 的行；`status` 与 `keyword` 为可选筛选。
4. 排序固定为 `created_at DESC, id DESC`；`totalElements` 与 `items` 在同一查询快照内。
5. 无写入、无缓存、无外部调用。
6. 状态可能在两次轮询之间被后台推进；前端应以最近一次响应为准，不合并历史状态。
7. 前端在存在 `PENDING` 或 `PROCESSING` 条目时应启用轮询（建议 2 秒起并以指数退避到 30 秒上限），全部进入终态后停止轮询。
8. 失败条目应展示 `errorCode` 对应的可读文案与"重新处理"入口，而不是展示原始异常文本。

[L1640-1640]
HTTP `200 OK`；排序 `created_at DESC, id DESC`。

[L1718-1718]
- 兼容：筛选参数、排序规则与分页语义在本版冻结；后续新增筛选参数必须是可选的，且不得改变 `created_at DESC, id DESC` 的排序与 1 基准分页。

[L1722-1722]
- 边界回归：知识库不存在时返回 404 而不是空数组，该行为必须由 `TEST-015` 固定，避免调用方把"库不存在"误判为"还没有文档"。

[L1724-1724]
#### 9.2.8 API-008 — 文档详情

[L1726-1758]
##### Necessity and interaction-cost decision

| Concern | Decision |
| --- | --- |
| Change classification | `New` |
| Independent consumer goal | 轮询单个文档直到终态；这是上传后唯一的进度观察入口，也是失败诊断入口 |
| Parameter ownership and derivation | 标识来自路径；全部字段由服务端派生 |
| Direct/no-new-interface alternative | 让列表承担轮询。不足：轮询一个文档却拉整页数据，且 `content` 长度等诊断信息不属于列表 |
| Caller use of result | 判断终态、展示失败原因、决定是否触发重新处理 |
| Round trips and failure points | 每次轮询一次 RTT |
| Verdict | `Add`，覆盖 `REQ-003`, `REQ-005` |

##### API style and CQRS semantics

| Concern | Decision/evidence |
| --- | --- |
| Protocol style | `REST Query` |
| CQRS role | 只读查询 |
| Resource/task semantics | `GET` 读取单个文档表示 |
| Read/write and side effects | 只读 |
| Consistency and idempotency | 读已提交；状态可能被后台并发推进 |
| Why this style | 单资源读取；轮询是 `202` 语义的自然配套 |

##### Identity and purpose

| Concern | Definition |
| --- | --- |
| Purpose/owner/consumer | 读取单个文档的状态与诊断信息；由 adapter 拥有 |
| Protocol and endpoint | `HTTP GET /api/v1/knowledge-documents/{documentId}` |
| Content type/version | `application/json`；v1 |
| Auth/permission/tenant | `X-Research-Api-Key`；无租户 |
| Timeout/retry/rate limit | 轮询由调用方控制；服务端不限制轮询频率但记录指标 |
| Idempotency/concurrency | 只读且幂等 |

[L1760-1765]
##### Request parameters

| Name | Location | Type/format | Required/null | Default | Validation/range/enum | Meaning | Example | Source |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| `X-Research-Api-Key` | Header | String | 必填 | 无 | 常量时间比较 | 服务凭据 | `demo-key` | 既有过滤器 |
| `documentId` | Path | String | 必填、非空 | 无 | `[A-Za-z0-9._-]{1,64}` | 文档标识 | `doc-01J5K9` | 上游接口 |

[L1767-1795]
##### Success response

HTTP `200 OK`。响应体为 `API-006` 的文档表示加上诊断字段。

```jsonc
{
  "documentId": "doc-01J5K9", // 文档标识。
  "knowledgeBaseId": "kb-01J5K9", // 所属知识库标识。
  "displayName": "report.pdf", // 展示名。
  "fileName": "report.pdf", // 原始文件名。
  "mimeType": "application/pdf", // 内容类型；未知时为 null。
  "sizeBytes": 182734, // 文件字节数。
  "status": "FAILED", // 处理状态。
  "chunkCount": 0, // 已写入的分块数；失败时为 0。
  "errorCode": "KNOWLEDGE_EMBEDDING_FAILED", // 失败时的稳定错误码；否则为 null。
  "errorMessage": "embedding provider call failed", // 失败时的安全摘要；不含供应商原文；否则为 null。
  "attemptCount": 3, // 已消耗的投递尝试次数，用于判断是否接近耗尽。
  "contentChars": 128400, // 已持久化原文文本的字符数；用于确认文本确实已落库。
  "createdAt": "2026-09-10T05:30:00.000Z", // 上传时间。
  "updatedAt": "2026-09-10T05:41:00.000Z", // 最近一次状态变更时间。
  "traceId": "4e9d6938" // 关联标识。
}
```

| Field path | Type/format | Required/null/default | Validation/enum/precision | Meaning and source | Frontend use |
| --- | --- | --- | --- | --- | --- |
| `attemptCount` | int | 必填 | ≥ 0 | outbox 的尝试次数 | 判断是否接近死信 |
| `contentChars` | int | 必填 | ≥ 0 | `content` 的字符数，不返回文本本身 | 确认文本已落库而不传输内容 |
| `errorMessage` | String | 失败时非空，否则 `null` | 安全摘要，不含供应商报文 | 失败原因 | 展示；不可解析为分支依据 |

[L1797-1814]
##### Error responses

| Condition | HTTP status | Business code | Response shape | Retryable | Frontend handling |
| --- | --- | --- | --- | --- | --- |
| 标识格式非法 | 400 | `KNOWLEDGE_VALIDATION_ERROR` | 既有错误体 | 修正后 | 返回列表 |
| API Key 缺失或错误 | 401 | `RESEARCH_UNAUTHORIZED` | 既有错误体 | 修正凭据后 | 提示配置 key |
| 不存在或已删除 | 404 | `KNOWLEDGE_DOCUMENT_NOT_FOUND` | 既有错误体 | 否 | 返回列表 |
| 未预期失败 | 500 | `KNOWLEDGE_INTERNAL_ERROR` | 既有错误体 | 携带 traceId 上报 | 通用错误提示 |

```jsonc
{
  "code": "KNOWLEDGE_DOCUMENT_NOT_FOUND", // 稳定机器码；已删除与不存在共用。
  "message": "knowledge document not found", // 安全摘要。
  "traceId": "4e9d6938", // 关联标识。
  "timestamp": "2026-09-10T05:41:00.000Z", // 服务端 UTC Instant。
  "fieldErrors": {} // 本接口无字段错误。
}
```

[L1816-1825]
##### Interface logic for frontend and consumers

1. 调用方必须携带有效 API Key；标识来自路径。
2. 校验顺序：标识格式 -> 文档存在且未删除。
3. 查询单行；`contentChars` 由持久化文本的长度派生，响应**不返回文本本身**，避免把大字段放进轮询路径。
4. 无写入、无缓存、无外部调用。
5. 状态与 `attemptCount` 由后台投递更新；轮询方看到的是已提交状态。
6. 终态为 `SUCCEEDED`、`FAILED` 与 `DEAD`；前两者可停止轮询，`DEAD` 需要人工介入。
7. 前端应在终态到达时停止轮询并刷新列表；`FAILED` 展示可重试入口，`DEAD` 展示需要人工处理的提示。
8. `errorMessage` 仅供展示，前端不得据其文本分支；分支一律使用 `errorCode`。

[L1843-1843]
- 兼容：响应字段只增；`contentChars` 的语义（字符数而非字节数）在本版冻结。

[L1847-1847]
#### 9.2.9 API-009 — 重新处理文档

[L1849-1881]
##### Necessity and interaction-cost decision

| Concern | Decision |
| --- | --- |
| Change classification | `New` |
| Independent consumer goal | 管理员对失败或已死信的文档重新启动处理；组件不提供 replay API（`EVD-017`），因此该目标必须由本项目实现 |
| Parameter ownership and derivation | 标识来自路径；是否允许重新处理由服务端按当前状态判定 |
| Direct/no-new-interface alternative | 要求管理员删除并重新上传。不足：会丢失 `documentId`、重新传输文件、并可能因客户端已无原文件而失败 |
| Caller use of result | 得到新的状态并重新开始轮询 |
| Round trips and failure points | 一次 RTT；失败点为不存在与状态不允许 |
| Verdict | `Add`，覆盖 `REQ-003`, `REQ-006` |

##### API style and CQRS semantics

| Concern | Decision/evidence |
| --- | --- |
| Protocol style | `REST Command` |
| CQRS role | 任务型 Command：再次入队一条后台任务 |
| Resource/task semantics | `POST /api/v1/knowledge-documents/{documentId}/reingest` 是对文档的从属动作资源 |
| Read/write and side effects | 写一条 outbox 行并把文档状态重置为待处理；不重新上传、不重新解析 |
| Consistency and idempotency | 单条插入在一个本地事务内；每次调用产生一条新任务记录，非幂等 |
| Why this style | 重新处理是一次有副作用的业务动作，不是资源状态替换；用从属动作资源表达比 `PATCH status` 更诚实 |

##### Identity and purpose

| Concern | Definition |
| --- | --- |
| Purpose/owner/consumer | 从已持久化文本重新执行切块与嵌入；由 adapter 拥有 |
| Protocol and endpoint | `HTTP POST /api/v1/knowledge-documents/{documentId}/reingest` |
| Content type/version | 无请求体；响应 `application/json`；v1 |
| Auth/permission/tenant | `X-Research-Api-Key`；无租户 |
| Timeout/retry/rate limit | 无独立设置；重复调用会被状态检查拒绝 |
| Idempotency/concurrency | 非幂等；文档处于非终态时返回 409 |

[L1883-1889]
##### Request parameters

| Name | Location | Type/format | Required/null | Default | Validation/range/enum | Meaning | Example | Source |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| `X-Research-Api-Key` | Header | String | 必填 | 无 | 常量时间比较 | 服务凭据 | `demo-key` | 既有过滤器 |
| `documentId` | Path | String | 必填、非空 | 无 | `[A-Za-z0-9._-]{1,64}` | 文档标识 | `doc-01J5K9` | 上游接口 |
| Body | Body | — | 无 | 无 | 请求体必须为空 | 不适用 | 无 | 不适用 |

[L1891-1918]
##### Success response

HTTP `202 Accepted`，返回与 `API-008` 同形的文档表示，其中 `status` 回到 `PENDING`、`errorCode` 与 `errorMessage` 清空、`attemptCount` 重置为 0。

| Field path | Type/format | Required/null/default | Validation/enum/precision | Meaning and source | Frontend use |
| --- | --- | --- | --- | --- | --- |
| `status` | String enum | 必填 | 本接口固定返回 `PENDING` | 已重新入队 | 恢复轮询 |
| `attemptCount` | int | 必填 | 本接口固定返回 `0` | 新任务的尝试计数 | 展示 |

```jsonc
{
  "documentId": "doc-01J5K9", // 文档标识。
  "knowledgeBaseId": "kb-01J5K9", // 所属知识库标识。
  "displayName": "report.pdf", // 展示名。
  "fileName": "report.pdf", // 原始文件名。
  "mimeType": "application/pdf", // 内容类型。
  "sizeBytes": 182734, // 文件字节数。
  "status": "PENDING", // 已重新入队，等待后台领取。
  "chunkCount": 0, // 重新处理前的分块数已作废，重置为 0。
  "errorCode": null, // 已清空。
  "errorMessage": null, // 已清空。
  "attemptCount": 0, // 新任务的尝试计数。
  "contentChars": 128400, // 复用的已持久化文本字符数；证明未重新解析。
  "createdAt": "2026-09-10T05:30:00.000Z", // 原始上传时间，不变。
  "updatedAt": "2026-09-10T05:45:00.000Z", // 本次重新入队时间。
  "traceId": "4e9d6938" // 关联标识。
}
```

[L1920-1939]
##### Error responses

| Condition | HTTP status | Business code | Response shape | Retryable | Frontend handling |
| --- | --- | --- | --- | --- | --- |
| 请求体非空 | 400 | `KNOWLEDGE_VALIDATION_ERROR` | 既有错误体 | 修正后 | 移除请求体 |
| API Key 缺失或错误 | 401 | `RESEARCH_UNAUTHORIZED` | 既有错误体 | 修正凭据后 | 提示配置 key |
| 不存在或已删除 | 404 | `KNOWLEDGE_DOCUMENT_NOT_FOUND` | 既有错误体 | 否 | 返回列表 |
| 文档处于非终态 | 409 | `KNOWLEDGE_DOCUMENT_BUSY` | 既有错误体 | 等待后 | 提示正在处理中 |
| 原文文本缺失 | 409 | `KNOWLEDGE_CONTENT_MISSING` | 既有错误体 | 否 | 提示删除后重新上传 |
| 未预期失败 | 500 | `KNOWLEDGE_INTERNAL_ERROR` | 既有错误体 | 携带 traceId 上报 | 通用错误提示 |

```jsonc
{
  "code": "KNOWLEDGE_DOCUMENT_BUSY", // 稳定机器码；表示文档仍在处理中。
  "message": "knowledge document is still being processed", // 安全摘要。
  "traceId": "4e9d6938", // 关联标识。
  "timestamp": "2026-09-10T05:45:00.000Z", // 服务端 UTC Instant。
  "fieldErrors": {} // 本接口无字段错误。
}
```

[L1941-1950]
##### Interface logic for frontend and consumers

1. 调用方必须携带有效 API Key；请求体必须为空。
2. 校验顺序：标识格式 -> 文档存在且未删除 -> 当前状态为终态 -> 持久化文本非空。
3. 服务端**不重新调用抽取**：handler 会从 `knowledge_document.content` 直接重建 `ExtractedDocumentBO`，因此本接口不会重新上传、下载或解析原文件。
4. 单条事务内插入一条 `channel=rag-ingest` 的 outbox 记录，并把文档状态重置为 `PENDING`、清空 `errorCode`/`errorMessage`、重置尝试计数。
5. 已存在的分块不在此处删除；摄取流程会按 `documentId` 先删后写，因此重跑得到相同分块 id 集合。
6. 文档处于非终态时返回 409，避免同一文档出现两条并发活跃任务。
7. 前端在收到 `202` 后应重新开始轮询详情；重复点击应被禁用直至拿到响应。
8. `KNOWLEDGE_CONTENT_MISSING` 表示历史数据缺少文本（例如从更早版本升级），此时唯一恢复路径是删除并重新上传，前端应给出该指引。

[L1968-1968]
- 兼容：动作资源的路径与 `202` 语义在本版冻结。

[L1972-1972]
#### 9.2.10 API-010 — 删文档

[L1974-2006]
##### Necessity and interaction-cost decision

| Concern | Decision |
| --- | --- |
| Change classification | `New` |
| Independent consumer goal | 管理员移除一份文档并确保其内容不再被检索到 |
| Parameter ownership and derivation | 标识来自路径；级联范围由服务端派生 |
| Direct/no-new-interface alternative | 无；删除是基础管理动作 |
| Caller use of result | 从列表移除并刷新计数 |
| Round trips and failure points | 一次 RTT |
| Verdict | `Add`，覆盖 `REQ-003` |

##### API style and CQRS semantics

| Concern | Decision/evidence |
| --- | --- |
| Protocol style | `REST Command` |
| CQRS role | 删除型 Command |
| Resource/task semantics | `DELETE` 删除该文档及其从属数据 |
| Read/write and side effects | 删除本地文件、删除向量分块、软删文档行 |
| Consistency and idempotency | 数据库变更在一个本地事务内；已删除再删返回 404 |
| Why this style | 标准资源删除语义 |

##### Identity and purpose

| Concern | Definition |
| --- | --- |
| Purpose/owner/consumer | 删除一份文档及其文件与向量；由 adapter 拥有 |
| Protocol and endpoint | `HTTP DELETE /api/v1/knowledge-documents/{documentId}` |
| Content type/version | 无请求体；成功无响应体 |
| Auth/permission/tenant | `X-Research-Api-Key`；无租户 |
| Timeout/retry/rate limit | 无独立设置 |
| Idempotency/concurrency | 已删除再删返回 404；处理中的文档允许删除并同时使其任务失效 |

[L2008-2014]
##### Request parameters

| Name | Location | Type/format | Required/null | Default | Validation/range/enum | Meaning | Example | Source |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| `X-Research-Api-Key` | Header | String | 必填 | 无 | 常量时间比较 | 服务凭据 | `demo-key` | 既有过滤器 |
| `documentId` | Path | String | 必填、非空 | 无 | `[A-Za-z0-9._-]{1,64}` | 文档标识 | `doc-01J5K9` | 上游接口 |
| Body | Body | — | 无 | 无 | 请求体必须为空 | 不适用 | 无 | 不适用 |

[L2016-2022]
##### Success response

HTTP `204 No Content`，无响应体。

| Field path | Type/format | Required/null/default | Validation/enum/precision | Meaning and source | Frontend use |
| --- | --- | --- | --- | --- | --- |
| Body | — | 无 | — | `204` 无内容 | 前端据状态码判定成功 |

[L2024-2041]
##### Error responses

| Condition | HTTP status | Business code | Response shape | Retryable | Frontend handling |
| --- | --- | --- | --- | --- | --- |
| 标识格式非法 | 400 | `KNOWLEDGE_VALIDATION_ERROR` | 既有错误体 | 修正后 | 返回列表 |
| API Key 缺失或错误 | 401 | `RESEARCH_UNAUTHORIZED` | 既有错误体 | 修正凭据后 | 提示配置 key |
| 不存在或已删除 | 404 | `KNOWLEDGE_DOCUMENT_NOT_FOUND` | 既有错误体 | 否 | 从本地列表移除 |
| 级联删除失败 | 500 | `KNOWLEDGE_INTERNAL_ERROR` | 既有错误体 | 携带 traceId 上报 | 通用错误提示；状态未变 |

```jsonc
{
  "code": "KNOWLEDGE_DOCUMENT_NOT_FOUND", // 稳定机器码。
  "message": "knowledge document not found", // 安全摘要。
  "traceId": "4e9d6938", // 关联标识。
  "timestamp": "2026-09-10T05:50:00.000Z", // 服务端 UTC Instant。
  "fieldErrors": {} // 本接口无字段错误。
}
```

[L2043-2052]
##### Interface logic for frontend and consumers

1. 调用方必须携带有效 API Key；请求体必须为空。
2. 校验顺序：标识格式 -> 文档存在且未删除。
3. 事务内先删除该文档在向量表中的全部分块（按 `documentId` 过滤），再软删文档行；本地文件删除在同一路径中执行，删除失败时以补偿记录处理。
4. 删除后该文档的分块不再可能被检索命中，这是本接口的核心保证。
5. 若该文档仍有活跃的 outbox 任务，任务执行时发现记录不存在将视为成功，不产生失败重试。
6. 重复删除返回 404；并发删除只有一个成功。
7. 前端必须要求显式确认后再提交；成功后刷新列表与知识库详情的 `documentCount`。
8. 删除不可撤销；已删除文档的文件与向量在成功响应后不可恢复。

[L2048-2048]
4. 删除后该文档的分块不再可能被检索命中，这是本接口的核心保证。

[L2050-2050]
6. 重复删除返回 404；并发删除只有一个成功。

[L2070-2070]
- 兼容：路径、`204` 状态码与级联范围在本版冻结；后续不得把删除改成异步（那会迫使调用方轮询一个不存在的状态资源）。

[L2074-2074]
- 并发回归：并发删除同一文档时只有一个返回 `204`，另一个返回 `404`；该行为由 `TEST-016` 固定。

[L2076-2076]
#### 9.2.11 API-011 — 检索调试

[L2078-2110]
##### Necessity and interaction-cost decision

| Concern | Decision |
| --- | --- |
| Change classification | `New` |
| Independent consumer goal | 管理员判断一次查询的召回片段、分位与分数，用于分辨"检索没召回到"与"模型没答好"；这是独立于问答的展示目标，且不产生模型计费 |
| Parameter ownership and derivation | 查询文本与条数由调用方拥有；强制过滤条件与逻辑模型由知识库派生，不接受覆盖 |
| Direct/no-new-interface alternative | 只用 `API-012` 问答。不足：问答会调用模型并计费，且只暴露被答案引用的片段，无法看到低分片段与分数分布 |
| Caller use of result | 展示片段与分数用于人工判断 |
| Round trips and failure points | 一次 RTT；失败点为参数越界与知识库不存在 |
| Verdict | `Add`，覆盖 `REQ-011` |

##### API style and CQRS semantics

| Concern | Decision/evidence |
| --- | --- |
| Protocol style | `REST Query` |
| CQRS role | 只读查询，拥有零业务写入与零模型调用 |
| Resource/task semantics | `POST` 承载结构化查询条件；无状态变更 |
| Read/write and side effects | 只读向量表；无审计以外的副作用 |
| Consistency and idempotency | 读已提交；相同请求在数据未变时结果一致 |
| Why this style | 查询条件含数组与数值，放在请求体比查询串更清晰；`POST` 用于非幂等的疑虑不成立，因为本接口无副作用 |

##### Identity and purpose

| Concern | Definition |
| --- | --- |
| Purpose/owner/consumer | 检索知识库内的相关分块并返回分数；由 adapter 拥有 |
| Protocol and endpoint | `HTTP POST /api/v1/knowledge-bases/{knowledgeBaseId}/retrieve` |
| Content type/version | 请求与响应 `application/json`；v1 |
| Auth/permission/tenant | `X-Research-Api-Key`；无租户 |
| Timeout/retry/rate limit | 无独立设置；服务端不重试 |
| Idempotency/concurrency | 只读且幂等 |

[L2112-2128]
##### Request parameters

| Name | Location | Type/format | Required/null | Default | Validation/range/enum | Meaning | Example | Source |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| `X-Research-Api-Key` | Header | String | 必填 | 无 | 常量时间比较 | 服务凭据 | `demo-key` | 既有过滤器 |
| `knowledgeBaseId` | Path | String | 必填、非空 | 无 | `[A-Za-z0-9._-]{1,64}` | 目标知识库 | `kb-01J5K9` | 上游接口 |
| `query` | Body | String | 必填、非空 | 无 | trim 后 1-2000 | 查询文本 | `如何配置超时` | 管理员 |
| `topK` | Body | int | 可选 | `rag.retrieval.default-top-k`（8） | 1 到 `rag.retrieval.max-top-k` | 返回条数上限 | `8` | 管理员 |
| `similarityThreshold` | Body | number | 可选 | `0.0` | 0.0-1.0 | 相似度下限 | `0.7` | 管理员 |

```jsonc
{
  "query": "如何配置超时", // 必填。查询文本，trim 后 1-2000 字符。
  "topK": 8, // 可选。返回条数上限，缺省取组件配置的默认值；越界返回 400。
  "similarityThreshold": 0.7 // 可选。相似度下限，缺省 0.0 表示接受全部；越界返回 400。
}
```

[L2130-2155]
##### Success response

HTTP `200 OK`。按相似度降序；无命中返回空数组。

```jsonc
{
  "items": [ // 必填。命中片段；无命中时为空数组。
    {
      "documentId": "doc-01J5K9", // 片段所属文档标识，可用于跳转详情。
      "chunkIndex": 7, // 文档内分块序号，从 0 开始。
      "score": 0.83, // 相似度分数；底层可能返回 null，此时该字段为 null。
      "content": "超时通过 max-duration 配置，默认 PT5M……", // 命中片段文本，用于人工判断召回质量。
      "displayName": "report.pdf" // 片段所属文档的展示名，避免调用方二次查询。
    }
  ],
  "query": "如何配置超时", // 回显规范化后的查询文本。
  "embeddingModel": "openai-small", // 本次检索使用的逻辑模型名，由知识库派生。
  "traceId": "4e9d6938" // 关联标识。
}
```

| Field path | Type/format | Required/null/default | Validation/enum/precision | Meaning and source | Frontend use |
| --- | --- | --- | --- | --- | --- |
| `items[].score` | number | 可空 | 由底层向量库返回 | 相似度 | 展示与排序依据 |
| `items[].content` | String | 必填、非空 | 片段文本 | 召回内容 | 人工判断 |
| `embeddingModel` | String | 必填、非空 | 知识库冻结的逻辑名 | 说明本次检索用的是哪个模型 | 展示 |

[L2157-2175]
##### Error responses

| Condition | HTTP status | Business code | Response shape | Retryable | Frontend handling |
| --- | --- | --- | --- | --- | --- |
| 查询文本空或过长、`topK`/阈值越界 | 400 | `KNOWLEDGE_VALIDATION_ERROR` | 既有错误体 + `fieldErrors` | 修正后 | 标红字段 |
| API Key 缺失或错误 | 401 | `RESEARCH_UNAUTHORIZED` | 既有错误体 | 修正凭据后 | 提示配置 key |
| 知识库不存在或已删除 | 404 | `KNOWLEDGE_BASE_NOT_FOUND` | 既有错误体 | 否 | 返回列表 |
| 向量库不可用 | 503 | `KNOWLEDGE_DEPENDENCY_UNAVAILABLE` | 既有错误体 | 是 | 稍后重试 |
| 未预期失败 | 500 | `KNOWLEDGE_INTERNAL_ERROR` | 既有错误体 | 携带 traceId 上报 | 通用错误提示 |

```jsonc
{
  "code": "KNOWLEDGE_DEPENDENCY_UNAVAILABLE", // 稳定机器码；表示向量库或检索依赖不可用。
  "message": "knowledge retrieval dependency is unavailable", // 安全摘要；不含连接串或供应商细节。
  "traceId": "4e9d6938", // 关联标识。
  "timestamp": "2026-09-10T05:55:00.000Z", // 服务端 UTC Instant。
  "fieldErrors": {} // 本接口无字段错误。
}
```

[L2177-2186]
##### Interface logic for frontend and consumers

1. 调用方必须携带有效 API Key；标识来自路径。
2. 校验顺序：知识库存在且未删除 -> 字段约束 -> `topK` 与阈值范围。
3. 逻辑模型与集合过滤由服务端从知识库配置派生并强制注入；请求不得提供 `embeddingModel`、`collectionId` 或任何元数据过滤条件。
4. 调用 RAG 组件的检索服务；该调用只读向量表，不调用嵌入或对话模型（查询文本的向量化由组件的嵌入模型完成，属于检索的必要步骤但不产生对话计费）。
5. 无持久化写入、无缓存。
6. 无命中返回空数组而非 404；依赖不可用返回 503 而不是空结果，避免把故障误判为"没有相关内容"。
7. 前端应允许调整 `topK` 与阈值并对比结果，用于判断分块配置是否合理；本接口不提供分块预览（`DEC-007`）。
8. 展示 `score` 时应说明它是相似度而非概率，且不同模型之间不可比较。

[L2132-2132]
HTTP `200 OK`。按相似度降序；无命中返回空数组。

[L2205-2205]
- 兼容：请求与响应字段只增；`score` 的可空性属于稳定契约。

[L2209-2209]
#### 9.2.12 API-012 — 知识库问答

[L2211-2243]
##### Necessity and interaction-cost decision

| Concern | Decision |
| --- | --- |
| Change classification | `New` |
| Independent consumer goal | 调用方就知识库内容提问并获得带引用的增量回答；这是与研究域并列的独立业务目标 |
| Parameter ownership and derivation | 问题文本由调用方拥有；引用、模型与过滤条件由服务端派生 |
| Direct/no-new-interface alternative | 复用研究域的 `POST /api/v1/deep-research/runs`。不足：其事件语义是研究报告、流程是固定多智能体工作流、且会启动昂贵的多路研究，与"基于知识库回答一个问题"完全不是同一件事 |
| Caller use of result | 增量展示答案与引用；结果不被转发给另一个接口 |
| Round trips and failure points | 一次 SSE 连接；失败点为校验、容量饱和、检索失败与生成失败 |
| Verdict | `Add`，覆盖 `REQ-012`, `REQ-022` |

##### API style and CQRS semantics

| Concern | Decision/evidence |
| --- | --- |
| Protocol style | `REST Command` |
| CQRS role | 任务型 Command：启动一次有外部计费副作用的生成，并以流返回结果 |
| Resource/task semantics | `POST /api/v1/knowledge-bases/{knowledgeBaseId}/chat` 是对知识库的从属动作；无持久资源 |
| Read/write and side effects | 只读向量表；调用对话模型；无数据库写入 |
| Consistency and idempotency | 非幂等：每次请求新建一次生成并重新计费；无会话与恢复 |
| Why this style | 增量输出需要流式传输；仓库既有的 SSE 先例（`EVD-012`, `EVD-013` 所在模块）使 MVC `SseEmitter` 是直接可行的选择 |

##### Identity and purpose

| Concern | Definition |
| --- | --- |
| Purpose/owner/consumer | 基于知识库检索结果生成回答并以 SSE 增量返回；由 adapter 拥有 |
| Protocol and endpoint | `HTTP POST /api/v1/knowledge-bases/{knowledgeBaseId}/chat` |
| Content type/version | 请求 `application/json`；响应 `text/event-stream`；v1 |
| Auth/permission/tenant | `X-Research-Api-Key`；无租户 |
| Timeout/retry/rate limit | 独立并发上限（默认 4）；饱和返回 429 + `Retry-After: 5`；服务端不自动重试 |
| Idempotency/concurrency | 非幂等；并发请求各自独立，除容量外互不影响 |

[L2245-2260]
##### Request parameters

| Name | Location | Type/format | Required/null | Default | Validation/range/enum | Meaning | Example | Source |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| `X-Research-Api-Key` | Header | String | 必填 | 无 | 常量时间比较 | 服务凭据 | `demo-key` | 既有过滤器 |
| `Accept` | Header | String | 约定要求 | 无 | 必须允许 `text/event-stream` | 响应协商 | `text/event-stream` | 调用方 |
| `knowledgeBaseId` | Path | String | 必填、非空 | 无 | `[A-Za-z0-9._-]{1,64}` | 目标知识库 | `kb-01J5K9` | 上游接口 |
| `question` | Body | String | 必填、非空 | 无 | trim 后 1-2000；拒绝控制字符 | 用户问题 | `超时怎么配置？` | 调用方 |
| `topK` | Body | int | 可选 | `rag.retrieval.default-top-k`（8） | 1 到 `rag.retrieval.max-top-k` | 引用条数上限 | `8` | 调用方 |

```jsonc
{
  "question": "超时怎么配置？", // 必填。用户问题，trim 后 1-2000 字符；控制字符被拒绝。
  "topK": 8 // 可选。检索引用条数上限，缺省取组件默认值；越界返回 400。
}
```

[L2262-2315]
##### Success response

HTTP `200 OK`，`Content-Type: text/event-stream`。`id` 固定为 `<answerId>:<sequence>`，`sequence` 从 1 严格递增；每个 `data` 是单行 UTF-8 JSON。服务不实现 `Last-Event-ID` 恢复。

| Event | When | Required data | Terminal |
| --- | --- | --- | --- |
| `knowledge.started` | 检索完成、生成开始前 | answerId, sequence, type, retrievals[], occurredAt, traceId | No |
| `knowledge.progress` | 生成增量 | answerId, sequence, type, delta, occurredAt | No |
| `knowledge.completed` | 生成结束 | answerId, sequence, type, answer, retrievals[], occurredAt, traceId | Yes |
| `knowledge.failed` | 200 之后发生安全失败 | answerId, sequence, type, code, message, retryable, occurredAt, traceId | Yes |

```jsonc
{
  "answerId": "qa-01J5K9", // 服务端生成的本次问答标识，仅在流内有效。
  "sequence": 1, // 流内严格递增事件序号，从 1 开始。
  "type": "STARTED", // 事件类型；STARTED、PROGRESS、COMPLETED 或 FAILED。
  "retrievals": [ // STARTED 与 COMPLETED 携带；本次检索使用的引用。
    {
      "documentId": "doc-01J5K9", // 引用所属文档标识。
      "chunkIndex": 7, // 文档内分块序号。
      "displayName": "report.pdf", // 文档展示名，便于展示来源。
      "score": 0.83 // 相似度分数；可能为 null。
    }
  ],
  "occurredAt": "2026-09-10T06:00:00.000Z", // 事件产生时间，UTC Instant。
  "traceId": "4e9d6938" // 与响应头一致的关联标识。
}
```

```jsonc
{
  "answerId": "qa-01J5K9", // 与 started/progress 相同的问答标识。
  "sequence": 12, // 最后一个事件序号。
  "type": "COMPLETED", // 唯一成功终态。
  "answer": "超时通过 max-duration 配置……", // 基于检索引用生成的回答文本；不是事实保证。
  "retrievals": [ // 与 started 相同的引用集合，便于调用方只消费终态。
    {
      "documentId": "doc-01J5K9", // 引用所属文档标识。
      "chunkIndex": 7, // 文档内分块序号。
      "displayName": "report.pdf", // 文档展示名。
      "score": 0.83 // 相似度分数；可能为 null。
    }
  ],
  "occurredAt": "2026-09-10T06:01:12.000Z", // 完成时间。
  "traceId": "4e9d6938" // 关联标识。
}
```

| Field path | Type/format | Required/null/default | Validation/enum/precision | Meaning and source | Frontend use |
| --- | --- | --- | --- | --- | --- |
| `data.retrievals` | Array | `STARTED`/`COMPLETED` 必填，可为空数组 | 每条含 documentId、chunkIndex、displayName、score | 引用来源 | 展示出处 |
| `data.delta` | String | `PROGRESS` 必填 | 有界安全文本 | 增量回答 | 追加展示 |
| `data.answer` | String | `COMPLETED` 必填 | 长度由模型决定；按不可信内容渲染 | 最终回答 | 替换增量展示 |
| `data.code`/`message`/`retryable` | 标量 | `FAILED` 必填 | 稳定安全映射 | 失败原因 | 错误展示与重试决策 |

[L2317-2351]
##### Error responses

| Condition | HTTP status | Business code | Response shape | Retryable | Frontend handling |
| --- | --- | --- | --- | --- | --- |
| 问题空或过长、`topK` 越界 | 400 | `KNOWLEDGE_VALIDATION_ERROR` | 既有错误体 + `fieldErrors` | 修正后 | 标红字段 |
| API Key 缺失或错误 | 401 | `RESEARCH_UNAUTHORIZED` | 既有错误体 | 修正凭据后 | 提示配置 key |
| 知识库不存在或已删除 | 404 | `KNOWLEDGE_BASE_NOT_FOUND` | 既有错误体 | 否 | 返回列表 |
| `Accept` 不接受事件流 | 406 | `KNOWLEDGE_NOT_ACCEPTABLE` | 既有错误体 | 否 | 设置 `Accept` |
| 并发容量饱和 | 429 | `KNOWLEDGE_CAPACITY_EXHAUSTED` | 既有错误体 + `Retry-After: 5` | 是 | 按 `Retry-After` 重试 |
| 流建立前的依赖不可用 | 503 | `KNOWLEDGE_DEPENDENCY_UNAVAILABLE` | 既有错误体 | 是 | 稍后重试 |
| 流建立后的检索或生成失败 | 流内 `knowledge.failed` | 稳定失败码 | 事件 JSON | 由 `retryable` 字段决定 | 展示失败并允许新建请求 |
| 未预期失败 | 500 | `KNOWLEDGE_INTERNAL_ERROR` | 既有错误体 | 携带 traceId 上报 | 通用错误提示 |

```jsonc
{
  "code": "KNOWLEDGE_CAPACITY_EXHAUSTED", // 稳定机器码；表示本机并发问答已达上限。
  "message": "knowledge qa capacity is exhausted", // 安全摘要。
  "traceId": "4e9d6938", // 关联标识。
  "timestamp": "2026-09-10T06:05:00.000Z", // 服务端 UTC Instant。
  "fieldErrors": {} // 本接口无字段错误。
}
```

```jsonc
{
  "answerId": "qa-01J5K9", // 已建立流的问答标识。
  "sequence": 9, // 唯一终态事件的序号。
  "type": "FAILED", // 失败终态。
  "code": "KNOWLEDGE_DEPENDENCY_UNAVAILABLE", // 稳定失败码；不暴露供应商错误。
  "message": "knowledge qa dependency failed", // 安全摘要。
  "retryable": true, // 客户端可决定是否新建请求；服务端不自动重试。
  "occurredAt": "2026-09-10T06:05:30.000Z", // 失败时间。
  "traceId": "4e9d6938" // 关联标识。
}
```

[L2353-2362]
##### Interface logic for frontend and consumers

1. 调用方必须逐个消费 SSE 事件，不把 HTTP 200 当业务完成；必须设置 `Accept: text/event-stream`。
2. 校验顺序：知识库存在且未删除 -> 媒体协商 -> 字段约束 -> 容量许可获取。任一失败都在建立流之前以普通 HTTP 错误返回。
3. 取得许可后创建流，随即执行一次检索；引用集合写入 `knowledge.started`，因此即使后续生成失败，调用方也已知晓"本来会引用哪些片段"。
4. 生成阶段把模型增量映射为 `knowledge.progress`；完整回答在 `knowledge.completed` 中一次性给出，调用方应以它为准而不是拼接增量。
5. 无数据库写入、无会话、无缓存；许可与订阅在终态或断连时释放一次。
6. 容量饱和在建立流之前返回 429；流内失败返回一个 `knowledge.failed` 并结束，EOF 前没有终态事件视为未知失败。
7. 前端应禁用重复提交直至拿到首个事件，增量展示时按不可信内容渲染并禁用原始 HTML；收到终态后关闭连接并提示可新建请求重试。
8. 客户端断连会取消下游并释放许可；服务端不保证已产生的增量会被回放，重连等于一次新的计费请求。

[L2264-2264]
HTTP `200 OK`，`Content-Type: text/event-stream`。`id` 固定为 `<answerId>:<sequence>`，`sequence` 从 1 严格递增；每个 `data` 是单行 UTF-8 JSON。服务不实现 `Last-Event-ID` 恢复。

[L2360-2360]
6. 容量饱和在建立流之前返回 429；流内失败返回一个 `knowledge.failed` 并结束，EOF 前没有终态事件视为未知失败。

[L2362-2362]
8. 客户端断连会取消下游并释放许可；服务端不保证已产生的增量会被回放，重连等于一次新的计费请求。

[L2382-2382]
- 兼容：事件名、字段与终态语义在本版冻结；新增事件类型需要显式版本决策。

### 13.3 OBSERVATIONS（子代理观察，非 spec 原文）

以下两段是摘录子代理在阅读时标注的疑点，**不是 Spec B 的文本**；保留在此是因为它们直接决定 `§7` 各 Step 在歧义处的取值理由，以及最终 Spec 一致性审计的候选清单。

#### 13.3.1 针对 API-001 – API-006

- 六节的成功响应字段表都只列"非显然"字段（如 API-002 只列 3 行、API-003 只列 2 行），jsonc 中出现的 `code`、`name`、`description`、`chunkStrategy`、`chunkConfig`、`traceId` 等字段在字段表中从未定义类型/约束/来源。
- API-001 的请求参数表定义了 `X-Trace-Id` 头，API-002 至 API-006 的参数表均未列出该头，但多个响应体返回 `traceId`，且文档称其"与响应头一致"——响应头的定义在本段缺失。
- 除 API-001 的 401 行提到 `WWW-Authenticate: ApiKey`、API-006 的 429 行提到 `Retry-After` 外，本段没有任何响应头表；`Retry-After` 的单位（秒/HTTP date）与 `ETag`/条件请求完全未出现。
- `knowledgeBaseId` 的生成规则（示例 `kb-01J5K9`）未定义，而路径校验正则为 `[A-Za-z0-9._-]{1,64}`，实现者需自行推断前缀与单调性要求。
- 排序口径不一致：API-002 正文用字段名 `createdAt DESC, knowledgeBaseId DESC`，步骤 4 改用数据库列名 `created_at DESC, id DESC`，二者映射关系需实现者推断。
- API-002 步骤 2 要求校验 `embeddingModel` 已注册，但错误示例注释称 `fieldErrors` 只可能出现 `page` 或 `size`，`embeddingModel` 非法时的状态码与错误码未定义。
- API-002 用 `is_deleted = false`、API-003 用"资源存在且未删除"，而 API-001 定义状态枚举为 `ACTIVE`/`DELETED`，`is_deleted` 与 `status` 是否同一标志未说明。
- 多张错误表以"既有错误体"指代响应形状，但 `KNOWLEDGE_VALIDATION_ERROR`、`KNOWLEDGE_MODEL_NOT_REGISTERED`、`KNOWLEDGE_FILE_TOO_LARGE`、`KNOWLEDGE_CAPACITY_EXHAUSTED` 的具体错误体示例在本段均未给出。
- API-005 步骤 3 提到"outbox 投递"、步骤 5 提到"补偿记录"，但补偿记录的实体、字段、存储位置在本段没有任何定义。
- API-005 声明 DELETE"语义上幂等"，同时又规定重复删除返回 404，二者对幂等的定义存在冲突；API-004 声明幂等但"并发写以后提交者为准"。
- API-006 的 `errorCode` 规定"失败时必填"，但 202 响应示例恒为 `null`，且同步失败（400/413/429/500）走 HTTP 错误码——失败错误码在哪一层返回未明示。
- API-006 步骤 2 提到"文档数未达上限"并映射 429，但上限的具体数值来源、计数口径（含非终态文档与否）未定义。
- API-004 请求参数表未列 `code`，但正文与错误示例把 `code` 归入"不可修改字段"；前端无法从表中得知该拒绝清单的完整成员。
- API-003 步骤 4 提到"与文档列表接口在同一筛选条件下口径一致"，但文档列表接口的筛选条件不在本段（且未给出接口号）。
- API-001 的 `(tenant_scope, code)` 唯一约束引用了租户列，而同一接口的鉴权行明确"无租户"，两者关系需要实现者猜测。
- API-001 步骤 6 以 `API-004` 这种编号形式引用其他接口，而本文件章节标题使用 `9.2.4 API-004`，引用格式不统一易误读。

#### 13.3.2 针对 API-007 – API-012

- API-007 响应只有 `items[].status` 与 `items[].errorCode` 两行字段级说明，其余字段（`documentId`/`displayName`/`mimeType`/`sizeBytes`/`chunkCount`/`createdAt`/`updatedAt`/`page`/`size`/`totalElements`/`totalPages`/`hasNext`）的类型与约束只能从 jsonc 注释推断。
- API-011/API-012 请求字段表 Location 写 `Body`，但 jsonc 是顶层平铺对象；包装类型 `RetrieveKnowledgeRequest`（L2201）与 `AskKnowledgeBaseRequest`（L2378）是否存在、字段命名是否一致需实现方猜测。
- API-012 没有 `knowledge.progress` 的 jsonc 示例，`delta` 之外是否携带 `retrievals`/`traceId` 无契约。
- API-012 错误表中"流建立后的检索或生成失败"的 HTTP status 列不是数字而是"流内 `knowledge.failed`"，流内失败是否仍以 HTTP 200 结束未明确。
- `knowledge.failed` 的 `code` 取值集合与 `retryable` 判定规则未定义，仅有一个示例值 `KNOWLEDGE_DEPENDENCY_UNAVAILABLE`。
- 文档 `errorCode`（API-007/008/009）的稳定码集合未枚举，仅在示例中出现 `KNOWLEDGE_EMBEDDING_FAILED`。
- `rag.retrieval.max-top-k` 只给出来源名，未给出具体数值；`rag.retrieval.default-top-k` 也只以"（8）"形式出现。
- 429 的 `Retry-After: 5` 未说明单位是秒还是 HTTP-date；单位对客户端退避实现是关键。
- L2287 声称 `traceId` "与响应头一致"，但六个接口的任何请求/响应头表都没有定义 `traceId` 响应头。
- "既有错误体"在本节范围内没有字段表，`code`/`message`/`traceId`/`timestamp`/`fieldErrors` 的必填性只能从示例推断。
- 未定义 `WWW-Authenticate`（401）与 `Cache-Control`（SSE）头；401 是否附带 challenge 不可知。
- API-012 `Accept` 行的 Required 列写"约定要求"，缺失 `Accept` 时返回 406 还是 200 未明确。
- API-010 身份表说处理中文档允许删除并"同时使其任务失效"（L2006），而步骤 5（L2049）说任务执行时发现记录不存在视为成功；"失效"的具体机制未定义。
- API-009 步骤 5 称重跑得到相同分块 id 集合，但未定义分块 id 与 `chunkIndex` 的稳定性契约，而 API-011/012 又把 `chunkIndex` 作为引用字段暴露。
- `answerId`（`qa-` 前缀）、`documentId`（`doc-` 前缀）、`knowledgeBaseId`（`kb-` 前缀）的生成规则与路径正则 `[A-Za-z0-9._-]{1,64}` 的关系未说明。

### 13.4 摘录的边界

本附录覆盖 `§9.2` 的 12 个接口契约；Spec B 中与实施同样相关但未收录于此的部分（`§9.1` 的接口清单与编号规则、`§9.3` 的消费方/前端页面细节、`§10` 的类清单与状态表、`§13` 的 Manual Check 清单）仍以 Spec B 原文为准，`§7` 各 Step 的引用行号已直接指向 Spec B。
