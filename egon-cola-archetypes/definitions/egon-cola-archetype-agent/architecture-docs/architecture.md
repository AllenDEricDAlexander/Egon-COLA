# agent 脚手架架构

更新：2026-10-17。以 `egon-cola-source-agent` 的 POM、Java 和架构测试为事实来源。Agent 六模块工程，承载 Deep Research 与 Knowledge 两项业务能力，不创建 facade 模块。

## 项目与模块

生成工程有两项业务能力：Deep Research 完全在进程内运行；Knowledge 保存文档、把它们嵌入启用了 `vector` 扩展的 PostgreSQL，并基于已存内容回答问题。除知识库表外，项目没有消息中间件、RPC、GraphQL、UI、状态查询、历史、续跑或持久化恢复面；二级缓存 starter 已接线，但目前没有任何查询启用缓存。

```text
common <- domain <- application <- adapter <- starter
                         ^             ^
                         |             |
                    infrastructure --+
```

`domain` 持有研究记录、事件、端口与服务契约，以及知识库词汇：知识库、文档、分块、摄取生命周期，和检索 / 回答端口。`application` 持有 `DeepResearchManage` 门面、校验、进程内容量隔板、截止时间与终态清理，以及知识库用例。`infrastructure` 基于共享的 Agent Flow 组件实现领域服务端口，持久化知识库表，并装配嵌入模型、向量库和摄取投递处理器。`adapter` 持有 trace 过滤器与知识库 tenant MDC 过滤器、JSON 边界、错误契约、唯一的 `POST /api/v1/deep-research/runs` SSE 命令，以及 `/api/v1/knowledge-bases` 与 `/api/v1/knowledge-documents` 下的 12 个知识库操作。`starter` 持有 Spring Boot、Spring AI 模型配置、OpenAPI 元数据、固定的 flow 图、数据源、迁移，以及 native HTTP 安全链。

## 当前源码定位

以下入口相对正常源工程 `egon-cola-source-agent`；生成工程替换项目/包名前缀。修改前重查源码，不按旧文档猜类名。

| 责任 | 源码入口 |
| --- | --- |
| Domain 业务端口 | `egon-cola-source-agent-domain/src/main/java/top/egon/cola/archetype/source/agent/domain/research/service/DeepResearchAgentService.java` |
| Domain 业务端口 | `egon-cola-source-agent-domain/src/main/java/top/egon/cola/archetype/source/agent/domain/knowledge/service/KnowledgeAnswerService.java` |
| Domain Repository 端口 | `egon-cola-source-agent-domain/src/main/java/top/egon/cola/archetype/source/agent/domain/knowledge/repository/KnowledgeBaseRepository.java` |
| Application 用例 | `egon-cola-source-agent-application/src/main/java/top/egon/cola/archetype/source/agent/application/research/manage/DeepResearchManage.java` |
| Application 用例 | `egon-cola-source-agent-application/src/main/java/top/egon/cola/archetype/source/agent/application/knowledge/manage/KnowledgeBaseManage.java` |
| Application 用例 | `egon-cola-source-agent-application/src/main/java/top/egon/cola/archetype/source/agent/application/knowledge/manage/KnowledgeQaManage.java` |
| Infrastructure Repository | `egon-cola-source-agent-infrastructure/src/main/java/top/egon/cola/archetype/source/agent/infrastructure/knowledge/service/KnowledgeBaseRepositoryImpl.java` |
| Infrastructure Repository | `egon-cola-source-agent-infrastructure/src/main/java/top/egon/cola/archetype/source/agent/infrastructure/knowledge/service/KnowledgeDocumentRepositoryImpl.java` |
| Adapter 入口 | `egon-cola-source-agent-adapter/src/main/java/top/egon/cola/archetype/source/agent/adapter/research/controller/DeepResearchController.java` |
| 安全装配 | `egon-cola-source-agent-starter/src/main/java/top/egon/cola/archetype/source/agent/starter/config/NativeHttpSecurityConfiguration.java` |
| 架构校验 | `egon-cola-source-agent-starter/src/test/java/top/egon/cola/archetype/source/agent/architecture/AgentArchitectureTest.java` |

此表是定位入口，不是全部用例清单；完整业务契约与运行配置见生成工程 README 和对应源码/测试。

## 分层与协议

研究请求不能选择 provider、模型、工具、提示词或 flow；知识库请求不能选择 collection、嵌入模型或 tenant。每个被接受的研究请求获得一个隔离的 Agent Flow session 和一个进程内 run。事件按 `runId:sequence` 排序；`research.completed` 或 `research.failed` 是唯一的公开终态事件，知识库回答流同样以 `knowledge.completed` 或 `knowledge.failed` 结束。客户端断开会取消该 run 或回答，并释放 session、订阅、容量租约以及回答的生成过程。模型、MCP 与文档的输出都不可信；报告 Markdown 和检索出的文本在渲染前必须先做净化。

认证由 starter 中的 native USER 边界负责：业务操作要求星垣签发的 Bearer 及其精确 scope 与鉴神资源权限，具体规则见生成工程 README 的「星垣身份与授权」一节。`adapter/filter/ResearchApiKeyFilter` 是标记为 `@Deprecated` 的隔离遗留 fixture，没有被注册，不是认证入口。

## 持久化与数据库

知识库领域持有三张表——`knowledge_base`、`knowledge_document` 与 outbox 队列 `egon_cola_outbox_message`——它们历史上由本工程的遗留迁移创建。目标规范现在是 MP-SDJ 分布式受管 DDL；现有 POM/配置仍需一次实现层面的迁移，当前实际运行的仍是 Flyway。向量表不在其中：`knowledgeRagVectorStore` bean 在首次启动时按组件校验过的嵌入模型维度创建它，因此不存在与向量库不一致的迁移。

两张知识库表都带 tenant 列。tenant 与操作者都取自已验证的 USER 身份，由 `KnowledgeTenantMdcFilter` 写入 MP 的 tenant 与审计 MDC，并在请求结束后恢复；缺少已验证身份时返回 401，不存在默认 tenant 的配置项。部署时需要一个允许应用角色执行 `CREATE EXTENSION vector` 的 PostgreSQL 数据库，以及四个 profile 声明的 `spring.datasource` 配置键。

支持软删除的业务唯一键必须由业务列 + deleted_at 组成，并强制有效行 NULL 语义；现有 SQL 需要在任何纠正迁移之前单独审查。已应用的 SQL/history 不可改写，不允许新增 Flyway 集成。

## Java 对象与校验

普通实体用 class；只有不可变值对象可用 record。普通构造选 @Builder，兼容的继承构造选 @SuperBuilder，不能机械地互相替换。持久化枚举用 @EnumValue，前端枚举用 @JsonValue。校验基于注解驱动，复用 Components 与 Common MP repository。

## CQE 与组件复用

Agent 保留基于 Spring AI 1.1.8、Agent Flow 与 Google ADK 的六模块 Deep Research 应用，并为知识库领域消费 RAG、outbox 与 MyBatis-Plus 组件；复用现有的 Knowledge 事务 outbox。它不引入 Egon RPC/Tianshu、Nacos、Dubbo 或 ShardingSphere 运行时依赖。Google ADK 可能独立带入 gRPC/Protobuf；仅由 BOM 管理不等于运行时依赖。研究 / 回答 SSE 是响应流，不能证明 CQE 业务事件已持久投递。

生成工程继承已发布的 `top.egon:egon-cola-archetypes-parent`，使用具体版本且 `relativePath` 留空。parent 导入 Components BOM，管理 Common 依赖与 ShardingSphere 5.5.3，并把 Commons Lang 固定在 3.20.0；各消费模块继承自己工程的根 POM。验证未发布版本前，先在本地安装对应的 parent/BOM 与所需工件；本地安装不等于发布。

## 验证边界

正常源工程是业务事实来源；本 definition 只是 Maven 打包契约，`.generated` 是被忽略的派生 reactor。生成工程必须在离线环境下用假的模型 / 工具依赖验证，不得从 archetype 获得凭据或特定 provider 的端点。

## 分布式 DDL 管理约束

统一复用 `egon-mp-sdj-ext-starter` 对应的 `egon-cola-component-common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter`，不再选用 Flyway。使用组件 `EgonColaPostgreDdlRunner`、显式物理 PRIMARY/schema/role 目标、SQL 版本与 SHA-256 Manifest；数据源与拓扑由 Starter 管理，业务项目不得复制本地 Bootstrapper 或并行使用 MP 默认 IDdl/DdlApplicationRunner。

schema advisory transaction lock 协调多实例；每份 SQL 与 ddl_history 同连接同事务提交。跨物理目标不是全局原子事务，后续目标失败不撤销前面已提交目标；必须设计续跑与幂等。未知提交结果先用新连接核实再决定重试。检查锁/语句/拓扑超时、脚本前缀、checksum 与路由指纹；非空未受管库或漂移不能自动 DROP、repair 或接管历史。

一个逻辑变更新增一个下一版本 SQL 和 Manifest 条目，不改写已应用 SQL/history。配置基于 `egon.cola.component.mybatis-plus.ddl` 与现有 profile；实际接线及真实 PostgreSQL 并发/失败恢复需要独立验证，文档不声称已完成运行时迁移。
