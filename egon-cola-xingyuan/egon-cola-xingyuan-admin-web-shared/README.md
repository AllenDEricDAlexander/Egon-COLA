# @egon-cola/xingyuan-admin-web-shared

Egon COLA 平台前端公共层，供四个管理控制台（Tianshu / Yuheng / Tianquan-Shoubing /
Tianquan-Jianshen）以及 `egon-cola-xingyuan-admin-portal` 微前端宿主共享。

## 包含内容

- **Layout**：`EnterpriseLayout` / `EnterpriseHeader` / `EnterpriseFooter` 及完整 TypeScript 类型。平台通过 config 传入平台名、Logo、导航菜单、用户信息与全局操作区，Header 自动按当前路由高亮（最长前缀匹配），窄屏自动切换抽屉导航，Layout 保证最小高度与贴底 Footer。
- **Theme**：`AdminThemeProvider`、`designTokens`、`injectTokens`。
- **API**：`createHttpClient`、`ApiError`、`classifyApiError`。
- **Auth**：`createGatewayAuthClient` 与 `GatewayAuthError`。它封装平台统一身份的
  Cookie/CSRF 浏览器登录（`GET /oauth2/login/csrf` + `POST /oauth2/login`，携带
  `X-Tianquan-Shoubing-CSRF`）和 `bootstrap(path?)` 授权引导读取，默认路径
  `/api/v1/auth/bootstrap`。会话保存在 `HttpOnly` Cookie 中，包内不实现 Token 存储，
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

```bash
npm run release:patch   # 0.1.x 补丁版本：自动升版本号（不打 git tag）+ 构建 + npm publish
npm run release:minor   # 次版本
```

注意事项：

- `build` 的 `prebuild` 会从仓库根复制 `favicon.png` 进包内，`postbuild` 会删除 `node_modules`（如需继续本地开发请重新 `npm install --legacy-peer-deps`）。
- 发布后，四个业务 Web 需执行 `rm -rf node_modules/@egon-cola/xingyuan-admin-web-shared && npm install --legacy-peer-deps` 切换到 registry 版本。
