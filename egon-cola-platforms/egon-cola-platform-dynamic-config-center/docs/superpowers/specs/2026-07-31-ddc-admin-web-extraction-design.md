# DDC Admin Web 摘出设计（2026-07-31）

## 背景与现状

DDC 平台的 webui（管理控制台）目前以 5 个原生 JS 文件（约 1070 行）躺在
`egon-cola-platform-dynamic-config-center-admin/src/main/resources/static/ddc-admin/`，
由 admin 后端（Spring Boot）同源托管。无构建工具、无 npm、无框架；JS 单测
（`admin/src/test/js/`）未接入任何构建链路。

后端 manifest（`/api/v1/ddc/manifest`）已暴露 `frontendModuleKey:
"dynamic-config-center"` 与 `routeBase: "/components/dynamic-config-center"`，
设计意图即 UI 独立于 admin 后端。

仓库已有先例：`egon-cola-platform-gateway` 采用
`gateway-admin`（后端）+ `gateway-admin-web`（独立前端：Vite + React 19 + antd 6 +
TypeScript，Vitest/Playwright，纯 Node 工程不进 Maven reactor，独立 Docker 部署，
`static-server.mjs` 静态托管并反代 `/api`）。

## 决策

| 决策点 | 结论 |
|---|---|
| 部署模型 | 独立部署，对齐 gateway-admin-web 范式 |
| 技术栈 | React 19 + antd 6 + Vite + TypeScript（对齐 gateway） |
| 功能范围 | 补齐全部管理能力（对等迁移现有功能 + 新增发布任务/实例/缓存/应用/命名空间页） |
| 命名 | `egon-cola-platform-dynamic-config-center-admin-web`（对齐 `-admin-web` 范式） |
| 认证 | 沿用"粘贴 Bearer Token 存 sessionStorage"模型，不引入 OAuth/session 体系 |
| Maven 集成 | 不进 Maven reactor，纯 Node 工程（gateway 先例） |

## 架构与组件

```
egon-cola-platforms/
└── egon-cola-platform-dynamic-config-center/
    ├── egon-cola-platform-dynamic-config-center-starter   (不变)
    ├── egon-cola-platform-dynamic-config-center-admin     (移除 static/ddc-admin + src/test/js)
    ├── egon-cola-platform-dynamic-config-center-admin-web (新增，纯 Node 工程)
    └── egon-cola-platform-dynamic-config-center-test      (不变)
```

新工程组件（对齐 gateway-admin-web）：

| 组件 | 内容 |
|---|---|
| `src/` | React 19 + TS 页面：登录、服务注册、配置管理+发布、应用、命名空间、发布任务、实例管理、缓存管理 |
| `static-server.mjs` | 静态托管 dist + `/api` 反代到 admin；env：`DDC_ADMIN_API_BASE_URL`、`DDC_ADMIN_API_DEVELOPMENT_PLAINTEXT`；`/healthz` |
| `Dockerfile` | node:22-alpine 两阶段构建，产物 + static-server，`USER node` |
| `vite.config.ts` | dev 代理 `/api` → 本机 admin（`DDC_ADMIN_PROXY`）；vitest jsdom 环境 |
| `e2e/` | Playwright 冒烟场景 |
| `src/test/` | vitest 单测（迁移现有 config-format/uuid 用例为 TS，新增请求层与关键组件用例） |

## 数据流

```
浏览器 (React SPA)
  │  GET /  →  static-server  →  dist/index.html（同源，无 CORS）
  │  GET /api/v1/ddc/**  →  static-server 反代 →  admin:18080
  │     请求头: Authorization: Bearer <sessionStorage 里的 token>
  │
  admin 侧不变：JWT resource server 校验 → controller → service/repository
```

- 登录：粘贴 Bearer Token → 存 `sessionStorage`（不落 URL、不落服务端）→
  首次加载用 `GET /api/v1/ddc/registry/services` 探活并验证 token。
- dev 模式：vite dev 代理 `/api` 到本机 admin，不依赖 static-server。
- 静态缓存：index.html `no-cache`，带 hash 产物 `immutable`（复用 static-server 现成实现）。
- 不引入 CORS（除落地步骤第 6 条移除 `/ddc-admin` permitAll 外，后端逻辑零改动）。

## 错误处理

| 场景 | 处理 |
|---|---|
| HTTP 401 | 清 sessionStorage → 回登录页，提示"登录已过期，请重新粘贴 Access Token"（保留现状行为） |
| 非 2xx 或 `success === false` | 取 `payload.message \|\| payload.code`，antd `message.error`（后端 `ResultRecord` 格式不变） |
| 反代 502（admin 不可达） | static-server 返回 `{"code":"DDC_ADMIN_WEB_UPSTREAM_UNAVAILABLE"}`，UI 显示"无法连接 DDC 管理端" + 重试 |
| 发布等长操作 | publish 同步返回 `changeId + status`（保持）；发布任务页轮询 `publish-tasks` 刷新状态 |

## 测试与工程化

- TypeScript strict + ESLint（复用 gateway-admin-web 的 eslint 配置线）。
- Vitest + jsdom 单测：迁移 `config-format`/`uuid` 用例为 TS；新增请求层用例
  （401 清 token、`success===false` 错误解析）与关键页面组件用例。
- Playwright e2e 冒烟：登录 → 服务注册 → 配置发布；与 gateway 同约定——仅在有真实
  admin 可达时跑 `npm run e2e`，不作 CI 必过项。
- npm scripts：`dev / typecheck / test / lint / build / e2e`，对齐 gateway 命名。
- `package.json` version 对齐 `5.3.2`。

## 落地步骤

1. 脚手架：基于 gateway-admin-web 骨架复制改造（`@egon-cola/ddc-admin-web`，
   环境变量前缀 `DDC_ADMIN_*`）。
2. 请求层 + 登录态（sessionStorage 粘贴 token）。
3. 页面逐个落地：服务注册、配置管理+发布、应用、命名空间、发布任务、实例管理、
   缓存管理（对照 11 个 controller 的 API 面逐一核对，无遗漏）。
4. `static-server.mjs` + `Dockerfile`（DDC 前缀，`/healthz`）。
5. 单测、e2e、lint、typecheck 全绿。
6. 清理 admin 侧：
   - 删 `src/main/resources/static/ddc-admin/` 与 `src/test/js/`；
   - `DdcAdminWebResourceTest` 改为断言资源已移除、`/ddc-admin` 不可达；
   - security 配置移除 `/ddc-admin`、`/ddc-admin/**` 的 permitAll；
   - admin 版本 bump（5.3.2 → 5.4.0，属破坏性变化）。
7. 文档：DDC README 增加 admin-web 模块说明（构建、部署、环境变量）；
   `docs/manifest.md` 不动（本就指向外部 UI）。

## 验收标准

- 新旧功能对等（配置 CRUD/发布、服务注册/实例浏览）+ 新增发布任务/实例/缓存/
  应用/命名空间页可用。
- 新前端 vitest、e2e、lint、typecheck 全绿；admin 侧既有测试全绿。
- admin jar 不再含 `static/ddc-admin`，`/ddc-admin` 返回 404，`/api/v1/ddc/**` 不受影响。
