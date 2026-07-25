# Gateway 17 项缺口修复实施计划

> 实施方式：在 `codex/gateway-wave-0-foundation` 隔离 Worktree 内按 TDD 小步提交。

## Task 1：Admin 身份与 Capability

文件：

- `egon-cola-component-gateway-admin/pom.xml`
- `.../interfaces/management/**`
- `.../infrastructure/security/**`
- `.../src/test/java/**/security/**`

步骤：

1. 增加伪造 Header、无 Token、Capability 不足的失败测试；
2. 增加 Resource Server、安全配置和 `AdminActorResolver`；
3. Controller 改为使用认证上下文；
4. 验证管理 API 401/403/200 与 OpenAPI HMAC 隔离；
5. 提交 `fix(gateway-admin): enforce authenticated management actors`。

## Task 2：流式 HTTP 与大小策略

文件：

- `...gateway-engine/.../http/ReactorNettyHttpUpstreamAdapter.java`
- `...gateway-engine/.../http/DefaultGatewayHttpDataPlaneHandler.java`
- `...gateway-engine/.../traffic/**`
- 对应 HTTP/Traffic 测试

步骤：

1. 增加多 Chunk 不聚合、取消传播、Request/Response Size 超限测试；
2. 响应模型改为流式 Publisher；
3. 接入 Content-Length 快速拒绝和流式计数；
4. 保持 HEAD/204/304、重试和 Header 语义；
5. 提交 `fix(gateway-engine): stream upstream responses and enforce size policies`。

## Task 3：CORS 与 Core Filter Chain

1. 增加预检、实际请求、非法 Origin 和 Filter 顺序测试；
2. 编译 CORS Policy；
3. 将 HTTP 运行路径接入 `DefaultGatewayExecutor`/`DefaultGatewayFilterChain`；
4. 删除 Handler 内重复的阶段调用；
5. 提交 `refactor(gateway-engine): execute http traffic through staged filters`。

## Task 4：主动健康探测

1. 增加阈值、抖动、超时、HTTP/RPC 探测测试；
2. 实现 Probe SPI、Scheduler 和状态仓库；
3. DDC Adapter 合并主动/被动状态；
4. 增加配置、指标和关闭；
5. 提交 `feat(gateway-engine): add active provider health probes`。

## Task 5：运行一致性与 Definition 生命周期

数据库：新增唯一 `V3__gateway_runtime_reliability.sql`，不得修改 V1/V2。

1. 增加逐 Engine Release/Checksum 一致性失败测试；
2. Engine 租约上报激活 Metadata；
3. Admin 按 Target/ACK/Metadata 逐节点投影；
4. 增加 Definition ACTIVE/RETIRED、Operation OFFLINE Reconciler 测试；
5. 新增 V3 状态/索引/上报 Receipt 字段；
6. 提交 `feat(gateway-admin): reconcile runtime and definition lifecycles`。

## Task 6：真实百分位与 OpenTelemetry

1. 增加非对称样本的 p50/p95/p99 测试；
2. PostgreSQL 使用 `percentile_cont`，测试数据库使用确定性降级；
3. 增加 Request/Attempt/DDC/Kafka Observation；
4. 验证 Span 传播、错误和低基数 Tag；
5. 提交 `feat(gateway): add accurate percentiles and otel observations`。

## Task 7：Starter 最终一致上报

1. 增加 Admin 长时间不可用后恢复、进程重启、POST 不确定结果测试；
2. 增加持久状态仓库和 GET Receipt；
3. 有界快速重试后转为低频永久 Reconcile；
4. 增加定义变化触发与单飞；
5. 提交 `fix(gateway-starter): reconcile definition reports until acknowledged`。

## Task 8：Kafka Consumer 自恢复

1. 增加 Handler 瞬时失败、毒消息、Wakeup、重平衡测试；
2. 增加监督循环、分区暂停重试和 DLT；
3. 只有投影成功或 DLT 成功才提交 Offset；
4. 增加指标；
5. 提交 `fix(gateway-admin): supervise call event consumption`。

## Task 9：Admin Web 管理闭环

1. 先增加 Session、路由保护、CRUD Form 的 Vitest；
2. 后端补 Session 与缺失 CRUD API；
3. 前端接入登录态和服务端 Capability；
4. 补 Group、Application/Credential、Catalog、Route/Policy 结构化页面；
5. typecheck、Vitest、lint、build；
6. 提交 `feat(gateway-admin-web): complete authenticated management workflows`。

## Task 10：Playwright、Live 和 CI

1. Playwright 增加 10 个管理场景；
2. Live IT 增加 Admin 可见、双 Engine、Kafka 闭环和恢复场景；
3. Compose 增加双 Engine/HA Profile/TLS Profile；
4. 增加 Gateway 快速、Live、E2E、Nightly 工作流；
5. 校验 YAML/Compose/脚本语法，不自动启动产品；
6. 提交 `test(gateway): add live topology e2e and ci gates`。

## Task 11：性能与故障注入

1. 增加 k6 基准、长稳、容量测试；
2. 增加 Redis/Kafka/PostgreSQL/Provider 故障注入；
3. 增加阈值和归档说明；
4. 脚本静态校验；
5. 提交 `perf(gateway): add capacity soak and fault scenarios`。

## Task 12：DDC HA、RPC 多活与 TLS/mTLS

1. 先增加 DDC 多实例任务竞争、RPC 多 Gateway 选择/TLS 配置测试；
2. DDC 发布任务改为共享存储 CAS 领取；
3. RPC Consumer 管理多个 INTERNAL_GATEWAY Channel；
4. HTTP/RPC/Admin/DDC 增加 TLS/mTLS 配置与 Netty/Spring SSL；
5. 增加证书轮换和到期指标；
6. 提交 `feat(gateway): support ha gateways and mutual tls`。

## Task 13：最终验证与审查

1. 运行所有新增定向测试；
2. 运行 Gateway 27 模块 `clean verify`；
3. 运行 Admin Web typecheck/test/lint/build；
4. 校验 Compose、Workflow、Node 和 Shell；
5. 检查 `git diff --check`、迁移数量、依赖边界、未跟踪文件；
6. 更新验收文档为真实结果；
7. 提交 `docs(gateway): record 17 gap remediation evidence`。
