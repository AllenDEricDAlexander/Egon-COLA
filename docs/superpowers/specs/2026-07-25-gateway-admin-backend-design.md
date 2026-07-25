# GWS-09 Gateway Admin 后端 Spec

状态：草案，等待审核

父文档：`2026-07-24-gateway-component-design.md`

索引：`2026-07-25-gateway-child-spec-index.md`

依赖：GWS-01、GWS-02、GWS-06

## 1. 目标

Gateway Admin 是管理平台后端，负责接口目录、Gateway Group、路由与策略编辑、发布
编排、运行节点投影、审计和管理 API。它不参与请求热路径，也不向 Engine 提供静态
Provider 地址。

本 Spec 固定：

1. Admin 的分层与领域边界；
2. PostgreSQL 持久化模型和一致性规则；
3. Redis/DDC 的使用边界；
4. 管理、上报、发布和查询 API；
5. Draft、Release、接口定义和节点投影的状态机；
6. 并发、幂等、审计、安全和测试要求。

## 2. 模块职责

Admin 采用 Spring Boot MVC + Bean Validation + Spring Data JPA + Flyway +
PostgreSQL。控制面不在请求热路径，使用阻塞式 MVC/JPA 比把事务代码包装成 Reactive
更直接。Kafka 投影使用 Spring Kafka；DDC 访问只经过 GWS-02 Management Client。

```text
gateway-admin
├── interfaces
│   ├── management       管理页面 API
│   ├── reporting        Starter 机器上报 API
│   └── scheduled        状态校准和保留期任务
├── application
│   ├── catalog          接口目录用例
│   ├── routing          Route/Draft 用例
│   ├── policy           Policy 用例
│   ├── release          校验、编译、发布、重试、回滚
│   ├── projection       Engine/Provider/DDC 投影
│   └── audit            审计编排
├── domain
│   ├── catalog
│   ├── gateway
│   ├── routing
│   ├── policy
│   ├── release
│   └── audit
└── infrastructure
    ├── persistence
    ├── ddc
    ├── kafka
    └── clock
```

Application 定义事务边界和用例编排；Domain 保持纯 Java；Interfaces 不直接访问
Repository；Infrastructure 通过 Port 接入 PostgreSQL、DDC 和 Kafka。

## 3. 核心领域

### 3.1 Gateway Group

`GatewayGroup` 表示一组共享规则坐标和 Engine 集群：

```text
gatewayGroupId
gatewayGroupCode
displayName
env
namespace
description
enabled
revision
createdAt/By
updatedAt/By
```

约束：

- `gatewayGroupCode + env + namespace` 唯一且创建后不可修改；
- 一个 Engine 实例首期只属于一个 Gateway Group；
- 禁用 Group 阻止新发布，不直接杀死 Engine 或清空 LKG；
- 删除只允许逻辑删除，且不存在活动 Engine、Route 和可回滚 Release；
- DDC Config `appCode` 按 GWS-06 由 Group Code 派生。

### 3.2 Application 与三级目录

目录层次固定：

```text
Application System
└── Business Domain
    └── Entity Domain
        └── Interface Group
            └── Operation
```

语义：

- 一个 Controller 或一个 RPC Contract 对应一个 Interface Group；
- 一组 Interface Group 组成一个 Entity Domain；
- 一组 Entity Domain 组成一个 Business Domain；
- Application 是上报和 Provider 注册的业务应用边界；
- 目录是接口定义事实，不等同于 Gateway Group；
- 同一 Operation 可以被不同 Gateway Group 的 Route 引用。

### 3.3 Operation

Operation 保存 GWS-01 的稳定身份，并包含：

- 协议、HTTP Method/Path 或 RPC Service/Method；
- 请求参数、Header、Body、响应和错误 Schema；
- 描述、标签、负责人、废弃信息；
- `externalAccessible`；
- Provider Service Identity；
- 定义来源、构建版本、Definition Set；
- 当前生命周期状态。

接口定义与治理配置分离：

- Starter 可以更新接口 Schema 和说明；
- Admin 用户配置 Route、限流、安全、负载均衡；
- Starter 重报不能覆盖人工治理配置；
- Operation Key 改变时创建新 Operation，不修改旧身份。

`externalAccessible` 属于接口定义的外部暴露资格：

- Starter 来源 Operation 由新 Definition 显式更新该字段，默认 false；
- Admin 的 PUBLIC Route/Exposure Policy 只能在字段为 true 时启用，不反写 Provider
  原始定义；
- 没有 Starter、由管理员维护的 Operation 可以由有权限用户显式修改该字段；
- 任何来源的 false→true 都属于重要变更，必须产生 Diff 和审计，且不会自动发布。

### 3.4 Draft

每个 Gateway Group 有一个当前可编辑 Draft：

```text
GatewayDraft
├── gatewayGroupId
├── revision
├── basedOnReleaseId
├── status = EDITABLE | VALIDATING | PUBLISHING
├── assignments[]
├── policies[]
├── updatedAt/By
└── changeSummary
```

Draft 是聚合概念，可以在数据库中拆表，但所有写入必须通过 `gatewayGroupId +
expectedRevision` 乐观锁。任意子资源修改成功后 Draft Revision 增加。

### 3.5 Release

Release 使用 GWS-06 的不可变模型。Admin 保存：

- Canonical Snapshot 原文；
- Snapshot/Activation/Chunk 摘要；
- 发布 Target 结果投影；
- 发布前校验报告；
- 操作人和变更说明；
- 与上一成功 Release 的结构化差异；
- DDC `changeId` 和状态。

Release 状态：

```text
CREATED
→ VALIDATING
→ READY
→ PUBLISHING
→ SUCCESS | FAILED | TIMEOUT | UNKNOWN

SUCCESS → SUPERSEDED
FAILED | TIMEOUT | UNKNOWN → PUBLISHING（显式 Retry，新 Attempt）
```

至少一个固化 Target 已激活、但 Release 整体不是 SUCCESS 时，使用
`partialApplied=true + Target 计数` 表达。它仍是 FAILED/TIMEOUT/UNKNOWN，不新增与
GWS-06 冲突的终态。Engine 节点可能短暂分歧，Admin 必须展示实际 ACK。

### 3.6 运行投影

Admin 展示但不拥有以下事实：

- Engine Config Client：来自 DDC Config Client；
- Provider Service/Instance：来自 DDC Service Registry；
- Engine 当前 Release：来自发布 ACK 和 Engine 状态上报；
- 调用统计：来自 GWS-12 的聚合消费。

投影可短期缓存，但响应必须携带：

```text
observedAt
source
stale
```

不能把缓存投影当作 Provider 调用寻址源。

Engine 运行详情按 GWS-06 从 DDC Config Client 的管理地址查询。Admin 必须校验
`instanceId + leaseId`、限制可信网段、禁止 HTTP Redirect，并在查询失败时保留最后
投影且标记 stale。

## 4. PostgreSQL 逻辑模型

实施时按仓库 Flyway 规则新增唯一的下一版本 Migration，不修改任何既有 Migration。
本 Spec 定义逻辑表，最终列名可在实施计划中按现有命名规范细化。

| 表 | 核心用途 |
|---|---|
| `gateway_group` | Gateway Group 和作用域 |
| `gateway_application` | 下游 Application |
| `gateway_application_credential` | Starter HMAC Access Key、Secret 密文/引用与轮换状态 |
| `gateway_hmac_nonce` | Starter OpenAPI 防重放 Nonce 短期唯一记录 |
| `gateway_business_domain` | 业务域 |
| `gateway_entity_domain` | 实体域 |
| `gateway_interface_group` | Controller/RPC Contract 分组 |
| `gateway_definition_set` | 一次完整构建上报 |
| `gateway_operation` | 当前 Operation 身份和生命周期 |
| `gateway_operation_definition` | 不可变接口定义版本 |
| `gateway_draft` | Group 当前 Draft 与 Revision |
| `gateway_route_draft` | 路由编辑态 |
| `gateway_policy_draft` | 策略编辑态 |
| `gateway_release` | 不可变发布事实 |
| `gateway_release_content` | Canonical Snapshot/Activation 内容 |
| `gateway_release_attempt` | 每次发布/重试 Attempt 与 DDC Task 状态 |
| `gateway_release_target` | DDC Target ACK 投影 |
| `gateway_audit_log` | 管理变更审计 |

### 4.1 唯一性

至少建立：

- Group：`gateway_group_code + env + namespace`；
- Application：`application_code + env + namespace`；
- 三级目录：各父级下 `code` 唯一；
- Operation：`application_id + operation_key` 唯一；
- Definition Set：`application_id + build_id + protocol + fingerprint` 唯一；
- Draft：每个 Group 一条当前记录；
- Release：`release_id` 唯一；`rule_content_sha256`、`artifact_sha256` 建普通索引，
  允许回滚/重发相同业务内容；
- Attempt：`release_id + attempt_no`；
- Target：`release_id + attempt_no + instance_id + lease_id`；
- 上报幂等请求：`application_id + report_id`。

逻辑删除数据的唯一索引必须保证活跃资源仍唯一，不依赖应用层先查再写。

### 4.2 JSON 使用边界

JSON/JSONB 可以保存：

- OpenAPI/参数 Schema；
- Protobuf Descriptor Snapshot；
- Canonical Release Content；
- 非敏感、受 Schema 约束的 Metadata。

关系和可筛选状态使用结构化列/关联表。不能把整个 Draft 聚合只存一个无约束 JSON，
也不能在 JSON 中保存 Secret。

### 4.3 时间、审计和锁

- 全部表使用带时区时间；
- 关键资源有 `created_by/updated_by`；
- Draft/Group/Application 使用版本列乐观锁；
- Release/Definition Version 创建后内容不可修改；
- 数据库事务只覆盖本地持久化，不跨 DDC HTTP 调用持锁。

## 5. Redis 与 DDC 边界

用户确认的“DB + Redis 双写并通过 Redis 消息下发”由 DDC Component 统一实现：

```text
Gateway Admin PostgreSQL
→ DDC Management OpenAPI
→ DDC PostgreSQL + Redis
→ Redis Pub/Sub full-value
→ Engine Apply/ACK
```

约束：

- Gateway Admin 不直连 DDC Redis；
- Gateway Admin 不写 DDC 表；
- Gateway Admin 只使用 GWS-02 `DdcManagementClient`；
- Gateway PostgreSQL 是管理模型和 Release 审计事实；
- DDC PostgreSQL/Redis 是配置版本、发布任务和下发事实；
- 本地事务提交后才允许调用 DDC；
- DDC 调用失败时 Release 保持可恢复状态，由显式重试/校准处理；
- 不实现分布式事务假象。

## 6. 管理 API

前缀：

```text
/api/v1/gateway/admin
```

### 6.1 Gateway Group 与目录

| Method | Path | 用途 |
|---|---|---|
| GET/POST | `/gateway-groups` | 查询/创建 Group |
| GET/PUT | `/gateway-groups/{id}` | 详情/乐观锁更新 |
| POST | `/gateway-groups/{id}/enable` | 启用 |
| POST | `/gateway-groups/{id}/disable` | 禁用 |
| GET/POST | `/applications` | 查询/创建 Application |
| GET/PUT | `/applications/{id}` | 详情/更新 |
| POST | `/applications/{id}/credentials` | 创建 Starter HMAC Credential |
| POST | `/applications/{id}/credentials/{keyId}/rotate` | 轮换 Credential |
| POST | `/applications/{id}/credentials/{keyId}/revoke` | 吊销 Credential |
| GET | `/applications/{id}/catalog` | 查询完整三级目录 |
| POST | `/applications/{id}/manual-interface-groups` | 创建人工维护三级目录/接口组 |
| POST | `/interface-groups/{id}/manual-operations` | 创建人工 Operation |
| GET | `/operations/{id}` | Operation 与定义历史 |
| PUT | `/operations/{id}/metadata` | 修改人工维护信息 |
| PUT | `/operations/{id}/manual-definition` | 更新 MANUAL 来源的定义并生成新版本 |
| POST | `/operations/{id}/deprecate` | 标记废弃 |

人工定义规则：

- 来源标记为 `MANUAL`，Starter 来源标记为 `STARTER`；
- MANUAL 更新同样创建不可变 Operation Definition Version；
- 不能用人工接口静默覆盖已存在的 STARTER Operation Key；
- 来源转换需要独立迁移操作、Diff 和审计，不由普通更新 API 完成；
- 人工定义必须经过与 Starter 报告相同的协议、Schema、外部暴露和 Provider Identity
  校验。

### 6.2 Draft、Route 与 Policy

| Method | Path | 用途 |
|---|---|---|
| GET | `/gateway-groups/{id}/draft` | 当前 Draft |
| PUT | `/gateway-groups/{id}/draft/routes/{routeId}` | 新增/更新 Route |
| DELETE | `/gateway-groups/{id}/draft/routes/{routeId}` | 删除 Draft Route |
| PUT | `/gateway-groups/{id}/draft/policies/{policyId}` | 新增/更新 Policy |
| DELETE | `/gateway-groups/{id}/draft/policies/{policyId}` | 删除 Draft Policy |
| POST | `/gateway-groups/{id}/draft/validate` | 完整校验/预编译 |
| GET | `/gateway-groups/{id}/draft/diff` | 与基线 Release 比较 |

所有 Draft 写请求携带：

```text
expectedRevision
idempotencyKey
changeReason
```

Revision 冲突返回 `409 GATEWAY_ADMIN_REVISION_CONFLICT` 和当前 Revision，不自动重放
用户修改。

### 6.3 发布

| Method | Path | 用途 |
|---|---|---|
| POST | `/gateway-groups/{id}/releases` | 基于指定 Revision 创建并发布 |
| GET | `/releases/{releaseId}` | Release、内容摘要和 Target |
| GET | `/releases/{releaseId}/diff` | 与指定/前一 Release 比较 |
| POST | `/releases/{releaseId}/retry` | 重试原固化 Target |
| POST | `/gateway-groups/{id}/rollback` | 用历史成功内容创建新 Release |
| GET | `/gateway-groups/{id}/releases` | 发布历史 |

创建发布必须携带 `expectedDraftRevision`。请求超时不代表发布失败，调用方使用返回的
`releaseId` 查询，不能直接再次创建。

### 6.4 运行投影

| Method | Path | 用途 |
|---|---|---|
| GET | `/gateway-groups/{id}/engine-nodes` | Engine 节点、租约、版本、ACK |
| GET | `/providers/services` | Provider Service 目录 |
| GET | `/providers/instances` | Provider Instance、Metadata、健康投影 |
| GET | `/gateway-groups/{id}/runtime-consistency` | 目标 Release 一致性 |

这些 API 是管理查询，不参与 Engine 热路径。

## 7. Starter 上报 API

前缀：

```text
/api/v1/gateway/openapi/interface-definitions
```

使用 GWS-02 相同的 HMAC 签名语义，机器身份与管理页面身份分离。

| Method | Path | 用途 |
|---|---|---|
| POST | `/reports` | 提交一个完整 Definition Set |
| GET | `/reports/{reportId}` | 查询幂等结果 |

请求模型由 GWS-10 主责，Admin 负责：

1. 验证签名、时间戳、Nonce、Contract Version；
2. 校验 Application/环境/namespace 作用域；
3. 在事务中创建 Definition Set 和 Operation Definition；
4. 根据 Operation Key 关联或创建 Operation；
5. 计算新增、变更、缺失和冲突结果；
6. 返回稳定 Report Result；
7. 不因上报直接发布 Gateway Rule。

## 8. 发布编排

### 8.1 创建 Release

```text
锁定 Draft Revision
→ 加载接口定义和策略
→ 完整校验
→ 编译 Canonical Snapshot
→ 本地事务保存 Release/Content
→ 提交事务
→ 调用 DDC Upsert/Publish
→ 持续投影 DDC Task
→ 更新 Release 终态
```

本地事务中不进行网络调用。发布编排器使用状态机模式，因为外部 DDC 调用存在超时、
部分成功和恢复需求，简单的一次 Controller 调用无法正确表达这些状态。

### 8.2 恢复

Admin 启动和定时任务扫描非终态 Release：

- 有 `changeId`：查询 DDC Task 并更新投影；
- DDC 返回 UNKNOWN：保留 UNKNOWN，允许人工核对/重试；
- 无 `changeId` 且已保存内容：在幂等保护下重新发起；
- 已成功：不重复发布；
- 长时间失败：保持诊断，不自动回滚；
- 重试沿用原 Release 内容和原固化 Target，不能重新编译当前 Draft。
- DDC Active Version 或 Group Release 已被更新版本推进时，拒绝旧 Release Retry，
  用户只能创建新回滚 Release。

### 8.3 回滚

回滚不是修改 DDC 版本或让 Engine 接受低版本，而是：

1. 选择历史成功 Release；
2. 复制其 Canonical 业务内容；
3. 创建新的 Release ID、版本和 Change ID；
4. 重新校验当前 Engine Capability；
5. 正常发布并记录回滚原因。

## 9. 接口定义生命周期

Operation 状态：

```text
DISCOVERED
→ ACTIVE
→ DEPRECATED
→ OFFLINE
```

规则：

- 新定义首次上报为 DISCOVERED，经合法完整集合确认后 ACTIVE；
- 单次上报失败或接口暂时缺失不能立即 OFFLINE；
- Definition Set 是否有权声明缺失由 GWS-10 的完整批次语义决定；
- 被活动 Route 引用的 Operation 不能直接 OFFLINE；
- DEPRECATED 仍可运行，但 Admin 发布校验给出明确警告；
- OFFLINE Operation 不进入新 Snapshot；
- 历史 Definition、Release 引用和审计永不物理级联删除。

## 10. 并发与幂等

- POST 创建请求使用 `Idempotency-Key`；
- 相同 Key + 相同 Payload 返回原结果；
- 相同 Key + 不同 Payload 返回冲突；
- Draft 写使用 Expected Revision；
- Definition Report 使用 `reportId + fingerprint`；
- DDC Upsert 使用确定 Config Coordinate 和 Expected Version；
- 发布恢复任务采用数据库租约/CAS，避免多 Admin 进程重复执行；
- 首期 DDC 仍是单 Admin/单 Redis，但 Gateway Admin 自身不得依赖 JVM 单例锁保证
  正确性；
- 所有后台任务可重入。

## 11. 管理后端安全

本 Spec 不选择具体 Admin 登录方案，但必须留出：

```text
AdminActor
├── actorId
├── actorType = USER | SERVICE
├── tenant/namespace scopes
└── roles/permissions
```

要求：

- 管理 API 和 Starter OpenAPI 使用不同 Security Chain；
- 生产环境 Starter 上报强制 HMAC；
- Access Key 绑定唯一 Application/Env/Namespace Scope；
- Nonce 使用 PostgreSQL `access_key + nonce` 唯一约束原子占用，并由定时任务按签名
  有效窗口清理；不借用 DDC Redis；
- HMAC Secret 通过 `GatewaySecretProtector` 加密存储或保存外部 Secret Reference，
  只在创建/轮换时返回一次；
- 默认 `GatewaySecretProtector` 使用 JCA AES-256-GCM，主密钥及 Key Version 由环境
  Secret 注入；生产可以替换为 KMS/Vault Adapter；
- 轮换允许有界双 Key 重叠期，吊销立即拒绝新请求；
- 发布、回滚、禁用和外部暴露变更属于重要操作，必须鉴权和审计；
- Secret 不存入 Gateway 业务表和审计 Payload；
- 日志对 HMAC、Cookie、Authorization 和 DDC Secret 脱敏；
- 页面身份 Header 不能被 Starter OpenAPI 接受。

## 12. 审计

审计记录：

- Actor、来源、Request/Trace ID；
- 资源类型和 ID；
- 动作；
- 变更前后摘要或结构化 Diff；
- Draft Revision/Release ID；
- 成功、失败和标准错误码；
- UTC 时间。

必须审计：

- Group 创建、启停；
- Route/Policy 修改；
- `externalAccessible` 变化；
- 校验、发布、重试、回滚；
- Operation 废弃/下线；
- Definition Report 接受/拒绝；
- 管理权限和配置变更。

大 Snapshot 不复制到审计表，只记录 SHA、大小和 Release 引用。

## 13. 错误模型

| Code | HTTP | 含义 |
|---|---:|---|
| `GATEWAY_ADMIN_VALIDATION_FAILED` | 422 | Draft/接口/策略校验失败 |
| `GATEWAY_ADMIN_REVISION_CONFLICT` | 409 | 乐观锁冲突 |
| `GATEWAY_ADMIN_IDEMPOTENCY_CONFLICT` | 409 | 幂等 Key Payload 不一致 |
| `GATEWAY_ADMIN_RESOURCE_IN_USE` | 409 | 资源仍被 Route/Release 引用 |
| `GATEWAY_ADMIN_RELEASE_IN_PROGRESS` | 409 | 同 Group 正在发布 |
| `GATEWAY_ADMIN_DDC_UNAVAILABLE` | 503 | DDC 调用不可用 |
| `GATEWAY_ADMIN_RELEASE_UNKNOWN` | 202 | 外部发布结果未知，需查询 |
| `GATEWAY_ADMIN_REPORT_REJECTED` | 422 | Definition Report 非法 |
| `GATEWAY_ADMIN_SCOPE_DENIED` | 403 | Actor 无作用域权限 |

校验错误返回稳定 `field/path/code/message` 列表，不返回 JPA、SQL 或 DDC 内部异常。

## 14. 可观测性

指标：

```text
gateway_admin_release_total{status}
gateway_admin_release_duration_seconds{status}
gateway_admin_release_targets{status}
gateway_admin_definition_report_total{status,protocol}
gateway_admin_projection_refresh_total{source,status}
gateway_admin_background_job_total{job,status}
```

日志使用 Trace/Request ID、Group/Release/Change ID。禁止把 Operation Path、
Application Code、Actor ID 等高基数字段放入指标 Label。

## 15. 测试设计

### 15.1 Domain/Application

1. Group 作用域唯一、禁用和删除约束；
2. 三级目录父子关系；
3. Operation Key 不随描述变化；
4. Starter 定义不能覆盖治理配置；
5. MANUAL/STARTER 来源冲突和人工定义版本；
6. Draft Expected Revision 并发冲突；
7. Idempotency Key 相同/不同 Payload；
8. Release 状态机非法跃迁拒绝；
9. 回滚创建新版本而不是版本倒退；
10. 活动引用阻止 Operation Offline；
11. 审计内容脱敏。

### 15.2 Persistence

1. 唯一索引和逻辑删除；
2. JSON Schema/Descriptor 往返；
3. Release Content 不可变；
4. Target 的 `instanceId + leaseId` 唯一；
5. 后台任务租约/CAS；
6. Flyway 从空库迁移；
7. 已迁移数据库升级。

### 15.3 DDC Integration

1. Upsert→Publish→Target ACK→Release 成功；
2. 部分失败、超时、UNKNOWN；
3. Admin 重启后恢复非终态 Release；
4. Chunk/Activation 顺序和清理；
5. DDC 不可用时本地 Release 可恢复；
6. Gateway Admin 未直接访问 DDC Redis/Repository。

### 15.4 API

1. 管理 API 分页、筛选和错误结构；
2. 乐观锁、幂等和 Scope；
3. Starter HMAC 成功、重放、过期、篡改；
4. Credential 创建只返回一次、密文落库、轮换重叠和吊销；
5. Entity/Secret 不出现在响应；
6. Provider/Engine 投影包含来源与新鲜度；
7. 发布超时后能用 Release ID 查询。

## 16. 验收标准

1. Admin 完整承载三级接口目录和详细 Operation 定义；
2. 接口定义与 Route/Policy 治理配置相互独立；
3. Draft 并发修改不会最后写覆盖；
4. Release 内容不可变且可审计、比较、重试和回滚；
5. DB/Redis 双写与 Redis 消息由 DDC 承接，Admin 不直连 DDC 存储；
6. Engine/Provider 地址只用于管理投影，不参与请求寻址；
7. Admin 重启后可恢复发布状态；
8. Starter 上报幂等且不能触发隐式发布；
9. 所有重要操作具有 Actor、Revision/Release 和结果审计；
10. 所有数据库变更只通过新的 Flyway Migration 实施。

## 17. 本轮审核项

1. 认可接口目录、治理 Draft 和 Release 三类事实分离；
2. 认可 Gateway Admin PostgreSQL 与 DDC PostgreSQL/Redis 的职责划分；
3. 认可发布使用可恢复状态机，不使用跨系统事务；
4. 认可 Operation 下线需完整上报语义且受活动 Route 保护；
5. 认可节点和 Provider 在 Admin 中仅作为带新鲜度的管理投影；
6. 认可管理 API 与 Starter HMAC OpenAPI 使用不同安全边界。
