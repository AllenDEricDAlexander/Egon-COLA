# Gateway Admin Scope And Observability Fix Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 让 Gateway Admin Web 使用部署配置的默认作用域，并让成功调用和路由未命中的 curl 都能在当前作用域自动展示。

**Architecture:** 保持 Gateway Admin 与 DDC 的现有投影接口不变，在 Web 边界增加一个纯函数解析 Vite 默认作用域，并让作用域选择器包含该部署值。调用观测继续使用 React Query 定时重新获取；Engine 在路由匹配前先给事件写入自身作用域，匹配成功后仍由路由上游作用域覆盖，因此未命中事件也能被作用域查询看到。

**Tech Stack:** Java 21、Spring Boot、Reactor、JUnit 5、React 19、TypeScript 6、Ant Design、TanStack Query、Vitest、Testing Library、Vite。

## Global Constraints

- 在当前 `main` 分支内联执行，不创建子代理或 worktree。
- 不使用 Docker，不修改 PostgreSQL、Redis 或现有 DDC/Gateway 数据。
- 保留用户已有未跟踪文件，只提交本任务文件。
- 直接实现足够；Strategy、Adapter 等设计模式不会降低这里的简单配置与轮询复杂度，因此不引入新抽象层。

---

### Task 1: 修复部署作用域与调用观测刷新

**Files:**
- Create: `egon-cola-components/egon-cola-component-gateway/egon-cola-component-gateway-admin-web/src/hooks/scopeDefaults.ts`
- Create: `egon-cola-components/egon-cola-component-gateway/egon-cola-component-gateway-admin-web/src/hooks/scopeDefaults.test.ts`
- Create: `egon-cola-components/egon-cola-component-gateway/egon-cola-component-gateway-admin-web/src/features/observability/TracesPage.test.tsx`
- Modify: `egon-cola-components/egon-cola-component-gateway/egon-cola-component-gateway-admin-web/src/hooks/useScope.tsx`
- Modify: `egon-cola-components/egon-cola-component-gateway/egon-cola-component-gateway-admin-web/src/layouts/AdminLayout.tsx`
- Modify: `egon-cola-components/egon-cola-component-gateway/egon-cola-component-gateway-admin-web/src/features/observability/TracesPage.tsx`
- Modify: `egon-cola-components/egon-cola-component-gateway/egon-cola-component-gateway-admin-web/README.zh-CN.md`
- Modify: `egon-cola-components/egon-cola-component-gateway/egon-cola-component-gateway-admin-web/README.md`

**Interfaces:**
- Consumes: `VITE_GATEWAY_ADMIN_DEFAULT_ENV`、`VITE_GATEWAY_ADMIN_DEFAULT_NAMESPACE`、现有 `Scope` 与 `gatewayApi.traces`。
- Produces: `resolveInitialScope(configuredEnv, configuredNamespace): Scope` 与 `scopeOptions(current, configured, defaults): SelectOption[]`；调用观测每 5 秒重新获取当前作用域。

- [ ] **Step 1: 写默认作用域失败测试**

  验证配置值会去除空白、空配置回退为 `dev/default`，且 `codex-local` 会进入可选命名空间列表。

- [ ] **Step 2: 运行默认作用域测试并确认 RED**

  Run: `npm test -- --run src/hooks/scopeDefaults.test.ts`

  Expected: FAIL because `scopeDefaults.ts` does not exist.

- [ ] **Step 3: 写调用观测自动刷新失败测试**

  用真实 `TracesPage`、`QueryClientProvider` 与 `ScopeProvider`，只替换远端 `gatewayApi.traces`，验证首屏空数据后定时获取会显示新增 trace。

- [ ] **Step 4: 运行页面测试并确认 RED**

  Run: `npm test -- --run src/features/observability/TracesPage.test.tsx`

  Expected: FAIL because the query has no refresh interval.

- [ ] **Step 5: 实现最小修复**

  从 Vite 环境解析初始作用域；作用域下拉项包含配置值和当前值；给调用观测查询增加 5 秒轮询。文档只补充两个非敏感部署变量。

- [ ] **Step 6: 运行前端验证并确认 GREEN**

  Run: `npm test -- --run src/hooks/scopeDefaults.test.ts src/features/observability/TracesPage.test.tsx`

  Run: `npm run typecheck && npm run lint && npm run build`

- [ ] **Step 7: 提交修复**

  Run: `git add <本任务文件> && git commit -m "fix: align gateway admin runtime scope"`

### Task 2: 给路由未命中事件补齐 Engine 作用域

**Files:**
- Modify: `egon-cola-components/egon-cola-component-gateway/egon-cola-component-gateway-engine/src/main/java/top/egon/cola/component/gateway/engine/http/DefaultGatewayHttpDataPlaneHandler.java`
- Modify: `egon-cola-components/egon-cola-component-gateway/egon-cola-component-gateway-engine/src/main/java/top/egon/cola/component/gateway/engine/GatewayEngineConfiguration.java`
- Modify: `egon-cola-components/egon-cola-component-gateway/egon-cola-component-gateway-engine/src/test/java/top/egon/cola/component/gateway/engine/http/DefaultGatewayHttpDataPlaneHandlerTraceTest.java`

**Interfaces:**
- Consumes: `GatewayEngineRuntimeProperties.env`、`GatewayEngineRuntimeProperties.namespace` 和现有 `GatewayCallObservation.scope`。
- Produces: 未匹配路由的 `GatewayCallEventV1.routing.env/namespace` 使用 Engine 配置值；匹配路由仍使用发布规则中的上游作用域。

- [ ] **Step 1: 扩展现有路由未命中测试并确认 RED**

  断言 `GATEWAY_ROUTE_NOT_FOUND` 事件携带 `dev/codex-local`；先运行测试确认构造器或作用域断言失败。

- [ ] **Step 2: 实现最小作用域注入**

  Handler 创建 Observation 后立即写入 Engine 作用域；Spring 配置传入现有 `properties.getEnv()` 与 `properties.getNamespace()`，不改变匹配路由覆盖逻辑。

- [ ] **Step 3: 运行 Engine 验证并提交**

  Run: `./mvnw -B -ntp -f egon-cola-components/egon-cola-component-gateway/egon-cola-component-gateway-engine/pom.xml test`

  Run: `git add <本任务文件> && git commit -m "fix: scope unmatched gateway traffic"`

### Task 3: 装载并验证本机修复

**Files:**
- Modify locally only: `target/local-dev-run/start-local-stack.zsh`

**Interfaces:**
- Consumes: 当前本机 Gateway Admin API、DDC、PostgreSQL、Redis、Kafka 与已签发 Admin Token。
- Produces: 以 `dev/codex-local` 为默认作用域运行的 Gateway Admin Web。

- [ ] **Step 1: 给本机 Vite 启动参数增加默认作用域**

  设置 `VITE_GATEWAY_ADMIN_DEFAULT_ENV=dev` 与 `VITE_GATEWAY_ADMIN_DEFAULT_NAMESPACE=codex-local`，不提交 `target/` 运行文件。

- [ ] **Step 2: 重启 Gateway Admin Web 与 Gateway Engine**

  保持 DDC、Gateway Admin、Provider、Consumer 与数据不变；Engine 使用原环境变量和数据目录重启。

- [ ] **Step 3: 验证 Provider 与流量闭环**

  验证 Web 为 200，Gateway Provider API 在 `dev/codex-local` 返回 HTTP/RPC Provider；成功路由和 `GATEWAY_ROUTE_NOT_FOUND` curl 都在该作用域出现新增调用记录。

- [ ] **Step 4: 最终检查**

  运行 `git diff --check`、确认服务健康，并确认工作区只保留用户原有未跟踪文件。
