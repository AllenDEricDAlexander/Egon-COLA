# DDC Admin Web Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在 DDC Admin `18070` 服务中增加可查看服务注册并管理配置的同源网页。

**Architecture:** 新的 JWT Admin Controller 复用 `DdcManagementFacade` 查询 Redis 服务目录，不改变 HMAC OpenAPI。静态 HTML、JavaScript 和 CSS 由 Spring Boot 直接提供，调用现有配置 API 和新的只读注册中心 API。

**Tech Stack:** Java 21、Spring Boot MVC/Security、JUnit 5、MockMvc、原生 HTML/CSS/JavaScript。

## Global Constraints

- 在当前 `main` 分支 inline execution，不创建子代理。
- 不引入依赖，不增加或修改 Flyway migration。
- 保持 Admin JWT 与 OpenAPI HMAC 安全边界。
- 不使用 Docker；最终连接本机 Redis 和 PostgreSQL 验证。

---

### Task 1: JWT 服务注册查询接口

**Files:**
- Create: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-admin/src/main/java/top/egon/cola/component/ddc/admin/controller/DdcRegistryAdminController.java`
- Create: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-admin/src/test/java/top/egon/cola/component/ddc/admin/controller/DdcRegistryAdminControllerTest.java`
- Modify: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-admin/src/main/java/top/egon/cola/component/ddc/admin/security/DdcAdminSecurityConfiguration.java`
- Modify: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-admin/src/test/java/top/egon/cola/component/ddc/admin/security/DdcAdminSecurityIntegrationTest.java`

**Interfaces:**
- Consumes: `DdcManagementFacade.getServiceKeys(DdcManagementServiceQuery)` and `getInstances(DdcManagementServiceQuery)`.
- Produces: `GET /api/v1/ddc/registry/services` and `GET /api/v1/ddc/registry/instances`, both returning `ResultDto` management projections.

- [ ] Write controller tests for catalog and instance projections.
- [ ] Run the focused tests and confirm 404/failure because the controller is absent.
- [ ] Add the minimal controller and `DDC_READ` security matchers.
- [ ] Add security assertions for anonymous rejection and read-capability access.
- [ ] Run focused controller and security tests until green.

### Task 2: 同源静态管理页面

**Files:**
- Create: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-admin/src/main/resources/static/ddc-admin/index.html`
- Create: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-admin/src/main/resources/static/ddc-admin/app.js`
- Create: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-admin/src/main/resources/static/ddc-admin/styles.css`
- Create: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-admin/src/test/java/top/egon/cola/component/ddc/admin/web/DdcAdminWebResourceTest.java`
- Modify: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-admin/src/main/java/top/egon/cola/component/ddc/admin/security/DdcAdminSecurityConfiguration.java`

**Interfaces:**
- Consumes: Bearer JWT, `/api/v1/ddc/registry/**`, `/api/v1/ddc/configs`, `/api/v1/ddc/apps`, and `/api/v1/ddc/namespaces`.
- Produces: `GET /ddc-admin/index.html` with service registration and configuration management tabs.

- [ ] Write the classpath resource contract test and verify it fails because resources are absent.
- [ ] Add static resource authorization and the minimal page shell.
- [ ] Implement session-only login, service catalog/instance loading, application/namespace initialization, config list/create/update/publish, and error display.
- [ ] Run the resource and security tests until green.

### Task 3: 打包和本机运行验证

**Files:**
- Modify only runtime files under `target/local-dev-run` when needed to restart the current DDC process without exposing secrets.

**Interfaces:**
- Consumes: local PostgreSQL `egon_local_ddc`, authenticated Redis `127.0.0.1:6379`, and the current generated JWT.
- Produces: live page `http://127.0.0.1:18070/ddc-admin/index.html`.

- [ ] Run the full DDC Admin test suite and build the executable JAR.
- [ ] Restart only DDC Admin with its existing runtime credentials and verify all other component processes remain alive.
- [ ] Verify the page and assets return 200, anonymous registry access returns 401, JWT registry queries return live HTTP/RPC/Gateway services, and config create/update/publish is persisted.
- [ ] Commit only the scoped source, tests, resources, design, and plan files; preserve unrelated untracked files.
