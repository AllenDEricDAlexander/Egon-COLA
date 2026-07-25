# GWS-05 Provider 发现、健康与负载均衡 Spec

状态：已实现，待用户验收

父文档：`2026-07-24-gateway-component-design.md`

索引：`2026-07-25-gateway-child-spec-index.md`

依赖：GWS-01、GWS-02

主模块：

- `egon-cola-component-gateway-engine`
- `egon-cola-component-gateway-provider-runtime`
- `egon-cola-component-gateway-admin`

## 1. 目标

建立统一 Provider Runtime 模型：

- HTTP Provider 通过独立 Runtime 注册 DDC；
- RPC Provider 继续由 RPC Component 注册；
- Engine 通过 DDC 服务目录和实例订阅维护本地 Provider Directory；
- Engine 根据 Route、Metadata、健康和 Admin Rule 选择候选；
- Engine 执行轮询、加权轮询、随机和最少在途请求。

Engine 不从 Gateway Admin 获取静态 Provider 地址。

## 2. Provider 身份

### 2.1 Service Key

统一映射为：

```text
ProviderServiceKey
├── env
├── namespace
├── protocolType = HTTP | RPC
├── serviceName
├── group
├── version
└── transport = http | https | grpc
```

DDC 对应：

- HTTP：`DdcServiceKind.HTTP_PROVIDER`；
- RPC：`DdcServiceKind.RPC_PROVIDER`。

### 2.2 Instance

```text
ProviderInstance
├── serviceKey
├── instanceId
├── leaseId
├── host
├── port
├── secure
├── metadata
├── leaseExpireAt
├── registryState
├── observedHealth
└── statistics
```

`instanceId + leaseId` 才是一次运行实例。相同 instanceId 获得新 leaseId 时视为新实例。

## 3. HTTP Provider Runtime

### 3.1 职责

`gateway-provider-runtime` 只负责：

1. 等待 Spring Boot HTTP Server Ready；
2. 解析可对外注册地址；
3. 注册 DDC `HTTP_PROVIDER`；
4. 定时心跳；
5. 租约失效时重新注册；
6. 优雅停止时注销。

不负责：

- 扫描接口；
- 上报接口定义；
- 拦截接口调用；
- Kafka；
- Route 或负载均衡；
- Gateway Engine Node 注册。

### 3.2 配置

```yaml
egon:
  cola:
    component:
      gateway:
        provider-runtime:
          enabled: false
          service-name: order-service
          group: default
          version: v1
          protocol: http
          host: 10.0.0.12
          port: 8080
          lease-seconds: 30
          heartbeat-interval-seconds: 10
          fail-fast: true
          metadata:
            gateway.zone: az-a
            gateway.weight: "100"
```

规则：

- 默认关闭，业务应用显式开启；
- Host 必须是 Engine 可访问地址；
- 禁止默认注册 `0.0.0.0`、Loopback 或随机测试地址到非 local 环境；
- `port=0` 仅测试允许，Ready 后解析真实端口；
- heartbeat interval 小于 lease seconds；
- Registry 失败是否阻止应用 Ready 由 fail-fast 配置决定，生产默认阻止；
- 同时安装 GWS-10 Starter 时，Runtime 读取公共
  `GatewayDefinitionIdentity` Bean，自动补充 Definition Set/Artifact/Build
  Metadata；
- 显式 Metadata 与该 Bean 的稳定身份冲突时启动失败，不能静默覆盖；
- 该可选读取不改变职责：Runtime 仍不扫描或上报接口，Starter 仍不维护租约。

### 3.3 生命周期

```text
NEW → WAITING_SERVER → REGISTERING → REGISTERED → RECOVERING → STOPPED
                               └────→ FAILED
```

同一实例在租约有效时不重复注册；`NOT_FOUND` 或 `LEASE_MISMATCH` 时创建新租约。

## 4. Engine DDC Adapter

Core 定义：

```java
public interface ProviderServiceRegistry {
    ProviderCatalogSnapshot getServiceKeys(ProviderQuery query);
    ProviderSnapshot getInstances(ProviderServiceKey key);
    ProviderSubscription subscribeServices(ProviderQuery query, ProviderCatalogListener listener);
    ProviderSubscription subscribe(ProviderServiceKey key, ProviderSnapshotListener listener);
}
```

DDC Adapter：

- 把 `DdcServiceKey` 映射为 `ProviderServiceKey`；
- 把 DDC 实例映射为不可变 `ProviderInstance`；
- 使用 `subscribeServices` 发现新增/删除 Service Key；
- 对每个被 Route 引用的 Service Key 建立实例订阅；
- 依赖 DDC 现有周期 Reconcile；
- 不把 DDC 类型泄漏到 Core。

## 5. Provider Directory

### 5.1 结构

```text
ProviderDirectory
└── AtomicReference<ProviderDirectorySnapshot>
    └── Map<ProviderServiceKey, ProviderServiceSnapshot>
        └── List<ProviderInstance>
```

每次 DDC 更新构建新的不可变 Service Snapshot，再原子替换；请求线程不锁住 DDC
Listener。

### 5.2 订阅范围

- Engine 只订阅当前 Rule Snapshot 引用的 Provider Service；
- Rule 新增 Service：先完成 DDC 初始查询和订阅，再允许 Rule 激活；
- Rule 删除 Service：停止新选择，等待 Channel/连接 Drain 后取消订阅；
- 同一 Service 被多个 Route 引用时共享订阅；
- 订阅引用计数必须可观测。

### 5.3 启动

Engine Ready 条件：

- Rule 所有必需 Provider Service 已完成首次查询；
- “首次查询成功但 0 实例”与“DDC 查询失败”分开表示；
- 是否允许 0 实例 Route 激活由发布策略决定，默认允许配置生效但请求快速 503；
- Engine 不把 Provider Host/Port 持久化为可跨重启复用的静态 LKG Directory；
- 冷启动时 DDC 完全不可达，即使 Rule LKG 有效也不 Ready；
- 已运行节点只可继续使用当前内存中尚未到 `leaseExpireAt` 的实例。

## 6. 注册状态与业务健康

### 6.1 三个维度

| 维度 | 来源 | 含义 |
|---|---|---|
| Registry State | DDC Lease | 实例仍在注册 |
| Active Health | Engine 主动探测 | 健康端点/协议探测结果 |
| Passive Health | 实际调用 | 连续连接/调用失败与恢复 |

DDC Lease 有效不等于业务接口健康。

### 6.2 首期策略

- 有效 DDC Lease 是进入候选的必要条件；
- Passive Health 默认启用；
- HTTP 可按 Service 配置主动 Health Path；
- RPC 可使用 gRPC Health Checking Protocol；Provider 未实现时不强制；
- 主动探测在独立有界 Scheduler 上运行；
- 单个 Engine 的观测只影响本地选择，不反写 DDC 租约；
- Admin 展示 DDC 状态与 Engine 观测状态，不能合并成一个“在线”字段。

### 6.3 Passive Ejection

实例满足以下条件时本地临时摘除：

- 连续可重试连接失败达到阈值；
- 短窗口失败率超过阈值且样本量足够；
- Circuit Breaker 打开。

不因业务 `4xx`、gRPC `INVALID_ARGUMENT` 或授权拒绝摘除 Provider。

摘除有最大时间并允许 Half-Open 探测。DDC Lease 消失时立即永久移出当前 Snapshot。

## 7. Metadata 与 Admin 覆盖

标准字段：

- `gateway.zone`
- `gateway.region`
- `gateway.weight`
- `gateway.tags`
- `gateway.protocol-version`
- `gateway.management-path`
- `gateway.definition-set-id`
- `gateway.artifact-version`
- `gateway.build-id`

最终值优先级：

```text
Admin Instance Override
> Admin Service Rule
> Provider Metadata
> System Default
```

规则：

- Admin Override 以 `serviceKey + instanceId` 为键，不以 Host 为键；
- 新 leaseId 继承同 instanceId 的管理覆盖，但页面标记为新运行实例；
- Definition Set/Artifact/Build 只用于接口目录关联和诊断，不参与候选过滤或权重计算；
- 非法 Metadata 不进入候选并产生诊断；
- 未知标签不自动匹配；
- Secret 不允许作为标签。

## 8. 候选过滤

按固定顺序：

1. 精确 Service Key；
2. DDC Lease 有效；
3. Protocol/secure 与 Route 一致；
4. Admin enabled；
5. Zone/Region/Tag 条件；
6. Active/Passive Health；
7. Circuit/Bulkhead 可用；
8. 权重大于 0。

每一步记录候选数量，但指标不能使用 instanceId 作为默认高基数标签。

0 候选返回 `GATEWAY_PROVIDER_UNAVAILABLE`，不回退静态地址或其他版本。

## 9. Load Balancer SPI

```java
public interface ProviderLoadBalancer {
    ProviderInstance select(ProviderSelectionContext context,
                            List<ProviderInstance> candidates);
}
```

Context：

- serviceKey；
- operationId；
- routeId；
- trace/request ID；
- candidate runtime statistics；
- policy config。

### 9.1 Round Robin

- 每个 `serviceKey + policyVersion` 独立游标；
- Snapshot 变化后安全归一化游标；
- 候选按稳定 instanceId/leaseId 排序，避免 DDC 返回顺序改变行为；
- 权重不参与普通轮询。

### 9.2 Smooth Weighted Round Robin

- 使用平滑加权轮询；
- 权重范围 1～10000；
- 权重变化创建新 Policy State；
- 不允许整数溢出；
- 权重 0 等同禁用，不进入候选。

### 9.3 Random

- 对候选做均匀随机；
- 支持测试注入确定性 Random；
- 不用于需要顺序粘性的场景；
- 安全随机不是必需，使用高性能伪随机。

### 9.4 Least In-Flight

- 选择当前本 Engine 在途请求最少的实例；
- 平局按 Round Robin 打散；
- 在调用开始前递增，成功、失败、取消均必须递减；
- 计数不能小于 0；
- 这是单 Engine 局部视图，不宣称全 Group 全局最少连接。

### 9.5 首期不做

- 一致性 Hash；
- Session Affinity；
- EWMA 延迟；
- 跨 Engine 全局在途统计；
- Consumer 侧负载均衡。

## 10. 选择与资源生命周期

选择结果：

```text
ProviderSelection
├── serviceKey
├── instanceId
├── leaseId
├── algorithm
├── policyVersion
└── diagnosticReason
```

- 每次 Attempt 产生独立 Selection；
- HTTP/RPC Adapter 只接受 Selection，不重新选择；
- Instance 被摘除后已有在途请求按协议完成/取消；
- 新请求绝不能复用已摘除实例的连接；
- 资源释放与 Directory Snapshot 更新解耦，通过 Drain 管理。

## 11. DDC 故障

### 11.1 短时不可用

- 保留最后一次 Directory Snapshot；
- 标记 Registry State 为 STALE；
- 继续使用尚未明确过期的租约；
- 超过本地租约截止时间后停止选择；
- 不无限延长实例存活。

### 11.2 Redis 重启

- Provider Runtime/RPC Component 重新注册新租约；
- Engine 周期 Reconcile 得到新 Catalog；
- 旧 leaseId Channel Drain；
- 新 leaseId 创建新 Channel；
- Admin 展示 Registry 恢复过程。

## 12. Admin 投影

Engine 以低频状态报告或管理查询提供：

- Service Key；
- DDC Instance/Lease；
- Registry 状态；
- Metadata 与 Admin Override；
- Active/Passive Health；
- 本地摘除截止时间；
- 在途数量；
- 最近成功/失败时间；
- 当前 Load Balancer Policy。

该投影用于管理和诊断，不进入请求热路径。

## 13. 配置

```text
provider-runtime.enabled
provider-runtime.service-name
provider-runtime.group
provider-runtime.version
provider-runtime.protocol
provider-runtime.host
provider-runtime.port
provider-runtime.lease-seconds
provider-runtime.heartbeat-interval-seconds
engine.discovery.reconcile-interval
engine.discovery.stale-grace-period
engine.health.passive.*
engine.health.active.*
```

stale grace period 不能超过 DDC 租约剩余时间。

## 14. 测试设计

### 14.1 Runtime

- HTTP Provider 真实端口注册；
- 心跳、注销、租约失效重注册；
- 非法 Host/Port/Metadata；
- Starter 未安装时仍可独立注册；
- Runtime 未安装时 Starter 仍可独立上报。

### 14.2 Directory

- Catalog 新增/删除 Service；
- 多实例上线、下线、新 leaseId；
- Pub/Sub 丢失由 DDC Reconcile 修复；
- Rule 引用计数和取消订阅；
- DDC 短时不可用、租约过期、Redis 重启。

### 14.3 Load Balance

- Round Robin 确定顺序；
- Smooth Weighted 分布；
- Random 使用确定 Seed；
- Least In-Flight 增减和取消；
- Candidate Filter 每个步骤；
- 0 Candidate 稳定失败；
- 并发 Snapshot 替换。

### 14.4 Health

- 连接失败临时摘除；
- 业务错误不摘除；
- Half-Open 恢复；
- Active Health 不阻塞 EventLoop；
- 不把本地 Health 写成 DDC Lease。

## 15. 验收标准

1. HTTP/RPC Provider 都通过 DDC 发现；
2. HTTP Runtime 与 Gateway Starter 职责独立；
3. Engine 不消费 Admin 静态 Provider 地址；
4. Directory 使用不可变 Snapshot 原子更新；
5. Rule 激活前完成新增 Service 首次查询；
6. DDC Lease、主动健康和被动健康分别建模；
7. 四种负载均衡算法结果可重复验证；
8. Least In-Flight 只声明本 Engine 局部语义；
9. 旧 leaseId 的 HTTP/RPC 资源能够 Drain；
10. DDC 故障不会无限使用已过期 Provider。

## 16. 本轮审核项

1. 认可独立 HTTP Provider Runtime；
2. 认可 Service Key 与 `instanceId + leaseId` 身份；
3. 认可三维健康模型和 Passive Ejection；
4. 认可 Metadata/Admin Override 优先级；
5. 认可固定 Candidate Filter 顺序；
6. 认可 Round Robin、Smooth Weighted、Random、Least In-Flight；
7. 认可 DDC 故障只保留未明确过期的最后目录。
