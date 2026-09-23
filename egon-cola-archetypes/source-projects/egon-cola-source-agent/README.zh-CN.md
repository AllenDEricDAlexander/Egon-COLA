# egon-cola-source-agent

[English](README.md) | [中文](README.zh-CN.md)

这是 `egon-cola-archetype-agent` 的内部 Deep Research source reactor。它采用 Web non-open 六模块结构，保留两个业务域：进程内的 `research`，以及把文档存入 PostgreSQL、做嵌入并据此问答的 `knowledge`。source project 只是构建输入，不是发布到 Central 的公共 artifact。

```text
common -> domain -> application -> adapter -> starter
             \-> infrastructure -> starter
```

项目通过 `egon-cola-component-agent-flow-starter` 消费通用 Agent Flow 能力，并通过 RAG、transactional-outbox 与 MyBatis-Plus 组件支撑 knowledge 域。provider 配置、MCP 凭据、数据库凭据和 API secret 由 Starter 的环境配置提供。除 knowledge 三张表之外，项目不包含 MQ、RPC、GraphQL、UI、历史、恢复 API；二级缓存已接线但尚无查询接入。

source reactor 使用 Java 21、Spring Boot 3.5.16、Spring AI 1.1.8、Springdoc 2.8.17，并通过 Components starter 使用 Google ADK 0.7.0。只有生成后的 `egon-cola-archetype-agent` definition 会成为公共 Archetype archetype。

## Maven Profiles 与外部启动参数

每次选择 `dev`、`test`、`prod` 中一个环境 profile，未指定时使用 `dev` 默认值。
这些 profile 控制应用启动，Surefire 仍使用原有 `test` 配置。堆内存参数是示例值，部署时按资源调整。

在项目根目录执行，多模块项目先安装兄弟模块依赖：

```bash
mvn install
mvn -pl egon-cola-source-agent-starter -Pdev spring-boot:run
mvn -Pprod -Drun.jvm-args="-Xms1g -Xmx2g" \
  -Drun.server-port=8080 \
  -Drun.config-location=file:/etc/myapp/override.yml package
```

Maven 在 `process-resources` 阶段生成 `egon-cola-source-agent-starter/target/launch.args`，打包时也会生成。
切换 profile 或 `-Drun.*` 参数后，无须 `clean` 即可更新文件。此执行仅使用 `@...@`
过滤 `src/main/launch/launch.args`，现有 YAML 占位符继续在运行时解析，参数文件不进入 JAR。

将可执行 JAR 与 `launch.args` 一起部署，JAR 可重命名为 `app.jar`，然后在部署目录执行：

```bash
java @launch.args -jar app.jar
java @launch.args -Xmx3g -jar app.jar --server.port=9080
```

文件记录 JVM 参数、Spring profile、端口及额外配置位置。这些显式系统属性优先于对应环境变量；
`-Drun.*` 覆盖 Maven 默认值，JVM 覆盖参数放在 `-jar` 前，Spring 命令行覆盖参数放在 JAR 后。
单独 `java -jar` 或 IDE 直接运行 main 不会自动读取该文件。相对配置路径以启动工作目录为准，
并非参数文件所在目录；部署建议使用绝对 `file:` 路径，Windows 路径使用正斜杠。
额外文件补充 `application.yml` 和选中的 `application-{profile}.yml`，不替换默认配置位置；
要求文件必须存在时去掉 `optional:`。密码和密钥继续通过现有环境变量/secrets 注入，不写入 `run.*`。

## 运行合同

唯一公共操作是 `POST /api/v1/deep-research/runs`，请求为 JSON，响应为 `text/event-stream`。请求体包含 `topic`、可选 `reportLanguage`（`ZH_CN` 或 `EN_US`，默认 `ZH_CN`）以及可选 `maxSources`（默认 `8`，范围 `3..20`）。必须提供 `X-Research-Api-Key`；`X-Trace-Id` 可选，缺失时由服务端生成。

每个通过校验的请求创建一个进程内 Agent Flow Session 和一次非幂等 run。事件按 `runId:sequence` 排序，名称为 `research.started`、`research.progress`、`research.completed` 或 `research.failed`；completed/failed 是唯一公共终态。重连会创建新的 run，不提供 status、history、resume 或持久化恢复 API。客户端断开时会取消 run，并释放 Session 与容量许可。

固定工作流为 `Planner -> ParallelResearch -> Writer`；并行节点包含 `EvidenceResearcher`、`CounterpointResearcher`、`FreshnessResearcher`。搜索回调来自一个配置的 MCP SSE 服务。请求不能选择模型、MCP 地址、密钥、工具或 prompt。

## 知识库接口

knowledge 域在 `/api/v1/knowledge-bases` 与 `/api/v1/knowledge-documents` 下新增 12 个操作。它们与研究域共用同一个 `X-Research-Api-Key`（`X-Trace-Id` 同样可选），失败时返回同一个 JSON 错误体——包括成功响应是事件流的问答接口。

| 方法与路径 | 用途 |
| --- | --- |
| `POST /api/v1/knowledge-bases` | 创建知识库：code、name、嵌入模型、分块策略 |
| `GET /api/v1/knowledge-bases` | 分页查询知识库 |
| `GET /api/v1/knowledge-bases/{knowledgeBaseId}` | 查询单个知识库 |
| `PUT /api/v1/knowledge-bases/{knowledgeBaseId}` | 更新名称、描述或分块策略 |
| `DELETE /api/v1/knowledge-bases/{knowledgeBaseId}` | 删除知识库及其文档与分块 |
| `POST /api/v1/knowledge-bases/{knowledgeBaseId}/documents` | 以 `multipart/form-data` 上传文件（`file`，可选 `displayName`） |
| `GET /api/v1/knowledge-bases/{knowledgeBaseId}/documents` | 分页并按条件筛选文档 |
| `GET /api/v1/knowledge-documents/{documentId}` | 查询单个文档的状态与诊断信息 |
| `DELETE /api/v1/knowledge-documents/{documentId}` | 删除文档及其文本与分块 |
| `POST /api/v1/knowledge-documents/{documentId}/reingest` | 用已落库文本再次入队 |
| `POST /api/v1/knowledge-bases/{knowledgeBaseId}/retrieve` | 嵌入查询并返回带分数的分块；不调用对话模型 |
| `POST /api/v1/knowledge-bases/{knowledgeBaseId}/chat` | 以 SSE 流式返回一次回答 |

`GET /v3/api-docs` 发布每个操作的请求与响应 schema，字段约束以它为准。有两条 schema 表达不出的行为在此写明：问答接口必须声明 `Accept: text/event-stream`，否则返回 `406`；请求不能指定 collection、嵌入模型或租户——读写哪个集合由知识库自身存储的模型决定。

检索入参为 `query`（必填、trim、至多 2000 字符）、`topK`（1 到 50，缺省取组件配置的默认值 8）与 `similarityThreshold`（0.0 到 1.0）。问答入参为 `question`（必填、trim、至多 2000 字符、不含控制字符）与 `topK`，事件依次为携带引用列表的 `knowledge.started`、零到多个携带 `delta` 的 `knowledge.progress`，以及恰好一个 `knowledge.completed` 或 `knowledge.failed` 作为最后一个事件。上传进度通过轮询文档详情观察。

## 摄取状态机

`DocumentIngestStatusEnum` 与一次摄取任务的 outbox 状态一一映射；下表即状态机，未列出的迁移一律拒绝。

| 当前状态 | 事件 | 下一状态 | 守卫 |
| --- | --- | --- | --- |
| （不存在） | 上传事务提交 | `PENDING` | 文件已存储、文本已落库、新增 outbox 行 |
| `PENDING` | 任务被领取 | `PROCESSING` | 存在待处理记录 |
| `PROCESSING` | 摄取成功 | `SUCCEEDED` | 向量已写入，回写 `chunkCount` |
| `PROCESSING` | 摄取失败且仍有额度 | `PENDING` | `attempt_count < max_attempts`；outbox 消息进入 `RETRY_WAIT` |
| `PROCESSING` | 摄取失败且额度耗尽 | `DEAD` | 无剩余额度；消息成为死信 |
| `SUCCEEDED`、`FAILED`、`DEAD` | 重新处理（`reingest`） | `PENDING` | 文本非空；清空失败字段、重置尝试计数 |

非终态文档的重新处理返回 `409`。重试的投递不会重新解析原文：它重新切块并重新嵌入已落在 `knowledge_document.content` 中的文本。

## 知识库配置

| 键 | 环境变量 | 含义 |
| --- | --- | --- |
| `spring.datasource.url` / `username` / `password` | `AGENT_DB_URL`、`AGENT_DB_USERNAME`、`AGENT_DB_PASSWORD` | PostgreSQL 数据库；`dev`/`prod` 必填 |
| `spring.flyway.*` | `AGENT_FLYWAY_ENABLED` | 当前真正生效的建库路径：`application.yml`、`dev` 与 `prod` 默认为 `true`，locations 为 `classpath:db/migration`，因此 knowledge 启动会执行三条 `V2026*` 脚本；`test` 强制 `false`。只能通过获批的 MP-SDJ DDL 迁移替换，不作为部署开关 |
| `spring.servlet.multipart.max-file-size` / `max-request-size` | `AGENT_MULTIPART_MAX_FILE_SIZE`、`AGENT_MULTIPART_MAX_REQUEST_SIZE` | 高于接口上限的容器防线（默认 25MB / 26MB） |
| `egon.cola.component.rag.*` | `AGENT_RAG_STORAGE_ROOT` | 维度、具名向量库与嵌入模型 Bean、存储根目录、检索上限（默认 8，最大 50） |
| `egon.cola.component.transactional-outbox.enabled` | — | 摄取队列及其投递 |
| `egon.cola.component.mybatis-plus.tenant-id` | — | 租户列，由 MDC 在每条语句上填充 |
| `agent.knowledge.embedding.base-url` / `api-key` / `model-name` | `AGENT_KNOWLEDGE_EMBEDDING_BASE_URL`、`AGENT_KNOWLEDGE_EMBEDDING_API_KEY`、`AGENT_KNOWLEDGE_EMBEDDING_MODEL` | 嵌入供应商；`dev`/`prod` 必填 |
| `agent.knowledge.tenant.default-id` | `AGENT_KNOWLEDGE_TENANT_ID` | 所有调用使用的租户，默认 `0` |
| `agent.knowledge.runtime.qa-max-concurrent` | `AGENT_KNOWLEDGE_QA_MAX_CONCURRENT` | 并发问答流数量（默认 4） |
| `agent.knowledge.runtime.max-upload-bytes` | `AGENT_KNOWLEDGE_MAX_UPLOAD_BYTES` | 接口执行的实际上传上限（默认 20MB） |
| `agent.knowledge.runtime.max-documents-per-base` | `AGENT_KNOWLEDGE_MAX_DOCUMENTS_PER_BASE` | 单个知识库可容纳的文档数（默认 10000） |
| `agent.knowledge.runtime.qa-max-duration` | `AGENT_KNOWLEDGE_QA_MAX_DURATION` | 单次问答的最长流式时长（默认 PT2M） |

knowledge 域部署有四个配置键无法满足的前提：

1. 数据库必须是 PostgreSQL，且应用角色可执行 `CREATE EXTENSION vector`——第一条迁移就会执行它。
2. 向量表由 `knowledgeRagVectorStore` Bean 在首次启动时创建，不由迁移管理，因此表维度不可能与组件校验过的嵌入模型不一致。
3. 租户是固定值（`agent.knowledge.tenant.default-id`，默认 `0`），**不构成隔离边界**；鉴权与租户解析不在本 archetype 范围内。
4. 知识问答与研究 run 共用同一个 API Key 与同一套进程内容量模型，但各有独立容量池：一方饱和不会拒绝另一方，进程重启后两者都不会保留。

## 环境配置合同

开发和生产环境必须从外部提供 `DEEP_RESEARCH_MODEL_BASE_URL`、`DEEP_RESEARCH_MODEL_API_KEY`、`DEEP_RESEARCH_MODEL_NAME`、`DEEP_RESEARCH_API_KEY`、`DEEP_RESEARCH_MCP_BASE_URI`、`DEEP_RESEARCH_MCP_SSE_ENDPOINT` 与 `DEEP_RESEARCH_MCP_API_KEY`，knowledge 域还须提供上表中的 `AGENT_DB_*` 与 `AGENT_KNOWLEDGE_EMBEDDING_*`。运行限制使用 `DEEP_RESEARCH_MAX_*`、`DEEP_RESEARCH_HEARTBEAT_INTERVAL` 与 `AGENT_KNOWLEDGE_*`。source 中不提交凭据、供应商 URL 或可用 secret；test profile 使用 fake `ChatModel` 与 fake `ToolCallback`、关闭 RAG 组件且不执行 Flyway，因此既不连接网络也不访问 PostgreSQL。

请求示例（shell 从外部 secret 环境展开 key）：

```bash
curl --fail-with-body --no-buffer \
  -H "X-Research-Api-Key: ${DEEP_RESEARCH_API_KEY}" \
  -H "Accept: text/event-stream" \
  -H "Content-Type: application/json" \
  -d '{"topic":"agent architecture","reportLanguage":"EN_US","maxSources":8}' \
  http://localhost:8080/api/v1/deep-research/runs
```

`reportMarkdown` 是不可信的模型输出，渲染前必须清理。服务端不会重试付费模型或 MCP 调用；重试会产生新的计费 run。进程重启后进程内 Session 与报告都会消失。生产环境关闭 API 文档；TLS、代理头与 secret 投递由部署负责。

## 校验

不启动服务即可运行 source 校验：

```bash
./mvnw -B -ntp clean verify
```

生成的 archetype 由仓库 archetype 脚本独立验证。该 source project 不包含消息中间件、RPC、GraphQL endpoint 或 UI；其持久面是 knowledge 的库表、迁移与 outbox 队列，以及已接线但尚无查询接入的二级缓存。

## 知识库 Repository 迁移

知识库查询统一为绑定参数的 Mapper XML。更新同时限制租户、活动状态、期望版本与入库状态；`deleted_at` 使用可空 UTC LocalDateTime，`version` 从 0 递增。JSONB 保留字段专用 handler。

统一规范改为 MP-SDJ 分布式受管 DDL，但本 source project 尚未到达该状态：实际执行的是 Flyway。infrastructure 依赖里有 `flyway-core` 与 `flyway-database-postgresql`，`spring.flyway.locations` 指向 `classpath:db/migration`，且 `dev`/`prod` 保持 `AGENT_FLYWAY_ENABLED` 的 `true` 默认值，因此三条 `V2026*` 脚本（含 `create extension if not exists vector`）会在启动时执行。Common DDL 另被关闭（`egon.cola.component.mybatis-plus.ddl.enabled: false`），本项目也没有 `db/egon-mp` SQL 与 `repository-manifest.json`，即受管 DDL 目前无可执行目标。这些是迁移差异而非目标选项。后续代码迁移需接入组件 Runner、Manifest 与物理目标，保持已有 SQL/数据并核对 Outbox/向量表归属；本次文档更新没有执行该运行时迁移。

生产配置 `EGON_ID_MACHINE_ID`。默认 H2/fake-model 测试不执行 PostgreSQL 迁移或向量集成，需在专用数据库上手动验收。

## 二级缓存

`application.yml`、`application-dev.yml`、`application-prod.yml` 均以 `egon.cola.component.cache.enabled=true` 交付；`test` 保留同样的键但设为 `enabled: false`，让单元与模块测试不依赖 Redis。`infrastructure/config/RedisConfig.java` 已带 `@EnableCaching` 并发布 `redissonClient`，配置好 Redis 连接即可直接使用二级缓存。mp-sd-ext 基类已移除缓存端口耦合，具体
Repository 通过 `@CacheConfig`、`@Cacheable`、`@CacheEvict` 等注解声明策略；当前没有任何 knowledge 查询被注解，因此打开开关本身不会改变行为。普通 CRUD
不再隐式失效缓存，一旦有读路径接入，其写入和删除入口也必须声明失效。所有 profile 都声明同一组五个 TTL 键（`l1-expire`、`l1-jitter`、`l2-expire`、`l2-jitter`、`null-expire`）以及共享的 `key-prefix`、`tenant-mdc-key` 与批量/锁预算，只有取值随环境不同。Key、条件、组合操作、事务与同步加载限制详见 Egon-COLA 仓库中的 `egon-cola-component-common-cache-spring-boot-starter` 文档。
