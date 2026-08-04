# IDP & RBAC3 前端企业级优化设计

**日期**: 2026-08-04
**版本**: 1.0
**状态**: 待审核

---

## 1. 目标

将 IDP 和 RBAC3 管理端 Web 前端提升为企业级水平，在现有 React 19 + antd 6 + TypeScript 6 技术栈基础上，建立统一基础设施，修复所有已知 Bug，重构架构缺陷，补齐企业级体验能力。

## 2. 方案

**方案 C: 统一基础设施先行**。新建共享包 `@egon-cola/admin-web-shared`，将两个项目的通用能力（OAuth、HTTP 客户端、JWT 工具、通用组件、设计 token、i18n）下沉到此包。两个消费项目逐步替换为共享包实现，从根本上消除代码重复和不一致。

## 3. 包依赖关系

```
@egon-cola/admin-web-shared
  ├── antd (peer)
  ├── react (peer)
  ├── react-dom (peer)
  ├── react-router-dom (peer)
  ├── @tanstack/react-query (peer)
  └── i18next + react-i18next (peer)

egon-cola-platform-idp-admin-web ──── depends on ────> @egon-cola/admin-web-shared

egon-cola-platform-rbac3-admin-web ── depends on ────> @egon-cola/admin-web-shared
                                                      + @egon-cola/rbac3-react-sdk
```

## 4. 共享包结构

```
egon-cola-platforms/
└── egon-cola-platform-admin-web-shared/
    ├── package.json
    ├── tsconfig.json
    ├── vite.config.ts
    └── src/
        ├── index.ts                        # 统一导出
        ├── auth/                           # OAuth 客户端
        │   ├── oauthClient.ts              # 以 IDP 实现为基础，修复 bug
        │   ├── oauthClient.test.ts
        │   ├── tokenStore.ts
        │   └── tokenStore.test.ts
        ├── api/                            # API 层
        │   ├── httpClient.ts               # 统一 fetch 封装
        │   ├── httpClient.test.ts
        │   ├── errors.ts                   # 错误分类体系
        │   └── jwt.ts                      # JWT 解码（TextDecoder 修复 UTF-8 bug）
        ├── components/                     # 通用组件
        │   ├── PageState.tsx               # 增强版加载/错误/空态
        │   ├── PageState.test.tsx
        │   ├── AppErrorBoundary.tsx        # 错误边界（含 telemetry 回调）
        │   ├── AppErrorBoundary.test.tsx
        │   ├── PageTemplate.tsx            # 页面模板（Card + breadcrumb + title）
        │   └── PageTemplate.test.tsx
        ├── hooks/                          # 通用 Hooks
        │   ├── useFeatureQuery.ts          # 消除 15 处 boilerplate（RBAC3）
        │   ├── usePermission.ts            # 权限判断 hook
        │   └── usePermission.test.ts
        ├── theme/                          # 设计系统
        │   ├── tokens.ts                   # 统一 design token
        │   └── ThemeProvider.tsx           # antd ConfigProvider 封装
        └── i18n/                           # 国际化
            ├── index.ts                    # initI18n / useT / I18nProvider
            ├── zh-CN.ts                    # 中文语言包
            └── en-US.ts                    # 英文语言包
```

## 5. Auth 模块详细设计

### 5.1 来源

以 IDP 项目 `oauthClient.ts` 为基础（代码质量是两个项目中最好的：DI 友好、PKCE S256 正确、已测试），修复已知 bug，增强后下沉。

### 5.2 Bug 修复清单

| Bug | 当前行为 | 修复方案 |
|-----|---------|---------|
| `decodeClaims` 的 `atob` UTF-8 破坏 | `btoa`/`atob` 处理非 ASCII 字符 → 乱码后 JSON.parse 抛错 | 统一用 `TextDecoder('utf-8')` / `TextEncoder` |
| refresh 用旧 nonce 校验新 token | `refresh()` 传入 `tokenStore.get()?.nonce` 作为 `expectedNonce` | refresh 时移除 nonce 校验，仅 authorization_code grant 阶段 |
| callback 失败后无恢复路径 | transaction 先删再校验 → 失败后不可重试 | 先校验 state/nonce/age 成功后再删 transaction；`callbackInFlight` 失败后清空允许重试 |
| `handleCallback` 丢弃 `error_description` | 任意错误 → "统一身份授权被拒绝" | 保留服务端返回的 `error_description` |
| `mustChangePassword` 被忽略 | 服务端返回了但客户端丢弃 | 在 login response 中保留字段，调用方按需处理 |
| JWT payload 解码重复实现 | `oauthClient.ts` 和 `tokenStore.ts` 各有一份 `atob` + `String.fromCharCode` | 合并为 `api/jwt.ts` 中的唯一实现 |

### 5.3 增强项

- **结构化错误类型**: `OAuthError` 类，包含 `code`、`description`，替代裸 `Error`
- **`TokenStore.subscribe` 启用**: `httpClient` 和 auth context 订阅 token 变化
- **OAuth 配置启动时校验**: 缺少必填 env var 时直接抛错，而非静默 fallback 到 localhost
- **Token 有效期主动检查**: `httpClient` 发请求前检查 `expiresAt`，快过期时先 refresh

### 5.4 接口

```typescript
export interface OAuthClient {
  beginAuthorization(tenantId: string, returnTo?: string): Promise<void>
  handleCallback(search: string): Promise<string>
  refresh(): Promise<string>
  revoke(): Promise<void>
}

export interface TokenStore {
  get(): AuthTokens | null
  set(tokens: AuthTokens): void
  clear(): void
  subscribe(fn: (tokens: AuthTokens | null) => void): () => void
}
```

## 6. API 层详细设计

### 6.1 来源

提取 IDP `idpApi` 和 RBAC3 `adminApiClient` 中的通用能力，修复已知问题，统一为 `HttpClient`。

### 6.2 接口

```typescript
export interface HttpClientConfig {
  readonly baseUrl: string
  readonly credentials: RequestCredentials    // 默认 'include'
  readonly onAuthError: () => Promise<string> // token 过期 → 注入 refresh
  readonly onFatalAuthError: () => void       // refresh 也失败 → 跳转登录
  readonly timeout?: number                   // 默认 30s
}

export interface HttpClient {
  request<T>(path: string, init?: RequestInit & { signal?: AbortSignal }): Promise<T>
}
```

### 6.3 Bug 修复清单

| Bug | 修复 |
|-----|------|
| 缺少 `credentials: 'include'`（IDP） | 通过 config 注入，默认 `'include'` |
| 401 重试链双重包装（RBAC3: adminApiClient + FeatureApi 各重试一次） | `httpClient` 单层 retry，promise 去重 |
| `204` 返回 `undefined as T` 类型说谎 | 分离 `requestOrEmpty` 方法 |
| 无请求超时 | 支持 AbortSignal 超时，默认 30s |
| `Content-Type` 盲设 `application/json` | 仅当 body 是 POJO 或 JSON string 时设置 |
| `tokenClaims`/`expiresIn` 未防御性解析（RBAC3） | 使用共享 `jwt.ts`，异常时不崩溃 |
| `tokenClaims` 不处理 `exp` 为 string（RBAC3） | `jwt.ts` 统一处理 number/string 两种 exp |

### 6.4 错误分类体系

```typescript
export class ApiError extends Error {
  readonly status: number
  readonly code: string
  readonly requestId?: string
  readonly retryable: boolean
}

export function classifyApiError(error: unknown): {
  type: 'auth' | 'permission' | 'validation' | 'server' | 'network'
  title: string
  retryable: boolean
}
```

### 6.5 JWT 工具（唯一实现）

```typescript
export function decodeTokenPayload(token: string): Record<string, unknown>
export function computeExpiresAt(token: string): Date | null
export function isTokenExpired(token: string): boolean
```

使用 `TextDecoder('utf-8')` 替代 `atob` + `String.fromCharCode`，正确支持非 ASCII 字符。

### 6.6 保留在项目内的 API 逻辑

- **RBAC3 `UnifiedRbac3ApiClient`**: 与 `@egon-cola/rbac3-react-sdk` 契约耦合，保留在 RBAC3 项目内，但底层改用共享 `HttpClient`
- **RBAC3 `FeatureApiProvider`**: 租户 header 注入逻辑保留在 RBAC3 项目内不变

## 7. 组件和 Hooks

### 7.1 PageState（增强）

来源：RBAC3 `features/shared/PageState.tsx`

```typescript
export interface PageStateProps {
  readonly loading: boolean
  readonly error: unknown
  readonly empty: boolean
  readonly emptyDescription?: string
  readonly skeleton?: ReactNode           // 新增：自定义骨架屏
  readonly errorRender?: (error: unknown) => ReactNode
  readonly showPartial?: boolean          // 新增：有错误但有陈旧数据时显示 banner 而非全屏错误
  readonly onRetry?: () => void           // 新增：错误时显示重试按钮
  readonly children: ReactNode
}
```

### 7.2 AppErrorBoundary（增强）

来源：RBAC3 `app/AppErrorBoundary.tsx`

```typescript
export interface AppErrorBoundaryProps {
  readonly onError?: (error: Error, info: ErrorInfo) => void  // telemetry hook
  readonly fallback?: ReactNode                                // 自定义崩溃页面
  readonly children: ReactNode
}
```

- `componentDidCatch` 调用 `onError` 回调（接入 Sentry/console）
- 默认 fallback 包含"刷新页面"按钮和错误摘要

### 7.3 PageTemplate（新增）

消除每页重复的 `<Card title=""> <PageState> <Table> </></>` 模式：

```typescript
export interface PageTemplateProps {
  readonly title: string
  readonly subtitle?: string
  readonly breadcrumbs?: BreadcrumbItem[]  // 面包屑
  readonly extra?: ReactNode               // Card extra 区域
  readonly pageState: Omit<PageStateProps, 'children'>
  readonly children: ReactNode
}
```

BRAC3 `RouteDescriptor` 增加 `breadcrumb` 字段。

### 7.4 useFeatureQuery（消除 RBAC3 中 15 处 boilerplate）

```typescript
export function useFeatureQuery<T>(
  keys: unknown[],
  queryFn: (api: FeatureApiClient) => Promise<T>,
  options?: { enabled?: boolean }
): UseQueryResult<T>
```

替代模式：自动合并 `tenantId` 到 queryKey、自动 gating `status === 'READY'`、自动调用 `useFeatureApi()`。

### 7.5 usePermission

```typescript
export function usePermission(): {
  has: (permission: string) => boolean
  hasAll: (...permissions: string[]) => boolean
  hasAny: (...permissions: string[]) => boolean
}
```

替代散落在各组件中的 `new Set(bootstrap?.permissions).has(...)` 模式。

## 8. 设计系统

### 8.1 Design Token

```typescript
export const designTokens = {
  color: {
    primary: '#2447b8',
    primaryHover: '#1a3696',
    text: '#172033',
    textSecondary: '#64748b',
    background: '#f4f7fb',
    backgroundAlt: '#ffffff',
    border: '#e7eaf0',
    error: '#dc2626',
    warning: '#f59e0b',
    success: '#16a34a',
  },
  radius: { sm: 4, md: 8, lg: 12 },
  spacing: { xs: 8, sm: 12, md: 16, lg: 24, xl: 32 },
} as const
```

- 注入为 CSS 变量到 `:root`
- 两个项目所有硬编码颜色/间距全部改为引用 token
- 两个项目各自移除 `index.css` 和 `global.css`，统一引用共享包的全局样式

### 8.2 ThemeProvider

```typescript
export const AdminThemeProvider = ({ children }: PropsWithChildren) => (
  <ConfigProvider
    locale={currentLocale}
    theme={{ token: { colorPrimary: tokens.color.primary, borderRadius: tokens.radius.md } }}
  >
    {children}
  </ConfigProvider>
)
```

两个项目统一使用此 Provider，不再各自定义。

## 9. 国际化 (i18n)

### 9.1 技术选型

- **库**: `i18next` + `react-i18next`
- **语言包**: `zh-CN.ts`（中文）、`en-US.ts`（英文），按 namespace 分 `common`、`idp`、`rbac3`
- **检测**: 默认 `navigator.language`，支持手动切换和 localStorage 持久化

### 9.2 导出接口

```typescript
export { initI18n, useT, I18nProvider, changeLanguage, currentLanguage }
```

### 9.3 项目使用方式

```typescript
// IDP: src/main.tsx
initI18n({
  defaultNS: 'common',
  resources: {
    'zh-CN': { common, idp },
  },
})

// RBAC3: src/main.tsx
initI18n({
  defaultNS: 'common',
  resources: {
    'zh-CN': { common, rbac3 },
  },
})
```

### 9.4 迁移策略

- 共享包组件先行国际化
- 两个项目页面逐页迁移，优先替换表头、按钮、提示文案
- 初期以中文为默认语言，英文后续补齐

## 10. IDP 重构

### 10.1 改动清单

| 改造项 | 改动内容 |
|--------|---------|
| 路由 | 接入 react-router v7 BrowserRouter，替换手写 `popstate` |
| App.tsx | 接入 `AdminThemeProvider`、`AppErrorBoundary`、路由定义 |
| AuthContext | 替换为共享包 OAuth client + `TokenStore.subscribe` |
| AdminConsole.tsx | **拆解为 5 个 feature 页面组件** |
| 数据层 | 全部改用 `useQuery`/`useMutation`，移除手动 useState |
| OAuth 回调 | 修复卡死 spinner，`CallbackPage` 组件处理 loading/error/success 三种状态 |
| 样式 | 删除 `styles/index.css`，改用共享 token |
| 测试 | 补齐所有页面组件测试 + API 层测试 |
| 路由级懒加载 | 所有页面组件 `React.lazy` + `Suspense` |

### 10.2 路由结构

```
/login              → CentralLoginPage（修复 login 逻辑）
/oauth/callback     → CallbackPage（修复卡死 bug，处理 error 状态）
/                   → AdminLayout（重定向到 /overview）
  /overview         → OverviewPage
  /users            → UserListPage
  /clients          → ClientListPage
  /keys             → SigningKeyPage
  /audits           → AuditLogPage
```

### 10.3 新增 Feature 组件

| 组件 | 职责 |
|------|------|
| `OverviewPage` | 当前授权上下文展示（现有 overview section） |
| `UserListPage` | 用户列表 + 创建用户 Modal + 重置密码/撤销会话 |
| `ClientListPage` | OAuth 客户端列表 + 创建客户端 Modal |
| `SigningKeyPage` | 签名密钥列表 |
| `AuditLogPage` | 审计日志列表（增加分页） |
| `CallbackPage` | OAuth 回调处理（loading/error/success 三种状态） |

### 10.4 代码删除清单

- `src/app/AdminConsole.tsx` — 拆解后删除
- `src/app/App.tsx` 中的手写路由逻辑 — 替换为 react-router
- `src/auth/oauthClient.ts` — 替换为共享包
- `src/auth/tokenStore.ts` — 替换为共享包
- `src/api/idpApi.ts` — 替换为共享 `HttpClient`
- `src/styles/index.css` — 替换为共享全局样式
- `src/api/types.ts` 中的 `decodeClaims` 引用 — 替换为共享 `jwt.ts`

## 11. RBAC3 重构

### 11.1 改动清单

| 改造项 | 改动内容 |
|--------|---------|
| OAuth client | 替换为共享包实现，删除 `features/auth/oauthClient.ts` |
| AppErrorBoundary | 替换为共享包版本 + `onError` telemetry hook |
| PageState | 替换为共享包增强版，各页加上 `onRetry` |
| Layout | 引入 `PageTemplate` 替代每页重复的 Card 包装 |
| 路由懒加载 | 所有页面 `React.lazy` + `Suspense` |
| API 层 | `adminApiClient.ts` 基于共享 `HttpClient` 实现 |
| 授权适配器修复 | `UnifiedRbac3ApiClient.refresh()` 修复 `roleActivationRequired: false` 写死 |
| JWT 解码 | 删除项目内实现，用共享 `jwt.ts` |
| i18n | 逐步替换硬编码中文文案 |
| `useFeatureQuery` | 15 个页面替换 boilerplate |
| `usePermission` | 替换 `Set.has` 模式 |
| Theme | 替换 `App.tsx` 中的 ConfigProvider 为共享 `AdminThemeProvider` |
| 样式 | 删除 `styles/global.css`，改用共享 token |

### 11.2 API 层改动

RBAC3 的 `adminApiClient.ts` 保留 `UnifiedRbac3ApiClient`（SDK 契约适配）和 `createAdminApiClients`（DI 入口），但内部 HTTP 传输委托给共享 `HttpClient`：

```
createAdminApiClients(baseUrl)
  ├── httpClient: HttpClient           ← 共享包
  ├── rbac3Client: UnifiedRbac3ApiClient
  │   └── (底层) httpClient.request()  ← 委托给共享包
  └── featureClient: FeatureApiClient
      └── FeatureApiProvider 保持不变（租户 header 逻辑不变）
```

### 11.3 修复清单

| 修复项 | 改动 |
|--------|------|
| `roleActivationRequired: false` 写死 | 从服务端返回值读取 |
| `RoleGraphPage` N+1 | `useQueries` 加并发上限（50）+ `AbortSignal` |
| `ConstraintPage` 死按钮 | 补充 `onClick` 或移除按钮 |
| `TenantDetailPage` 死路由 | 确认是否补路由，否则删除 |
| `AssignmentListPage` 隐藏 | 修复 `visibleNavigation` 的 `':'` 匹配 |
| `OverviewPage` 无效请求 | 删除 `overview.api` 的 runtime 调用，或渲染其数据 |
| `AuditLogPage` 时间冻结 | `initialFilter` 改为 state 初始化函数 |
| `tokenClaims`/`expiresIn` 防御性 | 使用共享 `jwt.ts` |
| 测试 boiletplate | 共享 `renderWithProviders` 函数 |

## 12. 构建与发布

### 12.1 共享包

```typescript
// vite.config.ts - Vite library mode
{
  build: {
    lib: {
      entry: resolve(__dirname, 'src/index.ts'),
      formats: ['es'],
    },
    rollupOptions: {
      external: ['react', 'react-dom', 'antd', '@tanstack/react-query',
                 'react-router-dom', 'i18next', 'react-i18next'],
    },
  },
}
```

### 12.2 消费项目引用

```jsonc
// IDP package.json
"dependencies": {
  "@egon-cola/admin-web-shared": "file:../egon-cola-platform-admin-web-shared",
  ...
}
```

Maven 构建顺序：先 `npm run build` 共享包，再 `npm install` 消费项目（`file:` 引用在 install 时 symlink）。

### 12.3 版本策略

- 共享包初始版本 `0.1.0`
- 两个消费项目由原先各自维护版本 → 统一引用共享包
- 后续迭代中共享包版本独立演进

## 13. 文件变更汇总

| 操作 | IDP | RBAC3 | 共享包 | 合计 |
|------|-----|-------|--------|------|
| 新建 | 7 | 0 | 25 | 32 |
| 修改 | 6 | 18 | 0 | 24 |
| 删除 | 4 | 2 | 0 | 6 |

## 14. 非目标

以下明确不做：

- **不替换 antd 或 react** — 保持现有技术栈
- **不引入状态管理库（Redux/Zustand）** — React Query + Context 足够
- **不修改后端 API** — 前端仅优化，后端合约不变
- **不引入 SSR 框架（Next.js/Remix）** — 保持 SPA 形态
- **不增加 monorepo 工具（Turborepo/Nx）** — 共享包通过 `file:` 引用，保持简单
- **`FeatureApiProvider` / `useFeatureTenantContext` 不下沉** — 与 rbac3-react-sdk 耦合
