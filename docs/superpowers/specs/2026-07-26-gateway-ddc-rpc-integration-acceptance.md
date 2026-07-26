# Gateway、DDC、RPC 联调闭环验收记录

状态：代码实现与默认自动化验证已完成；真实外部拓扑待人工启动验收

执行日期：2026-07-26

实施分支：`codex/gateway-ddc-rpc-integration`

关联设计：

- `2026-07-26-gateway-ddc-rpc-integration-remediation-design.md`
- `2026-07-26-gateway-ddc-rpc-integration-plan-index.md`
- `2026-07-26-integration-01-foundation-contracts.md` ～
  `2026-07-26-integration-07-live-demo-runbook.md`

## 1. 验收结论

本轮已经完成设计中 P0、P1、P2 的代码闭环、组件级测试、默认 Gateway 集成测试、
可选真实拓扑测试入口、Compose Demo 与中英文 Runbook。Gateway、DDC、RPC 当前在公共状态、
租约、规则发布、HTTP Provider、RPC 故障语义和 DDC 管理安全合同上可以正确组合。

本结论严格限定在已经执行的自动化证据内。以下两项没有执行，因此不能标记为真实环境通过：

1. `gateway-live` Testcontainers/独立 JVM 测试；
2. Compose Demo 的完整 `up-control` 到 `verify` 生命周期。

默认测试期间 Spring Boot 测试按需启动过随机端口的进程内嵌服务器，但没有启动业务项目、
Docker 容器或留下后台进程。

## 2. 交付能力树

```text
Gateway + DDC + RPC integration
├── Shared contracts
│   ├── ONLINE/OFFLINE/UNKNOWN normalized instance status
│   ├── legacy REGISTERED/UP/EXPIRED/DOWN compatibility
│   ├── lease-expiry-aware Gateway/Provider selection
│   └── stable runtime coordinates and service identity
├── DDC publication consistency
│   ├── draft and published version separation
│   ├── exact management query and CAS publication
│   ├── atomic Redis value/version/event publication
│   ├── cluster-safe Redis hash tags and scoped keys
│   ├── recoverable dispatch and bounded ACK retry
│   └── chunk application ordering and lifecycle cleanup
├── Gateway publication
│   ├── persistent publication journal before external calls
│   ├── recoverable CHUNK and ACTIVATION phases
│   ├── stable UUIDv7 changeId reuse on retry
│   ├── inline/chunked release, rollback and LKG lifecycle
│   └── bounded protected chunk garbage collection
├── HTTP Provider runtime
│   ├── Spring Boot auto-configuration
│   ├── Spring MVC annotated controller discovery
│   ├── Spring WebFlux Mono/JSON annotated controller discovery
│   ├── report, register, heartbeat and unregister lifecycle
│   └── lease restart/recovery test scenarios
├── RPC resilience
│   ├── explicit unary method idempotency contract
│   ├── Gateway-stage and Provider-stage failure classification
│   ├── cross-Gateway retry only for idempotent methods
│   ├── availability snapshot consistency
│   └── failed Gateway slot re-registration and lease recovery
├── DDC management security
│   ├── management API authentication boundary
│   ├── HMAC operation and coordinate scopes
│   ├── shared nonce state for replay protection
│   └── credential lifecycle and compatibility contract
└── Integration delivery
    ├── opt-in Testcontainers plus independent-JVM live scenarios
    ├── dual Engine, MVC, WebFlux, RPC and rule lifecycle fixtures
    ├── Compose Demo command facade and deterministic fixtures
    ├── safe down/purge and isolated runtime secrets
    └── Chinese and English developer integration Runbooks
```

## 3. P0/P1/P2 闭环矩阵

| 缺口 | 代码状态 | 已执行证据 | 尚未验证边界 |
|---|---|---|---|
| P0-01 Gateway→DDC 请求无效 | 已闭环 | Gateway/DDC Reactor、发布协调测试 | 真实 PostgreSQL/Redis 发布链路 |
| P0-02 服务状态不一致 | 已闭环 | DDC、RPC、Gateway 状态与选择测试 | 真实进程 TTL 时序 |
| P0-03 DDC 容器不可执行 | 已闭环 | thin/exec 构件打包测试 | 实际镜像启动 |
| P0-04 联调 Redis 地址错误 | 已闭环配置与 Fixture | Gateway 默认套件、Compose render | Testcontainers 随机端口实连 |
| P0-05 HTTP Provider 不可直接消费 | 已闭环 | MVC 5 项、WebFlux 1 项、Provider Runtime 13 项 | 独立 Provider 进程租约循环 |
| P1-01 Gateway 无发布恢复日志 | 已闭环 | Admin publication journal/recovery 测试 | 真实进程中途强杀 |
| P1-02 draft/published 混用 | 已闭环 | DDC publish/pull 一致性测试 | 真实 Redis 投递中断 |
| P1-03 Redis 发布非原子 | 已闭环 | Lua/Repository 单元与集成测试 | 外部 Redis 故障注入 |
| P1-04 Redisson Bean 串线 | 已闭环 | Bean 名称、qualifier、条件装配测试 | 复杂宿主应用组合 |
| P1-05 Redis Cluster CROSSSLOT | 已闭环 key 合同 | hash-tag/Lua 合同测试 | 真实 Redis Cluster |
| P1-06 ACK 无重试 | 已闭环 | bounded retry 与幂等 ACK 测试 | 真实网络抖动 |
| P1-07 chunk 顺序与生命周期 | 已闭环 | applier 顺序、保护期、GC 测试 | 大规则真实发布与重启 |
| P1-08 RPC 重试/错误分类 | 已闭环 | RPC 17 模块、suite 10/10 | 双 Engine 独立 JVM 故障转移 |
| P2 DDC 管理安全 | 已闭环 | DDC Reactor、HMAC scope/nonce 测试 | 多 Admin 真实共享 nonce |
| P2 Compose/真实测试 | 入口已交付 | Gateway suite 16/16、Compose 静态渲染 | 完整 Demo 和 live profile |
| P2 中英文文档 | 已闭环 | 链接与过期变量静态检查 | 按 Runbook 人工走查 |

## 4. 已执行验证

### 4.1 DDC

```bash
./mvnw -B -ntp \
  -f egon-cola-components/egon-cola-component-dynamic-config-center/pom.xml \
  clean verify
```

结果：PASS，5 个 Reactor 模块全部成功。验证覆盖 management client、starter、Admin 和
DDC test suite；没有连接外部 Redis/PostgreSQL。

### 4.2 RPC

```bash
./mvnw -B -ntp -f egon-cola-components/pom.xml \
  -pl egon-cola-component-rpc/egon-cola-component-rpc-starter,\
egon-cola-component-rpc/egon-cola-component-rpc-test/egon-cola-component-rpc-test-suite \
  -am verify
```

结果：PASS，17 个 Reactor 模块全部成功，RPC suite 10/10。

直接以 RPC 子聚合 POM 执行 `clean verify` 时，本地仓库中同版本 DDC Starter 旧构件会先于
当前源码被解析，不能构成正确的跨组件 Reactor。最终证据使用 Components 根 POM 和具体叶子模块，
确保 DDC、RPC 当前源码同时进入 Reactor。

### 4.3 Gateway

```bash
./mvnw -B -ntp -f egon-cola-components/pom.xml \
  -pl egon-cola-component-gateway/egon-cola-component-gateway-test/\
egon-cola-component-gateway-test-suite \
  -am clean verify
```

结果：PASS，27 个 Reactor 模块全部成功，Gateway suite 16/16。

干净构建额外发现 Gateway Engine 的主构件曾被 Boot 重打包覆盖，导致下游测试无法编译引用。
现已与 DDC Admin 的既有模式对齐：Admin/Engine 主构件为 thin JAR，可执行构件附加
`exec` classifier，Dockerfile 明确复制 `-exec.jar`。本次构建确认两类构件均已产出。

### 4.4 Demo 与静态交付

已执行并通过：

```bash
bash -n deployment/scripts/demo.sh \
  deployment/scripts/demo-token.sh \
  deployment/scripts/wait-ready.sh

deployment/scripts/demo.sh --help

docker compose --env-file deployment/.env.example \
  -f deployment/compose.yml \
  -f deployment/compose.demo.yml \
  config --quiet
```

同时确认 Gateway 文档中不存在已经废弃的 `VITE_GATEWAY_ADMIN_ACTOR_ID`。

## 5. 验收期间发现并修复的问题

1. RPC Mock Gateway 的默认服务名仍为旧的 `egon-internal-rpc-gateway`，与生产 Consumer 的
   `egon-gateway-rpc` 不一致。修复后 RPC 全量 suite 10/10。
2. Gateway Admin/Engine 可执行 Boot JAR 覆盖主构件，干净跨模块编译无法消费 Engine 类型。
   修复为 thin 主构件加 `exec` 可执行构件，并同步 Dockerfile；Gateway 27 模块全量通过。

这两项都由干净 Reactor 验证发现，说明增量构建或单模块测试不足以证明三组件闭环。

## 6. 未执行与剩余风险

以下项目有代码和操作入口，但本次遵守“不自动启动项目”的执行约束，没有运行：

```bash
./mvnw -B -ntp -f egon-cola-components/pom.xml \
  -pl egon-cola-component-gateway/egon-cola-component-gateway-test/\
egon-cola-component-gateway-test-suite \
  -am -Pgateway-live clean verify

cd egon-cola-components/egon-cola-component-gateway/deployment
./scripts/demo.sh doctor
./scripts/demo.sh build
./scripts/demo.sh up-control
./scripts/demo.sh init
./scripts/demo.sh up-providers
./scripts/demo.sh publish
./scripts/demo.sh up-consumer
./scripts/demo.sh verify
./scripts/demo.sh logs
./scripts/demo.sh down
```

因此仍需人工确认：

- 真实 PostgreSQL、DDC Redis、Rate Redis、Kafka 的组合行为；
- 双 Engine 独立 JVM 下的路由一致性、分布式限流、LKG 和故障转移；
- MVC/WebFlux/RPC Provider 的停止、TTL 摘除、新租约和恢复；
- Redis Sentinel/Cluster、TLS/mTLS 与 HA Compose 变体；
- 浏览器 E2E、长稳、性能和容量基线。

## 7. 设计模式与实现边界复核

发布恢复继续使用已经批准的显式 Coordinator、持久化 operation journal 和固定阶段状态机；
HTTP/RPC 接入沿用项目既有 AutoConfiguration、Adapter 和 Facade 边界。本轮没有引入通用 Saga、
2PC、Strategy 或 Chain 框架，因为阶段和变化点已经固定，新增抽象不会提高当前闭环的正确性。

## 8. 推荐人工验收顺序

1. 先执行 `demo.sh doctor` 和 `demo.sh build`，确认本机 Docker 与构件环境；
2. 执行完整 Demo 生命周期并在 `down` 前保留 `logs`；
3. 执行 `gateway-live`，重点核对双 Engine、Provider 新 lease、rollback/LKG 和分布式限流；
4. 在需要宣称生产级拓扑能力前，单独执行 Sentinel/Cluster、TLS/mTLS 和 HA 验收。
