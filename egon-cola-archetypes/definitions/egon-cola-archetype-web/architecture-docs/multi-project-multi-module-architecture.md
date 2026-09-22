# web 脚手架架构

更新：2026-09-22。以 `egon-cola-source-web` 的 POM、Java 和架构测试为事实来源。Organization HTTP/GraphQL/MQ；Web 使用自有 Organization Facade 和外部 Evaluation Facade。

## 项目与模块

```text
common
facade
domain
application
infrastructure
adapter
starter
```

| 模块 | 直接非测试内部依赖 |
| --- | --- |
| common | 无 |
| facade | 无 |
| domain | common |
| application | domain |
| infrastructure | domain |
| adapter | application, facade |
| starter | adapter, infrastructure |


## 当前源码定位

以下入口相对正常源工程 `egon-cola-source-web`；生成工程替换项目/包名前缀。修改前重查源码，不按旧文档猜类名。

| 责任 | 源码入口 |
| --- | --- |
| Domain 业务端口 | `egon-cola-source-web-domain/src/main/java/top/egon/cola/archetype/source/web/domain/service/CommandIdempotencyService.java` |
| Domain 业务端口 | `egon-cola-source-web-domain/src/main/java/top/egon/cola/archetype/source/web/domain/service/OrganizationEventService.java` |
| Domain 业务端口 | `egon-cola-source-web-domain/src/main/java/top/egon/cola/archetype/source/web/domain/teaching/service/EvaluationQueryService.java` |
| Application 用例 | `egon-cola-source-web-application/src/main/java/top/egon/cola/archetype/source/web/application/teaching/manage/GradeManage.java` |
| Application 用例 | `egon-cola-source-web-application/src/main/java/top/egon/cola/archetype/source/web/application/teaching/manage/SchoolClassManage.java` |
| Application 用例 | `egon-cola-source-web-application/src/main/java/top/egon/cola/archetype/source/web/application/user/manage/PermissionManage.java` |
| Infrastructure Repository | `egon-cola-source-web-infrastructure/src/main/java/top/egon/cola/archetype/source/web/infrastructure/teaching/repo/GradeRepository.java` |
| Infrastructure Repository | `egon-cola-source-web-infrastructure/src/main/java/top/egon/cola/archetype/source/web/infrastructure/teaching/repo/SchoolClassRepository.java` |
| Infrastructure Repository | `egon-cola-source-web-infrastructure/src/main/java/top/egon/cola/archetype/source/web/infrastructure/teaching/repo/SchoolClassUserRepository.java` |
| 架构校验 | `egon-cola-source-web-starter/src/test/java/top/egon/cola/archetype/source/web/architecture/WebArchitectureTest.java` |

此表是定位入口，不是全部用例清单；完整业务契约与运行配置见生成工程 README 和对应源码/测试。

## 分层与协议

Domain 持有业务对象、规则和服务端口；Application 编排用例和事务；Infrastructure 实现领域端口并拥有 PO、Mapper/XML、Repository、缓存、MQ 与外部 client；Adapter 处理协议、校验和转换；starter/start 只装配。业务代码按领域优先放置，如 `infrastructure/user/dao`、`infrastructure/user/po`、`infrastructure/user/converter`、`infrastructure/user/repo`，这些技术目录平级。

Domain service 不继承技术 CRUD 接口，Infrastructure 不反向依赖 Application。Facade 是当前项目自己的协议叶子；对端契约通过发布工件消费，不复制一份对端 Proto。Native/Open 保持各自当前 POM 中的平台或公共栈协议依赖，不因其他模板存在某项依赖就引入。

## 持久化与数据库

持久化模块必须使用 `egon-cola-component-common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter`。PO 继承 `EgonModel`，具体 Repository 继承 `EgonColaRepository`，DAO 继承 `EgonColaMapper`。Command 使用受守卫写入并检查版本/影响结果，Query 使用具名 Mapper XML。禁止 ActiveRecord、QueryChain 和通用 Wrapper 绕过。Domain 不暴露技术 PO 泛型。

组件统一管理数据源/拓扑与 `EgonColaPostgreDdlRunner` 受管 DDL，使用 `db/egon-mp` 与 `repository-manifest.json`，仅对物理 PRIMARY 执行初始化/检查。空库初始化；受管库核对 checksum、前缀和路由指纹；非空未受管库报 REBUILD_REQUIRED。旧 B/V/manual SQL 是档案，不是运行入口。`spring.sql.init.mode=never` 不会禁用该组件 runner。

保留当前 Snowflake Long ID、租户路由、version 和 `LocalDateTime deletedAt`；NULL 表示有效。业务唯一键必须组合业务列与 deleted_at，租户业务键还需 tenant_id。普通含 NULL 的联合唯一键无法单独保证有效行唯一，需验证数据库 NULLS NOT DISTINCT 能力或联合键 + 有效行部分唯一索引。检查旧业务列单独唯一约束、删除后重建、重复删除时间戳、恢复、并发和分片语义。当前 SQL 尚需逐表核对；本次文档修订不改旧 migration 或 checksum。

## Java 对象与校验

普通 POJO/实体用 class；只有不可变值对象可用 record，且集合/元素仍需不可变。class 通用注解为 @Data、@NoArgsConstructor、@AllArgsConstructor、@Accessors(chain = true)。普通构造目标用 @Builder；需要构建父类字段时用 @SuperBuilder 并检查整条父类链，两者不能混用。父类状态参与相等性时用 @EqualsAndHashCode(callSuper = true)，直接 Object 子类不能强用。核对全参目标和零字段重复构造器，不为注解添加无意义继承。

持久化枚举用 @EnumValue，前端编码用 @JsonValue，按当前 EgonEnum 和 Jackson 契约处理未知值。转换复用 MapStruct/MapStructPlus 和 BaseConverter。校验基于原生/自定义 @Constraint、ConstraintValidator、@Valid、@Validated、分组与级联；ValidationUtils 只作通用手工触发，不能集中承载全部业务校验。

## CQE 与组件复用

Command 是状态变更意图，Query 是无业务副作用读取，Event 是已经发生的事实。Event 必须经过 `egon-cola-component-transactional-outbox-starter` 或实际 MQ 中间件。Outbox 的 `TransactionalOutbox.enqueue(OutboxMessage)` 需要与业务事务正确绑定；直接 MQ 必须说明确认、失败重试/恢复和双写窗口。只有本地监听、异步回调或日志不算投递。afterCommit 实际调用 MQ 不能宣称原子投递或 exactly-once。

事件契约明确 eventId、业务标识、schema version、路由、消费校验/幂等及重试/死信。复用 egon-cola-components 现有 Common、ID、MP、缓存、线程池、协议和观测能力；先列候选 API 与缺口，再决定是否新增实现。纯领域/协议模块不引入无关运行依赖。

## 验证边界

当前模块 POM、架构测试与 packaging verifier 共同限定结构。现有旧 Builder/record 和业务唯一约束不能被描述成已满足新规范；后续改代码时逐个核对。源码、H2 和进程内 RPC 测试不证明真实 PostgreSQL/ShardingSphere、MQ、注册中心、TLS 或跨进程验收。生成检查只证明包装确定性，不自动启动项目、数据库或容器。

## 分布式 DDL 管理约束

统一复用 `egon-mp-sdj-ext-starter` 对应的 `egon-cola-component-common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter`，不再选用 Flyway。使用组件 `EgonColaPostgreDdlRunner`、显式物理 PRIMARY/schema/role 目标、SQL 版本与 SHA-256 Manifest；数据源与拓扑由 Starter 管理，业务项目不得复制本地 Bootstrapper 或并行使用 MP 默认 IDdl/DdlApplicationRunner。

schema advisory transaction lock 协调多实例；每份 SQL 与 ddl_history 同连接同事务提交。跨物理目标不是全局原子事务，后续目标失败不撤销前面已提交目标；必须设计续跑与幂等。未知提交结果先用新连接核实再决定重试。检查锁/语句/拓扑超时、脚本前缀、checksum 与路由指纹；非空未受管库或漂移不能自动 DROP、repair 或接管历史。

一个逻辑变更新增一个下一版本 SQL 和 Manifest 条目，不改写已应用 SQL/history。配置基于 `egon.cola.component.mybatis-plus.ddl` 与现有 profile；实际接线及真实 PostgreSQL 并发/失败恢复需要独立验证，文档不声称已完成运行时迁移。
