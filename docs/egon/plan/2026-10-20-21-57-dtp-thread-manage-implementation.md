# ThreadManage 动态线程池改造实施计划

| Field | Value |
| --- | --- |
| Document | `2026-10-20-21-57-dtp-thread-manage-implementation.md` |
| Template Version | 5 |
| Status | Completed |
| Created | 2026-10-20 21:57 Asia/Shanghai |
| Updated | 2026-10-20 22:50 Asia/Shanghai |
| Owner | mario |
| Repository | Egon-COLA |
| Scope | egon-cola-components/egon-cola-component-dynamic-thread-pool |
| Source Requirement | 用户明确要求按该方案实施，已批准依赖；不启动项目 |
| Baseline Revision | codex/dtp-thread-manage / b8f9c9a28；仅原方案 untracked |
| Implements Spec | [ThreadManage Spec](../spec/2026-10-20-20-53-dtp-thread-manage-observability.md) |
| Spec Status | Implemented |
| Spec Revision | 2026-10-20 21:57 Asia/Shanghai；用户当前消息明确批准 |
| Effective Specs | [ThreadManage Spec](../spec/2026-10-20-20-53-dtp-thread-manage-observability.md) |
| Depends On Plans | None |
| Supersedes | None |
| Superseded By | None |
| Related Plans | None |

## 1. Summary
一个逻辑任务、一个最终提交。按依赖顺序执行测试→模型/队列/观察→注册/执行器→自动配置→管理/指标→配置/文档→全模块验证；不运行应用或真实 Redis。

## 2. Target Spec and Effective Design
[ThreadManage Spec](../spec/2026-10-20-20-53-dtp-thread-manage-observability.md) 的 §4/§7..§16 为实施依据；§5.4依赖已获当前用户消息批准；MapStruct编译API为同一获准能力的必要编译输入。原迁移文档只保留组件模块及依赖方向，公共契约收敛只约束原枚举/信封。

## 3. Effective Requirements and Acceptance
| Requirement | Source Spec section | Effective statement | Observable acceptance | Implementation impact |
| --- | --- | --- | --- | --- |
| REQ-001 | Spec §4 | 统一管理 | Spec TEST-001 与 §14 | Step 1 |
| REQ-002 | Spec §4 | 旧任务接口兼容 | Spec TEST-002 与 §14 | Step 1 |
| REQ-003 | Spec §4 | MDC/Trace恢复 | Spec TEST-003 与 §14 | Step 1 |
| REQ-004 | Spec §4 | 任务监控 | Spec TEST-004 与 §14 | Step 1 |
| REQ-005 | Spec §4 | 动态线程与队列调参 | Spec TEST-005 与 §14 | Step 1 |
| REQ-006 | Spec §4 | Redis安全下发 | Spec TEST-006 与 §14 | Step 1 |
| REQ-007 | Spec §4 | 范围与生命周期 | Spec TEST-007 与 §14 | Step 1 |

## 4. Implementation Strategy and Dependency Order
测试首先在已通过的旧Service/Controller基线上定义缺失行为；公开类型缺失用反射测试得到行为失败，随后实现同一规范目录及受控任务边界。
### 4.5 Spec Simplicity and Implementation-necessity Audit
沿用 Spec §7.0：Facade/Adapter/Decorator；复用 common-trace、ValidationUtils、BaseForwardConverter 和原 Redis，无新 HTTP 工厂/数据库/UI。
### 4.6 Change-unit Dependency Matrix
测试 → 已批准依赖 → Command/queue/observer → executor/registry/manager → BPP/autoconfig → admin/metrics → profile/docs。所有单元归 Step 1，按用户一逻辑任务最多一提交约束一次验证交付。
### 4.7 Java, Spring, and Egon-COLA Implementation Standards
保留既有基础设施组件包与 A→S/T→S 方向，未强加业务三层。现有 trace/adapter/registry 足够；仅队列/任务观察缺口采用 Spec 规定的扩展，新增依赖已有用户批准。
| Concern | Current repository evidence | Effective Spec decision | Planned implementation consequence | Owning Steps/checks |
| --- | --- | --- | --- | --- |
| Existing component architecture | DTP starter/admin/test 与现有 package | Spec §6.1 | 原目录与依赖方向保持 | Step 1 / MC-ARCH-001 |
| Validation/conversion/Trace | common-core 与 common-trace | Spec §6.2/§10 | 复用Scope/ValidationUtils/BaseForwardConverter，依赖已批准 | Step 1 / MC-REUSE-001 |

| Need | Candidates inspected | Exact evidence | Fit/gap | Decision | Added dependency/custom code | Owning Step/check |
| --- | --- | --- | --- | --- | --- | --- |
| Task context | TraceContext、Dtp wrappers | S/context与common-trace | 原能力已存在 | 复用不重写 | None | Step 1 / MC-REUSE-001 |
| Queue/observation | JDK BlockingQueue与ManagedExecutor | Spec §2/§7 | 固定队列/缺任务终态 | 既定框架扩展 | 已批准小型队列/观察组件 | Step 1 / MC-DEP-001 |
| Runtime validation/documentation | Jakarta API、root版本属性 | S/A POM | 缺provider与API生成器 | 用户已批准增补 | Validation、springdoc-api、MapStruct processor | Step 1 / MC-DEP-001 |

### 4.8 User-mandated Java Rule Implementation Matrix
| Literal rule | Spec source | Repository evidence | Exact files and order | Pseudocode obligations | Validation gate | Steps | Status/blocker |
| --- | --- | --- | --- | --- | --- | --- | --- |
| Rule 1 | Spec §6.2 | DTP 当前结构与公共签名 | §5与§7精确清单 | 遵循该行既定实现约束；不破坏旧setter/Bean/enum | 模块编译及§8对应测试 | Step 1 | PASS |
| Rule 2 | Spec §6.2 | DTP 当前结构与公共签名 | §5与§7精确清单 | 遵循该行既定实现约束；不破坏旧setter/Bean/enum | 模块编译及§8对应测试 | Step 1 | PASS |
| Rule 3 | Spec §6.2 | DTP 当前结构与公共签名 | §5与§7精确清单 | 遵循该行既定实现约束；不破坏旧setter/Bean/enum | 模块编译及§8对应测试 | Step 1 | PASS |
| Rule 4 | Spec §6.2 | DTP 当前结构与公共签名 | §5与§7精确清单 | 遵循该行既定实现约束；不破坏旧setter/Bean/enum | 模块编译及§8对应测试 | Step 1 | PASS |
| Rule 5 | Spec §6.2 | DTP 当前结构与公共签名 | §5与§7精确清单 | 遵循该行既定实现约束；不破坏旧setter/Bean/enum | 模块编译及§8对应测试 | Step 1 | PASS |
| Rule 6 | Spec §6.2 | DTP 当前结构与公共签名 | §5与§7精确清单 | 遵循该行既定实现约束；不破坏旧setter/Bean/enum | 模块编译及§8对应测试 | Step 1 | PASS |
| Rule 7 | Spec §6.2 | DTP 当前结构与公共签名 | §5与§7精确清单 | 遵循该行既定实现约束；不破坏旧setter/Bean/enum | 模块编译及§8对应测试 | Step 1 | PASS |
| Rule 9 | Spec §6.2 | DTP 当前结构与公共签名 | §5与§7精确清单 | 遵循该行既定实现约束；不破坏旧setter/Bean/enum | 模块编译及§8对应测试 | Step 1 | PASS |
| Rule 10 | Spec §6.2 | DTP 当前结构与公共签名 | §5与§7精确清单 | 遵循该行既定实现约束；不破坏旧setter/Bean/enum | 模块编译及§8对应测试 | Step 1 | PASS |
| Rule 11 | Spec §6.2 | DTP 当前结构与公共签名 | §5与§7精确清单 | 遵循该行既定实现约束；不破坏旧setter/Bean/enum | 模块编译及§8对应测试 | Step 1 | PASS |
### 4.9 Time semantics and conversion contract


Time contract: Required

| Time ID | Fields/symbols | Semantics | Java/runtime type | Storage policy | Wire policy | Business zone source | Display zone source | Boundary / DST rules | Precision / Clock | Compatibility | Verification |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| TIME-001 | 排队/执行耗时、slow-task-threshold、queue-wait-threshold、冷却期 | Duration | Duration；nanoTime 差值为内部纳秒 long | Not persisted | Duration | Not applicable: 相对耗时 | Not applicable: 无 UI | 不依赖时区/DST；差值不作为时间戳 | nanoTime 单调差值；Micrometer 秒 | 既有 keepAliveSeconds 保持秒制 | Step 1; §8 TEST-004/010 |
| TIME-002 | DtpAuditEvent.createdAt、snapshot.reportTime、message.timestamp | Occurred instant | Instant | UTC instant | UTC Z | Not applicable: 事件瞬间 | Not applicable: 本次无显示层 | 不更改 JVM 默认区；事件日期键继续 UTC | Instant.now；JSON 保持 Z，允许小数秒 | 原 Redis codec 及 UTC 日期键不变 | Step 1; §8 TEST-009/012 |


## 5. Change File Tree
| Operation | Path | Current evidence/symbol | Final symbols/state | Responsibility | Step | Requirements | Validation owner |
| --- | --- | --- | --- | --- | --- | --- | --- |
| CREATE | `egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/test/java/top/egon/cola/component/dtp/executor/ThreadManageTest.java` | Spec §8及现有同包实现 | ThreadManageTest | reflection 查找 ThreadManage 和 CreateCommand；初始化后新池直接 submit 传播 Trace，统计失败与取消，队列支持运行期改容 | Step 1 | REQ-001, REQ-002, REQ-003, REQ-004, REQ-005, REQ-006, REQ-007 | §8 |
| CREATE | `egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/test/java/top/egon/cola/component/dtp/executor/support/ResizableCapacityBlockingQueueTest.java` | Spec §8及现有同包实现 | ResizableCapacityBlockingQueueTest | put 阻塞后扩容唤醒；低于 size 缩容原子拒绝；drainTo/iterator/remove/clear/interrupt 正确 | Step 1 | REQ-001, REQ-002, REQ-003, REQ-004, REQ-005, REQ-006, REQ-007 | §8 |
| CREATE | `egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/test/java/top/egon/cola/component/dtp/context/DtpTaskObserverTest.java` | Spec §8及现有同包实现 | DtpTaskObserverTest | submit/invokeAll/invokeAny 的失败取消一次计数；CallerRuns 恢复 MDC；旧预包装保持原 Trace | Step 1 | REQ-001, REQ-002, REQ-003, REQ-004, REQ-005, REQ-006, REQ-007 | §8 |
| MODIFY | `egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/pom.xml` | Spec §8及现有同包实现 | pom | 增加已批准 Validation starter；保持 common-trace core 与可选 micrometer 边界 | Step 1 | REQ-001, REQ-002, REQ-003, REQ-004, REQ-005, REQ-006, REQ-007 | §8 |
| CREATE | `egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/main/java/top/egon/cola/component/dtp/domain/model/entity/ThreadPoolCreateCommand.java` | Spec §8及现有同包实现 | ThreadPoolCreateCommand | 显式 executorName/core/max/queue，Duration keepAlive 默认60s，原生 factory/handler；class Lombok 基线与 Create 约束 | Step 1 | REQ-001, REQ-002, REQ-003, REQ-004, REQ-005, REQ-006, REQ-007 | §8 |
| MODIFY | `egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/main/java/top/egon/cola/component/dtp/domain/model/entity/ExecutorUpdateCommand.java` | Spec §8及现有同包实现 | ExecutorUpdateCommand | 新增 nullable queueCapacity，保留所有旧字段及 void setter；分组原生字段与交叉约束 | Step 1 | REQ-001, REQ-002, REQ-003, REQ-004, REQ-005, REQ-006, REQ-007 | §8 |
| CREATE | `egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/main/java/top/egon/cola/component/dtp/executor/support/ResizableCapacityBlockingQueue.java` | Spec §8及现有同包实现 | ResizableCapacityBlockingQueue | 单 ReentrantLock 与 notEmpty/notFull；容量验证与调参回调在锁内；FIFO/中断/drainTo/identity iterator | Step 1 | REQ-001, REQ-002, REQ-003, REQ-004, REQ-005, REQ-006, REQ-007 | §8 |
| CREATE | `egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/main/java/top/egon/cola/component/dtp/context/DtpTaskObserver.java` | Spec §8及现有同包实现 | DtpTaskObserver | 每池低基数统计、token 去重、TraceScope，计时回调隔离；日志限频；保留预包装快照 | Step 1 | REQ-001, REQ-002, REQ-003, REQ-004, REQ-005, REQ-006, REQ-007 | §8 |
| CREATE | `egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/main/java/top/egon/cola/component/dtp/context/DtpObservedFutureTask.java` | Spec §8及现有同包实现 | DtpObservedFutureTask | 任务执行边界统计真实终态；done 观察取消；计数 CAS，异常不被外壳误判 | Step 1 | REQ-001, REQ-002, REQ-003, REQ-004, REQ-005, REQ-006, REQ-007 | §8 |
| CREATE | `egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/main/java/top/egon/cola/component/dtp/executor/managed/DtpThreadPoolTaskExecutor.java` | Spec §8及现有同包实现 | DtpThreadPoolTaskExecutor | 幂等 initialize；可调队列；execute/submit/bulk 观察；拒绝处理原样委托；新后台任务根 Trace | Step 1 | REQ-001, REQ-002, REQ-003, REQ-004, REQ-005, REQ-006, REQ-007 | §8 |
| MODIFY | `egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/main/java/top/egon/cola/component/dtp/executor/ManagedExecutor.java` | Spec §8及现有同包实现 | ManagedExecutor | 增加 default 能力/observer 方法，现有实现兼容 | Step 1 | REQ-001, REQ-002, REQ-003, REQ-004, REQ-005, REQ-006, REQ-007 | §8 |
| MODIFY | `egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/main/java/top/egon/cola/component/dtp/executor/ManagedExecutorRegistry.java` | Spec §8及现有同包实现 | ManagedExecutorRegistry | 唯一同步注册表；原 get/list/constructor 保留；原对象/创建定义/归属原子登记，增量指标通知 | Step 1 | REQ-001, REQ-002, REQ-003, REQ-004, REQ-005, REQ-006, REQ-007 | §8 |
| CREATE | `egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/main/java/top/egon/cola/component/dtp/executor/ThreadManage.java` | Spec §8及现有同包实现 | ThreadManage | Spring facade，newThreadPool/executor/changeThreadPool；名称冲突拒绝，资源归属、关闭门禁；不访问 Redis | Step 1 | REQ-001, REQ-002, REQ-003, REQ-004, REQ-005, REQ-006, REQ-007 | §8 |
| MODIFY | `egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/main/java/top/egon/cola/component/dtp/executor/support/ThreadPoolResizeSupport.java` | Spec §8及现有同包实现 | ThreadPoolResizeSupport | 完整目标验证、单池串行、Spring getter/底层同步；queueCapacity 受锁验证后再 setter；失败恢复真实 after | Step 1 | REQ-001, REQ-002, REQ-003, REQ-004, REQ-005, REQ-006, REQ-007 | §8 |
| MODIFY | `egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/main/java/top/egon/cola/component/dtp/executor/adapter/ThreadPoolExecutorManagedExecutor.java` | Spec §8及现有同包实现 | ThreadPoolExecutorManagedExecutor | 旧构造保留，支持观察与原平台指标；update 复用 ResizeSupport，队列能力识别 | Step 1 | REQ-001, REQ-002, REQ-003, REQ-004, REQ-005, REQ-006, REQ-007 | §8 |
| MODIFY | `egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/main/java/top/egon/cola/component/dtp/executor/adapter/ThreadPoolTaskExecutorManagedExecutor.java` | Spec §8及现有同包实现 | ThreadPoolTaskExecutorManagedExecutor | 原 Spring Bean 不替换；保留 decorator；adapter submit 实际 Callable 观察；统一 resize | Step 1 | REQ-001, REQ-002, REQ-003, REQ-004, REQ-005, REQ-006, REQ-007 | §8 |
| MODIFY | `egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/main/java/top/egon/cola/component/dtp/executor/adapter/BoundedVirtualThreadManagedExecutor.java` | Spec §8及现有同包实现 | BoundedVirtualThreadManagedExecutor | 保留虚拟限制与原指标；新增观察接点，防重复计数，拒绝混用平台参数 | Step 1 | REQ-001, REQ-002, REQ-003, REQ-004, REQ-005, REQ-006, REQ-007 | §8 |
| MODIFY | `egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/main/java/top/egon/cola/component/dtp/executor/virtual/BoundedVirtualThreadExecutor.java` | Spec §8及现有同包实现 | BoundedVirtualThreadExecutor | 保留原 semaphore/FutureTask 与 bulk；仅添加任务观察接点，不二次计数或改变取消 | Step 1 | REQ-001, REQ-002, REQ-003, REQ-004, REQ-005, REQ-006, REQ-007 | §8 |
| MODIFY | `egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/main/java/top/egon/cola/component/dtp/context/DtpContextAwareExecutorService.java` | Spec §8及现有同包实现 | DtpContextAwareExecutorService | 旧构造/API 保留，受控观察 submit/bulk 的真实委托，原未来异常取消语义不变 | Step 1 | REQ-001, REQ-002, REQ-003, REQ-004, REQ-005, REQ-006, REQ-007 | §8 |
| MODIFY | `egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/main/java/top/egon/cola/component/dtp/context/DtpRunnable.java` | Spec §8及现有同包实现 | DtpRunnable | 保留 wrap(final模板)；可读取组件内部捕获 Trace；不覆盖预包装时点 | Step 1 | REQ-001, REQ-002, REQ-003, REQ-004, REQ-005, REQ-006, REQ-007 | §8 |
| MODIFY | `egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/main/java/top/egon/cola/component/dtp/context/DtpCallable.java` | Spec §8及现有同包实现 | DtpCallable | 保留 wrap/TraceRouteCallable；在真实 call 委托边界观察，上下文快照复用 | Step 1 | REQ-001, REQ-002, REQ-003, REQ-004, REQ-005, REQ-006, REQ-007 | §8 |
| MODIFY | `egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/main/java/top/egon/cola/component/dtp/context/DtpSupplier.java` | Spec §8及现有同包实现 | DtpSupplier | 保留 wrap/TraceRouteSupplier；提供有效 Trace 观察，不改变旧 Supplier 契约 | Step 1 | REQ-001, REQ-002, REQ-003, REQ-004, REQ-005, REQ-006, REQ-007 | §8 |
| MODIFY | `egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/main/java/top/egon/cola/component/dtp/context/DtpTaskDecorator.java` | Spec §8及现有同包实现 | DtpTaskDecorator | 复用旧 wrap；与新受控任务 token 协同，防止 MDC 与统计双重装饰 | Step 1 | REQ-001, REQ-002, REQ-003, REQ-004, REQ-005, REQ-006, REQ-007 | §8 |
| MODIFY | `egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/main/java/top/egon/cola/component/dtp/domain/DynamicThreadPoolService.java` | Spec §8及现有同包实现 | DynamicThreadPoolService | 原身份校验和失败结果保留；逐池隔离快照失败；更新使用统一锁/校验入口 | Step 1 | REQ-001, REQ-002, REQ-003, REQ-004, REQ-005, REQ-006, REQ-007 | §8 |
| CREATE | `egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/main/java/top/egon/cola/component/dtp/config/DtpExecutorBeanPostProcessor.java` | Spec §8及现有同包实现 | DtpExecutorBeanPostProcessor | static Bean 工厂、延迟 provider，不 eager 注入所有池；初始化后登记原对象不代理 | Step 1 | REQ-001, REQ-002, REQ-003, REQ-004, REQ-005, REQ-006, REQ-007 | §8 |
| MODIFY | `egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/main/java/top/egon/cola/component/dtp/config/DynamicThreadPoolAutoConfig.java` | Spec §8及现有同包实现 | DynamicThreadPoolAutoConfig | 空 Registry 与 threadManage/observer/validator；BPP 静态注册，原服务 Bean 名保留；Redis 与可选指标边界保留 | Step 1 | REQ-001, REQ-002, REQ-003, REQ-004, REQ-005, REQ-006, REQ-007 | §8 |
| MODIFY | `egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/main/java/top/egon/cola/component/dtp/config/DynamicThreadPoolAutoProperties.java` | Spec §8及现有同包实现 | DynamicThreadPoolAutoProperties | 新增 observation Duration 阈值与可选告警配置，注解/校验与密码日志保护 | Step 1 | REQ-001, REQ-002, REQ-003, REQ-004, REQ-005, REQ-006, REQ-007 | §8 |
| MODIFY | `egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/main/java/top/egon/cola/component/dtp/metrics/DtpMeterBinder.java` | Spec §8及现有同包实现 | DtpMeterBinder | 保留原 Gauge；新池增量绑定，任务 Counter/Timer、能力/queueCapacity Gauge；只解绑自有 meters | Step 1 | REQ-001, REQ-002, REQ-003, REQ-004, REQ-005, REQ-006, REQ-007 | §8 |
| MODIFY | `egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/main/java/top/egon/cola/component/dtp/trigger/job/ThreadPoolDataReportJob.java` | Spec §8及现有同包实现 | ThreadPoolDataReportJob | 原报告周期保留；单池隔离；可选连续压力告警与冷却，不自动改池 | Step 1 | REQ-001, REQ-002, REQ-003, REQ-004, REQ-005, REQ-006, REQ-007 | §8 |
| MODIFY | `egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/main/java/top/egon/cola/component/dtp/trigger/listener/ThreadPoolConfigAdjustListener.java` | Spec §8及现有同包实现 | ThreadPoolConfigAdjustListener | 目标过滤、envelope/payload 一致性、有限 Guava 缓存；同 ID 冲突拒绝；审计失败不伪回滚 | Step 1 | REQ-001, REQ-002, REQ-003, REQ-004, REQ-005, REQ-006, REQ-007 | §8 |
| MODIFY | `egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-admin/pom.xml` | Spec §8及现有同包实现 | pom | 已批准 webmvc-api 与 MapStruct processor；保留既有 Lombok/Boot processor，不增 UI | Step 1 | REQ-001, REQ-002, REQ-003, REQ-004, REQ-005, REQ-006, REQ-007 | §8 |
| MODIFY | `egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-admin/src/main/java/top/egon/cola/component/dtp/admin/trigger/model/ResizeExecutorRequest.java` | Spec §8及现有同包实现 | ResizeExecutorRequest | nullable queueCapacity、原签名与 enum 字符串；API schema 与 PlatformResize 校验 | Step 1 | REQ-001, REQ-002, REQ-003, REQ-004, REQ-005, REQ-006, REQ-007 | §8 |
| CREATE | `egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-admin/src/main/java/top/egon/cola/component/dtp/admin/trigger/converter/ResizeExecutorConverter.java` | Spec §8及现有同包实现 | ResizeExecutorConverter | MapStruct BaseForwardConverter；body参数映射，app/instance/name 来自路径；operator 保留 | Step 1 | REQ-001, REQ-002, REQ-003, REQ-004, REQ-005, REQ-006, REQ-007 | §8 |
| MODIFY | `egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-admin/src/main/java/top/egon/cola/component/dtp/admin/trigger/DynamicThreadPoolController.java` | Spec §8及现有同包实现 | DynamicThreadPoolController | 原 Response 与 HTTP 路由保持；组校验、生成 converter、可选容量下发；OpenAPI3 注解 | Step 1 | REQ-001, REQ-002, REQ-003, REQ-004, REQ-005, REQ-006, REQ-007 | §8 |
| MODIFY | `egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-admin/src/main/resources/application.yml` | Spec §8及现有同包实现 | application | 新增 observation 完整键结构；A 的 springdoc 生产关闭，test/dev 可开；其他值保持 | Step 1 | REQ-001, REQ-002, REQ-003, REQ-004, REQ-005, REQ-006, REQ-007 | §8 |
| MODIFY | `egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-admin/src/main/resources/application-dev.yml` | Spec §8及现有同包实现 | application-dev | 新增 observation 完整键结构；A 的 springdoc 生产关闭，test/dev 可开；其他值保持 | Step 1 | REQ-001, REQ-002, REQ-003, REQ-004, REQ-005, REQ-006, REQ-007 | §8 |
| MODIFY | `egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-admin/src/main/resources/application-test.yml` | Spec §8及现有同包实现 | application-test | 新增 observation 完整键结构；A 的 springdoc 生产关闭，test/dev 可开；其他值保持 | Step 1 | REQ-001, REQ-002, REQ-003, REQ-004, REQ-005, REQ-006, REQ-007 | §8 |
| MODIFY | `egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-admin/src/main/resources/application-prod.yml` | Spec §8及现有同包实现 | application-prod | 新增 observation 完整键结构；A 的 springdoc 生产关闭，test/dev 可开；其他值保持 | Step 1 | REQ-001, REQ-002, REQ-003, REQ-004, REQ-005, REQ-006, REQ-007 | §8 |
| MODIFY | `egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-test/src/main/resources/application.yml` | Spec §8及现有同包实现 | application | 新增 observation 完整键结构；A 的 springdoc 生产关闭，test/dev 可开；其他值保持 | Step 1 | REQ-001, REQ-002, REQ-003, REQ-004, REQ-005, REQ-006, REQ-007 | §8 |
| MODIFY | `egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-test/src/main/resources/application-dev.yml` | Spec §8及现有同包实现 | application-dev | 新增 observation 完整键结构；A 的 springdoc 生产关闭，test/dev 可开；其他值保持 | Step 1 | REQ-001, REQ-002, REQ-003, REQ-004, REQ-005, REQ-006, REQ-007 | §8 |
| MODIFY | `egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-test/src/main/resources/application-test.yml` | Spec §8及现有同包实现 | application-test | 新增 observation 完整键结构；A 的 springdoc 生产关闭，test/dev 可开；其他值保持 | Step 1 | REQ-001, REQ-002, REQ-003, REQ-004, REQ-005, REQ-006, REQ-007 | §8 |
| MODIFY | `egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-test/src/main/resources/application-prod.yml` | Spec §8及现有同包实现 | application-prod | 新增 observation 完整键结构；A 的 springdoc 生产关闭，test/dev 可开；其他值保持 | Step 1 | REQ-001, REQ-002, REQ-003, REQ-004, REQ-005, REQ-006, REQ-007 | §8 |
| MODIFY | `egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-test/src/main/java/top/egon/cola/component/dtp/test/config/ThreadPoolConfig.java` | Spec §8及现有同包实现 | ThreadPoolConfig | 组件样例切换 ThreadManage 创建，保留平台与虚拟池接入说明 | Step 1 | REQ-001, REQ-002, REQ-003, REQ-004, REQ-005, REQ-006, REQ-007 | §8 |
| CREATE | `egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/test/java/top/egon/cola/component/dtp/config/DtpExecutorBeanPostProcessorTest.java` | Spec §8及现有同包实现 | DtpExecutorBeanPostProcessorTest | 无循环、别名身份、生命周期、旧 Bean/可选 Micrometer 兼容 | Step 1 | REQ-001, REQ-002, REQ-003, REQ-004, REQ-005, REQ-006, REQ-007 | §8 |
| CREATE | `egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/test/java/top/egon/cola/component/dtp/metrics/DtpTaskMetricsTest.java` | Spec §8及现有同包实现 | DtpTaskMetricsTest | Counter/Timer、动态绑定释放、无 traceId 标签、部分能力与压力冷却 | Step 1 | REQ-001, REQ-002, REQ-003, REQ-004, REQ-005, REQ-006, REQ-007 | §8 |
| CREATE | `egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/test/java/top/egon/cola/component/dtp/trigger/listener/DtpConfigChangeSafetyTest.java` | Spec §8及现有同包实现 | DtpConfigChangeSafetyTest | 目标过滤、重复/冲突消息、审计失败、MDC 恢复 | Step 1 | REQ-001, REQ-002, REQ-003, REQ-004, REQ-005, REQ-006, REQ-007 | §8 |
| CREATE | `egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/test/java/top/egon/cola/component/dtp/executor/support/DtpResizeSafetyTest.java` | Spec §8及现有同包实现 | DtpResizeSafetyTest | Spring 与底层参数同步、所有非法组合零变更、队列锁/虚拟缩容 | Step 1 | REQ-001, REQ-002, REQ-003, REQ-004, REQ-005, REQ-006, REQ-007 | §8 |
| CREATE | `egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-admin/src/test/java/top/egon/cola/component/dtp/admin/trigger/converter/ResizeExecutorConverterTest.java` | Spec §8及现有同包实现 | ResizeExecutorConverterTest | 所有字段与路径身份映射，旧请求缺新字段兼容 | Step 1 | REQ-001, REQ-002, REQ-003, REQ-004, REQ-005, REQ-006, REQ-007 | §8 |
| CREATE | `egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-admin/src/test/java/top/egon/cola/component/dtp/admin/trigger/DynamicThreadPoolOpenApiTest.java` | Spec §8及现有同包实现 | DynamicThreadPoolOpenApiTest | MockMvc /v3/api-docs 解析真实 OAS：operationId、Boolean信封、queue可选、enum、HTTP状态 | Step 1 | REQ-001, REQ-002, REQ-003, REQ-004, REQ-005, REQ-006, REQ-007 | §8 |
| CREATE | `egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/test/java/top/egon/cola/component/dtp/config/DtpObservationPropertiesTest.java` | Spec §8及现有同包实现 | DtpObservationPropertiesTest | Duration/组约束/全profile新增键对照与生产docs关闭 | Step 1 | REQ-001, REQ-002, REQ-003, REQ-004, REQ-005, REQ-006, REQ-007 | §8 |
| MODIFY | `egon-cola-components/egon-cola-component-dynamic-thread-pool/README.md` | Spec §8及现有同包实现 | README | 新增 ThreadManage 使用示例、原Runnable等API兼容、能力边界、指标/告警/动态队列与Redis异步语义 | Step 1 | REQ-001, REQ-002, REQ-003, REQ-004, REQ-005, REQ-006, REQ-007 | §8 |
| MODIFY | `egon-cola-components/egon-cola-component-dynamic-thread-pool/README.zh-CN.md` | Spec §8及现有同包实现 | README.zh-CN | 新增 ThreadManage 使用示例、原Runnable等API兼容、能力边界、指标/告警/动态队列与Redis异步语义 | Step 1 | REQ-001, REQ-002, REQ-003, REQ-004, REQ-005, REQ-006, REQ-007 | §8 |

| MODIFY | `egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-admin/src/test/java/top/egon/cola/component/dtp/admin/trigger/DynamicThreadPoolControllerTest.java` | 原Controller测试/测试日志边界 | DynamicThreadPoolControllerTest | 为构造注入提供真实生成mapper Spy与ValidationUtils mock，保留原16项契约断言 | Step 1 | REQ-002, REQ-007 | Maven package |
| MODIFY | `egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-admin/src/test/resources/logback-dtp-test.xml` | 原Controller测试/测试日志边界 | logback-dtp-test | 隔离MockMvc Context日志到控制台，避免生成项目data目录 | Step 1 | REQ-002, REQ-007 | Maven package |

## 6. Prerequisites, Constraints, and Plan Clarifications
Java21/Maven3.9.14已验证；原 Service/Controller 针对性测试 BUILD SUCCESS（/tmp/egon-dtp-baseline.log）。无数据库迁移/脚手架生成/脚本harness。内部观察状态作为命名类的嵌套类，无新公共载体；旧公开构造兼容。保留现有目录不进行结构迁移。

## 7. Ordered File-by-file Implementation Steps
### Step 1 — 交付兼容的统一线程池管理与可观测动态调容
- Requirements: REQ-001, REQ-002, REQ-003, REQ-004, REQ-005, REQ-006, REQ-007
- Dependencies: None；用户直接授权整项实施和依赖
- Baseline state: b8f9c9a28；旧 Service/Controller 编译测试成功，DTP无用户已有改动。
- Observable outcome: 新池统一管理/Trace/统计/调容，旧API兼容，三模块测试通过。
- End state: Spec全部改造及测试/文档就绪；真实Redis/监控部署由用户自行启动验收。
- Test-first gate: Required — ThreadManage/可调队列缺失、旧Spring核心超时更新及原listener重复应用行为用回归测试固定。
- Manual Checks: MC-ARCH-001, MC-REUSE-001, MC-DEP-001, MC-NAME-001, MC-VALID-001, MC-MODEL-001, MC-CONVERT-001, MC-LOG-001, MC-BEAN-001, MC-UTIL-001, MC-JSON-001, MC-TIME-001, MC-CONFIG-001, MC-PATTERN-001, MC-SCOPE-001, MC-TEST-001, MC-BLOCKER-001
- Literal Rules: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11
- Ordered files:

#### File 1 — `CREATE egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/test/java/top/egon/cola/component/dtp/executor/ThreadManageTest.java`
- Purpose: reflection 查找 ThreadManage 和 CreateCommand；初始化后新池直接 submit 传播 Trace，统计失败与取消，队列支持运行期改容
- Symbols: ThreadManageTest 及 Spec §8/§10 对应方法/字段。
- Repository evidence: `egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/test/java/top/egon/cola/component/dtp/executor/ThreadManageTest.java` 及同包现有 executor/config/context/controller 测试风格，Spec §2/§8。
- Dependencies and consumers: 上述前序类型与现有 Registry/Service/common-trace；对应模块测试作为消费者。
- Why now: 先固定缺失行为，再按 Command/观察/注册/接入依赖连接；不引入另一套管理事实。
- Contract/signature changes: reflection 查找 ThreadManage 和 CreateCommand；初始化后新池直接 submit 传播 Trace，统计失败与取消，队列支持运行期改容；旧包、构造、void setter、Bean name、Response与枚举线值保持。
- Input/output and state mapping: 以 Spec §7.3/§9/§10 为准，名称原对象唯一；trace有效快照→任务Scope→finally恢复；queueCapacity null不变。
- Error and edge behavior: 参数非法无部分更新；同名不同对象拒绝；Future保留异常与取消；Redis失败不伪造已回滚。
- Standards impact: MC-SCOPE-001, MC-TEST-001；对应 Java采用命名Bean/Qualifier/Lombok与已有组件，非Java只验证配置及构建语义。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11；兼容优先、java.time、原包结构。
- Implementation pseudocode:
```text
ThreadManageTest: reflection 查找 ThreadManage 和 CreateCommand；初始化后新池直接 submit 传播 Trace，统计失败与取消，队列支持运行期改容
输入采用 Spec §9 的已有参数与约束，逐项测试正常/缺失/重复/失败路径，保留旧调用方式。
以 Registry 为唯一对象目录并读取实际 after 状态；执行Scope的finally恢复与有限状态清理，不创建运行进程。
```
- Verification contribution: §8 focused tests、旧契约测试与三模块 test/package。
- After this file: 本文件行为与前序类型就绪；后续接入在本Step完成，最终统一编译验证。

#### File 2 — `CREATE egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/test/java/top/egon/cola/component/dtp/executor/support/ResizableCapacityBlockingQueueTest.java`
- Purpose: put 阻塞后扩容唤醒；低于 size 缩容原子拒绝；drainTo/iterator/remove/clear/interrupt 正确
- Symbols: ResizableCapacityBlockingQueueTest 及 Spec §8/§10 对应方法/字段。
- Repository evidence: `egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/test/java/top/egon/cola/component/dtp/executor/support/ResizableCapacityBlockingQueueTest.java` 及同包现有 executor/config/context/controller 测试风格，Spec §2/§8。
- Dependencies and consumers: 上述前序类型与现有 Registry/Service/common-trace；对应模块测试作为消费者。
- Why now: 先固定缺失行为，再按 Command/观察/注册/接入依赖连接；不引入另一套管理事实。
- Contract/signature changes: put 阻塞后扩容唤醒；低于 size 缩容原子拒绝；drainTo/iterator/remove/clear/interrupt 正确；旧包、构造、void setter、Bean name、Response与枚举线值保持。
- Input/output and state mapping: 以 Spec §7.3/§9/§10 为准，名称原对象唯一；trace有效快照→任务Scope→finally恢复；queueCapacity null不变。
- Error and edge behavior: 参数非法无部分更新；同名不同对象拒绝；Future保留异常与取消；Redis失败不伪造已回滚。
- Standards impact: MC-SCOPE-001, MC-TEST-001；对应 Java采用命名Bean/Qualifier/Lombok与已有组件，非Java只验证配置及构建语义。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11；兼容优先、java.time、原包结构。
- Implementation pseudocode:
```text
ResizableCapacityBlockingQueueTest: put 阻塞后扩容唤醒；低于 size 缩容原子拒绝；drainTo/iterator/remove/clear/interrupt 正确
输入采用 Spec §9 的已有参数与约束，逐项测试正常/缺失/重复/失败路径，保留旧调用方式。
以 Registry 为唯一对象目录并读取实际 after 状态；执行Scope的finally恢复与有限状态清理，不创建运行进程。
```
- Verification contribution: §8 focused tests、旧契约测试与三模块 test/package。
- After this file: 本文件行为与前序类型就绪；后续接入在本Step完成，最终统一编译验证。

#### File 3 — `CREATE egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/test/java/top/egon/cola/component/dtp/context/DtpTaskObserverTest.java`
- Purpose: submit/invokeAll/invokeAny 的失败取消一次计数；CallerRuns 恢复 MDC；旧预包装保持原 Trace
- Symbols: DtpTaskObserverTest 及 Spec §8/§10 对应方法/字段。
- Repository evidence: `egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/test/java/top/egon/cola/component/dtp/context/DtpTaskObserverTest.java` 及同包现有 executor/config/context/controller 测试风格，Spec §2/§8。
- Dependencies and consumers: 上述前序类型与现有 Registry/Service/common-trace；对应模块测试作为消费者。
- Why now: 先固定缺失行为，再按 Command/观察/注册/接入依赖连接；不引入另一套管理事实。
- Contract/signature changes: submit/invokeAll/invokeAny 的失败取消一次计数；CallerRuns 恢复 MDC；旧预包装保持原 Trace；旧包、构造、void setter、Bean name、Response与枚举线值保持。
- Input/output and state mapping: 以 Spec §7.3/§9/§10 为准，名称原对象唯一；trace有效快照→任务Scope→finally恢复；queueCapacity null不变。
- Error and edge behavior: 参数非法无部分更新；同名不同对象拒绝；Future保留异常与取消；Redis失败不伪造已回滚。
- Standards impact: MC-SCOPE-001, MC-TEST-001；对应 Java采用命名Bean/Qualifier/Lombok与已有组件，非Java只验证配置及构建语义。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11；兼容优先、java.time、原包结构。
- Implementation pseudocode:
```text
DtpTaskObserverTest: submit/invokeAll/invokeAny 的失败取消一次计数；CallerRuns 恢复 MDC；旧预包装保持原 Trace
输入采用 Spec §9 的已有参数与约束，逐项测试正常/缺失/重复/失败路径，保留旧调用方式。
以 Registry 为唯一对象目录并读取实际 after 状态；执行Scope的finally恢复与有限状态清理，不创建运行进程。
```
- Verification contribution: §8 focused tests、旧契约测试与三模块 test/package。
- After this file: 本文件行为与前序类型就绪；后续接入在本Step完成，最终统一编译验证。

#### File 4 — `MODIFY egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/pom.xml`
- Purpose: 增加已批准 Validation starter；保持 common-trace core 与可选 micrometer 边界
- Symbols: pom 及 Spec §8/§10 对应方法/字段。
- Repository evidence: `egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/pom.xml` 及同包现有 executor/config/context/controller 测试风格，Spec §2/§8。
- Dependencies and consumers: 上述前序类型与现有 Registry/Service/common-trace；对应模块测试作为消费者。
- Why now: 先固定缺失行为，再按 Command/观察/注册/接入依赖连接；不引入另一套管理事实。
- Contract/signature changes: 增加已批准 Validation starter；保持 common-trace core 与可选 micrometer 边界；旧包、构造、void setter、Bean name、Response与枚举线值保持。
- Input/output and state mapping: 以 Spec §7.3/§9/§10 为准，名称原对象唯一；trace有效快照→任务Scope→finally恢复；queueCapacity null不变。
- Error and edge behavior: 参数非法无部分更新；同名不同对象拒绝；Future保留异常与取消；Redis失败不伪造已回滚。
- Standards impact: MC-SCOPE-001, MC-TEST-001；对应 Java采用命名Bean/Qualifier/Lombok与已有组件，非Java只验证配置及构建语义。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11；兼容优先、java.time、原包结构。
- Implementation pseudocode:
```text
pom: 增加已批准 Validation starter；保持 common-trace core 与可选 micrometer 边界
输入采用 Spec §9 的已有参数与约束，逐项测试正常/缺失/重复/失败路径，保留旧调用方式。
以 Registry 为唯一对象目录并读取实际 after 状态；执行Scope的finally恢复与有限状态清理，不创建运行进程。
```
- Verification contribution: §8 focused tests、旧契约测试与三模块 test/package。
- After this file: 本文件行为与前序类型就绪；后续接入在本Step完成，最终统一编译验证。

#### File 5 — `CREATE egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/main/java/top/egon/cola/component/dtp/domain/model/entity/ThreadPoolCreateCommand.java`
- Purpose: 显式 executorName/core/max/queue，Duration keepAlive 默认60s，原生 factory/handler；class Lombok 基线与 Create 约束
- Symbols: ThreadPoolCreateCommand 及 Spec §8/§10 对应方法/字段。
- Repository evidence: `egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/main/java/top/egon/cola/component/dtp/domain/model/entity/ThreadPoolCreateCommand.java` 及同包现有 executor/config/context/controller 测试风格，Spec §2/§8。
- Dependencies and consumers: 上述前序类型与现有 Registry/Service/common-trace；对应模块测试作为消费者。
- Why now: 先固定缺失行为，再按 Command/观察/注册/接入依赖连接；不引入另一套管理事实。
- Contract/signature changes: 显式 executorName/core/max/queue，Duration keepAlive 默认60s，原生 factory/handler；class Lombok 基线与 Create 约束；旧包、构造、void setter、Bean name、Response与枚举线值保持。
- Input/output and state mapping: 以 Spec §7.3/§9/§10 为准，名称原对象唯一；trace有效快照→任务Scope→finally恢复；queueCapacity null不变。
- Error and edge behavior: 参数非法无部分更新；同名不同对象拒绝；Future保留异常与取消；Redis失败不伪造已回滚。
- Standards impact: MC-SCOPE-001, MC-TEST-001；对应 Java采用命名Bean/Qualifier/Lombok与已有组件，非Java只验证配置及构建语义。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11；兼容优先、java.time、原包结构。
- Implementation pseudocode:
```text
ThreadPoolCreateCommand: 显式 executorName/core/max/queue，Duration keepAlive 默认60s，原生 factory/handler；class Lombok 基线与 Create 约束
输入采用 Spec §9 的已有参数与约束，逐项测试正常/缺失/重复/失败路径，保留旧调用方式。
以 Registry 为唯一对象目录并读取实际 after 状态；执行Scope的finally恢复与有限状态清理，不创建运行进程。
```
- Verification contribution: §8 focused tests、旧契约测试与三模块 test/package。
- After this file: 本文件行为与前序类型就绪；后续接入在本Step完成，最终统一编译验证。

#### File 6 — `MODIFY egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/main/java/top/egon/cola/component/dtp/domain/model/entity/ExecutorUpdateCommand.java`
- Purpose: 新增 nullable queueCapacity，保留所有旧字段及 void setter；分组原生字段与交叉约束
- Symbols: ExecutorUpdateCommand 及 Spec §8/§10 对应方法/字段。
- Repository evidence: `egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/main/java/top/egon/cola/component/dtp/domain/model/entity/ExecutorUpdateCommand.java` 及同包现有 executor/config/context/controller 测试风格，Spec §2/§8。
- Dependencies and consumers: 上述前序类型与现有 Registry/Service/common-trace；对应模块测试作为消费者。
- Why now: 先固定缺失行为，再按 Command/观察/注册/接入依赖连接；不引入另一套管理事实。
- Contract/signature changes: 新增 nullable queueCapacity，保留所有旧字段及 void setter；分组原生字段与交叉约束；旧包、构造、void setter、Bean name、Response与枚举线值保持。
- Input/output and state mapping: 以 Spec §7.3/§9/§10 为准，名称原对象唯一；trace有效快照→任务Scope→finally恢复；queueCapacity null不变。
- Error and edge behavior: 参数非法无部分更新；同名不同对象拒绝；Future保留异常与取消；Redis失败不伪造已回滚。
- Standards impact: MC-SCOPE-001, MC-TEST-001；对应 Java采用命名Bean/Qualifier/Lombok与已有组件，非Java只验证配置及构建语义。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11；兼容优先、java.time、原包结构。
- Implementation pseudocode:
```text
ExecutorUpdateCommand: 新增 nullable queueCapacity，保留所有旧字段及 void setter；分组原生字段与交叉约束
输入采用 Spec §9 的已有参数与约束，逐项测试正常/缺失/重复/失败路径，保留旧调用方式。
以 Registry 为唯一对象目录并读取实际 after 状态；执行Scope的finally恢复与有限状态清理，不创建运行进程。
```
- Verification contribution: §8 focused tests、旧契约测试与三模块 test/package。
- After this file: 本文件行为与前序类型就绪；后续接入在本Step完成，最终统一编译验证。

#### File 7 — `CREATE egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/main/java/top/egon/cola/component/dtp/executor/support/ResizableCapacityBlockingQueue.java`
- Purpose: 单 ReentrantLock 与 notEmpty/notFull；容量验证与调参回调在锁内；FIFO/中断/drainTo/identity iterator
- Symbols: ResizableCapacityBlockingQueue 及 Spec §8/§10 对应方法/字段。
- Repository evidence: `egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/main/java/top/egon/cola/component/dtp/executor/support/ResizableCapacityBlockingQueue.java` 及同包现有 executor/config/context/controller 测试风格，Spec §2/§8。
- Dependencies and consumers: 上述前序类型与现有 Registry/Service/common-trace；对应模块测试作为消费者。
- Why now: 先固定缺失行为，再按 Command/观察/注册/接入依赖连接；不引入另一套管理事实。
- Contract/signature changes: 单 ReentrantLock 与 notEmpty/notFull；容量验证与调参回调在锁内；FIFO/中断/drainTo/identity iterator；旧包、构造、void setter、Bean name、Response与枚举线值保持。
- Input/output and state mapping: 以 Spec §7.3/§9/§10 为准，名称原对象唯一；trace有效快照→任务Scope→finally恢复；queueCapacity null不变。
- Error and edge behavior: 参数非法无部分更新；同名不同对象拒绝；Future保留异常与取消；Redis失败不伪造已回滚。
- Standards impact: MC-SCOPE-001, MC-TEST-001；对应 Java采用命名Bean/Qualifier/Lombok与已有组件，非Java只验证配置及构建语义。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11；兼容优先、java.time、原包结构。
- Implementation pseudocode:
```text
ResizableCapacityBlockingQueue: 单 ReentrantLock 与 notEmpty/notFull；容量验证与调参回调在锁内；FIFO/中断/drainTo/identity iterator
输入采用 Spec §9 的已有参数与约束，逐项测试正常/缺失/重复/失败路径，保留旧调用方式。
以 Registry 为唯一对象目录并读取实际 after 状态；执行Scope的finally恢复与有限状态清理，不创建运行进程。
```
- Verification contribution: §8 focused tests、旧契约测试与三模块 test/package。
- After this file: 本文件行为与前序类型就绪；后续接入在本Step完成，最终统一编译验证。

#### File 8 — `CREATE egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/main/java/top/egon/cola/component/dtp/context/DtpTaskObserver.java`
- Purpose: 每池低基数统计、token 去重、TraceScope，计时回调隔离；日志限频；保留预包装快照
- Symbols: DtpTaskObserver 及 Spec §8/§10 对应方法/字段。
- Repository evidence: `egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/main/java/top/egon/cola/component/dtp/context/DtpTaskObserver.java` 及同包现有 executor/config/context/controller 测试风格，Spec §2/§8。
- Dependencies and consumers: 上述前序类型与现有 Registry/Service/common-trace；对应模块测试作为消费者。
- Why now: 先固定缺失行为，再按 Command/观察/注册/接入依赖连接；不引入另一套管理事实。
- Contract/signature changes: 每池低基数统计、token 去重、TraceScope，计时回调隔离；日志限频；保留预包装快照；旧包、构造、void setter、Bean name、Response与枚举线值保持。
- Input/output and state mapping: 以 Spec §7.3/§9/§10 为准，名称原对象唯一；trace有效快照→任务Scope→finally恢复；queueCapacity null不变。
- Error and edge behavior: 参数非法无部分更新；同名不同对象拒绝；Future保留异常与取消；Redis失败不伪造已回滚。
- Standards impact: MC-SCOPE-001, MC-TEST-001；对应 Java采用命名Bean/Qualifier/Lombok与已有组件，非Java只验证配置及构建语义。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11；兼容优先、java.time、原包结构。
- Implementation pseudocode:
```text
DtpTaskObserver: 每池低基数统计、token 去重、TraceScope，计时回调隔离；日志限频；保留预包装快照
输入采用 Spec §9 的已有参数与约束，逐项测试正常/缺失/重复/失败路径，保留旧调用方式。
以 Registry 为唯一对象目录并读取实际 after 状态；执行Scope的finally恢复与有限状态清理，不创建运行进程。
```
- Verification contribution: §8 focused tests、旧契约测试与三模块 test/package。
- After this file: 本文件行为与前序类型就绪；后续接入在本Step完成，最终统一编译验证。

#### File 9 — `CREATE egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/main/java/top/egon/cola/component/dtp/context/DtpObservedFutureTask.java`
- Purpose: 任务执行边界统计真实终态；done 观察取消；计数 CAS，异常不被外壳误判
- Symbols: DtpObservedFutureTask 及 Spec §8/§10 对应方法/字段。
- Repository evidence: `egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/main/java/top/egon/cola/component/dtp/context/DtpObservedFutureTask.java` 及同包现有 executor/config/context/controller 测试风格，Spec §2/§8。
- Dependencies and consumers: 上述前序类型与现有 Registry/Service/common-trace；对应模块测试作为消费者。
- Why now: 先固定缺失行为，再按 Command/观察/注册/接入依赖连接；不引入另一套管理事实。
- Contract/signature changes: 任务执行边界统计真实终态；done 观察取消；计数 CAS，异常不被外壳误判；旧包、构造、void setter、Bean name、Response与枚举线值保持。
- Input/output and state mapping: 以 Spec §7.3/§9/§10 为准，名称原对象唯一；trace有效快照→任务Scope→finally恢复；queueCapacity null不变。
- Error and edge behavior: 参数非法无部分更新；同名不同对象拒绝；Future保留异常与取消；Redis失败不伪造已回滚。
- Standards impact: MC-SCOPE-001, MC-TEST-001；对应 Java采用命名Bean/Qualifier/Lombok与已有组件，非Java只验证配置及构建语义。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11；兼容优先、java.time、原包结构。
- Implementation pseudocode:
```text
DtpObservedFutureTask: 任务执行边界统计真实终态；done 观察取消；计数 CAS，异常不被外壳误判
输入采用 Spec §9 的已有参数与约束，逐项测试正常/缺失/重复/失败路径，保留旧调用方式。
以 Registry 为唯一对象目录并读取实际 after 状态；执行Scope的finally恢复与有限状态清理，不创建运行进程。
```
- Verification contribution: §8 focused tests、旧契约测试与三模块 test/package。
- After this file: 本文件行为与前序类型就绪；后续接入在本Step完成，最终统一编译验证。

#### File 10 — `CREATE egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/main/java/top/egon/cola/component/dtp/executor/managed/DtpThreadPoolTaskExecutor.java`
- Purpose: 幂等 initialize；可调队列；execute/submit/bulk 观察；拒绝处理原样委托；新后台任务根 Trace
- Symbols: DtpThreadPoolTaskExecutor 及 Spec §8/§10 对应方法/字段。
- Repository evidence: `egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/main/java/top/egon/cola/component/dtp/executor/managed/DtpThreadPoolTaskExecutor.java` 及同包现有 executor/config/context/controller 测试风格，Spec §2/§8。
- Dependencies and consumers: 上述前序类型与现有 Registry/Service/common-trace；对应模块测试作为消费者。
- Why now: 先固定缺失行为，再按 Command/观察/注册/接入依赖连接；不引入另一套管理事实。
- Contract/signature changes: 幂等 initialize；可调队列；execute/submit/bulk 观察；拒绝处理原样委托；新后台任务根 Trace；旧包、构造、void setter、Bean name、Response与枚举线值保持。
- Input/output and state mapping: 以 Spec §7.3/§9/§10 为准，名称原对象唯一；trace有效快照→任务Scope→finally恢复；queueCapacity null不变。
- Error and edge behavior: 参数非法无部分更新；同名不同对象拒绝；Future保留异常与取消；Redis失败不伪造已回滚。
- Standards impact: MC-SCOPE-001, MC-TEST-001；对应 Java采用命名Bean/Qualifier/Lombok与已有组件，非Java只验证配置及构建语义。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11；兼容优先、java.time、原包结构。
- Implementation pseudocode:
```text
DtpThreadPoolTaskExecutor: 幂等 initialize；可调队列；execute/submit/bulk 观察；拒绝处理原样委托；新后台任务根 Trace
输入采用 Spec §9 的已有参数与约束，逐项测试正常/缺失/重复/失败路径，保留旧调用方式。
以 Registry 为唯一对象目录并读取实际 after 状态；执行Scope的finally恢复与有限状态清理，不创建运行进程。
```
- Verification contribution: §8 focused tests、旧契约测试与三模块 test/package。
- After this file: 本文件行为与前序类型就绪；后续接入在本Step完成，最终统一编译验证。

#### File 11 — `MODIFY egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/main/java/top/egon/cola/component/dtp/executor/ManagedExecutor.java`
- Purpose: 增加 default 能力/observer 方法，现有实现兼容
- Symbols: ManagedExecutor 及 Spec §8/§10 对应方法/字段。
- Repository evidence: `egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/main/java/top/egon/cola/component/dtp/executor/ManagedExecutor.java` 及同包现有 executor/config/context/controller 测试风格，Spec §2/§8。
- Dependencies and consumers: 上述前序类型与现有 Registry/Service/common-trace；对应模块测试作为消费者。
- Why now: 先固定缺失行为，再按 Command/观察/注册/接入依赖连接；不引入另一套管理事实。
- Contract/signature changes: 增加 default 能力/observer 方法，现有实现兼容；旧包、构造、void setter、Bean name、Response与枚举线值保持。
- Input/output and state mapping: 以 Spec §7.3/§9/§10 为准，名称原对象唯一；trace有效快照→任务Scope→finally恢复；queueCapacity null不变。
- Error and edge behavior: 参数非法无部分更新；同名不同对象拒绝；Future保留异常与取消；Redis失败不伪造已回滚。
- Standards impact: MC-SCOPE-001, MC-TEST-001；对应 Java采用命名Bean/Qualifier/Lombok与已有组件，非Java只验证配置及构建语义。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11；兼容优先、java.time、原包结构。
- Implementation pseudocode:
```text
ManagedExecutor: 增加 default 能力/observer 方法，现有实现兼容
输入采用 Spec §9 的已有参数与约束，逐项测试正常/缺失/重复/失败路径，保留旧调用方式。
以 Registry 为唯一对象目录并读取实际 after 状态；执行Scope的finally恢复与有限状态清理，不创建运行进程。
```
- Verification contribution: §8 focused tests、旧契约测试与三模块 test/package。
- After this file: 本文件行为与前序类型就绪；后续接入在本Step完成，最终统一编译验证。

#### File 12 — `MODIFY egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/main/java/top/egon/cola/component/dtp/executor/ManagedExecutorRegistry.java`
- Purpose: 唯一同步注册表；原 get/list/constructor 保留；原对象/创建定义/归属原子登记，增量指标通知
- Symbols: ManagedExecutorRegistry 及 Spec §8/§10 对应方法/字段。
- Repository evidence: `egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/main/java/top/egon/cola/component/dtp/executor/ManagedExecutorRegistry.java` 及同包现有 executor/config/context/controller 测试风格，Spec §2/§8。
- Dependencies and consumers: 上述前序类型与现有 Registry/Service/common-trace；对应模块测试作为消费者。
- Why now: 先固定缺失行为，再按 Command/观察/注册/接入依赖连接；不引入另一套管理事实。
- Contract/signature changes: 唯一同步注册表；原 get/list/constructor 保留；原对象/创建定义/归属原子登记，增量指标通知；旧包、构造、void setter、Bean name、Response与枚举线值保持。
- Input/output and state mapping: 以 Spec §7.3/§9/§10 为准，名称原对象唯一；trace有效快照→任务Scope→finally恢复；queueCapacity null不变。
- Error and edge behavior: 参数非法无部分更新；同名不同对象拒绝；Future保留异常与取消；Redis失败不伪造已回滚。
- Standards impact: MC-SCOPE-001, MC-TEST-001；对应 Java采用命名Bean/Qualifier/Lombok与已有组件，非Java只验证配置及构建语义。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11；兼容优先、java.time、原包结构。
- Implementation pseudocode:
```text
ManagedExecutorRegistry: 唯一同步注册表；原 get/list/constructor 保留；原对象/创建定义/归属原子登记，增量指标通知
输入采用 Spec §9 的已有参数与约束，逐项测试正常/缺失/重复/失败路径，保留旧调用方式。
以 Registry 为唯一对象目录并读取实际 after 状态；执行Scope的finally恢复与有限状态清理，不创建运行进程。
```
- Verification contribution: §8 focused tests、旧契约测试与三模块 test/package。
- After this file: 本文件行为与前序类型就绪；后续接入在本Step完成，最终统一编译验证。

#### File 13 — `CREATE egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/main/java/top/egon/cola/component/dtp/executor/ThreadManage.java`
- Purpose: Spring facade，newThreadPool/executor/changeThreadPool；名称冲突拒绝，资源归属、关闭门禁；不访问 Redis
- Symbols: ThreadManage 及 Spec §8/§10 对应方法/字段。
- Repository evidence: `egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/main/java/top/egon/cola/component/dtp/executor/ThreadManage.java` 及同包现有 executor/config/context/controller 测试风格，Spec §2/§8。
- Dependencies and consumers: 上述前序类型与现有 Registry/Service/common-trace；对应模块测试作为消费者。
- Why now: 先固定缺失行为，再按 Command/观察/注册/接入依赖连接；不引入另一套管理事实。
- Contract/signature changes: Spring facade，newThreadPool/executor/changeThreadPool；名称冲突拒绝，资源归属、关闭门禁；不访问 Redis；旧包、构造、void setter、Bean name、Response与枚举线值保持。
- Input/output and state mapping: 以 Spec §7.3/§9/§10 为准，名称原对象唯一；trace有效快照→任务Scope→finally恢复；queueCapacity null不变。
- Error and edge behavior: 参数非法无部分更新；同名不同对象拒绝；Future保留异常与取消；Redis失败不伪造已回滚。
- Standards impact: MC-SCOPE-001, MC-TEST-001；对应 Java采用命名Bean/Qualifier/Lombok与已有组件，非Java只验证配置及构建语义。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11；兼容优先、java.time、原包结构。
- Implementation pseudocode:
```text
ThreadManage: Spring facade，newThreadPool/executor/changeThreadPool；名称冲突拒绝，资源归属、关闭门禁；不访问 Redis
输入采用 Spec §9 的已有参数与约束，逐项测试正常/缺失/重复/失败路径，保留旧调用方式。
以 Registry 为唯一对象目录并读取实际 after 状态；执行Scope的finally恢复与有限状态清理，不创建运行进程。
```
- Verification contribution: §8 focused tests、旧契约测试与三模块 test/package。
- After this file: 本文件行为与前序类型就绪；后续接入在本Step完成，最终统一编译验证。

#### File 14 — `MODIFY egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/main/java/top/egon/cola/component/dtp/executor/support/ThreadPoolResizeSupport.java`
- Purpose: 完整目标验证、单池串行、Spring getter/底层同步；queueCapacity 受锁验证后再 setter；失败恢复真实 after
- Symbols: ThreadPoolResizeSupport 及 Spec §8/§10 对应方法/字段。
- Repository evidence: `egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/main/java/top/egon/cola/component/dtp/executor/support/ThreadPoolResizeSupport.java` 及同包现有 executor/config/context/controller 测试风格，Spec §2/§8。
- Dependencies and consumers: 上述前序类型与现有 Registry/Service/common-trace；对应模块测试作为消费者。
- Why now: 先固定缺失行为，再按 Command/观察/注册/接入依赖连接；不引入另一套管理事实。
- Contract/signature changes: 完整目标验证、单池串行、Spring getter/底层同步；queueCapacity 受锁验证后再 setter；失败恢复真实 after；旧包、构造、void setter、Bean name、Response与枚举线值保持。
- Input/output and state mapping: 以 Spec §7.3/§9/§10 为准，名称原对象唯一；trace有效快照→任务Scope→finally恢复；queueCapacity null不变。
- Error and edge behavior: 参数非法无部分更新；同名不同对象拒绝；Future保留异常与取消；Redis失败不伪造已回滚。
- Standards impact: MC-SCOPE-001, MC-TEST-001；对应 Java采用命名Bean/Qualifier/Lombok与已有组件，非Java只验证配置及构建语义。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11；兼容优先、java.time、原包结构。
- Implementation pseudocode:
```text
ThreadPoolResizeSupport: 完整目标验证、单池串行、Spring getter/底层同步；queueCapacity 受锁验证后再 setter；失败恢复真实 after
输入采用 Spec §9 的已有参数与约束，逐项测试正常/缺失/重复/失败路径，保留旧调用方式。
以 Registry 为唯一对象目录并读取实际 after 状态；执行Scope的finally恢复与有限状态清理，不创建运行进程。
```
- Verification contribution: §8 focused tests、旧契约测试与三模块 test/package。
- After this file: 本文件行为与前序类型就绪；后续接入在本Step完成，最终统一编译验证。

#### File 15 — `MODIFY egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/main/java/top/egon/cola/component/dtp/executor/adapter/ThreadPoolExecutorManagedExecutor.java`
- Purpose: 旧构造保留，支持观察与原平台指标；update 复用 ResizeSupport，队列能力识别
- Symbols: ThreadPoolExecutorManagedExecutor 及 Spec §8/§10 对应方法/字段。
- Repository evidence: `egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/main/java/top/egon/cola/component/dtp/executor/adapter/ThreadPoolExecutorManagedExecutor.java` 及同包现有 executor/config/context/controller 测试风格，Spec §2/§8。
- Dependencies and consumers: 上述前序类型与现有 Registry/Service/common-trace；对应模块测试作为消费者。
- Why now: 先固定缺失行为，再按 Command/观察/注册/接入依赖连接；不引入另一套管理事实。
- Contract/signature changes: 旧构造保留，支持观察与原平台指标；update 复用 ResizeSupport，队列能力识别；旧包、构造、void setter、Bean name、Response与枚举线值保持。
- Input/output and state mapping: 以 Spec §7.3/§9/§10 为准，名称原对象唯一；trace有效快照→任务Scope→finally恢复；queueCapacity null不变。
- Error and edge behavior: 参数非法无部分更新；同名不同对象拒绝；Future保留异常与取消；Redis失败不伪造已回滚。
- Standards impact: MC-SCOPE-001, MC-TEST-001；对应 Java采用命名Bean/Qualifier/Lombok与已有组件，非Java只验证配置及构建语义。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11；兼容优先、java.time、原包结构。
- Implementation pseudocode:
```text
ThreadPoolExecutorManagedExecutor: 旧构造保留，支持观察与原平台指标；update 复用 ResizeSupport，队列能力识别
输入采用 Spec §9 的已有参数与约束，逐项测试正常/缺失/重复/失败路径，保留旧调用方式。
以 Registry 为唯一对象目录并读取实际 after 状态；执行Scope的finally恢复与有限状态清理，不创建运行进程。
```
- Verification contribution: §8 focused tests、旧契约测试与三模块 test/package。
- After this file: 本文件行为与前序类型就绪；后续接入在本Step完成，最终统一编译验证。

#### File 16 — `MODIFY egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/main/java/top/egon/cola/component/dtp/executor/adapter/ThreadPoolTaskExecutorManagedExecutor.java`
- Purpose: 原 Spring Bean 不替换；保留 decorator；adapter submit 实际 Callable 观察；统一 resize
- Symbols: ThreadPoolTaskExecutorManagedExecutor 及 Spec §8/§10 对应方法/字段。
- Repository evidence: `egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/main/java/top/egon/cola/component/dtp/executor/adapter/ThreadPoolTaskExecutorManagedExecutor.java` 及同包现有 executor/config/context/controller 测试风格，Spec §2/§8。
- Dependencies and consumers: 上述前序类型与现有 Registry/Service/common-trace；对应模块测试作为消费者。
- Why now: 先固定缺失行为，再按 Command/观察/注册/接入依赖连接；不引入另一套管理事实。
- Contract/signature changes: 原 Spring Bean 不替换；保留 decorator；adapter submit 实际 Callable 观察；统一 resize；旧包、构造、void setter、Bean name、Response与枚举线值保持。
- Input/output and state mapping: 以 Spec §7.3/§9/§10 为准，名称原对象唯一；trace有效快照→任务Scope→finally恢复；queueCapacity null不变。
- Error and edge behavior: 参数非法无部分更新；同名不同对象拒绝；Future保留异常与取消；Redis失败不伪造已回滚。
- Standards impact: MC-SCOPE-001, MC-TEST-001；对应 Java采用命名Bean/Qualifier/Lombok与已有组件，非Java只验证配置及构建语义。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11；兼容优先、java.time、原包结构。
- Implementation pseudocode:
```text
ThreadPoolTaskExecutorManagedExecutor: 原 Spring Bean 不替换；保留 decorator；adapter submit 实际 Callable 观察；统一 resize
输入采用 Spec §9 的已有参数与约束，逐项测试正常/缺失/重复/失败路径，保留旧调用方式。
以 Registry 为唯一对象目录并读取实际 after 状态；执行Scope的finally恢复与有限状态清理，不创建运行进程。
```
- Verification contribution: §8 focused tests、旧契约测试与三模块 test/package。
- After this file: 本文件行为与前序类型就绪；后续接入在本Step完成，最终统一编译验证。

#### File 17 — `MODIFY egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/main/java/top/egon/cola/component/dtp/executor/adapter/BoundedVirtualThreadManagedExecutor.java`
- Purpose: 保留虚拟限制与原指标；新增观察接点，防重复计数，拒绝混用平台参数
- Symbols: BoundedVirtualThreadManagedExecutor 及 Spec §8/§10 对应方法/字段。
- Repository evidence: `egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/main/java/top/egon/cola/component/dtp/executor/adapter/BoundedVirtualThreadManagedExecutor.java` 及同包现有 executor/config/context/controller 测试风格，Spec §2/§8。
- Dependencies and consumers: 上述前序类型与现有 Registry/Service/common-trace；对应模块测试作为消费者。
- Why now: 先固定缺失行为，再按 Command/观察/注册/接入依赖连接；不引入另一套管理事实。
- Contract/signature changes: 保留虚拟限制与原指标；新增观察接点，防重复计数，拒绝混用平台参数；旧包、构造、void setter、Bean name、Response与枚举线值保持。
- Input/output and state mapping: 以 Spec §7.3/§9/§10 为准，名称原对象唯一；trace有效快照→任务Scope→finally恢复；queueCapacity null不变。
- Error and edge behavior: 参数非法无部分更新；同名不同对象拒绝；Future保留异常与取消；Redis失败不伪造已回滚。
- Standards impact: MC-SCOPE-001, MC-TEST-001；对应 Java采用命名Bean/Qualifier/Lombok与已有组件，非Java只验证配置及构建语义。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11；兼容优先、java.time、原包结构。
- Implementation pseudocode:
```text
BoundedVirtualThreadManagedExecutor: 保留虚拟限制与原指标；新增观察接点，防重复计数，拒绝混用平台参数
输入采用 Spec §9 的已有参数与约束，逐项测试正常/缺失/重复/失败路径，保留旧调用方式。
以 Registry 为唯一对象目录并读取实际 after 状态；执行Scope的finally恢复与有限状态清理，不创建运行进程。
```
- Verification contribution: §8 focused tests、旧契约测试与三模块 test/package。
- After this file: 本文件行为与前序类型就绪；后续接入在本Step完成，最终统一编译验证。

#### File 18 — `MODIFY egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/main/java/top/egon/cola/component/dtp/executor/virtual/BoundedVirtualThreadExecutor.java`
- Purpose: 保留原 semaphore/FutureTask 与 bulk；仅添加任务观察接点，不二次计数或改变取消
- Symbols: BoundedVirtualThreadExecutor 及 Spec §8/§10 对应方法/字段。
- Repository evidence: `egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/main/java/top/egon/cola/component/dtp/executor/virtual/BoundedVirtualThreadExecutor.java` 及同包现有 executor/config/context/controller 测试风格，Spec §2/§8。
- Dependencies and consumers: 上述前序类型与现有 Registry/Service/common-trace；对应模块测试作为消费者。
- Why now: 先固定缺失行为，再按 Command/观察/注册/接入依赖连接；不引入另一套管理事实。
- Contract/signature changes: 保留原 semaphore/FutureTask 与 bulk；仅添加任务观察接点，不二次计数或改变取消；旧包、构造、void setter、Bean name、Response与枚举线值保持。
- Input/output and state mapping: 以 Spec §7.3/§9/§10 为准，名称原对象唯一；trace有效快照→任务Scope→finally恢复；queueCapacity null不变。
- Error and edge behavior: 参数非法无部分更新；同名不同对象拒绝；Future保留异常与取消；Redis失败不伪造已回滚。
- Standards impact: MC-SCOPE-001, MC-TEST-001；对应 Java采用命名Bean/Qualifier/Lombok与已有组件，非Java只验证配置及构建语义。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11；兼容优先、java.time、原包结构。
- Implementation pseudocode:
```text
BoundedVirtualThreadExecutor: 保留原 semaphore/FutureTask 与 bulk；仅添加任务观察接点，不二次计数或改变取消
输入采用 Spec §9 的已有参数与约束，逐项测试正常/缺失/重复/失败路径，保留旧调用方式。
以 Registry 为唯一对象目录并读取实际 after 状态；执行Scope的finally恢复与有限状态清理，不创建运行进程。
```
- Verification contribution: §8 focused tests、旧契约测试与三模块 test/package。
- After this file: 本文件行为与前序类型就绪；后续接入在本Step完成，最终统一编译验证。

#### File 19 — `MODIFY egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/main/java/top/egon/cola/component/dtp/context/DtpContextAwareExecutorService.java`
- Purpose: 旧构造/API 保留，受控观察 submit/bulk 的真实委托，原未来异常取消语义不变
- Symbols: DtpContextAwareExecutorService 及 Spec §8/§10 对应方法/字段。
- Repository evidence: `egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/main/java/top/egon/cola/component/dtp/context/DtpContextAwareExecutorService.java` 及同包现有 executor/config/context/controller 测试风格，Spec §2/§8。
- Dependencies and consumers: 上述前序类型与现有 Registry/Service/common-trace；对应模块测试作为消费者。
- Why now: 先固定缺失行为，再按 Command/观察/注册/接入依赖连接；不引入另一套管理事实。
- Contract/signature changes: 旧构造/API 保留，受控观察 submit/bulk 的真实委托，原未来异常取消语义不变；旧包、构造、void setter、Bean name、Response与枚举线值保持。
- Input/output and state mapping: 以 Spec §7.3/§9/§10 为准，名称原对象唯一；trace有效快照→任务Scope→finally恢复；queueCapacity null不变。
- Error and edge behavior: 参数非法无部分更新；同名不同对象拒绝；Future保留异常与取消；Redis失败不伪造已回滚。
- Standards impact: MC-SCOPE-001, MC-TEST-001；对应 Java采用命名Bean/Qualifier/Lombok与已有组件，非Java只验证配置及构建语义。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11；兼容优先、java.time、原包结构。
- Implementation pseudocode:
```text
DtpContextAwareExecutorService: 旧构造/API 保留，受控观察 submit/bulk 的真实委托，原未来异常取消语义不变
输入采用 Spec §9 的已有参数与约束，逐项测试正常/缺失/重复/失败路径，保留旧调用方式。
以 Registry 为唯一对象目录并读取实际 after 状态；执行Scope的finally恢复与有限状态清理，不创建运行进程。
```
- Verification contribution: §8 focused tests、旧契约测试与三模块 test/package。
- After this file: 本文件行为与前序类型就绪；后续接入在本Step完成，最终统一编译验证。

#### File 20 — `MODIFY egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/main/java/top/egon/cola/component/dtp/context/DtpRunnable.java`
- Purpose: 保留 wrap(final模板)；可读取组件内部捕获 Trace；不覆盖预包装时点
- Symbols: DtpRunnable 及 Spec §8/§10 对应方法/字段。
- Repository evidence: `egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/main/java/top/egon/cola/component/dtp/context/DtpRunnable.java` 及同包现有 executor/config/context/controller 测试风格，Spec §2/§8。
- Dependencies and consumers: 上述前序类型与现有 Registry/Service/common-trace；对应模块测试作为消费者。
- Why now: 先固定缺失行为，再按 Command/观察/注册/接入依赖连接；不引入另一套管理事实。
- Contract/signature changes: 保留 wrap(final模板)；可读取组件内部捕获 Trace；不覆盖预包装时点；旧包、构造、void setter、Bean name、Response与枚举线值保持。
- Input/output and state mapping: 以 Spec §7.3/§9/§10 为准，名称原对象唯一；trace有效快照→任务Scope→finally恢复；queueCapacity null不变。
- Error and edge behavior: 参数非法无部分更新；同名不同对象拒绝；Future保留异常与取消；Redis失败不伪造已回滚。
- Standards impact: MC-SCOPE-001, MC-TEST-001；对应 Java采用命名Bean/Qualifier/Lombok与已有组件，非Java只验证配置及构建语义。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11；兼容优先、java.time、原包结构。
- Implementation pseudocode:
```text
DtpRunnable: 保留 wrap(final模板)；可读取组件内部捕获 Trace；不覆盖预包装时点
输入采用 Spec §9 的已有参数与约束，逐项测试正常/缺失/重复/失败路径，保留旧调用方式。
以 Registry 为唯一对象目录并读取实际 after 状态；执行Scope的finally恢复与有限状态清理，不创建运行进程。
```
- Verification contribution: §8 focused tests、旧契约测试与三模块 test/package。
- After this file: 本文件行为与前序类型就绪；后续接入在本Step完成，最终统一编译验证。

#### File 21 — `MODIFY egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/main/java/top/egon/cola/component/dtp/context/DtpCallable.java`
- Purpose: 保留 wrap/TraceRouteCallable；在真实 call 委托边界观察，上下文快照复用
- Symbols: DtpCallable 及 Spec §8/§10 对应方法/字段。
- Repository evidence: `egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/main/java/top/egon/cola/component/dtp/context/DtpCallable.java` 及同包现有 executor/config/context/controller 测试风格，Spec §2/§8。
- Dependencies and consumers: 上述前序类型与现有 Registry/Service/common-trace；对应模块测试作为消费者。
- Why now: 先固定缺失行为，再按 Command/观察/注册/接入依赖连接；不引入另一套管理事实。
- Contract/signature changes: 保留 wrap/TraceRouteCallable；在真实 call 委托边界观察，上下文快照复用；旧包、构造、void setter、Bean name、Response与枚举线值保持。
- Input/output and state mapping: 以 Spec §7.3/§9/§10 为准，名称原对象唯一；trace有效快照→任务Scope→finally恢复；queueCapacity null不变。
- Error and edge behavior: 参数非法无部分更新；同名不同对象拒绝；Future保留异常与取消；Redis失败不伪造已回滚。
- Standards impact: MC-SCOPE-001, MC-TEST-001；对应 Java采用命名Bean/Qualifier/Lombok与已有组件，非Java只验证配置及构建语义。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11；兼容优先、java.time、原包结构。
- Implementation pseudocode:
```text
DtpCallable: 保留 wrap/TraceRouteCallable；在真实 call 委托边界观察，上下文快照复用
输入采用 Spec §9 的已有参数与约束，逐项测试正常/缺失/重复/失败路径，保留旧调用方式。
以 Registry 为唯一对象目录并读取实际 after 状态；执行Scope的finally恢复与有限状态清理，不创建运行进程。
```
- Verification contribution: §8 focused tests、旧契约测试与三模块 test/package。
- After this file: 本文件行为与前序类型就绪；后续接入在本Step完成，最终统一编译验证。

#### File 22 — `MODIFY egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/main/java/top/egon/cola/component/dtp/context/DtpSupplier.java`
- Purpose: 保留 wrap/TraceRouteSupplier；提供有效 Trace 观察，不改变旧 Supplier 契约
- Symbols: DtpSupplier 及 Spec §8/§10 对应方法/字段。
- Repository evidence: `egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/main/java/top/egon/cola/component/dtp/context/DtpSupplier.java` 及同包现有 executor/config/context/controller 测试风格，Spec §2/§8。
- Dependencies and consumers: 上述前序类型与现有 Registry/Service/common-trace；对应模块测试作为消费者。
- Why now: 先固定缺失行为，再按 Command/观察/注册/接入依赖连接；不引入另一套管理事实。
- Contract/signature changes: 保留 wrap/TraceRouteSupplier；提供有效 Trace 观察，不改变旧 Supplier 契约；旧包、构造、void setter、Bean name、Response与枚举线值保持。
- Input/output and state mapping: 以 Spec §7.3/§9/§10 为准，名称原对象唯一；trace有效快照→任务Scope→finally恢复；queueCapacity null不变。
- Error and edge behavior: 参数非法无部分更新；同名不同对象拒绝；Future保留异常与取消；Redis失败不伪造已回滚。
- Standards impact: MC-SCOPE-001, MC-TEST-001；对应 Java采用命名Bean/Qualifier/Lombok与已有组件，非Java只验证配置及构建语义。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11；兼容优先、java.time、原包结构。
- Implementation pseudocode:
```text
DtpSupplier: 保留 wrap/TraceRouteSupplier；提供有效 Trace 观察，不改变旧 Supplier 契约
输入采用 Spec §9 的已有参数与约束，逐项测试正常/缺失/重复/失败路径，保留旧调用方式。
以 Registry 为唯一对象目录并读取实际 after 状态；执行Scope的finally恢复与有限状态清理，不创建运行进程。
```
- Verification contribution: §8 focused tests、旧契约测试与三模块 test/package。
- After this file: 本文件行为与前序类型就绪；后续接入在本Step完成，最终统一编译验证。

#### File 23 — `MODIFY egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/main/java/top/egon/cola/component/dtp/context/DtpTaskDecorator.java`
- Purpose: 复用旧 wrap；与新受控任务 token 协同，防止 MDC 与统计双重装饰
- Symbols: DtpTaskDecorator 及 Spec §8/§10 对应方法/字段。
- Repository evidence: `egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/main/java/top/egon/cola/component/dtp/context/DtpTaskDecorator.java` 及同包现有 executor/config/context/controller 测试风格，Spec §2/§8。
- Dependencies and consumers: 上述前序类型与现有 Registry/Service/common-trace；对应模块测试作为消费者。
- Why now: 先固定缺失行为，再按 Command/观察/注册/接入依赖连接；不引入另一套管理事实。
- Contract/signature changes: 复用旧 wrap；与新受控任务 token 协同，防止 MDC 与统计双重装饰；旧包、构造、void setter、Bean name、Response与枚举线值保持。
- Input/output and state mapping: 以 Spec §7.3/§9/§10 为准，名称原对象唯一；trace有效快照→任务Scope→finally恢复；queueCapacity null不变。
- Error and edge behavior: 参数非法无部分更新；同名不同对象拒绝；Future保留异常与取消；Redis失败不伪造已回滚。
- Standards impact: MC-SCOPE-001, MC-TEST-001；对应 Java采用命名Bean/Qualifier/Lombok与已有组件，非Java只验证配置及构建语义。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11；兼容优先、java.time、原包结构。
- Implementation pseudocode:
```text
DtpTaskDecorator: 复用旧 wrap；与新受控任务 token 协同，防止 MDC 与统计双重装饰
输入采用 Spec §9 的已有参数与约束，逐项测试正常/缺失/重复/失败路径，保留旧调用方式。
以 Registry 为唯一对象目录并读取实际 after 状态；执行Scope的finally恢复与有限状态清理，不创建运行进程。
```
- Verification contribution: §8 focused tests、旧契约测试与三模块 test/package。
- After this file: 本文件行为与前序类型就绪；后续接入在本Step完成，最终统一编译验证。

#### File 24 — `MODIFY egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/main/java/top/egon/cola/component/dtp/domain/DynamicThreadPoolService.java`
- Purpose: 原身份校验和失败结果保留；逐池隔离快照失败；更新使用统一锁/校验入口
- Symbols: DynamicThreadPoolService 及 Spec §8/§10 对应方法/字段。
- Repository evidence: `egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/main/java/top/egon/cola/component/dtp/domain/DynamicThreadPoolService.java` 及同包现有 executor/config/context/controller 测试风格，Spec §2/§8。
- Dependencies and consumers: 上述前序类型与现有 Registry/Service/common-trace；对应模块测试作为消费者。
- Why now: 先固定缺失行为，再按 Command/观察/注册/接入依赖连接；不引入另一套管理事实。
- Contract/signature changes: 原身份校验和失败结果保留；逐池隔离快照失败；更新使用统一锁/校验入口；旧包、构造、void setter、Bean name、Response与枚举线值保持。
- Input/output and state mapping: 以 Spec §7.3/§9/§10 为准，名称原对象唯一；trace有效快照→任务Scope→finally恢复；queueCapacity null不变。
- Error and edge behavior: 参数非法无部分更新；同名不同对象拒绝；Future保留异常与取消；Redis失败不伪造已回滚。
- Standards impact: MC-SCOPE-001, MC-TEST-001；对应 Java采用命名Bean/Qualifier/Lombok与已有组件，非Java只验证配置及构建语义。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11；兼容优先、java.time、原包结构。
- Implementation pseudocode:
```text
DynamicThreadPoolService: 原身份校验和失败结果保留；逐池隔离快照失败；更新使用统一锁/校验入口
输入采用 Spec §9 的已有参数与约束，逐项测试正常/缺失/重复/失败路径，保留旧调用方式。
以 Registry 为唯一对象目录并读取实际 after 状态；执行Scope的finally恢复与有限状态清理，不创建运行进程。
```
- Verification contribution: §8 focused tests、旧契约测试与三模块 test/package。
- After this file: 本文件行为与前序类型就绪；后续接入在本Step完成，最终统一编译验证。

#### File 25 — `CREATE egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/main/java/top/egon/cola/component/dtp/config/DtpExecutorBeanPostProcessor.java`
- Purpose: static Bean 工厂、延迟 provider，不 eager 注入所有池；初始化后登记原对象不代理
- Symbols: DtpExecutorBeanPostProcessor 及 Spec §8/§10 对应方法/字段。
- Repository evidence: `egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/main/java/top/egon/cola/component/dtp/config/DtpExecutorBeanPostProcessor.java` 及同包现有 executor/config/context/controller 测试风格，Spec §2/§8。
- Dependencies and consumers: 上述前序类型与现有 Registry/Service/common-trace；对应模块测试作为消费者。
- Why now: 先固定缺失行为，再按 Command/观察/注册/接入依赖连接；不引入另一套管理事实。
- Contract/signature changes: static Bean 工厂、延迟 provider，不 eager 注入所有池；初始化后登记原对象不代理；旧包、构造、void setter、Bean name、Response与枚举线值保持。
- Input/output and state mapping: 以 Spec §7.3/§9/§10 为准，名称原对象唯一；trace有效快照→任务Scope→finally恢复；queueCapacity null不变。
- Error and edge behavior: 参数非法无部分更新；同名不同对象拒绝；Future保留异常与取消；Redis失败不伪造已回滚。
- Standards impact: MC-SCOPE-001, MC-TEST-001；对应 Java采用命名Bean/Qualifier/Lombok与已有组件，非Java只验证配置及构建语义。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11；兼容优先、java.time、原包结构。
- Implementation pseudocode:
```text
DtpExecutorBeanPostProcessor: static Bean 工厂、延迟 provider，不 eager 注入所有池；初始化后登记原对象不代理
输入采用 Spec §9 的已有参数与约束，逐项测试正常/缺失/重复/失败路径，保留旧调用方式。
以 Registry 为唯一对象目录并读取实际 after 状态；执行Scope的finally恢复与有限状态清理，不创建运行进程。
```
- Verification contribution: §8 focused tests、旧契约测试与三模块 test/package。
- After this file: 本文件行为与前序类型就绪；后续接入在本Step完成，最终统一编译验证。

#### File 26 — `MODIFY egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/main/java/top/egon/cola/component/dtp/config/DynamicThreadPoolAutoConfig.java`
- Purpose: 空 Registry 与 threadManage/observer/validator；BPP 静态注册，原服务 Bean 名保留；Redis 与可选指标边界保留
- Symbols: DynamicThreadPoolAutoConfig 及 Spec §8/§10 对应方法/字段。
- Repository evidence: `egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/main/java/top/egon/cola/component/dtp/config/DynamicThreadPoolAutoConfig.java` 及同包现有 executor/config/context/controller 测试风格，Spec §2/§8。
- Dependencies and consumers: 上述前序类型与现有 Registry/Service/common-trace；对应模块测试作为消费者。
- Why now: 先固定缺失行为，再按 Command/观察/注册/接入依赖连接；不引入另一套管理事实。
- Contract/signature changes: 空 Registry 与 threadManage/observer/validator；BPP 静态注册，原服务 Bean 名保留；Redis 与可选指标边界保留；旧包、构造、void setter、Bean name、Response与枚举线值保持。
- Input/output and state mapping: 以 Spec §7.3/§9/§10 为准，名称原对象唯一；trace有效快照→任务Scope→finally恢复；queueCapacity null不变。
- Error and edge behavior: 参数非法无部分更新；同名不同对象拒绝；Future保留异常与取消；Redis失败不伪造已回滚。
- Standards impact: MC-SCOPE-001, MC-TEST-001；对应 Java采用命名Bean/Qualifier/Lombok与已有组件，非Java只验证配置及构建语义。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11；兼容优先、java.time、原包结构。
- Implementation pseudocode:
```text
DynamicThreadPoolAutoConfig: 空 Registry 与 threadManage/observer/validator；BPP 静态注册，原服务 Bean 名保留；Redis 与可选指标边界保留
输入采用 Spec §9 的已有参数与约束，逐项测试正常/缺失/重复/失败路径，保留旧调用方式。
以 Registry 为唯一对象目录并读取实际 after 状态；执行Scope的finally恢复与有限状态清理，不创建运行进程。
```
- Verification contribution: §8 focused tests、旧契约测试与三模块 test/package。
- After this file: 本文件行为与前序类型就绪；后续接入在本Step完成，最终统一编译验证。

#### File 27 — `MODIFY egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/main/java/top/egon/cola/component/dtp/config/DynamicThreadPoolAutoProperties.java`
- Purpose: 新增 observation Duration 阈值与可选告警配置，注解/校验与密码日志保护
- Symbols: DynamicThreadPoolAutoProperties 及 Spec §8/§10 对应方法/字段。
- Repository evidence: `egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/main/java/top/egon/cola/component/dtp/config/DynamicThreadPoolAutoProperties.java` 及同包现有 executor/config/context/controller 测试风格，Spec §2/§8。
- Dependencies and consumers: 上述前序类型与现有 Registry/Service/common-trace；对应模块测试作为消费者。
- Why now: 先固定缺失行为，再按 Command/观察/注册/接入依赖连接；不引入另一套管理事实。
- Contract/signature changes: 新增 observation Duration 阈值与可选告警配置，注解/校验与密码日志保护；旧包、构造、void setter、Bean name、Response与枚举线值保持。
- Input/output and state mapping: 以 Spec §7.3/§9/§10 为准，名称原对象唯一；trace有效快照→任务Scope→finally恢复；queueCapacity null不变。
- Error and edge behavior: 参数非法无部分更新；同名不同对象拒绝；Future保留异常与取消；Redis失败不伪造已回滚。
- Standards impact: MC-SCOPE-001, MC-TEST-001；对应 Java采用命名Bean/Qualifier/Lombok与已有组件，非Java只验证配置及构建语义。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11；兼容优先、java.time、原包结构。
- Implementation pseudocode:
```text
DynamicThreadPoolAutoProperties: 新增 observation Duration 阈值与可选告警配置，注解/校验与密码日志保护
输入采用 Spec §9 的已有参数与约束，逐项测试正常/缺失/重复/失败路径，保留旧调用方式。
以 Registry 为唯一对象目录并读取实际 after 状态；执行Scope的finally恢复与有限状态清理，不创建运行进程。
```
- Verification contribution: §8 focused tests、旧契约测试与三模块 test/package。
- After this file: 本文件行为与前序类型就绪；后续接入在本Step完成，最终统一编译验证。

#### File 28 — `MODIFY egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/main/java/top/egon/cola/component/dtp/metrics/DtpMeterBinder.java`
- Purpose: 保留原 Gauge；新池增量绑定，任务 Counter/Timer、能力/queueCapacity Gauge；只解绑自有 meters
- Symbols: DtpMeterBinder 及 Spec §8/§10 对应方法/字段。
- Repository evidence: `egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/main/java/top/egon/cola/component/dtp/metrics/DtpMeterBinder.java` 及同包现有 executor/config/context/controller 测试风格，Spec §2/§8。
- Dependencies and consumers: 上述前序类型与现有 Registry/Service/common-trace；对应模块测试作为消费者。
- Why now: 先固定缺失行为，再按 Command/观察/注册/接入依赖连接；不引入另一套管理事实。
- Contract/signature changes: 保留原 Gauge；新池增量绑定，任务 Counter/Timer、能力/queueCapacity Gauge；只解绑自有 meters；旧包、构造、void setter、Bean name、Response与枚举线值保持。
- Input/output and state mapping: 以 Spec §7.3/§9/§10 为准，名称原对象唯一；trace有效快照→任务Scope→finally恢复；queueCapacity null不变。
- Error and edge behavior: 参数非法无部分更新；同名不同对象拒绝；Future保留异常与取消；Redis失败不伪造已回滚。
- Standards impact: MC-SCOPE-001, MC-TEST-001；对应 Java采用命名Bean/Qualifier/Lombok与已有组件，非Java只验证配置及构建语义。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11；兼容优先、java.time、原包结构。
- Implementation pseudocode:
```text
DtpMeterBinder: 保留原 Gauge；新池增量绑定，任务 Counter/Timer、能力/queueCapacity Gauge；只解绑自有 meters
输入采用 Spec §9 的已有参数与约束，逐项测试正常/缺失/重复/失败路径，保留旧调用方式。
以 Registry 为唯一对象目录并读取实际 after 状态；执行Scope的finally恢复与有限状态清理，不创建运行进程。
```
- Verification contribution: §8 focused tests、旧契约测试与三模块 test/package。
- After this file: 本文件行为与前序类型就绪；后续接入在本Step完成，最终统一编译验证。

#### File 29 — `MODIFY egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/main/java/top/egon/cola/component/dtp/trigger/job/ThreadPoolDataReportJob.java`
- Purpose: 原报告周期保留；单池隔离；可选连续压力告警与冷却，不自动改池
- Symbols: ThreadPoolDataReportJob 及 Spec §8/§10 对应方法/字段。
- Repository evidence: `egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/main/java/top/egon/cola/component/dtp/trigger/job/ThreadPoolDataReportJob.java` 及同包现有 executor/config/context/controller 测试风格，Spec §2/§8。
- Dependencies and consumers: 上述前序类型与现有 Registry/Service/common-trace；对应模块测试作为消费者。
- Why now: 先固定缺失行为，再按 Command/观察/注册/接入依赖连接；不引入另一套管理事实。
- Contract/signature changes: 原报告周期保留；单池隔离；可选连续压力告警与冷却，不自动改池；旧包、构造、void setter、Bean name、Response与枚举线值保持。
- Input/output and state mapping: 以 Spec §7.3/§9/§10 为准，名称原对象唯一；trace有效快照→任务Scope→finally恢复；queueCapacity null不变。
- Error and edge behavior: 参数非法无部分更新；同名不同对象拒绝；Future保留异常与取消；Redis失败不伪造已回滚。
- Standards impact: MC-SCOPE-001, MC-TEST-001；对应 Java采用命名Bean/Qualifier/Lombok与已有组件，非Java只验证配置及构建语义。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11；兼容优先、java.time、原包结构。
- Implementation pseudocode:
```text
ThreadPoolDataReportJob: 原报告周期保留；单池隔离；可选连续压力告警与冷却，不自动改池
输入采用 Spec §9 的已有参数与约束，逐项测试正常/缺失/重复/失败路径，保留旧调用方式。
以 Registry 为唯一对象目录并读取实际 after 状态；执行Scope的finally恢复与有限状态清理，不创建运行进程。
```
- Verification contribution: §8 focused tests、旧契约测试与三模块 test/package。
- After this file: 本文件行为与前序类型就绪；后续接入在本Step完成，最终统一编译验证。

#### File 30 — `MODIFY egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/main/java/top/egon/cola/component/dtp/trigger/listener/ThreadPoolConfigAdjustListener.java`
- Purpose: 目标过滤、envelope/payload 一致性、有限 Guava 缓存；同 ID 冲突拒绝；审计失败不伪回滚
- Symbols: ThreadPoolConfigAdjustListener 及 Spec §8/§10 对应方法/字段。
- Repository evidence: `egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/main/java/top/egon/cola/component/dtp/trigger/listener/ThreadPoolConfigAdjustListener.java` 及同包现有 executor/config/context/controller 测试风格，Spec §2/§8。
- Dependencies and consumers: 上述前序类型与现有 Registry/Service/common-trace；对应模块测试作为消费者。
- Why now: 先固定缺失行为，再按 Command/观察/注册/接入依赖连接；不引入另一套管理事实。
- Contract/signature changes: 目标过滤、envelope/payload 一致性、有限 Guava 缓存；同 ID 冲突拒绝；审计失败不伪回滚；旧包、构造、void setter、Bean name、Response与枚举线值保持。
- Input/output and state mapping: 以 Spec §7.3/§9/§10 为准，名称原对象唯一；trace有效快照→任务Scope→finally恢复；queueCapacity null不变。
- Error and edge behavior: 参数非法无部分更新；同名不同对象拒绝；Future保留异常与取消；Redis失败不伪造已回滚。
- Standards impact: MC-SCOPE-001, MC-TEST-001；对应 Java采用命名Bean/Qualifier/Lombok与已有组件，非Java只验证配置及构建语义。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11；兼容优先、java.time、原包结构。
- Implementation pseudocode:
```text
ThreadPoolConfigAdjustListener: 目标过滤、envelope/payload 一致性、有限 Guava 缓存；同 ID 冲突拒绝；审计失败不伪回滚
输入采用 Spec §9 的已有参数与约束，逐项测试正常/缺失/重复/失败路径，保留旧调用方式。
以 Registry 为唯一对象目录并读取实际 after 状态；执行Scope的finally恢复与有限状态清理，不创建运行进程。
```
- Verification contribution: §8 focused tests、旧契约测试与三模块 test/package。
- After this file: 本文件行为与前序类型就绪；后续接入在本Step完成，最终统一编译验证。

#### File 31 — `MODIFY egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-admin/pom.xml`
- Purpose: 已批准 webmvc-api 与 MapStruct processor；保留既有 Lombok/Boot processor，不增 UI
- Symbols: pom 及 Spec §8/§10 对应方法/字段。
- Repository evidence: `egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-admin/pom.xml` 及同包现有 executor/config/context/controller 测试风格，Spec §2/§8。
- Dependencies and consumers: 上述前序类型与现有 Registry/Service/common-trace；对应模块测试作为消费者。
- Why now: 先固定缺失行为，再按 Command/观察/注册/接入依赖连接；不引入另一套管理事实。
- Contract/signature changes: 已批准 webmvc-api 与 MapStruct processor；保留既有 Lombok/Boot processor，不增 UI；旧包、构造、void setter、Bean name、Response与枚举线值保持。
- Input/output and state mapping: 以 Spec §7.3/§9/§10 为准，名称原对象唯一；trace有效快照→任务Scope→finally恢复；queueCapacity null不变。
- Error and edge behavior: 参数非法无部分更新；同名不同对象拒绝；Future保留异常与取消；Redis失败不伪造已回滚。
- Standards impact: MC-SCOPE-001, MC-TEST-001；对应 Java采用命名Bean/Qualifier/Lombok与已有组件，非Java只验证配置及构建语义。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11；兼容优先、java.time、原包结构。
- Implementation pseudocode:
```text
pom: 已批准 webmvc-api 与 MapStruct processor；保留既有 Lombok/Boot processor，不增 UI
输入采用 Spec §9 的已有参数与约束，逐项测试正常/缺失/重复/失败路径，保留旧调用方式。
以 Registry 为唯一对象目录并读取实际 after 状态；执行Scope的finally恢复与有限状态清理，不创建运行进程。
```
- Verification contribution: §8 focused tests、旧契约测试与三模块 test/package。
- After this file: 本文件行为与前序类型就绪；后续接入在本Step完成，最终统一编译验证。

#### File 32 — `MODIFY egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-admin/src/main/java/top/egon/cola/component/dtp/admin/trigger/model/ResizeExecutorRequest.java`
- Purpose: nullable queueCapacity、原签名与 enum 字符串；API schema 与 PlatformResize 校验
- Symbols: ResizeExecutorRequest 及 Spec §8/§10 对应方法/字段。
- Repository evidence: `egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-admin/src/main/java/top/egon/cola/component/dtp/admin/trigger/model/ResizeExecutorRequest.java` 及同包现有 executor/config/context/controller 测试风格，Spec §2/§8。
- Dependencies and consumers: 上述前序类型与现有 Registry/Service/common-trace；对应模块测试作为消费者。
- Why now: 先固定缺失行为，再按 Command/观察/注册/接入依赖连接；不引入另一套管理事实。
- Contract/signature changes: nullable queueCapacity、原签名与 enum 字符串；API schema 与 PlatformResize 校验；旧包、构造、void setter、Bean name、Response与枚举线值保持。
- Input/output and state mapping: 以 Spec §7.3/§9/§10 为准，名称原对象唯一；trace有效快照→任务Scope→finally恢复；queueCapacity null不变。
- Error and edge behavior: 参数非法无部分更新；同名不同对象拒绝；Future保留异常与取消；Redis失败不伪造已回滚。
- Standards impact: MC-SCOPE-001, MC-TEST-001；对应 Java采用命名Bean/Qualifier/Lombok与已有组件，非Java只验证配置及构建语义。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11；兼容优先、java.time、原包结构。
- Implementation pseudocode:
```text
ResizeExecutorRequest: nullable queueCapacity、原签名与 enum 字符串；API schema 与 PlatformResize 校验
输入采用 Spec §9 的已有参数与约束，逐项测试正常/缺失/重复/失败路径，保留旧调用方式。
以 Registry 为唯一对象目录并读取实际 after 状态；执行Scope的finally恢复与有限状态清理，不创建运行进程。
```
- Verification contribution: §8 focused tests、旧契约测试与三模块 test/package。
- After this file: 本文件行为与前序类型就绪；后续接入在本Step完成，最终统一编译验证。

#### File 33 — `CREATE egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-admin/src/main/java/top/egon/cola/component/dtp/admin/trigger/converter/ResizeExecutorConverter.java`
- Purpose: MapStruct BaseForwardConverter；body参数映射，app/instance/name 来自路径；operator 保留
- Symbols: ResizeExecutorConverter 及 Spec §8/§10 对应方法/字段。
- Repository evidence: `egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-admin/src/main/java/top/egon/cola/component/dtp/admin/trigger/converter/ResizeExecutorConverter.java` 及同包现有 executor/config/context/controller 测试风格，Spec §2/§8。
- Dependencies and consumers: 上述前序类型与现有 Registry/Service/common-trace；对应模块测试作为消费者。
- Why now: 先固定缺失行为，再按 Command/观察/注册/接入依赖连接；不引入另一套管理事实。
- Contract/signature changes: MapStruct BaseForwardConverter；body参数映射，app/instance/name 来自路径；operator 保留；旧包、构造、void setter、Bean name、Response与枚举线值保持。
- Input/output and state mapping: 以 Spec §7.3/§9/§10 为准，名称原对象唯一；trace有效快照→任务Scope→finally恢复；queueCapacity null不变。
- Error and edge behavior: 参数非法无部分更新；同名不同对象拒绝；Future保留异常与取消；Redis失败不伪造已回滚。
- Standards impact: MC-SCOPE-001, MC-TEST-001；对应 Java采用命名Bean/Qualifier/Lombok与已有组件，非Java只验证配置及构建语义。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11；兼容优先、java.time、原包结构。
- Implementation pseudocode:
```text
ResizeExecutorConverter: MapStruct BaseForwardConverter；body参数映射，app/instance/name 来自路径；operator 保留
输入采用 Spec §9 的已有参数与约束，逐项测试正常/缺失/重复/失败路径，保留旧调用方式。
以 Registry 为唯一对象目录并读取实际 after 状态；执行Scope的finally恢复与有限状态清理，不创建运行进程。
```
- Verification contribution: §8 focused tests、旧契约测试与三模块 test/package。
- After this file: 本文件行为与前序类型就绪；后续接入在本Step完成，最终统一编译验证。

#### File 34 — `MODIFY egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-admin/src/main/java/top/egon/cola/component/dtp/admin/trigger/DynamicThreadPoolController.java`
- Purpose: 原 Response 与 HTTP 路由保持；组校验、生成 converter、可选容量下发；OpenAPI3 注解
- Symbols: DynamicThreadPoolController 及 Spec §8/§10 对应方法/字段。
- Repository evidence: `egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-admin/src/main/java/top/egon/cola/component/dtp/admin/trigger/DynamicThreadPoolController.java` 及同包现有 executor/config/context/controller 测试风格，Spec §2/§8。
- Dependencies and consumers: 上述前序类型与现有 Registry/Service/common-trace；对应模块测试作为消费者。
- Why now: 先固定缺失行为，再按 Command/观察/注册/接入依赖连接；不引入另一套管理事实。
- Contract/signature changes: 原 Response 与 HTTP 路由保持；组校验、生成 converter、可选容量下发；OpenAPI3 注解；旧包、构造、void setter、Bean name、Response与枚举线值保持。
- Input/output and state mapping: 以 Spec §7.3/§9/§10 为准，名称原对象唯一；trace有效快照→任务Scope→finally恢复；queueCapacity null不变。
- Error and edge behavior: 参数非法无部分更新；同名不同对象拒绝；Future保留异常与取消；Redis失败不伪造已回滚。
- Standards impact: MC-SCOPE-001, MC-TEST-001；对应 Java采用命名Bean/Qualifier/Lombok与已有组件，非Java只验证配置及构建语义。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11；兼容优先、java.time、原包结构。
- Implementation pseudocode:
```text
DynamicThreadPoolController: 原 Response 与 HTTP 路由保持；组校验、生成 converter、可选容量下发；OpenAPI3 注解
输入采用 Spec §9 的已有参数与约束，逐项测试正常/缺失/重复/失败路径，保留旧调用方式。
以 Registry 为唯一对象目录并读取实际 after 状态；执行Scope的finally恢复与有限状态清理，不创建运行进程。
```
- Verification contribution: §8 focused tests、旧契约测试与三模块 test/package。
- After this file: 本文件行为与前序类型就绪；后续接入在本Step完成，最终统一编译验证。

#### File 35 — `MODIFY egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-admin/src/main/resources/application.yml`
- Purpose: 新增 observation 完整键结构；A 的 springdoc 生产关闭，test/dev 可开；其他值保持
- Symbols: application 及 Spec §8/§10 对应方法/字段。
- Repository evidence: `egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-admin/src/main/resources/application.yml` 及同包现有 executor/config/context/controller 测试风格，Spec §2/§8。
- Dependencies and consumers: 上述前序类型与现有 Registry/Service/common-trace；对应模块测试作为消费者。
- Why now: 先固定缺失行为，再按 Command/观察/注册/接入依赖连接；不引入另一套管理事实。
- Contract/signature changes: 新增 observation 完整键结构；A 的 springdoc 生产关闭，test/dev 可开；其他值保持；旧包、构造、void setter、Bean name、Response与枚举线值保持。
- Input/output and state mapping: 以 Spec §7.3/§9/§10 为准，名称原对象唯一；trace有效快照→任务Scope→finally恢复；queueCapacity null不变。
- Error and edge behavior: 参数非法无部分更新；同名不同对象拒绝；Future保留异常与取消；Redis失败不伪造已回滚。
- Standards impact: MC-SCOPE-001, MC-TEST-001；对应 Java采用命名Bean/Qualifier/Lombok与已有组件，非Java只验证配置及构建语义。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11；兼容优先、java.time、原包结构。
- Implementation pseudocode:
```text
application: 新增 observation 完整键结构；A 的 springdoc 生产关闭，test/dev 可开；其他值保持
输入采用 Spec §9 的已有参数与约束，逐项测试正常/缺失/重复/失败路径，保留旧调用方式。
以 Registry 为唯一对象目录并读取实际 after 状态；执行Scope的finally恢复与有限状态清理，不创建运行进程。
```
- Verification contribution: §8 focused tests、旧契约测试与三模块 test/package。
- After this file: 本文件行为与前序类型就绪；后续接入在本Step完成，最终统一编译验证。

#### File 36 — `MODIFY egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-admin/src/main/resources/application-dev.yml`
- Purpose: 新增 observation 完整键结构；A 的 springdoc 生产关闭，test/dev 可开；其他值保持
- Symbols: application-dev 及 Spec §8/§10 对应方法/字段。
- Repository evidence: `egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-admin/src/main/resources/application-dev.yml` 及同包现有 executor/config/context/controller 测试风格，Spec §2/§8。
- Dependencies and consumers: 上述前序类型与现有 Registry/Service/common-trace；对应模块测试作为消费者。
- Why now: 先固定缺失行为，再按 Command/观察/注册/接入依赖连接；不引入另一套管理事实。
- Contract/signature changes: 新增 observation 完整键结构；A 的 springdoc 生产关闭，test/dev 可开；其他值保持；旧包、构造、void setter、Bean name、Response与枚举线值保持。
- Input/output and state mapping: 以 Spec §7.3/§9/§10 为准，名称原对象唯一；trace有效快照→任务Scope→finally恢复；queueCapacity null不变。
- Error and edge behavior: 参数非法无部分更新；同名不同对象拒绝；Future保留异常与取消；Redis失败不伪造已回滚。
- Standards impact: MC-SCOPE-001, MC-TEST-001；对应 Java采用命名Bean/Qualifier/Lombok与已有组件，非Java只验证配置及构建语义。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11；兼容优先、java.time、原包结构。
- Implementation pseudocode:
```text
application-dev: 新增 observation 完整键结构；A 的 springdoc 生产关闭，test/dev 可开；其他值保持
输入采用 Spec §9 的已有参数与约束，逐项测试正常/缺失/重复/失败路径，保留旧调用方式。
以 Registry 为唯一对象目录并读取实际 after 状态；执行Scope的finally恢复与有限状态清理，不创建运行进程。
```
- Verification contribution: §8 focused tests、旧契约测试与三模块 test/package。
- After this file: 本文件行为与前序类型就绪；后续接入在本Step完成，最终统一编译验证。

#### File 37 — `MODIFY egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-admin/src/main/resources/application-test.yml`
- Purpose: 新增 observation 完整键结构；A 的 springdoc 生产关闭，test/dev 可开；其他值保持
- Symbols: application-test 及 Spec §8/§10 对应方法/字段。
- Repository evidence: `egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-admin/src/main/resources/application-test.yml` 及同包现有 executor/config/context/controller 测试风格，Spec §2/§8。
- Dependencies and consumers: 上述前序类型与现有 Registry/Service/common-trace；对应模块测试作为消费者。
- Why now: 先固定缺失行为，再按 Command/观察/注册/接入依赖连接；不引入另一套管理事实。
- Contract/signature changes: 新增 observation 完整键结构；A 的 springdoc 生产关闭，test/dev 可开；其他值保持；旧包、构造、void setter、Bean name、Response与枚举线值保持。
- Input/output and state mapping: 以 Spec §7.3/§9/§10 为准，名称原对象唯一；trace有效快照→任务Scope→finally恢复；queueCapacity null不变。
- Error and edge behavior: 参数非法无部分更新；同名不同对象拒绝；Future保留异常与取消；Redis失败不伪造已回滚。
- Standards impact: MC-SCOPE-001, MC-TEST-001；对应 Java采用命名Bean/Qualifier/Lombok与已有组件，非Java只验证配置及构建语义。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11；兼容优先、java.time、原包结构。
- Implementation pseudocode:
```text
application-test: 新增 observation 完整键结构；A 的 springdoc 生产关闭，test/dev 可开；其他值保持
输入采用 Spec §9 的已有参数与约束，逐项测试正常/缺失/重复/失败路径，保留旧调用方式。
以 Registry 为唯一对象目录并读取实际 after 状态；执行Scope的finally恢复与有限状态清理，不创建运行进程。
```
- Verification contribution: §8 focused tests、旧契约测试与三模块 test/package。
- After this file: 本文件行为与前序类型就绪；后续接入在本Step完成，最终统一编译验证。

#### File 38 — `MODIFY egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-admin/src/main/resources/application-prod.yml`
- Purpose: 新增 observation 完整键结构；A 的 springdoc 生产关闭，test/dev 可开；其他值保持
- Symbols: application-prod 及 Spec §8/§10 对应方法/字段。
- Repository evidence: `egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-admin/src/main/resources/application-prod.yml` 及同包现有 executor/config/context/controller 测试风格，Spec §2/§8。
- Dependencies and consumers: 上述前序类型与现有 Registry/Service/common-trace；对应模块测试作为消费者。
- Why now: 先固定缺失行为，再按 Command/观察/注册/接入依赖连接；不引入另一套管理事实。
- Contract/signature changes: 新增 observation 完整键结构；A 的 springdoc 生产关闭，test/dev 可开；其他值保持；旧包、构造、void setter、Bean name、Response与枚举线值保持。
- Input/output and state mapping: 以 Spec §7.3/§9/§10 为准，名称原对象唯一；trace有效快照→任务Scope→finally恢复；queueCapacity null不变。
- Error and edge behavior: 参数非法无部分更新；同名不同对象拒绝；Future保留异常与取消；Redis失败不伪造已回滚。
- Standards impact: MC-SCOPE-001, MC-TEST-001；对应 Java采用命名Bean/Qualifier/Lombok与已有组件，非Java只验证配置及构建语义。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11；兼容优先、java.time、原包结构。
- Implementation pseudocode:
```text
application-prod: 新增 observation 完整键结构；A 的 springdoc 生产关闭，test/dev 可开；其他值保持
输入采用 Spec §9 的已有参数与约束，逐项测试正常/缺失/重复/失败路径，保留旧调用方式。
以 Registry 为唯一对象目录并读取实际 after 状态；执行Scope的finally恢复与有限状态清理，不创建运行进程。
```
- Verification contribution: §8 focused tests、旧契约测试与三模块 test/package。
- After this file: 本文件行为与前序类型就绪；后续接入在本Step完成，最终统一编译验证。

#### File 39 — `MODIFY egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-test/src/main/resources/application.yml`
- Purpose: 新增 observation 完整键结构；A 的 springdoc 生产关闭，test/dev 可开；其他值保持
- Symbols: application 及 Spec §8/§10 对应方法/字段。
- Repository evidence: `egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-test/src/main/resources/application.yml` 及同包现有 executor/config/context/controller 测试风格，Spec §2/§8。
- Dependencies and consumers: 上述前序类型与现有 Registry/Service/common-trace；对应模块测试作为消费者。
- Why now: 先固定缺失行为，再按 Command/观察/注册/接入依赖连接；不引入另一套管理事实。
- Contract/signature changes: 新增 observation 完整键结构；A 的 springdoc 生产关闭，test/dev 可开；其他值保持；旧包、构造、void setter、Bean name、Response与枚举线值保持。
- Input/output and state mapping: 以 Spec §7.3/§9/§10 为准，名称原对象唯一；trace有效快照→任务Scope→finally恢复；queueCapacity null不变。
- Error and edge behavior: 参数非法无部分更新；同名不同对象拒绝；Future保留异常与取消；Redis失败不伪造已回滚。
- Standards impact: MC-SCOPE-001, MC-TEST-001；对应 Java采用命名Bean/Qualifier/Lombok与已有组件，非Java只验证配置及构建语义。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11；兼容优先、java.time、原包结构。
- Implementation pseudocode:
```text
application: 新增 observation 完整键结构；A 的 springdoc 生产关闭，test/dev 可开；其他值保持
输入采用 Spec §9 的已有参数与约束，逐项测试正常/缺失/重复/失败路径，保留旧调用方式。
以 Registry 为唯一对象目录并读取实际 after 状态；执行Scope的finally恢复与有限状态清理，不创建运行进程。
```
- Verification contribution: §8 focused tests、旧契约测试与三模块 test/package。
- After this file: 本文件行为与前序类型就绪；后续接入在本Step完成，最终统一编译验证。

#### File 40 — `MODIFY egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-test/src/main/resources/application-dev.yml`
- Purpose: 新增 observation 完整键结构；A 的 springdoc 生产关闭，test/dev 可开；其他值保持
- Symbols: application-dev 及 Spec §8/§10 对应方法/字段。
- Repository evidence: `egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-test/src/main/resources/application-dev.yml` 及同包现有 executor/config/context/controller 测试风格，Spec §2/§8。
- Dependencies and consumers: 上述前序类型与现有 Registry/Service/common-trace；对应模块测试作为消费者。
- Why now: 先固定缺失行为，再按 Command/观察/注册/接入依赖连接；不引入另一套管理事实。
- Contract/signature changes: 新增 observation 完整键结构；A 的 springdoc 生产关闭，test/dev 可开；其他值保持；旧包、构造、void setter、Bean name、Response与枚举线值保持。
- Input/output and state mapping: 以 Spec §7.3/§9/§10 为准，名称原对象唯一；trace有效快照→任务Scope→finally恢复；queueCapacity null不变。
- Error and edge behavior: 参数非法无部分更新；同名不同对象拒绝；Future保留异常与取消；Redis失败不伪造已回滚。
- Standards impact: MC-SCOPE-001, MC-TEST-001；对应 Java采用命名Bean/Qualifier/Lombok与已有组件，非Java只验证配置及构建语义。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11；兼容优先、java.time、原包结构。
- Implementation pseudocode:
```text
application-dev: 新增 observation 完整键结构；A 的 springdoc 生产关闭，test/dev 可开；其他值保持
输入采用 Spec §9 的已有参数与约束，逐项测试正常/缺失/重复/失败路径，保留旧调用方式。
以 Registry 为唯一对象目录并读取实际 after 状态；执行Scope的finally恢复与有限状态清理，不创建运行进程。
```
- Verification contribution: §8 focused tests、旧契约测试与三模块 test/package。
- After this file: 本文件行为与前序类型就绪；后续接入在本Step完成，最终统一编译验证。

#### File 41 — `MODIFY egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-test/src/main/resources/application-test.yml`
- Purpose: 新增 observation 完整键结构；A 的 springdoc 生产关闭，test/dev 可开；其他值保持
- Symbols: application-test 及 Spec §8/§10 对应方法/字段。
- Repository evidence: `egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-test/src/main/resources/application-test.yml` 及同包现有 executor/config/context/controller 测试风格，Spec §2/§8。
- Dependencies and consumers: 上述前序类型与现有 Registry/Service/common-trace；对应模块测试作为消费者。
- Why now: 先固定缺失行为，再按 Command/观察/注册/接入依赖连接；不引入另一套管理事实。
- Contract/signature changes: 新增 observation 完整键结构；A 的 springdoc 生产关闭，test/dev 可开；其他值保持；旧包、构造、void setter、Bean name、Response与枚举线值保持。
- Input/output and state mapping: 以 Spec §7.3/§9/§10 为准，名称原对象唯一；trace有效快照→任务Scope→finally恢复；queueCapacity null不变。
- Error and edge behavior: 参数非法无部分更新；同名不同对象拒绝；Future保留异常与取消；Redis失败不伪造已回滚。
- Standards impact: MC-SCOPE-001, MC-TEST-001；对应 Java采用命名Bean/Qualifier/Lombok与已有组件，非Java只验证配置及构建语义。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11；兼容优先、java.time、原包结构。
- Implementation pseudocode:
```text
application-test: 新增 observation 完整键结构；A 的 springdoc 生产关闭，test/dev 可开；其他值保持
输入采用 Spec §9 的已有参数与约束，逐项测试正常/缺失/重复/失败路径，保留旧调用方式。
以 Registry 为唯一对象目录并读取实际 after 状态；执行Scope的finally恢复与有限状态清理，不创建运行进程。
```
- Verification contribution: §8 focused tests、旧契约测试与三模块 test/package。
- After this file: 本文件行为与前序类型就绪；后续接入在本Step完成，最终统一编译验证。

#### File 42 — `MODIFY egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-test/src/main/resources/application-prod.yml`
- Purpose: 新增 observation 完整键结构；A 的 springdoc 生产关闭，test/dev 可开；其他值保持
- Symbols: application-prod 及 Spec §8/§10 对应方法/字段。
- Repository evidence: `egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-test/src/main/resources/application-prod.yml` 及同包现有 executor/config/context/controller 测试风格，Spec §2/§8。
- Dependencies and consumers: 上述前序类型与现有 Registry/Service/common-trace；对应模块测试作为消费者。
- Why now: 先固定缺失行为，再按 Command/观察/注册/接入依赖连接；不引入另一套管理事实。
- Contract/signature changes: 新增 observation 完整键结构；A 的 springdoc 生产关闭，test/dev 可开；其他值保持；旧包、构造、void setter、Bean name、Response与枚举线值保持。
- Input/output and state mapping: 以 Spec §7.3/§9/§10 为准，名称原对象唯一；trace有效快照→任务Scope→finally恢复；queueCapacity null不变。
- Error and edge behavior: 参数非法无部分更新；同名不同对象拒绝；Future保留异常与取消；Redis失败不伪造已回滚。
- Standards impact: MC-SCOPE-001, MC-TEST-001；对应 Java采用命名Bean/Qualifier/Lombok与已有组件，非Java只验证配置及构建语义。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11；兼容优先、java.time、原包结构。
- Implementation pseudocode:
```text
application-prod: 新增 observation 完整键结构；A 的 springdoc 生产关闭，test/dev 可开；其他值保持
输入采用 Spec §9 的已有参数与约束，逐项测试正常/缺失/重复/失败路径，保留旧调用方式。
以 Registry 为唯一对象目录并读取实际 after 状态；执行Scope的finally恢复与有限状态清理，不创建运行进程。
```
- Verification contribution: §8 focused tests、旧契约测试与三模块 test/package。
- After this file: 本文件行为与前序类型就绪；后续接入在本Step完成，最终统一编译验证。

#### File 43 — `MODIFY egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-test/src/main/java/top/egon/cola/component/dtp/test/config/ThreadPoolConfig.java`
- Purpose: 组件样例切换 ThreadManage 创建，保留平台与虚拟池接入说明
- Symbols: ThreadPoolConfig 及 Spec §8/§10 对应方法/字段。
- Repository evidence: `egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-test/src/main/java/top/egon/cola/component/dtp/test/config/ThreadPoolConfig.java` 及同包现有 executor/config/context/controller 测试风格，Spec §2/§8。
- Dependencies and consumers: 上述前序类型与现有 Registry/Service/common-trace；对应模块测试作为消费者。
- Why now: 先固定缺失行为，再按 Command/观察/注册/接入依赖连接；不引入另一套管理事实。
- Contract/signature changes: 组件样例切换 ThreadManage 创建，保留平台与虚拟池接入说明；旧包、构造、void setter、Bean name、Response与枚举线值保持。
- Input/output and state mapping: 以 Spec §7.3/§9/§10 为准，名称原对象唯一；trace有效快照→任务Scope→finally恢复；queueCapacity null不变。
- Error and edge behavior: 参数非法无部分更新；同名不同对象拒绝；Future保留异常与取消；Redis失败不伪造已回滚。
- Standards impact: MC-SCOPE-001, MC-TEST-001；对应 Java采用命名Bean/Qualifier/Lombok与已有组件，非Java只验证配置及构建语义。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11；兼容优先、java.time、原包结构。
- Implementation pseudocode:
```text
ThreadPoolConfig: 组件样例切换 ThreadManage 创建，保留平台与虚拟池接入说明
输入采用 Spec §9 的已有参数与约束，逐项测试正常/缺失/重复/失败路径，保留旧调用方式。
以 Registry 为唯一对象目录并读取实际 after 状态；执行Scope的finally恢复与有限状态清理，不创建运行进程。
```
- Verification contribution: §8 focused tests、旧契约测试与三模块 test/package。
- After this file: 本文件行为与前序类型就绪；后续接入在本Step完成，最终统一编译验证。

#### File 44 — `CREATE egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/test/java/top/egon/cola/component/dtp/config/DtpExecutorBeanPostProcessorTest.java`
- Purpose: 无循环、别名身份、生命周期、旧 Bean/可选 Micrometer 兼容
- Symbols: DtpExecutorBeanPostProcessorTest 及 Spec §8/§10 对应方法/字段。
- Repository evidence: `egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/test/java/top/egon/cola/component/dtp/config/DtpExecutorBeanPostProcessorTest.java` 及同包现有 executor/config/context/controller 测试风格，Spec §2/§8。
- Dependencies and consumers: 上述前序类型与现有 Registry/Service/common-trace；对应模块测试作为消费者。
- Why now: 先固定缺失行为，再按 Command/观察/注册/接入依赖连接；不引入另一套管理事实。
- Contract/signature changes: 无循环、别名身份、生命周期、旧 Bean/可选 Micrometer 兼容；旧包、构造、void setter、Bean name、Response与枚举线值保持。
- Input/output and state mapping: 以 Spec §7.3/§9/§10 为准，名称原对象唯一；trace有效快照→任务Scope→finally恢复；queueCapacity null不变。
- Error and edge behavior: 参数非法无部分更新；同名不同对象拒绝；Future保留异常与取消；Redis失败不伪造已回滚。
- Standards impact: MC-SCOPE-001, MC-TEST-001；对应 Java采用命名Bean/Qualifier/Lombok与已有组件，非Java只验证配置及构建语义。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11；兼容优先、java.time、原包结构。
- Implementation pseudocode:
```text
DtpExecutorBeanPostProcessorTest: 无循环、别名身份、生命周期、旧 Bean/可选 Micrometer 兼容
输入采用 Spec §9 的已有参数与约束，逐项测试正常/缺失/重复/失败路径，保留旧调用方式。
以 Registry 为唯一对象目录并读取实际 after 状态；执行Scope的finally恢复与有限状态清理，不创建运行进程。
```
- Verification contribution: §8 focused tests、旧契约测试与三模块 test/package。
- After this file: 本文件行为与前序类型就绪；后续接入在本Step完成，最终统一编译验证。

#### File 45 — `CREATE egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/test/java/top/egon/cola/component/dtp/metrics/DtpTaskMetricsTest.java`
- Purpose: Counter/Timer、动态绑定释放、无 traceId 标签、部分能力与压力冷却
- Symbols: DtpTaskMetricsTest 及 Spec §8/§10 对应方法/字段。
- Repository evidence: `egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/test/java/top/egon/cola/component/dtp/metrics/DtpTaskMetricsTest.java` 及同包现有 executor/config/context/controller 测试风格，Spec §2/§8。
- Dependencies and consumers: 上述前序类型与现有 Registry/Service/common-trace；对应模块测试作为消费者。
- Why now: 先固定缺失行为，再按 Command/观察/注册/接入依赖连接；不引入另一套管理事实。
- Contract/signature changes: Counter/Timer、动态绑定释放、无 traceId 标签、部分能力与压力冷却；旧包、构造、void setter、Bean name、Response与枚举线值保持。
- Input/output and state mapping: 以 Spec §7.3/§9/§10 为准，名称原对象唯一；trace有效快照→任务Scope→finally恢复；queueCapacity null不变。
- Error and edge behavior: 参数非法无部分更新；同名不同对象拒绝；Future保留异常与取消；Redis失败不伪造已回滚。
- Standards impact: MC-SCOPE-001, MC-TEST-001；对应 Java采用命名Bean/Qualifier/Lombok与已有组件，非Java只验证配置及构建语义。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11；兼容优先、java.time、原包结构。
- Implementation pseudocode:
```text
DtpTaskMetricsTest: Counter/Timer、动态绑定释放、无 traceId 标签、部分能力与压力冷却
输入采用 Spec §9 的已有参数与约束，逐项测试正常/缺失/重复/失败路径，保留旧调用方式。
以 Registry 为唯一对象目录并读取实际 after 状态；执行Scope的finally恢复与有限状态清理，不创建运行进程。
```
- Verification contribution: §8 focused tests、旧契约测试与三模块 test/package。
- After this file: 本文件行为与前序类型就绪；后续接入在本Step完成，最终统一编译验证。

#### File 46 — `CREATE egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/test/java/top/egon/cola/component/dtp/trigger/listener/DtpConfigChangeSafetyTest.java`
- Purpose: 目标过滤、重复/冲突消息、审计失败、MDC 恢复
- Symbols: DtpConfigChangeSafetyTest 及 Spec §8/§10 对应方法/字段。
- Repository evidence: `egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/test/java/top/egon/cola/component/dtp/trigger/listener/DtpConfigChangeSafetyTest.java` 及同包现有 executor/config/context/controller 测试风格，Spec §2/§8。
- Dependencies and consumers: 上述前序类型与现有 Registry/Service/common-trace；对应模块测试作为消费者。
- Why now: 先固定缺失行为，再按 Command/观察/注册/接入依赖连接；不引入另一套管理事实。
- Contract/signature changes: 目标过滤、重复/冲突消息、审计失败、MDC 恢复；旧包、构造、void setter、Bean name、Response与枚举线值保持。
- Input/output and state mapping: 以 Spec §7.3/§9/§10 为准，名称原对象唯一；trace有效快照→任务Scope→finally恢复；queueCapacity null不变。
- Error and edge behavior: 参数非法无部分更新；同名不同对象拒绝；Future保留异常与取消；Redis失败不伪造已回滚。
- Standards impact: MC-SCOPE-001, MC-TEST-001；对应 Java采用命名Bean/Qualifier/Lombok与已有组件，非Java只验证配置及构建语义。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11；兼容优先、java.time、原包结构。
- Implementation pseudocode:
```text
DtpConfigChangeSafetyTest: 目标过滤、重复/冲突消息、审计失败、MDC 恢复
输入采用 Spec §9 的已有参数与约束，逐项测试正常/缺失/重复/失败路径，保留旧调用方式。
以 Registry 为唯一对象目录并读取实际 after 状态；执行Scope的finally恢复与有限状态清理，不创建运行进程。
```
- Verification contribution: §8 focused tests、旧契约测试与三模块 test/package。
- After this file: 本文件行为与前序类型就绪；后续接入在本Step完成，最终统一编译验证。

#### File 47 — `CREATE egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/test/java/top/egon/cola/component/dtp/executor/support/DtpResizeSafetyTest.java`
- Purpose: Spring 与底层参数同步、所有非法组合零变更、队列锁/虚拟缩容
- Symbols: DtpResizeSafetyTest 及 Spec §8/§10 对应方法/字段。
- Repository evidence: `egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/test/java/top/egon/cola/component/dtp/executor/support/DtpResizeSafetyTest.java` 及同包现有 executor/config/context/controller 测试风格，Spec §2/§8。
- Dependencies and consumers: 上述前序类型与现有 Registry/Service/common-trace；对应模块测试作为消费者。
- Why now: 先固定缺失行为，再按 Command/观察/注册/接入依赖连接；不引入另一套管理事实。
- Contract/signature changes: Spring 与底层参数同步、所有非法组合零变更、队列锁/虚拟缩容；旧包、构造、void setter、Bean name、Response与枚举线值保持。
- Input/output and state mapping: 以 Spec §7.3/§9/§10 为准，名称原对象唯一；trace有效快照→任务Scope→finally恢复；queueCapacity null不变。
- Error and edge behavior: 参数非法无部分更新；同名不同对象拒绝；Future保留异常与取消；Redis失败不伪造已回滚。
- Standards impact: MC-SCOPE-001, MC-TEST-001；对应 Java采用命名Bean/Qualifier/Lombok与已有组件，非Java只验证配置及构建语义。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11；兼容优先、java.time、原包结构。
- Implementation pseudocode:
```text
DtpResizeSafetyTest: Spring 与底层参数同步、所有非法组合零变更、队列锁/虚拟缩容
输入采用 Spec §9 的已有参数与约束，逐项测试正常/缺失/重复/失败路径，保留旧调用方式。
以 Registry 为唯一对象目录并读取实际 after 状态；执行Scope的finally恢复与有限状态清理，不创建运行进程。
```
- Verification contribution: §8 focused tests、旧契约测试与三模块 test/package。
- After this file: 本文件行为与前序类型就绪；后续接入在本Step完成，最终统一编译验证。

#### File 48 — `CREATE egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-admin/src/test/java/top/egon/cola/component/dtp/admin/trigger/converter/ResizeExecutorConverterTest.java`
- Purpose: 所有字段与路径身份映射，旧请求缺新字段兼容
- Symbols: ResizeExecutorConverterTest 及 Spec §8/§10 对应方法/字段。
- Repository evidence: `egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-admin/src/test/java/top/egon/cola/component/dtp/admin/trigger/converter/ResizeExecutorConverterTest.java` 及同包现有 executor/config/context/controller 测试风格，Spec §2/§8。
- Dependencies and consumers: 上述前序类型与现有 Registry/Service/common-trace；对应模块测试作为消费者。
- Why now: 先固定缺失行为，再按 Command/观察/注册/接入依赖连接；不引入另一套管理事实。
- Contract/signature changes: 所有字段与路径身份映射，旧请求缺新字段兼容；旧包、构造、void setter、Bean name、Response与枚举线值保持。
- Input/output and state mapping: 以 Spec §7.3/§9/§10 为准，名称原对象唯一；trace有效快照→任务Scope→finally恢复；queueCapacity null不变。
- Error and edge behavior: 参数非法无部分更新；同名不同对象拒绝；Future保留异常与取消；Redis失败不伪造已回滚。
- Standards impact: MC-SCOPE-001, MC-TEST-001；对应 Java采用命名Bean/Qualifier/Lombok与已有组件，非Java只验证配置及构建语义。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11；兼容优先、java.time、原包结构。
- Implementation pseudocode:
```text
ResizeExecutorConverterTest: 所有字段与路径身份映射，旧请求缺新字段兼容
输入采用 Spec §9 的已有参数与约束，逐项测试正常/缺失/重复/失败路径，保留旧调用方式。
以 Registry 为唯一对象目录并读取实际 after 状态；执行Scope的finally恢复与有限状态清理，不创建运行进程。
```
- Verification contribution: §8 focused tests、旧契约测试与三模块 test/package。
- After this file: 本文件行为与前序类型就绪；后续接入在本Step完成，最终统一编译验证。

#### File 49 — `CREATE egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-admin/src/test/java/top/egon/cola/component/dtp/admin/trigger/DynamicThreadPoolOpenApiTest.java`
- Purpose: MockMvc /v3/api-docs 解析真实 OAS：operationId、Boolean信封、queue可选、enum、HTTP状态
- Symbols: DynamicThreadPoolOpenApiTest 及 Spec §8/§10 对应方法/字段。
- Repository evidence: `egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-admin/src/test/java/top/egon/cola/component/dtp/admin/trigger/DynamicThreadPoolOpenApiTest.java` 及同包现有 executor/config/context/controller 测试风格，Spec §2/§8。
- Dependencies and consumers: 上述前序类型与现有 Registry/Service/common-trace；对应模块测试作为消费者。
- Why now: 先固定缺失行为，再按 Command/观察/注册/接入依赖连接；不引入另一套管理事实。
- Contract/signature changes: MockMvc /v3/api-docs 解析真实 OAS：operationId、Boolean信封、queue可选、enum、HTTP状态；旧包、构造、void setter、Bean name、Response与枚举线值保持。
- Input/output and state mapping: 以 Spec §7.3/§9/§10 为准，名称原对象唯一；trace有效快照→任务Scope→finally恢复；queueCapacity null不变。
- Error and edge behavior: 参数非法无部分更新；同名不同对象拒绝；Future保留异常与取消；Redis失败不伪造已回滚。
- Standards impact: MC-SCOPE-001, MC-TEST-001；对应 Java采用命名Bean/Qualifier/Lombok与已有组件，非Java只验证配置及构建语义。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11；兼容优先、java.time、原包结构。
- Implementation pseudocode:
```text
DynamicThreadPoolOpenApiTest: MockMvc /v3/api-docs 解析真实 OAS：operationId、Boolean信封、queue可选、enum、HTTP状态
输入采用 Spec §9 的已有参数与约束，逐项测试正常/缺失/重复/失败路径，保留旧调用方式。
以 Registry 为唯一对象目录并读取实际 after 状态；执行Scope的finally恢复与有限状态清理，不创建运行进程。
```
- Verification contribution: §8 focused tests、旧契约测试与三模块 test/package。
- After this file: 本文件行为与前序类型就绪；后续接入在本Step完成，最终统一编译验证。

#### File 50 — `CREATE egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/test/java/top/egon/cola/component/dtp/config/DtpObservationPropertiesTest.java`
- Purpose: Duration/组约束/全profile新增键对照与生产docs关闭
- Symbols: DtpObservationPropertiesTest 及 Spec §8/§10 对应方法/字段。
- Repository evidence: `egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/test/java/top/egon/cola/component/dtp/config/DtpObservationPropertiesTest.java` 及同包现有 executor/config/context/controller 测试风格，Spec §2/§8。
- Dependencies and consumers: 上述前序类型与现有 Registry/Service/common-trace；对应模块测试作为消费者。
- Why now: 先固定缺失行为，再按 Command/观察/注册/接入依赖连接；不引入另一套管理事实。
- Contract/signature changes: Duration/组约束/全profile新增键对照与生产docs关闭；旧包、构造、void setter、Bean name、Response与枚举线值保持。
- Input/output and state mapping: 以 Spec §7.3/§9/§10 为准，名称原对象唯一；trace有效快照→任务Scope→finally恢复；queueCapacity null不变。
- Error and edge behavior: 参数非法无部分更新；同名不同对象拒绝；Future保留异常与取消；Redis失败不伪造已回滚。
- Standards impact: MC-SCOPE-001, MC-TEST-001；对应 Java采用命名Bean/Qualifier/Lombok与已有组件，非Java只验证配置及构建语义。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11；兼容优先、java.time、原包结构。
- Implementation pseudocode:
```text
DtpObservationPropertiesTest: Duration/组约束/全profile新增键对照与生产docs关闭
输入采用 Spec §9 的已有参数与约束，逐项测试正常/缺失/重复/失败路径，保留旧调用方式。
以 Registry 为唯一对象目录并读取实际 after 状态；执行Scope的finally恢复与有限状态清理，不创建运行进程。
```
- Verification contribution: §8 focused tests、旧契约测试与三模块 test/package。
- After this file: 本文件行为与前序类型就绪；后续接入在本Step完成，最终统一编译验证。

#### File 51 — `MODIFY egon-cola-components/egon-cola-component-dynamic-thread-pool/README.md`
- Purpose: 新增 ThreadManage 使用示例、原Runnable等API兼容、能力边界、指标/告警/动态队列与Redis异步语义
- Symbols: README 及 Spec §8/§10 对应方法/字段。
- Repository evidence: `egon-cola-components/egon-cola-component-dynamic-thread-pool/README.md` 及同包现有 executor/config/context/controller 测试风格，Spec §2/§8。
- Dependencies and consumers: 上述前序类型与现有 Registry/Service/common-trace；对应模块测试作为消费者。
- Why now: 先固定缺失行为，再按 Command/观察/注册/接入依赖连接；不引入另一套管理事实。
- Contract/signature changes: 新增 ThreadManage 使用示例、原Runnable等API兼容、能力边界、指标/告警/动态队列与Redis异步语义；旧包、构造、void setter、Bean name、Response与枚举线值保持。
- Input/output and state mapping: 以 Spec §7.3/§9/§10 为准，名称原对象唯一；trace有效快照→任务Scope→finally恢复；queueCapacity null不变。
- Error and edge behavior: 参数非法无部分更新；同名不同对象拒绝；Future保留异常与取消；Redis失败不伪造已回滚。
- Standards impact: MC-SCOPE-001, MC-TEST-001；对应 Java采用命名Bean/Qualifier/Lombok与已有组件，非Java只验证配置及构建语义。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11；兼容优先、java.time、原包结构。
- Implementation pseudocode:
```text
README: 新增 ThreadManage 使用示例、原Runnable等API兼容、能力边界、指标/告警/动态队列与Redis异步语义
输入采用 Spec §9 的已有参数与约束，逐项测试正常/缺失/重复/失败路径，保留旧调用方式。
以 Registry 为唯一对象目录并读取实际 after 状态；执行Scope的finally恢复与有限状态清理，不创建运行进程。
```
- Verification contribution: §8 focused tests、旧契约测试与三模块 test/package。
- After this file: 本文件行为与前序类型就绪；后续接入在本Step完成，最终统一编译验证。

#### File 52 — `MODIFY egon-cola-components/egon-cola-component-dynamic-thread-pool/README.zh-CN.md`
- Purpose: 新增 ThreadManage 使用示例、原Runnable等API兼容、能力边界、指标/告警/动态队列与Redis异步语义
- Symbols: README.zh-CN 及 Spec §8/§10 对应方法/字段。
- Repository evidence: `egon-cola-components/egon-cola-component-dynamic-thread-pool/README.zh-CN.md` 及同包现有 executor/config/context/controller 测试风格，Spec §2/§8。
- Dependencies and consumers: 上述前序类型与现有 Registry/Service/common-trace；对应模块测试作为消费者。
- Why now: 先固定缺失行为，再按 Command/观察/注册/接入依赖连接；不引入另一套管理事实。
- Contract/signature changes: 新增 ThreadManage 使用示例、原Runnable等API兼容、能力边界、指标/告警/动态队列与Redis异步语义；旧包、构造、void setter、Bean name、Response与枚举线值保持。
- Input/output and state mapping: 以 Spec §7.3/§9/§10 为准，名称原对象唯一；trace有效快照→任务Scope→finally恢复；queueCapacity null不变。
- Error and edge behavior: 参数非法无部分更新；同名不同对象拒绝；Future保留异常与取消；Redis失败不伪造已回滚。
- Standards impact: MC-SCOPE-001, MC-TEST-001；对应 Java采用命名Bean/Qualifier/Lombok与已有组件，非Java只验证配置及构建语义。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11；兼容优先、java.time、原包结构。
- Implementation pseudocode:
```text
README.zh-CN: 新增 ThreadManage 使用示例、原Runnable等API兼容、能力边界、指标/告警/动态队列与Redis异步语义
输入采用 Spec §9 的已有参数与约束，逐项测试正常/缺失/重复/失败路径，保留旧调用方式。
以 Registry 为唯一对象目录并读取实际 after 状态；执行Scope的finally恢复与有限状态清理，不创建运行进程。
```
- Verification contribution: §8 focused tests、旧契约测试与三模块 test/package。
- After this file: 本文件行为与前序类型就绪；后续接入在本Step完成，最终统一编译验证。
#### File 53 — `MODIFY egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-admin/src/test/java/top/egon/cola/component/dtp/admin/trigger/DynamicThreadPoolControllerTest.java`
- Purpose: 为构造注入提供真实生成mapper Spy与ValidationUtils mock，保留原16项契约断言。
- Symbols: DynamicThreadPoolControllerTest 以及原Controller构造和MockMvc测试日志配置。
- Repository evidence: 原ControllerTest的InjectMocks fixture和Admin生产logback的data文件路径。
- Dependencies and consumers: 原HTTP契约测试、生成ResizeExecutorConverter与隔离MockMvc Context。
- Why now: 生产构造注入和文档生成接入后，测试必须提供真实转换与无外部写入的日志边界。
- Contract/signature changes: 原16项断言与HTTP路由不变；日志配置只对新MockMvc测试生效。
- Input/output and state mapping: 旧请求/Redis mock -> 原信封；测试日志输出控制台，不写生产data。
- Error and edge behavior: 失败继续原断言；无真实Redis或Web监听端口；不改变应用日志配置。
- Standards impact: MC-SCOPE-001, MC-TEST-001；仅测试fixture/resource兼容与隔离。
- Literal rule enforcement: Rule 4, Rule 6, Rule 7, Rule 11；保留既有结构与wire行为。
- Implementation pseudocode:
```text
为构造注入提供真实生成mapper Spy与ValidationUtils mock，保留原16项契约断言；保持现有原始请求和期望状态、code、Boolean字段不变。
MockMvc加载真实生成mapper与Validation provider，Redis使用mock，生成OAS后解析其真实schema。
日志仅输出测试控制台，禁止触发Admin主程序、真实数据库或外部服务。
```
- Verification contribution: DynamicThreadPoolControllerTest、DynamicThreadPoolOpenApiTest均成功。
- After this file: 原fixture和新MockMvc契约完整通过，验证不污染应用data目录。

#### File 54 — `MODIFY egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-admin/src/test/resources/logback-dtp-test.xml`
- Purpose: 隔离MockMvc Context日志到控制台，避免生成项目data目录。
- Symbols: logback-dtp-test 以及原Controller构造和MockMvc测试日志配置。
- Repository evidence: 原ControllerTest的InjectMocks fixture和Admin生产logback的data文件路径。
- Dependencies and consumers: 原HTTP契约测试、生成ResizeExecutorConverter与隔离MockMvc Context。
- Why now: 生产构造注入和文档生成接入后，测试必须提供真实转换与无外部写入的日志边界。
- Contract/signature changes: 原16项断言与HTTP路由不变；日志配置只对新MockMvc测试生效。
- Input/output and state mapping: 旧请求/Redis mock -> 原信封；测试日志输出控制台，不写生产data。
- Error and edge behavior: 失败继续原断言；无真实Redis或Web监听端口；不改变应用日志配置。
- Standards impact: MC-SCOPE-001, MC-TEST-001；仅测试fixture/resource兼容与隔离。
- Literal rule enforcement: Rule 4, Rule 6, Rule 7, Rule 11；保留既有结构与wire行为。
- Implementation pseudocode:
```text
隔离MockMvc Context日志到控制台，避免生成项目data目录；保持现有原始请求和期望状态、code、Boolean字段不变。
MockMvc加载真实生成mapper与Validation provider，Redis使用mock，生成OAS后解析其真实schema。
日志仅输出测试控制台，禁止触发Admin主程序、真实数据库或外部服务。
```
- Verification contribution: DynamicThreadPoolControllerTest、DynamicThreadPoolOpenApiTest均成功。
- After this file: 原fixture和新MockMvc契约完整通过，验证不污染应用data目录。

- Validation working directory: /Users/mario/SelfProject/Egon-COLA
- Verification command: ./mvnw -B -ntp -f egon-cola-components/pom.xml -pl :egon-cola-component-dynamic-thread-pool-starter,:egon-cola-component-dynamic-thread-pool-admin,:egon-cola-component-dynamic-thread-pool-test -am test
- Expected result: Exit 0，全部针对测试与原契约回归成功，不启动应用/Redis/浏览器。
- Failure returns to: 当前 File 的实现/测试；仅本任务直接相关修复。
- Completion criteria: Spec REQ-001..007 静态/编译/模块测试证据完备，运行边界明确。
- Rollback: 以 b8f9c9a28 为只读对照，需要回退时使用本任务单独提交的逆向修复，不自动重写历史。
- Commit paths: egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/test/java/top/egon/cola/component/dtp/executor/ThreadManageTest.java; egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/test/java/top/egon/cola/component/dtp/executor/support/ResizableCapacityBlockingQueueTest.java; egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/test/java/top/egon/cola/component/dtp/context/DtpTaskObserverTest.java; egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/pom.xml; egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/main/java/top/egon/cola/component/dtp/domain/model/entity/ThreadPoolCreateCommand.java; egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/main/java/top/egon/cola/component/dtp/domain/model/entity/ExecutorUpdateCommand.java; egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/main/java/top/egon/cola/component/dtp/executor/support/ResizableCapacityBlockingQueue.java; egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/main/java/top/egon/cola/component/dtp/context/DtpTaskObserver.java; egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/main/java/top/egon/cola/component/dtp/context/DtpObservedFutureTask.java; egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/main/java/top/egon/cola/component/dtp/executor/managed/DtpThreadPoolTaskExecutor.java; egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/main/java/top/egon/cola/component/dtp/executor/ManagedExecutor.java; egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/main/java/top/egon/cola/component/dtp/executor/ManagedExecutorRegistry.java; egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/main/java/top/egon/cola/component/dtp/executor/ThreadManage.java; egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/main/java/top/egon/cola/component/dtp/executor/support/ThreadPoolResizeSupport.java; egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/main/java/top/egon/cola/component/dtp/executor/adapter/ThreadPoolExecutorManagedExecutor.java; egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/main/java/top/egon/cola/component/dtp/executor/adapter/ThreadPoolTaskExecutorManagedExecutor.java; egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/main/java/top/egon/cola/component/dtp/executor/adapter/BoundedVirtualThreadManagedExecutor.java; egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/main/java/top/egon/cola/component/dtp/executor/virtual/BoundedVirtualThreadExecutor.java; egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/main/java/top/egon/cola/component/dtp/context/DtpContextAwareExecutorService.java; egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/main/java/top/egon/cola/component/dtp/context/DtpRunnable.java; egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/main/java/top/egon/cola/component/dtp/context/DtpCallable.java; egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/main/java/top/egon/cola/component/dtp/context/DtpSupplier.java; egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/main/java/top/egon/cola/component/dtp/context/DtpTaskDecorator.java; egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/main/java/top/egon/cola/component/dtp/domain/DynamicThreadPoolService.java; egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/main/java/top/egon/cola/component/dtp/config/DtpExecutorBeanPostProcessor.java; egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/main/java/top/egon/cola/component/dtp/config/DynamicThreadPoolAutoConfig.java; egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/main/java/top/egon/cola/component/dtp/config/DynamicThreadPoolAutoProperties.java; egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/main/java/top/egon/cola/component/dtp/metrics/DtpMeterBinder.java; egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/main/java/top/egon/cola/component/dtp/trigger/job/ThreadPoolDataReportJob.java; egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/main/java/top/egon/cola/component/dtp/trigger/listener/ThreadPoolConfigAdjustListener.java; egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-admin/pom.xml; egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-admin/src/main/java/top/egon/cola/component/dtp/admin/trigger/model/ResizeExecutorRequest.java; egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-admin/src/main/java/top/egon/cola/component/dtp/admin/trigger/converter/ResizeExecutorConverter.java; egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-admin/src/main/java/top/egon/cola/component/dtp/admin/trigger/DynamicThreadPoolController.java; egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-admin/src/main/resources/application.yml; egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-admin/src/main/resources/application-dev.yml; egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-admin/src/main/resources/application-test.yml; egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-admin/src/main/resources/application-prod.yml; egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-test/src/main/resources/application.yml; egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-test/src/main/resources/application-dev.yml; egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-test/src/main/resources/application-test.yml; egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-test/src/main/resources/application-prod.yml; egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-test/src/main/java/top/egon/cola/component/dtp/test/config/ThreadPoolConfig.java; egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/test/java/top/egon/cola/component/dtp/config/DtpExecutorBeanPostProcessorTest.java; egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/test/java/top/egon/cola/component/dtp/metrics/DtpTaskMetricsTest.java; egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/test/java/top/egon/cola/component/dtp/trigger/listener/DtpConfigChangeSafetyTest.java; egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/test/java/top/egon/cola/component/dtp/executor/support/DtpResizeSafetyTest.java; egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-admin/src/test/java/top/egon/cola/component/dtp/admin/trigger/converter/ResizeExecutorConverterTest.java; egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-admin/src/test/java/top/egon/cola/component/dtp/admin/trigger/DynamicThreadPoolOpenApiTest.java; egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/test/java/top/egon/cola/component/dtp/config/DtpObservationPropertiesTest.java; egon-cola-components/egon-cola-component-dynamic-thread-pool/README.md; egon-cola-components/egon-cola-component-dynamic-thread-pool/README.zh-CN.md; egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-admin/src/test/java/top/egon/cola/component/dtp/admin/trigger/DynamicThreadPoolControllerTest.java; egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-admin/src/test/resources/logback-dtp-test.xml
- Commit: feat(dtp): unify thread management with trace monitoring and safe resizing

## 8. Test, Validation, and Quality Gates
| Gate/order | Working directory | Command or method | Scope | Expected result | Failure returns to | Requirements/runtime boundary |
| --- | --- | --- | --- | --- | --- | --- |
| RED | repository root | Maven -Dtest=ThreadManageTest -Dsurefire.failIfNoSpecifiedTests=false test | starter | 原代码断言缺少ThreadManage失败 | 首测试 | 无监听端口 |
| GREEN/regression | repository root | ./mvnw -B -ntp -f egon-cola-components/pom.xml -pl :egon-cola-component-dynamic-thread-pool-starter,:egon-cola-component-dynamic-thread-pool-admin,:egon-cola-component-dynamic-thread-pool-test -am test | 三模块及common依赖 | BUILD SUCCESS | 对应File | TEST-001..012；无真实服务 |
| Package | repository root | 同 reactor 参数 -DskipTests package | 三模块 | jar/编译成功 | 对应File | 不启动 |
| Static | repository root | git diff --check；Spec/Plan strict validator | 本任务路径 | 无错误 | 对应文档/File | 无运行证据 |

## 9. Migration, Compatibility, Rollout, and Rollback
按 Spec §16；旧原Bean/包装/API原样保留，先升级starter再发新容量字段；无SQL或UI迁移，不自动部署或执行用户运行验收。

## 10. Requirement-to-Step Traceability Matrix
| Requirement | Effective Spec section | Steps | Files | Tests/gates | Completion evidence |
| --- | --- | --- | --- | --- | --- |
| REQ-001 | Spec §4/§14 | Step 1 | §5/§7精确文件 | TEST-001..012及§8 | 测试结果与最终提交核对 |
| REQ-002 | Spec §4/§14 | Step 1 | §5/§7精确文件 | TEST-001..012及§8 | 测试结果与最终提交核对 |
| REQ-003 | Spec §4/§14 | Step 1 | §5/§7精确文件 | TEST-001..012及§8 | 测试结果与最终提交核对 |
| REQ-004 | Spec §4/§14 | Step 1 | §5/§7精确文件 | TEST-001..012及§8 | 测试结果与最终提交核对 |
| REQ-005 | Spec §4/§14 | Step 1 | §5/§7精确文件 | TEST-001..012及§8 | 测试结果与最终提交核对 |
| REQ-006 | Spec §4/§14 | Step 1 | §5/§7精确文件 | TEST-001..012及§8 | 测试结果与最终提交核对 |
| REQ-007 | Spec §4/§14 | Step 1 | §5/§7精确文件 | TEST-001..012及§8 | 测试结果与最终提交核对 |

## 11. Risks, Blockers, and User Decisions
用户已批准 DEC-004/005，并明确实施与不启动项目。无待用户决定的设计问题；Redis/线上监控部署不属于本次运行授权。

## 12. Review and Acceptance
当前用户已授权按方案直接实施；计划按同一逻辑任务单提交执行。手工检查表中 PASS 指实施方案可执行性，实际结果追加在最终验证记录。
### 12.5 Blocking Manual Check
| Check ID | Applicability | Status | Evidence | Finding | Required action/exception |
| --- | --- | --- | --- | --- | --- |
| MC-ARCH-001 | Applicable | PASS | DTP既有目录与A→S/T→S；无SQL/UI变化 | 用户批准保留基础设施结构 | None |
| MC-REUSE-001 | Applicable | PASS | TraceContext/Scope、Registry、三Adapter、ValidationUtils、BaseForwardConverter | 没有第二套池目录或Trace实现 | None |
| MC-DEP-001 | Applicable | PASS | S/A POM、用户批准DEC-004/005、Maven编译和生成mapper | Validation/springdoc/MapStruct同版本能力准入已满足 | None |
| MC-NAME-001 | Applicable | PASS | ThreadPoolCreateCommand、ThreadManage、原类型兼容清单 | 新类型语义清楚，旧公开命名未强改 | None |
| MC-VALID-001 | Applicable | PASS | 真实Provider、Create/PlatformResize/VirtualLimit组；Properties/MockMvc/ResizeSafety测试 | 原生约束与锁内状态校验在实际入口执行 | None |
| MC-MODEL-001 | Applicable | PASS | CreateCommand与新Properties使用Lombok基线；旧setter保留void；内部状态仅私有 | 无无意义父类、无公共POJO膨胀 | None |
| MC-CONVERT-001 | Applicable | PASS | ResizeExecutorConverter extends BaseForwardConverter；生成Impl及映射测试 | 请求映射由MapStruct编译生成，路径身份权威 | None |
| MC-LOG-001 | Applicable | PASS | TaskObserver、listener/job的安全限频日志，密码ToString排除 | Trace/池身份可关联，不输出任务载荷或密码 | None |
| MC-BEAN-001 | Applicable | PASS | Factory/BPP Context、Autowired/Qualifier反射测试、命名mapper | 无创建循环、原类型与别名保持、关闭归属单一 | None |
| MC-UTIL-001 | Applicable | PASS | JDK queue/locks/java.time、原Guava Cache | 无新增工具框架/Utils复制 | None |
| MC-JSON-001 | Applicable | PASS | OpenAPI/MockMvc/Redis codec测试，queueCapacity NON_NULL | 原信封/enum线值与缺失字段兼容 | None |
| MC-TIME-001 | Applicable | PASS | nanoTime/Duration与原Instant，Timer/阈值和codec测试 | 无java.util.Date/Calendar/SimpleDateFormat新增 | None |
| MC-CONFIG-001 | Applicable | PASS | 八个profile键检查、生产docs关闭、Properties绑定与负例测试 | 新增核心键一致且默认告警关闭 | None |
| MC-PATTERN-001 | Applicable | PASS | ThreadManage Facade、原Adapter、Dtp装饰/FutureTask扩展 | 模式直接对应三种执行器与异步结果差异 | None |
| MC-SCOPE-001 | Applicable | PASS | 最终diff限DTP与本Spec/Plan；没有target/日志/密钥/SQL | 未改变其他模块或用户数据；最多一提交 | None |
| MC-TEST-001 | Applicable | PASS | 最终package进程退出0；DTP145执行成功/17原环境用例跳过 | 已完成授权的编译/单元/Context/MockMvc/打包验证 | None |
| MC-BLOCKER-001 | Applicable | PASS | 所有授权范围需求与上述检查已逐项核对 | 代码交付无待处理阻断，真实环境验收明确留给用户 | None |
### 12.6 Final verdict
PASS — Ready for user review

执行例外：用户直接授权生成实施顺序并执行，不再等候二次计划审批；单Step对应用户要求的一逻辑任务最多一提交。

### 实施交付与验证记录

2026-10-20 22:50 Asia/Shanghai：已完成授权范围内的代码、配置、文档与测试。最终命令：

```bash
./mvnw -B -ntp -f egon-cola-components/pom.xml -pl :egon-cola-component-dynamic-thread-pool-starter,:egon-cola-component-dynamic-thread-pool-admin,:egon-cola-component-dynamic-thread-pool-test -am package
```

进程退出码0，BUILD SUCCESS（/tmp/egon-dtp-delivery-package.log）。DTP starter 107项、admin 34项、样例模块实际执行4项，共145项执行成功，失败0、错误0。样例模块另有17项依赖真实运行环境的原有用例跳过，未启动应用、真实Redis、Web监听端口或浏览器；MockMvc仅创建隔离测试Context。已构建starter/admin/test jar；未提交target、测试日志或生成mapper源码。

兼容核对：原任务接口/包装、旧构造、Registry插入顺序/不可修改集合、Bean别名、enum/Response、旧setter的void描述符、旧自动配置工厂签名均有编译或回归证据。queueCapacity缺失时不向Redis输出新字段，已用实际codec mapper验证。

实施澄清：MapStruct API在common-core中仅为test作用域，因此在获准的MapStruct能力内给admin补了同版本编译API与processor；生成converter采用AnnotateWith建立命名Spring Bean，不增加中间层。原样例配置保留以兼容无starter/无Redis的轻量Context；推荐工厂使用方式在中英文README与工厂Bean测试中给出。Admin OpenAPI测试使用独立控制台日志配置，避免生成应用data目录。原Spring映射未指定produces，生成OAS保留其通配媒体类型；运行JSON形状由MockMvc验证，不为文档收紧既有内容协商。

观测边界：JDK CompletableFuture.AsynchronousCompletionTask不公开stage结果；已回归验证仅记录提交/耗时并标记观察不完整，绝不把内部Runnable返回冒称业务成功。完整业务终态可使用受控ExecutorService.submit。该边界不改变任何Future/stage异常与取消语义。

### 最终逐规则审计

| Literal rule | Applicability | Status | Final diff/path/symbol evidence | Final test/static evidence | Finding | Required action/exception |
| --- | --- | --- | --- | --- | --- | --- |
| Rule 1 | Applicable | PASS | ThreadPoolCreateCommand、ThreadManage、原类型兼容清单 | 最终Maven package及对应测试/静态检查 | 新类型语义清楚，旧公开命名未强改 | None |
| Rule 2 | Applicable | PASS | 真实Provider、Create/PlatformResize/VirtualLimit组；Properties/MockMvc/ResizeSafety测试 | 最终Maven package及对应测试/静态检查 | 原生约束与锁内状态校验在实际入口执行 | None |
| Rule 3 | Applicable | PASS | CreateCommand与新Properties使用Lombok基线；旧setter保留void；内部状态仅私有 | 最终Maven package及对应测试/静态检查 | 无无意义父类、无公共POJO膨胀 | None |
| Rule 4 | Applicable | PASS | Factory/BPP Context、Autowired/Qualifier反射测试、命名mapper | 最终Maven package及对应测试/静态检查 | 无创建循环、原类型与别名保持、关闭归属单一 | None |
| Rule 5 | Applicable | PASS | JDK queue/locks/java.time、原Guava Cache | 最终Maven package及对应测试/静态检查 | 无新增工具框架/Utils复制 | None |
| Rule 6 | Applicable | PASS | OpenAPI/MockMvc/Redis codec测试，queueCapacity NON_NULL | 最终Maven package及对应测试/静态检查 | 原信封/enum线值与缺失字段兼容 | None |
| Rule 7 | Applicable | PASS | 八个profile键检查、生产docs关闭、Properties绑定与负例测试 | 最终Maven package及对应测试/静态检查 | 新增核心键一致且默认告警关闭 | None |
| Rule 9 | Applicable | PASS | ThreadManage Facade、原Adapter、Dtp装饰/FutureTask扩展 | 最终Maven package及对应测试/静态检查 | 模式直接对应三种执行器与异步结果差异 | None |
| Rule 10 | Applicable | PASS | nanoTime/Duration与原Instant，Timer/阈值和codec测试 | 最终Maven package及对应测试/静态检查 | 无java.util.Date/Calendar/SimpleDateFormat新增 | None |
| Rule 11 | Applicable | PASS | DTP既有目录与A→S/T→S；无SQL/UI变化 | 最终Maven package及对应测试/静态检查 | 用户批准保留基础设施结构 | None |

### 最终需求核对

| Requirement | Status | Code evidence | Validation evidence | Runtime boundary |
| --- | --- | --- | --- | --- |
| REQ-001 | Satisfied | ThreadManage、Registry、BPP | 并发创建/别名/Context/归属 | 本轮离线授权；真实Redis/部署/压测未执行 |
| REQ-002 | Satisfied | 原任务包装/原构造/default能力/Redis可选字段 | 原契约+旧setter+原Factory签名+插入顺序 | 本轮离线授权；真实Redis/部署/压测未执行 |
| REQ-003 | Satisfied | Scope捕获恢复与新后台根上下文 | 原7项Trace回归+新CallerRuns/预包装 | 本轮离线授权；真实Redis/部署/压测未执行 |
| REQ-004 | Satisfied | Observer、MeterBinder、报告告警 | 真实结果、取消竞态30轮、Timer、告警冷却/可选类 | 本轮离线授权；真实Redis/部署/压测未执行 |
| REQ-005 | Satisfied | ResizeSupport、队列、三Adapter | 低于backlog零变更、唤醒、旧core0、虚拟缩容 | 本轮离线授权；真实Redis/部署/压测未执行 |
| REQ-006 | Satisfied | 目标校验/有限去重/独立审计与报告 | 并发24次重复、冲突ID、非目标、审计失败 | 本轮离线授权；真实Redis/部署/压测未执行 |
| REQ-007 | Satisfied | 归属关闭、无真实服务、限定改动 | compile/test/package、git diff检查 | 本轮离线授权；真实Redis/部署/压测未执行 |

单一逻辑任务验证完成，提交范围按本计划精确文件列表核对；最终提交标识由Git与交付回复记录。未创建scripts/work验证脚本，不存在待处置harness。代码实施/离线验收符合Spec；线上投递、采集与长时间压测没有被冒称为已验证。
