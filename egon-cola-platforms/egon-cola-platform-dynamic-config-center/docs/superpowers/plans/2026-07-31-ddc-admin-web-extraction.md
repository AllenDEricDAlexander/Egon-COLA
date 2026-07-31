# DDC Admin Web 摘出实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 把 DDC 管理控制台从 admin 后端模块摘出，建成独立的 `egon-cola-platform-dynamic-config-center-admin-web` 前端工程（React + antd + Vite，对齐 gateway-admin-web 范式），并清理 admin 模块中的旧静态资源。

**Architecture:** 新建纯 Node 前端工程（不进 Maven reactor），覆盖 DDC 全部管理 API（服务注册/实例、配置管理+发布、应用、命名空间、发布任务、实例、缓存）。前端经 `static-server.mjs` 同源托管并以 `/api` 反代 admin（JWT Bearer 认证不变）。建成后删除 admin 的 `static/ddc-admin/` 与 `src/test/js/`，移除 `/ddc-admin/**` permitAll。

**Tech Stack:** React 19 + antd 6 + Vite 8 + TypeScript 6（版本号对齐 gateway-admin-web 的 package.json）；Vitest + jsdom；Playwright e2e；node:22-alpine + static-server.mjs。

## Global Constraints

- 模块目录：`egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin-web/`（下称 `<web>/`）。
- 纯 Node 工程：无 pom.xml，不进 Maven reactor，不参与 Maven 构建。
- `package.json` 的 `name` 为 `@egon-cola/ddc-admin-web`，`version` 为 `5.3.2`。
- 依赖版本一律照抄 gateway-admin-web 的 package.json：`react@19.2.8`、`react-dom@19.2.8`、`antd@6.5.2`、`@ant-design/icons@6.3.2`、`react-router-dom@7.18.1`、`@tanstack/react-query@5.101.4`、`vite@8.1.5`、`vitest@4.1.10`、`typescript@6.0.3`、`@vitejs/plugin-react@6.0.4`、`eslint@10.8.0`、`typescript-eslint@8.65.0`、`@playwright/test@1.62.0`、`@testing-library/react@16.3.2`、`@testing-library/jest-dom@7.0.0`、`jsdom@29.1.1`。
- 认证：Bearer token 存 `sessionStorage`，key 固定 `egon.ddc.admin.token`；HTTP 401 一律清 token 并回登录页，提示"登录已过期，请重新粘贴 Access Token"。
- 后端契约：所有响应为 `ResultRecord`：`{ success: boolean, code: number, status: string, message: string, data: T, traceId: string, timestamp: number }`；`!response.ok || payload.success === false` 视为错误，错误文案取 `payload.message || String(payload.code)`；API 字段一律用 camelCase（Jackson 默认）。
- UI 文案用中文，页面标题/菜单沿用旧 UI 用词：服务注册、配置管理、应用、命名空间、发布任务、实例、缓存。
- 创建应用/命名空间时沿用旧行为：应用 `{ appCode, appName: appCode, owner: 'local-admin', description: 'Created by DDC Admin Web', enabled: true }`；命名空间 `{ appCode, env, namespace, description: 'Created by DDC Admin Web', enabled: true }`。
- 发布配置请求体：`{ changeId: uuidV7(), configValue, expectedVersion: currentVersion, timeoutMs: 30000 }`。
- 部署环境变量前缀 `DDC_ADMIN_*`（`DDC_ADMIN_API_BASE_URL`、`DDC_ADMIN_API_DEVELOPMENT_PLAINTEXT`、`DDC_ADMIN_PROXY`）；反代失败错误码 `DDC_ADMIN_WEB_UPSTREAM_UNAVAILABLE`。
- e2e 仅在有真实 admin 可达时运行（`npm run e2e`），不作 CI 必过项。
- 参考实现：gateway-admin-web（`egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web/`，下称 `<gateway-web>/`）是本工程的结构模板；旧 webui（Task 12 之前仍存在于 admin 的 `static/ddc-admin/`）是功能行为模板。

---

### Task 1: 工程脚手架（package.json / tsconfig / vite / eslint / 入口）

**Files:**
- Create: `<web>/package.json`
- Create: `<web>/tsconfig.json`、`tsconfig.app.json`、`tsconfig.node.json`
- Create: `<web>/vite.config.ts`
- Create: `<web>/eslint.config.js`
- Create: `<web>/index.html`
- Create: `<web>/.gitignore`
- Create: `<web>/src/main.tsx`、`<web>/src/vite-env.d.ts`
- Create: `<web>/src/styles/index.css`
- Create: `<web>/src/test/setup.ts`
- Create: `<web>/src/App.tsx`（临时最小页面，Task 4 替换）
- Create: `<web>/src/App.test.tsx`（临时冒烟测试，Task 4 替换）

**Interfaces:**
- Consumes: 无（首个任务）。
- Produces: 可运行的 Vite + vitest 工程骨架；`npm run dev / typecheck / build / test / lint` 脚本齐备。

- [ ] **Step 1: 复制 gateway-web 骨架为起点**

```bash
cp -R <gateway-web> <web>
cd <web>
rm -rf node_modules dist .git* e2e playwright.config.ts static-server.mjs Dockerfile src features src/api src/auth src/layouts src/hooks src/components src/app 2>/dev/null
mkdir -p src/test
```

（保留根部的 package.json、tsconfig*.json、vite.config.ts、eslint.config.js、index.html 作模板；`package-lock.json` 保留，改 name 后 `npm install` 会自动校正。）

- [ ] **Step 2: 改写 package.json**

```json
{
  "name": "@egon-cola/ddc-admin-web",
  "version": "5.3.2",
  "private": true,
  "type": "module",
  "scripts": {
    "dev": "vite",
    "build": "tsc -b && vite build",
    "typecheck": "tsc -b --pretty false",
    "test": "vitest",
    "lint": "eslint .",
    "e2e": "playwright test"
  },
  "dependencies": {
    "@ant-design/icons": "^6.3.2",
    "@tanstack/react-query": "^5.101.4",
    "antd": "^6.5.2",
    "react": "^19.2.8",
    "react-dom": "^19.2.8",
    "react-router-dom": "^7.18.1"
  },
  "devDependencies": {
    "@eslint/js": "^10.0.1",
    "@playwright/test": "^1.62.0",
    "@testing-library/jest-dom": "^7.0.0",
    "@testing-library/react": "^16.3.2",
    "@types/node": "^26.1.1",
    "@types/react": "^19.2.17",
    "@types/react-dom": "^19.2.3",
    "@vitejs/plugin-react": "^6.0.4",
    "eslint": "^10.8.0",
    "eslint-plugin-react-hooks": "^7.1.1",
    "jsdom": "^29.1.1",
    "typescript": "^6.0.3",
    "typescript-eslint": "^8.65.0",
    "vite": "^8.1.5",
    "vitest": "^4.1.10"
  }
}
```

- [ ] **Step 3: 改写 vite.config.ts（代理前缀改 DDC）**

```ts
import { defineConfig } from 'vitest/config'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      '/api': {
        target: process.env.DDC_ADMIN_PROXY ?? 'http://127.0.0.1:18080',
        changeOrigin: true,
      },
    },
  },
  preview: {
    proxy: {
      '/api': {
        target: process.env.DDC_ADMIN_PROXY ?? 'http://127.0.0.1:18080',
        changeOrigin: true,
      },
    },
  },
  build: {
    sourcemap: false,
    chunkSizeWarningLimit: 900,
  },
  test: {
    environment: 'jsdom',
    setupFiles: './src/test/setup.ts',
    exclude: ['e2e/**', 'node_modules/**', 'dist/**'],
    coverage: {
      reporter: ['text', 'html'],
    },
  },
})
```

- [ ] **Step 4: 改写 index.html**

```html
<!doctype html>
<html lang="zh-CN">
  <head>
    <meta charset="UTF-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <title>DDC Admin</title>
  </head>
  <body>
    <div id="root"></div>
    <script type="module" src="/src/main.tsx"></script>
  </body>
</html>
```

- [ ] **Step 5: 写入口与临时页面**

`src/main.tsx`：

```tsx
import React from 'react'
import ReactDOM from 'react-dom/client'
import { ConfigProvider } from 'antd'
import zhCN from 'antd/locale/zh_CN'
import App from './App'
import './styles/index.css'

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <ConfigProvider locale={zhCN}>
      <App />
    </ConfigProvider>
  </React.StrictMode>,
)
```

`src/App.tsx`：

```tsx
export default function App() {
  return <div data-testid="app-root">DDC Admin</div>
}
```

`src/App.test.tsx`：

```tsx
import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import App from './App'

describe('App', () => {
  it('renders the application shell', () => {
    render(<App />)
    expect(screen.getByTestId('app-root')).toHaveTextContent('DDC Admin')
  })
})
```

`src/test/setup.ts`：

```ts
import '@testing-library/jest-dom/vitest'
```

`src/styles/index.css`（对齐 gateway-web 的版本，必要时保留 minimal reset，antd 负责组件样式）。

- [ ] **Step 6: 安装依赖并验证**

```bash
cd <web>
npm install
npm run typecheck
npm run test -- --run
npm run lint
npm run build
```

Expected: typecheck/lint/build 全部通过；vitest 1 条用例 PASS；`dist/` 生成 index.html 与 js 产物。

- [ ] **Step 7: Commit**

```bash
git add <web>
git commit -m "feat(ddc-admin-web): scaffold vite react antd project"
```

---

### Task 2: API 客户端（ResultRecord 契约 + 401 处理）

**Files:**
- Create: `<web>/src/api/client.ts`
- Create: `<web>/src/api/client.test.ts`
- Create: `<web>/src/api/types.ts`

**Interfaces:**
- Consumes: 无（独立纯函数模块，auth 在 Task 3 接入——client 通过注入的 `tokenProvider` 和 `onUnauthorized` 解耦）。
- Produces:
  - `export class DdcApiError extends Error { constructor(readonly status: number, readonly code: string, message: string, readonly traceId?: string) }`，带 `category: 'UNAUTHENTICATED' | 'FORBIDDEN' | 'NOT_FOUND' | 'CONFLICT' | 'VALIDATION' | 'SERVER' | 'NETWORK' | 'UNKNOWN'`（status 0 为 NETWORK）。
  - `export const setDdcTokenProvider(provider: () => string): void` — 设置取 token 的函数。
  - `export const setDdcUnauthorizedHandler(handler: () => void): void` — 401 时回调（Task 3 注册登出）。
  - `export async function ddcApi<T>(path: string, options?: { method?: string; body?: unknown }): Promise<T>` — 返回 `ResultRecord<T>['data']`；`GET` 不带 body；body 非空时自动设 `Content-Type: application/json` 并 `JSON.stringify`；非 2xx 或 `success === false` 抛 `DdcApiError`（message 取 `payload.message || String(payload.code)`）；401 调 unauthorized handler 后抛 `DdcApiError`（message "登录已过期，请重新粘贴 Access Token"）；网络失败（fetch reject）抛 `DdcApiError`（status 0, code 'DDC_ADMIN_WEB_UPSTREAM_UNAVAILABLE'）。
  - `src/api/types.ts`：`export type ResultRecord<T> = { success: boolean; code: number; status: string; message: string; data: T; traceId: string; timestamp: number }`，及后续任务复用的 DTO 类型（Task 6-9 各自的实体类型也放这里，见各任务）。

- [ ] **Step 1: 写失败的测试 `src/api/client.test.ts`**

```ts
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { DdcApiError, ddcApi, setDdcTokenProvider, setDdcUnauthorizedHandler } from './client'

const record = (data: unknown) => ({
  success: true, code: 0, status: 'SUCCESS', message: '', data,
  traceId: 'trace-1', timestamp: 1,
})

describe('ddcApi', () => {
  beforeEach(() => {
    setDdcTokenProvider(() => 'token-1')
    setDdcUnauthorizedHandler(() => {})
    vi.stubGlobal('fetch', vi.fn())
  })
  afterEach(() => vi.unstubAllGlobals())

  const jsonResponse = (body: unknown, status = 200) =>
    new Response(JSON.stringify(body), { status, headers: { 'Content-Type': 'application/json' } })

  it('sends bearer token and returns data', async () => {
    vi.mocked(fetch).mockResolvedValue(jsonResponse(record({ list: [1] })))
    await expect(ddcApi<{ list: number[] }>('/api/v1/ddc/apps')).resolves.toEqual({ list: [1] })
    const [url, init] = vi.mocked(fetch).mock.calls[0]
    expect(url).toBe('/api/v1/ddc/apps')
    expect((init!.headers as Headers).get('Authorization')).toBe('Bearer token-1')
  })

  it('stringifies JSON bodies with content type', async () => {
    vi.mocked(fetch).mockResolvedValue(jsonResponse(record(null)))
    await ddcApi('/api/v1/ddc/configs', { method: 'POST', body: { configKey: 'a' } })
    const [, init] = vi.mocked(fetch).mock.calls[0]
    expect((init!.headers as Headers).get('Content-Type')).toBe('application/json')
    expect(init!.body).toBe(JSON.stringify({ configKey: 'a' }))
  })

  it('throws DdcApiError with backend message on success=false', async () => {
    vi.mocked(fetch).mockResolvedValue(jsonResponse({ success: false, code: 500, status: 'FAIL', message: '配置格式无效', data: null, traceId: 't', timestamp: 1 }, 200))
    const error = await ddcApi('/api/v1/ddc/apps').catch((e) => e as DdcApiError)
    expect(error).toBeInstanceOf(DdcApiError)
    expect(error.message).toBe('配置格式无效')
    expect(error.category).toBe('SERVER')
  })

  it('on 401 calls the unauthorized handler and throws', async () => {
    const handler = vi.fn()
    setDdcUnauthorizedHandler(handler)
    vi.mocked(fetch).mockResolvedValue(jsonResponse({ success: false, code: 401, status: 'UNAUTHORIZED', message: 'jwt expired', data: null, traceId: 't', timestamp: 1 }, 401))
    const error = await ddcApi('/api/v1/ddc/apps').catch((e) => e as DdcApiError)
    expect(handler).toHaveBeenCalledTimes(1)
    expect(error.message).toBe('登录已过期，请重新粘贴 Access Token')
    expect(error.category).toBe('UNAUTHENTICATED')
  })

  it('maps network failures to NETWORK category', async () => {
    vi.mocked(fetch).mockRejectedValue(new TypeError('fetch failed'))
    const error = await ddcApi('/api/v1/ddc/apps').catch((e) => e as DdcApiError)
    expect(error.status).toBe(0)
    expect(error.category).toBe('NETWORK')
    expect(error.code).toBe('DDC_ADMIN_WEB_UPSTREAM_UNAVAILABLE')
  })
})
```

- [ ] **Step 2: 运行确认失败**

Run: `cd <web> && npx vitest run src/api/client.test.ts`
Expected: FAIL（`client.ts` 不存在，import 报错）。

- [ ] **Step 3: 写实现 `src/api/client.ts`**

```ts
import type { ResultRecord } from './types'

type TokenProvider = () => string
type UnauthorizedHandler = () => void

let tokenProvider: TokenProvider = () => ''
let unauthorizedHandler: UnauthorizedHandler = () => {}

export const setDdcTokenProvider = (provider: TokenProvider): void => {
  tokenProvider = provider
}

export const setDdcUnauthorizedHandler = (handler: UnauthorizedHandler): void => {
  unauthorizedHandler = handler
}

export class DdcApiError extends Error {
  constructor(
    readonly status: number,
    readonly code: string,
    message: string,
    readonly traceId?: string,
  ) {
    super(message)
    this.name = 'DdcApiError'
  }

  get category():
    | 'UNAUTHENTICATED'
    | 'FORBIDDEN'
    | 'NOT_FOUND'
    | 'CONFLICT'
    | 'VALIDATION'
    | 'SERVER'
    | 'NETWORK'
    | 'UNKNOWN' {
    if (this.status === 0) return 'NETWORK'
    if (this.status === 401) return 'UNAUTHENTICATED'
    if (this.status === 403) return 'FORBIDDEN'
    if (this.status === 404) return 'NOT_FOUND'
    if (this.status === 409) return 'CONFLICT'
    if (this.status === 422) return 'VALIDATION'
    if (this.status >= 500) return 'SERVER'
    return 'UNKNOWN'
  }
}

export type DdcRequestOptions = { method?: string; body?: unknown }

export async function ddcApi<T>(path: string, options: DdcRequestOptions = {}): Promise<T> {
  const headers = new Headers()
  headers.set('Authorization', `Bearer ${tokenProvider()}`)
  let body: string | undefined
  if (options.body !== undefined) {
    headers.set('Content-Type', 'application/json')
    body = JSON.stringify(options.body)
  }
  let response: Response
  try {
    response = await fetch(path, { method: options.method ?? 'GET', headers, body })
  } catch {
    throw new DdcApiError(0, 'DDC_ADMIN_WEB_UPSTREAM_UNAVAILABLE', '无法连接 DDC 管理端')
  }
  const payload = (await response.json().catch(() => ({}))) as Partial<ResultRecord<unknown>>
  if (response.status === 401) {
    unauthorizedHandler()
    throw new DdcApiError(401, 'UNAUTHORIZED', '登录已过期，请重新粘贴 Access Token', payload.traceId)
  }
  if (!response.ok || payload.success === false) {
    throw new DdcApiError(
      response.status,
      String(payload.code ?? response.status),
      payload.message || String(payload.code) || `请求失败 (${response.status})`,
      payload.traceId,
    )
  }
  return payload.data as T
}
```

- [ ] **Step 4: 运行测试确认通过**

Run: `cd <web> && npx vitest run src/api/client.test.ts`
Expected: PASS（5 条）。

- [ ] **Step 5: Commit**

```bash
git add <web>/src/api
git commit -m "feat(ddc-admin-web): add ddc api client with ResultRecord contract"
```

---

### Task 3: Token 存储 + 登录页（AuthContext / RouteGuards）

**Files:**
- Create: `<web>/src/auth/tokenStore.ts`
- Create: `<web>/src/auth/tokenStore.test.ts`
- Create: `<web>/src/auth/AuthContext.tsx`
- Create: `<web>/src/auth/LoginPage.tsx`
- Create: `<web>/src/auth/RouteGuards.tsx`

**Interfaces:**
- Consumes: Task 2 的 `ddcApi`、`setDdcTokenProvider`、`setDdcUnauthorizedHandler`。
- Produces:
  - `tokenStore`：`export const TOKEN_KEY = 'egon.ddc.admin.token'`；`export function getStoredToken(): string`；`export function saveToken(token: string): void`；`export function clearToken(): void`（sessionStorage）。
  - `AuthProvider`（Props: `{ children: ReactNode }`）：context 值 `{ token: string; setToken: (token: string) => void; logout: () => void }`；挂载时 `setDdcTokenProvider(getStoredToken)`、`setDdcUnauthorizedHandler(logout)`；`setToken` 同时写 sessionStorage。
  - `useAuth(): { token: string; setToken: (t: string) => void; logout: () => void }`。
  - `LoginPage`：antd 卡片 + TextArea（placeholder "粘贴 admin.token 内容"）+ 按钮"登录并加载"；提交时 `await ddcApi('/api/v1/ddc/apps')` 验证 token（沿用旧 UI 的探活路径），成功 `setToken`，失败 `message.error`。
  - `RequireAuth`（Props `{ children: ReactNode }`）：`token` 为空时渲染 `LoginPage`，否则渲染 children。

- [ ] **Step 1: 写失败的测试 `tokenStore.test.ts`**

```ts
import { afterEach, describe, expect, it } from 'vitest'
import { TOKEN_KEY, clearToken, getStoredToken, saveToken } from './tokenStore'

describe('tokenStore', () => {
  afterEach(() => sessionStorage.clear())

  it('persists and reads the token from sessionStorage', () => {
    expect(getStoredToken()).toBe('')
    saveToken('token-abc')
    expect(sessionStorage.getItem(TOKEN_KEY)).toBe('token-abc')
    expect(getStoredToken()).toBe('token-abc')
  })

  it('clears the token', () => {
    saveToken('token-abc')
    clearToken()
    expect(getStoredToken()).toBe('')
    expect(sessionStorage.getItem(TOKEN_KEY)).toBeNull()
  })
})
```

- [ ] **Step 2: 运行确认失败**

Run: `cd <web> && npx vitest run src/auth/tokenStore.test.ts`
Expected: FAIL（模块不存在）。

- [ ] **Step 3: 写实现**

`tokenStore.ts`：

```ts
export const TOKEN_KEY = 'egon.ddc.admin.token'

export const getStoredToken = (): string => sessionStorage.getItem(TOKEN_KEY) ?? ''

export const saveToken = (token: string): void => {
  sessionStorage.setItem(TOKEN_KEY, token)
}

export const clearToken = (): void => {
  sessionStorage.removeItem(TOKEN_KEY)
}
```

`AuthContext.tsx`：

```tsx
import { createContext, useCallback, useContext, useMemo, useState, type ReactNode } from 'react'
import { clearToken, getStoredToken, saveToken } from './tokenStore'
import { setDdcTokenProvider, setDdcUnauthorizedHandler } from '../api/client'

type AuthContextValue = {
  token: string
  setToken: (token: string) => void
  logout: () => void
}

const AuthContext = createContext<AuthContextValue | null>(null)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [token, setTokenState] = useState<string>(() => getStoredToken())

  const setToken = useCallback((next: string) => {
    saveToken(next)
    setTokenState(next)
  }, [])

  const logout = useCallback(() => {
    clearToken()
    setTokenState('')
  }, [])

  useMemo(() => setDdcTokenProvider(getStoredToken), [])
  useMemo(() => setDdcUnauthorizedHandler(logout), [logout])

  const value = useMemo(() => ({ token, setToken, logout }), [token, setToken, logout])
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export const useAuth = (): AuthContextValue => {
  const value = useContext(AuthContext)
  if (!value) throw new Error('useAuth must be used within AuthProvider')
  return value
}
```

`LoginPage.tsx`：

```tsx
import { useState } from 'react'
import { Button, Card, Input, Typography, message } from 'antd'
import { ddcApi } from '../api/client'
import { useAuth } from './AuthContext'

export default function LoginPage() {
  const { setToken } = useAuth()
  const [accessToken, setAccessToken] = useState('')
  const [loading, setLoading] = useState(false)

  const submit = async () => {
    setLoading(true)
    try {
      await ddcApi('/api/v1/ddc/apps')
      setToken(accessToken.trim())
    } catch (error) {
      message.error(error instanceof Error ? error.message : String(error))
    } finally {
      setLoading(false)
    }
  }

  return (
    <div style={{ maxWidth: 480, margin: '96px auto', padding: '0 16px' }}>
      <Card>
        <Typography.Title level={4}>连接本机 DDC 管理端</Typography.Title>
        <Typography.Paragraph type="secondary">
          Token 仅保存在当前浏览器会话，不会写入 URL 或服务端。
        </Typography.Paragraph>
        <Input.TextArea
          value={accessToken}
          onChange={(event) => setAccessToken(event.target.value)}
          rows={4}
          placeholder="粘贴 admin.token 内容"
          autoComplete="off"
        />
        <Button
          type="primary"
          block
          style={{ marginTop: 16 }}
          loading={loading}
          disabled={accessToken.trim() === ''}
          onClick={() => void submit()}
        >
          登录并加载
        </Button>
      </Card>
    </div>
  )
}
```

`RouteGuards.tsx`：

```tsx
import type { ReactNode } from 'react'
import { useAuth } from './AuthContext'
import LoginPage from './LoginPage'

export function RequireAuth({ children }: { children: ReactNode }) {
  const { token } = useAuth()
  if (token === '') return <LoginPage />
  return <>{children}</>
}
```

- [ ] **Step 4: 运行测试确认通过**

Run: `cd <web> && npx vitest run src/auth/tokenStore.test.ts`
Expected: PASS（2 条）。随后 `npm run typecheck` 通过。

- [ ] **Step 5: Commit**

```bash
git add <web>/src/auth
git commit -m "feat(ddc-admin-web): add token store, auth context and login page"
```

---

### Task 4: 布局 + 路由（AdminLayout / 菜单 / 路由表）

**Files:**
- Create: `<web>/src/layouts/AdminLayout.tsx`
- Modify: `<web>/src/App.tsx`（替换为路由表）
- Modify: `<web>/src/App.test.tsx`

**Interfaces:**
- Consumes: Task 3 的 `AuthProvider`、`RequireAuth`；Task 6-9 的页面组件（本任务先用 `src/pages/RegistryPage.tsx` 等 7 个最小占位组件建路由，后续任务逐个替换文件内容——见各任务 Files 的 "Modify"）。
- Produces:
  - `AdminLayout`：antd `Layout`（Sider 菜单 + Content）；菜单项固定 7 项，key 与标签：`registry` 服务注册、`configs` 配置管理、`apps` 应用、`namespaces` 命名空间、`publish-tasks` 发布任务、`instances` 实例、`cache` 缓存；顶部右侧"退出"按钮调 `logout()`；`Navigate` 根路径 → `/registry`。
  - 路由表：`/` 包裹 `RequireAuth` → `AdminLayout`，子路由 `registry / configs / apps / namespaces / publish-tasks / instances / cache`。

- [ ] **Step 1: 建 7 个占位页面**

每个文件一行实现（例 `src/pages/RegistryPage.tsx`）：

```tsx
export default function RegistryPage() {
  return <div data-testid="page-registry">服务注册</div>
}
```

同样创建 `ConfigsPage.tsx`（`data-testid="page-configs"`）、`AppsPage.tsx`、`NamespacesPage.tsx`、`PublishTasksPage.tsx`、`InstancesPage.tsx`、`CachePage.tsx`（testid 对应 `page-apps` / `page-namespaces` / `page-publish-tasks` / `page-instances` / `page-cache`）。

- [ ] **Step 2: 写 `AdminLayout.tsx`**

```tsx
import { Layout, Menu, Button, Space, Typography } from 'antd'
import { useNavigate, useLocation, Outlet } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'

const menuItems = [
  { key: 'registry', label: '服务注册' },
  { key: 'configs', label: '配置管理' },
  { key: 'apps', label: '应用' },
  { key: 'namespaces', label: '命名空间' },
  { key: 'publish-tasks', label: '发布任务' },
  { key: 'instances', label: '实例' },
  { key: 'cache', label: '缓存' },
]

export default function AdminLayout() {
  const navigate = useNavigate()
  const location = useLocation()
  const { logout } = useAuth()
  const selected = menuItems.find((item) => location.pathname.startsWith(`/${item.key}`))?.key ?? 'registry'

  return (
    <Layout style={{ minHeight: '100vh' }}>
      <Layout.Sider theme="light" width={200}>
        <div style={{ padding: 16 }}>
          <Typography.Text strong>DDC Admin</Typography.Text>
        </div>
        <Menu
          mode="inline"
          selectedKeys={[selected]}
          items={menuItems}
          onClick={({ key }) => navigate(`/${key}`)}
        />
      </Layout.Sider>
      <Layout>
        <Layout.Header style={{ background: '#fff', display: 'flex', justifyContent: 'flex-end', alignItems: 'center' }}>
          <Space>
            <Typography.Text>DDC 已连接</Typography.Text>
            <Button onClick={logout}>退出</Button>
          </Space>
        </Layout.Header>
        <Layout.Content style={{ padding: 24 }}>
          <Outlet />
        </Layout.Content>
      </Layout>
    </Layout>
  )
}
```

- [ ] **Step 3: 替换 `App.tsx` 为路由表**

```tsx
import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import { AuthProvider } from './auth/AuthContext'
import { RequireAuth } from './auth/RouteGuards'
import AdminLayout from './layouts/AdminLayout'
import RegistryPage from './pages/RegistryPage'
import ConfigsPage from './pages/ConfigsPage'
import AppsPage from './pages/AppsPage'
import NamespacesPage from './pages/NamespacesPage'
import PublishTasksPage from './pages/PublishTasksPage'
import InstancesPage from './pages/InstancesPage'
import CachePage from './pages/CachePage'

export default function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <Routes>
          <Route
            path="/"
            element={
              <RequireAuth>
                <AdminLayout />
              </RequireAuth>
            }
          >
            <Route index element={<Navigate to="/registry" replace />} />
            <Route path="registry" element={<RegistryPage />} />
            <Route path="configs" element={<ConfigsPage />} />
            <Route path="apps" element={<AppsPage />} />
            <Route path="namespaces" element={<NamespacesPage />} />
            <Route path="publish-tasks" element={<PublishTasksPage />} />
            <Route path="instances" element={<InstancesPage />} />
            <Route path="cache" element={<CachePage />} />
          </Route>
        </Routes>
      </AuthProvider>
    </BrowserRouter>
  )
}
```

- [ ] **Step 4: 替换 `App.test.tsx`（未登录时渲染登录页）**

```tsx
import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import App from './App'

describe('App', () => {
  it('renders the login page when no token is stored', () => {
    sessionStorage.clear()
    render(<App />)
    expect(screen.getByText('连接本机 DDC 管理端')).toBeInTheDocument()
  })
})
```

- [ ] **Step 5: 运行验证**

Run: `cd <web> && npm run test -- --run && npm run typecheck`
Expected: PASS（App.test 1 条 + tokenStore 2 条 + client 5 条），typecheck 通过。

- [ ] **Step 6: Commit**

```bash
git add <web>/src
git commit -m "feat(ddc-admin-web): add admin layout, menu and routes"
```

---

### Task 5: config-format 与 uuid 迁移为 TS（保留全部现有行为与用例）

**Files:**
- Create: `<web>/src/lib/uuid.ts`
- Create: `<web>/src/lib/configFormat.ts`
- Create: `<web>/src/lib/configFormat.test.ts`
- Create: `<web>/src/lib/uuid.test.ts`

**Interfaces:**
- Consumes: 无。
- Produces:
  - `export function uuidV7(timestamp?: number, randomBytes?: Uint8Array): string`（行为与旧 `uuid.mjs` 完全一致）。
  - `export type ConfigEditor = { format: 'JSON' | 'YAML' | 'TOML' | 'TXT'; content: string; adapter: 'PLAIN' | 'GATEWAY_INLINE_RULE'; originalValue: string; gateway: { activation: unknown; snapshot: unknown } | null; notice: string }`。
  - `export function detectConfigFormat(config: { configKey?: string; configValue?: string; valueType?: string }): 'JSON' | 'YAML' | 'TOML' | 'TXT'`。
  - `export function prepareConfigEditor(config?: { configKey?: string; configValue?: string; valueType?: string }): ConfigEditor`。
  - `export async function serializeConfigEditor(editor: ConfigEditor, content: string): Promise<string>`（JSON 时校验+缩进保存；GATEWAY_INLINE_RULE 时重建校验和与 activation，逻辑与旧 `config-format.mjs` 完全一致）。

- [ ] **Step 1: 写失败的测试（迁移自 `admin/src/test/js/ddc-admin/uuid.test.mjs` 与 `config-format.test.mjs`）**

`uuid.test.ts`：

```ts
import { describe, expect, it } from 'vitest'
import { uuidV7 } from './uuid'

describe('uuidV7', () => {
  it('encodes the timestamp and RFC variant as UUID version 7', () => {
    const value = uuidV7(
      0x0123456789ab,
      Uint8Array.from([0, 1, 2, 3, 4, 5, 6, 7, 8, 9]),
    )
    expect(value).toBe('01234567-89ab-7001-8203-040506070809')
  })
})
```

`configFormat.test.ts`（迁移 node:test 断言为 vitest 断言；`gatewayConfig()` 与用例数据原样保留）：

```ts
import { describe, expect, it } from 'vitest'
import { detectConfigFormat, prepareConfigEditor, serializeConfigEditor } from './configFormat'

const gatewayConfig = () => {
  const snapshot = {
    ruleSchemaVersion: 'v1',
    releaseId: 'release-1',
    generatedAt: '2026-07-28T00:00:00Z',
    ruleContentSha256: 'old-content-sha',
    artifactSha256: 'old-artifact-sha',
    content: { env: 'dev', routes: [] },
  }
  return {
    configKey: 'gateway.rules.active',
    valueType: 'JSON',
    configValue: JSON.stringify({
      activationSchemaVersion: 'v1',
      releaseId: 'release-1',
      mode: 'INLINE',
      ruleSchemaVersion: 'v1',
      totalSize: 1,
      ruleContentSha256: 'old-content-sha',
      artifactSha256: 'old-artifact-sha',
      inlineSnapshot: JSON.stringify(snapshot),
      chunks: [],
    }),
  }
}

describe('detectConfigFormat', () => {
  it('identifies supported configuration file formats', () => {
    expect(detectConfigFormat({ configKey: 'application.yaml', configValue: '' })).toBe('YAML')
    expect(detectConfigFormat({ configKey: 'runtime.toml', configValue: '' })).toBe('TOML')
    expect(detectConfigFormat({ configKey: 'banner.txt', configValue: '{}' })).toBe('TXT')
    expect(detectConfigFormat({ configKey: 'feature.flags', valueType: 'JSON', configValue: '{}' })).toBe('JSON')
    expect(detectConfigFormat({ configKey: 'runtime', configValue: 'server:\n  port: 8080' })).toBe('YAML')
    expect(detectConfigFormat({ configKey: 'runtime', configValue: '[server]\nport = 8080' })).toBe('TOML')
  })

  it('keeps typed scalar values as text configuration', () => {
    expect(detectConfigFormat({ configKey: 'feature.enabled', valueType: 'BOOLEAN', configValue: 'true' })).toBe('TXT')
    expect(detectConfigFormat({ configKey: 'request.timeout', valueType: 'INTEGER', configValue: '200' })).toBe('TXT')
    expect(detectConfigFormat({ configKey: 'feature.flags', valueType: 'STRING', configValue: '{"enabled":true}' })).toBe('JSON')
  })
})

describe('prepareConfigEditor', () => {
  it('preserves ordinary JSON business fields', () => {
    const value = { data: { version: 1 }, metadata: { owner: 'ops' } }
    const editor = prepareConfigEditor({ configKey: 'business.json', valueType: 'JSON', configValue: JSON.stringify(value) })
    expect(editor.adapter).toBe('PLAIN')
    expect(editor.format).toBe('JSON')
    expect(JSON.parse(editor.content)).toEqual(value)
    expect(editor.content).toMatch(/"metadata": \{/)
  })

  it('exposes only Gateway inline rule content', () => {
    const editor = prepareConfigEditor(gatewayConfig())
    expect(editor.adapter).toBe('GATEWAY_INLINE_RULE')
    expect(editor.format).toBe('JSON')
    expect(editor.content).toBe('{\n  "env": "dev",\n  "routes": []\n}')
    expect(editor.notice).toMatch(/Gateway/)
    expect(editor.content).not.toMatch(/artifactSha256|generatedAt|releaseId/)
  })

  it('does not unwrap an inconsistent Gateway-like object', () => {
    const config = gatewayConfig()
    const activation = JSON.parse(config.configValue) as Record<string, unknown>
    const snapshot = JSON.parse(String(activation.inlineSnapshot)) as Record<string, unknown>
    snapshot.releaseId = 'different-release'
    activation.inlineSnapshot = JSON.stringify(snapshot)
    const editor = prepareConfigEditor({ ...config, configValue: JSON.stringify(activation) })
    expect(editor.adapter).toBe('PLAIN')
    expect(editor.content).toMatch(/"inlineSnapshot":/)
  })
})

describe('serializeConfigEditor', () => {
  it('rebuilds Gateway checksums and activation metadata', async () => {
    const editor = prepareConfigEditor(gatewayConfig())
    const serialized = await serializeConfigEditor(editor, '{\n  "env": "prod",\n  "routes": []\n}')
    const activation = JSON.parse(serialized) as Record<string, unknown>
    const snapshot = JSON.parse(String(activation.inlineSnapshot)) as Record<string, unknown>
    expect(snapshot.content).toEqual({ env: 'prod', routes: [] })
    expect(snapshot.ruleContentSha256).toBe('a270803a31aceb109ad9e65bd4993c02049e2717798dc3be95b462b81c47167b')
    expect(snapshot.artifactSha256).toBe('784dc9c7bb589bdb5ab542f6170f0fa5751b89ad6d0a1035b2dd7d5902889303')
    expect(activation.ruleContentSha256).toBe(snapshot.ruleContentSha256)
    expect(activation.artifactSha256).toBe(snapshot.artifactSha256)
    expect(activation.totalSize).toBe(295)
    expect(activation.releaseId).toBe('release-1')
    expect(activation.activationSchemaVersion).toBe('v1')
    expect(activation.chunks).toEqual([])
    expect(snapshot.generatedAt).toBe('2026-07-28T00:00:00Z')
    expect(snapshot.releaseId).toBe('release-1')
  })

  it('rejects malformed JSON instead of storing it', async () => {
    const editor = prepareConfigEditor({ configKey: 'feature.json', valueType: 'JSON', configValue: '{}' })
    await expect(serializeConfigEditor(editor, '{ invalid')).rejects.toThrow(/JSON 配置格式无效/)
  })
})
```

- [ ] **Step 2: 运行确认失败**

Run: `cd <web> && npx vitest run src/lib`
Expected: FAIL（模块不存在）。

- [ ] **Step 3: 写实现——`uuid.ts` 内容为旧 `uuid.mjs` 逐行移植**（将 `export const` 保留，函数体不变）：

```ts
const hex = Array.from({ length: 256 }, (_, value) =>
  value.toString(16).padStart(2, '0'))

export const uuidV7 = (
  timestamp = Date.now(),
  randomBytes = crypto.getRandomValues(new Uint8Array(10)),
): string => {
  const bytes = new Uint8Array(16)
  let milliseconds = BigInt(timestamp)
  for (let index = 5; index >= 0; index -= 1) {
    bytes[index] = Number(milliseconds & 0xffn)
    milliseconds >>= 8n
  }
  bytes[6] = 0x70 | (randomBytes[0] & 0x0f)
  bytes[7] = randomBytes[1]
  bytes[8] = 0x80 | (randomBytes[2] & 0x3f)
  bytes.set(randomBytes.slice(3, 10), 9)

  return [
    [...bytes.slice(0, 4)].map((value) => hex[value]).join(''),
    [...bytes.slice(4, 6)].map((value) => hex[value]).join(''),
    [...bytes.slice(6, 8)].map((value) => hex[value]).join(''),
    [...bytes.slice(8, 10)].map((value) => hex[value]).join(''),
    [...bytes.slice(10, 16)].map((value) => hex[value]).join(''),
  ].join('-')
}
```

- [ ] **Step 4: 写实现——`configFormat.ts` 为旧 `config-format.mjs` 逐行移植**

要点（完整代码 = 旧文件内容 + 以下类型标注）：`extensionFormats`、`parseJson`、`jsonValue`、`isObject`、`gatewayInlineRule`、`extensionFormat`、`looksLikeToml`、`looksLikeYaml` 原样保留；`detectConfigFormat`、`prepareConfigEditor`、`serializeConfigEditor` 的返回类型标注为 Task 5 Interfaces 定义的类型；`sha256` 用 `globalThis.crypto.subtle`（jsdom 测试环境通过 `src/test/setup.ts` 引入 Node 的 `webcrypto` polyfill，见 Step 5）。逐行对照旧文件 `admin/src/main/resources/static/ddc-admin/config-format.mjs` 移植，不得改变任何判定逻辑与文案。

- [ ] **Step 5: 在 `src/test/setup.ts` 增加 webcrypto polyfill（jsdom 无 subtle）**

```ts
import '@testing-library/jest-dom/vitest'
import { webcrypto } from 'node:crypto'

if (!globalThis.crypto?.subtle) {
  Object.defineProperty(globalThis, 'crypto', { value: webcrypto })
}
```

- [ ] **Step 6: 运行测试确认通过**

Run: `cd <web> && npx vitest run src/lib && npm run typecheck`
Expected: PASS（uuid 1 条 + configFormat 6 条），typecheck 通过。

- [ ] **Step 7: Commit**

```bash
git add <web>/src/lib <web>/src/test/setup.ts
git commit -m "feat(ddc-admin-web): migrate config-format and uuid to typescript"
```

---

### Task 6: 服务注册页（RegistryPage）

**Files:**
- Modify: `<web>/src/pages/RegistryPage.tsx`（替换 Task 4 占位实现）
- Create: `<web>/src/pages/RegistryPage.test.tsx`
- Modify: `<web>/src/api/types.ts`（追加 registry 类型）

**Interfaces:**
- Consumes: Task 2 `ddcApi`；Task 5 无需。类型契约（后端 `DdcRegistryAdminController`）：
  - `GET /api/v1/ddc/registry/services?env&namespace&serviceKind&protocol` → `data: { services: RegistryService[] }`；service 字段：`serviceKind`、`protocol`、`serviceName`、`group`、`version`、`metadata`（可选 object）。
  - `GET /api/v1/ddc/registry/instances?env&namespace&serviceKind&protocol&serviceName&group?&version?` → `data: { instances: RegistryInstance[] }`；instance 字段：`instanceId`、`host`、`port`、`secure`、`status`、`lastHeartbeatAt`、`expireAt`、`metadata?: { buildId?: string }`。
- Produces:
  - `src/api/types.ts` 新增：`export type RegistryService = { serviceKind: string; protocol: string; serviceName: string; group?: string; version?: string; metadata?: Record<string, unknown> }`；`export type RegistryInstance = { instanceId: string; host: string; port: number; secure: boolean; status: string; lastHeartbeatAt?: string; expireAt?: string; metadata?: { buildId?: string } }`。
  - `RegistryPage`：env/namespace 两个输入框 + 刷新按钮；4 个统计卡（HTTP Provider / RPC Provider / Internal Gateway 服务数 + 在线实例数）；服务表格（label、serviceName、protocol、group/version，点击行加载实例）；实例表格（status 徽标、instanceId、host:port、lastHeartbeatAt、expireAt）。4 组查询：`[{ serviceKind: 'HTTP_PROVIDER', protocol: 'http', label: 'HTTP Provider' }, { serviceKind: 'HTTP_PROVIDER', protocol: 'https', label: 'HTTPS Provider' }, { serviceKind: 'RPC_PROVIDER', protocol: 'grpc', label: 'RPC Provider' }, { serviceKind: 'INTERNAL_GATEWAY', protocol: 'grpc', label: 'Internal Gateway' }]`；合并去重键 `serviceKind|protocol|serviceName|group|version`（对齐旧 `app.js` 的 `loadRegistry` / `serviceIdentity` / `loadInstances` 行为）。

- [ ] **Step 1: 写失败的测试 `RegistryPage.test.tsx`**

```tsx
import { render, screen, waitFor, fireEvent } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { setDdcTokenProvider, setDdcUnauthorizedHandler } from '../api/client'
import RegistryPage from './RegistryPage'

const record = (data: unknown) => ({
  success: true, code: 0, status: 'SUCCESS', message: '', data, traceId: 't', timestamp: 1,
})

describe('RegistryPage', () => {
  beforeEach(() => {
    setDdcTokenProvider(() => 'token')
    setDdcUnauthorizedHandler(() => {})
    vi.stubGlobal('fetch', vi.fn())
  })

  it('loads and renders the four service kinds with dedup', async () => {
    vi.mocked(fetch)
      .mockResolvedValueOnce(new Response(JSON.stringify(record({ services: [
        { serviceKind: 'HTTP_PROVIDER', protocol: 'http', serviceName: 'orders', group: 'g', version: 'v1' },
        { serviceKind: 'HTTP_PROVIDER', protocol: 'http', serviceName: 'orders', group: 'g', version: 'v1' },
      ] })), { status: 200 }))
      .mockResolvedValue(new Response(JSON.stringify(record({ services: [] })), { status: 200 }))

    render(<RegistryPage />)
    await waitFor(() => expect(screen.getByText('orders')).toBeInTheDocument())
    expect(screen.getAllByText('orders')).toHaveLength(1)
    expect(screen.getByText('HTTP Provider')).toBeInTheDocument()
  })

  it('loads instances when a service row is selected', async () => {
    vi.mocked(fetch)
      .mockResolvedValueOnce(new Response(JSON.stringify(record({ services: [
        { serviceKind: 'RPC_PROVIDER', protocol: 'grpc', serviceName: 'checkout', group: '', version: '' },
      ] })), { status: 200 }))
      .mockResolvedValueOnce(new Response(JSON.stringify(record({ instances: [
        { instanceId: 'i-1', host: '10.0.0.1', port: 8080, secure: false, status: 'ONLINE', lastHeartbeatAt: '2026-07-31T10:00:00Z', expireAt: '2026-07-31T11:00:00Z', metadata: { buildId: 'b-1' } },
      ] })), { status: 200 }))

    render(<RegistryPage />)
    await waitFor(() => expect(screen.getByText('checkout')).toBeInTheDocument())
    fireEvent.click(screen.getByText('checkout'))
    await waitFor(() => expect(screen.getByText('10.0.0.1:8080')).toBeInTheDocument())
    expect(screen.getByText('ONLINE')).toBeInTheDocument()
  })
})
```

- [ ] **Step 2: 运行确认失败**

Run: `cd <web> && npx vitest run src/pages/RegistryPage.test.tsx`
Expected: FAIL（组件仍为占位）。

- [ ] **Step 3: 实现 `RegistryPage.tsx`**

结构（antd）：`Input`（env/namespace）+ `Button` 刷新；`Statistic`/自绘 4 卡（服务数按 label 统计、在线实例数 = 已加载实例中 status==='ONLINE' 的数量）；`Table` 服务（columns：label、serviceName、protocol、group/version；`rowKey` 用去重键；onRow onClick 加载实例）；`Table` 实例（columns：status（Tag 绿 ONLINE / 灰其它）、instanceId（附 buildId 小字）、`${secure ? 'tls://' : ''}${host}:${port}`、lastHeartbeatAt、expireAt，时间用 `new Date(value).toLocaleString('zh-CN', { hour12: false })`，空值显示 '—'）。数据加载用 `useState + useEffect`（初始加载一次 + 刷新按钮），不引入 react-query（Task 8 起页面多用时再统一接入）。

去重逻辑（对齐旧 app.js）：

```ts
const serviceIdentity = (service: RegistryService) =>
  [service.serviceKind, service.protocol, service.serviceName, service.group ?? '', service.version ?? ''].join('|')
```

查询参数用 `URLSearchParams`，空值不携带（对齐旧 `query()` 行为）。

- [ ] **Step 4: 运行测试确认通过**

Run: `cd <web> && npx vitest run src/pages/RegistryPage.test.tsx && npm run typecheck`
Expected: PASS（2 条）。

- [ ] **Step 5: Commit**

```bash
git add <web>/src/pages/RegistryPage.tsx <web>/src/pages/RegistryPage.test.tsx <web>/src/api/types.ts
git commit -m "feat(ddc-admin-web): add service registry page"
```

---

### Task 7: 配置管理页（ConfigsPage + 配置编辑对话框）

**Files:**
- Modify: `<web>/src/pages/ConfigsPage.tsx`（替换占位）
- Create: `<web>/src/pages/ConfigEditorDialog.tsx`
- Create: `<web>/src/pages/ConfigsPage.test.tsx`
- Modify: `<web>/src/api/types.ts`（追加 config 类型）

**Interfaces:**
- Consumes: Task 2 `ddcApi`、Task 5 `prepareConfigEditor` / `serializeConfigEditor` / `detectConfigFormat`、Task 5 `uuidV7`。
- 契约（后端 `DdcConfigController` / `DdcAppController` / `DdcNamespaceController`）：
  - `GET /api/v1/ddc/configs?appCode&env&namespace&configKey&includeDeleted=false` → `data: DdcConfig[]`；字段：`id`、`appCode`、`env`、`namespace`、`configKey`、`configValue`、`defaultValue`、`valueType`、`currentVersion`、`description`、`createdAt`、`updatedAt`。
  - `POST /api/v1/ddc/configs` body `{ appCode, env, namespace, configKey, configValue, defaultValue, valueType, description }` → `data: DdcConfig`。
  - `PUT /api/v1/ddc/configs/{id}` body `{ configValue, changeReason, currentVersion }` → `data: DdcConfig`。
  - `DELETE /api/v1/ddc/configs/{id}?operator=local-admin&reason=delete config` → `data: DdcConfig`。
  - `POST /api/v1/ddc/configs/{id}/publish` body `{ changeId, configValue, expectedVersion, timeoutMs }` → `data: DdcPublishResult`，字段 `changeId`、`status`、`targetCount`、`ackCount`、`failedCount`、`ignoredCount`、`timeoutCount`、`attemptCount`、`targetVersion`、`contentChecksum`、`errorMessage`。
  - `GET /api/v1/ddc/configs/{id}/versions` → `data: DdcConfigVersion[]`；字段 `id`、`configId`、`version`、`oldValue`、`newValue`、`changeType`、`changeReason`、`operator`、`createdAt`。
  - `POST /api/v1/ddc/configs/{id}/rollback` body `{ configId, version, reason }` → `data: DdcConfig`。
  - 应用/命名空间自动补齐（创建配置前，对齐旧 `ensureScope`）：`GET /api/v1/ddc/apps` → 若无 `appCode` 匹配则 `POST /api/v1/ddc/apps`（body 见 Global Constraints）；`GET /api/v1/ddc/namespaces?appCode&env` → 若无 `namespace` 匹配则 `POST /api/v1/ddc/namespaces`。
- Produces:
  - `src/api/types.ts` 新增 `DdcConfig`、`DdcPublishResult`、`DdcConfigVersion` 类型（字段如上）。
  - `ConfigEditorDialog`（Props `{ open: boolean; config: DdcConfig | null; defaultScope: { appCode: string; env: string; namespace: string }; onClose: () => void; onSaved: () => void }`）：antd Modal + Form；编辑态锁定 appCode/env/namespace/configKey；字段：configKey、valueType（Select：STRING/JSON/INTEGER/BOOLEAN/YAML/TOML，默认 STRING）、configValue（TextArea，key/type/value 变化时重算 format 并显示格式徽标与 notice）、defaultValue（仅新建）、description、changeReason（仅编辑，默认 'DDC Admin Web update'）；保存：新建先 ensureScope → `serializeConfigEditor` 序列化后 POST；编辑 PUT `{ configValue, changeReason, currentVersion }`。
  - `ConfigsPage`：筛选行（appCode、env、namespace、configKey + 查询按钮 + 新建按钮）；Table：configKey、valueType、format（`detectConfigFormat` 徽标）、configValue 预览（`prepareConfigEditor(config).content` 压缩为单行，>96 字符截断，附 description 小字）、currentVersion、updatedAt、操作（编辑/发布/删除/版本）；发布/删除/回滚的确认统一用 `window.confirm`（对齐旧 UI："确认发布 {configKey} 当前版本？" / "确认删除 {configKey}？"），确认后调接口、`message.success(\`发布任务 ${result.changeId}：${result.status}\`)` 并刷新；版本对话框：`Modal` 内 `Table` 展示 `GET versions` 结果（列：version、changeType、changeReason、operator、createdAt、newValue 截断），每行"回滚"按钮 `window.confirm` 后 `POST rollback` body `{ configId: id, version, reason: 'rollback from DDC Admin Web' }`。

- [ ] **Step 1: 写失败的测试 `ConfigsPage.test.tsx`**

```tsx
import { render, screen, waitFor, fireEvent } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { setDdcTokenProvider, setDdcUnauthorizedHandler } from '../api/client'
import ConfigsPage from './ConfigsPage'

const record = (data: unknown) => ({
  success: true, code: 0, status: 'SUCCESS', message: '', data, traceId: 't', timestamp: 1,
})

const configRow = {
  id: 'cfg-1', appCode: 'orders', env: 'dev', namespace: 'default',
  configKey: 'feature.flags', configValue: '{"enabled":true}', defaultValue: '',
  valueType: 'JSON', currentVersion: 3, description: '功能开关',
  createdAt: '2026-07-01T00:00:00Z', updatedAt: '2026-07-02T00:00:00Z',
}

describe('ConfigsPage', () => {
  beforeEach(() => {
    setDdcTokenProvider(() => 'token')
    setDdcUnauthorizedHandler(() => {})
    vi.stubGlobal('fetch', vi.fn())
  })

  it('renders config rows with format badge and actions', async () => {
    vi.mocked(fetch).mockResolvedValue(new Response(JSON.stringify(record([configRow])), { status: 200 }))
    render(<ConfigsPage />)
    await waitFor(() => expect(screen.getByText('feature.flags')).toBeInTheDocument())
    expect(screen.getByText('JSON')).toBeInTheDocument()
    expect(screen.getByText('功能开关')).toBeInTheDocument()
  })

  it('publishes with uuid changeId and refreshes', async () => {
    vi.mocked(fetch)
      .mockResolvedValueOnce(new Response(JSON.stringify(record([configRow])), { status: 200 }))
      .mockResolvedValueOnce(new Response(JSON.stringify(record({ changeId: 'change-1', status: 'SUCCESS' })), { status: 200 }))
      .mockResolvedValue(new Response(JSON.stringify(record([configRow])), { status: 200 }))
    const confirm = vi.spyOn(window, 'confirm').mockReturnValue(true)
    render(<ConfigsPage />)
    await waitFor(() => expect(screen.getByText('feature.flags')).toBeInTheDocument())
    fireEvent.click(screen.getByText('发布'))
    await waitFor(() => expect(screen.getByText(/发布任务 change-1/)).toBeInTheDocument())
    confirm.mockRestore()
  })
})
```

- [ ] **Step 2: 运行确认失败**

Run: `cd <web> && npx vitest run src/pages/ConfigsPage.test.tsx`
Expected: FAIL（占位组件）。

- [ ] **Step 3: 实现 `ConfigEditorDialog.tsx`**

要点（antd Modal + Form）：`open` 控制 `Modal`；`Form` 初值从 `config` 或 `defaultScope` 派生；`configValue`/`configKey`/`valueType` 变化时 `setEditor(prepareConfigEditor({ configKey, configValue, valueType }))`，展示 `editor.format`（Tag）与 `editor.notice`（Typography.Text type="secondary"）；保存时：

```ts
const configValue = await serializeConfigEditor(editor, formValue.configValue)
if (editing) {
  await ddcApi(`/api/v1/ddc/configs/${encodeURIComponent(config.id)}`, {
    method: 'PUT',
    body: { configValue, changeReason: formValue.changeReason || 'DDC Admin Web update', currentVersion: config.currentVersion },
  })
} else {
  await ensureAppAndNamespace(scope)
  await ddcApi('/api/v1/ddc/configs', {
    method: 'POST',
    body: { ...scope, configKey: formValue.configKey, configValue, defaultValue: serializedDefault, valueType: formValue.valueType, description: formValue.description },
  })
}
```

`ensureAppAndNamespace`（对齐旧 `ensureScope`，`message.success(\`作用域 ${appCode}/${env}/${namespace} 已就绪\`)`）与 `serializedDefault`（默认值为空串则不传 `defaultValue`，否则同样过 `serializeConfigEditor`）按旧 app.js 行为实现。

- [ ] **Step 4: 实现 `ConfigsPage.tsx`**

按 Interfaces 描述的表格与操作实现；版本对话框：`Modal` 内 `Table` 展示 `GET versions` 结果，每行"回滚"按钮 `Modal.confirm` 后 `POST rollback`。

- [ ] **Step 5: 运行测试确认通过**

Run: `cd <web> && npx vitest run src/pages/ConfigsPage.test.tsx && npm run typecheck`
Expected: PASS（2 条）。

- [ ] **Step 6: Commit**

```bash
git add <web>/src/pages/ConfigsPage.tsx <web>/src/pages/ConfigsPage.test.tsx <web>/src/pages/ConfigEditorDialog.tsx <web>/src/api/types.ts
git commit -m "feat(ddc-admin-web): add config management page with editor dialog"
```

---

### Task 8: 应用与命名空间页

**Files:**
- Modify: `<web>/src/pages/AppsPage.tsx`、`<web>/src/pages/NamespacesPage.tsx`（替换占位）
- Create: `<web>/src/pages/AppsPage.test.tsx`、`<web>/src/pages/NamespacesPage.test.tsx`
- Modify: `<web>/src/api/types.ts`

**Interfaces:**
- Consumes: Task 2 `ddcApi`。
- 契约：
  - `GET /api/v1/ddc/apps` → `data: DdcApp[]`；字段 `id`、`appCode`、`appName`、`owner`、`description`、`enabled`、`createdAt`、`updatedAt`。
  - `POST /api/v1/ddc/apps` body `{ appCode, appName, owner, description, enabled }` → `data: DdcApp`。
  - `GET /api/v1/ddc/namespaces?appCode&env` → `data: DdcNamespace[]`；字段 `id`、`appCode`、`env`、`namespace`、`description`、`enabled`、`createdAt`、`updatedAt`。
  - `POST /api/v1/ddc/namespaces` body `{ appCode, env, namespace, description, enabled }` → `data: DdcNamespace`。
- Produces: `src/api/types.ts` 新增 `DdcApp`、`DdcNamespace`；`AppsPage`（Table：appCode、appName、owner、enabled（Tag）、description、updatedAt + 新建 Modal：appCode/appName/owner/description/enabled）；`NamespacesPage`（筛选 appCode + env；Table：appCode、env、namespace、enabled、description + 新建 Modal：appCode/env/namespace/description/enabled）。

- [ ] **Step 1: 写失败的测试（各一条用例，模式同 Task 6 Step 1：mock fetch 返回 record 数组，断言表格渲染与新建提交 body）**

`AppsPage.test.tsx`：

```tsx
import { render, screen, waitFor, fireEvent } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { setDdcTokenProvider, setDdcUnauthorizedHandler } from '../api/client'
import AppsPage from './AppsPage'

const record = (data: unknown) => ({ success: true, code: 0, status: 'SUCCESS', message: '', data, traceId: 't', timestamp: 1 })

describe('AppsPage', () => {
  beforeEach(() => {
    setDdcTokenProvider(() => 'token')
    setDdcUnauthorizedHandler(() => {})
    vi.stubGlobal('fetch', vi.fn())
  })

  it('renders apps and creates a new one', async () => {
    vi.mocked(fetch)
      .mockResolvedValueOnce(new Response(JSON.stringify(record([{ id: 'a1', appCode: 'orders', appName: '订单服务', owner: 'ops', description: '', enabled: true, createdAt: '2026-07-01T00:00:00Z', updatedAt: '2026-07-01T00:00:00Z' }])), { status: 200 }))
      .mockResolvedValueOnce(new Response(JSON.stringify(record({ id: 'a2', appCode: 'billing', appName: 'billing', owner: 'local-admin', description: '', enabled: true, createdAt: '2026-07-02T00:00:00Z', updatedAt: '2026-07-02T00:00:00Z' })), { status: 200 }))
      .mockResolvedValue(new Response(JSON.stringify(record([])), { status: 200 }))
    render(<AppsPage />)
    await waitFor(() => expect(screen.getByText('订单服务')).toBeInTheDocument())
    fireEvent.click(screen.getByText('新建应用'))
    fireEvent.change(screen.getByLabelText('应用编码'), { target: { value: 'billing' } })
    fireEvent.click(screen.getByRole('button', { name: '保存' }))
    await waitFor(() => expect(screen.getByText('billing')).toBeInTheDocument())
    const body = JSON.parse(String(vi.mocked(fetch).mock.calls[1][1]?.body))
    expect(body).toMatchObject({ appCode: 'billing', appName: 'billing', enabled: true })
  })
})
```

（`NamespacesPage.test.tsx` 同构：渲染 `orders/dev/default` 行 + 新建 `billing/prod/primary` 提交 body 断言。）

- [ ] **Step 2: 运行确认失败**

Run: `cd <web> && npx vitest run src/pages/AppsPage.test.tsx src/pages/NamespacesPage.test.tsx`
Expected: FAIL（占位组件）。

- [ ] **Step 3: 实现两个页面**（antd Table + Modal 表单，字段按契约；新建成功后 `message.success('应用已保存' / '命名空间已保存')` 并刷新；表格时间列 `toLocaleString('zh-CN', { hour12: false })`，空值 '—'）

- [ ] **Step 4: 运行测试确认通过**

Run: `cd <web> && npx vitest run src/pages/AppsPage.test.tsx src/pages/NamespacesPage.test.tsx && npm run typecheck`
Expected: PASS（各 1 条）。

- [ ] **Step 5: Commit**

```bash
git add <web>/src/pages/AppsPage.tsx <web>/src/pages/AppsPage.test.tsx <web>/src/pages/NamespacesPage.tsx <web>/src/pages/NamespacesPage.test.tsx <web>/src/api/types.ts
git commit -m "feat(ddc-admin-web): add apps and namespaces pages"
```

---

### Task 9: 发布任务、实例、缓存页

**Files:**
- Modify: `<web>/src/pages/PublishTasksPage.tsx`、`<web>/src/pages/InstancesPage.tsx`、`<web>/src/pages/CachePage.tsx`（替换占位）
- Create: `<web>/src/pages/PublishTasksPage.test.tsx`
- Modify: `<web>/src/api/types.ts`

**Interfaces:**
- Consumes: Task 2 `ddcApi`。
- 契约（后端 controller 对应字段见 Task 1 探索记录；以 `grep -n "private "` 核对实体）：
  - `GET /api/v1/ddc/publish-tasks` → `data: DdcPublishTask[]`；字段：`id`、`changeId`、`configId`、`appCode`、`env`、`namespace`、`configKey`、`targetVersion`、`publishMode`、`contentChecksum`、`attemptCount`、`dispatchedAt`、`completedAt`、`failureStage`、`status`、`targetCount`、`ackCount`、`failedCount`、`ignoredCount`、`timeoutCount`、`timeoutMs`、`operator`、`errorMessage`、`createdAt`、`updatedAt`。
  - `GET /api/v1/ddc/publish-tasks/{changeId}` → `data: DdcPublishTask`。
  - `POST /api/v1/ddc/publish-tasks/{changeId}/retry?operator=local-admin` → `data: DdcPublishResult`（`changeId`、`status`）。
  - `GET /api/v1/ddc/instances?appCode&env` → `data: DdcInstance[]`；字段：`id`、`instanceId`、`appCode`、`env`、`namespace`、`host`、`port`、`pid`、`sdkVersion`、`leaseId`、`leaseExpireAt`、`status`、`lastHeartbeatAt`、`createdAt`、`updatedAt`、`runtimeMetadata`（`Record<string, string>`）。
  - `POST /api/v1/ddc/cache/rebuild?appCode&env` → `data: number`（重建条数）。
  - `GET /api/v1/ddc/cache/check?appCode&env` → `data: DdcCacheCheckRow[]`；字段 `configKey`、`databaseValue`、`redisValue`、`databaseVersion`、`redisVersion`、`matched`（boolean）。
- Produces: `src/api/types.ts` 新增 `DdcPublishTask`、`DdcInstance`、`DdcCacheCheckRow`、`DdcPublishResult`（字段同 Task 7 定义）；三个页面组件：
  - `PublishTasksPage`：Table（changeId、appCode/env/namespace/configKey、targetVersion、status Tag、attemptCount、targetCount/ackCount/failedCount/timeoutCount、operator、createdAt、updatedAt、errorMessage 截断）+ "重试"按钮（`window.confirm` 后 POST retry，成功后 `message.success(\`重试任务 ${result.changeId}：${result.status}\`)` 并刷新）+ 点击 changeId 打开详情 Modal（`GET {changeId}` 全字段只读展示）。页面提供手动刷新按钮，并每 15 秒自动刷新一次（`useEffect` + `setInterval`，卸载时清理），满足 spec 的发布任务轮询约定。
  - `InstancesPage`：筛选 appCode + env；Table（status Tag、instanceId、host:port、pid、sdkVersion、leaseId、leaseExpireAt、lastHeartbeatAt）+ 展开行或 Modal 展示 `runtimeMetadata`（key-value 列表）。
  - `CachePage`：筛选 appCode + env；"重建缓存"按钮（`window.confirm` 后 POST rebuild，`message.success(\`已重建 ${n} 项缓存\`)`）；"检查缓存"按钮 → 表格渲染 `check` 结果（列：configKey、databaseVersion/redisVersion（不一致时红色）、matched（Tag：一致/不一致）、databaseValue/redisValue 截断）。

- [ ] **Step 1: 写失败的测试 `PublishTasksPage.test.tsx`**

```tsx
import { render, screen, waitFor, fireEvent } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { setDdcTokenProvider, setDdcUnauthorizedHandler } from '../api/client'
import PublishTasksPage from './PublishTasksPage'

const record = (data: unknown) => ({
  success: true, code: 0, status: 'SUCCESS', message: '', data, traceId: 't', timestamp: 1,
})

const task = {
  id: 't-1', changeId: 'change-9', configId: 'cfg-1', appCode: 'orders', env: 'dev',
  namespace: 'default', configKey: 'feature.flags', targetVersion: 4, publishMode: 'SYNC',
  contentChecksum: 'abc', attemptCount: 1, dispatchedAt: '2026-07-31T10:00:00Z',
  completedAt: '2026-07-31T10:00:05Z', failureStage: null, status: 'SUCCESS',
  targetCount: 3, ackCount: 3, failedCount: 0, ignoredCount: 0, timeoutCount: 0,
  timeoutMs: 30000, operator: 'local-admin', errorMessage: null,
  createdAt: '2026-07-31T10:00:00Z', updatedAt: '2026-07-31T10:00:05Z',
}

describe('PublishTasksPage', () => {
  beforeEach(() => {
    vi.useFakeTimers({ shouldAdvanceTime: true })
    setDdcTokenProvider(() => 'token')
    setDdcUnauthorizedHandler(() => {})
    vi.stubGlobal('fetch', vi.fn())
  })

  it('renders publish tasks and retries a failed one', async () => {
    vi.mocked(fetch)
      .mockResolvedValueOnce(new Response(JSON.stringify(record([task])), { status: 200 }))
      .mockResolvedValueOnce(new Response(JSON.stringify(record({ changeId: 'change-9', status: 'SUCCESS', targetCount: 3, ackCount: 3, failedCount: 0, ignoredCount: 0, timeoutCount: 0, attemptCount: 2, targetVersion: 4, contentChecksum: 'abc', errorMessage: null })), { status: 200 }))
      .mockResolvedValue(new Response(JSON.stringify(record([task])), { status: 200 }))
    const confirm = vi.spyOn(window, 'confirm').mockReturnValue(true)

    render(<PublishTasksPage />)
    await waitFor(() => expect(screen.getByText('change-9')).toBeInTheDocument())
    expect(screen.getByText('orders')).toBeInTheDocument()
    fireEvent.click(screen.getByText('重试'))
    await waitFor(() => expect(screen.getByText(/重试任务 change-9/)).toBeInTheDocument())
    confirm.mockRestore()
    vi.useRealTimers()
  })
})
```

- [ ] **Step 2: 运行确认失败**

Run: `cd <web> && npx vitest run src/pages/PublishTasksPage.test.tsx`
Expected: FAIL（占位组件）。

- [ ] **Step 3: 实现三个页面**（按契约与 Produces 描述；状态 Tag 颜色：SUCCESS 绿、FAILED/PENDING 橙/红、其它灰，`status` 为后端原值）

- [ ] **Step 4: 运行测试确认通过**

Run: `cd <web> && npx vitest run src/pages/PublishTasksPage.test.tsx && npm run typecheck && npm run lint`
Expected: PASS（1 条），typecheck/lint 通过。

- [ ] **Step 5: Commit**

```bash
git add <web>/src/pages/PublishTasksPage.tsx <web>/src/pages/PublishTasksPage.test.tsx <web>/src/pages/InstancesPage.tsx <web>/src/pages/CachePage.tsx <web>/src/api/types.ts
git commit -m "feat(ddc-admin-web): add publish tasks, instances and cache pages"
```

---

### Task 10: static-server 与 Dockerfile（DDC 前缀）

**Files:**
- Create: `<web>/static-server.mjs`
- Create: `<web>/Dockerfile`
- Create: `<web>/README.md`、`<web>/README.zh-CN.md`（部署与开发说明）

**Interfaces:**
- Consumes: Task 1 的构建产物 `dist/`。
- Produces: 独立可部署容器：`node:22-alpine`，`ENV PORT=8080 DDC_ADMIN_API_BASE_URL=http://ddc-admin:18080 DDC_ADMIN_API_DEVELOPMENT_PLAINTEXT=true`，`USER node`，`EXPOSE 8080`，`ENTRYPOINT ["node", "/app/static-server.mjs"]`。

- [ ] **Step 1: 写 `static-server.mjs`**

以 `<gateway-web>/static-server.mjs` 为模板逐段移植，以下三点必须替换：

```js
const apiBase = new URL(
  process.env.DDC_ADMIN_API_BASE_URL ?? 'http://ddc-admin:18080',
)
const developmentPlaintext =
  process.env.DDC_ADMIN_API_DEVELOPMENT_PLAINTEXT === 'true'
```

错误码（`proxy` 的 error 分支）改为：

```js
outgoing.end('{"code":"DDC_ADMIN_WEB_UPSTREAM_UNAVAILABLE"}')
```

其余逻辑（mTLS 文件读取、contentTypes 表、`/healthz`、`/api/` 反代、SPA fallback 到 index.html、`no-cache`/`immutable` 缓存头、PORT 校验）与 gateway 模板完全一致。

- [ ] **Step 2: 写 `Dockerfile`**

```dockerfile
FROM node:22-alpine AS build

WORKDIR /workspace
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
RUN npm run build

FROM node:22-alpine

ENV PORT=8080
ENV DDC_ADMIN_API_BASE_URL=http://ddc-admin:18080
ENV DDC_ADMIN_API_DEVELOPMENT_PLAINTEXT=true

WORKDIR /app
COPY --from=build --chown=node:node /workspace/dist /app/dist
COPY --chown=node:node static-server.mjs /app/static-server.mjs

USER node
EXPOSE 8080

ENTRYPOINT ["node", "/app/static-server.mjs"]
```

- [ ] **Step 3: 本地验证**

```bash
cd <web>
npm run build
node static-server.mjs &
sleep 1
curl -s http://127.0.0.1:8080/healthz   # 期望: ok
curl -s -o /dev/null -w '%{http_code}' http://127.0.0.1:8080/  # 期望: 200
curl -s -o /dev/null -w '%{http_code}' http://127.0.0.1:8080/api/v1/ddc/apps  # 期望: 502（admin 不可达时）
kill %1
```

- [ ] **Step 4: 写 README（中英）**：模块定位（独立管理控制台，仅调 DDC Admin）、开发命令（`npm ci && npm run typecheck && npm test -- --run && npm run lint && npm run build`）、部署（`docker build` + 环境变量表：`DDC_ADMIN_API_BASE_URL` / `DDC_ADMIN_API_DEVELOPMENT_PLAINTEXT` / `PORT`）、e2e 前置条件（可达的 admin + `DDC_E2E_TOKEN`，见 Task 11）。

- [ ] **Step 5: Commit**

```bash
git add <web>/static-server.mjs <web>/Dockerfile <web>/README.md <web>/README.zh-CN.md
git commit -m "feat(ddc-admin-web): add static server, dockerfile and docs"
```

---

### Task 11: Playwright e2e 冒烟

**Files:**
- Create: `<web>/playwright.config.ts`
- Create: `<web>/e2e/ddc-admin.spec.ts`

**Interfaces:**
- Consumes: 部署好的 admin（`DDC_E2E_ADMIN_URL`，默认 `http://127.0.0.1:18080`）+ 有效 token（`DDC_E2E_TOKEN`）；Task 1 的 dev server 与 Task 10 的 static-server。
- Produces: `npm run e2e` 可跑通冒烟：登录 → 服务注册加载 → 配置管理加载。

- [ ] **Step 1: 写 `playwright.config.ts`**

```ts
import { defineConfig } from '@playwright/test'

export default defineConfig({
  testDir: './e2e',
  use: {
    baseURL: 'http://127.0.0.1:4173',
    trace: 'on-first-retry',
  },
  webServer: {
    command: 'npm run build && npm run preview -- --port 4173 --strictPort',
    url: 'http://127.0.0.1:4173',
    reuseExistingServer: true,
  },
})
```

（`vite preview` 的 `/api` 代理已在 Task 1 的 `vite.config.ts` `preview` 段配置。）

- [ ] **Step 2: 写 `e2e/ddc-admin.spec.ts`**

```ts
import { expect, test } from '@playwright/test'

const token = process.env.DDC_E2E_TOKEN ?? ''
const adminUrl = process.env.DDC_E2E_ADMIN_URL ?? 'http://127.0.0.1:18080'

test('admin console smoke: login, registry, configs', async ({ page }) => {
  test.skip(token === '', 'DDC_E2E_TOKEN is required')

  await page.goto('/')
  await page.getByPlaceholder('粘贴 admin.token 内容').fill(token)
  await page.getByRole('button', { name: '登录并加载' }).click()

  await expect(page.getByText('服务注册目录')).toBeVisible()
  await page.getByRole('menuitem', { name: '配置管理' }).click()
  await expect(page.getByRole('button', { name: '新建配置' })).toBeVisible()
})
```

（页面文案以 Task 6/7 实际实现为准，若"服务注册目录"未出现在 RegistryPage 中，改为断言 `page.getByText('DDC 已连接')` 或注册表格存在。）

- [ ] **Step 3: 本地验证（admin 可达时）**

```bash
cd <web>
npx playwright install chromium
DDC_E2E_TOKEN=<有效 token> npm run e2e
```

Expected: 1 条 e2e PASS（无 token 时 SKIP）。

- [ ] **Step 4: Commit**

```bash
git add <web>/playwright.config.ts <web>/e2e <web>/vite.config.ts
git commit -m "test(ddc-admin-web): add playwright smoke e2e"
```

---

### Task 12: 清理 admin 模块旧 webui

**Files:**
- Delete: `admin/src/main/resources/static/ddc-admin/`（整个目录）
- Delete: `admin/src/test/js/`（整个目录）
- Modify: `admin/src/test/java/top/egon/cola/component/ddc/admin/web/DdcAdminWebResourceTest.java`（重写为断言资源已移除）
- Modify: `admin/src/test/java/top/egon/cola/component/ddc/admin/security/DdcAdminSecurityIntegrationTest.java:118`（`/ddc-admin/index.html` 断言改为 404）
- Modify: `admin/src/main/java/top/egon/cola/component/ddc/admin/security/DdcAdminSecurityConfiguration.java:51-52`（移除 `/ddc-admin`、`/ddc-admin/**` permitAll）
- Modify: `admin/pom.xml`（version 5.3.2 → 5.4.0）

**Interfaces:**
- Consumes: Task 1-11 的 admin-web 工程已验收可独立部署。
- Produces: admin jar 不再含任何 webui 资源；`/ddc-admin` 返回 401/404（由默认认证行为决定）；admin 侧测试全绿。

- [ ] **Step 1: 删除静态资源与 JS 测试**

```bash
git rm -r admin/src/main/resources/static/ddc-admin admin/src/test/js
```

- [ ] **Step 2: 重写 `DdcAdminWebResourceTest`**

```java
package top.egon.cola.component.ddc.admin.web;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DdcAdminWebResourceTest {

    @Test
    void noLongerShipsTheBundledAdminWeb() {
        assertThat(DdcAdminWebResourceTest.class.getClassLoader()
                .getResource("static/ddc-admin/index.html"))
                .as("the admin jar must not bundle the extracted ddc-admin web")
                .isNull();
    }
}
```

- [ ] **Step 3: 更新 `DdcAdminSecurityIntegrationTest.java:118`**

原断言（约 118 行，`mockMvc.perform(get("/ddc-admin/index.html"))` 期望 200）改为：

```java
mockMvc.perform(get("/ddc-admin/index.html"))
        .andExpect(status().is4xxClientError());
```

（先读该测试上下文确认断言写法，保持与文件内既有风格一致。）

- [ ] **Step 4: 移除 permitAll 中的 `/ddc-admin` 与 `/ddc-admin/**`**

`DdcAdminSecurityConfiguration.java` 中：

```java
.requestMatchers(
        "/api/v1/ddc/manifest",
        "/ddc-admin",
        "/ddc-admin/**",
        "/actuator/health/**",
        "/actuator/info"
).permitAll()
```

改为：

```java
.requestMatchers(
        "/api/v1/ddc/manifest",
        "/actuator/health/**",
        "/actuator/info"
).permitAll()
```

- [ ] **Step 5: bump admin 版本**

`admin/pom.xml`：`<version>5.3.2</version>` → `<version>5.4.0</version>`（parent 引用不改，DDC parent 仍为 5.3.2；仅本模块 artifact 版本上浮，表示破坏性变更）。

- [ ] **Step 6: 运行 admin 侧测试**

Run: `cd <ddc-platform>/egon-cola-platform-dynamic-config-center-admin && mvn -q test`
Expected: BUILD SUCCESS（含重写后的 `DdcAdminWebResourceTest`、`DdcAdminSecurityIntegrationTest` 全绿）。

- [ ] **Step 7: 全仓 grep 确认无残留**

Run: `grep -rn "ddc-admin/index.html\|static/ddc-admin" admin/src || true`
Expected: 无输出。

- [ ] **Step 8: Commit**

```bash
git add -A
git commit -m "refactor(ddc-admin): remove bundled webui after extraction to admin-web"
```

---

### Task 13: 平台文档更新

**Files:**
- Modify: `egon-cola-platform-dynamic-config-center/README.md`、`README.zh-CN.md`

**Interfaces:**
- Consumes: Task 12 完成后的最终结构。
- Produces: README 反映新模块结构（starter / admin / admin-web / test），并指向 `<web>/README.md`。

- [ ] **Step 1: 更新 README**

在模块列表增加：

```markdown
- `egon-cola-platform-dynamic-config-center-admin-web` — 独立管理控制台（React + antd + Vite），
  构建与部署说明见 `egon-cola-platform-dynamic-config-center-admin-web/README.md`。
```

并在"管理端访问"相关段落说明：webui 已从 admin jar 摘出，`/ddc-admin` 不再由 admin 提供服务；管理控制台经 `DDC_ADMIN_API_BASE_URL` 指向 admin。

- [ ] **Step 2: Commit**

```bash
git add README.md README.zh-CN.md
git commit -m "docs(ddc): document admin-web module after extraction"
```

---

## 验收清单（对照 spec）

- [ ] 新旧功能对等：服务注册/实例浏览、配置 CRUD+发布+版本回滚，行为与旧 webui 一致（对比旧 app.js 行为列表）。
- [ ] 新增能力可用：发布任务列表/详情/重试、实例管理、缓存重建/检查、应用/命名空间管理。
- [ ] `<web>` 全部 vitest 通过；`npm run lint`、`npm run typecheck`、`npm run build` 通过。
- [ ] e2e 冒烟在有 token 时通过（或无 token 时 SKIP）。
- [ ] admin jar 不再含 `static/ddc-admin`；`/ddc-admin` 不再可达；`/api/v1/ddc/**` 不受影响（admin 测试全绿）。
- [ ] 独立部署验证：`docker build` 出的镜像 `curl /healthz` 返回 ok，`/` 返回 index.html，`/api/**` 反代到 admin。
