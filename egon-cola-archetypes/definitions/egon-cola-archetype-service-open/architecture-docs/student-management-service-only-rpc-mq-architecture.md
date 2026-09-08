# 学生管理 Service Open 多 Project 架构

## 1. 定位与边界

Service Open 是基于开源 Spring Boot 3.5、Spring Cloud Alibaba、Dubbo Triple、MyBatis-Plus 和 ShardingSphere 的纯服务模板。业务入口只有 Dubbo Triple RPC 和 RabbitMQ；HTTP 只保留 Spring Boot Actuator 管理端点，不提供业务 Controller、Springdoc/OpenAPI、GraphQL、Spring Cloud Gateway 或 native grpc-java server。

该模板保留领域优先的传统三层结构：

```text
starter         启动与组合根
adapter         RPC/MQ 入站适配与 Proto provider
application     用例编排
domain          领域模型与端口
infrastructure  持久化、消息和出站 RPC client
common          本地通用错误/常量/枚举
facade          本地 Proto wire contract（仅契约，不依赖业务模块）
```

`student-management-organization` 与 `student-management-evaluation` 仍是两个可独立构建、部署的 Project。Evaluation Project 的 Organization 目录依赖是出站 Dubbo Proto client；当前 Course、Exam、Score 用例不隐式调用该目录端口。

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
                   └── domain + facade（出站 Organization Proto client）
```

- `facade` 只包含过滤后的 `.proto`、Dubbo 3.3 `tri` 生成代码和 descriptor contract test，不反向依赖 Domain/Application。
- `domain` 只声明聚合、值对象、Common MyBatis-Plus service contract、事件端口和 `OrganizationDirectoryPort`，不导入 Spring、Dubbo、gRPC 或 ShardingSphere。
- `application` 只编排 Domain service contract 和用例，不接触 DAO、PO、Proto 或外部 RPC。
- `adapter` 实现 Evaluation 的 11 个 Proto RPC 方法，负责校验、转换和统一 gRPC status/trailer。
- `infrastructure` 实现 Common MyBatis-Plus DAO/PO、Egon service impl、ShardingSphere 数据源、MQ publisher 与 Organization Proto client。
- `starter` 只负责 Boot、Nacos、Dubbo、DTP、配置和模块扫描。

## 3. 本地 Proto/Triple 契约

`facade/src/main/proto` 是唯一的线协议来源。业务 Proto 使用 `egon.evaluation.v1` 和 `egon.organization.v1`，Java package 由 `${package}` 过滤；Dubbo Maven plugin 3.3.6 使用 `dubboGenerateType=tri` 生成 unary Triple service。`google/protobuf/empty.proto` 是为 Dubbo 3.3.6 codegen 提供的 wire-compatible support source，仍声明 `google.protobuf.Empty`，不创建第二个服务器。

五个业务 Proto source 定义 8 个 service、21 个方法：

| Proto | Service | 方法数 |
| --- | --- | ---: |
| `evaluation/v1/course.proto` | `CourseService` | 4 |
| `evaluation/v1/exam.proto` | `ExamService` | 4 |
| `evaluation/v1/score.proto` | `ScoreService` | 3 |
| `organization/v1/user.proto` | `UserService`、`RoleService`、`PermissionService` | 5 |
| `organization/v1/teaching.proto` | `GradeService`、`SchoolClassService` | 5 |

技术 ID、外键和关系 ID 在 Proto 中统一为正 `int64`；时间使用 `google.protobuf.Timestamp`；无返回值使用 `google.protobuf.Empty`；分页固定包含 `records/current_page/total_pages/page_size/total_count`。对端收到非法、找不到、业务拒绝、依赖不可用或内部错误时，Provider 返回 canonical `StatusRuntimeException`，并写入 `x-egon-error-code` 与 `x-egon-trace-id` trailer。

Evaluation adapter 在同一个 Dubbo `tri` 端口上按 `course`、`exam`、`score` group 暴露 11 个方法，版本保持 `1.0.0`。测试同时通过生成的 Dubbo reference 和标准 gRPC `ManagedChannel` unary 调用验证 wire interop；不启动独立的 grpc-java server。

Organization client 使用本地生成的 `UserService` 和 `SchoolClassService` 引用，映射为 Domain 的正 `Long` 投影，调用只发生在显式目录端口中。

## 4. Long ID 与 Common 组件

所有内部技术 ID 均由 `top.egon.cola.component.common.id.generator.LongIdGenerator` 生成并持久化为 PostgreSQL `BIGINT`。Domain、Application、PO、DAO、事件和分片键只接受非空正 `Long`；HTTP/GraphQL 边界（本 Service 不提供业务 HTTP）若由其他模板消费，使用十进制字符串桥接。Service Open 不再包含 UUID 生成器、`UUID.randomUUID()` 或 UUID 分片算法。

每个实例必须显式设置 `EGON_ID_MACHINE_ID`；没有运行时默认机器号。测试 profile 使用 `0`，并关闭 DTP/Nacos 外连。Long 分片算法使用 `Long.hashCode`、`hash ^ (hash >>> 16)` 和 power-of-two mask，由不可变 node map 同时决定 database/table slot。

## 5. 持久化、手工 SQL 与分片

持久化使用 `egon-cola-component-common-mybatis-plus-spring-boot-starter`、Egon Common 的 `EgonColaIService`/`EgonColaServiceImpl`/`EgonColaMapper`、DAO XML 和 ShardingSphere `5.5.3`。Domain 只引用 Common service contract；PO 和 DAO 位于 infrastructure，service impl 也位于 infrastructure。禁止 `spring-boot-starter-data-jpa`、`JpaRepository`、`jakarta.persistence` 和 Flyway。

每个 Project 的 `infrastructure/src/main/resources/db/manual/postgresql` 保存按顺序执行的 PostgreSQL 建表/索引脚本，其 `README.md` 说明 DBA 手工更新顺序、目标物理库和回滚边界。应用不会在启动或测试时自动刷表，仓库不创建 `db/migration` 或 `flyway_schema_history`。

Evaluation 的初始拓扑是 `master_data` 主表库，以及 `shard_0`、`shard_1` 的两库两表分片。逻辑表为 `evaluation_course`、`evaluation_course_schedule`、`evaluation_exam`、`evaluation_exam_paper`、`evaluation_score`；全部按正 `tenant_id` 同时进行分库分表，同一租户保持同一物理 slot。未带分片键的 DML、范围路由、未知节点、非正 tenant ID 和不一致 node map 均 fail-fast。跨物理库流程只通过业务幂等、状态、事件、对账和补偿解决，不引入 JPA、Flyway、XA、Seata 或自动搬数。

## 6. 运行配置合同

- `spring.task.execution.pool` 默认 `core=8`、`max=32`、`queue=1000`、`keep-alive=60s`；只有一个 `applicationTaskExecutor`。
- `AsyncConfiguration` 将 Boot `ThreadPoolTaskExecutor` 交给 `DtpTaskDecorator`，保留 MDC/trace 传递与清理；DTP 自动发现同一 executor，不创建第二个 raw pool、Admin 或 Test 服务。
- `dev`/`prod` 开启 `egon.cola.component.dtp` Redis registry/report，使用 `DTP_REDIS_*`、`DTP_APP_NAME`、`DTP_INSTANCE_ID`；`test` 关闭 DTP report、Nacos discovery/config、RabbitMQ 和 Redis health。
- Compose 的 Nacos image 统一为 `nacos/nacos-server:v3.0.3`，应用容器显式传入 `EGON_ID_MACHINE_ID` 和 DTP Redis/report 环境变量；密码只来自 operator-owned `.env`。
- Dubbo 使用 `tri` protocol，默认 provider port `50051`，provider/consumer timeout `3000ms`，retries `0`。组织目录引用保留 group/version 配置，但不再依赖 `top.egon` facade artifact。

## 7. 测试与证据边界

在仓库根目录执行：

```bash
./mvnw -B -ntp -f egon-cola-archetypes/pom.xml \
  -pl :egon-cola-archetype-service-open -am clean integration-test
```

生成工程测试覆盖 Proto descriptor（8 services/21 methods）、Domain/Application、Common MyBatis-Plus DAO/service、ShardingSphere H2 路由、manual SQL convention、11 个 Triple provider 方法、标准 gRPC unary interop、Organization client/stub、DTP executor/context 和 ArchUnit。ArchUnit 规则取代内部 bytecode Maven plugin，并检查 facade/domain 方向、service-only 边界以及 JPA/Flyway/Gateway/Springdoc 禁止依赖。

测试与 `verify` 只证明源码、生成工程和本地 H2/内存 Triple 测试；不证明真实 PostgreSQL schema、Redis DTP registry、Nacos topology、RabbitMQ、跨 Project provider、部署网络或生产权限。启动应用、执行手工 SQL、Compose、发布镜像和 live topology 验证由使用者按环境单独执行。

## Dependency and runtime ownership

Generated projects inherit the released `top.egon:egon-cola-archetypes-parent` at a concrete version with an empty `relativePath`. The parent imports the Components BOM, manages Common dependencies and ShardingSphere 5.5.3, and keeps Commons Lang at 3.20.0. Consumer modules inherit their own project root. Install the matching parent/BOM and required artifacts locally before validating an unpublished release; a local install does not publish artifacts.

Open retains its existing Spring Cloud/Nacos stack and consumed Common Core/ID/MyBatis Plus/Dynamic Thread Pool components. Service/Web Open retain their local facade Protobuf contracts and the 21-operation external interoperability surface, using gRPC 1.73.0 / Protobuf 3.25.8 and Dubbo 3.3.6. The Open Gateway boundary remains external.
