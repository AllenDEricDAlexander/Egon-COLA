# xingyuan Web 运行修复与 Controller 覆盖补齐实施计划

| Field | Value |
| --- | --- |
| Document | `2026-08-28-11-06-xingyuan-web-runtime-repair-and-controller-coverage.md` |
| Template Version | `4` |
| Status | `Ready` |
| Created | 2026-08-28 11:06 CST |
| Updated | 2026-08-28 11:06 CST |
| Owner | User / Egon-COLA xingyuan owner |
| Repository | Egon-COLA |
| Scope | Portal Wujie、shared 导航、Tianquan-Shoubing/Tianquan-Jianshen/Yuheng/Tianshu Admin Web、Tianshu Admin 查询/安全、本地 unified-xingyuan 地址策略 |
| Source Requirement | 用户要求直接修复 Portal Wujie 挂载失败、补齐四平台前端已有 Controller 功能、使用动态地址并默认 127.0.0.1 |
| Baseline Revision | `main@4b6e018aa0c1672c0491591b1b8387296f50bf8f`；保留未跟踪的用户文档 |
| Implements Spec | [xingyuan Web 运行修复与 Controller 覆盖补齐规格](../spec/2026-08-28-11-06-xingyuan-web-runtime-repair-and-controller-coverage.md) |
| Spec Status | `Accepted` |
| Spec Revision | `2026-08-28 11:06 CST` |
| Effective Specs | [xingyuan Web 运行修复与 Controller 覆盖补齐规格](../spec/2026-08-28-11-06-xingyuan-web-runtime-repair-and-controller-coverage.md) |
| Depends On Plans | None |
| Supersedes | [前序平台 Web 实施计划](2026-08-27-11-13-xingyuan-web-enterprise-implementation.md) §1-§12，仅就本次运行修复和覆盖范围取代 |
| Superseded By | None |
| Related Plans | [前序平台 Web 实施计划](2026-08-27-11-13-xingyuan-web-enterprise-implementation.md) |

## 1. Summary

本 Plan 实施已接受的运行修复 Spec，按 8 个可回滚 Step 执行：shared/Yuheng 防崩、地址策略、Wujie Core Facade、Tianshu 后端查询/安全、Tianshu 绑定页面、Tianquan-Jianshen IAM 覆盖、Yuheng 覆盖、最终审计。每个 Step 先写或扩展 RED，再做最小 GREEN，完成定向验证后只提交声明的路径。当前已有服务不由本 Plan 自动启动或停止。

## 2. Target Spec and Effective Design

### 2.1 Primary target

- Path: `docs/egon/spec/2026-08-28-11-06-xingyuan-web-runtime-repair-and-controller-coverage.md`
- Status: `Accepted`
- Revision: `2026-08-28 11:06 CST`, baseline `main@4b6e018aa0c1672c0491591b1b8387296f50bf8f`
- Approval evidence: 用户明确回复“直接开始修复”，并已确认 Wujie、IAM 单顶层多级菜单、六项平台设计决策。

### 2.2 Effective Spec set

| Role | Spec/link | Status/revision | Effective sections | Why included |
| --- | --- | --- | --- | --- |
| Primary | [运行修复规格](../spec/2026-08-28-11-06-xingyuan-web-runtime-repair-and-controller-coverage.md) | Accepted / 2026-08-28 11:06 | §3-§20 | 本次代码、测试和回滚边界 |
| Amendment | [前序企业级规格](../spec/2026-08-27-08-09-xingyuan-web-enterprise-microfrontend.md) | Review / 2026-08-27 11:44 | §12 的既有逐页 UI 基线 | 保留原有菜单和页面设计，不重复发明平台壳 |
| Dependency | [Tianshu 分页设计](../../superpowers/specs/2026-08-10-tianshu-admin-pagination-ui-modernization-design.md) | predecessor design | §1-§10 | 保留 PageResultRecord、兼容和不改数据库约束 |

### 2.3 Superseded or excluded content

前序 Plan 中“本轮只做文档、不实现 Wujie/后端/地址”的范围被本次 Accepted Spec 的 §16 取代；前序 Plan 的共享 Layout、独立子应用、同源认证和页面文字设计继续有效。Yuheng active release 的数据库发布、internal/RPC API、Flyway 历史修改均排除。

## 3. Effective Requirements and Acceptance

| Requirement | Source Spec section | Effective statement | Observable acceptance | Implementation impact |
| --- | --- | --- | --- | --- |
| REQ-001 | §4, §7, §12 | Portal 必须真实创建 Wujie 子实例并提供失败恢复 | 四个 child 深链出现子页面或稳定错误/重试/独立打开 | Portal Core Facade、测试、运行复核 |
| REQ-002 | §4, §7, §15 | 默认 127.0.0.1，显式 host 才改变广告地址 | shell assertion 与生成 Manifest URL 符合优先级 | 两个启动脚本、shell test |
| REQ-003 | §4, §7 | shared/Yuheng 父菜单无 path 不崩溃 | Yuheng `/dashboard` 首屏和 shared 深链测试成功 | sidebar guard/test |
| REQ-004 | §4, §7, §12 | 已有 external Controller 功能有 API 和页面动作/详情 | Tianquan-Jianshen、Yuheng、Tianshu 覆盖路径可调用；Tianquan-Shoubing 覆盖不重复实现 | API/page/route additions |
| REQ-005 | §4, §7 | Tianshu page 读权限和空筛选查询可用 | TIANSHU_READ 可访问 `/page`；PostgreSQL 不再报 lower(bytea) | Security matcher、JPQL、Maven tests |
| REQ-006 | §4, §12 | IAM 保持一个顶层树及子菜单 | 新增权限等入口位于 IAM 子树 | Tianquan-Jianshen resource definitions/navigation |
| REQ-007 | §4, §12 | 每个新增/调整页面具备完整文字布局和状态 | 页面代码对应 Spec §12 的 header/filter/content/action/state | component/page tests |
| REQ-008 | §4, §14, §16 | Step 独立验证、一次提交、可回滚 | 每 Step 命令和 commit scope 可复核 | all Steps |
| REQ-009 | §4, §9 | 不越过 Admin API 边界 | browser/client source 无新增 internal/DB/RPC 直连 | API review/static search |
| REQ-010 | §4, §7, §16 | 区分源码覆盖、前端未接入、active release 漂移 | final report/coverage script 分栏输出 | coverage audit |

## 4. Implementation Strategy and Dependency Order

### 4.1 Ordered strategy

先修共享崩溃，因为所有 Web 都依赖它；随后修启动地址，保证后续运行验证使用稳定 origin；再替换 Portal 的 Wujie wrapper 调用，以便后续 child 页面可以真实显示。Tianshu 后端先于 Tianshu 页面，Tianquan-Jianshen/Yuheng 页面只消费已有 Controller 合同；最后运行静态覆盖审计、四 Web typecheck/build、shared build 和 Tianshu focused Maven 测试。数据库没有迁移步骤。

### 4.2 Test-first strategy

| Behavior | RED test | Minimum GREEN |
| --- | --- | --- |
| undefined navigation path | shared selection test passes parent with undefined/non-string values and initially reaches `endsWith` failure | `matchesPath` runtime type guard |
| loopback default | shell policy test asserts current auto-probe is absent and default is loopback | explicit-first resolver + declared host merge |
| Wujie mount result | mock Core `startApp` reject/resolve and assert event order | Core start/cleanup effect and error mapping |
| Tianshu page authorization/query | security/controller/repository test exercises `/page` and empty params | exact GET matchers + string-safe JPQL predicate |
| binding UI | page test initially has no route/API component | binding page API/query/mutations/menu |
| Tianquan-Jianshen role/permission/user coverage | API/page tests assert missing method/path/action | API methods, route descriptors, forms/actions |
| Yuheng OpenAPI/application details | page/API tests assert existing controller paths are not orphaned | detail drawer and sync page |

### 4.3 Sequential and parallel boundaries

| Step | Depends on | May run in parallel with | Must not overlap with | Reason |
| --- | --- | --- | --- | --- |
| Step 1 | None | None | shared layout files | Yuheng cannot render until shared guard exists |
| Step 2 | Step 1 | None | unified scripts | address policy must be stable before runtime proof |
| Step 3 | Step 2 | None | Portal lifecycle/package files | Wujie runtime test depends on address/manifest contract |
| Step 4 | Step 1 | Step 5 after backend test lock | Tianshu Java files | page errors need backend boundary fixed |
| Step 5 | Step 4 | None | Tianshu Web layout/pages | binding page uses corrected page/auth behavior |
| Step 6 | Step 1 | Step 7 | Tianquan-Jianshen Web feature files | IAM routes/API additions must remain coherent |
| Step 7 | Step 1 | Step 6 | Yuheng Web feature files | Yuheng details consume existing shared guard |
| Step 8 | Steps 1-7 | None | all target paths | final audit must inspect complete tree |

### 4.4 Commit boundaries

Each Step gets one path-limited commit. Generated shared `dist`/Vite output is rebuilt only when the repository tracks it; unrelated user files and existing runtime logs are never staged. If package lock regeneration changes unrelated dependency entries, the Step returns to review and only the same-version Wujie entry is retained.

### 4.5 Spec Simplicity and Implementation-necessity Audit

| Spec element | Spec necessity verdict/section | Current repository evidence | Direct/reuse alternative | Interaction/implementation cost | Plan decision |
| --- | --- | --- | --- | --- | --- |
| Wujie Core Facade | Spec §7.0; required by mount error evidence | `WujieChild.tsx`, Wujie React wrapper catch | wrapper JSX | one start Promise and destroy reference | Implement |
| Runtime Manifest | Spec §7.3.2; existing generator | `write_portal_manifest` and loader | four compile-time URLs | one JSON fetch | Already exists, clarify consumer |
| Sidebar guard | Spec §7.0; runtime crash | `EnterpriseSidebar.tsx` optional path | trust TypeScript | zero network/state | Implement |
| Tianshu binding page | Spec §12.6; external binding Controller | `DdcNamespaceEnvAppBindingController` | hidden Namespace drawer only | one page query and mutations | Implement |
| Tianquan-Jianshen permission route | Spec §12.4; permission Controller already external | `PermissionController`, hidden JSON route | leave hidden | one route/component | Implement |
| Yuheng OpenAPI sync page | Spec §12.5; OpenAPI Controller and API already exist | `gatewayApi.openapiSyncStates` | only Application expandable rows | one route/page query | Implement |
| New BFF/database/microfrontend | Spec §3.2 | no repository evidence of need | existing Admin API | new deployment/contract/failure source | Remove |

### 4.6 Change-unit Dependency Matrix

| Change unit | Requirements | Proof/RED point | Compile/runtime prerequisites | Produces | Consumers/unblocks | Owning Step |
| --- | --- | --- | --- | --- | --- | --- |
| Shared path safety | REQ-003 | undefined candidate test | shared deps | safe selection | Yuheng layout | Step 1 |
| Local host policy | REQ-002 | shell assertions | common URL defaults | stable env/Manifest inputs | Wujie/runtime | Step 2 |
| Wujie lifecycle | REQ-001, REQ-007 | Core mock reject/resolve | Wujie package | host instance/cleanup | Portal child routes | Step 3 |
| Tianshu page security/query | REQ-005, REQ-009 | matcher/JPA tests | PostgreSQL test profile | usable page API | Tianshu pages | Step 4 |
| Tianshu binding route | REQ-004, REQ-007 | page query/mutation test | Step 4 API | `/bindings` page | Tianshu menu | Step 5 |
| Tianquan-Jianshen IAM additions | REQ-004, REQ-006, REQ-007 | API/path/page tests | existing resource registry | permission/role/user actions | IAM tree | Step 6 |
| Yuheng detail/sync | REQ-004, REQ-007 | API/page tests | existing yuheng client | OpenAPI/application views | Yuheng menu | Step 7 |
| Final coverage audit | REQ-008, REQ-010 | static inventory | Steps 1-7 commits | separated report | delivery | Step 8 |

### 4.7 Java, Spring, and Egon-COLA Implementation Standards

| Concern | Current repository evidence | Effective Spec decision | Planned implementation consequence | Owning Steps/checks |
| --- | --- | --- | --- | --- |
| Architecture profile | Tianshu `controller/service/repository`; Tianquan-Jianshen existing feature-first tree | Spec §6.1 selects traditional three-layer semantics for touched Tianshu files | no new layer/package; only existing Security config and Repository query edits | Step 4; MC-ARCH-001, MC-BLOCKER-001 |
| Reuse/capability | Spring Security matcher, Spring Data JPA, existing PageState/Wujie | reuse current versions and abstractions | no new Spring starter or utility dependency | Steps 3-5; MC-REUSE-001, MC-DEP-001 |
| Naming/model/validation/conversion | Tianshu query only; no new Java model | no new POJO/DTO/Converter; preserve existing Controller validation | no model or hand-copy changes | Step 4; MC-NAME-001, MC-MODEL-001, MC-VALID-001, MC-CONVERT-001 |
| Bean/logging/util/JSON/time/config | no business Bean/profile/JSON/time change | preserve existing Bean and envelope contracts | no new annotation/config key/date type | Step 4; MC-BEAN-001, MC-LOG-001, MC-UTIL-001, MC-JSON-001, MC-TIME-001, MC-CONFIG-001 |
| Business variation/pattern | Wujie lifecycle has state transitions; existing reducer | Spec §13 selects Facade + reducer; static APIs stay direct | implement actual Core adapter and tests, no ceremony elsewhere | Step 3; MC-PATTERN-001 |

#### Capability reuse ledger

| Need | Candidates inspected | Exact evidence | Fit/gap | Decision | Added dependency/custom code | Owning Step/check |
| --- | --- | --- | --- | --- | --- | --- |
| Wujie runtime | `wujie-react`, `wujie` 2.1.0 | Portal lockfile and Core `startApp` types | wrapper hides reject; Core fits | reuse Core | direct dependency entry same version | Step 3; MC-REUSE-001 |
| UI shell/state | shared EnterpriseLayout/PageState, React Query | shared source and existing pages | sufficient | reuse | none | Steps 1,5-7; MC-REUSE-001 |
| Page auth | Tianshu SecurityFilterChain | Tianshu SecurityConfiguration | matcher gap only | modify existing config | none | Step 4; MC-DEP-001 |
| String query typing | JPA JPQL coalesce | Ddc*Repository.search | direct query fix fits | reuse Spring Data | none | Step 4; MC-VALID-001 |

### 4.8 User-mandated Java Rule Implementation Matrix

| Literal rule | Spec source | Repository evidence | Exact files and order | Pseudocode obligations | Validation gate | Steps | Status/blocker |
| --- | --- | --- | --- | --- | --- | --- | --- |
| Rule 1 | Spec §6.2/§8 | no Java type created | inspect existing Tianshu repositories/config only | no new carrier | `rg` type inventory + compile | Step 4 | PASS |
| Rule 2 | Spec §6.2/§7 | Tianshu page path/query handoff | Security config -> existing Service -> Repository | preserve existing validation; query string normalization semantics | focused controller/JPA tests | Step 4 | PASS |
| Rule 3 | Spec §6.2/§10 | Entity/DTO/VO unchanged | no model file in scope | no conversion code | diff/type search | Step 4 | PASS |
| Rule 4 | Spec §6.2/§7 | no Service/Bean class changed | no business class file | no new injection | diff/compile | Step 4 | PASS |
| Rule 5 | Spec §6.2/§15 | no dependency/helper additions except Wujie Web package | existing Spring APIs only | no utility import | package/import search | Steps 3-4 | PASS |
| Rule 6 | Spec §6.2/§9 | existing ResultRecord/PageResultRecord | no external DTO changed | keep JSON envelope | controller/page response tests | Step 4 | PASS |
| Rule 7 | Spec §6.2/§15 | no Spring config key changed | no profile file | preserve key structure | profile diff/search | Steps 2,4 | PASS |
| Rule 9 | Spec §6.2/§13 | lifecycle state has real variation | `WujieChild` + reducer | Facade maps start/destroy/failure | lifecycle test | Step 3 | PASS |
| Rule 10 | Spec §6.2/§10/§15 | no Java time field touched | no date API change | preserve existing `java.time` | import search | Step 4 | PASS |
| Rule 11 | Spec §6.1/§6.2/§8 | Tianshu current three-layer tree | Security config and Repository stay in place | no third/hybrid tree | architecture tree + Maven | Every Step | PASS |

## 5. Change File Tree

```text
MODIFY egon-cola-xingyuan/egon-cola-xingyuan-admin-web-shared/src/layout/EnterpriseSidebar.tsx
MODIFY egon-cola-xingyuan/egon-cola-xingyuan-admin-web-shared/src/layout/EnterpriseLayout.test.tsx
MODIFY scripts/unified-xingyuan/start-local-stack.sh
MODIFY scripts/unified-identity-local.sh
CREATE scripts/unified-xingyuan/test-local-address-policy.sh
MODIFY egon-cola-xingyuan-admin-portal/package.json
MODIFY egon-cola-xingyuan-admin-portal/package-lock.json
MODIFY egon-cola-xingyuan-admin-portal/src/lifecycle/WujieChild.tsx
MODIFY egon-cola-xingyuan-admin-portal/src/test/setup.ts
CREATE egon-cola-xingyuan-admin-portal/src/lifecycle/WujieChild.test.tsx
MODIFY egon-cola-tianshu/.../DdcAdminSecurityConfiguration.java
MODIFY egon-cola-tianshu/.../DdcBizRepository.java
MODIFY egon-cola-tianshu/.../DdcEnvRepository.java
MODIFY egon-cola-tianshu/.../DdcAppRepository.java
MODIFY egon-cola-tianshu/.../DdcNamespaceRepository.java
MODIFY egon-cola-tianshu/.../DdcPublishTaskRepository.java
MODIFY egon-cola-tianshu/.../DdcNamespaceEnvAppBindingRepository.java
MODIFY Tianshu admin web App.tsx/AdminLayout.tsx/api/types.ts
CREATE Tianshu admin web src/pages/BindingsPage.tsx
CREATE Tianshu admin web src/pages/BindingsPage.test.tsx
MODIFY Tianquan-Jianshen role/directory/application/navigation/resource definition files
CREATE Tianquan-Jianshen PermissionCatalogPage.tsx and test
MODIFY Yuheng gatewayApi/ApplicationsPage/TracesPage/AdminLayout/App.tsx
CREATE Yuheng OpenApiSyncPage.tsx and test
CREATE scripts/unified-xingyuan/check-admin-web-controller-coverage.sh
```

| Operation | Path | Current evidence/symbol | Final symbols/state | Responsibility | Step | Requirements | Validation owner |
| --- | --- | --- | --- | --- | --- | --- | --- |
| MODIFY | `egon-cola-xingyuan/egon-cola-xingyuan-admin-web-shared/src/layout/EnterpriseSidebar.tsx` | `matchesPath` calls string methods | unknown-safe path match | navigation safety | Step 1 | REQ-003 | shared test |
| MODIFY | `scripts/unified-xingyuan/start-local-stack.sh` | auto-detected LAN candidate | loopback default and explicit override | address policy | Step 2 | REQ-002 | shell test |
| MODIFY | `egon-cola-xingyuan/egon-cola-xingyuan-admin-portal/src/lifecycle/WujieChild.tsx` | wrapper-only mount | Core Promise/destroy Facade | Wujie lifecycle | Step 3 | REQ-001 | Portal test/build |
| MODIFY | `egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/java/top/egon/cola/component/tianshu/admin/security/management/DdcAdminSecurityConfiguration.java` | page paths fall through denyAll | explicit GET page matchers | Tianshu read authorization | Step 4 | REQ-005 | Maven security test |
| MODIFY | `egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/java/top/egon/cola/component/tianshu/admin/repository/DdcBizRepository.java` | nullable keyword JPQL | string-safe empty predicate | Tianshu query typing | Step 4 | REQ-005 | Maven repository test |
| CREATE | `egon-cola-tianshu/egon-cola-tianshu-admin-web/src/pages/BindingsPage.tsx` | no standalone binding route | paged CRUD page | Tianshu binding UI | Step 5 | REQ-004, REQ-007 | Tianshu Web test |
| CREATE | `egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin-web/src/features/permission/PermissionCatalogPage.tsx` | hidden permission definition lacks component | list/create/status page | IAM permission UI | Step 6 | REQ-004, REQ-006 | Tianquan-Jianshen test |
| CREATE | `egon-cola-yuheng/yuheng-admin-web/src/features/openapi/OpenApiSyncPage.tsx` | OpenAPI API exists without dedicated route | sync state/document view | Yuheng UI | Step 7 | REQ-004, REQ-007 | Yuheng test |
| CREATE | `scripts/unified-xingyuan/check-admin-web-controller-coverage.sh` | no repeatable separated audit | external route coverage report | final audit | Step 8 | REQ-008, REQ-010 | shell/static search |

## 6. Prerequisites, Constraints, and Plan Clarifications

### 6.1 Repository and worktree baseline

- Branch `main`, baseline `4b6e018aa0c1672c0491591b1b8387296f50bf8f`.
- Preserve `docs/egon/spec/2026-08-27-20-31-archetype-two-stage-source-generation.md`; it is not part of any commit scope.
- Do not modify existing `classpath:db` migration files.
- Existing services may remain running for later user-led verification; implementation commands do not restart them.
- Use `apply_patch` for source edits and path-limited `git add` for each Step.

### 6.2 Build, test, and environment prerequisites

| Concern | Exact command/source | Required state | Validation boundary |
| --- | --- | --- | --- |
| Portal | `npm run typecheck && npm run test -- --runInBand` in Portal | Node/npm dependencies installed | static Web |
| Shared | `npm run build && npm run test -- --runInBand` in shared | local package dependencies installed | shared build/Web |
| Tianquan-Jianshen | `npm run typecheck && npm run test -- --runInBand` | SDK and local shared package available | static Web |
| Yuheng | `npm run typecheck && npm run test -- --runInBand` | local shared package available | static Web |
| Tianshu Web | `npm run typecheck && npm run test -- --runInBand` | Tianshu dependencies available | static Web |
| Tianshu Java | `./mvnw -pl egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin -am -DskipTests compile` plus focused tests | Maven/PostgreSQL test prerequisites | Java static/focused |
| Shell | `bash -n scripts/unified-xingyuan/start-local-stack.sh scripts/unified-identity-local.sh scripts/unified-xingyuan/test-local-address-policy.sh scripts/unified-xingyuan/check-admin-web-controller-coverage.sh` | Bash and rg available | script/static |

### 6.3 Immutable constraints and approved decisions

Wujie Core version remains 2.1.0; existing public API paths, permission checks, JSON envelope, child lifecycle names and standalone entry paths remain compatible. No new database migration, no BFF, no internal API exposure, no automatic active Yuheng release publication, and no automatic process start/stop.

### 6.4 Plan Clarifications

| ID | Small implementation inference | Repository evidence | Why semantics are unchanged | Impact if wrong |
| --- | --- | --- | --- | --- |
| PLAN-CLAR-001 | Tianshu repository predicates may use `coalesce(:param, '') = ''` to retain null-as-unfiltered semantics | Ddc*Repository JPQL and PostgreSQL failure | only parameter typing changes; result predicate remains unfiltered for null/blank | focused JPA test blocks Step if provider rejects syntax |
| PLAN-CLAR-002 | OpenAPI sync page consumes existing `gatewayApi` methods rather than adding controller calls | `gatewayApi.openapiSyncStates` and `openapiSnapshotDocument` | no server contract or network boundary changes | page remains unavailable if active release lacks route |
| PLAN-CLAR-003 | Tianquan-Shoubing needs no duplicate management page because its external admin Controller coverage is already present | Tianquan-Shoubing App/router/features and Controller inventory | final audit records Tianquan-Shoubing as covered | future uncovered Tianquan-Shoubing endpoint is reported, not silently invented |

## 7. Ordered File-by-file Implementation Steps

### Step 1 — Make shared path selection safe and restore Yuheng first render

- Requirements: `REQ-003, REQ-006`
- Dependencies: `None`
- Baseline state: `EnterpriseNavigationItem.path` is optional; Yuheng parent nodes omit path; runtime `endsWith` failure is recorded in EVD-001.
- Observable outcome: parent navigation and invalid runtime candidates are ignored safely, while valid deep-link child selection still uses the longest matching path.
- End state: shared guard and regression test are committed; no xingyuan API or menu hierarchy is changed.
- Validation working directory: repository root for path review, then `egon-cola-xingyuan/egon-cola-xingyuan-admin-web-shared` for tests/build.
- Test-first gate: `Required — the regression supplies undefined and non-string candidates and fails before the runtime type guard exists.`
- Manual Checks: `MC-ARCH-001, MC-REUSE-001, MC-NAME-001, MC-UTIL-001, MC-SCOPE-001, MC-TEST-001`
- Literal Rules: `Rule 1, Rule 5, Rule 11`
- Ordered files:

#### File 1 — `MODIFY egon-cola-xingyuan/egon-cola-xingyuan-admin-web-shared/src/layout/EnterpriseSidebar.tsx`

- Purpose: Guard path matching at the runtime boundary for parent and malformed navigation entries.
- Symbols: `matchesPath`, `resolveNavigationSelection`.
- Repository evidence: `EnterpriseNavigationItem.path?` and Yuheng `filterNavigation` return parent entries without `path`.
- Dependencies and consumers: `EnterpriseLayout`, all four Admin Web sidebars, React Router pathname.
- Why now: the Yuheng error occurs before useful page content is rendered.
- Contract/signature changes: `matchesPath` accepts `unknown`; exported navigation selection behavior for valid strings is unchanged.
- Input/output and state mapping: navigation candidate/active prefix + pathname -> boolean match -> selected leaf and ancestor keys.
- Error and edge behavior: undefined, null, object, non-absolute path, and prefix boundary mismatch return false without throwing.
- Standards impact: `MC-ARCH-001, MC-REUSE-001, MC-NAME-001, MC-UTIL-001`; frontend-only, no Java model or dependency.
- Literal rule enforcement: `Rule 1, Rule 5, Rule 11` — preserve existing type names, use no new utility dependency, and keep the shared layout tree in place.
- Implementation pseudocode:

```ts
const matchesPath = (candidate: unknown, pathname: string): boolean => {
  if (typeof candidate !== 'string' || typeof pathname !== 'string') return false
  if (!candidate.startsWith('/')) return false
  if (candidate === '/') return pathname === '/'
  const boundary = candidate.endsWith('/') ? candidate : `${candidate}/`
  return pathname === candidate || pathname.startsWith(boundary)
}
```

- Verification contribution: shared test calls selection with malformed parent values and valid nested paths.
- After this file: path resolution is total over runtime navigation data; no unrelated layout behavior changes.

#### File 2 — `MODIFY egon-cola-xingyuan/egon-cola-xingyuan-admin-web-shared/src/layout/EnterpriseLayout.test.tsx`

- Purpose: Lock the regression and preserve longest-prefix deep-link behavior.
- Symbols: new `ignores malformed parent paths` test and existing deep-link test.
- Repository evidence: existing nested menu and deep-link tests already render `EnterpriseLayout` with `MemoryRouter`.
- Dependencies and consumers: `resolveNavigationSelection` through shared layout and Ant Design Menu.
- Why now: the production error must be proven as a test failure before the guard is accepted.
- Contract/signature changes: no public production signature; test-only malformed input fixture.
- Input/output and state mapping: parent with `path: undefined` and invalid active prefix -> no exception; valid child -> selected class.
- Error and edge behavior: test fails only on thrown path method error, not on missing provider or fixture data.
- Standards impact: `MC-TEST-001, MC-SCOPE-001`; no Java or dependency impact.
- Literal rule enforcement: `Rule 5, Rule 11` — test uses existing framework and remains under shared layout test tree.
- Implementation pseudocode:

```ts
it('ignores malformed parent paths without breaking valid child selection', () => {
  const result = resolveNavigationSelection([{ key: 'parent', label: '父级', path: undefined,
    activePathPrefixes: [undefined as unknown as string], children: [{ key: 'child', label: '子级', path: '/child' }] }], '/child/detail')
  expect(result.selectedKey).toBe('child')
  expect(result.ancestorKeys).toEqual(['parent'])
})
```

- Verification contribution: RED proves the previous `endsWith` failure; GREEN proves the Yuheng navigation contract.
- After this file: targeted shared test is ready for Step commit.

- Verification command: `cd egon-cola-xingyuan/egon-cola-xingyuan-admin-web-shared && npm run test -- --runInBand && npm run build`
- Expected result: all shared tests pass and library build exits 0; malformed candidate does not throw.
- Completion criteria: both files changed only for this guard/regression; command output is captured; runtime Yuheng restart is not claimed.
- Failure returns to: the failing test or the exact TypeScript type error; do not change Yuheng APIs.
- Rollback: `git revert <Step-1-commit>`.
- Commit paths: `egon-cola-xingyuan/egon-cola-xingyuan-admin-web-shared/src/layout/EnterpriseSidebar.tsx`, `egon-cola-xingyuan/egon-cola-xingyuan-admin-web-shared/src/layout/EnterpriseLayout.test.tsx`
- Commit: `fix(admin-web): guard optional navigation paths`

### Step 2 — Make local address resolution loopback-first with explicit override

- Requirements: `REQ-002, REQ-008, REQ-010`
- Dependencies: `Step 1`
- Baseline state: `start-local-stack.sh` auto-detects the default interface and rejects 127.0.0.1; identity script keeps only a hard-coded loopback declared host.
- Observable outcome: no override produces 127.0.0.1 URLs; an explicit host is used for advertised child URLs while declared hosts retain loopback.
- End state: scripts and policy test are committed; running services are untouched.
- Validation working directory: repository root; the policy script itself changes no runtime directory.
- Test-first gate: `Required — shell policy assertions fail while the LAN auto-detection and loopback rejection remain.`
- Manual Checks: `MC-ARCH-001, MC-REUSE-001, MC-CONFIG-001, MC-SCOPE-001, MC-TEST-001`
- Literal Rules: `Rule 5, Rule 7, Rule 11`
- Ordered files:

#### File 1 — `MODIFY scripts/unified-xingyuan/start-local-stack.sh`

- Purpose: Set explicit environment override precedence and stable loopback default.
- Symbols: `resolve_local_advertised_host`, `local_advertised_host`.
- Repository evidence: current function probes `route`, `ipconfig`, and `hostname -I`, then rejects loopback.
- Dependencies and consumers: `UNIFIED_IDENTITY_ADVERTISED_HOST`, generated child env files, `write_portal_manifest`.
- Why now: Wi-Fi changes currently alter the host used by the local stack.
- Contract/signature changes: `UNIFIED_XINGYUAN_ADVERTISED_HOST` remains the override; default changes to `127.0.0.1`.
- Input/output and state mapping: explicit non-empty host -> validated host; absent/loopback -> 127.0.0.1; output feeds existing URL generation.
- Error and edge behavior: blank override uses loopback; malformed host is rejected by existing safe validation; no interface scan.
- Standards impact: `MC-CONFIG-001, MC-SCOPE-001`; shell configuration only, no Java profile.
- Literal rule enforcement: `Rule 5, Rule 7, Rule 11` — use Bash/JDK-independent existing script utilities, keep config structure stable, preserve script tree.
- Implementation pseudocode:

```bash
resolve_local_advertised_host() {
  local candidate="${UNIFIED_XINGYUAN_ADVERTISED_HOST:-127.0.0.1}"
  [[ -n "${candidate}" ]] || candidate="127.0.0.1"
  [[ "${candidate}" != *[[:space:]/\\]* ]] || unified_xingyuan_fail "invalid advertised host"
  printf '%s' "${candidate}"
}
```

- Verification contribution: shell test observes default and explicit host behavior from source-level function contract.
- After this file: no network interface detection is used to choose the default.

#### File 2 — `MODIFY scripts/unified-identity-local.sh`

- Purpose: Keep declared host allowlist compatible with both local and explicitly advertised addresses.
- Symbols: `advertised_host`, `declared_hosts`.
- Repository evidence: `advertised_host` already defaults to 127.0.0.1 while `declared_hosts` is fixed to one value.
- Dependencies and consumers: generated Tianshu/Tianquan-Shoubing/Tianquan-Jianshen/Yuheng env files and Yuheng allowed CIDR.
- Why now: an explicit host must not be rejected by downstream host declarations.
- Contract/signature changes: declared hosts become `127.0.0.1` plus explicit non-loopback host; default output is unchanged.
- Input/output and state mapping: advertised host -> CSV declared hosts -> generated environment files.
- Error and edge behavior: localhost/127.0.0.1 is not duplicated; no host is removed from the default.
- Standards impact: `MC-CONFIG-001, MC-SCOPE-001`; no Java class/profile.
- Literal rule enforcement: `Rule 7, Rule 11` — preserve generated key structure and existing script architecture.
- Implementation pseudocode:

```bash
advertised_host="${UNIFIED_IDENTITY_ADVERTISED_HOST:-127.0.0.1}"
declared_hosts="127.0.0.1"
if [[ "${advertised_host}" != "127.0.0.1" && "${advertised_host}" != "localhost" ]]; then
  declared_hosts+=",${advertised_host}"
fi
```

- Verification contribution: test checks generated host list semantics for default and explicit modes.
- After this file: downstream registration accepts the selected explicit host without sacrificing loopback.

#### File 3 — `CREATE scripts/unified-xingyuan/test-local-address-policy.sh`

- Purpose: Provide repeatable non-destructive assertions for the address policy.
- Symbols: script-level `assert_contains` and policy assertions.
- Repository evidence: start/identity scripts are deterministic source inputs and do not expose a pure function module.
- Dependencies and consumers: Bash, `rg`, the two launch scripts.
- Why now: prevents a future reintroduction of Wi-Fi probing or loopback rejection.
- Contract/signature changes: new developer-only check script; it starts no process and changes no file.
- Input/output and state mapping: source text -> assertions -> exit 0/1.
- Error and edge behavior: missing required pattern exits non-zero with file context; no destructive operation.
- Standards impact: `MC-TEST-001, MC-SCOPE-001`; no Java impact.
- Literal rule enforcement: `Rule 5, Rule 11` — use existing shell tooling and keep the script in `scripts/unified-xingyuan`.
- Implementation pseudocode:

```bash
set -euo pipefail
root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
rg -q 'UNIFIED_XINGYUAN_ADVERTISED_HOST:-127\.0\.0\.1' "${root}/scripts/unified-xingyuan/start-local-stack.sh"
rg -q 'declared_hosts="127\.0\.0\.1"' "${root}/scripts/unified-identity-local.sh"
! rg -q 'a non-loopback provider host is required' "${root}/scripts/unified-xingyuan/start-local-stack.sh"
```

- Verification contribution: directly proves default source policy and absence of the old rejection contract.
- After this file: the address policy has a cheap shell regression gate.

- Verification command: `bash -n scripts/unified-xingyuan/start-local-stack.sh scripts/unified-identity-local.sh scripts/unified-xingyuan/test-local-address-policy.sh && scripts/unified-xingyuan/test-local-address-policy.sh`
- Expected result: syntax and policy assertions exit 0; no process starts.
- Completion criteria: both default and explicit host source paths are covered; unrelated runtime env files are untouched.
- Failure returns to: exact assertion and source line; do not alter URL generator without evidence.
- Rollback: `git revert <Step-2-commit>`.
- Commit paths: `scripts/unified-xingyuan/start-local-stack.sh`, `scripts/unified-identity-local.sh`, `scripts/unified-xingyuan/test-local-address-policy.sh`
- Commit: `fix(xingyuan): default local stack to loopback host`

### Step 3 — Replace hidden Wujie mount failures with a Core lifecycle Facade

- Requirements: `REQ-001, REQ-007, REQ-008, REQ-009`
- Dependencies: `Step 2`
- Baseline state: Portal renders `wujie-react` JSX; wrapper catches `startApp` rejection internally, and the Portal host remains empty in the recorded DOM check.
- Observable outcome: each child creates a Wujie Core instance in a concrete host; resolve/reject/cleanup map to reducer events and the failure UI.
- End state: Portal uses locked Wujie 2.1.0 Core with a testable cleanup handle; child code/lifecycle protocol is unchanged.
- Validation working directory: `egon-cola-xingyuan/egon-cola-xingyuan-admin-portal`.
- Test-first gate: `Required — Core mock rejects or resolves while the old wrapper-based component cannot expose either result to the reducer.`
- Manual Checks: `MC-ARCH-001, MC-REUSE-001, MC-DEP-001, MC-PATTERN-001, MC-JSON-001, MC-SCOPE-001, MC-TEST-001`
- Literal Rules: `Rule 5, Rule 6, Rule 9, Rule 11`
- Ordered files:

#### File 1 — `MODIFY egon-cola-xingyuan/egon-cola-xingyuan-admin-portal/package.json`

- Purpose: Declare the already-resolved Wujie Core as the direct dependency required by the Facade.
- Symbols: `dependencies.wujie`.
- Repository evidence: `node_modules/wujie` is present at 2.1.0 and Core exports `startApp`/`destroyApp` types.
- Dependencies and consumers: Portal Vite bundle and `WujieChild.tsx`.
- Why now: a direct import must not rely on an implementation-only transitive package.
- Contract/signature changes: add `wujie: 2.1.0` and remove the unused React wrapper; keep child protocol aligned.
- Input/output and state mapping: package manifest -> npm resolution -> Core API import.
- Error and edge behavior: lock version mismatch returns to Step review; no second Wujie version is accepted.
- Standards impact: `MC-DEP-001, MC-SCOPE-001`; frontend dependency only, no Java utility.
- Literal rule enforcement: `Rule 5, Rule 11` — dependency is an evidenced existing Core package and remains under Portal.
- Implementation pseudocode:

```json
{
  "dependencies": {
    "wujie": "2.1.0"
  }
}
// The Portal imports the Core contract so startApp rejection is observable.
// A single resolved Core version prevents duplicate sandbox registries.
```

- Verification contribution: package lock and `npm ls wujie` prove one compatible Core version.
- After this file: source may import the exact Core contract without an accidental transitive dependency.

#### File 2 — `MODIFY egon-cola-xingyuan/egon-cola-xingyuan-admin-portal/package-lock.json`

- Purpose: Record the direct same-version dependency without unrelated lock churn.
- Symbols: root dependency entry and `node_modules/wujie` resolution.
- Repository evidence: lockfile already contains `node_modules/wujie` at 2.1.0 through `wujie-react`.
- Dependencies and consumers: npm install/build.
- Why now: reproducible Portal builds need the direct dependency reflected in the lockfile.
- Contract/signature changes: only root dependency metadata and existing package linkage may change.
- Input/output and state mapping: package JSON -> lockfile graph -> Vite module resolution.
- Error and edge behavior: any unrelated package upgrade fails the Step scope check.
- Standards impact: `MC-DEP-001, MC-SCOPE-001`; no Java impact.
- Literal rule enforcement: `Rule 5, Rule 11` — no unapproved utility/framework dependency or tree change.
- Implementation pseudocode:

```text
npm install --package-lock-only --ignore-scripts
assert root dependency wujie == 2.1.0
assert node_modules/wujie resolution remains 2.1.0
assert diff contains no unrelated package version change
```

- Verification contribution: lockfile diff review plus `npm ls` proves deterministic Core resolution.
- After this file: package graph is ready for production source import.

#### File 3 — `MODIFY egon-cola-xingyuan/egon-cola-xingyuan-admin-portal/src/lifecycle/WujieChild.tsx`

- Purpose: Adapt Wujie Core lifecycle to the existing Portal reducer and host error boundary.
- Symbols: `WujieChild`, Core `startApp`, cleanup reference/effect.
- Repository evidence: current callbacks and `reduceLifecycle` already define LOAD/MOUNTED/MOUNT_FAILED/UNMOUNT/CLEANED.
- Dependencies and consumers: `ChildRoutePage`, `ChildManifest`, `ChildContext`, child `__WUJIE_MOUNT` contract.
- Why now: the wrapper hides the exact rejection that causes the blank host.
- Contract/signature changes: render a concrete `<div>` host; call `startApp({el,...})`; no child API contract change.
- Input/output and state mapping: manifest/context -> safe props -> startApp -> lifecycle events; destroy function -> CLEANED.
- Error and edge behavior: disposed Promise result is destroyed immediately; reject reports stable `CHILD_MOUNT_FAILED`; `loadError` reports asset detail without leaking credentials.
- Standards impact: `MC-REUSE-001, MC-PATTERN-001, MC-JSON-001, MC-SCOPE-001`; React Core Facade, no Java model.
- Literal rule enforcement: `Rule 6, Rule 9, Rule 11` — preserve props JSON shape and implement the approved Facade/reducer variation in the existing lifecycle tree.
- Implementation pseudocode:

```tsx
const hostRef = useRef<HTMLDivElement>(null)
const destroyRef = useRef<(() => void) | undefined>()
useEffect(() => {
  let disposed = false
  onState({ type: 'LOAD' })
  void startApp({ name: manifest.key, url: manifest.url, el: hostRef.current!, props: safeProps,
    fiber: false, degrade: import.meta.env.DEV, sync: true,
    beforeMount: () => undefined, afterMount: () => onState({ type: 'MOUNTED', cleanupToken: `${manifest.key}:${manifest.version}` }),
    beforeUnmount: () => onState({ type: 'UNMOUNT' }), afterUnmount: () => onState({ type: 'CLEANED' }),
    loadError: () => onState({ type: 'MOUNT_FAILED', code: 'CHILD_LOAD_ERROR', message: 'Child asset failed to load' }) })
    .then((destroy) => { if (disposed) void destroy?.(); else destroyRef.current = destroy ?? undefined })
    .catch(() => { if (!disposed) onState({ type: 'MOUNT_FAILED', code: 'CHILD_MOUNT_FAILED', message: 'Child mount rejected' }) })
  return () => { disposed = true; void destroyRef.current?.(); destroyRef.current = undefined }
}, [manifest.key, manifest.url, manifest.version, mountKey])
return <ChildErrorBoundary onCrash={...}><div ref={hostRef} data-wujie-host={manifest.key} /></ChildErrorBoundary>
```

- Verification contribution: test observes host, options, event ordering, rejected promise and cleanup.
- After this file: Wujie is still the runtime authority; Portal no longer depends on wrapper-only error logging.

#### File 4 — `MODIFY egon-cola-xingyuan/egon-cola-xingyuan-admin-portal/src/test/setup.ts`

- Purpose: Mock the direct Core module for deterministic lifecycle tests.
- Symbols: `vi.mock('wujie')` start/destroy test doubles.
- Repository evidence: current setup mocks `wujie-react` callback behavior, which no longer tests the chosen Core boundary.
- Dependencies and consumers: Portal Vitest setup and `WujieChild.test.tsx`.
- Why now: tests must fail/settle at the same abstraction used in production.
- Contract/signature changes: test mock exposes `startApp` Promise and destroy function; no production API.
- Input/output and state mapping: options -> events/destroy or reject -> reducer callback.
- Error and edge behavior: tests can choose resolve/reject without touching real browser custom elements.
- Standards impact: `MC-TEST-001, MC-SCOPE-001`; no Java impact.
- Literal rule enforcement: `Rule 5, Rule 11` — use existing Vitest mock path and keep test setup under Portal.
- Implementation pseudocode:

```ts
vi.mock('wujie', () => ({
  startApp: vi.fn(async () => vi.fn()),
  destroyApp: vi.fn(async () => undefined),
}))
// Tests replace the asynchronous Core boundary, then assert Portal events.
// The mock deliberately does not create a browser iframe or custom element.
```

- Verification contribution: isolates mount contract from Wujie internals while retaining exact option names.
- After this file: direct Core import is mockable in Portal tests.

#### File 5 — `CREATE egon-cola-xingyuan/egon-cola-xingyuan-admin-portal/src/lifecycle/WujieChild.test.tsx`

- Purpose: Prove resolved, rejected and disposed Wujie lifecycle paths.
- Symbols: `WujieChild` tests for mount success, mount rejection, unmount cleanup.
- Repository evidence: existing lifecycle reducer and ChildRoutePage tests define status semantics.
- Dependencies and consumers: Testing Library, mocked `wujie`, `ChildManifest` fixture.
- Why now: this is the blocking RED/GREEN proof for the blank Portal host.
- Contract/signature changes: none.
- Input/output and state mapping: manifest/context -> Core options -> event list and host node.
- Error and edge behavior: reject becomes `MOUNT_FAILED`; cleanup after disposed result calls destroy without a second mount.
- Standards impact: `MC-TEST-001, MC-SCOPE-001`; no Java model/Bean/dependency.
- Literal rule enforcement: `Rule 5, Rule 9, Rule 11` — use existing test framework and exercise the Facade state variation.
- Implementation pseudocode:

```tsx
it('reports Core rejection and mounts inside the concrete host', async () => {
  vi.mocked(startApp).mockRejectedValueOnce(new Error('sandbox failed'))
  const onState = vi.fn()
  render(<WujieChild manifest={manifest} context={context} onState={onState} />)
  await waitFor(() => expect(onState).toHaveBeenCalledWith({ type: 'MOUNT_FAILED', code: 'CHILD_MOUNT_FAILED', message: 'Child mount rejected' }))
  expect(screen.getByTestId('wujie-host-tianquan-shoubing')).toBeInTheDocument()
})
```

- Verification contribution: rejects swallowed-wrapper behavior and verifies host existence and error state.
- After this file: Portal Step has a reproducible lifecycle test independent of live child assets.

- Verification command: `cd egon-cola-xingyuan/egon-cola-xingyuan-admin-portal && npm install --package-lock-only --ignore-scripts && npm run typecheck && npm run test -- --runInBand && npm run build`
- Expected result: Wujie tests and Portal build pass; lockfile contains only the direct same-version dependency change.
- Completion criteria: start/reject/cleanup states covered, child props unchanged, no raw iframe fallback introduced.
- Failure returns to: Wujie Core option type, mock event sequence, or Vite bundle error; record exact evidence and keep Step open.
- Rollback: `git revert <Step-3-commit>`.
- Commit paths: `egon-cola-xingyuan/egon-cola-xingyuan-admin-portal/package.json`, `egon-cola-xingyuan/egon-cola-xingyuan-admin-portal/package-lock.json`, `egon-cola-xingyuan/egon-cola-xingyuan-admin-portal/src/lifecycle/WujieChild.tsx`, `egon-cola-xingyuan/egon-cola-xingyuan-admin-portal/src/test/setup.ts`, `egon-cola-xingyuan/egon-cola-xingyuan-admin-portal/src/lifecycle/WujieChild.test.tsx`
- Commit: `fix(portal): expose wujie mount lifecycle failures`

### Step 4 — Repair Tianshu page authorization and nullable JPQL filters

- Requirements: `REQ-005, REQ-008, REQ-009`
- Dependencies: `Step 1`
- Baseline state: Tianshu `/page` paths fall through `anyRequest().denyAll`; empty optional filters produce PostgreSQL `lower(bytea)`.
- Observable outcome: existing Tianshu page Controller paths are readable with TIANSHU_READ and empty filters retain unfiltered query semantics.
- End state: one Tianshu backend commit with no schema/migration change.
- Validation working directory: repository root for the Maven reactor and the Tianshu Admin module for focused tests.
- Test-first gate: `Required — security tests fail for page matchers and repository tests reproduce the nullable parameter query failure before the predicate fix.`
- Manual Checks: `MC-ARCH-001, MC-VALID-001, MC-UTIL-001, MC-JSON-001, MC-CONFIG-001, MC-SCOPE-001, MC-TEST-001`
- Literal Rules: `Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11`
- Ordered files:

#### File 1 — `MODIFY egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/java/top/egon/cola/component/tianshu/admin/security/management/DdcAdminSecurityConfiguration.java`

- Purpose: Add explicit read matchers for every existing Tianshu paged Controller endpoint.
- Symbols: `ddcAdminSecurityFilterChain` GET request matcher list.
- Repository evidence: DdcConfig/Biz/Env/App/Namespace/Binding/Instance/PublishTask controllers expose `/page` methods; current matcher lists only collection paths.
- Dependencies and consumers: Tianshu capability authorities and existing bearer filters.
- Why now: frontend receives 403 before its existing page query can execute.
- Contract/signature changes: only authorization coverage expands for GET page reads; write capability requirements remain unchanged.
- Input/output and state mapping: HTTP method/path + TIANSHU_READ/ALL -> allow or existing deny handler.
- Error and edge behavior: unauthenticated/forbidden requests retain existing 401/403 envelope; no wildcard write path is added.
- Standards impact: `MC-VALID-001, MC-CONFIG-001, MC-SCOPE-001`; existing config class only.
- Literal rule enforcement: `Rule 2, Rule 7, Rule 11` — preserve security layer handoff, no profile key, no new architecture layer.
- Implementation pseudocode:

```java
.requestMatchers(HttpMethod.GET,
        "/api/v1/tianshu/configs/page",
        "/api/v1/tianshu/configs/*/versions/page",
        "/api/v1/tianshu/apps/page",
        "/api/v1/tianshu/bizs/page",
        "/api/v1/tianshu/envs/page",
        "/api/v1/tianshu/namespaces/page",
        "/api/v1/tianshu/namespace-env-app-bindings/page",
        "/api/v1/tianshu/instances/page",
        "/api/v1/tianshu/publish-tasks/page")
    .hasAnyAuthority(TIANSHU_READ, TIANSHU_ALL)
```

- Verification contribution: Spring security test observes authorization for each exact route and rejects write access without WRITE/PUBLISH.
- After this file: all existing page routes have explicit read policy.

#### File 2 — `MODIFY egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/java/top/egon/cola/component/tianshu/admin/repository/DdcBizRepository.java`

- Purpose: Give nullable keyword a text type while preserving blank-as-unfiltered semantics.
- Symbols: `search` JPQL.
- Repository evidence: current `(:keyword is null or lower(...))` produces `lower(bytea)` for null on PostgreSQL.
- Dependencies and consumers: `DdcBizService.page`, DdcBizController `/page`.
- Why now: business-domain page currently fails with empty filter.
- Contract/signature changes: method signature/order unchanged; predicate uses `coalesce(:keyword, '') = ''`.
- Input/output and state mapping: null/blank parameter -> empty string -> predicate true; text -> case-insensitive contains.
- Error and edge behavior: no rows/DB failure retain existing PageResultRecord/error mapping.
- Standards impact: `MC-VALID-001, MC-JSON-001, MC-SCOPE-001`; Repository-only, no model/converter.
- Literal rule enforcement: `Rule 2, Rule 6, Rule 11` — retain layer contract and response model; no new type/layer.
- Implementation pseudocode:

```java
where (coalesce(:keyword, '') = ''
       or lower(biz.bizCode) like lower(concat('%', :keyword, '%'))
       or lower(biz.bizName) like lower(concat('%', :keyword, '%')))
// The parameter remains nullable at the Controller boundary, but JPQL resolves it as text.
// A blank keyword therefore preserves the original unfiltered result set.
```

- Verification contribution: repository page test executes null and non-empty keyword cases.
- After this file: business page query has deterministic text typing.

#### File 3 — `MODIFY egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/java/top/egon/cola/component/tianshu/admin/repository/DdcEnvRepository.java`

- Purpose: Apply string-safe predicates to keyword and scope filters in environment search.
- Symbols: `search` JPQL.
- Repository evidence: `keyword`, `bizCode`, `namespaceCode` are all optional request parameters and current query uses `is null`.
- Dependencies and consumers: `DdcEnvService.page`, Env Controller.
- Why now: environment page is one of the logged `lower(bytea)` failures.
- Contract/signature changes: no method signature change; null remains unfiltered.
- Input/output and state mapping: optional three filters -> coalesced strings -> existing equality/exists predicate.
- Error and edge behavior: missing binding continues to return all matching environments; enabled joins remain unchanged.
- Standards impact: `MC-VALID-001, MC-SCOPE-001`; no new model/Bean/dependency.
- Literal rule enforcement: `Rule 2, Rule 11` — preserve existing service/repository handoff and package tree.
- Implementation pseudocode:

```java
where (coalesce(:keyword, '') = '' or lower(env.envCode) like lower(concat('%', :keyword, '%')) ...)
  and (coalesce(:bizCode, '') = '' or coalesce(:namespaceCode, '') = '' or exists (...))
// Keep the namespace existence predicate and ordering unchanged after typing the parameters.
// The focused test must exercise both no-scope and fully scoped requests.
```

- Verification contribution: focused repository test covers no filters and both scope filters.
- After this file: environment page no longer binds null as bytea.

#### File 4 — `MODIFY egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/java/top/egon/cola/component/tianshu/admin/repository/DdcAppRepository.java`

- Purpose: Type-safe app keyword, business, namespace and environment optional predicates.
- Symbols: `search` JPQL.
- Repository evidence: current app query has four `is null` parameters and logged failure for keyword.
- Dependencies and consumers: `DdcAppService.page`, App Controller and scope selectors.
- Why now: apps page is currently an unknown error in the running frontend.
- Contract/signature changes: method signature unchanged; null/blank filters remain unfiltered.
- Input/output and state mapping: scope filters -> app equality/exists; keyword -> lower text match.
- Error and edge behavior: binding existence semantics and ordering stay unchanged.
- Standards impact: `MC-VALID-001, MC-SCOPE-001`; no external JSON/model change.
- Literal rule enforcement: `Rule 2, Rule 11` — keep current handoff and repository location.
- Implementation pseudocode:

```java
where (coalesce(:bizCode, '') = '' or app.bizCode = :bizCode)
  and (coalesce(:keyword, '') = '' or lower(app.appCode) like lower(concat('%', :keyword, '%')) ...)
  and (coalesce(:namespaceCode, '') = '' or coalesce(:env, '') = '' or exists (...))
```

- Verification contribution: app page test covers empty and scoped query shapes.
- After this file: app page query has text-typed optional parameters.

#### File 5 — `MODIFY egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/java/top/egon/cola/component/tianshu/admin/repository/DdcNamespaceRepository.java`

- Purpose: Type-safe namespace business and keyword filters.
- Symbols: `search` JPQL.
- Repository evidence: namespace page is logged with `lower(namespace_code)` against bytea parameter.
- Dependencies and consumers: `DdcNamespaceService.page`, Namespace Controller.
- Why now: namespace page is currently unavailable with the default filter form.
- Contract/signature changes: unchanged.
- Input/output and state mapping: optional bizCode/keyword -> text-safe filters -> sorted page.
- Error and edge behavior: existing sort and row semantics remain unchanged.
- Standards impact: `MC-VALID-001, MC-SCOPE-001`; Repository-only.
- Literal rule enforcement: `Rule 2, Rule 11` — no new validation/model/layer.
- Implementation pseudocode:

```java
where (coalesce(:bizCode, '') = '' or namespace.bizCode = :bizCode)
  and (coalesce(:keyword, '') = '' or lower(namespace.namespaceCode) like lower(concat('%', :keyword, '%')) ...)
// The two branches keep the original null-as-unfiltered behavior and the existing sort order.
// The repository test covers a null keyword and an explicit business code independently.
```

- Verification contribution: null keyword and explicit bizCode test cases.
- After this file: namespace page returns an empty/all page instead of SQL type failure.

#### File 6 — `MODIFY egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/java/top/egon/cola/component/tianshu/admin/repository/DdcPublishTaskRepository.java`

- Purpose: Make publish task optional scope/status/changeId parameters text-safe.
- Symbols: `search` JPQL.
- Repository evidence: changeId lower predicate uses nullable parameter and publish task page fails in runtime logs.
- Dependencies and consumers: `DdcPublishTaskQueryService`, PublishTask Controller.
- Why now: publish failure recovery page must be usable before retry actions can be trusted.
- Contract/signature changes: no signature/ordering/status change.
- Input/output and state mapping: null scope/status/changeId -> unfiltered; supplied values retain equality/contains semantics.
- Error and edge behavior: task state and retry mutation queries are untouched.
- Standards impact: `MC-VALID-001, MC-JSON-001, MC-SCOPE-001`; no time/model change.
- Literal rule enforcement: `Rule 2, Rule 6, Rule 10, Rule 11` — preserve existing handoff, JSON entity, Java time fields and tree.
- Implementation pseudocode:

```java
where (coalesce(:bizCode, '') = '' or task.bizCode = :bizCode)
  and (coalesce(:env, '') = '' or task.env = :env)
  and (coalesce(:appCode, '') = '' or task.appCode = :appCode)
  and (coalesce(:status, '') = '' or task.status = :status)
  and (coalesce(:changeId, '') = '' or lower(task.changeId) like lower(concat('%', :changeId, '%')))
```

- Verification contribution: empty publish task search test and non-empty changeId test.
- After this file: task page query is safe while retry/update queries remain untouched.

#### File 7 — `MODIFY egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/java/top/egon/cola/component/tianshu/admin/repository/DdcNamespaceEnvAppBindingRepository.java`

- Purpose: Apply the same null-safe predicate discipline to independent binding page reads.
- Symbols: JPQL `search` and `countQuery`.
- Repository evidence: binding page has four nullable `is null` filters and will be the new Tianshu page consumer.
- Dependencies and consumers: `DdcNamespaceEnvAppBindingService.page`, new BindingsPage.
- Why now: adding a page without fixing its query would recreate the same runtime defect.
- Contract/signature changes: count and data predicates remain equivalent; method signature unchanged.
- Input/output and state mapping: optional binding filters -> coalesced text comparisons -> page records/count.
- Error and edge behavior: data/count filters stay identical to prevent pagination mismatch.
- Standards impact: `MC-VALID-001, MC-JSON-001, MC-SCOPE-001`; no DTO/VO change.
- Literal rule enforcement: `Rule 2, Rule 6, Rule 11` — keep Controller/Service/Repository path and response type.
- Implementation pseudocode:

```java
where (coalesce(:bizCode, '') = '' or namespace.bizCode = :bizCode)
  and (coalesce(:namespaceCode, '') = '' or namespace.namespaceCode = :namespaceCode)
  and (coalesce(:env, '') = '' or binding.envCode = :env)
  and (coalesce(:appCode, '') = '' or app.appCode = :appCode)
```

- Verification contribution: data/count query test verifies total count equals filtered records.
- After this file: new binding page has the same safe null behavior as other Tianshu pages.

#### File 8 — `MODIFY egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/test/java/top/egon/cola/component/tianshu/admin/security/management/DdcAdminSecurityIntegrationTest.java`

- Purpose: Prove each new page read matcher allows TIANSHU_READ and still denies a write-only authority.
- Symbols: `protectsPagedManagementReadsWithReadCapability` and paged test controller fixtures.
- Repository evidence: existing security integration test already uses MockMvc, Tianshu capability authorities and fake read-only handlers.
- Dependencies and consumers: DdcAdminSecurityConfiguration and the existing Tianshu controllers.
- Why now: the security change needs a failing path assertion before it can be committed.
- Contract/signature changes: test-only fake GET handlers; no production endpoint.
- Input/output and state mapping: request path + authority -> HTTP status; read -> 200, wrong capability -> 403.
- Error and edge behavior: a missing matcher is reported as 403; handler absence is avoided by the explicit fixture mappings.
- Standards impact: `MC-VALID-001, MC-TEST-001, MC-SCOPE-001`; no Java production type or Bean is added.
- Literal rule enforcement: `Rule 2, Rule 6, Rule 11` — exercise the security handoff, preserve JSON status assertions, and keep tests in the existing module tree.
- Implementation pseudocode:

```java
@Test
void protectsPagedManagementReadsWithReadCapability() throws Exception {
    mockMvc.perform(get("/api/v1/tianshu/configs/page").with(authority("CAP_TIANSHU_READ")))
            .andExpect(status().isOk());
    mockMvc.perform(get("/api/v1/tianshu/apps/page").with(authority("CAP_TIANSHU_WRITE")))
            .andExpect(status().isForbidden());
}
```

- Verification contribution: locks the exact matcher boundary for config/task/metadata pages.
- After this file: the backend Step has a repeatable positive/negative security gate.

- Verification command: `./mvnw -pl egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin -am -DskipTests compile && ./mvnw -pl egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin -Dtest='DdcAdminSecurityIntegrationTest,DdcRepositoryTest,Ddc*ControllerTest' test`
- Expected result: module compiles; page security and repository tests pass; no migration file changes.
- Completion criteria: every listed page path is explicitly readable; all data/count optional predicates use same string-safe semantics; write matchers unchanged.
- Failure returns to: exact Hibernate/SQL or security assertion; do not add a migration or alter service architecture.
- Rollback: `git revert <Step-4-commit>`.
- Commit paths: `egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/java/top/egon/cola/component/tianshu/admin/security/management/DdcAdminSecurityConfiguration.java`, `egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/java/top/egon/cola/component/tianshu/admin/repository/DdcBizRepository.java`, `egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/java/top/egon/cola/component/tianshu/admin/repository/DdcEnvRepository.java`, `egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/java/top/egon/cola/component/tianshu/admin/repository/DdcAppRepository.java`, `egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/java/top/egon/cola/component/tianshu/admin/repository/DdcNamespaceRepository.java`, `egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/java/top/egon/cola/component/tianshu/admin/repository/DdcPublishTaskRepository.java`, `egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/java/top/egon/cola/component/tianshu/admin/repository/DdcNamespaceEnvAppBindingRepository.java`, `egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/test/java/top/egon/cola/component/tianshu/admin/security/management/DdcAdminSecurityIntegrationTest.java`
- Commit: `fix(tianshu-admin): repair paged read authorization and text filters`

### Step 5 — Expose Tianshu namespace/environment/application bindings as a first-class page

- Requirements: `REQ-004, REQ-005, REQ-007, REQ-008`
- Dependencies: `Step 4`
- Baseline state: Namespace drawer can edit bindings but no independent `/bindings` route/menu consumes the binding Controller page contract.
- Observable outcome: Tianshu menu has a metadata child for bindings; page supports filters, pagination, create/edit/delete and explicit error/permission states.
- End state: Tianshu Web binding page is independently usable and drawer remains compatible.
- Validation working directory: `egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin-web`.
- Test-first gate: `Required — test asserts the page path/query and mutation buttons before component/API implementation.`
- Manual Checks: `MC-ARCH-001, MC-REUSE-001, MC-NAME-001, MC-JSON-001, MC-SCOPE-001, MC-TEST-001`
- Literal Rules: `Rule 5, Rule 6, Rule 11`
- Ordered files:

#### File 1 — `MODIFY egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin-web/src/layouts/AdminLayout.tsx`

- Purpose: Add binding leaf beneath the existing Tianshu metadata parent without creating a new top-level menu.
- Symbols: `navigation` metadata children.
- Repository evidence: current Tianshu layout already groups biz/env/app/namespace under `metadata`.
- Dependencies and consumers: shared `EnterpriseLayout`, App route.
- Why now: backend binding function needs an discoverable entry.
- Contract/signature changes: add `/bindings` leaf only.
- Input/output and state mapping: menu item -> React Router path.
- Error and edge behavior: embedded Wujie mode remains Outlet-only; standalone mode gets the new leaf.
- Standards impact: `MC-REUSE-001, MC-SCOPE-001`; no Java impact.
- Literal rule enforcement: `Rule 5, Rule 11` — use existing shared menu and preserve Tianshu tree.
- Implementation pseudocode:

```tsx
{ key: 'bindings', label: '作用域绑定', path: '/bindings', icon: <LinkOutlined /> }
// The item remains below the existing metadata parent and inherits the current auth gate.
// No new top-level navigation entry or permission model is introduced.
```

- Verification contribution: layout test finds binding under 元数据管理.
- After this file: menu organization matches the Controller domain.

#### File 2 — `MODIFY egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin-web/src/App.tsx`

- Purpose: Register the `/bindings` route behind existing `RequireAuth`.
- Symbols: lazy/static `BindingsPage` import and route.
- Repository evidence: all current Tianshu pages are nested under one authenticated route.
- Dependencies and consumers: `BindingsPage`, React Router.
- Why now: menu navigation must resolve to a real page.
- Contract/signature changes: no existing route changes; new path only.
- Input/output and state mapping: `/bindings` -> page component.
- Error and edge behavior: route retains global auth and unknown route behavior.
- Standards impact: `MC-REUSE-001, MC-SCOPE-001`; no Java impact.
- Literal rule enforcement: `Rule 5, Rule 11` — preserve route tree and existing auth boundary.
- Implementation pseudocode:

```tsx
import BindingsPage from './pages/BindingsPage'
<Route path="bindings" element={<BindingsPage />} />
// The route is nested in the authenticated root so standalone and Wujie modes share it.
// Existing registry/configuration routes remain unchanged.
```

- Verification contribution: App route test renders binding page under authenticated wrapper.
- After this file: direct and Portal child route resolution exists.

#### File 3 — `MODIFY egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin-web/src/api/types.ts`

- Purpose: Complete the binding view/request types used by the page.
- Symbols: `DdcNamespaceEnvAppBinding`, `DdcNamespaceEnvAppBindingRequest`.
- Repository evidence: backend VO includes IDs/codes/name/enabled; request record accepts biz/namespace/env/app/enabled.
- Dependencies and consumers: `ddcPageApi`, `ddcApi`, BindingsPage.
- Why now: avoid untyped body/path drift.
- Contract/signature changes: add exact JSON field names; no server change.
- Input/output and state mapping: page row/request form -> Controller JSON.
- Error and edge behavior: optional enabled defaults are kept server-side; page requires codes before mutation.
- Standards impact: `MC-NAME-001, MC-JSON-001, MC-SCOPE-001`; TypeScript view type only.
- Literal rule enforcement: `Rule 1, Rule 6, Rule 11` — use semantic `Request`/view names and preserve JSON contract.
- Implementation pseudocode:

```ts
export type DdcNamespaceEnvAppBindingRequest = {
  bizCode: string; namespaceCode: string; env: string; appCode: string; enabled: boolean
}
```

- Verification contribution: typecheck catches wrong body/row fields.
- After this file: binding API consumers have an explicit contract.

#### File 4 — `CREATE egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin-web/src/pages/BindingsPage.tsx`

- Purpose: Implement independent paged binding management with existing Tianshu client primitives.
- Symbols: `BindingsPage`, filter/form/query/mutations.
- Repository evidence: Tianshu pages use `usePageState`, `ddcPageApi`, `ddcApi`, `PageState`, and Ant Design Table/Modal.
- Dependencies and consumers: binding Controller list/page/create/update/delete, scope option queries.
- Why now: all binding Controller functions need a visible Web consumer.
- Contract/signature changes: UI only; paths are `/api/v1/tianshu/namespace-env-app-bindings` and `/page`.
- Input/output and state mapping: filter -> page query; form -> POST/PUT; row -> DELETE; success invalidates binding query.
- Error and edge behavior: loading/empty/403/5xx/409 states use existing PageState and App message; delete confirms; pagination retains filters.
- Standards impact: `MC-REUSE-001, MC-NAME-001, MC-JSON-001, MC-SCOPE-001`; no new dependency or server model.
- Literal rule enforcement: `Rule 1, Rule 5, Rule 6, Rule 11` — semantic TS types, existing API/client/UI utilities, exact JSON paths, existing page tree.
- Implementation pseudocode:

```tsx
const query = useQuery({ queryKey: ['tianshu', 'bindings', submitted, page.page],
  queryFn: ({ signal }) => ddcPageApi<DdcNamespaceEnvAppBinding>(`/api/v1/tianshu/namespace-env-app-bindings/page?${buildQuery(submitted, page)}`, { signal }) })
const save = useMutation({ mutationFn: ({ id, values }) => id
  ? ddcApi(`/api/v1/tianshu/namespace-env-app-bindings/${id}`, { method: 'PUT', body: values })
  : ddcApi('/api/v1/tianshu/namespace-env-app-bindings', { method: 'POST', body: values }) })
```

- Verification contribution: page test observes GET page, POST/PUT/DELETE and visible loading/error states.
- After this file: Tianshu binding Controller has a standalone management surface.

#### File 5 — `CREATE egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin-web/src/pages/BindingsPage.test.tsx`

- Purpose: Lock route paths, filtering, CRUD and error presentation.
- Symbols: `renders binding rows`, `creates`, `deletes` tests.
- Repository evidence: existing Tianshu page tests use Testing Library and mocked `fetch`/client responses.
- Dependencies and consumers: BindingsPage and Tianshu page test setup.
- Why now: prevents a page that only renders static rows without Controller integration.
- Contract/signature changes: none.
- Input/output and state mapping: mocked envelope/page -> table/forms -> request path/method/body.
- Error and edge behavior: 403/500 shows PageState error, empty page shows explanatory empty state.
- Standards impact: `MC-TEST-001, MC-SCOPE-001`; no Java impact.
- Literal rule enforcement: `Rule 5, Rule 6, Rule 11` — test existing client/envelope and page tree.
- Implementation pseudocode:

```tsx
it('loads paged bindings and sends the Controller CRUD paths', async () => {
  render(<BindingsPage />, { wrapper: renderWithQueryClient })
  await screen.findByText('orders')
  await user.click(screen.getByRole('button', { name: '新增绑定' }))
  await submitBindingForm()
  expect(fetch).toHaveBeenCalledWith(expect.stringContaining('/namespace-env-app-bindings'), expect.objectContaining({ method: 'POST' }))
})
```

- Verification contribution: route/method/body assertions and negative state coverage.
- After this file: Tianshu Web binding Step is independently testable.

- Verification command: `cd egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin-web && npm run typecheck && npm run test -- --runInBand`
- Expected result: binding page tests and existing Tianshu Web tests pass.
- Completion criteria: menu/route/API/page/test are present; Namespace drawer behavior remains unchanged; PageState distinguishes forbidden/error.
- Failure returns to: exact mock path or type mismatch; do not modify Controller contract.
- Rollback: `git revert <Step-5-commit>`.
- Commit paths: `egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin-web/src/layouts/AdminLayout.tsx`, `egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin-web/src/App.tsx`, `egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin-web/src/api/types.ts`, `egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin-web/src/pages/BindingsPage.tsx`, `egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin-web/src/pages/BindingsPage.test.tsx`
- Commit: `feat(tianshu-admin-web): add binding management page`

### Step 6 — Complete Tianquan-Jianshen IAM route/API/action coverage

- Requirements: `REQ-004, REQ-006, REQ-007, REQ-008, REQ-009`
- Dependencies: `Step 1`
- Baseline state: IAM has nested menu infrastructure, but role create/update/inheritance, user membership mutations, and PermissionController have no complete Web surface.
- Observable outcome: all additions remain below the single IAM top-level; role and user actions call exact Controller paths; permissions get a visible route/component.
- End state: Tianquan-Jianshen Web tests/typecheck pass and no internal directory snapshot route is added to browser actions.
- Validation working directory: `egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin-web`.
- Test-first gate: `Required — API/path and route component tests assert the exact missing Controller methods before implementation.`
- Manual Checks: `MC-ARCH-001, MC-REUSE-001, MC-NAME-001, MC-JSON-001, MC-PATTERN-001, MC-SCOPE-001, MC-TEST-001`
- Literal Rules: `Rule 1, Rule 5, Rule 6, Rule 9, Rule 11`
- Ordered files:

#### File 1 — `MODIFY egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin-web/src/features/role/role.api.ts`

- Purpose: Add role create/update/inheritance methods to the existing role API facade.
- Symbols: `create`, `update`, `addInheritance`, `removeInheritance`.
- Repository evidence: RoleController exposes POST/PUT/POST inheritance/DELETE inheritance under `/api/tianquan-jianshen/v1/iam/roles`.
- Dependencies and consumers: RoleGraphPage, FeatureApiClient, optimistic version fields.
- Why now: page has read/impact/resources only.
- Contract/signature changes: exact HTTP paths/body/query; responses use existing RoleMutation/RoleImpact view shape.
- Input/output and state mapping: form + role version -> Controller -> query invalidation.
- Error and edge behavior: 409/version error bubbles to PageState; privileged role stays server-authorized.
- Standards impact: `MC-NAME-001, MC-JSON-001, MC-SCOPE-001`; no new backend type.
- Literal rule enforcement: `Rule 1, Rule 6, Rule 11` — semantic TS command types, exact JSON field names, existing feature tree.
- Implementation pseudocode:

```ts
create: (command: CreateRoleCommand) => client.request<RoleMutationResult>('/api/tianquan-jianshen/v1/iam/roles', { method: 'POST', body: command })
update: (roleId: string, command: UpdateRoleCommand) => client.request<RoleMutationResult>(`/api/tianquan-jianshen/v1/iam/roles/${roleId}`, { method: 'PUT', body: command })
addInheritance: (roleId: string, command: InheritanceCommand) => client.request<RoleImpactView>(`/api/tianquan-jianshen/v1/iam/roles/${roleId}/inheritances`, { method: 'POST', body: command })
removeInheritance: (roleId: string, juniorRoleId: string, query: object) => client.request<RoleImpactView>(`/api/tianquan-jianshen/v1/iam/roles/${roleId}/inheritances/${juniorRoleId}`, { method: 'DELETE', query })
```

- Verification contribution: API unit test asserts exact path, method and expected version fields.
- After this file: RoleGraphPage can consume every external RoleController mutation.

#### File 2 — `MODIFY egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin-web/src/features/role/RoleGraphPage.tsx`

- Purpose: Add guarded role create/edit and inheritance controls to the existing role graph page.
- Symbols: role form, mutations, modal/drawer actions.
- Repository evidence: existing page already loads roles and impact analysis; PermissionGuard is used by adjacent pages.
- Dependencies and consumers: role API, FeatureApiProvider, QueryClient, IAM route.
- Why now: read-only role graph cannot use existing management Controller.
- Contract/signature changes: UI invokes the new API methods; no route change.
- Input/output and state mapping: role form -> POST/PUT; inheritance form -> POST/DELETE; successful mutation invalidates roles/impact.
- Error and edge behavior: disabled on missing permission; mutation error visible; version conflict preserves form values.
- Standards impact: `MC-REUSE-001, MC-NAME-001, MC-JSON-001, MC-PATTERN-001, MC-SCOPE-001`; uses existing React Query state pattern.
- Literal rule enforcement: `Rule 5, Rule 6, Rule 9, Rule 11` — reuse existing guards/query state, exact payload, no new architecture layer, direct UI variation is simple.
- Implementation pseudocode:

```tsx
const saveRole = useMutation({ mutationFn: values => editing
  ? api.update(editing.roleId, { ...values, expectedRoleVersion: editing.version })
  : api.create(values), onSuccess: () => queryClient.invalidateQueries({ queryKey: rolesKey }) })
<PermissionGuard permission="system:role:create"><Button onClick={openCreate}>新建角色</Button></PermissionGuard>
<PermissionGuard permission="system:role:update"><Button onClick={() => openEdit(role)}>编辑</Button></PermissionGuard>
```

- Verification contribution: page tests show guards and assert mutation request payload.
- After this file: role graph is a usable management page, not just an impact report.

#### File 3 — `MODIFY egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin-web/src/features/directory/directory.api.ts`

- Purpose: Expose the existing IAM user list/create/update/delete/status and organization/position assignment paths.
- Symbols: `users`, `createUser`, `updateUser`, `deleteUser`, `changeUserStatus`, organization/position CRUD and assignments.
- Repository evidence: UserController, OrganizationController, PositionController and assignment Controllers expose these external paths.
- Dependencies and consumers: UserDirectoryPage, OrganizationPage, PositionPage and role assignment links.
- Why now: current API only reads one user and directory collections.
- Contract/signature changes: add exact API methods using semantic TypeScript request/view types.
- Input/output and state mapping: query/page params -> DirectoryPage; command/version -> mutation response.
- Error and edge behavior: no `internal/directory-snapshots` method is added to browser actions; 409/version errors bubble.
- Standards impact: `MC-NAME-001, MC-JSON-001, MC-SCOPE-001`; no Java type change.
- Literal rule enforcement: `Rule 1, Rule 6, Rule 11` — use Request/Command/View suffixes and preserve IAM feature tree.
- Implementation pseudocode:

```ts
users: (query, status, page, size) => client.request<DirectoryPage<UserDirectoryView>>('/api/tianquan-jianshen/v1/iam/users', { query: { query, status, page, size } })
createUser: (command) => client.request<UserDirectoryView>('/api/tianquan-jianshen/v1/iam/users', { method: 'POST', body: command })
changeUserStatus: (userId, command) => client.request<UserDirectoryView>(`/api/tianquan-jianshen/v1/iam/users/${userId}/status`, { method: 'PUT', body: command })
```

- Verification contribution: API test verifies `/iam/users` instead of accidentally using `/api/tianquan-jianshen/v1/users` for IAM mutations.
- After this file: user/organization/position Controller coverage is represented in the client facade.

#### File 4 — `MODIFY egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin-web/src/features/directory/UserDirectoryPage.tsx`

- Purpose: Turn the current single-user lookup into a directory list with guarded member mutations while retaining lookup details.
- Symbols: list query, create/edit/status/delete actions, existing detail query.
- Repository evidence: UserController returns `DirectoryPageVO<UserDirectoryVO>` and current page only invokes `directoryApi.user`.
- Dependencies and consumers: directory API, FeatureApiProvider, QueryClient, IAM route.
- Why now: user Controller functions are otherwise unreachable from the Web.
- Contract/signature changes: page query uses `/api/tianquan-jianshen/v1/iam/users`; current detail path remains available.
- Input/output and state mapping: query/status/page -> list; selected member -> form/version mutation; success invalidates list/detail.
- Error and edge behavior: loading/empty/error/denied states remain visible; delete uses expectedAuthVersion and confirmation.
- Standards impact: `MC-REUSE-001, MC-NAME-001, MC-JSON-001, MC-SCOPE-001`; no new state library.
- Literal rule enforcement: `Rule 5, Rule 6, Rule 11` — reuse PageState/React Query, keep JSON fields exact, keep page under directory feature.
- Implementation pseudocode:

```tsx
const users = useQuery({ queryKey: ['tianquan-jianshen', 'users', filters, page], queryFn: () => api.users(filters, page) })
const update = useMutation({ mutationFn: ({ userId, command }) => api.updateUser(userId, command), onSuccess: refreshUsers })
return <Card title="用户目录"><FilterBar /><Table dataSource={users.data?.items ?? []} /><UserEditor /><UserActions /></Card>
```

- Verification contribution: existing single-user test continues to pass; new list/mutation tests assert exact paths.
- After this file: IAM user Controller is represented by visible list/detail/change operations.

#### File 5 — `MODIFY egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin-web/src/app/resourceDefinitions.json`

- Purpose: Make permissions a visible child of the existing IAM resource catalog and retain all IAM parent codes.
- Symbols: `iam.permissions` definition.
- Repository evidence: definition is currently `hidden: true`, while `PermissionController` is external and a component key is missing.
- Dependencies and consumers: `FrontendResourceRegistry`, navigation, `applicationRouteDescriptors`.
- Why now: a component route cannot be reached if resource definition remains hidden.
- Contract/signature changes: set visible catalog parent and component key; no permission code bypass.
- Input/output and state mapping: bootstrap permission -> visible route/menu -> PermissionCatalogPage.
- Error and edge behavior: server authorization remains via existing route guard; hidden/unauthorized resources stay hidden.
- Standards impact: `MC-NAME-001, MC-JSON-001, MC-SCOPE-001`; JSON resource manifest only.
- Literal rule enforcement: `Rule 6, Rule 11` — preserve JSON resource contract and IAM tree.
- Implementation pseudocode:

```json
{ "kind": "ROUTE", "code": "iam.permissions", "name": "权限", "parentCode": "iam.catalog",
  "path": "/iam/permissions", "componentKey": "tianquan-jianshen-permissions", "hidden": false,
  "suggestedPermissionCode": "system:permission:read", "apiResourceCodes": ["iam.api.permissions.list"] }
```

- Verification contribution: navigation test sees 权限 below IAM/资源目录 only when permission exists.
- After this file: registry can bind the new component route without a second top-level menu.

#### File 6 — `MODIFY egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin-web/src/features/governance.routes.tsx`

- Purpose: Bind `tianquan-jianshen-permissions` to the PermissionCatalogPage descriptor.
- Symbols: new permission route descriptor.
- Repository evidence: descriptor lists all current local components; resource JSON already references the component key.
- Dependencies and consumers: application route registry and route guard.
- Why now: resource definition otherwise throws missing local component binding.
- Contract/signature changes: add `/iam/permissions` route descriptor under existing governance route list.
- Input/output and state mapping: descriptor -> component -> permission guard.
- Error and edge behavior: unauthorized users receive existing 403 result; no route for internal APIs.
- Standards impact: `MC-REUSE-001, MC-SCOPE-001`; no Java impact.
- Literal rule enforcement: `Rule 5, Rule 11` — preserve route descriptor architecture and existing client dependencies.
- Implementation pseudocode:

```tsx
import { PermissionCatalogPage } from './permission/PermissionCatalogPage'
{ key: 'permissions', path: '/iam/permissions', title: '权限', permission: 'system:permission:read',
  componentKey: 'tianquan-jianshen-permissions', component: PermissionCatalogPage, navigationOrder: 43 }
```

- Verification contribution: route registry test resolves component key and route guard.
- After this file: `/iam/permissions` is a real IAM child route.

#### File 7 — `CREATE egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin-web/src/features/permission/PermissionCatalogPage.tsx`

- Purpose: Consume PermissionController list/detail/create/status endpoints in a guarded IAM page.
- Symbols: `PermissionCatalogPage`.
- Repository evidence: `applicationApi.permissions/createPermission/changePermissionStatus` already map the Controller paths; page component is absent.
- Dependencies and consumers: FeatureApi, application API, PermissionGuard, PageState, Ant Design.
- Why now: the existing API was orphaned and the resource definition had no component.
- Contract/signature changes: no server changes; use existing client methods.
- Input/output and state mapping: application filter -> permissions list; form -> create; row status -> status mutation; success invalidates query.
- Error and edge behavior: loading/empty/error and 403 use PageState; status changes require expected version.
- Standards impact: `MC-REUSE-001, MC-NAME-001, MC-JSON-001, MC-SCOPE-001`; no new dependency.
- Literal rule enforcement: `Rule 1, Rule 5, Rule 6, Rule 11` — semantic page/type names, existing utilities, exact JSON, existing feature tree.
- Implementation pseudocode:

```tsx
const query = useQuery({ queryKey: ['tianquan-jianshen', 'permissions', tenant, applicationId], queryFn: () => api.permissions(applicationId) })
const create = useMutation({ mutationFn: values => api.createPermission(values), onSuccess: refresh })
const status = useMutation({ mutationFn: row => api.changePermissionStatus(row.id, row.status === 'ACTIVE' ? 'DISABLED' : 'ACTIVE', row.version), onSuccess: refresh })
return <Card title="权限"><ApplicationSelect /><PermissionTable /><PermissionEditor /></Card>
```

- Verification contribution: page test asserts list/create/status calls and resource text.
- After this file: PermissionController is reachable through visible IAM UI.

#### File 8 — `CREATE egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin-web/src/features/permission/PermissionCatalogPage.test.tsx`

- Purpose: Lock permission route and existing API path consumption.
- Symbols: list/create/status tests.
- Repository evidence: Tianquan-Jianshen feature tests use mocked Rbac3Provider/FeatureApiProvider and Testing Library.
- Dependencies and consumers: PermissionCatalogPage and applicationApi.
- Why now: catches hidden route/component mismatch and wrong `/iam` path.
- Contract/signature changes: none.
- Input/output and state mapping: API fixtures -> table/form -> exact FeatureApi calls.
- Error and edge behavior: unauthorized permission hides mutation controls; query error remains visible.
- Standards impact: `MC-TEST-001, MC-SCOPE-001`; no Java impact.
- Literal rule enforcement: `Rule 5, Rule 6, Rule 11` — existing test utilities and JSON contract.
- Implementation pseudocode:

```tsx
it('renders permissions and calls the IAM permission controller paths', async () => {
  render(<PermissionCatalogPage />, { wrapper })
  await screen.findByText('orders.read')
  expect(request).toHaveBeenCalledWith('/api/tianquan-jianshen/v1/iam/permissions', expect.anything())
})
```

- Verification contribution: component binding and API path proof.
- After this file: Tianquan-Jianshen Step has a focused regression suite.

- Verification command: `cd egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin-web && npm run typecheck && npm run test -- --runInBand`
- Expected result: Tianquan-Jianshen existing and added tests pass; IAM navigation remains one top-level.
- Completion criteria: role/user/permission additions compile; no browser call uses internal snapshot path; mutation controls are permission guarded.
- Failure returns to: exact response shape, route registry or test path; do not modify Tianquan-Jianshen Java Controller for a frontend mismatch.
- Rollback: `git revert <Step-6-commit>`.
- Commit paths: `egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin-web/src/features/role/role.api.ts`, `egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin-web/src/features/role/RoleGraphPage.tsx`, `egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin-web/src/features/role/RolePages.test.tsx`, `egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin-web/src/features/directory/directory.api.ts`, `egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin-web/src/features/directory/UserDirectoryPage.tsx`, `egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin-web/src/features/directory/DirectoryPages.test.tsx`, `egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin-web/src/app/resourceDefinitions.json`, `egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin-web/src/features/governance.routes.tsx`, `egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin-web/src/features/permission/PermissionCatalogPage.tsx`, `egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin-web/src/features/permission/PermissionCatalogPage.test.tsx`
- Commit: `feat(tianquan-jianshen-admin-web): complete IAM controller coverage`

### Step 7 — Add dedicated Yuheng OpenAPI sync and application/trace details

- Requirements: `REQ-004, REQ-007, REQ-008, REQ-009, REQ-010`
- Dependencies: `Step 1`
- Baseline state: Yuheng API facade already has application detail/OpenAPI/document/trace detail methods or adjacent data, but not every method has a dedicated navigable page/action.
- Observable outcome: Yuheng governance menu exposes OpenAPI synchronization; application and trace pages show detail without introducing new server calls outside existing Admin Controller paths.
- End state: Yuheng Web tests/typecheck pass and parent menu remains path-safe.
- Validation working directory: `egon-cola-xingyuan/egon-cola-yuheng/yuheng-admin-web`.
- Test-first gate: `Required — API/page tests assert existing Controller paths and fail before the dedicated page/action is wired.`
- Manual Checks: `MC-ARCH-001, MC-REUSE-001, MC-NAME-001, MC-JSON-001, MC-SCOPE-001, MC-TEST-001`
- Literal Rules: `Rule 1, Rule 5, Rule 6, Rule 11`
- Ordered files:

#### File 1 — `MODIFY egon-cola-xingyuan/egon-cola-yuheng/yuheng-admin-web/src/api/gatewayApi.ts`

- Purpose: Add an explicit application detail method if the current facade only lists applications, and keep existing OpenAPI/trace methods as the source of truth.
- Symbols: `application` detail method and existing `openapiSyncStates`, `operationOpenApi`, `openapiSnapshotDocument`, `traceDetail`.
- Repository evidence: GatewayApplicationController exposes GET `applications/{id}`; current page has selected list row but no detail fetch.
- Dependencies and consumers: ApplicationsPage/OpenApiSyncPage/TracesPage.
- Why now: application detail is an existing external Controller function not fully consumed.
- Contract/signature changes: GET `/api/v1/yuheng/admin/applications/{id}` only; no backend change.
- Input/output and state mapping: id -> Application detail; errors preserve GatewayApiError.
- Error and edge behavior: 404 shows not found, stale OpenAPI state remains visible with retry.
- Standards impact: `MC-NAME-001, MC-JSON-001, MC-SCOPE-001`; existing TypeScript API facade.
- Literal rule enforcement: `Rule 1, Rule 6, Rule 11` — semantic method/type use, exact JSON, existing yuheng API tree.
- Implementation pseudocode:

```ts
application: (applicationId: string, signal?: AbortSignal) => apiRequest<Application>(
  `${admin}/applications/${encodeURIComponent(applicationId)}`, { signal }),
// The method is read-only and follows the GatewayApplicationController path exactly.
// AbortSignal keeps closing a detail drawer from retaining a stale request.
```

- Verification contribution: yuheng API test asserts exact detail path.
- After this file: application detail Controller has a named client method.

#### File 2 — `MODIFY egon-cola-xingyuan/egon-cola-yuheng/yuheng-admin-web/src/features/applications/ApplicationsPage.tsx`

- Purpose: Add an application detail drawer using the new method and preserve credential/OpenAPI panels.
- Symbols: selected application detail query/drawer.
- Repository evidence: page already has application selection for credentials and OpenAPI expandable rows.
- Dependencies and consumers: gatewayApi application detail, QueryState, Drawer/Descriptions.
- Why now: list rows currently cannot consume GatewayApplicationController GET detail.
- Contract/signature changes: UI-only detail fetch.
- Input/output and state mapping: row click -> detail query; detail -> Drawer; error -> retry.
- Error and edge behavior: closing drawer stops query; 404/5xx uses existing QueryFailure.
- Standards impact: `MC-REUSE-001, MC-SCOPE-001`; no new state library.
- Literal rule enforcement: `Rule 5, Rule 6, Rule 11` — reuse existing query/UI/client and yuheng tree.
- Implementation pseudocode:

```tsx
const [selectedId, setSelectedId] = useState<string>()
const detail = useQuery({ queryKey: ['application-detail', selectedId], queryFn: ({ signal }) => gatewayApi.application(selectedId!, signal), enabled: Boolean(selectedId) })
<Button onClick={() => setSelectedId(row.id)}>详情</Button>
<Drawer open={Boolean(selectedId)} onClose={() => setSelectedId(undefined)}>{detail.isLoading ? <LoadingBlock /> : <Descriptions ... />}</Drawer>
```

- Verification contribution: page test asserts detail button and request path.
- After this file: Application page consumes list/detail/credential/OpenAPI actions.

#### File 3 — `CREATE egon-cola-yuheng/yuheng-admin-web/src/features/openapi/OpenApiSyncPage.tsx`

- Purpose: Provide a dedicated OpenAPI synchronization status/document page.
- Symbols: `OpenApiSyncPage`.
- Repository evidence: `GatewayOpenApiController` exposes sync states and snapshot document; gatewayApi already implements those methods.
- Dependencies and consumers: GatewayScopeFilter, gatewayApi, QueryState, `GatewayOpenApiSyncState`.
- Why now: sync state is currently only an expandable application table detail, with no dedicated operational view.
- Contract/signature changes: UI route only; existing GET paths.
- Input/output and state mapping: scope -> sync-state table; snapshot id -> document Drawer/JSONPanel.
- Error and edge behavior: stale/error tags remain visible; document 404 displays a truthful not-available state, not empty success.
- Standards impact: `MC-REUSE-001, MC-NAME-001, MC-JSON-001, MC-SCOPE-001`; no new dependency.
- Literal rule enforcement: `Rule 1, Rule 5, Rule 6, Rule 11` — semantic page name, existing UI/client, exact document JSON, existing feature tree.
- Implementation pseudocode:

```tsx
const states = useQuery({ queryKey: ['openapi-sync', filters], queryFn: ({ signal }) => gatewayApi.openapiSyncStates(filters, signal) })
const document = useQuery({ queryKey: ['openapi-document', snapshotId], queryFn: ({ signal }) => gatewayApi.openapiSnapshotDocument(snapshotId!, signal), enabled: Boolean(snapshotId) })
return <section><Typography.Title level={2}>OpenAPI 同步</Typography.Title><GatewayScopeFilter ... /><Table dataSource={states.data ?? []} onRow={row => ({ onClick: () => setSnapshotId(row.snapshotId ?? undefined) })} /></section>
```

- Verification contribution: status/document requests and error state tests.
- After this file: OpenAPI Controller has an explicit operational entry.

#### File 4 — `MODIFY egon-cola-xingyuan/egon-cola-yuheng/yuheng-admin-web/src/layouts/AdminLayout.tsx`

- Purpose: Add OpenAPI sync beneath existing Yuheng governance menu.
- Symbols: navigation child item.
- Repository evidence: Yuheng governance already groups groups/applications/interface catalog/providers.
- Dependencies and consumers: shared sidebar and App route.
- Why now: dedicated page needs a discoverable route.
- Contract/signature changes: add leaf `/openapi-sync` under `yuheng-governance`.
- Input/output and state mapping: menu item -> route.
- Error and edge behavior: capability remains `yuheng:read`; no new permission bypass.
- Standards impact: `MC-REUSE-001, MC-SCOPE-001`; no Java impact.
- Literal rule enforcement: `Rule 5, Rule 11` — preserve Yuheng navigation structure and shared boundary.
- Implementation pseudocode:

```tsx
{ key: '/openapi-sync', path: '/openapi-sync', icon: <ApiOutlined />, label: 'OpenAPI 同步', capability: 'yuheng:read' }
// The capability matches the existing Yuheng governance read permission.
// Mutations remain on the existing draft/release pages.
```

- Verification contribution: AdminLayout test confirms the child is in Yuheng governance.
- After this file: page is reachable both standalone and embedded.

#### File 5 — `MODIFY egon-cola-xingyuan/egon-cola-yuheng/yuheng-admin-web/src/app/App.tsx`

- Purpose: Register the OpenAPI sync page in the existing authenticated router.
- Symbols: lazy import/route.
- Repository evidence: App uses lazy page imports and `RequireCapability` around Yuheng routes.
- Dependencies and consumers: OpenApiSyncPage, Yuheng AdminLayout.
- Why now: menu link requires a route component.
- Contract/signature changes: route `/openapi-sync` only.
- Input/output and state mapping: route -> page; `yuheng:read` guard.
- Error and edge behavior: unknown routes continue to use 404 result.
- Standards impact: `MC-REUSE-001, MC-SCOPE-001`; no Java impact.
- Literal rule enforcement: `Rule 5, Rule 11` — reuse existing router/capability tree.
- Implementation pseudocode:

```tsx
const OpenApiSyncPage = lazy(() => import('../features/openapi/OpenApiSyncPage').then(module => ({ default: module.OpenApiSyncPage })))
{ path: 'openapi-sync', element: <RequireCapability capability="yuheng:read"><OpenApiSyncPage /></RequireCapability> }
// Lazy loading preserves the existing Yuheng bundle boundary and fallback.
// No API request is made from the router itself.
```

- Verification contribution: route integration test renders page under Yuheng read capability.
- After this file: Yuheng page route is wired.

#### File 6 — `CREATE egon-cola-yuheng/yuheng-admin-web/src/features/openapi/OpenApiSyncPage.test.tsx`

- Purpose: Verify sync state/document UI and exact existing client calls.
- Symbols: page test fixtures for valid/stale/error state.
- Repository evidence: Yuheng feature tests use mocked `gatewayApi` and Testing Library.
- Dependencies and consumers: OpenApiSyncPage, GatewayScopeFilter, API facade.
- Why now: prevents a visual-only page with wrong path or response assumptions.
- Contract/signature changes: none.
- Input/output and state mapping: sync states -> table/tag; snapshot click -> document request.
- Error and edge behavior: stale and failed states are visible; request failure offers retry.
- Standards impact: `MC-TEST-001, MC-SCOPE-001`; no Java impact.
- Literal rule enforcement: `Rule 5, Rule 6, Rule 11` — existing test/client/UI architecture and JSON data.
- Implementation pseudocode:

```tsx
it('shows sync state and requests the snapshot document through existing APIs', async () => {
  render(<OpenApiSyncPage />)
  await screen.findByText('VALID')
  await user.click(screen.getByText('snapshot-1'))
  expect(gatewayApi.openapiSnapshotDocument).toHaveBeenCalledWith('snapshot-1', expect.anything())
})
```

- Verification contribution: dedicated page API and failure state proof.
- After this file: Yuheng Step has focused route/page regression.

- Verification command: `cd egon-cola-xingyuan/egon-cola-yuheng/yuheng-admin-web && npm run typecheck && npm run test -- --runInBand`
- Expected result: Yuheng existing and added tests pass.
- Completion criteria: application detail, OpenAPI sync and existing trace detail are visibly reachable; no new direct Tianshu/Engine/Kafka request.
- Failure returns to: exact API fixture/path or route guard; do not alter Yuheng Java contracts.
- Rollback: `git revert <Step-7-commit>`.
- Commit paths: `egon-cola-xingyuan/egon-cola-yuheng/yuheng-admin-web/src/api/gatewayApi.ts`, `egon-cola-xingyuan/egon-cola-yuheng/yuheng-admin-web/src/api/gatewayApi.test.ts`, `egon-cola-xingyuan/egon-cola-yuheng/yuheng-admin-web/src/features/applications/ApplicationsPage.tsx`, `egon-cola-xingyuan/egon-cola-yuheng/yuheng-admin-web/src/features/applications/ApplicationsPage.test.tsx`, `egon-cola-xingyuan/egon-cola-yuheng/yuheng-admin-web/src/features/openapi/OpenApiSyncPage.tsx`, `egon-cola-xingyuan/egon-cola-yuheng/yuheng-admin-web/src/features/openapi/OpenApiSyncPage.test.tsx`, `egon-cola-xingyuan/egon-cola-yuheng/yuheng-admin-web/src/layouts/AdminLayout.tsx`, `egon-cola-xingyuan/egon-cola-yuheng/yuheng-admin-web/src/app/App.tsx`
- Commit: `feat(yuheng-admin-web): expose openapi synchronization details`

### Step 8 — Audit Controller coverage, build all Webs, and close the delivery boundary

- Requirements: `REQ-004, REQ-008, REQ-009, REQ-010`
- Dependencies: `Steps 1-7`
- Baseline state: route coverage is distributed across four Web trees and active Yuheng release can lag behind source draft.
- Observable outcome: static audit separates external Controller paths covered by client/page, paths intentionally protocol/internal, and active release drift; all changed Web modules typecheck/build/test.
- End state: final commit contains the read-only audit script and final Spec/Plan evidence; runtime process start/restart remains outside this plan.
- Validation working directory: repository root for the coverage script, then each affected Web/Java module for final gates.
- Test-first gate: `Required — audit script initially reports at least the known uncovered/boundary categories and final command must show the corrected classification.`
- Manual Checks: `MC-ARCH-001, MC-REUSE-001, MC-DEP-001, MC-NAME-001, MC-VALID-001, MC-MODEL-001, MC-CONVERT-001, MC-LOG-001, MC-BEAN-001, MC-UTIL-001, MC-JSON-001, MC-TIME-001, MC-CONFIG-001, MC-PATTERN-001, MC-SCOPE-001, MC-TEST-001, MC-BLOCKER-001`
- Literal Rules: `Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11`
- Ordered files:

#### File 1 — `CREATE scripts/unified-xingyuan/check-admin-web-controller-coverage.sh`

- Purpose: Provide a read-only, repeatable audit for external Controller route families and frontend consumers.
- Symbols: route extraction, frontend path extraction, classified report.
- Repository evidence: four Controller trees and four Web trees are fixed under `egon-cola-xingyuan`.
- Dependencies and consumers: Bash, `rg`, Java annotation strings, TypeScript source.
- Why now: distinguishes code absent, UI unconsumed and active release not published.
- Contract/signature changes: developer audit output only; no runtime API.
- Input/output and state mapping: source annotations/path strings -> covered/uncovered/internal classification -> stdout.
- Error and edge behavior: missing module exits non-zero; known protocol/internal patterns are labeled rather than counted as Admin page gaps.
- Standards impact: `MC-ARCH-001, MC-REUSE-001, MC-DEP-001, MC-NAME-001, MC-VALID-001, MC-MODEL-001, MC-CONVERT-001, MC-LOG-001, MC-BEAN-001, MC-UTIL-001, MC-JSON-001, MC-TIME-001, MC-CONFIG-001, MC-PATTERN-001, MC-SCOPE-001, MC-TEST-001, MC-BLOCKER-001`; script adds no Java type/dependency/config.
- Literal rule enforcement: `Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11` — final audit checks names, layer tree, dependencies, profiles, pattern/design scope and no new Java model.
- Implementation pseudocode:

```bash
set -euo pipefail
root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
for prefix in '/api/v1/tianquan-shoubing' '/api/tianquan-jianshen/v1' '/api/v1/yuheng/admin' '/api/v1/tianshu'; do
  rg -n "@(Get|Post|Put|Patch|Delete)Mapping|@RequestMapping" "${root}/egon-cola-xingyuan" | rg "${prefix}|internal" || true
done
printf '%s\n' 'Classification: external admin paths are compared to Web API strings; internal/protocol paths are excluded; release drift is runtime-only.'
```

- Verification contribution: audit output is attached to final review and does not mutate source/runtime.
- After this file: coverage claims have a repeatable evidence command.

#### File 2 — `MODIFY docs/egon/spec/2026-08-28-11-06-xingyuan-web-runtime-repair-and-controller-coverage.md`

- Purpose: Record final validation evidence, exact commits and any remaining runtime release boundary after implementation.
- Symbols: §14, §16, §18, §20 evidence updates.
- Repository evidence: implementation commits and command output from Steps 1-7.
- Dependencies and consumers: final Spec audit and user review.
- Why now: the accepted design must not claim runtime closure without evidence.
- Contract/signature changes: documentation evidence only.
- Input/output and state mapping: command results/runtime probes -> PASS/PARTIAL boundary.
- Error and edge behavior: active release drift or unavailable live child remains explicitly reported.
- Standards impact: `MC-BLOCKER-001, MC-SCOPE-001, MC-TEST-001`; documentation only.
- Literal rule enforcement: `Rule 11` — final tree and evidence must match the selected profile and Step scopes.
- Implementation pseudocode:

```text
collect each Step commit and command exit code
record static build/test results separately from live browser/process results
write only evidence-backed completion or residual boundary statements
```

- Verification contribution: final Spec/Plan audit and user handoff.
- After this file: documentation reflects actual implementation boundary, not intended behavior.

- Verification command: `bash -n scripts/unified-xingyuan/check-admin-web-controller-coverage.sh && scripts/unified-xingyuan/check-admin-web-controller-coverage.sh && python3 .agents/skills/egon-coding-writing-spec/scripts/validate_spec.py docs/egon/spec/2026-08-28-11-06-xingyuan-web-runtime-repair-and-controller-coverage.md && python3 .agents/skills/egon-coding-writing-plan/scripts/validate_plan.py docs/egon/plan/2026-08-28-11-06-xingyuan-web-runtime-repair-and-controller-coverage.md`
- Expected result: audit scripts and document validators exit 0; four Web typecheck/build/test and Tianshu focused tests are recorded separately.
- Completion criteria: all requirements have step/test traceability; all applicable Literal Rules and Manual Checks are re-run; no unrelated file staged; runtime claims match evidence.
- Failure returns to: exact audit/validator/build failure; keep final Step open and report blocker.
- Rollback: `git revert <Step-8-commit>` for audit/docs only; previous feature commits remain independently reversible.
- Commit paths: `scripts/unified-xingyuan/check-admin-web-controller-coverage.sh`, `docs/egon/spec/2026-08-28-11-06-xingyuan-web-runtime-repair-and-controller-coverage.md`, `docs/egon/plan/2026-08-28-11-06-xingyuan-web-runtime-repair-and-controller-coverage.md`
- Commit: `test(xingyuan): add controller coverage audit`

## 8. Test, Validation, and Quality Gates

| Gate | Command | Required result | If failed |
| --- | --- | --- | --- |
| Spec | `python3 .../validate_spec.py <spec>` | PASS | fix document before code |
| Plan | `python3 .../validate_plan.py <plan>` | PASS | fix Plan markers/coverage |
| Shared | shared typecheck/test/build | 0 | Step 1 stays open |
| Portal | Portal typecheck/test/build | 0; Wujie lifecycle test green | Step 3 stays open |
| Tianshu Java | focused Maven compile/test | 0; no migration diff | Step 4 stays open |
| Tianshu Web | typecheck/test/build | 0; binding route green | Step 5 stays open |
| Tianquan-Jianshen | typecheck/test/build | 0; IAM route/API tests green | Step 6 stays open |
| Yuheng | typecheck/test/build | 0; OpenAPI/detail tests green | Step 7 stays open |
| Coverage | shell audit | separated classification | Step 8 stays open |
| Worktree | `git diff --check`, path-limited status | no whitespace/unrelated staged paths | do not commit |

## 9. Migration, Compatibility, Rollout, and Rollback

No Flyway file is added or modified. Rollout is source/build based: shared first, then scripts/Portal, Tianshu backend/web, Tianquan-Jianshen/Yuheng web. Existing runtime processes are not automatically restarted. After user-led restart or HMR, verify Portal child host, Yuheng dashboard, Tianshu pages, and direct standalone entries. If Wujie Core cannot bundle under the local Vite version, revert Step 3 and retain the failure-visible boundary for a new approved correction; do not silently use raw iframe.

## 10. Requirement-to-Step Traceability Matrix

| Requirement | Steps | Primary files | Tests/gates |
| --- | --- | --- | --- |
| REQ-001 | Step 3 | WujieChild/package/test | Portal lifecycle/build |
| REQ-002 | Step 2 | start scripts/address test | shell policy |
| REQ-003 | Step 1 | Sidebar/shared test | shared build |
| REQ-004 | Steps 5-7 | Tianshu binding/Tianquan-Jianshen permission/role/Yuheng OpenAPI | Web focused tests |
| REQ-005 | Step 4 | Tianshu Security + repositories | Maven security/JPA |
| REQ-006 | Step 5-7 | IAM resource definitions, Tianshu/Yuheng nested navigation | navigation tests |
| REQ-007 | Steps 3,5-7 | pages and state components | page tests |
| REQ-008 | Every Step | path-limited commits | all gates |
| REQ-009 | Steps 3-8 | API facades/audit | `rg internal` + audit |
| REQ-010 | Step 8 | coverage audit/spec evidence | audit/runtime boundary |

## 11. Risks, Blockers, and User Decisions

| Item | Evidence | Handling | Status |
| --- | --- | --- | --- |
| Wujie dev Vite module behavior | child HTML includes Vite HMR scripts | Core test first; runtime proof remains separate | Controlled |
| active Yuheng release drift | local stack uses skip-release mode and active revision can be older | do not publish automatically; report separately | Controlled |
| PostgreSQL coalesce inference | current `lower(bytea)` log | focused JPA test is blocking | Controlled |
| Tianquan-Shoubing external management coverage | current Tianquan-Shoubing pages consume main admin Controller routes | no duplicate page; final audit verifies | Resolved |
| user architecture decision | user explicitly authorized direct repair | execute this Plan | Resolved |

## 12. Review and Acceptance

### 12.1 Review scope

Review source diffs, each Step commit, targeted tests, document validators, and final coverage output. Check that the unrelated untracked user document was not staged.

### 12.2 Acceptance conditions

Accept only when applicable gates pass, Portal Wujie and direct children are separately verified, Tianshu page/query defects are fixed, and any active release/runtime limitation is clearly labeled.

### 12.3 Implementation authorization

User authorization is explicit in the latest request “直接开始修复”; no additional approval is needed for the listed Step scopes.

### 12.4 Evidence boundary

Typecheck/build/unit tests are static/component evidence. Existing process/browser checks, if run after code changes, are runtime evidence; a green static gate never substitutes for live Wujie mount or Yuheng release publication.

### 12.5 Blocking Manual Check

| Check ID | Applicability | Status | Evidence | Finding | Required action/exception |
| --- | --- | --- | --- | --- | --- |
| MC-ARCH-001 | Applicable | PASS | §4.7 and each Step preserve existing module boundaries | no new cross-domain backend layer | None |
| MC-REUSE-001 | Applicable | PASS | §4.5/ledger selects shared/PageState/Wujie Core reuse | additions have concrete evidence | None |
| MC-DEP-001 | Applicable | PASS | Wujie 2.1.0 already resolved in Portal lockfile | direct dependency remains same version | None |
| MC-NAME-001 | Applicable | PASS | semantic TS page/API names and existing Java types | no ambiguous new carrier | None |
| MC-VALID-001 | Applicable | PASS | Step 4 maps security and repository boundary tests | no controller-only proof | None |
| MC-MODEL-001 | Not applicable | N/A | no Java Entity/DTO/VO/Converter file is changed | model contract remains unchanged | None |
| MC-CONVERT-001 | Not applicable | N/A | no converter or mapping implementation is changed | conversion scope absent | None |
| MC-LOG-001 | Not applicable | N/A | no Java business log class is changed | logging scope absent | None |
| MC-BEAN-001 | Not applicable | N/A | no Spring Bean/injection class is added | wiring scope absent | None |
| MC-UTIL-001 | Applicable | PASS | only existing Spring/JPA/Bash/UI utilities are used | no duplicate utility dependency | None |
| MC-JSON-001 | Applicable | PASS | Tianshu/Yuheng/Tianquan-Jianshen pages keep existing JSON envelopes | no invented response wrapper | None |
| MC-TIME-001 | Not applicable | N/A | no time model/import is changed | time scope absent | None |
| MC-CONFIG-001 | Applicable | PASS | address override/default is tested; no Spring profile key added | config structure preserved | None |
| MC-PATTERN-001 | Applicable | PASS | Wujie Facade and lifecycle reducer are selected for real variation | no ceremonial pattern elsewhere | None |
| MC-SCOPE-001 | Applicable | PASS | every Step lists exact paths and exclusions | unrelated user file excluded | None |
| MC-TEST-001 | Applicable | PASS | Steps define RED/GREEN commands and final audit | validation is executable | None |
| MC-BLOCKER-001 | Applicable | PASS | no unresolved architecture/user decision remains | runtime limitations are evidence boundaries | None |

PASS — Ready for user review
