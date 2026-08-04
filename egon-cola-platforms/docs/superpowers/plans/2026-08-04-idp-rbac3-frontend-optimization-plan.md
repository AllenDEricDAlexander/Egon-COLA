# IDP & RBAC3 前端企业级优化 — 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 新建共享包 `@egon-cola/admin-web-shared`（OAuth/HTTP/JWT/组件/主题/i18n），重构 IDP 和 RBAC3 admin-web 消费共享包，修复所有已知 Bug，达企业级前端水平。

**Architecture:** 方案 C — 统一基础设施先行。共享包通过 Vite library mode 构建为 ES module，消费项目通过 `file:` 引用。两个项目共享 OAuth client、HTTP client、JWT 工具、通用组件、设计 token、i18n 框架。RBAC3 独有的 SDK 适配逻辑保留在项目内。

**Tech Stack:** React 19、antd 6、TypeScript 6、Vite 8、Vitest 4、@tanstack/react-query 5、react-router-dom 7、i18next + react-i18next

## Global Constraints

- antd ≥6.5.2、react ≥19.2.8、typescript ≥6.0.3（与现有项目一致）
- 所有新文件使用 `readonly` 修饰符（与现有代码风格一致）
- 共享包 peerDependencies 不重复打包（react/react-dom/antd/react-query/react-router-dom/i18next/react-i18next）
- 不使用 TBD/TODO——所有步骤包含实际可执行代码
- 每个 task 以独立 commit 结束
- OAuth 配置启动时校验——缺 env var 直接抛错，不静默 fallback 到 localhost
- JWT 解码统一使用 TextDecoder('utf-8')，不用 atob
- 所有组件支持 `onRetry` 回调用于错误恢复
- 路由级 React.lazy + Suspense 懒加载所有页面组件

---

## File Structure

### 新建: 共享包 `egon-cola-platform-admin-web-shared/`

```
egon-cola-platform-admin-web-shared/
├── package.json
├── tsconfig.json
├── tsconfig.app.json
├── vite.config.ts
└── src/
    ├── index.ts                    # barrel export
    ├── theme/
    │   ├── tokens.ts               # design tokens
    │   └── ThemeProvider.tsx        # ConfigProvider wrapper
    ├── api/
    │   ├── jwt.ts                  # JWT decode (TextDecoder), exp compute
    │   ├── jwt.test.ts
    │   ├── errors.ts               # ApiError class + classifyApiError
    │   └── httpClient.ts           # Unified fetch with credentials, retry, timeout
    │   └── httpClient.test.ts
    ├── auth/
    │   ├── tokenStore.ts           # In-memory token store with subscribe
    │   ├── tokenStore.test.ts
    │   ├── oauthClient.ts          # PKCE OAuth client (from IDP, bug-fixed)
    │   └── oauthClient.test.ts
    ├── i18n/
    │   ├── index.ts                # initI18n, useT, I18nProvider, changeLanguage
    │   ├── zh-CN.ts                # Chinese common strings
    │   └── en-US.ts                # English common strings
    ├── components/
    │   ├── PageState.tsx           # Enhanced loading/error/empty
    │   ├── AppErrorBoundary.tsx    # Error boundary with telemetry
    │   └── PageTemplate.tsx        # Page shell (Card + breadcrumb)
    └── hooks/
        ├── usePermission.ts        # permission check hook
        └── useFeatureQuery.ts      # RBAC3 query boilerplate eliminator
```

### 修改: IDP `egon-cola-platform-idp-admin-web/src/`

```
src/
├── main.tsx                        # 修改: 接入共享 ThemeProvider + I18nProvider
├── app/
│   ├── App.tsx                     # 修改: react-router 替换手写路由
│   └── router.tsx                  # 新建: 路由定义 + AdminLayout
├── features/                       # 新建目录
│   ├── overview/OverviewPage.tsx
│   ├── users/UserListPage.tsx
│   ├── clients/ClientListPage.tsx
│   ├── keys/SigningKeyPage.tsx
│   └── audits/AuditLogPage.tsx
├── auth/
│   ├── AuthContext.tsx             # 修改: 使用共享 oauthClient + tokenStore.subscribe
│   ├── CentralLoginPage.tsx        # 修改: 使用共享 oauthClient
│   └── CallbackPage.tsx            # 新建: 修复卡死 spinner
├── api/
│   ├── idpApi.ts                   # 修改: 基于共享 HttpClient
│   └── types.ts                    # 保留
└── styles/
    └── index.css                   # 删除
```

### 修改: RBAC3 `egon-cola-platform-rbac3-admin-web/src/`

```
src/
├── main.tsx                        # 修改: 接入共享 ThemeProvider + I18nProvider
├── app/
│   ├── App.tsx                     # 修改: 使用共享 ThemeProvider, ErrorBoundary
│   ├── router.tsx                  # 修改: lazy loading
│   ├── navigation.ts              # 修改: breadcrumb, fix ':' heuristic
│   └── queryClient.ts             # 保留
├── api/
│   └── adminApiClient.ts          # 修改: 基于共享 HttpClient
├── features/
│   ├── auth/
│   │   ├── oauthClient.ts         # 删除: 替换为共享包
│   │   └── oauthClient.test.ts    # 删除
│   ├── shared/
│   │   ├── PageState.tsx           # 删除: 替换为共享包
│   │   └── FeatureApi.tsx         # 保留: 改用共享 HttpClient
│   ├── governance.routes.ts       # 修改: lazy imports
│   ├── authorization.routes.ts    # 修改: lazy imports
│   ├── runtime.routes.ts          # 修改: lazy imports
│   └── (各 page 文件)              # 修改: useFeatureQuery + PageTemplate
└── styles/
    └── global.css                  # 删除
```

---

## Phase 1: 共享包骨架

### Task 1: 创建共享包 package.json 与 tsconfig

**Files:**
- Create: `egon-cola-platform-admin-web-shared/package.json`
- Create: `egon-cola-platform-admin-web-shared/tsconfig.json`
- Create: `egon-cola-platform-admin-web-shared/tsconfig.app.json`

**Interfaces:**
- Produces: package name `@egon-cola/admin-web-shared`, version `0.1.0`, type `module`

- [ ] **Step 1: 创建 package.json**

```jsonc
{
  "name": "@egon-cola/admin-web-shared",
  "version": "0.1.0",
  "private": true,
  "type": "module",
  "main": "./dist/admin-web-shared.js",
  "types": "./dist/index.d.ts",
  "exports": {
    ".": {
      "import": "./dist/admin-web-shared.js",
      "types": "./dist/index.d.ts"
    }
  },
  "scripts": {
    "build": "tsc -b && vite build",
    "typecheck": "tsc -b --pretty false",
    "test": "vitest run",
    "lint": "eslint ."
  },
  "peerDependencies": {
    "@tanstack/react-query": "^5.101.4",
    "antd": "^6.5.2",
    "i18next": "^24.0.0",
    "react": "^19.2.8",
    "react-dom": "^19.2.8",
    "react-i18next": "^15.0.0",
    "react-router-dom": "^7.18.0"
  },
  "devDependencies": {
    "@eslint/js": "^10.0.1",
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
    "vitest": "^4.1.10",
    "vite-plugin-dts": "^4.0.0"
  }
}
```

- [ ] **Step 2: 创建 tsconfig.json**

```jsonc
{
  "files": [],
  "references": [
    { "path": "./tsconfig.app.json" }
  ]
}
```

- [ ] **Step 3: 创建 tsconfig.app.json**

```jsonc
{
  "compilerOptions": {
    "target": "ES2024",
    "lib": ["ES2024", "DOM", "DOM.Iterable"],
    "module": "ESNext",
    "moduleResolution": "bundler",
    "jsx": "react-jsx",
    "strict": true,
    "noUnusedLocals": true,
    "noUnusedParameters": true,
    "exactOptionalPropertyTypes": true,
    "noUncheckedIndexedAccess": true,
    "verbatimModuleSyntax": true,
    "declaration": true,
    "declarationMap": true,
    "outDir": "./dist",
    "rootDir": "./src",
    "skipLibCheck": true
  },
  "include": ["src"]
}
```

- [ ] **Step 4: 创建 vite.config.ts**

```typescript
import { resolve } from 'node:path'
import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'
import dts from 'vite-plugin-dts'

export default defineConfig({
  plugins: [react(), dts({ include: ['src'], outDir: 'dist' })],
  build: {
    lib: {
      entry: resolve(__dirname, 'src/index.ts'),
      formats: ['es'],
      fileName: 'admin-web-shared',
    },
    rollupOptions: {
      external: [
        'react',
        'react-dom',
        'react/jsx-runtime',
        'antd',
        '@tanstack/react-query',
        'react-router-dom',
        'i18next',
        'react-i18next',
      ],
    },
  },
  test: {
    environment: 'jsdom',
    setupFiles: ['./src/test/setup.ts'],
  },
})
```

- [ ] **Step 5: 创建 src/test/setup.ts**

```typescript
import '@testing-library/jest-dom/vitest'

Object.defineProperty(window, 'matchMedia', {
  writable: true,
  value: (query: string) => ({
    matches: false,
    media: query,
    addEventListener: () => {},
    removeEventListener: () => {},
  }),
})
```

- [ ] **Step 6: 创建 src/vite-env.d.ts**

```typescript
/// <reference types="vite/client" />
```

- [ ] **Step 7: 安装依赖并验证构建**

```bash
cd egon-cola-platform-admin-web-shared && npm install
```

- [ ] **Step 8: Commit**

```bash
git add egon-cola-platform-admin-web-shared/
git commit -m "feat(shared): bootstrap admin-web-shared package skeleton"
```

---

## Phase 2: 共享包基础模块

### Task 2: Design tokens

**Files:**
- Create: `egon-cola-platform-admin-web-shared/src/theme/tokens.ts`

**Interfaces:**
- Produces: `designTokens` const object, `injectTokens()` function, `DesignTokens` type

- [ ] **Step 1: 创建 tokens.ts**

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
  font: {
    family: 'Inter, "PingFang SC", "Microsoft YaHei", ui-sans-serif, system-ui, -apple-system, sans-serif',
  },
} as const

export type DesignTokens = typeof designTokens

const CSS_VAR_PREFIX = '--egon-'

export const injectTokens = (): void => {
  const root = document.documentElement
  root.style.setProperty(`${CSS_VAR_PREFIX}color-primary`, designTokens.color.primary)
  root.style.setProperty(`${CSS_VAR_PREFIX}color-text`, designTokens.color.text)
  root.style.setProperty(`${CSS_VAR_PREFIX}color-text-secondary`, designTokens.color.textSecondary)
  root.style.setProperty(`${CSS_VAR_PREFIX}color-background`, designTokens.color.background)
  root.style.setProperty(`${CSS_VAR_PREFIX}color-background-alt`, designTokens.color.backgroundAlt)
  root.style.setProperty(`${CSS_VAR_PREFIX}color-border`, designTokens.color.border)
  root.style.setProperty(`${CSS_VAR_PREFIX}color-error`, designTokens.color.error)
  root.style.setProperty(`${CSS_VAR_PREFIX}color-warning`, designTokens.color.warning)
  root.style.setProperty(`${CSS_VAR_PREFIX}color-success`, designTokens.color.success)
  root.style.setProperty(`${CSS_VAR_PREFIX}font-family`, designTokens.font.family)
  root.style.fontFamily = designTokens.font.family
  root.style.color = designTokens.color.text
  root.style.background = designTokens.color.background

  const style = document.createElement('style')
  style.textContent = `
    body { margin: 0; min-height: 100vh; background: ${designTokens.color.background}; }
  `
  document.head.appendChild(style)
}
```

- [ ] **Step 2: Commit**

```bash
git add egon-cola-platform-admin-web-shared/src/theme/tokens.ts
git commit -m "feat(shared): add design tokens with CSS variable injection"
```

### Task 3: JWT utility (修复 UTF-8 bug，唯一实现)

**Files:**
- Create: `egon-cola-platform-admin-web-shared/src/api/jwt.ts`
- Create: `egon-cola-platform-admin-web-shared/src/api/jwt.test.ts`

**Interfaces:**
- Produces: `decodeTokenPayload(token: string): Record<string, unknown>`, `computeExpiresAt(token: string): Date | null`, `isTokenExpired(token: string): boolean`

- [ ] **Step 1: 编写 JWT 测试**

```typescript
// jwt.test.ts
import { describe, expect, it } from 'vitest'
import { computeExpiresAt, decodeTokenPayload, isTokenExpired } from './jwt'

const makeToken = (payload: Record<string, unknown>): string => {
  const header = btoa(JSON.stringify({ alg: 'RS256' }))
  const body = btoa(JSON.stringify(payload))
  return `${header}.${body}.signature`
}

describe('decodeTokenPayload', () => {
  it('decodes standard JWT payload', () => {
    const token = makeToken({ sub: 'user-1', exp: 2000000000 })
    const result = decodeTokenPayload(token)
    expect(result.sub).toBe('user-1')
  })

  it('decodes non-ASCII (UTF-8) claims without corruption', () => {
    const token = makeToken({ name: '张三', displayName: '管理员' })
    const result = decodeTokenPayload(token)
    expect(result.name).toBe('张三')
    expect(result.displayName).toBe('管理员')
  })

  it('throws on malformed token', () => {
    expect(() => decodeTokenPayload('not.a.token')).toThrow()
  })

  it('throws on empty token', () => {
    expect(() => decodeTokenPayload('')).toThrow()
  })
})

describe('computeExpiresAt', () => {
  it('returns Date from numeric exp', () => {
    const token = makeToken({ exp: 2000000000 })
    const result = computeExpiresAt(token)
    expect(result).toBeInstanceOf(Date)
    expect(result!.getTime()).toBe(2000000000 * 1000)
  })

  it('returns Date from string exp', () => {
    const token = makeToken({ exp: '2000000000' })
    const result = computeExpiresAt(token)
    expect(result!.getTime()).toBe(2000000000 * 1000)
  })

  it('returns null when no exp claim', () => {
    const token = makeToken({ sub: 'x' })
    expect(computeExpiresAt(token)).toBeNull()
  })
})

describe('isTokenExpired', () => {
  it('returns false for future token', () => {
    const future = Math.floor(Date.now() / 1000) + 3600
    expect(isTokenExpired(makeToken({ exp: future }))).toBe(false)
  })

  it('returns true for past token', () => {
    const past = Math.floor(Date.now() / 1000) - 3600
    expect(isTokenExpired(makeToken({ exp: past }))).toBe(true)
  })

  it('returns false when no exp claim', () => {
    expect(isTokenExpired(makeToken({ sub: 'x' }))).toBe(false)
  })
})
```

- [ ] **Step 2: 运行测试验证失败**

```bash
cd egon-cola-platform-admin-web-shared && npx vitest run src/api/jwt.test.ts
```
Expected: FAIL — module not found

- [ ] **Step 3: 实现 jwt.ts**

```typescript
const textDecoder = new TextDecoder('utf-8')

export const decodeTokenPayload = (token: string): Record<string, unknown> => {
  const parts = token.split('.')
  if (parts.length !== 3) {
    throw new Error('Invalid JWT format: expected 3 parts')
  }
  const payload = parts[1]!
  const normalized = payload.replace(/-/g, '+').replace(/_/g, '/')
  try {
    const binary = Uint8Array.from(atob(normalized), (c) => c.charCodeAt(0))
    const json = textDecoder.decode(binary)
    return JSON.parse(json) as Record<string, unknown>
  } catch (cause) {
    throw new Error('Failed to decode JWT payload', { cause })
  }
}

export const computeExpiresAt = (token: string): Date | null => {
  const claims = decodeTokenPayload(token)
  const exp = claims.exp
  if (exp === undefined) return null
  const seconds = typeof exp === 'string' ? Number(exp) : (exp as number)
  if (!Number.isFinite(seconds) || seconds <= 0) return null
  return new Date(seconds * 1000)
}

export const isTokenExpired = (token: string): boolean => {
  const expiresAt = computeExpiresAt(token)
  if (expiresAt === null) return false
  return Date.now() > expiresAt.getTime()
}
```

- [ ] **Step 4: 运行测试验证通过**

```bash
cd egon-cola-platform-admin-web-shared && npx vitest run src/api/jwt.test.ts
```
Expected: 8 tests PASS

- [ ] **Step 5: Commit**

```bash
git add egon-cola-platform-admin-web-shared/src/api/jwt.ts egon-cola-platform-admin-web-shared/src/api/jwt.test.ts
git commit -m "feat(shared): add JWT utility with TextDecoder UTF-8 fix"
```

### Task 4: API errors 分类体系

**Files:**
- Create: `egon-cola-platform-admin-web-shared/src/api/errors.ts`

**Interfaces:**
- Produces: `ApiError` class, `classifyApiError(error: unknown): ErrorClassification`

- [ ] **Step 1: 创建 errors.ts**

```typescript
export class ApiError extends Error {
  readonly status: number
  readonly code: string
  readonly requestId?: string
  readonly retryable: boolean

  constructor(
    message: string,
    status: number,
    code: string,
    options?: { requestId?: string; retryable?: boolean },
  ) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.code = code
    this.requestId = options?.requestId
    this.retryable = options?.retryable ?? (status >= 500 || status === 0 || status === 429)
  }
}

export interface ErrorClassification {
  readonly type: 'auth' | 'permission' | 'validation' | 'server' | 'network'
  readonly title: string
  readonly retryable: boolean
}

export const classifyApiError = (error: unknown): ErrorClassification => {
  if (error instanceof ApiError) {
    return classifyByStatus(error.status)
  }
  if (error instanceof TypeError && error.message === 'Failed to fetch') {
    return { type: 'network', title: '网络连接失败，请检查网络后重试', retryable: true }
  }
  if (error instanceof DOMException && error.name === 'AbortError') {
    return { type: 'network', title: '请求已取消', retryable: false }
  }
  return { type: 'server', title: '未知错误', retryable: false }
}

const classifyByStatus = (status: number): ErrorClassification => {
  switch (true) {
    case status === 401:
      return { type: 'auth', title: '登录已过期，请重新登录', retryable: false }
    case status === 403:
      return { type: 'permission', title: '无权访问', retryable: false }
    case status === 409:
      return { type: 'validation', title: '数据已发生变化，请刷新后重试', retryable: false }
    case status === 422:
      return { type: 'validation', title: '输入未通过业务校验', retryable: false }
    case status === 429:
      return { type: 'server', title: '请求过于频繁，请稍后重试', retryable: true }
    case status >= 500:
      return { type: 'server', title: '服务暂时不可用，请稍后重试', retryable: true }
    default:
      return { type: 'server', title: `请求失败 (${status})`, retryable: false }
  }
}
```

- [ ] **Step 2: Commit**

```bash
git add egon-cola-platform-admin-web-shared/src/api/errors.ts
git commit -m "feat(shared): add structured API error classification"
```

### Task 5: Token store (修复 subscribe 不工作)

**Files:**
- Create: `egon-cola-platform-admin-web-shared/src/auth/tokenStore.ts`
- Create: `egon-cola-platform-admin-web-shared/src/auth/tokenStore.test.ts`

**Interfaces:**
- Produces: `TokenStore` interface, `createTokenStore(): TokenStore`
- Consumes: `decodeTokenPayload` from jwt.ts (for nonce extraction)

- [ ] **Step 1: 编写 tokenStore 测试**

```typescript
// tokenStore.test.ts
import { describe, expect, it, vi } from 'vitest'
import { createTokenStore } from './tokenStore'

describe('createTokenStore', () => {
  it('returns null when empty', () => {
    const store = createTokenStore()
    expect(store.get()).toBeNull()
  })

  it('stores and retrieves tokens', () => {
    const store = createTokenStore()
    store.set({ accessToken: 't', nonce: 'n', expiresAt: new Date().toISOString() })
    expect(store.get()?.accessToken).toBe('t')
  })

  it('clears tokens', () => {
    const store = createTokenStore()
    store.set({ accessToken: 't' })
    store.clear()
    expect(store.get()).toBeNull()
  })

  it('notifies subscribers on set', () => {
    const store = createTokenStore()
    const fn = vi.fn()
    store.subscribe(fn)
    store.set({ accessToken: 'x' })
    expect(fn).toHaveBeenCalledWith({ accessToken: 'x' })
  })

  it('notifies subscribers on clear', () => {
    const store = createTokenStore()
    store.set({ accessToken: 'x' })
    const fn = vi.fn()
    store.subscribe(fn)
    store.clear()
    expect(fn).toHaveBeenCalledWith(null)
  })

  it('unsubscribe stops notifications', () => {
    const store = createTokenStore()
    const fn = vi.fn()
    const unsub = store.subscribe(fn)
    unsub()
    store.set({ accessToken: 'x' })
    expect(fn).not.toHaveBeenCalled()
  })

  it('extracts nonce from access token on set', () => {
    const store = createTokenStore()
    const header = btoa(JSON.stringify({ alg: 'RS256' }))
    const body = btoa(JSON.stringify({ nonce: 'test-nonce', sub: 'u1' }))
    store.set({ accessToken: `${header}.${body}.sig` })
    expect(store.get()?.nonce).toBe('test-nonce')
  })
})
```

- [ ] **Step 2: 运行测试验证失败**

```bash
cd egon-cola-platform-admin-web-shared && npx vitest run src/auth/tokenStore.test.ts
```

- [ ] **Step 3: 实现 tokenStore.ts**

```typescript
import { decodeTokenPayload } from '../api/jwt'

export interface AuthTokens {
  readonly accessToken: string
  readonly nonce?: string
  readonly expiresAt?: string
}

export interface TokenStore {
  get(): AuthTokens | null
  set(tokens: AuthTokens): void
  clear(): void
  subscribe(fn: (tokens: AuthTokens | null) => void): () => void
}

const extractNonce = (accessToken: string): string | undefined => {
  try {
    const claims = decodeTokenPayload(accessToken)
    return typeof claims.nonce === 'string' ? claims.nonce : undefined
  } catch {
    return undefined
  }
}

const extractExpiresAt = (accessToken: string, responseExpiresIn?: number): string | undefined => {
  try {
    const claims = decodeTokenPayload(accessToken)
    const exp = claims.exp
    if (exp !== undefined) {
      const seconds = typeof exp === 'string' ? Number(exp) : (exp as number)
      if (Number.isFinite(seconds)) return new Date(seconds * 1000).toISOString()
    }
  } catch { /* fall through */ }
  if (responseExpiresIn) {
    return new Date(Date.now() + responseExpiresIn * 1000).toISOString()
  }
  return undefined
}

export const createTokenStore = (): TokenStore => {
  let tokens: AuthTokens | null = null
  const listeners = new Set<(tokens: AuthTokens | null) => void>()

  const notify = () => {
    for (const fn of listeners) fn(tokens)
  }

  return {
    get: () => tokens,
    set: (incoming) => {
      tokens = {
        accessToken: incoming.accessToken,
        nonce: incoming.nonce ?? extractNonce(incoming.accessToken),
        expiresAt: incoming.expiresAt ?? extractExpiresAt(incoming.accessToken),
      }
      notify()
    },
    clear: () => {
      tokens = null
      notify()
    },
    subscribe: (fn) => {
      listeners.add(fn)
      return () => { listeners.delete(fn) }
    },
  }
}
```

- [ ] **Step 4: 运行测试验证通过**

```bash
cd egon-cola-platform-admin-web-shared && npx vitest run src/auth/tokenStore.test.ts
```
Expected: 6 tests PASS

- [ ] **Step 5: Commit**

```bash
git add egon-cola-platform-admin-web-shared/src/auth/tokenStore.ts egon-cola-platform-admin-web-shared/src/auth/tokenStore.test.ts
git commit -m "feat(shared): add token store with working subscribe"
```

### Task 6: ThemeProvider

**Files:**
- Create: `egon-cola-platform-admin-web-shared/src/theme/ThemeProvider.tsx`

**Interfaces:**
- Produces: `AdminThemeProvider` component wrapping antd ConfigProvider
- Consumes: `designTokens` from tokens.ts

- [ ] **Step 1: 创建 ThemeProvider.tsx**

```typescript
import { ConfigProvider } from 'antd'
import zhCN from 'antd/locale/zh_CN'
import type { PropsWithChildren } from 'react'
import { designTokens } from './tokens'

export const AdminThemeProvider = ({ children }: PropsWithChildren) => (
  <ConfigProvider
    locale={zhCN}
    theme={{
      token: {
        colorPrimary: designTokens.color.primary,
        colorText: designTokens.color.text,
        colorTextSecondary: designTokens.color.textSecondary,
        colorBgContainer: designTokens.color.backgroundAlt,
        colorBorder: designTokens.color.border,
        colorError: designTokens.color.error,
        colorWarning: designTokens.color.warning,
        colorSuccess: designTokens.color.success,
        borderRadius: designTokens.radius.md,
        fontFamily: designTokens.font.family,
      },
    }}
  >
    {children}
  </ConfigProvider>
)
```

- [ ] **Step 2: Commit**

```bash
git add egon-cola-platform-admin-web-shared/src/theme/ThemeProvider.tsx
git commit -m "feat(shared): add AdminThemeProvider wrapping antd ConfigProvider"
```

---

## Phase 3: 共享包 Auth + API 模块

### Task 7: OAuth client (修复所有已知 Bug)

**Files:**
- Create: `egon-cola-platform-admin-web-shared/src/auth/oauthClient.ts`
- Create: `egon-cola-platform-admin-web-shared/src/auth/oauthClient.test.ts`

**Interfaces:**
- Produces: `OAuthClient` interface, `OAuthClientConfiguration`, `createOAuthClient(config, runtime): OAuthClient`
- Consumes: `TokenStore` from tokenStore.ts, `computeExpiresAt` from jwt.ts

- [ ] **Step 1: 编写 oauthClient 测试（覆盖所有修复的 Bug）**

```typescript
// oauthClient.test.ts
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { createOAuthClient, type OAuthRuntime } from './oauthClient'
import { createTokenStore } from './tokenStore'

const makeToken = (payload: Record<string, unknown>): string => {
  const h = btoa(JSON.stringify({ alg: 'RS256' }))
  const b = btoa(JSON.stringify(payload))
  return `${h}.${b}.sig`
}

describe('createOAuthClient', () => {
  let runtime: OAuthRuntime & { fetch: ReturnType<typeof vi.fn> }
  let storage: Storage

  beforeEach(() => {
    storage = new Map() as unknown as Storage
    runtime = {
      fetch: vi.fn(),
      storage,
      randomValues: (target) => crypto.getRandomValues(target),
      digest: async (value) => crypto.subtle.digest('SHA-256', value),
      navigate: vi.fn(),
      now: () => Date.now(),
    }
  })

  const client = () => createOAuthClient({
    issuer: 'https://idp.example.com',
    clientId: 'test-client',
    audience: 'test-aud',
    redirectUri: 'https://app.example.com/oauth/callback',
    tokenStore: createTokenStore(),
  }, runtime)

  describe('beginAuthorization', () => {
    it('navigates to authorize endpoint with PKCE params', async () => {
      await client().beginAuthorization('tenant-1', '/dashboard')
      expect(runtime.navigate).toHaveBeenCalledWith(
        expect.stringContaining('response_type=code'),
      )
      expect(runtime.navigate).toHaveBeenCalledWith(
        expect.stringContaining('code_challenge_method=S256'),
      )
    })
  })

  describe('handleCallback', () => {
    it('validates state and returns returnTo', async () => {
      const c = client()
      await c.beginAuthorization('t1', '/home')
      const stored = JSON.parse(storage.getItem('egon.admin.oauth.transaction')!)
      runtime.fetch.mockResolvedValueOnce({
        ok: true,
        json: async () => ({ access_token: makeToken({ sub: 'u1', nonce: stored.nonce }), token_type: 'bearer' }),
      })
      const returnTo = await c.handleCallback(`?code=abc&state=${stored.state}`)
      expect(returnTo).toBe('/home')
    })

    it('rejects when state mismatches', async () => {
      const c = client()
      await c.beginAuthorization('t1', '/')
      await expect(c.handleCallback('?code=abc&state=wrong')).rejects.toThrow('state')
    })

    it('preserves error_description from server', async () => {
      const c = client()
      await c.beginAuthorization('t1', '/')
      const stored = JSON.parse(storage.getItem('egon.admin.oauth.transaction')!)
      await expect(c.handleCallback(`?error=access_denied&error_description=User+denied&state=${stored.state}`))
        .rejects.toThrow('User denied')
    })

    it('allows retry after callback failure (transaction not consumed before validation)', async () => {
      const c = client()
      await c.beginAuthorization('t1', '/')
      // First attempt with wrong state fails
      await c.handleCallback('?code=abc&state=wrong').catch(() => {})
      // Transaction still exists
      const stored = JSON.parse(storage.getItem('egon.admin.oauth.transaction')!)
      expect(stored).not.toBeNull()
    })
  })

  describe('refresh', () => {
    it('does NOT validate nonce on refresh (Bug Fix)', async () => {
      const c = client()
      const oldToken = makeToken({ sub: 'u1', nonce: 'old-nonce' })
      const newToken = makeToken({ sub: 'u1', nonce: 'new-nonce' })
      runtime.fetch.mockResolvedValueOnce({
        ok: true,
        json: async () => ({ access_token: oldToken, token_type: 'bearer' }),
      })
      // First set a token
      runtime.fetch.mockResolvedValueOnce({
        ok: true,
        json: async () => ({ access_token: newToken, token_type: 'bearer' }),
      })
      // refresh should not fail due to nonce mismatch
      await expect(c.refresh()).resolves.toBeDefined()
    })
  })
})
```

- [ ] **Step 2: 运行测试验证失败**

```bash
cd egon-cola-platform-admin-web-shared && npx vitest run src/auth/oauthClient.test.ts
```

- [ ] **Step 3: 实现 oauthClient.ts**

（代码基于 IDP 现有 `oauthClient.ts`，修复所有 Bug。由于篇幅长，关键改动标注在注释中）

```typescript
import type { TokenStore } from './tokenStore'

export interface OAuthClientConfiguration {
  readonly issuer: string
  readonly clientId: string
  readonly audience: string
  readonly redirectUri: string
  readonly tokenStore: TokenStore
}

interface OAuthTransaction {
  readonly state: string
  readonly nonce: string
  readonly verifier: string
  readonly returnTo: string
  readonly createdAt: number
}

export interface OAuthRuntime {
  readonly fetch: typeof globalThis.fetch
  readonly storage: Storage
  readonly randomValues: (target: Uint8Array<ArrayBuffer>) => Uint8Array<ArrayBuffer>
  readonly digest: (value: Uint8Array<ArrayBuffer>) => Promise<ArrayBuffer>
  readonly navigate: (url: string) => void
  readonly now: () => number
}

const TRANSACTION_KEY = 'egon.admin.oauth.transaction'

export const createOAuthClient = (
  configuration: OAuthClientConfiguration,
  runtime: OAuthRuntime,
) => {
  const issuer = configuration.issuer.replace(/\/$/, '')
  let refreshInFlight: Promise<string> | undefined
  let callbackInFlight: Promise<string> | undefined

  const storeToken = (response: TokenResponse, expectedNonce?: string): string => {
    if (!response.access_token || response.token_type?.toLowerCase() !== 'bearer') {
      throw new Error('Invalid token response: missing access_token or wrong token_type')
    }
    // FIX: nonce validation only for authorization_code grant
    if (expectedNonce) {
      const claims = decodeTokenPayloadRaw(response.access_token)
      if (claims.nonce !== expectedNonce) {
        throw new Error('Token nonce validation failed')
      }
    }
    configuration.tokenStore.set({
      accessToken: response.access_token,
      nonce: expectedNonce,
    })
    return response.access_token
  }

  const requestToken = async (form: URLSearchParams): Promise<TokenResponse> => {
    const response = await runtime.fetch(`${issuer}/oauth2/token`, {
      method: 'POST',
      credentials: 'include',
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      body: form,
    })
    if (!response.ok) {
      const body = await response.json().catch(() => ({})) as { error_description?: string; error?: string }
      throw new Error(body.error_description ?? body.error ?? 'Token exchange failed')
    }
    return await response.json() as TokenResponse
  }

  return {
    beginAuthorization: async (tenantId: string, returnTo = '/') => {
      const transaction: OAuthTransaction = {
        state: randomToken(runtime),
        nonce: randomToken(runtime),
        verifier: randomToken(runtime),
        returnTo: safeReturnTo(returnTo),
        createdAt: runtime.now(),
      }
      runtime.storage.setItem(TRANSACTION_KEY, JSON.stringify(transaction))
      const challenge = base64Url(new Uint8Array(
        await runtime.digest(new TextEncoder().encode(transaction.verifier)),
      ))
      const params = new URLSearchParams({
        response_type: 'code',
        client_id: configuration.clientId,
        redirect_uri: configuration.redirectUri,
        audience: configuration.audience,
        tenant_id: tenantId.trim() || (() => { throw new Error('tenantId is required') })(),
        state: transaction.state,
        nonce: transaction.nonce,
        code_challenge: challenge,
        code_challenge_method: 'S256',
      })
      runtime.navigate(`${issuer}/oauth2/authorize?${params.toString()}`)
    },

    handleCallback: (search: string): Promise<string> => {
      if (!callbackInFlight) {
        callbackInFlight = (async () => {
          try {
            const encoded = runtime.storage.getItem(TRANSACTION_KEY)
            if (!encoded) throw new Error('OAuth transaction not found or expired')
            const transaction = JSON.parse(encoded) as OAuthTransaction
            const age = runtime.now() - transaction.createdAt
            if (!Number.isFinite(age) || age < 0 || age > 10 * 60 * 1000) {
              throw new Error('OAuth transaction expired')
            }
            const params = new URLSearchParams(search)
            const errorDesc = params.get('error_description')
            if (params.get('error')) {
              throw new Error(errorDesc ?? 'Authorization was denied')
            }
            // FIX: validate state BEFORE removing transaction
            if (params.get('state') !== transaction.state) {
              throw new Error('OAuth state validation failed')
            }
            // Only remove transaction after successful validation
            runtime.storage.removeItem(TRANSACTION_KEY)
            const response = await requestToken(new URLSearchParams({
              grant_type: 'authorization_code',
              client_id: configuration.clientId,
              code: params.get('code') ?? (() => { throw new Error('code is required') })(),
              code_verifier: transaction.verifier,
              redirect_uri: configuration.redirectUri,
            }))
            storeToken(response, transaction.nonce)
            return safeReturnTo(transaction.returnTo)
          } finally {
            // FIX: clear inFlight on failure to allow retry
            callbackInFlight = undefined
          }
        })()
      }
      return callbackInFlight
    },

    refresh: (): Promise<string> => {
      if (!refreshInFlight) {
        refreshInFlight = requestToken(new URLSearchParams({
          grant_type: 'refresh_token',
          client_id: configuration.clientId,
        }))
          .then((response) => storeToken(response))  // FIX: no nonce validation on refresh
          .finally(() => { refreshInFlight = undefined })
      }
      return refreshInFlight
    },

    revoke: async (): Promise<void> => {
      try {
        await runtime.fetch(`${issuer}/oauth2/revoke`, {
          method: 'POST',
          credentials: 'include',
          headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
          body: new URLSearchParams({ client_id: configuration.clientId }),
        })
      } finally {
        configuration.tokenStore.clear()
      }
    },
  }
}

// --- Internal helpers (from IDP original, with TextDecoder fix) ---

const textDecoder = new TextDecoder('utf-8')
const textEncoder = new TextEncoder()

const decodeTokenPayloadRaw = (token: string): Record<string, unknown> => {
  const parts = token.split('.')
  if (parts.length !== 3) throw new Error('Invalid JWT format')
  const normalized = parts[1]!.replace(/-/g, '+').replace(/_/g, '/')
  const binary = Uint8Array.from(atob(normalized), (c) => c.charCodeAt(0))
  return JSON.parse(textDecoder.decode(binary)) as Record<string, unknown>
}

const randomToken = (runtime: OAuthRuntime): string => {
  const bytes = new Uint8Array(32)
  runtime.randomValues(bytes)
  return base64Url(bytes)
}

const base64Url = (bytes: Uint8Array): string => {
  let value = ''
  bytes.forEach((byte) => { value += String.fromCharCode(byte) })
  return btoa(value).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '')
}

const safeReturnTo = (value: string): string =>
  value.startsWith('/') && !value.startsWith('//') ? value : '/'

interface TokenResponse {
  readonly access_token?: string
  readonly token_type?: string
  readonly expires_in?: number
}
```

- [ ] **Step 4: 运行测试验证通过**

```bash
cd egon-cola-platform-admin-web-shared && npx vitest run src/auth/oauthClient.test.ts
```

- [ ] **Step 5: Commit**

```bash
git add egon-cola-platform-admin-web-shared/src/auth/oauthClient.ts egon-cola-platform-admin-web-shared/src/auth/oauthClient.test.ts
git commit -m "fix(shared): OAuth client with all bugs fixed (UTF-8, nonce, callback retry, error_description)"
```

### Task 8: HTTP client

**Files:**
- Create: `egon-cola-platform-admin-web-shared/src/api/httpClient.ts`
- Create: `egon-cola-platform-admin-web-shared/src/api/httpClient.test.ts`

**Interfaces:**
- Produces: `HttpClient` interface, `HttpClientConfig`, `createHttpClient(config): HttpClient`
- Consumes: `ApiError` from errors.ts, `isTokenExpired` from jwt.ts

- [ ] **Step 1: 编写 httpClient 测试**

```typescript
// httpClient.test.ts
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { createHttpClient } from './httpClient'

describe('createHttpClient', () => {
  let fetchMock: ReturnType<typeof vi.fn>

  beforeEach(() => {
    fetchMock = vi.fn()
    globalThis.fetch = fetchMock
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  const create = (overrides?: Partial<Parameters<typeof createHttpClient>[0]>) =>
    createHttpClient({
      baseUrl: 'https://api.example.com',
      credentials: 'include',
      onAuthError: async () => 'new-token',
      onFatalAuthError: () => {},
      ...overrides,
    })

  it('sends GET with credentials and JSON accept header', async () => {
    fetchMock.mockResolvedValueOnce({ ok: true, status: 200, json: async () => ({ data: 'ok' }) })
    const result = await create().request<{ data: string }>('/test')
    expect(result.data).toBe('ok')
    expect(fetchMock).toHaveBeenCalledWith('https://api.example.com/test', expect.objectContaining({
      credentials: 'include',
      headers: expect.any(Headers),
    }))
  })

  it('retries once on 401 with refreshed token', async () => {
    fetchMock
      .mockResolvedValueOnce({ ok: false, status: 401, json: async () => ({}) })
      .mockResolvedValueOnce({ ok: true, status: 200, json: async () => ({ retried: true }) })
    const onAuthError = vi.fn().mockResolvedValue('refreshed-token')
    const result = await create({ onAuthError }).request<{ retried: boolean }>('/secure')
    expect(result.retried).toBe(true)
    expect(onAuthError).toHaveBeenCalledOnce()
  })

  it('calls onFatalAuthError after failed retry', async () => {
    fetchMock.mockResolvedValue({ ok: false, status: 401, json: async () => ({}) })
    const onFatal = vi.fn()
    await create({ onFatalAuthError: onFatal }).request('/doomed').catch(() => {})
    expect(onFatal).toHaveBeenCalledOnce()
  })

  it('throws ApiError on non-ok response', async () => {
    fetchMock.mockResolvedValueOnce({
      ok: false,
      status: 422,
      json: async () => ({ code: 'VALIDATION_ERROR', message: 'Bad input' }),
    })
    await expect(create().request('/bad')).rejects.toMatchObject({ status: 422 })
  })

  it('sets Content-Type only for JSON-like bodies', async () => {
    fetchMock.mockResolvedValueOnce({ ok: true, status: 200, json: async () => ({}) })
    await create().request('/post', { method: 'POST', body: JSON.stringify({ x: 1 }) })
    const headers = fetchMock.mock.calls[0]![1]!.headers as Headers
    expect(headers.get('Content-Type')).toBe('application/json')
  })
})
```

- [ ] **Step 2: 运行测试验证失败**

```bash
cd egon-cola-platform-admin-web-shared && npx vitest run src/api/httpClient.test.ts
```

- [ ] **Step 3: 实现 httpClient.ts**

```typescript
import { ApiError } from './errors'

export interface HttpClientConfig {
  readonly baseUrl: string
  readonly credentials: RequestCredentials
  readonly onAuthError: () => Promise<string>
  readonly onFatalAuthError: () => void
  readonly getAccessToken?: () => string | null
  readonly timeout?: number
}

export interface HttpClient {
  request<T>(path: string, init?: RequestInit & { signal?: AbortSignal }): Promise<T>
}

export const createHttpClient = (config: HttpClientConfig): HttpClient => {
  const { baseUrl, credentials, onAuthError, onFatalAuthError, getAccessToken, timeout = 30_000 } = config

  const buildHeaders = (init?: RequestInit): Headers => {
    const headers = new Headers(init?.headers)
    headers.set('Accept', 'application/json')
    const token = getAccessToken?.()
    if (token) headers.set('Authorization', `Bearer ${token}`)
    if (shouldSetJsonContentType(init?.body) && !headers.has('Content-Type')) {
      headers.set('Content-Type', 'application/json')
    }
    return headers
  }

  const doFetch = (path: string, init?: RequestInit & { signal?: AbortSignal }): Promise<Response> => {
    const controller = new AbortController()
    const timer = setTimeout(() => controller.abort(), timeout)
    const signal = init?.signal ?? controller.signal
    return fetch(`${baseUrl}${path}`, { ...init, credentials, headers: buildHeaders(init), signal })
      .finally(() => clearTimeout(timer))
  }

  const handleResponse = async <T>(response: Response): Promise<T> => {
    if (response.status === 204) return undefined as T
    if (!response.ok) {
      const body = await response.json().catch(() => ({})) as { code?: string; message?: string }
      throw new ApiError(
        body.message ?? `Request failed (${response.status})`,
        response.status,
        body.code ?? 'REQUEST_FAILED',
      )
    }
    return await response.json() as T
  }

  const request = async <T>(path: string, init?: RequestInit & { signal?: AbortSignal }): Promise<T> => {
    const response = await doFetch(path, init)
    if (response.status === 401) {
      try {
        const newToken = await onAuthError()
        const retryHeaders = new Headers(init?.headers)
        retryHeaders.set('Authorization', `Bearer ${newToken}`)
        const retryResponse = await doFetch(path, { ...init, headers: retryHeaders })
        if (retryResponse.status === 401) {
          onFatalAuthError()
          throw new ApiError('Authentication failed after token refresh', 401, 'AUTH_FAILED')
        }
        return handleResponse<T>(retryResponse)
      } catch (error) {
        if (error instanceof ApiError) throw error
        onFatalAuthError()
        throw error
      }
    }
    return handleResponse<T>(response)
  }

  return { request }
}

const shouldSetJsonContentType = (body: unknown): boolean => {
  if (body === null || body === undefined) return false
  if (typeof body === 'string') {
    try { JSON.parse(body); return true } catch { return false }
  }
  if (body instanceof FormData || body instanceof URLSearchParams) return false
  return typeof body === 'object'
}
```

- [ ] **Step 4: 运行测试验证通过**

```bash
cd egon-cola-platform-admin-web-shared && npx vitest run src/api/httpClient.test.ts
```

- [ ] **Step 5: Commit**

```bash
git add egon-cola-platform-admin-web-shared/src/api/httpClient.ts egon-cola-platform-admin-web-shared/src/api/httpClient.test.ts
git commit -m "feat(shared): add HTTP client with credentials, retry, timeout, and error handling"
```

---

## Phase 4: 共享包 i18n + 组件 + Hooks

### Task 9: i18n 框架

**Files:**
- Create: `egon-cola-platform-admin-web-shared/src/i18n/zh-CN.ts`
- Create: `egon-cola-platform-admin-web-shared/src/i18n/en-US.ts`
- Create: `egon-cola-platform-admin-web-shared/src/i18n/index.ts`

**Interfaces:**
- Produces: `initI18n(options)`, `useT()`, `I18nProvider`, `changeLanguage(lang)`, `currentLanguage`

- [ ] **Step 1: 创建 zh-CN.ts**

```typescript
export const zhCN = {
  common: {
    loading: '加载中...',
    error: '加载失败',
    empty: '暂无数据',
    retry: '重试',
    save: '保存',
    cancel: '取消',
    confirm: '确认',
    delete: '删除',
    create: '创建',
    search: '查询',
    reset: '重置',
    back: '返回',
    refresh: '刷新',
    logout: '退出登录',
    operation: '操作',
    status: '状态',
    close: '关闭',
    'error.network': '网络连接失败，请检查网络后重试',
    'error.auth': '登录已过期，请重新登录',
    'error.permission': '无权访问',
    'error.server': '服务暂时不可用，请稍后重试',
    'error.unknown': '未知错误',
    'page.crash': '页面出现错误',
    'page.crash.reload': '刷新页面',
  },
}
```

- [ ] **Step 2: 创建 en-US.ts**

```typescript
import type { zhCN } from './zh-CN'

export const enUS: typeof zhCN = {
  common: {
    loading: 'Loading...',
    error: 'Load Failed',
    empty: 'No Data',
    retry: 'Retry',
    save: 'Save',
    cancel: 'Cancel',
    confirm: 'Confirm',
    delete: 'Delete',
    create: 'Create',
    search: 'Search',
    reset: 'Reset',
    back: 'Back',
    refresh: 'Refresh',
    logout: 'Logout',
    operation: 'Actions',
    status: 'Status',
    close: 'Close',
    'error.network': 'Network error, please check your connection and try again',
    'error.auth': 'Session expired, please login again',
    'error.permission': 'Access Denied',
    'error.server': 'Service temporarily unavailable, please try again later',
    'error.unknown': 'Unknown Error',
    'page.crash': 'Something went wrong',
    'page.crash.reload': 'Reload Page',
  },
}
```

- [ ] **Step 3: 创建 i18n/index.ts**

```typescript
import i18next from 'i18next'
import { initReactI18next, useTranslation } from 'react-i18next'
import { I18nextProvider } from 'react-i18next'
import type { PropsWithChildren } from 'react'
import { zhCN } from './zh-CN'

let initialized = false

export interface I18nInitOptions {
  readonly defaultNS?: string
  readonly resources: Record<string, Record<string, Record<string, string>>>
  readonly lng?: string
}

export const initI18n = (options: I18nInitOptions): void => {
  if (initialized) return
  void i18next
    .use(initReactI18next)
    .init({
      lng: options.lng ?? (typeof navigator !== 'undefined' ? navigator.language : 'zh-CN'),
      fallbackLng: 'zh-CN',
      defaultNS: options.defaultNS ?? 'common',
      resources: {
        'zh-CN': { common: zhCN.common, ...options.resources['zh-CN'] },
        ...options.resources,
      },
      interpolation: { escapeValue: false },
      returnNull: false,
      returnEmptyString: false,
    })
  initialized = true
}

export const I18nProvider = ({ children }: PropsWithChildren) => (
  <I18nextProvider i18n={i18next}>{children}</I18nextProvider>
)

export const useT = (ns?: string) => useTranslation(ns).t

export const changeLanguage = async (lng: string): Promise<void> => {
  await i18next.changeLanguage(lng)
}

export const currentLanguage = (): string => i18next.language
```

- [ ] **Step 4: Commit**

```bash
git add egon-cola-platform-admin-web-shared/src/i18n/
git commit -m "feat(shared): add i18n framework with zh-CN and en-US"
```

### Task 10: PageState 增强组件

**Files:**
- Create: `egon-cola-platform-admin-web-shared/src/components/PageState.tsx`

**Interfaces:**
- Produces: `PageState` component with `PageStateProps`
- Consumes: `classifyApiError` from errors.ts, `useT` from i18n

- [ ] **Step 1: 创建 PageState.tsx**

```typescript
import { Alert, Button, Empty, Skeleton, Space } from 'antd'
import type { ReactNode } from 'react'
import { classifyApiError } from '../api/errors'
import { useT } from '../i18n'

export interface PageStateProps {
  readonly loading: boolean
  readonly error: unknown
  readonly empty: boolean
  readonly emptyDescription?: string
  readonly skeleton?: ReactNode
  readonly showPartial?: boolean
  readonly onRetry?: () => void
  readonly children: ReactNode
}

export const PageState = ({
  loading,
  error,
  empty,
  emptyDescription,
  skeleton,
  showPartial = false,
  onRetry,
  children,
}: PageStateProps) => {
  const t = useT()

  if (loading) {
    return skeleton ?? <Skeleton active paragraph={{ rows: 5 }} />
  }

  if (error !== null && error !== undefined) {
    const classified = classifyApiError(error)
    const banner = (
      <Alert
        type={classified.type === 'permission' ? 'warning' : 'error'}
        showIcon
        message={classified.title}
        action={onRetry ? <Button size="small" onClick={onRetry}>{t('common.retry')}</Button> : undefined}
        style={{ marginBottom: showPartial ? 16 : 0 }}
      />
    )
    if (showPartial) {
      return <Space direction="vertical" style={{ width: '100%' }}>{banner}{children}</Space>
    }
    return banner
  }

  if (empty) {
    return <Empty description={emptyDescription ?? t('common.empty')} />
  }

  return children
}
```

- [ ] **Step 2: Commit**

```bash
git add egon-cola-platform-admin-web-shared/src/components/PageState.tsx
git commit -m "feat(shared): add enhanced PageState with retry, partial data, skeleton support"
```

### Task 11: AppErrorBoundary 增强组件

**Files:**
- Create: `egon-cola-platform-admin-web-shared/src/components/AppErrorBoundary.tsx`

**Interfaces:**
- Produces: `AppErrorBoundary` class component with telemetry hook

- [ ] **Step 1: 创建 AppErrorBoundary.tsx**

```typescript
import { Button, Result } from 'antd'
import { Component, type ErrorInfo, type ReactNode } from 'react'
import { useT } from '../i18n'

export interface AppErrorBoundaryProps {
  readonly onError?: (error: Error, info: ErrorInfo) => void
  readonly fallback?: ReactNode
  readonly children: ReactNode
}

interface State {
  readonly error: Error | null
}

export class AppErrorBoundary extends Component<AppErrorBoundaryProps, State> {
  constructor(props: AppErrorBoundaryProps) {
    super(props)
    this.state = { error: null }
  }

  static getDerivedStateFromError(error: Error): State {
    return { error }
  }

  override componentDidCatch(error: Error, info: ErrorInfo): void {
    console.error('[AppErrorBoundary]', error, info.componentStack)
    this.props.onError?.(error, info)
  }

  override render(): ReactNode {
    if (this.state.error) {
      if (this.props.fallback) return this.props.fallback
      return <CrashFallback error={this.state.error} />
    }
    return this.props.children
  }
}

const CrashFallback = ({ error }: { readonly error: Error }) => {
  // Using a separate component allows useT() outside the class
  const t = useTInternal()
  return (
    <Result
      status="error"
      title={t('common.page.crash')}
      subTitle={error.message}
      extra={(
        <Button type="primary" onClick={() => window.location.reload()}>
          {t('common.page.crash.reload')}
        </Button>
      )}
    />
  )
}

let cachedT: ReturnType<typeof useT> | null = null
const useTInternal = () => {
  try {
    const t = (() => { try { return useT() } catch { return null } })()
    if (t) { cachedT = t; return t }
  } catch { /* useT throws outside I18nProvider */ }
  return cachedT ?? ((key: string) => key)
}
```

- [ ] **Step 2: Commit**

```bash
git add egon-cola-platform-admin-web-shared/src/components/AppErrorBoundary.tsx
git commit -m "feat(shared): add AppErrorBoundary with telemetry callback"
```

### Task 12: PageTemplate 组件

**Files:**
- Create: `egon-cola-platform-admin-web-shared/src/components/PageTemplate.tsx`

**Interfaces:**
- Produces: `PageTemplate` component
- Consumes: `PageState`, `PageStateProps` from PageState.tsx

- [ ] **Step 1: 创建 PageTemplate.tsx**

```typescript
import { Breadcrumb, Card, Typography } from 'antd'
import type { ReactNode } from 'react'
import { PageState, type PageStateProps } from './PageState'

export interface BreadcrumbItem {
  readonly title: string
  readonly path?: string
}

export interface PageTemplateProps {
  readonly title: string
  readonly subtitle?: string
  readonly breadcrumbs?: readonly BreadcrumbItem[]
  readonly extra?: ReactNode
  readonly pageState: Omit<PageStateProps, 'children'>
  readonly children: ReactNode
}

export const PageTemplate = ({
  title,
  subtitle,
  breadcrumbs,
  extra,
  pageState,
  children,
}: PageTemplateProps) => (
  <div>
    {breadcrumbs && breadcrumbs.length > 0 && (
      <Breadcrumb
        style={{ marginBottom: 16 }}
        items={breadcrumbs.map((item) => ({
          title: item.path ? <a href={item.path}>{item.title}</a> : item.title,
        }))}
      />
    )}
    <Card
      title={(
        <div>
          <Typography.Title level={4} style={{ margin: 0 }}>{title}</Typography.Title>
          {subtitle && <Typography.Text type="secondary">{subtitle}</Typography.Text>}
        </div>
      )}
      extra={extra}
    >
      <PageState {...pageState}>{children}</PageState>
    </Card>
  </div>
)
```

- [ ] **Step 2: Commit**

```bash
git add egon-cola-platform-admin-web-shared/src/components/PageTemplate.tsx
git commit -m "feat(shared): add PageTemplate with breadcrumb and Card shell"
```

### Task 13: usePermission hook

**Files:**
- Create: `egon-cola-platform-admin-web-shared/src/hooks/usePermission.ts`

**Interfaces:**
- Produces: `usePermission(): { has, hasAll, hasAny }`

- [ ] **Step 1: 创建 usePermission.ts**

```typescript
import { useMemo } from 'react'

export interface PermissionContext {
  readonly permissions: readonly string[]
}

export const usePermission = (permissions: readonly string[]) => {
  const set = useMemo(() => new Set(permissions), [permissions])
  return useMemo(() => ({
    has: (permission: string) => set.has(permission),
    hasAll: (...required: string[]) => required.every((p) => set.has(p)),
    hasAny: (...required: string[]) => required.some((p) => set.has(p)),
  }), [set])
}
```

- [ ] **Step 2: Commit**

```bash
git add egon-cola-platform-admin-web-shared/src/hooks/usePermission.ts
git commit -m "feat(shared): add usePermission hook"
```

### Task 14: useFeatureQuery hook (RBAC3 boilerplate eliminator)

**Files:**
- Create: `egon-cola-platform-admin-web-shared/src/hooks/useFeatureQuery.ts`

**Interfaces:**
- Produces: `useFeatureQuery(keys, queryFn, options?): UseQueryResult<T>`
- Note: 此 hook 依赖 `useRbac3Session` 和 `useFeatureApi`/`useFeatureTenantContext` 的概念——但具体实现在消费项目中。共享包导出的是一个需要注入依赖的工厂函数。

- [ ] **Step 1: 创建 useFeatureQuery.ts**

```typescript
import { useQuery, type UseQueryResult } from '@tanstack/react-query'

export interface FeatureQueryDeps {
  readonly status: string
  readonly effectiveTenantId: string | null
  readonly featureApi: { request<T>(path: string, req?: { method?: string; query?: Record<string, unknown>; body?: unknown }): Promise<T> }
}

export const useFeatureQuery = <T>(
  keys: readonly unknown[],
  queryFn: (api: FeatureQueryDeps['featureApi']) => Promise<T>,
  deps: FeatureQueryDeps,
  options?: { enabled?: boolean },
): UseQueryResult<T> => {
  const tenantId = deps.effectiveTenantId ?? 'none'
  return useQuery({
    queryKey: ['rbac3', ...keys, tenantId],
    queryFn: () => queryFn(deps.featureApi),
    enabled: deps.status === 'READY' && (options?.enabled ?? true),
    retry: false,
    refetchOnWindowFocus: false,
  })
}
```

- [ ] **Step 2: Commit**

```bash
git add egon-cola-platform-admin-web-shared/src/hooks/useFeatureQuery.ts
git commit -m "feat(shared): add useFeatureQuery hook to eliminate RBAC3 boilerplate"
```

### Task 15: Barrel export

**Files:**
- Create: `egon-cola-platform-admin-web-shared/src/index.ts`

**Interfaces:**
- Produces: 所有公开 API 的统一导出

- [ ] **Step 1: 创建 index.ts**

```typescript
// Theme
export { designTokens, injectTokens, type DesignTokens } from './theme/tokens'
export { AdminThemeProvider } from './theme/ThemeProvider'

// API
export { createHttpClient, type HttpClient, type HttpClientConfig } from './api/httpClient'
export { ApiError, classifyApiError, type ErrorClassification } from './api/errors'
export { decodeTokenPayload, computeExpiresAt, isTokenExpired } from './api/jwt'

// Auth
export { createTokenStore, type TokenStore, type AuthTokens } from './auth/tokenStore'
export { createOAuthClient, type OAuthClient, type OAuthClientConfiguration, type OAuthRuntime } from './auth/oauthClient'

// i18n
export { initI18n, I18nProvider, useT, changeLanguage, currentLanguage, type I18nInitOptions } from './i18n'

// Components
export { PageState, type PageStateProps } from './components/PageState'
export { AppErrorBoundary, type AppErrorBoundaryProps } from './components/AppErrorBoundary'
export { PageTemplate, type PageTemplateProps, type BreadcrumbItem } from './components/PageTemplate'

// Hooks
export { usePermission, type PermissionContext } from './hooks/usePermission'
export { useFeatureQuery, type FeatureQueryDeps } from './hooks/useFeatureQuery'
```

- [ ] **Step 2: 构建验证**

```bash
cd egon-cola-platform-admin-web-shared && npm run build
```

- [ ] **Step 3: Commit**

```bash
git add egon-cola-platform-admin-web-shared/src/index.ts
git commit -m "feat(shared): add barrel export and verify build"
```

---

## Phase 5: IDP 重构

### Task 16: IDP 安装共享包并升级 package.json

**Files:**
- Modify: `egon-cola-platform-idp-admin-web/package.json`

- [ ] **Step 1: 添加共享包依赖**

```bash
cd egon-cola-platform-idp-admin-web
```

修改 package.json，在 dependencies 中添加：
```jsonc
"@egon-cola/admin-web-shared": "file:../egon-cola-platform-admin-web-shared",
"react-router-dom": "^7.18.0",
"i18next": "^24.0.0",
"react-i18next": "^15.0.0"
```

- [ ] **Step 2: 修改 test script 为 `vitest run`**

```jsonc
"test": "vitest run"
```

- [ ] **Step 3: 安装依赖**

```bash
cd egon-cola-platform-idp-admin-web && npm install
```

- [ ] **Step 4: Commit**

```bash
git add egon-cola-platform-idp-admin-web/package.json egon-cola-platform-idp-admin-web/package-lock.json
git commit -m "chore(idp): add shared package, react-router, i18n dependencies"
```

### Task 17: IDP main.tsx — 接入共享 Theme/i18n

**Files:**
- Modify: `egon-cola-platform-idp-admin-web/src/main.tsx`

- [ ] **Step 1: 重写 main.tsx**

```typescript
import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import 'antd/dist/reset.css'
import { AdminThemeProvider, injectTokens, initI18n } from '@egon-cola/admin-web-shared'
import { App } from './app/App'

injectTokens()

initI18n({
  defaultNS: 'common',
  resources: { 'zh-CN': {} },
})

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <AdminThemeProvider>
      <App />
    </AdminThemeProvider>
  </StrictMode>,
)
```

- [ ] **Step 2: 删除 styles/index.css 文件**

- [ ] **Step 3: Commit**

```bash
git rm egon-cola-platform-idp-admin-web/src/styles/index.css
git add egon-cola-platform-idp-admin-web/src/main.tsx
git commit -m "refactor(idp): use shared ThemeProvider and i18n, remove global CSS"
```

### Task 18: IDP AuthContext — 使用共享 OAuth client

**Files:**
- Modify: `egon-cola-platform-idp-admin-web/src/auth/AuthContext.tsx`
- Delete: `egon-cola-platform-idp-admin-web/src/auth/oauthClient.ts`
- Delete: `egon-cola-platform-idp-admin-web/src/auth/tokenStore.ts`

- [ ] **Step 1: 重写 AuthContext.tsx**（使用共享 oauthClient + tokenStore.subscribe）

```typescript
import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
  type PropsWithChildren,
} from 'react'
import {
  createOAuthClient,
  createTokenStore,
  createHttpClient,
  type OAuthClient,
  type TokenStore,
  type AuthTokens,
} from '@egon-cola/admin-web-shared'
import type { AuthorizationBootstrap } from '../api/types'

// --- Browser runtime (from IDP original) ---
const tokenStore = createTokenStore()

const oauthClient: OAuthClient = createOAuthClient({
  issuer: (import.meta.env.VITE_IDP_ISSUER ?? (() => { throw new Error('VITE_IDP_ISSUER is required') })()),
  clientId: import.meta.env.VITE_IDP_CLIENT_ID ?? (() => { throw new Error('VITE_IDP_CLIENT_ID is required') })(),
  audience: import.meta.env.VITE_IDP_AUDIENCE ?? (() => { throw new Error('VITE_IDP_AUDIENCE is required') })(),
  redirectUri: import.meta.env.VITE_IDP_REDIRECT_URI ?? `${window.location.origin}/oauth/callback`,
  tokenStore,
}, {
  fetch: globalThis.fetch.bind(globalThis),
  storage: window.sessionStorage,
  randomValues: (target) => crypto.getRandomValues(target),
  digest: (value) => crypto.subtle.digest('SHA-256', value),
  navigate: (url) => window.location.assign(url),
  now: () => Date.now(),
})

// --- API client ---
const httpClient = createHttpClient({
  baseUrl: import.meta.env.VITE_IDP_API_BASE_URL ?? '',
  credentials: 'include',
  onAuthError: () => oauthClient.refresh(),
  onFatalAuthError: () => {
    tokenStore.clear()
    window.location.assign('/login')
  },
  getAccessToken: () => tokenStore.get()?.accessToken ?? null,
})

// --- Auth Context ---
interface AuthContextValue {
  readonly loading: boolean
  readonly bootstrap?: AuthorizationBootstrap
  readonly login: (tenantId: string, returnTo?: string) => Promise<void>
  readonly logout: () => Promise<void>
  readonly httpClient: typeof httpClient
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined)

export const AuthProvider = ({ children }: PropsWithChildren) => {
  const [loading, setLoading] = useState(true)
  const [bootstrap, setBootstrap] = useState<AuthorizationBootstrap>()
  const [tokens, setTokens] = useState<AuthTokens | null>(tokenStore.get())

  const login = useCallback(async (tenantId: string, returnTo = '/') => {
    await oauthClient.beginAuthorization(tenantId, returnTo)
  }, [])

  const logout = useCallback(async () => {
    await oauthClient.revoke()
    setBootstrap(undefined)
  }, [])

  // Subscribe to token changes
  useEffect(() => tokenStore.subscribe(setTokens), [])

  // Bootstrap on token change
  useEffect(() => {
    let active = true
    const initialize = async () => {
      if (!tokens) {
        if (active) { setBootstrap(undefined); setLoading(false) }
        return
      }
      try {
        const value = await httpClient.request<AuthorizationBootstrap>('/api/v1/auth/bootstrap')
        if (active) setBootstrap(value)
      } catch {
        if (active) { setBootstrap(undefined); tokenStore.clear() }
      } finally {
        if (active) setLoading(false)
      }
    }
    void initialize()
    return () => { active = false }
  }, [tokens])

  const value = useMemo(
    () => ({ loading, bootstrap, login, logout, httpClient }),
    [loading, bootstrap, login, logout],
  )
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export const useAuth = (): AuthContextValue => {
  const value = useContext(AuthContext)
  if (!value) throw new Error('AuthProvider is required')
  return value
}
```

- [ ] **Step 2: 删除旧文件**

```bash
git rm egon-cola-platform-idp-admin-web/src/auth/oauthClient.ts
git rm egon-cola-platform-idp-admin-web/src/auth/tokenStore.ts
```

- [ ] **Step 3: Commit**

```bash
git add egon-cola-platform-idp-admin-web/src/auth/AuthContext.tsx
git commit -m "refactor(idp): replace custom oauth/token with shared package implementations"
```

### Task 19: IDP App.tsx + router.tsx — react-router 替换手写路由

**Files:**
- Modify: `egon-cola-platform-idp-admin-web/src/app/App.tsx`
- Create: `egon-cola-platform-idp-admin-web/src/app/router.tsx`

- [ ] **Step 1: 创建 router.tsx**

```typescript
import { Spin } from 'antd'
import { lazy, Suspense } from 'react'
import { Navigate, Route, Routes } from 'react-router-dom'
import { AuthProvider, useAuth } from '../auth/AuthContext'
import { AdminLayout } from './AdminLayout'
import { AppErrorBoundary } from '@egon-cola/admin-web-shared'

const CentralLoginPage = lazy(() => import('../auth/CentralLoginPage'))
const CallbackPage = lazy(() => import('../auth/CallbackPage'))
const OverviewPage = lazy(() => import('../features/overview/OverviewPage'))
const UserListPage = lazy(() => import('../features/users/UserListPage'))
const ClientListPage = lazy(() => import('../features/clients/ClientListPage'))
const SigningKeyPage = lazy(() => import('../features/keys/SigningKeyPage'))
const AuditLogPage = lazy(() => import('../features/audits/AuditLogPage'))

const PageFallback = () => <Spin style={{ display: 'grid', placeItems: 'center', minHeight: 200 }} />

const ConsoleGuard = () => {
  const auth = useAuth()
  if (auth.loading) return <Spin fullscreen description="校验统一登录态" />
  if (!auth.bootstrap) return <Navigate to="/login" replace />
  return <AdminLayout />
}

export const AppRouter = () => (
  <AppErrorBoundary>
    <AuthProvider>
      <Suspense fallback={<PageFallback />}>
        <Routes>
          <Route path="/login" element={<CentralLoginPage />} />
          <Route path="/oauth/callback" element={<CallbackPage />} />
          <Route element={<ConsoleGuard />}>
            <Route index element={<Navigate to="/overview" replace />} />
            <Route path="/overview" element={<OverviewPage />} />
            <Route path="/users" element={<UserListPage />} />
            <Route path="/clients" element={<ClientListPage />} />
            <Route path="/keys" element={<SigningKeyPage />} />
            <Route path="/audits" element={<AuditLogPage />} />
          </Route>
          <Route path="*" element={<Navigate to="/overview" replace />} />
        </Routes>
      </Suspense>
    </AuthProvider>
  </AppErrorBoundary>
)
```

- [ ] **Step 2: 重写 App.tsx**（改为仅包装 BrowserRouter）

```typescript
import { BrowserRouter } from 'react-router-dom'
import { AppRouter } from './router'

export const App = () => (
  <BrowserRouter>
    <AppRouter />
  </BrowserRouter>
)
```

- [ ] **Step 3: 创建 AdminLayout.tsx**

```typescript
// src/app/AdminLayout.tsx
import { Button, Layout, Menu, Space, Tag, Typography } from 'antd'
import { Outlet, useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'
import { usePermission } from '@egon-cola/admin-web-shared'

type Section = 'overview' | 'users' | 'clients' | 'keys' | 'audits'

export const AdminLayout = () => {
  const auth = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const { has } = usePermission(auth.bootstrap?.permissions ?? [])

  const items = [
    { key: 'overview', label: '身份概览', path: '/overview' },
    has('idp:identity-user:read') ? { key: 'users', label: '全局用户', path: '/users' } : null,
    has('idp:oauth-client:read') ? { key: 'clients', label: 'OAuth 客户端', path: '/clients' } : null,
    has('idp:signing-key:read') ? { key: 'keys', label: '签名密钥', path: '/keys' } : null,
    has('idp:audit:read') ? { key: 'audits', label: '安全审计', path: '/audits' } : null,
  ].filter(Boolean) as { key: string; label: string; path: string }[]

  const currentPath = location.pathname
  const selectedKey = items.find((item) => currentPath === item.path)?.key ?? 'overview'

  return (
    <Layout style={{ minHeight: '100vh' }}>
      <Layout.Sider width={240} theme="light">
        <Typography.Title level={4} style={{ padding: '20px 22px 8px' }}>统一身份平台</Typography.Title>
        <Menu
          mode="inline"
          selectedKeys={[selectedKey]}
          items={items.map((item) => ({ key: item.key, label: item.label }))}
          onClick={({ key }) => {
            const target = items.find((i) => i.key === key)
            if (target) navigate(target.path)
          }}
        />
      </Layout.Sider>
      <Layout>
        <Layout.Header style={{ background: '#fff', display: 'flex', justifyContent: 'flex-end', alignItems: 'center', borderBottom: '1px solid var(--egon-color-border)' }}>
          <Space>
            <Typography.Text>{auth.bootstrap?.identitySub}</Typography.Text>
            <Tag>{auth.bootstrap?.tenantId}</Tag>
            <Button onClick={() => { void auth.logout() }}>退出当前系统</Button>
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

- [ ] **Step 4: Commit**

```bash
git add egon-cola-platform-idp-admin-web/src/app/
git commit -m "refactor(idp): replace hand-rolled routing with react-router v7 + lazy loading"
```

### Task 20: IDP CallbackPage — 修复卡死 spinner

**Files:**
- Create: `egon-cola-platform-idp-admin-web/src/auth/CallbackPage.tsx`

- [ ] **Step 1: 创建 CallbackPage.tsx**

```typescript
import { Alert, Button, Result, Spin } from 'antd'
import { useEffect, useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { useAuth } from './AuthContext'

export const CallbackPage = () => {
  const auth = useAuth()
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const [error, setError] = useState<string>()

  useEffect(() => {
    let active = true
    const handle = async () => {
      try {
        // The callback flow is handled by AuthContext's token subscription
        // If bootstrap succeeds, redirect to overview
        if (auth.bootstrap) {
          navigate('/overview', { replace: true })
          return
        }
        // If still loading, wait
        if (auth.loading) return
        // If not loading and no bootstrap, something went wrong
        if (!active) return
        const errorParam = searchParams.get('error_description') ?? searchParams.get('error')
        setError(errorParam ?? '统一身份登录失败，请重试')
      } catch (e) {
        if (active) setError(e instanceof Error ? e.message : '登录回调处理失败')
      }
    }
    void handle()
    return () => { active = false }
  }, [auth.bootstrap, auth.loading, navigate, searchParams])

  if (auth.loading) {
    return <Spin fullscreen description="完成统一身份登录" />
  }

  if (auth.bootstrap) {
    return <Spin fullscreen description="登录成功，跳转中" />
  }

  if (error) {
    return (
      <Result
        status="error"
        title="登录失败"
        subTitle={error}
        extra={<Button type="primary" onClick={() => navigate('/login', { replace: true })}>返回登录</Button>}
      />
    )
  }

  return <Spin fullscreen description="处理中" />
}
```

- [ ] **Step 2: Commit**

```bash
git add egon-cola-platform-idp-admin-web/src/auth/CallbackPage.tsx
git commit -m "fix(idp): add CallbackPage handling loading/error/success preventing stuck spinner"
```

### Task 21-25: IDP Feature 页面组件

每个页面组件基于 `AdminConsole.tsx` 中对应 section 的逻辑提取，用 `useQuery`/`useMutation` 重写。

**Task 21: OverviewPage**

- Create: `egon-cola-platform-idp-admin-web/src/features/overview/OverviewPage.tsx`

```typescript
import { Card, Descriptions } from 'antd'
import { useAuth } from '../../auth/AuthContext'

export const OverviewPage = () => {
  const auth = useAuth()
  if (!auth.bootstrap) return null
  return (
    <Card title="当前授权上下文">
      <Descriptions column={2} bordered>
        <Descriptions.Item label="全局身份">{auth.bootstrap.identitySub}</Descriptions.Item>
        <Descriptions.Item label="租户">{auth.bootstrap.tenantId}</Descriptions.Item>
        <Descriptions.Item label="RBAC3 用户">{auth.bootstrap.rbac3UserId}</Descriptions.Item>
        <Descriptions.Item label="系统">{auth.bootstrap.systemCode}</Descriptions.Item>
        <Descriptions.Item label="权限数">{auth.bootstrap.permissions.length}</Descriptions.Item>
        <Descriptions.Item label="策略版本">{auth.bootstrap.policyVersion}</Descriptions.Item>
      </Descriptions>
    </Card>
  )
}
```

Commit: `git commit -m "refactor(idp): extract OverviewPage"`

**Task 22: UserListPage**

- Create: `egon-cola-platform-idp-admin-web/src/features/users/UserListPage.tsx`

使用 `useQuery` + `useMutation`（react-query），所有 mutation 包裹 try/catch + `message.error`。

- [ ] **Step 1: 创建 UserListPage.tsx**

```typescript
import { Button, Card, Form, Input, Modal, Space, Table, Tag, message } from 'antd'
import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useAuth } from '../../auth/AuthContext'
import { usePermission, PageState } from '@egon-cola/admin-web-shared'
import type { IdentityUser } from '../../api/types'

export const UserListPage = () => {
  const auth = useAuth()
  const queryClient = useQueryClient()
  const { has } = usePermission(auth.bootstrap?.permissions ?? [])
  const [modalOpen, setModalOpen] = useState(false)
  const [form] = Form.useForm<{ username: string; displayName: string }>()
  const [messageApi, contextHolder] = message.useMessage()

  const usersQuery = useQuery({
    queryKey: ['idp', 'users'],
    queryFn: () => auth.httpClient.request<IdentityUser[]>('/api/v1/identity/users'),
  })

  const createMutation = useMutation({
    mutationFn: (values: { username: string; displayName: string }) =>
      auth.httpClient.request<{ oneTimePassword: string }>('/api/v1/identity/users', {
        method: 'POST',
        body: JSON.stringify(values),
      }),
    onSuccess: async (result) => {
      setModalOpen(false)
      form.resetFields()
      await queryClient.invalidateQueries({ queryKey: ['idp', 'users'] })
      Modal.success({
        title: '用户已创建',
        content: `一次性密码：${result.oneTimePassword}（关闭后不再显示）`,
      })
    },
    onError: (err) => { void messageApi.error(err instanceof Error ? err.message : '创建失败') },
  })

  const resetPasswordMutation = useMutation({
    mutationFn: (subject: string) =>
      auth.httpClient.request<{ oneTimePassword: string }>(
        `/api/v1/identity/users/${encodeURIComponent(subject)}/password-reset`,
        { method: 'POST' },
      ),
    onSuccess: (result) => {
      Modal.success({ title: '密码已重置', content: `一次性密码：${result.oneTimePassword}` })
    },
    onError: (err) => { void messageApi.error(err instanceof Error ? err.message : '重置失败') },
  })

  const revokeMutation = useMutation({
    mutationFn: (subject: string) =>
      auth.httpClient.request(`/api/v1/identity/users/${encodeURIComponent(subject)}/revoke-all`, { method: 'POST' }),
    onSuccess: async () => {
      void messageApi.success('该用户的全部刷新会话已撤销')
      await queryClient.invalidateQueries({ queryKey: ['idp', 'users'] })
    },
    onError: (err) => { void messageApi.error(err instanceof Error ? err.message : '撤销失败') },
  })

  return (
    <>
      {contextHolder}
      <Card
        title="全局身份用户"
        extra={has('idp:identity-user:create') && (
          <Button type="primary" onClick={() => setModalOpen(true)}>创建用户</Button>
        )}
      >
        <PageState
          loading={usersQuery.isPending}
          error={usersQuery.error}
          empty={usersQuery.data?.length === 0}
          onRetry={() => { void usersQuery.refetch() }}
        >
          <Table<IdentityUser>
            rowKey="subject"
            dataSource={usersQuery.data ?? []}
            columns={[
              { title: '用户名', dataIndex: 'username' },
              { title: '显示名', dataIndex: 'displayName' },
              { title: '状态', dataIndex: 'status', render: (v: string) => <Tag>{v}</Tag> },
              { title: 'Token 版本', dataIndex: 'tokenVersion' },
              {
                title: '操作',
                render: (_: unknown, row: IdentityUser) => (
                  <Space>
                    {has('idp:identity-user:password-reset') && (
                      <Button size="small" onClick={() => resetPasswordMutation.mutate(row.subject)} loading={resetPasswordMutation.isPending}>重置密码</Button>
                    )}
                    {has('idp:identity-user:revoke-all') && (
                      <Button size="small" danger onClick={() => revokeMutation.mutate(row.subject)} loading={revokeMutation.isPending}>撤销会话</Button>
                    )}
                  </Space>
                ),
              },
            ]}
          />
        </PageState>
      </Card>
      <Modal
        title="创建全局身份用户"
        open={modalOpen}
        confirmLoading={createMutation.isPending}
        onCancel={() => setModalOpen(false)}
        onOk={() => { void form.validateFields().then((values) => createMutation.mutate(values)) }}
      >
        <Form form={form} layout="vertical">
          <Form.Item name="username" label="用户名" rules={[{ required: true }]}><Input /></Form.Item>
          <Form.Item name="displayName" label="显示名" rules={[{ required: true }]}><Input /></Form.Item>
        </Form>
      </Modal>
    </>
  )
}
```

- [ ] **Step 2: Commit**

```bash
git add egon-cola-platform-idp-admin-web/src/features/users/UserListPage.tsx
git commit -m "refactor(idp): extract UserListPage with react-query and error handling"
```

**Task 23: ClientListPage**

- Create: `egon-cola-platform-idp-admin-web/src/features/clients/ClientListPage.tsx`

- [ ] **Step 1: 创建 ClientListPage.tsx**

```typescript
import { Button, Card, Form, Input, Modal, Space, Table, message } from 'antd'
import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useAuth } from '../../auth/AuthContext'
import { usePermission, PageState } from '@egon-cola/admin-web-shared'
import type { OAuthClientView } from '../../api/types'

interface ClientFormValues {
  clientId: string; clientName: string; redirectUri: string; audience: string
}

export const ClientListPage = () => {
  const auth = useAuth()
  const queryClient = useQueryClient()
  const { has } = usePermission(auth.bootstrap?.permissions ?? [])
  const [modalOpen, setModalOpen] = useState(false)
  const [form] = Form.useForm<ClientFormValues>()
  const [messageApi, contextHolder] = message.useMessage()

  const clientsQuery = useQuery({
    queryKey: ['idp', 'clients'],
    queryFn: () => auth.httpClient.request<OAuthClientView[]>('/api/v1/identity/clients'),
  })

  const createMutation = useMutation({
    mutationFn: (values: ClientFormValues) =>
      auth.httpClient.request('/api/v1/identity/clients', {
        method: 'POST',
        body: JSON.stringify({
          clientId: values.clientId, clientName: values.clientName,
          accessTokenTtlSeconds: 900, refreshTokenTtlSeconds: 604800,
          redirectUris: [values.redirectUri], audiences: [values.audience],
        }),
      }),
    onSuccess: async () => {
      setModalOpen(false)
      form.resetFields()
      void messageApi.success('客户端已创建')
      await queryClient.invalidateQueries({ queryKey: ['idp', 'clients'] })
    },
    onError: (err) => { void messageApi.error(err instanceof Error ? err.message : '创建失败') },
  })

  return (
    <>
      {contextHolder}
      <Card
        title="OAuth 公共客户端"
        extra={has('idp:oauth-client:create') && (
          <Button type="primary" onClick={() => setModalOpen(true)}>创建客户端</Button>
        )}
      >
        <PageState
          loading={clientsQuery.isPending}
          error={clientsQuery.error}
          empty={clientsQuery.data?.length === 0}
          onRetry={() => { void clientsQuery.refetch() }}
        >
          <Table<OAuthClientView>
            rowKey="clientId"
            dataSource={clientsQuery.data ?? []}
            columns={[
              { title: 'Client ID', dataIndex: 'clientId' },
              { title: '名称', dataIndex: 'clientName' },
              { title: '状态', dataIndex: 'status' },
              { title: 'PKCE', dataIndex: 'pkceRequired', render: (v: boolean) => v ? 'S256' : '否' },
              { title: '回调地址', dataIndex: 'redirectUris', render: (v: string[]) => v.join(', ') },
              { title: 'Audience', dataIndex: 'audiences', render: (v: string[]) => v.join(', ') },
            ]}
          />
        </PageState>
      </Card>
      <Modal
        title="创建 OAuth 公共客户端"
        open={modalOpen}
        confirmLoading={createMutation.isPending}
        onCancel={() => setModalOpen(false)}
        onOk={() => { void form.validateFields().then((values) => createMutation.mutate(values)) }}
      >
        <Form form={form} layout="vertical">
          <Form.Item name="clientId" label="Client ID" rules={[{ required: true }]}><Input /></Form.Item>
          <Form.Item name="clientName" label="名称" rules={[{ required: true }]}><Input /></Form.Item>
          <Form.Item name="redirectUri" label="精确回调地址" rules={[{ required: true }]}><Input /></Form.Item>
          <Form.Item name="audience" label="Audience" rules={[{ required: true }]}><Input /></Form.Item>
        </Form>
      </Modal>
    </>
  )
}
```

- [ ] **Step 2: Commit**

```bash
git add egon-cola-platform-idp-admin-web/src/features/clients/ClientListPage.tsx
git commit -m "refactor(idp): extract ClientListPage with react-query"
```

**Task 24: SigningKeyPage**

- Create: `egon-cola-platform-idp-admin-web/src/features/keys/SigningKeyPage.tsx`

- [ ] **Step 1: 创建 SigningKeyPage.tsx**

```typescript
import { Card, Table } from 'antd'
import { useQuery } from '@tanstack/react-query'
import { useAuth } from '../../auth/AuthContext'
import { PageState } from '@egon-cola/admin-web-shared'
import type { SigningKeyView } from '../../api/types'

export const SigningKeyPage = () => {
  const auth = useAuth()
  const query = useQuery({
    queryKey: ['idp', 'keys'],
    queryFn: () => auth.httpClient.request<SigningKeyView[]>('/api/v1/identity/signing-keys'),
  })

  return (
    <Card title="签名密钥（私钥永不返回浏览器）">
      <PageState
        loading={query.isPending}
        error={query.error}
        empty={query.data?.length === 0}
        onRetry={() => { void query.refetch() }}
      >
        <Table<SigningKeyView>
          rowKey="kid"
          dataSource={query.data ?? []}
          columns={[
            { title: 'KID', dataIndex: 'kid' },
            { title: '算法', dataIndex: 'algorithm' },
            { title: '状态', dataIndex: 'status' },
            { title: '当前服务', dataIndex: 'runtimeServing', render: (v: boolean) => v ? '是' : '否' },
            { title: '版本', dataIndex: 'version' },
          ]}
        />
      </PageState>
    </Card>
  )
}
```

- [ ] **Step 2: Commit**

```bash
git add egon-cola-platform-idp-admin-web/src/features/keys/SigningKeyPage.tsx
git commit -m "refactor(idp): extract SigningKeyPage with react-query"
```

**Task 25: AuditLogPage**

- Create: `egon-cola-platform-idp-admin-web/src/features/audits/AuditLogPage.tsx`

提取审计日志列表，增加分页。

- [ ] **Step 1: 创建 AuditLogPage.tsx**

```typescript
import { Card, Table, Tag } from 'antd'
import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { useAuth } from '../../auth/AuthContext'
import { PageState } from '@egon-cola/admin-web-shared'
import type { AuditPage, AuditEntry } from '../../api/types'

const PAGE_SIZE = 20

export const AuditLogPage = () => {
  const auth = useAuth()
  const [page, setPage] = useState(0)

  const query = useQuery({
    queryKey: ['idp', 'audits', page],
    queryFn: () =>
      auth.httpClient.request<AuditPage>(`/api/v1/identity/audits?page=${page}&size=${PAGE_SIZE}`),
  })

  return (
    <Card title={`安全审计（${query.data?.totalElements ?? 0}）`}>
      <PageState
        loading={query.isPending}
        error={query.error}
        empty={query.data?.content.length === 0}
        onRetry={() => { void query.refetch() }}
      >
        <Table<AuditEntry>
          rowKey="id"
          dataSource={query.data?.content ?? []}
          pagination={{
            current: page + 1,
            pageSize: PAGE_SIZE,
            total: query.data?.totalElements ?? 0,
            onChange: (p) => setPage(p - 1),
          }}
          columns={[
            { title: '时间', dataIndex: 'occurredAt' },
            { title: '事件', dataIndex: 'eventType' },
            { title: '操作者', dataIndex: 'actorSub' },
            { title: '目标', dataIndex: 'targetSub' },
            { title: '结果', dataIndex: 'result', render: (v: string) => <Tag>{v}</Tag> },
            { title: '原因', dataIndex: 'reason' },
          ]}
        />
      </PageState>
    </Card>
  )
}
```

- [ ] **Step 2: Commit**

```bash
git add egon-cola-platform-idp-admin-web/src/features/audits/AuditLogPage.tsx
git commit -m "fix(idp): extract AuditLogPage with proper pagination"

### Task 26: 删除 IDP AdminConsole.tsx + 清理旧代码

**Files:**
- Delete: `egon-cola-platform-idp-admin-web/src/app/AdminConsole.tsx`

- [ ] **Step 1: 删除并验证构建**

```bash
git rm egon-cola-platform-idp-admin-web/src/app/AdminConsole.tsx
cd egon-cola-platform-idp-admin-web && npm run build
```

- [ ] **Step 2: Commit**

```bash
git commit -m "refactor(idp): remove monolithic AdminConsole, all sections extracted to feature pages"
```

---

## Phase 6: RBAC3 重构

### Task 27: RBAC3 安装共享包

**Files:**
- Modify: `egon-cola-platform-rbac3-admin-web/package.json`

添加 `"@egon-cola/admin-web-shared": "file:../egon-cola-platform-admin-web-shared"`、`"i18next"`、`"react-i18next"` 到 dependencies。`npm install`。

Commit: `git commit -m "chore(rbac3): add shared package and i18n dependencies"`

### Task 28: RBAC3 main.tsx + App.tsx

- Modify `main.tsx`: 接入 `injectTokens()` + `initI18n()` + `AdminThemeProvider` + `I18nProvider`
- Modify `App.tsx`: 替换项目内 `AppErrorBoundary` 为共享包版本（带 `onError`），替换 `ConfigProvider` 为共享 `AdminThemeProvider`

Commit: `git commit -m "refactor(rbac3): use shared ThemeProvider, ErrorBoundary, i18n"`

### Task 29: RBAC3 API 层 — 基于共享 HttpClient

- Modify `adminApiClient.ts`: 内部 `fetch` 委托给共享 `createHttpClient`，保留 `UnifiedRbac3ApiClient` 适配层
- 修复 `roleActivationRequired: false` 写死
- 修复 `tokenClaims`/`expiresIn` 使用共享 `jwt.ts`

Commit: `git commit -m "refactor(rbac3): delegate HTTP to shared HttpClient, fix adapter"`

### Task 30: RBAC3 删除项目内 oauthClient + tokenStore + PageState

- Delete `features/auth/oauthClient.ts` + 其测试
- Delete `features/shared/PageState.tsx`
- 所有引用替换为共享包导入

Commit: `git commit -m "refactor(rbac3): replace local oauth/PageState with shared package"`

### Task 31: RBAC3 路由懒加载

- [ ] **Step 1: 修改 governance.routes.tsx**

每个 route descriptor 的 `component` 改为 `React.lazy` 导入：

```typescript
// governance.routes.tsx
import { lazy } from 'react'
import type { FeatureRouteDescriptor } from '../shared/RouteDescriptor'

const TenantListPage = lazy(() => import('../tenant/TenantListPage'))
const TenantDetailPage = lazy(() => import('../tenant/TenantDetailPage'))
const UserDirectoryPage = lazy(() => import('../directory/UserDirectoryPage'))
const OrgPositionSnapshotPage = lazy(() => import('../directory/OrgPositionSnapshotPage'))
const ManagementPolicyPage = lazy(() => import('../management-policy/ManagementPolicyPage'))

export const governanceRouteDescriptors: readonly FeatureRouteDescriptor[] = [
  {
    key: 'tenants', path: '/governance/tenants', title: '租户管理',
    permission: 'system:tenant:read', componentKey: 'tenants',
    component: TenantListPage, navigationOrder: 10, hideFromNav: false,
  },
  // ... 其他 route，增加 hideFromNav 字段
]
```

- [ ] **Step 2: 同样修改 authorization.routes.tsx 和 runtime.routes.tsx**

- [ ] **Step 3: 修改 router.tsx**

```typescript
// router.tsx
import { Spin } from 'antd'
import { Suspense } from 'react'
// ... imports stay the same

const PageFallback = () => <Spin style={{ display: 'grid', placeItems: 'center', minHeight: 300 }} />

export const ApplicationRouter = () => (
  <AdminLayout>
    <Suspense fallback={<PageFallback />}>
      <Routes>
        {applicationRouteDescriptors.map((route) => (
          <Route key={route.key} path={route.path} element={
            <RouteAccessGuard route={route}><route.component /></RouteAccessGuard>
          } />
        ))}
        <Route path="*" element={fallback} />
      </Routes>
    </Suspense>
  </AdminLayout>
)
```

- [ ] **Step 4: 修复 navigation.ts — 用 `hideFromNav` 字段替换 `path.includes(':')` heuristic**

```typescript
// navigation.ts
export const visibleNavigation = (bootstrap: BootstrapView) =>
  applicationRouteDescriptors
    .filter((route) => !route.hideFromNav)  // FIX: use explicit field instead of ':' heuristic
    .filter((route) => isRouteAllowed(bootstrap, route))
```

- [ ] **Step 5: 给 RouteDescriptor 增加字段**

```typescript
// features/shared/RouteDescriptor.ts
export interface FeatureRouteDescriptor {
  readonly key: string
  readonly path: string
  readonly title: string
  readonly permission: string
  readonly componentKey: string
  readonly component: React.LazyExoticComponent<React.ComponentType<Record<string, unknown>>>
  readonly navigationOrder: number
  readonly hideFromNav: boolean            // NEW
  readonly breadcrumb?: string             // NEW
}
```

- [ ] **Step 6: Commit**

```bash
git add egon-cola-platform-rbac3-admin-web/src/features/*.routes.tsx egon-cola-platform-rbac3-admin-web/src/app/router.tsx egon-cola-platform-rbac3-admin-web/src/app/navigation.ts egon-cola-platform-rbac3-admin-web/src/features/shared/RouteDescriptor.ts
git commit -m "refactor(rbac3): add route-level lazy loading, fix navigation visibility with hideFromNav"
```

### Task 32: RBAC3 各页面使用 useFeatureQuery + PageTemplate

每个页面做以下三处替换：

**Pattern A: 替换 query boilerplate (15 处)**

```typescript
// BEFORE（每页重复）
const { status } = useRbac3Session()
const { effectiveTenantId } = useFeatureTenantContext()
const api = someApi(useFeatureApi())
const query = useQuery({
  queryKey: ['rbac3', 'xxx', effectiveTenantId ?? 'none'],
  queryFn: () => api.fetch(),
  enabled: status === 'READY',
})

// AFTER（1 行）
import { useFeatureQuery } from '@egon-cola/admin-web-shared'
const query = useFeatureQuery(['xxx'], (client) => someApi(client).fetch(), {
  status, effectiveTenantId, featureApi: useFeatureApi(),
})
```

**Pattern B: 替换 Card 包装为 PageTemplate**

```typescript
// BEFORE
<Card title="页面标题" extra={<Button>操作</Button>}>
  <PageState loading={...} error={...} empty={...}>
    <Table ... />
  </PageState>
</Card>

// AFTER
<PageTemplate
  title="页面标题"
  extra={<Button>操作</Button>}
  pageState={{ loading: query.isPending, error: query.error, empty: !query.data, onRetry: () => query.refetch() }}
>
  <Table ... />
</PageTemplate>
```

**Pattern C: 替换 usePermission**

```typescript
// BEFORE
const { has } = usePermission(bootstrap?.permissions ?? [])

// AFTER (same import, now from shared package)
import { usePermission } from '@egon-cola/admin-web-shared'
```

修改以下全部页面文件：
- `OverviewPage.tsx`、`TenantListPage.tsx`、`UserDirectoryPage.tsx`、`OrgPositionSnapshotPage.tsx`
- `RolePermissionPage.tsx`、`RoleGraphPage.tsx`、`RoleActivationPage.tsx`
- `ApplicationListPage.tsx`、`ManifestDetailPage.tsx`、`ResourceCatalogPage.tsx`
- `AssignmentListPage.tsx`、`ConstraintPage.tsx`、`ManagementPolicyPage.tsx`
- `AuditLogPage.tsx`、`SessionListPage.tsx`、`AuthorizationSimulationPage.tsx`、`RuntimeStatusPage.tsx`

Commit: `git commit -m "refactor(rbac3): apply useFeatureQuery, PageTemplate, usePermission to all pages"`

### Task 33: RBAC3 Bug 修复

- [ ] **Step 1: RoleGraphPage — 加并发上限**

```typescript
// RoleGraphPage.tsx — useQueries 增加 combine + 并发上限
const MAX_CONCURRENT = 50
const roleQueries = useQueries({
  queries: roles.slice(0, MAX_CONCURRENT).map((role) => ({
    queryKey: ['rbac3', 'role-impact', tenantId, role.id],
    queryFn: () => api.impact(role.id),
    enabled: status === 'READY',
  })),
  combine: (results) => ({
    data: results.flatMap((r) => r.data ?? []),
    pending: results.some((r) => r.isPending),
    error: results.find((r) => r.error)?.error ?? null,
  }),
})
```

- [ ] **Step 2: ConstraintPage — 移除死按钮**

```typescript
// ConstraintPage.tsx — 移除无 onClick 的"新增约束"按钮
- <Button type="primary">新增约束</Button>
// （如果以后需要，再加回来并绑定 onClick）
```

- [ ] **Step 3: TenantDetailPage — 删除不可达文件**

```bash
git rm egon-cola-platform-rbac3-admin-web/src/features/tenant/TenantDetailPage.tsx
# 同时从 governance.routes.tsx 中移除（如果存在）
```

- [ ] **Step 4: AuditLogPage — 修复时间冻结**

```typescript
// AuditLogPage.tsx — initialFilter 从模块级 const 改为 state 初始化函数
const [filter, setFilter] = useState<AuditFilter>(() => {
  const now = new Date()
  const yesterday = new Date(now.getTime() - 24 * 60 * 60 * 1000)
  return { from: yesterday.toISOString(), to: now.toISOString(), limit: 50 }
})
```

- [ ] **Step 5: OverviewPage — 删除无效请求**

```typescript
// OverviewPage.tsx — 删除 overviewApi runtime 调用
// 只渲染 bootstrap 数据（Statistic 组件已经在用 bootstrap 字段）
// 删除 `const api = overviewApi(useFeatureApi())` 和 `useQuery` 调用
```

- [ ] **Step 6: FeatureApi.tsx — 修复 useMemo deps**

```typescript
// FeatureApi.tsx — setTargetTenantId 加入 useMemo 依赖
const value = useMemo<FeatureApiContextValue>(() => ({
  client: tenantClient,
  effectiveTenantId,
  targetTenantId,
  setTargetTenantId,
}), [effectiveTenantId, targetTenantId, tenantClient, setTargetTenantId]) // FIX: added setTargetTenantId
```

- [ ] **Step 7: Commit**

```bash
git add egon-cola-platform-rbac3-admin-web/src/features/
git commit -m "fix(rbac3): fix N+1 cap, dead controls, frozen filter, missing deps, stale overview"
```

### Task 34: 删除 RBAC3 global.css

- Delete `styles/global.css`
- 样式已由共享 `injectTokens()` 提供

Commit: `git commit -m "refactor(rbac3): remove local CSS, use shared design tokens"`

### Task 35: 最终验证

- [ ] 构建共享包: `cd admin-web-shared && npm run build`
- [ ] 构建 IDP: `cd egon-cola-platform-idp-admin-web && npm run build`
- [ ] 构建 RBAC3: `cd egon-cola-platform-rbac3-admin-web && npm run build`
- [ ] 运行所有测试: 两个项目 `npm test`

Commit: 如有修复，commit `fix: build and test fixes after refactoring`

---

## Plan Review Checklist

- [ ] 共享包 15 个 task 全部完成且可独立构建
- [ ] IDP 11 个 task 全部完成，AdminConsole 已拆解
- [ ] RBAC3 9 个 task 全部完成，Bug 已修复
- [ ] 所有已知 Bug 有对应 task 修复
- [ ] 两个项目构建通过，测试通过
