# 生成工程 README 规范

本文约束 `source-projects/egon-cola-source-<family>/README.md` 与 `README.zh-CN.md`。这两份文件会被复制进每个生成的业务工程，读者是业务工程的开发者。改动前请先读完本文。

## 1. 章节骨架

每份 README 只有一个一级标题（工程名），其下是一段简介和语言切换链接，然后按下表顺序排列 `##` 章节。各族只出现与自身相关的章节，但出现的章节必须使用表中的标题、按表中的相对顺序排列；中英文两份 README 的章节集合与顺序必须完全一致。

| 顺序 | English | 中文 | 使用的 family |
|---|---|---|---|
| 1 | Maven Profiles and External Launch Arguments | Maven Profiles 与外部启动参数 | 全部 |
| 2 | Build and Verification | 构建与验证 | 全部 |
| 3 | Stack | 技术栈 | light-open、web-open |
| 4 | Modules | 模块 | service、service-open、web、web-open |
| 5 | Domain-First Structure | 领域优先结构 | light、light-open、service、web |
| 6 | Dependency Graph | 依赖图 | light、light-open、web |
| 7 | Shared Contracts | 公共契约 | light、light-open |
| 8 | RPC Contracts | RPC 契约 | service-open、web-open |
| 9 | API Endpoints | API 端点 | web-open |
| 10 | Domains | 领域 | web |
| 11 | Runtime Contract | 运行契约 | agent |
| 12 | Knowledge Contract | 知识库契约 | agent |
| 13 | Knowledge Ingest States | 知识库摄取状态 | agent |
| 14 | Knowledge Configuration | 知识库配置 | agent |
| 15 | Environment Contract | 环境配置契约 | agent |
| 16 | Primary Workflows | 主要业务流程 | light、service |
| 17 | IDs and Persistence | ID 与持久化 | light-open、service-open、web-open |
| 18 | Runtime Profiles and Integrations | 运行 Profile 与集成 | light、service、service-open、web |
| 19 | Integration Ownership | 集成职责 | web |
| 20 | Runtime Governance | 运行治理 | light-open、web-open |
| 21 | Error Contract | 错误契约 | web |
| 22 | Native RPC and Tianshu | 原生 RPC 与 Tianshu | light、service、web |
| 23 | Xingyuan Identity and Authorization | 星垣身份与授权 | service、web、agent |
| 24 | Repository, CQE and PostgreSQL | Repository、CQE 与 PostgreSQL | 除 agent 外全部 |
| 25 | Knowledge Repository Migration | 知识库 Repository 迁移 | agent |
| 26 | Two-Level Cache | 二级缓存 | 全部 |
| 27 | Java and CQE Coding Requirements | Java 与 CQE 编码规范 | 除 agent 外全部 |
| 28 | UTC Time Contract | UTC 时间契约 | 全部 |
| 29 | Container Delivery | 容器交付 | light、light-open、service、web |
| 30 | Scope Boundary | 范围边界 | service |

写作约定：

- 英文标题用 Title Case，`and` / `or` / `of` / `the` 等虚词小写，连字符两侧都大写（`Domain-First`、`Two-Level`）。
- 标题描述内容，不写日期或批次。需要记录"何时改的"请写进提交信息，而不是写成 `2026-09-22 ...` 这样的章节名。
- 中文统一用「契约」而不是「合同」；平台名统一写「星垣」。
- 新增一类内容时，先在上表中按读者阅读顺序插入一个槽位，再写进各族 README；不要在文末追加一节了事。

## 2. 冻结子串

各族 `definitions/egon-cola-archetype-<family>/src/test/resources/projects/basic/verify.groovy` 会对**生成后**的 README 做逐字 `contains` 断言。断言在 Velocity 替换之后进行，所以源文件里的 `egon-cola-source-<family>` 在测试中会变成测试工程名（如 `basic`、`student-management-evaluation`）。

**以 `verify.groovy` 为准**，下表只是阅读辅助，可能随测试演进而过时。改 README 前用下面的命令重新确认当前断言：

```bash
grep -n 'README' egon-cola-archetypes/definitions/egon-cola-archetype-<family>/src/test/resources/projects/basic/verify.groovy
```

下表中「MP 七项」指 `EgonColaRepository`、`SHARDING_READWRITE`、`tenant_id`、`mix64-v1`、`LOCAL`、`REBUILD_REQUIRED`、`repository-manifest.json`；「交付六项」指 `deploy/container/Dockerfile`、`compose.docker.yaml`、`compose.podman.yaml`、`compose.nerdctl.yaml`、`Jenkinsfile`、`PUBLISH_IMAGE`；「分支三项」指 `` `dev` is the default profile ``、`` `feature/*` ``、`` `dev`, `release/*`, and `hotfix/*` ``。

| family | 英文 README 必须包含 | 中文 README 必须包含 | 禁止出现 |
|---|---|---|---|
| light | `# <工程名>`、`Docker`、`single Maven module`、三行依赖图（`start          -> adapter, infrastructure`、`adapter        -> application, facade`、`infrastructure -> domain`，列宽固定）、`Domain-First Structure`、`Primary Workflows`、`RABBITMQ_ENABLED=true`、`ConfigCipherCli`、MP 七项、交付六项、分支三项、`` from `main` `` | `# <工程名>`、MP 七项 | `Student Management`、`计费`、`Charge`、`docker build -t` |
| light-open | `Dynamic Thread Pool`、`MyBatis-Plus`、`manual` | — | — |
| service | 分支三项、`` `main` ``、`RabbitMQ support is intentionally basic transport`、`Organization Facade client is an unused infrastructure foundation`、`The Evaluation contract is published by this project itself`、`organization-facade.group-id` / `.artifact-id` / `.version` / `.package`、`domain/exam/entities`、`application/course/manage`、`infrastructure/exam/dao`、`infrastructure/exam/service/impl`、`adapter/exam/mq`、MP 七项、`service-only`、交付六项 | MP 七项、四个 `organization-facade.*`、`Evaluation 契约由本工程自己的` | `facade/api`、`application/manage/course`、`docker build -t` |
| service-open | `MyBatis-Plus`、`Proto`、`Springdoc`、四个 `organization-facade.*` | — | — |
| web | `MyBatis-Plus`、`tenant_id`、`Long`、`clean verify`、`The Organization contract is published by this project itself`、四个 `evaluation-facade.*` | `MyBatis-Plus`、四个 `evaluation-facade.*` | `Spring Data JPA`、`UUIDv7` |
| web-open | `Springdoc`、`MyBatis-Plus`、`Gateway`、`tenant_id`、`Long`、`db/egon-mp`、四个 `evaluation-facade.*` | `MyBatis-Plus` | `spring-boot-starter-data-jpa`、`UUIDv7` |
| agent | `Deep Research`、`POST /api/v1/deep-research/runs`、`clean verify`、`POST /api/v1/knowledge-bases/{knowledgeBaseId}/chat`、`PostgreSQL`、`CREATE EXTENSION vector` | `Deep Research`、`POST /api/v1/knowledge-bases/{knowledgeBaseId}/chat`、`PostgreSQL`、`CREATE EXTENSION vector` | `no database` |

注意第 1 节骨架里的 `Domain-First Structure` 与 `Primary Workflows` 两个标题本身就是 light 的冻结子串，不能改名。

## 3. 其他被断言的文档

| 文件 | 断言 |
|---|---|
| `deploy/container/README.md`（light、service） | 空白归一化后包含 `One Portable Dockerfile`、`Docker`、`Podman`、`nerdctl`、`Rootless And Rootful`、`Development Compose`、`Production Compose`、`Persistent Data`、`Jenkins`、`does not provide high availability` |
| `db/manual/postgresql/README.md`（light-open、service-open、web-open） | 文件必须存在；web-open 的版本还必须包含 `003__migrate` 与 `004__migrate` |
| `definitions/egon-cola-archetype-web/architecture-docs/architecture.md`、`web-open` 同名文件 | 包含 `EgonColaMapper`、`EgonModel`、`tenant_id`；不得包含 `JpaRepository`、`UuidV7` |
| `definitions/egon-cola-archetype-agent/architecture-docs/` | 至少一份文档包含 `Deep Research` |

## 4. 生成机制要点

- `scripts/generate_archetypes.sh` 对每个资源做文本替换：源 groupId → `${groupId}`、源版本 → `${version}`、源包名 → `${package}`、源 artifactId → `${rootArtifactId}`（`root` 拓扑为 `${artifactId}`）。
- Velocity 转义会处理 `$` 与 `##`，所以 README 中的 `##` 标题无需手工转义；`deploy/container/README.md`、`deploy/compose/*`、`mvnw*`、`.gitattributes`、`__gitignore__`、`Jenkinsfile`、wrapper properties 不做转义。
- `definitions/*/src/main/javadoc` 与 `architecture-docs` 中出现本机仓库绝对路径时，生成直接失败。

## 5. 改完怎么验证

按[索引](../README.md#修改与验证)中的顺序运行生成、检查与 archetype IT。archetype IT 是本文全部约束的最终裁判：上面的表格写错了不会让任何东西失败，`verify.groovy` 写的才算数。
