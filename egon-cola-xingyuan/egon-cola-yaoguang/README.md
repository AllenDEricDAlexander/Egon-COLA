# Egon COLA Yaoguang

摇光负责企业知识库与 LLM Wiki，当前只有一个可执行模块 `yaoguang-admin`，入口为 `top.egon.cola.component.yaoguang.admin.bootstrap.YaoguangAdminApplication`。沿用原来的 controller → service → repository → DAO/XML 分层、摄取/检索策略和 Wiki 发布策略；知识库、文档、分块、后台任务、Wiki、幂等记录及审计共用一个本地事务管理器。

Yuheng 继续持有渠道/模型配置和 `yuheng-llm-gateway`。摇光生产代码不依赖 `yuheng-admin`、其 Repository 或数据库。测试作用域保留该依赖，用于同一份回归验证迁移后的知识接口及留在 Yuheng 的模型配置接口。

## 接口与前端

继续复用 `egon-cola-yuheng/yuheng-admin-web`，知识页面仍为 `/knowledge`、`/knowledge/:kbId`、`/knowledge/:kbId/wiki/:pageId`。原来的 22 个知识/Wiki API、JSON、operationId、`yuheng:knowledge:*` 权限及 `X-Yuheng-Contract-Version` 保留。Edge 优先将以下路径转发到独立的 `edge-yaoguang` 资源：

- `/api/v1/yuheng/admin/knowledge-bases` 及其子路径；
- `/api/v1/yuheng/admin/knowledge-jobs/**`；
- 新宿主的授权引导 `/api/v1/yaoguang/auth/bootstrap`。

登录页通过关闭集合 `workspace=yaoguang` 选择知识资源；网关和 LLM 配置继续选择 Yuheng。两者使用同一 Edge 会话，各自取得与目标资源 audience 匹配的凭据；切换工作区重新读取权限并清理查询缓存。

部署时须在身份/权限平台登记独立的 Yaoguang Resource Server、`yaoguang-admin` 应用及既有知识权限，配置 Edge 的 `EGON_YUHENG_EDGE_YAOGUANG_RESOURCE_SERVER_ID`、`EGON_YUHENG_EDGE_YAOGUANG_RESOURCE_URI`、A/B issuer client-id 和 client-secret。前端默认 alias 为 `edge-yaoguang`，可用 `VITE_YAOGUANG_OAUTH_REGISTRATION_ID` 覆盖。现有隔离集成脚本的旧平台 manifest 不会自动新增这个资源或进程。

## 模型边界

`yaoguang.knowledge.model-client.base-url` 指向 Yuheng LLM Gateway 根地址。创建/替换知识库先通过 `GET /v1/model-policies?model=...&model=...` 查询至多两个别名的安全策略，再开启本地写事务；服务端按已验证主体检查每个别名的授权，响应不携带供应商地址、渠道明细、密钥或主体列表。

SERVICE token 仍由 `service-token-ref` 引用文件解析，需要目标 LLM 资源的 `yuheng:llm:read` 与 `yuheng:llm:invoke` scope，且对应模型须授权该 SERVICE 主体。聊天/向量仍调用原生 `/v1/chat/completions`、`/v1/embeddings`。出站 `X-Egon-Llm-Egress-Policy` 只能收紧已有路由：嵌入固定 `LOCAL_ONLY`，聊天遵循知识库策略，不改写原生请求 JSON。

## 空库初始化与配置

本次是未上线代码拆分，不迁移旧数据。Yuheng 的受管空库基线现为 42 张业务表；Yaoguang 使用独立数据库及 `yaoguang_0/public`，由 `yaoguang-admin` 独占执行 `src/main/resources/db/egon-mp/20261020_001_yaoguang_schema.sql`。九张业务表均使用 `yaoguang_` 前缀，另有 runner 技术表 `ddl_history`；保留租户、软删、版本、幂等唯一键及 Wiki/文档修订指针的延期外键。Yaoguang 库须预装 pgvector。既有 Flyway 文件没有修改。

配置见 `yaoguang-admin/src/main/resources/application.yml`，包含完整的九表单库路由。必填部署身份为 `YAOGUANG_PERSISTENCE_TENANT_ID`、`YAOGUANG_PERSISTENCE_IDENTITY_TENANT_ID`、`YAOGUANG_PERSISTENCE_SERVICE_USER_ID`、`YAOGUANG_PERSISTENCE_EXPECTED_SCHEMA_VERSION`、`YAOGUANG_PERSISTENCE_EXPECTED_SCHEMA_SHA256`，版本/摘要必须与同目录 manifest 一致。数据库使用 `YAOGUANG_DATABASE_URL`、`YAOGUANG_DATABASE_USERNAME`、`YAOGUANG_DATABASE_PASSWORD`，TLS、身份校验、撤销与审计配置也必须提供；缺少部署身份时不使用默认租户。

默认端口为 8090。知识与 worker 默认关闭，使用 `YAOGUANG_KNOWLEDGE_ENABLED`、`YAOGUANG_KNOWLEDGE_WORKER_ENABLED` 开启；模型根地址及凭据文件引用分别为 `YAOGUANG_LLM_BASE_URL`、`YAOGUANG_LLM_SERVICE_TOKEN_REF`。其他知识参数继续使用 `yaoguang.knowledge.*`。受管 DDL 的 schema 和摘要已改变，旧测试库需要由操作者重建；这里没有删库或修复历史记录的自动操作。

## 构建与验证

在仓库根目录构建：

```shell
./mvnw -pl egon-cola-xingyuan/egon-cola-yaoguang/yaoguang-admin -am -DskipTests package
```

输出为 `yaoguang-admin/target/yaoguang-admin-exec.jar`。测试覆盖静态 schema/manifest、原知识与 Wiki 合同、摄取/检索/发布行为、模型策略与本地事务顺序；实际 PostgreSQL 初始化、身份资源登记及完整部署联调仍需在配置完成后验证。构建和单元验证不会自动启动应用。
