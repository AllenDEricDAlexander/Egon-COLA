#set( $symbol_pound = '#' )
${symbol_pound} ${rootArtifactId}

[English](README.md) | 中文

`${rootArtifactId}` 是一个只面向 service 的 COLA 示例，覆盖 Course、Schedule、Exam、Paper 和 Score 流程。业务流量通过 Dubbo Triple RPC 或 RabbitMQ 进入；HTTP 仅用于 Spring Boot Actuator 管理端点。

${symbol_pound}${symbol_pound} 模块职责

- `${rootArtifactId}-common`：稳定的错误、常量、枚举和标识符工具。
- `${rootArtifactId}-domain`：实体、聚合、值对象、领域服务、仓储/事件端口，以及由消费者拥有的 Organization 目录端口。不包含持久化、MQ、Facade 或 Dubbo 实现。
- `${rootArtifactId}-application`：命令、查询、用例管理器、应用校验和结果模型。
- `${rootArtifactId}-infrastructure`：Spring Data JPA 仓储、Flyway migration、RabbitMQ/本地发布器实现，以及 `top.egon:egon-cola-organization-facade` 防腐适配器。
- `${rootArtifactId}-adapter`：`top.egon:egon-cola-evaluation-facade` 的 Dubbo provider、facade 转换、校验、异常转换和 score-command MQ consumer。
- `${rootArtifactId}-starter`：Spring Boot 组装、profile、管理配置以及架构/上下文测试。

${symbol_pound}${symbol_pound} 领域优先包布局

业务代码先按领域组织，再按技术职责组织：

```text
domain/exam/entities
application/course/manage
infrastructure/exam/repo
adapter/course/facade/impl
adapter/exam/mq
```

该项目保持 service-only：业务流量通过 Dubbo Triple 或 RabbitMQ 进入，不包含业务 Controller、Web Filter、GraphQL 或 VO 包。外部 Organization 边界位于 `domain/client/organization` 和 `infrastructure/client/organization`。

允许的内部依赖图为：

```text
Common <- Domain <- Application <- Adapter <- Canonical Evaluation Facade
          Domain <- Infrastructure <- Canonical Organization Facade
          Adapter <- Starter -> Infrastructure
```

更精确地说：Domain 只依赖 Common；Application 和 Infrastructure 只依赖 Domain；Adapter 只依赖 Application。Adapter 实现外部 Evaluation Facade 契约，Infrastructure 消费外部 Organization Facade 契约，两个已发布 Facade 都不依赖当前生成项目。Starter 是组合根，因此不存在 Web/Service Maven 循环依赖。

${symbol_pound}${symbol_pound} 示例流程

- Course RPC 创建具有唯一规范化 code 的课程，读取课程、分页查询课程，并在时间范围不重叠的情况下安排课程。
- Exam RPC 为课程创建考试，关联一张试卷，并且只在试卷就绪后发布考试。
- Score RPC 记录并查询经过校验的分数。RabbitMQ score command 通过 `RecordScoreConsumer` 进入，然后委托给同一个 Application 用例。
- Domain publisher port 描述课程安排、考试发布和分数记录；Infrastructure 提供本地或 RabbitMQ 实现。

RabbitMQ 支持有意保持为基础传输能力。示例不承诺重试、死信队列、幂等 inbox、事务 outbox，或超出 broker 配置行为之外的投递保证。

${symbol_pound}${symbol_pound} Profile 与集成

`dev` 是本地工作站开发和 `feature/*` 分支验证的默认 profile，使用由环境变量提供的 PostgreSQL、Nacos、RabbitMQ 和 Dubbo 集成。

Maven 测试会自动选择 `test`，`dev`、`release/*` 和 `hotfix/*` 分支的测试流水线也使用该 profile。它使用 PostgreSQL 兼容模式的 H2，关闭 RabbitMQ publisher 和 listener，并选择确定性的 `OrganizationDirectoryPort` stub，因此不需要 Nacos、RabbitMQ、PostgreSQL 或外部 Dubbo provider。

Organization Facade client 仍是一个暂未使用的 infrastructure 基础能力；当前没有 Application 用例调用 Organization port。

`prod` 仅用于 `main` 分支的运行时构建和部署。`dev` 与 `prod` 都选择真实的 Organization Dubbo client，通过生成的 POM 固定 `top.egon:egon-cola-organization-facade`，并在 provider 不可用时显式失败。请通过环境变量配置，不要提交敏感信息：

- 数据库：按下文为 `master_data`、`shard_0`、`shard_1` 配置 ShardingSphere 物理数据源。
- Nacos：`NACOS_SERVER_ADDR`、`NACOS_NAMESPACE`、`NACOS_USERNAME`、`NACOS_PASSWORD`。config 与 discovery 的分组是分开的：`NACOS_CONFIG_GROUP` 与 `NACOS_DISCOVERY_GROUP`；开关同样分开：`NACOS_CONFIG_ENABLED`、`NACOS_DISCOVERY_ENABLED`、`NACOS_CONFIG_REFRESH_ENABLED` 和 `DISCOVERY_ENABLED`。
- Dubbo：`DUBBO_REGISTRY_ADDRESS`、`DUBBO_PORT`、`DUBBO_CONSUMER_TIMEOUT`。
- Organization Facade：`ORGANIZATION_FACADE_ENABLED`、`ORGANIZATION_FACADE_GROUP`、`ORGANIZATION_FACADE_SERVICE_VERSION`。
- RabbitMQ：连接参数通过 Spring 自身的变量名绑定——`SPRING_RABBITMQ_HOST`、`SPRING_RABBITMQ_PORT`、`SPRING_RABBITMQ_USERNAME`、`SPRING_RABBITMQ_PASSWORD`；`RABBITMQ_ENABLED` 与 `RABBITMQ_LISTENER_AUTO_STARTUP` 则是本应用自己的开关。
- 配置解密：`EGON_CONFIG_DECRYPT_KEY`、`EGON_CONFIG_DECRYPT_KEY_FILE` 或文档化的 config-tree secret source。

${symbol_pound}${symbol_pound} 分片、读写分离与 Flyway

生成应用始终使用 ShardingSphere 逻辑数据源，支持两种路由模式：

```bash
SPRING_PROFILES_ACTIVE=dev APP_DATASOURCE_MODE=SHARDING bash ./mvnw -pl ${rootArtifactId}-starter spring-boot:run
SPRING_PROFILES_ACTIVE=dev APP_DATASOURCE_MODE=SHARDING_READWRITE bash ./mvnw -pl ${rootArtifactId}-starter spring-boot:run
```

环境 profile 只使用 `dev`、`test`、`prod`。`APP_DATASOURCE_MODE` 可取
`SHARDING`（默认）或 `SHARDING_READWRITE`。两种模式都先逐个迁移配置中的
物理 primary，再创建逻辑 `DataSource`；逻辑数据源和 replica 永远不是 Flyway
target。读写分离模式下，普通查询走 replica，写操作走 primary，事务内查询固定走
primary。模板内置 Compose 不模拟 replica，只默认运行 `SHARDING`。

表拓扑如下：

- 主数据表 `course` 固定在 `master_data`，并在 `!SHARDING.tables` 中显式使用
  `databaseStrategy.none` 和 `tableStrategy.none`。不使用 `!SINGLE`，也不存在
  应用级单数据源模式。
- `course_schedule` 按 `course_id` 分片。
- binding tables `exam`、`exam_paper`、`score` 分别按 `id`、`exam_id`、
  `exam_id` 分片；考试聚合统一使用 `examId`，因此共置在同一个物理库和表后缀。
- 四个分片表都启用 `DML_SHARDING_CONDITIONS`，拒绝未携带分片条件的 DML，
  且 `allowHintDisable=false` 禁止 hint 绕过。

仅分片模式配置 `EVALUATION_SHARDING_MASTER_DATA_URL`、
`EVALUATION_SHARDING_SHARD_0_URL`、`EVALUATION_SHARDING_SHARD_1_URL`、
`EVALUATION_SHARDING_USERNAME`、`EVALUATION_SHARDING_PASSWORD`，并可选配置
`EVALUATION_SHARDING_DRIVER_CLASS_NAME`。读写分离模式分别为
`EVALUATION_MASTER_DATA_PRIMARY`、`EVALUATION_MASTER_DATA_REPLICA_0`、
`EVALUATION_SHARD_0_PRIMARY`、`EVALUATION_SHARD_0_REPLICA_0`、
`EVALUATION_SHARD_1_PRIMARY`、`EVALUATION_SHARD_1_REPLICA_0` 配置 URL、
用户名和密码。

Flyway 只使用 `db/migration/sharding/master-data` 和
`db/migration/sharding/shard`，在逻辑数据源创建前按名称串行迁移物理 primary。
Spring Boot Flyway 自动配置被排除，replica 和逻辑数据源均不会刷表；
`FLYWAY_ENABLED=false` 时跳过物理 migration。

代理主键统一由应用生成 UUIDv7，并持久化为 36 位 RFC 字符串。迁移文件名必须符合
`VyyyyMMdd_NNN__description.sql`，每个文件开头依次包含 `变更内容`、`影响范围`
和 `兼容性说明` 三项注释。

数据库数、每库物理表数和总物理节点数都必须是 2 的幂。初始映射为
`2 库 × 每库 2 表 = 4 节点`，由 `EVALUATION_SHARDING_NODE_COUNT`（默认 `4`）与
`EVALUATION_SHARDING_NODE_MAP`（默认 `0=shard_0:0,1=shard_0:1,2=shard_1:0,3=shard_1:1`）承载，
逻辑库名由 `EVALUATION_SHARDING_DATABASE_NAME` 指定。容量按 2N 法扩展：每次只将一个维度从 `N`
调整为 `2N`，并整体发布完整的 `node-count` 与 `node-map`。当前是尚未执行过
迁移的新脚手架，没有历史数据，也不提供在线迁移、双写、CDC 或自动搬数机制。

事务只允许覆盖一个物理库。考试、试卷和成绩的一次聚合操作必须使用相同
`examId`；排课操作必须保留 `courseId`。跨分片流程通过业务幂等、显式状态、
事件、对账和补偿解决；项目不引入 XA、BASE、Seata 或其他分布式事务协调器。

${symbol_pound}${symbol_pound} 验证与打包

```bash
SPRING_PROFILES_ACTIVE=test bash ./mvnw -B -ntp clean verify
SPRING_PROFILES_ACTIVE=test bash ./mvnw -B -ntp -DskipTests package
```

测试套件包括 Domain 规则、Application 编排、JPA adapter、日期序列 Flyway
migration 契约、无 broker 的 MQ adapter、实际 Dubbo Triple proxy 调用、
无外部依赖的 Spring context 组装和架构依赖检查。构建镜像不会启动服务。

必须使用 `verify` 而不是 `test`：架构治理插件绑定在 `verify` 阶段并以
`unknownLayerPolicy=FAIL` 运行，`clean test` 会跑完全部单元测试但完全不做分层检查。
生成的 `.github/workflows/ci.yml` 与根目录 `Jenkinsfile` 也因此统一使用 `verify`。

使用通过 `EGON_CONFIG_DECRYPT_KEY` 或 `EGON_CONFIG_DECRYPT_KEY_FILE` 提供的 32 字节密钥加密配置值：

```bash
printf '%s' 'plain-text' | EGON_CONFIG_DECRYPT_KEY='replace-with-32-byte-secret-key' \
  bash ./mvnw -q -pl ${rootArtifactId}-starter -am -DskipTests compile exec:java \
  -Dexec.mainClass=${package}.starter.config.encryption.ConfigCipherCli
```

`ConfigCipherCli` 不接受任何参数，明文从标准输入读取。将输出的 `ENC(v1:...)` 值填入配置。

${symbol_pound}${symbol_pound} 容器交付

生成的项目使用一个从源码构建的 `deploy/container/Dockerfile`：

```bash
docker build --build-arg CONTAINER_ENGINE=docker -f deploy/container/Dockerfile -t ${rootArtifactId}:local .
podman build --build-arg CONTAINER_ENGINE=podman -f deploy/container/Dockerfile -t ${rootArtifactId}:local .
nerdctl build --build-arg CONTAINER_ENGINE=nerdctl -f deploy/container/Dockerfile -t ${rootArtifactId}:local .
```

使用以下命令启动完整的 Docker 开发栈：

```bash
docker compose --env-file deploy/env/.env.example -f deploy/compose/compose.docker.yaml up -d --build
```

模板自带的 Compose 栈默认使用 `APP_DATASOURCE_MODE=SHARDING`，创建
`postgres-master-data`、`postgres-shard-0`、`postgres-shard-1` 三个 PostgreSQL
primary。模板不创建 replica；`SHARDING_READWRITE` 仅提供代码和配置能力，需由部署
环境传入对应的 primary/replica 端点。

Podman 和 nerdctl 分别使用 `compose.podman.yaml` 和 `compose.nerdctl.yaml`。生产环境使用匹配的 `.prod.yaml` 文件和由运维方持有的 `.env.prod`。关于 rootless 前置条件、持久化、生产边界和数据删除警告，请参见 `deploy/container/README.md`。

根目录 `Jenkinsfile` 会运行测试，也可以发布不可变镜像。设置 `PUBLISH_IMAGE=true` 并提供 registry 参数即可发布；它不会执行部署。

${symbol_pound}${symbol_pound} 范围边界

该生成的 service 项目不包含业务 Controller、Web Filter、GraphQL endpoint、native grpc-java 模块或启用的 H2 console。Organization Facade client 有意未接入当前 Application 行为。
