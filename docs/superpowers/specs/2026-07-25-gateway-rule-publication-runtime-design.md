# GWS-06 Rule Snapshot、DDC 发布与 Engine 运行态 Spec

状态：草案，等待审核

父文档：`2026-07-24-gateway-component-design.md`

索引：`2026-07-25-gateway-child-spec-index.md`

依赖：GWS-01、GWS-02

主模块：

- `egon-cola-component-gateway-admin`
- `egon-cola-component-gateway-engine`
- `egon-cola-component-gateway-contract`

## 1. 目标

定义从 Admin 编辑态规则到 Engine 原子运行态的完整闭环：

```text
Admin Draft
→ Validate
→ Compile Rule Snapshot
→ Persist Management Release
→ DDC SYNC_ALL_ACK Publish
→ Engine Validate/Compile/Persist/Activate
→ Exact ACK
→ Admin Release Result
```

上一版“DB + Redis + Redis 消息”的要求由 DDC 承接。Gateway Admin 不直接操作
DDC DB/Redis。

## 2. 范围

- 编辑态与运行态分离；
- Gateway Group 级 Rule Snapshot；
- Snapshot Schema、Canonicalization 和 Checksum；
- DDC Config Coordinate；
- 发布 Target、ACK、失败、超时、UNKNOWN 和重试；
- Engine Rule Compiler、原子激活和磁盘 LKG；
- 历史、差异和回滚；
- 大 Snapshot 的 Activation/Chunk 机制；
- 节点版本分歧与收敛。

不包含具体 Admin 表字段、页面交互或各治理算法内部实现。

## 3. 规则作用域

规则可以作用于：

```text
GLOBAL
GATEWAY_GROUP
APPLICATION
BUSINESS_DOMAIN
ENTITY_DOMAIN
INTERFACE_GROUP
OPERATION
ROUTE
CALLER
PROVIDER_SERVICE
PROVIDER_INSTANCE
```

继承优先级从具体到全局。相同作用域多条互斥规则必须在发布时拒绝，不允许 Engine
按数据库返回顺序决定。

## 4. 管理态模型

### 4.1 Draft

Draft 包含：

- 基础版本 `baseRevision`；
- Route Assignment；
- Exposure；
- Security Policy Ref；
- Governance Policy Ref；
- Provider Selection/Load Balance；
- Response Mapping；
- CORS；
- 启停状态；
- 编辑人和变更说明。

使用乐观锁：

- 更新必须携带 `baseRevision`；
- Revision 不匹配返回冲突；
- 不进行最后写入覆盖；
- 一个 Gateway Group 可以多人读取，但同一 Draft Revision 只有一个成功写入者。

### 4.2 Release

Release 是不可变管理事实：

```text
GatewayRelease
├── releaseId
├── gatewayGroupId
├── sourceDraftRevision
├── ruleSchemaVersion
├── ruleContentSha256
├── artifactSha256
├── artifactSize
├── changeId
├── status
├── operator
├── reason
├── createdAt
└── completedAt
```

Release 创建后不修改 Snapshot 内容，只更新发布状态和诊断投影。

## 5. Rule Snapshot

### 5.1 Envelope

```text
GatewayRuleSnapshot
├── ruleSchemaVersion = "v1"
├── releaseId
├── generatedAt
├── ruleContentSha256
├── artifactSha256
└── content
    ├── gatewayGroupId
    ├── gatewayGroupCode
    ├── env
    ├── namespace
    ├── operations[]
    ├── routes[]
    ├── providerPolicies[]
    ├── trafficPolicies[]
    ├── securityPolicies[]
    ├── corsPolicies[]
    └── rpcDescriptors[]
```

Snapshot 不包含：

- 数据库主键以外的 Entity；
- DDC/Kafka/数据库 Secret；
- Provider 静态 Host/Port；
- Engine Node 地址；
- 页面展示状态；
- 未发布 Draft；
- 请求运行统计。

### 5.2 Runtime Operation

Operation 只保留数据面所需字段：

- Operation ID/Key；
- Protocol；
- HTTP/RPC Method Identity；
- 参数/响应 Schema；
- `externalAccessible`；
- Provider Service Key；
- Response Mode；
- Policy References；
- 废弃状态。

描述、负责人等纯管理字段不进入 Snapshot，避免不必要发布。

### 5.3 Canonicalization

先只对 `content` 计算 `ruleContentSha256`：

1. UTF-8；
2. Object Key 按字典序；
3. List 按定义的稳定 Key 排序；
4. 不输出 Null 与默认值的规则固定；
5. Decimal 使用规范字符串；
6. 不包含 releaseId、generatedAt、Operator 和任何发布过程字段；
7. 同一运行语义必须产生完全相同的 Content 字节和 Hash。

再把 Release Metadata、Content 和 `ruleContentSha256` 序列化为完整 Snapshot，对完整
字节计算 `artifactSha256`。新的回滚 Release 可以保持相同 `ruleContentSha256`，但因
releaseId/generatedAt 不同而拥有新的 `artifactSha256`。

Engine 对收到的 Content 和完整 Artifact 分别重新计算 SHA-256。

## 6. 发布前校验

### 6.1 结构

- Schema Version 支持；
- 所有 ID/Code 唯一；
- 引用完整；
- Snapshot 字节数符合上限；
- RPC Descriptor SHA 正确；
- 未知 Enum/Policy 拒绝。

### 6.2 Route

- Host/Method/Path 无歧义；
- PUBLIC Route 只能引用 `externalAccessible=true`；
- Provider Protocol 与 Operation 一致；
- HTTP→RPC 参数映射可编译；
- 不允许静态 Provider URL；
- 禁用 Operation 不进入活动 Route。

### 6.3 Policy

- 作用域合法；
- 阈值、超时、权重和重试边界合法；
- Security Provider ID 存在；
- 配置了鉴权但 Provider 缺失时失败；
- 分布式限流 Redis Key 模板合法；
- Retry 只引用可重试 Operation。

### 6.4 Target

- DDC Config Coordinate 已建立；
- 默认至少一个已注册、可执行 Rule Apply 的 Engine Config Client；
- 目标 Engine 支持当前 Rule Schema；
- Group/Env/Namespace 一致。

`CONFIG_APPLY_READY` 与业务流量 `READY` 不同：新 Engine 必须先初始化 Applier、
Compiler 和本地数据目录，再注册 DDC Config Client；它可以在尚无首个 Rule、业务
Readiness=false 时成为初始发布 Target，避免启动死锁。

## 7. DDC Config Coordinate

每个 Gateway Group 使用独立 scope：

```text
appCode   = gateway-engine-{gatewayGroupCode}
env       = {env}
namespace = {namespace}
```

统一激活配置：

```text
configKey = gateway.rules.active
```

该 appCode 只供对应 Group Engine 使用，避免 DDC `SYNC_ALL_ACK` 把其他 Group
实例固化为 Target。

`gateway.rules.active` 是唯一激活键，无论规则大小都只通过它推进运行版本。激活值为：

```text
GatewayRuleActivation
├── activationSchemaVersion = "v1"
├── releaseId
├── mode = INLINE | CHUNKED
├── ruleSchemaVersion
├── totalSize
├── ruleContentSha256
├── artifactSha256
├── inlineSnapshot          # INLINE 时存在
└── chunks[]                # CHUNKED 时存在
    ├── configKey
    ├── index
    ├── size
    └── sha256
```

禁止为小/大 Snapshot 使用两个独立激活键。DDC 对
`gateway.rules.active` 的单 Key Version 是 Engine 判断新旧激活指令的唯一顺序。

## 8. 小 Snapshot 发布

当 Canonical Snapshot 小于 Gateway 上限时：

1. Admin 生成 UUIDv7 `changeId`；
2. 在 Gateway DB 保存 Release 和完整 Snapshot；
3. 构造 `mode=INLINE` 的 `GatewayRuleActivation`；
4. 通过 DDC Management Client Upsert `gateway.rules.active`；
5. 调用 DDC Publish，传入 expectedVersion；
6. DDC 固化当前 Config Client `instanceId + leaseId`；
7. DDC DB/Redis 保存版本并发送 full-value Pub/Sub；
8. Engine 从 Inline 字段校验并 Apply；
9. DDC 等待全部 Target 成功或终态；
10. Admin 记录 DDC 结果。

Gateway 默认 Snapshot 上限 512 KiB，低于 DDC 默认 1 MiB，给 Envelope、Target 和
协议开销保留空间。

## 9. 大 Snapshot Activation/Chunk

### 9.1 触发

Canonical Snapshot 大于 512 KiB 时使用不可变 Chunk：

```text
gateway.rules.chunk.{releaseId}.{index}
gateway.rules.active
```

单 Chunk UTF-8 字节数不超过 256 KiB。

### 9.2 Chunked Activation

使用第 7 节同一个 `GatewayRuleActivation`，设置 `mode=CHUNKED`，不包含
`inlineSnapshot`，并列出全部 Chunk。它仍发布到唯一
`gateway.rules.active`，不创建第二个激活 Key。

### 9.3 发布顺序

1. 生成全部不可变 Chunk；
2. 逐个通过 DDC `SYNC_ALL_ACK` 发布 Chunk；
3. Engine Chunk Applier 只校验并写入本地 Staging，不激活；
4. 所有 Chunk 对全部固化 Target 成功后发布 CHUNKED Activation；
5. Engine Active Applier 加载、排序、校验全部 Chunk；
6. 重建完整 Snapshot，编译、持久化并原子激活；
7. Active Apply 成功后 ACK；
8. Active 发布成功才表示 Release 成功。

任何 Chunk 失败都不发布 Activation，当前活动版本不变。

从 INLINE 切换到 CHUNKED，或从 CHUNKED 切换到 INLINE，均只增加
`gateway.rules.active` 的 Version，因此 Pub/Sub、周期校准和启动全量拉取不会因为
两个激活键乱序而回退。

### 9.4 清理

- 保留当前和最近两个成功 Release 的 Chunk；
- 失败/超时 Release 的 Chunk 保留诊断期后清理；
- 清理通过 DDC Management API 删除 Config；
- 删除前确认没有当前 Active 或正在发布/允许 Retry 的 Activation 引用；
- 更早的历史 Release 仍可回滚，但回滚从 Gateway DB 的完整 Snapshot 创建新 Release
  和新 Chunk Key，不直接复用已清理 Key；
- Engine 本地 Staging 使用同样保留策略。

## 10. Engine Rule Apply

### 10.1 阶段

```text
RECEIVED
→ CHECKSUM_VERIFIED
→ SCHEMA_VALIDATED
→ COMPILED
→ DURABLE_STAGED
→ ACTIVE_POINTER_WRITTEN
→ MEMORY_ACTIVATED
→ ACK_SUCCESS
```

失败停在当前阶段，返回诊断并保留旧版本。

### 10.2 Rule Compiler

Compiler 输出：

```text
CompiledGatewayRules
├── RouteIndex
├── RpcMethodIndex
├── ProviderSubscriptionPlan
├── TrafficPolicyIndex
├── SecurityPolicyIndex
├── CorsPolicyIndex
└── ResourcePreparationPlan
```

编译不在 EventLoop 上运行。

### 10.3 资源准备

激活前：

- 新 Provider Service 完成首次查询；
- RPC Descriptor 可重建；
- Filter/Strategy 实现存在；
- Redis 限流连接配置有效；
- 引用的 Auth Provider 已安装；
- 需要的 Channel/Client Factory 可创建。

不要求预建所有 Provider 连接，但必须证明配置可执行。

## 11. 磁盘 LKG

目录：

```text
{engine-data-dir}/rules/{gatewayGroupCode}/
├── releases/{releaseId}.json
├── releases/{releaseId}.sha256
└── active
```

`.sha256` 保存完整 Snapshot 的 `artifactSha256`；Snapshot 内仍包含并校验
`ruleContentSha256`。

写入顺序：

1. 写临时 Release 文件；
2. 校验并 fsync；
3. 原子 rename 为正式 Release；
4. 写临时 active pointer；
5. fsync 并原子 rename；
6. 原子替换内存 Rule Reference；
7. 返回成功 ACK。

Rule 不含 Secret，因此不强制加密；目录权限必须限制。磁盘失败时不激活、不 ACK
成功。

### 11.1 启动恢复

1. 优先从 DDC 全量拉取；
2. DDC 不可用时读取 active LKG；
3. 校验 SHA、Schema 和 Engine 兼容；
4. LKG 有效只表示 Rule 可以加载，不代表 Provider Directory 已可执行；
5. 冷启动无法从 DDC 完成 Provider 首次查询时保持 Not Ready；
6. 已运行节点 DDC 中断时，可在内存中未过期 Provider Lease 范围内 Degraded Ready；
7. 无有效 DDC Rule 且无 LKG 时不 Ready；
8. DDC 恢复后周期校准新版本和 Provider Directory。

全量拉取不依赖 Config Key 排序：若 CHUNKED Activation 先于 Chunk 到达，Active
Applier 返回失败且不推进本地 Active Version；Chunk 完成 Staging 后，由后续周期校准
重新 Apply 同一远端 Active Version。此期间继续使用 LKG 或保持 Not Ready。

## 12. 单节点与 Group 一致性

### 12.1 单节点

Engine 内存切换是原子的：一个请求只看到完整旧版或完整新版。

### 12.2 Gateway Group

DDC `SYNC_ALL_ACK` 确认所有固化 Target 最终返回成功，但不提供分布式同时切换：

- 可能有节点先成功、新旧版本短时并存；
- 任一节点失败，任务整体 FAILED；
- Admin 必须展示节点版本分歧；
- 重试固定原 Target，不重新取当前在线实例；
- 已换租约的原 Target 导致重试失败；
- 收敛可以使用新 Release 回滚或重新发布。

页面和 API 不得把 `PUBLISHING` 描述为全 Group 已生效。

## 13. 发布状态

```text
CREATED
VALIDATING
READY
PUBLISHING
SUCCESS
FAILED
TIMEOUT
UNKNOWN
SUPERSEDED
```

- `CREATED`：Release 身份和不可变输入已建立，尚未完成校验；
- `SUCCESS`：DDC 全部 Target ACK 成功；
- `FAILED`：校验、准备、分发或任一 Target Apply 失败；
- `TIMEOUT`：截止时间未收齐成功 ACK；
- `UNKNOWN`：DDC Admin 在活跃任务期间重启；
- `SUPERSEDED`：一个原 SUCCESS Release 被后续 SUCCESS Release 替代。

每个 Publish Attempt 的终态不可修改。FAILED/TIMEOUT/UNKNOWN 重试时，在同一
Release/Change Task 下新增 `attemptNo`，Release 聚合状态重新进入 PUBLISHING，并可
根据新 Attempt 进入 SUCCESS 或新的失败终态；旧 Attempt 和 Target 结果必须完整保留。

如果部分 Target 已激活而当前 Attempt 不是 SUCCESS，通过 Target 计数和
`partialApplied=true` 诊断字段表达，不创建另一套 Release 终态。

## 14. 幂等与并发

- 同一 Gateway Group 同一时刻只有一个活动发布；
- `changeId` 全局唯一；
- 重复 publish 相同 changeId 返回原任务；
- retry 只新增 Attempt，不修改旧 Attempt/Target；
- 存在更新的 Release，或 DDC Active Version 已前进时，禁止重试旧 Release；需要恢复
  旧内容时走新回滚 Release；
- expectedVersion 防止覆盖更新后的 DDC Config；
- Admin 发布锁使用数据库唯一约束/乐观状态更新；
- 不使用 JVM 本地锁作为跨实例事实；
- 回滚也是新 releaseId/changeId；
- 不修改历史 Release 内容。

## 15. 回滚

1. 用户选择一个历史成功 Release；
2. Admin 重新执行当前校验；
3. 使用历史内容生成新的 releaseId；
4. 重新计算 Snapshot/Activation；
5. 通过正常 DDC 发布；
6. 全部成功后标记新 Release SUCCESS；
7. 审计记录来源 Release。

不直接修改 DDC 当前版本指针或 Engine active 文件。

## 16. Engine 状态反馈

Engine 在独立 Management Listener 暴露只读接口：

```text
GET /api/v1/gateway/internal/runtime/status
```

Gateway Admin 从 DDC Config Client 投影取得 Engine 的管理 Host/Port，再查询该接口。
Config Client 注册的 Host/Port 必须是 Admin 可达的管理地址，不使用 PUBLIC Listener
地址。

状态报告：

- active releaseId/DDC Active Version/Rule Schema/两个 SHA；
- LKG releaseId；
- DDC Config Client instanceId/leaseId；
- last Apply stage/result/error；
- Route/Operation/Provider Service 数量；
- Staging Chunk 数量；
- Readiness/Degraded；
- Listener 状态。

Gateway Admin 将该状态与 DDC Publish Target ACK 关联，但不以 Engine 自报替代 DDC
同步发布结果。

安全和正确性约束：

- 接口只绑定管理网络，不暴露在 PUBLIC 数据面 Listener；
- 使用 Gateway Admin↔Engine 独立 HMAC Service Credential，不能复用 DDC Secret；
- Engine 使用有界本地 Nonce TTL Cache 防止状态查询重放；
- 响应的 `instanceId + leaseId` 必须与 DDC 投影一致，否则丢弃；
- Admin Client 禁止重定向，并限制目标为配置的可信网段，避免 SSRF；
- 查询使用短超时、有界并发和周期刷新；
- 查询失败只标记投影 stale，不修改 DDC ACK 或 Engine Readiness；
- 响应不包含 Secret、Rule 原文、Provider Credential 或线程堆栈。

## 17. 错误代码

```text
GATEWAY_RELEASE_CONFLICT
GATEWAY_RELEASE_VALIDATION_FAILED
GATEWAY_RELEASE_NO_READY_TARGET
GATEWAY_RULE_SCHEMA_UNSUPPORTED
GATEWAY_RULE_CHECKSUM_MISMATCH
GATEWAY_RULE_COMPILE_FAILED
GATEWAY_RULE_RESOURCE_PREPARE_FAILED
GATEWAY_RULE_LKG_WRITE_FAILED
GATEWAY_RULE_CHUNK_MISSING
GATEWAY_RULE_CHUNK_CHECKSUM_MISMATCH
GATEWAY_DDC_PUBLISH_FAILED
GATEWAY_DDC_PUBLISH_TIMEOUT
GATEWAY_DDC_PUBLISH_UNKNOWN
```

## 18. 测试设计

### 18.1 Compiler

- Canonical JSON 稳定；
- 相同运行语义具有相同 Content SHA；
- 相同 Content 的新 Release 具有不同 Artifact SHA；
- Route/Policy/Descriptor 冲突；
- 未知 Schema/Enum；
- 大小边界。

### 18.2 Apply

- 成功编译、LKG、原子激活、ACK；
- 每一阶段失败保留旧版本；
- 并发请求切换只见完整版本；
- 进程在各写盘点崩溃后的恢复；
- 冷启动 DDC 不可用时加载 LKG 但在 Provider Directory 建立前不 Ready；
- 已运行节点 DDC 中断时按未过期 Provider Lease 降级；
- LKG 损坏时不 Ready。

### 18.3 Publish

- 0/1/多 Engine Target；
- SUCCESS/FAILED/TIMEOUT/UNKNOWN；
- 原 Target 重试和 lease 变化；
- 同 Group 并发发布冲突；
- expectedVersion 冲突；
- 回滚产生新版本。

### 18.4 Runtime Status

- Management Listener/HMAC；
- instanceId/leaseId 匹配；
- 超时、地址复用、重定向和不可信网段拒绝；
- 查询失败只产生 stale 投影；
- 响应不暴露规则原文和 Secret。

### 18.5 Chunk

- Chunk 全成功后 Activation 激活；
- 任一 Chunk 失败不发布 Activation；
- 丢 Chunk、乱序、重复、checksum 错误；
- 启动全量拉取顺序无关；
- 清理不删除活动 Release。

## 19. 验收标准

1. Gateway Admin 不直接写 DDC DB/Redis；
2. Rule Snapshot 不含 Provider 静态地址和 Secret；
3. 相同运行语义产生稳定 Content 字节和 `ruleContentSha256`；
4. Engine 真正激活并持久化后才 ACK 成功；
5. 单节点 Rule 切换原子；
6. Group 发布不虚构分布式同时切换；
7. DDC 丢消息可周期校准；
8. 已运行节点无 DDC 时可在有效 LKG 与未过期 Provider Lease 范围内 Degraded
   Ready，冷启动不能只凭 Rule LKG Ready；
9. 大 Snapshot 只有 CHUNKED Activation 成功后激活；
10. 回滚走完整新发布链。

## 20. 本轮审核项

1. 认可一 Group 一个 DDC appCode scope；
2. 认可 512 KiB 完整 Snapshot、256 KiB Chunk 阈值；
3. 认可 Chunk 先 Staging、统一 Active Key 后激活；
4. 认可 Engine LKG 写盘后再内存激活和 ACK；
5. 认可默认要求至少一个 CONFIG_APPLY_READY Target，而不是要求业务流量 Ready；
6. 认可 DDC `SYNC_ALL_ACK` 不是分布式同时切换；
7. 认可失败/超时/UNKNOWN 与回滚语义。
