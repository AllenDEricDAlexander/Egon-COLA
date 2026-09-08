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
向所有下游提供 `EgonModel`、`EgonColaIService`、`EgonColaServiceImpl`、
`EgonColaMapper`、租户线、审计填充、逻辑删除、乐观锁和校验能力。模板不复制这些
基础类，也不在模块内声明独立的 MyBatis-Plus 版本。

### 2.2 domain

domain 只表达领域实体、聚合、值对象、枚举、事件、校验器和服务契约。业务服务
接口位于 `domain/<业务域>/service`，使用泛型形式：

```java
public interface UserDomainService<P extends EgonModel<P>>
        extends EgonColaIService<P> {
    // 领域语义方法
}
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
继承 `EgonColaServiceImpl<DAO, PO>`，通过 DAO 完成 CRUD 和领域语义查询。DAO
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
`create_time`、`update_user_id`、`update_time`、`is_deleted`。逻辑删除值为 `0/1`，
所有业务唯一约束都带 `tenant_id` 和 `is_deleted`。

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

## 5. Flyway 与 SQL 迁移

legacy Web 模板继续由 Infrastructure 在物理 primary 创建逻辑数据源前执行
Flyway。Spring Boot Flyway 自动配置被排除；replica 和逻辑数据源永不作为
Flyway target。迁移目录仅有：

```text
db/migration/sharding/master-data
db/migration/sharding/shard
```

既有 `V20260726_001`、`V20260726_002` 不得修改；本次各独立 location 只新增一个
版本：`V20260825_003`（master-data）和 `V20260825_004`（shard）。新增迁移先做
未知历史数据 preflight，不能在 SQL 内猜测旧身份与租户；空脚手架和明确种子才可
自动映射。所有迁移文件开头必须依次包含 `变更内容`、`影响范围`、`兼容性说明`。

## 6. 运行与验证边界

`dev`、`test`、`prod` 三个 profile 的核心配置键保持一致。`test` 使用 H2
PostgreSQL 兼容模式、嵌入式 ShardingSphere、Flyway SQL、Common MP Mapper XML
和本地替身；生成项目验证只证明编译、架构规则、H2/SQL 合同、路由和迁移静态
合同，不证明真实 PostgreSQL、Redis、RabbitMQ、DDC 服务发现、IdP 认证或生产
拓扑。

完整验证命令为：

```bash
bash ./mvnw -B -ntp clean verify
```

架构插件以 `unknownLayerPolicy=FAIL` 执行；生成验证器还必须检查：无 Spring Data
JPA/Hibernate 依赖和源码、八个 `EgonModel` PO、八个 `EgonColaMapper` DAO、四个
Infrastructure ServiceImpl、四个 domain Service 契约、八份 DAO XML、Long/tenant
迁移和四份 ShardingSphere 配置。验证期间不启动应用、不连接外部基础设施。

## Dependency and runtime ownership

Generated projects inherit the released `top.egon:egon-cola-archetypes-parent` at a concrete version with an empty `relativePath`. The parent imports the Components BOM, manages Common dependencies and ShardingSphere 5.5.3, and keeps Commons Lang at 3.20.0. Consumer modules inherit their own project root. Install the matching parent/BOM and required artifacts locally before validating an unpublished release; a local install does not publish artifacts.

This native family uses Egon RPC unary Protobuf contracts (gRPC 1.75.0 / Protobuf 4.32.0), the RPC DDC adapter, DDC configuration and HTTP registration, and the platform OpenAPI MVC starter. Runtime configuration lives in `application.yml` plus the dev/test/prod files; imported configuration uses Spring Boot Config Data. Supply the DDC endpoints, HMAC credentials, TLS material and IdP SERVICE client settings described in the generated README. Test profiles disable external integration lifecycles.

Web exposes ten Organization operations through `top.egon:egon-cola-organization-facade` and consumes Evaluation through `top.egon:egon-cola-evaluation-facade`. Existing business facade DTOs, HTTP/GraphQL/MQ behavior and database contracts are retained.

Platform API document governance is opt-in. Controllers need explicit, unique `@Operation(operationId = ...)` values before enabling that catalog; existing business endpoints remain accessible with the default configuration. Live DDC/IdP/TLS discovery, cross-process RPC and production rollout require operator acceptance.
