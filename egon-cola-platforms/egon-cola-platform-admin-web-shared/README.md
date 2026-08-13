# @egon-cola/admin-web-shared

Egon COLA 四个管理 Web 平台（DDC / Gateway / IDP / RBAC3）共享的前端公共层。

## 包含内容

- **Layout**：`EnterpriseLayout` / `EnterpriseHeader` / `EnterpriseFooter` 及完整 TypeScript 类型。平台通过 config 传入平台名、Logo、导航菜单、用户信息与全局操作区，Header 自动按当前路由高亮（最长前缀匹配），窄屏自动切换抽屉导航，Layout 保证最小高度与贴底 Footer。
- **Theme**：`AdminThemeProvider`、`designTokens`、`injectTokens`。
- **API / Auth**：`createHttpClient`、`createOAuthClient`、`createTokenStore`、`decodeTokenPayload` 等。
- **i18n**：`initI18n`、`I18nProvider`、`useT`、`changeLanguage`。
- **Components / Hooks**：`PageState`、`PageTemplate`、`AppErrorBoundary`、`usePermission`、`useFeatureQuery`。
- **Vite 插件**（`@egon-cola/admin-web-shared/vite-plugin`）：`egonFaviconPlugin()`，统一为四个平台注入仓库根 `favicon.png`（dev/preview 中间件 + index.html 注入 + build 产物输出）。

## 使用

```tsx
import { EnterpriseLayout, type EnterpriseLayoutConfig } from '@egon-cola/admin-web-shared'
import { egonFaviconPlugin } from '@egon-cola/admin-web-shared/vite-plugin'

const config: EnterpriseLayoutConfig = {
  platformName: 'Gateway Admin',
  navigation: [{ key: 'dashboard', label: '总览', path: '/dashboard' }],
  user: { name: 'admin', menu: [{ key: 'logout', label: '退出登录' }] },
  footer: { version: '5.2.3' },
}
```

## 发布

```bash
npm run release:patch   # 0.1.x 补丁版本：自动升版本号（不打 git tag）+ 构建 + npm publish
npm run release:minor   # 次版本
```

注意事项：

- `build` 的 `prebuild` 会从仓库根复制 `favicon.png` 进包内，`postbuild` 会删除 `node_modules`（如需继续本地开发请重新 `npm install --legacy-peer-deps`）。
- 发布后，四个业务 Web 需执行 `rm -rf node_modules/@egon-cola/admin-web-shared && npm install --legacy-peer-deps` 切换到 registry 版本。
