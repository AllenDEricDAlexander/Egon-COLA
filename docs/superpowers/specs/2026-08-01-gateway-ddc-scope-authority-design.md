# Gateway 以 DDC 为作用域权威的集成设计

> 状态：待书面复核（2026-08-01）
> 日期：2026-08-01
> 适用仓库：`/Users/mario/SelfProject/Egon-COLA`
> 适用模块：DDC Admin、DDC Starter Management Client、Gateway Admin、Gateway Admin Web、Gateway Engine

## 1. 文档目的

本文固化 DDC 与 Gateway Admin 的作用域集成规则，解决 Gateway Admin 当前使用自有硬编码
`biz/app/env/namespace` 候选值，导致页面默认进入空作用域、Gateway 数据与 DDC 的
`biz/namespace/env/app` 绑定不一致，以及同一物理应用在多个 namespace 下的可见性被错误建模为多份应用的问题。

本设计以 DDC 已启用的 namespace-env-app 绑定为唯一作用域权威。Gateway Admin 不再维护第二套
业务域、namespace、环境和应用主数据；Gateway Web 只通过 Gateway Admin 后端读取 DDC 作用域，
不直接持有 DDC 管理凭据。

## 2. 当前问题与根因

### 2.1 已确认的实现问题

1. Gateway Web 的初始值硬编码为 `default/default-app/dev/default`，顶部候选项也是静态值；
2. 顶部选择顺序为 `biz/app/env/namespace`，与 DDC 管理模型 `biz/namespace/env/app` 不一致；
3. Gateway Application 创建页允许用户自由填写四个作用域字段，Gateway 数据库因此可能写入 DDC 中不存在的组合；
4. Gateway Application 列表接口返回全部记录，没有按当前 DDC 作用域过滤；
5. 页面请求虽携带作用域，部分后端列表接口并未消费这些查询参数；
6. Gateway Application 当前把 `namespace` 存在自身表中，容易被误当成物理应用身份；
7. DDC 的 namespace-env-app 绑定接口已经支持四个可选筛选条件，但 Gateway 使用的
   `DdcManagementClient` 尚未暴露该只读目录能力；
8. Gateway Admin 和 Gateway Engine 已分别以 `infra/ga`、`infra/ge` 为 DDC 应用身份，
   但 Gateway Web 的硬编码作用域无法展示这套 DDC 主数据。

### 2.2 本地现状说明

本地 DDC 已存在 `retail/default/local/order` 和 `retail/ops/local/order` 等有效绑定；Gateway 中已有
`retail/order/local/default` 应用记录。当前首页使用硬编码默认作用域时指标为空，而选择与 DDC 绑定
一致的 `retail/default/local/order` 后可以看到 Gateway 数据。初始作用域解析必须优先找到这类
“DDC 有绑定且 Gateway 已接入”的组合。

`ge` 下出现的 `egon-cola-gateway-engine` 与 `egon-gateway-rpc` 不是两份 Gateway 应用：前者是
Engine 的 HTTP 服务名，后者是同一 Engine 进程暴露给 RPC Consumer 的 RPC Gateway 服务名。
两者可以是不同 `serviceKind/protocol/serviceName` 的实例目录项，但共同属于 DDC 应用 `infra/ge`。

## 3. 目标与非目标

### 3.1 目标

1. Gateway Admin 所有可选作用域只来自 DDC 已启用的 namespace-env-app 绑定；
2. Gateway Web 按 `biz → namespace → env → app` 逐级选择，并且永远只产生 DDC 中真实存在的完整元组；
3. 同一物理应用可绑定多个 namespace，在每个绑定的 namespace 下都能看到同一份 Gateway 应用、
   接口目录和凭据；
4. Gateway Application 创建只能使用当前 DDC 绑定，后端必须再次校验，不能相信前端；
5. Gateway Application、接口目录和 Provider 视图按当前 DDC 作用域展示；
6. Gateway Admin 和 Gateway Engine 的 DDC 身份分别稳定为 `infra/ga` 与 `infra/ge`，本地环境使用
   DDC 中存在的 `namespace=default, env=local` 绑定；
7. DDC 不可用、没有有效绑定或旧 Gateway 数据无法匹配 DDC 时给出明确状态，不伪造硬编码回退。

### 3.2 非目标

1. 本次不实现“用户/角色可访问哪些 namespace”的 RBAC 授权规则；但所有作用域读取经 Gateway Admin，
   为后续按当前用户过滤 namespace 保留唯一入口；
2. 本次不把 DDC 主数据复制到 Gateway 数据库，也不增加定时同步任务；
3. 本次不重构 Gateway Group 的引擎分组模型，不改变 Release、路由规则或 Engine 选组语义；
4. 本次不把 `egon-cola-gateway-engine` 和 `egon-gateway-rpc` 合并为同一个服务名；它们是同一应用下
   不同协议用途的服务；
5. 本次不删除、不静默改写现有 Gateway Application、目录、凭据或发布数据。

## 4. 强制身份规则

### 4.1 DDC 可见作用域

一个可选择的 DDC 作用域是已启用绑定：

```text
(bizCode, namespaceCode, env, appCode)
```

`namespace` 表示可见性和管控边界，不是物理应用副本。DDC 可将同一个 App 绑定到同一业务域下的多个
namespace，也可在不同环境建立绑定。

### 4.2 Gateway 物理应用身份

Gateway Application 的物理唯一身份是：

```text
(bizCode, env, applicationCode)
```

其中 `applicationCode` 对应 DDC `appCode`。`namespace` 不参与物理唯一身份。于是：

```text
retail/default/local/order ─┐
                            ├─ 同一份 Gateway Application(order, local)
retail/ops/local/order ─────┘   同一份 Catalog / Credential / Application ID
```

用户切换 namespace 只改变“这份应用是否在当前管控边界可见”，不得复制 Gateway Application、接口目录、
凭据或发布资源。

### 4.3 Gateway 自身的 DDC 身份

| 进程 | DDC biz | DDC app | 本地 env | DDC 可见绑定 | 服务目录语义 |
|---|---|---|---|---|---|
| Gateway Admin | `infra` | `ga` | `local` | `default` | Admin HTTP Provider / Config Client（按启用能力） |
| Gateway Engine | `infra` | `ge` | `local` | `default` | Engine Lease、HTTP Provider、RPC Gateway Provider |

部署环境允许通过现有环境变量覆盖 `env/namespace`，但覆盖后的完整可见元组必须预先存在于 DDC。物理服务
注册身份仍为 `biz + env + app + serviceKind + protocol + serviceName + group + version`，namespace 不进入
物理服务键。默认 `biz/app` 不再退回旧的 Gateway 业务应用值。`ge` 下的多个 serviceName 用
`serviceKind + protocol + serviceName` 区分，不创建第二个 DDC App。

## 5. 方案选择

### 5.1 未选择：Gateway Web 直接调用 DDC

该方案要求浏览器同时处理 Gateway JWT 与 DDC HMAC/管理凭据，并引入跨域和双重权限边界。它还会绕过
未来 Gateway Admin 按用户 namespace 授权的入口，因此不采用。

### 5.2 未选择：同步 DDC 主数据到 Gateway 数据库

复制业务域、namespace、环境、应用及绑定会产生双主、同步延迟和删除/禁用竞态。用户看到的作用域可能
已经不是 DDC 当前事实，因此不采用。

### 5.3 选择：Management Client Adapter + Gateway Scope Facade

在现有 `DdcManagementClient` 上增加只读 scope binding 查询能力，继续复用现有 HTTP/HMAC Adapter；
Gateway Admin 增加一个小型 Scope Facade，将 DDC 有效绑定与 Gateway Application 物理身份做实时连接，
再向 Web 返回统一目录。

这里使用两个与现有架构一致的模式：

- **Adapter**：`HttpDdcManagementClient` 封装 DDC HTTP 路径、签名和响应格式，Gateway 业务服务不拼接
  DDC URL，也不理解 HMAC；
- **Facade**：Gateway Scope Facade 统一完成“读取 DDC、过滤启用绑定、匹配 Gateway 物理应用、稳定排序”
  的编排，避免 Controller 和多个页面分别复制判断逻辑。

不引入 Strategy、同步框架或新的领域层。当前变化点只有一个权威目录和一套固定匹配规则，继续拆分会增加
无实际收益的抽象。

## 6. 后端组件与契约

### 6.1 DDC Management Client 扩展

`DdcManagementClient` 增加只读方法，查询参数均可选：

```java
List<DdcManagementScopeBinding> getScopeBindings(
        DdcManagementScopeQuery query);
```

模型至少包含：

```text
bindingId, bizCode, namespaceCode, env,
appId, appCode, appName, enabled
```

DDC Admin 在现有签名管理边界增加只读端点，HTTP Adapter 调用：

```http
GET /api/v1/ddc/openapi/management/scope-bindings
    ?bizCode=&namespaceCode=&env=&appCode=
```

该端点复用 `DdcNamespaceEnvAppBindingService` 的只读查询语义，不复制第二份查询实现。现有面向 DDC Web 的
`/api/v1/ddc/namespace-env-app-bindings` 保持不变。空白条件不得发送为有值参数；零个或任意子集条件都合法。
返回只读数据，不在 Gateway 中创建 DDC 主数据。现有 HMAC 签名、超时和
`DdcManagementClientException` 处理方式保持不变。

为避免给现有测试 Stub 造成无关破坏，新接口可以像 `findConfig` 一样提供抛出
`UnsupportedOperationException` 的默认实现；Gateway Scope Facade 必须把“不支持”和“DDC 调用失败”
转换成明确的作用域加载错误，不能回退静态候选值。

### 6.2 Gateway Scope Catalog API

Gateway Admin 新增只读接口：

```http
GET /api/v1/gateway/admin/scopes
```

要求 `gateway:read` 能力，响应为按 `bizCode, namespaceCode, env, appCode` 稳定排序的有效绑定：

```json
[
  {
    "bindingId": "...",
    "bizCode": "retail",
    "namespace": "default",
    "env": "local",
    "appCode": "order",
    "appName": "Order",
    "connected": true,
    "gatewayApplicationId": "..."
  }
]
```

Facade 只返回 `enabled=true` 的 DDC 绑定。`connected` 的计算只使用物理身份
`bizCode + env + appCode`；不比较 Gateway Application 表中的 legacy `namespace`。同一 Gateway
Application 因多个 DDC 绑定可在响应中出现多次，但每行指向相同 `gatewayApplicationId`。

当前接口暂不按用户裁剪 namespace。后续 RBAC 接入只需在 Facade 返回前增加授权过滤，不改变 Web 或
DDC 契约。

### 6.3 Gateway Application 查询

Gateway Application 列表接受当前完整 DDC 作用域：

```http
GET /api/v1/gateway/admin/applications
    ?bizCode=retail&namespace=default&env=local&appCode=order
```

四个条件同时提供时，服务先确认该完整元组是 DDC 有效绑定，再按物理身份
`bizCode + env + applicationCode` 返回零或一份 Gateway Application。namespace 只参与 DDC 可见性校验，
不参与数据库应用匹配。

为保持已有管理和测试调用兼容，不带任何作用域条件时仍允许返回全量列表；部分条件必须按已提供条件过滤，
不得因条件缺失抛内部异常。Web 的常规页面始终发送完整当前作用域。

### 6.4 Gateway Application 创建与复用

创建页面从当前 DDC 绑定继承只读的 `bizCode/namespace/env/appCode`。用户只填写 Gateway 自有信息：

- `displayName`；
- `description`；
- 创建后按现有流程签发或管理 credential。

创建请求仍携带完整作用域用于后端校验，但 Controller/Service 必须：

1. 调用 Scope Facade 确认元组为当前 DDC 已启用绑定；
2. 用 `bizCode + env + appCode` 查找现有 Gateway Application；
3. 已存在时返回明确的“应用已接入”冲突及现有 applicationId，前端跳转到现有应用，不创建副本；
4. 不存在时只创建一份应用；记录中的 `namespace` 仅保存首次接入上下文，不再作为可见性依据；
5. 审计事件同时记录请求绑定 ID 和完整 DDC 作用域，便于追溯首次从哪个 namespace 接入。

为了在并发请求下保证“一份物理应用”，实施阶段增加且只增加一个新的 Gateway Flyway 迁移：

- 保留 `namespace` 列，避免破坏既有实体和历史数据；
- 将旧唯一约束从 `(application_code, env, namespace)` 调整为
  `(biz_code, application_code, env)`；
- 建索引前检测历史物理身份冲突；发现冲突时迁移明确失败，不自动删除、合并目录或凭据；
- 不修改任何已有迁移文件。

### 6.5 目录、凭据和 Provider 可见性

接口目录与凭据继续以 `gatewayApplicationId` 为聚合归属，不新增 namespace 副本。页面只能从当前作用域匹配到的
Gateway Application 进入其目录和凭据详情。

Provider 目录查询使用 DDC 的“namespace 可见视图 + 物理服务”规则，其中：

- DDC 绑定决定当前 namespace 是否有权展示该应用；
- Provider 物理服务和实例不包含 namespace；只部署一次的实例通过多个 DDC binding 在多个 namespace
  视图中展示，不能要求实例的 legacy namespace 等于当前选择值；
- `serviceId` 是物理服务键的稳定哈希，同一服务的多个无状态实例共享同一个 `serviceId`；每个运行实例以
  不同 `instanceId`（以及当前租约的 `leaseId`）区分。Starter 优先使用显式配置或自定义 Provider，未配置时
  生成 UUIDv7，不使用可能因容器、虚拟网卡或镜像克隆而碰撞的 MAC 哈希；
- `serviceKind + protocol + serviceName + group + version` 区分同一 DDC App 下的不同服务，例如
  `egon-cola-gateway-engine` 与 `egon-gateway-rpc`。

Gateway Group 继续使用现有 env/namespace 分组和 API。顶部 DDC 作用域可为现有查询提供 env/namespace，
但本次不修改 Gateway Group 表结构或物理唯一规则。

## 7. Gateway Web 状态模型

### 7.1 级联顺序

顶部选择器固定为：

```text
业务域 biz → 命名空间 namespace → 环境 env → 应用 app
```

候选项完全从 Scope Catalog 响应派生：

1. biz 列出所有有效绑定中的业务域；
2. namespace 只列出当前 biz 下的 namespace；
3. env 只列出当前 biz + namespace 下的环境；
4. app 只列出当前 biz + namespace + env 下的应用；
5. 上级变化时，下级保留仍合法的值，否则依次选择该分支第一个稳定排序值；
6. 不允许在状态中短暂拼出目录中不存在的完整元组后触发页面请求。

`Scope` 类型可继续保存 `bizCode/appCode/env/namespace` 字段，展示顺序与解析算法按 DDC 层级调整，
无需仅为字段排列做无意义重命名。

### 7.2 初始作用域优先级

Scope Catalog 加载成功后，按以下顺序选择第一条仍有效的完整绑定：

1. `localStorage` 保存的上次选择；
2. `VITE_GATEWAY_ADMIN_DEFAULT_*` 部署配置指定的完整作用域；
3. 第一条 `connected=true` 的 DDC 绑定；
4. 第一条有效 DDC 绑定。

每一层候选都必须与 Scope Catalog 做完整元组相等校验；无效配置直接跳过，不拼凑各字段。当前本地数据按
该规则应选择 `retail/default/local/order`，而不是硬编码的空 Dashboard 作用域。

选择变化后只保存完整有效元组。DDC 后续禁用绑定时，刷新目录会重新执行上述校验并选择下一个合法值。

### 7.3 加载、空状态与失败状态

- 首次加载目录期间不请求 Dashboard、Application、Catalog 或 Provider 数据；
- DDC 返回空目录时显示“DDC 暂无已启用的 namespace-env-app 绑定”；
- DDC 超时、不支持该 Management API 或返回错误时显示“DDC 作用域加载失败”及重试入口；
- 不能用 `default/default-app/dev/default` 或当前字符串临时补进候选项；
- 某绑定存在但 `connected=false` 时可正常选中，并显示“该 DDC 应用尚未接入 Gateway”，允许有写权限的用户
  从当前绑定创建/接入 Gateway Application。

## 8. 数据兼容与未匹配记录

### 8.1 Legacy namespace 字段

现有 `gateway_application.namespace` 保留为“首次接入上下文”，不是可见性权威。所有新查询、页面过滤和复用判断
必须使用 DDC Binding + Gateway 物理身份，不再使用该列判断应用是否能在某 namespace 下出现。

### 8.2 未匹配 DDC 的 Gateway 记录

全量管理视图中，找不到任一有效 DDC 绑定的现有 Gateway Application 标记为“未匹配 DDC”。它们：

- 不出现在顶部 Scope Catalog 候选项；
- 不被删除或自动重写；
- 其 Catalog、Credential 和历史审计继续保留；
- 只有在 DDC 增加对应绑定后才进入普通作用域视图；
- 不得被当作初始作用域的 `connected=true` 候选。

### 8.3 历史物理身份冲突

若迁移前存在多行相同 `(bizCode, env, applicationCode)`、不同 legacy namespace 的 Gateway Application，
系统不能猜测应保留哪份目录或凭据。部署前置检查必须列出冲突 ID，迁移失败并要求人工合并；本任务不做有损
自动合并。

## 9. 错误处理与安全边界

1. DDC 4xx/5xx、超时、签名失败和响应解析错误统一映射为明确的 Gateway Scope upstream 错误；
2. Gateway Web 不接收、存储或展示 DDC access key/secret key；
3. Gateway Admin 到 DDC 继续使用现有 HMAC Management Client；
4. 创建时 DDC 绑定已被禁用或删除，返回业务冲突，不创建孤立 Gateway Application；
5. 列表的可选筛选条件缺失只表示扩大结果集，不能转为 500；非法组合返回空结果或明确 4xx，不能包装成
   `DDC_INTERNAL_FAILURE`；
6. 日志记录 traceId、DDC endpoint 类别和失败阶段，不记录 HMAC secret、JWT 或 credential secret。

## 10. 测试策略

实施遵循测试先行，覆盖以下最小闭环。

### 10.1 DDC Starter / Admin

- Management Client 正确构造零个、单个和多个可选 scope 查询参数；
- HMAC 请求包含 `/api/v1/ddc/openapi/management/scope-bindings` 路径；
- DDC 绑定列表在缺少任意筛选条件时正常返回匹配子集；
- enabled、排序和响应映射正确；
- upstream 错误保留可诊断状态而不降级为空列表。

### 10.2 Gateway Admin

- Scope Facade 只返回 DDC enabled 绑定；
- 两个 namespace 绑定到同一物理应用时返回两个 scope、同一个 applicationId；
- legacy namespace 不影响物理匹配；
- 创建时拒绝不存在/已禁用 DDC 绑定；
- 从第二个 namespace 重复创建时复用/指向已有应用，不生成第二份 Catalog 或 Credential；
- Application 列表支持完整、部分和无筛选条件；
- DDC 不可用时返回明确 upstream 错误；
- `infra/ga` 与 `infra/ge` 的本地配置及 HTTP/RPC 服务名契约不回归；
- 新 Flyway 迁移从 V1..V5 的既有状态升级成功，并能拒绝历史物理身份冲突。

### 10.3 Gateway Web

- 级联顺序和候选裁剪为 biz → namespace → env → app；
- 上级切换后不会产生无效完整元组；
- 初始优先级依次为 last-valid、configured-valid、connected、first-valid；
- 无效 localStorage 和无效部署默认值被跳过；
- DDC 空目录与失败状态不使用硬编码回退；
- Application 创建表单四个作用域字段只读；
- 同一 applicationId 在多个 namespace 视图中进入同一 Catalog/Credential；
- 页面请求携带当前完整作用域。

### 10.4 本机联调

完成代码后使用用户现有本机 PostgreSQL、Redis 与项目配置，依次重启 DDC Admin、Gateway Admin、
Gateway Engine 和一组后端 Provider。验证：

1. DDC 中 `infra/default/local/ga`、`infra/default/local/ge` 可见；
2. `ge` 下 HTTP Engine 与 RPC Gateway 作为两个服务目录项、一个 DDC App 展示；
3. Gateway Web 自动进入 `retail/default/local/order` 或当时第一条已接入绑定；
4. Dashboard、Application、Catalog、Provider 均按当前 DDC scope 返回数据；
5. 切换到 `retail/ops/local/order` 仍看到同一 Gateway Application ID 与同一目录/凭据；
6. 启动两个相同无状态订单实例时，DDC 以同一 `serviceId`、不同 `instanceId/leaseId` 展示两条实例，
   Gateway 仍归属同一 App；
7. 不完整 DDC 筛选和 Gateway 列表筛选不产生 500；
8. DDC 暂停时 Gateway Web 给出可恢复的作用域加载错误，恢复后重试成功。

## 11. 验收标准

以下条件全部满足才算完成：

- [ ] Gateway 顶部不再包含任何硬编码 scope 候选；
- [ ] 顶部顺序为 biz → namespace → env → app，所有完整选择均来自 DDC enabled binding；
- [ ] 本地首次打开 Gateway Admin 不再落入空的 `default/default-app/dev/default`；
- [ ] 同一 `(biz, env, app)` 在多个 namespace 下指向同一 Gateway Application ID；
- [ ] 多 namespace 不复制 Catalog、Credential 或 Gateway Application；
- [ ] 创建 Gateway Application 时 scope 只读且后端二次校验；
- [ ] 现有未匹配记录保留并明确标识，不参与 scope 候选；
- [ ] Gateway Admin 为 `infra/ga`，Gateway Engine 为 `infra/ge`，本地绑定与 DDC 一致；
- [ ] `egon-cola-gateway-engine` 与 `egon-gateway-rpc` 被展示为 `ge` 下不同服务，而非两个 App；
- [ ] DDC/Gateway 可选筛选缺失不会产生内部错误；
- [ ] 后端、Web、迁移测试通过；
- [ ] 本机服务重启后完成 DDC → Gateway Admin → Gateway Engine → Provider 的实时验证。

## 12. 实施顺序与回滚边界

后续实施计划按以下依赖顺序拆分并逐项提交：

1. 扩展 DDC Management Client scope binding 只读契约与测试；
2. 增加 Gateway Scope Facade/API、物理身份查询和测试；
3. 增加唯一身份 Flyway 迁移及升级测试；
4. 调整 Gateway Application 创建、筛选、目录/Provider 可见性；
5. 重写 Gateway Web scope 加载、初始选择、级联和错误状态；
6. 对齐本地身份配置、构建并重启服务进行联调。

若上线后需要回滚应用代码，新增唯一索引仍与旧代码兼容：旧代码只会得到更严格的重复保护。若确需回滚数据库
约束，应另建后续 Flyway 迁移，禁止修改或删除已经执行的新迁移文件。
