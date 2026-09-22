# Egon-COLA Java、组件与 CQE 编码规范

更新：2026-09-22。本文规定新增及本次实际修改代码的目标规范；现有代码是模块结构与 API 的事实来源，旧实现不自动豁免新规范。文档修订不代表 Java、SQL 或运行环境已经完成迁移。

## 对象与 Lombok

普通 POJO、持久化实体和可变业务对象使用 `class`，不按字段多少改成 record。通用注解为 `@Data`、`@NoArgsConstructor`、`@AllArgsConstructor`、`@Accessors(chain = true)`；Builder 与相等性按场景选择：

| 场景 | 规范 |
| --- | --- |
| 无继承字段参与构建的普通类 | 使用 `@Builder`，核对全参构造目标 |
| 需要保持已有构造器或工厂逻辑 | 在对应构造器或方法上使用 `@Builder` |
| Builder 需要包含父类字段，如继承 `EgonModel` 的 PO | 使用 `@SuperBuilder`，实际父类链必须配套支持，不与 `@Builder` 混用 |
| 父类状态参与相等性 | `@EqualsAndHashCode(callSuper = true)` |
| 直接继承 Object | 不使用 `callSuper = true`；不能为注解虚构父类 |
| 不可变 value object | 可用 `record`，豁免可变 POJO 注解，集合和元素仍需不可变 |

类级 `@Builder` 必须有匹配构造目标；存在显式构造器时优先标注该构造器。与 Lombok 无参/全参构造器组合时，按项目实际版本检查生成结果。零字段类不能重复生成相同签名。`@RequiredArgsConstructor` 用于业务 Bean 注入，不作为 POJO 构造器堆叠要求。SuperBuilder 的 `toBuilder=true` 也要求父类链配套支持。版本与 import 以受管依赖为准。

依据：[Lombok Builder](https://projectlombok.org/features/Builder)、[SuperBuilder](https://projectlombok.org/features/experimental/SuperBuilder)、[EqualsAndHashCode](https://projectlombok.org/features/EqualsAndHashCode)。

record 必须具有不可变值语义，不能仅因为 DTO/Query/Command/Event 简短就采用。普通实体必须 class；值对象可用紧凑构造器做规范化和不变量检查，对可变引用防御性复制。PO、BO、DTO、VO、Query、Command、Event、Result 按真实角色命名；不机械创建每层一套对象。DAO/Mapper 是访问组件。

## 包、注释与业务实现

DDD 使用当前产品的领域优先目录，不自建第三套架构；准确模块依赖与端口位置见[架构总览](open-source-archetype-architecture.md)。简单三层结构保持现状。注释沿用相邻代码风格，解释业务含义、约束、事务/并发或兼容原因，不机械复述字段和方法。

业务类使用 @Slf4j、稳定显式 Spring Bean 名；依赖注入按项目约定使用 final 字段、@RequiredArgsConstructor 和 @Qualifier，核对 lombok.config 的注解传播。日志保留业务标识与失败阶段，不能输出秘密或不必要个人信息。不要为注入需求在 POJO 上叠加构造器注解。

复用现有组件和 JDK/已管理 Commons/Guava 工具，禁止重复基础工具。多环境配置保持 Key 结构一致，值可以不同；新增配置应有约束、默认值和秘密来源。设计模式只用于实际变化点、状态/规则组合和重复复杂分支，不为简单逻辑制造工厂、接口或层次；遵循本次任务的明确设计要求。

## 枚举与转换

- 入库业务编码字段使用 MP `@EnumValue`；前端 JSON 标量使用 Jackson `@JsonValue`。同一枚举跨两个边界时分别核对并同时配置。
- 复用适用的 `EgonEnum`，禁止 ordinal 业务编码；定义未知值、null、反序列化错误和历史编码兼容性。
- 转换复用 MapStruct/MapStructPlus 和 Common `BaseConverter<S,T>`（双向）或 `BaseForwardConverter<S,T>`（不可逆投影），保留项目现有 mapper/convertor 包名；不以 JSON、反射或 BeanUtils 代替业务映射。
- 外部 JSON 使用 Spring Boot Jackson，日期使用 `java.time`；明确 Long ID 在 HTTP/GraphQL 前端边界的字符串表示与内部数值表示。

## 组件复用与 MP Starter

必须尽量使用 `egon-cola-components`：先检查 Common、校验/转换、ID、MP、缓存、RPC、安全、线程池、观测、事务消息等已有能力。记录候选模块/API、匹配点、缺口和选择；已有组件满足时不自建替代基础设施，也不为“全部使用”向纯协议模块引入无关依赖。

持久化实现必须使用准确 Maven 工件 `top.egon:egon-cola-component-common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter`，版本由父 POM/BOM 管理。“Egon-Cola-MP-starter”只是简称。

- PO 继承 `EgonModel<T>`；复用 id、tenantId、审计字段、`LocalDateTime deletedAt` 和 version。
- Infrastructure 的具体 Repository 继承 `EgonColaRepository`，DAO/Mapper 继承 `EgonColaMapper`。Domain service 端口只声明业务语义，不继承技术 CRUD 接口。
- `dao`、`po`、`converter`、`repo` 按当前领域目录平级放置；禁止照搬旧 `repo/dao`、`repo/po` 布局。
- Command 使用受保护的 save、带版本 update/delete 与批处理，检查影响结果；Query 使用具名 Mapper XML，显式租户、有效行与路由条件。
- 禁止 ActiveRecord、QueryChain 和通用 Wrapper 绕过。自定义写 SQL 核对乐观锁、业务前态与路由键。
- 具体 Repository 声明缓存读写注解和一致的 Key/TTL；普通 CRUD 不隐式负责缓存失效。

## 注解驱动校验

基于 Jakarta 原生约束、组合/自定义 `@Constraint` 注解与 `ConstraintValidator` 扩展，配合 `@Valid` 级联、`@Validated` 方法激活、分组及必要的分组转换。

每个受影响边界明确约束、分组、规范化、错误映射与正反测试，覆盖 HTTP/RPC/MQ/内部入口、嵌套对象、非代理路径和自调用。`ValidationUtils` 只提供通用手工触发，不承载全部业务校验；已经由代理执行同一校验时不机械重复调用。跨字段规则使用类级约束，涉及数据库状态的业务不变量仍由所属领域/事务负责。电话规则复用 libphonenumber，自定义注解可以委托该库。

## 软删除唯一键

业务唯一键必须由业务列（有租户范围时包含 tenant_id）与 `deleted_at` 共同组成。用户简称 deleteAt 对应现有 Java `deletedAt`，不能另加同义字段。主键身份及明确永久去重的技术键单独说明语义。

当前 MP 有效行使用 NULL：普通 `UNIQUE (tenant_id, code, deleted_at)` 不能单独保证有效行唯一。必须保留联合键，并按数据库能力选择 NULLS NOT DISTINCT，或增加 `UNIQUE (tenant_id, code) WHERE deleted_at IS NULL` 有效行约束。只有部分索引不能静默替代用户要求的联合键；只在业务列上的旧唯一约束也不能保留并继续阻止删除后重建。

验证并发新建、删除后重建、多次软删除时间戳碰撞、恢复冲突、跨租户、业务列 NULL 和跨分片唯一性。联合键并不自动解决所有历史重复；具体升级应先验证旧数据。DDL 统一使用 MP-SDJ Starter 分布式管理；纠正新增 SQL 版本和 SHA-256 Manifest，已应用 SQL/history 不可改写，本次文档更新不执行数据库变更。

## CQE：Command、Query、Event

| 角色 | 责任 |
| --- | --- |
| Command | 状态变更意图、权限、约束、幂等、事务和失败语义 |
| Query | 只读检索，不隐藏业务写入或业务事件发布 |
| Event | 已发生事实，经事务 Outbox 或 MQ 中间件实际投递 |

事务消息复用 `egon-cola-component-transactional-outbox-starter` 的 `TransactionalOutbox.enqueue(OutboxMessage)`，在业务事务内使用并核对数据源、事务管理器与传输实现。`egon-cola-transcational-box` 是需求中的简称/拼写，不能虚构 Maven 工件。

允许直接 MQ，但必须说明发布确认、事务前后顺序、失败重试/补偿和 DB/MQ 双写窗口；需要原子业务写入与投递意图时采用 Outbox 或已验证的 broker 事务方案。单纯本地事件、@Async、日志或 afterCommit 回调不满足投递；afterCommit 真正调用 MQ 可以作为直接 MQ 路径，但不能宣称原子或 exactly-once。

事件契约必须包含事件 ID、业务标识、schema version、目标 topic/exchange/routing key、载荷、消费校验与幂等、重试/死信及观测。SSE 响应流与进程内生命周期回调不自动等于 CQE 业务事件；需明确分类，业务事实不能借此规避投递。

CQE 不要求新建 Event 模块、Event Sourcing 或独立读库。无事件的操作明确记为无事件，三层项目保持原结构。

## 当前实现与目标规范的差异

源码中仍有旧 `@Builder` PO 和普通 record 载体；必须按上述构造目标/值语义逐个判断，不能批量换注解。部分初始化 SQL 同时保留业务列单独 UNIQUE 和有效行部分索引，并未实现新的业务列 + deleted_at 联合键要求。

Web 的 `RabbitMqMessageServiceImpl` 在 afterCommit 发布 RabbitMQ，失败记录日志和指标，存在提交后丢失窗口，不能称为事务 Outbox。Agent knowledge 已使用事务 Outbox；研究/问答 SSE 是响应流。本文是后续修改的规范，不把这些差异描述成已修复。

## 验证与交付

检查模型/枚举、注解校验、组件与依赖边界、唯一键生命周期、CQE 真实投递及失败行为。先运行最小相关编译/测试，再运行适用架构与生成检查。源码、H2 或进程内测试不证明真实 PostgreSQL/ShardingSphere/MQ 验收；不自动启动应用、数据库、浏览器或容器。

## 分布式 DDL 管理约束

统一复用 `egon-mp-sdj-ext-starter` 对应的 `egon-cola-component-common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter`，不再选用 Flyway。使用组件 `EgonColaPostgreDdlRunner`、显式物理 PRIMARY/schema/role 目标、SQL 版本与 SHA-256 Manifest；数据源与拓扑由 Starter 管理，业务项目不得复制本地 Bootstrapper 或并行使用 MP 默认 IDdl/DdlApplicationRunner。

schema advisory transaction lock 协调多实例；每份 SQL 与 ddl_history 同连接同事务提交。跨物理目标不是全局原子事务，后续目标失败不撤销前面已提交目标；必须设计续跑与幂等。未知提交结果先用新连接核实再决定重试。检查锁/语句/拓扑超时、脚本前缀、checksum 与路由指纹；非空未受管库或漂移不能自动 DROP、repair 或接管历史。

一个逻辑变更新增一个下一版本 SQL 和 Manifest 条目，不改写已应用 SQL/history。配置基于 `egon.cola.component.mybatis-plus.ddl` 与现有 profile；实际接线及真实 PostgreSQL 并发/失败恢复需要独立验证，文档不声称已完成运行时迁移。
