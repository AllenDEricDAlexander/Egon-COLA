# egon-cola-source-web-open

`egon-cola-source-web-open` 是开源 Web 版组织管理 Project，保持领域优先布局并生成七个
Maven 模块。Gateway 属于外部部署平台，生成物不包含 Gateway 组件、路由或
`spring-cloud-starter-gateway`。


```text
egon-cola-source-web-open-common
egon-cola-source-web-open-domain
egon-cola-source-web-open-application
egon-cola-source-web-open-facade
egon-cola-source-web-open-infrastructure
egon-cola-source-web-open-adapter
egon-cola-source-web-open-starter
```

`facade` 保存本地 Proto 源码和 Dubbo Triple 生成类；Adapter 实现十个组织 Provider
方法；Infrastructure 负责 MyBatis-Plus、ShardingSphere、Redis/MQ adapter 和
标准 gRPC Evaluation client。


技术矩阵为 Spring Boot 3.5.16、Spring Cloud 2025.0.3、Spring Cloud Alibaba
2025.0.0.0、Dubbo 3.3.6、Nacos 3.0.3（Compose 镜像）、gRPC 1.73.0、Protobuf 3.x、
MyBatis-Plus 3.5.17、ShardingSphere 5.5.3、Springdoc 2.8.17。直接使用的 Egon
组件只有 Common core/ID 与 Dynamic Thread Pool starter。

项目明确不使用 Spring Data JPA、Flyway、Liquibase、Gateway starter、UUID 业务主键，
也不依赖外部 organization/evaluation facade artifact。`OpenArchitectureTest` 和
生成物 verifier 会重复检查这些边界。


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


Controller 和 GraphQL Resolver 只位于 Adapter：

```text
/api/v1/**
/graphql
/v3/api-docs
/swagger-ui.html
```

Starter 提供 Springdoc 文档与 UI。test profile 使用 H2、本地 cache、本地事件发布和
确定性的 Evaluation stub，不打开 Nacos、Redis、RabbitMQ 或远程 gRPC socket。


Starter 只创建一个 Boot `applicationTaskExecutor`，安装 `DtpTaskDecorator`，默认池边界为：

```text
core=8，max=32，queue=1000，keep-alive=60s
```

dev/prod 启用 Dynamic Thread Pool Redis registry/report；test 关闭 DTP 外部连接但仍
验证 executor、decorator 和 ID generator。Compose 示例使用
`nacos/nacos-server:v3.0.3`，并要求显式机器号。


```bash
bash ./mvnw -B -ntp clean verify
bash ./mvnw -pl egon-cola-source-web-open-starter -am spring-boot:run
docker compose --env-file deploy/env/.env.example \
  --file deploy/compose/compose.docker.yaml up -d --build
```

第一条命令会运行模块测试、Proto/Triple 测试、OpenAPI/DTP context 测试和 ArchUnit。
这些测试不等价于真实 Nacos、Redis、PostgreSQL、RabbitMQ、跨 JVM RPC 或外部 Gateway
拓扑证明；dev/prod 的运维服务和密钥由操作者提供。

## Maven Profiles 与外部启动参数

每次选择 `dev`、`test`、`prod` 中一个环境 profile，未指定时使用 `dev` 默认值。
这些 profile 控制应用启动，Surefire 仍使用原有 `test` 配置。堆内存参数是示例值，部署时按资源调整。

在项目根目录执行，多模块项目先安装兄弟模块依赖：

```bash
mvn install
mvn -pl egon-cola-source-web-open-starter -Pdev spring-boot:run
mvn -Pprod -Drun.jvm-args="-Xms1g -Xmx2g" \
  -Drun.server-port=8080 \
  -Drun.config-location=file:/etc/myapp/override.yml package
```

Maven 在 `process-resources` 阶段生成 `egon-cola-source-web-open-starter/target/launch.args`，打包时也会生成。
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
