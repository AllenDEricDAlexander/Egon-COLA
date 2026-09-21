# Web Open 多工程多模块架构说明

本文档描述 `egon-cola-archetype-web-open` 生成的开源 Web Project。它保持现有
领域优先结构，但把运行时契约全部落在 Spring Boot 3.5、Spring Cloud、Spring
Cloud Alibaba、Dubbo Triple、Nacos、gRPC/Protobuf、MyBatis-Plus 和
ShardingSphere 上。

## 1. 产品边界

Web Open 是独立的组织管理 Project，包含 `user` 与 `teaching` 两个垂直领域。
Gateway 是部署平台的外部组件，生成物不包含 Gateway module、Gateway route 或
`spring-cloud-starter-gateway`。Evaluation 通过标准 gRPC 客户端调用；组织能力
通过本地 Proto 生成的 Dubbo Triple Provider 对外提供。

生成物不依赖 Egon 内部平台实现，也不生成原始 archetype 的组件/平台副本。直接
使用的 `top.egon` 依赖只有 Common 核心/ID 与 Dynamic Thread Pool starter，版本
由 `egon-cola-components-bom` 管理。

## 2. Maven 模块

```text
${rootArtifactId}-common          # 工程通用依赖入口
${rootArtifactId}-domain          # 实体、值对象、领域端口与规则
${rootArtifactId}-application     # 用例编排与事务边界
${rootArtifactId}-facade          # 自有 Organization Proto 与 Dubbo Triple 生成契约
${rootArtifactId}-infrastructure  # MP Mapper、ShardingSphere、Redis/MQ/gRPC adapter
${rootArtifactId}-adapter          # MVC、GraphQL、入站 MQ、Triple Provider
${rootArtifactId}-starter          # Spring Boot 组装、配置、Async/DTP/OpenAPI
```

依赖方向固定为：

```text
facade -> (protobuf / Dubbo / gRPC only)
domain -> common
application -> domain
infrastructure -> domain + 自有 facade + 对端已发布 facade 工件
adapter -> application + 自有 facade
starter -> adapter + infrastructure
```

Domain 与 Application 不依赖 Mapper、Redis、MQ、Dubbo、gRPC、Springdoc、
ShardingSphere 或任何 Web 框架。Infrastructure 不反向依赖 Adapter；Starter 不放
业务逻辑。生成的 `OpenArchitectureTest` 以 ArchUnit 重复证明这些规则。

## 4. Proto 与 RPC

`facade/src/main/proto` 只保留本工程拥有的 wire contract，包含：

```text
organization/v1/user.proto
organization/v1/teaching.proto
google/protobuf/empty.proto
```

`egon.evaluation.v1` 的三个 service、11 个方法不再复制到此，由对端 Service Open 工程的
facade 工件独立发布；`infrastructure` 的 `GrpcEvaluationQueryClient` 通过生成 POM 的显式属性
解析该工件：

```text
本工程自有契约：模块 <rootArtifactId>-facade
对端契约依赖：<evaluation-facade.group-id>:<evaluation-facade.artifact-id>:<evaluation-facade.version>
```

`evaluation-facade.*` 是 Web Open 模板的必填生成参数；缺失时生成必须立即失败，而不是回退到
某个默认对端。两个 `facade` 工件互不依赖，wire 侧 package、service、method 与 field 完全不变，
只有 Java 与 Maven 归属发生变化。

Dubbo 3.3.6 Triple codegen 生成组织服务。十个组织操作由 Adapter Provider 实现，
统一以 group `student-management-organization`、version `1.0.0` 导出，dev/prod
协议名为 `tri`；test 使用 `injvm` 且关闭 registry。Evaluation 的
`GrpcEvaluationQueryClient` 使用标准 gRPC unary path、managed channel 和可配置
deadline，不实现隐式重试；test 通过 in-process gRPC server 验证序列化、路径和错误
映射。

## 5. Web、GraphQL 与 OpenAPI

HTTP Controller 与 GraphQL Resolver 只位于 Adapter，路径为 `/api/v1/**` 与
`/graphql`。Starter 提供 Springdoc 2.8.17：

```text
/v3/api-docs
/swagger-ui.html
```

`OpenApiContractTest` 验证标题、版本和 users/grades/school-classes operation。
Service Open 不携带 Springdoc；Gateway 仍由外部平台持有。

## 6. DTP、Nacos 与 profile

Starter 只创建一个 `applicationTaskExecutor`，通过 Boot
`ThreadPoolTaskExecutorBuilder` 创建，并安装 `DtpTaskDecorator`。默认边界为：

```text
core-size=8, max-size=32, queue-capacity=1000, keep-alive=60s
```

dev/prod 使用 Dynamic Thread Pool Redis registry 与 report；test 将 DTP、Redis、
RabbitMQ、Nacos 外部连接关闭，但仍验证 executor、decorator、ID generator 和
配置属性存在。Compose 示例使用 Nacos `nacos/nacos-server:v3.0.3`，机器号、Redis、
Nacos、RabbitMQ 和 PostgreSQL 连接均由环境变量提供。

三个 profile 的职责如下：

| profile | 连接行为 |
| --- | --- |
| `dev` | Nacos/Redis/RabbitMQ/真实 PostgreSQL、Triple `tri` |
| `test` | H2 + 本地 cache/event/evaluation stub、Nacos/DTP 外连关闭、Dubbo `injvm` |
| `prod` | 运维提供的 PostgreSQL/Redis/RabbitMQ/Nacos、Triple `tri` |

生产环境缺少机器号、数据库、Redis 或 Nacos 必需变量时应 fail-fast；示例不提交
凭据或解密密钥。

## 7. 验证与边界

```bash
bash ./mvnw -B -ntp clean verify
```

Archetype integration-test 会生成真实七模块 Project，执行模块测试、Proto contract、
Triple Provider 配置、in-process gRPC、OpenAPI、DTP context 和 ArchUnit。静态扫描
拒绝 Gateway、JPA、Flyway/Liquibase、UUID 生成器、复制的对端 facade 协议和第二个
executor；唯一允许的外部契约是对端独立发布的 Evaluation facade 工件。

这些测试证明的是生成物源码、依赖图和本地/H2/in-process 行为；它们不等价于真实
Nacos、Redis、PostgreSQL、RabbitMQ、跨 JVM Triple/gRPC 或生产 Gateway 拓扑证明。



## Repository、CQRS 与 PostgreSQL

本脚手架使用 MyBatis-Plus 3.5.16：Domain Service 保留业务语义，具体 Repository 继承 `EgonColaRepository`，Mapper 继承 `EgonColaMapper`；查询全部使用显式 XML。PO 继承 `EgonModel` 的 id、tenantId、创建/更新用户与时间、`LocalDateTime deletedAt`、`Long version`。活动行是 NULL，软删写 UTC 时间戳并递增版本。AR/QueryChain 不启用，技术元数据强制填充；枚举与字段 handler 遵循 Common 合同。

`APP_DATASOURCE_MODE` 支持 `SHARDING` 与 `SHARDING_READWRITE`，事务类型为 LOCAL。单表使用明确的 `!SINGLE group.schema.table`；广播表只读。默认 legacy tenant 路由保持原地址。可选两级模板见 `src/test/resources/sharding/two-level-readwrite.yml`（多模块项目在 infrastructure 中）：先按 tenant_id 散列到 tenant slot，再按业务根 ID 散列到 bucket。订单与明细分别使用 id/order_id 共享同一根语义；同租户固定在一个物理组。

算法固定为 mix64-v1，T/B 是不超过 1024 的二次幂，乘积不超过 4096。库间均衡还依赖均衡 slot map 和租户负载；没有自动重分布。Query 缺次级键/范围查询受 fanout 上限约束，Command 必须有精确键。修改分布配置需配套新建表/迁移设计，不能直接套用测试模板到已有业务库。

初始化由 `ShardingDataSourceBootstrapper` 调用 Common 受管 DDL runner，仅对物理 PRIMARY 执行 `db/egon-mp/V20260913_001__initialize_repository_schema.sql` 与 `repository-manifest.json`。空库首次初始化；受管库验证 checksum/前缀/路由指纹；非空未受管库报 REBUILD_REQUIRED。旧 B/V/manual SQL 原样保留作档案，不再作为本脚手架运行入口。

读写分离使用 PostgreSQL 自身复制，普通读走 ROUND_ROBIN 副本，事务读/锁定读/强制主库读走 PRIMARY；不自动创建副本或故障选主。单表、广播表及业务物理表均携带 tenant_id。跨物理组写入会拒绝并标记回滚。

每个实例配置唯一 `EGON_ID_MACHINE_ID`，生产使用现有 Common Snowflake。各 profile 保持同一 MP 配置键；dev 才开启诊断，原始 recorder logger 为 OFF。动态表名默认关闭，只接受明确映射；MybatisBatch 在调用方事务中执行。

默认测试使用隔离 H2 和受控依赖。真实路由测试需 `-Degon.pg.routing=true` 与 `EGON_TEST_PG_URL`；主从测试需 `-Degon.pg.readwrite=true` 与 `EGON_TEST_PG_PRIMARY_URL`、`EGON_TEST_PG_REPLICA_URL`，并提供专用 `EGON_TEST_PG_USER/PASSWORD`。它们只创建/清理自己的 UUID schema，不启动数据库。PG/SS 运行、迁移和性能 EXPLAIN 由使用者手动验收，跳过不表示通过。
