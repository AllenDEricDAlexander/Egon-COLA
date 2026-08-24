# ${rootArtifactId}

`${rootArtifactId}` 是开源 Web 版组织管理 Project，保持领域优先布局并生成七个
Maven 模块。Gateway 属于外部部署平台，生成物不包含 Gateway 组件、路由或
`spring-cloud-starter-gateway`。

## 模块

```text
${rootArtifactId}-common
${rootArtifactId}-domain
${rootArtifactId}-application
${rootArtifactId}-facade
${rootArtifactId}-infrastructure
${rootArtifactId}-adapter
${rootArtifactId}-starter
```

`facade` 保存本地 Proto 源码和 Dubbo Triple 生成类；Adapter 实现十个组织 Provider
方法；Infrastructure 负责 MyBatis-Plus、ShardingSphere、Redis/MQ adapter 和
标准 gRPC Evaluation client。

## 开源依赖边界

技术矩阵为 Spring Boot 3.5.16、Spring Cloud 2025.0.3、Spring Cloud Alibaba
2025.0.0.0、Dubbo 3.3.6、Nacos 3.0.3（Compose 镜像）、gRPC 1.73.0、Protobuf 3.x、
MyBatis-Plus 3.5.17、ShardingSphere 5.5.3、Springdoc 2.8.17。直接使用的 Egon
组件只有 Common core/ID 与 Dynamic Thread Pool starter。

项目明确不使用 Spring Data JPA、Flyway、Liquibase、Gateway starter、UUID 生成器，
也不依赖外部 organization/evaluation facade artifact。`OpenArchitectureTest` 和
生成物 verifier 会重复检查这些边界。

## ID 与数据库

业务 ID 统一为 `Long`，由 Common Snowflake `LongIdGenerator` 生成。每个运行实例必须
设置唯一 `EGON_ID_MACHINE_ID`；test profile 使用机器号 `0`。

持久化使用 MyBatis-Plus Mapper XML 与 ShardingSphere 5.5.3，支持 `SHARDING` 与
`SHARDING_READWRITE`。`spring.sql.init.mode=never`，表结构由 DBA 手工执行：

```text
student-management-organization-infrastructure/src/main/resources/db/manual/postgresql/
├── README.md
├── master-data/001__create_organization_master_data_schema.sql
└── shard/002__create_organization_sharded_schema.sql
```

应用不会创建或迁移表。执行 SQL 前备份每个物理 primary，失败即停止；回退使用备份
恢复或评审通过的前向 SQL。

## RPC 契约

以下六个文件是唯一 wire contract，并会复制到生成项目：

```text
facade/src/main/proto/organization/v1/user.proto
facade/src/main/proto/organization/v1/teaching.proto
facade/src/main/proto/evaluation/v1/course.proto
facade/src/main/proto/evaluation/v1/exam.proto
facade/src/main/proto/evaluation/v1/score.proto
facade/src/main/proto/google/protobuf/empty.proto
```

组织服务使用 Dubbo Triple，group 为 `student-management-organization`、version 为
`1.0.0`，dev/prod 协议为 `tri`。Evaluation 使用带 managed channel 和可配置 deadline
的 `GrpcEvaluationQueryClient`，不做隐式重试；测试用 in-process gRPC server。

## HTTP、GraphQL 与 OpenAPI

Controller 和 GraphQL Resolver 只位于 Adapter：

```text
/api/v1/**
/graphql
/v3/api-docs
/swagger-ui.html
```

Starter 提供 Springdoc 文档与 UI。test profile 使用 H2、本地 cache、本地事件发布和
确定性的 Evaluation stub，不打开 Nacos、Redis、RabbitMQ 或远程 gRPC socket。

## DTP 与运行 profile

Starter 只创建一个 Boot `applicationTaskExecutor`，安装 `DtpTaskDecorator`，默认池边界为：

```text
core=8，max=32，queue=1000，keep-alive=60s
```

dev/prod 启用 Dynamic Thread Pool Redis registry/report；test 关闭 DTP 外部连接但仍
验证 executor、decorator 和 ID generator。Compose 示例使用
`nacos/nacos-server:v3.0.3`，并要求显式机器号。

## 命令

```bash
bash ./mvnw -B -ntp clean verify
bash ./mvnw -pl ${rootArtifactId}-starter -am spring-boot:run
docker compose --env-file deploy/env/.env.example \
  --file deploy/compose/compose.docker.yaml up -d --build
```

第一条命令会运行模块测试、Proto/Triple 测试、OpenAPI/DTP context 测试和 ArchUnit。
这些测试不等价于真实 Nacos、Redis、PostgreSQL、RabbitMQ、跨 JVM RPC 或外部 Gateway
拓扑证明；dev/prod 的运维服务和密钥由操作者提供。
