# DDC Admin 全量分页查询与前端现代化设计

状态：设计已确认，等待书面规格复核

编写日期：2026-08-10

代码基线：`main@a07bf181`

主要涉及模块：

- `egon-cola-components/egon-cola-component-common/egon-cola-component-common-core`（只复用现有分页契约，不修改）
- `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin`
- `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin-web`
- `egon-cola-platforms/egon-cola-platform-admin-web-shared`（只复用现有主题和页面状态组件，原则上不修改）

本文固化用户于 2026-08-10 确认的设计：DDC Admin 所有集合型管理查询都增加 `PageResultRecord` 分页接口，原接口保持兼容；DDC Admin Web 全面迁移到服务端分页，并使用现有 Ant Design、React Query 和 Admin Web Shared 能力完成响应式现代化。本文只定义目标、契约、数据边界、交互和验收条件，不是实施 Plan。书面规格经用户审核通过后，才编写逐任务实施 Plan；在此之前不修改生产代码。

---

## 1. 已确认的核心结论

1. 公共分页响应类型使用仓库已有的 `PageResultRecord<T>`，不新增 `ResultPageRecord` 或语义重复类型；
2. 分页范围采用全量范围 C：Metadata、配置、版本、发布任务、持久化实例、缓存检查、Registry 服务目录和 Registry 实例快照全部提供分页查询；
3. 兼容策略采用增量方案 A：新增 `/page`，保留所有现有 List、Catalog 和 Snapshot 接口及响应结构；
4. 作用域下拉框、Namespace 绑定编辑等需要完整选项的前端调用继续使用现有非分页接口；
5. DDC Starter、RPC DDC Adapter、Gateway 和其他机器调用方继续使用完整 RPC Facade 契约，不感知 Admin HTTP 分页；
6. 数据库型集合必须在 Repository 层执行真实分页和总数查询，不允许先查全量再 `subList`；
7. Registry 等 Redis/聚合型集合保持原完整领域快照，在专用 Admin 查询适配层稳定排序并切页；
8. DDC Admin Web 的所有管理表格改为受控服务端分页；
9. 前端复用现有 `antd`、`@ant-design/icons`、`@tanstack/react-query` 和 `@egon-cola/admin-web-shared`，不引入新的 UI 框架或状态库；
10. 本次不新增或修改数据库表结构，不修改任何已有 Flyway 文件，也不新增 Flyway 迁移；
11. 本次不改变写接口、权限能力、JWT、RPC、配置发布、Redis Topic、租约或缓存一致性语义；
12. 实施验证不自动启动 DDC、Redis、PostgreSQL、Gateway 或浏览器进程。

---

## 2. 当前现状与问题

### 2.1 后端集合接口全部返回完整结果

当前主要集合接口使用以下三种形态：

- `ResultRecord<List<T>>`：Metadata、配置、配置版本、发布任务、持久化实例和缓存检查；
- `ResultRecord<DdcManagementServiceCatalog>`：Catalog 内部包含完整服务键集合；
- `ResultRecord<DdcManagementServiceSnapshot>`：Snapshot 内部包含完整实例集合。

这些接口没有统一的 `pageNo`、`pageSize`、总数、总页数和前后页信息。随着 Metadata、发布任务、版本、实例和服务目录增长，Admin 页面必须先接收所有数据，再由 Ant Design Table 在浏览器内分页。

### 2.2 Metadata 存在全表过滤和 N+1

当前 Metadata 查询并不只是缺少响应分页：

- `DdcBizService`、`DdcNamespaceService` 的部分分支加载全量后过滤；
- `DdcEnvService` 的可见环境查询先加载全部环境，再与可见编码做内存过滤；
- `DdcAppService` 的 Namespace + Env 可见应用分支先得到完整可见列表，再做关键字过滤；
- `DdcNamespaceEnvAppBindingService.list` 先 `findAll()`，再为每条绑定分别查询 Namespace 和 App，形成明显 N+1；
- 上述内存过滤无法给出低成本、数据库一致的分页总数。

因此仅在 Controller 对现有 List 做 `subList` 不能解决问题。

### 2.3 同一 Metadata List 同时承担表格和选项职责

现有以下调用共享同一 List 接口：

- Biz、Env、App、Namespace 管理表格；
- `ScopeSelects` 四级联动下拉；
- 配置创建/编辑作用域选择；
- Namespace 绑定管理加载环境、应用和当前绑定。

如果直接把原 `GET` 响应从 `ResultRecord<List<T>>` 改为 `PageResultRecord<T>`，下拉和绑定弹窗会只得到第一页，同时造成前端类型和运行时结构不兼容。这是本设计保留原接口并新增 `/page` 的直接原因。

### 2.4 当前前端分页只是浏览器分页

当前页面普遍使用：

```tsx
pagination={{ pageSize: 10, size: 'small' }}
```

由此产生以下问题：

- Card 标题中的数量只是已加载数组长度，不是服务端总数；
- 切页不触发服务端查询；
- 筛选后 Table 可能停留在旧页码，显示空页；
- 首次加载没有一致的 pending 状态；
- 旧请求可能晚于新请求返回并覆盖筛选结果；
- 接口失败主要依赖 toast，页面没有稳定错误状态和重试入口；
- Metadata 修改后独立 Promise Cache 不会被统一失效，下拉可能继续显示旧数据。

### 2.5 当前 UI 的具体风险

- `AdminLayout` 使用固定 200px Sider、固定 24px Content 和无响应式处理；
- 页面存在大量重复 inline style，布局和间距不统一；
- 宽表没有完整的横向滚动和固定操作列策略；
- 多按钮操作列容易换行或撑宽页面；
- Config、Publish、Cache 使用 `window.confirm`，与 Ant Design 主题和可访问性不一致；
- 多处直接使用静态 `message`，没有统一使用 `App.useApp()` 上下文；
- Namespace 绑定弹窗使用全量 `Checkbox.Group`，应用增加后会产生严重换行和横向溢出；
- Registry 打开一个应用后会并发请求该应用的全部服务实例；
- Publish Task 每 15 秒全量刷新，失败时可能重复弹出错误消息；
- 固定 860px Modal/Drawer 在窄视口容易溢出。

---

## 3. 目标与非目标

### 3.1 目标

1. 为 DDC Admin 的所有集合型管理查询提供统一 `PageResultRecord<T>` 成功响应；
2. 保持当前非分页 REST 和 RPC 契约兼容；
3. 将数据库型集合查询迁移为真实数据库分页；
4. 消除 Binding 列表 N+1 和 Metadata 可见性查询的全量内存过滤；
5. 为所有分页查询定义稳定、可重复的排序；
6. 让 Admin Web 表格显示真实总数并正确响应查询、翻页和 pageSize 变化；
7. 统一前端 pending、empty、error、retry 和 mutation feedback；
8. 改善桌面和窄屏布局，防止表格、操作列、Modal、Drawer 和多选器溢出；
9. 优化 Registry 实例的按需加载和 Publish Task 当前页轮询；
10. 用自动化测试覆盖分页契约、筛选、稳定排序、兼容边界和关键 UI 状态。

### 3.2 非目标

- 不删除、重命名或改变现有 List、Catalog、Snapshot 接口；
- 不把下拉选项改成远程搜索分页；
- 不改变 DDC Starter Java 端口、RPC Protobuf 或 RPC Provider；
- 不修改 `DdcManagementFacade` 返回完整 Catalog/Snapshot 的领域语义；
- 不增加通用 BaseController、通用 CRUD Facade 或新的前后端框架；
- 不改变 RBAC capability、JWT、CSRF、CORS 或登录流程；
- 不改变配置文件 YAML-only、发布状态机、重试、ACK 或缓存重建语义；
- 不在本次加入新的数据库索引；
- 不追求品牌重设计、动画系统或与功能无关的大范围视觉改版；
- 不自动启动项目或使用浏览器完成运行时验收。

---

## 4. 公共分页契约

### 4.1 请求契约

所有新增分页接口接受仓库现有 `PageQuery`：

```text
pageNo: 从 1 开始，缺失或小于 1 时归一化为 1
pageSize: 缺失或小于 1 时归一化为 10，最大 500
```

前端允许用户选择的 pageSize 为：

```text
10 / 20 / 50
```

后端不接受任意字段排序参数。每个接口使用服务端固定排序，避免未校验属性名、跨数据库差异和翻页漂移。

### 4.2 成功响应

成功响应直接使用：

```java
PageResultRecord<T>
```

JSON 结构为：

```json
{
  "success": true,
  "code": 0,
  "status": "SUCCESS",
  "message": "success",
  "records": [],
  "page": {
    "total": 0,
    "pageNo": 1,
    "pageSize": 10,
    "pages": 0,
    "hasNext": false,
    "hasPrevious": false
  },
  "traceId": null,
  "timestamp": 0
}
```

关键约束：

- `records` 永不为 `null`；
- 分页结果不再嵌套在 `ResultRecord.data` 中；
- `page.total` 是满足当前筛选条件的总记录数；
- 请求页码超过总页数时返回成功空页，不自动回退到最后一页；
- Controller 使用实际归一化后的 `pageNo/pageSize` 构建响应。

### 4.3 失败响应

现有 `DdcGlobalExceptionHandler` 继续返回 `ResultRecord<Void>`。本次不为了分页重写全局异常体系。

因此分页客户端必须按共享字段判断失败：

- HTTP 状态；
- `success`；
- `code/status/message/traceId`。

只有成功响应才要求存在 `records/page`。这样既复用现有全局错误语义，又避免同一异常在 List 和 Page 接口中出现两套映射。

---

## 5. 分页接口矩阵

### 5.1 Metadata

| 新接口 | 保留的筛选参数 | 记录类型 | 固定排序 |
|---|---|---|---|
| `GET /api/v1/ddc/bizs/page` | `keyword` | `DdcBizEntity` | `bizCode ASC, id ASC` |
| `GET /api/v1/ddc/envs/page` | `bizCode, namespaceCode, keyword` | `DdcEnvEntity` | `sortOrder ASC, envCode ASC, id ASC` |
| `GET /api/v1/ddc/apps/page` | `bizCode, namespaceCode, env, keyword` | `DdcAppEntity` | `bizCode ASC, appCode ASC, id ASC` |
| `GET /api/v1/ddc/namespaces/page` | `bizCode, keyword` | `DdcNamespaceEntity` | `bizCode ASC, namespaceCode ASC, id ASC` |
| `GET /api/v1/ddc/namespace-env-app-bindings/page` | `bizCode, namespaceCode, env, appCode` | `DdcNamespaceEnvAppBindingVO` | `bizCode, namespaceCode, env, appCode, id ASC` |

原 `GET` 接口保持不变，继续用于完整选项和兼容调用。

### 5.2 Config、Version 和 Publish Task

| 新接口 | 筛选参数 | 记录类型 | 固定排序 |
|---|---|---|---|
| `GET /api/v1/ddc/configs/page` | 现有 `DdcConfigQueryRequest` 全部字段 | `DdcConfigVO` | `bizCode, env, appCode, resourceName, id ASC` |
| `GET /api/v1/ddc/configs/{id}/versions/page` | `id` | `DdcConfigVersionVO` | `version DESC, id DESC` |
| `GET /api/v1/ddc/publish-tasks/page` | `bizCode, env, appCode, status, changeId` | `DdcPublishTaskEntity` | `createdAt DESC, id DESC` |

Publish Task 新增的筛选仅作用于新分页接口，不改变原 List 行为。

### 5.3 Instance 和 Cache

| 新接口 | 筛选参数 | 记录类型 | 固定排序 |
|---|---|---|---|
| `GET /api/v1/ddc/instances/page` | 必填 `bizCode, env, appCode` | `DdcInstanceEntity` | `updatedAt DESC, id DESC` |
| `GET /api/v1/ddc/cache/check/page` | 必填 `bizCode, env, appCode` | `DdcCacheCheckRow` | `resourceName ASC` |

Cache Check 分页只对当前页的配置执行 Redis 读取和一致性比较，不能先计算全部 Check Row 再截断。

### 5.4 Registry Admin

| 新接口 | 筛选参数 | 记录类型 | 固定排序 |
|---|---|---|---|
| `GET /api/v1/ddc/registry/services/page` | 现有 services 查询参数 | `DdcManagementServiceKey` | scope、kind、protocol、serviceName、group、version、serviceId |
| `GET /api/v1/ddc/registry/instances/page` | 现有 instances 查询参数 | `DdcManagementServiceInstance` | status、host、port、instanceId |

Registry Page 接口的 `records` 只承载表格需要的服务键或实例。完整接口仍保留：

- `GET /api/v1/ddc/registry/services` 返回 generation、observedAt 和完整 services；
- `GET /api/v1/ddc/registry/instances` 返回 serviceKey、generation、observedAt 和完整 instances。

分页接口不改变或削弱完整 Snapshot 的机器契约。

### 5.5 不分页的接口

以下接口不属于集合分页范围：

- Auth bootstrap；
- 单实体详情；
- create/update/delete/enable/publish/rollback/retry/rebuild 等命令；
- RPC Provider 和 DDC Starter 机器端口。

---

## 6. 后端结构设计

### 6.1 Controller 边界

Controller 负责：

1. 接收原有业务筛选参数和 `PageQuery`；
2. 调用对应 Service 的分页方法；
3. 将 Spring Data `Page<T>` 转成 `PageResultRecord.success(...)`；
4. 保持原 List、Catalog、Snapshot 方法不变。

Controller 不负责：

- 构造数据库查询条件；
- 执行全量加载后过滤；
- 逐条补齐关联对象；
- 自己复制分页边界计算逻辑。

可增加一个 DDC Admin 内部小型 Page Support，用于：

- `PageQuery` 转换为零基 `PageRequest`；
- Spring `Page<T>` 转换为公共 `PageResultRecord<T>`；
- 对 Registry 等已稳定排序的聚合集合构造 `PageImpl<T>`。

该 Support 不是通用 CRUD Framework，不向其他模块暴露。

### 6.2 Service 和 Repository 边界

数据库型分页 Service 返回 `Page<T>` 或等价的 Admin 内部分页值，不返回外部 `PageResultRecord<T>`。外部响应类型保持在 Controller 边界。

Repository 设计：

- 简单字段组合使用 Spring Data derived query + `Pageable`；
- 多可选条件和可见性 Join 使用明确 `@Query`；
- native pageable query 必须同时提供语义一致的 `countQuery`；
- Binding 使用单次 Join 投影查询 `DdcNamespaceEnvAppBindingVO`，不得调用现有逐条 `toVO` 路径；
- 所有 order by 都包含唯一字段兜底；
- 不把未知前端 sort 字符串直接传给 JPA。

### 6.3 Metadata 可见性

Env 和 App 的 Namespace 可见性分页必须在数据库完成：

```text
namespace + binding + env/app
             |
             +-- enabled / biz / namespace / env 条件
             +-- keyword 条件
             +-- distinct
             +-- count distinct
```

`visibleApps`、`visibleEnvCodes` 等完整 List 方法继续服务原接口和机器逻辑；分页查询新增独立 Repository 路径，不通过全量 List 方法间接实现。

### 6.4 Config 和 Cache

Config 现有 native search 增加 Page 版本：

- 查询条件和 Namespace 可见性 `exists` 语义保持不变；
- content 查询与 count query 的 where 条件必须完全一致；
- `includeDeleted` 行为不变。

Cache Check Page 流程：

```text
物理作用域
  -> 查询已发布、可检查的 Config Item Page
  -> 只为当前 Page 查对应 published version
  -> 只为当前 Page 读取 Redis value/version
  -> 生成 DdcCacheCheckRow Page
```

总数是可检查的已发布配置数量，不是 Redis 命中数量。

### 6.5 Registry Admin 适配层

增加只属于 Admin HTTP 的分页查询适配职责：

```text
DdcRegistryAdminController
  -> Admin Registry Page Query Service
      -> DdcManagementFacade 完整 Catalog/Snapshot
      -> 固定排序
      -> Page 切分
      -> PageResultRecord
```

强制边界：

- 不修改 `DdcManagementClient`；
- 不修改 `DdcManagementFacade` 公共方法签名；
- 不修改 DDC RPC Provider、Proto Mapper 或 RPC DDC Adapter；
- 不让 Starter 依赖 `PageQuery` 或 `PageResultRecord`；
- Registry 分页只能减少浏览器响应和渲染量，不虚假宣称 Redis 完整快照读取已经变成游标扫描。

---

## 7. 前端数据层设计

### 7.1 类型和客户端

在 DDC Admin Web 定义与后端 JSON 对齐的：

```ts
type PageMetaRecord = {
  total: number
  pageNo: number
  pageSize: number
  pages: number
  hasNext: boolean
  hasPrevious: boolean
}

type PageResultRecord<T> = {
  success: boolean
  code: number
  status: string
  message: string
  records: T[]
  page: PageMetaRecord
  traceId?: string
  timestamp: number
}
```

新增 `ddcPageApi<T>()`，与现有 `ddcApi<T>()` 共享底层请求函数。共享职责包括：

- Authorization Header；
- 401 refresh 后单次重试；
- unauthorized handler；
- HTTP 与业务失败识别；
- `DdcApiError` 和 Trace ID；
- JSON 解析和网络错误；
- AbortSignal。

`ddcApi<T>()` 成功时读取 `data`；`ddcPageApi<T>()` 成功时返回 `records/page`。两者都必须正确处理全局异常返回的 `ResultRecord<Void>`。

### 7.2 React Query

根节点增加单一 `QueryClientProvider`。所有页面查询使用 React Query 管理服务端状态：

- query key 至少包含 resource、已提交筛选、pageNo 和 pageSize；
- 草稿筛选不直接触发请求，点击查询或 Form submit 后才提交；
- 提交新筛选和重置时将 pageNo 设为 1；
- pageSize 变化时将 pageNo 设为 1；
- 使用 `keepPreviousData`/等价能力避免翻页闪空；
- query function 接收并传递 AbortSignal；
- Publish Task 只轮询当前 query key；
- 后台轮询错误保留旧数据并显示非侵入状态，不重复 toast。

Mutation 成功后：

- 失效当前资源 Page query；
- Metadata 变更同时失效相关 scope option query；
- 不通过手工复制数组模拟服务端最终状态。

现有 `useScopeOptions` 的独立 Promise Map Cache 迁移为 React Query cache 或由 QueryClient 统一失效，避免新建/改名/启停后选项陈旧。

---

## 8. 前端页面与视觉设计

### 8.1 全局 Layout

采用现有 Admin Theme 的蓝灰企业控制台风格：

- Desktop：可折叠 `Layout.Sider`；
- Narrow：隐藏固定 Sider，使用 `Drawer` 导航；
- Menu 使用 `@ant-design/icons` 并分为运行状态、配置管理、元数据管理；
- Header 显示当前页面、DDC 连接状态、登录身份和退出操作；
- Header 固定在内容顶部；
- Content padding 在 Desktop 使用 24px，在窄屏使用 12px；
- 页面根容器设置 `min-width: 0`，表格自己横向滚动，不允许 Layout 被撑宽。

只增加布局和响应式所需的少量样式文件。颜色、圆角、字体和状态颜色继续来自 Admin Web Shared 与 Ant Design Token，不复制一套设计系统。

### 8.2 页面统一结构

每个管理页面按以下层级组织：

1. 页面标题、说明和主操作；
2. 查询条件 Card；
3. 可选的状态摘要；
4. 数据 Table Card；
5. Drawer/Modal 详情或编辑。

复用 `PageState` 提供：

- 首次 Skeleton/Spin；
- Empty；
- 可见的错误 Alert；
- Retry；
- 必要时保留旧数据并显示局部错误。

允许增加 DDC Web 内部的轻量 `PageHeader`、`QueryCard` 等展示组件，但不建立包含业务列和 CRUD 行为的万能泛型表格。

### 8.3 Table 统一交互

所有管理 Table：

- `pagination.current/pageSize/total` 受控；
- `showSizeChanger`；
- `pageSizeOptions=[10,20,50]`；
- `showTotal` 显示真实总数；
- 配置合理 `scroll.x`；
- 长文本使用 ellipsis、Tooltip、copyable 或 Drawer；
- 操作列固定右侧；
- 高频主操作保留按钮，低频操作进入 Dropdown；
- Row mutation 显示对应 loading，阻止重复提交；
- 时间、状态、代码、空值采用统一 renderer。

### 8.4 Metadata 页面

Biz、Env、App、Namespace：

- 查询条件使用 Ant Design Form；
- 支持 Enter 提交、查询和重置；
- Card 数量使用 `page.total`；
- 新建、编辑保留现有业务字段和校验；
- 启停 Switch 在请求期间禁用；
- 删除使用 `Popconfirm`；
- 成功后刷新当前有效页，若删除导致当前页越界，则回退到前一页。

Namespace Binding：

- 从固定宽 Modal 改为响应式 Drawer；
- 环境保持表格行；
- 应用选择改为 `Select mode="multiple"`；
- 使用 `maxTagCount="responsive"`、搜索和虚拟滚动；
- 不再将全部应用渲染为 Checkbox；
- 保存时保留当前 create/enable/delete 语义和最终刷新。

### 8.5 Registry 页面

当前“应用聚合表 -> 打开应用 -> 并发拉取所有服务实例”改为：

```text
服务目录分页 Table
  -> 单个服务行
      -> 打开实例 Drawer
          -> 该服务的实例分页 Table
```

服务目录列包括：

- biz/env/app；
- serviceKind/protocol；
- serviceName/group/version；
- serviceId；
- 查看实例操作。

实例只在 Drawer 打开后加载，切换实例页只请求当前服务当前页。页面统计显示服务总数和本页状态，不再把“当前抽屉在线实例数”作为全局统计。

### 8.6 Config 和 Version

- Config 主表迁移 `/configs/page`；
- Scope 筛选保持现有级联语义；
- 发布、删除改为 Ant Design `Popconfirm` 或 `Modal.confirm`；
- YAML 预览设置最大宽度和 ellipsis；
- 操作列收敛，避免多个按钮撑宽；
- Version 历史使用响应式 Drawer 和独立分页状态；
- Rollback 成功后关闭或刷新 Version Drawer，并刷新 Config 当前页。

### 8.7 Publish Task

- 增加 scope、status 和 changeId 筛选；
- 使用 `/publish-tasks/page`；
- 保留 15 秒轮询，但只刷新当前筛选和页码；
- 详情继续使用 Descriptions；
- Retry 只在允许的状态展示或启用；
- background error 不重复 toast；
- 状态 Tag 颜色统一。

### 8.8 Cache

- 查询保持 biz/env/app 完整物理作用域必填；
- 检查结果使用 `/cache/check/page`；
- 增加匹配/不匹配的当前页摘要；
- 重建缓存使用 Ant Design 确认交互；
- DB/Redis 长值不会撑开表格；
- 重建完成后允许自动重新检查当前页。

---

## 9. 设计模式与取舍

### 9.1 使用的模式

**Adapter**

`ddcPageApi<T>()` 适配 `PageResultRecord<T>`，Registry Admin Page Query Service 适配完整 Catalog/Snapshot 到分页展示模型。两处都隔离了不同协议/数据形态，而没有污染领域契约。

**Repository Query**

分页、过滤、count 和稳定排序归属 Repository；Service 负责业务筛选组合和映射；Controller 只组装 HTTP 响应。

### 9.2 明确不使用的模式

- 不建立通用 BaseController：各 Controller 的筛选、权限和记录语义不同；
- 不建立通用 CRUD Facade：当前问题是查询分页和展示，不是重构写模型；
- 不建立 Strategy 树：数据源种类固定，直接的 Repository Page 与 Registry Adapter 更清楚；
- 不使用前端万能 PagedTable：列、mutation、Drawer 和筛选差异大，强抽象会降低可读性。

---

## 10. 安全、兼容与分布式边界

1. 新 `/page` 接口位于现有 `/api/v1/ddc/**` 安全边界内，沿用当前 JWT capability；
2. 不降低任何写权限，也不把 HMAC/RPC 机器凭据暴露给浏览器；
3. 保留原接口意味着当前仓库外调用方不会因分页上线而立即破坏；
4. DDC Admin 多实例部署时，数据库分页依赖共享数据库，Registry 分页依赖共享 Redis 快照，符合现有 Active-Active 边界；
5. offset/pageNo 分页不是跨事务快照，数据持续变化时允许总数变化，但固定排序和唯一兜底保证单次响应确定；
6. Publish Task 和 Registry 高频变化页面以前端刷新当前页为准，不承诺跨页强一致快照；
7. 原完整 RPC Catalog/Snapshot 仍包含 generation/observedAt；分页 HTTP 表格不冒充完整机器快照。

---

## 11. 测试与验证设计

### 11.1 后端测试

Controller MockMvc：

- 每个新增 `/page` 的 `records` 和 `page` 契约；
- 默认 `pageNo/pageSize`；
- pageSize 最大值归一化；
- total/pages/hasNext/hasPrevious；
- 超出总页数返回成功空 records；
- 原 List/Catalog/Snapshot 响应仍使用 `data`；
- 缺失必填作用域继续返回现有失败契约。

Repository/Service：

- 每种筛选组合的 content 和 total 一致；
- 稳定排序和相同排序值的 id 兜底；
- Namespace 可见 Env/App 分页；
- Binding Join 投影正确且不走逐条查询路径；
- Config native query 与 countQuery 条件一致；
- Version 倒序；
- Publish Task 新筛选；
- Cache 只检查当前页；
- Registry Page 切分不修改完整 Facade 返回值。

回归：

- DDC Management RPC Provider 测试；
- RPC DDC Adapter 相关契约测试在受影响时执行；
- DDC Admin 安全测试；
- 原 Controller 测试。

### 11.2 前端测试

- `ddcPageApi` 成功、业务失败、HTTP 失败、401 refresh、网络失败和 Trace ID；
- pageNo/pageSize/筛选参数序列化；
- 查询和重置回到第一页；
- pageSize 改变回到第一页；
- Table total 使用服务端值；
- 首次 loading、empty、error、retry 和保留旧页数据；
- 连续查询取消旧请求；
- Metadata mutation 后列表和 scope option 失效；
- 删除最后一条后页码回退；
- Registry 实例只在打开单个服务时请求；
- Publish Task 轮询只刷新当前页且错误不产生 toast 风暴；
- Namespace 多选绑定不渲染全量 Checkbox；
- 窄屏时导航、Drawer 宽度和 Table scroll 属性；
- Popconfirm/Modal 替代原 `window.confirm`。

### 11.3 实施阶段验证命令类别

实施 Plan 必须给出精确命令，至少覆盖：

- DDC Admin 定向 Maven test/compile；
- DDC Admin Web Vitest；
- TypeScript typecheck；
- ESLint；
- Vite production build；
- Repository-wide residual scan，确认管理 Table 不再调用非分页列表；
- residual scan，确认 Starter/RPC 契约未引用 Admin 分页类型。

不把“页面可以启动”或根路径 HTTP 200 当作 UI 正确性的证据，也不在自动实施中启动服务或浏览器。

---

## 12. 实施分层与提交边界

后续 Plan 应拆为可独立验证、逐任务提交的最小任务，建议顺序：

1. 分页契约客户端测试和 Backend Controller 契约测试先行；
2. Metadata Repository/Service 真实分页和 Binding N+1 修复；
3. Config、Version、Publish Task、Instance、Cache 分页；
4. Registry Admin Page Adapter；
5. 前端 `ddcPageApi`、QueryClient 和分页查询基础设施；
6. Metadata 页面迁移和绑定 Drawer；
7. Config、Version、Publish、Cache 页面迁移；
8. Registry 服务/实例分页交互；
9. Admin Layout 与全局响应式统一；
10. 全量验证、残留扫描和文档同步。

每个任务只能提交其负责的代码和测试，不夹带无关工作区修改。

---

## 13. 验收标准

### 13.1 API

- 12 个新增 `/page` 查询全部返回 `PageResultRecord<T>` 成功结构；
- 现有 List、Catalog、Snapshot URL 和成功响应结构不变；
- 所有 Page 接口具有真实 total、固定排序和空页语义；
- 数据库型 Page 不通过全量 List + `subList` 实现；
- Binding 分页不再发生逐条 Namespace/App 查询；
- Registry RPC/Starter 契约没有分页类型或 HTTP Admin 依赖。

### 13.2 UI

- 所有管理表格使用服务端分页；
- 查询、重置、翻页、pageSize 和 mutation 后刷新行为正确；
- 页面展示真实总数；
- 初始加载、空数据、错误和重试均有稳定可见状态；
- 无 `window.confirm`；
- 无依赖静态 `message` 的新增页面逻辑；
- 窄屏不因 Sider、固定宽 Modal/Drawer、宽表、长文本或操作列产生页面级横向溢出；
- Namespace 绑定不再全量展示 Checkbox；
- Registry 不再在打开应用时并发加载全部服务实例；
- Publish Task 后台刷新不会重复弹出错误 toast。

### 13.3 质量

- 定向后端测试通过；
- 前端 Vitest、typecheck、lint、build 通过；
- 没有启动项目进程；
- 没有修改现有 Flyway 文件；
- 没有引入新依赖；
- 没有修改无关模块或混入无关工作区文件。

---

## 14. 风险与控制

| 风险 | 控制 |
|---|---|
| 原 List 被误改导致下拉只剩第一页 | 新增 `/page`，原接口契约测试锁定 |
| native query content/count 条件漂移 | 同文件并列维护并增加筛选总数测试 |
| Binding Page 仍有 N+1 | Repository Join 投影，测试不走逐条映射 |
| Registry 分页被误扩散到 RPC | 专用 Admin Adapter 和依赖/残留扫描 |
| 高频数据翻页漂移 | 固定排序并增加唯一 id/serviceId/instanceId 兜底 |
| 前端旧请求覆盖新条件 | React Query query key + AbortSignal |
| mutation 后页码越界 | 刷新后检测空页并回退一页 |
| 窄屏布局溢出 | responsive Sider/Drawer、`min-width: 0`、Table scroll 和组件测试 |
| 后台轮询错误打扰用户 | 保留旧数据、非侵入错误状态、禁止重复 toast |
| 改造范围过大导致一次提交难审 | 后续 Plan 按后端域和前端页面逐任务提交 |

---

## 15. 最终边界摘要

本设计形成两条明确且互不污染的读取链路：

```text
人工管理表格
  -> DDC Admin /page HTTP
  -> PageQuery
  -> DB Page 或 Admin 聚合 Page Adapter
  -> PageResultRecord

下拉选项 / 完整快照 / 机器调用
  -> 原 List HTTP 或 DDC RPC Facade
  -> 完整 List / Catalog / Snapshot
```

分页是 DDC Admin 管理面的展示与查询能力，不是新的 DDC 机器协议，也不改变 DDC 作为平台控制面的直接 RPC 架构。
