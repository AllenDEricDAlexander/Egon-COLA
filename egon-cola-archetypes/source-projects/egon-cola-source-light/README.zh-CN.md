# egon-cola-source-light

[English](README.md) | [中文](README.zh-CN.md)

`egon-cola-source-light` 是由 `egon-cola-archetype-light` 生成的单 Maven 模块项目。它是一个可部署的单体应用，其 Java 包结构约束大型单体轻量领域架构；这些分层不是 Maven 子模块。

## Maven Profiles 与外部启动参数

每次选择 `dev`、`test`、`prod` 中一个环境 profile，未指定时使用 `dev` 默认值。
这些 profile 控制应用启动，Surefire 仍使用原有 `test` 配置。堆内存参数是示例值，部署时按资源调整。

在项目根目录执行，多模块项目先安装兄弟模块依赖：

```bash
mvn install
mvn -Pdev spring-boot:run
mvn -Pprod -Drun.jvm-args="-Xms1g -Xmx2g" \
  -Drun.server-port=8080 \
  -Drun.config-location=file:/etc/myapp/override.yml package
```

Maven 在 `process-resources` 阶段生成 `target/launch.args`，打包时也会生成。
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

## 领域优先结构

业务领域位于协议或技术细节之前，使 `user` 和 `teaching` 保持内聚，未来可以在不反转包顺序的情况下拆分为独立服务。

```text
src/main/java/top/egon/cola/archetype/source/light
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
│   ├── user/{entities,aggregates,vos,service,client,gateway,event,validators,enums,exceptions}
│   └── teaching/{entities,aggregates,vos,service,client,gateway,event,validators,enums,exceptions}
├── infrastructure
│   ├── user/{repo,service,validators,client,mq,cache}
│   ├── teaching/{repo,service,validators,client,mq,cache}
│   ├── aop
│   └── config
└── common/{constants,utils,enums,exceptions}
```

`adapter` 负责 HTTP、GraphQL、COLA RPC provider 和 RabbitMQ consumer 相关能力。`facade` 负责稳定的外部 RPC 契约。`application` 编排用例和事务。`domain` 负责业务状态、规则、gateway/cache/event 端口和服务契约。`infrastructure` 提供 MyBatis-Plus DAO、`EgonModel` 持久化对象以及 Domain 所有端口的实现。`common` 只包含与业务无关的基础类型。`start` 负责组装和运行时配置。

## 依赖图

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

Domain Service 接口位于 `domain.<business>.service`，实现位于 `infrastructure.<business>.service.impl`。Application service 编排这些端口。

依赖图由 `egon-cola-component-bytecode-architecture-maven-plugin` 强制检查。它绑定在 `verify` 阶段，把每个 `top.egon.cola.archetype.source.light.<layer>` 包映射到对应分层，并以 `unknownLayerPolicy=FAIL` 运行；因此落在映射之外的类会让构建失败，而不是只留下一条警告。`adapter.controller`、`infrastructure.repo` 这类技术优先的根包因为无法解析到任何分层而被拒绝。该检查只在 `verify` 阶段执行，单独运行 `./mvnw test` 不会触发它。

## 主要业务流程

1. 请求校验并完成外部身份查询后创建用户。
2. 为有效用户分配有效角色。
3. 为未归档角色授予有效权限。
4. 创建学校班级和课程。
5. 在校验班级、课程、学期和时间冲突规则后安排课程。

相同的 Application 用例服务于 HTTP、GraphQL、COLA RPC 和 RabbitMQ 入口。用户、权限、学校班级和课程查询都通过 Application 边界实现，而不是在各协议 adapter 中重复实现。

## 持久化与集成

持久化统一使用 `egon-cola-component-common-mybatis-plus-spring-boot-starter` 提供的 MyBatis-Plus。Domain service 接口继承 `EgonColaIService`，infrastructure service 实现继承 `EgonColaServiceImpl`，DAO 继承 `EgonColaMapper`，所有 PO 继承 `EgonModel` 并添加 MyBatis-Plus 表注解。Flyway 负责 H2/PostgreSQL schema。RabbitMQ、Redis、GraphQL、COLA native unary RPC、platform OpenAPI、AOP 监控、请求上下文过滤器和外部 HTTP client 都包含可运行的实现。

`dev` 是本地工作站开发和 `feature/*` 分支验证的默认 profile，使用由环境变量提供的 PostgreSQL、Redis、RabbitMQ、COLA RPC 和外部 HTTP 集成。

Maven 测试会自动选择 `test`，`dev`、`release/*` 和 `hotfix/*` 分支的测试流水线也使用该 profile。它使用 H2、内存 adapter 和确定性 stub，并关闭 RabbitMQ、Redis、COLA RPC registry 和外部 HTTP 调用。

`prod` 仅用于 `main` 分支的运行时构建和部署。`dev` 与 `prod` 通过 `RABBITMQ_ENABLED=true`、`REDIS_ENABLED=true`、`EXTERNAL_HTTP_ENABLED=true`、`DDC_RPC_TARGET=host:19090`、`DDC_ENABLED=true`、`DDC_REGISTRY_ENABLED=true` 和 `RPC_ENABLED=true` 等环境变量配置真实 adapter。消息代理凭据使用 Spring 自身的变量名 `SPRING_RABBITMQ_HOST`、`SPRING_RABBITMQ_PORT`、`SPRING_RABBITMQ_USERNAME`、`SPRING_RABBITMQ_PASSWORD`，而 `RABBITMQ_ENABLED` 与 `RABBITMQ_LISTENER_AUTO_STARTUP` 是应用自身的开关。

## 分片、读写分离与 Flyway

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
- SHARDING 表：`school_classes` 和 `class_course_schedules` 都按正数 Long
  `tenant_id` 分库分表。两者是 binding tables；同一租户的班级和排课共置在
  同一个物理库和表后缀，`school_class_id` 仍是排课关联键。
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

应用生成的代理主键统一使用正数 `Long`，租户上下文中的 `tenant_id` 也使用正数
`Long`，并作为数据库和表的分片键。迁移文件名必须符合
`VyyyyMMdd_NNN__description.sql`：日期使用文件创建日期，`NNN` 是当日三位序列号。
每个 SQL 文件开头必须依次包含 `变更内容`、`影响范围` 和 `兼容性说明` 三项注释。

数据库数、每库物理表数和总物理节点数都必须是 2 的幂。初始映射为
`2 库 × 每库 2 表 = 4 节点`，由 `LIGHT_SHARDING_NODE_COUNT`（默认 `4`）与
`LIGHT_SHARDING_NODE_MAP`（默认 `0=shard_0:0,1=shard_0:1,2=shard_1:0,3=shard_1:1`）承载，
逻辑库名由 `LIGHT_SHARDING_DATABASE_NAME` 指定。容量按 2N 法扩展：每次只将一个维度从 `N` 调整为
`2N`，并整体发布完整的 `node-count` 与 `node-map`。当前是尚未执行过迁移的新脚手架，
没有历史数据，也不提供在线迁移、双写、CDC 或自动搬数机制。

事务只允许覆盖一个物理库，聚合操作必须使用同一个分片根键。跨分片流程通过
业务幂等、显式状态、事件、对账和补偿解决；项目不引入 XA、BASE、Seata 或
其他分布式事务协调器。

## 命令

运行全部测试和架构检查：

```bash
./mvnw -B -ntp verify
```

必须使用 `verify`。架构治理插件绑定在该阶段，`./mvnw test` 只会跑测试，不做任何分层检查。

打包应用：

```bash
./mvnw -B -ntp -DskipTests package
```

配置好 `dev` 集成后在本地运行：

```bash
./mvnw spring-boot:run
```

## 容器交付

生成的项目使用一个从源码构建的 `deploy/container/Dockerfile`：

```bash
docker build --build-arg CONTAINER_ENGINE=docker -f deploy/container/Dockerfile -t egon-cola-source-light:local .
podman build --build-arg CONTAINER_ENGINE=podman -f deploy/container/Dockerfile -t egon-cola-source-light:local .
nerdctl build --build-arg CONTAINER_ENGINE=nerdctl -f deploy/container/Dockerfile -t egon-cola-source-light:local .
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
  ./mvnw -q -DskipTests compile exec:java \
  -Dexec.mainClass=top.egon.cola.archetype.source.light.start.config.encryption.ConfigCipherCli
```

`ConfigCipherCli` 不接受任何参数，明文从标准输入读取。`exec:java` 在 Maven 自身的 JVM 中运行，
上面的管道才能送达；`spring-boot:run` 这类会派生子进程的运行方式无法给该 CLI 提供可用的标准输入。

将输出的 `ENC(v1:...)` 值写入配置。请通过环境变量、挂载文件、`config/application-secrets.yml` 或 `configtree:/run/secrets/` 提供真实密钥；不要提交凭据或解密密钥。

## 原生 RPC 与 DDC 配置

10 个 unary 操作定义于 `src/main/proto/teaching_user_facade.proto`，由四个具名 RPC Provider 实现。platform OpenAPI MVC starter 保留 `/v3/api-docs` 及既有业务 HTTP 访问。文档治理默认关闭；启用 `egon.cola.component.gateway.openapi.enabled` 时，需要按平台合同补齐文档身份、发布分组和 JWT decoder，并使用 `gateway.openapi.read` scope。每个发布 handler 还必须显式声明 `@Operation(operationId = "...")`；现有示例业务 Controller 需完成该编目后才能开启治理。

`dev` 和 `prod` 使用部署方已有的 DDC RPC 服务（`DDC_RPC_TARGET`）和 DDC Redis 服务，还需提供注册 resource URI 及独立的 runtime/registry HMAC 凭据。RPC/DDC 配置前缀分别为 `egon.cola.component.rpc`、`egon.cola.component.ddc`；应用身份由 `APP_NAME`、`APP_ENV`、`DDC_BIZ_CODE`、`DDC_APP_CODE`、`INSTANCE_ID` 指定。advertised host 必须能被消费者访问。本工程不附带 DDC 容器。`test` 关闭 RPC provider/consumer、DDC config/registry/Redis、HTTP 注册及文档发布。

生产环境启用 RPC 和 DDC mTLS：通过对应的 `RPC_*`、`DDC_RPC_*` 变量传入证书链、私钥和信任证书路径，并将证书文件挂载到容器中的相同路径。开发环境显式允许明文。部署前填写 `deploy/env/.env.prod.example` 中的空白项，真实凭据通过环境变量或挂载文件提供。Compose 保留 PostgreSQL、Redis、RabbitMQ 及原有数据卷。静态和模块测试不能证明真实 DDC 注册、TLS 互通或容器就绪。

DDC Provider 和 HTTP 注册还需获取带 `ddc:registration:write` 的 IdP SERVICE Token。填写 `.env` 样例中的 `IDP_*` 字段，并配置 Spring OAuth2 Client 的 `ddcregistration` registration/provider，使用 `client_credentials`、`client_secret_basic`、client ID/secret 和 token URI。Compose 已映射标准 Spring 环境变量；直接 Java 启动时，请通过外部配置提供 `spring.security.oauth2.client.registration.ddcregistration` 及 `spring.security.oauth2.client.provider.ddcregistration.token-uri`。app ID、resource server ID/URI、registration resource URI 必须与部署方的 IdP/DDC 保持一致。IdP 的 Servlet filter 自动注册关闭，以保留业务 HTTP 行为；显式启用后，平台文档安全链仍负责文档权限。

排除默认 Redisson 自动配置，由 DDC 管理显式配置的 Redis client；业务 Redis 保持 Spring 原有 connection factory。关闭 DDC 的测试环境不会因此自动建立 Redis 连接。
