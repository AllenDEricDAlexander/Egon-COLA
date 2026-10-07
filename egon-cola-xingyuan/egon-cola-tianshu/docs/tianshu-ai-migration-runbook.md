# Tianshu Prompt/Skill 上线与迁移手册

本文说明 Prompt 与 Skill 管理功能的租户配置、跨 namespace 发布、数据库初始化、V9 数据导入和受控验收。Admin 不会在启动时创建表或导入数据；所有数据库操作都由显式 CLI 或显式集成测试完成。

## 上线前准备

为每个业务租户在 Tianshu Admin 登记唯一的正数 `tenantId`、身份租户标识和启用状态，并为需要访问的 RPC/REST 身份配置相应 namespace 范围。Prompt/Skill 的身份和权限始终来自已验证请求上下文，不能从资源请求体或缺省值推导。

每个直接使用 Tianshu Starter 的应用配置真实租户 ID 与 Redis ACL 用户名：

```yaml
egon:
  cola:
    component:
      tianshu:
        tenant-id: ${TIANSHU_TENANT_ID:}
        redis:
          username: ${TIANSHU_REDIS_USERNAME:}
        prompt:
          enabled: false
        skill:
          enabled: false
```

空租户 ID 不会回退到 `0` 或 `1`；启用 Redis 时 ACL 用户名必须非空。生产 Redis ACL 用户只允许访问其租户的 `tianshu:v4:tenant:<tenantId>:{...}:*` 键和对应 Pub/Sub channel。将 Redis 密码放在现有 secret 注入通道中，不放在仓库配置或命令行里。

SDK 仅需某一类资源时，可分别设置 `egon.cola.component.tianshu.prompt.enabled=true` 或 `skill.enabled=true`；未配置时两者均关闭。相应 RPC runtime 凭据需有只读 `AI_READ` 权限，管理页面则继续使用平台身份与既有 Tianshu AI namespace 权限。不要为客户端授予管理写权限来读取 Prompt/Skill。

## 管理与跨 namespace 发布

Admin Web 的 AI 资源页面提供 Prompt 文本编辑和 Skill 树形文件编辑。Skill 节点内容作为 UTF-8 文本保存；不会按 Markdown、图片或其他文件类型提供专用渲染。发布后版本是不可变快照，消费者固定读取时同时保存 `resourceId` 与 `versionNo`。

复制发布只允许在同一个 tenant 内进行。先在源 biz/namespace/env/app 中调试并发布版本，再选择明确的目标四维范围；服务端根据 `sourceResourceId + sourceVersionNo` 读取快照并创建目标版本，保留来源版本关系。现存目标草稿不被覆盖，发布结果不会清空目标编辑缓冲。跨 tenant 复制不支持。

删除资源采用软删除。旧版本仍保留为审计历史；同 key 可创建新的资源 ID。已删除根 ID 的固定版本读取不会自动切换到同 key 的新根。需要回滚时从版本历史选择既有 `resourceId + versionNo`，不要通过复活已删除根来恢复。

## PostgreSQL DDL 初始化

Tianshu Admin 只读检查受管 DDL manifest 和 `ddl_history`；生产运行关闭自动 DDL。发布前先将目标 PostgreSQL 备份，并将受保护的目标 YAML 配置文件放在权限为 `0600` 的位置。文件提供当前 Tianshu MP topology、真实 datasource、Outbox 同 datasource/transaction manager 配置及机器 ID；数据库 URL、用户名和密码从进程环境或专用 secret 注入。

先对目标执行 `validate`。核对 database/schema、route fingerprint 和 manifest 完全匹配后，才显式执行 `initialize`：

```bash
./mvnw -pl :egon-cola-tianshu-admin -DskipTests \
  spring-boot:run \
  -Dspring-boot.run.main-class=top.egon.cola.component.tianshu.admin.migration.DdcPersistenceMigrationCli \
  -Dspring-boot.run.arguments="validate --target-config /secure/tianshu-target.yml"

./mvnw -pl :egon-cola-tianshu-admin -DskipTests \
  spring-boot:run \
  -Dspring-boot.run.main-class=top.egon.cola.component.tianshu.admin.migration.DdcPersistenceMigrationCli \
  -Dspring-boot.run.arguments="initialize --target-config /secure/tianshu-target.yml"
```

CLI 使用 manifest 校验文件 checksum、真实 topology 与 PRIMARY 目标，并按版本登记 `public.ddl_history`。同一个目标重复初始化会验证已安装前缀并跳过已核验版本；checksum、route fingerprint 或版本前缀漂移时会失败关闭。DDL 提交响应不确定时 CLI 会重新读取 history 核验；无法证明已提交就报告 `DDL_COMMIT_UNKNOWN`，不要手工补写 history 或重跑改写过的 SQL。Outbox 技术 schema 与 Tianshu `public.ddl_history` 由各自 manifest 管理，不能混用同名 history。

日常应用启动只验证 DDL 已就绪。它不会替代上述显式初始化，也不会在启动时自动修复 schema。已应用 SQL 不得编辑；需要修正时新增一个向前 DDL 版本，并重新生成/审核唯一 manifest。

## V9 导入、核验与切流

源端必须是只读冻结的 V9 SQLite/PostgreSQL 备份，目标必须是已初始化且与源端 database identity 不重叠的专用 PostgreSQL 数据库。导入 mapping 使用受信 Ed25519 key 签名，目标配置声明可信公钥、租户映射、source history checksum 和目标 route fingerprint。mapping、source/target 配置和 report 目录不得位于公开或共享可写目录。

按顺序执行 `validate`、`import`、`verify`，每次使用相同 job ID、签名 mapping 和受保护的 source/target 配置。CLI 以最多 1000 行一批提交并维护 owner-only checkpoint/report；目标中已有的精确行会核验后跳过，目标冲突会停止。遇到错误先审阅 per-table 报告和 source ID/hash，不要清理或重写已提交批次。保留源库只读，直到业务方完成行数、ID、tenant、外键、版本/状态及字段 hash 的最终比对并明确切流。

数据库导入只迁移 V9 已有配置中心数据，不会从旧 YAML 自动构造 Prompt 或 Skill 版本。AI 资源需在管理页面重新创建并发布。

回滚边界：切流前停写并保留源库；切流后数据库 schema 采用向前修复，不能编辑旧 SQL 或恢复成不匹配的 history。业务恢复使用 Tianshu 固定版本回滚操作。若导入已经提交部分批次，通过修正后的 mapping/job 对目标做核验或安排向前补偿，不执行未经审核的物理删表。

## 受控集成验收

普通测试不会连接 PostgreSQL/Redis。只有显式启用 Failsafe profile 且提供隔离外部目标时，才会执行外部集成测试。至少配置：

```text
TIANSHU_AI_PERSISTENCE_IT_ENABLED=true
TIANSHU_AI_PERSISTENCE_IT_JDBC_URL
TIANSHU_AI_PERSISTENCE_IT_DB_USERNAME
TIANSHU_AI_PERSISTENCE_IT_DB_PASSWORD
TIANSHU_AI_PERSISTENCE_IT_REDIS_ENABLED=true
TIANSHU_AI_PERSISTENCE_IT_REDIS_ADDRESS
TIANSHU_AI_PERSISTENCE_IT_REDIS_USERNAME
TIANSHU_AI_PERSISTENCE_IT_REDIS_PASSWORD
TIANSHU_AI_PERSISTENCE_IT_TENANT_ID
TIANSHU_AI_PERSISTENCE_IT_FOREIGN_TENANT_ID
TIANSHU_AI_PERSISTENCE_IT_DDL_JDBC_URL
TIANSHU_AI_PERSISTENCE_IT_DDL_DB_USERNAME
TIANSHU_AI_PERSISTENCE_IT_DDL_DB_PASSWORD
TIANSHU_AI_PERSISTENCE_IT_OUTBOX_JDBC_URL
TIANSHU_AI_PERSISTENCE_IT_OUTBOX_DB_USERNAME
TIANSHU_AI_PERSISTENCE_IT_OUTBOX_DB_PASSWORD
TIANSHU_AI_PERSISTENCE_IT_LEGACY_JDBC_URL
TIANSHU_AI_PERSISTENCE_IT_LEGACY_DB_USERNAME
TIANSHU_AI_PERSISTENCE_IT_LEGACY_DB_PASSWORD
```

DDL IT 会在专用数据库创建临时 schema，并让 Tianshu DDL 创建 `egon_outbox` schema；Legacy IT 要求另一独立、已初始化且目标业务表为空的 PostgreSQL 库。两者都不要指向共享开发库或生产库。Redis IT 使用配置 ACL 用户验证本租户 key/topic 成功、他租户 key/topic 被拒绝，并检查周期快照对账能从错过通知的状态收敛。

在已完成备份且确认以上数据库均为一次性隔离目标后，可运行：

```bash
./mvnw -pl :egon-cola-tianshu-admin -am \
  -Ptianshu-ai-persistence-it verify
```

Profile 未启用时不会运行 `DdcConfigOutboxIntegrationTest`；普通 `mvn test` 只运行 mock/contract 测试。此文档本身不执行 DDL、导入、Redis ACL 探测或上述 Failsafe 命令。
