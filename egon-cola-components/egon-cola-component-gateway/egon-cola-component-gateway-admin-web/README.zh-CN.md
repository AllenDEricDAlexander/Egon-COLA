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
Origin；留空时使用当前 Origin。IAM 尚未由宿主平台提供时，
`VITE_GATEWAY_ADMIN_ACTOR_ID` 用于提供占位管理 Actor。

不要把凭据写入并提交的 `.env` 文件。身份提供方、浏览器 CORS/PKCE 配置、TLS 终止以及
Gateway Admin 授权策略仍由部署平台负责。
