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
${rootArtifactId}-facade          # 本地 Proto、Dubbo Triple 生成契约
${rootArtifactId}-infrastructure  # MP Mapper、ShardingSphere、Redis/MQ/gRPC adapter
${rootArtifactId}-adapter          # MVC、GraphQL、入站 MQ、Triple Provider
${rootArtifactId}-starter          # Spring Boot 组装、配置、Async/DTP/OpenAPI
```

依赖方向固定为：

```text
facade -> (protobuf / Dubbo / gRPC only)
domain -> common
application -> domain
infrastructure -> domain + facade
adapter -> application + facade
starter -> adapter + infrastructure
```

Domain 与 Application 不依赖 Mapper、Redis、MQ、Dubbo、gRPC、Springdoc、
ShardingSphere 或任何 Web 框架。Infrastructure 不反向依赖 Adapter；Starter 不放
业务逻辑。生成的 `OpenArchitectureTest` 以 ArchUnit 重复证明这些规则。

## 3. ID、持久化与手工 SQL

所有业务主键和关联键均为 `Long`，由 `top.egon.cola.component.common.id` 的
`LongIdGenerator`/Snowflake 实现生成。运行实例必须设置唯一的
`EGON_ID_MACHINE_ID`；测试 profile 固定为 `0`，不会使用 `UUID.randomUUID()`。

持久化使用 MyBatis-Plus 3.5.17 与 ShardingSphere 5.5.3：

- PO 使用 `@TableName`/`BaseMapper`，SQL 位于 Mapper XML；
- `school_classes`、`school_class_users` 按 `grade_id` 路由，主数据表固定到
  `master_data`；
- 支持 `SHARDING` 与 `SHARDING_READWRITE` 两种拓扑；
- `spring.sql.init.mode=never`，模板不使用 Spring Data JPA、Flyway 或 Liquibase；
- DBA 按 `deploy/sql/mysql-master.sql` 与 `deploy/sql/mysql-shard.sql` 手工更新
  表结构，失败停止，回退依靠备份或新的前向 SQL。

## 4. Proto 与 RPC

`facade/src/main/proto` 是唯一 wire contract，包含：

```text
organization/v1/user.proto
organization/v1/teaching.proto
evaluation/v1/course.proto
evaluation/v1/exam.proto
evaluation/v1/score.proto
google/protobuf/empty.proto
```

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
拒绝 Gateway、JPA、Flyway/Liquibase、UUID 生成器、外部 facade artifact 和第二个
executor。

这些测试证明的是生成物源码、依赖图和本地/H2/in-process 行为；它们不等价于真实
Nacos、Redis、PostgreSQL、RabbitMQ、跨 JVM Triple/gRPC 或生产 Gateway 拓扑证明。
