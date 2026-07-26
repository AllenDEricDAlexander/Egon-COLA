#set( $symbol_pound = '#' )
${symbol_pound} 学生管理

[English](README.md) | 中文

Student Management 是由 `egon-cola-archetype-light` 生成的单 Maven 模块项目。它是一个可部署的单体应用，其 Java 包结构约束大型单体轻量领域架构；这些分层不是 Maven 子模块。

${symbol_pound}${symbol_pound} 领域优先结构

业务领域位于协议或技术细节之前，使 `user` 和 `teaching` 保持内聚，未来可以在不反转包顺序的情况下拆分为独立服务。

```text
src/main/java/${packageInPathFormat}
├── start
├── adapter
│   ├── user/{controller,mq,rpc,graphql,facade,dto,vo,convertor,validators}
│   ├── teaching/{controller,mq,rpc,graphql,facade,dto,vo,convertor,validators}
│   ├── handler
│   └── filter
├── facade
│   ├── user/{dto,enums,exceptions,utils}
│   └── teaching/{dto,enums,exceptions,utils}
├── application
│   ├── user/{manage,command,query,result,convertor,validators,assemblers}
│   └── teaching/{manage,command,query,result,convertor,validators,assemblers}
├── domain
│   ├── user/{entities,aggregates,vos,service,repos,validators,enums,exceptions}
│   └── teaching/{entities,aggregates,vos,service,repos,validators,enums,exceptions}
├── infrastructure
│   ├── user/{repo,service,validators,client,mq,cache}
│   ├── teaching/{repo,service,validators,client,mq,cache}
│   ├── aop
│   └── config
└── common/{constants,utils,enums,exceptions}
```

`adapter` 负责 HTTP、GraphQL、Dubbo provider 和 RabbitMQ consumer 相关能力。`facade` 负责稳定的外部 RPC 契约。`application` 编排用例和事务。`domain` 负责业务状态、规则、仓储端口和服务端口。`infrastructure` 提供 JPA 仓储以及 Domain 所有端口的实现。`common` 只包含与业务无关的基础类型。`start` 负责组装和运行时配置。

${symbol_pound}${symbol_pound} 依赖图

内部层之间只允许以下依赖：

```text
start          -> adapter, infrastructure
adapter        -> application, facade
application    -> domain
domain         -> common
infrastructure -> domain
facade         -> no internal layer
common         -> no business layer
```

Domain Service 接口位于 `domain.<business>.service`，实现位于 `infrastructure.<business>.service.impl`。Application service 编排这些端口。ArchUnit 测试会强制检查该依赖图，并拒绝 `adapter.controller` 或 `infrastructure.repo` 这类技术优先的根包。

${symbol_pound}${symbol_pound} 主要业务流程

1. 请求校验并完成外部身份查询后创建用户。
2. 为有效用户分配有效角色。
3. 为未归档角色授予有效权限。
4. 创建学校班级和课程。
5. 在校验班级、课程、学期和时间冲突规则后安排课程。

相同的 Application 用例服务于 HTTP、GraphQL、Dubbo 和 RabbitMQ 入口。用户、权限、学校班级和课程查询都通过 Application 边界实现，而不是在各协议 adapter 中重复实现。

${symbol_pound}${symbol_pound} 持久化与集成

JPA 是唯一的持久化实现。Flyway 负责 H2/PostgreSQL schema。RabbitMQ、Redis、GraphQL、Dubbo Triple、Springdoc OpenAPI、AOP 监控、请求上下文过滤器和外部 HTTP client 都包含可运行的实现。

`dev` 是本地工作站开发和 `feature/*` 分支验证的默认 profile，使用由环境变量提供的 PostgreSQL、Redis、RabbitMQ、Nacos、Dubbo 和外部 HTTP 集成。

Maven 测试会自动选择 `test`，`dev`、`release/*` 和 `hotfix/*` 分支的测试流水线也使用该 profile。它使用 H2、内存 adapter 和确定性 stub，并关闭 RabbitMQ、Redis、Nacos、Dubbo registry 和外部 HTTP 调用。

`prod` 仅用于 `main` 分支的运行时构建和部署。`dev` 与 `prod` 通过 `RABBITMQ_ENABLED=true`、`REDIS_ENABLED=true`、`EXTERNAL_HTTP_ENABLED=true`、`NACOS_CONFIG_ENABLED=true`、`NACOS_DISCOVERY_ENABLED=true` 和 `DUBBO_REGISTRY_ADDRESS=nacos://host:8848` 等环境变量配置真实 adapter。

${symbol_pound}${symbol_pound} 分片、读写分离与 Flyway

生成应用始终使用 ShardingSphere 逻辑数据源，支持两种路由模式：

```bash
SPRING_PROFILES_ACTIVE=dev APP_DATASOURCE_MODE=SHARDING ./mvnw spring-boot:run
SPRING_PROFILES_ACTIVE=dev APP_DATASOURCE_MODE=SHARDING_READWRITE ./mvnw spring-boot:run
```

环境 profile 只使用 `dev`、`test`、`prod`。`APP_DATASOURCE_MODE` 可取
`SHARDING`（默认）或 `SHARDING_READWRITE`。两种模式都先逐个对物理 primary
执行 Flyway，再创建逻辑 `DataSource`；逻辑数据源和 replica 永远不是 Flyway
target。读写分离模式下，普通查询走 replica，写操作走 primary，事务内查询固定走
primary。

表拓扑如下：

- `master_data` 上的主数据表：`users`、`roles`、`permissions`、`user_roles`、
  `role_permissions`、`courses`。这些表在 `!SHARDING` 中显式配置
  `databaseStrategy.none` 与 `tableStrategy.none`，不使用 `!SINGLE`，也不存在
  应用级单数据源模式。
- SHARDING 表：`school_classes` 按 `id` 分片，
  `class_course_schedules` 按 `school_class_id` 分片。两者是 binding tables；
  班级和其排课统一使用班级根键，因此共置在同一个物理库和表后缀。
  `DML_SHARDING_CONDITIONS` 会拒绝未携带分片条件的更新或删除，且禁止 hint 绕过。

仅分片模式配置 `LIGHT_SHARDING_MASTER_DATA_URL`、`LIGHT_SHARDING_SHARD_0_URL`、
`LIGHT_SHARDING_SHARD_1_URL`、`LIGHT_SHARDING_USERNAME`、
`LIGHT_SHARDING_PASSWORD`，并可选配置 `LIGHT_SHARDING_DRIVER_CLASS_NAME`。
读写分离模式分别为 `LIGHT_MASTER_DATA_PRIMARY`、`LIGHT_MASTER_DATA_REPLICA_0`、
`LIGHT_SHARD_0_PRIMARY`、`LIGHT_SHARD_0_REPLICA_0`、
`LIGHT_SHARD_1_PRIMARY`、`LIGHT_SHARD_1_REPLICA_0` 配置 URL、用户名和密码；
例如 `LIGHT_SHARD_0_PRIMARY_URL`、`LIGHT_SHARD_0_PRIMARY_USERNAME`、
`LIGHT_SHARD_0_PRIMARY_PASSWORD`。

Flyway 使用 `db/migration/sharding/master-data` 和
`db/migration/sharding/shard` 两个 location，在逻辑数据源创建前按名称串行迁移
已配置的物理 primary。replica 必须是 primary 的数据库级复制节点，永远不能配置
为 Flyway target。Spring Boot Flyway 自动配置被排除，避免任何 migration 误刷逻辑
数据源；设置 `FLYWAY_ENABLED=false` 时跳过物理 migration。

应用生成的代理主键统一使用 UUIDv7，并序列化为 36 位 RFC 字符串。迁移文件名
必须符合 `VyyyyMMdd_NNN__description.sql`：日期使用文件创建日期，`NNN` 是当日
三位序列号。每个 SQL 文件开头必须依次包含 `变更内容`、`影响范围` 和
`兼容性说明` 三项注释。

数据库数、每库物理表数和总物理节点数都必须是 2 的幂。初始映射为
`2 库 × 每库 2 表 = 4 节点`。容量按 2N 法扩展：每次只将一个维度从 `N` 调整为
`2N`，并整体发布完整的 `node-count` 与 `node-map`。当前是尚未执行过迁移的新脚手架，
没有历史数据，也不提供在线迁移、双写、CDC 或自动搬数机制。

事务只允许覆盖一个物理库，聚合操作必须使用同一个分片根键。跨分片流程通过
业务幂等、显式状态、事件、对账和补偿解决；项目不引入 XA、BASE、Seata 或
其他分布式事务协调器。

${symbol_pound}${symbol_pound} 命令

运行全部测试和架构检查：

```bash
./mvnw -B -ntp test
```

打包应用：

```bash
./mvnw -B -ntp -DskipTests package
```

配置好 `dev` 集成后在本地运行：

```bash
./mvnw spring-boot:run
```

${symbol_pound}${symbol_pound} 容器交付

生成的项目使用一个从源码构建的 `deploy/container/Dockerfile`：

```bash
docker build --build-arg CONTAINER_ENGINE=docker -f deploy/container/Dockerfile -t ${artifactId}:local .
podman build --build-arg CONTAINER_ENGINE=podman -f deploy/container/Dockerfile -t ${artifactId}:local .
nerdctl build --build-arg CONTAINER_ENGINE=nerdctl -f deploy/container/Dockerfile -t ${artifactId}:local .
```

使用以下命令启动完整的 Docker 开发栈：

```bash
docker compose --env-file deploy/env/.env.example -f deploy/compose/compose.docker.yaml up -d --build
```

模板自带的 Compose 栈默认使用 `APP_DATASOURCE_MODE=SHARDING`，创建
`postgres-master-data`、`postgres-shard-0`、`postgres-shard-1` 三个 PostgreSQL
primary。模板不创建 replica；`SHARDING_READWRITE` 是为具备对应 primary/replica
端点的环境提供的代码与配置能力。

Podman 和 nerdctl 分别使用 `compose.podman.yaml` 和 `compose.nerdctl.yaml`。生产环境使用匹配的 `.prod.yaml` 文件和由运维方持有的 `.env.prod`。关于 rootless 前置条件、持久化、生产边界和数据删除警告，请参见 `deploy/container/README.md`。

根目录 `Jenkinsfile` 会运行测试，也可以发布不可变镜像。设置 `PUBLISH_IMAGE=true` 并提供 registry 参数即可发布；它不会执行部署。

使用通过 `EGON_CONFIG_DECRYPT_KEY` 或 `EGON_CONFIG_DECRYPT_KEY_FILE` 提供的 32 字节密钥加密配置值：

```bash
printf '%s' 'plain-text' | EGON_CONFIG_DECRYPT_KEY='replace-with-32-byte-secret-key' \
  ./mvnw -q -DskipTests \
  -Dspring-boot.run.main-class=${package}.start.config.encryption.ConfigCipherCli \
  spring-boot:run
```

将输出的 `ENC(v1:...)` 值写入配置。请通过环境变量、挂载文件、`config/application-secrets.yml` 或 `configtree:/run/secrets/` 提供真实密钥；不要提交凭据或解密密钥。
