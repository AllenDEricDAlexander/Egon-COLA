# 授柄 OAuth Client Grant 查询接口修订

| Field | Value |
| --- | --- |
| Document | `2026-09-23-10-57-oauth-grant-list-contract-amendment.md` |
| Template Version | `7` |
| Status | `Review` |
| Type | `Bugfix / API amendment` |
| Complexity | `Simple` |
| Complexity Drivers | `None`：一个只读方法，沿用主Spec已有realm守卫、客户端权限、MP表与VO，没有新的状态或数据迁移 |
| Created | `2026-09-23 10:57 Asia/Shanghai` |
| Updated | `2026-09-23 10:59 Asia/Shanghai` |
| Owner | `mario` |
| Repository | `Egon-COLA` |
| Scope | 授柄ClientResourceGrantController、ResourceServerService、OAuthGrantRepository/Mapper、现有Grant管理页 |
| Change Surface | 增加客户端Grant列表查询GET；复用已定义realm/tenant过滤与现有ClientResourceGrantVO；前端查询携带已选realm |
| Affected Chapters | §7, §8, §9, §12, §14, §16, §18 |
| Source Requirement | 用户2026-09-22多租户最小scope要求与[主Spec](2026-09-22-17-14-tianquan-multitenant-oauth-oidc-design.md) REQ-005；当前Grant页面既有请求有消费者但后端无路由 |
| Baseline Revision | `main@ff4430a3f849cfe58cc45a82ffaf480d427c2162`；只读检查与本Plan并发的脚本/CI工作树不属于此修订 |
| Amends | [已确认主Spec](2026-09-22-17-14-tianquan-multitenant-oauth-oidc-design.md) §9.1、§9.2、§9.3、§9.4、§12、§14、§19、§20：补一条原子Grant列表查询及前端消费/测试；其他契约保持 |
| Supersedes | None |
| Depends On | [已确认主Spec](2026-09-22-17-14-tianquan-multitenant-oauth-oidc-design.md) §7、§8.4、§9.0、§10、§11、§15、§16的realm、存储、权限、错误与切流规则 |
| Related Specs | None |
| Related Plans | [天权 OAuth/OIDC 实施 Plan 草稿](../plan/2026-09-23-10-42-tianquan-oauth-oidc-implementation.md)；本修订获确认前该Plan不得进入Ready或实现 |

## 1. Summary

现有授柄 Grant 管理页在打开客户端详情时调用 `GET /api/v1/tianquan-shoubing/clients/{clientId}/resources`，以展示和编辑当前资源授权。当前同前缀Controller只有PUT、DELETE、批量POST，GET缺失；主Spec也漏列这个只读合同。结果是页面拿不到受控授权集合，无法核对本轮要求的最小scope配置。

本修订只补该Query，仍使用已确认的issuer/realm、客户端和资源授权数据、`ClientResourceGrantVO`、既有权限码；不加参数预取、额外表、缓存、事件或新角色。状态是Review，待用户审核后作为主Spec的增量生效。

## 2. Background and Current State

| Evidence ID | Classification | Exact path/symbol | Observed fact | Design significance | Verification limit |
| --- | --- | --- | --- | --- | --- |
| EVD-001 | Static repository | `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin-web/src/features/resource-grants/ClientResourceGrantPage.tsx#grantQuery` | 请求该GET并将数组/页面结果交给normalizePage、按resourceServerId关联行 | 页面有独立展示/选择/批量编辑用途 | 只读静态代码，未开浏览器 |
| EVD-002 | Static repository | `.../admin/resource/controller/ClientResourceGrantController.java` | 现有路由为PUT/DELETE/批量POST，无GET列表 | 缺少目标API-045 | 未启动服务 |
| EVD-003 | Static repository | `.../admin/resource/domain/vo/ClientResourceGrantVO.java` 与前端 `src/api/types.ts` | 有clientId/resourceServerId/grantType/tenantId/allowedScopes/status/version | 可复用相同展示形状，不新建VO | 对序列化仍需合同测试 |
| EVD-004 | Accepted Spec | [主Spec](2026-09-22-17-14-tianquan-multitenant-oauth-oidc-design.md) §9.0、§11.2 OAuthGrant、§12 | 管理`realm`缺省当前已验tenant，显式platform须强化授权；oauth_grant已设计tenant+client索引/具名查询 | 新GET使用已有存储和守卫 | 主Spec的生产实现尚不存在 |

当前链：ClientResourceGrantPage `grantQuery` → 404 → `grantReadUnavailable`；现有增删可执行但完成后invalidate无法取得刚提交的授权集。目标链：页面以已选realm GET → Controller权限/realm校验 → Service读同realm Client → OAuthGrantRepository具名查询 → `ClientResourceGrantVO[]`；无写入及业务事件。HTTP、DAO、UI测试只读证据不证明生产数据库性能。

## 3. Goals and Non-goals

目标：同realm管理员按现有页面选择的客户端查看完整、有界、稳定排序的资源Grant列表，能验证最小scope和状态。非目标：改变PUT/DELETE/批量POST、引入新的client选择页、授权Client、扩大任何scope、改变鉴神角色或增加DDL/飞迁历史。

### 3.3 Change Surface and Design Depth

| Area/layer | Disposition | Exact repository evidence | Changed or preserved behavior/contract | Required Spec treatment | Chapter(s) |
| --- | --- | --- | --- | --- | --- |
| Grant列表HTTP/Service/Repository/Mapper与前端Query | Affected | EVD-001/002/004 | 新GET和该读取链，按realm过滤、序列化、UI状态 | 精确单接口与路径、排序/错误/测试 | §7, §8, §9, §12, §14, §16, §18 |
| `oauth_grant`结构/index | Unchanged | 主Spec §11.2 `oauth_grant`；`ix_grant_access` | 已有tenant_id/client_pk/resource_server_id，零DDL变化 | 仅§11核对SQL/索引，不复写整表或画ER | §11 |
| 现有Grant写接口与VO字段 | Unchanged | EVD-002/003；主Spec API-028–030 | 写入/错误/版本规则不变；继续复用VO | 回归边界 | §14 |
| 鉴神权限/资源目录、GraphQL | Not applicable | 主Spec §3.3与当前HTTP页面 | 无新授权模型或GraphQL | N/A | §13 |

## 4. Requirements and Acceptance Criteria

| ID | Atomic requirement | Priority | Observable acceptance criteria | Source |
| --- | --- | --- | --- | --- |
| REQ-001 | 已有Grant页面的GET返回当前已授权客户端在所选realm中的有界集合 | Must | 0条为`[]`；非空字段与ClientResourceGrantVO完全相同，稳定排序 | EVD-001；主Spec REQ-005 |
| REQ-002 | 查询只接受可信realm与原有Grant管理读权限 | Must | 他tenant、未授权platform、伪造身份头均拒绝，DB中无跨tenant行 | 主Spec §9.0/§15；EVD-004 |
| REQ-003 | GET是无业务写的Query且不增加存储/网络前置 | Must | 无INSERT/UPDATE/Outbox、无查询参数搬运接口；Mapper使用既有索引 | 主Spec CQE/最小设计 |

### 4.2 Use-case analysis

| Actor ID | Actor/role | Goal and responsibility | Entry/channel | Permission/tenant context | Evidence |
| --- | --- | --- | --- | --- | --- |
| ACTOR-001 | 授柄Grant管理员 | 核对一个客户端实际获授resource/scope | 已有Grant页面 | 鉴神现有 `tianquan-shoubing:resource-server:grant` 与当前tenant或受限platform | EVD-001/002 |

| ID | Use case/goal | Primary actor | Supporting actors/systems | Trigger | Preconditions | Main success outcome | Alternatives/failures | Postconditions | Requirements | Interfaces/pages | Tests |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| UC-001 | 查看客户端Grant | ACTOR-001 | 授柄Client/Grant存储 | 打开页面或写后刷新 | 用户/realm受信、client存在、原权限允许 | 返回≤64条同realmGrant | 空[]、跨租户403、未知client404、依赖503 | 无业务状态写/权限扩张；仅查询审计/指标 | REQ-001/002/003 | API-045；ClientResourceGrantPage | TEST-001/002/003 |

## 5. Constraints, Assumptions, and Decisions

保持主Spec的USER/SERVICE/PLATFORM、client永久身份、OAuthGrant表、原权限码、原VO和错误模型。ASM-001：一个客户端最多64个resourceUris，purpose固定且每resource只一种grantType，故最多64个有效Grant；源为主Spec §9/§11。若旧数据超过64条，导入在切流前报冲突而不截断结果。不存在开放的重大用户决策：本修订本身仍须用户作为后续修订确认，不能把其Review当Accepted。

| ID | Decision | Owner | Evidence/impact |
| --- | --- | --- | --- |
| DEC-001 | 在现有页面已调用的准确GET路由补齐，不新增selector/prefetch | 待用户审核本修订 | EVD-001/002；既有页面真实消费结果、零额外前置RTT |
| DEC-002 | 精确realm/权限校验与主Spec API-028–030同一能力；裸数组响应 | 待用户审核本修订 | EVD-003/004；原UI normalizePage接数组，写接口VO不变 |

## 6. Project Technology Context

Java21、Boot3.5.16、Spring MVC Controller、Jakarta Validation、MapStruct/MP和现有Ant Design/React Query见主Spec §6与本仓库POM。当前相关包实际为`admin.resource.controller/service/service.impl/repository/dao/domain`，选定主Spec准许的Traditional Three-Layer profile；Controller→Service接口→service.impl→Repository→Mapper，不增模块、架构层或依赖。复用：ClientResourceGrantController/ResourceServerService/ClientResourceGrantVO、主Spec新OAuthClientRepository/OAuthGrantRepository/OAuthGrantMapper、原权限端口与React Query；没有新的DTO、VO、Bean、缓存、表、依赖或Utils。

### 6.2 User-mandated Java rule compliance

| Literal rule | Affected? | Repository evidence | Exact design decision | Files/types/interfaces | Validation/test evidence | Status/blocker |
| --- | --- | --- | --- | --- | --- | --- |
| Rule 1 | No | EVD-003已有VO可用 | 不新增Java carrier或Access组件类型；仅现有符号加方法 | §8路径 | 命名清单零新增 | N/A |
| Rule 2 | Yes | MVC→Service→MP Query | @Validated方法参数与Repository可信realm/已验clientId，手工非代理入口ValidationUtils触发同一约束 | §8/9 | 空/非法/跨tenant负例 | PASS |
| Rule 3 | No | EVD-003/主Spec §10已定义VO与Converter | 复用VO与已选BaseForwardConverter<OAuthGrantBO,ClientResourceGrantVO>，不新增模型或转换器 | §10 | 字段JSON契约 | N/A |
| Rule 4 | Yes | 旧Controller手写构造需随本次触及改为具名Bean/@Slf4j/@RequiredArgsConstructor、每依赖@Qualifier | 仅受影响Controller/ServiceImpl符合主Spec并沿用SA lombok.config | §8 | 装配测试 | PASS |
| Rule 5 | No | 无新utility | 只调用已有标准List/Comparator | §7 | import搜索 | N/A |
| Rule 6 | Yes | VO与TS字段 EVD-003 | Jackson标准字段名与既有VO一致；tenantId null保留，secret不存在 | API-045 | JSON/OAS字段断言 | PASS |
| Rule 7 | No | 只新增方法，没有配置键 | base/local YAML保持结构不变 | §8 | profile diff无键改动 | N/A |
| Rule 9 | No | 本查询无状态机/变化轴 | 直接Service Query，权限/realm策略复用主Spec已有适配模式 | §7 | 少量单元负例 | N/A |
| Rule 10 | No | VO无日期字段；不改时钟 | 不引入Date/Instant字段 | §9 | import检查 | N/A |
| Rule 11 | Yes | 主Spec §6/8传统包/MP Starter | 原三层目录及Service/Repository/Mapper依赖方向不变、CQE Query无Event，schema受管未改 | §7/8/11 | Arch/static和Mapper SQL测试 | PASS |

## 7. Architecture Design

### 7.0 Minimum-design baseline and element-necessity audit

| Proposed element | Change | Requirements | Existing/direct alternative | Concrete inadequacy of alternative | Added calls/state/coupling/failures/migration/operations | Verdict |
| --- | --- | --- | --- | --- | --- | --- |
| API-045及对应Service/Mapper查询 | Add | REQ-001 | 仅现有PUT/DELETE/批量POST | 页面不能读取已授Grant，写后刷新仍404 | 一次用户主动查询，0新表/缓存/事件 | Add |
| 新realm预取API/新VO/新权限 | New candidate | REQ-002 | Principal/registry已知realm，旧VO足够 | 无缺口 | 多RTT和授权状态 | Remove |

现状打开页面：1个GET资源目录+1个GET Grant（后者404）；目标仍为这2次调用，后者200/[]，不新增前置请求。对同一client的写后invalidate也不增调用类型。权限与租户检查从现有上下文导出，前端选realm只表达操作意图，服务端再次核验。

| Path | Network calls | Client states | Server contracts/state | Failure and TOCTOU points | Additional user/business value |
| --- | --- | --- | --- | --- | --- |
| 当前页面 | 资源目录GET、Grant GET（404） | 加载后显示读取不可用 | 缺少Grant查询处理器 | 无法核对刚提交的授权 | 无 |
| 目标页面 | 同样两次GET，第二次200数组 | 加载、空、有值、拒绝、故障 | 仅增加一个只读GET | 写后重新读；服务端每次校验realm/权限 | 看到准确resource/scope/status/version |

### 7.1 System Architecture Design

Actor→现有Grant页→新增GET方法→ResourceServerService接口→原service.impl读取client后→OAuthGrantRepository/Mapper→已定义`oauth_grant`，结果经既有Converter为裸VO数组。客户端是否存在在读Grant之前验证，且与realm绑定；平台realm仅现有平台管理主体可选。鉴神/目录为认证/权限上下文，无新增调用。已有写接口和事件链照旧。

### 7.2 High-Level Design

输入clientId和可选realm（缺省已验principal tenant），先校验realm/权限及客户端在该realm存在，查该Client的active Grant，按resourceServerId升序再grantType、id升序，最多64。为空返回200[]，没有表写或Outbox；404仅client在当前授权可见realm不存在，其他realm不披露。Redis/DB权限依赖的503沿主Spec；不是因为无Grant就404。

### 7.3 Detailed Design

`ClientResourceGrantController#list(String clientId,String realm,IdentityPrincipal)`：对principal要求已有`resource-server:grant`权限，realm由主Spec受信解析并校验，`@Validated @PathVariable @NotBlank`；调用`ResourceServerService#listGrants(clientId,realm)`，只返回`List<ClientResourceGrantVO>`。Service实现读取`OAuthClientRepository.findByClientId(trustedRealm,clientId)`，无Client→404；随后`OAuthGrantRepository.listByClient(trustedRealm,clientPK)`；用已有`ClientResourceGrantViewConverter`映射，保持tenantId nullable；`@Transactional(readOnly=true)`。Mapper具名XML：`WHERE tenant_id=:trustedStoragePartitionId AND client_pk=:id AND deleted_at IS NULL ORDER BY resource_server_id,grant_type,id LIMIT 65`；65只为发现数据异常，检测到第65条不截断成功响应，报一致性错误503并告警/阻断该tenant切流。当前`ix_grant_access(tenant_id,client_pk,resource_server_id)`提供等值+顺序前缀，少量行允许grantType/id本地排序；不新增索引。

## 8. Package Structure and Code File Tree

| Operation | Exact path/symbol | Responsibility | Requirement |
| --- | --- | --- | --- |
| MODIFY | `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/resource/controller/ClientResourceGrantController.java#list` | 新GET/@Operation/安全校验；Controller只调用Service接口 | REQ-001/002 |
| MODIFY | `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/resource/service/ResourceServerService.java#listGrants` 与 `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/resource/service/impl/ResourceServerServiceImpl.java#listGrants` | 只读编排及realm client核验 | REQ-001/002/003 |
| MODIFY | 主Spec §8.4 `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/resource/repository/OAuthGrantRepository.java#listByClient`、`egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/resource/dao/OAuthGrantMapper.java#selectByClient`、`egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/resources/mapper/resource/OAuthGrantMapper.xml#selectByClient` | 启用主Spec已有index和具名SQL，无新表 | REQ-001/003 |
| MODIFY | `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin-web/src/features/resource-grants/ClientResourceGrantPage.tsx#grantQuery` | queryKey/请求携带已选realm、空/404/403/503分支 | REQ-001/002 |
| MODIFY | `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin-web/src/features/resource-grants/ClientResourceGrantPage.test.tsx`、新增Controller/Service/Mapper合同测试 | RED/GREEN和跨realm回归 | REQ-001/002/003 |

以上是完整仓库相对路径；主Spec的既有ClientResourceGrantVO和前端api/types.ts字段不变。现有`ClientResourceGrantVO`/前端`api/types.ts`字段不变。

## 9. Interface Definitions

### 9.0 API protocol and documentation governance

| Concern | Decision/evidence |
| --- | --- |
| Protocol selection | REST资源Query；现有UI已消费准确方法/路径；不增加GraphQL |
| CQE application level | L0只读Query，现有Service/DB，不发Event |
| REST source of truth | Code-first Controller Spring mapping+VO/Jackson/Validation+`io.swagger.v3.oas.annotations.*` |
| GraphQL source of truth | N/A：无GraphQL |
| Springdoc/OpenAPI compatibility | Boot3.5.16/springdoc2.8.17/OAS3.1，沿主Spec springdoc组 |
| Legacy Swagger/Springfox status | 当前Controller已用OpenAPI3，不引入Springfox |
| Security and documentation exposure | bearerAuth+现有Grant管理权限，realm guard；遵主Spec生产docs限制 |
| Contract publication and drift gate | OAuthOpenApiContractTest断言method/path/operationId/parameters/200数组及400/401/403/404/503；合同测试对真实Controller响应字段 |

### 9.1 Interface Inventory

| ID | Change/necessity verdict | Name/purpose | Kind | API style/CQE role | Consumer | Owner | Method + URL / GraphQL field / symbol / topic | Operation ID/schema source | Input | Output | Auth/tenant | Error model | Idempotency/version | Requirements |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| API-045 | New/Add：既有页面独立展示目的 | List client grants | HTTP | REST Query | ClientResourceGrantPage | ClientResourceGrantController | GET /api/v1/tianquan-shoubing/clients/{clientId}/resources | tianquan-shoubing-client-resource-grant-list-v1 | clientId path + realm query | 裸ClientResourceGrantVO数组 | Bearer USER，grant权限+realm核验 | code/message错误；200空数组 | 读幂等，按资源稳定排序 | REQ-001,REQ-002,REQ-003 |

### 9.2 Per-interface Detailed Contracts

#### 9.2.1 API-045 — List client grants

##### Necessity and interaction-cost decision

| Concern | Decision |
| --- | --- |
| Change classification | New GET；页面现有调用的目标路由缺失 |
| Independent consumer goal | 管理员核对已授资源、scope、status，并决定追加/停用；写后重读刷新 |
| Parameter ownership and derivation | clientId来自当前路由；realm来自已验Principal或管理员显式选择并后端授权，不从头部采信身份 |
| Direct/no-new-interface alternative | 已有PUT/DELETE/批量POST只返回单次命令结果，不能重建全部已有Grant；不可能由Client/资源目录推导实际Grant集合 |
| Caller use of result | 直接展示、按resourceServerId关联资源、批量变更前核对版本；没有复制原样给另一个命令 |
| Round trips and failure points | 页面已经发起此GET，本修订不加额外请求；DB查询/权限失败进入独立可恢复状态，写后仍须重新查询 |
| Verdict | Add，REQ-001/主Spec REQ-005；不加selector/prefetch |

##### API style and CQE semantics

| Concern | Decision/evidence |
| --- | --- |
| Protocol style | REST Query |
| CQE role | L0 Query，只读当前Grant，不提交业务命令/事件 |
| Resource/task semantics | GET现有Client的resources子集合；空集合仍为200 |
| Read/write and side effects | 读Client存在性与Grant集合；可记安全访问审计/指标，无业务表写 |
| Consistency and idempotency | 单次PRIMARY READ COMMITTED快照；写后invalidate再读，不承诺跨请求长事务快照 |
| Why this style | 现有页面代码已调用相同REST URL且需要授权集合，返回当前VO数组即可 |

##### Identity and purpose

| Concern | Definition |
| --- | --- |
| Purpose/owner/consumer | 客户端Grant列表；授柄Controller；现有ClientResourceGrantPage |
| Protocol and endpoint | GET /api/v1/tianquan-shoubing/clients/{clientId}/resources |
| Content type/version | application/json，原`api/v1`管理接口 |
| Auth/permission/tenant | USER Bearer经本地JWT和撤销校验；`tianquan-shoubing:resource-server:grant`；realm按主Spec §9.0缺省当前租户，platform仅平台管理员强认证 |
| Timeout/retry/rate limit | 管理查询默认网络10秒，单主体每分钟120次；503可由用户刷新一次，不自动无限重试 |
| Idempotency/concurrency | GET读幂等；Grant写入与此GET并发时取本次READ COMMITTED提交点的结果，按resourceServerId/grantType/id稳定排序 |

##### Request parameters

| Name | Location | Type/format | Required/null | Default | Validation/range/enum | Meaning | Example | Source |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| clientId | Path | string | 必填，非null非空 | None | 1–128 ASCII `[A-Za-z0-9._-]+`；精确大小写 | 已选择Client的外部ID | `orders-service` | 页面route |
| realm | Query | string | 可省；若存在不可null/空 | 已验Principal当前TENANT | `tenant:<正Long>`或`platform`，≤64字符；platform需强认证 | 实际管理的issuer/realm意图 | `tenant:1001` | 页面已选realm；后端重校验 |
| Authorization | Header | Bearer | 必填 | None | Spring Security verified USER AT，aud应包含本管理Resource | 身份与权限 | `Bearer synthetic` | SCG TokenRelay/外部客户端 |
| Body | None | None | 无 | None | GET不接受请求体 | 无 | 无 | 无 |

##### Success response

HTTP 200；`application/json`，`Cache-Control: no-store`。裸数组0–64项，空为`[]`且非null；按resourceServerId、grantType、内部id升序，不暴露内部id。各元素字段完全沿用已存在ClientResourceGrantVO，不新增issuer字段；当前选realm来自页面/session。单项完整JSONC：

```jsonc
[
  {
    "clientId": "orders-service", // 已选Client外部ID，精确字符串。
    "resourceServerId": "orders-resource", // 目标ResourceServer稳定外部ID。
    "grantType": "CLIENT_CREDENTIALS", // CLIENT_CREDENTIALS或USER_DELEGATION；与Client purpose一致。
    "tenantId": "1001", // TENANT grant的业务租户；USER_DELEGATION/PLATFORM时null。
    "allowedScopes": ["orders:read"], // 明确获准scope数组，0–64项、去重排序；无scope为[]。
    "status": "ACTIVE", // ACTIVE或DISABLED；不把已禁用Grant当有效授权。
    "version": 0 // 非负乐观锁版本，供现有编辑流程使用。
  }
]
```

##### Error responses

| Condition | HTTP/protocol status | Business code | Response shape | Retryable | Frontend handling |
| --- | --- | --- | --- | --- | --- |
| 缺/非法clientId或realm | 400 | OAUTH_REQUEST_INVALID | 下列管理错误JSON | 修正输入后 | 保留页面选择，不发查询 |
| Bearer缺失/无效 | 401 | INVALID_TOKEN | 管理错误JSON与WWW-Authenticate | 新登录后 | 转入登录，不无限跳转 |
| Grant权限不足/tenant或platform越权 | 403 | ACCESS_DENIED | 同形，不能透露其他realm客户端存在 | No | 显示无权限 |
| 此授权可见realm不存在Client | 404 | CLIENT_NOT_FOUND | 同形 | No | 返回Client列表或展示当前选择无效 |
| DB/撤销依赖不可用、存量超过64违约 | 503 | TEMPORARILY_UNAVAILABLE / GRANT_LIST_LIMIT_EXCEEDED | 同形，脱敏 | 依赖恢复后一次 | 显示错误，不把失败误成空列表 |

```jsonc
{
  "code": "CLIENT_NOT_FOUND", // 稳定错误码，实际状态由条件决定，不泄露跨租户对象。
  "message": "Client not available" // 可显示安全说明；不包含其他realm、SQL或secret。
}
```

##### Interface logic for frontend and consumers

1. 页面从当前已选择的clientId/realm发一个GET；UI queryKey包含realm，不能沿用另一个tenant的缓存。
2. Controller验证`@PathVariable`、可选realm、已验principal及`resource-server:grant`权限，拒绝任意身份头的租户覆盖。
3. Service从trusted realm读取Client；客户端缺失404；Repository以storagePartitionId/clientPK/activeRow查询0–65条，发现第65条报配置一致性故障而不截断。
4. Mapper用主Spec已定`ix_grant_access`；查询只读不持久化/不投Event；MapStruct已有ViewConverter输出原VO。
5. 空200[]；非空排序后返回，前端按resourceServerId关联资源，并只显示此realm的授权和版本。
6. 401转登录、403权限态、404当前client无效、503保留可重试数据/提示；写后invalidate该realm的Grant query。
7. 前端在loading/empty/error/denied状态间保持过滤/已选realm，不把另一realm数据当兜底。

##### Documentation contract

| Concern | Decision/evidence |
| --- | --- |
| Documentation authority | 原`ClientResourceGrantController`拥有Spring GET/@Operation/@ApiResponses，VO及Bean Validation/Jackson为真实wire权威 |
| REST OpenAPI operation / GraphQL SDL operation | `operationId=tianquan-shoubing-client-resource-grant-list-v1`，`GET /api/v1/tianquan-shoubing/clients/{clientId}/resources` |
| Annotation/mapping ownership | 原Controller加`@Operation`、`@Parameter`、`@ApiResponses`与现有`bearerAuth` SecurityRequirement；不建注解接口 |
| Generated schema elements | clientId Path必需、realm Query可选、200 ClientResourceGrantVO数组及空集合、400/401/403/404/503裸管理错误、no-store与bearerAuth |
| Compatibility and drift proof | MockMvc校验JSON字段/空集合/状态，OAuthOpenApiContractTest解析`/v3/api-docs`断言本route/唯一operationId/schema/security；前端Vitest核对queryKey |

| Target | Required annotation/configuration | Exact values/source | Generated OAS effect | Verification |
| --- | --- | --- | --- | --- |
| ClientResourceGrantController#list | `@GetMapping`, `@Operation`, `@ApiResponses`, `@Parameter`, `@SecurityRequirement` | 上述method/path/operationId/权限与错误状态 | 同一路由一个GET，不与PUT/DELETE冲突 | MockMvc OAS断言 |
| ClientResourceGrantVO | 现有Jackson字段规则及`@Schema`仅补非显然null/enum | 7个现有字段，上述完整JSONC | 数组item schema一致 | JSON/OAS字段比对 |

##### Compatibility and verification

新增GET仅填补已有前端的404，原PUT/DELETE/批量POST方法/URI/body/VO/权限不变。现有JS `normalizePage`可处理裸数组；当前404被页面当作“读取不可用”，新增合同后404只表示client当前realm不存在，须校正页面提示。测试覆盖空/单条/64条/第65条异常、两个tenant同clientId不同grant、PLATFORM管理员与普通tenant用户、禁用Grant状态、错误/JSON/OAS、前端realm query key/写后刷新。

### 9.3 OpenAPI 3 and springdoc annotation plan

| Target | Required annotation/configuration | Exact values/source | Generated OAS effect | Verification |
| --- | --- | --- | --- | --- |
| ClientResourceGrantController#list | `io.swagger.v3.oas.annotations.@Operation/@ApiResponses/@Parameter/@SecurityRequirement` | `operationId=tianquan-shoubing-client-resource-grant-list-v1`；bearerAuth；200/400/401/403/404/503 | GET集合与VO数组schema | OAuthOpenApiContractTest |
| ClientResourceGrantVO | 既有Jackson/Validation，必要`@Schema`说明nullable tenantId/enum | 7字段 | 真实字段名与required一致 | VO序列化+OAS校验 |

### 9.4 API contract generation and blocking gate

| Gate ID | Applicability | Status | Evidence | Finding | Required action/exception |
| --- | --- | --- | --- | --- | --- |
| API-GATE-001 | Applicable | PASS | §7.0/9.2/EVD-001 | 既有前端独立展示已授Grant，不是参数预取 | None |
| API-GATE-002 | Applicable | PASS | §9.0/9.2 | 单REST Query/L0，无GraphQL或Bus | None |
| API-GATE-003 | Applicable | PASS | §9.2 | GET/200空数组/no-store/具体400–503/排序与兼容 | None |
| API-GATE-004 | Not applicable | N/A | EVD-001/§9.0 | 无GraphQL操作/SDL/Resolver | None |
| API-GATE-005 | Applicable | PASS | EVD-003/§9.2/§10 | 现有VO 7字段/可空tenantId与JSONC/TS一致 | None |
| API-GATE-006 | Applicable | PASS | 主Spec §9.0、§9.2 | 权限/realm/敏感字段/限流/错误披露明确 | None |
| API-GATE-007 | Applicable | PASS | 当前Controller及§9.3 | springdoc2.8.17/OpenAPI3.1注解在原Controller | None |
| API-GATE-008 | Applicable | PASS | §14 TEST-001–003 | MockMvc OAS/Bean校验/前端Vitest有准确验证对象 | None |
| API-GATE-009 | Applicable | PASS | §4/9/10/11/12/14 | Client外部ID、tenant存储分区、VO与UI状态一致 | None |

## 10. POJO and Data Model Design

Unchanged：ClientResourceGrantVO、OAuthGrantPO/BO、ClientResourceGrantViewConverter均由主Spec §10/11定义；本修订不加字段、enum或新的Model。DTO源/视图边界保持：数据库client_pk与tenant_id仅在Service内部处理，输出只有VO的7个字段，`tenantId`保留null值用于PLATFORM/USER_DELEGATION。无跨新角色转换，仍由主Spec的BaseForwardConverter<OAuthGrantBO,ClientResourceGrantVO>映射。普通class/Lombok、@EnumValue/@JsonValue、Jackson按已确认主Spec执行，未触及用户密码/日期。

## 11. Database Design

Relational model change: No — 主Spec §11 `tianquan_oauth.oauth_client` 与`oauth_grant`已设计，表/键/索引/DDL无变化；不把旧Flyway文件改成假新版本。只复用`ix_grant_access(tenant_id,client_pk,resource_server_id)`及业务UK，查询SQL为`SELECT ... FROM tianquan_oauth.oauth_grant WHERE tenant_id=:trusted AND client_pk=:clientPK AND deleted_at IS NULL ORDER BY resource_server_id,grant_type,id LIMIT 65`。需要预先检查Client同realm可见；P95/EXPLAIN待真实PG数据验证。无ER变化不画新ER；下一代码Step按原PO/Mapper路径补具名查询即可。数据库query-only不产生Event/Outbox或事务内额外写。

## 12. Frontend Page Design

Affected：`ClientResourceGrantPage.tsx#grantQuery` 及同名test；复用现有Card/Table/Alert/Popconfirm、Permission guard和`normalizePage`，不重做页面布局。当前页面从clientId路由和Admin上下文获得已选realm；queryKey改为`['tianquan-shoubing','grants',realm,clientId]`，GET query明确附`realm`仅当管理员曾选择platform或另一个获授权realm；普通tenant可省略并由后端默认推断。写接口PUT/DELETE/批量POST沿主Spec §9.0携带同一个已选择realm，在后端逐次授权；写成功invalidate此realm的Grant查询，切realm清旧缓存/已选资源行，不能展示另一realm Grant。

状态：加载保留骨架和禁用写入；200[]显示空态和“尚未授权资源”；200非空显示resource/scope/status/version；401跳明确登录；403显示无权限；404显示当前client在此realm无效并提供返回Client列表入口；503展示错误与一次重试按钮，不清除操作者输入或把错误当空。屏幕阅读器读出状态和表头；键盘可重试/返回；移动窄屏保持现有表格横向容器，弹窗焦点返回触发按钮。保存/删除待服务端成功再刷新，双击时禁用按钮。TEST-003验证每状态和realm query key。

## 13. Design Patterns and Architecture Principles

本Query按原Controller→Service接口→ServiceImpl→OAuthGrantRepository→Mapper链直接实现；现有realm Adapter和BaseForwardConverter已由主Spec选定。无新增变化轴、状态机、Factory/Strategy/缓存，直接查询更清楚，符合主Spec最小化原则。Controller不访问Mapper或service.impl；Repository不做授权决策，Service统一验证当前Client和realm。

## 14. Test Design

| ID | Level | Target | Scenario/input | Expected assertion | Test double/data | Tool/path | Requirements |
| --- | --- | --- | --- | --- | --- | --- | --- |
| TEST-001 | Unit/MockMvc | ClientResourceGrantController#list/Service | 默认realm/显式platform/错误clientId | GET200[]或VO数组；400/401/403/404；权限在DAO前 | two realm USER principals | `ClientResourceGrantControllerTest` | REQ-001/002 |
| TEST-002 | DAO/PostgreSQL | OAuthGrantMapper#selectByClient | A/B同clientId不同PK，同resourceId，disabled/softdeleted，0/1/64/65 | 仅当前tenant；≤64稳定序；65报一致性503；无SQL写；索引EXPLAIN | 真实PG数据需用户启动验收，Mockito SQL片段先RED | `OAuthGrantRepositoryTest`/后续PG IT | REQ-001/002/003 |
| TEST-003 | React/Vitest | ClientResourceGrantPage#grantQuery | 200[]、200非空、401/403/404/503、切realm/写后刷新 | 对应UI状态，queryKey含realm，所有写请求使用同realm，无跨租户旧缓存 | mock httpClient | 既有`ClientResourceGrantPage.test.tsx` | REQ-001/002 |

不新增一次“获取realm信息再提交”的请求。Java focused Controller+Service测试与OAS生成合同测试用既有maven test，无服务启动；真实PG EXPLAIN/隔离需实施后用户启动验证，不在Spec阶段声称通过。

## 15. Non-functional and Cross-cutting Design

一次查询上限64条，响应`no-store`不被共享缓存，后端不把subject/clientId作为指标标签；现有trace记录结果/时延但不记录grant secret（VO无secret）。realm/数据库不可用与普通空列表严格区分，保护跨tenant客户端存在性；安全事件审计沿主Spec §15，而本Query不产生CQE业务事件。SQL过滤依靠已确认MP TenantLine+自定义Mapper双重条件，不从请求头读取tenant。

## 16. Compatibility, Migration, Rollout, and Rollback

主Spec的受管SQL/manifest版本不因本修订增加第二迁移；本接口可以在新表/Service/权限桥具备后与现有写接口一起发布。客户端现已调用目标URL，后端补路由使404转200；前端对真正不存在Client的404改语义提示，tenant切换清缓存。回退此GET实现会恢复页面404，因此随目标Grant管理读链一起回退；已签发/撤销状态不因只读接口回退。最小发布证据为本接口OAS、MockMvc、前端测试以及主Spec Step的同realm SQL验收。

## 17. Alternatives and Decisions

| Option | New elements and interactions | Advantages | Disadvantages/risks | Repository fit | Decision and rationale |
| --- | --- | --- | --- | --- | --- |
| A：直接补现有GET | 一条读API+Service/Repo/Mapper方法，页面现有一次GET不变 | 返回真实授权集合，用户能核对scope | 一个公开合同须长期维护 | 页面已在调用 | 推荐，待审核 |
| B：页面从资源目录猜Grant或先取多个单Grant | 多RTT/缓存/竞态且未获grant事实 | 无新集合API | 无法区分未授与禁用/跨tenant，额外耦合 | 不符合授权主权 | 拒绝 |

## 18. Risks and Open Questions

| ID | Risk/question | Probability | Impact | Mitigation or decision owner | Status |
| --- | --- | --- | --- | --- | --- |
| RISK-001 | 旧数据同Client授予超64条导致无法完整展示 | Unknown | 该tenant不应带缺失视图切流 | 迁移报告列冲突，人工收敛/明确权限后重验 | Open运行数据条件，不是设计待决 |
| RISK-002 | Plan已写但本修订尚未被用户接受 | Certain | 不能把API-045作为Approved设计执行 | 用户审核本修订；Plan状态Blocked | Open用户审核门禁 |

## 19. Traceability Matrix

| Requirement | Use case | Affected area/chapter | Context-only or unchanged boundary | Interface/model/database/frontend | Tests | Acceptance evidence |
| --- | --- | --- | --- | --- | --- | --- |
| REQ-001 | UC-001 | §7/8/9/12 | 现有VO/Grant写接口 | API-045 + ClientResourceGrantPage | TEST-001–003 | 200[]/有序VO数组、写后刷新 |
| REQ-002 | UC-001 | §7/9/12/15 | 鉴神原授权模型 | realm guarded Service Query | TEST-001/002/003 | 另tenant/普通用户platform均不能读 |
| REQ-003 | UC-001 | §7/9/11 | 主Spec OAuthGrant schema/index | 只读具名SQL，无Event | TEST-002 | SQL无写、无新增DDL/RTT |
| REQ-005（主Spec） | UC-001 | §7/9/12 | 主Spec最小scope及授权存储不变 | API-045展示实际授予scope | TEST-001–003 | 管理员可核对每个已授资源与scope |

## 20. Review and Acceptance

### 20.1 Original-request fidelity

修订只补当前Grant页面已请求的独立读能力；用户原有最小scope与跨realm隔离不变。未新增身份查询/预取API、表、依赖、页面、权限码或管理策略。

### 20.2 Repository and technical fidelity

当前Controller路由缺失及前端请求均由源代码核对，主Spec状态Accepted，底层MP目标表在其§11内。无应用启动或数据库连接证明。

### 20.3 Cross-section consistency

API-045的clientId/realm/字段/空集合/权限/错误与VO/React Query/MP SQL/测试一致；只有一个API ID、一个Method+URL和一个详情。Context-only与Unchanged区域无新文件/数据模型。接受本修订后，目标Plan在对应Step加入Controller/Service/Repository/Mapper与前端测试，并重新过全部Manual Checks。

### 20.4 Relationship and effective-design review

`Amends`只命名主Spec中遗漏的Grant列表接口与其UI/测试内容；主Spec其他章节、API-001–044及数据库版本不变。旧主Spec正文不改。此Review不是Accepted。

### 20.5 Blocking Manual Check

| Check ID | Applicability | Status | Evidence | Finding | Required action/exception |
| --- | --- | --- | --- | --- | --- |
| MC-ARCH-001 | Applicable | PASS | EVD-001–004；主Spec §9/10/11，本修订§7–14 | 本次设计已给出直接复用、错误与合同测试；不等于生产实现通过 | None |
| MC-REUSE-001 | Applicable | PASS | EVD-001–004；主Spec §9/10/11，本修订§7–14 | 本次设计已给出直接复用、错误与合同测试；不等于生产实现通过 | None |
| MC-DEP-001 | Applicable | PASS | EVD-001–004；主Spec §9/10/11，本修订§7–14 | 本次设计已给出直接复用、错误与合同测试；不等于生产实现通过 | None |
| MC-NAME-001 | Applicable | PASS | EVD-001–004；主Spec §9/10/11，本修订§7–14 | 本次设计已给出直接复用、错误与合同测试；不等于生产实现通过 | None |
| MC-VALID-001 | Applicable | PASS | EVD-001–004；主Spec §9/10/11，本修订§7–14 | 本次设计已给出直接复用、错误与合同测试；不等于生产实现通过 | None |
| MC-MODEL-001 | Applicable | PASS | EVD-001–004；主Spec §9/10/11，本修订§7–14 | 本次设计已给出直接复用、错误与合同测试；不等于生产实现通过 | None |
| MC-CONVERT-001 | Applicable | PASS | EVD-001–004；主Spec §9/10/11，本修订§7–14 | 本次设计已给出直接复用、错误与合同测试；不等于生产实现通过 | None |
| MC-LOG-001 | Applicable | PASS | EVD-001–004；主Spec §9/10/11，本修订§7–14 | 本次设计已给出直接复用、错误与合同测试；不等于生产实现通过 | None |
| MC-BEAN-001 | Applicable | PASS | EVD-001–004；主Spec §9/10/11，本修订§7–14 | 本次设计已给出直接复用、错误与合同测试；不等于生产实现通过 | None |
| MC-UTIL-001 | Applicable | PASS | EVD-001–004；主Spec §9/10/11，本修订§7–14 | 本次设计已给出直接复用、错误与合同测试；不等于生产实现通过 | None |
| MC-JSON-001 | Applicable | PASS | EVD-001–004；主Spec §9/10/11，本修订§7–14 | 本次设计已给出直接复用、错误与合同测试；不等于生产实现通过 | None |
| MC-TIME-001 | Applicable | PASS | EVD-001–004；主Spec §9/10/11，本修订§7–14 | 本次设计已给出直接复用、错误与合同测试；不等于生产实现通过 | None |
| MC-CONFIG-001 | Applicable | PASS | EVD-001–004；主Spec §9/10/11，本修订§7–14 | 本次设计已给出直接复用、错误与合同测试；不等于生产实现通过 | None |
| MC-PATTERN-001 | Applicable | PASS | EVD-001–004；主Spec §9/10/11，本修订§7–14 | 本次设计已给出直接复用、错误与合同测试；不等于生产实现通过 | None |
| MC-SCOPE-001 | Applicable | PASS | EVD-001–004；主Spec §9/10/11，本修订§7–14 | 本次设计已给出直接复用、错误与合同测试；不等于生产实现通过 | None |
| MC-TEST-001 | Applicable | PASS | EVD-001–004；主Spec §9/10/11，本修订§7–14 | 本次设计已给出直接复用、错误与合同测试；不等于生产实现通过 | None |
| MC-BLOCKER-001 | Applicable | PASS | EVD-001–004；主Spec §9/10/11，本修订§7–14 | 本次设计已给出直接复用、错误与合同测试；不等于生产实现通过 | None |

### 20.6 Final verdict

**PASS — Ready for user review**

此处PASS只表示文档内部完整。Status=Review；主Spec仍Accepted，本修订待用户决定后才纳入有效设计并使Plan可进入Review。未创建Plan执行文件之外的生产代码或数据库变更。
