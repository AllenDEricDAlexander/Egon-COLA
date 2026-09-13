# egon-cola-source-light-open

[English](README.md) | [中文](README.zh-CN.md)

`egon-cola-source-light-open` 是 Egon COLA Light 的开源版脚手架。它生成一个可部署的 Spring Boot 3.5.16 单模块应用；`start`、`adapter`、`facade`、`application`、`domain`、`infrastructure`、`common` 是单体内部的逻辑包，不是 Maven 子模块。

## Maven Profiles 与外部启动参数

每次选择 `dev`、`test`、`prod` 中一个环境 profile，未指定时使用 `dev` 默认值。
这些 profile 控制应用启动，Surefire 仍使用原有 `test` 配置。堆内存参数是示例值，部署时按资源调整。

在项目根目录执行，多模块项目先安装兄弟模块依赖：

```bash
mvn install
mvn -Pdev spring-boot:run
mvn -Pprod -Drun.jvm-args="-Xms1g -Xmx2g" \
  -Drun.server-port=8080 \
  -Drun.config-location=file:/etc/myapp/override.yml package
```

Maven 在 `process-resources` 阶段生成 `target/launch.args`，打包时也会生成。
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

## 技术栈

- Java 21、Spring Boot 3.5.16、Spring Cloud 2025.0.3、Spring Cloud Alibaba 2025.0.0.0、Nacos 3.0.3。
- 通过 Egon COLA Common MP starter 使用 MyBatis-Plus，持久化采用 DAO/XML SQL 与 EgonModel PO；ShardingSphere-JDBC 5.5.3 负责分库分表。
- Egon COLA Common Core、Common ID、Dynamic Thread Pool 组件。
- Springdoc OpenAPI 2.8.17 提供 HTTP API 文档；Gateway 属于外部平台，不在本工程内生成。
- 保留现有 Light 用例需要的 RabbitMQ、Redis、GraphQL 和外部 HTTP 可选集成。

## 领域优先结构

```text
src/main/java/top/egon/cola/archetype/source/lightopen
├── start              # 启动装配、OpenAPI、异步和数据源配置
├── adapter            # HTTP、GraphQL、MQ 入站和 facade 实现
├── facade             # 稳定的本地应用契约和 DTO
├── application        # 用例编排、校验和事务
├── domain             # 聚合、规则、服务和仓储端口
├── infrastructure     # MyBatis-Plus DAO/PO/ServiceImpl、客户端、缓存和 MQ 适配器
└── common             # 与业务无关的项目基础类型
```

依赖方向为 `adapter -> application -> domain -> common`；`infrastructure` 实现 Domain 端口，`start` 负责装配。生成的 `OpenArchitectureTest` 会检查向内依赖，并拒绝 Domain/Application 直接依赖持久化或传输框架。

## ID 与持久化

技术 ID 由 Common ID 的 `LongIdGenerator` 生成，在 PostgreSQL 中使用 `BIGINT`。Domain、Application、持久化和分片内部统一使用正数 `Long`；HTTP/GraphQL 边界仍使用十进制字符串。每个运行实例必须设置唯一的 `EGON_ID_MACHINE_ID`，运行时没有默认值。

持久化使用 Common MP Repository 与显式 Mapper XML；Domain Service 不依赖技术 CRUD 类型。

旧手工 SQL 为只读档案；当前初始化与运行合同见下方 Repository/PostgreSQL 章节。

旧手工 SQL 为只读档案；当前初始化与运行合同见下方 Repository/PostgreSQL 章节。

ShardingSphere 按正数 `tenant_id` 路由 `light_school_classes` 与 `light_class_course_schedules`，使用稳定的正数 Long 分片算法。班级与排课必须携带同一个根键；跨分片流程由调用方通过业务幂等和补偿处理。

## 运行治理

Spring Boot 的 `applicationTaskExecutor` 是唯一的应用异步执行器，环境可覆盖的默认边界为 core `8`、max `32`、queue `1000`、keep-alive `60s`。`DtpTaskDecorator` 负责传递并清理执行上下文，Dynamic Thread Pool 组件在 `dev`/`prod` 中通过 Redis 管理同一个执行器。

`test` profile 设置 `egon.cola.component.dtp.enabled=false`，关闭 DTP 上报和 Nacos，使用 H2，并关闭 RabbitMQ、Redis、外部 HTTP。因此生成测试不需要在线基础设施。`dev`/`prod` Compose 示例会显式传入机器 ID 和 DTP Redis/上报配置。

## API 与命令

Springdoc 在 `/v3/api-docs` 提供 OpenAPI 文档，在 `/swagger-ui.html` 提供 Swagger UI。

```bash
# 生成后的项目
./mvnw -B -ntp clean verify
./mvnw -B -ntp -DskipTests package
```

`verify` 包含生成的 ArchUnit 规则。`test`/`verify` 是静态和生成工程检查，不代表真实 Nacos、Redis、PostgreSQL、RabbitMQ 或外部服务拓扑已经可用。

## 容器交付

使用环境示例和六个 Compose 变体之一：

```bash
docker compose --env-file deploy/env/.env.example \
  -f deploy/compose/compose.docker.yaml up -d --build
```

开发和生产示例均使用 Nacos `nacos/nacos-server:v3.0.3`。请通过运维持有的环境文件提供数据库、Redis/RabbitMQ、Nacos 凭据及唯一的 `EGON_ID_MACHINE_ID`。archetype 生成过程不会启动应用，也不会执行数据库 SQL。

配置加密继续使用 `EGON_CONFIG_DECRYPT_KEY` 或 `EGON_CONFIG_DECRYPT_KEY_FILE` 提供 32 字节密钥；不要提交凭据或密钥。

## Repository、CQRS 与 PostgreSQL

本脚手架使用 MyBatis-Plus 3.5.16：Domain Service 保留业务语义，具体 Repository 继承 `EgonColaRepository`，Mapper 继承 `EgonColaMapper`；查询全部使用显式 XML。PO 继承 `EgonModel` 的 id、tenantId、创建/更新用户与时间、`LocalDateTime deletedAt`、`Long version`。活动行是 NULL，软删写 UTC 时间戳并递增版本。AR/QueryChain 不启用，技术元数据强制填充；枚举与字段 handler 遵循 Common 合同。

`APP_DATASOURCE_MODE` 支持 `SHARDING` 与 `SHARDING_READWRITE`，事务类型为 LOCAL。单表使用明确的 `!SINGLE group.schema.table`；广播表只读。默认 legacy tenant 路由保持原地址。可选两级模板见 `src/test/resources/sharding/two-level-readwrite.yml`（多模块项目在 infrastructure 中）：先按 tenant_id 散列到 tenant slot，再按业务根 ID 散列到 bucket。订单与明细分别使用 id/order_id 共享同一根语义；同租户固定在一个物理组。

算法固定为 mix64-v1，T/B 是不超过 1024 的二次幂，乘积不超过 4096。库间均衡还依赖均衡 slot map 和租户负载；没有自动重分布。Query 缺次级键/范围查询受 fanout 上限约束，Command 必须有精确键。修改分布配置需配套新建表/迁移设计，不能直接套用测试模板到已有业务库。

初始化由 `ShardingDataSourceBootstrapper` 调用 Common 受管 DDL runner，仅对物理 PRIMARY 执行 `db/egon-mp/V20260913_001__initialize_repository_schema.sql` 与 `repository-manifest.json`。空库首次初始化；受管库验证 checksum/前缀/路由指纹；非空未受管库报 REBUILD_REQUIRED。旧 B/V/manual SQL 原样保留作档案，不再作为本脚手架运行入口。

读写分离使用 PostgreSQL 自身复制，普通读走 ROUND_ROBIN 副本，事务读/锁定读/强制主库读走 PRIMARY；不自动创建副本或故障选主。单表、广播表及业务物理表均携带 tenant_id。跨物理组写入会拒绝并标记回滚。

每个实例配置唯一 `EGON_ID_MACHINE_ID`，生产使用现有 Common Snowflake。各 profile 保持同一 MP 配置键；dev 才开启诊断，原始 recorder logger 为 OFF。动态表名默认关闭，只接受明确映射；MybatisBatch 在调用方事务中执行。

默认测试使用隔离 H2 和受控依赖。真实路由测试需 `-Degon.pg.routing=true` 与 `EGON_TEST_PG_URL`；主从测试需 `-Degon.pg.readwrite=true` 与 `EGON_TEST_PG_PRIMARY_URL`、`EGON_TEST_PG_REPLICA_URL`，并提供专用 `EGON_TEST_PG_USER/PASSWORD`。它们只创建/清理自己的 UUID schema，不启动数据库。PG/SS 运行、迁移和性能 EXPLAIN 由使用者手动验收，跳过不表示通过。
