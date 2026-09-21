# Light Open 大型单体轻量领域架构

本文是 `egon-cola-archetype-light-open` 的实现说明。它只描述基于开源 Spring 生态的新增 sibling；原 `egon-cola-archetype-light` 的内部组件版保持不变。

## 1. 运行边界与技术栈

Light Open 生成一个 Java 21、Spring Boot 3.5.16 单模块应用，使用 Spring Cloud 2025.0.3、Spring Cloud Alibaba 2025.0.0.0、Nacos 3.0.3、Springdoc OpenAPI 2.8.17、Egon COLA Common MP starter 和 ShardingSphere-JDBC 5.5.3。

生成项目只负责业务单体 HTTP/GraphQL/MQ 入站及本地应用契约。Gateway、统一路由和外部 RPC 平台属于部署边界之外；Light Open 不引入 Gateway 或本地 Dubbo 运行时。

Common Core、Common ID 和 Dynamic Thread Pool 是允许使用的 Egon COLA 组件。技术 ID 不再生成 UUID：由 Common ID `LongIdGenerator` 产生正数 `Long`，HTTP/GraphQL 边界把它表示为十进制字符串，PostgreSQL 表使用 `BIGINT`。

## 2. 单体包结构

```text
${package}
├── start                        启动装配、OpenAPI、异步、配置解密
├── adapter
│   ├── {user,teaching}
│   │   ├── controller           HTTP 入站
│   │   ├── graphql              GraphQL 入站
│   │   ├── mq                   MQ 入站 consumer
│   │   ├── facade/impl          对外契约实现，直接调用 Manage
│   │   ├── pojo/{dto,vo}        层内手写载体
│   │   ├── pojo/convertor       载体与契约的双向映射
│   │   └── validators           入站校验
│   ├── handler                  全局异常与响应包装
│   └── filter                   trace/请求上下文
├── facade
│   └── {user,teaching}/{dto,enums,utils}   稳定契约、契约枚举与契约断言
├── application
│   └── {user,teaching}
│       ├── manage 与 manage/impl          用例编排与事务
│       ├── pojo/{command,query,result}     用例载体
│       ├── pojo/convertor                  领域到结果的单向快照投影
│       └── validators                      应用层校验
├── domain
│   └── {user,teaching}/{entities,aggregates,vos,service,validators,enums}
├── infrastructure
│   ├── {user,teaching}
│   │   ├── dao / po / converter / repo     MyBatis-Plus 技术四分
│   │   ├── service/impl                    领域服务契约实现
│   │   ├── client 与 client/impl           出站客户端
│   │   └── validators                      基础设施校验
│   ├── aop                                 DAO 监控与基础设施日志切面
│   ├── mq 与 mq/impl                       通用消息发送实现
│   └── config                              Redis、RabbitMQ、本地适配器等技术配置
└── common/{constants,utils,enums,exception}
```

这里的“层”是 Java 包边界，不是 Maven 子模块。`user` 与 `teaching` 业务继续采用 domain-first 目录，技术实现放在对应业务的 `adapter`、`application`、`infrastructure` 子包中。`domain` 不再有 `gateway`、`client`、`event`、`exceptions` 包，`application` 不再有 `assemblers` 与顶层 `command/query/result/convertor` 包，`infrastructure` 不再有 `cache` 与业务 `mq` 包，`common/exceptions` 统一收敛为 `common/exception`。与内部版 Light 不同，Light Open 没有 `adapter/pojo` 共享 RPC 载体与 `facade/validation` 分组：它只发布本地 HTTP/GraphQL 契约，不承载原生 RPC 协议。

## 3. 依赖方向和调用链

```text
start ──> adapter ──> application ──> domain ──> common.exception
  │             └────> facade ─────────────────> common.exception
  └────> infrastructure ──> domain, facade
```

- `start` 只做装配，不承载业务规则。
- `adapter` 将 HTTP/GraphQL 的十进制字符串解析为正数 `Long`，调用 Application，并把结果转回边界 DTO。
- `application` 负责一个用例的校验、事务和跨领域编排；不直接访问 DAO 或具体基础设施。
- `domain` 只依赖 `common.exception` 与自己的领域服务契约；查询、事件、幂等能力都以 `service` 接口暴露，不再有 `gateway` 端口。
- `infrastructure` 实现 Domain 契约与出站 client，负责 MyBatis-Plus、Redis 缓存、外部 HTTP、MQ 等技术细节。
- `facade` 是单体内稳定的应用契约，自包含、可独立发布；契约实现（`adapter.<domain>.facade/impl`）与 `facade` 接口构成“契约 → 实现 → Manage”的单向发布链，`facade` 永不反向依赖 `adapter`。它不是 Gateway，也不启动独立 RPC 服务器。
- 跨层只允许 `common.exception` 的异常根类型出现，其它 `common` 类型不得离开本层。

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

`EGON_ID_MACHINE_ID` 是运行时必填配置，范围为 Common ID 组件允许的 `0..1023`，多实例不得重复。Common ID starter 在启动时一次性静态初始化 `SnowflakeIdGenerator.initialize(machineId, maxClockBackward)`，业务代码只通过 `LongIdGenerator.nextLongId()` 取号，不再自行持有或传递生成器实例；同一个技术 ID 从 Domain 经 PO/DAO 传递到数据库。业务编码、外部标识和幂等键仍是业务字符串。

### 4.2 ShardingSphere

`light_school_classes` 与 `light_class_course_schedules` 均按 `tenant_id` 分库分表，二者使用同一个正数 Long 根键。算法使用稳定的 Long hash/spread/mask 规则；空值、非正数、范围路由和缺失物理节点均失败，不广播到所有节点。读写分离只改变物理数据源选择，不改变领域端口。

### 4.3 MyBatis-Plus 与手工 SQL

生成项目通过 Egon COLA Common MP starter 使用 MyBatis-Plus；Domain service interface 只声明业务语义、不继承任何技术 CRUD 类型，`infrastructure/<domain>/repo` 的具体 Repository 继承 EgonColaRepository，DAO 继承 EgonColaMapper，PO 继承 EgonModel；`dao`、`po`、`converter`、`repo` 四者平级，`repo` 不再容纳其他层的类型；租户与模型校验在启动时静态绑定一次（`EgonColaTenantIdProvider.initialize`、`EgonColaModelValidationUtils.initialize`），业务侧无需再实现端口。不使用 Spring Data JPA。应用启动不会创建或更新表结构，`spring.sql.init.mode` 固定为 `never`。DBA 必须按顺序对每个物理 PostgreSQL 目标手工执行：

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

二级缓存在 `base`/`dev`/`prod` 以 `egon.cola.component.cache.enabled=true` 交付，`test` 保留同一组键但设为 `enabled: false`；`infrastructure/config/RedisConfig.java` 已带 `@EnableCaching`，配置好 Redis 连接即可使用。所有 profile 都声明同一组五个 TTL 键（`l1-expire`、`l1-jitter`、`l2-expire`、`l2-jitter`、`null-expire`）以及共享的 `key-prefix`、`tenant-mdc-key` 与批量/锁预算，只有取值随环境不同。mp-sd-ext 基类不再依赖缓存端口，具体 Repository 用 `@CacheConfig`/`@Cacheable`/`@CacheEvict` 声明策略，并使用 Common 的 KeyGenerator 与主动失效；普通 CRUD 不隐式失效缓存。

## 5.1 公共合同

各层复用 Common Core 合同，不在本工程内复制工具或校验实现：

- 异常：`common/exception` 的根类型继承组件的 `BusinessException` / `CommonException` 链；对外稳定错误码是 `getStatus()` 字符串，数字型 `getCode()` 只是族内分类，不参与传输。
- 枚举：手写业务枚举实现 `EgonEnum` 并固定 `code`/`message`，序列化取声明的 `code` 而不是 `ordinal()`。
- 校验：每个入站交接都经过继承 `BaseValidator` 的校验器，统一注入 `egonColaValidationUtils`；普通规则用 Jakarta 原生注解表达，`facade/*/utils` 的契约断言同样委托 `ValidationUtils`，不再有本地 null/range 工具副本。
- 转换：DTO/Command/Domain/PO 映射由 MapStruct（`@Mapper`）生成并继承 `BaseConverter` 或 `BaseForwardConverter`；只做快照投影的转换器不补反向方法。
- 消息：MQ 拓扑与路由由 `infrastructure/mq/MqRouteEnum` 描述，生产与消费共用同一枚举；Manage 只通过领域 `*EventService` 发消息，不直接依赖 Publisher 或 `RabbitTemplate`。
- 监控：`infrastructure/aop/DaoMonitorAspect` 只环绕 DAO 代理与继承而来的 SQL 方法，`InfrastructureLogAspect` 匹配收敛后的基础设施日志范围，service/cache/converter 不计入 DAO 耗时。

Springdoc 提供 `/v3/api-docs` 和 `/swagger-ui.html`。Gateway、鉴权入口和统一流量策略由外部平台负责，Light Open 不复制这些组件。

## 6. 手工运行与验证边界

```bash
./mvnw -B -ntp clean verify
./mvnw -B -ntp -DskipTests package
```

`verify` 覆盖编译、测试、OpenAPI smoke、ArchUnit、DAO/manual-SQL 合同和 archetype verifier。它证明生成树的静态及测试 profile 合同，不证明真实 Nacos 3、Redis、PostgreSQL、RabbitMQ、外部 HTTP 或生产路由拓扑；这些由运维在手动验收 PG DDL、配置环境并部署后单独验收。



## Repository、CQRS 与 PostgreSQL

本脚手架使用 MyBatis-Plus 3.5.16：Domain Service 保留业务语义，具体 Repository 继承 `EgonColaRepository`，Mapper 继承 `EgonColaMapper`；查询全部使用显式 XML。PO 继承 `EgonModel` 的 id、tenantId、创建/更新用户与时间、`LocalDateTime deletedAt`、`Long version`。活动行是 NULL，软删写 UTC 时间戳并递增版本。AR/QueryChain 不启用，技术元数据强制填充；枚举与字段 handler 遵循 Common 合同。

`APP_DATASOURCE_MODE` 支持 `SHARDING` 与 `SHARDING_READWRITE`，事务类型为 LOCAL。单表使用明确的 `!SINGLE group.schema.table`；广播表只读。默认 legacy tenant 路由保持原地址。可选两级模板见 `src/test/resources/sharding/two-level-readwrite.yml`（多模块项目在 infrastructure 中）：先按 tenant_id 散列到 tenant slot，再按业务根 ID 散列到 bucket。订单与明细分别使用 id/order_id 共享同一根语义；同租户固定在一个物理组。

算法固定为 mix64-v1，T/B 是不超过 1024 的二次幂，乘积不超过 4096。库间均衡还依赖均衡 slot map 和租户负载；没有自动重分布。Query 缺次级键/范围查询受 fanout 上限约束，Command 必须有精确键。修改分布配置需配套新建表/迁移设计，不能直接套用测试模板到已有业务库。

初始化由 `ShardingDataSourceBootstrapper` 调用 Common 受管 DDL runner，仅对物理 PRIMARY 执行 `db/egon-mp/V20260913_001__initialize_repository_schema.sql` 与 `repository-manifest.json`。空库首次初始化；受管库验证 checksum/前缀/路由指纹；非空未受管库报 REBUILD_REQUIRED。旧 B/V/manual SQL 原样保留作档案，不再作为本脚手架运行入口。

读写分离使用 PostgreSQL 自身复制，普通读走 ROUND_ROBIN 副本，事务读/锁定读/强制主库读走 PRIMARY；不自动创建副本或故障选主。单表、广播表及业务物理表均携带 tenant_id。跨物理组写入会拒绝并标记回滚。

每个实例配置唯一 `EGON_ID_MACHINE_ID`，生产使用现有 Common Snowflake。各 profile 保持同一 MP 配置键；dev 才开启诊断，原始 recorder logger 为 OFF。动态表名默认关闭，只接受明确映射；MybatisBatch 在调用方事务中执行。

默认测试使用隔离 H2 和受控依赖。真实路由测试需 `-Degon.pg.routing=true` 与 `EGON_TEST_PG_URL`；主从测试需 `-Degon.pg.readwrite=true` 与 `EGON_TEST_PG_PRIMARY_URL`、`EGON_TEST_PG_REPLICA_URL`，并提供专用 `EGON_TEST_PG_USER/PASSWORD`。它们只创建/清理自己的 UUID schema，不启动数据库。PG/SS 运行、迁移和性能 EXPLAIN 由使用者手动验收，跳过不表示通过。
