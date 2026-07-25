# Egon-COLA Gateway 17 项缺口修复设计

状态：已确认，实施中

基线：

- `2026-07-24-gateway-component-design.md`
- `2026-07-25-gateway-implementation-acceptance.md`
- 当前 `codex/gateway-wave-0-foundation` 代码审计结果

## 1. 目标

本设计修复验收审计中识别出的 17 项缺口，使 Gateway 从“具备主体模块和 happy
path”提升为具备明确安全边界、流式数据面、规则闭环、最终一致上报、真实可观测性、
可操作管理平台和生产部署基线的完整 Component 项目。

本轮不改变以下已确认边界：

1. Gateway Engine 自研，不引入 Spring Cloud Gateway、Nacos 或 Dubbo；
2. Provider 发现、配置和规则下发使用 Egon-COLA DDC；
3. RPC 使用 Egon-COLA RPC；
4. Starter 只上报接口定义，不上报接口调用；
5. 调用事件只由 Engine 发送到 Kafka；
6. 下游业务权限系统不在 Gateway 内实现，但 Engine 保留业务鉴权扩展点；
7. Nginx 节点管理和动态配置不属于本项目。

## 2. 修复范围与验收口径

### 2.1 P0：安全与数据面正确性

#### GAP-01 Admin 管理 API 身份与权限

问题：管理 Controller 信任可伪造的 `X-Admin-Actor-Id`，并直接构造拥有 `*`
Capability 的管理员。

修复：

- Admin 引入 Spring Security Resource Server；
- 生产模式只接受经过签名校验的 JWT Bearer Token；
- 从 `sub`、`roles`、`capabilities` Claim 构造不可伪造的 `AdminActor`；
- Capability 由接口声明，缺少 Capability 返回 403，未认证返回 401；
- `X-Admin-Actor-Id` 仅保留在显式 `local-dev` 配置下，默认关闭；
- OpenAPI HMAC 上报接口与管理 API 使用独立 SecurityFilterChain；
- 审计日志记录认证主体和授权结果，禁止由请求 Header 覆盖主体。

验收：伪造 Header 不能取得管理权限；JWT 过期、签名错误、缺少 Capability 分别失败；
合法 Token 可执行被授权操作。

#### GAP-02 HTTP 上游响应流式转发

问题：上游响应通过 `asByteArray()` 无界聚合。

修复：

- `GatewayOutboundHttpResponse` 持有 `Flux<ByteBuf>`/`Publisher` 响应体；
- Reactor Netty Adapter 使用 `responseConnection` 保持上游连接生命周期；
- Engine 将上游数据块直接写入下游，不复制为完整 byte array；
- `RESPONSE_SIZE` 使用流式累计计数，超过上限立即取消上游订阅并返回受控错误；
- 重试只允许在响应 Header/Body 尚未提交前发生；
- 对无 Body、HEAD、204、304 保持协议语义。

验收：大响应不会整体进入堆；超过规则上限中断；下游取消会传播到上游。

### 2.2 P1：规则、健康、一致性和观测闭环

#### GAP-03 CORS 运行时

- CORS Policy 编译为不可变 Matcher；
- 预检请求在路由匹配后、业务安全链前处理；
- 实际请求在响应阶段附加允许的 CORS Header；
- Origin、Method、Header、Credentials 和 Max-Age 均由 Admin Rule 决定；
- 未匹配 Origin 不返回放行 Header，通配 Origin 与 Credentials 组合在编译期拒绝。

#### GAP-04 请求和响应大小策略

- `REQUEST_SIZE` 在读取 Body 前先检查 Content-Length，并在流式读取时二次计数；
- `RESPONSE_SIZE` 在写出前检查 Content-Length，并在流式写出时二次计数；
- Policy 支持 Gateway Group、Route、Operation 作用域，最具体规则优先；
- 未配置时使用 Engine 全局硬上限，规则不能突破全局硬上限。

#### GAP-05 主动健康探测

- 新增 `ProviderActiveHealthProbe` SPI；
- HTTP Provider 使用可配置 Method/Path/Timeout/成功状态码探测；
- RPC Provider 使用标准 gRPC Health 协议，未实现时按配置降级为连接探测；
- Scheduler 按抖动间隔并发探测，具有全局并发上限；
- 成功/失败阈值形成 HEALTHY/UNHEALTHY，UNKNOWN 只用于尚未探测；
- 主动和被动健康继续分维度保存，候选过滤同时评估二者。

#### GAP-06 Engine 接入 Core 阶段化执行链

- Engine HTTP Handler 只负责协议适配和请求生命周期；
- 核心流程接入 `DefaultGatewayExecutor` 与 `DefaultGatewayFilterChain`；
- CORS、安全、治理、Provider 选择、上游调用、响应处理映射为既有 Stage；
- 过滤器按 Stage + Order 排序并在快照切换时整体替换；
- 不为简单逻辑新增多层私有方法，避免进一步扩大单体 Handler。

采用 Chain of Responsibility：Stage 本身就是已存在的稳定扩展点，适合承载有顺序且可插拔
的网关处理器。直接继续堆叠条件分支会使 17 项修复相互耦合，因此这里使用既有模式，不新建
另一套链。

#### GAP-07 Engine 运行版本与 ACK 一致性

- Engine 租约 Metadata 上报 `activeReleaseId`、`activeRuleVersion`、
  `activeRuleChecksum`、`lastApplyStatus`、`lastAckAt`；
- 每次原子激活或失败后刷新租约 Metadata；
- Admin Runtime Consistency 按 Release Target 与在线 Engine Instance 逐节点对比；
- 只有目标 Release、Version、Checksum 和成功 ACK 全部一致才标记 CONSISTENT；
- 缺节点、旧版本、Checksum 不符、最后应用失败均给出节点级原因。

#### GAP-08 真实 p50/p95/p99

- PostgreSQL 使用 `percentile_cont(0.50/0.95/0.99) within group`；
- SQLite/单元测试使用 Java 侧确定性百分位算法；
- Dashboard DTO 保持现有字段，不再用平均值和最大值冒充；
- 查询按当前时间窗和 Gateway Group/Operation 过滤。

#### GAP-09 Definition Set 生命周期

- 新上报先进入 VERIFIED；
- Provider 租约携带 Definition Set ID 后，Reconciler 将集合置 ACTIVE；
- 同一 Application/Environment 的旧集合置 RETIRED；
- 只存在于旧集合且不再被任何 ACTIVE 集合引用的 Operation 置 OFFLINE；
- 重新出现的 Operation 恢复 ACTIVE；
- 生命周期切换幂等，并写审计事件。

采用 State 模式的显式状态转换规则，但保持为领域方法/枚举校验，不引入状态类层级。

#### GAP-10 OpenTelemetry

- 使用 Micrometer Observation + Micrometer Tracing Bridge OTel；
- 创建 Engine Request、Provider Attempt、DDC Apply、Kafka Send Observation；
- Trace ID 继续优先使用前端合法值，否则由 Engine 生成；
- W3C `traceparent`/`tracestate` 传播到 HTTP/RPC Provider；
- 指标使用低基数 Tag，Operation Key 等高基数信息只进入 Span/Event；
- Exporter 完全配置化，未配置端点时使用 no-op，不能阻塞数据面。

#### GAP-11 Starter 最终一致上报

- 本地持久化最后成功 Definition Set、Payload Hash、Admin Receipt 和更新时间；
- 启动、接口定义变化、Admin 恢复后均触发协调；
- 短周期指数退避达到 `maxAttempts` 后进入低频 Reconcile，不永久停止；
- POST 结果不确定时先 GET Admin Receipt/Definition Set 再决定是否重发；
- 同一 Payload Hash 单飞，重复触发合并；
- 状态文件原子替换，损坏时隔离并重新上报。

采用 Observer + 单飞协调：接口目录变化和生命周期事件只发信号，由一个 Coordinator
串行协调，避免多个触发源重复并发上报。

#### GAP-12 Kafka Consumer 自恢复

- Consumer 外层增加监督循环和有界退避；
- 单条持久化失败不退出 Worker，不提交该 Offset；
- 可重试异常暂停分区并重试，达到阈值后进入 DLT；
- 反序列化/契约错误直接 DLT，避免毒消息永久阻塞；
- 重平衡、Wakeup、关闭分别处理；
- 增加重启、重试、DLT、Lag 指标。

### 2.3 P2：管理体验、测试与生产基线

#### GAP-13 Admin Web 完整管理能力

- 增加真实登录态、Token 持久化、刷新、退出和 401/403 页面；
- Capability 从 `/api/management/session` 获取，不再硬编码；
- 补齐 Gateway Group 新建、编辑、启停；
- 补齐 Application、Credential 管理；
- Route/Policy 使用结构化表单，支持新增、编辑、删除和校验；
- 补齐业务域、实体域、接口组、Operation 的手工维护；
- 危险操作二次确认，并显示后端返回的审计主体和版本冲突。

#### GAP-14 Playwright 管理场景

至少覆盖：

1. 登录、登出、401；
2. Capability 导航与 403；
3. Gateway Group CRUD/启停；
4. Application/Credential；
5. 三级目录和 Operation；
6. Draft Route 编辑/删除；
7. Policy 结构化编辑/删除；
8. 校验并创建 Release；
9. Release Target/一致性；
10. Provider、Trace、Audit 查询。

测试使用独立测试数据前缀并可重复清理，不依赖人工预置。

#### GAP-15 真实拓扑验收入口

- 保留并扩展 `GatewayLiveTopologyIT`；
- Compose 启动 PostgreSQL、Redis、Kafka、DDC、Admin、两台 Engine、HTTP/RPC Provider；
- Maven Profile 执行真实 HTTP/RPC 注册、Admin 可见、规则发布、调用事件闭环；
- 前端 Playwright 连接同一 Compose；
- 本地默认 `verify` 不启动外部拓扑，`gateway-live` 与 CI Nightly/Manual 执行。

#### GAP-16 性能、长稳和故障注入

- 增加 k6 HTTP/RPC 场景、容量参数和阈值；
- 增加长稳脚本和资源采样；
- 增加 Kafka/Redis/PostgreSQL/Provider 故障注入脚本；
- CI 分为快速单元层、组件层、Live 拓扑层、Admin E2E 层和 Nightly 性能层；
- 阈值、机器规格、数据规模和测试时长进入版本库，结果产物可归档。

#### GAP-17 DDC HA、RPC Gateway 多活、TLS/mTLS

DDC HA：

- DDC Admin 保持无本地会话，多个实例共享 PostgreSQL 与 Redis；
- 发布任务领取使用数据库 CAS/Skip Locked，避免重复执行；
- Redis 使用 Sentinel/Cluster URL 配置，不绑定单节点；
- 两台 DDC Admin 的健康、就绪和故障转移加入 Compose HA Profile；
- 本轮不引入 Raft；一致性事实仍由 PostgreSQL 事务和版本/CAS 保证。

RPC Gateway 多活：

- `INTERNAL_GATEWAY` 从“单 Slot”改为同 Gateway Group 多实例集合；
- RPC Consumer 按 DDC Lease 发现全部健康 Engine；
- 使用 Round Robin + 失败摘除 + 有界重试；
- Lease 变化增量更新 Channel，关闭时排空；
- 相同 Group 不再因第二个 Engine 注册而失败。

TLS/mTLS：

- PUBLIC HTTP 默认可配置 TLS；
- INTERNAL HTTP、RPC、DDC Management、Admin 管理 API 支持强制 mTLS；
- Key/Certificate/Trust Bundle 只通过文件或 Secret 注入；
- 明文只允许显式 development 配置；
- 对端 SAN/Authority 校验不允许关闭；
- 提供证书轮换重载入口和到期指标。

## 3. 横切约束

1. 所有运行规则必须是不可变快照，激活失败保留 LKG；
2. 数据面不可因为 Kafka、OTel 或 Admin 不可用而阻塞业务调用；
3. 安全策略、请求/响应硬上限、TLS 失败必须 Fail Closed；
4. 新增数据库结构只通过一个新的 `V3` Flyway 文件实现，不修改 V1/V2；
5. 所有后台循环必须可关闭、可观测，并对瞬时故障自恢复；
6. 单元/组件测试使用确定性 Clock、Scheduler 和随机种子；
7. 不自动启动业务项目；真实拓扑通过显式 Profile 或 CI Job 执行。

## 4. 验收完成定义

每个 GAP 同时满足以下条件才算完成：

- 有失败测试证明原缺口；
- 生产代码实现；
- 定向测试通过；
- 相关模块回归通过；
- 配置和运维入口有文档；
- 不存在回退为 Header 信任、全量聚合、无限重试或静默失败的旁路。

最终再执行 Gateway 27 模块 Reactor、Admin Web 全套静态验证、Compose 配置校验和代码审查。
真实容器、浏览器和性能测试是否执行，必须按实际结果单独报告，不能用“测试代码存在”
替代“真实环境已通过”。
