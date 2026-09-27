# Transactional Outbox 与 Spring Statemachine 整合

源码基线：2026-09-27，Outbox 实现至 `deaac908d`。这是仓库接入规范，不表示上游最新版本；使用时须复核当前源码和 BOM。英文执行规范为 `references/outbox-statemachine-integration.md`。

用于 Outbox 接入、业务状态机 Event 消费、技术投递或旧数据迁移的设计与实施。普通 Query 或没有消息/状态机需求的任务不触发本规范。直接 MQ 以及已有 HTTP/RabbitMQ/custom handler 仍是有效的 CQE 方案，不要求所有 Event、Command 都启用业务状态机。

## 源码依据

以下路径从实际仓库根解析，不从 skill 目录解析：

- `O` = `egon-cola-components/egon-cola-component-transactional-outbox-starter`。
- `J` = `O/src/main/java/top/egon/cola/component/outbox`；`T` = `O/src/test/java/top/egon/cola/component/outbox`。
- 依赖：`O/pom.xml`、`egon-cola-components/egon-cola-components-bom/pom.xml`。当前 Spring Statemachine core 由 BOM 管理为 4.0.2；复用实际解析版本，本规范不授权升级或增加完整 Statemachine starter、data-* 持久化、Kryo。
- 技术生命周期：`J/statemachine/OutboxLifecycleStateMachineFactory.java`、`OutboxLifecycleService.java`、`StateMachineExecutionService.java`；`J/store/MybatisPlusOutboxStore.java`；`O/src/main/resources/mapper/outbox/OutboxMessageMapper.xml`。
- 业务消费：`J/statemachine/BusinessStateMachineService.java`、`BusinessStateMachineEvent.java`、`BusinessStateMachineSnapshotBO.java`、三个业务 SPI，以及 `J/delivery/statemachine/BusinessStateMachineDeliveryHandler.java`。
- 接线：`J/autoconfigure/OutboxBusinessStateMachineAutoConfiguration.java`、`OutboxStateMachineAutoConfiguration.java`、`OutboxMybatisPlusAutoConfiguration.java`、状态机/MP 两个 properties 类和 `J/migration`。
- 生效设计：`docs/egon/spec/2026-09-24-12-00-outbox-statemachine-cqe-integration.md`，特别是 DEC-009；实施记录：`docs/egon/plan/2026-09-24-13-16-outbox-statemachine-mp-implementation.md` Step 9。README 部分文字仍描述 SKIP LOCKED，应检查当前 Mapper 和生效 Spec，不能照搬旧文字。

## 1. 设计方案

### 1.1 范围、所有权与 CQE

保留已有单 Starter 结构。State 负责迁移判定，Strategy 负责宿主定义，Adapter 复用 DeliveryHandler。组件已经实现这些模式；业务接入通常只实现扩展点，不改写引擎。

| 职责 | 所有者与合同 |
| --- | --- |
| Command | 负责授权、校验和请求幂等；业务更新与 `TransactionalOutbox.enqueue(OutboxMessage)` 同一兼容本地事务。Outbox 去重不能替代 Command 的请求幂等。 |
| Query | 只读业务事实，不写数据、不发布 Event、不执行状态机。 |
| Event | 已发生的事实，具有稳定身份、版本与消费规则；经 Outbox 或真实 MQ producer 投递。`OutboxCommittedEvent` 只是唤醒提示。 |
| 技术生命周期 | Starter 自有 Spring Statemachine，Outbox 启用时必需；MP 消息行是权威状态。数据库锁/CAS决定提交者，不能取代状态机判定。 |
| 业务生命周期 | 宿主明确启用并拥有定义；业务行与持久化消费凭据是权威事实，内存 Machine/ExtendedState 不是持久化状态。 |

`statemachine` 是 Outbox worker 所在应用内的本地 DeliveryHandler，不会自动投递到另一个服务注册的定义。跨服务消费必须明确实际传输通道及接收合同。

Spec 对每条受影响链路明确 Command、Event、destination、聚合、可信租户来源、迁移表（源状态/信号/Guard/目标）、事务边界、重复/乱序规则与失败结果。区分“已入队”“传输完成”“业务迁移已提交”，保留至少一次投递语义，不宣称 exactly-once 或跨库原子性。

### 1.2 技术状态模型

| 源状态 | 信号 | 目标/条件 |
| --- | --- | --- |
| `__NEW__`（仅内存） | `ENQUEUE` | `PENDING` |
| `PENDING`、`RETRY_WAIT` | `CLAIM` | 到期后进入 `PROCESSING` |
| `PROCESSING` | `RECLAIM` | 租约过期后的外部自转换；换 owner，增加 attempt |
| `PROCESSING` | `DELIVERY_SUCCEEDED` | `SUCCEEDED` |
| `PROCESSING` | `DELIVERY_RETRYABLE` | 未耗尽 maxAttempts 为 `RETRY_WAIT`，否则 `DEAD` |
| `PROCESSING` | `SCHEDULE_RETRY` | Store 的显式 `markRetry`；保留该边，不重复实现重试预算策略 |
| `PROCESSING` | `DELIVERY_PERMANENT` | `DEAD` |
| `SUCCEEDED`、`DEAD` | — | 无重放/重置边；保留期清理只删除过期成功记录 |

不能给 reclaim 额外增加 maxAttempts 上限。租约过期但未被重领时，旧 owner 仍可完成；owner 已更换后，旧 worker 不能覆盖。技术成功记录清理会结束其 idempotencyKey 去重窗口，不应自动过期业务消费凭据。

### 1.3 业务事实与持久化消费凭据

复用 `BusinessStateMachineEvent`：`eventId`、正数 `tenantId`、`machineKey`、正数 `definitionVersion`、`businessId`、`eventType`、可空非负 `expectedVersion`、`Instant occurredAt`、Jackson 对象类型 `ObjectNode payload`。精确格式/长度以注解为准。expectedVersion 指消费目标聚合版本，不一定是生产方聚合版本。每个定义在访问持久化前校验自己的 payload。

区分传输 messageId、生产请求 key、Outbox idempotencyKey 与业务 eventId。消费凭据身份为 `(tenantId, machineKey, definitionVersion, businessId, eventId)`，保存组件传入的 fingerprint，不能另造指纹算法或只保留 lastEventId；E1→E2→重投E1仍应短路。

在聚合锁内，先查询凭据，再检查定义/expectedVersion：同身份同指纹返回成功，同身份不同内容为永久冲突。新事件定义不匹配或版本过旧为永久失败，未来版本可重试。expectedVersion 为空只省略该顺序校验，不取消锁、CAS和凭据去重。凭据保留期覆盖明确的重投/修复窗口；业务应用拥有存储和必要受管DDL，不创建组件通用业务表，也不把业务软删除规则机械套到永久技术凭据身份。

header/body 的 tenant 一致不等于授权。宿主 ContextExecutor 校验可信来源，同步绑定租户并在 finally 恢复，范围覆盖校验与业务事务。技术消息 tenant 固定为0，不能复制到业务字段，也不能把技术表 ignored-table 例外扩大到业务表。

### 1.4 事务与状态机限制

生产方业务更新与 enqueue 同一本地数据库事务。claim/reclaim/mark/cleanup 复用 worker 事务，claim 提交后再投递。可选业务 Handler 使用选定的 Outbox 事务管理器开启 `REQUIRES_NEW` / `READ_COMMITTED` 事务。业务 Repository 必须加入该事务，锁加载、状态CAS与凭据插入一同提交；`saveTransition` 内不能换数据源或另开独立 REQUIRES_NEW。

业务已提交而技术成功回写失败时允许重投，通过凭据防止重复业务迁移，不能仅因技术回写失败就补偿已提交业务。提交结果未知或暂时存储故障以原身份重试。

每次执行使用全新、未启动、平面单 region 的 Machine，显式单步外部转换（允许自转换）。该适配合同不支持缓存运行实例、共享 ExtendedState、timer、deferred event、do-action、层级/正交图或自动事件链。Guard/Action 只对复制的 Event/facts 做纯计算，不访问 JDBC/HTTP/MQ、不注册异步写，不依赖响应式执行线程的 ThreadLocal 租户；runner 返回后回到原事务线程持久化。这需要宿主代码审查与测试保证，不能宣称组件能拦截任意 Java I/O。

复用 `StateMachineExecutionService`：reset→start→sendEvent→消费结果 completion→验证恰好一次匹配的迁移完成及最终状态→有界 stop。仅 ACCEPTED 或仅观察状态未变化均不能证明成功。各阶段共享剩余截止时间，清理失败不能覆盖原始错误。

## 2. 实施细节

### 2.1 业务应用接入顺序

遵循实际宿主架构和生成器规范。Plan 把以下依赖映射到具体文件和验证，不按每一行机械造类：

1. 核实已有 Starter/BOM、本地 DataSource/TransactionManager/SqlSessionFactory 和传输通道。准确坐标为 `top.egon:egon-cola-component-transactional-outbox-starter`，transcational box 不是另一个依赖。
2. 确定业务图、Event payload、业务状态/版本列与持久化消费凭据。复用适合的已有表；只为缺失业务需求新增受管DDL，旧 SQL/history 不变。
3. 实现下表已有三个SPI。行为RED前先建立可编译合同；javac报错不是行为RED，不能提交空合同并宣称实现完成。
4. 注册具名 Bean、全新 Machine factory 和精确版本 destination，再启用适配。先部署能消费该版本的定义，再生产该版本事件；不把待投递旧事件静默升级到新图。
5. 在 Command 事务中按下述封装入队，Query 保持只读；复用组件 Service/Handler/runner，不另写 dispatcher 或存储引擎。
6. 先做真实状态机、宿主接线/事务测试，再做获准的隔离PG测试；代码完成与生产运行证据分别记录。

| 既有 SPI | 宿主实施要求 |
| --- | --- |
| `BusinessStateMachineDefinitionStrategy` | 实现 `destination()`、`stateMachineFactory()`、`repository()`、`validateEvent(event)`；destination 是唯一 `machineKey:definitionVersion`，定义负责 payload 校验。 |
| `BusinessStateMachineRepository` | `lockAndLoad(event,fingerprint)` 在 tenant/active/business identity 聚合锁下返回 `BusinessStateMachineSnapshotBO(currentState,version,definitionVersion,appliedFingerprint,facts)`，凭据必须按本次 Event 身份查询。`saveTransition(event,snapshot,targetState,fingerprint)` 返回void：tenant/identity/active/version条件CAS成功后同事务插入凭据；失败必须抛错回滚部分写入，可重试并发错误应使用现有Service能够分类的异常。 |
| `BusinessStateMachineContextExecutor` | `<T> T execute(event,Supplier<T> action)`；恰好一个名为 `outboxBusinessStateMachineContextExecutor` 的Bean，校验可信租户、同步调用并finally恢复上下文。 |

沿用具名Bean、带Qualifier的Lombok构造注入、注解/分组校验，以及真实模型边界上的MapStruct/Egon Converter。接口实现的参数约束不能违规加强，避免Bean Validation方法继承合同错误。

### 2.2 生产者消息封装

以下仅为已授权Command事务中的方法体片段。event 是已完整填写并校验的 `BusinessStateMachineEvent`，transportKey 针对此事实和destination稳定；这不是完整应用类，也不代替Command请求幂等。

```java
transactionalOutbox.enqueue(OutboxMessage.builder()
        .idempotencyKey(transportKey)
        .channel("statemachine")
        .destination(event.getMachineKey() + ":" + event.getDefinitionVersion())
        .contentType("application/json")
        .schemaVersion("1")
        .headers(Map.of(
                "egon-event-id", event.getEventId(),
                "egon-tenant-id", event.getTenantId().toString(),
                "egon-machine-key", event.getMachineKey(),
                "egon-definition-version", event.getDefinitionVersion().toString()))
        .payload(event)
        .build());
```

Handler 拒绝顶层未知字段与envelope/header/body不一致。业务definitionVersion与传输schemaVersion是不同概念。不要以已序列化字符串代替Event对象，也不要把凭据放入消息头或payload。顶层Jackson校验不能替代具体payload的领域规则。

### 2.3 配置与持久化接线

下列配置后缀均位于 `egon.cola.component.transactional-outbox`：

| 后缀 | 当前默认值/要求 |
| --- | --- |
| `state-machine.execution-timeout` | `100ms`；技术runner实际使用的单次执行预算 |
| `state-machine.claim-evaluation-timeout` | `1s`；启动校验executionTimeout≤该值≤leaseDuration。当前生命周期调用使用executionTimeout，不能据此宣称存在独立整批截止时间。 |
| `state-machine.business.enabled` | `false`；启用时必须有ContextExecutor及至少一个有效且唯一的definition/factory/repository |
| `state-machine.business.timeout` | `5s`，严格小于delivery.timeout；业务处理采用business和delivery deadline中较早者 |
| `storage.mp.sql-session-factory-bean-name` | `sqlSessionFactory` |
| `storage.mp.migration-mode` | `false`；仅本实例入队/调度维护门禁，不是全实例停写屏障 |
| `storage.mp.migration-lock-timeout` | `30s` |
| `storage.mp.manifest-resource` | `db/egon-outbox-mp/manifest.json` |

当前Store要求Common MP SHARDING/NATIVE/LOCAL、唯一PostgreSQL PRIMARY，以及指向egon_outbox schema技术消息表和ddl_history的两条SINGLE路由。选定的DataSourceTransactionManager、logical DataSource与SqlSessionFactory须引用相同DataSource，factory使用SpringManagedTransactionFactory。保留宿主原mapper locations/ignored tables，只增加所需精确技术表 `egon_cola_outbox_message`。Outbox不依赖Redis，不能因MP传递cache依赖就启Redis，也不能关闭宿主既有缓存需求。

复用 `OutboxManagedDdlInitializer` / `OutboxLogicalDataSourceFactory`，在logical datasource创建前调用现有 `EgonColaPostgreDdlRunner`。Starter拥有 `db/egon-outbox-mp/manifest.json`（component-outbox）及对应SQL，应用不得复制/改写组件脚本到自身迁移目录。自定义logical factory必须显式保留初始化hook。创建空schema/权限及执行生产迁移需要任务自身的运维授权。

### 2.4 组件维护与旧数据迁移

仅在任务确实修改Store内部或迁移旧安装时应用：

- claim/cleanup非锁候选扫描上限为 `min(10000, requestedLimit*4)`，公开请求limit为1..10000。逐候选通过目标表上带id/tenant0/active条件的SELECT取得 `pg_try_advisory_xact_lock(id)`，与CAS使用同一worker事务和PRIMARY。裸函数SELECT缺少所需SQL形状/表路由依据；当前ShardingSphere 5.5.3路径不要恢复不支持的 `FOR UPDATE SKIP LOCKED`。
- claim调用已有SSM，再按id/version/source/tenant/active/due或租约过期条件CAS；cleanup按id/version/SUCCEEDED/retention条件删除。锁false/缺行或CAS0跳过候选，真实数据库/引擎异常整批回滚。实际数量不超过请求limit，竞争时允许短批。advisory锁随事务结束释放，之后再投递；原owner完成操作的行锁/CAS语义继续保留。
- 可空幂等键必须验证实际PostgreSQL绑定：`idempotency_key = ?` 已保持NULL不匹配；未定类型的 `? IS NOT NULL` 在ShardingSphere路径可能触发参数类型错误。覆盖空/非空键和迁移续跑，不只检查XML。
- `OutboxLegacyMigrationService.migrate(sourceSchema,batchSize,verifyOnly)` 要求显式maintenance，运维停掉所有旧/新writer与worker，源/目标符合受支持的同PRIMARY拓扑。复用有界来源SHARE锁、keyset分批、目标原子写入、旧字段/指纹完整比较、续跑与最终验证。复制不重放状态机，也不能把DDL history当作数据已搬完。
- 保留旧表/SQL/history，不自动启动复制、不repair checksum、不在新表已有写入后直接退回旧表。生产切换要求数据verified且定义/消费者就绪；技能或测试命令不代表获准执行真实切换。

### 2.5 分阶段证据与有效验证

| 阶段 | 受影响范围内应交付的内容 |
| --- | --- |
| Spec设计 | 所有权/迁移与CQE合同，tenant/事务/凭据/版本规则，必要schema与错误/上线决策，明确是否启用业务适配。 |
| Plan | 宿主SPI/Bean/config/producer/storage精确文件、依赖顺序、行为RED/GREEN、SQL/manifest归属、负测/并发测试与提交范围；不重建已有组件类。 |
| 执行/最终审核 | 按Spec核对实际Bean接线、事务/凭据顺序、校验、Mapper SQL和测试报告；源码存在差距时如实记录，不因属性或图限制写在文档中就宣称已强制执行。 |

沿用现有Manual Check：MC-ARCH-001/MC-REUSE-001验证所有权与扩展点；MC-PATTERN-001验证实际State/Strategy/Adapter行为；MC-VALID-001/MC-BEAN-001/MC-CONFIG-001验证合同/接线；MC-MODEL-001/MC-TEST-001验证凭据、CAS、DDL与失败证据；MC-SCOPE-001核对组件与宿主范围。

按影响面选择：真实迁移完成、Guard拒绝/自转换；关闭/开启/缺失/重复定义；非法envelope、tenant拒绝与finally恢复；同指纹重复/异指纹冲突；E1/E2/E1；旧/未来版本；状态与凭据回滚；业务已提交技术回写失败；领取/重领/旧owner/清理竞争；迁移空键、复制/续跑/冲突/提交未知。

现有回归入口包括 `T/statemachine/*Test.java`、`T/autoconfigure/OutboxBusinessStateMachineAutoConfigurationTest.java`，以及 `T/integration/BusinessStateMachineIntegrationTest.java`、`PostgresqlOutboxConcurrencyIntegrationTest.java`、`PostgresqlOutboxCleanupIntegrationTest.java`、`OutboxMpMigrationIntegrationTest.java`、`OutboxManagedDdlIntegrationTest.java`。核对Maven实际发现规则。PG Failsafe需要 `EGON_OUTBOX_TEST_POSTGRES_ENABLED=true` 和Docker，默认跳过不能当运行通过。获准隔离运行时优先指定Outbox Failsafe类，避免 `-am verify`意外运行依赖模块外部集成。源码/编译/测试证据不授权也不证明生产迁移。
