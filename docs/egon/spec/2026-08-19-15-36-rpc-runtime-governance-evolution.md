# RPC 运行时治理、调用模型与生命周期演进规格

| Field | Value |
| --- | --- |
| Document | `2026-08-19-15-36-rpc-runtime-governance-evolution.md` |
| Template Version | `4` |
| Status | `Review` |
| Type | `Architecture` |
| Complexity | `Complex` |
| Complexity Drivers | `RPC 公共 Java API 与兼容性、DDC/Redis 最终一致发现、模式内多实例重试未知结果、五种负载均衡、一致性 Hash 键、Access Guard Provider 并发限流、同步/异步双调用、泛化调用、HTTP/2 多路复用、CGLIB 代理、跨生命周期部分失败` |
| Created | `2026-08-19 15:36 CST` |
| Updated | `2026-08-21 10:19 CST` |
| Owner | `Egon-COLA platform owner / User` |
| Repository | `Egon-COLA` |
| Scope | `egon-cola-component-rpc-starter、egon-cola-component-rpc-ddc-adapter、egon-cola-component-access-guard-starter 的限流算法扩展、RPC/Guard README 与聚焦测试；DDC Registry/Redis 和 Gateway RPC 数据面仅作边界上下文` |
| Change Surface | `两种 Reference 注解的公共字段归一与模式策略/工厂、Consumer 调用执行器/发现快照/负载均衡/模式内换节点/异步/泛化/CGLIB/共享 Channel、Provider 生命周期与 Access Guard 限流接线/算法工厂/UNAVAILABLE 映射、配置/异常/文档/测试；不改业务 Protobuf wire、DDC Admin 存储模型、Gateway 路由实现、关系库或前端` |
| Affected Chapters | `§7, §8, §9, §10, §11, §13, §14, §15, §16, §17, §18` |
| Source Requirement | `2026-08-19 用户提出的 11 项 RPC 模块改造需求；2026-08-21 用户对 DEC-008–DEC-014 的 7 项确认` |
| Baseline Revision | `main@4ae5419c4504250436b98886c03271889a8f2879；2026-08-21 10:19 CST dirty-worktree snapshot，现有未提交 Gateway/其他 Spec/Plan 改动不属于本规格修改` |
| Amends | `[DDC 单机闭环与轻量 RPC §4.2、§9.3、§11.4、§15.2/§15.3、§17.2、§20.2、§21.2](../../superpowers/specs/2026-07-24-ddc-standalone-rpc-framework-design.md#42-明确不实现)`；`[统一服务模型 §3.4、§4.3、§4.4](../../superpowers/specs/2026-07-26-ddc-rpc-gateway-unified-service-model-design.md#34-策略层servicecallpolicy解决-g3)` |
| Supersedes | `[Gateway BIZ/APP 与 DDC 直连 RPC Spec](./2026-08-15-16-57-gateway-biz-app-scope-direct-rpc-design.md) §1、§3.2、§4 REQ-005–REQ-011、§5 DEC-003–DEC-005、§7.3–§7.5、§8 RPC Consumer 子树、§9 ANN-001/ANN-002、§13.2、§15 Direct 治理、§16–§18 中的 RPC Reference/Direct 行为；其 Gateway BIZ/APP 安全与凭证中继设计不被替换` |
| Depends On | `[DDC 单机闭环与轻量 RPC §8 服务注册中心](../../superpowers/specs/2026-07-24-ddc-standalone-rpc-framework-design.md#8-ddc-服务注册中心)`；`[GWS-04 §5–§13 Gateway RPC 数据面](../../superpowers/specs/2026-07-25-gateway-engine-rpc-design.md#5-动态-grpc-handler)`；`[Gateway BIZ/APP 与 DDC 直连 RPC Spec §7.1–§7.2、§15 Security](./2026-08-15-16-57-gateway-biz-app-scope-direct-rpc-design.md#71-architecture-overview)` |
| Related Specs | `[统一服务模型与管理端优化](../../superpowers/specs/2026-07-26-ddc-rpc-gateway-unified-service-model-design.md)`；`[Gateway Provider 发现与负载均衡](../../superpowers/specs/2026-07-25-gateway-provider-discovery-load-balancing-design.md)` |
| Related Plans | `None` |

## 1. Summary

本规格把当前“两个注解、两个 Proxy Factory、两个 Channel Manager、部分未接线策略字段”的 RPC Consumer，演进为一条统一调用内核：两种 Reference 仍保持源码兼容，但先解析为共同的 `RpcReferencePolicy` 和带模式差异的 `RpcReferenceDefinition`；`RpcReferenceStrategyFactory` 在代理创建时固定选择 Gateway 或 Direct 策略，单次逻辑调用绝不跨模式。`RpcInvocationExecutor` 统一执行阻塞、异步、模式内换节点、Deadline、可用性故障重试、终态失败和本地 fallback。Consumer 复用当前 DDC `Redis Topic + 定期 RPC 全量拉取` 订阅，不创建第二套远程缓存；进程内保留按精确服务键的不可变 Provider/Gateway 快照，并以五种指定算法选择尚未尝试的同模式实例。

Provider 继续主动注册和 heartbeat，心跳间隔仍归 `egon.cola.component.rpc.provider` 配置；DDC 只验证租约身份、续期 TTL、被动过期和发布变更事件，不主动探测 Provider。`@EgonRpcProvider` 继续保持无字段的纯发现标记；业务实现方法使用现有 `@RateLimitGuard(ruleId, key)`，限流参数和算法归 Access Guard rule 配置。Guard 内以 Strategy + Factory 补齐漏桶、令牌桶、滑动窗口，RPC 通过异常适配器把 `RATE_LIMITED` 映射为 Provider-stage `UNAVAILABLE`。Typed Contract 同时允许 Protobuf 响应和 `CompletionStage<Protobuf响应>`，泛化调用通过原始 Protobuf bytes/Descriptor 调用既有 unary `fullMethodName`；其唯一格式是 gRPC canonical `fully.qualified.Service/Method`，不增加第二套 wire 服务。

用户已在 2026-08-21 关闭原 `DEC-008`–`DEC-014`：模式由客户端 Reference 配置且调用期间不切换；仅 channel acquisition/Provider-stage `UNAVAILABLE` 等可用性故障可按负载均衡更换同模式实例；业务错误原样报错；异步使用 `CompletionStage<Response>`；泛化使用 canonical `fullMethodName + Protobuf bytes/Descriptor`；一致性 Hash 由业务 `RpcLoadBalanceKeyResolver` 提供；`FAIL_OPEN` 返回 `null`。本稿因此进入 `Review`，等待用户逐章审核，但不代表已经 `Accepted` 或实现。

## 2. Background and Current State

### 2.1 Business and user context

RPC 组件已经有 Provider 租约、Gateway/Direct 两条引用、DDC Redis 订阅和 unary gRPC 基础链，但能力不完整：Direct 只轮询且单次调用；Gateway 重试只允许幂等且只识别 Gateway-stage `UNAVAILABLE`；注解的 `retries/loadBalance/failStrategy` 大部分未进入执行链；代理是 JDK Proxy；没有异步 Java 方法、泛化客户端、共享 Channel 池或 Provider 自身限流。

用户要求把这些能力作为 RPC 组件的一致运行时能力，而不是继续依赖 Gateway 代偿。业务代码明确承担重试幂等，例如使用唯一单据编号让重复写覆盖；框架因此不能再把 `@EgonRpcMethod.idempotent` 作为是否重试的硬门槛，但仍必须限制可重试故障、总次数和总 Deadline，避免对业务拒绝与参数错误盲目重放。

### 2.2 Repository evidence

以下证据均为当前 dirty-worktree 的静态源码/文档证据；未启动 DDC、Redis、Gateway、Provider 或 Consumer，未证明真实网络吞吐、Redis 丢消息恢复、连接并发或停机排空时间。为保持表格可读，下列别名是规范化的 repository-relative 路径前缀，表内 `别名/剩余路径` 可直接还原为实际文件：

| Path alias | Exact repository-relative prefix |
| --- | --- |
| `RPC_JAVA` | `egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/main/java/top/egon/cola/component/rpc` |
| `RPC_DDC_JAVA` | `egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-ddc-adapter/src/main/java/top/egon/cola/component/rpc/ddc` |
| `GUARD_JAVA` | `egon-cola-components/egon-cola-component-access-guard-starter/src/main/java/top/egon/cola/component/accessguard` |
| `DDC_STARTER_JAVA` | `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-starter/src/main/java/top/egon/cola/component/ddc` |
| `DDC_ADMIN_JAVA` | `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin/src/main/java/top/egon/cola/component/ddc/admin` |

| Evidence ID | Classification | Exact path/symbol/decision/command | Observed fact | Design significance | Verification limit/freshness |
| --- | --- | --- | --- | --- | --- |
| `EVD-001` | User decision | 需求 1 + 2026-08-21 决定 1 | 两个 Reference 要抽取公共字段并采用策略；Direct/Gateway 模式由客户端配置，失败只按 LB 换同模式实例，全部失败才报错，绝不跨模式 | 必须统一声明解析、模式策略工厂和候选排除；不得增加 Direct→Gateway 状态 | 用户已关闭原 `DEC-008` |
| `EVD-002` | Static repository | `RPC_JAVA/annotation/EgonRpcReference.java:18-74` | Gateway Reference 已声明 timeout、retries、loadBalance、group、version、fallbackBean、failStrategy | 这些字段是公共策略基线，但当前执行只使用 timeout | 静态声明不证明运行语义 |
| `EVD-003` | Static repository | `RPC_JAVA/annotation/EgonRpcDirectReference.java:16-28` | Direct 只有 bizCode、appCode、env、group、version、timeout | Direct 缺少与 Gateway 一致的 retries/loadBalance/终态策略 | 不证明外部项目的注解使用量 |
| `EVD-004` | Static repository | `RPC_JAVA/consumer/proxy/EgonRpcReferenceBeanPostProcessor.java:39-80,86-129` | 同字段双注解被拒；两个模式分别走两个 Factory；字段必须是接口 | 保留双注解和接口契约，内部改成模式策略比公开合并注解更兼容 | 仅扫描 Spring Bean 字段 |
| `EVD-005` | Static repository | `RPC_JAVA/consumer/proxy/RpcConsumerProxyFactory.java:99-117` | 使用 `java.lang.reflect.Proxy`，每个代理持有 InvocationHandler | 用户要求 CGLIB；可在不开放类契约的情况下让 CGLIB 实现接口 | 未做微基准 |
| `EVD-006` | Static repository | `RPC_JAVA/consumer/proxy/RpcConsumerInvocationHandler.java:73-143` | 每次调用线性查 Method；只阻塞 `blockingUnaryCall`；只有幂等 Gateway-stage UNAVAILABLE 重试 | 需要预编译 Method Plan、同步/异步执行器和新的重试判定 | 未证明当前热路径耗时占比 |
| `EVD-007` | Static repository | `RPC_JAVA/consumer/provider/RpcConsumerProviderManager.java:41-80,201-255` | 每个精确 `RpcProviderQuery` 共享一份订阅和 `activeProviders` 不可变列表 | Consumer 本地缓存已经存在，应扩展而不是再建缓存 | 当前只保留已建 Channel 的端点 |
| `EVD-008` | Static repository | `DDC_STARTER_JAVA/listener/registry/DdcManagedRegistrySubscription.java:77-98,107-143`；`DDC_STARTER_JAVA/listener/registry/DdcInstanceSubscription.java:69-102` | 先监听 Redis Topic，首次全量拉取，事件合并刷新，固定周期全量对账，失败时按租约过期本地摘除 | 已满足“事件监听 + 定期 DDC 拉取”的核心机制 | Redis 实际投递/断线未验证 |
| `EVD-009` | Static repository | `RPC_DDC_JAVA/autoconfigure/DdcRpcAutoConfiguration.java:134-171`；`DDC_STARTER_JAVA/autoconfigure/properties/DdcProperties.java:624-669` | RPC DDC Adapter 复用 `DdcRegistrySubscriptionCoordinator`，默认每 10 秒对账 | 不应复制轮询线程；保持 DDC Adapter 边界 | 当前间隔属于 DDC Registry 配置而非 RPC Consumer 配置 |
| `EVD-010` | Static repository | `DDC_ADMIN_JAVA/repository/DdcServiceRegistryRedisRepository.java:51-75,78-124,255-312,484-492` | 注册/注销/过期发布事件；heartbeat 只续 TTL；查询惰性清理过期租约 | DDC 是被动租约账本，不是主动健康探针 | 未验证生产 Redis 原子性/延迟 |
| `EVD-011` | Static repository | `RPC_JAVA/config/EgonRpcProperties.java:76-86`；`RPC_JAVA/provider/lifecycle/RpcProviderLifecycle.java:220-231` | heartbeat 间隔/lease/优雅关闭均已由 RPC Provider 配置，Provider 定时主动上报 | REQ-007 主要是固化与完善，而不是迁移到 DDC 配置 | 当前使用秒粒度固定延迟 |
| `EVD-012` | Static repository | `RPC_JAVA/provider/lifecycle/RpcProviderLifecycle.java:68-145` | Server 先启动但 Handler 不可用；注册成功后可用；停止先清可用、停恢复/心跳、注销、再 `Server.shutdown/awaitTermination` | 已有优雅启停骨架，可增状态和 Spring callback，不需重写 Server | 无高并发排空实测 |
| `EVD-013` | Static repository | `RPC_JAVA/annotation/LoadBalance.java:19-32`；`RPC_JAVA/consumer/provider/RpcConsumerProviderManager.java:141-163` | 枚举含 RR/SWRR/RANDOM/LEAST_IN_FLIGHT，但 Direct 运行时固定 RR；加权随机和一致性 Hash 不存在 | 必须让声明进入运行链并补两种算法；保留旧值兼容 | `LoadBalance.java` 当前只有用户未提交的 Javadoc 包名调整 |
| `EVD-014` | Static repository | `RPC_JAVA/annotation/EgonRpcService.java:33-39`；`DDC_STARTER_JAVA/format/ServiceInstanceMetaCodec.java:50-54,205-227`；`RPC_DDC_JAVA/registry/{DdcRpcProviderDirectory,DdcRpcGatewayDirectory}.java` | Contract/metadata 有 weight；DDC metadata 可解码 `gateway.weight`，但两个 RPC Directory 都丢弃结果 | Direct Provider 和 Gateway endpoint 的 weighted 算法可复用现有 metadata wire，不改 DDC Proto | 当前 Provider Contract 默认 weight 尚未稳定写入 metadata |
| `EVD-015` | Static repository + User decision | `RPC_JAVA/annotation/EgonRpcProvider.java`；2026-08-21 决定 3 | Provider 注解明确是永久无字段的发现标记；用户指定限流复用 Guard 注解 | `@EgonRpcProvider` 保持不变；限流声明使用实现方法上的 `@RateLimitGuard` | 需要验证 Spring AOP 代理调用链 |
| `EVD-016` | Static repository | `RPC_JAVA/provider/{binding/RpcProviderBeanScanner,server/RpcServerServiceDefinitionFactory}.java`；`GUARD_JAVA/adapter/aop/{SpringAopAccessGuardAdvisor,GuardBindingResolver}.java` | Scanner 保留 Spring 代理 bean 并用 `AopUtils.getTargetClass` 找 Contract；Server 反射调用该代理；Guard Advisor 用 most-specific method 解析实现方法注解 | Provider 方法上的 `@RateLimitGuard` 可在业务调用前生效；RPC 只需映射 Guard rejection 并支持 CompletionStage | 静态链路，仍需 RPC+Guard 组件测试证明实际 advice 命中 |
| `EVD-017` | Static repository | `RPC_JAVA/consumer/channel/RpcConsumerChannelFactory.java:29-49`；`RPC_JAVA/consumer/{gateway/RpcConsumerGatewayManager,provider/RpcConsumerProviderManager}.java` 的 `connect/reconcile/drain` | 每个 Manager/Query 会创建 ManagedChannel；没有跨 Query/模式共享池 | gRPC 本身能在一个 HTTP/2 Channel 多路复用，但当前所有权阻止充分复用 | 未测远端 SETTINGS_MAX_CONCURRENT_STREAMS |
| `EVD-018` | Static repository | `RPC_JAVA/contract/validation/RpcContractValidator.java:84-132`；`RPC_JAVA/contract/descriptor/RpcMethodDescriptor.java:8-15` | Java 方法必须 Message→Message，且已有 Proto MethodDescriptor | 可扩展为 Message 或 CompletionStage<Message>，也可用 descriptor/raw bytes 泛化 | 仍只允许 unary，符合本次范围 |
| `EVD-019` | Static repository | `README.zh-CN.md:240-243,427-434,529-539` | 文档明确直连不重试、Gateway 幂等重试，且把完整策略列为 Roadmap | README 必须与新合同同步更新 | 外部文档不在仓库内不可见 |
| `EVD-020` | Static repository + User decision | `docs/egon/spec/2026-08-15-16-57-gateway-biz-app-scope-direct-rpc-design.md §3.2/§4/§16`；2026-08-21 决定 1 | 前一 Review Spec 禁止两种路径自动 fallback；用户本次再次确认模式不切换 | 不需要 Gateway fallback 字段、Route 前提或 fallback-only demand | Gateway Reference 仍独立走现有 Gateway 数据面 |
| `EVD-021` | User decision | 本次需求 11 | 重试不判断幂等，业务用唯一业务标识保证重复安全 | 移除框架幂等门控，但保留故障白名单/次数/Deadline | 业务是否真的幂等只能由调用方验收 |
| `EVD-022` | Static repository | `GUARD_JAVA/api/RateLimitGuard.java`；`GUARD_JAVA/adapter/aop/GuardBindingResolver.java` | `@RateLimitGuard` 已存在，METHOD target，只声明 rule `value` 和可选 `key`，并强制专用规则只启用 rate-limit policy | RPC 不复制限流参数；Provider 方法只引用 Guard rule | 注解存在不等于三算法均已实现 |
| `EVD-023` | Static repository | `GUARD_JAVA/core/plan/AdmissionConfig.java:RateLimitAlgorithm`；`GUARD_JAVA/store/{local/LocalRateLimitBackend,redisson/RedissonRateLimitBackend}.java` | 当前算法枚举只有 `TOKEN_BUCKET`，Local/Redisson 后端也只实现令牌桶 | 为满足原需求 6，漏桶/滑动窗口必须在 Guard 组件内扩展，并由算法 Strategy/Factory 分派 | 没有运行 Redis 集成测试或吞吐实测 |
| `EVD-024` | Static repository + User decision | `GUARD_JAVA/api/AccessGuardRejectedException.java`；`RPC_JAVA/provider/server/RpcProviderExceptionMapper.java`；2026-08-21 决定 3 | Guard 默认 THROW 时抛出包含 `GuardOutcome` 的稳定异常；RPC 已有可插拔 Provider exception mapper；用户规定限流触发返回 `UNAVAILABLE` | 增加 Guard→gRPC Adapter，无需侵入 Provider 业务或复制 Guard 引擎 | 需把 `RATE_LIMITED` 与其他 Guard rejection 区分，其他拒绝保持既有映射/业务决策 |
| `EVD-025` | Static repository + User decision | `RPC_JAVA/contract/validation/RpcContractValidator.java`；`RPC_JAVA/contract/descriptor/RpcMethodDescriptor.java`；Gateway `RuntimeRpcRoute.java`；2026-08-21 决定 5 | 当前 authority 是 `grpcMethod.getFullMethodName()`；Gateway 强制值包含 `/`，gRPC fixture 用 `generateFullMethodName` 生成 `fully.qualified.Service/Method` | Generic 唯一定位必须沿用斜杠格式；`fullservice.method` 仅表示“完整服务+方法”概念，不是可接受 wire key | 具体业务 service/method 仍来自各自 descriptor |

### 2.3 Problem statement and gap

当前实现存在六个结构性缺口：

1. 两种 Reference 的共同策略字段不对称，且已声明字段没有统一解析/运行语义；
2. DDC 发现缓存已存在，但负载均衡仍硬编码轮询，实例 weight 被解码后丢弃，Channel 又按 Query 重复创建；
3. Retry 只服务 Gateway 幂等故障，Direct 不重试；两种模式都缺少“同一逻辑调用按 LB 排除失败实例、改选同模式节点”的统一语义；
4. Java Contract、Provider 分发和 Consumer 执行均只支持阻塞 Message 返回，没有异步或无接口泛化入口；
5. Provider 生命周期有租约/排空骨架，但没有明确状态模型；Guard 已有 `@RateLimitGuard` 和令牌桶，但 RPC 未把 Guard 限流拒绝映射为 Provider-stage `UNAVAILABLE`，Guard 也尚缺漏桶/滑动窗口；
6. JDK Proxy 每次执行仍做反射 Method 查找，无法复用一份预编译调用计划。

### 2.4 Evidence and current-chain map

| Entry/trigger | Current call chain | Data read/written | External dependency | Consumers | Evidence |
| --- | --- | --- | --- | --- | --- |
| Gateway 字段注入与调用 | `BeanPostProcessor -> RpcConsumerProxyFactory -> JDK Proxy -> RpcConsumerInvocationHandler -> GatewayRpcInvocationChannelProvider -> RpcConsumerGatewayManager` | Gateway snapshot、ManagedChannel；无持久写 | DDC subscription、INTERNAL_GATEWAY、Gateway | Spring 业务 Bean | `EVD-002`,`EVD-004`–`EVD-006` |
| Direct 字段注入与调用 | `BeanPostProcessor -> RpcDirectReferenceProxyFactory -> RpcConsumerProviderManager.register -> ProviderRpcInvocationChannelProvider -> fixed RR` | `Registration.activeProviders/revision/sequence` | DDC RPC_PROVIDER、Redis Topic、Provider | Spring 业务 Bean | `EVD-003`,`EVD-007`–`EVD-009`,`EVD-013` |
| Provider 启动与健康 | `RpcProviderLifecycle.start -> Server.start -> leaseManager.prepare/registerAll -> heartbeatAndRecover` | DDC 临时租约、availability set | DDC Admin/Redis | Direct Consumer、Gateway Engine | `EVD-010`–`EVD-012` |
| Provider 请求分发/Guard | `gRPC interceptor chain -> RpcServerServiceDefinitionFactory.invoke -> Spring proxied bean -> Access Guard Advisor -> Provider method -> observer` | Guard rule/key 的 Local 或 Redisson 限流状态；无 RPC 持久写 | Provider 业务代码、可选 Guard Redis storage | 所有 RPC Caller | `EVD-016`,`EVD-022`–`EVD-024` |
| DDC 变更通知 | `register/deregister/expire -> Redis Topic -> DdcManagedRegistrySubscription -> RPC full snapshot pull -> Consumer reconcile` | Redis TTL、revision、进程内 current snapshot | Redis + DDC Registry RPC | Gateway/Provider Directory listener | `EVD-008`–`EVD-010` |

## 3. Goals and Non-goals

### 3.1 Goals

- 保持 `@EgonRpcReference` 和 `@EgonRpcDirectReference` 两个公开入口，同时把共同策略解析为单一内部模型并通过 Strategy 执行。
- Reference 模式在客户端配置并在代理生命周期内固定；可用性故障只在同模式候选集中按负载均衡改选实例，所有候选/预算耗尽后报错，绝不跨模式。
- 明确复用 DDC Redis 事件订阅、首次/定期全量拉取和 Consumer 本地不可变快照。
- 在 Consumer 侧实现随机、加权随机、轮询、平滑加权轮询、一致性 Hash；保留既有 `LEAST_IN_FLIGHT` 兼容值。
- 让 Provider 主动 heartbeat，DDC 只做租约接收、TTL/身份验证、过期与事件发布。
- 让 Provider/Consumer 启停具备 READY/DRAINING/STOPPED 边界，停止时拒绝新调用并等待已接受 unary 完成。
- 通过 Provider 实现方法上的 Guard `@RateLimitGuard` 配置限流；三算法归 Guard Strategy/Factory，RPC 将 `RATE_LIMITED` 适配为 Provider-stage `UNAVAILABLE`。
- 支持 typed blocking、typed async、generic blocking、generic async unary 调用，并共享同一执行/失败策略。
- 通过 CGLIB 接口代理、Method Plan 缓存和共享 ManagedChannel 池减少每次调用的反射与连接成本。
- 重试不读取 `idempotent` 作为门槛；框架只控制故障类型/次数/Deadline，业务负责重复安全。

### 3.2 Non-goals

- 不支持 client/server/bidirectional streaming；本规格仍只处理现有 unary Proto Method。
- 不把 DDC 改成主动探活系统，不让 DDC 反向调用 Provider Health RPC。
- 不新增 DDC Proto 方法/DDC Redis Key、数据库表、Flyway migration 或关系型 ER 模型；Guard 仅为新算法增加既有 rate-limit namespace 下的 transient suffix keys，详见 §11。
- 不在 RPC Starter 内实现任何限流算法/存储；Local/Redisson 作用域、rule key、配额状态都由 Access Guard 既有配置与后端负责。
- 不把 Provider Guard 限流复用成 Gateway 流量治理，也不修改 Gateway 现有规则/限流实现。
- 不允许 Direct 与 Gateway 在一次逻辑调用中互相切换，不新增 `fallbackToGateway`、Gateway fallback demand 或跨模式错误码。
- 不自动证明业务幂等，不生成业务去重键，不持久化重试结果，不替业务覆盖重复单据。
- 不增加 Map/Object[] 第二序列化协议，不绕开 Protobuf 作为唯一 wire IDL。
- 不开放 concrete class RPC Contract；CGLIB 只替换代理实现，Contract 仍必须是 `@EgonRpcService` 接口。
- 不引入 Resilience4j、Guava RateLimiter、第三方一致性 Hash/限流依赖或新 Maven 模块；复用现有 Access Guard Starter。
- 不改 Gateway BIZ/APP 授权、Provider 下游鉴权、凭证中继和接口定义上报职责。
- 不启动项目、DDC、Redis、Gateway 或业务进程；Spec 验证是文档/源码边界。

### 3.3 Change Surface and Design Depth

| Area/layer | Disposition | Exact repository evidence | Changed or preserved behavior/contract | Required Spec treatment | Chapter(s) |
| --- | --- | --- | --- | --- | --- |
| RPC annotations/public Java declaration | `Affected` | `annotation/{EgonRpcReference,EgonRpcDirectReference,LoadBalance,EgonRpcMethod}.java`；`EgonRpcProvider.java` 为边界证据 | Direct 补公共字段但不增跨模式字段；Provider marker 不变；LB 补算法；method return shape 扩展 | 完整 Java API、默认值、兼容/校验设计 | `§7, §8, §9, §10, §13, §14, §16, §17, §18` |
| Consumer reference/proxy/invocation | `Affected` | `consumer/proxy/*`、`RpcConsumerInvocationHandler` | 两 Factory/Handler 归一为 Reference Strategy + CGLIB + plan cache + blocking/async executor | 组件、调用、失败、文件和测试详细设计 | `§7, §8, §9, §10, §13, §14, §15, §16, §17, §18` |
| Consumer discovery/LB/channel | `Affected` | `RpcConsumerProviderManager`、`RpcConsumerGatewayManager`、`RpcConsumerChannelFactory` | 保留快照缓存，增加 weight、五算法、共享 multiplex Channel、graceful drain | 状态/并发/算法/资源生命周期详细设计 | `§7, §8, §9, §10, §13, §14, §15, §16, §17, §18` |
| Generic invocation | `Affected` | 当前无生产类型；`RpcContractValidator` 与 Gateway raw forwarding 提供 descriptor/bytes 证据 | 新增无接口 unary blocking/async API，复用同一执行链 | 公共方法、模型、错误/安全/测试完整设计 | `§7, §8, §9, §10, §13, §14, §15, §16, §17, §18` |
| Provider lifecycle/Guard adapter | `Affected` | `RpcProviderLifecycle`、`RpcServerServiceDefinitionFactory`、`RpcProviderExceptionMapper`、`@EgonRpcProvider` | 明确状态/callback stop；保持 Provider marker；让 Guard AOP 命中并把限流适配为 `UNAVAILABLE` | 接线、顺序、排空、错误和测试详细设计 | `§7, §8, §9, §10, §13, §14, §15, §16, §17, §18` |
| Access Guard rate-limit algorithms | `Affected` | `AdmissionConfig.RateLimitAlgorithm`、`RateLimitRequest`、Local/Redisson backend、Guard README/tests | 保留 `@RateLimitGuard` rule/key contract，在 Guard 内补 `LEAKY_BUCKET`、`SLIDING_WINDOW` 并通过 Strategy/Factory 选择；增加算法后缀 Redis transient state | 算法、配置、Local/Redis key/原子性/兼容和测试详细设计 | `§7, §8, §9, §10, §11, §13, §14, §15, §16, §17, §18` |
| RPC properties/errors/docs/tests | `Affected` | `EgonRpcProperties`、`EgonRpcErrorCode`、双语 README、现有 RPC tests | 新默认/上限、错误码、文档和回归矩阵 | 精确字段/兼容/验证设计 | `§8, §9, §10, §14, §15, §16, §17, §18` |
| DDC Registry subscription implementation | `Context-only` | `DdcRegistrySubscriptionCoordinator`、`DdcManagedRegistrySubscription`、`DdcInstanceSubscription` | 保留 Redis Topic + first pull + periodic reconcile；RPC 只消费 | 记录复用边界，不设计第二套实现；聚焦契约测试 | `§7, §14, §15` |
| DDC Admin/Redis registry storage | `Context-only` | `DdcServiceRegistryRedisRepository` | 保留租约/TTL/revision/topic；不主动探活、不改 key | 记录权威/一致性/运行验证边界 | `§7, §11, §15, §16` |
| Gateway RPC data plane | `Context-only` | GWS-04；`RpcGatewayForwarder` | 仅供 Gateway Reference 使用；Direct 失败不得进入 Gateway，既有 fullMethodName Route/治理/错误 Trailer 不变 | 只定义模式隔离和兼容回归，不改 Gateway | `§7, §9, §14, §16, §18` |
| Business Protobuf contracts/providers | `Unchanged` | `@EgonRpcService` + `.proto` + existing generated descriptors | wire service/method/request/response field不变；业务负责幂等 | 一项明确不变量与旧 fixture 回归 | `§9, §10, §14, §16` |
| Relational database/migration | `Not applicable` | RPC registry/runtime state均为 Redis 临时租约或 JVM 内存；本范围无 DAO/Flyway 路径 | N/A，无表、列、索引、迁移 | §11 证据化 N/A | `§11` |
| Frontend | `Not applicable` | RPC Component 无 frontend module/route/page | N/A | §12 证据化 N/A | `§12` |

## 4. Requirements and Acceptance Criteria

| ID | Atomic requirement | Priority | Observable acceptance criteria | Source |
| --- | --- | --- | --- | --- |
| `REQ-001` | 两种 Reference 的 timeout/retries/loadBalance/group/version/fallbackBean/failStrategy 必须解析为一个共同策略模型 | Must | 单元测试对两注解得到字段同义、同默认/覆盖规则的 `RpcReferencePolicy`；模式特有字段不污染共同模型 | 需求 1 |
| `REQ-002` | 既有 `@EgonRpcReference` 源码写法继续编译并默认以 Gateway 为主路径 | Must | 不改既有属性名/类型/默认；无 Direct 选择时仍只查询 INTERNAL_GATEWAY | 需求 1 + compatibility |
| `REQ-003` | Gateway/Direct 模式必须由客户端 Reference 配置并在代理/调用期间固定，失败不得切换另一模式 | Must | Gateway Reference 只查询/调用 Gateway；Direct Reference 只查询/调用 Provider；源码与进程测试均证明无跨模式 demand/call | 需求 1 + 2026-08-21 决定 1 |
| `REQ-004` | 可用性失败必须按有效负载均衡规则改选同模式、未尝试实例；候选或预算全部耗尽后报错 | Must | 首节点 acquisition/Provider-stage `UNAVAILABLE` 后选择另一实例；每逻辑调用同一实例最多尝试一次；全部失败返回模式对应稳定错误 | 需求 1 + 2026-08-21 决定 1/2 |
| `REQ-005` | Consumer 必须按精确服务键缓存一份不可变 Provider 列表，并由 Redis 事件和周期 DDC 拉取更新 | Must | 同 query 只订阅一次；首次快照、事件刷新、周期对账均替换同一缓存；调用不每次访问 DDC | 需求 2 |
| `REQ-006` | Consumer 缓存必须拒绝旧 revision、过滤过期租约并在 DDC/Redis 暂失时仅使用未过期本地实例 | Must | out-of-order snapshot 不回退；过期实例在本地时钟到期后不可选；恢复全量快照可收敛 | 需求 2/3 |
| `REQ-007` | Provider heartbeat 必须由 RPC 侧主动调度，间隔和 lease 在 RPC Provider 配置，DDC 不主动探测 | Must | `provider.heartbeat-interval-seconds < lease-seconds` 启动校验；每周期 Provider 调 heartbeat；DDC 无 Provider probe 调用 | 需求 3 |
| `REQ-008` | Consumer 负载均衡必须支持 RANDOM、WEIGHTED_RANDOM、ROUND_ROBIN、SMOOTH_WEIGHTED_ROUND_ROBIN、CONSISTENT_HASH | Must | 固定候选和种子/序列测试验证分布、权重、轮询顺序与成员变化；旧 LEAST_IN_FLIGHT 仍可用 | 需求 4 |
| `REQ-009` | Weighted 算法必须读取 1–10000 的实例 weight；一致性 Hash 必须有稳定、非敏感 key | Must | 缺 weight 默认 100，非法远程值容错为默认；无 key resolver 的一致性 Hash 在创建计划时失败 | 需求 4 |
| `REQ-010` | Provider 优雅启动必须先绑定不可用 Handler，完成必需租约后才 READY | Must | 注册前调用得到 provider-stage UNAVAILABLE；fail-fast 注册失败不留下 Server/heartbeat/lease；成功后才可调用 | 需求 5 |
| `REQ-011` | Provider 与 Consumer 优雅关闭必须停止新调用、关闭订阅/恢复、注销租约并在超时内排空已开始 unary | Must | Spring stop callback 在排空或强关后执行；无新 Channel 获取；超时后 force close；重复 stop 幂等 | 需求 5 |
| `REQ-012` | Provider 实现方法必须复用 Guard `@RateLimitGuard`；Guard rate-limit rule 必须支持 LEAKY_BUCKET、TOKEN_BUCKET、SLIDING_WINDOW | Must | `@EgonRpcProvider` 保持无字段；Provider method advice 命中；三算法在 Local/Redisson 后端的并发/边界合同通过；非法规则启动失败 | 需求 6 + 2026-08-21 决定 3 |
| `REQ-013` | Guard `RATE_LIMITED` 必须在业务方法前映射为 Provider-stage gRPC `UNAVAILABLE`，可按同模式规则换实例 | Must | 被拒请求不执行 bean；Provider 发出 status=`UNAVAILABLE`、stage=`PROVIDER`、error-type=`rate-limit`；有剩余候选/预算时重试另一同模式实例；耗尽传输 status仍为 UNAVAILABLE，仅当 Direct 所有已观察 failures 都是 rate-limit 时映射 `RPC_RATE_LIMITED`，混合原因或 Gateway 安全 trailer 重建映射 mode/provider unavailable；日志无 payload/密钥 | 需求 6 + 2026-08-21 决定 3 |
| `REQ-014` | typed Contract 必须继续支持阻塞 unary 调用 | Must | Message→Message 旧契约零源码修改，仍在 Deadline 内返回/抛稳定异常 | 需求 7 |
| `REQ-015` | typed Contract 必须支持非阻塞 unary 调用并传播取消/Deadline/重试结果 | Must | Message→CompletionStage<Message> 验证通过；调用线程不等待；future cancel 取消 gRPC call；只完成一次 | 需求 7 |
| `REQ-016` | RPC 必须提供不依赖业务 Java Interface 的泛化 unary blocking/async 调用，并以 canonical `fully.qualified.Service/Method` 唯一定位方法 | Must | 仅凭服务身份、斜杠格式 fullMethodName 和合法 Protobuf bytes/Descriptor 可调用；点号/缺服务段/不一致 identity 被拒；错误与 typed 路径一致 | 需求 8 + 2026-08-21 决定 5 |
| `REQ-017` | Consumer 必须复用 ManagedChannel 的 HTTP/2 多路复用，并在相同端点间共享连接 | Must | 同 endpoint 不因不同 Reference/query 重复创建 Channel；并发 calls 使用同 Channel；摘除按引用/排空关闭 | 需求 9 |
| `REQ-018` | typed 代理必须基于 Spring CGLIB，代理创建时预编译 Method→InvocationPlan | Must | 代理类是 CGLIB；contract 仍是 interface；调用不线性扫描 method list；Object 方法不发 RPC | 需求 10 |
| `REQ-019` | Framework retry 不得检查 `RpcMethodDescriptor.idempotent` | Must | `idempotent=false` 在配置 retries>0 且故障可重试时也重试；文档警告业务自保 | 需求 11 |
| `REQ-020` | Retry/换节点必须受总 Deadline、最大次数、同模式候选集合、故障白名单和取消控制；业务错误必须原样报错 | Must | 只有 acquisition failure 与 Provider/Gateway-stage `UNAVAILABLE` 可换节点；`UNAVAILABLE` 保留给 availability/Guard rate-limit，业务 exception mapper 不得用它表达业务条件；INVALID_ARGUMENT、PERMISSION_DENIED、NOT_FOUND、FAILED_PRECONDITION、ALREADY_EXISTS、ABORTED、DEADLINE_EXCEEDED 等业务/语义错误不重试、不 fail-open、不 fallback；取消立即终止；无无限循环 | 2026-08-21 决定 2 + 正确性必要条件 |
| `REQ-021` | 本改造不得改变业务 Protobuf wire、DDC Registry Proto、关系库或 Gateway 安全合同 | Must | Descriptor SHA/字段不因 runtime 改造变化；无 proto/db migration/Gateway production diff | 最小变更与前置 Spec |
| `REQ-022` | 双语 README、配置元数据、单元/组件/进程测试必须与新行为一致 | Must | 旧“JDK Proxy/Direct 不重试/限流不在模块”文案消失；测试矩阵和配置绑定通过 | 可交付性 |
| `REQ-023` | Consumer 优雅启动必须先安装全部已声明模式的 Directory subscriptions/共享 Channel runtime，再允许新 invocation；模式缓存为空不得伪装 READY | Must | STARTING 时执行器拒绝调用；Gateway primary 保持现有 fail-fast；Direct 空集合进入 DEGRADED 并在调用时返回 Provider unavailable；启动失败清理所有 subscription/pool entry | 需求 5 + 2026-08-21 决定 1 |

### 4.1 Scenario matrix

| Scenario | Actor/trigger | Preconditions | Main path | Alternative/failure path | Data/state change | Observable result | Requirements |
| --- | --- | --- | --- | --- | --- | --- | --- |
| Typed blocking Gateway | Caller 调接口 | Gateway snapshot READY | CGLIB plan→Gateway LB→blocking unary | 可重试 UNAVAILABLE 选未尝试 Gateway | in-flight 计数；无持久写 | Message 或稳定异常 | `REQ-001`,`REQ-002`,`REQ-014`,`REQ-018`–`REQ-020` |
| Direct 成功 | Direct Caller | Provider cache有有效租约 | 按 LB 选 Provider，共享 Channel 调用 | 首个 Provider UNAVAILABLE 可按 retries 选另一个 | cache只读；attempt 状态 | 不触发 Gateway | `REQ-003`,`REQ-005`–`REQ-009`,`REQ-017` |
| Direct 节点耗尽 | Direct Caller | Provider candidates 依次 acquisition/UNAVAILABLE | 每次按 LB 在未尝试 Provider 中选下一个 | 候选或 `1+retries` 预算耗尽；不查询 Gateway | 本地 attempted-instance 状态 | `RPC_PROVIDER_UNAVAILABLE` 或最后限流时 `RPC_RATE_LIMITED`；无 Gateway call | `REQ-003`,`REQ-004`,`REQ-019`,`REQ-020` |
| Gateway 节点耗尽 | Gateway Caller | Gateway candidates 依次 acquisition/UNAVAILABLE | 每次按 LB 在未尝试 Gateway 中选下一个 | 候选或预算耗尽；不查询 Provider Directory | 本地 attempted-instance 状态 | `RPC_GATEWAY_UNAVAILABLE`；无 Direct Provider call | `REQ-003`,`REQ-004`,`REQ-020` |
| Consumer graceful startup | Spring lifecycle | Reference demand 已在 BeanPostProcessor 阶段登记 | Coordinator 先启动各自模式 subscriptions/pool，再 READY/DEGRADED | primary Gateway fail-fast；Direct 空集合 DEGRADED；异常清理；无 fallback-only demand | Consumer runtime state | Application 不在未初始化 discovery 上接受调用 | `REQ-005`,`REQ-011`,`REQ-023` |
| Redis 事件丢失 | Scheduler | 旧缓存未过期，Redis event 未达 | 周期 RPC 全量拉取较新 revision | DDC 暂不可用→只本地过期，不延长旧 lease | immutable snapshot替换或过期收缩 | 最迟一个 reconcile interval 收敛 | `REQ-005`,`REQ-006` |
| Out-of-order refresh | Redis/周期并发触发 | 已接受 revision N | 收到 N-1 丢弃；N 相同且内容同不通知 | 同 revision 内容不同必须记录 revision anomaly 并触发一次权威全量 refresh；在新 revision 到达前保留 current snapshot | 不回退缓存 | 选择集合单调不回旧版本 | `REQ-005`,`REQ-006` |
| Provider 初始注册失败 | Provider startup | registration REQUIRED | Server bound but unavailable→register | failFast=true 清理并启动失败；false 后台 heartbeat/recover | 无有效 lease/availability | 未发布未 Ready 服务 | `REQ-007`,`REQ-010` |
| Provider 心跳失败/恢复 | Heartbeat scheduler | 曾有 lease | 先 unavailable、丢旧 lease、重新 register | DDC 仍不可用保持 unavailable | DDC TTL最终过期；新 lease替换 | Consumer/Gateway摘除后恢复 | `REQ-006`,`REQ-007`,`REQ-010` |
| Provider 优雅关闭 | Spring shutdown | READY 且有 in-flight | DRAINING→不可用→停恢复/heartbeat→deregister→Server.shutdown | 超时→shutdownNow | lease删除；Channel逐步排空 | 已接受调用完成，新调用失败并可重试 | `REQ-011` |
| Provider Guard 限流 | 并发 Caller | Provider 实现方法 `@RateLimitGuard` rule/key 达到配额 | Guard Strategy/Backend 原子 acquire | `AccessGuardRejectedException(RATE_LIMITED)` 经 Adapter→UNAVAILABLE；不执行业务；Consumer 可换同模式节点 | Guard Local/Redis 算法状态更新 | 有候选则继续；耗尽均为 UNAVAILABLE，Direct 应用码 rate-limited、Gateway 应用码 provider-unavailable | `REQ-012`,`REQ-013`,`REQ-020` |
| Provider 业务错误 | Caller | 业务条件不满足 | Provider 按现有 mapper 返回业务 status/code | Consumer 立即终止，不换节点、不执行 FAIL_OPEN/LOCAL_FALLBACK | 业务事务按 Provider 规则提交/回滚 | 原业务错误对 Caller 可见 | `REQ-020` |
| Typed async | Caller 调 CompletionStage 方法 | 合法 async contract | futureUnaryCall→CompletionStage | cancel/deadline/重试都异步串联 | in-flight future | 调用线程立即返回，最终只完成一次 | `REQ-015`,`REQ-020` |
| Generic raw call | Operator/framework | 已知服务/方法/Protobuf payload | generic plan→相同策略→raw unary | payload无法被 Provider 解析→INVALID_ARGUMENT | 无新协议状态 | bytes/async bytes 或稳定异常 | `REQ-016`,`REQ-021` |
| 非幂等写重试 | Business Caller | retries>0，首次 UNAVAILABLE 结果未知 | 框架不查 idempotent 再发 | 业务唯一单据号覆盖/去重；若未实现可能重复副作用 | 业务数据由下游决定 | 框架只暴露 attempts/result | `REQ-019`,`REQ-020` |

### 4.2 Use-case analysis

#### 4.2.1 Actor inventory

| Actor ID | Actor/role | Goal and responsibility | Entry/channel | Permission/tenant context | Evidence |
| --- | --- | --- | --- | --- | --- |
| `ACTOR-001` | RPC 业务 Consumer | 以 typed blocking/async 调用获得业务结果，并为重试保证业务幂等 | 两种 Reference 注入字段 | 身份/tenant 继续由现有 Metadata/Token relay 提供 | 用户需求 1/7/11；现有 Reference |
| `ACTOR-002` | RPC Provider 应用 | 注册、heartbeat、限流、处理调用并优雅下线 | `@EgonRpcProvider` + Spring lifecycle | Provider 本地安全拦截器和业务授权不变 | 用户需求 3/5/6；`RpcProviderLifecycle` |
| `ACTOR-003` | 基础设施/运维调用方 | 无业务接口代码时执行 generic 诊断或集成调用 | `RpcGenericInvoker` | 必须使用与 typed 调用相同的 transport/auth metadata；不绕权限 | 用户需求 8 |
| `ACTOR-004` | DDC Registry/Redis | 保存临时服务租约并通知/提供权威快照 | DDC RPC + Redis Topic | Registry HMAC/mTLS 现状不变 | `EVD-008`–`EVD-010` |
| `ACTOR-005` | Gateway RPC Runtime | 仅为 Gateway Reference 按已有 Route 转发；不参与 Direct Reference 失败处理 | INTERNAL_GATEWAY unary | Gateway BIZ/APP/Provider 安全沿用前置 Spec | 2026-08-21 决定 1；GWS-04 |
| `ACTOR-006` | 平台开发者/运维者 | 配置算法/心跳/限流，观察失败并安全发布/回退 | annotations、application.yml、logs/tests | 配置写权限由应用部署控制 | 用户“仔细审核”与现有 README |

#### 4.2.2 Use-case artifact

```mermaid
flowchart LR
    Caller["ACTOR-001 RPC business Consumer"]
    Provider["ACTOR-002 RPC Provider application"]
    Generic["ACTOR-003 infrastructure/operator caller"]
    DDC["ACTOR-004 DDC Registry and Redis"]
    Gateway["ACTOR-005 Gateway RPC Runtime"]
    Operator["ACTOR-006 platform developer/operator"]

    subgraph Scope["Egon RPC component evolution boundary"]
        UC001(["UC-001 Invoke typed RPC with same-mode node retry"])
        UC002(["UC-002 Maintain convergent Consumer provider cache"])
        UC003(["UC-003 Select endpoint with declared load balancer"])
        UC004(["UC-004 Invoke blocking/async generic unary"])
        UC005(["UC-005 Start, heartbeat, recover and drain Provider"])
        UC006(["UC-006 Enforce Provider Guard rate limit"])
    end

    Caller -->|"typed method"| UC001
    Generic -->|"fullMethodName plus Protobuf"| UC004
    UC001 -->|"Gateway Reference only"| Gateway
    UC002 -->|"pull and subscribe"| DDC
    UC001 --> UC003
    UC004 --> UC003
    Provider -->|"lifecycle"| UC005
    UC005 -->|"register and heartbeat"| DDC
    Provider -->|"admit before business"| UC006
    Operator -->|"configure and observe"| UC002
    Operator -->|"configure and observe"| UC005
    Operator -->|"declare policy"| UC006
```

#### 4.2.3 Use-case details

| ID | Goal/trigger | Preconditions | Main success outcome | Alternatives/failures | Success/failure postconditions | Requirements/contracts/tests |
| --- | --- | --- | --- | --- | --- | --- |
| `UC-001` | Caller 调 typed method | Contract/Reference 合法，所配置模式有候选实例 | 在总 Deadline 内从同模式实例返回一次业务结果 | 无实例、retryable UNAVAILABLE、取消、业务拒绝；从不跨模式 | 成功仅完成一次 future/返回；可用性失败可重复业务执行；业务错误原样终止 | `REQ-001`–`REQ-004`,`REQ-014`,`REQ-015`,`REQ-019`,`REQ-020`; `INTERNAL-001`–`INTERNAL-005` |
| `UC-002` | Reference 注册 exact query并启动 Consumer runtime | DDC Registry + Redis 配置存在或按 mode 允许 DEGRADED | subscriptions/pool 安装；本地不可变列表通过事件快速、周期兜底收敛 | event 丢失、DDC timeout、旧 revision、lease expiry、startup cleanup | STARTING 不接受调用；不使用过期实例；恢复后替换 | `REQ-005`–`REQ-007`,`REQ-011`,`REQ-023`; `TEST-010`–`TEST-015`,`070`–`072` |
| `UC-003` | 每次 invocation 选 endpoint | 有未尝试候选，算法/weight/key 合法 | 算法返回一个 candidate/selection handle | 空候选、非法权重、缺 hash key | 算法状态有界；失败不创建 Channel | `REQ-008`,`REQ-009`,`REQ-017`; `INTERNAL-008`; `TEST-016`–`TEST-025` |
| `UC-004` | Generic caller 指定 method/payload | exact service identity、合法 fullMethodName、bytes/descriptor | 通过同一执行器返回 bytes/CompletionStage | method 不存在、payload 无法解析、权限/限流/timeout | 不新增 Provider generic service；错误与 typed 一致 | `REQ-016`,`REQ-020`,`REQ-021`; `INTERNAL-006`,`INTERNAL-007` |
| `UC-005` | Spring start/stop 或 heartbeat tick | Provider 配置/Contract/Registry 合法 | READY 只在 lease 后；停止排空并注销 | 初始注册失败、丢 lease、DDC outage、排空超时 | 不发布假 READY；失败/停止无恢复线程或残留 lease（TTL 兜底） | `REQ-007`,`REQ-010`,`REQ-011`; `TEST-031`–`TEST-038` |
| `UC-006` | Provider 收到已通过安全拦截的 unary | 实现方法 `@RateLimitGuard` rule/key 与 Guard 配置合法 | Guard 获取 permit 后执行业务 | 达限映射 UNAVAILABLE；算法/存储异常遵循 Guard failure policy | 限流拒绝不执行业务；状态作用域由 Guard Local/Redisson 后端定义 | `REQ-012`,`REQ-013`; `INTERNAL-003`; `TEST-039`–`TEST-048` |

## 5. Constraints, Assumptions, and Decisions

### 5.1 Confirmed constraints

- Java 21、Spring Boot 3.5.16、gRPC Java 1.75.0、Protobuf 4.32.0 是当前构建基线。
- 两种 Reference 是已有公开源码合同；本规格默认保留，不用单一新注解强制迁移全部调用点。
- 业务 Proto descriptor 是唯一 wire authority；typed/generic 只改变 Java 调用形态，不改变 service/method/field。
- Provider heartbeat/lease interval 归 RPC Provider 配置，DDC 只被动接收和过期。
- 用户明确决定重试不检查框架幂等标记；业务代码保证重复安全。
- 用户明确决定 Reference mode 由客户端配置且调用期间不可切换；Direct 不依赖 Gateway Route，Gateway 也不得退化为 Direct。
- 只有连接/Channel 获取失败和 Provider/Gateway-stage `UNAVAILABLE` 属于自动换节点白名单；业务错误是正常可见结果，必须报错且不进入 fail strategy。
- Provider/Gateway `UNAVAILABLE` 是 availability protocol contract（包含 Guard rate-limit）；业务 `RpcProviderExceptionMapper` 必须使用非-UNAVAILABLE status 表达业务校验、前置条件、冲突、拒绝或未找到。
- Guard `@RateLimitGuard` 是 Provider 限流声明 authority；`@EgonRpcProvider` 继续只负责 bean discovery。
- Generic 方法 identity 以 gRPC `MethodDescriptor#getFullMethodName()` 为 authority，格式固定为 `fully.qualified.Service/Method`。
- Registry、Gateway、Provider、业务授权是独立边界；本 Spec 不把服务发现成功当成业务权限允许。
- 当前模块采用 Core SPI + Adapter + Spring Boot AutoConfiguration 的功能包结构，不迁移为传统三层、DDD 或 COLA 包。

### 5.2 Small-gap assumptions

| ID | Inference | Repository evidence | Why locally reversible | Impact if wrong |
| --- | --- | --- | --- | --- |
| `ASM-001` | `SMOOTH_WEIGHTED_ROUND_ROBIN` 即用户所说“加权轮询”，保留现有名称避免枚举破坏 | `LoadBalance` 与 Gateway 已有相同常量 | 算法实现/文档可局部调整 | 若必须使用 `WEIGHTED_ROUND_ROBIN` 名称，需别名/弃用周期 |
| `ASM-002` | 旧 `LEAST_IN_FLIGHT` 继续保留，但不计入本次必须新增的五种算法 | 已发布 enum 与 Gateway 使用 | 保留值无新默认行为 | 若要求严格只保留五种，删除会破坏源码/配置 |
| `ASM-003` | DDC reconcile interval 继续使用 `egon.cola.component.ddc.registry.reconcile-interval-seconds`，不复制 RPC 属性 | Adapter 当前直接复用 coordinator | 配置归属可在后续调整 | 若必须全部置于 rpc namespace，需要 DDC Adapter 属性优先级迁移 |
| `ASM-004` | CGLIB 通过 `Enhancer.setInterfaces` 代理现有接口，不开放 class Contract | Validator/BeanPostProcessor 都强制 interface | 代理实现可回退而不改业务接口 | 若用户要代理 concrete class，Contract/Provider/测试面显著扩大 |
| `ASM-005` | Programmatic `RpcDirectClientFactory` 的显式基础设施 target 继续独占 handle，不加入业务发现共享池 | README 区分该能力与 DDC Direct Reference | 独立生命周期可局部保留 | 若也要池化，需定义不同 TLS/HMAC profile 的 pool key/ownership |

### 5.3 Resolved decisions

| ID | Decision | Decision owner | Evidence and rationale | Requirements |
| --- | --- | --- | --- | --- |
| `DEC-001` | 保留两个注解，抽取内部 `RpcReferencePolicy`，不新增第三个统一公开注解 | Spec recommendation from compatibility evidence | Java annotation 不能继承属性；公开嵌套 options 会制造双写冲突，内部归一最小 | `REQ-001`,`REQ-002` |
| `DEC-002` | 复用现有 DDC subscription 两层本地状态，不新建远程 cache/job/topic | Repository fact | 已有首次 pull、Redis event、periodic reconcile、expireLocal，新增一套只会增加一致性状态 | `REQ-005`,`REQ-006` |
| `DEC-003` | gRPC Client transport retry 继续 `disableRetry()`；RPC Consumer 到其所配 endpoint 的重试/同模式换节点由一个 `RpcInvocationExecutor` 控制 | Existing factory + observability need | 防止 Consumer 层与 gRPC service config 双重重试，确保 Consumer transmissions/Deadline 可计算；Gateway 数据面内部 Provider retry保持 Context-only 现状 | `REQ-019`,`REQ-020` |
| `DEC-004` | 实例 weight 沿用现有 `gateway.weight` metadata wire，RPC model 增 typed weight，不改 DDC Proto | `EVD-014` | 既有单一 metadata authority 已存在，新增字段会造成双事实源 | `REQ-008`,`REQ-009`,`REQ-021` |
| `DEC-005` | 多路复用通过共享 `ManagedChannel` 池兑现，不新增自定义帧/连接协议 | gRPC HTTP/2 transport | 线协议已原生 multiplex；真正缺口是 Channel 所有权和跨 Query 复用 | `REQ-017`,`REQ-021` |
| `DEC-006` | Provider 实现方法复用 `@RateLimitGuard`；Guard advice 在 business method 前执行，RPC exception adapter 将 `RATE_LIMITED` 映射为 Provider-stage `UNAVAILABLE` | User + `EVD-016`,`EVD-022`–`EVD-024` | 保持 Provider marker 单一职责，不复制 Guard rule/key/storage；限流可在同模式其他实例继续尝试；Gateway 保持现有安全 trailer 重建 | `REQ-012`,`REQ-013` |
| `DEC-007` | `idempotent` 字段保留作文档/Gateway/业务提示，但从 Consumer retry gate 移除 | User | 保持 contract compatibility，同时落实业务自保责任 | `REQ-019` |
| `DEC-008` | Gateway/Direct 模式在客户端 Reference 上固定；失败不跨模式，只在同模式候选中改选节点，全部失败报错 | User（2026-08-21 决定 1） | 避免隐藏拓扑切换和 Gateway Route 前提；与前置 Direct Spec 的禁止 fallback 规则一致 | `REQ-003`,`REQ-004`,`REQ-023` |
| `DEC-009` | 只有 channel acquisition failure 与 Provider/Gateway-stage `UNAVAILABLE` 可自动重试/换节点；业务错误和其他语义 status 立即报错 | User（2026-08-21 决定 2） | “不满足业务条件需要报错”；避免业务拒绝被另一实例或 FAIL_OPEN 隐藏 | `REQ-004`,`REQ-020` |
| `DEC-010` | 限流使用 Guard `@RateLimitGuard` 的 rule/key 和 Local/Redisson scope；RPC 不再定义 Provider 注解限流作用域 | User（2026-08-21 决定 3） | 当前 Guard 已拥有注解、rule plan、key、failure policy、storage；三算法缺口在 Guard 内补齐 | `REQ-012`,`REQ-013` |
| `DEC-011` | Typed async Java Contract 返回 `CompletionStage<Response>`，Provider/Consumer 使用同一方法形态 | User（2026-08-21 决定 4） | 类型安全、可组合/取消，无额外 callback/facade；wire 仍 unary | `REQ-015` |
| `DEC-012` | Generic 使用 canonical `fullMethodName + Protobuf bytes`，可选 Descriptor/DynamicMessage convenience；唯一格式为 `fully.qualified.Service/Method` | User（2026-08-21 决定 5）+ `EVD-025` | 复用 gRPC/Gateway 既有 identity，不增加 Map/Object[] 或 Generic Proto 服务；点号格式不是 alias | `REQ-016`,`REQ-021` |
| `DEC-013` | `CONSISTENT_HASH` typed 调用必须提供业务 `RpcLoadBalanceKeyResolver`；generic 调用显式提供 affinity key | User（2026-08-21 决定 6） | 业务拥有稳定 affinity 语义；框架不 hash 整包或 invocationId | `REQ-009` |
| `DEC-014` | `FAIL_OPEN` 只处理可用性路径耗尽；blocking typed、async typed、generic blocking/async 均正常返回/完成 `null` | User（2026-08-21 决定 7） | 按用户明确选择保留“结果缺失”语义；业务错误不允许 fail-open；调用方必须显式处理 nullable | `REQ-001`,`REQ-020` |

### 5.4 Open major decisions

`N/A` — 原 `DEC-008`–`DEC-014` 已由用户在 2026-08-21 全部关闭并移入 §5.3；本规格没有剩余阻塞决策。`Status=Review` 仅表示等待整份 Spec 审核，不等同于用户已接受。

## 6. Project Technology Context

| Concern | Current choice | Repository evidence | Constraint on design |
| --- | --- | --- | --- |
| Language/runtime | Java 21 | `egon-cola-components/pom.xml:68` | 使用 record、sealed-free 简单接口、CompletionStage；不引入新语言/runtime |
| Framework | Spring Boot 3.5.16 / Spring Context | parent BOM；`EgonRpcAutoConfig` | CGLIB 使用 Spring 已携带的 `org.springframework.cglib`，不加依赖 |
| RPC/wire | gRPC Java 1.75.0 + Protobuf 4.32.0，unary | parent POM、`RpcContractValidator` | blocking/async/generic 共享 unary MethodDescriptor，不改 Proto |
| Registry/event | DDC Registry RPC + Redisson Topic | `DdcRpcAutoConfiguration`、DDC subscription classes | Redis event 是提示，full snapshot/revision/lease 是权威 |
| Consumer architecture | neutral Directory SPI + DDC adapter + SmartLifecycle managers | `RpcProviderDirectory`、`RpcGatewayDirectory`、两个 Manager | RPC Starter 不依赖 DDC 实现；Adapter 转换 metadata/快照 |
| Provider architecture | scanner/binding/server/availability/lease lifecycle + optional Guard AOP integration | RPC `provider/*`；Guard advisor/binding resolver | RPC 保留 Spring proxied bean；rate policy不进入 Provider marker/Contract wire |
| Rate limiting | Access Guard rule plan + Local/Redisson backend + optional Micrometer | Guard `api/core/policy/store/autoconfigure` | `@RateLimitGuard`/rule/key/storage是 authority；RPC只依赖 Guard API并适配 status |
| Test stack | JUnit Jupiter 5.12.2、Spring Boot Test、真实 Netty gRPC fixtures | parent POM、RPC test modules | 单元 + component + process test 分层；无浏览器 |
| Persistence | DDC Redis transient registry + optional Guard transient rate keys；无 relation table | DDC repository、Guard Redisson backend/key factory | §11 设计新算法 suffix keys；无 Flyway/ER；不改 DDC keys/旧 migration |
| Repository state | Dirty worktree with unrelated Gateway/spec/plan work | 2026-08-21 `git status` | 后续实施需刷新基线、逐文件避让并 path-limited commit；本 Spec 只改本文档 |

### 6.1 Java three-layer applicability

| Architecture profile | Base package | Evidence or explicit decision | Existing deviations | Design action |
| --- | --- | --- | --- | --- |
| Other — feature-oriented Core SPI + Adapter + AutoConfiguration | `top.egon.cola.component.rpc` | `annotation/config/contract/consumer/provider/context/exception` 当前树；DDC adapter 独立模块 | 不使用 `biz.controller/service/dao`，这是基础设施组件而非三层业务模块 | Preserve current structure；不迁移到传统三层、DDD/COLA |

## 7. Architecture Design

### 7.0 Minimum-design baseline and element-necessity audit

直接基线是继续保留两个 Factory、固定 RR、JDK Proxy、blocking unary 和无 Provider 限流。它不能满足用户明确要求。选中方案仍复用现有 Directory、subscription、lease、Proto、gRPC Channel 和 lifecycle，不增加注册中心、消息协议、数据库或第三方库。

| Proposed element | Change | Requirements | Existing/direct alternative | Concrete inadequacy of alternative | Added calls/state/coupling/failures/migration/operations | Verdict |
| --- | --- | --- | --- | --- | --- | --- |
| `RpcReferencePolicy` | New | `REQ-001` | 两注解分别传参数 | 字段已漂移且 Direct 缺策略 | 一份 immutable object；无网络/迁移 | Add |
| `RpcReferenceStrategyFactory` + Gateway/Direct strategies | New/Merge | `REQ-001`–`REQ-004` | BeanPostProcessor if/else + 两 Proxy Factory | 两模式缺共同创建合同，且调用循环会把模式分支散落在 executor | 一个按 mode 枚举建策略的 Factory；两个策略各只注册/读取自身 Directory | Add；Factory 只在代理创建时选择一次，禁止跨模式 |
| `RpcInvocationExecutor` | New/merge | `REQ-014`–`REQ-020` | 扩写 InvocationHandler | blocking/async/generic 会复制 retry/deadline | 一处状态机、future/cancel 复杂度 | Add；merge existing handler loop |
| `RpcConsumerMethodInterceptor` | Replace | `REQ-018` | JDK InvocationHandler | 用户要求 CGLIB，且当前每次线性 method lookup | CGLIB runtime class；AOT 风险 | Add/Remove old handler |
| `RpcInvocationPlan` cache | New | `REQ-018` | 每次 `contract.method(method)` | O(method count) 查找且重复解析 policy | 每 Proxy 一张小 immutable map | Add |
| `RpcLoadBalancers` + SPI | New | `REQ-008`,`REQ-009` | Manager 内固定 RR | 缺四种必需运行算法/selection handle | 有界 per-service 算法状态 | Add |
| `RpcLoadBalanceKeyResolver` | New SPI | `REQ-009` | hash 全 payload | 业务 affinity 语义不可由框架可靠猜测 | 一个 Bean 解析调用；缺失 fail-fast | Add（`DEC-013`） |
| Existing DDC subscription/cache | Keep | `REQ-005`,`REQ-006` | 新建 RPC polling cache | 当前机制已完整，复制会双写/双线程 | 无新增调用；继续每 interval 拉取 | Keep |
| `weight` on neutral/Gateway/Provider endpoint models | Expand | `REQ-008`,`REQ-009` | 所有 weight=100 | 两种 Reference 均无法执行加权算法 | metadata decode 字段；wire不变 | Add default/fields |
| `RpcConsumerChannelPool` | New/merge ownership | `REQ-017` | 每 Query/Manager 独占 Channel | 相同 endpoint 重复连接，复用不足 | ref count/drain/in-flight state | Add |
| Typed async method shape | Expand | `REQ-015` | 业务自行线程池包 blocking call | 占线程、取消/Deadline 不原生 | future bridge + Provider async completion | Add（`DEC-011`） |
| `RpcGenericInvoker` | New API | `REQ-016` | 为每个调用建接口 | 运维/动态集成无法提前编译接口 | raw payload responsibility；无新 wire | Add（`DEC-012`） |
| Existing Guard `@RateLimitGuard` + RPC exception adapter | Keep/Add | `REQ-012`,`REQ-013` | RPC-local Provider 注解字段/算法 | 会复制 rule/key/storage/failure policy，违反 Provider marker 单一职责 | RPC 对 Guard optional compile coupling；业务应用显式引入 Guard；每拒绝一次 mapper 调用 | Keep annotation；Add adapter |
| Guard `RateLimitAlgorithmStrategyFactory` | New/Expand | `REQ-012` | 在 Local/Redisson backend 中继续硬编码 token bucket | 三种算法的状态/原子计算不同，单个 switch/脚本会继续膨胀 | 两个新算法状态/Redis script、配置兼容和测试成本 | Add in Guard；Factory 按枚举返回 Strategy |
| `RpcProviderRuntimeState` | New enum | `REQ-010`,`REQ-011` | boolean running + availability set | 无法表达 STARTING/DRAINING/FAILED | 一个原子状态；无外部存储 | Add |
| `RpcConsumerLifecycleCoordinator/RuntimeState` | New/merge lifecycle ownership | `REQ-011`,`REQ-023` | two Managers independently start/stop | no single gate proves subscriptions/pool ready or blocks new calls during drain | one coordinator/state；reuses managers/pool | Add |
| New DDC Proto/Redis key | Candidate | none | 复用 metadata/snapshot/topic | 无需求证明 | wire/部署/兼容成本高 | Remove |
| New generic envelope RPC service | Candidate | `REQ-016` | raw existing canonical fullMethodName | 增第二 endpoint/权限/路由且非必要 | Proto/Provider/Gateway 全链变更 | Remove（`DEC-012`） |
| RPC-local rate-limit annotation/algorithms | Candidate | `REQ-012` | Guard `@RateLimitGuard` + algorithms | 重复配置、key、存储和 failure policy，且违背用户决定 | 双事实源/双状态/额外测试 | Remove |

| Path | Network calls | Client states | Server contracts/state | Failure and TOCTOU points | Additional user/business value |
| --- | --- | --- | --- | --- | --- |
| Current Direct baseline | 每 invocation 1 次 Provider；发现由事件/周期刷新 | one blocking result | fixed RR、per-query Channel | Provider unavailable 直接失败 | 已有直连 |
| Selected Direct success | 同样 1 次 Provider | blocking 或 async | LB state + shared Channel | snapshot may change after selection；lease/Channel二次校验 | 五算法、异步、多路复用 |
| Selected same-mode failure | 最多 `min(1 + retries, distinct same-mode candidates)` 次传输 | attempted identities/terminal | one invocation plan；mode immutable | 结果未知重放、候选耗尽、deadline耗尽；无跨模式 TOCTOU | 同模式实例容灾，业务承担幂等 |
| Generic selected | 1 次或同一 retry/same-mode reselection policy | bytes/future bytes | 无新 Provider service | payload/schema mismatch | 无编译期接口调用 |

### 7.1 System Architecture Design

#### 7.1.1 Architecture Mermaid view

```mermaid
flowchart LR
    Business["ACTOR-001 Business Consumer"]
    Generic["ACTOR-003 Generic caller"]
    Operator["ACTOR-006 Operator"]

    subgraph ConsumerJVM["RPC Consumer JVM"]
        Annotation["Two Reference annotations"]
        Resolver["RpcReferenceDefinitionResolver"]
        Proxy["CGLIB proxy and Method Plan cache"]
        Executor["RpcInvocationExecutor"]
        RefFactory["RpcReferenceStrategyFactory"]
        GatewayStrategy["Gateway Reference Strategy"]
        DirectStrategy["Direct Reference Strategy"]
        GatewayCache["Gateway exact-key snapshots"]
        ProviderCache["Provider exact-key snapshots"]
        LB["RpcLoadBalancers"]
        Pool["Shared multiplexed ManagedChannel pool"]
        GenericInvoker["RpcGenericInvoker"]
    end

    subgraph ControlPlane["Existing control plane boundaries"]
        DDC["DDC Registry RPC"]
        Redis[("Redis leases and registry topics")]
    end

    Gateway["Existing Gateway RPC data plane"]

    subgraph ProviderJVM["RPC Provider JVM"]
        Lifecycle["RpcProviderLifecycle and active lease"]
        Server["gRPC unary server"]
        Rate["Access Guard @RateLimitGuard Strategy/Backend"]
        Bean["Business Provider bean"]
    end

    Business --> Annotation --> Resolver --> RefFactory --> Proxy --> Executor
    Generic --> GenericInvoker --> Executor
    RefFactory -->|"mode=GATEWAY, fixed"| GatewayStrategy
    RefFactory -->|"mode=DIRECT, fixed"| DirectStrategy
    Executor --> GatewayStrategy --> GatewayCache --> LB --> Pool
    Executor --> DirectStrategy --> ProviderCache --> LB
    Pool -->|"Gateway-mode HTTP/2 unary"| Gateway
    Pool -->|"Direct-mode HTTP/2 unary"| Server
    Gateway -->|"existing routed unary"| Server
    Lifecycle -->|"register and heartbeat"| DDC
    DDC --> Redis
    Redis -->|"topic event"| GatewayCache
    Redis -->|"topic event"| ProviderCache
    GatewayCache -->|"periodic full pull"| DDC
    ProviderCache -->|"periodic full pull"| DDC
    Server --> Rate --> Bean
    Operator -->|"annotations and RPC properties"| Resolver
    Operator -->|"provider properties"| Lifecycle
```

#### 7.1.2 Boundary and responsibility table

| Module/component | Capability and data owned | Inputs/outputs | Allowed dependencies | Forbidden responsibility | Requirements |
| --- | --- | --- | --- | --- | --- |
| Annotation resolver | 公共/模式特有声明归一、覆盖校验 | Field+Contract→Definition/Policy | annotation、contract descriptor、properties | 网络、发现、调用 | `REQ-001`,`REQ-002` |
| CGLIB Proxy/plan cache | Java method到预编译执行计划 | Method+request→blocking value/future | validator、executor | DDC/Channel ownership | `REQ-014`,`REQ-015`,`REQ-018` |
| Reference Strategy Factory | 依据 Reference mode 一次性创建 Gateway 或 Direct strategy | normalized definition→one strategy | two Directory managers | 运行时切换 mode、跨模式注册 demand | `REQ-001`–`REQ-004` |
| Invocation executor | Deadline、attempt、同模式候选排除、availability retry、terminal availability fallback | immutable plan/request→outcome | fixed strategy、LB、interceptors、status mapper | 服务目录写入、业务幂等、跨模式切换、吞掉业务错误 | `REQ-003`,`REQ-014`–`REQ-020` |
| Provider/Gateway managers | exact query快照、候选过滤、subscription refs | Directory snapshots→candidates | neutral Directory、LB、Channel pool | DDC 实现类型、业务 Route 发布 | `REQ-005`–`REQ-009`,`REQ-017` |
| DDC adapter | DDC key/snapshot/metadata 与 neutral RPC model转换 | DDC models↔RPC endpoints | DDC SDK、RPC SPI | 调用策略/限流/业务授权 | `REQ-005`–`REQ-009`,`REQ-021` |
| DDC Registry | 临时租约、revision、catalog、topic | register/heartbeat/query/event | Redis | 主动调用 Provider 健康检查 | `REQ-005`–`REQ-007` |
| Channel pool | endpoint→shared ManagedChannel 与 drain ownership | acquire/release/in-flight | channel factory | LB、retry、Route 决策 | `REQ-011`,`REQ-017` |
| Consumer lifecycle coordinator | subscriptions/managers/pool/invocation gate state | Spring start/stop + registered demands | managers、generic cache、pool、executor gate | DDC implementation、LB algorithm、business retry | `REQ-005`,`REQ-011`,`REQ-023` |
| Provider lifecycle | Server/lease/heartbeat/runtime state | Spring start/stop | Registry SPI、Server、availability | DDC 主动探活、业务授权 | `REQ-007`,`REQ-010`,`REQ-011` |
| Access Guard | rule/key、三算法、Local/Redisson 限流状态与 failure policy | `@RateLimitGuard` invocation→allow/`AccessGuardRejectedException` | Guard plan sources/backends | RPC mode/LB/retry、Provider discovery | `REQ-012`,`REQ-013` |
| RPC Guard exception adapter | 将 `GuardDecision.RATE_LIMITED` 映射到 gRPC | Guard exception→UNAVAILABLE + Provider stage + rate-limit type | Guard API、`RpcProviderExceptionMapper` | 实现限流算法、映射其他业务异常 | `REQ-013`,`REQ-020` |
| Gateway | existing Route/security/governance/forwarding | canonical fullMethodName unary | existing Gateway subsystems | Direct Reference fallback、Consumer 注解解析、Provider heartbeat | `REQ-003`,`REQ-021` |

### 7.2 High-Level Design

#### 7.2.1 Critical business/control flowchart

```mermaid
flowchart TD
    Start(["Typed or generic invocation starts"])
    Plan{"Reference/Method plan valid?"}
    Deadline{"Deadline remaining?"}
    Mode["Use immutable client-configured mode"]
    Select{"Untried same-mode candidate and retry budget?"}
    Call["Start unary attempt on shared Channel"]
    Result{"Attempt outcome"}
    Retryable{"Acquisition failure or UNAVAILABLE?"}
    Business["Propagate business/semantic error unchanged"]
    Terminal["Map availability exhaustion / apply availability fail strategy"]
    Success(["Return value or complete future; FAIL_OPEN may be null"])
    Cancel(["Cancel call and complete cancelled"])

    Start --> Plan
    Plan -->|"No"| Terminal
    Plan -->|"Yes"| Deadline
    Deadline -->|"No"| Terminal
    Deadline -->|"Cancelled"| Cancel
    Deadline -->|"Yes"| Mode
    Mode --> Select
    Select -->|"No"| Terminal
    Select -->|"Yes, LB selects"| Call
    Call --> Result
    Result -->|"Success"| Success
    Result -->|"Cancelled"| Cancel
    Result -->|"Failure"| Retryable
    Retryable -->|"No"| Business
    Retryable -->|"Yes"| Deadline
```

#### 7.2.2 High-level decision and quality matrix

| Concern/use case | Required behavior | Selected mechanism | Failure/degradation behavior | Trade-off | Verification | Requirements |
| --- | --- | --- | --- | --- | --- | --- |
| Discovery convergence | 事件快速、轮询兜底、无每调用 RPC | keep DDC coordinator + immutable Manager cache | DDC outage只本地过期，不延长 | 最多 reconcile interval 陈旧 | fake clock/event/revision tests + live gap | `REQ-005`,`REQ-006` |
| Mode isolation/availability | 多实例失败只在所配模式内换节点 | Factory 固定 Strategy；executor 维护 attempted identities | 同模式候选/预算耗尽即报错；无跨模式 demand/call | 可能重复业务执行，但拓扑行为可预测 | Direct/Gateway no-cross-mode + failure-stage matrix | `REQ-003`,`REQ-004`,`REQ-019`,`REQ-020` |
| LB correctness | 五算法、weight/hash有定义 | `RpcLoadBalancers` Strategy | 缺 key fail-fast；非法远程 weight default | per-service small mutable state | deterministic/property distribution tests | `REQ-008`,`REQ-009` |
| Async | 无调用线程阻塞且可取消 | `ClientCalls.futureUnaryCall` bridge | cancellation cancels active call；terminal once | Provider async failure callback complexity | virtual executor/latch tests | `REQ-015`,`REQ-020` |
| Generic | 无接口仍用同一安全/路由 | raw MethodDescriptor bytes | invalid payload由 Provider返回 INVALID_ARGUMENT | 调用方失去 compile-time payload检查 | raw/typed parity tests | `REQ-016`,`REQ-021` |
| Multiplexing | 同 endpoint少连接、多并发 streams | ref-counted shared ManagedChannel | endpoint drain后不发新 call；超时强关 | remote stream limit由HTTP/2协商 | channel factory count + concurrent calls | `REQ-011`,`REQ-017` |
| Provider admission | Guard 三算法、业务前拒绝、RPC status 统一 | Guard `@RateLimitGuard` + Algorithm Strategy Factory + RPC exception Adapter | limit→UNAVAILABLE并可换同模式节点；Guard backend failure 按 Guard policy | Guard 与 RPC 有可选集成依赖；Redisson 各算法需原子脚本 | deterministic clock/concurrency + RPC AOP component test | `REQ-012`,`REQ-013` |
| Business failure | 不满足业务条件必须报错 | 非 UNAVAILABLE status 直接 terminal，跳过 fail strategy | 不换节点、不返回 null、不执行 local fallback | 降级能力仅覆盖 availability exhaustion | status matrix and fallback non-invocation | `REQ-020` |
| Compatibility | wire/old typed API不破坏 | additive fields/enum/API；interface-only；Provider marker field-free | previously unwired retries/LB become active | 需要发布说明 | old fixture compile and calls | `REQ-002`,`REQ-014`,`REQ-021`,`REQ-022` |

### 7.3 Detailed Design

#### 7.3.1 Detailed component collaboration

| Step | Caller -> callee | Contract/symbol | Input/output mapping | State/data effect | Failure behavior | Requirements |
| --- | --- | --- | --- | --- | --- | --- |
| 1 | BeanPostProcessor→DefinitionResolver | `resolve(Field, ContractDescriptor)` | annotation+properties→ReferenceDefinition | none | conflicting/invalid declaration startup fails | `REQ-001`,`REQ-002` |
| 2 | DefinitionResolver→ReferenceStrategyFactory | `create(mode, definition)` | GATEWAY/DIRECT→one immutable-mode strategy | 只注册该模式 primary demand | 所需 Directory 缺失 startup/runtime 按模式报错；Factory 不提供 switch API | `REQ-003`,`REQ-004` |
| 3 | ProxyFactory→CGLIB | `create(contract, definition)` | each Java Method→InvocationPlan | immutable method map | unsupported return/overload invalid contract | `REQ-014`,`REQ-015`,`REQ-018` |
| 4 | CGLIB interceptor→Executor | `executeBlocking/executeAsync(plan, request)` | typed Message or raw bytes→logical invocation | invocation ID、deadline、attempt state | bad request rejected before Channel | `REQ-014`–`REQ-016`,`REQ-020` |
| 5 | Executor→fixed ReferenceStrategy | `candidates(context)` | immutable mode→same-mode candidate view | attempted identities只存在于 logical invocation | 空集合或预算耗尽直接 terminal，不查询另一模式 | `REQ-003`,`REQ-004`,`REQ-020` |
| 6 | Strategy→Manager cache/LB Factory | `create(effectiveAlgorithm)` / `select(query, context, excluded)` | immutable endpoints + weight/key→selection | LB sequence/current weights | no valid candidate→mode-specific unavailable | `REQ-005`–`REQ-009` |
| 7 | Manager→ChannelPool | `acquire(endpoint)` | transport key→Channel lease | ref/in-flight increments | pool draining rejects acquisition | `REQ-011`,`REQ-017` |
| 8 | Executor→gRPC ClientCalls | blocking/future unary | request/bytes→response/bytes | one HTTP/2 stream | status classified; lease released exactly once | `REQ-014`–`REQ-017`,`REQ-020` |
| 9 | Provider Lifecycle→DDC | register/heartbeat/deregister | ProviderRegistration→lease/result | Redis TTL/revision/event | lost lease unavailability then recovery | `REQ-007`,`REQ-010`,`REQ-011` |
| 10 | Provider handler→Spring proxied bean→Guard Advisor | implementation `@RateLimitGuard(ruleId,key)` | invocation→Guard plan/key→algorithm/backend decision | Local 或 Redisson Guard state | `RATE_LIMITED` throws before target method | `REQ-012`,`REQ-013` |
| 11 | RPC Guard exception adapter→observer | `RpcProviderExceptionMapper#map` | `AccessGuardRejectedException(RATE_LIMITED)`→UNAVAILABLE + provider stage + rate-limit type | no extra state | 非 rate-limit Guard exception不由该 Adapter吞掉 | `REQ-013`,`REQ-020` |
| 12 | Provider proxy→business method | sync/async binding invoke | Message→Message/CompletionStage | business-owned side effects | 业务异常按现有 mapper映射到 reserved non-UNAVAILABLE business status并原样终止；业务 mapper 使用 UNAVAILABLE 是合同错误/发布阻断 | `REQ-014`,`REQ-015`,`REQ-019`,`REQ-020` |

##### Consumer load-balancing algorithm contracts

候选输入在进入算法前统一处理：过滤 null、expired、shutdown/draining、当前 invocation 已尝试 endpoint；按 `instanceId + leaseId + host + port + secure` stable identity 排序；weight 来自 §10 canonical projection。算法只返回 selection handle，不创建/关闭 Channel。

| Effective algorithm | Exact selection rule | Mutable state and concurrency | Membership/weight change | Retry/exclusion behavior |
| --- | --- | --- | --- | --- |
| `RANDOM` | `ThreadLocalRandom.nextInt(candidateCount)` 均匀选一个 | no shared state | next invocation sees new immutable list | exclude 后在剩余集合重新随机 |
| `WEIGHTED_RANDOM` | 以 `long totalWeight` 计算 `[0,total)`，按 stable order cumulative weight 命中 | no shared state；sum uses overflow-safe add and max `10000 * endpoints` bound | each call uses current weights | exclude 后重算 total；不选 weight<=0（写侧已拒绝，读侧回退100） |
| `ROUND_ROBIN` | per exact service query atomic sequence floorMod list size | one `AtomicLong`; overflow-safe floorMod | list change保留 sequence，不承诺某 endpoint 立即连续公平 | exclude 后对剩余 stable list轮询 |
| `SMOOTH_WEIGHTED_ROUND_ROBIN` | 每轮 `current += weight`，选最大并减 total weight | per service synchronized small map keyed endpoint identity | remove stale keys；retained key保留 current；weight change next round生效 | excluded endpoint本轮不参与 current/total |
| `CONSISTENT_HASH` | SHA-256 affinity key→unsigned 64-bit；clockwise search on ring；每 endpoint `consumer.consistent-hash-virtual-nodes` 个 vnode，默认160，node hash=`endpointIdentity#vnodeIndex` | immutable `NavigableMap` per `(query,revision,endpoint identities)`；atomic replace | snapshot membership/identity变化重建；weight不影响 unweighted ring | attempted node被跳过，继续 clockwise；一圈无候选则 unavailable |
| `LEAST_IN_FLIGHT` | compatibility: choose lowest local in-flight, stable identity tie-break | per endpoint LongAdder + selection handle close | remove counters after endpoint absent and count 0 | excluded candidates omitted |

`CONSISTENT_HASH` deliberately is unweighted；如果未来需要 weighted consistent hash，必须新增独立 enum/Spec，不把 weight 暗中改变 vnode 数。Typed invocation key 由 `INTERNAL-008` 产生一次；generic invocation 使用显式 `affinityKey`。Raw key never enters logs/metrics/metadata。`consumer.consistent-hash-virtual-nodes` valid range 16–4096；a value change rebuilds rings at process restart/config rebind boundary，not mid-call。

##### Access Guard rate-limit Strategy/Factory contracts

`RateLimitPolicy` 在现有 `RateLimitRequest` 中增加 required `algorithm`，仍只依赖 `RateLimitBackend`。Local/Redisson backend 各自构造一个 `RateLimitAlgorithmStrategyFactory`；Factory 接收该 storage 下三个唯一 Strategy，启动时校验 enum 覆盖完整、无重复，`acquire(request)` 只按 `request.algorithm` 分派。这样保留现有 backend/failure-policy SPI，不让 RPC 认识算法实现，也避免一个 backend 方法堆叠三套 switch-heavy state machine。

| Storage Strategy | State identity/shape | Atomic/concurrent rule | Config change/reset | Expiry/cleanup | Failure behavior |
| --- | --- | --- | --- | --- | --- |
| Local Token Bucket | strategy-owned map key=`ruleId,stateVersion,keyHash`；token/lastRefill/config | per-key `ConcurrentHashMap.compute` | config mismatch creates full bucket | existing idle TTL/maxEntries/evict | `StoreOperationException` follows Guard failure policy |
| Local Leaky Bucket | separate strategy map same semantic key；level/lastLeak/config | per-key compute；elapsed full-period leak before add | config mismatch creates empty bucket | same bounded cleanup | same |
| Local Sliding Window | separate strategy map；bounded deque of accepted monotonic timestamps | per-key lock/compute；evict `<= now-window` then append | config mismatch clears deque | idle TTL + capacity bound | same |
| Redisson Token Bucket | existing `AccessGuardRedisKeyFactory.rateLimit(...)` HASH and current Lua | one Redis TIME + Lua transaction | stored config mismatch resets full | existing `PEXPIRE(idleTtl)` | existing wrapped store error |
| Redisson Leaky Bucket | existing base key suffixed `:leaky-bucket` HASH；level/lastLeak/config | one Redis TIME + Lua transaction | config mismatch resets empty | `PEXPIRE(idleTtl)` | wrapped store error |
| Redisson Sliding Window | existing base key suffixed `:sliding-window` LIST；each accepted entry encodes config version/capacity/window + millisecond timestamp | one Redis TIME + Lua；duplicate timestamps are valid LIST entries；pop expired then length-check/push | oldest-entry config mismatch atomically `DEL` then starts a new window | `PEXPIRE(idleTtl)`；list length <= capacity | wrapped store error |

算法从一个值切到另一个值时，即便业务未更新 `stateVersion`，不同 suffix/Redis type也不冲突；旧非当前算法 key 只由已有 idle TTL回收。`TOKEN_BUCKET` 保持现有 unsuffixed key，避免已有规则升级后丢失全部 quota state。Local 三 Strategy 的 map彼此分离，因此不需要把 algorithm 加进既有 key hash。

#### 7.3.2 Critical-path Mermaid swimlane

```mermaid
sequenceDiagram
    actor Caller as Business/Generic Caller
    participant Proxy as CGLIB Proxy or GenericInvoker
    participant Exec as RpcInvocationExecutor
    participant Cache as Fixed-mode Directory Cache
    participant LB as RpcLoadBalancer
    participant Pool as Shared Channel Pool
    participant Remote as Selected same-mode transport endpoint
    participant Provider as Routed or direct RPC Provider
    participant Guard as Access Guard on Provider
    participant Bean as Provider business method
    participant DDC as DDC Registry
    participant Redis as Redis Topic/Lease

    par Background discovery
        Redis-->>Cache: DdcRegistryEvent hint
        Cache->>DDC: GetServiceInstances full snapshot
        DDC-->>Cache: revision + leased endpoints
    and Provider liveness
        Provider->>DDC: heartbeat(active lease)
        DDC->>Redis: renew TTL only
    end

    Caller->>Proxy: typed method / generic invocation
    Proxy->>Exec: precompiled plan + request
    Exec->>Cache: current exact-key candidates
    Cache->>LB: immutable candidates + weight/hash key
    LB-->>Cache: selected endpoint
    Cache->>Pool: acquire endpoint channel lease
    Pool-->>Exec: shared ManagedChannel
    Exec->>Remote: unary attempt within remaining deadline
    alt mode is GATEWAY
        Remote->>Provider: existing routed unary
    else mode is DIRECT
        Note over Remote,Provider: Remote endpoint is this Provider instance
    end
    Provider->>Guard: invoke Spring proxied Provider method
    alt success
        Guard->>Bean: permit then invoke
        Bean-->>Provider: response
        Provider-->>Remote: response
        Remote-->>Exec: response
        Exec-->>Proxy: Message/bytes or complete future
        Proxy-->>Caller: result
    else Guard rate limited
        Guard-->>Provider: AccessGuardRejectedException(RATE_LIMITED)
        Provider-->>Remote: UNAVAILABLE + provider stage + rate-limit type
        Remote-->>Exec: UNAVAILABLE + provider stage; rate-limit type only survives Direct
        alt untried same-mode candidate and retry budget
            Exec->>Cache: exclude attempted and reselect
        else exhausted
            Exec-->>Caller: UNAVAILABLE; Direct may map RPC_RATE_LIMITED
        end
    else acquisition or availability failure
        Remote-->>Exec: acquisition failure or UNAVAILABLE
        Exec->>Cache: exclude attempted and reselect same mode
    else cancellation or business/semantic failure
        Remote-->>Exec: CANCELLED / non-UNAVAILABLE business status
        Exec-->>Caller: propagate; no retry, no fail strategy
    end
```

#### 7.3.3 Transactions, consistency, concurrency, and idempotency

| Concern/state change | Owner and boundary | Mechanism/isolation/lock | Concurrent or duplicate behavior | Commit/visibility point | Failure result | Requirements/tests |
| --- | --- | --- | --- | --- | --- | --- |
| Provider endpoint cache | Consumer Manager per exact query | synchronized reconcile + immutable list + monotonic revision | readers lock-free snapshot；old revision dropped | volatile/list replacement | last unexpired snapshot only | `REQ-005`,`REQ-006` / `TEST-010`–`015` |
| LB state | LB instance per service query/algorithm | atomic sequence or synchronized weighted state; ring immutable | each selection linearizable only for its algorithm state | selected candidate returned | empty/no key error | `REQ-008`,`REQ-009` / `TEST-016`–`025` |
| Channel ownership | Channel pool per transport key | concurrent map + ref/in-flight counters | same endpoint converges to one Channel entry | successful acquire | draining/unavailable | `REQ-011`,`REQ-017` / `TEST-026`–`030` |
| Logical retry | InvocationExecutor only | immutable deadline + attempt counters + excluded identity set | same invocation may execute business more than once | first accepted success/terminal completion wins | unknown business outcome documented | `REQ-019`,`REQ-020` / `TEST-049`–`056` |
| Business idempotency | Downstream business code/data store | unique document/order key, upsert/overwrite or business dedupe | framework does not check/store key | business transaction commit | duplicate side effects if business violates contract | `REQ-019` / business acceptance evidence |
| Provider lease | DDC Registry + Provider lease manager | exact service+instance+lease identity, Redis lock/TTL | stale heartbeat/deregister rejected | DDC returns renewed/registered lease | Provider unavailable/re-register | `REQ-007`,`REQ-010`,`REQ-011` |
| Rate-limit state | Access Guard rule/key + selected Local/Redisson backend | Local monotonic ticker/atomic state；Redis server time + atomic Lua | concurrent acquire cannot exceed selected algorithm rule | Guard permit decision | RATE_LIMITED→RPC UNAVAILABLE；backend failure按 Guard failure policy | `REQ-012`,`REQ-013` / `TEST-039`–`048` |
| Async completion | Executor and Provider observer | atomic terminal guard | cancel/response/error races complete once | first terminal CAS | losing completion ignored/logged | `REQ-015`,`REQ-020` / `TEST-057`–`061` |

本框架没有跨服务事务。Retry 可能发生在“Provider 已提交但 Consumer 未收到响应”的未知结果窗口；`idempotent=false` 不会阻止重试。业务实现必须用稳定业务键处理重复，且测试必须证明其业务不变量；框架测试只证明调用次数和失败分类。

#### 7.3.4 Failure semantics, recovery, and reconciliation

| Failure point | Detection | Immediate control flow | Data/transaction state | Retry and idempotency | Caller/frontend result | Recovery/reconciliation owner | Verification |
| --- | --- | --- | --- | --- | --- | --- | --- |
| Reference invalid | startup resolver/validator | abort injection/startup | none | none | `RPC_INVALID_CONTRACT` | developer fixes declaration | contract tests |
| DDC initial pull fails | subscription start exception | Gateway primary保持 fail-fast；Direct 进入 DEGRADED/按当前配置失败 | no cache | no business retry；不注册另一模式 demand | mode-specific discovery unavailable | DDC/operator | auto-config tests + live gap |
| Redis event lost | no event | periodic reconcile | old cache until interval/lease | none | calls may use still-valid old set | coordinator | fake scheduler test |
| DDC reconcile fails | exception/log | expire locally, keep unexpired | no lease extension | next scheduled reconcile | empty after expiry→unavailable | DDC/operator | subscription tests |
| selected Channel cannot connect | awaitReady/acquire | exclude endpoint，按 LB 选下一同模式 candidate | no business call known | availability retry；每实例最多一次 | eventual result/mode-specific unavailable | Consumer | component test |
| endpoint returns UNAVAILABLE | gRPC Status + failure stage | exclude endpoint，按 `DEC-008/009` 只换同模式节点 | business outcome may be unknown | business owns duplicates | success or terminal mapped error | Business + Consumer | retry/no-cross-mode matrix |
| business validation/permission/precondition/conflict/not-found | any non-UNAVAILABLE business/semantic status | immediate terminal；skip retry and all fail strategies | Provider business decides commit/rollback | never retry | original stable business error | business Provider | status + failStrategy non-invocation tests |
| Provider Guard rate limited | `AccessGuardRejectedException` with `RATE_LIMITED` | Adapter→UNAVAILABLE/provider/rate-limit；有候选则换同模式节点 | bean not invoked；Guard state已消费/拒绝 | availability retry；不检查 idempotent | success from another node or exhausted `RPC_RATE_LIMITED` | caller/Provider owner | Guard algorithm + RPC adapter/AOP tests |
| Deadline exhausted | remaining nanos<=0/status | cancel active call; terminal | business outcome may be unknown | no further retry | `RPC_DEADLINE_EXCEEDED` | business idempotency/support | deadline tests |
| async caller cancels | CompletionStage/Future cancel | cancel ClientCall, release selection/channel once | Provider may already run | no retry | `RPC_CANCELLED` | caller/business | cancellation test |
| availability paths exhausted | attempted candidates empty or transmission budget reached | apply configured availability failStrategy once | prior business outcome may be unknown | no further retry；`FAIL_OPEN` returns null | FAIL_CLOSED error / nullable null / local fallback | Caller/business owner | exhaustion + nullable contract tests |
| Provider heartbeat loses lease | not renewed/exception | availability off, recover register | old TTL expires | registration retry on heartbeat schedule | new calls UNAVAILABLE | Provider lifecycle | lease manager test |
| graceful drain timeout | awaitTermination false | shutdownNow | unfinished calls cancelled | caller may retry per policy | cancelled/unavailable | Provider/Consumer operator | lifecycle test |

#### 7.3.5 Observability and operational boundaries

| Signal/runbook | Emitting owner and point | Fields/dimensions | Sensitive-data rule | Success/failure threshold | Alert/dashboard/operator action | Verification boundary |
| --- | --- | --- | --- | --- | --- | --- |
| invocation structured log | Executor on terminal/reselection | service, method, immutable mode, algorithm, attemptCount, distinctEndpointCount, errorCode, latency, invocationId | no request/response/Authorization/hash key | warn on exhaustion；normal reselection debug/trace | investigate same-mode endpoints/DDC | source/unit; production aggregation unverified |
| discovery log | Manager on snapshot/reconcile failure | query service identity, old/new revision, endpoint count | no DDC credential/metadata payload | warn on reconcile failure or revision anomaly | check DDC RPC/Redis | source/component; live unverified |
| lease lifecycle log | LeaseManager | service identity, instanceId, leaseId hash/suffix, state/result | no admission ticket/HMAC secret | warn on heartbeat/recovery/deregister failure | check DDC/admission | existing + tests; live unverified |
| rate-limit event/metric/log | Access Guard at decision/finalization；RPC Adapter at mapped terminal | ruleId, decision, algorithm, storage, retryAfter bucket；RPC adds service/fullMethodName/invocationId | no payload/caller token/raw key/affinity key | use Guard existing bounded observability；RPC Adapter avoids duplicate per-attempt stack logs | tune Guard rule/backend/instance count | Guard unit/component；RPC integration；production dashboard unverified |
| drain log | Provider/Channel pool | component, state, inFlight, timeout, forced | no payload | warn if forced shutdown/inFlight>0 | investigate long calls/deadlines | lifecycle tests |

RPC Starter 不新增 Micrometer 依赖；Access Guard 已有 optional Micrometer/Actuator 观测能力并继续作为限流指标 owner。RPC 只追加低基数的 mode/endpoint-attempt/status 映射日志，不复制 Guard 指标。

#### 7.3.6 Conclusion evidence chain

| Conclusion | Repository/user evidence | Constraint or requirement | Design decision | Consequence and trade-off | Verification and acceptance evidence |
| --- | --- | --- | --- | --- | --- |
| 复用 DDC 订阅而非新建 cache | `EVD-007`–`EVD-010` | `REQ-005`,`REQ-006` | 保留 coordinator/current snapshot，扩展 RPC endpoint model/LB | 最少状态；更新延迟受 reconcile interval 约束 | event/poll/revision/expiry tests + live boundary |
| Retry 统一但不做幂等判断 | `EVD-006`,`EVD-019`,`EVD-021` | `REQ-019`,`REQ-020` | executor 按故障白名单/预算/Deadline，不读 idempotent | 满足用户；未知结果可重复副作用 | non-idempotent retry test + 业务幂等验收 |
| Mode 由客户端固定且不跨模式 | 用户 2026-08-21 决定 1、`EVD-020` | `REQ-003`,`REQ-004` | Strategy Factory 创建一个 immutable-mode strategy；executor 只排除/重选其候选 | 去掉 Gateway fallback 容灾，但拓扑、安全和错误可预测 | Direct/Gateway 双向 no-call tests + process call counters |
| CGLIB + plan cache替代JDK handler | `EVD-004`–`EVD-006` | `REQ-018` | interface-only CGLIB、proxy-time Method plan | 少热路径反射；AOT/native 需额外配置且未承诺 | proxy type/cache/object-method tests；benchmark非通过门槛 |
| 多路复用由共享 Channel 所有权兑现 | `EVD-017` + gRPC transport baseline | `REQ-017`,`REQ-021` | endpoint-keyed pool，不造协议 | 降连接数；ref/drain 并发更复杂 | factory create-count、parallel unary、drain tests |
| Provider 限流复用 Guard | 用户决定 3、`EVD-015`,`EVD-016`,`EVD-022`–`EVD-024` | `REQ-012`,`REQ-013` | 保持 Provider marker；Guard Strategy/Factory 补三算法；RPC Adapter 映射 UNAVAILABLE | 避免双策略源；RPC/Guard 有显式可选集成边界；Redisson 多脚本测试成本增加 | deterministic Local/Redis contracts + AOP non-invocation + mapper tests |
| Generic 复用 canonical raw unary | `EVD-018`,`EVD-025`、用户决定 5 | `REQ-016`,`REQ-021` | `fully.qualified.Service/Method` + bytes/descriptor，不加 generic service | wire兼容；调用方承担 schema正确性；不接受点号 alias | typed/raw parity + identity/malformed payload tests |

## 8. Package Structure and Code File Tree

### 8.1 Current relevant tree

```text
egon-cola-components/egon-cola-component-rpc/
├── README.md / README.zh-CN.md
├── egon-cola-component-rpc-starter/src/main/java/top/egon/cola/component/rpc/
│   ├── annotation/{EgonRpcReference,EgonRpcDirectReference,EgonRpcProvider,LoadBalance,EgonRpcMethod}.java
│   ├── config/{EgonRpcAutoConfig,EgonRpcProperties}.java
│   ├── consumer/
│   │   ├── channel/{RpcConsumerChannelFactory,RpcEndpoint,RpcInvocationChannelProvider}.java
│   │   ├── gateway/{RpcConsumerGatewayManager,GatewayRpcInvocationChannelProvider,RpcGatewayDirectory,RpcGatewayEndpoint,RpcGatewaySnapshot}.java
│   │   ├── provider/{RpcConsumerProviderManager,ProviderRpcInvocationChannelProvider,RpcProviderDirectory,RpcProviderEndpoint,RpcProviderSnapshot}.java
│   │   └── proxy/{EgonRpcReferenceBeanPostProcessor,RpcConsumerProxyFactory,RpcDirectReferenceProxyFactory,RpcConsumerInvocationHandler}.java
│   ├── contract/{descriptor,validation,catalog,snapshot}/ (existing exact contract packages)
│   ├── exception/{EgonRpcErrorCode,EgonRpcException,RpcStatusExceptionMapper}.java
│   └── provider/{binding,lifecycle,registration,server}/ (existing exact provider packages)
└── egon-cola-component-rpc-ddc-adapter/src/main/java/top/egon/cola/component/rpc/ddc/
    ├── autoconfigure/DdcRpcAutoConfiguration.java
    └── registry/{DdcRpcProviderDirectory,RpcDdcRegistrySnapshotLoader}.java
```

### 8.2 Target tree

下列是设计目标文件，不代表实施顺序。`package-info.java` 仅在新稳定包实际创建时同步新增；不创建空包。

```text
egon-cola-components/egon-cola-component-rpc/
├── README.md                                                        MODIFY
├── README.zh-CN.md                                                  MODIFY
├── egon-cola-component-rpc-starter/
│   ├── pom.xml                                                       MODIFY optional Access Guard integration dependency
│   ├── src/main/java/top/egon/cola/component/rpc/
│       ├── annotation/
│       │   ├── EgonRpcReference.java                                MODIFY
│       │   ├── EgonRpcDirectReference.java                          MODIFY
│       │   ├── EgonRpcMethod.java                                   MODIFY JavaDoc/return rules
│       │   └── LoadBalance.java                                     MODIFY
│       ├── config/
│       │   ├── EgonRpcAutoConfig.java                               MODIFY
│       │   ├── EgonRpcProperties.java                               MODIFY
│       │   └── RpcAccessGuardAutoConfiguration.java                 CREATE conditional adapter wiring
│       ├── consumer/
│       │   ├── channel/
│       │   │   ├── RpcConsumerChannelFactory.java                   MODIFY
│       │   │   ├── RpcEndpoint.java                                 MODIFY default weight contract
│       │   │   ├── RpcConsumerChannelPool.java                      CREATE
│       │   │   ├── RpcChannelKey.java                               CREATE
│       │   │   └── RpcChannelLease.java                             CREATE
│       │   ├── invocation/
│       │   │   ├── package-info.java                                CREATE
│       │   │   ├── RpcInvocationExecutor.java                       CREATE
│       │   │   ├── RpcInvocationPlan.java                           CREATE
│       │   │   ├── RpcInvocationContext.java                        CREATE
│       │   │   └── RpcInvocationMode.java                           CREATE
│       │   ├── lifecycle/
│       │   │   ├── package-info.java                                CREATE
│       │   │   ├── RpcConsumerLifecycleCoordinator.java             CREATE
│       │   │   └── RpcConsumerRuntimeState.java                     CREATE
│       │   ├── loadbalance/
│       │   │   ├── package-info.java                                CREATE
│       │   │   ├── RpcLoadBalancer.java                             CREATE
│       │   │   ├── RpcLoadBalancers.java                            CREATE (nested concrete strategies)
│       │   │   ├── RpcLoadBalanceContext.java                       CREATE
│       │   │   └── RpcLoadBalanceKeyResolver.java                   CREATE
│       │   ├── reference/
│       │   │   ├── package-info.java                                CREATE
│       │   │   ├── RpcReferenceMode.java                            CREATE
│       │   │   ├── RpcReferencePolicy.java                          CREATE
│       │   │   ├── RpcReferenceDefinition.java                      CREATE
│       │   │   ├── RpcReferenceDefinitionResolver.java              CREATE
│       │   │   ├── RpcReferenceStrategyFactory.java                 CREATE
│       │   │   ├── RpcReferenceStrategy.java                        CREATE
│       │   │   ├── GatewayRpcReferenceStrategy.java                 CREATE
│       │   │   └── DirectRpcReferenceStrategy.java                  CREATE
│       │   ├── generic/
│       │   │   ├── package-info.java                                CREATE
│       │   │   ├── RpcGenericInvoker.java                           CREATE
│       │   │   ├── RpcGenericTargetCache.java                       CREATE bounded target/plan/subscription cache
│       │   │   └── RpcGenericInvocation.java                        CREATE
│       │   ├── gateway/
│       │   │   ├── RpcConsumerGatewayManager.java                   MODIFY
│       │   │   └── RpcGatewayEndpoint.java                          MODIFY weight
│       │   ├── provider/
│       │   │   ├── RpcConsumerProviderManager.java                  MODIFY
│       │   │   └── RpcProviderEndpoint.java                         MODIFY weight
│       │   └── proxy/
│       │       ├── EgonRpcReferenceBeanPostProcessor.java           MODIFY
│       │       ├── RpcConsumerProxyFactory.java                     MODIFY CGLIB
│       │       ├── RpcConsumerMethodInterceptor.java                CREATE
│       │       ├── RpcDirectReferenceProxyFactory.java              DELETE after merge
│       │       └── RpcConsumerInvocationHandler.java                DELETE after merge
│       ├── contract/
│       │   ├── descriptor/{RpcContractDescriptor,RpcMethodDescriptor}.java MODIFY indexed plan/return mode
│       │   └── validation/RpcContractValidator.java                 MODIFY sync/async shape
│       ├── context/invocation/RpcMetadataKeys.java                  MODIFY shared rate-limit error-type key
│       ├── exception/
│       │   ├── EgonRpcErrorCode.java                                MODIFY
│       │   └── RpcStatusExceptionMapper.java                        MODIFY
│       └── provider/
│           ├── binding/{RpcProviderBinding,RpcProviderMethodBinding,RpcProviderBeanScanner}.java MODIFY async binding/proxy regression
│           ├── lifecycle/
│           │   ├── RpcProviderLifecycle.java                        MODIFY
│           │   └── RpcProviderRuntimeState.java                     CREATE
│           ├── registration/RpcProviderLeaseManager.java            MODIFY weight metadata/lifecycle logs
│           └── server/
│               ├── RpcServerServiceDefinitionFactory.java           MODIFY async completion/proxied invocation
│               └── RpcAccessGuardExceptionMapper.java               CREATE conditional rate-limit Adapter
│   └── src/main/resources/META-INF/spring/
│       └── org.springframework.boot.autoconfigure.AutoConfiguration.imports MODIFY add conditional Guard integration
└── egon-cola-component-rpc-ddc-adapter/
    └── src/main/java/top/egon/cola/component/rpc/ddc/
        └── registry/{DdcRpcProviderDirectory,DdcRpcGatewayDirectory}.java MODIFY weight projection

egon-cola-components/egon-cola-component-access-guard-starter/
├── README.md / README.zh-CN.md                                      MODIFY three-algorithm rules
└── src/main/java/top/egon/cola/component/accessguard/
    ├── core/plan/AdmissionConfig.java                               MODIFY enum/config validation
    ├── policy/ratelimit/
    │   ├── RateLimitPolicy.java                                     MODIFY pass algorithm to backend
    │   ├── RateLimitAlgorithmStrategy.java                          CREATE
    │   └── RateLimitAlgorithmStrategyFactory.java                   CREATE
    └── store/
        ├── RateLimitRequest.java                                    MODIFY algorithm-specific normalized fields
        ├── local/LocalRateLimitBackend.java                         MODIFY delegate three local strategies
        └── redisson/
            ├── AccessGuardRedisKeyFactory.java                      MODIFY algorithm suffix while token key stays compatible
            └── RedissonRateLimitBackend.java                        MODIFY select three atomic Lua strategies
```

测试在现有对应 `src/test/java` 包内 MODIFY/CREATE；RPC test contract 可增加一个 async Java Contract 和 generic raw fixture，但不新增 `.proto` 方法，优先复用现有 Echo unary descriptor。

### 8.3 Package and file responsibilities

| Operation | Path/package | Symbols | Responsibility | Dependencies | Requirements |
| --- | --- | --- | --- | --- | --- |
| Modify | `annotation/*Reference.java` | two annotations | 保持公开入口，Direct 只补共同字段；两者各自固定模式且不含跨模式开关 | annotation enums only | `REQ-001`–`REQ-004` |
| Keep | `annotation/EgonRpcProvider.java` | field-free marker | 仅发现 Provider bean；限流归 Provider 实现方法 `@RateLimitGuard` | none | `REQ-012`,`REQ-013` |
| Create | `consumer.reference` | definition/resolver/Factory/strategies | 归一声明；Factory 基于 mode 一次选择 Strategy；Strategy 只访问自身 Manager | managers、properties | `REQ-001`–`REQ-004` |
| Create | `consumer.invocation` | executor/plan/context/mode | 一处实现 blocking/async/generic/retry/deadline/terminal | gRPC ClientCalls、status mapper | `REQ-014`–`REQ-020` |
| Create | `consumer.lifecycle` | coordinator/runtime state | 在调用开放前安装 discovery/pool，统一 drain/cleanup gate | managers、executor、generic cache、pool | `REQ-005`,`REQ-011`,`REQ-023` |
| Create | `consumer.loadbalance` | SPI/factory/context/resolver | 五算法和选择生命周期 | endpoint candidate only | `REQ-008`,`REQ-009` |
| Modify/Create | `consumer.channel` | endpoint default weight + pool/key/lease | 为两模式提供 neutral weight；跨 query/mode共享 multiplex Channel并排空 | existing ChannelFactory | `REQ-008`,`REQ-009`,`REQ-011`,`REQ-017` |
| Modify | `consumer.gateway/provider Managers` | cache selection | 保留订阅/快照，改用 LB+pool；只提供各自模式 primary demand | neutral Directory | `REQ-003`–`REQ-009`,`REQ-017` |
| Modify/Create/Delete | `consumer.proxy` | CGLIB factory/interceptor | proxy-time plans；移除重复 Factory/Handler | Spring CGLIB、executor | `REQ-018` |
| Create | `consumer.generic` | generic API/models/bounded cache | exact service/fullMethodName raw unary；bounded reuse of discovery/plan state | executor、raw marshaller、Reference Strategy | `REQ-005`,`REQ-016`,`REQ-017` |
| Modify | `contract.validation/descriptor` | return-mode validation/index | 接受 Message或CompletionStage<Message>；O(1) method lookup | generated descriptors | `REQ-014`,`REQ-015`,`REQ-018` |
| Modify | `context.invocation/RpcMetadataKeys` + status mapper | safe error-type metadata | Provider Adapter/Direct mapper共享 `rate-limit` type；Gateway仍按现状重建安全 trailer | gRPC Metadata | `REQ-013`,`REQ-020` |
| Modify/Create | RPC Provider server integration | proxied binding + `RpcAccessGuardAutoConfiguration/RpcAccessGuardExceptionMapper` | optional class-safe AutoConfig；保持 Spring proxy advice；async observer；只把 Guard RATE_LIMITED 适配为 Provider-stage UNAVAILABLE | optional Guard API、existing mapper chain | `REQ-012`–`REQ-015`,`REQ-020` |
| Modify/Create | Access Guard `policy.ratelimit/store` | algorithm Strategy/Factory + Local/Redisson execution | Guard rule/key/storage authority内实现三算法、startup validation、retryAfter | existing Guard engine/backends | `REQ-012`,`REQ-013` |
| Modify | Provider lifecycle | runtime State | READY/DRAINING/FAILED 与 heartbeat/drain | existing registry/server | `REQ-007`,`REQ-010`,`REQ-011` |
| Modify | DDC Provider/Gateway Directories | metadata projection | `ServiceInstanceMetaCodec.decode(...).weight`→neutral endpoint | DDC SDK | `REQ-008`,`REQ-009`,`REQ-021` |
| Modify | README/tests/properties/errors | docs/config/error/tests | 公开合同同步、兼容/验收 | existing build | `REQ-022` |

## 9. Interface Definitions

本章只设计 RPC Component 的受影响 Java API/SPI。业务 `.proto` unary operation、DDC Registry RPC 和 Gateway RPC 入站/转发 operation 均保持 wire 不变，不为它们分配新的受影响接口 ID。

### 9.1 Interface Inventory

| ID | Change/necessity verdict | Name/purpose | Kind | Consumer | Owner | Method + URL / symbol / topic | Input | Output | Auth/tenant | Error model | Idempotency/version | Requirements |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| `INTERNAL-001` | Existing/Keep+runtime expansion | Gateway Reference declaration | Java annotation API | Spring business bean field | RPC Starter | `@EgonRpcReference` | shared call-site fields | Gateway-primary typed proxy | existing RPC metadata/auth | startup contract error；runtime stable RPC errors | existing annotation binary/source shape; additive resolver field only | `REQ-001`,`REQ-002`,`REQ-008`,`REQ-018`–`REQ-020` |
| `INTERNAL-002` | Existing/Modify | Direct Reference declaration | Java annotation API | Spring business bean field | RPC Starter | `@EgonRpcDirectReference` | target scope + shared policy；mode fixed Direct | Direct-only typed proxy | existing RPC metadata/auth | discovery/same-mode exhaustion/terminal errors | additive common fields；no cross-mode field | `REQ-001`,`REQ-003`,`REQ-004`,`REQ-008`,`REQ-009`,`REQ-019`,`REQ-020` |
| `INTERNAL-003` | Existing/Keep annotation + runtime/algorithm expansion | Provider method rate-limit declaration | Java annotation API + Guard rule contract | RPC Provider implementation method | Access Guard; RPC Adapter consumes rejection | `@RateLimitGuard(value, key)` | rule ID + optional key；algorithm/limits from Guard config | permit or `AccessGuardRejectedException(RATE_LIMITED)`；RPC maps latter to UNAVAILABLE | existing server security/Guard key contributors | invalid startup；UNAVAILABLE/rate-limit at runtime | annotation source shape unchanged；algorithm enum additive | `REQ-012`,`REQ-013`,`REQ-020` |
| `INTERNAL-004` | Existing/Keep | Typed blocking unary method | Java RPC Contract | business Consumer/Provider | business Contract + RPC Starter | `Response method(Request)` with `@EgonRpcMethod` | one generated Protobuf Message | one generated Protobuf Message | existing metadata/token relay | current Status→EgonRpcException | wire descriptor unchanged | `REQ-014`,`REQ-018`–`REQ-021` |
| `INTERNAL-005` | New/Add after async necessity | Typed async unary method | Java RPC Contract | business Consumer/Provider | business Contract + RPC Starter | `CompletionStage<Response> method(Request)` with `@EgonRpcMethod` | one generated Protobuf Message | CompletionStage of exact generated response | same as blocking | async stable exception/cancel | wire descriptor unchanged；Java shape v2 additive | `REQ-015`,`REQ-018`–`REQ-021` |
| `INTERNAL-006` | New/Add after generic necessity | Generic blocking invocation | Java programmatic API | infrastructure/operator adapter | RPC Starter | `RpcGenericInvoker#invokeBlocking(RpcGenericInvocation)` | exact target/method/options/raw bytes | raw response bytes | same interceptor metadata/auth as typed | stable synchronous EgonRpcException | caller/business owns duplicate safety | `REQ-016`,`REQ-019`–`REQ-021` |
| `INTERNAL-007` | New/Add after generic necessity | Generic async invocation | Java programmatic API | infrastructure/operator adapter | RPC Starter | `RpcGenericInvoker#invokeAsync(RpcGenericInvocation)` | same as INTERNAL-006 | `CompletionStage<byte[]>` | same as typed | future completes exceptionally/cancelled | same logical invocation semantics | `REQ-015`,`REQ-016`,`REQ-019`–`REQ-021` |
| `INTERNAL-008` | New/Add for real variation | Consistent-hash key resolution | Java SPI | typed invocation plan | RPC application | `RpcLoadBalanceKeyResolver#resolve(RpcLoadBalanceContext)` | service/method/request, never credentials | nonblank stable String | caller context already sanitized | invalid/missing key is plan/invocation error | not persisted/logged; resolver version owned by app | `REQ-009` |

### 9.2 Per-interface Detailed Contracts

#### 9.2.1 INTERNAL-001 — `@EgonRpcReference`

##### Necessity and interaction-cost decision

| Concern | Decision |
| --- | --- |
| Change classification | Existing annotation；字段保留，运行语义补齐；按 `DEC-013` 新增 `loadBalanceKeyResolver` |
| Independent consumer goal | 业务字段以 Gateway 为主路径获得 typed proxy |
| Parameter ownership and derivation | serviceName/request/response 从 Contract；group/version 可由调用点覆盖；timeout/retries/LB/terminal policy 属调用点 |
| Direct/no-new-interface alternative | 保留现有注解，不新增 `@EgonRpcGatewayReference`；内部 resolver 提取共同字段 |
| Caller use of result | 直接执行业务，不 fetch-then-forward |
| Round trips and failure points | 成功一条 Gateway unary；重试使用同总 Deadline；不访问 Provider Directory |
| Verdict | Keep/Expand runtime，满足 `REQ-001`,`REQ-002` |

##### Identity and purpose

- Target: `FIELD`，runtime retention；字段类型继续必须是 `@EgonRpcService` interface。
- Mode: `GATEWAY` primary；注册 primary Gateway demand，启动行为保持现有 fail-fast。
- Version: 现有属性不删除/改名/改默认；新增属性只能 additive。

##### Request parameters

| Name | Type | Required/default | Validation/meaning | Resolution/source |
| --- | --- | --- | --- | --- |
| `timeoutMs` | `long` | `-1` inherit | `-1` 或 `>0`；effective 为所有正 deadline ceiling 的最小值 | method/reference/service/consumer config |
| `retries` | `int` | `-1` inherit | `-1` 或 `0..consumer.maxRetries`；表示初始 attempt 之后的次数 | method > reference > service > consumer default |
| `loadBalance` | enum | `INHERIT` | 支持 §4 `REQ-008` 与兼容 `LEAST_IN_FLIGHT` | reference > service > consumer default |
| `group` | String | empty=Contract | trim 后需匹配 safe segment | annotation/Contract |
| `version` | String | empty=Contract | trim 后需匹配 safe segment | annotation/Contract |
| `fallbackBean` | String | empty | 仅 terminal `LOCAL_FALLBACK` 可非空；Bean 必须实现 Contract | ApplicationContext |
| `failStrategy` | enum | `INHERIT` | `FAIL_CLOSED/FAIL_OPEN/LOCAL_FALLBACK`；只在 availability paths exhausted 后执行；`FAIL_OPEN` 按 `DEC-014` 返回 null | annotation/config |
| `loadBalanceKeyResolver` | String | empty | effective `CONSISTENT_HASH` 时必须非空且命名 Bean 实现 `INTERNAL-008` | `DEC-013` |

##### Success response

注入成功后字段得到 INTERNAL-004/INTERNAL-005 所定义的 CGLIB typed proxy。Reference 自身无网络 response；Proxy 成功结果严格等于业务 Proto response，异步结果只多一层 CompletionStage。

##### Error responses

| Condition | Outcome | Retryable |
| --- | --- | --- |
| 非 interface/无 `@EgonRpcService`/字段冲突 | startup `RPC_INVALID_CONTRACT`，包含 bean+field，不含敏感值 | No |
| primary Gateway Directory 缺失/启动未 READY | `RPC_GATEWAY_UNAVAILABLE` | 仅按 `DEC-009` availability policy |
| Consistent Hash resolver 缺失/返回 blank | startup 或 invocation `RPC_INVALID_CONTRACT` | No |
| terminal local fallback 配置错误 | startup `RPC_INVALID_CONTRACT` | No |

##### Interface logic for frontend and consumers

1. BeanPostProcessor 检查单一 Reference annotation 和 interface field。
2. Resolver 合并注解、Contract、Method、Consumer defaults，并校验每个 plan。
3. Gateway Strategy 注册 primary demand；CGLIB Factory 预生成 Method Plan。
4. Invocation 通过 Gateway endpoint cache/LB/shared Channel 发出。
5. Retry 只按 §7.3.4 availability 白名单在其他 Gateway 实例上执行；总 Deadline 不重置，不读取 idempotent。
6. Gateway availability exhaustion 后才执行 failStrategy；业务/语义错误直接抛出；Direct path 永不作为 Gateway primary 的 fallback。
7. RPC Component 无 frontend；Caller 处理返回值/异常/future。

##### Compatibility and verification

- 旧注解源码与二进制属性保持；运行行为变化是原来未接线的 retries/loadBalance/failStrategy 开始生效。
- 测试：旧 fixture 编译、Gateway-only Directory 查询、字段覆盖、CGLIB 类型、Method Plan、每种终态策略。

#### 9.2.2 INTERNAL-002 — `@EgonRpcDirectReference`

##### Necessity and interaction-cost decision

| Concern | Decision |
| --- | --- |
| Change classification | Existing annotation，补与 Gateway Reference 同义的共同 call-site 字段；不增加跨模式字段 |
| Independent consumer goal | 业务调用只直连精确 RPC_PROVIDER，并在可用性失败时改选其他 Direct Provider |
| Parameter ownership and derivation | biz/app/env 是目标部署身份；service 从 Contract；group/version 可覆盖；策略字段与 INTERNAL-001 同义 |
| Direct/no-new-interface alternative | 保持独立 Direct 注解最兼容；让调用方 try/catch 会复制 deadline/status/attempt；内部 Strategy + Factory 必要 |
| Caller use of result | 直接消费业务结果；不机械转发参数 |
| Round trips and failure points | 成功 1 RTT；失败最多 `min(1+retries, distinct Direct candidates)`；不产生 Gateway RTT |
| Verdict | Modify/Add semantics，满足 `REQ-001`,`REQ-003`,`REQ-004` |

##### Identity and purpose

- Target/retention/type rule 与 INTERNAL-001 相同。
- Mode: `DIRECT` primary；exact query=`bizCode/env/appCode/RPC_PROVIDER/service/group/version/grpc`。
- Mode immutable: Proxy 创建后只持有 `DirectRpcReferenceStrategy`；没有 `fallbackToGateway` 属性、Gateway demand 或 runtime switch API。

##### Request parameters

| Name | Type | Required/default | Validation/meaning | Resolution/source |
| --- | --- | --- | --- | --- |
| `bizCode` | String | Required | safe segment；目标 Provider 业务域 | annotation |
| `appCode` | String | Required | safe segment；目标 Provider 应用 | annotation |
| `env` | String | empty=caller env | safe segment | annotation/process identity |
| `group/version/timeoutMs` | same as INTERNAL-001 | existing | 与 INTERNAL-001 完全同义 | common policy resolver |
| `retries/loadBalance/fallbackBean/failStrategy/loadBalanceKeyResolver` | same as INTERNAL-001 | additive | 与 INTERNAL-001 完全同义 | common policy resolver |

##### Success response

与 INTERNAL-004/INTERNAL-005 相同。Direct 成功只来自选中的 RPC_PROVIDER；任何 Gateway 调用都视为违反 `DEC-008`。

##### Error responses

| Condition | Stable outcome | Notes |
| --- | --- | --- |
| exact Provider cache empty/expired | `RPC_PROVIDER_UNAVAILABLE` | 无每调用 DDC pull；不查询 Gateway |
| 一个 Direct endpoint acquisition/UNAVAILABLE | 排除该 endpoint；在预算内按同一 effective LB 选择其他 Provider | same invocation/remaining deadline |
| 所有 Direct endpoints/attempts 耗尽 | 默认 `RPC_PROVIDER_UNAVAILABLE`；仅当每个 observed failure 均为 rate-limit 时映射 `RPC_RATE_LIMITED`；底层 status仍 UNAVAILABLE | 只执行一次 availability failStrategy；混合 acquisition/unavailable/rate-limit 不伪装为纯限流 |
| business/permission/precondition/conflict/not-found/deadline/cancel | 对应原稳定错误 | 立即 terminal；不换节点、不执行 failStrategy |

##### Interface logic for frontend and consumers

1. 解析 exact Provider query、common policy 和 immutable DIRECT mode。
2. Strategy Factory 只注册 Provider subscription；不得解析/注入 Gateway Directory。
3. 每 attempt 从本地 snapshot 过滤有效/未尝试 endpoint，再调用 effective LB。
4. 使用共享 Channel 发 Direct unary；acquisition/UNAVAILABLE 记录并排除 candidate。
5. 业务/语义错误立即终止并原样映射；不排除后重试另一节点。
6. 同模式候选/预算耗尽后执行一次 availability failStrategy；FAIL_OPEN 返回 null。
7. Caller 对 availability retry 造成的业务重复安全负责。

##### Compatibility and verification

- 新共同属性全部 additive 且有默认值；Direct 成功/失败都不依赖 Gateway，与前置 Spec 保持兼容。
- 测试覆盖 Direct-only、Direct success、multi-provider reselection、all-failed、Gateway manager 调用计数恒为零、业务错误不重试和总 Deadline。

#### 9.2.3 INTERNAL-003 — Provider method `@RateLimitGuard`

##### Necessity and interaction-cost decision

| Concern | Decision |
| --- | --- |
| Change classification | Existing Guard annotation Keep；Guard runtime/algorithm expansion；RPC mapping Add |
| Independent consumer goal | Provider owner在具体导出方法边界保护 Direct/Gateway 两类入站调用 |
| Parameter ownership and derivation | Provider method声明 Guard rule ID/key；Guard configuration owns algorithm/limits/storage/failure policy；RPC binding owns canonical fullMethodName only for status/observability |
| Direct/no-new-interface alternative | 往 `@EgonRpcProvider` 加字段会复制 Guard 并破坏 marker 单一职责；业务手写会复制算法；Gateway limit 对 Direct 无效 |
| Caller use of result | 无参数转发；请求被允许，或收到 Provider-stage UNAVAILABLE |
| Round trips and failure points | Local 无网络；Redisson 每次 admission 一次原子 script；RPC Adapter 不增加 RTT |
| Verdict | Keep `@RateLimitGuard`；Add Guard algorithms + RPC Adapter，满足 `REQ-012`,`REQ-013` |

##### Identity and purpose

`@EgonRpcProvider` 的 `@Component`、TYPE target、runtime retention、无字段和“至少实现一个 `@EgonRpcService`”规则全部保持。`@RateLimitGuard` 放在 Provider 实现方法上，METHOD target、runtime retention，`value` 是必填 Guard rule ID，`key` 是可选显式 key binding；该 rule 只能启用 rate-limit policy。Rate policy 不是服务身份/Proto metadata，不上报 DDC。

##### Request parameters

```java
@RateLimitGuard(value = "rpc.order.create", key = "")
public CompletionStage<CreateOrderResponse> create(CreateOrderRequest request) {
    // business implementation
}
```

Guard rule configuration remains under `egon.cola.component.access-guard.rules.<ruleId>.rate-limit`：

| Algorithm | Existing property mapping | Admission semantics | Local state | Redisson atomic state / retryAfter |
| --- | --- | --- | --- | --- |
| `TOKEN_BUCKET` | `capacity` bucket size；`refillTokens/refillPeriod` refill rate；`requestedTokens` per request | initial full；at each discrete period boundary refill up to capacity；enough tokens then subtract | existing token bucket record | existing hash/Lua extended with algorithm discriminator；time until enough refill periods |
| `LEAKY_BUCKET` | `capacity` water capacity；`refillTokens/refillPeriod` leak rate；`requestedTokens` water added | leak by elapsed full periods down to zero；if `level + requestedTokens <= capacity` admit and add，never queue/block | bucket level + lastLeak + lastAccess | hash/Lua using Redis TIME；time until sufficient water leaks |
| `SLIDING_WINDOW` | `capacity` max calls；`refillPeriod` exact window；`requestedTokens` must be `1`；`refillTokens` retained for config compatibility but ignored and documented N/A | remove accepted timestamps `<= now-window`；if count < capacity append unique timestamp/sequence，else reject | bounded deque/ring of accepted monotonic timestamps | sorted-set/Lua using Redis TIME and unique sequence；time until oldest accepted entry expires |

共同启动校验：`capacity/refillTokens/requestedTokens > 0`，`requestedTokens <= capacity`，`refillPeriod > 0`；Sliding Window 额外要求 `requestedTokens=1`、`capacity<=100000` 以限制本地/Redis 条目。未知 algorithm fail-fast；所有乘法/时间换算溢出安全。状态 key 继续由 Guard authority 生成：`ruleId + stateVersion + hashed resolved key`，Local/Redisson scope 与 failure policy 不由 RPC 改写。`RateLimitAlgorithmStrategyFactory` 必须对每个 enum 恰好返回一个 Strategy；缺失/重复映射在启动期失败。

##### Success response

Acquire 成功只表示可进入业务方法，不保证业务成功；permit 不因业务异常、取消或回滚返还。同步/`CompletionStage` Provider 使用 Guard 现有同一 advice；Async stage 的 rejection/完成必须保持单终态。

##### Error responses

| Condition | gRPC/status | Framework code | Retry rule |
| --- | --- | --- | --- |
| `@RateLimitGuard` rule missing、启用多个 policy、算法/参数非法 | startup failure | Guard startup diagnostic / application start failed | No |
| `AccessGuardRejectedException` 且 `outcome.decision=RATE_LIMITED` | `UNAVAILABLE` + `x-egon-rpc-failure-stage=provider` + `x-egon-rpc-error-type=rate-limit` | Direct exhaustion=`RPC_RATE_LIMITED`；Gateway exhaustion=`RPC_PROVIDER_UNAVAILABLE` | Yes，按同模式未尝试实例和总预算 |
| 其他 `AccessGuardRejectedException` | 不由 rate-limit Adapter 处理；交后续 Mapper/默认行为 | existing rejection/internal mapping | 按最终非-UNAVAILABLE status；不得伪装 rate-limit |
| limiter backend/internal failure | 遵循 Guard rule `failurePolicies.rateLimitBackend` 的 fail-open/local-fallback/fail-closed 合同 | Guard outcome or mapped RPC error | 只有最终映射为 UNAVAILABLE 才可换节点 |

`retryAfter` 继续属于 Guard outcome/event；本 Spec 不新增 retry-after RPC trailer。Rate-limit identity 用现有安全 `x-egon-rpc-error-type` 扩展 `rate-limit` 值。`RpcStatusExceptionMapper` 对单次 Direct UNAVAILABLE 先判断 rate-limit type，再判断 failure stage；Executor 在耗尽时只有“all observed failures are rate-limit”才保留 `RPC_RATE_LIMITED`，混合原因归 `RPC_PROVIDER_UNAVAILABLE`。当前 Gateway `providerTrailers(status)` 只重建 failure stage、不复制 Provider error type，所以 Gateway-mode Caller 始终看到 `RPC_PROVIDER_UNAVAILABLE`；两种模式传输 status 均是用户指定的 `UNAVAILABLE`。本规格不扩大 Gateway scope来统一应用码。

##### Interface logic for frontend and consumers

1. Server interceptor 完成现有 trace/identity/security；未通过者不进入 Handler。
2. Handler 检查 service availability/Provider lifecycle READY。
3. Handler 对 Spring proxy bean 调用 Contract method；Guard Advisor 用 most-specific implementation method 解析 `@RateLimitGuard`。
4. Guard resolver 解析 rule/key，Factory 选择算法 Strategy，Local/Redisson backend 原子 acquire。
5. RATE_LIMITED 在 target method 前抛出；RPC Adapter 映射 UNAVAILABLE/provider/rate-limit，业务调用次数保持 0。
6. acquire 成功调用同步或异步 Provider method；permit 不返还。
7. DRAINING availability gate 先于 Guard advice，以 UNAVAILABLE 拒绝且不消耗 permit。

RPC Starter 对 Access Guard 的 Maven dependency 标记 `optional=true`；没有 Guard 的 Provider/Consumer 不被强制引入 AOP/Web/Redisson。独立 `RpcAccessGuardAutoConfiguration` 进入 AutoConfiguration imports，并以 `@ConditionalOnClass(AccessGuardRejectedException.class)` 注册 `RpcAccessGuardExceptionMapper`；这样主 `EgonRpcAutoConfig` 不在无 Guard classpath 上解析 Guard 类型，现有 `ObjectProvider<RpcProviderExceptionMapper>` mapper chain 自动消费 Adapter。`x-egon-rpc-error-type` key 移到共享 `RpcMetadataKeys` 单一常量，Adapter 与 `RpcStatusExceptionMapper` 不得各自复制字符串。使用 `@RateLimitGuard` 的 Provider 应用必须显式依赖并启用 Guard；否则没有限流能力，README/startup configuration test必须明确该依赖前提。

##### Compatibility and verification

- `@EgonRpcProvider` 和 `@RateLimitGuard` 属性形状均不变；Token Bucket 现有规则/Local/Redisson key 继续兼容。
- 测试三算法的边界、并发、时钟前进、overflow、非法组合、Local/Redisson parity、Spring AOP Provider 方法命中、业务不执行、async parity，以及非 rate-limit Guard rejection 不被误映射。

#### 9.2.4 INTERNAL-004 — Typed blocking unary method

##### Necessity and interaction-cost decision

| Concern | Decision |
| --- | --- |
| Change classification | Existing/Keep |
| Independent consumer goal | 旧业务代码同步取得 Proto response |
| Parameter ownership and derivation | request/response type来自 generated descriptor；policy来自 Reference/Contract/Method/config |
| Direct/no-new-interface alternative | 原合同已满足，不新建 blocking facade |
| Caller use of result | 业务直接使用 response |
| Round trips and failure points | 一次逻辑调用可含受控 attempts；调用线程等待 terminal |
| Verdict | Keep，回归保护 `REQ-014` |

##### Identity and purpose

| Concern | Definition |
| --- | --- |
| Purpose/owner/consumer | Existing typed blocking unary contract used by business Consumer and implemented by Provider |
| Protocol and symbol | Generated gRPC unary `fullMethodName` bound by one `@EgonRpcMethod` Java method |
| Content/version | Protobuf request/response and Contract group/version；wire descriptor unchanged |
| Auth/tenant | Existing framework interceptors and verified metadata/token relay |
| Timeout/retry/rate limit | One total logical deadline；`DEC-009` availability retries；Provider Guard rate limit applies before business |
| Idempotency/concurrency | `idempotent` remains metadata only；business owns duplicate safety |

##### Request parameters

- Java signature declares exactly one generated `Message` request matching the Proto input descriptor。
- `null`、zero/multiple parameters、wrong Message descriptor and overloaded Java methods remain invalid contracts。

##### Success response

正常远端成功时，`ClientCalls.blockingUnaryCall` 返回一个非 null、与 Proto output 匹配的 generated Message；Provider synchronous method 必须返回同一类型。只有 availability paths exhausted 且 effective `FAIL_OPEN` 时，代理按 `DEC-014` 返回 `null`；调用点因此必须把返回值视为 nullable，不能用 Protobuf default instance 冒充远端成功。

##### Error responses

按照 §7.3.4 映射；总 Deadline 到期抛 `RPC_DEADLINE_EXCEEDED`；Caller interrupt/cancel 不产生额外 retry。非 UNAVAILABLE 业务/语义 status 原样报错，不执行 `FAIL_OPEN/LOCAL_FALLBACK`；rate-limit UNAVAILABLE 可换同模式节点。`idempotent` 不参与 gate。

##### Interface logic for frontend and consumers

1. CGLIB interceptor O(1) 取 plan；2. validate one Message；3. executor建立逻辑调用；4. 执行 attempts；5. success cast exact response；6. terminal map/fallback；7. 返回/抛出。

##### Compatibility and verification

Existing Echo Contract/Provider/Consumer fixtures must compile and pass without signature changes；JDK Proxy type assertion改为 CGLIB assertion。

#### 9.2.5 INTERNAL-005 — Typed async unary method

##### Necessity and interaction-cost decision

| Concern | Decision |
| --- | --- |
| Change classification | New supported Java method shape；no new wire method |
| Independent consumer goal | 不阻塞调用线程地组合/取消 RPC |
| Parameter ownership and derivation | generic return argument must be exact Proto output；raw CompletionStage禁止 |
| Direct/no-new-interface alternative | 在线程池包 blocking call会浪费线程并破坏 native cancellation/deadline |
| Caller use of result | compose/await/cancel future |
| Round trips and failure points | 与 blocking 相同；attempt callbacks serially advance |
| Verdict | Add（`DEC-011`） |

##### Identity and purpose

| Concern | Definition |
| --- | --- |
| Purpose/owner/consumer | Typed non-blocking unary contract for business Consumer/Provider |
| Protocol and symbol | Same generated unary `fullMethodName` as its Proto method；Java return shape is CompletionStage |
| Content/version | Generated Protobuf input and exact generic output；no wire version change |
| Auth/tenant | Same interceptor/security path as blocking |
| Timeout/retry/rate limit | Same total deadline、retry classifier and Provider rate limit |
| Idempotency/concurrency | Business owns duplicate safety；framework owns single terminal completion |

##### Request parameters

```java
@EgonRpcMethod(name = "Echo")
CompletionStage<EchoResponse> echo(EchoRequest request);
```

- parameter must be exact generated input Message；generic type must be concrete generated output Message。
- Request must be the exact generated input Message；raw/wildcard/nested CompletionStage declarations are invalid。

##### Success response

- Consumer returns a cancellable `CompletableFuture` view immediately；Provider may return any non-null CompletionStage。
- 远端正常成功时 Stage contains exact non-null generated response Message；Provider 返回 null stage/null success 是 INTERNAL contract violation。
- 只有 availability paths exhausted + effective `FAIL_OPEN` 时，Consumer 返回的 Stage 正常完成 `null`；不得与 Provider null contract violation 混淆。

##### Error responses

- Transport/status errors complete exceptionally with `EgonRpcException` as blocking parity。
- `cancel(true/false)` cancels active gRPC ClientCall, releases selection/Channel once and prohibits later retry。
- Provider CompletionStage error passes ordered exception mappers；if it also implements `Future`, server cancellation may call cancel；otherwise Provider observes gRPC Context cancellation。

##### Interface logic for frontend and consumers

1. Validator records ASYNC mode/output type；2. CGLIB returns stage without blocking；3. executor starts future unary；4. completion classifies outcome；5. availability failure chains next same-mode attempt；6. business error completes exceptionally without fail strategy；7. atomic terminal guard completes once，availability FAIL_OPEN completes null。

##### Compatibility and verification

No existing method changes. Reject raw/wildcard/nested CompletionStage, wrong output, null stage/response. Tests cover immediate return, success, exception, retry, switch, cancel-before/after-send and Provider async completion.

#### 9.2.6 INTERNAL-006 — Generic blocking invocation

##### Necessity and interaction-cost decision

| Concern | Decision |
| --- | --- |
| Change classification | New programmatic API |
| Independent consumer goal | 无业务 Java interface 的诊断、适配或动态集成调用 |
| Parameter ownership and derivation | Caller owns exact service/method and serialized Proto；framework owns discovery/policy/metadata |
| Direct/no-new-interface alternative | 动态生成 Java interface/Proxy不实际可行；新 generic wire service不必要 |
| Caller use of result | 自行按 Descriptor 解析 response bytes |
| Round trips and failure points | 与 typed相同；schema mismatch是显式风险 |
| Verdict | Add（`DEC-012`） |

##### Identity and purpose

| Concern | Definition |
| --- | --- |
| Purpose/owner/consumer | Blocking generic unary for infrastructure/operator adapters without a Java Contract interface |
| Protocol and symbol | Existing gRPC unary canonical `fullMethodName` (`fully.qualified.Service/Method`); `RpcGenericInvoker#invokeBlocking` |
| Content/version | Raw Protobuf request/response bytes；service group/version explicit |
| Auth/tenant | Framework-owned identity/security metadata only；caller cannot supply arbitrary headers |
| Timeout/retry/rate limit | Same executor policy and Provider admission as typed calls |
| Idempotency/concurrency | Caller/business owns duplicate safety；method blocks until terminal |

##### Request parameters

确定签名：

```java
byte[] invokeBlocking(RpcGenericInvocation invocation);
```

`RpcGenericInvocation` 必须包含：固定 `mode`；Direct 时 required bizCode/appCode/env；serviceName/group/version；canonical `fullMethodName`，格式必须恰有服务段与方法段并以 `/` 分隔，服务段必须等于 descriptor/gRPC `serviceName`；非空 requestPayload，最大值受现有 gRPC inbound 配置；call timeout/retries/loadBalance/failStrategy；CONSISTENT_HASH 时非空 affinityKey。不得接受 `fullservice.method` 点号 alias，不得允许任意 Metadata、Authorization、Host/port、另一模式 fallback 或 DDC credential 输入。

##### Success response

正常远端成功返回 Provider 原始 unary Protobuf response bytes 的防御性副本；空 Proto message 可为 zero-length bytes，且与 `null` 严格不同。只有 availability exhaustion + `FAIL_OPEN` 返回 `null`。框架不猜测 JSON/Map 类型。

##### Error responses

invalid identity/点号或不一致 fullMethodName/payload size→`RPC_INVALID_REQUEST/CONTRACT`；service/method missing、permission、business status、rate-limit UNAVAILABLE、deadline、cancel、unavailable 与 typed parity；response size由 gRPC配置保护。

##### Interface logic for frontend and consumers

1. validate/normalize target；2. build raw byte MethodDescriptor；3. build fixed-mode generic invocation plan；4. attach only framework-approved metadata/interceptors；5. execute same-mode LB/retry/deadline；6. return defensive bytes or availability FAIL_OPEN null；7. caller parses non-null bytes using its known descriptor。

##### Compatibility and verification

No Provider/Gateway/Proto changes. Tests compare typed and raw payload bytes for same Echo method, invalid service/method, malformed request, auth interceptor presence and no arbitrary metadata injection.

#### 9.2.7 INTERNAL-007 — Generic async invocation

##### Necessity and interaction-cost decision

| Concern | Decision |
| --- | --- |
| Change classification | New programmatic async API |
| Independent consumer goal | Generic caller composes/cancels an invocation without blocking a thread |
| Parameter ownership and derivation | Same exact target/method/payload ownership as INTERNAL-006 |
| Direct/no-new-interface alternative | Wrapping blocking generic invocation in an executor wastes threads and weakens cancellation |
| Caller use of result | parses response bytes after CompletionStage success |
| Round trips and failure points | same route/attempts as INTERNAL-006，plus async cancellation/completion races |
| Verdict | Add for `REQ-015`,`REQ-016` |

##### Identity and purpose

| Concern | Definition |
| --- | --- |
| Purpose/owner/consumer | Non-blocking generic unary for infrastructure/operator adapters |
| Protocol and symbol | Existing gRPC unary canonical `fully.qualified.Service/Method`; `RpcGenericInvoker#invokeAsync` |
| Content/version | Raw Protobuf bytes；no new wire service/version |
| Auth/tenant | Same framework-owned metadata/security as typed and blocking generic |
| Timeout/retry/rate limit | Same executor policy and Provider admission |
| Idempotency/concurrency | caller/business duplicate safety；single terminal future |

##### Request parameters

```java
CompletionStage<byte[]> invokeAsync(RpcGenericInvocation invocation);
```

Input rules exactly INTERNAL-006；method never blocks for discovery/network response beyond bounded plan validation/local cache access。

##### Success response

正常远端成功 completes with defensive non-null bytes；availability exhaustion + `FAIL_OPEN` completes normally with `null`；method returns before any network response。

##### Error responses

Errors/cancellation and attempts exactly INTERNAL-005/INTERNAL-006 parity；future cancellation cancels active raw gRPC call。

##### Interface logic for frontend and consumers

Validate→create fixed-mode raw plan→start future unary→classify callbacks→same-mode retry/reselection→business error exceptional completion or availability fail strategy→single terminal completion→release selection/channel。

##### Compatibility and verification

Additive API. Tests cover raw bytes parity, immediate return, cancellation, same-mode retry/reselection and terminal exception type.

#### 9.2.8 INTERNAL-008 — `RpcLoadBalanceKeyResolver#resolve`

##### Necessity and interaction-cost decision

| Concern | Decision |
| --- | --- |
| Change classification | New SPI |
| Independent consumer goal | 为 CONSISTENT_HASH 提供业务稳定 affinity key |
| Parameter ownership and derivation | Business application owns which request field is stable；framework supplies sanitized context |
| Direct/no-new-interface alternative | whole request hash会被时间戳/无关字段扰动；invocationId每次不同 |
| Caller use of result | 仅用于本地 endpoint selection，不上线/转发/持久化 |
| Round trips and failure points | no network；one resolver call per logical invocation, not per retry |
| Verdict | Add（`DEC-013`） |

##### Identity and purpose

| Concern | Definition |
| --- | --- |
| Purpose/owner/consumer | Resolve one business-stable key for a typed CONSISTENT_HASH invocation |
| Protocol and symbol | Internal Java SPI `RpcLoadBalanceKeyResolver#resolve`；no network operation |
| Content/version | read-only service/method/request context；resolver implementation owned by application |
| Auth/tenant | credentials are not exposed；tenant may only be used if already present in safe request/business context |
| Timeout/retry/rate limit | evaluated once before endpoint selection，not per retry |
| Idempotency/concurrency | resolver must be stateless/thread-safe and deterministic for the same business affinity |

##### Request parameters

```java
String resolve(RpcLoadBalanceContext context);
```

Context exposes service identity、fullMethodName、read-only request Message；不暴露 Authorization/DDC secret。

##### Success response

Result trim 后必须 1–512 UTF-8 bytes，不得为空；framework hashes bytes with SHA-256 before ring lookup and never logs raw key。

##### Error responses

Resolver missing/wrong type is startup `RPC_INVALID_CONTRACT`；throw/null/blank/oversized result is invocation `RPC_INVALID_REQUEST` and does not retry/fallback。

##### Interface logic for frontend and consumers

1. Proxy plan resolves the named Bean once；2. each logical invocation calls it once；3. validate and hash result；4. pass only digest to consistent ring；5. retries reuse the digest；6. raw key is discarded；7. no frontend behavior applies。

##### Compatibility and verification

Named Bean selection is additive and only required for CONSISTENT_HASH. Unit tests verify determinism, thread safety, redaction, length and single evaluation across retries。

## 10. POJO and Data Model Design

### 10.1 POJO role classification and class necessity

| Object/path | Selected role | Owner/boundary and consumers | Why a distinct class is necessary or reuse is safe | Mapping owner | Requirements |
| --- | --- | --- | --- | --- | --- |
| `RpcReferencePolicy` | internal immutable policy object | resolver→proxy/executor | 两注解公共语义，避免传 annotation instance 到热路径 | `RpcReferenceDefinitionResolver` | `REQ-001` |
| `RpcReferenceDefinition` | internal immutable definition | resolver→Reference Strategy | mode/Direct target 与 common policy 有不同生命周期，组合优于注解继承 | resolver | `REQ-001`–`REQ-004` |
| `RpcInvocationPlan` | internal compiled plan | CGLIB/generic→executor | method、marshaller、policy、mode预计算，避免每次反射解析 | proxy/generic factory | `REQ-014`–`REQ-018`,`REQ-020` |
| `RpcInvocationContext` | per-call mutable orchestration context | executor only | deadline/attempt/excluded/terminal guard是逻辑调用状态，不能放共享 plan | executor | `REQ-003`,`REQ-015`,`REQ-019`,`REQ-020` |
| `RpcLoadBalanceContext` | read-only SPI context | executor→LB/key resolver | 隐藏 credential/channel internals，typed/generic共用 | executor | `REQ-008`,`REQ-009` |
| `RpcChannelKey` | technical key object | Channel pool | endpoint transport identity需要稳定 equals/hash，不能直接用易变 lease object | manager/pool | `REQ-017` |
| `RpcChannelLease` | lifecycle handle | manager/executor | 把共享 ownership/release 从 raw ManagedChannel 分离，避免误关 | pool | `REQ-011`,`REQ-017` |
| `RpcGenericInvocation` | public command/parameter object | generic caller→invoker | 无 Interface 时必须承载 exact target/method/payload/options；不是持久对象 | caller/invoker validation | `REQ-016`,`REQ-020` |
| `RpcGenericTargetCache` | bounded technical cache | GenericInvoker lifecycle | dynamic targets otherwise retain Manager subscriptions/LB state forever；entry owns closeable reference demand | invoker | `REQ-005`,`REQ-016`,`REQ-017` |
| Existing `AdmissionConfig.RateLimitConfig` | Guard immutable compiled policy | Guard plan resolver→rate-limit policy/backend | 保持 rule配置 authority；algorithm enum扩展而非 RPC 复制 policy | Guard property/plan mapper | `REQ-012`,`REQ-013` |
| Existing `RateLimitRequest/RateLimitDecision` | Guard internal request/result | policy→backend→Guard outcome | 扩展 algorithm-specific normalized input；allowed/remaining/retryAfter仍为统一输出 | Guard policy/backend | `REQ-012`,`REQ-013` |
| `RpcProviderRuntimeState` | lifecycle enum | Provider lifecycle/health/logs | boolean无法表达 STARTING/READY/DEGRADED/DRAINING/FAILED | lifecycle | `REQ-010`,`REQ-011` |
| `RpcConsumerRuntimeState` | lifecycle enum | Consumer coordinator/executor | Manager booleans无法表达统一 STARTING/READY/DEGRADED/DRAINING/FAILED gate | coordinator | `REQ-011`,`REQ-023` |
| Existing `RpcProviderSnapshot` | reuse/expand only through endpoint | Directory→Manager | revision/observedAt/list语义已匹配，不新建 cache DTO | DDC adapter | `REQ-005`,`REQ-006` |
| Existing `RpcProviderEndpoint` | neutral endpoint record, add weight | Directory→LB/pool | exact lease/transport already owned；weight是选择必需同生命周期字段 | DDC adapter | `REQ-008`,`REQ-009` |
| Existing `RpcGatewayEndpoint` | neutral endpoint record, add weight | Gateway Directory→LB/pool | Gateway Reference 也公开 loadBalance；必须与 Provider 使用同一 weight 语义 | DDC adapter | `REQ-008`,`REQ-009` |

这些对象均非 PO/ORM Entity/DTO/VO。RPC 模块没有数据库对象；`Command` 只用于说明 `RpcGenericInvocation` 是一次调用意图，不引入传统三层/DDD 类型体系。

### 10.2 Persistence objects, ORM entities, and business data objects

N/A — §3.3 已证明本范围无关系持久化。DDC Redis lease 是 Context-only 的外部临时事实；Consumer cache/LB/Channel 是进程内对象。Access Guard 可按现有配置把 rate-limit state 放在 Local 内存或 Redisson key-value backend，但本规格不改变关系模型、不创建 PO/Entity/表，也不新增 Redis key namespace。

### 10.3 Field design

| Model.field | Type | Required/null/default | Validation and semantics | Source/mapping | Requirements |
| --- | --- | --- | --- | --- | --- |
| `RpcReferencePolicy.timeoutMs` | long | `>0` resolved | total logical deadline ceiling | annotations/service/config | `REQ-001`,`REQ-020` |
| `.retries` | int | `0..maxRetries` | initial attempt 后最多追加的同模式 transmission 次数；实际还受 distinct same-mode candidates 与 Deadline 限制 | annotations/service/config | `REQ-019`,`REQ-020` |
| `.loadBalance` | enum | non-INHERIT after resolve | selection algorithm | reference/service/config | `REQ-008` |
| `.fallbackBean/.failStrategy` | String/enum | normalized | 只在 availability paths exhausted 后执行；业务错误跳过；FAIL_OPEN result=null | Reference/config | `REQ-001`,`REQ-020` |
| `.loadBalanceKeyResolver` | String | required only for consistent hash | named Spring bean | Reference | `REQ-009` |
| `RpcReferenceDefinition.mode` | enum | required | GATEWAY or DIRECT | annotation type | `REQ-002`,`REQ-003` |
| `.bizCode/.appCode/.env` | String | Direct required；Gateway absent | exact target Provider scope | Direct annotation/process | `REQ-003`–`REQ-006` |
| `RpcInvocationPlan.fullMethodName` | String | required | canonical `fully.qualified.Service/Method`；exactly one `/` boundary；must match descriptor/target；dot alias rejected | Contract/generic input | `REQ-014`–`REQ-016` |
| `.requestMarshaller/.responseMarshaller` | gRPC marshaller | required | typed Message or raw byte[] pair | descriptor/factory | `REQ-014`–`REQ-016` |
| `.mode` | enum | BLOCKING/ASYNC | derived from Java return or generic API | Validator/invoker | `REQ-014`,`REQ-015` |
| `RpcInvocationContext.deadlineNanos` | long | required monotonic | one absolute deadline for all attempts | executor ticker | `REQ-020` |
| `.attemptCount` | int | starts 0 | every wire transmission increments | executor | `REQ-019`,`REQ-020` |
| `.referenceMode` | enum | immutable GATEWAY/DIRECT | created by Strategy Factory；cannot mutate during logical invocation | Reference/generic input | `REQ-003`,`REQ-004` |
| `.excludedEndpointIds` | Set<String> | empty | no same-mode endpoint twice within one logical invocation | executor | `REQ-004`,`REQ-008`,`REQ-020` |
| `RpcEndpoint.weight` | int | default method returns 100 | custom Registry endpoint不改源码也有兼容默认 | neutral SPI | `REQ-008`,`REQ-009` |
| `RpcProviderEndpoint.weight` / `RpcGatewayEndpoint.weight` | int | default 100 | 1–10000; remote invalid→codec default | DDC metadata `gateway.weight` | `REQ-008`,`REQ-009` |
| `RpcChannelKey.host/port/secure` | String/int/bool | required | routable host, port 1–65535；global business TLS profile implicit | endpoint | `REQ-017` |
| `RpcGenericInvocation.requestPayload` | byte[] | non-null defensive copy | max outbound/inbound limit；zero length allowed for Empty | caller | `REQ-016` |
| `.affinityKey` | String | consistent hash required | 1–512 UTF-8 bytes；raw not logged | caller | `REQ-009`,`REQ-016` |
| `AdmissionConfig.RateLimitConfig.algorithm` | Guard enum | default `TOKEN_BUCKET` | `TOKEN_BUCKET/LEAKY_BUCKET/SLIDING_WINDOW`；unknown fail-fast | Guard rule config | `REQ-012` |
| `.capacity` | long | `>0` | token/water capacity；Sliding max accepted calls | Guard rule config | `REQ-012` |
| `.refillTokens/.refillPeriod` | long/Duration | positive | token refill or leak rate；Sliding uses period as window and treats refillTokens N/A | Guard rule config | `REQ-012` |
| `.requestedTokens` | long | `1..capacity`；Sliding exactly 1 | per-request token/water cost；Sliding counts calls | Guard rule config | `REQ-012` |
| `RateLimitRequest.algorithm` | Guard enum | required | backend Strategy Factory dispatch key | Guard policy normalization | `REQ-012` |
| `RateLimitDecision.retryAfter` | Duration | nonnegative | Guard outcome/event operational hint；not a new RPC trailer | Guard algorithm | `REQ-013` |

Configuration field design:

| Property under `egon.cola.component.rpc` | Default | Validation | Runtime meaning | Compatibility |
| --- | --- | --- | --- | --- |
| `consumer.max-retries` | `3` | `0..10` | hard cap for annotation/service/method retries | new safety ceiling；values above fail startup |
| `consumer.default-load-balance` | `ROUND_ROBIN` | non-null/non-INHERIT | lowest-priority algorithm for both endpoint modes | preserves current Manager selection |
| `consumer.consistent-hash-virtual-nodes` | `160` | `16..4096` | ring vnode count per endpoint | only used by CONSISTENT_HASH |
| existing `consumer.gateway-max-attempts` | `2` | `1..10` | when retries are fully INHERIT, Gateway mode resolves retries to `gateway-max-attempts - 1`；also caps distinct Gateway endpoint transmissions | preserves current two-Gateway attempt count while removing only the idempotent gate |
| existing `consumer.channel-drain-timeout-ms` | `5000` | `>0` | pool/manager drain before force close | existing property reused for graceful Consumer shutdown |
| `consumer.generic-cache-max-entries` | `256` | `1..4096` | LRU cap of normalized generic target+method plans and closeable discovery demands | prevents dynamic target memory/subscription growth |
| `consumer.generic-cache-idle-timeout-ms` | `600000` | `1000..86400000` | idle eviction closes demand and releases pool refs | no effect when generic API unused |
| existing `provider.heartbeat-interval-seconds` | `10` | `>0` and `< lease-seconds` | Provider-owned fixed-delay heartbeat | unchanged, explicitly authoritative for `REQ-007` |
| existing `provider.lease-seconds` | `30` | `> heartbeat interval` | DDC passive lease TTL request | unchanged |
| existing `provider.graceful-shutdown-timeout-ms` | `10000` | `>=0` | wait for accepted Server unary calls before force close | unchanged, extended to callback state semantics |

RPC Consumer retry accounting is single-layered：`RpcInvocationExecutor` owns at most `1 + resolvedRetries` transmissions to its configured-mode endpoints，且实际 transmissions=`min(1+resolvedRetries, distinct eligible same-mode endpoints)`；`gateway-max-attempts` 是 Gateway endpoint transmission 的兼容上限，不形成 Consumer 内嵌循环。When method/reference/service retries are all INHERIT，Gateway mode resolves to `gateway-max-attempts - 1`，Direct mode resolves to `0`。每次失败只在 acquisition/UNAVAILABLE 时把 endpoint 加入 excluded set 并重选；mode 永远不变。Gateway data plane 对下游 Provider 的现有 Route retry 是 Context-only 的独立层，仍可能让一次 Gateway endpoint transmission 产生多个 Provider attempts；RPC Consumer metrics必须分开报告 endpoint attempts，不能谎称全链业务执行总次数。

### 10.4 Object flow and mapping relationships

```text
Annotation Field + Contract Descriptor + RPC Properties
  -> RpcReferenceDefinitionResolver
  -> RpcReferenceDefinition { mode-specific target + RpcReferencePolicy }
  -> RpcReferenceStrategyFactory -> exactly one mode Strategy
  -> CGLIB ProxyFactory
  -> Map<Java Method, RpcInvocationPlan>
  -> per call RpcInvocationContext
  -> RpcLoadBalanceContext
  -> RpcProviderEndpoint / RpcGatewayEndpoint
  -> RpcChannelLease
  -> gRPC unary request/response

Generic caller
  -> RpcGenericInvocation
  -> raw RpcInvocationPlan
  -> same RpcInvocationContext/LB/Channel/executor
```

No mapper chain is added. DDC adapter remains the single owner of `DdcServiceInstance.metadata -> ServiceInstanceMeta.weight -> RpcProviderEndpoint/RpcGatewayEndpoint.weight` conversion。

Provider registration uses `@EgonRpcService.weight` as the default `gateway.weight` only when `egon.cola.component.rpc.provider.metadata.gateway.weight` is absent；an explicit deployment metadata value therefore overrides the Contract default for that instance。Both values are validated 1–10000 before registration。`DdcRpcProviderDirectory` and `DdcRpcGatewayDirectory` decode the same canonical metadata into their endpoint weight，so Gateway and Direct Reference weighted algorithms have identical semantics without a new wire field。

### 10.5 Reuse, inheritance, and composition decisions

- Annotations cannot inherit common attributes. The design reuses an internal immutable policy rather than introducing annotation inheritance or a nested options annotation that would coexist ambiguously with legacy flat fields.
- CGLIB generated proxy implements the Contract interface; it does not subclass business services and does not change Provider inheritance.
- `RpcReferenceStrategy`、`RpcLoadBalancer`、Guard `RateLimitAlgorithmStrategy` 是当前真实 variation points；各由显式 Factory 按枚举/模式选择，均使用组合而非 `BaseStrategy`/`BaseService` inheritance。
- Consumer concrete LB algorithms remain private/package-private in `RpcLoadBalancers`/Factory following existing Gateway precedent；Guard concrete algorithms remain internal to Guard rate-limit packages/backends，RPC 不引用其实现类型。

### 10.6 State transitions and lifecycle

Provider runtime:

```text
NEW -> STARTING -> READY
                 -> DEGRADED (registration fail-fast false / lease lost)
READY/DEGRADED -> DRAINING -> STOPPED
STARTING -> FAILED -> STOPPED
```

- READY requires Server bound and every required service lease available.
- DEGRADED means Server may exist but unavailable services reject；recovery heartbeat may return READY。
- DRAINING is terminal for that lifecycle instance；no recovery/register/new business admission。
- Repeated start when READY and repeated stop when STOPPED are idempotent；start after STOPPED follows current SmartLifecycle restart support only if all executors/subscriptions can be recreated, otherwise explicitly reject and test。

Consumer pool entry:

```text
CONNECTING -> ACTIVE -> DRAINING -> CLOSED
     |           |
     +-> CLOSED  +-> CLOSED after refs=0 and inFlight=0 or timeout
```

Consumer runtime:

```text
NEW -> STARTING -> READY
                 -> DEGRADED (a Direct demand has no endpoint but its subscription is installed)
READY/DEGRADED -> DRAINING -> STOPPED
STARTING -> FAILED -> STOPPED
```

- STARTING installs only each Reference's configured-mode primary demand and creates the shared Channel runtime；the executor rejects application calls until the coordinator publishes READY or DEGRADED。
- Existing Gateway primary demand retains fail-fast discovery timeout。Direct empty snapshots do not claim READY；they enter DEGRADED and invocation returns Provider unavailable until the Provider snapshot recovers；no fallback-only demand exists。
- DRAINING atomically closes the new-invocation gate，then closes generic cache/demands/subscriptions，then drains Channel pool；new calls fail fast without DDC/Channel work。

### 10.7 Relational model consistency

N/A — no relational persistence/model is affected; no ER mapping exists for runtime records.

## 11. Database Design

Scope disposition: relational database/migrations are `Not applicable`；DDC Redis registry remains `Context-only`；Access Guard Redisson rate-limit transient keys are `Affected` because the two added algorithms need storage-specific state and atomic scripts。

### 11.1 Table Inventory

Relational table inventory: `N/A` — no table/schema/column/index/relationship is created or altered。The affected persistence surface is transient Redis key-value state only，listed below without treating keys as relational tables。

#### 11.1.1 Key-value state inventory

Define existing base `B = <keyPrefix>:<application>:<ruleId>:rate-limit:<stateVersion>:<lowercaseSha256KeyHash>`，由 `AccessGuardRedisKeyFactory` 验证每个 segment；raw business key never enters Redis key/log。

- `B` — Existing Token Bucket HASH，owned by Access Guard Redisson backend；retain exact key/type/fields and existing `HMGET/HSET/PEXPIRE` Lua so state is reused。
- `B:leaky-bucket` — New lazy Leaky Bucket HASH，owned by Access Guard Redisson backend；no bulk migration，idle TTL cleanup。
- `B:sliding-window` — New lazy Sliding Window LIST of encoded accepted timestamps，owned by Access Guard Redisson backend；no bulk migration，idle TTL cleanup。

### 11.2 Per-table Detailed Design

Relational per-table design: `N/A` for the evidence above。The following per-key details are the applicable key-value equivalent and are not tables/rows/columns。

**Redis Token Bucket key `B`**

1. **Purpose/ownership/lifecycle** — authoritative distributed admission state for one Guard `ruleId + stateVersion + keyHash` while `storage=REDISSON` and algorithm is TOKEN_BUCKET. Access Guard alone writes/reads；created on first request，refreshed on each acquire，deleted by `local.idle-ttl` equivalent Redis PEXPIRE。Tenant/caller separation depends on Guard key contributors；keyHash is sensitive pseudonymous data and must remain redacted。
2. **Complete value design** — Redis HASH fields remain `tokens` (long, `0..capacity`), `lastRefill` (Redis epoch millis), `capacity` (positive long), `refillTokens` (positive long), `refillPeriod` (positive millis). Missing state or config mismatch initializes a full bucket. No field may contain raw key/request/identity。
3. **Keys/constraints** — exact existing key stays unsuffixed for backward compatibility；`stateVersion` is application/plan-owned isolation；key factory rejects unsafe segments/non-SHA-256 hashes。
4. **Indexes** — N/A，single exact Redis key lookup；no scan/query index。
5. **Access/atomicity** — one READ_WRITE Lua execution obtains Redis TIME，refills by elapsed full periods，subtracts `requestedTokens` when possible，writes all fields and TTL，returns `{allowed,remaining,retryAfterMillis}` atomically。
6. **Migration/history** — no migration file or backfill；existing Token Bucket rule sees the same key and compatible HASH fields after upgrade。Rollback to the old Guard version remains safe while rules use TOKEN_BUCKET。
7. **Consistency/recovery** — Redis script is the linearization point across Provider JVMs sharing application/rule/key。Redis failure is wrapped as `StoreOperationException` and resolved by existing Guard failure policy；TTL loss resets quota to full on next request，which is an availability trade-off already present。

**Redis Leaky Bucket key `B:leaky-bucket`**

1. **Purpose/ownership/lifecycle** — authoritative distributed water level for the same Guard identity under LEAKY_BUCKET；Access Guard owns it，lazy creation/refresh/TTL matches Token Bucket。
2. **Complete value design** — Redis HASH fields: `level` (long, `0..capacity`), `lastLeak` (Redis epoch millis), `capacity`, `leakTokens` (from `refillTokens`), `leakPeriod` (from `refillPeriod` millis). Missing/config-mismatch state initializes level 0；no raw key/payload。
3. **Keys/constraints** — algorithm suffix prevents type/config collision with Token Bucket even when stateVersion is unchanged；safe static suffix is emitted only by key factory，never caller input。
4. **Indexes** — N/A，one exact key。
5. **Access/atomicity** — one READ_WRITE Lua obtains Redis TIME，subtracts `floor(elapsed/leakPeriod)*leakTokens` to minimum 0，then admits only when `level+requestedTokens<=capacity`；rejection computes periods until enough capacity；HSET/PEXPIRE/return are atomic。
6. **Migration/history** — no history conversion；key appears only after rule explicitly selects LEAKY_BUCKET。Changing away leaves the key to TTL；rollback requires rule change to TOKEN_BUCKET before old artifact deploy。
7. **Consistency/recovery** — same Redis linearization/failure-policy rules as §11.2.1；no queue or delayed request exists，only immediate admission/rejection。

**Redis Sliding Window key `B:sliding-window`**

1. **Purpose/ownership/lifecycle** — authoritative exact accepted-call timestamps for one Guard identity under SLIDING_WINDOW；Access Guard owns it；list exists only while active and expires by idle TTL。
2. **Complete value design** — every accepted call appends one ASCII LIST element `v1|<capacity>|<windowMillis>|<redisEpochMillis>` in nondecreasing timestamp order；`0<=LLEN<=capacity<=100000`。Duplicate millisecond timestamps are valid distinct calls because LIST entries need not be unique。No raw business key/payload is stored。
3. **Keys/constraints** — suffix isolates LIST type from HASH algorithms。If the oldest existing element's version/capacity/window differs from the current request，the script atomically deletes the list before evaluation，so a same-stateVersion rule edit cannot reuse incompatible timestamps。
4. **Indexes** — N/A；oldest item is list index 1，no range scan/index structure。
5. **Access/atomicity** — one READ_WRITE Lua obtains Redis TIME；parse/reset from `LINDEX 0`；while oldest timestamp `<= now-windowMillis`, `LPOP`；when `LLEN < capacity` RPUSH the encoded current entry，else reject and derive retryAfter from oldest；PEXPIRE and return atomically。Implementation must test empty/non-empty/config-change boundaries and avoid O(capacity) full LRANGE on every call。
6. **Migration/history** — lazy new key，no backfill/Flyway；changing algorithm/config atomically resets or moves to another suffix，old list expires。Rollback requires disabling/changing Sliding rule first；no destructive Redis delete is required。
7. **Consistency/recovery** — the script is the distributed ordering point；same-millisecond concurrency is counted exactly；Redis failure follows Guard policy。A lost/expired key resets the window，so production sizing must keep idle TTL greater than the window and verify eviction pressure。

### 11.3 Entity-relationship diagram

Relational model change: `No — no relational table, key, constraint, relationship, DAO, ORM Entity or Flyway path participates`；therefore no Mermaid `erDiagram` is applicable。DDC Registry keys/topics/lease semantics remain exactly unchanged。Access Guard Redis state is transient derived admission state，not business source of truth；source inspection does not prove production Redis mode、memory、eviction、script latency or cluster-slot behavior。Because every algorithm script uses exactly one key，Redis Cluster cross-slot behavior is avoided。

Verification includes Local deterministic tests，mocked Lua contract tests，and existing optional Testcontainers Redisson integration for all three algorithms、TTL、config reset and concurrent acquisition。No live Redis execution is claimed by this Spec task。

## 12. Frontend Page Design

N/A — repository evidence in §8 shows RPC Component consists of Java/Maven modules and bilingual Markdown README only；there is no affected frontend route, page, component, client state or browser flow。

## 13. Design Patterns and Architecture Principles

### 13.1 Selected patterns

| Pattern/principle | Concrete variation point or problem | Placement | Why direct code is insufficient | Repository alignment |
| --- | --- | --- | --- | --- |
| Strategy | Gateway vs Direct mode-specific demand/query/candidate behavior | `consumer.reference.RpcReferenceStrategy` | if/else across injection/executor would scatter mode rules and risk accidental cross-mode calls | existing `RpcInvocationChannelProvider` already expresses mode variation |
| Factory Method / Simple Factory | Select exactly one Reference Strategy and one LB implementation from validated enums | `RpcReferenceStrategyFactory`、`RpcLoadBalancerFactory/RpcLoadBalancers` | proxy/executor direct constructors would duplicate switches and allow mid-call replacement | existing Gateway `ProviderLoadBalancers` is factory-style registry；Factory output is immutable per plan |
| Strategy | 5 required LB algorithms + compatibility algorithm | `consumer.loadbalance.RpcLoadBalancer` | Manager hardcoded RR cannot vary/test independently | Gateway uses `ProviderLoadBalancer/ProviderLoadBalancers` |
| Strategy + Factory | 3 Provider rate-limit algorithms with Local/Redisson execution | Guard `policy.ratelimit/RateLimitAlgorithmStrategyFactory` and backends | token/leak/window state、retryAfter、Redis atomic script materially differ；one switch-heavy backend would be hard to verify | extends Access Guard's existing policy/backend separation；RPC reuses it |
| Proxy | typed Contract invocation through CGLIB | `consumer.proxy` | user explicitly requires CGLIB and plan interception | Spring runtime already supplies CGLIB |
| Observer | Redis event hint updates subscription cache | existing DDC subscription | polling-only increases staleness；event-only loses recovery | current `DdcManagedRegistrySubscription` kept |
| Adapter | DDC snapshot/metadata to neutral RPC endpoint | `rpc-ddc-adapter` | Starter must remain registry-neutral | current DDC directories |
| Adapter | Guard `RATE_LIMITED` outcome to RPC status/trailers | `RpcAccessGuardExceptionMapper` implementing existing `RpcProviderExceptionMapper` | Guard must not depend on gRPC and RPC must not reimplement Guard | existing ordered Provider exception-mapper SPI is the exact extension point |
| State model | Provider and Channel lifecycle states | lifecycle/pool | booleans cannot prove READY/DRAINING/terminal transitions | current Gateway/DDC runtimes use explicit states |

### 13.2 Rejected patterns and simpler alternative

- Reject Abstract Factory hierarchy for mode×call type×algorithm products. Two small enum-keyed factories are sufficient；they do not create families of coupled products。
- Reject Template Method/inheritance for reference factories or rate limiters. Composition keeps state ownership explicit and avoids deep call stacks.
- Reject Chain of Responsibility for retry/reselection. The availability whitelist、candidate exclusion and budget order are fixed and correctness-sensitive；an explicit loop/async state machine is clearer。
- Reject Command bus/Event Sourcing. Invocation state is transient and requires no persistence/replay log.
- Reject a second RPC-side DDC cache or polling job. Existing Observer + reconciliation already satisfies the requirement.
- Reject Map/Object[] generic serialization, dot-form method aliases and a new Generic Proto service under `DEC-012`; canonical raw Protobuf reuse has fewer contracts/failure points.
- Reject an RPC-owned local/distributed rate-limit backend. Guard already owns Local/Redisson availability、key、failure-policy semantics；RPC adds only the Adapter。

### 13.3 Architecture principles

- Dependency direction remains `rpc-starter` neutral SPI/runtime ← `rpc-ddc-adapter` implementation；Gateway/DDC production modules do not become RPC Starter dependencies。
- Information hiding: business caller sees annotations/typed proxy/generic API, not Redisson、DDC keys、ManagedChannel pool entries or limiter buckets。
- Single responsibility: Directory supplies snapshots；Manager owns current candidate view；LB selects；Pool owns channels；Executor owns same-mode logical attempt state；Guard owns admission；RPC Adapter owns only status mapping。
- Open/Closed is applied only at real variation points (2 modes, 5 LB algorithms, 3 limiter algorithms). No speculative interface for nonexistent streaming/cluster limiter。
- CGLIB Proxy, Provider implementation and business services use composition; no service/base-class inheritance。
- Class count is controlled with records and nested algorithm implementations; no parallel DTO/BO/VO/Entity variants。
- Security remains fail closed and existing metadata/auth pipeline is reused；generic API cannot inject arbitrary headers。

## 14. Test Design

### 14.1 Unit tests

- Reference resolver/Factory: common field parity、override/default、invalid combination、immutable mode、named hash resolver、fallback bean compatibility、no cross-mode dependency。
- Contract validator/plan: blocking/async exact Proto types、raw/wildcard CompletionStage rejection、O(1) lookup、Method plan immutable。
- CGLIB proxy: type/classloader、Object methods、one interceptor invocation、blocking/async return。
- Invocation executor: total Deadline、attempt budget、non-idempotent retry、availability whitelist、same-mode endpoint exclusion、both-direction no-cross-mode、business error bypass fail strategy、nullable FAIL_OPEN、single completion/cancel races。
- LB: deterministic RR/SWRR、seeded statistical RANDOM/WEIGHTED_RANDOM、weight defaults/bounds、consistent ring/key/membership minimal remap、excluded candidates、state cleanup。
- Channel pool: one creation per key、ref count、in-flight、drain/force close、concurrent acquire/release。
- Access Guard limiters: algorithm Factory coverage、Local monotonic time、Redisson TIME/Lua atomicity、burst/window boundaries、high concurrency、overflow、cleanup、rule/key/storage scope；RPC Adapter status/trailer mapping另测。
- Provider lifecycle: state transitions、fail-fast/degraded recovery、callback stop、heartbeat ownership、drain timeout。

### 14.2 Integration, contract, persistence, component, and end-to-end tests

- Starter component test uses real in-process Netty Server/Channel for blocking and async typed calls, raw generic parity and concurrent streams on one ManagedChannel。
- DDC Adapter contract test uses fake `DdcServiceRegistryClient`/Redisson coordinator fixtures to prove event + periodic full snapshot and weight mapping；does not start live Redis。
- Existing RPC test-contract reuses Echo Proto；add `CompletionStage<Response>` Java interface without adding a Proto method；generic uses the same canonical Echo fullMethodName。
- RPC+Guard component test loads both auto-configurations and a real Spring-proxied `@EgonRpcProvider` implementation method with `@RateLimitGuard`，proving target method is not invoked on rejection and adapter emits UNAVAILABLE。
- Process IT extends current DDC Admin/Mock Gateway/Provider/Consumer harness for two Providers/two Gateways、lease removal、mode isolation counters、same-mode reselection、graceful drain；this is later implementation validation and no service is started during Spec writing。
- No persistence/migration test is added because §11 is N/A。

### 14.3 Test cases and data

| ID | Level | Target | Scenario/input | Expected assertion | Test double/data | Tool/path | Requirements |
| --- | --- | --- | --- | --- | --- | --- | --- |
| `TEST-001` | Unit | Reference resolver | existing Gateway annotation defaults | mode Gateway；old fields preserved | reflection fixture | starter annotation/reference test | `REQ-001`,`REQ-002` |
| `TEST-002` | Unit | Reference resolver | Direct annotation common fields | same policy semantics + exact target | field fixture | starter test | `REQ-001`,`REQ-003` |
| `TEST-003` | Unit | Resolver | both annotations same field | startup error with bean/field | current fixture | BPP test | `REQ-001` |
| `TEST-004` | Unit | Direct Strategy | Direct success | Gateway never asked | fake managers | reference test | `REQ-003` |
| `TEST-005` | Unit | Direct Strategy | first Provider UNAVAILABLE；second healthy | second selected by effective LB；Gateway manager calls 0 | fake endpoints/status | reference/executor test | `REQ-003`,`REQ-004` |
| `TEST-006` | Unit | Direct Strategy | all Providers UNAVAILABLE/exhausted | `RPC_PROVIDER_UNAVAILABLE`；Gateway manager calls 0 | fake managers | executor test | `REQ-003`,`REQ-004` |
| `TEST-007` | Unit | Reference Strategy Factory | Direct/Gateway definitions | exactly one matching Strategy；no runtime switch method/other Directory dependency | fake factories/managers | reference test | `REQ-003`,`REQ-004` |
| `TEST-008` | Unit | Gateway Strategy | Gateway primary failure | never implicit Direct | fake Provider manager | reference test | `REQ-002`,`REQ-003` |
| `TEST-009` | Contract | README/public annotations | compile old source fixture | compiles; default behavior documented | Java compiler/test fixture | starter test | `REQ-002`,`REQ-022` |
| `TEST-010` | Unit | Provider Manager | same query twice | one subscription/cache | fake Directory | existing manager test | `REQ-005` |
| `TEST-011` | Unit | DDC subscription | initial start | listener registered before full pull；snapshot published | fake Redisson/loader | DDC starter test | `REQ-005` |
| `TEST-012` | Unit | DDC subscription | relevant event burst | refresh coalesced and full pull | scheduler fake | DDC test | `REQ-005` |
| `TEST-013` | Unit | DDC subscription | event lost | periodic pull converges | fake clock | DDC test | `REQ-005`,`REQ-006` |
| `TEST-014` | Unit | Provider Manager | revision N then N-1 | N-1 ignored | snapshots | manager test | `REQ-006` |
| `TEST-015` | Unit | cache expiry | DDC down past lease expiry | endpoint no longer selectable | fake clock/channel | manager test | `REQ-006` |
| `TEST-016` | Unit | RANDOM | 3 candidates seeded/statistical bound | all selected within tolerance | deterministic RNG seam | LB test | `REQ-008` |
| `TEST-017` | Unit | WEIGHTED_RANDOM | weights 1:3:6 | long-run ratio tolerance and zero invalid | RNG seam | LB test | `REQ-008`,`REQ-009` |
| `TEST-018` | Unit | ROUND_ROBIN | sorted candidates | stable cycle and exclusion | endpoint fixtures | LB test | `REQ-008` |
| `TEST-019` | Unit | SWRR | weights 1:2:3 | every six selects exact distribution | endpoint fixtures | LB test | `REQ-008`,`REQ-009` |
| `TEST-020` | Unit | CONSISTENT_HASH | stable keys/candidates | same key same endpoint | resolver/key fixtures | LB test | `REQ-008`,`REQ-009` |
| `TEST-021` | Unit | CONSISTENT_HASH | add/remove one endpoint | only bounded key subset remaps | 10k fixed keys | LB test | `REQ-008`,`REQ-009` |
| `TEST-022` | Unit | Hash resolver | missing/blank/raw secret-like key | fail/redact；resolver once per invocation | named beans | resolver test | `REQ-009` |
| `TEST-023` | Adapter | weight projection | Provider/Gateway metadata 80/invalid/missing | both endpoint kinds 80/default100/default100 | DDC snapshots | both DDC Directory tests | `REQ-009`,`REQ-021` |
| `TEST-024` | Unit | LEAST_IN_FLIGHT | old enum policy | existing selection preserved | candidates | LB compatibility test | `REQ-008`,`REQ-021` |
| `TEST-025` | Unit | LB registry | all enum values | every supported value resolved，INHERIT rejected after plan | enum loop | LB test | `REQ-008` |
| `TEST-026` | Unit | Channel pool | two queries same endpoint | factory called once | counting factory | channel test | `REQ-017` |
| `TEST-027` | Component | multiplexing | 100 concurrent unary calls | same Channel；all complete；no serialization | in-process server | starter component test | `REQ-017` |
| `TEST-028` | Unit | Channel drain | one ref removed, another retained | Channel active | pool entries | channel test | `REQ-011`,`REQ-017` |
| `TEST-029` | Unit | Channel drain | refs 0, inFlight >0 | waits then closes on completion | latch | channel test | `REQ-011`,`REQ-017` |
| `TEST-030` | Unit | Channel force close | drain timeout | shutdownNow once；callback completes | fake channel | channel test | `REQ-011`,`REQ-017` |
| `TEST-031` | Unit | Provider lifecycle | startup register success | STARTING→READY；availability only after lease | fake Server/Registry | lifecycle test | `REQ-007`,`REQ-010` |
| `TEST-032` | Unit | Provider lifecycle | failFast register fail | FAILED/STOPPED；no heartbeat/server | fake failure | lifecycle test | `REQ-010` |
| `TEST-033` | Unit | Provider lifecycle | non-failFast fail/recover | DEGRADED→READY after active heartbeat register | fake clock/Registry | lifecycle test | `REQ-007`,`REQ-010` |
| `TEST-034` | Unit | heartbeat | interval >= lease | startup rejects | properties | properties/lifecycle test | `REQ-007` |
| `TEST-035` | Unit | DDC boundary | heartbeat | no reverse Provider call/probe type | module boundary/static | DDC/RPC boundary test | `REQ-007`,`REQ-021` |
| `TEST-036` | Unit | Provider stop | READY with in-flight | DRAINING, no new, existing completes, STOPPED | latch/server | lifecycle test | `REQ-011` |
| `TEST-037` | Unit | Provider stop | timeout | shutdownNow and callback once | fake server | lifecycle test | `REQ-011` |
| `TEST-038` | Unit | Consumer stop | active subscriptions/channels | no new invocation；close/drain idempotent | fake managers | lifecycle test | `REQ-011` |
| `TEST-039` | Unit | Token bucket | burst/refill boundaries | exact admits/rejects/retryAfter | fake ticker | rate-limit test | `REQ-012`,`REQ-013` |
| `TEST-040` | Unit | Leaky bucket | burst/leak | water never over capacity | fake ticker | rate-limit test | `REQ-012` |
| `TEST-041` | Unit | Sliding window | boundary just before/after period | exact rolling count | fake ticker | rate-limit test | `REQ-012` |
| `TEST-042` | Concurrency | all limiters | 64 threads acquire | admits never exceed rule | executor/barrier | rate-limit test | `REQ-012` |
| `TEST-043` | Unit | Guard plan/startup validation | unknown algorithm、Sliding requestedTokens!=1、专用 rule多 policy | application startup fails with exact Guard diagnostic | rule fixtures | Guard validator test | `REQ-012` |
| `TEST-044` | Component | RPC Provider + Guard AOP | method `@RateLimitGuard` rejected | target bean count 0；UNAVAILABLE + provider stage + rate-limit type | real Spring proxy/fake Guard backend | RPC starter component test | `REQ-013` |
| `TEST-045` | Unit | RPC Guard Adapter/status mapper | RATE_LIMITED vs other Guard rejection；Direct vs Gateway trailers | only RATE_LIMITED→UNAVAILABLE；Direct error-type maps RPC_RATE_LIMITED；Gateway stage-only maps RPC_PROVIDER_UNAVAILABLE；other Guard rejection delegates | outcome/trailer fixtures | mapper test | `REQ-013`,`REQ-020` |
| `TEST-046` | Contract | Guard scope/storage | same/different ruleId,stateVersion,keyHash on Local/Redisson | same key shares quota；different key isolates；RPC fullMethodName not silently added | backend fixtures | Guard contract/integration test | `REQ-012` |
| `TEST-047` | Unit | Async Provider | permit success then future fail | permit not refunded；one error | future fixture | server test | `REQ-012`,`REQ-015` |
| `TEST-048` | Unit/Integration | Guard rate state cleanup | many transient keys / Redis expiry | Local bounded/evicted；Redisson TTL set for all algorithms | fake ticker/Testcontainers when enabled | Guard backend tests | `REQ-012`,`REQ-015` |
| `TEST-049` | Unit | Executor | idempotent=false + retries=1 + UNAVAILABLE | exactly 2 attempts | fake channels | executor test | `REQ-019` |
| `TEST-050` | Unit | Executor | representative non-UNAVAILABLE business statuses | one attempt；original error；FAIL_OPEN/local fallback not invoked | INVALID_ARGUMENT/PERMISSION_DENIED/NOT_FOUND/FAILED_PRECONDITION/ALREADY_EXISTS/ABORTED | executor test | `REQ-020` |
| `TEST-051` | Unit | Executor | rate-limit UNAVAILABLE then healthy；all-rate-limited；mixed unavailable+limited | second endpoint selected/mode unchanged；all-rate Direct→RPC_RATE_LIMITED；mixed Direct→RPC_PROVIDER_UNAVAILABLE；Gateway→RPC_PROVIDER_UNAVAILABLE | status/trailer fixtures | executor test | `REQ-013`,`REQ-020` |
| `TEST-052` | Unit | Executor | total deadline across two attempts | second gets only remaining time | fake ticker | executor test | `REQ-020` |
| `TEST-053` | Unit | Executor | cancel during attempt | call cancelled, no retry | fake ClientCall | executor test | `REQ-015`,`REQ-020` |
| `TEST-054` | Unit | Executor | retry budget max/negative overflow | bounded startup validation | properties/annotations | resolver test | `REQ-020` |
| `TEST-055` | Unit | Invocation identity | same-mode retry | same invocation ID/mode；failed endpoint excluded | interceptor capture | executor test | `REQ-019`,`REQ-020` |
| `TEST-056` | Unit | availability terminal failStrategy | FAIL_CLOSED/FAIL_OPEN/LOCAL_FALLBACK across typed/generic sync/async | closed throws；open returns/completes null；fallback once；business error never invokes any strategy | beans/statuses | executor test | `REQ-001`,`REQ-020` |
| `TEST-057` | Contract | Validator | Message→Message | BLOCKING plan, descriptor match | Echo contract | validator test | `REQ-014` |
| `TEST-058` | Contract | Validator | Message→CompletionStage<Response> | ASYNC plan, generic type match | async Echo interface | validator test | `REQ-015` |
| `TEST-059` | Unit | Async executor | immediate call | method returns before response latch | fake future call | executor test | `REQ-015` |
| `TEST-060` | Race | Async executor | response/error/cancel race | exactly one terminal/release | barriers | executor test | `REQ-015`,`REQ-020` |
| `TEST-061` | Component | Async Provider/Consumer | async Echo | end-to-end result/cancel | in-process Netty | component test | `REQ-015` |
| `TEST-062` | Component | Generic blocking | canonical `egon.rpc.fixture.v1.UnaryFixtureService/Echo` + typed request bytes | raw response equals typed serialization；dot alias/mismatched service rejected | Echo descriptor | generic test | `REQ-016`,`REQ-021` |
| `TEST-063` | Component | Generic async | same + cancel | parity and cancellation | Echo descriptor | generic test | `REQ-015`,`REQ-016` |
| `TEST-064` | Security | Generic invoker | attempts arbitrary Authorization/metadata | API has no field/injection path | compile/reflection | generic test | `REQ-016`,`REQ-021` |
| `TEST-065` | Unit | CGLIB proxy | old interface | CGLIB class; Object methods local; Method map O(1) | Echo contract | proxy test | `REQ-018` |
| `TEST-066` | Process | Direct mode isolation | two Providers；first unavailable/limited，second healthy；Gateway counter enabled | Direct reselects Provider and Gateway counter remains 0 | existing harness | RpcProcessIT | `REQ-003`,`REQ-004`,`REQ-019` |
| `TEST-067` | Process | Gateway mode isolation | two Gateways fail/exhaust；Direct Providers healthy | Gateway reselect/exhaust follows policy and Direct Provider-direct counter remains 0 | harness | RpcProcessIT | `REQ-003`,`REQ-004` |
| `TEST-068` | Static/docs | README/config metadata | new fields/behavior | bilingual agreement; old contradictions absent | rg + metadata JSON | module verification | `REQ-022` |
| `TEST-069` | Unit | Generic target cache | exceed max/advance idle timeout/concurrent same target | LRU/idle entries close demand exactly once；same key shares entry | fake strategy/demand handle | generic cache test | `REQ-005`,`REQ-016`,`REQ-017` |
| `TEST-070` | Unit | Consumer lifecycle | registered Gateway primary + Direct primary demands | only configured-mode subscriptions/pool start before gate；READY/DEGRADED exact；no fallback-only demand | fake managers/pool | lifecycle coordinator test | `REQ-005`,`REQ-023` |
| `TEST-071` | Unit | Consumer startup failure | primary Gateway discovery throws/times out | FAILED then all opened subscriptions/pool entries close；no invocation accepted | fake failure/latches | lifecycle coordinator test | `REQ-011`,`REQ-023` |
| `TEST-072` | Unit | Consumer draining | in-flight call plus new call | existing drains；new fails immediately；STOPPED callback once | fake executor/pool | lifecycle coordinator test | `REQ-011`,`REQ-023` |
| `TEST-073` | Integration | Redisson three algorithms | token/leak/window boundary + concurrent acquire against real Redis | Local-equivalent decisions；atomic admits never exceed rule；Redis TIME used | Testcontainers Redisson profile | Guard Redisson integration test | `REQ-012`,`REQ-013` |
| `TEST-074` | Unit | AccessGuardRedisKeyFactory | same rule/state/hash for three algorithms | token exact legacy key；leak/window exact safe suffix；raw key absent | fixed hash/config | Guard key-factory test | `REQ-012`,`REQ-021` |
| `TEST-075` | Unit/Integration | Redisson config/algorithm transition | same stateVersion changes capacity/window/algorithm | config mismatch resets atomically；old suffix TTL remains；no WRONGTYPE/cross-slot | scripted mock + Testcontainers | Guard backend test | `REQ-012`,`REQ-013` |

Targeted validation commands for later implementation:

```bash
./mvnw -B -ntp -f egon-cola-components/pom.xml \
  -pl :egon-cola-component-access-guard-starter,:egon-cola-component-rpc-starter,:egon-cola-component-rpc-ddc-adapter \
  -am test

./mvnw -B -ntp -f egon-cola-components/pom.xml \
  -pl :egon-cola-component-rpc-test-contract -am verify
```

Commands may be further focused by an explicit concrete `-Dtest` class list during a Plan, but a lost/terminated Maven handle is not a pass; capture exit code. No service is started automatically.

## 15. Non-functional and Cross-cutting Design

| Concern | Required behavior/SLO | Current evidence | Design mechanism | Failure/degradation behavior | Verification |
| --- | --- | --- | --- | --- | --- |
| Latency | success path adds no DDC call and no artificial rate-limit wait | current cache/direct call | immutable local snapshot、precompiled plan、shared Channel；Guard only admission, never queue | same-mode reselection/Redisson Guard adds latency only within same deadline | unit timing boundaries；benchmark optional, live P99 unverified |
| Throughput | concurrent unary multiplex without global invocation lock | gRPC ManagedChannel + current per-manager channels | shared pool；LB locks scoped per service/algorithm；limiter atomic | remote HTTP/2 stream cap queues in transport | 100+ concurrent component calls；production capacity unverified |
| Memory | RPC state bounded by active exact queries/endpoints/plans；Guard Local state bounded/idle-evicted | current registrations + Guard maxEntries/idleTtl | close subscriptions、remove LB state/pool refs；reuse Guard cleanup | dynamic generic targets or unbounded Guard keys could grow state | generic cache + Guard cleanup/TTL tests |
| Availability | Redis event loss recovered by poll；DDC outage never extends expired lease；mode never changes | existing coordinator + user decision | event hint + full snapshot + local expiry + same-mode candidate exclusion | empty/exhausted cache produces mode-specific unavailable；no cross-mode rescue | event/poll/outage/no-cross-mode tests |
| Correctness | no stale revision rollback；one terminal async completion | current revision field, no async | monotonic checks、atomic terminal guard | anomaly logged and full pull retried | race/revision tests |
| Retry safety | no idempotency gate but bounded duplicate attempts；business errors remain visible | user decision | acquisition/UNAVAILABLE whitelist、same-mode distinct-candidate cap、total deadline | business may duplicate if it violates contract；business error never hidden by fail strategy | framework attempt/status tests + separate business proof |
| Security | generic cannot inject arbitrary metadata；keys/payloads not logged | existing interceptors/metadata allowlist | same interceptor factories、API excludes headers、redaction | invalid resolver/payload fails closed | reflection/security tests |
| Rate-limit fairness | Guard rule/key scope, Local monotonic/Redis server time, no blocking | existing token bucket/Local/Redisson | algorithm Strategy Factory；exact window/leak/token contracts；RPC only Adapter | quota scope follows Guard backend；hot rule/key skew remains operational concern | deterministic/concurrent/Redis tests；Guard ops docs |
| Compatibility | old typed Proto、Provider marker and Guard annotation remain | current fixtures/source | additive Direct common fields/LB enum/Guard algorithm enum/API；wire unchanged | previously unwired retries/LB and nullable FAIL_OPEN become active | compile/descriptor/process regression |
| Operability | lifecycle/reselection/Guard-limit logs/events carry stable low-cardinality dimensions | existing trace/invocation ID + Guard observability | structured SLF4J、existing trace context、Guard metrics/events | no RPC-owned dashboard/alert | source/test only；deployment observability open |
| Maintainability | algorithms/route modes isolated without class explosion | current feature packages/Strategy precedents | nested strategies、records、one executor | complex executor requires exhaustive matrix | unit tests + package boundary tests |

Security/tenancy invariants:

- Direct/Gateway discovery target仍来自 Reference/Contract/server configuration，不从未经验证的 inbound request Header 选择 DDC scope。
- Generic caller cannot choose arbitrary credentials/metadata；existing process identity、trace、token relay/Server security apply。
- Load-balance affinity key and raw payload are sensitive-by-default：不得写日志、metric tag、exception message；hash only lives in invocation memory。
- Provider rate limit 通过 Guard advice 发生在 RPC availability/security gate 之后、业务 method 之前；它不是授权替代品。Guard raw key、request payload、token 都不得进入 RPC trailer/log。

## 16. Compatibility, Migration, Rollout, and Rollback

### 16.1 Compatibility contract

- Java source: existing `@EgonRpcReference`、`@EgonRpcDirectReference`、field-free `@EgonRpcProvider`、`@RateLimitGuard` usages compile；Reference 新共同元素有默认值，Guard annotation 不加字段。
- Behavior: Gateway Reference 永远只走 Gateway；Direct Reference 永远只走 Provider。可用性失败开始按 LB 换同模式节点；业务错误仍报错且不触发 fail strategy。
- Enum: existing LoadBalance constants remain；add `WEIGHTED_RANDOM` and `CONSISTENT_HASH`；`SMOOTH_WEIGHTED_ROUND_ROBIN` is weighted RR；`LEAST_IN_FLIGHT` retained。
- Wire: no business/DDC Proto modification；service/method/request/response descriptor unchanged。Generic call只接受 existing canonical `fully.qualified.Service/Method`。
- Status: add Direct-observable `RPC_RATE_LIMITED` application code；Guard limit transport status is always `UNAVAILABLE` + Provider stage，Provider/Direct 还带 rate-limit type；Gateway 当前重建 trailer 后映射 `RPC_PROVIDER_UNAVAILABLE`。不增加 `RPC_FAILOVER_EXHAUSTED`。
- Configuration: existing RPC properties remain；fully inherited Gateway calls preserve `gateway-max-attempts=2` but no longer check idempotent，fully inherited Direct calls preserve zero retries。Guard `algorithm=TOKEN_BUCKET` default不变，新增 enum values只在显式配置时生效。
- Nullability: `FAIL_OPEN` 的 blocking/async typed/generic availability-exhaustion result 变为 `null`；业务调用点必须 release-note 并增加 null handling。
- Persistence: no migration/data backfill。

### 16.2 Rollout order

1. User reviews this `Review` Spec and explicitly marks it `Accepted` before Plan；all behavior decisions are already closed，but acceptance is not inferred。
2. Publish Access Guard algorithm extension and RPC Starter/DDC Adapter at one compatible component version；DDC Admin/Gateway need no production code deployment。
3. Provider applications that use rate limiting explicitly include Access Guard Starter，annotate implementation methods，and upgrade Guard+RPC together；begin with `TOKEN_BUCKET` existing behavior and verify AOP/UNAVAILABLE mapping。
4. Async Java Contract module and its Provider/Consumer upgrade together because Java signature changes，even though Proto does not。
5. Upgrade Consumers with retries=0 first；verify discovery cache/channel pool/CGLIB and Direct/Gateway no-cross-mode counters。
6. Enable retries per call site only after business owner proves duplicate safety；retries remain same-mode。Enable weighted/hash LB only after weight/resolver distribution tests。
7. Explicitly enable LEAKY_BUCKET/SLIDING_WINDOW Guard rules conservatively；observe Guard rejections/backend load and same-mode retry amplification before tightening。

### 16.3 Rollback

- Consumer can roll back to previous Starter because wire/Proto/DDC storage unchanged；new async Java Contract binaries cannot run on an old runtime that rejects return shape, so roll back Contract/Provider/Consumer together for those methods。
- `@EgonRpcProvider`/`@RateLimitGuard` 无新字段，不需要 annotation binary rollback。Async Contract仍需 Contract/Provider/Consumer成组回退。
- Same-mode retries 可把 Reference `retries=0` 后 redeploy；不存在跨模式 fallback 开关或 Gateway Route cleanup。
- Removing shared Channel pool/LB state requires process restart only；no persisted data rollback。
- Guard rate-limit rollback先把 LEAKY_BUCKET/SLIDING_WINDOW rules改回 TOKEN_BUCKET或 disable，再回退 Guard artifact；现有 namespaced Redis state由 idle TTL清理，不做 destructive delete。
- If new runtime causes drain issues, revert application artifact；DDC TTL removes abandoned leases even if deregistration failed。

### 16.4 Deployment verification boundary

Static/module tests can prove contracts、algorithms、lifecycle ordering and in-process gRPC. They cannot prove production Redis Pub/Sub loss rate、Guard Redis contention、DDC latency、HTTP/2 negotiated stream caps、real P99、business idempotency or Kubernetes/systemd termination grace. Those require user-started runtime tests after implementation；the agent must not start services automatically。

## 17. Alternatives and Decisions

| Option | New elements and interactions | Advantages | Disadvantages/risks | Repository fit | Decision and rationale |
| --- | --- | --- | --- | --- | --- |
| A — direct/reuse baseline | keep two factories/JDK/fixed RR/blocking/no rate limit | no change | fails most explicit requirements | current code only | Rejected |
| B — selected unified invocation core | internal common policy、mode Strategy Factory、one executor、existing DDC subscription、shared Channel、Guard reuse/Adapter | smallest coherent design satisfying all 11 items；wire unchanged；no cross-mode state | executor/lifecycle concurrency + RPC/Guard integration complexity | aligns existing SPI/Adapter/Strategy/Guard | Selected；ready for review |
| C — one new public `@EgonRpcReference(mode=DIRECT)` | new annotation and migration aliases | one declaration surface | third annotation/deprecation/migration；does not avoid legacy fields now | weak compatibility | Rejected for current scope |
| D — use Gateway for all LB/retry/limit | no Direct governance | centralized | contradicts Consumer/Provider-side requirements and Direct mode | conflicts user | Rejected |
| E — new GenericInvoke Proto service | envelope method/Provider adapter/Gateway route | Map-like UX possible | second protocol、auth/routing/version/size attack surface | conflicts Proto single authority/minimal design | Rejected by `DEC-012` |
| F — RPC-local Provider limiter | new annotation fields/keys/backend/failure policy | RPC self-contained | duplicates Guard and breaks Provider marker；two policy authorities | conflicts user decision/repository ownership | Rejected；reuse Guard Local/Redisson |
| G — hash entire request | no resolver SPI | easy default | affinity changes with irrelevant fields; may hash sensitive bytes | technically possible, semantically weak | Rejected by `DEC-013` |

Material decision comparisons:

| Decision | Option A | Option B | Recommendation basis |
| --- | --- | --- | --- |
| Mode failure | same-mode endpoint reselection | Direct/Gateway cross-mode fallback | user fixed mode on client；前置 Direct Spec也禁止 fallback；select same-mode only |
| Rate ownership/scope | Guard rule/key + configured Local/Redisson backend | RPC Provider annotation/per-fullMethod state | user selected existing Guard annotation；Guard already owns key/storage/failure policy |
| Async | CompletionStage Contract | separate async facade | same typed method semantics、native cancellation、no ThreadLocal/facade mapping |
| Generic | canonical `Service/Method` + bytes/descriptor | dot alias、Map/Object[] or envelope | existing gRPC/Gateway identity and no second serializer |
| Consistent key | resolver | payload/invocationId | business owns affinity semantics |
| FAIL_OPEN | availability exhaustion returns null | default Proto/forbid | user explicitly selected null；business errors excluded from fail strategy |

## 18. Risks and Open Questions

| ID | Risk/question | Probability | Impact | Mitigation or decision owner | Status |
| --- | --- | --- | --- | --- | --- |
| `RISK-001` | Cross-mode code accidentally registers/uses the other Directory during refactor | Medium | hidden topology/security behavior violates client mode | Strategy Factory returns one immutable strategy；module mocks/process counters assert zero opposite-mode calls | Mitigated by design/tests |
| `RISK-002` | Same-mode candidate list changes concurrently with retries | Medium | endpoint may disappear or a new endpoint may not be tried in current call | each selection reads latest immutable snapshot but preserves excluded stable identities；current logical call is bounded, next call sees full new set | Mitigated by design |
| `RISK-003` | Retry after unknown Provider outcome duplicates non-idempotent side effects | High | business correctness/data loss | business unique key/upsert tests before retries>0；attempt cap | Accepted user risk, operational proof required |
| `RISK-004` | Guard quota scope differs between Local and Redisson or key contributors are too coarse | High | quota unexpectedly per-instance/cluster-wide or unrelated callers share quota | rule docs name storage/key contributors；Local/Redisson contract tests；operator owns rule review | Open operational, not design blocker |
| `RISK-005` | CGLIB may conflict with native image/AOT/module access | Medium | startup/proxy failure in unsupported deployment | interface-only、Spring CGLIB、AOT non-goal；fixture test | Open non-blocking unless native deployment exists |
| `RISK-006` | Shared Channel key ignores a future distinct TLS/credential profile | Low now | wrong security context reuse | key includes transport profile when profiles become real；programmatic Direct stays isolated | Mitigated by current single business TLS config |
| `RISK-007` | SWRR/consistent ring/subscription state grows for dynamic generic targets | Medium | memory leak | bounded 256-entry/10-minute-idle generic target cache；eviction closes demand；`TEST-069` | Mitigated by design |
| `RISK-008` | Async Provider CompletionStage ignores cancellation | Medium | wasted work after caller leaves | cancel Future when supported + require gRPC Context observation | Open, test/doc |
| `RISK-009` | Exact sliding-window state uses O(capacity) entries per Guard rule/key | Medium | high cardinality/capacity increases heap/Redis memory and script time | capacity cap 100,000、Local maxEntries/idle cleanup、Redis TTL、representative capacity tests | Mitigated by bounded design；production sizing required |
| `RISK-010` | Gateway 当前不保留 Provider rate-limit error-type trailer | Confirmed static | Gateway-mode exhaustion maps `RPC_PROVIDER_UNAVAILABLE` instead of `RPC_RATE_LIMITED` | 两者 transport status均为 UNAVAILABLE；Spec明确 mode-specific application-code差异；若未来要求统一应用码，另行修改 Gateway safe trailer allowlist | Closed by explicit scope/contract |
| `RISK-011` | Current dirty worktree/branch may change RPC/Guard paths before Plan | Medium | plan paths/evidence drift or merge conflict | refresh baseline/status and path-limited commits before Plan/implementation；preserve unrelated user edits | Open operational |
| `RISK-012` | DDC reconcile interval 10s and lease 30s may be unsuitable for some deployments | Medium | slow change convergence or load | existing configurable DDC property；capacity test before production | Open runtime tuning, not code blocker |
| `RISK-013` | Generic API weakens compile-time request correctness | High by nature | INVALID_ARGUMENT/runtime mistakes | canonical identity、raw-only restricted API、size validation、descriptor convenience、same auth | Accepted by `DEC-012` |
| `RISK-014` | Rate-limit is UNAVAILABLE and can amplify one hot request across every same-mode instance | High under cluster-wide bursts | extra Provider/Redis load and latency | retries/distinct candidates/Deadline hard caps；per-reference retries default conservative；Guard rejection metrics；load test before enablement | Open operational |
| `RISK-015` | `FAIL_OPEN=null` may cause NPE in callers written against historical non-null Protobuf expectations | High where FAIL_OPEN is enabled | caller failure after intended degradation | only availability exhaustion returns null；release note/static search/null-path tests；default FAIL_CLOSED remains recommended | Accepted user decision, migration review required |

Open major questions: `None`。`DEC-008`–`DEC-014` 已关闭；上述 open risks 是实现/容量/发布验证事项，不改变已选公共语义。

## 19. Traceability Matrix

| Requirement | Use case | Affected area/chapter | Context-only or unchanged boundary | Interface/model/database/frontend | Tests | Acceptance evidence |
| --- | --- | --- | --- | --- | --- | --- |
| `REQ-001` | `UC-001` | reference resolver/proxy `§7–§10,§13–§18` | DDC/Gateway contracts unchanged | `INTERNAL-001`,`INTERNAL-002`; `RpcReferencePolicy` | `TEST-001`–`003`,`056` | both annotations resolve identical common semantics |
| `REQ-002` | `UC-001` | Gateway strategy/proxy | Provider Directory unchanged for Gateway | `INTERNAL-001`,`INTERNAL-004/005` | `TEST-001`,`008`,`009`,`057`,`065` | old source compiles and queries only Gateway |
| `REQ-003` | `UC-001` | Strategy Factory/mode isolation | Gateway/Provider Directory implementations context-only | `INTERNAL-001`,`INTERNAL-002`; immutable mode | `TEST-004`–`008`,`066`,`067` | Direct/Gateway opposite-mode call count is zero |
| `REQ-004` | `UC-001` | executor same-mode reselection/exhaustion | no cross-mode fallback dependency | `INTERNAL-001`,`INTERNAL-002`; excluded endpoints | `TEST-005`–`008`,`051`,`066`,`067` | LB selects distinct same-mode nodes then mode-specific error |
| `REQ-005` | `UC-002` | Manager cache/LB integration | existing DDC coordinator kept | existing snapshot models | `TEST-010`–`013`,`069` | one cache/query, event+poll updates；generic demands bounded |
| `REQ-006` | `UC-002` | revision/expiry | DDC Redis storage context | endpoint/snapshot | `TEST-013`–`015` | no old revision/expired endpoint |
| `REQ-007` | `UC-005` | Provider lifecycle/config | DDC no probe | Provider registration/state; DB N/A | `TEST-031`–`035` | RPC-owned heartbeat and validated intervals |
| `REQ-008` | `UC-003` | LB strategies | Gateway LB unchanged | LoadBalance enum/SPI | `TEST-016`–`025` | all five algorithms + compatible least-in-flight |
| `REQ-009` | `UC-003` | weight/hash resolver | DDC metadata wire unchanged | endpoint weight; `INTERNAL-008` | `TEST-017`,`019`–`023` | weighted distribution and stable key |
| `REQ-010` | `UC-005` | Provider state/start order | DDC lease API unchanged | runtime state | `TEST-031`–`033` | READY only after lease, clean failure |
| `REQ-011` | `UC-005` | Provider/Consumer/pool drain | business calls unchanged | lifecycle/channel lease | `TEST-028`–`030`,`036`–`038` | no new calls, in-flight drain/force timeout |
| `REQ-012` | `UC-006` | Guard annotation/rate Strategy Factory/Redis state | `@EgonRpcProvider` unchanged | `INTERNAL-003`; Guard rate config/request/decision；§11 keys | `TEST-039`–`043`,`046`–`048`,`073`–`075` | three Local/Redisson algorithms, key compatibility and invalid config |
| `REQ-013` | `UC-006` | Guard AOP/RPC Adapter/error mapping | Gateway safe trailer reconstruction context-only | `INTERNAL-003`; Direct `RPC_RATE_LIMITED` / Gateway `RPC_PROVIDER_UNAVAILABLE` | `TEST-044`,`045`,`047`,`051`,`066`,`073`,`075` | both UNAVAILABLE/provider，bean not invoked，same-mode retry，application-code差异显式 |
| `REQ-014` | `UC-001` | validator/proxy/executor | Proto wire unchanged | `INTERNAL-004`; BLOCKING plan | `TEST-057`,`065` + existing Echo tests | old typed call parity |
| `REQ-015` | `UC-001`,`UC-004` | async validator/executor/server | unary wire unchanged | `INTERNAL-005`,`INTERNAL-007`; ASYNC plan | `TEST-047`,`053`,`058`–`061`,`063` | non-blocking/cancellable/single terminal |
| `REQ-016` | `UC-004` | generic API/raw marshaller/cache | no new Generic Proto/Gateway API | `INTERNAL-006`,`INTERNAL-007`; generic invocation/cache | `TEST-062`–`064`,`069` | canonical Service/Method, same unary result/error/security，bounded target state |
| `REQ-017` | `UC-003` | Channel pool/managers | gRPC HTTP/2 wire unchanged | channel key/lease | `TEST-026`–`030`,`069` | one Channel same endpoint, concurrent streams，eviction releases refs |
| `REQ-018` | `UC-001` | CGLIB/proxy plan | Contract remains interface | proxy/interceptor/plan | `TEST-001`,`057`,`058`,`065` | CGLIB and no per-call linear lookup |
| `REQ-019` | `UC-001` | executor retry gate | business data ownership unchanged | invocation context | `TEST-049`,`055`,`066` | idempotent=false still retries when configured |
| `REQ-020` | `UC-001`,`UC-004` | executor failure/deadline/cancel/null | business error meanings preserved | all call APIs | `TEST-050`–`056`,`060`,`063`,`066`,`067` | only acquisition/UNAVAILABLE retries；business error bypasses fail strategy；FAIL_OPEN null |
| `REQ-021` | all | compatibility/rollout | Proto/DDC/Gateway/DB unchanged | Interface/model only; DB/frontend N/A | `TEST-023`,`024`,`035`,`057`,`062`,`064` | zero proto/migration/Gateway production change |
| `REQ-022` | `ACTOR-006` operational goal | docs/config/tests | runtime proof remains user-owned | README/config metadata | `TEST-009`,`068` + Maven commands | bilingual docs and focused suite pass |
| `REQ-023` | `UC-002` | Consumer lifecycle/gate `§7,§8,§10,§14–§16` | DDC/Gateway primary semantics preserved | `RpcConsumerLifecycleCoordinator/RuntimeState` | `TEST-070`–`072` | subscriptions/pool precede invocation；failure/drain cleanup complete |

Every proposed file/model/interface in §7.0/§8/§9/§10 maps to one of the requirements above；removed candidates have an explicit `Remove` verdict and do not enter the target interface inventory。

## 20. Review and Acceptance

### 20.1 Original-request fidelity

用户 11 项需求全部映射：Reference 公共字段/客户端固定模式/同模式换节点=`REQ-001`–`004`；discovery cache=`REQ-005/006`；heartbeat=`REQ-007`；五算法=`REQ-008/009`；Provider/Consumer优雅启停=`REQ-010/011/023`；Guard Provider限流/UNAVAILABLE=`REQ-012/013`；阻塞/`CompletionStage`=`REQ-014/015`；canonical `Service/Method` 泛化=`REQ-016`；多路复用=`REQ-017`；CGLIB=`REQ-018`；业务幂等与业务错误可见=`REQ-019/020`。`FAIL_OPEN=null` 已进入 §5/§9/§10/§14/§16/§19。

### 20.2 Repository and technical fidelity

- Paths/symbols、Java/gRPC/Protobuf/Spring versions、现有 DDC subscription、Redis lease、Provider/Consumer lifecycle、tests 和 dirty-worktree 均来自本轮静态检查。
- 本设计保留 current custom feature architecture，不套用三层业务包；无 DAO/DB/frontend。
- 未把静态/Mock 证据表述为 live DDC/Redis/gRPC proof。
- 2026-08-21 基线存在无关 Gateway 删除/未跟踪 Spec/Plan/文件；本次只修改当前 RPC Spec。后续 Plan 必须刷新 RPC/Guard path 与 status，并继续 path-limited 操作。

### 20.3 Cross-section consistency

- Architecture、file tree、API、models、failure matrix、tests、rollout 和 traceability 使用同一“Strategy Factory 固定 Gateway/Direct mode→same-mode cache/LB→shared pool→endpoint”链，任何章节都不再允许跨模式 fallback。
- DDC Event 只触发 full pull；revision/lease full snapshot 是 authority；没有第二 cache/job/topic。
- Blocking/async/generic 都复用同一 invocation plan/executor；normal success non-null，而 availability `FAIL_OPEN` 的四种 Java shape均返回/完成 null。
- Provider rate limit uses Guard annotation/rule/key/storage/observability；RPC 只做 AOP proxy compatibility 和 RATE_LIMITED→UNAVAILABLE Adapter。Guard 三算法均在 Guard change surface/file tree/tests 中。
- Retry 明确不依赖 idempotent，同时仅允许 acquisition/UNAVAILABLE、受 same-mode distinct candidate、budget、total deadline、cancel 约束；业务错误跳过 retry/fail strategy。
- Generic `fullMethodName` 在需求、接口、字段、测试、兼容中统一为 `fully.qualified.Service/Method`；不接受点号 alias。
- relational/frontend chapters use evidence-backed N/A，未伪装为 unchanged redesign。

### 20.4 Relationship and effective-design review

本 Spec 显式修订 2026-07-24 的“无 Consumer LB/泛化/限流/重试”边界和 2026-07-26 的 `retryOnlyIdempotent` 选择；2026-07-26/当前源码的 Provider field-free marker 规则被明确保留，限流转由 Guard method annotation。对 2026-08-15 Spec 只 supersede RPC Reference/Direct/runtime governance 范围，Gateway BIZ/APP security、token relay、definition/registration separation 继续有效。没有修改 predecessor normative text。

### 20.5 Final verdict

`PASS — Ready for user review`

本规格已把用户 2026-08-21 的 7 项确认作为 `DEC-008`–`DEC-014` 固化，未遗留 major decision、占位内容或条件性公共合同。`Review/PASS` 仅表示文档内部完整并可供逐章审核；未获得用户明确批准前不得标记 `Accepted`，也不得进入 implementation Plan 或生产代码修改。
