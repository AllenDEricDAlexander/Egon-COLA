# Transactional Outbox 双层状态机与 MP 迁移实施计划

| Field | Value |
| --- | --- |
| Document | `2026-09-24-13-16-outbox-statemachine-mp-implementation.md` |
| Template Version | `4` |
| Status | `Completed` |
| Created | `2026-09-24 13:16 CST` |
| Updated | `2026-09-27 08:52 CST` |
| Owner | `mario` |
| Repository | `Egon-COLA` |
| Scope | `egon-cola-component-transactional-outbox-starter`、组件BOM、Common MP manifest family；仅必要的测试与组件README |
| Source Requirement | 用户确认双层Spring Statemachine/CQE Spec后明确选择Outbox存储迁移MP，并调用egon-coding-writing-plan要求开始计划 |
| Baseline Revision | `main @ 20fd847bdea92e43db081842454075ab1f524768`；2026-09-24 13:11 CST，主Spec为本任务未跟踪文档；`.qoderignore`与其它Agent工作不在范围 |
| Implements Spec | [双层状态机与MP迁移Spec](../spec/2026-09-24-12-00-outbox-statemachine-cqe-integration.md) |
| Spec Status | `Implemented` |
| Spec Revision | `Updated 2026-09-27 08:52 CST`；原基线 `b039589bedd9e23be7dd4c69920bfd38a191e694`；用户已批准DEC-009并授权实施 |
| Effective Specs | [双层状态机与MP迁移Spec](../spec/2026-09-24-12-00-outbox-statemachine-cqe-integration.md) |
| Depends On Plans | `None` |
| Supersedes | `None` |
| Superseded By | `None` |
| Related Plans | `None` |

## 1. Summary

本Plan的Steps 1–8已提交。隔离PostgreSQL验收发现ShardingSphere 5.5.3无法解析claim/cleanup中的`FOR UPDATE SKIP LOCKED`后，用户批准Spec DEC-009并授权Corrective Step 9。Step 9以有界候选读取、由目标表路由的PostgreSQL transaction advisory lock和状态/version CAS替换该锁形状，覆盖并发claim、reclaim与cleanup；不新增依赖、表/列/索引、公开Store签名或配置。全部九个Step均已实现并通过各自门禁；代码仍由Egon MP持久化、状态迁移由Spring Statemachine决定。

用户先授权基于Review Spec编写Plan，随后明确授权执行；实现依据和交付状态记录在§7、§8及§12。历史07-24 Outbox设计和09-20组件收敛的指定条款通过主Spec关系继承；本Plan仅实现主Spec列明的范围。

## 2. Target Spec and Effective Design

### 2.1 Primary target

- Path: [双层状态机与MP迁移Spec](../spec/2026-09-24-12-00-outbox-statemachine-cqe-integration.md)。
- Status: `Implemented`；用户明确授权按本Plan实施，全部九个Step完成并通过计划门禁。
- Revision: 初版Spec Updated 2026-09-24 13:32 CST；2026-09-27用户批准DEC-009，最终实现证据记录于§8/§12。Step9执行基线为`521b8d8639d37026ebf06c11847c2bc3ed8a502a`，Steps1–8已有前序提交。
- Approval evidence: 用户“confirm, now start write plan”、此前“迁移”关闭DEC-004；用户2026-09-27回复“确认，按照推荐方案做”批准Spec DEC-009。

### 2.2 Effective Spec set

| Role | Spec/link | Status/revision | Effective sections | Why included |
| --- | --- | --- | --- | --- |
| Primary | [双层状态机与MP迁移Spec](../spec/2026-09-24-12-00-outbox-statemachine-cqe-integration.md) | Implemented，Updated 2026-09-27 08:52 | §§1–20，REQ-001～REQ-010按原号 | 唯一实施目标和字段/状态/DDL/兼容真源 |
| Normative inherited context | [07-24 Outbox原设计](../../superpowers/specs/2026-07-24-transactional-outbox-component-design.md) | 历史审阅稿；以当前源码核验 | §8至少一次/重复窗口、§9公共入口、§15–20租约与重试，排除主Spec显式修订§4.15、§6、§12–14、§17 | 保留原at-least-once与旧投递协议；不恢复旧双模块树 |
| Normative inherited context | [09-20公共契约收敛](../spec/2026-09-20-12-18-archetype-component-contract-convergence.md) 与[批准记录](2026-09-20-12-59-archetype-component-contract-implementation.md) | Spec Draft元数据/Plan Review；相关用户批准已在Plan §2.1登记 | 主Spec Depends On的§6.1基础组件结构、§11 Outbox Snowflake与旧DDL不可变 | 只继承所指不变量；该大型前序其余REQ不成为本任务实现需求 |

`Effective Specs`头字段列唯一要求集合主Spec；上述两个前序以**精确章节**提供规范上下文，主Spec已逐条重述本任务适用约束。Plan不擅自实施前序范围内的其它28条REQ。

### 2.3 Superseded or excluded content

07-24文档中的旧双模块树、旧JDBC持久化方案及“业务事件建模框架不在范围”相关句子由主Spec §2.3/§8/§11覆盖；当前组件源码是一单Starter。业务平台Yuheng/Tianshu/Tianquan、历史SQL1、真实业务PO/表/页均在本计划之外。没有其它未解决的同级Accepted Spec冲突。

## 3. Effective Requirements and Acceptance

| Requirement | Source Spec section | Effective statement | Observable acceptance | Implementation impact |
| --- | --- | --- | --- | --- |
| REQ-001 | §4, §7.3.1 | 技术outbox各状态边由SSM决定 | ENQUEUE/CLAIM/RECLAIM/成功/重试/死信真实模型测试与SQL目标一致 | Steps 1,4,5,9 |
| REQ-002 | §4, §9.2.2/8 | 业务Event通过SSM消费 | 新channel启用后真实模型处理已发生事实并落权威业务状态 | Step 6 |
| REQ-003 | §4, §9.2.8 | Command/Query/Event职责清楚 | Query无写/投递；Event有稳定事实identity和协议 | Steps 6,8 |
| REQ-004 | §4, §7.3.4, §11 | 回滚/租约/owner正确 | 双worker竞争、advisory lock、CAS、迟到owner、未知commit与原子enqueue测试 | Steps 4,5,6,9 |
| REQ-005 | §4, §9.2.3/4 | 业务Event重复不二次迁移 | E1/E2/E1、回写失败后重投与指纹冲突测试 | Step 6 |
| REQ-006 | §4, §9.2.6, §10 | tenant/定义/状态版本受控 | 错tenant拒绝、finally恢复、旧/未来版本分流 | Steps 4,6 |
| REQ-007 | §4, §8, §15 | 单Starter、业务适配可选且旧协议兼容 | 关开配置、旧API/HTTP/Rabbit/custom回归，core始终启用 | Steps 1,4,6,8 |
| REQ-008 | §4, §14/20 | 文档和验证证据完整 | 双语README、测试矩阵、准入门禁与静态/运行边界记录 | Steps 1–9 |
| REQ-009 | §4, §10.5, §11 | 技术Store全迁至Egon MP | PO继承EgonModel、Repo/DAO/XML真实走MP，无JDBC运行回退 | Steps 1–5,8,9 |
| REQ-010 | §4, §9.2.10/11, §11/16 | 受管DDL和无损旧消息切换 | SQL2单版本、SHA-256、runner与copy逐字段核对/可续跑/回滚限制 | Steps 2,7,8,9 |

## 4. Implementation Strategy and Dependency Order

### 4.1 Ordered strategy

先加入用户确认的Spring Statemachine依赖和技术生命周期图，再扩展公共DDL family并通过MP映射建立受管技术表。Store复用唯一logical DataSource/SqlSessionFactory并保留OutboxStore签名；Dispatcher使用技术图完成状态决策；业务Event新通道可选启用，业务状态与消费凭据同事务保存。旧消息copy只允许在显式维护模式中执行。Step 9根据隔离PostgreSQL证据，以有界候选、目标表路由的transaction advisory lock和state/version CAS兼容ShardingSphere parser；不得引入新依赖、表/列/索引或公开Store签名。

可编译顺序为：BOM/POM→技术类型/runner→DDL脚本/manifest/family/hook→PO/DAO/XML/Converter→Store/wiring→Dispatcher→Event/SPI/Handler→migration service→README。Step 4与Step 7会再次触碰MybatisPlusOutboxStore；Step 4只实现标准消息CRUD，Step 7仅加入maintenance门禁。Step 2与Step 4会触碰OutboxMybatisPlusAutoConfiguration；前者只注册DDL引导，后者注册Mapper/Store。Step 4/6会追加同一AutoConfiguration.imports文件的不同条目。所有重复路径的符号边界和验证理由在§7明确。

### 4.2 Test-first strategy

| Slice | RED file and expected reason | GREEN file(s) | Focused proof |
| --- | --- | --- | --- |
| 技术状态图 | StateMachineExecutionServiceTest/OutboxLifecycleServiceTest：旧代码没有runner和双Guard | 四个statemachine类型、异常、properties | TEST-001/010 |
| 受管schema | EgonColaDdlManifestFamilyTest/OutboxManagedDdlIntegrationTest：旧family拒绝、无hook | Common校验、SQL2/manifest、initializer/factory | TEST-013 |
| MP对象/映射 | OutboxMessageConverterTest：无PO、映射、headers保真 | PO/enum/MapStruct/DAO/XML/Repository | TEST-011字段与静态XML |
| 默认Store | OutboxMybatisPlusAutoConfigurationTest/OutboxMybatisPlusIntegrationTest：旧默认为JDBC | MP Store/自动配置与原测试迁移 | TEST-002/011 |
| Dispatcher | 先建立required lifecycle constructor/Bean alias/wiring compile contract；再由OutboxDispatcherTest证明旧switch未调用生命周期服务RED | Dispatcher通过信号与目标函数表使用真实生命周期结果 | TEST-001/003 |
| 业务事实 | BusinessStateMachineServiceTest/BusinessStateMachineDeliveryHandlerTest：无新通道、无持久receipt分支 | Event/Snapshot/SPI/Service/Handler/自动配置 | TEST-004/005/006/007 |
| 停写copy | OutboxMpMigrationIntegrationTest：无维护门禁、不可复制/校验 | LegacyMigrationService/Result、Store/Dispatcher门禁 | TEST-012 |
| 文档/合同收口 | ComponentContractGovernanceTest：新依赖/Bean/无JDBC回退声明未冻结 | README中英文、已有兼容测试 | TEST-008/009/全量 |
| PG/SS锁兼容纠正 | 当前claim/cleanup parser RED | bounded candidates/advisory xact lock/state-version CAS | TEST-003/011 scoped PG Failsafe |

编译依赖无法在测试先行前满足时仅提前添加空合同或已批准的POM作为编译前置；随后测试仍需在相应行为不存在时RED。PG/Testcontainers最初受用户授权门禁；用户于2026-09-27明确授权本机隔离运行，实际运行范围和测试数见§8。该授权不包含生产数据库或生产迁移。

### 4.3 Sequential and parallel boundaries

| Step | Depends on | May run in parallel with | Must not overlap with | Reason |
| --- | --- | --- | --- | --- |
| 1 技术图/依赖 | None | None | 全部后续步骤 | 后续引用SSM API与版本 |
| 2 受管DDL | 1 | None | 3/4 | 新表DDL/历史先于MP写路径 |
| 3 MP类型/映射 | 2 | 业务适配契约的纯模型分析（不写文件） | 4 | 表列与PO映射需冻结 |
| 4 默认MP Store | 3 | None | 5/6/7 | 所有旧测试与消费者必须在同一步转换 |
| 5 Dispatcher | 4 | None | 6/7 | 技术状态完成规则先稳定 |
| 6 业务Event适配 | 5 | None | 7/8 | 消费完成与回写重复窗口联通 |
| 7 旧消息copy | 6 | None | 8 | 停写门禁和目标MP必须就绪 |
| 8 文档/总门禁 | 7 | None | None | 原PG验收发现parser不兼容后追加用户批准的Corrective Step9 |
| 9 PG锁兼容纠正 | 8 | None | None | 独立修复PG/SS解析与并发验收缺口；仅变更Step9清单路径 |

用户说有别的Agent在工作；本计划不分配并行写入，不处理它们的Yuheng路径。计划中不同Step语义虽可分析并行，落盘实施按上述顺序，避免同一个自动配置/Store/Dispatcher文件竞争修改。

### 4.4 Commit boundaries

每Step为一个逻辑变更，实施时只stage本Step `Commit paths`；每Step最多一个语义commit，符合当前AGENTS“每个逻辑任务最多一commit”，并记录实际提交后进入下一Step。Steps 1–8已有前序提交；Step 9包含其Spec、Plan修订与19个Outbox路径。其它Agent的改动始终留在工作区外，不覆盖、不清理、不提交。

### 4.5 Spec Simplicity and Implementation-necessity Audit

| Spec element | Spec necessity verdict/section | Current repository evidence | Direct/reuse alternative | Interaction/implementation cost | Plan decision |
| --- | --- | --- | --- | --- | --- |
| 双层SSM与共用runner | §7.0 Add；REQ-001/002 | 旧Dispatcher switch、Store SQL | 保留switch不满足用户 | 一个依赖/每次机器创建与有界等待 | Implement Step 1/5/6 |
| 原OutboxStore/DeliveryHandler SPI | §7.0 Keep；REQ-007 | J/store/OutboxStore.java、delivery/DeliveryHandler.java | 新bus/新worker更贵 | 零新网络接口 | Already exists，保留签名 |
| MP PO/DAO/XML/Repo/Converter | §7.0 Add；REQ-009 | 旧JDBC Store与Common EgonModel/EgonColaMapper | JDBC不满足用户迁移 | 一组技术映射/受管拓扑 | Implement Steps 3/4 |
| 新技术表/ddl_history/SQL/manifest | §7.0 Add；REQ-010 | runner拒绝非空无历史；旧SQL1需不动 | ALTER旧表会冒认历史 | 停写/空schema/数据复制 | Implement Steps 2/7 |
| 业务Event/DefinitionStrategy/Repository/ContextExecutor | §7.0 Add；REQ-002/005/006 | DeliveryHandler扩展点；无业务表归属 | 任意payload/只内存listener失去验证/持久幂等 | 本地SPI；接入方需有receipt | Implement Step 6 |
| 迁移维护入口 | §9.2.10 Add；REQ-010 | 无旧表迁移工具 | 启动自动复制导致竞态 | 停机维护与完整对比 | Implement Step 7 |
| 新REST/GraphQL/页面/cache/job | §3/7.0 Remove/N/A | Starter无Controller/UI | 当前本地SPI已有入口 | 0额外调用 | Do not implement |

独立审查：没有fetch-then-forward接口；tenant来自业务可信上下文、技术owner固定0；业务eventId和transport messageId分离；未追加策略表/Inbox/定时作业；MapStruct仅跨真实PO→record边界；新schema确由公共DDL运行器不可接纳旧非空schema这一硬约束导出。Spec审批覆盖这些成本，Plan不扩范围。

### 4.6 Change-unit Dependency Matrix

| Change unit | Requirements | Proof/RED point | Compile/runtime prerequisites | Produces | Consumers/unblocks | Owning Step |
| --- | --- | --- | --- | --- | --- | --- |
| 技术SSM | REQ-001/008 | TEST-001/010 | POM SSM core4.0.2 | runner/技术图 | Store/Dispatcher/业务适配 | Step 1 |
| DDL | REQ-010 | TEST-013 | Common MP runner、1SQL/family | egon_outbox受管schema | PO/Mapper、迁移 | Step 2 |
| MP映射 | REQ-009 | TEST-011 converter | EgonModel/Schema/processor | PO/DAO/XML/Repo | Store | Step 3 |
| Store | REQ-004/007/009 | TEST-002/011 | 前三步+physical/logical DS | MP OutboxStore | Dispatcher/消费/迁移 | Step 4 |
| 分派 | REQ-001/004 | TEST-003 | Store/技术图 | 技术结果→状态提交 | 业务投递 | Step 5 |
| Event | REQ-002/003/005/006/007 | TEST-004/005/006/007 | SSM/Store/Dispatcher | 可选业务Handler/SPI | 维护模式/文档 | Step 6 |
| 数据copy | REQ-010 | TEST-012 | DDL/Store/停写门禁 | 可续跑完整对比 | 切换与回滚 | Step 7 |
| 文档/门禁 | REQ-008 | TEST-008/009及模块回归 | 全部Step | 双语接入与审核证据 | 用户执行决策 | Step 8 |
| parser兼容claim/cleanup | REQ-001/004 | TEST-003/011 scoped PG | Shardingsphere 5.5.3与唯一PRIMARY SINGLE route | candidates/advisory lock/CAS | MP Store/cleanup | Step 9 |

### 4.7 Java, Spring, and Egon-COLA Implementation Standards

| Concern | Current repository evidence | Effective Spec decision | Planned implementation consequence | Owning Steps/checks |
| --- | --- | --- | --- | --- |
| Architecture profile | O单Starter内api/store/dispatch/delivery/autoconfigure；前序09-20§6.1明确components基础库不建业务层 | Spec DEC-002保留该基础组件结构；接入业务按自身Traditional或Archetype | 不新增业务模块或biz.*混合层；Common MP最小family修订 | 全Step，MC-ARCH-001 |
| Reuse/capability | OutboxStore/DeliveryHandler、MP EgonModel/EgonColaRepository/EgonColaMapper、PostgreDdlRunner/LogicalDataSourceFactory | §6.1复用；SSM core新增 | 仅指定依赖；无第二DDL/bootstrap/消息总线 | Steps 1–7，MC-REUSE/DEP |
| Naming/model/validation/conversion | Common BasePojo/BaseForwardConverter、ValidationUtils、components/lombok.config | §§9–10 | Event/BO/PO用正确Lombok；每handoff @Valid/@Validated；MapStruct真正前向投影 | Steps 3/6，MC-NAME/MODEL/VALID/CONVERT |
| Bean/logging/util/JSON/time/config | O现有autoConfig/Boot Jackson/java.time；Qualifier复制已配置 | §§8/10/15 | 具名Bean、final qualified字段、@Slf4j；局部严格Jackson reader；Instant/Duration；双语profile键矩阵 | Steps 1–8，MC-BEAN/LOG/UTIL/JSON/TIME/CONFIG |
| Business variation/pattern | Dispatcher原switch与多DeliveryHandler策略 | §13 State/Strategy/Adapter | 固定技术FSM；定义destination选择表；handler适配旧SPI | Steps 1/5/6，MC-PATTERN |

#### Capability reuse ledger

| Need | Candidates inspected | Exact evidence | Fit/gap | Decision | Added dependency/custom code | Owning Step/check |
| --- | --- | --- | --- | --- | --- | --- |
| 生命周期引擎 | JDK/Spring State/Scheduler、原switch | J/dispatch/OutboxDispatcher.java | 无用户指定SSM | 加SSM core | `org.springframework.statemachine:spring-statemachine-core:4.0.2`，BOM管理；传递Reactor需effective-pom核对 | Step1 MC-DEP/REUSE |
| MP存储/DDL | Spring JDBC旧实现、Common MP/runner | J/store/PostgresqlJdbcOutboxStore.java；MJ/extension/ddl/sharding | MP/runner已在组件，同O未依赖 | 复用 | `top.egon:egon-cola-component-common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter:${project.version}`；SS/Cache传递影响 | Steps1–4 MC-DEP/REUSE |
| 转换/校验 | Spring ObjectMapper/Validation、Common BaseForwardConverter | C/converter、O/validation/OutboxMessageValidator.java | 新PO→Record才需mapper | MapStruct/Jakarta | `org.mapstruct:mapstruct:1.6.3`和现有processor；不加BeanUtils映射 | Steps1/3 MC-CONVERT/VALID |
| Event可靠投递 | 原enqueue/handler/重试 | J/api/TransactionalOutbox.java、delivery/DeliveryHandler.java | 已满足at-least-once | Keep | None；业务层receipt由消费者提供 | Step6 MC-REUSE/PATTERN |
| tenant/时钟/缓存 | MP MDC provider/MetaObjectHandler/Clock、既有cache | MJ/business/EgonColaTenantIdProvider.java、egonColaMybatisPlusClock、cache starter | 需要技术owner0的局部作用域；不需缓存读取 | 小Executor复用existingMDCKEY与finally，宿主无缓存需求关cache.enabled | No新框架 | Steps3/4 MC-CONFIG/BEAN |

### 4.8 User-mandated Java Rule Implementation Matrix

| Literal rule | Spec source | Repository evidence | Exact files and order | Pseudocode obligations | Validation gate | Steps | Status/blocker |
| --- | --- | --- | --- | --- | --- | --- | --- |
| Rule 1 | §6.2/8/10 | 当前OutboxRecord/Status、Common EgonModel | Event/BO/PO→Service/DAO/Repository/Handler/Factory | 逐类型语义后缀；不生造Data/Info/Bean | 编译+命名审查 | 1,3,6,7,9 | PASS |
| Rule 2 | §6.2/9/10.3 | O validation starter、ValidationUtils；无HTTP入口 | Event/Service→Repository/Mapper与迁移输入→维护Service | Default @Valid/@Validated；DAO/Store batch约束对齐；直接路径ValidationUtils；状态Guard单独 | TEST-004/011/012、Step9并发Mapper验证 | 3,4,6,7,9 | PASS |
| Rule 3 | §6.2/10.5 | EgonModel @SuperBuilder、BaseForwardConverter | PO/Converter→Event/Snapshot/MigrationResult | class四注解+@SuperBuilder/callSuper；MapStruct one-way | 编译/TEST-009/011/012 | 3,6,7 | PASS |
| Rule 4 | §6.2/8 | components/lombok.config Qualifier | 各业务Bean→自动配置 | @Slf4j/具名Bean/@RequiredArgsConstructor/final @Qualifier | TEST-007/011 ContextRunner、Step9 wiring | 1–7,9 | PASS |
| Rule 5 | §6.2/15 | JDK/现有Common | runner/Converter/迁移 | JDK+已管理Commons/Guava；无新Utils | import/BOM搜索 | 全Step | PASS |
| Rule 6 | §6.2/9/10 | 旧JacksonOutboxMessageSerializer与OutboxStatus | Event/Handler/Status→PO/Mapper | Jackson ISO Instant；@EnumValue文本；无ordinal | TEST-004/009/011 | 3/6 | PASS |
| Rule 7 | §6.2/15 | O无生产application*.yml；README双语 | Properties→配置测试→README | 全profile同键树；值可异；组件双语示例一致 | TEST-007/config key检查 | 1,2,4,8 | PASS |
| Rule 9 | §6.2/13 | 原switch/handler registry | Factory/Strategy/Adapter→Dispatcher/业务Service | 实际SSM Guard决定状态，Map按destination路由；DB锁只协调提交 | TEST-001/004/003 | 1/5/6/9 | PASS |
| Rule 10 | §6.2/10/11/15 | 原Instant/Duration/PGtimestamptz | PO/Event/业务模型→Mapper | UTC Instant/PG微秒/Duration deadline，Clock统一 | TEST-009/010/011、Step9 due/retention | 1/3/6/7/9 | PASS |
| Rule 11 | §6.1/6.2/8/11 | 用户确认基础组件结构与MP治理 | 所有文件依Spec组件边界顺序 | MP PO/Mapper/Repo、DDL runner、CQE outbox，技术owner0仅技术表 | TEST-011/013/文件边界复核 | Every Step | PASS |

## 5. Change File Tree

```text
MODIFY  egon-cola-components/egon-cola-components-bom/pom.xml  [Step 1]
MODIFY  egon-cola-components/egon-cola-component-transactional-outbox-starter/pom.xml  [Step 1]
CREATE  egon-cola-components/egon-cola-component-transactional-outbox-starter/src/test/java/top/egon/cola/component/outbox/statemachine/StateMachineExecutionServiceTest.java  [Step 1]
CREATE  egon-cola-components/egon-cola-component-transactional-outbox-starter/src/test/java/top/egon/cola/component/outbox/statemachine/OutboxLifecycleServiceTest.java  [Step 1]
CREATE  egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/common/exception/OutboxStateMachineException.java  [Step 1]
CREATE  egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/statemachine/OutboxLifecycleSignalEnum.java  [Step 1]
CREATE  egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/statemachine/StateMachineExecutionService.java  [Step 1]
CREATE  egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/statemachine/OutboxLifecycleStateMachineFactory.java  [Step 1]
CREATE  egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/autoconfigure/OutboxStateMachineProperties.java  [Step 1]
CREATE  egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/statemachine/OutboxLifecycleService.java  [Step 1]
MULTI  egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/autoconfigure/OutboxStateMachineAutoConfiguration.java  [Step 1,5]
CREATE  egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter/src/test/java/top/egon/cola/component/common/mybatis/ddl/EgonColaDdlManifestFamilyTest.java  [Step 2]
MODIFY  egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter/src/main/java/top/egon/cola/component/common/mybatis/ddl/EgonColaDdlManifestBO.java  [Step 2]
CREATE  egon-cola-components/egon-cola-component-transactional-outbox-starter/src/test/java/top/egon/cola/component/outbox/integration/OutboxManagedDdlIntegrationTest.java  [Step 2, Step 9]
CREATE  egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/resources/db/egon-outbox-mp/V20260924_001__initialize_outbox_mp_schema.sql  [Step 2]
CREATE  egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/resources/db/egon-outbox-mp/manifest.json  [Step 2]
CREATE  egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/autoconfigure/OutboxMpStorageProperties.java  [Step 2, Step 9]
CREATE  egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/migration/OutboxManagedDdlInitializer.java  [Step 2]
CREATE  egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/migration/OutboxLogicalDataSourceFactory.java  [Step 2, Step 9]
MULTI  egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/autoconfigure/OutboxMybatisPlusAutoConfiguration.java  [Step 2,4,7]
CREATE  egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/persistence/po/OutboxMessagePO.java  [Step 3]
MODIFY  egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/store/OutboxStatus.java  [Step 3]
CREATE  egon-cola-components/egon-cola-component-transactional-outbox-starter/src/test/java/top/egon/cola/component/outbox/persistence/OutboxMessageConverterTest.java  [Step 3, Step 9]
CREATE  egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/persistence/converter/OutboxHeadersConverter.java  [Step 3]
CREATE  egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/persistence/converter/OutboxMessageConverter.java  [Step 3]
MULTI   egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/persistence/dao/OutboxMessageDAO.java  [Step 3,4, Step 9]
CREATE  egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/resources/mapper/outbox/OutboxMessageMapper.xml  [Step 3, Step 9]
CREATE  egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/persistence/repository/OutboxMessageRepository.java  [Step 3, Step 9]
MULTI  egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/persistence/OutboxTechnicalContextExecutor.java  [Step 3,7]
MULTI   egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/persistence/OutboxSchemaMetadataValidator.java  [Step 3,4]
CREATE  egon-cola-components/egon-cola-component-transactional-outbox-starter/src/test/java/top/egon/cola/component/outbox/autoconfigure/OutboxMybatisPlusAutoConfigurationTest.java  [Step 4]
CREATE  egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter/src/main/java/top/egon/cola/component/common/mybatis/extension/EgonColaExplicitTenantScopeMapper.java  [Step 4]
MODIFY  egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter/src/test/java/top/egon/cola/component/common/mybatis/autoconfigure/EgonColaMybatisPlusAutoConfigurationTest.java  [Step 4]
MODIFY  egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter/src/main/java/top/egon/cola/component/common/mybatis/autoconfigure/EgonColaMybatisPlusContractValidator.java  [Step 4]
MODIFY  egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/persistence/dao/OutboxMessageDAO.java  [Step 4, Step 9]
CREATE  egon-cola-components/egon-cola-component-transactional-outbox-starter/src/test/java/top/egon/cola/component/outbox/integration/OutboxMybatisPlusIntegrationTest.java  [Step 4, Step 9]
MULTI  egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/store/MybatisPlusOutboxStore.java  [Step 4,7, Step 9]
MULTI  egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/autoconfigure/TransactionalOutboxAutoConfiguration.java  [Step 4,5,7]
MULTI  egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports  [Step 4,6]
MULTI  egon-cola-components/egon-cola-component-transactional-outbox-starter/src/test/java/top/egon/cola/component/outbox/autoconfigure/TransactionalOutboxAutoConfigurationTest.java  [Step 4,5]
MODIFY  egon-cola-components/egon-cola-component-transactional-outbox-starter/src/test/java/top/egon/cola/component/outbox/autoconfigure/OutboxOptionalAutoConfigurationTest.java  [Step 5]
MULTI  egon-cola-components/egon-cola-component-transactional-outbox-starter/src/test/java/top/egon/cola/component/outbox/store/OutboxLongPrimaryKeyTest.java  [Step 4,7, Step 9]
MULTI  egon-cola-components/egon-cola-component-transactional-outbox-starter/src/test/java/top/egon/cola/component/outbox/contract/ComponentContractGovernanceTest.java  [Step 4,8]
MODIFY  egon-cola-components/egon-cola-component-transactional-outbox-starter/src/test/java/top/egon/cola/component/outbox/integration/OutboxDataSafetyIntegrationTest.java  [Step 4]
MODIFY  egon-cola-components/egon-cola-component-transactional-outbox-starter/src/test/java/top/egon/cola/component/outbox/integration/PostgresqlOutboxCleanupIntegrationTest.java  [Step 4, Step 9]
MODIFY  egon-cola-components/egon-cola-component-transactional-outbox-starter/src/test/java/top/egon/cola/component/outbox/integration/PostgresqlOutboxConcurrencyIntegrationTest.java  [Step 4, Step 9]
MODIFY  egon-cola-components/egon-cola-component-transactional-outbox-starter/src/test/java/top/egon/cola/component/outbox/integration/PostgresqlOutboxQueryPlanIntegrationTest.java  [Step 4, Step 9]
MULTI  egon-cola-components/egon-cola-component-transactional-outbox-starter/src/test/java/top/egon/cola/component/outbox/integration/PostgresqlOutboxRecoveryIntegrationTest.java  [Step 4,5,7]
MODIFY  egon-cola-components/egon-cola-component-transactional-outbox-starter/src/test/java/top/egon/cola/component/outbox/integration/TransactionalOutboxTransactionIntegrationTest.java  [Step 4]
MODIFY  egon-cola-components/egon-cola-component-transactional-outbox-starter/src/test/java/top/egon/cola/component/outbox/transaction/DefaultTransactionalOutboxTest.java  [Step 4]
MODIFY  egon-cola-components/egon-cola-component-transactional-outbox-starter/src/test/java/top/egon/cola/component/outbox/integration/PostgresqlOutboxTestSupport.java  [Step 4, Step 9]
DELETE  egon-cola-components/egon-cola-component-transactional-outbox-starter/src/test/java/top/egon/cola/component/outbox/integration/PostgresqlJdbcOutboxStoreIntegrationTest.java  [Step 4]
DELETE  egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/store/PostgresqlJdbcOutboxStore.java  [Step 4]
MULTI  egon-cola-components/egon-cola-component-transactional-outbox-starter/src/test/java/top/egon/cola/component/outbox/dispatch/OutboxDispatcherTest.java  [Step 5,7]
MULTI  egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/dispatch/OutboxDispatcher.java  [Step 5,7]
CREATE  egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/statemachine/BusinessStateMachineEvent.java  [Step 6]
CREATE  egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/statemachine/BusinessStateMachineSnapshotBO.java  [Step 6]
CREATE  egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/statemachine/BusinessStateMachineRepository.java  [Step 6]
CREATE  egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/statemachine/BusinessStateMachineContextExecutor.java  [Step 6]
CREATE  egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/statemachine/BusinessStateMachineDefinitionStrategy.java  [Step 6]
CREATE  egon-cola-components/egon-cola-component-transactional-outbox-starter/src/test/java/top/egon/cola/component/outbox/statemachine/BusinessStateMachineServiceTest.java  [Step 6]
CREATE  egon-cola-components/egon-cola-component-transactional-outbox-starter/src/test/java/top/egon/cola/component/outbox/delivery/statemachine/BusinessStateMachineDeliveryHandlerTest.java  [Step 6]
CREATE  egon-cola-components/egon-cola-component-transactional-outbox-starter/src/test/java/top/egon/cola/component/outbox/autoconfigure/OutboxBusinessStateMachineAutoConfigurationTest.java  [Step 6]
CREATE  egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/statemachine/BusinessStateMachineService.java  [Step 6]
CREATE  egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/delivery/statemachine/BusinessStateMachineDeliveryHandler.java  [Step 6]
CREATE  egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/autoconfigure/OutboxBusinessStateMachineAutoConfiguration.java  [Step 6]
MULTI  egon-cola-components/egon-cola-component-transactional-outbox-starter/src/test/java/top/egon/cola/component/outbox/integration/BusinessStateMachineIntegrationTest.java  [Step 6,7, Step 9]
CREATE  egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/migration/OutboxMigrationResult.java  [Step 7]
CREATE  egon-cola-components/egon-cola-component-transactional-outbox-starter/src/test/java/top/egon/cola/component/outbox/integration/OutboxMpMigrationIntegrationTest.java  [Step 7]
CREATE  egon-cola-components/egon-cola-component-transactional-outbox-starter/src/test/java/top/egon/cola/component/outbox/migration/OutboxLegacyMigrationServiceTest.java  [Step 7]
CREATE  egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/migration/OutboxLegacyMigrationService.java  [Step 7, Step 9]
MODIFY  egon-cola-components/egon-cola-component-transactional-outbox-starter/README.zh-CN.md  [Step 8]
MODIFY  egon-cola-components/egon-cola-component-transactional-outbox-starter/README.md  [Step 8]
MODIFY  docs/egon/plan/2026-09-24-13-16-outbox-statemachine-mp-implementation.md  [Step 9]
MODIFY  docs/egon/spec/2026-09-24-12-00-outbox-statemachine-cqe-integration.md  [Step 9]
MODIFY  egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/store/OutboxStore.java  [Step 9]
MODIFY  egon-cola-components/egon-cola-component-transactional-outbox-starter/src/test/java/top/egon/cola/component/outbox/integration/TransactionalMessageAnnotationSampleTest.java  [Step 9]
MODIFY  egon-cola-components/egon-cola-component-transactional-outbox-starter/src/test/java/top/egon/cola/component/outbox/integration/TransactionalOutboxDirectApiSampleTest.java  [Step 9]
```


| Operation | Path | Current evidence/symbol | Final symbols/state | Responsibility | Step | Requirements | Validation owner |
| --- | --- | --- | --- | --- | --- | --- | --- |
| MODIFY | `egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/store/OutboxStore.java` | Existing validated Store SPI | Matching @Positive/@Max batch contracts | Preserve public method names and validation boundary | Step 9 | REQ-001,REQ-004 | TEST-003 |
| MODIFY | `egon-cola-components/egon-cola-component-transactional-outbox-starter/src/test/java/top/egon/cola/component/outbox/integration/TransactionalMessageAnnotationSampleTest.java` | Existing public sample fixture | Real MP Store/SS wiring | Schema-qualified table and shared storage | Step 9 | REQ-004,REQ-008,REQ-009 | TEST-011 |
| MODIFY | `egon-cola-components/egon-cola-component-transactional-outbox-starter/src/test/java/top/egon/cola/component/outbox/integration/TransactionalOutboxDirectApiSampleTest.java` | Existing public sample fixture | Real MP Store/SS wiring | Schema-qualified table and shared storage | Step 9 | REQ-004,REQ-008,REQ-009 | TEST-011 |
| MODIFY | `docs/egon/spec/2026-09-24-12-00-outbox-statemachine-cqe-integration.md` | Approved DEC-009 execution update | Normative lock contract / ordered correction Step | Governing documents | Step 9 | REQ-001,REQ-004,REQ-008 | TEST-003 |
| MODIFY | `docs/egon/plan/2026-09-24-13-16-outbox-statemachine-mp-implementation.md` | Approved DEC-009 execution update | Normative lock contract / ordered correction Step | Governing documents | Step 9 | REQ-001,REQ-004,REQ-008 | TEST-003 |

| MODIFY | `egon-cola-components/egon-cola-components-bom/pom.xml` | BOM既有outbox与Common MP管理项 | spring-statemachine-core dependencyManagement | 版本统一由组件BOM管理，先于Starter声明 | Step 1 | REQ-001,REQ-002,REQ-007 | TEST-001/010 |

| MODIFY | `egon-cola-components/egon-cola-component-transactional-outbox-starter/pom.xml` | O/pom.xml当前无SSM和MP依赖 | spring-statemachine-core, Egon MP, MapStruct + processor | 测试导入SSM/MP类型前先满足编译前置 | Step 1 | REQ-001,REQ-002,REQ-009 | TEST-001/010 |

| CREATE | `egon-cola-components/egon-cola-component-transactional-outbox-starter/src/test/java/top/egon/cola/component/outbox/statemachine/StateMachineExecutionServiceTest.java` | egon-cola-components/egon-cola-component-transactional-outbox-starter/src/test/java/top/egon/cola/component/outbox/transaction/DefaultTransactionalOutboxTest.java JUnit5风格 | runner actual completion and timeout tests | 先用真实4.0.2图固定ACCEPTED但Guard拒绝的RED | Step 1 | REQ-001,REQ-008 | TEST-001/010 |

| CREATE | `egon-cola-components/egon-cola-component-transactional-outbox-starter/src/test/java/top/egon/cola/component/outbox/statemachine/OutboxLifecycleServiceTest.java` | egon-cola-components/egon-cola-component-transactional-outbox-starter/src/test/java/top/egon/cola/component/outbox/dispatch/OutboxDispatcherTest.java旧重试分支测试 | all fixed graph transitions and retry guards | runner测试后锁定固定图边矩阵RED | Step 1 | REQ-001,REQ-004 | TEST-001/010 |

| CREATE | `egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/common/exception/OutboxStateMachineException.java` | egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/common/exception/OutboxException.java当前错误基类 | reason and isRetryable() | 模型拒绝与超时需区分，供runner/service调用 | Step 1 | REQ-001,REQ-008 | TEST-001/010 |

| CREATE | `egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/statemachine/OutboxLifecycleSignalEnum.java` | egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/store/OutboxStatus.java已有EgonEnum结构 | seven EgonEnum technical signals | 工厂需要穷举稳定信号代码 | Step 1 | REQ-001,REQ-003 | TEST-001/010 |

| CREATE/MODIFY | `egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/statemachine/StateMachineExecutionService.java` | egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/transaction/OutboxTransactionGuard.java展示同步事务边界 | execute(StateMachine<S,E>,S,Message<E>,Duration):S | 异常和状态信号已稳定，写共用有界完成语义 | Step 1 | REQ-001,REQ-002,REQ-008 | TEST-001/010 |

| CREATE/MODIFY | `egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/statemachine/OutboxLifecycleStateMachineFactory.java` | egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/dispatch/OutboxDispatcher.java现有switch与死信分支 | create():StateMachine<String,OutboxLifecycleSignalEnum> | runner提供完成后，定义唯一固定技术图 | Step 1 | REQ-001,REQ-004 | TEST-001/010 |

| CREATE | `egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/autoconfigure/OutboxStateMachineProperties.java` | egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/autoconfigure/TransactionalOutboxProperties.java既有属性嵌套 | executionTimeout, claimEvaluationTimeout, business.enabled/timeout | 生命周期Service需先有有界预算配置 | Step 1 | REQ-001,REQ-007 | TEST-001/010 |

| CREATE/MODIFY | `egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/statemachine/OutboxLifecycleService.java` | egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/store/OutboxStatus.java五态与守卫所需maxAttempts | evaluate(String,SignalEnum,int,int):OutboxStatus | 固定图和属性已可用，提供Store/Dispatcher共用决策 | Step 1 | REQ-001,REQ-004 | TEST-001/010 |

| CREATE/MODIFY | `egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/autoconfigure/OutboxStateMachineAutoConfiguration.java` | egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/autoconfigure/TransactionalOutboxAutoConfiguration.java现有具名Bean工厂模式 | engine beans and aliases; Step5真实别名现有TransactionalOutboxProperties为outboxStateMachineOutboxProperties | Step1状态图注册；Step5为Dispatcher完成具名属性合同 | Step 1、Step 5 | REQ-001,REQ-007 | TEST-001/010；Step5 ContextRunner |

| CREATE | `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter/src/test/java/top/egon/cola/component/common/mybatis/ddl/EgonColaDdlManifestFamilyTest.java` | egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter/src/test/java/top/egon/cola/component/common/mybatis/ddl/EgonColaPostgreDdlRunnerTest.java | component-outbox family and old-six-family assertions | 先固定现有构造会拒绝新family的RED | Step 2 | REQ-010 | TEST-013 |

| MODIFY | `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter/src/main/java/top/egon/cola/component/common/mybatis/ddl/EgonColaDdlManifestBO.java` | egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter/src/main/java/top/egon/cola/component/common/mybatis/ddl/EgonColaDdlManifestBO.java现有Set.of六项 | allowed family set component-outbox | TEST-013 family RED之后只扩准确一个值 | Step 2 | REQ-010 | TEST-013 |

| CREATE | `egon-cola-components/egon-cola-component-transactional-outbox-starter/src/test/java/top/egon/cola/component/outbox/integration/OutboxManagedDdlIntegrationTest.java` | egon-cola-components/egon-cola-component-transactional-outbox-starter/src/test/java/top/egon/cola/component/outbox/integration/PostgresqlOutboxTestSupport.java | DDL-before-logical and drift/rollback tests | 先定义启动顺序与历史不变量RED，PG执行须另获授权 | Step 2, Step 9| REQ-010 | TEST-013 |

| CREATE | `egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/resources/db/egon-outbox-mp/V20260924_001__initialize_outbox_mp_schema.sql` | egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/resources/db/transactional-outbox/postgresql/V1__create_transactional_outbox_schema.sql原22字段定义 | DDL-001 one SQL script | runner family已可解析，先固定表/索引合同 | Step 2 | REQ-009,REQ-010 | TEST-013 |

| CREATE | `egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/resources/db/egon-outbox-mp/manifest.json` | egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/resources/db/transactional-outbox/postgresql/V1__create_transactional_outbox_schema.sql存量Flyway只读 | family/version/path/SHA-256 entry | SQL字节冻结后才写manifest哈希 | Step 2 | REQ-010 | TEST-013 |

| CREATE | `egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/autoconfigure/OutboxMpStorageProperties.java` | egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/autoconfigure/TransactionalOutboxProperties.java已有storage配置 | sqlSessionFactoryBeanName,migrationMode,migrationLockTimeout,manifestResource | DDL引导与后续Store共享明确受控参数 | Step 2, Step 9| REQ-007,REQ-009,REQ-010 | TEST-013 |

| CREATE | `egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/migration/OutboxManagedDdlInitializer.java` | egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter/src/main/java/top/egon/cola/component/common/mybatis/ddl/EgonColaPostgreDdlRunner.java#runTarget及sharding/bootstrap/LogicalDataSourceFactory | initialize(Map<String,DataSource>,byte[]) | SQL与manifest就绪后复用runner，不创建第二DDL引擎 | Step 2 | REQ-009,REQ-010 | TEST-013 |

| CREATE | `egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/migration/OutboxLogicalDataSourceFactory.java` | egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter/src/main/java/top/egon/cola/component/common/mybatis/autoconfigure/EgonColaShardingAutoConfiguration.java#egonColaShardingLogicalDataSourceFactory | create(Map<String,DataSource>,byte[]):DataSource | initializer已能证明DDL就绪，再接既有physical→logical扩展点 | Step 2, Step 9| REQ-010 | TEST-013 |

| CREATE/MODIFY | `egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/autoconfigure/OutboxMybatisPlusAutoConfiguration.java` | egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/autoconfigure/TransactionalOutboxAutoConfiguration.java既有auto-config模式 | pre-sharding DDL factory registration only；MapperScan, MP Store, validation Bean names；migration Service Bean registration | 先接DDL hook，Store Bean要等Step3/4类型存在再加 | Step 2、Step 4、Step 7 | REQ-007,REQ-009,REQ-010 | TEST-012 |

| CREATE | `egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/persistence/po/OutboxMessagePO.java` | egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter/src/main/java/top/egon/cola/component/common/mybatis/model/EgonModel.java以及SQL1原字段 | @TableName egon_outbox; EgonModel<OutboxMessagePO> | Mapper/Converter tests need compileable contract first | Step 3 | REQ-009 | TEST-011 |

| MODIFY | `egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/store/OutboxStatus.java` | egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/store/OutboxStatus.java现有五值EgonEnum | message field @EnumValue | PO需要明确VARCHAR持久化码再生成映射 | Step 3 | REQ-001,REQ-009 | TEST-011 |

| CREATE | `egon-cola-components/egon-cola-component-transactional-outbox-starter/src/test/java/top/egon/cola/component/outbox/persistence/OutboxMessageConverterTest.java` | egon-cola-components/egon-cola-component-transactional-outbox-starter/src/test/java/top/egon/cola/component/outbox/serialization/JacksonOutboxMessageSerializerTest.java | PO→OutboxRecord, insert/import projection assertions | PO/enum编译前置有了，缺Converter应RED | Step 3, Step 9| REQ-009 | TEST-011 |

| CREATE | `egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/persistence/converter/OutboxHeadersConverter.java` | egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/validation/OutboxMessageValidator.java和serializer现有Boot ObjectMapper | parse headersJson with Boot Jackson | Converter对headers_json有真实协议映射需求 | Step 3 | REQ-009 | TEST-011 |

| CREATE | `egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/persistence/converter/OutboxMessageConverter.java` | egon-cola-components/egon-cola-component-transactional-outbox-starter/pom.xml Step1加入MapStruct处理器 | BaseForwardConverter<OutboxMessagePO,OutboxRecord> | helper可用后MapStruct生成实际跨界映射 | Step 3 | REQ-009 | TEST-011 |

| CREATE | `egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/persistence/dao/OutboxMessageDAO.java` | egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter/src/main/java/top/egon/cola/component/common/mybatis/extension/EgonColaMapper.java和源项目UserDAO样例 | EgonColaMapper<OutboxMessagePO> plus named SQL methods | 模型已可用，先固定查询/写入方法签名再写XML | Step 3, Step 9| REQ-004,REQ-009 | TEST-011 |

| CREATE | `egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/resources/mapper/outbox/OutboxMessageMapper.xml` | egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter/src/main/java/top/egon/cola/component/common/mybatis/interceptor/EgonColaOriginalSqlGuardInterceptor.java#shape拒绝UPDATE FROM | all named mapped SQL and resultMap | DAO签名确定后写XML，形成真实MP边界 | Step 3, Step 9| REQ-004,REQ-009 | TEST-011 |

| CREATE | `egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/persistence/repository/OutboxMessageRepository.java` | egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/java/top/egon/cola/archetype/source/light/infrastructure/user/repo/UserRepository.java | EgonColaRepository<OutboxMessageDAO,OutboxMessagePO> | DAO/XML已具名，Repository封装技术操作 | Step 3, Step 9| REQ-004,REQ-009 | TEST-011 |

| CREATE/MODIFY | `egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/persistence/OutboxTechnicalContextExecutor.java` | egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter/src/main/java/top/egon/cola/component/common/mybatis/business/EgonColaTenantIdProvider.java当前读MDC | execute(Callable/Supplier) with tenant0/audit；executeMigration audit actor | Repository/MP模型验证需要显式tenant0作用域 | Step 3、Step 7 | REQ-004,REQ-006,REQ-009,REQ-010 | TEST-012 |

| CREATE | `egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/persistence/OutboxSchemaMetadataValidator.java` | egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/store/OutboxSchemaValidator.java旧readiness检查 | validate target schema, indexes, ddl_history | Store会用原OutboxSchemaValidator委托此纯元数据检查 | Step 3 | REQ-009,REQ-010 | TEST-011 |

| CREATE | `egon-cola-components/egon-cola-component-transactional-outbox-starter/src/test/java/top/egon/cola/component/outbox/autoconfigure/OutboxMybatisPlusAutoConfigurationTest.java` | egon-cola-components/egon-cola-component-transactional-outbox-starter/src/test/java/top/egon/cola/component/outbox/autoconfigure/TransactionalOutboxAutoConfigurationTest.java ContextRunner风格 | context wiring, missing MP, aliases, DS/factory | 先RED旧配置会产JDBC Store或忽视MapperXML缺失 | Step 4 | REQ-007,REQ-009 | TEST-002/011 |

| CREATE | `egon-cola-components/egon-cola-component-transactional-outbox-starter/src/test/java/top/egon/cola/component/outbox/integration/OutboxMybatisPlusIntegrationTest.java` | egon-cola-components/egon-cola-component-transactional-outbox-starter/src/test/java/top/egon/cola/component/outbox/integration/PostgresqlJdbcOutboxStoreIntegrationTest.java当前JDBC合同 | real MP/SS/PG old Store contract | 真实边界RED：旧默认仍JDBC且技术MDC不恢复 | Step 4, Step 9| REQ-004,REQ-009 | TEST-002/011 |

| CREATE/MODIFY | `egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/store/MybatisPlusOutboxStore.java` | egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/store/PostgresqlJdbcOutboxStore.java现有Store SPI完整实现 | OutboxStore complete SPI；enqueue maintenance guard | DAO/Repo/PO/SSM齐全后替换存储事实的唯一默认实现 | Step 4、Step 7, Step 9| REQ-001,REQ-004,REQ-009,REQ-010 | TEST-012 |

| MODIFY | `egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/autoconfigure/TransactionalOutboxAutoConfiguration.java` | egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/autoconfigure/TransactionalOutboxAutoConfiguration.java#outboxStore/outboxDispatcher | remove JDBC default Store bean; Step5 qualified Dispatcher injection; Step7 passes the same `outboxMpStorageProperties` into maintenance gating | 新auto config发布MP Store后只消费SPI；Dispatcher构造时读取唯一MP storage properties Bean，不复制配置 | Step 4、Step 5、Step 7 | REQ-001,REQ-004,REQ-007,REQ-009,REQ-010 | TEST-002/011/012；ContextRunner/DispatcherTest |

| MODIFY | `egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` | egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports现有四行 | three new auto-config names, first core/MP；append optional Business auto-config | Step4新增配置可导出，Step6再追加业务适配条目 | Step 4、Step 6 | REQ-001,REQ-002,REQ-007,REQ-009 | TEST-004/005/006/007 |

| MODIFY | `egon-cola-components/egon-cola-component-transactional-outbox-starter/src/test/java/top/egon/cola/component/outbox/autoconfigure/TransactionalOutboxAutoConfigurationTest.java` | egon-cola-components/egon-cola-component-transactional-outbox-starter/src/test/java/top/egon/cola/component/outbox/autoconfigure/TransactionalOutboxAutoConfigurationTest.java当前mockStore路径 | Step4 default Store context assertions; Step5 named state-machine properties/Clock/lifecycleService alias and Dispatcher wiring | 删除旧具体Bean后原ContextRunner断言对应MP；Step5状态配置上下文真实可解析 | Step 4、Step 5 | REQ-001,REQ-007,REQ-009 | TEST-002/011；Step5 ContextRunner |

| MODIFY | `egon-cola-components/egon-cola-component-transactional-outbox-starter/src/test/java/top/egon/cola/component/outbox/autoconfigure/OutboxOptionalAutoConfigurationTest.java` | egon-cola-components/egon-cola-component-transactional-outbox-starter/src/test/java/top/egon/cola/component/outbox/autoconfigure/OutboxOptionalAutoConfigurationTest.java当前只加载core auto-config | add state-machine config/Clock fixture so optional HTTP/Rabbit tests also satisfy required Dispatcher dependencies | SSM成为Dispatcher必需依赖后隔离optional上下文仍需真实装配；不启动网络/broker | Step 5 | REQ-001,REQ-007 | Step5 ContextRunner |

| MODIFY | `egon-cola-components/egon-cola-component-transactional-outbox-starter/src/test/java/top/egon/cola/component/outbox/store/OutboxLongPrimaryKeyTest.java` | egon-cola-components/egon-cola-component-transactional-outbox-starter/src/test/java/top/egon/cola/component/outbox/store/OutboxLongPrimaryKeyTest.java当前MP构造fixture | Snowflake/identity unchanged; add migration-mode enqueue guard assertion | 旧测试从JDBC参数校验迁至MP PO/Mapper绑定；Step7复用fixture验证maintenance模式零写入 | Step 4、Step 7, Step 9| REQ-007,REQ-009,REQ-010 | TEST-002/011/012 |

| MODIFY | `egon-cola-components/egon-cola-component-transactional-outbox-starter/src/test/java/top/egon/cola/component/outbox/contract/ComponentContractGovernanceTest.java` | egon-cola-components/egon-cola-component-transactional-outbox-starter/src/test/java/top/egon/cola/component/outbox/contract/ComponentContractGovernanceTest.java引用旧Store类 | drop stale concrete JDBC contract; keep public SPI；final dual-SSM/MP/CQE policy assertions | 删除JDBC具体类前先删静态符号引用 | Step 4、Step 8 | REQ-003,REQ-007,REQ-008,REQ-009,REQ-010 | TEST-008/009/011/012/013 |

| MODIFY | `egon-cola-components/egon-cola-component-transactional-outbox-starter/src/test/java/top/egon/cola/component/outbox/integration/OutboxDataSafetyIntegrationTest.java` | egon-cola-components/egon-cola-component-transactional-outbox-starter/src/test/java/top/egon/cola/component/outbox/integration/OutboxDataSafetyIntegrationTest.java旧类引用由rg确认 | data safety old-store fixture | 当前直接构造PostgresqlJdbcOutboxStore，删除前转到MP或接口fixture | Step 4 | REQ-004,REQ-007,REQ-009 | TEST-002/011 |

| MODIFY | `egon-cola-components/egon-cola-component-transactional-outbox-starter/src/test/java/top/egon/cola/component/outbox/integration/PostgresqlOutboxCleanupIntegrationTest.java` | egon-cola-components/egon-cola-component-transactional-outbox-starter/src/test/java/top/egon/cola/component/outbox/integration/PostgresqlOutboxCleanupIntegrationTest.java旧类引用由rg确认 | retention cleanup old-store fixture | 当前直接构造PostgresqlJdbcOutboxStore，删除前转到MP或接口fixture | Step 4, Step 9| REQ-004,REQ-007,REQ-009 | TEST-002/011 |

| MODIFY | `egon-cola-components/egon-cola-component-transactional-outbox-starter/src/test/java/top/egon/cola/component/outbox/integration/PostgresqlOutboxConcurrencyIntegrationTest.java` | egon-cola-components/egon-cola-component-transactional-outbox-starter/src/test/java/top/egon/cola/component/outbox/integration/PostgresqlOutboxConcurrencyIntegrationTest.java旧类引用由rg确认 | two-worker claim old-store fixture | 当前直接构造PostgresqlJdbcOutboxStore，删除前转到MP或接口fixture | Step 4, Step 9| REQ-004,REQ-007,REQ-009 | TEST-002/011 |

| MODIFY | `egon-cola-components/egon-cola-component-transactional-outbox-starter/src/test/java/top/egon/cola/component/outbox/integration/PostgresqlOutboxQueryPlanIntegrationTest.java` | egon-cola-components/egon-cola-component-transactional-outbox-starter/src/test/java/top/egon/cola/component/outbox/integration/PostgresqlOutboxQueryPlanIntegrationTest.java旧类引用由rg确认 | claim/cleanup EXPLAIN old-store fixture | 当前直接构造PostgresqlJdbcOutboxStore，删除前转到MP或接口fixture | Step 4, Step 9| REQ-004,REQ-007,REQ-009 | TEST-002/011 |

| MODIFY | `egon-cola-components/egon-cola-component-transactional-outbox-starter/src/test/java/top/egon/cola/component/outbox/integration/PostgresqlOutboxRecoveryIntegrationTest.java` | egon-cola-components/egon-cola-component-transactional-outbox-starter/src/test/java/top/egon/cola/component/outbox/integration/PostgresqlOutboxRecoveryIntegrationTest.java旧类引用由rg确认 | expired lease reclaim MP fixture; Step5/7 pass context `OutboxLifecycleService`, state-machine `Clock` and `OutboxMpStorageProperties` into direct Dispatcher construction | Step4改为MP source；Step5/7同步具名构造依赖，测试仍保持环境门控 | Step 4、Step 5、Step 7 | REQ-001,REQ-004,REQ-007,REQ-009,REQ-010 | TEST-002/011/012；PG仅编译、不执行 |

| MODIFY | `egon-cola-components/egon-cola-component-transactional-outbox-starter/src/test/java/top/egon/cola/component/outbox/integration/TransactionalOutboxTransactionIntegrationTest.java` | egon-cola-components/egon-cola-component-transactional-outbox-starter/src/test/java/top/egon/cola/component/outbox/integration/TransactionalOutboxTransactionIntegrationTest.java旧类引用由rg确认 | producer atomicity old-store fixture | 当前直接构造PostgresqlJdbcOutboxStore，删除前转到MP或接口fixture | Step 4 | REQ-004,REQ-007,REQ-009 | TEST-002/011 |

| MODIFY | `egon-cola-components/egon-cola-component-transactional-outbox-starter/src/test/java/top/egon/cola/component/outbox/transaction/DefaultTransactionalOutboxTest.java` | egon-cola-components/egon-cola-component-transactional-outbox-starter/src/test/java/top/egon/cola/component/outbox/transaction/DefaultTransactionalOutboxTest.java旧类引用由rg确认 | enqueue unit old-store fixture | 当前直接构造PostgresqlJdbcOutboxStore，删除前转到MP或接口fixture | Step 4 | REQ-004,REQ-007,REQ-009 | TEST-002/011 |

| MODIFY | `egon-cola-components/egon-cola-component-transactional-outbox-starter/src/test/java/top/egon/cola/component/outbox/integration/PostgresqlOutboxTestSupport.java` | egon-cola-components/egon-cola-component-transactional-outbox-starter/src/test/java/top/egon/cola/component/outbox/integration/PostgresqlOutboxTestSupport.java当前只有旧V1 JdbcTemplate | one PG fixture for old source and new target | 旧集成测试共享fixture，先迁移到NATIVE单PRIMARY目标 | Step 4, Step 9| REQ-004,REQ-007,REQ-009,REQ-010 | TEST-002/011 |

| DELETE | `egon-cola-components/egon-cola-component-transactional-outbox-starter/src/test/java/top/egon/cola/component/outbox/integration/PostgresqlJdbcOutboxStoreIntegrationTest.java` | egon-cola-components/egon-cola-component-transactional-outbox-starter/src/test/java/top/egon/cola/component/outbox/integration/PostgresqlJdbcOutboxStoreIntegrationTest.java旧具体测试 | old concrete integration test class | 新OutboxMybatisPlusIntegrationTest已覆盖旧Store公开断言 | Step 4 | REQ-007,REQ-009 | TEST-002/011 |

| DELETE | `egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/store/PostgresqlJdbcOutboxStore.java` | egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/store/PostgresqlJdbcOutboxStore.java当前所有JDBC CRUD | old runtime JDBC persistence implementation | 所有已知直接调用已先转到MP Store，再删旧具体类 | Step 4 | REQ-007,REQ-009 | TEST-002/011 |

| MODIFY | `egon-cola-components/egon-cola-component-transactional-outbox-starter/src/test/java/top/egon/cola/component/outbox/dispatch/OutboxDispatcherTest.java` | egon-cola-components/egon-cola-component-transactional-outbox-starter/src/test/java/top/egon/cola/component/outbox/dispatch/OutboxDispatcherTest.java已有三态配送测试 | result-to-signal-to-Store route table tests；migration mode no-claim assertions | MP Store已稳定，先让旧switch遇到耗尽和自转换RED | Step 5、Step 7 | REQ-001,REQ-004,REQ-010 | TEST-012 |

| MODIFY | `egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/dispatch/OutboxDispatcher.java` | egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/dispatch/OutboxDispatcher.java#applyResult/#applyRetryableFailure旧switch | applyResult/applyRetryableFailure use FSM target；qualified `OutboxMpStorageProperties` drives submitDue/submitMessageIds maintenance guard | RED指出旧switch仍硬编码失败耗尽 | Step 5、Step 7 | REQ-001,REQ-004,REQ-010 | TEST-012 |
| MODIFY | `egon-cola-components/egon-cola-component-transactional-outbox-starter/src/test/java/top/egon/cola/component/outbox/integration/BusinessStateMachineIntegrationTest.java` | Step6 end-to-end dispatcher fixture construction | pass default/qualified `OutboxMpStorageProperties` to the changed Dispatcher constructor without enabling migration mode | Step7 changes the Dispatcher constructor; the Step6 reliability test must still exercise normal delivery with migration mode false | Step 6、Step 7, Step 9| REQ-002,REQ-005,REQ-010 | TEST-005/012；PG仅编译、不执行 |

| CREATE | `egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/statemachine/BusinessStateMachineEvent.java` | egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/api/OutboxMessage.java已有传输envelope | eventId,tenantId,machineKey,definitionVersion,businessId,eventType,expectedVersion,occurredAt,payload | 协议载体是测试与SPI的必要编译前置 | Step 6 | REQ-002,REQ-003,REQ-006 | TEST-004/005/006/007 |

| CREATE | `egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/statemachine/BusinessStateMachineSnapshotBO.java` | egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter/src/main/java/top/egon/cola/component/common/mybatis/model/EgonModel.java业务应用PO的version来源 | currentState,version,definitionVersion,appliedFingerprint,facts | 消费SPI需一个受锁的权威快照 | Step 6 | REQ-002,REQ-005,REQ-006 | TEST-004/005/006/007 |

| CREATE | `egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/statemachine/BusinessStateMachineRepository.java` | egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/delivery/DeliveryHandler.java扩展点与Spec INTERNAL-003/004 | lockAndLoad; saveTransition | 快照类型已可编译，固定业务方必须实现的原子边界 | Step 6 | REQ-002,REQ-005,REQ-006 | TEST-004/005/006/007 |

| CREATE | `egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/statemachine/BusinessStateMachineContextExecutor.java` | egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/dispatch/OutboxDispatcher.java复用worker线程 | execute(Event,Supplier<T>):T | worker线程身份独立于Web请求 | Step 6 | REQ-002,REQ-006 | TEST-004/005/006/007 |

| CREATE | `egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/statemachine/BusinessStateMachineDefinitionStrategy.java` | egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/delivery/DeliveryHandlerRegistry.java既有channel策略注册 | destination,factory,repository,validateEvent | 明示变化轴后才用策略接口 | Step 6 | REQ-002,REQ-006,REQ-007 | TEST-004/005/006/007 |

| CREATE | `egon-cola-components/egon-cola-component-transactional-outbox-starter/src/test/java/top/egon/cola/component/outbox/statemachine/BusinessStateMachineServiceTest.java` | egon-cola-components/egon-cola-component-transactional-outbox-starter/src/test/java/top/egon/cola/component/outbox/dispatch/OutboxDispatcherTest.java现有DeliveryResult三态fixture | receipt replay/version/context/test-only real SSM | Event/Snapshot/SPI可编译后写RED，Service尚不存在 | Step 6 | REQ-002,REQ-005,REQ-006 | TEST-004/005/006/007 |

| CREATE | `egon-cola-components/egon-cola-component-transactional-outbox-starter/src/test/java/top/egon/cola/component/outbox/delivery/statemachine/BusinessStateMachineDeliveryHandlerTest.java` | egon-cola-components/egon-cola-component-transactional-outbox-starter/src/test/java/top/egon/cola/component/outbox/delivery/DeliveryHandlerRegistryTest.java当前SPI测试 | channel/destination/event envelope validation | Handler尚不存在，RED说明既有HandlerRegistry无statemachine通道 | Step 6 | REQ-002,REQ-006,REQ-007 | TEST-004/005/006/007 |

| CREATE | `egon-cola-components/egon-cola-component-transactional-outbox-starter/src/test/java/top/egon/cola/component/outbox/autoconfigure/OutboxBusinessStateMachineAutoConfigurationTest.java` | egon-cola-components/egon-cola-component-transactional-outbox-starter/src/test/java/top/egon/cola/component/outbox/autoconfigure/TransactionalOutboxAutoConfigurationTest.java ContextRunner | off by default; fail open without SPI | Bean图尚无业务自动配置，RED证明旧系统不支持可选通道 | Step 6 | REQ-002,REQ-007 | TEST-004/005/006/007 |

| CREATE | `egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/statemachine/BusinessStateMachineService.java` | egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/transaction/DefaultTransactionalOutbox.java同Spring事务处理惯例 | process(Event,DeliveryContext):DeliveryResult | RED合同已固定，最小实现消费事务/幂等/SSM | Step 6 | REQ-002,REQ-003,REQ-004,REQ-005,REQ-006 | TEST-004/005/006/007 |

| CREATE | `egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/delivery/statemachine/BusinessStateMachineDeliveryHandler.java` | egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/delivery/DeliveryHandler.java及Registry现有规则 | implements DeliveryHandler | Service处理结果稳定后适配已有投递SPI | Step 6 | REQ-002,REQ-003,REQ-006,REQ-007 | TEST-004/005/006/007 |

| CREATE | `egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/autoconfigure/OutboxBusinessStateMachineAutoConfiguration.java` | egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/autoconfigure/TransactionalOutboxAutoConfiguration.java既有 @Bean 模式 | business Bean registry/tx/handler | 定义和Handler均存在后才装配可选通道 | Step 6 | REQ-002,REQ-006,REQ-007 | TEST-004/005/006/007 |

| CREATE/MODIFY | `egon-cola-components/egon-cola-component-transactional-outbox-starter/src/test/java/top/egon/cola/component/outbox/integration/BusinessStateMachineIntegrationTest.java` | egon-cola-components/egon-cola-component-transactional-outbox-starter/src/test/java/top/egon/cola/component/outbox/integration/TransactionalOutboxTransactionIntegrationTest.java测试本地PG事务 | same-TX state+receipt and replay window; pass OutboxMpStorageProperties to normal-mode Dispatcher fixture in Step7 | unit结果已GREEN后补跨事务实证（执行PG需另授权） | Step 6、Step 7 | REQ-002,REQ-004,REQ-005,REQ-006,REQ-010 | TEST-004/005/006/007/012 |

| CREATE | `egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/migration/OutboxMigrationResult.java` | egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/statemachine/BusinessStateMachineSnapshotBO.java相同普通载体注解风格 | source/target/counts/firstDifferenceId/verified | 迁移Service/Test需要结果载体编译前置 | Step 7 | REQ-010 | TEST-012 |

| CREATE | `egon-cola-components/egon-cola-component-transactional-outbox-starter/src/test/java/top/egon/cola/component/outbox/integration/OutboxMpMigrationIntegrationTest.java` | egon-cola-components/egon-cola-component-transactional-outbox-starter/src/test/java/top/egon/cola/component/outbox/integration/PostgresqlOutboxTestSupport.java两schema测试基础 | copy/verify/resume/maintenance RED | 结果类型可编译，行为Service尚缺；测试定义无损验收 | Step 7 | REQ-009,REQ-010 | TEST-012 |
| CREATE | `egon-cola-components/egon-cola-component-transactional-outbox-starter/src/test/java/top/egon/cola/component/outbox/migration/OutboxLegacyMigrationServiceTest.java` | 同模块ValidationUtils/ContextRunner单测风格 | unsafe schema, migration-mode, batch-bound preflight assertions | compile contract已建立后，无PG测试因fail-fast检查缺失先RED | Step 7 | REQ-009,REQ-010 | TEST-012 |

| CREATE | `egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/migration/OutboxLegacyMigrationService.java` | egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/migration/OutboxManagedDdlInitializer.java Step2物理PRIMARY引用 | migrate(sourceSchema,batchSize,verifyOnly):Result | RED预期缺少真实copy/核对入口；所有依赖Step2/4就绪 | Step 7, Step 9| REQ-009,REQ-010 | TEST-012 |

| MODIFY | `egon-cola-components/egon-cola-component-transactional-outbox-starter/README.zh-CN.md` | egon-cola-components/egon-cola-component-transactional-outbox-starter/README.zh-CN.md旧JDBC/事务/测试说明 | 中文接入/迁移/运维合同 | 最终运行和维护模式路径已实现，写准确消费方指引 | Step 8 | REQ-003,REQ-007,REQ-008,REQ-009,REQ-010 | TEST-008/009/011/012/013 |

| MODIFY | `egon-cola-components/egon-cola-component-transactional-outbox-starter/README.md` | egon-cola-components/egon-cola-component-transactional-outbox-starter/README.md原英文投递/配置文档 | English mirror of the same contract | 中文说明先定稿，再对照复制同一技术字段/键 | Step 8 | REQ-003,REQ-007,REQ-008,REQ-009,REQ-010 | TEST-008/009/011/012/013 |

## 6. Prerequisites, Constraints, and Plan Clarifications

### 6.1 Repository and worktree baseline

仓库根`/Users/mario/SelfProject/Egon-COLA`、branch main；原Plan基线`20fd847`；Step9执行基线`521b8d8639d37026ebf06c11847c2bc3ed8a502a`。Step9接管§5声明的19个Outbox文件及Spec/Plan；其它agent的dirty/untracked路径从不暂存或覆盖。原SQL1/应用DDL历史只读。用户已授权仅使用隔离PostgreSQL Testcontainers。

### 6.2 Build, test, and environment prerequisites

| Concern | Exact command/source | Required state | Validation boundary |
| --- | --- | --- | --- |
| 构建 | 根pom.xml Boot3.5.16、组件pom Java21；`mvn -pl egon-cola-components/egon-cola-component-transactional-outbox-starter -am -DskipTests compile` | JDK21/Maven可用；不启动应用 | 将来编译验证，本文未执行 |
| 组件单测 | O/pom.xml surefire排除integration目录；`-Dtest=ClassA,ClassB -Dsurefire.failIfNoSpecifiedTests=false test` | 测试类真实执行且不是全skip | 将来单测 |
| PG集成 | O/pom.xml failsafe integration-test/verify，T/integration/PostgresqlOutboxTestSupport.java检查EGON_OUTBOX_TEST_POSTGRES_ENABLED | 用户另行允许Testcontainers/PG时才设置true；未授权不启动Docker/数据库 | 运行时门禁不由本文声称通过 |
| Spec/Plan静态 | skill validator `python3 .agents/skills/egon-coding-writing-plan/scripts/validate_plan.py PLANPATH --strict` | 路径/关系/REQ/Step/MC完整 | 本轮实际执行 |
| 生成器 | scripts/egon-codegen.sh只支持native Light/Web/Service；目标为用户批准保留的基础组件Starter | 不运行不支持的component模板，不写codegen日志 | 静态范围结论 |

### 6.3 Immutable constraints and approved decisions

SQL1与既有Flyway历史不可编辑；唯一SQL2/manifest hash保持原值。技术表tenant0与business Event.tenantId是不同所有权；单PRIMARY/NATIVE SINGLE/LOCAL；默认Store全MP；TransactionalOutbox/OutboxStore API保持；用户于2026-09-27批准DEC-009内部锁修订，无新依赖/schema/API。隔离PG Testcontainers已授权；生产/宿主DB不访问。

### 6.4 Plan Clarifications

| ID | Small implementation inference | Repository evidence | Why semantics are unchanged | Impact if wrong |
| --- | --- | --- | --- | --- |
| PLAN-CLAR-001 | 把旧`PostgresqlJdbcOutboxStoreIntegrationTest`用已有测试内容迁到Spec已列的`OutboxMybatisPlusIntegrationTest`，旧路径删除 | 现有测试用JDBC具体类，Spec §8明确新增MP集成测试并要求所有旧直接构造用例更新 | 同一Store SPI断言换真实MP实现，测试责任不变；只改变内部测试文件命名 | 若有外部引用旧测试类，需保留类壳而不能删除 |
| PLAN-CLAR-003 | Step1先建立三个最小编译合同、再跑真实SSM行为RED、同Step完善同一Factory/Runner/Service文件；不单独提交stub | 用户明确同意只调整Step1顺序与RED说明；Java testCompile需要目标签名存在 | 不改变REQ/最终文件集/后续Steps/commit paths，测试在没有完整状态边/runner时必须运行并因行为失败 | 若当前接口无法将scaffold与行为分开，停止并回到Plan审核 |
| PLAN-CLAR-004 | Step1 RED阶段的SSM compile-scaffold Factory保留单条已批准`__NEW__ --ENQUEUE--> PENDING`边，其余状态边由GREEN文件实现 | 实际4.0.2 `StateMachineBuilder.build()`验证拒绝零transition图：`MalformedConfigurationException: Must have at least one transition`；Spec§7.3.1已要求该入队边 | 仅使当前红测模型可构造；无新状态/信号/最终行为；Step1最终Factory完全匹配Spec§7.3.1 | 其它空图/测试编译路径再次被拒绝则Step保持In Progress继续查API，不降级为编译错误RED |
| PLAN-CLAR-002 | 多次写`OutboxMybatisPlusAutoConfiguration.java`、`MybatisPlusOutboxStore.java`和`AutoConfiguration.imports`按符号拆Step | Spec §8三处分别承载DDL、Store、维护门禁/业务适配 | 每步可编译且语义分明，无新增Bean或公开API | 若实际自动配置无法按此独立编译，合并相关Step并只commit完整语义 |
| PLAN-CLAR-005 | Step2先完成manifest family RED/GREEN，再写SQL/manifest/属性与DDL initializer、logical factory、AutoConfiguration的可编译合同；随后新增并运行`OutboxManagedDdlIntegrationTest`中的无PG委托顺序RED，最后在同一文件内完成实现 | 用户批准“编译合同先行”；按原File 3顺序集成测试引用尚不存在的Step2类型会导致`testCompile`失败，而非行为RED | 不改变Step2文件集、DDL/Bean/SQL合同、REQ、commit paths；PG/Testcontainers断言仍单独受授权门禁，不因无PG运行而记PASS | 若合同无法用已有组件API声明，Step保持In Progress并回到Plan审核 |
| PLAN-CLAR-006 | Step2 `OutboxManagedDdlInitializer.initialize(Map<String,DataSource>,byte[])` 采用 `void` 返回，与主Spec §8.4一致；runner结果只用于内部确认整个manifest已应用或核对通过，并记录只读就绪证据 | 用户在执行中明确选择“按 Spec 使用 void”；Spec §8.4成功响应是void，Plan原File 6列出未使用的 `List<EgonColaDdlResult>` | 不改变Step文件集合、SQL/DDL/Bean范围、REQ或commit paths；不向组件调用方暴露runner内部结果列表 | 若后续Store/metadata验证需要就绪态，复用同一initializer内部的只读ready状态，不新增公共返回合同 |
| PLAN-CLAR-007 | Step2 Failsafe验证命令同时调用 `failsafe:integration-test` 与 `failsafe:verify`，确保测试报告中的失败会转成非零构建结果 | 执行时观察到仅调用 `failsafe:integration-test` 会记录用例失败但命令退出为0；追加官方插件的verify目标后才成为有效阻断门 | 不改变测试集合、PG门控、Step顺序或commit paths；PG仍以未执行/skip保持Runtime unverified | 若集成用例新增为非PG行为，须由本命令实际执行并PASS，不能只看聚合构建状态 |
| PLAN-CLAR-008 | Step2最终聚焦验证使用Maven `verify`生命周期，先在reactor打包修改后的Common MP，再执行Outbox的Failsafe目标 | 直接调用Failsafe插件时O测试classpath解析到本地仓库旧Common MP JAR，运行期family校验拒绝component-outbox；当前源码family测试已在reactor单独通过 | 不改测试集合、PG门控、Step文件/顺序或commit paths；final verify命令同一reactor先打包Common，且Failsafe verify负责阻断失败 | 若reactor仍解析到旧artifact，先核对依赖坐标与Maven日志，不通过install/改源码绕过 |
| PLAN-CLAR-009 | File9增加无数据库正向测试：mock公共runner验证实际TopologyValidator解析出的唯一PRIMARY/schema/role/manifest/fingerprint与readiness；ApplicationContextRunner验证具名属性绑定及非法timeout/manifest拒绝 | 最终9个FailSafe用例中3个无数据库路径实际通过、6个PG用例保持单项环境门控；runner mock不打开连接 | 只增加同一已声明测试文件内对Step2合同的覆盖，不增加生产文件、数据库权限或commit path | 若mock无法证明公共runner真实PostgreSQL行为，该部分仍记录Runtime unverified并等待经授权的PG验收 |
| PLAN-CLAR-010 | 为Step3 OutboxSchemaMetadataValidator读取既有物理PRIMARY元数据，Step2 initializer在route fingerprint匹配的DDL readiness后返回其已保留的同一PRIMARY DataSource；调用方只做只读DatabaseMetaData/pg_catalog检查 | Step3 Spec要求既有物理连接；Common bootstrapper只把physical map传入LogicalDataSourceFactory且不向后续Bean暴露；initializer ReadinessBO已持有原DataSource | 同一已声明Step2文件增加内部readiness accessor和负向测试，不创建/关闭第二连接池，不改变DDL hook、schema、REQ或提交路径；Step2需单独corrective commit并重跑本步验证后，才完成Step3 File10 | 若元数据调用要求获得Connection写权或无法保持只读，将回到Plan审核，不绕过到逻辑DataSource或另建池 |
| PLAN-CLAR-011 | Step4为Common MP新增 `@EgonColaExplicitTenantScopeMapper`；ContractValidator只对该标记的Mapper允许其模型表出现在ignored-tables，并尊重具名default方法作为代码实现、不强制重复XML statement；OutboxMessageDAO使用标记且以default方法拒绝技术表软删除 | Common `EgonColaMybatisPlusContractValidator` 当前对任一被忽略的BaseMapper模型失败，且无条件要求deleteVersionedById XML；Spec §§11.2/15.3要求outbox技术表精确忽略全局TenantLine、具名SQL显式限定tenant，而Outbox DAO明确禁止软删除 | 增加可审计的窄范围扩展点与正反测试；抽象Mapper方法仍必须有XML；不关闭全局tenant/local-write/optimistic/model-validation守卫，也不放宽未标记业务Mapper | 若普通忽略模型不再失败、抽象方法可绕过XML或标记Mapper缺租户条件，默认MP Store不能启用 |
| PLAN-CLAR-012 | Step4治理测试更新Step1/3新增的生命周期枚举、状态机异常和OutboxSchemaMetadataValidator清单；MetadataValidator实现Common `BaseValidator`合同并返回同一ValidationUtils | Step1已新增OutboxStateMachineException/OutboxLifecycleSignalEnum，Step3新增的OutboxSchemaMetadataValidator使用ValidationUtils但未继承BaseValidator；现有ComponentContractGovernanceTest扫描所有相应类型并约束基类 | 只同步现有治理白名单并补齐公共Validator扩展合同，不改异常外部协议或Schema metadata验证行为 | 若继续按旧清单会使Step4规定的治理测试无法通过并漏检新增类型 |
| PLAN-CLAR-013 | Step4构造器限定符测试发现Step3的OutboxMessageRepository与OutboxTechnicalContextExecutor重复拼入`top.`；以独立Step3 corrective commit修正为Common MP实际注册的具名Properties Bean | Common auto-configuration注册名为`egon.cola.component.mybatis-plus-top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties`；Step4新增的构造器测试对比该名并发现两个生产类不一致 | 只修正两个既有`@Qualifier`元数据，不改变公开API、Store行为或Step4文件集合；正确值由Step4测试长期回归覆盖 | 若不修，Lombok构造参数合同与Common MP Bean名不一致，违反具名注入合同 |
| PLAN-CLAR-014 | Step5先补Dispatcher的生命周期依赖编译合同和Bean wiring，再运行断言`OutboxLifecycleService.evaluate`调用的行为RED，最后在同一Step完成FSM目标分派；同时更新直接构造Dispatcher的PG recovery fixture与两个隔离ContextRunner | OutboxDispatcher当前有手写十参数构造且仍以switch决定结果；TransactionalOutboxAutoConfiguration与PostgresqlOutboxRecoveryIntegrationTest均直接构造旧签名；两个auto-config测试仅加载Transactional配置；主Spec §15.3要求final qualified lifecycleService、clock、properties与旧状态写语义 | 只将编译合同提前，不单独提交scaffold、不改变DeliveryHandler/OutboxStore签名或重试策略；行为RED必须在构造/Bean合同可编译后因缺少lifecycleService调用失败；Recovery测试只更新构造合同且继续PG门控 | 若仍按旧两文件顺序，测试编译失败；若遗漏任何直接构造或隔离上下文，Step5之外会留下坏调用点/假绿测试 |
| PLAN-CLAR-015 | 在现有OutboxStateMachineAutoConfiguration的BeanFactoryPostProcessor中为唯一既有TransactionalOutboxProperties注册真实Bean alias `outboxStateMachineOutboxProperties`，Transaction auto-config用该alias、`outboxStateMachineClock`和`outboxLifecycleService`构造Dispatcher；补齐Transactional与Optional auto-config ContextRunner依赖 | 主Spec §15.3明确要求状态机属性别名、现有MP Clock别名以及每个构造依赖的精确Qualifier；当前扫描发现Clock alias已存在、TransactionalProperties alias缺失；隔离ContextRunner不导入状态机auto-config | 注册的是同一个属性Bean的alias，不新增Properties/Clock实例、不重复绑定或扩大外部配置；ContextRunner继续不启动数据库或外部broker | 若不补Properties alias，Dispatcher qualifier无法解析；若添加同类型新Bean，会破坏唯一Bean语义并偏离Spec |
| PLAN-CLAR-016 | 单独纠正Step4 `MybatisPlusOutboxStore.markRetry` 使用显式`SCHEDULE_RETRY`，不在Store重复判定预算或将`markRetry`转为死信；在既有Store单测验证signal与RETRY_WAIT目标 | 主Spec §§7.3.1/9.2.1明确Dispatcher先用`DELIVERY_RETRYABLE`判断预算，旧`OutboxStore.markRetry`对应`SCHEDULE_RETRY`；当前Step4 Store仍在markRetry内再发DELIVERY_RETRYABLE并可写DEAD | 仅修正Store已提交实现和其已声明单测路径；Dispatcher仍决定是否调用markRetry或markDead，Store在REQUIRES_NEW/owner/version事务中验证显式markRetry边 | 若不修，Store会重复拥有预算策略，且主Spec规定的markRetry操作语义不成立 |
| PLAN-CLAR-017 | Step6先声明BusinessStateMachineService、BusinessStateMachineDeliveryHandler与OutboxBusinessStateMachineAutoConfiguration的可编译合同，再运行Service/Handler/AutoConfiguration行为RED；随后在相同文件内完成GREEN | 原文件顺序在Service/Handler/AutoConfiguration类型创建前编译测试，无法区分javac失败与业务行为RED；现有Java测试须引用这些具体类型与构造签名 | 只提前同Step声明的类型/方法/具名Bean合同，不另提交stub；RED只观察服务未处理/handler未路由/启用缺SPI未失败，再完成行为 | 若不提前合同，`testCompile`失败会伪装为行为RED且违背test-first验证目的 |
| PLAN-CLAR-018 | Step7将唯一`OutboxMpStorageProperties`通过具名构造依赖传入Store与Dispatcher，使同一`migration-mode`同时门禁enqueue与两种调度入口；同步更新生产Bean工厂及所有直接构造fixture | `OutboxMpStorageProperties`持有唯一typed `migrationMode`；Dispatcher当前只拿`TransactionalOutboxProperties`；`rg 'new OutboxDispatcher'`确认Bean工厂、Dispatcher单测、PG recovery fixture及Step6业务集成为全部构造点；`rg 'new MybatisPlusOutboxStore'`确认MP工厂与OutboxLongPrimaryKeyTest为全部构造点 | 只把已有配置对象传递到Store/Dispatcher门禁，不增公共Store SPI方法、属性副本、JDBC访问或依赖；正常fixture保持migration-mode=false，新增enqueue门禁断言 | 若不传同一properties，Store或Dispatcher会漏掉新写/queued-message门禁，不能满足Spec §9.2.9/§15.3 |
| PLAN-CLAR-019 | Step7先建立Migration Service与`executeMigration`可编译合同，再创建PG行为集成测试；Dispatcher字段/Bean/所有构造fixture先编译合同，接着运行无PG维护门禁RED，最后在已声明相同路径完成GREEN | Integration测试调用尚不存在的Service/SPI；增加Dispatcher final配置依赖后，Bean工厂、MP Store测试、Dispatcher测试和两个集成fixture均需要新构造参数；测试必须因未门禁而RED，不能被javac失败遮蔽 | 只重排Step7已声明13条路径、先建同文件可编译合同并把实现延至RED之后；REQ、对外配置、commit path与PG禁止运行不变 | 若类型/构造未先存在，Maven `testCompile`会失败而不构成行为RED；若仅编译stub即提交则违反完成条件 |
| PLAN-CLAR-020 | Step7增加纯单测覆盖Migration Service的unsafe schema、关闭maintenance及非法batch预检，证明这些失败发生在读取DataSource或访问Repository之前 | PG行为类受到`EGON_OUTBOX_TEST_POSTGRES_ENABLED`门禁；三个入口条件都可在无数据库下直接断言；移除schema/mode预检后的3项RED实测失败，恢复检查后GREEN | 仅新增同Starter的test-only路径，不增加SPI、依赖、schema或生产入口；PG复制/锁/提交仍保持未运行 | 若不覆盖预检失败，MC-VALID/TEST-012无法区分错误配置是否先访问了源库 |
| PLAN-CLAR-021 | 用户批准Claim/Reclaim/Cleanup采用bounded non-lock candidate scan；`pg_try_advisory_xact_lock(id)`从目标消息表按id/tenant0/active谓词查询，以满足Common SQL-shape guard与SS唯一SINGLE路由；state/version/due/retention CAS二次核对；候选窗口min(10000,requestedLimit*4) | PG 16.6 Testcontainers：旧SKIP LOCKED触发DialectSQLParsingException；无FROM锁函数触发SQL_SHAPE_UNSUPPORTED；表路由锁SQL与claim/cleanup 4项竞争测试通过 | 不升级依赖、不改DDL/OutboxStore公开签名；lock false/CAS0跳过候选，DB/SSM真实异常仍整事务rollback | 若路由、事务锁、CAS或扫描上限失败，Step9阻断，不能移除保护 |

## 7. Ordered File-by-file Implementation Steps



### Step 1 — 建立双层状态机共用执行器与技术生命周期图

- Requirements: REQ-001 REQ-002 REQ-003 REQ-004 REQ-007 REQ-008 REQ-009
- Dependencies: None
- Baseline state: O只有JDBC Store/分派switch及现有POM，无SSM/MP消息表；有效单测需先确认旧基线。
- Observable outcome: 建立双层状态机共用执行器与技术生命周期图；验收对应TEST-001/010。
- End state: 仅有内存SSM技术决策；生产消息仍旧Store，禁止声称MP迁移完成。
- Test-first gate: `Required` — first add only the approved dependency, compile contracts, and one Spec-approved ENQUEUE edge so SSM 4.0.2 can build a graph; then run real StateMachine RED tests for missing transition completion/retry guards, not javac failure. Complete the same Factory/Runner/Service files after RED; the final Step commit must contain no scaffold sentinel.
- Manual Checks: `MC-ARCH-001`, `MC-BEAN-001`, `MC-CONFIG-001`, `MC-CONVERT-001`, `MC-DEP-001`, `MC-JSON-001`, `MC-LOG-001`, `MC-MODEL-001`, `MC-NAME-001`, `MC-PATTERN-001`, `MC-REUSE-001`, `MC-SCOPE-001`, `MC-TEST-001`, `MC-TIME-001`, `MC-VALID-001`。在本Step完成前以本段文件和命令逐项核对，已知失败不得延后到后续清理。
- Literal Rules: Rule 1, Rule 2, Rule 3, Rule 4, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11。每个文件如下展开；Rule 11覆盖全部路径。
- Ordered files:

#### File 1 — `MODIFY egon-cola-components/egon-cola-components-bom/pom.xml`

- Purpose: 版本统一由组件BOM管理，先于Starter声明。
- Symbols: spring-statemachine-core dependencyManagement。
- Repository evidence: BOM既有outbox与Common MP管理项。
- Dependencies and consumers: 已有BOM outbox坐标与组件parent版本；错误版本会在dependency:tree暴露。
- Why now: Step 1的第1个文件；版本统一由组件BOM管理，先于Starter声明。
- Contract/signature changes: spring-statemachine-core dependencyManagement；Boot 3.5.16基线；版本固定4.0.2，不导出一个新的组件模块。
- Input/output and state mapping: Boot 3.5.16基线；版本固定4.0.2，不导出一个新的组件模块。
- Error and edge behavior: 已有BOM outbox坐标与组件parent版本；错误版本会在dependency:tree暴露；失败不吞异常、不伪造成功，适用断言由TEST-001/010验证。
- Standards impact: `MC-DEP-001`, `MC-REUSE-001`, `MC-SCOPE-001`, `MC-TEST-001`；本文件按当前组件约定实现并限定在本Step的路径内。
- Literal rule enforcement: Rule 11：保留获批的单Starter基础组件结构与MP/受管DDL/CQE边界，绝不改业务平台层次。
- Implementation pseudocode:

```xml
<properties>spring-statemachine.version = 4.0.2</properties>
<dependencyManagement>org.springframework.statemachine:spring-statemachine-core:${spring-statemachine.version}</dependencyManagement>
assert no second Spring Framework/Reactor BOM override and no unrelated version change
```

- Verification contribution: TEST-001/010；版本统一由组件BOM管理，先于Starter声明。
- After this file: 已建立 spring-statemachine-core dependencyManagement，后续文件接入/验证前不得声称整个Step完成。

#### File 2 — `MODIFY egon-cola-components/egon-cola-component-transactional-outbox-starter/pom.xml`

- Purpose: 测试导入SSM/MP类型前先满足编译前置。
- Symbols: spring-statemachine-core, Egon MP, MapStruct + processor。
- Repository evidence: O/pom.xml当前无SSM和MP依赖。
- Dependencies and consumers: 旧Starter pom已有Common core/id、JDBC/TX、JUnit/Testcontainers；effective-pom守住Lombok/Boot处理器。
- Why now: Step 1的第2个文件；测试导入SSM/MP类型前先满足编译前置。
- Contract/signature changes: spring-statemachine-core, Egon MP, MapStruct + processor；新增SSM core4.0.2及已批准Egon MP starter; MapStruct1.6.3；不新增Redis等手写依赖。
- Input/output and state mapping: 新增SSM core4.0.2及已批准Egon MP starter; MapStruct1.6.3；不新增Redis等手写依赖。
- Error and edge behavior: 旧Starter pom已有Common core/id、JDBC/TX、JUnit/Testcontainers；effective-pom守住Lombok/Boot处理器；失败不吞异常、不伪造成功，适用断言由TEST-001/010验证。
- Standards impact: `MC-CONVERT-001`, `MC-DEP-001`, `MC-SCOPE-001`, `MC-TEST-001`；本文件按当前组件约定实现并限定在本Step的路径内。
- Literal rule enforcement: Rule 3：普通class完整Lombok注解；EgonModel PO用@SuperBuilder/callSuper；MapStruct仅映射真实边界；Rule 11：保留获批的单Starter基础组件结构与MP/受管DDL/CQE边界，绝不改业务平台层次。
- Implementation pseudocode:

```xml
dependencies += spring-statemachine-core (BOM-managed), common-mybatis-plus-sharding-jdbc-ext-starter ${project.version}, mapstruct 1.6.3
compiler annotationProcessorPaths retain existing lombok and spring-boot-configuration-processor; append mapstruct-processor 1.6.3
verify dependency:tree contains one SSM/MP/MapStruct line and keeps existing optional HTTP/Rabbit contracts
```

- Verification contribution: TEST-001/010；测试导入SSM/MP类型前先满足编译前置。
- After this file: 已建立 spring-statemachine-core, Egon MP, MapStruct + processor，后续文件接入/验证前不得声称整个Step完成。

#### File 3 — `CREATE egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/common/exception/OutboxStateMachineException.java`

- Purpose: 模型拒绝与超时需区分，供runner/service调用。
- Symbols: reason and isRetryable()。
- Repository evidence: egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/common/exception/OutboxException.java当前错误基类。
- Dependencies and consumers: 原OutboxException继承CommonException；由runner/handler映射为稳定DeliveryResult。
- Why now: Step 1的第5个文件；模型拒绝与超时需区分，供runner/service调用。
- Contract/signature changes: reason and isRetryable()；extends OutboxException保留getCode/getStatus；isRetryable返回局部machineRetryable。
- Input/output and state mapping: extends OutboxException保留getCode/getStatus；isRetryable返回局部machineRetryable。
- Error and edge behavior: 原OutboxException继承CommonException；由runner/handler映射为稳定DeliveryResult；失败不吞异常、不伪造成功，适用断言由TEST-001/010验证。
- Standards impact: `MC-LOG-001`, `MC-NAME-001`, `MC-SCOPE-001`, `MC-TEST-001`；本文件按当前组件约定实现并限定在本Step的路径内。
- Literal rule enforcement: Rule 1：类型名按Event/BO/PO/DAO/Repository/Service/Factory等语义后缀，不新增Data/Info/Param/Bean载体；Rule 4：实际业务类用@Slf4j、显式Bean名、@RequiredArgsConstructor及每个final依赖@Qualifier；Rule 11：保留获批的单Starter基础组件结构与MP/受管DDL/CQE边界，绝不改业务平台层次。
- Implementation pseudocode:

```java
class OutboxStateMachineException extends OutboxException: fields reason:String, machineRetryable:boolean
construct with stable OUTBOX_FSM_REJECTED/TIMEOUT/EXECUTION_FAILED, preserve original cause
@Override isRetryable() returns machineRetryable; never include payload, token, or raw SQL in message
```

- Verification contribution: TEST-001/010；模型拒绝与超时需区分，供runner/service调用。
- After this file: 已建立 reason and isRetryable()，后续文件接入/验证前不得声称整个Step完成。

#### File 4 — `CREATE egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/statemachine/OutboxLifecycleSignalEnum.java`

- Purpose: 工厂需要穷举稳定信号代码。
- Symbols: seven EgonEnum technical signals。
- Repository evidence: egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/store/OutboxStatus.java已有EgonEnum结构。
- Dependencies and consumers: Factory与Dispatcher依赖；不是业务已发生Event，也不使用ordinal。
- Why now: Step 1的第6个文件；工厂需要穷举稳定信号代码。
- Contract/signature changes: seven EgonEnum technical signals；ENQUEUE/CLAIM/RECLAIM/DELIVERY_SUCCEEDED/DELIVERY_RETRYABLE/SCHEDULE_RETRY/DELIVERY_PERMANENT按0..6稳定编码。
- Input/output and state mapping: ENQUEUE/CLAIM/RECLAIM/DELIVERY_SUCCEEDED/DELIVERY_RETRYABLE/SCHEDULE_RETRY/DELIVERY_PERMANENT按0..6稳定编码。
- Error and edge behavior: Factory与Dispatcher依赖；不是业务已发生Event，也不使用ordinal；失败不吞异常、不伪造成功，适用断言由TEST-001/010验证。
- Standards impact: `MC-JSON-001`, `MC-NAME-001`, `MC-PATTERN-001`, `MC-SCOPE-001`, `MC-TEST-001`；本文件按当前组件约定实现并限定在本Step的路径内。
- Literal rule enforcement: Rule 1：类型名按Event/BO/PO/DAO/Repository/Service/Factory等语义后缀，不新增Data/Info/Param/Bean载体；Rule 6：Spring Boot Jackson和@EnumValue/@JsonFormat；永不以ordinal作业务编码；Rule 9：复杂迁移由SSM State/Strategy/Adapter实际承担，禁止Dispatcher另写状态switch；Rule 11：保留获批的单Starter基础组件结构与MP/受管DDL/CQE边界，绝不改业务平台层次。
- Implementation pseudocode:

```java
enum OutboxLifecycleSignalEnum implements EgonEnum: explicit code/message for seven Spec signals
validate signal != null; names and codes are not persisted in outbox.status
assert no ordinal() dispatch, unknown string parsing or event-type reflection
```

- Verification contribution: TEST-001/010；工厂需要穷举稳定信号代码。
- After this file: 已建立 seven EgonEnum technical signals，后续文件接入/验证前不得声称整个Step完成。

#### File 5 — `CREATE egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/autoconfigure/OutboxStateMachineProperties.java`

- Purpose: 生命周期Service需先有有界预算配置。
- Symbols: executionTimeout, claimEvaluationTimeout, business.enabled/timeout。
- Repository evidence: egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/autoconfigure/TransactionalOutboxProperties.java既有属性嵌套。
- Dependencies and consumers: OutboxStateMachineAutoConfiguration绑定；组件无application*.yml。
- Why now: Step 1的第9个文件；生命周期Service需先有有界预算配置。
- Contract/signature changes: executionTimeout, claimEvaluationTimeout, business.enabled/timeout；100ms/1s/false/5s；@Valid嵌套且执行预算小于claim预算/lease。
- Input/output and state mapping: 100ms/1s/false/5s；@Valid嵌套且执行预算小于claim预算/lease。
- Error and edge behavior: OutboxStateMachineAutoConfiguration绑定；组件无application*.yml；失败不吞异常、不伪造成功，适用断言由TEST-001/010验证。
- Standards impact: `MC-CONFIG-001`, `MC-MODEL-001`, `MC-SCOPE-001`, `MC-TEST-001`, `MC-TIME-001`, `MC-VALID-001`；本文件按当前组件约定实现并限定在本Step的路径内。
- Literal rule enforcement: Rule 1：类型名按Event/BO/PO/DAO/Repository/Service/Factory等语义后缀，不新增Data/Info/Param/Bean载体；Rule 2：按@Validated/@Valid、原生约束与Default/已有MP组逐层校验，非代理处调用现有ValidationUtils；Rule 3：普通class完整Lombok注解；EgonModel PO用@SuperBuilder/callSuper；MapStruct仅映射真实边界；Rule 7：配置键对全部profile保持一致，组件无生产application YAML，双语README逐项同步；Rule 10：使用Instant/Duration/Clock与PG微秒，禁止Date/Calendar/SimpleDateFormat；Rule 11：保留获批的单Starter基础组件结构与MP/受管DDL/CQE边界，绝不改业务平台层次。
- Implementation pseudocode:

```java
@Validated class OutboxStateMachineProperties; named @Bean("outboxStateMachineProperties") method carries @ConfigurationProperties(prefix="egon.cola.component.transactional-outbox.state-machine")
class OutboxStateMachineProperties: Duration executionTimeout=100ms, claimEvaluationTimeout=1s; BusinessProperties enabled=false, timeout=5s
reject <=0, business timeout>=existing delivery timeout, or claim budget>lease; do not add environment-only missing keys
```

- Verification contribution: TEST-001/010；生命周期Service需先有有界预算配置。
- After this file: 已建立 executionTimeout, claimEvaluationTimeout, business.enabled/timeout，后续文件接入/验证前不得声称整个Step完成。

#### File 6 — `CREATE egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/statemachine/StateMachineExecutionService.java`

- Purpose: 建立测试可引用的泛型执行入口；这是同Step的编译合同，非可提交实现。
- Symbols: execute(StateMachine<S,E>,S,Message<E>,Duration):S
- Repository evidence: egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/transaction/OutboxTransactionGuard.java展示同步事务边界。
- Dependencies and consumers: OutboxLifecycleService和未来BusinessService消费；Boot ValidationUtils显式路径。
- Why now: 创建Spec与RED测试共同依赖的typed签名，使RED可以因行为而运行。
- Contract/signature changes: 仅建立具名自动装配的构造参数及execute泛型签名；File 12实现真实Reactive生命周期。
- Input/output and state mapping: 空图source与trigger输入；编译合同不产生目标state或存储副作用。
- Error and edge behavior: 以稳定OUTBOX_FSM_NOT_IMPLEMENTED异常标识暂存RED行为；此Sentinel不得进入Step完成提交。
- Standards impact: `MC-BEAN-001`, `MC-LOG-001`, `MC-PATTERN-001`, `MC-SCOPE-001`, `MC-TEST-001`, `MC-TIME-001`, `MC-VALID-001`；本文件按当前组件约定实现并限定在本Step的路径内。
- Literal rule enforcement: Rule 1：类型名按Event/BO/PO/DAO/Repository/Service/Factory等语义后缀，不新增Data/Info/Param/Bean载体；Rule 2：按@Validated/@Valid、原生约束与Default/已有MP组逐层校验，非代理处调用现有ValidationUtils；Rule 4：实际业务类用@Slf4j、显式Bean名、@RequiredArgsConstructor及每个final依赖@Qualifier；Rule 9：复杂迁移由SSM State/Strategy/Adapter实际承担，禁止Dispatcher另写状态switch；Rule 10：使用Instant/Duration/Clock与PG微秒，禁止Date/Calendar/SimpleDateFormat；Rule 11：保留获批的单Starter基础组件结构与MP/受管DDL/CQE边界，绝不改业务平台层次。
- Implementation pseudocode:

```java
declare typed execute(StateMachine<S,E>,S,Message<E>,Duration) and qualified ValidationUtils collaborator
compile-contract method throws OutboxStateMachineException("OUTBOX_FSM_NOT_IMPLEMENTED",false,...); this temporary body exists only until File 12
no database operation or swallowed error; this path will not be committed while sentinel remains
```

- Verification contribution: TEST-001/010；异常和状态信号已稳定，写共用有界完成语义。
- After this file: Runner及接口可编译，尚未处理转移/complete/超时。

#### File 7 — `CREATE egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/statemachine/OutboxLifecycleStateMachineFactory.java`

- Purpose: 建立真实可build的最小技术图，使行为测试能执行。
- Symbols: create():StateMachine<String,OutboxLifecycleSignalEnum>
- Repository evidence: egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/dispatch/OutboxDispatcher.java现有switch与死信分支。
- Dependencies and consumers: 由LifecycleService调用；无DB/网络action，图实例每次新建。
- Why now: SSM 4.0.2拒绝零transition图；只引入Spec已批准的ENQUEUE边供RED运行。
- Contract/signature changes: create返回fresh/autoStartup=false机器；File 11为同一模型补全其余§7.3.1迁移边。
- Input/output and state mapping: __NEW__+ENQUEUE→PENDING；所有其它状态/信号仍无边。
- Error and edge behavior: 不新建状态/持久化字段；除ENQUEUE外测试信号会被拒绝或未完成。
- Standards impact: `MC-BEAN-001`, `MC-LOG-001`, `MC-NAME-001`, `MC-PATTERN-001`, `MC-SCOPE-001`, `MC-TEST-001`；本文件按当前组件约定实现并限定在本Step的路径内。
- Literal rule enforcement: Rule 1：类型名按Event/BO/PO/DAO/Repository/Service/Factory等语义后缀，不新增Data/Info/Param/Bean载体；Rule 4：实际业务类用@Slf4j、显式Bean名、@RequiredArgsConstructor及每个final依赖@Qualifier；Rule 9：复杂迁移由SSM State/Strategy/Adapter实际承担，禁止Dispatcher另写状态switch；Rule 11：保留获批的单Starter基础组件结构与MP/受管DDL/CQE边界，绝不改业务平台层次。
- Implementation pseudocode:

```java
build fresh flat machine with __NEW__, five persisted states, autoStartup=false
register only the approved __NEW__ --ENQUEUE--> PENDING edge so StateMachineBuilder 4.0.2 can build; all other lifecycle transitions remain absent
File 11 adds the rest of the approved graph; no extra signal/state/action is committed
```

- Verification contribution: TEST-001/010；runner提供完成后，定义唯一固定技术图。
- After this file: 机图可构造且仅有一条Spec边，生命周期的CLAIM/RETRY测试仍RED。

#### File 8 — `CREATE egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/statemachine/OutboxLifecycleService.java`

- Purpose: 建立技术状态evaluate入口与校验/SSM映射，执行RED后贯通GREEN。
- Symbols: evaluate(...) complete
- Repository evidence: egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/store/OutboxStatus.java五态与守卫所需maxAttempts。
- Dependencies and consumers: Factory/runner/properties/ValidationUtils显式Qualifier；技术类型不解释业务Event。
- Why now: Factory的最小合法图与Runner编译合同已存在，测试可触达真实技术入口。
- Contract/signature changes: 签名/properties不变。
- Input/output and state mapping: source仅__NEW__/五态；attempt>=0/max>=1；target只映射五OutboxStatus。
- Error and edge behavior: 引擎错误/非法target稳定异常；超时保留可重试分类；不写Store/不回退switch。
- Standards impact: `MC-BEAN-001`, `MC-LOG-001`, `MC-PATTERN-001`, `MC-SCOPE-001`, `MC-TEST-001`, `MC-TIME-001`, `MC-VALID-001`；本文件按当前组件约定实现并限定在本Step的路径内。
- Literal rule enforcement: Rule 1：类型名按Event/BO/PO/DAO/Repository/Service/Factory等语义后缀，不新增Data/Info/Param/Bean载体；Rule 2：按@Validated/@Valid、原生约束与Default/已有MP组逐层校验，非代理处调用现有ValidationUtils；Rule 4：实际业务类用@Slf4j、显式Bean名、@RequiredArgsConstructor及每个final依赖@Qualifier；Rule 9：复杂迁移由SSM State/Strategy/Adapter实际承担，禁止Dispatcher另写状态switch；Rule 10：使用Instant/Duration/Clock与PG微秒，禁止Date/Calendar/SimpleDateFormat；Rule 11：保留获批的单Starter基础组件结构与MP/受管DDL/CQE边界，绝不改业务平台层次。
- Implementation pseudocode:

```java
@Validated evaluate validates known source, ENQUEUE only pairs with __NEW__, attempt>=0/max>=1; builds Message with immutable budgets
invoke fresh factory and typed executor; parse only five stable OutboxStatus messages from final engine state
invalid inputs return OUTBOX_FSM_INPUT_INVALID; no Store write or fallback switch; with temporary graph/runner behavior, legal success cases remain RED
```

- Verification contribution: TEST-001/010；固定图和属性已可用，提供Store/Dispatcher共用决策。
- After this file: 输入边界可用，但ENQUEUE会遇到Runner NOT_IMPLEMENTED，CLAIM等缺边；指定测试应RED。

#### File 9 — `CREATE egon-cola-components/egon-cola-component-transactional-outbox-starter/src/test/java/top/egon/cola/component/outbox/statemachine/StateMachineExecutionServiceTest.java`

- Purpose: 建立真实SSM runner的GREEN前行为断言。
- Symbols: accepted transition, guarded rejection, timeout, STOP failure
- Repository evidence: egon-cola-components/egon-cola-component-transactional-outbox-starter/src/test/java/top/egon/cola/component/outbox/transaction/DefaultTransactionalOutboxTest.java JUnit5风格。
- Dependencies and consumers: 新runner与SSM API；采用现有JUnit5/AssertJ测试风格。
- Why now: File 6编译入口和File 7实际无边machine已存在；此处测试测试运行时语义。
- Contract/signature changes: Required runner public method无更改。
- Input/output and state mapping: 成功边必须恰好1个triggerEnded；accepted-but-denied/timeout不得返回target。
- Error and edge behavior: RED必须由空转换/NOT_IMPLEMENTED触发，不能是javac缺类、fixture失败或依赖失败。
- Standards impact: `MC-PATTERN-001`, `MC-SCOPE-001`, `MC-TEST-001`, `MC-TIME-001`, `MC-VALID-001`；本文件按当前组件约定实现并限定在本Step的路径内。
- Literal rule enforcement: Rule 2：按@Validated/@Valid、原生约束与Default/已有MP组逐层校验，非代理处调用现有ValidationUtils；Rule 9：复杂迁移由SSM State/Strategy/Adapter实际承担，禁止Dispatcher另写状态switch；Rule 10：使用Instant/Duration/Clock与PG微秒，禁止Date/Calendar/SimpleDateFormat；Rule 11：保留获批的单Starter基础组件结构与MP/受管DDL/CQE边界，绝不改业务平台层次。
- Implementation pseudocode:

```java
build real SSM single-region source→target machine and call runner.execute(source,event,Duration)
assert completed accepted transition returns target; accepted-but-Guard-denied with no matching transitionEnded throws; timeout and stop errors never return success
run after Files 6–8 compile contracts and before production behavior MODIFY
```

- Verification contribution: TEST-001/010；先用真实4.0.2图固定ACCEPTED但Guard拒绝的RED。
- After this file: 测试类编译并明确RED在转换完成行为；Fixture与SSM依赖正常。

#### File 10 — `CREATE egon-cola-components/egon-cola-component-transactional-outbox-starter/src/test/java/top/egon/cola/component/outbox/statemachine/OutboxLifecycleServiceTest.java`

- Purpose: 建立技术生命周期transition matrix RED。
- Symbols: evaluate against every source/signal, attempt=max−1/max/>max
- Repository evidence: egon-cola-components/egon-cola-component-transactional-outbox-starter/src/test/java/top/egon/cola/component/outbox/dispatch/OutboxDispatcherTest.java旧重试分支测试。
- Dependencies and consumers: OutboxLifecycleService/Factory/SignalEnum；独立内存测试无DB。
- Why now: File 6–8提供类型/可调用链；实际图故意还无迁移边。
- Contract/signature changes: evaluate签名不改；断言Spec §7.3.1各target。
- Input/output and state mapping: max retry→DEAD、低于max→RETRY_WAIT、高attempt成功仍SUCCEEDED、terminal edge拒绝。
- Error and edge behavior: RED必须体现缺edge/未实现decision，不把源码编译失败当behavior RED。
- Standards impact: `MC-PATTERN-001`, `MC-SCOPE-001`, `MC-TEST-001`, `MC-VALID-001`；本文件按当前组件约定实现并限定在本Step的路径内。
- Literal rule enforcement: Rule 2：按@Validated/@Valid、原生约束与Default/已有MP组逐层校验，非代理处调用现有ValidationUtils；Rule 9：复杂迁移由SSM State/Strategy/Adapter实际承担，禁止Dispatcher另写状态switch；Rule 11：保留获批的单Starter基础组件结构与MP/受管DDL/CQE边界，绝不改业务平台层次。
- Implementation pseudocode:

```java
call OutboxLifecycleService with __NEW__/ENQUEUE, PENDING/CLAIM, PROCESSING/RETRYABLE and max boundaries
assert every valid source+signal resolves the exact OutboxStatus; assert invalid terminal/unknown state throws stable nonretryable reason
use a fresh real instance per call, no DB/mock-only fake transition
```

- Verification contribution: TEST-001/010；runner测试后锁定固定图边矩阵RED。
- After this file: RED已运行并证明SSM图/decision缺失。

#### File 11 — `MODIFY egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/statemachine/OutboxLifecycleStateMachineFactory.java`

- Purpose: 将初始compile图扩为Spec全部技术迁移边。
- Symbols: create()完整转换矩阵/Guard
- Repository evidence: egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/dispatch/OutboxDispatcher.java现有switch与死信分支。
- Dependencies and consumers: 由LifecycleService调用；无DB/网络action，图实例每次新建。
- Why now: File 9/10已证明其余迁移边/Guard缺失；此处只添加§7.3.1批准边。
- Contract/signature changes: 同create()方法签名/状态枚举。
- Input/output and state mapping: __NEW__→PENDING；claim/reclaim→PROCESSING；成功、重试、耗尽、永久失败分别定向现有五态。
- Error and edge behavior: terminal states无边；无startup transition Action side effect/timer/change retry budget。
- Standards impact: `MC-BEAN-001`, `MC-LOG-001`, `MC-NAME-001`, `MC-PATTERN-001`, `MC-SCOPE-001`, `MC-TEST-001`；本文件按当前组件约定实现并限定在本Step的路径内。
- Literal rule enforcement: Rule 1：类型名按Event/BO/PO/DAO/Repository/Service/Factory等语义后缀，不新增Data/Info/Param/Bean载体；Rule 4：实际业务类用@Slf4j、显式Bean名、@RequiredArgsConstructor及每个final依赖@Qualifier；Rule 9：复杂迁移由SSM State/Strategy/Adapter实际承担，禁止Dispatcher另写状态switch；Rule 11：保留获批的单Starter基础组件结构与MP/受管DDL/CQE边界，绝不改业务平台层次。
- Implementation pseudocode:

```java
add PENDING/RETRY_WAIT CLAIM→PROCESSING and PROCESSING RECLAIM→PROCESSING external transitions
add DELIVERY_SUCCEEDED→SUCCEEDED, DELIVERY_RETRYABLE Guard attempt<max→RETRY_WAIT and attempt>=max→DEAD, SCHEDULE_RETRY→RETRY_WAIT, DELIVERY_PERMANENT→DEAD
fresh flat single-region machine, no timer/side-effect Action/internal transition; each test signal produces one matching transitionEnded
```

- Verification contribution: TEST-001/010；runner提供完成后，定义唯一固定技术图。
- After this file: OutboxLifecycleServiceTest各source/signal/max分支GREEN，终态无出边。

#### File 12 — `MODIFY egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/statemachine/StateMachineExecutionService.java`

- Purpose: 实现每实例reactive生命周期、有界等待和真实转换完成判定。
- Symbols: execute(...) reset/start/sendEvent/result.complete/listener/stop
- Repository evidence: egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/transaction/OutboxTransactionGuard.java展示同步事务边界。
- Dependencies and consumers: OutboxLifecycleService和未来BusinessService消费；Boot ValidationUtils显式路径。
- Why now: File 9/10已在compile scaffold上观察到行为RED，状态图GREEN后完成执行器。
- Contract/signature changes: 泛型签名不变；一个总Duration deadline。
- Input/output and state mapping: 只有ACCEPTED+一个matching transitionEnded+complete成功+无machine error+bounded stop返回target。
- Error and edge behavior: DENIED/DEFERRED/empty/multi-region/multiple edge/异常/timeout均typed error；finally移除listener，禁止fire-and-forget。
- Standards impact: `MC-BEAN-001`, `MC-LOG-001`, `MC-PATTERN-001`, `MC-SCOPE-001`, `MC-TEST-001`, `MC-TIME-001`, `MC-VALID-001`；本文件按当前组件约定实现并限定在本Step的路径内。
- Literal rule enforcement: Rule 1：类型名按Event/BO/PO/DAO/Repository/Service/Factory等语义后缀，不新增Data/Info/Param/Bean载体；Rule 2：按@Validated/@Valid、原生约束与Default/已有MP组逐层校验，非代理处调用现有ValidationUtils；Rule 4：实际业务类用@Slf4j、显式Bean名、@RequiredArgsConstructor及每个final依赖@Qualifier；Rule 9：复杂迁移由SSM State/Strategy/Adapter实际承担，禁止Dispatcher另写状态switch；Rule 10：使用Instant/Duration/Clock与PG微秒，禁止Date/Calendar/SimpleDateFormat；Rule 11：保留获批的单Starter基础组件结构与MP/受管DDL/CQE边界，绝不改业务平台层次。
- Implementation pseudocode:

```java
resetStateMachineReactively(source), await startReactively, attach listener, sendEvent and await every StateMachineEventResult.complete using remaining single deadline
require ACCEPTED, exactly one transitionEnded with matching trigger/source/target and !hasStateMachineError(); validate one final state
finally detach listener and bounded stopReactively; preserve execution failure plus cleanup diagnostic, never return success after timeout
```

- Verification contribution: TEST-001/010；异常和状态信号已稳定，写共用有界完成语义。
- After this file: StateMachineExecutionServiceTest正常/Guard拒绝/动作异常/有界超时断言GREEN。

#### File 13 — `MODIFY egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/statemachine/OutboxLifecycleService.java`

- Purpose: 实现技术状态输入校验、重试目标、枚举映射和失败分类。
- Symbols: evaluate(...) complete
- Repository evidence: egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/store/OutboxStatus.java五态与守卫所需maxAttempts。
- Dependencies and consumers: Factory/runner/properties/ValidationUtils显式Qualifier；技术类型不解释业务Event。
- Why now: Factory和Runner真实SSM API已GREEN。
- Contract/signature changes: 签名/properties不变。
- Input/output and state mapping: source仅__NEW__/五态；attempt>=0/max>=1；target只映射五OutboxStatus。
- Error and edge behavior: 引擎错误/非法target稳定异常；超时保留可重试分类；不写Store/不回退switch。
- Standards impact: `MC-BEAN-001`, `MC-LOG-001`, `MC-PATTERN-001`, `MC-SCOPE-001`, `MC-TEST-001`, `MC-TIME-001`, `MC-VALID-001`；本文件按当前组件约定实现并限定在本Step的路径内。
- Literal rule enforcement: Rule 1：类型名按Event/BO/PO/DAO/Repository/Service/Factory等语义后缀，不新增Data/Info/Param/Bean载体；Rule 2：按@Validated/@Valid、原生约束与Default/已有MP组逐层校验，非代理处调用现有ValidationUtils；Rule 4：实际业务类用@Slf4j、显式Bean名、@RequiredArgsConstructor及每个final依赖@Qualifier；Rule 9：复杂迁移由SSM State/Strategy/Adapter实际承担，禁止Dispatcher另写状态switch；Rule 10：使用Instant/Duration/Clock与PG微秒，禁止Date/Calendar/SimpleDateFormat；Rule 11：保留获批的单Starter基础组件结构与MP/受管DDL/CQE边界，绝不改业务平台层次。
- Implementation pseudocode:

```java
@Validated evaluate validates parameters through ValidationUtils and rejects invalid source/ENQUEUE pairing and nonpositive attempt/max
create immutable trigger Message with attemptCount/maxAttempts headers; execute a new factory graph under configured executionTimeout
parse only existing OutboxStatus string names; unknown/no final target raises stable OutboxStateMachineException; no persistence side effect
```

- Verification contribution: TEST-001/010；固定图和属性已可用，提供Store/Dispatcher共用决策。
- After this file: OutboxLifecycleServiceTest各技术迁移/异常和预算断言GREEN，Step1行为闭合。

#### File 14 — `CREATE egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/autoconfigure/OutboxStateMachineAutoConfiguration.java`

- Purpose: 单元状态图已稳定，配置可注册但尚不触发存储。
- Symbols: engine beans and aliases。
- Repository evidence: egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/autoconfigure/TransactionalOutboxAutoConfiguration.java现有具名Bean工厂模式。
- Dependencies and consumers: 现有AutoConfiguration.imports将在Step4连接；不创建第二ObjectMapper/Clock。
- Why now: Step 1的第11个文件；单元状态图已稳定，配置可注册但尚不触发存储。
- Contract/signature changes: engine beans and aliases；具名runner/factory/lifecycle与properties、Clock别名；只处理core不启用business。
- Input/output and state mapping: 具名runner/factory/lifecycle与properties、Clock别名；只处理core不启用business。
- Error and edge behavior: 现有AutoConfiguration.imports将在Step4连接；不创建第二ObjectMapper/Clock；失败不吞异常、不伪造成功，适用断言由TEST-001/010验证。
- Standards impact: `MC-BEAN-001`, `MC-CONFIG-001`, `MC-SCOPE-001`, `MC-TEST-001`；本文件按当前组件约定实现并限定在本Step的路径内。
- Literal rule enforcement: Rule 4：实际业务类用@Slf4j、显式Bean名、@RequiredArgsConstructor及每个final依赖@Qualifier；Rule 7：配置键对全部profile保持一致，组件无生产application YAML，双语README逐项同步；Rule 11：保留获批的单Starter基础组件结构与MP/受管DDL/CQE边界，绝不改业务平台层次。
- Implementation pseudocode:

```java
@AutoConfiguration(after=EgonColaMybatisPlusAutoConfiguration.class); bind OutboxStateMachineProperties by one named @Bean method only
register named outboxStateMachineExecutionService/factory/lifecycleService; existing egonColaMybatisPlusClock gets real alias outboxStateMachineClock
register ObjectMapper alias without adding second typed Bean; fail if ambiguous or conflicting alias; no business handler yet
```

- Verification contribution: TEST-001/010；单元状态图已稳定，配置可注册但尚不触发存储。
- After this file: 已建立 engine beans and aliases，后续文件接入/验证前不得声称整个Step完成。

- Validation working directory: `/Users/mario/SelfProject/Egon-COLA`。
- Verification command: `mvn -pl egon-cola-components/egon-cola-component-transactional-outbox-starter -am -DfailIfNoTests=false -Dsurefire.failIfNoSpecifiedTests=false -Dtest=OutboxLifecycleServiceTest,StateMachineExecutionServiceTest test`。
- Expected result: exit 0，指定TEST-001/010相关非集成单测实际执行且全部通过；若涉及integration目录，在未获得PG/Testcontainers授权时标为未验证，不能以skip替代真实GREEN。
- Failure returns to: 本Step的第1个RED文件，定位测试/夹具错误；编译/行为失败返回对应生产文件；DDL/依赖不兼容返回Step 1或Step 2并暂停后续。
- Completion criteria: 仅有内存SSM技术决策；生产消息仍旧Store，禁止声称MP迁移完成；本Step适用Manual Checks已按实际静态/单测证据复核；禁止把未授权的PG运行写为PASS。
- Rollback: 限定回退本Step提交路径；若已切到新MP表并产生消息，必须遵守主Spec §16停写/核对/前向修复，不能直接启用旧JDBC。
- Commit paths: `egon-cola-components/egon-cola-components-bom/pom.xml` `egon-cola-components/egon-cola-component-transactional-outbox-starter/pom.xml` `egon-cola-components/egon-cola-component-transactional-outbox-starter/src/test/java/top/egon/cola/component/outbox/statemachine/StateMachineExecutionServiceTest.java` `egon-cola-components/egon-cola-component-transactional-outbox-starter/src/test/java/top/egon/cola/component/outbox/statemachine/OutboxLifecycleServiceTest.java` `egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/common/exception/OutboxStateMachineException.java` `egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/statemachine/OutboxLifecycleSignalEnum.java` `egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/statemachine/StateMachineExecutionService.java` `egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/statemachine/OutboxLifecycleStateMachineFactory.java` `egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/autoconfigure/OutboxStateMachineProperties.java` `egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/statemachine/OutboxLifecycleService.java` `egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/autoconfigure/OutboxStateMachineAutoConfiguration.java`
- Commit: `feat(outbox): introduce bounded state machine lifecycle`。

### Step 2 — 创建独立受管schema及物理到逻辑数据源引导

- Requirements: REQ-007 REQ-009 REQ-010
- Dependencies: Step 1
- Baseline state: Step 1提交后，仅有内存SSM技术决策；生产消息仍旧Store，禁止声称MP迁移完成。
- Observable outcome: 创建独立受管schema及物理到逻辑数据源引导；验收对应TEST-013。
- End state: 新受管SQL/manifest与引导hook具备；未执行DDL/旧表迁移。
- Test-first gate: Required — File 1的family测试先因当前BO拒绝component-outbox而RED，File 2后GREEN；按PLAN-CLAR-005提供完整编译合同后，`OutboxManagedDdlIntegrationTest`中的无PG初始化失败/逻辑DS未创建断言应在hook调用行为缺失时RED。真实PG DDL测试保持环境门禁并未授权运行，不能把skip当作GREEN。
- Manual Checks: `MC-ARCH-001`, `MC-REUSE-001`, `MC-DEP-001`, `MC-BEAN-001`, `MC-CONVERT-001`, `MC-CONFIG-001`, `MC-JSON-001`, `MC-LOG-001`, `MC-MODEL-001`, `MC-NAME-001`, `MC-UTIL-001`, `MC-PATTERN-001`, `MC-SCOPE-001`, `MC-TEST-001`, `MC-TIME-001`, `MC-VALID-001`, `MC-BLOCKER-001`。在本Step完成前以本段文件和命令逐项核对，已知失败不得延后到后续清理。
- Literal Rules: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11。每个文件如下展开；Rule 11覆盖全部路径。
- PLAN-CLAR-005执行顺序：File 1→2→3→4→5→6→7→8（File 6–8先建可编译合同）→File 9运行无PG hook RED→在File 6–8原路径完善实现。File 3的PG专属场景继续门禁；SQL/manifest与所有文件、commit paths不变。
- Ordered files:

#### File 1 — `CREATE egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter/src/test/java/top/egon/cola/component/common/mybatis/ddl/EgonColaDdlManifestFamilyTest.java`

- Purpose: 先固定现有构造会拒绝新family的RED。
- Symbols: component-outbox family and old-six-family assertions。
- Repository evidence: egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter/src/test/java/top/egon/cola/component/common/mybatis/ddl/EgonColaPostgreDdlRunnerTest.java。
- Dependencies and consumers: Common MP模块现有EgonColaPostgreDdlRunnerTest作为风格证据。
- Why now: Step 2的第1个文件；先固定现有构造会拒绝新family的RED。
- Contract/signature changes: component-outbox family and old-six-family assertions；family=component-outbox被接受；原六类仍接受；其它值仍拒绝。
- Input/output and state mapping: family=component-outbox被接受；原六类仍接受；其它值仍拒绝。
- Error and edge behavior: Common MP模块现有EgonColaPostgreDdlRunnerTest作为风格证据；失败不吞异常、不伪造成功，适用断言由TEST-013验证。
- Standards impact: `MC-SCOPE-001`, `MC-TEST-001`, `MC-VALID-001`；本文件按当前组件约定实现并限定在本Step的路径内。
- Literal rule enforcement: Rule 2：按@Validated/@Valid、原生约束与Default/已有MP组逐层校验，非代理处调用现有ValidationUtils；Rule 11：保留获批的单Starter基础组件结构与MP/受管DDL/CQE边界，绝不改业务平台层次。
- Implementation pseudocode:

```java
new EgonColaDdlManifestBO("component-outbox",List.of(ScriptBO(validVersion,path,sha256))) succeeds
for existing family in light/light-open/service/service-open/web/web-open assert unchanged
assert family="arbitrary" still throws; version ordering and path/checksum constraints remain intact
```

- Verification contribution: TEST-013；先固定现有构造会拒绝新family的RED。
- After this file: 本测试定义该Step的RED断言，生产行为仍缺失；其余文件填入后GREEN。

#### File 2 — `MODIFY egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter/src/main/java/top/egon/cola/component/common/mybatis/ddl/EgonColaDdlManifestBO.java`

- Purpose: TEST-013 family RED之后只扩准确一个值。
- Symbols: allowed family set component-outbox。
- Repository evidence: egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter/src/main/java/top/egon/cola/component/common/mybatis/ddl/EgonColaDdlManifestBO.java现有Set.of六项。
- Dependencies and consumers: EgonColaPostgreDdlRunner使用manifest；不改其它DDL工作流。
- Why now: Step 2的第2个文件；TEST-013 family RED之后只扩准确一个值。
- Contract/signature changes: allowed family set component-outbox；增允许集合component-outbox，保持ScriptBO版本/路径/hash校验字节不变。
- Input/output and state mapping: 增允许集合component-outbox，保持ScriptBO版本/路径/hash校验字节不变。
- Error and edge behavior: EgonColaPostgreDdlRunner使用manifest；不改其它DDL工作流；失败不吞异常、不伪造成功，适用断言由TEST-013验证。
- Standards impact: `MC-SCOPE-001`, `MC-TEST-001`, `MC-VALID-001`；本文件按当前组件约定实现并限定在本Step的路径内。
- Literal rule enforcement: Rule 1：类型名按Event/BO/PO/DAO/Repository/Service/Factory等语义后缀，不新增Data/Info/Param/Bean载体；Rule 2：按@Validated/@Valid、原生约束与Default/已有MP组逐层校验，非代理处调用现有ValidationUtils；Rule 11：保留获批的单Starter基础组件结构与MP/受管DDL/CQE边界，绝不改业务平台层次。
- Implementation pseudocode:

```java
allowedFamilies = existing six family strings + "component-outbox"
keep compact constructor validation of ordered version, unique path and lowercase SHA-256
no relaxed wildcard family, no history adoption, no method signature change
```

- Verification contribution: TEST-013；TEST-013 family RED之后只扩准确一个值。
- After this file: 已建立 allowed family set component-outbox，后续文件接入/验证前不得声称整个Step完成。

#### File 3 — `CREATE egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/resources/db/egon-outbox-mp/V20260924_001__initialize_outbox_mp_schema.sql`

- Purpose: runner family已可解析，先固定表/索引合同。
- Symbols: DDL-001 one SQL script。
- Repository evidence: egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/resources/db/transactional-outbox/postgresql/V1__create_transactional_outbox_schema.sql原22字段定义。
- Dependencies and consumers: 新独立空schemaegon_outbox，source SQL1保持字节不动；manifest下一个文件。
- Why now: Step 2的第3个文件；runner family已可解析，先固定表/索引合同。
- Contract/signature changes: DDL-001 one SQL script；新消息表原22列+EgonModel 7新增列；ddl_history 8列；PK/五态check/租户0/softdelete NULL/version；五索引。
- Input/output and state mapping: 新消息表原22列+EgonModel 7新增列；ddl_history 8列；PK/五态check/租户0/softdelete NULL/version；五索引。
- Error and edge behavior: 新独立空schemaegon_outbox，source SQL1保持字节不动；manifest下一个文件；失败不吞异常、不伪造成功，适用断言由TEST-013验证。
- Standards impact: `MC-JSON-001`, `MC-MODEL-001`, `MC-SCOPE-001`, `MC-TEST-001`, `MC-TIME-001`；本文件按当前组件约定实现并限定在本Step的路径内。
- Literal rule enforcement: Rule 3：普通class完整Lombok注解；EgonModel PO用@SuperBuilder/callSuper；MapStruct仅映射真实边界；Rule 6：Spring Boot Jackson和@EnumValue/@JsonFormat；永不以ordinal作业务编码；Rule 10：使用Instant/Duration/Clock与PG微秒，禁止Date/Calendar/SimpleDateFormat；Rule 11：保留获批的单Starter基础组件结构与MP/受管DDL/CQE边界，绝不改业务平台层次。
- Implementation pseudocode:

```sql
CREATE TABLE egon_cola_outbox_message(id bigint PK, original 21 remaining columns, tenant_id bigint NOT NULL CHECK=0, create_user_id/create_time/update_user_id/update_time, deleted_at NULL CHECK IS NULL, version bigint NOT NULL DEFAULT 0 CHECK>=0)
CREATE UNIQUE INDEX uk_outbox_message_id(message_id); CREATE UNIQUE INDEX uk_outbox_idempotency_key(idempotency_key) WHERE idempotency_key IS NOT NULL
CREATE idx_outbox_claim/reclaim/cleanup with exact Spec predicates; CREATE TABLE ddl_history with runner required PK(script,type), UK(type,version), tenant0, checksum/route; no old SQL1 edits
```

- Verification contribution: TEST-013；runner family已可解析，先固定表/索引合同。
- After this file: 已建立 DDL-001 one SQL script，后续文件接入/验证前不得声称整个Step完成。

#### File 4 — `CREATE egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/resources/db/egon-outbox-mp/manifest.json`

- Purpose: SQL字节冻结后才写manifest哈希。
- Symbols: family/version/path/SHA-256 entry。
- Repository evidence: egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/resources/db/transactional-outbox/postgresql/V1__create_transactional_outbox_schema.sql存量Flyway只读。
- Dependencies and consumers: Common DdlManifestBO/runner读取classpath唯一脚本；新目标空schema。
- Why now: Step 2的第4个文件；SQL字节冻结后才写manifest哈希。
- Contract/signature changes: family/version/path/SHA-256 entry；family component-outbox，唯一版本20260924_001，sha256来自上个SQL最终bytes。
- Input/output and state mapping: family component-outbox，唯一版本20260924_001，sha256来自上个SQL最终bytes。
- Error and edge behavior: Common DdlManifestBO/runner读取classpath唯一脚本；新目标空schema；失败不吞异常、不伪造成功，适用断言由TEST-013验证。
- Standards impact: `MC-SCOPE-001`, `MC-TEST-001`；本文件按当前组件约定实现并限定在本Step的路径内。
- Literal rule enforcement: Rule 11：保留获批的单Starter基础组件结构与MP/受管DDL/CQE边界，绝不改业务平台层次。
- Implementation pseudocode:

```json
compute sha256sum of V20260924_001__initialize_outbox_mp_schema.sql final bytes
manifest.family="component-outbox"; scripts=[{version:"20260924_001",path:"db/egon-outbox-mp/V20260924_001__initialize_outbox_mp_schema.sql",sha256:hex(SHA256(SQL2_file_bytes))}]
read back both bytes and manifest entry; reject changes to applied prefix or later checksum mismatch
```

- Verification contribution: TEST-013；SQL字节冻结后才写manifest哈希。
- After this file: 已建立 family/version/path/SHA-256 entry，后续文件接入/验证前不得声称整个Step完成。

#### File 5 — `CREATE egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/autoconfigure/OutboxMpStorageProperties.java`

- Purpose: DDL引导与后续Store共享明确受控参数。
- Symbols: sqlSessionFactoryBeanName,migrationMode,migrationLockTimeout,manifestResource。
- Repository evidence: egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/autoconfigure/TransactionalOutboxProperties.java已有storage配置。
- Dependencies and consumers: OutboxManagedDdlInitializer和Store/LegacyMigrationService消费；无应用profile文件。
- Why now: Step 2的第5个文件；DDL引导与后续Store共享明确受控参数，并先建立initializer的编译类型合同。
- Contract/signature changes: sqlSessionFactoryBeanName,migrationMode,migrationLockTimeout,manifestResource；默认sqlSessionFactory/false/30s/固定manifest；@Validated不允许任意资源。
- Input/output and state mapping: 默认sqlSessionFactory/false/30s/固定manifest；@Validated不允许任意资源。
- Error and edge behavior: OutboxManagedDdlInitializer和Store/LegacyMigrationService消费；无应用profile文件；失败不吞异常、不伪造成功，适用断言由TEST-013验证。
- Standards impact: `MC-CONFIG-001`, `MC-MODEL-001`, `MC-NAME-001`, `MC-SCOPE-001`, `MC-TEST-001`, `MC-TIME-001`, `MC-VALID-001`；本文件按当前组件约定实现并限定在本Step的路径内。
- Literal rule enforcement: Rule 1：类型名按Event/BO/PO/DAO/Repository/Service/Factory等语义后缀，不新增Data/Info/Param/Bean载体；Rule 2：按@Validated/@Valid、原生约束与Default/已有MP组逐层校验，非代理处调用现有ValidationUtils；Rule 3：普通class完整Lombok注解；EgonModel PO用@SuperBuilder/callSuper；MapStruct仅映射真实边界；Rule 7：配置键对全部profile保持一致，组件无生产application YAML，双语README逐项同步；Rule 10：使用Instant/Duration/Clock与PG微秒，禁止Date/Calendar/SimpleDateFormat；Rule 11：保留获批的单Starter基础组件结构与MP/受管DDL/CQE边界，绝不改业务平台层次。
- Implementation pseudocode:

```java
@Validated class OutboxMpStorageProperties; named @Bean("outboxMpStorageProperties") method carries @ConfigurationProperties(prefix="egon.cola.component.transactional-outbox.storage.mp")
class OutboxMpStorageProperties with four exact Spec §15.3 keys; @NotBlank bean name, @Positive Duration, fixed manifest path validator
migrationMode defaults false and never changes the runtime OutboxStore API; profile-key parity asserted in Step8 docs
```

- Verification contribution: TEST-013；DDL引导与后续Store共享明确受控参数。
- After this file: 已建立 sqlSessionFactoryBeanName,migrationMode,migrationLockTimeout,manifestResource；继续建立initializer/factory/configuration编译合同。

#### File 6 — `CREATE egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/migration/OutboxManagedDdlInitializer.java`

- Purpose: SQL与manifest就绪后复用runner，不创建第二DDL引擎。
- Symbols: void initialize(Map<String,DataSource>,byte[])。
- Repository evidence: egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter/src/main/java/top/egon/cola/component/common/mybatis/ddl/EgonColaPostgreDdlRunner.java#runTarget及sharding/bootstrap/LogicalDataSourceFactory。
- Dependencies and consumers: EgonColaPostgreDdlRunner与NATIVE validator/OutboxMpStorageProperties；LogicalFactory调用。
- Why now: Step 2的第6个文件；SQL与manifest就绪后建立runner/API compile contract，RED后在File 10完成主体；不创建第二DDL引擎。
- Contract/signature changes: void initialize(Map<String,DataSource>,byte[])；topologyValidator准确fingerprint；唯一PRIMARY且NATIVE两条SINGLE同egon_outbox；schema空或历史有效；runner结果仅供内部就绪判断。
- Input/output and state mapping: topologyValidator准确fingerprint；唯一PRIMARY且NATIVE两条SINGLE同egon_outbox；schema空或历史有效。
- Error and edge behavior: EgonColaPostgreDdlRunner与NATIVE validator/OutboxMpStorageProperties；LogicalFactory调用；失败不吞异常、不伪造成功，适用断言由TEST-013验证。
- Standards impact: `MC-BEAN-001`, `MC-LOG-001`, `MC-NAME-001`, `MC-PATTERN-001`, `MC-SCOPE-001`, `MC-TEST-001`, `MC-TIME-001`, `MC-VALID-001`；本文件按当前组件约定实现并限定在本Step的路径内。
- Literal rule enforcement: Rule 1：类型名按Event/BO/PO/DAO/Repository/Service/Factory等语义后缀，不新增Data/Info/Param/Bean载体；Rule 2：按@Validated/@Valid、原生约束与Default/已有MP组逐层校验，非代理处调用现有ValidationUtils；Rule 4：实际业务类用@Slf4j、显式Bean名、@RequiredArgsConstructor及每个final依赖@Qualifier；Rule 9：复杂迁移由SSM State/Strategy/Adapter实际承担，禁止Dispatcher另写状态switch；Rule 10：使用Instant/Duration/Clock与PG微秒，禁止Date/Calendar/SimpleDateFormat；Rule 11：保留获批的单Starter基础组件结构与MP/受管DDL/CQE边界，绝不改业务平台层次。
- Implementation pseudocode:

```java
@Slf4j @RequiredArgsConstructor with qualified runner, validator, shardingProperties, storageProperties; named @Bean("outboxManagedDdlInitializer") in OutboxMybatisPlusAutoConfiguration
compile contract void initialize(Map<String,DataSource>,byte[]); temporarily throws the stable pending-implementation failure until File 10
Keep applied/skipped runner details private and expose no result-list return contract to component consumers.
Keep the compile signature aligned with the existing Common runner, topology validator, and physical datasource map; the final implementation replaces only the temporary body.
```

- Verification contribution: TEST-013；SQL与manifest就绪后复用runner，不创建第二DDL引擎。
- After this file: 已建立 initialize(Map<String,DataSource>,byte[]) compile contract；暂不运行DDL，File 9用mock确认它被logical factory先调用。

#### File 7 — `CREATE egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/migration/OutboxLogicalDataSourceFactory.java`

- Purpose: initializer已能证明DDL就绪，再接既有physical→logical扩展点。
- Symbols: create(Map<String,DataSource>,byte[]):DataSource。
- Repository evidence: egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter/src/main/java/top/egon/cola/component/common/mybatis/autoconfigure/EgonColaShardingAutoConfiguration.java#egonColaShardingLogicalDataSourceFactory。
- Dependencies and consumers: Common EgonColaShardingAutoConfiguration默认@Bean的同名扩展点；自定义factory必须显式接hook。
- Why now: Step 2的第7个文件；initializer编译合同存在后建立接口合同，RED后在File 11接入DDL hook。
- Contract/signature changes: create(Map<String,DataSource>,byte[]):DataSource；先运行initializer后委托标准YamlShardingSphereDataSourceFactory.createDataSource；失败无logical DS。
- Input/output and state mapping: 先运行initializer后委托标准YamlShardingSphereDataSourceFactory.createDataSource；失败无logical DS。
- Error and edge behavior: Common EgonColaShardingAutoConfiguration默认@Bean的同名扩展点；自定义factory必须显式接hook；失败不吞异常、不伪造成功，适用断言由TEST-013验证。
- Standards impact: `MC-BEAN-001`, `MC-LOG-001`, `MC-NAME-001`, `MC-PATTERN-001`, `MC-SCOPE-001`, `MC-TEST-001`；本文件按当前组件约定实现并限定在本Step的路径内。
- Literal rule enforcement: Rule 1：类型名按Event/BO/PO/DAO/Repository/Service/Factory等语义后缀，不新增Data/Info/Param/Bean载体；Rule 4：实际业务类用@Slf4j、显式Bean名、@RequiredArgsConstructor及每个final依赖@Qualifier；Rule 9：复杂迁移由SSM State/Strategy/Adapter实际承担，禁止Dispatcher另写状态switch；Rule 11：保留获批的单Starter基础组件结构与MP/受管DDL/CQE边界，绝不改业务平台层次。
- Implementation pseudocode:

```java
@Slf4j @RequiredArgsConstructor implements LogicalDataSourceFactory; named @Bean("egonColaShardingLogicalDataSourceFactory") in OutboxMybatisPlusAutoConfiguration
compile create(Map<String,DataSource>,byte[]):DataSource contract; temporarily fails before initializer delegation until File 11
if existing custom factory owns name, do not override silently: configuration validation requires its explicit initializer call before ready
```

- Verification contribution: TEST-013；initializer已能证明DDL就绪，再接既有physical→logical扩展点。
- After this file: 已建立 LogicalDataSourceFactory compile contract；File 9 now compiles and can assert missing initializer delegation.

#### File 8 — `CREATE egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/autoconfigure/OutboxMybatisPlusAutoConfiguration.java`

- Purpose: 先接DDL hook，Store Bean要等Step3/4类型存在再加。
- Symbols: pre-sharding DDL factory registration only。
- Repository evidence: egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/autoconfigure/TransactionalOutboxAutoConfiguration.java既有auto-config模式。
- Dependencies and consumers: Common MP autoConfig/DDL runner已存在；Step4同文件仅新增DAO/Store Bean方法。
- Why now: Step 2的第8个文件；先注册initializer/factory编译合同，File 9执行无PG RED；Store Bean要等Step3/4类型存在再加。
- Contract/signature changes: pre-sharding DDL factory registration only；注册initializer/LogicalFactory/typed MP properties；此Step不发布默认OutboxStore。
- Input/output and state mapping: 注册initializer/LogicalFactory/typed MP properties；此Step不发布默认OutboxStore。
- Error and edge behavior: Common MP autoConfig/DDL runner已存在；Step4同文件仅新增DAO/Store Bean方法；失败不吞异常、不伪造成功，适用断言由TEST-013验证。
- Standards impact: `MC-BEAN-001`, `MC-CONFIG-001`, `MC-SCOPE-001`, `MC-TEST-001`；本文件按当前组件约定实现并限定在本Step的路径内。
- Literal rule enforcement: Rule 4：实际业务类用@Slf4j、显式Bean名、@RequiredArgsConstructor及每个final依赖@Qualifier；Rule 7：配置键对全部profile保持一致，组件无生产application YAML，双语README逐项同步；Rule 11：保留获批的单Starter基础组件结构与MP/受管DDL/CQE边界，绝不改业务平台层次。
- Implementation pseudocode:

```java
@AutoConfiguration(before=EgonColaShardingAutoConfiguration.class); bind OutboxMpStorageProperties through one named @Bean method
register named outboxManagedDdlInitializer and egonColaShardingLogicalDataSourceFactory hook; verify no duplicate BeanDefinition
leave outboxStore creation in existing TransactionalOutboxAutoConfiguration until Step4; do not create another datasource or bootstrapper
```

- Verification contribution: TEST-013；先接DDL hook，Store Bean要等Step3/4类型存在再加。
- After this file: 已建立 pre-sharding DDL factory registration only，后续文件接入/验证前不得声称整个Step完成。

#### File 9 — `CREATE egon-cola-components/egon-cola-component-transactional-outbox-starter/src/test/java/top/egon/cola/component/outbox/integration/OutboxManagedDdlIntegrationTest.java`

- Purpose: 编译合同就绪后固定无PG的DDL-hook先行断言；真实PG历史/拓扑方法保持单独授权门禁。
- Symbols: hook failure blocks logical DataSource; no-PG runner target/readiness and named properties; empty-schema, installed-prefix, drift and rollback scenarios。
- Repository evidence: egon-cola-components/egon-cola-component-transactional-outbox-starter/src/test/java/top/egon/cola/component/outbox/integration/PostgresqlOutboxTestSupport.java。
- Dependencies and consumers: File 6–8 compile contracts、Common runner；真实数据库fixture自行管理，Testcontainers仅在环境显式开启时启动。
- Why now: PLAN-CLAR-005批准的文件顺序；使用已存在类型定义可编译RED，且默认RED只用mock，不创建连接。
- Contract/signature changes: DDL-before-logical and drift/rollback tests；空egon_outbox初始化；非空无history拒绝；checksum/route drift；双实例锁；失败不建logical DS。
- Input/output and state mapping: 空egon_outbox初始化；非空无history拒绝；checksum/route drift；双实例锁；失败不建logical DS。
- Error and edge behavior: 原PG support + Common runner测试；测试内数据库生命周期由Testcontainers显式开关控制；失败不吞异常、不伪造成功，适用断言由TEST-013验证。
- Standards impact: `MC-SCOPE-001`, `MC-TEST-001`, `MC-VALID-001`；本文件按当前组件约定实现并限定在本Step的路径内。
- Literal rule enforcement: Rule 2：按@Validated/@Valid、原生约束与Default/已有MP组逐层校验，非代理处调用现有ValidationUtils；Rule 11：保留获批的单Starter基础组件结构与MP/受管DDL/CQE边界，绝不改业务平台层次。
- Implementation pseudocode:

```java
no-PG RED/GREEN: instantiate OutboxLogicalDataSourceFactory with a mocked OutboxManagedDdlInitializer that records invocation then throws a fixed RuntimeException; call create(physical,yaml), assert the same failure propagates and the logical datasource delegate was never reached; the compile scaffold missing delegation causes RED without opening a connection
no-PG initializer: use the real topology validator and a mocked common runner; assert the fixed PRIMARY/schema/role/manifest/fingerprint target and readiness evidence after a full result, without opening a physical connection
no-PG properties: ApplicationContextRunner binds the named properties/defaults and rejects a nonpositive migration lock timeout or nonfixed manifest resource
PG-gated: same PRIMARY and empty egon_outbox; initialize -> APPLIED, repeat -> SKIPPED; unmanaged schema/checksum/route drift fails without DROP or repair
Each database test independently checks EGON_OUTBOX_TEST_POSTGRES_ENABLED; compile and no-PG method remain runnable with that environment variable unset
```

- Verification contribution: TEST-013；先定义启动顺序与历史不变量RED，PG执行须另获授权。
- After this file: no-PG委托顺序测试为该Step第二个RED；PG数据库行为仍Runtime unverified；File 10–12实现后Failsafe no-PG test must pass.

#### File 10 — `MODIFY egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/migration/OutboxManagedDdlInitializer.java`

- Purpose: File 9 RED后完成复用公共runner的拓扑校验/执行主体。
- Symbols: initialize(Map<String,DataSource>,byte[]) body and readiness.
- Repository evidence: Common MP `EgonColaShardingTopologyValidator`, `EgonColaPostgreDdlRunner`, and DDL target APIs.
- Dependencies and consumers: properties, exact NATIVE topology, unique PRIMARY and component-outbox manifest; called before logical datasource creation.
- Why now: 已有编译合同与RED证据；填入最小受管DDL主体。
- Contract/signature changes: Signature is void; verify the complete manifest result internally and record read-only readiness only after one schema-scoped MASTER_DATA target returns APPLIED/SKIPPED for every entry.
- Input/output and state mapping: Require SHARDING/NATIVE/LOCAL, one PRIMARY/no REPLICA, exact two egon_outbox SINGLE nodes; derive route fingerprint from existing topology validator.
- Error and edge behavior: Any topology/manifest/runner error fails before returning ready; no table drop, repair, hidden history adoption, or pool ownership.
- Standards impact: `MC-BEAN-001`, `MC-LOG-001`, `MC-NAME-001`, `MC-PATTERN-001`, `MC-SCOPE-001`, `MC-TEST-001`, `MC-TIME-001`, `MC-VALID-001`.
- Literal rule enforcement: Rule 1 behavior class suffix; Rule 2 configuration + topology validation; Rule 4 named Bean/qualifiers; Rule 9 uses existing topology Validator/DDL runner; Rule 10 uses java.time; Rule 11 preserves Starter and Common MP ownership.
- Implementation pseudocode:

```java
validate typed topology and canonical native YAML; require one PRIMARY and exact message/history SINGLE profiles in egon_outbox
load one fixed classpath manifest through the existing unique ObjectMapper alias; run the common runner on one non-owning schema-scoped DataSource
set ready evidence only after a complete APPLIED/SKIPPED result; restore original schema before connection close
Reject ambiguous physical pools, unknown node groups, schema drift, or non-local/NATIVE topology before invoking the runner.
```

- Verification contribution: TEST-013 in-memory hook verification; actual PostgreSQL remains environment-gated.
- After this file: initializer returns void only after the fixed manifest prefix is verified/applied and the matching read-only readiness evidence is stored.

#### File 11 — `MODIFY egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/migration/OutboxLogicalDataSourceFactory.java`

- Purpose: File 9 RED后把受管DDL作为logical DataSource创建的前置条件。
- Symbols: `create(Map<String,DataSource>,byte[])` body.
- Repository evidence: Common `EgonColaShardingDataSourceBootstrapper.LogicalDataSourceFactory` extension and `YamlShardingSphereDataSourceFactory`.
- Dependencies and consumers: named `egonColaShardingLogicalDataSourceFactory` bean; Common bootstrapper supplies physical pools and normalized YAML.
- Why now: initializer is complete and the RED established that delegate ordering is required.
- Contract/signature changes: Signature unchanged; DDL initializer runs before ShardingSphere factory.
- Input/output and state mapping: returns the logical datasource only after DDL readiness.
- Error and edge behavior: initializer failure propagates and prevents logical datasource creation; never creates/owns a physical pool.
- Standards impact: `MC-BEAN-001`, `MC-LOG-001`, `MC-NAME-001`, `MC-PATTERN-001`, `MC-SCOPE-001`, `MC-TEST-001`.
- Literal rule enforcement: Rule 1 behavior suffix; Rule 4 named Bean and qualified constructor; Rule 9 adapter delegates to Common extension; Rule 11 preserves the three-layer component boundary.
- Implementation pseudocode:

```java
initializer.initialize(physical,yaml)
return YamlShardingSphereDataSourceFactory.createDataSource(physical,yaml)
Propagate initializer failures unchanged and do not parse/delegate the YAML or create a logical datasource before the initializer succeeds.
```

- Verification contribution: no-PG Failsafe ordering RED/GREEN; PG logical start remains unverified.
- After this file: no logical datasource can be returned before managed DDL succeeds.

#### File 12 — `MODIFY egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/autoconfigure/OutboxMybatisPlusAutoConfiguration.java`

- Purpose: 完成类型具名装配与Common Sharding auto-configuration排序。
- Symbols: named storage properties, initializer, logical data source factory, true property aliases.
- Repository evidence: `TransactionalOutboxAutoConfiguration` and Common `EgonColaShardingAutoConfiguration` bean names/ordering.
- Dependencies and consumers: Step 4 will add this class to `AutoConfiguration.imports`; Step 2 creates the wiring now without a second Store.
- Why now: Initializer/factory implementations are green and the corresponding named contracts are fixed.
- Contract/signature changes: `@AutoConfiguration(before=EgonColaShardingAutoConfiguration.class)`; exact named beans; no `OutboxStore` in this step.
- Input/output and state mapping: Bind storage properties, register the DDL initializer and logical factory before Common starts physical→logical bootstrap.
- Error and edge behavior: Bean-name conflict fails startup; no silent Common default factory override; not imported until the planned registry wiring step.
- Standards impact: `MC-BEAN-001`, `MC-CONFIG-001`, `MC-SCOPE-001`, `MC-TEST-001`.
- Literal rule enforcement: Rule 4 named beans/qualified constructor; Rule 7 typed keys and defaults; Rule 11 component-only auto-config boundary.
- Implementation pseudocode:

```java
@AutoConfiguration(before=EgonColaShardingAutoConfiguration.class)
@EnableConfigurationProperties(OutboxMpStorageProperties.class)
register uniquely named initializer and logical factory; verify duplicate logical factory bean rejects
The factory delegates to the Common logical datasource extension point only after the initializer returns successfully.
```

- Verification contribution: O test compilation; PostgreSQL DDL ordering test remains gated.
- After this file: hook is packaged as a bean definition; Step2 is complete without running DDL.

- Validation working directory: `/Users/mario/SelfProject/Egon-COLA`。
- Verification command:
  - `mvn -pl egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter -am -DfailIfNoTests=false -Dsurefire.failIfNoSpecifiedTests=false -Dtest=EgonColaDdlManifestFamilyTest test`
  - `mvn -pl egon-cola-components/egon-cola-component-transactional-outbox-starter -am -DskipTests test-compile`
  - `mvn -pl egon-cola-components/egon-cola-component-transactional-outbox-starter -am -DfailIfNoTests=false -Dsurefire.failIfNoSpecifiedTests=false -Dtest=EgonColaDdlManifestFamilyTest -Dit.test=OutboxManagedDdlIntegrationTest -Dfailsafe.failIfNoSpecifiedTests=false verify` with `EGON_OUTBOX_TEST_POSTGRES_ENABLED` unset.
- Expected result: family test's 3 nonintegration cases run/pass; O production and integration test sources compile; final Failsafe report runs 9 tests with 0 failures/errors, 3 no-PG paths passing, and 6 PG-gated paths skipped. PostgreSQL-gated integration cases are not run and remain `Runtime unverified`; skip is not a PostgreSQL pass.
- Failure returns to: 本Step的第1个RED文件，定位测试/夹具错误；编译/行为失败返回对应生产文件；DDL/依赖不兼容返回Step 1或Step 2并暂停后续。
- Completion criteria: 新受管SQL/manifest与引导hook具备；未执行DDL/旧表迁移；本Step适用Manual Checks已按实际静态/单测证据复核；禁止把未授权的PG运行写为PASS。
- Rollback: SQL2若尚未运行仅限本Step路径回退；一旦runner已在目标schema提交，不删除DDL/history，使用受控前向修复。
- Commit paths: `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter/src/test/java/top/egon/cola/component/common/mybatis/ddl/EgonColaDdlManifestFamilyTest.java` `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter/src/main/java/top/egon/cola/component/common/mybatis/ddl/EgonColaDdlManifestBO.java` `egon-cola-components/egon-cola-component-transactional-outbox-starter/src/test/java/top/egon/cola/component/outbox/integration/OutboxManagedDdlIntegrationTest.java` `egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/resources/db/egon-outbox-mp/V20260924_001__initialize_outbox_mp_schema.sql` `egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/resources/db/egon-outbox-mp/manifest.json` `egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/autoconfigure/OutboxMpStorageProperties.java` `egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/migration/OutboxManagedDdlInitializer.java` `egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/migration/OutboxLogicalDataSourceFactory.java` `egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/autoconfigure/OutboxMybatisPlusAutoConfiguration.java`
- Commit: `feat(outbox): add managed MP schema initialization`。

### Step 3 — 建立MP消息模型、显式SQL与前向转换

- Requirements: REQ-001 REQ-004 REQ-006 REQ-009 REQ-010
- Dependencies: Step 2
- Baseline state: Step 2提交后，新受管SQL/manifest与引导hook具备；未执行DDL/旧表迁移。
- Observable outcome: 建立MP消息模型、显式SQL与前向转换；验收对应TEST-011。
- End state: PO/DAO/XML/Converter/技术context可编译；默认Store仍JDBC。
- Test-first gate: Required — PO可编译后ConverterTest因没有MapStruct转换Bean、字段/headers投影而RED。PO/Enum先作为测试的编译前置，随后Converter测试必须因行为缺失而RED。
- Manual Checks: `MC-ARCH-001`, `MC-BEAN-001`, `MC-CONVERT-001`, `MC-JSON-001`, `MC-LOG-001`, `MC-MODEL-001`, `MC-NAME-001`, `MC-SCOPE-001`, `MC-TEST-001`, `MC-TIME-001`, `MC-VALID-001`。在本Step完成前以本段文件和命令逐项核对，已知失败不得延后到后续清理。
- Literal Rules: Rule 1, Rule 2, Rule 3, Rule 4, Rule 6, Rule 10, Rule 11。每个文件如下展开；Rule 11覆盖全部路径。
- Ordered files:

#### File 1 — `CREATE egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/persistence/po/OutboxMessagePO.java`

- Purpose: Mapper/Converter tests need compileable contract first。
- Symbols: @TableName egon_outbox; EgonModel<OutboxMessagePO>。
- Repository evidence: egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter/src/main/java/top/egon/cola/component/common/mybatis/model/EgonModel.java以及SQL1原字段。
- Dependencies and consumers: EgonModel/BasePojo, OutboxStatus; DAO/repository/MapStruct consume。
- Why now: Step 3的第1个文件；Mapper/Converter tests need compileable contract first。
- Contract/signature changes: @TableName egon_outbox; EgonModel<OutboxMessagePO>；inherit id/tenantId/createUserId/createTime/updateUserId/updateTime/deletedAt/version once; declare original 21 business/technical message columns。
- Input/output and state mapping: inherit id/tenantId/createUserId/createTime/updateUserId/updateTime/deletedAt/version once; declare original 21 business/technical message columns。
- Error and edge behavior: EgonModel/BasePojo, OutboxStatus; DAO/repository/MapStruct consume；失败不吞异常、不伪造成功，适用断言由TEST-011验证。
- Standards impact: `MC-JSON-001`, `MC-MODEL-001`, `MC-NAME-001`, `MC-SCOPE-001`, `MC-TEST-001`, `MC-TIME-001`, `MC-VALID-001`；本文件按当前组件约定实现并限定在本Step的路径内。
- Literal rule enforcement: Rule 1：类型名按Event/BO/PO/DAO/Repository/Service/Factory等语义后缀，不新增Data/Info/Param/Bean载体；Rule 2：按@Validated/@Valid、原生约束与Default/已有MP组逐层校验，非代理处调用现有ValidationUtils；Rule 3：普通class完整Lombok注解；EgonModel PO用@SuperBuilder/callSuper；MapStruct仅映射真实边界；Rule 6：Spring Boot Jackson和@EnumValue/@JsonFormat；永不以ordinal作业务编码；Rule 10：使用Instant/Duration/Clock与PG微秒，禁止Date/Calendar/SimpleDateFormat；Rule 11：保留获批的单Starter基础组件结构与MP/受管DDL/CQE边界，绝不改业务平台层次。
- Implementation pseudocode:

```java
@Data @NoArgsConstructor @AllArgsConstructor @Accessors(chain=true) @SuperBuilder @EqualsAndHashCode(callSuper=true)
@TableName(value="egon_cola_outbox_message",schema="egon_outbox",autoResultMap=true) class OutboxMessagePO extends EgonModel<OutboxMessagePO> implements BasePojo
fields original message_id..completed_at only; Instant maps timestamptz(6), headersJson String, OutboxStatus enum; inherited eight fields never redeclared
```

- Verification contribution: TEST-011；Mapper/Converter tests need compileable contract first。
- After this file: 已建立 @TableName egon_outbox; EgonModel<OutboxMessagePO>，后续文件接入/验证前不得声称整个Step完成。

#### File 2 — `MODIFY egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/store/OutboxStatus.java`

- Purpose: PO需要明确VARCHAR持久化码再生成映射。
- Symbols: message field @EnumValue。
- Repository evidence: egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/store/OutboxStatus.java现有五值EgonEnum。
- Dependencies and consumers: Mapper XML MP handler、已有配送记录与测试。
- Why now: Step 3的第2个文件；PO需要明确VARCHAR持久化码再生成映射。
- Contract/signature changes: message field @EnumValue；PENDING/PROCESSING/RETRY_WAIT/SUCCEEDED/DEAD字符串不变；EgonEnum int getCode不变。
- Input/output and state mapping: PENDING/PROCESSING/RETRY_WAIT/SUCCEEDED/DEAD字符串不变；EgonEnum int getCode不变。
- Error and edge behavior: Mapper XML MP handler、已有配送记录与测试；失败不吞异常、不伪造成功，适用断言由TEST-011验证。
- Standards impact: `MC-JSON-001`, `MC-NAME-001`, `MC-SCOPE-001`, `MC-TEST-001`；本文件按当前组件约定实现并限定在本Step的路径内。
- Literal rule enforcement: Rule 1：类型名按Event/BO/PO/DAO/Repository/Service/Factory等语义后缀，不新增Data/Info/Param/Bean载体；Rule 6：Spring Boot Jackson和@EnumValue/@JsonFormat；永不以ordinal作业务编码；Rule 11：保留获批的单Starter基础组件结构与MP/受管DDL/CQE边界，绝不改业务平台层次。
- Implementation pseudocode:

```java
annotate existing String message field with @EnumValue; do not annotate int code or change constructor order
assert persisted value is PENDING/PROCESSING/RETRY_WAIT/SUCCEEDED/DEAD, never ordinal or numeric code
keep getCode()/getMessage() and existing serialization behavior for old internal callers
```

- Verification contribution: TEST-011；PO需要明确VARCHAR持久化码再生成映射。
- After this file: 已建立 message field @EnumValue，后续文件接入/验证前不得声称整个Step完成。

#### File 3 — `CREATE egon-cola-components/egon-cola-component-transactional-outbox-starter/src/test/java/top/egon/cola/component/outbox/persistence/OutboxMessageConverterTest.java`

- Purpose: PO/enum编译前置有了，缺Converter应RED。
- Symbols: PO→OutboxRecord, insert/import projection assertions。
- Repository evidence: egon-cola-components/egon-cola-component-transactional-outbox-starter/src/test/java/top/egon/cola/component/outbox/serialization/JacksonOutboxMessageSerializerTest.java。
- Dependencies and consumers: Common BaseForwardConverter与MapStruct；现有Serialization测试作风格样本。
- Why now: Step 3的第3个文件；PO/enum编译前置有了，缺Converter应RED。
- Contract/signature changes: PO→OutboxRecord, insert/import projection assertions；来源PO的所有旧字段到Record，headers原JSON→Map；迁移原headers保留字节。
- Input/output and state mapping: 来源PO的所有旧字段到Record，headers原JSON→Map；迁移原headers保留字节。
- Error and edge behavior: Common BaseForwardConverter与MapStruct；现有Serialization测试作风格样本；失败不吞异常、不伪造成功，适用断言由TEST-011验证。
- Standards impact: `MC-CONVERT-001`, `MC-JSON-001`, `MC-MODEL-001`, `MC-SCOPE-001`, `MC-TEST-001`, `MC-TIME-001`, `MC-VALID-001`；本文件按当前组件约定实现并限定在本Step的路径内。
- Literal rule enforcement: Rule 2：按@Validated/@Valid、原生约束与Default/已有MP组逐层校验，非代理处调用现有ValidationUtils；Rule 3：普通class完整Lombok注解；EgonModel PO用@SuperBuilder/callSuper；MapStruct仅映射真实边界；Rule 6：Spring Boot Jackson和@EnumValue/@JsonFormat；永不以ordinal作业务编码；Rule 10：使用Instant/Duration/Clock与PG微秒，禁止Date/Calendar/SimpleDateFormat；Rule 11：保留获批的单Starter基础组件结构与MP/受管DDL/CQE边界，绝不改业务平台层次。
- Implementation pseudocode:

```java
assert converter.toTarget(poWithStatus(PROCESSING)) has same messageId/payload/Instant and Map headers; inherited tenant/version absent in OutboxRecord
assert toInsertPO(NewOutboxRecord) does not invent audit/tenant; toMigrationPO(oldRecord,rawPayload,rawHeadersJson) keeps old id and raw strings
assert null input does not masquerade as a legal record, unknown enum/status fails, no manual BeanUtils or JSON PO-copy
```

- Verification contribution: TEST-011；PO/enum编译前置有了，缺Converter应RED。
- After this file: 本测试定义该Step的RED断言，生产行为仍缺失；其余文件填入后GREEN。

#### File 4 — `CREATE egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/persistence/converter/OutboxHeadersConverter.java`

- Purpose: Converter对headers_json有真实协议映射需求。
- Symbols: parse headersJson with Boot Jackson。
- Repository evidence: egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/validation/OutboxMessageValidator.java和serializer现有Boot ObjectMapper。
- Dependencies and consumers: MapStruct uses=OutboxHeadersConverter；@RequiredArgsConstructor/@Qualifier objectMapper。
- Why now: Step 3的第4个文件；Converter对headers_json有真实协议映射需求。
- Contract/signature changes: parse headersJson with Boot Jackson；JSON object的string keys/values，拒绝null key/value及坏JSON；不改变迁移原字符串。
- Input/output and state mapping: JSON object的string keys/values，拒绝null key/value及坏JSON；不改变迁移原字符串。
- Error and edge behavior: MapStruct uses=OutboxHeadersConverter；@RequiredArgsConstructor/@Qualifier objectMapper；失败不吞异常、不伪造成功，适用断言由TEST-011验证。
- Standards impact: `MC-BEAN-001`, `MC-JSON-001`, `MC-LOG-001`, `MC-NAME-001`, `MC-SCOPE-001`, `MC-TEST-001`, `MC-VALID-001`；本文件按当前组件约定实现并限定在本Step的路径内。
- Literal rule enforcement: Rule 1：类型名按Event/BO/PO/DAO/Repository/Service/Factory等语义后缀，不新增Data/Info/Param/Bean载体；Rule 2：按@Validated/@Valid、原生约束与Default/已有MP组逐层校验，非代理处调用现有ValidationUtils；Rule 4：实际业务类用@Slf4j、显式Bean名、@RequiredArgsConstructor及每个final依赖@Qualifier；Rule 6：Spring Boot Jackson和@EnumValue/@JsonFormat；永不以ordinal作业务编码；Rule 11：保留获批的单Starter基础组件结构与MP/受管DDL/CQE边界，绝不改业务平台层次。
- Implementation pseudocode:

```java
@Slf4j @RequiredArgsConstructor; named @Bean("outboxHeadersConverter") from MP auto-config; final @Qualifier("outboxStateMachineObjectMapper") ObjectMapper
Map<String,String> parseHeaders(String json): strict Jackson tree object -> immutable string Map, reject forbidden/oversized header per OutboxMessageValidator
when source migration, keep raw headersJson untouched in PO; only PO→OutboxRecord parses at runtime
```

- Verification contribution: TEST-011；Converter对headers_json有真实协议映射需求。
- After this file: 已建立 parse headersJson with Boot Jackson，后续文件接入/验证前不得声称整个Step完成。

#### File 5 — `CREATE egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/persistence/converter/OutboxMessageConverter.java`

- Purpose: helper可用后MapStruct生成实际跨界映射。
- Symbols: BaseForwardConverter<OutboxMessagePO,OutboxRecord>。
- Repository evidence: egon-cola-components/egon-cola-component-transactional-outbox-starter/pom.xml Step1加入MapStruct处理器。
- Dependencies and consumers: Common BaseForwardConverter、headers helper；生成Bean outboxMessageConverterImpl。
- Why now: Step 3的第5个文件；helper可用后MapStruct生成实际跨界映射。
- Contract/signature changes: BaseForwardConverter<OutboxMessagePO,OutboxRecord>；toTarget、toInsertPO、toMigrationPO，字段/Null/Instant/status显式注解。
- Input/output and state mapping: toTarget、toInsertPO、toMigrationPO，字段/Null/Instant/status显式注解。
- Error and edge behavior: Common BaseForwardConverter、headers helper；生成Bean outboxMessageConverterImpl；失败不吞异常、不伪造成功，适用断言由TEST-011验证。
- Standards impact: `MC-CONVERT-001`, `MC-JSON-001`, `MC-MODEL-001`, `MC-NAME-001`, `MC-SCOPE-001`, `MC-TEST-001`, `MC-TIME-001`, `MC-VALID-001`；本文件按当前组件约定实现并限定在本Step的路径内。
- Literal rule enforcement: Rule 1：类型名按Event/BO/PO/DAO/Repository/Service/Factory等语义后缀，不新增Data/Info/Param/Bean载体；Rule 2：按@Validated/@Valid、原生约束与Default/已有MP组逐层校验，非代理处调用现有ValidationUtils；Rule 3：普通class完整Lombok注解；EgonModel PO用@SuperBuilder/callSuper；MapStruct仅映射真实边界；Rule 6：Spring Boot Jackson和@EnumValue/@JsonFormat；永不以ordinal作业务编码；Rule 10：使用Instant/Duration/Clock与PG微秒，禁止Date/Calendar/SimpleDateFormat；Rule 11：保留获批的单Starter基础组件结构与MP/受管DDL/CQE边界，绝不改业务平台层次。
- Implementation pseudocode:

```java
@Mapper(componentModel="spring", injectionStrategy=CONSTRUCTOR, uses=OutboxHeadersConverter.class, unmappedTargetPolicy=ERROR)
interface OutboxMessageConverter extends BaseForwardConverter<OutboxMessagePO,OutboxRecord>: toTarget maps headersJson→headers, status/string code, 22 original columns
NewOutboxRecord→PO maps supplied fields only; migration method uses rawPayload/rawHeadersJson parameters and old id; ignore inherited audit to MP fill, set version=0/deletedAt=null
```

- Verification contribution: TEST-011；helper可用后MapStruct生成实际跨界映射。
- After this file: 已建立 BaseForwardConverter<OutboxMessagePO,OutboxRecord>，后续文件接入/验证前不得声称整个Step完成。

#### File 6 — `CREATE egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/persistence/dao/OutboxMessageDAO.java`

- Purpose: 模型已可用，先固定查询/写入方法签名再写XML。
- Symbols: EgonColaMapper<OutboxMessagePO> plus named SQL methods。
- Repository evidence: egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter/src/main/java/top/egon/cola/component/common/mybatis/extension/EgonColaMapper.java和源项目UserDAO样例。
- Dependencies and consumers: OutboxMessageRepository唯一生产调用；由@MapperScan绑定同SqlSessionFactory。
- Why now: Step 3的第6个文件；模型已可用，先固定查询/写入方法签名再写XML。
- Contract/signature changes: EgonColaMapper<OutboxMessagePO> plus named SQL methods；insertMessage/selectExisting/selectDueForUpdate/claim/mark/cleanup/count/migration methods按Spec§11。
- Input/output and state mapping: insertMessage/selectExisting/selectDueForUpdate/claim/mark/cleanup/count/migration methods按Spec§11。
- Error and edge behavior: OutboxMessageRepository唯一生产调用；由@MapperScan绑定同SqlSessionFactory；失败不吞异常、不伪造成功，适用断言由TEST-011验证。
- Standards impact: `MC-NAME-001`, `MC-SCOPE-001`, `MC-TEST-001`, `MC-TIME-001`, `MC-VALID-001`；本文件按当前组件约定实现并限定在本Step的路径内。
- Literal rule enforcement: Rule 1：类型名按Event/BO/PO/DAO/Repository/Service/Factory等语义后缀，不新增Data/Info/Param/Bean载体；Rule 2：按@Validated/@Valid、原生约束与Default/已有MP组逐层校验，非代理处调用现有ValidationUtils；Rule 10：使用Instant/Duration/Clock与PG微秒，禁止Date/Calendar/SimpleDateFormat；Rule 11：保留获批的单Starter基础组件结构与MP/受管DDL/CQE边界，绝不改业务平台层次。
- Implementation pseudocode:

```java
@Mapper interface OutboxMessageDAO extends EgonColaMapper<OutboxMessagePO>
add Spec §11.2.1 named methods with scalar @Param(id,version,source,target,owner,limit,lease,Instant auditTime,userId); no QueryWrapper API
selectDueForUpdate and selectProcessingForUpdate return PO; updateClaim/mark return int affected rows; empty collection checked in Repository before mapper
selectActiveById/selectActiveByIds must have explicit XML; deleteVersionedById default method throws UnsupportedOperationException for non-soft-deletable technical table
```

- Verification contribution: TEST-011；模型已可用，先固定查询/写入方法签名再写XML。
- After this file: 已建立 EgonColaMapper<OutboxMessagePO> plus named SQL methods，后续文件接入/验证前不得声称整个Step完成。

#### File 7 — `CREATE egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/resources/mapper/outbox/OutboxMessageMapper.xml`

- Purpose: DAO签名确定后写XML，形成真实MP边界。
- Symbols: all named mapped SQL and resultMap。
- Repository evidence: egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter/src/main/java/top/egon/cola/component/common/mybatis/interceptor/EgonColaOriginalSqlGuardInterceptor.java#shape拒绝UPDATE FROM。
- Dependencies and consumers: original SQL guard仅接受平坦UPDATE/DELETE；DAO消费者在S4。
- Why now: Step 3的第7个文件；DAO签名确定后写XML，形成真实MP边界。
- Contract/signature changes: all named mapped SQL and resultMap；schema-qualified egon_outbox.*；tenant_id=0/deleted_at IS NULL；version/owner条件；所有原列与继承列显式映射。
- Input/output and state mapping: schema-qualified egon_outbox.*；tenant_id=0/deleted_at IS NULL；version/owner条件；所有原列与继承列显式映射。
- Error and edge behavior: original SQL guard仅接受平坦UPDATE/DELETE；DAO消费者在S4；失败不吞异常、不伪造成功，适用断言由TEST-011验证。
- Standards impact: `MC-JSON-001`, `MC-SCOPE-001`, `MC-TEST-001`, `MC-TIME-001`, `MC-VALID-001`；本文件按当前组件约定实现并限定在本Step的路径内。
- Literal rule enforcement: Rule 2：按@Validated/@Valid、原生约束与Default/已有MP组逐层校验，非代理处调用现有ValidationUtils；Rule 6：Spring Boot Jackson和@EnumValue/@JsonFormat；永不以ordinal作业务编码；Rule 10：使用Instant/Duration/Clock与PG微秒，禁止Date/Calendar/SimpleDateFormat；Rule 11：保留获批的单Starter基础组件结构与MP/受管DDL/CQE边界，绝不改业务平台层次。
- Implementation pseudocode:

```xml
<mapper namespace="top.egon.cola.component.outbox.persistence.dao.OutboxMessageDAO"> complete resultMap id+7 Egon fields+21 message columns
selectDueForUpdate/selectActiveById/selectActiveByIds: explicit columns and WHERE tenant_id=0 AND deleted_at IS NULL; due query ORDER BY next_attempt_at,id FOR UPDATE SKIP LOCKED LIMIT; no SELECT *
updateClaim/mark: flat UPDATE egon_outbox.egon_cola_outbox_message SET status=#{target},version=version+1,audit/lease WHERE id/version/source/owner/tenant0/deletedAt; insert ON CONFLICT DO NOTHING; bounded cleanup DELETE only prelocked succeeded IDs
```

- Verification contribution: TEST-011；DAO签名确定后写XML，形成真实MP边界。
- After this file: 已建立 all named mapped SQL and resultMap，后续文件接入/验证前不得声称整个Step完成。

#### File 8 — `CREATE egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/persistence/repository/OutboxMessageRepository.java`

- Purpose: DAO/XML已具名，Repository封装技术操作。
- Symbols: EgonColaRepository<OutboxMessageDAO,OutboxMessagePO>。
- Repository evidence: egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/java/top/egon/cola/archetype/source/light/infrastructure/user/repo/UserRepository.java。
- Dependencies and consumers: Common EgonColaRepository与props；注入qualified outboxMessageDAO。
- Why now: Step 3的第8个文件；DAO/XML已具名，Repository封装技术操作。
- Contract/signature changes: EgonColaRepository<OutboxMessageDAO,OutboxMessagePO>；业务/技术Store通过Repository调用，直接mapper和继承宽泛CRUD不成为默认Store路径。
- Input/output and state mapping: 业务/技术Store通过Repository调用，直接mapper和继承宽泛CRUD不成为默认Store路径。
- Error and edge behavior: Common EgonColaRepository与props；注入qualified outboxMessageDAO；失败不吞异常、不伪造成功，适用断言由TEST-011验证。
- Standards impact: `MC-BEAN-001`, `MC-LOG-001`, `MC-NAME-001`, `MC-SCOPE-001`, `MC-TEST-001`, `MC-TIME-001`, `MC-VALID-001`；本文件按当前组件约定实现并限定在本Step的路径内。
- Literal rule enforcement: Rule 1：类型名按Event/BO/PO/DAO/Repository/Service/Factory等语义后缀，不新增Data/Info/Param/Bean载体；Rule 2：按@Validated/@Valid、原生约束与Default/已有MP组逐层校验，非代理处调用现有ValidationUtils；Rule 4：实际业务类用@Slf4j、显式Bean名、@RequiredArgsConstructor及每个final依赖@Qualifier；Rule 10：使用Instant/Duration/Clock与PG微秒，禁止Date/Calendar/SimpleDateFormat；Rule 11：保留获批的单Starter基础组件结构与MP/受管DDL/CQE边界，绝不改业务平台层次。
- Implementation pseudocode:

```java
@Slf4j @Validated @RequiredArgsConstructor extends EgonColaRepository<OutboxMessageDAO,OutboxMessagePO>; @Bean("outboxMessageRepository") in MP auto-config
@Getter @Qualifier("outboxMessageDAO") final baseMapper; @Getter(PROTECTED) final @Qualifier("egon...EgonColaMybatisPlusProperties") properties
named insert/select/claim/mark/delete/count methods validate scalar ids/batch bounds/Default group and delegate DAO; assert affected count 0/1, never use QueryChain/ActiveRecord/cache annotations
```

- Verification contribution: TEST-011；DAO/XML已具名，Repository封装技术操作。
- After this file: 已建立 EgonColaRepository<OutboxMessageDAO,OutboxMessagePO>，后续文件接入/验证前不得声称整个Step完成。

#### File 9 — `CREATE egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/persistence/OutboxTechnicalContextExecutor.java`

- Purpose: Repository/MP模型验证需要显式tenant0作用域。
- Symbols: execute(Callable/Supplier) with tenant0/audit。
- Repository evidence: egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter/src/main/java/top/egon/cola/component/common/mybatis/business/EgonColaTenantIdProvider.java当前读MDC。
- Dependencies and consumers: EgonColaTenantIdProvider与MP properties的MDC key；业务Handler不在该作用域。
- Why now: Step 3的第9个文件；Repository/MP模型验证需要显式tenant0作用域。
- Contract/signature changes: execute(Callable/Supplier) with tenant0/audit；保存进入时MDC tenantId/userId；设置0/system:outbox；执行同步Mapper；finally还原。
- Input/output and state mapping: 保存进入时MDC tenantId/userId；设置0/system:outbox；执行同步Mapper；finally还原。
- Error and edge behavior: EgonColaTenantIdProvider与MP properties的MDC key；业务Handler不在该作用域；失败不吞异常、不伪造成功，适用断言由TEST-011验证。
- Standards impact: `MC-BEAN-001`, `MC-LOG-001`, `MC-NAME-001`, `MC-SCOPE-001`, `MC-TEST-001`, `MC-VALID-001`；本文件按当前组件约定实现并限定在本Step的路径内。
- Literal rule enforcement: Rule 1：类型名按Event/BO/PO/DAO/Repository/Service/Factory等语义后缀，不新增Data/Info/Param/Bean载体；Rule 2：按@Validated/@Valid、原生约束与Default/已有MP组逐层校验，非代理处调用现有ValidationUtils；Rule 4：实际业务类用@Slf4j、显式Bean名、@RequiredArgsConstructor及每个final依赖@Qualifier；Rule 11：保留获批的单Starter基础组件结构与MP/受管DDL/CQE边界，绝不改业务平台层次。
- Implementation pseudocode:

```java
@Slf4j @RequiredArgsConstructor with qualified EgonColaMybatisPlusProperties; @Bean("outboxTechnicalContextExecutor") in MP auto-config
execute(action): save MDC[properties.tenantId.mdcKey, properties.audit.userIdMdcKey]; set tenant="0", user="system:outbox"
try return action.get(); finally restore both prior values, including when mapper/validation/transaction throws; test nested invocation and business context restoration
```

- Verification contribution: TEST-011；Repository/MP模型验证需要显式tenant0作用域。
- After this file: 已建立 execute(Callable/Supplier) with tenant0/audit，后续文件接入/验证前不得声称整个Step完成。

#### File 10 — `CREATE egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/persistence/OutboxSchemaMetadataValidator.java`

- Purpose: Store会用原OutboxSchemaValidator委托此纯元数据检查。
- Symbols: validate target schema, indexes, ddl_history。
- Repository evidence: egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/store/OutboxSchemaValidator.java旧readiness检查。
- Dependencies and consumers: 源Store.validateSchema经此类执行；物理Connection只用于DatabaseMetaData/pg_catalog。
- Why now: Step 3的第10个文件；Store会用原OutboxSchemaValidator委托此纯元数据检查。
- Contract/signature changes: validate target schema, indexes, ddl_history；读取新schema确切列/索引/check/受管历史；不对业务消息读写JDBC。
- Input/output and state mapping: 读取新schema确切列/索引/check/受管历史；不对业务消息读写JDBC。
- Error and edge behavior: 源Store.validateSchema经此类执行；物理Connection只用于DatabaseMetaData/pg_catalog；失败不吞异常、不伪造成功，适用断言由TEST-011验证。
- Standards impact: `MC-BEAN-001`, `MC-LOG-001`, `MC-NAME-001`, `MC-SCOPE-001`, `MC-TEST-001`, `MC-VALID-001`；本文件按当前组件约定实现并限定在本Step的路径内。
- Literal rule enforcement: Rule 1：类型名按Event/BO/PO/DAO/Repository/Service/Factory等语义后缀，不新增Data/Info/Param/Bean载体；Rule 2：按@Validated/@Valid、原生约束与Default/已有MP组逐层校验，非代理处调用现有ValidationUtils；Rule 4：实际业务类用@Slf4j、显式Bean名、@RequiredArgsConstructor及每个final依赖@Qualifier；Rule 11：保留获批的单Starter基础组件结构与MP/受管DDL/CQE边界，绝不改业务平台层次。
- Implementation pseudocode:

```java
@Slf4j @RequiredArgsConstructor with qualified DDL readiness/physical metadata source; @Bean("outboxSchemaMetadataValidator") in MP auto-config
validate(): check egon_outbox.egon_cola_outbox_message has 29 columns including all inherited fields, five original indexes, three added checks, ddl_history prefix/hash/route
failure -> OutboxConfigurationException before poller ready; use only metadata reads, never insert/update/delete message rows via JDBC
```

- Verification contribution: TEST-011；Store会用原OutboxSchemaValidator委托此纯元数据检查。
- After this file: 已建立 validate target schema, indexes, ddl_history，后续文件接入/验证前不得声称整个Step完成。

- Validation working directory: `/Users/mario/SelfProject/Egon-COLA`。
- Verification command: `mvn -pl egon-cola-components/egon-cola-component-transactional-outbox-starter -am -DfailIfNoTests=false -Dsurefire.failIfNoSpecifiedTests=false -Dtest=OutboxMessageConverterTest test`。
- Expected result: exit 0，指定TEST-011相关非集成单测实际执行且全部通过；若涉及integration目录，在未获得PG/Testcontainers授权时标为未验证，不能以skip替代真实GREEN。
- Failure returns to: 本Step的第1个RED文件，定位测试/夹具错误；编译/行为失败返回对应生产文件；DDL/依赖不兼容返回Step 1或Step 2并暂停后续。
- Completion criteria: PO/DAO/XML/Converter/技术context可编译；默认Store仍JDBC；本Step适用Manual Checks已按实际静态/单测证据复核；禁止把未授权的PG运行写为PASS。
- Rollback: 限定回退本Step提交路径；若已切到新MP表并产生消息，必须遵守主Spec §16停写/核对/前向修复，不能直接启用旧JDBC。
- Commit paths: `egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/persistence/po/OutboxMessagePO.java` `egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/store/OutboxStatus.java` `egon-cola-components/egon-cola-component-transactional-outbox-starter/src/test/java/top/egon/cola/component/outbox/persistence/OutboxMessageConverterTest.java` `egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/persistence/converter/OutboxHeadersConverter.java` `egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/persistence/converter/OutboxMessageConverter.java` `egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/persistence/dao/OutboxMessageDAO.java` `egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/resources/mapper/outbox/OutboxMessageMapper.xml` `egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/persistence/repository/OutboxMessageRepository.java` `egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/persistence/OutboxTechnicalContextExecutor.java` `egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/persistence/OutboxSchemaMetadataValidator.java`
- Commit: `feat(outbox): model MP message persistence`。

### Step 4 — 以MP Store替换全部运行期JDBC消息持久化

- Requirements: REQ-001 REQ-004 REQ-007 REQ-008 REQ-009 REQ-010
- Dependencies: Step 3
- Baseline state: Step 3提交后，PO/DAO/XML/Converter/技术context可编译；默认Store仍JDBC。
- Observable outcome: 以MP Store替换全部运行期JDBC消息持久化；验收对应TEST-002/011。
- End state: 默认OutboxStore为MP且旧具体类已删除；Dispatcher结果策略留下一步。
- Test-first gate: Required — 先由File1 ContextRunner对默认JDBC Store做RED；Common MP显式租户标记测试须在旧验证器unmarked规则仍通过、marked Mapper因MODEL_TABLE_CANNOT_BE_IGNORED而RED；再实现精确标记放行。全部已知旧具体类引用在DELETE前修正，单个Step替换默认运行路径。
- Manual Checks: `MC-ARCH-001`, `MC-REUSE-001`, `MC-DEP-001`, `MC-BEAN-001`, `MC-CONFIG-001`, `MC-JSON-001`, `MC-LOG-001`, `MC-MODEL-001`, `MC-NAME-001`, `MC-PATTERN-001`, `MC-SCOPE-001`, `MC-TEST-001`, `MC-TIME-001`, `MC-VALID-001`。在本Step完成前以本段文件和命令逐项核对，已知失败不得延后到后续清理。
- Literal Rules: Rule 1, Rule 2, Rule 3, Rule 4, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11。每个文件如下展开；Rule 11覆盖全部路径。
- Ordered files:

#### File 1 — `CREATE egon-cola-components/egon-cola-component-transactional-outbox-starter/src/test/java/top/egon/cola/component/outbox/autoconfigure/OutboxMybatisPlusAutoConfigurationTest.java`

- Purpose: 先RED旧配置会产JDBC Store或忽视MapperXML缺失。
- Symbols: context wiring, missing MP, aliases, DS/factory。
- Repository evidence: egon-cola-components/egon-cola-component-transactional-outbox-starter/src/test/java/top/egon/cola/component/outbox/autoconfigure/TransactionalOutboxAutoConfigurationTest.java ContextRunner风格。
- Dependencies and consumers: ApplicationContextRunner复用现有autoConfig测试，不启动应用。
- Why now: Step 4的第1个文件；先RED旧配置会产JDBC Store或忽视MapperXML缺失。
- Contract/signature changes: context wiring, missing MP, aliases, DS/factory；唯一MP Store，缺SqlSessionFactory/Schema/Mapper时fail，关闭业务适配无新handler。
- Input/output and state mapping: 唯一MP Store，缺SqlSessionFactory/Schema/Mapper时fail，关闭业务适配无新handler。
- Error and edge behavior: ApplicationContextRunner复用现有autoConfig测试，不启动应用；失败不吞异常、不伪造成功，适用断言由TEST-002/011验证。
- Standards impact: `MC-BEAN-001`, `MC-CONFIG-001`, `MC-SCOPE-001`, `MC-TEST-001`, `MC-VALID-001`；本文件按当前组件约定实现并限定在本Step的路径内。
- Literal rule enforcement: Rule 2：按@Validated/@Valid、原生约束与Default/已有MP组逐层校验，非代理处调用现有ValidationUtils；Rule 4：实际业务类用@Slf4j、显式Bean名、@RequiredArgsConstructor及每个final依赖@Qualifier；Rule 7：配置键对全部profile保持一致，组件无生产application YAML，双语README逐项同步；Rule 11：保留获批的单Starter基础组件结构与MP/受管DDL/CQE边界，绝不改业务平台层次。
- Implementation pseudocode:

```java
configured ApplicationContextRunner supplies matching DataSourceTransactionManager and SqlSessionFactory with standard interceptors
assert OutboxStore bean is MybatisPlusOutboxStore, clock/ObjectMapper aliases resolve same instance, business.enabled=false yields no statemachine handler
mismatched DS/multiple PRIMARY/missing Mapper XML -> OutboxConfigurationException, never fallback to PostgresqlJdbcOutboxStore
```

- Verification contribution: TEST-002/011；先RED旧配置会产JDBC Store或忽视MapperXML缺失。
- After this file: 本测试定义该Step的RED断言，生产行为仍缺失；其余文件填入后GREEN。

#### File 2 — `CREATE egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter/src/main/java/top/egon/cola/component/common/mybatis/extension/EgonColaExplicitTenantScopeMapper.java`

- Purpose: 为确实不经全局TenantLine改写、而在每条具名SQL中自行限定tenant谓词的基础设施Mapper提供窄范围编译合同。
- Symbols: `@EgonColaExplicitTenantScopeMapper`。
- Repository evidence: Common MP `EgonColaMybatisPlusContractValidator` 当前拒绝所有位于`tenant-id.ignored-tables`中的BaseMapper模型；Spec §11.2/15.3要求Outbox技术表被精确忽略并使用tenant_id=0 SQL。
- Dependencies and consumers: Common验证器检查标记；O `OutboxMessageDAO`标记；业务Repository不使用。
- Why now: Step 4的第2个文件；标记类型先于RED测试与O Mapper引用提供完整编译合同。
- Contract/signature changes: RUNTIME保留、TYPE目标的标记注解；只声明Mapper语句自带显式租户范围，不关闭全局MP拦截器或绕过模型/SQL guard。
- Input/output and state mapping: 未标记且配置为ignored的Mapper仍拒绝；标记Mapper只有在租户忽略名单含其模型表时才允许由专用技术SQL负责隔离。
- Error and edge behavior: 业务DAO不得标记；标记不自动添加WHERE条件，遗漏tenant/deleted谓词仍由O具名SQL、治理测试及TEST-011阻断。
- Standards impact: `MC-ARCH-001`, `MC-REUSE-001`, `MC-DEP-001`, `MC-NAME-001`, `MC-SCOPE-001`, `MC-TEST-001`, `MC-VALID-001`；Common提供跨Starter的最小策略扩展。
- Literal rule enforcement: Rule 1：注解以语义明确的Mapper后缀命名；Rule 2：标记自身不替代边界/方法约束；Rule 4：不是Spring Bean；Rule 11：只为获批的Outbox基础设施增加显式MP扩展，不改业务分层。
- Implementation pseudocode:

```java
@Documented @Retention(RUNTIME) @Target(TYPE) public @interface EgonColaExplicitTenantScopeMapper {}
Javadoc states that every mapped read/write must carry its own tenant predicate and that ordinary business mappers must not use this marker
Do not change ignoredTables configuration semantics, disable tenant guards, or add an Outbox dependency to Common
```

- Verification contribution: TEST-002/011；建立Common验证器与O基础设施DAO之间的唯一声明合同。
- After this file: 测试已能引用标记，但Common验证行为仍拒绝ignored模型；下个文件先固定该行为差异。

#### File 3 — `MODIFY egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter/src/test/java/top/egon/cola/component/common/mybatis/autoconfigure/EgonColaMybatisPlusAutoConfigurationTest.java`

- Purpose: 先固定ignored业务Mapper仍被拒绝、显式租户作用域Mapper才可通过的RED/GREEN合同。
- Symbols: marked/unmarked mapper table-ignore validation tests。
- Repository evidence: 现有文件已提供`SafeOuterConfiguration`、MapperFactory测试SQLSessionFactory样例及ignoredTables配置测试；Common验证器原始错误码为`MODEL_TABLE_CANNOT_BE_IGNORED`。
- Dependencies and consumers: `EgonColaExplicitTenantScopeMapper`、测试EgonModel/BaseMapper、真实Common验证器；不连接数据库。
- Why now: Step 4的第3个文件；标记可编译后运行RED，证明失败是Common既有安全策略拒绝，而不是缺类型/依赖。
- Contract/signature changes: ignored且未标记的模型仍启动失败；ignored且标记的Mapper按显式谓词合同启动通过；抽象Mapper操作仍要求具名XML，default方法按其Java实现验证而不伪造mapped statement；非ignored的常规Mapper保持既有合同。
- Input/output and state mapping: ignored table name使用Common现有schema/name归一化；本地Test Mapper的mapped statements以XML资源名注册，满足其余合同门禁。
- Error and edge behavior: 本地Validator/TableInfo fixture必须让测试到达ignored模型策略分支；不mock ContractValidator、不跳过SmartInitializingSingleton、不打开DataSource连接。
- Standards impact: `MC-ARCH-001`, `MC-REUSE-001`, `MC-DEP-001`, `MC-NAME-001`, `MC-MODEL-001`, `MC-SCOPE-001`, `MC-TEST-001`, `MC-VALID-001`；覆盖正反两种Mapper声明。
- Literal rule enforcement: Rule 2：可观察Common启动验证异常与明确错误码；Rule 3：测试EgonModel保持继承字段不重声明；Rule 11：校验通用MP契约扩展不弱化业务模型隔离。
- Implementation pseudocode:

```java
build a local EgonModel + BaseMapper SqlSessionFactory fixture with required active-read XML statements and an explicit unsupported default deleteVersionedById
configure ignoredTables with egon_cola_outbox_message; assert unmarked mapper fails with MODEL_TABLE_CANNOT_BE_IGNORED
annotate a second fixture Mapper with EgonColaExplicitTenantScopeMapper; run same startup validator and observe RED at the existing unconditional rejection
```

- Verification contribution: TEST-002/011；标记测试在GREEN实现前必须因旧验证器拒绝标记Mapper而失败。
- After this file: RED原因锁定为唯一table-ignore策略分支；普通Mapper拒绝测试继续通过。

#### File 4 — `MODIFY egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter/src/main/java/top/egon/cola/component/common/mybatis/autoconfigure/EgonColaMybatisPlusContractValidator.java`

- Purpose: 按显式Mapper标记精确放行基础设施SQL的租户策略声明，不接受无声明的模型表忽略。
- Symbols: mapper/model tenant-ignore policy check in `afterSingletonsInstantiated()`。
- Repository evidence: 当前validator在mapper循环中统一抛`MODEL_TABLE_CANNOT_BE_IGNORED`；新增注解位于同一Common MP组件，无到Outbox依赖边。
- Dependencies and consumers: `EgonColaExplicitTenantScopeMapper`和EgonColaMybatisPlusProperties.TenantId.ignoredTables；O DAO稍后加标记。
- Why now: Step 4的第4个文件；Common RED已证明缺失行为，修改一个最窄检查点。
- Contract/signature changes: table为空仍拒绝；ignored table且Mapper未标记仍拒绝；只有标记且该表确实配置ignored时继续检查其余TableInfo、模型、factory、拦截器和全局守卫；abstract Mapper方法仍必须有XML，具名default方法由其Java代码实现满足、不要求重复statement。
- Input/output and state mapping: 对普通业务Mapper零放宽；标记仅跳过`MODEL_TABLE_CANNOT_BE_IGNORED`这一表忽略断言，不增加SQL、不代替SQL tenant predicate。
- Error and edge behavior: 标记缺失时保留原稳定错误；标记不合法或模型/映射/guard不完整仍由原有验证分支失败；异常不吞。
- Standards impact: `MC-ARCH-001`, `MC-REUSE-001`, `MC-DEP-001`, `MC-SCOPE-001`, `MC-TEST-001`, `MC-VALID-001`；不关闭Common任何既有MP guard。
- Literal rule enforcement: Rule 2：保留所有Mapper/model校验；Rule 4：该Validator仍是现有Spring Bean；Rule 11：只给显式声明的基础设施Mapper可审计扩展，不引入Outbox反向依赖。
- Implementation pseudocode:

```java
if table == null -> throw MODEL_TABLE_CANNOT_BE_IGNORED
if tenantId.ignores(normalizedTable) && !mapper.isAnnotationPresent(EgonColaExplicitTenantScopeMapper.class) -> throw same error
require XML for every abstract mapper method; recognize a Java default method by reflection as its explicit Mapper implementation, then execute every other model/XML/factory/interceptor check unchanged
```

- Verification contribution: TEST-002/011；marked default-operation Mapper成为GREEN，unmarked ignored Mapper保持拒绝，abstract Mapper操作仍需XML。
- After this file: Common只接受显式标记的ignored Mapper，Outbox DAO尚未加入标记。

#### File 5 — `MODIFY egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/persistence/dao/OutboxMessageDAO.java`

- Purpose: 声明该固定技术Mapper由具名XML显式承担tenant/data-lifecycle predicate。
- Symbols: `@EgonColaExplicitTenantScopeMapper` on `OutboxMessageDAO`。
- Repository evidence: Step 3 XML中每个消息查询/变更均含`tenant_id = 0`和`deleted_at IS NULL`；所有写附带id/version以及适用owner/status。
- Dependencies and consumers: Common MP标记；仅该技术DAO使用，业务Mapper不跟随。
- Why now: Step 4的第5个文件；Common验证规则已GREEN后才标记真实具名Mapper。
- Contract/signature changes: Mapper方法签名、EgonColaMapper继承、SQL namespace和查询结果不变；唯一新增类型标记。
- Input/output and state mapping: Mapper执行不受TenantLine隐式条件重写；所有当前与后续具名SQL仍固定tenant0、active-row、version/owner条件。
- Error and edge behavior: 标记不是放宽SQL guard的理由；未来增删Mapper方法仍必须保持显式安全谓词并通过mapper XML/test与TEST-011。
- Standards impact: `MC-ARCH-001`, `MC-REUSE-001`, `MC-DEP-001`, `MC-NAME-001`, `MC-SCOPE-001`, `MC-TEST-001`, `MC-VALID-001`；跨Step路径按本Step仅修改标记这一符号。
- Literal rule enforcement: Rule 1：保留DAO语义后缀；Rule 2：保留方法验证；Rule 11：只为固定单PRIMARY技术表声明显式租户作用域，业务Mapper照常受租户隔离。
- Implementation pseudocode:

```java
import Common's EgonColaExplicitTenantScopeMapper and annotate the existing OutboxMessageDAO interface
keep every method parameter, generic, XML statement and method id byte-for-byte otherwise unchanged
static review confirms all message-table statements include tenant_id=0 and deleted_at IS NULL; TEST-011 verifies live interceptor behavior when authorized
```

- Verification contribution: TEST-002/011；Common启动合同可识别真实Outbox技术Mapper的显式租户策略。
- After this file: Outbox DAO仍走同一SqlSessionFactory，且Common模型ignored检查与具名SQL scope一致。

#### File 6 — `CREATE egon-cola-components/egon-cola-component-transactional-outbox-starter/src/test/java/top/egon/cola/component/outbox/integration/OutboxMybatisPlusIntegrationTest.java`

- Purpose: 真实边界RED：旧默认仍JDBC且技术MDC不恢复。
- Symbols: real MP/SS/PG old Store contract。
- Repository evidence: egon-cola-components/egon-cola-component-transactional-outbox-starter/src/test/java/top/egon/cola/component/outbox/integration/PostgresqlJdbcOutboxStoreIntegrationTest.java当前JDBC合同。
- Dependencies and consumers: PostgresqlOutboxTestSupport目标egon_outbox fixture，PG开关需另授权。
- Why now: Step 4的第6个文件；真实边界RED：旧默认仍JDBC且技术MDC不恢复。
- Contract/signature changes: real MP/SS/PG old Store contract；新消息/幂等冲突/PG索引/租约/clean up/业务写同事务；原22字段及公共字段。
- Input/output and state mapping: 新消息/幂等冲突/PG索引/租约/clean up/业务写同事务；原22字段及公共字段。
- Error and edge behavior: PostgresqlOutboxTestSupport目标egon_outbox fixture，PG开关需另授权；失败不吞异常、不伪造成功，适用断言由TEST-002/011验证。
- Standards impact: `MC-JSON-001`, `MC-MODEL-001`, `MC-SCOPE-001`, `MC-TEST-001`, `MC-TIME-001`, `MC-VALID-001`；本文件按当前组件约定实现并限定在本Step的路径内。
- Literal rule enforcement: Rule 2：按@Validated/@Valid、原生约束与Default/已有MP组逐层校验，非代理处调用现有ValidationUtils；Rule 3：普通class完整Lombok注解；EgonModel PO用@SuperBuilder/callSuper；MapStruct仅映射真实边界；Rule 6：Spring Boot Jackson和@EnumValue/@JsonFormat；永不以ordinal作业务编码；Rule 10：使用Instant/Duration/Clock与PG微秒，禁止Date/Calendar/SimpleDateFormat；Rule 11：保留获批的单Starter基础组件结构与MP/受管DDL/CQE边界，绝不改业务平台层次。
- Implementation pseudocode:

```java
arrange one PRIMARY NATIVE SINGLE egon_outbox, activate real Egon MP interceptors and named Mapper XML
within business transaction write business row and transactionalOutbox.enqueue; assert both committed or both rolled back, tenant MDC restored, PO tenant0/version0/deletedAt null
claim with two workers and late owner; assert one bounded UPDATE per row/owner CAS and no JDBC runtime Store bean; confirm duplicate key same fingerprint returns original receipt
```

- Verification contribution: TEST-002/011；真实边界RED：旧默认仍JDBC且技术MDC不恢复。
- After this file: 本测试定义该Step的RED断言，生产行为仍缺失；其余文件填入后GREEN。

#### File 7 — `CREATE egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/store/MybatisPlusOutboxStore.java`

- Purpose: DAO/Repo/PO/SSM齐全后替换存储事实的唯一默认实现。
- Symbols: OutboxStore complete SPI。
- Repository evidence: egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/store/PostgresqlJdbcOutboxStore.java现有Store SPI完整实现。
- Dependencies and consumers: OutboxMessageRepository/Converter/technicalContext/lifecycle/worker TransactionTemplate/metadata validator。
- Why now: Step 4的第7个文件；DAO/Repo/PO/SSM齐全后替换存储事实的唯一默认实现。
- Contract/signature changes: OutboxStore complete SPI；enqueue技术FSM输出PENDING；claim锁候选后逐行SSM+CAS；mark owner/version；cleanup bounded物理删除。
- Input/output and state mapping: enqueue技术FSM输出PENDING；claim锁候选后逐行SSM+CAS；mark owner/version；cleanup bounded物理删除。
- Error and edge behavior: OutboxMessageRepository/Converter/technicalContext/lifecycle/worker TransactionTemplate/metadata validator；失败不吞异常、不伪造成功，适用断言由TEST-002/011验证。
- Standards impact: `MC-BEAN-001`, `MC-LOG-001`, `MC-NAME-001`, `MC-PATTERN-001`, `MC-SCOPE-001`, `MC-TEST-001`, `MC-TIME-001`, `MC-VALID-001`；本文件按当前组件约定实现并限定在本Step的路径内。
- Literal rule enforcement: Rule 1：类型名按Event/BO/PO/DAO/Repository/Service/Factory等语义后缀，不新增Data/Info/Param/Bean载体；Rule 2：按@Validated/@Valid、原生约束与Default/已有MP组逐层校验，非代理处调用现有ValidationUtils；Rule 4：实际业务类用@Slf4j、显式Bean名、@RequiredArgsConstructor及每个final依赖@Qualifier；Rule 9：复杂迁移由SSM State/Strategy/Adapter实际承担，禁止Dispatcher另写状态switch；Rule 10：使用Instant/Duration/Clock与PG微秒，禁止Date/Calendar/SimpleDateFormat；Rule 11：保留获批的单Starter基础组件结构与MP/受管DDL/CQE边界，绝不改业务平台层次。
- Implementation pseudocode:

```java
@Slf4j @RequiredArgsConstructor implements OutboxStore, final qualified repo,converter,technicalContext,workerTx,lifecycle,metadataValidator; @Bean("outboxStore") in MP auto-config
under existing producer TX: technicalContext.execute(() -> mapper insert PO status=evaluate(__NEW__,ENQUEUE)); on conflict selectExistingByIdentity and compare fingerprint/identity exactly)
claimDue/claimByMessageIds in workerTx REQUIRES_NEW: selectDueForUpdate SKIP LOCKED, for each PO compute CLAIM/RECLAIM, updateClaim(id,version,source,target,owner,lease), require affected=1, reselect actual DB times; mark* lock id/status/owner→SSM→flat update affected1 or false
```

- Verification contribution: TEST-002/011；DAO/Repo/PO/SSM齐全后替换存储事实的唯一默认实现。
- After this file: 已建立 OutboxStore complete SPI，后续文件接入/验证前不得声称整个Step完成。

#### File 8 — `MODIFY egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/autoconfigure/OutboxMybatisPlusAutoConfiguration.java`

- Purpose: Step2已有DDL hook，此时增加可编译的Repo/Store绑定。
- Symbols: MapperScan, MP Store, validation Bean names。
- Repository evidence: egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/autoconfigure/TransactionalOutboxAutoConfiguration.java现有同名OutboxStore @Bean。
- Dependencies and consumers: 原TransactionalOutboxAutoConfiguration Step4将转发至此Store。
- Why now: Step 4的第8个文件；Step2已有DDL hook，此时增加可编译的Repo/Store绑定。
- Contract/signature changes: MapperScan, MP Store, validation Bean names；Mapper仅组件精确包；唯一sqlSessionFactory与logical DS/transactionManager一致；无旧Store候选。
- Input/output and state mapping: Mapper仅组件精确包；唯一sqlSessionFactory与logical DS/transactionManager一致；无旧Store候选。
- Error and edge behavior: 原TransactionalOutboxAutoConfiguration Step4将转发至此Store；失败不吞异常、不伪造成功，适用断言由TEST-002/011验证。
- Standards impact: `MC-BEAN-001`, `MC-CONFIG-001`, `MC-SCOPE-001`, `MC-TEST-001`, `MC-VALID-001`；本文件按当前组件约定实现并限定在本Step的路径内。
- Literal rule enforcement: Rule 2：按@Validated/@Valid、原生约束与Default/已有MP组逐层校验，非代理处调用现有ValidationUtils；Rule 4：实际业务类用@Slf4j、显式Bean名、@RequiredArgsConstructor及每个final依赖@Qualifier；Rule 7：配置键对全部profile保持一致，组件无生产application YAML，双语README逐项同步；Rule 11：保留获批的单Starter基础组件结构与MP/受管DDL/CQE边界，绝不改业务平台层次。
- Implementation pseudocode:

```java
retain Step2 pre-sharding DDL hook beans; @MapperScan(basePackageClasses=OutboxMessageDAO.class,sqlSessionFactoryRef="${...storage.mp.sql-session-factory-bean-name:sqlSessionFactory}")
@Bean("outboxHeadersConverter") helper; @Bean("outboxMessageConverterImpl") instantiate generated OutboxMessageConverterImpl with helper (no general component scan); register named outboxMessageDAO/repository/technicalContext/metadataValidator/outboxStore
reject missing mapped statement, logical DS != SqlSessionFactory.environment.dataSource or wrong transaction factory
require tenant ignored table only egon_cola_outbox_message and SINGLE one PRIMARY; leave all MP guards enabled; register worker TransactionTemplate propagation REQUIRES_NEW
```

- Verification contribution: TEST-002/011；Step2已有DDL hook，此时增加可编译的Repo/Store绑定。
- After this file: 已建立 MapperScan, MP Store, validation Bean names，后续文件接入/验证前不得声称整个Step完成。

#### File 9 — `MODIFY egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/autoconfigure/TransactionalOutboxAutoConfiguration.java`

- Purpose: 新auto config发布MP Store后需旧入口只消费接口。
- Symbols: remove JDBC default Store bean, reuse newStore。
- Repository evidence: egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/autoconfigure/TransactionalOutboxAutoConfiguration.java#outboxStore/outboxDispatcher。
- Dependencies and consumers: 外部直接API/annotation/Dispatcher；显式qualifier与Bean顺序。
- Why now: Step 4的第9个文件；新auto config发布MP Store后需旧入口只消费接口。
- Contract/signature changes: 按主Spec §14.4 保持 MP 配置先于本配置、本配置先于状态机配置；不声明 TransactionalOutboxAutoConfiguration after OutboxStateMachineAutoConfiguration，避免与状态机已有的 after 依赖形成环。移除 JDBC default Store bean；OutboxStore/TransactionalOutbox签名不变；旧schema-validator改为新metadata；不产生双Store。此处是用户批准的 Step 4 File 5 顺序澄清。
- Input/output and state mapping: MP 配置预先注册具名 `outboxStore`；本配置保留原有 DataSource/Transaction auto-config 顺序、只按接口注入 Store；状态机配置仍排在本配置之后，其生命周期依赖在 Bean 实例创建时解析。无 JDBC fallback。
- Error and edge behavior: 外部直接API/annotation/Dispatcher；显式qualifier与Bean顺序；失败不吞异常、不伪造成功，适用断言由TEST-002/011验证。
- Standards impact: `MC-BEAN-001`, `MC-SCOPE-001`, `MC-TEST-001`, `MC-VALID-001`；本文件按当前组件约定实现并限定在本Step的路径内。
- Literal rule enforcement: Rule 2：按@Validated/@Valid、原生约束与Default/已有MP组逐层校验，非代理处调用现有ValidationUtils；Rule 4：实际业务类用@Slf4j、显式Bean名、@RequiredArgsConstructor及每个final依赖@Qualifier；Rule 11：保留获批的单Starter基础组件结构与MP/受管DDL/CQE边界，绝不改业务平台层次。
- Implementation pseudocode:

```java
preserve @AutoConfiguration(after=ValidationAutoConfiguration.class, afterName={DataSourceAutoConfiguration,JdbcTemplateAutoConfiguration,TransactionAutoConfiguration}); OutboxMybatisPlusAutoConfiguration remains before Common sharding/DataSource and therefore before this config; do not order this config after OutboxStateMachineAutoConfiguration
remove old PostgresqlJdbcOutboxStore bean and JDBC-specific template constructor usage; inject @Qualifier("outboxStore") OutboxStore into DefaultTransactionalOutbox/Dispatcher; let OutboxMybatisPlusAutoConfiguration register the only default Store bean
keep OutboxTransactionGuard selected logical DataSource and original enqueue/afterCommit ordering; lifecycle bean dependencies resolve after all auto-config definitions are registered; absent MP prerequisites fail startup
```

- Verification contribution: TEST-002/011；新auto config发布MP Store后需旧入口只消费接口。
- After this file: 已建立 remove JDBC default Store bean, reuse newStore，后续文件接入/验证前不得声称整个Step完成。

#### File 10 — `MODIFY egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`

- Purpose: Step4新增配置可导出，Step6再追加业务适配条目。
- Symbols: three new auto-config names, first core/MP。
- Repository evidence: egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports现有四行。
- Dependencies and consumers: Spring Boot imports现有四个Bean；重复/错序需context测试。
- Why now: Step 4的第10个文件；Step4新增配置可导出，Step6再追加业务适配条目。
- Contract/signature changes: three new auto-config names, first core/MP；加入OutboxStateMachineAutoConfiguration和OutboxMybatisPlusAutoConfiguration；业务尚不启用。
- Input/output and state mapping: 加入OutboxStateMachineAutoConfiguration和OutboxMybatisPlusAutoConfiguration；业务尚不启用。
- Error and edge behavior: Spring Boot imports现有四个Bean；重复/错序需context测试；失败不吞异常、不伪造成功，适用断言由TEST-002/011验证。
- Standards impact: `MC-CONFIG-001`, `MC-SCOPE-001`, `MC-TEST-001`；本文件按当前组件约定实现并限定在本Step的路径内。
- Literal rule enforcement: Rule 7：配置键对全部profile保持一致，组件无生产application YAML，双语README逐项同步；Rule 11：保留获批的单Starter基础组件结构与MP/受管DDL/CQE边界，绝不改业务平台层次。
- Implementation pseudocode:

```text
retain OutboxMetricsAutoConfiguration, TransactionalOutboxAutoConfiguration, OutboxHttpAutoConfiguration, OutboxRabbitAutoConfiguration lines
append OutboxStateMachineAutoConfiguration and OutboxMybatisPlusAutoConfiguration exactly once; their @AutoConfiguration before/after owns order
assert no business autoConfig entry before Step6 code exists; test default startup failure when MP absent
```

- Verification contribution: TEST-002/011；Step4新增配置可导出，Step6再追加业务适配条目。
- After this file: 已建立 three new auto-config names, first core/MP，后续文件接入/验证前不得声称整个Step完成。

#### File 11 — `MODIFY egon-cola-components/egon-cola-component-transactional-outbox-starter/src/test/java/top/egon/cola/component/outbox/autoconfigure/TransactionalOutboxAutoConfigurationTest.java`

- Purpose: 删除旧具体Bean后原ContextRunner断言需对应MP前置。
- Symbols: default Store context test adjustments。
- Repository evidence: egon-cola-components/egon-cola-component-transactional-outbox-starter/src/test/java/top/egon/cola/component/outbox/autoconfigure/TransactionalOutboxAutoConfigurationTest.java当前mockStore路径。
- Dependencies and consumers: 与Step4新OutboxMybatisPlusAutoConfigurationTest协同，不重复业务逻辑。
- Why now: Step 4的第11个文件；删除旧具体Bean后原ContextRunner断言需对应MP前置。
- Contract/signature changes: default Store context test adjustments；无DS仍退场；有唯一真实MP依赖时仅一个Store；旧自定义Store边界仍明确。
- Input/output and state mapping: 无DS仍退场；有唯一真实MP依赖时仅一个Store；旧自定义Store边界仍明确。
- Error and edge behavior: 与Step4新OutboxMybatisPlusAutoConfigurationTest协同，不重复业务逻辑；失败不吞异常、不伪造成功，适用断言由TEST-002/011验证。
- Standards impact: `MC-BEAN-001`, `MC-SCOPE-001`, `MC-TEST-001`, `MC-VALID-001`；本文件按当前组件约定实现并限定在本Step的路径内。
- Literal rule enforcement: Rule 2：按@Validated/@Valid、原生约束与Default/已有MP组逐层校验，非代理处调用现有ValidationUtils；Rule 4：实际业务类用@Slf4j、显式Bean名、@RequiredArgsConstructor及每个final依赖@Qualifier；Rule 11：保留获批的单Starter基础组件结构与MP/受管DDL/CQE边界，绝不改业务平台层次。
- Implementation pseudocode:

```java
configuredContext registers matching logical DataSource, DataSourceTransactionManager and SqlSessionFactory/Mapper evidence
assert one TransactionalOutbox, one MybatisPlusOutboxStore, default Dispatcher, no JDBC Store
custom OutboxStore must satisfy documented MP/SSM contract; no unavailable SqlSessionFactory yields explicit fail instead of mocked JDBC success
```

- Verification contribution: TEST-002/011；删除旧具体Bean后原ContextRunner断言需对应MP前置。
- After this file: 已建立 default Store context test adjustments，后续文件接入/验证前不得声称整个Step完成。

#### File 12 — `MODIFY egon-cola-components/egon-cola-component-transactional-outbox-starter/src/test/java/top/egon/cola/component/outbox/store/OutboxLongPrimaryKeyTest.java`

- Purpose: 旧测试捕获JDBC占位符；现在以MP PO/Mapper绑定证明。
- Symbols: Snowflake/identity unchanged, no JDBC parameter-order test。
- Repository evidence: egon-cola-components/egon-cola-component-transactional-outbox-starter/src/test/java/top/egon/cola/component/outbox/store/OutboxLongPrimaryKeyTest.java当前13个JDBC参数断言。
- Dependencies and consumers: 组件ID starter、OutboxMessagePO、insertMessage XML；不直接new旧Store。
- Why now: Step 4的第12个文件；旧测试捕获JDBC占位符；现在以MP PO/Mapper绑定证明。
- Contract/signature changes: Snowflake/identity unchanged, no JDBC parameter-order test；新消息Long id正数且机器segment不变；旧SQL1字节原样。
- Input/output and state mapping: 新消息Long id正数且机器segment不变；旧SQL1字节原样。
- Error and edge behavior: 组件ID starter、OutboxMessagePO、insertMessage XML；不直接new旧Store；失败不吞异常、不伪造成功，适用断言由TEST-002/011验证。
- Standards impact: `MC-MODEL-001`, `MC-SCOPE-001`, `MC-TEST-001`, `MC-VALID-001`；本文件按当前组件约定实现并限定在本Step的路径内。
- Literal rule enforcement: Rule 2：按@Validated/@Valid、原生约束与Default/已有MP组逐层校验，非代理处调用现有ValidationUtils；Rule 3：普通class完整Lombok注解；EgonModel PO用@SuperBuilder/callSuper；MapStruct仅映射真实边界；Rule 11：保留获批的单Starter基础组件结构与MP/受管DDL/CQE边界，绝不改业务平台层次。
- Implementation pseudocode:

```java
with real/recording MP mapper capture OutboxMessagePO.id on two enqueue; assert both Long positive and monotonic with expected machine segment
assert same messageId/idempotency conflict returns original OutboxReceipt and does not regenerate original row id
read ClassPathResource old V1 SQL and assert original identity text unchanged; remove JDBC placeholder-count fixture only
```

- Verification contribution: TEST-002/011；旧测试捕获JDBC占位符；现在以MP PO/Mapper绑定证明。
- After this file: 已建立 Snowflake/identity unchanged, no JDBC parameter-order test，后续文件接入/验证前不得声称整个Step完成。

#### File 13 — `MODIFY egon-cola-components/egon-cola-component-transactional-outbox-starter/src/test/java/top/egon/cola/component/outbox/contract/ComponentContractGovernanceTest.java`

- Purpose: 删除JDBC具体类前先删静态符号引用，并同步此前已批准的状态机/元数据类型治理清单。
- Symbols: drop stale concrete JDBC contract; keep public SPI; update enum/exception/validator inventory。
- Repository evidence: egon-cola-components/egon-cola-component-transactional-outbox-starter/src/test/java/top/egon/cola/component/outbox/contract/ComponentContractGovernanceTest.java引用旧Store类。
- Dependencies and consumers: Step8同文件追加新Bean/CQE禁止规则；不删除现有有效检查。
- Why now: Step 4的第13个文件；先移除旧Store结构依赖，再用治理测试覆盖当前已实现的MP/SSM类型。
- Contract/signature changes: drop stale concrete JDBC contract; keep public SPI；OutboxStore方法签名/异常/旧SQL1指纹不变；登记Step1 `OutboxStateMachineException`和第四个手写enum；`OutboxSchemaMetadataValidator`必须进入BaseValidator清单。
- Input/output and state mapping: 显式计数PENDING...DEAD与LifecycleSignal enum；保留状态码不变；元数据验证器构造注入同一公共ValidationUtils并继承BaseValidator。
- Error and edge behavior: Step8同文件追加新Bean/CQE禁止规则；不删除现有有效检查；失败不吞异常、不伪造成功，适用断言由TEST-002/011验证。
- Standards impact: `MC-MODEL-001`, `MC-NAME-001`, `MC-SCOPE-001`, `MC-TEST-001`, `MC-VALID-001`；本文件按当前组件约定实现并限定在本Step的路径内。
- Literal rule enforcement: Rule 1：类型名按Event/BO/PO/DAO/Repository/Service/Factory等语义后缀，不新增Data/Info/Param/Bean载体；Rule 2：Validator继承BaseValidator并使用既有ValidationUtils；Rule 3：普通class完整Lombok注解；EgonModel PO用@SuperBuilder/callSuper；MapStruct仅映射真实边界；Rule 11：保留获批的单Starter基础组件结构与MP/受管DDL/CQE边界，绝不改业务平台层次。
- Implementation pseudocode:

```java
replace literal PostgresqlJdbcOutboxStore structural check with MybatisPlusOutboxStore implements OutboxStore check
add OutboxStateMachineException special reason/retryability assertions; update enum inventory for the lifecycle signal and persisted OutboxStatus
add OutboxSchemaMetadataValidator to the BaseValidator inventory; assert OutboxRecord/Receipt/Message fields and old SQL1 bytes remain unchanged
```

- Verification contribution: TEST-002/011；删除旧具体类引用并保持公共SPI/异常/枚举/验证器公共合同。
- After this file: governance expectations cover current Step1/3 additions; the following validator update closes its BaseValidator contract.

#### File 14 — `MODIFY egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/persistence/OutboxSchemaMetadataValidator.java`

- Purpose: 让Step3新增的元数据validator遵守当前Common BaseValidator handoff合同。
- Symbols: `extends BaseValidator`; `getValidationUtils()`。
- Repository evidence: `OutboxSchemaMetadataValidator` already injects qualified `ValidationUtils` and calls it on the managed manifest, but does not inherit BaseValidator; File13 RED now detects this。
- Dependencies and consumers: `top.egon.cola.component.common.core.validation.BaseValidator`; existing `ValidationUtils` field and bean wiring remain unchanged。
- Why now: Step 4的第14个文件；先更新governance RED，再做无业务行为变化的继承补齐。
- Contract/signature changes: class extends BaseValidator and returns its existing injected validationUtils from protected `getValidationUtils()`; public `validate()` signature and metadata queries do not change。
- Input/output and state mapping: Manifest ValidationUtils calls and SQL metadata checks produce the same values/results; no DML and no second validation utility。
- Error and edge behavior: Common validation failures keep their original exception path; SQL/IOException wrapping and physical connection ownership remain unchanged。
- Standards impact: `MC-ARCH-001`, `MC-REUSE-001`, `MC-DEP-001`, `MC-BEAN-001`, `MC-NAME-001`, `MC-SCOPE-001`, `MC-TEST-001`, `MC-VALID-001`；本文件按当前组件约定实现并限定在本Step的路径内。
- Literal rule enforcement: Rule 1：保留Validator语义后缀；Rule 2：继承BaseValidator并复用公共ValidationUtils；Rule 3：普通class和@RequiredArgsConstructor保持既有模型注解合同；Rule 4：Spring Bean依赖仍final+Qualifier；Rule 11：只调整基础设施validator公共校验接入，不改业务层。
- Implementation pseudocode:

```java
public class OutboxSchemaMetadataValidator extends BaseValidator
keep the existing final qualified ValidationUtils validationUtils field and implement getValidationUtils() to return it
rerun ComponentContractGovernanceTest; preserve validate(), manifest decoding, SQL metadata checks, and read-only physical connection behavior
```

- Verification contribution: TEST-002/011；Common治理测试从旧validator清单RED转为完整pass。
- After this file: 元数据validator符合BaseValidator治理合同；原OutboxSchemaValidator委托语义不变。

#### File 15 — `MODIFY egon-cola-components/egon-cola-component-transactional-outbox-starter/src/test/java/top/egon/cola/component/outbox/integration/OutboxDataSafetyIntegrationTest.java`

- Purpose: 当前直接构造PostgresqlJdbcOutboxStore，删除前转到MP或接口fixture。
- Symbols: data safety old-store fixture。
- Repository evidence: egon-cola-components/egon-cola-component-transactional-outbox-starter/src/test/java/top/egon/cola/component/outbox/integration/OutboxDataSafetyIntegrationTest.java旧类引用由rg确认。
- Dependencies and consumers: 现有测试已在该文件；新MP Store / TestSupport替代旧构造。
- Why now: Step 4的第14个文件；当前直接构造PostgresqlJdbcOutboxStore，删除前转到MP或接口fixture。
- Contract/signature changes: data safety old-store fixture；原事件/查询输入和预期输出不变；bad payload/headers never committed; secure errors omit raw secrets。
- Input/output and state mapping: 原事件/查询输入和预期输出不变；bad payload/headers never committed; secure errors omit raw secrets。
- Error and edge behavior: 现有测试已在该文件；新MP Store / TestSupport替代旧构造；失败不吞异常、不伪造成功，适用断言由TEST-002/011验证。
- Standards impact: `MC-SCOPE-001`, `MC-TEST-001`, `MC-VALID-001`；本文件按当前组件约定实现并限定在本Step的路径内。
- Literal rule enforcement: Rule 2：按@Validated/@Valid、原生约束与Default/已有MP组逐层校验，非代理处调用现有ValidationUtils；Rule 11：保留获批的单Starter基础组件结构与MP/受管DDL/CQE边界，绝不改业务平台层次。
- Implementation pseudocode:

```java
replace new PostgresqlJdbcOutboxStore(jdbc,...) with shared MybatisPlusOutboxStore/OutboxStore test fixture and qualified MP collaborators
exercise original data safety scenario against target egon_outbox; assert bad payload/headers never committed; secure errors omit raw secrets
assert old test expectation no longer depends on JDBC SQL placeholders; require actual MP path in PG variant
```

- Verification contribution: TEST-002/011；当前直接构造PostgresqlJdbcOutboxStore，删除前转到MP或接口fixture。
- After this file: 已建立 data safety old-store fixture，后续文件接入/验证前不得声称整个Step完成。

#### File 16 — `MODIFY egon-cola-components/egon-cola-component-transactional-outbox-starter/src/test/java/top/egon/cola/component/outbox/integration/PostgresqlOutboxCleanupIntegrationTest.java`

- Purpose: 当前直接构造PostgresqlJdbcOutboxStore，删除前转到MP或接口fixture。
- Symbols: retention cleanup old-store fixture。
- Repository evidence: egon-cola-components/egon-cola-component-transactional-outbox-starter/src/test/java/top/egon/cola/component/outbox/integration/PostgresqlOutboxCleanupIntegrationTest.java旧类引用由rg确认。
- Dependencies and consumers: 现有测试已在该文件；新MP Store / TestSupport替代旧构造。
- Why now: Step 4的第16个文件；当前直接构造PostgresqlJdbcOutboxStore，删除前转到MP或接口fixture。
- Contract/signature changes: retention cleanup old-store fixture；原事件/查询输入和预期输出不变；bounded SUCCEEDED-only physical cleanup; DEAD/PENDING remain。
- Input/output and state mapping: 原事件/查询输入和预期输出不变；bounded SUCCEEDED-only physical cleanup; DEAD/PENDING remain。
- Error and edge behavior: 现有测试已在该文件；新MP Store / TestSupport替代旧构造；失败不吞异常、不伪造成功，适用断言由TEST-002/011验证。
- Standards impact: `MC-SCOPE-001`, `MC-TEST-001`, `MC-VALID-001`；本文件按当前组件约定实现并限定在本Step的路径内。
- Literal rule enforcement: Rule 2：按@Validated/@Valid、原生约束与Default/已有MP组逐层校验，非代理处调用现有ValidationUtils；Rule 11：保留获批的单Starter基础组件结构与MP/受管DDL/CQE边界，绝不改业务平台层次。
- Implementation pseudocode:

```java
replace new PostgresqlJdbcOutboxStore(jdbc,...) with shared MybatisPlusOutboxStore/OutboxStore test fixture and qualified MP collaborators
exercise original retention cleanup scenario against target egon_outbox; assert bounded SUCCEEDED-only physical cleanup; DEAD/PENDING remain
assert old test expectation no longer depends on JDBC SQL placeholders; require actual MP path in PG variant
```

- Verification contribution: TEST-002/011；当前直接构造PostgresqlJdbcOutboxStore，删除前转到MP或接口fixture。
- After this file: 已建立 retention cleanup old-store fixture，后续文件接入/验证前不得声称整个Step完成。

#### File 17 — `MODIFY egon-cola-components/egon-cola-component-transactional-outbox-starter/src/test/java/top/egon/cola/component/outbox/integration/PostgresqlOutboxConcurrencyIntegrationTest.java`

- Purpose: 当前直接构造PostgresqlJdbcOutboxStore，删除前转到MP或接口fixture。
- Symbols: two-worker claim old-store fixture。
- Repository evidence: egon-cola-components/egon-cola-component-transactional-outbox-starter/src/test/java/top/egon/cola/component/outbox/integration/PostgresqlOutboxConcurrencyIntegrationTest.java旧类引用由rg确认。
- Dependencies and consumers: 现有测试已在该文件；新MP Store / TestSupport替代旧构造。
- Why now: Step 4的第17个文件；当前直接构造PostgresqlJdbcOutboxStore，删除前转到MP或接口fixture。
- Contract/signature changes: two-worker claim old-store fixture；原事件/查询输入和预期输出不变；FOR UPDATE SKIP LOCKED plus owner/version CAS with no duplicate normal lease。
- Input/output and state mapping: 原事件/查询输入和预期输出不变；FOR UPDATE SKIP LOCKED plus owner/version CAS with no duplicate normal lease。
- Error and edge behavior: 现有测试已在该文件；新MP Store / TestSupport替代旧构造；失败不吞异常、不伪造成功，适用断言由TEST-002/011验证。
- Standards impact: `MC-SCOPE-001`, `MC-TEST-001`, `MC-VALID-001`；本文件按当前组件约定实现并限定在本Step的路径内。
- Literal rule enforcement: Rule 2：按@Validated/@Valid、原生约束与Default/已有MP组逐层校验，非代理处调用现有ValidationUtils；Rule 11：保留获批的单Starter基础组件结构与MP/受管DDL/CQE边界，绝不改业务平台层次。
- Implementation pseudocode:

```java
replace new PostgresqlJdbcOutboxStore(jdbc,...) with shared MybatisPlusOutboxStore/OutboxStore test fixture and qualified MP collaborators
exercise original two-worker claim scenario against target egon_outbox; assert FOR UPDATE SKIP LOCKED plus owner/version CAS with no duplicate normal lease
assert old test expectation no longer depends on JDBC SQL placeholders; require actual MP path in PG variant
```

- Verification contribution: TEST-002/011；当前直接构造PostgresqlJdbcOutboxStore，删除前转到MP或接口fixture。
- After this file: 已建立 two-worker claim old-store fixture，后续文件接入/验证前不得声称整个Step完成。

#### File 18 — `MODIFY egon-cola-components/egon-cola-component-transactional-outbox-starter/src/test/java/top/egon/cola/component/outbox/integration/PostgresqlOutboxQueryPlanIntegrationTest.java`

- Purpose: 当前直接构造PostgresqlJdbcOutboxStore，删除前转到MP或接口fixture。
- Symbols: claim/cleanup EXPLAIN old-store fixture。
- Repository evidence: egon-cola-components/egon-cola-component-transactional-outbox-starter/src/test/java/top/egon/cola/component/outbox/integration/PostgresqlOutboxQueryPlanIntegrationTest.java旧类引用由rg确认。
- Dependencies and consumers: 现有测试已在该文件；新MP Store / TestSupport替代旧构造。
- Why now: Step 4的第18个文件；当前直接构造PostgresqlJdbcOutboxStore，删除前转到MP或接口fixture。
- Contract/signature changes: claim/cleanup EXPLAIN old-store fixture；原事件/查询输入和预期输出不变；target egon_outbox indexes chosen or measured sort recorded, not assumed。
- Input/output and state mapping: 原事件/查询输入和预期输出不变；target egon_outbox indexes chosen or measured sort recorded, not assumed。
- Error and edge behavior: 现有测试已在该文件；新MP Store / TestSupport替代旧构造；失败不吞异常、不伪造成功，适用断言由TEST-002/011验证。
- Standards impact: `MC-SCOPE-001`, `MC-TEST-001`, `MC-VALID-001`；本文件按当前组件约定实现并限定在本Step的路径内。
- Literal rule enforcement: Rule 2：按@Validated/@Valid、原生约束与Default/已有MP组逐层校验，非代理处调用现有ValidationUtils；Rule 11：保留获批的单Starter基础组件结构与MP/受管DDL/CQE边界，绝不改业务平台层次。
- Implementation pseudocode:

```java
replace new PostgresqlJdbcOutboxStore(jdbc,...) with shared MybatisPlusOutboxStore/OutboxStore test fixture and qualified MP collaborators
exercise original claim/cleanup EXPLAIN scenario against target egon_outbox; assert target egon_outbox indexes chosen or measured sort recorded, not assumed
assert old test expectation no longer depends on JDBC SQL placeholders; require actual MP path in PG variant
```

- Verification contribution: TEST-002/011；当前直接构造PostgresqlJdbcOutboxStore，删除前转到MP或接口fixture。
- After this file: 已建立 claim/cleanup EXPLAIN old-store fixture，后续文件接入/验证前不得声称整个Step完成。

#### File 19 — `MODIFY egon-cola-components/egon-cola-component-transactional-outbox-starter/src/test/java/top/egon/cola/component/outbox/integration/PostgresqlOutboxRecoveryIntegrationTest.java`

- Purpose: 当前直接构造PostgresqlJdbcOutboxStore，删除前转到MP或接口fixture。
- Symbols: expired lease reclaim old-store fixture。
- Repository evidence: egon-cola-components/egon-cola-component-transactional-outbox-starter/src/test/java/top/egon/cola/component/outbox/integration/PostgresqlOutboxRecoveryIntegrationTest.java旧类引用由rg确认。
- Dependencies and consumers: 现有测试已在该文件；新MP Store / TestSupport替代旧构造。
- Why now: Step 4的第19个文件；当前直接构造PostgresqlJdbcOutboxStore，删除前转到MP或接口fixture。
- Contract/signature changes: expired lease reclaim old-store fixture；原事件/查询输入和预期输出不变；PROCESSING expired increments attempt/new owner; delayed old owner loses。
- Input/output and state mapping: 原事件/查询输入和预期输出不变；PROCESSING expired increments attempt/new owner; delayed old owner loses。
- Error and edge behavior: 现有测试已在该文件；新MP Store / TestSupport替代旧构造；失败不吞异常、不伪造成功，适用断言由TEST-002/011验证。
- Standards impact: `MC-SCOPE-001`, `MC-TEST-001`, `MC-VALID-001`；本文件按当前组件约定实现并限定在本Step的路径内。
- Literal rule enforcement: Rule 2：按@Validated/@Valid、原生约束与Default/已有MP组逐层校验，非代理处调用现有ValidationUtils；Rule 11：保留获批的单Starter基础组件结构与MP/受管DDL/CQE边界，绝不改业务平台层次。
- Implementation pseudocode:

```java
replace new PostgresqlJdbcOutboxStore(jdbc,...) with shared MybatisPlusOutboxStore/OutboxStore test fixture and qualified MP collaborators
exercise original expired lease reclaim scenario against target egon_outbox; assert PROCESSING expired increments attempt/new owner; delayed old owner loses
assert old test expectation no longer depends on JDBC SQL placeholders; require actual MP path in PG variant
```

- Verification contribution: TEST-002/011；当前直接构造PostgresqlJdbcOutboxStore，删除前转到MP或接口fixture。
- After this file: 已建立 expired lease reclaim old-store fixture，后续文件接入/验证前不得声称整个Step完成。

#### File 20 — `MODIFY egon-cola-components/egon-cola-component-transactional-outbox-starter/src/test/java/top/egon/cola/component/outbox/integration/TransactionalOutboxTransactionIntegrationTest.java`

- Purpose: 当前直接构造PostgresqlJdbcOutboxStore，删除前转到MP或接口fixture。
- Symbols: producer atomicity old-store fixture。
- Repository evidence: egon-cola-components/egon-cola-component-transactional-outbox-starter/src/test/java/top/egon/cola/component/outbox/integration/TransactionalOutboxTransactionIntegrationTest.java旧类引用由rg确认。
- Dependencies and consumers: 现有测试已在该文件；新MP Store / TestSupport替代旧构造。
- Why now: Step 4的第20个文件；当前直接构造PostgresqlJdbcOutboxStore，删除前转到MP或接口fixture。
- Contract/signature changes: producer atomicity old-store fixture；原事件/查询输入和预期输出不变；business+outbox share Spring TX; rollback and wrong datasource fail。
- Input/output and state mapping: 原事件/查询输入和预期输出不变；business+outbox share Spring TX; rollback and wrong datasource fail。
- Error and edge behavior: 现有测试已在该文件；新MP Store / TestSupport替代旧构造；失败不吞异常、不伪造成功，适用断言由TEST-002/011验证。
- Standards impact: `MC-SCOPE-001`, `MC-TEST-001`, `MC-VALID-001`；本文件按当前组件约定实现并限定在本Step的路径内。
- Literal rule enforcement: Rule 2：按@Validated/@Valid、原生约束与Default/已有MP组逐层校验，非代理处调用现有ValidationUtils；Rule 11：保留获批的单Starter基础组件结构与MP/受管DDL/CQE边界，绝不改业务平台层次。
- Implementation pseudocode:

```java
replace new PostgresqlJdbcOutboxStore(jdbc,...) with shared MybatisPlusOutboxStore/OutboxStore test fixture and qualified MP collaborators
exercise original producer atomicity scenario against target egon_outbox; assert business+outbox share Spring TX; rollback and wrong datasource fail
assert old test expectation no longer depends on JDBC SQL placeholders; require actual MP path in PG variant
```

- Verification contribution: TEST-002/011；当前直接构造PostgresqlJdbcOutboxStore，删除前转到MP或接口fixture。
- After this file: 已建立 producer atomicity old-store fixture，后续文件接入/验证前不得声称整个Step完成。

#### File 21 — `MODIFY egon-cola-components/egon-cola-component-transactional-outbox-starter/src/test/java/top/egon/cola/component/outbox/transaction/DefaultTransactionalOutboxTest.java`

- Purpose: 当前直接构造PostgresqlJdbcOutboxStore，删除前转到MP或接口fixture。
- Symbols: enqueue unit old-store fixture。
- Repository evidence: egon-cola-components/egon-cola-component-transactional-outbox-starter/src/test/java/top/egon/cola/component/outbox/transaction/DefaultTransactionalOutboxTest.java旧类引用由rg确认。
- Dependencies and consumers: 现有测试已在该文件；新MP Store / TestSupport替代旧构造。
- Why now: Step 4的第21个文件；当前直接构造PostgresqlJdbcOutboxStore，删除前转到MP或接口fixture。
- Contract/signature changes: enqueue unit old-store fixture；原事件/查询输入和预期输出不变；DefaultTransactionalOutbox still validates/idempotency/afterCommit using mocked OutboxStore。
- Input/output and state mapping: 原事件/查询输入和预期输出不变；DefaultTransactionalOutbox still validates/idempotency/afterCommit using mocked OutboxStore。
- Error and edge behavior: 现有测试已在该文件；新MP Store / TestSupport替代旧构造；失败不吞异常、不伪造成功，适用断言由TEST-002/011验证。
- Standards impact: `MC-SCOPE-001`, `MC-TEST-001`, `MC-VALID-001`；本文件按当前组件约定实现并限定在本Step的路径内。
- Literal rule enforcement: Rule 2：按@Validated/@Valid、原生约束与Default/已有MP组逐层校验，非代理处调用现有ValidationUtils；Rule 11：保留获批的单Starter基础组件结构与MP/受管DDL/CQE边界，绝不改业务平台层次。
- Implementation pseudocode:

```java
replace new PostgresqlJdbcOutboxStore(jdbc,...) with shared MybatisPlusOutboxStore/OutboxStore test fixture and qualified MP collaborators
exercise original enqueue unit scenario against target egon_outbox; assert DefaultTransactionalOutbox still validates/idempotency/afterCommit using mocked OutboxStore
assert old test expectation no longer depends on JDBC SQL placeholders; require actual MP path in PG variant
```

- Verification contribution: TEST-002/011；当前直接构造PostgresqlJdbcOutboxStore，删除前转到MP或接口fixture。
- After this file: 已建立 enqueue unit old-store fixture，后续文件接入/验证前不得声称整个Step完成。

#### File 22 — `MODIFY egon-cola-components/egon-cola-component-transactional-outbox-starter/src/test/java/top/egon/cola/component/outbox/integration/PostgresqlOutboxTestSupport.java`

- Purpose: 旧集成测试共享fixture，先迁移到NATIVE单PRIMARY目标。
- Symbols: one PG fixture for old source and new target。
- Repository evidence: egon-cola-components/egon-cola-component-transactional-outbox-starter/src/test/java/top/egon/cola/component/outbox/integration/PostgresqlOutboxTestSupport.java当前只有旧V1 JdbcTemplate。
- Dependencies and consumers: 全部现有HTTP/Rabbit/transaction PG集成测试继承此基类。
- Why now: Step 4的第22个文件；旧集成测试共享fixture，先迁移到NATIVE单PRIMARY目标。
- Contract/signature changes: one PG fixture for old source and new target；默认测试目标egon_outbox/DDL history；迁移测试另保留旧schema/V1只读来源fixture。
- Input/output and state mapping: 默认测试目标egon_outbox/DDL history；迁移测试另保留旧schema/V1只读来源fixture。
- Error and edge behavior: 全部现有HTTP/Rabbit/transaction PG集成测试继承此基类；失败不吞异常、不伪造成功，适用断言由TEST-002/011验证。
- Standards impact: `MC-CONFIG-001`, `MC-SCOPE-001`, `MC-TEST-001`, `MC-VALID-001`；本文件按当前组件约定实现并限定在本Step的路径内。
- Literal rule enforcement: Rule 2：按@Validated/@Valid、原生约束与Default/已有MP组逐层校验，非代理处调用现有ValidationUtils；Rule 7：配置键对全部profile保持一致，组件无生产application YAML，双语README逐项同步；Rule 11：保留获批的单Starter基础组件结构与MP/受管DDL/CQE边界，绝不改业务平台层次。
- Implementation pseudocode:

```java
initialize test PostgreSQL only when EGON_OUTBOX_TEST_POSTGRES_ENABLED=true and explicit user runtime permission exists
construct NATIVE single PRIMARY logical DataSource, run public EgonColaPostgreDdlRunner with SQL2/manifest, real SqlSessionFactory/MP guards
provide OutboxStore fixture from Spring context; old legacy schema loaded separately only for migration test; report skipped tests as not executed
```

- Verification contribution: TEST-002/011；旧集成测试共享fixture，先迁移到NATIVE单PRIMARY目标。
- After this file: 已建立 one PG fixture for old source and new target，后续文件接入/验证前不得声称整个Step完成。

#### File 23 — `DELETE egon-cola-components/egon-cola-component-transactional-outbox-starter/src/test/java/top/egon/cola/component/outbox/integration/PostgresqlJdbcOutboxStoreIntegrationTest.java`

- Purpose: 新OutboxMybatisPlusIntegrationTest已覆盖旧Store公开断言。
- Symbols: old concrete integration test class。
- Repository evidence: egon-cola-components/egon-cola-component-transactional-outbox-starter/src/test/java/top/egon/cola/component/outbox/integration/PostgresqlJdbcOutboxStoreIntegrationTest.java旧具体测试。
- Dependencies and consumers: 现有测试引用旧类会在Store删除后破坏编译，已先迁至新测试。
- Why now: Step 4的第23个文件；新OutboxMybatisPlusIntegrationTest已覆盖旧Store公开断言。
- Contract/signature changes: old concrete integration test class；删除旧类名/旧构造器测试，不删除覆盖内容；旧SQL1仍由OutboxMigrationContractTest保留。
- Input/output and state mapping: 删除旧类名/旧构造器测试，不删除覆盖内容；旧SQL1仍由OutboxMigrationContractTest保留。
- Error and edge behavior: 现有测试引用旧类会在Store删除后破坏编译，已先迁至新测试；失败不吞异常、不伪造成功，适用断言由TEST-002/011验证。
- Standards impact: `MC-SCOPE-001`, `MC-TEST-001`；本文件按当前组件约定实现并限定在本Step的路径内。
- Literal rule enforcement: Rule 11：保留获批的单Starter基础组件结构与MP/受管DDL/CQE边界，绝不改业务平台层次。
- Implementation pseudocode:

```java
before deletion list old test method names/assertions and map each to OutboxMybatisPlusIntegrationTest or retained integration case
remove this obsolete JDBC-concrete test path only after new target test contains enqueue/claim/retry/dead/conflict/schema assertions
search rg PostgresqlJdbcOutboxStore across O src/test; any remaining reference blocks next DELETE
```

- Verification contribution: TEST-002/011；新OutboxMybatisPlusIntegrationTest已覆盖旧Store公开断言。
- After this file: 已建立 old concrete integration test class，后续文件接入/验证前不得声称整个Step完成。

#### File 24 — `DELETE egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/store/PostgresqlJdbcOutboxStore.java`

- Purpose: 所有已知直接调用已先转到MP Store，再删旧具体类。
- Symbols: old runtime JDBC persistence implementation。
- Repository evidence: egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/store/PostgresqlJdbcOutboxStore.java当前所有JDBC CRUD。
- Dependencies and consumers: 旧自动配置/test文件已全部去引用；外部具体类调用需要迁移说明。
- Why now: Step 4的第24个文件；所有已知直接调用已先转到MP Store，再删旧具体类。
- Contract/signature changes: old runtime JDBC persistence implementation；OutboxStore公共接口仍实现；旧构造器不作假兼容；SQL1迁移来源保留。
- Input/output and state mapping: OutboxStore公共接口仍实现；旧构造器不作假兼容；SQL1迁移来源保留。
- Error and edge behavior: 旧自动配置/test文件已全部去引用；外部具体类调用需要迁移说明；失败不吞异常、不伪造成功，适用断言由TEST-002/011验证。
- Standards impact: `MC-SCOPE-001`, `MC-TEST-001`；本文件按当前组件约定实现并限定在本Step的路径内。
- Literal rule enforcement: Rule 11：保留获批的单Starter基础组件结构与MP/受管DDL/CQE边界，绝不改业务平台层次。
- Implementation pseudocode:

```java
rg -n 'PostgresqlJdbcOutboxStore' O/src/main O/src/test returns no remaining source symbol before delete
remove old concrete file; do not delete OutboxStore/NewOutboxRecord/OutboxRecord/SQL1 or old event delivery protocol
compile -pl O -am and assert auto-config publishes only MybatisPlusOutboxStore; no JDBC runtime fallback class exists
```

- Verification contribution: TEST-002/011；所有已知直接调用已先转到MP Store，再删旧具体类。
- After this file: 已建立 old runtime JDBC persistence implementation，后续文件接入/验证前不得声称整个Step完成。

- Validation working directory: `/Users/mario/SelfProject/Egon-COLA`。
- Verification command: `mvn -pl egon-cola-components/egon-cola-component-transactional-outbox-starter -am -DfailIfNoTests=false -Dsurefire.failIfNoSpecifiedTests=false -Dtest=OutboxMybatisPlusAutoConfigurationTest,EgonColaMybatisPlusAutoConfigurationTest,OutboxLongPrimaryKeyTest,TransactionalOutboxAutoConfigurationTest,DefaultTransactionalOutboxTest,ComponentContractGovernanceTest test`。
- Expected result: exit 0，指定TEST-002/011相关非集成单测实际执行且全部通过，Common标记正反测试真实执行；若涉及integration目录，在未获得PG/Testcontainers授权时标为未验证，不能以skip替代真实GREEN。
- Failure returns to: 本Step的第1个RED文件，定位测试/夹具错误；编译/行为失败返回对应生产文件；DDL/依赖不兼容返回Step 1或Step 2并暂停后续。
- Completion criteria: 默认OutboxStore为MP且旧具体类已删除；Dispatcher结果策略留下一步；本Step适用Manual Checks已按实际静态/单测证据复核；禁止把未授权的PG运行写为PASS。
- Rollback: 限定回退本Step提交路径；若已切到新MP表并产生消息，必须遵守主Spec §16停写/核对/前向修复，不能直接启用旧JDBC。
- Commit paths: `egon-cola-components/egon-cola-component-transactional-outbox-starter/src/test/java/top/egon/cola/component/outbox/autoconfigure/OutboxMybatisPlusAutoConfigurationTest.java` `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter/src/main/java/top/egon/cola/component/common/mybatis/extension/EgonColaExplicitTenantScopeMapper.java` `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter/src/test/java/top/egon/cola/component/common/mybatis/autoconfigure/EgonColaMybatisPlusAutoConfigurationTest.java` `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter/src/main/java/top/egon/cola/component/common/mybatis/autoconfigure/EgonColaMybatisPlusContractValidator.java` `egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/persistence/dao/OutboxMessageDAO.java` `egon-cola-components/egon-cola-component-transactional-outbox-starter/src/test/java/top/egon/cola/component/outbox/integration/OutboxMybatisPlusIntegrationTest.java` `egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/store/MybatisPlusOutboxStore.java` `egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/autoconfigure/OutboxMybatisPlusAutoConfiguration.java` `egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/autoconfigure/TransactionalOutboxAutoConfiguration.java` `egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` `egon-cola-components/egon-cola-component-transactional-outbox-starter/src/test/java/top/egon/cola/component/outbox/autoconfigure/TransactionalOutboxAutoConfigurationTest.java` `egon-cola-components/egon-cola-component-transactional-outbox-starter/src/test/java/top/egon/cola/component/outbox/store/OutboxLongPrimaryKeyTest.java` `egon-cola-components/egon-cola-component-transactional-outbox-starter/src/test/java/top/egon/cola/component/outbox/contract/ComponentContractGovernanceTest.java` `egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/persistence/OutboxSchemaMetadataValidator.java` `egon-cola-components/egon-cola-component-transactional-outbox-starter/src/test/java/top/egon/cola/component/outbox/integration/OutboxDataSafetyIntegrationTest.java` `egon-cola-components/egon-cola-component-transactional-outbox-starter/src/test/java/top/egon/cola/component/outbox/integration/PostgresqlOutboxCleanupIntegrationTest.java` `egon-cola-components/egon-cola-component-transactional-outbox-starter/src/test/java/top/egon/cola/component/outbox/integration/PostgresqlOutboxConcurrencyIntegrationTest.java` `egon-cola-components/egon-cola-component-transactional-outbox-starter/src/test/java/top/egon/cola/component/outbox/integration/PostgresqlOutboxQueryPlanIntegrationTest.java` `egon-cola-components/egon-cola-component-transactional-outbox-starter/src/test/java/top/egon/cola/component/outbox/integration/PostgresqlOutboxRecoveryIntegrationTest.java` `egon-cola-components/egon-cola-component-transactional-outbox-starter/src/test/java/top/egon/cola/component/outbox/integration/TransactionalOutboxTransactionIntegrationTest.java` `egon-cola-components/egon-cola-component-transactional-outbox-starter/src/test/java/top/egon/cola/component/outbox/transaction/DefaultTransactionalOutboxTest.java` `egon-cola-components/egon-cola-component-transactional-outbox-starter/src/test/java/top/egon/cola/component/outbox/integration/PostgresqlOutboxTestSupport.java` `egon-cola-components/egon-cola-component-transactional-outbox-starter/src/test/java/top/egon/cola/component/outbox/integration/PostgresqlJdbcOutboxStoreIntegrationTest.java` `egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/store/PostgresqlJdbcOutboxStore.java`
- Commit: `feat(outbox): replace runtime JDBC store with MP`。

### Step 5 — 让Dispatcher依据技术状态机结果分派

- Requirements: REQ-001 REQ-004
- Dependencies: Step 4
- Baseline state: Step 4提交后，默认OutboxStore为MP且旧具体类已删除；OutboxLifecycleService已由Step1注册，Dispatcher仍使用switch选择目标状态。
- Observable outcome: 所有DeliveryResult通过真实OutboxLifecycleService选择目标状态；验收对应TEST-001/003。
- End state: 旧三类配送结果全部经过技术状态机；成功/重试/死信的owner与计量规则保持；业务新handler尚未注册。
- Test-first gate: Required — 当前行为测试若先引用新增生命周期构造依赖会因testCompile失败，所以先按File1–6建立只含构造/Bean/调用点的编译合同，不实现结果路由；再更新File7并运行行为RED，确认旧Dispatcher未调用lifecycleService.evaluate；最后在File1同一生产文件内实现FSM目标分派并GREEN。RED不得以编译错误代替。
- Manual Checks: `MC-ARCH-001`, `MC-REUSE-001`, `MC-DEP-001`, `MC-BEAN-001`, `MC-LOG-001`, `MC-NAME-001`, `MC-UTIL-001`, `MC-PATTERN-001`, `MC-SCOPE-001`, `MC-TEST-001`, `MC-TIME-001`, `MC-VALID-001`, `MC-BLOCKER-001`。Step4两个隔离ContextRunner与恢复集成fixture同步新构造合同；PG测试只编译、不执行。
- Literal Rules: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11。每个文件如下展开；Rule 11覆盖全部路径。
- Ordered files:

#### File 1 — `MODIFY egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/dispatch/OutboxDispatcher.java`

- Purpose: 提供Dispatcher必需的状态机依赖与可编译主构造；行为RED期间暂保留旧结果switch。
- Symbols: `@Slf4j`, `@RequiredArgsConstructor`, qualified final dependencies, lifecycleService, idempotent delivery-permit initialization。
- Repository evidence: egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/dispatch/OutboxDispatcher.java当前使用LoggerFactory、手写十参数构造和内联switch；components/lombok.config已复制Spring Qualifier。
- Dependencies and consumers: Step5 TransactionalOutboxAutoConfiguration、OutboxDispatcherTest、PostgresqlOutboxRecoveryIntegrationTest；状态输入仍来自已claim的OutboxRecord。
- Why now: 主Spec要求Dispatcher拥有具名lifecycleService；先把其类型合同建齐，使测试能够因行为而RED。
- Contract/signature changes: 所有final依赖均有精确Qualifier；新lifecycleService使用outboxLifecycleService；内部队列与Semaphore不成为DI依赖；不改变OutboxStore/TransactionalOutbox/DeliveryHandler接口。
- Input/output and state mapping: 本文件先只建立构造与内部初始化合同；RED点仍由旧switch处理结果，GREEN才让引擎返回值成为Store目标。
- Error and edge behavior: Spring初始化与直接构造均只创建一次permit；状态写false仍走leaseLost；fatal Error仍传播。
- Standards impact: `MC-BEAN-001`, `MC-LOG-001`, `MC-NAME-001`, `MC-PATTERN-001`, `MC-SCOPE-001`, `MC-TEST-001`, `MC-TIME-001`, `MC-VALID-001`。
- Literal rule enforcement: Rule 1：无新载体类型；Rule 2：不改变跨层输入；Rule 4：Lombok注入与所有final依赖Qualifier；Rule 9：保留旧路由仅用于RED，GREEN只解释SSM目标；Rule 10：Clock/Instant/Duration不变；Rule 11：仍是Starter内部Dispatcher。
- Implementation pseudocode:

```java
@Slf4j @RequiredArgsConstructor; retain final qualified store, handlers, classifier, retry, notifier, metrics, identity, executor, properties, clock
add final qualified OutboxLifecycleService lifecycleService; replace LoggerFactory calls with Lombok log
@PostConstruct initializeDeliveryPermits calls one synchronized initializer; direct dispatch lazily invokes the same idempotent initializer
for the compile-contract stage, leave applyResult routing unchanged so File7 can observe the missing lifecycle call
```

- Verification contribution: TEST-001/003；构造类型与内部permit生命周期可编译。
- After this file: 已建立Dispatcher required lifecycle dependency编译合同；未宣称结果已由SSM决定。

#### File 2 — `MODIFY egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/autoconfigure/OutboxStateMachineAutoConfiguration.java`

- Purpose: 为Dispatcher暴露真实的TransactionalOutboxProperties Bean alias。
- Symbols: `outboxStateMachineOutboxProperties`。
- Repository evidence: 当前静态BeanFactoryPostProcessor已为OutboxStateMachineProperties、MP Clock、ObjectMapper注册alias，但缺少outbox业务Properties alias；主Spec §15.3要求复用同一属性实例。
- Dependencies and consumers: `TransactionalOutboxAutoConfiguration.outboxDispatcher`按精确alias注入；不创建第二Properties Bean。
- Why now: 生产wiring依赖此alias且Step5已具备生命周期构造合同。
- Contract/signature changes: 通过现有AliasRegistry helper把唯一TransactionalOutboxProperties bean注册为outboxStateMachineOutboxProperties；候选缺失/冲突按配置错误失败。
- Input/output and state mapping: alias与原配置Properties解析到同一实例，不重新绑定键、不复制配置。
- Error and edge behavior: alias名称已有其他bean时启动失败；不覆盖用户bean，不新增Clock或Properties候选。
- Standards impact: `MC-BEAN-001`, `MC-CONFIG-001`, `MC-SCOPE-001`, `MC-TEST-001`。
- Literal rule enforcement: Rule 2：属性仍由原@ConfigurationProperties验证；Rule 4：alias本身不是业务Bean；Rule 7：不改任何配置键；Rule 11：只扩展现有Starter自动配置。
- Implementation pseudocode:

```java
extend the existing static BeanFactoryPostProcessor
resolve exactly one TransactionalOutboxProperties bean name without eagerly creating a duplicate bean
register that existing name as outboxStateMachineOutboxProperties with the current alias-conflict guard
```

- Verification contribution: TEST-001/003；上下文断言alias与原TransactionalOutboxProperties同一引用。
- After this file: 已建立具名属性alias；后续生产Bean wiring可按Spec精确Qualifier解析。

#### File 3 — `MODIFY egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/autoconfigure/TransactionalOutboxAutoConfiguration.java`

- Purpose: 把StateMachine依赖按精确Bean名注入Dispatcher。
- Symbols: `outboxDispatcher` named bean。
- Repository evidence: 当前@Bean方法传入旧依赖并以Clock.systemUTC()构造Dispatcher；类已在Step4修改并从JDBC store转为OutboxStore SPI。
- Dependencies and consumers: OutboxLifecycleService与两个稳定alias由Step1/5状态机自动配置提供；其他Bean均由现有具名Bean工厂提供。
- Why now: File1/2构造与alias合同已存在，当前主应用装配必须同步。
- Contract/signature changes: 明确Bean名outboxDispatcher；所有依赖参数使用准确Qualifier，包括outboxStore、deliveryHandlerRegistry、deliveryFailureClassifier、outboxRetryPolicy、outboxDeadLetterNotifier、outboxMetrics、outboxWorkerIdentity、outboxDeliveryExecutor、outboxStateMachineOutboxProperties、outboxStateMachineClock、outboxLifecycleService。
- Input/output and state mapping: 保留core outbox启停、异步队列与retry配置；不改变其他Bean或fallback。
- Error and edge behavior: 必需状态机Bean/alias缺失则启动失败；不回退到switch-only或systemUTC临时实例。
- Standards impact: `MC-BEAN-001`, `MC-CONFIG-001`, `MC-SCOPE-001`, `MC-TEST-001`, `MC-TIME-001`。
- Literal rule enforcement: Rule 2：无外部输入变化；Rule 4：具名Bean与Qualifier构造依赖；Rule 7：键树不变；Rule 10：复用state-machine Clock alias；Rule 11：不增加模块层。
- Implementation pseudocode:

```java
@Bean(outboxDispatcher) and @ConditionalOnMissingBean
inject each final dependency with its exact Qualifier, including lifecycleService, existing outboxStateMachineClock and outboxStateMachineOutboxProperties
construct Dispatcher with those injected collaborators; do not create a Clock or Properties instance here
```

- Verification contribution: TEST-001/003；两个隔离ContextRunner能完整解析具名Dispatcher依赖。
- After this file: 已建立主Spring Bean wiring；File4–6同步已有测试fixture后运行行为RED。

#### File 4 — `MODIFY egon-cola-components/egon-cola-component-transactional-outbox-starter/src/test/java/top/egon/cola/component/outbox/integration/PostgresqlOutboxRecoveryIntegrationTest.java`

- Purpose: 更新直接Dispatcher构造以通过Spring fixture提供的真实生命周期服务与Clock。
- Symbols: `dispatcher(...)` test helper。
- Repository evidence: 当前helper直接new Dispatcher并传Clock.systemUTC；父类TestSupport已持有完整ApplicationContext但此fixture有环境门控。
- Dependencies and consumers: `PostgresqlOutboxTestSupport.context`中的outboxLifecycleService、outboxStateMachineClock；不运行容器。
- Why now: File1签名变化后这是已知的第二个直接生产外测试构造点。
- Contract/signature changes: 只追加必需依赖；原恢复与租约断言保持。
- Input/output and state mapping: 状态机用于dispatcher结果决策；本集成测试仍验证到期恢复，不改PG测试场景。
- Error and edge behavior: 本Step只编译此类；EGON_OUTBOX_TEST_POSTGRES_ENABLED未授权时保持未执行。
- Standards impact: `MC-BEAN-001`, `MC-SCOPE-001`, `MC-TEST-001`, `MC-TIME-001`。
- Literal rule enforcement: Rule 2：测试fixture提供已验证服务；Rule 4：新依赖来自Spring Context；Rule 10：使用别名Clock；Rule 11：只改组件测试。
- Implementation pseudocode:

```java
read outboxLifecycleService and outboxStateMachineClock from the existing test ApplicationContext
pass them to the local OutboxDispatcher fixture alongside its existing handler/store/clock collaborators
retain @BeforeAll EGON_OUTBOX_TEST_POSTGRES_ENABLED assumption; do not start Testcontainers in this Step
```

- Verification contribution: TEST-003；`mvn test`编译该test source但不执行Failsafe/PG。
- After this file: 已更新唯一的PG recovery direct-constructor caller；真实PG验证仍未执行。

#### File 5 — `MODIFY egon-cola-components/egon-cola-component-transactional-outbox-starter/src/test/java/top/egon/cola/component/outbox/autoconfigure/TransactionalOutboxAutoConfigurationTest.java`

- Purpose: 为必需的StateMachine Dispatcher依赖提供真实自动配置上下文并验证属性alias。
- Symbols: existing core runtime and alias assertions。
- Repository evidence: 当前ContextRunner只加载TransactionalOutboxAutoConfiguration并用mock Store；新增required lifecycle/Clock/properties Qualifiers后该上下文缺这些Bean。
- Dependencies and consumers: 加载OutboxStateMachineAutoConfiguration；fixture命名egonColaMybatisPlusClock；Outbox core配置提供ValidationUtils与TransactionalOutboxProperties。
- Why now: 验证production Dispatcher Bean可按精确Qualifier装配，避免将mock-only fixture误当默认系统证明。
- Contract/signature changes: 增加alias同一实例断言；其余core runtime/backoff行为保持。
- Input/output and state mapping: 仍用mock DataSource/Store，无数据库或网络；成功上下文含单Dispatcher。
- Error and edge behavior: Clock alias/Properties alias/lifecycleService缺失时测试失败；disabled路径仍不暴露运行Bean。
- Standards impact: `MC-BEAN-001`, `MC-CONFIG-001`, `MC-SCOPE-001`, `MC-TEST-001`, `MC-TIME-001`。
- Literal rule enforcement: Rule 2：测试无业务手动校验输入；Rule 4：具名Qualifier由context验证；Rule 7：只核对已有键；Rule 10：fixture使用java.time Clock；Rule 11：仍为组件ContextRunner。
- Implementation pseudocode:

```java
load TransactionalOutboxAutoConfiguration and OutboxStateMachineAutoConfiguration with named egonColaMybatisPlusClock test bean
assert outboxStateMachineOutboxProperties isSameAs the unique TransactionalOutboxProperties instance
assert one OutboxDispatcher and preserve existing disabled/custom bean backoff assertions
```

- Verification contribution: TEST-001/003；验证真实Bean定义名、alias身份与构造注入。
- After this file: 已建立core配置的state-machine依赖正向路径；无外部运行系统。

#### File 6 — `MODIFY egon-cola-components/egon-cola-component-transactional-outbox-starter/src/test/java/top/egon/cola/component/outbox/autoconfigure/OutboxOptionalAutoConfigurationTest.java`

- Purpose: 保持HTTP/Rabbit optional handler测试在新Dispatcher依赖下仍覆盖原有可选通道合同。
- Symbols: optional handler ApplicationContextRunner。
- Repository evidence: 当前fixture只加载Transactional配置并mock Store；它会因Dispatcher新增required StateMachine依赖而无法启动。
- Dependencies and consumers: 同File5加载StateMachine配置，并提供具名egonColaMybatisPlusClock fixture。
- Why now: 该隔离ContextRunner同样创建TransactionalOutboxAutoConfiguration的Dispatcher。
- Contract/signature changes: 只补必需状态配置和Clock fixture；HTTP/Rabbit开关与publisher safety断言不变。
- Input/output and state mapping: 仍只使用mock Rabbit/HTTP上下文，不执行发送或网络调用。
- Error and edge behavior: 保留publisher safety失败断言；missing StateMachine dependency由application wiring fail-fast。
- Standards impact: `MC-BEAN-001`, `MC-CONFIG-001`, `MC-SCOPE-001`, `MC-TEST-001`。
- Literal rule enforcement: Rule 2：外部输入与验证不变；Rule 4：ContextRunner按具名依赖建Bean；Rule 7：配置键结构不变；Rule 11：仅组件Starter测试。
- Implementation pseudocode:

```java
add OutboxStateMachineAutoConfiguration to the existing ordered ContextRunner
provide egonColaMybatisPlusClock under its production source name so the existing alias is registered
rerun optional HTTP/Rabbit tests without opening sockets or starting a broker
```

- Verification contribution: TEST-001/003；可选handler开关回归与新Dispatcher构造合同同测。
- After this file: 已更新另一个已知Dispatcher上下文；可进入纯内存行为RED。

#### File 7 — `MODIFY egon-cola-components/egon-cola-component-transactional-outbox-starter/src/test/java/top/egon/cola/component/outbox/dispatch/OutboxDispatcherTest.java`

- Purpose: 先锁定StateMachine route RED，再覆盖目标状态到Store动作。
- Symbols: DeliveryResult.Kind→OutboxLifecycleSignalEnum map; state target→Store action。
- Repository evidence: 当前RecordingStore与Handler fixture已覆盖成功、临时错误、耗尽、永久错误、旧owner和Error。
- Dependencies and consumers: Mockito OutboxLifecycleService；Step1真实状态边测试独立验证FSM。
- Why now: File1–6只建立了可编译注入合同，生产applyResult仍为旧switch。
- Contract/signature changes: test fixture通过新@RequiredArgsConstructor签名提供mock；增加对evaluate(PROCESSING, signal, attempt,max)的验证与target驱动动作断言。
- Input/output and state mapping: SUCCESS→DELIVERY_SUCCEEDED→SUCCEEDED；retryable below max→DELIVERY_RETRYABLE→RETRY_WAIT；retryable exhausted→DELIVERY_RETRYABLE→DEAD；permanent→DELIVERY_PERMANENT→DEAD。
- Error and edge behavior: 无效target不写Store；Store false只计leaseLost；dead notification与metrics只在提交成功后产生；Error仍传播。
- Standards impact: `MC-PATTERN-001`, `MC-SCOPE-001`, `MC-TEST-001`, `MC-VALID-001`。
- Literal rule enforcement: Rule 2：边界参数受现有类型/约束；Rule 9：验证FSM目标选择，不在Dispatcher重写业务策略；Rule 11：保持现有Starter架构。
- Implementation pseudocode:

```java
configure mock lifecycleService to return each authoritative target for a signal; first run and verify RED because old applyResult never calls evaluate
map SUCCESS/RETRYABLE_FAILURE/PERMANENT_FAILURE through immutable EnumMap signals
apply only SUCCEEDED/RETRY_WAIT/DEAD targets to markSucceeded/markRetry/markDead; invalid targets fail before persistence
verify leaseLost, retry/dead metrics and dead notification only after the conditional Store write succeeds
```

- Verification contribution: TEST-001/003；覆盖三种结果、重试预算边界、高attempt成功与迟到owner。
- After this file: 行为RED→GREEN闭环完成；技术FSM目标成为唯一消息状态写入决策。

- Validation working directory: `/Users/mario/SelfProject/Egon-COLA`。
- Verification command: `mvn -pl egon-cola-components/egon-cola-component-transactional-outbox-starter -am -DfailIfNoTests=false -Dsurefire.failIfNoSpecifiedTests=false -Dtest=OutboxDispatcherTest,TransactionalOutboxAutoConfigurationTest,OutboxOptionalAutoConfigurationTest,OutboxMybatisPlusAutoConfigurationTest,ComponentContractGovernanceTest test`。
- Expected result: exit 0；指定Dispatcher及ContextRunner测试实际执行且通过；Recovery PG测试只编译，未授权/未设置门禁时不运行。
- Failure returns to: 先定位File7行为RED/GREEN；bean alias/wiring失败返回File2/3；Recovery testCompile返回File4；MP或状态边不兼容返回Step1/4并暂停后续。
- Completion criteria: DeliveryResult只产生FSM信号；Store目标唯一来自真实FSM返回值；旧三类配送结果、租约失效、计量和死信通知均通过；适用Manual Checks复核；PG保持未执行。
- Rollback: 限定回退本Step提交路径；若MP消息已产生，按主Spec §16停写与核对前向修复，不切回JDBC。
- Commit paths: `egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/dispatch/OutboxDispatcher.java` `egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/autoconfigure/OutboxStateMachineAutoConfiguration.java` `egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/autoconfigure/TransactionalOutboxAutoConfiguration.java` `egon-cola-components/egon-cola-component-transactional-outbox-starter/src/test/java/top/egon/cola/component/outbox/integration/PostgresqlOutboxRecoveryIntegrationTest.java` `egon-cola-components/egon-cola-component-transactional-outbox-starter/src/test/java/top/egon/cola/component/outbox/autoconfigure/TransactionalOutboxAutoConfigurationTest.java` `egon-cola-components/egon-cola-component-transactional-outbox-starter/src/test/java/top/egon/cola/component/outbox/autoconfigure/OutboxOptionalAutoConfigurationTest.java` `egon-cola-components/egon-cola-component-transactional-outbox-starter/src/test/java/top/egon/cola/component/outbox/dispatch/OutboxDispatcherTest.java`。
- Commit: `feat(outbox): route delivery outcomes through state machine`。

### Step 6 — 提供可选业务Event状态机可靠消费适配

- Requirements: REQ-002 REQ-003 REQ-004 REQ-005 REQ-006 REQ-007
- Dependencies: Step 5
- Baseline state: Step 5提交后，旧三类配送结果全部通过技术图选择目标；业务新handler尚未注册。
- Observable outcome: 提供可选业务Event状态机可靠消费适配；验收对应TEST-004/005/006/007。
- End state: 业务Event本地可靠消费可选装配；维护copy入口留下一步。
- Test-first gate: Required — Files1–5先建Event/Snapshot/SPI；Files6–8先建Service/Handler/AutoConfiguration可编译合同但不实现消费、路由或启动门禁行为；Files9–11再运行真实内存SSM及ContextRunner行为RED，确认服务无迁移/handler未适配/启用缺SPI仍未fail-fast；最后在Files6–8相同路径完成GREEN，追加AutoConfiguration.imports，PG集成类只编译不执行。业务状态和receipt归接入应用。
- Manual Checks: `MC-ARCH-001`, `MC-REUSE-001`, `MC-DEP-001`, `MC-BEAN-001`, `MC-CONFIG-001`, `MC-JSON-001`, `MC-LOG-001`, `MC-MODEL-001`, `MC-NAME-001`, `MC-UTIL-001`, `MC-PATTERN-001`, `MC-SCOPE-001`, `MC-TEST-001`, `MC-TIME-001`, `MC-VALID-001`, `MC-BLOCKER-001`。在本Step完成前以本段文件和命令逐项核对，已知失败不得延后到后续清理。
- Literal Rules: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11。每个文件如下展开；Rule 11覆盖全部路径。
- Ordered files:

#### File 1 — `CREATE egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/statemachine/BusinessStateMachineEvent.java`

- Purpose: 协议载体是测试与SPI的必要编译前置。
- Symbols: eventId,tenantId,machineKey,definitionVersion,businessId,eventType,expectedVersion,occurredAt,payload。
- Repository evidence: egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/api/OutboxMessage.java已有传输envelope。
- Dependencies and consumers: 新Service/Handler；Boot Jackson局部strict reader；不是业务Command或PO。
- Why now: Step 6的第1个文件；协议载体是测试与SPI的必要编译前置。
- Contract/signature changes: eventId,tenantId,machineKey,definitionVersion,businessId,eventType,expectedVersion,occurredAt,payload；Java class+BasePojo/Builder；Instant UTC ISO；payload ObjectNode；tenant>0；定义/事件字段长度。
- Input/output and state mapping: Java class+BasePojo/Builder；Instant UTC ISO；payload ObjectNode；tenant>0；定义/事件字段长度。
- Error and edge behavior: 新Service/Handler；Boot Jackson局部strict reader；不是业务Command或PO；失败不吞异常、不伪造成功，适用断言由TEST-004/005/006/007验证。
- Standards impact: `MC-JSON-001`, `MC-MODEL-001`, `MC-NAME-001`, `MC-SCOPE-001`, `MC-TEST-001`, `MC-TIME-001`, `MC-VALID-001`；本文件按当前组件约定实现并限定在本Step的路径内。
- Literal rule enforcement: Rule 1：类型名按Event/BO/PO/DAO/Repository/Service/Factory等语义后缀，不新增Data/Info/Param/Bean载体；Rule 2：按@Validated/@Valid、原生约束与Default/已有MP组逐层校验，非代理处调用现有ValidationUtils；Rule 3：普通class完整Lombok注解；EgonModel PO用@SuperBuilder/callSuper；MapStruct仅映射真实边界；Rule 6：Spring Boot Jackson和@EnumValue/@JsonFormat；永不以ordinal作业务编码；Rule 10：使用Instant/Duration/Clock与PG微秒，禁止Date/Calendar/SimpleDateFormat；Rule 11：保留获批的单Starter基础组件结构与MP/受管DDL/CQE边界，绝不改业务平台层次。
- Implementation pseudocode:

```java
@Data @NoArgsConstructor @AllArgsConstructor @Accessors(chain=true) @Builder class BusinessStateMachineEvent implements BasePojo
fields and Jakarta @NotBlank/@Size/@Positive/@Min/@Pattern match Spec §10.2; @JsonFormat(shape=STRING) Instant occurredAt
strict consumer reader rejects unknown top-level fields and invalid payload shape; eventId is business fact identity, not transport messageId
```

- Verification contribution: TEST-004/005/006/007；协议载体是测试与SPI的必要编译前置。
- After this file: 已建立 eventId,tenantId,machineKey,definitionVersion,businessId,eventType,expectedVersion,occurredAt,payload，后续文件接入/验证前不得声称整个Step完成。

#### File 2 — `CREATE egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/statemachine/BusinessStateMachineSnapshotBO.java`

- Purpose: 消费SPI需一个受锁的权威快照。
- Symbols: currentState,version,definitionVersion,appliedFingerprint,facts。
- Repository evidence: egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter/src/main/java/top/egon/cola/component/common/mybatis/model/EgonModel.java业务应用PO的version来源。
- Dependencies and consumers: 由业务Repository返回，Service显式ValidationUtils验证；不对外JSON。
- Why now: Step 6的第2个文件；消费SPI需一个受锁的权威快照。
- Contract/signature changes: currentState,version,definitionVersion,appliedFingerprint,facts；null fingerprint=未消费；facts非null深拷贝；business state/version不等于outbox status/version。
- Input/output and state mapping: null fingerprint=未消费；facts非null深拷贝；business state/version不等于outbox status/version。
- Error and edge behavior: 由业务Repository返回，Service显式ValidationUtils验证；不对外JSON；失败不吞异常、不伪造成功，适用断言由TEST-004/005/006/007验证。
- Standards impact: `MC-MODEL-001`, `MC-NAME-001`, `MC-SCOPE-001`, `MC-TEST-001`, `MC-TIME-001`, `MC-VALID-001`；本文件按当前组件约定实现并限定在本Step的路径内。
- Literal rule enforcement: Rule 1：类型名按Event/BO/PO/DAO/Repository/Service/Factory等语义后缀，不新增Data/Info/Param/Bean载体；Rule 2：按@Validated/@Valid、原生约束与Default/已有MP组逐层校验，非代理处调用现有ValidationUtils；Rule 3：普通class完整Lombok注解；EgonModel PO用@SuperBuilder/callSuper；MapStruct仅映射真实边界；Rule 10：使用Instant/Duration/Clock与PG微秒，禁止Date/Calendar/SimpleDateFormat；Rule 11：保留获批的单Starter基础组件结构与MP/受管DDL/CQE边界，绝不改业务平台层次。
- Implementation pseudocode:

```java
@Data @NoArgsConstructor @AllArgsConstructor @Accessors(chain=true) @Builder class BusinessStateMachineSnapshotBO implements BasePojo
currentState @NotBlank, version @PositiveOrZero, definitionVersion @Min(1), appliedFingerprint nullable exact hex, facts nonnull ObjectNode
on handoff deep-copy facts before engine; if model state unknown do not reset to initial or skip transition
```

- Verification contribution: TEST-004/005/006/007；消费SPI需一个受锁的权威快照。
- After this file: 已建立 currentState,version,definitionVersion,appliedFingerprint,facts，后续文件接入/验证前不得声称整个Step完成。

#### File 3 — `CREATE egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/statemachine/BusinessStateMachineRepository.java`

- Purpose: 快照类型已可编译，固定业务方必须实现的原子边界。
- Symbols: lockAndLoad; saveTransition。
- Repository evidence: egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/delivery/DeliveryHandler.java扩展点与Spec INTERNAL-003/004。
- Dependencies and consumers: 应用自有MP Repository实现；组件不自建业务表/假内存Inbox。
- Why now: Step 6的第3个文件；快照类型已可编译，固定业务方必须实现的原子边界。
- Contract/signature changes: lockAndLoad; saveTransition；消费凭据identity=(tenant,machineKey,definitionVersion,businessId,eventId)，同指纹短路；save CAS version+receipt同TX。
- Input/output and state mapping: 消费凭据identity=(tenant,machineKey,definitionVersion,businessId,eventId)，同指纹短路；save CAS version+receipt同TX。
- Error and edge behavior: 应用自有MP Repository实现；组件不自建业务表/假内存Inbox；失败不吞异常、不伪造成功，适用断言由TEST-004/005/006/007验证。
- Standards impact: `MC-BEAN-001`, `MC-NAME-001`, `MC-SCOPE-001`, `MC-TEST-001`, `MC-VALID-001`；本文件按当前组件约定实现并限定在本Step的路径内。
- Literal rule enforcement: Rule 1：类型名按Event/BO/PO/DAO/Repository/Service/Factory等语义后缀，不新增Data/Info/Param/Bean载体；Rule 2：按@Validated/@Valid、原生约束与Default/已有MP组逐层校验，非代理处调用现有ValidationUtils；Rule 4：实际业务类用@Slf4j、显式Bean名、@RequiredArgsConstructor及每个final依赖@Qualifier；Rule 11：保留获批的单Starter基础组件结构与MP/受管DDL/CQE边界，绝不改业务平台层次。
- Implementation pseudocode:

```java
interface BusinessStateMachineRepository: @Validated SnapshotBO lockAndLoad(@Valid Event,@Pattern sha256 fingerprint)
void saveTransition(@Valid Event,@Valid SnapshotBO,@NotBlank targetState,@Pattern fingerprint) in same caller transaction
implementer locks tenant/business active row before reading receipt; update version/state affected=1 and insert durable receipt; never REQUIRES_NEW or send external IO
```

- Verification contribution: TEST-004/005/006/007；快照类型已可编译，固定业务方必须实现的原子边界。
- After this file: 已建立 lockAndLoad; saveTransition，后续文件接入/验证前不得声称整个Step完成。

#### File 4 — `CREATE egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/statemachine/BusinessStateMachineContextExecutor.java`

- Purpose: worker线程身份独立于Web请求。
- Symbols: execute(Event,Supplier<T>):T。
- Repository evidence: egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/dispatch/OutboxDispatcher.java复用worker线程。
- Dependencies and consumers: 消费应用提供具名outboxBusinessStateMachineContextExecutor Bean；无安全假默认实现。
- Why now: Step 6的第4个文件；worker线程身份独立于Web请求。
- Contract/signature changes: execute(Event,Supplier<T>):T；先验证tenant/source、绑定现有宿主context/MDC，finally恢复旧值。
- Input/output and state mapping: 先验证tenant/source、绑定现有宿主context/MDC，finally恢复旧值。
- Error and edge behavior: 消费应用提供具名outboxBusinessStateMachineContextExecutor Bean；无安全假默认实现；失败不吞异常、不伪造成功，适用断言由TEST-004/005/006/007验证。
- Standards impact: `MC-BEAN-001`, `MC-NAME-001`, `MC-SCOPE-001`, `MC-TEST-001`, `MC-VALID-001`；本文件按当前组件约定实现并限定在本Step的路径内。
- Literal rule enforcement: Rule 1：类型名按Event/BO/PO/DAO/Repository/Service/Factory等语义后缀，不新增Data/Info/Param/Bean载体；Rule 2：按@Validated/@Valid、原生约束与Default/已有MP组逐层校验，非代理处调用现有ValidationUtils；Rule 4：实际业务类用@Slf4j、显式Bean名、@RequiredArgsConstructor及每个final依赖@Qualifier；Rule 11：保留获批的单Starter基础组件结构与MP/受管DDL/CQE边界，绝不改业务平台层次。
- Implementation pseudocode:

```java
interface BusinessStateMachineContextExecutor: <T> T execute(@Valid Event event,@NotNull Supplier<T> action)
app validates event tenant/source in trusted registry, saves existing tenant/trace context, binds event.tenantId synchronously
try action.get() including business transaction; finally restore previous context even on validation/commit failure
```

- Verification contribution: TEST-004/005/006/007；worker线程身份独立于Web请求。
- After this file: 已建立 execute(Event,Supplier<T>):T，后续文件接入/验证前不得声称整个Step完成。

#### File 5 — `CREATE egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/statemachine/BusinessStateMachineDefinitionStrategy.java`

- Purpose: 明示变化轴后才用策略接口。
- Symbols: destination,factory,repository,validateEvent。
- Repository evidence: egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/delivery/DeliveryHandlerRegistry.java既有channel策略注册。
- Dependencies and consumers: Service用不可变Map按destination选取，不反射event.class。
- Why now: Step 6的第5个文件；明示变化轴后才用策略接口。
- Contract/signature changes: destination,factory,repository,validateEvent；精确machineKey:definitionVersion注册；工厂每次fresh flat single-region；业务payload注解约束。
- Input/output and state mapping: 精确machineKey:definitionVersion注册；工厂每次fresh flat single-region；业务payload注解约束。
- Error and edge behavior: Service用不可变Map按destination选取，不反射event.class；失败不吞异常、不伪造成功，适用断言由TEST-004/005/006/007验证。
- Standards impact: `MC-BEAN-001`, `MC-NAME-001`, `MC-PATTERN-001`, `MC-SCOPE-001`, `MC-TEST-001`, `MC-VALID-001`；本文件按当前组件约定实现并限定在本Step的路径内。
- Literal rule enforcement: Rule 1：类型名按Event/BO/PO/DAO/Repository/Service/Factory等语义后缀，不新增Data/Info/Param/Bean载体；Rule 2：按@Validated/@Valid、原生约束与Default/已有MP组逐层校验，非代理处调用现有ValidationUtils；Rule 4：实际业务类用@Slf4j、显式Bean名、@RequiredArgsConstructor及每个final依赖@Qualifier；Rule 9：复杂迁移由SSM State/Strategy/Adapter实际承担，禁止Dispatcher另写状态switch；Rule 11：保留获批的单Starter基础组件结构与MP/受管DDL/CQE边界，绝不改业务平台层次。
- Implementation pseudocode:

```java
interface BusinessStateMachineDefinitionStrategy: String destination(); StateMachineFactory<String,String> stateMachineFactory(); BusinessStateMachineRepository repository()
void validateEvent(@Valid BusinessStateMachineEvent event): bind known business payload type with local strict Jackson reader, invoke Jakarta annotations/groups, reject unknown fields
factory has autoStartup=false and fresh machine per event; only pure guards/actions; registry rejects duplicate/unknown destination rather than selecting latest version
```

- Verification contribution: TEST-004/005/006/007；明示变化轴后才用策略接口。
- After this file: 已建立 destination,factory,repository,validateEvent，后续文件接入/验证前不得声称整个Step完成。

#### File 6 — `CREATE egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/statemachine/BusinessStateMachineService.java`

- Purpose: 建立Service可编译构造和process签名；行为RED后在同一路径完成消费事务、幂等与SSM实现。
- Symbols: process(Event,DeliveryContext):DeliveryResult。
- Repository evidence: egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/transaction/DefaultTransactionalOutbox.java同Spring事务处理惯例。
- Dependencies and consumers: qualified definitions/executor/transactionTemplate/runner/Clock/ValidationUtils/ObjectMapper。
- Why now: Step 6的第6个文件；先提供Service构造与方法编译合同，让File9能因缺少消费行为而RED。
- Contract/signature changes: 先声明process(Event,DeliveryContext):DeliveryResult与全部qualified final依赖；RED后按headers/payload验证→tenantContext→lock→duplicate/hash→expectedVersion→SSM→CAS+receipt→commit完成实现。
- Input/output and state mapping: headers/payload验证→tenantContext→lock→duplicate/hash→expectedVersion→SSM→CAS+receipt→commit。
- Error and edge behavior: qualified definitions/executor/transactionTemplate/runner/Clock/ValidationUtils/ObjectMapper；失败不吞异常、不伪造成功，适用断言由TEST-004/005/006/007验证。
- Standards impact: `MC-BEAN-001`, `MC-JSON-001`, `MC-LOG-001`, `MC-NAME-001`, `MC-PATTERN-001`, `MC-SCOPE-001`, `MC-TEST-001`, `MC-TIME-001`, `MC-VALID-001`；本文件按当前组件约定实现并限定在本Step的路径内。
- Literal rule enforcement: Rule 1：类型名按Event/BO/PO/DAO/Repository/Service/Factory等语义后缀，不新增Data/Info/Param/Bean载体；Rule 2：按@Validated/@Valid、原生约束与Default/已有MP组逐层校验，非代理处调用现有ValidationUtils；Rule 4：实际业务类用@Slf4j、显式Bean名、@RequiredArgsConstructor及每个final依赖@Qualifier；Rule 6：Spring Boot Jackson和@EnumValue/@JsonFormat；永不以ordinal作业务编码；Rule 9：复杂迁移由SSM State/Strategy/Adapter实际承担，禁止Dispatcher另写状态switch；Rule 10：使用Instant/Duration/Clock与PG微秒，禁止Date/Calendar/SimpleDateFormat；Rule 11：保留获批的单Starter基础组件结构与MP/受管DDL/CQE边界，绝不改业务平台层次。
- Implementation pseudocode:

```java
compile contract: @Slf4j @Validated @RequiredArgsConstructor named Service; process(Event,DeliveryContext) signature and exact qualified dependency fields
compile-only body returns explicit OUTBOX_SM_EXECUTION_FAILED permanent result; File9 behavior tests must fail before implementation
after RED implement context validation, pre-TX definition validation, fingerprint/receipt short-circuit, locked snapshot, expectedVersion, fresh SSM, same-TX saveTransition, and post-commit success
```

- Verification contribution: TEST-004/005/006/007；RED合同已固定，最小实现消费事务/幂等/SSM。
- After this file: 已建立可编译Service合同；消费行为仍缺失，待File9 RED后在本路径完成GREEN。

#### File 7 — `CREATE egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/delivery/statemachine/BusinessStateMachineDeliveryHandler.java`

- Purpose: 建立Handler可编译DeliveryHandler合同；行为RED后在本路径完成envelope解析与Service适配。
- Symbols: implements DeliveryHandler。
- Repository evidence: egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/delivery/DeliveryHandler.java及Registry现有规则。
- Dependencies and consumers: DeliveryHandlerRegistry与businessService，关闭开关时不注册。
- Why now: Step 6的第7个文件；先提供Handler具体类型与接口方法合同，让File10能编译并因未路由而RED。
- Contract/signature changes: 先实现DeliveryHandler接口签名、Bean依赖构造和channel名；RED后完成destination注册、strict Event JSON解析/校验→process。
- Input/output and state mapping: channel=statemachine；destination注册检查；strict Event JSON解析/校验→process。
- Error and edge behavior: DeliveryHandlerRegistry与businessService，关闭开关时不注册；失败不吞异常、不伪造成功，适用断言由TEST-004/005/006/007验证。
- Standards impact: `MC-BEAN-001`, `MC-JSON-001`, `MC-LOG-001`, `MC-NAME-001`, `MC-PATTERN-001`, `MC-SCOPE-001`, `MC-TEST-001`, `MC-VALID-001`；本文件按当前组件约定实现并限定在本Step的路径内。
- Literal rule enforcement: Rule 1：类型名按Event/BO/PO/DAO/Repository/Service/Factory等语义后缀，不新增Data/Info/Param/Bean载体；Rule 2：按@Validated/@Valid、原生约束与Default/已有MP组逐层校验，非代理处调用现有ValidationUtils；Rule 4：实际业务类用@Slf4j、显式Bean名、@RequiredArgsConstructor及每个final依赖@Qualifier；Rule 6：Spring Boot Jackson和@EnumValue/@JsonFormat；永不以ordinal作业务编码；Rule 9：复杂迁移由SSM State/Strategy/Adapter实际承担，禁止Dispatcher另写状态switch；Rule 11：保留获批的单Starter基础组件结构与MP/受管DDL/CQE边界，绝不改业务平台层次。
- Implementation pseudocode:

```java
@Slf4j @RequiredArgsConstructor implements DeliveryHandler; named @Bean("outboxBusinessStateMachineDeliveryHandler") in business auto-config
channel() returns "statemachine"; validateDestination checks immutable definition map exact key/version
 deliver(context): require application/json/schemaVersion="1" and all egon-* headers equal Event fields; Boot ObjectMapper strict reader; return businessService.process(event,context)
```

- Verification contribution: TEST-004/005/006/007；Service处理结果稳定后适配已有投递SPI。
- After this file: 已建立可编译Handler合同；尚未消费Event，待File10 RED后在本路径完成GREEN。

#### File 8 — `CREATE egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/autoconfigure/OutboxBusinessStateMachineAutoConfiguration.java`

- Purpose: 建立可被ContextRunner加载的可编译AutoConfiguration类型；行为RED后在本路径注册可选通道。
- Symbols: business Bean registry/tx/handler。
- Repository evidence: egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/autoconfigure/TransactionalOutboxAutoConfiguration.java既有 @Bean 模式。
- Dependencies and consumers: 原OutboxInfrastructure selectedDataSource/transactionManager、业务定义Bean由消费方提供。
- Why now: Step 6的第8个文件；先提供可导入的自动配置类型，让File11能运行缺少Bean注册行为的ContextRunner RED。
- Contract/signature changes: 先声明AutoConfiguration类与business.enabled条件；RED后发布具名definitions/transaction/service/handler beans，并要求definitions与contextExecutor齐全。
- Input/output and state mapping: business.enabled=false不发布；true必须定义map+contextExecutor；timeout < delivery timeout。
- Error and edge behavior: 原OutboxInfrastructure selectedDataSource/transactionManager、业务定义Bean由消费方提供；失败不吞异常、不伪造成功，适用断言由TEST-004/005/006/007验证。
- Standards impact: `MC-BEAN-001`, `MC-CONFIG-001`, `MC-PATTERN-001`, `MC-SCOPE-001`, `MC-TEST-001`, `MC-VALID-001`；本文件按当前组件约定实现并限定在本Step的路径内。
- Literal rule enforcement: Rule 2：按@Validated/@Valid、原生约束与Default/已有MP组逐层校验，非代理处调用现有ValidationUtils；Rule 4：实际业务类用@Slf4j、显式Bean名、@RequiredArgsConstructor及每个final依赖@Qualifier；Rule 7：配置键对全部profile保持一致，组件无生产application YAML，双语README逐项同步；Rule 9：复杂迁移由SSM State/Strategy/Adapter实际承担，禁止Dispatcher另写状态switch；Rule 11：保留获批的单Starter基础组件结构与MP/受管DDL/CQE边界，绝不改业务平台层次。
- Implementation pseudocode:

```java
compile contract: declare OutboxBusinessStateMachineAutoConfiguration with business.enabled condition so tests can import the type
RED phase has no business beans; enabled=true without SPI must still fail the ContextRunner assertion
after RED register immutable definitions Map, validate each definition/fresh graph, require contextExecutor, and publish named transaction/Service/Handler beans
```

- Verification contribution: TEST-004/005/006/007；定义和Handler均存在后才装配可选通道。
- After this file: 已建立可编译auto-configuration类型；business Bean仍未装配，待File11 RED后在本路径完成GREEN。

#### File 9 — `CREATE egon-cola-components/egon-cola-component-transactional-outbox-starter/src/test/java/top/egon/cola/component/outbox/statemachine/BusinessStateMachineServiceTest.java`

- Purpose: Event/Snapshot/SPI可编译后写RED，Service尚不存在。
- Symbols: receipt replay/version/context/test-only real SSM。
- Repository evidence: egon-cola-components/egon-cola-component-transactional-outbox-starter/src/test/java/top/egon/cola/component/outbox/dispatch/OutboxDispatcherTest.java现有DeliveryResult三态fixture。
- Dependencies and consumers: 真实内存SSM＋Repository stub，保持测试不依赖业务生产表。
- Why now: Step 6的第9个文件；Event/Snapshot/SPI可编译后写RED，Service尚不存在。
- Contract/signature changes: receipt replay/version/context/test-only real SSM；E1/E2/E1仅E1一次迁移；同ID异内容冲突；未来版本重试、旧版本拒绝。
- Input/output and state mapping: E1/E2/E1仅E1一次迁移；同ID异内容冲突；未来版本重试、旧版本拒绝。
- Error and edge behavior: 真实内存SSM＋Repository stub，保持测试不依赖业务生产表；失败不吞异常、不伪造成功，适用断言由TEST-004/005/006/007验证。
- Standards impact: `MC-JSON-001`, `MC-MODEL-001`, `MC-PATTERN-001`, `MC-SCOPE-001`, `MC-TEST-001`, `MC-VALID-001`；本文件按当前组件约定实现并限定在本Step的路径内。
- Literal rule enforcement: Rule 2：按@Validated/@Valid、原生约束与Default/已有MP组逐层校验，非代理处调用现有ValidationUtils；Rule 3：普通class完整Lombok注解；EgonModel PO用@SuperBuilder/callSuper；MapStruct仅映射真实边界；Rule 6：Spring Boot Jackson和@EnumValue/@JsonFormat；永不以ordinal作业务编码；Rule 9：复杂迁移由SSM State/Strategy/Adapter实际承担，禁止Dispatcher另写状态switch；Rule 11：保留获批的单Starter基础组件结构与MP/受管DDL/CQE边界，绝不改业务平台层次。
- Implementation pseudocode:

```java
send PAYMENT_CONFIRMED Event with expectedVersion=3 to AWAITING_PAYMENT snapshot; assert PAID plus saveTransition once
on second identical event after E2 return DeliveryResult.SUCCESS without factory.getStateMachine or saveTransition; changed payload same eventId -> PERMANENT_FAILURE
simulate version gap/stale/guard denial/timeout; assert correct DeliveryResult and transaction rollback, context executor finally called
```

- Verification contribution: TEST-004/005/006/007；Event/Snapshot/SPI可编译后写RED，Service尚不存在。
- After this file: 本测试定义该Step的RED断言，生产行为仍缺失；其余文件填入后GREEN。

#### File 10 — `CREATE egon-cola-components/egon-cola-component-transactional-outbox-starter/src/test/java/top/egon/cola/component/outbox/delivery/statemachine/BusinessStateMachineDeliveryHandlerTest.java`

- Purpose: Handler尚不存在，RED说明既有HandlerRegistry无statemachine通道。
- Symbols: channel/destination/event envelope validation。
- Repository evidence: egon-cola-components/egon-cola-component-transactional-outbox-starter/src/test/java/top/egon/cola/component/outbox/delivery/DeliveryHandlerRegistryTest.java当前SPI测试。
- Dependencies and consumers: 现有DeliveryHandlerRegistryTest、ObjectMapper与业务Service stub。
- Why now: Step 6的第10个文件；Handler尚不存在，RED说明既有HandlerRegistry无statemachine通道。
- Contract/signature changes: channel/destination/event envelope validation；header/body机器key/tenant/eventId/definitionVersion精确一致；缺Bean/未知版本拒绝。
- Input/output and state mapping: header/body机器key/tenant/eventId/definitionVersion精确一致；缺Bean/未知版本拒绝。
- Error and edge behavior: 现有DeliveryHandlerRegistryTest、ObjectMapper与业务Service stub；失败不吞异常、不伪造成功，适用断言由TEST-004/005/006/007验证。
- Standards impact: `MC-JSON-001`, `MC-PATTERN-001`, `MC-SCOPE-001`, `MC-TEST-001`, `MC-VALID-001`；本文件按当前组件约定实现并限定在本Step的路径内。
- Literal rule enforcement: Rule 2：按@Validated/@Valid、原生约束与Default/已有MP组逐层校验，非代理处调用现有ValidationUtils；Rule 6：Spring Boot Jackson和@EnumValue/@JsonFormat；永不以ordinal作业务编码；Rule 9：复杂迁移由SSM State/Strategy/Adapter实际承担，禁止Dispatcher另写状态switch；Rule 11：保留获批的单Starter基础组件结构与MP/受管DDL/CQE边界，绝不改业务平台层次。
- Implementation pseudocode:

```java
handler.channel() == "statemachine"; validateDestination("order-payment:1") succeeds only registered definition
DeliveryContext with header tenantId!=payload tenantId -> DeliveryResult.permanentFailure(OUTBOX_SM_EVENT_INVALID) and no business Service call
valid JSON payload uses strict local reader, invokes service.process; transport messageId remains distinct from business eventId
```

- Verification contribution: TEST-004/005/006/007；Handler尚不存在，RED说明既有HandlerRegistry无statemachine通道。
- After this file: 本测试定义该Step的RED断言，生产行为仍缺失；其余文件填入后GREEN。

#### File 11 — `CREATE egon-cola-components/egon-cola-component-transactional-outbox-starter/src/test/java/top/egon/cola/component/outbox/autoconfigure/OutboxBusinessStateMachineAutoConfigurationTest.java`

- Purpose: Bean图尚无业务自动配置，RED证明旧系统不支持可选通道。
- Symbols: off by default; fail open without SPI。
- Repository evidence: egon-cola-components/egon-cola-component-transactional-outbox-starter/src/test/java/top/egon/cola/component/outbox/autoconfigure/TransactionalOutboxAutoConfigurationTest.java ContextRunner。
- Dependencies and consumers: ApplicationContextRunner沿用旧outbox配置测试风格。
- Why now: Step 6的第11个文件；Bean图尚无业务自动配置，RED证明旧系统不支持可选通道。
- Contract/signature changes: off by default; fail open without SPI；关闭时0 business Bean；开启无definition/context即startup error；重复destination error。
- Input/output and state mapping: 关闭时0 business Bean；开启无definition/context即startup error；重复destination error。
- Error and edge behavior: ApplicationContextRunner沿用旧outbox配置测试风格；失败不吞异常、不伪造成功，适用断言由TEST-004/005/006/007验证。
- Standards impact: `MC-BEAN-001`, `MC-CONFIG-001`, `MC-SCOPE-001`, `MC-TEST-001`, `MC-VALID-001`；本文件按当前组件约定实现并限定在本Step的路径内。
- Literal rule enforcement: Rule 2：按@Validated/@Valid、原生约束与Default/已有MP组逐层校验，非代理处调用现有ValidationUtils；Rule 4：实际业务类用@Slf4j、显式Bean名、@RequiredArgsConstructor及每个final依赖@Qualifier；Rule 7：配置键对全部profile保持一致，组件无生产application YAML，双语README逐项同步；Rule 11：保留获批的单Starter基础组件结构与MP/受管DDL/CQE边界，绝不改业务平台层次。
- Implementation pseudocode:

```java
run with business.enabled=false -> no outboxBusinessStateMachineDeliveryHandler and existing HTTP/Rabbit handlers untouched
run enabled=true with missing definition or outboxBusinessStateMachineContextExecutor -> OutboxConfigurationException
provide one named definition/repository/executor -> exactly one statemachine handler, stable qualifiers and timeout property binding
```

- Verification contribution: TEST-004/005/006/007；Bean图尚无业务自动配置，RED证明旧系统不支持可选通道。
- After this file: 本测试定义该Step的RED断言，生产行为仍缺失；其余文件填入后GREEN。

#### File 12 — `MODIFY egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`

- Purpose: Step4已注册core/MP；此时增加可编译业务类。
- Symbols: append optional Business auto-config。
- Repository evidence: egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports Step4状态。
- Dependencies and consumers: Spring Boot imports，原四个entries/core/MP entries保持顺序及唯一。
- Why now: Step 6的第12个文件；Step4已注册core/MP；此时增加可编译业务类。
- Contract/signature changes: append optional Business auto-config；仅增加业务autoConfig一行；其内部property条件关闭默认行为。
- Input/output and state mapping: 仅增加业务autoConfig一行；其内部property条件关闭默认行为。
- Error and edge behavior: Spring Boot imports，原四个entries/core/MP entries保持顺序及唯一；失败不吞异常、不伪造成功，适用断言由TEST-004/005/006/007验证。
- Standards impact: `MC-CONFIG-001`, `MC-SCOPE-001`, `MC-TEST-001`；本文件按当前组件约定实现并限定在本Step的路径内。
- Literal rule enforcement: Rule 7：配置键对全部profile保持一致，组件无生产application YAML，双语README逐项同步；Rule 11：保留获批的单Starter基础组件结构与MP/受管DDL/CQE边界，绝不改业务平台层次。
- Implementation pseudocode:

```text
retain existing OutboxMetrics/Transactional/HTTP/Rabbit plus Step4 Core/MP entries
append top.egon.cola.component.outbox.autoconfigure.OutboxBusinessStateMachineAutoConfiguration once
assert business.enabled=false context contains no statemachine DeliveryHandler; enabled=true without mandatory SPI fails explicitly
```

- Verification contribution: TEST-004/005/006/007；Step4已注册core/MP；此时增加可编译业务类。
- After this file: 已建立 append optional Business auto-config，后续文件接入/验证前不得声称整个Step完成。

#### File 13 — `CREATE egon-cola-components/egon-cola-component-transactional-outbox-starter/src/test/java/top/egon/cola/component/outbox/integration/BusinessStateMachineIntegrationTest.java`

- Purpose: unit结果已GREEN后补跨事务实证（执行PG需另授权）。
- Symbols: same-TX state+receipt and replay window。
- Repository evidence: egon-cola-components/egon-cola-component-transactional-outbox-starter/src/test/java/top/egon/cola/component/outbox/integration/TransactionalOutboxTransactionIntegrationTest.java测试本地PG事务。
- Dependencies and consumers: 测试专有业务fixture，生产仍无具体业务表。
- Why now: Step 6的第13个文件；unit结果已GREEN后补跨事务实证（执行PG需另授权）。
- Contract/signature changes: same-TX state+receipt and replay window；业务状态/receipt同事务、故障回滚、技术成功回写失败后重投、跨tenant隔离。
- Input/output and state mapping: 业务状态/receipt同事务、故障回滚、技术成功回写失败后重投、跨tenant隔离。
- Error and edge behavior: 测试专有业务fixture，生产仍无具体业务表；失败不吞异常、不伪造成功，适用断言由TEST-004/005/006/007验证。
- Standards impact: `MC-MODEL-001`, `MC-PATTERN-001`, `MC-SCOPE-001`, `MC-TEST-001`, `MC-VALID-001`；本文件按当前组件约定实现并限定在本Step的路径内。
- Literal rule enforcement: Rule 2：按@Validated/@Valid、原生约束与Default/已有MP组逐层校验，非代理处调用现有ValidationUtils；Rule 3：普通class完整Lombok注解；EgonModel PO用@SuperBuilder/callSuper；MapStruct仅映射真实边界；Rule 9：复杂迁移由SSM State/Strategy/Adapter实际承担，禁止Dispatcher另写状态switch；Rule 11：保留获批的单Starter基础组件结构与MP/受管DDL/CQE边界，绝不改业务平台层次。
- Implementation pseudocode:

```java
arrange test-only business aggregate/receipt on same PRIMARY; invoke statemachine channel through real OutboxStore/Dispatcher
commit business state+receipt but inject markSucceeded failure; after lease reclaim replay E1 and assert no second state mutation
inject saveTransition receipt/DB errors -> both rolled back; two workers same Event receive one durable receipt; external calls never inside SSM Action
```

- Verification contribution: TEST-004/005/006/007；unit结果已GREEN后补跨事务实证（执行PG需另授权）。
- After this file: 本测试定义该Step的RED断言，生产行为仍缺失；其余文件填入后GREEN。

- Validation working directory: `/Users/mario/SelfProject/Egon-COLA`。
- Verification command: `mvn -pl egon-cola-components/egon-cola-component-transactional-outbox-starter -am -DfailIfNoTests=false -Dsurefire.failIfNoSpecifiedTests=false -Dtest=BusinessStateMachineServiceTest,BusinessStateMachineDeliveryHandlerTest,OutboxBusinessStateMachineAutoConfigurationTest test`。
- Expected result: exit 0，指定TEST-004/005/006/007相关非集成单测实际执行且全部通过；若涉及integration目录，在未获得PG/Testcontainers授权时标为未验证，不能以skip替代真实GREEN。
- Failure returns to: 本Step的第1个RED文件，定位测试/夹具错误；编译/行为失败返回对应生产文件；DDL/依赖不兼容返回Step 1或Step 2并暂停后续。
- Completion criteria: 业务Event本地可靠消费可选装配；维护copy入口留下一步；本Step适用Manual Checks已按实际静态/单测证据复核；禁止把未授权的PG运行写为PASS。
- Rollback: 限定回退本Step提交路径；若已切到新MP表并产生消息，必须遵守主Spec §16停写/核对/前向修复，不能直接启用旧JDBC。
- Commit paths: `egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/statemachine/BusinessStateMachineEvent.java` `egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/statemachine/BusinessStateMachineSnapshotBO.java` `egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/statemachine/BusinessStateMachineRepository.java` `egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/statemachine/BusinessStateMachineContextExecutor.java` `egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/statemachine/BusinessStateMachineDefinitionStrategy.java` `egon-cola-components/egon-cola-component-transactional-outbox-starter/src/test/java/top/egon/cola/component/outbox/statemachine/BusinessStateMachineServiceTest.java` `egon-cola-components/egon-cola-component-transactional-outbox-starter/src/test/java/top/egon/cola/component/outbox/delivery/statemachine/BusinessStateMachineDeliveryHandlerTest.java` `egon-cola-components/egon-cola-component-transactional-outbox-starter/src/test/java/top/egon/cola/component/outbox/autoconfigure/OutboxBusinessStateMachineAutoConfigurationTest.java` `egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/statemachine/BusinessStateMachineService.java` `egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/delivery/statemachine/BusinessStateMachineDeliveryHandler.java` `egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/autoconfigure/OutboxBusinessStateMachineAutoConfiguration.java` `egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` `egon-cola-components/egon-cola-component-transactional-outbox-starter/src/test/java/top/egon/cola/component/outbox/integration/BusinessStateMachineIntegrationTest.java`
- Commit: `feat(outbox): adapt business events to state machines`。

### Step 7 — 提供停写复制、完整核对与维护门禁

- Requirements: REQ-004 REQ-006 REQ-009 REQ-010
- Dependencies: Step 6
- Baseline state: Step 6提交后，业务Event本地可靠消费可选装配；维护copy入口留下一步。
- Observable outcome: 提供停写复制、完整核对与维护门禁；验收对应TEST-012。
- End state: 维护模式与只读源复制核对可用；无真实生产迁移执行。
- Test-first gate: Required — File1 Result、File2 technical-context API、File3 Migration Service method/Bean compile contract先行；File4 PG集成测试定义复制行为后，File5 no-PG Service validation test因unsafe schema/mode/batch guard缺失先RED；Files6–10建立Store/Dispatcher字段、Bean wiring与直接构造调用合同，File11 Store enqueue与File12 Dispatcher维护测试分别RED。随后在Files3/6–8已声明路径完成GREEN，File13注册完整Migration Service Bean。PG复制/锁/提交仍只编译、不执行；新旧进程全局停写依赖运维门槛，不在测试里假定本机开关能停止别的节点。
- Manual Checks: `MC-ARCH-001`, `MC-REUSE-001`, `MC-DEP-001`, `MC-BEAN-001`, `MC-CONFIG-001`, `MC-JSON-001`, `MC-LOG-001`, `MC-MODEL-001`, `MC-NAME-001`, `MC-UTIL-001`, `MC-PATTERN-001`, `MC-SCOPE-001`, `MC-TEST-001`, `MC-TIME-001`, `MC-VALID-001`, `MC-BLOCKER-001`。在本Step完成前以本段文件和命令逐项核对，已知失败不得延后到后续清理。
- Literal Rules: Rule 1, Rule 2, Rule 3, Rule 4, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11。每个文件如下展开；Rule 11覆盖全部路径。
- Ordered files:

#### File 1 — `CREATE egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/migration/OutboxMigrationResult.java`

- Purpose: 迁移Service/Test需要结果载体编译前置。
- Symbols: source/target/counts/firstDifferenceId/verified。
- Repository evidence: egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/statemachine/BusinessStateMachineSnapshotBO.java相同普通载体注解风格。
- Dependencies and consumers: BasePojo与Lombok classBuilder；维护者只观察Java本地结果。
- Why now: Step 7第1个文件；迁移Service/Test需要结果载体编译前置。
- Contract/signature changes: source/target/counts/firstDifferenceId/verified；真实完整双向验证才verified=true；不输出payload或头。
- Input/output and state mapping: 真实完整双向验证才verified=true；不输出payload或头。
- Error and edge behavior: BasePojo与Lombok classBuilder；维护者只观察Java本地结果；失败不吞异常、不伪造成功，适用断言由TEST-012验证。
- Standards impact: `MC-MODEL-001`, `MC-NAME-001`, `MC-SCOPE-001`, `MC-TEST-001`, `MC-TIME-001`, `MC-VALID-001`；本文件按当前组件约定实现并限定在本Step的路径内。
- Literal rule enforcement: Rule 1：类型名按Event/BO/PO/DAO/Repository/Service/Factory等语义后缀，不新增Data/Info/Param/Bean载体；Rule 2：按@Validated/@Valid、原生约束与Default/已有MP组逐层校验，非代理处调用现有ValidationUtils；Rule 3：普通class完整Lombok注解；EgonModel PO用@SuperBuilder/callSuper；MapStruct仅映射真实边界；Rule 10：使用Instant/Duration/Clock与PG微秒，禁止Date/Calendar/SimpleDateFormat；Rule 11：保留获批的单Starter基础组件结构与MP/受管DDL/CQE边界，绝不改业务平台层次。
- Implementation pseudocode:

```java
@Data @NoArgsConstructor @AllArgsConstructor @Accessors(chain=true) @Builder class OutboxMigrationResult implements BasePojo
String sourceSchema,targetSchema; Long sourceCount,targetCount,copiedCount,matchedCount,differenceCount,firstDifferenceId nullable; boolean verified
all counts nonnegative; firstDifferenceId only safe row ID; verified only after all original 22 columns and totals/status counts match
```

- Verification contribution: TEST-012；迁移Service/Test需要结果载体编译前置。
- After this file: 已建立 source/target/counts/firstDifferenceId/verified，后续文件接入/验证前不得声称整个Step完成。

#### File 2 — `MODIFY egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/persistence/OutboxTechnicalContextExecutor.java`

- Purpose: 迁移导入审计与常规消息审计语义不同。
- Symbols: executeMigration audit actor。
- Repository evidence: egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/persistence/OutboxTechnicalContextExecutor.java Step3常规system:outbox。
- Dependencies and consumers: OutboxLegacyMigrationService调用；常规execute不改。
- Why now: Step 7第2个文件；迁移导入审计与常规消息审计语义不同。
- Contract/signature changes: executeMigration audit actor；技术tenant仍0，auditUser=system:outbox:migration，仅迁移事务内部；finally旧MDC。
- Input/output and state mapping: 技术tenant仍0，auditUser=system:outbox:migration，仅迁移事务内部；finally旧MDC。
- Error and edge behavior: OutboxLegacyMigrationService调用；常规execute不改；失败不吞异常、不伪造成功，适用断言由TEST-012验证。
- Standards impact: `MC-BEAN-001`, `MC-LOG-001`, `MC-SCOPE-001`, `MC-TEST-001`, `MC-VALID-001`；本文件按当前组件约定实现并限定在本Step的路径内。
- Literal rule enforcement: Rule 2：按@Validated/@Valid、原生约束与Default/已有MP组逐层校验，非代理处调用现有ValidationUtils；Rule 4：实际业务类用@Slf4j、显式Bean名、@RequiredArgsConstructor及每个final依赖@Qualifier；Rule 11：保留获批的单Starter基础组件结构与MP/受管DDL/CQE边界，绝不改业务平台层次。
- Implementation pseudocode:

```java
add executeMigration(Supplier<T> action) sharing same save/restore MDC implementation
set tenantId="0", userId="system:outbox:migration" during MP insert/read validation only
finally restore original tenant/user even when migration batch fails; do not copy unknown legacy user identity into new EgonModel audit fields
```

- Verification contribution: TEST-012；迁移导入审计与常规消息审计语义不同。
- After this file: 已建立 executeMigration audit actor，后续文件接入/验证前不得声称整个Step完成。

#### File 3 — `CREATE egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/migration/OutboxLegacyMigrationService.java`

- Purpose: RED预期缺少真实copy/核对入口；所有依赖Step2/4就绪。
- Symbols: migrate(sourceSchema,batchSize,verifyOnly):Result。
- Repository evidence: egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/migration/OutboxManagedDdlInitializer.java Step2物理PRIMARY引用。
- Dependencies and consumers: OutboxManagedDdlInitializer提供same PRIMARY；OutboxMessageRepository插入；technicalContext绑定actor。
- Why now: Step 7第3个文件；RED预期缺少真实copy/核对入口；所有依赖Step2/4就绪。
- Contract/signature changes: migrate(sourceSchema,batchSize,verifyOnly):Result；源READ_COMMITTED+SHARE表锁，按id游标读；目标MP batch事务写；最终双向逐字段/汇总验证。
- Input/output and state mapping: 源READ_COMMITTED+SHARE表锁，按id游标读；目标MP batch事务写；最终双向逐字段/汇总验证。
- Error and edge behavior: OutboxManagedDdlInitializer提供same PRIMARY；OutboxMessageRepository插入；technicalContext绑定actor；失败不吞异常、不伪造成功，适用断言由TEST-012验证。
- Standards impact: `MC-BEAN-001`, `MC-CONVERT-001`, `MC-JSON-001`, `MC-LOG-001`, `MC-MODEL-001`, `MC-NAME-001`, `MC-SCOPE-001`, `MC-TEST-001`, `MC-TIME-001`, `MC-VALID-001`；本文件复用现有MapStruct生成的OutboxMessageConverter.toMigrationPO并以其合同测试留证。
- Literal rule enforcement: Rule 1：类型名按Event/BO/PO/DAO/Repository/Service/Factory等语义后缀，不新增Data/Info/Param/Bean载体；Rule 2：按@Validated/@Valid、原生约束与Default/已有MP组逐层校验，非代理处调用现有ValidationUtils；Rule 3：普通class完整Lombok注解；EgonModel PO用@SuperBuilder/callSuper；MapStruct仅映射真实边界；Rule 4：实际业务类用@Slf4j、显式Bean名、@RequiredArgsConstructor及每个final依赖@Qualifier；Rule 6：Spring Boot Jackson和@EnumValue/@JsonFormat；永不以ordinal作业务编码；Rule 10：使用Instant/Duration/Clock与PG微秒，禁止Date/Calendar/SimpleDateFormat；Rule 11：保留获批的单Starter基础组件结构与MP/受管DDL/CQE边界，绝不改业务平台层次。
- Implementation pseudocode:

```java
@Slf4j @Validated @RequiredArgsConstructor; named @Bean("outboxLegacyMigrationService") in MP auto-config; reject !migrationMode, bad sourceSchema, targetSchema==source, target any version>0
source connection transaction READ_COMMITTED; set lock_timeout; LOCK TABLE schema.egon_cola_outbox_message IN SHARE MODE before first SELECT; no source DML
keyset scan id>cursor LIMIT batchSize; MP insertMigrationMessage preserves id/raw payload/raw headers, on conflict compare id+messageId+key+all 22 old columns, commit per batch; final full streaming source↔target compare sets/counts/status, return verified only if zero diff
```

- Verification contribution: TEST-012；RED预期缺少真实copy/核对入口；所有依赖Step2/4就绪。
- After this file: 已建立 migrate(sourceSchema,batchSize,verifyOnly):Result，后续文件接入/验证前不得声称整个Step完成。

#### File 4 — `CREATE egon-cola-components/egon-cola-component-transactional-outbox-starter/src/test/java/top/egon/cola/component/outbox/integration/OutboxMpMigrationIntegrationTest.java`

- Purpose: 结果类型可编译，行为Service尚缺；测试定义无损验收。
- Symbols: copy/verify/resume/maintenance RED。
- Repository evidence: egon-cola-components/egon-cola-component-transactional-outbox-starter/src/test/java/top/egon/cola/component/outbox/integration/PostgresqlOutboxTestSupport.java两schema测试基础。
- Dependencies and consumers: 两schema同PG fixture，TEST-012控制PG开关，绝不执行生产迁移。
- Why now: Step 7第4个文件；结果类型可编译，行为Service尚缺；测试定义无损验收。
- Contract/signature changes: copy/verify/resume/maintenance RED；五态/NULL/raw strings/未知commit/重跑/多余target行/版本已变/源锁冲突。
- Input/output and state mapping: 五态/NULL/raw strings/未知commit/重跑/多余target行/版本已变/源锁冲突。
- Error and edge behavior: 两schema同PG fixture，TEST-012控制PG开关，绝不执行生产迁移；失败不吞异常、不伪造成功，适用断言由TEST-012验证。
- Standards impact: `MC-MODEL-001`, `MC-SCOPE-001`, `MC-TEST-001`, `MC-VALID-001`；本文件按当前组件约定实现并限定在本Step的路径内。
- Literal rule enforcement: Rule 2：按@Validated/@Valid、原生约束与Default/已有MP组逐层校验，非代理处调用现有ValidationUtils；Rule 3：普通class完整Lombok注解；EgonModel PO用@SuperBuilder/callSuper；MapStruct仅映射真实边界；Rule 11：保留获批的单Starter基础组件结构与MP/受管DDL/CQE边界，绝不改业务平台层次。
- Implementation pseudocode:

```java
source SQL1 rows include PENDING/PROCESSING/RETRY_WAIT/SUCCEEDED/DEAD and raw JSON/text headers
run migrate(sourceSchema,batchSize=2,false), assert original 22 columns byte/text-identical, target version0/tenant0/deletedAt null, same legacy id; interrupted batch rerun skips exact same rows
source row modified under concurrent writer or target extra/different row/version>0 -> verified=false/error; maintenance mode blocks enqueue/submitDue/submitMessageIds
```

- Verification contribution: TEST-012；结果类型可编译，行为Service尚缺；测试定义无损验收。
- After this file: 本测试定义该Step的PG-gated数据复制断言；因未启用环境门禁，本轮只编译不运行。

#### File 5 — `CREATE egon-cola-components/egon-cola-component-transactional-outbox-starter/src/test/java/top/egon/cola/component/outbox/migration/OutboxLegacyMigrationServiceTest.java`

- Purpose: 在不开启PG的测试中锁定迁移入口预检，阻止错误source schema、关闭maintenance或非法batch先碰外部资源。
- Symbols: `migrate` unsafe source/mode/batch fail-fast。
- Repository evidence: File3已声明具名Service和签名；业务表在本地单测不需要任何连接。
- Dependencies and consumers: 复用Common ValidationUtils；Mock DDL initializer与Repository证明失败阶段。
- Why now: Step 7第5个文件；Service类型合同存在后先实测参数与maintenance保护，不能以PG skip替代无数据库覆盖。
- Contract/signature changes: 仅新增3个纯单测，不增生产API。
- Input/output and state mapping: 非法schema→OUTBOX_MIGRATION_SOURCE_SCHEMA_INVALID；mode=false→OUTBOX_MIGRATION_MODE_REQUIRED；batch越界→IllegalArgumentException。
- Error and edge behavior: 每个错误均断言DDL initializer及Repository无交互；用实际ValidationUtils初始化；不建立DataSource或事务。
- Standards impact: `MC-SCOPE-001`, `MC-TEST-001`, `MC-VALID-001`。
- Literal rule enforcement: Rule 2：测试覆盖外部schema/batch输入校验和先后顺序；Rule 11：只添加Starter本地测试，不引入架构层。
- Implementation pseudocode:

```java
assert migrate("legacy;drop", 10, false) -> OUTBOX_MIGRATION_SOURCE_SCHEMA_INVALID
assert migrate("legacy_outbox", 10, false) -> OUTBOX_MIGRATION_MODE_REQUIRED
assert migrate("legacy_outbox", 0, false) -> OUTBOX_MIGRATION_BATCH_SIZE_INVALID
verifyNoInteractions(ddlInitializer, repository) for every rejected input
```

- Verification contribution: TEST-012；无数据库证明迁移入口在任何源/目标访问前fail-fast。
- After this file: 3个无PG行为RED已观察，恢复所有预检后同一测试GREEN。

#### File 6 — `MODIFY egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/store/MybatisPlusOutboxStore.java`

- Purpose: migrationService存在后关闭所有默认入队路径。
- Symbols: enqueue maintenance guard。
- Repository evidence: egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/store/MybatisPlusOutboxStore.java Step4正常enqueue。
- Dependencies and consumers: 原OutboxStore签名不变，生产应用须停所有旧进程。
- Why now: Step 7第6个文件；migrationService存在后关闭所有默认入队路径。
- Contract/signature changes: enqueue maintenance guard；migrationMode=true在enqueue最前拒绝；claim与mark入口由Dispatcher gate禁新投递；不丢旧行。
- Input/output and state mapping: migrationMode=true在enqueue最前拒绝；claim与mark入口由Dispatcher gate禁新投递；不丢旧行。
- Error and edge behavior: 原OutboxStore签名不变，生产应用须停所有旧进程；失败不吞异常、不伪造成功，适用断言由TEST-012验证。
- Standards impact: `MC-BEAN-001`, `MC-SCOPE-001`, `MC-TEST-001`, `MC-VALID-001`；本文件按当前组件约定实现并限定在本Step的路径内。
- Literal rule enforcement: Rule 2：按@Validated/@Valid、原生约束与Default/已有MP组逐层校验，非代理处调用现有ValidationUtils；Rule 4：实际业务类用@Slf4j、显式Bean名、@RequiredArgsConstructor及每个final依赖@Qualifier；Rule 11：保留获批的单Starter基础组件结构与MP/受管DDL/CQE边界，绝不改业务平台层次。
- Implementation pseudocode:

```java
@RequiredArgsConstructor class MybatisPlusOutboxStore { @Qualifier("outboxMpStorageProperties") final OutboxMpStorageProperties storageProperties; ... }
enqueue(record): requireNonNull(record); if migrationMode throw OutboxConfigurationException("OUTBOX_MIGRATION_MODE") before technicalContext, ID generation, PO conversion or SQL
otherwise preserve Step4 MP transaction/SSM/idempotency behavior unchanged; never return a synthetic no-op receipt
OutboxLongPrimaryKeyTest asserts zero converter/Repository calls when true; TEST-012 also checks target row count and no post-commit wakeup
```

- Verification contribution: TEST-012；migrationService存在后关闭所有默认入队路径。
- After this file: 已建立 enqueue maintenance guard，后续文件接入/验证前不得声称整个Step完成。

#### File 7 — `MODIFY egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/dispatch/OutboxDispatcher.java`

- Purpose: enqueue门禁还不足以阻止已存消息被worker领走。
- Symbols: submitDue/submitMessageIds maintenance guard。
- Repository evidence: egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/dispatch/OutboxDispatcher.java Step5技术FSM分派。
- Dependencies and consumers: OutboxPoller/OutboxCommittedEventListener都调用这两个入口。
- Why now: Step 7第7个文件；enqueue门禁还不足以阻止已存消息被worker领走。
- Contract/signature changes: submitDue/submitMessageIds maintenance guard；maintenanceMode=true两种入口立即拒绝/不领取；原投递逻辑在false下不变。
- Input/output and state mapping: maintenanceMode=true两种入口立即拒绝/不领取；原投递逻辑在false下不变。
- Error and edge behavior: OutboxPoller/OutboxCommittedEventListener都调用这两个入口；失败不吞异常、不伪造成功，适用断言由TEST-012验证。
- Standards impact: `MC-BEAN-001`, `MC-PATTERN-001`, `MC-SCOPE-001`, `MC-TEST-001`, `MC-VALID-001`；本文件按当前组件约定实现并限定在本Step的路径内。
- Literal rule enforcement: Rule 2：按@Validated/@Valid、原生约束与Default/已有MP组逐层校验，非代理处调用现有ValidationUtils；Rule 4：实际业务类用@Slf4j、显式Bean名、@RequiredArgsConstructor及每个final依赖@Qualifier；Rule 9：复杂迁移由SSM State/Strategy/Adapter实际承担，禁止Dispatcher另写状态switch；Rule 11：保留获批的单Starter基础组件结构与MP/受管DDL/CQE边界，绝不改业务平台层次。
- Implementation pseudocode:

```java
submitDue(): if storage.mp.migrationMode then return without scheduling coordinator or touching Store
submitMessageIds(ids): if maintenance true then return without scheduling; preserve empty/null ids no-op semantics
on false reuse Step5 StateMachine route; verify no claimDue/claimByMessageIds calls under true and shutdown still releases worker
```

- Verification contribution: TEST-012；enqueue门禁还不足以阻止已存消息被worker领走。
- After this file: 已建立 submitDue/submitMessageIds maintenance guard，后续文件接入/验证前不得声称整个Step完成。

#### File 8 — `MODIFY egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/autoconfigure/TransactionalOutboxAutoConfiguration.java`

- Purpose: Dispatcher的新具名maintenance依赖需经Spring Bean图传入唯一配置实例。
- Symbols: `outboxDispatcher(..., outboxMpStorageProperties)`。
- Repository evidence: Step5的具名Dispatcher构造；MP配置以`outboxMpStorageProperties`发布唯一`OutboxMpStorageProperties`。
- Dependencies and consumers: 只传入同一Bean；隔离Core ContextRunner若没有MP属性Bean，Provider默认false以保留既有测试语义。
- Why now: Step 7第8个文件；先让生产装配和旧调用方共同满足新构造合同，再运行Dispatcher行为RED。
- Contract/signature changes: 新增具名`OutboxMpStorageProperties` Provider参数；Dispatcher获得同一`migrationMode`。
- Input/output and state mapping: `storage.mp.migration-mode`的唯一配置源传给Dispatcher；不复制配置，不修改TransactionalOutboxProperties。
- Error and edge behavior: Boot全量自动配置使用MP属性真实Bean；隔离Core ContextRunner没有该bean时默认false；失败不吞异常、不伪造成功，适用断言由TEST-012验证。
- Standards impact: `MC-BEAN-001`, `MC-CONFIG-001`, `MC-SCOPE-001`, `MC-TEST-001`。
- Literal rule enforcement: Rule 4：具名参数使用精确Qualifier并由Lombok构造传播；Rule 7：只读取现有typed配置、不增加profile键；Rule 11：保留Starter边界。
- Implementation pseudocode:

```java
@Bean outboxDispatcher(..., @Qualifier("outboxMpStorageProperties") ObjectProvider<OutboxMpStorageProperties> properties)
  OutboxMpStorageProperties storage = properties.getIfAvailable(OutboxMpStorageProperties::new)
  return new OutboxDispatcher(..., lifecycleService, storage)
full Boot imports provide the existing typed bean; only isolated Core-only ContextRunner defaults to migrationMode=false
do not create another bound properties bean or read the flag from a second prefix
```

- Verification contribution: TEST-012；生产Bean图把同一个typed maintenance属性传递给Dispatcher。
- After this file: 主Bean装配与已有直接构造调用点可编译；Dispatcher维护行为仍应RED。

#### File 9 — `MODIFY egon-cola-components/egon-cola-component-transactional-outbox-starter/src/test/java/top/egon/cola/component/outbox/integration/PostgresqlOutboxRecoveryIntegrationTest.java`

- Purpose: 已有PG recovery fixture直接构造Dispatcher，保持constructor扩展后的编译和正常模式回归。
- Symbols: Dispatcher构造传递默认`OutboxMpStorageProperties`。
- Repository evidence: File 6增加Dispatcher依赖；File 10覆盖maintenance mode行为。
- Dependencies and consumers: 测试fixture只编译，仍由`EGON_OUTBOX_TEST_POSTGRES_ENABLED`门禁。
- Why now: Step 7第9个文件；所有直接构造点都要把正常模式显式留在false。
- Contract/signature changes: 仅增加参数；不更改PG场景或执行方式。
- Input/output and state mapping: 现有Dispatcher/Store测试仍使用默认正常投递。
- Error and edge behavior: 不启动PG/Testcontainers；失败不吞异常、不伪造成功，适用断言由TEST-012验证。
- Standards impact: `MC-SCOPE-001`, `MC-TEST-001`。
- Literal rule enforcement: Rule 10：fixture时间继续使用`java.time`；Rule 11：只维护现有Outbox测试架构。
- Implementation pseudocode:

```java
OutboxMpStorageProperties migration = context.getBean("outboxMpStorageProperties", OutboxMpStorageProperties.class)
new OutboxDispatcher(..., lifecycleService, migration)
keep the inherited EGON_OUTBOX_TEST_POSTGRES_ENABLED assumption unchanged
leave the fixture's configured migrationMode=false so expiry/recovery assertions still run normally
```

- Verification contribution: TEST-012；全仓Dispatcher直接构造调用保持可编译。
- After this file: PG recovery fixture保持maintenance mode关闭，不启动其环境门禁。

#### File 10 — `MODIFY egon-cola-components/egon-cola-component-transactional-outbox-starter/src/test/java/top/egon/cola/component/outbox/integration/BusinessStateMachineIntegrationTest.java`

- Purpose: Step6提交的端到端业务Event fixture必须随Dispatcher构造依赖扩展。
- Symbols: 正常delivery Dispatcher constructor参数。
- Repository evidence: File 6增加`OutboxMpStorageProperties`；本文件在Step6以技术成功回写失败/重投验证业务receipt。
- Dependencies and consumers: 仅测试直接构造点；不启用迁移模式。
- Why now: Step 7第10个文件；维持已提交Step6的集成场景与新构造合同一致。
- Contract/signature changes: 只传入默认`OutboxMpStorageProperties`，migrationMode=false。
- Input/output and state mapping: 现有行为仍由真实Store/Dispatcher/Handler与业务事务验证。
- Error and edge behavior: 测试仍继承PG门禁，不运行；失败不吞异常、不伪造成功，适用断言由TEST-005/012验证。
- Standards impact: `MC-SCOPE-001`, `MC-TEST-001`。
- Literal rule enforcement: Rule 10：时间字段仍为`java.time`；Rule 11：不改测试夹具所有权或生产层次。
- Implementation pseudocode:

```java
OutboxMpStorageProperties migration = context.getBean("outboxMpStorageProperties", OutboxMpStorageProperties.class)
return new OutboxDispatcher(..., lifecycleService, migration)
assert/use the false default; do not set migration mode in this business replay fixture
preserve technical ack failure, lease expiry, replay and durable receipt assertions as submitted in Step6
```

- Verification contribution: TEST-005/012；已提交业务replay测试仍能编译并保持正常投递语义。
- After this file: Step6 integration constructor调用适配Step7依赖，PG仍未运行。

#### File 11 — `MODIFY egon-cola-components/egon-cola-component-transactional-outbox-starter/src/test/java/top/egon/cola/component/outbox/store/OutboxLongPrimaryKeyTest.java`

- Purpose: Store maintenance gate must reject enqueue before converter, Snowflake ID generation, validation context or MP SQL.
- Symbols: migration-mode enqueue rejection in the existing Store unit fixture.
- Repository evidence: File6 adds the typed storage-properties dependency; current file already mocks the MP Repository and converter.
- Dependencies and consumers: no database; the existing helper constructs the Store with the exact false default for previous tests and exposes true only to the new negative case.
- Why now: Step 7第11个文件；constructor compile contracts exist, so the behavior assertion can distinguish missing gate from javac failure.
- Contract/signature changes: update the fixture constructor for `OutboxMpStorageProperties`; add one test that sets migrationMode=true.
- Input/output and state mapping: migration-mode enqueue throws `OutboxConfigurationException` and performs no insert; default false preserves PENDING enqueue behavior.
- Error and edge behavior: null/invalid record behavior stays unchanged; the gate executes before ID allocation and database access.
- Standards impact: `MC-BEAN-001`, `MC-SCOPE-001`, `MC-TEST-001`。
- Literal rule enforcement: Rule 2：复用现有类型边界；Rule 4：测试fixture不新增Spring业务Bean；Rule 11：继续验证现有MP Store。
- Implementation pseudocode:

```java
OutboxMpStorageProperties properties = new OutboxMpStorageProperties().setMigrationMode(true)
assertThatThrownBy(() -> store(repository, converter, lifecycle, tx, properties).enqueue(record(...)))
  .isInstanceOf(OutboxConfigurationException.class)
verifyNoInteractions(converter, repository)
```

- Verification contribution: TEST-012；证明维护模式下不生成新ID、不转换消息、不执行MP写。
- After this file: 新RED断言能编译并在缺少Store gate时失败；GREEN在File5完成。

#### File 12 — `MODIFY egon-cola-components/egon-cola-component-transactional-outbox-starter/src/test/java/top/egon/cola/component/outbox/dispatch/OutboxDispatcherTest.java`

- Purpose: Dispatcher新增维护门禁需RED/回归。
- Symbols: migration mode no-claim assertions。
- Repository evidence: File7 adds the qualified storage property; this existing test already covers Step5 delivery-result routing.
- Dependencies and consumers: 已有Mockito Store/Executor fixture不需要PG。
- Why now: Step 7第12个文件；Dispatcher新增维护门禁需RED/回归。
- Contract/signature changes: migration mode no-claim assertions；true不提交任务、不通知成功、不更新DB；false仍执行原技术状态分派。
- Input/output and state mapping: true不提交任务、不通知成功、不更新DB；false仍执行原技术状态分派。
- Error and edge behavior: 已有Mockito Store/Executor fixture不需要PG；失败不吞异常、不伪造成功，适用断言由TEST-012验证。
- Standards impact: `MC-PATTERN-001`, `MC-SCOPE-001`, `MC-TEST-001`, `MC-VALID-001`；本文件按当前组件约定实现并限定在本Step的路径内。
- Literal rule enforcement: Rule 2：按@Validated/@Valid、原生约束与Default/已有MP组逐层校验，非代理处调用现有ValidationUtils；Rule 9：复杂迁移由SSM State/Strategy/Adapter实际承担，禁止Dispatcher另写状态switch；Rule 11：保留获批的单Starter基础组件结构与MP/受管DDL/CQE边界，绝不改业务平台层次。
- Implementation pseudocode:

```java
with migrationMode=true call dispatcher.submitDue() and submitMessageIds(List.of("msg1"))
verifyNoInteractions(outboxStore, deliveryHandler, metrics delivery counters); verify taskExecutor never scheduled
switch false -> original claim/delivery StateMachine behavior stays green; no change to public Dispatcher API
```

- Verification contribution: TEST-012；Dispatcher新增维护门禁需RED/回归。
- After this file: 已建立 migration mode no-claim assertions，后续文件接入/验证前不得声称整个Step完成。

#### File 13 — `MODIFY egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/autoconfigure/OutboxMybatisPlusAutoConfiguration.java`

- Purpose: 迁移类和Result可编译，此时将显式维护入口注册。
- Symbols: migration Service Bean registration。
- Repository evidence: egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/autoconfigure/OutboxMybatisPlusAutoConfiguration.java Steps2/4已有Bean区块。
- Dependencies and consumers: Step2 DDL hook/Step4 Store Bean方法保持字节/行为不变。
- Why now: Step 7第13个文件；迁移类和Result可编译、Dispatcher具名配置传递已GREEN，此时将显式维护入口注册并将同一个storage配置交给Store。
- Contract/signature changes: migration Service Bean registration；具名outboxLegacyMigrationService只作为本地Bean，不暴露HTTP/调度；sourcePhysical来自已就绪initializer。
- Input/output and state mapping: 具名outboxLegacyMigrationService只作为本地Bean，不暴露HTTP/调度；sourcePhysical来自已就绪initializer。
- Error and edge behavior: Step2 DDL hook/Step4 Store Bean方法保持字节/行为不变；失败不吞异常、不伪造成功，适用断言由TEST-012验证。
- Standards impact: `MC-BEAN-001`, `MC-CONFIG-001`, `MC-SCOPE-001`, `MC-TEST-001`, `MC-VALID-001`；本文件按当前组件约定实现并限定在本Step的路径内。
- Literal rule enforcement: Rule 2：按@Validated/@Valid、原生约束与Default/已有MP组逐层校验，非代理处调用现有ValidationUtils；Rule 4：实际业务类用@Slf4j、显式Bean名、@RequiredArgsConstructor及每个final依赖@Qualifier；Rule 7：配置键对全部profile保持一致，组件无生产application YAML，双语README逐项同步；Rule 11：保留获批的单Starter基础组件结构与MP/受管DDL/CQE边界，绝不改业务平台层次。
- Implementation pseudocode:

```java
new MybatisPlusOutboxStore(..., clock, storageProperties) using the unique qualified storage properties
add @Bean(name="outboxLegacyMigrationService") factory using qualified initializer, repository, converter, worker transaction and technicalContext
validate source physical PRIMARY reference already captured by initializer and same PostgreSQL endpoint as MP target
never auto-call migrate during Bean initialization; enabled=true outbox still supports manual maintenance mode while no worker starts
```

- Verification contribution: TEST-012；迁移类和Result可编译，此时将显式维护入口注册。
- After this file: 已建立 migration Service Bean registration，后续文件接入/验证前不得声称整个Step完成。

- Validation working directory: `/Users/mario/SelfProject/Egon-COLA`。
- Verification command: `mvn -pl egon-cola-components/egon-cola-component-transactional-outbox-starter -am -DfailIfNoTests=false -Dsurefire.failIfNoSpecifiedTests=false -Dtest=OutboxLegacyMigrationServiceTest,OutboxDispatcherTest,OutboxLongPrimaryKeyTest,OutboxMessageConverterTest test`。
- Expected result: exit 0，指定TEST-012相关非集成单测实际执行且全部通过；若涉及integration目录，在未获得PG/Testcontainers授权时标为未验证，不能以skip替代真实GREEN。
- Failure returns to: 本Step的第1个RED文件，定位测试/夹具错误；编译/行为失败返回对应生产文件；DDL/依赖不兼容返回Step 1或Step 2并暂停后续。
- Completion criteria: 维护模式与只读源复制核对可用；无真实生产迁移执行；本Step适用Manual Checks已按实际静态/单测证据复核；禁止把未授权的PG运行写为PASS。
- Rollback: 限定回退本Step提交路径；若已切到新MP表并产生消息，必须遵守主Spec §16停写/核对/前向修复，不能直接启用旧JDBC。
- Commit paths: `egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/migration/OutboxMigrationResult.java` `egon-cola-components/egon-cola-component-transactional-outbox-starter/src/test/java/top/egon/cola/component/outbox/integration/OutboxMpMigrationIntegrationTest.java` `egon-cola-components/egon-cola-component-transactional-outbox-starter/src/test/java/top/egon/cola/component/outbox/migration/OutboxLegacyMigrationServiceTest.java` `egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/persistence/OutboxTechnicalContextExecutor.java` `egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/migration/OutboxLegacyMigrationService.java` `egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/store/MybatisPlusOutboxStore.java` `egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/dispatch/OutboxDispatcher.java` `egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/autoconfigure/TransactionalOutboxAutoConfiguration.java` `egon-cola-components/egon-cola-component-transactional-outbox-starter/src/test/java/top/egon/cola/component/outbox/store/OutboxLongPrimaryKeyTest.java` `egon-cola-components/egon-cola-component-transactional-outbox-starter/src/test/java/top/egon/cola/component/outbox/dispatch/OutboxDispatcherTest.java` `egon-cola-components/egon-cola-component-transactional-outbox-starter/src/test/java/top/egon/cola/component/outbox/integration/PostgresqlOutboxRecoveryIntegrationTest.java` `egon-cola-components/egon-cola-component-transactional-outbox-starter/src/test/java/top/egon/cola/component/outbox/integration/BusinessStateMachineIntegrationTest.java` `egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/autoconfigure/OutboxMybatisPlusAutoConfiguration.java`
- Commit: `feat(outbox): add verified legacy migration mode`。

#### Step 7 pre-commit evidence

| Literal rule | Applicability | Status | Diff/path/symbol evidence | Test/static evidence | Finding | Required action/exception |
| --- | --- | --- | --- | --- | --- | --- |
| Rule 1 | Applicable | PASS | `OutboxMigrationResult`, `OutboxLegacyMigrationService`, private `LegacyOutboxRowBO`/`VerificationBO`/`CopyCountsBO`/`BatchCopyCountsBO` | Java 21 test compilation | Result is a named mutable BO; internal row/count records are immutable values | None |
| Rule 2 | Applicable | PASS | Service `migrate`, `validateRequest`, `validatedHeaders`, `ValidationUtils.validate(row/result/properties)` | `OutboxLegacyMigrationServiceTest` negative schema/mode/batch tests; `OutboxMessageConverterTest` malformed-header tests | Unsafe schema and invalid maintenance inputs fail before any datasource or Repository access | PG row-copy behavior remains runtime-unverified |
| Rule 3 | Applicable | PASS | `OutboxMigrationResult` Lombok model; immutable private BO records; existing `OutboxMessageConverter.toMigrationPO` | `OutboxMessageConverterTest.mapsNewAndMigratedMessagesWithoutInventingAuditOrRewritingRawFields`; generated MapStruct source compiled | No manual PO field copying was added; `EgonModel` is reused through the existing PO | None |
| Rule 4 | Applicable | PASS | Named `@Bean("outboxLegacyMigrationService")`; `@Slf4j`, `@Validated`, `@RequiredArgsConstructor`; every Service and Store/Dispatcher dependency carries an exact `@Qualifier` | 36 ContextRunner/unit tests across MP, transactional, business-state-machine, optional-handler contexts | Same `OutboxMpStorageProperties` instance reaches enqueue and dispatch gates | None |
| Rule 5 | Applicable | PASS | New production imports use JDK JDBC/time/collections plus existing Spring and Egon components | dependency/POM diff is empty; import scan | No utility or dependency added | None |
| Rule 6 | Applicable | PASS | `OutboxHeadersConverter.parseHeaders` validates Boot-Jackson string maps; migration writes the raw `headers_json` | `OutboxMessageConverterTest` covers raw-header preservation, malformed JSON and non-string headers | No JSON rewrite is used for persistence | None |
| Rule 7 | Not applicable | N/A | No profile or configuration key changed; existing `storage.mp.migration-mode` is injected as typed `OutboxMpStorageProperties` | Config diff/static inspection | Existing property contract is reused | None |
| Rule 9 | Applicable | PASS | Dispatcher keeps `OutboxLifecycleService` as the only delivery-state decision; the new mode guard exits before dispatch | `OutboxDispatcherTest.migrationModeDoesNotScheduleOrTouchTheStoreFromEitherEntryPoint`; StateMachine tests from Step5 remain unchanged | No second status switch was introduced | None |
| Rule 10 | Applicable | PASS | `Duration`, `Instant`, `OffsetDateTime`; source timestamps map to `Instant` | forbidden-date import scan had no matches; PG timestamp comparison is present in gated test | No `java.util.Date`, `Calendar`, or `SimpleDateFormat` | None |
| Rule 11 | Applicable | PASS | All production/test paths stay under the transactional-outbox Starter; source Repository and DDL hooks are reused | exact staged path list and `git diff --check` | No business platform layer or module direction changed | None |

| Check ID | Applicability | Status | Evidence | Finding | Required action/exception |
| --- | --- | --- | --- | --- | --- |
| MC-ARCH-001 | Applicable | PASS | 13 declared Step7 implementation paths remain under the existing Starter | Original component architecture is preserved | None |
| MC-REUSE-001 | Applicable | PASS | Existing `OutboxManagedDdlInitializer`, `OutboxMessageRepository`, `OutboxMessageConverter`, MP Store and StateMachine properties are reused | Target writes use MP; JDBC is limited to the legacy source connection | None |
| MC-DEP-001 | Applicable | PASS | Step7 diff contains no POM or BOM edits | No dependency gap or added dependency | None |
| MC-NAME-001 | Applicable | PASS | New declarations use `OutboxMigrationResult`, `OutboxLegacyMigrationService`, and semantic `*BO` nested values | `mvn ... test-compile` passed | No ambiguous carrier type added | None |
| MC-VALID-001 | Applicable | PASS | Service annotations and manual source/result/property validation; safe SQL identifier pattern | 3 fail-fast tests plus converter header validation tests passed | PG source-row validation is covered by code and a gated test, not a runtime claim |
| MC-MODEL-001 | Applicable | PASS | Result is a Lombok class; source/readback BO records are immutable values; existing PO not changed | Mapping test and module compilation passed | None |
| MC-CONVERT-001 | Applicable | PASS | Existing MapStruct `OutboxMessageConverter.toMigrationPO` and `BaseForwardConverter` implementation | `OutboxMessageConverterTest` migration projection passed | Raw payload/header strings remain unchanged | None |
| MC-LOG-001 | Applicable | PASS | Migration Service uses `@Slf4j` and logs only schema/count/result fields | diff inspection | No payload/header or credential logging | None |
| MC-BEAN-001 | Applicable | PASS | Stable `outboxLegacyMigrationService` bean; exact qualified constructor dependencies | ContextRunner suite passed | None |
| MC-UTIL-001 | Applicable | PASS | JDK `java.sql`, collections and time only; existing Jackson/Validation components are reused | dependency/import diff review | No duplicate utility class | None |
| MC-JSON-001 | Applicable | PASS | Existing Boot-Jackson header parser used by migration; raw header bytes preserved | `OutboxMessageConverterTest` passed | No alternate JSON stack | None |
| MC-TIME-001 | Applicable | PASS | Source JDBC timestamps use `OffsetDateTime` and `Instant`; lock budget uses `Duration` | module compilation and PG test compilation passed | Runtime timestamp precision comparison remains PG-gated | None |
| MC-CONFIG-001 | Not applicable | N/A | No configuration key or environment profile changed | config diff is empty | Existing migration-mode key is reused | None |
| MC-PATTERN-001 | Applicable | PASS | Dispatcher keeps the existing State-machine target routing; migration is an explicit Repository/transaction orchestration | State routing tests and migration fail-fast tests passed | Precondition `if` does not duplicate state transitions | None |
| MC-SCOPE-001 | Applicable | PASS | Staged path list matches the 13 Step7 paths; no other-Agent paths are staged | `git diff --cached --check` passed | Plan/Spec and concurrent work remain outside the code commit | None |
| MC-TEST-001 | Applicable | PASS | Focused command ran 24 tests: Service preflight 3, Dispatcher 10, Store 6, Converter 5; full testCompile compiled PG tests | 0 failures, 0 skips; strict Plan validation passed | PostgreSQL/Testcontainers tests are intentionally not run | None |
| MC-BLOCKER-001 | Applicable | PASS | All applicable rows above pass or have evidence-backed N/A | No in-scope code/test blocker remains; PG runtime is explicitly gated | Report PostgreSQL migration and production cutover as unverified | None |

### Step 8 — 冻结接入文档与最终回归门禁

- Requirements: REQ-003 REQ-007 REQ-008 REQ-009 REQ-010
- Dependencies: Step 7
- Baseline state: Step 7提交后，维护模式与只读源复制核对可用；无真实生产迁移执行。
- Observable outcome: 冻结接入文档与最终回归门禁；验收对应TEST-008/009/011/012/013。
- End state: Plan要求的全部代码、文档、静态/模块门禁闭合；PG/SS真实验收仍需授权运行。
- Test-first gate: Required — 治理测试对双层SSM/MP/旧SQL/双语配置的合同约束尚未全部成立。现有Maven/ContextRunner/validator覆盖本轮门禁，无需创建scripts/work临时脚本。
- Manual Checks: `MC-ARCH-001`, `MC-BEAN-001`, `MC-CONFIG-001`, `MC-JSON-001`, `MC-MODEL-001`, `MC-NAME-001`, `MC-PATTERN-001`, `MC-SCOPE-001`, `MC-TEST-001`, `MC-TIME-001`, `MC-UTIL-001`, `MC-VALID-001`。在本Step完成前以本段文件和命令逐项核对，已知失败不得延后到后续清理。
- Literal Rules: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11。每个文件如下展开；Rule 11覆盖全部路径。
- Ordered files:

#### File 1 — `MODIFY egon-cola-components/egon-cola-component-transactional-outbox-starter/src/test/java/top/egon/cola/component/outbox/contract/ComponentContractGovernanceTest.java`

- Purpose: Step4移除JDBC引用后，本次增最终静态约束而不重做旧有效断言。
- Symbols: final dual-SSM/MP/CQE policy assertions。
- Repository evidence: egon-cola-components/egon-cola-component-transactional-outbox-starter/src/test/java/top/egon/cola/component/outbox/contract/ComponentContractGovernanceTest.java已有静态/反射治理检查。
- Dependencies and consumers: 已存在治理测试；README双语与POM/Mapper XML可被静态读取。
- Why now: Step 8的第1个文件；Step4移除JDBC引用后，本次增最终静态约束而不重做旧有效断言。
- Contract/signature changes: final dual-SSM/MP/CQE policy assertions；禁止运行JDBC Store/第二DDL/ordinal；Bean/Qualifier、只一新增SQL版本、旧SQL1字节不变。
- Input/output and state mapping: 禁止运行JDBC Store/第二DDL/ordinal；Bean/Qualifier、只一新增SQL版本、旧SQL1字节不变。
- Error and edge behavior: 已存在治理测试；README双语与POM/Mapper XML可被静态读取；失败不吞异常、不伪造成功，适用断言由TEST-008/009/011/012/013验证。
- Standards impact: `MC-BEAN-001`, `MC-CONFIG-001`, `MC-JSON-001`, `MC-MODEL-001`, `MC-NAME-001`, `MC-PATTERN-001`, `MC-SCOPE-001`, `MC-TEST-001`, `MC-TIME-001`, `MC-UTIL-001`；本文件按当前组件约定实现并限定在本Step的路径内。
- Literal rule enforcement: Rule 1：类型名按Event/BO/PO/DAO/Repository/Service/Factory等语义后缀，不新增Data/Info/Param/Bean载体；Rule 3：普通class完整Lombok注解；EgonModel PO用@SuperBuilder/callSuper；MapStruct仅映射真实边界；Rule 4：实际业务类用@Slf4j、显式Bean名、@RequiredArgsConstructor及每个final依赖@Qualifier；Rule 5：只用JDK/已管理Commons/Guava，不新增工具库或BeanUtils对象拷贝；Rule 6：Spring Boot Jackson和@EnumValue/@JsonFormat；永不以ordinal作业务编码；Rule 7：配置键对全部profile保持一致，组件无生产application YAML，双语README逐项同步；Rule 9：复杂迁移由SSM State/Strategy/Adapter实际承担，禁止Dispatcher另写状态switch；Rule 10：使用Instant/Duration/Clock与PG微秒，禁止Date/Calendar/SimpleDateFormat；Rule 11：保留获批的单Starter基础组件结构与MP/受管DDL/CQE边界，绝不改业务平台层次。
- Implementation pseudocode:

```java
assert module contains StateMachineExecutionService and MybatisPlusOutboxStore, not PostgresqlJdbcOutboxStore runtime class
assert single SQL2 and manifest SHA-256 bytes, original SQL1 unchanged, exact Common family addition and semantic PO/DAO/Event suffixes
assert explicit named Bean/qualified injection, no StateMachine Action JDBC, no new HTTP/GraphQL/controller or business app module
```

- Verification contribution: TEST-008/009/011/012/013；Step4移除JDBC引用后，本次增最终静态约束而不重做旧有效断言。
- After this file: 已建立 final dual-SSM/MP/CQE policy assertions，后续文件接入/验证前不得声称整个Step完成。

#### File 2 — `MODIFY egon-cola-components/egon-cola-component-transactional-outbox-starter/README.zh-CN.md`

- Purpose: 最终运行和维护模式路径已实现，写准确消费方指引。
- Symbols: 中文接入/迁移/运维合同。
- Repository evidence: egon-cola-components/egon-cola-component-transactional-outbox-starter/README.zh-CN.md旧JDBC/事务/测试说明。
- Dependencies and consumers: 英文README下一文件逐项同步；不写任何环境地址或凭据。
- Why now: Step 8的第2个文件；最终运行和维护模式路径已实现，写准确消费方指引。
- Contract/signature changes: 中文接入/迁移/运维合同；依赖SSM/MP、NATIVE SINGLE同PG、cache关闭条件、mapper-locations、DDL先于logical、维护停写/核对/回滚。
- Input/output and state mapping: 依赖SSM/MP、NATIVE SINGLE同PG、cache关闭条件、mapper-locations、DDL先于logical、维护停写/核对/回滚。
- Error and edge behavior: 英文README下一文件逐项同步；不写任何环境地址或凭据；失败不吞异常、不伪造成功，适用断言由TEST-008/009/011/012/013验证。
- Standards impact: `MC-CONFIG-001`, `MC-JSON-001`, `MC-SCOPE-001`, `MC-TEST-001`, `MC-VALID-001`；本文件按当前组件约定实现并限定在本Step的路径内。
- Literal rule enforcement: Rule 2：按@Validated/@Valid、原生约束与Default/已有MP组逐层校验，非代理处调用现有ValidationUtils；Rule 6：Spring Boot Jackson和@EnumValue/@JsonFormat；永不以ordinal作业务编码；Rule 7：配置键对全部profile保持一致，组件无生产application YAML，双语README逐项同步；Rule 11：保留获批的单Starter基础组件结构与MP/受管DDL/CQE边界，绝不改业务平台层次。
- Implementation pseudocode:

```text
document Command under caller TX -> TransactionalOutbox.enqueue(Event fact), Query read-only, business definition/repository receipt idempotence
show exact four FSM keys and four MP keys, tenant ignored-table merge, one PRIMARY/NATIVE/LOCAL, schema/manifest/SQL2, test env gate
state old SQL1/read-only migration, SHARE lock, verified counts/differences, before/after cutover rollback, old JDBC concrete constructor migration
```

- Verification contribution: TEST-008/009/011/012/013；最终运行和维护模式路径已实现，写准确消费方指引。
- After this file: 已建立 中文接入/迁移/运维合同，后续文件接入/验证前不得声称整个Step完成。

#### File 3 — `MODIFY egon-cola-components/egon-cola-component-transactional-outbox-starter/README.md`

- Purpose: 中文说明先定稿，再对照复制同一技术字段/键。
- Symbols: English mirror of the same contract。
- Repository evidence: egon-cola-components/egon-cola-component-transactional-outbox-starter/README.md原英文投递/配置文档。
- Dependencies and consumers: 外部组件消费者；只有文档不改生产API。
- Why now: Step 8的第3个文件；中文说明先定稿，再对照复制同一技术字段/键。
- Contract/signature changes: English mirror of the same contract；同一Bean名/依赖/DDL路径/错误/StateMachine Event示例；无差异键。
- Input/output and state mapping: 同一Bean名/依赖/DDL路径/错误/StateMachine Event示例；无差异键。
- Error and edge behavior: 外部组件消费者；只有文档不改生产API；失败不吞异常、不伪造成功，适用断言由TEST-008/009/011/012/013验证。
- Standards impact: `MC-CONFIG-001`, `MC-JSON-001`, `MC-SCOPE-001`, `MC-TEST-001`；本文件按当前组件约定实现并限定在本Step的路径内。
- Literal rule enforcement: Rule 6：Spring Boot Jackson和@EnumValue/@JsonFormat；永不以ordinal作业务编码；Rule 7：配置键对全部profile保持一致，组件无生产application YAML，双语README逐项同步；Rule 11：保留获批的单Starter基础组件结构与MP/受管DDL/CQE边界，绝不改业务平台层次。
- Implementation pseudocode:

```text
mirror README.zh-CN state names, schemaVersion=1, machineKey:definitionVersion, retained at-least-once contract
match every egon.cola.component.transactional-outbox.state-machine and storage.mp config key, NATIVE two SINGLE routes, migration order
assert no old default JDBC-only setup instructions remain and no claim of live PG/SS validation in Plan-only work
```

- Verification contribution: TEST-008/009/011/012/013；中文说明先定稿，再对照复制同一技术字段/键。
- After this file: 已建立 English mirror of the same contract，后续文件接入/验证前不得声称整个Step完成。

- Validation working directory: `/Users/mario/SelfProject/Egon-COLA`。
- Verification command: `mvn -pl egon-cola-components/egon-cola-component-transactional-outbox-starter -am -DfailIfNoTests=false test`。
- Expected result: exit 0，指定TEST-008/009/011/012/013相关非集成单测实际执行且全部通过；若涉及integration目录，在未获得PG/Testcontainers授权时标为未验证，不能以skip替代真实GREEN。
- Failure returns to: 本Step的第1个RED文件，定位测试/夹具错误；编译/行为失败返回对应生产文件；DDL/依赖不兼容返回Step 1或Step 2并暂停后续。
- Completion criteria: Plan要求的全部代码、文档、静态/模块门禁闭合；PG/SS真实验收仍需授权运行；本Step适用Manual Checks已按实际静态/单测证据复核；禁止把未授权的PG运行写为PASS。
- Rollback: 限定回退本Step提交路径；若已切到新MP表并产生消息，必须遵守主Spec §16停写/核对/前向修复，不能直接启用旧JDBC。
- Commit paths: `egon-cola-components/egon-cola-component-transactional-outbox-starter/src/test/java/top/egon/cola/component/outbox/contract/ComponentContractGovernanceTest.java` `egon-cola-components/egon-cola-component-transactional-outbox-starter/README.zh-CN.md` `egon-cola-components/egon-cola-component-transactional-outbox-starter/README.md`
- Commit: `docs(outbox): document CQE and MP cutover contracts`。

### Step 9 — 使PG领取、恢复与清理锁协议兼容ShardingSphere parser

- Requirements: REQ-001, REQ-004, REQ-008, REQ-009, REQ-010
- Dependencies: 原Steps 1–8均有提交；主Spec DEC-009与本Plan PLAN-CLAR-021经用户2026-09-27明确批准。
- Baseline state: main @ 521b8d8639d37026ebf06c11847c2bc3ed8a502a；当前Outbox路径存在本Corrective Step已启动、未提交的PG验收纠正。其它agent改动与未跟踪文件保持不动。
- Observable outcome: Claim/Reclaim/Cleanup通过Common MP 5.4.1 / Shardingsphere 5.5.3 Mapper parser；PG并发不重复claim/delete；锁竞争可短批并由下一次poll补足。
- End state: Mapper无FOR UPDATE SKIP LOCKED；候选窗口min(10000, requestedLimit*4)；worker事务内try advisory lock后执行version/state/tenant/active/due或retention CAS；返回/删除不超请求量；没有新DDL、依赖、公开Store SPI。
- Test-first gate: Required — 原PostgreSQL claim集成测试先复现parser拒绝；DAO/Repository仅先建立编译合同；然后真实PG竞争用例在Mapper/Store实现前RED，锁释放与CAS语义GREEN。
- Manual Checks: MC-ARCH-001, MC-REUSE-001, MC-DEP-001, MC-NAME-001, MC-VALID-001, MC-MODEL-001, MC-CONVERT-001, MC-LOG-001, MC-BEAN-001, MC-UTIL-001, MC-JSON-001, MC-TIME-001, MC-CONFIG-001, MC-PATTERN-001, MC-SCOPE-001, MC-TEST-001, MC-BLOCKER-001。提交前每项独立复核。
- Literal Rules: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11。
- Ordered files:

- Lock protocol: unique PRIMARY NATIVE SINGLE outbox means all worker transactions enter the same PostgreSQL advisory lock domain. Mapper candidate SELECT is non-locking; each positive Snowflake id uses a target-table-routed SELECT pg_try_advisory_xact_lock(id) with id/tenant0/active predicates, held until worker transaction commit/rollback. On success SSM evaluates source and CAS repeats id/version/source/tenant0/deleted_at and due/lease predicates. Cleanup reuses the same lock and deletes only id/version/SUCCEEDED/tenant0/active/retention matches. False lock or CAS 0 skips one candidate; actual engine/Mapper errors roll back the whole transaction.


#### File 1 — `MODIFY docs/egon/spec/2026-09-24-12-00-outbox-statemachine-cqe-integration.md`

- Purpose: 批准并冻结PG parser兼容锁协议，保证实现有唯一规范真源。
- Symbols: EVD-019, DEC-009, §7.3.1, §7.3.4, §11.2.1, TEST-003, §15.2
- Repository evidence: 当前Spec明确写FOR UPDATE SKIP LOCKED；用户最新明确批准bounded candidates、PG transaction advisory lock与CAS。
- Dependencies and consumers: Plan、DAO/Repository/Mapper/Store及并发PG测试全部依赖此锁合同。
- Why now: 修订遵循Spec先于Plan/代码的顺序，并记录运行时发现与批准边界。
- Contract/signature changes: 维持REQ编号、OutboxStore public SPI、schema/DDL不变；内部映射改为非锁候选读取、pg_try_advisory_xact_lock(id)、CAS。
- Input/output and state mapping: 候选窗口min(10000, requestedLimit*4)；只claim到requestedLimit；cleanup按同一id锁和versioned delete；锁竞争可短批。
- Error and edge behavior: try-lock=false或CAS=0跳过；数据库/状态机异常仍回滚完整worker事务；租约与旧owner语义不变。
- Standards impact: MC-ARCH-001, MC-REUSE-001, MC-SCOPE-001, MC-TEST-001；修订只限批准的technical SQL lock behavior。
- Literal rule enforcement: Rule 11 — 保留单Starter、MP mapper与受管DDL架构；不加表/列/索引。
- Implementation pseudocode:

```java
replace the approved SKIP LOCKED lock shape with bounded candidate plus transaction advisory lock and CAS
preserve claim lifecycle, tenant-zero predicates and migration schema
assert TEST-003 identifies concurrent claim, cleanup and rollback expectations
```

- Verification contribution: Spec strict validator；rg审查所有claim与cleanup锁用语。
- After this file: Spec DEC-009成为Step9实现合同；其它需求和非目标保持不变。

#### File 2 — `MODIFY docs/egon/plan/2026-09-24-13-16-outbox-statemachine-mp-implementation.md`

- Purpose: 登记经批准的Step9、精确文件顺序、验证命令和已授权隔离运行时范围。
- Symbols: Plan metadata, §4 dependencies, §5 tree, PLAN-CLAR-021, Step 9, §8, §10–12
- Repository evidence: 原计划八步已提交；PG实际执行触发parser问题，当前19个Outbox路径属于获批的Step9纠正范围。
- Dependencies and consumers: Spec File 1；所有旧Steps 1–8 commits；Step9 path-limited gate。
- Why now: Plan在生产行为修改前声明纠正路径与测试顺序。
- Contract/signature changes: 新增一个纠正Step，未创建public API或schema；提交包括Spec/Plan与Step9 exact code paths。
- Input/output and state mapping: 将并发测试放在XML/Store行为之前；先合同编译，再观察PG RED，然后GREEN。
- Error and edge behavior: 验证测试、scope或Manual/Literal gate未闭合则Step保持In Progress，不允许提交或宣称PASS。
- Standards impact: MC-SCOPE-001, MC-TEST-001, MC-BLOCKER-001；仅审查既有Starter文件和Spec/Plan。
- Literal rule enforcement: Rule 11 — 改动限定在Outbox规范与计划，不改变项目架构。
- Implementation pseudocode:

```java
insert Step 9 after Step 8 and before Chapter 8
list each corrective path, exact red/green command, and commit ownership
record the approval response and parser evidence without changing REQ IDs
```

- Verification contribution: Plan strict validator；对照Step文件路径与最终git diff。
- After this file: 后续执行可以逐条按修订后的Step9复现。

#### File 3 — `MODIFY egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/persistence/dao/OutboxMessageDAO.java`

- Purpose: 给候选查询、事务锁和versioned cleanup声明明确mapper编译合同。
- Symbols: selectDueCandidates, selectByMessageIdsCandidates, tryAcquireTransactionLock, selectExpiredSucceededCandidates, deleteSucceededByIdVersion
- Repository evidence: 现有@Validated DAO已有selectDueForUpdate与deleteSucceededByIds，Mapper XML由其方法名绑定。
- Dependencies and consumers: Repository delegates；XML statement names；Store worker transaction callers。
- Why now: 先使新增PG行为测试可编译，之后测试仍因mapper/行为缺失RED。
- Contract/signature changes: tryAcquireTransactionLock(@Param("id") @Positive long id): boolean；候选查询接收bounded limit；delete按id/version/retention接收标量参数。
- Input/output and state mapping: DAO参数约束保留Min/Max/Positive/NotEmpty与tenant0边界；不暴露新public Store方法。
- Error and edge behavior: 空messageIds不执行Mapper；无映射statement在RED阶段必须失败而不是mock出成功。
- Standards impact: MC-NAME-001, MC-VALID-001, MC-SCOPE-001, MC-TEST-001。
- Literal rule enforcement: Rule 1, Rule 2, Rule 11 — DAO语义名称不改；边界参数继续Jakarta约束；维持Starter结构。
- Implementation pseudocode:

```java
declare bounded candidate mapper signatures with positive limits
boolean tryAcquireTransactionLock(long id) maps one transaction-scoped PostgreSQL lock result
declare deleteSucceededByIdVersion with explicit id, version and retention inputs
```

- Verification contribution: module testCompile；新增并发PG测试只通过真实Repository/DAO合同调用。
- After this file: Java调用点可编译；Mapper运行时statement尚不存在，预期RED。

#### File 4 — `MODIFY egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/persistence/repository/OutboxMessageRepository.java`

- Purpose: 为DAO新增查询/锁/删除合同提供唯一Egon Repository delegate。
- Symbols: candidate select delegates, tryAcquireTransactionLock(long), deleteSucceededByIdVersion
- Repository evidence: 现有Repository通过qualified final baseMapper委托命名DAO SQL，并执行batch limit/validation。
- Dependencies and consumers: DAO File 3 mapper methods；MybatisPlusOutboxStore callers。
- Why now: Store只依赖Repository，不应绕过Egon MP层直接访问Mapper。
- Contract/signature changes: 新增有界delegate；锁返回boolean；删除返回0或1并经requireAtMostOne检查。
- Input/output and state mapping: 原请求limit转换成candidateLimit由Store计算；Repository仅校验1..10000且保留messageIds empty fast path。
- Error and edge behavior: 参数违反约束立即拒绝；Mapper异常透传至Store transaction boundary。
- Standards impact: MC-REUSE-001, MC-NAME-001, MC-VALID-001, MC-BEAN-001, MC-SCOPE-001, MC-TEST-001。
- Literal rule enforcement: Rule 1, Rule 2, Rule 4, Rule 11 — 保留DAO/Repository语义分层、Validation与具名qualified构造注入。
- Implementation pseudocode:

```java
validate candidateLimit in the existing Repository batch guard
delegate tryAcquireTransactionLock to qualified baseMapper and return the database boolean
require at most one affected row for versioned cleanup delete
```

- Verification contribution: focused unit compile and actual PostgreSQL integration through Store.
- After this file: Store has one approved MP access path for candidates, locks and deletes.

#### File 5 — `MODIFY egon-cola-components/egon-cola-component-transactional-outbox-starter/src/test/java/top/egon/cola/component/outbox/integration/PostgresqlOutboxConcurrencyIntegrationTest.java`

- Purpose: 先固定同一消息advisory lock的事务可见性与claim竞争结果。
- Symbols: shouldClaimDisjointBatchesAndReleaseDatabaseLocksBeforeDelivery, new transaction-scoped lock contention test
- Repository evidence: 现有两worker测试用CyclicBarrier验证200条disjoint claim并确认Store事务提交后无行锁。
- Dependencies and consumers: DAO/Repository method signatures; existing real Shardingsphere DataSource and TransactionTemplate.
- Why now: 行为用例在Mapper/Store实现前运行，确认旧parser及缺少mapped behavior的RED。
- Contract/signature changes: 无生产API改动；只增加运行时竞争断言。
- Input/output and state mapping: 事务A锁定一个due id并等待latch；事务B claim返回空；A commit释放锁；后续claim只成功一次且attempt/version为1。
- Error and edge behavior: Latch/Executor有界等待；任何超时、SQL parse error或重复owner使测试失败。
- Standards impact: MC-ARCH-001, MC-REUSE-001, MC-SCOPE-001, MC-TEST-001。
- Literal rule enforcement: Rule 2, Rule 10, Rule 11 — 测试实际PG事务和java.time期限，文件仍在integration测试路径。
- Implementation pseudocode:

```java
start TransactionTemplate A and obtain repository.tryAcquireTransactionLock(id)
while A holds the latch assert store.claimDue(1, workerB, lease) is empty
commit A then assert workerB claims the row once and persisted attempt/version equal one
```

- Verification contribution: Failsafe PostgresqlOutboxConcurrencyIntegrationTest against PostgreSQL 16.6 through Shardingsphere.
- After this file: RED establishes lock collision/release criteria without sleep-based timing.

#### File 6 — `MODIFY egon-cola-components/egon-cola-component-transactional-outbox-starter/src/test/java/top/egon/cola/component/outbox/integration/PostgresqlOutboxCleanupIntegrationTest.java`

- Purpose: 证明两个cleanup worker不会重复删除同一expired SUCCEEDED行。
- Symbols: shouldDeleteOnlyOldSuccessAndEndOnlyItsDeduplicationWindow, new concurrent cleanup test
- Repository evidence: 现有类校验保留期、状态过滤和删除后去重窗口。
- Dependencies and consumers: Repository try-lock/versioned delete；store.deleteSucceeded。
- Why now: 同一受保护行的另一类SKIP LOCKED消费者也需实证。
- Contract/signature changes: 只增PG测试，不改清理公开API。
- Input/output and state mapping: 并发cleaner A持事务锁时B不删除；释放后单一DELETE影响1行；双调用总delete数为1。
- Error and edge behavior: DEAD/recent/pending状态永不删除；PG锁或CAS异常不能被测试吞掉。
- Standards impact: MC-ARCH-001, MC-REUSE-001, MC-SCOPE-001, MC-TEST-001。
- Literal rule enforcement: Rule 2, Rule 10, Rule 11 — 真实MP/PG事务校验保留期和时间精度，不更改架构。
- Implementation pseudocode:

```java
arrange one old SUCCEEDED row and retain its id/version
hold its transaction advisory lock while cleanup worker B runs and assert zero rows deleted
release the lock, run cleanup again, and assert exactly one delete with all non-target rows preserved
```

- Verification contribution: Failsafe PostgresqlOutboxCleanupIntegrationTest; inspect per-test count and database state.
- After this file: Cleanup concurrency expectation is explicit before mapper implementation.

#### File 7 — `MODIFY egon-cola-components/egon-cola-component-transactional-outbox-starter/src/test/java/top/egon/cola/component/outbox/persistence/OutboxMessageConverterTest.java`

- Purpose: 将MyBatis XML治理断言从旧SKIP LOCKED方法名更新到获批候选/锁/CAS statement集合。
- Symbols: mapperXmlBuildsItsExplicitStatementsWithoutSoftDeleteForTheTechnicalTable。
- Repository evidence: 现有单测使用XMLMapperBuilder加载真实OutboxMessageMapper.xml并验证必需statement及禁止软删除。
- Dependencies and consumers: DAO新方法名和生产Mapper XML；无数据库运行依赖。
- Why now: DAO编译合同及两条PG竞争RED完成后，先以轻量单测证明候选Mapper缺失为RED，再更新SQL。
- Contract/signature changes: 断言selectDueCandidates、selectByMessageIdsCandidates、tryAcquireTransactionLock、selectExpiredSucceededCandidates、deleteSucceededByIdVersion全部存在；保留insert、claim/update与软删禁止断言。
- Input/output and state mapping: 仅检查Statement存在性；不会将XML存在等同于ShardingSphere/PG实际解析。
- Error and edge behavior: 新statement缺失使单测断言失败；数据库/PG路由正确性仍由Failsafe另证。
- Standards impact: MC-SCOPE-001, MC-TEST-001；无PO、Jackson或配置文件改动。
- Literal rule enforcement: Rule 11 — 测试仍在原Starter package/tree；Rule 2 — 输入约束由真实Mapper合同保持，不绕过验证。
- Implementation pseudocode:

```java
parse OutboxMessageMapper.xml into the existing MyBatis Configuration
assert every bounded candidate, advisory lock, claim update and versioned cleanup statement exists
assert soft delete remains unsupported and legacy SKIP LOCKED statement ids are absent
```

- Verification contribution: OutboxMessageConverterTest GREEN proves mapper id contract; PG suite separately proves parser behavior.
- After this file: unit statement contract matches the Spec and remains honest about runtime proof.

#### File 8 — `MODIFY egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/resources/mapper/outbox/OutboxMessageMapper.xml`

- Purpose: 以parser兼容的候选查询与条件DML替换SKIP LOCKED，并保持可空幂等键查询可由PostgreSQL可靠绑定。
- Symbols: selectExistingByIdentity, selectDueCandidates, selectByMessageIdsCandidates, tryAcquireTransactionLock, updateClaim, selectExpiredSucceededCandidates, deleteSucceededByIdVersion
- Repository evidence: 三个既有SQL段含FOR UPDATE SKIP LOCKED；当前plain FOR UPDATE owner query已能被parser接受。
- Dependencies and consumers: DAO/Repository contracts; PostgreSQL outbox table and existing claim/cleanup indexes.
- Why now: 测试RED已固定，XML是第一个生产行为与SS parser的接口；PG迁移测试还证实`? IS NOT NULL`在null idempotencyKey时造成unknown parameter type，直接`idempotency_key = ?`保留NULL不匹配语义并通过数据库绑定。
- Contract/signature changes: 使用显式列与scalar parameters；try lock执行SELECT pg_try_advisory_xact_lock(:id)；update/delete分别version/state/due/retention CAS。
- Input/output and state mapping: due候选tenant_id=0、deleted_at IS NULL、due PENDING/RETRY_WAIT或expired PROCESSING并按next_attempt_at,id排序；cleanup按completed_at,id排序。
- Error and edge behavior: 可空幂等键不通过未定类型参数的IS NOT NULL判断；SQL NULL equality仍不命中已有null键。布尔false不持锁；CAS affected 0表示stale candidate；数据库/状态机真实错误交由事务回滚。OutboxMpMigrationIntegrationTest先以`could not determine data type of parameter $2`复现，再验证同一迁移用例通过。
- Standards impact: MC-REUSE-001, MC-VALID-001, MC-TIME-001, MC-SCOPE-001, MC-TEST-001。
- Literal rule enforcement: Rule 2, Rule 10, Rule 11 — Mapper参数合同继续校验，DB时钟/Duration语义沿用，位于现有MP基础组件路径。
- Implementation pseudocode:

```java
SELECT explicit candidate columns ORDER BY next_attempt_at,id LIMIT candidateLimit
SELECT pg_try_advisory_xact_lock(id) inside the bound worker transaction
UPDATE claim with id/version/source/tenant/active and due-or-expired predicate; DELETE cleanup with id/version/status/retention predicate
```

- Verification contribution: XML load plus actual claim, cleanup, parser, migration-with-null-idempotency-key and query-plan PostgreSQL tests; rg must find no SKIP LOCKED in this mapper.
- After this file: SQL is parseable and every state write is protected by the existing version/state CAS.

#### File 9 — `MODIFY egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/store/MybatisPlusOutboxStore.java`

- Purpose: 串联有界candidate scan、try-lock、SSM和CAS，同时维持Store SPI。
- Symbols: claimDue, claimByMessageIds, deleteSucceeded, claim(candidates,...), enqueue
- Repository evidence: 现有Store已在worker REQUIRES_NEW内读取候选、SSM判边、逐行CAS和重新读取DB lease时间。
- Dependencies and consumers: Repository mapped contracts; Lifecycle service; OutboxTechnicalContextExecutor; same MP transaction manager.
- Why now: XML/Repository可用后将全候选行为接入实际Store入口。
- Contract/signature changes: 对外签名不变；candidateLimit=min(10000, 4L*requestedLimit)，最终claim数量≤requestedLimit。
- Input/output and state mapping: 同事务顺序tryLock→evaluate source signal→CAS→reselect PROCESSING；竞争失败跳过；cleanup同id lock后执行versioned delete。
- Error and edge behavior: 数据库/状态机异常整批rollback；tryLock false、CAS 0为正常竞争；保留availableAt缺省时使用注入Clock的修复。
- Standards impact: MC-LOG-001, MC-BEAN-001, MC-UTIL-001, MC-TIME-001, MC-PATTERN-001, MC-SCOPE-001, MC-TEST-001。
- Literal rule enforcement: Rule 2, Rule 4, Rule 5, Rule 9, Rule 10, Rule 11 — 保留输入验证/具名注入/JDK集合/SSM状态模式/java.time与Starter边界。
- Implementation pseudocode:

```java
long scan = Math.min(10000L, Math.multiplyExact((long) requestedLimit, 4L))
for candidate in repository.selectDueCandidates(scan): stop when claimed count reaches request; continue if !tryAcquireTransactionLock(id)
run lifecycle.evaluate then updateClaim CAS; for cleanup repeat lock and versioned DELETE
```

- Verification contribution: Store unit suite and real PostgreSQL claim/reclaim/cleanup/business-state-machine Failsafe tests.
- After this file: No worker claim or cleanup statement requires SKIP LOCKED and the public SPI is stable.

#### File 10 — `MODIFY egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/autoconfigure/OutboxMpStorageProperties.java`

- Purpose: 避免validated class proxy检查未绑定的增强类字段。
- Symbols: OutboxMpStorageProperties class annotations and migrationLockTimeout validation
- Repository evidence: PG bootstrap showed values bound on the properties bean while class-level @Validated method proxy observed nulls; initializer already calls ValidationUtils.validate.
- Dependencies and consumers: OutboxManagedDdlInitializer performs explicit boundary validation.
- Why now: Runtime configuration correction is necessary for the same PG suite that proves parser-compatible locking.
- Contract/signature changes: 保持prefix/fields/defaults/constraint annotations；去掉class-level @Validated代理标注，校验仍由initializer实际调用。
- Input/output and state mapping: sqlSessionFactoryBeanName, migrationMode, timeout and manifestResource retain same values and constraints.
- Error and edge behavior: Invalid timeout or manifest still fails during initialization before Store/DDL activation.
- Standards impact: MC-VALID-001, MC-CONFIG-001, MC-SCOPE-001, MC-TEST-001。
- Literal rule enforcement: Rule 2, Rule 3, Rule 7, Rule 11 — 不删除字段约束与Lombok model baseline，typed config key不变，保留结构。
- Implementation pseudocode:

```java
bind properties to the same named OutboxMpStorageProperties bean
OutboxManagedDdlInitializer invokes ValidationUtils.validate on the populated instance
assert invalid timeout and manifest fail before logical datasource is returned
```

- Verification contribution: OutboxManagedDdlIntegrationTest invalid-setting assertions and ContextRunner wiring.
- After this file: Boot binding and manual constraint evaluation observe the same populated object.

#### File 11 — `MODIFY egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/migration/OutboxLogicalDataSourceFactory.java`

- Purpose: 保持实现的Bean Validation约束不强于Common extension parent contract。
- Symbols: create(Map<String,DataSource>,byte[])
- Repository evidence: Common LogicalDataSourceFactory declares unannotated override inputs; the Outbox override adding constraints triggers HV000151.
- Dependencies and consumers: Existing outboxManagedDdlInitializer validates the actual settings and wraps datasource creation.
- Why now: Context boot must reach the Outbox parser/claim tests without violating Jakarta override rules.
- Contract/signature changes: Remove strengthened parameter constraints only; retain DDL-before-logical factory order.
- Input/output and state mapping: physical sources and YAML still flow to initializer then Shardingsphere factory.
- Error and edge behavior: Initializer continues to reject missing/invalid topology before logical datasource creation.
- Standards impact: MC-VALID-001, MC-BEAN-001, MC-SCOPE-001, MC-TEST-001。
- Literal rule enforcement: Rule 2, Rule 4, Rule 11 — boundary validation remains at the initializer; retain named qualified Spring constructor injection and approved architecture.
- Implementation pseudocode:

```java
call ddlInitializer.initialize(physical, yaml) before Shardingsphere logical factory
let initializer validate topology and settings at the concrete boundary
assert no stronger override annotations are added to the parent method
```

- Verification contribution: OutboxManagedDdlIntegrationTest valid/invalid context cases and full startup path.
- After this file: Bean validation no longer fails with HV000151 and DDL startup guards remain active.

#### File 12 — `MODIFY egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/migration/OutboxLegacyMigrationService.java`

- Purpose: 让MP目标表检查在技术tenant=0作用域执行。
- Symbols: validateTargetNotStarted, verifyAllRows
- Repository evidence: Migration service reads message Mapper under the common tenant context guard; migration table is an explicit global technical table.
- Dependencies and consumers: OutboxTechnicalContextExecutor and OutboxMessageRepository.
- Why now: Real migration acceptance must not be blocked by ambient tenant mismatch or bypass guard.
- Contract/signature changes: Keep public migration behavior/Result unchanged; wrap target reads in executeMigration.
- Input/output and state mapping: source JDBC remains read-only; target MP reads/writes run under system:outbox:migration and tenant0; finally restore ambient context.
- Error and edge behavior: Any read/validation error propagates, current batch rollback rules remain unchanged.
- Standards impact: MC-VALID-001, MC-BEAN-001, MC-TIME-001, MC-SCOPE-001, MC-TEST-001。
- Literal rule enforcement: Rule 2, Rule 4, Rule 10, Rule 11 — explicit mapper scope, existing qualified bean, java.time, no new layer.
- Implementation pseudocode:

```java
validateTargetNotStarted executes its Repository reads inside technicalContext.executeMigration
verifyAllRows wraps every target keyset/read-count comparison the same way
leave source JDBC lock/read-only path and transaction ownership unchanged
```

- Verification contribution: OutboxMpMigrationIntegrationTest copy/verify/retry scenarios.
- After this file: Migration target reads are guarded MP operations with no ambient tenant leakage.

#### File 13 — `MODIFY egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/store/OutboxStore.java`

- Purpose: 使Store SPI约束与MybatisPlusOutboxStore实现方法一致。
- Symbols: enqueue, claimDue, claimByMessageIds, markSucceeded, markRetry, markDead and cleanup contract annotations
- Repository evidence: Jakarta validation method-override contract requires implementation constraints not to strengthen an unannotated interface.
- Dependencies and consumers: All callers inject OutboxStore; implementation is the sole default bean.
- Why now: The PG-backed Spring context must proxy/wire the validated Store without HV000151.
- Contract/signature changes: Add matching interface constraints only where implementation already requires the parameter; retain method names and descriptors.
- Input/output and state mapping: No record/model, database field, or runtime result changes.
- Error and edge behavior: Invalid public inputs continue to fail at the interface boundary and are exercised by existing unit tests.
- Standards impact: MC-NAME-001, MC-VALID-001, MC-BEAN-001, MC-SCOPE-001, MC-TEST-001。
- Literal rule enforcement: Rule 1, Rule 2, Rule 4, Rule 11 — preserve Store role name, mirror Jakarta constraints and retain caller injection convention.
- Implementation pseudocode:

```java
compare each implementation parameter constraint with OutboxStore declaration
mirror existing constraints without adding stronger implementation-only restrictions
run interface/implementation validation and public caller tests
```

- Verification contribution: Outbox unit suite, ComponentContractGovernanceTest and compile.
- After this file: Interface proxy validation matches the implementation contract.

#### File 14 — `MODIFY egon-cola-components/egon-cola-component-transactional-outbox-starter/src/test/java/top/egon/cola/component/outbox/integration/PostgresqlOutboxQueryPlanIntegrationTest.java`

- Purpose: 在真实PostgreSQL optimizer上验证partial indexes，不把逻辑EXPLAIN parser局限混作PG计划。
- Symbols: shouldExposeRequiredSchemaAndUseClaimIndexes, explain
- Repository evidence: Test support exposes physicalJdbcTemplate as well as logical Shardingsphere JdbcTemplate.
- Dependencies and consumers: SQL2 manifest/schema; target outbox indexes.
- Why now: EXPLAIN FORMAT JSON用于索引物理计划，直接physical connection避免ShardingSphere EXPLAIN grammar干扰。
- Contract/signature changes: 无生产API改动；runtime claim parser由Store concurrency tests另外覆盖。
- Input/output and state mapping: seed 1000 rows; explain due/reclaim query on physical PostgreSQL connection; assert claim/reclaim/cleanup indexes exist.
- Error and edge behavior: SQL/DDL错误或索引未选择使测试失败；不禁用真实Mapper parser验收。
- Standards impact: MC-ARCH-001, MC-REUSE-001, MC-SCOPE-001, MC-TEST-001。
- Literal rule enforcement: Rule 10, Rule 11 — database time and table shape stay intact; test remains in selected component integration package.
- Implementation pseudocode:

```java
seed rows through real Store and analyze the target table
run EXPLAIN FORMAT JSON through physicalJdbcTemplate for each bounded candidate shape
assert required index names and schema fields while concurrency test exercises logical routing
```

- Verification contribution: PostgresqlOutboxQueryPlanIntegrationTest plus PostgresqlOutboxConcurrencyIntegrationTest.
- After this file: Physical plan evidence is isolated from logical SQL parser evidence.

#### File 15 — `MODIFY egon-cola-components/egon-cola-component-transactional-outbox-starter/src/test/java/top/egon/cola/component/outbox/integration/PostgresqlOutboxTestSupport.java`

- Purpose: 修正集成测试的真实唯一PRIMARY NATIVE SINGLE环境与应用上下文。
- Symbols: initializePostgresql, TestApplication, cleanup
- Repository evidence: Shared fixture constructs PostgreSQL Testcontainers, Shardingsphere logical datasource, physical JdbcTemplate and Store.
- Dependencies and consumers: Failsafe env gate EGON_OUTBOX_TEST_POSTGRES_ENABLED and existing outbox auto-config.
- Why now: All Step9 runtime behavior must execute on the same route/transaction topology as production.
- Contract/signature changes: No production endpoints, credentials, database or application start; isolated disposable container only.
- Input/output and state mapping: Precreate egon_outbox; rule contains outbox table/ddl_history as SINGLE on one PRIMARY; cache auto-config excluded; MP storage keys explicitly bound.
- Error and edge behavior: When env gate is absent tests skip; approved command sets it and requires named tests non-skipped.
- Standards impact: MC-ARCH-001, MC-REUSE-001, MC-CONFIG-001, MC-SCOPE-001, MC-TEST-001。
- Literal rule enforcement: Rule 2, Rule 7, Rule 11 — test-only context supplies validated properties consistently and stays in existing component tests.
- Implementation pseudocode:

```java
start only the PostgreSQL Testcontainer after the explicit environment gate
configure one PRIMARY NATIVE SINGLE rule and one logical transaction manager
exclude unrelated app-owned cache auto-configuration and close the container/context after suite
```

- Verification contribution: Safe scoped Failsafe suite; report each integration class test count and skips.
- After this file: PG mapper calls route to one DB advisory-lock domain with no Redis dependency.

#### File 16 — `MODIFY egon-cola-components/egon-cola-component-transactional-outbox-starter/src/test/java/top/egon/cola/component/outbox/integration/OutboxManagedDdlIntegrationTest.java`

- Purpose: 验证typed properties与DDL启动合同能在实际Spring上下文解析。
- Symbols: ApplicationContextRunner setup, invalidSetting cases, managed DDL context
- Repository evidence: Existing tests use Testcontainers/shared PG support plus no-db ContextRunner paths.
- Dependencies and consumers: OutboxMpStorageProperties, initializer, test SqlSessionFactory and disabled optional config.
- Why now: Runtime configuration corrections must be proven before claim parser runs.
- Contract/signature changes: Test-only wiring; no SQL schema or property-key change.
- Input/output and state mapping: Context supplies outbox store fixture or disables it for pure DDL order test; invalid timeout/manifest rejected explicitly.
- Error and edge behavior: Any binding, validation or startup error fails named integration test.
- Standards impact: MC-VALID-001, MC-CONFIG-001, MC-BEAN-001, MC-SCOPE-001, MC-TEST-001。
- Literal rule enforcement: Rule 2, Rule 3, Rule 7, Rule 11 — preserve class annotations/defaults/config key shape and check every environment-independent property.
- Implementation pseudocode:

```java
bind storage.mp flags and the test SqlSessionFactory through the application context
assert DDL initializer executes before logical factory delegate
assert invalid settings fail with the expected configuration exception
```

- Verification contribution: OutboxManagedDdlIntegrationTest and strict Plan/Spec validation.
- After this file: The Spring config needed by MP and SSM tests starts under approved settings.

#### File 17 — `MODIFY egon-cola-components/egon-cola-component-transactional-outbox-starter/src/test/java/top/egon/cola/component/outbox/integration/BusinessStateMachineIntegrationTest.java`

- Purpose: 让业务SSM PG端到端fixture通过真实Store和唯一Dispatcher Bean合同。
- Symbols: test ApplicationContext, dispatcher construction, business schema initialization
- Repository evidence: Existing test exercises BusinessStateMachineDeliveryHandler with PostgreSQL-backed outbox and receipt fixture.
- Dependencies and consumers: OutboxStore, StateMachine autoconfiguration, MP props/clock, isolated business schema.
- Why now: Business SSM acceptance must continue after MP context corrections.
- Contract/signature changes: No business public SPI change; setup alters its fixture schema once before tests.
- Input/output and state mapping: Inject existing MP Store and aliases; schema-qualified business receipt table remains isolated from egon_outbox technical tenant.
- Error and edge behavior: Context/tenant/fingerprint/replay failures remain observable; no broad cache dependency.
- Standards impact: MC-ARCH-001, MC-REUSE-001, MC-VALID-001, MC-BEAN-001, MC-SCOPE-001, MC-TEST-001。
- Literal rule enforcement: Rule 2, Rule 4, Rule 9, Rule 11 — boundary validation and qualified beans stay; actual SSM remains selected pattern.
- Implementation pseudocode:

```java
build fixture with existing Store, LifecycleService, Clock and MP storage properties
initialize the schema-qualified test aggregate/receipt tables once before assertions
assert same-event replay and transaction rollback through the business StateMachine handler
```

- Verification contribution: BusinessStateMachineIntegrationTest named PG cases run non-skipped.
- After this file: The optional business Event SSM path remains green on the corrected core Store.

#### File 18 — `MODIFY egon-cola-components/egon-cola-component-transactional-outbox-starter/src/test/java/top/egon/cola/component/outbox/integration/TransactionalMessageAnnotationSampleTest.java`

- Purpose: 让annotation sample验证真实Starter入队和MP Store自动配置。
- Symbols: ApplicationContext test setup and annotation-driven enqueue test
- Repository evidence: Existing sample test loads TransactionalOutbox annotation infrastructure under Spring.
- Dependencies and consumers: Shared PG test context, actual OutboxStore, StateMachine auto-config and NATIVE SINGLE.
- Why now: Final public example must work through the same configured Store as API users.
- Contract/signature changes: No public API change; only test context qualifiers and schema names.
- Input/output and state mapping: Sample Command work and outbox Event commit in the same local transaction; database table uses egon_outbox schema.
- Error and edge behavior: Missing Bean, DDL, SQL parser or transaction errors fail the test.
- Standards impact: MC-REUSE-001, MC-BEAN-001, MC-SCOPE-001, MC-TEST-001。
- Literal rule enforcement: Rule 2, Rule 4, Rule 11 — preserve annotated command transaction handoff and named Spring dependencies.
- Implementation pseudocode:

```java
load the public annotation configuration with the shared real MP OutboxStore
submit one sample command and query the schema-qualified outbox row
assert command and Event intent commit together
```

- Verification contribution: TransactionalMessageAnnotationSampleTest actually executes under the safe PG command.
- After this file: The documented annotation entry point is backed by parser-compatible storage.

#### File 19 — `MODIFY egon-cola-components/egon-cola-component-transactional-outbox-starter/src/test/java/top/egon/cola/component/outbox/integration/TransactionalOutboxDirectApiSampleTest.java`

- Purpose: 让Direct API sample验证真实MP存储与事务合同。
- Symbols: ApplicationContext test setup and direct enqueue test
- Repository evidence: Existing test demonstrates TransactionalOutbox.enqueue public API and actual delivery receipt.
- Dependencies and consumers: Shared PG fixture; existing OutboxStore and transaction guard.
- Why now: The direct public entry is independent of the annotation sample and needs its own smoke assertion.
- Contract/signature changes: No API or schema change; use existing qualified Store and schema-qualified target table.
- Input/output and state mapping: one NewOutboxRecord returns OutboxReceipt and one active technical row.
- Error and edge behavior: Duplicate/fingerprint conflict and SQL failures retain existing exception behavior.
- Standards impact: MC-REUSE-001, MC-VALID-001, MC-BEAN-001, MC-SCOPE-001, MC-TEST-001。
- Literal rule enforcement: Rule 2, Rule 4, Rule 11 — validate existing public payloads and preserve component wiring.
- Implementation pseudocode:

```java
load direct API sample with real MP storage properties and the shared Store
invoke TransactionalOutbox.enqueue inside its caller transaction
assert receipt plus exactly one active row in egon_outbox.egon_cola_outbox_message
```

- Verification contribution: TransactionalOutboxDirectApiSampleTest executes in scoped PG Failsafe run.
- After this file: Both supported public enqueue examples run against the corrected path.

#### File 20 — `MODIFY egon-cola-components/egon-cola-component-transactional-outbox-starter/src/test/java/top/egon/cola/component/outbox/store/OutboxLongPrimaryKeyTest.java`

- Purpose: 冻结enqueue缺省可执行时间与long ID兼容语义。
- Symbols: enqueue without availableAt, long Snowflake id fixture
- Repository evidence: Existing focused Store unit test covers generated 64-bit IDs and mapper insert contract.
- Dependencies and consumers: MybatisPlusOutboxStore injected Clock and OutboxMessageConverter.
- Why now: Runtime PG fixtures exposed NOT NULL next_attempt_at when legacy API omits availableAt.
- Contract/signature changes: OutboxStore signature remains unchanged; omitted availability defaults to Clock.instant().
- Input/output and state mapping: Explicit availableAt is preserved; absent value uses injected Clock; ID remains positive long.
- Error and edge behavior: No Java system clock or null SQL default guessing; validation and idempotency behavior unchanged.
- Standards impact: MC-VALID-001, MC-MODEL-001, MC-TIME-001, MC-SCOPE-001, MC-TEST-001。
- Literal rule enforcement: Rule 2, Rule 3, Rule 10, Rule 11 — retain public validation/model and use java.time Clock.
- Implementation pseudocode:

```java
arrange fixed Clock and NewOutboxRecord without availableAt
call store.enqueue and capture generated OutboxMessagePO
assert nextAttemptAt equals clock.instant and the long id remains unchanged
```

- Verification contribution: OutboxLongPrimaryKeyTest plus real TransactionalOutboxTransactionIntegrationTest.
- After this file: The existing API can create rows satisfying the managed NOT NULL schema.

#### File 21 — `MODIFY egon-cola-components/egon-cola-component-transactional-outbox-starter/src/test/java/top/egon/cola/component/outbox/integration/OutboxMybatisPlusIntegrationTest.java`

- Purpose: 让真实MP Store集成测试容忍Spring CGLIB代理并继续验证目标实现。
- Symbols: shouldPersistAndTransitionThroughManagedMpStoreWhileRestoringTenantContext, markRetry, markDead。
- Repository evidence: OutboxStore由@Validated Spring Bean Factory注册；PG上下文启用方法校验/CGLIB proxy。
- Dependencies and consumers: MybatisPlusOutboxStore作为Store target class；其它调用仍只依赖OutboxStore接口。
- Why now: 最终PG集合显示实现行为均通过，但旧断言比较代理runtime class字符串导致1个假失败。
- Contract/signature changes: 以AopUtils.getTargetClass(store)断言MybatisPlusOutboxStore.class；不检查Spring生成的代理名。Store.markRetry只验证显式SCHEDULE_RETRY到RETRY_WAIT；测试在retry budget耗尽时显式调用markDead验证Dispatcher决策边界。
- Input/output and state mapping: 真实Store bean执行PENDING→PROCESSING→RETRY_WAIT→PROCESSING；retry次数到max不由Store重复判定，测试显式触发PERMANENT→DEAD；后续SUCCEEDED路径保持。
- Error and edge behavior: 若目标实现并非MybatisPlusOutboxStore仍失败；代理采用JDK或CGLIB都不影响合同；Store.markRetry即使attempt达上限仍只执行显式SCHEDULE_RETRY，DEAD由调用markDead产生。
- Standards impact: MC-REUSE-001, MC-BEAN-001, MC-SCOPE-001, MC-TEST-001。
- Literal rule enforcement: Rule 4 — 验证具名Spring Store target保持唯一MP实现；Rule 11 — 测试留在现有组件integration路径。
- Implementation pseudocode:

```java
resolve targetClass from the proxied OutboxStore with Spring AopUtils
assert targetClass equals MybatisPlusOutboxStore.class
run existing enqueue/claim/mark and tenant-context restoration assertions unchanged
```

- Verification contribution: scoped PG Failsafe suite必须运行OutboxMybatisPlusIntegrationTest；断言RETRY_WAIT与显式DEAD均在实际PG非skip通过。
- After this file: real proxy wiring remains tested without comparing generated proxy class names.

- Validation working directory: `/Users/mario/SelfProject/Egon-COLA`.
- Verification command: `mvn -pl egon-cola-components/egon-cola-component-transactional-outbox-starter -am -DfailIfNoTests=false -Dsurefire.failIfNoSpecifiedTests=false -Dtest=OutboxLongPrimaryKeyTest,OutboxMessageConverterTest,OutboxMybatisPlusAutoConfigurationTest,ComponentContractGovernanceTest test`; then run the Step9 scoped PostgreSQL Failsafe command recorded in §8 with `EGON_OUTBOX_TEST_POSTGRES_ENABLED=true`.
- Expected result: selected unit and Failsafe tests actually execute, zero failures/errors/skips; SQL parser accepts candidate and advisory-lock statements; concurrency assertions show one claim/delete; full module compile passes.
- Failure returns to: File 3/4 for RED fixture defects; File 7/8 for parser or CAS errors; File 9–21 for PostgreSQL context/migration/sample wiring; otherwise Step9 remains In Progress.
- Completion criteria: both strict document validators, unit, compile, scoped PostgreSQL tests, diff hygiene, all 17 Manual Checks, all ten Literal Rule rows, and commit-path audit pass.
- Rollback: revert only the Step9 commit if not released; no database/schema migration is part of this correction. For any later live-table writes, honor Spec §16 stop-write and forward-fix rules.
- Commit paths: docs/egon/spec/2026-09-24-12-00-outbox-statemachine-cqe-integration.md; docs/egon/plan/2026-09-24-13-16-outbox-statemachine-mp-implementation.md; egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/persistence/dao/OutboxMessageDAO.java; egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/persistence/repository/OutboxMessageRepository.java; egon-cola-components/egon-cola-component-transactional-outbox-starter/src/test/java/top/egon/cola/component/outbox/integration/PostgresqlOutboxConcurrencyIntegrationTest.java; egon-cola-components/egon-cola-component-transactional-outbox-starter/src/test/java/top/egon/cola/component/outbox/integration/PostgresqlOutboxCleanupIntegrationTest.java; egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/resources/mapper/outbox/OutboxMessageMapper.xml; egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/store/MybatisPlusOutboxStore.java; egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/autoconfigure/OutboxMpStorageProperties.java; egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/migration/OutboxLogicalDataSourceFactory.java; egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/migration/OutboxLegacyMigrationService.java; egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java/top/egon/cola/component/outbox/store/OutboxStore.java; egon-cola-components/egon-cola-component-transactional-outbox-starter/src/test/java/top/egon/cola/component/outbox/integration/PostgresqlOutboxQueryPlanIntegrationTest.java; egon-cola-components/egon-cola-component-transactional-outbox-starter/src/test/java/top/egon/cola/component/outbox/integration/PostgresqlOutboxTestSupport.java; egon-cola-components/egon-cola-component-transactional-outbox-starter/src/test/java/top/egon/cola/component/outbox/integration/OutboxManagedDdlIntegrationTest.java; egon-cola-components/egon-cola-component-transactional-outbox-starter/src/test/java/top/egon/cola/component/outbox/integration/BusinessStateMachineIntegrationTest.java; egon-cola-components/egon-cola-component-transactional-outbox-starter/src/test/java/top/egon/cola/component/outbox/integration/TransactionalMessageAnnotationSampleTest.java; egon-cola-components/egon-cola-component-transactional-outbox-starter/src/test/java/top/egon/cola/component/outbox/integration/TransactionalOutboxDirectApiSampleTest.java; egon-cola-components/egon-cola-component-transactional-outbox-starter/src/test/java/top/egon/cola/component/outbox/store/OutboxLongPrimaryKeyTest.java; egon-cola-components/egon-cola-component-transactional-outbox-starter/src/test/java/top/egon/cola/component/outbox/persistence/OutboxMessageConverterTest.java; egon-cola-components/egon-cola-component-transactional-outbox-starter/src/test/java/top/egon/cola/component/outbox/integration/OutboxMybatisPlusIntegrationTest.java
- Commit: `fix(outbox): use parser-compatible transactional claim locks`.
- Execution record: RED verified — baseline Failsafe claim query ran 1 test, 0 failures, 1 error, 0 skips with Shardingsphere 5.5.3 DialectSQLParsingException at FOR UPDATE SKIP LOCKED; compile-contract Failsafe ran 2 setup cases, 0 failures, 2 errors because the new candidate Mapper statements were intentionally absent; OutboxMessageConverterTest ran 5 tests, 1 expected mapper-contract failure. GREEN — OutboxMessageConverterTest 5/5; concurrency+cleanup Failsafe 4/4; final scoped PostgreSQL Failsafe 14 classes/50 tests, 0 failures/errors/skips; unit suite 38 classes/151 tests, 0 failures/errors/skips; module compile, both strict validators and diff-check pass. This Step is committed using the path-limited message above; its hash is recorded in the final delivery.

#### Step 9 Manual Check execution record

| Check ID | Applicability | Status | Evidence | Finding | Required action/exception |
| --- | --- | --- | --- | --- | --- |
| MC-ARCH-001 | Applicable | PASS | Existing egon-cola-component-transactional-outbox-starter api/store/dispatch/persistence tree remains the only target; diff is limited to 19 Outbox files plus its Spec/Plan. | No hybrid module/layer or dependency-direction change. | None |
| MC-REUSE-001 | Applicable | PASS | MybatisPlusOutboxStore and OutboxMessageRepository reuse Common MP; PostgreSQL transaction advisory lock and current worker TransactionTemplate are used. | No second lock service, persistence framework or scheduler. | None |
| MC-DEP-001 | Applicable | PASS | Step9 diff has no POM/BOM/dependency path; JDBC/MP/SS are existing dependencies. | No dependency added or upgraded. | None |
| MC-NAME-001 | Applicable | PASS | Only new DAO/Repository method roles are selectDueCandidates, selectByMessageIdsCandidates, tryAcquireTransactionLock, selectExpiredSucceededCandidates and deleteSucceededByIdVersion; no carrier type added. | Touched class names remain semantic DAO/Repository/Store/Test roles. | None |
| MC-VALID-001 | Applicable | PASS | OutboxMessageDAO uses Min/Max/Positive/NotEmpty; Repository repeats bounds; OutboxStore and MybatisPlusOutboxStore carry matching @Positive/@Max constraints; DDL initializer still validates typed storage properties. | Public batch and mapper bounds remain enforced; the scoped PG tests execute real mapper paths. | None |
| MC-MODEL-001 | Not applicable | N/A | No PO/BO/VO/DTO or persisted model definition changes; OutboxMessageConverterTest changes only mapper statement assertions. | No object-model or Lombok contract changed. | None |
| MC-CONVERT-001 | Not applicable | N/A | No Converter implementation or generated mapper is changed. | MapStruct/BaseForwardConverter contract unchanged. | None |
| MC-LOG-001 | Applicable | PASS | MybatisPlusOutboxStore and OutboxMessageRepository retain existing @Slf4j; no new logging branch or sensitive log is introduced. | Touched business Beans follow existing safe logging pattern. | None |
| MC-BEAN-001 | Applicable | PASS | Store/Repository Bean names and qualified constructor fields are unchanged; PG Testcontainers starts actual default Store wiring. | No duplicate or unqualified dependency injection added. | None |
| MC-UTIL-001 | Applicable | PASS | Only JDK Math/list/collection/latch primitives and existing Spring/JDBC/MyBatis APIs are used; POM diff is empty. | No utility library or helper framework added. | None |
| MC-JSON-001 | Not applicable | N/A | No JSON/Event payload, serializer or public transport contract changed. | Jackson behavior is unchanged. | None |
| MC-TIME-001 | Applicable | PASS | Store retains Clock/Duration/Instant; SQL eligibility and retention use PostgreSQL clock_timestamp; unit and PG transaction tests pass. | No java.util date/time API added. | None |
| MC-CONFIG-001 | Applicable | PASS | OutboxMpStorageProperties key/default shape is unchanged; OutboxManagedDdlIntegrationTest exercises bound/invalid settings; no application profile file is changed. | No key/profile drift; existing initializer validation remains active. | None |
| MC-PATTERN-001 | Applicable | PASS | OutboxLifecycleService/OutboxLifecycleStateMachineFactory still decide every transition; advisory locking only serializes persistence and version/state CAS. | No lifecycle switch or duplicate retry policy introduced. | None |
| MC-SCOPE-001 | Applicable | PASS | git diff --name-only lists 19 Outbox paths plus this Spec/Plan; all other dirty/untracked paths are unchanged and unstaged. | No unrelated agent path included. | None |
| MC-TEST-001 | Applicable | PASS | Unit command: 38 classes, 151 tests, 0 failures/errors/skips; scoped PG verify: 14 Failsafe classes, 50 tests, 0 failures/errors/skips; compile, both strict validators and git diff --check pass. | RED parser failure and GREEN concurrency/runtime evidence are recorded above. | None |
| MC-BLOCKER-001 | Applicable | PASS | All other Step9 MC rows pass or have positive N/A evidence; no unresolved failure/skip remains. | Step9 is ready for its path-limited commit. | None |

#### Step 9 Literal Rule execution record

| Literal rule | Applicability | Status | Diff/path/symbol evidence | Test/static evidence | Finding | Required action/exception |
| --- | --- | --- | --- | --- | --- | --- |
| Rule 1 | Applicable | PASS | DAO/Repository/Store methods use their behavior names; no new carrier classes; compile passes. | No ambiguous Data/Info/Bean type introduced. | None | None |
| Rule 2 | Applicable | PASS | DAO/Repository/Store parameter constraints align; storage properties still reach explicit initializer ValidationUtils; 151 unit and 50 PG tests pass. | Every changed persistence boundary keeps validation and affected-row outcomes. | None | None |
| Rule 3 | Not applicable | N/A | No PO/BO/VO/DTO/entity/converter is modified; existing OutboxMessagePO and MapStruct mapper are unchanged. | No model/Lombok/conversion rule affected. | None | None |
| Rule 4 | Applicable | PASS | Existing Store/Repository Beans retain @Slf4j, stable Bean names, @RequiredArgsConstructor and @Qualifier; Spring PG context boots. | No Bean wiring was weakened or added. | None | None |
| Rule 5 | Applicable | PASS | No dependency/import outside JDK and already-present Spring/MyBatis/PG APIs; no POM change. | Utility allowlist preserved. | None | None |
| Rule 6 | Not applicable | N/A | No JSON protocol, Jackson mapping or serialized field is touched. | JSON contract unchanged. | None | None |
| Rule 7 | Not applicable | N/A | No configuration key/profile is added or removed; OutboxMpStorageProperties retains the same fields/defaults and integration validation. | No environment-key parity change. | None | None |
| Rule 9 | Applicable | PASS | Lifecycle transitions still pass through the approved Spring State pattern; claim lock/CAS does not decide business state. | No hard-coded lifecycle branch replaces SSM. | None | None |
| Rule 10 | Applicable | PASS | Time values remain java.time Duration/Instant/Clock; database due/cutoff uses clock_timestamp. | No java.util.Date/Calendar/SimpleDateFormat added. | None | None |
| Rule 11 | Applicable | PASS | All code paths stay in the existing single transactional-outbox Starter persistence/store/tests tree; no business layer or module is created. | Architecture profile is preserved. | None | None |

## 8. Test, Validation, and Quality Gates

本章包含原Steps1–8门槛与用户批准的Corrective Step9验证。仅Step9获准运行本机隔离PostgreSQL Testcontainers；只执行§8具名Outbox Failsafe集合，不访问宿主/生产DB，不运行触发无关Common Cache Redis集成的未筛选reactor verify。以报告实际执行数与failure/error/skip为准。

| Gate/order | Working directory | Command or method | Scope | Expected result | Failure returns to | Requirements/runtime boundary |
| --- | --- | --- | --- | --- | --- | --- |
| Preflight | repository root | `python3 .agents/skills/egon-coding-writing-plan/scripts/validate_skill_resources.py` | Plan资源 | PASS；本轮已实际执行 | 技能安装/资源 | 文档静态 |
| Spec contract | repository root | `python3 .agents/skills/egon-coding-writing-spec/scripts/validate_spec.py docs/egon/spec/2026-09-24-12-00-outbox-statemachine-cqe-integration.md --strict` | 主Spec | 结构/链接/Trace PASS；不是运行证明 | 回主Spec | REQ-001～010，静态 |
| Step 1 RED/GREEN | repository root | `mvn -pl egon-cola-components/egon-cola-component-transactional-outbox-starter -am -DfailIfNoTests=false -Dsurefire.failIfNoSpecifiedTests=false -Dtest=OutboxLifecycleServiceTest,StateMachineExecutionServiceTest test` | 技术图与runner | RED为无真实SSM/Guard完成；GREEN为两类命名测试非skip且全部通过 | Step1相关文件 | REQ-001/008；单测 |
| Step 2 RED/GREEN | repository root | `mvn -pl egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter -am -DfailIfNoTests=false -Dsurefire.failIfNoSpecifiedTests=false -Dtest=EgonColaDdlManifestFamilyTest test` | family精确值 | RED旧family拒绝；GREEN新family和六旧family均通过 | Step2 family文件 | REQ-010；单测 |
| Step 2 manifest | repository root | `python3 -m json.tool egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/resources/db/egon-outbox-mp/manifest.json`；`shasum -a 256 egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/resources/db/egon-outbox-mp/V20260924_001__initialize_outbox_mp_schema.sql` | JSON/hash/唯一版本 | 解析exit0；真实hash与manifest相等；只有一新version；旧SQL1无diff | Step2 SQL/manifest | REQ-010；静态，不执行SQL |
| Step 3 RED/GREEN | repository root | `mvn -pl egon-cola-components/egon-cola-component-transactional-outbox-starter -am -DfailIfNoTests=false -Dsurefire.failIfNoSpecifiedTests=false -Dtest=OutboxMessageConverterTest test` | PO/Converter | RED缺映射Bean/投影；GREEN字段/null/Instant/headers/enum符合原Record | Step3 PO/MapStruct/helper | REQ-009；单测 |
| Step 4 RED/GREEN | repository root | `mvn -pl egon-cola-components/egon-cola-component-transactional-outbox-starter -am -DfailIfNoTests=false -Dsurefire.failIfNoSpecifiedTests=false -Dtest=OutboxMybatisPlusAutoConfigurationTest,OutboxLongPrimaryKeyTest,TransactionalOutboxAutoConfigurationTest,DefaultTransactionalOutboxTest,ComponentContractGovernanceTest test` | 默认Store/旧接口 | RED旧JDBC装配；GREEN唯一MP Store/旧API/ID/Rabbit/HTTP配置约束 | Step4 Store/自动配置及旧测试 | REQ-004/007/009；单测/ContextRunner |
| Step 5 RED/GREEN | repository root | `mvn -pl egon-cola-components/egon-cola-component-transactional-outbox-starter -am -DfailIfNoTests=false -Dsurefire.failIfNoSpecifiedTests=false -Dtest=OutboxDispatcherTest,TransactionalOutboxAutoConfigurationTest,OutboxOptionalAutoConfigurationTest,OutboxMybatisPlusAutoConfigurationTest,ComponentContractGovernanceTest test` | 技术状态选择/qualified Bean wiring | RED旧switch没有lifecycleService调用；GREEN FSM输出决定Retry/Dead/Success，alias与隔离auto-config可解析，迟到owner不记成功 | Step5 Dispatcher/config/callers | REQ-001/004；单测/ContextRunner |
| Step 6 RED/GREEN | repository root | `mvn -pl egon-cola-components/egon-cola-component-transactional-outbox-starter -am -DfailIfNoTests=false -Dsurefire.failIfNoSpecifiedTests=false -Dtest=BusinessStateMachineServiceTest,BusinessStateMachineDeliveryHandlerTest,OutboxBusinessStateMachineAutoConfigurationTest test` | 可选业务适配 | RED无Handler/消费服务；GREEN envelope/业务SPI/receipt/缺依赖失败 | Step6 Event/Service/Handler | REQ-002/003/005/006/007；单测 |
| Step 7 RED/GREEN | repository root | `mvn -pl egon-cola-components/egon-cola-component-transactional-outbox-starter -am -DfailIfNoTests=false -Dsurefire.failIfNoSpecifiedTests=false -Dtest=OutboxDispatcherTest test` | 维护门禁 | RED submit仍领取；GREEN两入口无调度且原false分支未变 | Step7门禁 | REQ-010；单测；copy真实性待PG |
| Step 8 regression | repository root | `mvn -pl egon-cola-components/egon-cola-component-transactional-outbox-starter -am -DfailIfNoTests=false test` | 组件单测及需参与的反应堆依赖 | 非集成用例全部通过；如报告存在未知skip应调查；无旧JDBC构造符号 | Owning Step | REQ-001～010；模块 |
| Compile/graph | repository root | `mvn -pl egon-cola-components/egon-cola-component-transactional-outbox-starter -am -DskipTests compile`；`mvn -pl egon-cola-components/egon-cola-component-transactional-outbox-starter -am -DskipTests dependency:tree` | Java21/processor/传递图 | 所有目标模块编译；generated MapStruct实现存在且其具名Bean通过自动配置装配；SSM/MP唯一有效版本，无未批准替代依赖 | Steps1/3/4 | REQ-007/009；构建 |
| Static change scope | repository root | `git diff --check -- egon-cola-components/egon-cola-component-transactional-outbox-starter egon-cola-components/egon-cola-components-bom/pom.xml egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter/src/main/java/top/egon/cola/component/common/mybatis/ddl/EgonColaDdlManifestBO.java docs/egon/spec/2026-09-24-12-00-outbox-statemachine-cqe-integration.md` | whitespace/限定路径 | exit0，且git status逐条对照§5；旧SQL1工作树与HEAD相同 | 各Step owner | REQ-008/010；静态 |
| Forbidden patterns | repository root | `rg -n 'PostgresqlJdbcOutboxStore|java\.util\.(Date|Calendar)|SimpleDateFormat|\.ordinal\(' egon-cola-components/egon-cola-component-transactional-outbox-starter/src/main/java` | 旧JDBC/时间/ordinal | 无输出（rg exit1代表无命中）；历史文档及SQL1未搜索替代目标 | Steps1/3/4/6 | Rules6/10/11；静态 |
| Profile parity | repository root | 对`rg --files egon-cola-components/egon-cola-component-transactional-outbox-starter | rg '/application[^/]*\.ya?ml$'`的结果记录“无生产profile”或精确列表；对所有列出的宿主profile逐个比对新增键集，双语README示例键集相等 | Boot键树 | 组件无profile则只检验typed binding与双语；接入宿主时所有profile键相同 | Step8/接入任务 | Rule7；静态/ContextRunner |
| Controlled PG/SS | repository root | `EGON_OUTBOX_TEST_POSTGRES_ENABLED=true mvn -pl egon-cola-components/egon-cola-component-transactional-outbox-starter -am -DfailIfNoTests=false -Dtest=__skip_surefire__ -Dsurefire.failIfNoSpecifiedTests=false -Dit.test=OutboxManagedDdlIntegrationTest,OutboxMpMigrationIntegrationTest,TransactionalOutboxTransactionIntegrationTest,PostgresqlOutboxQueryPlanIntegrationTest,RabbitDeliveryHandlerIntegrationTest,HttpDeliveryHandlerIntegrationTest,TransactionalMessageAnnotationSampleTest,TransactionalOutboxDirectApiSampleTest,OutboxMybatisPlusIntegrationTest,OutboxDataSafetyIntegrationTest,PostgresqlOutboxRecoveryIntegrationTest,PostgresqlOutboxConcurrencyIntegrationTest,PostgresqlOutboxCleanupIntegrationTest,BusinessStateMachineIntegrationTest -Dfailsafe.failIfNoSpecifiedTests=false verify` | TEST-002/003/005/011/012/013及Store claim/cleanup parser | 14类具名Failsafe共50 tests非skip且0 failure/error；实际查看索引计划和报告 | Step9；本机隔离PostgreSQL Testcontainer | REQ-001/004/008/009/010；不等同生产切换 |
| Manual release | 目标宿主环境（本轮不访问） | 逐项确认同PRIMARY、NATIVE两SINGLE、受管history前缀/sha/route、所有旧writer停写、copy Result.verified、所有新consumer ready后生产者开通 | 旧→新切换 | 未完成任何项则拒绝切换；来源旧表保留；切换后优先前向修复 | Step7/8或运维 | REQ-010；受控真实环境 |

无新框架无法覆盖的静态门禁：现有Maven/JUnit/ContextRunner/Failsafe、skill validator及`rg`/Git足以覆盖计划测试。无需`scripts/work/` harness；不会遗留无归属工作脚本。单测GREEN与真实PG/SS/运维切换分别留证，不混写为同一种通过。

## 9. Migration, Compatibility, Rollout, and Rollback

- **唯一DDL**：Step2写`db/egon-outbox-mp/V20260924_001__initialize_outbox_mp_schema.sql`与同目录manifest，family准确新增component-outbox。使用`EgonColaPostgreDdlRunner`对同PG唯一PRIMARY/空egon_outbox schema执行；脚本与ddl_history同事务，自动锁/未知commit新连接核验沿用Common。若目标非空无历史、checksum或route drift，停在校验，不修历史、不改SQL1。组件不是native Light/Web/Service生成项目，本轮没有generator配置、apply或DDL consumption log条目。
- **单步文件依赖**：Step3/4只有在DDL列/索引/MapperXML定稿后才产生MP写；所有运行期命令含显式tenant0/deletedAtISNULL/id/version/owner保护，技术Context仅在消息Mapper调用时绑定tenant0/system:outbox并finally恢复，业务Event仍使用实际tenant。Step4删除JDBC具体类，旧OutboxStore API不变。
- **Claim/cleanup锁**：Step9候选窗口min(10000,requestedLimit*4)；逐行经目标SINGLE表路由try advisory xact lock后再运行SSM和state/version/due或retention CAS；false lock/CAS0只跳过本候选，事务在提交/回滚时释放锁，不新增表或索引。
- **数据搬迁**：Step7的maintenancemode先停本机新入队和调度，同时运维必须停**所有**旧/新producer、worker并等待在途事务结束。新目标经runner就绪后调用INTERNAL-009：同PRIMARY旧来源schema由维护者显式给出，源表在READ_COMMITTED事务先获SHARE锁，按id keyset有界读取；目标经MP逐批插入/校验，原22列逐字节/逐字段保留、技术公共字段新填tenant0/version0/audit。分批commit失败可从头重跑，遇id/messageId/key内容差异停；结束流式双向比较/计数/status后verified=true才可考虑切换。源表与SQL1始终保留。
- **兼容窗口**：旧HTTP/Rabbit/custom处理器字段/通道不变；新statemachine业务通道默认关闭。所有消费者实例装配同version定义后才允许生产新的业务Event。消息业务receipt身份=(tenantId,machineKey,definitionVersion,businessId,eventId)，技术messageId与业务eventId分开；跨回复窗口重复经业务凭据短路。
- **回滚界限**：新表尚未有新写入/领取时可停新进程恢复旧配置；一旦新表变更，旧表陈旧，禁止直接回退。必须再次停写、核对/搬运新差异并另行批准回切；默认前向修复新路径，不DROP新schema/history。关闭业务适配前先停新Event生产并处理/隔离待投递新通道消息；不能让旧worker把它们变成DEAD。
- **上线后验收**：消息enqueue一次、业务写同事务、lease抢占/恢复、下游重复去重、DEAD告警、target行数/状态/lag、技/业务tenant隔离按§8测试与运维门槛核对；本Plan不声称已运行或可自动通过跨组/XA场景。

## 10. Requirement-to-Step Traceability Matrix

| Requirement | Effective Spec section | Steps | Files | Tests/gates | Completion evidence |
| --- | --- | --- | --- | --- | --- |
| REQ-001 | §7.3.1/9.2.1 | 1,4,5,9 | StateMachineExecutionService、OutboxLifecycleService、MybatisPlusOutboxStore、OutboxDispatcher | TEST-001/003/010 | SSM accepted+transitionEnded和Store条件更新一致 |
| REQ-002 | §7.3/9.2.2/8 | 1,6 | BusinessStateMachineEvent/Service/DeliveryHandler/业务配置 | TEST-004/005/007 | 启用时Event实际过业务SSM，禁用不注册 |
| REQ-003 | §4/9.2.8 | 1,6,8 | BusinessStateMachineEvent/README/contract test | TEST-008/009 | Command/Query/Event职责与身份字段文档/合同一致 |
| REQ-004 | §7.3.4/11 | 1,3,4,5,6,7,9 | OutboxMessageMapper.xml、MybatisPlusOutboxStore、OutboxDispatcher | TEST-002/003/005/011 | 本地事务/owner/version/回滚证据 |
| REQ-005 | §9.2.3/4 | 6 | BusinessStateMachineRepository/Service、business集成测试 | TEST-005/006 | receipt与业务状态同事务；E1/E2/E1短路 |
| REQ-006 | §9.2.6/10 | 3,6,7 | OutboxTechnicalContextExecutor、BusinessStateMachineContextExecutor、Event | TEST-004/006/011 | 两类tenant作用域不混淆、finally恢复 |
| REQ-007 | §8/15/16 | 1,4,6,8 | 自动配置、imports、旧API回归测试、README | TEST-007/009 | 旧协议保持、业务默认关/核心始终有效 |
| REQ-008 | §14/20 | 1–9 | 所有Step测试/README/contract test与质量门禁 | TEST-008/009及§8 | 所有静态/模块报告与运行边界被真实记录 |
| REQ-009 | §10.5/11 | 1,2,3,4,7,8,9 | PO、DAO、XML、Repository、MapStruct、MP Store | TEST-011/012 | 运行期消息DML只经MP，旧JDBC具体类退出 |
| REQ-010 | §9.2.10/11, §11/16 | 2,4,7,8,9 | SQL2/manifest、DDL hook、MigrationService/Result、维护mode | TEST-012/013 | 一个新版本、可续跑完整验证、无历史修改 |

## 11. Risks, Blockers, and User Decisions

| ID | Risk or decision | Impacted Steps/files | Evidence | Owner | Status/action |
| --- | --- | --- | --- | --- | --- |
| DEC-001/002/004/009 | 用户确认两层SSM、单Starter可选适配、Outbox迁移MP及Step9 advisory lock/CAS纠正 | 全部/Step9 | 主Spec §§5.3/DEC-009与2026-09-27用户批准 | User | Closed；Steps1–9实现与验证完成，Step9仅提交其声明路径 |
| RISK-001 | Spring Statemachine官方归档导致未来补丁维护风险 | Step1 POM/BOM | 主Spec §18；已选4.0.2 | 组件维护者 | 持续跟踪的维护风险，非缺失选型；依赖树/安全修复发布时另审 |
| RISK-002 | SOURCE旧表与新egon_outbox可能不同库、存在未托管数据或已写目标 | Steps2/7 | 主Spec §11；runner非空无历史拒绝 | 运维/实施者 | 实施前置：验证同PRIMARY/停写/空schema；不满足则不执行copy/切换 |
| RISK-003 | 其它Agent持续修改共享工作区 | 全部 | 当前`git status --short`列出archetype、skills、Yuheng及`.qoderignore`等非目标路径；Step9仅stage其21个声明路径 | 执行者 | 已逐次复核并保留；提交前后确认这些路径仍未stage/commit |
| RISK-004 | 未筛选reactor verify会触发无关Common Cache/Redis集成测试 | Step9验收 | 首次未筛选命令在EgonColaCacheClusterConvergenceTest的fixture失败，Outbox尚未执行 | 执行者 | 采用§8具名Outbox Failsafe集合隔离；其后14类50项全部通过，不将无关失败归为Outbox回归 |
| RISK-005 | 受控DDL hook与已有自定义LogicalDataSourceFactory/Bean命名冲突 | Steps2/4 | 主Spec §15.3/Common autoConfig | 组件维护者 | ContextRunner明确拒绝/要求自定义factory显式接hook，不覆盖 |
| RISK-006 | Shardingsphere 5.5.3 parser拒绝SKIP LOCKED | Step9 | Spec EVD-019/020；已复现RED并验证表路由advisory lock | 用户已审批DEC-009 | 并发claim/reclaim/cleanup及完整50项隔离PG Failsafe通过；不再是代码阻断，生产切换仍按运维门槛 |

没有未决设计问题。已按用户授权运行本机隔离PostgreSQL/Testcontainers验收；未连接生产数据库，也未执行生产DDL或旧数据切换。真实环境连接、停写窗口及切换核验仍是未来运维步骤，不是本次组件实现的代码阻断。

## 12. Review and Acceptance

### 12.1 Original requirement fidelity

REQ-001～010各在至少一个Step的Requirements行和§10；双层SSM、CQE、MP存储、旧消息无损迁移及用户的既有工作区边界都在§7–9有明确文件与证据。

### 12.2 Spec consistency

§4.5证明每个新增元素对应主Spec §7.0 necessity判定；不存在仅返回参数供下一个Command转发的API。唯一内部澄清是既有JDBC具体测试迁到Spec已要求的新MP集成路径、重复修改文件按符号分Step，详见§6.4；无新业务表、外部接口、框架、字段或授权模型。

### 12.3 Repository executability

各Step声明现存证据、精确相对路径、消费者、字段/状态/失败映射、伪代码、RED原因、GREEN命令、重复路径边界和路径限定提交。Steps 1–8有既有提交；Step9声明的19个Outbox路径与两份治理文档组成当前实现提交。工作区其它Agent的archetype、skill、Yuheng和忽略文件改动均保持未stage、未提交。

### 12.4 Test and release completeness

§8的Step9具名PostgreSQL Failsafe实际执行14类、50项，0 failure/error/skip；选定的38个单测类执行151项，0 failure/error/skip；模块compile及Spec/Plan strict validator、diff hygiene均通过。受控PG结果来自本机隔离Testcontainers，不是生产证明。§9仍将正式停写、同PRIMARY核验和切换后前向修复列为运维步骤，本轮没有连接生产数据库或运行生产迁移。

### 12.5 Blocking Manual Check

以下表保留Plan编制时的设计审查结论，不能代替下面基于实现树和验证报告所做的最终审计。

| Check ID | Applicability | Status | Evidence | Finding | Required action/exception |
| --- | --- | --- | --- | --- | --- |
| MC-ARCH-001 | Applicable | PASS | 主Spec DEC-002、O单Starter与§4.7/5 | 基础组件保留获准原包结构，业务应用不另造层 | None |
| MC-REUSE-001 | Applicable | PASS | §4.7能力账本、Common MP/runner/OutboxStore源码 | 复用现有Egon和Spring职责，无第二DDL/消息bus | None |
| MC-DEP-001 | Applicable | PASS | 用户确认主Spec §§5.3/6.1及Step1 BOM/POM | SSM4.0.2、Egon MP和既有MapStruct版本/传递影响逐项设计 | 实施时effective-pom验证 |
| MC-NAME-001 | Applicable | PASS | §5/7逐文件类型、主Spec§10 | PO/BO/Event/DAO/Repository/Service/Result等后缀准确 | None |
| MC-VALID-001 | Applicable | PASS | 主Spec §10.3，Step3/4/6/7边界与TEST-004/011/012 | 每个输入handoff的Default/MP组、direct ValidationUtils、错误分流已定 | None |
| MC-MODEL-001 | Applicable | PASS | Step3 OutboxMessagePO、Step6 Event/Snapshot、Step7 Result | class四注解+Builder/SuperBuilder/callSuper，EgonModel不重声明 | 编译检查 |
| MC-CONVERT-001 | Applicable | PASS | Step3 MapStruct Converter与BaseForwardConverter及TEST-011 | 真正前向PO→Record，headers helper不靠JSON复制业务模型 | None |
| MC-LOG-001 | Applicable | PASS | Step1/3/4/5/6/7新行为类型与§7伪代码 | @Slf4j安全日志不包含payload/凭据，现有错误保留 | None |
| MC-BEAN-001 | Applicable | PASS | O/lombok.config、主Spec §8 qualifier表、Step自动配置 | 显式Bean名字/final qualified字段/RequiredArgsConstructor，测试装配唯一性 | None |
| MC-UTIL-001 | Applicable | PASS | §4.7、Step1/3/7 import方案 | JDK/现有Commons/Guava，无新util框架或BeanUtils拷贝 | None |
| MC-JSON-001 | Applicable | PASS | Step3/6旧Jackson与Event JSON contract | Boot Jackson、@EnumValue/Instant、严格局部reader/未知字段负测 | None |
| MC-TIME-001 | Applicable | PASS | Step1 Duration、Step3/6 Instant、§11 SQL/TEST-009/011 | java.time、UTC、PG微秒、统一Clock/截止预算 | None |
| MC-CONFIG-001 | Applicable | PASS | Step1/2 typed properties、Step8双语README与§8 profile gate | 组件无生产application环境文件；接入宿主键树必须一致 | None |
| MC-PATTERN-001 | Applicable | PASS | Step1固定State、Step5转移表、Step6 Strategy/Adapter | 复杂两层行为由真实模式承担，无状态switch复制 | None |
| MC-SCOPE-001 | Applicable | PASS | §5唯一清单、§7 commit paths、git scoped diff | 旧SQL1和其它Agent路径不改；重复文件按符号分段 | None |
| MC-TEST-001 | Applicable | PASS | §4.2 RED/GREEN、§7命令、§8静态/PG/运行边界 | 每Step有可观察验证，受控PG gate未冒称已运行 | None |
| MC-BLOCKER-001 | Applicable | PASS | 用户确认主Spec、§11风险与上述16行 | 无未决设计选择；受控PG/切换是后续执行前置 | None |

### 12.6 Final Spec conformance audit

本审计对实施后的Spec逐项重读，并依据最终源文件和实际验证报告判断；不从Step预测或Step Manual Check状态推导。步骤提交范围和工作区边界在`git show`与提交前后`git status`中复核。

| Requirement | Effective Spec | Final implementation evidence | Validation evidence | Status | Boundary |
| --- | --- | --- | --- | --- | --- |
| REQ-001 | §§4, 7.3.1 | `OutboxLifecycleStateMachineFactory`, `OutboxLifecycleService`, `MybatisPlusOutboxStore`；claim/reclaim通过SSM，advisory lock只串行并发 | `OutboxLifecycleServiceTest`; scoped PG 14 classes/50 tests | Satisfied | 技术状态图随请求创建，不持久化运行中Machine |
| REQ-002 | §§4, 9.2.2/8 | `BusinessStateMachineService`, `BusinessStateMachineDeliveryHandler`, optional auto-configuration | Business service/handler tests; `BusinessStateMachineIntegrationTest` in scoped PG 50 | Satisfied | 宿主业务状态与Repository仍由接入方拥有 |
| REQ-003 | §§4, 9.2.8 | CQE Event/Snapshot/SPI、README与组件合同测试 | `ComponentContractGovernanceTest`; selected unit suite 151 tests | Satisfied | 不新增REST/GraphQL/通用总线 |
| REQ-004 | §§4, 7.3.4, 11 | `MybatisPlusOutboxStore`, `OutboxMessageRepository`, Mapper XML：事务owner、租约、advisory lock及version/state/due CAS | `PostgresqlOutboxConcurrencyIntegrationTest`, cleanup/recovery/transaction tests; scoped PG 50 tests | Satisfied | 生产端到端故障演练未运行 |
| REQ-005 | §§4, 9.2.3/4 | Business receipt/fingerprint repository与Service在业务事务内处理重复Event | Business service/handler tests与`BusinessStateMachineIntegrationTest`; scoped PG 50 tests | Satisfied | 不声称跨外部副作用恰好一次 |
| REQ-006 | §§4, 9.2.6/10 | 技术tenant=0 guard；业务tenant、definition version和context executor分别验证及finally恢复 | Business context/definition tests; scoped PG 50 tests | Satisfied | 不改变宿主tenant策略 |
| REQ-007 | §§4, 8, 15/16 | 核心技术图始终装配；业务状态机条件启用；旧Outbox API与DeliveryHandler保留 | auto-configuration/legacy API unit tests; Rabbit/HTTP/sample scoped PG tests | Satisfied | 旧at-least-once及幂等责任保留 |
| REQ-008 | §§4, 14/20 | 英文/中文README、治理合同测试、完整验证与运行边界记录 | both strict validators; `ComponentContractGovernanceTest`; `git diff --check` | Satisfied | 生产发布与人工切换不在实现提交内 |
| REQ-009 | §§4, 10.5, 11 | MP PO/DAO/XML/Repository/MapStruct及默认`MybatisPlusOutboxStore`；运行期不回退旧JDBC Store | converter/MP integration tests; scoped PG 50; source search finds no old JDBC Store path | Satisfied | 旧来源仅供显式维护迁移入口读取 |
| REQ-010 | §§4, 9.2.10/11, 11/16 | 一版新SQL/manifest、现有DDL runner hook、`OutboxLegacyMigrationService`与校验/续跑流程 | DDL/migration Failsafe included in scoped PG 50; checksum/SQL1 immutability checks | Satisfied | 生产数据库上未执行DDL或数据切换 |

实现未更改既有SQL1；没有启应用、MQ或生产数据库。用户授权的隔离Testcontainers覆盖本次数据库/解析/事务验收。

### 12.7 Final blocking Manual Check

状态根据最终实现范围、源文件复查和实际命令独立确定。`N/A`只用于最终实现中该Concern确实无变化的行。

| Check ID | Applicability | Status | Final evidence and finding |
| --- | --- | --- | --- |
| MC-ARCH-001 | Applicable | PASS | Starter现有api/store/dispatch/persistence结构保留；无业务层或新模块 |
| MC-REUSE-001 | Applicable | PASS | 复用Common MP、现有DDL runner、OutboxStore/DeliveryHandler及Spring事务；无第二套store或调度框架 |
| MC-DEP-001 | Applicable | PASS | SSM及MP依赖沿用Spec批准的BOM/Starter集成；Step9无POM/BOM依赖变化 |
| MC-NAME-001 | Applicable | PASS | 新增类型按Service/Handler/StateMachine/DAO/Repository/PO等语义命名；Step9未新增载体类型 |
| MC-VALID-001 | Applicable | PASS | CQE、Store、DAO/Repository及Migration Service边界保留Jakarta约束和显式初始化验证；单测与PG Mapper路径通过 |
| MC-MODEL-001 | Applicable | PASS | 最终PO/BO/Event遵循对应Lombok/record规则；Step9不改持久化模型；compile与converter tests通过 |
| MC-CONVERT-001 | Applicable | PASS | PO到公开Record的转换仍是MapStruct+Egon BaseForwardConverter；Step9不绕过转换器 |
| MC-LOG-001 | Applicable | PASS | 业务Bean使用既有`@Slf4j`约定；Step9未引入payload/凭据日志 |
| MC-BEAN-001 | Applicable | PASS | 自动配置Bean具名、Lombok构造依赖使用Qualifier；模块编译和真实Spring PG上下文通过 |
| MC-UTIL-001 | Applicable | PASS | 实现复用JDK/Spring/MyBatis/PG现有能力；Step9无依赖或工具库增加 |
| MC-JSON-001 | Applicable | PASS | 传输与业务Event JSON仍使用Spring Boot Jackson约定；Step9未更改序列化字段 |
| MC-TIME-001 | Applicable | PASS | 时间边界使用`Instant`/`Duration`/`Clock`和数据库时钟；没有引入`java.util.Date`路径 |
| MC-CONFIG-001 | Applicable | PASS | 状态机/MP配置键由properties与双语README一致管理；Step9没有增删配置键 |
| MC-PATTERN-001 | Applicable | PASS | 技术/业务状态迁移由State pattern与Strategy/Adapter协作；锁及CAS仅负责并发写保护 |
| MC-SCOPE-001 | Applicable | PASS | Step9提交只包含19个Outbox文件与Spec/Plan；其他Agent文件保持未提交 |
| MC-TEST-001 | Applicable | PASS | 实际报告：151 unit、50 scoped PostgreSQL tests均0失败/错误/跳过；compile、两个strict validators和diff check通过 |
| MC-BLOCKER-001 | Applicable | PASS | 无未决实现要求；未来生产数据库切换明确留在运维门禁，不阻断组件交付 |

### 12.8 Final Literal Rule audit

| Rule | Applicability | Status | Final finding |
| --- | --- | --- | --- |
| Rule 1 | Applicable | PASS | Java类型与操作名遵循POJO、CQE和持久化职责命名 |
| Rule 2 | Applicable | PASS | 输入边界使用Jakarta Validation/分组与显式校验；结果及affected-row由CQE边界检查 |
| Rule 3 | Applicable | PASS | 复杂类采用完整Lombok约定；PO→Record使用MapStruct及Egon BaseForwardConverter |
| Rule 4 | Applicable | PASS | Spring Bean具名，构造注入字段Qualifier清楚，业务类使用`@Slf4j` |
| Rule 5 | Applicable | PASS | 未引入未批准工具库或BeanUtils对象映射 |
| Rule 6 | Applicable | PASS | JSON复用Boot Jackson；持久化/传输状态使用明确编码而非ordinal |
| Rule 7 | Applicable | PASS | 配置键与环境/中英接入文档保持一致；本修正未增加profile键 |
| Rule 9 | Applicable | PASS | 复杂生命周期使用Spring StateMachine、Strategy和Adapter；不复制状态决策switch |
| Rule 10 | Applicable | PASS | Java时间使用`java.time`；存储截止由数据库时钟提供 |
| Rule 11 | Applicable | PASS | 保留获批Starter结构、Egon MP、受管DDL及CQE/outbox边界 |

### 12.9 Final verdict

PASS — Ready for user review

九个Step均已完成并有路径限定提交和有效验证证据；这里的“review”是对已完成实现交付的审阅。生产DDL、生产旧表复制与业务消费者发布未执行；它们属于§8/§9记录的未来受控运维操作，不被本结论冒称已完成。
