# egon-cola-source-service-open

[English](README.md) | 中文

`egon-cola-source-service-open` 是基于开源 Spring Boot 3.5、Spring Cloud Alibaba、Dubbo Triple、MyBatis-Plus 和 ShardingSphere 的纯 Service Open 示例，覆盖 Course、Schedule、Exam、Paper、Score 流程。业务流量通过 Dubbo Triple 或 RabbitMQ 进入；HTTP 只保留 Spring Boot Actuator 管理端点。


生成的 Maven reactor 包含一个父 POM 和七个模块：

```text
egon-cola-source-service-open-common        本地错误/常量/枚举
egon-cola-source-service-open-facade        本地 Proto wire contract 与 Triple codegen
egon-cola-source-service-open-domain         聚合、值对象、端口
egon-cola-source-service-open-application    用例编排
egon-cola-source-service-open-infrastructure MyBatis-Plus、分片、MQ、RPC client
egon-cola-source-service-open-adapter        Proto RPC provider、转换器、MQ consumer
egon-cola-source-service-open-starter         Boot 组装、profile、运行治理
```

业务代码保持领域优先：`domain/<业务域>`、`application/<业务域>`、`infrastructure/<业务域>`、`adapter/<业务域>`。依赖方向为：`facade` 只放契约；`domain -> common`；`application -> domain`；`adapter -> application/facade`；`infrastructure -> domain/facade`；`starter` 是组合根。


`facade/src/main/proto` 是唯一 RPC wire source。五个业务 Proto 文件在 `egon.evaluation.v1` 和 `egon.organization.v1` 下定义 8 个 service、21 个 unary 方法；Dubbo Maven plugin `3.3.6` 使用 `tri` 生成代码。ID 统一为正 `int64`，时间使用 `Timestamp`，无返回值使用 `google.protobuf.Empty`，分页固定包含 `records/current_page/total_pages/page_size/total_count`。目录中额外提供 wire-compatible 的 `google/protobuf/empty.proto`，仅用于 Dubbo 3.3.6 codegen。

Evaluation 的 11 个方法在同一个 Triple 端口按 `course`、`exam`、`score` group 暴露，版本为 `1.0.0`。Organization 目录调用使用相同本地 Proto 契约。测试通过标准 gRPC `ManagedChannel` 验证 unary interop；模板不启动第二个 grpc-java server。


所有技术 ID 都由 Common 的 `LongIdGenerator` 生成，PostgreSQL 中使用 `BIGINT`；Domain、Application、PO、DAO、事件和分片键内部统一使用正 `Long`。每个运行实例必须设置唯一的 `EGON_ID_MACHINE_ID`，没有运行时默认值。本 archetype 不包含 UUID 生成器或 UUID 分片算法。

持久化使用 Common MP Repository 与显式 Mapper XML；Domain Service 不依赖技术 CRUD 类型。

Evaluation 初始拓扑包含 `master_data`、`shard_0`、`shard_1`。逻辑表为 `evaluation_course`、`evaluation_course_schedule`、`evaluation_exam`、`evaluation_exam_paper`、`evaluation_score`；所有路由表都以正 `tenant_id` 同时进行分库分表，同一租户始终位于同一 database/table slot。非正 tenant ID、缺少分片键、范围路由、未知节点和不一致 node map 均快速失败。


`dev` 是本地默认 profile，依赖环境提供的 PostgreSQL、Nacos、RabbitMQ、Redis 和 Dubbo；`prod` 使用同样的契约和由运维持有的密钥；`test` 使用 PostgreSQL 兼容模式 H2、Local Organization stub，并关闭在线 Nacos、Redis、RabbitMQ 和外部 provider。

应用只有一个有界异步执行器 `applicationTaskExecutor`，默认 `core=8`、`max=32`、`queue=1000`、`keep-alive=60s`。`DtpTaskDecorator` 负责传递并清理执行上下文，Dynamic Thread Pool 在 `dev`/`prod` 中通过 Redis 管理同一个执行器；`test` 关闭 DTP 上报和 Nacos。

所有 Compose 变体都会传入明确的机器号和 DTP Redis/report 参数。Nacos 固定使用 `nacos/nacos-server:v3.0.3`；凭据和密码放在 `deploy/env/.env.example` 或运维持有的生产 env 文件中。

常用变量包括：

- Nacos：`NACOS_SERVER_ADDR`、`NACOS_NAMESPACE`、`NACOS_USERNAME`、`NACOS_PASSWORD`、`NACOS_CONFIG_ENABLED`、`NACOS_DISCOVERY_ENABLED`。
- Dubbo：`DUBBO_REGISTRY_ADDRESS`、`DUBBO_PORT`、`DUBBO_CONSUMER_TIMEOUT`。
- DTP/身份：`EGON_ID_MACHINE_ID`、`DTP_ENABLED`、`DTP_REPORT_ENABLED`、`DTP_REDIS_HOST`、`DTP_REDIS_PORT`、`DTP_REDIS_PASSWORD`。
- 数据库：`deploy/env/.env.example` 中的 `EVALUATION_SHARDING_*`。
- RabbitMQ：`SPRING_RABBITMQ_*`、`RABBITMQ_ENABLED`、`RABBITMQ_LISTENER_AUTO_STARTUP`。


在仓库根目录执行：

```bash
./mvnw -B -ntp -f egon-cola-archetypes/pom.xml \
  -pl :egon-cola-archetype-service-open -am clean integration-test
```

生成测试覆盖 Proto descriptor、Long identity、Common MyBatis-Plus DAO/service、隔离 H2 持久化与 typed ShardingSphere 路由合同、手工 SQL 约定、11 个 Triple provider、标准 gRPC unary interop、Organization client/stub、DTP executor 上下文和 ArchUnit 依赖方向。ArchUnit 取代内部 bytecode Maven plugin，并检查 service-only、无 JPA、无 Flyway、无 Gateway、无 Springdoc 边界。

这些检查只证明源码、生成工程和本地测试；不证明真实 PostgreSQL schema、Redis DTP registry、Nacos 拓扑、RabbitMQ、跨 Project provider、部署网络或生产权限。archetype 生成和上述命令不会自动启动应用，也不会执行数据库 SQL。

## Maven Profiles 与外部启动参数

每次选择 `dev`、`test`、`prod` 中一个环境 profile，未指定时使用 `dev` 默认值。
这些 profile 控制应用启动，Surefire 仍使用原有 `test` 配置。堆内存参数是示例值，部署时按资源调整。

在项目根目录执行，多模块项目先安装兄弟模块依赖：

```bash
mvn install
mvn -pl egon-cola-source-service-open-starter -Pdev spring-boot:run
mvn -Pprod -Drun.jvm-args="-Xms1g -Xmx2g" \
  -Drun.server-port=8081 \
  -Drun.config-location=file:/etc/myapp/override.yml package
```

Maven 在 `process-resources` 阶段生成 `egon-cola-source-service-open-starter/target/launch.args`，打包时也会生成。
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

## Repository、CQRS 与 PostgreSQL

本脚手架使用 MyBatis-Plus 3.5.16：Domain Service 保留业务语义，具体 Repository 继承 `EgonColaRepository`，Mapper 继承 `EgonColaMapper`；查询全部使用显式 XML。PO 继承 `EgonModel` 的 id、tenantId、创建/更新用户与时间、`LocalDateTime deletedAt`、`Long version`。活动行是 NULL，软删写 UTC 时间戳并递增版本。AR/QueryChain 不启用，技术元数据强制填充；枚举与字段 handler 遵循 Common 合同。

`APP_DATASOURCE_MODE` 支持 `SHARDING` 与 `SHARDING_READWRITE`，事务类型为 LOCAL。单表使用明确的 `!SINGLE group.schema.table`；广播表只读。默认 legacy tenant 路由保持原地址。可选两级模板见 `src/test/resources/sharding/two-level-readwrite.yml`（多模块项目在 infrastructure 中）：先按 tenant_id 散列到 tenant slot，再按业务根 ID 散列到 bucket。订单与明细分别使用 id/order_id 共享同一根语义；同租户固定在一个物理组。

算法固定为 mix64-v1，T/B 是不超过 1024 的二次幂，乘积不超过 4096。库间均衡还依赖均衡 slot map 和租户负载；没有自动重分布。Query 缺次级键/范围查询受 fanout 上限约束，Command 必须有精确键。修改分布配置需配套新建表/迁移设计，不能直接套用测试模板到已有业务库。

初始化由 `ShardingDataSourceBootstrapper` 调用 Common 受管 DDL runner，仅对物理 PRIMARY 执行 `db/egon-mp/V20260913_001__initialize_repository_schema.sql` 与 `repository-manifest.json`。空库首次初始化；受管库验证 checksum/前缀/路由指纹；非空未受管库报 REBUILD_REQUIRED。旧 B/V/manual SQL 原样保留作档案，不再作为本脚手架运行入口。

读写分离使用 PostgreSQL 自身复制，普通读走 ROUND_ROBIN 副本，事务读/锁定读/强制主库读走 PRIMARY；不自动创建副本或故障选主。单表、广播表及业务物理表均携带 tenant_id。跨物理组写入会拒绝并标记回滚。

每个实例配置唯一 `EGON_ID_MACHINE_ID`，生产使用现有 Common Snowflake。各 profile 保持同一 MP 配置键；dev 才开启诊断，原始 recorder logger 为 OFF。动态表名默认关闭，只接受明确映射；MybatisBatch 在调用方事务中执行。

默认测试使用隔离 H2 和受控依赖。真实路由测试需 `-Degon.pg.routing=true` 与 `EGON_TEST_PG_URL`；主从测试需 `-Degon.pg.readwrite=true` 与 `EGON_TEST_PG_PRIMARY_URL`、`EGON_TEST_PG_REPLICA_URL`，并提供专用 `EGON_TEST_PG_USER/PASSWORD`。它们只创建/清理自己的 UUID schema，不启动数据库。PG/SS 运行、迁移和性能 EXPLAIN 由使用者手动验收，跳过不表示通过。
