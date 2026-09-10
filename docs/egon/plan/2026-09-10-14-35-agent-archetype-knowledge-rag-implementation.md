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
│       │          DocumentIngestStatusEnum,KnowledgeBaseStatusEnum,
│       │          KnowledgeChunkingStrategyEnum}.java                # CREATE
│       ├── gateway/KnowledgeVectorGateway.java                       # CREATE
│       ├── repository/{KnowledgeBaseRepository,KnowledgeDocumentRepository}.java  # CREATE
│       ├── service/KnowledgeIngestService.java                       # CREATE
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
          ignored-tables: knowledge_base,knowledge_document
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
- Commit paths: `...-infrastructure/src/test/java/.../infrastructure/knowledge/KnowledgeSchemaMigrationTest.java`; `...-infrastructure/src/main/resources/db/migration/V20260910_001__create_knowledge_schema.sql`; `...-infrastructure/src/main/resources/db/migration/V20260910_002__create_transactional_outbox_schema.sql`; `...-starter/src/main/resources/{application.yml,application-dev.yml,application-test.yml,application-prod.yml}`
- Commit: `feat(agent-archetype): create the knowledge schema and configuration`

在实施期修正：本 Step 同时补上 Step 1 尚未加入的 `verify.groovy` 迁移文件断言与 `BOOT-INF/lib` 运行时库断言中依赖迁移的部分；依赖存在性断言在 Step 2 加入。

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

#### File 2 — `CREATE .../domain/knowledge/model/{KnowledgeBaseBO,KnowledgeDocumentBO,KnowledgeChunkBO}.java`

- Purpose: 领域载体，含冻结配置与状态机入口。
- Symbols: 三个 record 与紧凑构造器。
- Repository evidence: `DeepResearchTaskBO` 的 record 写法。
- Dependencies and consumers: application 与 infrastructure。
- Why now: 全部下游的类型前提。
- Contract/signature changes: 新增公开载体；**不携带审计列**（属 PO）。
- Input/output and state mapping: 字段见 Spec B `§10.3`。
- Error and edge behavior: 空白标识、负计数与未知枚举抛 `KnowledgeApplicationException`。
- Standards impact: `MC-NAME-001`（`BO` 后缀）、`MC-MODEL-001`（record + 紧凑构造器）、`MC-SCOPE-001`。
- Literal rule enforcement: `Rule 1`、`Rule 3`、`Rule 10`（`Instant` 时间字段）、`Rule 11`。
- Implementation pseudocode:

```java
public record KnowledgeBaseBO(Long id, Long tenantId, String code, String name, String description,
                              String embeddingModel, KnowledgeChunkingStrategyEnum chunkStrategy,
                              RagChunkingConfigBO chunkConfig, KnowledgeBaseStatusEnum status,
                              Instant createdAt, Instant updatedAt) {
    public KnowledgeBaseBO { /* trim、非空与状态校验 */ }
}
```

- Verification contribution: `TEST-006`。
- After this file: 载体就绪。

#### File 3 — `CREATE .../domain/knowledge/model/{DocumentIngestStatusEnum,KnowledgeBaseStatusEnum,KnowledgeChunkingStrategyEnum}.java`

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
    Optional<KnowledgeBaseBO> findById(Long id);
    List<KnowledgeBaseBO> page(int offset, int size, String keyword, String embeddingModel);
    long count(String keyword, String embeddingModel);
    void updateNameAndDescription(Long id, String name, String description);
    void softDelete(Long id);
}
```

- Verification contribution: `TEST-007`。
- After this file: 仓储契约就绪。

#### File 6 — `CREATE .../domain/knowledge/service/KnowledgeIngestService.java` 与 `.../domain/knowledge/package-info.java`

- Purpose: 摄取契约与包文档。
- Symbols: `KnowledgeIngestService#ingestDocument(Long documentId)`。
- Repository evidence: `DeepResearchRunService` 的领域服务写法。
- Dependencies and consumers: 由 handler 消费。
- Why now: 与端口同批。
- Contract/signature changes: 新增领域服务契约。
- Input/output and state mapping: 文档标识 → 无返回。
- Error and edge behavior: 记录不可见时静默返回（幂等）。
- Standards impact: `MC-NAME-001`、`MC-SCOPE-001`。
- Literal rule enforcement: `Rule 1`、`Rule 11`。
- Implementation pseudocode:

```java
public interface KnowledgeIngestService { void ingestDocument(Long documentId); }
```

- Verification contribution: `TEST-009`。
- After this file: Step 4 全部文件就位。

- Validation working directory: `/Users/mario/SelfProject/Egon-COLA`
- Verification command: `./mvnw -B -ntp -f egon-cola-archetypes/source-projects/egon-cola-source-agent/pom.xml clean verify`
- Expected result: 退出码 0；`KnowledgeDomainTest` 通过；`AgentArchitectureTest` 的 domain 无框架断言仍通过。
- Failure returns to: File 2/3 的校验或 File 4/5 的包位置。
- Completion criteria: `REQ-001` 与 `REQ-006` 的领域部分有证据。
- Rollback: 回退 domain 的 knowledge 子树。
- Commit paths: `...-domain/src/test/java/.../domain/knowledge/KnowledgeDomainTest.java`; `.../domain/knowledge/model/{KnowledgeBaseBO,KnowledgeDocumentBO,KnowledgeChunkBO}.java`; `.../domain/knowledge/model/{DocumentIngestStatusEnum,KnowledgeBaseStatusEnum,KnowledgeChunkingStrategyEnum}.java`; `.../domain/knowledge/gateway/KnowledgeVectorGateway.java`; `.../domain/knowledge/repository/{KnowledgeBaseRepository,KnowledgeDocumentRepository}.java`; `.../domain/knowledge/service/KnowledgeIngestService.java`
- Commit: `feat(agent-archetype): add the knowledge domain model and ports`

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
- Commit paths: `...-infrastructure/src/test/java/.../infrastructure/knowledge/KnowledgeRepositoryTest.java`; `.../infrastructure/knowledge/repo/po/{KnowledgeBasePO,KnowledgeDocumentPO}.java`; `.../infrastructure/knowledge/repo/converter/{KnowledgeBasePOConverter,KnowledgeDocumentPOConverter}.java`; `.../infrastructure/knowledge/repo/dao/{KnowledgeBaseDAO,KnowledgeDocumentDAO}.java`; `.../infrastructure/knowledge/service/{KnowledgeBaseRepositoryImpl,KnowledgeDocumentRepositoryImpl}.java`
- Commit: `feat(agent-archetype): persist knowledge bases and documents`

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
                                         @NotNull KnowledgeChunkingStrategyEnum chunkStrategy,
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
                                         @NotNull KnowledgeChunkingStrategyEnum chunkStrategy,
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
                              String embeddingModel, KnowledgeChunkingStrategyEnum chunkStrategy,
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
