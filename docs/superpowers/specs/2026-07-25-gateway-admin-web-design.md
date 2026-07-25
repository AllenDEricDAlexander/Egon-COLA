# GWS-11 Gateway Admin Web Spec

状态：草案，等待审核

父文档：`2026-07-24-gateway-component-design.md`

索引：`2026-07-25-gateway-child-spec-index.md`

依赖：GWS-09、GWS-10

技术栈：React、TypeScript、Vite、React Router、TanStack Query、Ant Design、
Ant Design Charts

## 1. 目标

Gateway Admin Web 是 Gateway 的管理平台前端，面向平台管理员、网关运维人员和接口
负责人，提供：

1. Gateway Group 与 Engine 节点总览；
2. 业务域 → 实体域 → 接口组 → Operation 的接口目录；
3. Route、流量治理和安全策略编辑；
4. 校验、差异、发布、Target ACK、重试和回滚；
5. Provider、Trace、调用统计和审计查询；
6. 所有前端请求优先生成并传递 Trace ID。

页面不直接访问 DDC、Redis、Kafka 或 Engine，只调用 GWS-09 Admin API。

## 2. 非目标

- 不在前端实现 Gateway 路由或规则编译；
- 不允许页面直接修改运行态 Redis；
- 不通过 WebSocket 向 Engine 直接下发规则；
- 不在浏览器保存 DDC/HMAC Secret；
- 不在本期实现完整企业 IAM、登录和权限后端；
- 不提供 Nginx 节点、配置或发布页面；
- 不提供 Nacos/Dubbo 管理页面；
- 不把调用明细原始 Kafka 消息全部加载到浏览器；
- 不以图表替代可审计的发布状态和 Target 明细。

## 3. 工程结构

```text
egon-cola-component-gateway-admin-web/
├── package.json
├── vite.config.ts
├── tsconfig.json
├── src/
│   ├── app/                 Router、Provider、全局错误边界
│   ├── api/                 Typed Client、DTO、错误转换
│   ├── components/          领域无关复用组件
│   ├── features/
│   │   ├── dashboard/
│   │   ├── gateway-groups/
│   │   ├── interface-catalog/
│   │   ├── routes/
│   │   ├── policies/
│   │   ├── releases/
│   │   ├── providers/
│   │   ├── observability/
│   │   └── audit/
│   ├── layouts/
│   ├── hooks/
│   ├── styles/
│   └── test/
└── e2e/
```

按 Feature 组织，不建立全局无边界 `utils` 或把所有页面状态塞入一个 Store。

## 4. 前端基础架构

### 4.1 路由

使用 React Router，推荐路径：

```text
/dashboard
/gateway-groups
/gateway-groups/:groupId/overview
/gateway-groups/:groupId/draft/routes
/gateway-groups/:groupId/draft/policies
/gateway-groups/:groupId/releases
/gateway-groups/:groupId/releases/:releaseId
/interface-catalog
/applications/:applicationId/catalog
/operations/:operationId
/providers
/observability/traces
/audit
```

URL 保存当前资源身份和可分享的筛选条件。Modal 临时状态不需要进入 URL。

### 4.2 API Client

基于浏览器 `fetch` 封装统一 Typed Client，负责：

- Admin API Base URL；
- Contract Version；
- Trace ID；
- 登录态凭据占位接入；
- AbortSignal；
- 统一错误模型；
- 分页和时间格式；
- 401/403/409/422/5xx 分类。

禁止页面直接散落 `fetch`/`axios` 调用。DTO 类型以 Admin 稳定 API 为准，不复用后端
JPA Entity。

### 4.3 Server State

使用 TanStack Query 的 Query/Mutation 管理：

- 缓存 Key；
- 加载、错误、空状态；
- 请求取消；
- Mutation 后的精确失效；
- 发布状态轮询；
- 页面离开时停止轮询。

首期不引入 Redux/Zustand 等全局客户端状态框架。Draft 未保存表单状态保留在
Feature 内，跨页面作用域只保留 Env/Namespace、当前 Actor 和 UI Preference。

### 4.4 组件与主题

- 使用 Ant Design 基础组件和 Token；
- 图表只使用 Ant Design Charts；
- 统一状态颜色、时间、错误和空状态；
- 支持桌面管理后台主尺寸，关键表格在窄屏可横向滚动；
- 禁止用颜色作为唯一状态表达；
- 中文为首期默认文案，代码与 API 字段保持英文。

## 5. Trace ID

每次用户触发的逻辑请求在浏览器生成 Trace ID：

```text
traceId = 32 个小写十六进制字符
```

传递方式：

```text
traceparent: 00-{traceId}-{spanId}-01
X-Trace-Id: {traceId}
```

规则：

1. 优先使用 Web Crypto 生成随机字节；
2. 一次 Mutation 及其状态查询保留同一业务 Trace ID，并为每个 HTTP 请求生成新
   Span ID；
3. 自动重试保留 Trace ID；
4. 新的用户操作生成新 Trace ID；
5. 后端返回合法 Trace ID 时错误页展示并允许复制；
6. 浏览器无法生成或 Header 被代理移除时，Engine/Admin 仍按 GWS-12 生成；
7. Trace ID 不包含用户、时间、资源 ID 或其他业务信息。

## 6. 信息架构与页面

### 6.1 全局布局

左侧导航：

```text
总览
Gateway Group
接口目录
Provider
调用观测
审计日志
```

顶部区域：

- 当前 Env/Namespace；
- 全局搜索；
- 当前用户/角色占位；
- 后端连接状态；
- 文档入口。

Env/Namespace 是强作用域选择。切换后清空不兼容缓存和未保存 Draft，并要求用户确认
未保存内容。

### 6.2 总览

卡片：

- Gateway Group 数量；
- Ready/Not Ready Engine 节点；
- 当前版本不一致 Group；
- 活跃 Provider/异常 Provider；
- 最近发布成功率；
- 请求量、错误率和延迟摘要。

图表：

- 请求量/错误率时间序列；
- P50/P95/P99 延迟；
- 按协议的调用分布；
- 最近发布状态。

所有图表下提供同等信息的表格或可访问摘要。高基数 Operation 不在总览一次性全部
绘制。

### 6.3 Gateway Group

列表展示：

- Code/名称；
- Env/Namespace；
- 启停；
- Ready/总节点；
- 目标/实际 Release；
- 一致性；
- 最近发布时间和结果。

详情分为：

- Overview；
- Engine Nodes；
- Draft Routes；
- Draft Policies；
- Releases；
- Runtime Consistency。

节点表必须区分：

```text
instanceId
leaseId
observedAt
stale
capabilities
activeReleaseId
lastAck
```

不能只显示一个模糊“在线”。

### 6.4 接口目录

左侧树：

```text
Application
└── Business Domain
    └── Entity Domain
        └── Interface Group
            └── Operation
```

右侧详情：

- 稳定 Code/ID；
- HTTP/RPC 身份；
- 参数、Body、响应、错误 Schema；
- `externalAccessible`；
- Definition Set、构建版本和状态；
- 来源 `STARTER|MANUAL`；
- Provider Service Identity；
- 当前引用它的 Route；
- 历史定义与 Diff；
- 废弃/下线状态。

树支持名称/Code/Method/Path/RPC Method 搜索。大量节点使用懒加载或虚拟滚动。

有权限用户可以创建 MANUAL 目录/Operation，并对 MANUAL Definition 生成新版本；
STARTER 原始定义只读，只允许编辑独立的管理元数据。页面必须显示来源，禁止把人工
修改伪装成 Starter 上报。

### 6.5 Route 编辑

Route 编辑表单按协议显示：

- Listener/Access Zone；
- Host、Method、Path 或 RPC Full Method；
- Operation；
- Provider Service；
- HTTP→RPC 映射；
- Policy 引用；
- 优先级；
- 启停。

交互约束：

- 选 PUBLIC 时立即提示并阻止选择
  `externalAccessible=false` Operation；
- 不提供静态 Provider URL 输入框；
- Path 冲突先做前端提示，最终以后端校验为准；
- 保存必须携带当前 Draft Revision；
- 409 时展示服务端最新 Revision 和本地未保存内容，不自动覆盖。

### 6.6 Policy 编辑

按能力分区：

- 超时；
- 限流；
- 并发隔离；
- 熔断；
- 重试；
- 负载均衡；
- 安全；
- CORS/响应处理。

表单以后端 Schema/枚举为准，单位明确。危险组合即时提示，例如：

- 非幂等 Operation 开启自动重试；
- 分布式限流选择 Fail Open；
- 安全 Provider 缺失；
- Retry 总预算超过 Deadline；
- PUBLIC Route 引用内部 Operation。

页面提示不能替代后端发布校验。

### 6.7 发布工作台

发布流程：

```text
查看 Draft Revision
→ 填写变更说明
→ 完整校验
→ 查看错误/警告
→ 查看与基线 Diff
→ 确认发布
→ 展示 Release/Change ID
→ 轮询 Target ACK
→ 展示终态
```

Target 表展示：

- Engine Instance/Lease；
- 目标版本；
- ACK 状态；
- Apply 阶段；
- 当前活动 Release；
- 耗时；
- 脱敏错误；
- 最后更新时间。

发布详情按 Attempt 展示历史；Retry 新增 Attempt，不能覆盖上一轮 FAILED/TIMEOUT/
UNKNOWN 的 Target 证据。

`UNKNOWN`、`TIMEOUT`、`FAILED` 或任何 `partialApplied=true` 不能渲染为绿色成功。
重试必须确认并明确“使用原 Release 内容和原 Target”。回滚必须展示它会创建新
Release。

### 6.8 Provider

按 Service Key 聚合展示：

- Protocol；
- Service/Group/Version；
- Instance 数量；
- Host/Port；
- Region/Zone/Weight/Tags；
- Definition Set ID；
- DDC Lease 与 observedAt；
- Engine 本地健康投影（若有）。

页面明确标注“管理投影，不是静态路由配置”。不得提供把 Instance 地址写入 Route 的
操作。

### 6.9 调用观测

支持按：

- 时间范围；
- Trace ID；
- Gateway Group；
- Protocol；
- Operation/Route；
- 状态类别；
- Engine Node；
- Provider Service。

查询聚合与受控明细，不直接消费 Kafka。Trace 详情展示一次调用/尝试时间线，但不
展示请求 Body、Credential、Cookie 或未经脱敏的 Header。

### 6.10 审计

筛选：

- Actor；
- Action；
- Resource Type/ID；
- Release/Trace ID；
- 时间；
- 成功/失败。

详情显示结构化 Diff、Revision/Release 和错误码。大 Snapshot 只提供 Release 引用和
SHA，不在浏览器渲染整份无界 JSON。

## 7. 表单与并发体验

- 打开 Draft 时保存 Revision；
- 保存 Mutation 带 Expected Revision 和 Idempotency Key；
- 页面离开有未保存内容时提示；
- 422 把错误定位到字段/规则；
- 409 显示冲突，不自动合并复杂 Route/Policy；
- 网络超时后先按 Idempotency/Release ID 查询，不盲目再次创建；
- 发布按钮在请求进行中防重复提交，但正确性依赖后端幂等；
- Date/Duration/Rate 输入显示明确单位和边界；
- 删除/禁用/回滚等重要操作二次确认并要求变更原因。

## 8. 权限扩展点

前端提供路由和操作级 Capability：

```text
gateway.group.read
gateway.draft.write
gateway.release.publish
gateway.release.rollback
gateway.operation.manage
gateway.audit.read
```

本期可以使用 Mock/占位 Actor 完成 UI 开发，但：

- 后端始终是权限裁决方；
- 隐藏按钮不是安全控制；
- 401、403 分开处理；
- 禁止把权限列表硬编码为生产信任；
- 后续 IAM 接入不应重写 Feature 页面。

## 9. 错误、加载与空状态

每个页面明确处理：

- 初次加载；
- 后台刷新；
- 空数据；
- 过滤后为空；
- 403；
- 404；
- 409；
- 422；
- 网络失败；
- 服务端 5xx；
- 数据 stale。

全局错误页展示：

- 用户可理解的摘要；
- 标准错误码；
- Trace ID；
- 重试入口；
- 不展示堆栈、SQL、Secret 或原始内部响应。

## 10. 性能

- 列表服务端分页、排序、过滤；
- 目录树按 Application/Domain 懒加载；
- 大 Schema 延迟加载；
- 图表限制时间范围和点数；
- 状态轮询只在可见页面和非终态 Release 开启；
- 相同请求去重和取消；
- 输入搜索防抖；
- 不在前端加载全部调用事件做聚合；
- Bundle 按 Feature 路由拆分。

## 11. 可访问性

- 所有表单有 Label、错误关联和键盘操作；
- Modal 打开/关闭正确管理焦点；
- 状态不只依靠颜色；
- 图表有文本摘要；
- 表格操作按钮有明确名称；
- 对比视图支持键盘和屏幕阅读器；
- 默认主题满足管理后台基本对比度；
- 自动刷新不抢焦点、不反复播报。

## 12. 测试设计

### 12.1 单元/组件

使用 Vitest 与 React Testing Library：

1. Typed Client 注入 Trace ID；
2. API 错误转换；
3. PUBLIC Route 禁止内部 Operation；
4. Draft 409 冲突；
5. 发布各终态显示；
6. Target ACK 表；
7. Definition Tree 与详情；
8. 敏感字段不渲染；
9. 权限能力只影响交互、后端错误仍处理；
10. 页面卸载停止轮询。

### 12.2 契约

- 以 Admin API DTO Fixture 验证序列化；
- 未知枚举显示“不支持”，不能默认为成功；
- 时间、分页、错误、Revision 与 Idempotency 契约；
- 前后端 Contract Version 不兼容时阻止危险 Mutation。

### 12.3 E2E

使用 Playwright：

1. 真实 Starter 上报后目录可见；
2. 创建 Route/Policy、校验、查看 Diff；
3. 发布成功并看到所有 Engine ACK；
4. 部分失败/超时/UNKNOWN 显示正确；
5. Revision 冲突；
6. 回滚创建新 Release；
7. Provider 注册/过期投影；
8. Trace ID 从浏览器贯穿 Admin 和 Engine 查询；
9. 403/422/5xx；
10. Nginx/Nacos/Dubbo 页面不存在。

## 13. 构建与部署

- Node 使用实施时仍受支持的 Active LTS，并通过仓库工具版本文件固定；
- 包管理器统一使用 npm，只提交 `package-lock.json`；
- `build` 产出静态制品；
- API Base URL 使用运行期部署配置或同源代理；
- Source Map 是否公开由生产安全配置决定；
- Admin Web 可独立于 Java Maven Reactor 构建；
- 不由本项目管理 Nginx；静态制品可交给现有平台托管。

## 14. 验收标准

1. 使用 React、Ant Design、Ant Design Charts 构建独立管理平台；
2. 用户能完整浏览三级接口目录与详细接口定义；
3. 用户能编辑、校验、比较、发布、重试和回滚规则；
4. 节点 ACK、版本不一致和 stale 投影被真实展示；
5. 页面不直接访问 DDC/Redis/Kafka/Engine；
6. 每个前端请求优先生成并传递 Trace ID；
7. Draft 并发冲突不会静默覆盖；
8. PUBLIC/INTERNAL 和 `externalAccessible` 在编辑时有明确约束；
9. 调用观测不泄露 Body、Credential 或敏感 Header；
10. 核心流程具有组件测试和真实 E2E。

## 15. 本轮审核项

1. 认可独立 Vite 前端，不强制并入 Java Maven Reactor；
2. 认可上述信息架构和主要页面；
3. 认可发布必须先校验、看 Diff，再观察逐节点 ACK；
4. 认可 Trace ID 由浏览器优先生成；
5. 认可节点/Provider 只展示管理投影，不提供静态寻址配置；
6. 认可首期只留 IAM Capability 接口，不在本 Spec 选择具体权限系统。
