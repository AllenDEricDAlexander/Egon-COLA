# DTP上下文与异常统一处理实施计划

| Field | Value |
| --- | --- |
| Document | `2026-10-21-10-56-dtp-context-exception-implementation.md` |
| Template Version | `5` |
| Status | `Completed` |
| Created | 2026-10-21 10:56 Asia/Shanghai |
| Updated | 2026-10-21 13:00 Asia/Shanghai |
| Owner | mario |
| Repository | Egon-COLA |
| Scope | 已批准完整DTP Spec、必要common-core及六种源配置 |
| Source Requirement | 用户批准完整方案及context-propagation依赖，要求直接实施、兼容/编译/测试、不启动项目 |
| Baseline Revision | main / 4ef1cf380；两份本任务草案未跟踪，无其他dirty |
| Implements Spec | [完整Spec](../spec/2026-10-21-10-10-dtp-context-and-exception-management.md) |
| Spec Status | Implemented |
| Spec Revision | 设计2026-10-21 10:30，实施2026-10-21 13:00 Asia/Shanghai |
| Effective Specs | [完整Spec](../spec/2026-10-21-10-10-dtp-context-and-exception-management.md)；[前置已实现Spec](../spec/2026-10-20-20-53-dtp-thread-manage-observability.md) §4/§6/§7兼容和治理基础 |
| Depends On Plans | None |
| Supersedes | None |
| Superseded By | None |
| Related Plans | None |

## 1. Summary

按用户直接实施授权完成一个逻辑任务，使用一个Step、一个聚焦提交；内部按测试、技术协议、上下文/终态、装配/生产者、回归和审计顺序工作。用户AGENTS的每逻辑任务至多一个提交优先于技能的多Step默认提交方式。不另设等待Plan确认，用户已明确批准完整设计和实施。

## 2. Target Spec and Effective Design

主Spec为上表完整Spec，用户本轮“就按这个完整方案实施，context-propagation依赖增补也同意”构成精确设计/依赖/执行授权。上一份异常Review草案被完整Spec §1–§20替代。前置Spec仅保留目录/Bean/API/队列/调容/Redis基础；不重写其批准内容。

## 3. Effective Requirements and Acceptance

| Requirement | Source Spec section | Effective statement | Observable acceptance | Implementation impact |
| --- | --- | --- | --- | --- |
| REQ-001 | 完整Spec §4 | 上下文生命周期由框架处理 | 裸Runnable/Callable在托管入口逐次捕获；普通/异常/嵌套/CallerRuns执行后恢复 | Step 1相关文件和TEST用例 |
| REQ-002 | 完整Spec §4 | 默认MDC/Trace和旧捕获时点兼容 | MDC全量快照、tenant/user延续；旧Dtp/TraceRoute捕获时点和空Trace不改 | Step 1相关文件和TEST用例 |
| REQ-003 | 完整Spec §4 | 标准扩展点支持自定义ThreadLocal | 一次声明Accessor Bean即可传递；无值时清空本任务可见值，结束恢复；未登记的不处理 | Step 1相关文件和TEST用例 |
| REQ-004 | 完整Spec §4 | 安装失败/清理失败有明确边界 | 安装失败不执行业务并回退已安装项；清理尝试全部、MDC独立恢复，技术失败可诊断 | Step 1相关文件和TEST用例 |
| REQ-005 | 完整Spec §4 | 任务异常在组件统一处理 | execute/Future/virtual/void Async/受控root CF真实失败各一次报告，无get也记录 | Step 1相关文件和TEST用例 |
| REQ-006 | 完整Spec §4 | CF后续切换上下文时点明确 | 绑定Executor在创建时捕获，外部线程完成上游也用绑定上下文；每次提交是独立任务token | Step 1相关文件和TEST用例 |
| REQ-007 | 完整Spec §4 | 标准Spring异步接入简单且不覆盖用户配置 | 缺省Boot池装饰器自动接入；六处Configurer可选委托；保留自定义池/装饰器/handler | Step 1相关文件和TEST用例 |
| REQ-008 | 完整Spec §4 | 原执行、监控和调容兼容 | 原API/Bean/拒绝/取消/虚拟许可/指标/Redis wire保留；调容回归无丢任务 | Step 1相关文件和TEST用例 |
| REQ-009 | 完整Spec §4 | 最小安全依赖和有界诊断 | 复用Trace/ExceptionLogger/标准SPI；无全局TL扫描/事务复制/高基数metric | Step 1相关文件和TEST用例 |
| REQ-010 | 完整Spec §4 | 本轮完整设计，后续隔离验证 | 单份完整方案包含接口/文件/错误/测试；仅文档校验，不运行项目 | Step 1相关文件和TEST用例 |

## 4. Implementation Strategy and Dependency Order

### 4.1 Ordered strategy

原TraceContext/Registry/Decorator/Observer继续拥有原职责。仅添加已批准标准Accessor协议、一个安全组装Manager、一个失败策略；不更改业务/数据库/agent/Reactive全局状态。

### 4.2 Test-first strategy

首先只增加旧submit入口的安全错误日志测试，运行RED确认现有仅WARN/无安全堆栈。随后每组新测试先定义契约，再实现对应类型/行为和装配；新增API的编译依赖在运行GREEN前补齐，保留具体RED原因。最后运行全受影响模块package、真实Async代理及源工程验证。

### 4.3 Sequential and parallel boundaries

| Step | Depends on | May run in parallel with | Must not overlap with | Reason |
| --- | --- | --- | --- | --- |
| Step 1 | 无 | 只读独立命令可批量 | 其他写入/子代理 | 单逻辑任务、单提交、同一Observer/Registry状态 |

### 4.4 Commit boundaries

按用户AGENTS仅一个完成后聚焦提交，不提交半完成架构；不push/merge/启动应用。单Step覆盖同一任务上下文—异常—终态集成所需跨模块编译和兼容验证。

### 4.5 Spec Simplicity and Implementation-necessity Audit

| Spec element | Spec necessity verdict/section | Current repository evidence | Direct/reuse alternative | Interaction/implementation cost | Plan decision |
| --- | --- | --- | --- | --- | --- |
| ContextManager/BO | §7.0 Add | TraceContext.Scope、Micrometer1.1.4源码 | 单MDC不足额外TL，默认Scope无逐项故障保证 | 一个本地登记集和线程内Scope、O(n)、无网络 | Implement |
| ExceptionLogger/Handler | §7.0 Add | 公共脱敏/分类、原Observer终态 | 不新造日志/任务框架 | 单SPI、原token | Implement |
| CF/绑定视图 | §7.0 Add | ManagedExecutor.taskObserver默认已存在 | 不扩展ManagedExecutor接口 | 三个ThreadManage新方法 | Implement |
| 原治理/adapter | 前置Spec Keep | 原ResizeSupport/Registry/三adapter | Observer集中扩展即可覆盖原适配入口 | 无重复改写 | Already exists |

### 4.6 Change-unit Dependency Matrix

| Change unit | Requirements | Proof/RED point | Compile/runtime prerequisites | Produces | Consumers/unblocks | Owning Step |
| --- | --- | --- | --- | --- | --- | --- |
| 默认异常日志 | REQ-005/009 | 旧submit日志RED | 原JUnit/Logback/Observer | 安全Logger重载/Handler | Observer/Async | Step 1 |
| 上下文 | REQ-001..004 | Manager故障/恢复测试 | 已批准SPI+BO | capture/open/Scope | Observer/Decorator/CF | Step 1 |
| 真实终态与绑定 | REQ-005/006/008 | root/外部完成/取消 | Observer+ContextManager | 原root、局部Executor | ThreadManage/Async | Step 1 |
| 装配/兼容 | REQ-007..010 | Boot/Async真实代理 | 原条件配置/六源工程 | 缺省Decorator/可选委托 | 业务普通入口 | Step 1 |

### 4.7 Java, Spring, and Egon-COLA Implementation Standards

| Concern | Current repository evidence | Effective Spec decision | Planned implementation consequence | Owning Steps/checks |
| --- | --- | --- | --- | --- |
| Architecture profile | 现有component包、精确六种archetype | 保留 | 不新增层/模块、维持生产者 | Step 1 MC-ARCH-001 |
| Reuse/capability | TraceContext/ExceptionLogger/原Observer/标准Accessor | 原地扩展 | 不复制TTL/不全局扫描 | Step 1 MC-REUSE-001 |
| Naming/model/validation/conversion | 技术BO、ValidationUtils | class完整Lombok/原生约束/Report组 | 无业务角色转换或新Converter | Step 1 MC-MODEL-001 |
| Bean/logging/util/JSON/time/config | Qualifier/Lazy、八profile、java.time | 显式名称、Lombok@Autowired、独立预算 | 运行期Provider延迟，配置结构一致 | Step 1 MC-BEAN-001 |
| Business variation/pattern | 装饰/观察和标准Accessor | 组合/策略 | 无业务重试/事务转移 | Step 1 MC-PATTERN-001 |

#### Capability reuse ledger

| Need | Candidates inspected | Exact evidence | Fit/gap | Decision | Added dependency/custom code | Owning Step/check |
| --- | --- | --- | --- | --- | --- | --- |
| 默认MDC | JDK/Trace Scope | common-trace TraceContext | 足够 | Reuse | None | Step 1 MC-REUSE-001 |
| 自定义TL | Micrometer/Spring Decorator/bytecode Carrier | 完整Spec EVD-007/008/012 | 标准SPI适合，故障组装需补齐 | approved addition | io.micrometer:context-propagation，沿现BOM版本 | Step 1 MC-DEP-001 |
| 终态/日志 | 原Observer/Future/ExceptionLogger | 完整Spec EVD-005/010 | 有基础，新增统一报告 | Reuse | 同目录少量行为/BO | Step 1 MC-REUSE-001 |

### 4.8 User-mandated Java Rule Implementation Matrix

| Literal rule | Spec source | Repository evidence | Exact files and order | Pseudocode obligations | Validation gate | Steps | Status/blocker |
| --- | --- | --- | --- | --- | --- | --- | --- |
| Rule 1 | 完整Spec §6.2 | §4.7现有证据 | §5/§7顺序 | BO/Handler/Manager/Exception明确角色，禁止歧义载体 | 相关聚焦测试/编译和最终人工检查 | Step 1 | PASS |
| Rule 2 | 完整Spec §6.2 | §4.7现有证据 | §5/§7顺序 | 原生@NotNull/@Valid/Report组；非代理ValidationUtils；key边界guard | 相关聚焦测试/编译和最终人工检查 | Step 1 | PASS |
| Rule 3 | 完整Spec §6.2 | §4.7现有证据 | §5/§7顺序 | 普通BO完整Lombok class，集合防御复制，Throwable/value不toString；无新转换层 | 相关聚焦测试/编译和最终人工检查 | Step 1 | PASS |
| Rule 4 | 完整Spec §6.2 | §4.7现有证据 | §5/§7顺序 | Slf4j/显式Bean名/final+Qualifier/RequiredArgsConstructor(onConstructor_=Autowired)，保留旧构造 | 相关聚焦测试/编译和最终人工检查 | Step 1 | PASS |
| Rule 5 | 完整Spec §6.2 | §4.7现有证据 | §5/§7顺序 | JDK/现有组件和已批准协议，不加工具库 | 相关聚焦测试/编译和最终人工检查 | Step 1 | PASS |
| Rule 6 | 完整Spec §6.2 | §4.7现有证据 | §5/§7顺序 | 内存BO不序列化，原Jackson wire不变 | 相关聚焦测试/编译和最终人工检查 | Step 1 | PASS |
| Rule 7 | 完整Spec §6.2 | §4.7现有证据 | §5/§7顺序 | 八个profile的五新键同结构 | 相关聚焦测试/编译和最终人工检查 | Step 1 | PASS |
| Rule 9 | 完整Spec §6.2 | §4.7现有证据 | §5/§7顺序 | 标准Accessor策略/组合、原Decorator/Observer和单Handler | 相关聚焦测试/编译和最终人工检查 | Step 1 | PASS |
| Rule 10 | 完整Spec §6.2 | §4.7现有证据 | §5/§7顺序 | Instant UTC Z/Duration正值/nanoTime差值 | 相关聚焦测试/编译和最终人工检查 | Step 1 | PASS |
| Rule 11 | 完整Spec §6.2 | §4.7现有证据 | §5/§7顺序 | 现有component与精确六种源树，不加层/SQL | 相关聚焦测试/编译和最终人工检查 | Step 1 | PASS |

### 4.9 Time semantics and conversion contract

Time contract: Required

| Time ID | Fields/symbols | Semantics | Java/runtime type | Storage policy | Wire policy | Business zone source | Display zone source | Boundary / DST rules | Precision / Clock | Compatibility | Verification |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| TIME-001 | FailureBO.observedAt | Occurred instant | Instant | Not persisted | Internal | Not applicable: 技术观察瞬间 | Not applicable: 无UI | 诊断文本ISO UTC Z；不是业务发生时间 | Instant.now，保留小数秒 | 不改既有审计时间 | Step 1 TEST-021 |
| TIME-002 | logInterval/等待/执行/Context耗时 | Duration | Duration/long纳秒差 | Not persisted | Internal | Not applicable: 单调相对时间 | Not applicable: 无UI | 日历/DST不参与阈值 | nanoTime差值；Duration正值/转换范围校验 | 原report/alarm阈值不改 | Step 1 TEST-017/021 |


## 5. Change File Tree

| Operation | Path | Current evidence/symbol | Final symbols/state | Responsibility | Step | Requirements | Validation owner |
| --- | --- | --- | --- | --- | --- | --- | --- |
| CREATE | `egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/test/java/top/egon/cola/component/dtp/context/DtpTaskExceptionHandlingTest.java` | 当前存在/新增同包职责，Spec §8/§14 | 先定义旧submit入口自动报告安全系统异常的RED测试 | 先定义旧submit入口自动报告安全系统异常的RED测试 | Step 1 | REQ-001, REQ-002, REQ-003, REQ-004, REQ-005, REQ-006, REQ-007, REQ-008, REQ-009, REQ-010 | DTP focused/module tests |
| MODIFY | `egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/pom.xml` | 当前存在/新增同包职责，Spec §8/§14 | 显式声明已批准标准传播协议 | 显式声明已批准标准传播协议 | Step 1 | REQ-001, REQ-002, REQ-003, REQ-004, REQ-005, REQ-006, REQ-007, REQ-008, REQ-009, REQ-010 | dependency:tree/package |
| CREATE | `egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/main/java/top/egon/cola/component/dtp/context/DtpTaskContextBO.java` | 当前存在/新增同包职责，Spec §8/§14 | 技术快照载体 | 技术快照载体 | Step 1 | REQ-001, REQ-002, REQ-003, REQ-004, REQ-005, REQ-006, REQ-007, REQ-008, REQ-009, REQ-010 | DTP focused/module tests |
| CREATE | `egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/main/java/top/egon/cola/component/dtp/exception/DtpContextPropagationException.java` | 当前存在/新增同包职责，Spec §8/§14 | 上下文捕获/安装技术错误 | 上下文捕获/安装技术错误 | Step 1 | REQ-001, REQ-002, REQ-003, REQ-004, REQ-005, REQ-006, REQ-007, REQ-008, REQ-009, REQ-010 | DTP focused/module tests |
| CREATE | `egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/main/java/top/egon/cola/component/dtp/context/DtpTaskContextManager.java` | 当前存在/新增同包职责，Spec §8/§14 | 标准Accessor登记及安全Scope | 标准Accessor登记及安全Scope | Step 1 | REQ-001, REQ-002, REQ-003, REQ-004, REQ-005, REQ-006, REQ-007, REQ-008, REQ-009, REQ-010 | DTP focused/module tests |
| CREATE | `egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/test/java/top/egon/cola/component/dtp/context/DtpTaskContextManagerTest.java` | 当前存在/新增同包职责，Spec §8/§14 | 隔离快照及坏Accessor故障验证 | 隔离快照及坏Accessor故障验证 | Step 1 | REQ-001, REQ-002, REQ-003, REQ-004, REQ-005, REQ-006, REQ-007, REQ-008, REQ-009, REQ-010 | DTP focused/module tests |
| MODIFY | `egon-cola-components/egon-cola-component-common/egon-cola-component-common-core/src/main/java/top/egon/cola/component/common/core/exception/ExceptionLogger.java` | 当前存在/新增同包职责，Spec §8/§14 | 兼容安全metadata重载 | 兼容安全metadata重载 | Step 1 | REQ-001, REQ-002, REQ-003, REQ-004, REQ-005, REQ-006, REQ-007, REQ-008, REQ-009, REQ-010 | ExceptionClassificationTest |
| MODIFY | `egon-cola-components/egon-cola-component-common/egon-cola-component-common-core/src/test/java/top/egon/cola/component/common/exception/ExceptionClassificationTest.java` | 当前存在/新增同包职责，Spec §8/§14 | 公共日志重载和原规则回归 | 公共日志重载和原规则回归 | Step 1 | REQ-001, REQ-002, REQ-003, REQ-004, REQ-005, REQ-006, REQ-007, REQ-008, REQ-009, REQ-010 | ExceptionClassificationTest |
| CREATE | `egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/main/java/top/egon/cola/component/dtp/exception/DtpTaskFailureBO.java` | 当前存在/新增同包职责，Spec §8/§14 | 失败处理策略输入 | 失败处理策略输入 | Step 1 | REQ-001, REQ-002, REQ-003, REQ-004, REQ-005, REQ-006, REQ-007, REQ-008, REQ-009, REQ-010 | DTP focused/module tests |
| CREATE | `egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/main/java/top/egon/cola/component/dtp/exception/DtpTaskExceptionHandler.java` | 当前存在/新增同包职责，Spec §8/§14 | 单个可替换异常策略 | 单个可替换异常策略 | Step 1 | REQ-001, REQ-002, REQ-003, REQ-004, REQ-005, REQ-006, REQ-007, REQ-008, REQ-009, REQ-010 | DTP focused/module tests |
| CREATE | `egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/main/java/top/egon/cola/component/dtp/exception/LoggingDtpTaskExceptionHandler.java` | 当前存在/新增同包职责，Spec §8/§14 | 公共Logger默认策略 | 公共Logger默认策略 | Step 1 | REQ-001, REQ-002, REQ-003, REQ-004, REQ-005, REQ-006, REQ-007, REQ-008, REQ-009, REQ-010 | DTP focused/module tests |
| MODIFY | `egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/main/java/top/egon/cola/component/dtp/config/DynamicThreadPoolAutoProperties.java` | 当前存在/新增同包职责，Spec §8/§14 | 传播/异常配置及Duration校验 | 传播/异常配置及Duration校验 | Step 1 | REQ-001, REQ-002, REQ-003, REQ-004, REQ-005, REQ-006, REQ-007, REQ-008, REQ-009, REQ-010 | DTP focused/module tests |
| MODIFY | `egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/main/java/top/egon/cola/component/dtp/context/DtpTaskObserver.java` | 当前存在/新增同包职责，Spec §8/§14 | 复用单token连接上下文/异常/真实root/UC | 复用单token连接上下文/异常/真实root/UC | Step 1 | REQ-001, REQ-002, REQ-003, REQ-004, REQ-005, REQ-006, REQ-007, REQ-008, REQ-009, REQ-010 | DTP focused/module tests |
| CREATE | `egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/test/java/top/egon/cola/component/dtp/executor/ThreadManageCompletableFutureTest.java` | 当前存在/新增同包职责，Spec §8/§14 | 根阶段与绑定上下文CF验证 | 根阶段与绑定上下文CF验证 | Step 1 | REQ-001, REQ-002, REQ-003, REQ-004, REQ-005, REQ-006, REQ-007, REQ-008, REQ-009, REQ-010 | DTP focused/module tests |
| MODIFY | `egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/main/java/top/egon/cola/component/dtp/executor/ManagedExecutorRegistry.java` | 当前存在/新增同包职责，Spec §8/§14 | 保持唯一目录、延迟传递策略 | 保持唯一目录、延迟传递策略 | Step 1 | REQ-001, REQ-002, REQ-003, REQ-004, REQ-005, REQ-006, REQ-007, REQ-008, REQ-009, REQ-010 | DTP focused/module tests |
| MODIFY | `egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/main/java/top/egon/cola/component/dtp/executor/ThreadManage.java` | 当前存在/新增同包职责，Spec §8/§14 | 三个便捷公开入口 | 三个便捷公开入口 | Step 1 | REQ-001, REQ-002, REQ-003, REQ-004, REQ-005, REQ-006, REQ-007, REQ-008, REQ-009, REQ-010 | DTP focused/module tests |
| MODIFY | `egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/main/java/top/egon/cola/component/dtp/executor/managed/DtpThreadPoolTaskExecutor.java` | 当前存在/新增同包职责，Spec §8/§14 | 自建工厂兜底与Spring CF根阶段 | 自建工厂兜底与Spring CF根阶段 | Step 1 | REQ-001, REQ-002, REQ-003, REQ-004, REQ-005, REQ-006, REQ-007, REQ-008, REQ-009, REQ-010 | DTP focused/module tests |
| MODIFY | `egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/main/java/top/egon/cola/component/dtp/executor/virtual/BoundedVirtualThreadExecutor.java` | 当前存在/新增同包职责，Spec §8/§14 | 虚拟工厂同一观察器兜底 | 虚拟工厂同一观察器兜底 | Step 1 | REQ-001, REQ-002, REQ-003, REQ-004, REQ-005, REQ-006, REQ-007, REQ-008, REQ-009, REQ-010 | DTP focused/module tests |
| MODIFY | `egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/main/java/top/egon/cola/component/dtp/context/DtpTaskDecorator.java` | 当前存在/新增同包职责，Spec §8/§14 | 原无参兼容及完整上下文装饰 | 原无参兼容及完整上下文装饰 | Step 1 | REQ-001, REQ-002, REQ-003, REQ-004, REQ-005, REQ-006, REQ-007, REQ-008, REQ-009, REQ-010 | DTP focused/module tests |
| CREATE | `egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/main/java/top/egon/cola/component/dtp/exception/DtpAsyncUncaughtExceptionHandler.java` | 当前存在/新增同包职责，Spec §8/§14 | 真实void异步失败桥 | 真实void异步失败桥 | Step 1 | REQ-001, REQ-002, REQ-003, REQ-004, REQ-005, REQ-006, REQ-007, REQ-008, REQ-009, REQ-010 | DTP focused/module tests |
| CREATE | `egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/main/java/top/egon/cola/component/dtp/config/DtpAsyncConfigurer.java` | 当前存在/新增同包职责，Spec §8/§14 | 已有Async启用下条件退让配置 | 已有Async启用下条件退让配置 | Step 1 | REQ-001, REQ-002, REQ-003, REQ-004, REQ-005, REQ-006, REQ-007, REQ-008, REQ-009, REQ-010 | DTP focused/module tests |
| CREATE | `egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/main/java/top/egon/cola/component/dtp/config/DtpTaskContextAutoConfiguration.java` | 当前存在/新增同包职责，Spec §8/§14 | Boot builder之前缺省装饰器 | Boot builder之前缺省装饰器 | Step 1 | REQ-001, REQ-002, REQ-003, REQ-004, REQ-005, REQ-006, REQ-007, REQ-008, REQ-009, REQ-010 | DTP focused/module tests |
| MODIFY | `egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/main/java/top/egon/cola/component/dtp/config/DynamicThreadPoolAutoConfig.java` | 当前存在/新增同包职责，Spec §8/§14 | 增强Registry装配且保留旧工厂调用 | 增强Registry装配且保留旧工厂调用 | Step 1 | REQ-001, REQ-002, REQ-003, REQ-004, REQ-005, REQ-006, REQ-007, REQ-008, REQ-009, REQ-010 | DTP focused/module tests |
| MODIFY | `egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` | 当前存在/新增同包职责，Spec §8/§14 | 注册新增自动配置 | 注册新增自动配置 | Step 1 | REQ-001, REQ-002, REQ-003, REQ-004, REQ-005, REQ-006, REQ-007, REQ-008, REQ-009, REQ-010 | DTP focused/module tests |
| MODIFY | `egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/main/java/top/egon/cola/component/dtp/metrics/DtpMeterBinder.java` | 当前存在/新增同包职责，Spec §8/§14 | 保持原指标并增加低基数技术失败 | 保持原指标并增加低基数技术失败 | Step 1 | REQ-001, REQ-002, REQ-003, REQ-004, REQ-005, REQ-006, REQ-007, REQ-008, REQ-009, REQ-010 | DTP focused/module tests |
| CREATE | `egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/test/java/top/egon/cola/component/dtp/config/DtpTaskContextAutoConfigurationTest.java` | 当前存在/新增同包职责，Spec §8/§14 | 缺省Boot装饰器/用户配置/配置开关 | 缺省Boot装饰器/用户配置/配置开关 | Step 1 | REQ-001, REQ-002, REQ-003, REQ-004, REQ-005, REQ-006, REQ-007, REQ-008, REQ-009, REQ-010 | DTP focused/module tests |
| CREATE | `egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/test/java/top/egon/cola/component/dtp/config/DtpAsyncIntegrationTest.java` | 当前存在/新增同包职责，Spec §8/§14 | 真实Async代理终态及上下文 | 真实Async代理终态及上下文 | Step 1 | REQ-001, REQ-002, REQ-003, REQ-004, REQ-005, REQ-006, REQ-007, REQ-008, REQ-009, REQ-010 | DTP focused/module tests |
| MODIFY | `egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/test/java/top/egon/cola/component/dtp/context/DtpContextPropagationTest.java` | 当前存在/新增同包职责，Spec §8/§14 | 旧捕获时点/多线程/CallerRuns回归 | 旧捕获时点/多线程/CallerRuns回归 | Step 1 | REQ-001, REQ-002, REQ-003, REQ-004, REQ-005, REQ-006, REQ-007, REQ-008, REQ-009, REQ-010 | DTP focused/module tests |
| MODIFY | `egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/test/java/top/egon/cola/component/dtp/context/DtpTaskObserverTest.java` | 当前存在/新增同包职责，Spec §8/§14 | 取消/批量/不透明阶段兼容 | 取消/批量/不透明阶段兼容 | Step 1 | REQ-001, REQ-002, REQ-003, REQ-004, REQ-005, REQ-006, REQ-007, REQ-008, REQ-009, REQ-010 | DTP focused/module tests |
| MODIFY | `egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/test/java/top/egon/cola/component/dtp/executor/ThreadManageTest.java` | 当前存在/新增同包职责，Spec §8/§14 | 旧API/工厂/调容及新增入口兼容 | 旧API/工厂/调容及新增入口兼容 | Step 1 | REQ-001, REQ-002, REQ-003, REQ-004, REQ-005, REQ-006, REQ-007, REQ-008, REQ-009, REQ-010 | DTP focused/module tests |
| MODIFY | `egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/test/java/top/egon/cola/component/dtp/executor/virtual/BoundedVirtualThreadExecutorTest.java` | 当前存在/新增同包职责，Spec §8/§14 | 虚拟上下文/许可并发验证 | 虚拟上下文/许可并发验证 | Step 1 | REQ-001, REQ-002, REQ-003, REQ-004, REQ-005, REQ-006, REQ-007, REQ-008, REQ-009, REQ-010 | DTP focused/module tests |
| MODIFY | `egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/test/java/top/egon/cola/component/dtp/config/DynamicThreadPoolAutoPropertiesTest.java` | 当前存在/新增同包职责，Spec §8/§14 | 新增配置绑定及profile一致 | 新增配置绑定及profile一致 | Step 1 | REQ-001, REQ-002, REQ-003, REQ-004, REQ-005, REQ-006, REQ-007, REQ-008, REQ-009, REQ-010 | DTP focused/module tests |
| MODIFY | `egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/test/java/top/egon/cola/component/dtp/config/DtpDependencyBoundaryTest.java` | 当前存在/新增同包职责，Spec §8/§14 | 依赖边界及公共类型兼容 | 依赖边界及公共类型兼容 | Step 1 | REQ-001, REQ-002, REQ-003, REQ-004, REQ-005, REQ-006, REQ-007, REQ-008, REQ-009, REQ-010 | DTP focused/module tests |
| MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/java/top/egon/cola/archetype/source/light/start/config/async/AsyncConfiguration.java` | 当前存在/新增同包职责，Spec §8/§14 | 标准源配置可选委托、原默认池保持 | 标准源配置可选委托、原默认池保持 | Step 1 | REQ-001, REQ-002, REQ-003, REQ-004, REQ-005, REQ-006, REQ-007, REQ-008, REQ-009, REQ-010 | DTP focused/module tests |
| MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-light/src/test/java/top/egon/cola/archetype/source/light/time/ApplicationExceptionClassificationTest.java` | 当前存在/新增同包职责，Spec §8/§14 | 生产者可选组件及构造兼容验证 | 生产者可选组件及构造兼容验证 | Step 1 | REQ-001, REQ-002, REQ-003, REQ-004, REQ-005, REQ-006, REQ-007, REQ-008, REQ-009, REQ-010 | source tests/archetype checks |
| MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-light-open/src/main/java/top/egon/cola/archetype/source/lightopen/start/config/async/AsyncConfiguration.java` | 当前存在/新增同包职责，Spec §8/§14 | 标准源配置可选委托、原默认池保持 | 标准源配置可选委托、原默认池保持 | Step 1 | REQ-001, REQ-002, REQ-003, REQ-004, REQ-005, REQ-006, REQ-007, REQ-008, REQ-009, REQ-010 | DTP focused/module tests |
| MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-light-open/src/test/java/top/egon/cola/archetype/source/lightopen/time/ApplicationExceptionClassificationTest.java` | 当前存在/新增同包职责，Spec §8/§14 | 生产者可选组件及构造兼容验证 | 生产者可选组件及构造兼容验证 | Step 1 | REQ-001, REQ-002, REQ-003, REQ-004, REQ-005, REQ-006, REQ-007, REQ-008, REQ-009, REQ-010 | source tests/archetype checks |
| MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-starter/src/main/java/top/egon/cola/archetype/source/service/starter/config/async/AsyncConfiguration.java` | 当前存在/新增同包职责，Spec §8/§14 | 标准源配置可选委托、原默认池保持 | 标准源配置可选委托、原默认池保持 | Step 1 | REQ-001, REQ-002, REQ-003, REQ-004, REQ-005, REQ-006, REQ-007, REQ-008, REQ-009, REQ-010 | DTP focused/module tests |
| MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-starter/src/test/java/top/egon/cola/archetype/source/service/time/ApplicationExceptionClassificationTest.java` | 当前存在/新增同包职责，Spec §8/§14 | 生产者可选组件及构造兼容验证 | 生产者可选组件及构造兼容验证 | Step 1 | REQ-001, REQ-002, REQ-003, REQ-004, REQ-005, REQ-006, REQ-007, REQ-008, REQ-009, REQ-010 | source tests/archetype checks |
| MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-service-open/egon-cola-source-service-open-starter/src/main/java/top/egon/cola/archetype/source/serviceopen/starter/config/async/AsyncConfiguration.java` | 当前存在/新增同包职责，Spec §8/§14 | 标准源配置可选委托、原默认池保持 | 标准源配置可选委托、原默认池保持 | Step 1 | REQ-001, REQ-002, REQ-003, REQ-004, REQ-005, REQ-006, REQ-007, REQ-008, REQ-009, REQ-010 | DTP focused/module tests |
| MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-service-open/egon-cola-source-service-open-starter/src/test/java/top/egon/cola/archetype/source/serviceopen/time/ApplicationExceptionClassificationTest.java` | 当前存在/新增同包职责，Spec §8/§14 | 生产者可选组件及构造兼容验证 | 生产者可选组件及构造兼容验证 | Step 1 | REQ-001, REQ-002, REQ-003, REQ-004, REQ-005, REQ-006, REQ-007, REQ-008, REQ-009, REQ-010 | source tests/archetype checks |
| MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-starter/src/main/java/top/egon/cola/archetype/source/web/starter/config/async/AsyncConfiguration.java` | 当前存在/新增同包职责，Spec §8/§14 | 标准源配置可选委托、原默认池保持 | 标准源配置可选委托、原默认池保持 | Step 1 | REQ-001, REQ-002, REQ-003, REQ-004, REQ-005, REQ-006, REQ-007, REQ-008, REQ-009, REQ-010 | DTP focused/module tests |
| MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-starter/src/test/java/top/egon/cola/archetype/source/web/time/ApplicationExceptionClassificationTest.java` | 当前存在/新增同包职责，Spec §8/§14 | 生产者可选组件及构造兼容验证 | 生产者可选组件及构造兼容验证 | Step 1 | REQ-001, REQ-002, REQ-003, REQ-004, REQ-005, REQ-006, REQ-007, REQ-008, REQ-009, REQ-010 | source tests/archetype checks |
| MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-web-open/egon-cola-source-web-open-starter/src/main/java/top/egon/cola/archetype/source/webopen/starter/config/async/AsyncConfiguration.java` | 当前存在/新增同包职责，Spec §8/§14 | 标准源配置可选委托、原默认池保持 | 标准源配置可选委托、原默认池保持 | Step 1 | REQ-001, REQ-002, REQ-003, REQ-004, REQ-005, REQ-006, REQ-007, REQ-008, REQ-009, REQ-010 | DTP focused/module tests |
| MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-web-open/egon-cola-source-web-open-starter/src/test/java/top/egon/cola/archetype/source/webopen/time/ApplicationExceptionClassificationTest.java` | 当前存在/新增同包职责，Spec §8/§14 | 生产者可选组件及构造兼容验证 | 生产者可选组件及构造兼容验证 | Step 1 | REQ-001, REQ-002, REQ-003, REQ-004, REQ-005, REQ-006, REQ-007, REQ-008, REQ-009, REQ-010 | source tests/archetype checks |
| MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-light-open/lombok.config` | 当前存在/新增同包职责，Spec §8/§14 | 构造参数Qualifier/Lazy传播 | 构造参数Qualifier/Lazy传播 | Step 1 | REQ-001, REQ-002, REQ-003, REQ-004, REQ-005, REQ-006, REQ-007, REQ-008, REQ-009, REQ-010 | reflection/source compilation |
| MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-service-open/lombok.config` | 当前存在/新增同包职责，Spec §8/§14 | 构造参数Qualifier/Lazy传播 | 构造参数Qualifier/Lazy传播 | Step 1 | REQ-001, REQ-002, REQ-003, REQ-004, REQ-005, REQ-006, REQ-007, REQ-008, REQ-009, REQ-010 | reflection/source compilation |
| MODIFY | `egon-cola-archetypes/source-projects/egon-cola-source-web-open/lombok.config` | 当前存在/新增同包职责，Spec §8/§14 | 构造参数Qualifier/Lazy传播 | 构造参数Qualifier/Lazy传播 | Step 1 | REQ-001, REQ-002, REQ-003, REQ-004, REQ-005, REQ-006, REQ-007, REQ-008, REQ-009, REQ-010 | reflection/source compilation |
| MODIFY | `egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-admin/src/main/resources/application.yml` | 当前存在/新增同包职责，Spec §8/§14 | 八profile配置键结构一致 | 八profile配置键结构一致 | Step 1 | REQ-001, REQ-002, REQ-003, REQ-004, REQ-005, REQ-006, REQ-007, REQ-008, REQ-009, REQ-010 | resource parity/package |
| MODIFY | `egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-admin/src/main/resources/application-dev.yml` | 当前存在/新增同包职责，Spec §8/§14 | 八profile配置键结构一致 | 八profile配置键结构一致 | Step 1 | REQ-001, REQ-002, REQ-003, REQ-004, REQ-005, REQ-006, REQ-007, REQ-008, REQ-009, REQ-010 | resource parity/package |
| MODIFY | `egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-admin/src/main/resources/application-test.yml` | 当前存在/新增同包职责，Spec §8/§14 | 八profile配置键结构一致 | 八profile配置键结构一致 | Step 1 | REQ-001, REQ-002, REQ-003, REQ-004, REQ-005, REQ-006, REQ-007, REQ-008, REQ-009, REQ-010 | resource parity/package |
| MODIFY | `egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-admin/src/main/resources/application-prod.yml` | 当前存在/新增同包职责，Spec §8/§14 | 八profile配置键结构一致 | 八profile配置键结构一致 | Step 1 | REQ-001, REQ-002, REQ-003, REQ-004, REQ-005, REQ-006, REQ-007, REQ-008, REQ-009, REQ-010 | resource parity/package |
| MODIFY | `egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-test/src/main/resources/application.yml` | 当前存在/新增同包职责，Spec §8/§14 | 八profile配置键结构一致 | 八profile配置键结构一致 | Step 1 | REQ-001, REQ-002, REQ-003, REQ-004, REQ-005, REQ-006, REQ-007, REQ-008, REQ-009, REQ-010 | resource parity/package |
| MODIFY | `egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-test/src/main/resources/application-dev.yml` | 当前存在/新增同包职责，Spec §8/§14 | 八profile配置键结构一致 | 八profile配置键结构一致 | Step 1 | REQ-001, REQ-002, REQ-003, REQ-004, REQ-005, REQ-006, REQ-007, REQ-008, REQ-009, REQ-010 | resource parity/package |
| MODIFY | `egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-test/src/main/resources/application-test.yml` | 当前存在/新增同包职责，Spec §8/§14 | 八profile配置键结构一致 | 八profile配置键结构一致 | Step 1 | REQ-001, REQ-002, REQ-003, REQ-004, REQ-005, REQ-006, REQ-007, REQ-008, REQ-009, REQ-010 | resource parity/package |
| MODIFY | `egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-test/src/main/resources/application-prod.yml` | 当前存在/新增同包职责，Spec §8/§14 | 八profile配置键结构一致 | 八profile配置键结构一致 | Step 1 | REQ-001, REQ-002, REQ-003, REQ-004, REQ-005, REQ-006, REQ-007, REQ-008, REQ-009, REQ-010 | resource parity/package |
| MODIFY | `egon-cola-components/egon-cola-component-dynamic-thread-pool/README.md` | 当前存在/新增同包职责，Spec §8/§14 | 业务简单接入、兼容及边界说明 | 业务简单接入、兼容及边界说明 | Step 1 | REQ-001, REQ-002, REQ-003, REQ-004, REQ-005, REQ-006, REQ-007, REQ-008, REQ-009, REQ-010 | doc review |
| MODIFY | `egon-cola-components/egon-cola-component-dynamic-thread-pool/README.zh-CN.md` | 当前存在/新增同包职责，Spec §8/§14 | 业务简单接入、兼容及边界说明 | 业务简单接入、兼容及边界说明 | Step 1 | REQ-001, REQ-002, REQ-003, REQ-004, REQ-005, REQ-006, REQ-007, REQ-008, REQ-009, REQ-010 | doc review |
| MODIFY | `docs/egon/spec/2026-10-21-10-10-dtp-context-and-exception-management.md` | 当前存在/新增同包职责，Spec §8/§14 | Accepted到Implemented及验证证据 | Accepted到Implemented及验证证据 | Step 1 | REQ-001, REQ-002, REQ-003, REQ-004, REQ-005, REQ-006, REQ-007, REQ-008, REQ-009, REQ-010 | Spec/Plan strict validators |
| MODIFY | `docs/egon/spec/2026-10-21-09-38-dtp-unified-task-exception-handling.md` | 当前存在/新增同包职责，Spec §8/§14 | 旧草案仅标Superseded | 旧草案仅标Superseded | Step 1 | REQ-001, REQ-002, REQ-003, REQ-004, REQ-005, REQ-006, REQ-007, REQ-008, REQ-009, REQ-010 | Spec/Plan strict validators |
| CREATE | `docs/egon/plan/2026-10-21-10-56-dtp-context-exception-implementation.md` | 当前存在/新增同包职责，Spec §8/§14 | 执行记录、单提交审计证据 | 执行记录、单提交审计证据 | Step 1 | REQ-001, REQ-002, REQ-003, REQ-004, REQ-005, REQ-006, REQ-007, REQ-008, REQ-009, REQ-010 | Spec/Plan strict validators |

| MODIFY | `egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/test/java/top/egon/cola/component/dtp/contract/RemainingComponentContractTest.java` | 原精确异常库存断言 | 验证唯一批准技术异常复用CommonException系统状态 | 保持原异常治理约束 | Step 1 | REQ-004/009 | 全starter回归 |

## 6. Prerequisites, Constraints, and Plan Clarifications

main基线4ef1cf380，初始仅两份本任务设计稿未跟踪。保留用户数据、原API和所有SQL；无外部环境或凭据需求。只使用已有mvnw、JUnit、生成脚本；不创建临时仓库脚本。

PLAN-CLAR-001：三个adapter、DtpObservedFutureTask及DtpContextAwareExecutorService已汇合到原Observer；若集中接入已完整覆盖，不为满足文件数量产生无语义修改。通过原测试及新增终态测试证明。
PLAN-CLAR-002：未知第三方能力复用已有taskObserver()默认null，不新增ManagedExecutor接口方法。
PLAN-CLAR-003：Context/UC carrier、日志预算、Scope、root bridge作为Spec已声明类的内部实现，不新增公开管理层。实际root在调度前构造，安装失败也必须使其异常完成。
PLAN-CLAR-004：Boot3.5.16源码默认注册applicationTaskExecutorAsyncConfigurer；普通解析仍返回null，force模式继续明确选择原applicationTaskExecutor，防止接入改变原选择。
PLAN-CLAR-005：普通第三方Spring Future仅通过TaskDecorator接入时无法合法设置内部异常原因；若安装失败则取消尚未执行业务的原Future并作安全技术诊断，避免永久等待。组件受控Future/root仍按原技术异常完成。对JDK opaque阶段不宣称可读取/改写隐藏结果。
PLAN-CLAR-006：旧RemainingComponentContractTest假设本模块无异常，新增已批准ContextPropagationException后改为精确库存，并要求继承现有CommonException/ResultCode.SYSTEM_ERROR；不放开任意异常体系。
依赖已由本轮用户精确批准；无需再次确认。源工程维护不触发示例删除。

## 7. Ordered File-by-file Implementation Steps

### Step 1 — 完整交付任务上下文与统一异常并保持原治理兼容

- Requirements: REQ-001, REQ-002, REQ-003, REQ-004, REQ-005, REQ-006, REQ-007, REQ-008, REQ-009, REQ-010
- Dependencies: None；前置ThreadManage已实现
- Baseline state: 4ef1cf380，Trace-only包装/Observer已有；默认只有有限WARN诊断
- Observable outcome: 普通任务在框架获得正确上下文及真实失败日志，旧API/治理保持
- End state: 全部批准接口、装配、配置、文档及隔离测试可用，无应用/Redis启动
- Test-first gate: Required — File 1旧submit安全ERROR/堆栈缺失，应先失败
- Manual Checks: MC-ARCH-001, MC-REUSE-001, MC-DEP-001, MC-NAME-001, MC-VALID-001, MC-MODEL-001, MC-CONVERT-001, MC-LOG-001, MC-BEAN-001, MC-UTIL-001, MC-JSON-001, MC-TIME-001, MC-CONFIG-001, MC-PATTERN-001, MC-SCOPE-001, MC-TEST-001, MC-BLOCKER-001
- Literal Rules: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11
- Ordered files:

#### File 1 — `CREATE egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/test/java/top/egon/cola/component/dtp/context/DtpTaskExceptionHandlingTest.java`

- Purpose: 先定义旧submit入口自动报告安全系统异常的RED测试。
- Symbols: DtpTaskExceptionHandlingTest；submit(() -> throw original)。
- Repository evidence: 完整Spec §8/§14的同包现有结构；当前文件尚无，按已批准同包角色创建。
- Dependencies and consumers: 同模块原调用链、ContextManager/Observer/配置/测试；common只向下依赖，生产者保持原模块。
- Why now: 首先锁定可编译旧API的RED行为。
- Contract/signature changes: submit(() -> throw original); await finished; assert ERROR event has redacted stack and saved traceId; assert future.get cause == original。
- Input/output and state mapping: 执行输入来自可信提交点；null/空值/原Trace时点/真实终态依完整Spec §7/§9/§10；无业务持久化变化。
- Error and edge behavior: 本文件按上述算法保留原异常/取消/拒绝；故障不暗改业务结果，不忽略恢复，不新增身份默认值。
- Standards impact: MC-SCOPE-001、MC-TEST-001及§4.7相关命名/校验/Bean/日志/配置检查。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11；具体强制约束见§4.8逐项矩阵，当前文件仅在其实际角色适用，不添加无意义框架。
- Implementation pseudocode:

```java
  submit(() ->
throw original)
  await finished
  assert ERROR event has redacted stack and saved traceId
  assert future.get cause == original
```

- Verification contribution: DTP focused/module tests；完整Spec TEST-001..023中本文件对应断言。
- After this file: RED用例已定义并运行，生产实现尚缺。

#### File 2 — `MODIFY egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/pom.xml`

- Purpose: 显式声明已批准标准传播协议。
- Symbols: pom；add dependency io.micrometer:context-propagation。
- Repository evidence: 完整Spec §8/§14的同包现有结构；当前文件存在，保留既有接口/风格。
- Dependencies and consumers: 同模块原调用链、ContextManager/Observer/配置/测试；common只向下依赖，生产者保持原模块。
- Why now: 依赖前面契约/测试和原实现，逐步连通同一集成任务。
- Contract/signature changes: add dependency io.micrometer:context-propagation; no version override; retain optional metrics boundary。
- Input/output and state mapping: 执行输入来自可信提交点；null/空值/原Trace时点/真实终态依完整Spec §7/§9/§10；无业务持久化变化。
- Error and edge behavior: 本文件按上述算法保留原异常/取消/拒绝；故障不暗改业务结果，不忽略恢复，不新增身份默认值。
- Standards impact: MC-SCOPE-001、MC-TEST-001及§4.7相关命名/校验/Bean/日志/配置检查。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11；具体强制约束见§4.8逐项矩阵，当前文件仅在其实际角色适用，不添加无意义框架。
- Implementation pseudocode:

```java
add dependency io.micrometer:context-propagation
  no version override
  retain optional metrics boundary
dependencyManagement remains Spring Boot / Micrometer BOM; do not pin or upgrade version
context-propagation is ordinary compile/transitive; micrometer-core remains optional
dependency:tree confirms context-propagation 1.1.4 and Spring 6.2.19 before packaging
```

- Verification contribution: dependency:tree/package；完整Spec TEST-001..023中本文件对应断言。
- After this file: 本文件责任具备，继续补齐本Step依赖后运行聚焦GREEN/模块门禁。

#### File 3 — `CREATE egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/main/java/top/egon/cola/component/dtp/context/DtpTaskContextBO.java`

- Purpose: 技术快照载体。
- Symbols: DtpTaskContextBO；class @Data @NoArgsConstructor @AllArgsConstructor @Accessors(chain=true) @Builder。
- Repository evidence: 完整Spec §8/§14的同包现有结构；当前文件尚无，按已批准同包角色创建。
- Dependencies and consumers: 同模块原调用链、ContextManager/Observer/配置/测试；common只向下依赖，生产者保持原模块。
- Why now: 依赖前面契约/测试和原实现，逐步连通同一集成任务。
- Contract/signature changes: class @Data @NoArgsConstructor @AllArgsConstructor @Accessors(chain=true) @Builder; @NotNull traceContext/registrationIdentity; defensive List<AccessorValueBO>; exclude values from toString。
- Input/output and state mapping: 执行输入来自可信提交点；null/空值/原Trace时点/真实终态依完整Spec §7/§9/§10；无业务持久化变化。
- Error and edge behavior: 本文件按上述算法保留原异常/取消/拒绝；故障不暗改业务结果，不忽略恢复，不新增身份默认值。
- Standards impact: MC-SCOPE-001、MC-TEST-001及§4.7相关命名/校验/Bean/日志/配置检查。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11；具体强制约束见§4.8逐项矩阵，当前文件仅在其实际角色适用，不添加无意义框架。
- Implementation pseudocode:

```java
  class @Data @NoArgsConstructor @AllArgsConstructor @Accessors(chain=true) @Builder
  @NotNull traceContext/registrationIdentity
  defensive List<AccessorValueBO>
  exclude values from toString
```

- Verification contribution: DTP focused/module tests；完整Spec TEST-001..023中本文件对应断言。
- After this file: 本文件责任具备，继续补齐本Step依赖后运行聚焦GREEN/模块门禁。

#### File 4 — `CREATE egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/main/java/top/egon/cola/component/dtp/exception/DtpContextPropagationException.java`

- Purpose: 上下文捕获/安装技术错误。
- Symbols: DtpContextPropagationException；extend CommonException with existing ResultCode.SYSTEM_ERROR。
- Repository evidence: 完整Spec §8/§14的同包现有结构；当前文件尚无，按已批准同包角色创建。
- Dependencies and consumers: 同模块原调用链、ContextManager/Observer/配置/测试；common只向下依赖，生产者保持原模块。
- Why now: 依赖前面契约/测试和原实现，逐步连通同一集成任务。
- Contract/signature changes: extend CommonException with existing ResultCode.SYSTEM_ERROR; retain phase/key/cause only; do not mutate business Throwable。
- Input/output and state mapping: 执行输入来自可信提交点；null/空值/原Trace时点/真实终态依完整Spec §7/§9/§10；无业务持久化变化。
- Error and edge behavior: 本文件按上述算法保留原异常/取消/拒绝；故障不暗改业务结果，不忽略恢复，不新增身份默认值。
- Standards impact: MC-SCOPE-001、MC-TEST-001及§4.7相关命名/校验/Bean/日志/配置检查。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11；具体强制约束见§4.8逐项矩阵，当前文件仅在其实际角色适用，不添加无意义框架。
- Implementation pseudocode:

```java
extend CommonException with existing ResultCode.SYSTEM_ERROR
  retain phase/key/cause only
  do not mutate business Throwable
constructor(phase,key,cause): keep cause identity and bounded safe key
super technical message contains phase/key only, never task data or original message
backup/install errors are thrown; restore errors only emit independent diagnostic
```

- Verification contribution: DTP focused/module tests；完整Spec TEST-001..023中本文件对应断言。
- After this file: 本文件责任具备，继续补齐本Step依赖后运行聚焦GREEN/模块门禁。

#### File 5 — `CREATE egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/main/java/top/egon/cola/component/dtp/context/DtpTaskContextManager.java`

- Purpose: 标准Accessor登记及安全Scope。
- Symbols: DtpTaskContextManager；validate qualified accessor keys。
- Repository evidence: 完整Spec §8/§14的同包现有结构；当前文件尚无，按已批准同包角色创建。
- Dependencies and consumers: 同模块原调用链、ContextManager/Observer/配置/测试；common只向下依赖，生产者保持原模块。
- Why now: 依赖前面契约/测试和原实现，逐步连通同一集成任务。
- Contract/signature changes: validate qualified accessor keys; capture immutable values; backup all; install or clear missing; rollback attempted in reverse; close all with isolated diagnostics and Trace.close last; scope thread-confined/idempotent。
- Input/output and state mapping: 执行输入来自可信提交点；null/空值/原Trace时点/真实终态依完整Spec §7/§9/§10；无业务持久化变化。
- Error and edge behavior: 本文件按上述算法保留原异常/取消/拒绝；故障不暗改业务结果，不忽略恢复，不新增身份默认值。
- Standards impact: MC-SCOPE-001、MC-TEST-001及§4.7相关命名/校验/Bean/日志/配置检查。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11；具体强制约束见§4.8逐项矩阵，当前文件仅在其实际角色适用，不添加无意义框架。
- Implementation pseudocode:

```java
  validate qualified accessor keys
  capture immutable values
  backup all
  install or clear missing
  rollback attempted in reverse
  close all with isolated diagnostics and Trace.close last
  scope thread-confined/idempotent
```

- Verification contribution: DTP focused/module tests；完整Spec TEST-001..023中本文件对应断言。
- After this file: 本文件责任具备，继续补齐本Step依赖后运行聚焦GREEN/模块门禁。

#### File 6 — `CREATE egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/test/java/top/egon/cola/component/dtp/context/DtpTaskContextManagerTest.java`

- Purpose: 隔离快照及坏Accessor故障验证。
- Symbols: DtpTaskContextManagerTest；assert empty removes, immutable snapshot, nested restores, rollback includes failing setter, broken restore continues, no original-result mutation, cross-manager/thread refused。
- Repository evidence: 完整Spec §8/§14的同包现有结构；当前文件尚无，按已批准同包角色创建。
- Dependencies and consumers: 同模块原调用链、ContextManager/Observer/配置/测试；common只向下依赖，生产者保持原模块。
- Why now: 依赖前面契约/测试和原实现，逐步连通同一集成任务。
- Contract/signature changes: assert empty removes, immutable snapshot, nested restores, rollback includes failing setter, broken restore continues, no original-result mutation, cross-manager/thread refused。
- Input/output and state mapping: 执行输入来自可信提交点；null/空值/原Trace时点/真实终态依完整Spec §7/§9/§10；无业务持久化变化。
- Error and edge behavior: 本文件按上述算法保留原异常/取消/拒绝；故障不暗改业务结果，不忽略恢复，不新增身份默认值。
- Standards impact: MC-SCOPE-001、MC-TEST-001及§4.7相关命名/校验/Bean/日志/配置检查。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11；具体强制约束见§4.8逐项矩阵，当前文件仅在其实际角色适用，不添加无意义框架。
- Implementation pseudocode:

```java
assert empty removes, immutable snapshot, nested restores, rollback includes failing setter, broken restore continues, no original-result mutation, cross-manager/thread refused
set holder A; capture snapshot; change parent B; open on worker C => sees A; close => C
second accessor writes then throws: rollback includes it and first accessor; delegate count remains zero
restore throws: next accessor and MDC still restored; original business cause unchanged
```

- Verification contribution: DTP focused/module tests；完整Spec TEST-001..023中本文件对应断言。
- After this file: 本文件责任具备，继续补齐本Step依赖后运行聚焦GREEN/模块门禁。

#### File 7 — `MODIFY egon-cola-components/egon-cola-component-common/egon-cola-component-common-core/src/main/java/top/egon/cola/component/common/core/exception/ExceptionLogger.java`

- Purpose: 兼容安全metadata重载。
- Symbols: ExceptionLogger；old log(logger,cause) -> overload(empty map)。
- Repository evidence: 完整Spec §8/§14的同包现有结构；当前文件存在，保留既有接口/风格。
- Dependencies and consumers: 同模块原调用链、ContextManager/Observer/配置/测试；common只向下依赖，生产者保持原模块。
- Why now: 依赖前面契约/测试和原实现，逐步连通同一集成任务。
- Contract/signature changes: old log(logger,cause) -> overload(empty map); retain classification/redaction; append max16 allowlisted sanitized metadata; never override reserved fields。
- Input/output and state mapping: 执行输入来自可信提交点；null/空值/原Trace时点/真实终态依完整Spec §7/§9/§10；无业务持久化变化。
- Error and edge behavior: 本文件按上述算法保留原异常/取消/拒绝；故障不暗改业务结果，不忽略恢复，不新增身份默认值。
- Standards impact: MC-SCOPE-001、MC-TEST-001及§4.7相关命名/校验/Bean/日志/配置检查。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11；具体强制约束见§4.8逐项矩阵，当前文件仅在其实际角色适用，不添加无意义框架。
- Implementation pseudocode:

```java
  old log(logger,cause) ->
overload(empty map)
  retain classification/redaction
  append max16 allowlisted sanitized metadata
  never override reserved fields
```

- Verification contribution: ExceptionClassificationTest；完整Spec TEST-001..023中本文件对应断言。
- After this file: 本文件责任具备，继续补齐本Step依赖后运行聚焦GREEN/模块门禁。

#### File 8 — `MODIFY egon-cola-components/egon-cola-component-common/egon-cola-component-common-core/src/test/java/top/egon/cola/component/common/exception/ExceptionClassificationTest.java`

- Purpose: 公共日志重载和原规则回归。
- Symbols: ExceptionClassificationTest；capture events。
- Repository evidence: 完整Spec §8/§14的同包现有结构；当前文件存在，保留既有接口/风格。
- Dependencies and consumers: 同模块原调用链、ContextManager/Observer/配置/测试；common只向下依赖，生产者保持原模块。
- Why now: 依赖前面契约/测试和原实现，逐步连通同一集成任务。
- Contract/signature changes: capture events; assert reserved keys protected, free-form messages redacted, old levels/causes preserved。
- Input/output and state mapping: 执行输入来自可信提交点；null/空值/原Trace时点/真实终态依完整Spec §7/§9/§10；无业务持久化变化。
- Error and edge behavior: 本文件按上述算法保留原异常/取消/拒绝；故障不暗改业务结果，不忽略恢复，不新增身份默认值。
- Standards impact: MC-SCOPE-001、MC-TEST-001及§4.7相关命名/校验/Bean/日志/配置检查。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11；具体强制约束见§4.8逐项矩阵，当前文件仅在其实际角色适用，不添加无意义框架。
- Implementation pseudocode:

```java
capture events
  assert reserved keys protected, free-form messages redacted, old levels/causes preserved
assert metadata cannot replace exceptionCategory, exceptionLevel, traceId or requestId
assert unknown/password metadata absent, allowed executorName bounded and control-free
assert original business INFO and system ERROR redacted stack match old two-argument API
```

- Verification contribution: ExceptionClassificationTest；完整Spec TEST-001..023中本文件对应断言。
- After this file: 本文件责任具备，继续补齐本Step依赖后运行聚焦GREEN/模块门禁。

#### File 9 — `CREATE egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/main/java/top/egon/cola/component/dtp/exception/DtpTaskFailureBO.java`

- Purpose: 失败处理策略输入。
- Symbols: DtpTaskFailureBO；class complete Lombok baseline。
- Repository evidence: 完整Spec §8/§14的同包现有结构；当前文件尚无，按已批准同包角色创建。
- Dependencies and consumers: 同模块原调用链、ContextManager/Observer/配置/测试；common只向下依赖，生产者保持原模块。
- Why now: 依赖前面契约/测试和原实现，逐步连通同一集成任务。
- Contract/signature changes: class complete Lombok baseline; Report group @NotBlank failureId/source @NotNull cause/time; optional unknown pool/trace; no raw TL/MDC/args。
- Input/output and state mapping: 执行输入来自可信提交点；null/空值/原Trace时点/真实终态依完整Spec §7/§9/§10；无业务持久化变化。
- Error and edge behavior: 本文件按上述算法保留原异常/取消/拒绝；故障不暗改业务结果，不忽略恢复，不新增身份默认值。
- Standards impact: MC-SCOPE-001、MC-TEST-001及§4.7相关命名/校验/Bean/日志/配置检查。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11；具体强制约束见§4.8逐项矩阵，当前文件仅在其实际角色适用，不添加无意义框架。
- Implementation pseudocode:

```java
  class complete Lombok baseline
  Report group @NotBlank failureId/source @NotNull cause/time
  optional unknown pool/trace
  no raw TL/MDC/args
```

- Verification contribution: DTP focused/module tests；完整Spec TEST-001..023中本文件对应断言。
- After this file: 本文件责任具备，继续补齐本Step依赖后运行聚焦GREEN/模块门禁。

#### File 10 — `CREATE egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/main/java/top/egon/cola/component/dtp/exception/DtpTaskExceptionHandler.java`

- Purpose: 单个可替换异常策略。
- Symbols: DtpTaskExceptionHandler；void handle(DtpTaskFailureBO failure)。
- Repository evidence: 完整Spec §8/§14的同包现有结构；当前文件尚无，按已批准同包角色创建。
- Dependencies and consumers: 同模块原调用链、ContextManager/Observer/配置/测试；common只向下依赖，生产者保持原模块。
- Why now: 依赖前面契约/测试和原实现，逐步连通同一集成任务。
- Contract/signature changes: void handle(DtpTaskFailureBO failure); caller validates Report group and isolates nonfatal reporting errors。
- Input/output and state mapping: 执行输入来自可信提交点；null/空值/原Trace时点/真实终态依完整Spec §7/§9/§10；无业务持久化变化。
- Error and edge behavior: 本文件按上述算法保留原异常/取消/拒绝；故障不暗改业务结果，不忽略恢复，不新增身份默认值。
- Standards impact: MC-SCOPE-001、MC-TEST-001及§4.7相关命名/校验/Bean/日志/配置检查。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11；具体强制约束见§4.8逐项矩阵，当前文件仅在其实际角色适用，不添加无意义框架。
- Implementation pseudocode:

```java
void handle(DtpTaskFailureBO failure)
  caller validates Report group and isolates nonfatal reporting errors
handle receives @Valid nonnull DtpTaskFailureBO with Report validation group
same task token calls once, distinct tokens using same Throwable each call
SPI cannot return replacement result; nonfatal failures stay inside reporting boundary
```

- Verification contribution: DTP focused/module tests；完整Spec TEST-001..023中本文件对应断言。
- After this file: 本文件责任具备，继续补齐本Step依赖后运行聚焦GREEN/模块门禁。

#### File 11 — `CREATE egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/main/java/top/egon/cola/component/dtp/exception/LoggingDtpTaskExceptionHandler.java`

- Purpose: 公共Logger默认策略。
- Symbols: LoggingDtpTaskExceptionHandler；@Slf4j default handler。
- Repository evidence: 完整Spec §8/§14的同包现有结构；当前文件尚无，按已批准同包角色创建。
- Dependencies and consumers: 同模块原调用链、ContextManager/Observer/配置/测试；common只向下依赖，生产者保持原模块。
- Why now: 依赖前面契约/测试和原实现，逐步连通同一集成任务。
- Contract/signature changes: @Slf4j default handler; honor logEligible; build safe technical metadata; call existing ExceptionLogger overload。
- Input/output and state mapping: 执行输入来自可信提交点；null/空值/原Trace时点/真实终态依完整Spec §7/§9/§10；无业务持久化变化。
- Error and edge behavior: 本文件按上述算法保留原异常/取消/拒绝；故障不暗改业务结果，不忽略恢复，不新增身份默认值。
- Standards impact: MC-SCOPE-001、MC-TEST-001及§4.7相关命名/校验/Bean/日志/配置检查。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11；具体强制约束见§4.8逐项矩阵，当前文件仅在其实际角色适用，不添加无意义框架。
- Implementation pseudocode:

```java
@Slf4j default handler
  honor logEligible
  build safe technical metadata
  call existing ExceptionLogger overload
if failure.logEligible == false return without affecting observer failure counters
metadata includes only app/instance/executor/kind/source/failure/thread/method/time/suppressed
ExceptionLogger.log(log, failure.cause, metadata) keeps original category and redacted stack
```

- Verification contribution: DTP focused/module tests；完整Spec TEST-001..023中本文件对应断言。
- After this file: 本文件责任具备，继续补齐本Step依赖后运行聚焦GREEN/模块门禁。

#### File 12 — `MODIFY egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/main/java/top/egon/cola/component/dtp/config/DynamicThreadPoolAutoProperties.java`

- Purpose: 传播/异常配置及Duration校验。
- Symbols: DynamicThreadPoolAutoProperties；add @Valid ContextPropagation and ExceptionHandling classes。
- Repository evidence: 完整Spec §8/§14的同包现有结构；当前文件存在，保留既有接口/风格。
- Dependencies and consumers: 同模块原调用链、ContextManager/Observer/配置/测试；common只向下依赖，生产者保持原模块。
- Why now: 依赖前面契约/测试和原实现，逐步连通同一集成任务。
- Contract/signature changes: add @Valid ContextPropagation and ExceptionHandling classes; customTL enabled=true; reporting independent; positive Duration fits nanos。
- Input/output and state mapping: 执行输入来自可信提交点；null/空值/原Trace时点/真实终态依完整Spec §7/§9/§10；无业务持久化变化。
- Error and edge behavior: 本文件按上述算法保留原异常/取消/拒绝；故障不暗改业务结果，不忽略恢复，不新增身份默认值。
- Standards impact: MC-SCOPE-001、MC-TEST-001及§4.7相关命名/校验/Bean/日志/配置检查。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11；具体强制约束见§4.8逐项矩阵，当前文件仅在其实际角色适用，不添加无意义框架。
- Implementation pseudocode:

```java
  add @Valid ContextPropagation and ExceptionHandling classes
  customTL enabled=true
  reporting independent
  positive Duration fits nanos
```

- Verification contribution: DTP focused/module tests；完整Spec TEST-001..023中本文件对应断言。
- After this file: 本文件责任具备，继续补齐本Step依赖后运行聚焦GREEN/模块门禁。

#### File 13 — `MODIFY egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/main/java/top/egon/cola/component/dtp/context/DtpTaskObserver.java`

- Purpose: 复用单token连接上下文/异常/真实root/UC。
- Symbols: DtpTaskObserver；old constructor delegates defaults。
- Repository evidence: 完整Spec §8/§14的同包现有结构；当前文件存在，保留既有接口/风格。
- Dependencies and consumers: 同模块原调用链、ContextManager/Observer/配置/测试；common只向下依赖，生产者保持原模块。
- Why now: 依赖前面契约/测试和原实现，逐步连通同一集成任务。
- Contract/signature changes: old constructor delegates defaults; lazy manager/handler suppliers; owner token dedup; split outcome/report/stat gates; root constructed before dispatch and completed on install failure; contextual carrier fixed snapshot/new token; owner worker escape slot; original UC delegate。
- Input/output and state mapping: 执行输入来自可信提交点；null/空值/原Trace时点/真实终态依完整Spec §7/§9/§10；无业务持久化变化。
- Error and edge behavior: 本文件按上述算法保留原异常/取消/拒绝；故障不暗改业务结果，不忽略恢复，不新增身份默认值。
- Standards impact: MC-SCOPE-001、MC-TEST-001及§4.7相关命名/校验/Bean/日志/配置检查。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11；具体强制约束见§4.8逐项矩阵，当前文件仅在其实际角色适用，不添加无意义框架。
- Implementation pseudocode:

```java
  old constructor delegates defaults
  lazy manager/handler suppliers
  owner token dedup
  split outcome/report/stat gates
  root constructed before dispatch and completed on install failure
  contextual carrier fixed snapshot/new token
  owner worker escape slot
  original UC delegate
```

- Verification contribution: DTP focused/module tests；完整Spec TEST-001..023中本文件对应断言。
- After this file: 本文件责任具备，继续补齐本Step依赖后运行聚焦GREEN/模块门禁。

#### File 14 — `CREATE egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/test/java/top/egon/cola/component/dtp/executor/ThreadManageCompletableFutureTest.java`

- Purpose: 根阶段与绑定上下文CF验证。
- Symbols: ThreadManageCompletableFutureTest；assert root identity/results/cancel/rejection/install failure completes。
- Repository evidence: 完整Spec §8/§14的同包现有结构；当前文件尚无，按已批准同包角色创建。
- Dependencies and consumers: 同模块原调用链、ContextManager/Observer/配置/测试；common只向下依赖，生产者保持原模块。
- Why now: 依赖前面契约/测试和原实现，逐步连通同一集成任务。
- Contract/signature changes: assert root identity/results/cancel/rejection/install failure completes; external thread completes upstream but bound executor sees captured A; per-dispatch independent token。
- Input/output and state mapping: 执行输入来自可信提交点；null/空值/原Trace时点/真实终态依完整Spec §7/§9/§10；无业务持久化变化。
- Error and edge behavior: 本文件按上述算法保留原异常/取消/拒绝；故障不暗改业务结果，不忽略恢复，不新增身份默认值。
- Standards impact: MC-SCOPE-001、MC-TEST-001及§4.7相关命名/校验/Bean/日志/配置检查。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11；具体强制约束见§4.8逐项矩阵，当前文件仅在其实际角色适用，不添加无意义框架。
- Implementation pseudocode:

```java
  assert root identity/results/cancel/rejection/install failure completes
  external thread completes upstream but bound executor sees captured A
  per-dispatch independent token
```

- Verification contribution: DTP focused/module tests；完整Spec TEST-001..023中本文件对应断言。
- After this file: 本文件责任具备，继续补齐本Step依赖后运行聚焦GREEN/模块门禁。

#### File 15 — `MODIFY egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/main/java/top/egon/cola/component/dtp/executor/ManagedExecutorRegistry.java`

- Purpose: 保持唯一目录、延迟传递策略。
- Symbols: ManagedExecutorRegistry；preserve all constructors。
- Repository evidence: 完整Spec §8/§14的同包现有结构；当前文件存在，保留既有接口/风格。
- Dependencies and consumers: 同模块原调用链、ContextManager/Observer/配置/测试；common只向下依赖，生产者保持原模块。
- Why now: 依赖前面契约/测试和原实现，逐步连通同一集成任务。
- Contract/signature changes: preserve all constructors; add enhanced constructor with manager/handler suppliers; create/register observers with known kind; no global registry/bean replacement。
- Input/output and state mapping: 执行输入来自可信提交点；null/空值/原Trace时点/真实终态依完整Spec §7/§9/§10；无业务持久化变化。
- Error and edge behavior: 本文件按上述算法保留原异常/取消/拒绝；故障不暗改业务结果，不忽略恢复，不新增身份默认值。
- Standards impact: MC-SCOPE-001、MC-TEST-001及§4.7相关命名/校验/Bean/日志/配置检查。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11；具体强制约束见§4.8逐项矩阵，当前文件仅在其实际角色适用，不添加无意义框架。
- Implementation pseudocode:

```java
  preserve all constructors
  add enhanced constructor with manager/handler suppliers
  create/register observers with known kind
  no global registry/bean replacement
```

- Verification contribution: DTP focused/module tests；完整Spec TEST-001..023中本文件对应断言。
- After this file: 本文件责任具备，继续补齐本Step依赖后运行聚焦GREEN/模块门禁。

#### File 16 — `MODIFY egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/main/java/top/egon/cola/component/dtp/executor/ThreadManage.java`

- Purpose: 三个便捷公开入口。
- Symbols: ThreadManage；runAsync(name,Runnable), supplyAsync(name,Supplier), contextualExecutor(name)。
- Repository evidence: 完整Spec §8/§14的同包现有结构；当前文件存在，保留既有接口/风格。
- Dependencies and consumers: 同模块原调用链、ContextManager/Observer/配置/测试；common只向下依赖，生产者保持原模块。
- Why now: 依赖前面契约/测试和原实现，逐步连通同一集成任务。
- Contract/signature changes: runAsync(name,Runnable), supplyAsync(name,Supplier), contextualExecutor(name); reuse existing executor(name).taskObserver(); null capability -> UnsupportedOperationException only for new APIs。
- Input/output and state mapping: 执行输入来自可信提交点；null/空值/原Trace时点/真实终态依完整Spec §7/§9/§10；无业务持久化变化。
- Error and edge behavior: 本文件按上述算法保留原异常/取消/拒绝；故障不暗改业务结果，不忽略恢复，不新增身份默认值。
- Standards impact: MC-SCOPE-001、MC-TEST-001及§4.7相关命名/校验/Bean/日志/配置检查。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11；具体强制约束见§4.8逐项矩阵，当前文件仅在其实际角色适用，不添加无意义框架。
- Implementation pseudocode:

```java
  runAsync(name,Runnable), supplyAsync(name,Supplier), contextualExecutor(name)
  reuse existing executor(name).taskObserver()
  null capability ->
UnsupportedOperationException only for new APIs
```

- Verification contribution: DTP focused/module tests；完整Spec TEST-001..023中本文件对应断言。
- After this file: 本文件责任具备，继续补齐本Step依赖后运行聚焦GREEN/模块门禁。

#### File 17 — `MODIFY egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/main/java/top/egon/cola/component/dtp/executor/managed/DtpThreadPoolTaskExecutor.java`

- Purpose: 自建工厂兜底与Spring CF根阶段。
- Symbols: DtpThreadPoolTaskExecutor；compose thread factory through observer。
- Repository evidence: 完整Spec §8/§14的同包现有结构；当前文件存在，保留既有接口/风格。
- Dependencies and consumers: 同模块原调用链、ContextManager/Observer/配置/测试；common只向下依赖，生产者保持原模块。
- Why now: 依赖前面契约/测试和原实现，逐步连通同一集成任务。
- Contract/signature changes: compose thread factory through observer; retain custom factory/handler attributes; override submitCompletable Runnable/Callable with same root bridge。
- Input/output and state mapping: 执行输入来自可信提交点；null/空值/原Trace时点/真实终态依完整Spec §7/§9/§10；无业务持久化变化。
- Error and edge behavior: 本文件按上述算法保留原异常/取消/拒绝；故障不暗改业务结果，不忽略恢复，不新增身份默认值。
- Standards impact: MC-SCOPE-001、MC-TEST-001及§4.7相关命名/校验/Bean/日志/配置检查。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11；具体强制约束见§4.8逐项矩阵，当前文件仅在其实际角色适用，不添加无意义框架。
- Implementation pseudocode:

```java
  compose thread factory through observer
  retain custom factory/handler attributes
  override submitCompletable Runnable/Callable with same root bridge
```

- Verification contribution: DTP focused/module tests；完整Spec TEST-001..023中本文件对应断言。
- After this file: 本文件责任具备，继续补齐本Step依赖后运行聚焦GREEN/模块门禁。

#### File 18 — `MODIFY egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/main/java/top/egon/cola/component/dtp/executor/virtual/BoundedVirtualThreadExecutor.java`

- Purpose: 虚拟工厂同一观察器兜底。
- Symbols: BoundedVirtualThreadExecutor；at owned virtual thread creation consult observer and compose factory。
- Repository evidence: 完整Spec §8/§14的同包现有结构；当前文件存在，保留既有接口/风格。
- Dependencies and consumers: 同模块原调用链、ContextManager/Observer/配置/测试；common只向下依赖，生产者保持原模块。
- Why now: 依赖前面契约/测试和原实现，逐步连通同一集成任务。
- Contract/signature changes: at owned virtual thread creation consult observer and compose factory; preserve permits and original metrics lifecycle。
- Input/output and state mapping: 执行输入来自可信提交点；null/空值/原Trace时点/真实终态依完整Spec §7/§9/§10；无业务持久化变化。
- Error and edge behavior: 本文件按上述算法保留原异常/取消/拒绝；故障不暗改业务结果，不忽略恢复，不新增身份默认值。
- Standards impact: MC-SCOPE-001、MC-TEST-001及§4.7相关命名/校验/Bean/日志/配置检查。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11；具体强制约束见§4.8逐项矩阵，当前文件仅在其实际角色适用，不添加无意义框架。
- Implementation pseudocode:

```java
at owned virtual thread creation consult observer and compose factory
  preserve permits and original metrics lifecycle
retain base virtual factory and semaphore construction order
thread creation reads volatile observer; observer-owned factory composes original handler
do not move cancellation cleanup into Future.done or release a running permit early
```

- Verification contribution: DTP focused/module tests；完整Spec TEST-001..023中本文件对应断言。
- After this file: 本文件责任具备，继续补齐本Step依赖后运行聚焦GREEN/模块门禁。

#### File 19 — `MODIFY egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/main/java/top/egon/cola/component/dtp/context/DtpTaskDecorator.java`

- Purpose: 原无参兼容及完整上下文装饰。
- Symbols: DtpTaskDecorator；keep noarg Trace-only。
- Repository evidence: 完整Spec §8/§14的同包现有结构；当前文件存在，保留既有接口/风格。
- Dependencies and consumers: 同模块原调用链、ContextManager/Observer/配置/测试；common只向下依赖，生产者保持原模块。
- Why now: 依赖前面契约/测试和原实现，逐步连通同一集成任务。
- Contract/signature changes: keep noarg Trace-only; enhanced constructor accepts lazy manager provider; observed/carrier task forwards token/snapshot without recapture。
- Input/output and state mapping: 执行输入来自可信提交点；null/空值/原Trace时点/真实终态依完整Spec §7/§9/§10；无业务持久化变化。
- Error and edge behavior: 本文件按上述算法保留原异常/取消/拒绝；故障不暗改业务结果，不忽略恢复，不新增身份默认值。
- Standards impact: MC-SCOPE-001、MC-TEST-001及§4.7相关命名/校验/Bean/日志/配置检查。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11；具体强制约束见§4.8逐项矩阵，当前文件仅在其实际角色适用，不添加无意义框架。
- Implementation pseudocode:

```java
  keep noarg Trace-only
  enhanced constructor accepts lazy manager provider
  observed/carrier task forwards token/snapshot without recapture
```

- Verification contribution: DTP focused/module tests；完整Spec TEST-001..023中本文件对应断言。
- After this file: 本文件责任具备，继续补齐本Step依赖后运行聚焦GREEN/模块门禁。

#### File 20 — `CREATE egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/main/java/top/egon/cola/component/dtp/exception/DtpAsyncUncaughtExceptionHandler.java`

- Purpose: 真实void异步失败桥。
- Symbols: DtpAsyncUncaughtExceptionHandler；@Slf4j qualified lazy reporter。
- Repository evidence: 完整Spec §8/§14的同包现有结构；当前文件尚无，按已批准同包角色创建。
- Dependencies and consumers: 同模块原调用链、ContextManager/Observer/配置/测试；common只向下依赖，生产者保持原模块。
- Why now: 依赖前面契约/测试和原实现，逐步连通同一集成任务。
- Contract/signature changes: @Slf4j qualified lazy reporter; ACTIVE observation marks actual failure before Spring wrapper returns; no token -> diagnostic only; ignore params。
- Input/output and state mapping: 执行输入来自可信提交点；null/空值/原Trace时点/真实终态依完整Spec §7/§9/§10；无业务持久化变化。
- Error and edge behavior: 本文件按上述算法保留原异常/取消/拒绝；故障不暗改业务结果，不忽略恢复，不新增身份默认值。
- Standards impact: MC-SCOPE-001、MC-TEST-001及§4.7相关命名/校验/Bean/日志/配置检查。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11；具体强制约束见§4.8逐项矩阵，当前文件仅在其实际角色适用，不添加无意义框架。
- Implementation pseudocode:

```java
  @Slf4j qualified lazy reporter
  ACTIVE observation marks actual failure before Spring wrapper returns
  no token ->
diagnostic only
  ignore params
```

- Verification contribution: DTP focused/module tests；完整Spec TEST-001..023中本文件对应断言。
- After this file: 本文件责任具备，继续补齐本Step依赖后运行聚焦GREEN/模块门禁。

#### File 21 — `CREATE egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/main/java/top/egon/cola/component/dtp/config/DtpAsyncConfigurer.java`

- Purpose: 已有Async启用下条件退让配置。
- Symbols: DtpAsyncConfigurer；implement AsyncConfigurer only when enabled and no existing configurer。
- Repository evidence: 完整Spec §8/§14的同包现有结构；当前文件尚无，按已批准同包角色创建。
- Dependencies and consumers: 同模块原调用链、ContextManager/Observer/配置/测试；common只向下依赖，生产者保持原模块。
- Why now: 依赖前面契约/测试和原实现，逐步连通同一集成任务。
- Contract/signature changes: implement AsyncConfigurer only when enabled and no existing configurer; return null executor; lazy named handler; no @EnableAsync。
- Input/output and state mapping: 执行输入来自可信提交点；null/空值/原Trace时点/真实终态依完整Spec §7/§9/§10；无业务持久化变化。
- Error and edge behavior: 本文件按上述算法保留原异常/取消/拒绝；故障不暗改业务结果，不忽略恢复，不新增身份默认值。
- Standards impact: MC-SCOPE-001、MC-TEST-001及§4.7相关命名/校验/Bean/日志/配置检查。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11；具体强制约束见§4.8逐项矩阵，当前文件仅在其实际角色适用，不添加无意义框架。
- Implementation pseudocode:

```java
  implement AsyncConfigurer only when enabled and no existing configurer
  return null executor
  lazy named handler
  no @EnableAsync
```

- Verification contribution: DTP focused/module tests；完整Spec TEST-001..023中本文件对应断言。
- After this file: 本文件责任具备，继续补齐本Step依赖后运行聚焦GREEN/模块门禁。

#### File 22 — `CREATE egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/main/java/top/egon/cola/component/dtp/config/DtpTaskContextAutoConfiguration.java`

- Purpose: Boot builder之前缺省装饰器。
- Symbols: DtpTaskContextAutoConfiguration；@AutoConfiguration(before=TaskExecutionAutoConfiguration.class)。
- Repository evidence: 完整Spec §8/§14的同包现有结构；当前文件尚无，按已批准同包角色创建。
- Dependencies and consumers: 同模块原调用链、ContextManager/Observer/配置/测试；common只向下依赖，生产者保持原模块。
- Why now: 依赖前面契约/测试和原实现，逐步连通同一集成任务。
- Contract/signature changes: @AutoConfiguration(before=TaskExecutionAutoConfiguration.class); named manager/handler; qualified accessor provider; lazy decorator; missing user decorator only; optional configurer uses already enabled Async。
- Input/output and state mapping: 执行输入来自可信提交点；null/空值/原Trace时点/真实终态依完整Spec §7/§9/§10；无业务持久化变化。
- Error and edge behavior: 本文件按上述算法保留原异常/取消/拒绝；故障不暗改业务结果，不忽略恢复，不新增身份默认值。
- Standards impact: MC-SCOPE-001、MC-TEST-001及§4.7相关命名/校验/Bean/日志/配置检查。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11；具体强制约束见§4.8逐项矩阵，当前文件仅在其实际角色适用，不添加无意义框架。
- Implementation pseudocode:

```java
  @AutoConfiguration(before=TaskExecutionAutoConfiguration.class)
  named manager/handler
  qualified accessor provider
  lazy decorator
  missing user decorator only
  optional configurer uses already enabled Async
```

- Verification contribution: DTP focused/module tests；完整Spec TEST-001..023中本文件对应断言。
- After this file: 本文件责任具备，继续补齐本Step依赖后运行聚焦GREEN/模块门禁。

#### File 23 — `MODIFY egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/main/java/top/egon/cola/component/dtp/config/DynamicThreadPoolAutoConfig.java`

- Purpose: 增强Registry装配且保留旧工厂调用。
- Symbols: DynamicThreadPoolAutoConfig；keep original factory overloads。
- Repository evidence: 完整Spec §8/§14的同包现有结构；当前文件存在，保留既有接口/风格。
- Dependencies and consumers: 同模块原调用链、ContextManager/Observer/配置/测试；common只向下依赖，生产者保持原模块。
- Why now: 依赖前面契约/测试和原实现，逐步连通同一集成任务。
- Contract/signature changes: keep original factory overloads; new named autowired factory uses lazy manager/handler suppliers; no eager all-bean collection。
- Input/output and state mapping: 执行输入来自可信提交点；null/空值/原Trace时点/真实终态依完整Spec §7/§9/§10；无业务持久化变化。
- Error and edge behavior: 本文件按上述算法保留原异常/取消/拒绝；故障不暗改业务结果，不忽略恢复，不新增身份默认值。
- Standards impact: MC-SCOPE-001、MC-TEST-001及§4.7相关命名/校验/Bean/日志/配置检查。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11；具体强制约束见§4.8逐项矩阵，当前文件仅在其实际角色适用，不添加无意义框架。
- Implementation pseudocode:

```java
  keep original factory overloads
  new named autowired factory uses lazy manager/handler suppliers
  no eager all-bean collection
```

- Verification contribution: DTP focused/module tests；完整Spec TEST-001..023中本文件对应断言。
- After this file: 本文件责任具备，继续补齐本Step依赖后运行聚焦GREEN/模块门禁。

#### File 24 — `MODIFY egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`

- Purpose: 注册新增自动配置。
- Symbols: org.springframework.boot.autoconfigure.AutoConfiguration；append DtpTaskContextAutoConfiguration。
- Repository evidence: 完整Spec §8/§14的同包现有结构；当前文件存在，保留既有接口/风格。
- Dependencies and consumers: 同模块原调用链、ContextManager/Observer/配置/测试；common只向下依赖，生产者保持原模块。
- Why now: 依赖前面契约/测试和原实现，逐步连通同一集成任务。
- Contract/signature changes: append DtpTaskContextAutoConfiguration; retain existing auto config entry。
- Input/output and state mapping: 执行输入来自可信提交点；null/空值/原Trace时点/真实终态依完整Spec §7/§9/§10；无业务持久化变化。
- Error and edge behavior: 本文件按上述算法保留原异常/取消/拒绝；故障不暗改业务结果，不忽略恢复，不新增身份默认值。
- Standards impact: MC-SCOPE-001、MC-TEST-001及§4.7相关命名/校验/Bean/日志/配置检查。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11；具体强制约束见§4.8逐项矩阵，当前文件仅在其实际角色适用，不添加无意义框架。
- Implementation pseudocode:

```java
append DtpTaskContextAutoConfiguration
  retain existing auto config entry
retain top.egon.cola.component.dtp.config.DynamicThreadPoolAutoConfig
append top.egon.cola.component.dtp.config.DtpTaskContextAutoConfiguration once
new configuration precedes Boot TaskExecutionAutoConfiguration via annotation ordering
```

- Verification contribution: DTP focused/module tests；完整Spec TEST-001..023中本文件对应断言。
- After this file: 本文件责任具备，继续补齐本Step依赖后运行聚焦GREEN/模块门禁。

#### File 25 — `MODIFY egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/main/java/top/egon/cola/component/dtp/metrics/DtpMeterBinder.java`

- Purpose: 保持原指标并增加低基数技术失败。
- Symbols: DtpMeterBinder；register context failures by fixed phase, reporter failures and uncaught counters。
- Repository evidence: 完整Spec §8/§14的同包现有结构；当前文件存在，保留既有接口/风格。
- Dependencies and consumers: 同模块原调用链、ContextManager/Observer/配置/测试；common只向下依赖，生产者保持原模块。
- Why now: 依赖前面契约/测试和原实现，逐步连通同一集成任务。
- Contract/signature changes: register context failures by fixed phase, reporter failures and uncaught counters; keep all old IDs/tags/removal lifecycle; no trace/user/key metric tags。
- Input/output and state mapping: 执行输入来自可信提交点；null/空值/原Trace时点/真实终态依完整Spec §7/§9/§10；无业务持久化变化。
- Error and edge behavior: 本文件按上述算法保留原异常/取消/拒绝；故障不暗改业务结果，不忽略恢复，不新增身份默认值。
- Standards impact: MC-SCOPE-001、MC-TEST-001及§4.7相关命名/校验/Bean/日志/配置检查。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11；具体强制约束见§4.8逐项矩阵，当前文件仅在其实际角色适用，不添加无意义框架。
- Implementation pseudocode:

```java
  register context failures by fixed phase, reporter failures and uncaught counters
  keep all old IDs/tags/removal lifecycle
  no trace/user/key metric tags
```

- Verification contribution: DTP focused/module tests；完整Spec TEST-001..023中本文件对应断言。
- After this file: 本文件责任具备，继续补齐本Step依赖后运行聚焦GREEN/模块门禁。

#### File 26 — `CREATE egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/test/java/top/egon/cola/component/dtp/config/DtpTaskContextAutoConfigurationTest.java`

- Purpose: 缺省Boot装饰器/用户配置/配置开关。
- Symbols: DtpTaskContextAutoConfigurationTest；isolated ContextRunner with mocked Redis only when old config included。
- Repository evidence: 完整Spec §8/§14的同包现有结构；当前文件尚无，按已批准同包角色创建。
- Dependencies and consumers: 同模块原调用链、ContextManager/Observer/配置/测试；common只向下依赖，生产者保持原模块。
- Why now: 依赖前面契约/测试和原实现，逐步连通同一集成任务。
- Contract/signature changes: isolated ContextRunner with mocked Redis only when old config included; assert decorator chosen before builder, qualifier registry, no global accessors, user decorator/configurer wins, metrics absent allowed。
- Input/output and state mapping: 执行输入来自可信提交点；null/空值/原Trace时点/真实终态依完整Spec §7/§9/§10；无业务持久化变化。
- Error and edge behavior: 本文件按上述算法保留原异常/取消/拒绝；故障不暗改业务结果，不忽略恢复，不新增身份默认值。
- Standards impact: MC-SCOPE-001、MC-TEST-001及§4.7相关命名/校验/Bean/日志/配置检查。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11；具体强制约束见§4.8逐项矩阵，当前文件仅在其实际角色适用，不添加无意义框架。
- Implementation pseudocode:

```java
isolated ContextRunner with mocked Redis only when old config included
  assert decorator chosen before builder, qualifier registry, no global accessors, user decorator/configurer wins, metrics absent allowed
no user decorator => Boot builder obtains exactly dtpTaskDecorator before creating pool
existing user decorator/configurer => component backs off without replacing Bean type
qualified accessor selected lazily; global or unqualified accessor is ignored
```

- Verification contribution: DTP focused/module tests；完整Spec TEST-001..023中本文件对应断言。
- After this file: 本文件责任具备，继续补齐本Step依赖后运行聚焦GREEN/模块门禁。

#### File 27 — `CREATE egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/test/java/top/egon/cola/component/dtp/config/DtpAsyncIntegrationTest.java`

- Purpose: 真实Async代理终态及上下文。
- Symbols: DtpAsyncIntegrationTest；finite managed pool + @EnableAsync isolated context。
- Repository evidence: 完整Spec §8/§14的同包现有结构；当前文件尚无，按已批准同包角色创建。
- Dependencies and consumers: 同模块原调用链、ContextManager/Observer/配置/测试；common只向下依赖，生产者保持原模块。
- Why now: 依赖前面契约/测试和原实现，逐步连通同一集成任务。
- Contract/signature changes: finite managed pool + @EnableAsync isolated context; void/Future/CF actual proxies; assert caller MDC/TL restored and one failure; missing component fallback tested。
- Input/output and state mapping: 执行输入来自可信提交点；null/空值/原Trace时点/真实终态依完整Spec §7/§9/§10；无业务持久化变化。
- Error and edge behavior: 本文件按上述算法保留原异常/取消/拒绝；故障不暗改业务结果，不忽略恢复，不新增身份默认值。
- Standards impact: MC-SCOPE-001、MC-TEST-001及§4.7相关命名/校验/Bean/日志/配置检查。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11；具体强制约束见§4.8逐项矩阵，当前文件仅在其实际角色适用，不添加无意义框架。
- Implementation pseudocode:

```java
  finite managed pool + @EnableAsync isolated context
  void/Future/CF actual proxies
  assert caller MDC/TL restored and one failure
  missing component fallback tested
```

- Verification contribution: DTP focused/module tests；完整Spec TEST-001..023中本文件对应断言。
- After this file: 本文件责任具备，继续补齐本Step依赖后运行聚焦GREEN/模块门禁。

#### File 28 — `MODIFY egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/test/java/top/egon/cola/component/dtp/context/DtpContextPropagationTest.java`

- Purpose: 旧捕获时点/多线程/CallerRuns回归。
- Symbols: DtpContextPropagationTest；reuse original wrappers。
- Repository evidence: 完整Spec §8/§14的同包现有结构；当前文件存在，保留既有接口/风格。
- Dependencies and consumers: 同模块原调用链、ContextManager/Observer/配置/测试；common只向下依赖，生产者保持原模块。
- Why now: 依赖前面契约/测试和原实现，逐步连通同一集成任务。
- Contract/signature changes: reuse original wrappers; test per-submit A/B/empty, nested cross-pool, saturated CallerRuns, same Runnable; use latches not sleeps。
- Input/output and state mapping: 执行输入来自可信提交点；null/空值/原Trace时点/真实终态依完整Spec §7/§9/§10；无业务持久化变化。
- Error and edge behavior: 本文件按上述算法保留原异常/取消/拒绝；故障不暗改业务结果，不忽略恢复，不新增身份默认值。
- Standards impact: MC-SCOPE-001、MC-TEST-001及§4.7相关命名/校验/Bean/日志/配置检查。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11；具体强制约束见§4.8逐项矩阵，当前文件仅在其实际角色适用，不添加无意义框架。
- Implementation pseudocode:

```java
  reuse original wrappers
  test per-submit A/B/empty, nested cross-pool, saturated CallerRuns, same Runnable
  use latches not sleeps
```

- Verification contribution: DTP focused/module tests；完整Spec TEST-001..023中本文件对应断言。
- After this file: 本文件责任具备，继续补齐本Step依赖后运行聚焦GREEN/模块门禁。

#### File 29 — `MODIFY egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/test/java/top/egon/cola/component/dtp/context/DtpTaskObserverTest.java`

- Purpose: 取消/批量/不透明阶段兼容。
- Symbols: DtpTaskObserverTest；retain original tests。
- Repository evidence: 完整Spec §8/§14的同包现有结构；当前文件存在，保留既有接口/风格。
- Dependencies and consumers: 同模块原调用链、ContextManager/Observer/配置/测试；common只向下依赖，生产者保持原模块。
- Why now: 依赖前面契约/测试和原实现，逐步连通同一集成任务。
- Contract/signature changes: retain original tests; add observation disabled but errors report/context transfer, same Throwable distinct tasks, opaque stage never false success。
- Input/output and state mapping: 执行输入来自可信提交点；null/空值/原Trace时点/真实终态依完整Spec §7/§9/§10；无业务持久化变化。
- Error and edge behavior: 本文件按上述算法保留原异常/取消/拒绝；故障不暗改业务结果，不忽略恢复，不新增身份默认值。
- Standards impact: MC-SCOPE-001、MC-TEST-001及§4.7相关命名/校验/Bean/日志/配置检查。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11；具体强制约束见§4.8逐项矩阵，当前文件仅在其实际角色适用，不添加无意义框架。
- Implementation pseudocode:

```java
retain original tests
  add observation disabled but errors report/context transfer, same Throwable distinct tasks, opaque stage never false success
保留本文件原有设计/元数据历史与相对链接，不改批准的规范正文
状态只根据本轮用户授权及实际完成的构建/测试证据更新
记录单Step单提交、完整需求/Manual/Literal审计与未启动运行环境边界
```

- Verification contribution: DTP focused/module tests；完整Spec TEST-001..023中本文件对应断言。
- After this file: 本文件责任具备，继续补齐本Step依赖后运行聚焦GREEN/模块门禁。

#### File 30 — `MODIFY egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/test/java/top/egon/cola/component/dtp/executor/ThreadManageTest.java`

- Purpose: 旧API/工厂/调容及新增入口兼容。
- Symbols: ThreadManageTest；retain old construction/aliases/name errors/queue resize checks。
- Repository evidence: 完整Spec §8/§14的同包现有结构；当前文件存在，保留既有接口/风格。
- Dependencies and consumers: 同模块原调用链、ContextManager/Observer/配置/测试；common只向下依赖，生产者保持原模块。
- Why now: 依赖前面契约/测试和原实现，逐步连通同一集成任务。
- Contract/signature changes: retain old construction/aliases/name errors/queue resize checks; unknown external observer capability rejects only new APIs。
- Input/output and state mapping: 执行输入来自可信提交点；null/空值/原Trace时点/真实终态依完整Spec §7/§9/§10；无业务持久化变化。
- Error and edge behavior: 本文件按上述算法保留原异常/取消/拒绝；故障不暗改业务结果，不忽略恢复，不新增身份默认值。
- Standards impact: MC-SCOPE-001、MC-TEST-001及§4.7相关命名/校验/Bean/日志/配置检查。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11；具体强制约束见§4.8逐项矩阵，当前文件仅在其实际角色适用，不添加无意义框架。
- Implementation pseudocode:

```java
retain old construction/aliases/name errors/queue resize checks
  unknown external observer capability rejects only new APIs
assert old 3-arg constructor, same-name unique pool and error strings remain compatible
null observer in third-party ManagedExecutor rejects only new helper calls
bound view routes to same registry pool after safe resize and rejects original way after shutdown
```

- Verification contribution: DTP focused/module tests；完整Spec TEST-001..023中本文件对应断言。
- After this file: 本文件责任具备，继续补齐本Step依赖后运行聚焦GREEN/模块门禁。

#### File 31 — `MODIFY egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/test/java/top/egon/cola/component/dtp/executor/virtual/BoundedVirtualThreadExecutorTest.java`

- Purpose: 虚拟上下文/许可并发验证。
- Symbols: BoundedVirtualThreadExecutorTest；assert cancellation does not clear another thread。
- Repository evidence: 完整Spec §8/§14的同包现有结构；当前文件存在，保留既有接口/风格。
- Dependencies and consumers: 同模块原调用链、ContextManager/Observer/配置/测试；common只向下依赖，生产者保持原模块。
- Why now: 依赖前面契约/测试和原实现，逐步连通同一集成任务。
- Contract/signature changes: assert cancellation does not clear another thread; underlying permits released once and old counters preserved。
- Input/output and state mapping: 执行输入来自可信提交点；null/空值/原Trace时点/真实终态依完整Spec §7/§9/§10；无业务持久化变化。
- Error and edge behavior: 本文件按上述算法保留原异常/取消/拒绝；故障不暗改业务结果，不忽略恢复，不新增身份默认值。
- Standards impact: MC-SCOPE-001、MC-TEST-001及§4.7相关命名/校验/Bean/日志/配置检查。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11；具体强制约束见§4.8逐项矩阵，当前文件仅在其实际角色适用，不添加无意义框架。
- Implementation pseudocode:

```java
assert cancellation does not clear another thread
  underlying permits released once and old counters preserved
use latches to hold worker under captured user and cancel from separate caller thread
assert caller context unchanged, worker finally restores and permit releases once
repeat failure/saturation/limit update with original metrics expectations intact
```

- Verification contribution: DTP focused/module tests；完整Spec TEST-001..023中本文件对应断言。
- After this file: 本文件责任具备，继续补齐本Step依赖后运行聚焦GREEN/模块门禁。

#### File 32 — `MODIFY egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/test/java/top/egon/cola/component/dtp/config/DynamicThreadPoolAutoPropertiesTest.java`

- Purpose: 新增配置绑定及profile一致。
- Symbols: DynamicThreadPoolAutoPropertiesTest；bind defaults/disable flags/invalid Duration。
- Repository evidence: 完整Spec §8/§14的同包现有结构；当前文件存在，保留既有接口/风格。
- Dependencies and consumers: 同模块原调用链、ContextManager/Observer/配置/测试；common只向下依赖，生产者保持原模块。
- Why now: 依赖前面契约/测试和原实现，逐步连通同一集成任务。
- Contract/signature changes: bind defaults/disable flags/invalid Duration; assert five new keys across eight resources。
- Input/output and state mapping: 执行输入来自可信提交点；null/空值/原Trace时点/真实终态依完整Spec §7/§9/§10；无业务持久化变化。
- Error and edge behavior: 本文件按上述算法保留原异常/取消/拒绝；故障不暗改业务结果，不忽略恢复，不新增身份默认值。
- Standards impact: MC-SCOPE-001、MC-TEST-001及§4.7相关命名/校验/Bean/日志/配置检查。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11；具体强制约束见§4.8逐项矩阵，当前文件仅在其实际角色适用，不添加无意义框架。
- Implementation pseudocode:

```java
bind defaults/disable flags/invalid Duration
  assert five new keys across eight resources
assert custom-thread-locals-enabled and exception enabled/uncaught/async defaults true
assert log-interval is positive Duration and toNanos never overflows after validation
flatten all eight resource profiles and compare the same five new key paths
```

- Verification contribution: DTP focused/module tests；完整Spec TEST-001..023中本文件对应断言。
- After this file: 本文件责任具备，继续补齐本Step依赖后运行聚焦GREEN/模块门禁。

#### File 33 — `MODIFY egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/test/java/top/egon/cola/component/dtp/config/DtpDependencyBoundaryTest.java`

- Purpose: 依赖边界及公共类型兼容。
- Symbols: DtpDependencyBoundaryTest；assert common-core no context-propagation dependency。
- Repository evidence: 完整Spec §8/§14的同包现有结构；当前文件存在，保留既有接口/风格。
- Dependencies and consumers: 同模块原调用链、ContextManager/Observer/配置/测试；common只向下依赖，生产者保持原模块。
- Why now: 依赖前面契约/测试和原实现，逐步连通同一集成任务。
- Contract/signature changes: assert common-core no context-propagation dependency; old raw Bean/ManagedExecutor contracts remain; existing classpath behavior retained。
- Input/output and state mapping: 执行输入来自可信提交点；null/空值/原Trace时点/真实终态依完整Spec §7/§9/§10；无业务持久化变化。
- Error and edge behavior: 本文件按上述算法保留原异常/取消/拒绝；故障不暗改业务结果，不忽略恢复，不新增身份默认值。
- Standards impact: MC-SCOPE-001、MC-TEST-001及§4.7相关命名/校验/Bean/日志/配置检查。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11；具体强制约束见§4.8逐项矩阵，当前文件仅在其实际角色适用，不添加无意义框架。
- Implementation pseudocode:

```java
  assert common-core no context-propagation dependency
  old raw Bean/ManagedExecutor contracts remain
  existing classpath behavior retained
```

- Verification contribution: DTP focused/module tests；完整Spec TEST-001..023中本文件对应断言。
- After this file: 本文件责任具备，继续补齐本Step依赖后运行聚焦GREEN/模块门禁。

#### File 34 — `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/java/top/egon/cola/archetype/source/light/start/config/async/AsyncConfiguration.java`

- Purpose: 标准源配置可选委托、原默认池保持。
- Symbols: AsyncConfiguration；retain old constructors。
- Repository evidence: 完整Spec §8/§14的同包现有结构；当前文件存在，保留既有接口/风格。
- Dependencies and consumers: 同模块原调用链、ContextManager/Observer/配置/测试；common只向下依赖，生产者保持原模块。
- Why now: 依赖前面契约/测试和原实现，逐步连通同一集成任务。
- Contract/signature changes: retain old constructors; Lombok final qualified optional handler Provider; open lazy Executor and full decorator manager Provider; no DTP import for native; fallback ExceptionLogger。
- Input/output and state mapping: 执行输入来自可信提交点；null/空值/原Trace时点/真实终态依完整Spec §7/§9/§10；无业务持久化变化。
- Error and edge behavior: 本文件按上述算法保留原异常/取消/拒绝；故障不暗改业务结果，不忽略恢复，不新增身份默认值。
- Standards impact: MC-SCOPE-001、MC-TEST-001及§4.7相关命名/校验/Bean/日志/配置检查。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11；具体强制约束见§4.8逐项矩阵，当前文件仅在其实际角色适用，不添加无意义框架。
- Implementation pseudocode:

```java
  retain old constructors
  Lombok final qualified optional handler Provider
  open lazy Executor and full decorator manager Provider
  no DTP import for native
  fallback ExceptionLogger
```

- Verification contribution: DTP focused/module tests；完整Spec TEST-001..023中本文件对应断言。
- After this file: 本文件责任具备，继续补齐本Step依赖后运行聚焦GREEN/模块门禁。

#### File 35 — `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-light/src/test/java/top/egon/cola/archetype/source/light/time/ApplicationExceptionClassificationTest.java`

- Purpose: 生产者可选组件及构造兼容验证。
- Symbols: ApplicationExceptionClassificationTest；assert old constructors still fallback。
- Repository evidence: 完整Spec §8/§14的同包现有结构；当前文件存在，保留既有接口/风格。
- Dependencies and consumers: 同模块原调用链、ContextManager/Observer/配置/测试；common只向下依赖，生产者保持原模块。
- Why now: 依赖前面契约/测试和原实现，逐步连通同一集成任务。
- Contract/signature changes: assert old constructors still fallback; supply optional Spring handler provider once; verify delegated exception and no method args logging。
- Input/output and state mapping: 执行输入来自可信提交点；null/空值/原Trace时点/真实终态依完整Spec §7/§9/§10；无业务持久化变化。
- Error and edge behavior: 本文件按上述算法保留原异常/取消/拒绝；故障不暗改业务结果，不忽略恢复，不新增身份默认值。
- Standards impact: MC-SCOPE-001、MC-TEST-001及§4.7相关命名/校验/Bean/日志/配置检查。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11；具体强制约束见§4.8逐项矩阵，当前文件仅在其实际角色适用，不添加无意义框架。
- Implementation pseudocode:

```java
  assert old constructors still fallback
  supply optional Spring handler provider once
  verify delegated exception and no method args logging
```

- Verification contribution: source tests/archetype checks；完整Spec TEST-001..023中本文件对应断言。
- After this file: 本文件责任具备，继续补齐本Step依赖后运行聚焦GREEN/模块门禁。

#### File 36 — `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-light-open/src/main/java/top/egon/cola/archetype/source/lightopen/start/config/async/AsyncConfiguration.java`

- Purpose: 标准源配置可选委托、原默认池保持。
- Symbols: AsyncConfiguration；retain old constructors。
- Repository evidence: 完整Spec §8/§14的同包现有结构；当前文件存在，保留既有接口/风格。
- Dependencies and consumers: 同模块原调用链、ContextManager/Observer/配置/测试；common只向下依赖，生产者保持原模块。
- Why now: 依赖前面契约/测试和原实现，逐步连通同一集成任务。
- Contract/signature changes: retain old constructors; Lombok final qualified optional handler Provider; open lazy Executor and full decorator manager Provider; no DTP import for native; fallback ExceptionLogger。
- Input/output and state mapping: 执行输入来自可信提交点；null/空值/原Trace时点/真实终态依完整Spec §7/§9/§10；无业务持久化变化。
- Error and edge behavior: 本文件按上述算法保留原异常/取消/拒绝；故障不暗改业务结果，不忽略恢复，不新增身份默认值。
- Standards impact: MC-SCOPE-001、MC-TEST-001及§4.7相关命名/校验/Bean/日志/配置检查。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11；具体强制约束见§4.8逐项矩阵，当前文件仅在其实际角色适用，不添加无意义框架。
- Implementation pseudocode:

```java
  retain old constructors
  Lombok final qualified optional handler Provider
  open lazy Executor and full decorator manager Provider
  no DTP import for native
  fallback ExceptionLogger
```

- Verification contribution: DTP focused/module tests；完整Spec TEST-001..023中本文件对应断言。
- After this file: 本文件责任具备，继续补齐本Step依赖后运行聚焦GREEN/模块门禁。

#### File 37 — `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-light-open/src/test/java/top/egon/cola/archetype/source/lightopen/time/ApplicationExceptionClassificationTest.java`

- Purpose: 生产者可选组件及构造兼容验证。
- Symbols: ApplicationExceptionClassificationTest；assert old constructors still fallback。
- Repository evidence: 完整Spec §8/§14的同包现有结构；当前文件存在，保留既有接口/风格。
- Dependencies and consumers: 同模块原调用链、ContextManager/Observer/配置/测试；common只向下依赖，生产者保持原模块。
- Why now: 依赖前面契约/测试和原实现，逐步连通同一集成任务。
- Contract/signature changes: assert old constructors still fallback; supply optional Spring handler provider once; verify delegated exception and no method args logging。
- Input/output and state mapping: 执行输入来自可信提交点；null/空值/原Trace时点/真实终态依完整Spec §7/§9/§10；无业务持久化变化。
- Error and edge behavior: 本文件按上述算法保留原异常/取消/拒绝；故障不暗改业务结果，不忽略恢复，不新增身份默认值。
- Standards impact: MC-SCOPE-001、MC-TEST-001及§4.7相关命名/校验/Bean/日志/配置检查。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11；具体强制约束见§4.8逐项矩阵，当前文件仅在其实际角色适用，不添加无意义框架。
- Implementation pseudocode:

```java
  assert old constructors still fallback
  supply optional Spring handler provider once
  verify delegated exception and no method args logging
```

- Verification contribution: source tests/archetype checks；完整Spec TEST-001..023中本文件对应断言。
- After this file: 本文件责任具备，继续补齐本Step依赖后运行聚焦GREEN/模块门禁。

#### File 38 — `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-starter/src/main/java/top/egon/cola/archetype/source/service/starter/config/async/AsyncConfiguration.java`

- Purpose: 标准源配置可选委托、原默认池保持。
- Symbols: AsyncConfiguration；retain old constructors。
- Repository evidence: 完整Spec §8/§14的同包现有结构；当前文件存在，保留既有接口/风格。
- Dependencies and consumers: 同模块原调用链、ContextManager/Observer/配置/测试；common只向下依赖，生产者保持原模块。
- Why now: 依赖前面契约/测试和原实现，逐步连通同一集成任务。
- Contract/signature changes: retain old constructors; Lombok final qualified optional handler Provider; open lazy Executor and full decorator manager Provider; no DTP import for native; fallback ExceptionLogger。
- Input/output and state mapping: 执行输入来自可信提交点；null/空值/原Trace时点/真实终态依完整Spec §7/§9/§10；无业务持久化变化。
- Error and edge behavior: 本文件按上述算法保留原异常/取消/拒绝；故障不暗改业务结果，不忽略恢复，不新增身份默认值。
- Standards impact: MC-SCOPE-001、MC-TEST-001及§4.7相关命名/校验/Bean/日志/配置检查。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11；具体强制约束见§4.8逐项矩阵，当前文件仅在其实际角色适用，不添加无意义框架。
- Implementation pseudocode:

```java
  retain old constructors
  Lombok final qualified optional handler Provider
  open lazy Executor and full decorator manager Provider
  no DTP import for native
  fallback ExceptionLogger
```

- Verification contribution: DTP focused/module tests；完整Spec TEST-001..023中本文件对应断言。
- After this file: 本文件责任具备，继续补齐本Step依赖后运行聚焦GREEN/模块门禁。

#### File 39 — `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-starter/src/test/java/top/egon/cola/archetype/source/service/time/ApplicationExceptionClassificationTest.java`

- Purpose: 生产者可选组件及构造兼容验证。
- Symbols: ApplicationExceptionClassificationTest；assert old constructors still fallback。
- Repository evidence: 完整Spec §8/§14的同包现有结构；当前文件存在，保留既有接口/风格。
- Dependencies and consumers: 同模块原调用链、ContextManager/Observer/配置/测试；common只向下依赖，生产者保持原模块。
- Why now: 依赖前面契约/测试和原实现，逐步连通同一集成任务。
- Contract/signature changes: assert old constructors still fallback; supply optional Spring handler provider once; verify delegated exception and no method args logging。
- Input/output and state mapping: 执行输入来自可信提交点；null/空值/原Trace时点/真实终态依完整Spec §7/§9/§10；无业务持久化变化。
- Error and edge behavior: 本文件按上述算法保留原异常/取消/拒绝；故障不暗改业务结果，不忽略恢复，不新增身份默认值。
- Standards impact: MC-SCOPE-001、MC-TEST-001及§4.7相关命名/校验/Bean/日志/配置检查。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11；具体强制约束见§4.8逐项矩阵，当前文件仅在其实际角色适用，不添加无意义框架。
- Implementation pseudocode:

```java
  assert old constructors still fallback
  supply optional Spring handler provider once
  verify delegated exception and no method args logging
```

- Verification contribution: source tests/archetype checks；完整Spec TEST-001..023中本文件对应断言。
- After this file: 本文件责任具备，继续补齐本Step依赖后运行聚焦GREEN/模块门禁。

#### File 40 — `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-service-open/egon-cola-source-service-open-starter/src/main/java/top/egon/cola/archetype/source/serviceopen/starter/config/async/AsyncConfiguration.java`

- Purpose: 标准源配置可选委托、原默认池保持。
- Symbols: AsyncConfiguration；retain old constructors。
- Repository evidence: 完整Spec §8/§14的同包现有结构；当前文件存在，保留既有接口/风格。
- Dependencies and consumers: 同模块原调用链、ContextManager/Observer/配置/测试；common只向下依赖，生产者保持原模块。
- Why now: 依赖前面契约/测试和原实现，逐步连通同一集成任务。
- Contract/signature changes: retain old constructors; Lombok final qualified optional handler Provider; open lazy Executor and full decorator manager Provider; no DTP import for native; fallback ExceptionLogger。
- Input/output and state mapping: 执行输入来自可信提交点；null/空值/原Trace时点/真实终态依完整Spec §7/§9/§10；无业务持久化变化。
- Error and edge behavior: 本文件按上述算法保留原异常/取消/拒绝；故障不暗改业务结果，不忽略恢复，不新增身份默认值。
- Standards impact: MC-SCOPE-001、MC-TEST-001及§4.7相关命名/校验/Bean/日志/配置检查。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11；具体强制约束见§4.8逐项矩阵，当前文件仅在其实际角色适用，不添加无意义框架。
- Implementation pseudocode:

```java
  retain old constructors
  Lombok final qualified optional handler Provider
  open lazy Executor and full decorator manager Provider
  no DTP import for native
  fallback ExceptionLogger
```

- Verification contribution: DTP focused/module tests；完整Spec TEST-001..023中本文件对应断言。
- After this file: 本文件责任具备，继续补齐本Step依赖后运行聚焦GREEN/模块门禁。

#### File 41 — `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-service-open/egon-cola-source-service-open-starter/src/test/java/top/egon/cola/archetype/source/serviceopen/time/ApplicationExceptionClassificationTest.java`

- Purpose: 生产者可选组件及构造兼容验证。
- Symbols: ApplicationExceptionClassificationTest；assert old constructors still fallback。
- Repository evidence: 完整Spec §8/§14的同包现有结构；当前文件存在，保留既有接口/风格。
- Dependencies and consumers: 同模块原调用链、ContextManager/Observer/配置/测试；common只向下依赖，生产者保持原模块。
- Why now: 依赖前面契约/测试和原实现，逐步连通同一集成任务。
- Contract/signature changes: assert old constructors still fallback; supply optional Spring handler provider once; verify delegated exception and no method args logging。
- Input/output and state mapping: 执行输入来自可信提交点；null/空值/原Trace时点/真实终态依完整Spec §7/§9/§10；无业务持久化变化。
- Error and edge behavior: 本文件按上述算法保留原异常/取消/拒绝；故障不暗改业务结果，不忽略恢复，不新增身份默认值。
- Standards impact: MC-SCOPE-001、MC-TEST-001及§4.7相关命名/校验/Bean/日志/配置检查。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11；具体强制约束见§4.8逐项矩阵，当前文件仅在其实际角色适用，不添加无意义框架。
- Implementation pseudocode:

```java
  assert old constructors still fallback
  supply optional Spring handler provider once
  verify delegated exception and no method args logging
```

- Verification contribution: source tests/archetype checks；完整Spec TEST-001..023中本文件对应断言。
- After this file: 本文件责任具备，继续补齐本Step依赖后运行聚焦GREEN/模块门禁。

#### File 42 — `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-starter/src/main/java/top/egon/cola/archetype/source/web/starter/config/async/AsyncConfiguration.java`

- Purpose: 标准源配置可选委托、原默认池保持。
- Symbols: AsyncConfiguration；retain old constructors。
- Repository evidence: 完整Spec §8/§14的同包现有结构；当前文件存在，保留既有接口/风格。
- Dependencies and consumers: 同模块原调用链、ContextManager/Observer/配置/测试；common只向下依赖，生产者保持原模块。
- Why now: 依赖前面契约/测试和原实现，逐步连通同一集成任务。
- Contract/signature changes: retain old constructors; Lombok final qualified optional handler Provider; open lazy Executor and full decorator manager Provider; no DTP import for native; fallback ExceptionLogger。
- Input/output and state mapping: 执行输入来自可信提交点；null/空值/原Trace时点/真实终态依完整Spec §7/§9/§10；无业务持久化变化。
- Error and edge behavior: 本文件按上述算法保留原异常/取消/拒绝；故障不暗改业务结果，不忽略恢复，不新增身份默认值。
- Standards impact: MC-SCOPE-001、MC-TEST-001及§4.7相关命名/校验/Bean/日志/配置检查。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11；具体强制约束见§4.8逐项矩阵，当前文件仅在其实际角色适用，不添加无意义框架。
- Implementation pseudocode:

```java
  retain old constructors
  Lombok final qualified optional handler Provider
  open lazy Executor and full decorator manager Provider
  no DTP import for native
  fallback ExceptionLogger
```

- Verification contribution: DTP focused/module tests；完整Spec TEST-001..023中本文件对应断言。
- After this file: 本文件责任具备，继续补齐本Step依赖后运行聚焦GREEN/模块门禁。

#### File 43 — `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-starter/src/test/java/top/egon/cola/archetype/source/web/time/ApplicationExceptionClassificationTest.java`

- Purpose: 生产者可选组件及构造兼容验证。
- Symbols: ApplicationExceptionClassificationTest；assert old constructors still fallback。
- Repository evidence: 完整Spec §8/§14的同包现有结构；当前文件存在，保留既有接口/风格。
- Dependencies and consumers: 同模块原调用链、ContextManager/Observer/配置/测试；common只向下依赖，生产者保持原模块。
- Why now: 依赖前面契约/测试和原实现，逐步连通同一集成任务。
- Contract/signature changes: assert old constructors still fallback; supply optional Spring handler provider once; verify delegated exception and no method args logging。
- Input/output and state mapping: 执行输入来自可信提交点；null/空值/原Trace时点/真实终态依完整Spec §7/§9/§10；无业务持久化变化。
- Error and edge behavior: 本文件按上述算法保留原异常/取消/拒绝；故障不暗改业务结果，不忽略恢复，不新增身份默认值。
- Standards impact: MC-SCOPE-001、MC-TEST-001及§4.7相关命名/校验/Bean/日志/配置检查。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11；具体强制约束见§4.8逐项矩阵，当前文件仅在其实际角色适用，不添加无意义框架。
- Implementation pseudocode:

```java
  assert old constructors still fallback
  supply optional Spring handler provider once
  verify delegated exception and no method args logging
```

- Verification contribution: source tests/archetype checks；完整Spec TEST-001..023中本文件对应断言。
- After this file: 本文件责任具备，继续补齐本Step依赖后运行聚焦GREEN/模块门禁。

#### File 44 — `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-web-open/egon-cola-source-web-open-starter/src/main/java/top/egon/cola/archetype/source/webopen/starter/config/async/AsyncConfiguration.java`

- Purpose: 标准源配置可选委托、原默认池保持。
- Symbols: AsyncConfiguration；retain old constructors。
- Repository evidence: 完整Spec §8/§14的同包现有结构；当前文件存在，保留既有接口/风格。
- Dependencies and consumers: 同模块原调用链、ContextManager/Observer/配置/测试；common只向下依赖，生产者保持原模块。
- Why now: 依赖前面契约/测试和原实现，逐步连通同一集成任务。
- Contract/signature changes: retain old constructors; Lombok final qualified optional handler Provider; open lazy Executor and full decorator manager Provider; no DTP import for native; fallback ExceptionLogger。
- Input/output and state mapping: 执行输入来自可信提交点；null/空值/原Trace时点/真实终态依完整Spec §7/§9/§10；无业务持久化变化。
- Error and edge behavior: 本文件按上述算法保留原异常/取消/拒绝；故障不暗改业务结果，不忽略恢复，不新增身份默认值。
- Standards impact: MC-SCOPE-001、MC-TEST-001及§4.7相关命名/校验/Bean/日志/配置检查。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11；具体强制约束见§4.8逐项矩阵，当前文件仅在其实际角色适用，不添加无意义框架。
- Implementation pseudocode:

```java
  retain old constructors
  Lombok final qualified optional handler Provider
  open lazy Executor and full decorator manager Provider
  no DTP import for native
  fallback ExceptionLogger
```

- Verification contribution: DTP focused/module tests；完整Spec TEST-001..023中本文件对应断言。
- After this file: 本文件责任具备，继续补齐本Step依赖后运行聚焦GREEN/模块门禁。

#### File 45 — `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-web-open/egon-cola-source-web-open-starter/src/test/java/top/egon/cola/archetype/source/webopen/time/ApplicationExceptionClassificationTest.java`

- Purpose: 生产者可选组件及构造兼容验证。
- Symbols: ApplicationExceptionClassificationTest；assert old constructors still fallback。
- Repository evidence: 完整Spec §8/§14的同包现有结构；当前文件存在，保留既有接口/风格。
- Dependencies and consumers: 同模块原调用链、ContextManager/Observer/配置/测试；common只向下依赖，生产者保持原模块。
- Why now: 依赖前面契约/测试和原实现，逐步连通同一集成任务。
- Contract/signature changes: assert old constructors still fallback; supply optional Spring handler provider once; verify delegated exception and no method args logging。
- Input/output and state mapping: 执行输入来自可信提交点；null/空值/原Trace时点/真实终态依完整Spec §7/§9/§10；无业务持久化变化。
- Error and edge behavior: 本文件按上述算法保留原异常/取消/拒绝；故障不暗改业务结果，不忽略恢复，不新增身份默认值。
- Standards impact: MC-SCOPE-001、MC-TEST-001及§4.7相关命名/校验/Bean/日志/配置检查。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11；具体强制约束见§4.8逐项矩阵，当前文件仅在其实际角色适用，不添加无意义框架。
- Implementation pseudocode:

```java
  assert old constructors still fallback
  supply optional Spring handler provider once
  verify delegated exception and no method args logging
```

- Verification contribution: source tests/archetype checks；完整Spec TEST-001..023中本文件对应断言。
- After this file: 本文件责任具备，继续补齐本Step依赖后运行聚焦GREEN/模块门禁。

#### File 46 — `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-light-open/lombok.config`

- Purpose: 构造参数Qualifier/Lazy传播。
- Symbols: lombok；retain Qualifier copy。
- Repository evidence: 完整Spec §8/§14的同包现有结构；当前文件存在，保留既有接口/风格。
- Dependencies and consumers: 同模块原调用链、ContextManager/Observer/配置/测试；common只向下依赖，生产者保持原模块。
- Why now: 依赖前面契约/测试和原实现，逐步连通同一集成任务。
- Contract/signature changes: retain Qualifier copy; add org.springframework.context.annotation.Lazy。
- Input/output and state mapping: 执行输入来自可信提交点；null/空值/原Trace时点/真实终态依完整Spec §7/§9/§10；无业务持久化变化。
- Error and edge behavior: 本文件按上述算法保留原异常/取消/拒绝；故障不暗改业务结果，不忽略恢复，不新增身份默认值。
- Standards impact: MC-SCOPE-001、MC-TEST-001及§4.7相关命名/校验/Bean/日志/配置检查。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11；具体强制约束见§4.8逐项矩阵，当前文件仅在其实际角色适用，不添加无意义框架。
- Implementation pseudocode:

```java
retain Qualifier copy
  add org.springframework.context.annotation.Lazy
retain config.stopBubbling and current existing copyable annotation rules
lombok.copyableAnnotations += org.springframework.beans.factory.annotation.Qualifier
lombok.copyableAnnotations += org.springframework.context.annotation.Lazy
```

- Verification contribution: reflection/source compilation；完整Spec TEST-001..023中本文件对应断言。
- After this file: 本文件责任具备，继续补齐本Step依赖后运行聚焦GREEN/模块门禁。

#### File 47 — `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-service-open/lombok.config`

- Purpose: 构造参数Qualifier/Lazy传播。
- Symbols: lombok；retain Qualifier copy。
- Repository evidence: 完整Spec §8/§14的同包现有结构；当前文件存在，保留既有接口/风格。
- Dependencies and consumers: 同模块原调用链、ContextManager/Observer/配置/测试；common只向下依赖，生产者保持原模块。
- Why now: 依赖前面契约/测试和原实现，逐步连通同一集成任务。
- Contract/signature changes: retain Qualifier copy; add org.springframework.context.annotation.Lazy。
- Input/output and state mapping: 执行输入来自可信提交点；null/空值/原Trace时点/真实终态依完整Spec §7/§9/§10；无业务持久化变化。
- Error and edge behavior: 本文件按上述算法保留原异常/取消/拒绝；故障不暗改业务结果，不忽略恢复，不新增身份默认值。
- Standards impact: MC-SCOPE-001、MC-TEST-001及§4.7相关命名/校验/Bean/日志/配置检查。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11；具体强制约束见§4.8逐项矩阵，当前文件仅在其实际角色适用，不添加无意义框架。
- Implementation pseudocode:

```java
retain Qualifier copy
  add org.springframework.context.annotation.Lazy
retain config.stopBubbling and current existing copyable annotation rules
lombok.copyableAnnotations += org.springframework.beans.factory.annotation.Qualifier
lombok.copyableAnnotations += org.springframework.context.annotation.Lazy
```

- Verification contribution: reflection/source compilation；完整Spec TEST-001..023中本文件对应断言。
- After this file: 本文件责任具备，继续补齐本Step依赖后运行聚焦GREEN/模块门禁。

#### File 48 — `MODIFY egon-cola-archetypes/source-projects/egon-cola-source-web-open/lombok.config`

- Purpose: 构造参数Qualifier/Lazy传播。
- Symbols: lombok；retain Qualifier copy。
- Repository evidence: 完整Spec §8/§14的同包现有结构；当前文件存在，保留既有接口/风格。
- Dependencies and consumers: 同模块原调用链、ContextManager/Observer/配置/测试；common只向下依赖，生产者保持原模块。
- Why now: 依赖前面契约/测试和原实现，逐步连通同一集成任务。
- Contract/signature changes: retain Qualifier copy; add org.springframework.context.annotation.Lazy。
- Input/output and state mapping: 执行输入来自可信提交点；null/空值/原Trace时点/真实终态依完整Spec §7/§9/§10；无业务持久化变化。
- Error and edge behavior: 本文件按上述算法保留原异常/取消/拒绝；故障不暗改业务结果，不忽略恢复，不新增身份默认值。
- Standards impact: MC-SCOPE-001、MC-TEST-001及§4.7相关命名/校验/Bean/日志/配置检查。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11；具体强制约束见§4.8逐项矩阵，当前文件仅在其实际角色适用，不添加无意义框架。
- Implementation pseudocode:

```java
retain Qualifier copy
  add org.springframework.context.annotation.Lazy
retain config.stopBubbling and current existing copyable annotation rules
lombok.copyableAnnotations += org.springframework.beans.factory.annotation.Qualifier
lombok.copyableAnnotations += org.springframework.context.annotation.Lazy
```

- Verification contribution: reflection/source compilation；完整Spec TEST-001..023中本文件对应断言。
- After this file: 本文件责任具备，继续补齐本Step依赖后运行聚焦GREEN/模块门禁。

#### File 49 — `MODIFY egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-admin/src/main/resources/application.yml`

- Purpose: 八profile配置键结构一致。
- Symbols: application；add context-propagation.custom-thread-locals-enabled。
- Repository evidence: 完整Spec §8/§14的同包现有结构；当前文件存在，保留既有接口/风格。
- Dependencies and consumers: 同模块原调用链、ContextManager/Observer/配置/测试；common只向下依赖，生产者保持原模块。
- Why now: 依赖前面契约/测试和原实现，逐步连通同一集成任务。
- Contract/signature changes: add context-propagation.custom-thread-locals-enabled; exception-handling.enabled/log-interval/uncaught-enabled/async-void-enabled; preserve original values。
- Input/output and state mapping: 执行输入来自可信提交点；null/空值/原Trace时点/真实终态依完整Spec §7/§9/§10；无业务持久化变化。
- Error and edge behavior: 本文件按上述算法保留原异常/取消/拒绝；故障不暗改业务结果，不忽略恢复，不新增身份默认值。
- Standards impact: MC-SCOPE-001、MC-TEST-001及§4.7相关命名/校验/Bean/日志/配置检查。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11；具体强制约束见§4.8逐项矩阵，当前文件仅在其实际角色适用，不添加无意义框架。
- Implementation pseudocode:

```java
  add context-propagation.custom-thread-locals-enabled
  exception-handling.enabled/log-interval/uncaught-enabled/async-void-enabled
  preserve original values
```

- Verification contribution: resource parity/package；完整Spec TEST-001..023中本文件对应断言。
- After this file: 本文件责任具备，继续补齐本Step依赖后运行聚焦GREEN/模块门禁。

#### File 50 — `MODIFY egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-admin/src/main/resources/application-dev.yml`

- Purpose: 八profile配置键结构一致。
- Symbols: application-dev；add context-propagation.custom-thread-locals-enabled。
- Repository evidence: 完整Spec §8/§14的同包现有结构；当前文件存在，保留既有接口/风格。
- Dependencies and consumers: 同模块原调用链、ContextManager/Observer/配置/测试；common只向下依赖，生产者保持原模块。
- Why now: 依赖前面契约/测试和原实现，逐步连通同一集成任务。
- Contract/signature changes: add context-propagation.custom-thread-locals-enabled; exception-handling.enabled/log-interval/uncaught-enabled/async-void-enabled; preserve original values。
- Input/output and state mapping: 执行输入来自可信提交点；null/空值/原Trace时点/真实终态依完整Spec §7/§9/§10；无业务持久化变化。
- Error and edge behavior: 本文件按上述算法保留原异常/取消/拒绝；故障不暗改业务结果，不忽略恢复，不新增身份默认值。
- Standards impact: MC-SCOPE-001、MC-TEST-001及§4.7相关命名/校验/Bean/日志/配置检查。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11；具体强制约束见§4.8逐项矩阵，当前文件仅在其实际角色适用，不添加无意义框架。
- Implementation pseudocode:

```java
  add context-propagation.custom-thread-locals-enabled
  exception-handling.enabled/log-interval/uncaught-enabled/async-void-enabled
  preserve original values
```

- Verification contribution: resource parity/package；完整Spec TEST-001..023中本文件对应断言。
- After this file: 本文件责任具备，继续补齐本Step依赖后运行聚焦GREEN/模块门禁。

#### File 51 — `MODIFY egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-admin/src/main/resources/application-test.yml`

- Purpose: 八profile配置键结构一致。
- Symbols: application-test；add context-propagation.custom-thread-locals-enabled。
- Repository evidence: 完整Spec §8/§14的同包现有结构；当前文件存在，保留既有接口/风格。
- Dependencies and consumers: 同模块原调用链、ContextManager/Observer/配置/测试；common只向下依赖，生产者保持原模块。
- Why now: 依赖前面契约/测试和原实现，逐步连通同一集成任务。
- Contract/signature changes: add context-propagation.custom-thread-locals-enabled; exception-handling.enabled/log-interval/uncaught-enabled/async-void-enabled; preserve original values。
- Input/output and state mapping: 执行输入来自可信提交点；null/空值/原Trace时点/真实终态依完整Spec §7/§9/§10；无业务持久化变化。
- Error and edge behavior: 本文件按上述算法保留原异常/取消/拒绝；故障不暗改业务结果，不忽略恢复，不新增身份默认值。
- Standards impact: MC-SCOPE-001、MC-TEST-001及§4.7相关命名/校验/Bean/日志/配置检查。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11；具体强制约束见§4.8逐项矩阵，当前文件仅在其实际角色适用，不添加无意义框架。
- Implementation pseudocode:

```java
  add context-propagation.custom-thread-locals-enabled
  exception-handling.enabled/log-interval/uncaught-enabled/async-void-enabled
  preserve original values
```

- Verification contribution: resource parity/package；完整Spec TEST-001..023中本文件对应断言。
- After this file: 本文件责任具备，继续补齐本Step依赖后运行聚焦GREEN/模块门禁。

#### File 52 — `MODIFY egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-admin/src/main/resources/application-prod.yml`

- Purpose: 八profile配置键结构一致。
- Symbols: application-prod；add context-propagation.custom-thread-locals-enabled。
- Repository evidence: 完整Spec §8/§14的同包现有结构；当前文件存在，保留既有接口/风格。
- Dependencies and consumers: 同模块原调用链、ContextManager/Observer/配置/测试；common只向下依赖，生产者保持原模块。
- Why now: 依赖前面契约/测试和原实现，逐步连通同一集成任务。
- Contract/signature changes: add context-propagation.custom-thread-locals-enabled; exception-handling.enabled/log-interval/uncaught-enabled/async-void-enabled; preserve original values。
- Input/output and state mapping: 执行输入来自可信提交点；null/空值/原Trace时点/真实终态依完整Spec §7/§9/§10；无业务持久化变化。
- Error and edge behavior: 本文件按上述算法保留原异常/取消/拒绝；故障不暗改业务结果，不忽略恢复，不新增身份默认值。
- Standards impact: MC-SCOPE-001、MC-TEST-001及§4.7相关命名/校验/Bean/日志/配置检查。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11；具体强制约束见§4.8逐项矩阵，当前文件仅在其实际角色适用，不添加无意义框架。
- Implementation pseudocode:

```java
  add context-propagation.custom-thread-locals-enabled
  exception-handling.enabled/log-interval/uncaught-enabled/async-void-enabled
  preserve original values
```

- Verification contribution: resource parity/package；完整Spec TEST-001..023中本文件对应断言。
- After this file: 本文件责任具备，继续补齐本Step依赖后运行聚焦GREEN/模块门禁。

#### File 53 — `MODIFY egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-test/src/main/resources/application.yml`

- Purpose: 八profile配置键结构一致。
- Symbols: application；add context-propagation.custom-thread-locals-enabled。
- Repository evidence: 完整Spec §8/§14的同包现有结构；当前文件存在，保留既有接口/风格。
- Dependencies and consumers: 同模块原调用链、ContextManager/Observer/配置/测试；common只向下依赖，生产者保持原模块。
- Why now: 依赖前面契约/测试和原实现，逐步连通同一集成任务。
- Contract/signature changes: add context-propagation.custom-thread-locals-enabled; exception-handling.enabled/log-interval/uncaught-enabled/async-void-enabled; preserve original values。
- Input/output and state mapping: 执行输入来自可信提交点；null/空值/原Trace时点/真实终态依完整Spec §7/§9/§10；无业务持久化变化。
- Error and edge behavior: 本文件按上述算法保留原异常/取消/拒绝；故障不暗改业务结果，不忽略恢复，不新增身份默认值。
- Standards impact: MC-SCOPE-001、MC-TEST-001及§4.7相关命名/校验/Bean/日志/配置检查。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11；具体强制约束见§4.8逐项矩阵，当前文件仅在其实际角色适用，不添加无意义框架。
- Implementation pseudocode:

```java
  add context-propagation.custom-thread-locals-enabled
  exception-handling.enabled/log-interval/uncaught-enabled/async-void-enabled
  preserve original values
```

- Verification contribution: resource parity/package；完整Spec TEST-001..023中本文件对应断言。
- After this file: 本文件责任具备，继续补齐本Step依赖后运行聚焦GREEN/模块门禁。

#### File 54 — `MODIFY egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-test/src/main/resources/application-dev.yml`

- Purpose: 八profile配置键结构一致。
- Symbols: application-dev；add context-propagation.custom-thread-locals-enabled。
- Repository evidence: 完整Spec §8/§14的同包现有结构；当前文件存在，保留既有接口/风格。
- Dependencies and consumers: 同模块原调用链、ContextManager/Observer/配置/测试；common只向下依赖，生产者保持原模块。
- Why now: 依赖前面契约/测试和原实现，逐步连通同一集成任务。
- Contract/signature changes: add context-propagation.custom-thread-locals-enabled; exception-handling.enabled/log-interval/uncaught-enabled/async-void-enabled; preserve original values。
- Input/output and state mapping: 执行输入来自可信提交点；null/空值/原Trace时点/真实终态依完整Spec §7/§9/§10；无业务持久化变化。
- Error and edge behavior: 本文件按上述算法保留原异常/取消/拒绝；故障不暗改业务结果，不忽略恢复，不新增身份默认值。
- Standards impact: MC-SCOPE-001、MC-TEST-001及§4.7相关命名/校验/Bean/日志/配置检查。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11；具体强制约束见§4.8逐项矩阵，当前文件仅在其实际角色适用，不添加无意义框架。
- Implementation pseudocode:

```java
  add context-propagation.custom-thread-locals-enabled
  exception-handling.enabled/log-interval/uncaught-enabled/async-void-enabled
  preserve original values
```

- Verification contribution: resource parity/package；完整Spec TEST-001..023中本文件对应断言。
- After this file: 本文件责任具备，继续补齐本Step依赖后运行聚焦GREEN/模块门禁。

#### File 55 — `MODIFY egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-test/src/main/resources/application-test.yml`

- Purpose: 八profile配置键结构一致。
- Symbols: application-test；add context-propagation.custom-thread-locals-enabled。
- Repository evidence: 完整Spec §8/§14的同包现有结构；当前文件存在，保留既有接口/风格。
- Dependencies and consumers: 同模块原调用链、ContextManager/Observer/配置/测试；common只向下依赖，生产者保持原模块。
- Why now: 依赖前面契约/测试和原实现，逐步连通同一集成任务。
- Contract/signature changes: add context-propagation.custom-thread-locals-enabled; exception-handling.enabled/log-interval/uncaught-enabled/async-void-enabled; preserve original values。
- Input/output and state mapping: 执行输入来自可信提交点；null/空值/原Trace时点/真实终态依完整Spec §7/§9/§10；无业务持久化变化。
- Error and edge behavior: 本文件按上述算法保留原异常/取消/拒绝；故障不暗改业务结果，不忽略恢复，不新增身份默认值。
- Standards impact: MC-SCOPE-001、MC-TEST-001及§4.7相关命名/校验/Bean/日志/配置检查。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11；具体强制约束见§4.8逐项矩阵，当前文件仅在其实际角色适用，不添加无意义框架。
- Implementation pseudocode:

```java
  add context-propagation.custom-thread-locals-enabled
  exception-handling.enabled/log-interval/uncaught-enabled/async-void-enabled
  preserve original values
```

- Verification contribution: resource parity/package；完整Spec TEST-001..023中本文件对应断言。
- After this file: 本文件责任具备，继续补齐本Step依赖后运行聚焦GREEN/模块门禁。

#### File 56 — `MODIFY egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-test/src/main/resources/application-prod.yml`

- Purpose: 八profile配置键结构一致。
- Symbols: application-prod；add context-propagation.custom-thread-locals-enabled。
- Repository evidence: 完整Spec §8/§14的同包现有结构；当前文件存在，保留既有接口/风格。
- Dependencies and consumers: 同模块原调用链、ContextManager/Observer/配置/测试；common只向下依赖，生产者保持原模块。
- Why now: 依赖前面契约/测试和原实现，逐步连通同一集成任务。
- Contract/signature changes: add context-propagation.custom-thread-locals-enabled; exception-handling.enabled/log-interval/uncaught-enabled/async-void-enabled; preserve original values。
- Input/output and state mapping: 执行输入来自可信提交点；null/空值/原Trace时点/真实终态依完整Spec §7/§9/§10；无业务持久化变化。
- Error and edge behavior: 本文件按上述算法保留原异常/取消/拒绝；故障不暗改业务结果，不忽略恢复，不新增身份默认值。
- Standards impact: MC-SCOPE-001、MC-TEST-001及§4.7相关命名/校验/Bean/日志/配置检查。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11；具体强制约束见§4.8逐项矩阵，当前文件仅在其实际角色适用，不添加无意义框架。
- Implementation pseudocode:

```java
  add context-propagation.custom-thread-locals-enabled
  exception-handling.enabled/log-interval/uncaught-enabled/async-void-enabled
  preserve original values
```

- Verification contribution: resource parity/package；完整Spec TEST-001..023中本文件对应断言。
- After this file: 本文件责任具备，继续补齐本Step依赖后运行聚焦GREEN/模块门禁。

#### File 57 — `MODIFY egon-cola-components/egon-cola-component-dynamic-thread-pool/README.md`

- Purpose: 业务简单接入、兼容及边界说明。
- Symbols: README；document normal execute/submit, accessor qualifier, root helpers, request-local bound Executor, Async optional delegation, cancellation/hidden-stage limits。
- Repository evidence: 完整Spec §8/§14的同包现有结构；当前文件存在，保留既有接口/风格。
- Dependencies and consumers: 同模块原调用链、ContextManager/Observer/配置/测试；common只向下依赖，生产者保持原模块。
- Why now: 依赖前面契约/测试和原实现，逐步连通同一集成任务。
- Contract/signature changes: document normal execute/submit, accessor qualifier, root helpers, request-local bound Executor, Async optional delegation, cancellation/hidden-stage limits; no runtime claim。
- Input/output and state mapping: 执行输入来自可信提交点；null/空值/原Trace时点/真实终态依完整Spec §7/§9/§10；无业务持久化变化。
- Error and edge behavior: 本文件按上述算法保留原异常/取消/拒绝；故障不暗改业务结果，不忽略恢复，不新增身份默认值。
- Standards impact: MC-SCOPE-001、MC-TEST-001及§4.7相关命名/校验/Bean/日志/配置检查。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11；具体强制约束见§4.8逐项矩阵，当前文件仅在其实际角色适用，不添加无意义框架。
- Implementation pseudocode:

```java
document normal execute/submit, accessor qualifier, root helpers, request-local bound Executor, Async optional delegation, cancellation/hidden-stage limits
  no runtime claim
show standard Runnable/Callable submission with no manual MDC copying or log catch
show @Qualifier dtpContextAccessor registration and request-local contextualExecutor
explain CF root vs appended stage, cancellation cleanup, optional Async and no automatic business retry
```

- Verification contribution: doc review；完整Spec TEST-001..023中本文件对应断言。
- After this file: 本文件责任具备，继续补齐本Step依赖后运行聚焦GREEN/模块门禁。

#### File 58 — `MODIFY egon-cola-components/egon-cola-component-dynamic-thread-pool/README.zh-CN.md`

- Purpose: 业务简单接入、兼容及边界说明。
- Symbols: README.zh-CN；document normal execute/submit, accessor qualifier, root helpers, request-local bound Executor, Async optional delegation, cancellation/hidden-stage limits。
- Repository evidence: 完整Spec §8/§14的同包现有结构；当前文件存在，保留既有接口/风格。
- Dependencies and consumers: 同模块原调用链、ContextManager/Observer/配置/测试；common只向下依赖，生产者保持原模块。
- Why now: 依赖前面契约/测试和原实现，逐步连通同一集成任务。
- Contract/signature changes: document normal execute/submit, accessor qualifier, root helpers, request-local bound Executor, Async optional delegation, cancellation/hidden-stage limits; no runtime claim。
- Input/output and state mapping: 执行输入来自可信提交点；null/空值/原Trace时点/真实终态依完整Spec §7/§9/§10；无业务持久化变化。
- Error and edge behavior: 本文件按上述算法保留原异常/取消/拒绝；故障不暗改业务结果，不忽略恢复，不新增身份默认值。
- Standards impact: MC-SCOPE-001、MC-TEST-001及§4.7相关命名/校验/Bean/日志/配置检查。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11；具体强制约束见§4.8逐项矩阵，当前文件仅在其实际角色适用，不添加无意义框架。
- Implementation pseudocode:

```java
document normal execute/submit, accessor qualifier, root helpers, request-local bound Executor, Async optional delegation, cancellation/hidden-stage limits
  no runtime claim
写出原Runnable/Callable入口无需逐任务复制MDC或记录错误
示例包含qualified Accessor与请求局部contextualExecutor，禁止当单例缓存
明确CF root/后续阶段、取消清理、Async一次委托、真实Redis未验证
```

- Verification contribution: doc review；完整Spec TEST-001..023中本文件对应断言。
- After this file: 本文件责任具备，继续补齐本Step依赖后运行聚焦GREEN/模块门禁。

#### File 59 — `MODIFY docs/egon/spec/2026-10-21-10-10-dtp-context-and-exception-management.md`

- Purpose: Accepted到Implemented及验证证据。
- Symbols: 2026-10-21-10-10-dtp-context-and-exception-management；update status/relationship/evidence only。
- Repository evidence: 完整Spec §8/§14的同包现有结构；当前文件存在，保留既有接口/风格。
- Dependencies and consumers: 同模块原调用链、ContextManager/Observer/配置/测试；common只向下依赖，生产者保持原模块。
- Why now: 依赖前面契约/测试和原实现，逐步连通同一集成任务。
- Contract/signature changes: update status/relationship/evidence only; original design history remains; no unrelated document edits。
- Input/output and state mapping: 执行输入来自可信提交点；null/空值/原Trace时点/真实终态依完整Spec §7/§9/§10；无业务持久化变化。
- Error and edge behavior: 本文件按上述算法保留原异常/取消/拒绝；故障不暗改业务结果，不忽略恢复，不新增身份默认值。
- Standards impact: MC-SCOPE-001、MC-TEST-001及§4.7相关命名/校验/Bean/日志/配置检查。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11；具体强制约束见§4.8逐项矩阵，当前文件仅在其实际角色适用，不添加无意义框架。
- Implementation pseudocode:

```java
update status/relationship/evidence only
  original design history remains
  no unrelated document edits
保留本文件原有设计/元数据历史与相对链接，不改批准的规范正文
状态只根据本轮用户授权及实际完成的构建/测试证据更新
记录单Step单提交、完整需求/Manual/Literal审计与未启动运行环境边界
```

- Verification contribution: Spec/Plan strict validators；完整Spec TEST-001..023中本文件对应断言。
- After this file: 本文件责任具备，继续补齐本Step依赖后运行聚焦GREEN/模块门禁。

#### File 60 — `MODIFY docs/egon/spec/2026-10-21-09-38-dtp-unified-task-exception-handling.md`

- Purpose: 旧草案仅标Superseded。
- Symbols: 2026-10-21-09-38-dtp-unified-task-exception-handling；update status/relationship/evidence only。
- Repository evidence: 完整Spec §8/§14的同包现有结构；当前文件存在，保留既有接口/风格。
- Dependencies and consumers: 同模块原调用链、ContextManager/Observer/配置/测试；common只向下依赖，生产者保持原模块。
- Why now: 依赖前面契约/测试和原实现，逐步连通同一集成任务。
- Contract/signature changes: update status/relationship/evidence only; original design history remains; no unrelated document edits。
- Input/output and state mapping: 执行输入来自可信提交点；null/空值/原Trace时点/真实终态依完整Spec §7/§9/§10；无业务持久化变化。
- Error and edge behavior: 本文件按上述算法保留原异常/取消/拒绝；故障不暗改业务结果，不忽略恢复，不新增身份默认值。
- Standards impact: MC-SCOPE-001、MC-TEST-001及§4.7相关命名/校验/Bean/日志/配置检查。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11；具体强制约束见§4.8逐项矩阵，当前文件仅在其实际角色适用，不添加无意义框架。
- Implementation pseudocode:

```java
update status/relationship/evidence only
  original design history remains
  no unrelated document edits
保留本文件原有设计/元数据历史与相对链接，不改批准的规范正文
状态只根据本轮用户授权及实际完成的构建/测试证据更新
记录单Step单提交、完整需求/Manual/Literal审计与未启动运行环境边界
```

- Verification contribution: Spec/Plan strict validators；完整Spec TEST-001..023中本文件对应断言。
- After this file: 本文件责任具备，继续补齐本Step依赖后运行聚焦GREEN/模块门禁。

#### File 61 — `CREATE docs/egon/plan/2026-10-21-10-56-dtp-context-exception-implementation.md`

- Purpose: 执行记录、单提交审计证据。
- Symbols: 2026-10-21-10-56-dtp-context-exception-implementation；update status/relationship/evidence only。
- Repository evidence: 完整Spec §8/§14的同包现有结构；当前文件尚无，按已批准同包角色创建。
- Dependencies and consumers: 同模块原调用链、ContextManager/Observer/配置/测试；common只向下依赖，生产者保持原模块。
- Why now: 依赖前面契约/测试和原实现，逐步连通同一集成任务。
- Contract/signature changes: update status/relationship/evidence only; original design history remains; no unrelated document edits。
- Input/output and state mapping: 执行输入来自可信提交点；null/空值/原Trace时点/真实终态依完整Spec §7/§9/§10；无业务持久化变化。
- Error and edge behavior: 本文件按上述算法保留原异常/取消/拒绝；故障不暗改业务结果，不忽略恢复，不新增身份默认值。
- Standards impact: MC-SCOPE-001、MC-TEST-001及§4.7相关命名/校验/Bean/日志/配置检查。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11；具体强制约束见§4.8逐项矩阵，当前文件仅在其实际角色适用，不添加无意义框架。
- Implementation pseudocode:

```java
update status/relationship/evidence only
  original design history remains
  no unrelated document edits
保留本文件原有设计/元数据历史与相对链接，不改批准的规范正文
状态只根据本轮用户授权及实际完成的构建/测试证据更新
记录单Step单提交、完整需求/Manual/Literal审计与未启动运行环境边界
```

- Verification contribution: Spec/Plan strict validators；完整Spec TEST-001..023中本文件对应断言。
- After this file: 本文件责任具备，继续补齐本Step依赖后运行聚焦GREEN/模块门禁。

#### File 62 — `MODIFY egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/test/java/top/egon/cola/component/dtp/contract/RemainingComponentContractTest.java`

- Purpose: 原治理库存与明确批准的技术异常一致。
- Symbols: handwrittenEnumsAreAllGovernedAndContextFailureUsesCommonHierarchy。
- Repository evidence: 原测试按源码枚举所有Exception并断言为空，已明确检测本次新增类型。
- Dependencies and consumers: CommonException、ResultCode.SYSTEM_ERROR、已批准DtpContextPropagationException。
- Why now: 完整starter回归发现精确库存需要随已批准新增类型更新。
- Contract/signature changes: 只允许这个明确技术异常，验证其公共系统错误状态、原cause和阶段。
- Input/output and state mapping: 源码库存为一项；无业务error code或wire变化。
- Error and edge behavior: 未声明的其他异常仍使库存断言失败。
- Standards impact: MC-REUSE-001、MC-SCOPE-001、MC-TEST-001，复用公共异常治理。
- Literal rule enforcement: Rule 1, Rule 5, Rule 6, Rule 11，保持组件/公共层方向。
- Implementation pseudocode:

```java
assert sourceExceptionInventory containsExactly DtpContextPropagationException
assert CommonException.class.isAssignableFrom(DtpContextPropagationException.class)
assert contextFailure.code == ResultCode.SYSTEM_ERROR.code and category == SYSTEM
assert originalCause identity preserved and phase/key bounded technical metadata only
```

- Verification contribution: RemainingComponentContractTest及完整starter回归。
- After this file: 新增明确类型有严格治理证明，不再错误断言模块永远不能有异常。

- Validation working directory: /Users/mario/SelfProject/Egon-COLA
- Verification command: §8列出的mvnw聚焦测试、package、依赖图及原生成检查
- Expected result: 所有受影响测试/编译通过；无意外skip、无服务启动、无无关diff
- Failure returns to: 本Step对应文件及测试；运行失败先定位并修复，不跳过批准要求
- Completion criteria: REQ-001..010全部有代码/测试证据，Manual/Literal检查通过，diff已审阅
- Rollback: 完成后一个聚焦提交可path-limited revert；不动历史SQL和用户数据
- Commit paths: `egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/test/java/top/egon/cola/component/dtp/contract/RemainingComponentContractTest.java`，`egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/test/java/top/egon/cola/component/dtp/context/DtpTaskExceptionHandlingTest.java`，`egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/pom.xml`，`egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/main/java/top/egon/cola/component/dtp/context/DtpTaskContextBO.java`，`egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/main/java/top/egon/cola/component/dtp/exception/DtpContextPropagationException.java`，`egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/main/java/top/egon/cola/component/dtp/context/DtpTaskContextManager.java`，`egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/test/java/top/egon/cola/component/dtp/context/DtpTaskContextManagerTest.java`，`egon-cola-components/egon-cola-component-common/egon-cola-component-common-core/src/main/java/top/egon/cola/component/common/core/exception/ExceptionLogger.java`，`egon-cola-components/egon-cola-component-common/egon-cola-component-common-core/src/test/java/top/egon/cola/component/common/exception/ExceptionClassificationTest.java`，`egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/main/java/top/egon/cola/component/dtp/exception/DtpTaskFailureBO.java`，`egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/main/java/top/egon/cola/component/dtp/exception/DtpTaskExceptionHandler.java`，`egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/main/java/top/egon/cola/component/dtp/exception/LoggingDtpTaskExceptionHandler.java`，`egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/main/java/top/egon/cola/component/dtp/config/DynamicThreadPoolAutoProperties.java`，`egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/main/java/top/egon/cola/component/dtp/context/DtpTaskObserver.java`，`egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/test/java/top/egon/cola/component/dtp/executor/ThreadManageCompletableFutureTest.java`，`egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/main/java/top/egon/cola/component/dtp/executor/ManagedExecutorRegistry.java`，`egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/main/java/top/egon/cola/component/dtp/executor/ThreadManage.java`，`egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/main/java/top/egon/cola/component/dtp/executor/managed/DtpThreadPoolTaskExecutor.java`，`egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/main/java/top/egon/cola/component/dtp/executor/virtual/BoundedVirtualThreadExecutor.java`，`egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/main/java/top/egon/cola/component/dtp/context/DtpTaskDecorator.java`，`egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/main/java/top/egon/cola/component/dtp/exception/DtpAsyncUncaughtExceptionHandler.java`，`egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/main/java/top/egon/cola/component/dtp/config/DtpAsyncConfigurer.java`，`egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/main/java/top/egon/cola/component/dtp/config/DtpTaskContextAutoConfiguration.java`，`egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/main/java/top/egon/cola/component/dtp/config/DynamicThreadPoolAutoConfig.java`，`egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`，`egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/main/java/top/egon/cola/component/dtp/metrics/DtpMeterBinder.java`，`egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/test/java/top/egon/cola/component/dtp/config/DtpTaskContextAutoConfigurationTest.java`，`egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/test/java/top/egon/cola/component/dtp/config/DtpAsyncIntegrationTest.java`，`egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/test/java/top/egon/cola/component/dtp/context/DtpContextPropagationTest.java`，`egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/test/java/top/egon/cola/component/dtp/context/DtpTaskObserverTest.java`，`egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/test/java/top/egon/cola/component/dtp/executor/ThreadManageTest.java`，`egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/test/java/top/egon/cola/component/dtp/executor/virtual/BoundedVirtualThreadExecutorTest.java`，`egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/test/java/top/egon/cola/component/dtp/config/DynamicThreadPoolAutoPropertiesTest.java`，`egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-starter/src/test/java/top/egon/cola/component/dtp/config/DtpDependencyBoundaryTest.java`，`egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/java/top/egon/cola/archetype/source/light/start/config/async/AsyncConfiguration.java`，`egon-cola-archetypes/source-projects/egon-cola-source-light/src/test/java/top/egon/cola/archetype/source/light/time/ApplicationExceptionClassificationTest.java`，`egon-cola-archetypes/source-projects/egon-cola-source-light-open/src/main/java/top/egon/cola/archetype/source/lightopen/start/config/async/AsyncConfiguration.java`，`egon-cola-archetypes/source-projects/egon-cola-source-light-open/src/test/java/top/egon/cola/archetype/source/lightopen/time/ApplicationExceptionClassificationTest.java`，`egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-starter/src/main/java/top/egon/cola/archetype/source/service/starter/config/async/AsyncConfiguration.java`，`egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-starter/src/test/java/top/egon/cola/archetype/source/service/time/ApplicationExceptionClassificationTest.java`，`egon-cola-archetypes/source-projects/egon-cola-source-service-open/egon-cola-source-service-open-starter/src/main/java/top/egon/cola/archetype/source/serviceopen/starter/config/async/AsyncConfiguration.java`，`egon-cola-archetypes/source-projects/egon-cola-source-service-open/egon-cola-source-service-open-starter/src/test/java/top/egon/cola/archetype/source/serviceopen/time/ApplicationExceptionClassificationTest.java`，`egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-starter/src/main/java/top/egon/cola/archetype/source/web/starter/config/async/AsyncConfiguration.java`，`egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-starter/src/test/java/top/egon/cola/archetype/source/web/time/ApplicationExceptionClassificationTest.java`，`egon-cola-archetypes/source-projects/egon-cola-source-web-open/egon-cola-source-web-open-starter/src/main/java/top/egon/cola/archetype/source/webopen/starter/config/async/AsyncConfiguration.java`，`egon-cola-archetypes/source-projects/egon-cola-source-web-open/egon-cola-source-web-open-starter/src/test/java/top/egon/cola/archetype/source/webopen/time/ApplicationExceptionClassificationTest.java`，`egon-cola-archetypes/source-projects/egon-cola-source-light-open/lombok.config`，`egon-cola-archetypes/source-projects/egon-cola-source-service-open/lombok.config`，`egon-cola-archetypes/source-projects/egon-cola-source-web-open/lombok.config`，`egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-admin/src/main/resources/application.yml`，`egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-admin/src/main/resources/application-dev.yml`，`egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-admin/src/main/resources/application-test.yml`，`egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-admin/src/main/resources/application-prod.yml`，`egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-test/src/main/resources/application.yml`，`egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-test/src/main/resources/application-dev.yml`，`egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-test/src/main/resources/application-test.yml`，`egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-test/src/main/resources/application-prod.yml`，`egon-cola-components/egon-cola-component-dynamic-thread-pool/README.md`，`egon-cola-components/egon-cola-component-dynamic-thread-pool/README.zh-CN.md`，`docs/egon/spec/2026-10-21-10-10-dtp-context-and-exception-management.md`，`docs/egon/spec/2026-10-21-09-38-dtp-unified-task-exception-handling.md`，`docs/egon/plan/2026-10-21-10-56-dtp-context-exception-implementation.md`
- Commit: feat(dtp): unify task context propagation and exception handling

## 8. Test, Validation, and Quality Gates

| Gate/order | Working directory | Command or method | Scope | Expected result | Failure returns to | Requirements/runtime boundary |
| --- | --- | --- | --- | --- | --- | --- |
| RED | 仓库根 | ./mvnw -B -ntp -f egon-cola-components/pom.xml -pl :egon-cola-component-dynamic-thread-pool-starter -am -Dtest=DtpTaskExceptionHandlingTest -Dsurefire.failIfNoSpecifiedTests=false test | 旧submit新日志断言 | 预期因缺少安全ERROR日志失败 | File 1/Observer | REQ-005；无外部服务 |
| GREEN | 仓库根 | 同上选择新增Context/CF/Async测试及原测试 | 相关模块 | 新用例通过，原契约不退化 | 当前Step | REQ-001..009 |
| Dependencies | 仓库根 | ./mvnw -B -ntp -f egon-cola-components/pom.xml -pl :egon-cola-component-dynamic-thread-pool-starter -am dependency:tree -Dincludes=io.micrometer:context-propagation,org.springframework:spring-core | 已批准依赖版本 | 沿BOM解析、无升级 | POM | REQ-009 |
| Package | 仓库根 | ./mvnw -B -ntp -f egon-cola-components/pom.xml -pl :egon-cola-component-common-core,:egon-cola-component-dynamic-thread-pool-starter,:egon-cola-component-dynamic-thread-pool-admin,:egon-cola-component-dynamic-thread-pool-test -am package | 受影响reactor | 编译/测试通过，环境skip如实记录 | 对应文件 | REQ-001..010 |
| Producer | 仓库根 | ./scripts/generate_archetypes.sh generate；./scripts/generate_archetypes.sh check；已有family/native/dependency边界检查 | 六种源工程与派生产物 | 原生产者验证通过，不启动应用 | 源配置/测试 | REQ-007/008 |
| Source compile | 各精确源工程根 | 已有mvnw -f对应pom test/package，限定受影响测试并保留反应器依赖 | 六种producer | 新旧构造/可选委托/Qualifier/Lazy通过 | 生产者 | REQ-007/008 |
| Static | 仓库根 | git diff --check；Spec/Plan严格validator；最终人工逐项审计 | 全部本任务路径 | 无空白/追踪错误/无关改动 | 对应文件 | REQ-010 |

无scratch harness；直接使用JUnit和原脚本，scripts/work无本任务遗留。真实Redis/数据库、告警投递和用户应用运行不在本轮授权验证范围。

## 9. Migration, Compatibility, Rollout, and Rollback

无数据库迁移、无业务数据变更。普通API/原Bean/Trace时点/拒绝/取消保留；仅登记TL进入新能力。旧生成项目不批量修改。新版本可关闭额外TL/异常策略；完整退回通过本任务单提交revert。无push/merge/deploy。

## 10. Requirement-to-Step Traceability Matrix

| Requirement | Effective Spec section | Steps | Files | Tests/gates | Completion evidence |
| --- | --- | --- | --- | --- | --- |
| REQ-001 | 完整Spec §4/§7/§9/§14 | Step 1 | §5精确目标 | TEST-001..023对应行/§8门禁 | 结束时记录实际命令和结果 |
| REQ-002 | 完整Spec §4/§7/§9/§14 | Step 1 | §5精确目标 | TEST-001..023对应行/§8门禁 | 结束时记录实际命令和结果 |
| REQ-003 | 完整Spec §4/§7/§9/§14 | Step 1 | §5精确目标 | TEST-001..023对应行/§8门禁 | 结束时记录实际命令和结果 |
| REQ-004 | 完整Spec §4/§7/§9/§14 | Step 1 | §5精确目标 | TEST-001..023对应行/§8门禁 | 结束时记录实际命令和结果 |
| REQ-005 | 完整Spec §4/§7/§9/§14 | Step 1 | §5精确目标 | TEST-001..023对应行/§8门禁 | 结束时记录实际命令和结果 |
| REQ-006 | 完整Spec §4/§7/§9/§14 | Step 1 | §5精确目标 | TEST-001..023对应行/§8门禁 | 结束时记录实际命令和结果 |
| REQ-007 | 完整Spec §4/§7/§9/§14 | Step 1 | §5精确目标 | TEST-001..023对应行/§8门禁 | 结束时记录实际命令和结果 |
| REQ-008 | 完整Spec §4/§7/§9/§14 | Step 1 | §5精确目标 | TEST-001..023对应行/§8门禁 | 结束时记录实际命令和结果 |
| REQ-009 | 完整Spec §4/§7/§9/§14 | Step 1 | §5精确目标 | TEST-001..023对应行/§8门禁 | 结束时记录实际命令和结果 |
| REQ-010 | 完整Spec §4/§7/§9/§14 | Step 1 | §5精确目标 | TEST-001..023对应行/§8门禁 | 结束时记录实际命令和结果 |

## 11. Risks, Blockers, and User Decisions

| ID | Risk or decision | Impacted Steps/files | Evidence | Owner | Status/action |
| --- | --- | --- | --- | --- | --- |
| APPROVAL-001 | 精确依赖和整体实施 | Step 1 | 用户本轮明确同意 | User | Closed |
| COMPAT-001 | 单提交及旧API保留 | Step 1 | 用户AGENTS/原Spec | Agent | 执行门禁 |
| RUNTIME-001 | 实际应用/Redis验收 | 无启动路径 | 用户不要启动项目 | User | 明确不执行，离线证据不冒充运行验收 |

## 12. Review and Acceptance

全部要求/必要路径已覆盖；没有新增业务接口、层、数据库或全局TL行为。按当前直接实施授权执行本Plan，不增加审批往返。以下为实施前设计审查，执行结束另附实际结果，不把预期当成测试通过。

### 12.5 Blocking Manual Check

| Check ID | Applicability | Status | Evidence | Finding | Required action/exception |
| --- | --- | --- | --- | --- | --- |
| MC-ARCH-001 | Applicable | PASS | 原component包、六种源profile；family/native边界脚本exit0 | 无新模块、业务层、SQL或混合架构 | None |
| MC-REUSE-001 | Applicable | PASS | TraceContext/ExceptionLogger/原Observer/标准Accessor；ContextManager与真实终态测试 | 复用既有能力，未复制TTL/全局扫描/agent体系 | None |
| MC-DEP-001 | Applicable | PASS | 用户精确批准；dependency:tree=io.micrometer:context-propagation:1.1.4:compile、Spring6.2.19 | 唯一新增声明，沿现有BOM，无版本升级；指标仍optional | None |
| MC-NAME-001 | Applicable | PASS | 新增TaskContextBO/AccessorValueBO/FailureBO；Manager/Handler/Exception及内部生命周期状态 | 公开载体语义明确，私有Scope/Observation状态不对外作DTO | None |
| MC-VALID-001 | Applicable | PASS | Jakarta约束/Report组+ValidationUtils、注册key及scope身份guard、配置负值/溢出测试 | 非代理技术边界手工约束，业务原命令校验保留 | None |
| MC-MODEL-001 | Applicable | PASS | 三个技术BO完整Lombok class；值列表防御复制；Throwable/value排除toString；构造编译通过 | 未用record冒充可变状态；无无意义继承/载体链 | None |
| MC-CONVERT-001 | Not applicable | N/A | 仅本组件技术快照/日志投影，无业务层、PO/DTO或持久化转换 | 无新Converter；没有BeanUtils/反射/JSON对象拷贝 | None |
| MC-LOG-001 | Applicable | PASS | 默认策略/Common Logger重载、预算/抑制/脱敏/保留字段测试、reporter故障测试 | Slf4j安全出口，不记录任务参数/完整MDC/TL值；保留原异常 | None |
| MC-BEAN-001 | Applicable | PASS | 显式Bean名；依赖Provider final+Qualifier、Lombok@Autowired；六种源构造反射测试 | 原native无参/open Executor构造保留；三个open复制Lazy和Qualifier | None |
| MC-UTIL-001 | Applicable | PASS | POM/导入diff；JDK集合/并发/java.time及已批准Micrometer协议 | 无新工具库/Utils/并行治理框架 | None |
| MC-JSON-001 | Applicable | PASS | 原REST/Redis契约/枚举/快照回归通过；内存BO不进入wire | 原Jackson协议不变，没有历史SQL或序列化输出变更 | None |
| MC-TIME-001 | Applicable | PASS | observedAt=Instant UTC Z、Duration校验、nanoTime限频/耗时；日志用例通过 | 不改业务日期/数据库/默认时区，阈值不受DST影响 | None |
| MC-CONFIG-001 | Applicable | PASS | 八个profile五个新键；属性绑定/非法Duration、独立开关测试 | typed属性、默认值和配置metadata已编译，保留旧键/值 | None |
| MC-PATTERN-001 | Applicable | PASS | 原Decorator/Observer/Adapter；标准Accessor策略/组合；单异常Handler策略 | 技术变化点有具体实现和故障覆盖，无额外抽象层 | None |
| MC-SCOPE-001 | Applicable | PASS | git diff与Plan精确路径核对；无.sql变更；生成文件仅ignored派生 | 只改本逻辑任务；用户数据/外部工作/服务状态不动 | None |
| MC-TEST-001 | Applicable | PASS | 最终install：293通过/17跳过；六种源工程18通过；generate/check和三边界检查通过 | 虚拟安装失败/取消许可、CF ForkJoin载体、CallerRuns、真实Async均覆盖 | None |
| MC-BLOCKER-001 | Applicable | PASS | 所有REQ和以上逐项检查均已复核；无未关闭技术或依赖批准项 | 运行验收未执行属于用户明确边界，不冒充真实Redis/应用验收 | None |

### 12.6 Final verdict

PASS — Ready for user review

上面的Ready verdict是实施前设计审查历史；当前Step已完成，以下为实际执行与交付审计。

### 12.7 Execution evidence and final conformance audit

| Step | State | Baseline / branch | Commit | Paths | Validation |
| --- | --- | --- | --- | --- | --- |
| Step 1 | Committed | 4ef1cf380 / codex/dtp-context-exceptions | 包含本Plan的本任务唯一提交；以git log -1 --format=%H -- 本Plan路径查询，最终回复提供hash | §5全部精确路径，无额外路径 | 下列实际日志/结果及§12.5逐项审计 |

RED：/tmp/egon-dtp-context-red.log，旧submit只有WARN类名诊断，新安全ERROR断言按预期失败。GREEN：核心7例通过；后续真实Async/CF/虚拟/不透明载体和全部相关回归均通过。

| Gate | Actual command/log | Observed result |
| --- | --- | --- |
| 最终编译/打包/本地安装 | ./mvnw -B -ntp -f egon-cola-components/pom.xml -pl :egon-cola-component-common-core,:egon-cola-component-dynamic-thread-pool-starter,:egon-cola-component-dynamic-thread-pool-admin,:egon-cola-component-dynamic-thread-pool-test -am install；/tmp/egon-dtp-context-verified-build.log | BUILD SUCCESS，293通过/17环境skip，0失败/错误 |
| 源工程六族 | ./mvnw -B -ntp -o -f 各源工程pom.xml -am -Dtest=ApplicationExceptionClassificationTest -Dsurefire.failIfNoSpecifiedTests=false test；/tmp/egon-dtp-source-release-各族.log | 六次BUILD SUCCESS，各3例，共18通过；无应用启动 |
| 依赖图 | §8 dependency:tree；/tmp/egon-dtp-context-dependencies.log | context-propagation1.1.4:compile、spring-core6.2.19；不升级 |
| archetype派生 | ./scripts/generate_archetypes.sh generate；check | 7族标准派生，check确认deterministic matches current workspace；无手改/提交.generated |
| 静态架构/依赖 | check-archetype-family-boundaries.py；check-native-archetype-boundaries.py；check-archetype-dependency-ownership.py | 三项exit0，原profile/依赖边界保持 |
| 文档/差异 | Spec/Plan --strict；git diff --check；精确路径清单检查 | 文档结构/追踪有效，无SQL和无关文件 |

| Module | Tests | Passed | Skipped |
| --- | --- | --- | --- |
| egon-cola-component-common-trace | 8 | 8 | 0 |
| egon-cola-component-common-core | 87 | 87 | 0 |
| egon-cola-component-common-trace-spring-boot-starter | 14 | 14 | 0 |
| egon-cola-component-dynamic-thread-pool-starter | 146 | 146 | 0 |
| egon-cola-component-dynamic-thread-pool-admin | 34 | 34 | 0 |
| egon-cola-component-dynamic-thread-pool-test | 21 | 4 | 17 |
| six source families | 18 | 18 | 0 |

原先“模块无自定义异常”的库存假设随明确批准的技术异常更新，仍严格要求CommonException系统状态。其余变化均保持批准设计；原adapter/ContextAwareExecutorService/ObservedFutureTask无需无语义改写，已用原回归证明。新增内部Future完成回调用于组件拥有的虚拟Future安装失败，不添加业务SPI或反射拆JDK。

| Requirement ID | Effective Spec source | Expected behavior | Commit evidence | Code/symbol evidence | Test/validation evidence | Status | Gap/impact/next action |
| --- | --- | --- | --- | --- | --- | --- | --- |
| REQ-001 | 完整Spec §4/§7/§9/§14 | 每次捕获/执行安装/恢复 | 本任务唯一提交 | ContextManager、Observer、Decorator | 并发Scope、同Runnable复用、CallerRuns回归 | Satisfied | None |
| REQ-002 | 完整Spec §4/§7/§9/§14 | Trace/MDC与旧时点兼容 | 本任务唯一提交 | TraceContext复用、explicitContext/effectiveTrace | 旧包装和未知私有快照、完整MDC恢复 | Satisfied | None |
| REQ-003 | 完整Spec §4/§7/§9/§14 | 标准一次登记/缺失值清除 | 本任务唯一提交 | qualified Accessor、固定key缓存/副本 | 重复/保留/缺清理key、可变副本、未限定Bean | Satisfied | None |
| REQ-004 | 完整Spec §4/§7/§9/§14 | 安装回退/恢复/取消线程正确 | 本任务唯一提交 | Scope、RootTask、虚拟Future完成与许可外层finally | 局部安装/恢复、多线程关闭、取消/安装交错 | Satisfied | None |
| REQ-005 | 完整Spec §4/§7/§9/§14 | 真实终态统一报告/不篡改结果 | 本任务唯一提交 | 单Handler、Common Logger、Async桥、UC组合 | 无get日志、同Throwable多任务、报告器错误、预算 | Satisfied | None |
| REQ-006 | 完整Spec §4/§7/§9/§14 | CF注册时点绑定且无伪终态 | 本任务唯一提交 | runAsync/supplyAsync/contextualExecutor、opaque guard | 外部complete/取消/安装失败、ForkJoin载体独立状态 | Satisfied | None |
| REQ-007 | 完整Spec §4/§7/§9/§14 | 原配置/Bean/默认执行器兼容 | 本任务唯一提交 | 缺省Decorator/原Configurer可选委托 | 真实Async代理、force/用户配置、6族构造/Lazy/Qualifier | Satisfied | None |
| REQ-008 | 完整Spec §4/§7/§9/§14 | 原API/监控/调容/Redis兼容 | 本任务唯一提交 | 原registry/adapter/queue/update保持 | 原全部starter/admin/样例回归、外部ManagedExecutor | Satisfied | None |
| REQ-009 | 完整Spec §4/§7/§9/§14 | 最小依赖/隔离/有界诊断 | 本任务唯一提交 | 标准SPI1.1.4、无全局Registry、固定phase指标 | 元数据白名单、无敏感标签、metrics可缺失、依赖图 | Satisfied | None |
| REQ-010 | 完整Spec §4/§7/§9/§14 | 完整交付及授权范围验证 | 本任务唯一提交 | 本Plan/Spec/双语README/一次提交 | 实际构建/18源工程测试/生成静态检查，无运行服务 | Satisfied | None |

### 12.8 Final Literal Rule Gate

| Literal rule | Applicability | Status | Final diff/path/symbol evidence | Final test/static evidence | Finding | Required action/exception |
| --- | --- | --- | --- | --- | --- | --- |
| Rule 1 | Applicable | PASS | BO/Handler/Manager/Exception与内部生命周期状态角色明确 | 命名/源码检查、编译 | 没有歧义业务载体或类层级扩散 | None |
| Rule 2 | Applicable | PASS | 配置Jakarta约束、Report分组/ValidationUtils及技术guard | 配置负例/真实Async/注册故障测试 | 保持原入口校验，程序化内部状态按原生不变量构造 | None |
| Rule 3 | Applicable | PASS | 技术BO完整Lombok class、防御复制、不打印cause/value | 所有模块和源构造编译/副本测试 | 无伪不可变record或无意义模型转换 | None |
| Rule 4 | Applicable | PASS | 日志类Slf4j、显式Bean名、DI类RAC@Autowired/final+Qualifier | 6族反射/Lazy及真实Context测试 | 保留旧构造和Provider延迟；基础技术状态按现有程序化构造风格 | None |
| Rule 5 | Applicable | PASS | JDK/既有Common/已批准标准Micrometer | POM与导入diff、依赖边界检查 | 没有新增工具库或重复Utils | None |
| Rule 6 | Applicable | PASS | 原Jackson wire/枚举保持，BO仅本机技术对象 | 原序列化/Redis/OpenAPI及日志元数据测试 | 没有JSON对象拷贝或敏感payload输出 | None |
| Rule 7 | Applicable | PASS | 8个profile同5键、typed新配置 | 完整profile键结构及绑定测试 | 无单环境漏键 | None |
| Rule 9 | Applicable | PASS | Decorator/Observer/Adapter+标准Accessor策略/组合+单Handler | 终态/扩展/故障/真实代理覆盖 | 复杂技术变化点有实际参与者，未加业务继承层 | None |
| Rule 10 | Applicable | PASS | Instant UTC Z/Duration/nanoTime | Duration负值溢出/日志格式及源码检查 | 无新java.util日期或隐式时区转换 | None |
| Rule 11 | Applicable | PASS | 原component及六族精确source树 | 三静态边界检查、生成check、源码和编译 | 无第三种架构、SQL或业务模块扩张 | None |

### 12.9 Delivery boundaries and final verdict

2026-10-21 12:57 Asia/Shanghai：311个实际用例通过，17个环境依赖用例跳过。未执行应用/Redis/数据库/浏览器/部署/真实告警投递，不将离线证据当作运行验收。scratch harness：未创建，无scripts/work遗留。没有push/PR/merge；用户工作与原数据保持。仅本任务精确路径进入唯一提交，提交后用git show与git status复核。

PASS — Implementation conforms to the effective Specs
