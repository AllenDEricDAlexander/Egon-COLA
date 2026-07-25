# GWS-04 Gateway Engine Egon RPC 数据面 Spec

状态：已实现，待用户验收

父文档：`2026-07-24-gateway-component-design.md`

索引：`2026-07-25-gateway-child-spec-index.md`

依赖：GWS-01、GWS-02、GWS-03

主模块：`egon-cola-component-gateway-engine`

## 1. 目标

在 Gateway Engine 中实现生产级 Egon RPC 数据面：

- INTERNAL gRPC Listener；
- `INTERNAL_GATEWAY` 单活 Gateway Slot；
- 动态 Unary Method Handler；
- RPC Provider 发现后的 Channel 管理和转发；
- Deadline、Cancellation、Metadata、Trace 和 Status/Trailer 透传；
- PUBLIC/INTERNAL HTTP → RPC Provider 参数绑定。

生产实现不能依赖 RPC Test 的 Mock Gateway 类。

Engine 可以依赖 RPC Starter 中 GWS-02 固化的公共 Contract/Metadata/Error 类型，但
必须保持 `egon.cola.component.rpc.consumer.enabled=false`，不得创建
`RpcConsumerGatewayManager`。否则 Engine 会再次寻找 `INTERNAL_GATEWAY` 并形成错误
回路；Engine 到业务 Provider 的发现、选择和 Channel 始终由 Gateway 自己完成。

## 2. 调用场景

### 2.1 RPC Consumer → RPC Provider

```text
Egon RPC Consumer
  → DDC 发现唯一 INTERNAL_GATEWAY
  → Engine INTERNAL gRPC Listener
  → RPC Route
  → Provider Directory / Load Balancer
  → Egon RPC Provider
```

### 2.2 HTTP → RPC Provider

```text
PUBLIC / INTERNAL HTTP
  → HTTP Route
  → HTTP 参数绑定
  → Protobuf DynamicMessage
  → Provider Directory / Load Balancer
  → Egon RPC Provider
  → Protobuf Response
  → HTTP Response Mapping
```

首期不要求 RPC 入站转换到 HTTP Provider。

## 3. RPC Listener

配置：

```yaml
egon:
  cola:
    component:
      gateway:
        engine:
          rpc:
            enabled: true
            port: 9090
            max-inbound-message-bytes: 4194304
            slot:
              enabled: false
              service-name: gateway-internal
              group: default
              version: v1
```

规则：

1. RPC Listener 只属于 INTERNAL；
2. `slot.enabled=false` 时可以启动 Listener 做预热，但不注册 `INTERNAL_GATEWAY`；
3. Slot 只有在 Engine Ready、Rule Ready、Provider Directory Ready 后注册；
4. Drain 开始时先注销 Slot，再停止接收新调用；
5. RPC Listener 和 HTTP Listener 可以运行在同一个 Engine 进程；
6. RPC 端口不能与 HTTP 端口相同；
7. 首期只支持明文内网或外部提供的 TLS 终止；证书管理不属于本项目。

## 4. RPC Gateway Slot

### 4.1 DDC 身份

```text
env
+ namespace
+ INTERNAL_GATEWAY
+ serviceName
+ group
+ version
+ grpc
```

Instance：

```text
engineNodeId/instanceId
+ leaseId
+ advertisedHost
+ rpcPort
+ metadata
```

Metadata 至少包含：

```text
egon.rpc.transport=grpc
egon.rpc.serialization=protobuf
egon.rpc.runtime-version={componentVersion}
gateway.engine-version={gatewayVersion}
gateway.group-code={gatewayGroupCode}
```

活动 Release/Rule Version 不写入 Slot Metadata，避免每次发布通过换租约刷新动态字段。
该状态由 DDC Publish ACK 和 GWS-06 Engine Runtime Status 提供。

### 4.2 单活规则

RPC Consumer 当前要求同一 Service Key 恰好一个活跃实例：

- 0 个：Consumer 返回 `RPC_GATEWAY_UNAVAILABLE`；
- 多个：Consumer 返回 `RPC_GATEWAY_AMBIGUOUS`；
- 1 个：建立单 Channel。

因此首期：

1. 一个 RPC Gateway Slot 只有一个 Engine 注册；
2. 其他 Engine 可以承担 HTTP 或作为 RPC Standby，但不能注册相同 Slot；
3. 切换前等待旧租约注销或过期；
4. Admin 只做受控切换，不模拟自动选主；
5. 没有 DDC Fencing Token 前不承诺无损自动故障转移。

## 5. 动态 gRPC Handler

### 5.1 Handler Registry

Engine 提供生产实现：

```text
RpcGatewayHandlerRegistry
├── ActiveRpcMethodIndex
├── RawByteMethodDescriptorFactory
└── RpcGatewayServerCallHandler
```

Registry 使用当前 Rule Snapshot 的 RPC Method Index：

```text
fullMethodName
→ operationId
→ serviceName/group/version
→ request/response descriptor
→ policy refs
```

未知方法返回 gRPC `UNIMPLEMENTED`，不尝试反射加载业务接口。

### 5.2 Raw Byte Marshaller

RPC Consumer 已经按业务 Protobuf Descriptor 编码请求。RPC→RPC 透明代理路径：

- 接收原始 Protobuf Bytes；
- 不反序列化再序列化业务 Message；
- 不改变字段和 Unknown Fields；
- 检查消息大小；
- Provider 响应原始 Bytes 返回 Consumer。

只有需要内容级鉴权、转换或 HTTP→RPC 时才使用 Descriptor 解析
`DynamicMessage`。

### 5.3 Registry 切换

- Rule Compiler 生成完整不可变 Method Index；
- 所有 Descriptor 在激活前验证；
- 新 Index 通过原子引用切换；
- 在途 ServerCall 保持创建时捕获的旧 Index；
- 旧 Descriptor/资源在引用归零后释放；
- 不能逐 Method 原地修改活动 Map。

## 6. RPC Route

```text
RuntimeRpcRoute
├── routeId
├── operationId
├── fullMethodName
├── targetServiceName
├── targetGroup
├── targetVersion
├── requestDescriptorSha256
├── responseDescriptorSha256
├── policyRefs
└── responseMode
```

发布校验：

1. `fullMethodName` 全局唯一映射到一个 Operation；
2. Route Service Identity 与 Contract Snapshot 一致；
3. Request/Response Descriptor 可重建；
4. 只允许 Unary；
5. Provider Service 必须存在于接口目录；
6. 未知 Metadata/Policy 引用失败；
7. 同一方法重复 Route 失败。

## 7. RPC Metadata

### 7.1 入站读取

复用 RPC Component 公共 Key：

- service；
- group；
- version；
- invocation ID；
- source app/instance；
- Trace ID；
- `traceparent`；
- `tracestate`。

Engine 还必须以 gRPC Full Method Name 为协议事实，Metadata 与方法身份冲突时拒绝。

### 7.2 透传策略

- 只透传白名单 Metadata；
- 移除客户端伪造的内部身份、Provider 地址和 Gateway 决策字段；
- Trace、Invocation、Principal 由 Engine 重新写入；
- Binary Metadata 只有明确 Schema 时允许；
- 不透传 DDC Secret、Admin Token 或任意 `authorization` 到 Provider，除非安全
  Mapper 显式允许；
- Metadata 总大小受限。

### 7.3 Invocation ID

- Consumer 已提供合法 Invocation ID 时保留；
- 缺失时 Engine 使用 UUIDv7 生成；
- 每次重试 Attempt 拥有独立 Attempt ID，但共享 Invocation ID；
- Provider 日志与 Kafka Event 同时记录二者。

## 8. Provider Channel

### 8.1 Cache Key

```text
env + namespace + serviceKey + instanceId + leaseId + host + port + secure
```

`leaseId` 必须进入 Key，避免复用旧实例的 Channel。

### 8.2 生命周期

1. 首次选择实例时按需创建 ManagedChannel；
2. 并发创建使用 Single-Flight；
3. Provider 摘除后立即停止新选择；
4. Channel 进入 Drain，在途调用允许有界完成；
5. Drain 超时后 ShutdownNow；
6. 新租约即使地址相同也创建新 Channel；
7. Engine 停止时关闭全部 Channel。

Channel Cache 不属于 RPC Component。

## 9. Unary Forwarder

处理顺序：

1. 验证 Method 与 Metadata；
2. 创建 Gateway Exchange/Context；
3. 执行 Exposure/Security/Governance；
4. 选择 Provider；
5. 获取 Provider Channel；
6. 构造相同 Full Method Name 的动态 Unary Method Descriptor；
7. 计算有效 Deadline；
8. 写入受控 Metadata；
9. 发起 Provider Call；
10. 转发 Message、Headers、Status、Trailers；
11. 完成观测与资源释放。

Forwarder 不自行选择 Provider，也不自行解析 Admin 规则。

## 10. Deadline 与 Cancellation

有效 Deadline：

```text
min(inbound remaining deadline, route timeout, engine maximum timeout)
```

规则：

- 入站已经超时则不调用 Provider；
- 必须给网关处理和响应预留最小预算；
- Provider Call 使用剩余预算；
- Consumer 取消立即取消 Provider Call；
- Provider 取消/超时映射回 Consumer；
- Engine Drain 超时可以取消剩余调用；
- 取消不触发默认重试；
- 不吞掉 gRPC Context Cancellation。

## 11. Status 与 Trailer

### 11.1 透明优先

Provider 标准 gRPC Status 和受允许 Trailer 原样转发：

- `INVALID_ARGUMENT`
- `NOT_FOUND`
- `ALREADY_EXISTS`
- `FAILED_PRECONDITION`
- `RESOURCE_EXHAUSTED`
- `UNAVAILABLE`
- `DEADLINE_EXCEEDED`
- `INTERNAL`

### 11.2 Gateway 错误

| Gateway Error | gRPC Status |
|---|---|
| Route Not Found | `UNIMPLEMENTED` |
| External Not Accessible | `PERMISSION_DENIED`，仅内部诊断；RPC Listener 本身不对外 |
| Authentication Failed | `UNAUTHENTICATED` |
| Authorization Denied | `PERMISSION_DENIED` |
| Rate Limited | `RESOURCE_EXHAUSTED` |
| No Provider | `UNAVAILABLE` |
| Circuit Open | `UNAVAILABLE` |
| Gateway Deadline | `DEADLINE_EXCEEDED` |
| Invalid Metadata/Message | `INVALID_ARGUMENT` |
| Internal Error | `INTERNAL` |

Gateway Trailer 只包含稳定 Error Code、Trace ID 和可公开重试提示。

## 12. HTTP → RPC

### 12.1 Contract

Rule Snapshot 必须包含 GWS-02 的 Protobuf `FileDescriptorSet` 与 SHA-256。Engine 在
激活时重建 Descriptor，不能在首个请求时下载业务 JAR。

### 12.2 参数映射

Route 明确配置：

```text
HTTP Path/Query/Header/Body
→ Protobuf Field Path
```

规则：

- JSON Body 默认按 Protobuf JSON Mapping；
- Path/Query/Header 覆盖关系在发布时固定；
- Enum 使用名称，是否接受数字由 Route 明确；
- `bytes` 使用 Base64；
- Timestamp/Duration 使用 Protobuf JSON 标准；
- Oneof 冲突失败；
- Unknown Field 默认拒绝；
- Required 业务约束由上报 Schema/规则校验。

### 12.3 响应

- Protobuf Response 可按 Protobuf JSON Mapping 返回；
- 或按 Route 选择统一 `ResultDto`；
- Binary Protobuf HTTP 响应需要显式 Content-Type；
- Provider gRPC Status 映射为 GWS-03 HTTP Error。

## 13. 重试边界

RPC Component Consumer 默认关闭重试，Gateway 也默认不重试 RPC。

只有 GWS-07 明确满足以下条件才允许：

- Operation 标记幂等；
- 未收到可证明成功的 Provider 响应；
- 剩余 Deadline 足够；
- Attempt 上限明确；
- 不对业务 `INVALID_ARGUMENT`、`PERMISSION_DENIED` 等重试；
- Kafka Event 记录每个 Attempt。

## 14. 运行状态

RPC 子系统状态：

```text
DISABLED
STARTING
LISTENING_NOT_REGISTERED
REGISTERED_READY
DRAINING
FAILED
STOPPED
```

Admin 节点页面分别展示：

- Listener 是否监听；
- Slot 是否注册；
- DDC leaseId/过期时间；
- 当前 RPC Route Version；
- Provider Directory 状态；
- Channel 数量和 Drain 数量；
- 最近错误。

## 15. 测试设计

### 15.1 Contract

- 使用 RPC Test Proto；
- 验证 Full Method Name、Descriptor SHA；
- 未知方法、重复方法、Streaming 拒绝。

### 15.2 真实 gRPC

- 真 Consumer → 真 Engine → 真 Provider；
- 多 Provider 由 Engine 选择；
- Provider 摘除关闭 Channel；
- 新 leaseId 不复用旧 Channel；
- Metadata、Trace、Deadline、Cancellation；
- Provider Headers/Status/Trailers；
- 大消息限制；
- 客户端中途取消。

### 15.3 Slot

- 0 Slot Consumer 快速失败；
- 1 Slot 调用成功；
- 2 Slot Consumer Ambiguous；
- Drain 先注销后停 Listener；
- 旧租约过期后受控切换。

### 15.4 HTTP → RPC

- Path/Query/JSON → DynamicMessage；
- Enum、bytes、Timestamp、Oneof；
- Unknown Field 和 Schema 错误；
- Protobuf Response → JSON；
- gRPC Status → HTTP Error。

## 16. 验收标准

1. 生产 Gateway 不引用 RPC Test 包；
2. RPC 入站只支持 Unary；
3. Consumer 只连接唯一 `INTERNAL_GATEWAY`；
4. Provider Directory 与负载均衡只在 Engine；
5. RPC→RPC 透明路径不重复序列化 Protobuf；
6. Deadline、Cancellation、Status 和 Trailer 端到端正确；
7. Channel Cache 以 leaseId 隔离并正确 Drain；
8. Rule 切换不会产生半更新 Method Registry；
9. HTTP→RPC 不依赖业务接口 JAR；
10. Slot 未 Ready、多个 Slot 或无 Slot 都有稳定失败语义。

## 17. 本轮审核项

1. 认可同一 Engine 增加独立 INTERNAL gRPC Listener；
2. 认可 RPC Gateway Slot 首期单活和受控切换；
3. 认可 Raw Byte Unary 透明转发；
4. 认可 Channel Cache 属于 Gateway Engine；
5. 认可 HTTP→RPC 使用 FileDescriptorSet/DynamicMessage；
6. 认可 RPC 默认不重试；
7. 认可 gRPC Status/Trailer 透明优先和统一 Gateway Error 映射。
