# 学生管理 Service Open 多 Project 架构

## 1. 定位与边界

Service Open 是基于开源 Spring Boot 3.5、Spring Cloud Alibaba、Dubbo Triple、MyBatis-Plus 和 ShardingSphere 的纯服务模板。业务入口只有 Dubbo Triple RPC 和 RabbitMQ；HTTP 只保留 Spring Boot Actuator 管理端点，不提供业务 Controller、Springdoc/OpenAPI、GraphQL、Spring Cloud Gateway 或 native grpc-java server。

该模板保留领域优先的传统三层结构：

```text
starter         启动与组合根
adapter         RPC/MQ 入站适配与 Proto provider
application     用例编排
domain          领域模型与领域服务契约
infrastructure  持久化、消息和出站 RPC client
common          本地通用错误/常量/枚举
facade          本工程自有 Evaluation Proto wire contract（仅契约，不依赖业务模块）
```

`student-management-organization` 与 `student-management-evaluation` 仍是两个可独立构建、部署的 Project。Evaluation Project 的 Organization 目录依赖是出站 Dubbo Proto client；当前 Course、Exam、Score 用例不隐式调用该目录领域服务。

RPC 对外契约由每个生成工程自己的 `facade` 模块持有并对外发布；跨工程调用只依赖
对端已发布的契约工件，该工件通过生成 POM 的显式属性解析：

```text
本工程自有契约：模块 <rootArtifactId>-facade
对端契约依赖：<organization-facade.group-id>:<organization-facade.artifact-id>:<organization-facade.version>
```

`organization-facade.*` 是 Service Open 模板的必填生成参数；缺失时生成必须立即失败，
而不是回退到某个默认对端。两个 `facade` 工件互不依赖，只有 `infrastructure` 引用对端契约。

## 2. Maven 模块与依赖方向

每个生成的 Project 有七个业务模块（Maven reactor 还包含一个父 POM）：

```text
${rootArtifactId}-common
${rootArtifactId}-facade
${rootArtifactId}-domain
${rootArtifactId}-application
${rootArtifactId}-infrastructure
${rootArtifactId}-adapter
${rootArtifactId}-starter
```

依赖方向固定为：

```text
facade                         （不依赖业务模块）
  ↑
adapter -> application -> domain -> common
    ↑             ↑
starter       infrastructure
                   └── domain + 自有 facade + 对端 facade 工件（出站 Organization Proto client）
```

- `facade` 只包含本工程拥有的过滤后 `.proto`、Dubbo 3.3 `tri` 生成代码和 descriptor contract test，不反向依赖 Domain/Application，也不复制对端协议。
- `domain` 只声明聚合、值对象、Common MyBatis-Plus service contract、领域事件服务和 `OrganizationDirectoryService` 契约，不导入 Spring、Dubbo、gRPC 或 ShardingSphere；这些能力的实现与出站 client 都位于 `infrastructure`。
- `application` 只编排 Domain service contract 和用例，不接触 DAO、PO、Proto 或外部 RPC。
- `adapter` 实现 Evaluation 的 11 个 Proto RPC 方法，负责校验、转换和统一 gRPC status/trailer。
- `infrastructure` 实现 Common MyBatis-Plus DAO/PO、Egon service impl、ShardingSphere 数据源、MQ publisher 与 Organization Proto client。
- `starter` 只负责 Boot、Nacos、Dubbo、DTP、配置和模块扫描。

## 3. 自有 Proto/Triple 契约与对端契约

`facade/src/main/proto` 只保留本工程拥有的线协议来源。业务 Proto 使用 `egon.evaluation.v1`，Java package 由 `${package}` 过滤；Dubbo Maven plugin 3.3.6 使用 `dubboGenerateType=tri` 生成 unary Triple service。`google/protobuf/empty.proto` 是为 Dubbo 3.3.6 codegen 提供的 wire-compatible support source，仍声明 `google.protobuf.Empty`，不创建第二个服务器。

三个业务 Proto source 定义 3 个 service、11 个方法：

| Proto | Service | 方法数 |
| --- | --- | ---: |
| `evaluation/v1/course.proto` | `CourseService` | 4 |
| `evaluation/v1/exam.proto` | `ExamService` | 4 |
| `evaluation/v1/score.proto` | `ScoreService` | 3 |

`egon.organization.v1` 的 5 个 service、10 个方法不再出现在本工程，而是由对端 Web Open 工程的
facade 工件独立发布。`infrastructure` 通过 `${organizationFacadePackage}` 引用的 Java 归属消费它，
wire 侧的 package、service、method 与 field 完全保持原状，只有 Java 与 Maven 归属发生变化。

技术 ID、外键和关系 ID 在 Proto 中统一为正 `int64`；时间使用 `google.protobuf.Timestamp`；无返回值使用 `google.protobuf.Empty`；分页固定包含 `records/current_page/total_pages/page_size/total_count`。对端收到非法、找不到、业务拒绝、依赖不可用或内部错误时，Provider 返回 canonical `StatusRuntimeException`，并写入 `x-egon-error-code` 与 `x-egon-trace-id` trailer。

Evaluation adapter 在同一个 Dubbo `tri` 端口上按 `course`、`exam`、`score` group 暴露 11 个方法，版本保持 `1.0.0`。测试同时通过生成的 Dubbo reference 和标准 gRPC `ManagedChannel` unary 调用验证 wire interop；不启动独立的 grpc-java server。

Organization client 使用对端 facade 工件生成的 `UserService` 和 `SchoolClassService` 引用，映射为 Domain 的正 `Long` 投影；接口与实现都在 `infrastructure/client/organization` 及其 `impl`，Domain 只看到 `OrganizationDirectoryService`。

## 4. Long ID 与 Common 组件

所有内部技术 ID 均由 `top.egon.cola.component.common.id.generator.LongIdGenerator` 生成并持久化为 PostgreSQL `BIGINT`。Domain、Application、PO、DAO、事件和分片键只接受非空正 `Long`；HTTP/GraphQL 边界（本 Service 不提供业务 HTTP）若由其他模板消费，使用十进制字符串桥接。Service Open 不再包含 UUID 生成器、`UUID.randomUUID()` 或 UUID 分片算法。

每个实例必须显式设置 `EGON_ID_MACHINE_ID`；没有运行时默认机器号。测试 profile 使用 `0`，并关闭 DTP/Nacos 外连。Long 分片算法使用 `Long.hashCode`、`hash ^ (hash >>> 16)` 和 power-of-two mask，由不可变 node map 同时决定 database/table slot。

## 6. 运行配置合同

- `spring.task.execution.pool` 默认 `core=8`、`max=32`、`queue=1000`、`keep-alive=60s`；只有一个 `applicationTaskExecutor`。
- `AsyncConfiguration` 将 Boot `ThreadPoolTaskExecutor` 交给 `DtpTaskDecorator`，保留 MDC/trace 传递与清理；DTP 自动发现同一 executor，不创建第二个 raw pool、Admin 或 Test 服务。
- `dev`/`prod` 开启 `egon.cola.component.dtp` Redis registry/report，使用 `DTP_REDIS_*`、`DTP_APP_NAME`、`DTP_INSTANCE_ID`；`test` 关闭 DTP report、Nacos discovery/config、RabbitMQ 和 Redis health。
- Compose 的 Nacos image 统一为 `nacos/nacos-server:v3.0.3`，应用容器显式传入 `EGON_ID_MACHINE_ID` 和 DTP Redis/report 环境变量；密码只来自 operator-owned `.env`。
- Dubbo 使用 `tri` protocol，默认 provider port `50051`，provider/consumer timeout `3000ms`，retries `0`。组织目录引用保留 group/version 配置，并只依赖对端独立发布的 facade 工件。

## 7. 测试与证据边界

在仓库根目录执行：

```bash
./mvnw -B -ntp -f egon-cola-archetypes/pom.xml \
  -pl :egon-cola-archetype-service-open -am clean integration-test
```

生成工程测试覆盖自有 Proto descriptor（3 services/11 methods）、对端契约归属（`OpenPeerFacadeContractTest`）、Domain/Application、Common MyBatis-Plus DAO/service、typed ShardingSphere 路由合同、manual SQL convention、11 个 Triple provider 方法、标准 gRPC unary interop、Organization client 双剖面实现、DTP executor/context 和 ArchUnit。ArchUnit 规则取代内部 bytecode Maven plugin，并检查 facade/domain 方向、service-only 边界以及 JPA/Flyway/Gateway/Springdoc 禁止依赖。

测试与 `verify` 只证明源码、生成工程和本地 H2/内存 Triple 测试；不证明真实 PostgreSQL schema、Redis DTP registry、Nacos topology、RabbitMQ、跨 Project provider、部署网络或生产权限。启动应用、手动验收 PG DDL、Compose、发布镜像和 live topology 验证由使用者按环境单独执行。



## Repository、CQRS 与 PostgreSQL

本脚手架使用 MyBatis-Plus 3.5.16：Domain Service 保留业务语义，具体 Repository 继承 `EgonColaRepository`，Mapper 继承 `EgonColaMapper`；查询全部使用显式 XML。PO 继承 `EgonModel` 的 id、tenantId、创建/更新用户与时间、`LocalDateTime deletedAt`、`Long version`。活动行是 NULL，软删写 UTC 时间戳并递增版本。AR/QueryChain 不启用，技术元数据强制填充；枚举与字段 handler 遵循 Common 合同。

`APP_DATASOURCE_MODE` 支持 `SHARDING` 与 `SHARDING_READWRITE`，事务类型为 LOCAL。单表使用明确的 `!SINGLE group.schema.table`；广播表只读。默认 legacy tenant 路由保持原地址。可选两级模板见 `src/test/resources/sharding/two-level-readwrite.yml`（多模块项目在 infrastructure 中）：先按 tenant_id 散列到 tenant slot，再按业务根 ID 散列到 bucket。订单与明细分别使用 id/order_id 共享同一根语义；同租户固定在一个物理组。

算法固定为 mix64-v1，T/B 是不超过 1024 的二次幂，乘积不超过 4096。库间均衡还依赖均衡 slot map 和租户负载；没有自动重分布。Query 缺次级键/范围查询受 fanout 上限约束，Command 必须有精确键。修改分布配置需配套新建表/迁移设计，不能直接套用测试模板到已有业务库。

初始化由 `ShardingDataSourceBootstrapper` 调用 Common 受管 DDL runner，仅对物理 PRIMARY 执行 `db/egon-mp/V20260913_001__initialize_repository_schema.sql` 与 `repository-manifest.json`。空库首次初始化；受管库验证 checksum/前缀/路由指纹；非空未受管库报 REBUILD_REQUIRED。旧 B/V/manual SQL 原样保留作档案，不再作为本脚手架运行入口。

读写分离使用 PostgreSQL 自身复制，普通读走 ROUND_ROBIN 副本，事务读/锁定读/强制主库读走 PRIMARY；不自动创建副本或故障选主。单表、广播表及业务物理表均携带 tenant_id。跨物理组写入会拒绝并标记回滚。

每个实例配置唯一 `EGON_ID_MACHINE_ID`，生产使用现有 Common Snowflake。各 profile 保持同一 MP 配置键；dev 才开启诊断，原始 recorder logger 为 OFF。动态表名默认关闭，只接受明确映射；MybatisBatch 在调用方事务中执行。

默认测试使用隔离 H2 和受控依赖。真实路由测试需 `-Degon.pg.routing=true` 与 `EGON_TEST_PG_URL`；主从测试需 `-Degon.pg.readwrite=true` 与 `EGON_TEST_PG_PRIMARY_URL`、`EGON_TEST_PG_REPLICA_URL`，并提供专用 `EGON_TEST_PG_USER/PASSWORD`。它们只创建/清理自己的 UUID schema，不启动数据库。PG/SS 运行、迁移和性能 EXPLAIN 由使用者手动验收，跳过不表示通过。
