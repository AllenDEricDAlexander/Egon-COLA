# Light Open 大型单体轻量领域架构

本文是 `egon-cola-archetype-light-open` 的实现说明。它只描述基于开源 Spring 生态的新增 sibling；原 `egon-cola-archetype-light` 的内部组件版保持不变。

## 1. 运行边界与技术栈

Light Open 生成一个 Java 21、Spring Boot 3.5.16 单模块应用，使用 Spring Cloud 2025.0.3、Spring Cloud Alibaba 2025.0.0.0、Nacos 3.0.3、Springdoc OpenAPI 2.8.17、Egon COLA Common MP starter 和 ShardingSphere-JDBC 5.5.3。

生成项目只负责业务单体 HTTP/GraphQL/MQ 入站及本地应用契约。Gateway、统一路由和外部 RPC 平台属于部署边界之外；Light Open 不引入 Gateway 或本地 Dubbo 运行时。

Common Core、Common ID 和 Dynamic Thread Pool 是允许使用的 Egon COLA 组件。技术 ID 不再生成 UUID：由 Common ID `LongIdGenerator` 产生正数 `Long`，HTTP/GraphQL 边界把它表示为十进制字符串，PostgreSQL 表使用 `BIGINT`。

## 2. 单体包结构

```text
${package}
├── start              启动装配、OpenAPI、异步、数据源
├── adapter            HTTP、GraphQL、MQ 入站与 facade 实现
├── facade             稳定 DTO/契约
├── application        用例编排、事务、应用校验
├── domain             聚合、领域规则、服务、仓储端口
├── infrastructure     DAO、PO、ServiceImpl、XML、缓存、客户端、消息适配器
└── common             与业务无关的基础类型
```

这里的“层”是 Java 包边界，不是 Maven 子模块。`user` 与 `teaching` 业务继续采用 domain-first 目录，技术实现放在对应业务的 `adapter`、`application`、`infrastructure` 子包中。

## 3. 依赖方向和调用链

```text
start ──> adapter ──> application ──> domain ──> common
  │             └────> facade
  └────> infrastructure ──> domain
```

- `start` 只做装配，不承载业务规则。
- `adapter` 将 HTTP/GraphQL 的十进制字符串解析为正数 `Long`，调用 Application，并把结果转回边界 DTO。
- `application` 负责一个用例的校验、事务和跨领域编排；不直接访问 DAO 或具体基础设施。
- `domain` 只依赖通用基础类型以及自己的仓储/服务端口。
- `infrastructure` 实现 Domain 端口，负责 MyBatis-Plus、缓存、外部 HTTP、MQ 等技术细节。
- `facade` 是单体内稳定的应用契约；它不是 Gateway，也不启动独立 RPC 服务器。

典型创建课程调用链：

```text
CourseController
  -> CourseManage
    -> CourseDomainService<?>
      -> CourseDomainServiceImpl
        ->  CourseDAO + CourseDAO.xml
          -> ShardingSphere logical DataSource
            -> PostgreSQL physical target
```

`OpenArchitectureTest` 在生成工程中对上述向内依赖进行 ArchUnit 检查，并拒绝 Domain/Application 依赖 ShardingSphere 或其它边缘技术。

## 4. ID、分片与数据访问

### 4.1 ID 合同

`EGON_ID_MACHINE_ID` 是运行时必填配置，范围为 Common ID 组件允许的 `0..1023`，多实例不得重复。应用在创建用例中调用一次 `LongIdGenerator.nextLongId()`；同一个技术 ID 从 Domain 经 PO/DAO 传递到数据库。业务编码、外部标识和幂等键仍是业务字符串。

### 4.2 ShardingSphere

`light_school_classes` 与 `light_class_course_schedules` 均按 `tenant_id` 分库分表，二者使用同一个正数 Long 根键。算法使用稳定的 Long hash/spread/mask 规则；空值、非正数、范围路由和缺失物理节点均失败，不广播到所有节点。读写分离只改变物理数据源选择，不改变领域端口。

### 4.3 MyBatis-Plus 与手工 SQL

生成项目通过 Egon COLA Common MP starter 使用 MyBatis-Plus；domain service interface 继承 EgonColaIService，infrastructure ServiceImpl 继承 EgonColaServiceImpl，DAO 继承 EgonColaMapper，PO 继承 EgonModel；不使用 Spring Data JPA。应用启动不会创建或更新表结构，`spring.sql.init.mode` 固定为 `never`。DBA 必须按顺序对每个物理 PostgreSQL 目标手工执行：

```text
db/manual/postgresql/master-data/001__create_light_master_data_schema.sql
db/manual/postgresql/shard/002__create_light_sharded_schema.sql
db/manual/postgresql/master-data/003__migrate_light_master_data_to_egon_model.sql
db/manual/postgresql/shard/004__migrate_light_sharded_to_tenant_model.sql
```

脚本和 `db/manual/postgresql/README.md` 共同记录目标、顺序、BIGINT 字段、校验 SQL、checksum 和回滚操作。测试只通过显式 `ManualSchemaTestSupport` 在 H2 中执行脚本；普通 Spring context 不执行 schema SQL。

## 5. 线程池、配置与 API

Boot 自动配置的 `applicationTaskExecutor` 是唯一应用异步执行器。`AsyncConfiguration` 注入该 bean，注册 `DtpTaskDecorator`，并让 `AsyncConfigurer` 返回同一个 executor，不创建第二个线程池。默认边界是 core `8`、max `32`、queue `1000`、keep-alive `60s`，均可通过环境变量覆盖。

开发/生产通过 `egon.cola.component.dtp.registry.redis` 使用 Redis 注册 DTP 快照和配置变更，并启用报告；`test` profile 关闭 DTP、报告、Nacos、Redis、RabbitMQ 和外部 HTTP，所以生成工程验证无需在线依赖。

Springdoc 提供 `/v3/api-docs` 和 `/swagger-ui.html`。Gateway、鉴权入口和统一流量策略由外部平台负责，Light Open 不复制这些组件。

## 6. 手工运行与验证边界

```bash
./mvnw -B -ntp clean verify
./mvnw -B -ntp -DskipTests package
```

`verify` 覆盖编译、测试、OpenAPI smoke、ArchUnit、DAO/manual-SQL 合同和 archetype verifier。它证明生成树的静态及测试 profile 合同，不证明真实 Nacos 3、Redis、PostgreSQL、RabbitMQ、外部 HTTP 或生产路由拓扑；这些由运维在执行手工 SQL、配置环境并部署后单独验收。
