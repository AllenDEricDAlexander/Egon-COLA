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

`facade` 保存本工程自有的 Organization Proto 源码及其 Dubbo Triple 生成类；Adapter
实现十个组织 Provider 方法；Infrastructure 负责 MyBatis-Plus、ShardingSphere、Redis/MQ
adapter，以及消费对端 Service Open facade 工件的标准 gRPC Evaluation client。

Organization 契约由本工程自身的 `-facade` 模块发布；被消费的 Evaluation 契约始终是外部
发布的工件，生成的 POM 通过 generation 时显式给出的对端 facade 坐标属性解析它。生成消费方
之前必须先发布对端的 `parent`、`common` 与 `facade`，且两个 facade 互不依赖，Maven 图因此
保持无环。


技术矩阵为 Spring Boot 3.5.16、Spring Cloud 2025.0.3、Spring Cloud Alibaba
2025.0.0.0、Dubbo 3.3.6、Nacos 3.0.3（Compose 镜像）、gRPC 1.73.0、Protobuf 3.x、
MyBatis-Plus 3.5.16、ShardingSphere 5.5.3、Springdoc 2.8.17。直接使用的 Egon
组件只有 Common core/ID 与 Dynamic Thread Pool starter。

项目明确不使用 Spring Data JPA、Flyway、Liquibase、Gateway starter 和 UUID 业务主键，
也不再复制对端协议：唯一的外部契约是由 Service Open 工程发布的 Evaluation facade 工件。
`OpenArchitectureTest`、`OpenPeerFacadeContractTest` 和生成物 verifier 会重复检查这些边界。


业务 ID 统一为 `Long`，由 Common Snowflake `LongIdGenerator` 生成。每个运行实例必须
设置唯一 `EGON_ID_MACHINE_ID`；test profile 使用机器号 `0`。

旧手工 SQL 为只读档案；当前初始化与运行合同见下方 Repository/PostgreSQL 章节。

旧手工 SQL 为只读档案；当前初始化与运行合同见下方 Repository/PostgreSQL 章节。

旧手工 SQL 为只读档案；当前初始化与运行合同见下方 Repository/PostgreSQL 章节。


以下三个文件是本工程唯一拥有的 wire contract，并会复制到生成项目；Evaluation 协议位于
对端 facade 工件中：

```text
facade/src/main/proto/organization/v1/user.proto
facade/src/main/proto/organization/v1/teaching.proto
facade/src/main/proto/google/protobuf/empty.proto
```

组织服务使用 Dubbo Triple，group 为 `student-management-organization`、version 为
`1.0.0`，dev/prod 协议为 `tri`。Evaluation 使用带 managed channel 和可配置 deadline
的 `GrpcEvaluationQueryClientImpl`，不做隐式重试；测试用 in-process gRPC server。


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

## Repository、CQE 与 PostgreSQL

本脚手架使用 MyBatis-Plus 3.5.16：Domain Service 保留业务语义，具体 Repository 继承 `EgonColaRepository`，Mapper 继承 `EgonColaMapper`；查询全部使用显式 XML。PO 继承 `EgonModel` 的 id、tenantId、创建/更新用户与时间、`LocalDateTime deletedAt`、`Long version`。活动行是 NULL，软删写 UTC 时间戳并递增版本。AR/QueryChain 不启用，技术元数据强制填充；枚举与字段 handler 遵循 Common 合同。

`APP_DATASOURCE_MODE` 支持 `SHARDING` 与 `SHARDING_READWRITE`，事务类型为 LOCAL。单表使用明确的 `!SINGLE group.schema.table`；广播表只读。默认 legacy tenant 路由保持原地址。可选两级模板见 `src/test/resources/sharding/two-level-readwrite.yml`（多模块项目在 infrastructure 中）：先按 tenant_id 散列到 tenant slot，再按业务根 ID 散列到 bucket。订单与明细分别使用 id/order_id 共享同一根语义；同租户固定在一个物理组。

算法固定为 mix64-v1，T/B 是不超过 1024 的二次幂，乘积不超过 4096。库间均衡还依赖均衡 slot map 和租户负载；没有自动重分布。Query 缺次级键/范围查询受 fanout 上限约束，Command 必须有精确键。修改分布配置需配套新建表/迁移设计，不能直接套用测试模板到已有业务库。

初始化由 组件统一管理数据源/拓扑与 `EgonColaPostgreDdlRunner` 受管 DDL，仅对物理 PRIMARY 执行 `db/egon-mp/V20260913_001__initialize_repository_schema.sql` 与 `repository-manifest.json`。空库首次初始化；受管库验证 checksum/前缀/路由指纹；非空未受管库报 REBUILD_REQUIRED。旧 B/V/manual SQL 原样保留作档案，不再作为本脚手架运行入口。

读写分离使用 PostgreSQL 自身复制，普通读走 ROUND_ROBIN 副本，事务读/锁定读/强制主库读走 PRIMARY；不自动创建副本或故障选主。单表、广播表及业务物理表均携带 tenant_id。跨物理组写入会拒绝并标记回滚。

每个实例配置唯一 `EGON_ID_MACHINE_ID`，生产使用现有 Common Snowflake。各 profile 保持同一 MP 配置键；dev 才开启诊断，原始 recorder logger 为 OFF。动态表名默认关闭，只接受明确映射；MybatisBatch 在调用方事务中执行。

默认测试使用隔离 H2 和受控依赖。真实路由测试需 `-Degon.pg.routing=true` 与 `EGON_TEST_PG_URL`；主从测试需 `-Degon.pg.readwrite=true` 与 `EGON_TEST_PG_PRIMARY_URL`、`EGON_TEST_PG_REPLICA_URL`，并提供专用 `EGON_TEST_PG_USER/PASSWORD`。它们只创建/清理自己的 UUID schema，不启动数据库。PG/SS 运行、迁移和性能 EXPLAIN 由使用者手动验收，跳过不表示通过。

## 二级缓存骨架（默认关闭）

生成工程保留缓存 starter 依赖和默认 `enabled: false` 配置。启用时由宿主提供 `RedissonClient`，设置
`egon.cola.component.cache.enabled=true`，并在配置类显式添加 `@EnableCaching`。mp-sd-ext 基类已移除缓存端口耦合，具体
Repository 通过 `@CacheConfig`、`@Cacheable`、`@CacheEvict` 等注解声明策略；已有 Repository 示例使用 `findCachedById` /
`updateCachedById`（Agent 按业务自行声明）。普通 CRUD
不再隐式失效缓存，其他写入和删除入口也须声明失效。Key、条件、组合操作、事务与同步加载限制详见 [cache starter README](../../../egon-cola-components/egon-cola-component-common/egon-cola-component-common-cache-spring-boot-starter/README.md)。

## 2026-09-22 Java / CQE 维护规范

普通实体使用 class，仅不可变值对象可用 record。普通构造目标使用 @Builder，包含父类字段的构建使用兼容继承链的 @SuperBuilder。持久化枚举使用 @EnumValue，前端 JSON 使用 @JsonValue。必须复用 Components 和 Common MP Repository。校验基于原生/自定义约束、@Valid、@Validated 与分组，ValidationUtils 仅作通用手工触发。软删除业务唯一键必须组合业务列与 deleted_at，并验证有效行 NULL 语义。Event 经 Egon 事务 Outbox 或实际 MQ 投递，明确事务、失败和消费幂等。现有代码及旧 SQL 应按新规范逐项复核；本文档更新不代表已经迁移。
