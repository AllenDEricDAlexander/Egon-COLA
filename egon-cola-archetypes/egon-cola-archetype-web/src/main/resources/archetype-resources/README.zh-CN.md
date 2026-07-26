#set( $symbol_pound = '#' )
${symbol_pound} ${rootArtifactId}

[English](README.md) | [中文](README.zh-CN.md)

`${rootArtifactId}` 由 `egon-cola-archetype-web` 生成，是一个独立的、只负责组织领域的 Project。Adapter 实现 `top.egon:egon-cola-organization-facade`，Infrastructure 消费 `top.egon:egon-cola-evaluation-facade`。

${symbol_pound}${symbol_pound} 模块

```text
${rootArtifactId}-common
${rootArtifactId}-domain
${rootArtifactId}-application
${rootArtifactId}-infrastructure
${rootArtifactId}-adapter
${rootArtifactId}-starter
```

${symbol_pound}${symbol_pound} 领域优先包布局

业务代码先按领域组织，再按技术职责组织：

```text
domain/user/entities
application/teaching/manage
infrastructure/user/repo
adapter/user/facade/impl
adapter/teaching/controller
```

共享运行时能力保留在各自的层根包中。外部 Evaluation 边界位于 `domain/client/evaluation` 和 `infrastructure/client/evaluation`，这是有意保留的例外。

${symbol_pound}${symbol_pound} 依赖方向

```text
common         -> no generated module
domain         -> common
application    -> domain
infrastructure -> domain, canonical Evaluation Facade
adapter        -> application, canonical Organization Facade
starter        -> adapter, infrastructure
```

Infrastructure 实现 Domain 所有的端口。Adapter 不能直接访问 Infrastructure，Starter 只包含组装配置。两个 canonical Facade 都是独立 artifact，不依赖当前生成项目，因此 Web/Service Maven 依赖图不会形成循环。

${symbol_pound}${symbol_pound} 领域

完整的 `user` 垂直领域负责创建和查询用户、分配角色、授予权限、缓存用户读取结果，并发布已提交的变更。

完整的 `teaching` 垂直领域负责创建和查询成绩与学校班级、缓存查询结果，并发布已提交的变更。用户与班级的分配是一个同时使用两个 Domain repository 的 Application 事务；`school_class_users` 是权威的成员关系。

${symbol_pound}${symbol_pound} 集成职责

- Adapter 负责 HTTP `/api/v1/**`、GraphQL `/graphql`、入站 RabbitMQ command、Dubbo Facade export、请求校验、过滤器和协议转换。
- Infrastructure 负责 JPA、Flyway、Redis adapter、出站 RabbitMQ event、Evaluation Facade 防腐 adapter、本地 fallback adapter，以及 Application 方法日志 AOP。
- Starter 负责 OpenAPI 组装、运行时 profile、Actuator、Prometheus、Jackson、异步执行和配置解密。
- `top.egon:egon-cola-organization-facade` 是 Provider 契约，`top.egon:egon-cola-evaluation-facade` 是消费契约；两者都不会作为本地模块重复生成。

生成的 `EvaluationQueryPort` 是暂未使用的集成基础能力；当前没有 Application 用例调用它。

RabbitMQ command 使用总计三次尝试、有限退避和死信队列。领域事件在提交后发布。该示例会报告事件发布耗尽，但不声称具备事务 outbox 的投递保证。

${symbol_pound}${symbol_pound} 运行时 Profile

`dev` 是本地工作站开发和 `feature/*` 分支验证的默认 profile，使用由环境变量提供的 PostgreSQL、Redis、RabbitMQ、Nacos 和 Dubbo 集成。

Maven 测试会自动选择 `test`，`dev`、`release/*` 和 `hotfix/*` 分支的测试流水线也使用该 profile。它使用 PostgreSQL 兼容模式的 H2、内存缓存/幂等 adapter、本地事件发布器、确定性的 Evaluation 查询 stub、已关闭的 RabbitMQ 与 Nacos 连接，以及不使用 registry 的 Dubbo `injvm`。

`prod` 仅用于 `main` 分支的运行时构建和部署。`dev` 与 `prod` 都使用 Dubbo Evaluation Facade client，超时 3000 ms、重试次数为 0，并在启动时检查引用。每个被消费的 Facade 都有各自的 group 变量：`EVALUATION_COURSE_FACADE_GROUP`（默认 `course`）、`EVALUATION_EXAM_FACADE_GROUP`（默认 `exam`）、`EVALUATION_SCORE_FACADE_GROUP`（默认 `score`），版本号为 `EVALUATION_FACADE_SERVICE_VERSION`（默认 `1.0.0`）。

Nacos 使用 `NACOS_SERVER_ADDR`、`NACOS_NAMESPACE`、`NACOS_USERNAME`、`NACOS_PASSWORD`，config 与 discovery 的分组变量是分开的 `NACOS_CONFIG_GROUP` 与 `NACOS_DISCOVERY_GROUP`，开关为 `NACOS_CONFIG_ENABLED`、`NACOS_DISCOVERY_ENABLED`、`NACOS_CONFIG_REFRESH_ENABLED` 和 `DISCOVERY_ENABLED`。RabbitMQ 连接参数使用 Spring 自身的 `SPRING_RABBITMQ_HOST`、`SPRING_RABBITMQ_PORT`、`SPRING_RABBITMQ_USERNAME`、`SPRING_RABBITMQ_PASSWORD`。其他 datasource、Redis、cache 和幂等配置仍由环境变量提供。

${symbol_pound}${symbol_pound} 分片、读写分离与 Flyway

生成应用始终使用 ShardingSphere 逻辑数据源，支持两种路由模式：

```bash
SPRING_PROFILES_ACTIVE=dev APP_DATASOURCE_MODE=SHARDING bash ./mvnw -pl ${rootArtifactId}-starter spring-boot:run
SPRING_PROFILES_ACTIVE=dev APP_DATASOURCE_MODE=SHARDING_READWRITE bash ./mvnw -pl ${rootArtifactId}-starter spring-boot:run
```

环境 profile 只使用 `dev`、`test`、`prod`。 `APP_DATASOURCE_MODE`
只能取 `SHARDING`（默认）或 `SHARDING_READWRITE`。两种模式都先逐个迁移
配置中的物理 primary，再创建逻辑 `DataSource`；逻辑数据源和 replica 永远不是
Flyway target。读写分离模式下，普通查询走 replica，写操作走 primary，事务内查询
固定走 primary。模板内置 Compose 不模拟 replica，只默认运行 `SHARDING`。

表拓扑如下：

- 主数据表 `users`、`roles`、`permissions`、`user_roles`、
  `role_permissions`、`grades` 固定在 `master_data`，并在
  `!SHARDING.tables` 中显式使用 `databaseStrategy.none` 和
  `tableStrategy.none`；不使用 `!SINGLE`，也不存在应用级单数据源模式。
- binding tables `school_classes` 和 `school_class_users` 都按 `grade_id`
  分片。成员关系中冗余的 `gradeId` 是必填路由键，使班级及其成员关系共置在同一
  物理库和表后缀。
- 两个分片表都启用 `DML_SHARDING_CONDITIONS`，拒绝未携带分片条件的 DML，
  且 `allowHintDisable=false` 禁止 hint 绕过。

仅分片模式配置 `ORGANIZATION_SHARDING_MASTER_DATA_URL`、
`ORGANIZATION_SHARDING_SHARD_0_URL`、`ORGANIZATION_SHARDING_SHARD_1_URL`、
`ORGANIZATION_SHARDING_USERNAME`、`ORGANIZATION_SHARDING_PASSWORD`，并可选
配置 `ORGANIZATION_SHARDING_DRIVER_CLASS_NAME`。读写分离模式分别为
`ORGANIZATION_MASTER_DATA_PRIMARY`、`ORGANIZATION_MASTER_DATA_REPLICA_0`、
`ORGANIZATION_SHARD_0_PRIMARY`、`ORGANIZATION_SHARD_0_REPLICA_0`、
`ORGANIZATION_SHARD_1_PRIMARY`、`ORGANIZATION_SHARD_1_REPLICA_0` 配置
URL、用户名和密码。

Flyway 只使用 `db/migration/sharding/master-data` 和
`db/migration/sharding/shard`，在逻辑数据源创建前按名称串行迁移物理 primary。
Spring Boot Flyway 自动配置被排除，replica 和逻辑数据源均不会刷表；
`FLYWAY_ENABLED=false` 时跳过物理 migration。

代理主键统一由应用生成 UUIDv7，并持久化为 36 位 RFC 字符串。迁移文件名必须符合
`VyyyyMMdd_NNN__description.sql`，每个文件开头依次包含 `变更内容`、`影响范围`
和 `兼容性说明` 三项注释。

数据库数、每库物理表数和总物理节点数都必须是 2 的幂。初始映射为
`2 库 × 每库 2 表 = 4 节点`，由 `ORGANIZATION_SHARDING_NODE_COUNT`（默认 `4`）与
`ORGANIZATION_SHARDING_NODE_MAP`（默认 `0=shard_0:0,1=shard_0:1,2=shard_1:0,3=shard_1:1`）承载，
逻辑库名由 `ORGANIZATION_SHARDING_DATABASE_NAME` 指定。容量按 2N 法扩展：每次只将一个维度从 `N`
调整为 `2N`，并整体发布完整的 `node-count` 与 `node-map`。当前是尚未执行过
迁移的新脚手架，没有历史数据，也不提供在线迁移、双写、CDC 或自动搬数机制。

事务只允许覆盖一个物理库。班级及其成员的一次聚合操作必须使用相同 `gradeId`。
跨分片流程通过业务幂等、显式状态、事件、对账和补偿解决；项目不引入 XA、BASE、
Seata 或其他分布式事务协调器。

${symbol_pound}${symbol_pound} 错误契约

HTTP 失败使用包含 `code`、`message`、`traceId`、`timestamp` 和 `fieldErrors` 的稳定响应体。状态语义为：`400` 校验失败，`403` 禁止访问，`404` 资源不存在，`409` 冲突，`422` Domain 拒绝，`503` 必需依赖不可用，`500` 未预期失败。GraphQL 通过 error extensions 暴露相同字段；Facade 失败携带相同 code 和 trace ID。

${symbol_pound}${symbol_pound} 命令

验证所有模块：

```bash
bash ./mvnw -V --no-transfer-progress clean verify
```

必须使用 `verify` 而非 `test`：架构治理插件绑定在该阶段，并以 `unknownLayerPolicy=FAIL`
运行，任何解析不到分层的模块或类都会让构建失败。

打包分层可执行 Jar：

```bash
bash ./mvnw -V --no-transfer-progress -DskipTests package
```

使用通过 `EGON_CONFIG_DECRYPT_KEY` 或 `EGON_CONFIG_DECRYPT_KEY_FILE` 提供的 32 字节密钥加密配置值：

```bash
printf '%s' 'plain-text' | EGON_CONFIG_DECRYPT_KEY='replace-with-32-byte-secret-key' \
  bash ./mvnw -q -pl ${rootArtifactId}-starter -am -DskipTests compile exec:java \
  -Dexec.mainClass=${package}.starter.config.encryption.ConfigCipherCli
```

`ConfigCipherCli` 不接受任何参数，明文从标准输入读取；`exec:java` 在 Maven 自身的 JVM 中运行，
上面的管道才能送达。将输出的 `ENC(v1:...)` 值填入配置。

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

模板自带的 Compose 栈创建三个 PostgreSQL primary（`master_data`、`shard_0`
和 `shard_1`），默认使用 `APP_DATASOURCE_MODE=SHARDING`。它不会模拟 replica；
如需 `SHARDING_READWRITE`，应使用运维方管理的读写拓扑并完整配置相应变量。

Podman 和 nerdctl 分别使用 `compose.podman.yaml` 和 `compose.nerdctl.yaml`。生产环境使用匹配的 `.prod.yaml` 文件和由运维方持有的 `.env.prod`。关于 rootless 前置条件、持久化、生产边界和数据删除警告，请参见 `deploy/container/README.md`。

根目录 `Jenkinsfile` 会运行测试，也可以发布不可变镜像。设置 `PUBLISH_IMAGE=true` 并提供 registry 参数即可发布；它不会执行部署。

可选的本地运行：

```bash
SPRING_PROFILES_ACTIVE=dev bash ./mvnw -pl ${rootArtifactId}-starter spring-boot:run
```

敏感值应放在环境变量、挂载文件、`config/application-secrets.yml` 或 `configtree:/run/secrets/` 中。不要提交凭据或解密密钥。
