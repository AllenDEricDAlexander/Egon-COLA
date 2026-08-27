# RBAC3 资源授权、控制面分层与四端左树布局实施计划

| Field | Value |
| --- | --- |
| Document | `2026-08-25-10-19-rbac3-resource-grants-layering-implementation.md` |
| Template Version | `2` |
| Status | `Review` |
| Created | `2026-08-25 10:19 CST` |
| Updated | `2026-08-25 10:42 CST` |
| Owner | `Mario / Egon-COLA` |
| Repository | `Egon-COLA` |
| Scope | `RBAC3 Contract/Core/Starter/Admin/React SDK/Admin Web；RBAC3 PostgreSQL V13；Admin Web Shared 0.2.0；DDC/Gateway/IdP/RBAC3 四个 Admin Web` |
| Source Requirement | `2026-08-25 用户确认最新 RBAC3 资源授权与四端左侧树规格，要求开始编写实施 Plan` |
| Baseline Revision | `main@df425ed9484d20e6235666d91e947742998ab475；目标 Spec 及若干无关 Spec/Plan 为用户未提交文档，实施时必须路径隔离` |
| Implements Spec | [RBAC3 资源授权、权限字符内隐化与控制面重新分层规格](../spec/2026-08-24-17-30-rbac3-resource-grants-layering.md) |
| Spec Status | `Accepted` |
| Spec Revision | `Updated 2026-08-25 10:42 CST；baseline main@df425ed9484d20e6235666d91e947742998ab475` |
| Effective Specs | [本次资源授权分层规格](../spec/2026-08-24-17-30-rbac3-resource-grants-layering.md)；[前置注解/UserDetails/字段授权规格](../spec/2026-08-17-09-37-rbac3-annotation-userdetails-field-authorization.md)；[统一身份无 Session JWT 规格](../../superpowers/specs/2026-08-13-unified-identity-stateless-jwt-session-removal-design.md)；[IAM 聚合迁移旧规格](../../../egon-cola-platforms/egon-cola-platform-rbac3/docs/iam-package-aggregation-migration-spec.md) |
| Depends On Plans | [RBAC3 注解权限、全局资源目录与无状态认证实施计划](2026-08-17-15-07-rbac3-annotation-resource-catalog-implementation.md) |
| Supersedes | `None` |
| Superseded By | `None` |
| Related Plans | `None` |

## 1. Summary

本计划实现已接受的资源授权分层规格：用 `RoleResourceGrant` 替换角色直接绑定权限字符，以 `Resource.requiredPermissionId` 作为唯一实际映射，以 `ResourceApiBinding` 表达 ROUTE/ACTION 调用 API，并把有效资源、权限字符、About、Method Security 和前端展示串成一条可验证链。与此同时，RBAC3 Admin 按六类 IAM 基础对象、授权控制面、运行时与 CI 注册重新分包；共享 Admin Web 升级为 `0.2.0`，DDC、IdP、Gateway、RBAC3 四端把业务树移到左侧 Sider，移动端复用左 Drawer。

实施拆为 14 个顺序 Step。先固定跨模块契约和 Admin 包边界，再完成资源授权数据模型、管理 API、CI 注册、运行时投影与唯一 V13；随后迁移 React SDK、RBAC3 管理页面和共享壳，最后逐端升级四个消费者。完成证据由 `TEST-001`–`TEST-042` 对应的 JUnit/MockMvc/PostgreSQL、Vitest/Node、静态残留扫描、npm clean-install/build 与用户控制的 Playwright/真实部署检查组成。本计划不改 AT/RT、Session 已移除模型、IdP/DDC 主数据归属，也不启动项目。

## 2. Target Spec and Effective Design

### 2.1 Primary target

- Path: [docs/egon/spec/2026-08-24-17-30-rbac3-resource-grants-layering.md](../spec/2026-08-24-17-30-rbac3-resource-grants-layering.md)
- Status: `Accepted`
- Revision: `Updated 2026-08-25 10:42 CST；main@df425ed9484d20e6235666d91e947742998ab475`
- Approval evidence: 用户在 2026-08-25 明确回复“确认”并点名 `egon-coding-writing-plan`；本轮只授权编写 Plan，不授权执行代码。

### 2.2 Effective Spec set

| Role | Spec/link | Status/revision | Effective sections | Why included |
| --- | --- | --- | --- | --- |
| Primary | [资源授权分层规格](../spec/2026-08-24-17-30-rbac3-resource-grants-layering.md) | Accepted / 2026-08-25 10:42 CST | 全文 | 本计划唯一主目标；定义 REQ-001–REQ-025、接口、V13、页面、包和发布边界 |
| Normative dependency | [注解/UserDetails/字段授权规格](../spec/2026-08-17-09-37-rbac3-annotation-userdetails-field-authorization.md) | Accepted / 2026-08-17 15:07 CST | §3.2、§5.1、§7.1–§7.3.2、§7.3.4、§15–§16 未被主规格修订部分 | 保留 SecurityContext、UserDetails、active role、字段/DataScope/SOD/Fence、无 Session 与 common Result 规则 |
| Transitive dependency | [统一身份无 Session JWT 规格](../../superpowers/specs/2026-08-13-unified-identity-stateless-jwt-session-removal-design.md) | 已确认 / 2026-08-14 | §7–§9、§11、§12.1、§12.5、§15，经前置 Accepted Spec 引入 | 固定双 Token、Cookie、IdP 验签、业务服务只接 AT、角色激活无 Session；本计划只做回归，不重写 |
| Amended legacy baseline | [IAM 聚合迁移旧规格](../../../egon-cola-platforms/egon-cola-platform-rbac3/docs/iam-package-aggregation-migration-spec.md) | Draft legacy；仅由两个 Accepted Spec 明确引用的未冲突内容有效 | §2.2–§2.4、§4.1、§5.1–§5.4；其 §3、§4.2–§4.4、§5.5–§9 被主规格修订 | 保留最小 User、DDC BIZ/APP、组织岗位来源、tenant/application 隔离等既有事实；不把旧目标包/Manifest/RolePermission 带回 |

### 2.3 Superseded or excluded content

- 前置 Spec 中角色直接绑定 `RolePermission`、CI 写实际权限映射、前端按 `permissions` 显示 MENU/ROUTE/ACTION、顶部 Header 桌面导航等内容，由主 Spec 明确修订。
- IAM 旧 Spec 中 `iam.resource/permission/policy`、`role.assignment/activation/inheritance`、Manifest 所有权、旧 URI 和把 Tenant 视为 RBAC 基础实体的目标不再有效。
- IdP OAuth Client 规格只是 Related Spec；它不改变本计划文件或执行顺序。
- 既有 2026-08-17 实施 Plan 是已落地基线，不在本计划重复 JWT、RT 在线态、UserDetails、字段 Jackson PEP、DDC 全局编码或 Manifest 删除工作。

## 3. Effective Requirements and Acceptance

| Requirement | Source Spec section | Effective statement | Observable acceptance | Implementation impact |
| --- | --- | --- | --- | --- |
| `REQ-001` | 主 Spec §4、API-001/002 | 角色提交资源 ID，不提交 permission ID/code | 请求与角色页均无权限字符输入 | Grant DTO/API/PO/UI |
| `REQ-002` | 主 Spec §4、§12 | 角色页按菜单页面树、按钮和独立 API 展示 | MENU 仅分组，ROUTE/ACTION/独立 API 可读配置 | 树 VO、组件测试 |
| `REQ-003` | 主 Spec API-003/004 | 管理员在资源目录确认实际映射 | 显示 suggestion/actual/in-use，角色页不显示字符 | Mapping Service/Drawer |
| `REQ-004` | 主 Spec §11 | `resource.required_permission_id` 是唯一映射 | 无 permission_resource 表/PO/调用方 | V13、静态扫描 |
| `REQ-005` | 主 Spec §7.3.3 | active/effective role 的资源授权派生权限 | 未激活角色、无效资源/映射不进入快照 | Core facts、JPA projector |
| `REQ-006` | 主 Spec INTERNAL-001 | API 注解检查资源和权限；通用注解只检查权限 | 任一 API 条件缺失拒绝，通用能力兼容 | Contract/Starter |
| `REQ-007` | 主 Spec API-005 | About 暴露排序 resourceCodes，前端据此显示资源 | MENU/ROUTE/ACTION guard 不读 permissions | About/SDK/Web |
| `REQ-008` | 主 Spec §4、§14 | FIELD、DataScope、SOD、Fence 继续 fail closed | 原回归全部通过，FIELD 不进 RoleResourceGrant | Starter/Core/Admin regression |
| `REQ-009` | 主 Spec API-006 | CI 只写机械事实和 suggestedPermissionCode | actual mapping、role grant 零变化 | Registration store/Node script |
| `REQ-010` | 主 Spec §8、API-006 | CI 能力归 `registration.ci` 和唯一新 URL | 无旧 report 包/URL，脚本仍 CI-only | 包迁移、Controller、bundle guard |
| `REQ-011` | 主 Spec §7.3.4、§8 | `admin.iam` 仅六个顶层业务包 | 精确为 user/role/business/application/organization/position | 包迁移、ArchUnit |
| `REQ-012` | 主 Spec §7.1、§8 | resource/permission/grant/policy/runtime/simulation 明确分层 | 架构测试禁止旧依赖方向 | Admin 生产/测试包迁移 |
| `REQ-013` | 主 Spec §8、§16 | 旧模型、principal、envelope、空 tenant 壳直接删除/归位 | 无旧类、表、URL、package-info 壳 | cleanup/static gate |
| `REQ-014` | 主 Spec API-002、§7.3.5 | role-resource replace 原子、隔离并暴露冲突 | 409/422/故障均零部分写 | JPA transaction/concurrency |
| `REQ-015` | 主 Spec API-004 | 在用资源不得静默换实际权限 | direct 或反向 API 来源在用时不同映射返回 409 | Mapping reverse query |
| `REQ-016` | 主 Spec §9 | 新增/替换 HTTP 使用完整 ResultRecord | 成功/失败 JSON 契约一致 | Controllers/MockMvc/clients |
| `REQ-017` | 主 Spec §11、§16 | V13 破坏式切换，不迁旧授权和 alias | 旧表/URL 不存在，内置管理员有新 grant | Migration/bootstrap |
| `REQ-018` | 主 Spec §7.3.5 | grant/mapping/binding 变更推进版本并失效快照 | 新快照可见；传播异常 fail closed | publisher/outbox/cache regression |
| `REQ-019` | 主 Spec §7.3.3 | 页面/按钮关联 API 做 OR 并集展开 | 任一有效来源或直授即可获得共享 API | Binding table/runtime/UI |
| `REQ-020` | 主 Spec API-006、§11 | Binding source 仅 ROUTE/ACTION，target 仅同 APP API | 非法整批 422 且零写 | Registration validation/FK |
| `REQ-021` | 主 Spec §7.3.3 | ROUTE 基础 API 与 ACTION 操作 API 不混淆 | 仅 ROUTE grant 不获得 ACTION-only API | CI declarations/runtime tests |
| `REQ-022` | 主 Spec INTERNAL-002、§12 | desktop 主导航只在左 Sider 递归树 | Header 无 horizontal Menu；Sider inline tree | Shared layout |
| `REQ-023` | 主 Spec INTERNAL-002、§12 | Header 只做 Banner，mobile 仅提供打开导航触发器 | 顶部只有品牌/操作/用户/窄屏按钮 | Shared header/sidebar |
| `REQ-024` | 主 Spec §12、§16 | DDC/IdP/Gateway/RBAC3 四端协调升级 shared 0.2.0 | 四端都解析 0.2.0 并有左树 | Consumer trees/package locks |
| `REQ-025` | 主 Spec §12.3–§12.5 | 折叠、deep-link、最长边界匹配、移动 Drawer 完整 | 页面不 remount、祖先展开、Esc/focus 可用 | Shared/consumer tests |
| `REQ-026` | 前置 Accepted Spec §4/§10/§11，经主 Spec 保留 | 全局资源目录与 tenant 授权事实仍分离 | catalog/binding 无 tenant；grant 保持 tenant/app 校验 | V13/JPA/回归；不新增业务接口 |

## 4. Implementation Strategy and Dependency Order

### 4.1 Ordered strategy

1. 先扩展 `PermissionRequest` 与 About 契约，使 Starter 和 React 后续都有稳定编译目标；不触碰 JWT Filter。
2. 在 Admin 行为变更前完成纯包迁移和架构 RED/GREEN，使所有新代码直接写入最终 `authorization`、`registration`、`shared.tenant` 路径。
3. 先增加两个新持久模型和 repository port，再分别完成角色资源、资源映射、CI 注册三个控制面用例；三者都只依赖现有 Application/Resource/Permission/UserDetails 边界。
4. 等所有旧 `RolePermission/PermissionResource` 调用已替换后，一次性加入唯一 V13、改运行时事实/Bootstrap、删除旧 PO/DTO/表，并运行 PostgreSQL/Flyway 与完整 Admin 回归。中间提交只用于编译/单元验证，不可独立部署。
5. 后端契约稳定后切 React SDK 和 RBAC3 管理页面；浏览器运行展示只用 resourceCodes，CI 脚本仍位于 `scripts/`。
6. 共享壳先完成 `0.2.0` 的测试、构建和发布，再分别更新 DDC、IdP、Gateway、RBAC3 消费者和各自 lockfile。四端认证/bootstrap/route URL 保持各自所有权。

### 4.2 Test-first strategy

| Behavior | RED test first | Expected RED reason | Minimum GREEN |
| --- | --- | --- | --- |
| API 双条件与 About resourceCodes | Contract/Starter focused tests | record/factory/字段和资源检查不存在 | 扩展原类型与原 service/manager |
| Admin 目标包 | ArchUnit tests | 旧 IAM/runtime/report 根仍存在 | 机械 `git mv` + imports，不改行为 |
| RoleResourceGrant/Binding model | PO/repository contract tests | 类、约束映射、repository port 不存在 | 两个实体/枚举/port |
| Role resource API | MockMvc/service/persistence tests | URL、DTO、事务 replace 不存在 | Controller + Service + JPA repository |
| Mapping API | MockMvc/service tests | mapping view/update/in-use guard 不存在 | 复用 ResourcePO.requiredPermissionId |
| CI registration | Node/controller/store tests | 旧 permission 字段/URL/report package 仍生效 | registration rename、suggestion/binding replace |
| Runtime/V13 | Core/Admin/migration tests | facts 仍读 role_permission；schema 无新表 | union projector、bootstrap、新迁移、旧模型删除 |
| SDK/Admin Web | Vitest tests | navigation/action 仍读取 permissions；旧角色文本表单 | resourceCodes guard + resource tree UI |
| Shared shell | shared component tests | Header 仍有 horizontal Menu；无 Sider | internal Sidebar + Layout-owned state |
| Four consumers | each layout/app test | flat/group navigation 或旧依赖 | 本地 children tree + 0.2.0 lock |

### 4.3 Sequential and parallel boundaries

| Step | Depends on | May run in parallel with | Must not overlap with | Reason |
| --- | --- | --- | --- | --- |
| Step 1 | None | Step 10 | Contract/Starter files | 先固定公共 Java 契约 |
| Step 2 | Step 1 | None | 全部 RBAC3 Admin Java/test package | 单次机械重分层，避免双路径 |
| Step 3 | Step 2 | None | 新 grant/binding model 与 ResourcePO | 后续三个服务共同基础 |
| Step 4 | Step 3 | Step 5 的只读设计检查 | role/grant 文件 | 角色资源垂直链 |
| Step 5 | Step 3 | None | resource/permission/binding repository | mapping 与 CI 都访问 ResourcePO，顺序实施 |
| Step 6 | Steps 3、5 | None | registration/resource store 和 CI scripts | 注册维护 binding/suggestion |
| Step 7 | Steps 4–6 | None | runtime/bootstrap/migration/legacy files | 唯一破坏式后端切换点 |
| Step 8 | Steps 1、7 | Step 10 | React SDK files | About/运行时字段已经稳定 |
| Step 9 | Steps 4–8 | Step 10 | RBAC3 feature/routes files | 后端和 SDK 契约均稳定 |
| Step 10 | None | Steps 1–9 | shared package files | 独立公共布局；发布后解锁消费者 |
| Steps 11–14 | Step 10 已构建并发布 0.2.0 | 相互可并行但默认按序提交 | 各自 Admin Web 与 lockfile，不交叉 | 四端写范围独立；RBAC3 还依赖 Step 9 |

### 4.4 Commit boundaries

每个 Step 一个语义提交，使用 `codex/` 工作分支或用户指定分支；只暂存 Step 的 `Commit paths`。Step 2 虽文件多，但只有“无行为的包归属迁移”一个语义，不能与资源授权行为混在同一提交。Step 7 是唯一允许同时出现 SQL、runtime、bootstrap 和删除旧模型的提交，因为拆开会产生旧二进制与 V13 表不一致的不可部署状态。npm publish 是 Step 10 提交通过后、Step 11 前的受控发布动作，不产生额外源码提交。

### 4.5 Spec Simplicity and Implementation-necessity Audit

| Spec element | Spec necessity verdict/section | Current repository evidence | Direct/reuse alternative | Interaction/implementation cost | Plan decision |
| --- | --- | --- | --- | --- | --- |
| RoleResourceGrant table/API | Add / 主 Spec §7.0、API-001/002 | `RolePermissionPO` 与 permission-ID UI 错位 | 只改标签仍持久化错误主语 | 一表、两接口、一次 replace 事务 | Implement |
| ResourceApiBinding table | Add / §7.0、REQ-019–021 | 当前无页面/按钮调用 API 关系 | JSON 列无 FK/反向查询/完整 replace | 一表，无新网络调用 | Implement |
| Resource mapping API | Add / API-003/004 | generic Resource CRUD 缺 suggestion/in-use 保护 | 直接暴露 requiredPermissionId 会绕过业务校验 | 一 GET/PUT 和 Drawer | Implement |
| About.resourceCodes | Add / API-005 | `AppAuthorizationContext.resourceCodes` 已存在 | 资源树接口不必要 | 一个字段，无额外 RTT | Reuse existing snapshot / implement projection |
| 新授权引擎/缓存 | Reject / §7.0 | `AuthorizationService` 与 snapshot cache 已稳定 | 原服务做两个 set membership | 无新增状态 | Do not create |
| CI registration rename | Move / API-006 | `iam.resource.report` 当前写 actual mapping | 保留旧名会继续混淆所有权 | URL/package breaking rename，无 startup job | Implement |
| Legacy/compatibility package | Reject / §7.0 | 用户允许破坏式更新 | alias 会维持两套模型 | 长期维护成本 | Do not create |
| EnterpriseSidebar | Add internal / INTERNAL-002 | 四端共同依赖 EnterpriseLayout；Header 当前水平菜单 | 四端复制会重复 openKeys/Drawer/deep-link | 一个内部组件，三个本地 UI state | Implement |
| 新导航 Provider/store/API | Reject / INTERNAL-002 | navigation 已由各 AdminLayout 本地提供 | Layout local state 直接满足 | 零服务器状态 | Do not create |
| Fetch-then-forward/context 参数 | No defect | role tree 返回人类选择和 version；PUT 仍权威重验；tenant/app/actor 均服务端派生 | 无多余 preflight/透传参数 | GET 是独立 UI 读取价值 | Implement as specified |
| 设计模式 | Existing Facade/Repository/Adapter；不新增通用 Strategy/Factory | 当前 Controller→Facade→Repository 和 DDC adapter 已适合 | 直接服务方法比新 handler 链清楚 | 仅 binding 集合并集算法 | Keep repository style; no speculative pattern |

### 4.6 Change-unit Dependency Matrix

| Change unit | Requirements | Proof/RED point | Compile/runtime prerequisites | Produces | Consumers/unblocks | Owning Step |
| --- | --- | --- | --- | --- | --- | --- |
| PermissionRequest/About | `REQ-006`,`007`,`008`,`016` | `TEST-017`,`019`–`022` | existing Contract/Starter | API/generic request + resourceCodes view | SDK/runtime | Step 1 |
| Admin package boundaries | `REQ-011`–`013` | `TEST-027`,`029` | current imports compile | final package roots | all Admin changes | Step 2 |
| Grant/binding domain | `REQ-001`,`004`,`019`,`020`,`026` | entity/port focused tests | Step 2 | PO/enums/ports/suggestion field | APIs/runtime/V13 | Step 3 |
| Role resource control plane | `REQ-001`,`002`,`014`,`016`,`018`,`019`,`021` | `TEST-001`–`006`,`030`–`032` backend part | Step 3 | API-001/002 | runtime/UI | Step 4 |
| Mapping control plane | `REQ-003`,`004`,`015`,`016`,`018`,`019` | `TEST-007`–`010` | Step 3 | API-003/004 | role grantability/CI | Step 5 |
| CI registration | `REQ-009`,`010`,`016`,`020`,`021` | `TEST-011`–`014`,`030` | Steps 3、5 | API-006 + binding replace | runtime/V13 | Step 6 |
| Runtime and V13 cutover | `REQ-004`,`005`,`008`,`013`,`017`–`021`,`026` | `TEST-023`–`032` | Steps 4–6 | new facts/schema/bootstrap | SDK/Web/release | Step 7 |
| React resource guard | `REQ-007`,`008`,`019` | `TEST-015`,`016`,`031`,`032` | Steps 1、7 | resourceCodes UI runtime | RBAC3 Web | Step 8 |
| RBAC3 management UI | `REQ-001`–`003`,`007`,`014`–`016`,`019`–`021` | `TEST-001`,`002`,`018` | Steps 4–8 | resource tree/mapping pages | RBAC consumer | Step 9 |
| Shared shell 0.2.0 | `REQ-022`–`025` | `TEST-033`–`036` | existing AntD/Router | Banner+Sider/Drawer contract | four consumers | Step 10 |
| DDC/IdP/Gateway/RBAC3 trees | `REQ-007`,`022`–`025` | `TEST-037`–`042` | published shared 0.2.0 | local tree definitions/locks | release | Steps 11–14 |

## 5. Change File Tree

```text
egon-cola-platforms/
├── egon-cola-platform-rbac3/
│   ├── egon-cola-platform-rbac3-contract/                         MODIFY PermissionRequest, Rbac3AboutView, tests
│   ├── egon-cola-platform-rbac3-core/                             MODIFY ResourceGrantBinding union facts/tests
│   ├── egon-cola-platform-rbac3-starter/                          MODIFY API double-check/About/tests
│   ├── egon-cola-platform-rbac3-admin/
│   │   ├── src/main/java/.../admin/
│   │   │   ├── iam/{user,role,business,application,organization,position}/   KEEP/SPLIT
│   │   │   ├── authorization/{resource,permission,grant,policy,runtime,simulation}/ RENAME/CREATE
│   │   │   ├── registration/ci/                                  RENAME/MODIFY
│   │   │   └── shared/tenant/                                    RENAME
│   │   └── src/main/resources/db/migration/V13__replace_role_permissions_and_add_resource_api_bindings.sql CREATE
│   ├── egon-cola-platform-rbac3-react-sdk/src/                    MODIFY resourceCodes guards
│   ├── egon-cola-platform-rbac3-admin-web/src/                    MODIFY role/resource/routes/navigation
│   └── package-lock.json                                          MODIFY shared 0.2.0
├── egon-cola-platform-admin-web-shared/                           MODIFY/PUBLISH 0.2.0
├── egon-cola-platform-dynamic-config-center/...-admin-web/        MODIFY DDC tree/package lock
├── egon-cola-platform-idp/...-admin-web/                          MODIFY IdP tree/package lock
└── egon-cola-platform-gateway/...-admin-web/                      MODIFY Gateway tree/package lock
```

| Operation | Path | Current evidence/symbol | Final symbols/state | Responsibility | Step | Requirements | Validation owner |
| --- | --- | --- | --- | --- | --- | --- | --- |
| MODIFY | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-contract/src/main/java/top/egon/cola/platform/rbac3/contract/authorization/PermissionRequest.java` | one-field record + `of` | optional `resourceCode` + `api` factory | internal decision request | 1 | `REQ-006` | Contract/Starter tests |
| MODIFY | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-contract/src/main/java/top/egon/cola/platform/rbac3/contract/auth/Rbac3AboutView.java` | no resourceCodes | sorted immutable resourceCodes | About wire contract | 1 | `REQ-007` | serialization test |
| MODIFY | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-contract/src/main/java/top/egon/cola/platform/rbac3/contract/authorization/SystemAuthorizationSnapshot.java` | Starter runtime snapshot has permissions but no resourceCodes | same-snapshot sorted resourceCodes | PEP/About runtime carrier | 1、7 | `REQ-005`–`007` | Contract/Starter/runtime tests |
| MODIFY | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-starter/src/main/java/top/egon/cola/platform/rbac3/starter/{authorization,security}/**` | permission-only/API code ignored | API double-check + About projection | Method Security/PEP | 1 | `REQ-006`–`008` | Starter tests |
| RENAME | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/{main,test}/java/top/egon/cola/platform/rbac3/admin/{iam,authorization,management,participation,runtime,simulation}/**` | mixed ownership in 16 roots | exact target roots from Spec §8.2 | mechanical package ownership | 2 | `REQ-011`–`013` | ArchUnit/compile |
| RENAME | `.../admin/iam/tenant/{controller/filter,domain,service}/**` | tenant top-level IAM shell | `.../admin/shared/tenant/**`; empty package-info deleted | trusted tenant context | 2 | `REQ-011`,`013` | ArchUnit/rg |
| CREATE | `.../admin/authorization/grant/roleresource/{domain,repository,service,controller}/**` | absent | RoleResourceGrant complete vertical slice | role-resource control plane | 3–4 | `REQ-001`,`002`,`014` | Admin tests |
| CREATE | `.../admin/authorization/resource/apibinding/{domain,repository}/**` | absent | ResourceApiBinding PO/port/JPA support | page/button API relation | 3、6 | `REQ-019`–`021` | persistence/registration tests |
| MODIFY | `.../admin/authorization/resource/domain/po/ResourcePO.java` | actual mapping only | nullable suggestion + actual unchanged | catalog entity | 3 | `REQ-003`,`009` | entity/migration tests |
| CREATE | `.../admin/authorization/permission/{controller,domain,repository,service}/ResourcePermissionMapping*` | absent | API-003/004 mapping vertical slice | actual mapping administration | 5 | `REQ-003`,`015`,`016` | MockMvc/JPA tests |
| RENAME | `.../admin/iam/resource/report/**` | `CiResourceReport*` old package/URL | `.../admin/registration/ci/**` + `CiResourceRegistration*` | SERVICE registration | 2、6 | `REQ-009`,`010` | Node/MockMvc/IT |
| CREATE | `.../admin/src/main/resources/db/migration/V13__replace_role_permissions_and_add_resource_api_bindings.sql` | V12 latest | new tables/suggestion, old tables dropped | destructive schema cutover | 7 | `REQ-004`,`017`,`020` | PostgreSQL/Flyway IT |
| MODIFY | `.../egon-cola-platform-rbac3-core/src/main/java/top/egon/cola/platform/rbac3/core/{activation,decision}/**` | PermissionBinding from role_permission | ResourceGrantBinding + union permissions/resources | pure authorization projection | 7 | `REQ-005`,`019` | Core/Admin tests |
| MODIFY | `.../admin/authorization/runtime/**` and `.../admin/bootstrap/**` | old RolePermission reads/bootstrap | grant/binding reads and built-in resource grants | runtime/bootstrap | 7 | `REQ-005`,`017`,`018` | IT/CLI smoke |
| DELETE | `.../admin/**/{RolePermissionPO,RolePermissionStatusEnum,AssignPermissionCommandDTO,AssignPermissionsCommandDTO,RemovePermissionCommandDTO,BindPermissionsRequestDTO,PermissionResourcePO}.java` | old/duplicate model | absent | destructive cleanup | 7 | `REQ-004`,`013`,`017` | rg/compile/migration |
| MODIFY | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-react-sdk/src/{types.ts,registry/FrontendResourceRegistry.ts,guards/ActionGuard.tsx,index.ts}` | UI resource definitions require permission | MENU/ROUTE/ACTION checks use resourceCodes; generic PermissionGuard retained | browser authorization API | 8 | `REQ-007`,`008` | Vitest/typecheck |
| MODIFY/RENAME | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin-web/src/features/{role,application}/**` | comma IDs + standalone permission page | RoleResourceGrantPage + mapping Drawer/selector | RBAC control UI | 9 | `REQ-001`–`003` | Vitest/build |
| MODIFY | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin-web/src/{app,features}/**` | role-permissions route + permission-based registry | role-resources route + resourceCodes | RBAC routes/navigation | 9、14 | `REQ-007`,`013` | App integration |
| MODIFY | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin-web/scripts/{report-rbac-resources.mjs,report-rbac-resources.test.mjs,verify-browser-bundle.mjs,verify-rbac3-conformance.mjs}` | permission field/old URL | suggestion/apiResourceCodes/new URL/guards | CI-only producer/static gate | 6、7 | `REQ-009`,`010`,`013` | Node scripts |
| CREATE | `egon-cola-platforms/egon-cola-platform-admin-web-shared/src/layout/EnterpriseSidebar.tsx` | absent | internal desktop Sider/mobile Drawer | shared navigation renderer | 10 | `REQ-022`–`025` | shared component tests |
| MODIFY | `egon-cola-platforms/egon-cola-platform-admin-web-shared/src/layout/{types.ts,EnterpriseHeader.tsx,EnterpriseLayout.tsx,EnterpriseLayout.test.tsx}` | group + Header navigation | activePathPrefixes + Banner + Layout state | shared public shell | 10 | `REQ-022`–`025` | Vitest/build |
| MODIFY | `egon-cola-platforms/egon-cola-platform-admin-web-shared/{package.json,package-lock.json}` | 0.1.4 | 0.2.0 | package release | 10 | `REQ-024` | npm pack/publish |
| MODIFY | `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin-web/{src/layouts/AdminLayout.tsx,src/layouts/AdminLayout.test.tsx,package.json,package-lock.json}` | flat group + shared 0.1.4 | three parent trees + 0.2.0 | DDC consumer | 11 | `REQ-024`,`025` | DDC npm gates |
| MODIFY | `egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-admin-web/{src/app/AdminLayout.tsx,src/app/App.test.tsx,package.json,package-lock.json}` | flat filtered nav + 0.1.4 | four roots/groups + recursive filter + 0.2.0 | IdP consumer | 12 | `REQ-024`,`025` | IdP npm gates |
| MODIFY | `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web/{src/layouts/AdminLayout.tsx,src/layouts/AdminLayout.test.tsx,package.json,package-lock.json}` | flat capability nav + 0.1.4 | parent trees + operation active prefix + 0.2.0 | Gateway consumer | 13 | `REQ-024`,`025` | Gateway npm gates |
| MODIFY | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin-web/{src/app/App.integration.test.tsx,package.json}` | shared 0.1.4 | resourceCodes tree shell + 0.2.0 | RBAC consumer | 14 | `REQ-007`,`024`,`025` | RBAC npm gates |
| MODIFY | `egon-cola-platforms/egon-cola-platform-rbac3/package-lock.json` | platform lock resolves 0.1.4 | resolves exact 0.2.0 graph | RBAC shared dependency lock | 14 | `REQ-024` | npm ls/clean install |

## 6. Prerequisites, Constraints, and Plan Clarifications

### 6.1 Repository and worktree baseline

- 适用仓库指令要求最小安全改动、每个任务一次提交、禁止自动启动项目、禁止修改既有 Flyway 文件；本计划遵守。
- 当前分支/提交：`main@df425ed9484d20e6235666d91e947742998ab475`。
- 当前最新 RBAC migration 是 `V12__seed_rbac3_about_permission.sql`，所以新文件必须且只能是 V13。
- 用户无关脏文件：`docs/egon/plan/2026-08-21-14-14-idp-oauth-client-tenant-migration.md`、`docs/egon/spec/2026-08-21-07-51-idp-oauth-client-tenant-ownership.md`，以及 Access Guard/Open Archetype 的未跟踪 Spec/Plan；执行时不得暂存或改写。
- 目标 Spec 当前也是未跟踪文档；Plan 与 Spec 元数据提交应独立于生产实现提交，执行阶段采用路径限制 `git add`，不得使用宽泛 `git add .`。
- 包迁移使用 `git mv` 保留历史；生成的 Java/RPC 源不在本次范围，Admin JPA/DTO 均为手写源码。

### 6.2 Build, test, and environment prerequisites

| Concern | Exact command/source | Required state | Validation boundary |
| --- | --- | --- | --- |
| Java toolchain | `java -version && mvn -version` | Java 21；Maven 使用同一 JDK | toolchain only |
| RBAC Maven root | `egon-cola-platforms/egon-cola-platform-rbac3/pom.xml` | reactor 可选择 contract/core/starter/admin | module/static；不等于 live topology |
| Focused Maven | `mvn -pl egon-cola-platform-rbac3-contract,egon-cola-platform-rbac3-core,egon-cola-platform-rbac3-starter,egon-cola-platform-rbac3-admin -am -Dsurefire.failIfNoSpecifiedTests=false -Dtest=... test` | selector 命中的测试全部通过 | JUnit/MockMvc/local integration |
| PostgreSQL migration | Admin `maven-failsafe-plugin` + existing `Rbac3FlywayPostgresqlIT` pattern | Docker/Testcontainers 可用；V1→V13 clean migration | real PostgreSQL container, not production |
| Frontend package manager | each `package.json` scripts and checked-in `package-lock.json` | Node/npm version compatible with lockfile；clean `npm ci --legacy-peer-deps --no-audit --no-fund` | package-local |
| Shared package | `npm run typecheck && npm test && npm run build && npm pack --dry-run` | version 0.2.0 package contents correct | built npm artifact |
| Consumer package resolution | `npm ls @egon-cola/admin-web-shared --depth=0` | every consumer resolves 0.2.0 | installed graph |
| E2E | DDC/Gateway/RBAC3 existing `npm run e2e`; IdP has no e2e script | user starts services and supplies topology | browser/live boundary；执行代理不得自动启动 |

### 6.3 Immutable constraints and approved decisions

- 不改 V1–V12；V13 只有一个文件，不 backfill `rbac3_role_permission`。
- 角色直接 root 只允许 ROUTE/ACTION/API；MENU 仅导航祖先，FIELD 永不进入 grant payload。
- `required_permission_id` 是唯一实际映射；CI suggestion 永不替代 actual mapping。
- `ResourceApiBinding` 只允许同 APP 的 ROUTE/ACTION→API；runtime 只计算 OR union，不保存派生 grant 或来源证明。
- 只 active/effective role 进入上下文；BIZ/APP、字段/DataScope/SOD/Fence、UserDetails、SecurityContext 与两 Token 无 Session 模型保持。
- 所有新增/替换 HTTP 使用 common `ResultRecord`；不创建新 envelope、Result wrapper 或第二授权服务。
- shared 0.2.0 必须先发布再更新四端 lock；四端不混装 0.1.x/0.2.x。
- 不创建 Manifest、processor、registration starter、Node plugin、新缓存、消息队列或全局前端导航 store。

### 6.4 Plan Clarifications

| ID | Small implementation inference | Repository evidence | Why semantics are unchanged | Impact if wrong |
| --- | --- | --- | --- | --- |
| `PLAN-CLAR-001` | Role/Permission/Resource 的管理 HTTP 保持当前 Controller 方法拆分，但统一最终 `/api/rbac3/v1/iam` 根；资源 grant 新 URL 按主 Spec | 当前 `RolePermissionController` 同时混有 Role CRUD 与旧 permission endpoints；主 Spec §8.3要求拆为 RoleController + GrantController | 只把已存在 Role CRUD 从错误类名/旧根迁到已批准 IAM 根，不新增动作 | 若某外部消费者仍调用旧 `/api/rbac3/v1/roles`，破坏式策略要求同版本更新，不加 alias |
| `PLAN-CLAR-002` | `PermissionPage.tsx` 不作为独立路由保留；其 permission 列表/选择能力收敛为 ResourceCatalog mapping Drawer 内的 selector | 主 Spec §12.1明确 Standalone PermissionPage 无 navigation，§8.3要求其参与 advanced selector | 权限 CRUD 后端仍在，只有普通导航入口和用户可见角色配置改变 | 若产品仍需独立高级页，应先修订 Spec，而不是执行时偷偷保留菜单 |
| `PLAN-CLAR-003` | Step 2 对数百个纯 package/import 文件按 Spec §8.3 根映射机械迁移；行为文件在后续 Step 单独列符号 | 主 Spec 明确根映射且说明不逐个复制纯 import 文件；当前 Admin 有 716 个 main Java 文件 | 所有文件仍由 `git diff --name-status`、ArchUnit、compile 和旧根 `rg` 完整约束，未省略行为决策 | 若任一文件同时需要行为改造，必须归到后续 owning Step 并在提交说明列明 |
| `PLAN-CLAR-004` | 在既有 `SystemAuthorizationSnapshot` 增加 resourceCodes，作为 `DefaultAuthorizationService` 与 About 共用的同快照载体 | 当前 `RuntimeAuthorizationContext.snapshot` 类型就是 SystemAuthorizationSnapshot，且它只有permissions；AppAuthorizationContext已有resourceCodes | 主Spec已经要求同一snapshot完成API双检查和About输出；增加字段只补齐已批准数据通路，不增加新查询/模型 | 若不补该字段，Starter无法在不访问DB/网络的前提下完成REQ-006，属于不可实现 |

## 7. Ordered File-by-file Implementation Steps

### Step 1 — 固定 API 资源双条件判定与 About resourceCodes 契约

- Requirements: `REQ-005`, `REQ-006`, `REQ-007`, `REQ-008`, `REQ-016`
- Dependencies: `None`
- Baseline state: `PermissionRequest` 只有 `permissionCode`；Method Manager 丢弃 `RBACAPIResource.code`；About 未返回 snapshot 中已有的 resourceCodes。
- Observable outcome: API 注解同时按 permission/resource 判定，通用注解保持 permission-only；About 从同一 UserDetails snapshot 返回排序去重 resourceCodes。
- End state: Contract/Starter focused tests全部为 GREEN；JWT Filter、UserDetails 装载、字段/DataScope/SOD/Fence 接口未改变。
- Test-first gate: Required — 新 factory/字段/deny reason 尚不存在，focused tests 应先因构造签名或断言不满足而 RED。
- Ordered files:

#### File 1 — `MODIFY egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-contract/src/test/java/top/egon/cola/platform/rbac3/contract/{AuthorizationContractTest.java,ContractSerializationTest.java}`

- Purpose: 先固定 `PermissionRequest` 两种模式及 About wire shape。
- Symbols: `permission_request_supports_generic_and_api_modes`, `about_serializes_resource_codes_without_catalog_fields`。
- Repository evidence: 两个现有测试分别覆盖 authorization records 与 Jackson contract，沿用 JUnit 5/assertThrows/ObjectMapper 风格。
- Dependencies and consumers: 目标生产类 `PermissionRequest`、`Rbac3AboutView`；Contract module test runtime。
- Why now: 这是跨 Starter/SDK 的最早 RED 契约，后续模块只能消费这一形状。
- Contract/signature changes: 断言 `of(code)` 得到 null resource、`api(permission,resource)` trim 后双字段；About JSON 含排序 resourceCodes 且仍无 path/tree/component。
- Input/output and state mapping: raw strings/fixture snapshot fields -> record constructor -> accessor/JSON；无持久化或事件。
- Error and edge behavior: blank permission、blank API resource、null set 抛 `IllegalArgumentException/NullPointerException`；empty resourceCodes 合法。
- Implementation pseudocode:

```java
assertThat(PermissionRequest.of("system:user:read").resourceCode()).isNull();
assertThat(PermissionRequest.api("system:user:read", "iam.api.users.list"))
    .extracting(PermissionRequest::permissionCode, PermissionRequest::resourceCode)
    .containsExactly("system:user:read", "iam.api.users.list");
assertThat(serializedAbout).contains("resourceCodes").doesNotContain("navigationTree", "path", "componentKey");
assertThatThrownBy(() -> PermissionRequest.api("system:user:read", " ")).isInstanceOf(IllegalArgumentException.class);
```

- Verification contribution: RED/GREEN selector `AuthorizationContractTest,ContractSerializationTest`；证明内部 record 与外部 JSON 契约。
- After this file: 测试只因生产契约缺少 resourceCode/resourceCodes 而失败，不因 fixture 或 ObjectMapper 配置失败。

#### File 2 — `MODIFY egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-contract/src/main/java/top/egon/cola/platform/rbac3/contract/{authorization/PermissionRequest.java,authorization/SystemAuthorizationSnapshot.java,auth/Rbac3AboutView.java}`

- Purpose: 提供最小、不可变、向后兼容的 Java 契约。
- Symbols: `PermissionRequest(String permissionCode,String resourceCode)`, `of`, `api`, `SystemAuthorizationSnapshot.resourceCodes`, `Rbac3AboutView.resourceCodes`。
- Repository evidence: 当前 record compact constructor 已执行 required/trim；About 使用 defensive copy 且不携带资源树。
- Dependencies and consumers: Starter manager/service、React JSON client、Contract serialization tests；不依赖 Admin/JPA。
- Why now: Starter 和后端 About controller 编译前必须先有权威类型。
- Contract/signature changes: `of` 保持原调用源兼容；新增 `api`；System snapshot与About均在`permissions`后加入 `Set<String> resourceCodes`并提供兼容constructor默认空集合，构造时排序/不可变。
- Input/output and state mapping: annotation strings -> validated record；Admin AppAuthorizationContext resource set -> System snapshot -> deterministic About collection；null仅允许generic resourceCode。
- Error and edge behavior: API resource blank fail fast；generic null resource 合法；集合元素 blank 拒绝；重复 code 去重。
- Implementation pseudocode:

```java
record PermissionRequest(String permissionCode, String resourceCode) {
    compact: permissionCode = required(permissionCode); resourceCode = optional(resourceCode);
    static of(permission) => new PermissionRequest(permission, null);
    static api(permission, resource) => new PermissionRequest(permission, required(resource));
}
Rbac3AboutView compact constructor:
    permissions = immutableSortedSet(permissions);
    resourceCodes = immutableSortedSet(resourceCodes);
    preserve activeRoles, fieldPolicies, landingRouteCode, authVersion, policyVersion validations;
SystemAuthorizationSnapshot constructors:
    canonical constructor validates/sorts resourceCodes; legacy overloads delegate with Set.of() until Step 7 projector supplies real codes;
```

- Verification contribution: 使 File 1 Contract tests GREEN，并给 Starter 编译提供稳定 accessor。
- After this file: Contract module有一条System snapshot→About/PEP的resourceCodes载体；尚未改变实际授权决定或Admin投影。

#### File 3 — `MODIFY egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-starter/src/test/java/top/egon/cola/platform/rbac3/starter/{authorization/DefaultAuthorizationServiceTest.java,authorization/Rbac3AboutServiceTest.java,security/Rbac3MethodAuthorizationManagerTest.java,security/StarterFailClosedSecurityMatrixTest.java}`

- Purpose: 固定双 set membership、reason code、注解请求顺序和同 snapshot About 投影。
- Symbols: API allow/deny/fenced/unavailable tests、`current_about_contains_resource_codes`、annotation invocation spy。
- Repository evidence: 现有 tests 已用 Mockito/fixture `RuntimeAuthorizationContext` 验证 permission-only 与 fail-closed。
- Dependencies and consumers: File 2 contracts、`DefaultAuthorizationService`、`Rbac3MethodAuthorizationManager`、`Rbac3AboutService`。
- Why now: 在修改运行 PEP 前先证明缺失 API resource 时必须拒绝且业务方法未执行。
- Contract/signature changes: 测试捕获传入 service 的完整 `PermissionRequest`；About fixture 同时提供 permissions/resourceCodes。
- Input/output and state mapping: snapshot permission/resource sets + annotation -> AuthorizationDecision/Method AuthorizationDecision；UserDetails snapshot -> About。
- Error and edge behavior: permission 有但 resource 缺失为 `RESOURCE_NOT_GRANTED`；resource 有但 permission 缺失仍 `PERMISSION_DENIED`；fenced/unavailable 保持既有结果。
- Implementation pseudocode:

```java
givenSnapshot(permissions = Set.of("system:user:read"), resources = Set.of("iam.api.users.list"));
assertAllow(service.requirePermission(PermissionRequest.api("system:user:read", "iam.api.users.list")));
assertReason("RESOURCE_NOT_GRANTED", service.requirePermission(PermissionRequest.api("system:user:read", "iam.api.users.delete")));
invokeAnnotatedMethod(); verify(authorization).requirePermission(PermissionRequest.api("system:user:read", "iam.api.users.list"));
assertThat(about.resourceCodes()).containsExactlyInAnyOrderElementsOf(details.snapshot().appContexts().getFirst().resourceCodes());
```

- Verification contribution: 对应 `TEST-017`,`TEST-019`–`TEST-022`,`TEST-024` 的 Starter 部分。
- After this file: 测试因服务仍只检查 permission、manager 仍传 `of`、About 未复制 codes 而 RED。

#### File 4 — `MODIFY egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-starter/src/main/java/top/egon/cola/platform/rbac3/starter/{authorization/DefaultAuthorizationService.java,authorization/Rbac3AboutService.java,security/Rbac3MethodAuthorizationManager.java}`

- Purpose: 在现有 AuthorizationService/Method Security/About 边界内实现最小 GREEN。
- Symbols: `requirePermission`, annotation request resolver、`Rbac3AboutService.current`。
- Repository evidence: service 已集中处理 fenced/unavailable 和 reason；manager 已稳定遍历 class/method declarations；About 已只读 UserDetails。
- Dependencies and consumers: File 2 record/view；Spring Method Security；所有 Starter consumer；无 DB 网络调用。
- Why now: RED tests 已固定顺序、失败原因和同 snapshot 一致性。
- Contract/signature changes: manager 由 permission string 列表改为 `PermissionRequest` 列表；API annotation 用 `api`，generic 用 `of`。
- Input/output and state mapping: current context -> permissions/resourceCodes membership -> existing AuthorizationDecision；UserDetails app context -> About fields。
- Error and edge behavior: fenced 优先；permission 缺失优先于 resource；runtime unavailable 为 INDETERMINATE；任何非 ALLOW 阻断方法。
- Implementation pseudocode:

```java
context = contextSource.load();
if (context.fenced()) return deny("AUTHORIZATION_FENCED");
if (!context.snapshot().permissions().contains(request.permissionCode())) return deny("PERMISSION_DENIED");
if (request.resourceCode() != null && !context.snapshot().resourceCodes().contains(request.resourceCode()))
    return deny("RESOURCE_NOT_GRANTED");
return allow();
manager: generic annotations -> PermissionRequest.of; RBACAPIResource -> PermissionRequest.api(permission, code);
about: copy permissions, snapshot.resourceCodes, fieldPolicies and versions from the same Rbac3UserDetails SystemAuthorizationSnapshot;
```

- Verification contribution: File 3 tests GREEN；Starter method security integration继续通过。
- After this file: Java PEP/About 契约完成；Admin runtime 仍按旧 role_permission 生成 resourceCodes，留给 Step 7。

- Validation working directory: `egon-cola-platforms/egon-cola-platform-rbac3`
- Verification command: `mvn -pl egon-cola-platform-rbac3-contract,egon-cola-platform-rbac3-starter -am -Dsurefire.failIfNoSpecifiedTests=false -Dtest=AuthorizationContractTest,ContractSerializationTest,DefaultAuthorizationServiceTest,Rbac3AboutServiceTest,Rbac3MethodAuthorizationManagerTest,StarterFailClosedSecurityMatrixTest test`
- Expected result: exit 0；命名 tests 全部通过；`rg "PermissionRequest\.api"` 命中 manager/tests，JWT Filter 文件无 diff。
- Failure returns to: File 1/3 若契约断言不准确；File 2/4 若 constructor、reason 或请求顺序不一致；涉及公开字段重排之外的新行为则返回 Spec。
- Completion criteria: `TEST-017`,`019`–`022`,`024` 的本 Step 范围可观察；通用 `PermissionRequest.of` 原调用全部编译。
- Rollback: revert 本 Step Contract/Starter 路径；尚无 schema 或数据变化。
- Commit paths: `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-contract/src/test/java/top/egon/cola/platform/rbac3/contract/{AuthorizationContractTest.java,ContractSerializationTest.java}` ; `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-contract/src/main/java/top/egon/cola/platform/rbac3/contract/{authorization/PermissionRequest.java,authorization/SystemAuthorizationSnapshot.java,auth/Rbac3AboutView.java}` ; `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-starter/src/test/java/top/egon/cola/platform/rbac3/starter/{authorization/DefaultAuthorizationServiceTest.java,authorization/Rbac3AboutServiceTest.java,security/Rbac3MethodAuthorizationManagerTest.java,security/StarterFailClosedSecurityMatrixTest.java}` ; `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-starter/src/main/java/top/egon/cola/platform/rbac3/starter/{authorization/DefaultAuthorizationService.java,authorization/Rbac3AboutService.java,security/Rbac3MethodAuthorizationManager.java}`
- Commit: `feat(rbac3-starter): enforce API resource and permission decisions`

### Step 2 — 把 RBAC3 Admin 机械迁入最终 IAM、authorization、registration 和 shared 边界

- Requirements: `REQ-008`, `REQ-010`, `REQ-011`, `REQ-012`, `REQ-013`, `REQ-026`
- Dependencies: `Step 1`
- Baseline state: `admin.iam` 有 11 个顶层包，授权关系嵌在 business/application/role，resource report 在 IAM，顶层 authorization/runtime/management/participation/simulation 分散。
- Observable outcome: `admin.iam` 精确只剩六类基础对象；授权目录、grant、policy、runtime、simulation、CI 与 tenant context 全部到主 Spec 目标根，行为和 URL 暂不改变。
- End state: 所有 main/test package 与 imports 编译；旧根只允许出现在历史文档/迁移字符串，生产源码中为零；后续 Step 使用最终路径。
- Test-first gate: Required — 先收紧 ArchUnit/模块边界，测试应因旧根与第七个 IAM 顶层包存在而 RED。
- Ordered files:

#### File 1 — `MODIFY egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/architecture/{AdminLayerBoundaryTest.java,Rbac3AuthorizationArchitectureTest.java,Rbac3ModuleBoundaryTest.java}`

- Purpose: 把精确包集合、依赖方向和禁止旧根写成可执行边界。
- Symbols: `iam_contains_exactly_six_business_roots`, `authorization_layers_do_not_depend_on_registration`, `legacy_roots_are_absent`。
- Repository evidence: 三个现有 architecture tests 已扫描 Admin package/import/module boundaries。
- Dependencies and consumers: 编译后的 Admin classes；JUnit；不依赖 DB/Redis。
- Why now: 大规模移动前先建立唯一 RED 验收，防止留下空壳或循环依赖。
- Contract/signature changes: 无生产 API；测试允许 `iam.user/role/business/application/organization/position`，禁止 `iam.resource/permission/policy/tenant/authorizationstate` 等。
- Input/output and state mapping: compiled class package graph -> allowed root sets/edge assertions -> pass/fail。
- Error and edge behavior: 空 package-info 也算旧根失败；test classes 与 main imports 同时扫描；audit/bootstrap/config/shared 顶层允许但不能拥有 grant/policy。
- Implementation pseudocode:

```java
Set<String> iamRoots = directSubpackagesOf("...admin.iam");
assertThat(iamRoots).containsExactlyInAnyOrder("user", "role", "business", "application", "organization", "position");
assertNoClassesIn("..admin.iam.resource..", "..admin.runtime..", "..admin.management..", "..admin.participation..");
assertLayer("registration").mayDependOn("authorization.resource", "iam.application", "shared");
assertLayer("authorization.runtime").mayDependOn("authorization.grant", "authorization.policy", "authorization.resource");
```

- Verification contribution: `TEST-027`,`TEST-029` package portion。
- After this file: architecture tests RED only because current packages have not moved。

#### File 2 — `RENAME egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/{main,test}/java/top/egon/cola/platform/rbac3/admin/{iam/role/assignment->authorization/grant/userrole,iam/role/inheritance->authorization/grant/roleinheritance,iam/role/activation->authorization/runtime/activation,iam/resource/{controller,domain,field,service}->authorization/resource/{controller,domain,field,service},iam/permission->authorization/permission,iam/policy->authorization/policy,iam/authorizationstate->authorization/runtime/state,authorization/{controller,domain,repository,service}->authorization/runtime/decision/{controller,domain,repository,service},management->authorization/policy/management,participation->authorization/policy/participation,simulation->authorization/simulation,runtime->authorization/runtime}`

- Purpose: 机械移动主要授权目录、关系、策略、现有decision与runtime源/测试树；保留`iam/resource/report`给File 3单独迁registration。
- Symbols: 所有现有类名暂保持；只改 package/import/FQCN 与 package-info 文案。
- Repository evidence: 主 Spec §8.3 给出根映射；当前 `find` 证实这些路径真实存在且由 Spring 根扫描覆盖。
- Dependencies and consumers: Admin 内部 Controller/Facade/Repository/config/bootstrap/audit；Core/Starter imports；对应 tests。
- Why now: 先搬完整根，再对 business/application/tenant 的混合包做精确拆分，降低中间 import 歧义。
- Contract/signature changes: 无方法/URL/表字段变化；Spring component/entity scan 仍由 `admin` 根覆盖。
- Input/output and state mapping: 原 FQCN -> 目标 FQCN 一对一；bean name 默认值、JPA table name、事务、JSON 不变。
- Error and edge behavior: 更新 string-based JPQL entity names、reflection/ArchUnit allowlist 和 test package；禁止复制后保留旧类。
- Implementation pseudocode:

```text
for each approved root mapping in Spec section 8.3:
  git mv source subtree to target subtree;
  rewrite package declarations and imports using the exact old/new prefix pair;
  preserve annotations, method bodies, table names, endpoint mappings and bean construction;
  run rg for the old prefix and classify only historical documentation/migration text as allowed;
```

- Verification contribution: Admin compile + architecture tests；证明行为保持的机械迁移。
- After this file: 大部分授权类位于目标根；business/application 授权关系和 tenant context 仍待 File 3 拆分。

#### File 3 — `RENAME egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/{main,test}/java/top/egon/cola/platform/rbac3/admin/{iam/business/UserBusinessAccess*->authorization/grant/business,iam/application/TenantApplication*->authorization/grant/application,iam/tenant/**->shared/tenant,iam/resource/report/**->registration/ci}`

- Purpose: 把混在基础实体包内的授权事实、可信 tenant context 与 CI 入站移出 IAM。
- Symbols: `UserBusinessAccess*`, `TenantApplication*`, `TenantContext*`, `CiResourceReport*`；CI 类名在本 Step 只移动，Step 6 再统一 rename/行为。
- Repository evidence: current source list精确显示 catalog 与 access/grant 类共包，tenant 只有 context/filter 和空壳，report 共有 8 个类。
- Dependencies and consumers: BusinessCatalog/ApplicationPO 留在 IAM；Role eligibility/runtime/config/filter/tests 更新 imports。
- Why now: 完成 IAM 精确六根，并保留 Step 6 对 CI 行为的独立 RED/GREEN 边界。
- Contract/signature changes: 无 HTTP/body/表变化；删除没有类型的 tenant dto/enums/po/vo/repository package-info 壳。
- Input/output and state mapping: catalog read models 保留原包；tenant-scoped grant PO/Facade/Controller 迁到 grant 子域；request context 迁到 shared。
- Error and edge behavior: `TenantContextFilter` 过滤顺序与 trusted-source 校验不变；DDC RPC adapter仍在 IAM business catalog；CI browser boundary 不变。
- Implementation pseudocode:

```text
move UserBusinessAccess controller/command/status/PO/VO/repository/facade to authorization.grant.business;
move TenantApplication status/PO/repository/facade and grant-facing controller methods to authorization.grant.application;
move TenantContext, Resolver, Filter, ResolutionException to shared.tenant and delete empty shell package-info files;
move CiResourceReport subtree to registration.ci without changing endpoint or payload until Step 6;
rewrite every caller import and keep tenant/application derivation exactly unchanged;
```

- Verification contribution: `TEST-027` exact IAM root；tenant/filter and DDC boundary regressions。
- After this file: IAM 顶层精确六类；尚未按新授权语义改类名或表。

#### File 4 — `MODIFY egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/{main,test}/java/top/egon/cola/platform/rbac3/admin/{audit,bootstrap,config,shared,iam}/**`

- Purpose: 修复所有非移动 owner 的 imports、Spring wiring、reflection 字符串和 tests。
- Symbols: config bean parameters、bootstrap repositories、audit assemblers、IAM controllers/services 的新 imports。
- Repository evidence: `rg` 显示 bootstrap/config/runtime 与旧 IAM PO/FQCN 有直接依赖；Spring 仍从 `admin` 根扫描。
- Dependencies and consumers: Files 2/3 目标包；Maven compiler；application context tests。
- Why now: 只有所有外围消费者更新后 Step 2 才能形成独立编译提交。
- Contract/signature changes: 仅 import/FQCN；所有公开 URI、JSON、transactions 和 DDC/IdP clients 原样保留到行为 owning Steps。
- Input/output and state mapping: DI target type变化但 bean 实例/构造参数一致；无持久化数据变动。
- Error and edge behavior: string JPQL entity name、`Class.forName`、component scan、test package 必须同步；禁止临时 adapter/legacy wrapper。
- Implementation pseudocode:

```text
compile once to collect unresolved old FQCN consumers;
for each error, replace only the approved package prefix and preserve the referenced symbol;
update architecture/config/application-context tests to the same final FQCN;
scan main and test sources for forbidden prefixes and empty tenant shells;
rerun compile and architecture tests until zero unresolved imports and zero forbidden roots;
```

- Verification contribution: Admin whole-module compilation，`TEST-027`,`029` 的静态部分。
- After this file: package-only migration完成且行为不变；Step 3 可在最终路径创建新模型。

- Validation working directory: `egon-cola-platforms/egon-cola-platform-rbac3`
- Verification command: `mvn -pl egon-cola-platform-rbac3-admin -am -DskipTests compile && mvn -pl egon-cola-platform-rbac3-admin -am -Dsurefire.failIfNoSpecifiedTests=false -Dtest=AdminLayerBoundaryTest,Rbac3AuthorizationArchitectureTest,Rbac3ModuleBoundaryTest,TenantContextFilterTest,RpcDdcCatalogGatewayTest test && ! rg -n "top\.egon\.cola\.platform\.rbac3\.admin\.(iam\.(resource|permission|policy|tenant|authorizationstate)|runtime|management|participation|simulation)" egon-cola-platform-rbac3-admin/src/main/java egon-cola-platform-rbac3-admin/src/test/java`
- Expected result: 两条 Maven 命令 exit 0；最终 `rg` exit 1（零命中）；`git diff --name-status` 只含批准根映射及其消费者。
- Failure returns to: File 2/3 若映射漏类；File 4 若仅 import/JPQL/wiring 未同步；若需要改变业务方法或 URL，移到相应后续 Step。
- Completion criteria: IAM 六根、authorization/registration/shared 边界和全部 package tests GREEN；无 compatibility wrapper。
- Rollback: 以本 Step 提交整体 revert；纯源码移动，无 schema/data 变化。
- Commit paths: `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/architecture/{AdminLayerBoundaryTest.java,Rbac3AuthorizationArchitectureTest.java,Rbac3ModuleBoundaryTest.java}` ; `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/{main,test}/java/top/egon/cola/platform/rbac3/admin/{iam/role/assignment->authorization/grant/userrole,iam/role/inheritance->authorization/grant/roleinheritance,iam/role/activation->authorization/runtime/activation,iam/resource/{controller,domain,field,service}->authorization/resource/{controller,domain,field,service},iam/permission->authorization/permission,iam/policy->authorization/policy,iam/authorizationstate->authorization/runtime/state,authorization/{controller,domain,repository,service}->authorization/runtime/decision/{controller,domain,repository,service},management->authorization/policy/management,participation->authorization/policy/participation,simulation->authorization/simulation,runtime->authorization/runtime}` ; `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/{main,test}/java/top/egon/cola/platform/rbac3/admin/{iam/business/UserBusinessAccess*->authorization/grant/business,iam/application/TenantApplication*->authorization/grant/application,iam/tenant/**->shared/tenant,iam/resource/report/**->registration/ci}` ; `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/{main,test}/java/top/egon/cola/platform/rbac3/admin/{audit,bootstrap,config,shared,iam}/**`
- Commit: `refactor(rbac3-admin): separate IAM authorization and registration packages`

### Step 3 — 建立 RoleResourceGrant、ResourceApiBinding 与 suggestion 持久模型

- Requirements: `REQ-001`, `REQ-003`, `REQ-004`, `REQ-014`, `REQ-019`, `REQ-020`, `REQ-021`, `REQ-026`
- Dependencies: `Step 2`
- Baseline state: 最终包已存在，但没有 role-resource/binding PO 或 repository port；ResourcePO 只有 actual requiredPermissionId。
- Observable outcome: 两个新模型、枚举和 repository contract 可编译并由 entity tests 固定；CI suggestion 与 actual mapping 在对象层分离。
- End state: 尚未加入 V13 或切换 runtime；新 vertical slices 可由 Steps 4–6 实现，旧模型暂留直到 Step 7。
- Test-first gate: Required — entity/constructor/port tests 应先因目标类不存在而 RED；本 Step 不运行 application context 或 migration。
- Ordered files:

#### File 1 — `CREATE egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/authorization/{grant/roleresource/RoleResourceGrantPersistenceTest.java,resource/apibinding/ResourceApiBindingPersistenceTest.java}`

- Purpose: 固定 PO table/column/tenant/window/status 与 binding source-target 约束映射。
- Symbols: constructor validation、JPA annotation reflection、equality/ID/version expectations。
- Repository evidence: Admin 现有 `ConstraintPersistenceEntityTest` 与 migration/entity tests 使用反射检查 `@Table/@Column` 和构造不变量。
- Dependencies and consumers: 新 PO/enums；JUnit/JPA annotations；不连接 DB。
- Why now: 先固定最小数据模型，避免 Service 实现时发明字段。
- Contract/signature changes: tests 要求 Spec §10.2/§11 完整字段；grant 复用 tenant/audit/version pattern，binding 为 global audited/version entity。
- Input/output and state mapping: constructor inputs -> PO fields -> future V13 columns；null/default/status/window 与 precision 按 Instant/BIGINT。
- Error and edge behavior: grant validTo<=validFrom 拒绝；binding source==target 拒绝；blank source build/checksum 与非法 status 拒绝。
- Implementation pseudocode:

```java
assertTable(RoleResourceGrantPO.class, "rbac3_role_resource_grant");
assertColumns(RoleResourceGrantPO.class, "tenant_id", "application_id", "role_id", "resource_id", "valid_from", "valid_to", "status");
assertTable(ResourceApiBindingPO.class, "rbac3_resource_api_binding");
assertColumns(ResourceApiBindingPO.class, "application_id", "source_resource_id", "api_resource_id", "source_build_id", "source_checksum", "status");
assertThatThrownBy(() -> grant(validFrom, validFrom)).isInstanceOf(IllegalArgumentException.class);
```

- Verification contribution: 为 `TEST-025`,`030` 提供对象层 RED/GREEN。
- After this file: tests因新类型缺失而 RED，且不要求未创建的 DB table。

#### File 2 — `CREATE egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/authorization/grant/roleresource/{domain/po/RoleResourceGrantPO.java,domain/enums/RoleResourceGrantStatusEnum.java,repository/RoleResourceGrantRepository.java}`

- Purpose: 定义 tenant role-resource direct grant 的持久对象和业务 repository port。
- Symbols: `RoleResourceGrantPO`, `RoleResourceGrantStatusEnum`, `RoleResourceGrantRepository` read/replace/query records。
- Repository evidence: 当前 RolePO/RolePermissionPO/TenantScopedPO 风格提供 audit/version/Instant/status 先例；不新增 BaseGrantService。
- Dependencies and consumers: RolePO、ResourcePO IDs；Step 4 service/JPA implementation；Step 7 runtime fact reader。
- Why now: API DTO/Service 之前先有唯一持久事实和原子 repository contract。
- Contract/signature changes: port 接收 server-derived tenant/application/actor/window/version；不接收 permission IDs/codes。
- Input/output and state mapping: requested direct resource IDs -> ACTIVE rows；removed rows -> DISABLED；window inclusive/exclusive；derived API 从不持久化。
- Error and edge behavior: duplicate tuple、cross tenant/app、inactive role/resource、optimistic conflict 由实现映射为 domain errors；empty set 合法 replace。
- Implementation pseudocode:

```java
interface RoleResourceGrantRepository {
  RoleResourceGrantTreeFacts loadTree(long tenantId, long roleId, Instant now);
  ReplaceResult replace(ReplaceCommand command); // implementation owns one transaction and role lock
  Set<Long> activeDirectResourceIds(Set<Long> roleIds, Instant now);
}
RoleResourceGrantPO.activate(tenant, app, role, resource, from, to, actor):
  require positive IDs; require to == null || to.isAfter(from); status = ACTIVE;
disable(actor, now): status = DISABLED; validTo = min(existingValidTo, now); bump version;
```

- Verification contribution: File 1 grant test、Step 4 service contract、Step 7 runtime query。
- After this file: grant model/port 编译；尚无 Controller/JPA implementation/schema。

#### File 3 — `CREATE egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/authorization/resource/apibinding/{domain/po/ResourceApiBindingPO.java,domain/enums/ResourceApiBindingStatusEnum.java,repository/ResourceApiBindingRepository.java}`

- Purpose: 定义 CI-owned ROUTE/ACTION→API 机械关系与反向查询 port。
- Symbols: `ResourceApiBindingPO`, status enum, `replaceForApplication`, `apiIdsForSources`, `activeSourceGrantCountForApi`。
- Repository evidence: existing CI report complete replace 和 ResourcePO source fields 提供 build/checksum/status/version 风格；global catalog 无 tenant。
- Dependencies and consumers: ResourcePO IDs/applicationId；Steps 5/6 mapping/registration；Step 7 runtime union。
- Why now: mapping in-use guard 与 CI transaction 需要共享一份可校验关系，不可用 JSON 或 permission 相等推断。
- Contract/signature changes: repository keys为 application/source/api；无 tenant 参数；replace输入完整 application binding set。
- Input/output and state mapping: normalized `apiResourceCodes` -> resolved API IDs -> ACTIVE rows；缺失旧 CI rows -> STALE/DISABLED；共享 target保留多 source。
- Error and edge behavior: source/target相同、source类型非 ROUTE/ACTION、target非 API、跨 APP、重复 pair 均在 write 前拒绝并零写。
- Implementation pseudocode:

```java
interface ResourceApiBindingRepository {
  void replaceForApplication(long applicationId, String buildId, String checksum, Set<BindingPair> pairs);
  Set<Long> apiIdsForSources(long applicationId, Set<Long> sourceIds, Instant now);
  long countDistinctActiveRolesDerivingApi(long applicationId, long apiResourceId, Instant now);
}
BindingPair(sourceId, apiId): require sourceId > 0 && apiId > 0 && sourceId != apiId;
PO.fromResolvedResources: copy applicationId/source/api/build/checksum; status = ACTIVE;
```

- Verification contribution: File 1 binding test，后续 `TEST-009`,`030`–`032`。
- After this file: binding model/port存在；实际 type/same-app 验证由 registration/JPA 实现补齐。

#### File 4 — `MODIFY egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/authorization/resource/domain/po/ResourcePO.java`

- Purpose: 增加非权威 `suggestedPermissionCode`，保留 `requiredPermissionId` 为唯一 actual mapping。
- Symbols: field/constructor/getter/update mechanical facts whitelist。
- Repository evidence: ResourcePO 当前含 requiredPermissionId、sourceBuildId/checksum/mechanicalFacts/version，CI store 当前覆盖 actual mapping。
- Dependencies and consumers: registration store、mapping view、V13；Role grantability仍只读取 requiredPermissionId。
- Why now: Steps 5/6 必须在对象层区分 suggestion/actual，避免继续双写。
- Contract/signature changes: nullable trimmed suggestion；generic resource update不得从普通 body 任意覆盖，CI只更新 suggestion。
- Input/output and state mapping: report permission suggestion -> nullable column；actual mapping admin command -> requiredPermissionId；两者独立版本字段共用 resource optimistic version。
- Error and edge behavior: blank suggestion normalized null or rejected per current DTO convention；CI null不清除 MANUAL actual；API suggestion mismatch由 Step 5返回422。
- Implementation pseudocode:

```java
private String suggestedPermissionCode;
updateMechanicalFacts(..., String suggestion, ...):
  this.suggestedPermissionCode = optionalTrimmed(suggestion);
  preserve this.requiredPermissionId exactly;
confirmActualPermission(long permissionId):
  require permissionId > 0; this.requiredPermissionId = permissionId; bump optimistic version;
getter exposes suggestion only to admin mapping/registration assemblers;
```

- Verification contribution: entity tests、Step 5 mapping view、Step 6 zero-write assertions。
- After this file: ResourcePO 可以同时保存 suggestion 与 actual；schema仍由 Step 7 V13 创建。

- Validation working directory: `egon-cola-platforms/egon-cola-platform-rbac3`
- Verification command: `mvn -pl egon-cola-platform-rbac3-admin -am -Dsurefire.failIfNoSpecifiedTests=false -Dtest=RoleResourceGrantPersistenceTest,ResourceApiBindingPersistenceTest,ConstraintPersistenceEntityTest test`
- Expected result: exit 0；两个新 PO/port 与 ResourcePO 编译；未运行 application context/Flyway。
- Failure returns to: File 1 若测试要求超出 Spec 字段；Files 2–4 若 PO/port 与 V13 设计不一致；任何新表/字段需求返回 Spec。
- Completion criteria: 对象层完整且不新增第三张映射表、不写 derived grant、不引入 tenant 到 binding/catalog。
- Rollback: revert 新目录与 ResourcePO 单字段；无 DB 迁移。
- Commit paths: `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/authorization/{grant/roleresource/RoleResourceGrantPersistenceTest.java,resource/apibinding/ResourceApiBindingPersistenceTest.java}` ; `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/authorization/grant/roleresource/{domain/po/RoleResourceGrantPO.java,domain/enums/RoleResourceGrantStatusEnum.java,repository/RoleResourceGrantRepository.java}` ; `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/authorization/resource/apibinding/{domain/po/ResourceApiBindingPO.java,domain/enums/ResourceApiBindingStatusEnum.java,repository/ResourceApiBindingRepository.java}` ; `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/authorization/resource/domain/po/ResourcePO.java`
- Commit: `feat(rbac3-admin): define resource grant and API binding models`

### Step 4 — 以资源树和原子 replace 替换角色权限字符接口

- Requirements: `REQ-001`, `REQ-002`, `REQ-014`, `REQ-016`, `REQ-018`, `REQ-019`, `REQ-021`, `REQ-026`
- Dependencies: `Step 3`
- Baseline state: Role CRUD/impact 与旧 permission POST/DELETE 混在原 Controller/Facade；新 grant port 没有实现或 HTTP 入口。
- Observable outcome: API-001/002 完整可用；角色页未来只需读取树并提交 direct resource IDs，所有 server-derived tenant/app/actor 与 TOCTOU 重验在服务端。
- End state: 旧 permission POST/DELETE 与 `bindPermissions/unbindPermission` 调用链从 Controller/RoleFacade/JpaRoleRepository 移除；旧 PO 本身到 Step 7 删除。
- Test-first gate: Required — Controller/service/persistence tests 先因 API/DTO/implementation 缺失而 RED。
- Ordered files:

#### File 1 — `CREATE egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/authorization/grant/roleresource/{RoleResourceGrantControllerTest.java,RoleResourceGrantServiceTest.java,JpaRoleResourceGrantRepositoryIT.java}`

- Purpose: 固定 API-001/002 wire shape、无 permission 字段、原子 replace、并发与并集摘要。
- Symbols: read tree、empty replace、duplicate/window、cross-app、unmapped、version conflict、rollback、shared API union tests。
- Repository evidence: 现有 RoleControlFacadeTest、CiResourceReportControllerTest、role concurrency IT 提供 MockMvc/common Result/JPA lock patterns。
- Dependencies and consumers: 新 DTO/VO/Controller/Service/JPA repo；Testcontainers/EntityManager fixture。
- Why now: 先把业务事务、错误和完整 JSON 作为 RED，避免照搬旧增量语义。
- Contract/signature changes: GET/PUT `/api/rbac3/v1/iam/roles/{roleId}/resources`；请求无 applicationId/tenant/permission。
- Input/output and state mapping: selected direct roots/window/version -> ACTIVE/DISABLED grant rows -> direct/inherited/derived/effective summaries。
- Error and edge behavior: MENU/APP/FIELD、PENDING/unmapped、duplicate、cross tenant/app 返回400/404/422；stale version 409；注入 failure 全 rollback。
- Implementation pseudocode:

```java
mockMvc.get(roleResourcesUrl).with(currentUser).andExpect(noJsonPath("..permissionCode"));
mockMvc.put(roleResourcesUrl, body(resourceIds, expectedRoleVersion)).andExpect(status().isOk());
assertActiveDirectGrants(roleId, requestedIds); assertNoPersistedGrant(derivedApiId);
runTwoReplacesWithSameVersion(); assertExactlyOneSuccessAndOneConflict();
injectFailureAfterDisableBeforeInsert(); assertGrantRowsUnchanged(); assertVersionsUnchanged();
assertDerivedApisUnion(routeBindings, actionBindings, directApiRoot);
```

- Verification contribution: `TEST-003`–`006`,`023`,`030`–`032` 的管理端部分，且契约覆盖 `TEST-001`,`002` server fixture。
- After this file: tests因目标 Controller/Service/JPA 未实现而 RED。

#### File 2 — `CREATE egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/authorization/grant/roleresource/{domain/dto/ReplaceRoleResourcesRequestDTO.java,domain/dto/ReplaceRoleResourcesCommandDTO.java,domain/vo/RoleResourceGrantTreeVO.java,domain/vo/RoleResourceGrantMutationVO.java}`

- Purpose: 分离不可信 HTTP 请求、server-derived command 和人类可读响应。
- Symbols: DTO records及 nested summary/node/linked API records。
- Repository evidence: Admin 当前使用 RequestDTO→CommandDTO→VO，common ResultRecord 包装；ID wire 使用 string。
- Dependencies and consumers: Controller、Service、Admin Web API types；不依赖 JPA PO serialization。
- Why now: Service/Controller 实现前固定字段、null/default/enum 和禁止字段。
- Contract/signature changes: 精确实现主 Spec API-001/002 JSON；request resourceIds 0–2000、expectedRoleVersion>=0、optional window。
- Input/output and state mapping: path roleId + CurrentRbac3User + clock + request -> command tenant/application resolved later；facts -> sorted VO。
- Error and edge behavior: duplicate/invalid decimal IDs、validTo<=effective validFrom、negative version fail before repository；response从不含 permission ID/code。
- Implementation pseudocode:

```java
ReplaceRoleResourcesRequestDTO.validate():
  require resourceIds != null && size <= 2000 && all decimal-positive && distinct;
  require expectedRoleVersion >= 0; require validTo == null || validTo > effectiveValidFrom;
Controller maps currentUser.tenantId/subject + roleId + databaseClock.instant into CommandDTO;
TreeVO nodes map technicalType/category/mappingStatus/grantState/grantable/disabledReason/linkedApis/children;
MutationVO returns sorted direct/derived/effective IDs and counts, never permission characters;
```

- Verification contribution: File 1 MockMvc JSON/validation assertions。
- After this file: HTTP/domain types编译；无写行为。

#### File 3 — `CREATE egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/authorization/grant/roleresource/{repository/jpa/JpaRoleResourceGrantRepository.java,service/RoleResourceGrantService.java,controller/RoleResourceGrantController.java}`

- Purpose: 实现 tree query、authoritative revalidation、role lock 与 direct grant 差集事务。
- Symbols: `tree`, `replace`, `loadTree`, `replace`；Spring transaction/method annotations。
- Repository evidence: `JpaRoleRepository` 已有 role lock/version与 RoleEligibilityService；GlobalResourceCatalog/activation facts 有 resource/closure查询；统一异常 mapper 已存在。
- Dependencies and consumers: Files 2 DTO/VO、Step 3 ports/PO、CurrentRbac3User、DatabaseClock、version publisher/outbox、ResourceApiBindingRepository。
- Why now: RED tests已固定完整行为，且持久模型已稳定。
- Contract/signature changes: Controller使用 `@RequiresPermission("system:role-resource:read/manage")` 和 common ResultRecord；不接受 applicationId/tenant。
- Input/output and state mapping: current tenant+role -> role.application -> catalog/grants/closure/bindings -> tree；PUT -> disable/insert/version/audit -> after-commit invalidation。
- Error and edge behavior: read cross-tenant non-disclosure；replace rechecks ACTIVE/mapping/permission/type/same-app/TenantApplication；same set+same version no-op；publication pending fail closed。
- Implementation pseudocode:

```java
@Transactional replace(command):
  role = lockRole(command.tenantId, command.roleId); require role.version == expected;
  roots = loadResources(command.resourceIds); validate type in ROUTE/ACTION/API, ACTIVE, same app, actual permission ACTIVE;
  current = activeDirectGrants(role, now); disable(current - requested); insert(requested - current);
  role.bumpVersion(); state = authorizationState.advanceAuthVersion(tenant); writeAudit(counts, actor);
  afterCommitPublisher.invalidate(tenant, affectedUsersForRole(role.id));
  effective = union(directAndInheritedRoots(role), apiBindingsForRouteAndActionRoots); return mutationVO(effective);
tree(role): batch load catalog, direct/inherited grants and bindings; assemble deterministic parent tree without N+1;
```

- Verification contribution: File 1 GREEN；`TEST-003`–`006`,`023`,`026`,`030`–`032` relevant paths。
- After this file: 新角色资源 API 可编译/测试；runtime 快照仍在 Step 7 才读取新表。

#### File 4 — `MODIFY egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/iam/role/{controller/RoleController.java,service/RoleFacade.java,repository/jpa/JpaRoleRepository.java}`

- Purpose: 让基础 Role CRUD/impact 留在 IAM，删除旧 permission bind/unbind 行为和旧 `/api/rbac3/v1/roles` 根。
- Symbols: `RoleController`（由包迁移后的旧 RolePermissionController rename/split）、RoleFacade methods、JpaRoleRepository old mapping methods。
- Repository evidence: 当前单 Controller 含 roles/create/update/bind/unbind/inheritance/impact；主 Spec 要求 Role CRUD与grant分离并统一 IAM URL。
- Dependencies and consumers: Role UI、inheritance service、Step 3/4 Grant Controller；RolePO 仍保留。
- Why now: 新替代 API 已存在，才可删除旧 endpoint 而不留下空窗。
- Contract/signature changes: Role CRUD/impact 切 `/api/rbac3/v1/iam/roles`；permission POST/DELETE完全删除；inheritance由目标 grant.roleinheritance owner继续提供。
- Input/output and state mapping: Role CRUD/impact原映射不变；不再把 permission IDs写入 RoleFacade/JpaRoleRepository。
- Error and edge behavior: 旧 URL 明确404；不增加 redirect/alias；RoleResourceGrant Controller承担新权限动作。
- Implementation pseudocode:

```java
rename controller class to RoleController and set @RequestMapping("/api/rbac3/v1/iam/roles");
retain list/create/update/impact methods with current ResultRecord and tenant derivation;
remove bindPermissions/unbindPermission handler methods and associated Facade/JpaRoleRepository methods;
delegate inheritance endpoints to authorization.grant.roleinheritance owner without changing hierarchy invariants;
assert no RequestMapping contains "/permissions" and no service accepts permissionIds for a role;
```

- Verification contribution: `TEST-015`,`029` old/new URL，编译时消除旧调用链。
- After this file: Role 管理与资源授权职责分开；旧 RolePermission DTO/PO到 Step 7 删除。

- Validation working directory: `egon-cola-platforms/egon-cola-platform-rbac3`
- Verification command: `mvn -pl egon-cola-platform-rbac3-admin -am -Dsurefire.failIfNoSpecifiedTests=false -Dtest=RoleResourceGrantControllerTest,RoleResourceGrantServiceTest,JpaRoleResourceGrantRepositoryIT,RoleControlFacadeTest,Rbac3ControlPlaneGatewayDiscoveryTest test`
- Expected result: exit 0；API-001/002 tests GREEN；source scan无 role permission POST/DELETE mapping；旧 URL contract断言404。
- Failure returns to: File 1/2 contract、File 3 transaction/query、File 4 legacy split；若需要新的授权来源或 AND 规则返回 Spec。
- Completion criteria: direct roots原子替换、derived API只读并集、错误/版本/失效语义可观察；无 permission 字符角色入口。
- Rollback: revert Step 4；V13尚未加入，旧 PO/table仍存在，可源码级回退。
- Commit paths: `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/authorization/grant/roleresource/{RoleResourceGrantControllerTest.java,RoleResourceGrantServiceTest.java,JpaRoleResourceGrantRepositoryIT.java}` ; `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/authorization/grant/roleresource/{domain/dto/ReplaceRoleResourcesRequestDTO.java,domain/dto/ReplaceRoleResourcesCommandDTO.java,domain/vo/RoleResourceGrantTreeVO.java,domain/vo/RoleResourceGrantMutationVO.java}` ; `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/authorization/grant/roleresource/{repository/jpa/JpaRoleResourceGrantRepository.java,service/RoleResourceGrantService.java,controller/RoleResourceGrantController.java}` ; `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/iam/role/{controller/RoleController.java,service/RoleFacade.java,repository/jpa/JpaRoleRepository.java}`
- Commit: `feat(rbac3-admin): replace role permissions with resource grants`

### Step 5 — 增加管理员专用的资源实际权限映射接口

- Requirements: `REQ-003`, `REQ-004`, `REQ-015`, `REQ-016`, `REQ-018`, `REQ-019`, `REQ-026`
- Dependencies: `Step 3`
- Baseline state: ResourcePO 有 actual 字段但由 generic CRUD/CI 隐式写；无 suggestion/actual/in-use 的专用读取和安全更新。
- Observable outcome: API-003/004 可读取和确认 actual mapping；不同映射在 direct 或反向 API 来源在用时稳定 409，同值重放幂等。
- End state: actual mapping 只有 ResourcePO 一处写入口；CI 尚在 Step 6 才停止旧覆盖；V13 尚未执行。
- Test-first gate: Required — mapping Controller/Service/VO 不存在，focused tests 先 RED。
- Ordered files:

#### File 1 — `CREATE egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/authorization/permission/{ResourcePermissionMappingControllerTest.java,ResourcePermissionMappingServiceTest.java,JpaResourcePermissionMappingRepositoryIT.java}`

- Purpose: 固定管理员权限、完整 ResultRecord、in-use 反向判断、幂等和回滚。
- Symbols: configured/unconfigured GET、initial/same/different PUT、API suggestion mismatch、direct/reverse grant conflict tests。
- Repository evidence: 当前 Resource CRUD/controller tests 和 common exception handler tests 可复用 security/MockMvc/EntityManager fixtures。
- Dependencies and consumers: 目标 mapping DTO/VO/service/repository/controller；RoleResourceGrant/Binding ports。
- Why now: 先用可观察结果定义唯一实际映射写口，随后最小实现。
- Contract/signature changes: GET/PUT `/api/rbac3/v1/iam/resources/{resourceId}/permission-mapping`；raw code只出现在管理员响应。
- Input/output and state mapping: resource + actual/suggestion + active role counts -> MappingVO；PUT permission/version/reason -> requiredPermissionId/version/audit/invalidation。
- Error and edge behavior: malformed ID 400、forbidden 403、missing/cross-app/inactive/suggestion mismatch 404/422、stale/in-use 409、DB failure rollback。
- Implementation pseudocode:

```java
getMapping(resourceId).andExpect(jsonPath("$.data.suggestedPermissionCode").value("system:user:read"));
putMapping(resourceId, samePermission, version).andExpect(jsonPath("$.data.changed").value(false));
givenActiveDirectGrant(resourceId); putDifferentPermission().andExpect(status().isConflict());
givenApiBinding(routeId, apiId).andActiveGrant(routeId); putDifferentPermission(apiId).andExpect(status().isConflict());
assertRequiredPermissionAndVersionUnchangedAfterConflictOrInjectedFailure();
```

- Verification contribution: `TEST-007`–`010`,`026` mapping 部分。
- After this file: tests因目标 vertical slice 不存在而 RED。

#### File 2 — `CREATE egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/authorization/permission/{domain/dto/UpdateResourcePermissionMappingRequestDTO.java,domain/vo/ResourcePermissionMappingVO.java,repository/ResourcePermissionMappingRepository.java}`

- Purpose: 固定专用 request/view 和 resource lock/in-use repository contract。
- Symbols: request record、mapping/read result records、repository `loadForUpdate/countActiveUsage/updateActualMapping`。
- Repository evidence: 现有 PermissionPO/ResourcePO/PermissionController 使用 decimal string wire 和 optimistic version；common DTO validation 风格已存在。
- Dependencies and consumers: Step 3 ResourcePO/Binding/Grant ports；File 3 service/controller；Admin Web Drawer。
- Why now: 先定义不暴露 generic Resource update 的最小管理边界。
- Contract/signature changes: request仅 permissionId、expectedResourceVersion、optional reason；resource/application/status/code均服务端读取。
- Input/output and state mapping: DB IDs/status/version/count -> VO strings/enums；reason trim 1–500；permission nullable仅 GET unconfigured。
- Error and edge behavior: missing/blank/negative输入 fail fast；in-use count按 distinct active role 计算；同值不要求 count 为零。
- Implementation pseudocode:

```java
record UpdateResourcePermissionMappingRequestDTO(String permissionId, long expectedResourceVersion, String reason) {
  validate decimalPositive(permissionId); require expectedResourceVersion >= 0; reason = optionalTrim(reason, 500);
}
repository.loadForUpdate(resourceId) returns resource + actual Permission projection;
repository.countActiveUsage(resourceId, type, now) unions direct grant roles and reverse binding source-grant roles;
repository.updateActualMapping(resource, permission, actor, reason) persists one ResourcePO version change and audit;
```

- Verification contribution: File 1 validation/JSON/transaction fixture。
- After this file: mapping types/port可编译；无 Controller/写实现。

#### File 3 — `CREATE egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/authorization/permission/{repository/jpa/JpaResourcePermissionMappingRepository.java,service/ResourcePermissionMappingService.java,controller/ResourcePermissionMappingController.java}`

- Purpose: 实现管理员读取、资源锁、权限校验、in-use guard、audit/version/invalidation。
- Symbols: `get`, `update`, JPA batch/count queries。
- Repository evidence: Global Resource/Permission JPA queries与 authorization state publisher已经存在；统一异常 mapper 可映射409/422。
- Dependencies and consumers: File 2、CurrentRbac3User、PermissionPO、RoleResourceGrant/Binding PO、version publisher。
- Why now: RED tests与 repository contract已固定。
- Contract/signature changes: `@RequiresPermission(system:resource-permission:read/manage)`；返回 common ResultRecord，不复用 ApiEnvelope。
- Input/output and state mapping: trusted current actor授权平台操作；catalog仍global；real change更新 requiredPermissionId/resource version/policy version，after-commit失效。
- Error and edge behavior: API suggestion存在时 selected permission code必须相等；same ID no-op；different且usage>0先409；任一 DB异常 rollback mapping/audit。
- Implementation pseudocode:

```java
@Transactional update(resourceId, request, actor):
  resource = repository.lock(resourceId); require resource.version == expected;
  permission = repository.findActivePermission(request.permissionId); require same application;
  if (resource.type == API && suggestion != null && suggestion != permission.code) throw MappingInvalid;
  if (Objects.equals(actualId, permission.id)) return noOpMapping(resource, activeUsageCount);
  usage = repository.countActiveUsage(resource.id, resource.type, now); if (usage > 0) throw MappingInUse;
  resource.confirmActualPermission(permission.id); writeAudit(old, new, reason, actor);
  advancePolicyVersionAndPublishAfterCommit(resource.applicationId); return changedMapping;
```

- Verification contribution: File 1 GREEN，`TEST-007`–`010`,`026`。
- After this file: API-003/004 完整；CI旧逻辑仍需 Step 6 禁止写 actual。

#### File 4 — `MODIFY egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/authorization/resource/{domain/vo/ResourceVO.java,service/GlobalResourceCatalogService.java,controller/ApplicationResourceController.java}`

- Purpose: 从 generic Resource CRUD 去掉低层 actual mapping 写入，把管理员映射强制收敛到 API-003/004。
- Symbols: ResourceVO fields、create/update assembler、Controller request mapping。
- Repository evidence: current ResourceVO 暴露 requiredPermissionId，generic resource controller/service 可以绕过 mapping in-use guard。
- Dependencies and consumers: Mapping API、ResourceCatalogPage、CI registration；Resource type/status/mechanical facts CRUD 保留。
- Why now: 专用写口可用后才能关闭旧低层入口。
- Contract/signature changes: generic update不接受/写 requiredPermissionId；read只提供 mappingStatus 或引导 mapping API，不泄露 role page。
- Input/output and state mapping: generic mechanical/display/status fields -> ResourcePO；actual mapping字段保持原值。
- Error and edge behavior: caller提交未知 requiredPermissionId按统一 unknown-field/DTO 策略拒绝；旧 direct mapping调用无 alias。
- Implementation pseudocode:

```java
remove requiredPermissionId from generic create/update request mapping;
GlobalResourceCatalogService preserves entity.getRequiredPermissionId() on every non-mapping mutation;
ResourceVO emits mappingStatus = configured ? CONFIGURED : UNCONFIGURED, not mutable permission identity;
Controller routes mapping read/write only through ResourcePermissionMappingController;
contract test posts old field and asserts validation rejection plus unchanged ResourcePO mapping;
```

- Verification contribution: `REQ-004` 单写口静态/contract证明。
- After this file: generic Resource 路径不能绕过 mapping service；Admin Drawer为唯一控制面。

- Validation working directory: `egon-cola-platforms/egon-cola-platform-rbac3`
- Verification command: `mvn -pl egon-cola-platform-rbac3-admin -am -Dsurefire.failIfNoSpecifiedTests=false -Dtest=ResourcePermissionMappingControllerTest,ResourcePermissionMappingServiceTest,JpaResourcePermissionMappingRepositoryIT,ResourceCrudControllerTest test`
- Expected result: exit 0；mapping所有 focused tests GREEN；generic update 无 requiredPermissionId 写分支。
- Failure returns to: File 1契约、File 2边界、File 3查询/事务、File 4旧入口；若产品要求绕过在用保护，返回 Spec。
- Completion criteria: actual mapping admin-only、同值幂等、在用不同映射409、版本/失效可见、common wrapper完整。
- Rollback: revert本 Step；V13未加入，schema未改变。
- Commit paths: `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/authorization/permission/{ResourcePermissionMappingControllerTest.java,ResourcePermissionMappingServiceTest.java,JpaResourcePermissionMappingRepositoryIT.java}` ; `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/authorization/permission/{domain/dto/UpdateResourcePermissionMappingRequestDTO.java,domain/vo/ResourcePermissionMappingVO.java,repository/ResourcePermissionMappingRepository.java}` ; `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/authorization/permission/{repository/jpa/JpaResourcePermissionMappingRepository.java,service/ResourcePermissionMappingService.java,controller/ResourcePermissionMappingController.java}` ; `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/authorization/resource/{domain/vo/ResourceVO.java,service/GlobalResourceCatalogService.java,controller/ApplicationResourceController.java}`
- Commit: `feat(rbac3-admin): add guarded resource permission mappings`

### Step 6 — 将 CI report 改为 registration 并原子维护 suggestion 与 API bindings

- Requirements: `REQ-009`, `REQ-010`, `REQ-016`, `REQ-018`, `REQ-020`, `REQ-021`, `REQ-026`
- Dependencies: `Steps 3, 5`
- Baseline state: 已移动的 CI 类仍叫 Report、使用旧 IAM URL/permissionCode，并可能覆盖 Resource.requiredPermissionId；binding port 无 JPA 实现。
- Observable outcome: API-006 只写机械事实、suggestedPermissionCode 和同 APP ROUTE/ACTION→API binding；actual mapping/grants/tenant facts 零变化。
- End state: 唯一 registration URL、类名与脚本完成；相同 build/checksum 幂等，非法图整批零写；浏览器无 report client。
- Test-first gate: Required — 先修改 Node/Controller/store tests，使旧字段/URL/actual mapping写入导致 RED。
- Ordered files:

#### File 1 — `RENAME egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/registration/ci/{CiResourceReportControllerTest.java->CiResourceRegistrationControllerTest.java,CiResourceReportServiceIT.java->CiResourceRegistrationServiceIT.java}`

- Purpose: 固定新命名/URL、suggestion/binding canonicalization、零写边界和幂等。
- Symbols: SERVICE auth/source checks、same-app/type graph、shared API、binding replace/remove、actual/grant before-after checks。
- Repository evidence: current CI tests 已覆盖 SERVICE scope、checksum、幂等和 catalog counts，可在同 fixtures扩展。
- Dependencies and consumers: 新 Registration types/service/store、ResourceApiBinding JPA、DDC catalog gateway。
- Why now: 先让旧 report 行为和 URL 明确失败。
- Contract/signature changes: PUT `/api/rbac3/v1/registration/businesses/{businessCode}/applications/{applicationCode}/frontend-resources`；resource使用 suggestedPermissionCode/apiResourceCodes。
- Input/output and state mapping: complete request -> normalized checksum -> Resource/Field suggestion/binding diff -> committed counts；actual/grants snapshot before=after。
- Error and edge behavior: spoof source/checksum/duplicate/cycle/cross-app/non-API target 4xx且零写；same build different checksum 409；store failure rollback。
- Implementation pseudocode:

```java
request.resources[ROUTE].apiResourceCodes = List.of("iam.api.users.list");
putNewRegistrationUrl(servicePrincipal).andExpect(successWith("apiBindingsAdded", 1));
assertResource(resource).suggestedPermissionCodeEquals("system:user:read");
assertActualPermissionAndRoleGrantChecksumsUnchanged(before);
putBindingToOtherApplicationOrRouteTarget().andExpect(status().isUnprocessableEntity());
assertNoResourceFieldBindingOrHeadWritesAfterRejectedRequest();
```

- Verification contribution: `TEST-012`–`014`,`030`,`031` backend registration。
- After this file: tests因旧 URL/DTO/store行为而 RED。

#### File 2 — `MODIFY egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin-web/scripts/{report-rbac-resources.test.mjs,report-rbac-resources.mjs}`

- Purpose: 先固定并实现 Node canonical projection 与唯一 registration URL。
- Symbols: `projectReport`, `canonicalChecksum`, `reportResources`。
- Repository evidence: current script读取 checked-in JSON、稳定排序SHA-256、用短期 SERVICE AT且在browser graph之外。
- Dependencies and consumers: release pipeline、API-006；`resourceDefinitions.json` 当前 permission暂映射为 suggestion，Step 8切最终字段名。
- Why now: server DTO canonicalizer必须和唯一 CI producer使用相同字段/排序fixture。
- Contract/signature changes: output field `suggestedPermissionCode`，ROUTE/ACTION output sorted unique `apiResourceCodes`；endpoint去掉 `/iam/resource-catalog`。
- Input/output and state mapping: local definitions -> component-free resources/fields -> checksum/body；SERVICE token只进 Authorization header。
- Error and edge behavior: unknown/duplicate API codes由server拒绝；Node先校验array/string/duplicates；HTTP/business failure令 process非零且不输出token/body。
- Implementation pseudocode:

```javascript
const projected = definitions.filter(nonField).map(def => ({
  type: def.kind, code: def.code, name: def.name, parentCode: def.parentCode ?? null,
  suggestedPermissionCode: def.suggestedPermissionCode ?? def.permission ?? null,
  apiResourceCodes: [...new Set(def.apiResourceCodes ?? [])].sort(),
  path: def.path ?? null, componentKey: def.componentKey ?? null, routeCode: def.routeCode ?? null,
}));
checksum canonical order includes sorted apiResourceCodes and suggestedPermissionCode;
PUT `/api/rbac3/v1/registration/businesses/${biz}/applications/${app}/frontend-resources`;
```

- Verification contribution: `TEST-011`,`013`,`029` CI producer/bundle boundary。
- After this file: Node tests GREEN；script仍能读取当前 definitions，最终移除 legacy source field由 Step 8完成。

#### File 3 — `RENAME egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/registration/ci/{controller/CiResourceReportController.java->controller/CiResourceRegistrationController.java,domain/dto/CiResourceReportRequestDTO.java->domain/dto/CiResourceRegistrationRequestDTO.java,domain/vo/CiResourceReportResultVO.java->domain/vo/CiResourceRegistrationResultVO.java,service/CiResourceReportCanonicalizer.java->service/CiResourceRegistrationCanonicalizer.java,service/CiResourceReportService.java->service/CiResourceRegistrationService.java,service/CiResourceReportStore.java->service/CiResourceRegistrationStore.java,service/JpaCiResourceReportStore.java->service/JpaCiResourceRegistrationStore.java}`

- Purpose: 统一 registration 命名并实现新 request/result/canonicalization/transaction。
- Symbols: renamed classes、Resource record fields、result pendingMapping/apiBindings counts。
- Repository evidence: 8个现有类已形成 Controller→Service→Store，不需新模块或额外 layer。
- Dependencies and consumers: DDC catalog、SERVICE principal/scope、ResourcePO/FieldDefinitionPO、Step 3 binding port、common ResultRecord。
- Why now: Node/server RED契约已固定，沿用现有单 transaction结构最小修改。
- Contract/signature changes: 新 URL/DTO；删除 permissionCode actual语义；新增 suggestion/apiResourceCodes；类名不再包含 Report。
- Input/output and state mapping: service source Biz/App与path逐项匹配；canonical set解析 API IDs；同事务upsert mechanical facts/suggestion/fields/bindings/head/audit。
- Error and edge behavior: 不写 requiredPermissionId、RoleResourceGrant/TenantApplication/UserBusinessAccess/FieldRule；任何 validation/store异常rollback；幂等重放不推进version。
- Implementation pseudocode:

```java
validateServicePrincipalSource(principal, businessCode, applicationCode);
catalog = ddcCatalog.requireEnabledHierarchy(businessCode, applicationCode);
canonical = canonicalizer.normalizeAndVerifyChecksum(request);
resolved = store.resolveResourcesByApplicationAndCode(canonical.allCodes());
validate sources are ROUTE/ACTION and targets are same-app API;
@Transactional store.replaceMechanicalFactsAndBindings(catalog.applicationId, build, checksum, canonical);
preserve ResourcePO.requiredPermissionId and all grant/tenant/policy tables; update suggestion only;
return committed diff counts in ResultRecord;
```

- Verification contribution: File 1 GREEN、`TEST-012`–`014`,`030`,`031`。
- After this file: server CI registration contract完成；binding repository JPA细节在 File 4。

#### File 4 — `CREATE egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/authorization/resource/apibinding/repository/jpa/JpaResourceApiBindingRepository.java`

- Purpose: 在 registration transaction内批量解析、校验、replace bindings并支持 mapping/runtime反向查询。
- Symbols: Step 3 port所有方法、batch JPQL/SQL queries。
- Repository evidence: current Jpa CI store使用EntityManager batch lookup/upsert；ResourcePO含 application/type/status/source fields。
- Dependencies and consumers: Registration store、Mapping Service、Step 7 runtime fact repository。
- Why now: registration已提供完整 canonical binding set，JPA实现才能保持同一事务原子性。
- Contract/signature changes: 无HTTP；global application维度，无tenant列/参数。
- Input/output and state mapping: pairs -> active rows；旧CI pairs缺失 -> STALE/DISABLED；反向count distinct active role；shared API多source不去重source row。
- Error and edge behavior: type/app/status再次权威校验；unique conflict/foreign key异常映射整个 registration rollback；空集合清理旧CI bindings。
- Implementation pseudocode:

```java
resources = batchLoadByIds(sourceIds union apiIds);
for pair: require source.applicationId == api.applicationId == requestedApp;
          require source.type in ROUTE/ACTION && api.type == API;
current = loadCiOwnedBindingsForApplication(appId);
disable(currentPairs - requestedPairs); persist(requestedPairs - currentPairs); retain intersection;
apiIdsForSources returns distinct ACTIVE target IDs;
countDistinctActiveRolesDerivingApi joins ACTIVE binding sources to ACTIVE current-window role_resource_grant;
```

- Verification contribution: registration IT、mapping reverse guard、runtime union tests。
- After this file: ResourceApiBinding完整 persistence实现可被 Steps 5/7消费。

- Validation working directory: `egon-cola-platforms/egon-cola-platform-rbac3`
- Verification command: `mvn -pl egon-cola-platform-rbac3-admin -am -Dsurefire.failIfNoSpecifiedTests=false -Dtest=CiResourceRegistrationControllerTest,CiResourceRegistrationServiceIT,ResourcePermissionMappingServiceTest test && cd egon-cola-platform-rbac3-admin-web && npm ci --legacy-peer-deps --no-audit --no-fund && npm run test:report && npm run build && npm run verify:bundle`
- Expected result: Maven与Node命令exit 0；request/body无 `permissionCode` actual字段；旧 CI URL/source package零命中；bundle guard确认脚本/Scope/token配置不在dist graph。
- Failure returns to: File 1/2 canonical contract、File 3 transaction/source校验、File 4 batch/type/app约束；新增交互或startup上报需求返回 Spec。
- Completion criteria: CI registration幂等且只写批准机械事实/suggestion/bindings；所有授权/tenant表before-after相同。
- Rollback: revert Step 6；旧 URL会恢复但尚未执行V13，数据未迁移。
- Commit paths: `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/registration/ci/{CiResourceReportControllerTest.java->CiResourceRegistrationControllerTest.java,CiResourceReportServiceIT.java->CiResourceRegistrationServiceIT.java}` ; `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin-web/scripts/{report-rbac-resources.test.mjs,report-rbac-resources.mjs}` ; `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/registration/ci/{controller/CiResourceReportController.java->controller/CiResourceRegistrationController.java,domain/dto/CiResourceReportRequestDTO.java->domain/dto/CiResourceRegistrationRequestDTO.java,domain/vo/CiResourceReportResultVO.java->domain/vo/CiResourceRegistrationResultVO.java,service/CiResourceReportCanonicalizer.java->service/CiResourceRegistrationCanonicalizer.java,service/CiResourceReportService.java->service/CiResourceRegistrationService.java,service/CiResourceReportStore.java->service/CiResourceRegistrationStore.java,service/JpaCiResourceReportStore.java->service/JpaCiResourceRegistrationStore.java}` ; `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/authorization/resource/apibinding/repository/jpa/JpaResourceApiBindingRepository.java`
- Commit: `feat(rbac3-registration): register suggestions and page API bindings`

### Step 7 — 切换运行时资源并集、V13、Bootstrap 与历史残留

- Requirements: `REQ-004`, `REQ-005`, `REQ-008`, `REQ-011`, `REQ-012`, `REQ-013`, `REQ-017`, `REQ-018`, `REQ-019`, `REQ-020`, `REQ-021`, `REQ-026`
- Dependencies: `Steps 4, 5, 6`
- Baseline state: 管理面新模型可编译，但 Core/Admin runtime/Bootstrap仍读取 RolePermission；数据库仍V12且旧表存在；重复 principal/envelope旧类仍残留。
- Observable outcome: V1→V13 clean schema和新 runtime链一致；有效root/继承/binding做OR union后派生permissions/resourceCodes；内置管理员bootstrap新grant；旧表/类/URL/FQCN为零。
- End state: 后端完整切换并可进行模块集成测试；V13后旧二进制不兼容，后续只 forward-fix。
- Test-first gate: Required — 先改 Core/runtime/migration/bootstrap/architecture tests，分别因旧 fact、缺表、旧类和bootstrap无grant而 RED。
- Ordered files:

#### File 1 — `MODIFY egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-{core,admin}/src/test/java/top/egon/cola/platform/rbac3/{core/activation/DefaultRoleActivationResolverTest.java,core/decision/AuthorizationMergeAlgebraTest.java,admin/authorization/runtime/UserAuthorizationSnapshotProjectorTest.java,admin/repository/Rbac3FlywayPostgresqlIT.java,admin/repository/Rbac3MigrationContractTest.java,admin/bootstrap/Rbac3PlatformAdminBootstrapCliIT.java,admin/architecture/Rbac3AuthorizationArchitectureTest.java}`

- Purpose: 固定 effective roots/API union、父MENU展示但不派生permission、V13 schema、bootstrap readiness与零旧残留。
- Symbols: ResourceGrantBinding fixtures、shared API撤销、ROUTE/ACTION边界、V13 metadata assertions、bootstrap grant smoke。
- Repository evidence: 这些现有 tests 分别覆盖 Core algebra、projector、PostgreSQL Flyway、CLI bootstrap和架构残留。
- Dependencies and consumers: 新 PO/repositories、V13、Core facts/projector/bootstrap implementation。
- Why now: 唯一破坏式切换前必须让所有层的失败点同时可见。
- Contract/signature changes: Core fixture由 permission binding改 resource grant facts；migration要求新表/列/constraints且旧表absent。
- Input/output and state mapping: active effective role roots + bindings + resource mapping -> permissions/resourceCodes；schema V12 -> V13；empty grant -> bootstrap grants。
- Error and edge behavior: inactive/unmapped/stale/cross-app facts排除并触发一致性失败；仅父MENU不增加permission；bootstrap失败 readiness false。
- Implementation pseudocode:

```java
facts = resourceGrants(routeRoot, actionRoot, directApi).withBindings(routeToListApi, actionToDisableApi);
assertSnapshot(routeOnly).containsResource(listApi).doesNotContainResource(disableApi);
assertSharedApiRemainsWhileAnySourceOrDirectGrantExists(); assertRemovedAfterLastSourceRevoked();
migrateCleanDatabase(V1_TO_V13); assertTablesPresent("rbac3_role_resource_grant", "rbac3_resource_api_binding");
assertTablesAbsent("rbac3_role_permission", "rbac3_permission_resource");
runPlatformAdminBootstrap(); assertRequiredActiveResourceGrantsAndReadiness();
```

- Verification contribution: `TEST-023`–`032`、`TEST-027`–`029`。
- After this file: tests因生产fact/schema/bootstrap尚未切换而RED。

#### File 2 — `MODIFY egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-{core,admin}/src/main/java/top/egon/cola/platform/rbac3/{core/{activation/AuthorizationRuleFacts.java,activation/ActivationAuthorizationSnapshot.java,decision/PermissionSetMerger.java,decision/UserAuthorizationSnapshotBuilder.java},admin/authorization/runtime/{activation/repository/jpa/JpaRoleActivationFactRepository.java,snapshot/service/UserAuthorizationSnapshotProjector.java}}`

- Purpose: 把运行事实从 role-permission 改为有效资源root + API union，并生成同一 snapshot 的 permission/resource集合。
- Symbols: `AuthorizationRuleFacts.ResourceGrantBinding`、Core builder/merger、JPA fact query、projector。
- Repository evidence: current `PermissionBinding(roleId,permissionCode)`来自 role_permission；AppAuthorizationContext/ActivationSnapshot已含 resourceCodes。
- Dependencies and consumers: RoleResourceGrant/ResourceApiBinding/Resource/Permission表；Starter/About/AuthorizationService；active role closure。
- Why now: RED algebra已固定纯函数结果，且管理面写事实已完成。
- Contract/signature changes: fact包含 roleId/resourceId/code/type/permissionCode；不携带source provenance；projector排序去重。
- Input/output and state mapping: active/effective role IDs -> current-window direct/inherited roots -> binding API union -> actual ACTIVE permission -> resource/permission sets；父MENU只补resource。
- Error and edge behavior: PENDING/STALE/unmapped/inactive/cross-app不进入；binding target失效使一致性检查fail closed；Data/Field/SOD/Fence merger输入继续用派生permission set。
- Implementation pseudocode:

```java
directRoots = query ACTIVE grants for effectiveRoleIds and current window;
effectiveRoots = union(directRoots, inheritedRoleRoots);
derivedApis = bindingRepository.apiIdsForSources(ROUTE/ACTION roots);
effectiveGrantable = union(effectiveRoots, derivedApis, direct API roots);
permissions = distinct actual ACTIVE permissionCode for effectiveGrantable;
resourceCodes = codes(effectiveGrantable) union navigationAncestorsOfRoutes(effectiveGrantable);
never add ancestor MENU.requiredPermissionCode to permissions;
merge existing dataScopes/fieldRules/SOD/Fence using the derived permissions and existing fail-closed paths;
```

- Verification contribution: Core/runtime `TEST-020`–`024`,`031`,`032` GREEN。
- After this file: runtime source code不再读 role_permission；database migration尚在 File 4。

#### File 3 — `MODIFY egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/bootstrap/repository/jpa/{JpaDevelopmentTopologyBootstrapRepository.java,JpaPlatformAdminBootstrapRepository.java}`

- Purpose: Bootstrap built-in管理员映射到actual resources并写新RoleResourceGrant，而非 permission rows。
- Symbols: `ensurePlatformAdminResourceGrants`, required resource code inventory、readiness checks。
- Repository evidence: 两个 repository 当前创建 Permission/RolePermission；V12 seed已提供about permission，主Spec要求V13后bootstrap grant。
- Dependencies and consumers: RoleResourceGrantPO、ResourcePO actual mapping、Application/Role/TenantApplication、CLI/Readiness。
- Why now: runtime已切新事实；V13启动后必须在ready前恢复内置管理访问。
- Contract/signature changes: CLI外部参数不变；内部 bootstrap选择稳定resource codes，不反推旧permission IDs。
- Input/output and state mapping: built-in role + required active mapped resources -> idempotent ACTIVE grant rows；重复运行no-op；推进必要versions。
- Error and edge behavior: resource不存在/未映射/permission非ACTIVE则bootstrap失败并保持readiness false；不静默创建actual mapping。
- Implementation pseudocode:

```java
role = requireBuiltInPlatformAdminRole(tenant, application);
resources = requireActiveMappedResources(REQUIRED_PLATFORM_ADMIN_RESOURCE_CODES);
for resource in resources:
  if (!activeGrantExists(role.id, resource.id, now)) persist(RoleResourceGrantPO.activate(...));
advanceAuthorizationVersionOnceWhenChanged();
readiness = all required resource grants active && snapshot build succeeds;
do not persist RolePermissionPO or infer mapping from permission equality;
```

- Verification contribution: `TEST-028` CLI/bootstrap/readiness。
- After this file: 新bootstrap可在V13空grant表恢复平台管理员；旧RolePermission bootstrap引用消失。

#### File 4 — `CREATE egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/resources/db/migration/V13__replace_role_permissions_and_add_resource_api_bindings.sql`

- Purpose: 一次性创建新表/约束/索引、增加suggestion、修正unmapped ACTIVE状态并删除两个旧表。
- Symbols: `rbac3_role_resource_grant`, `rbac3_resource_api_binding`, `suggested_permission_code` and named constraints/indexes。
- Repository evidence: V12是最新；V1/V7含旧表/FK；项目使用PostgreSQL Flyway不可变迁移。
- Dependencies and consumers: Files 2/3 JPA/runtime/bootstrap；migration tests；维护窗口。
- Why now: 只有所有新读写代码已存在，才可破坏式drop旧表。
- Contract/signature changes: 数据库schema breaking；不迁/导出RolePermission或PermissionResource rows。
- Input/output and state mapping: V12 schema -> V13；new tables empty；suggestion null；unmapped ACTIVE grantable resource先转PENDING_VALIDATION；bootstrap随后建grant。
- Error and edge behavior: transaction失败令Flyway失败/readiness false；不使用trigger推导cross-app/type；FK/UK/check/index按Spec §11；旧二进制禁止启动。
- Implementation pseudocode:

```sql
CREATE TABLE rbac3_role_resource_grant (... tenant_id, application_id, role_id, resource_id,
  valid_from timestamptz NOT NULL, valid_to timestamptz, status varchar(32), version bigint, audit ...);
ADD unique fact constraint and role/status/window plus resource/status indexes; add role/resource FKs;
CREATE TABLE rbac3_resource_api_binding (... application_id, source_resource_id, api_resource_id,
  source_build_id, source_checksum, status, version, audit ...);
ADD unique(source_resource_id, api_resource_id), application/source and api/status indexes/FKs/checks;
ALTER TABLE rbac3_resource ADD COLUMN suggested_permission_code varchar(256);
UPDATE grantable ACTIVE resources without required_permission_id SET status='PENDING_VALIDATION';
DROP TABLE rbac3_role_permission; DROP TABLE rbac3_permission_resource;
```

- Verification contribution: `TEST-025` clean PostgreSQL migration；immutability gate。
- After this file: schema与新JPA模型一致；数据库层无旧关系。

#### File 5 — `DELETE egon-cola-platforms/egon-cola-platform-rbac3/{egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/**/{RolePermissionPO.java,RolePermissionStatusEnum.java,AssignPermissionCommandDTO.java,AssignPermissionsCommandDTO.java,RemovePermissionCommandDTO.java,BindPermissionsRequestDTO.java,PermissionResourcePO.java,CurrentRbac3Principal.java,RequiresRbac3Permission.java,Rbac3AdminAuthenticationToken.java,Rbac3AdminPrincipalFilter.java,ApiEnvelopeVO.java},egon-cola-platform-rbac3-contract/src/main/java/top/egon/cola/platform/rbac3/contract/error/Rbac3ErrorResponse.java}`

- Purpose: 删除已替代的关系、安全principal/annotation和重复响应类型，并同步所有调用方用现有标准边界。
- Symbols: 列举旧类及其 imports/constructor/exception response consumers。
- Repository evidence: 当前 `rg` 命中 controllers/services/tests；前置 Accepted Spec已创建 Rbac3UserDetails/CurrentRbac3User/common Result替代。
- Dependencies and consumers: 所有Admin controllers、shared exception handler、Contract/SDK error types、architecture tests。
- Why now: 新 runtime/V13/Result/UserDetails路径全部可用后再删，避免编译空窗。
- Contract/signature changes: Controllers直接用 `@RequiresPermission`/CurrentRbac3User；HTTP success/error用ResultRecord；旧annotation alias和envelope不保留。
- Input/output and state mapping: SecurityContext Rbac3UserDetails -> CurrentRbac3User；domain result/error -> common ResultRecord；无Header/参数 principal透传。
- Error and edge behavior: 非Rbac3UserDetails fail closed 401/403；统一exception handler填code/status/message/data null/trace/timestamp；旧 JSON/类编译消费者必须同版本更新。
- Implementation pseudocode:

```text
replace every RequiresRbac3Permission(permission=code) with starter RequiresPermission(code);
replace CurrentRbac3Principal method/field access with CurrentRbac3User.require() inside service boundary;
replace ApiEnvelopeVO/Rbac3ErrorResponse construction with common ResultRecord factories and shared exception mapping;
delete old security filter/token because Starter filter already establishes Rbac3AuthenticationToken/UserDetails;
delete old PO/DTO/enum and remove all JPQL/entity references after V13 reader/writer cutover;
rg production/test/generated artifacts for every deleted simple/FQCN and require zero matches except historical docs;
```

- Verification contribution: `TEST-027`,`029` cleanup与`REQ-008`,`016` regression。
- After this file: source/JAR不含旧关系/principal/envelope；所有Controller编译到标准边界。

- Validation working directory: `egon-cola-platforms/egon-cola-platform-rbac3`
- Verification command: `mvn -pl egon-cola-platform-rbac3-core,egon-cola-platform-rbac3-starter,egon-cola-platform-rbac3-admin -am verify && ! rg -n "RolePermission|PermissionResource|CurrentRbac3Principal|RequiresRbac3Permission|ApiEnvelopeVO|Rbac3ErrorResponse|rbac3_role_permission|rbac3_permission_resource" egon-cola-platform-rbac3-{contract,core,starter,admin}/src/main/java`
- Expected result: Maven verify exit 0，PostgreSQL/Flyway IT与bootstrap/architecture tests通过；最终rg exit 1（零源码命中）；V13是唯一新增migration。
- Failure returns to: File 1期望、File 2 query/algebra、File 3 bootstrap、File 4 DDL、File 5 consumer cleanup；live数据/备份问题属于部署门禁，不修改历史migration。
- Completion criteria: `TEST-023`–`032` 后端范围全部GREEN；新schema/runtime/bootstrap一致，旧能力无残留；Data/Field/SOD/Fence回归通过。
- Rollback: 源码提交可revert仅限V13未应用环境；V13一旦应用，只能维护窗口恢复备份或forward-fix，不能启动旧二进制。
- Commit paths: `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-{core,admin}/src/test/java/top/egon/cola/platform/rbac3/{core/activation/DefaultRoleActivationResolverTest.java,core/decision/AuthorizationMergeAlgebraTest.java,admin/authorization/runtime/UserAuthorizationSnapshotProjectorTest.java,admin/repository/Rbac3FlywayPostgresqlIT.java,admin/repository/Rbac3MigrationContractTest.java,admin/bootstrap/Rbac3PlatformAdminBootstrapCliIT.java,admin/architecture/Rbac3AuthorizationArchitectureTest.java}` ; `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-{core,admin}/src/main/java/top/egon/cola/platform/rbac3/{core/{activation/AuthorizationRuleFacts.java,activation/ActivationAuthorizationSnapshot.java,decision/PermissionSetMerger.java,decision/UserAuthorizationSnapshotBuilder.java},admin/authorization/runtime/{activation/repository/jpa/JpaRoleActivationFactRepository.java,snapshot/service/UserAuthorizationSnapshotProjector.java}}` ; `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/bootstrap/repository/jpa/{JpaDevelopmentTopologyBootstrapRepository.java,JpaPlatformAdminBootstrapRepository.java}` ; `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/resources/db/migration/V13__replace_role_permissions_and_add_resource_api_bindings.sql` ; `egon-cola-platforms/egon-cola-platform-rbac3/{egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/**/{RolePermissionPO.java,RolePermissionStatusEnum.java,AssignPermissionCommandDTO.java,AssignPermissionsCommandDTO.java,RemovePermissionCommandDTO.java,BindPermissionsRequestDTO.java,PermissionResourcePO.java,CurrentRbac3Principal.java,RequiresRbac3Permission.java,Rbac3AdminAuthenticationToken.java,Rbac3AdminPrincipalFilter.java,ApiEnvelopeVO.java},egon-cola-platform-rbac3-contract/src/main/java/top/egon/cola/platform/rbac3/contract/error/Rbac3ErrorResponse.java}`
- Commit: `feat(rbac3-runtime): cut over resource grants with V13`

### Step 8 — 让 React SDK 用 resourceCodes 控制导航、路由和按钮

- Requirements: `REQ-007`, `REQ-008`, `REQ-009`, `REQ-019`, `REQ-021`
- Dependencies: `Steps 1, 7`
- Baseline state: About TS type无resourceCodes；FrontendResourceRegistry/ActionGuard按permission；local definitions仍以permission作为运行字段。
- Observable outcome: MENU/ROUTE/ACTION展示只查 resourceCodes；FIELD继续查fieldPolicies；generic PermissionGuard仍为高级内部能力；CI suggestion字段不参与展示。
- End state: SDK typecheck/tests/build通过；unknown/not READY/resource absent均fail closed；script最终不再兼容旧definition.permission。
- Test-first gate: Required — 修改现有 registry/guard/provider tests，先因缺resourceCodes与仍读permissions而RED。
- Ordered files:

#### File 1 — `MODIFY egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-react-sdk/src/{registry/FrontendResourceRegistry.test.ts,guards/Rbac3Guards.test.tsx,provider/Rbac3Provider.test.tsx,types.test.ts}`

- Purpose: 固定resourceCodes导航/route/action、notREADY隐藏和Field不变。
- Symbols: registry navigation/canAccessRoute、ActionGuard resource prop、About fixtures、type shape tests。
- Repository evidence: 现有 Vitest覆盖registry validation、PermissionGuard/ActionGuard/FieldGuard和Provider状态机。
- Dependencies and consumers: target SDK types/registry/guards；React Testing Library。
- Why now: 先证明即使 permissions含字符，只要resourceCodes缺少目标也必须隐藏。
- Contract/signature changes: About fixture新增resourceCodes；local resource definition运行授权字段由code本身决定，suggestion只给CI。
- Input/output and state mapping: about READY resourceCodes + local definitions -> visible recursive nodes/route/action render；fieldPolicies -> FieldGuard。
- Error and edge behavior: duplicate/unknown/cycle继续拒绝；parent MENU仅在可见后代时出现；hidden route可deep link；notREADY/absent resource隐藏。
- Implementation pseudocode:

```typescript
const about = fixture({permissions: ['system:admin:*'], resourceCodes: ['iam', 'iam.users']})
expect(registry.navigation(about)).toContainRoute('iam.users')
expect(registry.canAccessRoute('iam.roles', about)).toBe(false)
render(<ActionGuard resourceCode="iam.user.create">create</ActionGuard>, readyAbout(about))
expect(screen.queryByText('create')).not.toBeInTheDocument()
expect(getField('user.email', about.fieldPolicies).level).toBe('MASKED_READ')
```

- Verification contribution: `TEST-015`,`016`,`018`,`031`,`032` browser authorization部分。
- After this file: tests因production仍按permissions而RED。

#### File 2 — `MODIFY egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-react-sdk/src/{types.ts,registry/FrontendResourceRegistry.ts,guards/ActionGuard.tsx,index.ts}`

- Purpose: 实现About resourceCodes类型和本地资源guard，不创建第二store/provider。
- Symbols: `Rbac3AboutView.resourceCodes`, `FrontendResourceDefinition.suggestedPermissionCode/apiResourceCodes`, `canAccessResource`, ActionGuard props/export。
- Repository evidence: AppAuthorizationContext TS已有resourceCodes；Registry是本地唯一源；ActionGuard当前只是PermissionGuard别名。
- Dependencies and consumers: Rbac3Provider、RBAC3 Admin Web、CI definitions/script；generic PermissionGuard保留。
- Why now: RED tests固定最终行为和兼容边界。
- Contract/signature changes: registry runtime方法用definition.code；ACTION guard接resourceCode/actionCode解析；serializable输出suggestion/apiResourceCodes但不输出React component。
- Input/output and state mapping: local code + about.resourceCodes membership -> boolean/render；suggested permission仅 serializable/CI；Field policy逻辑不变。
- Error and edge behavior: About未READY由Provider guard fail closed；definition suggestion null合法；apiResourceCodes只能ROUTE/ACTION且去重；permission字符不能影响navigation/action。
- Implementation pseudocode:

```typescript
interface Rbac3AboutView { resourceCodes: readonly string[]; permissions: readonly string[]; /* unchanged policy fields */ }
normalize(def): trim code/name/suggestedPermissionCode; sortUnique(apiResourceCodes ?? [])
navigation(about): build MENU/ROUTE only when about.resourceCodes.includes(def.code); keep visible ancestors
canAccessRoute(code, about): route exists && about.resourceCodes.includes(code)
canAccessResource(code, about): about.resourceCodes.includes(code)
ActionGuard: read READY about and render children only when target resource code is present
serializable: expose mechanical facts, suggestion and apiResourceCodes; never use suggestion for UI access
```

- Verification contribution: File 1 GREEN；SDK public type/build。
- After this file: SDK runtime展示完全resource-based；generic PermissionGuard仍可显式使用。

#### File 3 — `MODIFY egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin-web/{src/app/resourceDefinitions.json,scripts/report-rbac-resources.mjs,scripts/report-rbac-resources.test.mjs}`

- Purpose: 把checked-in定义切到最终 suggestedPermissionCode/apiResourceCodes schema并移除脚本旧permission fallback。
- Symbols: all MENU/ROUTE/ACTION/FIELD entries、project/canonical fixtures。
- Repository evidence: JSON是Registry和CI script共同源；Step 6临时兼容旧key只为保持中间commit可验证。
- Dependencies and consumers: File 2 registry、API-006、Node tests；browser bundle可包含非秘密suggestion但不读取它做授权。
- Why now: SDK已理解最终字段，现可完成破坏式源schema切换并删除fallback。
- Contract/signature changes: `permission` -> `suggestedPermissionCode`；每个ROUTE/ACTION按真实触发源声明apiResourceCodes；FIELD保持field metadata。
- Input/output and state mapping: same routes/actions/fields -> runtime code visibility + CI suggestion/bindings；不改变path/componentKey。
- Error and edge behavior: ACTION-only API不挂ROUTE；共享API可在多个source数组出现；未知API让CI注册422阻断发布。
- Implementation pseudocode:

```jsonc
{ "kind":"ROUTE", "code":"iam.users", "suggestedPermissionCode":"system:user:read",
  "apiResourceCodes":["iam.api.users.list"], "path":"/iam/users", "componentKey":"rbac3-users" }
{ "kind":"ACTION", "code":"iam.user.disable", "routeCode":"iam.users",
  "suggestedPermissionCode":"system:user:disable", "apiResourceCodes":["iam.api.users.disable"] }
```

```javascript
projectReport reads only definition.suggestedPermissionCode and definition.apiResourceCodes;
assert no object/property named permission remains in resourceDefinitions or report projection;
```

- Verification contribution: `TEST-011`,`016`,`030`–`032` source declaration与Node checksum。
- After this file: local source无旧permission key；runtime/CI共享同一最终定义。

- Validation working directory: `egon-cola-platforms/egon-cola-platform-rbac3`
- Verification command: `cd egon-cola-platform-rbac3-react-sdk && npm ci --legacy-peer-deps --no-audit --no-fund && npm run typecheck && npm run test -- --run && npm run build && cd ../egon-cola-platform-rbac3-admin-web && npm ci --legacy-peer-deps --no-audit --no-fund && npm run test:report && npm run build && npm run verify:bundle`
- Expected result: 所有命令exit 0；registry/action tests证明permissions不影响资源展示；Node report无旧permission字段/fallback。
- Failure returns to: File 1期望、File 2 SDK行为、File 3定义真实调用关系；API source关系不清必须修Spec/资源声明owner，不在runtime猜测。
- Completion criteria: About resourceCodes贯通SDK；FIELD/generic permission能力保持；CI source schema最终化。
- Rollback: revert SDK/definitions/scripts；后端仍可运行但新About字段不会被旧SDK使用，发布必须成组回退。
- Commit paths: `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-react-sdk/src/{registry/FrontendResourceRegistry.test.ts,guards/Rbac3Guards.test.tsx,provider/Rbac3Provider.test.tsx,types.test.ts}` ; `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-react-sdk/src/{types.ts,registry/FrontendResourceRegistry.ts,guards/ActionGuard.tsx,index.ts}` ; `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin-web/{src/app/resourceDefinitions.json,scripts/report-rbac-resources.mjs,scripts/report-rbac-resources.test.mjs}`
- Commit: `feat(rbac3-react): guard UI resources with resource codes`

### Step 9 — 将 RBAC3 角色和资源目录页面切到可读资源配置

- Requirements: `REQ-001`, `REQ-002`, `REQ-003`, `REQ-007`, `REQ-013`, `REQ-014`, `REQ-015`, `REQ-016`, `REQ-019`, `REQ-020`, `REQ-021`
- Dependencies: `Steps 4, 5, 6, 8`
- Baseline state: 角色页是逗号permission IDs Modal；ResourceCatalog无mapping Drawer；独立PermissionPage在导航；旧role-permissions route存在。
- Observable outcome: 角色页展示MENU/ROUTE树、ACTION和API关联并只提交direct resourceIds；资源目录提供admin mapping Drawer；独立权限字符导航和旧route删除。
- End state: RBAC3 feature tests/typecheck/build通过；409/422保留用户选择并给出恢复；角色页不渲染任何permission字符。
- Test-first gate: Required — 修改RolePages/ApplicationPages/App integration tests，先因旧文本表单、API和route而RED。
- Ordered files:

#### File 1 — `MODIFY egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin-web/src/features/{role/RolePages.test.tsx,application/ApplicationPages.test.tsx,../app/App.integration.test.tsx}`

- Purpose: 固定可读资源树、mapping Drawer、旧导航缺失和common错误状态。
- Symbols: RoleResourceGrantPage component tests、ResourceCatalog mapping tests、route/deep-link integration。
- Repository evidence: 现有tests用QueryClient/mock FeatureApi验证 RolePermissionPage/PermissionPage/ResourceCatalog。
- Dependencies and consumers: target pages/apis/routes；SDK resource guards；shared PageState。
- Why now: 在重写页面前固定UI状态和请求body，保证不再出现ID文本框/权限字符。
- Contract/signature changes: GET/PUT resources、GET/PUT permission-mapping；route `/iam/roles/:roleId/resources`。
- Input/output and state mapping: tree direct/inherited/derived -> checked/disabled/read-only groups；form selection -> resourceIds/version/window；mapping form -> permissionId/version/reason。
- Error and edge behavior: loading/empty/unconfigured/conflict/denied；derived/inherited disabled；409刷新需重确认，422保留选择；standalone Permission route absent。
- Implementation pseudocode:

```typescript
renderRolePage(apiTreeFixture({direct:['501'], derivedApis:['701'], unmapped:['502']}))
expect(screen.getByText('用户管理')).toBeVisible(); expect(screen.queryByText(/permission/i)).toBeNull()
selectRoute('501'); selectAction('502'); save(); expect(lastBody.resourceIds).toEqual(['501','502'])
expect(lastBody).not.toHaveProperty('permissionIds'); expect(lastBody).not.toHaveProperty('applicationId')
openMappingDrawer(resource501); choosePermission901(); saveMapping(); expect(resourceQueriesInvalidated())
navigate('/iam/permissions'); expectNotFoundOrNoRoute();
```

- Verification contribution: `TEST-001`,`002`,`018`,`040` feature部分。
- After this file: tests因旧页面/API/routes而RED。

#### File 2 — `RENAME egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin-web/src/features/role/{RolePermissionPage.tsx->RoleResourceGrantPage.tsx,role.api.ts}`

- Purpose: 实现角色资源GET/PUT、tree/tabs/check states和query invalidation。
- Symbols: `RoleResourceGrantPage`, API types/methods `resources/replaceResources`。
- Repository evidence: current page已用TanStack Query/Mutation、PermissionGuard/PageState；只需替换输入和呈现，不引入新store。
- Dependencies and consumers: API-001/002、SDK resource/PermissionGuard、AntD Tree/Tabs/Alert、governance route。
- Why now: RED组件测试与后端契约均稳定。
- Contract/signature changes: 删除 BindRolePermissionsCommand；增加完整 tree/mutation TS types；管理按钮用资源管理权限。
- Input/output and state mapping: data.directResourceIds初始化selection；nodes递归渲染；linkedApis/derived只读；save payload exact resourceIds/window/version。
- Error and edge behavior: inherited/derived/unconfigured/nonACTIVE不能选；dirty false禁止save；pending禁止重复；409/422/5xx保留local selection并显示trace/recovery。
- Implementation pseudocode:

```typescript
const treeQuery = useQuery({key:['role-resources', tenant, roleId], fn: api.resources})
const [selected, setSelected] = useState(new Set(treeQuery.data?.directResourceIds ?? []))
render MENU_PAGE recursive Tree; render ACTION grouped by owning ROUTE; render API list with derived disabled and standalone selectable
onSave => api.replaceResources(roleId, {resourceIds:[...selected].sort(), validFrom, validTo, expectedRoleVersion:data.roleVersion})
onSuccess => invalidate role-resources, role-impact, about; on409 => show reload/reconfirm; on422 => keep selected and mark invalid nodes
never render mapping permissionCode/permissionId fields even if unexpected payload contains them
```

- Verification contribution: Role component/API tests GREEN；`REQ-001`,`002`,`014`,`019`,`021`。
- After this file: 角色配置完整切换到资源；旧RolePermissionPage删除。

#### File 3 — `RENAME egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin-web/src/features/application/{ResourceCatalogPage.tsx,PermissionPage.tsx->PermissionSelector.tsx,application.api.ts}`

- Purpose: 在资源目录提供actual mapping Drawer，把权限字符选择限制在高级管理员流程。
- Symbols: mapping query/mutation types、`PermissionSelector`、Drawer states/actions。
- Repository evidence: ResourceCatalog已选择Application并列Resource；PermissionPage已有ACTIVE permission query/create/status列表，可抽取selector而不新建第二client。
- Dependencies and consumers: API-003/004、existing permission CRUD、SDK PermissionGuard、AntD Drawer/Form/Alert。
- Why now: 后端mapping API已稳定，独立权限字符页可移除。
- Contract/signature changes: applicationApi增加 get/update mapping；Resource generic type不依赖requiredPermissionId；selector只返回ACTIVE permission ID/label/code给admin。
- Input/output and state mapping: row resourceId -> mapping query -> suggestion/actual/count/version；form -> permissionId/version/reason；success invalidate mapping/resource/role-tree/about。
- Error and edge behavior: unconfigured提示；in-use count与409指导先调整角色；forbidden隐藏按钮；version conflict刷新并重确认；Drawer关闭恢复row focus。
- Implementation pseudocode:

```typescript
row action visible only through PermissionGuard('system:resource-permission:read')
open => query api.permissionMapping(resource.resourceId)
render suggestion hint and current actual; PermissionSelector filters status === 'ACTIVE'
save => api.updatePermissionMapping(id,{permissionId,expectedResourceVersion:mapping.resourceVersion,reason})
on success invalidate mapping/resource/tree/about keys and close; on 409 retain selection and show affectedActiveGrantCount guidance
remove standalone PermissionPage route/navigation while retaining permission CRUD APIs needed by selector/admin workflows
```

- Verification contribution: `TEST-007`–`010` frontend、`TEST-018` denied state。
- After this file: 资源目录是唯一普通UI mapping入口；无 standalone permission字符导航。

#### File 4 — `MODIFY egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin-web/src/{features/governance.routes.tsx,app/router.tsx,app/resourceDefinitions.json,app/navigation.ts}`

- Purpose: 同步新role-resources route、移除permissions route，并确保RBAC navigation按SDK resourceCodes输出递归树。
- Symbols: route descriptors/component registry、resource definitions、`visibleNavigation`。
- Repository evidence: current governance routes同时注册 resources/permissions/role-permissions；navigation使用 FrontendResourceRegistry。
- Dependencies and consumers: Files 2/3 pages、Step 8 SDK、Step 10 shared children树（当前shared 0.1.4仍兼容children）。
- Why now: 页面/API实现后再切路由，保持每个commit可构建。
- Contract/signature changes: `/iam/roles/:roleId/permissions` -> `/iam/roles/:roleId/resources`；permission route和navigation definition删除；hidden detail父roles定位保留。
- Input/output and state mapping: resourceDefinitions codes -> registry.visible tree -> EnterpriseLayout navigation；route params -> RoleResourceGrantPage roleId。
- Error and edge behavior: old route 404；no visible route保持既有403；hidden detail仍由最长可见父路径定位，不新增redirect。
- Implementation pseudocode:

```typescript
register componentKey 'rbac3-role-resources' -> RoleResourceGrantRoute
define hidden ROUTE code 'iam.role-resources', path '/iam/roles/:roleId/resources', parent 'iam.authorization'
delete standalone 'iam.permissions' definition and PermissionPage route
visibleNavigation = registry.navigation(about) // registry now checks resourceCodes
router leaves unknown old permission path to existing not-found element; no compatibility redirect
```

- Verification contribution: `TEST-015`,`018`,`040` 路由/导航部分。
- After this file: RBAC3 Web功能和路由契约完成，等待shared 0.2.0视觉壳与依赖升级。

- Validation working directory: `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin-web`
- Verification command: `npm ci --legacy-peer-deps --no-audit --no-fund && npm run typecheck && npm test && npm run test:report && npm run build && npm run verify:bundle && npm run verify:conformance`
- Expected result: exit 0；角色请求无permissionIds；旧页面/route/navigation字符串零命中；mapping和resource guard测试通过。
- Failure returns to: File 1 fixtures/contract、File 2 role state、File 3 mapping state、File 4 routes；新的产品页或权限行为返回 Spec。
- Completion criteria: 管理员按可读资源配置角色；字符只在高级mapping流程；resourceCodes控制RBAC Web；common错误/恢复闭环。
- Rollback: revert本Step前端；不得与已发布V13后端单独上线，部署必须成组。
- Commit paths: `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin-web/src/features/{role/RolePages.test.tsx,application/ApplicationPages.test.tsx,../app/App.integration.test.tsx}` ; `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin-web/src/features/role/{RolePermissionPage.tsx->RoleResourceGrantPage.tsx,role.api.ts}` ; `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin-web/src/features/application/{ResourceCatalogPage.tsx,PermissionPage.tsx->PermissionSelector.tsx,application.api.ts}` ; `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin-web/src/{features/governance.routes.tsx,app/router.tsx,app/resourceDefinitions.json,app/navigation.ts}`
- Commit: `feat(rbac3-web): configure roles through readable resources`

### Step 10 — 发布 Banner + 左侧树的 Admin Web Shared 0.2.0

- Requirements: `REQ-022`, `REQ-023`, `REQ-024`, `REQ-025`
- Dependencies: `None`; code work may proceed independently, but publish must occur before Step 11。
- Baseline state: shared 0.1.4在Header桌面渲染horizontal Menu，Layout无Sider，mobile Drawer由Header自己维护且支持flat group。
- Observable outcome: desktop Header仅Banner，左Sider递归inline树且可折叠；mobile由Banner按钮打开复用同树的左Drawer；最长边界匹配与祖先open可用。
- End state: shared 0.2.0 typecheck/test/build/pack通过并发布；不导出EnterpriseSidebar，不新增Provider/API/store。
- Test-first gate: Required — 先重写EnterpriseLayout tests，旧Header应因仍有horizontal菜单、无Sider/activePathPrefixes而RED。
- Ordered files:

#### File 1 — `MODIFY egon-cola-platforms/egon-cola-platform-admin-web-shared/src/layout/EnterpriseLayout.test.tsx`

- Purpose: 固定desktop/mobile/empty/deep-link/collapse/a11y以及children不remount。
- Symbols: `TEST-033`–`TEST-036` component scenarios、matchMedia fixtures、stateful child mount counter。
- Repository evidence: current test已覆盖desktop menu、route高亮、click、mobile drawer和nested tree，可直接替换断言。
- Dependencies and consumers: target types/Header/Layout/Sidebar；React Router MemoryRouter；AntD jsdom。
- Why now: 公共package行为必须先RED，四端consumer才能安全依赖。
- Contract/signature changes: config不再用group；增加activePathPrefixes；Header不出现`mode=horizontal`主菜单。
- Input/output and state mapping: pathname/tree -> selected leaf/open ancestors；breakpoint -> Sider或Drawer；collapsed/openKeys/drawerOpen仅组件state。
- Error and edge behavior: empty tree隐藏Sider/trigger；pathless parent只展开；detail prefix不改URL；collapse不卸载children；Esc/关闭恢复focus。
- Implementation pseudocode:

```typescript
renderLayout(wide=true, path='/iam/roles/301', nestedTree)
expect(header).toHaveRole('banner'); expect(header.querySelector('.ant-menu-horizontal')).toBeNull()
expect(screen.getByRole('navigation',{name:'主菜单'})).toContainInlineMenuWithSelected('roles')
expect(parentMenu('iam')).toBeExpanded(); collapseSider(); expect(childMountCount).toBe(1)
renderLayout(path='/operations/701', item.activePathPrefixes=['/operations']); expect(item).toBeSelected()
setViewport(false); expectNoSider(); openDrawer(); clickParentKeepsDrawer(); clickLeafClosesAndNavigates(); pressEscapeRestoresTriggerFocus()
```

- Verification contribution: `TEST-033`–`036`,`042` shared contract。
- After this file: tests因旧shared实现而RED。

#### File 2 — `MODIFY egon-cola-platforms/egon-cola-platform-admin-web-shared/src/layout/types.ts`

- Purpose: 把navigation/onNavigate从Header职责归还Layout，删除group并加入activePathPrefixes。
- Symbols: `EnterpriseNavigationItem`, `EnterpriseHeaderConfig`, `EnterpriseLayoutConfig`, Header internal props配套类型。
- Repository evidence: children已存在；group只用于Header mobile分组；四端都是EnterpriseLayout消费者。
- Dependencies and consumers: Header/Layout/Sidebar、四AdminLayout编译。
- Why now: Sidebar/Layout实现前先固定0.2.0 public类型。
- Contract/signature changes: HeaderConfig仅platform/logo/actions/user；LayoutConfig新增navigation/onNavigate；item删除group、增加readonly activePathPrefixes。
- Input/output and state mapping: platform已经授权的树 -> shared只读props；prefix只参与selection，不导航。
- Error and edge behavior: key/label必填；path/prefix绝对；pathless children parent合法；无navigation默认[]。
- Implementation pseudocode:

```typescript
interface EnterpriseNavigationItem {
  readonly key:string; readonly label:string; readonly path?:string; readonly icon?:ReactNode;
  readonly activePathPrefixes?:readonly string[]; readonly children?:readonly EnterpriseNavigationItem[];
}
interface EnterpriseHeaderConfig { platformName:string; logo?:ReactNode; actions?:ReactNode; user?:EnterpriseUser }
interface EnterpriseLayoutConfig extends EnterpriseHeaderConfig {
  navigation?:readonly EnterpriseNavigationItem[]; onNavigate?:(item)=>void; footer?:...; contentStyle?:...;
}
```

- Verification contribution: shared/consumer TS compile，禁止flat group。
- After this file: public type为0.2.0目标；旧实现暂编译失败，等待Files 3/4。

#### File 3 — `CREATE egon-cola-platforms/egon-cola-platform-admin-web-shared/src/layout/EnterpriseSidebar.tsx`

- Purpose: 包内统一递归Menu、route selection/open ancestor、desktop Sider和mobile Drawer。
- Symbols: `EnterpriseSidebar`, internal flatten/index/match helpers、props。
- Repository evidence: current Header已有toItem/flattenLeaves/longest path/mobile Drawer代码，可移动复用；AntD Layout.Sider/Menu/Drawer已依赖。
- Dependencies and consumers: Layout ownsstate/handlers；types；React Router pathname/navigate由Layout传入或component读取。
- Why now: 类型已稳定，先集中导航算法再简化Header/Layout。
- Contract/signature changes: 组件不从package index导出；接受items/selected/open/collapsed/mobile/actions/onNavigate callbacks。
- Input/output and state mapping: recursive items -> AntD items/key-parent index；pathname候选path+prefix -> longest boundary match；click parent/leaf分流。
- Error and edge behavior: duplicate key/非绝对prefix在dev/test fail；无icon collapsed有tooltip/aria label；mobile parent不关闭、leaf关闭；shared不做403/404 redirect。
- Implementation pseudocode:

```typescript
indexTree(items, ancestors=[]): collect byKey, leaf candidates(path and activePathPrefixes), ancestorKeys
matches(candidate, pathname) = pathname===candidate || pathname startsWith `${candidate}/`; choose longest candidate
selectedKey = best?.item.key; selectedAncestors = best?.ancestors ?? []
desktop => <Layout.Sider collapsible collapsed={collapsed}><Menu mode="inline" items={tree} selectedKeys .../></Layout.Sider>
mobile => <Drawer placement="left" open={drawerOpen}><actions/><Menu mode="inline" same items .../></Drawer>
onMenuClick(key): if parent without path toggle only; else call onNavigate/default navigate and close mobile drawer
```

- Verification contribution: Files 1 `TEST-033`–`036` generic tree行为。
- After this file: 核心tree renderer存在但尚未由Layout使用。

#### File 4 — `MODIFY egon-cola-platforms/egon-cola-platform-admin-web-shared/src/layout/{EnterpriseHeader.tsx,EnterpriseLayout.tsx}`

- Purpose: Header收敛Banner；Layout组合Header、responsive Sidebar/Drawer、Content/Footer并拥有本地state。
- Symbols: `EnterpriseHeaderProps.mobileNavigationVisible/onOpenNavigation`, `EnterpriseLayout` states/effects/render。
- Repository evidence: Header当前同时有导航/Drawer/state；Layout当前只有Header+Content+Footer且读取breakpoint。
- Dependencies and consumers: File 3 Sidebar、File 2 types、four consumers。
- Why now: Sidebar算法已集中，可从Header删除所有business navigation代码。
- Contract/signature changes: Header不接navigation/onNavigate；Layout传mobile trigger；children永远放在稳定MainColumn subtree。
- Input/output and state mapping: navigation length + screens.lg -> Sider/trigger/Drawer；route changes补齐selected ancestors但保留用户手动其他openKeys；collapse不改children key。
- Error and edge behavior: empty tree全宽；breakpoint切换关闭Drawer；pathless parent安全；actions desktop在Banner/mobile在Drawer顶部；Header user area保持。
- Implementation pseudocode:

```typescript
EnterpriseHeader renders brand, desktop actions, user, and conditional mobile menu Button only;
EnterpriseLayout state: collapsed=false, openKeys=[], drawerOpen=false;
derive selection/ancestor keys from location and navigation; effect merges selected ancestors into openKeys;
render <Layout><Header/><Layout><Sidebar desktop-or-drawer props/><MainColumn><Content>{children}</Content><Footer/></MainColumn></Layout></Layout>;
on leaf navigate: config.onNavigate?.(item) ?? navigate(item.path); close drawer;
on breakpoint lg true: drawerOpen=false; never change child element identity on collapsed/open updates;
```

- Verification contribution: File 1全部GREEN；Header Banner/empty layout断言。
- After this file: shared行为完整；package仍为0.1.4直到File 5。

#### File 5 — `MODIFY egon-cola-platforms/egon-cola-platform-admin-web-shared/{package.json,package-lock.json}`

- Purpose: 标记breaking public prop为0.2.0并生成可发布锁/包内容。
- Symbols: package version/root lock version；不改依赖列表。
- Repository evidence: current 0.1.4且四端`^0.1.4`；release scripts使用npm version/build/publish。
- Dependencies and consumers: Files 1–4必须GREEN；npm registry权限；Steps 11–14。
- Why now: 只有公共组件验证通过后才可发布消费边界。
- Contract/signature changes: semver minor `0.2.0`；无新runtime dependency/export。
- Input/output and state mapping: package sources -> dist/tarball -> registry 0.2.0；postbuild清理node_modules行为按现有脚本处理。
- Error and edge behavior: npm publish冲突/权限失败停止消费者步骤；不得改tag回退到0.1.4；tarball不得含tests/node_modules/secrets。
- Implementation pseudocode:

```text
set package.json version to 0.2.0 and regenerate root package-lock metadata with npm install --package-lock-only;
run typecheck, vitest, build and npm pack --dry-run; inspect tarball file list and package version;
publish exact 0.2.0 using existing registry configuration only after source commit is accepted for execution;
verify npm view @egon-cola/admin-web-shared@0.2.0 version returns 0.2.0 before any consumer lock update;
```

- Verification contribution: `TEST-041` shared artifact half。
- After this file: repository和registry都可提供0.2.0；consumer升级被解锁。

- Validation working directory: `egon-cola-platforms/egon-cola-platform-admin-web-shared`
- Verification command: `npm ci --legacy-peer-deps --no-audit --no-fund && npm run typecheck && npm test && npm run build && npm pack --dry-run && npm view @egon-cola/admin-web-shared@0.2.0 version`
- Expected result: 所有命令exit 0；test无horizontal Header menu；pack/version均为0.2.0；publish动作在源码commit后执行并有registry回执。
- Failure returns to: File 1契约、File 2类型、File 3算法/a11y、File 4composition、File 5包元数据/registry；registry权限失败为发布阻塞，不改设计。
- Completion criteria: `TEST-033`–`036`,`041` shared范围通过；无EnterpriseSidebar public export、无新增API/store/依赖。
- Rollback: publish前可revert Step 10；publish后不得删除版本，消费者未升级时保留0.2.0未使用；整体回滚须四端统一锁回0.1.4。
- Commit paths: `egon-cola-platforms/egon-cola-platform-admin-web-shared/src/layout/EnterpriseLayout.test.tsx` ; `egon-cola-platforms/egon-cola-platform-admin-web-shared/src/layout/types.ts` ; `egon-cola-platforms/egon-cola-platform-admin-web-shared/src/layout/EnterpriseSidebar.tsx` ; `egon-cola-platforms/egon-cola-platform-admin-web-shared/src/layout/{EnterpriseHeader.tsx,EnterpriseLayout.tsx}` ; `egon-cola-platforms/egon-cola-platform-admin-web-shared/{package.json,package-lock.json}`
- Commit: `feat(admin-web-shared): move navigation into a left tree shell`

### Step 11 — 把 DDC Admin Web 的三组业务导航迁到左树

- Requirements: `REQ-022`, `REQ-023`, `REQ-024`, `REQ-025`
- Dependencies: `Step 10` and registry availability of shared `0.2.0`
- Baseline state: 8条flat items依靠group字符串；shared依赖/lock为^0.1.4。
- Observable outcome: DDC产生运行状态、配置管理、元数据管理三父树，原路由/认证不变并由shared左Sider/Drawer渲染。
- End state: DDC clean install/typecheck/test/build解析0.2.0；App/页面无导航回归。
- Test-first gate: Required — AdminLayout test先断言三父树/左Sider，旧flat/group结构RED。
- Ordered files:

#### File 1 — `MODIFY egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin-web/src/layouts/AdminLayout.test.tsx`

- Purpose: 固定三父组、子顺序、route click、无Header主菜单与empty行为。
- Symbols: DDC `TEST-037` desktop/mobile/deep link assertions。
- Repository evidence: 现有AdminLayout test已mock AuthContext/render routes；shared generic行为由Step10覆盖。
- Dependencies and consumers: target AdminLayout、shared 0.2.0。
- Why now: 先把DDC本地信息架构与shared职责分开验证。
- Contract/signature changes: tests不再读取group；查找运行状态/配置管理/元数据管理parent。
- Input/output and state mapping: existing authenticated DDC context -> local tree -> selected child；无backend变化。
- Error and edge behavior: parent click不导航；现有route guard保留；empty fixture不显示空rail。
- Implementation pseudocode:

```typescript
renderAdminLayout('/apps')
expect(leftNavigation()).toHaveParentsInOrder(['运行状态','配置管理','元数据管理'])
expand('元数据管理'); expect(children()).toEqual(['业务域','环境','应用','命名空间'])
expect(header()).not.toContainHorizontalBusinessMenu()
click('配置资源'); expect(routerLocation()).toBe('/configs')
setMobile(); openBannerTrigger(); expectSameTreeInLeftDrawer()
```

- Verification contribution: `TEST-037`,`042` DDC部分。
- After this file: test因旧flat items/shared旧lock而RED。

#### File 2 — `MODIFY egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin-web/{src/layouts/AdminLayout.tsx,package.json,package-lock.json}`

- Purpose: 生成children树并升级shared依赖图，不改变DDC routes/AuthContext。
- Symbols: navigation constant/config、dependency version/lock resolved entry。
- Repository evidence: current item.group已给出三个业务分组；package有local lock。
- Dependencies and consumers: shared 0.2.0、DDC App Router、AuthContext。
- Why now: RED测试与published package均就绪。
- Contract/signature changes: group删除；parent无path、children保留当前key/label/path/icon/order；dependency锁到兼容0.2.0。
- Input/output and state mapping: existing flat route facts -> three parent item children；onNavigate/default router不变。
- Error and edge behavior: parent不可导航；route不存在仍由Router处理；不得改变DDC API调用/权限；npm lock不得保留0.1.4 tarball/link。
- Implementation pseudocode:

```typescript
navigation = [
 {key:'runtime',label:'运行状态',children:[registry,publishTasks,cache]},
 {key:'configuration',label:'配置管理',children:[configs]},
 {key:'metadata',label:'元数据管理',children:[bizs,environments,apps,namespaces]},
]
pass navigation unchanged to EnterpriseLayout; preserve status/actions/user/auth flow;
set @egon-cola/admin-web-shared dependency to ^0.2.0 and regenerate package-lock from registry;
assert npm ls resolves exactly 0.2.0 and no lock entry resolves 0.1.4;
```

- Verification contribution: File 1 GREEN、`TEST-041` DDC graph。
- After this file: DDC采用统一左树且其他页面/route/API不变。

- Validation working directory: `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin-web`
- Verification command: `npm ci --legacy-peer-deps --no-audit --no-fund && npm ls @egon-cola/admin-web-shared --depth=0 && npm run typecheck && npm test && npm run build`
- Expected result: exit 0；npm ls显示0.2.0；AdminLayout/App tests与build通过。
- Failure returns to: File 1信息架构断言或File 2 tree/lock；DDC认证/路由行为不得为适配shared而改写。
- Completion criteria: `TEST-037`,`041`及可自动执行的DDC shell tests通过；E2E留给质量门禁。
- Rollback: 同时revert DDC tree/package/lock；不得只回lock或只回代码。
- Commit paths: `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin-web/src/layouts/AdminLayout.test.tsx` ; `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin-web/{src/layouts/AdminLayout.tsx,package.json,package-lock.json}`
- Commit: `feat(ddc-web): group navigation in the shared left tree`

### Step 12 — 把 IdP Admin Web 的授权导航递归过滤后交给左树

- Requirements: `REQ-022`, `REQ-023`, `REQ-024`, `REQ-025`
- Dependencies: `Step 10` and registry availability of shared `0.2.0`
- Baseline state: IdP flat ALL_NAV_ITEMS按bootstrap permission过滤；shared 0.1.4；无独立AdminLayout test，App.test覆盖壳。
- Observable outcome: 身份概览根route、身份目录、OAuth与资源、安全治理四根/组按child permission递归剪枝，detail最长父项定位。
- End state: IdP typecheck/test/build和clean dependency graph通过；ConsoleGuard/bootstrap/current tenant不变。
- Test-first gate: Required — App.test先加入tree/pruning/client resource-grant detail断言，旧flat items RED。
- Ordered files:

#### File 1 — `MODIFY egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-admin-web/src/app/App.test.tsx`

- Purpose: 固定四根/组、permission subset递归剪枝和OAuth client detail selection。
- Symbols: IdP `TEST-038` scenarios、mock bootstrap permission sets。
- Repository evidence: current App.test已覆盖Auth/bootstrap/router/layout，可继续作为consumer integration owner。
- Dependencies and consumers: target AdminLayout、router、shared 0.2.0。
- Why now: IdP没有独立Layout test，必须在现有真实App harness先RED。
- Contract/signature changes: test fixture仍用现有permissions，不改About/API；只断言输出tree。
- Input/output and state mapping: bootstrap permission subset -> visible children -> prune empty parent；pathname detail -> OAuth客户端selected。
- Error and edge behavior: no child permission不显示parent；overview保持root；ConsoleGuard/403/loading行为原断言继续。
- Implementation pseudocode:

```typescript
renderApp({permissions:['idp:users:read']}, '/users')
expect(leftTree()).toContainPath(['身份目录','全局用户'])
expect(leftTree()).not.toContain('OAuth与资源')
renderApp(fullPermissions, '/oauth-clients/client-1/resource-grants')
expect(menuItem('OAuth 客户端')).toBeSelected(); expect(parent('OAuth与资源')).toBeExpanded()
expect(header()).not.toContainHorizontalBusinessMenu()
```

- Verification contribution: `TEST-038`,`042` IdP部分。
- After this file: tests因flat filter/old shared而RED。

#### File 2 — `MODIFY egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-admin-web/{src/app/AdminLayout.tsx,package.json,package-lock.json}`

- Purpose: 建立IdP本地tree/recursive filter并升级shared，保留所有认证与route事实。
- Symbols: `ALL_NAV_ITEMS`, recursive filter helper, EnterpriseLayout config、lock。
- Repository evidence: current AdminLayout已拥有route labels/permissions/icons/order与bootstrap过滤；不需shared知道IdP权限。
- Dependencies and consumers: existing bootstrap Auth context、router、shared 0.2.0。
- Why now: RED测试与package已就绪。
- Contract/signature changes: tree roots按Spec §12.2；OAuth client item声明其现有detail active prefix或使用可见父path；dependency^0.2.0。
- Input/output and state mapping: permissionless overview always visible；each protected child checks existing permission；parents kept only with visible children。
- Error and edge behavior: unknown permission fail closed；current tenant/bootstrap/loading不动；不新增server navigation或重命名routes。
- Implementation pseudocode:

```typescript
ALL_NAV_ITEMS = [overview,
 parent('identity','身份目录',[globalUsers,tenants]),
 parent('oauth','OAuth与资源',[oauthClients,resourceServers]),
 parent('security','安全治理',[signingKeys,audit])]
filterTree(items, permissions): recursively filter children; keep root leaf when its existing predicate passes; drop empty parent
oauthClients.activePathPrefixes includes its existing resource-grant detail prefix only when normal path matching cannot select it
pass filtered tree to EnterpriseLayout; preserve tenant selector/actions/user/ConsoleGuard
upgrade dependency and regenerate local package-lock to resolved 0.2.0
```

- Verification contribution: File 1 GREEN、`TEST-041` IdP graph。
- After this file: IdP左树完成，认证/bootstrap契约无diff。

- Validation working directory: `egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-admin-web`
- Verification command: `npm ci --legacy-peer-deps --no-audit --no-fund && npm ls @egon-cola/admin-web-shared --depth=0 && npm run typecheck && npm test && npm run build`
- Expected result: exit 0；0.2.0 resolved；App tests覆盖pruning/detail且build成功。
- Failure returns to: File 1 fixture或File 2recursive filter/prefix/lock；不得改IdP权限字符或服务API。
- Completion criteria: `TEST-038`,`041`自动门禁通过；IdP四根/组与deep link行为符合Spec。
- Rollback: 同一提交revert AdminLayout/package/lock；四端整体版本回滚规则见§9。
- Commit paths: `egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-admin-web/src/app/App.test.tsx` ; `egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-admin-web/{src/app/AdminLayout.tsx,package.json,package-lock.json}`
- Commit: `feat(idp-web): render authorized navigation in the left tree`

### Step 13 — 把 Gateway Admin Web 的 capability 导航迁到左树并定位 operation detail

- Requirements: `REQ-022`, `REQ-023`, `REQ-024`, `REQ-025`
- Dependencies: `Step 10` and registry availability of shared `0.2.0`
- Baseline state: Gateway flat items按capability过滤；`/operations/:id`不是`/interface-catalog`前缀；shared 0.1.4。
- Observable outcome: 总览根、网关治理、MCP、观测与审计树正确剪枝；operation detail通过activePathPrefixes选择接口目录。
- End state: Gateway clean install/typecheck/test/build解析0.2.0，原capability/route/errorElement不变。
- Test-first gate: Required — AdminLayout test先覆盖MCP absent/present、group/operation detail selection和左树，旧flat实现RED。
- Ordered files:

#### File 1 — `MODIFY egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web/src/layouts/AdminLayout.test.tsx`

- Purpose: 固定本地tree/capability pruning与两个非平凡deep link。
- Symbols: Gateway `TEST-039` scenarios。
- Repository evidence: 现有test已mock capability provider并验证navigation；可直接扩展。
- Dependencies and consumers: target AdminLayout、shared 0.2.0、MemoryRouter。
- Why now: 先证明activePathPrefixes是唯一需要的新local声明，不改route。
- Contract/signature changes: fixture保留当前 capability codes；断言operation detail选接口目录、group detail选Gateway Group。
- Input/output and state mapping: gateway read/MCP capability -> visible tree；pathname -> selected leaf/open ancestors。
- Error and edge behavior: 无MCP权限整个parent消失；unknown detail无selection不redirect；Header无horizontal menu。
- Implementation pseudocode:

```typescript
renderLayout({gatewayRead:true,mcpRead:false}, '/groups/g-1')
expect(parent('网关治理')).toBeExpanded(); expect(item('Gateway Group')).toBeSelected(); expectNo('MCP')
renderLayout({gatewayRead:true,mcpRead:true}, '/operations/op-1')
expect(parent('MCP')).toExist(); expect(item('接口目录')).toBeSelected()
expect(location.pathname).toBe('/operations/op-1'); expect(header()).not.toContainHorizontalBusinessMenu()
```

- Verification contribution: `TEST-039`,`042` Gateway部分。
- After this file: tests因flat navigation/无prefix/shared旧版而RED。

#### File 2 — `MODIFY egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web/{src/layouts/AdminLayout.tsx,package.json,package-lock.json}`

- Purpose: 建立Gateway local tree、capability递归过滤、operation active prefix并升级shared。
- Symbols: navigation tree/filters、`activePathPrefixes:['/operations']`、package graph。
- Repository evidence: current routes/capabilities/icons/order全部在AdminLayout，且Spec EVD-025确认非前缀detail。
- Dependencies and consumers: CapabilityProvider、router、shared 0.2.0。
- Why now: RED测试与共享package已就绪。
- Contract/signature changes: parent pathless；接口目录item加prefix；MCP parent按existing capability；route URL不变。
- Input/output and state mapping: existing flat facts -> roots/children；capability filter先于传shared；prefix仅selection。
- Error and edge behavior: parent空则删除；Provider/operation detail既有guards/errors保留；lock无0.1.4。
- Implementation pseudocode:

```typescript
navigation = [dashboard,
 parent('gateway-governance','网关治理',[groups,applicationsCredentials,interfaceCatalog({...activePathPrefixes:['/operations']}),providers]),
 parent('mcp','MCP',[controlPlane,remoteMcp]),
 parent('observability','观测与审计',[traces,audit])]
filter children with existing capability predicates; prune empty parents; never change path strings
pass to EnterpriseLayout with existing status/actions/user
set shared dependency ^0.2.0 and regenerate lock; assert resolved version 0.2.0
```

- Verification contribution: File 1 GREEN、`TEST-041` Gateway graph。
- After this file: Gateway左树/deep-link完成，capability和URL不变。

- Validation working directory: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web`
- Verification command: `npm ci --legacy-peer-deps --no-audit --no-fund && npm ls @egon-cola/admin-web-shared --depth=0 && npm run typecheck && npm test && npm run build`
- Expected result: exit 0；0.2.0 resolved；MCP/prefix tests与build通过。
- Failure returns to: File 1断言或File 2tree/filter/prefix/lock；不得把operation URL改成catalog子路径。
- Completion criteria: `TEST-039`,`041`自动门禁通过；operation detail定位接口目录且无URL rewrite。
- Rollback: 同步revert Gateway代码/package/lock；不单独降级shared。
- Commit paths: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web/src/layouts/AdminLayout.test.tsx` ; `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web/{src/layouts/AdminLayout.tsx,package.json,package-lock.json}`
- Commit: `feat(gateway-web): organize capability navigation in the left tree`

### Step 14 — 升级 RBAC3 Admin Web 消费 shared 0.2.0 并完成四端发布门禁

- Requirements: `REQ-007`, `REQ-022`, `REQ-023`, `REQ-024`, `REQ-025`
- Dependencies: `Steps 9, 10`; DDC/IdP/Gateway commits may already exist independently。
- Baseline state: RBAC3已有recursive resourceDefinitions tree且Step 9改为resourceCodes，但package/platform lock仍0.1.4；integration未断言左Sider/empty/hidden detail。
- Observable outcome: RBAC3解析0.2.0并通过left tree/hidden detail/empty tests；四个consumer clean dependency/build gate统一完成。
- End state: 四端source/lock均无0.1.4解析；自动测试/构建完成，真实Playwright与live服务验证留给用户启动环境。
- Test-first gate: Required — App integration先断言左Sider/hidden role resources/empty no-rail，旧shared 0.1.4 RED。
- Ordered files:

#### File 1 — `MODIFY egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin-web/src/app/App.integration.test.tsx`

- Purpose: 固定RBAC3 resourceCodes递归tree、hidden detail parent selection、无route时不出现空rail。
- Symbols: RBAC3 `TEST-040` scenarios、Rbac3Provider fixtures。
- Repository evidence: current integration test已覆盖provider/router/resourceDefinitions；shared generic行为由Step10覆盖。
- Dependencies and consumers: Step 8 SDK、Step 9 routes/navigation、shared 0.2.0。
- Why now: 最后一个consumer必须用真实RBAC auth state证明tree输入正确。
- Contract/signature changes: About fixture使用resourceCodes；hidden route改role-resources；不读取permission来决定menu。
- Input/output and state mapping: READY about subset -> visible recursive tree；hidden detail pathname -> visible roles parentselected；empty -> existing403内容+无rail。
- Error and edge behavior: pending/403/503不闪现protected tree；no route不伪造navigation；Header无horizontal business menu。
- Implementation pseudocode:

```typescript
renderApp(readyAbout({resourceCodes:['iam','iam.authorization','iam.roles']}), '/iam/roles')
expect(leftTree()).toContainPath(['IAM','授权','角色']); expect(item('角色')).toBeSelected()
renderApp(readyAbout(fullCodes), '/iam/roles/301/resources')
expect(item('角色')).toBeSelected(); expect(location.pathname).toBe('/iam/roles/301/resources')
renderApp(readyAbout({resourceCodes:[]}), '/iam/roles')
expectNoLeftNavigationOrMobileTrigger(); expectExistingForbiddenOutcome()
```

- Verification contribution: `TEST-040`,`042` RBAC3部分。
- After this file: test因old shared shell/dependency而RED。

#### File 2 — `MODIFY egon-cola-platforms/egon-cola-platform-rbac3/{egon-cola-platform-rbac3-admin-web/package.json,package-lock.json}`

- Purpose: 升级RBAC3 shared依赖并更新平台级lock，随后运行四端统一解析检查。
- Symbols: package dependency、root/workspace lock entries。
- Repository evidence: RBAC3 Admin Web无local lock，使用`egon-cola-platform-rbac3/package-lock.json`；当前解析0.1.4。
- Dependencies and consumers: published shared 0.2.0、Step 9/14 tests、RBAC3 build。
- Why now: RBAC功能和integration test均稳定，最后切依赖图。
- Contract/signature changes: dependency `^0.2.0`；无其他npm依赖版本变化。
- Input/output and state mapping: platform lock -> installed exact0.2.0；source tree由shared自动左侧渲染。
- Error and edge behavior: lock不得出现file/link/0.1.4；npm registry失败停止；不手改integrity生成值。
- Implementation pseudocode:

```text
change admin-web dependency to ^0.2.0;
from egon-cola-platform-rbac3 npm root regenerate package-lock using npm install --package-lock-only;
run npm ci --legacy-peer-deps --no-audit --no-fund for the correct package root and assert npm ls resolves exactly @egon-cola/admin-web-shared@0.2.0;
scan all four consumer lockfiles for stale 0.1.4 resolution, local tarball or file link and require zero matches;
run each consumer typecheck/test/build command recorded in Steps 11-14;
```

- Verification contribution: File 1 GREEN、`TEST-041`完整四图检查。
- After this file: RBAC3与其余三端统一消费0.2.0；source release单元完成。

#### File 3 — `MODIFY egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin-web/scripts/{verify-browser-bundle.mjs,verify-rbac3-conformance.mjs}`

- Purpose: 最终静态门禁覆盖旧角色/权限/CI URL、浏览器CI泄漏与shared版本。
- Symbols: forbidden symbol/path/version lists、dist inspection。
- Repository evidence: 两脚本已经验证report脚本不进browser bundle与RBAC3结构一致性。
- Dependencies and consumers: built Admin Web dist、source tree/package lock、release pipeline。
- Why now: 所有最终命名和包版本已确定，才能写无临时例外的零残留断言。
- Contract/signature changes: 无runtime API；release command失败码扩展。
- Input/output and state mapping: source/dist/package metadata -> forbidden-match report/exit code；不读取credentials。
- Error and edge behavior: 任一RolePermission/PermissionResource/old URL/old route/old report package/0.1.4或SERVICE token config进入dist即非零；历史docs/migration allowlist不扫描或明确排除。
- Implementation pseudocode:

```javascript
const forbiddenSource = ['RolePermission','PermissionResource','/roles/{roleId}/permissions','/iam/resource-catalog/','iam.resource.report']
const forbiddenBundle = ['RBAC3_SERVICE_ACCESS_TOKEN','rbac3:resource-catalog:report','report-rbac-resources.mjs']
scan production source and built dist; report exact file/token and set process.exitCode=1 on any hit
read package manifests/locks and assert shared required/resolved version is 0.2.0 for all four consumers
exclude only docs and immutable historical migrations from semantic-source scan
```

- Verification contribution: `TEST-029`,`041` final static/release gate。
- After this file: release pipeline可判定新旧模型/包/前端版本是否混用。

- Validation working directory: `egon-cola-platforms/egon-cola-platform-rbac3`
- Verification command: `npm ci --legacy-peer-deps --no-audit --no-fund && npm --prefix egon-cola-platform-rbac3-admin-web run typecheck && npm --prefix egon-cola-platform-rbac3-admin-web test && npm --prefix egon-cola-platform-rbac3-admin-web run build && npm --prefix egon-cola-platform-rbac3-admin-web run verify:bundle && npm --prefix egon-cola-platform-rbac3-admin-web run verify:conformance && npm --prefix egon-cola-platform-rbac3-admin-web ls @egon-cola/admin-web-shared --depth=0`
- Expected result: exit 0；integration/静态/build通过；npm ls为0.2.0；四lock无旧解析。
- Failure returns to: File 1 RBAC fixture/navigation、File 2 lock/registry、File 3残留清单；E2E失败返回对应consumer Step，不在shared中猜平台逻辑。
- Completion criteria: `TEST-040`,`041`自动门禁完成；`TEST-042`测试代码/命令就绪但需用户启动四端服务后运行。
- Rollback: 代码未部署时revert本Step；部署回滚必须shared与四consumer整体回0.1.4且RBAC后端/V13按§9限制处理。
- Commit paths: `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin-web/src/app/App.integration.test.tsx` ; `egon-cola-platforms/egon-cola-platform-rbac3/{egon-cola-platform-rbac3-admin-web/package.json,package-lock.json}` ; `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin-web/scripts/{verify-browser-bundle.mjs,verify-rbac3-conformance.mjs}`
- Commit: `feat(rbac3-web): adopt the shared left navigation shell`

## 8. Test, Validation, and Quality Gates

| Gate/order | Working directory | Command or method | Scope | Expected result | Failure returns to | Requirements/runtime boundary |
| --- | --- | --- | --- | --- | --- | --- |
| Skill/Plan gate | repository root | `python3 .agents/skills/egon-coding-writing-plan/scripts/validate_plan.py docs/egon/plan/2026-08-25-10-19-rbac3-resource-grants-layering-implementation.md --strict` | Plan document | exit 0 | Plan owning section | planning only |
| RED Step 1 | RBAC Maven root | focused Contract/Starter selector from Step 1 before production files | Java contract/PEP | only missing fields/factory/membership assertions fail | Step 1 File 1/3 | `REQ-006`,`007` |
| GREEN Step 1 | RBAC Maven root | Step 1 Maven command | Contract/Starter | exit 0 | Step 1 File 2/4 | module/static |
| Package RED/GREEN | RBAC Maven root | Step 2 compile + architecture selector + forbidden-root `rg` | Admin package graph | compile/tests exit0，rg zero matches | Step 2 | `REQ-011`–`013` |
| Grant model | RBAC Maven root | Step 3 focused entity tests | PO/ports | exit 0 | Step 3 | no DB/runtime claim |
| Role resource API | RBAC Maven root | Step 4 focused MockMvc/service/IT | API-001/002 | JSON/atomic/concurrency/rollback pass | Step 4 | `REQ-001`,`002`,`014`,`019` |
| Mapping API | RBAC Maven root | Step 5 focused MockMvc/service/IT | API-003/004 | idempotent/in-use/version/audit pass | Step 5 | `REQ-003`,`015` |
| Registration | RBAC Maven root + RBAC Web | Step 6 Maven selector + `npm run test:report && npm run verify:bundle` | API-006/CI | zero actual/grant writes；Node exit0 | Step 6 | CI artifact, not browser/runtime |
| Runtime/Core | RBAC Maven root | `mvn -pl egon-cola-platform-rbac3-core,egon-cola-platform-rbac3-starter,egon-cola-platform-rbac3-admin -am verify` | Core/Starter/Admin | all unit/IT/Flyway/bootstrap pass | Step 7 Files 1–4 | container/module |
| V13 migration | RBAC Admin tests | `Rbac3FlywayPostgresqlIT,Rbac3MigrationContractTest` via Step 7 | V1→V13 PostgreSQL | new tables/constraints/indexes，old absent | Step 7 File 4 | Testcontainers, not live DB |
| Legacy static | RBAC Maven root | Step 7/14 `rg` and conformance scripts | source/JAR/dist | zero old class/table/URL/package/bundle token matches | Steps 7/14 | static artifact |
| SDK | RBAC React SDK | `npm ci --legacy-peer-deps --no-audit --no-fund && npm run typecheck && npm test -- --run && npm run build` | types/registry/guards | exit0；resourceCodes controls UI | Step 8 | browser unit/build |
| RBAC feature | RBAC Admin Web | `npm ci --legacy-peer-deps --no-audit --no-fund && npm run typecheck && npm test && npm run test:report && npm run build && npm run verify:bundle && npm run verify:conformance` | role/mapping/routes | exit0 | Step 9/14 | browser unit/build |
| Shared component | Admin Web Shared | `npm ci --legacy-peer-deps --no-audit --no-fund && npm run typecheck && npm test && npm run build && npm pack --dry-run` | shell/types/artifact | exit0；0.2.0 pack | Step 10 | component/package |
| Shared registry | Admin Web Shared | `npm publish` followed by `npm view @egon-cola/admin-web-shared@0.2.0 version` | external package registry | publish succeeds and prints0.2.0 | Step 10 File 5 | external release action |
| DDC consumer | DDC Admin Web | Step 11 npm command | tree/lock/build | exit0；resolved0.2.0 | Step 11 | frontend module |
| IdP consumer | IdP Admin Web | Step 12 npm command | tree/filter/lock/build | exit0；resolved0.2.0 | Step 12 | frontend module |
| Gateway consumer | Gateway Admin Web | Step 13 npm command | tree/capability/prefix/lock/build | exit0；resolved0.2.0 | Step 13 | frontend module |
| RBAC consumer | RBAC npm root/Admin Web | Step 14 npm command | resourceCodes tree/lock/build | exit0；resolved0.2.0 | Step 14 | frontend module |
| Four-lock clean graph | each four consumer root | `npm ci --legacy-peer-deps --no-audit --no-fund && npm ls @egon-cola/admin-web-shared --depth=0` | installed graphs | four times0.2.0；nofile/link/0.1.4 | owning consumer Step | `REQ-024` |
| Full RBAC reactor | RBAC Maven root | `mvn verify` | complete RBAC reactor | exit0 | Steps 1–7 | module/containers only |
| E2E shell | user-started four live apps | DDC/Gateway/RBAC3 existing `npm run e2e`; IdP uses existing App integration plus manual viewport smoke unless an approved Playwright config exists | desktop/mobile live UI | Banner top、left tree desktop、left Drawer mobile、selected route visible | owning consumer/shared Step | `TEST-042`; live proof |
| Post-deploy auth smoke | user-controlled unified stack | login→about→role resource GET/PUT→mapped API call→field response→logout/RT revoke | Gateway/IdP/RBAC/Redis/PostgreSQL | resourceCodes/PEP/active role/cache behavior observable | Step 7/9 or operations | not claimed by Plan/static tests |

RED tests必须先单独运行并确认失败原因是缺失目标行为；若因fixture、JDK、registry、Docker或网络失败，先修环境而不是写生产代码。GREEN后才运行所在module回归，最后运行跨模块/构建门禁。Playwright和真实拓扑需要用户启动服务；执行代理不得为了验证本计划自动启动项目。

## 9. Migration, Compatibility, Rollout, and Rollback

### 9.1 Source and schema sequence

1. 完成 Steps 1–6 的源码/单元契约，但这些中间提交不得独立部署到现有环境。
2. Step 7 在同一发布候选中加入唯一 V13、runtime/bootstrap切换和旧模型删除；确认没有第二个V13或修改V1–V12。
3. 在维护窗口前执行只读preflight：备份、确认旧Admin writer停写、列出ACTIVE但未映射的grantable resources、确认DDC BIZ/APP与built-in Application/Role/resource codes完整。
4. 构建 Contract/Core/Starter/Admin/React SDK/RBAC Web与shared；发布shared 0.2.0后再生成四端consumer locks。
5. 维护窗口停止旧RBAC3 Admin writer，部署V13兼容二进制并让Flyway执行：创建新表、增加suggestion、把unmapped ACTIVE转PENDING、drop旧表。
6. 使用API-006重新上报当前完整前端资源及bindings；管理员确认actual mappings；运行platform admin bootstrap建立必要RoleResourceGrant，再通过readiness。
7. 清理旧RBAC snapshot Redis keys或按现有auth/policy version/invalidation机制推进，使新resource union被重新投影。
8. 同一发布单元部署Starter消费者、RBAC3 Web与四端shared 0.2.0 consumer；不允许旧Admin/old shared混跑。
9. 完成About、API PEP、role tree/mapping、page/action API边界、desktop/mobile shell和logout/RT revoke smoke后结束维护窗口。

### 9.2 Compatibility

- 破坏式删除旧 role permission HTTP、旧 CI URL、旧role-permissions前端route、旧Java类和两张表；无alias、redirect、dual read/write。
- `PermissionRequest.of`、通用 `@RequiresPermission`、About原permissions/fieldPolicies/versions、JWT/AT/RT claims保持；新增resourceCodes和API factory。
- V13后旧Admin二进制会因表/类/契约缺失失败，必须整组升级。
- shared 0.2.0修改公共props；四个consumer必须同时升级package source/lock，不能让某端仍解析0.1.4。

### 9.3 Rollback and forward-fix

- V13应用前：每个Step可以path-limited revert；shared 0.2.0即使已发布也不删除，只要consumer未升级即可不使用。
- V13应用后：应用级回退不受支持。只能在维护窗口恢复预部署数据库备份并整体回滚后端/前端/shared consumer，或继续forward-fix。
- 仅回滚前端布局时，shared和四consumer必须整体退回0.1.4的source/lock；不能只回一个应用。
- invalidation/outbox失败不回滚已提交DB事实；按现有重试恢复，版本缺口期间授权fail closed。
- CI registration网络未知结果用同build/checksum幂等重试；4xx不重试，先修source/catalog/mapping。

## 10. Requirement-to-Step Traceability Matrix

| Requirement | Effective Spec section | Steps | Files | Tests/gates | Completion evidence |
| --- | --- | --- | --- | --- | --- |
| `REQ-001` | 主 Spec §4/API-001/002 | 3、4、9 | Grant PO/API/Page | `TEST-001`,`003`–`006` | role request/UI无permission IDs |
| `REQ-002` | 主 Spec §12 | 4、9 | TreeVO/RoleResourceGrantPage | `TEST-001`,`002` | 菜单页面/按钮/API可读树 |
| `REQ-003` | 主 Spec API-003/004 | 3、5、9 | ResourcePO/mapping slice/Drawer | `TEST-007`–`010` | suggestion/actual admin-only |
| `REQ-004` | 主 Spec §11 | 3、5、7 | ResourcePO/V13/deletes | `TEST-025`,`029` | 一处actual mapping，无duplicate table |
| `REQ-005` | 主 Spec §7.3.3 | 1、7 | Core facts/JPA/projector | `TEST-020`–`024` | active resource roots派生permissions |
| `REQ-006` | 主 Spec INTERNAL-001 | 1 | PermissionRequest/Service/Manager | `TEST-019`–`022` | API双条件，generic兼容 |
| `REQ-007` | 主 Spec API-005 | 1、8、9、14 | About/SDK/RBAC Web | `TEST-015`–`018`,`040` | UI按resourceCodes |
| `REQ-008` | 主 Spec §14 | 1、2、7、8 | Starter/Core/Admin/SDK regressions | `TEST-024`+module verify | Field/Data/SOD/Fence保持 |
| `REQ-009` | 主 Spec API-006 | 3、6、8 | Resource suggestion/Registration/CI script | `TEST-011`–`014` | CI零actual/grant写 |
| `REQ-010` | 主 Spec §8/API-006 | 2、6、14 | registration.ci/new URL/conformance | `TEST-011`,`029` | 无旧report包/URL，CI-only |
| `REQ-011` | 主 Spec §8 | 2、7 | package moves/cleanup | `TEST-027` | IAM精确六根 |
| `REQ-012` | 主 Spec §7.1/§8 | 2、7 | authorization layers/architecture | `TEST-027` | 依赖方向可执行 |
| `REQ-013` | 主 Spec §8/§16 | 2、7、9、14 | deletes/routes/static scripts | `TEST-025`,`027`,`029` | 无旧类/表/URL/route |
| `REQ-014` | 主 Spec API-002 | 3、4、9 | Grant repository/API/Page | `TEST-003`–`006` | atomic replace/409/422 |
| `REQ-015` | 主 Spec API-004 | 5、9 | Mapping service/Drawer | `TEST-008`–`010` | in-use remap 409 |
| `REQ-016` | 主 Spec §9 | 1、4、5、6、9 | About/new Controllers/clients | `TEST-007`,`013`,`017` | complete ResultRecord |
| `REQ-017` | 主 Spec §11/§16 | 7 | V13/bootstrap/deletes | `TEST-025`,`028`,`029` | destructive cutover complete |
| `REQ-018` | 主 Spec §7.3.5 | 4、5、6、7 | version publishers/runtime | `TEST-026`,`028` | monotonic version/invalidation |
| `REQ-019` | 主 Spec §7.3.3 | 3–9 | Binding/Grant/runtime/UI | `TEST-023`,`030`–`032` | page/button/shared API OR union |
| `REQ-020` | 主 Spec API-006/§11 | 3、6、7、9 | Binding validation/V13 | `TEST-030`,`031` | same-app typed relation |
| `REQ-021` | 主 Spec §7.3.3 | 4、6–9 | source declarations/runtime/UI | `TEST-030`–`032` | ROUTE无ACTION-only API |
| `REQ-022` | 主 Spec INTERNAL-002 | 10–14 | Shared Sidebar/four trees | `TEST-033`–`035`,`042` | desktop业务菜单只在左Sider |
| `REQ-023` | 主 Spec INTERNAL-002 | 10–14 | Header/Layout/four consumers | `TEST-033`,`036`,`042` | Header只Banner/mobile trigger |
| `REQ-024` | 主 Spec §12/§16 | 10–14 | shared0.2.0/fourpackage locks | `TEST-037`–`042` | 四端统一左树/version |
| `REQ-025` | 主 Spec §12.3–§12.5 | 10–14 | route matcher/state/consumer prefixes | `TEST-034`–`042` | collapse/deep-link/mobile闭环 |
| `REQ-026` | 前置 Spec global catalog/tenant split | 2–7 | package/JPA/V13/runtime | migration/Admin regression | catalog无tenant，grant隔离 |

## 11. Risks, Blockers, and User Decisions

| ID | Risk or decision | Impacted Steps/files | Evidence | Owner | Status/action |
| --- | --- | --- | --- | --- | --- |
| `RISK-001` | Step 2机械移动面大，易漏FQCN/JPQL/reflection | Step 2全部Admin Java/test | 716个main Java文件、多个跨包consumer | Implementer | Mitigated：ArchUnit+compile+forbidden rg+单独提交 |
| `RISK-002` | V13不可应用级回退且旧授权数据丢失 | Step 7 migration/bootstrap | 用户批准破坏式更新，Spec §16 | Operator | Accepted：备份、维护窗口、readiness、forward-fix |
| `RISK-003` | Bootstrap资源缺失/未映射导致管理员锁死 | Step 7 bootstrap | 主 Spec RISK-001/TEST-028 | Operator/RBAC owner | Mitigated：preflight+PENDING review+bootstrap readiness |
| `RISK-004` | 页面/按钮API声明不真实导致授权过宽或页面失败 | Steps 6、8、9 | Source owner掌握真实调用，runtime禁止猜测 | Frontend/resource owner | Mitigated：typed same-app validation、ROUTE/ACTION边界tests、CI阻断 |
| `RISK-005` | shared 0.2.0 registry或consumer lock不一致 | Steps 10–14 | 四端当前均0.1.4且locks分散 | Frontend release owner | Mitigated：先publish、四次clean npm ls、整体回滚 |
| `RISK-006` | Docker/registry/live服务环境不可用 | Steps 7、10–14 gates | Plan阶段未连接DB/registry/服务 | User/Operator | Accepted validation boundary：静态/模块先行，外部动作失败明确停点 |
| `DECISION-001` | 不新增缓存、导航store、Manifest或build scanner | 全计划 | 主 Spec §7.0/§5.4 | Mario | Confirmed |
| `DECISION-002` | active role、双Token、无Session、DDC/IdP ownership不变 | Steps 1、2、7 | Effective Specs | Mario | Confirmed |
| `DECISION-003` | 顶部叫Banner，MENU/ROUTE在左侧树，四端都改 | Steps 10–14 | 用户最新文字纠正 | Mario | Confirmed |

不存在未决的业务、接口、schema、权限、tenancy或发布决策。npm registry、Docker或live topology不可用时属于执行环境阻塞，不能通过改Spec语义绕过。

## 12. Review and Acceptance

### 12.1 Original requirement fidelity

本计划逐项覆盖角色绑定资源而非字符、页面/按钮API并集、管理员mapping、About/resourceCodes、API双条件、active role、字段/DataScope/SOD/Fence保留、CI-only registration、六类IAM/授权分层、V13破坏式清理，以及四端Banner+左树/mobile Drawer。没有加入用户未要求的Session、第三Token、Manifest、API scanner、DataScope SQL改写或导航服务。

### 12.2 Spec consistency

所有公共URL、DTO字段、错误、事务、PO/表、页面状态、shared props、四端树和rollout均来自effective Specs。`PLAN-CLAR-001`–`003`只解决现有Controller拆分、PermissionPage收敛和机械文件清单表达，不增加业务能力。简单性审计确认不存在fetch-then-forward接口、可由可信上下文派生却要求caller提交的tenant/app/actor，也没有 speculative cache/provider/factory/legacy layer。

### 12.3 Repository executability

路径、类名、V12最新版本、Maven模块、package scripts、四个package/lock位置和现有tests均在`main@df425ed...`核对。每个Step给出RED/GREEN、依赖、文件顺序、pseudocode、工作目录、命令、失败返回点、回滚和path-limited commit。Step 2的大量纯package/import文件由精确根映射、`git diff --name-status`、ArchUnit、compile和零旧根scan共同闭合；行为修改只在后续owning Step发生。

### 12.4 Test and release completeness

`TEST-001`–`TEST-042`均映射到Step/gate；V13只有一个新migration且不改历史；shared先publish后consumer lock；V13后只允许备份恢复或forward-fix。Plan阶段只验证文档和静态仓库基线，不声称数据库、registry、服务、浏览器或生产拓扑已经通过；这些证据在实施/用户启动环境后产生。

### 12.5 Final verdict

PASS — Ready for user review
