# Tianshu Admin Web

[English](README.md) | [Tianshu 总览](../README.zh-CN.md)

Tianshu Admin Web 是 Egon COLA 动态配置中心的独立管理控制台。它只与 Tianshu Admin
通信，不直接调用其他组件。

## 认证

控制台通过 `@egon-cola/xingyuan-admin-web-shared` 的 `createGatewayAuthClient` 走平台
统一身份（Tianquan-Shoubing）登录：登录页提交租户 ID、用户名和密码，经 CSRF 保护的
`/oauth2/login` 完成认证，会话保存在浏览器不读取的 `HttpOnly` Cookie 中。

因此控制台不持有 Access Token，也不发送 `Authorization` 头，所有请求依赖 Cookie 凭据。
身份与权限来自 `GET /api/v1/tianshu/auth/bootstrap`，只有响应授予 `TIANSHU_READ` 时
控制台才算已授权。收到 401 时清空本地 bootstrap 状态并回到登录页。

## 开发

```bash
npm ci
npm run typecheck
npm test -- --run
npm run lint
npm run build
```

`npm run dev` 绑定 `http://127.0.0.1:18152`（`strictPort`），并把 `/oauth2` 和 `/api`
一并代理到 Tianshu Admin 前的网关，默认 `http://127.0.0.1:18180`。两个上游可分别用
`TIANSHU_AUTH_PROXY` 和 `TIANSHU_ADMIN_PROXY` 覆盖；`npm run preview` 复用同一张代理表。

注册中心首次请求会携带完整四级作用域。本地作用域不是
`default / default-app / dev / default` 时，可设置以下构建时默认值：

```bash
VITE_TIANSHU_ADMIN_DEFAULT_BIZ_CODE=retail \
VITE_TIANSHU_ADMIN_DEFAULT_APP_CODE=orders \
VITE_TIANSHU_ADMIN_DEFAULT_ENV=local \
VITE_TIANSHU_ADMIN_DEFAULT_NAMESPACE=default \
npm run dev
```

`npm run e2e` 需要存在可达的 Tianshu Admin：Playwright 会先构建控制台，再用
`npm run preview` 在 `http://127.0.0.1:4173` 提供服务，场景流量经上面的 preview 代理表
到达 Admin。该命令不能替代 Tianshu 的 Maven 测试套件。

> 已知缺陷：`e2e/tianshu-admin.spec.ts` 仍在填写早已移除的“粘贴 admin token”表单，并
> 依赖 `TIANSHU_E2E_TOKEN`；未设置该变量时整条用例被跳过，设置后也会在当前登录页上
> 失败。该 spec 需要源码侧修复，可参照 Yuheng Admin Web 的 Playwright 用例改写为
> Cookie 登录。

## 运行时配置

浏览器通过 static-server 的 `/api` 反向代理访问 Tianshu Admin。用
`TIANSHU_ADMIN_API_BASE_URL` 指向管理端后端：

| 变量 | 默认值 | 含义 |
|---|---|---|
| `PORT` | `8080` | static-server 的 HTTP 端口 |
| `TIANSHU_ADMIN_API_BASE_URL` | `http://tianshu-admin:18080` | `/api` 代理的 Tianshu Admin 上游 |
| `TIANSHU_ADMIN_API_DEVELOPMENT_PLAINTEXT` | `false` | 允许明文 HTTP 上游，必须显式设为 `true` |
| `TIANSHU_ADMIN_API_TLS_CA_PATH` | — | mTLS 上游的 CA 文件（`https:` 上游必填） |
| `TIANSHU_ADMIN_API_TLS_CERTIFICATE_PATH` | — | mTLS 上游的客户端证书 |
| `TIANSHU_ADMIN_API_TLS_PRIVATE_KEY_PATH` | — | mTLS 上游的客户端私钥 |

static-server 遇到 `http:` 上游且未开启明文开关时会快速失败，但随仓 `Dockerfile` 已把
`TIANSHU_ADMIN_API_DEVELOPMENT_PLAINTEXT=true` 固化进镜像供本地使用；真实 TLS 环境必须
取消或覆盖该变量。

不要把凭据写进提交的 `.env` 文件。TLS 终结与 Tianshu Admin 的授权策略属于部署职责。

## 作用域模型

作用域层级为 业务域（biz）→ 应用（app）→ 命名空间（ns）→ 环境（env）。
注册身份恒为 biz-ns-env-app；业务域、应用、命名空间、环境均为独立管理实体，
禁用任一实体后该作用域的新注册与配置拉取会被拒绝（`TIANSHU_SCOPE_DISABLED`）。

作用域筛选为可选下拉，选项来自后端：业务域列表来自 `/bizs`，应用列表按所选
业务域过滤，命名空间列表按所选应用过滤，环境列表来自受管实体 `/envs`；
所有下拉均支持直接输入新值。注册查询始终携带完整四级作用域，首次查询使用上述构建时默认值。

## 服务端分页

管理表格调用新增的 `/page` 接口，并读取 `PageResultRecord.records` 和
`PageResultRecord.page`。页码从 1 开始，界面支持每页 10、20 或 50 条。作用域选择器
与命名空间绑定编辑器需要完整选项集，因此有意继续使用旧有列表接口。

注册中心表格对服务标识分页，并仅在选中一个服务后按需分页加载其实例。Tianshu Starter
和 RPC 客户端仍消费完整目录与快照；Admin Web 分页不属于机器 RPC 契约。

## 配置资源契约

配置页面按 YAML 资源而不是独立键值项管理配置。创建请求固定提交
`resourceName=application.yml`、`format=YAML` 和完整的 `content`；后端也接受
`application.yaml`，列表会按响应中的实际资源名和格式展示。每个
`bizCode + env + appCode` 只能存在一份 YAML 资源，命名空间绑定只控制可见性。

该接口是破坏性新契约，不再发送或读取 `configKey`、`configValue`、`valueType`
及 `contentChecksum` 等旧字段。

## 部署

```bash
docker build -t egon-cola/tianshu-admin-web .
docker run --rm -p 8080:8080 \
  -e TIANSHU_ADMIN_API_BASE_URL=http://tianshu-admin:18080 \
  -e TIANSHU_ADMIN_API_DEVELOPMENT_PLAINTEXT=true \
  egon-cola/tianshu-admin-web
```

健康检查：`GET /healthz` 返回 `ok`。
