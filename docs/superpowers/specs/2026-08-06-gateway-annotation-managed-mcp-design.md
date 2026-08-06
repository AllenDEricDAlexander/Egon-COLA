# Gateway 注解托管 MCP 设计与破坏性迁移

## 1. 目标

Gateway 本地 MCP Tool 只允许由业务接口注解声明。控制面不再保存本地
Tool 的名称、Schema、Operation 绑定、幂等性或字段绑定，也不提供创建、
编辑、删除本地 Tool 的入口。

本次是破坏性升级：不读取旧 `LOCAL_OPERATION` Tool Draft，不提供双写、
回退或兼容 API。Remote MCP、Resource、Resource Template、Prompt、App、
Task、Approval 和 MCP Server 平台配置继续由控制面管理。

## 2. 唯一事实来源

本地 Tool 的代码事实来源如下：

```text
@GatewayInterfaceGroup.mcpServerCode
@GatewayOperation.registerMcp
@GatewayOperation.mcpName
@GatewayOperation.mcpRequiredPermissions
@GatewayOperation.mcpRiskLevel
@GatewayOperation.idempotent
HTTP/RPC Operation Definition and Schema
```

Starter 将其上报为 `operation.attributes.mcpExposure`。HTTP 和 Unary RPC
共用同一份元数据模型；没有 `registerMcp = true` 的 Operation 不生成 Tool。

控制面只保存严格覆盖项：

- 禁用某个 Managed Tool；
- 将 Tool 移到另一个 MCP Server；
- 追加权限；
- 提高最低风险等级。

覆盖项不能改 Tool 名、描述、Schema、Operation、幂等性，不能删除代码权限，
不能删除已有追加权限，也不能降低代码风险或已有环境最低风险。删除覆盖项表示
显式恢复全部注解默认值。

## 3. 发布投影

`McpReleaseContentFactory` 作为唯一投影边界，读取当前 Catalog Definition，
将 `mcpExposure` 转换为 Managed Tool。它继续使用项目已有 Factory/Projection
模式；变化点只有一处，不增加 Strategy 层级。

Tool ID 固定为：

```text
lowercaseHex(SHA-256(serverCode + "\0" + operationKey))
```

其中 `operationKey` 是 HTTP/RPC 已有的稳定业务标识，数据库随机
`operationId` 只作为本次发布内的调用引用。

HTTP Tool 的输入 Schema 从 `attributes.parameters` 合并 PATH、QUERY 和 BODY；
同时生成 `inputLocations`，运行时据此构造位置感知调用。HEADER 和 COOKIE
永远不进入模型 Schema：optional 参数忽略，Authorization 由 Gateway 身份上下文
注入，其他 required HEADER/COOKIE 拒绝投影。PART、Multipart 和 Streaming
不允许自动投影。

Unary RPC Tool 直接复用 Protobuf 请求 Schema，将整个 MCP `arguments` 作为
RPC 请求对象，不生成 `inputLocations`。HTTP 和 RPC 都直接复用响应 Schema。

最终值的合并规则为：

```text
enabled             = code exposure exists AND override.enabled != false
server              = override.server OR code server
permissions         = code permissions UNION additional permissions
risk                 = max(code risk, minimum risk)
name/schema/operation/idempotent = code only
```

同一 Server 内 Tool 名冲突、引用不存在 Server、非法 Schema 或非法参数位置
都会阻止发布，不允许静默覆盖。

## 4. Operation 收集

Release 必须先构造 MCP 内容，再计算 Runtime Operation 并集：

```text
enabled Route references
UNION enabled Managed Tool references
UNION Resource/ResourceTemplate/Prompt/Completion local Operation references
```

disabled Route 不再贡献 Operation。只供 MCP 使用的 Operation 由 Managed Tool
直接带入 Release，因此不再需要 disabled Route 锚点。

## 5. 控制面边界

保留：

- MCP Server 的 OAuth、Dialect、Instructions、缓存和启停配置；
- Managed Tool 只读目录与严格 Override；
- Remote MCP Provider、Mount 和 Remote Tool；
- Resource、Resource Template、Prompt、Completion、App、Task、Approval；
- 审计、幂等键、草稿修订和统一 Gateway Release。

删除：

- `CapabilityKind.TOOL` 通用本地 Tool 分支；
- `/servers/{serverId}/tools`、`/tools/{id}` 本地 Tool CRUD；
- Operation selector、input/output Schema、argument/result bindings、手工幂等表单；
- Draft 到本地 Runtime Tool 的构造逻辑；
- `McpArgumentBinder` 和结果字段选择；
- disabled Route 锚点及其启动、恢复、校验脚本；
- 控制面和启动脚本中的手工本地 Tool Draft JSON 夹具。

Remote Tool 使用独立 `/remote-tools` API。Managed Tool 使用只读
`/groups/{groupId}/managed-tools` 和 `/managed-tools/{toolId}/override` API，
两者不共享本地 Tool 表单或存储。

## 6. V10 数据迁移

V10 是本次唯一数据库迁移，并且不得修改 V1-V9：

1. 创建 `gateway_mcp_managed_tool_override`；
2. 创建 `gateway_mcp_remote_tool_draft`；
3. 将旧 `gateway_mcp_tool_draft` 中 `source_type = 'REMOTE_MCP'` 的数据迁入
   Remote Tool 专表并保留原 ID、修订和审计字段；
4. 不迁移任何 `LOCAL_OPERATION` 数据；
5. 删除 `gateway_mcp_tool_draft`。

旧本地 Tool 的名称、Schema、bindings、权限、风险和幂等配置都会永久丢弃。
需要继续暴露的接口必须先完成注解和新 buildId 上报。

## 7. 部署顺序

由于不提供兼容层，本次使用维护窗口，不做新旧版本滚动混跑：

1. 为现有数据库创建可恢复备份，冻结 Gateway/MCP 草稿写入和发布；
2. 升级 Starter 和业务应用，为目标接口添加注解并使用新 buildId 上报；
3. 确认当前 Catalog Definition 已包含完整 `mcpExposure`，且 Server/Tool 名唯一；
4. 停止旧 Gateway Admin 和 Engine，避免旧 Runtime Tool 契约继续消费新发布；
5. 部署新 Gateway Admin，执行 V10；
6. 部署新 Gateway Engine；
7. 检查 MCP Server、Remote Provider/Mount/Tool 和其他 capability 草稿；
8. 创建第一份注解托管 Release，确认无 Route 的 Managed Tool 也带入 Operation；
9. 验证 PATH/QUERY/BODY、Unary RPC、权限、HIGH/CRITICAL Approval、Task、
   Remote MCP、Resource/Prompt/App 以及双 Engine 激活；
10. 恢复对外流量和控制面写入。

回滚只能恢复数据库备份并整体回退 Admin、Engine、Starter 和业务应用版本。
不能依赖已经删除的本地 Tool Draft 或旧 API 做应用级回退。

## 8. 完成判定

- 仓库中不存在本地 Tool 手工 CRUD、表单、bindings 或 Draft 存储；
- `registerMcp = true` 且无 Route 的 HTTP/RPC Operation 能发布和调用；
- 取消注解后，下一次 Release 不再包含对应 Managed Tool；
- Override 只能收紧，删除 Override 可恢复代码默认值；
- Remote MCP 和其他 capability 行为不回退；
- `gateway_mcp_tool_draft` 已删除，旧 `LOCAL_OPERATION` 数据未保留。
