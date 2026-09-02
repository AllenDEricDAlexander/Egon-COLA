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

持久化使用 `egon-cola-component-common-mybatis-plus-spring-boot-starter` 和 ShardingSphere `5.5.3`。Domain service contract 使用 Common 的 `EgonColaIService`；infra service impl 继承 `EgonColaServiceImpl`，DAO 继承 `EgonColaMapper`。明确不使用 Spring Data JPA、`JpaRepository`、`jakarta.persistence`、Flyway 或自动刷表器。PostgreSQL 建表和索引脚本按顺序放在 `infrastructure/src/main/resources/db/manual/postgresql`，按照该目录 `README.md` 的 DBA 顺序手工针对物理 primary 执行；应用启动不会创建或更新表。

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

生成测试覆盖 Proto descriptor、Long identity、Common MyBatis-Plus DAO/service、ShardingSphere H2 路由、手工 SQL 约定、11 个 Triple provider、标准 gRPC unary interop、Organization client/stub、DTP executor 上下文和 ArchUnit 依赖方向。ArchUnit 取代内部 bytecode Maven plugin，并检查 service-only、无 JPA、无 Flyway、无 Gateway、无 Springdoc 边界。

这些检查只证明源码、生成工程和本地测试；不证明真实 PostgreSQL schema、Redis DTP registry、Nacos 拓扑、RabbitMQ、跨 Project provider、部署网络或生产权限。archetype 生成和上述命令不会自动启动应用，也不会执行数据库 SQL。
