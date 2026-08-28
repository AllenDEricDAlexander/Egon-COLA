# platforms Web UI 完整性、Wujie 聚合与登录运行时修复实施计划

| Field | Value |
| --- | --- |
| Document | `2026-08-29-01-35-platforms-ui-completeness-runtime-repair.md` |
| Template Version | `4` |
| Status | `Review` |
| Created | `2026-08-29 01:35 CST` |
| Updated | `2026-08-29 01:35 CST` |
| Owner | `User / Egon-COLA platform owner` |
| Repository | `Egon-COLA` |
| Scope | `egon-cola-platforms` Portal、admin-web-shared、IDP/RBAC3/Gateway/DDC Admin Web 与本地统一平台脚本 |
| Source Requirement | 用户要求修复 Portal 内嵌四个平台的 UI 展示、Wujie 挂载、登录失败和 loopback 地址，并逐 Controller 校验前端体现，重点补齐 RBAC3 IAM 用户/角色/权限 CRUD 与关系绑定 CRUD；用户要求直接开始修复 |
| Baseline Revision | `main@86a47ed74`；工作区另有用户未跟踪文档 `docs/egon/spec/2026-08-27-20-31-archetype-two-stage-source-generation.md`，必须保留 |
| Implements Spec | [platforms Web UI 完整性、Wujie 聚合与登录运行时修复规格](../spec/2026-08-29-01-11-platforms-ui-completeness-and-runtime-repair.md) |
| Spec Status | `Accepted` |
| Spec Revision | `2026-08-29 01:11 CST`，commit `86a47ed74` |
| Effective Specs | [platforms Web UI 完整性、Wujie 聚合与登录运行时修复规格](../spec/2026-08-29-01-11-platforms-ui-completeness-and-runtime-repair.md)、[平台 Web 企业级微前端规格](../spec/2026-08-27-08-09-platforms-web-enterprise-microfrontend.md)、[DDC Admin 分页设计](../../superpowers/specs/2026-08-10-ddc-admin-pagination-ui-modernization-design.md) |
| Depends On Plans | [平台 Web 运行修复前序计划](2026-08-28-11-06-platforms-web-runtime-repair-and-controller-coverage.md) |
| Supersedes | None |
| Superseded By | None |
| Related Plans | None |

## 1. Summary

本计划实现已 Accepted 的平台 Web UI 完整性规格，按五个独立语义 Step 推进：先修共享嵌入壳、Wujie child route/fallback、登录 origin 与启动诊断；再补 RBAC3 用户/组织/岗位/角色任职关系；随后补 RBAC3 角色、权限、资源、约束、业务目录和应用详情；最后补 IDP/Gateway/DDC 的真实 Controller action/UI 缺口与 Ant Design UI bug，并以逐方法 Controller/UI 覆盖审计收口。每个 Step 先建立 focused RED，再实现最小修改，执行模块验证后路径限定提交。

完成证据由 Portal/shared/RBAC3/Gateway/IDP/DDC focused Vitest、四套 Web typecheck/build、静态 Controller/UI audit、`git diff --check` 和每 Step commit 组成。Gateway Engine active release、Cookie/CSRF、Wujie DOM 高度和真实 IAM CRUD 的浏览器证据属于用户控制的重启后验证，本计划不自动重启项目或打开浏览器。

## 2. Target Spec and Effective Design

### 2.1 Primary target

- Path: [docs/egon/spec/2026-08-29-01-11-platforms-ui-completeness-and-runtime-repair.md](../spec/2026-08-29-01-11-platforms-ui-completeness-and-runtime-repair.md)
- Status: `Accepted`
- Revision: `2026-08-29 01:11 CST`, baseline `86a47ed74`
- Approval evidence: 用户已明确要求“直接开始修复”；本计划仍标记 `Review`，等待本计划本身的审查后再进入执行技能。

### 2.2 Effective Spec set

| Role | Spec/link | Status/revision | Effective sections | Why included |
| --- | --- | --- | --- | --- |
| Primary | [UI 完整性、Wujie 聚合与登录运行时修复](../spec/2026-08-29-01-11-platforms-ui-completeness-and-runtime-repair.md) | Accepted / 2026-08-29 01:11 | §1-§20；REQ-001..REQ-011；TEST-001..TEST-011 | 本轮用户要求、现状证据、页面布局、范围和验收唯一主设计 |
| Normative dependency | [平台 Web 企业级微前端](../spec/2026-08-27-08-09-platforms-web-enterprise-microfrontend.md) | accepted predecessor / repository document | §5、§7、§12 | 保留 Portal/child ownership、manifest、Wujie 和菜单组织基线 |
| Normative dependency | [DDC Admin 分页设计](../../superpowers/specs/2026-08-10-ddc-admin-pagination-ui-modernization-design.md) | repository design dependency | §1-§10 | 保留 DDC 分页、横向滚动和详情交互合同 |

### 2.3 Superseded or excluded content

前序平台 Web 运行修复规格中关于旧现场“可运行”或“覆盖计数足够”的结论，在本计划范围内由主 Spec §2.2、§7.3.6、§14、§16、§18 取代；后端 Controller 合同、认证 Cookie/CSRF、租户/权限/expected version、数据库/Flyway 历史和 Wujie 版本仍有效。OAuth protocol、internal/RPC、snapshot ingestion、后台 worker 不编造成普通业务 CRUD 页面，而由覆盖审计显式标记 `PROTOCOL_OR_INTERNAL`。

## 3. Effective Requirements and Acceptance

| Requirement | Source Spec section | Effective statement | Observable acceptance | Implementation impact |
| --- | --- | --- | --- | --- |
| `REQ-001` | 主 Spec §4、§7.3、§12.1 | Wujie fallback 不得加载 Portal 自身，child host 具有稳定可滚动视口 | `attrs.src` 为 `about:blank`；host 有明确高度、最小高度和 overflow；reject 进入可恢复状态 | Step 1 修改 Wujie adapter、Portal host 和测试 |
| `REQ-002` | 主 Spec §4、§7.2、§12.1 | Portal 将 `/platform/{key}` 后的当前路由传给 child 初始 URL | 四个平台深链打开对应 child 页面 | Step 1 增加 route resolver 与 route 测试 |
| `REQ-003` | 主 Spec §4、§7.1、§12.2-§12.5 | embedded child 保留领域侧栏，standalone 保持完整 shell | 四个平台侧栏在 Portal 内可导航，重复 Header/Footer 隐藏 | Step 1 扩展 shared Layout 与四个平台 Layout |
| `REQ-004` | 主 Spec §4、§7.2、§15 | 登录 origin、CSRF、Cookie、Gateway Engine active route 一致可诊断 | 客户端和启动脚本均指向 Engine；401/timeout/CSRF 失败能区分 | Step 1 修改 auth client、start/status 诊断 |
| `REQ-005` | 主 Spec §4、§5.1、§16 | 未覆盖时本地地址默认 loopback-first，换 Wi-Fi 不需改 IP | common URL、manifest、Web API 默认 127.0.0.1；LAN 只能显式覆盖 | Step 1 验证并收紧启动输出/检查 |
| `REQ-006` | 主 Spec §4、§7.3.3、§14 | 每个 external Controller method 有 UI/API 体现或解释性排除 | 审计逐方法输出四种分类；管理接口无 `UNCONSUMED` | Step 5 建立 Python inventory/classifier 和 shell gate |
| `REQ-007` | 主 Spec §4、§7.3.4、§12.3 | IAM 用户、角色、权限具备 CRUD 闭环 | 用户新增/编辑/状态/归档，角色新增/编辑/停用，权限新建/详情/状态均可操作 | Steps 2-3 使用已有 RBAC3 API/Query/PermissionGuard |
| `REQ-008` | 主 Spec §4、§7.3.4、§12.3 | IAM 组织/岗位/角色任职/继承/角色资源/权限映射/约束关系可 CRUD | 从用户、角色、资源、策略主体进入绑定动作，403/409 可恢复 | Steps 2-3 增加 panel、drawer、editor 和 route entry |
| `REQ-009` | 主 Spec §4、§12.1-§12.6 | 每个受影响页面有文字设计对应的布局和 loading/empty/error/403/409 | 页面可审查、宽表不裁剪、Drawer/Modal 可用、状态不伪造 | Steps 1-4 统一状态和响应式样式 |
| `REQ-010` | 主 Spec §4、§3.2、§15 | 不扩大安全、数据和架构范围，并修复触及页面的 AntD UI bug | 无 token/secret/raw body；无新 BFF/DB；受影响页消除旧属性告警 | Steps 1-4 source/static review |
| `REQ-011` | 主 Spec §4、§14、§16 | 每 Step 独立验证、路径限定提交、可回退 | 五个 semantic commit；最终 audit 和全模块 gate 可重现 | 全部 Steps 执行 commit/rollback contract |

## 4. Implementation Strategy and Dependency Order

### 4.1 Ordered strategy

1. Step 1 先完成 shared 类型/布局契约，再完成 Portal Wujie route/fallback/viewport，最后接入四 child embedded Layout 和 login/startup diagnostics；它解除下游页面“存在但看不到”和登录地址不一致两个公共阻塞。
2. Step 2 只处理 RBAC3 directory/relations：先让用户详情能够组合组织、岗位、角色任职，再让组织/岗位自身的 CRUD 可见；它依赖 shared shell，但不改角色、权限、约束文件。
3. Step 3 处理 RBAC3 authorization/catalog：角色资源入口、权限详情、租户应用详情、业务目录、约束写操作和管理策略能力读取形成一个资源/策略语义单元；它依赖 Step 2 的主体入口但写作用域不重叠。
4. Step 4 处理 IDP/Gateway/DDC 真实页面 action 和受影响 UI props；只消费已存在 Controller，不为不存在的 Trace detail 增加后端合同。
5. Step 5 扩展现有 shell audit 为逐方法 registry/classifier，先由 Python 单元测试固定解析和分类，再由 shell 集成 gate 验证四个 Admin 源码目录，最后执行所有 Web 模块回归和文件范围审计。

这种顺序不需要生成代码、Java 编译或数据库迁移：Java Controller 是 context-only source，前端 API 合同已存在，shared layout 类型必须在 child 编译前就绪，audit 只有在页面动作完成后才能判断 `UNCONSUMED`。Step 2 与 Step 3 在设计上可由不同开发者并行分析，但由于 Step 3 依赖 Step 2 后的 user-entry contract，执行时按序提交；所有写入路径不重叠。

### 4.2 Test-first strategy

| Behavior | RED test/file | Expected RED reason | Minimum GREEN implementation | Permitted refactor/wiring |
| --- | --- | --- | --- | --- |
| shared embedded shell | `admin-web-shared/src/layout/EnterpriseLayout.test.tsx` | `hideHeader/hideFooter` 配置当前类型不存在或仍渲染 Header/Footer | 类型增加两个可选开关，Layout 条件渲染 | 只调整主列/footer和 shell CSS |
| Portal Wujie route/fallback/viewport | `WujieChild.test.tsx`, `ChildRoutePage.test.tsx` | mock 未收到 route suffix/about:blank，host 高度仍为 420 或初始 URL 为 root | route resolver、`attrs.src`、host style 和安全 reject 状态 | 只接入已有 lifecycle reducer/error boundary |
| child embedded navigation | existing IDP/RBAC3/Gateway/DDC layout/App tests | embedded branch 当前直接返回 children/Outlet，无 domain nav | 四个 Layout 复用 shared config，hide header/footer | 不改变 standalone nav/perms |
| auth diagnosis/startup | shared auth tests and shell static assertions | generic Error/无 preflight 无法区分 origin/CSRF/timeout | typed local error mapping、Engine preflight、127 output | 不改变 backend Cookie/CSRF contract |
| IAM directory relations | `UserRelationsPanel.test.tsx`, Directory/Assignment tests | org/position actions 未消费或 user 页无关系入口 | existing directoryApi + assignmentApi + Query invalidation | subject drawer/tab composition |
| IAM role/permission/constraint/catalog | Role/Application/Constraint tests | role resource/permission detail/constraint writes/business route absent | existing API methods + minimal page actions/route registry | avoid new state store/BFF |
| Gateway/IDP/DDC coverage/UI | existing platform focused tests | release diff/MCP capability validate unused, Trace calls nonexistent, old props warn | actual API actions and current AntD props | preserve existing page design and redacted data |
| Controller audit | `scripts/unified-platform/controller_ui_coverage_test.py` | parser/classification registry absent | stdlib parser + explicit exclusion/management registry | shell wrapper output formatting only |

### 4.3 Sequential and parallel boundaries

| Step | Depends on | May run in parallel with | Must not overlap with | Reason |
| --- | --- | --- | --- | --- |
| Step 1 | None | None | shared/Portal/child layout/startup paths | all child page work needs embedded shell and route contract |
| Step 2 | Step 1 shared layout contract | Static inventory preparation for Step 5 | RBAC3 Step 3 directory/user files | user relation panel and directory CRUD own these files |
| Step 3 | Step 1; Step 2 user-entry behavior | Step 4 only during read-only analysis | RBAC3 role/application/constraint/policy files | role/resource and catalog actions use Step 2 navigation semantics |
| Step 4 | Step 1; Step 3 not required for platform isolation | Step 3 during analysis only | IDP/Gateway/DDC files and gateway audit files | remaining platform UI must not alter RBAC3 files |
| Step 5 | Steps 1-4 source changes committed | None during final audit | `check-admin-web-controller-coverage.sh` and new audit files | final classifier must inspect final UI, then close REQ-006/011 |

### 4.4 Commit boundaries

Each Step has exactly one semantic, path-limited commit. Step 1 is one shared/runtime outcome even though it crosses five Web packages and scripts; without all pieces the public embedded shell or login diagnosis is incomplete. Step 2 and Step 3 are separated by controller domain and file scope. Step 4 owns only remaining UI gaps. Step 5 owns the audit implementation and final static assertions. No Step stages the unrelated untracked archetype Spec.

### 4.5 Spec Simplicity and Implementation-necessity Audit

| Spec element | Spec necessity verdict/section | Current repository evidence | Direct/reuse alternative | Interaction/implementation cost | Plan decision |
| --- | --- | --- | --- | --- | --- |
| Wujie `attrs.src` fallback | Add, §7.0 `Wujie fallback attribute` | `WujieChild.tsx` omits `attrs.src`; Wujie 2.1.0 `iframeGenerator` falls back to `mainHostPath` | Keep omission | proven Portal self-recursion, null document and cleanup failure | Implement in Step 1 |
| `hideHeader`/`hideFooter` shared flags | Add, §7.0 `Shared Header/Footer visibility flags` | shared `EnterpriseLayout` always renders both; child embedded branch removes all shell | Four child-specific wrappers | duplicates shell and hides domain nav | Implement in existing config in Step 1 |
| child route resolver | Add local function, §7.0 | Portal `ChildRoutePage` receives `location.pathname` but starts `manifest.url` root | always root | deep links lose selected page; no extra request | Implement local resolver in Step 1 |
| relation panel | Add composition, §7.0/§13 | `directory.api.ts` and `assignment.api.ts` already expose calls; user page has no consumers | orphan route/manual ID entry | existing APIs stay, but adds query/mutation UI states | Implement in Step 2 |
| constraint write API methods | Add to existing client, §12.3/§7.3.3 | `constraint.api.ts` only has four GETs; Java `ConstraintController` has POST/PUT methods | read-only policy text | violates CRUD and leaves management methods unconsumed | Implement in Step 3 |
| business catalog page | Add route/page, §7.3.3/§12.3 | Java `BusinessCatalogController` has two external GETs; no current route/page | omit because data is indirectly visible | no direct business catalog entry, audit remains uncovered | Implement in Step 3 |
| per-capability MCP validation action | Reuse `gatewayApi.validateMcpCapability`, §7.0/§12.4 | API already exists; capability panels only save/delete; server-level validate exists | server-level validation only | controller method remains unused; users cannot isolate invalid capability | Implement in Step 4 |
| Trace detail removal | Remove false UI call, §7.3.3/§12.4 | `TracesPage` calls `gatewayApi.traceDetail`, but Java observability controller has no mapping | add backend endpoint | new public contract/backend scope not approved and detail page reports missing | Remove frontend call/action in Step 4 |
| startup OAuth preflight | Add script diagnostic, §7.0/§16 | current `start-local-stack.sh` reuses/release routes without checking Engine `/oauth2/login/csrf` | trust old release or control-plane 401 | one safe GET and explicit failure message; no data mutation | Implement in existing startup/status scripts Step 1 |
| Python controller/UI audit registry | Add, §7.0/§7.3.3 | old shell only counts Controller files/mapping lines and Web references | retain counts | cannot answer per method and risks false confidence | Implement stdlib script + shell gate Step 5 |
| BFF, DB migration, new dependency | Remove, §7.0 | existing Gateway/shared/API clients and Java controllers are sufficient | add BFF/store/framework | new auth/data ownership, calls, migration and ops cost without gap | Not implemented |

No fetch-then-forward API is added: child route is derived from current Portal pathname, tenant/identity remain server/bootstrap-owned, role/permission/constraint requests use the selected row and expected version already held by the page, and the audit only reads source text.

### 4.6 Change-unit Dependency Matrix

| Change unit | Requirements | Proof/RED point | Compile/runtime prerequisites | Produces | Consumers/unblocks | Owning Step |
| --- | --- | --- | --- | --- | --- | --- |
| Shared embedded shell flags | `REQ-003`, `REQ-009` | EnterpriseLayout test hides duplicate shell but keeps nav | existing shared package types/AntD | `EnterpriseLayoutConfig.hideHeader/hideFooter` | four child Layouts | Step 1 |
| Wujie initial URL/fallback/viewport | `REQ-001`, `REQ-002` | WujieChild/ChildRoute tests assert options and style | existing Wujie Core mock/lifecycle reducer | safe mount adapter and child route | Portal pages and runtime | Step 1 |
| Login origin diagnosis/preflight | `REQ-004`, `REQ-005` | auth/shell assertions | common URL variables and Engine route | typed client errors/status output | user restart/runtime proof | Step 1 |
| User subject relation composition | `REQ-007`, `REQ-008` | relation panel mutation tests | directory/assignment APIs and existing FeatureApi | org/position/assignment tabs | UserDirectoryPage | Step 2 |
| Directory CRUD pages | `REQ-007`, `REQ-008`, `REQ-009` | Organization/Position tests | shared layout and directory API | create/edit/archive forms | IAM directory nav | Step 2 |
| Role/resource/permission/catalog actions | `REQ-007`, `REQ-008` | Role/Application/Permission tests | Step 2 route/user entry and applicationApi/roleApi | action links, detail reads, business route | IAM resource tree | Step 3 |
| Constraint/policy mutation workbench | `REQ-006`, `REQ-008`, `REQ-009` | Constraint tests | existing DTO field contracts and Query client | POST/PUT action forms | authorization policy page | Step 3 |
| Remaining platform actions and UI cleanup | `REQ-006`, `REQ-009`, `REQ-010` | Gateway/IDP/DDC focused tests | existing APIs and page state components | real action/detail surfaces | final audit | Step 4 |
| Per-method coverage classifier | `REQ-006`, `REQ-011` | Python RED fixtures/source assertions | Steps 1-4 committed source | report statuses and shell nonzero gate | final review/release | Step 5 |

### 4.7 Java, Spring, and Egon-COLA Implementation Standards

本计划是前端、脚本和静态审计计划；没有 Java production file、Spring Bean、DTO/VO、Repository、Flyway migration 或 Java dependency 变更。Java 规则仍按 Context-only 合同逐项约束前端请求字段和审计边界。

| Concern | Current repository evidence | Effective Spec decision | Planned implementation consequence | Owning Steps/checks |
| --- | --- | --- | --- | --- |
| Architecture profile | 四个平台 Java 的 `admin/controller`→`service`→`repository` 与 Web `src/features`；主 Spec §6.1 | preserve existing Traditional Three-Layer backend context + feature-first React; no hybrid Java tree | only modify existing Web feature/layout and `scripts/unified-platform`; no Java package | Steps 1-5; `MC-ARCH-001` |
| Reuse/capability | shared `EnterpriseLayout/PageState/httpClient`、Wujie Core、RBAC3 `FeatureApiClient`/TanStack Query、Gateway/DDC/IDP API clients | existing capability sufficient; no BFF/new framework | extend existing types/hooks/page components; use stdlib Python only for audit | Steps 1-5; `MC-REUSE-001`, `MC-DEP-001` |
| Naming/model/validation/conversion | existing Controller DTO/VO names and frontend `*View/*Command`, Form rules | no Java models or converters added; preserve server validation/version/idempotency contract | TypeScript form mapping trims user fields, keeps expected version and ISO values; no new backend object | Steps 2-4; `MC-NAME-001`, `MC-VALID-001`, `MC-MODEL-001`, `MC-CONVERT-001` |
| Bean/logging/util/JSON/time/config | no changed Java Bean; existing JSON envelopes and `java.time` contracts; `common.sh` URL constants | no Java Bean/logging/config/time change; no secret/raw JSON in UI/audit | use existing clients, shell/curl/Python stdlib; never read token/secret/cookie; preserve ISO strings | Steps 1-5; `MC-LOG-001`, `MC-BEAN-001`, `MC-UTIL-001`, `MC-JSON-001`, `MC-TIME-001`, `MC-CONFIG-001` |
| Business variation/pattern | Wujie lifecycle, coverage status taxonomy and composed relations are real variation points; Spec §13 | use Adapter/Reducer/Registry/Composition only where already justified | Wujie adapter owns lifecycle; audit registry owns classification; React Query owns mutation states; relation panels compose APIs | Steps 1-5; `MC-PATTERN-001` |

#### Capability reuse ledger

| Need | Candidates inspected | Exact evidence | Fit/gap | Decision | Added dependency/custom code | Owning Step/check |
| --- | --- | --- | --- | --- | --- | --- |
| Embedded shell | shared `EnterpriseLayout`, `EnterpriseSidebar`, `EnterpriseHeader/Footer` | `egon-cola-platform-admin-web-shared/src/layout/*` | missing only header/footer flags; sidebar already recursive/responsive | reuse/extend | no dependency; two config booleans | Step 1; `MC-REUSE-001` |
| Wujie mount | `wujie.startApp` and lifecycle reducer | Portal `WujieChild.tsx`, `lifecycleState.ts`, lockfile 2.1.0 | Core exists; missing safe attrs/route/viewport | reuse adapter | no new microfrontend | Step 1; `MC-REUSE-001`, `MC-DEP-001` |
| Auth transport | shared `createGatewayAuthClient`, Gateway Engine OAuth routes | `admin-web-shared/src/auth/gatewayAuthClient.ts`, `OAuthLoginController` | contract exists; errors/preflight insufficient | reuse and improve diagnosis | no token store/BFF | Step 1; `MC-JSON-001`, `MC-CONFIG-001` |
| IAM data/mutations | RBAC3 `FeatureApiClient`, `directoryApi`, `assignmentApi`, `roleApi`, `applicationApi`, Query | RBAC3 `src/features/*` | APIs mostly exist; page consumers missing | reuse existing clients | no SDK replacement/store | Steps 2-3; `MC-REUSE-001` |
| Gateway actions | `gatewayApi`, `useMcpCapabilityCollection`, QueryState | Gateway `src/api`, `src/features/mcp`, `src/components/QueryState` | actual methods exist; two action surfaces unused | reuse and wire | no backend/BFF | Step 4; `MC-REUSE-001` |
| Coverage parsing | Bash `rg` shell audit, Python stdlib available | `scripts/unified-platform/check-admin-web-controller-coverage.sh` and local Python | count-only; needs per-method registry | extend existing script with stdlib helper | no pip/npm dependency | Step 5; `MC-UTIL-001`, `MC-DEP-001` |

### 4.8 User-mandated Java Rule Implementation Matrix

本轮没有受影响的 Java 文件；每一行均为证据支持的 `N/A` 或 preservation，Rule 11 仍适用于所有目标路径，确保不会引入第三种结构。

| Literal rule | Spec source | Repository evidence | Exact files and order | Pseudocode obligations | Validation gate | Steps | Status/blocker |
| --- | --- | --- | --- | --- | --- | --- | --- |
| Rule 1 | 主 Spec §6.2、§8、§10 | 无 Java 类型新增；existing `*Controller/*DTO/*VO` untouched | N/A; audit reads Java names only | 不创建 Java POJO；TypeScript uses existing `View/Command` suffixes | `rg` Java diff review + module typecheck | Steps 1-5 | N/A — no Java type |
| Rule 2 | 主 Spec §6.2、§7、§9、§10 | validation is on existing Java controller/service; frontend Form rules preserve it | existing API clients/pages in Step 2/3 order | preserve required fields, expected versions, server 403/409/422; no client replacement | focused negative form/API tests | Steps 2-4 | N/A — no Java handoff change |
| Rule 3 | 主 Spec §6.2、§10 | no entity/record/Lombok/MapStruct/BaseConverter change | N/A | no Java model or converter pseudocode; map only existing JSON to TS views | dependency/diff review | Steps 1-5 | N/A — no Java model |
| Rule 4 | 主 Spec §6.2、§7、§13 | no Spring Bean/business class added | N/A | no Bean injection; UI hooks call existing clients | typecheck/static import review | Steps 1-5 | N/A — no Bean |
| Rule 5 | 主 Spec §6.2、§15 | package manifests unchanged; script uses bash/curl/jq/Python stdlib already required | existing script helper order | no new utility library; call existing shell helpers and stdlib parser | lockfile/dependency search | Steps 1,5 | PASS — reuse proven |
| Rule 6 | 主 Spec §6.2、§9、§10 | existing ResultRecord/envelope and JSON request contracts unchanged | existing `*.api.ts` request mappings | retain field names/null/defaults; redact token/secret/raw body | API fixture tests/source review | Steps 1-5 | N/A — no Java JSON contract |
| Rule 7 | 主 Spec §6.2、§15 | `common.sh` and generated env key shapes exist; no profile files changed | startup/status files after current constants | preserve same URL key structure and explicit LAN override | shell syntax/default output gate | Step 1 | PASS — config parity preserved |
| Rule 9 | 主 Spec §6.2、§13 | lifecycle/status/relations have genuine variation points | WujieChild, audit registry, Query panels | Adapter/Reducer/Registry/Composition names and branch obligations appear in pseudocode | focused behavior tests | Steps 1-5 | PASS — selected patterns used |
| Rule 10 | 主 Spec §6.2、§10、§15 | Java controllers use existing `java.time`; UI sends existing ISO values | forms/API mapping in Steps 2-4 | no `java.util.Date` or new Java time; preserve null/ISO precision | source search + form/API tests | Steps 2-4 | N/A — no Java time code |
| Rule 11 | 主 Spec §6.1、§6.2、§8 | current backend Traditional Three-Layer and feature-first Web tree verified | all paths in §5 remain existing Web/script profile | no Java production path/new architecture/dependency; every Step preserves tree | path audit + package builds | Every Step | PASS — structure locked |

## 5. Change File Tree

```text
MODIFY egon-cola-platforms/egon-cola-platform-admin-web-shared/src/layout/types.ts
MODIFY egon-cola-platforms/egon-cola-platform-admin-web-shared/src/layout/EnterpriseLayout.tsx
MODIFY egon-cola-platforms/egon-cola-platform-admin-web-shared/src/layout/EnterpriseLayout.test.tsx
MODIFY egon-cola-platforms/egon-cola-platform-admin-portal/src/lifecycle/WujieChild.tsx
MODIFY egon-cola-platforms/egon-cola-platform-admin-portal/src/lifecycle/WujieChild.test.tsx
MODIFY egon-cola-platforms/egon-cola-platform-admin-portal/src/pages/ChildRoutePage.tsx
MODIFY egon-cola-platforms/egon-cola-platform-admin-portal/src/pages/ChildRoutePage.test.tsx
MODIFY egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-admin-web/src/app/AdminLayout.tsx
MODIFY egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-admin-web/src/app/App.test.tsx
MODIFY egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin-web/src/app/router.tsx
MODIFY egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin-web/src/app/App.integration.test.tsx
MODIFY egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web/src/layouts/AdminLayout.tsx
MODIFY egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web/src/layouts/AdminLayout.test.tsx
MODIFY egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin-web/src/layouts/AdminLayout.tsx
MODIFY egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin-web/src/layouts/AdminLayout.test.tsx
MODIFY egon-cola-platforms/egon-cola-platform-admin-web-shared/src/auth/gatewayAuthClient.ts
MODIFY scripts/unified-platform/start-local-stack.sh
MODIFY scripts/unified-platform/status-local-stack.sh
MODIFY egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin-web/src/features/directory/directory.api.ts
CREATE egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin-web/src/features/directory/UserRelationsPanel.tsx
CREATE egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin-web/src/features/directory/UserRelationsPanel.test.tsx
MODIFY egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin-web/src/features/directory/UserDirectoryPage.tsx
MODIFY egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin-web/src/features/directory/OrganizationPage.tsx
MODIFY egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin-web/src/features/directory/OrganizationPage.test.tsx
MODIFY egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin-web/src/features/directory/PositionPage.tsx
MODIFY egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin-web/src/features/directory/PositionPage.test.tsx
MODIFY egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin-web/src/features/assignment/AssignmentPages.test.tsx
MODIFY egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin-web/src/features/role/RoleGraphPage.tsx
MODIFY egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin-web/src/features/role/RolePages.test.tsx
MODIFY egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin-web/src/features/application/application.api.ts
MODIFY egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin-web/src/features/application/ApplicationListPage.tsx
MODIFY egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin-web/src/features/application/ApplicationPages.test.tsx
MODIFY egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin-web/src/features/permission/PermissionCatalogPage.tsx
MODIFY egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin-web/src/features/permission/PermissionCatalogPage.test.tsx
MODIFY egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin-web/src/features/constraint/constraint.api.ts
MODIFY egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin-web/src/features/constraint/ConstraintPage.tsx
MODIFY egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin-web/src/features/constraint/ConstraintPage.test.tsx
CREATE egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin-web/src/features/business/business.api.ts
CREATE egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin-web/src/features/business/BusinessCatalogPage.tsx
CREATE egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin-web/src/features/business/BusinessCatalogPage.test.tsx
MODIFY egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin-web/src/features/governance.routes.tsx
MODIFY egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin-web/src/app/resourceDefinitions.json
MODIFY egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin-web/src/features/management-policy/managementPolicy.api.ts
MODIFY egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin-web/src/features/management-policy/ManagementPolicyPage.tsx
MODIFY egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin-web/src/features/management-policy/ManagementPolicyPage.test.tsx
MODIFY egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web/src/api/gatewayApi.ts
MODIFY egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web/src/api/gatewayApi.test.ts
MODIFY egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web/src/features/releases/ReleaseDetailPage.tsx
MODIFY egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web/src/features/releases/ReleaseDetailPage.test.tsx
MODIFY egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web/src/features/observability/TracesPage.tsx
MODIFY egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web/src/features/observability/TracesPage.test.tsx
CREATE egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web/src/features/mcp/McpCapabilityValidationButton.tsx
CREATE egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web/src/features/mcp/McpCapabilityValidationButton.test.tsx
MODIFY egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web/src/features/mcp/McpPromptsPanel.tsx
MODIFY egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web/src/features/mcp/McpResourcesPanel.tsx
MODIFY egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web/src/features/mcp/McpAppsPanel.tsx
MODIFY egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web/src/features/mcp/McpTasksPanel.tsx
MODIFY egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-admin-web/src/features/users/UserListPage.tsx
MODIFY egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-admin-web/src/features/clients/ClientListPage.tsx
MODIFY egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-admin-web/src/features/tenants/TenantListPage.tsx
MODIFY egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-admin-web/src/features/resource-servers/ResourceServerListPage.tsx
MODIFY egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-admin-web/src/features/resource-grants/ClientResourceGrantPage.tsx
MODIFY egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-admin-web/src/features/keys/SigningKeyPage.tsx
MODIFY egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-admin-web/src/features/overview/OverviewPage.tsx
MODIFY egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin-web/src/auth/RouteGuards.tsx
CREATE scripts/unified-platform/controller_ui_coverage.py
CREATE scripts/unified-platform/controller_ui_coverage_test.py
MODIFY scripts/unified-platform/check-admin-web-controller-coverage.sh
```

| Operation | Path | Current evidence/symbol | Final state | Responsibility | Step | Requirements | Validation owner |
| --- | --- | --- | --- | --- | --- | --- | --- |
| MODIFY | `egon-cola-platforms/egon-cola-platform-admin-web-shared/src/layout/types.ts` | `EnterpriseLayoutConfig` | optional embedded flags | shared contract | 1 | REQ-003/009 | shared typecheck |
| MODIFY | `egon-cola-platforms/egon-cola-platform-admin-web-shared/src/layout/EnterpriseLayout.tsx` | unconditional Header/Footer | conditional shell, intact sidebar/content | layout geometry | 1 | REQ-001/003/009 | shared test |
| MODIFY | `egon-cola-platforms/egon-cola-platform-admin-portal/src/lifecycle/WujieChild.tsx` | `startApp` adapter | safe fallback, child route, viewport | lifecycle boundary | 1 | REQ-001/002 | Portal tests |
| MODIFY | `egon-cola-platforms/egon-cola-platform-admin-portal/src/pages/ChildRoutePage.tsx` | manifest page | route/viewport/state composition | Portal page | 1 | REQ-001/002/009 | Portal build |
| MODIFY | `egon-cola-platforms/egon-cola-platform-admin-web-shared/src/auth/gatewayAuthClient.ts` | generic auth errors | safe typed diagnosis | login transport | 1 | REQ-004/005 | shared tests |
| MODIFY | `scripts/unified-platform/start-local-stack.sh` | release reuse/publish | public login preflight | startup gate | 1 | REQ-004/005 | bash/static |
| MODIFY | `scripts/unified-platform/status-local-stack.sh` | process/health output | origin/login status | operator diagnosis | 1 | REQ-004/005 | shell/static |
| MODIFY | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin-web/src/features/directory/directory.api.ts` | relation API methods | stable view/form mappings | directory client | 2 | REQ-007/008 | RBAC3 tests |
| CREATE | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin-web/src/features/directory/UserRelationsPanel.tsx` | absent | org/position tabs and assignment link | relation composition | 2 | REQ-008 | panel test |
| MODIFY | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin-web/src/features/directory/UserDirectoryPage.tsx` | user detail only | relation panel entry | user subject UI | 2 | REQ-007/008 | RBAC3 test |
| MODIFY | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin-web/src/features/directory/OrganizationPage.tsx` | read-only | CRUD forms/actions | organization UI | 2 | REQ-008 | RBAC3 test |
| MODIFY | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin-web/src/features/directory/PositionPage.tsx` | read-only | CRUD forms/actions | position UI | 2 | REQ-008 | RBAC3 test |
| MODIFY | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin-web/src/features/role/RoleGraphPage.tsx` | role actions | role resource entry | role relation UI | 3 | REQ-007/008 | RBAC3 test |
| MODIFY | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin-web/src/features/application/application.api.ts` | list/resource methods | detail methods | catalog client | 3 | REQ-006/007 | RBAC3 tests |
| MODIFY | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin-web/src/features/permission/PermissionCatalogPage.tsx` | list row drawer | controller detail query | permission UI | 3 | REQ-006/007 | RBAC3 test |
| MODIFY | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin-web/src/features/constraint/constraint.api.ts` | GET only | POST/PUT mappings | constraint client | 3 | REQ-006/008 | RBAC3 test |
| MODIFY | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin-web/src/features/constraint/ConstraintPage.tsx` | read-only tabs | CRUD editor workbench | policy UI | 3 | REQ-008/009 | RBAC3 test |
| CREATE | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin-web/src/features/business/BusinessCatalogPage.tsx` | absent | business/application catalog | catalog UI | 3 | REQ-006 | RBAC3 test |
| MODIFY | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin-web/src/features/governance.routes.tsx` | route descriptors | catalog route | route reachability | 3 | REQ-006/009 | RBAC3 route test |
| MODIFY | `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin-web/src/app/resourceDefinitions.json` | IAM resource tree | business child node | menu ownership | 3 | REQ-003/009 | resource report |
| MODIFY | `egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web/src/features/releases/ReleaseDetailPage.tsx` | structured diff only | actual diff action | release UI | 4 | REQ-006/009 | Gateway test |
| MODIFY | `egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web/src/features/observability/TracesPage.tsx` | fake detail call | summary-only view | honest observability | 4 | REQ-006/010 | Gateway test |
| CREATE | `egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web/src/features/mcp/McpCapabilityValidationButton.tsx` | absent | per-capability validate | MCP UI composition | 4 | REQ-006/009 | Gateway test |
| MODIFY | `egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-admin-web/src/features/users/UserListPage.tsx` | old modal prop | current AntD prop/state | IDP UI bug | 4 | REQ-009/010 | IDP test |
| MODIFY | `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin-web/src/layouts/AdminLayout.tsx` | embedded Outlet only | domain sidebar retained | DDC shell | 4 | REQ-003/009 | DDC test |
| MODIFY | `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin-web/src/auth/RouteGuards.tsx` | `Spin.tip` | `Spin.description` | DDC UI bug | 4 | REQ-010 | DDC typecheck |
| CREATE | `scripts/unified-platform/controller_ui_coverage.py` | absent | per-method inventory/classifier | static audit | 5 | REQ-006/011 | Python test |
| CREATE | `scripts/unified-platform/controller_ui_coverage_test.py` | absent | parser/classifier tests | audit RED/GREEN | 5 | REQ-006/011 | Python unittest |
| MODIFY | `scripts/unified-platform/check-admin-web-controller-coverage.sh` | count-only | authoritative per-method gate | audit command | 5 | REQ-006/011 | shell audit |

Tests not listed individually in this summary table are present in the code tree and owned by the same Step as their production symbol. No Java, Flyway, database, generated artifact, package manifest, lockfile or runtime secret file is in the change tree.

## 6. Prerequisites, Constraints, and Plan Clarifications

### 6.1 Repository and worktree baseline

- Repository: `/Users/mario/SelfProject/Egon-COLA`; branch `main`; baseline `86a47ed74` after the accepted Spec commit.
- At Plan creation, `git status --short` contains only `?? docs/egon/spec/2026-08-27-20-31-archetype-two-stage-source-generation.md`; this user-owned path must never be staged.
- `node_modules`, `target`, generated local env, cookies, secrets, Java jars and active Gateway release are runtime/generated state, not commit paths.
- Before each Step: `git status --short`, `git diff --check`, `git ls-files --error-unmatch` for MODIFY paths, and `test -e` for CREATE parents. Use path-limited `git add`.
- Do not modify existing Flyway files under `classpath:db`, Java Controller/service/DTO/VO, database, active release or user untracked Spec.

### 6.2 Build, test, and environment prerequisites

| Concern | Exact command/source | Required state | Validation boundary |
| --- | --- | --- | --- |
| Skill resources | `python3 .agents/skills/egon-coding-writing-plan/scripts/validate_skill_resources.py` | exit 0 | skill integrity |
| shared Web | `cd egon-cola-platforms/egon-cola-platform-admin-web-shared && npm run test -- --run && npm run typecheck && npm run build` | installed dependencies | static/module |
| Portal Web | `cd egon-cola-platforms/egon-cola-platform-admin-portal && npm run test -- --run && npm run typecheck && npm run build` | installed Wujie/Vite dependencies | static/module |
| IDP Web | `cd egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-admin-web && npm run test -- --run && npm run typecheck && npm run build` | existing shared resolution | static/module |
| RBAC3 Web | `cd egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin-web && npm run test -- --run && npm run typecheck && npm run build && npm run report:resources` | existing SDK/API mocks | static/module |
| Gateway Web | `cd egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web && npm run test -- --run && npm run typecheck && npm run build` | Gateway API mocks | static/module |
| DDC Web | `cd egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin-web && npm run test -- --run && npm run typecheck && npm run build` | DDC mocks | static/module |
| Shell/static | `bash -n scripts/unified-platform/start-local-stack.sh scripts/unified-platform/status-local-stack.sh scripts/unified-platform/check-admin-web-controller-coverage.sh && python3 scripts/unified-platform/controller_ui_coverage_test.py` | shell/Python available | static |
| Runtime proof | user-controlled restart/re-publish then loopback curl/browser | current process and Gateway release refreshed | runtime, not automatic |

### 6.3 Immutable constraints and approved decisions

- Wujie 2.1.0 remains primary; no new microfrontend framework, bare iframe primary mode, BFF, Redux, database table, migration or dependency.
- `GATEWAY_BASE_URL` is the browser public origin and defaults to `http://127.0.0.1:18180`; `18140` is control-plane only.
- `UNIFIED_PLATFORM_ADVERTISED_HOST` remains an explicit LAN override; default 127.0.0.1 is backward-compatible for local use. Source changes do not mutate running processes.
- Existing backend paths, permissions, tenant isolation, CSRF header/cookie, HttpOnly token ownership, expected version, idempotency and error contracts remain unchanged.
- Protocol/internal/callback methods are included in audit evidence but are not forced into misleading CRUD pages.

### 6.4 Plan Clarifications

| ID | Small implementation inference | Repository evidence | Why semantics are unchanged | Impact if wrong |
| --- | --- | --- | --- | --- |
| `PLAN-CLAR-001` | Use existing Vitest files for package tests and add only `UserRelationsPanel.test.tsx`, `BusinessCatalogPage.test.tsx`, `McpCapabilityValidationButton.test.tsx` and the Python audit test for absent behaviors | existing package `*.test.tsx` and Vitest scripts; Spec TEST-001..011 | only changes proof placement, not public API/page behavior | move a fixture into the nearest existing App test before staging if package setup cannot host it |
| `PLAN-CLAR-002` | Use one MCP validation button for plural values already accepted by `gatewayApi.validateMcpCapability`; server/remote-tool validation stays on existing endpoints | `gatewayApi.ts` signature and `useMcpCapabilityCollection` | exact endpoint and no new network contract | unsupported capability is marked audit-only rather than sent to a new path |
| `PLAN-CLAR-003` | Add no separate `RouteDescriptor` behavior unless typecheck proves the business route needs it; existing descriptor shape remains the owner | `features/shared/RouteDescriptor.ts` and governance route registry | avoids speculative route abstraction | return to the existing registry file if no type change is needed |

## 7. Ordered File-by-file Implementation Steps

### Step 1 — Restore complete Portal embedding and loopback-login diagnostics

- Requirements: `REQ-001`, `REQ-002`, `REQ-003`, `REQ-004`, `REQ-005`, `REQ-009`, `REQ-010`, `REQ-011`, `REQ-013`, `REQ-014`, `REQ-015`
- Dependencies: `None`; baseline `86a47ed74`
- Baseline state: Wujie `startApp` omits `attrs.src`, Portal passes the manifest root regardless of current pathname, host has `minHeight: 420`, and each child embedded branch removes its shared shell. `common.sh` defaults URLs to 127 but current running stack may still be old.
- Observable outcome: Portal child deep links load the selected child route into a stable viewport without fallback recursion; embedded child sidebars render while duplicate header/footer are hidden; auth/startup/status identify the public Engine origin and loopback/release failure clearly.
- End state: shared config and four Layouts compile; Portal tests assert route/fallback/style; auth/start scripts have static/targeted proof. No process is restarted and no backend contract changes.
- Test-first gate: `Required` — tests first fail because existing options/root URL/empty embedded shell do not meet the new assertions; `types.ts` is only the compile prerequisite for new config fields.
- Manual Checks: `MC-ARCH-001`, `MC-REUSE-001`, `MC-DEP-001`, `MC-VALID-001`, `MC-UTIL-001`, `MC-JSON-001`, `MC-CONFIG-001`, `MC-PATTERN-001`, `MC-SCOPE-001`, `MC-TEST-001`, `MC-BLOCKER-001`
- Literal Rules: `Rule 2`, `Rule 5`, `Rule 6`, `Rule 7`, `Rule 9`, `Rule 11`
- Ordered files:

#### File 1 — `MODIFY egon-cola-platforms/egon-cola-platform-admin-web-shared/src/layout/types.ts`

- Purpose: Extend the shared shell contract with optional embedded visibility switches.
- Symbols: `EnterpriseLayoutConfig`, `hideHeader?: boolean`, `hideFooter?: boolean`.
- Repository evidence: the interface already carries header/footer/navigation/content style and all four child Layouts import it.
- Dependencies and consumers: `EnterpriseLayout.tsx`, IDP `AdminLayout`, RBAC3 `router.tsx`, Gateway/DDC `AdminLayout`; no generated output.
- Why now: new shell tests and Layout implementation need the contract before their behavior can compile.
- Contract/signature changes: add two optional readonly booleans; absent/false preserves standalone rendering.
- Input/output and state mapping: `undefined/false` renders Header/Footer; `true` removes only that region while navigation/content remain.
- Error and edge behavior: flags are independent; no navigation still means no sidebar; no network or persisted state is introduced.
- Standards impact: `MC-ARCH-001`, `MC-REUSE-001`, `MC-DEP-001`, `MC-SCOPE-001` — reuse the existing interface and add no dependency or Java surface.
- Literal rule enforcement: `Rule 5`, `Rule 7`, `Rule 11` — no utility/profile change and the type remains in the current shared feature-first Web structure.
- Implementation pseudocode:

```typescript
interface EnterpriseLayoutConfig extends EnterpriseHeaderConfig {
  readonly navigation?: readonly EnterpriseNavigationItem[]
  readonly onNavigate?: (item: EnterpriseNavigationItem) => void
  readonly footer?: EnterpriseFooterConfig
  readonly contentStyle?: CSSProperties
  readonly hideHeader?: boolean
  readonly hideFooter?: boolean
}
```

- Verification contribution: shared typecheck proves old configs remain valid; File 2 consumes the new flags.
- After this file: compile prerequisite exists; rendering is unchanged and the new shell test is expected RED.

#### File 2 — `MODIFY egon-cola-platforms/egon-cola-platform-admin-web-shared/src/layout/EnterpriseLayout.test.tsx`

- Purpose: Define embedded shell behavior before changing rendering.
- Symbols: add `keepsNavigationWhenEmbeddedShellIsHidden`; retain desktop/mobile/nested selection tests.
- Repository evidence: current test uses MemoryRouter, asserts `主菜单`, user/footer and nested navigation.
- Dependencies and consumers: `EnterpriseLayoutConfig` from File 1 and existing Header/Sidebar/Footer DOM roles.
- Why now: independent Header/Footer hiding is the RED contract.
- Contract/signature changes: config with both flags hides platform header/footer but keeps menu and page content; single-flag case proves independence.
- Input/output and state mapping: flags -> DOM presence only; no network or persistence.
- Error and edge behavior: default config continues to render full shell; hidden footer does not hide content; sidebar remains accessible.
- Standards impact: `MC-REUSE-001`, `MC-VALID-001`, `MC-SCOPE-001`, `MC-TEST-001` — existing Testing Library/MemoryRouter only.
- Literal rule enforcement: `Rule 2`, `Rule 6`, `Rule 11` — preserve input/JSON-like props and current package structure.
- Implementation pseudocode:

```typescript
it('keeps the domain menu while hiding duplicate embedded shell regions', () => {
  renderLayoutWith({...config, hideHeader: true, hideFooter: true})
  expect(screen.queryByText('DDC Admin')).not.toBeInTheDocument()
  expect(screen.queryByText(/版本 v5\.3\.2/)).not.toBeInTheDocument()
  expect(screen.getByRole('navigation', {name: '主菜单'})).toBeInTheDocument()
})
```

- Verification contribution: focused test fails against unconditional Header/Footer rendering and guards standalone regressions.
- After this file: shell behavior is intentionally RED; production still renders the old shell.

#### File 3 — `MODIFY egon-cola-platforms/egon-cola-platform-admin-web-shared/src/layout/EnterpriseLayout.tsx`

- Purpose: Implement conditional shell regions without duplicating layout code in children.
- Symbols: `EnterpriseLayout`, `mainColumn`, conditional `EnterpriseHeader` and `EnterpriseFooter`.
- Repository evidence: current `mainColumn` always renders footer and root always renders header; `EnterpriseSidebar` already owns responsive drawer/selection.
- Dependencies and consumers: Files 1-2; all child Layouts pass config; Grid/Router behavior stays unchanged.
- Why now: GREEN implementation for the shell RED test.
- Contract/signature changes: `config.hideHeader !== true` guards Header; `config.hideFooter !== true` guards Footer; content/sidebar style remains.
- Input/output and state mapping: flags alter only DOM regions; navigation/actions/user/contentStyle/openKeys/drawer state remain.
- Error and edge behavior: missing flags render current full shell; no nav still has no sidebar; footer hidden does not remove main column.
- Standards impact: `MC-ARCH-001`, `MC-REUSE-001`, `MC-VALID-001`, `MC-CONFIG-001`, `MC-SCOPE-001`, `MC-TEST-001` — preserve existing responsive geometry and defaults.
- Literal rule enforcement: `Rule 2`, `Rule 6`, `Rule 7`, `Rule 11` — preserve validation/JSON/config semantics and current layout profile.
- Implementation pseudocode:

```tsx
const mainColumn = <Layout style={{minWidth: 0, flex: '1 1 auto'}}>
  <Layout.Content style={{flex: '1 0 auto', minWidth: 0, overflowX: 'hidden', ...config.contentStyle}}>{children}</Layout.Content>
  {config.hideFooter !== true && <EnterpriseFooter platformName={config.platformName} {...config.footer} />}
</Layout>
return <Layout style={{minHeight: '100vh'}}>{config.hideHeader !== true && <EnterpriseHeader {...headerProps} />}<Layout hasSider={full && navigation.length > 0}>{sidebar}{mainColumn}</Layout></Layout>
```

- Verification contribution: File 2 turns GREEN; existing desktop/mobile/nested tests prove selection and footer defaults.
- After this file: shared embedded behavior is GREEN and standalone behavior remains green.

#### File 4 — `MODIFY egon-cola-platforms/egon-cola-platform-admin-portal/src/lifecycle/WujieChild.test.tsx`

- Purpose: Lock the safe Wujie fallback, child URL and viewport contract before adapter changes.
- Symbols: extend existing mount test with `attrs.src`, route URL and host style assertions; retain reject/destroy tests.
- Repository evidence: test mocks `startApp`, asserts `name/url/el/fiber/sync`, and observes cleanup events.
- Dependencies and consumers: `WujieChild.tsx`, Portal test setup, lifecycle callbacks.
- Why now: focused RED proves the runtime log’s self-recursive fallback is represented in tests.
- Contract/signature changes: expected options include `attrs: {src: 'about:blank'}`, `sync: false`, route intent and explicit height/overflow.
- Input/output and state mapping: manifest/context -> startApp options; reject -> `MOUNT_FAILED`; destroy -> `CLEANED`; no credential fields.
- Error and edge behavior: reject leaves a host for visible error; cleanup still emits `afterUnmount`; options cannot contain token/cookie/password.
- Standards impact: `MC-REUSE-001`, `MC-JSON-001`, `MC-SCOPE-001`, `MC-TEST-001` — exercise Core only through existing mock and presentation props.
- Literal rule enforcement: `Rule 6`, `Rule 9`, `Rule 11` — preserve JSON-safe props, Adapter/Reducer lifecycle and current Web tree.
- Implementation pseudocode:

```typescript
expect(vi.mocked(startApp)).toHaveBeenCalledWith(expect.objectContaining({
  attrs: {src: 'about:blank'},
  url: expect.stringMatching(/\/overview$/),
  el: screen.getByTestId('wujie-host-idp'),
}))
expect(screen.getByTestId('wujie-host-idp')).toHaveStyle({height: 'calc(100vh - 160px)', overflow: 'hidden'})
```

- Verification contribution: fails until safe fallback/route/viewport are implemented; existing reject/cleanup assertions remain.
- After this file: Wujie option contract is locked and intentionally RED.

#### File 5 — `MODIFY egon-cola-platforms/egon-cola-platform-admin-portal/src/pages/ChildRoutePage.test.tsx`

- Purpose: Define Portal pathname-to-child-route behavior and visible mount host.
- Symbols: add compatible deep-link test for `/platform/gateway/dashboard`; retain incompatible-manifest/cleanup tests.
- Repository evidence: fixture has gateway manifest and MemoryRouter deep link but does not assert route suffix.
- Dependencies and consumers: `ChildRoutePage.tsx`, manifest loader and Wujie mock.
- Why now: route acceptance must exist before page composition changes.
- Contract/signature changes: compatible child receives `/dashboard` suffix; malformed/incompatible key still 404/manifest error without Wujie call.
- Input/output and state mapping: Portal pathname suffix -> initial child URL; standalone URL remains independent link.
- Error and edge behavior: no suffix maps to child root; `..`/query escape is rejected/normalized; incompatible manifest never mounts.
- Standards impact: `MC-VALID-001`, `MC-JSON-001`, `MC-SCOPE-001`, `MC-TEST-001` — route/manifest boundary uses existing fixtures only.
- Literal rule enforcement: `Rule 2`, `Rule 6`, `Rule 11` — validate route input, preserve manifest JSON fields, and add no layer.
- Implementation pseudocode:

```typescript
it('passes the Portal deep-link suffix to the selected child', async () => {
  vi.stubGlobal('fetch', vi.fn().mockResolvedValue(response(manifest())))
  renderRoute('/platform/gateway/dashboard')
  await screen.findByTestId('wujie-host-gateway')
  expect(startApp).toHaveBeenCalledWith(expect.objectContaining({url: expect.stringMatching(/\/dashboard$/)}))
})
```

- Verification contribution: RED exposes current root-only URL; existing manifest safety tests stay green.
- After this file: deep-link behavior is specified; production remains unchanged.

#### File 6 — `MODIFY egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-admin-web/src/app/App.test.tsx`

- Purpose: Assert IDP embedded mode keeps its permission-filtered domain navigation.
- Symbols: add embedded layout render assertion to existing app/navigation tests.
- Repository evidence: current tests already verify navigation pruning and deep-link selection; App passes an `embedded` prop.
- Dependencies and consumers: IDP `AdminLayout.tsx` and shared Layout flags.
- Why now: child shell RED tests must cover each platform’s actual entry point.
- Contract/signature changes: embedded render must show an IDP menu item and omit duplicate platform banner/footer; standalone test remains unchanged.
- Input/output and state mapping: `$wujie.props.embedded`/App prop -> Layout DOM; permission list still prunes unauthorized nodes.
- Error and edge behavior: no permission remains hidden; no auth bootstrap means existing app behavior remains.
- Standards impact: `MC-REUSE-001`, `MC-VALID-001`, `MC-SCOPE-001`, `MC-TEST-001` — reuse current mocks and permission guard.
- Literal rule enforcement: `Rule 2`, `Rule 6`, `Rule 11` — preserve validation/JSON permission context and package structure.
- Implementation pseudocode:

```typescript
it('renders the IDP domain navigation when embedded', async () => {
  renderApp({embedded: true, permissions: ['idp:identity-user:read']})
  expect(await screen.findByText('身份目录')).toBeInTheDocument()
  expect(screen.queryByText('统一身份平台')).not.toBeInTheDocument()
})
```

- Verification contribution: fails against the current `return <>{children}</>` branch.
- After this file: IDP embedded contract is RED and standalone guards remain.

#### File 7 — `MODIFY egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin-web/src/app/App.integration.test.tsx`

- Purpose: Assert RBAC3 embedded mode exposes the IAM tree.
- Symbols: add route rendering case for `embedded: true`, visible `IAM` and child route labels.
- Repository evidence: current integration tests cover organization/position routes, permission guards and navigation registry.
- Dependencies and consumers: RBAC3 `router.tsx`, `visibleNavigation(about)`, shared Layout.
- Why now: IAM menu visibility is a direct user requirement and must be tested at router boundary.
- Contract/signature changes: no new route contract; existing `about` menus/routes/actions drive visible navigation.
- Input/output and state mapping: about permissions/routes -> sidebar DOM; route guard still returns 403 for forbidden manual paths.
- Error and edge behavior: no accessible route keeps existing 403/no-rail behavior; embedded only changes shell visibility.
- Standards impact: `MC-REUSE-001`, `MC-VALID-001`, `MC-SCOPE-001`, `MC-TEST-001` — use existing SDK fixtures and route descriptors.
- Literal rule enforcement: `Rule 2`, `Rule 6`, `Rule 11` — retain permission validation/JSON and existing feature route architecture.
- Implementation pseudocode:

```typescript
it('keeps IAM navigation inside the embedded RBAC3 child', () => {
  renderApplicationRouter({embedded: true, about: authorizedAbout(['system:user:read', 'system:role:read'])})
  expect(screen.getByText('IAM')).toBeInTheDocument()
  expect(screen.getByText('用户目录')).toBeInTheDocument()
  expect(screen.queryByText('RBAC3 权限平台')).not.toBeInTheDocument()
})
```

- Verification contribution: fails while embedded returns bare children; existing route access tests remain.
- After this file: RBAC3 shell contract is RED.

#### File 8 — `MODIFY egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web/src/layouts/AdminLayout.test.tsx`

- Purpose: Assert Gateway capability-filtered navigation remains visible in embedded mode.
- Symbols: add `$wujie.props.embedded` test; retain standalone capability pruning and operation deep-link tests.
- Repository evidence: current test renders AdminLayout through MemoryRouter and stubs auth/capabilities.
- Dependencies and consumers: Gateway `AdminLayout.tsx`, shared Layout flags and `filterNavigation`.
- Why now: verifies the third child’s actual shell branch.
- Contract/signature changes: embedded still renders Gateway menu such as `网关治理`, while `Gateway Admin` Header/Footer are hidden.
- Input/output and state mapping: capability booleans -> nav tree; embedded flag -> shell DOM.
- Error and edge behavior: MCP menu remains hidden without `gateway:mcp:read`; standalone behavior unchanged.
- Standards impact: `MC-REUSE-001`, `MC-VALID-001`, `MC-SCOPE-001`, `MC-TEST-001` — existing capability hooks/mocks only.
- Literal rule enforcement: `Rule 2`, `Rule 6`, `Rule 11` — preserve capability validation/JSON and current Layout path.
- Implementation pseudocode:

```typescript
it('keeps Gateway navigation in embedded mode', () => {
  renderLayout('/dashboard', {embedded: true, canRead: true, canReadMcp: true})
  expect(screen.getByText('网关治理')).toBeInTheDocument()
  expect(screen.queryByText('Gateway Admin')).not.toBeInTheDocument()
})
```

- Verification contribution: RED against current early Outlet return.
- After this file: Gateway embedded shell behavior is fixed by test contract only.

#### File 9 — `MODIFY egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin-web/src/layouts/AdminLayout.test.tsx`

- Purpose: Assert DDC runtime/metadata second-level menu stays visible when embedded.
- Symbols: add embedded `$wujie` case; retain current responsive/standalone assertions.
- Repository evidence: existing DDC layout test already controls `matchMedia` and tests responsive shell behavior.
- Dependencies and consumers: DDC `AdminLayout.tsx`, shared sidebar and Outlet.
- Why now: covers the fourth downstream child and the user’s incomplete embedded-page complaint.
- Contract/signature changes: embedded DOM contains `运行状态`/`元数据管理` children and omits duplicate DDC banner/footer.
- Input/output and state mapping: embedded flag -> Layout; current identity/logout config is preserved in standalone.
- Error and edge behavior: child route still renders Outlet; mobile drawer remains controlled by shared Layout.
- Standards impact: `MC-REUSE-001`, `MC-VALID-001`, `MC-SCOPE-001`, `MC-TEST-001` — existing responsive fixture and shared component.
- Literal rule enforcement: `Rule 2`, `Rule 6`, `Rule 11` — preserve auth/JSON and current module structure.
- Implementation pseudocode:

```typescript
it('keeps DDC runtime and metadata navigation when embedded', () => {
  renderLayout({embedded: true})
  expect(screen.getByText('运行状态')).toBeInTheDocument()
  expect(screen.getByText('元数据管理')).toBeInTheDocument()
  expect(screen.queryByText('DDC Admin')).not.toBeInTheDocument()
})
```

- Verification contribution: RED against bare Outlet; responsive standalone tests remain.
- After this file: DDC embedded shell contract is RED.

#### File 10 — `MODIFY egon-cola-platforms/egon-cola-platform-admin-portal/src/lifecycle/WujieChild.tsx`

- Purpose: Implement safe Wujie lifecycle options and explicit host viewport.
- Symbols: `WujieChild`, local child URL resolver usage, `startApp` options, existing cleanup callbacks.
- Repository evidence: `context.routeIntent` is already passed into child props; Wujie 2.1.0 uses `attrs.src` as fallback; reducer/error boundary already exist.
- Dependencies and consumers: Files 4-5, Portal ChildRoutePage, Wujie Core; child receives `embedded` props only.
- Why now: GREEN implementation for Wujie RED tests.
- Contract/signature changes: pass `attrs: {src: 'about:blank'}`, route-resolved `url`, `sync: false`, fixed `height/minHeight/overflow`; keep callbacks and `degrade` behavior.
- Input/output and state mapping: manifest URL + route intent -> child URL; lifecycle state remains `LOAD/MOUNTED/MOUNT_FAILED/CLEANED`; host style is presentation-only.
- Error and edge behavior: fallback never equals Portal origin; start reject reports failure; disposed promise destroys returned instance; no unhandled cleanup or secret props.
- Standards impact: `MC-ARCH-001`, `MC-REUSE-001`, `MC-VALID-001`, `MC-JSON-001`, `MC-PATTERN-001`, `MC-SCOPE-001`, `MC-TEST-001` — Wujie Adapter remains sole Core boundary and lifecycle Reducer remains owner.
- Literal rule enforcement: `Rule 2`, `Rule 6`, `Rule 9`, `Rule 11` — preserve input/JSON contracts, use Adapter/Reducer for real lifecycle variation and current structure.
- Implementation pseudocode:

```tsx
void startApp({
  name: manifest.key,
  url: resolveChildUrl(manifest.url, context.routeIntent),
  el: host,
  attrs: {src: 'about:blank'}, sync: false, fiber: false, degrade: import.meta.env.DEV,
  props: safeProps, afterMount: mounted, beforeUnmount: reportUnmount,
  afterUnmount: reportCleaned, loadError: reportLoadFailure,
})
return <div ref={hostRef} data-testid={`wujie-host-${manifest.key}`} style={{width: '100%', height: 'calc(100vh - 160px)', minHeight: 640, overflow: 'hidden'}} />
```

- Verification contribution: File 4 turns GREEN; reject/destroy tests prove failure and cleanup order.
- After this file: Wujie no longer targets Portal as fallback and host geometry is explicit.

#### File 11 — `MODIFY egon-cola-platforms/egon-cola-platform-admin-portal/src/pages/ChildRoutePage.tsx`

- Purpose: Resolve the Portal deep-link suffix and use current AntD layout/loading properties.
- Symbols: local `resolveChildUrl`/route-intent handoff, `ChildRoutePage`, `Space.orientation`, `Spin.description`, embedded viewport wrapper.
- Repository evidence: page already has `useLocation`, manifest query, lifecycle reducer, `PageHeader`, `WujieChild` and standalone retry link.
- Dependencies and consumers: WujieChild File 10, Portal router, `PORTAL_PLATFORM_ITEMS/navigationFor`, shared EnterpriseLayout.
- Why now: page must pass route intent and wrap the now-fixed host without changing manifest ownership.
- Contract/signature changes: child context route intent remains current pathname; route suffix maps to `/overview`, `/roles`, `/dashboard`, `/registry` through pathname rather than a new API.
- Input/output and state mapping: location -> safe child URL; manifest pending/error/empty/mounting/failure -> existing PageState-like branches; viewport fills available content.
- Error and edge behavior: invalid platform stays 404; incompatible manifest shows standalone link; retry waits for cleanup; no query string is synchronized back to Portal.
- Standards impact: `MC-REUSE-001`, `MC-VALID-001`, `MC-JSON-001`, `MC-CONFIG-001`, `MC-SCOPE-001`, `MC-TEST-001` — reuse existing reducer/ErrorBoundary/manifest and correct current props.
- Literal rule enforcement: `Rule 2`, `Rule 6`, `Rule 7`, `Rule 11` — preserve input/manifest/config field shapes and current page structure.
- Implementation pseudocode:

```tsx
const safeContext = buildChildProps({...baseContext, routeIntent: location.pathname})
const content = <Space orientation="vertical" size="middle" style={{width: '100%'}}>
  {isFailure && <MountFailure ... />}
  {!remounting && <div className="embedded-viewport"><WujieChild key={mountKey} manifest={manifest} context={safeContext} onState={dispatch}/></div>}
</Space>
```

- Verification contribution: ChildRoute test turns GREEN and Portal build catches route/style type errors.
- After this file: Portal deep links and viewport composition are complete.

#### File 12 — `MODIFY egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-admin-web/src/app/AdminLayout.tsx`

- Purpose: Keep IDP’s permission-filtered second-level menu in embedded mode.
- Symbols: existing `config`, `navigation`, `breadcrumbItems`, embedded return branch.
- Repository evidence: current config is complete but `if (embedded) return <>{children}</>` discards it.
- Dependencies and consumers: shared Layout File 3, IDP App/router and AuthContext.
- Why now: implements File 6 RED without duplicating shell/config.
- Contract/signature changes: embedded returns shared Layout with `hideHeader:true, hideFooter:true` and same breadcrumb/children; standalone return unchanged.
- Input/output and state mapping: permission bootstrap -> nav tree; children -> content; header/footer flags -> DOM only.
- Error and edge behavior: filterNavigation still removes unauthorized leaves; logout remains on standalone/user config; no cross-platform data.
- Standards impact: `MC-ARCH-001`, `MC-REUSE-001`, `MC-VALID-001`, `MC-SCOPE-001`, `MC-TEST-001` — reuse shared config and existing permission hook.
- Literal rule enforcement: `Rule 2`, `Rule 6`, `Rule 11` — retain validation/JSON permission semantics and current feature tree.
- Implementation pseudocode:

```tsx
const layoutChildren = <><Breadcrumb items={breadcrumbItems} style={{marginBottom: 16}} />{children}</>
return embedded
  ? <EnterpriseLayout config={{...config, hideHeader: true, hideFooter: true}}>{layoutChildren}</EnterpriseLayout>
  : <EnterpriseLayout config={config}>{layoutChildren}</EnterpriseLayout>
```

- Verification contribution: IDP App test turns GREEN and standalone shell remains visible.
- After this file: IDP embedded sidebar and page content are reachable.

#### File 13 — `MODIFY egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin-web/src/app/router.tsx`

- Purpose: Keep RBAC3 `visibleNavigation(about)` and IAM tree in embedded mode.
- Symbols: local `AdminLayout`, `ApplicationRouter`, `EnterpriseLayout` return branch.
- Repository evidence: `AdminLayout` computes `visibleNavigation(about)` but drops it when `embedded` is true.
- Dependencies and consumers: SDK `about`, `RouteAccessGuard`, shared Layout and route descriptors.
- Why now: implements File 7 RED at the router boundary.
- Contract/signature changes: embedded wraps `children` with shared Layout flags; route/fallback/403 logic is unchanged.
- Input/output and state mapping: about -> navigation; routes -> content; `embedded` -> shell flags.
- Error and edge behavior: no about remains null; denied route remains 403; no permission is inferred from visibility.
- Standards impact: `MC-ARCH-001`, `MC-REUSE-001`, `MC-VALID-001`, `MC-SCOPE-001`, `MC-TEST-001` — reuse SDK and shared shell.
- Literal rule enforcement: `Rule 2`, `Rule 6`, `Rule 11` — preserve authorization validation/JSON and current feature-first structure.
- Implementation pseudocode:

```tsx
const shell = <EnterpriseLayout config={{...config, hideHeader: embedded, hideFooter: embedded}}>{children}</EnterpriseLayout>
const embeddedLabel = embedded ? 'embedded' : 'standalone'
assertShellMode(shell, embeddedLabel)
return shell
```

- Verification contribution: RBAC3 integration test turns GREEN; direct route guard tests remain.
- After this file: IAM menu is visible inside Portal.

#### File 14 — `MODIFY egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web/src/layouts/AdminLayout.tsx`

- Purpose: Preserve Gateway capability-filtered navigation in embedded mode.
- Symbols: `AdminLayout`, existing `navigation`, `filterNavigation`, `$wujie` embedded detection.
- Repository evidence: config is built after capability checks, but embedded branch returns `<Outlet />` before it is used.
- Dependencies and consumers: shared Layout, `useAuth/useCapability`, Gateway BrowserRouter/Outlet.
- Why now: implements File 8 RED while keeping capability ownership in Gateway.
- Contract/signature changes: compute `items/config` for both modes; embedded wraps `Outlet` in shared flags; standalone remains full shell.
- Input/output and state mapping: capability -> filtered nav; auth user/actions -> config; Outlet -> content.
- Error and edge behavior: no capability gives no shell item; logout/query clear behavior remains; no route/API contract changes.
- Standards impact: `MC-ARCH-001`, `MC-REUSE-001`, `MC-VALID-001`, `MC-SCOPE-001`, `MC-TEST-001` — shared composition and existing capability guard only.
- Literal rule enforcement: `Rule 2`, `Rule 6`, `Rule 11` — preserve capability input/JSON and package structure.
- Implementation pseudocode:

```tsx
const content = <EnterpriseLayout config={{...config, hideHeader: embedded, hideFooter: embedded}}><Outlet /></EnterpriseLayout>
const navigationItems = visibleNavigation(about)
assert navigationItems.length >= 0
return content
```

- Verification contribution: Gateway embedded test turns GREEN; standalone tests preserve Header/Footer.
- After this file: Gateway child menu is visible in Portal.

#### File 15 — `MODIFY egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin-web/src/layouts/AdminLayout.tsx`

- Purpose: Preserve DDC runtime/metadata navigation in embedded mode.
- Symbols: `AdminLayout`, `navigation`, `$wujie` detection, shared config.
- Repository evidence: current embedded branch returns `<Outlet />` although the full navigation config already exists.
- Dependencies and consumers: shared Layout, DDC AuthContext, BrowserRouter Outlet.
- Why now: implements File 9 RED.
- Contract/signature changes: embedded wraps Outlet in `EnterpriseLayout` with only header/footer hidden; standalone remains unchanged.
- Input/output and state mapping: identity/logout/actions remain config values; nav/content render in child viewport.
- Error and edge behavior: auth failure remains owned by RouteGuards; responsive menu remains shared; no data contract changes.
- Standards impact: `MC-ARCH-001`, `MC-REUSE-001`, `MC-VALID-001`, `MC-SCOPE-001`, `MC-TEST-001` — reuse existing shell/auth.
- Literal rule enforcement: `Rule 2`, `Rule 6`, `Rule 11` — preserve auth validation/JSON and current module structure.
- Implementation pseudocode:

```tsx
const body = <EnterpriseLayout config={{...config, hideHeader: embedded, hideFooter: embedded}}><Outlet /></EnterpriseLayout>
const shellMode = embedded ? 'domain-navigation' : 'full-shell'
assertShellMode(body, shellMode)
return body
```

- Verification contribution: DDC embedded test turns GREEN; standalone responsive behavior remains.
- After this file: all four child Layouts expose their domain menus.

#### File 16 — `MODIFY egon-cola-platforms/egon-cola-platform-admin-web-shared/src/auth/gatewayAuthClient.ts`

- Purpose: Make login errors distinguish normalized origin, CSRF challenge and HTTP/network failure without exposing credentials.
- Symbols: local/exported `GatewayAuthError` if package convention requires; `request`, `csrf`, `createGatewayAuthClient`.
- Repository evidence: client already uses trailing-slash normalization, `credentials:'include'`, `/oauth2/login/csrf`, `X-IDP-CSRF` and envelope parsing.
- Dependencies and consumers: IDP/RBAC3/Gateway/DDC AuthContext/LoginPage; backend `OAuthLoginController`.
- Why now: source login must expose actionable cause when public Engine route is stale.
- Contract/signature changes: preserve `GatewayAuthClient` methods/payloads; add safe code/status/path metadata to errors; do not read/store/return token.
- Input/output and state mapping: base URL -> request origin; CSRF token is transient header; result remains `GatewayLoginResult`; errors classify network/timeout, CSRF and HTTP status.
- Error and edge behavior: no automatic POST retry; malformed JSON uses status-safe message; password/token/cookie/raw body never enters error text.
- Standards impact: `MC-REUSE-001`, `MC-VALID-001`, `MC-JSON-001`, `MC-CONFIG-001`, `MC-SCOPE-001`, `MC-TEST-001` — preserve existing auth contract and no new state store.
- Literal rule enforcement: `Rule 2`, `Rule 6`, `Rule 7`, `Rule 11` — server validation/JSON/config key semantics and shared structure remain unchanged.
- Implementation pseudocode:

```typescript
const request = async <T>(path: string, init: RequestInit = {}): Promise<T> => {
  try { const response = await fetcher(`${baseUrl}${path}`, {...init, credentials: 'include', headers}); if (!response.ok) throw new GatewayAuthError(statusCode(response.status), path, response.status); return unwrap<T>(response) }
  catch (error) { if (error instanceof GatewayAuthError) throw error; throw new GatewayAuthError('NETWORK_OR_TIMEOUT', path, undefined, error) }
}
```

- Verification contribution: shared auth tests assert include credentials, exact CSRF header/body and sanitized errors.
- After this file: child login screens can show actionable categories; actual route availability still needs restart.

#### File 17 — `MODIFY scripts/unified-platform/start-local-stack.sh`

- Purpose: Preflight the public Gateway Engine login route after active release reuse/publication and report loopback origin.
- Symbols: `resolve_local_advertised_host`, new `preflight_gateway_login_route`.
- Repository evidence: `common.sh` already defaults `GATEWAY_BASE_URL` to 18180; start script has skip/publish branch but no login route check.
- Dependencies and consumers: supervisor, generated env/manifest and legacy publish script; no application data mutation.
- Why now: source defaults cannot repair an already-running process or stale active release.
- Contract/signature changes: safe GET `${GATEWAY_BASE_URL}/oauth2/login/csrf` with Origin, expect 200; output actual public origin/advertised host; retain explicit LAN override.
- Input/output and state mapping: environment -> URL/status; response body discarded; timeout/401/5xx returns nonzero with restart/re-publish guidance.
- Error and edge behavior: skip-release still preflights; no token/cookie/body persistence; 18140 is not substituted for login.
- Standards impact: `MC-UTIL-001`, `MC-JSON-001`, `MC-CONFIG-001`, `MC-SCOPE-001`, `MC-TEST-001` — existing curl/bash helpers and safe output only.
- Literal rule enforcement: `Rule 5`, `Rule 6`, `Rule 7`, `Rule 11` — allowed utilities, stable config keys, safe JSON boundary and current script tree.
- Implementation pseudocode:

```bash
preflight_gateway_login_route() {
  local status
  status="$(curl --max-time 5 -sS -o /dev/null -w '%{http_code}' -H "Origin: ${PLATFORM_PORTAL_URL}" "${GATEWAY_BASE_URL}/oauth2/login/csrf" 2>/dev/null || true)"
  [[ "${status}" == "200" ]] || unified_platform_fail "Gateway Engine login route is ${status:-timeout}; restart the stack and publish the current release"
}
preflight_gateway_login_route
printf 'Gateway public origin: %s; advertised host: %s\n' "${GATEWAY_BASE_URL}" "${local_advertised_host}"
```

- Verification contribution: `bash -n` and static text checks prove the gate; runtime HTTP is user-controlled.
- After this file: future starts fail clearly on stale public login route; current processes remain unchanged.

#### File 18 — `MODIFY scripts/unified-platform/status-local-stack.sh`

- Purpose: Show loopback/public origin and OAuth route status alongside process health.
- Symbols: `print_status`, startup summary and public login check.
- Repository evidence: status script already prints PID/process/actuator/web health but not advertised host or public OAuth status.
- Dependencies and consumers: `lib/common.sh` URL variables; operator terminal output; read-only curl.
- Why now: status must answer “是没重启还是 IP 不对” without exposing secrets.
- Contract/signature changes: add `GATEWAY_BASE_URL`, `PLATFORM_PORTAL_URL`, effective host and `/oauth2/login/csrf` code; no writes.
- Input/output and state mapping: URL -> status string; unreachable remains `unreachable`; no runtime state update.
- Error and edge behavior: timeout is labeled, control-plane health remains separate, no cookie/token/body output.
- Standards impact: `MC-UTIL-001`, `MC-CONFIG-001`, `MC-SCOPE-001`, `MC-TEST-001` — reuse read-only status helper and loopback defaults.
- Literal rule enforcement: `Rule 5`, `Rule 7`, `Rule 11` — existing shell utilities/config keys and operational structure only.
- Implementation pseudocode:

```bash
printf 'Portal URL: %s\nGateway public origin: %s\nAdvertised host: %s\n' "${PLATFORM_PORTAL_URL}" "${GATEWAY_BASE_URL}" "${UNIFIED_PLATFORM_ADVERTISED_HOST:-127.0.0.1}"
print_status gateway-login "${GATEWAY_BASE_URL}/oauth2/login/csrf"
login_status="$(unified_platform_http_code "${GATEWAY_BASE_URL}/oauth2/login/csrf")"
printf 'Gateway login route status: %s\n' "${login_status:-unreachable}"
```

- Verification contribution: shell syntax and output assertions prove diagnostic fields; no process is restarted.
- After this file: operators can distinguish 127 loopback source defaults from a stale LAN runtime.

- Validation working directory: repository root for shell checks; shared/Portal/IDP/RBAC3/Gateway/DDC package roots for focused tests.
- Verification command: `bash -n scripts/unified-platform/start-local-stack.sh scripts/unified-platform/status-local-stack.sh && cd egon-cola-platforms/egon-cola-platform-admin-web-shared && npm run test -- --run src/layout/EnterpriseLayout.test.tsx && npm run typecheck && cd ../egon-cola-platform-admin-portal && npm run test -- --run src/lifecycle/WujieChild.test.tsx src/pages/ChildRoutePage.test.tsx && npm run typecheck`
- Expected result: shell syntax, shared/Portal focused tests and typechecks pass; four child layout tests prove domain navigation; no process/release is mutated by validation.
- Completion criteria: REQ-001..REQ-005 source behavior is implemented/tested; all four embedded child menus remain; `git diff --check` is clean and no unrelated path is staged.
- Failure returns to: Files 2/4/5/6/7/8/9 for RED fixture issues; Files 3/10-15 for shell/Wujie wiring; Files 16-18 for auth/preflight diagnostics.
- Rollback: revert only Step 1 paths; leave runtime DB/env/cookies/secrets and user untracked document unchanged.
- Commit paths: `egon-cola-platforms/egon-cola-platform-admin-web-shared/src/layout/types.ts`, `egon-cola-platforms/egon-cola-platform-admin-web-shared/src/layout/EnterpriseLayout.test.tsx`, `egon-cola-platforms/egon-cola-platform-admin-web-shared/src/layout/EnterpriseLayout.tsx`, `egon-cola-platforms/egon-cola-platform-admin-portal/src/lifecycle/WujieChild.test.tsx`, `egon-cola-platforms/egon-cola-platform-admin-portal/src/pages/ChildRoutePage.test.tsx`, `egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-admin-web/src/app/App.test.tsx`, `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin-web/src/app/App.integration.test.tsx`, `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web/src/layouts/AdminLayout.test.tsx`, `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin-web/src/layouts/AdminLayout.test.tsx`, `egon-cola-platforms/egon-cola-platform-admin-portal/src/lifecycle/WujieChild.tsx`, `egon-cola-platforms/egon-cola-platform-admin-portal/src/pages/ChildRoutePage.tsx`, `egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-admin-web/src/app/AdminLayout.tsx`, `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin-web/src/app/router.tsx`, `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web/src/layouts/AdminLayout.tsx`, `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin-web/src/layouts/AdminLayout.tsx`, `egon-cola-platforms/egon-cola-platform-admin-web-shared/src/auth/gatewayAuthClient.ts`, `scripts/unified-platform/start-local-stack.sh`, `scripts/unified-platform/status-local-stack.sh`.
- Commit: `fix(platforms): restore embedded child shell and loopback login diagnostics`

### Step 2 — Complete RBAC3 IAM directory and user relation CRUD

- Requirements: `REQ-007`, `REQ-008`, `REQ-009`, `REQ-010`, `REQ-011`, `REQ-013`, `REQ-014`, `REQ-015`
- Dependencies: Step 1 committed; shared embedded Layout available; existing `directoryApi`/`assignmentApi` contracts unchanged.
- Baseline state: user page has user CRUD and a detail block but no organization/position/role relation composition; organization and position pages are read-only; relation API methods already exist; role assignment route is hidden and discoverable only by manually constructing its URL.
- Observable outcome: from `/iam/users` user detail, an administrator can view and mutate organization/position assignments and open the role assignment workbench; `/iam/organizations` and `/iam/positions` provide create/edit/archive actions with expected versions and clear states.
- End state: directory files/tests compile and focused tests prove existing API paths, mutation payloads, query invalidation, PermissionGuard and 403/409 handling. Role/permission/constraint/catalog work remains Step 3.
- Test-first gate: `Required` — current page tests prove reads only and would fail to find new action controls/API calls; add relation tests before component wiring.
- Manual Checks: `MC-ARCH-001`, `MC-REUSE-001`, `MC-VALID-001`, `MC-MODEL-001`, `MC-CONVERT-001`, `MC-JSON-001`, `MC-TIME-001`, `MC-PATTERN-001`, `MC-SCOPE-001`, `MC-TEST-001`, `MC-BLOCKER-001`
- Literal Rules: `Rule 2`, `Rule 5`, `Rule 6`, `Rule 9`, `Rule 10`, `Rule 11`
- Ordered files:

#### File 1 — `MODIFY egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin-web/src/features/directory/OrganizationPage.test.tsx`

- Purpose: Define organization create/edit/archive and version/error behavior before page changes.
- Symbols: add tests for `新增组织`, edit drawer, archive `DELETE`, `expectedVersion`, forbidden/manage guard and conflict state.
- Repository evidence: current test mocks FeatureApiClient/Rbac3Provider and only verifies the organization list endpoint.
- Dependencies and consumers: `OrganizationPage.tsx`, `directoryApi`, QueryClient and existing MemoryRouter wrapper.
- Why now: first RED contract for the currently read-only organization page.
- Contract/signature changes: assert existing `/api/rbac3/v1/iam/organizations` POST, `/{orgUnitId}` PUT and DELETE query; no new endpoint.
- Input/output and state mapping: form values -> API body; selected organization version -> update/delete expected version; success -> invalidate list.
- Error and edge behavior: required name/type/validFrom blocks submit; `validTo` empty maps null; 403/409 stays visible and does not close editor.
- Standards impact: `MC-VALID-001`, `MC-MODEL-001`, `MC-CONVERT-001`, `MC-JSON-001`, `MC-TIME-001`, `MC-SCOPE-001`, `MC-TEST-001` — assert exact existing command fields and ISO/null mapping.
- Literal rule enforcement: `Rule 2`, `Rule 6`, `Rule 10`, `Rule 11` — validation, JSON/time semantics and current feature path are preserved.
- Implementation pseudocode:

```typescript
it('creates an organization and sends its row version when archiving', async () => {
  render(<OrganizationPage />, {wrapper: manageOrganizationWrapper})
  await user.click(screen.getByRole('button', {name: '新增组织'}))
  await fillOrganizationForm({type: 'DEPARTMENT', name: '华东', parentId: null, validFrom: '2026-08-29T00:00:00Z', validTo: null})
  await user.click(screen.getByRole('button', {name: '保存'}))
  expect(request).toHaveBeenCalledWith('/api/rbac3/v1/iam/organizations', expect.objectContaining({method: 'POST'}))
})
```

- Verification contribution: test fails because current page has no `新增组织` or mutation call.
- After this file: organization RED contract exists; production remains read-only.

#### File 2 — `MODIFY egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin-web/src/features/directory/PositionPage.test.tsx`

- Purpose: Define position create/edit/archive behavior and required organization scope.
- Symbols: tests for `新增岗位`, create body, update/delete version, empty org validation and 409 rendering.
- Repository evidence: current test verifies only `/api/rbac3/v1/iam/positions` GET with `orgUnitId` query.
- Dependencies and consumers: `PositionPage.tsx`, directory API and existing test provider.
- Why now: second RED contract for read-only position page.
- Contract/signature changes: assert existing positions POST/PUT/DELETE paths and `expectedVersion`; no route change.
- Input/output and state mapping: orgUnitId/code/name/validFrom/to -> command; row version -> mutation query; success invalidates filtered list.
- Error and edge behavior: position cannot submit without orgUnitId/name/validFrom; empty validTo becomes null; 403/409 preserves modal.
- Standards impact: `MC-VALID-001`, `MC-MODEL-001`, `MC-CONVERT-001`, `MC-JSON-001`, `MC-TIME-001`, `MC-SCOPE-001`, `MC-TEST-001` — exact backend fields and time mapping are tested.
- Literal rule enforcement: `Rule 2`, `Rule 6`, `Rule 10`, `Rule 11` — required validation/JSON/time and current structure remain.
- Implementation pseudocode:

```typescript
it('requires an organization and archives a position with expectedVersion', async () => {
  render(<PositionPage />, {wrapper: managePositionWrapper})
  await user.click(screen.getByRole('button', {name: '新增岗位'}))
  await user.click(screen.getByRole('button', {name: '保存'}))
  expect(screen.getByText(/组织/)).toBeInTheDocument()
  expect(request).not.toHaveBeenCalledWith(expect.stringContaining('/positions/2001'), expect.objectContaining({method: 'DELETE'}))
})
```

- Verification contribution: RED exposes absent form/action and guards required organization.
- After this file: position RED contract exists.

#### File 3 — `MODIFY egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin-web/src/features/assignment/AssignmentPages.test.tsx`

- Purpose: Prove role assignment workbench remains the target for user relation navigation and mutation lifecycle.
- Symbols: add link/deep-route assertion and existing create/suspend/resume/revoke expected-version assertions.
- Repository evidence: `AssignmentListPage` already implements all four assignment operations and `authorization.routes.tsx` registers `/iam/users/:userId/role-assignments` as hidden.
- Dependencies and consumers: new UserRelationsPanel link, AssignmentListPage, assignment API and route descriptors.
- Why now: relation panel must point to a tested, existing workbench rather than duplicate role assignment logic.
- Contract/signature changes: preserve `Idempotency-Key`, `expectedAssignmentVersion`, `expectedUserAuthVersion`; no new endpoint.
- Input/output and state mapping: userId route param -> assignment list; selected row/status -> operation POST; success invalidates user assignment query.
- Error and edge behavior: revoked/expired rows cannot be changed; 403/409 retains state and shows retry; non-idempotent create has no automatic repeat.
- Standards impact: `MC-REUSE-001`, `MC-VALID-001`, `MC-JSON-001`, `MC-SCOPE-001`, `MC-TEST-001` — reuse existing assignment client and idempotency contract.
- Literal rule enforcement: `Rule 2`, `Rule 6`, `Rule 11` — preserve server validation/JSON and existing route structure.
- Implementation pseudocode:

```typescript
it('opens role assignments from the user subject route', async () => {
  render(<AssignmentListPage userId="7" />, {wrapper: assignmentWrapper})
  await screen.findByText(/用户 7 的角色任职/)
  await user.click(screen.getByRole('button', {name: '新增任职资格'}))
  expect(screen.getByRole('dialog')).toBeInTheDocument()
  expect(request).toHaveBeenCalledWith('/api/rbac3/v1/users/7/role-assignments', expect.anything())
})
```

- Verification contribution: keeps existing workbench green and fixes discoverability proof.
- After this file: assignment target contract is protected.

#### File 4 — `CREATE egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin-web/src/features/directory/UserRelationsPanel.test.tsx`

- Purpose: Define composed organization/position tabs and the role assignment link before the component exists.
- Symbols: `loadsThreeRelationTabs`, `assignsAndRevokesOrganization`, `assignsAndRevokesPosition`, `opensRoleAssignmentWorkbench`, forbidden/conflict tests.
- Repository evidence: `directory.api.ts` already has all organization/position relation methods; `assignment.api.ts` has list/create/change; user detail has userId/authVersion.
- Dependencies and consumers: new UserRelationsPanel, UserDirectoryPage, QueryClient and FeatureApiProvider.
- Why now: this is the central RED contract for the user/role/org/position relationship gap.
- Contract/signature changes: props use selected user ID and auth version; role link target is exact hidden route; no backend change.
- Input/output and state mapping: user -> three query keys; form -> POST; row assignmentId/version -> DELETE; success invalidates only affected relation key.
- Error and edge behavior: source-managed rows have no revoke; 403 disables manage controls but leaves read; 409 keeps selection and offers refresh; empty relation is a normal state.
- Standards impact: `MC-REUSE-001`, `MC-VALID-001`, `MC-JSON-001`, `MC-TIME-001`, `MC-PATTERN-001`, `MC-SCOPE-001`, `MC-TEST-001` — existing Query/Composition and version/time fields only.
- Literal rule enforcement: `Rule 2`, `Rule 6`, `Rule 9`, `Rule 10`, `Rule 11` — required fields precede calls, exact JSON/time are retained, relation variations use Composition.
- Implementation pseudocode:

```typescript
it('loads organization relations and maps revoke with row version', async () => {
  render(<UserRelationsPanel userId="7" authVersion={3} />, {wrapper: relationWrapper})
  await user.click(await screen.findByRole('tab', {name: '组织关系'}))
  expect(await screen.findByText('总部')).toBeInTheDocument()
  await user.click(screen.getByRole('button', {name: '撤销组织关系'}))
  expect(request).toHaveBeenCalledWith('/api/rbac3/v1/iam/users/7/organizations/assignment-1', expect.objectContaining({method: 'DELETE', query: {expectedVersion: 4}}))
  expect(screen.getByRole('link', {name: '打开角色任职工作台'})).toHaveAttribute('href', '/iam/users/7/role-assignments')
})
```

- Verification contribution: fails because the component/action/link do not exist.
- After this file: relation behavior is locked and intentionally RED.

#### File 5 — `MODIFY egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin-web/src/features/directory/directory.api.ts`

- Purpose: Stabilize relation view/command mappings needed by the new panel without changing endpoint contracts.
- Symbols: existing relation interfaces and `organizationAssignments`, `assignOrganization`, `revokeOrganization`, `positionAssignments`, `assignPosition`, `revokePosition` methods.
- Repository evidence: methods already exist but have no page consumers; fields match Controller views/commands.
- Dependencies and consumers: UserRelationsPanel, UserDirectoryPage, directory CRUD pages and FeatureApiClient envelope.
- Why now: component should call one typed client rather than construct URLs itself.
- Contract/signature changes: retain URLs; ensure nullable `validTo/sourceId/reason/ticketNo`, numeric/string IDs and `version` are represented; no new route.
- Input/output and state mapping: trim IDs/reason/ticket; empty validTo -> null; expected version comes from selected row; response passes through.
- Error and edge behavior: encode IDs; empty userId disables calls; server errors propagate; no automatic retry for POST assignment.
- Standards impact: `MC-REUSE-001`, `MC-VALID-001`, `MC-MODEL-001`, `MC-CONVERT-001`, `MC-JSON-001`, `MC-TIME-001`, `MC-SCOPE-001`, `MC-TEST-001` — exact existing API client and ISO/null mapping.
- Literal rule enforcement: `Rule 2`, `Rule 5`, `Rule 6`, `Rule 10`, `Rule 11` — validation/utility/JSON/time/tree requirements remain literal.
- Implementation pseudocode:

```typescript
assignPosition: (userId, values) => client.request<UserPositionAssignmentView>(
  `/api/rbac3/v1/iam/users/${encodeURIComponent(userId)}/positions`,
  {method: 'POST', body: {...values, validTo: values.validTo || null, reason: values.reason.trim(), ticketNo: values.ticketNo.trim()}},
)
revokePosition: (userId, assignmentId, expectedVersion) => client.request<null>(
  `/api/rbac3/v1/iam/users/${encodeURIComponent(userId)}/positions/${encodeURIComponent(assignmentId)}`,
  {method: 'DELETE', query: {expectedVersion}},
)
```

- Verification contribution: API calls remain exact and enable panel tests without a duplicate client.
- After this file: typed client is ready; page behavior remains RED.

#### File 6 — `CREATE egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin-web/src/features/directory/UserRelationsPanel.tsx`

- Purpose: Compose the three user relationship surfaces with existing Query, mutation and permission primitives.
- Symbols: `UserRelationsPanel`, organization/position query keys, assign/revoke mutations, tab renderers and `Link`.
- Repository evidence: adjacent RBAC3 pages use `useRbac3Authorization`, `useFeatureApi`, `useFeatureTenantContext`, `PageState`, `PermissionGuard`, AntD Table/Form/Modal.
- Dependencies and consumers: File 5 API, UserDirectoryPage File 9, assignment route and QueryClient.
- Why now: implements the File 4 RED contract with no global store or new backend layer.
- Contract/signature changes: props `{userId, authVersion}`; relation requests are enabled only for nonempty user ID and READY auth; role link uses encoded user ID.
- Input/output and state mapping: org form `{orgUnitId,validFrom,validTo,reason,ticketNo}`; position adds `positionId,primaryAssignment`; revoke uses row assignmentId/version; success invalidates matching key.
- Error and edge behavior: tab-local loading/empty/error; pending row lock; 403 visible and controls disabled; 409 preserves selected row/form; source-managed entries not manually revoked.
- Standards impact: `MC-REUSE-001`, `MC-VALID-001`, `MC-JSON-001`, `MC-TIME-001`, `MC-PATTERN-001`, `MC-SCOPE-001`, `MC-TEST-001` — existing Query/PermissionGuard/PageState and Composition only.
- Literal rule enforcement: `Rule 2`, `Rule 6`, `Rule 9`, `Rule 10`, `Rule 11` — form validation, JSON/time mapping, Composition and current structure are explicit.
- Implementation pseudocode:

```tsx
const organizations = useQuery({queryKey: orgKey, queryFn: () => api.organizationAssignments(userId), enabled: ready})
const assignOrganization = useMutation({mutationFn: (value: OrganizationForm) => api.assignOrganization(userId, {...value, validTo: value.validTo || null}), onSuccess: () => queryClient.invalidateQueries({queryKey: orgKey})})
const revokeOrganization = useMutation({mutationFn: (row: UserOrganizationAssignmentView) => api.revokeOrganization(userId, row.assignmentId, row.version), onSuccess: () => queryClient.invalidateQueries({queryKey: orgKey})})
return <Tabs items={[organizationTab, positionTab, {key: 'roles', label: '角色任职', children: <Link to={`/iam/users/${encodeURIComponent(userId)}/role-assignments`}>打开角色任职工作台</Link>}]} />
```

- Verification contribution: File 4 turns GREEN for relation reads/mutations/link and error branches.
- After this file: relation panel is independently usable; user page still needs mounting.

#### File 7 — `MODIFY egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin-web/src/features/directory/OrganizationPage.tsx`

- Purpose: Turn the organization list/detail drawer into a version-safe CRUD workbench.
- Symbols: editor state/form, `createOrganization`, `updateOrganization`, `deleteOrganization` mutations, `PermissionGuard` actions.
- Repository evidence: current page already queries list, uses search params/Form, PageState, Table and Drawer; directory API methods are present.
- Dependencies and consumers: File 1 tests, File 5 API, shared PageState/PermissionGuard and position page’s organization IDs.
- Why now: GREEN implementation for organization RED test.
- Contract/signature changes: form maps type/code/name/parentId/externalId/validFrom/validTo; update/delete carries row `version`; no route change.
- Input/output and state mapping: selected row -> form -> POST/PUT/DELETE -> close/reset -> invalidate current parent query; selected detail remains until success.
- Error and edge behavior: manage guard hides actions; duplicate/validation/403/409 displays error without silent close; archive confirmation prevents accidental delete; `snapshotId` rows are labeled source.
- Standards impact: `MC-REUSE-001`, `MC-VALID-001`, `MC-MODEL-001`, `MC-CONVERT-001`, `MC-JSON-001`, `MC-TIME-001`, `MC-SCOPE-001`, `MC-TEST-001` — reuse exact commands and existing form/state components.
- Literal rule enforcement: `Rule 2`, `Rule 6`, `Rule 10`, `Rule 11` — validation/JSON/time and current feature-first tree are retained.
- Implementation pseudocode:

```tsx
const save = useMutation({mutationFn: (values: OrganizationForm) => selected
  ? api.updateOrganization(selected.orgUnitId, {...values, parentId: values.parentId || null, validTo: values.validTo || null, expectedVersion: selected.version})
  : api.createOrganization({...values, parentId: values.parentId || null, validTo: values.validTo || null}),
  onSuccess: () => { closeEditor(); void queryClient.invalidateQueries({queryKey}) }})
```

- Verification contribution: organization tests become GREEN for actions, payload and refresh.
- After this file: organization CRUD is visible and version-aware.

#### File 8 — `MODIFY egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin-web/src/features/directory/PositionPage.tsx`

- Purpose: Turn position list/detail into a CRUD workbench requiring an organization.
- Symbols: editor form, `createPosition`, `updatePosition`, `deletePosition` mutations and PermissionGuard action column.
- Repository evidence: page already filters by `orgUnitId`, uses Table/Drawer/Form/PageState; API has all three mutation methods.
- Dependencies and consumers: File 2 tests, File 5 API and organization IDs from directory context.
- Why now: GREEN implementation for position RED test after client mapping.
- Contract/signature changes: create maps code/name/orgUnitId/externalId/validFrom/validTo; update/delete uses `positionId/version`; no path change.
- Input/output and state mapping: filter query -> list; selected row -> form; success invalidates `['rbac3','positions']` and closes editor.
- Error and edge behavior: orgUnitId/name/time required; 403 hides write actions; 409 keeps modal; archived/source rows not silently overwritten.
- Standards impact: `MC-REUSE-001`, `MC-VALID-001`, `MC-MODEL-001`, `MC-CONVERT-001`, `MC-JSON-001`, `MC-TIME-001`, `MC-SCOPE-001`, `MC-TEST-001` — existing API/Form/Table only.
- Literal rule enforcement: `Rule 2`, `Rule 6`, `Rule 10`, `Rule 11` — exact validation/JSON/time and current structure.
- Implementation pseudocode:

```tsx
const save = useMutation({mutationFn: (values: PositionForm) => selected
  ? api.updatePosition(selected.positionId, {...values, validTo: values.validTo || null, expectedVersion: selected.version})
  : api.createPosition({...values, validTo: values.validTo || null}),
  onSuccess: () => { closeEditor(); void queryClient.invalidateQueries({queryKey: positionsKey}) }})
```

- Verification contribution: position tests become GREEN and retain org scope query.
- After this file: position CRUD is visible and version-aware.

#### File 9 — `MODIFY egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin-web/src/features/directory/UserDirectoryPage.tsx`

- Purpose: Mount the relation panel below user detail while preserving existing user CRUD/status/archive.
- Symbols: render `UserRelationsPanel` with `displayUser.userId` and `displayUser.authVersion`; invalidate relation queries on archive.
- Repository evidence: detail `Descriptions` is rendered only when `displayUser`; user list mutations already invalidate detail/list; role assignment route is registered separately.
- Dependencies and consumers: File 6 panel, existing user API/PermissionGuard and QueryClient.
- Why now: connects the independently tested panel to the user subject entry.
- Contract/signature changes: no backend/API change; no user password/token field is added; selected user ID remains encoded by panel/API.
- Input/output and state mapping: detail -> relation queries; user archive clears detail and panel; user update/status refreshes user/relation state.
- Error and edge behavior: detail 403/404 prevents relation calls; relation error is local; archived user cannot submit new relation; current user list remains usable.
- Standards impact: `MC-REUSE-001`, `MC-VALID-001`, `MC-JSON-001`, `MC-SCOPE-001`, `MC-TEST-001` — existing user contract and composed panel only.
- Literal rule enforcement: `Rule 2`, `Rule 6`, `Rule 9`, `Rule 11` — preserve server validation/JSON and Composition/current structure.
- Implementation pseudocode:

```tsx
{displayUser && <>
  <Descriptions bordered column={2}>...</Descriptions>
  <UserRelationsPanel userId={displayUser.userId} authVersion={displayUser.authVersion} />
</>}
```

- Verification contribution: user-page test observes tabs/link only after detail resolves and removes them after archive.
- After this file: Step 2 directory and user relationship UI is complete.

- Validation working directory: `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin-web`.
- Verification command: `npm run test -- --run src/features/directory/OrganizationPage.test.tsx src/features/directory/PositionPage.test.tsx src/features/directory/UserRelationsPanel.test.tsx src/features/assignment/AssignmentPages.test.tsx && npm run typecheck && npm run build`
- Expected result: focused tests pass; exact existing paths/methods/version queries are observed; RBAC3 typecheck/build pass.
- Completion criteria: users can discover and mutate org/position/role relationships from IAM, organization/position pages are CRUD-capable, and no new endpoint/dependency/migration exists.
- Failure returns to: Files 1-4 for fixtures; File 5 for API mapping; File 6 for relation state/cache; Files 7-9 for page wiring.
- Rollback: revert only Step 2 paths; tests/build do not mutate runtime or persistent application data.
- Commit paths: `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin-web/src/features/directory/OrganizationPage.test.tsx`, `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin-web/src/features/directory/PositionPage.test.tsx`, `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin-web/src/features/assignment/AssignmentPages.test.tsx`, `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin-web/src/features/directory/UserRelationsPanel.test.tsx`, `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin-web/src/features/directory/directory.api.ts`, `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin-web/src/features/directory/UserRelationsPanel.tsx`, `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin-web/src/features/directory/OrganizationPage.tsx`, `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin-web/src/features/directory/PositionPage.tsx`, `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin-web/src/features/directory/UserDirectoryPage.tsx`.
- Commit: `feat(rbac3-admin-web): complete IAM directory relations`

### Step 3 — Complete RBAC3 IAM role, permission, resource, catalog and policy workbenches

- Requirements: `REQ-006`, `REQ-007`, `REQ-008`, `REQ-009`, `REQ-010`, `REQ-011`, `REQ-013`, `REQ-014`, `REQ-015`
- Dependencies: Step 1 committed; Step 2 user relation entry committed; existing `roleApi`, `applicationApi`, `managementPolicyApi` and read-only `constraintApi` verified.
- Baseline state: role CRUD/inheritance/impact exists but has no role-resource entry; permission page uses list row as detail and does not consume `PermissionController.find`; tenant applications lack detail; business catalog page is absent; constraint page has only four GET queries; management policy page lacks detail/capability/target reads.
- Observable outcome: IAM menu exposes business catalog and all relevant role/resource/permission/policy actions; detail and policy writes use actual Controller paths and version/idempotency contracts.
- End state: RBAC3 focused tests prove role resource links, permission/application detail, business catalog route, constraint POST/PUT fields and management-policy reads. No Java/DB changes.
- Test-first gate: `Required` — existing tests do not observe the new action paths; add assertions before production wiring.
- Manual Checks: `MC-ARCH-001`, `MC-REUSE-001`, `MC-VALID-001`, `MC-MODEL-001`, `MC-CONVERT-001`, `MC-JSON-001`, `MC-TIME-001`, `MC-PATTERN-001`, `MC-SCOPE-001`, `MC-TEST-001`, `MC-BLOCKER-001`
- Literal Rules: `Rule 2`, `Rule 5`, `Rule 6`, `Rule 9`, `Rule 10`, `Rule 11`
- Ordered files:

#### File 1 — `MODIFY egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin-web/src/features/role/RolePages.test.tsx`

- Purpose: Define role-resource discoverability and version-safe role relationship actions before page changes.
- Symbols: resource authorization link/action assertion; existing create/edit/inheritance/impact tests remain.
- Repository evidence: `RoleResourceGrantPage` and hidden route already exist; `RoleGraphPage` currently renders only impact/edit/inheritance actions.
- Dependencies and consumers: `RoleGraphPage.tsx`, existing `roleApi.resources/replaceResources`, governance route descriptor.
- Why now: role relation gap is the first authorization RED contract.
- Contract/signature changes: assert link `/iam/roles/:roleId/resources` guarded by `system:role-resource:read`; no endpoint change.
- Input/output and state mapping: role row -> resource route -> tree/version; direct resource save remains existing PUT.
- Error and edge behavior: derived/inherited nodes remain disabled; 403/409 is visible; role create/edit behavior remains unchanged.
- Standards impact: `MC-REUSE-001`, `MC-VALID-001`, `MC-JSON-001`, `MC-PATTERN-001`, `MC-SCOPE-001`, `MC-TEST-001` — existing page/guard/client only.
- Literal rule enforcement: `Rule 2`, `Rule 6`, `Rule 9`, `Rule 11` — preserve authorization/JSON and use existing role/resource Composition.
- Implementation pseudocode:

```typescript
it('exposes resource authorization from each role', async () => {
  render(<RoleGraphPage />, {wrapper: roleManageWrapper})
  await screen.findByText('订单管理员')
  expect(screen.getByRole('link', {name: '资源授权'})).toHaveAttribute('href', '/iam/roles/role-1/resources')
})
```

- Verification contribution: fails because current role cards have no resource link.
- After this file: role relation RED contract exists.

#### File 2 — `MODIFY egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin-web/src/features/application/ApplicationPages.test.tsx`

- Purpose: Define tenant application detail and existing resource/field/mapping regression behavior.
- Symbols: detail Drawer GET assertion for `/tenant-applications/{applicationId}`; retain admission/status/remove/resource tests.
- Repository evidence: test already renders `ApplicationListPage`, `ResourceCatalogPage`, `FieldDefinitionPage` with FeatureApi mock; Java ApplicationController has detail GET.
- Dependencies and consumers: application API/page files in later blocks.
- Why now: application detail is a real external method currently absent from UI.
- Contract/signature changes: selected row ID -> existing GET; expected version stays on status/remove.
- Input/output and state mapping: row -> detail query -> Drawer; close does not clear list; mutation invalidates list/detail.
- Error and edge behavior: 403/404 stays in Drawer PageState; empty list remains valid; no DDC direct access.
- Standards impact: `MC-VALID-001`, `MC-JSON-001`, `MC-SCOPE-001`, `MC-TEST-001` — exact path/JSON and error assertions.
- Literal rule enforcement: `Rule 2`, `Rule 6`, `Rule 11` — preserve server validation/JSON and current feature structure.
- Implementation pseudocode:

```typescript
it('loads tenant application detail through the existing controller', async () => {
  render(<ApplicationListPage />, {wrapper: applicationWrapper})
  await user.click(await screen.findByRole('button', {name: '查看详情'}))
  expect(request).toHaveBeenCalledWith('/api/rbac3/v1/iam/tenant-applications/71', expect.anything())
})
```

- Verification contribution: RED until detail action/query is added.
- After this file: application detail contract is fixed.

#### File 3 — `MODIFY egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin-web/src/features/permission/PermissionCatalogPage.test.tsx`

- Purpose: Define direct permission detail endpoint consumption and status/create regression behavior.
- Symbols: selected permission -> `GET /api/rbac3/v1/iam/permissions/{id}` assertion; 403/409 and list refresh cases.
- Repository evidence: current page opens a Drawer from list row; `application.api.ts` has list/create/status but no find method; Java PermissionController has GET detail.
- Dependencies and consumers: PermissionCatalogPage and application API changes.
- Why now: prevents a list object from being mistaken for Controller detail coverage.
- Contract/signature changes: no request body; encoded permission ID; detail response has version/status/source fields.
- Input/output and state mapping: row click -> detail query -> Drawer; close clears selection; status mutation invalidates list.
- Error and edge behavior: detail loading/error/404 is local; secret/source payload fields are not rendered.
- Standards impact: `MC-REUSE-001`, `MC-VALID-001`, `MC-JSON-001`, `MC-SCOPE-001`, `MC-TEST-001` — use existing Query/PageState.
- Literal rule enforcement: `Rule 2`, `Rule 6`, `Rule 11` — preserve validation/JSON and current client/page structure.
- Implementation pseudocode:

```typescript
it('requests permission detail instead of using only the selected list row', async () => {
  render(<PermissionCatalogPage />, {wrapper: permissionWrapper})
  await user.click(await screen.findByText('orders:read'))
  expect(request).toHaveBeenCalledWith('/api/rbac3/v1/iam/permissions/91', expect.anything())
})
```

- Verification contribution: RED against current row-only Drawer.
- After this file: permission detail contract is fixed.

#### File 4 — `MODIFY egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin-web/src/features/constraint/ConstraintPage.test.tsx`

- Purpose: Define ConstraintController write mappings, DSD validation and version conflict behavior.
- Symbols: `createSod/updateSod`, prerequisite/cardinality, data/field/operation create/update test assertions.
- Repository evidence: current tests verify four GET roots and DSD selection; Java controller has POST/PUT methods for all listed rule types.
- Dependencies and consumers: constraint API/Page files, existing `validateDsdRoleSelection`.
- Why now: constraint page is currently read-only despite external management methods.
- Contract/signature changes: assert exact policy root paths, request fields, row expected versions; no endpoint invention.
- Input/output and state mapping: form -> JSON command -> mutation result -> invalidate tab query; dates are ISO/null.
- Error and edge behavior: invalid DSD selection blocks call; 403/422/409 remains visible; POST is not retried automatically.
- Standards impact: `MC-VALID-001`, `MC-MODEL-001`, `MC-CONVERT-001`, `MC-JSON-001`, `MC-TIME-001`, `MC-PATTERN-001`, `MC-SCOPE-001`, `MC-TEST-001` — exact DTO fields and four-tab Composition.
- Literal rule enforcement: `Rule 2`, `Rule 6`, `Rule 9`, `Rule 10`, `Rule 11` — validation, JSON/time, pattern and current structure are tested.
- Implementation pseudocode:

```typescript
it('posts an SSD set with the selected roles and expected dates', async () => {
  render(<ConstraintPage />, {wrapper: constraintManageWrapper})
  await user.click(screen.getByRole('button', {name: '新建 SSD/DSD'}))
  await fillSodForm({setCode: 'maker-checker', constraintType: 'SSD', memberRoleIds: ['1'], validFrom: '2026-08-29T00:00:00Z', validTo: null})
  await user.click(screen.getByRole('button', {name: '保存'}))
  expect(request).toHaveBeenCalledWith('/api/rbac3/v1/iam/policies/sod-sets', expect.objectContaining({method: 'POST'}))
})
```

- Verification contribution: RED against absent buttons/POST/PUT methods.
- After this file: policy mutation contract is fixed.

#### File 5 — `MODIFY egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin-web/src/features/management-policy/ManagementPolicyPage.test.tsx`

- Purpose: Define management policy detail/capability/manageable-target read coverage while preserving CRUD tests.
- Symbols: tests for detail Drawer, `/management-capabilities/me`, `/manageable-users`, `/manageable-roles` and existing If-Match/idempotency calls.
- Repository evidence: page has list/editor/disable; API only has list/create/update/disable; Java controller exposes all six GET methods.
- Dependencies and consumers: management API/page changes and existing editor.
- Why now: real external read methods need visible use before audit.
- Contract/signature changes: exact GET paths; no mutation contract changes.
- Input/output and state mapping: selected policy -> detail; page readiness -> capability summary; editor open -> target queries.
- Error and edge behavior: target empty is valid; capability 403 does not hide list; detail 404 stays local.
- Standards impact: `MC-REUSE-001`, `MC-VALID-001`, `MC-JSON-001`, `MC-SCOPE-001`, `MC-TEST-001` — existing Query/editor/error components.
- Literal rule enforcement: `Rule 2`, `Rule 6`, `Rule 11` — existing validation/JSON/header semantics and current feature tree.
- Implementation pseudocode:

```typescript
it('loads policy capability and selected policy detail', async () => {
  render(<ManagementPolicyPage />, {wrapper: policyWrapper})
  await screen.findByText('授权代理策略')
  await user.click(screen.getByRole('button', {name: '查看详情'}))
  expect(request).toHaveBeenCalledWith('/api/rbac3/v1/management-policies/policy-1', expect.anything())
  expect(request).toHaveBeenCalledWith('/api/rbac3/v1/management-capabilities/me', expect.anything())
})
```

- Verification contribution: RED until detail/capability queries are wired.
- After this file: management policy read contract is fixed.

#### File 6 — `MODIFY egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin-web/src/features/role/RoleGraphPage.tsx`

- Purpose: Expose existing `RoleResourceGrantPage` from each role.
- Symbols: row `Link`/navigate action guarded by `system:role-resource:read`; retain role CRUD/inheritance/impact.
- Repository evidence: hidden route and resource page already exist; role cards currently have three actions only.
- Dependencies and consumers: role API/resource page/governance routes and Step 1 embedded sidebar.
- Why now: minimum GREEN implementation for File 1 without duplicate resource logic.
- Contract/signature changes: only route link carrying encoded role ID; no API change.
- Input/output and state mapping: role row -> route -> resource tree reads role version/direct/derived/inherited IDs.
- Error and edge behavior: guard read permission; derived/inherited nodes remain disabled on target page; 403/404 route guard remains.
- Standards impact: `MC-REUSE-001`, `MC-VALID-001`, `MC-JSON-001`, `MC-PATTERN-001`, `MC-SCOPE-001`, `MC-TEST-001` — reuse resource Composition and guard.
- Literal rule enforcement: `Rule 2`, `Rule 6`, `Rule 9`, `Rule 11` — preserve server auth/JSON and current feature structure.
- Implementation pseudocode:

```tsx
<PermissionGuard permission="system:role-resource:read">
  <Link to={`/iam/roles/${encodeURIComponent(role.roleId)}/resources`}>资源授权</Link>
</PermissionGuard>
```

- Verification contribution: RolePages test turns GREEN and target page tests preserve direct/derived/inherited behavior.
- After this file: role resource relation is discoverable.

#### File 7 — `MODIFY egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin-web/src/features/application/application.api.ts`

- Purpose: Add client methods for existing tenant application and permission detail GET mappings.
- Symbols: `application(applicationId)`, `permission(id)` (or repository-equivalent names), existing list/create/status methods.
- Repository evidence: Java controllers expose both GET detail endpoints; client has no methods for them.
- Dependencies and consumers: ApplicationListPage/PermissionCatalogPage, FeatureApiClient envelope and Query.
- Why now: page code needs a single typed client and exact URL encoding.
- Contract/signature changes: GET only, encoded ID, return existing `TenantApplicationView`/`PermissionView` shape; no new DTO.
- Input/output and state mapping: selected ID -> response; error propagates through existing client.
- Error and edge behavior: empty ID is not called; 403/404 remains client error; no sensitive field widening.
- Standards impact: `MC-REUSE-001`, `MC-VALID-001`, `MC-MODEL-001`, `MC-CONVERT-001`, `MC-JSON-001`, `MC-SCOPE-001`, `MC-TEST-001` — reuse existing View types/client.
- Literal rule enforcement: `Rule 2`, `Rule 6`, `Rule 11` — preserve validation/JSON and no new model/converter layer.
- Implementation pseudocode:

```typescript
application: (applicationId: string) => client.request<TenantApplicationView>(
  `/api/rbac3/v1/iam/tenant-applications/${encodeURIComponent(applicationId)}`,
),
permission: (id: string) => client.request<PermissionView>(
  `/api/rbac3/v1/iam/permissions/${encodeURIComponent(id)}`,
),
```

- Verification contribution: API/page tests observe exact Controller paths.
- After this file: detail client contract is available.

#### File 8 — `MODIFY egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin-web/src/features/permission/PermissionCatalogPage.tsx`

- Purpose: Load actual permission detail in the existing Drawer while retaining create/status CRUD.
- Symbols: selected permission ID, `useQuery` detail, Drawer `PageState`.
- Repository evidence: current row click stores whole `PermissionView` and renders list data; API File 7 adds the Controller detail call.
- Dependencies and consumers: File 3 test/File 7 API, existing application selector/query key and PermissionGuard.
- Why now: GREEN implementation for permission detail RED.
- Contract/signature changes: selection stores ID; detail query enabled only for nonempty ID and READY status; list/status/create unchanged.
- Input/output and state mapping: selected ID -> GET -> detail fields; close clears ID; status success invalidates list/detail.
- Error and edge behavior: loading/error/404 within Drawer; no raw source secret; row remains visible after detail error.
- Standards impact: `MC-REUSE-001`, `MC-VALID-001`, `MC-JSON-001`, `MC-SCOPE-001`, `MC-TEST-001` — existing Query/PageState/Guard.
- Literal rule enforcement: `Rule 2`, `Rule 6`, `Rule 11` — preserve validation/JSON and current feature architecture.
- Implementation pseudocode:

```tsx
const detail = useQuery({queryKey: ['rbac3','permission',tenant,selectedId], queryFn: () => api.permission(selectedId!), enabled: ready && Boolean(selectedId)})
<Drawer open={Boolean(selectedId)} onClose={() => setSelectedId(undefined)} title="权限详情">
  <PageState loading={detail.isPending} error={detail.error} empty={!detail.data} onRetry={() => void detail.refetch()}>{detail.data && <Descriptions ... />}</PageState>
</Drawer>
```

- Verification contribution: Permission test turns GREEN and proves list/detail distinction.
- After this file: PermissionController detail is represented.

#### File 9 — `MODIFY egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin-web/src/features/application/ApplicationListPage.tsx`

- Purpose: Add tenant application detail Drawer and keep admission/status/remove CRUD intact.
- Symbols: selected application ID, detail query, `查看详情` action, expected-version mutation guards.
- Repository evidence: page already has list/form/status/remove and application view fields; controller has detail GET.
- Dependencies and consumers: File 2 test/File 7 API, PageState, QueryClient/PermissionGuard.
- Why now: GREEN implementation for application detail RED.
- Contract/signature changes: no write changes; selected row ID only; GET detail uses existing path.
- Input/output and state mapping: row -> detail GET -> Drawer; list mutation invalidates list/detail.
- Error and edge behavior: detail 403/404 local; remove still confirms; no inferred DDC fields.
- Standards impact: `MC-REUSE-001`, `MC-VALID-001`, `MC-JSON-001`, `MC-SCOPE-001`, `MC-TEST-001` — existing UI/API and expected version.
- Literal rule enforcement: `Rule 2`, `Rule 6`, `Rule 11` — preserve server validation/JSON and current page structure.
- Implementation pseudocode:

```tsx
const detail = useQuery({queryKey: ['rbac3','tenant-application',tenant,selectedId], queryFn: () => api.application(selectedId!), enabled: ready && Boolean(selectedId)})
<Button size="small" onClick={(event) => {event.stopPropagation(); setSelectedId(row.applicationId)}}>查看详情</Button>
const detailState = detail.isPending ? 'loading' : detail.error ? 'error' : 'ready'
renderDetailState(detailState, detail.data)
```

- Verification contribution: ApplicationPages test turns GREEN and existing write actions remain covered.
- After this file: tenant application detail method is consumed.

#### File 10 — `CREATE egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin-web/src/features/business/business.api.ts`

- Purpose: Provide the existing BusinessCatalogController read methods through FeatureApiClient.
- Symbols: `BusinessCatalogView`, `ApplicationCatalogView`, `businesses(keyword)`, `applications(ddcBusinessId, keyword)`.
- Repository evidence: Java controller has `/api/rbac3/v1/iam/businesses` and `/{ddcBusinessId}/applications`; no client/page exists.
- Dependencies and consumers: new BusinessCatalogPage, FeatureApiClient and PageState.
- Why now: page needs exact API paths before creation.
- Contract/signature changes: keyword is optional and omitted when blank; response fields map only controller VO fields.
- Input/output and state mapping: keyword -> business list; business ID -> application list.
- Error and edge behavior: empty business ID disables applications call; server errors propagate; no DDC direct access.
- Standards impact: `MC-REUSE-001`, `MC-VALID-001`, `MC-MODEL-001`, `MC-CONVERT-001`, `MC-JSON-001`, `MC-SCOPE-001`, `MC-TEST-001` — existing client/View pattern, no new dependency.
- Literal rule enforcement: `Rule 2`, `Rule 5`, `Rule 6`, `Rule 11` — input/query validation, allowed utilities, JSON and current structure.
- Implementation pseudocode:

```typescript
export const businessApi = (client: FeatureApiClient) => ({
  businesses: (keyword?: string) => client.request<readonly BusinessCatalogView[]>('/api/rbac3/v1/iam/businesses', {query: {keyword: keyword?.trim() || undefined}}),
  applications: (businessId: string, keyword?: string) => client.request<readonly ApplicationCatalogView[]>(`/api/rbac3/v1/iam/businesses/${encodeURIComponent(businessId)}/applications`, {query: {keyword: keyword?.trim() || undefined}}),
})
```

- Verification contribution: Business page test can assert both exact methods.
- After this file: business catalog client exists with no mutation.

#### File 11 — `CREATE egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin-web/src/features/business/BusinessCatalogPage.test.tsx`

- Purpose: Define business/application catalog query and selection states before page implementation.
- Symbols: list query, selected-business application query, empty/error behavior.
- Repository evidence: existing RBAC3 page tests use QueryClient/Rbac3Provider/FeatureApiProvider and mock `request` by path.
- Dependencies and consumers: BusinessCatalogPage and business API File 10.
- Why now: RED proof for the new page/controller coverage.
- Contract/signature changes: assert `/businesses?keyword` query and `/{ddcBusinessId}/applications` query; no write.
- Input/output and state mapping: keyword -> table; row select -> drawer query; no selection -> no second call.
- Error and edge behavior: empty lists show normal PageState; 403/error remains retryable.
- Standards impact: `MC-REUSE-001`, `MC-VALID-001`, `MC-JSON-001`, `MC-SCOPE-001`, `MC-TEST-001` — existing test harness only.
- Literal rule enforcement: `Rule 2`, `Rule 6`, `Rule 11` — preserve query input/JSON and current test structure.
- Implementation pseudocode:

```typescript
it('loads applications only after selecting a DDC business', async () => {
  render(<BusinessCatalogPage />, {wrapper: businessWrapper})
  await user.click(await screen.findByText('交易域'))
  expect(await screen.findByText('订单应用')).toBeInTheDocument()
  expect(request).toHaveBeenCalledWith('/api/rbac3/v1/iam/businesses/business-1/applications', expect.anything())
})
```

- Verification contribution: RED until page/API exist.
- After this file: catalog behavior is specified.

#### File 12 — `CREATE egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin-web/src/features/business/BusinessCatalogPage.tsx`

- Purpose: Render the business catalog and selected business applications as a read-only IAM resource-directory page.
- Symbols: `BusinessCatalogPage`, keyword state, business/application queries, Table/Drawer/PageState.
- Repository evidence: existing catalog pages use Card/Table/Drawer/PageState and FeatureApiClient; controller is query-only.
- Dependencies and consumers: File 10 API/File 11 test, governance route and resource definition.
- Why now: GREEN implementation for the catalog RED test.
- Contract/signature changes: no mutation; route component accepts no security context beyond existing Rbac3 provider.
- Input/output and state mapping: keyword trimmed -> businesses; selected `ddcBusinessId` -> applications; Drawer close clears selection.
- Error and edge behavior: no selection avoids network call; query error has retry; 403 route guard remains; no direct DDC access.
- Standards impact: `MC-REUSE-001`, `MC-VALID-001`, `MC-JSON-001`, `MC-SCOPE-001`, `MC-TEST-001` — existing PageState/Table/Query only.
- Literal rule enforcement: `Rule 2`, `Rule 6`, `Rule 11` — preserve input validation/JSON and feature-first structure.
- Implementation pseudocode:

```tsx
const businesses = useQuery({queryKey: ['rbac3','businesses',tenant,keyword], queryFn: () => api.businesses(keyword), enabled: ready})
const applications = useQuery({queryKey: ['rbac3','business-applications',tenant,selectedId], queryFn: () => api.applications(selectedId!), enabled: ready && Boolean(selectedId)})
return <Card title="业务目录"><Input.Search onSearch={(value) => setKeyword(value.trim())}/><Table dataSource={businesses.data ?? []} onRow={(row) => ({onClick: () => setSelectedId(row.ddcBusinessId)})}/><Drawer open={Boolean(selectedId)}><PageState loading={applications.isPending} error={applications.error}>{applications.data && <Table dataSource={applications.data}/>}</PageState></Drawer></Card>
```

- Verification contribution: business catalog test turns GREEN and route build proves component import.
- After this file: both BusinessCatalogController reads have visible UI.

#### File 13 — `MODIFY egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin-web/src/features/governance.routes.tsx`

- Purpose: Register the business catalog route under the existing IAM resource-directory navigation group.
- Symbols: `governanceRouteDescriptors` new `business-catalog` descriptor and component import.
- Repository evidence: route descriptors already group tenant applications/resources/fields/permissions and `visibleNavigation` consumes them.
- Dependencies and consumers: BusinessCatalogPage File 12, ApplicationRouter, resource registry.
- Why now: page cannot be reached from Portal/IAM without route descriptor.
- Contract/signature changes: add only existing external-read route, with `system:business:read` permission and no parent route behavior change.
- Input/output and state mapping: route -> page; about permission -> guard/menu visibility.
- Error and edge behavior: manual path without permission returns existing 403; parent is navigation-only.
- Standards impact: `MC-ARCH-001`, `MC-REUSE-001`, `MC-VALID-001`, `MC-SCOPE-001`, `MC-TEST-001` — existing descriptor/guard pattern.
- Literal rule enforcement: `Rule 2`, `Rule 6`, `Rule 11` — preserve permission/JSON semantics and current route structure.
- Implementation pseudocode:

```tsx
{key: 'business-catalog', path: '/iam/businesses', title: '业务目录', permission: 'system:business:read', componentKey: 'rbac3-business-catalog', component: BusinessCatalogPage, navigationOrder: 42}
const route = governanceRouteDescriptors.find((item) => item.key === 'business-catalog')
assert route?.permission === 'system:business:read'
```

- Verification contribution: route test/resource report observes the page.
- After this file: business catalog route is reachable.

#### File 14 — `MODIFY egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin-web/src/app/resourceDefinitions.json`

- Purpose: Place business catalog under IAM’s second-level `资源目录` tree.
- Symbols: existing IAM/resource catalog node and new business child resource definition.
- Repository evidence: JSON is the local resource/menu registry used by RBAC3 resource report; current entries include tenant apps/resources/fields/permissions.
- Dependencies and consumers: route descriptor, `report:resources`, backend registration conventions.
- Why now: route alone does not satisfy the user’s IAM menu organization requirement.
- Contract/signature changes: add one read resource with path `/iam/businesses`, permission `system:business:read`; preserve group structure and no duplicate top-level route.
- Input/output and state mapping: JSON definition -> navigation/report; no business data.
- Error and edge behavior: schema/report validation catches duplicate key/path; unauthorized user sees pruned node.
- Standards impact: `MC-REUSE-001`, `MC-JSON-001`, `MC-CONFIG-001`, `MC-SCOPE-001`, `MC-TEST-001` — existing JSON registry/schema and no new config key.
- Literal rule enforcement: `Rule 6`, `Rule 7`, `Rule 11` — valid JSON, key structure and existing resource-tree architecture are preserved.
- Implementation pseudocode:

```json
{"key":"business-catalog","parentKey":"resource-catalog","path":"/iam/businesses","permission":"system:business:read","type":"PAGE"}
// Validate the definition against the existing resource registry schema.
// The definition remains a child of IAM resource-catalog and has no write action.
```

- Verification contribution: `npm run report:resources` proves JSON shape and IAM nesting.
- After this file: business catalog is organized under IAM resource directory.

#### File 15 — `MODIFY egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin-web/src/features/constraint/constraint.api.ts`

- Purpose: Map all approved ConstraintController POST/PUT methods to typed client calls.
- Symbols: request interfaces and `create/updateSod`, `savePrerequisites`, `saveCardinality`, `create/updateDataRule`, `create/updateFieldRule`, `create/updateOperationSodRule`.
- Repository evidence: current client has only `sodSets/dataRules/fieldRules/operationSodRules`; Java DTO imports/method annotations define write paths.
- Dependencies and consumers: ConstraintPage, FeatureApiClient, existing View types.
- Why now: page editor needs exact body/path methods; no controller change.
- Contract/signature changes: preserve exact DTO fields and expected versions listed in Spec §7.3.3; return `MutationResult`/existing views as controller returns.
- Input/output and state mapping: TS form -> DTO JSON; empty optional dates -> null; row ID/version -> PUT path/body; response -> invalidate key.
- Error and edge behavior: encode IDs; no POST retry; errors/409 propagate; do not invent prerequisite list GET when controller has only save POST.
- Standards impact: `MC-REUSE-001`, `MC-VALID-001`, `MC-MODEL-001`, `MC-CONVERT-001`, `MC-JSON-001`, `MC-TIME-001`, `MC-PATTERN-001`, `MC-SCOPE-001`, `MC-TEST-001` — existing client/typed View pattern and four-tab Composition.
- Literal rule enforcement: `Rule 2`, `Rule 5`, `Rule 6`, `Rule 9`, `Rule 10`, `Rule 11` — validate fields, use existing utilities, preserve JSON/time, apply Composition and current tree.
- Implementation pseudocode:

```typescript
createSod: (request: SodSetRequest) => client.request<MutationResult>('/api/rbac3/v1/iam/policies/sod-sets', {method: 'POST', body: request}),
updateSod: (setId: string, request: SodSetRequest) => client.request<MutationResult>(`/api/rbac3/v1/iam/policies/sod-sets/${encodeURIComponent(setId)}`, {method: 'PUT', body: request}),
savePrerequisites: (roleId: string, request: PrerequisiteGroupRequest) => client.request<MutationResult>(`/api/rbac3/v1/iam/policies/roles/${encodeURIComponent(roleId)}/prerequisite-groups`, {method: 'POST', body: request}),
```

- Verification contribution: ConstraintPage tests assert the complete method/path/body matrix.
- After this file: all target constraint write methods have client mappings.

#### File 16 — `MODIFY egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin-web/src/features/constraint/ConstraintPage.tsx`

- Purpose: Turn the four read tabs into a CRUD policy workbench with safe DSD/prerequisite/cardinality actions.
- Symbols: editor state, `PermissionGuard system:authorization-constraint:manage`, mutation hooks and tab tables/forms.
- Repository evidence: current page has four queries, only SSD/DSD table, and explanatory text for other tabs; existing `validateDsdRoleSelection` already expresses one domain invariant.
- Dependencies and consumers: File 4 tests/File 16 API, role/application/field IDs already available in page context or entered according to backend contract.
- Why now: GREEN implementation for ConstraintController write RED.
- Contract/signature changes: new/edit forms use exact DTO fields; row version is expected version; no new endpoint/field.
- Input/output and state mapping: tab query -> table; form -> POST/PUT -> invalidate selected tab; role context -> prerequisite/cardinality action.
- Error and edge behavior: 403 hides manage controls and keeps reads; 422 shows field/server message; 409 keeps modal and refresh button; derived data text is not represented as editable local state.
- Standards impact: `MC-REUSE-001`, `MC-VALID-001`, `MC-JSON-001`, `MC-TIME-001`, `MC-PATTERN-001`, `MC-SCOPE-001`, `MC-TEST-001` — existing PageState/Form/Query and Composition.
- Literal rule enforcement: `Rule 2`, `Rule 6`, `Rule 9`, `Rule 10`, `Rule 11` — validation/JSON/time/pattern and current structure are explicit.
- Implementation pseudocode:

```tsx
const saveRule = useMutation({mutationFn: (value: RuleForm) => value.ruleId ? api.updateFieldRule(value.ruleId, toFieldRequest(value)) : api.createFieldRule(toFieldRequest(value)), onSuccess: () => queryClient.invalidateQueries({queryKey: fieldKey})})
const openCreate = (kind: RuleKind) => { setEditor({kind, value: emptyForm(kind)}); form.resetFields(); }
return <Card title="授权约束"><Tabs items={sodTab(dataTab, fieldTab, operationTab)} /></Card>
```

- Verification contribution: ConstraintPage tests turn GREEN for editors, DSD validation, mutation payloads and conflicts.
- After this file: constraint management methods are visible in IAM.

#### File 17 — `MODIFY egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin-web/src/features/management-policy/managementPolicy.api.ts`

- Purpose: Add existing ManagementPolicyController detail/capability/target GET methods to the client.
- Symbols: `get`, `capabilities`, `manageableUsers`, `manageableRoles`; preserve list/create/update/disable idempotency and If-Match headers.
- Repository evidence: controller exposes six GETs; client currently maps only list plus three mutations.
- Dependencies and consumers: ManagementPolicyPage, FeatureApiClient, existing `ManagementPolicyView` and VO-derived shapes.
- Why now: page requires typed read calls before UI wiring.
- Contract/signature changes: exact paths `/api/rbac3/v1/management-policies/{policyId}`, `/api/rbac3/v1/management-capabilities/me`, `/manageable-users`, `/manageable-roles`; no write changes.
- Input/output and state mapping: IDs -> detail/target arrays; response fields remain server-owned.
- Error and edge behavior: encode IDs; empty target arrays valid; 403/404 propagates; no target value is copied into a write unless selected by existing editor.
- Standards impact: `MC-REUSE-001`, `MC-VALID-001`, `MC-JSON-001`, `MC-SCOPE-001`, `MC-TEST-001` — existing client and idempotency contract.
- Literal rule enforcement: `Rule 2`, `Rule 6`, `Rule 11` — preserve server validation/JSON and current feature tree.
- Implementation pseudocode:

```typescript
get: (policyId: string) => client.request<ManagementPolicyView>(`/api/rbac3/v1/management-policies/${encodeURIComponent(policyId)}`),
capabilities: () => client.request<CapabilityView>('/api/rbac3/v1/management-capabilities/me'),
manageableUsers: () => client.request<readonly ManagedUserView[]>('/api/rbac3/v1/management-capabilities/manageable-users'),
manageableRoles: () => client.request<readonly ManagedRoleView[]>('/api/rbac3/v1/management-capabilities/manageable-roles'),
```

- Verification contribution: policy tests assert all new GET paths.
- After this file: policy client covers read/detail surface.

#### File 18 — `MODIFY egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin-web/src/features/management-policy/ManagementPolicyPage.tsx`

- Purpose: Expose policy detail and management capability/target summaries while keeping existing CRUD.
- Symbols: capability query, selected-policy detail query, Drawer, existing editor/disable mutations.
- Repository evidence: page already has list/editor/disable and QueryClient; API File 18 adds missing reads.
- Dependencies and consumers: File 5 test/File 18 API, `ManagementPolicyEditor`, PermissionGuard.
- Why now: final GREEN implementation for policy read coverage.
- Contract/signature changes: no mutation changes; selected row opens detail; target queries load only when editor needs them.
- Input/output and state mapping: list -> capability summary; row -> detail Drawer; editor subject/scope selectors -> manageable target data.
- Error and edge behavior: capability/detail errors are local; existing list remains; 403/409 on writes retains editor and If-Match/idempotency.
- Standards impact: `MC-REUSE-001`, `MC-VALID-001`, `MC-JSON-001`, `MC-SCOPE-001`, `MC-TEST-001` — reuse existing editor/query/permission patterns.
- Literal rule enforcement: `Rule 2`, `Rule 6`, `Rule 11` — server validation/JSON/header semantics and current structure remain.
- Implementation pseudocode:

```tsx
const capability = useQuery({queryKey: ['rbac3','management-capabilities',tenant], queryFn: api.capabilities, enabled: ready})
const detail = useQuery({queryKey: ['rbac3','management-policy',tenant,selected?.policyId], queryFn: () => api.get(selected!.policyId), enabled: ready && Boolean(selected)})
return <><CapabilitySummary value={capability.data}/><Table onRow={(row) => setSelected(row)}/><Drawer open={Boolean(selected)}><PageState loading={detail.isPending} error={detail.error}>{detail.data && <Descriptions .../>}</PageState></Drawer><ManagementPolicyEditor ... /></>
```

- Verification contribution: policy tests turn GREEN and existing CRUD/disable tests remain.
- After this file: Step 3 IAM authorization/catalog workbench is complete.

- Validation working directory: `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin-web`.
- Verification command: `npm run test -- --run src/features/role/RolePages.test.tsx src/features/application/ApplicationPages.test.tsx src/features/permission/PermissionCatalogPage.test.tsx src/features/constraint/ConstraintPage.test.tsx src/features/management-policy/ManagementPolicyPage.test.tsx src/features/business/BusinessCatalogPage.test.tsx && npm run typecheck && npm run build && npm run report:resources`
- Expected result: focused tests pass; route/resource report contains business catalog under IAM resource directory; typecheck/build/report exit 0.
- Completion criteria: IAM role/resource/permission/application/business/policy/constraint Controller methods have visible action/detail/API consumers; no new backend contract, DB or dependency.
- Failure returns to: Files 1-5 for RED fixtures; Files 6-9 for role/detail wiring; Files 10-15 for business route; Files 16-19 for constraint/policy mapping/UI.
- Rollback: revert only Step 3 paths; no runtime or persistent mutation is part of tests/build.
- Commit paths: `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin-web/src/features/role/RolePages.test.tsx`, `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin-web/src/features/application/ApplicationPages.test.tsx`, `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin-web/src/features/permission/PermissionCatalogPage.test.tsx`, `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin-web/src/features/constraint/ConstraintPage.test.tsx`, `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin-web/src/features/management-policy/ManagementPolicyPage.test.tsx`, `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin-web/src/features/role/RoleGraphPage.tsx`, `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin-web/src/features/application/application.api.ts`, `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin-web/src/features/permission/PermissionCatalogPage.tsx`, `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin-web/src/features/application/ApplicationListPage.tsx`, `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin-web/src/features/business/business.api.ts`, `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin-web/src/features/business/BusinessCatalogPage.test.tsx`, `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin-web/src/features/business/BusinessCatalogPage.tsx`, `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin-web/src/features/governance.routes.tsx`, `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin-web/src/app/resourceDefinitions.json`, `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin-web/src/features/constraint/constraint.api.ts`, `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin-web/src/features/constraint/ConstraintPage.tsx`, `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin-web/src/features/management-policy/managementPolicy.api.ts`, `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin-web/src/features/management-policy/ManagementPolicyPage.tsx`.
- Commit: `feat(rbac3-admin-web): complete IAM authorization workbenches`

### Step 4 — Close remaining IDP/Gateway/DDC Controller actions and UI bugs

- Requirements: `REQ-006`, `REQ-009`, `REQ-010`, `REQ-011`, `REQ-012`, `REQ-013`, `REQ-014`, `REQ-015`
- Dependencies: Step 1 committed; Step 2/3 do not overlap these files; Gateway/IDP/DDC API and PageState components remain authoritative.
- Baseline state: Gateway API has unused release diff and capability validation actions; TracesPage calls a backend-nonexistent detail endpoint; IDP management pages contain old `destroyOnClose`; DDC embedded layout is fixed by Step 1 but RouteGuards uses old `Spin.tip`.
- Observable outcome: Gateway release diff and per-capability MCP validation are usable; Trace page offers only backend-supported summary; touched IDP/DDC/Gateway pages use current Ant Design props and retain loading/empty/error states; IDP profile endpoint is visible in overview.
- End state: remaining management methods have real UI/API consumers or will be explicitly classified in Step 5. No backend Trace endpoint is added.
- Test-first gate: `Required` — existing Gateway tests and package tests must first assert missing actions/no-fake endpoint/current props before production edits.
- Manual Checks: `MC-ARCH-001`, `MC-REUSE-001`, `MC-DEP-001`, `MC-VALID-001`, `MC-MODEL-001`, `MC-CONVERT-001`, `MC-UTIL-001`, `MC-JSON-001`, `MC-TIME-001`, `MC-CONFIG-001`, `MC-PATTERN-001`, `MC-SCOPE-001`, `MC-TEST-001`, `MC-BLOCKER-001`
- Literal Rules: `Rule 2`, `Rule 5`, `Rule 6`, `Rule 7`, `Rule 9`, `Rule 10`, `Rule 11`
- Ordered files:

#### File 1 — `MODIFY egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web/src/api/gatewayApi.test.ts`

- Purpose: Define actual release-diff/capability validation calls and remove false Trace detail expectation.
- Symbols: `releaseDiff`, `validateMcpCapability`, and no production `traceDetail` call assertion.
- Repository evidence: current test already calls `releaseDiff` and `traceDetail`; API exposes both methods, but only the first has a backend Controller mapping.
- Dependencies and consumers: Gateway API/page tests and implementations in later files.
- Why now: fixes the real/fake endpoint boundary before UI changes.
- Contract/signature changes: assert GET release diff and POST capability validate paths; remove test that expects a 404 Trace detail business action.
- Input/output and state mapping: IDs/group/plural -> request URL; report -> test result; no response body secrets.
- Error and edge behavior: 404/403/409 remains visible to the page; unsupported Trace detail is not called.
- Standards impact: `MC-REUSE-001`, `MC-JSON-001`, `MC-SCOPE-001`, `MC-TEST-001` — existing Gateway client/test harness only.
- Literal rule enforcement: `Rule 5`, `Rule 6`, `Rule 11` — existing utility/client, JSON and package structure are preserved.
- Implementation pseudocode:

```typescript
it('calls the release diff endpoint with the selected release', async () => {
  await gatewayApi.releaseDiff('release-1')
  expect(request).toHaveBeenCalledWith(expect.stringContaining('/releases/release-1/diff'), expect.anything())
})
it('calls the capability validation endpoint with plural and group', async () => {
  await gatewayApi.validateMcpCapability('prompts', 'prompt-1', 'group-1')
  expect(request).toHaveBeenCalledWith(expect.stringContaining('/mcp/prompts/prompt-1/validate'), expect.objectContaining({method: 'POST'}))
})
```

- Verification contribution: RED until test fixtures match the actual page/action contract; protects no-fake-detail boundary.
- After this file: Gateway endpoint tests are ready for GREEN implementation.

#### File 2 — `MODIFY egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web/src/features/releases/ReleaseDetailPage.test.tsx`

- Purpose: Define a user-visible Release Diff action and loading/error panel.
- Symbols: `查看 Release Diff`, diff query and JsonPanel assertions; preserve retry/rollback tests.
- Repository evidence: ReleaseDetail already renders structured diff, retry and rollback; `gatewayApi.releaseDiff` is unused by the page.
- Dependencies and consumers: ReleaseDetailPage and gatewayApi.
- Why now: a real Release controller read must have a visible action before audit.
- Contract/signature changes: selected release ID invokes existing `releaseDiff`; no new route/body.
- Input/output and state mapping: button -> GET -> structured JSON; close Drawer clears local query view.
- Error and edge behavior: error has retry; report does not expose raw target secrets; release retry remains separate.
- Standards impact: `MC-REUSE-001`, `MC-JSON-001`, `MC-SCOPE-001`, `MC-TEST-001` — existing Query/JsonPanel.
- Literal rule enforcement: `Rule 2`, `Rule 6`, `Rule 11` — preserve API validation/JSON and current feature tree.
- Implementation pseudocode:

```typescript
it('opens structured release diff through the existing API', async () => {
  render(<ReleaseDetailPage />, {wrapper: releaseWrapper})
  await user.click(screen.getByRole('button', {name: '查看 Release Diff'}))
  await waitFor(() => expect(gatewayApi.releaseDiff).toHaveBeenCalledWith('release-1', expect.anything()))
  expect(await screen.findByText('Structured Diff')).toBeInTheDocument()
})
```

- Verification contribution: RED because no button/query currently exists.
- After this file: Release Diff behavior is fixed.

#### File 3 — `MODIFY egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web/src/features/observability/TracesPage.test.tsx`

- Purpose: Prove observability page never advertises the absent Trace detail Controller.
- Symbols: remove old `traceDetail` mock expectations; assert summary table, redacted notice and no `查看详情` action.
- Repository evidence: Java `GatewayObservabilityController` has dashboard/traces/audit only; current page contains `traceDetail` query and “待补齐” drawer.
- Dependencies and consumers: TracesPage, gatewayApi and QueryState.
- Why now: test must force removal rather than preserving a misleading UI fallback.
- Contract/signature changes: no Trace detail request; existing summary scope/search/paging remains.
- Input/output and state mapping: scope/filter -> summary query -> table; no row-to-detail state.
- Error and edge behavior: refresh error retains last result; missing scope remains empty state; raw body/header never shown.
- Standards impact: `MC-REUSE-001`, `MC-JSON-001`, `MC-SCOPE-001`, `MC-TEST-001` — existing summary client and redaction test.
- Literal rule enforcement: `Rule 5`, `Rule 6`, `Rule 11` — no new utility/JSON/backend structure.
- Implementation pseudocode:

```typescript
it('keeps traces summary-only when the backend has no detail mapping', async () => {
  render(<TracesPage />, {wrapper: traceWrapperWithScope})
  await screen.findByText('trace-1')
  expect(screen.queryByRole('button', {name: '查看详情'})).not.toBeInTheDocument()
  expect(gatewayApi.traceDetail).not.toHaveBeenCalled()
})
```

- Verification contribution: test initially fails against the current detail button/query.
- After this file: unsupported Trace detail is explicitly out of UI scope.

#### File 4 — `CREATE egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web/src/features/mcp/McpCapabilityValidationButton.test.tsx`

- Purpose: Define shared per-capability validation button behavior before component creation.
- Symbols: `validatesOneCapability`, disabled permission state, success findings and 403/409 error states.
- Repository evidence: `gatewayApi.validateMcpCapability(plural,id,group)` already exists; capability panels have row IDs/group IDs but no shared action.
- Dependencies and consumers: new button, Gateway API and capability panel rows.
- Why now: closes the unused capability validation method with a focused RED test.
- Contract/signature changes: props use `McpCapabilityPlural`, capability ID and gateway group ID; POST has no new body.
- Input/output and state mapping: click -> mutation -> `McpValidationReport`; result stays local to row.
- Error and edge behavior: pending lock; no permission disables; findings render safe code/path/message; no auto-publish/retry.
- Standards impact: `MC-REUSE-001`, `MC-JSON-001`, `MC-PATTERN-001`, `MC-SCOPE-001`, `MC-TEST-001` — existing client/Query and justified component Composition.
- Literal rule enforcement: `Rule 5`, `Rule 6`, `Rule 9`, `Rule 11` — existing utility/JSON, real capability variation and current structure.
- Implementation pseudocode:

```typescript
it('posts the exact capability validation request and displays findings', async () => {
  vi.mocked(gatewayApi.validateMcpCapability).mockResolvedValue({valid: false, findings: [{code: 'INVALID', path: '$.name', message: 'bad'}]})
  render(<McpCapabilityValidationButton plural="prompts" capabilityId="p-1" gatewayGroupId="g-1" />, {wrapper})
  await user.click(screen.getByRole('button', {name: '校验'}))
  expect(gatewayApi.validateMcpCapability).toHaveBeenCalledWith('prompts', 'p-1', 'g-1')
  expect(await screen.findByText('INVALID')).toBeInTheDocument()
})
```

- Verification contribution: RED because component is absent.
- After this file: reusable validation behavior is specified.

#### File 5 — `MODIFY egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web/src/api/gatewayApi.ts`

- Purpose: Keep actual `releaseDiff`/`validateMcpCapability` mappings and remove unsupported `traceDetail` after consumer proof.
- Symbols: `releaseDiff`, `validateMcpCapability`, `traceDetail` search/removal decision.
- Repository evidence: release diff and capability validate paths are defined; Trace detail has no Java mapping and only TracesPage calls it.
- Dependencies and consumers: Files 1-4 tests, ReleaseDetail, TracesPage and new MCP button.
- Why now: production API surface must align with tests/backend inventory.
- Contract/signature changes: retain real methods exactly; delete `traceDetail` only after `rg` confirms no production consumer, including tests updated in File 3.
- Input/output and state mapping: existing IDs/signal/group/plural map to URLs; no change to HTTP headers/body.
- Error and edge behavior: `GatewayApiError` stays intact; unsupported detail is removed rather than hidden behind a fake 404.
- Standards impact: `MC-REUSE-001`, `MC-VALID-001`, `MC-JSON-001`, `MC-SCOPE-001`, `MC-TEST-001` — no new endpoint/dependency.
- Literal rule enforcement: `Rule 2`, `Rule 5`, `Rule 6`, `Rule 11` — preserve validation/client JSON and current structure.
- Implementation pseudocode:

```typescript
releaseDiff: (releaseId, signal) => apiRequest<ReleaseDiff>(`${admin}/releases/${encodeURIComponent(releaseId)}/diff`, {signal}),
validateMcpCapability: (plural, capabilityId, gatewayGroupId) => apiRequest<McpValidationReport>(`${admin}/mcp/${plural}/${encodeURIComponent(capabilityId)}/validate?${new URLSearchParams({gatewayGroupId})}`, {method: 'POST'}),
assertNoTraceDetailConsumerBeforeDelete()
```

- Verification contribution: API tests turn GREEN and `rg traceDetail` proves deletion is safe.
- After this file: API methods match real backend coverage.

#### File 6 — `MODIFY egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web/src/features/releases/ReleaseDetailPage.tsx`

- Purpose: Add on-demand Release Diff Drawer using the existing API and structured redacted data.
- Symbols: `diffOpen`, `releaseDiff` query, `JsonPanel`, `QueryFailure`, current retry/rollback actions.
- Repository evidence: page already has QueryClient, release query, structured diff panels and status/error UI.
- Dependencies and consumers: File 2 test/File 5 API, Gateway route and capability guard.
- Why now: GREEN implementation for real Release Diff coverage.
- Contract/signature changes: no backend change; GET query enabled only when Drawer open.
- Input/output and state mapping: release ID -> diff query -> JSON panel; close leaves release query intact.
- Error and edge behavior: loading/error/retry visible; no automatic write retry; no raw credential/target body.
- Standards impact: `MC-REUSE-001`, `MC-VALID-001`, `MC-JSON-001`, `MC-SCOPE-001`, `MC-TEST-001` — existing query/panel components.
- Literal rule enforcement: `Rule 2`, `Rule 6`, `Rule 11` — preserve server validation/JSON and current page tree.
- Implementation pseudocode:

```tsx
const diff = useQuery({queryKey: ['release-diff', releaseId], queryFn: ({signal}) => gatewayApi.releaseDiff(releaseId, signal), enabled: diffOpen})
<Button onClick={() => setDiffOpen(true)}>查看 Release Diff</Button>
<Drawer open={diffOpen} onClose={() => setDiffOpen(false)} title="Release Diff">{diff.isLoading ? <LoadingBlock/> : diff.error ? <QueryFailure error={diff.error} retry={() => void diff.refetch()}/> : <JsonPanel title="Structured Diff" value={diff.data}/>}</Drawer>
```

- Verification contribution: ReleaseDetail test turns GREEN.
- After this file: release diff is a real visible Controller read.

#### File 7 — `MODIFY egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web/src/features/observability/TracesPage.tsx`

- Purpose: Remove unsupported Trace detail query/drawer/action and retain summary observability.
- Symbols: remove `selectedTraceId`, `detailQuery`, `TraceDetailContent` and row detail button; preserve scope/filter/table/redaction text.
- Repository evidence: current page’s only non-summary call is `gatewayApi.traceDetail`; backend has no mapping.
- Dependencies and consumers: File 3 test/File 5 API, `GatewayScopeFilter`, QueryState, `sanitizeForDisplay`.
- Why now: GREEN implementation of honest backend/UI alignment.
- Contract/signature changes: no new API; summary query signature unchanged.
- Input/output and state mapping: scope/search params -> summary query and pagination; no detail state.
- Error and edge behavior: query error with cached data continues warning/retry; absent scope shows EmptyBlock; no raw data.
- Standards impact: `MC-REUSE-001`, `MC-JSON-001`, `MC-SCOPE-001`, `MC-TEST-001` — reuse summary/redaction components.
- Literal rule enforcement: `Rule 5`, `Rule 6`, `Rule 11` — no new utility/JSON/backend structure.
- Implementation pseudocode:

```tsx
<Table dataSource={query.data?.items ?? []} columns={[traceIdColumn, scopeColumns, statusColumn]} />
<Typography.Paragraph type="secondary">仅提供后端现有的脱敏 Trace 汇总查询，不展示不存在的详情接口。</Typography.Paragraph>
const hasDetailAction = false
assert(hasDetailAction === false)
```

- Verification contribution: TracesPage tests turn GREEN and no unsupported method remains.
- After this file: observability UI is complete for actual Controller surface.

#### File 8 — `CREATE egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web/src/features/mcp/McpCapabilityValidationButton.tsx`

- Purpose: Implement one reusable per-capability validation action.
- Symbols: `McpCapabilityValidationButton`, props `plural/capabilityId/gatewayGroupId`, `useMutation`, result Alert.
- Repository evidence: panels use existing row capabilities and Gateway API; server-level preview is separate.
- Dependencies and consumers: File 4 test/File 5 API, four capability panels.
- Why now: GREEN component implementation before row wiring.
- Contract/signature changes: no public backend contract; plural is constrained to API-supported values.
- Input/output and state mapping: click -> POST -> report; mutation state/render is local.
- Error and edge behavior: no permission/pending disables; error text uses existing Gateway API error; findings display safe fields only; no publish side effect.
- Standards impact: `MC-REUSE-001`, `MC-JSON-001`, `MC-PATTERN-001`, `MC-SCOPE-001`, `MC-TEST-001` — reuse Query/API and Composition.
- Literal rule enforcement: `Rule 5`, `Rule 6`, `Rule 9`, `Rule 11` — existing utility/JSON, justified capability composition, current tree.
- Implementation pseudocode:

```tsx
const validate = useMutation({mutationFn: () => gatewayApi.validateMcpCapability(plural, capabilityId, gatewayGroupId)})
return <Space><Button disabled={!canRead || validate.isPending} loading={validate.isPending} onClick={() => validate.mutate()}>校验</Button>{validate.data && <Alert type={validate.data.valid ? 'success' : 'error'} message={validate.data.valid ? '校验通过' : '校验失败'} description={renderFindings(validate.data.findings)} />}</Space>
const message = validate.error ? safeGatewayError(validate.error) : validate.data?.valid ? 'valid' : 'invalid'
renderStatus(message)
```

- Verification contribution: File 4 test turns GREEN.
- After this file: shared action can be composed into capability rows.

#### File 9 — `MODIFY egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web/src/features/mcp/McpPromptsPanel.tsx`

- Purpose: Expose per-row prompt validation.
- Symbols: import/render `McpCapabilityValidationButton` with `plural="prompts"`, row ID and group ID.
- Repository evidence: panel already uses `useMcpCapabilityCollection('prompts',...)` and renders each capability row.
- Dependencies and consumers: File 8 button, existing save/remove mutations.
- Why now: first row wiring for the shared action.
- Contract/signature changes: no API; only a table action.
- Input/output and state mapping: row ID/group -> validation report shown in row/action area.
- Error and edge behavior: write/read capability guard follows panel convention; save/delete unaffected.
- Standards impact: `MC-REUSE-001`, `MC-JSON-001`, `MC-SCOPE-001`, `MC-TEST-001` — existing panel and button only.
- Literal rule enforcement: `Rule 5`, `Rule 6`, `Rule 11` — reuse utility/API/JSON and current structure.
- Implementation pseudocode:

```tsx
<McpCapabilityValidationButton plural="prompts" capabilityId={row.id} gatewayGroupId={gatewayGroupId} />
const canValidatePrompt = Boolean(row.id && gatewayGroupId)
renderPromptValidationState(canValidatePrompt)
```

- Verification contribution: MCP panel test observes prompt validate action.
- After this file: Prompt controller validate method is represented.

#### File 10 — `MODIFY egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web/src/features/mcp/McpResourcesPanel.tsx`

- Purpose: Expose validation for resources and resource templates using the exact plural route.
- Symbols: render button for `resources` and `resource-templates` rows where IDs are present.
- Repository evidence: panel has two `useMcpCapabilityCollection` instances and row tables.
- Dependencies and consumers: File 8 component and existing collection queries.
- Why now: covers both resource capability variants without duplicate mutation code.
- Contract/signature changes: no API; plural value matches `McpCapabilityPlural` and backend mapping.
- Input/output and state mapping: selected row -> POST report; list/cache unchanged.
- Error and edge behavior: unsupported/missing ID leaves action unavailable; report is local and safe.
- Standards impact: `MC-REUSE-001`, `MC-JSON-001`, `MC-PATTERN-001`, `MC-SCOPE-001`, `MC-TEST-001` — reuse shared component.
- Literal rule enforcement: `Rule 5`, `Rule 6`, `Rule 9`, `Rule 11` — existing utility/JSON, composition and structure.
- Implementation pseudocode:

```tsx
const plural = target === 'templates' ? 'resource-templates' : 'resources'
<McpCapabilityValidationButton plural={plural} capabilityId={row.id} gatewayGroupId={gatewayGroupId} />
const validationTarget = {plural, capabilityId: row.id, gatewayGroupId}
assert validationTarget.capabilityId.length > 0
```

- Verification contribution: resource panel tests observe each supported capability kind.
- After this file: resource validation methods are represented.

#### File 11 — `MODIFY egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web/src/features/mcp/McpAppsPanel.tsx`

- Purpose: Expose validation for managed MCP App capability rows.
- Symbols: render button with `plural="apps"` in binding table.
- Repository evidence: panel uses `useMcpCapabilityCollection`/app binding rows and has gatewayGroupId/serverId context.
- Dependencies and consumers: File 8 and existing app save/remove invalidation.
- Why now: completes App capability controller action.
- Contract/signature changes: no backend change.
- Input/output and state mapping: row ID/group -> POST report; binding state unchanged.
- Error and edge behavior: permission/pending lock; findings safe; delete remains separate.
- Standards impact: `MC-REUSE-001`, `MC-JSON-001`, `MC-SCOPE-001`, `MC-TEST-001` — existing panel/client.
- Literal rule enforcement: `Rule 5`, `Rule 6`, `Rule 11` — allowed utility, JSON and current tree.
- Implementation pseudocode:

```tsx
<McpCapabilityValidationButton plural="apps" capabilityId={row.id} gatewayGroupId={gatewayGroupId} />
const appValidationTarget = {plural: 'apps', id: row.id, group: gatewayGroupId}
renderAppValidationStatus(appValidationTarget)
```

- Verification contribution: App panel action test calls exact API method.
- After this file: App validation is represented.

#### File 12 — `MODIFY egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web/src/features/mcp/McpTasksPanel.tsx`

- Purpose: Expose validation for task policy capability rows.
- Symbols: render button with `plural="tasks"` beside task save/delete actions.
- Repository evidence: panel has `useMcpCapabilityCollection` and row IDs/draft revision.
- Dependencies and consumers: File 8 and existing task query invalidation.
- Why now: completes task capability validation UI.
- Contract/signature changes: no API change.
- Input/output and state mapping: row -> local validation report; save/delete unchanged.
- Error and edge behavior: no publish; 403/409 remains visible; pending row locked.
- Standards impact: `MC-REUSE-001`, `MC-JSON-001`, `MC-SCOPE-001`, `MC-TEST-001` — reuse existing panel/client.
- Literal rule enforcement: `Rule 5`, `Rule 6`, `Rule 11` — existing utility/JSON/tree only.
- Implementation pseudocode:

```tsx
<McpCapabilityValidationButton plural="tasks" capabilityId={row.id} gatewayGroupId={gatewayGroupId} />
const taskValidationTarget = {plural: 'tasks', id: row.id, group: gatewayGroupId}
renderTaskValidationStatus(taskValidationTarget)
```

- Verification contribution: Task panel test proves exact capability validation action.
- After this file: target MCP capability rows have validation actions.

#### File 13 — `MODIFY egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-admin-web/src/features/users/UserListPage.tsx`

- Purpose: Replace deprecated modal lifecycle props in identity user CRUD without changing one-time password semantics.
- Symbols: both `Modal` instances using `destroyOnClose` -> `destroyOnHidden`; existing create/edit/reset/revoke handlers.
- Repository evidence: current file contains two old props and tests cover user API actions.
- Dependencies and consumers: IDP AuthContext/httpClient, UserListPage tests and IdentityUserController.
- Why now: concrete UI warning in a high-use CRUD page.
- Contract/signature changes: Ant Design prop rename only; backend paths/body/version unchanged.
- Input/output and state mapping: modal close destroys form; one-time password remains only in successful result view.
- Error and edge behavior: mutation errors stay visible; secret is never persisted/logged.
- Standards impact: `MC-REUSE-001`, `MC-VALID-001`, `MC-JSON-001`, `MC-SCOPE-001`, `MC-TEST-001` — mechanical current AntD API correction.
- Literal rule enforcement: `Rule 2`, `Rule 6`, `Rule 11` — preserve validation/JSON and current feature structure.
- Implementation pseudocode:

```tsx
<Modal open={createOpen} destroyOnHidden onCancel={closeCreate} onOk={() => void submitCreate()} />
<Modal open={resetResult !== undefined} destroyOnHidden footer={null} onCancel={clearResetResult} />
assert createOpen === false || resetResult === undefined || isOneTimeDisplay(resetResult)
```

- Verification contribution: existing user tests, typecheck and deprecation search.
- After this file: user CRUD modals no longer emit the warning.

#### File 14 — `MODIFY egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-admin-web/src/features/clients/ClientListPage.tsx`

- Purpose: Correct client CRUD/secret/URI modal lifecycle props while preserving OAuth client actions.
- Symbols: every `destroyOnClose` -> `destroyOnHidden`; existing client/credential/redirect/resource URI handlers.
- Repository evidence: current file has several old props and ClientListPage tests cover API calls.
- Dependencies and consumers: httpClient, OAuthClientController and existing tests.
- Why now: concrete warning across the client management surface.
- Contract/signature changes: prop rename only; secret rotation and expected-version requests unchanged.
- Input/output and state mapping: form reset on hidden; one-time secret remains shown only after response.
- Error and edge behavior: 403/409 messages remain; raw secret never enters logs/shared state.
- Standards impact: `MC-REUSE-001`, `MC-VALID-001`, `MC-JSON-001`, `MC-SCOPE-001`, `MC-TEST-001` — existing API and AntD correction.
- Literal rule enforcement: `Rule 2`, `Rule 6`, `Rule 11` — preserve validation/JSON and current structure.
- Implementation pseudocode:

```tsx
<Modal open={editorOpen} destroyOnHidden onCancel={closeEditor} onOk={() => void submitClient()} />
<Modal open={secretResult !== undefined} destroyOnHidden footer={null} onCancel={clearSecretResult} />
assert secretResult === undefined || isOneTimeSecretView(secretResult)
```

- Verification contribution: existing client tests/typecheck and deprecation search.
- After this file: client modals use the supported lifecycle prop.

#### File 15 — `MODIFY egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-admin-web/src/features/tenants/TenantListPage.tsx`

- Purpose: Correct tenant/member modal lifecycle while preserving expected-version mutations.
- Symbols: tenant create/edit/member modals `destroyOnClose` -> `destroyOnHidden`.
- Repository evidence: current file contains three old props and TenantListPage tests cover tenant/member endpoints.
- Dependencies and consumers: httpClient, TenantController and existing query invalidation.
- Why now: direct UI warning in tenant administration.
- Contract/signature changes: AntD prop only; status/settings/version JSON unchanged.
- Input/output and state mapping: tenant/member selection -> form; hidden -> reset; success refreshes list/detail/member queries.
- Error and edge behavior: 403/409 remains visible; settings are not broadened or logged.
- Standards impact: `MC-REUSE-001`, `MC-VALID-001`, `MC-JSON-001`, `MC-SCOPE-001`, `MC-TEST-001` — existing page/client.
- Literal rule enforcement: `Rule 2`, `Rule 6`, `Rule 11` — preserve validation/JSON and current tree.
- Implementation pseudocode:

```tsx
<Modal open={tenantEditorOpen} destroyOnHidden onOk={() => void form.submit()} onCancel={closeTenantEditor} />
<Modal open={memberEditorOpen} destroyOnHidden onCancel={closeMemberEditor} />
assert tenantEditorOpen === false || memberEditorOpen === false || preserveExpectedVersion()
```

- Verification contribution: tenant tests/typecheck and source search.
- After this file: tenant/member modals use current AntD API.

#### File 16 — `MODIFY egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-admin-web/src/features/resource-servers/ResourceServerListPage.tsx`

- Purpose: Correct resource-server detail/editor modal lifecycle and retain batch/version actions.
- Symbols: resource modals `destroyOnClose` -> `destroyOnHidden`; existing create/update/enable/disable/batch handlers.
- Repository evidence: current file contains old modal prop and ResourceServer tests cover list/actions.
- Dependencies and consumers: ResourceServerController, ClientResourceGrantPage and httpClient.
- Why now: concrete warning in resource-server CRUD.
- Contract/signature changes: prop rename only; IDs/version/body unchanged.
- Input/output and state mapping: selected resource -> form; hidden resets; success invalidates list.
- Error and edge behavior: batch partial/error stays visible; no credential fields shown.
- Standards impact: `MC-REUSE-001`, `MC-VALID-001`, `MC-JSON-001`, `MC-SCOPE-001`, `MC-TEST-001` — existing API and AntD.
- Literal rule enforcement: `Rule 2`, `Rule 6`, `Rule 11` — preserve backend validation/JSON and current structure.
- Implementation pseudocode:

```tsx
<Modal open={resourceEditorOpen} destroyOnHidden onCancel={closeResourceEditor} />
<Modal open={selectedResource !== undefined} destroyOnHidden footer={null} />
assert selectedResource === undefined || hideCredentialFields(selectedResource)
```

- Verification contribution: resource-server tests/typecheck and search.
- After this file: resource-server modal warning is removed.

#### File 17 — `MODIFY egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-admin-web/src/features/resource-grants/ClientResourceGrantPage.tsx`

- Purpose: Correct grant editor/detail modal lifecycle and preserve client-resource relationship CRUD.
- Symbols: grant modals `destroyOnClose` -> `destroyOnHidden`; existing grant mutations and table actions.
- Repository evidence: page consumes ClientResourceGrantController and contains the old prop.
- Dependencies and consumers: httpClient, grant tests and resource-server page.
- Why now: concrete UI bug in relationship management.
- Contract/signature changes: prop rename only; grant type/scopes/expected versions unchanged.
- Input/output and state mapping: selected client/resource -> grant form -> PUT/DELETE; hidden closes/reset form; success refreshes.
- Error and edge behavior: invalid grant/403/409 visible; no token display.
- Standards impact: `MC-REUSE-001`, `MC-VALID-001`, `MC-JSON-001`, `MC-SCOPE-001`, `MC-TEST-001` — existing grant client and current AntD.
- Literal rule enforcement: `Rule 2`, `Rule 6`, `Rule 11` — preserve validation/JSON and feature tree.
- Implementation pseudocode:

```tsx
<Modal open={grantEditorOpen} destroyOnHidden onOk={() => void submitGrant()} onCancel={closeGrantEditor} />
<Modal open={selectedGrant !== undefined} destroyOnHidden footer={null} />
assert selectedGrant === undefined || preserveGrantVersion(selectedGrant.version)
```

- Verification contribution: grant tests/typecheck and deprecation search.
- After this file: grant modal warning is removed.

#### File 18 — `MODIFY egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-admin-web/src/features/keys/SigningKeyPage.tsx`

- Purpose: Correct key lifecycle modal and preserve publish/activate/retire controls.
- Symbols: key modal `destroyOnClose` -> `destroyOnHidden`; existing key API mutations.
- Repository evidence: current file has one old prop; SigningKeyController lifecycle methods are already consumed.
- Dependencies and consumers: httpClient, key page permissions and existing source fields.
- Why now: concrete UI warning in security administration.
- Contract/signature changes: prop rename only; no private key field or endpoint change.
- Input/output and state mapping: key row/version -> lifecycle action; hidden closes/reset; success refreshes keys.
- Error and edge behavior: only public metadata shown; 403/409 visible; private material not logged.
- Standards impact: `MC-REUSE-001`, `MC-VALID-001`, `MC-JSON-001`, `MC-SCOPE-001`, `MC-TEST-001` — existing API/security boundary.
- Literal rule enforcement: `Rule 2`, `Rule 6`, `Rule 11` — preserve validation/JSON and current tree.
- Implementation pseudocode:

```tsx
<Modal open={publishOpen} destroyOnHidden onOk={() => void publish.mutate()} onCancel={closePublish} />
const publishResult = publish.data ?? null
assert publishResult === null || isPublicKeyMetadata(publishResult)
```

- Verification contribution: key typecheck/build and source search.
- After this file: key modal warning is removed.

#### File 19 — `MODIFY egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-admin-web/src/features/overview/OverviewPage.tsx`

- Purpose: Consume `IdentityProfileController.me` safely in the visible overview.
- Symbols: `IdentityProfileView`, `httpClient` query, profile PageState/Descriptions card; existing bootstrap authorization cards remain.
- Repository evidence: `/api/v1/identity/me` is an external GET with `IdentityPrincipal`; current Overview only uses bootstrap, and principal `tokenId/audience` are sensitive.
- Dependencies and consumers: IDP AuthContext/httpClient, shared PageState, IdentityProfileController.
- Why now: gives the profile Controller method a real UI consumer without displaying token identifiers.
- Contract/signature changes: GET only; display subject/tenant and safe times, never tokenId/audience.
- Input/output and state mapping: authenticated bootstrap -> profile GET -> profile card; bootstrap remains authorization source.
- Error and edge behavior: profile 403/404/error is local and does not block overview permission cards; no token read/retry write.
- Standards impact: `MC-REUSE-001`, `MC-VALID-001`, `MC-JSON-001`, `MC-TIME-001`, `MC-SCOPE-001`, `MC-TEST-001` — existing httpClient/PageState and allowlisted fields.
- Literal rule enforcement: `Rule 2`, `Rule 6`, `Rule 10`, `Rule 11` — preserve validation/JSON/time and current feature tree.
- Implementation pseudocode:

```tsx
const profile = useQuery({queryKey: ['idp','identity-profile'], queryFn: () => httpClient.request<IdentityProfileView>('/api/v1/identity/me'), enabled: Boolean(auth.bootstrap)})
<PageState loading={profile.isPending} error={profile.error} empty={!profile.data} onRetry={() => void profile.refetch()}>{profile.data && <Descriptions><Descriptions.Item label="主体">{profile.data.subject}</Descriptions.Item><Descriptions.Item label="租户">{profile.data.tenantId}</Descriptions.Item></Descriptions>}</PageState>
const safeProfile = profile.data ? {subject: profile.data.subject, tenantId: profile.data.tenantId} : null
assert safeProfile === null || !('tokenId' in safeProfile) && !('audience' in safeProfile)
```

- Verification contribution: overview test/API source check observes `/identity/me` and asserts tokenId/audience absent from DOM.
- After this file: IdentityProfileController is represented safely.

#### File 20 — `MODIFY egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin-web/src/auth/RouteGuards.tsx`

- Purpose: Replace deprecated `Spin.tip` with Ant Design’s current `description` while preserving auth loading state.
- Symbols: `RouteGuards` loading branch.
- Repository evidence: current source uses `<Spin fullscreen tip="校验统一登录态" />`.
- Dependencies and consumers: DDC AuthContext/router and Ant Design 6.
- Why now: direct UI warning and smallest safe fix.
- Contract/signature changes: prop rename only; auth transition unchanged.
- Input/output and state mapping: loading boolean -> fullscreen spinner text.
- Error and edge behavior: auth failure/redirect unchanged.
- Standards impact: `MC-REUSE-001`, `MC-SCOPE-001`, `MC-TEST-001` — current component API only.
- Literal rule enforcement: `Rule 11` — no architecture change; Java rules otherwise N/A.
- Implementation pseudocode:

```tsx
if (loading) return <Spin fullscreen description="校验统一登录态" />
if (!loading && authError) return <Navigate to="/login" replace />
return <Outlet />
```

- Verification contribution: DDC typecheck/build and deprecation search.
- After this file: DDC auth loading warning is removed.

- Validation working directory: run Gateway, IDP and DDC package commands separately; repository root for unsupported endpoint/deprecation searches.
- Verification command: `cd egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web && npm run test -- --run src/api/gatewayApi.test.ts src/features/releases/ReleaseDetailPage.test.tsx src/features/observability/TracesPage.test.tsx src/features/mcp/McpCapabilityValidationButton.test.tsx && npm run typecheck && npm run build`; then run `npm run test -- --run && npm run typecheck && npm run build` in the IDP and DDC Web roots.
- Expected result: Gateway/IDP/DDC tests and builds pass; Release Diff/MCP validation use real paths; production has no fake Trace detail or touched deprecated props.
- Completion criteria: remaining in-scope IDP/Gateway/DDC UI methods are consumed, redacted data boundaries remain, and Step paths are clean.
- Failure returns to: Files 1-4 for RED fixtures; Files 5-7 for Gateway API/pages; Files 8-12 for MCP; Files 13-19 for IDP; File 20 for DDC.
- Rollback: revert only Step 4 paths; no Java/backend/runtime state changes.
- Commit paths: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web/src/api/gatewayApi.test.ts`, `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web/src/features/releases/ReleaseDetailPage.test.tsx`, `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web/src/features/observability/TracesPage.test.tsx`, `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web/src/features/mcp/McpCapabilityValidationButton.test.tsx`, `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web/src/api/gatewayApi.ts`, `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web/src/features/releases/ReleaseDetailPage.tsx`, `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web/src/features/observability/TracesPage.tsx`, `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web/src/features/mcp/McpCapabilityValidationButton.tsx`, `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web/src/features/mcp/McpPromptsPanel.tsx`, `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web/src/features/mcp/McpResourcesPanel.tsx`, `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web/src/features/mcp/McpAppsPanel.tsx`, `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web/src/features/mcp/McpTasksPanel.tsx`, `egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-admin-web/src/features/users/UserListPage.tsx`, `egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-admin-web/src/features/clients/ClientListPage.tsx`, `egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-admin-web/src/features/tenants/TenantListPage.tsx`, `egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-admin-web/src/features/resource-servers/ResourceServerListPage.tsx`, `egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-admin-web/src/features/resource-grants/ClientResourceGrantPage.tsx`, `egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-admin-web/src/features/keys/SigningKeyPage.tsx`, `egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-admin-web/src/features/overview/OverviewPage.tsx`, `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin-web/src/auth/RouteGuards.tsx`.
- Commit: `fix(platforms-admin-web): close controller actions and UI gaps`

### Step 5 — Produce per-method Controller/UI coverage audit and final static closure

- Requirements: `REQ-006`, `REQ-008`, `REQ-009`, `REQ-010`, `REQ-011`, `REQ-012`, `REQ-013`, `REQ-014`, `REQ-015`, `REQ-016`
- Dependencies: Steps 1-4 committed; Java Controller source and all four Web source trees readable; no running process required.
- Baseline state: `check-admin-web-controller-coverage.sh` counts files/mapping lines/API references but cannot identify an individual method’s UI/API evidence; protocol/internal/callback endpoints must not be forced into business pages.
- Observable outcome: one command inventories every external Controller method and emits platform/controller/method/http/path/status/evidence, with management methods `UI_ACTION` or `API_CONSUMED`, explicit `PROTOCOL_OR_INTERNAL`, and nonzero on unexplained `UNCONSUMED`.
- End state: audit test/script/shell gate are committed; final Web typecheck/build and path audit run. Login/Wujie/real CRUD browser proof remains user-controlled TEST-011.
- Test-first gate: `Required` — Python tests first fail because the classifier/registry is absent; implement parser/classifier and shell integration afterward.
- Manual Checks: `MC-ARCH-001`, `MC-REUSE-001`, `MC-DEP-001`, `MC-UTIL-001`, `MC-JSON-001`, `MC-CONFIG-001`, `MC-PATTERN-001`, `MC-SCOPE-001`, `MC-TEST-001`, `MC-BLOCKER-001`
- Literal Rules: `Rule 5`, `Rule 6`, `Rule 7`, `Rule 9`, `Rule 11`
- Ordered files:

#### File 1 — `CREATE scripts/unified-platform/controller_ui_coverage_test.py`

- Purpose: Fix parser/classifier behavior with Python standard-library tests before implementation.
- Symbols: class/method mapping composition, external annotation, protocol/internal exclusion, UI/API evidence and management-unconsumed test cases.
- Repository evidence: old shell audit uses `rg`; Python 3 is used by repository skill scripts; Java annotations/Web routes are source evidence.
- Dependencies and consumers: new `controller_ui_coverage.py`, shell wrapper and four platform source roots.
- Why now: prevents count-only false success or silent parser omission.
- Contract/signature changes: expected rows contain `platform/controller/method/http/path/status/evidence`; no business data.
- Input/output and state mapping: temporary source fixtures -> inventory/classification; status -> CLI exit code.
- Error and edge behavior: unsupported dynamic mapping is review/internal with evidence, never silently UI-covered; secrets are not read or printed.
- Standards impact: `MC-REUSE-001`, `MC-UTIL-001`, `MC-JSON-001`, `MC-SCOPE-001`, `MC-TEST-001` — deterministic stdlib tests only.
- Literal rule enforcement: `Rule 5`, `Rule 6`, `Rule 11` — allowed stdlib, safe fields and current script structure.
- Implementation pseudocode:

```python
def test_composes_class_and_method_mapping():
    rows = inventory_java(fixture('@RequestMapping("/api")', '@GetMapping("/users")'))
    assert rows[0].path == '/api/users'
def test_management_without_evidence_fails():
    result = classify([method('RoleController', 'create')], web_inventory={})
    assert result.exit_code != 0 and result.rows[0].status == 'UNCONSUMED'
```

- Verification contribution: tests are RED until module functions exist, then prove parser/classifier edge cases.
- After this file: audit test contract exists and is intentionally RED.

#### File 2 — `CREATE scripts/unified-platform/controller_ui_coverage.py`

- Purpose: Inventory external Java Controller methods and classify frontend representation against explicit evidence.
- Symbols: `inventory_java`, `inventory_web`, `classify`, `render_report`, `main`; statuses `UI_ACTION`, `API_CONSUMED`, `PROTOCOL_OR_INTERNAL`, `UNCONSUMED`.
- Repository evidence: four Admin Java roots use class-level RequestMapping and Spring mappings; Web code uses route literals/API methods/action labels; previous shell script defines source roots.
- Dependencies and consumers: Python stdlib `pathlib/re/json/argparse`; shell wrapper; no generated output.
- Why now: fulfills the per-Controller/per-method user requirement rather than file counting.
- Contract/signature changes: CLI accepts `--repo-root` and `--format text|json`; management `UNCONSUMED` exits 1; protocol/internal requires a reason.
- Input/output and state mapping: annotations -> method/path rows; Web inventory/registry -> evidence; report -> stdout/exit only.
- Error and edge behavior: compose class/method paths; recognize only external mappings; dynamic paths need explicit registry evidence; never print source bodies/secrets.
- Standards impact: `MC-ARCH-001`, `MC-REUSE-001`, `MC-UTIL-001`, `MC-JSON-001`, `MC-CONFIG-001`, `MC-PATTERN-001`, `MC-SCOPE-001`, `MC-TEST-001` — Registry handles stable variation without dependency.
- Literal rule enforcement: `Rule 5`, `Rule 6`, `Rule 7`, `Rule 9`, `Rule 11` — stdlib allowlist, safe report, stable roots, Registry and current tree.
- Implementation pseudocode:

```python
def inventory_java(root):
    for source in java_admin_sources(root):
        if not is_external_controller(source):
            continue
        class_path = read_class_mapping(source)
        for method in mapped_methods(source):
            yield compose_row(source, class_path, method)

def classify(row, web):
    if row.key in PROTOCOL_INTERNAL_REGISTRY:
        return evidence('PROTOCOL_OR_INTERNAL', PROTOCOL_INTERNAL_REGISTRY[row.key])
    return match_registry_or_web(row, web) or evidence('UNCONSUMED', 'no UI/API evidence')
```

- Verification contribution: Python tests turn GREEN; current source run reveals true gaps for the owning UI Step.
- After this file: deterministic per-method engine exists; shell gate is not yet integrated.

#### File 3 — `MODIFY scripts/unified-platform/check-admin-web-controller-coverage.sh`

- Purpose: Preserve quick assertions and invoke the per-method audit as the authoritative gate.
- Symbols: existing source-root/assertion logic plus Python CLI invocation and intentional exclusion output.
- Repository evidence: shell script already knows Java/Web roots and checks key paths; it has no method-level status.
- Dependencies and consumers: File 2 CLI, developer/CI invocation; no runtime process.
- Why now: wraps the tested classifier in the existing command.
- Contract/signature changes: exits nonzero on management `UNCONSUMED`; prints evidence rows without credentials.
- Input/output and state mapping: source roots -> report/exit status; no writes unless caller redirects.
- Error and edge behavior: missing roots fail; protocol/internal rows show reasons; no blanket ignore for dynamic methods.
- Standards impact: `MC-UTIL-001`, `MC-JSON-001`, `MC-CONFIG-001`, `MC-SCOPE-001`, `MC-TEST-001` — existing shell and stdlib helper only.
- Literal rule enforcement: `Rule 5`, `Rule 6`, `Rule 7`, `Rule 11` — allowed utilities, safe output, stable config and current operational tree.
- Implementation pseudocode:

```bash
python3 "${script_dir}/controller_ui_coverage.py" --repo-root "${unified_platform_repo_root}" --format text
audit_status=$?
[[ "${audit_status}" -eq 0 ]] || unified_platform_fail "external Controller/UI coverage has unexplained management methods"
```

- Verification contribution: shell command runs Python audit and retains existing key assertions.
- After this file: one command gives per-method coverage and an honest nonzero gate.

- Validation working directory: repository root.
- Verification command: `python3 scripts/unified-platform/controller_ui_coverage_test.py && bash -n scripts/unified-platform/check-admin-web-controller-coverage.sh && bash scripts/unified-platform/check-admin-web-controller-coverage.sh && git diff --check`
- Expected result: Python tests pass; shell exits 0; every external mapping has status/evidence; management rows are not unexplained; no runtime/secret/user-doc path is staged.
- Completion criteria: REQ-006/011 closed by reproducible audit and five semantic commits; all module static gates reported; TEST-011 remains explicitly runtime-unverified.
- Failure returns to: File 1/2 for parser errors; File 3 for shell integration; owning Step 2-4 for a true missing UI action. Do not suppress a management gap with a blanket exclusion.
- Rollback: revert only Step 5 paths; prior UI commits remain independently revertible and runtime/database state is untouched.
- Commit paths: `scripts/unified-platform/controller_ui_coverage.py`, `scripts/unified-platform/controller_ui_coverage_test.py`, `scripts/unified-platform/check-admin-web-controller-coverage.sh`.
- Commit: `test(platforms): audit every external controller against admin web`

## 8. Test, Validation, and Quality Gates

### 8.1 Per-Step RED/GREEN gates

| Step | RED evidence | GREEN focused command | Module gate | Commit gate |
| --- | --- | --- | --- | --- |
| Step 1 | shared/Wujie/child embedded tests fail on missing flags/options/sidebar | shared Layout + Portal tests/typecheck | Portal/shared and four child layout tests/typechecks | diff check, path status, no runtime start |
| Step 2 | directory/relation tests cannot find actions/requests | RBAC3 directory/relation/assignment tests | RBAC3 typecheck/build | exact Step 2 paths |
| Step 3 | role/catalog/constraint/policy tests cannot find action/detail | RBAC3 Step 3 tests | RBAC3 typecheck/build/resource report | exact Step 3 paths |
| Step 4 | Gateway/IDP/DDC tests observe unused/fake actions/old props | Gateway/IDP/DDC focused tests | three Web typecheck/builds | no unsupported production endpoint/old props |
| Step 5 | Python classifier/module absent | Python unittest + shell audit | all four Web final typecheck/build | final diff/path/audit gate |

### 8.2 Exact validation commands

Run from `/Users/mario/SelfProject/Egon-COLA` unless a package root is shown:

```bash
python3 scripts/unified-platform/controller_ui_coverage_test.py
bash -n scripts/unified-platform/start-local-stack.sh scripts/unified-platform/status-local-stack.sh scripts/unified-platform/check-admin-web-controller-coverage.sh
bash scripts/unified-platform/check-admin-web-controller-coverage.sh
```

```bash
cd egon-cola-platforms/egon-cola-platform-admin-web-shared && npm run test -- --run && npm run typecheck && npm run build
cd ../egon-cola-platform-admin-portal && npm run test -- --run && npm run typecheck && npm run build
cd ../egon-cola-platform-idp/egon-cola-platform-idp-admin-web && npm run test -- --run && npm run typecheck && npm run build
cd ../../egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin-web && npm run test -- --run && npm run typecheck && npm run build && npm run report:resources
cd ../../egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web && npm run test -- --run && npm run typecheck && npm run build
cd ../../egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin-web && npm run test -- --run && npm run typecheck && npm run build
```

Each command must run from its actual package root; reset to repository root if a relative `cd` is invalid after a failure. Expected result is exit 0 with no TypeScript/test/build failure. Maven is not a required gate because no Java/backend files are planned; an accidental Java or migration diff must be removed before commit.

### 8.3 Static quality assertions

```bash
rg -n "traceDetail\(" egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web/src
rg -n "destroyOnClose|<Space[^>]*direction=|<Spin[^>]*tip=" egon-cola-platforms/egon-cola-platform-admin-portal egon-cola-platforms/egon-cola-platform-idp egon-cola-platforms/egon-cola-platform-gateway egon-cola-platforms/egon-cola-platform-dynamic-config-center egon-cola-platforms/egon-cola-platform-admin-web-shared/src
rg -n "VITE_GATEWAY_ORIGIN|127\.0\.0\.1|UNIFIED_PLATFORM_ADVERTISED_HOST" scripts/unified-platform egon-cola-platforms/egon-cola-platform-admin-portal
```

The first search must have no production consumer after Step 4. The deprecation search must have no touched production usage; unrelated remaining usage is recorded rather than broad-refactored. The origin search must show loopback defaults and explicit LAN override only.

### 8.4 Runtime boundary

TEST-011 is not closed by static validation. After user-controlled clean restart and, if preflight requires it, current Gateway route publication, verify:

1. `http://127.0.0.1:18125/` and each Portal platform deep link show full child viewport plus child sidebar.
2. `http://127.0.0.1:18180/oauth2/login/csrf` returns 200; super-admin login succeeds via Cookie/CSRF; 18140 remains control-plane only.
3. IAM user detail loads organization/position/role relations; CRUD success refreshes and 409 is visible.
4. Gateway Release Diff/MCP validation and DDC/IDP routes respond through the public origin.

If preflight reports timeout/401, source is not claimed broken: old Java/Vite processes or stale active release must be restarted/re-published according to script output. The implementation turn does not start/stop the current stack automatically.

## 9. Migration, Compatibility, Rollout, and Rollback

- Database/Flyway: no new migration and no existing migration modification. After every Step, verify `git diff --name-only -- '*/db/*'` is empty.
- Backend compatibility: no Controller path, permission, tenant, JSON envelope, Cookie/CSRF, expected version, idempotency or error contract changes; frontend consumes existing methods only.
- Frontend compatibility: optional shared flags default to standalone shell; Portal manifest version and child props retain `embedded`, `routeIntent`, `scopeDisplay`, `hostVersion`; child BrowserRouter owns internal links.
- Configuration compatibility: `common.sh` URL key names remain; default values are loopback; `UNIFIED_PLATFORM_ADVERTISED_HOST` explicitly opts into LAN. Existing LAN users continue with an override but must restart to regenerate env/manifest.
- Rollout: commit Step 1 -> user-controlled clean restart/release preflight -> Steps 2-5 build-verified; final runtime proof after all Web bundles refresh. No automatic process start.
- Rollback: revert individual semantic commits in reverse order if needed. Never delete runtime DB, cookies, secrets, env, target jars or Flyway history. If only runtime is stale, restart/re-publish rather than revert source.
- Release safety: do not claim public login or Wujie DOM proof from npm build; runtime evidence remains partial until TEST-011.

## 10. Requirement-to-Step Traceability Matrix

| Requirement | Step 1 | Step 2 | Step 3 | Step 4 | Step 5 | Primary proof |
| --- | --- | --- | --- | --- | --- | --- |
| `REQ-001` | X |  |  |  |  | Wujie attrs/fixed viewport tests |
| `REQ-002` | X |  |  |  |  | Portal child route test |
| `REQ-003` | X |  |  |  |  | four child embedded layout tests |
| `REQ-004` | X |  |  |  |  | auth client + startup/status preflight |
| `REQ-005` | X |  |  |  |  | common/start/status loopback assertions |
| `REQ-006` |  |  | X | X | X | API/page action tests + per-method audit |
| `REQ-007` |  | X | X |  |  | IAM CRUD/relation tests |
| `REQ-008` |  | X | X |  |  | relation/role/resource/constraint tests |
| `REQ-009` | X | X | X | X | X | page/layout/status/build gates |
| `REQ-010` | X | X | X | X | X | security/deprecation/source audit |
| `REQ-011` | X | X | X | X | X | five path-limited commits and final gates |
| `REQ-012` |  |  |  | X | X | DDC UI/build and final controller audit |
| `REQ-013` | X | X | X | X | X | typed auth/PageState/403/409/timeout handling |
| `REQ-014` | X | X | X | X | X | pending locks, idempotency/version preservation and audit evidence |
| `REQ-015` | X | X | X | X | X | independent child entries and per-Step rollback |
| `REQ-016` |  |  |  |  | X | review-only Plan governance before execution |

## 11. Risks, Blockers, and User Decisions

| ID | Risk or question | Evidence | Impact | Mitigation/status |
| --- | --- | --- | --- | --- |
| `RISK-001` | Wujie srcdoc trick may still fail in a browser after safe fallback | current Portal log/EVD-001 and Wujie source/EVD-004 | child may remain unavailable | visible retry/standalone path; TEST-011 user runtime proof |
| `RISK-002` | running Gateway Engine/active release remains stale after source change | supervisor LAN override and 18180 timeout/EVD-002/003 | login remains unavailable until restart/re-publish | startup preflight names action; runtime state is not mutated |
| `RISK-003` | Java annotation paths or computed frontend API paths can evade parser | EVD-012 and Gateway `admin` composition | false manual/unconsumed status | parser tests, explicit registry evidence, owning Step follow-up |
| `RISK-004` | two sidebars reduce content width | EVD-005/ASM-002 | dense UI on narrow viewport | shared responsive drawer/overflow and manual review |
| `RISK-005` | Constraint DTO details may drift before execution | current `ConstraintController` imports/methods | wrong frontend payload | reopen DTO declarations before code; return to Spec on contract drift |

No unresolved major product decision remains. The only external-state decision is user-controlled restart/re-publish/browser proof, already closed by repository execution rules and not a code blocker for this Plan.

## 12. Review and Acceptance

### 12.1 Original requirement fidelity

The Plan covers the user’s three complaints: Portal child pages become fully visible with Wujie/domain navigation/UI fixes; login becomes loopback-first and diagnosable while distinguishing old process/IP/active-release failure; every external Controller is checked against a frontend action/API consumer, with IAM user/role/permission CRUD and relation CRUD explicitly owned by Steps 2-3.

### 12.2 Spec consistency

The Plan implements only the Accepted Spec’s existing Wujie/shared/API/Controller contracts. It does not introduce BFF, new backend endpoints, tables, migrations, dependencies, protocol CRUD pages or a backend Trace detail endpoint. Business catalog, constraint write forms and MCP per-capability validation are approved Spec elements; new focused tests are local proof support.

### 12.3 Repository executability

All production paths are existing Web/script locations except explicitly marked new component/page/audit tests. File order starts with compile prerequisites and RED tests, then minimum implementation and wiring. Each Step has focused commands, module gates, rollback point and one path-limited commit. Before execution, `git ls-files`/`test -e` must confirm each path.

### 12.4 Coverage and release safety

Controller coverage is static evidence, not runtime authorization proof. Runtime login, active Gateway release, cookies, Wujie iframe geometry and real CRUD remain user-controlled TEST-011. A build passing with the old supervisor does not prove the open Portal is refreshed; preflight and handoff keep that boundary explicit.

### 12.5 Blocking Manual Check

| Check ID | Applicability | Status | Evidence | Finding | Required action/exception |
| --- | --- | --- | --- | --- | --- |
| `MC-ARCH-001` | Applicable | PASS | §4.7 existing backend Traditional Three-Layer + feature-first Web; §5 paths | no new architecture profile | preserve structure |
| `MC-REUSE-001` | Applicable | PASS | §4.5/§4.7 capability audit; shared/Wujie/FeatureApi/Gateway clients exist | reuse sufficient; no duplicate client/BFF | use existing symbols |
| `MC-DEP-001` | Applicable | PASS | reuse ledger; no package/lockfile change | no new dependency | verify manifests/diff |
| `MC-NAME-001` | Not applicable | N/A | no Java type created; TS names follow current conventions | no Java naming surface | no Java naming action |
| `MC-VALID-001` | Applicable | PASS | Steps 2-4 preserve server validation, 403/409 and expected versions | required fields remain | focused negative/error tests |
| `MC-MODEL-001` | Not applicable | N/A | no Java model/entity/DTO/VO change | no model surface | no Java model action |
| `MC-CONVERT-001` | Not applicable | N/A | no Java mapper/converter change; TS maps existing JSON | no converter surface | no converter action |
| `MC-LOG-001` | Not applicable | N/A | no Java business/logging change | no logging surface | no Java logging action |
| `MC-BEAN-001` | Not applicable | N/A | no Spring Bean/injection/config change | no Bean surface | no Bean action |
| `MC-UTIL-001` | Applicable | PASS | stdlib Python and existing bash/curl/jq only | utility allowlist closed | import/command review |
| `MC-JSON-001` | Applicable | PASS | existing API envelopes/DTO paths; no sensitive display | no new envelope/leakage | API fixtures/source review |
| `MC-TIME-001` | Applicable | PASS | existing ISO time fields and expected versions in Steps 2-3 | no new Java time semantics | form/API tests/source search |
| `MC-CONFIG-001` | Applicable | PASS | common.sh key structure/loopback defaults; Step 1 preflight | explicit LAN override preserved | shell/default gate |
| `MC-PATTERN-001` | Applicable | PASS | §4.5/§4.7 Adapter/Reducer/Registry/Composition necessity | patterns solve real variation | focused tests |
| `MC-SCOPE-001` | Applicable | PASS | §5 path tree; no Java/db/BFF; untracked doc excluded | path-limited Steps | pre-commit status/diff |
| `MC-TEST-001` | Applicable | PASS | §4.2, §8, all five Step gates and TEST-001..011 | test-first/module/runtime boundaries explicit | execute before commits |
| `MC-BLOCKER-001` | Applicable | PASS | §11 has risks but no open design decision; runtime is approved user action | Plan can proceed after review | user reviews this Plan |

`PASS — Ready for user review`
