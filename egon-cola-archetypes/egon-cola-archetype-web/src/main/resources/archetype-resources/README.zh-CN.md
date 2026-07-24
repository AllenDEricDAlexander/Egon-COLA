#set( $symbol_pound = '#' )
${symbol_pound} 学生管理组织

[English](README.md) | 中文

该项目由 `egon-cola-archetype-web` 生成，是一个独立的、只负责组织领域的 Project。Adapter 实现 `top.egon:egon-cola-organization-facade`，Infrastructure 消费 `top.egon:egon-cola-evaluation-facade`。

${symbol_pound}${symbol_pound} 模块

```text
student-management-organization-common
student-management-organization-domain
student-management-organization-application
student-management-organization-infrastructure
student-management-organization-adapter
student-management-organization-starter
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

`prod` 仅用于 `main` 分支的运行时构建和部署。`dev` 与 `prod` 都使用 Dubbo Evaluation Facade client，超时 3000 ms、重试次数为 0，并在启动时检查引用。Facade group 和 version 使用 `EVALUATION_*_FACADE_GROUP` 与 `EVALUATION_FACADE_SERVICE_VERSION`；其他 datasource、Redis、RabbitMQ、Nacos、cache 和幂等配置仍由环境变量提供。

${symbol_pound}${symbol_pound} 分片、读写分离与 Flyway

Starter 支持默认、分片、分片加读写分离三种启动方式：

```bash
SPRING_PROFILES_ACTIVE=dev bash ./mvnw -pl ${rootArtifactId}-starter spring-boot:run
SPRING_PROFILES_ACTIVE=dev,sharding bash ./mvnw -pl ${rootArtifactId}-starter spring-boot:run
SPRING_PROFILES_ACTIVE=dev,sharding,readwrite bash ./mvnw -pl ${rootArtifactId}-starter spring-boot:run
```

默认模式使用 Spring Boot 单 `DataSource` 和 Flyway 生命周期。`sharding`
先迁移全部物理 primary，再创建 ShardingSphere JDBC 逻辑 `DataSource`。
`readwrite` 必须与 `sharding` 组合启用：普通查询走 replica，写操作走 primary，
事务内查询固定走 primary。

表拓扑如下：

- `single` 上的 SINGLE 表：`users`、`roles`、`permissions`、`user_roles`、
  `role_permissions`、`grades`。
- SHARDING binding tables：`school_classes` 和 `school_class_users` 都按
  `grade_id` 分片。成员关系中冗余的 `gradeId` 是必填路由键，使班级及其成员关系
  共置在同一个物理库和表后缀。

仅分片模式配置 `ORGANIZATION_SHARDING_SINGLE_URL`、
`ORGANIZATION_SHARDING_SHARD_0_URL`、`ORGANIZATION_SHARDING_SHARD_1_URL`、
`ORGANIZATION_SHARDING_USERNAME`、`ORGANIZATION_SHARDING_PASSWORD`，并可选
配置 `ORGANIZATION_SHARDING_DRIVER_CLASS_NAME`。读写分离模式分别为
`ORGANIZATION_SINGLE_PRIMARY`、`ORGANIZATION_SINGLE_REPLICA_0`、
`ORGANIZATION_SHARD_0_PRIMARY`、`ORGANIZATION_SHARD_0_REPLICA_0`、
`ORGANIZATION_SHARD_1_PRIMARY`、`ORGANIZATION_SHARD_1_REPLICA_0` 配置
URL、用户名和密码；例如 `ORGANIZATION_SINGLE_REPLICA_0_URL`、
`ORGANIZATION_SINGLE_REPLICA_0_USERNAME` 和
`ORGANIZATION_SINGLE_REPLICA_0_PASSWORD`。

Flyway 负责 `db/migration/default`、`db/migration/sharding/single` 和
`db/migration/sharding/shard`。ShardingSphere 模式下，它在逻辑数据源启动前
按名称串行迁移已配置的 primary。replica 必须是 primary 的数据库级复制节点，
永远不能配置为 migration target。

代理主键由应用生成 UUIDv7，并持久化为 36 位 RFC 字符串。迁移文件必须使用
`VyyyyMMdd_NNN__description.sql`，其中日期是文件创建日期，`NNN` 是当日三位
序列号。每个 SQL 文件开头必须包含 `变更内容`、`影响范围` 和 `兼容性说明`
三项注释。

数据库数、每库物理表数和总物理节点数都必须是 2 的幂。初始追加式映射为
`2 库 × 每库 2 表 = 4 节点`，`mapping-version: 1`。单次只允许从 `N` 扩成
`2N`；两个维度都翻倍时必须拆成两次扩容。扩容时保留旧槽位，先迁移新增
primary，追加 `N..2N-1` 槽位，仅搬迁和核对新槽位为 `oldSlot + N` 的记录，
再原子发布递增后的映射版本。稳定槽位契约使理论上仅约一半分片键需要迁移，
但不包含在线双写、CDC 或自动搬数。

只支持一个物理库内的本地事务。参与同一次聚合变更的表必须使用相同
`gradeId`。跨分片流程通过业务幂等、显式状态、事件、对账和补偿解决；项目
不引入 XA、BASE、Seata 或其他分布式事务协调器。

${symbol_pound}${symbol_pound} 错误契约

HTTP 失败使用包含 `code`、`message`、`traceId`、`timestamp` 和 `fieldErrors` 的稳定响应体。状态语义为：`400` 校验失败，`403` 禁止访问，`404` 资源不存在，`409` 冲突，`422` Domain 拒绝，`503` 必需依赖不可用，`500` 未预期失败。GraphQL 通过 error extensions 暴露相同字段；Facade 失败携带相同 code 和 trace ID。

${symbol_pound}${symbol_pound} 命令

验证所有模块：

```bash
bash ./mvnw -V --no-transfer-progress clean verify
```

打包分层可执行 Jar：

```bash
bash ./mvnw -V --no-transfer-progress -DskipTests package
```

加密配置值：

```bash
CONFIG_DECRYPT_KEY=base64-encoded-32-byte-key \
  bash ./mvnw -pl ${rootArtifactId}-starter \
  -Dexec.mainClass=${package}.starter.config.encryption.ConfigCipherCli \
  -Dexec.args='encrypt plaintext-value' exec:java
```

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

Podman 和 nerdctl 分别使用 `compose.podman.yaml` 和 `compose.nerdctl.yaml`。生产环境使用匹配的 `.prod.yaml` 文件和由运维方持有的 `.env.prod`。关于 rootless 前置条件、持久化、生产边界和数据删除警告，请参见 `deploy/container/README.md`。

根目录 `Jenkinsfile` 会运行测试，也可以发布不可变镜像。设置 `PUBLISH_IMAGE=true` 并提供 registry 参数即可发布；它不会执行部署。

可选的本地运行：

```bash
SPRING_PROFILES_ACTIVE=dev bash ./mvnw -pl ${rootArtifactId}-starter spring-boot:run
```

敏感值应放在环境变量、挂载文件、`config/application-secrets.yml` 或 `configtree:/run/secrets/` 中。不要提交凭据或解密密钥。
