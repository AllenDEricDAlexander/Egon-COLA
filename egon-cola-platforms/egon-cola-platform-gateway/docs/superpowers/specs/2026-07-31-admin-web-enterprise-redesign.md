# Egon Gateway Admin Web — 企业级前端重构设计

**日期**: 2026-07-31  
**状态**: 设计完成，待实现  
**范围**: `egon-cola-platform-gateway-admin-web` 全模块

---

## 目标

将 admin-web 从基础功能性页面升级为 Ant Design Pro 经典风格的企业级管理后台。

**约束**：
- 不引入新依赖包（零新增包）
- 纯 antd 6 原生组件实现 Pro 级质感
- 不改变现有路由结构和后端 API 契约
- 保持现有 TypeScript + React 19 + Vite 技术栈

---

## 架构：三层改造

| 层 | 内容 | 涉及文件 |
|------|------|----------|
| **基础层** | 布局、主题、设计 tokens、通用 hooks | `AdminLayout.tsx`, `App.tsx`, `index.css`, 新增 `hooks/useTheme.ts`, `hooks/useTableConfig.ts` |
| **组件层** | 可复用的企业级组件 | 新增 `components/PageHeader.tsx`, `components/StatCard.tsx`, `components/PageSkeleton.tsx` |
| **页面层** | 逐页升级 | 10 个 `features/*` 页面 |

---

## 一、布局与导航系统

### 改动点

**1.1 Header 用户下拉菜单**
- 文件: `AdminLayout.tsx`
- 当前：纯文字 `{auth.session?.displayName}` + "退出" Button
- 目标：`Dropdown` + `Button` 触发器，内含 `Avatar` + 用户名 + 下拉箭头
- 下拉项：`个人设置`（disabled/预留）、`切换主题`、`退出登录`（Popconfirm）

**1.2 自动面包屑**
- 文件: 新增 `components/PageHeader.tsx`（集成面包屑功能）
- 用 `useLocation()` 解析路径，映射为中文层级
- 路径映射表：

| 路径片段 | 中文 |
|---------|------|
| `/dashboard` | 运行总览 |
| `/gateway-groups` | Gateway Group |
| `/gateway-groups/:id/overview` | 概览 |
| `/gateway-groups/:id/draft/routes` | Draft · Routes |
| `/gateway-groups/:id/draft/policies` | Draft · Policies |
| `/gateway-groups/:id/releases` | 发布历史 |
| `/gateway-groups/:id/releases/:rid` | Release 详情 |
| `/applications` | Application / Credential |
| `/interface-catalog` | 接口目录 |
| `/operations/:id` | Operation 详情 |
| `/providers` | Provider |
| `/observability/traces` | 调用观测 |
| `/audit` | 审计日志 |

- 面包屑最后一项不可点击（当前页），前面是可跳转的 `Link`

**1.3 环境/命名空间选择器**
- `window.confirm` 替换为 antd `Popconfirm`，包裹 `Select` 组件
- 确认对话框内容："切换作用域会清空当前缓存和未保存表单，是否继续？"

**1.4 页脚**
- 使用 antd `Layout.Footer`
- 内容：`© 2025 Egon Gateway · v{version}`（version 从 `package.json` 读取，构建时注入）

**1.5 Sider 菜单**
- 在 `navigation` 数组中添加分组分隔（`Menu.Divider` 方式）
- 分组：`核心管理`（Dashboard, Gateway Group, Application, 接口目录, Provider） + `观测审计`（调用观测, 审计日志）
- collapsed 时菜单项增加 `Tooltip`（antd 6 Menu 的 `inlineCollapsed` 模式原生支持）

**1.6 受影响文件**
- `layouts/AdminLayout.tsx` — 主要改动
- `styles/index.css` — 新增布局相关样式
- `app/App.tsx` — 如有全局配置变更

---

## 二、Dashboard 统计卡片与可视化

### 改动点

**2.1 StatCard 组件**
- 文件: 新增 `components/StatCard.tsx`
- 基于 antd `Card` + `Statistic`，封装为统一组件
- Props:

```ts
interface StatCardProps {
  title: string
  value: number | string
  suffix?: string
  precision?: number
  trend?: { direction: 'up' | 'down'; value: string }
  color?: 'default' | 'success' | 'warning' | 'error'
  loading?: boolean
}
```

- 颜色语义：顶部彩色边框（`Card` 的 `styles.body` 加 `border-top`）
  - `warning`: `inconsistentGroups > 0` 时
  - `error`: `abnormalProviders > 0` 时
  - `success`: 正常指标
- 底部 `trend` 展示：箭头图标（`ArrowUpOutlined` / `ArrowDownOutlined`）+ 百分比文字

**2.2 图表区**
- 请求量图表默认展示，表格默认折叠
- `Card` 的 `extra` 区域放 `Segmented` 控件切换 `图表 | 表格`
- Pie 图使用 `@ant-design/charts` 的 `statistic` 配置，中心显示总调用次数
- 移除原来的纯文本 `<ul>` 无障碍替代，改用图例

**2.3 快速入口卡片行（新增）**
- 接口目录入口 Card、Provider 管理入口 Card、最近发布 Card
- "最近发布" Card 使用 antd `List` 组件，通过 `useQuery` 获取最近 5 条 Release
- 每个入口 Card 底部带 "→ 前往" 链接按钮

### 受影响文件
- `components/StatCard.tsx` — 新增
- `features/dashboard/DashboardPage.tsx` — 主要重写

---

## 三、表格企业级增强

### 改动点

**3.1 useTableConfig hook**
- 文件: 新增 `hooks/useTableConfig.ts`
- 功能：
  - `density`: `'small' | 'middle' | 'large'`，localStorage 持久化
  - `visibleColumns`: `string[]`，列显隐控制，localStorage 持久化
  - `selectedRowKeys`: `React.Key[]`，批量选择状态
- 签名：

```ts
function useTableConfig(key: string, allColumns: string[]): {
  density: 'small' | 'middle' | 'large'
  setDensity: (d: Density) => void
  visibleColumns: string[]
  setVisibleColumns: (cols: string[]) => void
  selectedRowKeys: React.Key[]
  setSelectedRowKeys: (keys: React.Key[]) => void
  columns: ColumnsType  // 已过滤的列
}
```

**3.2 TableToolbar 模式**
- 不是独立组件，而是在每个页面中按需组装
- 模板结构：

```
<Card>  {/* 搜索区 */}
  <Form layout="inline">...</Form>
</Card>
<Space>  {/* 工具栏 */}
  <Segmented options={densityOptions} value={density} onChange={setDensity} />
  <Popover content={<Checkbox.Group options={allColumns} value={visibleColumns} onChange={setVisibleColumns} />}>
    <Button icon={<SettingOutlined />}>列设置</Button>
  </Popover>
  <Button icon={<DownloadOutlined />} onClick={handleExportCSV}>导出</Button>
</Space>
{selectedRowKeys.length > 0 && (
  <Alert type="info" message={`已选 ${selectedRowKeys.length} 项`} action={批量操作按钮} />
)}
<Table rowSelection={...} size={density} columns={filteredColumns} loading={...} />
```

**3.3 CSV 导出**
- 纯前端实现，工具函数放在 `utils/export.ts`
- 将当前表格数据 + 可见列导出为 CSV Blob，通过 `URL.createObjectURL` 下载
- 文件名格式：`{页面名}-{时间戳}.csv`

**3.4 各页面应用范围**

| 页面 | useTableConfig | 批量操作 | 搜索/筛选 | 导出 |
|------|:---:|:---:|:---:|:---:|
| Gateway Groups | ✓ | ✓ 批量启停 | ✓ | ✓ |
| Applications | ✓ | — | ✓ | ✓ |
| Providers | ✓ | — | — | ✓ |
| Draft Routes | ✓ | ✓ 批量删除 | ✓ | ✓ |
| Draft Policies | ✓ | ✓ 批量删除 | — | ✓ |
| Releases | ✓ | — | — | ✓ |
| Traces | ✓ | — | ✓（已有） | ✓ |
| Audit | ✓ | — | ✓（已有） | ✓ |

### 受影响文件
- `hooks/useTableConfig.ts` — 新增
- `utils/export.ts` — 新增（导出工具函数）
- `features/gateway-groups/GatewayGroupsPage.tsx`
- `features/applications/ApplicationsPage.tsx`
- `features/providers/ProvidersPage.tsx`
- `features/releases/ReleasesPage.tsx`
- `features/draft/DraftPage.tsx`
- `features/observability/TracesPage.tsx`
- `features/audit/AuditPage.tsx`

---

## 四、表单交互模式

### 改动点

**4.1 简单表单（≤8 字段）：保持 Modal**
- 适用页面：Gateway Group CRUD, Application CRUD, Policy, 三级目录, Operation
- 改动：
  - 增加 **"保存并新建"** 按钮：`Button` + `onClick` 触发保存后重置表单而非关闭
  - 表单校验失败时自动 `scrollToFirstError`
  - `Form` 的 `scrollToFirstError` 属性（antd 原生支持）

**4.2 复杂表单（Route 编辑）：Modal → Drawer + Steps**
- 文件：`features/draft/DraftPage.tsx` 中 Route 编辑部分
- `Modal` 替换为 `Drawer`（`width={720}`）
- 用 `Steps` 组件分三步：

```
Step 1: 基本信息
  — Route ID, Operation ID, Host, Access Zones, HTTP Method, Path Pattern

Step 2: Transport Policy（可跳过）
  — Profile, Transport Protocol, Request/Response Mode, Timeouts, WebSocket, Body Log, Retry
  — 当前 transportState.transportEditable === false 时自动跳过

Step 3: 高级 & 提交
  — 优先级, 高级 JSON, 启用, 变更原因
```

- 每步独立校验（`Form.validateFields` 传参指定字段）
- 操作按钮：`上一步` | `下一步` | `保存`
- 当前所有的表单校验逻辑（`validateTransportRoute`、`validatePublicRoute`、Operation Protocol 读取等）保持不变

**4.3 Policy 表单保持不变**
- Policy 表单字段适中（10 个左右），保持在 Modal 即可
- 增加 "保存并新建" 按钮

### 受影响文件
- `features/draft/DraftPage.tsx` — 主要改动
- `features/gateway-groups/GatewayGroupsPage.tsx` — "保存并新建"
- `features/applications/ApplicationsPage.tsx` — "保存并新建"
- `features/interface-catalog/CatalogPage.tsx` — "保存并新建"

---

## 五、加载体验升级

### 改动点

**5.1 PageSkeleton 组件**
- 文件: 新增 `components/PageSkeleton.tsx`
- 接口：

```ts
interface PageSkeletonProps {
  type: 'dashboard' | 'list' | 'detail'
}
```

- `dashboard` 型：Skeleton 模拟统计卡片行（4-6 个卡片）+ 图表区
- `list` 型：Skeleton 模拟搜索区 + 表格（表头 + 5 行）
- `detail` 型：Skeleton 模拟标题 + Descriptions + 内容区

**5.2 替换规则**

| 页面 | 当前 | 替换为 |
|------|------|--------|
| DashboardPage | `LoadingBlock` | `PageSkeleton type="dashboard"` |
| GatewayGroupsPage | `LoadingBlock` | `PageSkeleton type="list"` |
| ApplicationsPage | `LoadingBlock` | `PageSkeleton type="list"` |
| ProvidersPage | `LoadingBlock` | `PageSkeleton type="list"` |
| ReleasesPage | `LoadingBlock` | `PageSkeleton type="list"` |
| DraftPage | `LoadingBlock` | `PageSkeleton type="list"` |
| CatalogPage | `LoadingBlock` | `PageSkeleton type="detail"` |
| TracesPage | `LoadingBlock` | `PageSkeleton type="list"` |
| AuditPage | `LoadingBlock` | `PageSkeleton type="list"` |
| GatewayGroupDetailPage | `LoadingBlock` | `PageSkeleton type="detail"` |
| ReleaseDetailPage | `LoadingBlock` | `PageSkeleton type="detail"` |
| OperationPage | `LoadingBlock` | `PageSkeleton type="detail"` |

**5.3 表格内加载**
- 所有 `Table` 组件：`loading` 改为条件 `query.isFetching && !query.isLoading`
- 首次加载 → Skeleton；后续刷新 → Table 的 loading 进度条
- 已在数据展示时，不因为 refetch 而隐藏表格内容

**5.4 空状态**
- 每个页面检查 `data.length === 0`（或等效），显示 antd `Empty` 组件
- 空状态包含：插图（`Empty.PRESENTED_IMAGE_SIMPLE`）+ 上下文文案 + CTA 按钮（如有写权限）
- 例如 Gateway Groups 为空时："还没有 Gateway Group，创建第一个开始管理 API 网关" + `[新建 Gateway Group]`

### 受影响文件
- `components/PageSkeleton.tsx` — 新增
- `components/QueryState.tsx` — 可保留 LoadingBlock/EmptyBlock 作为 fallback
- 所有 features 页面 — 替换 LoadingBlock 为 PageSkeleton

---

## 六、视觉系统与设计 Token

### 改动点

**6.1 CSS 变量体系**
- 文件: `styles/index.css`
- 将所有硬编码颜色/间距值迁移到 CSS 变量
- 定义：

```css
:root {
  --color-primary: #3157d5;
  --color-primary-hover: #4265e0;
  --color-primary-active: #2548b8;
  --color-success: #52c41a;
  --color-warning: #faad14;
  --color-error: #ff4d4f;
  --color-info: #1677ff;
  --color-text-primary: #172033;
  --color-text-secondary: #697586;
  --color-text-tertiary: #94a3b8;
  --color-border: #e7eaf0;
  --color-bg-layout: #f4f6fa;
  --color-bg-container: #ffffff;
  --spacing-xs: 8px;
  --spacing-sm: 12px;
  --spacing-md: 16px;
  --spacing-lg: 24px;
  --spacing-xl: 32px;
  --spacing-2xl: 48px;
  --font-size-sm: 12px;
  --font-size-base: 14px;
  --font-size-lg: 16px;
  --font-size-xl: 20px;
  --font-size-2xl: 24px;
  --font-size-3xl: 30px;
  --shadow-card: 0 1px 3px rgba(0,0,0,0.04);
  --shadow-card-hover: 0 4px 12px rgba(0,0,0,0.08);
  --radius-sm: 4px;
  --radius-md: 8px;
  --radius-lg: 12px;
}
```

- 与 antd 6 `ConfigProvider` 的 `theme.token` 保持一致
- 深色模式用 `[data-theme="dark"]` 覆写

**6.2 PageHeader 组件**
- 文件: 新增 `components/PageHeader.tsx`
- 接口：

```ts
interface PageHeaderProps {
  title: string
  subtitle?: string
  breadcrumbs?: { label: string; path?: string }[]
  extra?: React.ReactNode
}
```

- 内部结构：
  - 顶部 `Breadcrumb`（如果提供 breadcrumbs）
  - `Typography.Title level={3}` 作为标题
  - `Typography.Text type="secondary"` 作为副标题（如果提供）
  - 右侧 `extra` 操作区

**6.3 排版层级规范**

| 用途 | 组件 | 尺寸 |
|------|------|------|
| 页面标题 | `Typography.Title level={3}` | 20px |
| 区块标题 | `Card title` 或 `Typography.Title level={4}` | 16px |
| 正文 | `Typography.Text` | 14px, `--color-text-primary` |
| 辅助说明 | `Typography.Text type="secondary"` | 14px, `--color-text-secondary` |
| 数据指标 | `Statistic valueStyle` | 数值突出 |
| 代码/ID | `Typography.Text code` 或 `copyable` | 14px monospace |

- 当前各页面使用的 `level={2}` 改为 `level={3}`，避免标题过大
- 也可以通过 `PageHeader` 组件统一

**6.4 间距规范**
- 页面内容区间距：`--spacing-lg` (24px)
- 卡片/表格之间的间距：`--spacing-md` (16px)
- 表单项之间的间距：antd Form 默认
- 使用 CSS 变量替代当前硬编码的 `margin-top: 16px` 等

### 受影响文件
- `styles/index.css` — 全面重构
- `components/PageHeader.tsx` — 新增
- `app/App.tsx` — 确认 `ConfigProvider` token 与 CSS 变量一致
- 所有 features 页面 — 采用 PageHeader 组件

---

## 七、深色模式

### 改动点

**7.1 useTheme hook**
- 文件: 新增 `hooks/useTheme.ts`
- 基于 `useState` + `localStorage`
- 接口：

```ts
function useTheme(): {
  mode: 'light' | 'dark'
  toggle: () => void
}
```

- 初始化从 `localStorage.getItem('theme-mode')` 读取
- `toggle` 写回 localStorage + 设置 `document.documentElement.setAttribute('data-theme', next)`

**7.2 ConfigProvider 集成**
- 文件: `app/App.tsx`
- 在 `ConfigProvider` 的 `theme` 中：

```ts
theme={{
  algorithm: mode === 'dark' ? antdTheme.darkAlgorithm : antdTheme.defaultAlgorithm,
  token: {
    colorPrimary: '#3157d5',
    borderRadius: 8,
    colorBgLayout: mode === 'dark' ? '#141414' : '#f4f6fa',
  },
}}
```

- 需要从 antd 导入 `theme` 对象：`import { theme as antdTheme } from 'antd'`

**7.3 CSS 变量覆写**
- 文件: `styles/index.css`
- 新增 `[data-theme="dark"]` 选择器，覆写关键变量：

```css
[data-theme="dark"] {
  --color-text-primary: #e8e8e8;
  --color-text-secondary: #a0a0a0;
  --color-text-tertiary: #666;
  --color-border: #303030;
  --color-bg-layout: #141414;
  --color-bg-container: #1f1f1f;
  --shadow-card: 0 1px 3px rgba(0,0,0,0.3);
  --shadow-card-hover: 0 4px 12px rgba(0,0,0,0.4);
}
```

- `.brand`、`.app-header`、`.login-shell` 等自定义类也要引用 CSS 变量或添加 `[data-theme="dark"]` 覆写

**7.4 主题切换入口**
- 文件: `AdminLayout.tsx` — Header 用户 Dropdown 中
- 图标：`SunOutlined`（浅色模式）/ `MoonOutlined`（深色模式）
- Tooltip："切换主题"

### 受影响文件
- `hooks/useTheme.ts` — 新增
- `app/App.tsx` — ConfigProvider 集成
- `styles/index.css` — 深色变量覆写
- `layouts/AdminLayout.tsx` — 切换入口

---

## 文件变更总览

### 新增文件
| 文件 | 类型 |
|------|------|
| `src/hooks/useTheme.ts` | Hook |
| `src/hooks/useTableConfig.ts` | Hook |
| `src/components/PageHeader.tsx` | 组件 |
| `src/components/StatCard.tsx` | 组件 |
| `src/components/PageSkeleton.tsx` | 组件 |
| `src/utils/export.ts` | 工具 |

### 修改文件
| 文件 | 改动幅度 |
|------|:---:|
| `src/styles/index.css` | 大 |
| `src/layouts/AdminLayout.tsx` | 大 |
| `src/app/App.tsx` | 中 |
| `src/features/dashboard/DashboardPage.tsx` | 大 |
| `src/features/draft/DraftPage.tsx` | 大 |
| `src/features/gateway-groups/GatewayGroupsPage.tsx` | 中 |
| `src/features/applications/ApplicationsPage.tsx` | 中 |
| `src/features/providers/ProvidersPage.tsx` | 中 |
| `src/features/releases/ReleasesPage.tsx` | 中 |
| `src/features/observability/TracesPage.tsx` | 中 |
| `src/features/audit/AuditPage.tsx` | 中 |
| `src/features/interface-catalog/CatalogPage.tsx` | 中 |
| `src/features/gateway-groups/GatewayGroupDetailPage.tsx` | 中 |
| `src/features/releases/ReleaseDetailPage.tsx` | 中 |
| `src/features/interface-catalog/OperationPage.tsx` | 中 |
| `src/auth/LoginPage.tsx` | 小 |
| `src/components/QueryState.tsx` | 小 |

### 不改动的文件
- 所有 `*.test.ts` / `*.test.tsx` — 实现完成后更新测试
- `src/api/` — API 层不变
- `src/auth/` — 认证逻辑不变（LoginPage 仅小改视觉）
- `src/hooks/useScope.tsx`, `scopeDefaults.ts` — 业务 hooks 不变

---

## 风险与对策

| 风险 | 对策 |
|------|------|
| antd 6 API 与设计中的假设不一致 | 实现前先验证关键 API（Drawer Steps、Skeleton 类型等） |
| DraftPage 表单逻辑复杂，重构容易引入 bug | 保持校验逻辑（routeValidation/routeTransport）不变，只改 UI 容器 |
| 深色模式 CSS 变量覆盖不完整 | 逐页测试，发现遗漏后补充变量 |
| 包体积增大（新组件） | 所有新组件均为轻量封装，无外部依赖 |

---

## 非目标（本期不做）

- 国际化（i18n）— 当前只有中文，暂不添加
- 移动端适配 — 管理后台以桌面端为主
- 实时数据推送（WebSocket）— 当前轮询已够用
- 通知中心 — 无后端支持
- 页面级权限颗粒度细化 — 当前 capability 体系已够
- E2E 测试重写 — 实现完成后再更新
