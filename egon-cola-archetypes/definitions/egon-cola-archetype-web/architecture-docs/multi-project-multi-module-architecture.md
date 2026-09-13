# 学生管理系统 Web 多工程多模块分层架构

## 1. 适用范围

本文档是 `egon-cola-archetype-web` 的生效架构合同。模板生成一个独立的
`organization` Project，包含 `user` 和 `teaching` 两个业务领域；评价工程通过
独立的 Evaluation Facade 被 Infrastructure client 消费，不复制为本地 Maven 模块。

```text
student-management-organization
├── common
├── domain
├── application
├── infrastructure
├── adapter
└── starter
```

模块依赖方向固定为：

```text
common         <- domain <- application <- adapter <- starter
                         \                    /
                          -> infrastructure -/
```

更精确地说：`domain -> common`，`application -> domain`，
`infrastructure -> domain + Evaluation Facade`，`adapter -> application +
Organization Facade`，`starter -> adapter + infrastructure`。Starter 只负责装配，
canonical Facade 不反向依赖任何生成模块。

## 2. 分层职责与边界

### 2.1 common

common 提供工程内的异常、常量和通用工具；Common MP Starter 由 domain 依赖并
技术持久化能力仅由 infrastructure 使用：`EgonModel`、`EgonColaIRepository`、`EgonColaRepository`、
`EgonColaMapper`、租户线、审计填充、逻辑删除、乐观锁和校验能力。模板不复制这些
基础类，也不在模块内声明独立的 MyBatis-Plus 版本。

### 2.2 domain

domain 只表达领域实体、聚合、值对象、枚举、事件、校验器和服务契约。业务服务
接口位于 `domain/<业务域>/service`，使用泛型形式：

```java
public interface UserDomainService { User save(User user); }
// Infrastructure: DomainServiceImpl -> UserRepository -> UserDAO (explicit XML)
```

domain 不声明 DAO、PO 或技术实现；服务实现不放在 domain。domain 可以依赖
Common MP Starter 暴露的契约类型，这是本模板为统一 Service/Model 合同保留的
唯一持久化相关依赖。

### 2.3 application

application 编排用例、事务、应用级校验、装配和结果转换。它只调用 domain
service/client 契约，不触碰 DAO、PO、MyBatis XML、RedisTemplate、消息模板或
外部 Facade client 实现。

### 2.4 infrastructure

infrastructure 承担所有出站技术实现。每个业务域使用以下结构：

```text
infrastructure/<domain>
├── repo
│   ├── dao          # EgonColaMapper 接口
│   ├── po           # EgonModel 持久化对象
│   └── converter    # BaseConverter / MapStruct 转换
├── service/impl     # DomainServiceImpl
├── cache
└── mq               # 仅出站
```

每一个 `*DomainServiceImpl` 都在 Infrastructure 实现对应的 domain service，
组合具体 Repository，Repository 继承 `EgonColaRepository<DAO, PO>`，通过 DAO 执行明确 SQL。DAO
继承 `EgonColaMapper<PO>`，XML 位于
`src/main/resources/mybatis/mapper/{user,teaching}`，namespace 必须精确指向
DAO。PO 必须继承 `EgonModel<PO>`，使用 MyBatis-Plus 的 `@TableName`、
`@TableField` 等映射注解。

PO 的 Lombok 约束是固定的：

```text
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Accessors(chain = true)
```

PO 不使用 `@RequiredArgsConstructor` 或 `@SuperBuilder`；Builder 只覆盖业务
字段，EgonModel 的租户、审计、删除和版本字段由 Common MP 映射/填充维护。具体
ServiceImpl 使用 Lombok `@RequiredArgsConstructor` 注入 DAO、校验器和配置。

### 2.5 adapter

adapter 是唯一入站适配层，负责 HTTP、GraphQL、入站 MQ、RPC Provider、Facade
实现、协议 DTO/VO 转换、过滤器和错误契约。Adapter 只能调用 application 和
canonical Provider Facade，不能直接访问 Infrastructure。

HTTP/GraphQL 的 ID 字段保留十进制字符串以避免 JavaScript 精度损失；进入
application/domain 前必须解析为正数 `Long`，非法或非正数直接拒绝。native Protobuf/Java
Facade 契约使用 `Long`。

### 2.6 starter

starter 只包含启动类和运行时装配。启动类使用 `@MapperScan` 扫描 user/teaching
DAO，使用 `@EnableConfigurationProperties(EgonColaMybatisPlusProperties.class)`
启用 Common MP 配置，并提供 `LongIdGenerator`。不得出现
`@EntityScan`、`@EnableJpaRepositories` 或业务 Service。

## 3. 主键、租户与持久化模型

业务 ID、关联 ID 和数据库主键统一为正数 `Long`。应用通过 Common
`LongIdGenerator` 生成新 ID；协议层只在 HTTP/GraphQL 文本边界使用十进制字符串。
租户和审计用户来自受信任的请求上下文/MDC，不接受 body、query 或 path 中的
调用方覆盖值；请求结束必须清理 MDC。

每张表都包含 EgonModel 技术字段：`id`、`tenant_id`、`create_user_id`、
`create_time`、`update_user_id`、`update_time`、`deleted_at` / `version`。活动值为 NULL，删除写 UTC 时间戳并递增版本，
所有业务唯一约束都带 `tenant_id` 和 `deleted_at` / `version`。

## 4. ShardingSphere 路由

主数据表 `users`、`roles`、`permissions`、`user_roles`、`role_permissions` 和
`grades` 固定在 `master_data`，显式使用 `databaseStrategy.none` 与
`tableStrategy.none`。`school_classes` 与 `school_class_users` 使用同一
`tenant_id` 做 database/table 双路由：

```text
tenant_long_database_bucket -> LongTenantShardingAlgorithm(target=database)
tenant_long_table_bucket    -> LongTenantShardingAlgorithm(target=table)
```

两张绑定表启用 `DML_SHARDING_CONDITIONS` 且 `allowHintDisable=false`；批量、更新、
删除和关联查询必须携带租户条件。`tenant_id` 缺失、非正数或范围路由一律失败，
不回退到广播或猜测路由。读写分离配置只改变 primary/replica 拓扑，不改变租户
路由键。

## 6. 运行与验证边界

`dev`、`test`、`prod` 三个 profile 的核心配置键保持一致。`test` 使用 H2
PostgreSQL 兼容模式、typed ShardingSphere 合同、受管 DDL 资源、Common MP Mapper XML
和本地替身；生成项目验证只证明编译、架构规则、H2/SQL 合同、路由和迁移静态
合同，不证明真实 PostgreSQL、Redis、RabbitMQ、Tianshu 服务发现、Tianquan-Shoubing 认证或生产
拓扑。

完整验证命令为：

```bash
bash ./mvnw -B -ntp clean verify
```

架构插件以 `unknownLayerPolicy=FAIL` 执行；生成验证器还必须检查：无 Spring Data
JPA/Hibernate 依赖和源码、八个 `EgonModel` PO、八个 `EgonColaMapper` DAO、四个
Infrastructure ServiceImpl、四个 domain Service 契约、八份 DAO XML、Long/tenant
迁移和四份 ShardingSphere 配置。验证期间不启动应用、不连接外部基础设施。



## Repository、CQRS 与 PostgreSQL

本脚手架使用 MyBatis-Plus 3.5.16：Domain Service 保留业务语义，具体 Repository 继承 `EgonColaRepository`，Mapper 继承 `EgonColaMapper`；查询全部使用显式 XML。PO 继承 `EgonModel` 的 id、tenantId、创建/更新用户与时间、`LocalDateTime deletedAt`、`Long version`。活动行是 NULL，软删写 UTC 时间戳并递增版本。AR/QueryChain 不启用，技术元数据强制填充；枚举与字段 handler 遵循 Common 合同。

`APP_DATASOURCE_MODE` 支持 `SHARDING` 与 `SHARDING_READWRITE`，事务类型为 LOCAL。单表使用明确的 `!SINGLE group.schema.table`；广播表只读。默认 legacy tenant 路由保持原地址。可选两级模板见 `src/test/resources/sharding/two-level-readwrite.yml`（多模块项目在 infrastructure 中）：先按 tenant_id 散列到 tenant slot，再按业务根 ID 散列到 bucket。订单与明细分别使用 id/order_id 共享同一根语义；同租户固定在一个物理组。

算法固定为 mix64-v1，T/B 是不超过 1024 的二次幂，乘积不超过 4096。库间均衡还依赖均衡 slot map 和租户负载；没有自动重分布。Query 缺次级键/范围查询受 fanout 上限约束，Command 必须有精确键。修改分布配置需配套新建表/迁移设计，不能直接套用测试模板到已有业务库。

初始化由 `ShardingDataSourceBootstrapper` 调用 Common 受管 DDL runner，仅对物理 PRIMARY 执行 `db/egon-mp/V20260913_001__initialize_repository_schema.sql` 与 `repository-manifest.json`。空库首次初始化；受管库验证 checksum/前缀/路由指纹；非空未受管库报 REBUILD_REQUIRED。旧 B/V/manual SQL 原样保留作档案，不再作为本脚手架运行入口。

读写分离使用 PostgreSQL 自身复制，普通读走 ROUND_ROBIN 副本，事务读/锁定读/强制主库读走 PRIMARY；不自动创建副本或故障选主。单表、广播表及业务物理表均携带 tenant_id。跨物理组写入会拒绝并标记回滚。

每个实例配置唯一 `EGON_ID_MACHINE_ID`，生产使用现有 Common Snowflake。各 profile 保持同一 MP 配置键；dev 才开启诊断，原始 recorder logger 为 OFF。动态表名默认关闭，只接受明确映射；MybatisBatch 在调用方事务中执行。

默认测试使用隔离 H2 和受控依赖。真实路由测试需 `-Degon.pg.routing=true` 与 `EGON_TEST_PG_URL`；主从测试需 `-Degon.pg.readwrite=true` 与 `EGON_TEST_PG_PRIMARY_URL`、`EGON_TEST_PG_REPLICA_URL`，并提供专用 `EGON_TEST_PG_USER/PASSWORD`。它们只创建/清理自己的 UUID schema，不启动数据库。PG/SS 运行、迁移和性能 EXPLAIN 由使用者手动验收，跳过不表示通过。
