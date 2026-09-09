# Gateway Admin Web

[English](README.md) | [Gateway 概览](../README.md)

Gateway Admin Web 是独立的 React 管理控制台。它只调用 Gateway Admin，不直接调用
Gateway Engine、DDC Admin 或 Provider 端点。

## 认证

Admin Web 不发送 actor identity header。默认将已校验的 IAM Bearer Token 存储在
`sessionStorage`，并从 `GET /api/v1/gateway/admin/session` 获取 actor 与 capabilities。
选择“persist login”后，Token Bundle 会移动到 `localStorage`；退出登录会删除两处副本。

可选的自动刷新配置：

```text
VITE_GATEWAY_ADMIN_TOKEN_URL=https://iam.example.com/oauth2/token
VITE_GATEWAY_ADMIN_CLIENT_ID=gateway-admin-web
```

配置的身份提供方必须允许浏览器客户端，并自行实施 CORS/PKCE 策略。Web Bundle 中不嵌入
Client Secret。

## 开发

```bash
npm ci
npm run typecheck
npm test -- --run
npm run lint
npm run build
```

只有在 Gateway Admin 可访问且浏览器场景所需拓扑已经启动时，才运行 `npm run e2e`。
该命令不能替代 Gateway 的 Maven Live Suite。

## 运行时配置

浏览器调用 Gateway Admin。设置 `VITE_GATEWAY_ADMIN_API_BASE_URL` 可使用不同的 API
Origin；留空时使用当前 Origin。Actor 与 capabilities 由已鉴权的 Session API 提供，
浏览器不配置占位 Actor。

作用域是页面自己的查询条件。每个页面会把选中的 `bizCode`、`appCode`、`env`、`namespace`
保存在本页面 URL 中，页面之间切换不会再因为共享筛选条件而隐藏其他作用域的数据。跨作用域
页面先加载当前账号有权限的完整结果集，再在页面内筛选。`GET /api/v1/gateway/admin/scopes`
只用于填充页面控件和创建表单，不再作为全局顶部上下文。

Dashboard 和 Provider 页面要求四个字段完整；Trace 和 Audit 页面要求 `env`、`namespace`；
Gateway Group、MCP Server、Remote Provider 页面使用可选的 `env`、`namespace` 筛选；接口目录、
MCP Resource/Prompt 页面使用完整的可选作用域筛选。

不要把凭据写入并提交的 `.env` 文件。身份提供方、浏览器 CORS/PKCE 配置、TLS 终止以及
Gateway Admin 授权策略仍由部署平台负责。
