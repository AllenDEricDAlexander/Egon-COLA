# 天枢 Prompt/Skill 使用反馈、自动优化闭环与 AI Starter 实施计划

| Field | Value |
| --- | --- |
| Document | `2026-10-17-14-12-tianshu-ai-feedback-optimization-implementation.md` |
| Template Version | `5` |
| Status | `Ready` |
| Created | `2026-10-17 14:12 Asia/Shanghai` |
| Updated | `2026-10-17 15:04 Asia/Shanghai` |
| Owner | mario |
| Repository | Egon-COLA |
| Scope | `egon-cola-xingyuan/egon-cola-tianshu`（starter、admin、新 ai-starter、admin-web）与 `egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-tianshu-adapter`；玉衡只作被调用方 |
| Source Requirement | 用户 2026-10-17：Prompt/Skill 在线管理之上增加按使用结果自动优化（达阈值停止）、复杂 Skill 下发、自动上传更新、在线查看与专用下游 starter；同日四项决定（玉衡通道、策略 autoPublish、上行+下行、纯文本树落盘）；Spec 审核通过后要求写 Plan |
| Baseline Revision | `main@4ad87524a`；工作树仅含本 Spec/Plan 两个文档改动，无其他未提交代码 |
| Implements Spec | [天枢 Prompt/Skill 使用反馈、自动优化闭环与下游 AI Starter](../spec/2026-10-17-13-40-tianshu-ai-feedback-optimization-starter.md) |
| Spec Status | `Accepted` |
| Spec Revision | `2026-10-17 14:09 Asia/Shanghai`（用户审核通过后置为 Accepted）；基线 `main@4ad87524a` |
| Effective Specs | [主 Spec](../spec/2026-10-17-13-40-tianshu-ai-feedback-optimization-starter.md)（其 §1 关系元数据引用的四份前置/规范 Spec 已由 2026-09-23 Plan 实施，本 Plan 只实施主 Spec 的 REQ-001～REQ-014；前置文档作为规范引用列于 §2.3） |
| Depends On Plans | [天枢多租户 Prompt/Skill 实施计划](2026-09-23-06-16-tianshu-multitenant-ai-implementation.md) |
| Supersedes | `None` |
| Superseded By | `None` |
| Related Plans | `None` |

## 1. Summary

本计划实现主 Spec 的全部 14 条需求：在已交付的 Prompt/Skill 管理（前置 Plan Step 1–24 已提交）之上，增加使用反馈上报与持久化、玉衡 LLM-as-judge 评分、按资源策略的自动优化闭环（达阈值停止）、发布事件经 Outbox→Redis 投递、下游 Skill 工作区落盘/订阅刷新与创作目录上传、新模块 `egon-cola-tianshu-ai-starter`、管理前端“反馈与优化”页签。

依赖方向：base starter 共享契约 → RPC adapter 契约/客户端 → DDL-201 与持久化模型 → Repository/Mapper → 反馈服务与 RPC 上报 → 玉衡模型端口 → 评分作业 → 策略/运行/优化器 → 发布事件 → 作业装配与配置 → 管理 REST → 同步 RPC → ai-starter（上报、工作区、上传）→ 前端 → 文档与静态门禁。共 18 个 Step，每步一次语义提交。完成证据：各步 RED/GREEN 聚焦测试、模块 `mvn -pl ... test`、OpenAPI 契约断言、opt-in PostgreSQL/Outbox Failsafe IT、前端 Vitest/typecheck；本文只定义未来验证，不声称已运行。

## 2. Target Spec and Effective Design

### 2.1 Primary target

- Path: [2026-10-17-13-40-tianshu-ai-feedback-optimization-starter.md](../spec/2026-10-17-13-40-tianshu-ai-feedback-optimization-starter.md)
- Status: `Accepted`
- Revision: Updated `2026-10-17 14:09 Asia/Shanghai`；Baseline `main@4ad87524a`
- Approval evidence: 用户 2026-10-17 在本会话答复“Spec 审核通过，按该 Spec 写实施 Plan”；四项主要决定（DEC-001～004）由用户同日作出

### 2.2 Effective Spec set

| Role | Spec/link | Status/revision | Effective sections | Why included |
| --- | --- | --- | --- | --- |
| Primary | [主 Spec](../spec/2026-10-17-13-40-tianshu-ai-feedback-optimization-starter.md) | Accepted；2026-10-17 14:09 | §1–§20 全部 | 本计划目标 |
| Dependency | [原 Prompt/Skill Spec](../spec/2026-09-21-17-18-tianshu-prompt-skill-management.md) | Accepted | §9.0.1（wire/错误码）、§9.5（租户传输）、§11.2.12–§11.2.16（AI 五表）、§15（租户/Redis 隔离） | 新接口沿用其 wire、权限与隔离规则 |
| Dependency | [Java/CQE 修订稿](../spec/2026-09-22-17-16-tianshu-java-cqe-standards-amendment.md) | Accepted | §10（class/record 与校验）、§11.0（lifecycle/live 唯一键）、§15（Outbox 参数） | 建模与持久化合同 |
| Dependency | [Outbox 与状态机集成](../spec/2026-09-24-12-00-outbox-statemachine-cqe-integration.md) | Accepted | §7（投递语义） | EVENT-103 投递 |
| Dependency | [玉衡 LLM 网关设计](../spec/2026-09-21-16-58-yuheng-llm-knowledge-wiki-design.md) | Accepted | §9（API-001 Chat Completions 与 SERVICE token） | INTERNAL-103 协议 |

### 2.3 Superseded or excluded content

主 Spec Amends 原 Spec §3（解除“客户端订阅/定时同步/内容缓存”非目标，仅限 AI 发布事件与 Skill 工作区）、§7.3.2（starter 开关矩阵）、§9.1（RPC 新方法）、§12（编辑页新页签），以及修订稿 §5.3“AI CRUD 不发无消费者 Event”条款和 §9.1 事件清单。其余条款继续有效。原 Spec/修订稿中 Prompt 本地上传、二进制节点、自动合并等不在有效范围（主 Spec §3.2）。

### 2.4 Normative references already implemented

下列文档是主 Spec 的 `Depends On`/`Related Specs`，其需求已由 [2026-09-23 实施计划](2026-09-23-06-16-tianshu-multitenant-ai-implementation.md) 落地并提交（见 §6.1 基线）；本 Plan 复用其契约与约束而不重新实施，因此不把它们的 REQ 编号纳入 §10 覆盖范围：

- [原 Prompt/Skill 管理 Spec](../spec/2026-09-21-17-18-tianshu-prompt-skill-management.md)：既有 11 REST、2 RPC、五表与发布链路（本 Plan 的 REQ-001 复用边界）。
- [Java/CQE 标准修订稿](../spec/2026-09-22-17-16-tianshu-java-cqe-standards-amendment.md)：POJO/校验/转换/Bean/Outbox 规则（本 Plan 的 REQ-014 与 §4.7/§4.8 依据）。
- [Outbox 与状态机集成 Spec](../spec/2026-09-24-12-00-outbox-statemachine-cqe-integration.md)：`TransactionalOutbox`/`DeliveryHandler` 契约（Step 10 依据）。
- [玉衡 LLM 网关设计](../spec/2026-09-21-16-58-yuheng-llm-knowledge-wiki-design.md)：`/v1/chat/completions` 协议与 SERVICE token（Step 7 依据）。

## 3. Effective Requirements and Acceptance

| Requirement | Source Spec section | Effective statement | Observable acceptance | Implementation impact |
| --- | --- | --- | --- | --- |
| REQ-001 | 主 Spec §4/§19 | 既有增删改查、版本、发布、推广、只读 RPC 与编辑页面保持不变 | API-001～011、RPC-001/002 既有契约测试与 OpenAPI 断言不变；新增代码不改其 wire | Step 10（只追加入队）、Step 12/13（回归）、Step 18 |
| REQ-002 | 主 Spec §4/§9.2.7/§11.2.1 | 下游按次上报使用结果，`(tenant_id, usage_id)` 唯一落库，重复返回已接受 | 回执逐条状态；越权整批拒绝；队列满丢弃计数 | Step 1、2、3、4、5、6、11、14 |
| REQ-003 | 主 Spec §4/§9.2.12/§9.2.15 | 无显式分数样本由玉衡自动评分，最多 3 次失败 | `PENDING→DONE` 带 0..100 `judgeScore`；显式分优先 | Step 7、8、11 |
| REQ-004 | 主 Spec §4/§7.2.1/§9.2.13/§10.6 | 策略驱动自动优化；avg≥阈值进入 CONVERGED 停止 | 运行创建条件、状态机推进、autoPublish 分支 | Step 9、10、11、12 |
| REQ-005 | 主 Spec §4/§9.2.3/§9.2.5/§11.2.3 | 运行可追溯可控：基线、样本、哈希、产物、错误码；草稿脏跳过；连续失败 3 次暂停 | 运行行字段与终态；手动触发 202 与幂等 | Step 9、11、12 |
| REQ-006 | 主 Spec §4/§7.3.3 | ai-starter 原子落盘已发布 Skill 树 | `{root}/{resourceKey}/versions/{versionNo}` 与 `current`；无半包；拒绝路径穿越 | Step 15 |
| REQ-007 | 主 Spec §4/§9.2.8/§9.2.11 | 发布事件下行；订阅或对账刷新；钉住版本不切换 | EVENT-103 投递与 listener/对账 | Step 1、2、10、13、15 |
| REQ-008 | 主 Spec §4/§9.2.9/§9.2.10 | 创作目录变化上传为草稿并可发布；修订冲突停止 | RPC-005/006 与状态文件 | Step 1、2、13、16 |
| REQ-009 | 主 Spec §4/§9.2.1–§9.2.6/§12 | 编辑页“反馈与优化”页签 | 策略、评估、运行、反馈可见；权限分支 | Step 12、17 |
| REQ-010 | 主 Spec §4/§8 | 专用下游 starter，三组开关默认关闭 | 新模块可引入；关闭时无 Bean/无网络 | Step 14、15、16 |
| REQ-011 | 主 Spec §4/§15.1 | 多租户隔离 | 新表 tenant_id；凭据绑定；作业逐租户；Topic 含 tenant | Step 3、5、6、10、11、15 |
| REQ-012 | 主 Spec §4/§5.1 | Spec 轮只产出文档 | 该轮已完成；本 Plan 同样只产出文档 | 本计划不含代码 |
| REQ-013 | 主 Spec §4/§9.2.15/§15.4 | 玉衡通道，Admin 不加依赖 | `A/pom.xml` 无新坐标；RestClient + SERVICE token | Step 7 |
| REQ-014 | 主 Spec §4/§6.2 | 2026-09-22 Java/CQE 规范 | class POJO、BaseConverter、具名 Bean、Outbox 事件、键矩阵 | 每个 Java Step；Step 18 静态门禁 |

## 4. Implementation Strategy and Dependency Order

### 4.1 Ordered strategy

1. 先在 base starter 建立下游与 Admin 共用的 Port/DTO/Event/Redis 键（Step 1），因为 adapter、Admin、ai-starter 都依赖它编译。
2. RPC adapter 追加 proto 方法、操作权限、客户端实现（Step 2），使 Admin Provider 与 ai-starter 有契约可编译。
3. 数据库先行：DDL-201、manifest、表登记、PO/BO/枚举/转换器（Step 3），再 Mapper/XML/Repository（Step 4）。
4. 反馈链路自底向上（Step 5 服务与授权 → Step 6 RPC-003 Provider），先于任何作业。
5. 模型端口（Step 7）是评分（Step 8）与优化（Step 9）的共同前置。
6. 优化器依赖既有发布链；发布事件（Step 10）单独成步，因它改动既有 `DdcAiResourceServiceImpl` 且被运行执行与下游订阅共同依赖。
7. 作业装配与三 profile 配置键（Step 11）在服务全部就绪后启用；PG/Outbox IT 同步加入。
8. REST（Step 12）与同步 RPC（Step 13）在服务层稳定后接线。
9. ai-starter 三步（14 上报、15 工作区、16 上传）依赖 Step 1/2 契约，且 Step 15 依赖 Step 13 的 `GetVersionHead`。
10. 前端（Step 17）依赖 Step 12 的 wire；文档/静态门禁/最终回归（Step 18）收尾。

### 4.2 Test-first strategy

每个行为先写聚焦 RED 测试：Step 1 `DdcRedisKeysTest#aiTopic` 因方法不存在编译失败→这是编译前置，真正 RED 在断言键格式；Step 2 `DdcAiRpcContractTest` 断言 4 个新方法描述符缺失；Step 3 `DdcAiOptimizationSchemaScriptTest` 断言 SQL/manifest 不含三表；Step 4 Repository 合同测试断言命名查询缺失；Step 5/6 服务与 Provider 测试断言 DUPLICATE/权限矩阵；Step 7 `MockRestServiceServer` 断言错误分类；Step 8/9 Strategy 与 Service 单测断言状态机/跳过码；Step 10 断言发布后 `TransactionalOutbox.enqueue` 被调用；Step 11 作业单测与 IT；Step 12 MockMvc/OAS；Step 13 Provider 单测；Step 14–16 `ApplicationContextRunner`/临时目录；Step 17 Vitest；Step 18 静态门禁。编译前置文件（接口、DTO）在 RED 测试之前创建时在文件块中注明“仅建立编译前置”。

### 4.3 Sequential and parallel boundaries

| Step | Depends on | May run in parallel with | Must not overlap with | Reason |
| --- | --- | --- | --- | --- |
| Step 1 | None | None | base starter 文件 | 所有模块的编译前置 |
| Step 2 | Step 1 | Step 3 | adapter 文件 | 需要 Step 1 的 SDK DTO |
| Step 3 | Step 1 | Step 2 | Admin model/db 文件 | 需要 Step 1 的 `DdcAiOutcomeEnum` |
| Step 4 | Step 3 | None | Admin repository/dao | 需要 PO/BO |
| Step 5 | Step 4 | None | Admin service/ai、config | 需要 Repository |
| Step 6 | Step 2, Step 5 | Step 7 | Admin rpc/security | 需要 proto 与服务 |
| Step 7 | Step 5（配置类） | Step 6 | Admin service/ai/model | 需要 `DdcAiOptimizationProperties` |
| Step 8 | Step 7 | None | Admin job/judge | 需要模型端口 |
| Step 9 | Step 8 | None | Admin service/ai 优化文件 | 需要模板加载器与策略注册 |
| Step 10 | Step 9 | None | `DdcAiResourceServiceImpl`、notification | 修改既有发布链 |
| Step 11 | Step 10 | None | job、yml、pom profile | 作业装配与 IT |
| Step 12 | Step 9 | Step 13 | controller、advice、web config | REST 只依赖服务 |
| Step 13 | Step 6, Step 10 | Step 12 | rpc provider、service 接口 | 新 RPC 方法 |
| Step 14 | Step 2 | Step 12 | ai-starter 模块 | 新模块 |
| Step 15 | Step 13, Step 14 | Step 16 | ai-starter workspace、base starter Redis 条件 | 需要 `GetVersionHead` 与 autoconfig |
| Step 16 | Step 14 | Step 15 | ai-starter authoring | 独立目录 |
| Step 17 | Step 12 | Step 14–16 | admin-web | 前端 |
| Step 18 | 全部 | None | docs、contract test | 收尾 |

### 4.4 Commit boundaries

每 Step 一次路径限定提交；提交信息 `feat(tianshu): ai-<主题>`、文档 `docs(tianshu): ...`，沿既有 `feat(tianshu): step-24` 风格。Step 3 的 SQL+manifest+PO/BO/Converter 同一提交，因 manifest 哈希必须与 SQL 同步且 PO 单独提交无法编译验证。工作树中本 Spec/Plan 文档已单独提交（`docs(tianshu)`），不与代码提交混合。

### 4.5 Spec Simplicity and Implementation-necessity Audit

| Spec element | Spec necessity verdict/section | Current repository evidence | Direct/reuse alternative | Interaction/implementation cost | Plan decision |
| --- | --- | --- | --- | --- | --- |
| `ddc_ai_usage_feedback` + RPC-003 | 主 Spec §7.0 Add | 无同类表；`ddc_operation_log` 无版本/分数语义 | 复用操作日志（否） | 一表、一 RPC、清理作业 | Implement |
| `ddc_ai_optimization_policy` | §7.0 Add | `ddc_ai_resource` 无策略列 | 资源加列（否：权限/CAS 分离） | 一表 | Implement |
| `ddc_ai_optimization_run` | §7.0 Add | 无 | 仅日志（否） | 一表、部分唯一索引 | Implement |
| `DdcAiModelPort` + `YuhengLlmModelAdapter` | §7.0 Add；DEC-001 | Admin 已依赖 `IdpServiceOAuth2Client`、`spring-boot-starter-web`（RestClient） | 直接 RestClient（否：两处调用） | 一接口一实现 | Implement |
| `DdcAiSystemActor` 授权分支 | §7.0 Add；DEC-006 | `DdcAiScopeValidator#requireNamespacePermission` 仅用户/RPC 两分支 | 复制写链（否） | 一 ThreadLocal | Implement |
| EVENT-103 + `aiTopic` + handler | §7.0 Add；DEC-008 | `DdcNotificationPublisher`/`DdcRegistryEventDeliveryHandler` 模式可复用 | 仅轮询（保留为兜底） | 一事件、一 handler | Implement |
| RPC-004 `GetVersionHead` | §7.0 Add | `GetSkill` 返回全文 | 用 GetSkill 对账（否：1MiB） | 一方法 + Service 新增只读方法 | Implement（见 PLAN-CLAR-002） |
| RPC-005/006 | §7.0 Add | REST 不接受 HMAC | 让下游调 REST（否） | 两方法、两操作 | Implement |
| `egon-cola-tianshu-ai-starter` | §7.0 Add；REQ-010 | `http-registration-starter` 为组装 starter 先例 | 塞进 base starter（否） | 一模块 | Implement |
| 6 个 REST | §9.2.x 必要性表 Add | 控制器族存在 | 并入既有 API（否） | 6 operation | Implement |
| Prompt 本地上传、二进制、自动合并、评分 MQ、Factory | §7.0 Remove | — | — | — | 不实现 |

无预取转发接口：策略 `revision` 仅用于 CAS，运行 `Location` 指向状态资源，上报/上传的 tenant/scope 由凭据派生。审计未发现需返回 Spec 的项。

### 4.6 Change-unit Dependency Matrix

| Change unit | Requirements | Proof/RED point | Compile/runtime prerequisites | Produces | Consumers/unblocks | Owning Step |
| --- | --- | --- | --- | --- | --- | --- |
| 共享 Port/DTO/Event/aiTopic | REQ-002/007/008/010 | `DdcRedisKeysTest#aiTopic`、`DdcAiSdkContractTest` | 无 | `DdcAiFeedbackClient`、`DdcAiSyncClient`、SDK DTO、`DdcAiPublicationEvent`、`aiTopic` | Step 2/5/10/14 | Step 1 |
| RPC 契约与客户端 | REQ-002/007/008 | `DdcAiRpcContractTest` | Step 1 | 4 方法、3 操作、2 客户端 | Step 6/13/14 | Step 2 |
| DDL-201 与模型 | REQ-002/004/005/011 | `DdcAiOptimizationSchemaScriptTest` | Step 1 枚举 | 三表 SQL、PO/BO/枚举/Converter | Step 4 | Step 3 |
| Repository/Mapper | REQ-002/004/005 | `DdcAiFeedbackOptimizationRepositoryContractTest` | Step 3 | 三 Repository | Step 5/9 | Step 4 |
| 反馈服务与授权 | REQ-002/003/011/014 | `DdcAiFeedbackServiceTest`、`DdcAiScopeValidatorAuthorityTest` | Step 4 | `DdcAiFeedbackService`、`DdcAiSystemActor`、配置类 | Step 6/8/11/12 | Step 5 |
| RPC-003 Provider | REQ-002/011 | `DdcAiResourceRpcProviderTest#reportUsage` | Step 2/5 | Provider 方法、scope 提取 | Step 14 | Step 6 |
| 模型端口 | REQ-013 | `YuhengLlmModelAdapterTest` | Step 5 配置类 | `DdcAiModelPort` | Step 8/9 | Step 7 |
| 评分 | REQ-003 | `DdcAiJudgeStrategyTest`、`DdcAiJudgeScannerTest` | Step 7 | JOB-001 | Step 11 | Step 8 |
| 策略/运行/优化器 | REQ-004/005 | `DdcAiOptimizationServiceTest` 等 | Step 8 | `DdcAiOptimizationService` | Step 10/11/12 | Step 9 |
| EVENT-103 | REQ-001/007 | `DdcAiPublicationOutboxTest` | Step 9 | Notifier/Handler/路由 | Step 11/15 | Step 10 |
| 作业与配置 | REQ-002/003/004/005/011/014 | 作业单测、PG IT | Step 10 | 三作业、键矩阵 | Step 18 | Step 11 |
| REST | REQ-004/005/009 | `DdcAiOptimizationControllerTest`、OAS | Step 9 | 6 operation | Step 17 | Step 12 |
| 同步 RPC | REQ-007/008 | Provider 单测 | Step 6/10 | RPC-004/005/006 | Step 15/16 | Step 13 |
| ai-starter 上报 | REQ-002/010 | `DdcAiStarterAutoConfigurationTest`、Reporter 测试 | Step 2 | 模块、属性、Reporter | Step 15/16 | Step 14 |
| 工作区 | REQ-006/007/010/011 | Materializer/Listener/Reconcile 测试 | Step 13/14 | 落盘、订阅、对账 | — | Step 15 |
| 上传 | REQ-008/010 | Watcher/TreeReader 测试 | Step 14 | 监听上传 | — | Step 16 |
| 前端 | REQ-009 | Vitest | Step 12 | 两面板、页签 | — | Step 17 |
| 文档/门禁 | REQ-001/014 | `DdcAiOptimizationStandardsTest` | 全部 | 手册、README、静态门禁 | — | Step 18 |

### 4.7 Java, Spring, and Egon-COLA Implementation Standards

| Concern | Current repository evidence | Effective Spec decision | Planned implementation consequence | Owning Steps/checks |
| --- | --- | --- | --- | --- |
| Architecture profile | Admin 传统三层 `admin.controller/service/service.ai.impl/repository/repository.impl/dao/model.*`（`DdcAiResourceServiceImpl`、`DdcAiPublicationRepositoryImpl`）；starter/adapter 为技术模块 | 主 Spec §6.1 保留 | 新类进入同一包族；ai-starter 为组装 starter（`http-registration-starter` 先例） | 全部 Step；MC-ARCH-001 |
| Reuse/capability | `RestClient`（spring-web）、`IdpServiceOAuth2Client`、`TransactionalOutbox`、`DdcTenantContextExecutor`、`EgonColaRepository`、`BaseConverter`、`ValidationUtils`、`DdcAiContentStrategy` | §6.1 账本 | 无新 Maven 坐标；ai-starter 依赖 tianshu-starter + rpc-tianshu-adapter | Step 7/10/14；MC-REUSE-001/MC-DEP-001 |
| Naming/model/validation/conversion | 既有 `*PO/*BO/*Command/*Query/*Result/*Converter`，`@SuperBuilder` PO，`@JsonCreator` Command，`DdcAiEnumConverter` | §6.2 Rule 1–3、§10 | 新类型同后缀；三 BaseConverter + 四 BaseForwardConverter；`@ValidDurationRange` 自定义约束 | Step 3/5/9/13；MC-NAME-001/MC-VALID-001/MC-MODEL-001/MC-CONVERT-001 |
| Bean/logging/util/JSON/time/config | `@Service("ddcAiResourceService")`+`@RequiredArgsConstructor`+`@Qualifier`；`T/lombok.config`；`jacksonObjectMapper`；`ddcPublishClock`；三 profile yml | §6.2 Rule 4–7/10 | 具名 Bean、`@Slf4j`、`Instant/Duration`、`@ConfigurationProperties` 两处、三 profile 同键 | Step 5/7/11/14；MC-LOG-001/MC-BEAN-001/MC-UTIL-001/MC-JSON-001/MC-TIME-001/MC-CONFIG-001 |
| Business variation/pattern | `ddcAiContentStrategyRegistry` Map 注册 Strategy | §13.1 Strategy×2、Adapter、表驱动 State | `DdcAiOptimizerStrategy`（Prompt/Skill）、`DdcAiJudgeStrategy`、`DdcAiModelPort`、`DdcAiOptimizationStateTransition` | Step 7/8/9；MC-PATTERN-001 |

#### Capability reuse ledger

| Need | Candidates inspected | Exact evidence | Fit/gap | Decision | Added dependency/custom code | Owning Step/check |
| --- | --- | --- | --- | --- | --- | --- |
| 模型 HTTP 调用 | Spring `RestClient`、`JdkClientHttpRequestFactory`；Spring AI（agent-flow） | `yuheng-starter/.../GatewayReportHttpClient.java`；`A/pom.xml` 含 spring-boot-starter-web、无 spring-ai | RestClient 足够 | 复用 | None | Step 7；MC-REUSE-001 |
| SERVICE token | `IdpServiceOAuth2Client.authorize(IdpServiceTokenRequest)` | `SJ/service/lifecycle/DdcInstanceService.java:177`；`A/pom.xml` 含 shoubing-starter | 直接复用 | 复用 | None | Step 7 |
| 可靠事件 | Outbox starter、`DdcNotificationPublisher` | `AJ/service/notification/*` | 直接复用模式 | 复用 | None | Step 10 |
| 作业逐租户 | `DdcTenantAdminProperties`、`DdcTenantRegistry`、`DdcTenantContextExecutor` | `AJ/service/lease/DdcLeaseExpiryScanner.java` | 直接复用 | 复用 | None | Step 8/11 |
| 行锁/认领 | 命名 XML `FOR UPDATE SKIP LOCKED` | `DdcAiPublicationMapper.xml` 命名查询写法 | 可表达 | 复用 | None | Step 4 |
| 哈希 | `MessageDigest`、`HexFormat`、`DdcAiContentStrategy.sha256Hex` | `AJ/service/ai/impl/DdcAiContentStrategy.java` | 直接复用 | 复用 | None | Step 9 |
| 文件原子落盘/监听 | `java.nio.file.Files`、`WatchService` | JDK | 足够 | JDK | None | Step 15/16 |
| 校验/转换 | Jakarta、MapStruct 1.6.3、`BaseConverter`/`BaseForwardConverter`、`ValidationUtils` | `T/pom.xml` 处理器；`DdcAiPublicationConverter` | 直接复用 | 复用 | None | Step 3/5/9 |
| JSON | `jacksonObjectMapper` Bean、`DdcAiWebConfiguration` 严格 mapper | `AJ/config/DdcAiWebConfiguration.java` | 复用 | 复用 | None | Step 7/12 |
| 指标 | `simpleMeterRegistry` | `DdcAiResourceServiceImpl#meterRegistry` | 复用 | 复用 | None | Step 5/9 |

### 4.8 User-mandated Java Rule Implementation Matrix

| Literal rule | Spec source | Repository evidence | Exact files and order | Pseudocode obligations | Validation gate | Steps | Status/blocker |
| --- | --- | --- | --- | --- | --- | --- | --- |
| Rule 1 | 主 Spec §6.2/§8/§10 | 既有 `DdcAi*PO/BO/Command/Query/Result/Converter` | §5 全部 Java 文件按后缀命名；无 Data/Info/Param/Bean | 伪代码使用最终类型名 | `DdcAiOptimizationStandardsTest` 反射扫描 + 编译 | Step 1–16, 18 | PASS |
| Rule 2 | 主 Spec §6.2/§10.3.1 | `DdcAiResourceService` `@Validated` 分组；`DdcAiScopeValidator` | 每个 Service 接口 `@Validated`+组；Repository `Insert/Update/Query/Delete` 组；Provider `ValidationUtils`；模型输出 Save+Publish 组；自定义 `@ValidDurationRange` | 每文件列出组与级联 | 各 Step 负例测试 | Step 4–9, 12, 13 | PASS |
| Rule 3 | 主 Spec §6.2/§10 | `DdcAiPublicationBO`/`PO` 注解集 | PO `@SuperBuilder`+`callSuper=true`；BO/Command/Result `@Builder`；两 record | 三 BaseConverter、四 BaseForwardConverter 明示 | 转换器测试、编译 | Step 3, 5, 9, 10, 13 | PASS |
| Rule 4 | 主 Spec §6.2 | `@Service("ddcAiResourceService")`、`T/lombok.config` | 业务类 `@Slf4j`、具名 Bean、`@RequiredArgsConstructor(onConstructor_ = @Autowired)`、`@Qualifier` | 每个 Bean 伪代码写注解 | `DdcAiOptimizationStandardsTest` + `ApplicationContextRunner` | Step 5–11, 14–16 | PASS |
| Rule 5 | 主 Spec §6.2 | `commons-lang3` 在 classpath | 仅 JDK/commons-lang3 | 不新增 Utils 类 | import 扫描 | Step 7, 9, 15, 16, 18 | PASS |
| Rule 6 | 主 Spec §9.0.1/§10 | `@JsonFormat(STRING)`、`@JsonInclude(ALWAYS)`、严格 mapper | Result/DTO/Event 注解；枚举 `@JsonValue`/`@EnumValue` | 每个 wire 类型列注解 | OAS 契约测试、序列化测试 | Step 1, 3, 5, 9, 10, 12 | PASS |
| Rule 7 | 主 Spec §15.4 | 三 profile yml | 三 profile 同键矩阵 | `DdcAiOptimizationPropertiesTest` 解析三文件 | TEST-030 | Step 5, 11 | PASS |
| Rule 9 | 主 Spec §13.1 | `ddcAiContentStrategyRegistry` | Strategy×2、Judge Strategy、Adapter、State 表 | 注册 Map 与转移表 | Strategy/状态机测试 | Step 7, 8, 9 | PASS |
| Rule 10 | 主 Spec §6.3 | `EgonModel` Instant；`ddcPublishClock` | `Instant`/`Duration` 全部；秒整数列 ↔ Duration 转换 | §4.9 | TEST-004/008/014/017 | Step 3, 5, 9, 11 | PASS |
| Rule 11 | 主 Spec §6.1/§8 | 传统三层；MP starter；Outbox | 每个文件在既有包族；DDL 经 manifest；lifecycle/live 键；事件经 Outbox | 每 Step `Literal Rules` 含 Rule 11 | 架构门禁 | Every Step | PASS |

### 4.9 Time semantics and conversion contract

Time contract: Required

| Time ID | Fields/symbols | Semantics | Java/runtime type | Storage policy | Wire policy | Business zone source | Display zone source | Boundary / DST rules | Precision / Clock | Compatibility | Verification |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| TIME-001 | `ddc_ai_usage_feedback.occurred_at`、`DdcAiUsageReportDTO.occurredAt`、`DdcAiUsageReportCommand.occurredAt` | Occurred instant | `Instant` | UTC instant | UTC Z | Not applicable: 只做瞬时窗口比较 | 前端浏览器本地显示并标注 UTC 原值 | RFC 3339 带 offset 输入归一为 Instant；`[now-7d, now+5m]` 之外拒绝（REJECTED CLOCK_SKEW）；无 DST 处理 | `timestamptz(6)`；下游 `Clock.systemUTC()`，服务端 `ddcPublishClock` | 新字段 | Step 5, Step 11；TEST-004 |
| TIME-002 | `judged_at`、`judge_lease_until`、`last_evaluated_at`、`converged_at`、`started_at`、`finished_at`、`lease_until`、`window_start`、`window_end` 与对应 Result 字段 | Occurred instant | `Instant` | UTC instant | UTC Z | Not applicable: 服务端瞬时记录 | 前端浏览器本地显示 | 全部由 `ddcPublishClock` 生成；`lease_until = now + run-lease` | 微秒；单一 Clock 注入 | 新字段 | Step 8, Step 9, Step 11；TEST-008, TEST-014 |
| TIME-003 | `evaluation_window_seconds`、`cooldown_seconds`、策略 REST `evaluationWindow`/`cooldown`、配置 `scan-interval`/`run-lease`/`model.read-timeout`/`feedback.retention` | Duration | `Duration` | Duration | Duration | Not applicable: 经过时间 | Not applicable: ISO-8601 字符串原样显示 | REST ISO-8601 `PT24H` 形式，`@ValidDurationRange` 校验范围；列存秒整数（Converter 显式映射） | 秒；`now.minus(window)` | 新字段 | Step 3, Step 9, Step 12；TEST-008, TEST-017 |

## 5. Change File Tree

路径别名（§7 文件标题与 Commit paths 使用完整路径，本节为可读性使用别名）：`S=egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-starter`，`SJ=S/src/main/java/top/egon/cola/component/tianshu`，`ST=S/src/test/java/top/egon/cola/component/tianshu`，`R=egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-tianshu-adapter`，`RJ=R/src/main/java/top/egon/cola/component/rpc/tianshu`，`RT=R/src/test/java/top/egon/cola/component/rpc/tianshu`，`A=egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin`，`AJ=A/src/main/java/top/egon/cola/component/tianshu/admin`，`AT=A/src/test/java/top/egon/cola/component/tianshu/admin`，`AR=A/src/main/resources`，`N=egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-ai-starter`，`NJ=N/src/main/java/top/egon/cola/component/tianshu/ai`，`NT=N/src/test/java/top/egon/cola/component/tianshu/ai`，`W=egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin-web/src`。

```text
S/                                                         # Step 1、15
├── SJ/redis/DdcRedisKeys.java                             MODIFY aiTopic
├── SJ/autoconfigure/DdcRedisAutoConfiguration.java        MODIFY 条件表达式（Step 15）
├── SJ/api/client/{DdcAiFeedbackClient,DdcAiSyncClient}.java                              CREATE
├── SJ/model/ai/{DdcAiOutcomeEnum,DdcAiUsageReportStatusEnum,DdcAiUsageReportDTO,DdcAiUsageReportBatchDTO,
│       DdcAiUsageReportReceiptDTO,DdcAiVersionHeadDTO,DdcAiSkillDraftUploadDTO,DdcAiSkillPublishDTO,
│       DdcAiRevisionDTO,DdcAiPublicationReceiptDTO}.java                                 CREATE
├── SJ/model/event/DdcAiPublicationEvent.java              CREATE
├── ST/redis/DdcRedisKeysTest.java                         MODIFY
├── ST/model/ai/DdcAiSdkContractTest.java                  CREATE
└── ST/autoconfigure/DdcRedisAutoConfigurationTest.java    MODIFY（Step 15）
R/                                                         # Step 2
├── src/main/proto/tianshu_ai_resource.proto               MODIFY
├── RJ/security/{DdcRpcOperation,DdcRpcOperationResolver}.java                            MODIFY
├── RJ/contract/DdcAiResourceRpc.java                      MODIFY
├── RJ/client/ai/DdcAiProtoConverter.java                  MODIFY
├── RJ/client/ai/{RpcDdcAiFeedbackClient,RpcDdcAiSyncClient}.java                         CREATE
├── RJ/client/DdcRpcClientFactory.java                     MODIFY
├── RJ/autoconfigure/DdcRpcAutoConfiguration.java          MODIFY
└── RT/{DdcAiRpcContractTest,DdcRpcAutoConfigurationTest}.java（实际路径见 §7）             MODIFY
A/                                                         # Step 3–13、18
├── AR/db/egon-mp/V20261017_001__tianshu_ai_feedback_optimization.sql                     CREATE
├── AR/db/egon-mp/repository-manifest.json                 MODIFY
├── AR/egon-tianshu-persistence.yml                        MODIFY
├── AR/{application,application-local,application-test}.yml                               MODIFY（Step 5）
├── AR/mybatis/mapper/tianshu/{DdcAiUsageFeedbackMapper,DdcAiOptimizationPolicyMapper,DdcAiOptimizationRunMapper}.xml  CREATE
├── AR/tianshu/ai/prompts/{judge-system,optimize-prompt-system,optimize-skill-system}.txt  CREATE
├── A/pom.xml                                              MODIFY（Failsafe includes，Step 11）
├── AJ/model/enums/{DdcAiOutcomePersistenceEnum,DdcAiJudgeStatePersistenceEnum,DdcAiPolicyStatePersistenceEnum,
│       DdcAiRunStatePersistenceEnum,DdcAiRunTriggerPersistenceEnum,DdcAiRunErrorCodeEnum}.java     CREATE
├── AJ/model/po/{DdcAiUsageFeedbackPO,DdcAiOptimizationPolicyPO,DdcAiOptimizationRunPO}.java        CREATE
├── AJ/model/bo/{DdcAiUsageFeedbackBO,DdcAiOptimizationPolicyBO,DdcAiOptimizationRunBO,DdcAiScoreAggregateBO}.java  CREATE
├── AJ/model/convertor/{DdcAiUsageFeedbackConverter,DdcAiOptimizationPolicyConverter,DdcAiOptimizationRunConverter,
│       DdcAiUsageFeedbackResultConverter,DdcAiOptimizationPolicyResultConverter,DdcAiOptimizationRunResultConverter,
│       DdcAiPublicationEventConverter,DdcAiSyncCommandConverter}.java                     CREATE
├── AJ/model/convertor/DdcAiEnumConverter.java             MODIFY
├── AJ/model/command/{DdcAiUsageReportCommand,DdcAiUsageReportBatchCommand,DdcAiOptimizationPolicyCommand,
│       DdcAiOptimizationTriggerCommand,DdcAiSkillDraftUploadCommand,DdcAiSkillPublishCommand}.java  CREATE
├── AJ/model/query/{DdcAiUsageFeedbackPageQuery,DdcAiOptimizationRunPageQuery,DdcAiOptimizationRunQuery}.java  CREATE
├── AJ/model/vo/{DdcAiUsageReportReceiptResult,DdcAiUsageFeedbackResult,DdcAiOptimizationPolicyResult,
│       DdcAiOptimizationRunResult,DdcAiVersionHeadResult,DdcAiRevisionResult}.java       CREATE
├── AJ/model/dto/{DdcAiJudgeVerdictDTO,DdcAiOptimizationEvaluationDTO,DdcAiModelRequestDTO,DdcAiModelResultDTO}.java  CREATE
├── AJ/dao/{DdcAiUsageFeedbackMapper,DdcAiOptimizationPolicyMapper,DdcAiOptimizationRunMapper}.java  CREATE
├── AJ/repository/{DdcAiUsageFeedbackRepository,DdcAiOptimizationPolicyRepository,DdcAiOptimizationRunRepository}.java  CREATE
├── AJ/repository/impl/{DdcAiUsageFeedbackRepositoryImpl,DdcAiOptimizationPolicyRepositoryImpl,DdcAiOptimizationRunRepositoryImpl}.java  CREATE
├── AJ/service/ai/{DdcAiFeedbackService,DdcAiOptimizationService,DdcAiModelPort}.java     CREATE
├── AJ/service/ai/DdcAiResourceService.java                MODIFY（getVersionHead，Step 13）
├── AJ/service/ai/impl/{DdcAiFeedbackServiceImpl,DdcAiOptimizationServiceImpl,DdcAiOptimizerStrategy,
│       DdcPromptOptimizerStrategy,DdcSkillOptimizerStrategy,DdcAiJudgeStrategy,DdcDefaultAiJudgeStrategy,
│       DdcAiOptimizationStateTransition,DdcAiPromptTemplateLoader}.java                  CREATE
├── AJ/service/ai/impl/DdcAiResourceServiceImpl.java       MODIFY（Step 10 事件入队、Step 13 getVersionHead）
├── AJ/service/ai/model/{YuhengLlmModelAdapter,DdcAiModelException}.java                  CREATE
├── AJ/service/ai/job/{DdcAiJudgeScanner,DdcAiOptimizationScanner,DdcAiFeedbackRetentionScanner}.java  CREATE
├── AJ/service/ai/notification/{DdcAiPublicationNotifier,DdcAiPublicationDeliveryHandler}.java  CREATE
├── AJ/service/ai/validation/{DdcAiSystemActor,ValidDurationRange,DurationRangeValidator}.java  CREATE
├── AJ/service/ai/validation/DdcAiScopeValidator.java      MODIFY
├── AJ/config/{DdcAiOptimizationProperties,DdcAiOptimizationConfiguration}.java            CREATE
├── AJ/config/DdcOutboxConfiguration.java                  MODIFY
├── AJ/controller/ai/{DdcAiResourceController,DdcAiExceptionAdvice}.java                  MODIFY
├── AJ/config/DdcAiWebConfiguration.java                   MODIFY
├── AJ/rpc/provider/DdcAiResourceRpcProvider.java          MODIFY
├── AJ/security/rpc/{DdcRpcScopeExtractor,DdcRpcServerInterceptor}.java                    MODIFY
└── AT/...（各 Step 列出的测试与 IT）                       CREATE/MODIFY
N/                                                         # Step 14–16
├── pom.xml；src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports  CREATE
├── NJ/autoconfigure/DdcAiStarterAutoConfiguration.java    CREATE
├── NJ/properties/{DdcAiStarterProperties,DdcAiFeedbackProperties,DdcAiWorkspaceProperties,DdcAiAuthoringProperties,DdcAiWatchedResourceDTO}.java  CREATE
├── NJ/feedback/{DdcAiUsageReporter,DefaultDdcAiUsageReporter}.java                       CREATE
├── NJ/workspace/{DdcAiSkillWorkspace,DdcAiSkillWorkspaceService,DdcAiSkillWorkspaceStateDTO,DdcAiSkillMaterializer,DdcAiPublicationListener,DdcAiWorkspaceReconcileScheduler}.java  CREATE
├── NJ/authoring/{DdcAiSkillAuthoringWatcher,DdcAiSkillTreeReader,DdcAiAuthoringStateDTO}.java  CREATE
└── NT/...（测试）                                           CREATE
egon-cola-xingyuan/egon-cola-tianshu/pom.xml               MODIFY modules
egon-cola-xingyuan/pom.xml                                 MODIFY dependencyManagement
W/api/{types,ai}.ts                                        MODIFY
W/pages/{AiOptimizationPanel,AiFeedbackPanel}.tsx + .test.tsx   CREATE
W/pages/AiResourceEditorPage.tsx + AiResourceEditorPage.test.tsx  MODIFY
egon-cola-xingyuan/egon-cola-tianshu/docs/tianshu-ai-migration-runbook.md、README.md、README.zh-CN.md  MODIFY
```

| Operation | Path | Current evidence/symbol | Final symbols/state | Responsibility | Step | Requirements | Validation owner |
| --- | --- | --- | --- | --- | --- | --- | --- |
| MODIFY | `SJ/redis/DdcRedisKeys.java` | `topic(...)`、`buildKey/configTag` | `aiTopic(tenantId,bizCode,env,appCode)` | AI 发布 Topic 键 | Step 1 | REQ-007 | `DdcRedisKeysTest` |
| CREATE | `SJ/api/client/DdcAiFeedbackClient.java`、`DdcAiSyncClient.java` | 不存在；同包 `DdcPromptClient` | 两个 Port | SDK 端口 | Step 1 | REQ-002/007/008/010 | `DdcAiSdkContractTest` |
| CREATE | `SJ/model/ai/*`（10 个 DTO/枚举）、`SJ/model/event/DdcAiPublicationEvent.java` | 不存在；同包 `DdcPromptResult` 写法 | SDK 模型与事件 | 传输载体 | Step 1 | REQ-002/007/008 | `DdcAiSdkContractTest` |
| MODIFY | `R/src/main/proto/tianshu_ai_resource.proto`、`RJ/contract/DdcAiResourceRpc.java`、`RJ/security/DdcRpcOperation.java`、`DdcRpcOperationResolver.java` | 2 方法、`AI_READ(19)` | 6 方法、`AI_REPORT(20)/AI_WRITE(21)/AI_PUBLISH(22)` | RPC 契约与权限 | Step 2 | REQ-002/007/008 | `DdcAiRpcContractTest` |
| MODIFY/CREATE | `RJ/client/ai/DdcAiProtoConverter.java`、`RpcDdcAiFeedbackClient.java`、`RpcDdcAiSyncClient.java`、`RJ/client/DdcRpcClientFactory.java`、`RJ/autoconfigure/DdcRpcAutoConfiguration.java` | `RpcDdcPromptClient`、`promptClient()` | 两个 Port 的实现与条件装配 | 传输 | Step 2 | REQ-010 | `DdcRpcAutoConfigurationTest` |
| CREATE/MODIFY | `AR/db/egon-mp/V20261017_001__tianshu_ai_feedback_optimization.sql`、`repository-manifest.json`、`AR/egon-tianshu-persistence.yml` | 两条已登记版本；16+2 表登记 | DDL-201、三表登记 | 受管 DDL | Step 3 | REQ-002/004/005/011 | `DdcAiOptimizationSchemaScriptTest`、`DdcTianshuDdlIT` |
| CREATE | `AJ/model/enums/*`（6）、`AJ/model/po/*`（3）、`AJ/model/bo/*`（4）、`AJ/model/convertor/{三 BaseConverter}`、MODIFY `DdcAiEnumConverter` | 既有 `DdcAiPublicationPO/BO/Converter` | 持久化模型 | 模型 | Step 3 | REQ-014 | `DdcAiOptimizationConverterTest` |
| CREATE | `AJ/dao/*`（3）、`AR/mybatis/mapper/tianshu/*.xml`（3）、`AJ/repository/*`（3）、`AJ/repository/impl/*`（3） | `DdcAiPublicationMapper(.xml)`、`DdcAiPublicationRepositoryImpl` | 命名查询与反腐层 | 持久化访问 | Step 4 | REQ-002/004/005 | `DdcAiFeedbackOptimizationRepositoryContractTest` |
| CREATE/MODIFY | `AJ/service/ai/validation/DdcAiSystemActor.java`、`DdcAiScopeValidator.java`、`DdcAiFeedbackService.java`、`impl/DdcAiFeedbackServiceImpl.java`、命令/查询/Result/DTO/转换器（反馈）、`AJ/config/DdcAiOptimizationProperties.java`、三 yml | `DdcAiScopeValidator` 两分支 | 反馈服务、系统主体、配置 | 业务 | Step 5 | REQ-002/003/011/013/014 | `DdcAiFeedbackServiceTest`、`DdcAiScopeValidatorAuthorityTest`、`DdcAiOptimizationPropertiesTest` |
| MODIFY/CREATE | `AJ/security/rpc/DdcRpcScopeExtractor.java`、`DdcRpcServerInterceptor.java`、`AJ/rpc/provider/DdcAiResourceRpcProvider.java`、`AJ/model/convertor/DdcAiSyncCommandConverter.java` | `aiReadScope`、`AI_READ` 分支 | RPC-003 入口 | RPC | Step 6 | REQ-002/011 | `DdcAiResourceRpcProviderTest`、`DdcRpcServerInterceptorTest` |
| CREATE | `AJ/model/dto/DdcAiModelRequestDTO.java`、`DdcAiModelResultDTO.java`、`AJ/service/ai/DdcAiModelPort.java`、`AJ/service/ai/model/{YuhengLlmModelAdapter,DdcAiModelException}.java`、`AJ/config/DdcAiOptimizationConfiguration.java` | 无；`GatewayReportHttpClient` 写法 | 模型端口 | 外部调用 | Step 7 | REQ-013 | `YuhengLlmModelAdapterTest` |
| CREATE | `AR/tianshu/ai/prompts/judge-system.txt`、`AJ/service/ai/impl/{DdcAiPromptTemplateLoader,DdcAiJudgeStrategy,DdcDefaultAiJudgeStrategy}.java`、`AJ/service/ai/job/DdcAiJudgeScanner.java` | `DdcLeaseExpiryScanner` 模式 | 评分 | 作业 | Step 8 | REQ-003 | `DdcAiJudgeStrategyTest`、`DdcAiJudgeScannerTest` |
| CREATE/MODIFY | 优化命令/查询/Result/DTO/转换器、`ValidDurationRange`、`DurationRangeValidator`、`DdcAiOptimizationStateTransition`、`DdcAiOptimizerStrategy`+两实现、`DdcAiOptimizationService`+Impl、两模板、`DdcAiOptimizationConfiguration`（注册表） | `ddcAiContentStrategyRegistry` 模式 | 策略/运行/优化 | 业务 | Step 9 | REQ-004/005 | `DdcAiOptimizationServiceTest` 等 |
| CREATE/MODIFY | `AJ/model/convertor/DdcAiPublicationEventConverter.java`、`AJ/config/DdcOutboxConfiguration.java`、`AJ/service/ai/notification/{DdcAiPublicationNotifier,DdcAiPublicationDeliveryHandler}.java`、`AJ/service/ai/impl/DdcAiResourceServiceImpl.java` | `DdcNotificationPublisher`、`DdcRegistryEventDeliveryHandler` | EVENT-103 | 事件 | Step 10 | REQ-001/007 | `DdcAiPublicationOutboxTest`、`DdcAiVersionServiceTest` |
| CREATE/MODIFY | `AJ/service/ai/job/{DdcAiOptimizationScanner,DdcAiFeedbackRetentionScanner}.java`、`DdcAiOptimizationConfiguration.java`、`A/pom.xml`、三个 IT | Failsafe profile `tianshu-ai-persistence-it` | 作业与 IT | 装配 | Step 11 | REQ-002～005/011 | 作业单测、IT |
| MODIFY | `AJ/controller/ai/DdcAiResourceController.java`、`DdcAiExceptionAdvice.java`、`AJ/config/DdcAiWebConfiguration.java` | 11 operation、状态映射、严格类列表 | 17 operation | REST | Step 12 | REQ-004/005/009 | `DdcAiOptimizationControllerTest`、`DdcAiOpenApiContractTest` |
| CREATE/MODIFY | `AJ/model/command/{DdcAiSkillDraftUploadCommand,DdcAiSkillPublishCommand}.java`、`AJ/model/vo/{DdcAiVersionHeadResult,DdcAiRevisionResult}.java`、`DdcAiSyncCommandConverter.java`、`DdcAiResourceRpcProvider.java`、`DdcAiResourceService.java`、`DdcAiResourceServiceImpl.java` | Provider 2 方法 | RPC-004/005/006 | RPC | Step 13 | REQ-007/008 | `DdcAiResourceRpcProviderTest` |
| CREATE/MODIFY | `N/pom.xml`、imports、`NJ/autoconfigure`、`NJ/properties/*`、`NJ/feedback/*`、`egon-cola-xingyuan/egon-cola-tianshu/pom.xml`、`egon-cola-xingyuan/pom.xml` | `http-registration-starter` 先例 | 新模块与上报 | starter | Step 14 | REQ-002/010 | `DdcAiStarterAutoConfigurationTest`、`DefaultDdcAiUsageReporterTest` |
| CREATE/MODIFY | `NJ/workspace/*`、`SJ/autoconfigure/DdcRedisAutoConfiguration.java`、`ST/autoconfigure/DdcRedisAutoConfigurationTest.java`、`NJ/autoconfigure/DdcAiStarterAutoConfiguration.java` | `DdcConfigChangeListener`、`DdcRedisTopicSubscription` | 落盘/订阅/对账 | starter | Step 15 | REQ-006/007/010/011 | 三个 workspace 测试 |
| CREATE/MODIFY | `NJ/authoring/*`、`DdcAiStarterAutoConfiguration.java` | 无 | 上传 | starter | Step 16 | REQ-008/010 | 两个 authoring 测试 |
| MODIFY/CREATE | `W/api/types.ts`、`W/api/ai.ts`、`W/pages/AiOptimizationPanel.tsx`、`AiFeedbackPanel.tsx`、两测试、`AiResourceEditorPage.tsx`、`AiResourceEditorPage.test.tsx` | Tabs 两项、`ai.ts` 客户端 | 第三页签 | 前端 | Step 17 | REQ-009 | Vitest |
| CREATE/MODIFY | `AT/contract/DdcAiOptimizationStandardsTest.java`、`T/docs/tianshu-ai-migration-runbook.md`、`T/README.md`、`T/README.zh-CN.md` | 既有手册与 README 模块表 | 门禁与文档 | 收尾 | Step 18 | REQ-001/014 | `DdcAiOptimizationStandardsTest` |

## 6. Prerequisites, Constraints, and Plan Clarifications

### 6.1 Repository and worktree baseline

- 仓库及父目录无 `AGENTS.md`；遵循会话用户规则与两个 Skill 参考。
- `main@4ad87524a`；工作树仅新增/修改本 Spec 与本 Plan 文档；执行前先提交文档（`docs(tianshu): spec and plan for ai feedback optimization`），再按 Step 提交代码。
- 不触碰：`egon-cola-archetypes/**`、`egon-cola-yuheng/**`、天权模块、既有 SQL `V20260922_001`/`V20260928_001` 与 manifest 两条已登记项、`DdcAiResourceController` 既有 11 个方法体。
- 生成文件：proto 由 `protobuf-maven-plugin` 生成到 `target`（不提交）；MapStruct Impl 由处理器生成；前端 `dist/` 不提交新构建产物。
- 每 Step 只 `git add` 本步 Commit paths；未提交的其他路径保持原状。

### 6.2 Build, test, and environment prerequisites

| Concern | Exact command/source | Required state | Validation boundary |
| --- | --- | --- | --- |
| Maven 构建 | `./mvnw -q -pl egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-starter -am -DskipTests install`（base starter 变更后先安装到本地仓库供 adapter/admin 解析） | JDK 21、`mvnw` 可执行 | static/module |
| Admin 单元/契约测试 | `./mvnw -pl egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin -Dtest=按Step选择器 -Dsurefire.failIfNoSpecifiedTests=false test` | 不需要 PG/Redis | module |
| Admin PG/Outbox IT | `./mvnw -pl egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin -am -Ptianshu-ai-persistence-it verify` + 手册环境变量 `TIANSHU_AI_PERSISTENCE_IT_*` | 隔离 PostgreSQL/Redis | user-controlled runtime |
| adapter 测试 | `./mvnw -pl egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-tianshu-adapter -Dtest=按Step选择器 test` | protoc 由 os-maven-plugin 解析 | module |
| ai-starter 测试 | `./mvnw -pl egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-ai-starter -Dtest=按Step选择器 test` | 模块已登记 | module |
| 前端 | `npm --prefix egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin-web run typecheck`、`... run test -- 按Step文件`、`... run lint` | `npm ci` 已执行 | static/module |
| Spec/Plan 校验 | `python3 .claude/skills/egon-coding-writing-plan/scripts/validate_plan.py docs/egon/plan/2026-10-17-14-12-tianshu-ai-feedback-optimization-implementation.md --strict` | — | static |

测试选择器、Plan 路径与前端测试文件在各 Step 给出精确值。

### 6.3 Immutable constraints and approved decisions

- DEC-001～DEC-008（主 Spec §5.3）；不新增 Maven 依赖（Admin/base starter/adapter）；ai-starter 仅依赖 `egon-cola-tianshu-starter`、`egon-cola-component-rpc-tianshu-adapter`、`spring-boot-autoconfigure`、`spring-boot-configuration-processor`（optional）、`spring-boot-starter-test`（test）。
- 既有 11 REST、2 RPC wire 与错误码不变；既有 SQL/manifest 条目不变；lifecycle/live 唯一键规则；事件只经 Outbox。
- 限额：256 节点、256KiB 文件、1MiB 整包、4MiB gRPC；摘录 8KiB/32KiB；批 100。
- 三 profile 键结构一致；`admin.ai.optimization.enabled` 默认 false；策略 `autoPublish` 默认 false。

### 6.4 Plan Clarifications

| ID | Small implementation inference | Repository evidence | Why semantics are unchanged | Impact if wrong |
| --- | --- | --- | --- | --- |
| PLAN-CLAR-001 | `DdcAiUsageReportItemReceiptDTO` 作为 `DdcAiUsageReportReceiptDTO` 的静态嵌套类而非独立文件 | SDK 包内 `DdcSkillResult` 等均为独立顶层类，但嵌套类不改变 wire | 字段与 proto 一一对应 | 拆为独立文件即可 |
| PLAN-CLAR-002 | 为实现 RPC-004，在既有 `DdcAiResourceService` 追加只读方法 `getVersionHead(DdcAiResourceQuery)`（返回 `DdcAiVersionHeadResult`），而非在 Provider 内组合两次调用 | 主 Spec §9.2.8 逻辑“只读事务：findActiveById + 当前版本头”；`getPublishedVersion` 同类写法 | 接口追加方法不改既有 wire；Spec §8.3 已标 `DdcAiResourceServiceImpl` MODIFY | 改为 Provider 内两次调用 |
| PLAN-CLAR-003 | 三个 RPC 命令映射（上报/上传/发布）合并在一个 `DdcAiSyncCommandConverter`（BaseForwardConverter 多方法） | Spec §10.4 命名 `DdcAiSyncCommandConverter`；`DdcAiProtoConverter` 同样是多方法 Mapper | 转换语义不变 | 拆为三个 Mapper |
| PLAN-CLAR-004 | `DdcAiModelException` 放在 `AJ/service/ai/model/` 与适配器同包 | Spec 只给出异常名；`AJ/common/DdcAdminException.java` 是旧通用异常 | 内部异常，不出 Service | 移动包 |
| PLAN-CLAR-005 | 策略表 Save/Update 用 `@ConvertGroup` 从 Default 到 `EgonColaModelValidationGroups.Insert/Update`，与 `DdcAiDraftCommand` 的 `@ConvertGroup` 写法一致 | `AJ/model/command/DdcAiDraftCommand.java:42` | 校验语义不变 | 显式 group 参数 |
| PLAN-CLAR-006 | `DdcRpcServerInterceptorTest`、`DdcRpcAutoConfigurationTest` 等既有测试按既有命名新增测试方法，不新建并行测试类 | 列出的既有测试类 | 仅测试组织 | 新建类 |
| PLAN-CLAR-007 | Admin 测试 profile 下 `admin.ai.optimization.enabled=false`，作业/模型 Bean 不装配，但 `DdcAiFeedbackService`/`DdcAiOptimizationService` 与 REST 仍装配（默认 `@Service`），手动触发在通道关闭时返回 56322 | 主 Spec §14.2 与 §9.2.3 | 行为即 Spec 规定 | 无 |
| PLAN-CLAR-008 | §7 中结构同构的兄弟文件（如同一包内的 6 个持久化枚举、3 个 PO、伴随测试类）合并在一个 File 块描述，块标题取首个路径，其余路径在 Symbols/伪代码中逐一命名并全部列入 Commit paths | 既有 `DdcAiPublicationPO`/`DdcAiPublicationBO` 等同构类写法一致 | 仅文档组织，不改变任何文件的内容或顺序 | 拆分为独立块 |
| PLAN-CLAR-009 | `DdcRpcScopeExtractor.Scope` 增加 `Set<String> namespaceCodes` 分量（五参构造器保留并默认为单元素集合），拦截器对四个 AI 操作按集合逐一匹配 namespace 模式 | `Scope` record 第 222 行；拦截器第 158 行只校验单个 `namespaceCode` | 批量上报可含多个 namespace，Spec §9.2.7 要求逐条 namespace 权限；集合校验只是把同一规则应用到批 | 改为 Provider 内逐条校验（已在 Service `assertCanReport` 兜底） |
| PLAN-CLAR-010 | `DdcAiOptimizationProperties` 以 `@EnableConfigurationProperties` 在 `DdcAiOptimizationConfiguration` 登记，Bean 名沿 Spring 默认 `egon.cola.component.tianshu.admin.ai-top...DdcAiOptimizationProperties` | `DdcAdminProperties` 的既有登记与 `@Qualifier("egon.cola.component.tianshu.admin-top...DdcAdminProperties")` 写法 | 仅 Bean 登记方式 | 改为 `@ConfigurationPropertiesScan` |
| PLAN-CLAR-011 | 玉衡 SERVICE token 请求使用 `ServiceTokenContext.PLATFORM` 且 `tenantId=null` | `IdpServiceTokenRequest` 紧凑构造器 `Objects.requireNonNull(context)`，Spec §9.2.15 写的 `null` 无法编译；`tenantId` 仅 TENANT 上下文需要 | Admin 作为平台控制面调用网关，与 Spec “按请求键缓存单 token” 一致 | 改为 TENANT 上下文并传 identityTenantId |
| PLAN-CLAR-012 | 运行认领的 `lease_owner` 取既有 Admin 实例标识（若 `DdcAdminProperties` 无实例 ID 则用主机名 + PID） | `DdcAdminProperties` 无 instanceId 字段；JOB-002 只要求可区分实例 | 仅诊断用途 | 改为配置键 |
| PLAN-CLAR-013 | 工作区 `current` 用标记文件（内容 `v{versionNo}`）而非符号链接 | Windows/容器只读层不保证符号链接；Spec 只要求“原子切换 current” | `move(ATOMIC_MOVE)` 同样原子 | 改为符号链接 |
| PLAN-CLAR-014 | ai-starter 的对账与创作目录轮询用自管 `ScheduledExecutorService`，不依赖宿主 `@EnableScheduling` | `DdcHttpRegistrationAutoConfiguration` 心跳为自管线程 | 调度机制不影响行为 | 改为 `@Scheduled` |

## 7. Ordered File-by-file Implementation Steps

> 每个 Step 独立可验证并对应一次语义提交。Java 文件均在既有传统三层或技术模块包族内；`@RequiredArgsConstructor(onConstructor_ = @Autowired)`、`@Slf4j`、具名 Bean、`@Qualifier`、`lombok.config` 传播为每个业务 Bean 的固定要求，文件块不再逐条复述注解语法，只写本文件的具体义务。

### Step 1 — 建立 base starter 共享 AI 契约（Port、SDK 模型、事件、aiTopic）

- Requirements: `REQ-002`, `REQ-007`, `REQ-008`, `REQ-010`
- Dependencies: None
- Baseline state: `SJ/api/client` 仅有 `DdcPromptClient/DdcSkillClient`；`SJ/model/ai` 有 `DdcAiResourceQuery/DdcPromptResult/DdcSkillResult/DdcAiNodeDTO/DdcAiContentDTO`；`DdcRedisKeys` 无 `aiTopic`；`ST/redis/DdcRedisKeysTest` 通过。
- Observable outcome: adapter/Admin/ai-starter 可以编译依赖的两个 Port、10 个 SDK 模型、事件类与 `aiTopic` 键；SDK 模型 JSON 往返与校验可证明。
- End state: base starter 新契约就绪并通过聚焦测试；尚无实现者（Port 无 Bean），不影响既有装配。
- Test-first gate: Required — `DdcRedisKeysTest#aiTopicUsesTenantScopedV4PrefixAndAiSuffix` 因 `aiTopic` 不存在编译失败；`DdcAiSdkContractTest` 因模型类不存在编译失败；两者在 File 3 以后变为断言级 RED→GREEN。
- Manual Checks: MC-ARCH-001, MC-REUSE-001, MC-NAME-001, MC-VALID-001, MC-MODEL-001, MC-JSON-001, MC-TIME-001, MC-SCOPE-001, MC-TEST-001
- Literal Rules: Rule 1, Rule 2, Rule 3, Rule 5, Rule 6, Rule 10, Rule 11
- Ordered files:

#### File 1 — `MODIFY egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-starter/src/test/java/top/egon/cola/component/tianshu/redis/DdcRedisKeysTest.java`

- Purpose: 固定 AI 发布 Topic 的键格式。
- Symbols: `aiTopicUsesTenantScopedV4PrefixAndAiSuffix`、`aiTopicRejectsMissingTenant`
- Repository evidence: 既有测试类覆盖 `topic/registryTopic` 格式（`ST/redis/DdcRedisKeysTest.java`、`DdcTenantRedisKeyTest.java`）。
- Dependencies and consumers: 调用 `DdcRedisKeys.aiTopic`；被 Step 10 Handler 正则与 Step 15 listener 共用。
- Why now: RED 契约先于实现。
- Contract/signature changes: 断言 `aiTopic(42L,"xingyuan","dev","order-service")` 匹配 `^tianshu:v4:tenant:42:\{[a-f0-9]{64}}:ai:topic$` 且与 `topic(...)` 共享同一 slot hash 段。
- Input/output and state mapping: 固定入参→键字符串；无状态。
- Error and edge behavior: tenantId 为 null/非正抛 `IllegalArgumentException`（沿既有 `buildKey` 行为）。
- Standards impact: MC-TEST-001, MC-SCOPE-001 — 只新增测试方法。
- Literal rule enforcement: Rule 11 — 测试位于 base starter 既有测试包。
- Implementation pseudocode:

```java
@Test void aiTopicUsesTenantScopedV4PrefixAndAiSuffix() {
    String topic = DdcRedisKeys.aiTopic(42L, "xingyuan", "dev", "order-service");
    String config = DdcRedisKeys.topic(42L, "xingyuan", "dev", "order-service");
    assertThat(topic).matches("^tianshu:v4:tenant:42:\\{[a-f0-9]{64}}:ai:topic$");
    assertThat(slotHash(topic)).isEqualTo(slotHash(config)); // 同 configTag
}
@Test void aiTopicRejectsMissingTenant() {
    assertThatThrownBy(() -> DdcRedisKeys.aiTopic(null, "b", "e", "a")).isInstanceOf(IllegalArgumentException.class);
}
```

- Verification contribution: `-Dtest=DdcRedisKeysTest` RED→GREEN。
- After this file: 编译失败（方法缺失），属预期编译前置缺口。

#### File 2 — `CREATE egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-starter/src/test/java/top/egon/cola/component/tianshu/model/ai/DdcAiSdkContractTest.java`

- Purpose: 固定 SDK 模型的 Jackson wire、约束与枚举 `@JsonValue`。
- Symbols: `usageReportSerializesLongsAsStringsAndInstantAsUtc`、`usageReportRejectsInvalidUsageIdAndScore`、`publicationEventRoundTripsWithinTenantEnvelope`、`enumsExposeStableJsonValues`
- Repository evidence: `ST/model/management/DdcManagementDtoSerializationTest.java`（Jackson 往返写法）、`DdcAiStableVersionSelectorTest`（校验写法）。
- Dependencies and consumers: 使用 Step 1 File 4–15 的类型；`ValidationUtils` 来自 common-core。
- Why now: 行为 RED 契约。
- Contract/signature changes: 断言 `DdcAiUsageReportDTO` JSON 键集合、`versionId/resourceId` 字符串、`occurredAt` 形如 `2026-10-17T04:58:12.345Z`；`DdcAiPublicationEvent` 在 `DdcTenantMessageDTO` 信封中往返；`DdcAiOutcomeEnum.FAILURE` 序列化为 `"FAILURE"`。
- Input/output and state mapping: 构造→序列化→反序列化→相等。
- Error and edge behavior: `usageId` 不符 `^[A-Za-z0-9_-]{16,64}$`、`explicitScore=101`、`occurredAt=null` 产生 ConstraintViolation。
- Standards impact: MC-JSON-001, MC-VALID-001, MC-MODEL-001, MC-TEST-001, MC-SCOPE-001。
- Literal rule enforcement: Rule 6（Jackson 唯一）、Rule 2（SDK 边界注解）、Rule 10（Instant Z）、Rule 11。
- Implementation pseudocode:

```java
ObjectMapper mapper = JsonMapper.builder().addModule(new JavaTimeModule()).disable(WRITE_DATES_AS_TIMESTAMPS).build();
@Test void usageReportSerializesLongsAsStringsAndInstantAsUtc() {
    DdcAiUsageReportDTO report = DdcAiUsageReportDTO.builder().usageId("01JAX0M7Q9K2R5T8V1W4X7Y0ZB")
        .namespaceCode("default").resourceKey("refund").resourceId(1L).versionId(2L).versionNo(4L)
        .outcome(DdcAiOutcomeEnum.FAILURE).occurredAt(Instant.parse("2026-10-17T04:58:12.345Z")).build();
    JsonNode json = mapper.valueToTree(report);
    assertThat(json.get("resourceId").asText()).isEqualTo("1"); assertThat(json.get("occurredAt").asText()).endsWith("Z");
    assertThat(mapper.treeToValue(json, DdcAiUsageReportDTO.class)).isEqualTo(report);
}
@Test void usageReportRejectsInvalidUsageIdAndScore() { assertThat(validator.validate(report.toBuilder().usageId("short").explicitScore(101).build())).hasSize(2); }
```

- Verification contribution: `-Dtest=DdcAiSdkContractTest`。
- After this file: 编译失败直到 File 4–15 存在；之后断言 GREEN。

#### File 3 — `MODIFY egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-starter/src/main/java/top/egon/cola/component/tianshu/redis/DdcRedisKeys.java`

- Purpose: 新增 AI 发布 Topic 键。
- Symbols: `public static String aiTopic(Long tenantId, String bizCode, String env, String appCode)`
- Repository evidence: `topic(...)` 实现 `buildKey(tenantId, configTag(tenantId,bizCode,env,appCode), "topic")`（`SJ/redis/DdcRedisKeys.java:68`）。
- Dependencies and consumers: Step 10 `DdcAiPublicationNotifier/DeliveryHandler`、Step 15 listener。
- Why now: 使 File 1 编译并 GREEN。
- Contract/signature changes: 新静态方法；现有方法不变。
- Input/output and state mapping: 同 `topic`，后缀改为 `"ai","topic"`。
- Error and edge behavior: 复用 `buildKey` 的参数校验。
- Standards impact: MC-NAME-001, MC-UTIL-001（JDK）。
- Literal rule enforcement: Rule 5（无新工具库）、Rule 11（键在既有工具类）。
- Implementation pseudocode:

```java
/** AI 发布事实 Topic：tianshu:v4:tenant:{tenantId}:{slotHash}:ai:topic */
public static String aiTopic(Long tenantId, String bizCode, String env, String appCode) {
    return buildKey(tenantId, configTag(tenantId, bizCode, env, appCode), "ai", "topic");
}
```

- Verification contribution: File 1 GREEN。
- After this file: `DdcRedisKeysTest` 全绿。

#### File 4 — `CREATE egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-starter/src/main/java/top/egon/cola/component/tianshu/model/ai/DdcAiOutcomeEnum.java`

- Purpose: SDK 使用结果枚举。
- Symbols: `SUCCESS, FAILURE, UNKNOWN`；`@JsonValue getValue()`；`@JsonCreator fromValue`
- Repository evidence: `SJ/model/ai/DdcAiKindEnum.java`（SDK 枚举带 `@JsonValue/@JsonCreator`，无 MP 依赖）。
- Dependencies and consumers: `DdcAiUsageReportDTO`、adapter 枚举映射、Admin `DdcAiEnumConverter`。
- Why now: DTO 编译前置。
- Contract/signature changes: 新枚举。
- Input/output and state mapping: JSON `"SUCCESS"` ↔ 常量；未知值抛 `IllegalArgumentException`→400。
- Error and edge behavior: 不接受 ordinal。
- Standards impact: MC-NAME-001（`*Enum`）、MC-JSON-001。
- Literal rule enforcement: Rule 6（`@JsonValue`，无 `@EnumValue`：SDK 无 MP）、Rule 1、Rule 11。
- Implementation pseudocode:

```java
public enum DdcAiOutcomeEnum {
    SUCCESS("SUCCESS"), FAILURE("FAILURE"), UNKNOWN("UNKNOWN");
    private final String value;
    @JsonValue public String getValue() { return value; }
    @JsonCreator public static DdcAiOutcomeEnum fromValue(String value) { /* 精确匹配，否则 IllegalArgumentException */ }
}
```

- Verification contribution: `DdcAiSdkContractTest#enumsExposeStableJsonValues`。
- After this file: 编译通过；DTO 仍缺。

#### File 5 — `CREATE egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-starter/src/main/java/top/egon/cola/component/tianshu/model/ai/DdcAiUsageReportStatusEnum.java`

- Purpose: 上报回执状态枚举。
- Symbols: `ACCEPTED, ACCEPTED_DUPLICATE, REJECTED`
- Repository evidence: 同 File 4。
- Dependencies and consumers: `DdcAiUsageReportReceiptDTO`、ai-starter 指标维度。
- Why now: 回执 DTO 前置。
- Contract/signature changes: 新枚举。
- Input/output and state mapping: JSON 字符串。
- Error and edge behavior: 未知值拒绝。
- Standards impact: MC-NAME-001, MC-JSON-001。
- Literal rule enforcement: Rule 1, Rule 6, Rule 11。
- Implementation pseudocode:

```java
public enum DdcAiUsageReportStatusEnum {
    ACCEPTED("ACCEPTED"), ACCEPTED_DUPLICATE("ACCEPTED_DUPLICATE"), REJECTED("REJECTED");
    @JsonValue public String getValue();
    @JsonCreator public static DdcAiUsageReportStatusEnum fromValue(String value);
}
```

- Verification contribution: `DdcAiSdkContractTest`。
- After this file: 编译通过。

#### File 6 — `CREATE egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-starter/src/main/java/top/egon/cola/component/tianshu/model/ai/DdcAiUsageReportDTO.java`

- Purpose: 单条使用上报载体。
- Symbols: 字段 `usageId, namespaceCode, resourceKey, resourceId, versionId, versionNo, outcome, explicitScore, inputExcerpt, outputExcerpt, latencyMs, occurredAt, traceId`
- Repository evidence: `SJ/model/ai/DdcSkillResult.java`（class + `@Builder` + `@JsonFormat(STRING)` + `@JsonInclude(ALWAYS)`）。
- Dependencies and consumers: `DdcAiUsageReporter`（Step 14）、adapter 转换、Admin `DdcAiSyncCommandConverter`。
- Why now: 批 DTO 与回执前置。
- Contract/signature changes: 新 class：`@Data @NoArgsConstructor @AllArgsConstructor @Accessors(chain=true) @Builder(toBuilder=true) @JsonInclude(ALWAYS)`。
- Input/output and state mapping: Long 字段 `@JsonFormat(shape=STRING)`；`occurredAt:Instant`（`@JsonFormat(shape=STRING)`，UTC Z）；`outcome:DdcAiOutcomeEnum`。
- Error and edge behavior: 约束：`@NotBlank @Pattern(^[A-Za-z0-9_-]{16,64}$) usageId`；`@NotBlank @Size(max=128)` namespaceCode/resourceKey；`@NotNull @Positive` resourceId/versionId/versionNo；`@NotNull` outcome/occurredAt；`@Min(0) @Max(100)` explicitScore；`@Size(max=8192)` inputExcerpt（字节上限由服务端再截断）；`@Size(max=32768)` outputExcerpt；`@PositiveOrZero` latencyMs；`@Size(max=64)` traceId。
- Standards impact: MC-MODEL-001（class 基线）、MC-VALID-001、MC-JSON-001、MC-TIME-001（Instant）。
- Literal rule enforcement: Rule 3（完整 Lombok class）、Rule 2（SDK 入口约束）、Rule 6、Rule 10、Rule 11。
- Implementation pseudocode:

```java
@Data @NoArgsConstructor @AllArgsConstructor @Accessors(chain = true) @Builder(toBuilder = true) @JsonInclude(JsonInclude.Include.ALWAYS)
public class DdcAiUsageReportDTO {
    @NotBlank @Pattern(regexp = "^[A-Za-z0-9_-]{16,64}$") private String usageId;
    @NotBlank @Size(max = 128) private String namespaceCode;  @NotBlank @Size(max = 128) private String resourceKey;
    @NotNull @Positive @JsonFormat(shape = STRING) private Long resourceId;  /* versionId, versionNo 同 */
    @NotNull private DdcAiOutcomeEnum outcome;  @Min(0) @Max(100) private Integer explicitScore;
    @Size(max = 8192) private String inputExcerpt;  @Size(max = 32768) private String outputExcerpt;
    @PositiveOrZero private Integer latencyMs;  @NotNull @JsonFormat(shape = STRING) private Instant occurredAt;  @Size(max = 64) private String traceId;
}
```

- Verification contribution: `DdcAiSdkContractTest` 前两个用例。
- After this file: 编译通过。

#### File 7 — `CREATE egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-starter/src/main/java/top/egon/cola/component/tianshu/model/ai/DdcAiUsageReportBatchDTO.java`

- Purpose: 上报批载体。
- Symbols: `List<@Valid DdcAiUsageReportDTO> reports`
- Repository evidence: `DdcAiContentDTO` 的 `@NotNull @Valid @Size(max=257) List` 写法。
- Dependencies and consumers: `DdcAiFeedbackClient#report`。
- Why now: Port 签名前置。
- Contract/signature changes: 新 class；`@NotNull @Size(min=1,max=100) @Valid`。
- Input/output and state mapping: 列表。
- Error and edge behavior: 空批与超 100 拒绝。
- Standards impact: MC-MODEL-001, MC-VALID-001。
- Literal rule enforcement: Rule 2, Rule 3, Rule 11。
- Implementation pseudocode:

```java
@Data @NoArgsConstructor @AllArgsConstructor @Accessors(chain = true) @Builder
public class DdcAiUsageReportBatchDTO {
    @NotNull @Size(min = 1, max = 100) @Valid private List<DdcAiUsageReportDTO> reports;
}
```

- Verification contribution: `DdcAiSdkContractTest`。
- After this file: 编译通过。

#### File 8 — `CREATE egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-starter/src/main/java/top/egon/cola/component/tianshu/model/ai/DdcAiUsageReportReceiptDTO.java`

- Purpose: 上报回执（含嵌套条目类型，PLAN-CLAR-001）。
- Symbols: `List<ItemDTO> items`；`public static class ItemDTO { String usageId; DdcAiUsageReportStatusEnum status; String reason; }`
- Repository evidence: 同 File 6。
- Dependencies and consumers: `DdcAiFeedbackClient#report` 返回；Reporter 指标。
- Why now: Port 返回类型前置。
- Contract/signature changes: 新 class 与嵌套 class（同 Lombok 基线）。
- Input/output and state mapping: 与 proto `AiUsageReportReceipt` 一一对应。
- Error and edge behavior: `reason` 仅 REJECTED 非空。
- Standards impact: MC-MODEL-001, MC-JSON-001。
- Literal rule enforcement: Rule 1（`*DTO`）、Rule 3、Rule 11。
- Implementation pseudocode:

```java
@Data @NoArgsConstructor @AllArgsConstructor @Accessors(chain = true) @Builder @JsonInclude(ALWAYS)
public class DdcAiUsageReportReceiptDTO {
    @NotNull @Valid private List<ItemDTO> items;
    @Data @NoArgsConstructor @AllArgsConstructor @Accessors(chain = true) @Builder
    public static class ItemDTO { @NotBlank private String usageId; @NotNull private DdcAiUsageReportStatusEnum status; @Size(max = 64) private String reason; }
}
```

- Verification contribution: `DdcAiSdkContractTest`。
- After this file: 编译通过。

#### File 9 — `CREATE egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-starter/src/main/java/top/egon/cola/component/tianshu/model/ai/DdcAiVersionHeadDTO.java`

- Purpose: RPC-004 返回头。
- Symbols: `resourceId, resourceKey, versionId(nullable), versionNo(nullable), contentSha256(nullable), resourceRevision, published`
- Repository evidence: `DdcPromptResult` 写法。
- Dependencies and consumers: `DdcAiSyncClient#getVersionHead`、对账调度器。
- Why now: Port 前置。
- Contract/signature changes: 新 class。
- Input/output and state mapping: Long→String JSON；`published=false` 时三个可空字段为 null。
- Error and edge behavior: `contentSha256` 若非空匹配 `^[0-9a-f]{64}$`。
- Standards impact: MC-MODEL-001, MC-JSON-001。
- Literal rule enforcement: Rule 1, Rule 3, Rule 6, Rule 11。
- Implementation pseudocode:

```java
@Data @NoArgsConstructor @AllArgsConstructor @Accessors(chain = true) @Builder @JsonInclude(ALWAYS)
public class DdcAiVersionHeadDTO {
    @NotNull @Positive @JsonFormat(shape = STRING) private Long resourceId;  @NotBlank private String resourceKey;
    @Positive @JsonFormat(shape = STRING) private Long versionId;  @Positive @JsonFormat(shape = STRING) private Long versionNo;
    @Pattern(regexp = "^[0-9a-f]{64}$") private String contentSha256;  @NotNull @Min(0) @JsonFormat(shape = STRING) private Long resourceRevision;  @NotNull private Boolean published;
}
```

- Verification contribution: `DdcAiSdkContractTest`。
- After this file: 编译通过。

#### File 10 — `CREATE egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-starter/src/main/java/top/egon/cola/component/tianshu/model/ai/DdcAiSkillDraftUploadDTO.java`

- Purpose: RPC-005 上传载体。
- Symbols: `namespaceCode, resourceKey, resourceId, expectedRevision, displayName, description, List<DdcAiNodeDTO> files`
- Repository evidence: `DdcAiContentDTO`（`@ValidAiContent` Save 组可复用于 files）。
- Dependencies and consumers: `DdcAiSyncClient#replaceSkillDraft`、`DdcAiSkillAuthoringWatcher`。
- Why now: Port 前置。
- Contract/signature changes: 新 class；`files` 带 `@Valid @Size(max=257)`；类级 `@ValidAiContent` 不适用（它作用于 `DdcAiContentDTO`），改为在 Admin 转换为 `DdcAiContentDTO` 后校验（Step 13）。
- Input/output and state mapping: 与 proto `AiSkillDraftUpload` 对应；`expectedRevision:Long @Min(0)`。
- Error and edge behavior: `displayName 1..128`、`description ≤2048`。
- Standards impact: MC-MODEL-001, MC-VALID-001。
- Literal rule enforcement: Rule 2, Rule 3, Rule 11。
- Implementation pseudocode:

```java
@Data @NoArgsConstructor @AllArgsConstructor @Accessors(chain = true) @Builder
public class DdcAiSkillDraftUploadDTO {
    @NotBlank @Size(max = 128) private String namespaceCode;  @NotBlank @Size(max = 128) private String resourceKey;
    @NotNull @Positive private Long resourceId;  @NotNull @Min(0) private Long expectedRevision;
    @NotBlank @Size(max = 128) private String displayName;  @NotNull @Size(max = 2048) private String description;
    @NotNull @Size(min = 1, max = 257) @Valid private List<DdcAiNodeDTO> files;
}
```

- Verification contribution: `DdcAiSdkContractTest`。
- After this file: 编译通过。

#### File 11 — `CREATE egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-starter/src/main/java/top/egon/cola/component/tianshu/model/ai/DdcAiSkillPublishDTO.java`

- Purpose: RPC-006 发布载体。
- Symbols: `namespaceCode, resourceKey, resourceId, expectedRevision, requestId, changeNote`
- Repository evidence: 同上。
- Dependencies and consumers: `DdcAiSyncClient#publishSkillDraft`。
- Why now: Port 前置。
- Contract/signature changes: 新 class；`requestId ^[A-Za-z0-9_-]{16,64}$`；`changeNote ≤512`。
- Input/output and state mapping: 与 proto 对应。
- Error and edge behavior: 约束失败即拒绝。
- Standards impact: MC-MODEL-001, MC-VALID-001。
- Literal rule enforcement: Rule 2, Rule 3, Rule 11。
- Implementation pseudocode:

```java
@Data @NoArgsConstructor @AllArgsConstructor @Accessors(chain = true) @Builder
public class DdcAiSkillPublishDTO {
    @NotBlank @Size(max = 128) private String namespaceCode;  @NotBlank @Size(max = 128) private String resourceKey;
    @NotNull @Positive private Long resourceId;  @NotNull @Min(0) private Long expectedRevision;
    @NotBlank @Pattern(regexp = "^[A-Za-z0-9_-]{16,64}$") private String requestId;  @NotNull @Size(max = 512) private String changeNote;
}
```

- Verification contribution: `DdcAiSdkContractTest`。
- After this file: 编译通过。

#### File 12 — `CREATE egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-starter/src/main/java/top/egon/cola/component/tianshu/model/ai/DdcAiRevisionDTO.java`

- Purpose: RPC-005 回执。
- Symbols: `resourceId, resourceRevision, draftSha256, draftMatchesCurrent`
- Repository evidence: 同上。
- Dependencies and consumers: 上传监听器状态文件。
- Why now: Port 前置。
- Contract/signature changes: 新 class。
- Input/output and state mapping: 与 proto `AiRevisionResult` 对应。
- Error and edge behavior: `draftSha256` 64 hex。
- Standards impact: MC-MODEL-001, MC-JSON-001。
- Literal rule enforcement: Rule 1, Rule 3, Rule 11。
- Implementation pseudocode:

```java
@Data @NoArgsConstructor @AllArgsConstructor @Accessors(chain = true) @Builder @JsonInclude(ALWAYS)
public class DdcAiRevisionDTO {
    @NotNull @Positive @JsonFormat(shape = STRING) private Long resourceId;
    @NotNull @Min(0) @JsonFormat(shape = STRING) private Long resourceRevision;
    @NotBlank @Pattern(regexp = "^[0-9a-f]{64}$") private String draftSha256;  @NotNull private Boolean draftMatchesCurrent;
}
```

- Verification contribution: `DdcAiSdkContractTest`。
- After this file: 编译通过。

#### File 13 — `CREATE egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-starter/src/main/java/top/egon/cola/component/tianshu/model/ai/DdcAiPublicationReceiptDTO.java`

- Purpose: RPC-006 回执。
- Symbols: `publicationId, resourceId, targetVersionId, targetVersionNo, resourceRevision, contentSha256`
- Repository evidence: 同上。
- Dependencies and consumers: 上传监听器。
- Why now: Port 前置。
- Contract/signature changes: 新 class。
- Input/output and state mapping: 与 proto `AiPublicationReceipt` 对应。
- Error and edge behavior: 正 Long、64 hex。
- Standards impact: MC-MODEL-001, MC-JSON-001。
- Literal rule enforcement: Rule 1, Rule 3, Rule 11。
- Implementation pseudocode:

```java
@Data @NoArgsConstructor @AllArgsConstructor @Accessors(chain = true) @Builder @JsonInclude(ALWAYS)
public class DdcAiPublicationReceiptDTO {
    @NotNull @Positive @JsonFormat(shape = STRING) private Long publicationId;  @NotNull @Positive @JsonFormat(shape = STRING) private Long resourceId;
    @NotNull @Positive @JsonFormat(shape = STRING) private Long targetVersionId;  @NotNull @Positive @JsonFormat(shape = STRING) private Long targetVersionNo;
    @NotNull @Min(0) @JsonFormat(shape = STRING) private Long resourceRevision;  @NotBlank @Pattern(regexp = "^[0-9a-f]{64}$") private String contentSha256;
}
```

- Verification contribution: `DdcAiSdkContractTest`。
- After this file: 编译通过。

#### File 14 — `CREATE egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-starter/src/main/java/top/egon/cola/component/tianshu/model/event/DdcAiPublicationEvent.java`

- Purpose: EVENT-103 载体（Admin 与 ai-starter 共用）。
- Symbols: `eventId, tenantId, resourceId, kind, bizCode, namespaceCode, env, appCode, resourceKey, operation, versionId, versionNo, contentSha256, resourceRevision, actor, occurredAt`；`hasValidIdentity()`
- Repository evidence: `SJ/model/event/DdcRegistryRevisionEvent.java`（事件 class + `hasValidIdentity`）。
- Dependencies and consumers: `DdcAiPublicationEventConverter`（Step 10）、Handler、listener。
- Why now: Admin 与 starter 共用类型必须先存在。
- Contract/signature changes: 新 class，Lombok 基线，`@JsonInclude(ALWAYS)`。
- Input/output and state mapping: `operation` 为字符串 `PUBLISH/ROLLBACK/PROMOTE`（`@Pattern`），`kind` `PROMPT/SKILL`；Long 字段 STRING；`occurredAt` Instant Z。
- Error and edge behavior: `eventId` 匹配 `^ai-pub-[1-9][0-9]{0,18}$`；`contentSha256` 64 hex；`hasValidIdentity` 要求 eventId 与 `publicationId` 一致由 Notifier 保证。
- Standards impact: MC-MODEL-001, MC-JSON-001, MC-TIME-001。
- Literal rule enforcement: Rule 1（`*Event`）、Rule 3、Rule 6、Rule 10、Rule 11。
- Implementation pseudocode:

```java
@Data @NoArgsConstructor @AllArgsConstructor @Accessors(chain = true) @Builder @JsonInclude(ALWAYS)
public class DdcAiPublicationEvent {
    @NotBlank @Pattern(regexp = "^ai-pub-[1-9][0-9]{0,18}$") private String eventId;
    @NotNull @Positive @JsonFormat(shape = STRING) private Long tenantId;  @NotNull @Positive @JsonFormat(shape = STRING) private Long resourceId;
    @NotBlank @Pattern(regexp = "^(PROMPT|SKILL)$") private String kind;  @NotBlank private String bizCode; /* namespaceCode, env, appCode, resourceKey */
    @NotBlank @Pattern(regexp = "^(PUBLISH|ROLLBACK|PROMOTE)$") private String operation;
    @NotNull @Positive @JsonFormat(shape = STRING) private Long versionId;  @NotNull @Positive @JsonFormat(shape = STRING) private Long versionNo;
    @NotBlank @Pattern(regexp = "^[0-9a-f]{64}$") private String contentSha256;  @NotNull @Min(0) @JsonFormat(shape = STRING) private Long resourceRevision;
    @NotBlank @Size(max = 512) private String actor;  @NotNull @JsonFormat(shape = STRING) private Instant occurredAt;
    public boolean hasValidIdentity() { return eventId != null && tenantId != null && tenantId > 0 && resourceId != null && versionId != null; }
}
```

- Verification contribution: `DdcAiSdkContractTest#publicationEventRoundTripsWithinTenantEnvelope`。
- After this file: 编译通过。

#### File 15 — `CREATE egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-starter/src/main/java/top/egon/cola/component/tianshu/api/client/DdcAiFeedbackClient.java`

- Purpose: 上报 Port。
- Symbols: `DdcAiUsageReportReceiptDTO report(@NotNull @Valid DdcAiUsageReportBatchDTO batch)`
- Repository evidence: `SJ/api/client/DdcPromptClient.java`（接口方法带 `@NotNull @Valid`）。
- Dependencies and consumers: 实现 `RpcDdcAiFeedbackClient`（Step 2）；消费 `DefaultDdcAiUsageReporter`（Step 14）。
- Why now: adapter 实现前置。
- Contract/signature changes: 新接口。
- Input/output and state mapping: 批→回执。
- Error and edge behavior: 实现抛 `DdcException` 子类（既有 `DdcRpcStatusExceptionMapper` 恢复）。
- Standards impact: MC-NAME-001（`*Client` 行为后缀，沿既有）、MC-VALID-001。
- Literal rule enforcement: Rule 2（Port 入参约束）、Rule 11。
- Implementation pseudocode:

```java
public interface DdcAiFeedbackClient {
    /** 批量上报；回执与请求同序同长 */
    DdcAiUsageReportReceiptDTO report(@NotNull @Valid DdcAiUsageReportBatchDTO batch);
}
```

- Verification contribution: `DdcAiSdkContractTest` 编译；Step 2 客户端测试。
- After this file: 编译通过。

#### File 16 — `CREATE egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-starter/src/main/java/top/egon/cola/component/tianshu/api/client/DdcAiSyncClient.java`

- Purpose: 同步 Port（头读取、上传、发布）。
- Symbols: `DdcAiVersionHeadDTO getVersionHead(@NotNull @Valid DdcAiResourceQuery query)`、`DdcAiRevisionDTO replaceSkillDraft(@NotNull @Valid DdcAiSkillDraftUploadDTO upload)`、`DdcAiPublicationReceiptDTO publishSkillDraft(@NotNull @Valid DdcAiSkillPublishDTO publish)`
- Repository evidence: 同 File 15。
- Dependencies and consumers: 实现 `RpcDdcAiSyncClient`；消费 workspace/authoring（Step 15/16）。
- Why now: 同上。
- Contract/signature changes: 新接口；`getVersionHead` 的 `query.versionNo()` 必须为 null（实现校验）。
- Input/output and state mapping: 三方法与 RPC-004/005/006 对应。
- Error and edge behavior: 冲突 56303 由 `DdcException` 携带状态串 `TIANSHU_AI_REVISION_CONFLICT`。
- Standards impact: MC-NAME-001, MC-VALID-001。
- Literal rule enforcement: Rule 2, Rule 11。
- Implementation pseudocode:

```java
public interface DdcAiSyncClient {
    DdcAiVersionHeadDTO getVersionHead(@NotNull @Valid DdcAiResourceQuery query);        // versionNo 必须缺省
    DdcAiRevisionDTO replaceSkillDraft(@NotNull @Valid DdcAiSkillDraftUploadDTO upload);  // expectedRevision CAS
    DdcAiPublicationReceiptDTO publishSkillDraft(@NotNull @Valid DdcAiSkillPublishDTO publish); // requestId 幂等
}
```

- Verification contribution: Step 2 `RpcDdcAiSyncClient` 测试。
- After this file: Step 1 全部编译；两测试 GREEN。

- Validation working directory: 仓库根 `/Users/mario/SelfProject/Egon-COLA`
- Verification command: `./mvnw -pl egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-starter -Dtest=DdcRedisKeysTest,DdcAiSdkContractTest,DdcPackageDocumentationTest,DdcPlatformBoundaryTest -Dsurefire.failIfNoSpecifiedTests=false test && ./mvnw -q -pl egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-starter -DskipTests install`
- Expected result: 四个测试类通过；`DdcPackageDocumentationTest`（包文档/`package-info` 门禁）若要求新包说明，已在既有 `model/ai`、`model/event`、`api/client` 包内无需新增 package-info；本地仓库安装成功供后续模块解析。
- Failure returns to: File 1/2 断言与 File 3–16 实现。
- Completion criteria: 两个新 Port、10 个模型、事件类、`aiTopic` 存在且测试 GREEN；既有 starter 测试全绿。
- Rollback: 仅回退本步 16 个路径。
- Commit paths: egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-starter/src/test/java/top/egon/cola/component/tianshu/redis/DdcRedisKeysTest.java, egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-starter/src/test/java/top/egon/cola/component/tianshu/model/ai/DdcAiSdkContractTest.java, egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-starter/src/main/java/top/egon/cola/component/tianshu/redis/DdcRedisKeys.java, egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-starter/src/main/java/top/egon/cola/component/tianshu/model/ai/DdcAiOutcomeEnum.java, egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-starter/src/main/java/top/egon/cola/component/tianshu/model/ai/DdcAiUsageReportStatusEnum.java, egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-starter/src/main/java/top/egon/cola/component/tianshu/model/ai/DdcAiUsageReportDTO.java, egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-starter/src/main/java/top/egon/cola/component/tianshu/model/ai/DdcAiUsageReportBatchDTO.java, egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-starter/src/main/java/top/egon/cola/component/tianshu/model/ai/DdcAiUsageReportReceiptDTO.java, egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-starter/src/main/java/top/egon/cola/component/tianshu/model/ai/DdcAiVersionHeadDTO.java, egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-starter/src/main/java/top/egon/cola/component/tianshu/model/ai/DdcAiSkillDraftUploadDTO.java, egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-starter/src/main/java/top/egon/cola/component/tianshu/model/ai/DdcAiSkillPublishDTO.java, egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-starter/src/main/java/top/egon/cola/component/tianshu/model/ai/DdcAiRevisionDTO.java, egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-starter/src/main/java/top/egon/cola/component/tianshu/model/ai/DdcAiPublicationReceiptDTO.java, egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-starter/src/main/java/top/egon/cola/component/tianshu/model/event/DdcAiPublicationEvent.java, egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-starter/src/main/java/top/egon/cola/component/tianshu/api/client/DdcAiFeedbackClient.java, egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-starter/src/main/java/top/egon/cola/component/tianshu/api/client/DdcAiSyncClient.java
- Commit: `feat(tianshu): ai-starter shared ports, sdk models and publication event`

### Step 2 — 扩展 Tianshu RPC 契约、操作权限与下游客户端

- Requirements: `REQ-002`, `REQ-007`, `REQ-008`, `REQ-010`
- Dependencies: Step 1
- Baseline state: `tianshu_ai_resource.proto` 两方法；`DdcRpcOperation.AI_READ(19)` 为末尾；`DdcAiResourceRpc` 两方法；`DdcRpcClientFactory#promptClient/skillClient`；`DdcAiRpcContractTest`、`DdcRpcAutoConfigurationTest` 通过。
- Observable outcome: gRPC 服务含 `ReportUsage/GetVersionHead/ReplaceSkillDraft/PublishSkillDraft`；操作 `AI_REPORT(20)/AI_WRITE(21)/AI_PUBLISH(22)` 可解析；`DdcAiFeedbackClient/DdcAiSyncClient` 有 RPC 实现并按开关装配。
- End state: adapter 契约与客户端就绪；服务端 Provider 尚未实现（Step 6/13），客户端调用旧服务端得 `UNIMPLEMENTED`（§9 兼容规则）。
- Test-first gate: Required — `DdcAiRpcContractTest#aiResourceServiceExposesFeedbackAndSyncMethods` 因描述符缺少 4 个方法 RED；`DdcRpcAutoConfigurationTest` 新用例因 Bean 缺失 RED。
- Manual Checks: MC-ARCH-001, MC-REUSE-001, MC-NAME-001, MC-VALID-001, MC-MODEL-001, MC-CONVERT-001, MC-BEAN-001, MC-JSON-001, MC-CONFIG-001, MC-SCOPE-001, MC-TEST-001
- Literal Rules: Rule 1, Rule 2, Rule 3, Rule 4, Rule 6, Rule 7, Rule 11
- Ordered files:

#### File 1 — `MODIFY egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-tianshu-adapter/src/test/java/top/egon/cola/component/rpc/tianshu/client/ai/DdcAiRpcContractTest.java`

- Purpose: 固定新 RPC 方法、幂等标记与操作映射。
- Symbols: `aiResourceServiceExposesFeedbackAndSyncMethods`、`newAiOperationsResolveToDedicatedPermissions`、`protoMessagesCarryExpectedFields`
- Repository evidence: 既有 `DdcAiRpcContractTest` 断言 `GetPrompt/GetSkill` 描述符与 `@EgonRpcMethod`（`RT/.../DdcAiRpcContractTest.java`，实际目录以 `find` 结果为准：`R/src/test/java/top/egon/cola/component/rpc/tianshu/`）。
- Dependencies and consumers: `DdcAiResourceServiceGrpc.getServiceDescriptor()`、`DdcRpcOperationResolver`。
- Why now: RED 契约。
- Contract/signature changes: 断言描述符含 6 个方法；`DdcRpcOperationResolver.resolve("egon.tianshu.v1.DdcAiResourceService/ReportUsage")==AI_REPORT`，`ReplaceSkillDraft→AI_WRITE`，`PublishSkillDraft→AI_PUBLISH`，`GetVersionHead→AI_READ`；`@EgonRpcMethod(idempotent)` 仅 `GetPrompt/GetSkill/GetVersionHead` 为 true。
- Input/output and state mapping: 反射读取注解与描述符。
- Error and edge behavior: `AiUsageReport` 消息含 `usage_id`、`occurred_at`、`explicit_score` optional。
- Standards impact: MC-TEST-001, MC-SCOPE-001。
- Literal rule enforcement: Rule 11 — 契约测试在 adapter 既有测试包。
- Implementation pseudocode:

```java
@Test void aiResourceServiceExposesFeedbackAndSyncMethods() {
    var names = DdcAiResourceServiceGrpc.getServiceDescriptor().getMethods().stream().map(m -> m.getBareMethodName()).toList();
    assertThat(names).containsExactlyInAnyOrder("GetPrompt","GetSkill","ReportUsage","GetVersionHead","ReplaceSkillDraft","PublishSkillDraft");
    assertThat(DdcAiResourceRpc.class.getMethod("reportUsage", AiUsageReportBatch.class).getAnnotation(EgonRpcMethod.class).idempotent()).isFalse();
}
@Test void newAiOperationsResolveToDedicatedPermissions() {
    var r = new DdcRpcOperationResolver();
    assertThat(r.resolve(AI + "ReportUsage")).isEqualTo(DdcRpcOperation.AI_REPORT); /* AI_WRITE, AI_PUBLISH, AI_READ 同理 */
    assertThat(DdcRpcOperation.AI_PUBLISH.getCode()).isEqualTo(22);
}
```

- Verification contribution: `-Dtest=DdcAiRpcContractTest` RED→GREEN。
- After this file: 编译失败（新消息类型缺失）直到 File 2 生成。

#### File 2 — `MODIFY egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-tianshu-adapter/src/main/proto/tianshu_ai_resource.proto`

- Purpose: 追加 4 个 rpc 与消息（向后兼容）。
- Symbols: `ReportUsage`、`GetVersionHead`、`ReplaceSkillDraft`、`PublishSkillDraft`；`AiUsageReport`、`AiUsageReportBatch`、`AiUsageReportItemReceipt`、`AiUsageReportReceipt`、`AiOutcome`、`AiUsageReportStatus`、`AiVersionHead`、`AiSkillDraftUpload`、`AiRevisionResult`、`AiSkillPublishRequest`、`AiPublicationReceipt`
- Repository evidence: 既有文件 `package egon.tianshu.v1; java_package ...contract.proto.v1; java_multiple_files=true`。
- Dependencies and consumers: `protobuf-maven-plugin` 生成；`DdcAiResourceRpc`、`DdcAiProtoConverter`、Admin Provider。
- Why now: 生成类是契约接口与测试的编译前置。
- Contract/signature changes: 只追加；既有字段号不变；枚举值带前缀避免 proto3 包级冲突。
- Input/output and state mapping: 字段号按主 Spec §9.2.7–§9.2.10。
- Error and edge behavior: `AiUsageReport.explicit_score/input_excerpt/output_excerpt/latency_ms/trace_id` 为 optional；`AiVersionHead.version_id/version_no/content_sha256` optional。
- Standards impact: MC-SCOPE-001（追加）。
- Literal rule enforcement: Rule 11（proto 在 adapter 技术模块）。
- Implementation pseudocode:

```text
service DdcAiResourceService { ...GetPrompt; GetSkill;
  rpc ReportUsage(AiUsageReportBatch) returns (AiUsageReportReceipt);
  rpc GetVersionHead(AiResourceQuery) returns (AiVersionHead);
  rpc ReplaceSkillDraft(AiSkillDraftUpload) returns (AiRevisionResult);
  rpc PublishSkillDraft(AiSkillPublishRequest) returns (AiPublicationReceipt); }
enum AiOutcome { AI_OUTCOME_UNSPECIFIED=0; AI_OUTCOME_SUCCESS=1; AI_OUTCOME_FAILURE=2; AI_OUTCOME_UNKNOWN=3; }
enum AiUsageReportStatus { AI_USAGE_REPORT_STATUS_UNSPECIFIED=0; AI_USAGE_REPORT_ACCEPTED=1; AI_USAGE_REPORT_ACCEPTED_DUPLICATE=2; AI_USAGE_REPORT_REJECTED=3; }
message AiUsageReport { string usage_id=1; string namespace_code=2; string resource_key=3; string resource_id=4; string version_id=5; int64 version_no=6; AiOutcome outcome=7; optional int32 explicit_score=8; optional string input_excerpt=9; optional string output_excerpt=10; optional int32 latency_ms=11; string occurred_at=12; optional string trace_id=13; }
message AiUsageReportBatch { repeated AiUsageReport reports=1; }
message AiUsageReportItemReceipt { string usage_id=1; AiUsageReportStatus status=2; optional string reason=3; }
message AiUsageReportReceipt { repeated AiUsageReportItemReceipt items=1; }
message AiVersionHead { string resource_id=1; string resource_key=2; optional string version_id=3; optional int64 version_no=4; optional string content_sha256=5; string resource_revision=6; bool published=7; }
message AiSkillDraftUpload { string namespace_code=1; string resource_key=2; string resource_id=3; string expected_revision=4; string display_name=5; string description=6; repeated AiNode files=7; }
message AiRevisionResult { string resource_id=1; string resource_revision=2; string draft_sha256=3; bool draft_matches_current=4; }
message AiSkillPublishRequest { string namespace_code=1; string resource_key=2; string resource_id=3; string expected_revision=4; string request_id=5; string change_note=6; }
message AiPublicationReceipt { string publication_id=1; string resource_id=2; string target_version_id=3; int64 target_version_no=4; string resource_revision=5; string content_sha256=6; }
```

- Verification contribution: `DdcRpcGeneratedContractTest`（既有生成合同测试）与 File 1。
- After this file: 生成类可用；`DdcAiResourceRpc` 尚未声明新方法。

#### File 3 — `MODIFY egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-tianshu-adapter/src/main/java/top/egon/cola/component/rpc/tianshu/security/DdcRpcOperation.java`

- Purpose: 新增三个操作常量。
- Symbols: `AI_REPORT(20, "AI_REPORT")`、`AI_WRITE(21, "AI_WRITE")`、`AI_PUBLISH(22, "AI_PUBLISH")`
- Repository evidence: `AI_READ(19, "AI_READ")` 为末尾；`EgonEnum` code/message。
- Dependencies and consumers: resolver、Admin 拦截器 `write()`（default→true）、凭据 `allowed-operations` 字符串。
- Why now: resolver 映射前置。
- Contract/signature changes: 追加常量；`management()` 仍按前缀判断。
- Input/output and state mapping: 名称即配置串。
- Error and edge behavior: 无。
- Standards impact: MC-NAME-001。
- Literal rule enforcement: Rule 6（不用 ordinal，EgonEnum code 显式）、Rule 11。
- Implementation pseudocode:

```java
MANAGEMENT_CATALOG_READ(18, "MANAGEMENT_CATALOG_READ"),
AI_READ(19, "AI_READ"),
AI_REPORT(20, "AI_REPORT"),
AI_WRITE(21, "AI_WRITE"),
AI_PUBLISH(22, "AI_PUBLISH");
```

- Verification contribution: File 1 `newAiOperationsResolveToDedicatedPermissions`。
- After this file: 编译通过。

#### File 4 — `MODIFY egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-tianshu-adapter/src/main/java/top/egon/cola/component/rpc/tianshu/security/DdcRpcOperationResolver.java`

- Purpose: 登记 4 个方法到操作。
- Symbols: `operations()` 四行 `values.put(AI_RESOURCE + ...)`
- Repository evidence: `operations()` 末尾两行 `AI_RESOURCE + "GetPrompt"/"GetSkill"`。
- Dependencies and consumers: Admin `DdcRpcServerInterceptor`（解析 fail-closed）。
- Why now: 未登记方法被拒绝。
- Contract/signature changes: 无签名变化。
- Input/output and state mapping: 方法名→操作。
- Error and edge behavior: 未知方法仍抛 `IllegalArgumentException`。
- Standards impact: MC-SCOPE-001。
- Literal rule enforcement: Rule 11。
- Implementation pseudocode:

```java
values.put(AI_RESOURCE + "ReportUsage", DdcRpcOperation.AI_REPORT);
values.put(AI_RESOURCE + "GetVersionHead", DdcRpcOperation.AI_READ);
values.put(AI_RESOURCE + "ReplaceSkillDraft", DdcRpcOperation.AI_WRITE);
values.put(AI_RESOURCE + "PublishSkillDraft", DdcRpcOperation.AI_PUBLISH);
```

- Verification contribution: File 1。
- After this file: resolver 用例 GREEN。

#### File 5 — `MODIFY egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-tianshu-adapter/src/main/java/top/egon/cola/component/rpc/tianshu/contract/DdcAiResourceRpc.java`

- Purpose: 契约接口追加 4 个方法。
- Symbols: `reportUsage(AiUsageReportBatch)`、`getVersionHead(AiResourceQuery)`、`replaceSkillDraft(AiSkillDraftUpload)`、`publishSkillDraft(AiSkillPublishRequest)`
- Repository evidence: `@EgonRpcService(grpcClass=DdcAiResourceServiceGrpc.class, group="tianshu", version="1.0.0")` 与 `@EgonRpcMethod(name, idempotent)`。
- Dependencies and consumers: Provider 实现（Step 6/13）、客户端 direct proxy。
- Why now: 客户端实现前置。
- Contract/signature changes: 追加方法；`idempotent=true` 仅 `GetVersionHead`。
- Input/output and state mapping: proto 消息。
- Error and edge behavior: 无。
- Standards impact: MC-NAME-001。
- Literal rule enforcement: Rule 11。
- Implementation pseudocode:

```java
@EgonRpcMethod(name = "ReportUsage", idempotent = false) AiUsageReportReceipt reportUsage(AiUsageReportBatch batch);
@EgonRpcMethod(name = "GetVersionHead", idempotent = true) AiVersionHead getVersionHead(AiResourceQuery query);
@EgonRpcMethod(name = "ReplaceSkillDraft", idempotent = false) AiRevisionResult replaceSkillDraft(AiSkillDraftUpload upload);
@EgonRpcMethod(name = "PublishSkillDraft", idempotent = false) AiPublicationReceipt publishSkillDraft(AiSkillPublishRequest request);
```

- Verification contribution: File 1 描述符/注解断言。
- After this file: File 1 GREEN；`DdcRpcContractDescriptorTest` 需仍通过。

#### File 6 — `MODIFY egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-tianshu-adapter/src/main/java/top/egon/cola/component/rpc/tianshu/client/ai/DdcAiProtoConverter.java`

- Purpose: SDK DTO ↔ 新 proto 消息映射。
- Symbols: `AiUsageReportBatch toProto(DdcAiUsageReportBatchDTO, scope)`（scope 由 `DdcProperties` 提供 biz/env/app——proto 请求不含 scope，仅 namespace/key；故签名 `toProto(DdcAiUsageReportBatchDTO)`）、`DdcAiUsageReportReceiptDTO fromReceipt(AiUsageReportReceipt)`、`DdcAiVersionHeadDTO fromVersionHead(AiVersionHead)`、`AiSkillDraftUpload toProto(DdcAiSkillDraftUploadDTO)`、`DdcAiRevisionDTO fromRevision(AiRevisionResult)`、`AiSkillPublishRequest toProto(DdcAiSkillPublishDTO)`、`DdcAiPublicationReceiptDTO fromPublicationReceipt(AiPublicationReceipt)`；`@ValueMapping` 枚举 `outcomeToProto/outcomeFromProto/statusFromProto`；`instantToRfc3339/rfc3339ToInstant`
- Repository evidence: 既有 `@Mapper(componentModel="spring", unmappedTargetPolicy=ERROR)`、`positiveLong/positiveString/nodeTypeToProto` 写法。
- Dependencies and consumers: 两个新客户端（Admin Provider 侧用自己的 `DdcAiSyncCommandConverter`，不复用本 Mapper）。
- Why now: 客户端实现前置。
- Contract/signature changes: 追加方法；`BaseForwardConverter<DdcAiQueryDTO, AiResourceQuery>` 父接口不变。
- Input/output and state mapping: Long↔十进制字符串；`occurredAt`→`DateTimeFormatter.ISO_INSTANT`；optional 字段 `has*()` 判断；枚举 ANY_REMAINING→THROW。
- Error and edge behavior: 非法 sha256/ID 抛 `IllegalArgumentException`（客户端映射为 `TIANSHU_INVALID_REQUEST`）。
- Standards impact: MC-CONVERT-001（MapStruct + BaseForwardConverter）、MC-TIME-001、MC-JSON-001。
- Literal rule enforcement: Rule 3（MapStruct 转换，无手工 BeanUtils）、Rule 10（Instant）、Rule 11。
- Implementation pseudocode:

```java
@BeanMapping(ignoreByDefault = true, nullValueCheckStrategy = ALWAYS)
@Mapping(target = "usageId", source = "usageId") @Mapping(target = "namespaceCode", source = "namespaceCode") @Mapping(target = "resourceKey", source = "resourceKey")
@Mapping(target = "resourceId", source = "resourceId", qualifiedByName = "positiveString") @Mapping(target = "versionId", source = "versionId", qualifiedByName = "positiveString")
@Mapping(target = "versionNo", source = "versionNo", qualifiedByName = "positiveVersion") @Mapping(target = "outcome", source = "outcome", qualifiedByName = "outcomeToProto")
@Mapping(target = "occurredAt", source = "occurredAt", qualifiedByName = "instantToRfc3339") /* optional 字段在 default 方法里按 null 判断 setXxx */
AiUsageReport mapReport(DdcAiUsageReportDTO source);
default AiUsageReportBatch toProto(DdcAiUsageReportBatchDTO batch) { builder.addAllReports(batch.getReports().stream().map(this::toProtoWithOptionals).toList()) }
@Named("instantToRfc3339") default String instantToRfc3339(Instant v) { return DateTimeFormatter.ISO_INSTANT.format(v); }
@ValueMapping(source="FAILURE", target="AI_OUTCOME_FAILURE") /* SUCCESS/UNKNOWN 同；ANY_REMAINING→THROW */ AiOutcome outcomeToProto(DdcAiOutcomeEnum s);
```

- Verification contribution: 新建 `DdcAiProtoConverterTest`（本步 File 11 旁）由 File 7/8 测试覆盖；`DdcRpcGeneratedContractTest`。
- After this file: 编译通过；MapStruct 生成 Impl。

#### File 7 — `CREATE egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-tianshu-adapter/src/main/java/top/egon/cola/component/rpc/tianshu/client/ai/RpcDdcAiFeedbackClient.java`

- Purpose: `DdcAiFeedbackClient` 的 Direct RPC 实现。
- Symbols: `public final class RpcDdcAiFeedbackClient implements DdcAiFeedbackClient`；构造 `(DdcAiResourceRpc rpc, DdcAiProtoConverter converter, DdcRpcStatusExceptionMapper errors)`；`report(batch)`
- Repository evidence: `RpcDdcPromptClient`（`@Validated`、`invoke` 包装 `errors.restore(failure, DdcRpcOperation.AI_READ)`）。
- Dependencies and consumers: `DdcRpcClientFactory#feedbackClient()` 创建；`DefaultDdcAiUsageReporter` 调用。
- Why now: 工厂方法前置。
- Contract/signature changes: 新类；非 Spring Bean（由 factory 构造，沿既有）。
- Input/output and state mapping: DTO→proto→RPC→proto→DTO。
- Error and edge behavior: `errors.restore(failure, DdcRpcOperation.AI_REPORT)`；`UNIMPLEMENTED` 由既有 mapper 转为 `DdcClientTransportException`。
- Standards impact: MC-NAME-001, MC-VALID-001（`@Validated` + `@Valid`）、MC-CONVERT-001。
- Literal rule enforcement: Rule 2（Port 入参校验）、Rule 1、Rule 11。
- Implementation pseudocode:

```java
@Validated public final class RpcDdcAiFeedbackClient implements DdcAiFeedbackClient {
    @Override public DdcAiUsageReportReceiptDTO report(@NotNull @Valid DdcAiUsageReportBatchDTO batch) {
        AiUsageReportBatch request = converter.toProto(batch);
        AiUsageReportReceipt response = invoke(() -> rpc.reportUsage(request));   // catch RuntimeException → errors.restore(failure, AI_REPORT)
        DdcAiUsageReportReceiptDTO receipt = converter.fromReceipt(response);
        if (receipt.getItems().size() != batch.getReports().size()) throw new IllegalStateException("AI_REPORT_RECEIPT_SIZE_MISMATCH");
        return receipt;
    }
}
```

- Verification contribution: 新建 `RpcDdcAiFeedbackClientTest`（Mockito `DdcAiResourceRpc`）——本步 File 11 之前列为 File 9 的测试？见 File 11 说明：测试放在 `DdcRpcAutoConfigurationTest` 与 `DdcAiRpcContractTest`；客户端行为由 `RpcDdcAiSyncClientTest`/`RpcDdcAiFeedbackClientTest` 覆盖（本步 File 11 创建）。
- After this file: 编译通过。

#### File 8 — `CREATE egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-tianshu-adapter/src/main/java/top/egon/cola/component/rpc/tianshu/client/ai/RpcDdcAiSyncClient.java`

- Purpose: `DdcAiSyncClient` 的 Direct RPC 实现。
- Symbols: `getVersionHead(query)`、`replaceSkillDraft(upload)`、`publishSkillDraft(publish)`；构造 `(DdcAiResourceRpc, DdcAiProtoConverter, DdcRpcStatusExceptionMapper, DdcProperties)`
- Repository evidence: `RpcDdcSkillClient`（scope 从 `DdcProperties` 派生 `DdcAiQueryDTO`）。
- Dependencies and consumers: factory；workspace/authoring。
- Why now: 工厂前置。
- Contract/signature changes: 新类。
- Input/output and state mapping: `getVersionHead`：`DdcAiQueryDTO(bizCode, namespaceCode, env, appCode, resourceKey, null, resourceId)`→`AiResourceQuery`（不设 versionNo）；上传/发布：DTO→proto（scope 不入 proto，服务端按凭据）。
- Error and edge behavior: `query.versionNo()!=null`→`IllegalArgumentException`；错误恢复分别用 `AI_READ/AI_WRITE/AI_PUBLISH`；deadline 30s 由 `callOptions` 覆盖（既有 `RpcDirectClientHandle` 支持方法级 deadline 时使用，否则沿 `defaultTimeout`，作为 PLAN-CLAR-006 同类实现细节）。
- Standards impact: MC-NAME-001, MC-VALID-001, MC-CONVERT-001。
- Literal rule enforcement: Rule 2, Rule 1, Rule 11。
- Implementation pseudocode:

```java
@Override public DdcAiVersionHeadDTO getVersionHead(@NotNull @Valid DdcAiResourceQuery query) {
    if (query.versionNo() != null) throw new IllegalArgumentException("versionNo must be absent for version head");
    AiResourceQuery request = converter.toQuery(new DdcAiQueryDTO(properties.getBizCode(), query.namespaceCode(), properties.getEnv(), properties.getAppCode(), query.resourceKey(), null, query.resourceId()));
    return converter.fromVersionHead(invoke(() -> rpc.getVersionHead(request), DdcRpcOperation.AI_READ));
}
@Override public DdcAiRevisionDTO replaceSkillDraft(@NotNull @Valid DdcAiSkillDraftUploadDTO upload) { return converter.fromRevision(invoke(() -> rpc.replaceSkillDraft(converter.toProto(upload)), DdcRpcOperation.AI_WRITE)); }
@Override public DdcAiPublicationReceiptDTO publishSkillDraft(@NotNull @Valid DdcAiSkillPublishDTO publish) { return converter.fromPublicationReceipt(invoke(() -> rpc.publishSkillDraft(converter.toProto(publish)), DdcRpcOperation.AI_PUBLISH)); }
```

- Verification contribution: File 11 测试。
- After this file: 编译通过。

#### File 9 — `MODIFY egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-tianshu-adapter/src/main/java/top/egon/cola/component/rpc/tianshu/client/DdcRpcClientFactory.java`

- Purpose: 工厂新增两个 handle。
- Symbols: `DdcRpcClientHandle<DdcAiFeedbackClient> feedbackClient()`、`DdcRpcClientHandle<DdcAiSyncClient> syncClient()`
- Repository evidence: `promptClient()`（`create(DdcAiResourceRpc.class, () -> rpcProperties.runtimeCredential())`，失败 `direct.close()`）。
- Dependencies and consumers: `DdcRpcAutoConfiguration`。
- Why now: 装配前置。
- Contract/signature changes: 追加两方法。
- Input/output and state mapping: 同 `promptClient`。
- Error and edge behavior: 创建失败关闭 direct handle。
- Standards impact: MC-REUSE-001。
- Literal rule enforcement: Rule 11。
- Implementation pseudocode:

```java
public DdcRpcClientHandle<DdcAiFeedbackClient> feedbackClient() {
    RpcDirectClientHandle<DdcAiResourceRpc> direct = create(DdcAiResourceRpc.class, () -> rpcProperties.runtimeCredential());
    try { return new DdcRpcClientHandle<>(new RpcDdcAiFeedbackClient(direct.client(), aiProtoConverter, new DdcRpcStatusExceptionMapper()), direct); }
    catch (RuntimeException failure) { direct.close(); throw failure; }
}
public DdcRpcClientHandle<DdcAiSyncClient> syncClient() { /* 同上，构造 RpcDdcAiSyncClient(direct.client(), aiProtoConverter, mapper, ddcProperties) */ }
```

- Verification contribution: `DdcRpcClientFactoryTest` 既有用例不变；File 11 装配测试。
- After this file: 编译通过。

#### File 10 — `MODIFY egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-tianshu-adapter/src/main/java/top/egon/cola/component/rpc/tianshu/autoconfigure/DdcRpcAutoConfiguration.java`

- Purpose: 条件装配两个新 Port。
- Symbols: `ddcAiFeedbackRpcClientHandle`（`@ConditionalOnProperty(prefix="egon.cola.component.tianshu.ai.feedback", name="enabled", havingValue="true")`，`@ConditionalOnMissingBean(DdcAiFeedbackClient.class)`，`destroyMethod="close"`）、`ddcAiFeedbackRpcClient`；`ddcAiSyncRpcClientHandle`（`@ConditionalOnExpression("${egon.cola.component.tianshu.ai.skill.workspace.enabled:false} || ${egon.cola.component.tianshu.ai.skill.authoring.enabled:false}")`，`@ConditionalOnMissingBean(DdcAiSyncClient.class)`）、`ddcAiSyncRpcClient`
- Repository evidence: `ddcAiPromptRpcClientHandle/ddcAiPromptRpcClient` 对（行 153–170）。
- Dependencies and consumers: ai-starter Reporter/Workspace/Authoring。
- Why now: 开关真值矩阵。
- Contract/signature changes: 新 Bean 方法；既有不变。
- Input/output and state mapping: 属性→Bean。
- Error and edge behavior: 关闭时无 Bean、无连接。
- Standards impact: MC-BEAN-001（具名 Bean）、MC-CONFIG-001。
- Literal rule enforcement: Rule 4（`@Bean` 具名）、Rule 7（键与 ai-starter 属性一致）、Rule 11。
- Implementation pseudocode:

```java
@Bean(name = "ddcAiFeedbackRpcClientHandle", destroyMethod = "close")
@ConditionalOnProperty(prefix = "egon.cola.component.tianshu.ai.feedback", name = "enabled", havingValue = "true")
@ConditionalOnMissingBean(DdcAiFeedbackClient.class)
public DdcRpcClientHandle<DdcAiFeedbackClient> ddcAiFeedbackRpcClientHandle(DdcRpcClientFactory factory) { return factory.feedbackClient(); }
@Bean("ddcAiFeedbackRpcClient") @ConditionalOnBean(name = "ddcAiFeedbackRpcClientHandle") @ConditionalOnMissingBean(DdcAiFeedbackClient.class)
public DdcAiFeedbackClient ddcAiFeedbackRpcClient(@Qualifier("ddcAiFeedbackRpcClientHandle") DdcRpcClientHandle<DdcAiFeedbackClient> handle) { return handle.client(); }
/* ddcAiSyncRpcClientHandle / ddcAiSyncRpcClient 同构，条件为 workspace.enabled || authoring.enabled */
```

- Verification contribution: File 11。
- After this file: 装配就绪。

#### File 11 — `MODIFY egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-tianshu-adapter/src/test/java/top/egon/cola/component/rpc/tianshu/autoconfigure/DdcRpcAutoConfigurationTest.java`

- Purpose: 证明开关真值与客户端行为（PLAN-CLAR-006：在既有装配测试类追加，并在同目录新增 `RpcDdcAiSyncClientTest`、`RpcDdcAiFeedbackClientTest` 作为本文件描述的伴随测试，一并计入 Commit paths）。
- Symbols: `aiFeedbackClientAbsentByDefault`、`aiFeedbackClientPresentWhenEnabled`、`aiSyncClientPresentWhenWorkspaceOrAuthoringEnabled`；伴随类：`reportMapsBatchAndRestoresStatus`、`getVersionHeadRejectsVersionNo`、`replaceSkillDraftRestoresAbortedAsRevisionConflict`
- Repository evidence: 既有 `DdcRpcAutoConfigurationTest` 用 `ApplicationContextRunner` 断言 `ddcAiPromptRpcClient` 条件；`RpcDdcConfigClientTest` Mockito 写法。
- Dependencies and consumers: File 7–10。
- Why now: 本步 GREEN 门禁。
- Contract/signature changes: 无。
- Input/output and state mapping: 属性组合→Bean 存在性；假 `DdcAiResourceRpc` 返回 proto→DTO 断言。
- Error and edge behavior: `StatusRuntimeException(ABORTED, trailer TIANSHU_AI_REVISION_CONFLICT)`→`DdcException` 状态串断言。
- Standards impact: MC-TEST-001, MC-CONFIG-001。
- Literal rule enforcement: Rule 7（开关键）、Rule 11。
- Implementation pseudocode:

```java
@Test void aiSyncClientPresentWhenWorkspaceOrAuthoringEnabled() {
    runner.withPropertyValues(BASE_PROPS, "egon.cola.component.tianshu.ai.skill.workspace.enabled=true")
          .run(ctx -> assertThat(ctx).hasBean("ddcAiSyncRpcClient").getBean(DdcAiSyncClient.class).isInstanceOf(RpcDdcAiSyncClient.class));
    runner.withPropertyValues(BASE_PROPS).run(ctx -> assertThat(ctx).doesNotHaveBean(DdcAiSyncClient.class).doesNotHaveBean(DdcAiFeedbackClient.class));
}
// RpcDdcAiFeedbackClientTest: when(rpc.reportUsage(any())).thenReturn(receipt(ACCEPTED, ACCEPTED_DUPLICATE)); assertThat(client.report(batch(2)).getItems()).extracting(status).containsExactly(ACCEPTED, ACCEPTED_DUPLICATE);
```

- Verification contribution: 本步 GREEN。
- After this file: adapter 模块测试全绿。

- Validation working directory: 仓库根
- Verification command: `./mvnw -pl egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-tianshu-adapter -Dtest=DdcAiRpcContractTest,DdcRpcAutoConfigurationTest,RpcDdcAiFeedbackClientTest,RpcDdcAiSyncClientTest,DdcRpcGeneratedContractTest,DdcRpcContractDescriptorTest,DdcRpcClientFactoryTest -Dsurefire.failIfNoSpecifiedTests=false test && ./mvnw -q -pl egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-tianshu-adapter -DskipTests install`
- Expected result: 7 个测试类通过；proto 生成无 warning 升级为 error；本地安装成功。
- Failure returns to: File 2（proto 字段/枚举）、File 6（映射）、File 10（条件）。
- Completion criteria: 4 方法、3 操作、2 客户端、2 条件装配均有测试证据。
- Rollback: 回退本步 11 路径（含两个伴随测试文件）。
- Commit paths: egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-tianshu-adapter/src/test/java/top/egon/cola/component/rpc/tianshu/client/ai/DdcAiRpcContractTest.java, egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-tianshu-adapter/src/main/proto/tianshu_ai_resource.proto, egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-tianshu-adapter/src/main/java/top/egon/cola/component/rpc/tianshu/security/DdcRpcOperation.java, egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-tianshu-adapter/src/main/java/top/egon/cola/component/rpc/tianshu/security/DdcRpcOperationResolver.java, egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-tianshu-adapter/src/main/java/top/egon/cola/component/rpc/tianshu/contract/DdcAiResourceRpc.java, egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-tianshu-adapter/src/main/java/top/egon/cola/component/rpc/tianshu/client/ai/DdcAiProtoConverter.java, egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-tianshu-adapter/src/main/java/top/egon/cola/component/rpc/tianshu/client/ai/RpcDdcAiFeedbackClient.java, egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-tianshu-adapter/src/main/java/top/egon/cola/component/rpc/tianshu/client/ai/RpcDdcAiSyncClient.java, egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-tianshu-adapter/src/main/java/top/egon/cola/component/rpc/tianshu/client/DdcRpcClientFactory.java, egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-tianshu-adapter/src/main/java/top/egon/cola/component/rpc/tianshu/autoconfigure/DdcRpcAutoConfiguration.java, egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-tianshu-adapter/src/test/java/top/egon/cola/component/rpc/tianshu/autoconfigure/DdcRpcAutoConfigurationTest.java, egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-tianshu-adapter/src/test/java/top/egon/cola/component/rpc/tianshu/client/ai/RpcDdcAiFeedbackClientTest.java, egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-tianshu-adapter/src/test/java/top/egon/cola/component/rpc/tianshu/client/ai/RpcDdcAiSyncClientTest.java
- Commit: `feat(tianshu): ai rpc feedback and sync contract with starter clients`

### Step 3 — DDL-201、受管清单与持久化模型

- Requirements: `REQ-002`, `REQ-004`, `REQ-005`, `REQ-011`, `REQ-014`
- Dependencies: Step 1（base starter 安装）
- Baseline state: `AR/db/egon-mp/` 两个版本脚本与 manifest 两条目；`egon-tianshu-persistence.yml` 登记 18 张逻辑表；`AJ/model/po` 有 `DdcAiResourcePO/DdcAiVersionPO/DdcAiNodePO/DdcAiPublicationPO`；`DdcAiEnumConverter` 处理既有 4 个枚举。
- Observable outcome: 新 SQL 与 manifest 条目通过 `DdcAiOptimizationSchemaScriptTest`（脚本可解析、CHECK/索引/外键全部出现、SHA-256 与 manifest 一致）；三张逻辑表登记 1×1 拓扑；6 枚举、3 PO、4 BO、3 BaseConverter 与枚举转换通过 `DdcAiOptimizationConverterTest`。
- End state: 持久化模型可编译，无 Mapper/Repository；`DdcTianshuDdlIT` 在 Failsafe profile 下会校验三表存在（本步仅修改 IT，运行留给 Step 11 门禁）。
- Test-first gate: Required — `DdcAiOptimizationSchemaScriptTest` 因文件不存在 RED；`DdcAiOptimizationConverterTest` 因类缺失编译 RED。
- Manual Checks: MC-ARCH-001, MC-REUSE-001, MC-NAME-001, MC-MODEL-001, MC-CONVERT-001, MC-JSON-001, MC-TIME-001, MC-SCOPE-001, MC-TEST-001
- Literal Rules: Rule 1, Rule 3, Rule 6, Rule 10, Rule 11
- Ordered files:

#### File 1 — `CREATE egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/test/java/top/egon/cola/component/tianshu/admin/repository/DdcAiOptimizationSchemaScriptTest.java`

- Purpose: 以静态方式固定 DDL-201 的表、约束、索引、外键与 manifest 摘要。
- Symbols: `scriptCreatesThreeTablesWithConstraints`、`manifestListsScriptWithMatchingSha256`、`persistenceYamlRegistersThreeLogicalTables`
- Repository evidence: `AT/repository/DdcSchemaScriptTest.java` 读取 `db/egon-mp/V20260922_001__tianshu_platform.sql` 并断言 `CREATE TABLE`/`CHECK`；manifest JSON 结构 `{"scripts":[{"version","file","sha256"}]}`（以实际文件为准）。
- Dependencies and consumers: File 2/3/4。
- Why now: RED 契约。
- Contract/signature changes: 无（测试）。
- Input/output and state mapping: classpath 资源→字符串断言。
- Error and edge behavior: 断言 `judge_attempts BETWEEN 0 AND 10`、`UNIQUE (tenant_id, usage_id)`、部分唯一索引 `ux_ai_optimization_run_active`（`WHERE state IN ('PENDING','RUNNING') AND deleted_at IS NULL`）与 `ux_ai_optimization_run_request`（`WHERE request_id IS NOT NULL AND deleted_at IS NULL`）、策略 lifecycle/live 两条唯一索引。
- Standards impact: MC-TEST-001, MC-SCOPE-001。
- Literal rule enforcement: Rule 11（DDL 由 MP-SDJ 受管，测试校验清单）。
- Implementation pseudocode:

```java
@Test void scriptCreatesThreeTablesWithConstraints() throws IOException {
    String sql = read("db/egon-mp/V20261017_001__tianshu_ai_feedback_optimization.sql");
    assertThat(sql).contains("CREATE TABLE IF NOT EXISTS ddc_ai_usage_feedback_t0", "CREATE TABLE IF NOT EXISTS ddc_ai_optimization_policy_t0", "CREATE TABLE IF NOT EXISTS ddc_ai_optimization_run_t0")
        .contains("CHECK (judge_attempts BETWEEN 0 AND 10)", "CONSTRAINT uk_ai_usage_feedback_usage UNIQUE (tenant_id, usage_id)")
        .contains("CREATE UNIQUE INDEX IF NOT EXISTS ux_ai_optimization_run_active", "WHERE state IN ('PENDING','RUNNING') AND deleted_at IS NULL")
        .contains("ux_ai_optimization_policy_lifecycle", "ux_ai_optimization_policy_live");
}
@Test void manifestListsScriptWithMatchingSha256() { assertThat(manifestEntry("V20261017_001__tianshu_ai_feedback_optimization.sql").sha256()).isEqualTo(DigestUtils.sha256Hex(bytes(sql))); }
@Test void persistenceYamlRegistersThreeLogicalTables() { assertThat(yamlLogicTables()).contains("ddc_ai_usage_feedback", "ddc_ai_optimization_policy", "ddc_ai_optimization_run"); }
```

- Verification contribution: RED→GREEN。
- After this file: RED。

#### File 2 — `CREATE egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/resources/db/egon-mp/V20261017_001__tianshu_ai_feedback_optimization.sql`

- Purpose: DDL-201，三张物理表、约束、索引、外键、注释。
- Symbols: `ddc_ai_usage_feedback_t0`、`ddc_ai_optimization_policy_t0`、`ddc_ai_optimization_run_t0`；索引 `ix_ai_usage_feedback_judge_claim`、`ix_ai_usage_feedback_window`、`ix_ai_usage_feedback_retention`、`ux_ai_optimization_policy_lifecycle`、`ux_ai_optimization_policy_live`、`ix_ai_optimization_policy_scan`、`ux_ai_optimization_run_no`、`ux_ai_optimization_run_active`、`ux_ai_optimization_run_request`、`ix_ai_optimization_run_page`、`ix_ai_optimization_run_lease`
- Repository evidence: `V20260922_001__tianshu_platform.sql` 的 `ddc_ai_publication_t0` 段（技术八字段、`CHECK (id > 0)`、`timestamp(6) without time zone` 的 `deleted_at`、复合外键 `(tenant_id, resource_id)`）。
- Dependencies and consumers: manifest（File 3）、`DdcTianshuDdlIT`、三个 PO。
- Why now: 持久化基础。
- Contract/signature changes: 只新增表；不改既有脚本。
- Input/output and state mapping: 列定义逐项对应主 Spec §11.2.1–§11.2.3。
- Error and edge behavior: 全部 `IF NOT EXISTS` 幂等；外键 `ON DELETE RESTRICT`；`deleted_at` 保持既有 Legacy UTC 约定；policy 增加 `converged_at timestamptz(6)` 与 `active_run_id bigint`（§10.3 字段列出，§11.2.2 表头“技术八字段/应用维护”同款列由本脚本写出）。
- Standards impact: MC-TIME-001（`timestamptz(6)` 存 UTC）、MC-SCOPE-001。
- Literal rule enforcement: Rule 11（业务唯一键组合 `deleted_at`：policy 的 `ux_ai_optimization_policy_lifecycle (tenant_id, resource_id, deleted_at)` + `ux_ai_optimization_policy_live (tenant_id, resource_id) WHERE deleted_at IS NULL`；feedback 的 `usage_id` 为永久技术身份按 Spec 用普通 UNIQUE）、Rule 10。
- Implementation pseudocode:

```sql
CREATE TABLE IF NOT EXISTS ddc_ai_usage_feedback_t0 (
  id bigint NOT NULL, tenant_id bigint NOT NULL, create_user_id varchar(512) NOT NULL, create_time timestamptz(6) NOT NULL,
  update_user_id varchar(512) NOT NULL, update_time timestamptz(6) NOT NULL, deleted_at timestamp(6) without time zone NULL, version bigint NOT NULL DEFAULT 0,
  resource_id bigint NOT NULL, version_id bigint NOT NULL, usage_id varchar(64) NOT NULL, reporter_credential_id varchar(128) NOT NULL,
  biz_code varchar(128) NOT NULL, env_code varchar(32) NOT NULL, app_code varchar(128) NOT NULL, outcome varchar(16) NOT NULL,
  explicit_score smallint NULL, judge_score smallint NULL, judge_state varchar(16) NOT NULL, judge_attempts smallint NOT NULL DEFAULT 0,
  judge_lease_until timestamptz(6) NULL, judged_at timestamptz(6) NULL, judge_error varchar(64) NULL,
  input_excerpt text NULL, output_excerpt text NULL, latency_ms integer NULL, occurred_at timestamptz(6) NOT NULL, trace_id varchar(64) NULL,
  CONSTRAINT pk_ai_usage_feedback PRIMARY KEY (id), CONSTRAINT uk_ai_usage_feedback_tenant_id UNIQUE (tenant_id, id),
  CONSTRAINT uk_ai_usage_feedback_usage UNIQUE (tenant_id, usage_id),
  CONSTRAINT ck_ai_usage_feedback_usage_id CHECK (usage_id ~ '^[A-Za-z0-9_-]{16,64}$'),
  CONSTRAINT ck_ai_usage_feedback_outcome CHECK (outcome IN ('SUCCESS','FAILURE','UNKNOWN')),
  CONSTRAINT ck_ai_usage_feedback_judge_state CHECK (judge_state IN ('NOT_REQUIRED','PENDING','JUDGING','DONE','FAILED')),
  CONSTRAINT ck_ai_usage_feedback_judge_done CHECK ((judge_state = 'DONE') = (judge_score IS NOT NULL)),
  CONSTRAINT ck_ai_usage_feedback_judge_attempts CHECK (judge_attempts BETWEEN 0 AND 10),
  CONSTRAINT ck_ai_usage_feedback_scores CHECK ((explicit_score IS NULL OR explicit_score BETWEEN 0 AND 100) AND (judge_score IS NULL OR judge_score BETWEEN 0 AND 100)),
  CONSTRAINT ck_ai_usage_feedback_excerpt_bytes CHECK (octet_length(coalesce(input_excerpt,'')) <= 8192 AND octet_length(coalesce(output_excerpt,'')) <= 32768),
  CONSTRAINT fk_ai_usage_feedback_resource FOREIGN KEY (tenant_id, resource_id) REFERENCES ddc_ai_resource_t0 (tenant_id, id) ON DELETE RESTRICT,
  CONSTRAINT fk_ai_usage_feedback_version FOREIGN KEY (tenant_id, resource_id, version_id) REFERENCES ddc_ai_version_t0 (tenant_id, resource_id, id) ON DELETE RESTRICT);
CREATE INDEX IF NOT EXISTS ix_ai_usage_feedback_judge_claim ON ddc_ai_usage_feedback_t0 (tenant_id, resource_id, judge_state, judge_lease_until) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS ix_ai_usage_feedback_window ON ddc_ai_usage_feedback_t0 (tenant_id, resource_id, version_id, occurred_at) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS ix_ai_usage_feedback_retention ON ddc_ai_usage_feedback_t0 (tenant_id, occurred_at) WHERE deleted_at IS NULL;
-- ddc_ai_optimization_policy_t0：§11.2.2 全列 + converged_at timestamptz(6) NULL + active_run_id bigint NULL；
--   ux_ai_optimization_policy_lifecycle (tenant_id, resource_id, deleted_at)；ux_ai_optimization_policy_live (tenant_id, resource_id) WHERE deleted_at IS NULL；
--   ix_ai_optimization_policy_scan (tenant_id, enabled, state, last_evaluated_at) WHERE deleted_at IS NULL
-- ddc_ai_optimization_run_t0：§11.2.3 全列与 CHECK；ux_ai_optimization_run_no (tenant_id, resource_id, run_no)；
--   ux_ai_optimization_run_active (tenant_id, resource_id) WHERE state IN ('PENDING','RUNNING') AND deleted_at IS NULL；
--   ux_ai_optimization_run_request (tenant_id, resource_id, request_id) WHERE request_id IS NOT NULL AND deleted_at IS NULL；
--   ix_ai_optimization_run_page (tenant_id, resource_id, run_no DESC)；ix_ai_optimization_run_lease (tenant_id, state, lease_until) WHERE state = 'RUNNING'
```

- Verification contribution: File 1；`DdcTianshuDdlIT`（Step 11 运行）。
- After this file: 脚本未登记。

#### File 3 — `MODIFY egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/resources/db/egon-mp/repository-manifest.json`

- Purpose: 追加 DDL-201 条目与 SHA-256。
- Symbols: 新数组元素 `{"version":"20261017_001","file":"V20261017_001__tianshu_ai_feedback_optimization.sql","sha256":"实施时计算的 64 位十六进制"}` 的同构条目（字段名以现有条目为准）
- Repository evidence: manifest 现有两条（`V20260922_001`、`V20260928_001`）。
- Dependencies and consumers: MP-SDJ 运行器；File 1 测试。
- Why now: 未登记脚本不会执行。
- Contract/signature changes: 仅追加。
- Input/output and state mapping: `sha256sum` 输出→JSON。
- Error and edge behavior: 既有两条不改；摘要不匹配时运行器拒绝（测试先行捕获）。
- Standards impact: MC-SCOPE-001。
- Literal rule enforcement: Rule 11（DDL 分布式受管）。
- Implementation pseudocode:

```text
1. 实施时执行 shasum -a 256 egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/resources/db/egon-mp/V20261017_001__tianshu_ai_feedback_optimization.sql
2. 在 "scripts" 数组末尾追加与既有条目同字段的对象（version/file/sha256），保持 JSON 两空格缩进与既有顺序
3. DdcAiOptimizationSchemaScriptTest#manifestListsScriptWithMatchingSha256 重新计算并比对
```

- Verification contribution: File 1。
- After this file: manifest 一致。

#### File 4 — `MODIFY egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/resources/egon-tianshu-persistence.yml`

- Purpose: 登记三张逻辑表到 `tianshu` 分片族（1 库 × 1 表，`STANDARD_TENANT_ID`）。
- Symbols: `ddc_ai_usage_feedback`、`ddc_ai_optimization_policy`、`ddc_ai_optimization_run` 三条与 `ddc_ai_publication` 同构的条目
- Repository evidence: 文件中 `ddc_ai_publication` 条目（actual-data-nodes `ds0.ddc_ai_publication_t0`、`STANDARD_TENANT_ID`）。
- Dependencies and consumers: MP sharding starter、PO 的 `@TableName`。
- Why now: PO 查询路由。
- Contract/signature changes: 无。
- Input/output and state mapping: 逻辑表→物理表。
- Error and edge behavior: 漏登记时 ShardingSphere 报未知表（File 1 第三用例先行捕获）。
- Standards impact: MC-CONFIG-001, MC-SCOPE-001。
- Literal rule enforcement: Rule 11（MP Starter）。
- Implementation pseudocode:

```yaml
ddc_ai_usage_feedback:
  actual-data-nodes: ds0.ddc_ai_usage_feedback_t0
  table-strategy: { standard: { sharding-column: tenant_id, sharding-algorithm-name: STANDARD_TENANT_ID } }
# ddc_ai_optimization_policy / ddc_ai_optimization_run 同构（字段名与缩进以 ddc_ai_publication 条目为准）
```

- Verification contribution: File 1。
- After this file: 表登记完成。

#### File 5 — `CREATE egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/java/top/egon/cola/component/tianshu/admin/model/enums/DdcAiOutcomePersistenceEnum.java`

- Purpose: 6 个持久化枚举（本块按 PLAN-CLAR-008 合并：`DdcAiOutcomePersistenceEnum`、`DdcAiJudgeStatePersistenceEnum`、`DdcAiPolicyStatePersistenceEnum`、`DdcAiRunStatePersistenceEnum`、`DdcAiRunTriggerPersistenceEnum`、`DdcAiRunErrorCodeEnum`，同目录）。
- Symbols: 每个枚举 `@EnumValue @JsonValue private final String code;`，常量集合分别为 SUCCESS/FAILURE/UNKNOWN；NOT_REQUIRED/PENDING/JUDGING/DONE/FAILED；ARMED/AWAITING_PUBLISH/CONVERGED/EXHAUSTED/PAUSED；PENDING/RUNNING/SUCCEEDED/FAILED/SKIPPED；SCHEDULED/MANUAL；MODEL_AUTH/MODEL_TIMEOUT/MODEL_ERROR/MODEL_RATE_LIMITED/INVALID_OUTPUT/BASE_CHANGED/DRAFT_DIRTY/PUBLISH_CONFLICT/LEASE_EXPIRED/NOT_PUBLISHED；`static fromCode(String)`
- Repository evidence: `AJ/model/enums/DdcAiPublicationKindPersistenceEnum.java`（`@EnumValue` + `@JsonValue` + `fromCode`）。
- Dependencies and consumers: PO/BO/Result；`DdcAiEnumConverter`。
- Why now: PO 字段类型。
- Contract/signature changes: 新枚举。
- Input/output and state mapping: `code` 即数据库 CHECK 值与 JSON 值。
- Error and edge behavior: `fromCode` 未知值抛 `IllegalArgumentException`。
- Standards impact: MC-JSON-001, MC-NAME-001。
- Literal rule enforcement: Rule 6（`@EnumValue` 持久化、`@JsonValue` 输出、禁止 ordinal）、Rule 1、Rule 11。
- Implementation pseudocode:

```java
@Getter @RequiredArgsConstructor public enum DdcAiRunStatePersistenceEnum {
    PENDING("PENDING"), RUNNING("RUNNING"), SUCCEEDED("SUCCEEDED"), FAILED("FAILED"), SKIPPED("SKIPPED");
    @EnumValue @JsonValue private final String code;
    public boolean terminal() { return this == SUCCEEDED || this == FAILED || this == SKIPPED; }
    public static DdcAiRunStatePersistenceEnum fromCode(String code) { return Arrays.stream(values()).filter(v -> v.code.equals(code)).findFirst().orElseThrow(() -> new IllegalArgumentException("unknown run state: " + code)); }
}
// 其余 5 个枚举同构；DdcAiPolicyStatePersistenceEnum 增加 boolean evaluable() { return this == ARMED; }
```

- Verification contribution: File 10 转换测试。
- After this file: 编译通过。

#### File 6 — `CREATE egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/java/top/egon/cola/component/tianshu/admin/model/po/DdcAiUsageFeedbackPO.java`

- Purpose: 3 个 PO（合并块：`DdcAiUsageFeedbackPO`、`DdcAiOptimizationPolicyPO`、`DdcAiOptimizationRunPO`，同目录）。
- Symbols: `@TableName("ddc_ai_usage_feedback") class DdcAiUsageFeedbackPO extends EgonModel`；字段与 §11.2 列一一对应（`Short` 对 smallint、`Integer` 对 integer、`BigDecimal` 对 numeric(5,2)、`Instant` 对 timestamptz、枚举字段用持久化枚举）；policy 的 `evaluationWindowSeconds/cooldownSeconds` 为 `Integer`
- Repository evidence: `AJ/model/po/DdcAiPublicationPO.java`（`@Data @NoArgsConstructor @AllArgsConstructor @Accessors(chain=true) @SuperBuilder @EqualsAndHashCode(callSuper=true) @TableName`，`@TableField` 显式列名）。
- Dependencies and consumers: Mapper（Step 4）、Converter（File 9）。
- Why now: 模型基座。
- Contract/signature changes: 新类。
- Input/output and state mapping: 列↔字段；`@TableField("judge_lease_until")` 等显式列名。
- Error and edge behavior: 继承 `EgonModel` 的 `deletedAt/version` 不重复声明。
- Standards impact: MC-MODEL-001, MC-NAME-001, MC-TIME-001。
- Literal rule enforcement: Rule 3（class + Lombok 四注解 + `@SuperBuilder` + `callSuper=true`）、Rule 10（`Instant`）、Rule 1、Rule 11（`EgonModel`）。
- Implementation pseudocode:

```java
@Data @NoArgsConstructor @AllArgsConstructor @Accessors(chain = true) @SuperBuilder @EqualsAndHashCode(callSuper = true)
@TableName("ddc_ai_optimization_run")
public class DdcAiOptimizationRunPO extends EgonModel {
    @TableField("resource_id") private Long resourceId; @TableField("policy_id") private Long policyId; @TableField("run_no") private Long runNo;
    @TableField("iteration_no") private Short iterationNo; @TableField("trigger") private DdcAiRunTriggerPersistenceEnum trigger;
    @TableField("request_id") private String requestId; @TableField("request_sha256") private String requestSha256; @TableField("state") private DdcAiRunStatePersistenceEnum state;
    @TableField("base_version_id") private Long baseVersionId; @TableField("sample_count") private Integer sampleCount; @TableField("average_score") private BigDecimal averageScore;
    /* threshold_snapshot, window_start/end(Instant), model_alias, prompt_sha256, output_sha256, prompt_tokens, completion_tokens, produced_version_id, publication_id, auto_published, error_code(DdcAiRunErrorCodeEnum), error_message, note, lease_owner, lease_until, started_at, finished_at */
}
```

- Verification contribution: File 10。
- After this file: 编译通过。

#### File 7 — `CREATE egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/java/top/egon/cola/component/tianshu/admin/model/bo/DdcAiUsageFeedbackBO.java`

- Purpose: 4 个 BO（合并块：`DdcAiUsageFeedbackBO`、`DdcAiOptimizationPolicyBO`、`DdcAiOptimizationRunBO`、`DdcAiScoreAggregateBO`，同目录）。
- Symbols: 字段按主 Spec §10.3；policy BO 的 `evaluationWindow/cooldown` 为 `Duration`；`DdcAiScoreAggregateBO(sampleCount, averageScore, windowStart, windowEnd, worstSampleIds)` 为聚合查询结果载体；Jakarta 约束（`@NotNull`、`@Min/@Max`、`@Pattern`、`@Size`）+ `EgonColaModelValidationGroups.Persisted`
- Repository evidence: `AJ/model/bo/DdcAiPublicationBO.java`（class + Lombok + 约束）。
- Dependencies and consumers: Service；Converter。
- Why now: 领域载体。
- Contract/signature changes: 新类。
- Input/output and state mapping: PO 秒↔BO `Duration`（Converter 显式）。
- Error and edge behavior: `@Pattern(regexp="^[a-z0-9][a-z0-9-]{0,63}$")` 用于 alias；`@Pattern("^[A-Za-z0-9_-]{16,64}$")` 用于 usageId/requestId。
- Standards impact: MC-MODEL-001, MC-VALID-001, MC-NAME-001, MC-TIME-001。
- Literal rule enforcement: Rule 3、Rule 2（约束注解在载体上，Service 用 `@Validated`）、Rule 10（`Duration/Instant`）、Rule 11。
- Implementation pseudocode:

```java
@Data @NoArgsConstructor @AllArgsConstructor @Accessors(chain = true) @Builder
public class DdcAiOptimizationPolicyBO {
    @NotNull(groups = Persisted.class) @Positive private Long id; @NotNull @Positive private Long resourceId;
    @NotNull private Boolean enabled; @NotNull @Min(1) @Max(100) private Short scoreThreshold; @NotNull @Min(1) @Max(100000) private Integer minSamples;
    @NotNull @ValidDurationRange(minSeconds = 300, maxSeconds = 2592000) private Duration evaluationWindow;   // 注解在 Step 9 File 7 创建；本步先以 @NotNull 占位，Step 9 追加
    @NotNull @Min(1) @Max(50) private Short maxIterations; @NotNull private Duration cooldown; @NotNull private Boolean autoPublish; @NotNull private Boolean judgeEnabled;
    @NotBlank @Pattern(regexp = "^[a-z0-9][a-z0-9-]{0,63}$") private String optimizerModelAlias; @NotBlank @Pattern(regexp = "^[a-z0-9][a-z0-9-]{0,63}$") private String judgeModelAlias;
    @NotNull @Size(max = 4096) private String optimizerInstruction; @NotNull private DdcAiPolicyStatePersistenceEnum state; @NotNull @Min(0) private Short iterationsDone; @NotNull @Min(0) private Short consecutiveFailures;
    private Instant lastEvaluatedAt; private Instant convergedAt; private Long lastEvaluatedVersionId; private Long activeRunId; private BigDecimal lastAverageScore; private Integer lastSampleCount; private Long version;
}
```

- Verification contribution: File 10。
- After this file: 编译通过（`@ValidDurationRange` 在本步不引用，Step 9 追加）。

#### File 8 — `MODIFY egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/java/top/egon/cola/component/tianshu/admin/model/convertor/DdcAiEnumConverter.java`

- Purpose: 追加 6 个持久化枚举与 SDK/Result 枚举之间的 `@ValueMapping`，以及 `Duration↔Integer` 秒、`DdcAiOutcomeEnum↔DdcAiOutcomePersistenceEnum`。
- Symbols: `DdcAiOutcomePersistenceEnum toPersistence(DdcAiOutcomeEnum)`、`DdcAiOutcomeEnum toSdk(DdcAiOutcomePersistenceEnum)`、`@Named("durationToSeconds") Integer durationToSeconds(Duration)`、`@Named("secondsToDuration") Duration secondsToDuration(Integer)`
- Repository evidence: 既有 `DdcAiEnumConverter`（`@Mapper(componentModel="spring")` 的枚举映射集合，被各 Converter `uses`）。
- Dependencies and consumers: File 9 三 Converter、Step 5/9 Result 转换器。
- Why now: 转换前置。
- Contract/signature changes: 追加方法。
- Input/output and state mapping: 枚举 code 同名映射；`Duration.ofSeconds`。
- Error and edge behavior: `ANY_REMAINING→THROW`（MapStruct `@ValueMapping(source=MappingConstants.ANY_REMAINING, target=MappingConstants.THROW_EXCEPTION)`）。
- Standards impact: MC-CONVERT-001, MC-TIME-001。
- Literal rule enforcement: Rule 3（MapStruct）、Rule 10、Rule 11。
- Implementation pseudocode:

```java
@ValueMapping(source = "SUCCESS", target = "SUCCESS") @ValueMapping(source = "FAILURE", target = "FAILURE") @ValueMapping(source = "UNKNOWN", target = "UNKNOWN")
DdcAiOutcomePersistenceEnum toPersistence(DdcAiOutcomeEnum source);
@InheritInverseConfiguration DdcAiOutcomeEnum toSdk(DdcAiOutcomePersistenceEnum source);
@Named("durationToSeconds") default Integer durationToSeconds(Duration value) { return value == null ? null : Math.toIntExact(value.toSeconds()); }
@Named("secondsToDuration") default Duration secondsToDuration(Integer value) { return value == null ? null : Duration.ofSeconds(value); }
```

- Verification contribution: File 10。
- After this file: 编译通过。

#### File 9 — `CREATE egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/java/top/egon/cola/component/tianshu/admin/model/convertor/DdcAiUsageFeedbackConverter.java`

- Purpose: 3 个 PO↔BO `BaseConverter`（合并块：`DdcAiUsageFeedbackConverter`、`DdcAiOptimizationPolicyConverter`、`DdcAiOptimizationRunConverter`）。
- Symbols: `@Mapper(componentModel="spring", uses=DdcAiEnumConverter.class, unmappedTargetPolicy=ReportingPolicy.ERROR) interface DdcAiOptimizationPolicyConverter extends BaseConverter<DdcAiOptimizationPolicyPO, DdcAiOptimizationPolicyBO>`；policy 的 `@Mapping(target="evaluationWindow", source="evaluationWindowSeconds", qualifiedByName="secondsToDuration")` 与反向
- Repository evidence: `AJ/model/convertor/DdcAiPublicationConverter.java`（`BaseConverter<PO,BO>` + `uses`）。
- Dependencies and consumers: Repository Impl（Step 4）。
- Why now: 反腐层前置。
- Contract/signature changes: 新 Mapper。
- Input/output and state mapping: 技术字段（`createTime/updateTime/deletedAt/version`）按 `BaseConverter` 既有约定映射；`BO.version` 来自 `PO.version`。
- Error and edge behavior: `unmappedTargetPolicy=ERROR` 在编译期暴露漏字段。
- Standards impact: MC-CONVERT-001。
- Literal rule enforcement: Rule 3（必须 `BaseConverter`）、Rule 11。
- Implementation pseudocode:

```java
@Mapper(componentModel = "spring", uses = DdcAiEnumConverter.class, unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface DdcAiOptimizationPolicyConverter extends BaseConverter<DdcAiOptimizationPolicyPO, DdcAiOptimizationPolicyBO> {
    @Override @Mapping(target = "evaluationWindow", source = "evaluationWindowSeconds", qualifiedByName = "secondsToDuration")
    @Mapping(target = "cooldown", source = "cooldownSeconds", qualifiedByName = "secondsToDuration") DdcAiOptimizationPolicyBO toTarget(DdcAiOptimizationPolicyPO po);
    @Override @InheritInverseConfiguration(name = "toTarget") @Mapping(target = "evaluationWindowSeconds", source = "evaluationWindow", qualifiedByName = "durationToSeconds")
    @Mapping(target = "cooldownSeconds", source = "cooldown", qualifiedByName = "durationToSeconds") DdcAiOptimizationPolicyPO toSource(DdcAiOptimizationPolicyBO bo);
}
```

- Verification contribution: File 10。
- After this file: MapStruct 生成通过。

#### File 10 — `CREATE egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/test/java/top/egon/cola/component/tianshu/admin/model/DdcAiOptimizationConverterTest.java`

- Purpose: 证明三组转换往返与枚举/Duration 映射。
- Symbols: `policyRoundTripsDurationsAsSeconds`、`runEnumsMapByCodeNotOrdinal`、`feedbackOutcomeMapsBetweenSdkAndPersistence`
- Repository evidence: 既有 `AT/model/...` 下 Converter 测试用 `Mappers.getMapper` 或 Spring 上下文（以同目录既有测试为准）。
- Dependencies and consumers: File 5–9。
- Why now: 本步 GREEN。
- Contract/signature changes: 无。
- Input/output and state mapping: 构造 BO→PO→BO 相等。
- Error and edge behavior: 断言 `DdcAiRunErrorCodeEnum.fromCode("NOPE")` 抛 `IllegalArgumentException`。
- Standards impact: MC-TEST-001。
- Literal rule enforcement: Rule 6、Rule 11。
- Implementation pseudocode:

```java
@Test void policyRoundTripsDurationsAsSeconds() {
    var bo = DdcAiOptimizationPolicyBO.builder().resourceId(1L).evaluationWindow(Duration.ofHours(24)).cooldown(Duration.ofHours(1)).state(ARMED).build();
    var po = converter.toSource(bo); assertThat(po.getEvaluationWindowSeconds()).isEqualTo(86400); assertThat(converter.toTarget(po)).usingRecursiveComparison().isEqualTo(bo);
}
@Test void runEnumsMapByCodeNotOrdinal() { assertThat(DdcAiRunStatePersistenceEnum.SKIPPED.getCode()).isEqualTo("SKIPPED"); assertThatThrownBy(() -> DdcAiRunErrorCodeEnum.fromCode("NOPE")).isInstanceOf(IllegalArgumentException.class); }
```

- Verification contribution: GREEN。
- After this file: 本步模型完成。

#### File 11 — `MODIFY egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/test/java/top/egon/cola/component/tianshu/admin/integration/DdcTianshuDdlIT.java`

- Purpose: 在 Failsafe IT 中增加三表存在与两条部分唯一索引存在断言。
- Symbols: `aiOptimizationTablesExist`、`aiOptimizationPartialUniqueIndexesExist`
- Repository evidence: 既有 `DdcTianshuDdlIT` 查询 `information_schema.tables` 验证 `ddc_ai_publication_t0`。
- Dependencies and consumers: Step 11 运行。
- Why now: DDL 变更同步 IT。
- Contract/signature changes: 无。
- Input/output and state mapping: `pg_indexes.indexdef` 包含 `WHERE`。
- Error and edge behavior: 无 PG 环境时按既有 profile 跳过。
- Standards impact: MC-TEST-001。
- Literal rule enforcement: Rule 11。
- Implementation pseudocode:

```java
@Test void aiOptimizationTablesExist() {
    assertThat(jdbc.queryForList("select table_name from information_schema.tables where table_name in ('ddc_ai_usage_feedback_t0','ddc_ai_optimization_policy_t0','ddc_ai_optimization_run_t0')", String.class)).hasSize(3);
}
@Test void aiOptimizationPartialUniqueIndexesExist() {
    assertThat(jdbc.queryForObject("select indexdef from pg_indexes where indexname = 'ux_ai_optimization_run_active'", String.class)).contains("WHERE");
}
```

- Verification contribution: Step 11 IT 门禁。
- After this file: 本步完成。

- Validation working directory: 仓库根
- Verification command: `./mvnw -pl egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin -Dtest=DdcAiOptimizationSchemaScriptTest,DdcAiOptimizationConverterTest,DdcSchemaScriptTest -Dsurefire.failIfNoSpecifiedTests=false test`
- Expected result: 3 个测试类通过；MapStruct 无 unmapped 报错。
- Failure returns to: File 2（SQL）、File 3（摘要）、File 9（映射）。
- Completion criteria: 脚本/manifest/yml/模型全部有测试证据；既有脚本与 manifest 条目 diff 为零。
- Rollback: 回退本步 Commit paths（未执行任何数据库变更）。
- Commit paths: egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/test/java/top/egon/cola/component/tianshu/admin/repository/DdcAiOptimizationSchemaScriptTest.java, egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/resources/db/egon-mp/V20261017_001__tianshu_ai_feedback_optimization.sql, egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/resources/db/egon-mp/repository-manifest.json, egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/resources/egon-tianshu-persistence.yml, egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/java/top/egon/cola/component/tianshu/admin/model/enums/DdcAiOutcomePersistenceEnum.java, egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/java/top/egon/cola/component/tianshu/admin/model/enums/DdcAiJudgeStatePersistenceEnum.java, egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/java/top/egon/cola/component/tianshu/admin/model/enums/DdcAiPolicyStatePersistenceEnum.java, egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/java/top/egon/cola/component/tianshu/admin/model/enums/DdcAiRunStatePersistenceEnum.java, egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/java/top/egon/cola/component/tianshu/admin/model/enums/DdcAiRunTriggerPersistenceEnum.java, egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/java/top/egon/cola/component/tianshu/admin/model/enums/DdcAiRunErrorCodeEnum.java, egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/java/top/egon/cola/component/tianshu/admin/model/po/DdcAiUsageFeedbackPO.java, egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/java/top/egon/cola/component/tianshu/admin/model/po/DdcAiOptimizationPolicyPO.java, egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/java/top/egon/cola/component/tianshu/admin/model/po/DdcAiOptimizationRunPO.java, egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/java/top/egon/cola/component/tianshu/admin/model/bo/DdcAiUsageFeedbackBO.java, egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/java/top/egon/cola/component/tianshu/admin/model/bo/DdcAiOptimizationPolicyBO.java, egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/java/top/egon/cola/component/tianshu/admin/model/bo/DdcAiOptimizationRunBO.java, egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/java/top/egon/cola/component/tianshu/admin/model/bo/DdcAiScoreAggregateBO.java, egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/java/top/egon/cola/component/tianshu/admin/model/convertor/DdcAiEnumConverter.java, egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/java/top/egon/cola/component/tianshu/admin/model/convertor/DdcAiUsageFeedbackConverter.java, egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/java/top/egon/cola/component/tianshu/admin/model/convertor/DdcAiOptimizationPolicyConverter.java, egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/java/top/egon/cola/component/tianshu/admin/model/convertor/DdcAiOptimizationRunConverter.java, egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/test/java/top/egon/cola/component/tianshu/admin/model/DdcAiOptimizationConverterTest.java, egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/test/java/top/egon/cola/component/tianshu/admin/integration/DdcTianshuDdlIT.java
- Commit: `feat(tianshu): ai feedback and optimization schema with persistence models`

### Step 4 — Mapper、命名 XML 与 Repository 反腐层

- Requirements: `REQ-002`, `REQ-004`, `REQ-005`, `REQ-011`
- Dependencies: Step 3
- Baseline state: `AJ/dao/DdcAiPublicationMapper.java`（`EgonColaMapper<PO>`，default 覆盖 `selectActiveById`，所有方法带 `@Param` 与 Query 组约束）、同名 XML（resultMap + `<sql>` 片段 + 租户/deleted_at 条件）、`DdcAiPublicationRepositoryImpl extends EgonColaRepository<Mapper, PO>`（`@Repository("...")`、`@Qualifier` 三注入）。
- Observable outcome: 三组 Mapper/XML/Repository 可编译；`DdcAiFeedbackOptimizationRepositoryContractTest` 以 MyBatis `Configuration` 解析三份 XML 并断言 statement id 与 `resultMap` 列集合与 PO 字段一致，且 Repository Impl 在 Mockito Mapper 上把 `DataIntegrityViolationException` 原样抛出（由 Service 映射 DUPLICATE）。
- End state: 持久化访问层完成；无 Service。
- Test-first gate: Required — 契约测试因 Mapper 类与 XML 不存在 RED。
- Manual Checks: MC-ARCH-001, MC-REUSE-001, MC-NAME-001, MC-VALID-001, MC-CONVERT-001, MC-LOG-001, MC-BEAN-001, MC-TIME-001, MC-SCOPE-001, MC-TEST-001
- Literal Rules: Rule 1, Rule 2, Rule 3, Rule 4, Rule 10, Rule 11
- Ordered files:

#### File 1 — `CREATE egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/test/java/top/egon/cola/component/tianshu/admin/repository/DdcAiFeedbackOptimizationRepositoryContractTest.java`

- Purpose: 固定三组 statement 清单、resultMap 列覆盖与 Impl 行为。
- Symbols: `mapperXmlDeclaresExpectedStatements`、`resultMapsCoverEveryPoField`、`insertFeedbackPropagatesUniqueViolation`、`claimPendingForJudgeUsesSkipLocked`
- Repository evidence: `AT/repository/DdcSchemaScriptTest.java` 同包；既有 Mapper XML 可用 `org.apache.ibatis.session.Configuration` + `XMLMapperBuilder` 离线解析（JUnit 无需数据库）。
- Dependencies and consumers: File 2–5。
- Why now: RED。
- Contract/signature changes: 无。
- Input/output and state mapping: XML→`Configuration.getMappedStatementNames()`。
- Error and edge behavior: 断言 `claimPendingForJudge` SQL 文本含 `FOR UPDATE SKIP LOCKED`；`selectWorstSamples` 含 `ORDER BY coalesce(explicit_score, judge_score) ASC`。
- Standards impact: MC-TEST-001。
- Literal rule enforcement: Rule 11。
- Implementation pseudocode:

```java
@Test void mapperXmlDeclaresExpectedStatements() {
    Configuration cfg = parse("mybatis/mapper/tianshu/DdcAiUsageFeedbackMapper.xml", "mybatis/mapper/tianshu/DdcAiOptimizationPolicyMapper.xml", "mybatis/mapper/tianshu/DdcAiOptimizationRunMapper.xml");
    assertThat(cfg.getMappedStatementNames()).contains(FEEDBACK + ".selectActiveById", FEEDBACK + ".claimPendingForJudge", FEEDBACK + ".completeJudge", FEEDBACK + ".failJudge", FEEDBACK + ".aggregateWindow", FEEDBACK + ".selectWorstSamples", FEEDBACK + ".selectPage", FEEDBACK + ".countPage", FEEDBACK + ".softDeleteExpired",
        POLICY + ".selectActiveByResourceId", POLICY + ".selectEvaluableForUpdateSkipLocked", POLICY + ".updateStateVersioned", POLICY + ".softDeleteByResourceId",
        RUN + ".selectActiveById", RUN + ".selectActiveByResourceAndRequestId", RUN + ".selectNextRunNo", RUN + ".selectActiveRunForResource", RUN + ".claimPending", RUN + ".updateVersioned", RUN + ".selectExpiredRunning", RUN + ".selectPage", RUN + ".countPage");
    assertThat(sqlOf(cfg, FEEDBACK + ".claimPendingForJudge")).contains("FOR UPDATE SKIP LOCKED");
}
@Test void insertFeedbackPropagatesUniqueViolation() { when(mapper.insert(any())).thenThrow(new DuplicateKeyException("uk_ai_usage_feedback_usage")); assertThatThrownBy(() -> repository.insertFeedback(bo())).isInstanceOf(DataIntegrityViolationException.class); }
```

- Verification contribution: RED→GREEN。
- After this file: RED。

#### File 2 — `CREATE egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/java/top/egon/cola/component/tianshu/admin/dao/DdcAiUsageFeedbackMapper.java`

- Purpose: 3 个 Mapper 接口（合并块：`DdcAiUsageFeedbackMapper`、`DdcAiOptimizationPolicyMapper`、`DdcAiOptimizationRunMapper`，同目录），所有查询显式 `tenantId` 参数；聚合投影 PO `DdcAiScoreAggregatePO` 放 `model/po`。
- Symbols: feedback — `selectActiveById`、`selectActiveByIds`、`claimPendingForJudge(tenantId, resourceIds, now, leaseUntil, maxAttempts, limit, actor)`、`completeJudge(tenantId, id, version, score, judgedAt, actor)`、`failJudge(tenantId, id, version, errorCode, finalFailure, actor)`、`aggregateWindow(tenantId, resourceId, versionId, from, to)`、`selectWorstSamples(tenantId, resourceId, versionId, from, to, limit)`、`selectPage/countPage(tenantId, resourceId, versionId, outcome, judgeState, from, to, offset, limit)`、`softDeleteExpired(tenantId, before, deletedAt, limit)`；policy — `selectActiveByResourceId`、`selectEvaluableForUpdateSkipLocked(tenantId, now, limit)`、`updateStateVersioned(po)`、`softDeleteByResourceId`；run — `selectActiveByResourceAndRequestId`、`selectNextRunNo(tenantId, resourceId)`、`selectActiveRunForResource`、`claimPending(tenantId, id, version, owner, leaseUntil, startedAt, actor)`、`updateVersioned(po)`、`selectExpiredRunning(tenantId, now, limit)`、`selectPage/countPage(tenantId, resourceId, state, trigger, offset, limit)`
- Repository evidence: `DdcAiPublicationMapper`（default `selectActiveById(Serializable)` 转调 `(tenantId,id)`，`restoreVersionedById` 禁用）。
- Dependencies and consumers: XML（File 3）、Repository Impl（File 5）。
- Why now: 访问契约。
- Contract/signature changes: 新接口。
- Input/output and state mapping: 时间参数 `Instant`；聚合结果 PO 字段 `BigDecimal averageScore, Integer sampleCount`。
- Error and edge behavior: run Mapper 的 `deleteVersionedById` 抛 `UnsupportedOperationException("AI_OPTIMIZATION_RUN_IMMUTABLE")`（审计）；feedback 的 `restoreVersionedById` 禁用。
- Standards impact: MC-VALID-001（Query/Update 组约束）、MC-NAME-001、MC-TIME-001。
- Literal rule enforcement: Rule 2、Rule 10、Rule 11（`EgonColaMapper`）。
- Implementation pseudocode:

```java
@Mapper @Validated public interface DdcAiUsageFeedbackMapper extends EgonColaMapper<DdcAiUsageFeedbackPO> {
    @Override default DdcAiUsageFeedbackPO selectActiveById(Serializable id) { return selectActiveById(EgonColaTenantIdProvider.currentTenantId(), requireInternalId(id)); }
    @Override default int restoreVersionedById(DdcAiUsageFeedbackPO row) { throw new UnsupportedOperationException("AI_USAGE_FEEDBACK_RESTORE_DISABLED"); }
    @Validated(Query.class) List<DdcAiUsageFeedbackPO> claimPendingForJudge(@Param("tenantId") @NotNull @Positive Long tenantId, @Param("resourceIds") @NotEmpty Collection<@Positive Long> resourceIds,
        @Param("now") @NotNull Instant now, @Param("leaseUntil") @NotNull Instant leaseUntil, @Param("maxAttempts") @Min(1) @Max(10) int maxAttempts, @Param("limit") @Min(1) @Max(500) int limit, @Param("actor") @NotBlank String actor);
    @Validated(Update.class) int completeJudge(@Param("tenantId") Long tenantId, @Param("id") Long id, @Param("version") Long version, @Param("score") @Min(0) @Max(100) short score, @Param("judgedAt") Instant judgedAt, @Param("actor") String actor);
    @Validated(Query.class) DdcAiScoreAggregatePO aggregateWindow(@Param("tenantId") Long tenantId, @Param("resourceId") Long resourceId, @Param("versionId") Long versionId, @Param("from") Instant from, @Param("to") Instant to);
    @Validated(Update.class) int softDeleteExpired(@Param("tenantId") Long tenantId, @Param("before") Instant before, @Param("deletedAt") LocalDateTime deletedAt, @Param("limit") @Min(1) @Max(5000) int limit);
}
```

- Verification contribution: File 1。
- After this file: 编译通过；XML 未就绪。

#### File 3 — `CREATE egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/resources/mybatis/mapper/tianshu/DdcAiUsageFeedbackMapper.xml`

- Purpose: 3 份命名 XML（合并块：`DdcAiUsageFeedbackMapper.xml`、`DdcAiOptimizationPolicyMapper.xml`、`DdcAiOptimizationRunMapper.xml`）。
- Symbols: 每份 `resultMap`（技术八字段 + 业务列）、`<sql id="…Columns">`、`<sql id="Active…">`（`tenant_id = #{tenantId} AND deleted_at IS NULL`）、File 2 列出的全部 statement
- Repository evidence: `DdcAiPublicationMapper.xml`（resultMap/`<sql>`/JOIN 资源表判活）。
- Dependencies and consumers: File 2。
- Why now: SQL 实现。
- Contract/signature changes: 无。
- Input/output and state mapping: 认领为 PostgreSQL `UPDATE ... WHERE id IN (SELECT ... FOR UPDATE SKIP LOCKED) RETURNING`（MyBatis 以 `<select>` 承载 RETURNING）；聚合 `AVG(coalesce(explicit_score, judge_score))`，仅统计 `explicit_score IS NOT NULL OR judge_state='DONE'`。
- Error and edge behavior: 所有 UPDATE 带 `AND version = #{version}` 与 `version = version + 1, update_time = now(), update_user_id = #{actor}`；`selectNextRunNo` 用 `coalesce(max(run_no),0)+1`，重复由运行表部分唯一索引兜底。
- Standards impact: MC-TIME-001（timestamptz 绑定 `Instant`）。
- Literal rule enforcement: Rule 10、Rule 11。
- Implementation pseudocode:

```xml
<select id="claimPendingForJudge" resultMap="DdcAiUsageFeedbackPOResultMap">
  UPDATE ddc_ai_usage_feedback f SET judge_state = 'JUDGING', judge_attempts = f.judge_attempts + 1, judge_lease_until = #{leaseUntil}, version = f.version + 1, update_time = now(), update_user_id = #{actor}
  WHERE f.tenant_id = #{tenantId} AND f.id IN (SELECT c.id FROM ddc_ai_usage_feedback c WHERE c.tenant_id = #{tenantId} AND c.deleted_at IS NULL
      AND c.resource_id IN <foreach collection="resourceIds" item="r" open="(" separator="," close=")">#{r}</foreach>
      AND c.explicit_score IS NULL AND c.judge_attempts &lt; #{maxAttempts} AND (c.judge_state = 'PENDING' OR (c.judge_state = 'JUDGING' AND c.judge_lease_until &lt; #{now}))
      ORDER BY c.occurred_at LIMIT #{limit} FOR UPDATE SKIP LOCKED)
  RETURNING <include refid="FeedbackColumnsUnaliased"/>
</select>
<select id="aggregateWindow" resultType="top.egon.cola.component.tianshu.admin.model.po.DdcAiScoreAggregatePO">
  SELECT COUNT(*) AS sample_count, ROUND(AVG(coalesce(explicit_score, judge_score))::numeric, 2) AS average_score FROM ddc_ai_usage_feedback
  WHERE tenant_id = #{tenantId} AND deleted_at IS NULL AND resource_id = #{resourceId} AND version_id = #{versionId}
    AND occurred_at &gt;= #{from} AND occurred_at &lt; #{to} AND (explicit_score IS NOT NULL OR judge_state = 'DONE')
</select>
<select id="selectEvaluableForUpdateSkipLocked" resultMap="DdcAiOptimizationPolicyPOResultMap">
  SELECT <include refid="PolicyColumns"/> FROM ddc_ai_optimization_policy p WHERE p.tenant_id = #{tenantId} AND p.deleted_at IS NULL AND p.enabled = true AND p.state IN ('ARMED','AWAITING_PUBLISH')
    AND (p.last_evaluated_at IS NULL OR p.last_evaluated_at + make_interval(secs => p.cooldown_seconds) &lt;= #{now}) ORDER BY p.last_evaluated_at NULLS FIRST LIMIT #{limit} FOR UPDATE SKIP LOCKED
</select>
```

- Verification contribution: File 1。
- After this file: XML 可解析。

#### File 4 — `CREATE egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/java/top/egon/cola/component/tianshu/admin/repository/DdcAiUsageFeedbackRepository.java`

- Purpose: 3 个 Repository 接口（合并块：`DdcAiUsageFeedbackRepository`、`DdcAiOptimizationPolicyRepository`、`DdcAiOptimizationRunRepository`）。
- Symbols: feedback — `insertFeedback(BO)`、`findActiveById`、`claimPendingForJudge(resourceIds, now, leaseUntil, maxAttempts, limit)`、`completeJudge(id, version, score, judgedAt)`→boolean、`failJudge(id, version, code, finalFailure)`→boolean、`aggregateWindow(resourceId, versionId, from, to)`→`DdcAiScoreAggregateBO`、`selectWorstSamples(resourceId, versionId, from, to, limit)`、`page(DdcAiUsageFeedbackPageQuery)`、`softDeleteExpired(before, limit)`；policy — `findActiveByResourceId`、`insertPolicy`、`updatePolicy(BO)`→boolean、`lockEvaluable(now, limit)`、`softDeleteByResourceId`；run — `insertRun`、`findActiveById`、`findByResourceAndRequestId`、`nextRunNo(resourceId)`、`findActiveRunForResource`、`claimPending(id, version, owner, leaseUntil, startedAt)`→boolean、`updateRun(BO)`→boolean、`findExpiredRunning(now, limit)`、`page(DdcAiOptimizationRunPageQuery)`
- Repository evidence: `DdcAiPublicationRepository`（接口 `@Validated` + 方法级组注解 + `PageQuery`）。
- Dependencies and consumers: Service（Step 5/8/9/11）。
- Why now: 反腐层契约。
- Contract/signature changes: 新接口。
- Input/output and state mapping: BO 进出；`DdcPageResult` 返回。
- Error and edge behavior: 唯一冲突原样抛 Spring `DataIntegrityViolationException`；CAS 失败返回 false，不抛。
- Standards impact: MC-VALID-001, MC-NAME-001。
- Literal rule enforcement: Rule 2（接口级 `@Validated` + 组）、Rule 1、Rule 11。
- Implementation pseudocode:

```java
@Validated public interface DdcAiOptimizationRunRepository {
    @Validated(Insert.class) DdcAiOptimizationRunBO insertRun(@NotNull @Valid DdcAiOptimizationRunBO run);
    @Validated(Query.class) Optional<DdcAiOptimizationRunBO> findByResourceAndRequestId(@NotNull @Positive Long resourceId, @NotNull @Pattern(regexp = "^[A-Za-z0-9_-]{16,64}$") String requestId);
    @Validated(Query.class) long nextRunNo(@NotNull @Positive Long resourceId);
    @Validated(Update.class) boolean claimPending(@NotNull @Positive Long id, @NotNull @Min(0) Long version, @NotBlank @Size(max = 128) String owner, @NotNull Instant leaseUntil, @NotNull Instant startedAt);
    @Validated(Update.class) boolean updateRun(@NotNull @Valid DdcAiOptimizationRunBO run);
    @Validated(Query.class) DdcPageResult<DdcAiOptimizationRunBO> page(@NotNull @Valid DdcAiOptimizationRunPageQuery query);
}
```

- Verification contribution: File 1（Impl 测试通过接口）。
- After this file: 编译通过。

#### File 5 — `CREATE egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/java/top/egon/cola/component/tianshu/admin/repository/impl/DdcAiUsageFeedbackRepositoryImpl.java`

- Purpose: 3 个 Impl（合并块：`DdcAiUsageFeedbackRepositoryImpl`、`DdcAiOptimizationPolicyRepositoryImpl`、`DdcAiOptimizationRunRepositoryImpl`）；`DdcAiUsageFeedbackConverter` 追加 `DdcAiScoreAggregateBO toAggregate(DdcAiScoreAggregatePO)`。
- Symbols: `@Repository("ddcAiUsageFeedbackRepository")`、`@Repository("ddcAiOptimizationPolicyRepository")`、`@Repository("ddcAiOptimizationRunRepository")`；`extends EgonColaRepository<Mapper, PO>`；注入 `@Qualifier("ddcAiUsageFeedbackMapper")`、`@Qualifier("ddcAiUsageFeedbackConverterImpl")`、MP properties 限定名同既有
- Repository evidence: `DdcAiPublicationRepositoryImpl`（`@Slf4j @Repository("...") @RequiredArgsConstructor @Validated`，`getBaseMapper/getProperties` 覆盖，`EgonColaTenantIdProvider.currentTenantId()`）。
- Dependencies and consumers: Service。
- Why now: 访问实现。
- Contract/signature changes: 新类。
- Input/output and state mapping: `converter.toSource/toTarget/toTargetList`；`actor` 取 `MDC.get("userId")`。
- Error and edge behavior: `insert*` 用 `save(po)` 返回 false 抛 `IllegalStateException("AI_USAGE_FEEDBACK_INSERT_FAILED")` 等；CAS 更新返回 `== 1`。
- Standards impact: MC-LOG-001（`@Slf4j`）、MC-BEAN-001（具名 + `@RequiredArgsConstructor` + `@Qualifier`）、MC-CONVERT-001。
- Literal rule enforcement: Rule 4、Rule 3、Rule 11。
- Implementation pseudocode:

```java
@Slf4j @Repository("ddcAiOptimizationRunRepository") @RequiredArgsConstructor @Validated
public class DdcAiOptimizationRunRepositoryImpl extends EgonColaRepository<DdcAiOptimizationRunMapper, DdcAiOptimizationRunPO> implements DdcAiOptimizationRunRepository {
    @Qualifier("ddcAiOptimizationRunMapper") private final DdcAiOptimizationRunMapper mapper;
    @Qualifier("ddcAiOptimizationRunConverterImpl") private final DdcAiOptimizationRunConverter converter;
    @Qualifier("egon.cola.component.mybatis-plus-top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties") private final EgonColaMybatisPlusProperties properties;
    @Override public boolean claimPending(Long id, Long version, String owner, Instant leaseUntil, Instant startedAt) {
        return mapper.claimPending(EgonColaTenantIdProvider.currentTenantId(), id, version, owner, leaseUntil, startedAt, MDC.get("userId")) == 1;
    }
    @Override public long nextRunNo(Long resourceId) { return mapper.selectNextRunNo(EgonColaTenantIdProvider.currentTenantId(), resourceId); }
    @Override public DdcAiOptimizationRunBO insertRun(DdcAiOptimizationRunBO run) { var po = converter.toSource(run); if (!save(po)) throw new IllegalStateException("AI_OPTIMIZATION_RUN_INSERT_FAILED"); return converter.toTarget(po); }
}
```

- Verification contribution: File 1 Impl 用例。
- After this file: 本步 GREEN。

- Validation working directory: 仓库根
- Verification command: `./mvnw -pl egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin -Dtest=DdcAiFeedbackOptimizationRepositoryContractTest,DdcAiOptimizationConverterTest -Dsurefire.failIfNoSpecifiedTests=false test`
- Expected result: 两测试类通过；XML 解析无 `BuilderException`。
- Failure returns to: File 3（SQL）、File 5（Impl）。
- Completion criteria: 22 个 statement 与接口方法一一对应；Impl 具名 Bean。
- Rollback: 回退本步 Commit paths。
- Commit paths: egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/test/java/top/egon/cola/component/tianshu/admin/repository/DdcAiFeedbackOptimizationRepositoryContractTest.java, egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/java/top/egon/cola/component/tianshu/admin/dao/DdcAiUsageFeedbackMapper.java, egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/java/top/egon/cola/component/tianshu/admin/dao/DdcAiOptimizationPolicyMapper.java, egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/java/top/egon/cola/component/tianshu/admin/dao/DdcAiOptimizationRunMapper.java, egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/java/top/egon/cola/component/tianshu/admin/model/po/DdcAiScoreAggregatePO.java, egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/resources/mybatis/mapper/tianshu/DdcAiUsageFeedbackMapper.xml, egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/resources/mybatis/mapper/tianshu/DdcAiOptimizationPolicyMapper.xml, egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/resources/mybatis/mapper/tianshu/DdcAiOptimizationRunMapper.xml, egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/java/top/egon/cola/component/tianshu/admin/repository/DdcAiUsageFeedbackRepository.java, egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/java/top/egon/cola/component/tianshu/admin/repository/DdcAiOptimizationPolicyRepository.java, egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/java/top/egon/cola/component/tianshu/admin/repository/DdcAiOptimizationRunRepository.java, egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/java/top/egon/cola/component/tianshu/admin/repository/impl/DdcAiUsageFeedbackRepositoryImpl.java, egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/java/top/egon/cola/component/tianshu/admin/repository/impl/DdcAiOptimizationPolicyRepositoryImpl.java, egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/java/top/egon/cola/component/tianshu/admin/repository/impl/DdcAiOptimizationRunRepositoryImpl.java, egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/java/top/egon/cola/component/tianshu/admin/model/convertor/DdcAiUsageFeedbackConverter.java
- Commit: `feat(tianshu): ai feedback and optimization mappers with repositories`

### Step 5 — 系统主体、权限分支、反馈服务与 Admin 配置

- Requirements: `REQ-002`, `REQ-003`, `REQ-011`, `REQ-013`, `REQ-014`
- Dependencies: Step 4
- Baseline state: `DdcAiScopeValidator.Permission{READ,WRITE,PUBLISH}`，RPC 分支只允许 `READ` + `AI_READ`；`DdcAdminProperties` 无 `ai` 子节；三份 yml 无 `admin.ai.*` 键；runtime 凭据 `allowed-operations` 末尾为 `AI_READ`；`AJ/model/command|query|vo|dto` 仅既有 AI 类。
- Observable outcome: `DdcAiScopeValidatorAuthorityTest` 证明四操作矩阵、系统主体分支与用户分支不变；`DdcAiFeedbackServiceTest` 证明 accept 的截断/时间窗/DUPLICATE/REJECTED 与 CAS 写回；`DdcAiOptimizationPropertiesTest` 证明三 profile 键集合相等且默认值与主 Spec §15.4 一致。
- End state: 反馈服务可被 RPC Provider（Step 6）与作业（Step 8/11）调用；无 RPC 入口。
- Test-first gate: Required — 三个测试类因符号缺失 RED。
- Manual Checks: MC-ARCH-001, MC-REUSE-001, MC-NAME-001, MC-VALID-001, MC-MODEL-001, MC-CONVERT-001, MC-LOG-001, MC-BEAN-001, MC-UTIL-001, MC-JSON-001, MC-TIME-001, MC-CONFIG-001, MC-SCOPE-001, MC-TEST-001
- Literal Rules: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 10, Rule 11
- Ordered files:

#### File 1 — `CREATE egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/test/java/top/egon/cola/component/tianshu/admin/service/ai/DdcAiScopeValidatorAuthorityTest.java`

- Purpose: TEST-003 矩阵。
- Symbols: `rpcPrincipalRequiresMatchingAllowedOperation`（参数化：READ/AI_READ 允许、REPORT 需 AI_REPORT、WRITE 需 AI_WRITE、PUBLISH 需 AI_PUBLISH、缺失→56307）、`systemActorBypassesPrincipalChecksWithinTenant`、`userBranchUnchanged`
- Repository evidence: `AT/service/ai/DdcAiVersionServiceTest.java` 同目录；`DdcServicePrincipal` 为 record 可直接构造；`DdcServicePrincipal.current()` 读 gRPC `Context`（测试用 `Context.current().withValue(CONTEXT_KEY, principal).call(...)` 或既有测试辅助）。
- Dependencies and consumers: File 4/5。
- Why now: RED。
- Contract/signature changes: 无。
- Input/output and state mapping: 构造主体 + MDC tenantId→调用 `assertCanReport/assertCanWrite/assertCanPublish/assertCanRead`。
- Error and edge behavior: 系统主体未绑定租户（MDC 缺失）→56307。
- Standards impact: MC-TEST-001。
- Literal rule enforcement: Rule 11。
- Implementation pseudocode:

```java
@ParameterizedTest @CsvSource({"READ,AI_READ,true", "REPORT,AI_REPORT,true", "REPORT,AI_READ,false", "WRITE,AI_WRITE,true", "PUBLISH,AI_PUBLISH,true", "PUBLISH,AI_WRITE,false"})
void rpcPrincipalRequiresMatchingAllowedOperation(String permission, String allowed, boolean ok) throws Exception {
    DdcServicePrincipal principal = principal(Set.of(allowed));   // clientType SDK, tenantId 42, namespacePatterns ["ns-*"]
    MDC.put("tenantId", "42");
    Callable<Void> call = () -> { invoke(validator, permission, resource("ns-a")); return null; };
    if (ok) Context.current().withValue(DdcServicePrincipal.CONTEXT_KEY, principal).call(call);
    else assertThatThrownBy(() -> Context.current().withValue(DdcServicePrincipal.CONTEXT_KEY, principal).call(call)).hasFieldOrPropertyWithValue("code", 56307);
}
@Test void systemActorBypassesPrincipalChecksWithinTenant() { MDC.put("tenantId", "42"); DdcAiSystemActor.bind("system:ai-judge", () -> { validator.assertCanPublish(resource("ns-a")); return null; }); }
```

- Verification contribution: RED→GREEN。
- After this file: RED。

#### File 2 — `CREATE egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/test/java/top/egon/cola/component/tianshu/admin/service/ai/DdcAiFeedbackServiceTest.java`

- Purpose: TEST-002/TEST-007 与 accept 规则。
- Symbols: `acceptTruncatesExcerptsAndMarksPendingJudge`、`acceptMapsUniqueViolationToDuplicate`、`acceptRejectsOutsideClockWindow`、`acceptRejectsVersionNotOfResource`、`completeJudgeIgnoresCasMiss`、`acceptKeepsReceiptOrder`
- Repository evidence: Mockito 风格同 `DdcAiVersionServiceTest`（替身 Repository，固定 `Clock`）。
- Dependencies and consumers: File 8–14。
- Why now: RED。
- Contract/signature changes: 无。
- Input/output and state mapping: `DdcAiUsageReportBatchCommand(3 条)`→`DdcAiUsageReportReceiptResult.items` 同序。
- Error and edge behavior: 第二条 Repository 抛 `DuplicateKeyException`→`ACCEPTED_DUPLICATE`；第三条 `occurredAt = now + 10m`（skew 5m）→`REJECTED("OCCURRED_AT_OUT_OF_WINDOW")`。
- Standards impact: MC-TEST-001, MC-TIME-001。
- Literal rule enforcement: Rule 10、Rule 11。
- Implementation pseudocode:

```java
@Test void acceptMapsUniqueViolationToDuplicate() {
    when(feedbackRepository.insertFeedback(any())).thenReturn(saved()).thenThrow(new DuplicateKeyException("uk_ai_usage_feedback_usage")).thenReturn(saved());
    when(transactionTemplate.execute(any())).thenAnswer(inv -> ((TransactionCallback<?>) inv.getArgument(0)).doInTransaction(status));
    var receipt = service.accept(batch(report("u1"), report("u1"), report("u3")));
    assertThat(receipt.getItems()).extracting(DdcAiUsageReportReceiptResult.ItemResult::getStatus).containsExactly(ACCEPTED, ACCEPTED_DUPLICATE, ACCEPTED);
}
@Test void acceptRejectsOutsideClockWindow() { var r = report("u9").setOccurredAt(fixedNow.plus(Duration.ofMinutes(10))); assertThat(service.accept(batch(r)).getItems().get(0)).extracting("status", "reason").containsExactly(REJECTED, "OCCURRED_AT_OUT_OF_WINDOW"); verifyNoInteractions(feedbackRepository); }
@Test void completeJudgeIgnoresCasMiss() { when(feedbackRepository.completeJudge(1L, 3L, (short) 70, fixedNow)).thenReturn(false); assertThatCode(() -> service.completeJudge(1L, 3L, (short) 70)).doesNotThrowAnyException(); }
```

- Verification contribution: RED→GREEN。
- After this file: RED。

#### File 3 — `CREATE egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/test/java/top/egon/cola/component/tianshu/admin/config/DdcAiOptimizationPropertiesTest.java`

- Purpose: TEST-030 键矩阵 + 绑定默认值。
- Symbols: `threeProfilesDeclareIdenticalAiKeySets`、`bindsDefaultsFromSpec`、`rejectsOutOfRangeJudgeMaxAttempts`、`runtimeCredentialAllowsAiReport`
- Repository evidence: `AT/config/` 存在既有配置测试目录；SnakeYAML 在 Spring Boot test 依赖中可用。
- Dependencies and consumers: File 6/7。
- Why now: RED。
- Contract/signature changes: 无。
- Input/output and state mapping: 解析三份 yml→扁平键集合（前缀 `egon.cola.component.tianshu.admin.ai.`）相等；`Binder` 绑定 `application-test.yml`→`DdcAiOptimizationProperties`。
- Error and edge behavior: `judge.max-attempts=11`→`BindValidationException`。
- Standards impact: MC-CONFIG-001, MC-TEST-001。
- Literal rule enforcement: Rule 7、Rule 11。
- Implementation pseudocode:

```java
@Test void threeProfilesDeclareIdenticalAiKeySets() {
    Set<String> base = aiKeys("application.yml"), local = aiKeys("application-local.yml"), test = aiKeys("application-test.yml");
    assertThat(local).isEqualTo(base); assertThat(test).isEqualTo(base); assertThat(base).contains("optimization.enabled", "optimization.scan-interval", "optimization.run-lease", "optimization.max-concurrent-runs", "optimization.sample-limit", "judge.scan-interval", "judge.batch-size", "judge.max-attempts", "judge.max-context-bytes", "judge.lease", "model.base-url", "model.resource-uri", "model.registration-id", "model.app-id", "model.scopes", "model.connect-timeout", "model.read-timeout", "model.max-output-tokens", "model.temperature", "feedback.retention", "feedback.retention-scan-interval", "feedback.max-batch", "feedback.max-clock-skew", "feedback.max-input-excerpt-bytes", "feedback.max-output-excerpt-bytes");
}
@Test void bindsDefaultsFromSpec() { var p = bind("application-test.yml"); assertThat(p.getOptimization().isEnabled()).isFalse(); assertThat(p.getJudge().getMaxAttempts()).isEqualTo(3); assertThat(p.getFeedback().getRetention()).isEqualTo(Duration.ofDays(30)); }
@Test void runtimeCredentialAllowsAiReport() { assertThat(flatten("application.yml").get("...rpc.credentials[0].allowed-operations")).asString().endsWith("AI_READ,AI_REPORT"); }
```

- Verification contribution: RED→GREEN。
- After this file: RED。

#### File 4 — `CREATE egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/java/top/egon/cola/component/tianshu/admin/service/ai/validation/DdcAiSystemActor.java`

- Purpose: 作业线程的系统主体（ThreadLocal），供 `DdcAiScopeValidator` 第三分支与 `DdcAiResourceServiceImpl#authenticatedActor` 识别。
- Symbols: `public final class DdcAiSystemActor`；`static <T> T bind(String actorId, Supplier<T> body)`（绑定→执行→恢复旧值）；`static Optional<String> current()`；常量 `JUDGE = "system:ai-judge"`、`OPTIMIZER = "system:ai-optimizer"`、`RETENTION = "system:ai-retention"`
- Repository evidence: `DdcTenantContextExecutor.call(DdcTenantContextDTO, Supplier)` 也用 ThreadLocal/MDC 绑定-恢复模式。
- Dependencies and consumers: 三个作业、`DdcAiScopeValidator`、`DdcAiResourceServiceImpl`（Step 10）。
- Why now: 权限分支前置。
- Contract/signature changes: 新类。
- Input/output and state mapping: actorId 匹配 `^system:[a-z-]{2,32}$`。
- Error and edge behavior: 嵌套绑定允许，恢复为上一值；异常后 finally 恢复。
- Standards impact: MC-NAME-001, MC-UTIL-001（仅 JDK）。
- Literal rule enforcement: Rule 5（JDK）、Rule 11。
- Implementation pseudocode:

```java
public final class DdcAiSystemActor {
    private static final ThreadLocal<String> CURRENT = new ThreadLocal<>();
    private static final Pattern ACTOR = Pattern.compile("^system:[a-z-]{2,32}$");
    public static <T> T bind(String actorId, Supplier<T> body) {
        if (actorId == null || !ACTOR.matcher(actorId).matches()) throw new IllegalArgumentException("invalid system actor");
        String previous = CURRENT.get(); CURRENT.set(actorId);
        try { return body.get(); } finally { if (previous == null) CURRENT.remove(); else CURRENT.set(previous); }
    }
    public static Optional<String> current() { return Optional.ofNullable(CURRENT.get()); }
}
```

- Verification contribution: File 1 系统主体用例。
- After this file: 编译通过。

#### File 5 — `MODIFY egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/java/top/egon/cola/component/tianshu/admin/service/ai/validation/DdcAiScopeValidator.java`

- Purpose: `Permission.REPORT`、RPC 分支按操作映射、系统主体分支、`resolveNamespaceIdForReport/assertCanReport`。
- Symbols: `Permission{READ,WRITE,PUBLISH,REPORT}` 与 `rpcOperation()`→`"AI_READ"/"AI_WRITE"/"AI_PUBLISH"/"AI_REPORT"`；`requireNamespacePermission` 新顺序：用户 Authentication 分支（不变）→系统主体分支→RPC 分支
- Repository evidence: 上文源码第 118–160 行（`permission != Permission.READ || !"SDK".equals(principal.clientType()) ... allowedOperations().contains("AI_READ")`）。
- Dependencies and consumers: Service 层所有 AI 入口。
- Why now: 四操作授权。
- Contract/signature changes: 新增两个 public 方法；既有方法签名不变。
- Input/output and state mapping: 系统主体分支校验 `MDC tenantId` 非空即通过（作业已由 `DdcTenantContextExecutor` 绑定）；RPC 分支把 `"AI_READ"` 字面量替换为 `permission.rpcOperation()`，去掉 `permission != READ` 的无条件拒绝；用户分支 oauthScope 映射：READ→`TIANSHU_AI_READ`，其余→`TIANSHU_AI_WRITE`（REPORT 不由用户调用，用户分支遇 REPORT 直接 56307）。
- Error and edge behavior: `clientType` 仍必须为 `SDK`；namespace 模式匹配不变。
- Standards impact: MC-SCOPE-001（最小修改）、MC-LOG-001。
- Literal rule enforcement: Rule 4（既有具名 Bean 不变）、Rule 11。
- Implementation pseudocode:

```java
public Long resolveNamespaceIdForReport(@NotNull @Valid DdcAiScopeDTO scope) { return resolveNamespaceId(scope, Permission.REPORT); }
public void assertCanReport(@NotNull @Valid DdcAiResourceBO resource) { assertResourceScope(resource, Permission.REPORT); }
private void requireNamespacePermission(String namespacePublicId, Permission permission, DdcAiScopeDTO scope) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication != null) { if (permission == Permission.REPORT) throw scopeForbidden(); /* 既有用户分支逐行保留 */ return; }
    if (DdcAiSystemActor.current().isPresent()) { if (MDC.get("tenantId") == null) throw scopeForbidden(); return; }
    DdcServicePrincipal principal = currentPrincipalOrForbidden();
    if (!"SDK".equals(principal.clientType()) || !Objects.equals(MDC.get("tenantId"), Long.toString(principal.tenantId())) || !Objects.equals(principal.bizCode(), scope.bizCode())
        || !Objects.equals(principal.env(), scope.env()) || !Objects.equals(principal.appCode(), scope.appCode()) || !principal.allowedOperations().contains(permission.rpcOperation()) || !namespaceAllowed(principal, scope)) throw scopeForbidden();
}
private enum Permission { READ("AI_READ"), WRITE("AI_WRITE"), PUBLISH("AI_PUBLISH"), REPORT("AI_REPORT"); private final String rpcOperation; String rpcOperation() { return rpcOperation; } }
```

- Verification contribution: File 1 全部用例。
- After this file: File 1 GREEN。

#### File 6 — `CREATE egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/java/top/egon/cola/component/tianshu/admin/config/DdcAiOptimizationProperties.java`

- Purpose: `egon.cola.component.tianshu.admin.ai` 绑定类（嵌套 `Optimization/Judge/Model/Feedback`）。
- Symbols: `@ConfigurationProperties(prefix="egon.cola.component.tianshu.admin.ai") @Validated @Data class DdcAiOptimizationProperties`；`Optimization{boolean enabled=false; Duration scanInterval=60s; Duration runLease=10m; @Min(1)@Max(16) int maxConcurrentRuns=2; @Min(1)@Max(200) int sampleLimit=20}`；`Judge{Duration scanInterval=30s; @Min(1)@Max(500) int batchSize=50; @Min(1)@Max(10) int maxAttempts=3; @Min(1024)@Max(262144) int maxContextBytes=16384; Duration lease=5m}`；`Model{URI baseUrl; URI resourceUri; String registrationId; String appId; List<String> scopes; Duration connectTimeout=3s; Duration readTimeout=120s; @Min(1)@Max(32768) int maxOutputTokens=8192; @DecimalMin("0")@DecimalMax("2") double temperature=0.2}`；`Feedback{Duration retention=30d; Duration retentionScanInterval=1h; @Min(1)@Max(100) int maxBatch=100; Duration maxClockSkew=5m; @Min(256)@Max(8192) int maxInputExcerptBytes=8192; @Min(256)@Max(32768) int maxOutputExcerptBytes=32768}`
- Repository evidence: `DdcAdminProperties`（同前缀父节 `egon.cola.component.tianshu.admin`，用 getter/setter）；lombok 在 admin 模块可用（`lombok.config` 位于 `egon-cola-xingyuan/egon-cola-tianshu/lombok.config`）。
- Dependencies and consumers: `DdcAiFeedbackServiceImpl`、作业、模型适配器、`DdcAiOptimizationConfiguration`。
- Why now: Service 读取上限。
- Contract/signature changes: 新 Bean（由 `@EnableConfigurationProperties` 在 Step 7 配置类登记；本步用 `@ConfigurationPropertiesScan` 既有机制或在 `DdcAiTransactionConfiguration` 旁登记——按既有 `DdcAdminProperties` 的登记方式复制，PLAN-CLAR-010）。
- Input/output and state mapping: 键→字段，Duration 用 Spring 转换（`60s`、`30d`）。
- Error and edge behavior: `@Validated` 越界启动失败；`model.baseUrl` 为空时适配器在启用通道时拒绝启动（Step 7）。
- Standards impact: MC-CONFIG-001, MC-MODEL-001, MC-VALID-001, MC-TIME-001。
- Literal rule enforcement: Rule 3（class + `@Data`）、Rule 2、Rule 7、Rule 10（`Duration`）、Rule 11。
- Implementation pseudocode:

```java
@ConfigurationProperties(prefix = "egon.cola.component.tianshu.admin.ai") @Validated
@Data @NoArgsConstructor @AllArgsConstructor @Accessors(chain = true) @Builder
public class DdcAiOptimizationProperties {
    @NotNull @Valid private Optimization optimization = new Optimization(); @NotNull @Valid private Judge judge = new Judge(); @NotNull @Valid private Model model = new Model(); @NotNull @Valid private Feedback feedback = new Feedback();
    @Data @NoArgsConstructor @AllArgsConstructor @Accessors(chain = true) @Builder public static class Judge { @NotNull private Duration scanInterval = Duration.ofSeconds(30); @Min(1) @Max(500) private int batchSize = 50; @Min(1) @Max(10) private int maxAttempts = 3; @Min(1024) @Max(262144) private int maxContextBytes = 16384; @NotNull private Duration lease = Duration.ofMinutes(5); }
    /* Optimization / Model / Feedback 同构，默认值见 Symbols */
}
```

- Verification contribution: File 3。
- After this file: 绑定类就绪。

#### File 7 — `MODIFY egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/resources/application.yml`

- Purpose: 三份 profile 增加 `admin.ai.*` 同构键（合并块：`application.yml`、`application-local.yml`、`application-test.yml`）并在 runtime 凭据 `allowed-operations` 追加 `AI_REPORT`。
- Symbols: `egon.cola.component.tianshu.admin.ai.{optimization,judge,model,feedback}` 共 26 键；`rpc.credentials[runtime].allowed-operations: ...,AI_READ,AI_REPORT`
- Repository evidence: `application.yml:176` `admin:` 节；`:208` runtime 凭据 `allowed-operations` 以 `AI_READ` 结尾。
- Dependencies and consumers: File 6；拦截器（Step 6）。
- Why now: 键一致性门禁。
- Contract/signature changes: 配置追加。
- Input/output and state mapping: 值按主 Spec §15.4（`application.yml` 用环境变量占位；local 用本地值；test 用 `http://127.0.0.1:1` 与占位值）。
- Error and edge behavior: 三份文件键结构必须一致（File 3 断言）。
- Standards impact: MC-CONFIG-001。
- Literal rule enforcement: Rule 7、Rule 11。
- Implementation pseudocode:

```yaml
egon.cola.component.tianshu.admin:
  ai:
    optimization: { enabled: ${TIANSHU_AI_OPTIMIZATION_ENABLED:false}, scan-interval: 60s, run-lease: 10m, max-concurrent-runs: 2, sample-limit: 20 }
    judge: { scan-interval: 30s, batch-size: 50, max-attempts: 3, max-context-bytes: 16384, lease: 5m }
    model: { base-url: ${TIANSHU_AI_MODEL_BASE_URL:}, resource-uri: ${TIANSHU_AI_MODEL_RESOURCE_URI:}, registration-id: ${TIANSHU_AI_MODEL_REGISTRATION_ID:}, app-id: ${TIANSHU_AI_MODEL_APP_ID:}, scopes: ${TIANSHU_AI_MODEL_SCOPES:}, connect-timeout: 3s, read-timeout: 120s, max-output-tokens: 8192, temperature: 0.2 }
    feedback: { retention: 30d, retention-scan-interval: 1h, max-batch: 100, max-clock-skew: 5m, max-input-excerpt-bytes: 8192, max-output-excerpt-bytes: 32768 }
  rpc.credentials[runtime].allowed-operations: SDK_REGISTER,SDK_HEARTBEAT,SDK_OFFLINE,CONFIG_PULL,PUBLISH_ACK,AI_READ,AI_REPORT
# application-local.yml：同键，max-concurrent-runs: 1，model.base-url 为本地玉衡地址；application-test.yml：同键，max-concurrent-runs: 1，model.base-url: http://127.0.0.1:1，registration-id/app-id/scopes 为占位值
```

- Verification contribution: File 3。
- After this file: File 3 GREEN。

#### File 8 — `CREATE egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/java/top/egon/cola/component/tianshu/admin/model/command/DdcAiUsageReportCommand.java`

- Purpose: RPC-003 命令（合并块：`DdcAiUsageReportCommand`、`DdcAiUsageReportBatchCommand`）。
- Symbols: `DdcAiUsageReportCommand{usageId, namespaceCode, resourceKey, resourceId, versionId, versionNo, outcome(DdcAiOutcomeEnum), explicitScore, inputExcerpt, outputExcerpt, latencyMs, occurredAt(Instant), traceId}`；`DdcAiUsageReportBatchCommand{@NotEmpty @Size(max=100) List<@Valid DdcAiUsageReportCommand> reports}`
- Repository evidence: `AJ/model/command/DdcAiPublishCommand.java`（class + Lombok + Jakarta 约束 + `DdcAiValidationGroups`）。
- Dependencies and consumers: Provider（Step 6）、Service。
- Why now: Service 签名。
- Contract/signature changes: 新类。
- Input/output and state mapping: 字段同 SDK `DdcAiUsageReportDTO`，但 Admin 自有类（不跨层复用 SDK DTO）。
- Error and edge behavior: `usageId @Pattern`、`resourceId/versionId` 为十进制正整数字符串（与既有 `LONG_ID_PATTERN` 一致）、`explicitScore @Min(0)@Max(100)`、`latencyMs @Min(0)`、`traceId @Size(max=64)`。
- Standards impact: MC-NAME-001, MC-VALID-001, MC-MODEL-001。
- Literal rule enforcement: Rule 1（Command 后缀）、Rule 2、Rule 3、Rule 10、Rule 11。
- Implementation pseudocode:

```java
@Data @NoArgsConstructor @AllArgsConstructor @Accessors(chain = true) @Builder
public class DdcAiUsageReportCommand {
    @NotBlank @Pattern(regexp = "^[A-Za-z0-9_-]{16,64}$") private String usageId;
    @NotBlank @Size(max = 128) private String namespaceCode; @NotBlank @Size(max = 128) private String resourceKey;
    @NotBlank @Pattern(regexp = "^[1-9][0-9]{0,18}$") private String resourceId; @NotBlank @Pattern(regexp = "^[1-9][0-9]{0,18}$") private String versionId; @NotNull @Positive private Long versionNo;
    @NotNull private DdcAiOutcomeEnum outcome; @Min(0) @Max(100) private Integer explicitScore; private String inputExcerpt; private String outputExcerpt; @Min(0) private Integer latencyMs;
    @NotNull private Instant occurredAt; @Size(max = 64) private String traceId;
}
```

- Verification contribution: File 2。
- After this file: 编译通过。

#### File 9 — `CREATE egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/java/top/egon/cola/component/tianshu/admin/model/query/DdcAiUsageFeedbackPageQuery.java`

- Purpose: API-017 查询对象。
- Symbols: `resourceId(Long)`、`versionId(Long, nullable)`、`outcome(DdcAiOutcomePersistenceEnum, nullable)`、`judgeState(DdcAiJudgeStatePersistenceEnum, nullable)`、`from/to(Instant, nullable)`、`pageNo/pageSize`
- Repository evidence: `DdcAiPublicationPageQuery`（class + Lombok + Query 组）。
- Dependencies and consumers: Service、Controller（Step 12）。
- Why now: Service 签名。
- Contract/signature changes: 新类。
- Input/output and state mapping: Controller 从 query string 绑定。
- Error and edge behavior: `pageSize @Max(200)`；`from < to` 由 Service 校验（56301）。
- Standards impact: MC-NAME-001, MC-VALID-001。
- Literal rule enforcement: Rule 1（Query 后缀）、Rule 2、Rule 3、Rule 11。
- Implementation pseudocode:

```java
@Data @NoArgsConstructor @AllArgsConstructor @Accessors(chain = true) @Builder
public class DdcAiUsageFeedbackPageQuery {
    @NotNull(groups = Query.class) @Positive(groups = Query.class) private Long resourceId; @Positive(groups = Query.class) private Long versionId;
    private DdcAiOutcomePersistenceEnum outcome; private DdcAiJudgeStatePersistenceEnum judgeState; private Instant from; private Instant to;
    @NotNull(groups = Query.class) @Min(value = 1, groups = Query.class) private Integer pageNo; @NotNull(groups = Query.class) @Min(value = 1, groups = Query.class) @Max(value = 200, groups = Query.class) private Integer pageSize;
}
```

- Verification contribution: File 2 分页用例。
- After this file: 编译通过。

#### File 10 — `CREATE egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/java/top/egon/cola/component/tianshu/admin/model/vo/DdcAiUsageReportReceiptResult.java`

- Purpose: Result 载体（合并块：`DdcAiUsageReportReceiptResult`（嵌套 `ItemResult{usageId,status,reason}`）、`DdcAiUsageFeedbackResult`）。
- Symbols: `DdcAiUsageFeedbackResult{id(String), resourceId(String), versionId(String), usageId, reporterCredentialId, bizCode, envCode, appCode, outcome, explicitScore, judgeScore, judgeState, judgeAttempts, judgedAt, judgeError, inputExcerpt, outputExcerpt, latencyMs, occurredAt, traceId, createTime}`
- Repository evidence: `DdcAiPublicationResult`（ID 以十进制字符串输出，`Instant` 直接 Jackson 序列化）。
- Dependencies and consumers: Controller、Provider。
- Why now: Service 返回值。
- Contract/signature changes: 新类。
- Input/output and state mapping: Long→String 由 Result 转换器完成。
- Error and edge behavior: 摘录原样输出（页面 READ 权限）。
- Standards impact: MC-NAME-001, MC-JSON-001, MC-MODEL-001。
- Literal rule enforcement: Rule 1（Result 为既有 VO 包命名约定）、Rule 3、Rule 6、Rule 11。
- Implementation pseudocode:

```java
@Data @NoArgsConstructor @AllArgsConstructor @Accessors(chain = true) @Builder
public class DdcAiUsageReportReceiptResult {
    @NotNull @Size(max = 100) private List<@Valid ItemResult> items;
    @Data @NoArgsConstructor @AllArgsConstructor @Accessors(chain = true) @Builder
    public static class ItemResult { @NotBlank private String usageId; @NotNull private DdcAiUsageReportStatusEnum status; @Size(max = 64) private String reason; }
}
```

- Verification contribution: File 2。
- After this file: 编译通过。

#### File 11 — `CREATE egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/java/top/egon/cola/component/tianshu/admin/model/dto/DdcAiJudgeVerdictDTO.java`

- Purpose: 评分裁决不可变值对象。
- Symbols: `public record DdcAiJudgeVerdictDTO(@Min(0) @Max(100) int score, @NotNull @Size(max = 512) String rationale)`
- Repository evidence: 既有 `DdcAiScopeDTO` 为 record 值对象（base starter）。
- Dependencies and consumers: `DdcAiJudgeStrategy`（Step 8）、`DdcAiFeedbackService#completeJudge`。
- Why now: Service 签名。
- Contract/signature changes: 新 record。
- Input/output and state mapping: 由 Strategy `parse` 产出。
- Error and edge behavior: 紧凑构造器裁剪 rationale 到 512 字符。
- Standards impact: MC-MODEL-001（record 仅用于不可变值对象）。
- Literal rule enforcement: Rule 3（record 豁免条款）、Rule 1、Rule 11。
- Implementation pseudocode:

```java
public record DdcAiJudgeVerdictDTO(@Min(0) @Max(100) int score, @NotNull @Size(max = 512) String rationale) {
    public DdcAiJudgeVerdictDTO {
        if (score < 0 || score > 100) throw new IllegalArgumentException("score out of range");
        rationale = rationale == null ? "" : (rationale.length() > 512 ? rationale.substring(0, 512) : rationale);
    }
}
```

- Verification contribution: Step 8 Strategy 测试。
- After this file: 编译通过。

#### File 12 — `CREATE egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/java/top/egon/cola/component/tianshu/admin/model/convertor/DdcAiUsageFeedbackResultConverter.java`

- Purpose: BO→Result 不可逆投影。
- Symbols: `@Mapper(componentModel="spring", uses=DdcAiEnumConverter.class, unmappedTargetPolicy=ERROR) interface DdcAiUsageFeedbackResultConverter extends BaseForwardConverter<DdcAiUsageFeedbackBO, DdcAiUsageFeedbackResult>`；`@Mapping(target="id", source="id", qualifiedByName="longToDecimal")`
- Repository evidence: `DdcAiResourceResultConverter`（`BaseForwardConverter` + `longToDecimal`）。
- Dependencies and consumers: Service。
- Why now: Service 返回。
- Contract/signature changes: 新 Mapper。
- Input/output and state mapping: Long ID→十进制字符串。
- Error and edge behavior: 无。
- Standards impact: MC-CONVERT-001。
- Literal rule enforcement: Rule 3（`BaseForwardConverter`）、Rule 11。
- Implementation pseudocode:

```java
@Mapper(componentModel = "spring", uses = DdcAiEnumConverter.class, unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface DdcAiUsageFeedbackResultConverter extends BaseForwardConverter<DdcAiUsageFeedbackBO, DdcAiUsageFeedbackResult> {
    @Override @Mapping(target = "id", source = "id", qualifiedByName = "longToDecimal") @Mapping(target = "resourceId", source = "resourceId", qualifiedByName = "longToDecimal")
    @Mapping(target = "versionId", source = "versionId", qualifiedByName = "longToDecimal") DdcAiUsageFeedbackResult toTarget(DdcAiUsageFeedbackBO source);
}
```

- Verification contribution: File 2 分页用例。
- After this file: 编译通过。

#### File 13 — `CREATE egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/java/top/egon/cola/component/tianshu/admin/service/ai/DdcAiFeedbackService.java`

- Purpose: 反馈服务接口。
- Symbols: `DdcAiUsageReportReceiptResult accept(@NotNull @Valid DdcAiUsageReportBatchCommand)`；`List<DdcAiUsageFeedbackBO> claimForJudge(Set<Long> resourceIds, int limit)`；`void completeJudge(Long id, Long version, DdcAiJudgeVerdictDTO verdict)`；`void failJudge(Long id, Long version, String errorCode)`；`DdcAiScoreAggregateBO aggregate(Long resourceId, Long versionId, Instant from, Instant to)`；`List<DdcAiUsageFeedbackBO> worstSamples(Long resourceId, Long versionId, Instant from, Instant to, int limit)`；`DdcPageResult<DdcAiUsageFeedbackResult> pageFeedback(@Valid DdcAiUsageFeedbackPageQuery)`；`int purgeExpired(int limit)`
- Repository evidence: `DdcAiResourceService` 接口 `@Validated` + 组注解写法。
- Dependencies and consumers: Provider、作业、Controller。
- Why now: 实现前置。
- Contract/signature changes: 新接口。
- Input/output and state mapping: 见 Impl。
- Error and edge behavior: `pageFeedback` 先 `assertCanRead(resource)`。
- Standards impact: MC-NAME-001, MC-VALID-001。
- Literal rule enforcement: Rule 2、Rule 11。
- Implementation pseudocode:

```java
@Validated public interface DdcAiFeedbackService {
    DdcAiUsageReportReceiptResult accept(@NotNull @Valid DdcAiUsageReportBatchCommand command);
    List<DdcAiUsageFeedbackBO> claimForJudge(@NotEmpty Set<@Positive Long> resourceIds, @Min(1) @Max(500) int limit);
    void completeJudge(@NotNull @Positive Long id, @NotNull @Min(0) Long version, @NotNull @Valid DdcAiJudgeVerdictDTO verdict);
    @Validated(EgonColaModelValidationGroups.Query.class) DdcPageResult<DdcAiUsageFeedbackResult> pageFeedback(@NotNull @Valid DdcAiUsageFeedbackPageQuery query);
    int purgeExpired(@Min(1) @Max(5000) int limit);
}
```

- Verification contribution: File 2。
- After this file: 编译通过。

#### File 14 — `CREATE egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/java/top/egon/cola/component/tianshu/admin/service/ai/impl/DdcAiFeedbackServiceImpl.java`

- Purpose: 反馈服务实现。
- Symbols: `@Service("ddcAiFeedbackService")`；注入 `@Qualifier("ddcAiUsageFeedbackRepository")`、`@Qualifier("ddcAiResourceRepository")`、`@Qualifier("ddcAiVersionRepository")`、`@Qualifier("ddcAiOptimizationPolicyRepository")`、`@Qualifier("ddcAiScopeValidator")`、`@Qualifier("ddcAiUsageFeedbackResultConverterImpl")`、`@Qualifier("ddcAiCommandTransactionTemplate")`、`@Qualifier("ddcAiReadTransactionTemplate")`、`DdcAiOptimizationProperties`、`@Qualifier("ddcPublishClock") Clock`
- Repository evidence: `DdcAiResourceServiceImpl`（`ddcAiCommandTransactionTemplate` REQUIRES_NEW；`resourceRepository.findActiveById`；`DdcServicePrincipal.current()`）。
- Dependencies and consumers: Provider、作业、Controller。
- Why now: 核心业务。
- Contract/signature changes: 新类。
- Input/output and state mapping: `accept`：取主体（credentialId/biz/env/app）；逐条：解析 `resourceId` 字符串→Long→`resourceRepository.findActiveById` 且 `namespaceCode/resourceKey` 一致→`scopeValidator.assertCanReport(resource)`→`versionRepository.findActiveById(versionId)` 且 `resourceId` 一致且 `versionNo` 一致否则 `REJECTED("VERSION_NOT_OF_RESOURCE")`；`occurredAt ∈ [now - feedback.retention, now + feedback.maxClockSkew]` 否则 `REJECTED("OCCURRED_AT_OUT_OF_WINDOW")`；截断摘录到字节上限（UTF-8 安全截断，去除 U+0000）；`judgeState = explicitScore != null ? NOT_REQUIRED : PENDING`；每条在独立 `commandTransactionTemplate.execute` 中 `insertFeedback`，捕获 `DataIntegrityViolationException`→`ACCEPTED_DUPLICATE`；回执同序。
- Error and edge behavior: 作用域失败（56307）→该条 `REJECTED("SCOPE_FORBIDDEN")` 不抛出；资源不存在→`REJECTED("RESOURCE_NOT_FOUND")`；`completeJudge/failJudge` CAS 0 行只记 DEBUG；`purgeExpired` 用 `clock.instant().minus(retention)` 与 `LocalDateTime.now(ZoneOffset.UTC)`（既有 Legacy UTC `deleted_at` 约定）。
- Standards impact: MC-LOG-001, MC-BEAN-001, MC-UTIL-001（截断用 JDK `CharsetEncoder` 或 commons-lang3 `StringUtils`）、MC-TIME-001、MC-PATTERN-001（Simple 流程，直接实现）。
- Literal rule enforcement: Rule 4、Rule 2、Rule 5、Rule 10、Rule 11。
- Implementation pseudocode:

```java
@Slf4j @Service("ddcAiFeedbackService") @RequiredArgsConstructor(onConstructor_ = @Autowired) @Validated
public class DdcAiFeedbackServiceImpl implements DdcAiFeedbackService {
    @Override public DdcAiUsageReportReceiptResult accept(DdcAiUsageReportBatchCommand command) {
        DdcServicePrincipal principal = DdcServicePrincipal.current(); Instant now = clock.instant();
        List<ItemResult> items = new ArrayList<>(command.getReports().size());
        for (DdcAiUsageReportCommand report : command.getReports()) items.add(acceptOne(principal, report, now));
        return DdcAiUsageReportReceiptResult.builder().items(items).build();
    }
    private ItemResult acceptOne(DdcServicePrincipal principal, DdcAiUsageReportCommand report, Instant now) {
        if (report.getOccurredAt().isBefore(now.minus(properties.getFeedback().getRetention())) || report.getOccurredAt().isAfter(now.plus(properties.getFeedback().getMaxClockSkew()))) return rejected(report, "OCCURRED_AT_OUT_OF_WINDOW");
        try {
            return commandTransactionTemplate.execute(status -> {
                DdcAiResourceBO resource = resourceRepository.findActiveById(Long.parseLong(report.getResourceId())).filter(r -> r.getResourceKey().equals(report.getResourceKey())).orElse(null);
                if (resource == null) return rejected(report, "RESOURCE_NOT_FOUND");
                scopeValidator.assertCanReport(resource);                       // 56307 → catch → SCOPE_FORBIDDEN
                DdcAiVersionBO version = versionRepository.findActiveById(Long.parseLong(report.getVersionId())).filter(v -> v.getResourceId().equals(resource.getId()) && v.getVersionNo().equals(report.getVersionNo())).orElse(null);
                if (version == null) return rejected(report, "VERSION_NOT_OF_RESOURCE");
                feedbackRepository.insertFeedback(toBo(principal, report, resource, version, truncate(report.getInputExcerpt(), properties.getFeedback().getMaxInputExcerptBytes()), truncate(report.getOutputExcerpt(), properties.getFeedback().getMaxOutputExcerptBytes())));
                return accepted(report, DdcAiUsageReportStatusEnum.ACCEPTED);
            });
        } catch (DataIntegrityViolationException duplicate) { return accepted(report, DdcAiUsageReportStatusEnum.ACCEPTED_DUPLICATE); }
          catch (CommonException forbidden) { return rejected(report, forbidden.getCode() == 56307 ? "SCOPE_FORBIDDEN" : "INVALID_REPORT"); }
    }
    @Override public void completeJudge(Long id, Long version, DdcAiJudgeVerdictDTO verdict) { if (!feedbackRepository.completeJudge(id, version, (short) verdict.score(), clock.instant())) log.debug("AI_JUDGE_CAS_MISS id={}", id); }
}
```

- Verification contribution: File 2 全部用例。
- After this file: 本步 GREEN。

- Validation working directory: 仓库根
- Verification command: `./mvnw -pl egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin -Dtest=DdcAiScopeValidatorAuthorityTest,DdcAiFeedbackServiceTest,DdcAiOptimizationPropertiesTest,DdcAiVersionServiceTest -Dsurefire.failIfNoSpecifiedTests=false test`
- Expected result: 四测试类通过（既有 `DdcAiVersionServiceTest` 证明用户分支不变）。
- Failure returns to: File 5（权限分支）、File 14（accept 规则）、File 7（键一致）。
- Completion criteria: 四操作矩阵、accept 五条规则、26 键三 profile 一致均有断言。
- Rollback: 回退本步 Commit paths；yml 删除 `ai:` 节与 `AI_REPORT`。
- Commit paths: egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/test/java/top/egon/cola/component/tianshu/admin/service/ai/DdcAiScopeValidatorAuthorityTest.java, egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/test/java/top/egon/cola/component/tianshu/admin/service/ai/DdcAiFeedbackServiceTest.java, egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/test/java/top/egon/cola/component/tianshu/admin/config/DdcAiOptimizationPropertiesTest.java, egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/java/top/egon/cola/component/tianshu/admin/service/ai/validation/DdcAiSystemActor.java, egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/java/top/egon/cola/component/tianshu/admin/service/ai/validation/DdcAiScopeValidator.java, egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/java/top/egon/cola/component/tianshu/admin/config/DdcAiOptimizationProperties.java, egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/resources/application.yml, egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/resources/application-local.yml, egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/resources/application-test.yml, egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/java/top/egon/cola/component/tianshu/admin/model/command/DdcAiUsageReportCommand.java, egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/java/top/egon/cola/component/tianshu/admin/model/command/DdcAiUsageReportBatchCommand.java, egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/java/top/egon/cola/component/tianshu/admin/model/query/DdcAiUsageFeedbackPageQuery.java, egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/java/top/egon/cola/component/tianshu/admin/model/vo/DdcAiUsageReportReceiptResult.java, egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/java/top/egon/cola/component/tianshu/admin/model/vo/DdcAiUsageFeedbackResult.java, egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/java/top/egon/cola/component/tianshu/admin/model/dto/DdcAiJudgeVerdictDTO.java, egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/java/top/egon/cola/component/tianshu/admin/model/convertor/DdcAiUsageFeedbackResultConverter.java, egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/java/top/egon/cola/component/tianshu/admin/service/ai/DdcAiFeedbackService.java, egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/java/top/egon/cola/component/tianshu/admin/service/ai/impl/DdcAiFeedbackServiceImpl.java
- Commit: `feat(tianshu): ai usage feedback service with system actor authorization`

### Step 6 — RPC-003 服务端入口：作用域提取、拦截器与 Provider

- Requirements: `REQ-002`, `REQ-011`
- Dependencies: Step 2（adapter 已安装）、Step 5
- Baseline state: `DdcRpcScopeExtractor.extract` 的 AI 分支只有 `GetPrompt/GetSkill`→`aiReadScope`；`DdcRpcServerInterceptor` 仅对 `AI_READ` 做 namespace 校验、`write()` default→true；`DdcAiResourceRpcProvider` 两方法；`DdcAiResourceRpcProviderTest` 以 `new DdcAiResourceRpcProvider(service, new DdcAiProtoConverterImpl())` 构造。
- Observable outcome: `ReportUsage` 经 HMAC 认证→`AI_REPORT` 凭据与 namespace 模式校验→Provider→`DdcAiFeedbackService#accept`→proto 回执；其余三方法在本步返回 `UNIMPLEMENTED`（Step 13 替换）。
- End state: 下游可上报；Admin 编译完整实现 `DdcAiResourceRpc` 六方法。
- Test-first gate: Required — `DdcRpcServerInterceptorTest` 新用例（AI_REPORT 属写操作、四 AI 操作 namespace 拒绝）与 `DdcAiResourceRpcProviderTest#reportUsageDelegatesToFeedbackServiceAndPreservesOrder` RED。
- Manual Checks: MC-ARCH-001, MC-REUSE-001, MC-NAME-001, MC-VALID-001, MC-CONVERT-001, MC-LOG-001, MC-BEAN-001, MC-SCOPE-001, MC-TEST-001
- Literal Rules: Rule 1, Rule 2, Rule 3, Rule 4, Rule 11
- Ordered files:

#### File 1 — `MODIFY egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/test/java/top/egon/cola/component/tianshu/admin/security/rpc/DdcRpcServerInterceptorTest.java`

- Purpose: 固定新操作的分类与 namespace 校验。
- Symbols: `treatsAiReportWriteAndPublishAsWriteOperations`、`deniesAiMethodsOutsideCredentialNamespacePatterns`（参数化四方法）、`acceptsReportUsageBatchWhenEveryNamespaceMatches`
- Repository evidence: 既有 `rejectsWrongOperationScopeAndUnknownMethodBeforeFacade`、`failsClosedForNonceStoreOutageOnWriteRequests`（构造 nonce store 故障验证 write 分类）。
- Dependencies and consumers: File 3/4。
- Why now: RED。
- Contract/signature changes: 无。
- Input/output and state mapping: 构造 `AiUsageReportBatch`（两条 namespace `ns-a`、`ns-b`）；凭据 namespace 模式 `ns-a*`→`PERMISSION_DENIED`。
- Error and edge behavior: 空批→`INVALID_ARGUMENT`。
- Standards impact: MC-TEST-001。
- Literal rule enforcement: Rule 11。
- Implementation pseudocode:

```java
@ParameterizedTest @ValueSource(strings = {"ReportUsage", "GetVersionHead", "ReplaceSkillDraft", "PublishSkillDraft"})
void deniesAiMethodsOutsideCredentialNamespacePatterns(String method) {
    Message request = switch (method) { case "ReportUsage" -> batch("ns-a", "ns-zzz"); case "GetVersionHead" -> aiQuery("ns-zzz"); case "ReplaceSkillDraft" -> upload("ns-zzz"); default -> publish("ns-zzz"); };
    assertThatThrownBy(() -> interceptor.authenticate(AI + method, request, signedHeaders(method, request))).extracting(STATUS_CODE).isEqualTo(Status.Code.PERMISSION_DENIED);
}
@Test void treatsAiReportWriteAndPublishAsWriteOperations() { nonceStoreDown(); for (String m : List.of("ReportUsage", "ReplaceSkillDraft", "PublishSkillDraft")) assertThatThrownBy(() -> interceptor.authenticate(AI + m, requestFor(m), headers(m))).extracting(STATUS_CODE).isEqualTo(Status.Code.UNAVAILABLE); }
```

- Verification contribution: RED→GREEN。
- After this file: RED。

#### File 2 — `MODIFY egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/test/java/top/egon/cola/component/tianshu/admin/rpc/provider/DdcAiResourceRpcProviderTest.java`

- Purpose: TEST-001 Provider 侧。
- Symbols: `reportUsageDelegatesToFeedbackServiceAndPreservesOrder`、`reportUsageRejectsOversizedBatchWithFeedbackInvalidTrailer`、`reportUsageRequiresAiReportPrincipal`、`unimplementedSyncMethodsUntilStep13`（Step 13 删除）
- Repository evidence: 既有 `promptReadDelegatesToTheSinglePublishedVersionServiceQuery`、`knownAiScopeFailureKeepsPermissionStatusAndTypedTrailer`。
- Dependencies and consumers: File 5/6。
- Why now: RED。
- Contract/signature changes: 构造函数增加 `DdcAiFeedbackService` 与 `DdcAiSyncCommandConverterImpl` 参数（测试同步更新 `new DdcAiResourceRpcProvider(service, feedbackService, new DdcAiProtoConverterImpl(), new DdcAiSyncCommandConverterImpl())`）。
- Input/output and state mapping: 101 条→`INVALID_ARGUMENT` + trailer `TIANSHU_AI_FEEDBACK_INVALID`。
- Error and edge behavior: 主体 `allowedOperations` 无 `AI_REPORT`→`PERMISSION_DENIED`。
- Standards impact: MC-TEST-001。
- Literal rule enforcement: Rule 11。
- Implementation pseudocode:

```java
@Test void reportUsageDelegatesToFeedbackServiceAndPreservesOrder() {
    when(feedbackService.accept(any())).thenReturn(receipt(item("u1", ACCEPTED), item("u2", ACCEPTED_DUPLICATE), item("u3", REJECTED, "OCCURRED_AT_OUT_OF_WINDOW")));
    AiUsageReportReceipt receipt = withPrincipal(aiReportPrincipal(), () -> provider.reportUsage(batch("u1", "u2", "u3")));
    assertThat(receipt.getItemsList()).extracting(AiUsageReportItemReceipt::getStatus).containsExactly(AI_USAGE_REPORT_ACCEPTED, AI_USAGE_REPORT_ACCEPTED_DUPLICATE, AI_USAGE_REPORT_REJECTED);
    ArgumentCaptor<DdcAiUsageReportBatchCommand> captor = ArgumentCaptor.forClass(DdcAiUsageReportBatchCommand.class); verify(feedbackService).accept(captor.capture()); assertThat(captor.getValue().getReports()).extracting(DdcAiUsageReportCommand::getUsageId).containsExactly("u1", "u2", "u3");
}
```

- Verification contribution: RED→GREEN。
- After this file: RED（编译失败直到 File 5/6）。

#### File 3 — `MODIFY egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/java/top/egon/cola/component/tianshu/admin/security/rpc/DdcRpcScopeExtractor.java`

- Purpose: 四个新方法的作用域提取；批量上报返回全部 namespace。
- Symbols: `case AI_RESOURCE + "GetVersionHead" -> aiReadScope(query)`；`case AI_RESOURCE + "ReplaceSkillDraft" -> aiNamespaceScope(upload.getNamespaceCode())`；`case AI_RESOURCE + "PublishSkillDraft" -> aiNamespaceScope(request.getNamespaceCode())`；`case AI_RESOURCE + "ReportUsage" -> aiBatchScope(batch)`；`Scope` 新增第六分量 `Set<String> namespaceCodes`（PLAN-CLAR-009：既有五参构造器保持，namespaceCodes 默认 `namespaceCode==null ? Set.of() : Set.of(namespaceCode)`）
- Repository evidence: `aiReadScope(...)` 从 `AiResourceQuery.getScope()` 取 biz/env/app/namespace；`Scope` record 第 222 行。
- Dependencies and consumers: 拦截器 File 4。
- Why now: 拦截器 fail-closed。
- Contract/signature changes: `Scope` 新增分量与 `namespaceCodes()` 访问器；既有构造器签名保留。
- Input/output and state mapping: 上传/发布/上报请求无 biz/env/app（取凭据），clientType=`SDK`。
- Error and edge behavior: 空批或任一 `namespace_code` 空→`IllegalArgumentException`（拦截器映射 INVALID_ARGUMENT）。
- Standards impact: MC-SCOPE-001。
- Literal rule enforcement: Rule 11。
- Implementation pseudocode:

```java
case AI_RESOURCE + "ReportUsage" -> aiBatchScope(type(request, AiUsageReportBatch.class));
case AI_RESOURCE + "GetVersionHead" -> aiReadScope(type(request, AiResourceQuery.class).getScope());
case AI_RESOURCE + "ReplaceSkillDraft" -> aiNamespaceScope(type(request, AiSkillDraftUpload.class).getNamespaceCode());
case AI_RESOURCE + "PublishSkillDraft" -> aiNamespaceScope(type(request, AiSkillPublishRequest.class).getNamespaceCode());
private Scope aiBatchScope(AiUsageReportBatch batch) {
    if (batch.getReportsCount() == 0) throw new IllegalArgumentException("empty usage report batch");
    Set<String> namespaces = batch.getReportsList().stream().map(AiUsageReport::getNamespaceCode).peek(ns -> { if (!text(ns)) throw new IllegalArgumentException("namespace_code required"); }).collect(Collectors.toUnmodifiableSet());
    return new Scope("SDK", null, null, null, namespaces.iterator().next(), namespaces);
}
```

- Verification contribution: File 1。
- After this file: 编译通过。

#### File 4 — `MODIFY egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/java/top/egon/cola/component/tianshu/admin/security/rpc/DdcRpcServerInterceptor.java`

- Purpose: namespace 校验扩展到四个 AI 操作，并按批量 namespace 集合逐一匹配。
- Symbols: `operation.aiScoped()`（在 `DdcRpcOperation` 无此方法，故用本地 `Set<DdcRpcOperation> AI_OPERATIONS = EnumSet.of(AI_READ, AI_REPORT, AI_WRITE, AI_PUBLISH)`）；`permitsNamespaces(patterns, scope.namespaceCodes())`
- Repository evidence: 第 158–161 行；`write()` 第 246–258 行（default→true 已覆盖新操作，无需修改）。
- Dependencies and consumers: 所有 RPC。
- Why now: fail-closed。
- Contract/signature changes: 无。
- Input/output and state mapping: 集合全部匹配才放行。
- Error and edge behavior: `scope.namespaceCodes()` 为空集且操作为 AI→拒绝。
- Standards impact: MC-SCOPE-001。
- Literal rule enforcement: Rule 11。
- Implementation pseudocode:

```java
private static final Set<DdcRpcOperation> AI_OPERATIONS = EnumSet.of(DdcRpcOperation.AI_READ, DdcRpcOperation.AI_REPORT, DdcRpcOperation.AI_WRITE, DdcRpcOperation.AI_PUBLISH);
if (AI_OPERATIONS.contains(operation) && !permitsNamespaces(namespacePatterns, scope.namespaceCodes())) throw denied();
private boolean permitsNamespaces(Set<String> patterns, Set<String> namespaceCodes) {
    return !namespaceCodes.isEmpty() && namespaceCodes.stream().allMatch(code -> permitsNamespace(patterns, code));
}
```

- Verification contribution: File 1。
- After this file: File 1 GREEN。

#### File 5 — `CREATE egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/java/top/egon/cola/component/tianshu/admin/model/convertor/DdcAiSyncCommandConverter.java`

- Purpose: proto↔Admin 命令/Result 转换（PLAN-CLAR-003）；本步实现上报方向，Step 13 追加上传/发布/版本头。
- Symbols: `@Mapper(componentModel="spring", uses=DdcAiEnumConverter.class, unmappedTargetPolicy=ERROR) interface DdcAiSyncCommandConverter extends BaseForwardConverter<AiUsageReportBatch, DdcAiUsageReportBatchCommand>`；`DdcAiUsageReportCommand toCommand(AiUsageReport)`（optional 字段 `has*()`）；`AiUsageReportReceipt toProto(DdcAiUsageReportReceiptResult)`；`@Named("rfc3339ToInstant")`；`@ValueMapping` `AiOutcome→DdcAiOutcomeEnum`、`DdcAiUsageReportStatusEnum→AiUsageReportStatus`
- Repository evidence: `DdcAiProtoConverter`（adapter 内 MapStruct 处理 proto builder 的 `nullValueCheckStrategy`/default 方法写法）。
- Dependencies and consumers: Provider。
- Why now: Provider 前置。
- Contract/signature changes: 新 Mapper。
- Input/output and state mapping: `occurred_at` RFC3339→`Instant.parse`（失败→`IllegalArgumentException`→Provider 映射 56320）。
- Error and edge behavior: `AI_OUTCOME_UNSPECIFIED`→抛出。
- Standards impact: MC-CONVERT-001, MC-TIME-001。
- Literal rule enforcement: Rule 3、Rule 10、Rule 11。
- Implementation pseudocode:

```java
default DdcAiUsageReportCommand toCommand(AiUsageReport r) {
    return DdcAiUsageReportCommand.builder().usageId(r.getUsageId()).namespaceCode(r.getNamespaceCode()).resourceKey(r.getResourceKey()).resourceId(r.getResourceId()).versionId(r.getVersionId()).versionNo(r.getVersionNo())
        .outcome(outcome(r.getOutcome())).explicitScore(r.hasExplicitScore() ? r.getExplicitScore() : null).inputExcerpt(r.hasInputExcerpt() ? r.getInputExcerpt() : null).outputExcerpt(r.hasOutputExcerpt() ? r.getOutputExcerpt() : null)
        .latencyMs(r.hasLatencyMs() ? r.getLatencyMs() : null).occurredAt(rfc3339ToInstant(r.getOccurredAt())).traceId(r.hasTraceId() ? r.getTraceId() : null).build();
}
@Override default DdcAiUsageReportBatchCommand toTarget(AiUsageReportBatch batch) { return DdcAiUsageReportBatchCommand.builder().reports(batch.getReportsList().stream().map(this::toCommand).toList()).build(); }
default AiUsageReportReceipt toProto(DdcAiUsageReportReceiptResult result) { var b = AiUsageReportReceipt.newBuilder(); result.getItems().forEach(i -> { var ib = AiUsageReportItemReceipt.newBuilder().setUsageId(i.getUsageId()).setStatus(status(i.getStatus())); if (i.getReason() != null) ib.setReason(i.getReason()); b.addItems(ib); }); return b.build(); }
```

- Verification contribution: File 2。
- After this file: 编译通过。

#### File 6 — `MODIFY egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/java/top/egon/cola/component/tianshu/admin/rpc/provider/DdcAiResourceRpcProvider.java`

- Purpose: `reportUsage` 实现；其余三方法临时 `UNIMPLEMENTED`。
- Symbols: 新注入 `@Qualifier("ddcAiFeedbackService") DdcAiFeedbackService feedbackService`、`@Qualifier("ddcAiSyncCommandConverterImpl") DdcAiSyncCommandConverter syncConverter`；`reportUsage(AiUsageReportBatch)`；`requirePrincipalOperation("AI_REPORT")`（泛化既有 `requireAiReadPrincipal`）；`getVersionHead/replaceSkillDraft/publishSkillDraft` 抛 `Status.UNIMPLEMENTED`
- Repository evidence: `invoke(String, Invocation)`、`mapAiFailure`、`requireAiReadPrincipal`（第 117–166 行）。
- Dependencies and consumers: gRPC 服务端。
- Why now: RPC-003 可用。
- Contract/signature changes: 构造器参数增加（Lombok 生成）；既有两方法不变。
- Input/output and state mapping: `batch.getReportsCount()` 1..100 且序列化大小 ≤4MiB（由既有 gRPC 入站上限兜底）否则 `INVALID_ARGUMENT` + `TIANSHU_AI_FEEDBACK_INVALID`；`ConstraintViolationException`→同码。
- Error and edge behavior: `accept` 的逐条拒绝不抛异常，整体 OK。
- Standards impact: MC-BEAN-001（`@Qualifier`）、MC-LOG-001、MC-VALID-001。
- Literal rule enforcement: Rule 4、Rule 2、Rule 11。
- Implementation pseudocode:

```java
@Override public AiUsageReportReceipt reportUsage(@NotNull @Valid AiUsageReportBatch batch) {
    return invoke("ReportUsage", () -> {
        requirePrincipalOperation("AI_REPORT");
        if (batch.getReportsCount() < 1 || batch.getReportsCount() > 100) throw new CommonException(56320, "TIANSHU_AI_FEEDBACK_INVALID", "usage report batch must contain 1..100 reports");
        DdcAiUsageReportBatchCommand command; try { command = syncConverter.toTarget(batch); } catch (IllegalArgumentException | DateTimeParseException invalid) { throw new CommonException(56320, "TIANSHU_AI_FEEDBACK_INVALID", "invalid usage report"); }
        return syncConverter.toProto(feedbackService.accept(command));
    });
}
@Override public AiVersionHead getVersionHead(AiResourceQuery query) { throw Status.UNIMPLEMENTED.withDescription("GetVersionHead").asRuntimeException(); }   // Step 13 替换
```

- Verification contribution: File 2。
- After this file: 本步 GREEN。

- Validation working directory: 仓库根
- Verification command: `./mvnw -pl egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin -Dtest=DdcRpcServerInterceptorTest,DdcAiResourceRpcProviderTest,DdcRpcScopeExtractorTest -Dsurefire.failIfNoSpecifiedTests=false test`
- Expected result: 三测试类通过（`DdcRpcScopeExtractorTest` 若存在则既有用例不变）。
- Failure returns to: File 3（提取）、File 6（Provider）。
- Completion criteria: 四 AI 方法 namespace fail-closed；上报回执同序。
- Rollback: 回退本步 Commit paths。
- Commit paths: egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/test/java/top/egon/cola/component/tianshu/admin/security/rpc/DdcRpcServerInterceptorTest.java, egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/test/java/top/egon/cola/component/tianshu/admin/rpc/provider/DdcAiResourceRpcProviderTest.java, egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/java/top/egon/cola/component/tianshu/admin/security/rpc/DdcRpcScopeExtractor.java, egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/java/top/egon/cola/component/tianshu/admin/security/rpc/DdcRpcServerInterceptor.java, egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/java/top/egon/cola/component/tianshu/admin/model/convertor/DdcAiSyncCommandConverter.java, egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/java/top/egon/cola/component/tianshu/admin/rpc/provider/DdcAiResourceRpcProvider.java
- Commit: `feat(tianshu): accept ai usage reports over hmac rpc with namespace guard`

### Step 7 — 模型端口与玉衡 LLM 适配器

- Requirements: `REQ-013`, `REQ-014`
- Dependencies: Step 5（属性类）
- Baseline state: Admin 已依赖 `egon-cola-tianquan-shoubing-starter`（`IdpServiceOAuth2Client`，Bean `idpServiceOAuth2Client`，`authorize(IdpServiceTokenRequest)` 返回 `OAuth2AccessToken` 并内部缓存/续期，`invalidate(request)`）与 Spring `RestClient`；`yuheng-starter` 的 `GatewayReportHttpClient` 用 `HttpClient.newBuilder().connectTimeout` + `JdkClientHttpRequestFactory` + `RestClient.builder()`；无 `DdcAiModelPort`。
- Observable outcome: `YuhengLlmModelAdapterTest`（`MockRestServiceServer`）证明成功解析、401 失效缓存→`MODEL_AUTH`、429→`MODEL_RATE_LIMITED`、超时→`MODEL_TIMEOUT`、非 JSON→`MODEL_ERROR`、token 请求参数（registrationId/appId/audience/PLATFORM/scopes）。
- End state: `ddcAiModelPort` Bean 在 `admin.ai.optimization.enabled=true` 时装配；无调用方。
- Test-first gate: Required — 测试因类缺失 RED。
- Manual Checks: MC-ARCH-001, MC-REUSE-001, MC-DEP-001, MC-NAME-001, MC-VALID-001, MC-MODEL-001, MC-LOG-001, MC-BEAN-001, MC-UTIL-001, MC-JSON-001, MC-TIME-001, MC-CONFIG-001, MC-SCOPE-001, MC-TEST-001
- Literal Rules: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 10, Rule 11
- Ordered files:

#### File 1 — `CREATE egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/test/java/top/egon/cola/component/tianshu/admin/service/ai/model/YuhengLlmModelAdapterTest.java`

- Purpose: TEST-006。
- Symbols: `completesAndParsesChatCompletion`、`unauthorizedInvalidatesTokenAndMapsToModelAuth`、`tooManyRequestsMapsToRateLimited`、`readTimeoutMapsToModelTimeout`、`nonJsonBodyMapsToModelError`、`tokenRequestUsesPlatformContextAndConfiguredScopes`、`neverLogsPromptOrToken`
- Repository evidence: `yuheng-starter/src/test/.../GatewayReportHttpClientTest.java` 使用 `MockRestServiceServer.bindTo(RestClient.Builder)`。
- Dependencies and consumers: File 2–6。
- Why now: RED。
- Contract/signature changes: 无。
- Input/output and state mapping: `DdcAiModelRequestDTO("qwen-plus", system, user, 1024, 0.2, "JUDGE")`→POST `/v1/chat/completions` body `{model, messages[2], temperature, max_tokens, stream:false}` + `Authorization: Bearer tkn`。
- Error and edge behavior: 401 后断言 `verify(oauth2Client).invalidate(request)`；超时用 `ResourceAccessException(new HttpTimeoutException())`。
- Standards impact: MC-TEST-001, MC-JSON-001。
- Literal rule enforcement: Rule 11。
- Implementation pseudocode:

```java
@Test void unauthorizedInvalidatesTokenAndMapsToModelAuth() {
    when(oauth2Client.authorize(any())).thenReturn(token("tkn"));
    server.expect(requestTo("http://yuheng.test/v1/chat/completions")).andExpect(header("Authorization", "Bearer tkn")).andExpect(jsonPath("$.stream").value(false)).andRespond(withStatus(HttpStatus.UNAUTHORIZED));
    assertThatThrownBy(() -> adapter.complete(request("JUDGE"))).isInstanceOf(DdcAiModelException.class).extracting("code").isEqualTo(DdcAiRunErrorCodeEnum.MODEL_AUTH);
    verify(oauth2Client).invalidate(any(IdpServiceTokenRequest.class));
}
@Test void completesAndParsesChatCompletion() { server.expect(anything()).andRespond(withSuccess("{\"choices\":[{\"message\":{\"content\":\"{\\\"score\\\":77}\"},\"finish_reason\":\"stop\"}],\"usage\":{\"prompt_tokens\":10,\"completion_tokens\":5},\"model\":\"qwen-plus\"}", APPLICATION_JSON)); var r = adapter.complete(request("JUDGE")); assertThat(r.text()).contains("77"); assertThat(r.completionTokens()).isEqualTo(5); }
```

- Verification contribution: RED→GREEN。
- After this file: RED。

#### File 2 — `CREATE egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/java/top/egon/cola/component/tianshu/admin/model/dto/DdcAiModelRequestDTO.java`

- Purpose: 模型请求/结果不可变值对象（合并块：`DdcAiModelRequestDTO`、`DdcAiModelResultDTO`）。
- Symbols: `record DdcAiModelRequestDTO(@NotBlank @Pattern(regexp="^[a-z0-9][a-z0-9-]{0,63}$") String modelAlias, @NotBlank String systemPrompt, @NotBlank String userPrompt, @Min(1) @Max(32768) int maxOutputTokens, @DecimalMin("0") @DecimalMax("2") double temperature, @NotBlank @Pattern(regexp="^(JUDGE|OPTIMIZE)$") String purpose)`；`record DdcAiModelResultDTO(@NotBlank String text, Integer promptTokens, Integer completionTokens, String model, String finishReason)`
- Repository evidence: 主 Spec §9.2.15 字段；record 用法同 `DdcAiScopeDTO`。
- Dependencies and consumers: Port、Adapter、Strategy。
- Why now: Port 签名。
- Contract/signature changes: 新 record。
- Input/output and state mapping: 直接映射到 OpenAI 请求体字段。
- Error and edge behavior: 紧凑构造器拒绝空文本。
- Standards impact: MC-MODEL-001, MC-VALID-001, MC-NAME-001。
- Literal rule enforcement: Rule 3（record 仅不可变值对象）、Rule 2、Rule 1、Rule 11。
- Implementation pseudocode:

```java
public record DdcAiModelRequestDTO(String modelAlias, String systemPrompt, String userPrompt, int maxOutputTokens, double temperature, String purpose) {
    public DdcAiModelRequestDTO { Objects.requireNonNull(modelAlias); Objects.requireNonNull(systemPrompt); Objects.requireNonNull(userPrompt); Objects.requireNonNull(purpose); }
    public boolean judge() { return "JUDGE".equals(purpose); }
}
public record DdcAiModelResultDTO(String text, Integer promptTokens, Integer completionTokens, String model, String finishReason) { public boolean truncated() { return "length".equals(finishReason); } }
```

- Verification contribution: File 1。
- After this file: 编译通过。

#### File 3 — `CREATE egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/java/top/egon/cola/component/tianshu/admin/service/ai/DdcAiModelPort.java`

- Purpose: INTERNAL-103 端口。
- Symbols: `@Validated interface DdcAiModelPort { DdcAiModelResultDTO complete(@NotNull @Valid DdcAiModelRequestDTO request); }`
- Repository evidence: `AJ/service/ai/` 既有接口 `DdcAiResourceService` 同包放置。
- Dependencies and consumers: Judge/Optimizer Strategy。
- Why now: Adapter 实现目标。
- Contract/signature changes: 新接口。
- Input/output and state mapping: 1 次调用 1 次 HTTPS。
- Error and edge behavior: 只抛 `DdcAiModelException`。
- Standards impact: MC-NAME-001, MC-VALID-001。
- Literal rule enforcement: Rule 2、Rule 11。
- Implementation pseudocode:

```java
@Validated
public interface DdcAiModelPort {
    /** 一次非流式补全；失败统一为 DdcAiModelException(code)。 */
    DdcAiModelResultDTO complete(@NotNull @Valid DdcAiModelRequestDTO request);
}
```

- Verification contribution: File 1（通过 Adapter）。
- After this file: 编译通过。

#### File 4 — `CREATE egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/java/top/egon/cola/component/tianshu/admin/service/ai/model/DdcAiModelException.java`

- Purpose: 模型调用异常（PLAN-CLAR-004）。
- Symbols: `class DdcAiModelException extends RuntimeException`；`DdcAiRunErrorCodeEnum code`；`int httpStatus`（可空 0）；构造 `(code, message, cause)`
- Repository evidence: `AJ/common/DdcAdminException.java` 为旧通用异常；本异常内部使用不出 Service。
- Dependencies and consumers: Adapter 抛、Strategy/Service 捕获。
- Why now: Adapter 前置。
- Contract/signature changes: 新类。
- Input/output and state mapping: code ∈ MODEL_AUTH/MODEL_TIMEOUT/MODEL_ERROR/MODEL_RATE_LIMITED。
- Error and edge behavior: message 不含正文/token。
- Standards impact: MC-NAME-001。
- Literal rule enforcement: Rule 11。
- Implementation pseudocode:

```java
@Getter public class DdcAiModelException extends RuntimeException {
    private final DdcAiRunErrorCodeEnum code; private final int httpStatus;
    public DdcAiModelException(DdcAiRunErrorCodeEnum code, int httpStatus, String message, Throwable cause) { super(message, cause); this.code = Objects.requireNonNull(code); this.httpStatus = httpStatus; }
}
```

- Verification contribution: File 1。
- After this file: 编译通过。

#### File 5 — `CREATE egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/java/top/egon/cola/component/tianshu/admin/service/ai/model/YuhengLlmModelAdapter.java`

- Purpose: 玉衡 OpenAI 兼容补全适配器。
- Symbols: `public class YuhengLlmModelAdapter implements DdcAiModelPort`（非注解 Bean，由 File 6 以 `@Bean("ddcAiModelPort")` 构造）；构造 `(RestClient client, IdpServiceOAuth2Client oauth2Client, ObjectMapper objectMapper, DdcAiOptimizationProperties.Model model, MeterRegistry meters)`；`complete`；`tokenRequest()`；`classify(RestClientResponseException)`
- Repository evidence: `GatewayReportHttpClient`（`RestClient` + `JdkClientHttpRequestFactory`，`RestClientResponseException` 分类）；`IdpServiceTokenRequest(registrationId, appId, audience, ServiceTokenContext.PLATFORM, null, scopes)`（record 要求 context 非空——PLAN-CLAR-011）。
- Dependencies and consumers: Strategy。
- Why now: 外部调用边界。
- Contract/signature changes: 新类。
- Input/output and state mapping: 请求 JSON 用 Jackson `ObjectNode`；响应 `choices[0].message.content` 非空；`usage.prompt_tokens/completion_tokens` 可空；`finish_reason`。
- Error and edge behavior: `authorize` 抛任何异常→`MODEL_AUTH`；401/403→`invalidate` + `MODEL_AUTH`；408/`HttpTimeoutException`/`ResourceAccessException`(cause timeout)→`MODEL_TIMEOUT`；429→`MODEL_RATE_LIMITED`；其余 4xx/5xx/非 JSON/无 choices/空 content→`MODEL_ERROR`；日志 `AI_MODEL_CALL alias={} purpose={} status={} latencyMs={} promptBytes={} completionBytes={}`。
- Standards impact: MC-LOG-001, MC-UTIL-001（JDK `HttpClient`、Jackson）、MC-JSON-001, MC-TIME-001（`Duration` 超时）、MC-DEP-001（无新依赖）。
- Literal rule enforcement: Rule 4（`@Slf4j`）、Rule 5、Rule 6（Jackson）、Rule 10、Rule 11。
- Implementation pseudocode:

```java
@Override public DdcAiModelResultDTO complete(DdcAiModelRequestDTO request) {
    IdpServiceTokenRequest tokenRequest = new IdpServiceTokenRequest(model.getRegistrationId(), model.getAppId(), model.getResourceUri(), ServiceTokenContext.PLATFORM, null, Set.copyOf(model.getScopes()));
    String token; try { token = oauth2Client.authorize(tokenRequest).getTokenValue(); } catch (RuntimeException failure) { throw new DdcAiModelException(MODEL_AUTH, 0, "service token unavailable", failure); }
    ObjectNode body = objectMapper.createObjectNode().put("model", request.modelAlias()).put("temperature", request.temperature()).put("max_tokens", request.maxOutputTokens()).put("stream", false);
    body.putArray("messages").add(message("system", request.systemPrompt())).add(message("user", request.userPrompt()));
    long started = System.nanoTime();
    try {
        String raw = client.post().uri("/v1/chat/completions").header(HttpHeaders.AUTHORIZATION, "Bearer " + token).contentType(MediaType.APPLICATION_JSON).body(body).retrieve().body(String.class);
        JsonNode root = objectMapper.readTree(raw); JsonNode choice = root.path("choices").path(0); String text = choice.path("message").path("content").asText(null);
        if (text == null || text.isBlank()) throw new DdcAiModelException(MODEL_ERROR, 200, "empty completion", null);
        return new DdcAiModelResultDTO(text, intOrNull(root.at("/usage/prompt_tokens")), intOrNull(root.at("/usage/completion_tokens")), root.path("model").asText(null), choice.path("finish_reason").asText(null));
    } catch (RestClientResponseException http) { if (http.getStatusCode().value() == 401 || http.getStatusCode().value() == 403) oauth2Client.invalidate(tokenRequest); throw new DdcAiModelException(classify(http.getStatusCode().value()), http.getStatusCode().value(), "model gateway status " + http.getStatusCode().value(), http); }
      catch (ResourceAccessException io) { throw new DdcAiModelException(isTimeout(io) ? MODEL_TIMEOUT : MODEL_ERROR, 0, "model gateway unreachable", io); }
      catch (JsonProcessingException bad) { throw new DdcAiModelException(MODEL_ERROR, 200, "model response is not JSON", bad); }
    finally { log.info("AI_MODEL_CALL alias={} purpose={} latencyMs={}", request.modelAlias(), request.purpose(), (System.nanoTime() - started) / 1_000_000); }
}
private static DdcAiRunErrorCodeEnum classify(int status) { return switch (status) { case 401, 403 -> MODEL_AUTH; case 408 -> MODEL_TIMEOUT; case 429 -> MODEL_RATE_LIMITED; default -> MODEL_ERROR; }; }
```

- Verification contribution: File 1 全部用例。
- After this file: File 1 GREEN（Adapter 直接构造）。

#### File 6 — `CREATE egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/java/top/egon/cola/component/tianshu/admin/config/DdcAiOptimizationConfiguration.java`

- Purpose: 优化通道装配（本步：属性登记、`RestClient`、`ddcAiModelPort`、执行器；Step 8/9/11 追加 Judge/Strategy/Scanner Bean）。
- Symbols: `@Configuration(value="ddcAiOptimizationConfiguration", proxyBeanMethods=false) @EnableConfigurationProperties(DdcAiOptimizationProperties.class)`；`@Bean("ddcAiModelRestClient") @ConditionalOnProperty(prefix="egon.cola.component.tianshu.admin.ai.optimization", name="enabled", havingValue="true") RestClient`；`@Bean("ddcAiModelPort") @ConditionalOnProperty(同上) @ConditionalOnMissingBean(DdcAiModelPort.class) DdcAiModelPort`；`@Bean("ddcAiOptimizationExecutor", destroyMethod="shutdown") @ConditionalOnProperty(同上) ExecutorService`（固定线程池 `max-concurrent-runs`，线程名 `tianshu-ai-opt-`）
- Repository evidence: `DdcAiTransactionConfiguration`、`DdcAdminRedisConfig#ddcLeaseExpiryScanner` 的 `@Qualifier` 显式注入；`DdcAdminProperties` 用 `@ConfigurationProperties`（登记方式随既有 `@EnableConfigurationProperties`/扫描，PLAN-CLAR-010）。
- Dependencies and consumers: 作业、Service。
- Why now: 条件装配。
- Contract/signature changes: 新配置类。
- Input/output and state mapping: `model.base-url` 空且通道启用→`IllegalStateException("TIANSHU_AI_MODEL_BASE_URL_REQUIRED")` 启动失败。
- Error and edge behavior: 测试 profile 下通道关闭→三个 Bean 不装配（PLAN-CLAR-007）。
- Standards impact: MC-BEAN-001, MC-CONFIG-001。
- Literal rule enforcement: Rule 4（具名 Bean）、Rule 7、Rule 11。
- Implementation pseudocode:

```java
@Bean("ddcAiModelRestClient") @ConditionalOnProperty(prefix = OPT, name = "enabled", havingValue = "true")
public RestClient ddcAiModelRestClient(@Qualifier("egon.cola.component.tianshu.admin.ai-top.egon.cola.component.tianshu.admin.config.DdcAiOptimizationProperties") DdcAiOptimizationProperties properties) {
    DdcAiOptimizationProperties.Model model = properties.getModel();
    if (model.getBaseUrl() == null) throw new IllegalStateException("TIANSHU_AI_MODEL_BASE_URL_REQUIRED");
    HttpClient http = HttpClient.newBuilder().connectTimeout(model.getConnectTimeout()).build();
    JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(http); factory.setReadTimeout(model.getReadTimeout());
    return RestClient.builder().baseUrl(model.getBaseUrl().toString()).requestFactory(factory).build();
}
@Bean("ddcAiModelPort") @ConditionalOnProperty(prefix = OPT, name = "enabled", havingValue = "true") @ConditionalOnMissingBean(DdcAiModelPort.class)
public DdcAiModelPort ddcAiModelPort(@Qualifier("ddcAiModelRestClient") RestClient client, @Qualifier("idpServiceOAuth2Client") IdpServiceOAuth2Client oauth2Client, @Qualifier("jacksonObjectMapper") ObjectMapper objectMapper, DdcAiOptimizationProperties properties, MeterRegistry meters) { return new YuhengLlmModelAdapter(client, oauth2Client, objectMapper, properties.getModel(), meters); }
@Bean(name = "ddcAiOptimizationExecutor", destroyMethod = "shutdown") @ConditionalOnProperty(prefix = OPT, name = "enabled", havingValue = "true")
public ExecutorService ddcAiOptimizationExecutor(DdcAiOptimizationProperties properties) { return Executors.newFixedThreadPool(properties.getOptimization().getMaxConcurrentRuns(), new CustomizableThreadFactory("tianshu-ai-opt-")); }
```

- Verification contribution: Step 11 装配测试；本步编译。
- After this file: 本步完成。

- Validation working directory: 仓库根
- Verification command: `./mvnw -pl egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin -Dtest=YuhengLlmModelAdapterTest,DdcAiOptimizationPropertiesTest -Dsurefire.failIfNoSpecifiedTests=false test`
- Expected result: 两测试类通过；日志断言无 token/正文。
- Failure returns to: File 5（分类）、File 6（条件）。
- Completion criteria: 五类错误分类、token 参数、无敏感日志均有断言。
- Rollback: 回退本步 Commit paths。
- Commit paths: egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/test/java/top/egon/cola/component/tianshu/admin/service/ai/model/YuhengLlmModelAdapterTest.java, egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/java/top/egon/cola/component/tianshu/admin/model/dto/DdcAiModelRequestDTO.java, egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/java/top/egon/cola/component/tianshu/admin/model/dto/DdcAiModelResultDTO.java, egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/java/top/egon/cola/component/tianshu/admin/service/ai/DdcAiModelPort.java, egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/java/top/egon/cola/component/tianshu/admin/service/ai/model/DdcAiModelException.java, egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/java/top/egon/cola/component/tianshu/admin/service/ai/model/YuhengLlmModelAdapter.java, egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/java/top/egon/cola/component/tianshu/admin/config/DdcAiOptimizationConfiguration.java
- Commit: `feat(tianshu): yuheng llm model port for ai judge and optimizer`

### Step 8 — LLM-as-judge 策略与评分作业（JOB-001）

- Requirements: `REQ-003`, `REQ-013`
- Dependencies: Step 5、Step 7
- Baseline state: `DdcLeaseExpiryScanner`（普通类 + `@Scheduled(fixedDelayString=...)`，逐租户 `tenantRegistry.requireEnabledTenant` + `DdcTenantContextExecutor.call(new DdcTenantContextDTO(tenantId, identityTenantId, "system:..."), ...)`）在 `DdcAdminRedisConfig` 以 `@Bean` 构造；无 `AR/tianshu/ai/prompts/`。
- Observable outcome: `DdcAiJudgeStrategyTest` 证明 compose 含版本摘录/样本摘录且截断、parse 严格 JSON `{score, rationale}`；`DdcAiJudgeScannerTest` 证明认领→模型→写回、模型错误→`failJudge(code)`、attempts 达上限→FAILED、`judgeEnabled=false` 跳过。
- End state: 评分作业在通道启用时装配。
- Test-first gate: Required — 两测试类 RED。
- Manual Checks: MC-ARCH-001, MC-REUSE-001, MC-NAME-001, MC-VALID-001, MC-MODEL-001, MC-LOG-001, MC-BEAN-001, MC-UTIL-001, MC-JSON-001, MC-TIME-001, MC-CONFIG-001, MC-PATTERN-001, MC-SCOPE-001, MC-TEST-001
- Literal Rules: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 9, Rule 10, Rule 11
- Ordered files:

#### File 1 — `CREATE egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/test/java/top/egon/cola/component/tianshu/admin/service/ai/DdcAiJudgeStrategyTest.java`

- Purpose: compose/parse 契约。
- Symbols: `composeIncludesTruncatedVersionAndSampleExcerpts`、`parseAcceptsStrictJsonVerdict`、`parseRejectsNonJsonOrOutOfRangeScore`、`composeUsesSkillRootSkillMdWhenPresent`
- Repository evidence: 纯函数测试风格同 `DdcAiDraftServiceTest`。
- Dependencies and consumers: File 3–5。
- Why now: RED。
- Contract/signature changes: 无。
- Input/output and state mapping: `DdcAiJudgeContextBO`（本步新增内部载体，见 File 5）→`DdcAiModelRequestDTO`；模型文本→`DdcAiJudgeVerdictDTO`。
- Error and edge behavior: `{"score":101}`→`IllegalArgumentException("INVALID_OUTPUT")`。
- Standards impact: MC-TEST-001。
- Literal rule enforcement: Rule 11。
- Implementation pseudocode:

```java
@Test void composeIncludesTruncatedVersionAndSampleExcerpts() {
    var request = strategy.compose(context(promptText("x".repeat(40_000)), sample("in", "out")), 16_384);
    assertThat(request.userPrompt()).contains("## Prompt").contains("## Input").contains("in").contains("## Output").contains("out");
    assertThat(request.userPrompt().getBytes(UTF_8).length).isLessThanOrEqualTo(16_384 + 2_048); assertThat(request.purpose()).isEqualTo("JUDGE");
}
@Test void parseRejectsNonJsonOrOutOfRangeScore() { assertThatThrownBy(() -> strategy.parse("not json")).hasMessage("INVALID_OUTPUT"); assertThatThrownBy(() -> strategy.parse("{\"score\":101,\"rationale\":\"\"}")).hasMessage("INVALID_OUTPUT"); }
```

- Verification contribution: RED→GREEN。
- After this file: RED。

#### File 2 — `CREATE egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/test/java/top/egon/cola/component/tianshu/admin/service/ai/DdcAiJudgeScannerTest.java`

- Purpose: TEST-005（单测部分）、TEST-007 作业侧。
- Symbols: `claimsJudgesAndWritesBackScore`、`modelFailureRecordsErrorAndKeepsPendingUntilMaxAttempts`、`skipsResourcesWithJudgeDisabled`、`tenantBindingChangeSkipsTenant`
- Repository evidence: 作业测试风格可参照 `DdcLeaseExpiryScanner` 既有测试（`AT/service/lease/`，若存在）与固定 `Clock`。
- Dependencies and consumers: File 6/7。
- Why now: RED。
- Contract/signature changes: 无。
- Input/output and state mapping: 假 `DdcAiFeedbackService`（Mockito）返回 2 条认领；假 `DdcAiModelPort` 返回 `{"score":66,"rationale":"ok"}`；断言 `completeJudge(id, version, verdict(66))`。
- Error and edge behavior: `DdcAiModelException(MODEL_TIMEOUT)`→`failJudge(id, version, "MODEL_TIMEOUT")`。
- Standards impact: MC-TEST-001。
- Literal rule enforcement: Rule 11。
- Implementation pseudocode:

```java
@Test void claimsJudgesAndWritesBackScore() {
    when(policyRepository.findJudgeEnabledResourceIds()).thenReturn(Set.of(7L));
    when(feedbackService.claimForJudge(Set.of(7L), 50)).thenReturn(List.of(sample(1L, 3L), sample(2L, 1L)));
    when(modelPort.complete(any())).thenReturn(new DdcAiModelResultDTO("{\"score\":66,\"rationale\":\"ok\"}", 1, 1, "m", "stop"));
    scanner.scanJudge();
    verify(feedbackService).completeJudge(eq(1L), eq(3L), eq(new DdcAiJudgeVerdictDTO(66, "ok"))); verify(feedbackService).completeJudge(eq(2L), eq(1L), any());
}
@Test void modelFailureRecordsErrorAndKeepsPendingUntilMaxAttempts() { when(modelPort.complete(any())).thenThrow(new DdcAiModelException(MODEL_TIMEOUT, 0, "t", null)); scanner.scanJudge(); verify(feedbackService).failJudge(1L, 3L, "MODEL_TIMEOUT"); }
```

- Verification contribution: RED→GREEN。
- After this file: RED。

#### File 3 — `CREATE egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/resources/tianshu/ai/prompts/judge-system.txt`

- Purpose: 评分系统提示模板（UTF-8 纯文本，占位 `{{threshold}}` 无；固定内容）。
- Symbols: 文本：角色说明、评分 0..100 标准、严格输出 `{"score": <0-100 整数>, "rationale": "<=512 字符"}` 且不得输出其他内容
- Repository evidence: `AR/` 下既有 `mybatis/`、`db/` 资源目录；新目录 `tianshu/ai/prompts/`。
- Dependencies and consumers: `DdcAiPromptTemplateLoader`。
- Why now: Strategy 输入。
- Contract/signature changes: 新资源。
- Input/output and state mapping: classpath 读取一次并缓存。
- Error and edge behavior: 缺失→启动失败。
- Standards impact: MC-SCOPE-001。
- Literal rule enforcement: Rule 11。
- Implementation pseudocode:

```text
你是提示词质量评审员。给定一个 Prompt/Skill 的当前内容、一次实际调用的输入摘录与输出摘录，评价本次输出相对于该 Prompt/Skill 目标的质量。
评分标准：0=完全失败或有害；50=部分满足；80=满足且无明显缺陷；100=完美。只依据给定材料，不要臆测缺失信息。
只输出一个 JSON 对象，不要输出任何其他文字：{"score": 0到100的整数, "rationale": "不超过512字符的中文说明"}
```

- Verification contribution: File 1 加载断言。
- After this file: 资源就绪。

#### File 4 — `CREATE egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/java/top/egon/cola/component/tianshu/admin/service/ai/impl/DdcAiPromptTemplateLoader.java`

- Purpose: 加载三份系统提示模板并缓存。
- Symbols: `@Component("ddcAiPromptTemplateLoader")`；`String judgeSystem()`、`String optimizePromptSystem()`、`String optimizeSkillSystem()`；`@PostConstruct load()`
- Repository evidence: Spring `ClassPathResource` + `StreamUtils.copyToString`（JDK/Spring，无新依赖）。
- Dependencies and consumers: Judge/Optimizer Strategy。
- Why now: Strategy 前置。
- Contract/signature changes: 新类。
- Input/output and state mapping: classpath `tianshu/ai/prompts/*.txt`→不可变字符串。
- Error and edge behavior: 空文件→`IllegalStateException("AI_PROMPT_TEMPLATE_EMPTY:" + name)`。
- Standards impact: MC-LOG-001, MC-BEAN-001, MC-UTIL-001。
- Literal rule enforcement: Rule 4、Rule 5、Rule 11。
- Implementation pseudocode:

```java
@Slf4j @Component("ddcAiPromptTemplateLoader")
public class DdcAiPromptTemplateLoader {
    private static final String BASE = "tianshu/ai/prompts/";
    private volatile Map<String, String> templates = Map.of();
    @PostConstruct void load() { templates = Map.of("judge", read("judge-system.txt"), "optimize-prompt", read("optimize-prompt-system.txt"), "optimize-skill", read("optimize-skill-system.txt")); }
    private static String read(String name) { try (var in = new ClassPathResource(BASE + name).getInputStream()) { String text = StreamUtils.copyToString(in, UTF_8).strip(); if (text.isEmpty()) throw new IllegalStateException("AI_PROMPT_TEMPLATE_EMPTY:" + name); return text; } catch (IOException e) { throw new IllegalStateException("AI_PROMPT_TEMPLATE_MISSING:" + name, e); } }
    public String judgeSystem() { return templates.get("judge"); }
}
```

- Verification contribution: File 1。
- After this file: 编译通过；本步 `load()` 只登记 `judge` 模板，Step 9 扩展为三份。

#### File 5 — `CREATE egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/java/top/egon/cola/component/tianshu/admin/service/ai/impl/DdcAiJudgeStrategy.java`

- Purpose: 评分策略接口与默认实现（合并块：`DdcAiJudgeStrategy`、`DdcDefaultAiJudgeStrategy`；内部载体 `DdcAiJudgeContextBO{kind, versionText, inputExcerpt, outputExcerpt, outcome, latencyMs}` 放 `model/bo`）。
- Symbols: `interface DdcAiJudgeStrategy { DdcAiModelRequestDTO compose(DdcAiJudgeContextBO context, int maxContextBytes, String modelAlias, int maxOutputTokens); DdcAiJudgeVerdictDTO parse(String modelText); }`；`@Component("ddcDefaultAiJudgeStrategy") class DdcDefaultAiJudgeStrategy implements DdcAiJudgeStrategy`
- Repository evidence: `DdcAiContentStrategy` + `ddcAiContentStrategyRegistry` 的策略模式先例。
- Dependencies and consumers: Scanner。
- Why now: Rule 9 设计模式。
- Contract/signature changes: 新接口/类。
- Input/output and state mapping: compose 用 `judgeSystem()` + 用户段落（`## Prompt`/`## Skill`、`## Input`、`## Output`、`## Outcome`）；UTF-8 安全截断到 `maxContextBytes`；`temperature=0`。
- Error and edge behavior: parse：去除 Markdown 代码围栏→`objectMapper.readTree`→`score` 整数 0..100、`rationale` 字符串；否则 `IllegalArgumentException("INVALID_OUTPUT")`。
- Standards impact: MC-PATTERN-001（Strategy）、MC-JSON-001、MC-UTIL-001、MC-LOG-001、MC-BEAN-001。
- Literal rule enforcement: Rule 9、Rule 6、Rule 4、Rule 11。
- Implementation pseudocode:

```java
@Override public DdcAiModelRequestDTO compose(DdcAiJudgeContextBO c, int maxContextBytes, String modelAlias, int maxOutputTokens) {
    String body = "## " + (c.getKind() == DdcAiKindEnum.PROMPT ? "Prompt" : "Skill") + "\n" + truncateUtf8(c.getVersionText(), maxContextBytes / 2) + "\n\n## Input\n" + truncateUtf8(nullToEmpty(c.getInputExcerpt()), maxContextBytes / 4) + "\n\n## Output\n" + truncateUtf8(nullToEmpty(c.getOutputExcerpt()), maxContextBytes / 4) + "\n\n## Outcome\n" + c.getOutcome().getCode();
    return new DdcAiModelRequestDTO(modelAlias, templates.judgeSystem(), body, Math.min(maxOutputTokens, 512), 0.0d, "JUDGE");
}
@Override public DdcAiJudgeVerdictDTO parse(String text) {
    try { JsonNode node = objectMapper.readTree(stripFences(text)); if (!node.path("score").canConvertToInt()) throw invalid(); int score = node.get("score").intValue(); if (score < 0 || score > 100) throw invalid(); return new DdcAiJudgeVerdictDTO(score, node.path("rationale").asText("")); }
    catch (JsonProcessingException e) { throw new IllegalArgumentException("INVALID_OUTPUT", e); }
}
```

- Verification contribution: File 1 全部用例。
- After this file: File 1 GREEN。

#### File 6 — `CREATE egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/java/top/egon/cola/component/tianshu/admin/service/ai/job/DdcAiJudgeScanner.java`

- Purpose: JOB-001。
- Symbols: `public class DdcAiJudgeScanner`（非注解，File 7 构造）；伴随 `@Component("ddcAiJudgeContextLoader") class DdcAiJudgeContextLoader`（`DdcAiJudgeContextBO load(DdcAiUsageFeedbackBO)`：读 `ddcAiReadTransactionTemplate` 内的版本正文/Skill 根文件与策略 `judgeModelAlias`）；`@Scheduled(fixedDelayString="${egon.cola.component.tianshu.admin.ai.judge.scan-interval:30s}", initialDelayString=同)`；`int scanJudge()`；`judgeTenant()`；`judgeOne(DdcAiUsageFeedbackBO)`；`DdcAiOptimizationPolicyRepository#findJudgeEnabledResourceIds()`（本步在 Step 4 接口追加该查询：`SELECT resource_id ... WHERE enabled AND judge_enabled AND deleted_at IS NULL`，XML `selectJudgeEnabledResourceIds`）
- Repository evidence: `DdcLeaseExpiryScanner#scanExpired`（逐租户、`TENANT_BINDING_CHANGED_DURING_SCAN`、`log.error` 后继续）。
- Dependencies and consumers: File 7 装配。
- Why now: 评分闭环。
- Contract/signature changes: 新类；Repository 追加一个查询方法（Step 4 文件小改，计入本步 Commit paths）。
- Input/output and state mapping: 每租户：`DdcAiSystemActor.bind(JUDGE, ...)` 内 `claimForJudge(resourceIds, batchSize)`（事务由 Service 的 command 模板提交）→事务外逐条：加载版本文本（`versionRepository.findActiveById` + Prompt 正文或 Skill 根 `SKILL.md`/首个 FILE 节点，由 `DdcAiResourceService` 既有只读查询 `getVersionContent` 或节点仓库直接读取）→`strategy.compose`→`modelPort.complete`→`strategy.parse`→`feedbackService.completeJudge`；指标 `tianshu.ai.judge.runs{result}`。
- Error and edge behavior: `DdcAiModelException`→`failJudge(id, version, code.getCode())`；`IllegalArgumentException("INVALID_OUTPUT")`→`failJudge(..., "INVALID_OUTPUT")`；`failJudge` 内部由 Service 按 `judgeAttempts >= maxAttempts` 置 FAILED（Repository `failJudge(finalFailure)`）；`result.truncated()`→INVALID_OUTPUT。
- Standards impact: MC-LOG-001, MC-TIME-001, MC-PATTERN-001。
- Literal rule enforcement: Rule 4（`@Slf4j`）、Rule 10、Rule 11。
- Implementation pseudocode:

```java
@Scheduled(fixedDelayString = "${egon.cola.component.tianshu.admin.ai.judge.scan-interval:30s}", initialDelayString = "${egon.cola.component.tianshu.admin.ai.judge.scan-interval:30s}")
public int scanJudge() {
    int total = 0;
    for (DdcTenantBindingDTO configured : tenantProperties.getBindings()) {
        if (configured == null || !configured.isEnabled()) continue;
        try { DdcTenantBindingDTO registered = tenantRegistry.requireEnabledTenant(configured.getTenantId()); requireUnchanged(configured, registered);
              DdcTenantContextDTO ctx = new DdcTenantContextDTO(registered.getTenantId(), registered.getIdentityTenantId(), DdcAiSystemActor.JUDGE);
              total += tenantContextExecutor.call(ctx, () -> DdcAiSystemActor.bind(DdcAiSystemActor.JUDGE, this::judgeTenant)); }
        catch (RuntimeException failure) { log.error("AI judge scan failed for tenantId={}", configured.getTenantId(), failure); }
    }
    return total;
}
private int judgeTenant() {
    Set<Long> resourceIds = policyRepository.findJudgeEnabledResourceIds(); if (resourceIds.isEmpty()) return 0;
    List<DdcAiUsageFeedbackBO> claimed = feedbackService.claimForJudge(resourceIds, properties.getJudge().getBatchSize());
    for (DdcAiUsageFeedbackBO sample : claimed) {
        try { DdcAiJudgeContextBO context = contextLoader.load(sample); DdcAiModelResultDTO result = modelPort.complete(strategy.compose(context, properties.getJudge().getMaxContextBytes(), context.getJudgeModelAlias(), properties.getModel().getMaxOutputTokens()));
              if (result.truncated()) throw new IllegalArgumentException("INVALID_OUTPUT"); feedbackService.completeJudge(sample.getId(), sample.getVersion(), strategy.parse(result.text())); meters.counter("tianshu.ai.judge.runs", "result", "DONE").increment(); }
        catch (DdcAiModelException model) { feedbackService.failJudge(sample.getId(), sample.getVersion(), model.getCode().getCode()); meters.counter("tianshu.ai.judge.runs", "result", model.getCode().getCode()).increment(); }
        catch (IllegalArgumentException invalid) { feedbackService.failJudge(sample.getId(), sample.getVersion(), "INVALID_OUTPUT"); }
    }
    return claimed.size();
}
```

- Verification contribution: File 2 全部用例。
- After this file: File 2 GREEN（直接构造）。

#### File 7 — `MODIFY egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/java/top/egon/cola/component/tianshu/admin/config/DdcAiOptimizationConfiguration.java`

- Purpose: 装配 `ddcAiJudgeScanner`。
- Symbols: `@Bean("ddcAiJudgeScanner") @ConditionalOnProperty(prefix=OPT, name="enabled", havingValue="true") DdcAiJudgeScanner ddcAiJudgeScanner(...)`
- Repository evidence: `DdcAdminRedisConfig#ddcLeaseExpiryScanner` 的 `@Qualifier` 列表（`DdcTenantAdminProperties` 限定名 `egon.cola.component.tianshu.admin.tenancy-top.egon.cola.component.tianshu.admin.security.tenancy.DdcTenantAdminProperties`、`ddcTenantRegistry`、`ddcTenantContextExecutor`）。
- Dependencies and consumers: Spring 调度（既有 `@EnableScheduling`）。
- Why now: 作业生效。
- Contract/signature changes: 新 Bean。
- Input/output and state mapping: 属性→构造参数。
- Error and edge behavior: 通道关闭不装配。
- Standards impact: MC-BEAN-001, MC-CONFIG-001。
- Literal rule enforcement: Rule 4、Rule 11。
- Implementation pseudocode:

```java
@Bean("ddcAiJudgeScanner") @ConditionalOnProperty(prefix = OPT, name = "enabled", havingValue = "true")
public DdcAiJudgeScanner ddcAiJudgeScanner(@Qualifier("ddcAiFeedbackService") DdcAiFeedbackService feedbackService, @Qualifier("ddcAiOptimizationPolicyRepository") DdcAiOptimizationPolicyRepository policyRepository,
        @Qualifier("ddcAiJudgeContextLoader") DdcAiJudgeContextLoader contextLoader, @Qualifier("ddcDefaultAiJudgeStrategy") DdcAiJudgeStrategy strategy, @Qualifier("ddcAiModelPort") DdcAiModelPort modelPort,
        @Qualifier("egon.cola.component.tianshu.admin.tenancy-top.egon.cola.component.tianshu.admin.security.tenancy.DdcTenantAdminProperties") DdcTenantAdminProperties tenantProperties, @Qualifier("ddcTenantRegistry") DdcTenantRegistry tenantRegistry,
        @Qualifier("ddcTenantContextExecutor") DdcTenantContextExecutor tenantContextExecutor, DdcAiOptimizationProperties properties, MeterRegistry meters) {
    return new DdcAiJudgeScanner(feedbackService, policyRepository, contextLoader, strategy, modelPort, tenantProperties, tenantRegistry, tenantContextExecutor, properties, meters);
}
```

- Verification contribution: Step 11 装配测试。
- After this file: 本步完成。

- Validation working directory: 仓库根
- Verification command: `./mvnw -pl egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin -Dtest=DdcAiJudgeStrategyTest,DdcAiJudgeScannerTest,DdcAiFeedbackOptimizationRepositoryContractTest -Dsurefire.failIfNoSpecifiedTests=false test`
- Expected result: 三测试类通过（Repository 契约测试覆盖新增 `selectJudgeEnabledResourceIds`）。
- Failure returns to: File 5（parse）、File 6（作业分支）。
- Completion criteria: 认领→评分→写回、失败分类、上限 FAILED 均有断言。
- Rollback: 回退本步 Commit paths。
- Commit paths: egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/test/java/top/egon/cola/component/tianshu/admin/service/ai/DdcAiJudgeStrategyTest.java, egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/test/java/top/egon/cola/component/tianshu/admin/service/ai/DdcAiJudgeScannerTest.java, egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/resources/tianshu/ai/prompts/judge-system.txt, egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/java/top/egon/cola/component/tianshu/admin/service/ai/impl/DdcAiPromptTemplateLoader.java, egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/java/top/egon/cola/component/tianshu/admin/service/ai/impl/DdcAiJudgeStrategy.java, egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/java/top/egon/cola/component/tianshu/admin/service/ai/impl/DdcDefaultAiJudgeStrategy.java, egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/java/top/egon/cola/component/tianshu/admin/service/ai/impl/DdcAiJudgeContextLoader.java, egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/java/top/egon/cola/component/tianshu/admin/model/bo/DdcAiJudgeContextBO.java, egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/java/top/egon/cola/component/tianshu/admin/service/ai/job/DdcAiJudgeScanner.java, egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/java/top/egon/cola/component/tianshu/admin/config/DdcAiOptimizationConfiguration.java, egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/java/top/egon/cola/component/tianshu/admin/repository/DdcAiOptimizationPolicyRepository.java, egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/java/top/egon/cola/component/tianshu/admin/repository/impl/DdcAiOptimizationPolicyRepositoryImpl.java, egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/java/top/egon/cola/component/tianshu/admin/dao/DdcAiOptimizationPolicyMapper.java, egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/resources/mybatis/mapper/tianshu/DdcAiOptimizationPolicyMapper.xml, egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/test/java/top/egon/cola/component/tianshu/admin/repository/DdcAiFeedbackOptimizationRepositoryContractTest.java
- Commit: `feat(tianshu): llm-as-judge scoring job for ai usage feedback`

### Step 9 — 优化策略、状态机、Optimizer Strategy 与优化服务

- Requirements: `REQ-004`, `REQ-005`, `REQ-013`, `REQ-014`
- Dependencies: Step 5、Step 7、Step 8（模板加载器）
- Baseline state: `DdcAiResourceService#replaceDraft(DdcAiIdQuery, DdcAiDraftCommand)`/`publish(DdcAiIdQuery, requestId, DdcAiPublishCommand)` 既有；`DdcAiResourceServiceImpl#authenticatedActor()` 仅识别 `Rbac3UserDetails`（本步不改，Step 10 追加系统主体识别；本步 `executeRun` 中的系统主体调用在 Step 10 前以 Mockito 替身验证）；`ddcAiContentStrategyRegistry` 为 `EnumMap` 注册表先例；`DdcAiContentDTO` 带 `@ValidAiContent(Save)` / `@PublishableAiContent(Publish)`。
- Observable outcome: 状态机矩阵（TEST-008）、两 Optimizer Strategy compose/parse（TEST-009/010）、评估/认领/执行/回收（TEST-008/012/013/014）、REST 服务方法（策略读写、触发、分页、单读）全部在 Mockito 层通过。
- End state: `DdcAiOptimizationService` 可被作业（Step 11）与控制器（Step 12）调用。
- Test-first gate: Required — 三个测试类 RED。
- Manual Checks: MC-ARCH-001, MC-REUSE-001, MC-NAME-001, MC-VALID-001, MC-MODEL-001, MC-CONVERT-001, MC-LOG-001, MC-BEAN-001, MC-UTIL-001, MC-JSON-001, MC-TIME-001, MC-CONFIG-001, MC-PATTERN-001, MC-SCOPE-001, MC-TEST-001
- Literal Rules: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 9, Rule 10, Rule 11
- Ordered files:

#### File 1 — `CREATE egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/test/java/top/egon/cola/component/tianshu/admin/service/ai/DdcAiOptimizationStateTransitionTest.java`

- Purpose: TEST-008 状态机表。
- Symbols: `transitionTable`（参数化：`(ARMED, INSUFFICIENT_SAMPLES)→ARMED`、`(ARMED, THRESHOLD_MET)→CONVERGED`、`(ARMED, ITERATIONS_EXHAUSTED)→EXHAUSTED`、`(ARMED, RUN_SUCCEEDED, autoPublish=false)→AWAITING_PUBLISH`、`(ARMED, RUN_SUCCEEDED, autoPublish=true)→ARMED`、`(ARMED, RUN_FAILED, failures=3)→PAUSED`、`(AWAITING_PUBLISH, BASE_PUBLISHED)→ARMED`、`(PAUSED, REARM)→ARMED`、`(CONVERGED, SCHEDULED_EVALUATE)→CONVERGED`（不可评估））、`rejectsUndefinedSignal`
- Repository evidence: 主 Spec §10.6 状态转移表与 §7.2.1 流程。
- Dependencies and consumers: File 11。
- Why now: RED。
- Contract/signature changes: 无。
- Input/output and state mapping: `next(DdcAiPolicyStatePersistenceEnum, DdcAiOptimizationStateTransition.Signal, Context)`。
- Error and edge behavior: 未定义组合→`IllegalStateException("AI_POLICY_TRANSITION_UNDEFINED")`。
- Standards impact: MC-TEST-001。
- Literal rule enforcement: Rule 9、Rule 11。
- Implementation pseudocode:

```java
@ParameterizedTest @CsvSource({"ARMED,THRESHOLD_MET,false,0,CONVERGED", "ARMED,ITERATIONS_EXHAUSTED,false,0,EXHAUSTED", "ARMED,RUN_SUCCEEDED,false,0,AWAITING_PUBLISH", "ARMED,RUN_SUCCEEDED,true,0,ARMED", "ARMED,RUN_FAILED,false,3,PAUSED", "ARMED,RUN_FAILED,false,1,ARMED", "AWAITING_PUBLISH,BASE_PUBLISHED,false,0,ARMED", "PAUSED,REARM,false,0,ARMED", "EXHAUSTED,REARM,false,0,ARMED"})
void transitionTable(String from, String signal, boolean autoPublish, int failures, String expected) {
    assertThat(transition.next(DdcAiPolicyStatePersistenceEnum.valueOf(from), Signal.valueOf(signal), new Context(autoPublish, failures, 3))).isEqualTo(DdcAiPolicyStatePersistenceEnum.valueOf(expected));
}
@Test void rejectsUndefinedSignal() { assertThatThrownBy(() -> transition.next(CONVERGED, Signal.RUN_SUCCEEDED, ctx())).isInstanceOf(IllegalStateException.class).hasMessage("AI_POLICY_TRANSITION_UNDEFINED"); }
```

- Verification contribution: RED→GREEN。
- After this file: RED。

#### File 2 — `CREATE egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/test/java/top/egon/cola/component/tianshu/admin/service/ai/DdcAiOptimizerStrategyTest.java`

- Purpose: TEST-009/010。
- Symbols: `promptComposeIncludesBaselineInstructionAndWorstSamples`、`promptParseStrictJsonYieldsContent`、`skillParseTreeJsonYieldsNodes`、`skillParseRejectsPathTraversalOversizeAndMissingRoot`、`parseRejectsTruncatedFinishReason`
- Repository evidence: `DdcSkillContentStrategy#prepare` 的限额（256 节点、256KiB 文件、1MiB 整包）由既有 `@ValidAiContent` 校验执行，本策略只做结构解析。
- Dependencies and consumers: File 12。
- Why now: RED。
- Contract/signature changes: 无。
- Input/output and state mapping: `compose(DdcAiOptimizationContextBO)`→`DdcAiModelRequestDTO(purpose=OPTIMIZE)`；`parse(text)`→`DdcAiContentDTO`。
- Error and edge behavior: Skill JSON `{"files":[{"path":"../x","content":""}]}`→`IllegalArgumentException("INVALID_OUTPUT")`。
- Standards impact: MC-TEST-001。
- Literal rule enforcement: Rule 11。
- Implementation pseudocode:

```java
@Test void promptComposeIncludesBaselineInstructionAndWorstSamples() {
    var req = promptStrategy.compose(context(promptText("base"), instruction("更简洁"), samples(sample(12, "in1", "out1"), sample(30, "in2", "out2"))));
    assertThat(req.systemPrompt()).isEqualTo(templates.optimizePromptSystem()); assertThat(req.userPrompt()).contains("## Current Prompt", "base", "## Operator Instruction", "更简洁", "## Low-score Samples", "score: 12", "in1"); assertThat(req.purpose()).isEqualTo("OPTIMIZE");
}
@Test void skillParseTreeJsonYieldsNodes() { var content = skillStrategy.parse("{\"files\":[{\"path\":\"SKILL.md\",\"content\":\"# s\"},{\"path\":\"ref/a.md\",\"content\":\"a\"}]}"); assertThat(content.getNodes()).extracting(DdcAiNodeDTO::getPath).containsExactly("SKILL.md", "ref", "ref/a.md"); }
@Test void skillParseRejectsPathTraversalOversizeAndMissingRoot() { assertThatThrownBy(() -> skillStrategy.parse("{\"files\":[{\"path\":\"../x\",\"content\":\"\"}]}")).hasMessage("INVALID_OUTPUT"); assertThatThrownBy(() -> skillStrategy.parse("{\"files\":[{\"path\":\"a.md\",\"content\":\"\"}]}")).hasMessage("INVALID_OUTPUT"); }
```

- Verification contribution: RED→GREEN。
- After this file: RED。

#### File 3 — `CREATE egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/test/java/top/egon/cola/component/tianshu/admin/service/ai/DdcAiOptimizationServiceTest.java`

- Purpose: TEST-008（evaluateTenant）、TEST-012、TEST-013、TEST-014，以及 REST 服务方法规则（TEST-017/020 的服务层部分）。
- Symbols: `evaluateCreatesPendingRunWhenBelowThresholdWithEnoughSamples`、`evaluateMarksConvergedWhenAverageMeetsThreshold`、`evaluateSkipsNotPublishedResource`、`awaitingPublishReturnsArmedWhenBaseChanged`、`claimRunReturnsFalseOnCasMiss`、`executeRunSkipsWhenBaseChangedOrDraftDirty`、`executeRunBindsSystemActorAndRestoresMdc`、`reclaimExpiredRunsFailsWithLeaseExpiredAndCountsFailure`、`replacePolicyRejectsRearmWithActiveRun`、`triggerReplaysSameRequestAndConflictsOnDifferentDigest`、`triggerRejectsWhenChannelDisabled`
- Repository evidence: `DdcAiVersionServiceTest` 的 Mockito + `TransactionTemplate` 替身写法。
- Dependencies and consumers: File 13/14。
- Why now: RED。
- Contract/signature changes: 无。
- Input/output and state mapping: 固定 `Clock`；`policyRepository.lockEvaluable` 返回 1 条 ARMED；`feedbackService.aggregate` 返回 `(count=25, avg=71.50)`→断言 `runRepository.insertRun(PENDING, SCHEDULED, baseVersionId=current)`。
- Error and edge behavior: `executeRun` 中 `resource.currentVersionId != run.baseVersionId`→`updateRun(SKIPPED, BASE_CHANGED)`；草稿 hash ≠ 版本 hash→`DRAFT_DIRTY`；`trigger` 通道关闭→`CommonException(56322)`。
- Standards impact: MC-TEST-001, MC-TIME-001。
- Literal rule enforcement: Rule 10、Rule 11。
- Implementation pseudocode:

```java
@Test void evaluateCreatesPendingRunWhenBelowThresholdWithEnoughSamples() {
    when(policyRepository.lockEvaluable(now, 50)).thenReturn(List.of(policy(ARMED, threshold 80, minSamples 20, iterationsDone 0)));
    when(resourceRepository.findActiveById(7L)).thenReturn(Optional.of(resource(currentVersionId 99L)));
    when(feedbackService.aggregate(7L, 99L, now.minus(Duration.ofDays(1)), now)).thenReturn(aggregate(25, "71.50"));
    when(runRepository.nextRunNo(7L)).thenReturn(4L);
    service.evaluateTenant(now);
    verify(runRepository).insertRun(argThat(r -> r.getState() == PENDING && r.getTrigger() == SCHEDULED && r.getRunNo() == 4L && r.getBaseVersionId() == 99L && r.getThresholdSnapshot() == 80));
    verify(policyRepository).updatePolicy(argThat(p -> p.getActiveRunId() != null && p.getLastAverageScore().compareTo(new BigDecimal("71.50")) == 0));
}
@Test void executeRunSkipsWhenBaseChangedOrDraftDirty() { run(baseVersionId 99L); resource(currentVersionId 100L); service.executeRun(runId); verify(runRepository).updateRun(argThat(r -> r.getState() == SKIPPED && r.getErrorCode() == BASE_CHANGED)); verifyNoInteractions(modelPort); }
@Test void triggerReplaysSameRequestAndConflictsOnDifferentDigest() { when(runRepository.findByResourceAndRequestId(7L, KEY)).thenReturn(Optional.of(run(requestSha256 = sha(KEY, actor, 7L, "n")))); assertThat(service.trigger(idQuery(7L), KEY, trigger("n")).getRunNo()).isEqualTo("4"); assertThatThrownBy(() -> service.trigger(idQuery(7L), KEY, trigger("other"))).hasFieldOrPropertyWithValue("code", 56305); }
```

- Verification contribution: RED→GREEN。
- After this file: RED。

#### File 4 — `CREATE egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/resources/tianshu/ai/prompts/optimize-prompt-system.txt`

- Purpose: 两份改写系统提示（合并块：`optimize-prompt-system.txt`、`optimize-skill-system.txt`）；`DdcAiPromptTemplateLoader#load` 扩展登记二者（MODIFY，计入 Commit paths）。
- Symbols: Prompt 模板要求只输出 `{"promptText": "..."}`；Skill 模板要求只输出 `{"files":[{"path":"SKILL.md","content":"..."}, ...]}`，路径相对、无 `..`、必须含根 `SKILL.md`、文件总数 ≤128、单文件 ≤128KiB
- Repository evidence: Step 8 File 3/4。
- Dependencies and consumers: File 12。
- Why now: Strategy 输入。
- Contract/signature changes: 新资源 + Loader 小改。
- Input/output and state mapping: classpath 读取。
- Error and edge behavior: 缺失→启动失败。
- Standards impact: MC-SCOPE-001。
- Literal rule enforcement: Rule 11。
- Implementation pseudocode:

```text
optimize-prompt-system.txt：你是提示词优化专家。给定当前 Prompt、运营者的附加指令和若干低分样本（输入/输出/分数），改写 Prompt 以提升这些样本上的表现，保持原始意图、语言与约束；不得加入与任务无关的内容。
只输出一个 JSON 对象，不得输出任何其他文字：{"promptText": "改写后的完整 Prompt"}
optimize-skill-system.txt：你是 Skill 维护专家。给定当前 Skill 文件树（路径与内容）、附加指令和低分样本，输出改进后的完整文件树。路径必须为相对路径、不含 ".."、必须包含根目录下的 SKILL.md；只能使用 UTF-8 文本。
只输出一个 JSON 对象：{"files":[{"path":"SKILL.md","content":"..."}]}
```

- Verification contribution: File 2。
- After this file: 资源就绪。

#### File 5 — `CREATE egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/java/top/egon/cola/component/tianshu/admin/model/command/DdcAiOptimizationPolicyCommand.java`

- Purpose: API-013/014 命令（合并块：`DdcAiOptimizationPolicyCommand`、`DdcAiOptimizationTriggerCommand`）。
- Symbols: policy 命令字段：`expectedRevision @Pattern("^(0|[1-9][0-9]{0,18})$")`、`enabled`、`scoreThreshold 1..100`、`minSamples 1..100000`、`evaluationWindow Duration @ValidDurationRange(minSeconds=300, maxSeconds=2592000)`、`maxIterations 1..50`、`cooldown @ValidDurationRange(60, 604800)`、`autoPublish`、`judgeEnabled`、`optimizerModelAlias/judgeModelAlias @Pattern alias`、`optimizerInstruction @Size(max=4096)`、`rearm`；trigger 命令：`note @NotNull @Size(max=512)`
- Repository evidence: `DdcAiPublishCommand`、`DdcAiDraftCommand`（`@ConvertGroup`）。
- Dependencies and consumers: Service、Controller。
- Why now: Service 签名。
- Contract/signature changes: 新类。
- Input/output and state mapping: Jackson 解析 ISO-8601 Duration（`"PT24H"`）。
- Error and edge behavior: 任何约束失败→56301。
- Standards impact: MC-NAME-001, MC-VALID-001, MC-MODEL-001, MC-JSON-001, MC-TIME-001。
- Literal rule enforcement: Rule 1、Rule 2、Rule 3、Rule 6、Rule 10、Rule 11。
- Implementation pseudocode:

```java
@Data @NoArgsConstructor @AllArgsConstructor @Accessors(chain = true) @Builder
public class DdcAiOptimizationPolicyCommand {
    @NotBlank @Pattern(regexp = "^(0|[1-9][0-9]{0,18})$") private String expectedRevision; @NotNull private Boolean enabled;
    @NotNull @Min(1) @Max(100) private Integer scoreThreshold; @NotNull @Min(1) @Max(100000) private Integer minSamples; @NotNull @ValidDurationRange(minSeconds = 300, maxSeconds = 2592000) private Duration evaluationWindow;
    @NotNull @Min(1) @Max(50) private Integer maxIterations; @NotNull @ValidDurationRange(minSeconds = 60, maxSeconds = 604800) private Duration cooldown; @NotNull private Boolean autoPublish; @NotNull private Boolean judgeEnabled;
    @NotBlank @Pattern(regexp = "^[a-z0-9][a-z0-9-]{0,63}$") private String optimizerModelAlias; @NotBlank @Pattern(regexp = "^[a-z0-9][a-z0-9-]{0,63}$") private String judgeModelAlias; @NotNull @Size(max = 4096) private String optimizerInstruction; @NotNull private Boolean rearm;
}
```

- Verification contribution: File 3。
- After this file: 编译通过（`@ValidDurationRange` 在 File 10 创建；File 5 与 File 10 同一编译单元提交）。

#### File 6 — `CREATE egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/java/top/egon/cola/component/tianshu/admin/model/query/DdcAiOptimizationRunPageQuery.java`

- Purpose: API-015/016 查询（合并块：`DdcAiOptimizationRunPageQuery{resourceId, state, trigger, pageNo, pageSize}`、`DdcAiOptimizationRunQuery{resourceId, runId}`）。
- Symbols: Query 组约束同 `DdcAiPublicationPageQuery`。
- Repository evidence: `DdcAiPublicationPageQuery`、`DdcAiVersionQuery`。
- Dependencies and consumers: Service、Controller。
- Why now: Service 签名。
- Contract/signature changes: 新类。
- Input/output and state mapping: 控制器路径/查询串→对象。
- Error and edge behavior: `pageSize @Max(200)`。
- Standards impact: MC-NAME-001, MC-VALID-001。
- Literal rule enforcement: Rule 1、Rule 2、Rule 3、Rule 11。
- Implementation pseudocode:

```java
@Data @NoArgsConstructor @AllArgsConstructor @Accessors(chain = true) @Builder
public class DdcAiOptimizationRunQuery {
    @NotNull(groups = Query.class) @Positive(groups = Query.class) private Long resourceId;
    @NotNull(groups = Query.class) @Positive(groups = Query.class) private Long runId;
}
```

- Verification contribution: File 3。
- After this file: 编译通过。

#### File 7 — `CREATE egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/java/top/egon/cola/component/tianshu/admin/model/vo/DdcAiOptimizationPolicyResult.java`

- Purpose: API-012/013/014/015/016 响应载体（合并块：`DdcAiOptimizationPolicyResult`（含 `persisted`、`revision`、`evaluation{sampleCount, averageScore, pendingJudgeCount, windowStart, windowEnd}`、`activeRunId`）、`DdcAiOptimizationRunResult`）。
- Symbols: ID/序号为十进制字符串；枚举直接输出 code；`Instant` 字段 ISO Z。
- Repository evidence: `DdcAiPublicationResult`。
- Dependencies and consumers: Controller、前端 `types.ts`。
- Why now: Service 返回。
- Contract/signature changes: 新类。
- Input/output and state mapping: 字段按主 Spec §9.2.1/§9.2.5 响应示例。
- Error and edge behavior: 未持久化策略返回默认值 + `persisted=false, revision="0"`。
- Standards impact: MC-NAME-001, MC-JSON-001, MC-MODEL-001, MC-TIME-001。
- Literal rule enforcement: Rule 1、Rule 3、Rule 6、Rule 10、Rule 11。
- Implementation pseudocode:

```java
@Data @NoArgsConstructor @AllArgsConstructor @Accessors(chain = true) @Builder
public class DdcAiOptimizationRunResult {
    @NotBlank private String id; @NotBlank private String resourceId; @NotBlank private String runNo; @NotNull private Integer iterationNo; @NotNull private DdcAiRunTriggerPersistenceEnum trigger; private String requestId;
    @NotNull private DdcAiRunStatePersistenceEnum state; @NotBlank private String baseVersionId; @NotNull private Integer sampleCount; private BigDecimal averageScore; @NotNull private Integer thresholdSnapshot; @NotNull private Instant windowStart; @NotNull private Instant windowEnd;
    @NotBlank private String modelAlias; private String promptSha256; private String outputSha256; private Integer promptTokens; private Integer completionTokens; private String producedVersionId; private String publicationId; @NotNull private Boolean autoPublished;
    private DdcAiRunErrorCodeEnum errorCode; private String errorMessage; @NotNull private String note; private Instant startedAt; private Instant finishedAt; @NotNull private Instant createTime;
}
```

- Verification contribution: File 3。
- After this file: 编译通过。

#### File 8 — `CREATE egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/java/top/egon/cola/component/tianshu/admin/model/dto/DdcAiOptimizationEvaluationDTO.java`

- Purpose: 单次评估结论的不可变值对象。
- Symbols: `record DdcAiOptimizationEvaluationDTO(int sampleCount, BigDecimal averageScore, Instant windowStart, Instant windowEnd, Decision decision)`；`enum Decision { NOT_PUBLISHED, INSUFFICIENT_SAMPLES, THRESHOLD_MET, ITERATIONS_EXHAUSTED, COOLING_DOWN, CREATE_RUN }`
- Repository evidence: record 值对象先例。
- Dependencies and consumers: Service 内部与策略快照。
- Why now: `evaluate` 返回。
- Contract/signature changes: 新 record。
- Input/output and state mapping: 由聚合 + 策略计算。
- Error and edge behavior: `windowStart < windowEnd` 断言。
- Standards impact: MC-MODEL-001, MC-TIME-001。
- Literal rule enforcement: Rule 3、Rule 10、Rule 11。
- Implementation pseudocode:

```java
public record DdcAiOptimizationEvaluationDTO(int sampleCount, BigDecimal averageScore, Instant windowStart, Instant windowEnd, Decision decision) {
    public enum Decision { NOT_PUBLISHED, INSUFFICIENT_SAMPLES, THRESHOLD_MET, ITERATIONS_EXHAUSTED, COOLING_DOWN, CREATE_RUN }
    public DdcAiOptimizationEvaluationDTO { if (!windowStart.isBefore(windowEnd)) throw new IllegalArgumentException("window must be ordered"); Objects.requireNonNull(decision); }
}
```

- Verification contribution: File 3。
- After this file: 编译通过。

#### File 9 — `CREATE egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/java/top/egon/cola/component/tianshu/admin/model/convertor/DdcAiOptimizationPolicyResultConverter.java`

- Purpose: BO→Result 投影（合并块：`DdcAiOptimizationPolicyResultConverter`、`DdcAiOptimizationRunResultConverter`）；`DdcAiOptimizationPolicyConverter`（Step 3）追加 `void applyCommand(DdcAiOptimizationPolicyCommand command, @MappingTarget DdcAiOptimizationPolicyBO target)`（忽略 `expectedRevision/rearm` 与状态列）。
- Symbols: `BaseForwardConverter<DdcAiOptimizationPolicyBO, DdcAiOptimizationPolicyResult>`（`revision` 来自 `version`，`longToDecimal`）；`BaseForwardConverter<DdcAiOptimizationRunBO, DdcAiOptimizationRunResult>`
- Repository evidence: `DdcAiResourceResultConverter`。
- Dependencies and consumers: Service。
- Why now: Service 返回。
- Contract/signature changes: 新 Mapper + 既有 Mapper 追加方法。
- Input/output and state mapping: `Short`→`Integer`（MapStruct 隐式）；`Duration`→`Duration`。
- Error and edge behavior: `unmappedTargetPolicy=ERROR`。
- Standards impact: MC-CONVERT-001。
- Literal rule enforcement: Rule 3、Rule 11。
- Implementation pseudocode:

```java
@Mapper(componentModel = "spring", uses = DdcAiEnumConverter.class, unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface DdcAiOptimizationRunResultConverter extends BaseForwardConverter<DdcAiOptimizationRunBO, DdcAiOptimizationRunResult> {
    @Override @Mapping(target = "id", source = "id", qualifiedByName = "longToDecimal") @Mapping(target = "resourceId", source = "resourceId", qualifiedByName = "longToDecimal") @Mapping(target = "runNo", source = "runNo", qualifiedByName = "longToDecimal")
    @Mapping(target = "baseVersionId", source = "baseVersionId", qualifiedByName = "longToDecimal") @Mapping(target = "producedVersionId", source = "producedVersionId", qualifiedByName = "longToDecimal") @Mapping(target = "publicationId", source = "publicationId", qualifiedByName = "longToDecimal")
    DdcAiOptimizationRunResult toTarget(DdcAiOptimizationRunBO source);
}
// DdcAiOptimizationPolicyConverter 追加：@BeanMapping(ignoreByDefault = true) @Mapping(target="enabled") ... @Mapping(target="optimizerInstruction") void applyCommand(DdcAiOptimizationPolicyCommand c, @MappingTarget DdcAiOptimizationPolicyBO t);
```

- Verification contribution: File 3。
- After this file: 编译通过。

#### File 10 — `CREATE egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/java/top/egon/cola/component/tianshu/admin/service/ai/validation/ValidDurationRange.java`

- Purpose: `Duration` 范围约束注解与校验器（合并块：`ValidDurationRange`、`DurationRangeValidator`）。
- Symbols: `@Documented @Constraint(validatedBy = DurationRangeValidator.class) @Target({FIELD, PARAMETER}) @Retention(RUNTIME) @interface ValidDurationRange { long minSeconds(); long maxSeconds(); String message() default "{top.egon.cola.tianshu.ai.ValidDurationRange.message}"; Class<?>[] groups() default {}; Class<? extends Payload>[] payload() default {}; }`；`class DurationRangeValidator implements ConstraintValidator<ValidDurationRange, Duration>`
- Repository evidence: `@ValidAiContent` 自定义约束先例（base starter `model/ai`）。
- Dependencies and consumers: BO/Command。
- Why now: 校验前置。
- Contract/signature changes: 新注解。
- Input/output and state mapping: null 视为有效（配合 `@NotNull`）。
- Error and edge behavior: 负 Duration 无效。
- Standards impact: MC-VALID-001, MC-TIME-001。
- Literal rule enforcement: Rule 2（ConstraintValidator 扩展）、Rule 10、Rule 11。
- Implementation pseudocode:

```java
public class DurationRangeValidator implements ConstraintValidator<ValidDurationRange, Duration> {
    private long min, max;
    @Override public void initialize(ValidDurationRange a) { min = a.minSeconds(); max = a.maxSeconds(); if (min < 0 || max < min) throw new IllegalArgumentException("invalid duration range"); }
    @Override public boolean isValid(Duration value, ConstraintValidatorContext ctx) { if (value == null) return true; long s = value.toSeconds(); return !value.isNegative() && s >= min && s <= max; }
}
```

- Verification contribution: File 3（命令校验用例）。
- After this file: 编译通过；Step 3 File 7 的 BO 追加该注解（本步 MODIFY `DdcAiOptimizationPolicyBO`）。

#### File 11 — `CREATE egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/java/top/egon/cola/component/tianshu/admin/service/ai/impl/DdcAiOptimizationStateTransition.java`

- Purpose: 表驱动状态机（Rule 9）。
- Symbols: `@Component("ddcAiOptimizationStateTransition")`；`enum Signal { INSUFFICIENT_SAMPLES, THRESHOLD_MET, ITERATIONS_EXHAUSTED, COOLING_DOWN, RUN_CREATED, RUN_SUCCEEDED, RUN_FAILED, RUN_SKIPPED, BASE_PUBLISHED, REARM }`；`record Context(boolean autoPublish, int consecutiveFailures, int pauseAfterFailures)`；`DdcAiPolicyStatePersistenceEnum next(state, signal, context)`
- Repository evidence: Spec §10.6；既有 Strategy 注册表模式。
- Dependencies and consumers: Service。
- Why now: 状态规则集中。
- Contract/signature changes: 新类。
- Input/output and state mapping: `Map<State, Map<Signal, Function<Context, State>>>` 表。
- Error and edge behavior: 未定义→`IllegalStateException("AI_POLICY_TRANSITION_UNDEFINED")`。
- Standards impact: MC-PATTERN-001（State 表驱动）、MC-BEAN-001。
- Literal rule enforcement: Rule 9、Rule 4、Rule 11。
- Implementation pseudocode:

```java
private static final Map<DdcAiPolicyStatePersistenceEnum, Map<Signal, Function<Context, DdcAiPolicyStatePersistenceEnum>>> TABLE = Map.of(
    ARMED, Map.of(INSUFFICIENT_SAMPLES, c -> ARMED, COOLING_DOWN, c -> ARMED, RUN_CREATED, c -> ARMED, THRESHOLD_MET, c -> CONVERGED, ITERATIONS_EXHAUSTED, c -> EXHAUSTED,
                  RUN_SUCCEEDED, c -> c.autoPublish() ? ARMED : AWAITING_PUBLISH, RUN_FAILED, c -> c.consecutiveFailures() >= c.pauseAfterFailures() ? PAUSED : ARMED, RUN_SKIPPED, c -> ARMED, REARM, c -> ARMED),
    AWAITING_PUBLISH, Map.of(BASE_PUBLISHED, c -> ARMED, REARM, c -> ARMED, INSUFFICIENT_SAMPLES, c -> AWAITING_PUBLISH),
    CONVERGED, Map.of(REARM, c -> ARMED), EXHAUSTED, Map.of(REARM, c -> ARMED), PAUSED, Map.of(REARM, c -> ARMED));
public DdcAiPolicyStatePersistenceEnum next(DdcAiPolicyStatePersistenceEnum state, Signal signal, Context context) {
    var fn = TABLE.getOrDefault(state, Map.of()).get(signal); if (fn == null) throw new IllegalStateException("AI_POLICY_TRANSITION_UNDEFINED"); return fn.apply(context);
}
```

- Verification contribution: File 1。
- After this file: File 1 GREEN。

#### File 12 — `CREATE egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/java/top/egon/cola/component/tianshu/admin/service/ai/impl/DdcAiOptimizerStrategy.java`

- Purpose: 改写策略接口与两实现（合并块：`DdcAiOptimizerStrategy`、`DdcPromptOptimizerStrategy`（`@Component("ddcPromptOptimizerStrategy")`）、`DdcSkillOptimizerStrategy`（`@Component("ddcSkillOptimizerStrategy")`）；内部载体 `DdcAiOptimizationContextBO{kind, baselineContent(DdcAiContentDTO), instruction, samples(List<DdcAiUsageFeedbackBO>), maxContextBytes}` 放 `model/bo`）。
- Symbols: `interface DdcAiOptimizerStrategy { DdcAiKindEnum kind(); DdcAiModelRequestDTO compose(DdcAiOptimizationContextBO ctx, String modelAlias, int maxOutputTokens); DdcAiContentDTO parse(String modelText); }`
- Repository evidence: `DdcAiContentStrategy`/`DdcPromptContentStrategy`/`DdcSkillContentStrategy` 的 `kind()` + 注册表。
- Dependencies and consumers: Service `executeRun`。
- Why now: Rule 9。
- Contract/signature changes: 新接口/类。
- Input/output and state mapping: Prompt：用户段 `## Current Prompt`/`## Operator Instruction`/`## Low-score Samples`（每样本 `score/outcome/input/output` 截断）；parse `{"promptText"}`→`DdcAiContentDTO.of(promptText, null)`；Skill：用户段含文件树 JSON（内容截断至 `maxContextBytes` 预算）；parse `{"files":[{path,content}]}`→补齐目录节点→`DdcAiContentDTO.of(null, nodes)`。
- Error and edge behavior: 路径含 `..`、以 `/` 开头、重复、无根 `SKILL.md`、文件数 >128、单文件 >128KiB、非字符串内容→`IllegalArgumentException("INVALID_OUTPUT")`；最终限额仍由 `ValidationUtils.validate(content, Save, Publish)` 在 Service 执行。
- Standards impact: MC-PATTERN-001, MC-JSON-001, MC-UTIL-001, MC-BEAN-001, MC-LOG-001。
- Literal rule enforcement: Rule 9、Rule 6、Rule 5、Rule 4、Rule 11。
- Implementation pseudocode:

```java
@Slf4j @Component("ddcSkillOptimizerStrategy") @RequiredArgsConstructor(onConstructor_ = @Autowired)
public class DdcSkillOptimizerStrategy implements DdcAiOptimizerStrategy {
    @Qualifier("ddcAiPromptTemplateLoader") private final DdcAiPromptTemplateLoader templates; @Qualifier("jacksonObjectMapper") private final ObjectMapper objectMapper;
    @Override public DdcAiKindEnum kind() { return DdcAiKindEnum.SKILL; }
    @Override public DdcAiContentDTO parse(String text) {
        try { JsonNode files = objectMapper.readTree(stripFences(text)).path("files"); if (!files.isArray() || files.isEmpty() || files.size() > 128) throw invalid();
              LinkedHashMap<String, DdcAiNodeDTO> nodes = new LinkedHashMap<>();
              for (JsonNode f : files) { String path = f.path("path").asText(null); String content = f.path("content").isTextual() ? f.get("content").asText() : null;
                  if (path == null || content == null || path.startsWith("/") || path.contains("..") || path.isBlank() || content.getBytes(UTF_8).length > 128 * 1024) throw invalid();
                  addDirectories(nodes, path); if (nodes.put(path, DdcAiNodeDTO.builder().path(path).nodeType(FILE).content(content).build()) != null) throw invalid(); }
              if (!nodes.containsKey("SKILL.md")) throw invalid();
              return DdcAiContentDTO.of(null, List.copyOf(nodes.values())); }
        catch (JsonProcessingException e) { throw new IllegalArgumentException("INVALID_OUTPUT", e); }
    }
}
```

- Verification contribution: File 2。
- After this file: File 2 GREEN。

#### File 13 — `CREATE egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/java/top/egon/cola/component/tianshu/admin/service/ai/DdcAiOptimizationService.java`

- Purpose: 优化服务接口（REST + 作业）。
- Symbols: `DdcAiOptimizationPolicyResult getPolicy(@Valid DdcAiIdQuery)`；`DdcAiOptimizationPolicyResult replacePolicy(@Valid DdcAiIdQuery, @Valid DdcAiOptimizationPolicyCommand)`；`DdcAiOptimizationRunResult trigger(@Valid DdcAiIdQuery, requestId, @Valid DdcAiOptimizationTriggerCommand)`；`DdcPageResult<DdcAiOptimizationRunResult> pageRuns(@Valid DdcAiOptimizationRunPageQuery)`；`DdcAiOptimizationRunResult getRun(@Valid DdcAiOptimizationRunQuery)`；`int reclaimExpiredRuns(Instant now)`；`List<Long> evaluateTenant(Instant now)`；`boolean claimRun(Long runId, String owner, Instant now)`；`void executeRun(Long runId)`；`void onResourceDeleted(Long resourceId)`（Step 10 由 `DdcAiResourceServiceImpl#delete` 同事务调用）
- Repository evidence: `DdcAiResourceService` 接口写法。
- Dependencies and consumers: Controller、作业、`DdcAiResourceServiceImpl`。
- Why now: 实现前置。
- Contract/signature changes: 新接口。
- Input/output and state mapping: 见 Impl。
- Error and edge behavior: 见 Impl。
- Standards impact: MC-NAME-001, MC-VALID-001。
- Literal rule enforcement: Rule 2、Rule 11。
- Implementation pseudocode:

```java
@Validated public interface DdcAiOptimizationService {
    DdcAiOptimizationPolicyResult getPolicy(@NotNull @Valid DdcAiIdQuery query);
    DdcAiOptimizationPolicyResult replacePolicy(@NotNull @Valid DdcAiIdQuery query, @NotNull @Valid DdcAiOptimizationPolicyCommand command);
    DdcAiOptimizationRunResult trigger(@NotNull @Valid DdcAiIdQuery query, @NotBlank @Pattern(regexp = "^[A-Za-z0-9_-]{16,64}$") String requestId, @NotNull @Valid DdcAiOptimizationTriggerCommand command);
    int reclaimExpiredRuns(@NotNull Instant now); List<Long> evaluateTenant(@NotNull Instant now); boolean claimRun(@NotNull @Positive Long runId, @NotBlank String owner, @NotNull Instant now); void executeRun(@NotNull @Positive Long runId);
}
```

- Verification contribution: File 3。
- After this file: 编译通过。

#### File 14 — `CREATE egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/java/top/egon/cola/component/tianshu/admin/service/ai/impl/DdcAiOptimizationServiceImpl.java`

- Purpose: 优化服务实现（评估、运行、REST）。
- Symbols: `@Service("ddcAiOptimizationService")`；注入 `@Qualifier("ddcAiOptimizationPolicyRepository")`、`@Qualifier("ddcAiOptimizationRunRepository")`、`@Qualifier("ddcAiResourceRepository")`、`@Qualifier("ddcAiVersionRepository")`、`@Qualifier("ddcAiVersionNodeRepository")`、`@Qualifier("ddcAiDraftNodeRepository")`、`@Qualifier("ddcAiFeedbackService")`、`@Qualifier("ddcAiResourceService")`、`@Qualifier("ddcAiScopeValidator")`、`@Qualifier("ddcAiOptimizationStateTransition")`、`@Qualifier("ddcAiOptimizerStrategyRegistry") Map<DdcAiKindEnum, DdcAiOptimizerStrategy>`、`ObjectProvider<DdcAiModelPort>`（通道关闭时为空→56322）、`@Qualifier("ddcAiOptimizationPolicyConverterImpl")`、两个 Result 转换器、`@Qualifier("egonColaValidationUtils") ValidationUtils`、`@Qualifier("ddcAiCommandTransactionTemplate")`、`@Qualifier("ddcAiReadTransactionTemplate")`、`DdcAiOptimizationProperties`、`@Qualifier("ddcPublishClock") Clock`
- Repository evidence: `DdcAiResourceServiceImpl`（`commandSha256`、`replayOrConflict`、`lockResourceAfterScope`、`preparePersistedDraft` 的 hash 比较、`DdcAiIdQuery`）。
- Dependencies and consumers: Controller、作业。
- Why now: 核心闭环。
- Contract/signature changes: 新类。
- Input/output and state mapping: `getPolicy`：读事务→资源存在 + `assertCanRead`→策略行或默认 + 评估快照（`feedbackService.aggregate(current, now-window, now)` 与待评分数）；`replacePolicy`：命令事务→`resourceRepository.findActiveForUpdateById` + `assertCanPublish`→策略 FOR UPDATE（`findActiveByResourceId`）→CAS `version==expectedRevision`（无行要求 `"0"` 并 insert）→`rearm && activeRunId!=null`→56323；`rearm`→`next(state, REARM)`、计数清零→`updatePolicy`；`trigger`：`modelPort.getIfAvailable()==null`→56322；`assertCanPublish`；`findByResourceAndRequestId` 重放/56305（摘要 `sha256("tianshu-ai-command-v1|OPTIMIZE|" + actor + "|" + resourceId + "|" + note)`）；策略无行→56324；`activeRunId!=null`→56323；资源无 `currentVersionId`→56306；插入 `PENDING MANUAL run_no=nextRunNo`，捕获 `DuplicateKeyException`→复核 requestId 否则 56323；`evaluateTenant`：`lockEvaluable(now, 50)` 逐策略：AWAITING_PUBLISH 且 `resource.currentVersionId != lastRun.baseVersionId`→`BASE_PUBLISHED`；未发布→快照 NOT_PUBLISHED；聚合→决策→`transition.next`→可能 `insertRun(SCHEDULED)` + `activeRunId`；`claimRun`：`runRepository.claimPending(id, version, owner, now+runLease, now)`；`executeRun`：读运行/策略/资源/基线版本与节点、`feedbackService.worstSamples`→`strategy.compose`→`modelPort.complete`→`parse`→`validationUtils.validate(content, Save, Publish)`→命令事务：`lockResourceAfterScope` 等价（`findActiveForUpdateById`）→`currentVersionId==baseVersionId` 否则 SKIPPED(BASE_CHANGED)→草稿 hash==版本 hash 否则 SKIPPED(DRAFT_DIRTY)→`DdcAiSystemActor.bind(OPTIMIZER, () -> resourceService.replaceDraft(idQuery, draftCommand(expectedRevision=resource.version)))`→`autoPublish`→`resourceService.publish(idQuery, "opt-" + tenantId + "-" + leftPad(runId, 19, '0'), publishCommand(changeNote="ai-optimization run " + runNo + ": " + note))`→运行 SUCCEEDED（producedVersionId/publicationId/autoPublished/hash/tokens/finishedAt）→策略 `iterationsDone++`、`consecutiveFailures=0`、`activeRunId=null`、`next(RUN_SUCCEEDED)`。
- Error and edge behavior: `DdcAiModelException`→FAILED(code)；`IllegalArgumentException("INVALID_OUTPUT")`/`ConstraintViolationException`→FAILED(INVALID_OUTPUT)；`CommonException(56303)` 于 publish→FAILED(PUBLISH_CONFLICT)；任何 FAILED：独立命令事务写运行终态 + 策略 `consecutiveFailures++`、`activeRunId=null`、`next(RUN_FAILED)`；SKIPPED 不计失败；`reclaimExpiredRuns`：`findExpiredRunning(now, 100)`→FAILED(LEASE_EXPIRED) + 策略计数；所有 `updateRun/updatePolicy` CAS 失败→`IllegalStateException("AI_OPTIMIZATION_CAS_MISS")` 回滚。
- Standards impact: MC-LOG-001, MC-BEAN-001, MC-VALID-001, MC-TIME-001, MC-PATTERN-001（Strategy + State）、MC-UTIL-001（commons-codec `DigestUtils`、commons-lang3 `StringUtils.leftPad`）。
- Literal rule enforcement: Rule 4、Rule 2、Rule 5、Rule 9、Rule 10、Rule 11。
- Implementation pseudocode:

```java
@Override public void executeRun(Long runId) {
    Prepared p = readTransactionTemplate.execute(s -> loadForExecution(runId));           // run(RUNNING)、policy、resource、baseline version+nodes、worst samples；基线已变→直接 SKIPPED(BASE_CHANGED)
    if (p == null) return;
    DdcAiContentDTO candidate;
    try { DdcAiOptimizerStrategy strategy = strategies.get(p.resource().getKind()); DdcAiModelResultDTO out = modelPort().complete(strategy.compose(p.context(), p.policy().getOptimizerModelAlias(), properties.getModel().getMaxOutputTokens()));
          if (out.truncated()) throw new IllegalArgumentException("INVALID_OUTPUT"); candidate = strategy.parse(out.text()); validationUtils.validate(candidate, DdcAiValidationGroups.Save.class, DdcAiValidationGroups.Publish.class); p = p.withModel(out); }
    catch (DdcAiModelException m) { finishFailed(p, m.getCode(), m.getMessage()); return; }
    catch (IllegalArgumentException | ConstraintViolationException bad) { finishFailed(p, DdcAiRunErrorCodeEnum.INVALID_OUTPUT, "optimizer output rejected"); return; }
    final Prepared prepared = p; final DdcAiContentDTO content = candidate;
    try {
        commandTransactionTemplate.executeWithoutResult(status -> {
            DdcAiResourceBO resource = resourceRepository.findActiveForUpdateById(prepared.resource().getId()).orElseThrow(DdcAiOptimizationServiceImpl::notFound);
            if (!Objects.equals(resource.getCurrentVersionId(), prepared.run().getBaseVersionId())) { finishSkipped(prepared, BASE_CHANGED); return; }
            if (!draftMatchesVersion(resource, prepared.baselineNodes())) { finishSkipped(prepared, DRAFT_DIRTY); return; }
            DdcAiIdQuery idQuery = DdcAiIdQuery.builder().id(resource.getId()).build();
            DdcAiResourceResult draft = DdcAiSystemActor.bind(DdcAiSystemActor.OPTIMIZER, () -> resourceService.replaceDraft(idQuery, DdcAiDraftCommand.builder().expectedRevision(Long.toString(resource.getVersion())).displayName(resource.getDisplayName()).description(resource.getDescription()).draft(content).build()));
            DdcAiPublicationResult publication = null;
            if (Boolean.TRUE.equals(prepared.policy().getAutoPublish())) publication = DdcAiSystemActor.bind(DdcAiSystemActor.OPTIMIZER, () -> resourceService.publish(idQuery, "opt-" + tenantId() + "-" + StringUtils.leftPad(Long.toString(prepared.run().getId()), 19, '0'), DdcAiPublishCommand.builder().expectedRevision(draft.getResource().getRevision()).changeNote("ai-optimization run " + prepared.run().getRunNo() + ": " + prepared.run().getNote()).build()));
            finishSucceeded(prepared, draft, publication);
        });
    } catch (CommonException conflict) { if (conflict.getCode() == 56303) finishFailed(prepared, PUBLISH_CONFLICT, "publish revision conflict"); else throw conflict; }
}
```

- Verification contribution: File 3 全部用例。
- After this file: 本步 GREEN（注册表 Bean 在 File 15）。

#### File 15 — `MODIFY egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/java/top/egon/cola/component/tianshu/admin/config/DdcAiOptimizationConfiguration.java`

- Purpose: `ddcAiOptimizerStrategyRegistry` Bean（EnumMap，不依赖通道开关，因 REST 触发仍需 56322 判定）。
- Symbols: `@Bean("ddcAiOptimizerStrategyRegistry") Map<DdcAiKindEnum, DdcAiOptimizerStrategy>`
- Repository evidence: `DdcAiTransactionConfiguration#ddcAiContentStrategyRegistry`。
- Dependencies and consumers: File 14。
- Why now: 注册表。
- Contract/signature changes: 新 Bean。
- Input/output and state mapping: 两策略→不可变 EnumMap；缺失→`IllegalStateException("AI_OPTIMIZER_STRATEGY_REGISTRATION_INCOMPLETE")`。
- Error and edge behavior: 重复 kind→异常。
- Standards impact: MC-BEAN-001, MC-PATTERN-001。
- Literal rule enforcement: Rule 4、Rule 9、Rule 11。
- Implementation pseudocode:

```java
@Bean("ddcAiOptimizerStrategyRegistry")
public Map<DdcAiKindEnum, DdcAiOptimizerStrategy> ddcAiOptimizerStrategyRegistry(@Qualifier("ddcPromptOptimizerStrategy") DdcAiOptimizerStrategy prompt, @Qualifier("ddcSkillOptimizerStrategy") DdcAiOptimizerStrategy skill) {
    EnumMap<DdcAiKindEnum, DdcAiOptimizerStrategy> map = new EnumMap<>(DdcAiKindEnum.class);
    for (DdcAiOptimizerStrategy s : List.of(prompt, skill)) if (map.putIfAbsent(s.kind(), s) != null) throw new IllegalStateException("AI_OPTIMIZER_STRATEGY_DUPLICATE:" + s.kind());
    if (!map.keySet().equals(Set.of(DdcAiKindEnum.PROMPT, DdcAiKindEnum.SKILL))) throw new IllegalStateException("AI_OPTIMIZER_STRATEGY_REGISTRATION_INCOMPLETE");
    return Collections.unmodifiableMap(map);
}
```

- Verification contribution: Step 11 装配测试。
- After this file: 本步完成。

- Validation working directory: 仓库根
- Verification command: `./mvnw -pl egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin -Dtest=DdcAiOptimizationStateTransitionTest,DdcAiOptimizerStrategyTest,DdcAiOptimizationServiceTest,DdcAiOptimizationConverterTest -Dsurefire.failIfNoSpecifiedTests=false test`
- Expected result: 四测试类通过。
- Failure returns to: File 11（状态表）、File 12（parse）、File 14（事务分支）。
- Completion criteria: 状态矩阵 9 行、两策略 compose/parse、评估/执行/回收/触发/策略替换全部有断言。
- Rollback: 回退本步 Commit paths。
- Commit paths: egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/test/java/top/egon/cola/component/tianshu/admin/service/ai/DdcAiOptimizationStateTransitionTest.java, egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/test/java/top/egon/cola/component/tianshu/admin/service/ai/DdcAiOptimizerStrategyTest.java, egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/test/java/top/egon/cola/component/tianshu/admin/service/ai/DdcAiOptimizationServiceTest.java, egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/resources/tianshu/ai/prompts/optimize-prompt-system.txt, egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/resources/tianshu/ai/prompts/optimize-skill-system.txt, egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/java/top/egon/cola/component/tianshu/admin/service/ai/impl/DdcAiPromptTemplateLoader.java, egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/java/top/egon/cola/component/tianshu/admin/model/command/DdcAiOptimizationPolicyCommand.java, egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/java/top/egon/cola/component/tianshu/admin/model/command/DdcAiOptimizationTriggerCommand.java, egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/java/top/egon/cola/component/tianshu/admin/model/query/DdcAiOptimizationRunPageQuery.java, egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/java/top/egon/cola/component/tianshu/admin/model/query/DdcAiOptimizationRunQuery.java, egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/java/top/egon/cola/component/tianshu/admin/model/vo/DdcAiOptimizationPolicyResult.java, egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/java/top/egon/cola/component/tianshu/admin/model/vo/DdcAiOptimizationRunResult.java, egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/java/top/egon/cola/component/tianshu/admin/model/dto/DdcAiOptimizationEvaluationDTO.java, egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/java/top/egon/cola/component/tianshu/admin/model/bo/DdcAiOptimizationContextBO.java, egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/java/top/egon/cola/component/tianshu/admin/model/bo/DdcAiOptimizationPolicyBO.java, egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/java/top/egon/cola/component/tianshu/admin/model/convertor/DdcAiOptimizationPolicyResultConverter.java, egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/java/top/egon/cola/component/tianshu/admin/model/convertor/DdcAiOptimizationRunResultConverter.java, egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/java/top/egon/cola/component/tianshu/admin/model/convertor/DdcAiOptimizationPolicyConverter.java, egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/java/top/egon/cola/component/tianshu/admin/service/ai/validation/ValidDurationRange.java, egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/java/top/egon/cola/component/tianshu/admin/service/ai/validation/DurationRangeValidator.java, egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/java/top/egon/cola/component/tianshu/admin/service/ai/impl/DdcAiOptimizationStateTransition.java, egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/java/top/egon/cola/component/tianshu/admin/service/ai/impl/DdcAiOptimizerStrategy.java, egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/java/top/egon/cola/component/tianshu/admin/service/ai/impl/DdcPromptOptimizerStrategy.java, egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/java/top/egon/cola/component/tianshu/admin/service/ai/impl/DdcSkillOptimizerStrategy.java, egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/java/top/egon/cola/component/tianshu/admin/service/ai/DdcAiOptimizationService.java, egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/java/top/egon/cola/component/tianshu/admin/service/ai/impl/DdcAiOptimizationServiceImpl.java, egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/java/top/egon/cola/component/tianshu/admin/config/DdcAiOptimizationConfiguration.java
- Commit: `feat(tianshu): ai optimization policy state machine and optimizer service`

### Step 10 — EVENT-103：Outbox 生产者、投递处理器与既有发布事务入队

- Requirements: `REQ-001`, `REQ-007`, `REQ-014`
- Dependencies: Step 1（事件类、`aiTopic`）、Step 5（系统主体）、Step 9（`onResourceDeleted`）
- Baseline state: `DdcNotificationPublisher`（`OutboxMessage.builder().messageId/idempotencyKey/channel/destination/payload/contentType/schemaVersion/header`，`transactionalOutbox.enqueue`）；`DdcRegistryEventDeliveryHandler implements DeliveryHandler`（`channel()`、`validateDestination` 正则、`deliver` 用 `redissonClient.getTopic(destination, StringCodec.INSTANCE).publish(payload)`、`DeliveryResult.permanentFailure/retryableFailure`）；`DdcOutboxConfiguration` 的 `DdcConfigDispatchOutboxRouteDTO`；`DdcAiResourceServiceImpl` 三处 `publicationResult(publicationRepository.insertPublication(publication))`（第 349/385/492 行）与 `authenticatedActor()`（第 687 行）、`deleteInTransaction`（第 872 行）。
- Observable outcome: `DdcAiPublicationOutboxTest` 证明信封 JSON、目的地 `tianshu:v4:tenant:{t}:{hash}:ai:topic`、`eventId=ai-pub-{publicationId}`、投递 permanent/retryable 分类；`DdcAiVersionServiceTest` 新用例证明 publish/rollback/promote 在回执插入后同事务调用 `notifier.enqueue`，入队异常使事务回滚（不吞异常，与注册表通知的 best-effort 不同——Spec §7.3.3 要求同事务）。
- End state: 既有三条发布路径产生 EVENT-103；系统主体可作为 `actor` 发布；资源删除级联软删除策略。
- Test-first gate: Required — `DdcAiPublicationOutboxTest` 与 `DdcAiVersionServiceTest` 新用例 RED。
- Manual Checks: MC-ARCH-001, MC-REUSE-001, MC-NAME-001, MC-VALID-001, MC-MODEL-001, MC-CONVERT-001, MC-LOG-001, MC-BEAN-001, MC-JSON-001, MC-TIME-001, MC-SCOPE-001, MC-TEST-001
- Literal Rules: Rule 1, Rule 2, Rule 3, Rule 4, Rule 6, Rule 10, Rule 11
- Ordered files:

#### File 1 — `CREATE egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/test/java/top/egon/cola/component/tianshu/admin/service/notification/DdcAiPublicationOutboxTest.java`

- Purpose: TEST-022（Admin 侧）。
- Symbols: `enqueuesTenantEnvelopeWithAiTopicDestination`、`eventIdDerivesFromPublicationId`、`deliveryRejectsForeignDestinationPermanently`、`deliveryRetriesOnRedisFailure`、`notifierIsNamedServiceWithQualifiedDependencies`
- Repository evidence: `DdcRegistryOutboxTest`（Mockito `TransactionalOutbox`、`RedissonClient`，Bean 名反射断言）。
- Dependencies and consumers: File 2–5。
- Why now: RED。
- Contract/signature changes: 无。
- Input/output and state mapping: `DdcAiPublicationBO + DdcAiResourceBO + versionNo/contentSha256`→`OutboxMessage`（捕获断言 channel/destination/headers/payload JSON 字段）。
- Error and edge behavior: `deliver` 的 destination 不匹配 `^tianshu:v4:tenant:[1-9][0-9]*:\{[a-f0-9]{64}}:ai:topic$`→permanent；`publish` 抛 `RedisException`→retryable。
- Standards impact: MC-TEST-001, MC-JSON-001。
- Literal rule enforcement: Rule 11。
- Implementation pseudocode:

```java
@Test void enqueuesTenantEnvelopeWithAiTopicDestination() throws Exception {
    MDC.put("tenantId", "42"); notifier.enqueue(publication(id 555L, PUBLISH, targetVersionId 9L, targetVersionNo 3L, resultRevision 7L), resource(kind SKILL, biz "xingyuan", ns "ns-a", env "dev", app "order"), "sha…", "system:ai-optimizer");
    ArgumentCaptor<OutboxMessage> c = ArgumentCaptor.forClass(OutboxMessage.class); verify(outbox).enqueue(c.capture());
    assertThat(c.getValue().channel()).isEqualTo("tianshu.ai-publication"); assertThat(c.getValue().destination()).isEqualTo(DdcRedisKeys.aiTopic(42L, "xingyuan", "dev", "order")); assertThat(c.getValue().messageId()).isEqualTo("ai-pub-555");
    JsonNode payload = mapper.readTree(c.getValue().payload()); assertThat(payload.path("tenantId").asLong()).isEqualTo(42L); assertThat(payload.at("/payload/versionNo").asLong()).isEqualTo(3L); assertThat(payload.at("/payload/operation").asText()).isEqualTo("PUBLISH");
}
@Test void deliveryRejectsForeignDestinationPermanently() { assertThatThrownBy(() -> handler.validateDestination("tianshu:v4:tenant:42:{" + "a".repeat(64) + "}:registry:topic:x")).isInstanceOf(OutboxValidationException.class); }
```

- Verification contribution: RED→GREEN。
- After this file: RED。

#### File 2 — `CREATE egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/java/top/egon/cola/component/tianshu/admin/model/convertor/DdcAiPublicationEventConverter.java`

- Purpose: 回执 + 资源→事件投影。
- Symbols: `@Mapper(componentModel="spring", uses=DdcAiEnumConverter.class, unmappedTargetPolicy=ERROR) interface DdcAiPublicationEventConverter extends BaseForwardConverter<DdcAiPublicationBO, DdcAiPublicationEvent>`；`default DdcAiPublicationEvent toEvent(DdcAiPublicationBO publication, DdcAiResourceBO resource, String contentSha256, String actor, Instant occurredAt, Long tenantId)`
- Repository evidence: `DdcAiPublicationConverter`；`DdcAiPublicationEvent` 字段（Step 1 File 14）。
- Dependencies and consumers: Notifier。
- Why now: Notifier 前置。
- Contract/signature changes: 新 Mapper。
- Input/output and state mapping: `eventId="ai-pub-"+publication.id`；`resourceRevision=publication.resultRevision`；`versionId/versionNo=targetVersionId/targetVersionNo`；`operation=publication.operation.code`。
- Error and edge behavior: 缺 `targetVersionId`→`IllegalArgumentException`。
- Standards impact: MC-CONVERT-001, MC-TIME-001。
- Literal rule enforcement: Rule 3、Rule 10、Rule 11。
- Implementation pseudocode:

```java
default DdcAiPublicationEvent toEvent(DdcAiPublicationBO p, DdcAiResourceBO r, String contentSha256, String actor, Instant occurredAt, Long tenantId) {
    if (p.getTargetVersionId() == null) throw new IllegalArgumentException("publication target version is required");
    return DdcAiPublicationEvent.builder().eventId("ai-pub-" + p.getId()).tenantId(tenantId).resourceId(r.getId()).kind(r.getKind().name()).bizCode(r.getBizCode()).namespaceCode(r.getNamespaceCode()).env(r.getEnvCode()).appCode(r.getAppCode())
        .resourceKey(r.getResourceKey()).operation(p.getOperation().getCode()).versionId(p.getTargetVersionId()).versionNo(p.getTargetVersionNo()).contentSha256(contentSha256).resourceRevision(p.getResultRevision()).actor(actor).occurredAt(occurredAt).build();
}
```

- Verification contribution: File 1。
- After this file: 编译通过。

#### File 3 — `MODIFY egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/java/top/egon/cola/component/tianshu/admin/config/DdcOutboxConfiguration.java`

- Purpose: 新路由常量 Bean `ddcAiPublicationOutboxRoute`。
- Symbols: `record DdcAiPublicationOutboxRouteDTO(String channel, String schemaVersion)`（`channel="tianshu.ai-publication"`，`eventType()="TIANSHU_AI_PUBLICATION"`，`destination(tenantId, bizCode, env, appCode)`→`DdcRedisKeys.aiTopic`，`TOPIC_PATTERN ^tianshu:v4:tenant:[1-9][0-9]*:\{[a-f0-9]{64}}:ai:topic$`）
- Repository evidence: 同文件 `DdcConfigDispatchOutboxRouteDTO`。
- Dependencies and consumers: Notifier、Handler。
- Why now: 路由单一来源。
- Contract/signature changes: 新 Bean/record。
- Input/output and state mapping: 常量。
- Error and edge behavior: 构造校验。
- Standards impact: MC-BEAN-001, MC-MODEL-001（record 值对象）。
- Literal rule enforcement: Rule 4、Rule 3、Rule 11。
- Implementation pseudocode:

```java
@Bean("ddcAiPublicationOutboxRoute") public DdcAiPublicationOutboxRouteDTO ddcAiPublicationOutboxRoute() { return new DdcAiPublicationOutboxRouteDTO("tianshu.ai-publication", "1"); }
public record DdcAiPublicationOutboxRouteDTO(String channel, String schemaVersion) {
    private static final Pattern TOPIC_PATTERN = Pattern.compile("^tianshu:v4:tenant:[1-9][0-9]*:\\{[a-f0-9]{64}}:ai:topic$");
    public DdcAiPublicationOutboxRouteDTO { if (!"tianshu.ai-publication".equals(channel) || !"1".equals(schemaVersion)) throw new IllegalArgumentException("Invalid Tianshu ai-publication Outbox route"); }
    public String eventType() { return "TIANSHU_AI_PUBLICATION"; }
    public String destination(Long tenantId, String bizCode, String env, String appCode) { String d = DdcRedisKeys.aiTopic(tenantId, bizCode, env, appCode); if (!TOPIC_PATTERN.matcher(d).matches()) throw new IllegalStateException("AI_TOPIC_DESTINATION_INVALID"); return d; }
}
```

- Verification contribution: File 1。
- After this file: 编译通过。

#### File 4 — `CREATE egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/java/top/egon/cola/component/tianshu/admin/service/ai/notification/DdcAiPublicationNotifier.java`

- Purpose: 在当前命令事务内入队 EVENT-103。
- Symbols: `@Service("ddcAiPublicationNotifier")`；注入 `@Qualifier("transactionalOutbox") TransactionalOutbox`（Bean 名以既有 `DdcNotificationPublisher` 注入为准）、`@Qualifier("jacksonObjectMapper") ObjectMapper`、`@Qualifier("ddcAiPublicationEventConverterImpl")`、`@Qualifier("ddcAiPublicationOutboxRoute")`、`@Qualifier("ddcPublishClock") Clock`；`void enqueue(@Valid DdcAiPublicationBO, @Valid DdcAiResourceBO, @Pattern hex64 String contentSha256, @NotBlank String actor)`
- Repository evidence: `DdcNotificationPublisher#enqueue`（第 63–74 行）。
- Dependencies and consumers: `DdcAiResourceServiceImpl`（File 6）。
- Why now: 生产者。
- Contract/signature changes: 新类。
- Input/output and state mapping: `DdcTenantMessageDTO<>(tenantId, event)`→JSON→`OutboxMessage`；`tenantId` 取 `EgonColaTenantIdProvider.currentTenantId()`。
- Error and edge behavior: 与注册表通知不同：不在内部开事务、不吞异常——`JsonProcessingException`→`IllegalStateException("AI_PUBLICATION_EVENT_SERIALIZE_FAILED")`，调用方事务回滚（Spec §7.3.3 原子性）。
- Standards impact: MC-LOG-001, MC-BEAN-001, MC-JSON-001, MC-VALID-001。
- Literal rule enforcement: Rule 4、Rule 2、Rule 6、Rule 11（Event 经 Outbox）。
- Implementation pseudocode:

```java
public void enqueue(DdcAiPublicationBO publication, DdcAiResourceBO resource, String contentSha256, String actor) {
    Long tenantId = EgonColaTenantIdProvider.currentTenantId();
    DdcAiPublicationEvent event = converter.toEvent(publication, resource, contentSha256, actor, clock.instant(), tenantId);
    String payload; try { payload = objectMapper.writeValueAsString(new DdcTenantMessageDTO<>(tenantId, event)); } catch (JsonProcessingException e) { throw new IllegalStateException("AI_PUBLICATION_EVENT_SERIALIZE_FAILED", e); }
    transactionalOutbox.enqueue(OutboxMessage.builder().messageId(event.getEventId()).idempotencyKey(event.getEventId()).channel(route.channel()).destination(route.destination(tenantId, resource.getBizCode(), resource.getEnvCode(), resource.getAppCode()))
        .payload(payload).contentType("application/json").schemaVersion(route.schemaVersion()).header("tenantId", Long.toString(tenantId)).header("eventType", route.eventType()).build());
    log.info("AI_PUBLICATION_ENQUEUED tenantId={} eventId={} resourceId={} versionNo={}", tenantId, event.getEventId(), resource.getId(), event.getVersionNo());
}
```

- Verification contribution: File 1。
- After this file: 编译通过。

#### File 5 — `CREATE egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/java/top/egon/cola/component/tianshu/admin/service/ai/notification/DdcAiPublicationDeliveryHandler.java`

- Purpose: Outbox `DeliveryHandler` 投递到 Redis Topic。
- Symbols: `@Component("ddcAiPublicationDeliveryHandler") implements DeliveryHandler`；`channel()`、`validateDestination`、`deliver`；`TypeReference<DdcTenantMessageDTO<DdcAiPublicationEvent>>`
- Repository evidence: `DdcRegistryEventDeliveryHandler`（逐行同构）。
- Dependencies and consumers: Outbox dispatcher（按 channel 自动发现）。
- Why now: 投递。
- Contract/signature changes: 新类。
- Input/output and state mapping: payload 原样 `publish`；解析仅用于校验与日志。
- Error and edge behavior: 解析/约束失败→`permanentFailure("TIANSHU_AI_PUBLICATION_EVENT_INVALID")`；Redis 异常→`retryableFailure("TIANSHU_AI_PUBLICATION_DELIVERY_RETRY")`；信封 `tenantId` 与 destination 中租户不一致→permanent。
- Standards impact: MC-LOG-001, MC-BEAN-001, MC-JSON-001。
- Literal rule enforcement: Rule 4、Rule 6、Rule 11。
- Implementation pseudocode:

```java
@Override public DeliveryResult deliver(DeliveryContext context) {
    DdcTenantMessageDTO<DdcAiPublicationEvent> envelope;
    try { envelope = objectMapper.readValue(context.payload(), ENVELOPE_TYPE); validationUtils.validate(envelope); validationUtils.validate(envelope.getPayload());
          if (!context.destination().startsWith("tianshu:v4:tenant:" + envelope.getTenantId() + ":")) throw new IllegalArgumentException("tenant mismatch"); }
    catch (JsonProcessingException | ConstraintViolationException | IllegalArgumentException invalid) { log.warn("Rejecting invalid Tianshu ai-publication outbox record: messageId={}", context.messageId()); return DeliveryResult.permanentFailure("TIANSHU_AI_PUBLICATION_EVENT_INVALID", "invalid ai publication event"); }
    try { redissonClient.getTopic(context.destination(), StringCodec.INSTANCE).publish(context.payload()); return DeliveryResult.success(); }
    catch (RuntimeException transientFailure) { log.warn("Tianshu ai-publication delivery failed and will be retried: tenantId={}, eventId={}", envelope.getTenantId(), envelope.getPayload().getEventId(), transientFailure); return DeliveryResult.retryableFailure("TIANSHU_AI_PUBLICATION_DELIVERY_RETRY", "ai topic publish failed"); }
}
```

- Verification contribution: File 1。
- After this file: File 1 GREEN。

#### File 6 — `MODIFY egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/java/top/egon/cola/component/tianshu/admin/service/ai/impl/DdcAiResourceServiceImpl.java`

- Purpose: 三条发布事务入队；`authenticatedActor` 识别系统主体；`delete` 级联策略软删除。
- Symbols: 注入 `@Qualifier("ddcAiPublicationNotifier") DdcAiPublicationNotifier notifier`、`@Qualifier("ddcAiOptimizationService") ObjectProvider<DdcAiOptimizationService>`（避免循环依赖：`DdcAiOptimizationServiceImpl` 依赖本服务）；`publishInTransaction/rollbackInTransaction/promoteInTransaction` 在 `insertPublication` 后调用 `notifier.enqueue(publication, resource, prepared.contentSha256(), authenticatedActor())`；`authenticatedActor()` 先 `DdcAiSystemActor.current()`；`deleteInTransaction` 末尾 `optimizationService.ifAvailable(s -> s.onResourceDeleted(resource.getId()))`
- Repository evidence: 第 349/385/492 行 `publicationResult(publicationRepository.insertPublication(publication))`；第 687 行 `authenticatedActor`；第 872 行 `deleteInTransaction`。
- Dependencies and consumers: 全部发布路径。
- Why now: REQ-001 复用既有链路 + REQ-007。
- Contract/signature changes: 公共方法签名不变；构造器新增两参数（Lombok）。
- Input/output and state mapping: `contentSha256` 取 `PreparedContent` 既有 hash（promote 用源版本 hash）。
- Error and edge behavior: `enqueue` 抛出→整个命令事务回滚（既有 `executeWithLifecycleRetries` 不重试 `IllegalStateException`）。
- Standards impact: MC-SCOPE-001（最小插入点）、MC-BEAN-001。
- Literal rule enforcement: Rule 4、Rule 11（Event 经 Outbox 同事务）。
- Implementation pseudocode:

```java
// publishInTransaction（第 349 行）：
DdcAiPublicationBO receipt = publicationRepository.insertPublication(publication);
notifier.enqueue(receipt, resource, prepared.contentSha256(), authenticatedActor());
return publicationResult(receipt);
// rollbackInTransaction / promoteInTransaction 同构（promote 的 resource 为目标资源、hash 为源版本 hash）
private String authenticatedActor() {
    Optional<String> system = DdcAiSystemActor.current(); if (system.isPresent()) return system.get();
    /* 既有 Rbac3UserDetails 分支逐行保留 */
}
// deleteInTransaction 末尾：optimizationService.ifAvailable(service -> service.onResourceDeleted(resource.getId()));
```

- Verification contribution: File 7。
- After this file: 编译通过。

#### File 7 — `MODIFY egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/test/java/top/egon/cola/component/tianshu/admin/service/ai/DdcAiVersionServiceTest.java`

- Purpose: 证明三条路径入队与回滚语义、系统主体 actor。
- Symbols: `publishEnqueuesPublicationEventInsideCommandTransaction`、`rollbackAndPromoteEnqueueEvents`、`enqueueFailureRollsBackPublication`、`systemActorIsAcceptedAsPublicationActor`
- Repository evidence: 既有 publish/rollback/promote 用例（Mockito `TransactionTemplate` 替身）。
- Dependencies and consumers: File 6。
- Why now: GREEN 证据。
- Contract/signature changes: 测试构造器参数更新。
- Input/output and state mapping: `verify(notifier).enqueue(any(), any(), eq(sha), eq("user-sub"))`；`DdcAiSystemActor.bind(OPTIMIZER, () -> service.publish(...))` 后 actor 为 `system:ai-optimizer`。
- Error and edge behavior: `doThrow(new IllegalStateException()).when(notifier).enqueue(...)`→`publish` 抛出且 `insertPublication` 发生在同一 `execute` 回调内（通过替身记录调用顺序断言）。
- Standards impact: MC-TEST-001。
- Literal rule enforcement: Rule 11。
- Implementation pseudocode:

```java
@Test void enqueueFailureRollsBackPublication() {
    doThrow(new IllegalStateException("AI_PUBLICATION_EVENT_SERIALIZE_FAILED")).when(notifier).enqueue(any(), any(), anyString(), anyString());
    assertThatThrownBy(() -> service.publish(idQuery(7L), KEY, publishCommand())).isInstanceOf(IllegalStateException.class);
    InOrder order = inOrder(publicationRepository, notifier); order.verify(publicationRepository).insertPublication(any()); order.verify(notifier).enqueue(any(), any(), anyString(), anyString());
}
@Test void systemActorIsAcceptedAsPublicationActor() { DdcAiSystemActor.bind(DdcAiSystemActor.OPTIMIZER, () -> service.publish(idQuery(7L), KEY, publishCommand())); verify(notifier).enqueue(any(), any(), anyString(), eq("system:ai-optimizer")); }
```

- Verification contribution: 本步 GREEN。
- After this file: 本步完成。

- Validation working directory: 仓库根
- Verification command: `./mvnw -pl egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin -Dtest=DdcAiPublicationOutboxTest,DdcAiVersionServiceTest,DdcAiPromotionServiceTest,DdcAiDraftServiceTest,DdcRegistryOutboxTest -Dsurefire.failIfNoSpecifiedTests=false test`
- Expected result: 五测试类通过（既有 AI 服务测试在构造器变更后仍绿）。
- Failure returns to: File 6（插入点）、File 4（信封）。
- Completion criteria: 三路径入队、回滚、投递分类、系统主体均有断言。
- Rollback: 回退本步 Commit paths。
- Commit paths: egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/test/java/top/egon/cola/component/tianshu/admin/service/notification/DdcAiPublicationOutboxTest.java, egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/java/top/egon/cola/component/tianshu/admin/model/convertor/DdcAiPublicationEventConverter.java, egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/java/top/egon/cola/component/tianshu/admin/config/DdcOutboxConfiguration.java, egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/java/top/egon/cola/component/tianshu/admin/service/ai/notification/DdcAiPublicationNotifier.java, egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/java/top/egon/cola/component/tianshu/admin/service/ai/notification/DdcAiPublicationDeliveryHandler.java, egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/java/top/egon/cola/component/tianshu/admin/service/ai/impl/DdcAiResourceServiceImpl.java, egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/test/java/top/egon/cola/component/tianshu/admin/service/ai/DdcAiVersionServiceTest.java
- Commit: `feat(tianshu): publish ai publication events through transactional outbox`

### Step 11 — 优化/回收作业装配与 PostgreSQL 集成测试

- Requirements: `REQ-002`, `REQ-003`, `REQ-004`, `REQ-005`, `REQ-007`, `REQ-011`
- Dependencies: Step 8、Step 9、Step 10
- Baseline state: `A/pom.xml` profile `tianshu-ai-persistence-it` 的 Failsafe `<includes>` 列出 8 个 IT；`DdcAiPersistenceIT` 以 `TIANSHU_AI_PERSISTENCE_IT_ENABLED/JDBC_URL/DB_USERNAME/DB_PASSWORD` 环境变量启用；`DdcAiOptimizationConfiguration` 已有模型/执行器/Judge/注册表 Bean。
- Observable outcome: `DdcAiOptimizationScannerTest`/`DdcAiFeedbackRetentionScannerTest` 单测通过；`DdcAiOptimizationConfigurationTest`（`ApplicationContextRunner`）证明通道开关真值；三个 IT 在 Failsafe profile 下可运行（TEST-004/005/011/015）。
- End state: 后台闭环完整；IT 需要用户提供 PG/Redis 环境（§8 user-controlled runtime）。
- Test-first gate: Required — 两个作业单测与装配测试 RED；IT 为新文件（RED 需环境，静态编译先行）。
- Manual Checks: MC-ARCH-001, MC-REUSE-001, MC-NAME-001, MC-LOG-001, MC-BEAN-001, MC-TIME-001, MC-CONFIG-001, MC-SCOPE-001, MC-TEST-001
- Literal Rules: Rule 4, Rule 7, Rule 10, Rule 11
- Ordered files:

#### File 1 — `CREATE egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/test/java/top/egon/cola/component/tianshu/admin/service/ai/DdcAiOptimizationScannerTest.java`

- Purpose: 三阶段编排与执行器提交（合并块：`DdcAiOptimizationScannerTest`、`DdcAiFeedbackRetentionScannerTest`）。
- Symbols: `scanReclaimsEvaluatesThenClaimsAndExecutesOnExecutor`、`claimFailureSkipsExecution`、`tenantBindingChangeIsLoggedAndSkipped`；retention：`purgesInBatchesUntilBelowLimit`
- Repository evidence: Step 8 File 2 风格。
- Dependencies and consumers: File 2/3。
- Why now: RED。
- Contract/signature changes: 无。
- Input/output and state mapping: `evaluateTenant` 返回 `[11L, 12L]`；`claimRun(11L, owner, now)` true、`claimRun(12L)` false→仅 `executeRun(11L)`；执行器用 `Runnable::run` 的同步 `ExecutorService` 替身。
- Error and edge behavior: `executeRun` 抛 RuntimeException→记录 ERROR，继续。
- Standards impact: MC-TEST-001。
- Literal rule enforcement: Rule 11。
- Implementation pseudocode:

```java
@Test void scanReclaimsEvaluatesThenClaimsAndExecutesOnExecutor() {
    when(optimizationService.reclaimExpiredRuns(now)).thenReturn(1); when(optimizationService.evaluateTenant(now)).thenReturn(List.of(11L, 12L));
    when(optimizationService.claimRun(11L, "admin-1", now)).thenReturn(true); when(optimizationService.claimRun(12L, "admin-1", now)).thenReturn(false);
    scanner.scanOptimization();
    InOrder o = inOrder(optimizationService); o.verify(optimizationService).reclaimExpiredRuns(now); o.verify(optimizationService).evaluateTenant(now); o.verify(optimizationService).executeRun(11L); verify(optimizationService, never()).executeRun(12L);
}
@Test void purgesInBatchesUntilBelowLimit() { when(feedbackService.purgeExpired(1000)).thenReturn(1000, 1000, 7); assertThat(retentionScanner.scanRetention()).isEqualTo(2007); }
```

- Verification contribution: RED→GREEN。
- After this file: RED。

#### File 2 — `CREATE egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/java/top/egon/cola/component/tianshu/admin/service/ai/job/DdcAiOptimizationScanner.java`

- Purpose: JOB-002 与 JOB-003（合并块：`DdcAiOptimizationScanner`、`DdcAiFeedbackRetentionScanner`）。
- Symbols: `scanOptimization()` `@Scheduled(fixedDelayString="${egon.cola.component.tianshu.admin.ai.optimization.scan-interval:60s}")`；逐租户：`reclaimExpiredRuns(now)`→`evaluateTenant(now)`→对每个 runId：`claimRun(runId, instanceId, now)` 为 true 时 `executor.submit(() -> tenantContextExecutor.call(ctx, () -> DdcAiSystemActor.bind(OPTIMIZER, () -> { optimizationService.executeRun(runId); return null; })))`；`scanRetention()` `@Scheduled(fixedDelayString="${...feedback.retention-scan-interval:1h}")` 逐租户 `purgeExpired(1000)` 循环直到 <1000
- Repository evidence: `DdcLeaseExpiryScanner`；`DdcAiJudgeScanner`（Step 8）。
- Dependencies and consumers: File 3 装配。
- Why now: 闭环驱动。
- Contract/signature changes: 新类。
- Input/output and state mapping: `instanceId` 来自 `DdcAdminProperties`/环境主机名（既有实例标识若存在则复用，否则 `InetAddress.getLocalHost().getHostName() + ":" + pid`，PLAN-CLAR-012）。
- Error and edge behavior: 认领与执行均在系统主体与租户上下文内；执行器拒绝（已满）→不认领下一条（提交前检查 `semaphore.tryAcquire`）。
- Standards impact: MC-LOG-001, MC-TIME-001, MC-BEAN-001。
- Literal rule enforcement: Rule 4、Rule 10、Rule 11。
- Implementation pseudocode:

```java
@Scheduled(fixedDelayString = "${egon.cola.component.tianshu.admin.ai.optimization.scan-interval:60s}", initialDelayString = "${egon.cola.component.tianshu.admin.ai.optimization.scan-interval:60s}")
public int scanOptimization() {
    int submitted = 0;
    for (DdcTenantBindingDTO configured : tenantProperties.getBindings()) {
        if (configured == null || !configured.isEnabled()) continue;
        try { DdcTenantContextDTO ctx = tenantContext(configured, DdcAiSystemActor.OPTIMIZER); Instant now = clock.instant();
              List<Long> runIds = tenantContextExecutor.call(ctx, () -> DdcAiSystemActor.bind(DdcAiSystemActor.OPTIMIZER, () -> { optimizationService.reclaimExpiredRuns(now); return optimizationService.evaluateTenant(now); }));
              for (Long runId : runIds) { if (!slots.tryAcquire()) break;
                  boolean claimed = tenantContextExecutor.call(ctx, () -> DdcAiSystemActor.bind(DdcAiSystemActor.OPTIMIZER, () -> optimizationService.claimRun(runId, instanceId, clock.instant())));
                  if (!claimed) { slots.release(); continue; }
                  executor.submit(() -> { try { tenantContextExecutor.call(ctx, () -> DdcAiSystemActor.bind(DdcAiSystemActor.OPTIMIZER, () -> { optimizationService.executeRun(runId); return null; })); } catch (RuntimeException f) { log.error("AI_OPT_RUN execution failed runId={}", runId, f); } finally { slots.release(); } }); submitted++; } }
        catch (RuntimeException failure) { log.error("AI optimization scan failed for tenantId={}", configured.getTenantId(), failure); }
    }
    return submitted;
}
```

- Verification contribution: File 1。
- After this file: File 1 GREEN（直接构造）。

#### File 3 — `MODIFY egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/java/top/egon/cola/component/tianshu/admin/config/DdcAiOptimizationConfiguration.java`

- Purpose: 装配 `ddcAiOptimizationScanner`、`ddcAiFeedbackRetentionScanner`（后者不依赖通道开关：保留期清理在 `feedback.retention` 下始终运行）。
- Symbols: `@Bean("ddcAiOptimizationScanner") @ConditionalOnProperty(prefix=OPT, name="enabled", havingValue="true")`；`@Bean("ddcAiFeedbackRetentionScanner")`
- Repository evidence: Step 8 File 7。
- Dependencies and consumers: 调度。
- Why now: 作业生效。
- Contract/signature changes: 新 Bean。
- Input/output and state mapping: 注入 `@Qualifier("ddcAiOptimizationExecutor") ExecutorService`、`@Qualifier("ddcAiOptimizationService")`、`@Qualifier("ddcAiFeedbackService")`、租户三件套、`@Qualifier("ddcPublishClock") Clock`。
- Error and edge behavior: 通道关闭只装配清理作业。
- Standards impact: MC-BEAN-001, MC-CONFIG-001。
- Literal rule enforcement: Rule 4、Rule 7、Rule 11。
- Implementation pseudocode:

```java
@Bean("ddcAiOptimizationScanner") @ConditionalOnProperty(prefix = OPT, name = "enabled", havingValue = "true")
public DdcAiOptimizationScanner ddcAiOptimizationScanner(@Qualifier("ddcAiOptimizationService") DdcAiOptimizationService optimizationService, @Qualifier("ddcAiOptimizationExecutor") ExecutorService executor,
        @Qualifier("egon.cola.component.tianshu.admin.tenancy-top.egon.cola.component.tianshu.admin.security.tenancy.DdcTenantAdminProperties") DdcTenantAdminProperties tenantProperties, @Qualifier("ddcTenantRegistry") DdcTenantRegistry tenantRegistry,
        @Qualifier("ddcTenantContextExecutor") DdcTenantContextExecutor tenantContextExecutor, DdcAiOptimizationProperties properties, @Qualifier("ddcPublishClock") Clock clock) {
    return new DdcAiOptimizationScanner(optimizationService, executor, tenantProperties, tenantRegistry, tenantContextExecutor, properties.getOptimization().getMaxConcurrentRuns(), clock);
}
@Bean("ddcAiFeedbackRetentionScanner")
public DdcAiFeedbackRetentionScanner ddcAiFeedbackRetentionScanner(@Qualifier("ddcAiFeedbackService") DdcAiFeedbackService feedbackService, /* 租户三件套 */ @Qualifier("ddcPublishClock") Clock clock) { return new DdcAiFeedbackRetentionScanner(feedbackService, tenantProperties, tenantRegistry, tenantContextExecutor, clock); }
```

- Verification contribution: File 4。
- After this file: 装配就绪。

#### File 4 — `CREATE egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/test/java/top/egon/cola/component/tianshu/admin/config/DdcAiOptimizationConfigurationTest.java`

- Purpose: 通道开关真值（PLAN-CLAR-007）。
- Symbols: `channelDisabledRegistersOnlyRetentionScannerAndRegistry`、`channelEnabledRegistersModelPortJudgeAndOptimizationScanners`、`channelEnabledWithoutBaseUrlFailsFast`
- Repository evidence: `DdcAdminRedisConfigTest`（`ApplicationContextRunner` + `withUserConfiguration`）。
- Dependencies and consumers: File 3。
- Why now: 装配证据。
- Contract/signature changes: 无。
- Input/output and state mapping: `withBean` 注入替身（Service、OAuth2 client、租户三件套、Clock、ObjectMapper）。
- Error and edge behavior: `enabled=true` 且无 base-url→`context.getStartupFailure()` 消息含 `TIANSHU_AI_MODEL_BASE_URL_REQUIRED`。
- Standards impact: MC-TEST-001, MC-CONFIG-001。
- Literal rule enforcement: Rule 7、Rule 11。
- Implementation pseudocode:

```java
@Test void channelEnabledRegistersModelPortJudgeAndOptimizationScanners() {
    runner.withPropertyValues("egon.cola.component.tianshu.admin.ai.optimization.enabled=true", "egon.cola.component.tianshu.admin.ai.model.base-url=http://127.0.0.1:1", "egon.cola.component.tianshu.admin.ai.model.registration-id=r", "egon.cola.component.tianshu.admin.ai.model.app-id=a", "egon.cola.component.tianshu.admin.ai.model.resource-uri=https://yuheng.test/llm")
        .run(ctx -> assertThat(ctx).hasBean("ddcAiModelPort").hasBean("ddcAiJudgeScanner").hasBean("ddcAiOptimizationScanner").hasBean("ddcAiOptimizationExecutor").hasBean("ddcAiFeedbackRetentionScanner").hasBean("ddcAiOptimizerStrategyRegistry"));
}
@Test void channelDisabledRegistersOnlyRetentionScannerAndRegistry() { runner.run(ctx -> assertThat(ctx).doesNotHaveBean("ddcAiModelPort").doesNotHaveBean("ddcAiJudgeScanner").doesNotHaveBean("ddcAiOptimizationScanner").hasBean("ddcAiFeedbackRetentionScanner").hasBean("ddcAiOptimizerStrategyRegistry")); }
```

- Verification contribution: 本步 GREEN。
- After this file: 单测完成。

#### File 5 — `CREATE egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/test/java/top/egon/cola/component/tianshu/admin/integration/DdcAiFeedbackPersistenceIT.java`

- Purpose: 三个 PG IT（合并块：`DdcAiFeedbackPersistenceIT`（TEST-004/005）、`DdcAiOptimizationConcurrencyIT`（TEST-011）、`DdcAiOptimizationPublishIT`（TEST-015））。
- Symbols: feedback IT：`rejectsOccurredAtOutsideWindow`、`isolatesTenants`、`softDeletesExpiredInBatches`、`concurrentJudgeClaimsNeverOverlap`、`expiredJudgeLeaseIsReclaimed`；concurrency IT：`onlyOneActiveRunPerResourceUnderConcurrentEvaluateAndTrigger`；publish IT：`autoPublishRunProducesVersionReceiptOutboxRowAndSucceededRun`、`outboxEnqueueFailureRollsBackPublication`
- Repository evidence: `DdcAiPersistenceIT`（环境变量门控、`Assumptions.assumeTrue`、Spring 上下文 + 真实 PG）；`DdcConfigOutboxIntegrationTest` 读取 `egon_cola_outbox_message`。
- Dependencies and consumers: Failsafe profile（File 6）。
- Why now: 运行时证据（用户环境）。
- Contract/signature changes: 无。
- Input/output and state mapping: 真实表行数与 `deleted_at`；`SELECT count(*) FROM egon_cola_outbox_message WHERE channel='tianshu.ai-publication'`。
- Error and edge behavior: 未设环境变量→跳过（不是失败）。
- Standards impact: MC-TEST-001。
- Literal rule enforcement: Rule 11。
- Implementation pseudocode:

```java
@Test void concurrentJudgeClaimsNeverOverlap() throws Exception {
    seedPendingFeedback(resourceId, 40);
    var pool = Executors.newFixedThreadPool(2); var a = pool.submit(() -> feedbackService.claimForJudge(Set.of(resourceId), 25)); var b = pool.submit(() -> feedbackService.claimForJudge(Set.of(resourceId), 25));
    Set<Long> ids = new HashSet<>(a.get().stream().map(DdcAiUsageFeedbackBO::getId).toList()); assertThat(ids).doesNotContainAnyElementsOf(b.get().stream().map(DdcAiUsageFeedbackBO::getId).toList()); assertThat(ids.size() + b.get().size()).isEqualTo(40);
}
@Test void autoPublishRunProducesVersionReceiptOutboxRowAndSucceededRun() {
    stubModelPort("{\"promptText\":\"improved\"}"); long runId = createPendingRun(resourceId, autoPublish true); assertThat(optimizationService.claimRun(runId, "it", now)).isTrue(); optimizationService.executeRun(runId);
    assertThat(jdbc.queryForObject("select state from ddc_ai_optimization_run_t0 where id = ?", String.class, runId)).isEqualTo("SUCCEEDED");
    assertThat(jdbc.queryForObject("select count(*) from egon_cola_outbox_message where channel = 'tianshu.ai-publication' and message_id = ?", Long.class, "ai-pub-" + publicationIdOf(runId))).isEqualTo(1L);
}
```

- Verification contribution: Failsafe 门禁（用户环境）。
- After this file: IT 可编译。

#### File 6 — `MODIFY egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/pom.xml`

- Purpose: Failsafe includes 追加三个 IT。
- Symbols: `<include>**/DdcAiFeedbackPersistenceIT.java</include>`、`<include>**/DdcAiOptimizationConcurrencyIT.java</include>`、`<include>**/DdcAiOptimizationPublishIT.java</include>`
- Repository evidence: `A/pom.xml:264–272`。
- Dependencies and consumers: `-Ptianshu-ai-persistence-it verify`。
- Why now: IT 纳入门禁。
- Contract/signature changes: 构建配置。
- Input/output and state mapping: 无。
- Error and edge behavior: 无。
- Standards impact: MC-TEST-001, MC-SCOPE-001。
- Literal rule enforcement: Rule 11。
- Implementation pseudocode:

```xml
<include>**/DdcLifecycleUniqueIT.java</include>
<include>**/DdcAiFeedbackPersistenceIT.java</include>
<include>**/DdcAiOptimizationConcurrencyIT.java</include>
<include>**/DdcAiOptimizationPublishIT.java</include>
```

- Verification contribution: Failsafe。
- After this file: 本步完成。

- Validation working directory: 仓库根
- Verification command: `./mvnw -pl egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin -Dtest=DdcAiOptimizationScannerTest,DdcAiFeedbackRetentionScannerTest,DdcAiOptimizationConfigurationTest -Dsurefire.failIfNoSpecifiedTests=false test && ./mvnw -q -pl egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin -DskipTests test-compile`；运行时：`TIANSHU_AI_PERSISTENCE_IT_ENABLED=true TIANSHU_AI_PERSISTENCE_IT_JDBC_URL=... ./mvnw -pl egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin -am -Ptianshu-ai-persistence-it verify`
- Expected result: 单测通过、IT 编译；IT 在用户环境通过（TEST-004/005/011/015）。
- Failure returns to: File 2（编排）、Step 9 File 14（事务）、Step 10 File 6（入队）。
- Completion criteria: 三阶段顺序、并发互斥、同事务 Outbox 行均有断言。
- Rollback: 回退本步 Commit paths。
- Commit paths: egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/test/java/top/egon/cola/component/tianshu/admin/service/ai/DdcAiOptimizationScannerTest.java, egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/test/java/top/egon/cola/component/tianshu/admin/service/ai/DdcAiFeedbackRetentionScannerTest.java, egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/java/top/egon/cola/component/tianshu/admin/service/ai/job/DdcAiOptimizationScanner.java, egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/java/top/egon/cola/component/tianshu/admin/service/ai/job/DdcAiFeedbackRetentionScanner.java, egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/java/top/egon/cola/component/tianshu/admin/config/DdcAiOptimizationConfiguration.java, egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/test/java/top/egon/cola/component/tianshu/admin/config/DdcAiOptimizationConfigurationTest.java, egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/test/java/top/egon/cola/component/tianshu/admin/integration/DdcAiFeedbackPersistenceIT.java, egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/test/java/top/egon/cola/component/tianshu/admin/integration/DdcAiOptimizationConcurrencyIT.java, egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/test/java/top/egon/cola/component/tianshu/admin/integration/DdcAiOptimizationPublishIT.java, egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/pom.xml
- Commit: `feat(tianshu): ai optimization scanners with postgres integration tests`

### Step 12 — REST API-012～017、错误码与 OpenAPI 契约

- Requirements: `REQ-004`, `REQ-005`, `REQ-009`
- Dependencies: Step 5、Step 9
- Baseline state: `DdcAiResourceController` 11 个 operation（常量 `TENANT_HEADER`、`IDEMPOTENCY_HEADER`、`IDEMPOTENCY_PATTERN`、`LONG_ID_PATTERN`；`@EgonApiCatalog`、`@EgonGatewayPolicy(EXTERNAL)`、`@Operation(operationId)`、`Cache-Control: no-store` 响应头）；`DdcAiExceptionAdvice#httpStatus` switch（56301..56999）；`DdcAiWebConfiguration` 严格绑定类列表（5 个 Command）；`DdcAiOpenApiContractTest#assertOperation(paths, path, method, operationId, status)`。
- Observable outcome: 6 个新 operation 的 MockMvc 用例（TEST-016/017/020/021）与 OAS 用例（TEST-018）通过；新错误码映射 400/503/409/404。
- End state: 前端可对接。
- Test-first gate: Required — `DdcAiOptimizationControllerTest` 新建 RED；`DdcAiOpenApiContractTest` 新断言 RED。
- Manual Checks: MC-ARCH-001, MC-REUSE-001, MC-NAME-001, MC-VALID-001, MC-JSON-001, MC-TIME-001, MC-SCOPE-001, MC-TEST-001
- Literal Rules: Rule 2, Rule 6, Rule 10, Rule 11
- Ordered files:

#### File 1 — `CREATE egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/test/java/top/egon/cola/component/tianshu/admin/controller/DdcAiOptimizationControllerTest.java`

- Purpose: TEST-016/017/020/021。
- Symbols: `getPolicyReturnsDefaultsWhenNotPersisted`、`replacePolicyCreatesWithRevisionZeroAndConflictsOnStale`、`replacePolicyRejectsRearmWithActiveRun409`、`replacePolicyRejectsDurationOutOfRange400`、`triggerReturns202WithLocationAndReplaysSameKey`、`triggerRejectsMissingIdempotencyKey400`、`triggerReturns503WhenChannelDisabled`、`pageRunsFiltersByStateAndTrigger`、`getRunReturns404ForForeignResource`、`pageFeedbackRequiresReadPermission`
- Repository evidence: `DdcAiResourceControllerTest`（`@WebMvcTest` + `@MockBean` Service + 租户头）。
- Dependencies and consumers: File 2–4。
- Why now: RED。
- Contract/signature changes: 无。
- Input/output and state mapping: 断言 `ResultRecord` 七字段与 `Location` 头。
- Error and edge behavior: `CommonException(56323)`→409；`56322`→503；`56325`→404。
- Standards impact: MC-TEST-001。
- Literal rule enforcement: Rule 11。
- Implementation pseudocode:

```java
@Test void triggerReturns202WithLocationAndReplaysSameKey() throws Exception {
    when(optimizationService.trigger(eq(idQuery(7L)), eq(KEY), any())).thenReturn(run(id 88L, runNo 4L, PENDING));
    mvc.perform(post("/api/v1/tianshu/ai/resources/7/optimization-runs").header(TENANT, "42").header("Idempotency-Key", KEY).contentType(APPLICATION_JSON).content("{\"note\":\"n\"}"))
       .andExpect(status().isAccepted()).andExpect(header().string("Location", "/api/v1/tianshu/ai/resources/7/optimization-runs/88")).andExpect(jsonPath("$.data.state").value("PENDING")).andExpect(header().string("Cache-Control", "no-store"));
}
@Test void replacePolicyRejectsDurationOutOfRange400() throws Exception { mvc.perform(put("/api/v1/tianshu/ai/resources/7/optimization-policy").header(TENANT, "42").contentType(APPLICATION_JSON).content(policyJson("evaluationWindow", "PT1M"))).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value(56301)); }
```

- Verification contribution: RED→GREEN。
- After this file: RED。

#### File 2 — `MODIFY egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/java/top/egon/cola/component/tianshu/admin/controller/ai/DdcAiExceptionAdvice.java`

- Purpose: 新增 6 个业务码状态映射。
- Symbols: `httpStatus` switch 追加 `case 56320, 56321 -> BAD_REQUEST`、`case 56322 -> SERVICE_UNAVAILABLE`、`case 56323, 56324 -> CONFLICT`、`case 56325 -> NOT_FOUND`
- Repository evidence: 第 74–83 行。
- Dependencies and consumers: 控制器。
- Why now: 状态码正确。
- Contract/signature changes: 无。
- Input/output and state mapping: 码→状态。
- Error and edge behavior: 其余不变。
- Standards impact: MC-SCOPE-001。
- Literal rule enforcement: Rule 11。
- Implementation pseudocode:

```java
case 56301, 56308, 56320, 56321 -> HttpStatus.BAD_REQUEST;
case 56302, 56306, 56325 -> HttpStatus.NOT_FOUND;
case 56303, 56304, 56305, 56311, 56314, 56323, 56324 -> HttpStatus.CONFLICT;
case 56310, 56313, 56322 -> HttpStatus.SERVICE_UNAVAILABLE;
```

- Verification contribution: File 1。
- After this file: 编译通过。

#### File 3 — `MODIFY egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/java/top/egon/cola/component/tianshu/admin/config/DdcAiWebConfiguration.java`

- Purpose: 严格绑定类列表追加两个命令。
- Symbols: 列表追加 `DdcAiOptimizationPolicyCommand.class`、`DdcAiOptimizationTriggerCommand.class`
- Repository evidence: 第 67–71 行。
- Dependencies and consumers: Jackson 严格反序列化（未知字段→400）。
- Why now: 请求体规范。
- Contract/signature changes: 无。
- Input/output and state mapping: 无。
- Error and edge behavior: 未知字段→56301。
- Standards impact: MC-JSON-001。
- Literal rule enforcement: Rule 6、Rule 11。
- Implementation pseudocode:

```java
// DdcAiWebConfiguration 严格绑定类列表（既有五项之后追加两项，保持 FAIL_ON_UNKNOWN_PROPERTIES 严格反序列化）
DdcAiRollbackCommand.class,
DdcAiPromotionCommand.class,
DdcAiOptimizationPolicyCommand.class,
DdcAiOptimizationTriggerCommand.class
```

- Verification contribution: File 1 未知字段用例。
- After this file: 编译通过。

#### File 4 — `MODIFY egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/java/top/egon/cola/component/tianshu/admin/controller/ai/DdcAiResourceController.java`

- Purpose: 6 个新端点。
- Symbols: 注入 `@Qualifier("ddcAiOptimizationService") DdcAiOptimizationService optimizationService`、`@Qualifier("ddcAiFeedbackService") DdcAiFeedbackService feedbackService`；`getOptimizationPolicy`（`GET /{id}/optimization-policy`，operationId `getAiOptimizationPolicy`）、`replaceOptimizationPolicy`（`PUT`，`replaceAiOptimizationPolicy`）、`triggerOptimizationRun`（`POST /{id}/optimization-runs`，`triggerAiOptimizationRun`，202 + Location）、`pageOptimizationRuns`（`GET /{id}/optimization-runs`，`pageAiOptimizationRuns`）、`getOptimizationRun`（`GET /{id}/optimization-runs/{runId}`，`getAiOptimizationRun`）、`pageUsageFeedback`（`GET /{id}/usage-feedback`，`pageAiUsageFeedback`）
- Repository evidence: `publish` 方法注解集（第 321–345 行）；`@Parameters` 租户/幂等头；`LONG_ID_PATTERN`。
- Dependencies and consumers: 前端、OAS。
- Why now: REST 暴露。
- Contract/signature changes: 新方法；既有 11 个不变。
- Input/output and state mapping: 路径 `id/runId` `@Pattern(LONG_ID_PATTERN)`→`Long.parseLong`；查询串 `state/trigger/outcome/judgeState/versionId/from/to/pageNo/pageSize`；响应 `ResultRecord.success(...)`/`PageResultRecord`；`Cache-Control: no-store`。
- Error and edge behavior: `Idempotency-Key` 缺失/非法→既有 `MissingRequestHeaderException`/`ConstraintViolationException`→56301（TEST-021）。
- Standards impact: MC-VALID-001, MC-JSON-001, MC-TIME-001（`Instant` 查询参数 `@DateTimeFormat(iso = DATE_TIME)`）。
- Literal rule enforcement: Rule 2、Rule 10、Rule 11。
- Implementation pseudocode:

```java
@Operation(operationId = "triggerAiOptimizationRun", summary = "手动触发一次优化运行", security = @SecurityRequirement(name = "tianshuBearerAuth"))
@Parameters({ @Parameter(name = TENANT_HEADER, in = ParameterIn.HEADER, required = true), @Parameter(name = IDEMPOTENCY_HEADER, in = ParameterIn.HEADER, required = true, schema = @Schema(type = "string", pattern = IDEMPOTENCY_PATTERN, minLength = 16, maxLength = 64)) })
@ApiResponses({ @ApiResponse(responseCode = "202", description = "Optimization run accepted", headers = { @Header(name = "Location", schema = @Schema(type = "string")), @Header(name = "Cache-Control", schema = @Schema(type = "string", example = "no-store")) }), /* 400/401/403/404/409/503/500 同 publish */ })
@EgonGatewayPolicy(exposure = EgonGatewayPolicy.Exposure.EXTERNAL) @PostMapping(path = "/{id}/optimization-runs", consumes = MediaType.APPLICATION_JSON_VALUE)
public ResponseEntity<ResultRecord<DdcAiOptimizationRunResult>> triggerOptimizationRun(@PathVariable("id") @Pattern(regexp = LONG_ID_PATTERN) String id, @RequestHeader(IDEMPOTENCY_HEADER) @Pattern(regexp = IDEMPOTENCY_PATTERN) String requestId, @Valid @RequestBody DdcAiOptimizationTriggerCommand command) {
    DdcAiOptimizationRunResult run = optimizationService.trigger(DdcAiIdQuery.builder().id(Long.parseLong(id)).build(), requestId, command);
    return ResponseEntity.accepted().header(HttpHeaders.CACHE_CONTROL, "no-store").header(HttpHeaders.LOCATION, "/api/v1/tianshu/ai/resources/" + id + "/optimization-runs/" + run.getId()).body(ResultRecord.success(run));
}
```

- Verification contribution: File 1、File 5。
- After this file: File 1 GREEN。

#### File 5 — `MODIFY egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/test/java/top/egon/cola/component/tianshu/admin/controller/DdcAiOpenApiContractTest.java`

- Purpose: TEST-018。
- Symbols: 追加 6 行 `assertOperation(paths, ".../optimization-policy", "get", "getAiOptimizationPolicy", "200")` 等；`optimizationRunAcceptedExposesLocationHeader`；`newOperationsCarryBearerSecurity`
- Repository evidence: 第 54–56 行既有断言；`assertOperation` 第 143 行。
- Dependencies and consumers: File 4。
- Why now: 契约证据。
- Contract/signature changes: 无。
- Input/output and state mapping: `/v3/api-docs/tianshu` JSON。
- Error and edge behavior: 无。
- Standards impact: MC-TEST-001。
- Literal rule enforcement: Rule 11。
- Implementation pseudocode:

```java
assertOperation(paths, "/api/v1/tianshu/ai/resources/{id}/optimization-policy", "get", "getAiOptimizationPolicy", "200");
assertOperation(paths, "/api/v1/tianshu/ai/resources/{id}/optimization-policy", "put", "replaceAiOptimizationPolicy", "200");
assertOperation(paths, "/api/v1/tianshu/ai/resources/{id}/optimization-runs", "post", "triggerAiOptimizationRun", "202");
assertOperation(paths, "/api/v1/tianshu/ai/resources/{id}/optimization-runs", "get", "pageAiOptimizationRuns", "200");
assertOperation(paths, "/api/v1/tianshu/ai/resources/{id}/optimization-runs/{runId}", "get", "getAiOptimizationRun", "200");
assertOperation(paths, "/api/v1/tianshu/ai/resources/{id}/usage-feedback", "get", "pageAiUsageFeedback", "200");
```

- Verification contribution: 本步 GREEN。
- After this file: 本步完成。

- Validation working directory: 仓库根
- Verification command: `./mvnw -pl egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin -Dtest=DdcAiOptimizationControllerTest,DdcAiOpenApiContractTest,DdcAiResourceControllerTest,DdcGlobalExceptionHandlerTest -Dsurefire.failIfNoSpecifiedTests=false test`
- Expected result: 四测试类通过；既有 11 个 operation 的断言不变。
- Failure returns to: File 4（注解/绑定）、File 2（状态码）。
- Completion criteria: 6 operation、6 错误码、Location/Cache-Control 头均有断言。
- Rollback: 回退本步 Commit paths。
- Commit paths: egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/test/java/top/egon/cola/component/tianshu/admin/controller/DdcAiOptimizationControllerTest.java, egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/java/top/egon/cola/component/tianshu/admin/controller/ai/DdcAiExceptionAdvice.java, egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/java/top/egon/cola/component/tianshu/admin/config/DdcAiWebConfiguration.java, egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/java/top/egon/cola/component/tianshu/admin/controller/ai/DdcAiResourceController.java, egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/test/java/top/egon/cola/component/tianshu/admin/controller/DdcAiOpenApiContractTest.java
- Commit: `feat(tianshu): ai optimization and feedback rest api with openapi contract`

### Step 13 — RPC-004/005/006：版本头、Skill 草稿上传与发布

- Requirements: `REQ-007`, `REQ-008`, `REQ-011`
- Dependencies: Step 6、Step 10
- Baseline state: Provider 三方法为 `UNIMPLEMENTED` 占位；`DdcAiResourceService` 无版本头查询；`DdcAiSyncCommandConverter` 仅上报方向。
- Observable outcome: `DdcAiResourceRpcProviderTest` 新用例证明：`GetVersionHead` 只读返回 `published=false` 或版本头；`ReplaceSkillDraft` 在系统外以 RPC 主体（`AI_WRITE`）调用既有 `replaceDraft`（kind 必须为 SKILL，否则 `INVALID_ARGUMENT`）并返回修订/哈希；`PublishSkillDraft` 以 `request_id` 调用既有 `publish`（`AI_PUBLISH`）并返回回执；56303→`ABORTED` + typed trailer。
- End state: 下游上行链路服务端完成。
- Test-first gate: Required — Provider 测试新用例 RED（删除 Step 6 的 `unimplementedSyncMethodsUntilStep13`）。
- Manual Checks: MC-ARCH-001, MC-REUSE-001, MC-NAME-001, MC-VALID-001, MC-MODEL-001, MC-CONVERT-001, MC-LOG-001, MC-BEAN-001, MC-SCOPE-001, MC-TEST-001
- Literal Rules: Rule 1, Rule 2, Rule 3, Rule 4, Rule 11
- Ordered files:

#### File 1 — `MODIFY egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/test/java/top/egon/cola/component/tianshu/admin/rpc/provider/DdcAiResourceRpcProviderTest.java`

- Purpose: 三方法契约。
- Symbols: `getVersionHeadReturnsUnpublishedHeadWithoutVersion`、`getVersionHeadReturnsCurrentVersionHead`、`replaceSkillDraftRequiresAiWritePrincipalAndSkillKind`、`replaceSkillDraftMapsRevisionConflictToAbortedTrailer`、`publishSkillDraftUsesRequestIdAndReturnsReceipt`
- Repository evidence: 既有 `knownAiScopeFailureKeepsPermissionStatusAndTypedTrailer`。
- Dependencies and consumers: File 2–6。
- Why now: RED。
- Contract/signature changes: 无。
- Input/output and state mapping: `AiSkillDraftUpload(ns, key, id, expected_revision "3", files[...])`→`verify(resourceService).replaceDraft(idQuery(7L), argThat(c -> "3".equals(c.getExpectedRevision()) && c.getDraft().getNodes().size() == 2))`。
- Error and edge behavior: `CommonException(56303)`→`Status.ABORTED` + trailer `TIANSHU_AI_REVISION_CONFLICT`（沿既有 `mapAiFailure`）。
- Standards impact: MC-TEST-001。
- Literal rule enforcement: Rule 11。
- Implementation pseudocode:

```java
@Test void publishSkillDraftUsesRequestIdAndReturnsReceipt() {
    when(resourceService.publish(eq(idQuery(7L)), eq(KEY), any())).thenReturn(publicationResult(id 555L, targetVersionId 9L, targetVersionNo 3L, resultRevision 8L, contentSha256 SHA));
    AiPublicationReceipt receipt = withPrincipal(principal("AI_PUBLISH"), () -> provider.publishSkillDraft(AiSkillPublishRequest.newBuilder().setNamespaceCode("ns-a").setResourceKey("k").setResourceId("7").setExpectedRevision("7").setRequestId(KEY).setChangeNote("from authoring").build()));
    assertThat(receipt.getPublicationId()).isEqualTo("555"); assertThat(receipt.getTargetVersionNo()).isEqualTo(3L); assertThat(receipt.getResourceRevision()).isEqualTo("8");
}
@Test void replaceSkillDraftMapsRevisionConflictToAbortedTrailer() { when(resourceService.replaceDraft(any(), any())).thenThrow(new CommonException(56303, "TIANSHU_AI_REVISION_CONFLICT", "stale")); assertThatThrownBy(() -> withPrincipal(principal("AI_WRITE"), () -> provider.replaceSkillDraft(upload()))).extracting(STATUS_CODE).isEqualTo(Status.Code.ABORTED); }
```

- Verification contribution: RED→GREEN。
- After this file: RED。

#### File 2 — `CREATE egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/java/top/egon/cola/component/tianshu/admin/model/command/DdcAiSkillDraftUploadCommand.java`

- Purpose: RPC-005/006 命令（合并块：`DdcAiSkillDraftUploadCommand{namespaceCode, resourceKey, resourceId, expectedRevision, displayName, description, content(DdcAiContentDTO @ConvertGroup(Save))}`、`DdcAiSkillPublishCommand{namespaceCode, resourceKey, resourceId, expectedRevision, requestId, changeNote}`）。
- Symbols: 约束同 `DdcAiDraftCommand`/`DdcAiPublishCommand`。
- Repository evidence: `DdcAiDraftCommand:42 @ConvertGroup(from = Default.class, to = DdcAiValidationGroups.Save.class)`。
- Dependencies and consumers: Converter、Provider。
- Why now: Provider 输入。
- Contract/signature changes: 新类。
- Input/output and state mapping: Provider 将其转为既有 `DdcAiDraftCommand`/`DdcAiPublishCommand`（Converter File 4）。
- Error and edge behavior: `resourceId` 十进制正整数串。
- Standards impact: MC-NAME-001, MC-VALID-001, MC-MODEL-001。
- Literal rule enforcement: Rule 1、Rule 2、Rule 3、Rule 11。
- Implementation pseudocode:

```java
@Data @NoArgsConstructor @AllArgsConstructor @Accessors(chain = true) @Builder
public class DdcAiSkillDraftUploadCommand {
    @NotBlank @Size(max = 128) private String namespaceCode; @NotBlank @Size(max = 128) private String resourceKey; @NotBlank @Pattern(regexp = "^[1-9][0-9]{0,18}$") private String resourceId;
    @NotBlank @Pattern(regexp = "^[1-9][0-9]{0,18}$") private String expectedRevision; @NotBlank @Size(max = 128) private String displayName; @NotNull @Size(max = 512) private String description;
    @NotNull @Valid @ConvertGroup(from = Default.class, to = DdcAiValidationGroups.Save.class) private DdcAiContentDTO content;
}
```

- Verification contribution: File 1。
- After this file: 编译通过。

#### File 3 — `CREATE egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/java/top/egon/cola/component/tianshu/admin/model/vo/DdcAiVersionHeadResult.java`

- Purpose: 版本头/修订 Result（合并块：`DdcAiVersionHeadResult{resourceId, resourceKey, versionId, versionNo, contentSha256, resourceRevision, published}`、`DdcAiRevisionResult{resourceId, resourceRevision, draftSha256, draftMatchesCurrent}`）。
- Symbols: class + Lombok；ID 十进制字符串。
- Repository evidence: `DdcAiPublicationResult`。
- Dependencies and consumers: Service/Provider。
- Why now: Service 返回。
- Contract/signature changes: 新类。
- Input/output and state mapping: `published = currentVersionId != null`。
- Error and edge behavior: 无。
- Standards impact: MC-NAME-001, MC-MODEL-001。
- Literal rule enforcement: Rule 1、Rule 3、Rule 11。
- Implementation pseudocode:

```java
@Data @NoArgsConstructor @AllArgsConstructor @Accessors(chain = true) @Builder
public class DdcAiVersionHeadResult {
    @NotBlank private String resourceId; @NotBlank private String resourceKey; private String versionId; private Long versionNo; @Pattern(regexp = "^[a-f0-9]{64}$") private String contentSha256; @NotBlank private String resourceRevision; @NotNull private Boolean published;
}
```

- Verification contribution: File 1。
- After this file: 编译通过。

#### File 4 — `MODIFY egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/java/top/egon/cola/component/tianshu/admin/model/convertor/DdcAiSyncCommandConverter.java`

- Purpose: 追加 proto↔命令/Result 方法。
- Symbols: `DdcAiSkillDraftUploadCommand toUploadCommand(AiSkillDraftUpload)`（`AiNode`→`DdcAiNodeDTO` 复用既有 `DdcAiProtoConverter#nodeTypeFromProto` 等价映射，本 Mapper 自带 `@ValueMapping`）、`DdcAiDraftCommand toDraftCommand(DdcAiSkillDraftUploadCommand)`、`DdcAiSkillPublishCommand toPublishCommand(AiSkillPublishRequest)`、`DdcAiPublishCommand toPublish(DdcAiSkillPublishCommand)`、`AiVersionHead toProto(DdcAiVersionHeadResult)`、`AiRevisionResult toProto(DdcAiRevisionResult)`、`AiPublicationReceipt toProto(DdcAiPublicationResult, contentSha256)`
- Repository evidence: Step 6 File 5；`DdcAiProtoConverter` 节点映射。
- Dependencies and consumers: Provider。
- Why now: Provider 前置。
- Contract/signature changes: 追加方法。
- Input/output and state mapping: optional proto 字段 `has*()`。
- Error and edge behavior: 空 files→命令校验失败（`@ValidAiContent`）。
- Standards impact: MC-CONVERT-001。
- Literal rule enforcement: Rule 3、Rule 11。
- Implementation pseudocode:

```java
default DdcAiDraftCommand toDraftCommand(DdcAiSkillDraftUploadCommand c) { return DdcAiDraftCommand.builder().expectedRevision(c.getExpectedRevision()).displayName(c.getDisplayName()).description(c.getDescription()).draft(c.getContent()).build(); }
default AiVersionHead toProto(DdcAiVersionHeadResult r) { var b = AiVersionHead.newBuilder().setResourceId(r.getResourceId()).setResourceKey(r.getResourceKey()).setResourceRevision(r.getResourceRevision()).setPublished(r.getPublished());
    if (r.getVersionId() != null) b.setVersionId(r.getVersionId()); if (r.getVersionNo() != null) b.setVersionNo(r.getVersionNo()); if (r.getContentSha256() != null) b.setContentSha256(r.getContentSha256()); return b.build(); }
default AiPublicationReceipt toProto(DdcAiPublicationResult p, String contentSha256) { return AiPublicationReceipt.newBuilder().setPublicationId(p.getId()).setResourceId(p.getResourceId()).setTargetVersionId(p.getTargetVersionId()).setTargetVersionNo(Long.parseLong(p.getTargetVersionNo())).setResourceRevision(p.getResultRevision()).setContentSha256(contentSha256).build(); }
```

- Verification contribution: File 1。
- After this file: 编译通过。

#### File 5 — `MODIFY egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/java/top/egon/cola/component/tianshu/admin/service/ai/DdcAiResourceService.java`

- Purpose: PLAN-CLAR-002：追加只读 `getVersionHead` 与 `getDraftRevision`（合并块：接口 + `DdcAiResourceServiceImpl` 实现）。
- Symbols: `@Validated(Query.class) DdcAiVersionHeadResult getVersionHead(@Valid DdcAiResourceQuery selector)`（`ddcAiReadTransactionTemplate`：按 scope/key/id 解析资源 + `assertCanRead` + 当前版本头，`published=false` 时版本字段为 null）；`DdcAiRevisionResult draftRevision(DdcAiIdQuery)`（读草稿 hash 与当前版本 hash 比较，供 RPC-005 回执）
- Repository evidence: `getPublishedVersion(DdcAiResourceQuery, kind)` 的解析逻辑（Impl 第 ~200 行）可抽取共用 `resolveReadableResource`。
- Dependencies and consumers: Provider。
- Why now: RPC-004/005 回执。
- Contract/signature changes: 接口追加两方法；既有不变。
- Input/output and state mapping: `DdcAiResourceBO.currentVersionId/lastVersionNo/version` + `DdcAiVersionBO.contentSha256`。
- Error and edge behavior: 资源不存在→56302。
- Standards impact: MC-VALID-001, MC-SCOPE-001。
- Literal rule enforcement: Rule 2、Rule 11。
- Implementation pseudocode:

```java
@Override public DdcAiVersionHeadResult getVersionHead(DdcAiResourceQuery selector) {
    return readTransactionTemplate.execute(status -> {
        DdcAiResourceBO resource = resolveReadableResource(selector);                      // 与 getPublishedVersion 共用：scope→namespaceId→resourceKey/resourceId→assertCanRead
        if (resource.getCurrentVersionId() == null) return DdcAiVersionHeadResult.builder().resourceId(dec(resource.getId())).resourceKey(resource.getResourceKey()).resourceRevision(dec(resource.getVersion())).published(false).build();
        DdcAiVersionBO version = versionRepository.findActiveById(resource.getCurrentVersionId()).orElseThrow(DdcAiResourceServiceImpl::notFound);
        return DdcAiVersionHeadResult.builder().resourceId(dec(resource.getId())).resourceKey(resource.getResourceKey()).versionId(dec(version.getId())).versionNo(version.getVersionNo()).contentSha256(version.getContentSha256()).resourceRevision(dec(resource.getVersion())).published(true).build();
    });
}
```

- Verification contribution: File 1（Mockito Service）；`DdcAiVersionServiceTest` 追加 `getVersionHeadReflectsCurrentVersion`。
- After this file: 编译通过。

#### File 6 — `MODIFY egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/java/top/egon/cola/component/tianshu/admin/rpc/provider/DdcAiResourceRpcProvider.java`

- Purpose: 三方法实现替换占位。
- Symbols: `getVersionHead`（`requirePrincipalOperation("AI_READ")`→`resourceService.getVersionHead(selector(query))`→proto）；`replaceSkillDraft`（`AI_WRITE`；命令校验；`resourceService.get(idQuery)` 校验 kind==SKILL 与 namespace/key 一致否则 56301；`replaceDraft(idQuery, syncConverter.toDraftCommand(cmd))`→`resourceService.draftRevision(idQuery)`→proto）；`publishSkillDraft`（`AI_PUBLISH`；`publish(idQuery, requestId, toPublish(cmd))`→版本 hash（`getVersionHead`）→proto 回执）
- Repository evidence: `invoke`/`mapAiFailure`（56303→ABORTED 既有映射）。
- Dependencies and consumers: ai-starter authoring/workspace。
- Why now: 上行链路。
- Contract/signature changes: 无。
- Input/output and state mapping: scope 来自 `DdcServicePrincipal.current()`（biz/env/app）+ 请求 namespace/key/id。
- Error and edge behavior: 主体权限缺失→`PERMISSION_DENIED`；kind 非 SKILL→`INVALID_ARGUMENT` + `TIANSHU_AI_VALIDATION_ERROR`。
- Standards impact: MC-BEAN-001, MC-VALID-001, MC-LOG-001。
- Literal rule enforcement: Rule 4、Rule 2、Rule 11。
- Implementation pseudocode:

```java
@Override public AiRevisionResult replaceSkillDraft(@NotNull @Valid AiSkillDraftUpload upload) {
    return invoke("ReplaceSkillDraft", () -> {
        requirePrincipalOperation("AI_WRITE");
        DdcAiSkillDraftUploadCommand command = syncConverter.toUploadCommand(upload);
        DdcAiIdQuery idQuery = DdcAiIdQuery.builder().id(positiveLong(command.getResourceId())).build();
        DdcAiResourceResult current = resourceService.get(idQuery);
        if (!"SKILL".equals(current.getResource().getKind()) || !command.getResourceKey().equals(current.getResource().getResourceKey()) || !command.getNamespaceCode().equals(current.getResource().getNamespaceCode())) throw new CommonException(56301, "TIANSHU_AI_VALIDATION_ERROR", "upload target mismatch");
        resourceService.replaceDraft(idQuery, syncConverter.toDraftCommand(command));
        return syncConverter.toProto(resourceService.draftRevision(idQuery));
    });
}
```

- Verification contribution: File 1 全部用例。
- After this file: 本步 GREEN。

- Validation working directory: 仓库根
- Verification command: `./mvnw -pl egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin -Dtest=DdcAiResourceRpcProviderTest,DdcAiVersionServiceTest,DdcRpcServerInterceptorTest -Dsurefire.failIfNoSpecifiedTests=false test`
- Expected result: 三测试类通过；无 `UNIMPLEMENTED` 残留（`grep -n UNIMPLEMENTED` 于 Provider 为 0）。
- Failure returns to: File 6（Provider）、File 5（版本头）。
- Completion criteria: 三方法、权限、kind 校验、冲突映射均有断言。
- Rollback: 回退本步 Commit paths。
- Commit paths: egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/test/java/top/egon/cola/component/tianshu/admin/rpc/provider/DdcAiResourceRpcProviderTest.java, egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/java/top/egon/cola/component/tianshu/admin/model/command/DdcAiSkillDraftUploadCommand.java, egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/java/top/egon/cola/component/tianshu/admin/model/command/DdcAiSkillPublishCommand.java, egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/java/top/egon/cola/component/tianshu/admin/model/vo/DdcAiVersionHeadResult.java, egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/java/top/egon/cola/component/tianshu/admin/model/vo/DdcAiRevisionResult.java, egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/java/top/egon/cola/component/tianshu/admin/model/convertor/DdcAiSyncCommandConverter.java, egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/java/top/egon/cola/component/tianshu/admin/service/ai/DdcAiResourceService.java, egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/java/top/egon/cola/component/tianshu/admin/service/ai/impl/DdcAiResourceServiceImpl.java, egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/java/top/egon/cola/component/tianshu/admin/rpc/provider/DdcAiResourceRpcProvider.java, egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/test/java/top/egon/cola/component/tianshu/admin/service/ai/DdcAiVersionServiceTest.java
- Commit: `feat(tianshu): ai version head, skill draft upload and publish rpc`

### Step 14 — 新模块 egon-cola-tianshu-ai-starter：骨架、属性与使用上报

- Requirements: `REQ-002`, `REQ-010`, `REQ-014`
- Dependencies: Step 1、Step 2
- Baseline state: `egon-cola-tianshu/pom.xml` modules 四项；`egon-cola-xingyuan/pom.xml` dependencyManagement 第 116–120 行登记 `egon-cola-tianshu-http-registration-starter`；该 starter 的 pom 依赖（`egon-cola-tianshu-starter`、`egon-cola-component-rpc-tianshu-adapter`、`spring-boot-autoconfigure`、`spring-boot-configuration-processor`(optional)、`spring-boot-starter-test`(test)）、`META-INF/spring/...AutoConfiguration.imports` 一行、`@ConfigurationProperties(PREFIX)` 无 Lombok 的属性类、`@AutoConfiguration(after=...) @EnableConfigurationProperties @ConditionalOnProperty` 结构；base starter `DdcAiResourceAutoConfiguration` 以 `ddcAiPromptClientRequirement/ddcAiSkillClientRequirement` 在缺 Port 时启动失败。
- Observable outcome: 新模块编译并纳入 reactor；`DdcAiStarterAutoConfigurationTest` 证明 `feedback.enabled=true` 装配 `ddcAiUsageReporter`、缺 `DdcAiFeedbackClient` 失败、`prompt/skill` 均未启用时失败；`DefaultDdcAiUsageReporterTest` 证明入队/批量刷写/退避/队列满丢弃计数/回执同序（TEST-001 下游侧）。
- End state: 下游可上报；workspace/authoring 在 Step 15/16。
- Test-first gate: Required — 两测试类因模块/类缺失 RED（模块骨架 File 1–3 先于测试创建以使测试可编译，属于构建前置而非行为）。
- Manual Checks: MC-ARCH-001, MC-REUSE-001, MC-DEP-001, MC-NAME-001, MC-VALID-001, MC-MODEL-001, MC-LOG-001, MC-BEAN-001, MC-UTIL-001, MC-TIME-001, MC-CONFIG-001, MC-SCOPE-001, MC-TEST-001
- Literal Rules: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 7, Rule 10, Rule 11
- Ordered files:

#### File 1 — `CREATE egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-ai-starter/pom.xml`

- Purpose: 新模块坐标与依赖（无新第三方依赖）。
- Symbols: `<artifactId>egon-cola-tianshu-ai-starter</artifactId>`；parent `egon-cola-tianshu:5.4.1`；依赖 `egon-cola-tianshu-starter`、`egon-cola-component-rpc-tianshu-adapter`、`spring-boot-autoconfigure`、`spring-boot-configuration-processor`(optional)、`spring-boot-starter-test`(test)；`micrometer-core` 通过 base starter 传递（指标）
- Repository evidence: `egon-cola-tianshu-http-registration-starter/pom.xml`（去掉 `tianquan-shoubing-starter` 与 `spring-boot-actuator`）。
- Dependencies and consumers: reactor。
- Why now: 模块存在。
- Contract/signature changes: 新模块。
- Input/output and state mapping: 无。
- Error and edge behavior: 无。
- Standards impact: MC-DEP-001（零新增依赖）、MC-ARCH-001。
- Literal rule enforcement: Rule 11（复用组件，不引新库）。
- Implementation pseudocode:

```xml
<parent><groupId>top.egon</groupId><artifactId>egon-cola-tianshu</artifactId><version>5.4.1</version></parent>
<artifactId>egon-cola-tianshu-ai-starter</artifactId>
<dependencies>
  <dependency><groupId>top.egon</groupId><artifactId>egon-cola-tianshu-starter</artifactId></dependency>
  <dependency><groupId>top.egon</groupId><artifactId>egon-cola-component-rpc-tianshu-adapter</artifactId></dependency>
  <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-autoconfigure</artifactId></dependency>
  <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-configuration-processor</artifactId><optional>true</optional></dependency>
  <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-test</artifactId><scope>test</scope></dependency>
</dependencies>
```

- Verification contribution: `./mvnw -q -pl egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-ai-starter -am -DskipTests install`。
- After this file: 模块未登记。

#### File 2 — `MODIFY egon-cola-xingyuan/egon-cola-tianshu/pom.xml`

- Purpose: 登记模块（合并块：`egon-cola-xingyuan/egon-cola-tianshu/pom.xml` 的 modules 列表追加 `egon-cola-tianshu-ai-starter`；`egon-cola-xingyuan/pom.xml` dependencyManagement 追加同构条目）。
- Symbols: 两处 XML 追加。
- Repository evidence: 上文行号。
- Dependencies and consumers: reactor、下游 BOM。
- Why now: 编译纳入。
- Contract/signature changes: 构建。
- Input/output and state mapping: 无。
- Error and edge behavior: 无。
- Standards impact: MC-SCOPE-001。
- Literal rule enforcement: Rule 11。
- Implementation pseudocode:

```xml
<!-- egon-cola-xingyuan/egon-cola-tianshu/pom.xml：modules 列表在 egon-cola-tianshu-http-registration-starter 之后追加一个子元素，文本为 egon-cola-tianshu-ai-starter -->
<!-- 顺序：egon-cola-tianshu-starter, egon-cola-tianshu-http-registration-starter, egon-cola-tianshu-ai-starter, egon-cola-tianshu-admin, egon-cola-tianshu-test -->
<!-- egon-cola-xingyuan/pom.xml dependencyManagement（第 116–120 行之后）追加： -->
<dependency><groupId>top.egon</groupId><artifactId>egon-cola-tianshu-ai-starter</artifactId><version>${project.version}</version></dependency>
```

- Verification contribution: reactor 构建。
- After this file: 模块可构建。

#### File 3 — `CREATE egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-ai-starter/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`

- Purpose: 自动配置登记。
- Symbols: 一行 `top.egon.cola.component.tianshu.ai.autoconfigure.DdcAiStarterAutoConfiguration`
- Repository evidence: http-registration starter 同名文件。
- Dependencies and consumers: Spring Boot。
- Why now: 装配入口。
- Contract/signature changes: 无。
- Input/output and state mapping: 无。
- Error and edge behavior: 无。
- Standards impact: MC-CONFIG-001。
- Literal rule enforcement: Rule 11。
- Implementation pseudocode:

```text
# egon-cola-tianshu-ai-starter/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
# 一行一个自动配置类全名；本模块只登记一个：
top.egon.cola.component.tianshu.ai.autoconfigure.DdcAiStarterAutoConfiguration
```

- Verification contribution: File 9。
- After this file: 登记完成。

#### File 4 — `CREATE egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-ai-starter/src/test/java/top/egon/cola/component/tianshu/ai/autoconfigure/DdcAiStarterAutoConfigurationTest.java`

- Purpose: TEST-025（本步 feedback 部分；Step 15/16 追加用例）。
- Symbols: `feedbackDisabledByDefaultRegistersNothing`、`feedbackEnabledRegistersReporterWhenPromptEnabled`、`feedbackEnabledFailsWithoutFeedbackClient`、`feedbackEnabledFailsWhenNeitherPromptNorSkillEnabled`
- Repository evidence: `DdcHttpRegistrationAutoConfigurationTest`（`ApplicationContextRunner`）。
- Dependencies and consumers: File 5–9。
- Why now: RED。
- Contract/signature changes: 无。
- Input/output and state mapping: 属性组合→Bean/启动失败消息。
- Error and edge behavior: 失败消息含 `egon.cola.component.tianshu.ai.feedback.enabled requires`。
- Standards impact: MC-TEST-001, MC-CONFIG-001。
- Literal rule enforcement: Rule 7、Rule 11。
- Implementation pseudocode:

```java
@Test void feedbackEnabledRegistersReporterWhenPromptEnabled() {
    runner.withPropertyValues("egon.cola.component.tianshu.enabled=true", "egon.cola.component.tianshu.prompt.enabled=true", "egon.cola.component.tianshu.ai.feedback.enabled=true").withBean(DdcAiFeedbackClient.class, () -> mock(DdcAiFeedbackClient.class)).withBean(DdcPromptClient.class, () -> mock(DdcPromptClient.class))
        .run(ctx -> assertThat(ctx).hasBean("ddcAiUsageReporter").getBean(DdcAiUsageReporter.class).isInstanceOf(DefaultDdcAiUsageReporter.class));
}
@Test void feedbackEnabledFailsWhenNeitherPromptNorSkillEnabled() { runner.withPropertyValues("egon.cola.component.tianshu.ai.feedback.enabled=true").withBean(DdcAiFeedbackClient.class, () -> mock(DdcAiFeedbackClient.class)).run(ctx -> assertThat(ctx).hasFailed().getFailure().hasMessageContaining("requires egon.cola.component.tianshu.prompt.enabled or egon.cola.component.tianshu.skill.enabled")); }
```

- Verification contribution: RED→GREEN。
- After this file: RED。

#### File 5 — `CREATE egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-ai-starter/src/test/java/top/egon/cola/component/tianshu/ai/feedback/DefaultDdcAiUsageReporterTest.java`

- Purpose: TEST-001 下游侧。
- Symbols: `reportEnqueuesAndFlushesInBatchesPreservingOrder`、`fullQueueDropsAndCounts`、`transportFailureRetriesWithBackoffUpToMaxAttempts`、`rejectedItemsAreNotRetried`、`closeFlushesRemaining`
- Repository evidence: 假 `DdcAiFeedbackClient`（记录批次）。
- Dependencies and consumers: File 7/8。
- Why now: RED。
- Contract/signature changes: 无。
- Input/output and state mapping: `queueCapacity=3, batchSize=2, flushInterval=10ms, maxAttempts=3`。
- Error and edge behavior: 第 4 条入队返回 false 且 `dropped` 计数 1；`DdcClientTransportException` 重试 3 次后丢弃并计数。
- Standards impact: MC-TEST-001。
- Literal rule enforcement: Rule 11。
- Implementation pseudocode:

```java
@Test void transportFailureRetriesWithBackoffUpToMaxAttempts() {
    FakeFeedbackClient client = new FakeFeedbackClient(); client.failNext(3, new DdcClientTransportException("UNAVAILABLE"));
    var reporter = new DefaultDdcAiUsageReporter(client, props(capacity 100, batch 10, flush 5ms, maxAttempts 3), meters, Clock.fixed(...), manualScheduler);
    assertThat(reporter.report(sample("u1"))).isTrue(); manualScheduler.runAll();
    assertThat(client.attempts()).isEqualTo(3); assertThat(meters.counter("tianshu.ai.feedback.dropped", "reason", "TRANSPORT").count()).isEqualTo(1.0);
}
@Test void fullQueueDropsAndCounts() { var reporter = new DefaultDdcAiUsageReporter(client, props(capacity 1, ...), meters, clock, manualScheduler); assertThat(reporter.report(sample("u1"))).isTrue(); assertThat(reporter.report(sample("u2"))).isFalse(); assertThat(meters.counter("tianshu.ai.feedback.dropped", "reason", "QUEUE_FULL").count()).isEqualTo(1.0); }
```

- Verification contribution: RED→GREEN。
- After this file: RED。

#### File 6 — `CREATE egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-ai-starter/src/main/java/top/egon/cola/component/tianshu/ai/properties/DdcAiStarterProperties.java`

- Purpose: 五个属性类（合并块：`DdcAiStarterProperties`（prefix `egon.cola.component.tianshu.ai`，含 `feedback/skill.workspace/skill.authoring` 三子节）、`DdcAiFeedbackProperties{enabled=false, queueCapacity=10000, batchSize=100, flushInterval=2s, maxAttempts=3}`、`DdcAiWorkspaceProperties{enabled=false, root(java.nio.file.Path), subscriptionEnabled=false, reconcileInterval=60s, resources(List<DdcAiWatchedResourceDTO>)}`、`DdcAiAuthoringProperties{enabled=false, root, pollInterval=5s, debounce=2s, resources(List<DdcAiWatchedResourceDTO>)}`、`DdcAiWatchedResourceDTO{namespaceCode, resourceKey, resourceId, pinnedVersionNo, directory, autoPublish=false, changeNote}`）。
- Symbols: `@ConfigurationProperties("egon.cola.component.tianshu.ai") @Validated @Data ...`；Jakarta 约束（`@Min/@Max`、`@NotNull` on Path when enabled 由自动配置校验）。
- Repository evidence: `DdcHttpRegistrationProperties`（无 Lombok，但 Rule 3 要求 POJO Lombok，本模块新建类遵循 Rule 3；`lombok.config` 在 `egon-cola-tianshu/` 父目录对子模块生效）。
- Dependencies and consumers: 自动配置、Reporter、Workspace、Authoring。
- Why now: 配置绑定。
- Contract/signature changes: 新类。
- Input/output and state mapping: 键按主 Spec §15.4。
- Error and edge behavior: `queueCapacity 1..1_000_000`、`batchSize 1..100`、`maxAttempts 1..10`。
- Standards impact: MC-CONFIG-001, MC-MODEL-001, MC-VALID-001, MC-TIME-001。
- Literal rule enforcement: Rule 3、Rule 2、Rule 7、Rule 10、Rule 11。
- Implementation pseudocode:

```java
@ConfigurationProperties(DdcAiStarterProperties.PREFIX) @Validated
@Data @NoArgsConstructor @AllArgsConstructor @Accessors(chain = true) @Builder
public class DdcAiStarterProperties {
    public static final String PREFIX = "egon.cola.component.tianshu.ai";
    @NotNull @Valid private DdcAiFeedbackProperties feedback = new DdcAiFeedbackProperties(); @NotNull @Valid private Skill skill = new Skill();
    @Data @NoArgsConstructor @AllArgsConstructor @Accessors(chain = true) @Builder public static class Skill { @NotNull @Valid private DdcAiWorkspaceProperties workspace = new DdcAiWorkspaceProperties(); @NotNull @Valid private DdcAiAuthoringProperties authoring = new DdcAiAuthoringProperties(); }
}
@Data @NoArgsConstructor @AllArgsConstructor @Accessors(chain = true) @Builder
public class DdcAiFeedbackProperties { private boolean enabled; @Min(1) @Max(1_000_000) private int queueCapacity = 10_000; @Min(1) @Max(100) private int batchSize = 100; @NotNull private Duration flushInterval = Duration.ofSeconds(2); @Min(1) @Max(10) private int maxAttempts = 3; }
```

- Verification contribution: File 4。
- After this file: 编译通过。

#### File 7 — `CREATE egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-ai-starter/src/main/java/top/egon/cola/component/tianshu/ai/feedback/DdcAiUsageReporter.java`

- Purpose: 下游上报 SPI（非阻塞）。
- Symbols: `public interface DdcAiUsageReporter { boolean report(@NotNull @Valid DdcAiUsageReportDTO report); }`；静态工厂辅助 `DdcAiUsageReportDTO.success(...)`/`failure(...)` 不在此（保持 DTO 纯载体）
- Repository evidence: SDK Port 风格（`DdcPromptClient`）。
- Dependencies and consumers: 下游业务代码。
- Why now: 公共 API。
- Contract/signature changes: 新接口。
- Input/output and state mapping: 返回是否入队。
- Error and edge behavior: 永不抛出（校验失败返回 false 并计数 `INVALID`）。
- Standards impact: MC-NAME-001, MC-VALID-001。
- Literal rule enforcement: Rule 2、Rule 11。
- Implementation pseudocode:

```java
@Validated
public interface DdcAiUsageReporter {
    /** 非阻塞入队；队列满或校验失败返回 false 并计数，不抛出。 */
    boolean report(@NotNull @Valid DdcAiUsageReportDTO report);
}
```

- Verification contribution: File 5。
- After this file: 编译通过。

#### File 8 — `CREATE egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-ai-starter/src/main/java/top/egon/cola/component/tianshu/ai/feedback/DefaultDdcAiUsageReporter.java`

- Purpose: 有界队列 + 定时批量刷写 + 退避。
- Symbols: `public class DefaultDdcAiUsageReporter implements DdcAiUsageReporter, AutoCloseable`；构造 `(DdcAiFeedbackClient client, DdcAiFeedbackProperties props, MeterRegistry meters, Clock clock, ScheduledExecutorService scheduler)`；`ArrayBlockingQueue<DdcAiUsageReportDTO>`；`flush()`；`sendWithRetry(List)`；指标 `tianshu.ai.feedback.reported{status}`、`tianshu.ai.feedback.dropped{reason}`
- Repository evidence: JDK 并发工具；`DdcClientTransportException`（adapter 既有）。
- Dependencies and consumers: 自动配置。
- Why now: 上报实现。
- Contract/signature changes: 新类。
- Input/output and state mapping: 批 ≤100→`client.report(new DdcAiUsageReportBatchDTO(batch))`→按 `status` 计数；`REJECTED` 不重试。
- Error and edge behavior: `DdcClientTransportException`→退避 `min(2^attempt * 200ms, 5s)` 后重试，达 `maxAttempts` 丢弃并计数 `TRANSPORT`；其他 RuntimeException→丢弃计数 `ERROR`；`close()` 执行最后一次 flush（最多 5s）。
- Standards impact: MC-LOG-001, MC-UTIL-001, MC-TIME-001。
- Literal rule enforcement: Rule 4（`@Slf4j`）、Rule 5、Rule 10、Rule 11。
- Implementation pseudocode:

```java
@Override public boolean report(DdcAiUsageReportDTO report) {
    if (closed.get()) { drop("CLOSED"); return false; }
    if (!queue.offer(report)) { drop("QUEUE_FULL"); return false; }
    return true;
}
void flush() {
    List<DdcAiUsageReportDTO> batch = new ArrayList<>(props.getBatchSize()); queue.drainTo(batch, props.getBatchSize()); if (batch.isEmpty()) return;
    for (int attempt = 1; ; attempt++) {
        try { DdcAiUsageReportReceiptDTO receipt = client.report(new DdcAiUsageReportBatchDTO(batch)); receipt.getItems().forEach(i -> meters.counter("tianshu.ai.feedback.reported", "status", i.getStatus().name()).increment()); return; }
        catch (DdcClientTransportException transport) { if (attempt >= props.getMaxAttempts()) { drop("TRANSPORT", batch.size()); return; } sleep(Duration.ofMillis(Math.min(200L << attempt, 5_000L))); }
        catch (RuntimeException other) { log.warn("AI usage report batch dropped: {}", other.toString()); drop("ERROR", batch.size()); return; }
    }
}
```

- Verification contribution: File 5。
- After this file: File 5 GREEN。

#### File 9 — `CREATE egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-ai-starter/src/main/java/top/egon/cola/component/tianshu/ai/autoconfigure/DdcAiStarterAutoConfiguration.java`

- Purpose: 自动配置（本步 feedback；Step 15/16 追加 workspace/authoring Bean）。
- Symbols: `@AutoConfiguration(after = {DdcRpcAutoConfiguration.class, DdcAiResourceAutoConfiguration.class}) @EnableConfigurationProperties({DdcAiStarterProperties.class, DdcProperties.class, DdcTenantProperties.class})`；`@Bean(name = "ddcAiUsageReporter", destroyMethod = "close") @ConditionalOnProperty(prefix = "egon.cola.component.tianshu.ai.feedback", name = "enabled", havingValue = "true") @ConditionalOnMissingBean(DdcAiUsageReporter.class)`；`@Bean("ddcAiFeedbackScheduler", destroyMethod = "shutdownNow")`；启动校验 `requireReadPortEnabled(Environment)`
- Repository evidence: `DdcHttpRegistrationAutoConfiguration`。
- Dependencies and consumers: 下游应用。
- Why now: 装配。
- Contract/signature changes: 新类。
- Input/output and state mapping: `feedback.enabled` 且 `prompt.enabled || skill.enabled` 为假→`IllegalStateException("egon.cola.component.tianshu.ai.feedback.enabled requires egon.cola.component.tianshu.prompt.enabled or egon.cola.component.tianshu.skill.enabled")`；缺 `DdcAiFeedbackClient`→`ObjectProvider.getIfAvailable()==null`→`IllegalStateException("... requires a DdcAiFeedbackClient adapter")`。
- Error and edge behavior: `MeterRegistry` 缺失时用 `SimpleMeterRegistry`（`ObjectProvider`）。
- Standards impact: MC-BEAN-001, MC-CONFIG-001。
- Literal rule enforcement: Rule 4、Rule 7、Rule 11。
- Implementation pseudocode:

```java
@Bean(name = "ddcAiUsageReporter", destroyMethod = "close") @ConditionalOnProperty(prefix = "egon.cola.component.tianshu.ai.feedback", name = "enabled", havingValue = "true") @ConditionalOnMissingBean(DdcAiUsageReporter.class)
public DdcAiUsageReporter ddcAiUsageReporter(Environment env, ObjectProvider<DdcAiFeedbackClient> client, DdcAiStarterProperties properties, ObjectProvider<MeterRegistry> meters, @Qualifier("ddcAiFeedbackScheduler") ScheduledExecutorService scheduler) {
    boolean prompt = env.getProperty("egon.cola.component.tianshu.prompt.enabled", Boolean.class, false), skill = env.getProperty("egon.cola.component.tianshu.skill.enabled", Boolean.class, false);
    if (!prompt && !skill) throw new IllegalStateException("egon.cola.component.tianshu.ai.feedback.enabled requires egon.cola.component.tianshu.prompt.enabled or egon.cola.component.tianshu.skill.enabled");
    DdcAiFeedbackClient port = client.getIfAvailable(); if (port == null) throw new IllegalStateException("egon.cola.component.tianshu.ai.feedback.enabled requires a DdcAiFeedbackClient adapter");
    return new DefaultDdcAiUsageReporter(port, properties.getFeedback(), meters.getIfAvailable(SimpleMeterRegistry::new), Clock.systemUTC(), scheduler);
}
```

- Verification contribution: File 4。
- After this file: 本步 GREEN。

- Validation working directory: 仓库根
- Verification command: `./mvnw -pl egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-ai-starter -am -DskipTests -q install && ./mvnw -pl egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-ai-starter -Dtest=DdcAiStarterAutoConfigurationTest,DefaultDdcAiUsageReporterTest test`
- Expected result: 模块构建；两测试类通过；`spring-configuration-metadata.json` 含 `egon.cola.component.tianshu.ai.feedback.enabled`。
- Failure returns to: File 9（条件）、File 8（队列）。
- Completion criteria: 开关矩阵、队列/退避/回执计数均有断言。
- Rollback: 回退本步 Commit paths（删除模块目录与两处 pom 登记）。
- Commit paths: egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-ai-starter/pom.xml, egon-cola-xingyuan/egon-cola-tianshu/pom.xml, egon-cola-xingyuan/pom.xml, egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-ai-starter/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports, egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-ai-starter/src/test/java/top/egon/cola/component/tianshu/ai/autoconfigure/DdcAiStarterAutoConfigurationTest.java, egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-ai-starter/src/test/java/top/egon/cola/component/tianshu/ai/feedback/DefaultDdcAiUsageReporterTest.java, egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-ai-starter/src/main/java/top/egon/cola/component/tianshu/ai/properties/DdcAiStarterProperties.java, egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-ai-starter/src/main/java/top/egon/cola/component/tianshu/ai/properties/DdcAiFeedbackProperties.java, egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-ai-starter/src/main/java/top/egon/cola/component/tianshu/ai/properties/DdcAiWorkspaceProperties.java, egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-ai-starter/src/main/java/top/egon/cola/component/tianshu/ai/properties/DdcAiAuthoringProperties.java, egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-ai-starter/src/main/java/top/egon/cola/component/tianshu/ai/properties/DdcAiWatchedResourceDTO.java, egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-ai-starter/src/main/java/top/egon/cola/component/tianshu/ai/feedback/DdcAiUsageReporter.java, egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-ai-starter/src/main/java/top/egon/cola/component/tianshu/ai/feedback/DefaultDdcAiUsageReporter.java, egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-ai-starter/src/main/java/top/egon/cola/component/tianshu/ai/autoconfigure/DdcAiStarterAutoConfiguration.java
- Commit: `feat(tianshu): add egon-cola-tianshu-ai-starter with usage reporter`

### Step 15 — 下行：Skill 工作区落盘、Redis 订阅与对账

- Requirements: `REQ-006`, `REQ-007`, `REQ-010`, `REQ-011`
- Dependencies: Step 14、Step 1（`aiTopic`、事件）、Step 2（`DdcAiSyncClient`）
- Baseline state: `DdcRedisAutoConfiguration#ddcRedissonClient` 条件 `(tianshu.enabled && redis.enabled) || registry.enabled`；`DdcRedisTopicSubscription(List<RTopic>, Class<T>, MessageListener<T>)`；`DdcConfigChangeListener#onMessage` 用 `tenantProperties.requireTenantId()` + `envelope.belongsToTenant`；`DdcSkillClient#get(DdcAiResourceQuery)` 返回 `DdcSkillResult{resourceId, resourceKey, versionId, versionNo, contentSha256, files}`。
- Observable outcome: `DdcAiSkillMaterializerTest`（TEST-024）原子落盘/失败保留旧 current/路径穿越拒绝；`DdcAiWorkspaceReconcileSchedulerTest`（TEST-023）；`DdcAiPublicationListenerTest`（TEST-022 下游侧：租户不匹配丢弃、幂等）；`DdcRedisAutoConfigurationTest` 新用例证明 `ai.skill.workspace.subscription-enabled=true` 单独可装配 Redisson（TEST-025）。
- End state: 下游工作区自动刷新。
- Test-first gate: Required — 四个测试（含既有类新用例）RED。
- Manual Checks: MC-ARCH-001, MC-REUSE-001, MC-NAME-001, MC-VALID-001, MC-MODEL-001, MC-LOG-001, MC-BEAN-001, MC-UTIL-001, MC-JSON-001, MC-TIME-001, MC-CONFIG-001, MC-PATTERN-001, MC-SCOPE-001, MC-TEST-001
- Literal Rules: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 10, Rule 11
- Ordered files:

#### File 1 — `CREATE egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-ai-starter/src/test/java/top/egon/cola/component/tianshu/ai/workspace/DdcAiSkillMaterializerTest.java`

- Purpose: 三个 workspace 测试（合并块：`DdcAiSkillMaterializerTest`、`DdcAiWorkspaceReconcileSchedulerTest`、`DdcAiPublicationListenerTest`）+ `DdcAiStarterAutoConfigurationTest` 追加 `workspaceEnabledRegistersWorkspaceServiceAndScheduler`、`subscriptionEnabledRegistersListenerWithTenantTopic`。
- Symbols: materializer：`writesVersionDirectoryAndAtomicallySwitchesCurrentSymlinkOrMarker`、`failureKeepsPreviousCurrent`、`rejectsPathTraversalAndAbsolutePaths`、`stateFileRecordsVersionNoAndSha`；scheduler：`pullsWhenHeadDiffers`、`noopWhenHeadMatches`、`pinnedVersionNeverSwitches`；listener：`ignoresEnvelopeOfOtherTenant`、`triggersPullByVersionNoOnce`
- Repository evidence: JUnit `@TempDir`；假 `DdcAiSyncClient`/`DdcSkillClient`。
- Dependencies and consumers: File 2–7。
- Why now: RED。
- Contract/signature changes: 无。
- Input/output and state mapping: `root/{namespaceCode}/{resourceKey}/versions/v{versionNo}/...` + `current` 标记文件（`current` 内容为 `v{versionNo}`，PLAN-CLAR-013：不用符号链接以兼容 Windows/容器只读层）+ `state.json`。
- Error and edge behavior: 写入中途异常→`versions/v3.tmp-*` 清理、`current` 仍指向 `v2`。
- Standards impact: MC-TEST-001。
- Literal rule enforcement: Rule 11。
- Implementation pseudocode:

```java
@Test void failureKeepsPreviousCurrent(@TempDir Path root) throws IOException {
    materializer.materialize(root, "ns-a", "skill-x", skill(2L, files("SKILL.md", "# v2"))); assertThat(Files.readString(root.resolve("ns-a/skill-x/current"))).isEqualTo("v2");
    assertThatThrownBy(() -> materializer.materialize(root, "ns-a", "skill-x", skill(3L, files("../evil", "x")))).isInstanceOf(IllegalArgumentException.class);
    assertThat(Files.readString(root.resolve("ns-a/skill-x/current"))).isEqualTo("v2"); assertThat(root.resolve("ns-a/skill-x/versions/v3")).doesNotExist();
}
@Test void pinnedVersionNeverSwitches() { when(syncClient.getVersionHead(any())).thenReturn(head(versionNo 9L)); scheduler.reconcileOnce(); verify(workspaceService, never()).pull(any(), anyLong()); }
```

- Verification contribution: RED→GREEN。
- After this file: RED。

#### File 2 — `CREATE egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-ai-starter/src/main/java/top/egon/cola/component/tianshu/ai/workspace/DdcAiSkillWorkspaceStateDTO.java`

- Purpose: 工作区状态值对象（`state.json`）。
- Symbols: `record DdcAiSkillWorkspaceStateDTO(@NotBlank String namespaceCode, @NotBlank String resourceKey, @NotNull Long resourceId, Long versionNo, String contentSha256, @NotNull Instant updatedAt)`；Jackson 读写
- Repository evidence: record 值对象；Jackson `jsr310`（base starter 已带）。
- Dependencies and consumers: Materializer、Scheduler。
- Why now: 状态契约。
- Contract/signature changes: 新 record。
- Input/output and state mapping: JSON 文件。
- Error and edge behavior: 损坏文件→视为无状态并 WARN。
- Standards impact: MC-MODEL-001, MC-JSON-001, MC-TIME-001。
- Literal rule enforcement: Rule 3、Rule 6、Rule 10、Rule 11。
- Implementation pseudocode:

```java
public record DdcAiSkillWorkspaceStateDTO(String namespaceCode, String resourceKey, Long resourceId, Long versionNo, String contentSha256, Instant updatedAt) {
    public DdcAiSkillWorkspaceStateDTO { Objects.requireNonNull(namespaceCode); Objects.requireNonNull(resourceKey); Objects.requireNonNull(resourceId); Objects.requireNonNull(updatedAt); }
    public boolean matches(DdcAiVersionHeadDTO head) { return head.getVersionNo() != null && head.getVersionNo().equals(versionNo) && Objects.equals(head.getContentSha256(), contentSha256); }
}
```

- Verification contribution: File 1。
- After this file: 编译通过。

#### File 3 — `CREATE egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-ai-starter/src/main/java/top/egon/cola/component/tianshu/ai/workspace/DdcAiSkillMaterializer.java`

- Purpose: 原子落盘。
- Symbols: `public class DdcAiSkillMaterializer`；`Path materialize(Path root, String namespaceCode, String resourceKey, DdcSkillResult skill)`；`static Path safeResolve(Path base, String relative)`
- Repository evidence: JDK NIO（`Files.createTempDirectory`、`Files.move(ATOMIC_MOVE)`、`Files.writeString`）。
- Dependencies and consumers: WorkspaceService。
- Why now: 核心落盘。
- Contract/signature changes: 新类。
- Input/output and state mapping: 临时目录 `versions/.tmp-v{n}-{uuid}`→写全部 FILE 节点（DIRECTORY 节点 `createDirectories`）→`fsync`→`move` 到 `versions/v{n}`（已存在则校验 sha 后复用）→写 `current.tmp`→`move(ATOMIC_MOVE, REPLACE_EXISTING)` 到 `current`→写 `state.json`。
- Error and edge behavior: 路径含 `..`/绝对/`\\`/控制字符→`IllegalArgumentException`；`safeResolve` 检查 `normalized.startsWith(base)`；任何异常删除临时目录。
- Standards impact: MC-LOG-001, MC-UTIL-001。
- Literal rule enforcement: Rule 4、Rule 5、Rule 11。
- Implementation pseudocode:

```java
public Path materialize(Path root, String ns, String key, DdcSkillResult skill) throws IOException {
    Path resourceDir = safeResolve(safeResolve(root, ns), key); Path versions = resourceDir.resolve("versions"); Files.createDirectories(versions);
    Path target = versions.resolve("v" + skill.getVersionNo()); Path tmp = Files.createTempDirectory(versions, ".tmp-v" + skill.getVersionNo() + "-");
    try { for (DdcAiNodeDTO node : skill.getFiles()) { Path p = safeResolve(tmp, node.getPath()); if (node.getNodeType() == DIRECTORY) Files.createDirectories(p); else { Files.createDirectories(p.getParent()); Files.writeString(p, node.getContent(), UTF_8, CREATE_NEW); } }
          if (Files.exists(target)) deleteRecursively(tmp); else Files.move(tmp, target, ATOMIC_MOVE);
          Path currentTmp = resourceDir.resolve("current.tmp"); Files.writeString(currentTmp, "v" + skill.getVersionNo(), UTF_8); Files.move(currentTmp, resourceDir.resolve("current"), ATOMIC_MOVE, REPLACE_EXISTING);
          writeState(resourceDir, new DdcAiSkillWorkspaceStateDTO(ns, key, skill.getResourceId(), skill.getVersionNo(), skill.getContentSha256(), clock.instant())); return target; }
    catch (IOException | RuntimeException failure) { deleteRecursively(tmp); throw failure; }
}
```

- Verification contribution: File 1。
- After this file: materializer 用例 GREEN。

#### File 4 — `CREATE egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-ai-starter/src/main/java/top/egon/cola/component/tianshu/ai/workspace/DdcAiSkillWorkspace.java`

- Purpose: 下游读取当前落盘路径的公共 API 与服务（合并块：`DdcAiSkillWorkspace` 接口 `Optional<java.nio.file.Path> currentDirectory(String namespaceCode, String resourceKey)`、`Optional<DdcAiSkillWorkspaceStateDTO> state(...)`；`DdcAiSkillWorkspaceService implements DdcAiSkillWorkspace`：`pull(DdcAiWatchedResourceDTO, Long versionNo)`、`pullCurrent(resource)`、`reconcile(resource)`）。
- Symbols: `DdcAiSkillWorkspaceService(DdcSkillClient skillClient, DdcAiSyncClient syncClient, DdcAiSkillMaterializer materializer, DdcAiWorkspaceProperties props, MeterRegistry meters)`；每资源 `ReentrantLock`
- Repository evidence: `DdcSkillClient#get(DdcAiResourceQuery(namespaceCode, resourceKey, versionNo, resourceId))`。
- Dependencies and consumers: Listener、Scheduler、下游代码。
- Why now: 拉取编排。
- Contract/signature changes: 新接口/类。
- Input/output and state mapping: `pull(resource, versionNo)`：`skillClient.get(query(versionNo))`→`materializer.materialize`；`reconcile`：`pinnedVersionNo != null`→`pull(pinned)` 一次后不再切换；否则 `syncClient.getVersionHead(query(null))`→`published && !state.matches(head)`→`pull(head.versionNo)`。
- Error and edge behavior: `DdcClientTransportException`→WARN + 指标 `tianshu.ai.workspace.pull{result}`，下周期重试；`UNIMPLEMENTED`（服务端未升级）→WARN 一次并禁用对账直到重启（§9 兼容规则）。
- Standards impact: MC-LOG-001, MC-PATTERN-001（Simple，直接实现）。
- Literal rule enforcement: Rule 4、Rule 11。
- Implementation pseudocode:

```java
public void reconcile(DdcAiWatchedResourceDTO r) {
    lockFor(r).lock();
    try { Optional<DdcAiSkillWorkspaceStateDTO> state = state(r.getNamespaceCode(), r.getResourceKey());
          if (r.getPinnedVersionNo() != null) { if (state.map(s -> r.getPinnedVersionNo().equals(s.versionNo())).orElse(false)) return; pull(r, r.getPinnedVersionNo()); return; }
          DdcAiVersionHeadDTO head = syncClient.getVersionHead(new DdcAiResourceQuery(r.getNamespaceCode(), r.getResourceKey(), null, r.getResourceId()));
          if (!Boolean.TRUE.equals(head.getPublished())) return; if (state.map(s -> s.matches(head)).orElse(false)) return; pull(r, head.getVersionNo()); }
    catch (DdcClientTransportException transport) { log.warn("AI workspace reconcile deferred: {} {}", r.getResourceKey(), transport.toString()); meters.counter("tianshu.ai.workspace.pull", "result", "TRANSPORT").increment(); }
    finally { lockFor(r).unlock(); }
}
```

- Verification contribution: File 1 scheduler/listener 用例。
- After this file: 编译通过。

#### File 5 — `CREATE egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-ai-starter/src/main/java/top/egon/cola/component/tianshu/ai/workspace/DdcAiPublicationListener.java`

- Purpose: 订阅 `aiTopic` 的 StringCodec 消息，解析信封并触发按版本号拉取。
- Symbols: `public class DdcAiPublicationListener implements MessageListener<String>`；构造 `(ObjectMapper, DdcTenantProperties, DdcAiWorkspaceService, Map<String, DdcAiWatchedResourceDTO> watched, ExecutorService)`；`onMessage(CharSequence channel, String json)`
- Repository evidence: `DdcConfigChangeListener#onMessage`（`requireTenantId`、`belongsToTenant`、`runWithTenant`）；Admin 投递用 `StringCodec.INSTANCE.publish(payload)`。
- Dependencies and consumers: 自动配置订阅。
- Why now: 秒级刷新。
- Contract/signature changes: 新类。
- Input/output and state mapping: JSON→`DdcTenantMessageDTO<DdcAiPublicationEvent>`→`tenantId` 匹配 + `kind==SKILL` + `(namespaceCode, resourceKey)` 在 watched→`executor.submit(() -> workspace.pull(resource, event.getVersionNo()))`（已是该版本则跳过）。
- Error and edge behavior: 解析失败→WARN 丢弃；非本租户→DEBUG 丢弃；`operation` 任意（PUBLISH/ROLLBACK/PROMOTE 均按 versionNo 拉取）。
- Standards impact: MC-LOG-001, MC-JSON-001。
- Literal rule enforcement: Rule 4、Rule 6、Rule 11。
- Implementation pseudocode:

```java
@Override public void onMessage(CharSequence channel, String json) {
    DdcTenantMessageDTO<DdcAiPublicationEvent> envelope;
    try { envelope = objectMapper.readValue(json, ENVELOPE_TYPE); } catch (JsonProcessingException bad) { log.warn("Ignoring malformed ai publication message on {}", channel); return; }
    long tenantId = tenantProperties.requireTenantId(); if (envelope == null || !envelope.belongsToTenant(tenantId) || envelope.getPayload() == null) { log.debug("Ignoring ai publication for another tenant"); return; }
    DdcAiPublicationEvent event = envelope.getPayload(); if (!"SKILL".equals(event.getKind())) return;
    DdcAiWatchedResourceDTO watched = this.watched.get(event.getNamespaceCode() + "/" + event.getResourceKey()); if (watched == null || watched.getPinnedVersionNo() != null) return;
    executor.submit(() -> { try { workspace.pull(watched, event.getVersionNo()); } catch (RuntimeException f) { log.warn("AI workspace pull after event failed: {}", event.getEventId(), f); } });
}
```

- Verification contribution: File 1 listener 用例。
- After this file: 编译通过。

#### File 6 — `CREATE egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-ai-starter/src/main/java/top/egon/cola/component/tianshu/ai/workspace/DdcAiWorkspaceReconcileScheduler.java`

- Purpose: 周期对账兜底。
- Symbols: `public class DdcAiWorkspaceReconcileScheduler`；`@Scheduled(fixedDelayString = "${egon.cola.component.tianshu.ai.skill.workspace.reconcile-interval:60s}")`（PLAN-CLAR-014：下游应用需 `@EnableScheduling`；若无 `TaskScheduler`，自动配置退化为 `ScheduledExecutorService` 自行调度——以自管 `ScheduledExecutorService` 为准，避免依赖宿主调度开关）；`reconcileOnce()`
- Repository evidence: `DdcHttpRegistrationAutoConfiguration` 的心跳线程自管模式。
- Dependencies and consumers: 自动配置。
- Why now: 丢事件兜底。
- Contract/signature changes: 新类。
- Input/output and state mapping: 遍历 `props.getResources()`→`workspace.reconcile(r)`。
- Error and edge behavior: 单资源异常不影响其他。
- Standards impact: MC-LOG-001, MC-TIME-001。
- Literal rule enforcement: Rule 4、Rule 10、Rule 11。
- Implementation pseudocode:

```java
public void start() { scheduler.scheduleWithFixedDelay(this::reconcileOnce, Duration.ZERO.toMillis(), props.getReconcileInterval().toMillis(), TimeUnit.MILLISECONDS); }
public int reconcileOnce() {
    int ok = 0;
    for (DdcAiWatchedResourceDTO r : props.getResources()) { try { workspace.reconcile(r); ok++; } catch (RuntimeException f) { log.warn("AI workspace reconcile failed for {}/{}", r.getNamespaceCode(), r.getResourceKey(), f); } }
    return ok;
}
```

- Verification contribution: File 1 scheduler 用例。
- After this file: 编译通过。

#### File 7 — `MODIFY egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-starter/src/main/java/top/egon/cola/component/tianshu/autoconfigure/DdcRedisAutoConfiguration.java`

- Purpose: Redisson 条件追加 `|| ${egon.cola.component.tianshu.ai.skill.workspace.subscription-enabled:false}`（合并块：含 `egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-starter/src/test/java/top/egon/cola/component/tianshu/autoconfigure/DdcRedisAutoConfigurationTest.java` 新用例 `createsClientWhenOnlyAiWorkspaceSubscriptionEnabled`）。
- Symbols: `@ConditionalOnExpression` 字符串追加一个 OR 分支。
- Repository evidence: 第 31–35 行。
- Dependencies and consumers: ai-starter 订阅。
- Why now: 仅订阅不拉配置的下游也能拿到 Redisson。
- Contract/signature changes: 条件扩展（默认 false，不影响既有）。
- Input/output and state mapping: 无。
- Error and edge behavior: 既有三组真值不变（RISK-207）。
- Standards impact: MC-CONFIG-001, MC-SCOPE-001。
- Literal rule enforcement: Rule 7、Rule 11。
- Implementation pseudocode:

```java
@ConditionalOnExpression(
        "(${egon.cola.component.tianshu.enabled:false} && ${egon.cola.component.tianshu.redis.enabled:true}) || "
                + "${egon.cola.component.tianshu.registry.enabled:false} || "
                + "${egon.cola.component.tianshu.ai.skill.workspace.subscription-enabled:false}"
)
```

- Verification contribution: `DdcRedisAutoConfigurationTest`。
- After this file: base starter 需重新 install。

#### File 8 — `MODIFY egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-ai-starter/src/main/java/top/egon/cola/component/tianshu/ai/autoconfigure/DdcAiStarterAutoConfiguration.java`

- Purpose: workspace Bean 装配。
- Symbols: `@Bean("ddcAiSkillMaterializer")`、`@Bean("ddcAiSkillWorkspace")`（`@ConditionalOnProperty(prefix="egon.cola.component.tianshu.ai.skill.workspace", name="enabled", havingValue="true")`，要求 `skill.enabled=true` 与 `DdcSkillClient`/`DdcAiSyncClient` 存在，`root` 非空且可写）、`@Bean("ddcAiWorkspaceReconcileScheduler", initMethod="start", destroyMethod="stop")`、`@Bean("ddcAiPublicationSubscription", destroyMethod="close") @ConditionalOnProperty(... "subscription-enabled")` 创建 `new DdcRedisTopicSubscription(List.of(redisson.getTopic(DdcRedisKeys.aiTopic(tenantId, biz, env, app), StringCodec.INSTANCE)), String.class, listener)`
- Repository evidence: `DdcRedisTopicSubscription` 构造；`DdcProperties.getBizCode()/getEnv()/getAppCode()`；`DdcTenantProperties.requireTenantId()`；`@Qualifier("ddcRedissonClient")`。
- Dependencies and consumers: 下游。
- Why now: 装配。
- Contract/signature changes: 新 Bean。
- Input/output and state mapping: 属性→Bean。
- Error and edge behavior: `subscription-enabled` 但无 `ddcRedissonClient`→`IllegalStateException("... requires egon.cola.component.tianshu.redis")`。
- Standards impact: MC-BEAN-001, MC-CONFIG-001。
- Literal rule enforcement: Rule 4、Rule 7、Rule 11。
- Implementation pseudocode:

```java
@Bean(name = "ddcAiPublicationSubscription", destroyMethod = "close") @ConditionalOnProperty(prefix = "egon.cola.component.tianshu.ai.skill.workspace", name = "subscription-enabled", havingValue = "true") @ConditionalOnBean(name = "ddcAiSkillWorkspace")
public DdcRedisTopicSubscription<String> ddcAiPublicationSubscription(@Qualifier("ddcRedissonClient") RedissonClient redisson, DdcProperties properties, DdcTenantProperties tenantProperties, DdcAiStarterProperties ai, @Qualifier("ddcAiSkillWorkspace") DdcAiSkillWorkspaceService workspace, @Qualifier("jacksonObjectMapper") ObjectMapper objectMapper, @Qualifier("ddcAiWorkspaceExecutor") ExecutorService executor) {
    String topic = DdcRedisKeys.aiTopic(tenantProperties.requireTenantId(), properties.getBizCode(), properties.getEnv(), properties.getAppCode());
    DdcAiPublicationListener listener = new DdcAiPublicationListener(objectMapper, tenantProperties, workspace, watchedIndex(ai.getSkill().getWorkspace().getResources()), executor);
    return new DdcRedisTopicSubscription<>(List.of(redisson.getTopic(topic, StringCodec.INSTANCE)), String.class, listener);
}
```

- Verification contribution: File 1 装配用例。
- After this file: 本步 GREEN。

- Validation working directory: 仓库根
- Verification command: `./mvnw -q -pl egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-starter -DskipTests install && ./mvnw -pl egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-starter -Dtest=DdcRedisAutoConfigurationTest test && ./mvnw -pl egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-ai-starter -Dtest=DdcAiSkillMaterializerTest,DdcAiWorkspaceReconcileSchedulerTest,DdcAiPublicationListenerTest,DdcAiStarterAutoConfigurationTest test`
- Expected result: 五测试类通过。
- Failure returns to: File 3（落盘）、File 8（装配）。
- Completion criteria: 原子落盘、租户过滤、对账决策、Redisson 条件均有断言。
- Rollback: 回退本步 Commit paths。
- Commit paths: egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-ai-starter/src/test/java/top/egon/cola/component/tianshu/ai/workspace/DdcAiSkillMaterializerTest.java, egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-ai-starter/src/test/java/top/egon/cola/component/tianshu/ai/workspace/DdcAiWorkspaceReconcileSchedulerTest.java, egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-ai-starter/src/test/java/top/egon/cola/component/tianshu/ai/workspace/DdcAiPublicationListenerTest.java, egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-ai-starter/src/test/java/top/egon/cola/component/tianshu/ai/autoconfigure/DdcAiStarterAutoConfigurationTest.java, egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-ai-starter/src/main/java/top/egon/cola/component/tianshu/ai/workspace/DdcAiSkillWorkspaceStateDTO.java, egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-ai-starter/src/main/java/top/egon/cola/component/tianshu/ai/workspace/DdcAiSkillMaterializer.java, egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-ai-starter/src/main/java/top/egon/cola/component/tianshu/ai/workspace/DdcAiSkillWorkspace.java, egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-ai-starter/src/main/java/top/egon/cola/component/tianshu/ai/workspace/DdcAiSkillWorkspaceService.java, egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-ai-starter/src/main/java/top/egon/cola/component/tianshu/ai/workspace/DdcAiPublicationListener.java, egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-ai-starter/src/main/java/top/egon/cola/component/tianshu/ai/workspace/DdcAiWorkspaceReconcileScheduler.java, egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-starter/src/main/java/top/egon/cola/component/tianshu/autoconfigure/DdcRedisAutoConfiguration.java, egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-starter/src/test/java/top/egon/cola/component/tianshu/autoconfigure/DdcRedisAutoConfigurationTest.java, egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-ai-starter/src/main/java/top/egon/cola/component/tianshu/ai/autoconfigure/DdcAiStarterAutoConfiguration.java
- Commit: `feat(tianshu): ai-starter skill workspace with publication subscription`

### Step 16 — 上行：创作目录监视与草稿上传/发布

- Requirements: `REQ-008`, `REQ-010`
- Dependencies: Step 15（属性、自动配置）、Step 13（服务端 RPC-005/006）
- Baseline state: ai-starter 已有 workspace；`DdcAiSyncClient#replaceSkillDraft/publishSkillDraft`；`DdcAiSkillDraftUploadDTO{namespaceCode, resourceKey, resourceId, expectedRevision, displayName, description, files}`。
- Observable outcome: `DdcAiSkillTreeReaderTest`（TEST-028）隐藏文件忽略/超限/非 UTF-8 拒绝；`DdcAiSkillAuthoringWatcherTest`（TEST-026/027）哈希变化→上传→可选发布→状态文件更新；56303→CONFLICT 停止重试，`resync` 恢复。
- End state: 上行链路完成。
- Test-first gate: Required — 两测试类 RED。
- Manual Checks: MC-ARCH-001, MC-REUSE-001, MC-NAME-001, MC-VALID-001, MC-MODEL-001, MC-LOG-001, MC-BEAN-001, MC-UTIL-001, MC-JSON-001, MC-TIME-001, MC-CONFIG-001, MC-SCOPE-001, MC-TEST-001
- Literal Rules: Rule 1, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 10, Rule 11
- Ordered files:

#### File 1 — `CREATE egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-ai-starter/src/test/java/top/egon/cola/component/tianshu/ai/authoring/DdcAiSkillTreeReaderTest.java`

- Purpose: 两测试类（合并块：`DdcAiSkillTreeReaderTest`、`DdcAiSkillAuthoringWatcherTest`）+ `DdcAiStarterAutoConfigurationTest` 追加 `authoringEnabledRegistersWatcher`。
- Symbols: reader：`ignoresHiddenFilesAndDirectories`、`rejectsMoreThan256NodesOrFileOver256KiB`、`rejectsNonUtf8Content`、`producesStableSha256OverSortedPaths`；watcher：`uploadsWhenTreeHashChanges`、`publishesWhenAutoPublishConfigured`、`conflictStopsUntilResync`、`debouncesRapidEdits`
- Repository evidence: `@TempDir`；假 `DdcAiSyncClient` 抛 `DdcException`（状态 `TIANSHU_AI_REVISION_CONFLICT`）。
- Dependencies and consumers: File 2–5。
- Why now: RED。
- Contract/signature changes: 无。
- Input/output and state mapping: 目录→`DdcAiSkillDraftUploadDTO`；`authoring-state.json`。
- Error and edge behavior: 冲突后状态 `CONFLICT`，再次 poll 不调用 `replaceSkillDraft`；删除状态文件（`resync`）后恢复。
- Standards impact: MC-TEST-001。
- Literal rule enforcement: Rule 11。
- Implementation pseudocode:

```java
@Test void conflictStopsUntilResync(@TempDir Path dir) throws IOException {
    write(dir, "SKILL.md", "# a"); when(syncClient.replaceSkillDraft(any())).thenThrow(conflict56303());
    watcher.pollOnce(); assertThat(stateOf(dir).status()).isEqualTo(CONFLICT);
    write(dir, "SKILL.md", "# b"); watcher.pollOnce(); verify(syncClient, times(1)).replaceSkillDraft(any());
    watcher.resync(resource); when(syncClient.getVersionHead(any())).thenReturn(head(revision "9")); when(syncClient.replaceSkillDraft(any())).thenReturn(revision("10", sha)); watcher.pollOnce(); verify(syncClient, times(2)).replaceSkillDraft(argThat(u -> "9".equals(u.getExpectedRevision())));
}
```

- Verification contribution: RED→GREEN。
- After this file: RED。

#### File 2 — `CREATE egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-ai-starter/src/main/java/top/egon/cola/component/tianshu/ai/authoring/DdcAiAuthoringStateDTO.java`

- Purpose: 上行状态值对象。
- Symbols: `record DdcAiAuthoringStateDTO(String namespaceCode, String resourceKey, Long resourceId, String baseRevision, String lastUploadedSha256, Status status, String lastError, Instant updatedAt)`；`enum Status { IDLE, UPLOADED, PUBLISHED, CONFLICT, ERROR }`
- Repository evidence: record 值对象。
- Dependencies and consumers: Watcher。
- Why now: 状态契约。
- Contract/signature changes: 新 record。
- Input/output and state mapping: `authoring-state.json`。
- Error and edge behavior: 无。
- Standards impact: MC-MODEL-001, MC-JSON-001, MC-TIME-001。
- Literal rule enforcement: Rule 3、Rule 6、Rule 10、Rule 11。
- Implementation pseudocode:

```java
public record DdcAiAuthoringStateDTO(String namespaceCode, String resourceKey, Long resourceId, String baseRevision, String lastUploadedSha256, Status status, String lastError, Instant updatedAt) {
    public enum Status { IDLE, UPLOADED, PUBLISHED, CONFLICT, ERROR }
    public DdcAiAuthoringStateDTO withStatus(Status s, String error, Instant at) { return new DdcAiAuthoringStateDTO(namespaceCode, resourceKey, resourceId, baseRevision, lastUploadedSha256, s, error, at); }
}
```

- Verification contribution: File 1。
- After this file: 编译通过。

#### File 3 — `CREATE egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-ai-starter/src/main/java/top/egon/cola/component/tianshu/ai/authoring/DdcAiSkillTreeReader.java`

- Purpose: 目录→节点列表与稳定哈希。
- Symbols: `public class DdcAiSkillTreeReader`；`DdcAiSkillTreeSnapshotDTO read(Path directory)`（record `DdcAiSkillTreeSnapshotDTO(List<DdcAiNodeDTO> nodes, String sha256)` 同包）；限额 256 节点、256KiB 文件、1MiB 总量
- Repository evidence: 限额与 `DdcSkillContentStrategy` 一致（DEC-004）。
- Dependencies and consumers: Watcher。
- Why now: 上传内容。
- Contract/signature changes: 新类。
- Input/output and state mapping: `Files.walk` 排序；忽略以 `.` 开头的文件/目录；`sha256` 为按路径排序的 `path\0content\0` 拼接摘要（commons-codec `DigestUtils`）。
- Error and edge behavior: 非 UTF-8（`CharsetDecoder` REPORT）→`IllegalArgumentException("NON_UTF8:" + path)`；超限→`IllegalArgumentException("LIMIT:...")`；缺根 `SKILL.md`→拒绝。
- Standards impact: MC-UTIL-001, MC-LOG-001。
- Literal rule enforcement: Rule 5、Rule 4、Rule 11。
- Implementation pseudocode:

```java
public DdcAiSkillTreeSnapshotDTO read(Path dir) throws IOException {
    List<java.nio.file.Path> paths; try (var s = Files.walk(dir)) { paths = s.filter(p -> !p.equals(dir)).filter(p -> !hidden(dir, p)).sorted().toList(); }
    if (paths.size() > 256) throw new IllegalArgumentException("LIMIT:NODES"); List<DdcAiNodeDTO> nodes = new ArrayList<>(); long total = 0; MessageDigest digest = DigestUtils.getSha256Digest();
    for (Path p : paths) { String rel = dir.relativize(p).toString().replace('\\', '/');
        if (Files.isDirectory(p)) { nodes.add(DdcAiNodeDTO.builder().path(rel).nodeType(DIRECTORY).build()); continue; }
        byte[] bytes = Files.readAllBytes(p); if (bytes.length > 256 * 1024) throw new IllegalArgumentException("LIMIT:FILE:" + rel); total += bytes.length; if (total > 1024 * 1024) throw new IllegalArgumentException("LIMIT:TOTAL");
        String content = decodeStrictUtf8(bytes, rel); nodes.add(DdcAiNodeDTO.builder().path(rel).nodeType(FILE).content(content).build()); digest.update(rel.getBytes(UTF_8)); digest.update((byte) 0); digest.update(bytes); digest.update((byte) 0); }
    if (nodes.stream().noneMatch(n -> "SKILL.md".equals(n.getPath()))) throw new IllegalArgumentException("ROOT_SKILL_MD_REQUIRED");
    return new DdcAiSkillTreeSnapshotDTO(List.copyOf(nodes), Hex.encodeHexString(digest.digest()));
}
```

- Verification contribution: File 1 reader 用例。
- After this file: reader 用例 GREEN。

#### File 4 — `CREATE egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-ai-starter/src/main/java/top/egon/cola/component/tianshu/ai/authoring/DdcAiSkillAuthoringWatcher.java`

- Purpose: 轮询 + 去抖 + 上传 + 可选发布 + 状态文件。
- Symbols: `public class DdcAiSkillAuthoringWatcher`；构造 `(DdcAiSyncClient, DdcAiSkillTreeReader, DdcAiAuthoringProperties, ObjectMapper, MeterRegistry, Clock, ScheduledExecutorService)`；`start()/stop()`；`pollOnce()`；`resync(DdcAiWatchedResourceDTO)`；每资源 `lastSeenSha/lastChangeAt`
- Repository evidence: 自管调度线程模式（Step 15 File 6）。
- Dependencies and consumers: 自动配置。
- Why now: 上行驱动。
- Contract/signature changes: 新类。
- Input/output and state mapping: 每 `pollInterval` 读树→sha 变化且距上次变化 ≥`debounce`→`baseRevision`（状态文件；缺失时 `getVersionHead().resourceRevision`）→`replaceSkillDraft(upload(expectedRevision=baseRevision))`→状态 `UPLOADED(baseRevision=result.resourceRevision, lastUploadedSha256=sha)`→`autoPublish`→`publishSkillDraft(requestId="auth-" + sha 前 32 位 + "-" + resourceId 的稳定派生, changeNote)`→`PUBLISHED(baseRevision=receipt.resourceRevision)`。
- Error and edge behavior: `DdcException` 状态 `TIANSHU_AI_REVISION_CONFLICT`→`CONFLICT`，该资源停止上传直到 `resync`（删除状态文件或调用方法）；指标 `tianshu.ai.authoring.uploads{result}`；`CONFLICT` 时 ERROR 日志一次；其他异常→`ERROR` 状态，下周期重试。
- Standards impact: MC-LOG-001, MC-JSON-001, MC-TIME-001, MC-UTIL-001。
- Literal rule enforcement: Rule 4、Rule 6、Rule 10、Rule 5、Rule 11。
- Implementation pseudocode:

```java
public void pollOnce() {
    for (DdcAiWatchedResourceDTO r : props.getResources()) {
        try { DdcAiAuthoringStateDTO state = loadState(r); if (state.status() == CONFLICT) continue;
              DdcAiSkillTreeSnapshotDTO snapshot = reader.read(directory(r)); Instant now = clock.instant();
              if (snapshot.sha256().equals(state.lastUploadedSha256())) continue;
              if (!snapshot.sha256().equals(lastSeen.get(r.key()))) { lastSeen.put(r.key(), snapshot.sha256()); lastChange.put(r.key(), now); continue; }
              if (Duration.between(lastChange.get(r.key()), now).compareTo(props.getDebounce()) < 0) continue;
              String base = state.baseRevision() != null ? state.baseRevision() : syncClient.getVersionHead(query(r)).getResourceRevision();
              DdcAiRevisionDTO rev = syncClient.replaceSkillDraft(upload(r, base, snapshot)); state = new DdcAiAuthoringStateDTO(r.getNamespaceCode(), r.getResourceKey(), r.getResourceId(), rev.getResourceRevision(), snapshot.sha256(), UPLOADED, null, now);
              if (Boolean.TRUE.equals(r.getAutoPublish())) { DdcAiPublicationReceiptDTO receipt = syncClient.publishSkillDraft(publish(r, rev.getResourceRevision(), requestIdFor(r, snapshot.sha256()))); state = state.withBaseRevision(receipt.getResourceRevision()).withStatus(PUBLISHED, null, now); }
              saveState(r, state); meters.counter("tianshu.ai.authoring.uploads", "result", state.status().name()).increment(); }
        catch (DdcException failure) { if ("TIANSHU_AI_REVISION_CONFLICT".equals(failure.getStatus())) { saveState(r, loadState(r).withStatus(CONFLICT, failure.getStatus(), clock.instant())); log.error("AI authoring conflict for {}/{}; run resync after pulling the remote version", r.getNamespaceCode(), r.getResourceKey()); } else saveState(r, loadState(r).withStatus(ERROR, failure.getStatus(), clock.instant())); }
        catch (IOException | RuntimeException failure) { log.warn("AI authoring poll failed for {}/{}", r.getNamespaceCode(), r.getResourceKey(), failure); }
    }
}
```

- Verification contribution: File 1 watcher 用例。
- After this file: File 1 GREEN。

#### File 5 — `MODIFY egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-ai-starter/src/main/java/top/egon/cola/component/tianshu/ai/autoconfigure/DdcAiStarterAutoConfiguration.java`

- Purpose: authoring Bean 装配。
- Symbols: `@Bean("ddcAiSkillTreeReader")`、`@Bean(name="ddcAiSkillAuthoringWatcher", initMethod="start", destroyMethod="stop") @ConditionalOnProperty(prefix="egon.cola.component.tianshu.ai.skill.authoring", name="enabled", havingValue="true")`；要求 `DdcAiSyncClient` 存在与 `root` 可读；workspace 与 authoring `root` 相同→启动失败（DEC-003 分目录）
- Repository evidence: Step 15 File 8。
- Dependencies and consumers: 下游。
- Why now: 装配。
- Contract/signature changes: 新 Bean。
- Input/output and state mapping: 属性→Bean。
- Error and edge behavior: `authoring.root` 等于 `workspace.root`（规范化后）→`IllegalStateException("tianshu ai authoring root must differ from workspace root")`。
- Standards impact: MC-BEAN-001, MC-CONFIG-001。
- Literal rule enforcement: Rule 4、Rule 7、Rule 11。
- Implementation pseudocode:

```java
@Bean(name = "ddcAiSkillAuthoringWatcher", initMethod = "start", destroyMethod = "stop") @ConditionalOnProperty(prefix = "egon.cola.component.tianshu.ai.skill.authoring", name = "enabled", havingValue = "true")
public DdcAiSkillAuthoringWatcher ddcAiSkillAuthoringWatcher(ObjectProvider<DdcAiSyncClient> syncClient, DdcAiStarterProperties ai, @Qualifier("ddcAiSkillTreeReader") DdcAiSkillTreeReader reader, @Qualifier("jacksonObjectMapper") ObjectMapper objectMapper, ObjectProvider<MeterRegistry> meters) {
    DdcAiAuthoringProperties a = ai.getSkill().getAuthoring(); DdcAiWorkspaceProperties w = ai.getSkill().getWorkspace();
    if (a.getRoot() == null) throw new IllegalStateException("egon.cola.component.tianshu.ai.skill.authoring.root is required");
    if (w.isEnabled() && w.getRoot() != null && w.getRoot().toAbsolutePath().normalize().equals(a.getRoot().toAbsolutePath().normalize())) throw new IllegalStateException("tianshu ai authoring root must differ from workspace root");
    DdcAiSyncClient port = syncClient.getIfAvailable(); if (port == null) throw new IllegalStateException("egon.cola.component.tianshu.ai.skill.authoring.enabled requires a DdcAiSyncClient adapter");
    return new DdcAiSkillAuthoringWatcher(port, reader, a, objectMapper, meters.getIfAvailable(SimpleMeterRegistry::new), Clock.systemUTC(), Executors.newSingleThreadScheduledExecutor(new CustomizableThreadFactory("tianshu-ai-authoring-")));
}
```

- Verification contribution: File 1 装配用例。
- After this file: 本步 GREEN。

- Validation working directory: 仓库根
- Verification command: `./mvnw -pl egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-ai-starter test`
- Expected result: 模块全部测试通过（含 Step 14/15）。
- Failure returns to: File 4（状态机）、File 3（限额）。
- Completion criteria: 上传/发布/冲突/resync/去抖/限额均有断言。
- Rollback: 回退本步 Commit paths。
- Commit paths: egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-ai-starter/src/test/java/top/egon/cola/component/tianshu/ai/authoring/DdcAiSkillTreeReaderTest.java, egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-ai-starter/src/test/java/top/egon/cola/component/tianshu/ai/authoring/DdcAiSkillAuthoringWatcherTest.java, egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-ai-starter/src/test/java/top/egon/cola/component/tianshu/ai/autoconfigure/DdcAiStarterAutoConfigurationTest.java, egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-ai-starter/src/main/java/top/egon/cola/component/tianshu/ai/authoring/DdcAiAuthoringStateDTO.java, egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-ai-starter/src/main/java/top/egon/cola/component/tianshu/ai/authoring/DdcAiSkillTreeReader.java, egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-ai-starter/src/main/java/top/egon/cola/component/tianshu/ai/authoring/DdcAiSkillTreeSnapshotDTO.java, egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-ai-starter/src/main/java/top/egon/cola/component/tianshu/ai/authoring/DdcAiSkillAuthoringWatcher.java, egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-ai-starter/src/main/java/top/egon/cola/component/tianshu/ai/autoconfigure/DdcAiStarterAutoConfiguration.java
- Commit: `feat(tianshu): ai-starter authoring watcher uploads skill drafts`

### Step 17 — 管理 Web：第三页签“反馈与优化”

- Requirements: `REQ-009`
- Dependencies: Step 12
- Baseline state: `AiResourceEditorPage.tsx` `Tabs items` 两项（`draft`、`versions`）；`api/ai.ts` 的 `mutationOptions(requestId, body)` 设置 `Idempotency-Key`、`ddcPageApi`、`queryString`、`resourcePath`；`types.ts` 以 `DdcAi*` 类型命名；Vitest + Testing Library 测试（`AiVersionsPanel.test.tsx` 风格）。
- Observable outcome: `AiOptimizationPanel.test.tsx`/`AiFeedbackPanel.test.tsx`（TEST-019）与 `AiResourceEditorPage.test.tsx` 新用例通过；`typecheck`、`lint` 通过。
- End state: 页签可配置策略、rearm、手动触发并轮询运行、浏览反馈。
- Test-first gate: Required — 两个面板测试与页面测试新用例 RED。
- Manual Checks: MC-SCOPE-001, MC-TEST-001, MC-TIME-001
- Literal Rules: Rule 10, Rule 11
- Ordered files:

#### File 1 — `CREATE egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin-web/src/pages/AiOptimizationPanel.test.tsx`

- Purpose: 面板测试（合并块：`AiOptimizationPanel.test.tsx`、`AiFeedbackPanel.test.tsx`）。
- Symbols: `renders default policy and saves with revision 0`、`shows 409 conflict and keeps form values`、`rearm disabled while run active`、`trigger posts Idempotency-Key and polls until terminal state`、`shows channel disabled banner on 503`；feedback：`filters by outcome and judge state`、`renders excerpts read-only and marks UTC instants`
- Repository evidence: `AiVersionsPanel.test.tsx`（`vi.mock('../api/ai')`、`renderWithProviders`）。
- Dependencies and consumers: File 2–4。
- Why now: RED。
- Contract/signature changes: 无。
- Input/output and state mapping: 假 API 返回 `DdcAiOptimizationPolicyResult`/`DdcAiOptimizationRunResult` fixtures。
- Error and edge behavior: 轮询在 `SUCCEEDED/FAILED/SKIPPED` 停止；`vi.useFakeTimers()`。
- Standards impact: MC-TEST-001。
- Literal rule enforcement: Rule 11。
- Implementation pseudocode:

```tsx
it('trigger posts Idempotency-Key and polls until terminal state', async () => {
  vi.useFakeTimers()
  mocked(triggerAiOptimizationRun).mockResolvedValue(run({ id: '88', state: 'PENDING' }))
  mocked(getAiOptimizationRun).mockResolvedValueOnce(run({ id: '88', state: 'RUNNING' })).mockResolvedValueOnce(run({ id: '88', state: 'SUCCEEDED', producedVersionId: '9' }))
  renderPanel({ policy: policy({ persisted: true, state: 'ARMED', activeRunId: null }) })
  await userEvent.click(screen.getByRole('button', { name: '立即优化一次' }))
  expect(mocked(triggerAiOptimizationRun).mock.calls[0][1]).toMatch(/^[A-Za-z0-9_-]{16,64}$/)
  await vi.advanceTimersByTimeAsync(2000); await vi.advanceTimersByTimeAsync(2000)
  expect(mocked(getAiOptimizationRun)).toHaveBeenCalledTimes(2); expect(screen.getByText('SUCCEEDED')).toBeInTheDocument()
})
```

- Verification contribution: RED→GREEN。
- After this file: RED。

#### File 2 — `MODIFY egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin-web/src/api/types.ts`

- Purpose: 新类型。
- Symbols: `DdcAiPolicyState`、`DdcAiRunState`、`DdcAiRunTrigger`、`DdcAiRunErrorCode`、`DdcAiOutcome`、`DdcAiJudgeState`、`DdcAiOptimizationEvaluation`、`DdcAiOptimizationPolicyResult`、`DdcAiOptimizationPolicyCommand`、`DdcAiOptimizationTriggerCommand`、`DdcAiOptimizationRunResult`、`DdcAiUsageFeedbackResult`、`DdcAiOptimizationRunPageParams`、`DdcAiUsageFeedbackPageParams`
- Repository evidence: `types.ts:248–310`（`DdcAiKind`、`DdcAiVersionSummary` 字段均 string ID）。
- Dependencies and consumers: `ai.ts`、面板。
- Why now: 类型前置。
- Contract/signature changes: 追加类型。
- Input/output and state mapping: 与 Step 9 File 7 的 Result 字段一一对应；`Duration` 为 ISO-8601 字符串；`Instant` 为 ISO Z 字符串。
- Error and edge behavior: 无。
- Standards impact: MC-SCOPE-001。
- Literal rule enforcement: Rule 11。
- Implementation pseudocode:

```ts
export type DdcAiPolicyState = 'ARMED' | 'AWAITING_PUBLISH' | 'CONVERGED' | 'EXHAUSTED' | 'PAUSED'
export type DdcAiRunState = 'PENDING' | 'RUNNING' | 'SUCCEEDED' | 'FAILED' | 'SKIPPED'
export type DdcAiOptimizationPolicyResult = { resourceId: string; persisted: boolean; revision: string; enabled: boolean; scoreThreshold: number; minSamples: number; evaluationWindow: string; maxIterations: number; cooldown: string; autoPublish: boolean; judgeEnabled: boolean; optimizerModelAlias: string; judgeModelAlias: string; optimizerInstruction: string; state: DdcAiPolicyState; iterationsDone: number; consecutiveFailures: number; lastEvaluatedAt: string | null; convergedAt: string | null; activeRunId: string | null; evaluation: DdcAiOptimizationEvaluation | null }
export type DdcAiOptimizationRunResult = { id: string; resourceId: string; runNo: string; iterationNo: number; trigger: DdcAiRunTrigger; requestId: string | null; state: DdcAiRunState; baseVersionId: string; sampleCount: number; averageScore: number | null; thresholdSnapshot: number; windowStart: string; windowEnd: string; modelAlias: string; producedVersionId: string | null; publicationId: string | null; autoPublished: boolean; errorCode: DdcAiRunErrorCode | null; errorMessage: string | null; note: string; startedAt: string | null; finishedAt: string | null; createTime: string }
```

- Verification contribution: `typecheck`。
- After this file: 类型就绪。

#### File 3 — `MODIFY egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin-web/src/api/ai.ts`

- Purpose: 6 个 API 函数与 query keys。
- Symbols: `getAiOptimizationPolicy(resourceId)`、`replaceAiOptimizationPolicy(resourceId, command)`（`PUT`）、`triggerAiOptimizationRun(resourceId, requestId, command)`（`mutationOptions`）、`pageAiOptimizationRuns(resourceId, params)`、`getAiOptimizationRun(resourceId, runId)`、`pageAiUsageFeedback(resourceId, params)`；`ddcAiQueryKeys.optimizationPolicy/optimizationRuns/optimizationRun/usageFeedback`
- Repository evidence: `ai.ts:58–70、195–210`。
- Dependencies and consumers: 面板。
- Why now: 数据访问。
- Contract/signature changes: 追加导出。
- Input/output and state mapping: 路径 `${resourcePath(id)}/optimization-policy` 等；`PUT` 用 `{ ...options, method: 'PUT', body }`。
- Error and edge behavior: 沿既有 `ddcApi` 错误映射（`code` 透传）。
- Standards impact: MC-SCOPE-001。
- Literal rule enforcement: Rule 11。
- Implementation pseudocode:

```ts
export const replaceAiOptimizationPolicy = (resourceId: string, command: DdcAiOptimizationPolicyCommand, options: RequestOptions = {}): Promise<ResultRecord<DdcAiOptimizationPolicyResult>> => {
  requireTenant(); return ddcApi<DdcAiOptimizationPolicyResult>(`${resourcePath(resourceId)}/optimization-policy`, { ...options, method: 'PUT', body: command })
}
export const triggerAiOptimizationRun = (resourceId: string, requestId: string, command: DdcAiOptimizationTriggerCommand, options: RequestOptions = {}): Promise<ResultRecord<DdcAiOptimizationRunResult>> => {
  requireTenant(); return ddcApi<DdcAiOptimizationRunResult>(`${resourcePath(resourceId)}/optimization-runs`, mutationOptions(requestId, command, options))
}
export const pageAiUsageFeedback = (resourceId: string, query: DdcAiUsageFeedbackPageParams = {}, options: RequestOptions = {}) => { requireTenant(); const search = queryString({ versionId: query.versionId, outcome: query.outcome, judgeState: query.judgeState, from: query.from, to: query.to, pageNo: query.pageNo ?? 1, pageSize: query.pageSize ?? 20 }); return ddcPageApi<DdcAiUsageFeedbackResult>(`${resourcePath(resourceId)}/usage-feedback?${search}`, options) }
```

- Verification contribution: File 1（mock）、`ai.test.ts` 追加路径断言。
- After this file: API 就绪。

#### File 4 — `CREATE egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin-web/src/pages/AiOptimizationPanel.tsx`

- Purpose: 两个面板组件（合并块：`AiOptimizationPanel.tsx`、`AiFeedbackPanel.tsx`）。
- Symbols: `AiOptimizationPanel({ resourceId, canPublish, canRead })`：策略表单（Form + InputNumber/Switch/Input，Duration 以 ISO 字符串输入并校验 `PT5M..P30D`）、状态徽标、评估快照卡、rearm 按钮（`activeRunId` 非空禁用）、“立即优化一次”（生成 requestId，202 后每 2s 轮询 `getAiOptimizationRun` 至终态）、运行表（分页、state/trigger 筛选）；`AiFeedbackPanel({ resourceId })`：筛选（versionId/outcome/judgeState/from/to）、表格（摘录折叠展示、分数、评分状态、`occurredAt` 显示本地时间并 tooltip 原始 UTC）
- Repository evidence: `AiVersionsPanel.tsx`（`useQuery`、权限 props、`Alert`）。
- Dependencies and consumers: 编辑页。
- Why now: UI。
- Contract/signature changes: 新组件。
- Input/output and state mapping: 409→保留表单值并提示刷新修订；503(56322)→横幅“优化通道未启用”；56323→提示等待运行结束。
- Error and edge behavior: 轮询在卸载时清理；终态停止。
- Standards impact: MC-TIME-001（显示时区与 UTC 原值）。
- Literal rule enforcement: Rule 10（时间仅做显示转换）、Rule 11。
- Implementation pseudocode:

```tsx
const policyQuery = useQuery({ queryKey: ddcAiQueryKeys.optimizationPolicy(resourceId), queryFn: () => getAiOptimizationPolicy(resourceId) })
const save = useMutation({ mutationFn: (command: DdcAiOptimizationPolicyCommand) => replaceAiOptimizationPolicy(resourceId, command),
  onError: (error) => { if (isDdcError(error, 56303)) setConflict(true); else if (isDdcError(error, 56323)) message.warning('存在活动运行，暂不能重新启用'); else if (isDdcError(error, 56322)) setChannelDisabled(true) },
  onSuccess: (result) => { form.setFieldValue('expectedRevision', result.data.revision); queryClient.invalidateQueries({ queryKey: ddcAiQueryKeys.optimizationPolicy(resourceId) }) } })
const trigger = async () => { const requestId = createRequestId(); const run = await triggerAiOptimizationRun(resourceId, requestId, { note }); setActiveRun(run.data) }
useEffect(() => { if (!activeRun || ['SUCCEEDED', 'FAILED', 'SKIPPED'].includes(activeRun.state)) return; const t = setInterval(async () => { const r = await getAiOptimizationRun(resourceId, activeRun.id); setActiveRun(r.data) }, 2000); return () => clearInterval(t) }, [activeRun?.id, activeRun?.state])
```

- Verification contribution: File 1。
- After this file: File 1 GREEN。

#### File 5 — `MODIFY egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin-web/src/pages/AiResourceEditorPage.tsx`

- Purpose: 第三页签（合并块：含 `AiResourceEditorPage.test.tsx` 新用例 `shows optimization tab and keeps draft buffer when switching`）。
- Symbols: `Tabs items` 追加 `{ key: 'optimization', label: '反馈与优化', children: <Space ...><AiOptimizationPanel .../><AiFeedbackPanel .../></Space> }`；`handleTabChange` 允许 `optimization`
- Repository evidence: `AiResourceEditorPage.tsx:509–620`。
- Dependencies and consumers: 路由。
- Why now: 入口。
- Contract/signature changes: 无。
- Input/output and state mapping: `canPublish/canRead` 由既有权限上下文派生。
- Error and edge behavior: 切换页签不丢草稿（既有缓冲）。
- Standards impact: MC-SCOPE-001。
- Literal rule enforcement: Rule 11。
- Implementation pseudocode:

```tsx
{
  key: 'optimization',
  label: '反馈与优化',
  children: (
    <Space orientation="vertical" size="large" className="tianshu-ai-page">
      <AiOptimizationPanel resourceId={resourceId} canPublish={permissions.canPublish} canRead={permissions.canRead} />
      <AiFeedbackPanel resourceId={resourceId} />
    </Space>
  ),
},
```

- Verification contribution: 页面测试。
- After this file: 本步 GREEN。

- Validation working directory: `egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin-web`
- Verification command: `npm run typecheck && npm run test -- src/pages/AiOptimizationPanel.test.tsx src/pages/AiFeedbackPanel.test.tsx src/pages/AiResourceEditorPage.test.tsx src/api/ai.test.ts && npm run lint`
- Expected result: 三命令退出 0。
- Failure returns to: File 4（状态/轮询）、File 3（路径）。
- Completion criteria: 状态矩阵、409/503/409(56323)、轮询终止、筛选均有断言。
- Rollback: 回退本步 Commit paths。
- Commit paths: egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin-web/src/pages/AiOptimizationPanel.test.tsx, egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin-web/src/pages/AiFeedbackPanel.test.tsx, egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin-web/src/api/types.ts, egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin-web/src/api/ai.ts, egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin-web/src/api/ai.test.ts, egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin-web/src/pages/AiOptimizationPanel.tsx, egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin-web/src/pages/AiFeedbackPanel.tsx, egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin-web/src/pages/AiResourceEditorPage.tsx, egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin-web/src/pages/AiResourceEditorPage.test.tsx
- Commit: `feat(tianshu-web): ai feedback and optimization tab`

### Step 18 — 标准门禁测试、运维手册与 README

- Requirements: `REQ-001`, `REQ-012`, `REQ-014`
- Dependencies: Step 1–17
- Baseline state: `AT/contract/DdcArchitectureContractTest.java`（反射扫描 Mapper 继承 `EgonColaMapper` 等）；`T/docs/tianshu-ai-migration-runbook.md` 节：上线前准备 / 管理与跨 namespace 发布 / PostgreSQL DDL 初始化 / V9 导入、核验与切流 / 受控集成验收；`T/README.md:52–53` 模块表。
- Observable outcome: `DdcAiOptimizationStandardsTest`（TEST-029/030）扫描本计划新增的 Admin 与 ai-starter 类：后缀命名、`@Slf4j`、具名 Bean、构造注入 `@Qualifier`、无 `java.util.Date`、无禁用工具库、`admin.ai.*` 三 profile 键一致；手册与 README 更新。
- End state: 文档与门禁收尾；无 `scripts/work/` 临时脚本（所有门禁由既有 Maven/npm 命令覆盖）。
- Test-first gate: Required — 门禁测试新建 RED（若扫描发现违规，先修对应 Step 文件再 GREEN）。
- Manual Checks: MC-ARCH-001, MC-REUSE-001, MC-NAME-001, MC-LOG-001, MC-BEAN-001, MC-UTIL-001, MC-TIME-001, MC-CONFIG-001, MC-SCOPE-001, MC-TEST-001
- Literal Rules: Rule 1, Rule 4, Rule 5, Rule 7, Rule 10, Rule 11
- Ordered files:

#### File 1 — `CREATE egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/test/java/top/egon/cola/component/tianshu/admin/contract/DdcAiOptimizationStandardsTest.java`

- Purpose: TEST-029/030 静态门禁。
- Symbols: `newTypesUseSemanticSuffixes`（包 `model.po/bo/dto/vo/command/query/event/enums` 下本计划新类后缀 ∈ {PO,BO,DTO,Result,Command,Query,Event,PageQuery,Enum,Converter}）、`businessClassesDeclareSlf4jNamedBeansAndQualifiedConstructors`（`service.ai.*`、`repository.impl`、`notification` 新类：`@Slf4j`；`@Service/@Component/@Repository` 值非空；构造参数均 `@Qualifier` 或为属性/`ObjectProvider`/`Clock`）、`noJavaUtilDateOrForbiddenUtilities`（源码文本扫描 `java.util.Date`、`cn.hutool`、`org.joda`）、`aiProfilesDeclareIdenticalKeys`（复用 Step 5 File 3 逻辑扫描 `admin.ai.*`）、`aiStarterMetadataListsAllKeys`（读取 ai-starter jar 中 `spring-configuration-metadata.json`）
- Repository evidence: `DdcArchitectureContractTest`（ClassGraph/反射扫描写法）。
- Dependencies and consumers: 全部 Step。
- Why now: 收尾门禁。
- Contract/signature changes: 无。
- Input/output and state mapping: 类路径扫描 `top.egon.cola.component.tianshu.admin` 与 `top.egon.cola.component.tianshu.ai`。
- Error and edge behavior: 白名单：既有类不在范围（按本计划 Commit paths 的类名清单常量）。
- Standards impact: MC-TEST-001, MC-NAME-001, MC-LOG-001, MC-BEAN-001, MC-UTIL-001, MC-TIME-001, MC-CONFIG-001。
- Literal rule enforcement: Rule 1、Rule 4、Rule 5、Rule 10、Rule 7、Rule 11。
- Implementation pseudocode:

```java
private static final Set<String> NEW_BUSINESS_CLASSES = Set.of("DdcAiFeedbackServiceImpl", "DdcAiOptimizationServiceImpl", "DdcAiPublicationNotifier", "DdcAiPublicationDeliveryHandler", "DdcDefaultAiJudgeStrategy", "DdcPromptOptimizerStrategy", "DdcSkillOptimizerStrategy", "DdcAiUsageFeedbackRepositoryImpl", "DdcAiOptimizationPolicyRepositoryImpl", "DdcAiOptimizationRunRepositoryImpl", "DdcAiPromptTemplateLoader", "DdcAiJudgeContextLoader", "DdcAiOptimizationStateTransition");
@Test void businessClassesDeclareSlf4jNamedBeansAndQualifiedConstructors() {
    for (Class<?> type : load(NEW_BUSINESS_CLASSES)) {
        assertThat(hasField(type, "log")).as("%s must declare @Slf4j", type).isTrue();
        String beanName = beanName(type); assertThat(beanName).as("%s must name its bean", type).isNotBlank();
        Constructor<?> ctor = type.getConstructors()[0]; for (Parameter p : ctor.getParameters()) assertThat(p.isAnnotationPresent(Qualifier.class) || EXEMPT_TYPES.contains(p.getType())).as("%s param %s needs @Qualifier", type, p.getName()).isTrue();
    }
}
@Test void noJavaUtilDateOrForbiddenUtilities() throws IOException { try (var files = Files.walk(Path.of("src/main/java/top/egon/cola/component/tianshu/admin"))) { files.filter(p -> p.toString().endsWith(".java") && NEW_FILES.contains(p.getFileName().toString())).forEach(p -> assertThat(read(p)).doesNotContain("java.util.Date", "cn.hutool", "org.joda")); } }
```

- Verification contribution: 本步 GREEN。
- After this file: 门禁生效。

#### File 2 — `MODIFY egon-cola-xingyuan/egon-cola-tianshu/docs/tianshu-ai-migration-runbook.md`

- Purpose: 运维手册追加章节（主 Spec §15.5）。
- Symbols: `## AI 反馈与自动优化（DDL-201）`：DDL-201 `validate→initialize`、玉衡 audience/scopes 与 SERVICE client 登记、`admin.ai.*` 环境变量、凭据 `allowed-operations` 扩展顺序（Admin 先于下游）、策略 PAUSED/冲突处理、ai-starter `resync` 操作、物理清理 SQL（`DELETE FROM ddc_ai_usage_feedback_t0 WHERE deleted_at < now() - interval '90 days'`）
- Repository evidence: 手册既有节结构。
- Dependencies and consumers: 运维。
- Why now: 上线前文档。
- Contract/signature changes: 文档。
- Input/output and state mapping: 无。
- Error and edge behavior: 无。
- Standards impact: MC-SCOPE-001。
- Literal rule enforcement: Rule 11。
- Implementation pseudocode:

```markdown
## AI 反馈与自动优化（DDL-201）
1. 部署顺序：Admin（含 `V20261017_001__tianshu_ai_feedback_optimization.sql`）→ 按手册 `validate` 后 `initialize` → 确认 `ddl_history` 新行 → 再升级 adapter/ai-starter 的下游。
2. 玉衡：登记 Admin 的 SERVICE client（`TIANSHU_AI_MODEL_REGISTRATION_ID/APP_ID/RESOURCE_URI/SCOPES`），`TIANSHU_AI_MODEL_BASE_URL` 指向网关；`TIANSHU_AI_OPTIMIZATION_ENABLED=true` 在验证模型连通后再开启。
3. 凭据：runtime 凭据 `allowed-operations` 追加 `AI_REPORT`（上报）；需要上行的下游另配 `AI_WRITE,AI_PUBLISH` 凭据并限制 namespace 模式。
4. 策略 PAUSED：查看运行 `error_code`，修复后在页面 rearm；上行 CONFLICT：拉取远端 current 后删除 `authoring-state.json` 或调用 `resync`。
5. 物理清理：`DELETE FROM ddc_ai_usage_feedback_t0 WHERE deleted_at IS NOT NULL AND deleted_at < now() - interval '90 days';`
```

- Verification contribution: 人工阅读。
- After this file: 手册完成。

#### File 3 — `MODIFY egon-cola-xingyuan/egon-cola-tianshu/README.md`

- Purpose: 模块表与功能说明（合并块：`README.md`、`README.zh-CN.md`）。
- Symbols: 模块表追加 `egon-cola-tianshu-ai-starter` 行；AI 段落追加“使用反馈上报、LLM-as-judge 评分、按策略自动优化、Skill 工作区落盘/订阅/对账、创作目录上传”与配置键示例
- Repository evidence: `README.md:52–53`。
- Dependencies and consumers: 用户。
- Why now: 文档。
- Contract/signature changes: 文档。
- Input/output and state mapping: 无。
- Error and edge behavior: 两语言同步。
- Standards impact: MC-SCOPE-001。
- Literal rule enforcement: Rule 11。
- Implementation pseudocode:

```markdown
| `egon-cola-tianshu-ai-starter` | Downstream AI starter: usage-feedback reporter, published Skill workspace (atomic materialization, Redis publication subscription, reconcile) and authoring-directory upload with optional auto publish |
（README.zh-CN.md 同行中文：下游 AI starter：使用反馈上报、已发布 Skill 工作区（原子落盘、Redis 发布订阅、对账）与创作目录上传（可选自动发布））
配置示例（yaml 代码块，键与 §15.4 一致）：
egon.cola.component.tianshu.ai.feedback.enabled: true
egon.cola.component.tianshu.ai.skill.workspace: { enabled: true, root: /var/lib/app/skills, subscription-enabled: true, resources: [ { namespaceCode: ns-a, resourceKey: order-skill, resourceId: 123 } ] }
egon.cola.component.tianshu.ai.skill.authoring: { enabled: false, root: /var/lib/app/skill-authoring }
```

- Verification contribution: 人工阅读。
- After this file: 本步完成。

- Validation working directory: 仓库根
- Verification command: `./mvnw -pl egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin -Dtest=DdcAiOptimizationStandardsTest,DdcArchitectureContractTest,DdcProfileParityTest -Dsurefire.failIfNoSpecifiedTests=false test && ./mvnw -pl egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-starter,egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-tianshu-adapter,egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin,egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-ai-starter test`
- Expected result: 门禁通过；四个 Java 模块全部测试通过（不含 Failsafe IT）。
- Failure returns to: 违规所在 Step 的文件。
- Completion criteria: 门禁全绿、手册/README 更新、`scripts/work/` 无残留。
- Rollback: 回退本步 Commit paths。
- Commit paths: egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/test/java/top/egon/cola/component/tianshu/admin/contract/DdcAiOptimizationStandardsTest.java, egon-cola-xingyuan/egon-cola-tianshu/docs/tianshu-ai-migration-runbook.md, egon-cola-xingyuan/egon-cola-tianshu/README.md, egon-cola-xingyuan/egon-cola-tianshu/README.zh-CN.md
- Commit: `test(tianshu): ai optimization standards gate with runbook and readme`

## 8. Test, Validation, and Quality Gates

| Gate/order | Working directory | Command or method | Scope | Expected result | Failure returns to | Requirements/runtime boundary |
| --- | --- | --- | --- | --- | --- | --- |
| RED/GREEN Step 1 | 仓库根 | `./mvnw -pl egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-starter -Dtest=DdcRedisKeysTest,DdcAiSdkContractTest test` | base starter | 先 RED 后 GREEN | Step 1 File 3/5 | `REQ-002/007/008/010`; module |
| RED/GREEN Step 2 | 仓库根 | Step 2 Verification command | adapter | 7 测试类通过 | Step 2 File 2/6/10 | `REQ-002/007/008/010`; module |
| RED/GREEN Step 3–13 | 仓库根 | 各 Step Verification command（`-Dtest=` 选择器） | admin 单元/契约 | 逐步 GREEN | 对应 Step | `REQ-001～005/007～009/011/013/014`; module |
| RED/GREEN Step 14–16 | 仓库根 | 各 Step Verification command | ai-starter | 逐步 GREEN | 对应 Step | `REQ-002/006/007/008/010/011`; module |
| RED/GREEN Step 17 | `egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin-web` | `npm run typecheck && npm run test -- 四个测试文件 && npm run lint` | web | 退出 0 | Step 17 File 3/4 | `REQ-009`; static/module |
| Static/format | 仓库根 | `./mvnw -q -DskipTests -pl 四个 Java 模块 compile`（MapStruct `unmappedTargetPolicy=ERROR`、proto 生成） | 四模块 | 无编译错误 | 所在 Step | All; static |
| Module regression | 仓库根 | Step 18 第二条命令（四模块 `test`） | 四模块 | 全部通过 | 所在 Step | All; module |
| 标准门禁 | 仓库根 | `-Dtest=DdcAiOptimizationStandardsTest,DdcArchitectureContractTest,DdcProfileParityTest` | admin | 通过 | Step 18 | `REQ-014`; static |
| 迁移/集成（用户环境） | 仓库根 | `TIANSHU_AI_PERSISTENCE_IT_ENABLED=true ... ./mvnw -pl egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin -am -Ptianshu-ai-persistence-it verify` | PG + Redis + Outbox | `DdcTianshuDdlIT`、`DdcAiFeedbackPersistenceIT`、`DdcAiOptimizationConcurrencyIT`、`DdcAiOptimizationPublishIT` 通过 | Step 3/11 | `REQ-002～005/007/011`; user-controlled runtime |
| 玉衡联调（用户环境） | 部署环境 | 按手册配置 `TIANSHU_AI_MODEL_*` 后启用通道，观察 `tianshu.ai.judge.runs`/`tianshu.ai.optimization.runs` 指标与运行表 | Admin + 玉衡 | 评分与运行进入终态 | Step 7/8/9 | `REQ-003/004/013`; user-controlled runtime |
| 页签冒烟（用户环境） | 浏览器 | 打开资源编辑页第三页签，保存策略、手动触发、查看反馈 | web + admin | 无控制台错误 | Step 17 | `REQ-009`; manual |
| Plan 自检 | 仓库根 | `python3 .claude/skills/egon-coding-writing-plan/scripts/validate_plan.py docs/egon/plan/2026-10-17-14-12-tianshu-ai-feedback-optimization-implementation.md --strict` | 本文件 | PASS | §7 | `REQ-012`; static |

所有门禁均由既有 Maven/npm/Python 命令覆盖，无需 `scripts/work/` 临时脚本；运行时门禁（PG/Redis/玉衡/浏览器）在用户环境执行，本 Plan 不声称运行时证据。

## 9. Migration, Compatibility, Rollout, and Rollback

- 受管 DDL：唯一新版本 `V20261017_001__tianshu_ai_feedback_optimization.sql`（DDL-201），manifest 追加一条 SHA-256 条目；物理目标 `public.ddc_ai_usage_feedback_t0`、`public.ddc_ai_optimization_policy_t0`、`public.ddc_ai_optimization_run_t0`（1 库 × 1 表，`STANDARD_TENANT_ID`）；脚本全部 `IF NOT EXISTS`，按手册 `validate`→`initialize`，部分失败时修复后重跑同一版本（幂等），`ddl_history` 保留；既有 `V20260922_001`/`V20260928_001` 与 manifest 两条目不变；新表为空无回填。非 archetype 项目，无 `GENERATED` 步骤。
- 生成契约：proto 只追加方法/消息/枚举，`protobuf-maven-plugin` 生成到 `target`；旧客户端不受影响；旧服务端对新方法返回 `UNIMPLEMENTED`，ai-starter 记录 WARN 并禁用对应功能直到重启（Step 15 File 4）。
- 部署顺序：Admin（含 DDL）→ 配置 `admin.ai.*`（通道默认关闭）→ 为 runtime 凭据追加 `AI_REPORT` → 升级下游 adapter/ai-starter → 开启 `feedback.enabled` → 验证玉衡连通后开启 `TIANSHU_AI_OPTIMIZATION_ENABLED=true` → 按资源保存策略（默认 `autoPublish=false`）。
- 功能开关：`admin.ai.optimization.enabled`（模型/作业 Bean）、策略 `enabled/judgeEnabled/autoPublish`、ai-starter `feedback.enabled`、`skill.workspace.enabled/subscription-enabled`、`skill.authoring.enabled` 全部默认 false。
- 兼容：API-001～011、RPC-001/002 wire 与错误码不变；Outbox 既有两通知不变；base starter 既有 Port 不变；`DdcRpcOperation` 既有序号不变；Redisson 条件只增加默认 false 的 OR 分支。
- 回滚：应用回滚后新表保留；Outbox 中未投递的 `tianshu.ai-publication` 消息在旧版本无 handler 时按 Outbox 既有未知 channel 处理（保持 PENDING）；ai-starter 回滚只需移除依赖与配置；策略/运行数据不影响旧代码。
- 前向修复：模型输出异常频繁→先 `judgeEnabled=false` 或策略 `enabled=false`，无需回滚代码。

## 10. Requirement-to-Step Traceability Matrix

| Requirement | Effective Spec section | Steps | Files | Tests/gates | Completion evidence |
| --- | --- | --- | --- | --- | --- |
| `REQ-001` | 主 Spec §3.1、§9.2.16、§16 | Step 10、Step 18 | `DdcAiResourceServiceImpl`（入队点）、既有 11 REST/2 RPC 不变 | `DdcAiVersionServiceTest`、`DdcAiOpenApiContractTest`（既有 11 operation 断言）、`DdcAiRpcContractTest` | 既有测试全绿 + 入队用例 |
| `REQ-002` | §9.2.7、§11.2.1、§9.2.14 | Step 1–6、11、14 | SDK DTO、proto、`ddc_ai_usage_feedback`、`DdcAiFeedbackServiceImpl`、Provider、`DefaultDdcAiUsageReporter` | TEST-001/002/004 | `-Dtest=` 选择器 + Failsafe |
| `REQ-003` | §9.2.12、§9.2.15 | Step 5、7、8 | `DdcAiJudgeScanner`、`DdcDefaultAiJudgeStrategy`、`YuhengLlmModelAdapter` | TEST-005/006/007 | 单测 + IT |
| `REQ-004` | §7.2.1、§9.2.1/§9.2.2、§9.2.13、§10.6、§11.2.2 | Step 3、4、9、11、12 | 策略表、`DdcAiOptimizationStateTransition`、`DdcAiOptimizationServiceImpl`、Optimizer Strategy、API-012/013 | TEST-008/009/010/015/017 | 单测 + IT + MockMvc |
| `REQ-005` | §9.2.3～§9.2.5、§11.2.3 | Step 3、4、9、11、12 | 运行表、`trigger/claimRun/executeRun/reclaimExpiredRuns`、API-014/015/016 | TEST-011/012/013/014/015/020/021 | 单测 + IT + MockMvc |
| `REQ-006` | §7.3、§8.2（ai-starter workspace） | Step 15 | `DdcAiSkillMaterializer`、`DdcAiSkillWorkspaceService` | TEST-024/025 | ai-starter 单测 |
| `REQ-007` | §9.2.8、§9.2.11、§15 | Step 1、2、10、13、15 | `aiTopic`、`DdcAiPublicationEvent`、Notifier/Handler、RPC-004、Listener/Scheduler | TEST-015/022/023 | 单测 + IT |
| `REQ-008` | §9.2.9、§9.2.10 | Step 1、2、13、16 | `DdcAiSyncClient`、RPC-005/006 Provider、`DdcAiSkillAuthoringWatcher`、`DdcAiSkillTreeReader` | TEST-026/027/028 | 单测 |
| `REQ-009` | §9.2.1～§9.2.6、§12 | Step 12、17 | 控制器 6 operation、`AiOptimizationPanel`、`AiFeedbackPanel`、第三页签 | TEST-016～021 | MockMvc + Vitest |
| `REQ-010` | §8.2、§15.4 | Step 1、2、14、15、16 | 新模块、两个 Port、条件装配 | TEST-025、`DdcRpcAutoConfigurationTest` | 装配测试 |
| `REQ-011` | §15.1、§9.2.7 | Step 3、4、5、6、11、15 | 租户列/外键、`DdcAiScopeValidator`、拦截器 namespace、Listener 租户过滤 | TEST-003/004/025 | 单测 + IT |
| `REQ-012` | §5.1 | 全部 Step 的 `Commit paths` | 仅本计划列出的文件 | Plan 校验器 `--strict`；每步 `git add` 限定路径 | 校验 PASS |
| `REQ-013` | §9.2.15、§15.4、§17 | Step 5、7、8、9 | `DdcAiOptimizationProperties.Model`、`YuhengLlmModelAdapter`、`DdcAiModelPort` | TEST-006/030 | 单测 |
| `REQ-014` | §6.2、§10、§13 | Step 1–18（每文件 Standards impact） | 全部新类 | TEST-029/030、`DdcAiOptimizationStandardsTest` | 门禁通过 |

UC-001（上报）→ REQ-002；UC-002（评分）→ REQ-003；UC-003（自动优化）→ REQ-004/005；UC-004/UC-005（配置/查看/触发）→ REQ-009；UC-006（下行）→ REQ-006/007；UC-007（上行）→ REQ-008。

## 11. Risks, Blockers, and User Decisions

| ID | Risk or decision | Impacted Steps/files | Evidence | Owner | Status/action |
| --- | --- | --- | --- | --- | --- |
| `RISK-P01` | 玉衡模型输出不稳定导致 INVALID_OUTPUT 频繁（Spec RISK-201） | Step 8/9 Strategy parse | 严格 JSON 指令、`finishReason=length` 判定、`optimizerInstruction` 可调 | 实施者 | Closed：策略可 PAUSED，页面 rearm；指标告警 |
| `RISK-P02` | 真实 PG/Outbox/玉衡运行未验证（Spec RISK-208） | Step 11 IT、§8 运行时门禁 | IT 进入 Failsafe profile，需用户环境 | 用户 | Closed：§8 明确为 user-controlled runtime，不声称运行时证据 |
| `RISK-P03` | `DdcAiResourceServiceImpl` 构造器变更触发既有测试大面积更新 | Step 10 File 6/7、Step 13 File 5 | 既有三个 Service 测试直接 `new` 构造 | 实施者 | Closed：Step 10/13 Commit paths 已含测试更新 |
| `RISK-P04` | `IdpServiceTokenRequest` 要求 `context` 非空而 Spec 写 `null` | Step 7 File 5 | record 紧凑构造器 `Objects.requireNonNull(context)` | 实施者 | Closed：PLAN-CLAR-011 用 `ServiceTokenContext.PLATFORM`，tenantId 为 null，与 Spec 其余参数一致 |
| `RISK-P05` | 多 Admin 实例并发评估同一资源（Spec RISK-204） | Step 4 XML、Step 9 File 14、Step 11 IT | `FOR UPDATE SKIP LOCKED` + 部分唯一索引 | 实施者 | Closed：TEST-011 IT 覆盖 |
| `RISK-P06` | Redisson 条件改动影响既有装配（Spec RISK-207） | Step 15 File 7 | 仅追加默认 false 的 OR 分支 | 实施者 | Closed：`DdcRedisAutoConfigurationTest` 既有三组真值 + 新用例 |
| `DEC-P01` | 本 Plan 按 Accepted Spec 的 DEC-001～DEC-008 执行，不重开决策 | 全部 | 主 Spec §5.3 | 用户 | Closed |

无 Open 阻塞；Plan 状态 `Ready`（用户 2026-10-17 审核通过）。

## 12. Review and Acceptance

### 12.1 Original requirement fidelity

REQ-001～REQ-014 全部出现在至少一个 Step 的 Requirements 行并在 §10 可追溯；用户原始诉求（增删改查复用、按使用结果自动优化到阈值停止、复杂 skill 下发、自动上传更新双向、在线查看、专用 starter）分别落在 Step 10/18、Step 8–9、Step 15、Step 15–16、Step 12/17、Step 14–16；四项用户决策（DEC-001～004）在 Step 7（玉衡）、Step 9（autoPublish/AWAITING_PUBLISH）、Step 15–16（分目录双向）、Step 13/16（纯文本树、限额不变）逐一体现。排除项（不新增依赖、不改既有 wire、不做二进制 Skill）在 §6.3 与各 Step 的 Contract/signature changes 中保持。

### 12.2 Spec consistency

未重设计：表/列/约束来自 §11.2，接口来自 §9.2，状态机来自 §10.6，模块与包来自 §8.2/§8.3，配置键来自 §15.4，测试来自 §14.3。所有实现推断记录为 PLAN-CLAR-001～014（嵌套回执类、`getVersionHead` 服务方法、合并 Sync 转换器、异常包位置、`@ConvertGroup`、既有测试类扩展、测试 profile 通道关闭、同构文件合并描述、批量 namespace 集合校验、属性登记方式、`ServiceTokenContext.PLATFORM`、实例标识来源、`current` 标记文件而非符号链接、自管调度线程），均为局部、可逆且不改变对外契约。必要性审计（§4.5）未发现 fetch-then-forward 接口：RPC-004 版本头仅用于对账比较，不被转发；API-014 由服务端派生策略/窗口；RPC-005/006 的 `expected_revision` 来自上次回执而非额外读取。

### 12.3 Repository executability

所有路径以 `find`/`sed` 核对（adapter 测试位于 `client/ai/DdcAiRpcContractTest.java`；admin 测试目录 `controller/`、`integration/`、`repository/`、`rpc/provider/`、`security/rpc/`、`service/ai/`、`service/notification/`、`config/`、`contract/`；web `src/api`、`src/pages`）；符号以源码行号引用（`DdcAiScopeValidator` 第 118–160 行、`DdcRpcServerInterceptor` 第 158/246 行、`DdcAiExceptionAdvice` 第 74–83 行、`DdcAiResourceServiceImpl` 第 349/385/492/687/872 行、`A/pom.xml` 第 264–272 行等）。依赖顺序：base starter（Step 1）→ adapter（Step 2）→ admin 持久化（3–4）→ 服务与权限（5）→ RPC 入口（6）→ 模型与作业（7–8）→ 优化（9）→ 事件（10）→ 作业装配与 IT（11）→ REST（12）→ 上行 RPC（13）→ ai-starter（14–16）→ web（17）→ 门禁/文档（18）；每步只写本步 Commit paths，base starter 变更后先 `install` 再编译下游（§6.2）。每文件均给出证据、映射、错误行为与完成后状态；每步给出验证目录、命令与提交边界。

### 12.4 Test and release completeness

每步先写失败测试再实现（Test-first gate 均为 Required，Step 14 的模块骨架为构建前置）；TEST-001～030 全部映射到 Step（§10）；迁移为单一受管 DDL 版本 + manifest，幂等可重跑；兼容性（proto 追加、UNIMPLEMENTED 降级、默认关闭开关、既有 wire 不变）与回滚（新表保留、Outbox 未知 channel 保持 PENDING、移除 starter 依赖）在 §9；可观测性指标与日志键在 Step 7/8/9/11/14/15/16 文件中明确；运行时门禁边界在 §8 标注为用户环境。

### 12.5 Blocking Manual Check

| Check ID | Applicability | Status | Evidence | Finding | Required action/exception |
| --- | --- | --- | --- | --- | --- |
| `MC-ARCH-001` | `Applicable` | `PASS` | §4.1 架构画像：admin 传统三层（`controller/service/service.ai.impl/repository/repository.impl/dao/model.*`），starter/adapter 为技术模块；新模块复制 `http-registration-starter` 结构；无新层 | 单一既有 profile 保持 | None |
| `MC-REUSE-001` | `Applicable` | `PASS` | §4.7 复用账本：MP Starter/`EgonColaRepository`/`EgonColaMapper`、Transactional Outbox、Redis v4 键、HMAC RPC、`IdpServiceOAuth2Client`、`RestClient`、`BaseConverter`、`ValidationUtils`、既有 `replaceDraft/publish`、`DdcTenantContextExecutor`、`DdcRedisTopicSubscription` | 无自造基础设施 | None |
| `MC-DEP-001` | `Applicable` | `PASS` | Step 14 File 1 零新增第三方依赖；admin/base starter/adapter pom 不改依赖；仅 reactor/BOM 登记新模块 | 无依赖引入 | None |
| `MC-NAME-001` | `Applicable` | `PASS` | §5 清单与各 Step Symbols：PO/BO/DTO/Result/Command/Query/PageQuery/Event/Enum/Converter/Repository/Mapper/Service/Scanner/Strategy 后缀；Step 18 门禁 `newTypesUseSemanticSuffixes` | 语义后缀一致 | None |
| `MC-VALID-001` | `Applicable` | `PASS` | Repository/Mapper 接口 `@Validated` + `EgonColaModelValidationGroups`；Command/Query Jakarta 约束；`@ConvertGroup(Save)`；`ValidDurationRange` ConstraintValidator；Service `@Validated` | 每层交接均校验 | None |
| `MC-MODEL-001` | `Applicable` | `PASS` | PO/BO/Command/Query/Result/Properties 为 class + `@Data @NoArgsConstructor @AllArgsConstructor @Accessors(chain=true)` + `@Builder/@SuperBuilder`（PO `@EqualsAndHashCode(callSuper=true)`）；record 仅 `DdcAiJudgeVerdictDTO/DdcAiModelRequestDTO/DdcAiModelResultDTO/DdcAiOptimizationEvaluationDTO/StateDTO/SnapshotDTO/RouteDTO` 等不可变值对象 | 构造规范符合 Rule 3 | None |
| `MC-CONVERT-001` | `Applicable` | `PASS` | PO↔BO 三个 `BaseConverter`；BO→Result/Event/proto 为 `BaseForwardConverter`；`DdcAiEnumConverter` 扩展；`unmappedTargetPolicy=ERROR`；无手工 BeanUtils | MapStruct + Egon 基类全覆盖 | None |
| `MC-LOG-001` | `Applicable` | `PASS` | 所有 Service/Repository Impl/Scanner/Notifier/Handler/Adapter/Strategy/Reporter/Watcher 标 `@Slf4j`；Step 18 门禁 `hasField(type,"log")` | 日志注入完整 | None |
| `MC-BEAN-001` | `Applicable` | `PASS` | `@Service("ddcAiFeedbackService")`、`@Service("ddcAiOptimizationService")`、`@Repository("ddcAi…Repository")`、`@Component("ddc…")`、`@Bean("ddcAi…")`；`@RequiredArgsConstructor(onConstructor_ = @Autowired)` + 字段 `@Qualifier`；配置类 `@Bean` 参数 `@Qualifier`；`lombok.config` 已存在于 `egon-cola-tianshu/` | DI 规范符合 Rule 4 | None |
| `MC-UTIL-001` | `Applicable` | `PASS` | 仅 JDK（NIO、并发、`HttpClient`）、commons-lang3 `StringUtils`、commons-codec `DigestUtils/Hex`、Jackson；Step 18 门禁扫描禁用库 | 工具库合规 | None |
| `MC-JSON-001` | `Applicable` | `PASS` | 持久化枚举 `@EnumValue` + `@JsonValue`，禁止 ordinal；Outbox 信封/模型请求/状态文件均 Jackson `ObjectMapper`（`jacksonObjectMapper`）；严格绑定类列表扩展 | Jackson 单一序列化 | None |
| `MC-TIME-001` | `Applicable` | `PASS` | §4.9 TIME-001～003：`Instant` + `timestamptz(6)`；`Duration` 配置/策略字段（PO 存秒，Converter 显式映射）；RFC3339 `ISO_INSTANT` 转换；`deleted_at` 保持既有 Legacy UTC；前端本地显示 + UTC 原值；Step 18 门禁禁止 `java.util.Date` | 时间语义一致 | None |
| `MC-CONFIG-001` | `Applicable` | `PASS` | Step 5 File 7 三 profile 26 键同构；`DdcAiOptimizationPropertiesTest`/`DdcAiOptimizationStandardsTest` 断言集合相等；ai-starter 元数据由 configuration-processor 生成 | 键一致、值可异 | None |
| `MC-PATTERN-001` | `Applicable` | `PASS` | Complex 流程：Strategy（Judge/Optimizer 注册表）、State（`DdcAiOptimizationStateTransition` 表驱动）、Port/Adapter（`DdcAiModelPort`）；Simple 流程（反馈 accept、落盘）直接实现 | 模式强制且不冗余 | None |
| `MC-SCOPE-001` | `Applicable` | `PASS` | §5 清单 = 各 Step Commit paths 并集；§6.1 不触碰清单；既有方法体/脚本/manifest 条目不改；每步 `git add` 限定路径 | 写作用域受控 | None |
| `MC-TEST-001` | `Applicable` | `PASS` | §8 门禁矩阵；每步 RED→GREEN；TEST-001～030 映射；标准门禁 `DdcAiOptimizationStandardsTest`；运行时门禁标注用户环境 | 验证可执行 | None |
| `MC-BLOCKER-001` | `Applicable` | `PASS` | §11 无 Open；以上 16 行均 PASS | 无未闭合阻塞 | None |

### 12.6 Final verdict

`PASS — Ready for user review`
