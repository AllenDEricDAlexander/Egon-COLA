# @egon-cola/admin-web-shared

Egon COLA 平台前端公共层，供四个管理控制台（Tianshu / Yuheng / Tianquan-Shoubing /
Tianquan-Jianshen）以及 `egon-cola-xingyuan-admin-portal` 微前端宿主共享。

## 包含内容

- **Layout**：`EnterpriseLayout` / `EnterpriseHeader` / `EnterpriseFooter` 及完整 TypeScript 类型。平台通过 config 传入平台名、Logo、导航菜单、用户信息与全局操作区，Header 自动按当前路由高亮（最长前缀匹配），窄屏自动切换抽屉导航，Layout 保证最小高度与贴底 Footer。
- **Theme**：`AdminThemeProvider`、`designTokens`、`injectTokens`。
- **API**：`createHttpClient`、`ApiError`、`classifyApiError`。
- **Auth**：`createGatewayAuthClient` 与 `GatewayAuthError`。它只导航到配置的 Edge OIDC
  registration，并读取 `/bff/session`、`/bff/csrf`、`/bff/logout` 和 `bootstrap(path?)`
  授权引导（默认 `/api/v1/auth/bootstrap`）。浏览器只持 Edge 的 `HttpOnly` 会话 Cookie；包内不接收凭据、不读写 AT/RT，
  也没有 `createOAuthClient` / `createTokenStore` / `decodeTokenPayload` 之类的导出。
- **i18n**：`initI18n`、`I18nProvider`、`useT`、`changeLanguage`、`currentLanguage`。
- **Components / Hooks**：`PageHeader`、`PageState`、`PageTemplate`、
  `AppErrorBoundary`、`usePermission`、`useFeatureQuery`。
- **Vite 插件**（`@egon-cola/xingyuan-admin-web-shared/vite-plugin`）：`egonFaviconPlugin()`，统一为四个控制台与 Portal 宿主注入 `egon-cola-xingyuan/favicon.png`（dev/preview 中间件 + index.html 注入 + build 产物输出）。

## 使用

```tsx
import { EnterpriseLayout, type EnterpriseLayoutConfig } from '@egon-cola/xingyuan-admin-web-shared'
import { egonFaviconPlugin } from '@egon-cola/xingyuan-admin-web-shared/vite-plugin'

const config: EnterpriseLayoutConfig = {
  platformName: 'Yuheng Admin',
  navigation: [{ key: 'dashboard', label: '总览', path: '/dashboard' }],
  user: { name: 'admin', menu: [{ key: 'logout', label: '退出登录' }] },
  footer: { version: '5.4.1' },
}
```

## 发布

实际 npm 发布包为 `@egon-cola/admin-web-shared`；控制台保留
`@egon-cola/xingyuan-admin-web-shared` 源码 import，通过 npm alias 指向此制品。
`0.2.5` 包含当前源码的无密码 OIDC 导航、CSRF logout 与 `remoteState` 契约；
发布前须验证源码测试和实际打包产物，不能仅检查版本号。


```bash
npm run release:patch   # 0.1.x 补丁版本：自动升版本号（不打 git tag）+ 构建 + npm publish
npm run release:minor   # 次版本
```

注意事项：

- `build` 的 `prebuild` 会从仓库根复制 `favicon.png` 进包内，`postbuild` 会删除 `node_modules`（如需继续本地开发请重新 `npm install --legacy-peer-deps`）。
- 发布后，获准接入的业务 Web 按现有 npm alias 和锁文件安装 registry 制品。本轮 `0.2.5` 只更新授柄、鉴神自身；其他平台和模板接入另行安排。
