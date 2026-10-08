# Yuheng Admin Web

[English](README.md) | [Yuheng 概览](../README.md)

Yuheng Admin Web 是独立的 React 管理控制台。它只调用 Yuheng Admin API，不直接调用
Yuheng Engine 业务路由、Tianshu Admin 或 Provider 端点。

## 认证

控制台不持有凭据或 Token。`src/auth/AuthContext.tsx` 使用
`@egon-cola/xingyuan-admin-web-shared` 的 `createGatewayAuthClient`；登录跳转到配置的 Edge OIDC
registration（`/oauth2/authorization/edge-ui`）。Edge 在服务端保管 OAuth Token，并给浏览器
`HttpOnly` 会话 Cookie。浏览器读取 Edge BFF 会话，用其 CSRF 响应发起退出，并通过 Edge 会话发送后续
API 请求；不会提交租户、用户名或密码，也不会保存 AT/RT。

Actor 与 capabilities 来自 `GET /api/v1/auth/bootstrap`（`GatewayAuthBootstrapController`，该接口
本身要求 `yuheng:read` 权限）。Provider 在挂载时、每次登录成功后调用它，并可通过
`refreshAuthorization()` 再次刷新。`CapabilityProvider` 把返回的 `permissions` 列表转成能力集合，
`hasCapability` 同时接受 `*` 通配。退出登录通过 Edge BFF 的 CSRF 保护端点清理本地会话与授权状态。
bootstrap 失败时 `RequireAuth` 跳转 `/login`，`RequireCapability` 依据同一列表渲染 403。Admin Web
从不发送 actor identity header。

## 开发

```bash
npm ci
npm run typecheck
npm test -- --run
npm run lint
npm run build
```

`npm run dev` 绑定 `127.0.0.1:18141` 且 `strictPort`，并把 `/oauth2`、`/bff` 和 `/login/oauth2`
OIDC 回调代理到 Edge 对外入口，把 `/api` 代理到 Yuheng Admin。上游可分别用 `YUHENG_AUTH_PROXY` 和 `YUHENG_ADMIN_PROXY` 覆盖；认证
上游应指向 Edge。Yuheng Admin 仍校验签名 Token 并执行 Tianquan-Jianshen 授权判定。

`npm run e2e` 通过 Playwright 针对 `http://127.0.0.1:4173` 执行；若无进程监听，会自动启动
`npm run dev -- --host 127.0.0.1 --port 4173`。用例会mock OIDC入口与管理API响应，用于验证浏览器
接线，不证明真实Edge、授权服务器、Redis会话或Yuheng拓扑可用。

## 运行时配置

Bundle 只读取 `VITE_YUHENG_ORIGIN` 这一个 Origin 变量，留空时请求保持在当前 Origin——这就是开发
代理和容器代理都无需重新构建即可生效的原因。登录不从浏览器读取租户标识。

镜像内 `static-server.mjs` 提供 `/app/dist` 静态资源、响应 `/healthz`，并把 `/api/*` 反向代理到
`YUHENG_ADMIN_API_BASE_URL`（默认 `http://yuheng-admin:18080`）。上游为 `http:` 时还必须显式设置
`YUHENG_ADMIN_API_DEVELOPMENT_PLAINTEXT=true`；上游为 `https:` 时需要
`YUHENG_ADMIN_API_TLS_CA_PATH`、`YUHENG_ADMIN_API_TLS_CERTIFICATE_PATH` 和
`YUHENG_ADMIN_API_TLS_PRIVATE_KEY_PATH`。`PORT` 默认 `8080`。

作用域是页面自己的查询条件。每个页面通过 `hooks/scopeSearchParams.ts` 把选中的 `bizCode`、
`namespace`、`env`、`appCode` 保存在本页面 URL 中，页面之间切换不会再因为共享筛选条件而隐藏其他
作用域的数据。跨作用域页面先加载当前账号有权限的完整结果集，再在页面内筛选。
`GET /api/v1/yuheng/admin/scopes` 只用于填充页面控件和创建表单，不再作为全局顶部上下文。

Dashboard 和 Provider 页面要求四个字段完整；Trace 和 Audit 页面要求 `env`、`namespace`；
网关分组、MCP Server、Remote Provider 页面使用可选的 `env`、`namespace` 筛选；接口目录、
Applications、OpenAPI Sync 与 MCP Resource/Prompt 页面使用完整的可选作用域筛选。

不要把凭据写入并提交的 `.env` 文件。TLS 终止、Engine 侧针对非安全方法的 Cookie 可信 Origin
白名单，以及 Yuheng Admin 授权策略仍由部署平台负责。
