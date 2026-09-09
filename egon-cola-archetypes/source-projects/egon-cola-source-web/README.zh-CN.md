# egon-cola-source-web

[English](README.md) | [中文](README.zh-CN.md)

`egon-cola-source-web` 由 `egon-cola-archetype-web` 生成，是一个独立的、只负责组织领域的 Project。Adapter 实现 `top.egon:egon-cola-organization-facade`，Infrastructure 消费 `top.egon:egon-cola-evaluation-facade`。

## Maven Profiles 与外部启动参数

每次选择 `dev`、`test`、`prod` 中一个环境 profile，未指定时使用 `dev` 默认值。
这些 profile 控制应用启动，Surefire 仍使用原有 `test` 配置。堆内存参数是示例值，部署时按资源调整。

在项目根目录执行，多模块项目先安装兄弟模块依赖：

```bash
mvn install
mvn -pl egon-cola-source-web-starter -Pdev spring-boot:run
mvn -Pprod -Drun.jvm-args="-Xms1g -Xmx2g" \
  -Drun.server-port=8080 \
  -Drun.config-location=file:/etc/myapp/override.yml package
```

Maven 在 `process-resources` 阶段生成 `egon-cola-source-web-starter/target/launch.args`，打包时也会生成。
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

## 模块

```text
egon-cola-source-web-common
egon-cola-source-web-domain
egon-cola-source-web-application
egon-cola-source-web-infrastructure
egon-cola-source-web-adapter
egon-cola-source-web-starter
```

## 领域优先包布局

业务代码先按领域组织，再按技术职责组织：

```text
domain/user/entities
application/teaching/manage
infrastructure/user/repo
adapter/user/facade/impl
adapter/teaching/controller
```

共享运行时能力保留在各自的层根包中。外部 Evaluation 边界位于 `domain/client/evaluation` 和 `infrastructure/client/evaluation`，这是有意保留的例外。

## 依赖方向

```text
common         -> no generated module
domain         -> common
application    -> domain
infrastructure -> domain, canonical Evaluation Facade
adapter        -> application, canonical Organization Facade
starter        -> adapter, infrastructure
```

Infrastructure 实现 Domain 所有的端口。Adapter 不能直接访问 Infrastructure，Starter 只包含组装配置。两个 canonical Facade 都是独立 artifact，不依赖当前生成项目，因此 Web/Service Maven 依赖图不会形成循环。

持久化统一使用 Egon-COLA Common MyBatis-Plus starter。Domain 的 service 接口
继承 `EgonColaIService`；实现、`EgonColaMapper` DAO 和 `EgonModel` PO 全部位于
Infrastructure。PO 使用 Long 技术主键，租户、审计、逻辑删除和乐观锁由 Common
starter 提供。

## 领域

完整的 `user` 垂直领域负责创建和查询用户、分配角色、授予权限、缓存用户读取结果，并发布已提交的变更。

完整的 `teaching` 垂直领域负责创建和查询成绩与学校班级、缓存查询结果，并发布已提交的变更。用户与班级的分配是一个同时使用两个 Domain repository 的 Application 事务；`school_class_users` 是权威的成员关系。

## 集成职责

- Adapter 负责 HTTP `/api/v1/**`、GraphQL `/graphql`、入站 RabbitMQ command、COLA RPC Facade export、请求校验、过滤器和协议转换。
- Infrastructure 负责 Common MyBatis-Plus 持久化、Flyway、Redis adapter、出站 RabbitMQ event、Evaluation Facade 防腐 adapter、本地 fallback adapter，以及 Application 方法日志 AOP。
- Starter 负责 OpenAPI 组装、运行时 profile、Actuator、Prometheus、Jackson、异步执行和配置解密。
- `top.egon:egon-cola-organization-facade` 是 Provider 契约，`top.egon:egon-cola-evaluation-facade` 是消费契约；两者都不会作为本地模块重复生成。

生成的 `EvaluationQueryPort` 是暂未使用的集成基础能力；当前没有 Application 用例调用它。

RabbitMQ command 使用总计三次尝试、有限退避和死信队列。领域事件在提交后发布。该示例会报告事件发布耗尽，但不声称具备事务 outbox 的投递保证。

## 运行时 Profile

`dev` 是本地工作站开发和 `feature/*` 分支验证的默认 profile，使用由环境变量提供的 PostgreSQL、Redis、RabbitMQ、外部 Tianshu 和 COLA RPC 集成。

Maven 测试会自动选择 `test`，`dev`、`release/*` 和 `hotfix/*` 分支的测试流水线也使用该 profile。它使用 PostgreSQL 兼容模式的 H2、内存缓存/幂等 adapter、本地事件发布器、确定性的 Evaluation 查询 stub、已关闭的 RabbitMQ 与 外部 Tianshu 连接，以及已关闭的 COLA RPC provider/consumer 与 registry。

`prod` 仅用于 `main` 分支的运行时构建和部署。`dev` 与 `prod` 都使用 COLA RPC Evaluation Facade client，超时 3000 ms、重试次数为 0，并在启动时检查引用。每个被消费的 Facade 都有各自的 group 变量：`EVALUATION_COURSE_FACADE_GROUP`（默认 `course`）、`EVALUATION_EXAM_FACADE_GROUP`（默认 `exam`）、`EVALUATION_SCORE_FACADE_GROUP`（默认 `score`），版本号为 `EVALUATION_FACADE_SERVICE_VERSION`（默认 `1.0.0`）。

- Tianshu：使用 `TIANSHU_RPC_TARGET`、`TIANSHU_NAMESPACE`、独立的 runtime/registry HMAC 凭据和 Tianquan-Shoubing SERVICE Token 配置；`TIANSHU_ENABLED` 与 `TIANSHU_REGISTRY_ENABLED` 分别控制配置及服务注册。连接参数详见下方“原生 RPC、Tianshu 与远程查询”。

## 分片、读写分离与 Flyway

生成应用始终使用 ShardingSphere 逻辑数据源，支持两种路由模式：

```bash
SPRING_PROFILES_ACTIVE=dev APP_DATASOURCE_MODE=SHARDING bash ./mvnw -pl egon-cola-source-web-starter spring-boot:run
SPRING_PROFILES_ACTIVE=dev APP_DATASOURCE_MODE=SHARDING_READWRITE bash ./mvnw -pl egon-cola-source-web-starter spring-boot:run
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
- binding tables `school_classes` 和 `school_class_users` 都按正数
  `tenant_id` 使用 Common Long 租户算法分库分表。成员关系中冗余的 `gradeId`
  仍是聚合完整性的必填字段，租户上下文负责物理库和表后缀路由。
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

业务和技术主键统一由应用通过 Common `LongIdGenerator` 生成正数 `Long`；HTTP 和
GraphQL 边界仍使用十进制字符串表达。迁移文件名必须符合
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

## 错误契约

HTTP 失败使用包含 `code`、`message`、`traceId`、`timestamp` 和 `fieldErrors` 的稳定响应体。状态语义为：`400` 校验失败，`403` 禁止访问，`404` 资源不存在，`409` 冲突，`422` Domain 拒绝，`503` 必需依赖不可用，`500` 未预期失败。GraphQL 通过 error extensions 暴露相同字段；Facade 失败携带相同 code 和 trace ID。

## 命令

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
  bash ./mvnw -q -pl egon-cola-source-web-starter -am -DskipTests compile exec:java \
  -Dexec.mainClass=top.egon.cola.archetype.source.web.starter.config.encryption.ConfigCipherCli
```

`ConfigCipherCli` 不接受任何参数，明文从标准输入读取；`exec:java` 在 Maven 自身的 JVM 中运行，
上面的管道才能送达。将输出的 `ENC(v1:...)` 值填入配置。

## 容器交付

生成的项目使用一个从源码构建的 `deploy/container/Dockerfile`：

```bash
docker build --build-arg CONTAINER_ENGINE=docker -f deploy/container/Dockerfile -t egon-cola-source-web:local .
podman build --build-arg CONTAINER_ENGINE=podman -f deploy/container/Dockerfile -t egon-cola-source-web:local .
nerdctl build --build-arg CONTAINER_ENGINE=nerdctl -f deploy/container/Dockerfile -t egon-cola-source-web:local .
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
SPRING_PROFILES_ACTIVE=dev bash ./mvnw -pl egon-cola-source-web-starter spring-boot:run
```

敏感值应放在环境变量、挂载文件、`config/application-secrets.yml` 或 `configtree:/run/secrets/` 中。不要提交凭据或解密密钥。

## 原生 RPC、Tianshu 与远程查询

本工程使用共享 organization 的 10 个 Protobuf unary 操作。Provider 继续调用原有 Facade；远程查询通过既有领域端口、MapStruct/BaseConverter 和组件的 DIRECT proxy/strategy 工厂完成。配置 `organization.integrations.evaluation` 下的 biz-code、app-code、group/version 与 timeout-ms；`EVALUATION_FACADE_APP_CODE` 必须填写对端在 Tianshu 中注册的实际 app code。调用使用当前进程 env，默认版本为 `1.0`、最多 3000ms（同时受组件 timeout 上限约束）、retries=0、FAIL_CLOSED，无外部协议回退。

`dev`/`prod` 需提供已有 Tianshu RPC/Redis 服务、注册 resource URI、runtime/registry HMAC 凭据，以及具备 `tianshu:registration:write` 的 Tianquan-Shoubing SERVICE Token client。填写 `.env` 样例中的 `TIANSHU_*`、`TIANQUAN_SHOUBING_*`、RPC/HTTP advertised host；Compose 已映射 Spring OAuth2 Client 的 `tianshuregistration` registration/provider。直接 Java 启动时，须通过外部配置提供对应的 `spring.security.oauth2.client.registration.tianshuregistration` 和 `spring.security.oauth2.client.provider.tianshuregistration.token-uri`。生产启用 RPC/Tianshu mTLS，请按环境变量配置并挂载证书链、私钥和信任证书文件。Tianshu/Tianquan-Shoubing 服务不随 Compose 创建。

文档由 platform OpenAPI MVC starter 提供，现有业务 HTTP 访问保持。文档治理默认关闭；开启前须配置平台身份、发布分组、JWT decoder/`yuheng.openapi.read` scope，并为已发布 handler 显式补齐 `@Operation(operationId = "...")`。Tianquan-Shoubing Servlet filter 自动注册关闭。HTTP 注册端口跟随实际 `server.port`。

`test` 关闭 RPC provider/consumer、Tianshu config/registry/Redis、HTTP 注册和外部查询客户端，保留既有 H2/本地 stub。默认 Redisson 自动配置被排除，由 Tianshu 创建其显式配置的 Redis client。原 PostgreSQL、Redis、RabbitMQ 及数据卷保持。配置解密在 Spring Boot Config Data 加载后执行，使用显式 import/configtree 替代旧 bootstrap；加解密与密钥规则不变。静态、模块和进程内 RPC 测试不能证明真实 Tianshu/Tianquan-Shoubing、TLS 或容器互通。
