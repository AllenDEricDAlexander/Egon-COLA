# platforms Admin Web 企业级平台与 Wujie 混合宿主实施计划

| Field | Value |
| --- | --- |
| Document | `2026-08-27-11-13-platforms-web-enterprise-implementation.md` |
| Template Version | `4` |
| Status | `Review` |
| Created | `2026-08-27 11:13 CST` |
| Updated | `2026-08-27 11:44 CST` |
| Owner | User / Egon-COLA platform owner |
| Repository | Egon-COLA |
| Scope | `egon-cola-platforms` 下的 shared、IDP/RBAC3/Gateway/DDC Admin Web 与新增 Platform Portal；本 Plan 只实现 Web/Portal，后端缺失接口仍按 Spec §9 作为后续 Java API 输入 |
| Source Requirement | 用户确认 Wujie、混合宿主、静态菜单、同源 Cookie/CSRF、首页只读 Facade、后续 Java 传统三层，并要求完善 Spec 后开始写 Plan |
| Baseline Revision | `main@95039c0b`；2026-08-27 dirty-worktree snapshot，用户既有 docs/Plan/Spec 修改和 `egon-cola-archetype-web-open/` 按 §6.1 保持不动 |
| Implements Spec | [platforms Admin Web 企业级平台与微前端需求架构规格](../spec/2026-08-27-08-09-platforms-web-enterprise-microfrontend.md) |
| Spec Status | `Review` |
| Spec Revision | `2026-08-27 11:44 CST`; baseline `main@95039c0b` |
| Effective Specs | [platforms Admin Web 企业级平台与微前端需求架构规格](../spec/2026-08-27-08-09-platforms-web-enterprise-microfrontend.md)<br>[DDC Admin 全量分页查询与前端现代化设计](../../superpowers/specs/2026-08-10-ddc-admin-pagination-ui-modernization-design.md) |
| Depends On Plans | `None` |
| Supersedes | `None` |
| Superseded By | `None` |
| Related Plans | `None` |

## 1. Summary

本 Plan 将 Spec 的 Web 范围拆成 6 个可独立验证、逐 Step 提交的结果：shared 页面壳与状态规范、IDP 页面/API 消费闭环、RBAC3 路由与页面职责修复、Gateway 现有能力接入、DDC 现有分页能力与实例入口、Wujie Portal/子应用生命周期/首页摘要 Facade。每个 Step 先写聚焦 RED 测试，再写最小实现和 wiring；不启动服务、不执行数据库迁移、不修改 Java 生产代码。

最终证据是：四个独立 Web 仍可单独构建和访问，Portal 可加载带版本约束的子应用 Manifest，挂载/卸载/子应用异常互不污染，左侧菜单由平台静态资源与 capability 裁剪，页面的 loading/empty/partial/error/denied/conflict/retry 状态有测试，已有 Admin API 被前端正确消费，Spec §9 的真正接口缺口没有被“已有接口未接入”混淆。

统一首页采用 Portal 内的 `PlatformSummaryFacade`，先编排已配置的只读摘要适配器和子应用生命周期状态；不会新增通用 BFF。后端 `GET /api/v1/platform/console/summary`、DDC 审计/校验/Diff、IDP 分页扩展、Gateway Trace 详情等仍是 Spec §9 的后续接口契约输入，不在本 Plan 中伪造 Java/API 合同。

## 2. Target Spec and Effective Design

### 2.1 Primary target

- Path: [docs/egon/spec/2026-08-27-08-09-platforms-web-enterprise-microfrontend.md](../spec/2026-08-27-08-09-platforms-web-enterprise-microfrontend.md)
- Status: `Review`
- Revision: `2026-08-27 11:44 CST`; baseline `main@95039c0b`
- Approval evidence: 用户在本轮明确确认 DEC-101 至 DEC-106，并明确要求完善 Spec 后开始写 Plan；本文件因此是供用户审查的 `Review` Plan，不代表实施批准。

### 2.2 Effective Spec set

| Role | Spec/link | Status/revision | Effective sections | Why included |
| --- | --- | --- | --- | --- |
| Primary | [platforms Admin Web 企业级平台与微前端需求架构规格](../spec/2026-08-27-08-09-platforms-web-enterprise-microfrontend.md) | `Review / 2026-08-27 11:44 CST` | §1-§20，重点为 §3、§4、§5、§7、§8、§9、§12、§13、§14、§15、§16、§17、§18 | 定义四个 Web、Wujie 混合宿主、静态菜单、同源认证、首页摘要、页面设计、接口缺口和不改 Java/数据库边界 |
| Normative dependency | [DDC Admin 全量分页查询与前端现代化设计](../../superpowers/specs/2026-08-10-ddc-admin-pagination-ui-modernization-design.md) | predecessor document，未产生本 Plan 的 REQ 编号 | §1-§10，尤其分页 wrapper、`/page` 兼容、旧 List/RPC 保留和不改数据库 | DDC 页面必须沿用既有分页设计，不能用本 Plan 重写 DDC 后端契约 |

### 2.3 Superseded or excluded content

- Spec §9 中分类为 Candidate/Deferred server-side option 的新增后端 API 不在本 Plan 生成；它们仍是后续后端契约的输入。
- Spec §6.1/§6.2 中已确认的 Java 传统三层只是后续 Java 实现约束；本 Plan 不迁移四个现有 feature-first Admin 模块，不创建 Java 类型、Bean、Repository、Migration 或 POM 模块。
- 动态数据库菜单、全页面通用 BFF、iframe 默认方案、Redux/Zustand、数据库迁移和四个 Web 合并为单 SPA 均被 Spec §3.2、§7.0、§17 排除。

## 3. Effective Requirements and Acceptance

| Requirement | Source Spec section | Effective statement | Observable acceptance | Implementation impact |
| --- | --- | --- | --- | --- |
| `REQ-001` | Spec §4、§7、§12 | 四个平台使用统一企业级控制台壳，并提供统一首页摘要体验 | shared 的 Header/Sidebar/PageHeader/PageState 规则在四个 Web 可复用；Portal 首页可显示每个平台的成功、局部失败或未配置状态 | Step 1、Step 6；shared components、Portal summary |
| `REQ-002` | Spec §4、§7、§12、§15 | 菜单按平台业务域和 capability 裁剪，按钮不能越过后端权限 | 无读权限的路由节点不出现在菜单；直接深链显示 403；写按钮按 action capability 控制 | Step 1-6 的导航/guard 测试 |
| `REQ-003` | Spec §4、§12 | 当前路由和目标补齐页面均有标题、筛选、主体、详情、状态和操作说明并落到实现 | 页面使用统一 PageHeader/PageState；新增入口有对应组件测试和空/错状态 | Step 1-5 |
| `REQ-004` | Spec §4、§7、§12；DDC predecessor §4、§7 | URL、Query Cache 和服务端分页是可恢复状态来源 | 刷新恢复 scope/filter/page；Mutation 精确失效；旧请求不会覆盖新筛选；DDC 继续使用 `/page` | Step 2-5；Step 6 host route |
| `REQ-005` | Spec §4、§7、§8、§16 | Wujie 采用统一入口、子应用独立访问的混合模式 | 四个独立入口仍构建；Portal Manifest 可校验并挂载子应用；挂载失败只影响当前 child | Step 6；四个 child main/lifecycle |
| `REQ-006` | Spec §4、§5、§6、§17 | Wujie 依赖和宿主契约可审查并先过兼容性门 | Portal lockfile 固定 `wujie-react@2.1.0`；React 19/Vite 8 构建、Manifest version、basename 和 mount contract 通过 | Step 6 package/config/tests |
| `REQ-007` | Spec §4、§7、§12、§15 | 宿主和子应用不传播明文 Token、Secret、Cookie、Body 或敏感 Header | bridge/props 测试只允许 platform/route/scope/capability summary；敏感字段负向断言通过 | Step 2、Step 6 |
| `REQ-008` | Spec §4、§9 | 后端已有未消费能力与真正缺失接口分开记录和消费 | route/API static mapping 清单无未解释错配；Spec §9 的 deferred API 不被前端当成已存在成功接口 | Step 2-5、Step 6 |
| `REQ-009` | Spec §4、§9、§12 | IDP 页面提供规模化列表和 Grant 变更体验 | 用户/Client/Resource Server 支持 page wrapper 或旧 List 兼容；Grant 有读取能力缺失的明确状态，并消费现有保存/删除/批量接口 | Step 2 |
| `REQ-010` | Spec §4、§7、§9、§12 | RBAC3 页面职责和真实 `/iam`/`/internal` 路径一致 | 组织/岗位不再绑定用户查询页；角色、策略、目录快照的 request path 与 Controller 一致 | Step 3 |
| `REQ-011` | Spec §4、§9、§12、§15 | Gateway 目录、发布和观测页面消费已有能力并保留安全脱敏边界 | Release Diff、Operation metadata/manual-definition/deprecate、Trace scope/filter/detail unavailable 状态均不直连内部组件 | Step 4 |
| `REQ-012` | Spec §4、§9、§11、§12；DDC predecessor §4、§7 | DDC 继续支持配置、发布、缓存、注册和实例分页体验；缺失审计接口不伪造为空 | `/instances/page` 有独立入口；配置/发布页保留真实 page total、polling、retry；审计缺口显示能力状态并回链 §9 | Step 5 |
| `REQ-013` | Spec §4、§7、§12、§15 | 401/403/404/409/422/5xx/timeout/unknown 有不同 UI 语义 | 每个平台 PageState/QueryState 测试验证登录恢复、权限拒绝、字段错误、冲突保留、受控重试和未知状态 | Step 1-6 |
| `REQ-014` | Spec §4、§7、§12、§15 | 管理操作可追踪、防重复并能恢复 | pending 禁止重复点击；query invalidation 精确；Gateway/DDC 的 retry/rollback 保留业务 identity 与审计提示 | Step 2-5、Step 6 |
| `REQ-015` | Spec §4、§7、§8、§16、§17 | 宿主、共享壳和单个子应用可独立回滚 | Portal 可打开 standalone URL；child mount 失败有独立入口；版本不兼容不挂载危险页面 | Step 6 |
| `REQ-016` | Spec §4、§5、§20 | 本轮只完善文档并提供 Review Plan，不写生产代码 | Spec/Plan 校验通过；本轮工作区仅新增/修改两份文档，执行阶段另行授权 | 本 Plan 的全部 Steps 先供审查；无运行时声明 |

## 4. Implementation Strategy and Dependency Order

### 4.1 Ordered strategy

先完成 shared 的最小页面骨架和状态契约，四个平台再按业务边界消费；IDP/RBAC3/Gateway/DDC 的改动只触及各自 Web package，不将跨平台业务 API 搬到 shared。Wujie Portal 最后接入，因为它依赖 shared 导出的页面壳、子应用可独立构建、child 生命周期入口和稳定的前端 API 状态。

每个业务 Step 的文件顺序为“聚焦测试 -> 类型/纯函数 -> API mapping -> 页面/路由 -> 入口回归”。如果测试引用的新文件尚未存在，Plan 明确这是编译前置 RED；它不能被解释成测试已通过。缺失后端接口只在前端显示能力缺口或保留后续适配边界，不通过增加别名接口掩盖路径问题。

### 4.2 Test-first strategy

| Behavior | RED proof | Minimum GREEN implementation | Wiring/regression |
| --- | --- | --- | --- |
| shared PageHeader/PageState | `PageHeader.test.tsx` 先因模块不存在或断言缺失而失败 | 创建 PageHeader，调整 PageTemplate/PageState 的状态和标题渲染 | shared typecheck/test/lint |
| IDP scalable list/Grant/audit | page normalizer、用户列表、Grant 删除/404、审计筛选测试先锁定当前失败行为 | `page.ts`、types 和五个页面最小修改；旧 List wrapper 仍能显示 | App test、IDP typecheck/test |
| RBAC3 route ownership/path | routes/API client test 先断言组织/岗位组件和 `/iam`/`/internal` path，当前源码不满足 | 修复 route descriptor、API path 和页面职责组件 | RBAC3 App integration、conformance/resource tests |
| Gateway consume existing capabilities | gatewayApi/Operation/Release/Trace focused tests 先断言缺失 client wiring 和状态 | 增加 API methods、脱敏详情/不可用状态、Diff/metadata controls | Gateway typecheck/test/lint |
| DDC instance/pagination/recovery | Instances page and current-page polling tests先断言 route/query/page state | 使用现有 `ddcPageApi` 和 `PageResultRecord`；只为实例新增入口，审计缺口显示能力状态 | DDC typecheck/test/lint |
| Wujie lifecycle/bridge/summary | manifest/lifecycle/bridge/Facade tests先断言 invalid version、cleanup、敏感字段过滤和 partial | Portal package、Wujie adapter、summary facade、child `__WUJIE_MOUNT/UNMOUNT` | Portal build/test；用户启动后手工验证 |

### 4.3 Sequential and parallel boundaries

| Step | Depends on | May run in parallel with | Must not overlap with | Reason |
| --- | --- | --- | --- | --- |
| Step 1 | None | None | shared package paths in later Steps | 四个平台都依赖 shared exports 和 PageState 行为 |
| Step 2 | Step 1 | Step 3, Step 4, Step 5 | IDP Web paths only | IDP 可独立验证，不与其他 Web package 写集重叠 |
| Step 3 | Step 1 | Step 2, Step 4, Step 5 | RBAC3 Web paths only | RBAC3 SDK/API 与其他平台无编译依赖 |
| Step 4 | Step 1 | Step 2, Step 3, Step 5 | Gateway Web paths only | Gateway 页面和 client 可单独 typecheck/test |
| Step 5 | Step 1 and DDC predecessor semantics | Step 2, Step 3, Step 4 | DDC Web paths only | 必须沿用 predecessor 的 PageResultRecord 和 `/page` 兼容 |
| Step 6 | Step 1-5 focused contracts and standalone child entry review | None | Portal and all four child entry/layout paths | Host mount depends on shared API, child lifecycle and independently buildable children |

### 4.4 Commit boundaries

每个 Step 一个语义提交，且只 stage 本 Step 的精确路径。Step 6 的 Portal 新 package、四个 child entry/lifecycle 改动和测试属于一个不可分割的宿主接入结果；不把 Wujie 依赖先落到四个子应用，也不提交工作区其他用户变更。数据库无变更，因此没有 Flyway 提交。

### 4.5 Spec Simplicity and Implementation-necessity Audit

| Spec element | Spec necessity verdict/section | Current repository evidence | Direct/reuse alternative | Interaction/implementation cost | Plan decision |
| --- | --- | --- | --- | --- | --- |
| PageHeader | Spec §7.0 Keep/Expand、§8.3；四个平台重复标题/面包屑/主操作 | shared 当前只有 `PageTemplate`，平台页面重复 `Card`/`Typography.Title` | 继续每页自绘可少一个文件，但会保留标题、主操作和面包屑漂移 | 1 个 shared component、1 个测试、四个平台导入调整 | Implement in Step 1 |
| PageState/PageTemplate | Spec §7.0 Keep/Expand | shared 已有 `PageState.tsx`、`PageTemplate.tsx` | 四个平台自写 loading/error/empty 会重复分支 | 不增加网络调用，只统一状态和可访问结构 | Implement in Step 1 |
| IDP page normalizer | Spec §4、§9、§12；三类列表同时面对旧 List/新 page wrapper | `UserListPage`、`ClientListPage`、`ResourceServerListPage` 当前直接消费数组 | 每页写一次兼容分支会重复且不一致 | 1 个纯函数和测试，无新网络调用 | Implement in Step 2 |
| RBAC3 organization/position pages | Spec §4、§12；当前两条路由错误绑定 `UserDirectoryPage` | `governance.routes.tsx` 明确复用错误组件；后端 Organization/Position Controller 已存在 | 继续复用会展示错误业务事实 | 2 个页面组件、现有 API client 扩展和 focused tests | Implement in Step 3 |
| DDC instances page | Spec §9.2、§12；后端 `/api/v1/ddc/instances/page` 已存在但前端无入口 | DDC `App.tsx`/`AdminLayout.tsx` 无 instances route/menu | 将实例藏在其他页面会混淆配置客户端与服务注册实例 | 1 个页面、1 个 route/menu、复用 `ddcPageApi` | Implement in Step 5 |
| Wujie Portal/adapter | Spec §5.4 DEC-101/102、§7、§8 | 当前没有 Portal/Wujie；四个 Web 是独立 Vite apps | 只保留独立 URL 无统一入口和局部 mount isolation | 新 package、Manifest、lifecycle、bridge、version gate | Implement in Step 6 |
| PlatformSummaryFacade | Spec §5.4 DEC-105、§7.0、§8.3 | 四个平台独立 API/child 状态；shared 不拥有业务事实 | Portal 页面直接散落 `Promise.all` 会重复 partial/error/timeout 处理 | 1 个 host-side read-only Facade；不新增通用 BFF | Implement in Step 6 |
| Server-side summary API | Spec §9.1 P2 deferred row | 当前没有统一后端宿主或聚合服务 | Portal-side Facade 已满足本轮首页状态；没有部署/权限/性能证据证明必须新增 BFF | 新 Java service、权限、跨服务 timeout 和运维面 | Deferred; not in this Plan |
| Dynamic menu/database migration | Spec §7.0、§17 | RBAC3 `resourceDefinitions.json` 已含 MENU/ROUTE/ACTION/FIELD | 静态资源 + bootstrap capability 已满足当前导航权威 | 菜单表、缓存、发布、租户和审计成本无当前独立价值 | Removed |
| Java three-layer migration | Spec §6.1、DEC-106 | 四个 Admin Java modules 当前 feature-first，RBAC3 architecture tests 显式约束现状 | 本 Plan 不改 Java；后续新 Java API 单独使用传统 profile | 全历史迁移会扩大到数百 production files 和测试 | Context-only; separate Java Spec/Plan |

### 4.6 Change-unit Dependency Matrix

| Change unit | Requirements | Proof/RED point | Compile/runtime prerequisites | Produces | Consumers/unblocks | Owning Step |
| --- | --- | --- | --- | --- | --- | --- |
| Shared page header/state contract | `REQ-001`, `REQ-003`, `REQ-013` | PageHeader/PageState component tests | React/AntD/shared Vite config | `PageHeader`, stable PageState branches and exports | all platform pages | Step 1 |
| IDP page wrapper and mutation UI | `REQ-002`, `REQ-004`, `REQ-008`, `REQ-009`, `REQ-013`, `REQ-014` | page normalizer and page tests | shared export; existing `httpClient` | URL-aware list/Grant/audit UI | IDP App regression | Step 2 |
| RBAC3 route/API ownership | `REQ-002`, `REQ-004`, `REQ-008`, `REQ-010`, `REQ-013`, `REQ-014` | route/API path tests | existing Rbac3 SDK and FeatureApi | correct descriptor/component/path mapping | RBAC3 ApplicationRouter | Step 3 |
| Gateway observable/admin actions | `REQ-002`, `REQ-004`, `REQ-008`, `REQ-011`, `REQ-013`, `REQ-014` | gatewayApi and page tests | existing `apiRequest`, query state and capabilities | existing API methods consumed with safe states | Gateway routes/pages | Step 4 |
| DDC existing page/instance state | `REQ-002`, `REQ-004`, `REQ-008`, `REQ-012`, `REQ-013`, `REQ-014` | instance/page/polling tests | DDC `PageResultRecord`, `usePageState`, `/page` predecessor | `/instances` route and page; explicit deferred API state | DDC App/Layout | Step 5 |
| Wujie host/child lifecycle | `REQ-001`, `REQ-002`, `REQ-003`, `REQ-004`, `REQ-005`, `REQ-006`, `REQ-007`, `REQ-013`, `REQ-014`, `REQ-015`, `REQ-016` | Manifest/lifecycle/bridge/Facade tests | Steps 1-5 contracts; `wujie-react@2.1.0` lockfile | Portal package, child mount/unmount, partial summary | deployment/manual verification | Step 6 |

### 4.7 Java, Spring, and Egon-COLA Implementation Standards

This is a TypeScript/React Plan. No Java file, Spring Bean, Java dependency, database entity, Flyway migration, or Java public contract is in the change tree. The user’s DEC-106 is preserved as a future backend constraint, not silently waived. The frontend capability ledger is:

| Concern | Current repository evidence | Effective Spec decision | Planned implementation consequence | Owning Steps/checks |
| --- | --- | --- | --- | --- |
| Architecture profile | `egon-cola-platforms` has four independent Vite Web packages; Java Admin modules are feature-first | Spec §6.1/DEC-106 selects traditional three-layer for future Java | Keep current Web package boundaries; do not add Java or mix `biz.*` into existing modules | Step 1-6; `MC-ARCH-001` N/A for current non-Java scope |
| Reuse/capability | shared `EnterpriseLayout`/`PageState`; React Query; AntD; each package already has Vitest/typecheck/lint | Spec §3.1 G-006 and §7.0 keep/reuse decisions | Reuse existing components/clients/query cache; only add `wujie-react@2.1.0` in Portal | Step 1-6; `MC-REUSE-001`, `MC-DEP-001` |
| Naming/model/validation/conversion | TypeScript interfaces in `src/api/types.ts`; existing clients map protocol values | Java rules are not implementation scope; API JSON shape remains platform-owned | Use explicit TypeScript `PageResult`, `Manifest`, `MountState`, `PlatformSummary` names; no Java carrier/converter | Step 2-6; `MC-SCOPE-001`, `MC-TEST-001` |
| Bean/logging/util/JSON/time/config | No Java files; frontend uses browser `fetch`, package `import.meta.env`, and existing API errors | No Java annotations or Spring configuration changes | Do not create Java utility/Bean/config; Wujie fetch/props bridge never carries credentials | Step 6; `MC-DEP-001`, `MC-PATTERN-001` |
| Business variation/pattern | Host has lifecycle and partial-summary variations; pages already own platform state | Spec §13 selects Adapter, Facade, State, composition | `WujieChild` is Adapter boundary; `lifecycleState` is explicit State reducer; `PlatformSummaryFacade` owns partial aggregation; no universal BFF | Step 6; `MC-PATTERN-001` |

#### Capability reuse ledger

| Need | Candidates inspected | Exact evidence | Fit/gap | Decision | Added dependency/custom code | Owning Step/check |
| --- | --- | --- | --- | --- | --- | --- |
| Common shell | Existing shared React/AntD components | `egon-cola-platforms/egon-cola-platform-admin-web-shared/src/layout/EnterpriseLayout.tsx`, `EnterpriseSidebar.tsx` | Fits 240/72 Sidebar, Header, Drawer and active path requirements | Reuse and extend PageHeader/PageState only | No new UI framework | Step 1; `MC-REUSE-001` |
| Server state | TanStack Query in every Admin Web | package manifests and current `useQuery/useMutation` pages | Fits URL/query-cache/server state model | Reuse | None | Step 2-5; `MC-REUSE-001` |
| HTTP/auth | existing shared `createHttpClient`/`gatewayAuthClient`, platform API clients | shared `src/api/httpClient.ts`, `src/auth/gatewayAuthClient.ts`, IDP/Gateway/DDC clients | Fits same-site credentials and platform ownership | Reuse; host bridge never replaces bootstrap | None | Step 2-6; `MC-REUSE-001` |
| Wujie runtime | no existing local microfrontend runtime | `rg -n -i wujie/wijie/qiankun/microfrontend` has no repository implementation | Genuine gap after user approved hybrid host; official React wrapper and package metadata identify `wujie-react@2.1.0` | Add one exact Portal dependency after spike | `wujie-react@2.1.0`, package-lock impact only in Portal | Step 6; `MC-DEP-001` |
| Summary aggregation | existing platform API reads and child lifecycle state | four independent Web/API boundaries in Spec §2.4/§7.1 | Direct calls satisfy business pages but duplicate homepage partial/error handling | Add host-side read-only `PlatformSummaryFacade`; no server BFF | One local Facade and adapters | Step 6; `MC-PATTERN-001` |
| Menu authority | static platform definitions and RBAC3 registry | IDP `AdminLayout.tsx`, Gateway/DDC layouts, RBAC3 `resourceDefinitions.json` | Satisfies current route/capability ownership | Reuse; no DB menu API | None | Step 1-6; `MC-SCOPE-001` |

### 4.8 User-mandated Java Rule Implementation Matrix

The exact Java rule source remains normative in the selected skills and Spec §6.2. This Plan has no Java file; each row is evidence-backed `N/A` for the current Web scope, while Rule 11 records the confirmed future traditional profile.

| Literal rule | Spec source | Repository evidence | Exact files and order | Pseudocode obligations | Validation gate | Steps | Status/blocker |
| --- | --- | --- | --- | --- | --- | --- | --- |
| Rule 1 | Spec §6.2、§10 | No Java file in this Plan; frontend types are not Java POJO carriers | None; TypeScript files only | No Java suffix inventory; use explicit frontend `PageResult`, `Manifest`, `MountState` names | Web typecheck and changed-file review | Step 1-6 | N/A — non-Java Plan |
| Rule 2 | Spec §6.2、§9 | No Java Controller/Service/DAO handoff; browser forms use existing AntD validation | None; no Java validation group | No Java `@Valid`/`@Validated` design; frontend 422 mapping remains explicit | focused component tests | Step 1-6 | N/A — non-Java Plan |
| Rule 3 | Spec §6.2、§10 | No Java DTO/VO/PO/Entity/Converter | None | No Lombok/Record/MapStruct/BaseConverter file; no manual Java mapping | TypeScript compile/tests | Step 1-6 | N/A — non-Java Plan |
| Rule 4 | Spec §6.2、§13 | No Spring-managed business class | None | No `@Slf4j`/Bean/Qualifier obligations; React hooks stay local to platform | lint/typecheck | Step 1-6 | N/A — non-Java Plan |
| Rule 5 | Spec §6.2、§7.0 | No Java utility or dependency; existing browser/JDK-independent Web APIs are reused | `package.json` only changes in Portal for Wujie runtime | No Java utility allowlist decision; no second frontend utility library | dependency diff/import scan | Step 6 | N/A — non-Java Plan |
| Rule 6 | Spec §6.2、§9 | No Java external JSON contract; Manifest JSON is frontend-local and schema-tested | Portal Manifest files and tests | No Jackson file; Manifest fields are parsed and rejected when invalid | Manifest contract tests | Step 6 | N/A — non-Java Plan |
| Rule 7 | Spec §6.2、§15 | No Spring profile or Java configuration key | No Java profile files | No Spring profile parity work; child Vite env keys are listed per Portal contract | Portal build/typecheck and env-key review | Step 6 | N/A — non-Java Plan |
| Rule 9 | Spec §6.2、§13 | Complex variation is frontend host lifecycle/partial aggregation, not Java business logic | `WujieChild.tsx`, `lifecycleState.ts`, `platformSummaryFacade.ts` | Use Adapter/State/Facade composition; no string-dispatch or universal BFF | lifecycle/partial-failure tests | Step 6 | N/A — no Java business flow |
| Rule 10 | Spec §6.2、§10 | No Java date/time model or persistence change | None | Existing ISO strings remain display values; no Java `Date`/`Calendar` change | Web typecheck and page tests | Step 1-6 | N/A — non-Java Plan |
| Rule 11 | Spec §6.1、DEC-106、§8 | Four Web packages are current target; Java current tree is feature-first and untouched | Every target path stays inside current Web packages or new Portal package; future Java target is `biz.controller/service/service.impl/dao/config/utils/domain` | No Java hybrid/third structure; host uses frontend Adapter/Facade only | path inventory, package builds, Spec/Plan review | Every Step | N/A — no Java file; future profile confirmed |

## 5. Change File Tree

```text
egon-cola-platforms/
├── egon-cola-platform-admin-web-shared/
│   └── src/
│       ├── components/PageHeader.test.tsx                  CREATE
│       ├── components/PageHeader.tsx                       CREATE
│       ├── components/PageState.tsx                        MODIFY
│       ├── components/PageTemplate.tsx                     MODIFY
│       ├── i18n/en-US.ts                                   MODIFY
│       ├── i18n/zh-CN.ts                                   MODIFY
│       └── index.ts                                        MODIFY
├── egon-cola-platform-idp/egon-cola-platform-idp-admin-web/src/
│   ├── api/page.test.ts                                    CREATE
│   ├── api/page.ts                                         CREATE
│   ├── api/types.ts                                        MODIFY
│   ├── app/App.test.tsx                                    MODIFY
│   └── features/{users,clients,resource-servers,resource-grants,audits}/*  MODIFY/CREATE
├── egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin-web/src/
│   ├── app/App.integration.test.tsx                        MODIFY
│   └── features/{governance.routes,directory,role,constraint}/*             MODIFY/CREATE
├── egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web/src/
│   ├── api/gatewayApi.test.ts                               MODIFY
│   ├── api/gatewayApi.ts                                    MODIFY
│   ├── api/types.ts                                         MODIFY
│   └── features/{interface-catalog,releases,observability}/*               MODIFY/CREATE
├── egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin-web/src/
│   ├── api/client.test.ts                                   MODIFY
│   ├── api/types.ts                                         MODIFY
│   ├── App.test.tsx                                         MODIFY
│   ├── App.tsx                                              MODIFY
│   ├── layouts/AdminLayout.test.tsx                         MODIFY
│   ├── layouts/AdminLayout.tsx                              MODIFY
│   └── pages/InstancesPage.*                                CREATE
├── egon-cola-platform-admin-portal/
│   ├── package.json                                         CREATE
│   ├── package-lock.json                                    GENERATED
│   ├── index.html                                           CREATE
│   ├── vite.config.ts                                       CREATE
│   ├── tsconfig.json                                        CREATE
│   └── src/{app,bridge,lifecycle,manifest,pages,summary,test}/*             CREATE
└── four Admin Web/src/main.tsx and selected AdminLayout/App/router files      MODIFY for Wujie lifecycle
```

| Operation | Path | Current evidence/symbol | Final symbols/state | Responsibility | Step | Requirements | Validation owner |
| --- | --- | --- | --- | --- | --- | --- | --- |
| CREATE | `egon-cola-platforms/egon-cola-platform-admin-web-shared/src/components/PageHeader.test.tsx` | No PageHeader test exists; shared tests use Vitest/RTL | `rendersTitleBreadcrumbAndActions`, `rendersSubtitleAndAccessibleMainAction` | RED/GREEN contract for common page header | Step 1 | `REQ-001`, `REQ-003` | shared Vitest |
| CREATE | `egon-cola-platforms/egon-cola-platform-admin-web-shared/src/components/PageHeader.tsx` | No common PageHeader symbol; `PageTemplate.tsx` repeats title markup | `PageHeaderProps`, `PageHeader` | Title/subtitle/breadcrumb/action composition | Step 1 | `REQ-001`, `REQ-003` | shared typecheck/test |
| MODIFY | `egon-cola-platforms/egon-cola-platform-admin-web-shared/src/components/PageState.tsx` | Existing loading/empty/error/showPartial branches | Preserve branches and expose consistent retry/permission/partial copy | Step 1 | `REQ-003`, `REQ-013` | shared component tests |
| MODIFY | `egon-cola-platforms/egon-cola-platform-admin-web-shared/src/components/PageTemplate.tsx` | Existing Card title and PageState composition | Render PageHeader while preserving `PageTemplateProps` compatibility | Step 1 | `REQ-001`, `REQ-003` | shared typecheck/test |
| MODIFY | `egon-cola-platforms/egon-cola-platform-admin-web-shared/src/i18n/zh-CN.ts` | Existing common state/layout keys | Add only PageHeader/PageState/host-safe common copy keys | Step 1 | `REQ-003`, `REQ-013` | i18n key review |
| MODIFY | `egon-cola-platforms/egon-cola-platform-admin-web-shared/src/i18n/en-US.ts` | Mirrors `zh-CN` shape | Keep equivalent key structure and English values | Step 1 | `REQ-003`, `REQ-016` | key-parity review |
| MODIFY | `egon-cola-platforms/egon-cola-platform-admin-web-shared/src/index.ts` | Exports PageState/PageTemplate but no PageHeader | Export `PageHeader` and its prop type without changing old exports | Step 1 | `REQ-001`, `REQ-003` | package typecheck |
| CREATE | `egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-admin-web/src/api/page.test.ts` | IDP list pages each expect arrays | `normalizesArray`, `normalizesPage`, `keepsEmptyPage` | RED contract for old List/new page wrapper compatibility | Step 2 | `REQ-004`, `REQ-009` | IDP Vitest |
| CREATE | `egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-admin-web/src/api/page.ts` | No IDP page normalizer | `PageEnvelope<T>`, `normalizePage<T>` | Normalize `content/page/size/totalElements/totalPages` and old arrays | Step 2 | `REQ-004`, `REQ-009` | IDP typecheck/test |
| MODIFY | `egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-admin-web/src/api/types.ts` | User/Client/Resource Server types lack stable page aliases; audit filters absent | Add page aliases and filter request types, preserving existing DTO/VO fields | Step 2 | `REQ-004`, `REQ-008`, `REQ-009` | typecheck/API fixture |
| CREATE | `egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-admin-web/src/features/users/UserListPage.test.tsx` | No focused user page test; current page requests unbounded array | Tests URL filter/page, empty/error, pending mutation and one-time password redaction | Step 2 | `REQ-002`, `REQ-004`, `REQ-009`, `REQ-013`, `REQ-014` | IDP RTL/Vitest |
| MODIFY | `egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-admin-web/src/features/users/UserListPage.tsx` | Direct `/users` array query and local mutations | URL-backed filter/page, normalized page, permission-aware actions, 422/409 retention | Step 2 | `REQ-002`, `REQ-004`, `REQ-009`, `REQ-013`, `REQ-014` | IDP user test/App test |
| MODIFY | `egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-admin-web/src/features/clients/ClientListPage.test.tsx` | Existing client test covers secret safety but not page/filter state | Add page/filter/detail/Grant entry assertions and precise invalidation | Step 2 | `REQ-004`, `REQ-007`, `REQ-009`, `REQ-014` | IDP client test |
| MODIFY | `egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-admin-web/src/features/clients/ClientListPage.tsx` | Direct array query; detail/secret actions exist | Normalized paged table, URL filter, detail drawer, one-time secret lifecycle and Grant link | Step 2 | `REQ-002`, `REQ-004`, `REQ-007`, `REQ-009`, `REQ-013`, `REQ-014` | IDP client/App test |
| CREATE | `egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-admin-web/src/features/resource-servers/ResourceServerListPage.test.tsx` | Existing page has no focused test; backend detail/batch already exist | Assert page state, detail drawer, batch pending and old/new page response | Step 2 | `REQ-004`, `REQ-009`, `REQ-013`, `REQ-014` | IDP Vitest |
| MODIFY | `egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-admin-web/src/features/resource-servers/ResourceServerListPage.tsx` | Direct array query and single-row actions | Normalize page, add filter/page state, detail/batch controls, safe status refresh | Step 2 | `REQ-002`, `REQ-004`, `REQ-009`, `REQ-013`, `REQ-014` | resource server test |
| CREATE | `egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-admin-web/src/features/resource-grants/ClientResourceGrantPage.test.tsx` | Current page only loads Resource Servers and PUTs Grant | Assert existing PUT/DELETE/batch paths, candidate read 404 state, conflict retention | Step 2 | `REQ-007`, `REQ-008`, `REQ-009`, `REQ-013`, `REQ-014` | IDP RTL/Vitest |
| MODIFY | `egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-admin-web/src/features/resource-grants/ClientResourceGrantPage.tsx` | Current table cannot show current Grant and only saves | Consume approved existing mutations; expose read-capability unavailable state instead of false empty; add delete/batch controls when capability exists | Step 2 | `REQ-002`, `REQ-007`, `REQ-008`, `REQ-009`, `REQ-013`, `REQ-014` | Grant focused test |
| CREATE | `egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-admin-web/src/features/audits/AuditLogPage.test.tsx` | Current query only page/size and no filters | Assert filter serialization, page reset, empty/403/retry and extra filters remain explicit | Step 2 | `REQ-004`, `REQ-008`, `REQ-009`, `REQ-013` | IDP audit test |
| MODIFY | `egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-admin-web/src/features/audits/AuditLogPage.tsx` | Direct page query and table without detail/filter | URL filter bar, normalized page, detail redaction and deferred export state | Step 2 | `REQ-004`, `REQ-008`, `REQ-009`, `REQ-013`, `REQ-014` | audit focused test |
| MODIFY | `egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-admin-web/src/app/App.test.tsx` | Existing route/API fixture assertions | Update fixtures for page wrapper and assert all revised paths/permission states | Step 2 | `REQ-002`, `REQ-004`, `REQ-008`, `REQ-009`, `REQ-013` | IDP App test |
| MODIFY | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin-web/src/features/governance.routes.tsx` | Organizations and positions both use `UserDirectoryPage` | Bind `/iam/organizations` to `OrganizationPage`, `/iam/positions` to `PositionPage` | Step 3 | `REQ-003`, `REQ-010` | RBAC3 route test |
| CREATE | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin-web/src/features/directory/OrganizationPage.test.tsx` | No organization page test | Assert organization tree/list ownership, scope, empty/403 and CRUD entry | Step 3 | `REQ-002`, `REQ-003`, `REQ-010`, `REQ-013`, `REQ-014` | RBAC3 RTL/Vitest |
| CREATE | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin-web/src/features/directory/OrganizationPage.tsx` | Backend OrganizationController exists; frontend route has wrong component | Organization-specific table/tree shell with existing FeatureApi request paths and version-aware edit boundary | Step 3 | `REQ-003`, `REQ-010`, `REQ-013`, `REQ-014` | organization test/typecheck |
| CREATE | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin-web/src/features/directory/PositionPage.test.tsx` | No position page test | Assert position list/org filter, empty/403 and edit entry | Step 3 | `REQ-002`, `REQ-003`, `REQ-010`, `REQ-013`, `REQ-014` | RBAC3 RTL/Vitest |
| CREATE | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin-web/src/features/directory/PositionPage.tsx` | Backend PositionController exists; route currently renders user lookup | Position-specific list/detail shell; do not query user detail endpoint as substitute | Step 3 | `REQ-003`, `REQ-010`, `REQ-013`, `REQ-014` | position test/typecheck |
| MODIFY | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin-web/src/features/role/role.api.ts` | `roles`/`impact` omit `/iam`; resources already include it | Change only list/impact paths to `/api/rbac3/v1/iam/roles...`; preserve role resource paths and string IDs | Step 3 | `REQ-008`, `REQ-010` | API path test |
| MODIFY | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin-web/src/features/constraint/constraint.api.ts` | Constraint client calls top-level `/sod-sets`/`/data-rules` paths | Map all reads under `/api/rbac3/v1/iam/policies`; preserve typed result names | Step 3 | `REQ-008`, `REQ-010` | constraint API/page test |
| MODIFY | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin-web/src/features/directory/directory.api.ts` | Snapshot submit calls `/api/rbac3/v1/directory/snapshots`; backend is `/internal/directory-snapshots` | Use exact controller path; keep generatedAt ISO and payload JSON boundary | Step 3 | `REQ-008`, `REQ-010`, `REQ-014` | directory API test |
| MODIFY | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin-web/src/features/directory/DirectoryPages.test.tsx` | Existing test covers user ID string/snapshot copy | Add organization/position route component and corrected snapshot path assertions | Step 3 | `REQ-003`, `REQ-008`, `REQ-010` | RBAC3 focused test |
| MODIFY | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin-web/src/features/role/RolePages.test.tsx` | Existing role tests cover role/resource behavior | Add `/iam` list/impact path assertions and conflict/pending UI | Step 3 | `REQ-008`, `REQ-010`, `REQ-013`, `REQ-014` | role focused test |
| MODIFY | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin-web/src/features/constraint/ConstraintPage.test.tsx` | Existing constraint page test covers validation helper | Add exact policy paths, tab failure/empty states and permission guard | Step 3 | `REQ-008`, `REQ-010`, `REQ-013` | constraint focused test |
| MODIFY | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin-web/src/app/App.integration.test.tsx` | Existing integration tests verify visible routes and denied route | Assert organizations/positions component ownership and no removed route/path mismatch | Step 3 | `REQ-002`, `REQ-003`, `REQ-010`, `REQ-013` | RBAC3 App integration |
| MODIFY | `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web/src/api/types.ts` | `OperationDetail`/Trace types lack explicit safe detail/status variants | Add `TraceDetail`/metadata request result types only for existing/candidate response handling; keep sensitive fields excluded | Step 4 | `REQ-007`, `REQ-008`, `REQ-011`, `REQ-013` | Gateway typecheck/contract fixtures |
| MODIFY | `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web/src/api/gatewayApi.test.ts` | Existing API tests cover core calls | Add exact Release Diff, operation metadata/manual definition/deprecate and trace detail/unavailable call assertions | Step 4 | `REQ-008`, `REQ-011`, `REQ-014` | Gateway API test |
| MODIFY | `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web/src/api/gatewayApi.ts` | Existing methods include `draftDiff`, `release`, `traces`, but not catalog lifecycle methods | Add only wrappers for existing Controller paths; candidate trace detail returns typed unavailable/error without raw body | Step 4 | `REQ-008`, `REQ-011`, `REQ-013`, `REQ-014` | gatewayApi test/typecheck |
| MODIFY | `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web/src/features/interface-catalog/OperationPage.test.tsx` | Operation page has schema/catalog tests but no lifecycle mutation assertions | Add metadata/manual-definition/deprecate permission, pending, 409 and redaction assertions | Step 4 | `REQ-002`, `REQ-007`, `REQ-011`, `REQ-013`, `REQ-014` | Operation RTL/Vitest |
| MODIFY | `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web/src/features/interface-catalog/OperationPage.tsx` | Operation detail is mostly read-only despite existing backend mutation paths | Add tabs and guarded mutation controls; MANUAL writable, RPC/OPENAPI read-only; conflict keeps local draft | Step 4 | `REQ-002`, `REQ-007`, `REQ-011`, `REQ-013`, `REQ-014` | Operation page test |
| CREATE | `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web/src/features/releases/ReleaseDetailPage.test.tsx` | Release detail has no focused test file | Assert retry/rollback pending, reason, unknown/partial status and no false-success copy | Step 4 | `REQ-011`, `REQ-013`, `REQ-014` | Gateway RTL/Vitest |
| MODIFY | `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web/src/features/releases/ReleaseDetailPage.tsx` | Existing release page already calls retry/rollback and displays structured diff | Add explicit Diff/target timeline states, safe error/unknown copy, exact invalidation and action guards | Step 4 | `REQ-002`, `REQ-011`, `REQ-013`, `REQ-014` | Release detail test |
| MODIFY | `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web/src/features/observability/TracesPage.test.tsx` | Existing trace tests cover summary/refresh baseline | Add filter serialization, page reset, detail lazy-load/unavailable and sensitive-field negative assertions | Step 4 | `REQ-004`, `REQ-007`, `REQ-011`, `REQ-013`, `REQ-014` | Trace RTL/Vitest |
| MODIFY | `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web/src/features/observability/TracesPage.tsx` | Current page has env/namespace/trace/protocol/status filters and 5s refresh, no detail drawer | Keep visible polling bounded; add scope/filter fields, row detail boundary and safe unavailable state; no raw Body/Header | Step 4 | `REQ-004`, `REQ-007`, `REQ-011`, `REQ-013`, `REQ-014` | Trace page test |
| MODIFY | `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin-web/src/api/client.test.ts` | Client tests cover envelope/page parsing | Add `/instances/page` and deferred-audit 404/error classification assertions | Step 5 | `REQ-008`, `REQ-012`, `REQ-013` | DDC client test |
| MODIFY | `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin-web/src/api/types.ts` | Existing `DdcInstance`, `PageResultRecord` already exist | Add explicit audit capability/error view and preserve DDC predecessor page metadata | Step 5 | `REQ-004`, `REQ-008`, `REQ-012`, `REQ-013` | DDC typecheck/test |
| MODIFY | `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin-web/src/App.test.tsx` | Existing route tests cover registry/configs/etc. | Assert `/instances` route, navigation label and no false `/audits` success when API is absent | Step 5 | `REQ-002`, `REQ-003`, `REQ-008`, `REQ-012` | DDC App test |
| MODIFY | `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin-web/src/App.tsx` | Root redirects to `/registry`, no instances route | Register `/instances` and preserve all existing routes; do not register uncontracted audit route as success | Step 5 | `REQ-003`, `REQ-005`, `REQ-012` | DDC App test/typecheck |
| MODIFY | `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin-web/src/layouts/AdminLayout.test.tsx` | Existing layout test covers current menu | Add instance menu visibility and active-path behavior | Step 5 | `REQ-001`, `REQ-002`, `REQ-003` | DDC layout test |
| MODIFY | `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin-web/src/layouts/AdminLayout.tsx` | Runtime menu has registry/publish/cache but no instances | Add `配置客户端实例` static route item under runtime; keep permissions from bootstrap and shared Sidebar | Step 5 | `REQ-001`, `REQ-002`, `REQ-003`, `REQ-012` | DDC layout/App test |
| CREATE | `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin-web/src/pages/InstancesPage.test.tsx` | Backend instance page exists; no frontend page/test | Assert scope filter, pageNo/pageSize, empty/error/retry and stable instance row key | Step 5 | `REQ-003`, `REQ-004`, `REQ-012`, `REQ-013` | DDC RTL/Vitest |
| CREATE | `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin-web/src/pages/InstancesPage.tsx` | `DdcInstance` and `ddcPageApi` exist; no route consumer | Server-paged instance table with scope selectors, status/lease/heartbeat details and PageState | Step 5 | `REQ-003`, `REQ-004`, `REQ-012`, `REQ-013` | DDC page/typecheck |
| CREATE | `egon-cola-platforms/egon-cola-platform-admin-portal/package.json` | No Portal package exists; root has React/Vite/AntD versions | Private React 19/Vite package with exact `wujie-react: 2.1.0`, existing shared package, React Router and Vitest scripts | Step 6 | `REQ-005`, `REQ-006`, `REQ-015` | npm dependency/build gate |
| GENERATED | `egon-cola-platforms/egon-cola-platform-admin-portal/package-lock.json` | No Portal lockfile exists | Lock exact dependency graph after `npm install --package-lock-only --ignore-scripts --save-exact wujie-react@2.1.0` | Step 6 | `REQ-006`, `REQ-016` | lockfile diff/license review |
| CREATE | `egon-cola-platforms/egon-cola-platform-admin-portal/index.html` | New Vite app requires HTML entry | `root` mount node and no credential-bearing inline script | Step 6 | `REQ-005`, `REQ-007` | Portal build |
| CREATE | `egon-cola-platforms/egon-cola-platform-admin-portal/vite.config.ts` | Existing apps use React plugin/favicon plugin and Vitest config | React/Vitest build, same-site `/api` proxy contract, test jsdom and no cross-origin token relay | Step 6 | `REQ-005`, `REQ-006`, `REQ-007` | Portal typecheck/test/build |
| CREATE | `egon-cola-platforms/egon-cola-platform-admin-portal/tsconfig.json` | Existing Vite apps use strict referenced configs | Strict TypeScript config for `src`, JSX, no unused locals/parameters | Step 6 | `REQ-006`, `REQ-016` | Portal typecheck |
| CREATE | `egon-cola-platforms/egon-cola-platform-admin-portal/src/main.tsx` | New Portal has no entry | Inject shared tokens/i18n and render Portal App under React StrictMode | Step 6 | `REQ-001`, `REQ-003`, `REQ-005` | Portal typecheck/test |
| CREATE | `egon-cola-platforms/egon-cola-platform-admin-portal/src/app/App.tsx` | New Portal has no provider composition | Compose QueryClient, shared theme/i18n, Router and top-level error boundary without business API ownership | Step 6 | `REQ-001`, `REQ-005`, `REQ-013`, `REQ-015` | Portal component test |
| CREATE | `egon-cola-platforms/egon-cola-platform-admin-portal/src/app/router.tsx` | No host routes/standalone links | Define `/`, `/platform/:platformKey/*`, `/standalone/:platformKey`, 404 and platform switch route state | Step 6 | `REQ-002`, `REQ-004`, `REQ-005`, `REQ-015` | Portal router test |
| CREATE | `egon-cola-platforms/egon-cola-platform-admin-portal/src/manifest/types.ts` | No manifest contract | `PlatformKey`, `ChildManifest`, `ManifestEnvironment`, version range, required capability and standalone URL types | Step 6 | `REQ-005`, `REQ-006`, `REQ-015` | manifest contract test |
| CREATE | `egon-cola-platforms/egon-cola-platform-admin-portal/src/manifest/loader.ts` | No Manifest loader | Fetch same-site versioned manifest, validate required fields/range, reject invalid URL/version before mount | Step 6 | `REQ-005`, `REQ-006`, `REQ-007`, `REQ-013` | loader test |
| CREATE | `egon-cola-platforms/egon-cola-platform-admin-portal/src/manifest/loader.test.ts` | No Manifest test | Assert valid/invalid/missing/version mismatch/timeout outcomes and no sensitive field accepted | Step 6 | `REQ-005`, `REQ-006`, `REQ-007`, `REQ-013` | Portal Vitest |
| CREATE | `egon-cola-platforms/egon-cola-platform-admin-portal/src/lifecycle/lifecycleState.ts` | No host mount state | Reducer/state model for `IDLE`, `LOADING`, `MOUNTED`, `MOUNT_FAILED`, `UNMOUNTING`, `CRASHED` with retry/remount transitions | Step 6 | `REQ-005`, `REQ-013`, `REQ-015` | lifecycle state test |
| CREATE | `egon-cola-platforms/egon-cola-platform-admin-portal/src/lifecycle/lifecycleState.test.ts` | No lifecycle state test | Assert legal transitions, cleanup-before-remount and terminal error copy | Step 6 | `REQ-005`, `REQ-013`, `REQ-015` | Portal Vitest |
| CREATE | `egon-cola-platforms/egon-cola-platform-admin-portal/src/lifecycle/WujieChild.tsx` | No Wujie wrapper | Adapter around `WujieReact` with props, hooks, error boundary, cleanup and standalone link | Step 6 | `REQ-005`, `REQ-006`, `REQ-007`, `REQ-013`, `REQ-015` | Portal component/integration test |
| CREATE | `egon-cola-platforms/egon-cola-platform-admin-portal/src/bridge/context.ts` | No host-child bridge | Allow-list non-sensitive `platformKey`, `routeIntent`, `scopeDisplay`, `capabilitySummary`, `hostVersion`; strip credentials/body | Step 6 | `REQ-007`, `REQ-015` | bridge security test |
| CREATE | `egon-cola-platforms/egon-cola-platform-admin-portal/src/bridge/context.test.ts` | No bridge security test | Assert token/secret/cookie/authorization/raw body fields are removed from props/events | Step 6 | `REQ-007` | Portal Vitest |
| CREATE | `egon-cola-platforms/egon-cola-platform-admin-portal/src/summary/platformSummaryFacade.ts` | No summary orchestration; four child states are independent | `loadSummary` uses approved read-only adapters/child state, `Promise.allSettled`, per-platform timeout and explicit partial/error/not-configured result | Step 6 | `REQ-001`, `REQ-004`, `REQ-008`, `REQ-013` | Facade partial test |
| CREATE | `egon-cola-platforms/egon-cola-platform-admin-portal/src/summary/platformSummaryFacade.test.ts` | No partial summary test | Assert one adapter timeout leaves other cards visible, no partial result becomes global success, retry is isolated | Step 6 | `REQ-001`, `REQ-004`, `REQ-013`, `REQ-014` | Portal Vitest |
| CREATE | `egon-cola-platforms/egon-cola-platform-admin-portal/src/pages/PortalHomePage.tsx` | No unified host home | Left platform navigation, summary cards, mount/health state, independent links and per-card retry | Step 6 | `REQ-001`, `REQ-002`, `REQ-003`, `REQ-004`, `REQ-005`, `REQ-013`, `REQ-015` | Portal RTL |
| CREATE | `egon-cola-platforms/egon-cola-platform-admin-portal/src/pages/PortalHomePage.test.tsx` | No host home test | Assert static platform groups, capability pruning, partial card and standalone fallback | Step 6 | `REQ-001`, `REQ-002`, `REQ-003`, `REQ-005`, `REQ-013`, `REQ-015` | Portal Vitest |
| CREATE | `egon-cola-platforms/egon-cola-platform-admin-portal/src/pages/ChildRoutePage.tsx` | No child route host page | Load/validate Manifest, mount selected child, display local loading/error/remount/standalone controls | Step 6 | `REQ-004`, `REQ-005`, `REQ-006`, `REQ-013`, `REQ-015` | Portal integration |
| CREATE | `egon-cola-platforms/egon-cola-platform-admin-portal/src/pages/ChildRoutePage.test.tsx` | No child route test | Assert manifest failure, child crash isolation, unmount cleanup and route-preserving retry | Step 6 | `REQ-005`, `REQ-006`, `REQ-013`, `REQ-015` | Portal Vitest |
| CREATE | `egon-cola-platforms/egon-cola-platform-admin-portal/src/test/setup.ts` | No Portal test setup | jsdom, Testing Library matchMedia/ResizeObserver and WujieReact mock boundary | Step 6 | `REQ-006`, `REQ-016` | Portal test bootstrap |
| MODIFY | `egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-admin-web/src/main.tsx` | Direct `createRoot(...).render(<App/>)`, no Wujie lifecycle | Expose `window.__WUJIE_MOUNT/UNMOUNT`, call `window.__WUJIE.mount()` when powered by Wujie, retain standalone createRoot | Step 6 | `REQ-005`, `REQ-006`, `REQ-015` | IDP build/lifecycle test |
| MODIFY | `egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-admin-web/src/app/App.tsx` | `App` always wraps BrowserRouter and AppRouter | Accept embedded context and pass it to AppRouter without changing standalone providers | Step 6 | `REQ-005`, `REQ-015` | IDP typecheck/App test |
| MODIFY | `egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-admin-web/src/app/router.tsx` | `AppRouter` always renders full `AdminLayout` | Use `window.$wujie.props.embedded`/explicit prop to hide duplicate outer shell while retaining route guards | Step 6 | `REQ-002`, `REQ-005`, `REQ-007` | IDP route test |
| MODIFY | `egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-admin-web/src/app/AdminLayout.tsx` | Always renders EnterpriseLayout | Add embedded branch that returns children only; standalone path keeps current shared shell/navigation | Step 6 | `REQ-001`, `REQ-005`, `REQ-015` | IDP App test |
| MODIFY | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin-web/src/main.tsx` | Direct root render, no Wujie lifecycle | Expose mount/unmount and retain Rbac3Provider/FeatureApiProvider in both modes | Step 6 | `REQ-005`, `REQ-006`, `REQ-015` | RBAC3 build/lifecycle test |
| MODIFY | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin-web/src/app/App.tsx` | Providers and BrowserRouter have no embedded mode | Carry embedded flag to ApplicationRouter while preserving SDK/provider order | Step 6 | `REQ-005`, `REQ-007`, `REQ-015` | RBAC3 typecheck/integration |
| MODIFY | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin-web/src/app/router.tsx` | Local `AdminLayout` always wraps EnterpriseLayout | Add embedded branch and retain SDK route guard/capability semantics | Step 6 | `REQ-001`, `REQ-002`, `REQ-005`, `REQ-007` | RBAC3 integration |
| MODIFY | `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web/src/main.tsx` | Direct root render, no Wujie lifecycle | Expose mount/unmount and use `$wujie.props.embedded` only for layout presentation | Step 6 | `REQ-005`, `REQ-006`, `REQ-015` | Gateway build/lifecycle test |
| MODIFY | `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web/src/layouts/AdminLayout.tsx` | Always renders shared outer shell | Embedded branch hides duplicate Header/Sidebar; page routes/API clients remain unchanged | Step 6 | `REQ-001`, `REQ-002`, `REQ-005`, `REQ-015` | Gateway layout/typecheck |
| MODIFY | `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin-web/src/main.tsx` | Direct root render with QueryClient/Antd providers | Expose mount/unmount while preserving DDC provider order and standalone mode | Step 6 | `REQ-005`, `REQ-006`, `REQ-015` | DDC build/lifecycle test |
| MODIFY | `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin-web/src/layouts/AdminLayout.tsx` | Always renders EnterpriseLayout | Embedded branch hides duplicate global shell and keeps static DDC menu for standalone | Step 6 | `REQ-001`, `REQ-002`, `REQ-005`, `REQ-015` | DDC layout/App test |

## 6. Prerequisites, Constraints, and Plan Clarifications

### 6.1 Repository and worktree baseline

- 当前分支为 `main`，基线为 `95039c0b`；工作区已有 `docs/egon/plan/`、`docs/egon/spec/` 修改、未跟踪 Plan/Spec 和 `egon-cola-archetype-web-open/`，执行时只允许 path-limited stage 本 Plan 所列文件。
- 适用的本地 AGENTS 约束要求：不自动启动项目；不修改无关文件；每个 Step 一次语义提交；Flyway 历史迁移不可修改；本 Plan 不涉及数据库。
- 四个 Web 当前均有独立 `package.json`、Vite、Vitest、typecheck/lint；shared 有 `EnterpriseLayout`/`EnterpriseSidebar`/`PageState`/`PageTemplate`；Portal 当前不存在。
- 既有 Java Admin 模块的 feature-first 结构和架构测试是只读背景证据；本 Plan 不触及 `egon-cola-platforms/**/src/main/java`、`pom.xml`、`lombok.config` 或 Flyway。

### 6.2 Build, test, and environment prerequisites

| Concern | Exact command/source | Required state | Validation boundary |
| --- | --- | --- | --- |
| Shared baseline | `cd egon-cola-platforms/egon-cola-platform-admin-web-shared && npm run typecheck && npm run test -- --run` | Current shared package compiles and existing tests pass before Step 1 | static/package |
| IDP baseline | `cd egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-admin-web && npm run typecheck && npm run test -- --run` | Current IDP fixtures are green before Step 2 | static/package |
| RBAC3 baseline | `cd egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin-web && npm run typecheck && npm run test -- --run` | Existing SDK mock/integration tests are green before Step 3 | static/package |
| Gateway baseline | `cd egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web && npm run typecheck && npm run test -- --run` | Existing API/page tests are green before Step 4 | static/package |
| DDC baseline | `cd egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin-web && npm run typecheck && npm run test -- --run` | Existing DDC PageResultRecord/query tests are green before Step 5 | static/package |
| Wujie package | `cd egon-cola-platforms/egon-cola-platform-admin-portal && npm install --package-lock-only --ignore-scripts --save-exact wujie-react@2.1.0` | Official/package metadata baseline is `wujie-react@2.1.0`; lockfile is generated and reviewed before any host code | dependency/static; no runtime |
| Portal standalone build | `cd egon-cola-platforms/egon-cola-platform-admin-portal && npm run typecheck && npm run test -- --run && npm run build` | Portal build emits a standalone artifact and test suite passes without services | static/package |

### 6.3 Immutable constraints and approved decisions

- Wujie package scope is Portal-only; do not add `wujie-react` to IDP/RBAC3/Gateway/DDC packages.
- Same-site Cookie/CSRF remains the auth boundary. `credentials: include` stays in existing clients; host props/event payloads cannot contain cookies, access tokens, refresh tokens, Client Secret, raw Authorization, raw Body or sensitive Header.
- Static/config-driven menu remains authoritative. Portal can show platform-level links, but each child retains its own route/capability registry; no database menu endpoint or menu migration.
- Existing DDC `/page` design and old List/RPC compatibility remain unchanged. No existing Flyway file can be modified.
- Frontend may represent an absent backend capability explicitly; it must not render a 404/501 candidate endpoint as a successful empty list.
- Future Java interface work must use traditional `biz.controller`, `biz.service`, `biz.service.impl`, `biz.dao`, `biz.config`, `biz.utils`, `biz.domain`; this Plan has no Java implementation.

### 6.4 Plan Clarifications

| ID | Small implementation inference | Repository evidence | Why semantics are unchanged | Impact if wrong |
| --- | --- | --- | --- | --- |
| `PLAN-CLAR-001` | 将 Spec §8 的 generic `src/manifest`、`src/lifecycle`、`src/bridge`、`src/summary` 展开为本 Plan 的 exact filenames | Portal 当前不存在，Spec §8.2/§8.3 只规定职责；现有 Web 使用按 feature 目录组织 | 只确定局部文件名和测试放置，不增加模块、接口或业务事实 | 仅需调整 Portal 内部路径，不改变 Manifest/bridge/lifecycle 合同 |
| `PLAN-CLAR-002` | PageHeader 只抽取标题/副标题/面包屑/主操作，不新增万能业务表格或 Scope 数据组件 | shared `PageTemplate.tsx` 已有 title/subtitle/breadcrumbs/extra props；四个平台有重复标题壳 | 保留现有 `PageTemplateProps`，PageHeader 不拥有业务筛选或数据请求 | 若视觉审查不接受抽取，可回退 PageHeader 文件而不影响 API/page contracts |
| `PLAN-CLAR-003` | Portal summary 首期允许 adapter 返回 `NOT_CONFIGURED`，等待 §9 后端摘要合同，不把业务 API 猜成固定路径 | Spec §9 P2 summary API 明确 deferred；各平台 scope model 不同且当前无统一 summary endpoint | 首页仍有统一 partial/error 状态和独立入口，未制造新的后端合同 | 若后续确定服务端摘要 API，只替换 adapter，不改变 Portal card/result state |
| `PLAN-CLAR-004` | Wujie 子应用用 `window.__POWERED_BY_WUJIE__` 与 `$wujie.props.embedded` 作为运行态探测，入口同时保留 standalone 分支 | Wujie 官方 Vite/React wrapper 文档记录该 lifecycle/global 约定；现有四个入口都是直接 `createRoot` | 探测只影响壳和 mount lifecycle，不改变子应用业务 API、权限或数据所有权 | 若 spike 发现 wrapper lifecycle 差异，只改 Adapter/entry glue，不改 page contracts |

## 7. Ordered File-by-file Implementation Steps

### Step 1 — 建立 shared 页面标题与统一状态壳

- Requirements: `REQ-001, REQ-003, REQ-013, REQ-016`
- Dependencies: `None`
- Baseline state: shared 已有 `EnterpriseLayout`/`EnterpriseSidebar`、`PageState`、`PageTemplate` 和测试；缺少可复用 PageHeader，页面状态文案和标题结构由各页面自行决定。
- Observable outcome: shared 导出稳定的 `PageHeader`；`PageTemplate` 使用它但兼容原有 props；PageState 的 loading/empty/error/partial/retry 语义有聚焦测试。
- End state: shared package 可被四个 Web 继续导入；没有改菜单权威、认证、API 或 Java/数据库。
- Test-first gate: `Required — File 1 先建立 PageHeader import/behavior RED；模块不存在时的编译失败是已知 compile prerequisite，File 2 只提供最小契约后使行为断言继续 RED/GREEN。`
- Manual Checks: `MC-REUSE-001, MC-SCOPE-001, MC-TEST-001`
- Literal Rules: `Rule 11` — no Java file; preserve the Spec-selected future traditional profile and current Web package boundaries.
- Ordered files:

#### File 1 — `CREATE egon-cola-platforms/egon-cola-platform-admin-web-shared/src/components/PageHeader.test.tsx`

- Purpose: 在生产组件前固定标题、说明、面包屑和主操作的可见/可访问合同。
- Symbols: `PageHeader` import；`rendersTitleBreadcrumbAndActions`；`rendersSubtitleAndAccessibleMainAction`。
- Repository evidence: `src/layout/EnterpriseLayout.test.tsx` 使用 Testing Library/Vitest；`PageTemplate.tsx` 已有 `title/subtitle/breadcrumbs/extra` 输入。
- Dependencies and consumers: 预期依赖 `PageHeader.tsx`、AntD `Breadcrumb/Typography/Button`；Step 1 的 PageTemplate 和后续平台页面消费。
- Why now: 这是本 Step 唯一的 RED 行为证明；先锁定对用户可见的壳，不先改变 shared 生产行为。
- Contract/signature changes: 测试使用 `PageHeader({title, subtitle, breadcrumbs, extra})`；主操作按钮必须有可读名称，面包屑文本必须按输入顺序出现。
- Input/output and state mapping: title/subtitle/breadcrumb list/extra node -> header DOM；无 subtitle 时不产生空占位；无 extra 时不渲染伪按钮。
- Error and edge behavior: 空 breadcrumbs 不渲染 Breadcrumb；长 title 保留文本；主操作只验证渲染，不触发业务请求；测试不会把 href/route 业务规则引入 shared。
- Standards impact: `MC-SCOPE-001, MC-TEST-001` — 只新增 shared UI test，复用现有 RTL/Vitest setup；`MC-REUSE-001` — 复用 AntD/shared test fixtures。
- Literal rule enforcement: `Rule 11` — test path stays under the existing shared Web package; no Java architecture is introduced.
- Implementation pseudocode:

```tsx
it('renders title breadcrumb and main action', () => {
  render(<PageHeader title="配置资源" breadcrumbs={[{title: '配置中心'}, {title: '配置资源'}]} extra={<button>新建配置</button>} />)
  expect(screen.getByRole('heading', {name: '配置资源'})).toBeInTheDocument()
  expect(screen.getByText('配置中心')).toBeInTheDocument()
  expect(screen.getByRole('button', {name: '新建配置'})).toBeInTheDocument()
})

it('omits empty subtitle and accepts an accessible action', () => {
  render(<PageHeader title="审计" extra={<button aria-label="刷新审计">刷新</button>} />)
  expect(screen.queryByText('undefined')).not.toBeInTheDocument()
  expect(screen.getByRole('button', {name: '刷新审计'})).toBeInTheDocument()
})
```

- Verification contribution: `npm run test -- --run src/components/PageHeader.test.tsx` first fails for the absent component/import and then proves the exact rendering contract after File 2.
- After this file: the new test file exists with an intentional RED compile/behavior gate; no production file or unrelated test changed.

#### File 2 — `CREATE egon-cola-platforms/egon-cola-platform-admin-web-shared/src/components/PageHeader.tsx`

- Purpose: 提供不拥有业务数据的页面标题/面包屑/主操作组合组件。
- Symbols: `PageHeaderProps`；`PageHeader`。
- Repository evidence: `PageTemplateProps` already defines exact title/subtitle/breadcrumbs/extra shape；AntD is a shared peer dependency。
- Dependencies and consumers: imports AntD `Breadcrumb`, `Space`, `Typography`; consumed by PageTemplate and later pages through shared index.
- Why now: supplies the compile prerequisite and minimum implementation for File 1 without introducing a generic table/form abstraction。
- Contract/signature changes: `PageHeaderProps` has readonly `title: string`, optional `subtitle`, optional readonly `breadcrumbs: readonly BreadcrumbItem[]`, optional `extra: ReactNode`; output is a `<section>` with heading and optional Breadcrumb/extra.
- Input/output and state mapping: each breadcrumb title/path maps to existing `BreadcrumbItem`; a path is rendered as an anchor only as existing PageTemplate does; extra is rendered unchanged and has no data side effect。
- Error and edge behavior: empty title is still a caller validation concern and rendered as provided; empty breadcrumb array is omitted; no navigation callback or API request is added。
- Standards impact: `MC-REUSE-001, MC-SCOPE-001, MC-TEST-001` — use existing AntD and shared `BreadcrumbItem`; no new package/state store; component test observes all branches。
- Literal rule enforcement: `Rule 11` — file remains in the established shared frontend package; no Java layer or third architecture.
- Implementation pseudocode:

```tsx
export interface PageHeaderProps {
  readonly title: string
  readonly subtitle?: string
  readonly breadcrumbs?: readonly BreadcrumbItem[]
  readonly extra?: ReactNode
}

export const PageHeader = ({title, subtitle, breadcrumbs, extra}: PageHeaderProps) => (
  <div className="egon-page-header">
    {breadcrumbs && breadcrumbs.length > 0 && <Breadcrumb items={breadcrumbs.map(toBreadcrumbItem)} />}
    <Space align="start" style={{display: 'flex', justifyContent: 'space-between'}}>
      <div><Typography.Title level={4}>{title}</Typography.Title>{subtitle && <Typography.Text type="secondary">{subtitle}</Typography.Text>}</div>
      {extra}
    </Space>
  </div>
)
```

- Verification contribution: makes the File 1 import compile and its assertions pass; `tsc` verifies readonly props and `ReactNode` contract。
- After this file: PageHeader is a real but not-yet-exported shared component; PageTemplate wiring remains for File 4。

#### File 3 — `MODIFY egon-cola-platforms/egon-cola-platform-admin-web-shared/src/components/PageState.tsx`

- Purpose: 统一 PageState 的可见状态和 permission/retry 文案，同时保留现有 `showPartial` 行为。
- Symbols: `PageStateProps`；`PageState` error branch and action rendering。
- Repository evidence: current file already classifies `ApiError` and renders Skeleton/Alert/Empty；`classifyApiError` is the shared error boundary。
- Dependencies and consumers: `PageTemplate` and all platform pages; imports remain `antd` and shared `classifyApiError`。
- Why now: PageTemplate must be able to place PageHeader outside the content state without losing retry/partial semantics。
- Contract/signature changes: preserve all existing props; make Alert title/action use i18n-safe common copy; `showPartial=true` continues to render stale children plus banner。
- Input/output and state mapping: loading -> skeleton; error+partial -> banner+children; permission -> warning; empty -> Empty; success -> children; retry callback remains caller-owned。
- Error and edge behavior: 401/403 copy does not include resource detail; retry never fires automatically for 409/422; `children` stays mounted only in partial mode。
- Standards impact: `MC-REUSE-001, MC-SCOPE-001, MC-TEST-001` — reuse existing `classifyApiError`, AntD Alert/Button/Empty/Skeleton, and current public props。
- Literal rule enforcement: `Rule 11` — no Java file; preserve current shared package boundary.
- Implementation pseudocode:

```tsx
if (loading) return skeleton ?? <Skeleton active paragraph={{rows: 5}} />
if (error !== null && error !== undefined) {
  const classification = classifyApiError(error)
  const action = onRetry ? <Button size="small" onClick={onRetry}>重试</Button> : undefined
  const alert = <Alert type={classification.type === 'permission' ? 'warning' : 'error'} message={classification.title} action={action} />
  return showPartial ? <Space direction="vertical" style={{width: '100%'}}>{alert}{children}</Space> : alert
}
if (empty) return <Empty description={emptyDescription} />
return children
```

- Verification contribution: shared tests exercise each branch and verify retry action/focus-safe partial rendering。
- After this file: status semantics are compatible but copy/placement is ready for PageTemplate integration。

#### File 4 — `MODIFY egon-cola-platforms/egon-cola-platform-admin-web-shared/src/components/PageTemplate.tsx`

- Purpose: 将 PageHeader 接入现有模板，避免平台页面继续复制标题 Card 结构。
- Symbols: `PageTemplateProps` unchanged；`PageTemplate` render tree。
- Repository evidence: existing PageTemplate already owns `title/subtitle/breadcrumbs/extra/pageState/children`; no callers need a new prop。
- Dependencies and consumers: imports local `PageHeader` and `PageState`; all current platform pages that already use PageTemplate remain source-compatible。
- Why now: production wiring follows the RED test and PageHeader contract；this is the only shared layout composition change。
- Contract/signature changes: keep `BreadcrumbItem` export and `PageTemplateProps`; render PageHeader before the content Card or use it as the Card title region without removing `extra`。
- Input/output and state mapping: title/subtitle/breadcrumb/extra -> PageHeader；pageState -> PageState; children remain content body；no URL/query changes。
- Error and edge behavior: old callers with no breadcrumbs/extra still render; pageState errors do not suppress PageHeader; no duplicate breadcrumb is rendered。
- Standards impact: `MC-REUSE-001, MC-SCOPE-001, MC-TEST-001` — preserve public props and existing PageState/AntD usage; no new data or state owner。
- Literal rule enforcement: `Rule 11` — existing shared frontend structure only。
- Implementation pseudocode:

```tsx
return (
  <div>
    <PageHeader title={title} subtitle={subtitle} breadcrumbs={breadcrumbs} extra={extra} />
    <Card>
      <PageState {...pageState}>{children}</PageState>
    </Card>
  </div>
)
```

- Verification contribution: shared typecheck proves old `PageTemplate` callers compile; PageHeader test plus existing layout tests prove title/action and navigation remain visible。
- After this file: all shared UI files in Step 1 are wired, but translation exports still need key parity and index export。

#### File 5 — `MODIFY egon-cola-platforms/egon-cola-platform-admin-web-shared/src/i18n/zh-CN.ts`

- Purpose: 为新增 shared header/state copy 添加中文 keys。
- Symbols: `zhCN.common` new page/partial/permission strings。
- Repository evidence: current `zhCN` is the source shape for `en-US.ts` and already contains common layout/error keys。
- Dependencies and consumers: `en-US.ts` mirrors its object type; PageHeader/PageState use the keys through existing i18n helper or stable fallback。
- Why now: text additions must be paired with the English resource in the same Step。
- Contract/signature changes: add only keys required by Step 1, such as page header action/partial/retry/permission copy; do not add business-specific menu strings。
- Input/output and state mapping: key -> localized label; no runtime state or network side effect。
- Error and edge behavior: absent optional key falls back to literal text; key names remain identical with `en-US.ts`。
- Standards impact: `MC-SCOPE-001, MC-TEST-001` — no dependency or state change; key shape is verified against the paired resource。
- Literal rule enforcement: `Rule 11` — frontend i18n file, no Java architecture impact。
- Implementation pseudocode:

```ts
export const zhCN = {
  common: {
    ...existingCommon,
    'page.header.actions': '页面操作',
    'page.partial': '部分数据加载失败',
    'page.permission': '当前账号没有访问权限',
  },
}
```

- Verification contribution: key-parity script/review ensures every added key exists in en-US with the same nested shape。
- After this file: Chinese resource contains all shared Step 1 keys; English mirror remains the next file。

#### File 6 — `MODIFY egon-cola-platforms/egon-cola-platform-admin-web-shared/src/i18n/en-US.ts`

- Purpose: 保持多语言资源结构与 zh-CN 一致。
- Symbols: `enUS.common` counterparts for File 5。
- Repository evidence: `enUS` is typed as `typeof zhCN`, so TypeScript already exposes shape parity。
- Dependencies and consumers: imported by existing `i18n/index.tsx`; no new package or locale loader。
- Why now: paired configuration/resource change must be completed before shared export validation。
- Contract/signature changes: exact same key set as `zhCN.common`; values are English equivalents, no extra key。
- Input/output and state mapping: resource key -> text only。
- Error and edge behavior: typecheck catches missing key; no fallback changes outside new shared copy。
- Standards impact: `MC-SCOPE-001, MC-TEST-001` — paired resource key structure is checked; no Java configuration applies。
- Literal rule enforcement: `Rule 11` — frontend resource file remains in shared package。
- Implementation pseudocode:

```ts
export const enUS: typeof zhCN = {
  common: {
    ...existingCommon,
    'page.header.actions': 'Page actions',
    'page.partial': 'Some data failed to load',
    'page.permission': 'You do not have permission to access this page',
  },
}
```

- Verification contribution: `npm run typecheck` proves the same key structure; shared test setup can initialize either locale without missing values。
- After this file: locale resources are structurally paired and ready for index export。

#### File 7 — `MODIFY egon-cola-platforms/egon-cola-platform-admin-web-shared/src/index.ts`

- Purpose: 将 PageHeader 公共 API 暴露给四个 Admin Web 和 Portal。
- Symbols: `export { PageHeader, type PageHeaderProps }`。
- Repository evidence: index already exports PageState/PageTemplate/layout types and is the package public entry。
- Dependencies and consumers: package build emits declarations consumed by package dependencies; no deep import should be added in platform pages。
- Why now: final wiring after implementation prevents consumers from depending on internal path。
- Contract/signature changes: add named exports without removing existing exports or changing package version in this Step。
- Input/output and state mapping: module export -> TypeScript import; no state/network behavior。
- Error and edge behavior: type declaration generation must include PageHeader; any unused/duplicate export fails typecheck。
- Standards impact: `MC-REUSE-001, MC-SCOPE-001, MC-TEST-001` — preserve package API compatibility and existing build/export convention。
- Literal rule enforcement: `Rule 11` — public export stays in shared frontend package, no Java layer。
- Implementation pseudocode:

```ts
export {PageHeader, type PageHeaderProps} from './components/PageHeader'
export {PageState, type PageStateProps} from './components/PageState'
export {PageTemplate, type PageTemplateProps, type BreadcrumbItem} from './components/PageTemplate'
```

- Verification contribution: shared `typecheck` and `build` verify declaration/export resolution; downstream package typechecks consume the public path。
- After this file: Step 1 call path is wired and can be committed。

- Validation working directory: `/Users/mario/SelfProject/Egon-COLA/egon-cola-platforms/egon-cola-platform-admin-web-shared`
- Verification command: `npm run test -- --run src/components/PageHeader.test.tsx src/layout/EnterpriseLayout.test.tsx && npm run typecheck && npm run lint`
- Expected result: focused PageHeader and existing EnterpriseLayout tests pass; typecheck/lint exit 0; no removed public export or i18n key-shape error。
- Failure returns to: File 1 for assertion/setup failure; File 2-4 for render/type failure; Files 5-7 for resource/export parity。
- Completion criteria: shared PageHeader is exported, PageTemplate remains source-compatible, PageState has explicit state coverage, and only Step 1 paths are changed。
- Rollback: path-limited revert of the seven Step 1 paths; no production API/database rollback required。
- Commit paths: `egon-cola-platforms/egon-cola-platform-admin-web-shared/src/components/PageHeader.test.tsx`, `egon-cola-platforms/egon-cola-platform-admin-web-shared/src/components/PageHeader.tsx`, `egon-cola-platforms/egon-cola-platform-admin-web-shared/src/components/PageState.tsx`, `egon-cola-platforms/egon-cola-platform-admin-web-shared/src/components/PageTemplate.tsx`, `egon-cola-platforms/egon-cola-platform-admin-web-shared/src/i18n/zh-CN.ts`, `egon-cola-platforms/egon-cola-platform-admin-web-shared/src/i18n/en-US.ts`, `egon-cola-platforms/egon-cola-platform-admin-web-shared/src/index.ts`
- Commit: `feat(admin-web-shared): standardize page header and states`

### Step 2 — 闭合 IDP 列表、Grant 与审计页面的前端消费边界

- Requirements: `REQ-002, REQ-003, REQ-004, REQ-007, REQ-008, REQ-009, REQ-013, REQ-014, REQ-016`
- Dependencies: `Step 1`
- Baseline state: IDP users/clients/resource servers directly expect unbounded arrays; Grant page only saves; audit page only accepts page/size; backend has Grant DELETE/batch and current list endpoints, while page/filter extensions remain in Spec §9.
- Observable outcome: IDP pages use URL/query/page normalization, consume all already-existing safe mutations, explicitly represent deferred read/detail capabilities, and never expose one-time secrets after the success dialog closes。
- End state: IDP Web compiles with old List and page-wrapper fixtures; no IDP Java/API endpoint is created or renamed。
- Test-first gate: `Required — page normalizer, user/resource server/Grant/audit focused tests are written before their page behavior changes; RED must identify array/page mismatch, missing action/path or false empty state.`
- Manual Checks: `MC-REUSE-001, MC-SCOPE-001, MC-TEST-001`
- Literal Rules: `Rule 11` — no Java file; all changes remain in the IDP Web package.
- Ordered files:

#### File 1 — `CREATE egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-admin-web/src/api/page.test.ts`

- Purpose: 固定旧数组与服务端分页 wrapper 的兼容归一化行为。
- Symbols: `normalizesArray`, `normalizesPage`, `keepsEmptyPage`。
- Repository evidence: IDP `types.ts` has `TenantPageVO`/`TenantMembershipPageVO`; user/client/resource server pages still request arrays。
- Dependencies and consumers: File 2 `normalizePage`; users/clients/resource servers and their tests。
- Why now: this is the compile-independent pure behavior RED gate before changing pages。
- Contract/signature changes: input `readonly T[] | PageEnvelope<T>` -> `{content, page, size, totalElements, totalPages}`; page index remains zero-based at API boundary。
- Input/output and state mapping: array length becomes totalElements and page=0; wrapper fields preserve server values; missing optional array fields normalize to `[]` only when contract says empty。
- Error and edge behavior: invalid wrapper or non-array content throws a typed local error; total cannot be silently inferred from a failed response; empty page remains a successful empty result。
- Standards impact: `MC-REUSE-001, MC-SCOPE-001, MC-TEST-001` — pure TypeScript helper uses no dependency and tests both compatible response forms。
- Literal rule enforcement: `Rule 11` — frontend API helper remains inside IDP package; no Java layer。
- Implementation pseudocode:

```ts
it('normalizes legacy array into first page', () => {
  expect(normalizePage([{subject: 'u-1'}])).toEqual({content: [{subject: 'u-1'}], page: 0, size: 1, totalElements: 1, totalPages: 1})
})

it('keeps server page metadata and empty content', () => {
  const result = normalizePage({content: [], page: 2, size: 20, totalElements: 40, totalPages: 2})
  expect(result.page).toBe(2)
  expect(result.totalElements).toBe(40)
  expect(result.content).toEqual([])
})
```

- Verification contribution: pure RED/GREEN test identifies the response-shape boundary without involving network or React。
- After this file: helper tests specify the response contract but fail until File 2 exists。

#### File 2 — `CREATE egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-admin-web/src/api/page.ts`

- Purpose: 提供 IDP 多个列表页面共享的响应归一化，不创建新的服务器 API。
- Symbols: `PageEnvelope<T>`；`IdentityPage<T>`；`normalizePage<T>`。
- Repository evidence: three IDP pages repeat direct `httpClient.request<T[]>`; existing Tenant page already uses page metadata。
- Dependencies and consumers: imports only TypeScript types; consumers are File 5, File 7, File 9 and their tests。
- Why now: supplies the minimum GREEN implementation for File 1 and prevents each page implementing different compatibility logic。
- Contract/signature changes: `normalizePage<T>(value: readonly T[] | PageEnvelope<T>): IdentityPage<T>`; no fetch or query side effect。
- Input/output and state mapping: array -> zero-based first page; wrapper -> field-preserving page; all `content` values remain typed T and row IDs remain strings。
- Error and edge behavior: reject null/non-object/invalid content; do not coerce server `totalElements` to a guessed count when wrapper is malformed。
- Standards impact: `MC-REUSE-001, MC-SCOPE-001, MC-TEST-001` — reuse TypeScript/JDK-independent language features; no utility library or state store。
- Literal rule enforcement: `Rule 11` — exact IDP Web feature/API package placement only。
- Implementation pseudocode:

```ts
export type PageEnvelope<T> = {content: readonly T[]; page: number; size: number; totalElements: number; totalPages: number}
export type IdentityPage<T> = PageEnvelope<T>

export const normalizePage = <T,>(value: readonly T[] | PageEnvelope<T>): IdentityPage<T> => {
  if (Array.isArray(value)) return {content: value, page: 0, size: value.length, totalElements: value.length, totalPages: value.length === 0 ? 0 : 1}
  if (!value || !Array.isArray(value.content)) throw new Error('IDP 分页响应格式无效')
  return value
}
```

- Verification contribution: File 1 becomes GREEN; callers can use one page shape while preserving legacy endpoint compatibility。
- After this file: shared IDP page helper compiles and passes pure tests; page consumers remain unchanged until later files。

#### File 3 — `MODIFY egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-admin-web/src/api/types.ts`

- Purpose: 补充前端 page/filter 类型并保持现有 DTO/VO 字段兼容。
- Symbols: page aliases for `IdentityUserVO`, `OAuthClientVO`, `ResourceServerVO`; `IdentityListFilter`/`AuditFilter`。
- Repository evidence: existing interfaces use readonly response fields and mutable request fields；`AuditPageVO` currently lacks filter fields。
- Dependencies and consumers: page components and `page.ts`; no backend type import。
- Why now: types must compile before page query signatures are changed。
- Contract/signature changes: aliases are TypeScript-only; do not add fields to server responses. Filter fields are optional and serialized only when non-empty。
- Input/output and state mapping: query string `page/size/query/status/from/to/actor/event/result/traceId` -> request; response page alias -> normalized content/metadata。
- Error and edge behavior: unknown status values remain strings and render as unknown; missing optional timestamps remain `-`; no secret field is added to shared/public types。
- Standards impact: `MC-SCOPE-001, MC-TEST-001` — explicit semantic TypeScript names and response field preservation; no new dependency。
- Literal rule enforcement: `Rule 11` — file remains in IDP Web API type boundary; no Java model is created。
- Implementation pseudocode:

```ts
export type IdentityUserPageVO = IdentityPage<IdentityUserVO>
export type OAuthClientPageVO = IdentityPage<OAuthClientVO>
export type ResourceServerPageVO = IdentityPage<ResourceServerVO>
export interface IdentityListFilter { readonly page: number; readonly size: number; readonly query?: string; readonly status?: string }
export interface AuditFilter { readonly page: number; readonly size: number; readonly actorSub?: string; readonly eventType?: string; readonly result?: string; readonly from?: string; readonly to?: string; readonly traceId?: string }
```

- Verification contribution: IDP typecheck rejects malformed query/response mappings before page implementation。
- After this file: all page/filter symbols compile and no server contract is claimed beyond Spec §9 candidate extensions。

#### File 4 — `CREATE egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-admin-web/src/features/users/UserListPage.test.tsx`

- Purpose: 锁定用户列表 URL/filter/page、empty/error、pending mutation 和一次性密码安全行为。
- Symbols: `rendersPagedUsersAndResetsPageOnFilter`; `keepsSecretOutOfHostUrlAfterCreate`。
- Repository evidence: existing `App.test.tsx` mocks `httpClient`; current UserListPage has create/edit/reset/revoke mutations and no focused page test。
- Dependencies and consumers: `UserListPage`, `normalizePage`, mocked `httpClient`, `MemoryRouter`/QueryClient。
- Why now: test is the RED behavior gate before File 5 changes query state.
- Contract/signature changes: requests include only non-empty filter/page fields; response can be array or page wrapper; create success opens one-time Modal and does not put secret in URL/global state。
- Input/output and state mapping: URL query -> submitted filter/page; response -> table rows/total; mutation pending -> disabled controls; 422/409 -> form retained。
- Error and edge behavior: empty content is contextual Empty, 403 is denied/warning, transient read error offers retry; one-time password is only rendered inside success Modal and removed on close。
- Standards impact: `MC-SCOPE-001, MC-TEST-001` — use existing query/auth mocks; security negative assertion covers `REQ-007` without adding a transport.
- Literal rule enforcement: `Rule 11` — test remains under IDP frontend feature package。
- Implementation pseudocode:

```tsx
it('renders server total and resets page when filter changes', async () => {
  mockRequest.mockResolvedValue({content: [{subject: 'alice'}], page: 1, size: 20, totalElements: 21, totalPages: 2})
  render(<UserListPage />, {wrapper: queryAndRouter('/users?page=2')})
  expect(await screen.findByText('alice')).toBeInTheDocument()
  await userEvent.type(screen.getByRole('textbox', {name: '用户'}), 'alice')
  await userEvent.click(screen.getByRole('button', {name: '查询'}))
  expect(window.location.search).toContain('page=0')
  expect(mockRequest).toHaveBeenCalledWith(expect.stringContaining('query=alice'))
})

it('does not put one-time password in URL', async () => {
  mockRequest.mockResolvedValue({subject: 'new', oneTimePassword: 'secret-once'})
  await createUserThroughForm()
  expect(screen.getByText('secret-once')).toBeInTheDocument()
  expect(window.location.href).not.toContain('secret-once')
})
```

- Verification contribution: fails against current unbounded query/shape and missing filter controls; after File 5 it proves page and sensitive-state behavior。
- After this file: user test is RED for expected implementation reasons, with network fixture/setup valid。

#### File 5 — `MODIFY egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-admin-web/src/features/users/UserListPage.tsx`

- Purpose: 将用户列表从无界数组改为 URL-backed server-page-compatible UI，并保留 existing CRUD。
- Symbols: `UserListPage` query/filter/page state, `usersQuery`, create/edit/reset/revoke mutations。
- Repository evidence: current file uses `useQuery`, `useMutation`, `PageState`, permission checks and `httpClient` paths。
- Dependencies and consumers: File 2/3 normalizer/types; AuthContext permissions; QueryClient invalidation; AdminLayout route `/users`。
- Why now: makes File 4 GREEN and is the minimum IDP list implementation。
- Contract/signature changes: query key includes `page`, `size`, trimmed filters; request path preserves `/api/v1/identity/users`; mutations keep current paths and add `expectedVersion`/existing request fields only。
- Input/output and state mapping: URL submitted filter -> request query; array/page response -> normalized Page; current page total -> AntD Table pagination; mutation success -> exact `['idp','users',filters]` invalidation。
- Error and edge behavior: 401/403 through existing client; 422 maps fields; 409 leaves edit form and shows conflict; no automatic mutation retry; reset returns page 0; action buttons use `has` permission and loading state。
- Standards impact: `MC-REUSE-001, MC-SCOPE-001, MC-TEST-001` — reuse existing client/query/PageState/permission; no new store or package。
- Literal rule enforcement: `Rule 11` — feature file remains in IDP Web package; Java rules are not applicable。
- Implementation pseudocode:

```tsx
const [searchParams, setSearchParams] = useSearchParams()
const submitted = readUserFilter(searchParams)
const usersQuery = useQuery({
  queryKey: ['idp', 'users', submitted],
  queryFn: () => httpClient.request<IdentityUserVO[] | IdentityUserPageVO>(`/api/v1/identity/users?${serialize(submitted)}`).then(normalizePage),
})
const submit = (values: UserFilterForm) => setSearchParams(serializeParams({...values, page: 0, size: values.size ?? 20}))
const onMutationSuccess = async () => { await queryClient.invalidateQueries({queryKey: ['idp', 'users']}); closeForm() }
return <PageState loading={usersQuery.isPending} error={usersQuery.error} empty={usersQuery.data?.content.length === 0} onRetry={() => void usersQuery.refetch()}>{renderPagedTable(usersQuery.data, submit, guardedMutations)}</PageState>
```

- Verification contribution: File 4 becomes GREEN; App test observes route/query contract and existing create/reset/revoke behavior。
- After this file: user list is URL/page aware and secret-safe; Client/Resource Server/Grant/Audit remain for later files。

#### File 6 — `MODIFY egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-admin-web/src/features/clients/ClientListPage.test.tsx`

- Purpose: 扩展现有 Client 测试，覆盖分页、过滤、详情/Grant 入口和 secret lifecycle。
- Symbols: existing tests plus `rendersClientPageAndPreservesSecretSafety`, `invalidatesClientListAfterMutation`。
- Repository evidence: current test already asserts one-time secret not in `window.location.href`; current page has create/update/rotate/redirect/resource mutations。
- Dependencies and consumers: File 7 page, File 2 normalizer, mocked `httpClient` and QueryClient。
- Why now: existing focused test is the RED gate for client UI changes without creating duplicate test infrastructure。
- Contract/signature changes: request assertions accept `page/size/query/status` only when submitted; mutation paths stay existing。
- Input/output and state mapping: client page wrapper -> table total; row -> drawer; rotate/create result -> one-time modal; successful mutation -> client list/detail query invalidation。
- Error and edge behavior: secret only once; 409 preserves drawer form; 422 field errors; no raw client secret in React Query key or Portal bridge。
- Standards impact: `MC-SCOPE-001, MC-TEST-001` — extend existing RTL tests and retain negative credential assertion。
- Literal rule enforcement: `Rule 11` — test under IDP Web package。
- Implementation pseudocode:

```tsx
it('renders client page metadata and opens a safe detail drawer', async () => {
  mockRequest.mockResolvedValue(pageOf([{clientId: 'client-1', clientName: 'Orders', version: 3}]))
  render(<ClientListPage />, {wrapper: queryAndRouter('/clients?page=0')})
  expect(await screen.findByText('Orders')).toBeInTheDocument()
  await userEvent.click(screen.getByText('Orders'))
  expect(screen.getByRole('dialog')).toHaveTextContent('client-1')
  expect(screen.queryByText('raw-secret')).not.toBeInTheDocument()
})

it('invalidates the exact client list after update', async () => {
  await updateClient('client-1')
  expect(queryClient.invalidateQueries).toHaveBeenCalledWith(expect.objectContaining({queryKey: ['idp', 'clients']}))
})
```

- Verification contribution: identifies direct-array and invalidation regressions before File 7。
- After this file: existing Client tests include the new RED assertions and preserve old secret regression。

#### File 7 — `MODIFY egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-admin-web/src/features/clients/ClientListPage.tsx`

- Purpose: 实现 OAuth Client 的分页/过滤/详情 drawer 和已有 secret/Grant 入口闭环。
- Symbols: `ClientListPage` query, filter form, detail drawer, mutation callbacks。
- Repository evidence: current page already owns all client mutations and uses `useNavigate` to Grant route。
- Dependencies and consumers: File 2/3; existing `/api/v1/identity/clients` and mutation endpoints; `AdminLayout` permissions。
- Why now: File 6 fixes expected client behavior；implementation stays in existing feature.
- Contract/signature changes: list query can consume legacy array or page; detail/secret mutation signatures unchanged; URL stores submitted filters only。
- Input/output and state mapping: client row -> detail tabs; `version` -> expected version; `secretHint` display-safe; `clientSecret` only one-time modal state; Query invalidation scoped to client/list。
- Error and edge behavior: no automatic retries for writes; `Cache-Control` remains server-owned; forbidden actions hidden/disabled; no secret in URL/query/cache key/host bridge。
- Standards impact: `MC-REUSE-001, MC-SCOPE-001, MC-TEST-001` — reuse current client, Query, AntD Drawer/Form and permission hook。
- Literal rule enforcement: `Rule 11` — no Java file or new cross-platform layer。
- Implementation pseudocode:

```tsx
const clientsQuery = useQuery({queryKey: ['idp', 'clients', submitted], queryFn: () => requestClients(submitted).then(normalizePage)})
const detail = selectedClient ? <Drawer open onClose={closeDetail}><Descriptions items={safeClientFields(selectedClient)} /><Button onClick={() => navigate(`/clients/${selectedClient.clientId}/resource-grants`)}>Resource Grant</Button></Drawer> : null
const mutationOptions = {onSuccess: async () => { await queryClient.invalidateQueries({queryKey: ['idp', 'clients']}); await queryClient.invalidateQueries({queryKey: ['idp', 'client', selectedId]}) }}
if (clientsQuery.isError) return <PageState error={clientsQuery.error} onRetry={() => void clientsQuery.refetch()}>{null}</PageState>
return <PagedClientTable page={clientsQuery.data} onFilter={resetToFirstPage} onRowClick={setSelectedClient} mutations={permissionGuardedMutations(mutationOptions)} />
```

- Verification contribution: File 6 becomes GREEN and App test confirms Client route/Grant navigation。
- After this file: Client page is page-aware and secret-safe; Resource Server follows。

#### File 8 — `CREATE egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-admin-web/src/features/resource-servers/ResourceServerListPage.test.tsx`

- Purpose: 覆盖 Resource Server page/detail/batch/status 交互。
- Symbols: `rendersPageTotalAndDetail`, `submitsBatchActionOnlyForAuthorizedRows`。
- Repository evidence: current ResourceServerListPage has list/detail/create/enable/disable/batch-capable backend paths but no focused test。
- Dependencies and consumers: File 9 page, existing AuthContext/httpClient, QueryClient。
- Why now: test-first gate for replacing the direct array table。
- Contract/signature changes: list can be array/page; batch request preserves existing `expectedVersions` map and action values。
- Input/output and state mapping: filter/page -> request; selected rows -> batch command; detail response -> Drawer; mutation -> exact list invalidation。
- Error and edge behavior: version conflict retains selected state; disabled action shows permission denial; unknown status renders text not green success。
- Standards impact: `MC-SCOPE-001, MC-TEST-001` — no new dependency; test uses current page setup。
- Literal rule enforcement: `Rule 11` — IDP Web feature test only。
- Implementation pseudocode:

```tsx
it('renders server total and lazy detail', async () => {
  mockRequest.mockResolvedValue(pageOf([{resourceServerId: 'orders', displayName: 'Orders API', version: 4, status: 'ACTIVE'}]))
  render(<ResourceServerListPage />, {wrapper: queryAndRouter('/resource-servers?page=0')})
  expect(await screen.findByText('Orders API')).toBeInTheDocument()
  await userEvent.click(screen.getByRole('button', {name: '查看详情'}))
  expect(await screen.findByRole('dialog')).toHaveTextContent('orders')
})

it('keeps the old row state after a conflict', async () => {
  mockRequest.mockRejectedValueOnce(apiError(409))
  await userEvent.click(screen.getByRole('button', {name: '禁用'}))
  expect(screen.getByText(/版本冲突/)).toBeInTheDocument()
})
```

- Verification contribution: catches missing detail/batch and false-success states before File 9。
- After this file: Resource Server focused test is RED only for page/detail behavior。

#### File 9 — `MODIFY egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-admin-web/src/features/resource-servers/ResourceServerListPage.tsx`

- Purpose: 让 Resource Server 页面可规模化查询，并接入已有 detail/batch/status 能力。
- Symbols: list query, filter bar, detail Drawer, batch action bar, status mutation。
- Repository evidence: backend `ResourceServerController` already has list/detail/create/enable/disable/batch; current page consumes only list/basic actions。
- Dependencies and consumers: File 2/3; existing permissions and `httpClient`; route `/resource-servers`。
- Why now: File 8 defines expected page behavior。
- Contract/signature changes: preserve `/api/v1/identity/resource-servers` and existing mutation paths; only append submitted query filters/page when backend extension is available; legacy response remains accepted。
- Input/output and state mapping: page/filter -> normalized table; selected resource -> detail; version -> expectedVersion; batch rows -> explicit IDs/versions; success -> current list invalidation。
- Error and edge behavior: 403 hides batch; 409 leaves selection and displays conflict; 5xx retry query only; status changes never optimistically mark success before server response。
- Standards impact: `MC-REUSE-001, MC-SCOPE-001, MC-TEST-001` — reuse existing API/auth/Query/AntD components。
- Literal rule enforcement: `Rule 11` — no Java or cross-platform service layer。
- Implementation pseudocode:

```tsx
const query = useQuery({queryKey: ['idp', 'resource-servers', submitted], queryFn: () => httpClient.request<ResourceServerVO[] | ResourceServerPageVO>(buildPath(submitted)).then(normalizePage)})
const batch = useMutation({mutationFn: (command: BatchResourceServerActionDTO) => httpClient.request('/api/v1/identity/resource-servers/actions/batch', {method: 'POST', body: JSON.stringify(command)}), onSuccess: () => queryClient.invalidateQueries({queryKey: ['idp', 'resource-servers']})})
const openDetail = (row: ResourceServerVO) => setSelected(row)
return <PageState loading={query.isPending} error={query.error} empty={!query.data?.content.length} onRetry={() => void query.refetch()}><ResourceServerTable page={query.data} onDetail={openDetail} onBatch={canBatch ? batch.mutate : undefined} /></PageState>
```

- Verification contribution: File 8 becomes GREEN and App test sees normalized row/permission behavior。
- After this file: Resource Server page is complete against existing endpoints; Grant page is next。

#### File 10 — `CREATE egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-admin-web/src/features/resource-grants/ClientResourceGrantPage.test.tsx`

- Purpose: 固定 Grant 当前读取缺口与已有 PUT/DELETE/batch 能力的安全 UI 语义。
- Symbols: `rendersGrantReadUnavailableInsteadOfFalseEmpty`, `deletesGrantWithConfirmation`, `keepsSelectionOnConflict`。
- Repository evidence: current page calls Resource Server list and PUT only; backend controller also has DELETE and batch, but no list/get Grant endpoint。
- Dependencies and consumers: File 11 page; existing `httpClient`, permission and QueryClient。
- Why now: exposes the true §9 candidate without claiming backend availability。
- Contract/signature changes: when read endpoint is configured and returns data, render status; when 404/unsupported, render explicit “读取接口待补齐” state while existing write buttons remain available only with current version input。
- Input/output and state mapping: clientId path -> resource list/Grant actions; resource/version -> mutation body; 404 -> capability state, not empty array。
- Error and edge behavior: DELETE requires confirmation; 409 preserves form/selection; 403 disables write; no automatic retry of mutation。
- Standards impact: `MC-SCOPE-001, MC-TEST-001` — security-negative tests prevent missing API from becoming a false business fact。
- Literal rule enforcement: `Rule 11` — feature-local frontend test only。
- Implementation pseudocode:

```tsx
it('shows read capability unavailable rather than an empty grant list', async () => {
  mockRequest.mockRejectedValueOnce(apiError(404, 'GRANT_READ_NOT_AVAILABLE'))
  render(<ClientResourceGrantPage />, {wrapper: queryAndRouter('/clients/client-1/resource-grants')})
  expect(await screen.findByText(/读取接口待补齐/)).toBeInTheDocument()
  expect(screen.queryByText('暂无授权')).not.toBeInTheDocument()
})

it('confirms delete and retains selection on 409', async () => {
  await deleteGrantWithConfirm()
  expect(mockRequest).toHaveBeenCalledWith(expect.stringContaining('/resources/'), expect.objectContaining({method: 'DELETE'}))
  mockRequest.mockRejectedValueOnce(apiError(409))
  expect(await screen.findByText(/版本冲突/)).toBeInTheDocument()
})
```

- Verification contribution: distinguishes missing backend capability from successful empty data and covers existing mutation paths。
- After this file: Grant test defines safe behavior; page implementation remains。

#### File 11 — `MODIFY egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-admin-web/src/features/resource-grants/ClientResourceGrantPage.tsx`

- Purpose: 消费已有 Grant mutation 能力，并对候选读取接口做明确能力状态处理。
- Symbols: resource server query, optional grant query, upsert/delete/batch mutations, `PageState` branch。
- Repository evidence: current file already has `upsertMutation`, Resource Server query, expected resource/grant version fields; backend controller paths are known。
- Dependencies and consumers: File 10 test; IDP AuthContext/permission; Client detail navigation。
- Why now: implements only the behavior proven by File 10 and does not add a backend alias。
- Contract/signature changes: keep current PUT body semantics; add DELETE/batch body only from existing DTO fields; read endpoint URL stays a deferred configuration boundary and 404 is typed UI state。
- Input/output and state mapping: Resource Server row -> selected resource; form -> `UpsertClientResourceGrantDTO`; delete -> `DeleteClientResourceGrantDTO`; batch -> `BatchClientResourceGrantDTO`; success -> exact resource/grant invalidation。
- Error and edge behavior: no empty success on 404; modal close clears local form only after user cancel; mutation pending disables repeated submit; 409 keeps selection and version comparison。
- Standards impact: `MC-REUSE-001, MC-SCOPE-001, MC-TEST-001` — reuse existing client/query/permission and current UI primitives。
- Literal rule enforcement: `Rule 11` — IDP feature package only。
- Implementation pseudocode:

```tsx
const grantRead = useQuery({queryKey: ['idp', 'grants', clientId], queryFn: () => httpClient.request<ClientResourceGrantVO[]>(grantPath(clientId!))})
const readState = grantRead.error && isCapabilityMissing(grantRead.error) ? 'UNAVAILABLE' : grantRead.isPending ? 'LOADING' : 'READY'
const remove = useMutation({mutationFn: (input: DeleteClientResourceGrantDTO) => requestDelete(clientId!, selectedRs!.resourceServerId, input), onSuccess: () => invalidateGrantQueries(clientId!)})
return readState === 'UNAVAILABLE' ? <CapabilityState title="Grant 读取接口待补齐" action={<GrantWriteDrawer ... />} /> : <GrantMatrix data={grantRead.data} onDelete={remove.mutate} onUpsert={upsertMutation.mutate} />
```

- Verification contribution: File 10 becomes GREEN; existing PUT request and new existing DELETE/batch paths are observable。
- After this file: Grant UI is honest about backend gap and ready for later API contract without page redesign。

#### File 12 — `CREATE egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-admin-web/src/features/audits/AuditLogPage.test.tsx`

- Purpose: 固定安全审计 FilterBar、分页和详情脱敏边界。
- Symbols: `serializesAuditFiltersAndResetsPage`, `rendersForbiddenAndRetry`。
- Repository evidence: current page only uses `page/size`; `AuditPageVO` has content/totalElements; Spec §9 identifies filter extension candidate。
- Dependencies and consumers: File 13 page, existing `httpClient`/PageState。
- Why now: RED tests prevent adding visual filters without state/query assertions。
- Contract/signature changes: extra filters are serialized only when submitted; current page/size path remains compatible；detail is optional/deferred。
- Input/output and state mapping: time/operator/event/result/trace -> query string; page reset to 0 on submit; page total -> Table。
- Error and edge behavior: 403 displays permission state; 5xx provides retry; empty list is valid only when server returned a successful page wrapper; no raw audit payload in table。
- Standards impact: `MC-SCOPE-001, MC-TEST-001` — tests cover safe audit UI and no new API contract。
- Literal rule enforcement: `Rule 11` — IDP frontend test only。
- Implementation pseudocode:

```tsx
it('serializes submitted filters and resets to first page', async () => {
  render(<AuditLogPage />, {wrapper: queryAndRouter('/audits?page=3')})
  await userEvent.type(screen.getByRole('textbox', {name: '操作者'}), 'alice')
  await userEvent.click(screen.getByRole('button', {name: '查询'}))
  expect(mockRequest).toHaveBeenCalledWith(expect.stringMatching(/actorSub=alice.*page=0/))
})

it('separates forbidden from successful empty', async () => {
  mockRequest.mockRejectedValueOnce(apiError(403))
  render(<AuditLogPage />, {wrapper: queryAndRouter('/audits')})
  expect(await screen.findByText(/无权访问/)).toBeInTheDocument()
})
```

- Verification contribution: establishes audit filter behavior before File 13。
- After this file: audit test is RED for the current page-only UI and ready for implementation。

#### File 13 — `MODIFY egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-admin-web/src/features/audits/AuditLogPage.tsx`

- Purpose: 将审计页改为 URL/filter/page 状态并保持当前后端合同兼容。
- Symbols: `AuditLogPage` filter state/query/table/detail boundary。
- Repository evidence: current page already uses Query/PageState/Table and `AuditPageVO`; backend only guarantees page/size today。
- Dependencies and consumers: File 12 audit test, IDP `httpClient`, `AuditPageVO`, shared `PageTemplate`/`PageState`, and App route `/audits`。
- Why now: implements File 12 while keeping filter extension a backend-owned candidate。
- Contract/signature changes: query serializes optional filter keys; response accepts current `AuditPageVO`; export/detail controls render deferred capability state if no endpoint。
- Input/output and state mapping: URL submitted filter -> request; content/total -> table; row click -> safe detail summary; no operationContent/raw payload loaded by current page。
- Error and edge behavior: 403/5xx distinct; over-wide range uses server error text; page reset on filter; retry is query-only。
- Standards impact: `MC-REUSE-001, MC-SCOPE-001, MC-TEST-001` — reuse current httpClient/PageState/AntD and preserve platform API ownership。
- Literal rule enforcement: `Rule 11` — existing IDP Web feature boundary。
- Implementation pseudocode:

```tsx
const [params, setParams] = useSearchParams()
const filters = readAuditFilters(params)
const query = useQuery({queryKey: ['idp', 'audits', filters], queryFn: () => httpClient.request<AuditPageVO>(`/api/v1/identity/audits?${serializeAudit(filters)}`)})
const submit = (value: AuditFilterForm) => setParams(toSearchParams({...value, page: 0, size: 20}))
return <PageTemplate title="安全审计" extra={<Button onClick={() => void query.refetch()}>刷新</Button>} pageState={{loading: query.isPending, error: query.error, empty: query.data?.content.length === 0, onRetry: () => void query.refetch()}}><AuditFilterBar value={filters} onSubmit={submit} /><AuditTable rows={query.data?.content ?? []} total={query.data?.totalElements ?? 0} /></PageTemplate>
```

- Verification contribution: File 12 becomes GREEN; App test sees corrected query fixture and no false success on missing detail/export。
- After this file: IDP audit UI is filter-ready and backend gap remains explicit in §9。

#### File 14 — `MODIFY egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-admin-web/src/app/App.test.tsx`

- Purpose: 更新 IDP application-level fixtures and cross-route assertions after list response normalization。
- Symbols: existing `beforeEach` mock routes and `it.each` page cases。
- Repository evidence: current test enumerates `/users`, `/clients`, `/tenants`, `/keys`, `/audits` and exact request paths。
- Dependencies and consumers: Files 5/7/9/11/13; App Router and AuthContext mock。
- Why now: final wiring/regression after each page change。
- Contract/signature changes: fixtures support array/page wrapper; assertions accept exact query serialization and assert Grant mutation paths without changing backend.
- Input/output and state mapping: route -> expected request/result; permission list -> visible menu/action; page wrapper -> expected row text/total。
- Error and edge behavior: unexpected path still fails test; secret negative assertions remain; unauthorized navigation remains hidden。
- Standards impact: `MC-SCOPE-001, MC-TEST-001` — regression fixture only, no new dependency or public contract。
- Literal rule enforcement: `Rule 11` — application test remains under IDP Web package。
- Implementation pseudocode:

```tsx
it.each([
  ['/users?page=0&size=20', 'alice'],
  ['/clients?page=0&size=20', 'IdP Admin Web'],
  ['/resource-servers?page=0&size=20', 'Orders API'],
  ['/audits?page=0&size=20', 'LOGIN_SUCCEEDED'],
])('renders %s with the expected safe request', async (route, text) => {
  window.history.replaceState({}, '', route)
  render(<App />)
  await waitFor(() => expect(screen.getByText(text)).toBeInTheDocument())
  expect(mockRequest).toHaveBeenCalledWith(expect.stringContaining(route.split('?')[0]))
})
```

- Verification contribution: closes the IDP route/API regression path and prevents fixture drift from hiding a wrong endpoint。
- After this file: all Step 2 files are wired and ready for focused/module validation。

- Validation working directory: `/Users/mario/SelfProject/Egon-COLA/egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-admin-web`
- Verification command: `npm run test -- --run src/api/page.test.ts src/features/users/UserListPage.test.tsx src/features/clients/ClientListPage.test.tsx src/features/resource-servers/ResourceServerListPage.test.tsx src/features/resource-grants/ClientResourceGrantPage.test.tsx src/features/audits/AuditLogPage.test.tsx src/app/App.test.tsx && npm run typecheck && npm run lint`
- Expected result: all named tests pass, request paths/filters are exact, no sensitive value enters URL, typecheck/lint exit 0。
- Failure returns to: Files 1-3 for response/type normalization; Files 4-13 for individual page behavior; File 14 for app fixture/path mismatch。
- Completion criteria: five IDP page areas have explicit states and query keys, existing DELETE/batch paths are consumed where present, deferred read contracts show capability state, and no backend file changed。
- Rollback: revert only the fourteen Step 2 paths; existing IDP server behavior and auth contract remain untouched。
- Commit paths: `egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-admin-web/src/api/page.test.ts`, `egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-admin-web/src/api/page.ts`, `egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-admin-web/src/api/types.ts`, `egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-admin-web/src/features/users/UserListPage.test.tsx`, `egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-admin-web/src/features/users/UserListPage.tsx`, `egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-admin-web/src/features/clients/ClientListPage.test.tsx`, `egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-admin-web/src/features/clients/ClientListPage.tsx`, `egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-admin-web/src/features/resource-servers/ResourceServerListPage.test.tsx`, `egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-admin-web/src/features/resource-servers/ResourceServerListPage.tsx`, `egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-admin-web/src/features/resource-grants/ClientResourceGrantPage.test.tsx`, `egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-admin-web/src/features/resource-grants/ClientResourceGrantPage.tsx`, `egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-admin-web/src/features/audits/AuditLogPage.test.tsx`, `egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-admin-web/src/features/audits/AuditLogPage.tsx`, `egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-admin-web/src/app/App.test.tsx`
- Commit: `feat(idp-admin-web): close paged identity management flows`

### Step 3 — 修复 RBAC3 页面职责与真实 Admin API 路径

- Requirements: `REQ-002, REQ-003, REQ-004, REQ-008, REQ-010, REQ-013, REQ-014, REQ-016`
- Dependencies: `Step 1`
- Baseline state: `governance.routes.tsx` binds organizations and positions to `UserDirectoryPage`; `role.api.ts` omits `/iam` for roles/impact; `constraint.api.ts` omits `/iam/policies`; `directory.api.ts` uses `/directory/snapshots` instead of `/internal/directory-snapshots`。
- Observable outcome: each governance route renders its own page responsibility and all tested client paths match the actual Controller mappings; tenant/user IDs remain strings and conflict/permission states remain explicit。
- End state: RBAC3 Web route descriptor, API clients and tests agree; SDK/runtime snapshot ownership is unchanged。
- Test-first gate: `Required — route/path/component tests precede route/API/page changes; RED must identify the current wrong component or exact path mismatch.`
- Manual Checks: `MC-REUSE-001, MC-SCOPE-001, MC-TEST-001`
- Literal Rules: `Rule 11` — no Java file; preserve the RBAC3 Web package and SDK boundary.
- Ordered files:

#### File 1 — `CREATE egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin-web/src/features/directory/OrganizationPage.test.tsx`

- Purpose: 固定组织页面不再调用用户 lookup 的职责边界。
- Symbols: `rendersOrganizationScopeAndEmptyState`, `doesNotRequestUserDetail`。
- Repository evidence: current route points to `UserDirectoryPage`; backend OrganizationController exists; DirectoryPages test currently covers only user/snapshot。
- Dependencies and consumers: File 2 component, `FeatureApiProvider`, Rbac3 auth mock, route descriptor。
- Why now: RED test detects current wrong component before route modification。
- Contract/signature changes: organization page uses an explicit organization read path from existing Controller mapping; no client-supplied tenant authority beyond SDK tenant context。
- Input/output and state mapping: tenant from `useFeatureTenantContext` -> organization query; response -> tree/list; empty -> contextual CTA; 403 -> denied。
- Error and edge behavior: no `/directory/users/{id}` request; scope change invalidates organization query; mutations retain version/conflict state。
- Standards impact: `MC-REUSE-001, MC-SCOPE-001, MC-TEST-001` — reuse FeatureApi/Query/PageState and SDK authorization。
- Literal rule enforcement: `Rule 11` — feature test stays inside RBAC3 Web and does not create a Java layer。
- Implementation pseudocode:

```tsx
it('renders organization page and never uses user detail endpoint', async () => {
  api.request.mockResolvedValue({content: [{organizationId: 'org-1', name: '总部'}], page: 0, size: 20, totalElements: 1, totalPages: 1})
  render(<OrganizationPage />, {wrapper: rbacWrapper('/iam/organizations')})
  expect(await screen.findByText('总部')).toBeInTheDocument()
  expect(api.request).toHaveBeenCalledWith('/api/rbac3/v1/iam/organizations', expect.anything())
  expect(api.request).not.toHaveBeenCalledWith(expect.stringContaining('/directory/users/'), expect.anything())
})
```

- Verification contribution: fails with current route/component and fixes the ownership contract before File 2。
- After this file: focused organization test is RED for current code, with SDK/API wrapper valid。

#### File 2 — `CREATE egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin-web/src/features/directory/OrganizationPage.tsx`

- Purpose: 提供组织树/层级列表的独立页面职责。
- Symbols: `OrganizationPage`；organization query/filter/edit state。
- Repository evidence: OrganizationController is an existing Admin owner; `UserDirectoryPage` is a user-specific lookup and cannot be reused。
- Dependencies and consumers: File 1; `useFeatureApi`, `useFeatureTenantContext`, Rbac3 authorization and shared `PageState`; route File 3。
- Why now: creates the minimal component required by the corrected descriptor。
- Contract/signature changes: page accepts no userId prop; query uses tenant context and organization filters; mutation fields include existing expected version only when backend contract exposes it。
- Input/output and state mapping: organization response -> Tree/Table nodes; parent/child IDs -> lazy child loading; selected node -> detail drawer; query key includes tenant/filter。
- Error and edge behavior: empty tree has create CTA only with capability; 403 is denied; 409 retains local edit; no cross-tenant node is inferred from URL。
- Standards impact: `MC-REUSE-001, MC-SCOPE-001, MC-TEST-001` — reuse SDK tenant context/FeatureApi and shared state components。
- Literal rule enforcement: `Rule 11` — existing RBAC3 Web feature structure only。
- Implementation pseudocode:

```tsx
const tenant = useFeatureTenantContext().effectiveTenantId ?? 'none'
const query = useQuery({queryKey: ['rbac3', 'organizations', tenant, filter], queryFn: () => api.listOrganizations({filter}), enabled: status === 'READY'})
if (query.isPending) return <PageState loading error={null} empty={false}>{null}</PageState>
if (query.error) return <PageState loading={false} error={query.error} empty={false} onRetry={() => void query.refetch()}>{null}</PageState>
return <PageTemplate title="组织" subtitle="按组织层级维护目录事实" pageState={{loading: false, error: null, empty: query.data.length === 0}}><OrganizationTree nodes={query.data} onSelect={setSelected} /></PageTemplate>
```

- Verification contribution: File 1 becomes GREEN; route test can observe organization copy and request path。
- After this file: component exists but route still imports it only after File 3。

#### File 3 — `CREATE egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin-web/src/features/directory/PositionPage.test.tsx`

- Purpose: 固定岗位页面与用户目录查询分离，并验证组织筛选/权限状态。
- Symbols: `rendersPositionListWithOrganizationFilter`, `showsDeniedState`。
- Repository evidence: PositionController exists; current route incorrectly uses UserDirectoryPage。
- Dependencies and consumers: File 4 component, FeatureApi/auth mock, governance route。
- Why now: second RED ownership test before creating the component/route mapping。
- Contract/signature changes: position list request uses position endpoint/filter; no userId query or user detail response accepted as a position result。
- Input/output and state mapping: organization filter -> position query; position row -> detail/edit; empty/403 -> PageState。
- Error and edge behavior: stale scope clears filter; conflict retains edit; no unauthorized action rendered。
- Standards impact: `MC-REUSE-001, MC-SCOPE-001, MC-TEST-001` — reuse existing FeatureApi and test wrapper。
- Literal rule enforcement: `Rule 11` — RBAC3 Web feature test only。
- Implementation pseudocode:

```tsx
it('requests positions with selected organization filter', async () => {
  api.request.mockResolvedValue([{positionId: 'pos-1', name: '平台主管', organizationId: 'org-1'}])
  render(<PositionPage />, {wrapper: rbacWrapper('/iam/positions')})
  await userEvent.selectOptions(screen.getByRole('combobox', {name: '组织'}), 'org-1')
  expect(await screen.findByText('平台主管')).toBeInTheDocument()
  expect(api.request).toHaveBeenCalledWith('/api/rbac3/v1/iam/positions', expect.objectContaining({query: expect.objectContaining({organizationId: 'org-1'})}))
})
```

- Verification contribution: proves position ownership and filter mapping independently of organization page。
- After this file: position test is RED until File 4 implements the page。

#### File 4 — `CREATE egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin-web/src/features/directory/PositionPage.tsx`

- Purpose: 提供岗位列表、组织筛选和岗位详情/编辑入口。
- Symbols: `PositionPage`；position query/filter state。
- Repository evidence: PositionController and current FeatureApi abstraction；user lookup page is semantically wrong。
- Dependencies and consumers: File 3; route File 5; Query/SDK tenant context。
- Why now: creates the correct component for route descriptor。
- Contract/signature changes: no `initialUserId`; query takes organization filter/status and uses exact existing endpoint; mutation uses server version when present。
- Input/output and state mapping: organization selection -> URL/query key; row -> Drawer; version/status -> edit form; mutation -> position query invalidation。
- Error and edge behavior: no organization selected means no unbounded position query; 403/404/409 distinct; action capability gates write controls。
- Standards impact: `MC-REUSE-001, MC-SCOPE-001, MC-TEST-001` — reuse existing SDK/API/Query/AntD; no new global store。
- Literal rule enforcement: `Rule 11` — frontend feature placement remains current RBAC3 structure。
- Implementation pseudocode:

```tsx
const [organizationId, setOrganizationId] = useSearchParam('organizationId')
const query = useQuery({queryKey: ['rbac3', 'positions', tenant, organizationId], queryFn: () => api.positions({organizationId}), enabled: status === 'READY' && Boolean(organizationId)})
return <PageTemplate title="岗位" pageState={{loading: query.isPending, error: query.error, empty: query.data?.length === 0, onRetry: () => void query.refetch()}}><PositionFilter value={organizationId} onChange={setOrganizationId} /><PositionTable rows={query.data ?? []} canEdit={has('system:position:manage')} onEdit={openEditor} /></PageTemplate>
```

- Verification contribution: File 3 becomes GREEN and demonstrates no user-detail call。
- After this file: both new responsibilities exist and await route descriptor wiring。

#### File 5 — `MODIFY egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin-web/src/features/governance.routes.tsx`

- Purpose: 将组织/岗位路由从错误的用户页组件改为专属组件。
- Symbols: imports and `governanceRouteDescriptors` entries for `directory-organizations`/`directory-positions`。
- Repository evidence: current exact lines bind both to `UserDirectoryPage`; other descriptors use feature-local components。
- Dependencies and consumers: Files 2/4; `applicationRouteDescriptors` and visible navigation in `navigation.ts`。
- Why now: route wiring follows focused ownership tests/components。
- Contract/signature changes: paths/title/permission keys remain; only `component` and imports change; no route code rename。
- Input/output and state mapping: RBAC3 resource registry componentKey -> local component; permission -> menu/guard; deep link -> correct selected menu。
- Error and edge behavior: missing capability continues to hide route; direct path returns existing 403 from `RouteAccessGuard`; no fallback to user page。
- Standards impact: `MC-REUSE-001, MC-SCOPE-001, MC-TEST-001` — preserve existing registry/SDK navigation and route descriptor pattern。
- Literal rule enforcement: `Rule 11` — descriptor stays inside existing RBAC3 frontend feature root。
- Implementation pseudocode:

```tsx
import {OrganizationPage} from './directory/OrganizationPage'
import {PositionPage} from './directory/PositionPage'

export const governanceRouteDescriptors = [
  {...existingUserRoute, component: UserDirectoryPage},
  {...organizationRoute, path: '/iam/organizations', component: OrganizationPage},
  {...positionRoute, path: '/iam/positions', component: PositionPage},
] as const
```

- Verification contribution: directory tests and App.integration test observe exact component output and menu ownership。
- After this file: governance routes are correctly wired; API path fixes remain。

#### File 6 — `MODIFY egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin-web/src/features/role/role.api.ts`

- Purpose: 修复角色列表和影响分析调用路径。
- Symbols: `roleApi.roles`, `roleApi.impact`。
- Repository evidence: current file calls `/api/rbac3/v1/roles` and `/api/rbac3/v1/roles/{id}/impact-analysis`; backend RoleController base is `/api/rbac3/v1/iam/roles`。
- Dependencies and consumers: `RoleGraphPage`, RolePages test, FeatureApiClient；resources/replaceResources already use `/iam` and remain untouched。
- Why now: route ownership is fixed; exact API path is the next independent RED/GREEN contract。
- Contract/signature changes: list -> `/api/rbac3/v1/iam/roles`; impact -> `/api/rbac3/v1/iam/roles/{roleId}/impact-analysis`; roleId remains encoded string。
- Input/output and state mapping: applicationId query stays optional; RoleView/RoleImpactView types unchanged; server error maps through FeatureApi。
- Error and edge behavior: no fallback to old path; 404/403 visible in page state; role IDs never numeric-coerced。
- Standards impact: `MC-SCOPE-001, MC-TEST-001` — path-only client correction backed by exact Controller evidence and API test。
- Literal rule enforcement: `Rule 11` — existing RBAC3 frontend API boundary, no Java change。
- Implementation pseudocode:

```ts
roles: (applicationId?: string) => client.request<readonly RoleView[]>(
  '/api/rbac3/v1/iam/roles', {query: {applicationId}},
),
impact: (roleId: string) => client.request<RoleImpactView>(
  `/api/rbac3/v1/iam/roles/${encodeURIComponent(roleId)}/impact-analysis`,
),
resources: (roleId: string) => client.request<RoleResourceGrantTreeView>(
  `/api/rbac3/v1/iam/roles/${encodeURIComponent(roleId)}/resources`,
),
```

- Verification contribution: RolePages test observes exact paths and existing resource path remains green。
- After this file: role list/impact client path is corrected。

#### File 7 — `MODIFY egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin-web/src/features/constraint/constraint.api.ts`

- Purpose: 将约束读取路径与 `ConstraintController` base path 对齐。
- Symbols: `constraintApi.sodSets/dataRules/fieldRules/operationSodRules`。
- Repository evidence: current calls top-level endpoints; backend ConstraintController is under `/api/rbac3/v1/iam/policies`。
- Dependencies and consumers: ConstraintPage and its tests; FeatureApiClient.
- Why now: path contract is independent from page visual changes and should be green before tabs are polished。
- Contract/signature changes: each method uses `/api/rbac3/v1/iam/policies/...`; response view names unchanged。
- Input/output and state mapping: no caller values added; tenant context remains in FeatureApi client; arrays map to tabs/count labels。
- Error and edge behavior: one tab error does not erase successful other tab data; 403/5xx uses PageState; no retry on 409 because reads only。
- Standards impact: `MC-SCOPE-001, MC-TEST-001` — exact path mapping and typed response reuse。
- Literal rule enforcement: `Rule 11` — existing RBAC3 frontend API file only。
- Implementation pseudocode:

```ts
export const constraintApi = (client: FeatureApiClient) => ({
  sodSets: () => client.request<readonly SodSetView[]>('/api/rbac3/v1/iam/policies/sod-sets'),
  dataRules: () => client.request<readonly DataRuleView[]>('/api/rbac3/v1/iam/policies/data-rules'),
  fieldRules: () => client.request<readonly FieldRuleView[]>('/api/rbac3/v1/iam/policies/field-rules'),
  operationSodRules: () => client.request<readonly OperationSodRuleView[]>('/api/rbac3/v1/iam/policies/operation-sod-rules'),
})
```

- Verification contribution: ConstraintPage test can assert all exact paths and independent tab state。
- After this file: policy reads no longer rely on unowned top-level aliases。

#### File 8 — `MODIFY egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin-web/src/features/directory/directory.api.ts`

- Purpose: 修复目录快照提交路径并保留 trusted tenant/ISO payload boundary。
- Symbols: `directoryApi.submitSnapshot`。
- Repository evidence: current path `/api/rbac3/v1/directory/snapshots`; backend DirectoryController exposes `/api/rbac3/v1/internal/directory-snapshots`。
- Dependencies and consumers: OrgPositionSnapshotPage and DirectoryPages test; `FeatureApiClient`.
- Why now: exact path correction closes the static mismatch before page behavior tests。
- Contract/signature changes: only URL changes; `DirectorySnapshotCommand` fields stay providerCode/snapshotVersion/checksum/generatedAt/payload。
- Input/output and state mapping: form JSON -> parsed payload; generatedAt remains ISO string; response -> snapshotId/outcome/counts UI。
- Error and edge behavior: invalid JSON remains client validation; 401/403/409/422 pass through existing FeatureApi error mapping; no retry of snapshot submission after unknown result。
- Standards impact: `MC-SCOPE-001, MC-TEST-001` — preserve existing typed command and exact server path。
- Literal rule enforcement: `Rule 11` — RBAC3 frontend API boundary only。
- Implementation pseudocode:

```ts
submitSnapshot: (command: DirectorySnapshotCommand) => client.request<DirectorySyncView>(
  '/api/rbac3/v1/internal/directory-snapshots',
  {method: 'POST', body: command},
),
```

- Verification contribution: DirectoryPages test observes corrected request path and unchanged immutable snapshot copy。
- After this file: directory API static mismatch is removed。

#### File 9 — `MODIFY egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin-web/src/features/directory/DirectoryPages.test.tsx`

- Purpose: 汇总组织/岗位新页面、snapshot path 和现有 user string ID 回归。
- Symbols: existing directory tests plus route/component/path assertions。
- Repository evidence: current tests use Rbac3Provider/FeatureApiProvider wrapper and assert string IDs/snapshot text。
- Dependencies and consumers: Files 2/4/5/8; shared PageState and SDK auth。
- Why now: final directory regression after new page and API path implementation。
- Contract/signature changes: fixture adds organization/position response shapes; path assertion changes only to actual Controller mapping。
- Input/output and state mapping: route -> component text/request; user ID remains string; snapshot form -> internal path/response。
- Error and edge behavior: organization/position empty and denied states remain distinct; malformed snapshot JSON remains rejected before request。
- Standards impact: `MC-SCOPE-001, MC-TEST-001` — existing integration style extended, no new dependency。
- Literal rule enforcement: `Rule 11` — test stays in RBAC3 Web feature tree。
- Implementation pseudocode:

```tsx
it('renders organization and position responsibilities separately', async () => {
  render(<OrganizationPage />, {wrapper: rbacWrapper})
  expect(await screen.findByText('总部')).toBeInTheDocument()
  cleanup()
  render(<PositionPage />, {wrapper: rbacWrapper})
  expect(await screen.findByText('平台主管')).toBeInTheDocument()
})

it('submits immutable snapshot through internal controller path', async () => {
  await submitSnapshotForm()
  expect(api.request).toHaveBeenCalledWith('/api/rbac3/v1/internal/directory-snapshots', expect.objectContaining({method: 'POST'}))
})
```

- Verification contribution: provides focused GREEN evidence for directory ownership/path and keeps large numeric IDs as strings。
- After this file: directory slice is ready for commit once route/API tests pass。

#### File 10 — `MODIFY egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin-web/src/features/role/RolePages.test.tsx`

- Purpose: 关闭角色路径错配并验证 role/resource conflict state。
- Symbols: existing role page tests plus exact path assertions。
- Repository evidence: test already mocks FeatureApi requests ending `/resources` and tests role navigation/deep link。
- Dependencies and consumers: File 6 `role.api`, RoleGraphPage/RoleResourceGrantPage, SDK auth wrapper。
- Why now: role API correction needs a focused consumer proof。
- Contract/signature changes: expected calls change only to `/iam/roles`; resource endpoint remains unchanged。
- Input/output and state mapping: role list/impact -> cards/tags; roleId string -> encoded path; resource tree -> selected/expected version。
- Error and edge behavior: 403 route guard; 409 resource replacement keeps local selection; no N+1 beyond existing capped role impact behavior。
- Standards impact: `MC-SCOPE-001, MC-TEST-001` — static path assertions and existing query mocks。
- Literal rule enforcement: `Rule 11` — existing RBAC3 Web tests only。
- Implementation pseudocode:

```tsx
it('uses IAM role list and impact paths', async () => {
  render(<ApplicationRouter />, {wrapper: wrapper(['system:role:read'], '/iam/roles')})
  await waitFor(() => expect(screen.getByText('角色图谱')).toBeInTheDocument())
  expect(feature.request).toHaveBeenCalledWith('/api/rbac3/v1/iam/roles', expect.anything())
  expect(feature.request).toHaveBeenCalledWith('/api/rbac3/v1/iam/roles/301/impact-analysis', expect.anything())
})
```

- Verification contribution: ensures role list/impact consumer observes File 6 path changes and no old alias remains。
- After this file: role path regression is covered。

#### File 11 — `MODIFY egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin-web/src/features/constraint/ConstraintPage.test.tsx`

- Purpose: 验证策略 tabs 使用 `/iam/policies` 并独立呈现 partial/empty/error。
- Symbols: existing `validateDsdRoleSelection` tests plus query path/state tests。
- Repository evidence: current page uses four useQuery calls and displays data/field/operation explanatory text。
- Dependencies and consumers: File 7 `constraint.api`, ConstraintPage, FeatureApi wrapper。
- Why now: page test is the consumer proof for corrected API paths。
- Contract/signature changes: expected request paths only; no constraint business rule change。
- Input/output and state mapping: each query -> tab count/content; one error -> its tab state; tenant key remains in Query key。
- Error and edge behavior: no one failed tab hides other successful tabs; DSD validation helper remains unchanged and tested。
- Standards impact: `MC-SCOPE-001, MC-TEST-001` — no new dependency; existing validation helper and page states reused。
- Literal rule enforcement: `Rule 11` — frontend test boundary。
- Implementation pseudocode:

```tsx
it('requests all policy tabs under IAM policy root', async () => {
  render(<ConstraintPage />, {wrapper: rbacWrapper})
  await waitFor(() => expect(screen.getByText(/SSD|DSD/)).toBeInTheDocument())
  expect(api.request).toHaveBeenCalledWith('/api/rbac3/v1/iam/policies/sod-sets', expect.anything())
  expect(api.request).toHaveBeenCalledWith('/api/rbac3/v1/iam/policies/data-rules', expect.anything())
  expect(api.request).toHaveBeenCalledWith('/api/rbac3/v1/iam/policies/field-rules', expect.anything())
  expect(api.request).toHaveBeenCalledWith('/api/rbac3/v1/iam/policies/operation-sod-rules', expect.anything())
})
```

- Verification contribution: catches a path change that only typecheck could miss。
- After this file: constraint client/page contract is green-ready。

#### File 12 — `MODIFY egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin-web/src/app/App.integration.test.tsx`

- Purpose: 完成 RBAC3 route/permission/deep-link regression。
- Symbols: existing application router tests and new organization/position/route-path cases。
- Repository evidence: current integration test already checks unauthorized route, landing, hidden resources, selected navigation and no empty rail。
- Dependencies and consumers: Files 2/4/5/6/7/8/9/10/11; local resource registry and SDK mocks。
- Why now: final cross-feature gate after individual tests。
- Contract/signature changes: add route path/component text assertions; no change to SDK contract or backend authorization。
- Input/output and state mapping: about permissions/resource codes -> visible menu; deep link -> selected ancestor/403; API call -> page result。
- Error and edge behavior: user without route permission sees 403; no resource/menu report exposure; organization/position never render user lookup。
- Standards impact: `MC-REUSE-001, MC-SCOPE-001, MC-TEST-001` — use existing integration wrapper and resource registry as source of truth。
- Literal rule enforcement: `Rule 11` — application integration test remains in existing RBAC3 Web package。
- Implementation pseudocode:

```tsx
it('maps organization and position routes to their own pages', async () => {
  render(<ApplicationRouter />, {wrapper: wrapper(['system:organization:read'], '/iam/organizations')})
  await waitFor(() => expect(screen.getByText('组织')).toBeInTheDocument())
  cleanup()
  render(<ApplicationRouter />, {wrapper: wrapper(['system:position:read'], '/iam/positions')})
  await waitFor(() => expect(screen.getByText('岗位')).toBeInTheDocument())
  expect(screen.queryByText('用户 ID')).not.toBeInTheDocument()
})
```

- Verification contribution: proves route ownership, permission pruning and direct deep-link behavior together。
- After this file: RBAC3 Step is fully wired。

- Validation working directory: `/Users/mario/SelfProject/Egon-COLA/egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin-web`
- Verification command: `npm run test -- --run src/features/directory/OrganizationPage.test.tsx src/features/directory/PositionPage.test.tsx src/features/directory/DirectoryPages.test.tsx src/features/role/RolePages.test.tsx src/features/constraint/ConstraintPage.test.tsx src/app/App.integration.test.tsx && npm run typecheck && npm run verify:conformance && npm run verify:bundle`
- Expected result: all named route/component/path tests pass; conformance/bundle scripts exit 0; no old `/api/rbac3/v1/roles`, top-level policy path or `/directory/snapshots` call remains in affected files。
- Failure returns to: Files 1-5 for route component ownership; Files 6-8 for path mapping; Files 9-12 for consumer fixture/integration mismatch。
- Completion criteria: organizations/positions have dedicated page components; role/constraint/directory paths match verified Controller roots; existing SDK permission/tenant semantics remain unchanged。
- Rollback: revert only the twelve Step 3 paths; no RBAC3 Java/RPC/runtime snapshot change。
- Commit paths: `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin-web/src/features/directory/OrganizationPage.test.tsx`, `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin-web/src/features/directory/OrganizationPage.tsx`, `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin-web/src/features/directory/PositionPage.test.tsx`, `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin-web/src/features/directory/PositionPage.tsx`, `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin-web/src/features/governance.routes.tsx`, `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin-web/src/features/role/role.api.ts`, `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin-web/src/features/constraint/constraint.api.ts`, `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin-web/src/features/directory/directory.api.ts`, `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin-web/src/features/directory/DirectoryPages.test.tsx`, `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin-web/src/features/role/RolePages.test.tsx`, `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin-web/src/features/constraint/ConstraintPage.test.tsx`, `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin-web/src/app/App.integration.test.tsx`
- Commit: `fix(rbac3-admin-web): align governance pages and admin paths`

### Step 4 — 接入 Gateway 已存在的目录、发布和观测能力

- Requirements: `REQ-002, REQ-003, REQ-004, REQ-007, REQ-008, REQ-011, REQ-013, REQ-014, REQ-016`
- Dependencies: `Step 1`
- Baseline state: Gateway client already has release/draft diff, trace summary and audit list; OperationPage does not expose existing metadata/manual-definition/deprecate methods because the client lacks wrappers; Trace page has limited filters/detail; ReleaseDetailPage needs stronger unknown/partial UI assertions。
- Observable outcome: Gateway UI consumes existing Admin paths, distinguishes read/detail unavailability from empty data, preserves Release/Target evidence and never shows raw Body/credential/header。
- End state: Gateway Web typechecks/tests with no direct DDC/Redis/Kafka/Engine access and no new backend controller。
- Test-first gate: `Required — API/page tests are modified or created before client/page behavior; RED identifies missing wrapper, wrong query state or unsafe success rendering.`
- Manual Checks: `MC-REUSE-001, MC-SCOPE-001, MC-TEST-001`
- Literal Rules: `Rule 11` — no Java file; changes remain in Gateway Web package.
- Ordered files:

#### File 1 — `MODIFY egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web/src/api/types.ts`

- Purpose: 定义 Gateway 前端需要的安全详情/操作结果类型，不扩大敏感字段。
- Symbols: `TraceDetail`, `GatewayOperationMutationResult`, optional safe detail state types。
- Repository evidence: current `TraceSummary`, `OperationDetail`, `GatewayRelease`, `ValidationReport` already define API-owned fields and redaction conventions。
- Dependencies and consumers: Files 2-9; `gatewayApi.ts`, Trace/Operation/Release pages。
- Why now: client test and wrappers need explicit return types before production API methods。
- Contract/signature changes: new types represent existing/candidate response shapes only; raw request Body, Authorization, Cookie, Secret and sensitive Header are excluded by type design。
- Input/output and state mapping: traceId -> timeline/attempt summary; operationId -> metadata/manual definition mutation result; release -> structured diff/target status。
- Error and edge behavior: nullable detail fields stay absent/undefined; unknown status remains string and maps to neutral/unknown Tag; no default success enum。
- Standards impact: `MC-SCOPE-001, MC-TEST-001` — explicit frontend protocol types and negative sensitive-field assertions; no new library。
- Literal rule enforcement: `Rule 11` — Gateway Web API type boundary only。
- Implementation pseudocode:

```ts
export type TraceDetail = {
  traceId: string
  startedAt?: string
  finishedAt?: string
  attempts: readonly {attemptId: string; status: string; durationMs?: number; errorCode?: string}[]
  redactedAttributes: Readonly<Record<string, string | number | boolean | null>>
}
export type GatewayOperationMutationResult = {operationId: string; revision: number; status: string; traceId?: string}
```

- Verification contribution: API wrappers/pages can compile against explicit safe result shape。
- After this file: types are available; API methods are not yet wired。

#### File 2 — `MODIFY egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web/src/api/gatewayApi.test.ts`

- Purpose: 固定 Gateway API method/path/body/query and no-sensitive mapping。
- Symbols: existing gateway API tests plus `requestsOperationMetadata`, `requestsManualDefinition`, `deprecatesOperation`, `requestsTraceDetail`。
- Repository evidence: current API tests cover core methods; backend CatalogController/ReleaseController methods already exist per Spec evidence。
- Dependencies and consumers: File 3 client; OperationPage/Release/Traces consumers。
- Why now: RED contract before adding wrappers。
- Contract/signature changes: metadata/manual/deprecate use exact existing paths and idempotency/trace behavior; trace detail candidate must surface 404/unavailable, not synthesize detail。
- Input/output and state mapping: operationId + request -> API method; releaseId -> diff; traceId -> safe detail; API error -> typed/retryable result。
- Error and edge behavior: mutation methods do not retry automatically; 409/422 returned for page conflict UI; raw body never appears in mapped result。
- Standards impact: `MC-SCOPE-001, MC-TEST-001` — static method/path/body assertions and sensitive negative checks。
- Literal rule enforcement: `Rule 11` — Gateway Web API tests only。
- Implementation pseudocode:

```ts
it('maps existing catalog lifecycle methods to Admin API paths', async () => {
  server.expect('/api/v1/gateway/admin/operations/op-1/metadata', {method: 'PUT'}).respond({operationId: 'op-1', revision: 2})
  await gatewayApi.updateOperationMetadata('op-1', {summary: 'Orders'})
  expect(server.lastRequest.body).toEqual({summary: 'Orders'})
  expect(server.lastRequest.body).not.toHaveProperty('secret')
})

it('keeps trace detail candidate as unavailable on 404', async () => {
  server.expect('/api/v1/gateway/admin/observability/traces/t-1').respondStatus(404)
  await expect(gatewayApi.traceDetail('t-1')).rejects.toMatchObject({status: 404})
})
```

- Verification contribution: fails against current missing method exports and path wiring; becomes GREEN after File 3。
- After this file: API test contract is written before production wrappers。

#### File 3 — `MODIFY egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web/src/api/gatewayApi.ts`

- Purpose: 增加现有 Gateway Admin 能力的 typed wrappers，保持 domain API ownership。
- Symbols: `updateOperationMetadata`, `updateManualDefinition`, `deprecateOperation`, `releaseDiff`, `traceDetail`。
- Repository evidence: current `admin` prefix, `apiRequest`, `createLogicalTrace`, `newIdempotencyKey` and existing `release/draftDiff/traces` methods。
- Dependencies and consumers: File 2 tests; OperationPage, ReleaseDetailPage, TracesPage。
- Why now: minimum GREEN implementation after exact method/path tests。
- Contract/signature changes: use `/api/v1/gateway/admin/operations/{id}/metadata`, `/manual-definition`, `/deprecate`, existing `/releases/{id}/diff` and trace detail candidate; mutation bodies include only existing UI inputs and idempotency key generated by existing helper。
- Input/output and state mapping: server result -> typed result; 404 trace -> caller error state; release diff -> existing JsonPanel/structured diff; all requests include AbortSignal for reads。
- Error and edge behavior: no direct internal store calls; 401/403/409/422 remain `apiRequest` errors; trace detail does not load raw Body/Header; mutation retry remains false at page layer。
- Standards impact: `MC-REUSE-001, MC-SCOPE-001, MC-TEST-001` — reuse existing `apiRequest`, trace/idempotency and types; no second HTTP client。
- Literal rule enforcement: `Rule 11` — Gateway frontend API package only。
- Implementation pseudocode:

```ts
updateOperationMetadata: (operationId: string, body: {summary?: string; tags?: string[]}) => apiRequest<GatewayOperationMutationResult>(
  `${admin}/operations/${encodeURIComponent(operationId)}/metadata`, {method: 'PUT', body, trace: createLogicalTrace(), idempotencyKey: newIdempotencyKey()},
),
updateManualDefinition: (operationId: string, body: Record<string, unknown>) => apiRequest<GatewayOperationMutationResult>(
  `${admin}/operations/${encodeURIComponent(operationId)}/manual-definition`, {method: 'PUT', body, trace: createLogicalTrace(), idempotencyKey: newIdempotencyKey()},
),
deprecateOperation: (operationId: string, reason: string) => apiRequest<GatewayOperationMutationResult>(
  `${admin}/operations/${encodeURIComponent(operationId)}/deprecate`, {method: 'POST', body: {reason}, trace: createLogicalTrace(), idempotencyKey: newIdempotencyKey()},
),
traceDetail: (traceId: string, signal?: AbortSignal) => apiRequest<TraceDetail>(`${admin}/observability/traces/${encodeURIComponent(traceId)}`, {signal}),
```

- Verification contribution: File 2 becomes GREEN; pages now consume only Gateway Admin paths。
- After this file: client wrappers exist; page tests remain RED for presentation/state behavior。

#### File 4 — `MODIFY egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web/src/features/interface-catalog/OperationPage.test.tsx`

- Purpose: 固定 Operation lifecycle controls、权限和 source type read-only boundary。
- Symbols: existing OperationPage tests plus metadata/manual-definition/deprecate cases。
- Repository evidence: current OperationPage/SchemaPanel tests and Gateway API types; backend CatalogController methods already exist。
- Dependencies and consumers: File 3 API methods; OperationPage capabilities/QueryClient。
- Why now: API wrappers are typed but page must prove permissions and safe mutation state。
- Contract/signature changes: MANUAL source may edit; RPC_DESCRIPTOR/OPENAPI31 renders read-only; mutation result invalidates operation query and shows traceId if present。
- Input/output and state mapping: operation detail/sourceType -> controls; form -> metadata/manual definition body; 409 -> local form retained; success -> refetch。
- Error and edge behavior: 403 disables controls; 422 maps field message; deprecate requires reason/confirmation; no schema/raw credential output。
- Standards impact: `MC-SCOPE-001, MC-TEST-001` — RTL assertions for permission, conflict and sensitive-field absence。
- Literal rule enforcement: `Rule 11` — feature-local Gateway test。
- Implementation pseudocode:

```tsx
it('allows metadata edit only for manual operations', async () => {
  render(<OperationPage />, {wrapper: operationWrapper({sourceType: 'MANUAL'})})
  await userEvent.click(screen.getByRole('button', {name: '编辑元数据'}))
  await userEvent.click(screen.getByRole('button', {name: '保存'}))
  expect(gatewayApi.updateOperationMetadata).toHaveBeenCalledWith('op-1', expect.objectContaining({summary: expect.any(String)}))
})

it('keeps RPC definition read-only and never renders secret', async () => {
  render(<OperationPage />, {wrapper: operationWrapper({sourceType: 'RPC_DESCRIPTOR', secret: 'hidden'})})
  expect(screen.queryByRole('button', {name: '编辑定义'})).not.toBeInTheDocument()
  expect(screen.queryByText('hidden')).not.toBeInTheDocument()
})
```

- Verification contribution: catches source/permission and sensitive rendering bugs。
- After this file: Operation page tests are ready to turn GREEN after File 5。

#### File 5 — `MODIFY egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web/src/features/interface-catalog/OperationPage.tsx`

- Purpose: 接入 Operation metadata/manual definition/deprecate 页面动作。
- Symbols: `OperationPage` queries, mutation hooks, source-type guard, detail tabs。
- Repository evidence: current page renders operation/schema but API client lacks the existing CatalogController mutation methods。
- Dependencies and consumers: File 3 methods; File 4 tests; Gateway capability provider and QueryClient。
- Why now: implements tested UI after API wrappers。
- Contract/signature changes: no route change; add local form state and mutation calls; invalidate `['operation', operationId]` on success。
- Input/output and state mapping: OperationDetail -> tabs; MANUAL -> editable form; source type -> read-only state; mutation result revision/trace -> feedback。
- Error and edge behavior: pending disables duplicate; 409 preserves local form; 403/422 visible; deprecate Popconfirm requires reason; no raw definition secret/body。
- Standards impact: `MC-REUSE-001, MC-SCOPE-001, MC-TEST-001` — reuse `gatewayApi`, `useCapability`, `useQuery/useMutation`, JsonPanel; no global store。
- Literal rule enforcement: `Rule 11` — Gateway frontend feature boundary。
- Implementation pseudocode:

```tsx
const operation = useQuery({queryKey: ['operation', operationId], queryFn: ({signal}) => gatewayApi.operation(operationId!, signal)})
const metadata = useMutation({mutationFn: (body: OperationMetadataForm) => gatewayApi.updateOperationMetadata(operationId!, body), onSuccess: () => queryClient.invalidateQueries({queryKey: ['operation', operationId]})})
const writable = operation.data?.operation.sourceType === 'MANUAL' && canWrite
return <PageTemplate title={operation.data?.operation.operationKey ?? 'Operation'} pageState={pageState(operation)}><SourceBadge value={operation.data?.operation.sourceType} /><OperationTabs detail={operation.data} writable={writable} onMetadataSave={metadata.mutate} onManualDefinitionSave={manual.mutate} onDeprecate={deprecate.mutate} /></PageTemplate>
```

- Verification contribution: File 4 becomes GREEN and exact existing catalog methods are visible only under correct source/capability。
- After this file: Operation lifecycle UI is complete。

#### File 6 — `CREATE egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web/src/features/releases/ReleaseDetailPage.test.tsx`

- Purpose: 固定 Release retry/rollback/Diff/UNKNOWN/PARTIAL 语义。
- Symbols: `rendersStructuredDiffAndTargetTimeline`, `doesNotMarkPartialAsSuccess`, `requiresRollbackReason`。
- Repository evidence: ReleaseDetailPage already uses `release`, `retryRelease`, `rollback`, `structuredDiff`, target attempts and capability guards but has no focused test。
- Dependencies and consumers: File 7 page; current Gateway API/StatusTag/QueryClient mocks。
- Why now: RED test before presentation/recovery refinements。
- Contract/signature changes: retry/rollback calls retain current IDs/revisions/reason; result status controls visible copy/actions。
- Input/output and state mapping: release/attempt/target -> status/timeline/table; draft revision -> rollback command; error/unknown -> recovery panel。
- Error and edge behavior: partial/unknown never green; retry is pending-disabled; rollback requires reason and current draft; 409 retains dialog input。
- Standards impact: `MC-SCOPE-001, MC-TEST-001` — recovery tests assert no duplicate/false success and existing trace/idempotency calls。
- Literal rule enforcement: `Rule 11` — Gateway Web test only。
- Implementation pseudocode:

```tsx
it('renders partial release as a non-success state', async () => {
  mockRelease({status: 'PARTIAL', partialApplied: true, structuredDiff: {routes: []}})
  render(<ReleaseDetailPage />, {wrapper: releaseWrapper('/groups/g-1/releases/r-1')})
  expect(await screen.findByText(/部分生效/)).toBeInTheDocument()
  expect(screen.getByRole('button', {name: /重试/})).toBeEnabled()
  expect(screen.queryByText('发布成功')).not.toBeInTheDocument()
})

it('requires reason before rollback request', async () => {
  openRollbackModal()
  expect(screen.getByRole('button', {name: '创建回滚 Release'})).toBeDisabled()
})
```

- Verification contribution: isolates asynchronous recovery semantics from other Gateway pages。
- After this file: Release test is RED for missing assertions/state copy。

#### File 7 — `MODIFY egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web/src/features/releases/ReleaseDetailPage.tsx`

- Purpose: 完善 Release Diff/Target timeline/unknown/partial recovery UI。
- Symbols: `ReleaseDetailPage` release/draft/retry/rollback queries and controls。
- Repository evidence: current page already has all server calls and structured diff panels; changes are presentation/state guards only。
- Dependencies and consumers: File 6 test; existing `gatewayApi`, `StatusTag`, `shouldPollRelease`, capabilities。
- Why now: tests prove current gaps; no new API method needed for existing release fields。
- Contract/signature changes: keep release/retry/rollback signatures; add explicit `release.data.status` branches and stable query invalidation after rollback/retry。
- Input/output and state mapping: status `UNKNOWN/TIMEOUT/PARTIAL` -> warning/error/reconcile panel; attempts -> timeline/Table; structuredDiff -> safe JsonPanel; reason -> rollback command。
- Error and edge behavior: no auto retry for mutation; polling only while visible and non-terminal; retry preserves original release identity; rollback creates new release copy text。
- Standards impact: `MC-REUSE-001, MC-SCOPE-001, MC-TEST-001` — reuse current API/StatusTag/polling state and existing capability guard。
- Literal rule enforcement: `Rule 11` — Gateway Web feature file only。
- Implementation pseudocode:

```tsx
const release = useQuery({queryKey: ['release', releaseId], queryFn: ({signal}) => gatewayApi.release(releaseId!, signal), refetchInterval: ({state}) => visible && shouldPollRelease(state.data) ? 2000 : false})
const retry = useMutation({mutationFn: () => gatewayApi.retryRelease(releaseId!), onSuccess: () => queryClient.invalidateQueries({queryKey: ['release', releaseId]})})
const statusPanel = release.data?.partialApplied || ['UNKNOWN', 'TIMEOUT'].includes(release.data?.status ?? '') ? <RecoveryPanel status={release.data.status} onRetry={retry.mutate} /> : null
return <ReleaseLayout summary={release.data} statusPanel={statusPanel} diff={<JsonPanel value={release.data.structuredDiff} />} attempts={release.data.attempts} rollback={guardedRollback} />
```

- Verification contribution: File 6 becomes GREEN; existing Release API calls remain unchanged。
- After this file: release recovery copy/actions are explicit and safe。

#### File 8 — `MODIFY egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web/src/features/observability/TracesPage.test.tsx`

- Purpose: 固定 Trace filter/page/refresh/detail/de-sensitivity behavior。
- Symbols: existing trace tests plus filter/detail/error cases。
- Repository evidence: current TracesPage has env/namespace/traceId/protocol/status filters, 5s refresh and summary table; `gatewayApi.traces` exists。
- Dependencies and consumers: File 9 page, File 3 `traceDetail`, QueryClient/visibility test helpers。
- Why now: page test is RED before adding detail drawer/filter fields。
- Contract/signature changes: query keys include submitted filters/page; detail request is lazy by traceId; 404 -> explicit unavailable state。
- Input/output and state mapping: URL -> query; summary row -> detail query; visibility -> polling enabled; error -> PageState/QueryFailure。
- Error and edge behavior: no raw Body/Header/credential; stale data + background failure shows partial; page reset after filter; hidden page stops unnecessary polling。
- Standards impact: `MC-SCOPE-001, MC-TEST-001` — tests cover data boundary and sensitive-field negative assertions。
- Literal rule enforcement: `Rule 11` — Gateway Web test only。
- Implementation pseudocode:

```tsx
it('resets page and lazy-loads safe trace detail', async () => {
  mockTraceList(pageOf([{traceId: 't-1', statusCategory: 'SUCCESS'}]))
  render(<TracesPage />, {wrapper: traceWrapper('/observability/traces?env=prod&namespace=public&page=2')})
  await userEvent.click(await screen.findByText('t-1'))
  expect(gatewayApi.traceDetail).toHaveBeenCalledWith('t-1', expect.anything())
  expect(screen.queryByText(/Authorization|Cookie|raw body/i)).not.toBeInTheDocument()
})

it('keeps old rows when background refresh fails', async () => {
  mockNextRefreshError()
  expect(await screen.findByRole('alert')).toHaveTextContent(/刷新失败/)
  expect(screen.getByText('t-1')).toBeInTheDocument()
})
```

- Verification contribution: catches high-cardinality/filter/refresh/detail regressions。
- After this file: trace tests define the safe observer contract。

#### File 9 — `MODIFY egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web/src/features/observability/TracesPage.tsx`

- Purpose: 按 Spec 逐步补齐 Trace 过滤和安全详情边界。
- Symbols: `TracesPage` URL search state, list query, detail query/drawer, polling visibility。
- Repository evidence: current page uses `useSearchParams`, `useQuery`, `GatewayScopeFilter`, 5s interval and `TraceSummary` table。
- Dependencies and consumers: File 3 `traceDetail`; File 8 tests; existing `gatewayApi.traces`.
- Why now: implements tested observer behavior without adding backend detail if endpoint is absent。
- Contract/signature changes: include only existing/candidate filter names in URL; detail query is enabled on selected row; no raw payload type.
- Input/output and state mapping: scope/filter/page -> list query key; list row -> selectedTraceId; detail success -> redacted drawer; 404 -> capability unavailable; background error -> old rows + banner。
- Error and edge behavior: `refetchInterval` false when no scope/hidden/terminal; page reset after submit; copy only safe IDs; no automatic mutation (read-only)。
- Standards impact: `MC-REUSE-001, MC-SCOPE-001, MC-TEST-001` — reuse existing client/Query/StatusTag/GatewayScopeFilter and safe `sanitize` conventions。
- Literal rule enforcement: `Rule 11` — Gateway Web feature path only。
- Implementation pseudocode:

```tsx
const list = useQuery({queryKey: ['traces', requestScope, submitted.toString()], queryFn: ({signal}) => gatewayApi.traces(requestScope!, submitted, signal), enabled: Boolean(requestScope), refetchInterval: visible ? 5000 : false, refetchIntervalInBackground: false})
const detail = useQuery({queryKey: ['trace-detail', selectedTraceId], queryFn: ({signal}) => gatewayApi.traceDetail(selectedTraceId!, signal), enabled: selectedTraceId !== null})
const onFilter = (values: TraceFilterForm) => setSearchParams(toTraceParams({...values, page: 1}))
return <TraceTable rows={list.data?.items ?? []} onRowClick={setSelectedTraceId} partialError={list.isFetching && Boolean(list.data)} /><TraceDrawer detail={detail.data} unavailable={detail.error?.status === 404} />
```

- Verification contribution: File 8 becomes GREEN and existing trace list API remains the only data source。
- After this file: Trace page satisfies filters/refresh/safe detail boundary。

- Validation working directory: `/Users/mario/SelfProject/Egon-COLA/egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web`
- Verification command: `npm run test -- --run src/api/gatewayApi.test.ts src/features/interface-catalog/OperationPage.test.tsx src/features/releases/ReleaseDetailPage.test.tsx src/features/observability/TracesPage.test.tsx && npm run typecheck && npm run lint`
- Expected result: API paths/mutations, Operation source guards, Release recovery and Trace filters/detail safety pass; typecheck/lint exit 0。
- Failure returns to: Files 1-3 for API type/wrapper mismatch; Files 4-7 for Operation/Release behavior; Files 8-9 for Trace query/detail/polling behavior。
- Completion criteria: existing Gateway Catalog/Release capabilities are consumed, no direct internal store access is added, unknown/partial is never green, and Trace detail is safe/unavailable when backend candidate is absent。
- Rollback: revert only the nine Step 4 paths; Gateway backend and existing release/tracing contracts remain unchanged。
- Commit paths: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web/src/api/types.ts`, `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web/src/api/gatewayApi.test.ts`, `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web/src/api/gatewayApi.ts`, `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web/src/features/interface-catalog/OperationPage.test.tsx`, `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web/src/features/interface-catalog/OperationPage.tsx`, `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web/src/features/releases/ReleaseDetailPage.test.tsx`, `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web/src/features/releases/ReleaseDetailPage.tsx`, `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web/src/features/observability/TracesPage.test.tsx`, `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web/src/features/observability/TracesPage.tsx`
- Commit: `feat(gateway-admin-web): close catalog release and trace workflows`

### Step 5 — 补齐 DDC 已有实例分页入口并固化缺失接口状态

- Requirements: `REQ-002, REQ-003, REQ-004, REQ-008, REQ-012, REQ-013, REQ-014, REQ-016`
- Dependencies: `Step 1` and DDC predecessor pagination semantics。
- Baseline state: DDC uses `ddcPageApi`/`PageResultRecord` for registry/config/publish pages; backend has `/api/v1/ddc/instances/page`, but App/Layout has no instances route/menu; DDC audit/validate/diff APIs remain candidates in Spec §9。
- Observable outcome: `配置客户端实例` becomes an independent server-paged page; DDC existing polling/filter/empty/error behavior remains; absent audit API is represented as capability state, not successful empty data。
- End state: DDC Web keeps `/page` response and old List/RPC compatibility; no DDC Java/table/Flyway modification。
- Test-first gate: `Required — client/App/Layout/Instances tests precede route/page changes; RED must identify missing route or page/query contract.`
- Manual Checks: `MC-REUSE-001, MC-SCOPE-001, MC-TEST-001`
- Literal Rules: `Rule 11` — no Java file; preserve current DDC Web package and predecessor boundary.
- Ordered files:

#### File 1 — `MODIFY egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin-web/src/api/client.test.ts`

- Purpose: 固定 DDC page envelope 和缺失能力错误分类。
- Symbols: existing `ddcPageApi` tests plus instances/audit candidate cases。
- Repository evidence: current test verifies envelope parsing and invalid page error; `DdcApiError.category` already distinguishes 404/403/5xx。
- Dependencies and consumers: File 2 types, File 7 InstancesPage, App tests。
- Why now: RED client contract before adding page route。
- Contract/signature changes: no response wrapper change; assert `records/page.total/page.pageNo/page.pageSize` preserved and 404 remains `NOT_FOUND`/capability state。
- Input/output and state mapping: HTTP envelope -> typed page; 404 candidate -> error classification; abort -> DOMException passthrough。
- Error and edge behavior: malformed page rejects; no fallback to full List; 401 invokes existing unauthorized handler。
- Standards impact: `MC-REUSE-001, MC-SCOPE-001, MC-TEST-001` — reuse existing client/error classifier and predecessor page contract。
- Literal rule enforcement: `Rule 11` — DDC frontend API test only。
- Implementation pseudocode:

```ts
it('keeps DDC page metadata for instances', async () => {
  fetchMock.mockResolvedValue(pageResponse([{instanceId: 'i-1'}], {pageNo: 1, pageSize: 20, total: 41, pages: 3}))
  const page = await ddcPageApi<DdcInstance>('/api/v1/ddc/instances/page?pageNo=1&pageSize=20')
  expect(page.page.total).toBe(41)
  expect(page.records[0].instanceId).toBe('i-1')
})

it('classifies missing audit endpoint without turning it into empty success', async () => {
  fetchMock.mockResolvedValue(response404())
  await expect(ddcPageApi('/api/v1/ddc/audits/page')).rejects.toMatchObject({status: 404, category: 'NOT_FOUND'})
})
```

- Verification contribution: ensures client response semantics are not weakened for the new page。
- After this file: DDC client tests define RED behavior; no route/page file changed yet。

#### File 2 — `MODIFY egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin-web/src/api/types.ts`

- Purpose: 明确 DDC instance page and deferred audit capability frontend types。
- Symbols: preserve `PageResultRecord<T>`, `DdcInstance`; add `DdcCapabilityState`/safe audit capability view。
- Repository evidence: `DdcInstance`, `PageMetaRecord`, `DdcOperationLog`-aligned fields already exist in TypeScript/Java evidence。
- Dependencies and consumers: InstancesPage and client tests; no server model change。
- Why now: type definitions precede route/page consumer。
- Contract/signature changes: `DdcCapabilityState` is UI-local (`READY | NOT_CONFIGURED | FORBIDDEN | UNAVAILABLE`); `DdcInstance` fields unchanged。
- Input/output and state mapping: page record -> row fields; lease/heartbeat optional display; capability error -> state copy。
- Error and edge behavior: optional timestamp/metadata values render em dash; unknown status is text; no JPA Entity is exposed。
- Standards impact: `MC-SCOPE-001, MC-TEST-001` — explicit frontend types and existing page wrapper reuse。
- Literal rule enforcement: `Rule 11` — DDC Web type boundary only。
- Implementation pseudocode:

```ts
export type DdcCapabilityState = 'READY' | 'NOT_CONFIGURED' | 'FORBIDDEN' | 'UNAVAILABLE'
export type DdcCapabilityView = {state: DdcCapabilityState; message: string; traceId?: string}
export type DdcInstancePage = PageResultRecord<DdcInstance>
```

- Verification contribution: typecheck ensures InstancesPage uses existing `PageResultRecord` instead of inventing a second pagination shape。
- After this file: DDC response/state types ready for App/Layout/page tests。

#### File 3 — `MODIFY egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin-web/src/App.test.tsx`

- Purpose: 固定 `/instances` route，并确认未纳入当前路由契约的审计入口不会被误报为成功页面。
- Symbols: existing App route tests plus `rendersInstancesRoute`; audit 404 semantics由 File 1 client test 覆盖。
- Repository evidence: current App test covers login/registry/config routes; App has no instances/audits route。
- Dependencies and consumers: File 4 App, File 7 InstancesPage, AuthProvider/QueryClient test wrapper。
- Why now: App route RED before production route/menu changes。
- Contract/signature changes: `/instances` must render InstancesPage; uncontracted `/audits` remains outside this Step's successful route contract。
- Input/output and state mapping: Browser route -> page component; DDC audit API 404 -> File 1 capability state; auth -> RequireAuth。
- Error and edge behavior: unknown route keeps current behavior; authenticated route stays under DDC shell; 401 uses existing logout/redirect。
- Standards impact: `MC-SCOPE-001, MC-TEST-001` — route-level regression only, no new dependency。
- Literal rule enforcement: `Rule 11` — DDC Web App test only。
- Implementation pseudocode:

```tsx
it('renders configuration client instances at the dedicated route', async () => {
  window.history.replaceState({}, '', '/instances')
  render(<App />, {wrapper: ddcWrapper()})
  expect(await screen.findByText('配置客户端实例')).toBeInTheDocument()
})
```

- Verification contribution: route test catches menu/route mismatch；File 1 catches that a missing audit endpoint is shown as capability-unavailable rather than an empty table。
- After this file: App tests are RED for missing route/page as intended。

#### File 4 — `MODIFY egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin-web/src/App.tsx`

- Purpose: 注册 `/instances` route while preserving existing DDC routes/providers。
- Symbols: BrowserRouter route list and imports。
- Repository evidence: current root route redirects to `/registry` and lists eight existing pages。
- Dependencies and consumers: File 3 test, File 7 InstancesPage; AdminLayout Outlet。
- Why now: route wiring follows RED test and type definition。
- Contract/signature changes: add `Route path="instances"`; retain `/registry`, `/configs`, `/bizs`, `/envs`, `/apps`, `/namespaces`, `/publish-tasks`, `/cache`; no audit success route without contract。
- Input/output and state mapping: path -> component; `RequireAuth` continues guarding all routes。
- Error and edge behavior: direct `/instances` loads under auth; unknown route behavior remains existing; no new API request at router level。
- Standards impact: `MC-REUSE-001, MC-SCOPE-001, MC-TEST-001` — reuse BrowserRouter/RequireAuth/AdminLayout; no new state manager。
- Literal rule enforcement: `Rule 11` — existing DDC frontend route structure。
- Implementation pseudocode:

```tsx
import InstancesPage from './pages/InstancesPage'

<Route element={<RequireAuth><AdminLayout /></RequireAuth>}>
  <Route index element={<Navigate to="/registry" replace />} />
  <Route path="registry" element={<RegistryPage />} />
  <Route path="instances" element={<InstancesPage />} />
  <Route path="configs" element={<ConfigsPage />} />
</Route>
```

- Verification contribution: File 3 becomes GREEN and route remains within DDC auth shell。
- After this file: route exists but menu/page components still need wiring。

#### File 5 — `MODIFY egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin-web/src/layouts/AdminLayout.test.tsx`

- Purpose: 固定 instances static menu node和 active path。
- Symbols: existing layout tests plus `rendersConfigurationClientInstancesMenu`。
- Repository evidence: current layout tests cover EnterpriseLayout/navigation; current navigation has runtime/config/metadata groups but no instances。
- Dependencies and consumers: File 6 layout, shared Sidebar/path resolver, App route。
- Why now: menu test before modifying production navigation。
- Contract/signature changes: add exact label/path only; capability source remains DDC bootstrap/static config, no DB menu call。
- Input/output and state mapping: `/instances` -> runtime group selected/open; collapse/mobile drawer retains existing behavior。
- Error and edge behavior: no permission -> node hidden only if existing DDC permission pruning supports it; no backend call from shared menu。
- Standards impact: `MC-REUSE-001, MC-SCOPE-001, MC-TEST-001` — reuse shared EnterpriseLayout test patterns。
- Literal rule enforcement: `Rule 11` — DDC frontend layout test only。
- Implementation pseudocode:

```tsx
it('shows configuration client instances under runtime', () => {
  render(<AdminLayout />, {wrapper: ddcLayoutWrapper('/instances')})
  expect(screen.getByText('配置客户端实例')).toBeInTheDocument()
  expect(screen.getByText('配置客户端实例').closest('.ant-menu-item')).toHaveClass('ant-menu-item-selected')
})
```

- Verification contribution: catches path/label mismatch before File 6。
- After this file: layout test is RED until static navigation is extended。

#### File 6 — `MODIFY egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin-web/src/layouts/AdminLayout.tsx`

- Purpose: 将实例入口加入 DDC runtime 菜单，保持静态/config-driven 权威。
- Symbols: `navigation` runtime group。
- Repository evidence: current runtime group has registry/publish-tasks/cache; shared Sidebar recursively renders passed items。
- Dependencies and consumers: File 5 test, App route, `EnterpriseLayout`。
- Why now: production menu wiring follows route test。
- Contract/signature changes: add `{key:'instances', label:'配置客户端实例', path:'/instances'}`; no menu API or capability fetch。
- Input/output and state mapping: static item -> Sidebar; current location -> selectedKey via shared longest-path resolver; user identity/actions remain unchanged。
- Error and edge behavior: standalone route keeps menu; mobile Drawer close on navigate remains shared behavior。
- Standards impact: `MC-REUSE-001, MC-SCOPE-001, MC-TEST-001` — reuse shared menu and current DDC layout config。
- Literal rule enforcement: `Rule 11` — existing DDC Web layout path; no backend layer。
- Implementation pseudocode:

```tsx
const navigation = [
  {key: 'runtime', label: '运行状态', children: [
    {key: 'registry', label: '服务注册', path: '/registry'},
    {key: 'instances', label: '配置客户端实例', path: '/instances'},
    {key: 'publish-tasks', label: '发布任务', path: '/publish-tasks'},
    {key: 'cache', label: '缓存', path: '/cache'},
  ]},
  ...existingGroups,
]
return <EnterpriseLayout config={{...config, navigation}}><Outlet /></EnterpriseLayout>
```

- Verification contribution: File 5 becomes GREEN and App route is discoverable。
- After this file: DDC menu and route are connected; page implementation remains。

#### File 7 — `CREATE egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin-web/src/pages/InstancesPage.test.tsx`

- Purpose: 固定 Instances page scope/query/pagination/empty/error behavior。
- Symbols: `rendersPagedInstances`, `resetsPageAfterScopeChange`, `showsRetryOnReadFailure`。
- Repository evidence: RegistryPage already uses `ddcPageApi`, `ScopeSelects`, `usePageState`, `keepPreviousData`, Table pagination patterns。
- Dependencies and consumers: File 8 page; DDC client/types and App route。
- Why now: page RED contract before creating the new page。
- Contract/signature changes: request exact `/api/v1/ddc/instances/page?pageNo=&pageSize=&bizCode=&env=&appCode=`; page metadata maps to AntD one-based display only。
- Input/output and state mapping: ScopeSelects -> submitted scope; PageResultRecord -> rows/total; last heartbeat/lease -> display fields; Query key includes submitted scope/page。
- Error and edge behavior: invalid page rejects; empty page is contextual; 403/5xx distinct; retry keeps scope/page; stable rowKey `instanceId`。
- Standards impact: `MC-REUSE-001, MC-SCOPE-001, MC-TEST-001` — reuse predecessor `PageResultRecord`/`ddcPageApi` and existing DDC test renderer。
- Literal rule enforcement: `Rule 11` — DDC Web page test only。
- Implementation pseudocode:

```tsx
it('requests instances with submitted scope and server page metadata', async () => {
  mockDdcPage([{instanceId: 'i-1', bizCode: 'commerce', env: 'prod', appCode: 'orders', status: 'ONLINE'}], {pageNo: 1, pageSize: 20, total: 21})
  render(<InstancesPage />, {wrapper: ddcQueryWrapper('/instances')})
  await userEvent.click(screen.getByRole('button', {name: '查询'}))
  expect(await screen.findByText('i-1')).toBeInTheDocument()
  expect(fetchMock).toHaveBeenCalledWith(expect.stringContaining('/api/v1/ddc/instances/page'))
  expect(screen.getByText('共 21 条')).toBeInTheDocument()
})

it('shows retryable error without clearing submitted scope', async () => {
  mockDdcError(500)
  render(<InstancesPage />, {wrapper: ddcQueryWrapper('/instances?env=prod')})
  expect(await screen.findByRole('button', {name: '重试'})).toBeInTheDocument()
})
```

- Verification contribution: defines page contract and prevents full-list or wrong page-base implementation。
- After this file: Instances page test is RED before File 8。

#### File 8 — `CREATE egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin-web/src/pages/InstancesPage.tsx`

- Purpose: 提供配置客户端实例的独立服务端分页运营页。
- Symbols: `InstancesPage` scope/filter state, `instancesQuery`, columns and pagination。
- Repository evidence: backend path exists in Spec §9.2; DDC RegistryPage demonstrates exact `ddcPageApi`/`ScopeSelects`/`PageState` conventions。
- Dependencies and consumers: File 7 test, File 2 types, DDC App/Layout route, `usePageState`/`buildQuery`。
- Why now: implements the existing endpoint only after RED contract。
- Contract/signature changes: GET `/api/v1/ddc/instances/page`; query parameters use `pageNo/pageSize` and submitted scope; no new server field。
- Input/output and state mapping: DdcInstance -> columns instanceId/status/host/port/lease/lastHeartbeatAt; page meta -> current/pageSize/total; scope change resets page。
- Error and edge behavior: `keepPreviousData` can display prior rows with fetching state; 401 uses existing unauthorized handler; 403/5xx PageState; no blind retry for writes because page is read-only。
- Standards impact: `MC-REUSE-001, MC-SCOPE-001, MC-TEST-001` — reuse all existing DDC page primitives and no new dependency/store。
- Literal rule enforcement: `Rule 11` — file stays under DDC `src/pages` frontend profile。
- Implementation pseudocode:

```tsx
const pageState = usePageState()
const [draft, setDraft] = useState<ScopeValue>(() => ({...emptyScope}))
const [submitted, setSubmitted] = useState<ScopeValue>(() => ({...emptyScope}))
const query = useQuery({queryKey: ['ddc', 'instances', submitted, pageState.page], queryFn: ({signal}) => ddcPageApi<DdcInstance>(`/api/v1/ddc/instances/page?${buildQuery({...submitted, pageNo: pageState.page.pageNo, pageSize: pageState.page.pageSize})}`, {signal}), placeholderData: keepPreviousData})
return <><AdminPageHeader title="配置客户端实例" description="按作用域查看配置客户端租约与最近心跳。" /><ScopeFilter draft={draft} onSubmit={() => {setSubmitted({...draft}); pageState.resetPage()}} /><PageState loading={query.isPending} error={query.error} empty={(query.data?.records.length ?? 0) === 0} onRetry={() => void query.refetch()}><Table rowKey="instanceId" dataSource={query.data?.records ?? []} pagination={{current: query.data?.page.pageNo ?? pageState.page.pageNo, pageSize: query.data?.page.pageSize ?? pageState.page.pageSize, total: query.data?.page.total ?? 0, onChange: pageState.onTableChange}} /></PageState></>
```

- Verification contribution: File 7 becomes GREEN; App/Layout tests observe route/menu and real page path。
- After this file: DDC existing instance capability is complete; audit/validate/diff candidates remain explicitly deferred。

- Validation working directory: `/Users/mario/SelfProject/Egon-COLA/egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin-web`
- Verification command: `npm run test -- --run src/api/client.test.ts src/App.test.tsx src/layouts/AdminLayout.test.tsx src/pages/InstancesPage.test.tsx && npm run typecheck && npm run lint`
- Expected result: DDC page envelope/client tests, `/instances` route/menu and InstancesPage tests pass; `/page` fields and page total are exact; typecheck/lint exit 0。
- Failure returns to: Files 1-2 for envelope/types; Files 3-6 for route/menu; Files 7-8 for page query/table behavior。
- Completion criteria: existing DDC pagination compatibility is preserved, instances has a discoverable independent page, and no audit/validate/diff endpoint is falsely reported as implemented。
- Rollback: revert only the eight Step 5 paths; no DDC server/config/database state changes。
- Commit paths: `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin-web/src/api/client.test.ts`, `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin-web/src/api/types.ts`, `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin-web/src/App.test.tsx`, `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin-web/src/App.tsx`, `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin-web/src/layouts/AdminLayout.test.tsx`, `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin-web/src/layouts/AdminLayout.tsx`, `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin-web/src/pages/InstancesPage.test.tsx`, `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin-web/src/pages/InstancesPage.tsx`
- Commit: `feat(ddc-admin-web): add paged client instance operations`

### Step 6 — 建立 Wujie Portal、child lifecycle、非敏感 bridge 与首页 Facade

- Requirements: `REQ-001, REQ-002, REQ-003, REQ-004, REQ-005, REQ-006, REQ-007, REQ-008, REQ-013, REQ-014, REQ-015, REQ-016`
- Dependencies: `Step 1-5` focused contracts; standalone child builds available; Wujie package compatibility spike and lockfile review must precede host implementation。
- Baseline state: no Portal/Manifest/lifecycle/bridge; all four child `main.tsx` directly call `createRoot`; child layouts always render their own shared shell; no host summary boundary。
- Observable outcome: Portal can load/validate a versioned Manifest, render unified home, mount a selected child through `WujieReact`, pass only an allow-listed non-sensitive context, isolate child failure, clean lifecycle resources and retain standalone child links。
- End state: Portal and child lifecycle contracts are static/component-testable; Wujie dependency is only in Portal; runtime/browser/deployment proof remains user-controlled after this Plan。
- Test-first gate: `Required — manifest, lifecycle state, bridge security, summary partial and Portal pages are written before implementation; child main/layout lifecycle changes follow the host contract tests. Dependency installation is a compile prerequisite and is explicitly validated before RED/GREEN.`
- Manual Checks: `MC-DEP-001, MC-REUSE-001, MC-PATTERN-001, MC-SCOPE-001, MC-TEST-001`
- Literal Rules: `Rule 11` — no Java file; Portal uses frontend Adapter/State/Facade composition and future Java traditional profile remains untouched.
- Ordered files:

#### File 1 — `CREATE egon-cola-platforms/egon-cola-platform-admin-portal/src/manifest/loader.test.ts`

- Purpose: 固定 Manifest schema/version/URL/timeout gate before Wujie mount。
- Symbols: `acceptsCompatibleManifest`, `rejectsInvalidManifest`, `rejectsUnsupportedHostRange`。
- Repository evidence: no current Manifest; Spec §7.3.1 defines platformKey/environment -> child asset/version metadata and `MOUNT_FAILED`。
- Dependencies and consumers: File 2 loader, File 15 ChildRoutePage, WujieChild；test setup File 23。
- Why now: first Portal RED test does not require child mounting or business API。
- Contract/signature changes: `loadManifest(platformKey, environment, signal)` returns `ChildManifest`; required fields `name,url,standaloneUrl,version,contractVersion,compatibleHostRange,requiredCapabilities`。
- Input/output and state mapping: same-site fetch JSON -> validated manifest; timeout/invalid/version mismatch -> typed mount error; no token/secret fields accepted。
- Error and edge behavior: non-2xx/invalid JSON/missing URL/unsupported semver -> fail before Wujie; AbortSignal aborts without retry; standaloneUrl remains fallback only。
- Standards impact: `MC-DEP-001, MC-PATTERN-001, MC-SCOPE-001, MC-TEST-001` — no runtime call in test; dependency is mocked at boundary; manifest validation is Adapter prerequisite。
- Literal rule enforcement: `Rule 11` — Portal frontend package is a new host package, not a Java layer。
- Implementation pseudocode:

```ts
it('accepts a compatible manifest and keeps standalone fallback', async () => {
  mockFetch.ok(manifest({version: '5.3.2', compatibleHostRange: '>=1.0.0'}))
  const result = await loadManifest('idp', 'local', new AbortController().signal)
  expect(result.url).toContain('idp')
  expect(result.standaloneUrl).toContain('idp')
})

it('rejects unsupported version before mount', async () => {
  mockFetch.ok(manifest({compatibleHostRange: '>=9.0.0'}))
  await expect(loadManifest('idp', 'local', signal)).rejects.toMatchObject({code: 'MANIFEST_VERSION_UNSUPPORTED'})
  expect(startWujieMock).not.toHaveBeenCalled()
})
```

- Verification contribution: RED validates the most important host safety gate before production loader。
- After this file: Manifest test is RED only for absent loader/fixture helper; no child is mounted。

#### File 2 — `CREATE egon-cola-platforms/egon-cola-platform-admin-portal/src/manifest/loader.ts`

- Purpose: 加载并验证同源 Manifest，阻断不兼容子应用。
- Symbols: `loadManifest`；`ManifestLoadError`；schema/range validator。
- Repository evidence: Spec §7.3.1/§15 requires versioned cached assets, compatibility range and `MOUNT_FAILED`; no current loader exists。
- Dependencies and consumers: File 1 test, ChildRoutePage, lifecycle adapter; uses browser fetch only and package-local manifest types。
- Why now: minimum GREEN implementation for Manifest test before Wujie wrapper。
- Contract/signature changes: GET `/portal-manifest/{environment}.json` or configured same-site manifest URL; output exact `ChildManifest`; no auth header/token propagation.
- Input/output and state mapping: platformKey/environment -> manifest URL; response JSON -> normalized typed object; contract version/range -> accepted/rejected state。
- Error and edge behavior: AbortError rethrows; HTTP/JSON/schema/version errors map to stable code/message; unsafe URL schemes rejected; no fallback to arbitrary external URL。
- Standards impact: `MC-DEP-001, MC-PATTERN-001, MC-SCOPE-001, MC-TEST-001` — Adapter boundary contains Wujie-independent manifest logic and adds no package beyond approved runtime。
- Literal rule enforcement: `Rule 11` — Portal manifest code belongs to frontend host package; no Java profile mixing。
- Implementation pseudocode:

```ts
export const loadManifest = async (platformKey: PlatformKey, environment: string, signal: AbortSignal): Promise<ChildManifest> => {
  const response = await fetch(`/portal-manifest/${encodeURIComponent(environment)}.json`, {credentials: 'include', signal})
  if (!response.ok) throw new ManifestLoadError('MANIFEST_HTTP_ERROR', response.status)
  const raw = await response.json()
  const manifest = parseChildManifest(raw, platformKey)
  if (!isCompatible(manifest.compatibleHostRange, PORTAL_VERSION)) throw new ManifestLoadError('MANIFEST_VERSION_UNSUPPORTED')
  if (!isSameSiteOrApprovedAssetUrl(manifest.url)) throw new ManifestLoadError('MANIFEST_URL_NOT_ALLOWED')
  return manifest
}
```

- Verification contribution: File 1 becomes GREEN; invalid manifest cannot reach WujieChild。
- After this file: Manifest loader is a stable host boundary; lifecycle state still follows。

#### File 3 — `CREATE egon-cola-platforms/egon-cola-platform-admin-portal/src/lifecycle/lifecycleState.test.ts`

- Purpose: 固定 mount/unmount/remount state transitions and cleanup-before-remount。
- Symbols: `initialLifecycleState`, `reduceLifecycle` cases for `LOAD`, `MOUNTED`, `MOUNT_FAILED`, `CRASHED`, `UNMOUNT`。
- Repository evidence: Spec §7.3.4/§12.5 requires cleanup of listeners/timers/query observers/DOM; no host lifecycle state exists。
- Dependencies and consumers: File 4 reducer, WujieChild, ChildRoutePage。
- Why now: State pattern test precedes adapter implementation。
- Contract/signature changes: reducer accepts explicit events and returns immutable state; remount event is illegal while `UNMOUNTING` cleanup not complete。
- Input/output and state mapping: state + event -> state/status/error/retryCount; no business data mutation。
- Error and edge behavior: duplicate mount ignored or returns existing loading; crash moves only child to `CRASHED`; cleanup failure becomes local `MOUNT_FAILED` with standalone fallback。
- Standards impact: `MC-PATTERN-001, MC-SCOPE-001, MC-TEST-001` — explicit State pattern is tested; no global store or runtime dependency in pure reducer。
- Literal rule enforcement: `Rule 11` — frontend State pattern inside Portal host package。
- Implementation pseudocode:

```ts
it('requires cleanup before remount', () => {
  let state = reduceLifecycle(initialLifecycleState, {type: 'LOAD'})
  state = reduceLifecycle(state, {type: 'MOUNT_FAILED', error: 'crashed'})
  expect(reduceLifecycle(state, {type: 'RETRY'}).status).toBe('LOADING')
  state = reduceLifecycle(state, {type: 'UNMOUNT'})
  expect(reduceLifecycle(state, {type: 'MOUNT'}).status).toBe('MOUNT_FAILED')
})

it('keeps child failure local', () => {
  const state = reduceLifecycle({status: 'MOUNTED', child: 'gateway'}, {type: 'CHILD_CRASHED', error: 'render'})
  expect(state.status).toBe('CRASHED')
  expect(state.child).toBe('gateway')
})
```

- Verification contribution: proves State pattern transitions before Wujie callbacks are wired。
- After this file: lifecycle reducer test defines legal host transitions。

#### File 4 — `CREATE egon-cola-platforms/egon-cola-platform-admin-portal/src/lifecycle/lifecycleState.ts`

- Purpose: 实现 host lifecycle State reducer。
- Symbols: `LifecycleStatus`, `LifecycleState`, `LifecycleEvent`, `reduceLifecycle`。
- Repository evidence: Spec lifecycle states `LOADING/MOUNT_FAILED/mounted/unmounted/error`; no existing host state.
- Dependencies and consumers: File 3, WujieChild, ChildRoutePage; pure TypeScript only。
- Why now: makes lifecycle test GREEN and isolates state semantics from Wujie API。
- Contract/signature changes: state carries `platformKey`, `status`, optional `errorCode/errorMessage`, `retryCount`, `cleanupToken`; events are typed and exhaustive。
- Input/output and state mapping: `LOAD` -> loading; `MOUNTED` -> mounted; `CHILD_CRASHED` -> crashed; `UNMOUNT` -> unmounting; `CLEANED` -> idle。
- Error and edge behavior: status transitions never clear standaloneUrl/route input; duplicate cleanup is idempotent; retry count bounded by adapter caller。
- Standards impact: `MC-PATTERN-001, MC-SCOPE-001, MC-TEST-001` — State pattern is the only host state abstraction; no QueryClient replacement。
- Literal rule enforcement: `Rule 11` — Portal lifecycle module remains frontend host package。
- Implementation pseudocode:

```ts
export const reduceLifecycle = (state: LifecycleState, event: LifecycleEvent): LifecycleState => {
  switch (event.type) {
    case 'LOAD': return {...state, status: 'LOADING', errorCode: undefined}
    case 'MOUNTED': return {...state, status: 'MOUNTED', cleanupToken: event.cleanupToken}
    case 'MOUNT_FAILED': return {...state, status: 'MOUNT_FAILED', errorCode: event.code, errorMessage: event.message}
    case 'CHILD_CRASHED': return {...state, status: 'CRASHED', errorCode: 'CHILD_RUNTIME_ERROR', errorMessage: event.message}
    case 'UNMOUNT': return state.status === 'MOUNTED' ? {...state, status: 'UNMOUNTING'} : state
    case 'CLEANED': return {...state, status: 'IDLE', cleanupToken: undefined}
  }
}
```

- Verification contribution: File 3 becomes GREEN; WujieChild can use typed transitions instead of booleans。
- After this file: State pattern is available to adapter/page files。

#### File 5 — `CREATE egon-cola-platforms/egon-cola-platform-admin-portal/src/bridge/context.test.ts`

- Purpose: 固定 host-child bridge 的敏感字段清理。
- Symbols: `sanitizeChildContext`, `buildChildProps`, `emitsSafeRouteEvent`。
- Repository evidence: Spec §7.1/§15/TEST-017 restricts bridge to non-sensitive route/scope/capability summary; Wujie docs support props/bus but do not authorize credential relay。
- Dependencies and consumers: File 6 context, WujieChild, Portal pages。
- Why now: security RED test before bridge implementation。
- Contract/signature changes: input may include unsafe unknown keys but output only allow-list fields; event payload is serializable and non-secret。
- Input/output and state mapping: host context -> child props/event; `platformKey/routeIntent/scopeDisplay/capabilitySummary/hostVersion` retained; token/secret/cookie/body/header removed。
- Error and edge behavior: nested sensitive keys are recursively omitted; function values are not serialized into event payload; unknown fields dropped rather than forwarded。
- Standards impact: `MC-SCOPE-001, MC-TEST-001` — negative security assertions are blocking; no new transport/dependency。
- Literal rule enforcement: `Rule 11` — bridge stays inside Portal frontend host.
- Implementation pseudocode:

```ts
it('removes credentials and raw request material from child context', () => {
  const value = sanitizeChildContext({platformKey: 'gateway', routeIntent: '/dashboard', accessToken: 'x', cookie: 'y', headers: {Authorization: 'z'}, body: {secret: 'q'}})
  expect(value).toEqual({platformKey: 'gateway', routeIntent: '/dashboard'})
  expect(JSON.stringify(value)).not.toMatch(/accessToken|cookie|Authorization|secret|body/i)
})

it('emits only a safe route event', () => {
  const event = buildRouteEvent({platformKey: 'ddc', routeIntent: '/instances', scopeDisplay: 'prod'})
  expect(event).toEqual({platformKey: 'ddc', routeIntent: '/instances', scopeDisplay: 'prod'})
})
```

- Verification contribution: prevents a host/child integration from accidentally relaying credentials。
- After this file: bridge security test is RED until File 6 implements allow-list sanitizer。

#### File 6 — `CREATE egon-cola-platforms/egon-cola-platform-admin-portal/src/bridge/context.ts`

- Purpose: 实现非敏感 host-child context allow-list。
- Symbols: `ChildContext`, `sanitizeChildContext`, `buildRouteEvent`。
- Repository evidence: Spec bridge boundary and Wujie `$wujie.props`/bus contract; no local bridge exists。
- Dependencies and consumers: File 5, WujieChild, Portal Home/ChildRoute pages。
- Why now: makes security RED/GREEN contract executable before wrapper integration。
- Contract/signature changes: output fields are explicit; no function/token/cookie/body/header pass-through; `capabilitySummary` contains only labels/booleans not credentials。
- Input/output and state mapping: input unknown record -> safe context; route/scope values are display/intent only and never trusted permission facts。
- Error and edge behavior: strip nested case-insensitive sensitive key names; preserve only primitive safe values/approved records; invalid platform key rejects context。
- Standards impact: `MC-SCOPE-001, MC-TEST-001` — allow-list security and no new network layer。
- Literal rule enforcement: `Rule 11` — Portal frontend bridge module only。
- Implementation pseudocode:

```ts
const SAFE_KEYS = new Set(['platformKey', 'routeIntent', 'scopeDisplay', 'capabilitySummary', 'hostVersion'])
const SENSITIVE = /token|secret|cookie|authorization|password|raw.?body|header/i
export const sanitizeChildContext = (source: Record<string, unknown>): ChildContext => Object.fromEntries(
  Object.entries(source).filter(([key, value]) => SAFE_KEYS.has(key) && !SENSITIVE.test(key) && isSafeValue(value)),
) as ChildContext
export const buildRouteEvent = (context: Record<string, unknown>) => sanitizeChildContext(context)
```

- Verification contribution: File 5 becomes GREEN; Wujie props/events can use one safe context function。
- After this file: bridge API is safe and independent of Wujie runtime。

#### File 7 — `CREATE egon-cola-platforms/egon-cola-platform-admin-portal/src/summary/platformSummaryFacade.test.ts`

- Purpose: 固定首页 Facade 的 partial/timeout/retry semantics。
- Symbols: `returnsPartialWhenOneAdapterTimesOut`, `doesNotCachePartialAsGlobalSuccess`, `retriesOnlyOnePlatform`。
- Repository evidence: Spec DEC-105/§7.3.3 requires read-only aggregate with per-platform partial; current four platforms have independent status/query boundaries。
- Dependencies and consumers: File 8 Facade, PortalHomePage, adapter fixtures。
- Why now: Facade complexity is tested before host page and Wujie mount。
- Contract/signature changes: `loadSummary(context, signal)` returns one result per platform with `status`, `durationMs`, optional `traceId`; partial is explicit。
- Input/output and state mapping: adapters -> `Promise.allSettled`; fulfilled -> READY/DEGRADED; rejected/timeout -> ERROR/TIMEOUT; retry(platformKey) invokes only that adapter。
- Error and edge behavior: timeout does not abort other adapters; partial response is not stored as global success; 401/403 remain platform-specific statuses; no write calls。
- Standards impact: `MC-PATTERN-001, MC-SCOPE-001, MC-TEST-001` — Facade pattern has concrete adapter/timeout/partial tests; no universal BFF。
- Literal rule enforcement: `Rule 11` — frontend Facade inside Portal package, no Java business service。
- Implementation pseudocode:

```ts
it('returns partial cards when one platform times out', async () => {
  const result = await loadSummary({scope: 'default'}, signal, {idp: resolves('READY'), rbac3: timeout(), gateway: resolves('READY'), ddc: notConfigured()})
  expect(result.overall).toBe('PARTIAL')
  expect(result.cards.idp.status).toBe('READY')
  expect(result.cards.rbac3.status).toBe('TIMEOUT')
  expect(result.cards.ddc.status).toBe('NOT_CONFIGURED')
})

it('retries one card without re-requesting others', async () => {
  await retrySummary('gateway')
  expect(adapters.gateway).toHaveBeenCalledTimes(2)
  expect(adapters.idp).toHaveBeenCalledTimes(1)
})
```

- Verification contribution: proves the approved Facade pattern and failure semantics before UI composition。
- After this file: summary test is RED until File 8 implementation。

#### File 8 — `CREATE egon-cola-platforms/egon-cola-platform-admin-portal/src/summary/platformSummaryFacade.ts`

- Purpose: 实现 host-side read-only summary Facade，不创建服务端 BFF。
- Symbols: `PlatformSummary`, `PlatformSummaryAdapter`, `loadSummary`, `retrySummary`。
- Repository evidence: Spec §8.3 explicitly assigns Portal `PlatformSummaryFacade`; §9 server summary is deferred; existing Admin APIs remain domain-owned。
- Dependencies and consumers: File 7 test, PortalHomePage, manifest/lifecycle state; browser fetch adapters are injected, not hard-coded business clients。
- Why now: minimum GREEN implementation for partial/timeout test and concrete Facade pattern participant。
- Contract/signature changes: adapter registry is injected/configured; host can return `NOT_CONFIGURED` if no approved summary URL/child state exists; no mutation method。
- Input/output and state mapping: context/signal -> per-platform card; `Promise.allSettled` -> overall; duration/traceId -> visible support copy; retry -> per-card query only。
- Error and edge behavior: each adapter has bounded timeout; 401/403/404/5xx map per card; partial is never cached as full success; AbortSignal cancels host request only。
- Standards impact: `MC-PATTERN-001, MC-REUSE-001, MC-SCOPE-001, MC-TEST-001` — Facade/Adapter uses injected existing read boundaries; no new state library or backend contract。
- Literal rule enforcement: `Rule 11` — frontend pattern in Portal package; no Java service structure introduced。
- Implementation pseudocode:

```ts
export const loadSummary = async (context: SummaryContext, signal: AbortSignal, adapters: SummaryAdapters): Promise<PlatformSummary> => {
  const entries = await Promise.all(Object.entries(adapters).map(async ([platformKey, adapter]) => {
    const started = performance.now()
    try { return [platformKey, await withTimeout(adapter(context, signal), 3000, signal, 'TIMEOUT', started)] as const }
    catch (error) { return [platformKey, mapSummaryFailure(error, performance.now() - started)] as const }
  }))
  const cards = Object.fromEntries(entries)
  return {cards, overall: Object.values(cards).every((card) => card.status === 'READY') ? 'READY' : 'PARTIAL'}
}
```

- Verification contribution: File 7 becomes GREEN; Portal home can show per-card state without knowing platform business models。
- After this file: summary Facade is implemented and ready for HomePage composition。

#### File 9 — `CREATE egon-cola-platforms/egon-cola-platform-admin-portal/src/app/router.tsx`

- Purpose: 定义 Portal 首页、平台 child route、standalone fallback 和 404。
- Symbols: `portalRoutes`/`PortalRouter`。
- Repository evidence: each child has BrowserRouter and standalone routes; Spec §7.2 requires host route intent/deep link/standalone link。
- Dependencies and consumers: PortalHomePage, ChildRoutePage, App, Manifest/lifecycle state。
- Why now: routing is independent from Wujie import and enables page tests。
- Contract/signature changes: route path `/`, `/platform/:platformKey/*`, `/standalone/:platformKey`; query/scope stays URL display context; no backend authority in route params。
- Input/output and state mapping: platform key/path -> manifest/load child; invalid key -> 404; direct child URL -> standalone anchor。
- Error and edge behavior: refresh deep link returns host route; unsupported platform shows recoverable error and standalone link; no silent route rewrite to another platform。
- Standards impact: `MC-REUSE-001, MC-SCOPE-001, MC-TEST-001` — reuse React Router 7 already present in workspace; no new router/state library。
- Literal rule enforcement: `Rule 11` — Portal frontend routing module only。
- Implementation pseudocode:

```tsx
export const portalRoutes = [
  {path: '/', element: <PortalHomePage />},
  {path: '/platform/:platformKey/*', element: <ChildRoutePage />},
  {path: '/standalone/:platformKey', element: <StandaloneRedirectPage />},
  {path: '*', element: <Result status="404" title="页面不存在" />},
]
export const PortalRouter = () => <Routes>{portalRoutes.map((route) => <Route key={route.path} {...route} />)}</Routes>
```

- Verification contribution: Portal page tests can navigate exact paths and assert no business API route is owned by host。
- After this file: route contract exists; provider composition follows。

#### File 10 — `CREATE egon-cola-platforms/egon-cola-platform-admin-portal/src/app/App.tsx`

- Purpose: 组合 Portal QueryClient、shared theme/i18n、Router 和顶层错误隔离。
- Symbols: `App` and host QueryClient configuration。
- Repository evidence: existing Gateway/DDC/IDP main entries show provider patterns; shared exports `AdminThemeProvider`, `I18nProvider`, `AppErrorBoundary`。
- Dependencies and consumers: File 9 router, File 14/16 pages, Portal main。
- Why now: app composition follows route and Facade contracts but precedes entry file。
- Contract/signature changes: no business API client is added; QueryClient only stores host manifest/summary state; retry policy excludes unsafe mutations because host has none。
- Input/output and state mapping: provider context -> pages; host query keys include platform/environment/scope; child data remains child-owned。
- Error and edge behavior: AppErrorBoundary displays local host crash/reload; child errors are handled in WujieChild; no token is read from browser storage。
- Standards impact: `MC-REUSE-001, MC-SCOPE-001, MC-TEST-001` — reuse shared providers and QueryClient; no new store/framework。
- Literal rule enforcement: `Rule 11` — Portal frontend composition only。
- Implementation pseudocode:

```tsx
const queryClient = new QueryClient({defaultOptions: {queries: {retry: false, refetchOnWindowFocus: false}, mutations: {retry: false}}})
export const App = () => (
  <QueryClientProvider client={queryClient}>
    <AdminThemeProvider><I18nProvider><BrowserRouter><AppErrorBoundary><PortalRouter /></AppErrorBoundary></BrowserRouter></I18nProvider></AdminThemeProvider>
  </QueryClientProvider>
)
```

- Verification contribution: app/page tests run with the same provider contract as production entry。
- After this file: Portal application composition is wired; main/index/test setup remain。

#### File 11 — `CREATE egon-cola-platforms/egon-cola-platform-admin-portal/src/manifest/types.ts`

- Purpose: 定义完整 child Manifest/host compatibility frontend contract。
- Symbols: `PlatformKey`, `ChildManifest`, `ManifestEnvironment`, `ManifestCapability`。
- Repository evidence: Spec §7.3.1/§8.2 requires platformKey, child asset/version metadata, compatibility and standalone link。
- Dependencies and consumers: loader/Portal pages/WujieChild; no external schema library required。
- Why now: type source must exist before loader/host components compile; it is a local contract, not backend API。
- Contract/signature changes: required `key`, `displayName`, `url`, `standaloneUrl`, `version`, `contractVersion`, `compatibleHostRange`, `requiredCapabilities`; optional `summaryUrl` is non-secret same-site URL and may be absent。
- Input/output and state mapping: JSON -> typed Manifest; missing optional summary -> NOT_CONFIGURED; required fields missing -> loader error。
- Error and edge behavior: URL schemes and arbitrary external targets rejected by loader; capabilities are display hints, not authorization source。
- Standards impact: `MC-DEP-001, MC-SCOPE-001, MC-TEST-001` — exact TypeScript contract supports approved Wujie dependency and no public sensitive field。
- Literal rule enforcement: `Rule 11` — Portal manifest type is frontend-only。
- Implementation pseudocode:

```ts
export type PlatformKey = 'idp' | 'rbac3' | 'gateway' | 'ddc'
export type ChildManifest = {
  key: PlatformKey; displayName: string; url: string; standaloneUrl: string
  version: string; contractVersion: string; compatibleHostRange: string
  requiredCapabilities: readonly string[]; summaryUrl?: string
}
```

- Verification contribution: loader/test/page code has one exact type source。
- After this file: manifest type is available to Files 1/2/13/15。

#### File 12 — `CREATE egon-cola-platforms/egon-cola-platform-admin-portal/src/lifecycle/WujieChild.tsx`

- Purpose: 用 Adapter 隔离 `wujie-react` 具体生命周期，处理 mount/unmount/error/standalone fallback。
- Symbols: `WujieChildProps`, `WujieChild`。
- Repository evidence: official wrapper exposes `WujieReact` with `name/url/sync/props/beforeLoad/beforeMount/afterMount/beforeUnmount/afterUnmount/loadError`; Spec §13 selects Adapter。
- Dependencies and consumers: Files 4/6 State/bridge, File 15 ChildRoutePage, package `wujie-react@2.1.0`。
- Why now: adapter is added only after manifest/state/bridge contracts are fixed。
- Contract/signature changes: props include manifest, routeIntent, scopeDisplay, capabilitySummary and lifecycle callbacks; Wujie receives sanitized `props`, not auth credentials。
- Input/output and state mapping: Manifest -> Wujie `name/url`; lifecycle callbacks -> `reduceLifecycle`; child error -> local error card; unmount -> cleanup event/handler disposal。
- Error and edge behavior: invalid manifest prevents mount; callback failure dispatches MOUNT_FAILED; retry destroys prior instance/observers before remount; standalone anchor always remains available。
- Standards impact: `MC-DEP-001, MC-PATTERN-001, MC-SCOPE-001, MC-TEST-001` — Adapter uses the single approved runtime and State/bridge contracts; no Wujie calls outside this file。
- Literal rule enforcement: `Rule 11` — frontend Adapter module, no Java layer.
- Implementation pseudocode:

```tsx
export const WujieChild = ({manifest, context, onState}: WujieChildProps) => {
  const safeProps = sanitizeChildContext({...context, embedded: true, hostVersion: PORTAL_VERSION})
  return <ErrorBoundary fallback={<ChildFailure standaloneUrl={manifest.standaloneUrl} onRetry={remount} />}>
    <WujieReact name={manifest.key} url={manifest.url} width="100%" height="100%" sync props={safeProps}
      beforeLoad={() => onState({type: 'LOAD'})} afterMount={() => onState({type: 'MOUNTED', cleanupToken: manifest.version})}
      beforeUnmount={() => onState({type: 'UNMOUNT'})} afterUnmount={() => onState({type: 'CLEANED'})}
      loadError={(url, error) => onState({type: 'MOUNT_FAILED', code: 'CHILD_LOAD_ERROR', message: `${url}:${String(error)}`})} />
  </ErrorBoundary>
}
```

- Verification contribution: ChildRoutePage tests can mock one adapter and assert local failure/cleanup/standalone link。
- After this file: Wujie API is concentrated in one Adapter; child app entry work remains。

#### File 13 — `CREATE egon-cola-platforms/egon-cola-platform-admin-portal/src/pages/PortalHomePage.tsx`

- Purpose: 提供统一首页的左侧平台菜单、摘要卡、局部重试和独立入口。
- Symbols: `PortalHomePage`；platform navigation/capability filter；summary query。
- Repository evidence: shared `EnterpriseLayout` already supports left navigation/collapse/drawer; Spec §12.1 defines platform-level host menu and SummaryCards。
- Dependencies and consumers: File 8 Facade, File 9 router, File 10 App, File 14 test, shared PageHeader/PageState/EnterpriseLayout。
- Why now: page composes already-defined contracts and does not own child business menus。
- Contract/signature changes: navigation item only contains platform key/label/path; summary card fields are aggregate status/count/display values, never domain rows/secrets。
- Input/output and state mapping: bootstrap/capability summary -> platform nav; scope -> Facade context; summary cards -> READY/PARTIAL/TIMEOUT/NOT_CONFIGURED; card retry -> adapter-specific refetch。
- Error and edge behavior: no capability hides platform; no child API success means card remains NOT_CONFIGURED; host error keeps standalone links; menu click only changes route intent。
- Standards impact: `MC-REUSE-001, MC-PATTERN-001, MC-SCOPE-001, MC-TEST-001` — reuse shared shell and Facade; no universal business table/global store。
- Literal rule enforcement: `Rule 11` — Portal UI page remains in frontend host package。
- Implementation pseudocode:

```tsx
const summary = useQuery({queryKey: ['portal-summary', scope], queryFn: ({signal}) => loadSummary({scope}, signal, adapters)})
const navigation = platformItems.filter((item) => capabilitySummary.canSee(item.requiredCapability))
return <EnterpriseLayout config={{platformName: 'Egon COLA Platform', navigation, footer: {version: PORTAL_VERSION}}}>
  <PageHeader title="平台工作台" subtitle="统一查看身份、权限、网关和配置平台状态" />
  <SummaryCards cards={summary.data?.cards ?? {}} onRetry={(key) => queryClient.invalidateQueries({queryKey: ['portal-summary', key]})} />
  <PlatformQuickLinks items={navigation} />
</EnterpriseLayout>
```

- Verification contribution: File 14 observes shell/menu/summary/partial behavior。
- After this file: host home is renderable with no Wujie child mounted by default。

#### File 14 — `CREATE egon-cola-platforms/egon-cola-platform-admin-portal/src/pages/PortalHomePage.test.tsx`

- Purpose: 固定首页菜单、summary partial、独立入口和 capability pruning。
- Symbols: `rendersPlatformNavigation`, `rendersPartialSummary`, `hidesUnauthorizedPlatform`。
- Repository evidence: shared EnterpriseLayout test pattern and Spec TEST-019/portal requirements。
- Dependencies and consumers: File 13 page, File 8 Facade mock, shared test setup, router/App。
- Why now: page RED contract before App integration。
- Contract/signature changes: no host business API; input adapter result -> cards; platform capability -> nav visibility。
- Input/output and state mapping: `idp/rbac3/gateway/ddc` -> left groups/cards; one timeout -> partial card and other cards intact; standalone URL visible on failure。
- Error and edge behavior: no unauthorized platform menu; no raw capability detail/secret; retry only targeted card。
- Standards impact: `MC-PATTERN-001, MC-SCOPE-001, MC-TEST-001` — verifies Facade and static/config-driven navigation。
- Literal rule enforcement: `Rule 11` — Portal page test only。
- Implementation pseudocode:

```tsx
it('renders static platform menu and partial summary', async () => {
  mockSummary({idp: readyCard(), gateway: timeoutCard(), rbac3: notConfiguredCard(), ddc: readyCard()})
  render(<PortalHomePage />, {wrapper: portalWrapper()})
  expect(screen.getByText('身份与安全')).toBeInTheDocument()
  expect(screen.getByText(/Gateway.*超时/)).toBeInTheDocument()
  expect(screen.getByRole('link', {name: /独立打开 Gateway/})).toBeInTheDocument()
})

it('hides a platform without host-level capability', () => {
  render(<PortalHomePage />, {wrapper: portalWrapper({allowed: ['idp']})})
  expect(screen.queryByText('配置中心')).not.toBeInTheDocument()
})
```

- Verification contribution: fails against missing Portal shell/page and proves homepage acceptance。
- After this file: Portal Home test is RED until File 13/App/provider are wired。

#### File 15 — `CREATE egon-cola-platforms/egon-cola-platform-admin-portal/src/pages/ChildRoutePage.tsx`

- Purpose: 负责单个 child Manifest load、Wujie mount、局部错误和 standalone fallback。
- Symbols: `ChildRoutePage`；selected platform/route state；Manifest query/lifecycle state。
- Repository evidence: Spec §7.3.1/§7.3.4 and TEST-015/016; no current host route。
- Dependencies and consumers: Files 1/2/4/6/11/12/16; router File 9。
- Why now: composes manifest/adapter/state after contracts, before Portal entry。
- Contract/signature changes: path platformKey/routeIntent is display/navigation context; child Manifest controls URL/version; no host-owned business API call except approved summary Facade。
- Input/output and state mapping: params -> load manifest; manifest -> WujieChild; lifecycle -> PageState/error panel; standaloneUrl -> link; retry -> cleanup then reload。
- Error and edge behavior: manifest timeout/version mismatch never mounts; child crash isolates DOM subtree; route refresh preserves intent; other platform cards/menu remain available。
- Standards impact: `MC-DEP-001, MC-PATTERN-001, MC-SCOPE-001, MC-TEST-001` — Adapter/State composition and Wujie dependency are isolated here。
- Literal rule enforcement: `Rule 11` — frontend Portal page/host boundary only。
- Implementation pseudocode:

```tsx
const {platformKey = 'idp'} = useParams()
const manifest = useQuery({queryKey: ['manifest', platformKey, environment], queryFn: ({signal}) => loadManifest(platformKey as PlatformKey, environment, signal)})
if (manifest.isPending) return <PageState loading error={null} empty={false}>{null}</PageState>
if (manifest.error) return <MountFailure error={manifest.error} onRetry={() => void manifest.refetch()} standaloneUrl={manifest.data?.standaloneUrl} />
return <ChildBoundary state={lifecycle} onRetry={remount}><WujieChild manifest={manifest.data} context={safeRouteContext} onState={dispatchLifecycle} /></ChildBoundary>
```

- Verification contribution: File 16 asserts manifest/mount/error/standalone behavior。
- After this file: child route page is ready to mount a mocked Adapter。

#### File 16 — `CREATE egon-cola-platforms/egon-cola-platform-admin-portal/src/pages/ChildRoutePage.test.tsx`

- Purpose: 固定 child mount failure/cleanup/remount/deep-link behavior。
- Symbols: `rendersManifestFailure`, `isolatesChildCrash`, `cleansUpBeforeRemount`, `preservesStandaloneLink`。
- Repository evidence: Spec TEST-015/016/024 and Wujie lifecycle requirements；no current host test。
- Dependencies and consumers: File 15 page, mocked loader/WujieChild, lifecycle/bridge tests。
- Why now: child page behavior must be proven before package entry and child app changes。
- Contract/signature changes: loader failure prevents Wujie call; after mount child error only replaces child content; retry calls cleanup then load/mount once。
- Input/output and state mapping: route -> manifest -> lifecycle; error -> local action; standalone URL remains clickable; unrelated host nav remains rendered。
- Error and edge behavior: invalid version, timeout, runtime crash, 404 child asset have distinct copy; no token appears in props/mock event.
- Standards impact: `MC-DEP-001, MC-PATTERN-001, MC-SCOPE-001, MC-TEST-001` — tests mock only Adapter boundary and assert isolated state transitions。
- Literal rule enforcement: `Rule 11` — Portal host page test only。
- Implementation pseudocode:

```tsx
it('does not mount an incompatible child and offers standalone entry', async () => {
  mockManifestRejects('MANIFEST_VERSION_UNSUPPORTED')
  render(<ChildRoutePage />, {wrapper: portalRouteWrapper('/platform/gateway/dashboard')})
  expect(await screen.findByText(/版本不兼容/)).toBeInTheDocument()
  expect(screen.getByRole('link', {name: /独立打开/})).toBeInTheDocument()
  expect(wujieMock.start).not.toHaveBeenCalled()
})

it('isolates a child crash and remounts after cleanup', async () => {
  render(<ChildRoutePage />, {wrapper: portalRouteWrapper('/platform/idp/users')})
  await screen.findByText(/子应用已加载/)
  triggerChildCrash()
  await userEvent.click(screen.getByRole('button', {name: '重新挂载'}))
  expect(wujieMock.destroy).toHaveBeenCalledBefore(wujieMock.start)
  expect(screen.getByText(/平台工作台/)).toBeInTheDocument()
})
```

- Verification contribution: provides host integration proof without starting browser/service。
- After this file: ChildRoute behavior is contract-tested and ready for Wujie package wiring。

#### File 17 — `CREATE egon-cola-platforms/egon-cola-platform-admin-portal/src/test/setup.ts`

- Purpose: 为 Portal jsdom/Vitest 提供 matchMedia/ResizeObserver/Wujie wrapper mock setup。
- Symbols: setup globals and test-safe Wujie adapter mock。
- Repository evidence: shared and child packages already provide matchMedia/ResizeObserver setup files; Portal uses AntD/shared layout。
- Dependencies and consumers: all Portal tests; no production import。
- Why now: test files in Steps 6 require stable jsdom primitives before Portal package scripts execute。
- Contract/signature changes: mock only DOM/runtime boundary; Wujie mock exposes start/destroy hooks observable by tests, not production API.
- Input/output and state mapping: jsdom -> deterministic layout/test events; no auth/network side effect unless test explicitly mocks fetch。
- Error and edge behavior: ResizeObserver cleanup is no-op; fetch remains test-mocked; no credentials are generated or stored。
- Standards impact: `MC-SCOPE-001, MC-TEST-001` — test infrastructure follows existing packages and is not bundled into production。
- Literal rule enforcement: `Rule 11` — test setup under Portal frontend package。
- Implementation pseudocode:

```ts
import '@testing-library/jest-dom/vitest'
Object.defineProperty(window, 'matchMedia', {writable: true, value: (query: string) => ({matches: false, media: query, addEventListener() {}, removeEventListener() {}})})
class ResizeObserverStub { observe() {} unobserve() {} disconnect() {} }
Object.defineProperty(window, 'ResizeObserver', {writable: true, value: ResizeObserverStub})
vi.mock('wujie-react', () => ({default: MockWujieReact}))
```

- Verification contribution: makes all Portal component tests deterministic and avoids actual child network/mount。
- After this file: Portal test environment is ready。

#### File 18 — `CREATE egon-cola-platforms/egon-cola-platform-admin-portal/src/main.tsx`

- Purpose: 建立 Portal standalone Vite entry and shared provider bootstrap。
- Symbols: `main` createRoot call, `injectTokens`, `initI18n`。
- Repository evidence: IDP/RBAC3/Gateway/DDC main files all use createRoot/StrictMode/shared theme/i18n; Portal must follow same package style。
- Dependencies and consumers: File 10 App, shared package, index.html。
- Why now: entry is last among Portal app composition and test setup contracts。
- Contract/signature changes: `createRoot(document.getElementById('root')!)`; no token bootstrap in host; auth is same-site child/bootstrap behavior。
- Input/output and state mapping: DOM root -> App; theme/i18n -> common UI; no business data ownership。
- Error and edge behavior: missing root fails build/test; host error boundary handles render failure; no localStorage token extraction。
- Standards impact: `MC-REUSE-001, MC-SCOPE-001, MC-TEST-001` — reuse shared tokens/i18n and existing React entry convention。
- Literal rule enforcement: `Rule 11` — Portal standalone frontend entry only。
- Implementation pseudocode:

```tsx
injectTokens()
initI18n({defaultNS: 'common', resources: {'zh-CN': {}}})
createRoot(document.getElementById('root')!).render(
  <StrictMode><I18nProvider><AdminThemeProvider><App /></AdminThemeProvider></I18nProvider></StrictMode>,
)
```

- Verification contribution: Portal build uses exact root/providers and no runtime start is performed in this task。
- After this file: standalone Portal entry is wired。

#### File 19 — `CREATE egon-cola-platforms/egon-cola-platform-admin-portal/index.html`

- Purpose: 提供 Portal Vite HTML mount root。
- Symbols: `#root` and module script `/src/main.tsx`。
- Repository evidence: existing Vite packages require an index entry; no Portal HTML exists。
- Dependencies and consumers: File 18 main, Vite build。
- Why now: entry file needs a concrete root for build validation。
- Contract/signature changes: no inline credential/config payload; title/charset/viewport only。
- Input/output and state mapping: browser document -> root node -> React App。
- Error and edge behavior: no redirect script; standalone URL is handled by router/manifest not HTML。
- Standards impact: `MC-SCOPE-001, MC-TEST-001` — static HTML only, no dependency or auth logic。
- Literal rule enforcement: `Rule 11` — Portal frontend package root。
- Implementation pseudocode:

```html
<!doctype html>
<html lang="zh-CN"><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1.0"><title>Egon COLA Platform</title></head><body>
<main id="root"></main>
<script type="module" src="/src/main.tsx"></script>
</body></html>
```

- Verification contribution: Vite build verifies HTML/module resolution and no inline secret path。
- After this file: HTML entry is available for Portal build。

#### File 20 — `CREATE egon-cola-platforms/egon-cola-platform-admin-portal/vite.config.ts`

- Purpose: 建立 Portal React/Vite/Vitest configuration and same-site API/Manifest boundary。
- Symbols: `defineConfig` with React plugin, test setup, build and server proxy。
- Repository evidence: existing Vite configs use `@vitejs/plugin-react`, shared favicon plugin, jsdom test, local proxy ports。
- Dependencies and consumers: package.json scripts, source/test files; package `wujie-react` resolves through npm。
- Why now: package dependency/config is required before typecheck/build; no runtime service is started。
- Contract/signature changes: `server.proxy` routes `/api`/`/portal-manifest` only to configured same-site gateway during local development; no auth header injection; `build.sourcemap`/chunk limit follows existing style。
- Input/output and state mapping: Vite env -> child URLs/manifest environment; test config -> jsdom/setup; build -> standalone artifact。
- Error and edge behavior: missing proxy target is a user startup concern; build must not embed secrets; manifest URLs remain validated by loader。
- Standards impact: `MC-DEP-001, MC-REUSE-001, MC-SCOPE-001, MC-TEST-001` — reuse existing Vite/React/Vitest versions; only approved Wujie runtime added。
- Literal rule enforcement: `Rule 11` — Portal Vite configuration, no Java/Spring config。
- Implementation pseudocode:

```ts
export default defineConfig({
  plugins: [react()],
  server: {proxy: {'/api': {target: process.env.PORTAL_API_PROXY ?? 'http://127.0.0.1:18100'}, '/portal-manifest': {target: process.env.PORTAL_MANIFEST_PROXY ?? 'http://127.0.0.1:18100'}}},
  build: {sourcemap: false, chunkSizeWarningLimit: 900},
  test: {environment: 'jsdom', setupFiles: './src/test/setup.ts', exclude: ['node_modules/**', 'dist/**']},
})
```

- Verification contribution: Portal typecheck/test/build use the exact configured dependency/toolchain。
- After this file: Vite can resolve source/test entry; package lock/config still needs package metadata。

#### File 21 — `CREATE egon-cola-platforms/egon-cola-platform-admin-portal/tsconfig.json`

- Purpose: 固定 Portal strict TypeScript compile boundary。
- Symbols: `compilerOptions`/`include` JSON。
- Repository evidence: existing shared/child tsconfigs use strict, bundler resolution, React JSX, noUnusedLocals/Parameters。
- Dependencies and consumers: Vite/typecheck scripts and all `src` files。
- Why now: exact compile gate for new package。
- Contract/signature changes: ES2024/DOM, ESNext/bundler, `jsx: react-jsx`, strict/noUnused, skipLibCheck; no path alias to child internal modules。
- Input/output and state mapping: source TS/TSX -> typecheck; no runtime state。
- Error and edge behavior: unused imports/implicit any fail; no `any` escape for bridge/manifest without a typed boundary。
- Standards impact: `MC-SCOPE-001, MC-TEST-001` — mirror repository TypeScript strictness。
- Literal rule enforcement: `Rule 11` — Portal frontend config only。
- Implementation pseudocode:

```json
{
  "compilerOptions": {"target": "ES2024", "lib": ["ES2024", "DOM", "DOM.Iterable"], "module": "ESNext", "moduleResolution": "bundler", "jsx": "react-jsx", "strict": true, "noUnusedLocals": true, "noUnusedParameters": true, "verbatimModuleSyntax": true, "skipLibCheck": true},
  "include": ["src", "vite.config.ts"]
}
```

- Verification contribution: typecheck gate catches manifest/bridge/Wujie prop mismatch before build。
- After this file: strict compiler boundary is defined。

#### File 22 — `CREATE egon-cola-platforms/egon-cola-platform-admin-portal/package.json`

- Purpose: 声明 Portal package scripts/dependency and exact Wujie wrapper version。
- Symbols: scripts `dev/build/typecheck/test/lint`; dependencies `wujie-react`.
- Repository evidence: four Web package manifests use React 19/Vite 8/Vitest and shared package; no microfrontend dependency exists (EVD-004)。
- Dependencies and consumers: Files 20/21/23; npm lockfile File 23; source imports WujieReact only from this package。
- Why now: package metadata is the dependency gate before installing/wiring Wujie。
- Contract/signature changes: `wujie-react: "2.1.0"` exact; use existing workspace React/ReactDOM/Router/AntD/Query ranges; no `wujie` direct duplicate unless package resolution proves wrapper requires it。
- Input/output and state mapping: npm scripts -> static checks/build; package dependency -> bundled host runtime only。
- Error and edge behavior: install uses `--ignore-scripts` for lockfile first; license/security/version diff reviewed before code commit; no dependency in child packages。
- Standards impact: `MC-DEP-001, MC-REUSE-001, MC-SCOPE-001, MC-TEST-001` — approved runtime is the only new dependency; existing stack reused。
- Literal rule enforcement: `Rule 11` — package is a frontend host package, not a Java module。
- Implementation pseudocode:

```json
{
  "name": "@egon-cola/platform-admin-portal",
  "private": true,
  "type": "module",
  "scripts": {"dev": "vite", "build": "tsc -p tsconfig.json && vite build", "typecheck": "tsc -p tsconfig.json --pretty false", "test": "vitest run", "lint": "eslint ."},
  "dependencies": {"wujie-react": "2.1.0", "@egon-cola/admin-web-shared": "^0.2.0", "@tanstack/react-query": "^5.101.4", "antd": "^6.5.2", "react": "^19.2.8", "react-dom": "^19.2.8", "react-router-dom": "^7.18.0"}
}
```

- Verification contribution: dependency diff and package scripts are explicit; lockfile generation remains before host source is committed。
- After this file: package manifest is ready for lockfile generation and version/license review。

#### File 23 — `GENERATED egon-cola-platforms/egon-cola-platform-admin-portal/package-lock.json`

- Purpose: 锁定 Portal dependency graph for reproducible host build。
- Symbols: npm lockfile root/dependency entries for `wujie-react@2.1.0` and transitive packages。
- Repository evidence: existing Web packages use npm lockfiles/root hoisted packages; Portal has no current lockfile。
- Dependencies and consumers: File 22 package manifest; CI/install/build; no source import directly consumes lockfile。
- Why now: generated dependency output must be created after manifest and before Wujie source import validation。
- Contract/signature changes: generated only by exact npm command; no hand-edit; root package/version must match File 22。
- Input/output and state mapping: package.json -> lock graph; no runtime business state。
- Error and edge behavior: installation failure or unexpected major transitive version returns to dependency review; package-lock must not rewrite other package locks。
- Standards impact: `MC-DEP-001, MC-REUSE-001, MC-SCOPE-001, MC-TEST-001` — generated dependency proof and path-limited diff; no unrelated lockfile mutation。
- Literal rule enforcement: `Rule 11` — generated file belongs to Portal frontend package。
- Implementation pseudocode:

```text
cd egon-cola-platforms/egon-cola-platform-admin-portal
npm install --package-lock-only --ignore-scripts --save-exact wujie-react@2.1.0
parse package-lock.json and assert packages['node_modules/wujie-react'].version == '2.1.0'
assert no IDP/RBAC3/Gateway/DDC package.json or lockfile changed
```

- Verification contribution: exact version/reproducibility gate before Wujie host implementation。
- After this file: dependency graph is reproducible and reviewed; source can import `wujie-react`。

#### File 24 — `MODIFY egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-admin-web/src/main.tsx`

- Purpose: 让 IDP 作为 Wujie child 暴露 mount/unmount，同时保留 standalone entry。
- Symbols: `window.__POWERED_BY_WUJIE__`, `window.__WUJIE_MOUNT`, `window.__WUJIE_UNMOUNT`, `mount` helper。
- Repository evidence: current file direct `createRoot`; Wujie official Vite guidance uses these globals and `window.__WUJIE.mount()`。
- Dependencies and consumers: Portal WujieChild; IDP App/Auth/Router; React root lifecycle。
- Why now: child lifecycle wiring follows host package/contract and must be isolated per child package。
- Contract/signature changes: standalone branch keeps existing `StrictMode/I18n/AdminTheme/App`; Wujie branch stores root and unmounts it exactly once; embedded flag derives from `$wujie.props`.
- Input/output and state mapping: host lifecycle -> React root mount/unmount; props -> App embedded presentation only; auth/bootstrap remains child-owned same-site request。
- Error and edge behavior: repeated mount no duplicate root; unmount removes root; no token extraction from props; startup failures remain child boundary error。
- Standards impact: `MC-REUSE-001, MC-SCOPE-001, MC-TEST-001` — reuse current entry/providers; Wujie types are local global declarations, no child dependency addition。
- Literal rule enforcement: `Rule 11` — current IDP Web entry package only。
- Implementation pseudocode:

```tsx
let root: Root | undefined
const mount = () => { root ??= createRoot(document.getElementById('root')!); root.render(<StrictMode><I18nProvider><AdminThemeProvider><App embedded /></AdminThemeProvider></I18nProvider></StrictMode>) }
if (window.__POWERED_BY_WUJIE__) { window.__WUJIE_MOUNT = mount; window.__WUJIE_UNMOUNT = () => { root?.unmount(); root = undefined }; window.__WUJIE?.mount() } else { mountStandalone() }
```

- Verification contribution: child build/typecheck and Portal mocked lifecycle test observe mount/unmount contract。
- After this file: IDP child entry supports both modes; App/router/layout files remain for embedded shell branch。

#### File 25 — `MODIFY egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-admin-web/src/app/App.tsx`

- Purpose: 将 embedded presentation flag 传入 IDP Router，保留 Query/BrowserRouter/Auth provider 顺序。
- Symbols: `App({embedded?: boolean})`。
- Repository evidence: current App owns QueryClientProvider/BrowserRouter and AppRouter; no prop currently。
- Dependencies and consumers: File 24 main; File 26 router; standalone callers use `<App />` unchanged。
- Why now: lifecycle entry needs a typed way to hide duplicate shell。
- Contract/signature changes: optional `embedded` prop defaults false; no API/auth change。
- Input/output and state mapping: embedded -> router/layout presentation; QueryClient/Auth remain child-owned and independent。
- Error and edge behavior: standalone default false; no host context treated as permission; AppErrorBoundary remains outer child boundary。
- Standards impact: `MC-REUSE-001, MC-SCOPE-001, MC-TEST-001` — preserve provider order and existing API clients。
- Literal rule enforcement: `Rule 11` — IDP Web app file only。
- Implementation pseudocode:

```tsx
export const App = ({embedded = false}: {readonly embedded?: boolean}) => (
  <QueryClientProvider client={queryClient}>
    <BrowserRouter><AppRouter embedded={embedded} /></BrowserRouter>
  </QueryClientProvider>
)
```

- Verification contribution: IDP typecheck confirms standalone call sites compile and embedded prop reaches Router。
- After this file: Router can select embedded layout branch。

#### File 26 — `MODIFY egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-admin-web/src/app/router.tsx`

- Purpose: 让 IDP embedded mode 隐藏重复全局壳但保留 auth/route guards。
- Symbols: `AppRouter({embedded?: boolean})`, `ConsoleGuard`/`AdminLayout` call。
- Repository evidence: current Router owns AuthProvider, lazy pages and ConsoleGuard with AdminLayout wrapper。
- Dependencies and consumers: File 25 App, File 27 AdminLayout, Portal child lifecycle。
- Why now: presentation branch follows App prop and must not move auth ownership。
- Contract/signature changes: embedded is visual only; `ConsoleGuard` still redirects `/login` and route permissions unchanged。
- Input/output and state mapping: auth bootstrap -> same routes/menu; embedded -> `AdminLayout embedded` branch; child data/query unchanged。
- Error and edge behavior: 401 still login; 403/unknown route unchanged; no host route intent used as backend permission。
- Standards impact: `MC-REUSE-001, MC-SCOPE-001, MC-TEST-001` — retain current AuthProvider/lazy route pattern。
- Literal rule enforcement: `Rule 11` — IDP Web routing remains current frontend structure。
- Implementation pseudocode:

```tsx
export const AppRouter = ({embedded = false}: {readonly embedded?: boolean}) => (
  <AppErrorBoundary><AuthProvider><Suspense fallback={<PageFallback />}><Routes>
    <Route path="/login" element={<LoginRoute />} />
    <Route element={<ConsoleGuard embedded={embedded} />}><Route index element={<Navigate to="/overview" replace />} />{allConsoleRoutes}</Route>
  </Routes></Suspense></AuthProvider></AppErrorBoundary>
)
```

- Verification contribution: existing App test and child lifecycle test verify route behavior remains unchanged while shell differs。
- After this file: embedded flag reaches AdminLayout。

#### File 27 — `MODIFY egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-admin-web/src/app/AdminLayout.tsx`

- Purpose: 在 embedded mode 不重复渲染 Header/Sidebar，standalone 模式保持当前左侧菜单。
- Symbols: `AdminLayout({children, embedded?: boolean})`。
- Repository evidence: current AdminLayout creates platform navigation from permissions and always returns EnterpriseLayout。
- Dependencies and consumers: File 26 Router; shared EnterpriseLayout; Portal child mode。
- Why now: final IDP embedded presentation boundary after entry/router plumbing。
- Contract/signature changes: optional embedded prop; `embedded ? <>{children}</> : <EnterpriseLayout ...>`; navigation construction remains current static/capability logic。
- Input/output and state mapping: permission bootstrap -> child navigation only standalone; child route content -> host content in embedded。
- Error and edge behavior: no menu duplication; auth user/actions still exist for standalone; embedded logout remains child-owned via child route/app controls。
- Standards impact: `MC-REUSE-001, MC-SCOPE-001, MC-TEST-001` — reuse current navigation filtering and shared shell; no global host menu copied。
- Literal rule enforcement: `Rule 11` — existing IDP frontend layout file。
- Implementation pseudocode:

```tsx
export const AdminLayout = ({children, embedded = false}: PropsWithChildren<{readonly embedded?: boolean}>) => {
  const layout = <EnterpriseLayout config={config}>{children}</EnterpriseLayout>
  return embedded ? <>{children}</> : layout
}
```

- Verification contribution: standalone App tests continue to find menu; embedded integration observes no second `主菜单`。
- After this file: IDP child can render in host without duplicate shell。

#### File 28 — `MODIFY egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin-web/src/main.tsx`

- Purpose: 暴露 RBAC3 Wujie mount/unmount while retaining Rbac3 SDK provider tree。
- Symbols: same Wujie globals and root lifecycle as IDP, with `App embedded`。
- Repository evidence: current direct createRoot and provider composition in `App.tsx`; Wujie Vite lifecycle convention from Spec EVD-025。
- Dependencies and consumers: Portal adapter; RBAC3 App/ApplicationRouter/AdminLayout。
- Why now: each child must independently satisfy host lifecycle before the Portal can claim four-child support。
- Contract/signature changes: no SDK/auth path changes; embedded flag is presentation-only。
- Input/output and state mapping: Wujie lifecycle -> root; host props -> embedded; Rbac3 SDK still fetches its own about/bootstrap and capability。
- Error and edge behavior: root cleanup idempotent; no host permission treated as RBAC3 `about`; standalone branch unchanged。
- Standards impact: `MC-REUSE-001, MC-SCOPE-001, MC-TEST-001` — preserve provider order and SDK boundary。
- Literal rule enforcement: `Rule 11` — RBAC3 Web entry only。
- Implementation pseudocode:

```tsx
let root: Root | undefined
const mount = () => { root ??= createRoot(document.getElementById('root')!); root.render(<StrictMode><I18nProvider><AdminThemeProvider><App embedded /></AdminThemeProvider></I18nProvider></StrictMode>) }
if (window.__POWERED_BY_WUJIE__) { window.__WUJIE_MOUNT = mount; window.__WUJIE_UNMOUNT = () => { root?.unmount(); root = undefined }; window.__WUJIE?.mount() } else { mountStandalone() }
```

- Verification contribution: RBAC3 package typecheck/build and host mock lifecycle gate。
- After this file: RBAC3 entry supports both modes。

#### File 29 — `MODIFY egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin-web/src/app/App.tsx`

- Purpose: 将 embedded prop through Rbac3 provider tree without changing SDK/client construction。
- Symbols: `App({embedded?: boolean})`。
- Repository evidence: current App owns QueryClientProvider/Rbac3Provider/FeatureApiProvider/BrowserRouter/AuthenticationShell。
- Dependencies and consumers: File 28 main, File 30 router。
- Why now: entry needs typed embedded flow into local AdminLayout。
- Contract/signature changes: optional embedded prop; provider order remains Query -> Rbac3 -> FeatureApi -> BrowserRouter -> ErrorBoundary -> Auth shell。
- Input/output and state mapping: embedded -> ApplicationRouter/AdminLayout presentation; about/permissions remain SDK-owned。
- Error and edge behavior: no host fallback for SDK/auth failure; standalone default false。
- Standards impact: `MC-REUSE-001, MC-SCOPE-001, MC-TEST-001` — no provider replacement or new state store。
- Literal rule enforcement: `Rule 11` — RBAC3 Web app file only。
- Implementation pseudocode:

```tsx
export const App = ({embedded = false}: {readonly embedded?: boolean}) => (
  <QueryClientProvider client={queryClient}><Rbac3Provider client={clients.rbac3Client}><FeatureApiProvider client={clients.featureClient}>
    <BrowserRouter><AppErrorBoundary><AuthenticationShell><ApplicationRouter embedded={embedded} /></AuthenticationShell></AppErrorBoundary></BrowserRouter>
  </FeatureApiProvider></Rbac3Provider></QueryClientProvider>
)
```

- Verification contribution: integration tests verify provider/auth route behavior remains same in standalone default。
- After this file: ApplicationRouter accepts embedded state。

#### File 30 — `MODIFY egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin-web/src/app/router.tsx`

- Purpose: embedded branch for local RBAC3 AdminLayout without weakening SDK RouteAccessGuard。
- Symbols: `ApplicationRouter({embedded?: boolean})`, local `AdminLayout`。
- Repository evidence: current router uses `useRbac3Authorization`, visibleNavigation and `RouteAccessGuard`; local AdminLayout always EnterpriseLayout。
- Dependencies and consumers: File 29 App; existing route descriptors/navigation; File 31 not needed because local layout is same file。
- Why now: presentation branch after prop wiring, preserving authorization source。
- Contract/signature changes: add optional prop and pass to local layout; `about`/`isRouteAllowed` unchanged。
- Input/output and state mapping: SDK about -> visible child nav standalone; embedded -> no duplicate shell; route components unchanged。
- Error and edge behavior: no about -> null as current; unauthorized route -> 403; host context never grants RBAC3 access。
- Standards impact: `MC-REUSE-001, MC-SCOPE-001, MC-TEST-001` — reuse SDK guards/navigation and shared shell only in standalone。
- Literal rule enforcement: `Rule 11` — existing RBAC3 Web app/router structure。
- Implementation pseudocode:

```tsx
export const ApplicationRouter = ({embedded = false}: {readonly embedded?: boolean}) => {
  const {about} = useRbac3Authorization()
  if (!about) return null
  return <AdminLayout embedded={embedded}><Routes>{applicationRouteDescriptors.map(renderGuardedRoute)}<Route path="*" element={fallback} /></Routes></AdminLayout>
}
const AdminLayout = ({children, embedded = false}: PropsWithChildren<{readonly embedded?: boolean}>) => embedded ? <>{children}</> : <EnterpriseLayout config={buildConfig()}>{children}</EnterpriseLayout>
```

- Verification contribution: RBAC3 integration test observes no duplicate shell and same permission denial。
- After this file: RBAC3 child presentation supports embedded mode。

#### File 31 — `MODIFY egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web/src/main.tsx`

- Purpose: 暴露 Gateway Wujie mount/unmount lifecycle。
- Symbols: root lifecycle globals and standalone/embedded `App` render。
- Repository evidence: current direct createRoot; App owns QueryClient/Auth/Router providers。
- Dependencies and consumers: Portal WujieChild; Gateway App/AdminLayout。
- Why now: Gateway child must be lifecycle-safe before host integration。
- Contract/signature changes: no route/API change; `$wujie.props.embedded` only layout presentation。
- Input/output and state mapping: root -> App; lifecycle cleanup -> no lingering query observers/listeners; auth remains child-owned。
- Error and edge behavior: unmount clears root; no raw host context in auth client; standalone branch unchanged。
- Standards impact: `MC-REUSE-001, MC-SCOPE-001, MC-TEST-001` — preserve current providers/client and add no child dependency。
- Literal rule enforcement: `Rule 11` — Gateway Web entry only。
- Implementation pseudocode:

```tsx
let root: Root | undefined
const mount = () => { root ??= createRoot(document.getElementById('root')!); root.render(<StrictMode><I18nProvider><AdminThemeProvider><App /></AdminThemeProvider></I18nProvider></StrictMode>) }
if (window.__POWERED_BY_WUJIE__) { window.__WUJIE_MOUNT = mount; window.__WUJIE_UNMOUNT = () => { root?.unmount(); root = undefined }; window.__WUJIE?.mount() } else { mount() }
```

- Verification contribution: host lifecycle mock can mount/unmount Gateway without starting its API。
- After this file: Gateway entry lifecycle is exposed。

#### File 32 — `MODIFY egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web/src/layouts/AdminLayout.tsx`

- Purpose: embedded Gateway child hides duplicate shell while preserving standalone nav/capabilities。
- Symbols: `AdminLayout` embedded detection and return branch。
- Repository evidence: current layout builds capability-filtered navigation and always renders EnterpriseLayout/Outlet。
- Dependencies and consumers: Gateway App router, Wujie props global, shared shell。
- Why now: layout branch is the final Gateway child presentation change after entry lifecycle。
- Contract/signature changes: read `window.$wujie?.props?.embedded === true`; no new host permission/auth API。
- Input/output and state mapping: capability/auth -> existing nav in standalone; embedded -> Outlet content; query/client/action state unchanged。
- Error and edge behavior: no `$wujie` -> standalone; host props missing -> safe default; no child menu copied into host。
- Standards impact: `MC-REUSE-001, MC-SCOPE-001, MC-TEST-001` — reuse existing capability filtering and EnterpriseLayout.
- Literal rule enforcement: `Rule 11` — Gateway Web layout only。
- Implementation pseudocode:

```tsx
const embedded = window.$wujie?.props?.embedded === true
const content = <Outlet />
if (embedded) return content
return <EnterpriseLayout config={config}>{content}</EnterpriseLayout>
```

- Verification contribution: Gateway layout test/build confirms standalone menu unchanged and embedded has no duplicate navigation。
- After this file: Gateway child is host-presentable。

#### File 33 — `MODIFY egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin-web/src/main.tsx`

- Purpose: 暴露 DDC Wujie lifecycle while preserving QueryClient/Antd providers。
- Symbols: root lifecycle globals; existing provider composition inside mount。
- Repository evidence: current main renders I18n/Theme/QueryClient/Antd/App directly; no lifecycle hooks。
- Dependencies and consumers: Portal host; DDC App/AdminLayout。
- Why now: DDC is the fourth child lifecycle slice and must not be mounted through an iframe-only shortcut。
- Contract/signature changes: preserve provider order; unmount root and query observers with React root cleanup; no DDC client path change。
- Input/output and state mapping: lifecycle -> root; child props -> embedded display only; DDC auth/client remains same-site child-owned。
- Error and edge behavior: no duplicate root; standalone branch exact current behavior; provider cleanup occurs before remount。
- Standards impact: `MC-REUSE-001, MC-SCOPE-001, MC-TEST-001` — reuse current providers and no child Wujie dependency。
- Literal rule enforcement: `Rule 11` — DDC Web entry only。
- Implementation pseudocode:

```tsx
let root: Root | undefined
const renderApp = (embedded = false) => { root ??= createRoot(document.getElementById('root')!); root.render(<StrictMode><I18nProvider><AdminThemeProvider><QueryClientProvider client={queryClient}><AntdApp><App /></AntdApp></QueryClientProvider></AdminThemeProvider></I18nProvider></StrictMode>) }
if (window.__POWERED_BY_WUJIE__) { window.__WUJIE_MOUNT = () => renderApp(true); window.__WUJIE_UNMOUNT = () => { root?.unmount(); root = undefined }; window.__WUJIE?.mount() } else { renderApp(false) }
```

- Verification contribution: DDC typecheck/build and Portal lifecycle mock see provider-safe mount/unmount。
- After this file: fourth child entry lifecycle exists。

#### File 34 — `MODIFY egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin-web/src/layouts/AdminLayout.tsx`

- Purpose: embedded DDC child hides duplicate EnterpriseLayout while standalone keeps current static menu。
- Symbols: `AdminLayout` embedded branch。
- Repository evidence: current DDC layout constructs static navigation and always returns EnterpriseLayout with Outlet。
- Dependencies and consumers: File 33 main, DDC App routes, shared layout。
- Why now: final child layout branch for four-app host contract。
- Contract/signature changes: `embedded` read from `$wujie.props`; no navigation/API ownership change; instances route remains available in standalone and host child。
- Input/output and state mapping: DDC identity/actions -> standalone config; embedded -> Outlet only; page QueryClient remains child-owned。
- Error and edge behavior: absent global defaults false; no static menu database call; unauthorized/API errors stay page-owned。
- Standards impact: `MC-REUSE-001, MC-SCOPE-001, MC-TEST-001` — reuse current DDC config and shared layout。
- Literal rule enforcement: `Rule 11` — DDC Web layout only。
- Implementation pseudocode:

```tsx
const embedded = window.$wujie?.props?.embedded === true
const content = <Outlet />
return embedded ? content : <EnterpriseLayout config={config}>{content}</EnterpriseLayout>
```

- Verification contribution: DDC App/Layout tests observe standalone navigation; Portal child test observes no duplicate global shell。
- After this file: all four child entry/layout slices are lifecycle-ready。

- Validation working directory: `/Users/mario/SelfProject/Egon-COLA/egon-cola-platforms/egon-cola-platform-admin-portal` plus each child package for child typecheck/build。
- Verification command: `npm install --package-lock-only --ignore-scripts --save-exact wujie-react@2.1.0 && npm run test -- --run src/manifest/loader.test.ts src/lifecycle/lifecycleState.test.ts src/bridge/context.test.ts src/summary/platformSummaryFacade.test.ts src/pages/PortalHomePage.test.tsx src/pages/ChildRoutePage.test.tsx && npm run typecheck && npm run build`
- Child verification commands: run `npm run typecheck && npm run build` in each IDP/RBAC3/Gateway/DDC Admin Web package; no service/browser is started by this Plan。
- Expected result: lockfile has exact `wujie-react@2.1.0`; all Portal contract/lifecycle/bridge/partial tests pass; Portal build and all four standalone child builds/typechecks exit 0; no child package gains Wujie dependency。
- Failure returns to: Files 1-8 for Manifest/State/bridge/Facade contract; Files 9-23 for Portal routing/package/provider/build; Files 24-34 for individual child lifecycle/layout。
- Completion criteria: Wujie Adapter is the sole direct runtime import, lifecycle cleanup is testable, bridge is allow-listed, homepage partial is explicit, child standalone path remains, and package/child path scopes are clean。
- Rollback: first remove Portal package/lockfile and its exact source/test paths; if child lifecycle is deployed separately, revert only the four child entry/layout groups to standalone root rendering. No backend/database rollback。
- Commit paths: `egon-cola-platforms/egon-cola-platform-admin-portal/package.json`, `egon-cola-platforms/egon-cola-platform-admin-portal/package-lock.json`, `egon-cola-platforms/egon-cola-platform-admin-portal/index.html`, `egon-cola-platforms/egon-cola-platform-admin-portal/vite.config.ts`, `egon-cola-platforms/egon-cola-platform-admin-portal/tsconfig.json`, `egon-cola-platforms/egon-cola-platform-admin-portal/src/manifest/loader.test.ts`, `egon-cola-platforms/egon-cola-platform-admin-portal/src/manifest/loader.ts`, `egon-cola-platforms/egon-cola-platform-admin-portal/src/lifecycle/lifecycleState.test.ts`, `egon-cola-platforms/egon-cola-platform-admin-portal/src/lifecycle/lifecycleState.ts`, `egon-cola-platforms/egon-cola-platform-admin-portal/src/bridge/context.test.ts`, `egon-cola-platforms/egon-cola-platform-admin-portal/src/bridge/context.ts`, `egon-cola-platforms/egon-cola-platform-admin-portal/src/summary/platformSummaryFacade.test.ts`, `egon-cola-platforms/egon-cola-platform-admin-portal/src/summary/platformSummaryFacade.ts`, `egon-cola-platforms/egon-cola-platform-admin-portal/src/app/router.tsx`, `egon-cola-platforms/egon-cola-platform-admin-portal/src/app/App.tsx`, `egon-cola-platforms/egon-cola-platform-admin-portal/src/manifest/types.ts`, `egon-cola-platforms/egon-cola-platform-admin-portal/src/lifecycle/WujieChild.tsx`, `egon-cola-platforms/egon-cola-platform-admin-portal/src/pages/PortalHomePage.tsx`, `egon-cola-platforms/egon-cola-platform-admin-portal/src/pages/PortalHomePage.test.tsx`, `egon-cola-platforms/egon-cola-platform-admin-portal/src/pages/ChildRoutePage.tsx`, `egon-cola-platforms/egon-cola-platform-admin-portal/src/pages/ChildRoutePage.test.tsx`, `egon-cola-platforms/egon-cola-platform-admin-portal/src/test/setup.ts`, `egon-cola-platforms/egon-cola-platform-admin-portal/src/main.tsx`, `egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-admin-web/src/main.tsx`, `egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-admin-web/src/app/App.tsx`, `egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-admin-web/src/app/router.tsx`, `egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-admin-web/src/app/AdminLayout.tsx`, `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin-web/src/main.tsx`, `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin-web/src/app/App.tsx`, `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin-web/src/app/router.tsx`, `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web/src/main.tsx`, `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web/src/layouts/AdminLayout.tsx`, `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin-web/src/main.tsx`, `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin-web/src/layouts/AdminLayout.tsx`
- Commit: `feat(platform-portal): add Wujie hybrid host and child lifecycle boundary`

## 8. Test, Validation, and Quality Gates

| Gate/order | Working directory | Command or method | Scope | Expected result | Failure returns to | Requirements/runtime boundary |
| --- | --- | --- | --- | --- | --- | --- |
| RED Step 1 | `egon-cola-platforms/egon-cola-platform-admin-web-shared` | `npm run test -- --run src/components/PageHeader.test.tsx` before File 2 | PageHeader absent/behavior | Fails for the stated missing component/behavior, not setup | Step 1 File 1 | `REQ-001/003`; static package |
| GREEN Step 1 | same | `npm run test -- --run src/components/PageHeader.test.tsx src/layout/EnterpriseLayout.test.tsx && npm run typecheck && npm run lint` | shared shell/state | Exit 0 and existing navigation tests remain green | Step 1 Files 2-7 | `REQ-001/003/013`; static package |
| RED Step 2 | IDP Web package | `npm run test -- --run src/api/page.test.ts src/features/users/UserListPage.test.tsx src/features/resource-grants/ClientResourceGrantPage.test.tsx` before page implementation | list/Grant contracts | Fails for current array/path/capability gaps | Step 2 Files 1/4/10 | `REQ-004/007/009`; static package |
| GREEN Step 2 | IDP Web package | exact Step 2 command in §7 | IDP pages/App | named tests/typecheck/lint exit 0; no secret URL | Step 2 | `REQ-002/004/007/008/009/013/014`; static package |
| RED Step 3 | RBAC3 Web package | `npm run test -- --run src/features/directory/OrganizationPage.test.tsx src/features/directory/PositionPage.test.tsx src/features/role/RolePages.test.tsx src/features/constraint/ConstraintPage.test.tsx` before implementation | route/path/ownership | Fails on current wrong component/old paths | Step 3 Files 1/3/6/7 | `REQ-008/010`; static package |
| GREEN Step 3 | RBAC3 Web package | exact Step 3 command in §7 | routes/API/pages | named tests, conformance, bundle and typecheck exit 0 | Step 3 | `REQ-002/003/004/008/010/013/014`; static package |
| RED Step 4 | Gateway Web package | `npm run test -- --run src/api/gatewayApi.test.ts src/features/interface-catalog/OperationPage.test.tsx src/features/releases/ReleaseDetailPage.test.tsx src/features/observability/TracesPage.test.tsx` before implementation | catalog/release/trace | Fails for missing wrappers/state assertions | Step 4 Files 2/4/6/8 | `REQ-008/011/013/014`; static package |
| GREEN Step 4 | Gateway Web package | exact Step 4 command in §7 | API/page/recovery | named tests/typecheck/lint exit 0; safe detail output | Step 4 | `REQ-002/004/007/008/011/013/014`; static package |
| RED Step 5 | DDC Web package | `npm run test -- --run src/api/client.test.ts src/App.test.tsx src/layouts/AdminLayout.test.tsx src/pages/InstancesPage.test.tsx` before implementation | DDC instance route/page | Fails for missing route/menu/page | Step 5 Files 1/3/5/7 | `REQ-004/008/012`; static package |
| GREEN Step 5 | DDC Web package | exact Step 5 command in §7 | page/route/pagination | named tests/typecheck/lint exit 0; predecessor wrapper unchanged | Step 5 | `REQ-002/003/004/008/012/013/014`; static package |
| Dependency gate Step 6 | Portal package | `npm install --package-lock-only --ignore-scripts --save-exact wujie-react@2.1.0` | dependency graph | only Portal lockfile changes; version exact 2.1.0 | Step 6 File 22/23 | `REQ-006`; static/dependency |
| RED Step 6 | Portal package | `npm run test -- --run src/manifest/loader.test.ts src/lifecycle/lifecycleState.test.ts src/bridge/context.test.ts src/summary/platformSummaryFacade.test.ts src/pages/PortalHomePage.test.tsx src/pages/ChildRoutePage.test.tsx` before implementation | host contracts | Fails for missing loaders/state/bridge/facade/pages | Step 6 Files 1/3/5/7/14/16 | `REQ-005/006/007/013/015`; static package |
| GREEN Step 6 | Portal package | `npm run test -- --run src/manifest/loader.test.ts src/lifecycle/lifecycleState.test.ts src/bridge/context.test.ts src/summary/platformSummaryFacade.test.ts src/pages/PortalHomePage.test.tsx src/pages/ChildRoutePage.test.tsx && npm run typecheck && npm run build` | Portal host | all named tests/build exit 0; Wujie import only in adapter | Step 6 | `REQ-001/005/006/007/013/015`; static package |
| Child build regression | each of four child Web package directories | `npm run typecheck && npm run build` | standalone/embedded entry compile | each exits 0; no child package gets Wujie dependency | Step 6 child files | `REQ-005/006/015`; static package |
| Static forbidden-path scan | repository root | `rg -n 'redis|kafka|ddc rpc|engine|window\.(localStorage|sessionStorage).*token|Authorization|clientSecret|rawBody' egon-cola-platforms/egon-cola-platform-admin-portal egon-cola-platforms/egon-cola-platform-*-admin-web/src` with reviewed allow-list exceptions | host/child sensitive/direct-access boundary | no unreviewed direct internal-store or credential relay path | owning Step | `REQ-007/011`; static |
| Requirement/path regression | repository root | `rg -n '/api/rbac3/v1/(roles|sod-sets|data-rules|field-rules|operation-sod-rules)|/api/rbac3/v1/directory/snapshots' egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin-web/src` | corrected RBAC3 paths | no old path in affected clients/tests except negative test strings | Step 3 | `REQ-008/010`; static |
| User-controlled manual/runtime | deployment environment, only after user starts systems | Open Portal and each standalone child; validate login, 401/403, `/`, `/platform/{key}/...`, refresh deep link, Manifest mismatch, child crash/remount, same-site Cookie/CSRF, independent rollback | browser/deployment topology | user observes actual route/API/auth/asset behavior and records trace/version; no source-only claim substitutes | Step 6 or deployment follow-up | `REQ-005/006/007/013/015`; runtime/manual, not executed here |

No command in this Plan starts Java, Node dev servers, browsers, Docker, PostgreSQL, Redis, Kafka or Gateway services. Runtime/manual rows are future user-controlled proof only。

## 9. Migration, Compatibility, Rollout, and Rollback

### 9.1 Dependency and asset compatibility

1. Generate and review only Portal `package-lock.json` with exact `wujie-react@2.1.0`; verify license/security/transitive diff and React 19/Vite 8 build。
2. Build each child standalone first; then enable lifecycle globals and embedded branch while retaining the standalone path。
3. Publish a versioned Manifest containing `version`, `contractVersion`, `compatibleHostRange`, `url`, `standaloneUrl` and required capability hints。
4. Start with one low-risk read-only child, then add the remaining children only after mount/unmount/error/refresh/deep-link checks pass。

### 9.2 API and data compatibility

- IDP/RBAC3/Gateway/DDC existing API/RPC/database behavior remains unchanged. Frontend adapters accept current wrappers and expose candidate API absence explicitly。
- DDC keeps existing `/page`, `PageResultRecord`, List/Catalog/Snapshot/RPC compatibility. No Flyway file is created or modified。
- Wujie child `embedded` is a presentation/lifecycle flag. It does not change tenant, permission, mutation, transaction, idempotency or audit authority。
- The host-side summary Facade is read-only and can use `NOT_CONFIGURED`; the deferred server-side summary endpoint remains outside this Plan. No partial result is persisted as a global success。

### 9.3 Rollout and rollback

| Unit | Rollout | Health gate | Rollback |
| --- | --- | --- | --- |
| shared | publish/build shared package, then typecheck all four Web consumers | shared tests + four consumer typechecks/builds | restore prior shared package/artifact; no backend rollback |
| one child embedded lifecycle | deploy child artifact with standalone URL and Manifest version | standalone route, host mount, unmount/remount, 401/403, no duplicate shell | Manifest points to prior child or host hides child entry; standalone remains |
| Portal | deploy host with one read-only child, then incrementally enable platforms | Manifest compatibility, local child failure, independent link, deep-link refresh | serve prior host/independent Web entry; do not roll back child business data |
| backend gap candidates | separate platform-owned API Spec/Plan and release | contract/permission/tenant/pagination/audit tests | remove frontend candidate entry or forward-fix API; do not add alias endpoint as rollback |

## 10. Requirement-to-Step Traceability Matrix

| Requirement | Effective Spec section | Steps | Files | Tests/gates | Completion evidence |
| --- | --- | --- | --- | --- | --- |
| `REQ-001` | Spec §4、§7、§12 | Step 1, Step 5, Step 6 | shared PageHeader/PageTemplate/PageState; DDC AdminLayout; Portal Home | shared/Portal shell tests | common shell + Portal cards render |
| `REQ-002` | Spec §4、§7、§12、§15 | Step 2, Step 3, Step 4, Step 5, Step 6 | platform layouts/routes/pages | permission/route/App tests | unauthorized nodes/actions hidden and direct 403 preserved |
| `REQ-003` | Spec §4、§12 | Step 1-6 | PageHeader/PageTemplate and page files | component/page tests | each affected route has described title/filter/state/action implementation |
| `REQ-004` | Spec §4、§7、§12；DDC predecessor | Step 2-6 | API/page query files; Portal router/summary | query/page/filter/deep-link tests | URL/query key/page total/invalidation behavior |
| `REQ-005` | Spec §4、§5、§7、§8、§16 | Step 6 | Portal package, Manifest/lifecycle, four child main/layout files | manifest/lifecycle/build/manual gates | child mounted/standalone/isolated |
| `REQ-006` | Spec §5、§6、§7、§17 | Step 6 | Portal package/lock/vite/Manifest | dependency/build/compatibility tests | exact wrapper version and compatibility gate |
| `REQ-007` | Spec §4、§7、§12、§15 | Step 2, Step 4, Step 6 | secret-safe IDP/Gateway pages; bridge/WujieChild | negative sensitive-field tests | no credential relay/raw sensitive render |
| `REQ-008` | Spec §4、§9 | Step 2-6 | API clients/capability state/summary adapters | path/static/candidate-state tests | existing vs deferred endpoint distinction remains explicit |
| `REQ-009` | Spec §4、§9、§12 | Step 2 | IDP page/types/helper/test files | IDP focused/App tests | scalable page wrapper/Grant existing mutations/audit filters |
| `REQ-010` | Spec §4、§7、§9、§12 | Step 3 | RBAC3 directory/routes/API/tests | RBAC3 route/path/conformance tests | page responsibility and exact Controller paths |
| `REQ-011` | Spec §4、§9、§12、§15 | Step 4 | Gateway API/Operation/Release/Trace files | Gateway API/page/security tests | catalog/release/trace safe consumption |
| `REQ-012` | Spec §4、§9、§11、§12；DDC predecessor | Step 5 | DDC client/types/App/Layout/Instances files | DDC page/client/App tests | instances page plus preserved pagination/polling and explicit gap state |
| `REQ-013` | Spec §4、§7、§12、§15 | Step 1-6 | PageState/PageState consumers/Portal lifecycle | error/partial/conflict/retry tests | distinct failure UI and no false success |
| `REQ-014` | Spec §4、§7、§12、§15 | Step 2-6 | mutation/recovery/query invalidation/summary retry files | pending/conflict/recovery tests | no duplicate actions, identity retained, targeted invalidation/retry |
| `REQ-015` | Spec §4、§7、§8、§16、§17 | Step 6 | Portal/Manifest/child lifecycle/layout | mount/standalone/rollback manual gate | independent entry/version rollback path |
| `REQ-016` | Spec §4、§5、§20 | Step 1-6 and Plan document | this Plan/Spec only; no production code in current turn | Spec/Plan validators; git status review | Review Plan delivered, implementation not started |

## 11. Risks, Blockers, and User Decisions

| ID | Risk or decision | Impacted Steps/files | Evidence | Owner | Status/action |
| --- | --- | --- | --- | --- | --- |
| `DEC-101` | “wijie” means Wujie/无界 | Step 6 Portal/child lifecycle | User confirmation; Spec §5.4 | User/platform owner | Closed; use `wujie-react@2.1.0` baseline |
| `DEC-102` | Unified Wujie entry plus four standalone child URLs | Step 6 | User confirmation; Spec §5.4/§16 | User/platform owner | Closed; no standalone entry deletion |
| `DEC-103` | Static/config-driven menu plus capability pruning | Step 1-6 | existing IDP/Gateway/DDC layouts and RBAC3 resource registry | User/platform owner | Closed; no DB menu/API |
| `DEC-104` | Same-site Cookie/CSRF, no token relay | Step 2/4/6 | existing `credentials: include` clients and Spec security boundary | User/security owner | Closed; bridge security tests are blocking |
| `DEC-105` | Read-only homepage summary Facade, not universal BFF | Step 6 | Spec §5.4/§7.0; deferred server summary row §9 | User/platform owner | Closed; host-side Facade first, server API remains follow-up |
| `DEC-106` | Future Java additions use traditional three-layer; historical feature-first migration is separate | no Java Step; future backend Spec | current Java tree/EVD-022 and Spec §6.1 | User/architecture owner | Closed; this Plan does not modify Java |
| `RISK-001` | `wujie-react@2.1.0` with React 19/Vite 8 may require compatibility adjustment | Step 6 package/lock/adapter | current repo has no Wujie; external package/docs only | Portal owner | Monitoring; package/build/mount spike before commit |
| `RISK-002` | Manifest basename/publicPath/deep-link/asset cache may differ by deployment | Step 6 Manifest/child Vite/runtime | four current Vite apps use independent output; no current Manifest | deployment owner | Monitoring; same-site versioned Manifest and user manual check |
| `RISK-003` | Candidate backend API absence could be mistaken for empty business data | Step 2/4/5/6 | Spec §9 candidate rows and current Controller inventory | platform API owner | Mitigated; explicit unavailable/capability states and static path tests |
| `RISK-004` | Existing dirty worktree could be staged accidentally | all Steps | `git status --short` baseline in §6.1 | implementer | Mitigated; path-limited staging/commit and before/after status snapshot |
| `RISK-005` | Runtime/production topology remains unproven in this doc-only turn | Step 6 manual gate | no service/browser/database start authorized; Spec §2.4/§15 | user/deployment owner | Monitoring; user runs manual/runtime gates after review |

No unresolved user decision or Plan architecture conflict remains for this Web-scoped Plan. Backend API implementation is a separately bounded follow-up, not a hidden blocker for the current document plan。

## 12. Review and Acceptance

### 12.1 Original requirement fidelity

- Covers the original `platforms` Web scope: shared, IDP, RBAC3, Gateway and DDC, plus the newly approved Portal boundary。
- Keeps the left 若依-style enterprise shell: 240px/72px Sidebar, Header, breadcrumb, capability-pruned static menu, responsive Drawer and explicit PageState。
- Keeps the Spec’s per-page UI design as the implementation source: list/filter/summary/detail/edit/recovery states map to exact page files in Steps 1-5。
- Separates existing backend capabilities from candidate missing APIs. The Plan consumes existing endpoints and renders deferred capability state rather than inventing server aliases。
- Implements the confirmed Wujie hybrid direction as a Portal-only dependency with standalone child paths, same-site auth boundary, bridge allow-list, lifecycle cleanup and homepage summary Facade。
- Preserves the confirmed future Java traditional three-layer decision without mixing it into current feature-first Java modules or this non-Java Plan。

### 12.2 Spec consistency

- No page, API, schema, permission, migration, Java package, transaction or BFF contract is added beyond Spec §8/§9/§12 and the evidence-backed exact-file clarifications in §6.4。
- `PageHeader` is limited to repeated shell structure; no universal business table or cross-platform business service is created。
- `PlatformSummaryFacade` is host-side read-only composition with explicit partial/not-configured states; the deferred server summary API remains outside this Plan。
- DDC uses the predecessor’s `/page`/`PageResultRecord` boundary; no database or historical migration is modified。

### 12.3 Repository executability

- Every target path is under the current four Web packages, shared package or new Portal package; current Java/POM/Flyway paths are excluded。
- Each Step has RED-first order, exact file operations, symbols, repository evidence, mapping, error behavior, pseudocode, validation cwd/command, completion, rollback and path-limited commit。
- Parallel Steps 2-5 have disjoint write scopes; Step 6 remains sequential because host/child lifecycle and dependency contracts must align。
- Commands are taken from existing package scripts, with Portal scripts explicitly defined before use; no runtime command is executed by this Plan。

### 12.4 Test and release completeness

- Shared, IDP, RBAC3, Gateway and DDC have focused/component/API/route/typecheck/lint gates；RBAC3 conformance/bundle gates remain in the Step 3 command。
- Portal has Manifest, State, bridge security, Facade partial, Home and ChildRoute tests；each child has standalone typecheck/build after lifecycle changes。
- Release/rollback/deep-link/same-site Cookie/CSRF and actual asset serving require the user-controlled manual/runtime gate in §8；this document does not claim those checks ran。
- No migration, data backfill, Java contract, service startup or browser proof is included as completed evidence。

### 12.5 Blocking Manual Check

| Check ID | Applicability | Status | Evidence | Finding | Required action/exception |
| --- | --- | --- | --- | --- | --- |
| `MC-ARCH-001` | `Not applicable` | `N/A` | §4.7/§4.8；target tree contains only TypeScript/React/Web package files；Spec §6.1 records future Java traditional profile | Current Plan has no Java file or Java layer selection to verify; future Java profile is explicitly preserved | None for current Plan; future Java API Plan must re-run architecture check |
| `MC-REUSE-001` | `Applicable` | `PASS` | §4.7 capability ledger；existing shared Layout/PageState, React Query, AntD, current HTTP/auth clients and platform API clients are reused | Existing capabilities are used before adding host code; only Wujie runtime is a proven approved gap | None |
| `MC-DEP-001` | `Applicable` | `PASS` | EVD-004/EVD-025；§4.7 ledger；Step 6 Files 22-23 pin only `wujie-react@2.1.0` in Portal and require lock/build/license review | New dependency is user-approved and isolated; no duplicate Wujie dependency is planned in child apps | None; execute package/license/build gate before Step 6 commit |
| `MC-NAME-001` | `Not applicable` | `N/A` | §4.8 Rule 1 row；no Java type is created/renamed/modified | No Java PO/DTO/VO/Query/Command carrier is in scope | None |
| `MC-VALID-001` | `Not applicable` | `N/A` | §4.8 Rule 2 row；no Java layer handoff; frontend validation remains existing AntD/API error behavior | No Java `@Valid`/group/ValidatorUtils design is claimed | None; backend API Plan must design Java handoffs |
| `MC-MODEL-001` | `Not applicable` | `N/A` | §4.8 Rule 3 row；no Java model or converter | No Java Record/Lombok/MapStruct/BaseConverter file | None |
| `MC-CONVERT-001` | `Not applicable` | `N/A` | §4.8 Rule 3 row；no Java cross-layer converter | No Java conversion path is touched | None |
| `MC-LOG-001` | `Not applicable` | `N/A` | §4.8 Rule 4 row；no Java business class | No Spring business Bean/logging file | None |
| `MC-BEAN-001` | `Not applicable` | `N/A` | §4.8 Rule 4 row；no Spring-managed Java Bean or `lombok.config` change | No Java DI/Qualifier path | None |
| `MC-UTIL-001` | `Not applicable` | `N/A` | §4.8 Rule 5 row；no Java utility; frontend reuses browser/TypeScript and existing package stack | No Java utility dependency or duplicate `*Utils` file | None |
| `MC-JSON-001` | `Not applicable` | `N/A` | §4.8 Rule 6 row；no Java external JSON DTO; frontend Manifest is covered by loader tests | No Jackson contract in current Plan | None |
| `MC-TIME-001` | `Not applicable` | `N/A` | §4.8 Rule 10 row；no Java date/time/persistence field | ISO strings are existing frontend display values only | None |
| `MC-CONFIG-001` | `Not applicable` | `N/A` | §4.8 Rule 7 row；no Spring config/profile or Java property key; Portal Vite keys are listed in Step 6 | No Java environment parity change | None |
| `MC-PATTERN-001` | `Applicable` | `PASS` | Spec §13；Step 6 Files 3-4 State reducer, File 12 Wujie Adapter, Files 7-8 Summary Facade, and their tests | Complex host lifecycle/partial aggregation has concrete State/Adapter/Facade participants; simple page mapping remains direct | None |
| `MC-SCOPE-001` | `Applicable` | `PASS` | §5 tree, §6.1 dirty-worktree rule, §7 commit paths；Java/POM/Flyway/backend production paths excluded | Change tree is bounded to Web/Portal; unrelated user changes are protected | None; stage exact paths only |
| `MC-TEST-001` | `Applicable` | `PASS` | §4.2, §8 and every Step’s RED/GREEN command; focused tests precede implementation | Each behavior has a named test/gate and future runtime boundary is explicit | None |
| `MC-BLOCKER-001` | `Applicable` | `PASS` | §11 decisions/risks and all rows above；DEC-101-106 closed; no unresolved architecture/contract blocker for Web scope | All current Plan checks are closed or evidence-backed N/A; runtime is a future user gate, not an unknown claim of completion | None; stop for user review before execution |

### 12.6 Final verdict

PASS — Ready for user review
