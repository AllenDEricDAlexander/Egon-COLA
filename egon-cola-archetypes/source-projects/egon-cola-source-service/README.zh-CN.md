# egon-cola-source-service

[English](README.md) | 中文

`egon-cola-source-service` 是一个只面向 service 的 COLA 示例，覆盖 Course、Schedule、Exam、Paper 和 Score 流程。业务流量通过 COLA native unary RPC RPC 或 RabbitMQ 进入；HTTP 仅用于 Spring Boot Actuator 管理端点。

## Maven Profiles 与外部启动参数

每次选择 `dev`、`test`、`prod` 中一个环境 profile，未指定时使用 `dev` 默认值。
这些 profile 控制应用启动，Surefire 仍使用原有 `test` 配置。堆内存参数是示例值，部署时按资源调整。

在项目根目录执行，多模块项目先安装兄弟模块依赖：

```bash
mvn install
mvn -pl egon-cola-source-service-starter -Pdev spring-boot:run
mvn -Pprod -Drun.jvm-args="-Xms1g -Xmx2g" \
  -Drun.server-port=8081 \
  -Drun.config-location=file:/etc/myapp/override.yml package
```

Maven 在 `process-resources` 阶段生成 `egon-cola-source-service-starter/target/launch.args`，打包时也会生成。
切换 profile 或 `-Drun.*` 参数后，无须 `clean` 即可更新文件。此执行仅使用 `@...@`
过滤 `src/main/launch/launch.args`，现有 YAML 占位符继续在运行时解析，参数文件不进入 JAR。

将可执行 JAR 与 `launch.args` 一起部署，JAR 可重命名为 `app.jar`，然后在部署目录执行：

```bash
java @launch.args -jar app.jar
java @launch.args -Xmx3g -jar app.jar --server.port=9081
```

文件记录 JVM 参数、Spring profile、端口及额外配置位置。这些显式系统属性优先于对应环境变量；
`-Drun.*` 覆盖 Maven 默认值，JVM 覆盖参数放在 `-jar` 前，Spring 命令行覆盖参数放在 JAR 后。
单独 `java -jar` 或 IDE 直接运行 main 不会自动读取该文件。相对配置路径以启动工作目录为准，
并非参数文件所在目录；部署建议使用绝对 `file:` 路径，Windows 路径使用正斜杠。
额外文件补充 `application.yml` 和选中的 `application-{profile}.yml`，不替换默认配置位置；
要求文件必须存在时去掉 `optional:`。密码和密钥继续通过现有环境变量/secrets 注入，不写入 `run.*`。

## 模块职责

持久化使用 Common MP Repository 与显式 Mapper XML；Domain Service 不依赖技术 CRUD 类型。

## 领域优先包布局

业务代码先按领域组织，再按技术职责组织：

```text
domain/exam/entities
application/course/manage
infrastructure/exam/repo/dao
infrastructure/exam/service/impl
adapter/course/facade/impl
adapter/exam/mq
```

该项目保持 service-only：业务流量通过 COLA native unary RPC 或 RabbitMQ 进入，不包含业务 Controller、Web Filter、GraphQL 或 VO 包。外部 Organization 边界位于 `domain/client/organization` 和 `infrastructure/client/organization`。

允许的内部依赖图为：

```text
Common <- Domain <- Application <- Adapter <- Canonical Evaluation Facade
          Domain <- Infrastructure <- Canonical Organization Facade
          Adapter <- Starter -> Infrastructure
```

更精确地说：Domain 只依赖 Common；Application 和 Infrastructure 只依赖 Domain；Adapter 只依赖 Application。Adapter 实现外部 Evaluation Facade 契约，Infrastructure 消费外部 Organization Facade 契约，两个已发布 Facade 都不依赖当前生成项目。Starter 是组合根，因此不存在 Web/Service Maven 循环依赖。

## 示例流程

- Course RPC 创建具有唯一规范化 code 的课程，读取课程、分页查询课程，并在时间范围不重叠的情况下安排课程。
- Exam RPC 为课程创建考试，关联一张试卷，并且只在试卷就绪后发布考试。
- Score RPC 记录并查询经过校验的分数。RabbitMQ score command 通过 `RecordScoreConsumer` 进入，然后委托给同一个 Application 用例。
- Domain publisher port 描述课程安排、考试发布和分数记录；Infrastructure 提供本地或 RabbitMQ 实现。

RabbitMQ 支持有意保持为基础传输能力。示例不承诺重试、死信队列、幂等 inbox、事务 outbox，或超出 broker 配置行为之外的投递保证。

## Profile 与集成

`dev` 是本地工作站开发和 `feature/*` 分支验证的默认 profile，使用由环境变量提供的 PostgreSQL、RabbitMQ 和 COLA RPC 集成。

Maven 测试会自动选择 `test`，`dev`、`release/*` 和 `hotfix/*` 分支的测试流水线也使用该 profile。它使用 PostgreSQL 兼容模式的 H2，关闭 RabbitMQ publisher 和 listener，并选择确定性的 `OrganizationDirectoryPort` stub，因此不需要 RabbitMQ、PostgreSQL 或外部 COLA RPC provider。

Organization Facade client 仍是一个暂未使用的 infrastructure 基础能力；当前没有 Application 用例调用 Organization port。

`prod` 仅用于 `main` 分支的运行时构建和部署。`dev` 与 `prod` 都选择真实的 Organization COLA RPC client，通过生成的 POM 固定 `top.egon:egon-cola-organization-facade`，并在 provider 不可用时显式失败。请通过环境变量配置，不要提交敏感信息：

- 数据库：按下文为 `master_data`、`shard_0`、`shard_1` 配置 ShardingSphere 物理数据源。
- Tianshu：使用 `TIANSHU_RPC_TARGET`、`TIANSHU_NAMESPACE`、独立的 runtime/registry HMAC 凭据和 Tianquan-Shoubing SERVICE Token 配置；`TIANSHU_ENABLED` 与 `TIANSHU_REGISTRY_ENABLED` 分别控制配置及服务注册。连接参数详见下方“原生 RPC、Tianshu 与远程查询”。
- COLA RPC：`TIANSHU_RPC_TARGET`、`RPC_PORT`、`ORGANIZATION_FACADE_TIMEOUT_MS`。
- Organization Facade：`ORGANIZATION_FACADE_ENABLED`、`ORGANIZATION_FACADE_GROUP`、`ORGANIZATION_FACADE_SERVICE_VERSION`。
- RabbitMQ：连接参数通过 Spring 自身的变量名绑定——`SPRING_RABBITMQ_HOST`、`SPRING_RABBITMQ_PORT`、`SPRING_RABBITMQ_USERNAME`、`SPRING_RABBITMQ_PASSWORD`；`RABBITMQ_ENABLED` 与 `RABBITMQ_LISTENER_AUTO_STARTUP` 则是本应用自己的开关。
- 配置解密：`EGON_CONFIG_DECRYPT_KEY`、`EGON_CONFIG_DECRYPT_KEY_FILE` 或文档化的 config-tree secret source。

## 验证与打包

```bash
SPRING_PROFILES_ACTIVE=test bash ./mvnw -B -ntp clean verify
SPRING_PROFILES_ACTIVE=test bash ./mvnw -B -ntp -DskipTests package
```

测试套件包括 Domain 规则、Application 编排、MyBatis-Plus DAO 契约、日期序列 Flyway
migration 契约、无 broker 的 MQ adapter、实际 COLA native unary RPC proxy 调用、
无外部依赖的 Spring context 组装和架构依赖检查。构建镜像不会启动服务。

必须使用 `verify` 而不是 `test`：架构治理插件绑定在 `verify` 阶段并以
`unknownLayerPolicy=FAIL` 运行，`clean test` 会跑完全部单元测试但完全不做分层检查。
生成的 `.github/workflows/ci.yml` 与根目录 `Jenkinsfile` 也因此统一使用 `verify`。

使用通过 `EGON_CONFIG_DECRYPT_KEY` 或 `EGON_CONFIG_DECRYPT_KEY_FILE` 提供的 32 字节密钥加密配置值：

```bash
printf '%s' 'plain-text' | EGON_CONFIG_DECRYPT_KEY='replace-with-32-byte-secret-key' \
  bash ./mvnw -q -pl egon-cola-source-service-starter -am -DskipTests compile exec:java \
  -Dexec.mainClass=top.egon.cola.archetype.source.service.starter.config.encryption.ConfigCipherCli
```

`ConfigCipherCli` 不接受任何参数，明文从标准输入读取。将输出的 `ENC(v1:...)` 值填入配置。

## 容器交付

生成的项目使用一个从源码构建的 `deploy/container/Dockerfile`：

```bash
docker build --build-arg CONTAINER_ENGINE=docker -f deploy/container/Dockerfile -t egon-cola-source-service:local .
podman build --build-arg CONTAINER_ENGINE=podman -f deploy/container/Dockerfile -t egon-cola-source-service:local .
nerdctl build --build-arg CONTAINER_ENGINE=nerdctl -f deploy/container/Dockerfile -t egon-cola-source-service:local .
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

## 范围边界

该生成的 service 项目不包含业务 Controller、Web Filter、GraphQL endpoint、native grpc-java 模块或启用的 H2 console。Organization Facade client 有意未接入当前 Application 行为。

## 原生 RPC、Tianshu 与远程查询

本工程使用共享 evaluation 的 11 个 Protobuf unary 操作。Provider 继续调用原有 Facade；远程查询通过既有领域端口、MapStruct/BaseConverter 和组件的 DIRECT proxy/strategy 工厂完成。配置 `app.integrations.organization` 下的 biz-code、app-code、group/version 与 timeout-ms；`ORGANIZATION_FACADE_APP_CODE` 必须填写对端在 Tianshu 中注册的实际 app code。调用使用当前进程 env，默认版本为 `1.0`、最多 3000ms（同时受组件 timeout 上限约束）、retries=0、FAIL_CLOSED，无外部协议回退。

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

Facade contracts: `top.egon:egon-cola-evaluation-facade` (local Evaluation) and `top.egon:egon-cola-organization-facade` (Organization client).

## 二级缓存骨架（默认关闭）

生成工程已内置 `egon-cola-component-common-cache-spring-boot-starter` 依赖与 `egon.cola.component.cache` 键块（`enabled: false`），不显式开启即运行行为零变化。启用两步：①装配 `RedissonClient` Bean——starter 绝不自建客户端，`enabled: true` 而客户端缺位时 fail-fast（按名优先、唯一兜底）；②置 `egon.cola.component.cache.enabled: true`。`CourseRepository` 已示范 `EgonColaCachePort` 端口注入：声明式缓存读直接调用 `getByCache`/`listByCache`，受控写方法提交后自动跨节点失效对应 `tenantId:id` 键（谓词更新失效租户前缀）。配置键、TTL 与 Redis 事件语义以组件文档为单一事实源：[cache starter README](../../../egon-cola-components/egon-cola-component-common/egon-cola-component-common-cache-spring-boot-starter/README.md)。
