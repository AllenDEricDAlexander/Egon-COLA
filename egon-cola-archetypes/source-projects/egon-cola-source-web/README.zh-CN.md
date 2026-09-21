# egon-cola-source-web

[English](README.md) | [中文](README.zh-CN.md)

`egon-cola-source-web` 由 `egon-cola-archetype-web` 生成，是一个独立的、只负责组织领域的 Project。Adapter 实现本工程自有 Facade 模块发布的 Organization 契约，Infrastructure 消费对端工程发布的 Evaluation 契约。

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
application/teaching/manage/impl
application/user/pojo/command
infrastructure/teaching/po
infrastructure/user/repo
adapter/teaching/pojo/dto
adapter/user/facade/impl
```

共享运行时能力保留在各自的层根包中。外部 Evaluation 边界位于 `infrastructure/client/evaluation`，这是有意保留的例外；领域侧只持有 `domain/teaching/service` 端口与值对象。

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

持久化使用 Common MP Repository 与显式 Mapper XML；Domain Service 不依赖技术 CRUD 类型。

## 领域

完整的 `user` 垂直领域负责创建和查询用户、分配角色、授予权限、缓存用户读取结果，并发布已提交的变更。

完整的 `teaching` 垂直领域负责创建和查询成绩与学校班级、缓存查询结果，并发布已提交的变更。用户与班级的分配是一个同时使用两个 Domain repository 的 Application 事务；`school_class_users` 是权威的成员关系。

## 集成职责

- Adapter 负责 HTTP `/api/v1/**`、GraphQL `/graphql`、入站 RabbitMQ command、COLA RPC Facade export、请求校验、过滤器和协议转换。
- Infrastructure 负责 Common MyBatis-Plus 持久化、Flyway、Redis adapter、出站 RabbitMQ event、Evaluation Facade 防腐 adapter、本地 fallback adapter，以及 Application 方法日志 AOP。
- Starter 负责 OpenAPI 组装、运行时 profile、Actuator、Prometheus、Jackson、异步执行和配置解密。
- Organization 契约由本工程自己的 `top.egon.internal.archetype.source:egon-cola-source-web-facade` 模块发布；被消费的 Evaluation 契约仍是独立发布的工件，由生成的 POM 通过 `evaluation-facade.group-id`、`evaluation-facade.artifact-id`、`evaluation-facade.version` 与 `evaluation-facade.package` 属性解析，这些属性必须在生成时显式给出。两个契约都不会作为本地模块重复生成。

生成的 `EvaluationQueryPort` 是暂未使用的集成基础能力；当前没有 Application 用例调用它。

RabbitMQ command 使用总计三次尝试、有限退避和死信队列。领域事件在提交后发布。该示例会报告事件发布耗尽，但不声称具备事务 outbox 的投递保证。

## 运行时 Profile

`dev` 是本地工作站开发和 `feature/*` 分支验证的默认 profile，使用由环境变量提供的 PostgreSQL、Redis、RabbitMQ、外部 Tianshu 和 COLA RPC 集成。

Maven 测试会自动选择 `test`，`dev`、`release/*` 和 `hotfix/*` 分支的测试流水线也使用该 profile。它使用 PostgreSQL 兼容模式的 H2、内存缓存/幂等 adapter、本地事件发布器、确定性的 Evaluation 查询 stub、已关闭的 RabbitMQ 与 外部 Tianshu 连接，以及已关闭的 COLA RPC provider/consumer 与 registry。

`prod` 仅用于 `main` 分支的运行时构建和部署。`dev` 与 `prod` 都使用 COLA RPC Evaluation Facade client，超时 3000 ms、重试次数为 0，并在启动时检查引用。每个被消费的 Facade 都有各自的 group 变量：`EVALUATION_COURSE_FACADE_GROUP`（默认 `course`）、`EVALUATION_EXAM_FACADE_GROUP`（默认 `exam`）、`EVALUATION_SCORE_FACADE_GROUP`（默认 `score`），版本号为 `EVALUATION_FACADE_SERVICE_VERSION`（默认 `1.0.0`）。

- Tianshu：使用 `TIANSHU_RPC_TARGET`、`TIANSHU_NAMESPACE`、独立的 runtime/registry HMAC 凭据和 Tianquan-Shoubing SERVICE Token 配置；`TIANSHU_ENABLED` 与 `TIANSHU_REGISTRY_ENABLED` 分别控制配置及服务注册。连接参数详见下方“原生 RPC、Tianshu 与远程查询”。

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

本工程发布自有 Facade 模块中的 Protobuf 契约所包含的 10 个 organization unary 操作。每个具名 `*FacadeImpl` 都是一个契约唯一的 native provider，并继续调用既有用例；远程查询通过既有领域端口、MapStruct/BaseConverter 和组件的 DIRECT proxy/strategy 工厂完成。配置 `organization.integrations.evaluation` 下的 biz-code、app-code、group/version 与 timeout-ms；`EVALUATION_FACADE_APP_CODE` 必须填写对端在 Tianshu 中注册的实际 app code。调用使用当前进程 env，默认版本为 `1.0`、最多 3000ms（同时受组件 timeout 上限约束）、retries=0、FAIL_CLOSED，无外部协议回退。

`dev`/`prod` 需提供已有 Tianshu RPC/Redis 服务、注册 resource URI、runtime/registry HMAC 凭据，以及具备 `tianshu:registration:write` 的 Tianquan-Shoubing SERVICE Token client。填写 `.env` 样例中的 `TIANSHU_*`、`TIANQUAN_SHOUBING_*`、RPC/HTTP advertised host；Compose 已映射 Spring OAuth2 Client 的 `tianshuregistration` registration/provider。直接 Java 启动时，须通过外部配置提供对应的 `spring.security.oauth2.client.registration.tianshuregistration` 和 `spring.security.oauth2.client.provider.tianshuregistration.token-uri`。生产启用 RPC/Tianshu mTLS，请按环境变量配置并挂载证书链、私钥和信任证书文件。Tianshu/Tianquan-Shoubing 服务不随 Compose 创建。

文档由 platform OpenAPI MVC starter 提供，现有业务 HTTP 访问保持。文档治理默认关闭；开启前须配置平台身份、发布分组、JWT decoder/`yuheng.openapi.read` scope，并为已发布 handler 显式补齐 `@Operation(operationId = "...")`。Tianquan-Shoubing Servlet filter 自动注册关闭。HTTP 注册端口跟随实际 `server.port`。

`test` 关闭 RPC provider/consumer、Tianshu config/registry/Redis、HTTP 注册和外部查询客户端，保留既有 H2/本地 stub。默认 Redisson 自动配置被排除，由 Tianshu 创建其显式配置的 Redis client。原 PostgreSQL、Redis、RabbitMQ 及数据卷保持。配置解密在 Spring Boot Config Data 加载后执行，使用显式 import/configtree 替代旧 bootstrap；加解密与密钥规则不变。静态、模块和进程内 RPC 测试不能证明真实 Tianshu/Tianquan-Shoubing、TLS 或容器互通。

## Repository、CQRS 与 PostgreSQL

本脚手架使用 MyBatis-Plus 3.5.16：Domain Service 保留业务语义，具体 Repository 继承 `EgonColaRepository`，Mapper 继承 `EgonColaMapper`；查询全部使用显式 XML。PO 继承 `EgonModel` 的 id、tenantId、创建/更新用户与时间、`LocalDateTime deletedAt`、`Long version`。活动行是 NULL，软删写 UTC 时间戳并递增版本。AR/QueryChain 不启用，技术元数据强制填充；枚举与字段 handler 遵循 Common 合同。

`APP_DATASOURCE_MODE` 支持 `SHARDING` 与 `SHARDING_READWRITE`，事务类型为 LOCAL。单表使用明确的 `!SINGLE group.schema.table`；广播表只读。默认 legacy tenant 路由保持原地址。可选两级模板见 `src/test/resources/sharding/two-level-readwrite.yml`（多模块项目在 infrastructure 中）：先按 tenant_id 散列到 tenant slot，再按业务根 ID 散列到 bucket。订单与明细分别使用 id/order_id 共享同一根语义；同租户固定在一个物理组。

算法固定为 mix64-v1，T/B 是不超过 1024 的二次幂，乘积不超过 4096。库间均衡还依赖均衡 slot map 和租户负载；没有自动重分布。Query 缺次级键/范围查询受 fanout 上限约束，Command 必须有精确键。修改分布配置需配套新建表/迁移设计，不能直接套用测试模板到已有业务库。

初始化由 `ShardingDataSourceBootstrapper` 调用 Common 受管 DDL runner，仅对物理 PRIMARY 执行 `db/egon-mp/V20260913_001__initialize_repository_schema.sql` 与 `repository-manifest.json`。空库首次初始化；受管库验证 checksum/前缀/路由指纹；非空未受管库报 REBUILD_REQUIRED。旧 B/V/manual SQL 原样保留作档案，不再作为本脚手架运行入口。

读写分离使用 PostgreSQL 自身复制，普通读走 ROUND_ROBIN 副本，事务读/锁定读/强制主库读走 PRIMARY；不自动创建副本或故障选主。单表、广播表及业务物理表均携带 tenant_id。跨物理组写入会拒绝并标记回滚。

每个实例配置唯一 `EGON_ID_MACHINE_ID`，生产使用现有 Common Snowflake。各 profile 保持同一 MP 配置键；dev 才开启诊断，原始 recorder logger 为 OFF。动态表名默认关闭，只接受明确映射；MybatisBatch 在调用方事务中执行。

默认测试使用隔离 H2 和受控依赖。真实路由测试需 `-Degon.pg.routing=true` 与 `EGON_TEST_PG_URL`；主从测试需 `-Degon.pg.readwrite=true` 与 `EGON_TEST_PG_PRIMARY_URL`、`EGON_TEST_PG_REPLICA_URL`，并提供专用 `EGON_TEST_PG_USER/PASSWORD`。它们只创建/清理自己的 UUID schema，不启动数据库。PG/SS 运行、迁移和性能 EXPLAIN 由使用者手动验收，跳过不表示通过。

## 二级缓存

`base`、`dev`、`prod` 均以 `egon.cola.component.cache.enabled=true` 交付；`test` 保留同样的键但设为 `enabled: false`，让单元与模块测试不依赖 Redis。`infrastructure/config/OrganizationRedisConfig.java` 已带 `@EnableCaching`，配置好 Redis 连接即可直接使用二级缓存。mp-sd-ext 基类已移除缓存端口耦合，具体
Repository 通过 `@CacheConfig`、`@Cacheable`、`@CacheEvict` 等注解声明策略；已有 Repository 示例使用 `findCachedById` /
`updateCachedById`（Agent 按业务自行声明）。普通 CRUD
不再隐式失效缓存，其他写入和删除入口也须声明失效。所有 profile 都声明同一组五个 TTL 键（`l1-expire`、`l1-jitter`、`l2-expire`、`l2-jitter`、`null-expire`）以及共享的 `key-prefix`、`tenant-mdc-key` 与批量/锁预算，只有取值随环境不同。Key、条件、组合操作、事务与同步加载限制详见 [cache starter README](../../../egon-cola-components/egon-cola-component-common/egon-cola-component-common-cache-spring-boot-starter/README.md)。
