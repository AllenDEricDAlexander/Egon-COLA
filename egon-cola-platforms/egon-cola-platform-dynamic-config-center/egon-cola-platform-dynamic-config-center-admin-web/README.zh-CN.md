# DDC Admin Web

[English](README.md) | [DDC 总览](../README.zh-CN.md)

DDC Admin Web 是 Egon COLA 动态配置中心的独立管理控制台。它只与 DDC Admin
通信，不直接调用其他组件。

## 认证

沿用 DDC Admin 的 Bearer Token 模型：在登录页粘贴 admin Bearer Access Token；
Token 仅保存在 `sessionStorage`，不会写入 URL 或发送到浏览器会话的服务端。
收到 401 时清空 Token 并回到登录页。

## 开发

```bash
npm ci
npm run typecheck
npm test -- --run
npm run lint
npm run build
```

`npm run dev` 会把 `/api` 代理到本机 DDC Admin（默认
`http://127.0.0.1:18080`，可用 `DDC_ADMIN_PROXY` 覆盖）。

注册中心首次请求会携带完整四级作用域。本地作用域不是
`default / default-app / dev / default` 时，可设置以下构建时默认值：

```bash
VITE_DDC_ADMIN_DEFAULT_BIZ_CODE=retail \
VITE_DDC_ADMIN_DEFAULT_APP_CODE=orders \
VITE_DDC_ADMIN_DEFAULT_ENV=local \
VITE_DDC_ADMIN_DEFAULT_NAMESPACE=default \
npm run dev
```

`npm run e2e` 仅在存在可达的 DDC Admin 且 `DDC_E2E_TOKEN` 配置了有效 token
时运行（上游地址可用 `DDC_E2E_ADMIN_URL` 覆盖）。该命令不能替代 DDC 的
Maven 测试套件。

## 运行时配置

浏览器通过 static-server 的 `/api` 反向代理访问 DDC Admin。用
`DDC_ADMIN_API_BASE_URL` 指向管理端后端：

| 变量 | 默认值 | 含义 |
|---|---|---|
| `PORT` | `8080` | static-server 的 HTTP 端口 |
| `DDC_ADMIN_API_BASE_URL` | `http://ddc-admin:18080` | `/api` 代理的 DDC Admin 上游 |
| `DDC_ADMIN_API_DEVELOPMENT_PLAINTEXT` | `false` | 允许明文 HTTP 上游，必须显式设为 `true` |
| `DDC_ADMIN_API_TLS_CA_PATH` | — | mTLS 上游的 CA 文件（`https:` 上游必填） |
| `DDC_ADMIN_API_TLS_CERTIFICATE_PATH` | — | mTLS 上游的客户端证书 |
| `DDC_ADMIN_API_TLS_PRIVATE_KEY_PATH` | — | mTLS 上游的客户端私钥 |

不要把凭据写进提交的 `.env` 文件。TLS 终结与 DDC Admin 的授权策略属于部署职责。

## 作用域模型

作用域层级为 业务域（biz）→ 应用（app）→ 命名空间（ns）→ 环境（env）。
注册身份恒为 biz-ns-env-app；业务域、应用、命名空间、环境均为独立管理实体，
禁用任一实体后该作用域的新注册与配置拉取会被拒绝（`DDC_SCOPE_DISABLED`）。

作用域筛选为可选下拉，选项来自后端：业务域列表来自 `/bizs`，应用列表按所选
业务域过滤，命名空间列表按所选应用过滤，环境列表来自受管实体 `/envs`；
所有下拉均支持直接输入新值。注册查询始终携带完整四级作用域，首次查询使用上述构建时默认值。

## 配置资源契约

配置页面按 YAML 资源而不是独立键值项管理配置。创建请求固定提交
`resourceName=application.yml`、`format=YAML` 和完整的 `content`；后端也接受
`application.yaml`，列表会按响应中的实际资源名和格式展示。每个
`bizCode + env + appCode` 只能存在一份 YAML 资源，命名空间绑定只控制可见性。

该接口是破坏性新契约，不再发送或读取 `configKey`、`configValue`、`valueType`
及 `contentChecksum` 等旧字段。

## 部署

```bash
docker build -t egon-cola/ddc-admin-web .
docker run --rm -p 8080:8080 \
  -e DDC_ADMIN_API_BASE_URL=http://ddc-admin:18080 \
  -e DDC_ADMIN_API_DEVELOPMENT_PLAINTEXT=true \
  egon-cola/ddc-admin-web
```

健康检查：`GET /healthz` 返回 `ok`。
