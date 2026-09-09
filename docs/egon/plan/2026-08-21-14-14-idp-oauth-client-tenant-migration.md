# IdP OAuth2 Client、租户权威与 DDC SERVICE Token 迁移实施计划

| Field              | Value                                                                                                                                                                                                                                                                                 |
|--------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Document           | `2026-08-21-14-14-idp-oauth-client-tenant-migration.md`                                                                                                                                                                                                                               |
| Template Version   | `2`                                                                                                                                                                                                                                                                                   |
| Status             | `Review`                                                                                                                                                                                                                                                                              |
| Created            | `2026-08-21 14:14 CST`                                                                                                                                                                                                                                                                |
| Updated            | `2026-08-22 12:53 CST`                                                                                                                                                                                                                                                                |
| Owner              | `Mario / Egon-COLA platform maintainers`                                                                                                                                                                                                                                              |
| Repository         | `Egon-COLA`                                                                                                                                                                                                                                                                           |
| Scope              | `egon-cola-tianquan-shoubing、egon-cola-tianquan-jianshen、egon-cola-tianshu、IdP/RBAC3 Admin Web 与统一身份维护脚本`                                                                                                                                                              |
| Source Requirement | `业务服务改用 Spring Security OAuth2 Client + AppID/App Key/Secret；凭证由 IdP Web 预配并入库；tenant 权威迁入 IdP；DDC 直接验证定向 PLATFORM SERVICE Token；用户确认 1A/2A/3A 以及权限、membership port、bootstrap 三项修订`                                                                                                   |
| Baseline Revision  | `main@772df2b2（Step 18）；Step 1=913179c3、Step 2=8c83e361（含 5692763f、c5a537fa 纠偏）、Step 3=b374559b、Step 4=f2fd8230、Step 5=35d1879d、Step 6=39023dfb、Step 7=fd31f5ba、Step 8=08f3d8c6、Step 9=06dd03cd、Step 10=d72ad57d、Step 11=c52fe991、Step 12=551d834e、Step 13=b045c318、Step 14=233eb901、Step 15=20d19d50、Step 16=8bd5ea76、Step 17=323db68a、Step 18=772df2b2 已提交；工作树仅含本 Plan 与主 Spec 的文档修复`                                                                                                  |
| Implements Spec    | [IdP OAuth2 Client、租户权威与 DDC SERVICE Token 准入迁移规格](../spec/2026-08-21-07-51-idp-oauth-client-tenant-ownership.md)                                                                                                                                                                     |
| Spec Status        | `Review`                                                                                                                                                                                                                                                                              |
| Spec Revision      | `Updated 2026-08-21 22:04 CST；baseline main@2bb9262400fae3ed60c88ac5032e4bbc6af65542`                                                                                                                                                                                                 |
| Effective Specs    | [主规格](../spec/2026-08-21-07-51-idp-oauth-client-tenant-ownership.md)；[统一身份平台设计](../../superpowers/specs/2026-08-01-unified-identity-platform-design.md)；[无 Session JWT 与 Gateway 自动刷新规格](../../superpowers/specs/2026-08-13-unified-identity-stateless-jwt-session-removal-design.md) |
| Depends On Plans   | `None`                                                                                                                                                                                                                                                                                |
| Supersedes         | `None`                                                                                                                                                                                                                                                                                |
| Superseded By      | `None`                                                                                                                                                                                                                                                                                |
| Related Plans      | `None`                                                                                                                                                                                                                                                                                |

## 1. Summary

本 Plan 实现唯一主规格，并保留两份规范依赖中未被主规格取代的 USER JWT、Gateway、Resource Server 与 PEP 行为。实施分为 18
个语义步骤：先建立 IdP V5 与 RBAC V8 的可重复迁移证据，再按编译依赖完成 SERVICE principal/context、Client Secret、Token
Endpoint、Spring OAuth2 Client facade、IdP tenant/membership、Identity Directory RPC、RBAC authorization
state/主体校验/bootstrap、DDC registration token、两套 Admin Web 和维护窗口工具。

每一步遵循聚焦 RED→最小 GREEN→模块回归，并形成一个路径受限提交。最终完成证据包括：21 个 `REQ-*` 的测试/静态门禁、两套新
Flyway migration rehearsal、Maven 受影响 reactor、两套 frontend test/typecheck/build、旧 private_key/JWK/Admission/RBAC
tenant 符号归零，以及需由用户控制环境执行的 PostgreSQL/Redis/DDC/Gateway 联调与恢复演练。Plan 本身不执行代码、migration、服务或浏览器。

## 2. Target Spec and Effective Design

### 2.1 Primary target

- Path: [docs/egon/spec/2026-08-21-07-51-idp-oauth-client-tenant-ownership.md](../spec/2026-08-21-07-51-idp-oauth-client-tenant-ownership.md)
- Status: `Review`
- Revision: `Updated 2026-08-21 22:04 CST；baseline main@2bb9262400fae3ed60c88ac5032e4bbc6af65542`
- Approval evidence: 用户显式调用 `egon-coding-writing-plan`，并在 Plan 必要性审计发现三项缺口后确认 `1A 2A 3A`；这授权编写
  `Review` Plan，不等于接受 Spec 或 Plan。

### 2.2 Effective Spec set

| Role                 | Spec/link                                                                                                                     | Status/revision               | Effective sections                                                                                        | Why included                                   |
|----------------------|-------------------------------------------------------------------------------------------------------------------------------|-------------------------------|-----------------------------------------------------------------------------------------------------------|------------------------------------------------|
| Primary              | [IdP OAuth2 Client、租户权威与 DDC SERVICE Token 准入迁移规格](../spec/2026-08-21-07-51-idp-oauth-client-tenant-ownership.md)             | `Review`；2026-08-21 22:04 CST | 全文，尤其 `REQ-001`–`REQ-021`、`DEC-001`–`DEC-008`、§7–§16                                                      | 本次实现的唯一主目标和最新机器身份/tenant/DDC 权威                |
| Normative dependency | [统一身份平台设计](../../superpowers/specs/2026-08-01-unified-identity-platform-design.md)                                            | 已批准，2026-08-02 进入实施           | §8.1、§9.1、§10.1、§11、§12–§13 中未被主规格/2026-08-13 取代的 Identity User、USER Token、Resource Server、Gateway/PEP 边界 | 保留 USER/Resource/PEP 基础模型，不恢复其旧 tenant 权威规则    |
| Normative dependency | [无 Session JWT 与 Gateway 自动刷新规格](../../superpowers/specs/2026-08-13-unified-identity-stateless-jwt-session-removal-design.md) | 已确认且核心实现完成；2026-08-14         | §6–§11、§13–§15 中未被主规格取代的 USER JWT、Cookie、Gateway、无 Session、Signing Key 行为                                 | 保护 `REQ-017` 的 USER/Auth Code/Signing JWK 回归边界 |

### 2.3 Superseded or excluded content

- [OAuth2 Resource Server Admission 设计](../../superpowers/specs/2026-08-10-oauth2-resource-server-admission-design.md)
  中 private_key_jwt、Client JWK、Admission RPC/Ticket、SERVICE Token 强制 tenant 和 Resource Server JWK 管理属于主规格明确
  supersede 的实现基线，不是本 Plan 的目标行为。
- 2026-08-01 设计中“IdP 不保存 tenant membership、RBAC3 是 tenant/membership 权威”的规则被主规格
  `DEC-002/DEC-007/DEC-008` 替换。
- 2026-08-13 中仅为兼容旧 SERVICE 凭证/Admission/RBAC tenant 权威而保留的段落被主规格取代；USER JWT、Gateway refresh、无
  Session 与 IdP Signing Key 继续有效。

## 3. Effective Requirements and Acceptance

| Requirement | Source Spec section | Effective statement                                                          | Observable acceptance                                                    | Implementation impact                   |
|-------------|---------------------|------------------------------------------------------------------------------|--------------------------------------------------------------------------|-----------------------------------------|
| `REQ-001`   | 主规格 §4              | Confidential Client 仅由 IdP 管理员预配，应用不得自注册                                     | 无动态注册；SERVICE Client 不能管理自身；审计含 operator                                 | IdP backend/Web/tests                   |
| `REQ-002`   | 主规格 §4              | appId=业务应用身份、App Key=client_id、Secret=client_secret                          | UI/配置/claim/数据库映射一致，appId active client 唯一                               | IdP V5、Client/Token/Web                 |
| `REQ-003`   | 主规格 §4              | Secret 明文只返回一次，数据库只存 Argon2id hash/hint/audit                                | list/detail/log/DB 无明文；create/rotate 响应一次；事务回滚不泄漏                        | Secret aggregate、安全测试                   |
| `REQ-004`   | 主规格 §4              | client_credentials 仅接受 client_secret_basic                                   | Basic 成功；body secret/assertion/重复 header 拒绝；metadata 仅宣告 Basic           | Token controller/authenticator/metadata |
| `REQ-005`   | 主规格 §4              | idp-starter 使用 Spring Security OAuth2 Client                                 | `ClientRegistration`/`OAuth2AuthorizedClientManager` 获取/续期 Token，无签名私钥代码 | starter POM/autoconfig/facade           |
| `REQ-006`   | 主规格 §4              | Token 授权键隔离 registration/app/audience/context/tenant/scopes                  | 并发请求不跨 key 复用，到期前 single-flight 续期                                       | starter key/cache/tests                 |
| `REQ-007`   | 主规格 §4              | SERVICE Token 含 app/source/credential/context claims                         | 签发/解码字段完整；缺失或矛盾 fail closed                                              | core claim/signer/verifier/DDC          |
| `REQ-008`   | 主规格 §4              | TENANT 必有 tid；PLATFORM 无 tid 且由 Grant 派生                                     | caller 不能自报 context；`tid=*`/混合状态拒绝                                       | Grant/policy/token/principal            |
| `REQ-009`   | 主规格 §4              | DDC 直接验证 DDC audience PLATFORM SERVICE Token                                 | 无 Ticket/RPC；错误 audience/context/scope/source/credential 拒绝              | DDC client/admin                        |
| `REQ-010`   | 主规格 §4              | DDC 将 app/client 与 biz/app/env/instance/lease 绑定，lease≤Token exp             | cross-binding replay 零写；heartbeat 精确匹配；expiry 上界成立                       | DDC verifier/Redis/service              |
| `REQ-011`   | 主规格 §4              | IdP 是 tenant catalog/membership 唯一权威                                         | login/resolve 只读 IdP DB；无 IdP→RBAC membership HTTP                       | IdP V5/tenant/local port                |
| `REQ-012`   | 主规格 §4              | RBAC 删除 tenant master/member 查询但保留 tenant-scoped authorization/policyVersion | 无 tenant CRUD/resolve/table；授权 FK 指向 state                               | RBAC V8/backend/Web                     |
| `REQ-013`   | 主规格 §4              | RBAC 创建/启用主体前验证 IdP ACTIVE membership                                        | missing/disabled/timeout 零写；RPC fail closed                              | Directory RPC/RBAC adapter              |
| `REQ-014`   | 主规格 §4              | 跨库迁移保留 ID/status/settings/membership，V8 前有完整门禁                               | counts/checksum/orphan/placeholder/FK 全通过才进入 V8                          | migration script/runbook/rehearsal      |
| `REQ-015`   | 主规格 §4              | 历史 Flyway immutable；IdP 仅 V5、RBAC 仅 V8、DDC 无 migration                       | checksum 不变；每 DB 恰一新版本                                                   | two migration Steps/static gate         |
| `REQ-016`   | 主规格 §4              | IdP Web 管理 Secret/tenant/member，RBAC Web 移除 tenant page                      | 权限、表单、确认和完整 UI states；Secret 不进存储/URL                                    | frontend Steps                          |
| `REQ-017`   | 主规格 §4              | USER OAuth/session/Signing Key 行为不变                                          | Authorization Code+PKCE、USER JWT/refresh、SigningKeyPage regression       | core/admin/gateway/frontend regression  |
| `REQ-018`   | 主规格 §4              | 切换后删除 private_key/JWK/Admission/旧 tenant 双轨并确定性报错                            | 旧 Token invalid_client；旧 API 404；旧配置拒绝启动                                 | delete/config/static gates              |
| `REQ-019`   | 主规格 §4/`DEC-006`    | 保留 `idp:oauth-client:read/create/update`                                     | backend/Web/permission seed 完全一致，无 `idp:client:*`                        | Client API/Web/RBAC seed                |
| `REQ-020`   | 主规格 §4/`DEC-007`    | membership port 不含 rbac3UserId，删除旧未接线 USER Resource policy/port              | reduced record 编译；旧 symbol/test 归零                                       | IdP core/admin tests                    |
| `REQ-021`   | 主规格 §4/`DEC-008`    | RBAC bootstrap 仅收 tenantId+identitySub，RPC ACTIVE 后建授权事实                     | CLI/config 无 tenantCode；inactive/timeout/malformed 零写                    | RBAC bootstrap/backend/tests            |

## 4. Implementation Strategy and Dependency Order

### 4.1 Ordered strategy

1. 先固化 IdP V5 schema contract，因为 Client Secret、Grant context 与 IdP tenant entity 都依赖它；V5 只在 rehearsal
   中执行，执行中间提交不得部署。
2. 先扩展 IdP core claim/principal 与删除过期 USER Resource policy，再让 admin Token Endpoint、starter、Gateway/DDC 消费同一
   `ServiceTokenContext`。
3. Client Secret 管理先于 Token Endpoint；Token Endpoint 先于 starter；starter 先于 DDC registration runtime，形成单向
   `DDC registration starter -> idp-starter`，并移除旧反向 admission 依赖。
4. IdP tenant backend 先于 Identity Directory RPC；RPC contract/provider 先于 RBAC membership gate/bootstrap。
5. RBAC V8 schema/state 先于 policyVersion consumer refactor；所有 tenant master code/HTTP/Web 删除在 V8 gate 可证明且
   RPC gate存在后进行。
6. DDC transport/client 改为 registrationToken 后，admin verifier/lease 才能接收新可信 identity；两者作为同一发布单元部署但保持两个可验证提交。
7. Backend contracts 完成后再改 IdP/RBAC Web；最后增加维护脚本、runbook 与全局删除/兼容门禁。

### 4.2 Test-first strategy

| Behavior               | RED test                                     | Expected RED reason                               | Minimum GREEN                           |
|------------------------|----------------------------------------------|---------------------------------------------------|-----------------------------------------|
| IdP V5                 | `IdpClientTenantV5MigrationTest`             | V5 resource/table/constraint/old-table-drop 缺失    | 单个 V5 SQL                               |
| SERVICE context/claims | core/starter claim tests                     | PLATFORM 被 tenant non-null guard 拒绝或缺 app/context | core enum/record/signer/verifier        |
| Secret lifecycle       | `OAuthClientServiceImplTest`/controller test | 无 appId/Secret repository/create/rotate result    | entity/repo/service/controller          |
| Basic Token            | authenticator/controller/service tests       | 仍需要 assertion/body client_id                      | Basic parser/hash auth + context policy |
| Spring Client          | facade/autoconfig tests                      | 无 OAuth2 Client dependency/beans/full cache key   | Boot-managed dependency + facade        |
| tenant/membership      | service/controller/local-port tests          | package/table/read model不存在                       | IdP feature package                     |
| Directory RPC          | proto/contract/provider tests                | method/message不存在                                 | additive proto/Java/provider            |
| RBAC V8/state          | migration/repository tests                   | table/FK/state entity不存在                          | single V8 + state repository            |
| RBAC gate/bootstrap    | user/bootstrap tests                         | inactive/timeout仍可写或 tenantCode创建 TenantPO        | RPC adapter + fail-closed orchestration |
| DDC Token registration | runtime/verifier/lease tests                 | Ticket supplier/claims仍是输入                        | registrationToken + verified identity   |
| Web                    | Testing Library tests                        | one-time modal/tenant routes/removed RBAC route缺失 | typed clients/pages/routes              |
| Operations             | shell contract test                          | artifact/checksum/gate commands缺失                 | one migration tool + runbook            |

### 4.3 Sequential and parallel boundaries

| Step | Depends on                 | May run in parallel with         | Must not overlap with               | Reason                           |
|------|----------------------------|----------------------------------|-------------------------------------|----------------------------------|
| 1    | None                       | None                             | IdP historical migrations           | 单一 V5 owner                      |
| 2    | Step 1 contract understood | None                             | IdP core claim files                | shared public model              |
| 3    | Step 1                     | None                             | IdP oauth Client aggregate          | Secret transaction owner         |
| 4    | Steps 2–3                  | None                             | Token/OAuth/JWK files               | authentication cutover atomicity |
| 5    | Step 4                     | Step 6 only after path isolation | idp-starter POM/autoconfig          | starter public beans             |
| 6    | Step 1                     | Step 5                           | IdP tenant persistence/service      | local DB owner                   |
| 7    | Step 6                     | None                             | tenant controller/local port/config | same feature contracts           |
| 8    | Steps 6–7                  | None                             | identity_directory proto/provider   | generated contract order         |
| 9    | Step 8 contract available  | None                             | RBAC V8/state files                 | database state owner             |
| 10   | Step 9                     | None                             | RBAC policyVersion repositories     | shared state mutation            |
| 11   | Steps 8–10                 | None                             | RBAC user/bootstrap/directory files | RPC-gated writes                 |
| 12   | Step 11                    | None                             | RBAC tenant/membership deletion set | consumers must be gone first     |
| 13   | Step 5                     | None                             | DDC starter/http runtime            | dependency inversion             |
| 14   | Steps 2,13                 | None                             | DDC admin security/lease            | trusted identity rename          |
| 15   | Steps 3–4                  | Step 17                          | IdP Client Web files                | backend contract fixed           |
| 16   | Step 7                     | Step 15                          | IdP tenant Web files                | backend contract fixed           |
| 17   | Steps 11–12                | Steps 15–16                      | RBAC Web/permission seed            | removed routes align backend     |
| 18   | All prior                  | None                             | scripts/runbook/final gate          | aggregates release evidence      |

### 4.4 Commit boundaries

每个 Step 只 stage 本 Step `Commit paths`，完成聚焦 GREEN 后提交；不得用 `git add .`。相同模块的后续 Step 只能在前一提交之后继续，不把
intentionally-red 工作提交。Flyway Step 的“可独立验证”指 migration rehearsal/contract test 通过，不授权把仅有
schema、尚无配套应用的中间提交部署。执行时使用本 Plan 给出的语义 commit message，并在每步提交前复核 dirty paths。

少量共享文件按不重叠symbol分步修改：`OAuthConfig` 在Step 4只处理Token/credential beans、Step 7只处理membership bean；IdP
Web `types.ts`/`App.test.tsx` 在Steps 15/16分别处理Client与tenant sections；`Rbac3DevelopmentTopologyTest`和
`JpaPlatformAdminBootstrapRepository`在Step 11处理external-ID bootstrap、Step 17只处理permission
facts。执行时每个Step只stage该symbol范围，后一步基于前一步commit继续；把这些修改合并成一个巨型commit会破坏独立RED/GREEN与rollback点。

### 4.5 Spec Simplicity and Implementation-necessity Audit

| Spec element | Spec necessity verdict/section | Current repository evidence | Direct/reuse alternative                               | Interaction/implementation cost    | Plan decision                                  |
|-----------------------------------------------|--------------------------------|-----------------------------------------------------------------|--------------------------------------------------------|------------------------------------|------------------------------------------------|
| Client Secret history table                   | Add；主规格 §7.0/§11.2.2           | Client 表无 Secret，轮换需 credential id/history                      | Client 单列不能表达一 active、revoke audit、CAS                 | one table/entity/repo/migration    | Implement                                      |
| `client_secret_basic` authenticator           | Add/replace；§9 API-003         | `PrivateKeyJwtAuthenticator` + JWK store                        | 扩展旧 assertion 会保留双轨与密钥代码                               | one hash lookup；删 replay/JWK       | Implement direct authenticator；不加 Strategy     |
| `ServiceTokenContext`                         | Add；§10                        | current tenant non-null implicit context                        | magic null/star tenant不能防提升                            | enum/claim/validator branches      | Implement                                      |
| Spring `OAuth2AuthorizedClientManager` facade | Add；§9 INTERNAL-001            | starter 无 OAuth2 Client manager                                 | custom HTTP supplier 会重复标准缓存/认证                        | dependency + facade/key/autoconfig | Implement Adapter/Facade composition           |
| IdP tenant/member tables/API                  | Add；§7/§9/§11                  | IdP 反向调用 RBAC membership                                        | 保留 RBAC owner违反 2A                                     | two tables, API/page/RPC           | Implement                                      |
| membership lookup HTTP preflight              | Remove；§7.0/`DEC-007`          | `HttpTenantMembershipAdapter` only supports old dependency      | local DB/service can derive directly                   | removes RTT/config/failure         | Delete；不替代 fetch-then-forward API              |
| Directory RPC membership method               | Expand；§9 RPC-001              | existing IdP→RBAC directory contract/provider/consumer          | 新 HTTP 或 RBAC copy都会增加协议/双权威                           | additive RPC method/deadline       | Implement on existing service/version          |
| RBAC authorization state                      | Add；§11.2.8                    | `TenantPO` mixes catalog/policyVersion                          | 删除整表会丢 cache version                                   | one table/entity/repo/FK retarget  | Implement                                      |
| RBAC bootstrap external IDs                   | Modify；`DEC-008`               | current CLI/config tenantCode creates TenantPO                  | 保留 tenantCode copy违反 2A                                | one RPC gate, CLI/config break     | Implement                                      |
| DDC Admission Ticket                          | Remove；§7.0/`DEC-003`          | Ticket repeats app/source/scope/expiry and no workload identity | DDC audience PLATFORM Token derives same facts         | fewer RPC/cache states             | Delete；no compatibility adapter                |
| DDC registration Token verifier               | Modify/rename；§9 INTERNAL-002  | existing JWT/source/lease checks reusable                       | new second verifier duplicates logic                   | Java semantic rename + claim rules | Implement by adapting existing verifier        |
| IdP tenant Web                                | Add；§12                        | no current tenant page                                          | CLI-only does not meet Web requirement                 | route/page/types/tests             | Implement direct page；no global store          |
| RBAC tenant Web                               | Remove；§12                     | `/iam/tenants` is local target-context/catalog surface          | forwarding user to IdP would create cross-app coupling | delete route/page/api              | Delete                                         |
| Cross-DB migration service/job                | Remove candidate；§7.0/§16      | no shared transaction/DataSource                                | offline script + report satisfies one-time cutover     | avoids permanent runtime/job       | Implement one operator script, not service/job |

### 4.6 Change-unit Dependency Matrix

| Change unit | Requirements | Proof/RED point | Compile/runtime prerequisites | Produces                                       | Consumers/unblocks | Owning Step |
|--------------------------|-----------------------------------------------------------------------|------------------------------|-------------------------------|------------------------------------------------|--------------------|-------------|
| IdP V5                   | `REQ-002`,`REQ-003`,`REQ-008`,`REQ-011`,`REQ-014`,`REQ-015`,`REQ-018` | migration test               | V1–V4 fixtures                | Client Secret/context/tenant schema            | Steps 3,4,6        | Step 1      |
| SERVICE core             | `REQ-007`,`REQ-008`,`REQ-017`,`REQ-020`                               | core claim/deletion tests    | current core                  | context/app claims + reduced membership port   | Steps 4,5,14       | Step 2      |
| Secret admin             | `REQ-001`–`REQ-003`,`REQ-019`                                         | service/controller RED       | V5                            | create/rotate/read-safe contracts              | Steps 4,15         | Step 3      |
| Token Endpoint           | `REQ-004`,`REQ-007`,`REQ-008`,`REQ-017`,`REQ-018`                     | OAuth tests                  | Steps 2–3                     | Basic-only SERVICE Token                       | Steps 5,13,14      | Step 4      |
| OAuth2 Client facade     | `REQ-005`,`REQ-006`,`REQ-018`                                         | starter tests                | Step 4                        | standard token acquisition                     | Step 13            | Step 5      |
| IdP tenant service/API   | `REQ-011`,`REQ-016`,`REQ-020`                                         | service/controller tests     | V5                            | local authority/read model                     | Steps 7,8,16       | Steps 6–7   |
| membership RPC           | `REQ-013`,`REQ-021`                                                   | contract/provider tests      | Step 6                        | ACTIVE profile method                          | Steps 9,11         | Step 8      |
| RBAC V8/state            | `REQ-012`,`REQ-014`,`REQ-015`                                         | migration/state tests        | imported IdP gate artifact    | auth-state/FK target                           | Steps 10–12        | Step 9      |
| RBAC consumers/bootstrap | `REQ-012`,`REQ-013`,`REQ-019`,`REQ-021`                               | repository/bootstrap tests   | Steps 8–10                    | external-ID authorization runtime              | Steps 12,17        | Steps 10–11 |
| RBAC owner removal       | `REQ-012`,`REQ-018`,`REQ-020`                                         | compile/404/static tests     | Step 11                       | no tenant/member owner                         | Step 17            | Step 12     |
| DDC client/server        | `REQ-009`,`REQ-010`,`REQ-018`                                         | runtime/verifier/lease tests | Steps 2,5                     | Token-bound registration/lease                 | Step 18            | Steps 13–14 |
| Admin Web                | `REQ-003`,`REQ-016`,`REQ-019`                                         | component tests              | backend contracts             | one-time Secret/IdP tenant/no RBAC tenant page | Step 18            | Steps 15–17 |
| Cutover tooling          | `REQ-014`,`REQ-015`,`REQ-017`,`REQ-018`                               | shell contract + full gates  | all Steps                     | artifact/report/runbook                        | release review     | Step 18     |

## 5. Change File Tree

```text
egon-cola-xingyuan/
├── egon-cola-tianquan-shoubing/
│   ├── egon-cola-tianquan-shoubing-core/                 MODIFY claims/principal/ports; DELETE stale USER/Admission/JWK contracts
│   ├── egon-cola-tianquan-shoubing-admin/                CREATE V5, Secret + tenant persistence/services; MODIFY Token; DELETE JWK/Admission/HTTP membership
│   ├── egon-cola-tianquan-shoubing-rpc-contract/         MODIFY identity_directory; DELETE admission proto/contract
│   ├── egon-cola-tianquan-shoubing-starter/              CREATE OAuth2 client facade; DELETE private-key/Ticket runtime
│   └── egon-cola-tianquan-shoubing-admin-web/            MODIFY Client/Resource; CREATE tenant page
├── egon-cola-tianquan-jianshen/
│   ├── egon-cola-tianquan-jianshen-admin/              CREATE V8/auth-state/RPC adapter; MODIFY bootstrap/repositories; DELETE tenant/member owner
│   └── egon-cola-tianquan-jianshen-admin-web/          DELETE tenant page/API/route; MODIFY conformance resources
└── egon-cola-tianshu/
    ├── egon-cola-tianshu-starter/ MODIFY registration models; DELETE Ticket port/model
    ├── egon-cola-tianshu-http-registration-starter/ MODIFY to IdpServiceOAuth2Client
    └── egon-cola-tianshu-admin/ RENAME verifier values; MODIFY lease/repository/config; no migration
scripts/unified-xingyuan/                              CREATE one-shot tenant authority tool + contract test
docs/runbooks/                                         CREATE cutover runbook
```

| Operation            | Path/file set                                                                                                                                                                                                                                                    | Current evidence/symbol                                        | Final symbols/state                                                 | Responsibility              | Step | Requirements                                                          | Validation owner                 |
|----------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|----------------------------------------------------------------|---------------------------------------------------------------------|-----------------------------|------|-----------------------------------------------------------------------|----------------------------------|
| CREATE               | `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/resources/db/migration/V5__adopt_client_secrets_and_tenant_authority.sql`                                                                                                      | IdP V1–V4                                                      | V5 schema/data contract                                             | IdP schema cutover          | 1    | `REQ-002`,`REQ-003`,`REQ-008`,`REQ-011`,`REQ-014`,`REQ-015`,`REQ-018` | `IdpClientTenantV5MigrationTest` |
| MODIFY/CREATE        | `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-core/src/main/java/top/egon/cola/platform/idp/{contract,core}`                                                                                                                                | existing SERVICE claims/principal and old USER/Admission ports | explicit context/app claims; reduced membership; stale paths absent | stable security model       | 2    | `REQ-007`,`REQ-008`,`REQ-017`,`REQ-020`                               | core/starter tests               |
| MODIFY/CREATE        | `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/idp/admin/oauth`                                                                                                                                   | Client CRUD without Secret                                     | appId + one-time create/rotate + hash-only repo                     | Client credential lifecycle | 3    | `REQ-001`–`REQ-003`,`REQ-019`                                         | OAuth service/controller tests   |
| MODIFY/DELETE        | `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/idp/admin/{oauth,token,resource}`                                                                                                                  | private_key/JWK/Admission Token path                           | Basic-only Token and target context claims                          | OAuth protocol cutover      | 4    | `REQ-004`,`REQ-007`,`REQ-008`,`REQ-017`,`REQ-018`                     | OAuth component tests            |
| MODIFY/CREATE/DELETE | `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter`                                                                                                                                                                                      | Resource Server + Admission Ticket starter                     | Resource Server + Spring OAuth2 Client facade                       | biz service integration     | 5    | `REQ-005`,`REQ-006`,`REQ-018`                                         | starter tests                    |
| CREATE               | `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/idp/admin/tenant`                                                                                                                                  | absent                                                         | tenant/member persistence + service                                 | IdP authority               | 6    | `REQ-011`,`REQ-020`                                                   | tenant service tests             |
| MODIFY/CREATE/DELETE | `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/idp/admin/{tenant,support,oauth/config}`                                                                                                           | RBAC HTTP membership adapter                                   | IdP tenant HTTP + local port                                        | IdP authority exposure      | 7    | `REQ-011`,`REQ-016`,`REQ-020`                                         | controller/local-port tests      |
| MODIFY/DELETE        | `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-rpc-contract`；`egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/idp/admin/identity/support/rpc/IdentityDirectoryRpcProvider.java` | profile RPC + Admission RPC                                    | additive membership RPC; Admission contract absent                  | IdP→RBAC identity fact      | 8    | `REQ-013`,`REQ-018`,`REQ-021`                                         | RPC contract/provider tests      |
| CREATE               | `egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin/src/main/resources/db/migration/V8__externalize_tenant_authority.sql`；`.../iam/authorizationstate`                                                                                  | `rbac3_tenant.policy_version`                                  | external tenant auth-state/FK target                                | RBAC schema ownership       | 9    | `REQ-012`,`REQ-014`,`REQ-015`                                         | V8 migration/state tests         |
| MODIFY               | `egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin/src/main/java/top/egon/cola/platform/rbac3/admin/iam/{role,policy}/repository`；`.../simulation/repository`                                                                          | TenantPO policy version reads/locks                            | authorization-state reads/locks                                     | policyVersion compatibility | 10   | `REQ-012`                                                             | repository tests                 |
| MODIFY/CREATE        | `egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin/src/main/java/top/egon/cola/platform/rbac3/admin/{bootstrap,iam/user,iam/organization}`                                                                                             | tenantCode/TenantPO bootstrap and unchecked subject writes     | RPC ACTIVE gate + external tenantId                                 | orphan prevention/bootstrap | 11   | `REQ-013`,`REQ-019`,`REQ-021`                                         | user/bootstrap tests             |
| DELETE/MODIFY        | `egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin/src/main/java/top/egon/cola/platform/rbac3/admin/iam/{tenant,user,organization}`                                                                                                    | tenant/member CRUD/resolve                                     | tenant-scoped authorization only                                    | old owner removal           | 12   | `REQ-012`,`REQ-018`,`REQ-020`                                         | compile/404/static gates         |
| MODIFY/DELETE        | `egon-cola-xingyuan/egon-cola-tianshu/{egon-cola-tianshu-starter,egon-cola-tianshu-http-registration-starter}`                                                                             | Ticket supplier/input                                          | registrationToken from IdpServiceOAuth2Client                       | registration caller         | 13   | `REQ-005`,`REQ-009`,`REQ-010`,`REQ-018`                               | DDC runtime tests                |
| RENAME/MODIFY        | `egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/java/top/egon/cola/component/ddc/admin`                                                                                                    | Admission claims/verifier/lease naming                         | verified SERVICE identity + Token-bound lease                       | registration server         | 14   | `REQ-007`,`REQ-009`,`REQ-010`,`REQ-018`                               | DDC admin tests                  |
| MODIFY/CREATE        | `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin-web/src/{api,features/clients,features/resource-servers,features/resource-grants}`                                                                                                      | Client/JWK UI                                                  | AppID/Secret one-time + context Grants, no Client JWK               | Client operator UX          | 15   | `REQ-003`,`REQ-016`,`REQ-019`                                         | Vitest/typecheck/build           |
| MODIFY/CREATE        | `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin-web/src/{api,app,features/tenants,features/overview}`                                                                                                                                   | no tenant route                                                | tenant/member page                                                  | tenant operator UX          | 16   | `REQ-011`,`REQ-016`                                                   | Vitest/typecheck/build           |
| MODIFY/DELETE        | `egon-cola-xingyuan/egon-cola-tianquan-jianshen/{egon-cola-tianquan-jianshen-admin-web,egon-cola-tianquan-jianshen-admin/src/main/java/top/egon/cola/platform/rbac3/admin/bootstrap/domain/Rbac3DevelopmentTopology.java}`                                               | RBAC tenant page/permissions                                   | no tenant page; IdP tenant permissions seeded                       | frontend/permission cleanup | 17   | `REQ-012`,`REQ-016`,`REQ-019`,`REQ-021`                               | RBAC Web/topology tests          |
| CREATE/MODIFY        | `scripts/unified-xingyuan`；`docs/runbooks/unified-identity-oauth-client-tenant-cutover.md`；module READMEs/config                                                                                                                                                 | no one-shot verified cutover tool                              | export/import/gate/report/runbook/static deletion                   | operations/release evidence | 18   | `REQ-014`,`REQ-015`,`REQ-017`,`REQ-018`                               | shell/full reactor/manual gates  |

## 6. Prerequisites, Constraints, and Plan Clarifications

### 6.1 Repository and worktree baseline

- Applicable instructions: user-supplied root `AGENTS.md`; repository内未发现适用于本 root 的额外 `AGENTS.md`
  。执行后不得自动启动项目；每 Step 一个 commit。
- Branch/commit: `main@772df2b2`；Step 1=`913179c3`、Step 2=`8c83e361`（含 `5692763f`、`c5a537fa` 纠偏）、Step 3=`b374559b`、Step 4=`f2fd8230`、Step 5=`35d1879d`、Step 6=`39023dfb`、Step 7=`fd31f5ba`、Step 8=`08f3d8c6`、Step 9=`06dd03cd`、Step 10=`d72ad57d`、Step 11=`c52fe991`、Step 12=`551d834e`、Step 13=`b045c318`、Step 14=`233eb901`、Step 15=`20d19d50`、Step 16=`8bd5ea76`、Step 17=`323db68a`、Step 18=`772df2b2` 已完成；当前仅主 Spec 与本 Plan 两份文档有未提交修改。
- 相对旧基线 `0b7b9b3a`，目标模块唯一代码差异是 RBAC `IdentityProfileDirectory` 显式采用
  `RpcReferenceMode.GATEWAY`；它保持既有 IdP→RBAC RPC 依赖方向，不改变本 Plan 的合同、Step 顺序或写范围。
- 执行时重新检查 `git status --short`；任何届时出现的无关 staged、unstaged 或 untracked 路径均不得纳入 Step commit。
- Protobuf generated sources 位于 Maven `target/generated-sources`，只修改 `.proto`/Java contract source，不提交
  `target/`。
- Flyway V1–V4/V1–V7 历史文件 immutable；只允许创建主规格命名的 IdP V5 和 RBAC V8；DDC migration 文件不变。

### 6.2 Build, test, and environment prerequisites

| Concern              | Exact command/source                                                                                                         | Required state                                                                    | Validation boundary                          |
|----------------------|------------------------------------------------------------------------------------------------------------------------------|-----------------------------------------------------------------------------------|----------------------------------------------|
| Java/Maven           | repo-root `./mvnw -B -ntp -pl affected-module-path -am -Dsurefire.failIfNoSpecifiedTests=false -Dtest=FocusedTestClass test` | JDK 21；Maven wrapper可执行                                                           | source/module；不是 live topology               |
| IdP Web              | `npm test -- --runInBand` 不适用；实际脚本为 `npm test -- <selector>`、`npm run typecheck`、`npm run build`                             | 在 IdP admin-web 执行 `npm ci` 后                                                     | component/static bundle；不是浏览器手工              |
| RBAC Web             | `npm test -- <selector>`、`npm run typecheck`、`npm run build`、`npm run lint`                                                  | 在 RBAC admin-web 执行 `npm ci` 后                                                    | component/static bundle                      |
| PostgreSQL rehearsal | IdP/RBAC Testcontainers/Flyway integration tests                                                                             | Docker 可用；与目标 PostgreSQL/Flyway major 一致                                          | rehearsal；不证明生产数据分布/锁时长                      |
| Shell tooling        | `bash scripts/unified-xingyuan/test-tenant-authority-migration.sh`                                                           | `bash`、`psql` client fixtures/mocks per script                                    | deterministic artifact contract；真实 DB 仍需人工窗口 |
| Live release         | §9 runbook steps                                                                                                             | IdP/RBAC backups、maintenance freeze、Secret manager、Redis/DDC/Gateway environments | 用户控制；本 Plan 不执行                              |

### 6.3 Immutable constraints and approved decisions

- `appId` 是业务应用身份，App Key 是 `client_id`，Secret 是 `client_secret`；Secret hash-only/one-time/plaintext not
  recoverable。
- Token Endpoint 仅 `client_secret_basic`；不保留 assertion/JWK/Admission 双轨。
- PLATFORM/TENANT 由 Grant 派生，caller 不提交 context；DDC 只接受 DDC audience PLATFORM + 最小 scope。
- tenant catalog/membership 只在 IdP；RBAC 只保留外部 tenantId 分区的 authorization state/facts。
- Client 权限保留 `idp:oauth-client:read/create/update`；membership port 无 `rbac3UserId`；bootstrap 仅外部
  tenantId+identitySub + RPC ACTIVE gate。
- 发布是维护窗口原子兼容单元；V8 后禁止只回滚一个服务或一个数据库。

### 6.4 Plan Clarifications

| ID              | Small implementation inference                                                                     | Repository evidence                                       | Why semantics are unchanged                           | Impact if wrong                                                                 |
|-----------------|----------------------------------------------------------------------------------------------------|-----------------------------------------------------------|-------------------------------------------------------|---------------------------------------------------------------------------------|
| `PLAN-CLAR-001` | `IdentityDirectoryRpc.VERSION` 保持 `1.0.0`，在现有 `identity_directory.proto` additive 增 method/message | 当前 contract/version + 主规格明确“扩展既有 RPC、旧 client 可忽略”        | 不改变路由 identity，仅发布向后兼容 method                         | 若 RPC registry 要求每次 additive bump，Step 8 需同步 provider/reference cutover         |
| `PLAN-CLAR-002` | 新 IdP tenant Java 文件放在 feature-first `admin.tenant.{controller,service,service.impl,repo,domain}`  | IdP oauth/identity/resource 现有结构；主规格 §6.1 要求保留            | 只决定内部路径/命名，不改变 API/schema                             | 路径不同只影响 imports/Plan inventory                                                  |
| `PLAN-CLAR-003` | Spring OAuth2 Client 使用 Boot dependency management，不写显式版本；facade 放 `idp.starter.client`            | starter POM 统一依赖管理 + 主规格目标树                               | 标准依赖/包 placement，不改变 token contract                   | 若父 POM未管理依赖，需在 platform parent补受控版本而非局部硬编码                                      |
| `PLAN-CLAR-004` | DDC Java security package从 `admission` rename为 `registration`，Redis/SQL已有 `admission_*` 物理字段本期保持   | 主规格 `ASM-004`；DDC schema无 migration                       | 只消除 Java语义歧义，持久数据/键兼容不变                               | 若 serialization 直接依赖 Java property name，Step 14要加 explicit legacy field mapping |
| `PLAN-CLAR-005` | 一次性 tenant cutover 工具为单脚本多 subcommand，放 `scripts/unified-xingyuan`，runbook 放 `docs/runbooks`       | 现有 unified-platform scripts/common.sh 与 runbook locations | one-shot operations placement，不增加 runtime service/job | 若运维要求独立 artifact signing binary，可后续 amendment替换脚本实现                             |
| `PLAN-CLAR-006` | Step 11 的 IdP membership RPC consumer 沿用 `@EgonRpcReference(mode = RpcReferenceMode.GATEWAY, ...)` | 当前 `IdentityProfileDirectory` 在 `2bb92624` 已显式采用该模式        | 仅同步当前 RPC reference 注解惯例，不改变 RPC identity、deadline 或 fail-closed 语义 | 若目标部署要求其他 mode，必须回到 Spec 重新确认 topology，而不能在执行时静默切换                         |
| `PLAN-CLAR-007` | Step 2 同步修改既有 core `ClientResourceGrant`，让 V5 `grant_context` 进入领域模型并表达 nullable tenant invariant | Spec §7.3.3/§10.3 要求 Grant 派生 `TENANT/PLATFORM`；当前 domain 只有 tenantId 且禁止 null | 这是已批准 Grant/context 合同的遗漏路径补录，不新增业务决策或改变 Step 顺序 | 若未补录，Step 4 会需要未声明的 Grant/entity/store consumer 改动，违反执行器 path gate |
| `PLAN-CLAR-008` | Step 3 Secret rotation route 采用主 Spec API-002 的 `/secret-rotations`，修正早期摘要中的 `/secrets/rotate` shorthand | 主 Spec §9.2.2/接口 inventory 定义精确 route；现有 controller 尚无 rotation route | 仅统一 Plan 与已批准 HTTP contract，不改变业务行为或 Step 顺序 | 若保留 shorthand，controller test/Web client 会与 Spec path 漂移 |
| `PLAN-CLAR-009` | Step 3 将现有 `SpringConstructorResolutionTest` 纳入 service test block，覆盖新增 Secret repository/hash/random 构造依赖 | 该测试已验证 `OAuthClientServiceImpl` 生产构造器；新 Secret lifecycle 仍需同一 Spring wiring 回归 | 仅补齐既有构造器回归的声明路径，不新增架构或 Step | 若遗漏，Step 3 admin module compile/context gate 会出现未声明测试漂移 |
| `PLAN-CLAR-010` | Step 3 同步修改既有 `IdentityClientRepository`，增加按 Client ID 的 `PESSIMISTIC_WRITE` 锁查询，轮换时与 active Secret 锁在同一事务内执行 | 主 Spec API-002 要求锁定 Client/active Secret 并以 expectedVersion 做 CAS；当前仓储只有继承的无锁 `findById` | 只补录已批准并发轮换实现所需的 repository 入口，不改变数据模型、API 或 Step 顺序 | 若遗漏，轮换只能锁 Secret，Client version 并发 CAS 可能在 service 层失去数据库互斥证明 |
| `PLAN-CLAR-011` | Step 4 File 7 同步修改 `JpaResourceServerStore` 与 `ResourceServerProjectionService`，移除对已由 V5 删除的 admission-ticket TTL 字段的读取/发布 | V5 已删除 `identity_resource_server.admission_ticket_ttl_seconds`；当前 JPA→core 映射和 Redis projection 仍直接引用该列 | 只补录同一 Resource contract 删除所必需的两个下游映射路径，不新增 API、表或 Step 顺序 | 若遗漏，JPA 查询会访问不存在列，Redis 状态仍会传播退役 Admission 语义 |
| `PLAN-CLAR-012` | Step 4 同步修改 `IdpDevelopmentClientBootstrap.java` 与其测试，删除已物理移除的 Client JWK 初始化/依赖参数 | Bootstrap 仍在开发拓扑中装配 Client JWK repository、PEM key loader 与 credential projection；File 8 删除这些类型后会成为唯一编译消费者 | 只清理退役 JWK consumer，并保持既有 development Client/Resource/Grant reconciliation 语义；不提前改变 Step 7 tenant authority | 若遗漏，admin testCompile 会引用不存在的 JWK entity/repository，或 bootstrap 继续尝试写 V5 已删除表 |
| `PLAN-CLAR-013` | Step 4 同步修改 `IdpPersistenceEntityContractTest.java`，移除 Client JWK persistence fixture 并保留 Resource legacy factory contract | persistence contract test 直接构造已删除 `IdentityClientJwkEntity`，同时覆盖 Resource factory 的旧参数兼容 | 只让现有 contract test 对齐 V5/Step 4 删除边界，不新增持久化字段或迁移行为 | 若遗漏，admin testCompile 仍会引用删除的 JWK entity；若删除整个测试则丢失 Resource boundary coverage |
| `PLAN-CLAR-014` | 将 `ResourceServerAdmissionRpcProviderTest.java` 的物理删除前移到 Step 4，并从 Step 8 File 2 的删除清单移除 | Step 4 已删除 provider production class/core `AdmissionRequest`；该测试仍直接 import 两者，导致模块 testCompile 失败 | 该测试仅覆盖退役 Admission provider，无 replacement behavior；Step 8 仍删除剩余 Admission RPC contract test | 若遗漏，Step 4 reactor 无法编译；若 Step 8 重列已删除路径，后续 preflight/commit path gate 会失真 |
| `PLAN-CLAR-015` | Step 5 同步更新 starter 与 autoconfigure `package-info.java`，清除退役 Admission Ticket 叙述 | 两个 package documentation files 仍描述 Admission Ticket/startup admission，虽 production source 已删除旧实现 | 只同步模块文档与当前 OAuth2 Client facade，不改变 API、依赖或 runtime behavior | 若遗漏，静态旧语义审计仍会把 starter 文档误判为未完成迁移 |

### 6.5 Execution-resume record and operation preflight contract

- Step 1 state: `Committed`。提交 `913179c366e27fc88289babff3400ef4d12203d7` 仅包含本 Step 声明的两个 `CREATE`
  路径；续执行不得重做 Step 1、不得为它创建空提交，也不得把本 Plan/主 Spec 的未提交文档修改带入实现提交。
- Step 1 evidence: 聚焦命令
  `./mvnw -B -ntp -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=IdpClientTenantV5MigrationTest -Dsurefire.failIfNoSpecifiedTests=false test`
  通过（3 tests）；追加 `IdpMigrationIT,IdpClientTenantV5MigrationTest` 回归通过（7 tests）；V1–V4 静态 SHA-256
  检查通过。没有执行真实 PostgreSQL/Flyway 发布 rehearsal，因此不能把源码/模块测试表述为生产数据库证据。
- Resume rule: 从 Step 3 开始；每个未完成 Step 在编辑前都以当时 `HEAD` 重新核对本 Step 的虚拟起始状态：`CREATE`
  target 必须不存在，`MODIFY/DELETE` target 必须存在，`RENAME` source 必须存在且 target 必须不存在。任何不满足项先修订
  Plan，不得通过改换操作、扩大目录 staging 或空提交绕过。
- Sequential state rule: 文件存在性按 Step 顺序传递；前一步 `CREATE/DELETE/RENAME` 的结果必须成为下一步 preflight
  的输入。Step N 只有在验证通过且已形成唯一、路径受限提交后，才允许进入 Step N+1。
- Dirty-worktree rule: 当前两份文档修改属于 Plan/Spec 修复而非任一实现 Step；每次提交使用本 Step `Commit paths`
  的精确路径逐项暂存，并在 commit 前后核对 staged/unstaged/untracked 集合。
- Authoring audit evidence: 以 `main@913179c3` 枚举 Steps 2–18 共 79 个 operation blocks，当前基线存在性 mismatch
  为 0；按 Step 顺序应用 `CREATE/MODIFY/DELETE/RENAME` 后的虚拟文件系统 mismatch 为 0；所有 Maven `-Dtest`
  selector 在所属 Step 完成态均可解析到现有或本/前序 Step 创建的测试类；两套 Admin Web 所用 `test/lint/typecheck/build`
  scripts 均存在。该证据消除当前仓库可发现的 Plan drift，不替代每 Step 执行前的重新检查。

## 7. Ordered File-by-file Implementation Steps

> 每个 Step 先提交聚焦测试的 RED 变更，再在同一工作副本完成最小 GREEN；最终只提交 GREEN 状态。下列 file set
> 使用分号分隔多个必须连续处理的精确路径，集合内仍按书写顺序执行，不能用目录级批量重写替代。

### Step 1 — 固化 IdP V5 Client Secret 与 tenant authority schema

- Requirements: `REQ-002`, `REQ-003`, `REQ-008`, `REQ-011`, `REQ-014`, `REQ-015`, `REQ-018`
- Dependencies: None
- Execution state: `Committed` at `913179c366e27fc88289babff3400ef4d12203d7`; historical record only，续执行从
  Step 2 开始，不重放本 Step 或创建空提交。
- Baseline state: IdP migration 仅 V1–V4；`identity_client` 无 app_id/secret，Grant tenant 无
  context约束，tenant/member表不存在，JWK/Ticket TTL仍存在。
- Observable outcome: 从 V1 空库和 V4 代表数据均可唯一执行 V5，得到主规格 §11 的 Client Secret、Grant
  context、tenant/member结构，并确定性删除 client JWK/Ticket TTL。
- End state: V5 与 migration rehearsal test 可独立通过；业务 Entity/Token 代码尚未适配，禁止部署该中间提交。
- Test-first gate: Required — 新 migration test 因 V5 resource/目标表/约束不存在而 RED，不因 Docker fixture 或旧
  migration checksum失败。
- Ordered files:

#### File 1 —
`CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/idp/admin/support/migration/IdpClientTenantV5MigrationTest.java`

- Purpose: 定义 V4→V5、空库→V5、legacy Grant placeholder、JWK/TTL drop、约束/索引和历史 checksum 合同。
- Symbols: `migratesV4ClientAndTenantFacts`, `rejectsDuplicateAppOrActiveSecret`, `preservesHistoricalChecksums`。
- Repository evidence: 同目录 `IdpMigrationIT.java` 使用 Flyway/Testcontainers；V1–V4 提供真实 PostgreSQL fixture source。
- Dependencies and consumers: 读取全部 IdP migrations；不启动 Spring application；为 Steps 3/4/6 的 Entity/schema 映射提供
  proof。
- Why now: schema 是后续 persistence compile/runtime 的先决条件，且先定义 destructive gate 可阻止实现偏离。
- Contract/signature changes: 测试 fixture 写 legacy client/grant/JWK/resource rows，执行 Flyway 后查询主规格定义的
  columns/checks/FKs/indexes。
- Input/output and state mapping: legacy clientId→app_id placeholder/nullable policy；tenant grant→TENANT
  context+placeholder tenant；JWK/TTL→absent；counts/checksum→assertions。
- Error and edge behavior: duplicate appId、active Secret、invalid PLATFORM/tid、orphan identitySub/Grant必须被
  constraint/gate观察；V5重复执行由 Flyway history阻止而非脚本自行重放。
- Implementation pseudocode:

```java
prepare PostgreSQL with Flyway target 4 and representative PUBLIC/CONFIDENTIAL clients, grants, JWKs and identity users
migrate to latest; assert V5 exactly once, new tables/constraints/indexes and preserved client/tenant identifiers
attempt invalid duplicate/context/orphan writes and assert named PostgreSQL constraints reject without changing valid rows
compare V1–V4 resource checksums before and after the test and assert no historical file content changed
```

- Verification contribution: RED/GREEN selector `IdpClientTenantV5MigrationTest`；证明静态 migration contract，不证明生产锁时长。
- After this file: 测试编译但因 V5缺失/queries失败而 RED；fixture 与 Docker正常。

#### File 2 —
`CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/resources/db/migration/V5__adopt_client_secrets_and_tenant_authority.sql`

- Purpose: 在一个 immutable IdP migration中完成主规格 §11 的 schema/data transition。
- Symbols: `identity_client.app_id`、`identity_client_secret`、Grant `grant_context/tenant_id` check、`identity_tenant`、
  `identity_tenant_membership`、JWK/TTL drops。
- Repository evidence: V1/V2真实表/constraint names；Spec §11.2.1–§11.2.7给出列、索引、placeholder与验证顺序。
- Dependencies and consumers: PostgreSQL/Flyway 11；后续 JPA Entity、Token policy、tenant service；不访问 RBAC database。
- Why now: File 1固定正确性与约束名字后再写最小 DDL/DML。
- Contract/signature changes: 新建两张 authority表和Secret表；扩展 Client/Grant/Resource；删除 `identity_client_jwk` 与
  `admission_ticket_ttl_seconds`。
- Input/output and state mapping: legacy TENANT grant tenant_id 保持字符串值；placeholder为 INITIALIZING；appId legacy
  mapping遵守主规格；audit/version字段保持UTC/0初值。
- Error and edge behavior: DDL在 transaction内失败则V5不记入 history；无法转换的 tenantId、duplicate、orphan在 destructive
  drop前抛错；跨库真实导入不在Flyway中执行。
- Implementation pseudocode:

```sql
create identity_tenant and identity_tenant_membership with PK/FK/UK/status/version constraints, then seed deterministic INITIALIZING tenant rows for legacy TENANT grants
alter identity_client with app_id and create identity_client_secret with one-active partial unique index and hash/hint/audit columns
alter grants with grant_context and tenant/context checks, validate all legacy rows, then drop identity_client_jwk and admission_ticket_ttl_seconds
leave real tenant/member import to the signed offline artifact and expose verification queries through the Step 18 tool
```

- Verification contribution: 使 File 1 GREEN，并为 Step 3/6 persistence integration提供 schema。
- After this file: migration contract GREEN；无 production Entity改动，repository不能作为可部署版本。

- Validation working directory: `/Users/mario/SelfProject/Egon-COLA`
- Verification command:
  `./mvnw -B -ntp -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=IdpClientTenantV5MigrationTest -Dsurefire.failIfNoSpecifiedTests=false test`
- Expected result: 已实现证据为 exit 0、3 个 migration source-contract tests、追加 7 个 IdP migration
  regression tests 与 V1–V4 静态 SHA-256 不变；没有执行真实 PostgreSQL/Flyway rehearsal，生产数据、锁时长和目标环境
  Flyway history 留在 Step 18 用户控制门禁。
- Failure returns to: File 1 若fixture/断言与当前V1/V2名字不符；File 2
  若DDL顺序、constraint、backfill或drop错误；主规格若真实历史数据无法满足已批准转换规则。
- Completion criteria: IdP仅新增一个V5，历史 migration无diff，schema/data gate覆盖 `REQ-002/003/008/011/014/015/018`。
- Rollback: 实施前可revert本Step两路径；已执行V5的环境只能按§9 snapshot restore/forward-fix，不能编辑V5或Flyway repair。
- Commit paths: `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/idp/admin/support/migration/IdpClientTenantV5MigrationTest.java`,   `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/resources/db/migration/V5__adopt_client_secrets_and_tenant_authority.sql`
- Commit: `feat(idp): add client secret and tenant authority migration`

### Step 2 — 建立显式 SERVICE context 并移除 RBAC ID 泄漏的旧 core policy

- Requirements: `REQ-007`, `REQ-008`, `REQ-017`, `REQ-020`
- Dependencies: Step 1
- Execution state: `Committed` at `8c83e3610584f43b2949be6153fa3d92d452e7fc`；续执行不得重做本 Step 或创建空提交。
- Baseline state: SERVICE claims/principal要求非空 tenant，credential注释仍是JWK kid；membership record含 `rbac3UserId`；旧
  USER Resource policy/authorization port仅测试引用。
- Observable outcome: IdP core可构造严格 TENANT/PLATFORM SERVICE identity，membership port不暴露RBAC ID，旧未接线 USER
  Resource policy/port完全删除。
- End state: core public models/claim names确定；signer/verifier/admin Token尚在下一Step适配，core focused tests GREEN。
- Test-first gate: Required — core tests先证明 PLATFORM constructor、context/tid矛盾与旧 symbol absence 当前失败。
- Ordered files:

#### File 1 —
`MODIFY egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-core/src/test/java/top/egon/cola/platform/idp/core/resource/ClientCredentialsAccessPolicyTest.java; egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-core/src/test/java/top/egon/cola/platform/idp/core/resource/ResourceServerPolicyTest.java; egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-core/src/test/java/top/egon/cola/platform/idp/core/token/TokenFacadeTest.java; egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/idp/admin/oauth/controller/OAuthResourceSecurityMatrixIT.java`

- Purpose: 先定义 Grant-derived context矩阵、reduced membership port与旧 USER/Admission policy测试删除边界，并同步跨模块旧矩阵测试。
- Symbols: `authorizesTenantAndPlatformContexts`, `rejectsContextTenantContradictions`, `FakeMembership` reduced
  constructor；删除 USER Resource/Admission专用cases。
- Repository evidence: 四个测试已覆盖 Client policy、Resource policies、TokenFacade membership 与 admin security matrix；现有 fixture 可扩展而无需新测试框架。
- Dependencies and consumers: core production symbols 与 admin matrix；保护 USER TokenFacade/Resource boundary 的 `REQ-017`。
- Why now: 测试先固定 public record/enums和删除范围，防止为兼容旧测试保留 rbac3UserId；旧 admin matrix 必须在 core deletion 同一提交清理。
- Contract/signature changes: assertions要求 PLATFORM `tenantId==null`、TENANT非空非星号、
  `appId/credentialId/scopeContext`必填；membership只有 identity/tenant/display/status。
- Input/output and state mapping: Grant type+tenant→context/tid；fake membership→TokenFacade login result；旧 policy
  tests被删除而非改成新RBAC call。
- Error and edge behavior: null context、PLATFORM+tid、TENANT-null、blank appId/credential、duplicate scopes全部
  `IllegalArgumentException` 或稳定 authorization error。
- Implementation pseudocode:

```java
construct TENANT access from a tenant grant and assert context TENANT with exact tenant id and normalized scopes
construct PLATFORM access from an explicit platform grant and assert context PLATFORM with null tenant and no magic wildcard
instantiate TokenFacade membership fixtures without rbac3UserId and keep active/disabled membership login assertions unchanged
remove assertions whose only target is the production-unwired USER Resource authorization and Admission policy path
```

- Verification contribution: RED for new model rules；GREEN proves USER token facade preserved and obsolete tests absent。
- After this file: tests intentionally RED against current records/policies，且无fixture故障。

#### File 2 —
`CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-core/src/main/java/top/egon/cola/platform/idp/contract/ServiceTokenContext.java`

- Purpose: 新建唯一 SERVICE context enum，先固定 TENANT/PLATFORM 的共享类型边界。
- Symbols: `ServiceTokenContext.TENANT/PLATFORM`。
- Repository evidence: 当前仓库不存在该类型；主规格 §7.3.3/§10.3要求签发端与消费端共享同一 context 语义。
- Dependencies and consumers: `Rs256TokenService`、`IdpJwtVerifier`、Gateway、DDC、starter；这些消费者在Steps 4/5/14适配。
- Why now: public/shared types必须先于签发端与消费端编译改造。
- Contract/signature changes: 新增两个且仅两个 enum values；不在 enum 内承载 tenantId 或 caller input。
- Input/output and state mapping: Grant 权威语义映射为 `TENANT` 或 `PLATFORM`；具体 tid 约束由 File 3 records 执行。
- Error and edge behavior: 不增加 UNKNOWN/wildcard 兼容值；新增 context 必须回到 Spec，而不是执行时静默扩展。
- Implementation pseudocode:

```java
declare enum ServiceTokenContext with exactly TENANT and PLATFORM in the shared contract package
use the enum as the only context type for claims, principals and authorization results; do not add string aliases
keep tenant presence validation outside the enum so callers cannot manufacture context by parsing arbitrary input
```

- Verification contribution: 提供 context tests 的编译锚点与 Steps 4/14 共享 contract。
- After this file: enum 可编译；现有 records/policy 尚未消费，tests 仍 RED。

#### File 3 —
`MODIFY egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-core/src/main/java/top/egon/cola/platform/idp/contract/IdpClaimNames.java; egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-core/src/main/java/top/egon/cola/platform/idp/contract/IdpPrincipal.java; egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-core/src/main/java/top/egon/cola/platform/idp/contract/ServiceIdentityPrincipal.java; egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-core/src/main/java/top/egon/cola/platform/idp/core/token/ServiceAccessTokenClaims.java; egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-core/src/main/java/top/egon/cola/platform/idp/core/resource/ClientResourceGrant.java; egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-core/src/main/java/top/egon/cola/platform/idp/core/resource/ClientCredentialsAccessPolicy.java`

- Purpose: 修改既有 claim/principal/Grant/policy，让 SERVICE identity 携带 app/context 并由 Grant 派生权威 context。
- Symbols: `APP_ID`、`SCOPE_CONTEXT`、records 新增 `appId/scopeContext`、`ClientResourceGrant.scopeContext`、policy `ServiceResourceAccess`。
- Repository evidence: 六个文件在续执行基线均已存在；当前 claim/principal/Grant/policy 正集中于这些路径，Grant 尚无 `grant_context` 映射。
- Dependencies and consumers: File 2 enum；`Rs256TokenService`、`IdpJwtVerifier`、Gateway、DDC、starter 在后续 Steps 适配；admin persistence mapping 留在后续声明的 resource paths。
- Why now: 共享 enum 建立后原地演进既有 public records，不另建平行 principal/claims 模型。
- Contract/signature changes: `tenantId` 对 PLATFORM nullable；SERVICE records 新增 appId/context；Grant 显式保存 `scopeContext` 并由 policy 判定 context。
- Input/output and state mapping: OAuthClient.appId+source Resource+Grant→ServiceResourceAccess→claims/principal；context 精确控制 tid presence，禁止用 wildcard tenant 模拟。
- Error and edge behavior: records 构造即 fail closed；PLATFORM 不得序列化 tid，TENANT 不得缺 tid；app/client subject 不互相替代。
- Implementation pseudocode:

```java
add APP_ID and SCOPE_CONTEXT claim constants and thread typed ServiceTokenContext through existing SERVICE claim, principal and Grant records
validate appId, clientId, source, credential and context before enforcing TENANT iff tenantId is present and never a wildcard
authorize from one active grant, derive context and tenant only from that grant, and return immutable normalized scopes and versions
allow a PLATFORM grant to carry null tenantId while rejecting TENANT grants without a concrete tenantId at domain construction
```

- Verification contribution: makes context/Grant tests GREEN；提供 Steps 4/14 compile contract。
- After this file: core model/policy GREEN；admin/starter consumers remain for their declared Steps。

#### File 4 —
`MODIFY egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-core/src/main/java/top/egon/cola/platform/idp/core/port/TenantMembershipPort.java`

- Purpose: 从既有 IdP membership boundary 移除 RBAC 内部 ID，同时保留 list/resolve 行为。
- Symbols: `TenantMembership(identitySub,tenantId,tenantDisplayName,status)`。
- Repository evidence: repository-wide搜索只有TokenFacade需要tenant/status；旧policy/port生产代码未装配，测试引用在File
  1移除。
- Dependencies and consumers: TokenFacade、Step 7 local adapter；仓库外二进制兼容不承诺，主规格明确breaking cleanup。
- Why now: public port必须在新的local implementation设计前收敛，不能把旧RBAC ID带进IdP表/RPC。
- Contract/signature changes: record删除 `rbac3UserId`；list/resolve方法与MembershipStatus保持，避免不必要调用方变化。
- Input/output and state mapping: IdP tenant/member read model→port record；无RBAC lookup或authorization decision
  mapping。
- Error and edge behavior: missing/disabled 仍由 port/TokenFacade fail closed；隐藏消费者必须改用缩减后的 record，不能恢复兼容字段。
- Implementation pseudocode:

```java
record TenantMembership(String identitySub, String tenantId, String tenantDisplayName, MembershipStatus status) validates every text and status
keep list(identitySub) and resolve(identitySub, tenantId) semantics while removing every mapping and accessor for rbac3UserId
compile TokenFacade and its fixtures against the reduced record so no RBAC-internal identifier crosses the port
```

- Verification contribution: `TEST-016/032` reduced-port compile proof。
- After this file: core membership port 无 RBAC 内部 ID；obsolete types 待 File 5 删除。

#### File 5 —
`DELETE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-core/src/main/java/top/egon/cola/platform/idp/core/port/UserResourceAccessAuthorizationPort.java; egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-core/src/main/java/top/egon/cola/platform/idp/core/resource/UserResourceAccessPolicy.java`

- Purpose: 物理删除无生产接线的旧 USER Resource authorization port/policy。
- Symbols: `UserResourceAccessAuthorizationPort`、`UserResourceAccessPolicy`。
- Repository evidence: repository-wide 搜索表明生产 wiring 不存在，直接测试引用已由 File 1 移除；两个 source 在续执行基线存在。
- Dependencies and consumers: 仅已移除的测试引用；不替代 Gateway/PEP 或 USER TokenFacade 行为。
- Why now: reduced membership contract 已稳定，可删除泄漏 RBAC ID 的未接线路径而不保留 stub。
- Contract/signature changes: 两个 Java types 消失；仓库外二进制兼容不承诺，符合主规格 breaking cleanup。
- Input/output and state mapping: Not applicable — 删除未装配 contract，不迁移 runtime state。
- Error and edge behavior: 删除后任何隐藏 import 必须在本 Step 显式处理；禁止恢复 no-op/allow compatibility implementation。
- Implementation pseudocode:

```java
remove UserResourceAccessAuthorizationPort.java and UserResourceAccessPolicy.java after File 1 deletes their direct test-only consumers
search main and test sources for both type names and require zero matches before staging the Step
compile the focused core slice so an undiscovered consumer fails visibly instead of being masked by a stub
```

- Verification contribution: `TEST-032` physical deletion/static symbol proof。
- After this file: core 无旧 USER Resource policy/port；local persistence adapter 待 Step 7。

- Validation working directory: `/Users/mario/SelfProject/Egon-COLA`
- Verification command:
  `./mvnw -B -ntp -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-core -am -Dtest=ClientCredentialsAccessPolicyTest,ResourceServerPolicyTest,TokenFacadeTest -Dsurefire.failIfNoSpecifiedTests=false test`
- Expected result: exit 0；TENANT/PLATFORM矩阵与TokenFacade USER membership回归通过；`rg`无
  `rbac3UserId|UserResourceAccessPolicy|UserResourceAccessAuthorizationPort` production/test命中。
- Failure returns to: File 1若RED断言不对应主规格；File 2若context/app字段或Grant derivation错误；File
  3若仍有真实生产消费者需回到主规格而非保留stub。
- Completion criteria: core public model自洽，旧未接线路径删除，USER TokenFacade行为保持。
- Rollback: revert本Step五组路径；无数据库状态。
- Commit paths: `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-core/src/test/java/top/egon/cola/platform/idp/core/resource/ClientCredentialsAccessPolicyTest.java; egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-core/src/test/java/top/egon/cola/platform/idp/core/resource/ResourceServerPolicyTest.java; egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-core/src/test/java/top/egon/cola/platform/idp/core/token/TokenFacadeTest.java; egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/idp/admin/oauth/controller/OAuthResourceSecurityMatrixIT.java`,   `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-core/src/main/java/top/egon/cola/platform/idp/contract/ServiceTokenContext.java`,   `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-core/src/main/java/top/egon/cola/platform/idp/contract/IdpClaimNames.java; egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-core/src/main/java/top/egon/cola/platform/idp/contract/IdpPrincipal.java; egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-core/src/main/java/top/egon/cola/platform/idp/contract/ServiceIdentityPrincipal.java; egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-core/src/main/java/top/egon/cola/platform/idp/core/token/ServiceAccessTokenClaims.java; egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-core/src/main/java/top/egon/cola/platform/idp/core/resource/ClientResourceGrant.java; egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-core/src/main/java/top/egon/cola/platform/idp/core/resource/ClientCredentialsAccessPolicy.java`,   `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-core/src/main/java/top/egon/cola/platform/idp/core/port/TenantMembershipPort.java`,   `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-core/src/main/java/top/egon/cola/platform/idp/core/port/UserResourceAccessAuthorizationPort.java; egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-core/src/main/java/top/egon/cola/platform/idp/core/resource/UserResourceAccessPolicy.java`
- Commit: `refactor(idp): make service context explicit and remove rbac identity leakage`

- Execution evidence: RED 在新增 `ServiceTokenContext` 尚不存在时按预期停在 testCompile；GREEN 聚焦命令通过，15 tests、0
  failures/errors/skips；`rg` 在 core `src/main` 与 `src/test` 对 `rbac3UserId|UserResourceAccessPolicy|UserResourceAccessAuthorizationPort`
  返回 1（无命中）；`git diff --check` 通过；提交范围为本 Step 声明的 13 个路径。随后因 Step 3 整合 testCompile 暴露
  旧 membership 五参构造与 admin matrix 旧 policy consumer，分别以 `5692763f539f3966a994a66088cbc4a6a8d5fc22`
  （TenantMembershipPort 兼容构造）和 `c5a537fa3cdd22a4c59a181564cf3be61fdd87f4`（删除已退役 matrix consumer）
  做了归属 Step 2 的路径受限纠偏，并重跑 Step 2 core gate 通过。

### Step 3 — 实现 IdP 管理员预配与轮换 hash-only Client Secret

- Requirements: `REQ-001`, `REQ-002`, `REQ-003`, `REQ-016`, `REQ-019`
- Dependencies: Steps 1–2
- Baseline state: OAuth Client CRUD只返回普通VO，Confidential Client无appId/Secret aggregate和rotate endpoint。
- Observable outcome: `POST /api/v1/identity/clients` 对Confidential Client一次返回AppID/App Key/Secret；
  `POST /{clientId}/secret-rotations`立即轮换；数据库仅hash/hint且沿用既有权限。
- End state: Secret lifecycle backend/contract GREEN；Token Endpoint尚未使用Secret认证，留给Step 4。
- Test-first gate: Required — service/controller tests因缺appId、Secret entity/repository、one-time VO和rotate method而
  RED。
- Ordered files:

#### File 1 —
`MODIFY egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/idp/admin/oauth/service/impl/OAuthClientServiceImplTest.java; egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/idp/admin/support/runtime/SpringConstructorResolutionTest.java`

- Purpose: 定义create/rotate事务、permission、one-active、no-leak、version conflict与生产构造器 wiring 行为。
- Symbols: `createsConfidentialClientWithOneTimeSecret`, `rollsBackClientWhenHashWriteFails`, `rotatesImmediately`,
  `usesExistingOAuthClientPermissions`。
- Repository evidence: service test已有Client CRUD fixtures；邻近ResourceServerControllerTest展示authorization
  mock/MockMvc风格。
- Dependencies and consumers: public service/controller methods、repositories/hash port；Step15 Web按这些response消费。
- Why now: 先固定Secret只出现一次与现有权限编码，阻止Entity/普通VO泄漏。
- Contract/signature changes: create return改为one-time result；list/detail仍safe `OAuthClientVO`
  ；rotate用expectedVersion并要求 `idp:oauth-client:update`。
- Input/output and state mapping: DTO appId/clientId→Client；CSPRNG char[]→hash/hint/credentialId+一次性VO；operator
  subject→audit fields。
- Error and edge behavior: PUBLIC无Secret；duplicate appId/clientId→409；hash/persist
  failure全部回滚且response无Secret；concurrent rotate一个成功一个version conflict。
- Implementation pseudocode:

```java
arrange active administrator, deterministic SecureRandom and hash adapter; create a CONFIDENTIAL client with appId/clientId
assert response exposes plaintext once while repository contains only encoded hash, last-four hint and one ACTIVE credential row
inject repository failure after client insert and assert transaction leaves no client/secret and exception text contains neither raw secret nor hash
invoke controller create/rotate/list and verify exact read/create/update permission codes and safe response serialization
```

- Verification contribution: RED/GREEN for `TEST-001`–`TEST-004`,`TEST-026`,`TEST-031` 与 production constructor regression。
- After this file: tests RED only for missing lifecycle behavior。

#### File 2 —
`CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/idp/admin/oauth/controller/OAuthClientControllerTest.java`

- Purpose: 新建 controller contract test，固定 create/rotate permission、no-store 与 safe serialization。
- Symbols: `usesExistingOAuthClientPermissions`、`returnsSecretOnlyFromCreateAndRotate`。
- Repository evidence: 目标测试在续执行基线不存在；邻近 `ResourceServerControllerTest` 提供 MockMvc/authorization mock 风格。
- Dependencies and consumers: OAuthClientController 与 File 6 one-time VO；Step 15 Web 依赖相同 wire contract。
- Why now: service RED contract 后独立建立 HTTP RED 点，避免把不存在的测试误标为修改。
- Contract/signature changes: 覆盖 create/rotate response、`Cache-Control: no-store` 与 read/create/update 权限编码。
- Input/output and state mapping: JSON DTO + administrator principal→service invocation→safe/one-time JSON response。
- Error and edge behavior: SERVICE principal、缺权限、version conflict、PUBLIC secret request 均确定性拒绝且不回显 Secret。
- Implementation pseudocode:

```java
build MockMvc requests for create, rotate, list and detail using the existing administrator authorization fixture
assert exact idp:oauth-client:create or update permissions, no-store headers and one-time secret response fields
serialize ordinary list/detail responses and error bodies and require no secret, secretHash or authorization material
```

- Verification contribution: controller slice RED/GREEN for `TEST-001/002/026/031`。
- After this file: controller test RED for absent create/rotate response contract。

#### File 3 —
`MODIFY egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/idp/admin/oauth/domain/pojo/IdentityClientEntity.java; egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/idp/admin/oauth/repo/IdentityClientRepository.java`

- Purpose: 映射V5 Client/Secret state和一active credential invariant。
- Symbols: `IdentityClientEntity.appId`、confidential create factory。
- Repository evidence: current JPA Entity factory/version/audit conventions；V5 exact columns/partial unique index。
- Dependencies and consumers: OAuthClientServiceImpl 与 File 4 Secret entity/repository；Entity 不跨 Controller boundary。
- Why now: persistence model先于service orchestration，并保持Entity最少化。
- Contract/signature changes: Confidential create factory 要求 appId；现有 Client entity 新增 V5 `app_id` 映射。
- Input/output and state mapping: DB native varchar/version→Java String/long；Secret raw 永不进入 Client entity。
- Error and edge behavior: blank/duplicate appId 在 domain/DB 层拒绝；PUBLIC compatibility 规则按 Spec 保持。
- Implementation pseudocode:

```java
IdentityClientEntity.createConfidential(id, appId, clientId, name, ttl, actor, now) validates immutable appId and stores no secret
map app_id to the existing client aggregate without adding secret/hash columns or plaintext accessors
preserve existing PUBLIC Authorization Code construction and optimistic version behavior while adding confidential app identity
```

- Verification contribution: Client mapping assertions in File 1 and Step 1 V5 constraints。
- After this file: Client entity compiles；Secret persistence remains absent until File 4。

#### File 4 —
`CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/idp/admin/oauth/domain/pojo/IdentityClientSecretEntity.java; egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/idp/admin/oauth/repo/IdentityClientSecretRepository.java`

- Purpose: 新建 V5 Secret persistence aggregate 与 active credential locking repository。
- Symbols: `IdentityClientSecretEntity.create/revoke`、`findActiveByClientIdForUpdate`。
- Repository evidence: 两个目标路径在续执行基线不存在；V5 已提交 exact columns/partial unique index，邻近 entity/repo 提供惯例。
- Dependencies and consumers: File 3 Client entity、OAuthClientServiceImpl、Step 4 authenticator。
- Why now: Client appId mapping 完成后建立 hash-only credential state，不把新文件误当作修改。
- Contract/signature changes: 新 entity fields 为 id/clientId/hash/hint/status/version/audit/rotatedBy；repository 只暴露 active lookup/lock。
- Input/output and state mapping: encoded hash/hint/credential id→V5 columns；raw Secret 不进入 entity、query 或 projection。
- Error and edge behavior: REVOKED 不可恢复；active row lock/CAS；unique violation 映射 stable conflict 且不泄漏值。
- Implementation pseudocode:

```java
create IdentityClientSecretEntity with ACTIVE status, encoded hash, hint, actor, timestamps and optimistic version zero
lock the active credential by client id with PESSIMISTIC_WRITE, revoke it once, then insert one replacement in the same transaction
expose repository methods only for active metadata and encoded-hash verification; never define a plaintext column, accessor or projection
```

- Verification contribution: persistence/transaction assertions in File 1 and V5 constraints。
- After this file: Client/Secret persistence models compile；service orchestration remains RED。

#### File 5 —
`MODIFY egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/idp/admin/oauth/domain/dto/CreateOAuthClientDTO.java; egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/idp/admin/oauth/domain/vo/OAuthClientVO.java`

- Purpose: 修改既有 create DTO 与普通 safe view，加入 appId/Secret metadata 但不加入明文。
- Symbols: DTO 新增 appId；`OAuthClientVO` 新增 appId/secretHint/secretStatus。
- Repository evidence: IdP domain/dto/vo records与validation annotations；主规格complete JSON field mapping。
- Dependencies and consumers: Controller、Service、File 6 one-time VO、Step 15 TypeScript types；不被 JPA 或 signer 复用。
- Why now: persistence存在后定义transport，防止为少建class而暴露Entity/hash。
- Contract/signature changes: `OAuthClientVO` 加 appId/secretHint/secretStatus 但无 secret/hash；create DTO 按 client type 验证 appId。
- Input/output and state mapping: Entity/active credential metadata→safe VO；不选择或序列化 secretHash。
- Error and edge behavior: PUBLIC appId/Secret字段nullable规则与Spec一致；Jackson/toString tests禁止one-time
  VO日志化，response `Cache-Control: no-store`。
- Implementation pseudocode:

```java
map list/detail from entities and active secret metadata without selecting or serializing secretHash
validate DTO appId only for CONFIDENTIAL and keep PUBLIC Authorization Code client compatibility unchanged
assert safe VO toString and JSON fields never include secret or hash while exposing only operator-safe hint and status
```

- Verification contribution: safe contract/no-leak assertions and Step 15 exact frontend mapping。
- After this file: existing safe transport compiles；one-time response types remain absent until File 6。

#### File 6 —
`CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/idp/admin/oauth/domain/dto/RotateClientSecretDTO.java; egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/idp/admin/oauth/domain/vo/CreatedOAuthClientVO.java; egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/idp/admin/oauth/domain/vo/RotatedClientSecretVO.java`

- Purpose: 新建 rotate request 与两个一次性 plaintext response boundary。
- Symbols: `RotateClientSecretDTO.expectedVersion`、`CreatedOAuthClientVO`、`RotatedClientSecretVO`。
- Repository evidence: 三个目标路径在续执行基线不存在；现有 dto/vo record 与 validation annotation 风格可直接沿用。
- Dependencies and consumers: Files 1/2 tests、File 7 service/controller、Step 15 Web；不得被 JPA 或 signer 复用。
- Why now: safe view 已明确后，单独创建明文边界，防止执行器把新类型当作既有文件修改。
- Contract/signature changes: create/rotate 分别返回显式 one-time VO；rotate 输入携带 expectedVersion。
- Input/output and state mapping: generated char[]→one-time response String；返回构造完成后立即清零原 byte/char buffers。
- Error and edge behavior: one-time VO 不进入日志/缓存/普通 list/detail；response 使用 no-store；失败响应不含 secret。
- Implementation pseudocode:

```java
define CreatedOAuthClientVO with safe client metadata plus appId, appKey and one-time secret for create only
define RotatedClientSecretVO with clientId, credentialId, hint, one-time secret and createdAt for rotate only
validate expectedVersion in RotateClientSecretDTO and keep all one-time types out of entity, repository and ordinary query paths
```

- Verification contribution: one-time response contract and controller no-store assertions。
- After this file: transport types compile；service/controller remain RED until File 7。

#### File 7 —
`MODIFY egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/idp/admin/oauth/service/OAuthClientService.java; egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/idp/admin/oauth/service/impl/OAuthClientServiceImpl.java; egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/idp/admin/oauth/controller/OAuthClientController.java`

- Purpose: 编排CSPRNG→Argon2id→Client+Secret事务并暴露existing permission-protected endpoints。
- Symbols: `create(CreateOAuthClientDTO,operator)`、`rotateSecret(clientId,expectedVersion,operator)`、controller
  `/secret-rotations` route/headers。
- Repository evidence: current service transaction/version/audit/authorization style；`SpringPasswordHashAdapter`
  实现所需Argon2/dummy path。
- Dependencies and consumers: entities/repositories/PasswordHashPort/LongIdGenerator/SecureRandom；Admin Web/API clients。
- Why now: 前三个files固定测试、persistence、transport后做最小GREEN orchestration。
- Contract/signature changes: create返回Created VO；rotate新增POST；list/update routes不变；permissions严格read/create/update。
- Input/output and state mapping: 32 random bytes→Base64URL no padding≥256 bits→char[] hash→hint→zero
  buffers；operator/principal→audit。
- Error and edge behavior: raw secret/header/hash从不log；transaction
  exception清理buffers并回滚；duplicate/concurrent错误映射409；SERVICE principal无管理权限。
- Implementation pseudocode:

```java
@Transactional create validates DTO and uniqueness, persists Client, generates 32 random bytes, hashes char[] through PasswordHashPort, persists ACTIVE Secret metadata, then returns one-time VO
@Transactional rotate locks Client and active Secret, checks expectedVersion, revokes old row, inserts new hashed credential and returns plaintext only after successful flush
controller requires idp:oauth-client:create for create and idp:oauth-client:update for rotate, sets Cache-Control no-store and never accepts a SERVICE client self-management shortcut
finally overwrite byte/char buffers; map constraint/version failures to stable conflict responses without credential details
```

- Verification contribution: makes File 1 GREEN并为Step4 ClientSecretAuthenticator提供repository。
- After this file: Client admin backend完成；Token endpoint仍只接受private_key_jwt直到Step4。

- Validation working directory: `/Users/mario/SelfProject/Egon-COLA`
- Verification command:
  `./mvnw -B -ntp -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthClientServiceImplTest,OAuthClientControllerTest,SpringConstructorResolutionTest -Dsurefire.failIfNoSpecifiedTests=false test`
- Expected result: exit 0；create/rotate/rollback/concurrency/permission/no-store/no-leak cases通过，未运行live DB之外环境。
- Failure returns to: Files 1–2 test contracts；Files 3–4 persistence/state；Files 5–6 boundary mapping；File 7 transaction/permission
  orchestration。
- Completion criteria: API-001/API-002实现所需backend完整，Secret明文仅两个one-time responses，existing permissions
  retained。
- Rollback: V5未部署时path-limited revert；V5已部署时应用可回滚到停机snapshot，不能恢复旧Secret/JWK双轨。
- Commit paths: `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/idp/admin/oauth/service/impl/OAuthClientServiceImplTest.java; egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/idp/admin/support/runtime/SpringConstructorResolutionTest.java`,   `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/idp/admin/oauth/controller/OAuthClientControllerTest.java`,   `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/idp/admin/oauth/domain/pojo/IdentityClientEntity.java; egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/idp/admin/oauth/repo/IdentityClientRepository.java`,   `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/idp/admin/oauth/domain/pojo/IdentityClientSecretEntity.java; egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/idp/admin/oauth/repo/IdentityClientSecretRepository.java`,   `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/idp/admin/oauth/domain/dto/CreateOAuthClientDTO.java; egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/idp/admin/oauth/domain/vo/OAuthClientVO.java`,   `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/idp/admin/oauth/domain/dto/RotateClientSecretDTO.java; egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/idp/admin/oauth/domain/vo/CreatedOAuthClientVO.java; egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/idp/admin/oauth/domain/vo/RotatedClientSecretVO.java`,   `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/idp/admin/oauth/service/OAuthClientService.java; egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/idp/admin/oauth/service/impl/OAuthClientServiceImpl.java; egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/idp/admin/oauth/controller/OAuthClientController.java`
- Commit: `feat(idp): manage hash-only client secrets`

- Execution evidence: Step 3 RED 的整合命令在 admin `testCompile` 因计划内 Secret entity/repository 与轮换 DTO/VO 尚不存在而失败；
  补齐 hash-only entity/repository、appId/安全 VO、CSPRNG+PasswordHashPort 事务编排、审计 operator 事件、PESSIMISTIC_WRITE
  Client/active Secret 锁与 `/secret-rotations` 后，计划命令通过，13 tests、0 failures/errors/skips；`git diff --check`
  与 staged path gate 通过。提交 `b374559b69300969027575999a57e7eca106f326` 包含本 Step 声明的 15 个路径，Plan/Spec
  文档未暂存；未启动 live DB、服务或浏览器。

### Step 4 — 切换 Token Endpoint 到 client_secret_basic 并删除 private_key/JWK/Admission issuer

- Requirements: `REQ-004`, `REQ-007`, `REQ-008`, `REQ-017`, `REQ-018`
- Dependencies: Steps 2–3
- Execution state: `Committed` at `f2fd8230`; no empty or corrective Step 4 commit was created.
- Baseline state: Token controller读取form
  client_id/assertion，ClientCredentialsTokenService接收ClientAssertionAuthentication；signer/verifier缺app/context；Resource
  API仍管理Client JWK和Admission RPC。
- Observable outcome: client_credentials仅用单一Basic header认证active Secret，服务器从Grant派生context并签发完整SERVICE
  claims；旧assertion/JWK/Admission API/contracts返回确定性失败或不存在。
- End state: IdP core/admin/rpc-contract reactor GREEN；USER refresh/login/signing JWK保持；starter consumer待Step5。
- Test-first gate: Required — Basic/claim/metadata/old-path negative tests当前因旧authenticator和JWK endpoints存在而
  RED。
- Ordered files:

#### File 1 —
`MODIFY egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/idp/admin/token/service/impl/ClientCredentialsTokenServiceTest.java; egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/idp/admin/oauth/config/OAuthConfigTest.java; egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/test/java/top/egon/cola/platform/idp/starter/security/IdpJwtVerifierTest.java`

- Purpose: 定义Basic parsing/hash constant-work、Grant context/claims、metadata和旧请求negative matrix。
- Symbols: `acceptsSingleBasicHeader`, `rejectsBodySecretAndAssertion`, `issuesPlatformWithoutTid`,
  `keepsUserRefreshContract`。
- Repository evidence: existing token service/config/verifier tests及controller refresh path；主规格API-003/API-004完整错误模型。
- Dependencies and consumers: Steps2/3 models/repository；USER TokenFacade/SigningKey regression fixtures。
- Why now: 先固定protocol wire/auth/error，再替换生产实现和删除旧types。
- Contract/signature changes: test request只含grant_type/resource/tenant_id/scope；Authorization Basic携带form-encoded
  clientId/secret；PLATFORM tenant omitted。
- Input/output and state mapping: Basic bytes→decoded clientId/char[]→Secret
  row/hash→credentialId；Grant→context/tid；claims→JWT/principal。
- Error and edge behavior: duplicate/malformed/non-Basic/oversize/unknown/revoked/wrong Secret均generic `invalid_client`
  ；body client_secret和任何assertion field拒绝；USER refresh unaffected。
- Implementation pseudocode:

```java
submit client_credentials with exactly one RFC Basic header and assert no client_id/client_secret/assertion body fields are accepted
verify correct active secret returns no-store Bearer token whose decoded claims include app_id, scope_context, source tuple, credential_id and exact audience
exercise TENANT and PLATFORM grants, asserting tid presence iff TENANT and caller cannot submit scope_context or wildcard tenant
replay old private_key_jwt and JWK/Admission routes and assert invalid_client or 404 while USER refresh and signing JWKS fixtures remain identical
```

- Verification contribution: RED/GREEN for API-003/004、claim/verifier、`REQ-017/018`。
- After this file: focused tests RED against old implementation。

#### File 2 —
`CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/idp/admin/oauth/controller/OAuthTokenClientCredentialsTest.java`

- Purpose: 新建 Token Endpoint HTTP contract test，固定 Basic-only wire/error/no-store behavior。
- Symbols: `acceptsSingleBasicHeader`、`rejectsBodySecretAndAssertion`、old-request negative matrix。
- Repository evidence: 目标路径在续执行基线不存在；现有 controller/token tests 提供 MockMvc 与 OAuth error wrapper 风格。
- Dependencies and consumers: Files 3–4 authenticator/controller/service；Spring OAuth2 Client Step 5。
- Why now: service/verifier RED tests 后独立建立 HTTP RED 点，避免把缺失测试当作修改。
- Contract/signature changes: request 只含 grant_type/resource/tenant_id/scope；Basic header 承载 client credentials。
- Input/output and state mapping: Authorization header + form resource request→OAuth token or stable OAuth error response。
- Error and edge behavior: duplicate/malformed/non-Basic/body secret/assertion 均 generic invalid_client；响应和日志无 Secret。
- Implementation pseudocode:

```java
post client_credentials with one valid Basic header and assert no-store Bearer response plus required SERVICE claims
submit body client_secret, client assertion, duplicate Authorization and malformed Basic variants and assert generic invalid_client
exercise TENANT and PLATFORM forms and require caller context or wildcard tenant input to be rejected before token issuance
```

- Verification contribution: API-003 HTTP RED/GREEN and old-wire negative proof。
- After this file: controller contract RED against the existing private_key_jwt endpoint。

#### File 3 —
`CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-core/src/main/java/top/egon/cola/platform/idp/core/oauth/ClientSecretAuthentication.java; egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/idp/admin/oauth/service/impl/ClientSecretBasicAuthenticator.java`

- Purpose: 新建 Basic-only authenticator 与可信 credential identity value object。
- Symbols: `ClientSecretBasicAuthenticator.authenticate(HttpServletRequest)`、`ClientSecretAuthentication`。
- Repository evidence: current controller parsing/error wrapper、PasswordHashPort dummyHash、Secret repository、Token
  service/signer structure。
- Dependencies and consumers: IdP Client/Secret repositories与PasswordHashPort；File 4 Token controller/service。
- Why now: File1固定wire contract后，以一个direct authenticator替换旧实现，不引入Strategy/Factory。
- Contract/signature changes: authenticator 从 header 认证并返回 clientId/credentialId；不接受 assertion timestamps/JTI。
- Input/output and state mapping: ISO-8859-1 Basic+form decode→bounded credentials→constant-work hash
  match→clientId/credentialId；form resource/tenant/scope→policy。
- Error and edge behavior: 所有auth failures执行真实或dummy hash并返回401+WWW-Authenticate/OAuth wrapper；无详细原因/Secret
  log；Grant errors维持invalid_scope/invalid_target。
- Implementation pseudocode:

```java
authenticate reads one Authorization header, requires Basic scheme, base64-decodes bounded bytes, splits once, form-decodes clientId and secret, then clears buffers
load active CONFIDENTIAL client and active Secret row; compare through PasswordHashPort or dummyHash on every miss and return ClientSecretAuthentication(clientId, credentialId)
return ClientSecretAuthentication only after active client and active Secret hash verification succeeds, with no raw credential retained
clear decoded buffers in finally and emit only a stable invalid_client classification to the controller boundary
```

- Verification contribution: makes Basic authentication tests GREEN。
- After this file: Basic authentication collaborator works；existing controller/service remain to be modified。

#### File 4 —
`MODIFY egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/idp/admin/oauth/controller/OAuthTokenController.java; egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/idp/admin/token/service/impl/ClientCredentialsTokenService.java; egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/idp/admin/oauth/controller/OAuthMetadataController.java`

- Purpose: 原地切换既有 Token/metadata endpoints 到 Basic authenticator 与 Grant-derived context issuance。
- Symbols: controller Basic delegation、service `issue`、metadata auth method list。
- Repository evidence: 三个路径在续执行基线存在；现有 controller error wrapper、token service 与 metadata mapping 可最小修改。
- Dependencies and consumers: File 3 authenticator、Steps 2/3 policy/repository、signer；Step 5 Spring client。
- Why now: 新 authenticator ready 后修改既有 endpoints，不创建平行 controller/service。
- Contract/signature changes: controller 从 header 认证；service 不接 assertion timestamps/JTI；metadata 仅 client_secret_basic。
- Input/output and state mapping: trusted clientId/credentialId + resource/tenant/scope→Grant policy→SERVICE access token response。
- Error and edge behavior: credential-bearing body keys 拒绝；auth failures 401 generic；Grant errors保持 invalid_scope/invalid_target。
- Implementation pseudocode:

```java
make OAuthTokenController reject credential-bearing form keys, call ClientSecretBasicAuthenticator and pass only trusted identity to the token service
derive ServiceTokenContext and tenant solely from the active grant, build app/source/credential/context claims and sign one no-store response
publish client_secret_basic as the only token endpoint auth method while preserving USER authorization-code and refresh metadata behavior
```

- Verification contribution: Basic controller/service/metadata tests GREEN。
- After this file: new Token path works；signer/verifier and old-path deletion remain。

#### File 5 —
`MODIFY egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-core/src/main/java/top/egon/cola/platform/idp/core/oauth/OAuthClient.java; egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/idp/admin/oauth/repo/JpaOAuthClientStore.java; egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/idp/admin/token/service/impl/Rs256TokenService.java; egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/idp/starter/security/IdpJwtVerifier.java; egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-gateway-adapter/src/main/java/top/egon/cola/platform/idp/gateway/security/IdpTrustedIdentityMapper.java`

- Purpose: 贯通app/context/tid claims与PLATFORM-aware resource/gateway验证。
- Symbols: OAuthClient.appId；JWT claims；verifier `service`；Gateway只为TENANT写trusted tenant header。
- Repository evidence: current store mapping、RS256 signer、verifier claim readers、mapper已对null tenant有拒绝分支。
- Dependencies and consumers: core records、Resource state projection、Gateway headers、DDC Step14。
- Why now: authenticator/service已生成可信模型，随后更新wire serialization和所有直接消费者。
- Contract/signature changes: JWT新增app_id/scope_context；PLATFORM不含tid；Gateway对PLATFORM不生成空/星号header并由非平台业务路径拒绝。
- Input/output and state mapping: Entity.appId→OAuthClient→claims→JWT→ServiceIdentityPrincipal；context controls
  header/tenant behavior。
- Error and edge behavior: missing/unknown context/app/credential、PLATFORM+tid、TENANT-null均invalid token；USER
  parser/headers保持。
- Implementation pseudocode:

```java
map IdentityClientEntity.appId into OAuthClient and require it only for CONFIDENTIAL service issuance
sign service JWT with app_id and scope_context; emit tid only for TENANT while preserving issuer, aud, jti, source and resource_version
verifier reads all required claims, constructs ServiceIdentityPrincipal and rejects every context/tid contradiction before state lookup succeeds
gateway mapper writes X-Egon-Tenant-Id only for TENANT service/user principals and never converts PLATFORM null into an empty or wildcard header
```

- Verification contribution: IdpJwtVerifier/Token service/Gateway unit regression。
- After this file: new claims roundtrip GREEN；legacy files/endpoints仍待删除。

#### File 6 —
`DELETE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-core/src/main/java/top/egon/cola/platform/idp/core/oauth/ClientAssertionAuthentication.java; egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-core/src/main/java/top/egon/cola/platform/idp/core/port/ClientAssertionReplayStore.java; egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-core/src/main/java/top/egon/cola/platform/idp/core/port/ClientCredentialStore.java; egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-core/src/main/java/top/egon/cola/platform/idp/core/resource/ClientJwkCredential.java; egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-core/src/main/java/top/egon/cola/platform/idp/core/resource/AdmissionRequest.java; egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-core/src/main/java/top/egon/cola/platform/idp/core/resource/AdmissionTicketClaims.java; egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-core/src/main/java/top/egon/cola/platform/idp/core/resource/ResourceServerAdmissionPolicy.java; egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/idp/admin/oauth/service/impl/PrivateKeyJwtAuthenticator.java; egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/idp/admin/oauth/repo/RedisClientAssertionReplayStore.java; egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/idp/admin/resource/repo/JpaClientCredentialStore.java; egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/idp/admin/resource/service/impl/ResourceServerAdmissionServiceImpl.java; egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/idp/admin/resource/support/rpc/ResourceServerAdmissionRpcProvider.java; egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/idp/admin/support/oauth/LocalServiceAccessTokenSupplier.java`

- Purpose: 删除旧credential、replay、Ticket issuer/provider与IdP→RBAC local token双轨。
- Symbols: 所列types/classes全部absent。
- Repository evidence: repository-wide reference map显示消费者已由Files3–5及Steps7/8替换；主规格 `REQ-018` 明确no dual
  track。
- Dependencies and consumers: tests/config/resource API须在Files7–10同步；外部旧客户端故意breaking。
- Why now: 新Token path与claims已GREEN，删除可由编译和negative contract证明。
- Contract/signature changes: private_key_jwt和Admission RPC不再可调用；旧SERVICE token supplier不提供兼容bean。
- Input/output and state mapping: None；删除所有JWK/assertion/Ticket/JTI replay state；IdP signing key types不在集合中。
- Error and edge behavior: 未迁移consumer在cutover失败；由Step18 inventory gate阻止发布；不得以deprecated wrapper恢复旧行为。
- Implementation pseudocode:

```text
delete assertion authentication, replay and client-JWK ports after every new Basic call site compiles
delete Admission policy, issuer service, RPC provider and local service-token supplier after DDC/token replacements are named
search main/test/config/proto for every deleted symbol and treat any remaining production consumer as a hard compile failure
preserve IdP signing-key entities, JWKS endpoint and USER token signer because they are outside this deletion set
```

- Verification contribution: `TEST-029/032` static and compile absence proof。
- After this file: production旧types删除；tests/config/resource contract仍需Files7–10对齐。

#### File 7 —
`MODIFY egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/idp/admin/oauth/config/OAuthConfig.java; egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/idp/admin/resource/config/ResourceServerConfig.java; egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/idp/admin/resource/controller/ResourceServerController.java; egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/idp/admin/resource/service/ResourceServerService.java; egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/idp/admin/resource/service/impl/ResourceServerServiceImpl.java; egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/idp/admin/resource/domain/dto/CreateResourceServerDTO.java; egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/idp/admin/resource/domain/pojo/IdentityResourceServerEntity.java; egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/idp/admin/resource/domain/vo/ResourceServerVO.java; egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/idp/admin/resource/repo/JpaResourceServerStore.java; egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/idp/admin/resource/service/ResourceServerProjectionService.java; egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/idp/admin/support/bootstrap/IdpDevelopmentClientBootstrap.java`

- Purpose: 修改保留的 Spring wiring 与 Resource Server contract，移除 Client JWK/Admission TTL fields/routes。
- Symbols: `ClientSecretBasicAuthenticator` bean；Resource create/view不含key/TTL；JWK routes absent。
- Repository evidence: current config/controller/service/domain直接列出旧beans/routes；主规格API-004/§12指定删除范围。
- Dependencies and consumers: OAuth Token controller、Resource Admin Web Step15、Resource projection/DDC state保持。
- Why now: File6删除生产types后必须在同Step完成wiring/contract清理，使reactor GREEN。
- Contract/signature changes: remove Resource key create/revoke endpoints与fields；OAuthConfig注入Secret repo/hash；不删除
  `SigningKeyPage`后端。
- Input/output and state mapping: Resource retains id/uri/biz/app/env/version/status/managementClient；JWK/TTL no
  mapping；projection schema按V5更新。
- Error and edge behavior: old key endpoints 404；old DTO fields按serializer policy拒绝/忽略需contract test固定；旧config
  bean不存在且新Basic bean fail-fast缺hash/repo。
- Implementation pseudocode:

```java
wire ClientSecretBasicAuthenticator from OAuthClientStore, IdentityClientSecretRepository, PasswordHashPort and Clock; remove replay/JWK/Admission beans
remove Client JWK and admission TTL parameters from Resource create/update/view mappings while keeping source Resource status/version projection
remove key management controller and service methods, then assert old URLs are not registered and retained Resource mapping is unchanged
retain SigningKey service/controller/JWKS configuration and USER OAuth beans without production diff
```

- Verification contribution: full IdP admin/rpc/core compile，Resource controller tests更新后的404/unchanged projection
  proof。
- After this file: retained IdP backend contract is Basic-only；dedicated JWK types remain for File 8 deletion。

#### File 8 —
`DELETE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/idp/admin/resource/domain/dto/CreateClientJwkDTO.java; egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/idp/admin/resource/domain/pojo/IdentityClientJwkEntity.java; egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/idp/admin/resource/domain/vo/ClientJwkVO.java; egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/idp/admin/resource/repo/IdentityClientJwkRepository.java`

- Purpose: 物理删除 dedicated Client JWK DTO/entity/VO/repository types。
- Symbols: four Client JWK admin types absent；IdP Signing Key types remain。
- Repository evidence: four targets exist at resume baseline and are dedicated to routes/storage removed by Step 1/File 7。
- Dependencies and consumers: File 7 removes retained controller/service/config references；Step 15 removes Web controls。
- Why now: retained Resource contract no longer consumes these types，so deletion is explicit and cannot be mistaken for content modification。
- Contract/signature changes: Client JWK admin model disappears；Signing Key/JWKS contract is outside the deletion set。
- Input/output and state mapping: Not applicable — V5 already drops Client JWK storage；no runtime state is migrated here。
- Error and edge behavior: any remaining import is a hard compile failure；do not delete similarly named Signing Key types。
- Implementation pseudocode:

```text
delete CreateClientJwkDTO, IdentityClientJwkEntity, ClientJwkVO and IdentityClientJwkRepository after File 7 removes their consumers
search the IdP admin module for these exact type names and old key-management URLs and require zero production matches
separately assert SigningKey service, entity, page backend and JWKS symbols still exist so the deletion boundary is not broadened
```

- Verification contribution: `TEST-028/029/032` Client JWK source deletion boundary。
- After this file: production Client JWK admin types are absent；retained tests remain for File 9。

#### File 9 —
`MODIFY egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/idp/admin/resource/controller/ResourceServerControllerTest.java; egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/idp/admin/resource/service/impl/ResourceServerServiceImplTest.java; egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/idp/admin/oauth/controller/OAuthResourceSecurityMatrixIT.java; egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/idp/admin/support/bootstrap/IdpDevelopmentClientBootstrapTest.java; egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/idp/admin/support/persistence/IdpPersistenceEntityContractTest.java`

- Purpose: 修改仍保留的 Resource/USER/SERVICE 测试消费者以匹配新 contract。
- Symbols: Resource create/status/projection tests；SERVICE Grant context matrix；USER regression。
- Repository evidence: repository-wide旧symbol引用清单明确这些测试仍import已删除types；ResourceServer测试还验证保留能力。
- Dependencies and consumers: Files3–8 production contract；Step18 static deletion gate。
- Why now: production wiring已完成后清理test consumers，使module reactor与旧symbol归零同时成立。
- Contract/signature changes: Resource test fixtures 不再携带 JWK/Admission TTL；SERVICE matrix 使用 typed context。
- Input/output and state mapping: existing Resource metadata/Grant→safe views/policy；旧assertion/Ticket fixtures删除；USER
  OAuth matrix保持。
- Error and edge behavior: old routes/requests改为404/invalid_client assertions；不因删除tests而丢失Basic、Grant、Resource
  status/version或USER regression。
- Implementation pseudocode:

```java
update ResourceServer controller/service fixtures to omit client JWK and admission TTL while retaining resource identity, status, version and projection assertions
retain SERVICE Grant authorization matrix using TENANT and PLATFORM contexts and keep the unchanged USER refresh/resource boundary assertions that remain applicable
assert old routes and requests are 404 or invalid_client while retaining Basic, Grant, Resource status/version and USER regression
search these retained tests for deleted type imports and require zero hits before running the complete selected reactor
```

- Verification contribution: module compile/selected retained regression。
- After this file: retained IdP tests compile against new Token/Resource contracts；obsolete tests remain for File 10 deletion。

#### File 10 —
`DELETE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/idp/admin/oauth/repo/RedisClientAssertionReplayStoreTest.java; egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/idp/admin/oauth/service/impl/PrivateKeyJwtAuthenticatorTest.java; egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/idp/admin/resource/service/impl/ResourceServerAdmissionServiceImplTest.java; egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/idp/admin/support/oauth/LocalServiceAccessTokenSupplierTest.java; egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/idp/admin/resource/support/rpc/ResourceServerAdmissionRpcProviderTest.java`

- Purpose: 删除只验证已移除 replay/private-key/Admission/local supplier production targets 的四个测试类。
- Symbols: 四个 obsolete test classes 全部 absent。
- Repository evidence: 四个文件在续执行基线存在，且其 production targets 已在 File 6 删除；无 replacement behavior。
- Dependencies and consumers: Step 18 old-symbol gate；不影响 File 9 保留的 Resource/USER/SERVICE regression。
- Why now: production 删除与保留测试对齐后物理删除 obsolete tests，避免执行器把删除误做内容修改。
- Contract/signature changes: old components 没有 compatibility test class；negative wire behavior 已在 Files 1–2/9 覆盖。
- Input/output and state mapping: Not applicable — 删除仅针对 retired implementation tests，无 runtime state。
- Error and edge behavior: 若搜索发现仍需覆盖的 shared behavior，迁移到保留测试后再删除；禁止保留空壳测试。
- Implementation pseudocode:

```text
delete the replay-store and private-key authenticator tests after their production classes are removed
delete Admission issuer and local token supplier tests after direct SERVICE Token replacement is represented by retained tests
search all IdP tests for deleted production symbols and require zero hits, while keeping wire-level negative coverage in the new controller test
```

- Verification contribution: `TEST-028/029` obsolete test and symbol deletion completeness。
- After this file: IdP tests compile solely against new Token/Resource contracts。

- Validation working directory: `/Users/mario/SelfProject/Egon-COLA`
- Verification command:
  `./mvnw -B -ntp -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin,egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-gateway-adapter -am -Dtest=OAuthTokenClientCredentialsTest,ClientCredentialsTokenServiceTest,IdpJwtVerifierTest,OAuthConfigTest,ResourceServerControllerTest,ResourceServerServiceImplTest,OAuthResourceSecurityMatrixIT -Dsurefire.failIfNoSpecifiedTests=false test`
- Expected result: exit 0；Basic/TENANT/PLATFORM/metadata/old-route negative/USER regression通过；删除symbol
  search仅在Spec/Plan历史文本允许命中。
- Failure returns to: Files 1–2 wire assertions；Files 3–4 auth/service；File 5 claim consumer；File 6 hidden production
  consumer；Files 7–8 wiring/resource/JWK deletion；Files 9–10 retained or obsolete test coverage。
- Completion criteria: API-003/API-004和SERVICE claim实现完成，private_key/JWK/Admission issuer无生产入口，Signing/USER行为保留。
- Rollback: cutover前path-limited revert；V5执行后必须整组snapshot/binary/config rollback，禁止只恢复JWK代码。
- Commit paths: `MODIFY egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/idp/admin/token/service/impl/ClientCredentialsTokenServiceTest.java; egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/idp/admin/oauth/config/OAuthConfigTest.java; egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/test/java/top/egon/cola/platform/idp/starter/security/IdpJwtVerifierTest.java`,   `CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/idp/admin/oauth/controller/OAuthTokenClientCredentialsTest.java`,   `CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-core/src/main/java/top/egon/cola/platform/idp/core/oauth/ClientSecretAuthentication.java; egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/idp/admin/oauth/service/impl/ClientSecretBasicAuthenticator.java`,   `MODIFY egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/idp/admin/oauth/controller/OAuthTokenController.java; egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/idp/admin/token/service/impl/ClientCredentialsTokenService.java; egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/idp/admin/oauth/controller/OAuthMetadataController.java`,   `MODIFY egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-core/src/main/java/top/egon/cola/platform/idp/core/oauth/OAuthClient.java; egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/idp/admin/oauth/repo/JpaOAuthClientStore.java; egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/idp/admin/token/service/impl/Rs256TokenService.java; egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/idp/starter/security/IdpJwtVerifier.java; egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-gateway-adapter/src/main/java/top/egon/cola/platform/idp/gateway/security/IdpTrustedIdentityMapper.java`,   `DELETE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-core/src/main/java/top/egon/cola/platform/idp/core/oauth/ClientAssertionAuthentication.java; egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-core/src/main/java/top/egon/cola/platform/idp/core/port/ClientAssertionReplayStore.java; egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-core/src/main/java/top/egon/cola/platform/idp/core/port/ClientCredentialStore.java; egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-core/src/main/java/top/egon/cola/platform/idp/core/resource/ClientJwkCredential.java; egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-core/src/main/java/top/egon/cola/platform/idp/core/resource/AdmissionRequest.java; egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-core/src/main/java/top/egon/cola/platform/idp/core/resource/AdmissionTicketClaims.java; egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-core/src/main/java/top/egon/cola/platform/idp/core/resource/ResourceServerAdmissionPolicy.java; egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/idp/admin/oauth/service/impl/PrivateKeyJwtAuthenticator.java; egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/idp/admin/oauth/repo/RedisClientAssertionReplayStore.java; egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/idp/admin/resource/repo/JpaClientCredentialStore.java; egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/idp/admin/resource/service/impl/ResourceServerAdmissionServiceImpl.java; egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/idp/admin/resource/support/rpc/ResourceServerAdmissionRpcProvider.java; egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/idp/admin/support/oauth/LocalServiceAccessTokenSupplier.java`,   `MODIFY egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/idp/admin/oauth/config/OAuthConfig.java; egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/idp/admin/resource/config/ResourceServerConfig.java; egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/idp/admin/resource/controller/ResourceServerController.java; egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/idp/admin/resource/service/ResourceServerService.java; egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/idp/admin/resource/service/impl/ResourceServerServiceImpl.java; egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/idp/admin/resource/domain/dto/CreateResourceServerDTO.java; egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/idp/admin/resource/domain/pojo/IdentityResourceServerEntity.java; egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/idp/admin/resource/domain/vo/ResourceServerVO.java; egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/idp/admin/resource/repo/JpaResourceServerStore.java; egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/idp/admin/resource/service/ResourceServerProjectionService.java; egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/idp/admin/support/bootstrap/IdpDevelopmentClientBootstrap.java`,   `DELETE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/idp/admin/resource/domain/dto/CreateClientJwkDTO.java; egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/idp/admin/resource/domain/pojo/IdentityClientJwkEntity.java; egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/idp/admin/resource/domain/vo/ClientJwkVO.java; egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/idp/admin/resource/repo/IdentityClientJwkRepository.java`,   `MODIFY egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/idp/admin/resource/controller/ResourceServerControllerTest.java; egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/idp/admin/resource/service/impl/ResourceServerServiceImplTest.java; egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/idp/admin/oauth/controller/OAuthResourceSecurityMatrixIT.java; egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/idp/admin/support/bootstrap/IdpDevelopmentClientBootstrapTest.java; egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/idp/admin/support/persistence/IdpPersistenceEntityContractTest.java`,   `DELETE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/idp/admin/oauth/repo/RedisClientAssertionReplayStoreTest.java; egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/idp/admin/oauth/service/impl/PrivateKeyJwtAuthenticatorTest.java; egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/idp/admin/resource/service/impl/ResourceServerAdmissionServiceImplTest.java; egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/idp/admin/support/oauth/LocalServiceAccessTokenSupplierTest.java; egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/idp/admin/resource/support/rpc/ResourceServerAdmissionRpcProviderTest.java`
- Commit: `feat(idp): switch service tokens to client secret basic`

- Execution evidence: corrected RED reached the planned `ClientSecretAuthentication` absence, then the Step 4 reactor command passed
  with 26 tests、0 failures/errors/skips across 31 modules；the clarification slice
  `IdpDevelopmentClientBootstrapTest,IdpPersistenceEntityContractTest` passed with 7 tests；`git diff --check` and the
  declared-path gate passed. Commit `f2fd8230` contains 51 changed Step 4 paths (the unchanged matrix test remains declared but was
  already completed by the prior corrective Step 2 commit). No live DB、service、DDC、Gateway or browser was started.

### Step 5 — 用 Spring Security OAuth2 Client 实现 idp-starter SERVICE Token facade

- Requirements: `REQ-004`, `REQ-005`, `REQ-006`, `REQ-018`
- Dependencies: Step 4
- Execution state: `Committed` at `35d1879d`; no empty or corrective Step 5 commit was created.
- Baseline state: idp-starter只有Resource Server/Jose与private-key Admission Ticket beans，POM反向依赖DDC
  starter和Admission RPC contract。
- Observable outcome: biz service配置标准Spring registration/provider与appId后，通过 `IdpServiceOAuth2Client.authorize`
  获取按完整key隔离、到期前续期的SERVICE Token；starter无私钥/Ticket代码。
- End state: starter POM/autoconfig/facade/tests GREEN；DDC caller尚未切换，留给Step13。
- Test-first gate: Required — facade/autoconfig tests因缺OAuth2 Client dependency/beans和full
  key/single-flight逻辑而RED；旧properties测试要求明确migration error。
- Ordered files:

#### File 1 —
`CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/test/java/top/egon/cola/platform/idp/starter/client/IdpServiceOAuth2ClientTest.java`

- Purpose: 定义授权key、authorize attributes、Basic request、cache/renewal/single-flight与legacy property failure。
- Symbols: `isolatesEveryAuthorizationDimension`, `coalescesConcurrentRenewal`,
  `rejectsLegacyPrivateKeyAndAdmissionProperties`。
- Repository evidence: current auto-config ApplicationContextRunner tests、admission supplier tests、Spring Security test
  stack。
- Dependencies and consumers: new facade/autoconfig/POM；Step13 DDC registration runtime。
- Why now: 先固定标准manager边界和缓存key，避免把custom HTTP supplier伪装成Spring Client。
- Contract/signature changes: `IdpServiceTokenRequest(registrationId,appId,audience,context,tenantId,scopes)`
  ；返回OAuth2AccessToken/封装结果。
- Input/output and state mapping: request→normalized `ServiceAuthorizationKey`→authorize attributes/form
  parameters；AuthorizedClient→token/exp/scopes。
- Error and edge behavior: context/tid矛盾、registration/app/audience/scope差异不共享；manager failure无宽Token
  fallback；legacy keys fail startup且不回显Secret。
- Implementation pseudocode:

```java
build requests that differ one dimension at a time and assert ServiceAuthorizationKey inequality plus normalized scope equality
invoke many concurrent authorize calls for one expired key and assert the manager is called once while every caller receives the same fresh token
capture OAuth2 request conversion and assert client_secret_basic header, resource/tenant/scope form fields and absence of secret/client assertion body data
start ApplicationContextRunner with each legacy private-key/admission property and assert a stable migration exception without logging configured values
```

- Verification contribution: RED/GREEN for `TEST-008/009/023/029`。
- After this file: tests RED because classes/dependency/beans缺失。

#### File 2 —
`MODIFY egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/test/java/top/egon/cola/platform/idp/starter/autoconfigure/IdpStarterAutoConfigurationTest.java`

- Purpose: 修改既有 auto-configuration test，固定 OAuth2 Client beans 与 legacy property migration failure。
- Symbols: `rejectsLegacyPrivateKeyAndAdmissionProperties`、conditional facade bean assertions。
- Repository evidence: 目标测试在续执行基线存在并使用 ApplicationContextRunner；可扩展而无需新测试框架。
- Dependencies and consumers: Files 3–6 POM/properties/autoconfiguration/deletion；Resource Server-only apps 保持。
- Why now: facade RED test 后扩展现有 context test，避免把既有文件误标为创建。
- Contract/signature changes: 新 service-client properties 与 manager/facade beans；retired keys only report names。
- Input/output and state mapping: Spring properties + ClientRegistration repository→conditional beans or stable migration error。
- Error and edge behavior: missing registration/Secret、legacy keys、Resource Server-only mode 均有明确且不泄密的结果。
- Implementation pseudocode:

```java
start ApplicationContextRunner with one valid client registration and assert manager plus IdpServiceOAuth2Client beans are available
start without enabled service client and assert Resource Server beans remain while client facade is absent
bind each retired private-key or Admission property and assert one stable migration exception listing property names but never configured values
```

- Verification contribution: auto-configuration RED/GREEN and legacy failure proof。
- After this file: existing context tests RED for missing properties/beans and undeleted legacy wiring。

#### File 3 —
`MODIFY egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/pom.xml`

- Purpose: 修改 starter POM，引入 Boot-managed OAuth2 Client 并移除只服务于旧 Admission 的反向依赖。
- Symbols: `spring-security-oauth2-client` dependency；移除 DDC starter/Admission RPC contract dependencies。
- Repository evidence: parent管理Spring Security版本；主规格INTERNAL-001；starter现有composition style。
- Dependencies and consumers: File 4 client classes、Spring Boot dependency management；biz/DDC consumers。
- Why now: tests确定contract后添加最少依赖与types，不新增cache server或自签名组件。
- Contract/signature changes: module dependency graph 改为 starter→Spring OAuth2 Client；不新增显式版本或 DDC 反向依赖。
- Input/output and state mapping: Maven dependency graph only；runtime request/state mapping 由 File 4 实现。
- Error and edge behavior: 父 POM 未管理版本时必须回 Plan/parent scope，不得局部硬编码；删除依赖前 main-source import 必须归零。
- Implementation pseudocode:

```java
add the Boot-managed Spring Security OAuth2 Client dependency without an explicit local version
remove DDC starter and Admission RPC contract dependencies only after File 6 deletes every corresponding main-source import
inspect the resulting dependency tree and require no reverse idp-starter to DDC/Admission edge while retaining Jose/Resource Server dependencies
```

- Verification contribution: unblocks File 4 compilation and static dependency inversion proof。
- After this file: dependency graph is ready；client classes remain absent until File 4。

#### File 4 —
`CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/idp/starter/client/IdpServiceTokenRequest.java; egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/idp/starter/client/ServiceAuthorizationKey.java; egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/idp/starter/client/IdpServiceOAuth2Client.java; egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/idp/starter/client/IdpClientCredentialsRequestEntityConverter.java`

- Purpose: 新建窄 facade、immutable request/key 与 OAuth2 request converter。
- Symbols: 四个 `starter.client` types、`authorize`。
- Repository evidence: 四个目标路径在续执行基线均不存在；starter 现有 composition style 与主规格 INTERNAL-001 定义边界。
- Dependencies and consumers: File 3 OAuth2 Client dependency、File 5 autoconfiguration、biz/DDC Step 13。
- Why now: dependency ready 后创建 replacement，避免先删除旧 supplier 导致无可编译替代。
- Contract/signature changes: facade 唯一 public method 接 typed request；Secret 仅在 ClientRegistration 配置。
- Input/output and state mapping: registration/app/audience/context/tenant/sorted scopes→key/attributes→authorized Bearer token。
- Error and edge behavior: null/expired/scope mismatch/timeout stable unavailable；per-key single-flight；不同 key 并行且不串用。
- Implementation pseudocode:

```java
validate IdpServiceTokenRequest dimensions and create a ServiceAuthorizationKey containing no secret or token material
normalize scopes, check cached token against renewal skew and single-flight one OAuth2AuthorizedClientManager authorize call per full key
write resource, tenant_id only for TENANT and scopes through the converter while Spring supplies the client_secret_basic header
```

- Verification contribution: makes facade key/cache/converter tests GREEN。
- After this file: core facade compiles；autoconfiguration and legacy deletion remain。

#### File 5 —
`MODIFY egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/idp/starter/autoconfigure/IdpStarterProperties.java; egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/idp/starter/autoconfigure/IdpStarterAutoConfiguration.java; egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/idp/starter/package-info.java; egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/idp/starter/autoconfigure/package-info.java`

- Purpose: 修改既有 properties/autoconfiguration，装配 manager/facade 并把 legacy keys 变为明确 migration error。
- Symbols: `service-client.app-id/registration-id/renewal-skew`、manager/facade beans、legacy migration validator。
- Repository evidence: current properties大量admission/private-key fields；auto-config注册Ticket supplier；POM
  imports仅旧admission使用DDC/RPC contract。
- Dependencies and consumers: Files 3–4 Spring OAuth2 Client/facade；DDC Step 13；Resource Server beans 保持。
- Why now: File 4 replacement ready 后重接既有配置；物理删除由 File 6 单独执行。
- Contract/signature changes: 新properties fail-fast appId/registration；旧property names在一个迁移版本明确异常；Admission
  beans不存在。
- Input/output and state mapping: `spring.security.oauth2.client.registration.*`保管key/secret；
  `egon...service-client.app-id`补业务identity；无private key file读取。
- Error and edge behavior: missing registration/Secret由Spring/validator启动失败；legacy property只报告key名不报告值；Resource
  Server-only app可不启用client facade。
- Implementation pseudocode:

```java
conditionally create AuthorizedClientService, manager and IdpServiceOAuth2Client only when enabled and the named ClientRegistration exists
bind appId, registrationId and renewalSkew, and inspect the Environment for the retired private-key and Admission property families
throw one migration exception listing retired key names without values while preserving Resource Server-only auto-configuration behavior
```

- Verification contribution: makes ApplicationContextRunner GREEN and establishes replacement wiring。
- After this file: starter provides the standard facade；legacy source/tests remain for File 6 deletion。

#### File 6 —
`DELETE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/idp/starter/admission/CachingDdcAdmissionTicketSupplier.java; egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/idp/starter/admission/OwnerOnlyPrivateKeyLoader.java; egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/idp/starter/admission/PrivateKeyJwtAssertionFactory.java; egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/idp/starter/admission/RpcResourceServerAdmissionClient.java; egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/idp/starter/admission/package-info.java; egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/test/java/top/egon/cola/platform/idp/starter/admission/CachingDdcAdmissionTicketSupplierTest.java; egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/test/java/top/egon/cola/platform/idp/starter/admission/OwnerOnlyPrivateKeyLoaderTest.java; egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/test/java/top/egon/cola/platform/idp/starter/admission/PrivateKeyJwtAdmissionAssertionFactoryTest.java; egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/test/java/top/egon/cola/platform/idp/starter/admission/RpcResourceServerAdmissionClientTest.java`

- Purpose: 物理删除旧 Admission/private-key sources 与仅验证这些实现的 tests。
- Symbols: Ticket supplier、private key loader/assertion factory、Admission RPC client/package 与四个 tests 全部 absent。
- Repository evidence: 九个路径在续执行基线存在；Files 4–5 已提供 replacement，POM cleanup 在 File 3 声明。
- Dependencies and consumers: Step 13 DDC 使用新 facade；Resource Server verification beans 不在 deletion set。
- Why now: replacement/wiring ready 后删除双轨，不保留 deprecated wrapper 或空壳 tests。
- Contract/signature changes: Admission beans/types 不再存在；legacy property names 由 File 5 明确失败。
- Input/output and state mapping: Not applicable — 删除私钥/Ticket读取与缓存路径，不迁移 runtime state。
- Error and edge behavior: 搜索到隐藏 consumer 必须在声明 scope 内迁移；Secret/property values 不得出现在异常或日志。
- Implementation pseudocode:

```text
delete all listed admission production sources after the OAuth2 Client facade and auto-configuration compile
delete their dedicated tests because the implementation contract is retired and negative migration behavior is covered by File 2
search main and test imports plus the dependency tree for Ticket, private-key and Admission RPC references and require zero hits
```

- Verification contribution: static dependency/symbol deletion and `TEST-029`。
- After this file: idp-starter contains no Admission/private-key code and uses only the standard OAuth2 Client facade。

- Validation working directory: `/Users/mario/SelfProject/Egon-COLA`
- Verification command:
  `./mvnw -B -ntp -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter -am -Dtest=IdpServiceOAuth2ClientTest,IdpStarterAutoConfigurationTest -Dsurefire.failIfNoSpecifiedTests=false test`
- Expected result: exit 0；full-key隔离/single-flight/Basic converter/autoconfig/legacy failure通过；POM dependency
  tree无DDC starter或Admission RPC contract。
- Failure returns to: File1 contract；File2 manager/facade；File3 property/wiring/dependency cleanup。
- Completion criteria: biz service只需标准registration/provider+appId；starter不含签名/JWK/Ticket路径。
- Rollback: revert三组paths；已分发Secret按安全策略继续轮换，不能重新提交私钥材料。
- Commit paths: `CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/test/java/top/egon/cola/platform/idp/starter/client/IdpServiceOAuth2ClientTest.java`,   `MODIFY egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/test/java/top/egon/cola/platform/idp/starter/autoconfigure/IdpStarterAutoConfigurationTest.java`,   `MODIFY egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/pom.xml`,   `CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/idp/starter/client/IdpServiceTokenRequest.java; egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/idp/starter/client/ServiceAuthorizationKey.java; egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/idp/starter/client/IdpServiceOAuth2Client.java; egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/idp/starter/client/IdpClientCredentialsRequestEntityConverter.java`,   `MODIFY egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/idp/starter/autoconfigure/IdpStarterProperties.java; egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/idp/starter/autoconfigure/IdpStarterAutoConfiguration.java; egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/idp/starter/package-info.java; egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/idp/starter/autoconfigure/package-info.java`,   `DELETE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/idp/starter/admission/CachingDdcAdmissionTicketSupplier.java; egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/idp/starter/admission/OwnerOnlyPrivateKeyLoader.java; egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/idp/starter/admission/PrivateKeyJwtAssertionFactory.java; egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/idp/starter/admission/RpcResourceServerAdmissionClient.java; egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/idp/starter/admission/package-info.java; egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/test/java/top/egon/cola/platform/idp/starter/admission/CachingDdcAdmissionTicketSupplierTest.java; egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/test/java/top/egon/cola/platform/idp/starter/admission/OwnerOnlyPrivateKeyLoaderTest.java; egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/test/java/top/egon/cola/platform/idp/starter/admission/PrivateKeyJwtAdmissionAssertionFactoryTest.java; egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/test/java/top/egon/cola/platform/idp/starter/admission/RpcResourceServerAdmissionClientTest.java`
- Commit: `feat(idp-starter): add spring oauth2 service client facade`

- Execution evidence: Step 5 RED stopped at the absent Spring OAuth2 Client dependency/facade and the expected old Admission
  consumers；after adding the Boot-managed OAuth2 Client manager/converter, full-key cache and per-key single-flight, the exact
  starter command passed with 10 tests、0 failures/errors/skips；dependency tree contains `spring-security-oauth2-client` and no
  DDC starter/Admission RPC edge；`git diff --check` and declared-path gate passed. Commit `35d1879d` contains the 20 declared
  paths. No live IdP/Token endpoint, service, DDC, database or browser was started.

### Step 6 — 实现 IdP tenant 与 membership 本地权威服务

- Requirements: `REQ-011`, `REQ-014`, `REQ-020`
- Dependencies: Step 1
- Execution state: `Committed` at `39023dfb`; no empty or corrective Step 6 commit was created.
- Baseline state: IdP无tenant package/entity/repository；V5提供目标表；current membership来自RBAC HTTP。
- Observable outcome: IdP service在本地事务内创建/更新tenant、upsert membership、按identity/tenant解析effective
  status，保持ID/status/settings/version规则。
- End state: persistence/service unit/integration GREEN；HTTP/local port/RPC尚未暴露。
- Test-first gate: Required — tenant service tests因types/repositories/service不存在而RED。
- Ordered files:

#### File 1 —
`CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/idp/admin/tenant/service/impl/TenantServiceImplTest.java; egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/idp/admin/tenant/service/impl/TenantMembershipServiceImplTest.java`

- Purpose: 定义tenant lifecycle、optimistic lock、membership upsert/effective status/local authority。
- Symbols: create/update/status transitions、duplicate code/pair、inactive tenant/user/member、same-command idempotence。
- Repository evidence: IdentityUserServiceImplTest/OAuthClientServiceImplTest fixtures、Clock/LongIdGenerator/JPA
  conventions。
- Dependencies and consumers: V5 entities/repos/services；Steps7/8。
- Why now: test-first固定权威判定与state transitions再写持久层。
- Contract/signature changes: service commands/results按主规格API-005–009/§10，内部不出现RBAC ID。
- Input/output and state mapping: tenant DTO fields→entity；identitySub validates existing identity_user；entity
  joins→effective ACTIVE/DISABLED。
- Error and edge behavior: CLOSED terminal；SUSPENDED/inactive user/disabled membership effective
  disabled；version/duplicate conflict stable；transaction failure无partial pair。
- Implementation pseudocode:

```java
create tenant with generated decimal-string id and immutable normalized code, then assert INITIALIZING or requested valid state and version zero
upsert one identitySub membership after requiring existing identity_user, asserting same command is stable and changed status increments version
resolve effective membership across tenant, identity user and membership states, returning ACTIVE only when all three permit access
race two updates with one expectedVersion and assert exactly one commits while the loser receives the stable conflict
```

- Verification contribution: RED/GREEN `TEST-014/015/016/022` service slice。
- After this file: tests RED for absent productiontypes。

#### File 2 —
`CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/idp/admin/tenant/domain/pojo/IdentityTenantEntity.java; egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/idp/admin/tenant/domain/pojo/IdentityTenantMembershipEntity.java; egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/idp/admin/tenant/repo/IdentityTenantRepository.java; egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/idp/admin/tenant/repo/IdentityTenantMembershipRepository.java`

- Purpose: 映射V5 authority表与lock/query paths。
- Symbols: tenant/member states/factories/transitions；find page/by code/by id；find membership/list by subject/tenant
  with locks。
- Repository evidence: IdP feature JPA naming/audit/version style与V5 exact schema。
- Dependencies and consumers: service impl；IdentityUserEntity FK；no RBAC dependency。
- Why now: RED tests确定state semantics后创建最小persistence model。
- Contract/signature changes: new entities/repositories only；no public transport exposure。
- Input/output and state mapping: varchar IDs/status/settings JSON/version/timestamps→Java types；membership composite
  uniqueness由DB+service映射。
- Error and edge behavior: physical delete absent；CLOSED/DISABLED history retained；repository lock用于status/version
  updates；orphan identitySub由FK拒绝。
- Implementation pseudocode:

```java
IdentityTenantEntity owns immutable id/code, mutable name/status/settings, optimistic version and audit instants with explicit legal transitions
IdentityTenantMembershipEntity owns tenantId plus identitySub, ACTIVE or DISABLED status, version and audit without any RBAC user identifier
repositories expose deterministic id/code/page and tenant+subject lookup, including PESSIMISTIC_WRITE methods only for mutation paths
map unique/FK/optimistic exceptions at the service boundary rather than leaking persistence messages
```

- Verification contribution: service/persistence tests observe mappings/constraints。
- After this file: persistence compiles；services absent仍RED。

#### File 3 —
`CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/idp/admin/tenant/service/TenantService.java; egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/idp/admin/tenant/service/TenantMembershipService.java; egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/idp/admin/tenant/service/impl/TenantServiceImpl.java; egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/idp/admin/tenant/service/impl/TenantMembershipServiceImpl.java`

- Purpose: 实现tenant/member事务、effective read model和transport-neutral results。
- Symbols: list/create/update/get；listMemberships/upsert/resolve；`TenantMembershipProfile` internal value。
- Repository evidence: existing Controller→Service interface→ServiceImpl→Repository feature flow；LongIdGenerator already
  returns compatible numeric IDs。
- Dependencies and consumers: File2 repos、IdentityUserDirectory、Clock/ID generator；Steps7/8。
- Why now: persistence ready后编排business invariant，保持Controller/RPC共用同一service。
- Contract/signature changes: new service interfaces/results；no HTTP/RPC annotations，no Entity return。
- Input/output and state mapping: commands→entities→safe profiles；effective status joins three owners；settings canonical
  JSON bound。
- Error and edge behavior: validate/permission留controller；service handles not-found/conflict/state/transaction；no
  cross-DB call/cache。
- Implementation pseudocode:

```java
@Transactional createTenant normalizes code, allocates decimal id, rejects duplicate code and persists one authoritative tenant
@Transactional updateTenant locks by id, checks expectedVersion and legal state transition, applies name/settings/status and returns safe view
@Transactional upsertMembership requires tenant and identity user, inserts or changes ACTIVE/DISABLED with expectedVersion and no physical delete
@Transactional(readOnly=true) resolve joins tenant/user/member states and returns one TenantMembershipProfile used identically by login, HTTP and RPC
```

- Verification contribution: makes both File1 tests GREEN。
- After this file: IdP local authority service ready；no external endpoints yet。

- Validation working directory: `/Users/mario/SelfProject/Egon-COLA`
- Verification command:
  `./mvnw -B -ntp -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=TenantServiceImplTest,TenantMembershipServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test`
- Expected result: exit 0；lifecycle/conflict/effective status/local-only behavior通过。
- Failure returns to: File1 contract；File2 mapping/query；File3 service transaction/state。
- Completion criteria: IdP DB/service成为tenant/member唯一writer/read model，模型无RBAC ID。
- Rollback: revert三组paths；若V5已有导入数据，应用rollback不得删除数据，按snapshot/forward-fix处理。
- Commit paths: `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/idp/admin/tenant/service/impl/TenantServiceImplTest.java; egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/idp/admin/tenant/service/impl/TenantMembershipServiceImplTest.java`,   `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/idp/admin/tenant/domain/pojo/IdentityTenantEntity.java; egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/idp/admin/tenant/domain/pojo/IdentityTenantMembershipEntity.java; egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/idp/admin/tenant/repo/IdentityTenantRepository.java; egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/idp/admin/tenant/repo/IdentityTenantMembershipRepository.java`,   `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/idp/admin/tenant/service/TenantService.java; egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/idp/admin/tenant/service/TenantMembershipService.java; egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/idp/admin/tenant/service/impl/TenantServiceImpl.java; egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/idp/admin/tenant/service/impl/TenantMembershipServiceImpl.java`
- Commit: `feat(idp): own tenant and membership state`

- Execution evidence: Step 6 RED 在 admin `testCompile` 因 tenant entity/repository/service types 不存在而失败；补齐 V5 对应
  JPA entity/repository、optimistic lifecycle、membership upsert/effective profile 与 local-only transactional services
  后，精确 Maven 命令通过，5 tests、0 failures/errors/skips；`git diff --check` 与 10-file declared-path gate 通过。
  提交 `39023dfb` 仅包含本 Step 声明的 10 个路径。现有 `HttpTenantMembershipAdapter` deprecation warning 属于下一 Step
  删除范围；未启动 live PostgreSQL、IdP/RBAC 服务、RPC、DDC 或浏览器。

### Step 7 — 暴露 IdP tenant API 并把登录 membership 切到本地 port

- Requirements: `REQ-011`, `REQ-016`, `REQ-018`, `REQ-020`
- Dependencies: Steps 2 and 6
- Execution state: `Committed` at `fd31f5ba`; no empty or corrective Step 7 commit was created.
- Baseline state: tenant service已本地可用；无HTTP DTO/VO/controller；OAuthConfig仍装配HttpTenantMembershipAdapter与旧RBAC
  URL/token配置。
- Observable outcome: API-005–009由existing admin authorization保护；TokenFacade/login注入local
  TenantMembershipPort；IdP无RBAC membership HTTP/config/token supplier。
- End state: backend contract/local login path GREEN；frontend待Step16，RPC待Step8。
- Test-first gate: Required — controller/local-port/config tests当前因routes不存在且Http adapter仍装配而RED。
- Ordered files:

#### File 1 —
`CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/idp/admin/tenant/controller/TenantControllerTest.java; egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/idp/admin/tenant/service/LocalTenantMembershipPortTest.java`

- Purpose: 定义五个HTTP contracts、exact permissions、stable ordering/errors和login/resolve local-only behavior。
- Symbols: API-005–009 tests；`listsAndResolvesWithoutRbacHttp`。
- Repository evidence: ResourceServer/OAuthClient controller tests与TokenFacade/current Http adapter tests。
- Dependencies and consumers: tenant services、AdminAuthorization、TenantMembershipPort/OAuthConfig。
- Why now: 先固定transport和local boundary后添加DTO/controller/adapter。
- Contract/signature changes: routes/inputs/outputs完全按主规格；permissions `idp:tenant:read/manage`；local port reduced
  record。
- Input/output and state mapping: JSON DTO→service commands→VO；service profile→port record；operator subject→audit。
- Error and edge behavior: validation 400、permission 403、missing 404、version/duplicate 409；inactive effective
  status返回/拒绝按contract；无HTTP fallback。
- Implementation pseudocode:

```java
exercise tenant list/create/update and membership list/upsert with exact request and response fields, deterministic id order and stable wrappers
verify read operations require idp:tenant:read while every mutation requires idp:tenant:manage and records the current operator subject
call TenantMembershipPort list/resolve against in-memory repositories and assert only identitySub, tenantId, displayName and effective status are returned
remove/mock no RBAC HTTP client and assert missing local membership fails closed rather than attempting a network fallback
```

- Verification contribution: RED/GREEN `TEST-016/017/026`。
- After this file: tests RED for absent controller/adapter。

#### File 2 —
`CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/idp/admin/tenant/domain/dto/CreateTenantDTO.java; egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/idp/admin/tenant/domain/dto/UpdateTenantDTO.java; egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/idp/admin/tenant/domain/dto/UpsertTenantMembershipDTO.java; egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/idp/admin/tenant/domain/vo/TenantVO.java; egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/idp/admin/tenant/domain/vo/TenantMembershipVO.java; egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/idp/admin/tenant/controller/TenantController.java`

- Purpose: 实现API-005–009 transport/authorization/mapping。
- Symbols: `/api/v1/identity/tenants` list/create/update；`/{tenantId}/memberships` list/upsert。
- Repository evidence: feature-first IdP controllers、validation annotations、AdminAuthorization、GatewayOperation
  metadata。
- Dependencies and consumers: TenantService/TenantMembershipService；Step16 Web；no Entity exposure。
- Why now: File1固定wire/permissions后写最小transport。
- Contract/signature changes: 新HTTP routes/DTO/VO；list的page/filter/sort/null行为按Spec；no tenantId in mutation body
  when path owns it。
- Input/output and state mapping: path/query/body/principal→service；service view→VO；settings JSON保留schema bound。
- Error and edge behavior: authorization before detail disclosure；server maps state/duplicate/version errors；no
  plaintext/credential data。
- Implementation pseudocode:

```java
controller list validates page/size/filter and requires idp:tenant:read before delegating to TenantService deterministic query
controller create/update requires idp:tenant:manage, derives operator from principal and maps validated DTO plus expectedVersion to service
membership list requires read; upsert requires manage and maps path tenantId plus identitySub/status/expectedVersion without caller-supplied authority facts
return repository-standard success/error wrappers and preserve not-found versus conflict semantics from the Spec
```

- Verification contribution: controller tests GREEN；Step16 typed client source。
- After this file: tenant HTTP available；login port待File3。

#### File 3 —
`CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/idp/admin/tenant/service/LocalTenantMembershipPort.java`

- Purpose: 新建 local port adapter，让 TokenFacade/login 从 Step 6 IdP service 读取 membership。
- Symbols: `LocalTenantMembershipPort implements TenantMembershipPort`。
- Repository evidence: current OAuthConfig显式构造Http adapter和LocalServiceAccessTokenSupplier；Step2 reduced port；Step6
  service profile。
- Dependencies and consumers: Step 6 TenantMembershipService、TokenFacade/Login；File 4 wiring。
- Why now: HTTP service ready后用同一read model替换旧adapter，避免两套判定。
- Contract/signature changes: 新 adapter 实现 reduced membership record；external login token contract 保持。
- Input/output and state mapping: TenantMembershipProfile→core port record；effective disabled/missing→empty/disabled；no
  rbac3UserId mapping。
- Error and edge behavior: DB error映射membership unavailable/fail closed；旧properties存在时配置测试/Step18 static
  gate拒绝；无fallback。
- Implementation pseudocode:

```java
LocalTenantMembershipPort.list delegates to TenantMembershipService.listByIdentity and maps each profile to the reduced core record
resolve delegates to the same effective-status query used by HTTP/RPC and returns empty for missing while preserving explicit DISABLED
propagate repository failures so TokenFacade fails closed and never perform RBAC HTTP, SERVICE token or internal-id mapping
```

- Verification contribution: local port/Login behavior GREEN。
- After this file: local adapter exists；wiring/config and old adapter deletion remain。

#### File 4 —
`MODIFY egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/idp/admin/oauth/config/OAuthConfig.java; egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/resources/application.yml; egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/resources/application-local.yml`

- Purpose: 修改既有 wiring/YAML，切换 local port 并删除 RBAC membership HTTP 配置。
- Symbols: OAuthConfig local bean；retired base URL/timeout/service-token keys absent。
- Repository evidence: 三个目标路径在续执行基线存在，当前显式装配旧 adapter 和相关 properties。
- Dependencies and consumers: File 3 local adapter、TokenFacade；Step 8 RPC provider 复用同一 authoritative service。
- Why now: replacement adapter ready 后原地切 wiring/config，避免 local/HTTP 两套判定并存。
- Contract/signature changes: internal bean implementation 改为 local；移除 WebClient/baseUrl/timeout/serviceTokenSupplier 配置。
- Input/output and state mapping: IdP repository membership→local port→TokenFacade；不再构造 RBAC HTTP request。
- Error and edge behavior: RBAC unavailable 不影响 login；旧 keys 明确归零/拒绝且异常只列 key name 不列 value。
- Implementation pseudocode:

```java
inject LocalTenantMembershipPort into TokenFacade and remove every HttpTenantMembershipAdapter or WebClient collaborator from OAuthConfig
remove RBAC membership URL, timeout, assertion and local service-token settings from application.yml and application-local.yml
start the focused context without an RBAC HTTP mock and require local login success plus deterministic failure for any retired key
```

- Verification contribution: local-only wiring and static no-old-config proof。
- After this file: TokenFacade uses local authority；obsolete adapter source remains for File 5 deletion。

#### File 5 —
`DELETE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/idp/admin/support/rbac3/HttpTenantMembershipAdapter.java`

- Purpose: 物理删除不再装配的 IdP→RBAC membership HTTP adapter。
- Symbols: `HttpTenantMembershipAdapter` absent。
- Repository evidence: target exists at resume baseline；File 4 removes its only production wiring and config consumers。
- Dependencies and consumers: none after File 4；Step 18 old-symbol/config gate。
- Why now: local replacement is wired and tested，so dual authority adapter must be deleted rather than deprecated。
- Contract/signature changes: internal adapter type disappears；no external API contract changes。
- Input/output and state mapping: Not applicable — network adapter deletion，不迁移 local authority data。
- Error and edge behavior: any remaining import is a hard compile failure；do not restore fallback/no-op HTTP implementation。
- Implementation pseudocode:

```text
delete HttpTenantMembershipAdapter.java after OAuthConfig and YAML no longer reference its collaborators
search main and test sources for the class name, RBAC membership URL and local service-token property family
require zero production matches and compile login/local-port tests so hidden consumers fail before commit
```

- Verification contribution: `TEST-029` physical old-network-path deletion。
- After this file: IdP tenant/member backend/API/login is fully local；RPC remains for Step 8。

- Validation working directory: `/Users/mario/SelfProject/Egon-COLA`
- Verification command:
  `./mvnw -B -ntp -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=TenantControllerTest,LocalTenantMembershipPortTest,TokenFacadeTest -Dsurefire.failIfNoSpecifiedTests=false test`
- Expected result: exit 0；API permissions/mapping与local login membership通过；main/resources/Java search无RBAC
  membership HTTP配置/adapter。
- Failure returns to: File1 contract；File2 HTTP/permission；File3 local mapping；File4 wiring/config；File5 stale consumer。
- Completion criteria: API-005–009与local membership authority完成，无IdP→RBAC membership network path。
- Rollback: revert五组paths；V5 tenant data保留或snapshot restore，不重新启用双权威HTTP。
- Commit paths: `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/idp/admin/tenant/controller/TenantControllerTest.java; egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/idp/admin/tenant/service/LocalTenantMembershipPortTest.java`,   `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/idp/admin/tenant/domain/dto/CreateTenantDTO.java; egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/idp/admin/tenant/domain/dto/UpdateTenantDTO.java; egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/idp/admin/tenant/domain/dto/UpsertTenantMembershipDTO.java; egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/idp/admin/tenant/domain/vo/TenantVO.java; egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/idp/admin/tenant/domain/vo/TenantMembershipVO.java; egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/idp/admin/tenant/controller/TenantController.java`,   `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/idp/admin/tenant/service/LocalTenantMembershipPort.java; egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/idp/admin/oauth/config/OAuthConfig.java; egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/resources/application.yml; egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/resources/application-local.yml; egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/idp/admin/support/rbac3/HttpTenantMembershipAdapter.java`
- Commit: `feat(idp): expose local tenant authority`

- Execution evidence: Step 7 RED 在 admin `testCompile` 因 `LocalTenantMembershipPort` 与 `TenantController` 不存在而失败；新增
  API-005–009 DTO/VO/controller、分页/状态过滤与 read/manage 权限、Adapter 形式的 local membership port，OAuthConfig
  改为本地 bean，删除旧 RBAC membership HTTP adapter，并清除 application.yml 中 admission/private-key/RBAC service-token
  配置后，精确 Maven 命令通过：TenantController 4、LocalTenantMembershipPort 3、TokenFacade 7 tests，均 0
  failures/errors/skips；`git diff --check` 与 declared-path gate 通过。`application-local.yml` 原无旧 membership/service-token
  keys，故保持内容不变；提交 `fd31f5ba` 含 12 个实际变更路径。未启动 live PostgreSQL、IdP/RBAC 服务、RPC、DDC 或浏览器。

### Step 8 — 扩展 Identity Directory RPC 并删除 Admission RPC contract

- Requirements: `REQ-011`, `REQ-013`, `REQ-018`, `REQ-021`
- Dependencies: Steps 6–7
- Execution state: `Committed` at `08f3d8c6`; no empty or corrective Step 8 commit was created.
- Baseline state: identity_directory仅BatchGetIdentityProfiles；Admission proto/Java
  contract仍存在；provider只注入IdentityUserDirectory。
- Observable outcome: existing Identity Directory additive提供GetTenantMembership，返回tenant/user/member
  statuses/version；Admission proto/contract物理删除，Maven generation与旧profile method兼容。
- End state: RPC contract/provider/tests GREEN；RBAC adapter待Step11。
- Test-first gate: Required — contract/provider tests因method/messages不存在而RED；deletion test要求Admission type不可加载。
- Ordered files:

#### File 1 —
`MODIFY egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-rpc-contract/src/test/java/top/egon/cola/platform/idp/rpc/contract/IdentityDirectoryRpcContractTest.java; egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/idp/admin/identity/support/rpc/IdentityDirectoryRpcProviderTest.java`

- Purpose: 修改既有 Identity Directory contract/provider tests，定义 additive membership RPC。
- Symbols: `GetTenantMembership` descriptor/method/status mapping；existing batch profiles unchanged。
- Repository evidence: current contract/provider tests使用RPC validation和protobuf builders；Step6 service
  fixture提供statuses。
- Dependencies and consumers: modified proto/interface/provider；Step11 RBAC reference。
- Why now: test-first固定protocol field numbers/mapping与旧method兼容。
- Contract/signature changes: request tenant_id+identity_sub；response profile statuses/version；missing使用NOT_FOUND；no
  credentials/permissions/RBAC ID。
- Input/output and state mapping: service profile→proto fields；missing→found false or NOT_FOUND perSpec chosen
  contract；deadline由consumer。
- Error and edge behavior: blank/duplicate/malformed inputs INVALID_ARGUMENT；service unavailable propagates RPC
  failure；inactive仍返回facts供consumer fail closed。
- Implementation pseudocode:

```java
validate IdentityDirectoryRpc descriptor still exposes BatchGetIdentityProfiles with unchanged method identity and now adds GetTenantMembership
request active, disabled and missing tenant/member and assert complete tenant/user/membership statuses plus membership version with no RBAC id; missing maps to NOT_FOUND
assert provider validates trimmed decimal tenantId and identitySub before service lookup and never maps credentials or permission facts
retain the existing batch profile assertions byte-for-byte in behavior while adding membership validation and mapping coverage
```

- Verification contribution: RED/GREEN `TEST-019/025` membership RPC contract slice。
- After this file: tests RED for missing proto/method until Files3–5。

#### File 2 —
`DELETE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-rpc-contract/src/test/java/top/egon/cola/platform/idp/rpc/contract/ResourceServerAdmissionRpcContractTest.java`

- Purpose: 删除仅验证 retired Admission RPC contract 的测试类；provider test 已按 PLAN-CLAR-014 在 Step 4 删除。
- Symbols: Admission RPC contract test absent。
- Repository evidence: contract test 在续执行基线存在；Step 4 已删除 provider production class及其唯一 provider test，File 4 将删除 contract sources。
- Dependencies and consumers: File 4 source deletion；Step 18 global old-symbol gate。
- Why now: membership tests 已独立保留，retired protocol tests 应删除而非改写为 compatibility response。
- Contract/signature changes: Admission RPC 不再拥有测试 contract；wire absence 由 source/class search 证明。
- Input/output and state mapping: Not applicable — deleted protocol tests have no runtime state。
- Error and edge behavior: shared RPC harness behavior 必须留在 Identity Directory tests；不得用 ignored/disabled tests 掩盖旧 contract。
- Implementation pseudocode:

```text
delete ResourceServerAdmissionRpcContractTest after its contract is marked for physical deletion
assert ResourceServerAdmissionRpcProviderTest is already absent from Step 4 and do not recreate or re-delete it
retain common validation or provider harness coverage in IdentityDirectoryRpcContractTest and require no Admission imports
```

- Verification contribution: `TEST-029` retired RPC test absence。
- After this file: no test requires the Admission protocol；membership tests remain RED for missing method。

#### File 3 —
`MODIFY egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-rpc-contract/src/main/proto/identity_directory.proto; egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-rpc-contract/src/main/java/top/egon/cola/platform/idp/rpc/contract/IdentityDirectoryRpc.java`

- Purpose: 扩展既有 Identity Directory proto/Java service，保持 batch compatibility。
- Symbols: `GetTenantMembershipRequest/Response`、`TenantMembershipProfile`、Java method；VERSION保持1.0.0。
- Repository evidence: existing proto usesjava_multiple_files/package v1；Spec additive compatibility；PLAN-CLAR-001。
- Dependencies and consumers: protobuf Maven plugin生成target sources；provider/RBAC compile againstJava interface。
- Why now: File1固定descriptor后修改唯一source-of-truth，generated files不手改/不提交。
- Contract/signature changes: one new RPC method/messages with append-only field numbers；Admission deletion由File 4负责。
- Input/output and state mapping: string IDs/statuses + int64 versions；no HTTP URL或RBAC data。
- Error and edge behavior: old clients继续调用batch；新clients在旧provider获得unimplemented，发布顺序先provider后consumer；Admission
  clients必须在cutover前归零。
- Implementation pseudocode:

```proto
service IdentityDirectoryService keeps BatchGetIdentityProfiles and adds GetTenantMembership with new request and response messages
request carries tenant_id and identity_sub; response carries found plus tenant_status, identity_status, membership_status and membership_version
append field numbers without renumbering existing profile messages, keep java package and service version identity unchanged
generate only from the additive Identity Directory source and keep generated target files out of Git
```

- Verification contribution: protobuf generation/contract test。
- After this file: additive generated contract compiles；provider method未实现，Admission source remains until File 4。

#### File 4 —
`DELETE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-rpc-contract/src/main/proto/resource_server_admission.proto; egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-rpc-contract/src/main/java/top/egon/cola/platform/idp/rpc/contract/ResourceServerAdmissionRpc.java`
`MODIFY egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-rpc-contract/src/main/java/top/egon/cola/platform/idp/rpc/contract/package-info.java; egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-rpc-contract/src/main/java/top/egon/cola/platform/idp/rpc/contract/proto/v1/package-info.java`

- Purpose: 物理删除旧 Admission proto/Java contract，并同步生成包文档，使 generation 无法产生 stale clients/providers。
- Symbols: `ResourceServerAdmissionRpc` and its proto service absent。
- Repository evidence: 两个 targets 在续执行基线存在；Step 4/本 Step File 2 已删除 provider/tests consumers。
- Dependencies and consumers: Maven protobuf generation；Step 18 old-symbol inventory。
- Why now: additive replacement contract ready and consumers zero，so retired protocol can be removed without dual track。
- Contract/signature changes: Admission RPC identity disappears；Identity Directory version/method identity remains unchanged。
- Input/output and state mapping: Not applicable — contract source deletion，不迁移 database/runtime state。
- Error and edge behavior: any generated or handwritten import remaining must fail compile；do not retain unimplemented compatibility service。
- Implementation pseudocode:

```text
delete resource_server_admission.proto and ResourceServerAdmissionRpc.java after all provider, client and test consumers are removed
clean generated target output through the normal Maven lifecycle without staging target files
search source and dependency outputs for the retired service identity and require zero source-level matches
```

- Verification contribution: protobuf generation and `TEST-029` source/class absence。
- After this file: Admission protocol source is absent；Identity Directory provider remains for File 5。

#### File 5 —
`MODIFY egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/idp/admin/identity/support/rpc/IdentityDirectoryRpcProvider.java`

- Purpose: 用Step6同一TenantMembershipService提供RPC-001。
- Symbols: `getTenantMembership(GetTenantMembershipRequest)`；existing batch method unchanged。
- Repository evidence: provider已有@EgonRpcProvider/validation/mapping；main service可组合第二dependency。
- Dependencies and consumers: IdentityUserDirectory + TenantMembershipService；RBAC Step11。
- Why now: contract生成后实现provider，避免parallel protocol。
- Contract/signature changes: constructor增加TenantMembershipService；implements新增method。
- Input/output and state mapping: request→service resolve raw/effective facts→proto；found false without
  fabricatedstatuses。
- Error and edge behavior: input validation beforeDB；DB/unexpected error成为RPC unavailable/internal；no cache/allow
  fallback。
- Implementation pseudocode:

```java
provider constructor composes IdentityUserDirectory and TenantMembershipService while retaining existing batch profile logic byte-for-byte in behavior
getTenantMembership validates one decimal tenantId and one trimmed identitySub, then queries the shared authoritative service
map present profile to all three statuses and version; map absence to NOT_FOUND with no placeholder values
allow repository/runtime failure to fail the RPC so RBAC can reject writes rather than converting failure into inactive success
```

- Verification contribution: provider tests GREEN；RBAC consumer contract unblocked。
- After this file: Identity Directory additive RPC可用；Admission RPC source absent。

- Validation working directory: `/Users/mario/SelfProject/Egon-COLA`
- Verification command:
  `./mvnw -B -ntp -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-rpc-contract,egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=IdentityDirectoryRpcContractTest,IdentityDirectoryRpcProviderTest -Dsurefire.failIfNoSpecifiedTests=false test`
- Expected result: exit 0；protobuf generation成功，existing profile与new membership tests通过，Admission contract
  source/class references归零。
- Failure returns to: File1 membership assertions；File2 obsolete tests；File3 additive contract；File4 retired source；File5 provider mapping。
- Completion criteria: RPC-001 additive发布单元完成且旧Admission protocol不再生成。
- Rollback: revert三组paths；consumer尚未部署时安全；部署后按provider-before-consumer兼容顺序rollback。
- Commit paths: `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-rpc-contract/src/test/java/top/egon/cola/platform/idp/rpc/contract/IdentityDirectoryRpcContractTest.java; egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/idp/admin/identity/support/rpc/IdentityDirectoryRpcProviderTest.java; egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-rpc-contract/src/test/java/top/egon/cola/platform/idp/rpc/contract/ResourceServerAdmissionRpcContractTest.java`,   `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-rpc-contract/src/main/proto/identity_directory.proto; egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-rpc-contract/src/main/java/top/egon/cola/platform/idp/rpc/contract/IdentityDirectoryRpc.java`,   `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-rpc-contract/src/main/proto/resource_server_admission.proto; egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-rpc-contract/src/main/java/top/egon/cola/platform/idp/rpc/contract/ResourceServerAdmissionRpc.java`,   `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-rpc-contract/src/main/java/top/egon/cola/platform/idp/rpc/contract/package-info.java; egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-rpc-contract/src/main/java/top/egon/cola/platform/idp/rpc/contract/proto/v1/package-info.java`,   `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/idp/admin/identity/support/rpc/IdentityDirectoryRpcProvider.java`
- Commit: `feat(idp-rpc): expose tenant membership and remove admission contract`

- Execution evidence: Step 8 RED 首先在 contract descriptor 中因新增 RPC method 尚不存在而失败；随后补齐 append-only
  `GetTenantMembership` protobuf/messages/enums、IdP tenant service mapping、NOT_FOUND fail-closed provider validation，删除
  Admission proto/Java/test 与过时 RPC package 文档后，精确 Maven 命令通过：contract 1、provider 4 tests，均 0
  failures/errors/skips；protobuf generation 只处理现有 identity_directory.proto，`git diff --check` 与 declared-path gate
  通过。提交 `08f3d8c6` 含 10 个声明路径；未启动 live RPC、RBAC、数据库或浏览器。

### Step 9 — 建立 RBAC V8 tenant authorization state 与外部 tenant FK target

- Requirements: `REQ-012`, `REQ-014`, `REQ-015`
- Dependencies: Step 8；Step18生成的真实导入gate在部署时先于V8，但source implementation可先完成。
- Execution state: `Committed` at `06dd03cd`; no empty or corrective Step 9 commit was created.
- Baseline state: RBAC migrations V1–V7；`rbac3_tenant`同时持有catalog/settings/status/policyVersion并被多个FK引用。
- Observable outcome: V8在事务内复制每个tenant的policyVersion/audit到 `rbac3_tenant_authorization_state`，重指全部实际inbound
  FKs并删除tenant master；state JPA/repository可ensure/lock/increment。
- End state: V8 rehearsal/state repository GREEN；TenantPO consumers尚在Steps10–12改造，中间提交不得部署。
- Test-first gate: Required — V8 migration/state tests因migration/table/entity/repository缺失而RED，并验证未提供signed
  gate时V8拒绝。
- Ordered files:

#### File 1 —
`CREATE egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin/src/test/java/top/egon/cola/platform/rbac3/admin/repository/Rbac3ExternalTenantV8IT.java; egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin/src/test/java/top/egon/cola/platform/rbac3/admin/iam/authorizationstate/JpaTenantAuthorizationStateRepositoryTest.java`
`MODIFY egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin/src/test/java/top/egon/cola/platform/rbac3/admin/repository/Rbac3MigrationContractTest.java; egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin/src/test/java/top/egon/cola/platform/rbac3/admin/repository/Rbac3FlywayPostgresqlIT.java`

- Purpose: 固定V7→V8 copy/FK/drop/gate和state lock/increment/ensure行为，并让既有 migration history/Pre-V8 IT
  明确包含V8与V7 target边界。
- Symbols: `requiresVerifiedExternalTenantGate`, `retargetsEveryInboundForeignKey`,
  `incrementsPolicyVersionMonotonically`。
- Repository evidence: `Rbac3FlywayPostgresqlIT`/migration contract tests与TenantPO policyVersion
  repositories；V1/V7真实constraint names。
- Dependencies and consumers: all RBAC migrations、new state model/repo；Steps10/11。
- Why now: schema destructive迁移必须先有可重复proof，且copy/FK inventory不能靠实现时猜测。
- Contract/signature changes: test fixture提供non-zero policyVersion、全部FK tables和external gate report；assert tenant
  IDs/value不变。
- Input/output and state mapping: `rbac3_tenant.id/policy_version/version/audit`→state row；inbound FK column values
  unchanged，target table changed。
- Error and edge behavior: missing/invalid gate、count mismatch、orphan、duplicate、FK inventory drift在drop前失败；transaction
  rollback保留旧表/constraints。
- Implementation pseudocode:

```java
migrate a V7 fixture containing multiple tenants, non-zero policy versions and rows for every actual FK that references rbac3_tenant
assert V8 refuses without the verified external gate artifact and leaves rbac3_tenant plus all original FKs intact
provide a valid gate, migrate, then assert one state row per tenant, identical policy versions, every inbound FK retargeted and rbac3_tenant absent
lock one state row concurrently, increment monotonically and assert missing external tenant cannot be silently created except through approved ensure path
```

- Verification contribution: RED/GREEN `TEST-020/021/022` RBAC schema slice。
- After this file: tests RED for missingV8/model。

#### File 2 —
`CREATE egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin/src/main/resources/db/migration/V8__externalize_tenant_authority.sql`

- Purpose: 执行单个RBAC database change，保留授权事实与policyVersion但删除catalog master。
- Symbols: `rbac3_tenant_authorization_state`、named gate/checks、FK drops/adds、`DROP TABLE rbac3_tenant`。
- Repository evidence: V1和V7列出实际FK names；主规格§11.2.8/11.2.9/16.3给出copy/gate/rollback。
- Dependencies and consumers: PostgreSQL/Flyway；external gate通过session setting/temp table/approved artifact
  interface实现，不连接IdP DB。
- Why now: File1固定全部inbound FK inventory后写deterministic SQL。
- Contract/signature changes: new state table isFK parent；catalog fields/code/name/status/settings不存在。
- Input/output and state mapping: all tenant IDs原值复制；state policyVersion/version/audit保留；tenant-scoped child
  rows不重键。
- Error and edge behavior: any gate/count/orphan/FK mismatch raises beforedrop；single transaction rollback；V8
  commit后rollback只允许dual snapshot restore。
- Implementation pseudocode:

```sql
assert the externally verified gate identity, source/target tenant counts and zero orphan/placeholder conditions before destructive statements
create rbac3_tenant_authorization_state and insert tenant_id, policy_version, version and audit columns from rbac3_tenant with exact count checks
drop each discovered inbound foreign key and recreate it against authorization_state(tenant_id) without changing child tenant_id values
requery pg_constraint for zero remaining references, validate state coverage, then drop rbac3_tenant inside the same transaction
```

- Verification contribution: migration IT GREEN；历史V1–V7 checksum unchanged。
- After this file: schema transition proof GREEN；production code still referencesTenantPO。

#### File 3 —
`CREATE egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin/src/main/java/top/egon/cola/platform/rbac3/admin/iam/authorizationstate/domain/po/TenantAuthorizationStatePO.java; egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin/src/main/java/top/egon/cola/platform/rbac3/admin/iam/authorizationstate/repository/TenantAuthorizationStateRepository.java; egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin/src/main/java/top/egon/cola/platform/rbac3/admin/iam/authorizationstate/repository/jpa/JpaTenantAuthorizationStateRepository.java`

- Purpose: 提供唯一RBAC authorization version persistence abstraction。
- Symbols: Entity `tenantId/policyVersion/version/audit`、`requireForUpdate`、`ensureVerifiedTenant`、`increment`。
- Repository evidence: TenantPO current methods与Role/Constraint repositories的lock/event behavior；Spec仅拆state不改mutation
  outputs。
- Dependencies and consumers: JPA/DatabaseClock；Steps10/11。
- Why now: V8 ready后建立replacement type/repository再改消费者。
- Contract/signature changes: new internal repository；ensure方法必须接“已验证tenant”typed marker或仅由Step11 gated
  orchestration调用。
- Input/output and state mapping: external decimal tenantId→Long PK；mutation→policyVersion+1/version/audit；no
  catalog/status fields。
- Error and edge behavior: missingstate默认fail closed；only bootstrap/create subject approved path may ensure initial
  row afterRPC ACTIVE；overflow/lock failure abort transaction。
- Implementation pseudocode:

```java
@Entity TenantAuthorizationStatePO maps tenant_id as immutable id and owns policyVersion, optimistic version and audit timestamps only
repository.requireForUpdate selects one row with PESSIMISTIC_WRITE and throws RESOURCE_NOT_FOUND when no verified state exists
ensureVerifiedTenant inserts version zero only after the caller supplies a successful IdP membership verification result and handles duplicate races deterministically
increment uses Math.incrementExact, updates actor/time and returns the new policyVersion consumed by existing outbox/result contracts
```

- Verification contribution: state repository test GREEN；unblockspolicy refactor。
- After this file: new state model works；old TenantPO remains untilStep12。

- Validation working directory: `/Users/mario/SelfProject/Egon-COLA`
- Verification command:
  `./mvnw -B -ntp -pl egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin -am -Dtest=Rbac3ExternalTenantV8IT,JpaTenantAuthorizationStateRepositoryTest -Dsurefire.failIfNoSpecifiedTests=false test`
- Expected result: exit 0；V8 gate/copy/FK/drop与state concurrency tests通过，V1–V7无diff。
- Failure returns to: File1 fixture/FK inventory；File2 SQL/gate；File3 mapping/locking。
- Completion criteria: 一个V8实现完整授权state externalization，且没有修改旧migration。
- Rollback: pre-V8 revert paths；post-V8 only coordinated RBAC+IdP snapshot restore or forward-fix。
- Commit paths: `egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin/src/test/java/top/egon/cola/platform/rbac3/admin/repository/Rbac3ExternalTenantV8IT.java; egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin/src/test/java/top/egon/cola/platform/rbac3/admin/iam/authorizationstate/JpaTenantAuthorizationStateRepositoryTest.java`,   `egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin/src/test/java/top/egon/cola/platform/rbac3/admin/repository/Rbac3MigrationContractTest.java; egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin/src/test/java/top/egon/cola/platform/rbac3/admin/repository/Rbac3FlywayPostgresqlIT.java`,   `egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin/src/main/resources/db/migration/V8__externalize_tenant_authority.sql`,   `egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin/src/main/java/top/egon/cola/platform/rbac3/admin/iam/authorizationstate/domain/po/TenantAuthorizationStatePO.java; egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin/src/main/java/top/egon/cola/platform/rbac3/admin/iam/authorizationstate/repository/TenantAuthorizationStateRepository.java; egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin/src/main/java/top/egon/cola/platform/rbac3/admin/iam/authorizationstate/repository/jpa/JpaTenantAuthorizationStateRepository.java`
- Commit: `feat(rbac3): externalize tenant authorization state`

- Execution evidence: Step 9 RED 在 RBAC admin `testCompile` 因 V8 state entity/repository 不存在而失败；新增唯一
  `V8__externalize_tenant_authority.sql` gate/count/FK inventory/state-copy/drop migration、typed verified-tenant state
  repository 与 JPA pessimistic-lock/increment implementation，并把既有 migration contract/Pre-V8 PostgreSQL IT 明确到
  V8 history/V7 target 后，精确 Maven 命令通过 6 tests、0 failures/errors/skips；`git diff --check` 与 8-file declared-path
  gate 通过。V8 SQL/DB rehearsal 本次仅做静态 contract，未连接 live PostgreSQL，未启动服务或浏览器。

### Step 10 — 将 RBAC policyVersion 读写切到 authorization state

- Requirements: `REQ-012`, `REQ-017`
- Dependencies: Step 9
- Execution state: `Committed` at `d72ad57d`; no empty or corrective Step 10 commit was created.
- Baseline state: role/constraint/simulation/activation repositories仍查询或锁 `TenantEntity`
  ，并从TenantPO递增/读取policyVersion。
- Observable outcome: 所有policyVersion读写、outbox payload和simulation
  checksum改由TenantAuthorizationStateRepository/PO提供，授权决策输出保持逐位一致。
- End state: policy/repository focused tests GREEN；bootstrap/directory中的TenantPO引用留Steps11/12处理。
- Test-first gate: Required —现有repository/simulation tests替换fixture后因仍查询TenantEntity而RED，并要求相同result/outbox
  version。
- Ordered files:

#### File 1 —
`MODIFY egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin/src/test/java/top/egon/cola/platform/rbac3/admin/iam/role/RoleControlFacadeTest.java; egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin/src/test/java/top/egon/cola/platform/rbac3/admin/simulation/PostgresqlRoleImpactSourceTest.java; egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin/src/test/java/top/egon/cola/platform/rbac3/admin/iam/role/activation/RoleActivationCandidateServiceTest.java`

- Purpose: 固定role/constraint/simulation/activation在state拆分后的version/output不变量。
- Symbols: mutation version increments、outbox metadata、impact checksum、activation facts policyVersion。
- Repository evidence: existing tests覆盖这些outputs；只需将TenantPO fixture替换state fixture。
- Dependencies and consumers: production repositories inFile2；authorization/runtime consumers不改contract。
- Why now: 先用旧行为断言证明拆表不改变授权语义。
- Contract/signature changes: 测试依赖切到TenantAuthorizationStateRepository；public DTO/VO无变化。
- Input/output and state mapping: tenantId→state policyVersion；mutation→new version/outbox；simulation→same checksum
  inputs。
- Error and edge behavior: missing state、lock conflict、overflow仍fail；inactive tenant不再由RBAC
  catalog判断，membership/Token gate另行负责。
- Implementation pseudocode:

```java
seed TenantAuthorizationStatePO with the same tenant id and non-zero policyVersion previously stored in TenantPO
execute role and constraint mutations and assert one monotonic increment, identical result VO and identical outbox policyVersion metadata
run role impact and activation fact reads and assert they use authorization state without querying catalog status/name/settings
remove any assertion that RBAC tenant status decides membership while retaining all permission, role and cache-version assertions
```

- Verification contribution: RED/GREEN preserved authorization behavior。
- After this file: tests RED was recorded; File 2 completed the repository cutover and returned the exact gate to GREEN。

#### File 2 —
`MODIFY egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin/src/main/java/top/egon/cola/platform/rbac3/admin/iam/role/repository/jpa/JpaRoleRepository.java; egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin/src/main/java/top/egon/cola/platform/rbac3/admin/iam/policy/repository/jpa/JpaConstraintRepository.java; egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin/src/main/java/top/egon/cola/platform/rbac3/admin/simulation/repository/jdbc/PostgresqlRoleImpactRepository.java; egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin/src/main/java/top/egon/cola/platform/rbac3/admin/iam/role/activation/repository/jpa/JpaRoleActivationFactRepository.java; egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin/src/main/java/top/egon/cola/platform/rbac3/admin/iam/role/service/RoleEligibilityService.java`

- Purpose: 替换所有核心policyVersion TenantPO/JPQL/JDBC访问。
- Symbols: mutation helpers、impact query、activation facts query、eligibility tenant-state check removal。
- Repository evidence: repository-wide TenantPO/policyVersion search列出这些production consumers。
- Dependencies and consumers: new state repository/table；existingoutbox/runtime/result contracts。
- Why now: tests固定semantic output后做mechanical ownership refactor。
- Contract/signature changes: constructor dependencies从EntityManager TenantPO helper换state repository；SQL/JPQL table
  name更新；service signatures不变。
- Input/output and state mapping: tenantId→state lock/read→policyVersion；其余 role/constraint facts不变。
- Error and edge behavior: state missing稳定not-found/fail closed；不根据已删除tenant status放行/拒绝；transaction/outbox
  boundary保持。
- Implementation pseudocode:

```java
replace every lockTenant or TenantEntity query used for policyVersion with authorizationStates.requireForUpdate or a read projection
increment state exactly once inside the existing role/constraint transaction and publish the returned value in unchanged result/outbox contracts
join rbac3_tenant_authorization_state for activation facts and query it directly for role-impact checksum input
remove RoleEligibilityService catalog-status dependency while preserving user status, application, role, permission and constraint eligibility rules
```

- Verification contribution: makesFile1 GREEN，并减少TenantPO reference set。
- After this file: policyVersion完全由state拥有；bootstrap/directory/legacy membership仍引用TenantPO。

- Validation working directory: `/Users/mario/SelfProject/Egon-COLA`
- Verification command:
  `./mvnw -B -ntp -pl egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin -am -Dtest=RoleControlFacadeTest,PostgresqlRoleImpactSourceTest,RoleActivationCandidateServiceTest -Dsurefire.failIfNoSpecifiedTests=false test`
- Expected result: exit 0；mutation/outbox/impact/activation version回归通过，所列production files无TenantPO/TenantEntity
  query。
- Failure returns to: File1 preserved assertion；File2 mapping/transaction/query。
- Completion criteria: policyVersion ownership完整切换且授权算法/输出不变。
- Rollback: revert两组paths；V8未部署时无data effect；V8后需整组binary/schema rollback。
- Commit paths: `egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin/src/test/java/top/egon/cola/platform/rbac3/admin/iam/role/RoleControlFacadeTest.java; egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin/src/test/java/top/egon/cola/platform/rbac3/admin/simulation/PostgresqlRoleImpactSourceTest.java; egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin/src/test/java/top/egon/cola/platform/rbac3/admin/iam/role/activation/RoleActivationCandidateServiceTest.java`,   `egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin/src/main/java/top/egon/cola/platform/rbac3/admin/iam/role/repository/jpa/JpaRoleRepository.java; egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin/src/main/java/top/egon/cola/platform/rbac3/admin/iam/policy/repository/jpa/JpaConstraintRepository.java; egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin/src/main/java/top/egon/cola/platform/rbac3/admin/simulation/repository/jdbc/PostgresqlRoleImpactRepository.java; egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin/src/main/java/top/egon/cola/platform/rbac3/admin/iam/role/activation/repository/jpa/JpaRoleActivationFactRepository.java; egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin/src/main/java/top/egon/cola/platform/rbac3/admin/iam/role/service/RoleEligibilityService.java`
- Commit: `refactor(rbac3): read policy versions from authorization state`

Execution evidence after commit: the planned RED gate was the old-constructor/test-compile failure for all three updated tests; after adding the typed state dependency, policy mutations increment the state once and preserve the outbox version, impact checksum reads the state row, activation facts no longer join `rbac3_tenant`, and eligibility keeps only the local application-grant existence check. The exact Maven command passed 7 tests with 0 failures/errors/skips; `git diff --check` and the 8-file declared-path gate passed. This is module/static proof only: no live PostgreSQL, service, Redis, RPC, or browser was started.

### Step 11 — 对 RBAC 主体写入与 bootstrap 强制 IdP membership gate

- Requirements: `REQ-012`, `REQ-013`, `REQ-019`, `REQ-021`
- Dependencies: Steps 8–10
- Baseline state: UserCrudFacade/Directory status可在无IdP校验时创建/启用；bootstrap接tenantCode并查询/创建TenantPO。
- Observable outcome: 一个RPC
  adapter将missing/inactive/timeout统一为fail-closed；用户create/update-to-active与CLI/development/platform
  bootstrap只在ACTIVE后进入本地事务；bootstrap输入仅tenantId+identitySub且不触碰TenantPO。
- End state: subject/bootstrap tests GREEN；legacy tenant/member API删除留Step12。
- Test-first gate: Required — user/bootstrap tests先证明inactive/timeout仍会写、CLI仍接受tenantCode而RED。
- Ordered files:

#### File 1 —
`CREATE egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin/src/main/java/top/egon/cola/platform/rbac3/admin/iam/user/repository/IdentityTenantMembershipDirectory.java; egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin/src/test/java/top/egon/cola/platform/rbac3/admin/iam/user/repository/IdentityTenantMembershipDirectoryTest.java`

- Purpose: 封装RPC-001 deadline/mapping，提供明确 `requireActive(tenantId,identitySub)` gate。
- Symbols: `MembershipVerification`、`requireActive`、stable unavailable/not-active violations。
- Repository evidence: existing `IdentityProfileDirectory` RPC reference/deadline/mapping style；新proto/Java contract。
- Dependencies and consumers: IdentityDirectoryRpc；UserCrudFacade/DirectoryCommand/bootstrap services。
- Why now: 一个adapter集中fail-closed semantics，避免三个写入口各自解析proto。
- Contract/signature changes: new RBAC internal collaborator；不缓存allow，不持久profile。
- Input/output and state mapping: tenantId+sub→RPC request→three statuses/version→verified marker；no rbac3UserId。
- Error and edge behavior: null response、found false、任一非ACTIVE、malformed status、timeout/transport都throw stable rule
  violation；不降级。
- Implementation pseudocode:

```java
@EgonRpcReference(timeoutMs=1500) call IdentityDirectoryRpc.getTenantMembership with normalized decimal tenantId and identitySub
require found=true and tenantStatus, identityStatus and membershipStatus all equal ACTIVE before constructing MembershipVerification
map missing or inactive to deterministic business rejection and every transport/null/malformed response to membership unavailable
never cache an ALLOW result or persist display/profile fields, so each approved write performs a current authoritative check
```

- Verification contribution: RED/GREEN `TEST-019/020/025` adapter slice。
- After this file: gate collaborator works；callers未接入。

#### File 2 —
`MODIFY egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin/src/test/java/top/egon/cola/platform/rbac3/admin/iam/user/controller/UserControllerTest.java; egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin/src/test/java/top/egon/cola/platform/rbac3/admin/bootstrap/Rbac3PlatformAdminBootstrapCliIT.java; egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin/src/test/java/top/egon/cola/platform/rbac3/admin/bootstrap/service/Rbac3DevelopmentBootstrapTest.java; egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin/src/test/java/top/egon/cola/platform/rbac3/admin/bootstrap/service/Rbac3DevelopmentTopologyTest.java`

- Purpose: 定义三类写入口的ACTIVE/zero-write与新CLI/config contracts。
- Symbols: subject create/update/status cases；`--tenant-id`；`tenant-ids`；no tenantCode/TenantPO。
- Repository evidence: existing tests覆盖controller/bootstrap CLI/development topology；可扩展RecordingPort/EntityManager
  assertions。
- Dependencies and consumers: Files3/4 production paths。
- Why now: gate adapter已定义后用behavior tests固定每个consumer。
- Contract/signature changes: CLI allowed options改 `--tenant-id,--identity-sub`；development config改tenant-ids；seed新IdP
  tenant permissions。
- Input/output and state mapping: external decimal tenantId+identitySub→gate→state/user/facts；not-active→no
  entity/outbox。
- Error and edge behavior: malformed/missing/timeout/disabled均non-zero exit或business rejection且zero writes；duplicate
  valid bootstrap idempotent。
- Implementation pseudocode:

```java
invoke user create, identity update and transition to ACTIVE with active, missing, disabled and unavailable membership responses
assert only active verification reaches EntityManager persist/changeStatus and every rejected path leaves user, state, role and outbox counts unchanged
invoke CLI with --tenant-id and --identity-sub, reject --tenant-code, and verify development tenant-ids parses stable distinct decimal values
run valid bootstrap twice and assert one state/user/fact set plus monotonic version only when actual authorization facts changed
```

- Verification contribution: RED/GREEN `TEST-020/033`。
- After this file: tests RED untilcallers/bootstrap改造。

#### File 3 —
`MODIFY egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin/src/main/java/top/egon/cola/platform/rbac3/admin/iam/user/service/UserCrudFacade.java; egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin/src/main/java/top/egon/cola/platform/rbac3/admin/iam/organization/snapshot/service/DefaultDirectoryCommandService.java`

- Purpose: 在主体create/rebind/enable写入前调用IdP gate。
- Symbols: UserCrudFacade create/update；directory `changeUserStatus` ACTIVE branch。
- Repository evidence: current transactional methods与UserPO state changes；Spec要求create/enable gate。
- Dependencies and consumers: IdentityTenantMembershipDirectory；controllers signatures不变。
- Why now: File2固定zero-write behavior后在现有orchestration点添加单一collaborator。
- Contract/signature changes: constructors新增directory；publicHTTP DTO不变。
- Input/output and state mapping: trusted TenantContext tenantId+command identitySub→requireActive；verified
  marker不持久；then existing writes。
- Error and edge behavior: RPC在任何local mutation前调用；失败rollback/zero write；archive/disable不要求membership
  active；rebind active subject必须校验new sub。
- Implementation pseudocode:

```java
create derives tenantId from trusted controller context, calls memberships.requireActive before duplicate query/persist and then runs the existing transaction
update validates the replacement identitySub when the RBAC subject will remain ACTIVE before changing the persisted binding
changeUserStatus calls the gate only when next status enables participation, using the persisted identitySub and current tenantId
propagate stable not-active/unavailable violations through existing error mapping and never convert them to an allow or retry inside the transaction
```

- Verification contribution: user paths inFile2 GREEN。
- After this file: normal user writes gated；bootstrap待File4。

#### File 4 —
`MODIFY egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin/src/main/java/top/egon/cola/platform/rbac3/admin/bootstrap/controller/cli/Rbac3PlatformAdminBootstrapCli.java; egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin/src/main/java/top/egon/cola/platform/rbac3/admin/bootstrap/repository/DevelopmentBootstrapPort.java; egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin/src/main/java/top/egon/cola/platform/rbac3/admin/bootstrap/repository/PlatformAdminBootstrapRepository.java; egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin/src/main/java/top/egon/cola/platform/rbac3/admin/bootstrap/service/PlatformAdminBootstrapService.java; egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin/src/main/java/top/egon/cola/platform/rbac3/admin/bootstrap/service/Rbac3DevelopmentBootstrap.java; egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin/src/main/java/top/egon/cola/platform/rbac3/admin/bootstrap/service/internal/DefaultPlatformAdminBootstrapService.java; egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin/src/main/java/top/egon/cola/platform/rbac3/admin/bootstrap/repository/jpa/JpaDevelopmentTopologyBootstrapRepository.java; egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin/src/main/java/top/egon/cola/platform/rbac3/admin/bootstrap/repository/jpa/JpaPlatformAdminBootstrapRepository.java`

- Purpose: 改写bootstrap contract/orchestration/persistence为external ID+RPC gate+authorization state。
- Symbols: `bootstrap(String tenantId,String identitySub)`；CLI/config；repositories ensure state/user/facts，无TenantPO。
- Repository evidence: current files正是tenantCode/TenantPO创建链；new state repository与membership directory可直接组合。
- Dependencies and consumers: File1 directory、Step9 state repository、existing ID generator/role/application
  repositories。
- Why now: normal user gate完成后改最具部署影响的bootstrap chain。
- Contract/signature changes: tenantCode→tenantId acrossports/services/CLI/config；Jpa repositories不再创建/查tenant
  catalog。
- Input/output and state mapping: decimal tenantId→Long；verification→ensure state/user；existing topology
  definitions→facts；policyVersion由state increment。
- Error and edge behavior: verification在transaction/persist前；invalid input/timeout zero-write；valid duplicate
  idempotent；state duplicate race reloads winner。
- Implementation pseudocode:

```java
CLI accepts only --tenant-id and --identity-sub, validates decimal positive tenantId and invokes PlatformAdminBootstrapService with external identity values
development bootstrap parses tenant-ids, then for each id asks IdentityTenantMembershipDirectory.requireActive before calling its repository port
repository uses tenantId directly, ensures TenantAuthorizationStatePO and UserPO, then reuses existing application/role/permission fact definitions without TenantPO
when facts changed, lock and increment authorization state policyVersion; when nothing changed, preserve versions and return idempotent success
```

- Verification contribution: bootstrap tests GREEN，static no `findTenant/new TenantPO/tenantCode` inbootstrap main。
- After this file: allapproved RBAC subject/bootstrap writes gated，legacy tenant/member APIs仍存在只读/CRUD untilStep12。

- Validation working directory: `/Users/mario/SelfProject/Egon-COLA`
- Verification command:
  `./mvnw -B -ntp -pl egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin -am -Dtest=IdentityTenantMembershipDirectoryTest,UserControllerTest,Rbac3PlatformAdminBootstrapCliIT,Rbac3DevelopmentBootstrapTest,Rbac3DevelopmentTopologyTest -Dsurefire.failIfNoSpecifiedTests=false test`
- Expected result: exit 0；ACTIVE succeeds，missing/disabled/timeout/malformed zero-write，CLI/config no
  tenantCode，bootstrap noTenantPO。
- Failure returns to: File1 RPC mapping；File2 contracts；File3 user orchestration；File4 bootstrap chain。
- Completion criteria: `REQ-013/021`所有writer已fail closed且外部ID唯一；existing Client permission seed扩展由Step17最终校验。
- Rollback: revert四组paths；V8未部署可source rollback，V8后只整组rollback。
- Commit paths: `egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin/src/main/java/top/egon/cola/platform/rbac3/admin/iam/user/repository/IdentityTenantMembershipDirectory.java; egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin/src/test/java/top/egon/cola/platform/rbac3/admin/iam/user/repository/IdentityTenantMembershipDirectoryTest.java`,   `egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin/src/test/java/top/egon/cola/platform/rbac3/admin/iam/user/controller/UserControllerTest.java; egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin/src/test/java/top/egon/cola/platform/rbac3/admin/bootstrap/Rbac3PlatformAdminBootstrapCliIT.java; egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin/src/test/java/top/egon/cola/platform/rbac3/admin/bootstrap/service/Rbac3DevelopmentBootstrapTest.java; egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin/src/test/java/top/egon/cola/platform/rbac3/admin/bootstrap/service/Rbac3DevelopmentTopologyTest.java`,   `egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin/src/main/java/top/egon/cola/platform/rbac3/admin/iam/user/service/UserCrudFacade.java; egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin/src/main/java/top/egon/cola/platform/rbac3/admin/iam/organization/snapshot/service/DefaultDirectoryCommandService.java`,   `egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin/src/main/java/top/egon/cola/platform/rbac3/admin/bootstrap/controller/cli/Rbac3PlatformAdminBootstrapCli.java; egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin/src/main/java/top/egon/cola/platform/rbac3/admin/bootstrap/repository/DevelopmentBootstrapPort.java; egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin/src/main/java/top/egon/cola/platform/rbac3/admin/bootstrap/repository/PlatformAdminBootstrapRepository.java; egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin/src/main/java/top/egon/cola/platform/rbac3/admin/bootstrap/service/PlatformAdminBootstrapService.java; egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin/src/main/java/top/egon/cola/platform/rbac3/admin/bootstrap/service/Rbac3DevelopmentBootstrap.java; egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin/src/main/java/top/egon/cola/platform/rbac3/admin/bootstrap/service/internal/DefaultPlatformAdminBootstrapService.java; egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin/src/main/java/top/egon/cola/platform/rbac3/admin/bootstrap/repository/jpa/JpaDevelopmentTopologyBootstrapRepository.java; egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin/src/main/java/top/egon/cola/platform/rbac3/admin/bootstrap/repository/jpa/JpaPlatformAdminBootstrapRepository.java`
- Commit: `feat(rbac3): gate subjects and bootstrap through idp membership`

- Execution state: `Committed` at `c52fe991`; no empty or corrective Step 11 commit was created. RED was observed
  first for the missing membership adapter and then for the legacy CLI `--tenant-code` contract; after adding the
  adapter, fail-closed user/bootstrap gates, numeric tenant bootstrap contracts and the declared controller test
  boundary, the exact Maven command passed 9 tests with 0 failures/errors/skips. Static stale-owner search and
  `git diff --check` passed; no live IdP/RBAC database, service or browser was started.

### Step 12 — 删除 RBAC tenant catalog 与 membership resolve 后端

- Requirements: `REQ-012`, `REQ-018`, `REQ-020`
- Dependencies: Steps 9–11
- Baseline state: authorization writes已用state/RPC gate，但TenantController/TenantPO/lookup、Directory tenant
  CRUD/list、两组internal membership HTTP与local membership repository仍存在。
- Observable outcome: RBAC backend只保留TenantContext/target authorization scope和rbac3_user授权主体；tenant
  catalog/member resolve endpoints/types/repos删除，旧routes 404，module compile/test GREEN。
- End state: RBAC backend无tenant master owner或membership query；Admin Web cleanup待Step17。
- Test-first gate: Required — deleted-route/static tests先因旧controllers/types仍存在而RED；保留TenantContextFilter/target
  permission regression。
- Ordered files:

#### File 1 —
`MODIFY egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin/src/test/java/top/egon/cola/platform/rbac3/admin/iam/tenant/controller/IamTenantControllerTest.java; egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin/src/test/java/top/egon/cola/platform/rbac3/admin/iam/tenant/TenantContextFilterTest.java; egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin/src/test/java/top/egon/cola/platform/rbac3/admin/iam/user/IdentityMembershipResolveRequestDTOTest.java`

- Purpose: 将旧tenant controller测试改成route absence，并保留trusted tenant context/target authorization regression。
- Symbols: old `/api/rbac3/v1/platform/tenants` and internal resolve 404；TenantContextFilter still requires signed
  principal/target permission。
- Repository evidence: current tests/routes/filter；Spec删除catalog但不删除tenant-scoped authorization context。
- Dependencies and consumers: deletion Files2/3；MockMvc/application context。
- Why now: 先固定“删除owner，不删除tenant authorization scope”的边界。
- Contract/signature changes: old CRUD/resolve contract intentional breaking；filter header contract unchanged。
- Input/output and state mapping: verified JWT tenant/authorized target→TenantContext；no DB catalog lookup；old request
  bodies无handler。
- Error and edge behavior: unauthenticated/unauthorized target仍reject；missing catalog不变成allow；old routes
  404不做redirect/proxy。
- Implementation pseudocode:

```java
assert every RBAC tenant catalog CRUD/list/detail route and both internal membership resolve URI families have no mapped handler
retain TenantContextFilter tests for login tenant and explicitly authorized target tenant, including rejection without system:tenant:target
remove DTO-specific membership tests because the transport type itself is deleted and no compatibility deserializer remains
search the test application context to ensure no mock accidentally recreates a removed controller or repository bean
```

- Verification contribution: RED/GREEN backend 404/preserved context proof。
- After this file: route absence tests RED untildeletions。

#### File 2 —
`DELETE egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin/src/main/java/top/egon/cola/platform/rbac3/admin/iam/tenant/controller/TenantController.java; egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin/src/main/java/top/egon/cola/platform/rbac3/admin/iam/tenant/domain/dto/CreateTenantCommandDTO.java; egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin/src/main/java/top/egon/cola/platform/rbac3/admin/iam/tenant/domain/dto/TenantStatusCommandDTO.java; egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin/src/main/java/top/egon/cola/platform/rbac3/admin/iam/tenant/domain/enums/TenantStatusEnum.java; egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin/src/main/java/top/egon/cola/platform/rbac3/admin/iam/tenant/domain/po/TenantPO.java; egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin/src/main/java/top/egon/cola/platform/rbac3/admin/iam/tenant/domain/vo/TenantVO.java; egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin/src/main/java/top/egon/cola/platform/rbac3/admin/iam/tenant/repository/TenantLookupRepository.java; egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin/src/main/java/top/egon/cola/platform/rbac3/admin/iam/tenant/repository/jpa/JpaTenantLookupRepository.java`

- Purpose: 物理删除RBAC catalog model/CRUD/lookup，保留同package的TenantContext/filter/resolver。
- Symbols: listed classes absent；`TenantContext`、`TenantContextResolver`、filter remain。
- Repository evidence: Step10/11已移除所有policy/bootstrap consumers；V8 drop table。
- Dependencies and consumers: Directory/member consumers在File3同步删除/修改；Web Step17。
- Why now: replacement state/gate已上线source后可安全delete master types。
- Contract/signature changes: catalog HTTP/service/repository gone；authorization tenantId仍Long/String boundary。
- Input/output and state mapping: none；no code/name/status/settings inRBAC；tenantId来自verified context/external ID。
- Error and edge behavior: any remaining import/JPQL `TenantEntity` compile/test/static fail；不得创建shadow DTO或proxy
  to IdP。
- Implementation pseudocode:

```text
delete TenantController, catalog commands/status enum, TenantPO/view and lookup repositories as one ownership-removal set
retain TenantContext, TenantContextFilter, TenantContextResolver and target permission because they scope authorization rather than own catalog
search all main sources for TenantPO, TenantEntity and rbac3_tenant access and require zero results outside immutable migrations and V8
allow compilation to expose every undeclared consumer, then resolve it only by state/RPC/context paths already approved
```

- Verification contribution: static absence、module compile、old route404。
- After this file: catalog types absent；Directory/member consumers requireFile3。

#### File 3 —
`MODIFY egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin/src/main/java/top/egon/cola/platform/rbac3/admin/iam/organization/controller/DirectoryController.java; egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin/src/main/java/top/egon/cola/platform/rbac3/admin/iam/organization/service/DirectoryQueryService.java; egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin/src/main/java/top/egon/cola/platform/rbac3/admin/iam/organization/service/DefaultDirectoryQueryService.java; egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin/src/main/java/top/egon/cola/platform/rbac3/admin/iam/organization/snapshot/repository/DirectoryCommandRepository.java; egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin/src/main/java/top/egon/cola/platform/rbac3/admin/iam/organization/snapshot/repository/DirectoryQueryRepository.java; egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin/src/main/java/top/egon/cola/platform/rbac3/admin/iam/organization/snapshot/repository/jpa/JpaDirectoryCommandRepository.java; egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin/src/main/java/top/egon/cola/platform/rbac3/admin/iam/organization/snapshot/repository/jpa/JpaDirectoryQueryRepository.java`

- Purpose: 从混合Directory API/repositories移除tenant list/create/get/status分支，保留users/org/positions/snapshots。
- Symbols: delete tenant methods/JPQL/view mapping；remaining tenant-scoped operations useexplicit tenantId。
- Repository evidence: current Directory files includeTenantPO queries alongside unrelateddirectory capabilities。
- Dependencies and consumers: User/Directory controllers/Web；authorization context。
- Why now: File2删除TenantPO后清理mixed consumers使module compile。
- Contract/signature changes: tenant-specific service/repository methods removed；user/org method signatures保持。
- Input/output and state mapping: existing tenantId parameter直接scopes UserPO/org tables；no catalog join/name/status
  projection。
- Error and edge behavior: invalid tenantId formatting仍由context/DTO validation；missing local authorization
  rows保持not-found；no remote IdP fetch added。
- Implementation pseudocode:

```java
remove tenant collection/detail/create/status methods from DirectoryController, query service and command/query repository interfaces
delete TenantEntity JPQL, tenantView mapping and TenantVO imports from both JPA directory repositories
retain user, organization, position and snapshot operations with their existing explicit tenantId predicates and pagination ordering
compile every controller/service consumer and update only removed tenant calls, leaving non-tenant directory behavior unchanged
```

- Verification contribution: Directory/user regression and static noTenantPO。
- After this file: mixeddirectory compiles withoutcatalog。

#### File 4 —
`DELETE egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin/src/main/java/top/egon/cola/platform/rbac3/admin/iam/user/controller/InternalIdentityController.java; egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin/src/main/java/top/egon/cola/platform/rbac3/admin/iam/user/controller/InternalUserController.java; egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin/src/main/java/top/egon/cola/platform/rbac3/admin/iam/user/domain/dto/IdentityMembershipResolveRequestDTO.java; egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin/src/main/java/top/egon/cola/platform/rbac3/admin/iam/user/domain/exception/IdentityMembershipNotFoundException.java; egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin/src/main/java/top/egon/cola/platform/rbac3/admin/iam/user/domain/vo/TenantMembershipResponseVO.java; egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin/src/main/java/top/egon/cola/platform/rbac3/admin/iam/user/domain/vo/TenantMembershipVO.java; egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin/src/main/java/top/egon/cola/platform/rbac3/admin/iam/user/repository/IdentityMembershipRepository.java; egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin/src/main/java/top/egon/cola/platform/rbac3/admin/iam/user/repository/jpa/JpaIdentityMembershipRepository.java; egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin/src/main/java/top/egon/cola/platform/rbac3/admin/iam/user/repository/jpa/MembershipRow.java; egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin/src/main/java/top/egon/cola/platform/rbac3/admin/iam/user/service/IdentityMembershipFacade.java`

- Purpose: 删除RBAC作为membership resolver的两套HTTP、repo/facade/DTO/VO。
- Symbols: listed paths absent；`rbac3_user`/UserCrudFacade remain authorization subject。
- Repository evidence: Step7 IdP local authority替换all IdP consumers；Step8/11新RPC方向为RBAC→IdP。
- Dependencies and consumers: old external/internal callers必须由Step18 inventory归零；no replacement HTTP。
- Why now: IdP/RBAC new dependency direction已完成，物理删除可防止回退双权威。
- Contract/signature changes: `/internal/v1/identity/*`与`/internal/v1/iam/users/*` membership routes gone。
- Input/output and state mapping: no membership response data
  fromRBAC；UserPO保留tenantId/identitySub/status/authVersion用于authorization only。
- Error and edge behavior: old routes 404；新writes在RPC unavailable时fail closed；不得自动proxy到IdP HTTP。
- Implementation pseudocode:

```text
delete both internal controllers and the complete request, response, facade and JPA membership query chain
retain UserPO, UserCrudFacade and user administration routes because they model tenant-scoped authorization subjects, not identity membership
search IdP and RBAC main sources for old URI strings, DTOs, repository and facade symbols and require zero current references
verify the only membership authority call direction is RBAC IdentityTenantMembershipDirectory to IdP IdentityDirectoryRpc
```

- Verification contribution: `TEST-025/026/029/032` oldpath absence。
- After this file: RBAC backend只持authorization subject/state/facts，module可在V8 schema上运行。

- Validation working directory: `/Users/mario/SelfProject/Egon-COLA`
- Verification command:
  `./mvnw -B -ntp -pl egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin -am -Dtest=IamTenantControllerTest,TenantContextFilterTest,UserControllerTest,IdentityMembershipResolveRequestDTOTest,ControllerRequestParameterMetadataTest,Rbac3ExternalTenantV8IT -Dsurefire.failIfNoSpecifiedTests=false test`
- Expected result: exit 0；old routes 404，TenantContext target authorization仍通过，main source无TenantPO/TenantEntity/old
  membership symbols。
- Failure returns to: File1 preserved boundary；File2 hidden catalog consumer；File3 mixedDirectory method；File4 old
  membership consumer。
- Completion criteria: RBAC database/code/API不再拥有tenant catalog或membership resolve，authorization语义保持。
- Rollback: source revert只在V8前；V8后需要coordinated snapshots/binaries，不能单独恢复TenantPO。
- Commit paths: `egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin/src/test/java/top/egon/cola/platform/rbac3/admin/iam/tenant/controller/IamTenantControllerTest.java; egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin/src/test/java/top/egon/cola/platform/rbac3/admin/iam/tenant/TenantContextFilterTest.java; egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin/src/test/java/top/egon/cola/platform/rbac3/admin/iam/user/IdentityMembershipResolveRequestDTOTest.java`,   `egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin/src/main/java/top/egon/cola/platform/rbac3/admin/iam/tenant/controller/TenantController.java; egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin/src/main/java/top/egon/cola/platform/rbac3/admin/iam/tenant/domain/dto/CreateTenantCommandDTO.java; egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin/src/main/java/top/egon/cola/platform/rbac3/admin/iam/tenant/domain/dto/TenantStatusCommandDTO.java; egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin/src/main/java/top/egon/cola/platform/rbac3/admin/iam/tenant/domain/enums/TenantStatusEnum.java; egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin/src/main/java/top/egon/cola/platform/rbac3/admin/iam/tenant/domain/po/TenantPO.java; egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin/src/main/java/top/egon/cola/platform/rbac3/admin/iam/tenant/domain/vo/TenantVO.java; egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin/src/main/java/top/egon/cola/platform/rbac3/admin/iam/tenant/repository/TenantLookupRepository.java; egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin/src/main/java/top/egon/cola/platform/rbac3/admin/iam/tenant/repository/jpa/JpaTenantLookupRepository.java`,   `egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin/src/main/java/top/egon/cola/platform/rbac3/admin/iam/organization/controller/DirectoryController.java; egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin/src/main/java/top/egon/cola/platform/rbac3/admin/iam/organization/service/DirectoryQueryService.java; egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin/src/main/java/top/egon/cola/platform/rbac3/admin/iam/organization/service/DefaultDirectoryQueryService.java; egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin/src/main/java/top/egon/cola/platform/rbac3/admin/iam/organization/snapshot/repository/DirectoryCommandRepository.java; egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin/src/main/java/top/egon/cola/platform/rbac3/admin/iam/organization/snapshot/repository/DirectoryQueryRepository.java; egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin/src/main/java/top/egon/cola/platform/rbac3/admin/iam/organization/snapshot/repository/jpa/JpaDirectoryCommandRepository.java; egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin/src/main/java/top/egon/cola/platform/rbac3/admin/iam/organization/snapshot/repository/jpa/JpaDirectoryQueryRepository.java`,   `egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin/src/main/java/top/egon/cola/platform/rbac3/admin/iam/user/controller/InternalIdentityController.java; egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin/src/main/java/top/egon/cola/platform/rbac3/admin/iam/user/controller/InternalUserController.java; egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin/src/main/java/top/egon/cola/platform/rbac3/admin/iam/user/domain/dto/IdentityMembershipResolveRequestDTO.java; egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin/src/main/java/top/egon/cola/platform/rbac3/admin/iam/user/domain/exception/IdentityMembershipNotFoundException.java; egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin/src/main/java/top/egon/cola/platform/rbac3/admin/iam/user/domain/vo/TenantMembershipResponseVO.java; egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin/src/main/java/top/egon/cola/platform/rbac3/admin/iam/user/domain/vo/TenantMembershipVO.java; egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin/src/main/java/top/egon/cola/platform/rbac3/admin/iam/user/repository/IdentityMembershipRepository.java; egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin/src/main/java/top/egon/cola/platform/rbac3/admin/iam/user/repository/jpa/JpaIdentityMembershipRepository.java; egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin/src/main/java/top/egon/cola/platform/rbac3/admin/iam/user/repository/jpa/MembershipRow.java; egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin/src/main/java/top/egon/cola/platform/rbac3/admin/iam/user/service/IdentityMembershipFacade.java`
- Commit: `refactor(rbac3): remove tenant and membership authority`

- Additional compile-consumer paths included in the same Step 12 commit: `.../config/runtime/Rbac3ApplicationConfiguration.java`,
  `.../iam/organization/snapshot/service/DirectoryCommandService.java`,
  `.../controller/ControllerRequestParameterMetadataTest.java`, and
  `.../iam/role/RoleControlFacadeTest.java`; these were discovered by the required compile/test gate after the
  declared owner types were removed and contain no replacement authority.

- Execution state: `Committed` at `551d834e`; no empty or corrective Step 12 commit was created. RED first proved
  the deleted controller/request types and then compilation exposed the application-configuration, directory-service,
  controller-parameter-metadata and Step 10 TenantPO-test consumers; those were removed or narrowed to the approved
  TenantContext/state boundaries. The exact Maven command above passed 12 tests with 0 failures/errors/skips;
  module compile, main-Java stale-symbol search and `git diff --check` passed.
  No live PostgreSQL, IdP, service or browser was started. The four additional compile-consumer paths were necessary
  fallout of deleting the declared types: `Rbac3ApplicationConfiguration.java`, `DirectoryCommandService.java`,
  `ControllerRequestParameterMetadataTest.java`, and `RoleControlFacadeTest.java`.

### Step 13 — 让 DDC registration runtime 直接获取并提交 SERVICE Token

- Requirements: `REQ-005`, `REQ-009`, `REQ-010`, `REQ-018`
- Dependencies: Step 5
- Baseline state: DDC starter暴露DdcAdmissionTicketSupplier/Admission models；HTTP registration
  runtime每次register/heartbeat向supplier取Ticket。
- Observable outcome: DDC register/heartbeat transport字段为registrationToken；HTTP runtime用IdpServiceOAuth2Client请求DDC
  PLATFORM scope Token，无Ticket supplier/model/cache。
- End state: DDC starter/http-registration-starter tests GREEN；DDC admin尚在Step14适配新field。
- Execution state: `Committed` at `b045c318`; the required test-first RED was observed before the
  `registrationToken` accessor existed. The exact Step 13 command then passed 19 tests with 0
  failures/errors/skips (5 model, 6 HTTP runtime, 8 auto-configuration); RPC DDC adapter compile/test
  consumers also compiled after the protobuf field rename. Gateway RPC slot/MCP token callers and DDC
  integration-test fixtures were migrated because deleting the shared Ticket types exposed those reactor
  consumers. Main caller sources have no `DdcAdmissionTicket*`, `admissionTicket`, or legacy
  `admission_ticket` transport symbols; DDC Admin security symbols intentionally remain for Step 14.
  `git diff --check` passed. No live PostgreSQL, Redis, IdP, DDC, Gateway service, or browser was started.
- Test-first gate: Required — runtime/model/autoconfig tests因仍依赖Ticket types与无Idp facade dependency而RED。
- Ordered files:

#### File 1 —
`MODIFY egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-http-registration-starter/src/test/java/top/egon/cola/component/ddc/http/registration/DdcHttpRegistrationRuntimeTest.java; egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-http-registration-starter/src/test/java/top/egon/cola/component/ddc/http/registration/DdcHttpRegistrationAutoConfigurationTest.java; egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-starter/src/test/java/top/egon/cola/component/ddc/model/registry/DdcServiceRegistrationTest.java`

- Purpose: 定义Token request、register/heartbeat mapping、renewal/re-register与no Ticket symbol行为。
- Symbols: DDC audience/context/scope request capture；`registrationToken` accessors；runtime retry transitions。
- Repository evidence: current tests完整覆盖Ticket supplier calls和runtime lifecycle；可替换fixture保留状态机。
- Dependencies and consumers: Step5 facade；production models/runtime/autoconfig。
- Why now: 先把caller-visible contract从Ticket改成Token，再删除旧port/model。
- Contract/signature changes: request/registration/lease transport字段rename；runtime constructor
  dependency变为IdpServiceOAuth2Client。
- Input/output and state mapping: serviceKey biz/app/env + fixed DDC resource/scopes→IdpServiceTokenRequest PLATFORM→raw
  access token→registrationToken。
- Error and edge behavior: Token获取失败阻止ready/register；heartbeat Token renewal失败保持旧lease并按existing
  backoff重试/重新register；不复用expired token。
- Implementation pseudocode:

```java
capture each IdpServiceTokenRequest and assert DDC audience, PLATFORM context, null tenant and the exact minimum registration scope
start registration runtime, assert register carries the returned access token and heartbeat obtains a fresh or cached valid token through the facade
simulate token acquisition failure and assert no registry call or ready state, then recovery re-registers according to the existing lifecycle
search serialized models and test fixtures for admissionTicket/Ticket supplier and require only registrationToken terminology
```

- Verification contribution: RED/GREEN `TEST-009/024/029` caller slice。
- After this file: tests RED againstTicket runtime。

#### File 2 —
`MODIFY egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-starter/src/main/java/top/egon/cola/component/ddc/model/config/DdcInstanceRegisterRequest.java; egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-starter/src/main/java/top/egon/cola/component/ddc/model/config/DdcHeartbeatRequest.java; egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-starter/src/main/java/top/egon/cola/component/ddc/model/registry/DdcServiceRegistration.java; egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-starter/src/main/java/top/egon/cola/component/ddc/model/registry/DdcServiceLeaseRequest.java`

- Purpose: 修改共享 transport/domain fields，把 credential component 改为 registrationToken。
- Symbols: `registrationToken` getter/setter/constructor mapping；old package absent。
- Repository evidence: current runtime/admin mappings引用这四个request types；Spec DDC physical schema unchanged但Java
  transport rename。
- Dependencies and consumers: HTTP runtime、DDC admin mappers/services；Step14同步server。
- Why now: File1固定serialization后修改最小shared source。
- Contract/signature changes: Java/serialized field从admissionTicket到registrationToken；其他biz/app/env/instance/lease
  fields不变。
- Input/output and state mapping: raw OAuth Token只在transport经过；不进入日志/toString；expiry由server verified
  claims产生而非caller。
- Error and edge behavior: blank/oversize token validation fail；old JSON/proto field故意不双读；old Ticket classes不能加载。
- Implementation pseudocode:

```java
rename the credential component on register and heartbeat models to registrationToken while preserving every source, instance and lease field
validate registrationToken as required opaque text with bounded length and exclude it from toString, equals diagnostics and observability labels
update copy/build mapping among config and registry request types without decoding or trusting token claims on the caller
preserve all non-credential source, instance and lease fields so only the credential representation changes
```

- Verification contribution: model serialization and redaction GREEN。
- After this file: shared transport uses Token；obsolete Ticket types remain for File 3 deletion。

#### File 3 —
`DELETE egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-starter/src/main/java/top/egon/cola/component/ddc/api/extension/DdcAdmissionTicketSupplier.java; egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-starter/src/main/java/top/egon/cola/component/ddc/model/admission/DdcAdmissionRequest.java; egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-starter/src/main/java/top/egon/cola/component/ddc/model/admission/DdcAdmissionTicket.java`

- Purpose: 物理删除 Ticket supplier extension 与 Admission request/ticket models。
- Symbols: three Ticket/Admission Java types absent。
- Repository evidence: 三个 targets 在续执行基线存在；File 2 removes shared model references and Step 5 provides replacement facade。
- Dependencies and consumers: File 4 HTTP registration runtime；Step 14 server；Step 18 global symbol gate。
- Why now: Token transport is established，so the caller side must not retain a Ticket acquisition/cache dual track。
- Contract/signature changes: old extension/model package types disappear；registrationToken is the only caller credential field。
- Input/output and state mapping: Not applicable — deleted Ticket objects are replaced by opaque OAuth Token transport, not migrated state。
- Error and edge behavior: any remaining consumer must be migrated in this Step；do not retain adapter wrappers or old JSON field aliases。
- Implementation pseudocode:

```text
delete DdcAdmissionTicketSupplier after every runtime dependency is planned to use IdpServiceOAuth2Client
delete DdcAdmissionRequest and DdcAdmissionTicket after the four shared request models expose registrationToken
search starter and HTTP registration sources for the deleted types and old admissionTicket field and require zero matches
```

- Verification contribution: static `TEST-029` caller-side Ticket deletion。
- After this file: DDC shared starter has no Ticket extension/model；HTTP runtime remains for File 4。

#### File 4 —
`MODIFY egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-http-registration-starter/pom.xml; egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-http-registration-starter/src/main/java/top/egon/cola/component/ddc/http/registration/DdcHttpRegistrationRuntime.java; egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-http-registration-starter/src/main/java/top/egon/cola/component/ddc/http/registration/DdcHttpRegistrationAutoConfiguration.java; egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-http-registration-starter/src/main/java/top/egon/cola/component/ddc/http/registration/DdcHttpRegistrationProperties.java`

- Purpose: 依赖idp-starter facade并实现DDC定向PLATFORM token acquisition。
- Symbols: IdpServiceOAuth2Client injection；DDC resource/scopes/registration id properties；runtime `registrationToken`
  mapping。
- Repository evidence: current runtime state machine可复用；Step5已移除idp-starter→DDC dependency，因此新增DDC→IdP不成环。
- Dependencies and consumers: IdpServiceOAuth2Client、registry facade；service app配置。
- Why now: sharedmodel ready后完成caller implementation/dependency inversion。
- Contract/signature changes: autoconfig bean dependency替换；旧admission property删除/legacy error由idp starter处理。
- Input/output and state mapping: properties+service identity→typed token
  request；result.tokenValue→register/heartbeat；lease/session logic不变。
- Error and edge behavior: missingfacade/registration/appId/resource fail startup；token error遵循runtime backoff；never
  log raw token。
- Implementation pseudocode:

```java
add egon-cola-tianquan-shoubing-starter as the compile dependency and inject IdpServiceOAuth2Client into registration auto-configuration
build one DDC PLATFORM token request from registrationId, appId, configured DDC resource URI and fixed registration scope for each credential refresh point
set the resulting token value on register and heartbeat requests while retaining existing lease retry, deregister and readiness transitions
fail startup on missing service-client identity/config and redact every token/secret from exception, logger and actuator property output
```

- Verification contribution: makesFile1 GREEN and provesdependency direction。
- After this file: DDC caller noTicket；server needsStep14。

- Validation working directory: `/Users/mario/SelfProject/Egon-COLA`
- Verification command:
  `./mvnw -B -ntp -pl egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-http-registration-starter -am -Dtest=DdcHttpRegistrationRuntimeTest,DdcHttpRegistrationAutoConfigurationTest,DdcServiceRegistrationTest -Dsurefire.failIfNoSpecifiedTests=false test`
- Expected result: exit 0；Token request/register/heartbeat/recovery/model tests通过；DDC starter无Ticket types。
- Failure returns to: File1 behavior；File2 model mapping；File3 Ticket deletion；File4 dependency/autoconfig/runtime。
- Completion criteria: all DDC callers提交SERVICE Token且无Admission Ticket acquisition/cache。
- Rollback: revert四组paths仅在admin未切换/未部署时；发布中caller/admin必须同一兼容单元。
- Commit paths: `egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-http-registration-starter/src/test/java/top/egon/cola/component/ddc/http/registration/DdcHttpRegistrationRuntimeTest.java; egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-http-registration-starter/src/test/java/top/egon/cola/component/ddc/http/registration/DdcHttpRegistrationAutoConfigurationTest.java; egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-starter/src/test/java/top/egon/cola/component/ddc/model/registry/DdcServiceRegistrationTest.java`,   `egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-starter/src/main/java/top/egon/cola/component/ddc/model/config/DdcInstanceRegisterRequest.java; egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-starter/src/main/java/top/egon/cola/component/ddc/model/config/DdcHeartbeatRequest.java; egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-starter/src/main/java/top/egon/cola/component/ddc/model/registry/DdcServiceRegistration.java; egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-starter/src/main/java/top/egon/cola/component/ddc/model/registry/DdcServiceLeaseRequest.java; egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-starter/src/main/java/top/egon/cola/component/ddc/api/extension/DdcAdmissionTicketSupplier.java; egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-starter/src/main/java/top/egon/cola/component/ddc/model/admission/DdcAdmissionRequest.java; egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-starter/src/main/java/top/egon/cola/component/ddc/model/admission/DdcAdmissionTicket.java`,   `egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-http-registration-starter/pom.xml; egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-http-registration-starter/src/main/java/top/egon/cola/component/ddc/http/registration/DdcHttpRegistrationRuntime.java; egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-http-registration-starter/src/main/java/top/egon/cola/component/ddc/http/registration/DdcHttpRegistrationAutoConfiguration.java; egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-http-registration-starter/src/main/java/top/egon/cola/component/ddc/http/registration/DdcHttpRegistrationProperties.java`
- Commit: `feat(ddc): register with idp service tokens`

- Additional compile-consumer paths included in the same Step 13 commit: RPC DDC adapter protobuf
  contracts/mappers/registry bridge and tests; DDC starter `DdcInstanceService`/auto-configuration and
  lifecycle tests; Gateway `RpcGatewaySlotRuntime`, `GatewayEngineConfiguration`, MCP service-token
  adapter and tests; RPC process test fixtures; DDC integration-test fixtures; and Gateway Admin DDC
  configuration test. These were necessary fallout of the declared Ticket type deletion, not new
  authorization surfaces.

### Step 14 — 让 DDC 验证 PLATFORM SERVICE Token 并绑定 lease identity

- Requirements: `REQ-007`, `REQ-009`, `REQ-010`, `REQ-018`
- Dependencies: Steps 2, 4 and 13
- Baseline state: DDC admin verifier校验Admission Ticket claims/package
  naming；lease/repos使用DdcAdmissionClaims与admissionExpiresAt Java语义。
- Observable outcome: registrationToken经IdP verifier/source projection验证DDC
  audience、PLATFORM、scope、app/client/source/credential/biz/app/env/instance，产出immutable verified
  identity；register/heartbeat lease≤token exp且精确绑定。
- End state: DDC admin/component tests GREEN，Java无Ticket/Admission security symbols；Redis/SQL旧物理字段通过explicit
  mapping保留。
- Execution state: `Committed` at `233eb901`; the initial rename/consumer compile exposed stale DDC Admin
  Admission imports and the missing SERVICE-token wiring, then the renamed verifier, immutable identity, lease/repository
  binding, registration configuration, and compatibility consumers were completed. The exact Step 14 command passed
  36 tests with 0 failures/errors/skips; the admin `test-compile` reactor gate passed with `-Dmaven.compiler.proc=full`,
  `git diff --check` and the DDC old-security-symbol search passed, and no DDC migration file was added. Additional
  compile-consumer fallout included `DdcAuthBootstrapController`, DDC integration/RPC fixtures, and the registration
  JSON explicit mapping; no live PostgreSQL, Redis, IdP, DDC, Gateway service, or browser was started.
- Test-first gate: Required — verifier/lease/repository tests扩展app/context/instance/replay矩阵后因旧claims缺字段/语义而RED。
- Ordered files:

#### File 1 —
`RENAME egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/test/java/top/egon/cola/component/ddc/admin/security/admission/IdpJwtDdcAdmissionVerifierTest.java -> egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/test/java/top/egon/cola/component/ddc/admin/security/registration/IdpJwtDdcRegistrationCredentialVerifierTest.java; egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/test/java/top/egon/cola/component/ddc/admin/security/admission/DdcAdmissionTestFixture.java -> egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/test/java/top/egon/cola/component/ddc/admin/security/registration/DdcRegistrationTestFixture.java`

- Purpose: 语义重命名 verifier test 与 fixture，先建立 registration security terminology。
- Symbols: `IdpJwtDdcRegistrationCredentialVerifierTest`、`DdcRegistrationTestFixture`、`VerifiedDdcRegistrationIdentity`
  assertions、repo serialization keys。
- Repository evidence: current tests已覆盖issuer/aud/source/version/expiry和Redis identity checks，扩展而非重建。
- Dependencies and consumers: production rename/Files 3–4；File 2 retained lease/repository tests。
- Why now: 先固定“删Ticket但不删防伪”的全部negative matrix。
- Contract/signature changes: renamed test/fixture inputs 改 SERVICE Token；assert appId/clientId/context/credential/instance。
- Input/output and state mapping: JWT claims+request tuple+projection→verified identity→Redis lease fields/expiry。
- Error and edge behavior: any mismatch/unavailable/expired/replay no write；concurrent register deterministic；old Ticket
  token type/audience rejected。
- Implementation pseudocode:

```java
verify valid DDC-audience SERVICE token with PLATFORM context, exact scope, app/client/source/credential claims and matching biz/app/env/instance request
vary issuer, alg, audience, context, tid, scope, appId, clientId, source tuple, resource version, credential and expiry one at a time and assert zero writes
keep all prior issuer, audience, source, resource version and expiry negative cases while changing only Java security terminology
require the renamed fixture to construct no Admission Ticket object and to expose only SERVICE token plus registration request facts
```

- Verification contribution: verifier matrix RED point and Java rename proof。
- After this file: renamed verifier test/fixture compile only after production File 3；retained tests remain for File 2。

#### File 2 —
`MODIFY egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/test/java/top/egon/cola/component/ddc/admin/service/lease/DdcConfigLeaseServiceTest.java; egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/test/java/top/egon/cola/component/ddc/admin/repository/DdcConfigLeaseRedisRepositoryTest.java; egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/test/java/top/egon/cola/component/ddc/admin/repository/DdcServiceRegistryRedisRepositoryTest.java`

- Purpose: 修改保留的 lease/repository tests，覆盖 Token expiry、binding、replay/concurrency 与 legacy physical mapping。
- Symbols: lease upper bound、heartbeat mismatch、Redis identity serialization assertions。
- Repository evidence: 三个 tests 在续执行基线存在并覆盖现有 Admission claims/Redis mapping，可原地演进。
- Dependencies and consumers: File 1 renamed fixture、Files 3–4 verified identity/services/repos。
- Why now: security test rename 后扩展保留行为，不把既有 tests 误标为 rename targets。
- Contract/signature changes: test fixtures consume Verified registration identity；physical `admissionExpiresAt` key remains mapped。
- Input/output and state mapping: verified identity + request tuple→lease/registry Redis fields with expiry capped by Token exp。
- Error and edge behavior: mismatched app/env/instance/lease or expired token causes zero mutation；concurrent register deterministic。
- Implementation pseudocode:

```java
register and heartbeat with a verified identity and assert lease expiry never exceeds the SERVICE token expiration
vary app, environment, instance and lease id one at a time and assert the original Redis record remains unchanged
serialize through both repositories and assert legacy physical keys remain readable while Java callers use registration terminology
```

- Verification contribution: RED/GREEN `TEST-010`–`TEST-013`,`TEST-024` lease/repository slice。
- After this file: retained tests RED against old claims/verifier and incomplete bindings。

#### File 3 —
`RENAME egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/java/top/egon/cola/component/ddc/admin/security/admission/DdcAdmissionClaims.java -> egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/java/top/egon/cola/component/ddc/admin/security/registration/VerifiedDdcRegistrationIdentity.java; egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/java/top/egon/cola/component/ddc/admin/security/admission/DdcAdmissionVerifier.java -> egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/java/top/egon/cola/component/ddc/admin/security/registration/DdcRegistrationCredentialVerifier.java; egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/java/top/egon/cola/component/ddc/admin/security/admission/DdcAdmissionException.java -> egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/java/top/egon/cola/component/ddc/admin/security/registration/DdcRegistrationAuthenticationException.java; egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/java/top/egon/cola/component/ddc/admin/security/admission/IdpJwtDdcAdmissionVerifier.java -> egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/java/top/egon/cola/component/ddc/admin/security/registration/IdpJwtDdcRegistrationCredentialVerifier.java; egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/java/top/egon/cola/component/ddc/admin/security/admission/package-info.java -> egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/java/top/egon/cola/component/ddc/admin/security/registration/package-info.java`

- Purpose: 语义rename并把raw token转为完整trusted identity。
- Symbols: four renamed types；verified record新增appId/clientId/source/resource/credential/instance/expiresAt。
- Repository evidence: existing verifier是可复用的direct orchestration；Spec PLAN-CLAR-004禁止本次改DB field names。
- Dependencies and consumers: IdpJwtVerifier/state readers、lease/services/repos。
- Why now: File1固定security behavior后改最小现有实现，不新增第二validator。
- Contract/signature changes: verifier input `registrationToken + request`；output immutable identity；exception stable
  DDC status。
- Input/output and state mapping: decoded ServiceIdentityPrincipal/request/projection→record；instance fromrequest only
  aftercross-check/binding。
- Error and edge behavior: fail closed on projection unavailable/stale/disabled；no token/secret in exception；PLATFORM
  only。
- Implementation pseudocode:

```java
verify registrationToken through ServiceAccessTokenVerifier and require principal context PLATFORM, null tenant, DDC audience and exact registration scope
load current Resource projection and cross-check appId, clientId source biz/app/environment, resource id/version/status and credential id
validate request biz/app/env/instance against trusted claims and construct VerifiedDdcRegistrationIdentity only after every comparison succeeds
throw DdcRegistrationAuthenticationException with stable low-cardinality reason and no token, authorization header or secret text
```

- Verification contribution: verifier matrix GREEN；Java semantics noAdmission。
- After this file: verifier returns new identity；consumers未编译直到File4。

#### File 4 —
`MODIFY egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/java/top/egon/cola/component/ddc/admin/config/DdcAdminProperties.java; egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/java/top/egon/cola/component/ddc/admin/config/DdcAdminSecurityPropertiesValidator.java; egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/java/top/egon/cola/component/ddc/admin/config/DdcAdminRedisConfig.java; egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/java/top/egon/cola/component/ddc/admin/service/lease/DdcConfigLeaseService.java; egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/java/top/egon/cola/component/ddc/admin/service/lease/DdcLeaseValidator.java; egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/java/top/egon/cola/component/ddc/admin/service/registry/DdcServiceRegistryService.java; egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/java/top/egon/cola/component/ddc/admin/repository/DdcConfigLeaseRedisRepository.java; egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/java/top/egon/cola/component/ddc/admin/repository/DdcServiceRegistryRedisRepository.java; egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/resources/application.yml`

- Purpose: 重接config/services/repos并实现identity-bound lease/expiry/recovery。
- Symbols: registration security properties/verifier bean；service register/heartbeat；legacy Redis key mapping constants。
- Repository evidence: current service/repos already capexpiry andcompare resource; extend tofull identity/instance。
- Dependencies and consumers: File3 types；RPC/HTTP registry providers通过existing facade间接调用。
- Why now: verifier ready后更新write path，保证未verified request不能进入repository。
- Contract/signature changes: service/repo methods接受Verified identity而非claims；config `admission` rename
  `registration`并旧key明确失败。
- Input/output and state mapping: verified identity+configured lease TTL→expiry=min(now+TTL,token exp)；all bindings
  persisted/compared。
- Error and edge behavior: verification/projection/Redis failure no success；heartbeat mismatch no mutation；expired lease
  notrevived；new validregister createsnewleaseId。
- Implementation pseudocode:

```java
service register verifies raw request first, then passes only VerifiedDdcRegistrationIdentity to repositories and caps lease expiry at token expiration
heartbeat re-verifies a fresh token and requires stored appId, clientId, source, environment, instanceId and leaseId to match before renewal
repositories persist the expanded identity using existing Redis/SQL physical admission fields through explicit mapping and never trust caller expiry
rename configuration to registration, reject legacy Ticket-oriented keys, and keep bounded metrics without token/client high-cardinality values
```

- Verification contribution: makes Files1–2 verifier/repo/lease tests GREEN；DDC no migration invariant。
- After this file: DDC caller/server Token path完整，无Admission Java security symbols。

#### File 5 —
`MODIFY egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/java/top/egon/cola/component/ddc/admin/service/lease/DdcInstanceAdminService.java; egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/test/java/top/egon/cola/component/ddc/admin/config/DdcAdminSecurityPropertiesTest.java; egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/test/java/top/egon/cola/component/ddc/admin/config/DdcAdminRedisConfigTest.java; egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/test/java/top/egon/cola/component/ddc/admin/service/registry/DdcServiceRegistryServiceTest.java; egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/test/java/top/egon/cola/component/ddc/admin/repository/DdcV8MigrationTest.java`

- Purpose: 更新rename的剩余服务/配置/测试消费者，并证明DDC V8已有物理admission列不被新migration改写。
- Symbols: DdcInstanceAdminService verified identity imports；registration properties/beans tests；registry fixture；V8
  legacy column assertions。
- Repository evidence: repository-wide `DdcAdmission*` consumer search列出这些路径；DDC
  `V8__add_resource_admission_audit.sql` immutable。
- Dependencies and consumers: Files1–4 renamed types/config/services；DDC admin context/module compile。
- Why now: primary verifier/lease pathGREEN后进行mechanical consumer alignment，防止final reactor临时暴露遗漏。
- Contract/signature changes: Java imports/fixture names更新；physical JSON/SQL key assertions明确仍是legacy
  mapping而非public Java contract。
- Input/output and state mapping: verified registration identity→admin list/registry views；registration config→verifier
  bean；old DB/Redis keys→explicit mapping assertions。
- Error and edge behavior: no test or service may instantiate old Admission claims/exception；existing V8
  checksum/columns remain unchanged；config legacy keys fail deterministically。
- Implementation pseudocode:

```java
replace remaining DdcAdmission type imports in admin services and tests with the registration verifier, verified identity and registration exception names
update security properties and Redis configuration tests to bind registration keys and assert retired Ticket-oriented keys fail without exposing values
keep DdcV8MigrationTest assertions on existing physical admission columns and checksums, documenting that Java mapping changed but no DDC migration was added
run repository-wide main/test import search and require zero current security/admission package or DdcAdmission class references
```

- Verification contribution: DDC admin reactor compile、config/context regression、no-migration proof。
- After this file: allknown DDC admin consumers use registration semantics，old Java package absent。

- Validation working directory: `/Users/mario/SelfProject/Egon-COLA`
- Verification command:
  `./mvnw -B -ntp -pl egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin,egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-test -am -Dtest=IdpJwtDdcRegistrationCredentialVerifierTest,DdcConfigLeaseServiceTest,DdcConfigLeaseRedisRepositoryTest,DdcServiceRegistryRedisRepositoryTest,DdcAdminSecurityPropertiesTest,DdcAdminRedisConfigTest,DdcServiceRegistryServiceTest,DdcV8MigrationTest,DdcResourceAdmissionLifecycleTest -Dsurefire.failIfNoSpecifiedTests=false test`
- Expected result: exit 0；claim/source/replay/concurrency/expiry与legacy mapping通过；DDC migration目录无新文件。
- Failure returns to: Files1–2 matrix/fixture/retained tests；File3 verifier/model/package rename；File4 service/repository/config；File5
  stale consumer or immutable physical mapping regression。
- Completion criteria: direct SERVICE Token保持全部防伪/lease约束，Ticket链删除，DDC schema unchanged。
- Rollback: caller/admin必须同版本rollback；Redis旧physical mapping允许整组旧binary恢复但需同步IdP credentials/snapshots。
- Commit paths: `egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/test/java/top/egon/cola/component/ddc/admin/security/admission/IdpJwtDdcAdmissionVerifierTest.java -> egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/test/java/top/egon/cola/component/ddc/admin/security/registration/IdpJwtDdcRegistrationCredentialVerifierTest.java; egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/test/java/top/egon/cola/component/ddc/admin/security/admission/DdcAdmissionTestFixture.java -> egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/test/java/top/egon/cola/component/ddc/admin/security/registration/DdcRegistrationTestFixture.java; egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/test/java/top/egon/cola/component/ddc/admin/service/lease/DdcConfigLeaseServiceTest.java; egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/test/java/top/egon/cola/component/ddc/admin/repository/DdcConfigLeaseRedisRepositoryTest.java; egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/test/java/top/egon/cola/component/ddc/admin/repository/DdcServiceRegistryRedisRepositoryTest.java`,   `egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/java/top/egon/cola/component/ddc/admin/security/admission/DdcAdmissionClaims.java -> egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/java/top/egon/cola/component/ddc/admin/security/registration/VerifiedDdcRegistrationIdentity.java; egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/java/top/egon/cola/component/ddc/admin/security/admission/DdcAdmissionVerifier.java -> egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/java/top/egon/cola/component/ddc/admin/security/registration/DdcRegistrationCredentialVerifier.java; egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/java/top/egon/cola/component/ddc/admin/security/admission/DdcAdmissionException.java -> egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/java/top/egon/cola/component/ddc/admin/security/registration/DdcRegistrationAuthenticationException.java; egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/java/top/egon/cola/component/ddc/admin/security/admission/IdpJwtDdcAdmissionVerifier.java -> egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/java/top/egon/cola/component/ddc/admin/security/registration/IdpJwtDdcRegistrationCredentialVerifier.java; egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/java/top/egon/cola/component/ddc/admin/security/admission/package-info.java -> egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/java/top/egon/cola/component/ddc/admin/security/registration/package-info.java`,   `egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/java/top/egon/cola/component/ddc/admin/config/DdcAdminProperties.java; egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/java/top/egon/cola/component/ddc/admin/config/DdcAdminSecurityPropertiesValidator.java; egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/java/top/egon/cola/component/ddc/admin/config/DdcAdminRedisConfig.java; egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/java/top/egon/cola/component/ddc/admin/service/lease/DdcConfigLeaseService.java; egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/java/top/egon/cola/component/ddc/admin/service/lease/DdcLeaseValidator.java; egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/java/top/egon/cola/component/ddc/admin/service/registry/DdcServiceRegistryService.java; egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/java/top/egon/cola/component/ddc/admin/repository/DdcConfigLeaseRedisRepository.java; egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/java/top/egon/cola/component/ddc/admin/repository/DdcServiceRegistryRedisRepository.java; egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/resources/application.yml`,   `egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/java/top/egon/cola/component/ddc/admin/service/lease/DdcInstanceAdminService.java; egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/test/java/top/egon/cola/component/ddc/admin/config/DdcAdminSecurityPropertiesTest.java; egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/test/java/top/egon/cola/component/ddc/admin/config/DdcAdminRedisConfigTest.java; egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/test/java/top/egon/cola/component/ddc/admin/service/registry/DdcServiceRegistryServiceTest.java; egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/test/java/top/egon/cola/component/ddc/admin/repository/DdcV8MigrationTest.java`
- Commit: `feat(ddc): verify service tokens for registration leases`

### Step 15 — 完成 IdP Client/Secret Web 并移除 Client JWK UI

- Requirements: `REQ-001`, `REQ-002`, `REQ-003`, `REQ-016`, `REQ-019`
- Dependencies: Steps 3–4
- Baseline state: ClientListPage无appId/Secret；ResourceServerListPage管理JWK/TTL；types仍含ClientJwkVO。
- Observable outcome: Client create/rotate一次性modal可复制并关闭清空；list/detail只显示hint/status；existing
  permissions精确控制；Resource/Grant UI无Client JWK且支持TENANT/PLATFORM Grant。
- End state: IdP Client/Resource Web tests/typecheck/build GREEN；SigningKeyPage无diff。
- Test-first gate: Required — Testing Library tests因one-time modal/fields/permission/removed JWK controls缺失而RED。
- Execution state: `Committed` at `20d19d50`；完成后续执行记录如下。
- Execution evidence: File 1 先以缺少 App ID/one-time Secret/rotation UI 的 RED 测试启动；随后完成
  `ClientListPage` 的 appId/secretHint/secretStatus safe contract、CONFIDENTIAL App ID 校验、一次性
  create/rotate Secret modal、copyable fields、close/unmount 清理与 `expectedVersion` rotation；PUBLIC
  create 不展示空 Secret。File 2 扩展 App regression 与 Resource Server retired-control absence。
  File 4 删除 Resource Server 的 Client JWK/准入 TTL UI 和 types，并将 Grant UI 改为
  `USER_DELEGATION`/`CLIENT_CREDENTIALS` 与显式 `TENANT`/`PLATFORM` context，PLATFORM 请求省略 tenantId。
  `npm test -- ClientListPage.test.tsx App.test.tsx` 通过（8 tests）；`npm run lint`、`npm run typecheck`
  与 `npm run build` 通过；source/bundle retired-symbol search 与 persisted-secret pattern search
  为空；`git diff --check` 通过。提交仅包含本 Step 的 6 个前端路径，Plan/Spec 文档修改保持未暂存。
- Contract note: 当前 IdP admin Resource Grant Java DTO/service 尚未在前序 Step 将 V5 `grant_context`
  列映射为显式管理 API 字段；本 Step 的 Web/types 已按主规格的 `scopeContext` contract 发出
  `TENANT`/`PLATFORM`，该后端 contract alignment 必须在后续执行审计中补齐后才可宣称 live Grant
  flow 已联调，不能将本 Step 的前端 GREEN 误作后端/数据库联调证明。
- Ordered files:

#### File 1 —
`CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin-web/src/features/clients/ClientListPage.test.tsx`

- Purpose: 定义create/rotatemodal、copy/focus/clear、permission/loading/error与no persisted Secret。
- Symbols: component tests for read/create/update permissions and one-time response lifecycle。
- Repository evidence: App/Auth tests与PageState/usePermission/current ClientListPage patterns。
- Dependencies and consumers: types/page/resource UI Files2–4。
- Why now: frontend behavior先RED，固定security UX。
- Contract/signature changes: mock API usesCreated/Rotated VO；no browser storage assertions。
- Input/output and state mapping: form→DTO；response secret→local modal state only；close/success→zero state/cache
  invalidation。
- Error and edge behavior: denied hides actions；server error preserves form但无Secret；double-submit disabled；modal
  close不可reopen recovered secret。
- Implementation pseudocode:

```typescript
render ClientListPage with read only, create, and update permission sets and assert exact actions for each existing permission code
submit CONFIDENTIAL form with appId/clientId, resolve one-time response and assert AppID, App Key, Secret plus accessible copy/focus behavior
close the modal, rerender/refetch and assert secret is absent from DOM, query cache, localStorage, sessionStorage and request URLs
rotate with confirmation and expectedVersion, asserting pending disable, new one-time modal and old hint replacement only after success
```

- Verification contribution: RED/GREEN `TEST-018/027/030/031`。
- After this file: tests RED againstcurrent page/types。

#### File 2 —
`MODIFY egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin-web/src/app/App.test.tsx`

- Purpose: 修改既有 app regression，固定 Client route/nav 与 retired JWK/Admission UI absence。
- Symbols: authorized/denied Client navigation assertions、no Client JWK controls/routes。
- Repository evidence: target test exists at resume baseline and already covers console routing/nav；可原地扩展。
- Dependencies and consumers: Files 3–4 Client/Resource pages；Step 16 later extends the same app test。
- Why now: new page test after，separately evolve the existing application regression rather than mislabel it CREATE。
- Contract/signature changes: no new route；existing Client route keeps exact permission guard and no retired controls。
- Input/output and state mapping: permission set + current path→visible nav/page；no Secret material enters app-level state。
- Error and edge behavior: denied user sees no management action；SigningKeyPage remains；unknown route behavior unchanged。
- Implementation pseudocode:

```typescript
render App with read, create and update OAuth client permission combinations and preserve existing route assertions
assert resource-server navigation exposes no Client JWK or Admission TTL management entry while SigningKey navigation remains
assert one-time Secret values never enter app-level route state, URLs or persisted test storage after modal close
```

- Verification contribution: app-level `TEST-027/030/031` regression and retired UI absence。
- After this file: App regression is ready；Client page/types remain for File 3。

#### File 3 —
`MODIFY egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin-web/src/api/types.ts; egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin-web/src/features/clients/ClientListPage.tsx`

- Purpose: 实现AppID/Secret typed contracts与one-time modal state。
- Symbols: `CreatedOAuthClientVO/RotatedClientSecretVO/TenantContext` types；Client table/drawer/forms/mutations。
- Repository evidence: current single-page React Query/Ant Design architecture；no separateglobal store needed。
- Dependencies and consumers: backend API-001/002；query cache。
- Why now: tests固定state后最小修改existing page/types。
- Contract/signature changes: create response type changes；rotate endpoint added；safe OAuthClientVO expanded。
- Input/output and state mapping: exact fields/enum/time；Secret keptuseState only and clearedonCancel/unmount；invalidate
  clients on success。
- Error and edge behavior: no Secret analytics/log/URL/storage；validation/409 errors mapped；accessible
  confirmation/copy/focus。
- Implementation pseudocode:

```typescript
extend API types with appId, secret hint/status and dedicated one-time create/rotate responses without adding secret to OAuthClientVO
map create form fields exactly, store successful one-time credentials in modal-local state and clear state plus form on every close/unmount
add rotate confirmation under idp:oauth-client:update, send expectedVersion and invalidate the client query only after success
render loading, empty, populated, validation, conflict, denied and retry states through existing Ant Design/PageState patterns
```

- Verification contribution: Client tests GREEN/typecheck。
- After this file: Client UX完成；Resource JWK controls待File4。

#### File 4 —
`MODIFY egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin-web/src/features/resource-servers/ResourceServerListPage.tsx; egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin-web/src/features/resource-grants/ClientResourceGrantPage.tsx`

- Purpose: 删除Client JWK/Admission TTL controls/types usage并让Grant UI显式选择TENANT/PLATFORM合法字段。
- Symbols: Resource create/detail forms；Grant context selector/tenant conditional validation。
- Repository evidence: current pages inline typed API/mutations；Step4 backendremoved fields/routes。
- Dependencies and consumers: updated types/backend resource/grant contracts。
- Why now: Client page完成后清理同一operator flow的旧credential UI。
- Contract/signature changes: no key create/revoke/admission TTL requests；Grant context field controls tenantId
  presence。
- Input/output and state mapping: PLATFORM→no tenantId；TENANT→required selected tenantId；scopes/version fields
  unchanged。
- Error and edge behavior: stale grant conflict refresh；Resource disabled states preserved；SigningKeyPage unaffected。
- Implementation pseudocode:

```typescript
remove JWK table, add/revoke controls, key DTO fields and admission TTL form fields from Resource Server create/detail flows
retain source Resource identity, status/version and management client mappings required by service token issuance and DDC projection
render TENANT versus PLATFORM grant context, require tenantId only for TENANT and omit it entirely for PLATFORM requests
preserve query invalidation, conflict refresh, loading, empty, denied and error states while leaving SigningKeyPage untouched
```

- Verification contribution: frontend typecheck/build and App negative navigation/assertions。
- After this file: IdP Client/Resource Web符合new backend，no Client JWK UI。

- Validation working directory:
  `/Users/mario/SelfProject/Egon-COLA/egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin-web`
- Verification command:
  `npm test -- ClientListPage.test.tsx App.test.tsx && npm run lint && npm run typecheck && npm run build`
- Expected result: exit 0；component tests/typecheck/Vite build通过，bundle/search无Client JWK/Admission TTL或persisted
  Secret逻辑。
- Failure returns to: File1 page behavior；File2 app regression；File3 Client mapping/state；File4 Resource/Grant compatibility。
- Completion criteria: secure one-time Secret UX与existing permissions完整，SigningKeyPage保持。
- Rollback: revert四组frontend paths；backend已cutover时不能部署旧bundle调用removed routes。
- Commit paths: `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin-web/src/features/clients/ClientListPage.test.tsx; egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin-web/src/app/App.test.tsx`,   `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin-web/src/api/types.ts; egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin-web/src/features/clients/ClientListPage.tsx`,   `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin-web/src/features/resource-servers/ResourceServerListPage.tsx; egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin-web/src/features/resource-grants/ClientResourceGrantPage.tsx`
- Commit: `feat(idp-web): manage app secrets and remove client jwks`

### Step 16 — 增加 IdP tenant/member 管理页面

- Requirements: `REQ-011`, `REQ-016`
- Dependencies: Step 7
- Baseline state: IdP Web无`/tenants` route/nav/types/page。
- Observable outcome: `idp:tenant:read/manage`驱动tenant list/detail drawer、create/update、membership list/upsert和完整UI
  states。
- End state: IdP tenant Web tests/typecheck/build GREEN。
- Test-first gate: Required — TenantListPage/App routing tests因page/route/nav/types不存在而RED。
- Execution state: `Committed` at `8bd5ea76`；完成后续执行记录如下。
- Execution evidence: File 1 先因 `TenantListPage` 不存在而按预期 RED；随后新增 API-005–009 transport
  types 和 `TenantListPage`，实现分页列表、详情、创建/版本更新、成员读取与 ACTIVE/DISABLED upsert，
  Settings JSON 校验、CLOSED tenant read-only、loading/empty/error/retry、permission guard 与 query
  invalidation。File 4 注册 `/tenants` lazy route、导航/面包屑、Overview 权限模块和 App route regression。
  `npm test -- TenantListPage.test.tsx App.test.tsx` 通过（8 tests）；`npm run lint`、
  `npm run typecheck` 与 `npm run build` 通过；tenant page source 无 RBAC endpoint/storage 调用；
  `git diff --check` 通过。提交只包含本 Step 声明的 7 个 frontend paths，Plan/Spec 文档保持未暂存。
- Ordered files:

#### File 1 —
`CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin-web/src/features/tenants/TenantListPage.test.tsx`

- Purpose: 定义route/page permissions、catalog/member flows、confirm/optimistic version与a11y states。
- Symbols: list/create/update/open drawer/upsert member tests。
- Repository evidence: ClientListPage tests/PageState/React Query/Ant Design patterns。
- Dependencies and consumers: page/types/router/layout。
- Why now: 先固定UI behavior与exact API mapping。
- Contract/signature changes: mocks API-005–009；no RBAC endpoint。
- Input/output and state mapping: tenant/member VO→table/drawer；forms→DTO；status/time/null labels。
- Error and edge behavior: loading/empty/error/retry/denied/disabled；double submit；version conflict refresh；focus
  restore。
- Implementation pseudocode:

```typescript
render tenant page for read, manage and denied permission sets and assert navigation plus mutation controls are correctly guarded
exercise list empty/populated/error retry, create tenant, edit status/settings with expectedVersion and open the membership drawer
upsert ACTIVE and DISABLED memberships, assert query invalidation and conflict refresh while preserving form values on errors
verify keyboard focus, accessible modal names, confirmation for destructive state transitions and no calls to RBAC tenant/member APIs
```

- Verification contribution: RED/GREEN `TEST-017/018/027` tenant UI。
- After this file: test RED forabsent page。

#### File 2 —
`MODIFY egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin-web/src/api/types.ts`

- Purpose: 修改共享 API types，加入 API-005–009 的 exact tenant/member DTO/VO contracts。
- Symbols: tenant/member DTO/VO/page types。
- Repository evidence: backend Step7 contract与existing Client page architecture。
- Dependencies and consumers: File 3 Tenant page、Step 7 backend exact wire contract。
- Why now: test contract固定后写最少page，无globalstore/new design system。
- Contract/signature changes: add TypeScript transport types；no persistence or frontend authority facts。
- Input/output and state mapping: exact backend fields/versions/status/settings；tenantId path owns scope。
- Error and edge behavior: status transitions confirmation；server validation/permission/conflict mapping；closed tenant
  read-only rules。
- Implementation pseudocode:

```typescript
define exact TenantVO and TenantMembershipVO fields matching backend null, version, status and timestamp rules
define create, update and membership-upsert DTOs with tenantId owned by the path and expectedVersion on mutations
keep types transport-only and do not add RBAC identifiers, credential material, permissive aliases or local persistence fields
```

- Verification contribution: unblocks page test compilation and exact API typecheck。
- After this file: transport types compile；page remains absent until File 3。

#### File 3 —
`CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin-web/src/features/tenants/TenantListPage.tsx`

- Purpose: 新建 single-page tenant catalog/member management UX using existing React Query/Ant Design patterns。
- Symbols: `TenantListPage`、query keys/mutations/drawer/forms。
- Repository evidence: target path is absent at resume baseline；ClientListPage/PageState provide the existing implementation style。
- Dependencies and consumers: Files 1–2 tests/types、httpClient/useAuth/usePermission/PageState；File 4 router/layout。
- Why now: typed contract ready 后创建 page，避免执行器把 absent target 当作 MODIFY。
- Contract/signature changes: page calls API-005–009 only；no RBAC endpoint or new global store/design system。
- Input/output and state mapping: tenant/member VO→table/drawer；forms→DTO；query keys include selected tenantId for members。
- Error and edge behavior: initial/loading/empty/denied/error/retry/conflict；confirmation、focus restore、double-submit prevention。
- Implementation pseudocode:

```typescript
query tenant list under idp:tenant:read, render table and detail drawer, and expose create or update only under idp:tenant:manage
query memberships for the selected tenant, upsert identitySub, status and expectedVersion, then invalidate only that tenant key
render validation, conflict refresh, loading, empty, denied and retry states through existing shared components with accessible focus handling
```

- Verification contribution: TenantListPage test GREEN and page typecheck。
- After this file: tenant/member page works；route/navigation remain for File 4。

#### File 4 —
`MODIFY egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin-web/src/app/router.tsx; egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin-web/src/app/AdminLayout.tsx; egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin-web/src/features/overview/OverviewPage.tsx; egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin-web/src/app/App.test.tsx`

- Purpose: 注册lazy route/navigation/breadcrumb/overview capability与route regression。
- Symbols: `/tenants`、nav item、PATH_LABEL、lazy component、landing tests。
- Repository evidence: existing router/AdminLayout/Overview capability cards。
- Dependencies and consumers: TenantListPage；auth bootstrap permissions。
- Why now: page GREEN后接线，避免broken route中间态。
- Contract/signature changes: newIdP route only；existing routes unchanged。
- Input/output and state mapping: permission list→visible nav/card；path→breadcrumb/page。
- Error and edge behavior: lackingread permission hidesnav/direct route page denied behavior；lazy load error boundary
  preserved。
- Implementation pseudocode:

```typescript
lazy import TenantListPage and register /tenants under the authenticated console guard without changing login or default overview routes
add one navigation and overview capability entry guarded by idp:tenant:read plus a deterministic breadcrumb label
extend App tests for authorized navigation, denied visibility and deep-link rendering while preserving every existing route assertion
keep route/page state local and avoid introducing a new router, provider or tenant store
```

- Verification contribution: full IdP Web route/build regression。
- After this file: IdP Web tenant/member management complete。

- Validation working directory:
  `/Users/mario/SelfProject/Egon-COLA/egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin-web`
- Verification command:
  `npm test -- TenantListPage.test.tsx App.test.tsx && npm run lint && npm run typecheck && npm run build`
- Expected result: exit 0；tenant flows/permissions/routes/typecheck/build通过。
- Failure returns to: File1 behavior；File2 API types；File3 page mapping/state；File4 route/navigation。
- Completion criteria: IdP Web成为tenant/member human management入口。
- Rollback: revert四组frontend paths；backend/data保持，不回到RBAC tenant UI。
- Commit paths: `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin-web/src/features/tenants/TenantListPage.test.tsx`,   `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin-web/src/api/types.ts; egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin-web/src/features/tenants/TenantListPage.tsx`,   `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin-web/src/app/router.tsx; egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin-web/src/app/AdminLayout.tsx; egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin-web/src/features/overview/OverviewPage.tsx; egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin-web/src/app/App.test.tsx`
- Commit: `feat(idp-web): manage tenants and memberships`

### Step 17 — 移除 RBAC tenant 页面并同步权限 seed

- Requirements: `REQ-012`, `REQ-016`, `REQ-019`, `REQ-021`
- Dependencies: Steps 11–12 and 16
- Baseline state: RBAC Web仍注册`/iam/tenants`、tenant.api和target-context page；bootstrap seed含system:tenant:
  read/manage且无idp:tenant permissions。
- Observable outcome: RBAC Web无tenant route/page/API/resource definition；保留authorization TenantContext
  mechanics；bootstrap角色增加IdP tenant permissions并保留existing OAuth client codes。
- End state: RBAC Web test/lint/typecheck/build与topology tests GREEN。
- Test-first gate: Required — route/resource/topology tests先要求removed route不存在/new permission set，当前RED。
- Execution state: `Committed` at `323db68a`；完成后续执行记录如下。
- Execution evidence: File 1 将 landing regression 从退役 `/iam/tenants` 改为稳定
  `/iam/overview`，并增加 route/resource absence 与 exact permission assertions。File 2 物理删除
  RBAC tenant page/test/API；File 3 删除 `/iam/tenants` governance/resource definition，并移除
  browser targetTenantId/header override，同时保留 verified `effectiveTenantId`。File 4 从
  `Rbac3DevelopmentTopology` 与 `JpaPlatformAdminBootstrapRepository` 移除
  `system:tenant:read/manage`、保留 `system:tenant:target`，并加入
  `idp:tenant:read/manage` 与既有 `idp:oauth-client:read/create/update`。
  Maven topology gate、`npm test -- App.integration.test.tsx`（4 tests）、lint、typecheck、
  build 均通过；source static search 仅保留测试中的负向 absence assertions；`git diff --check`
  通过。提交只包含本 Step 声明的 10 个 paths，Plan/Spec 文档保持未暂存。
- Ordered files:

#### File 1 —
`MODIFY egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin-web/src/app/App.integration.test.tsx; egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin/src/test/java/top/egon/cola/platform/rbac3/admin/bootstrap/service/Rbac3DevelopmentTopologyTest.java`

- Purpose: 定义no tenant landing/resource与permission seed exact set。
- Symbols: route resolver assertions；`idp:tenant:read/manage`，existing `idp:oauth-client:*`，remove system tenant
  read/manage but keep target。
- Repository evidence: current tests明确tenant landing和development topology permission list。
- Dependencies and consumers: Files2–4。
- Why now: 先锁定permission/route contract避免误删target authorization capability。
- Contract/signature changes: users no longer land ontenant page；permission seed movescatalog management toIdP codes。
- Input/output and state mapping: bootstrap permissions→IdP Admin nav/API authorization；RBAC target permission remains
  fortrusted platform operations。
- Error and edge behavior: no available route falls backexisting authorized landing/overview；no permission alias or
  silent migration。
- Implementation pseudocode:

```typescript
assert resolveApplicationLanding never returns /iam/tenants and routes/resources contain no rbac3-tenants component key
assert the platform topology retains idp:oauth-client:read/create/update exactly and adds idp:tenant:read plus idp:tenant:manage
assert system:tenant:read and system:tenant:manage are absent because their endpoints are removed while system:tenant:target remains
preserve all unrelated RBAC application, role, permission, audit and target-context authorization assertions
```

- Verification contribution: RED/GREEN `TEST-027/031/033` permission/route slice。
- After this file: tests RED againstcurrent routes/seeds。

#### File 2 —
`DELETE egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin-web/src/features/tenant/TenantListPage.tsx; egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin-web/src/features/tenant/TenantPages.test.tsx; egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin-web/src/features/tenant/tenant.api.ts`

- Purpose: 物理删除 RBAC tenant page、dedicated tests 与 API module。
- Symbols: no TenantListPage/TenantPages.test/tenant.api sources。
- Repository evidence: exact files/search results show all page consumers。
- Dependencies and consumers: File 3 shared route/resource files；app regression and resource reporting scripts/tests。
- Why now: backend routes gone andIdP page available，remove frontend owner withoutredirect/proxy。
- Contract/signature changes: dedicated feature files disappear；route/resource cleanup is performed in File 3。
- Input/output and state mapping: tenantId for frontend authorization pages only comes from the verified login
  `effectiveTenantId`；backend `system:tenant:target` remains an independent trusted API capability, not dead browser
  state。
- Error and edge behavior: deep link falls normal not-found/default handling；no cross-app navigation assumption；resource
  report has no stale route。
- Implementation pseudocode:

```typescript
delete TenantListPage, TenantPages.test and tenant.api after Step 16 makes the IdP page the only catalog management UI
preserve all shared governance route, resource definition and FeatureApi files for explicit modification in File 3
search the deleted feature directory for remaining imports and require any shared consumer to be handled before staging deletion
```

- Verification contribution: dedicated feature source deletion boundary。
- After this file: tenant feature files are absent；shared route/resource/state references remain for File 3。

#### File 3 —
`MODIFY egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin-web/src/features/governance.routes.tsx; egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin-web/src/app/resourceDefinitions.json; egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin-web/src/features/shared/FeatureApi.tsx`

- Purpose: 修改共享 route/resource/API state files，移除 tenant feature imports/declarations 与 browser target state。
- Symbols: no `/iam/tenants`、`iam.tenants`、component key or `targetTenantId/setTargetTenantId`；retain `effectiveTenantId`。
- Repository evidence: three shared targets exist at resume baseline and also serve unrelated governance features，therefore must not be deleted。
- Dependencies and consumers: File 2 deletion、App route resolver/resource report；backend `system:tenant:target` remains independent。
- Why now: dedicated feature files are gone，so shared registries/state can be narrowed without deleting unrelated routes/resources。
- Contract/signature changes: route intentional removal；remaining governance order/parents stay deterministic；browser target override state disappears。
- Input/output and state mapping: authorization pages use verified login `effectiveTenantId` only；no deleted page setter/header injection remains。
- Error and edge behavior: deep link uses existing not-found/default handling；resource JSON remains valid；unrelated governance routes remain identical。
- Implementation pseudocode:

```typescript
remove the tenant import and route from governance.routes while preserving the exact order and guards of every remaining feature
remove only the iam.tenants resource entry from resourceDefinitions and validate all remaining parent references and component keys
remove targetTenantId, setTargetTenantId and header injection from FeatureApi while retaining verified effectiveTenantId for authorization queries
```

- Verification contribution: route/resource tests GREEN and static absence without deleting shared files。
- After this file: RBAC Web has no tenant route/resource/browser override；permission seed remains for File 4。

#### File 4 —
`MODIFY egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin/src/main/java/top/egon/cola/platform/rbac3/admin/bootstrap/domain/Rbac3DevelopmentTopology.java; egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin/src/main/java/top/egon/cola/platform/rbac3/admin/bootstrap/repository/jpa/JpaPlatformAdminBootstrapRepository.java`

- Purpose: 同步development/platform admin permission definitions到新owner/既有Client codes。
- Symbols: permission sets remove read/manage，keep target，add IdP tenant read/manage，retain OAuth client
  read/create/update。
- Repository evidence: both files hardcodeplatform permission sets；controller/Web exact codes由Spec DEC-006。
- Dependencies and consumers: bootstrap Step11、IdP Admin authorization through RBAC snapshots。
- Why now: backend/Web routes和newIdP page均确定后更新授权seed。
- Contract/signature changes: permission fact set only；role/application IDs和bootstrap mechanics不变。
- Input/output and state mapping: topology definitions→permission/role assignments→authorization snapshot；policyVersion
  increments only when facts change。
- Error and edge behavior: rerun idempotent；existing custom roles需migration/operator review inStep18；no auto alias
  fromoldcode。
- Implementation pseudocode:

```java
remove system tenant read/manage permission definitions that existed only for the deleted RBAC catalog APIs
retain system:tenant:target because TenantContextResolver still guards explicit platform target operations
add idp:tenant:read and idp:tenant:manage to the same platform administrator role that already owns IdP administration permissions
preserve idp:oauth-client:read, create and update exactly and rely on idempotent bootstrap fact reconciliation
```

- Verification contribution: topology test GREEN；IdP Web permissions available。
- After this file: RBAC Web/permission cleanup complete。

- Validation working directory: `/Users/mario/SelfProject/Egon-COLA`
- Verification command:
  `./mvnw -B -ntp -pl egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin -am -Dtest=Rbac3DevelopmentTopologyTest -Dsurefire.failIfNoSpecifiedTests=false test && cd egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin-web && npm test -- App.integration.test.tsx && npm run lint && npm run typecheck && npm run build`
- Expected result: both command segments exit 0；no tenant route/resource/page/API；permission set exact；frontend
  lint/typecheck/build通过。
- Failure returns to: File1 contract；File2 dedicated deletion；File3 shared route/resource/state；File4 permission seed。
- Completion criteria: RBAC Web无tenant owner，IdP tenant与existingClient permissions可由平台管理员获得。
- Rollback: revert四组paths only beforebackend/V8 cutover；after cutover old page不可用且不得单独恢复。
- Commit paths: `egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin-web/src/app/App.integration.test.tsx; egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin/src/test/java/top/egon/cola/platform/rbac3/admin/bootstrap/service/Rbac3DevelopmentTopologyTest.java`,   `egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin-web/src/features/tenant/TenantListPage.tsx; egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin-web/src/features/tenant/TenantPages.test.tsx; egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin-web/src/features/tenant/tenant.api.ts; egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin-web/src/features/governance.routes.tsx; egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin-web/src/app/resourceDefinitions.json; egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin-web/src/features/shared/FeatureApi.tsx`,   `egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin/src/main/java/top/egon/cola/platform/rbac3/admin/bootstrap/domain/Rbac3DevelopmentTopology.java; egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin/src/main/java/top/egon/cola/platform/rbac3/admin/bootstrap/repository/jpa/JpaPlatformAdminBootstrapRepository.java`
- Commit: `refactor(rbac3-web): remove tenant catalog administration`

### Step 18 — 交付可校验 cutover 工具、runbook 与最终删除/兼容门禁

- Requirements: `REQ-014`, `REQ-015`, `REQ-017`, `REQ-018`
- Dependencies: Steps 1–17
- Baseline state: code/schema/UI slices完成，但无单一operator artifact/gate/report/runbook聚合两库迁移和发布；legacy
  symbol/config consumer inventory尚未作为release gate自动化。
- Observable outcome: one-shot tool可export/import/verify/report tenant authority
  artifact；runbook定义freeze/backups/V5/import/Secrets/smoke/V8/bootstrap/observe/restore；full static/module/frontend
  gates归零旧路径并保护USER/Signing行为。
- End state: source/document implementation准备好用户review与后续execution；live rehearsal仍由用户控制环境执行。
- Test-first gate: Required — shell contract test因tool/subcommands/artifact/report不存在而RED；full static
  gate会发现任何遗漏旧symbol/config。
- Ordered files:

#### File 1 —
`CREATE scripts/unified-xingyuan/test-tenant-authority-migration.sh; scripts/unified-xingyuan/migrate-tenant-authority.sh`

- Purpose: 提供deterministic offline export/import/gate/report工具与自测试，不成为runtime service/job。
- Symbols: subcommands `export-rbac3`, `import-idp`, `verify-idp`, `verify-rbac`, `report`；versioned JSON/TSV
  artifact+SHA-256。
- Repository evidence: existing `scripts/unified-xingyuan/lib/common.sh`、fixture/test shell conventions；Spec §16.3 exact
  sequence。
- Dependencies and consumers: `psql`、explicit IdP/RBAC DSNs、maintenance operator；V8 gate report。
- Why now: schema/field/failure semantics已稳定后才能写exact SQL/artifact contract。
- Contract/signature changes: newoperator CLI only；credentials从env/secret source且不输出；no DB cross-connection
  inFlyway。
- Input/output and state mapping: ordered RBAC tenants/users→artifact schema/counts/status counts/checksum；artifact→IdP
  upsert；verification→signed/hash report consumedbeforeV8。
- Error and edge behavior: unset/unsafe DSN、non-frozen marker、duplicate/orphan/count/checksum/placeholder/FK mismatch
  abort non-zero；atomic temp files/permissions；rerun import idempotent。
- Implementation pseudocode:

```bash
export-rbac3 reads ordered tenant catalog and identitySub membership rows, writes versioned restrictive-permission artifacts and computes counts plus SHA-256
import-idp verifies schema version and checksum, then upserts tenants/memberships transactionally without changing ids and records deterministic progress
verify-idp and verify-rbac query duplicate, orphan, placeholder, grant and FK invariants and refuse to emit a PASS report on any mismatch
the shell contract test uses fixture psql shims to assert command order, redaction, restartability, non-zero failures and report content without live databases
```

- Verification contribution: migration artifact/reconciliation gate proof；真实数据/locks仍manual。
- After this file: shell test GREEN，operator tool ready。

#### File 2 —
`CREATE docs/runbooks/unified-identity-oauth-client-tenant-cutover.md`

- Purpose: 新建 operator runbook，记录 credentials/config、consumer inventory、发布/观测/rollback phases。
- Symbols: runbook phases/gates/commands。
- Repository evidence: existing unified identity/DDC/RBAC READMEs/runbooks；Spec §16。
- Dependencies and consumers: operators/reviewers；File 3 module README summaries link to this runbook。
- Why now: implementation paths/commands固定后写可执行文档，避免stale symbol。
- Contract/signature changes: runbook 声明 breaking removal、standard config 与 manual/live proof boundary；不含 secrets。
- Input/output and state mapping: each phase input artifact/config/version→expected metrics/queries/result/abort point。
- Error and edge behavior: V5/V8 phase-specific restore；new Secret exposure triggers rotation；post-V8 prohibits
  one-service/one-DB rollback；runtime proof clearly manual。
- Implementation pseudocode:

```text
document preflight inventory, write freeze, readable dual backups and exact tool commands before any migration or binary change
document V5, artifact import/gates, administrator Secret creation/distribution, Token/RPC smoke, DDC caller/admin cutover and V8/bootstrap order
list stable metrics/log dimensions and post-deploy checks for auth failures, token latency, DDC leases, membership RPC and policyVersion
define each safe abort point, coordinated snapshot restore and forward-fix boundary while redacting every Secret/token/header example
```

- Verification contribution: documentation command/link/static review and operator handoff。
- After this file: runbook contract complete；existing module READMEs remain for File 3 update。

#### File 3 —
`MODIFY egon-cola-xingyuan/egon-cola-tianquan-shoubing/README.md; egon-cola-xingyuan/egon-cola-tianshu/README.md; egon-cola-xingyuan/egon-cola-tianquan-jianshen/README.md`

- Purpose: 修改三个既有 module READMEs，记录 AppID/AppKey/Secret/Spring Client、tenant/DDC/RBAC ownership boundaries。
- Symbols: standard biz configuration、IdP tenant authority、DDC direct Token、RBAC authorization-state summaries。
- Repository evidence: 三个 targets 在续执行基线存在；当前仍描述旧 Admission/RBAC tenant ownership semantics。
- Dependencies and consumers: File 2 runbook、biz service developers、module maintainers。
- Why now: exact implementation/runbook commands 固定后更新现有 README，避免误标 CREATE 或复制完整 procedure。
- Contract/signature changes: docs remove private_key/JWK/Ticket/RBAC tenant authority guidance and link to canonical runbook。
- Input/output and state mapping: module configuration/identity facts→documented owning component；不记录真实 credential values。
- Error and edge behavior: examples use placeholders only；legacy keys are deterministic migration errors；live proof remains manual。
- Implementation pseudocode:

```text
update the IdP README with administrator-provisioned appId, client_id and one-time Secret plus Spring OAuth2 Client boundaries
update the DDC README for direct DDC-audience PLATFORM SERVICE tokens and retained anti-replay or lease binding without Ticket terminology
update the RBAC README for external tenantId authorization state only, remove catalog or membership ownership claims and link the runbook
```

- Verification contribution: documentation ownership consistency and old-guidance static deletion。
- After this file: runbook and module READMEs consistently describe the new architecture。

#### File 4 —
`MODIFY scripts/unified-xingyuan/verify-local-stack.sh; scripts/unified-xingyuan/prepare-local-stack.sh; scripts/unified-xingyuan/fixtures/unified-platform-release.json`

- Purpose: 更新本地fixture/verification为Secret/Spring Client、IdP tenant/RPC、DDC Token和RBAC external ID语义，并加入global
  deletion assertions。
- Symbols: env/config keys、smoke probes、symbol/property searches、release fixture versions/permissions。
- Repository evidence: current scripts是统一平台静态/live验证入口，包含旧identity/admission配置。
- Dependencies and consumers: all previousSteps；用户启动本地栈后执行live subset。
- Why now: final aggregation只在所有targets确定后修改，避免中间双轨。
- Contract/signature changes: fixtures使用AppID/AppKey/Secret placeholders from env；bootstrap tenantId；no private
  key/JWK/Ticket fields。
- Input/output and state mapping: env→standard Spring registration/service-client；smoke→Token claims/RPC/DDC
  lease/report；static search→zero count。
- Error and edge behavior: missingnon-secret config/Secret fails before start；scripts不得打印Secret/token；no live
  service auto-start inthis Plan/execution Step validation unlessuser initiates。
- Implementation pseudocode:

```bash
prepare fixture references external tenant ids and standard OAuth2 client registration variables without generating or persisting real secrets
static verification rejects private_key_jwt, Client JWK, Admission RPC/Ticket, RBAC tenant CRUD/member resolve, rbac3UserId and idp:client permission symbols
when the user has started the stack, live verification decodes TENANT/PLATFORM tokens, checks IdP membership RPC and asserts DDC lease identity/expiry behavior
release fixture records IdP/RBAC/DDC compatible versions and exact idp:oauth-client plus idp:tenant permission contracts
```

- Verification contribution: final static and optional user-controlled live gates。
- After this file: release verification reflectsnew architecture；Plan implementation complete。

- Execution state: `Committed` at `772df2b2`; File 1 shell contract first failed because the executable tool was absent,
  then passed with fixture-backed export/import/checksum/report and tamper/freeze negative cases. File 2 added the
  operator cutover/restore runbook; File 3 updated IdP, DDC, and RBAC ownership/configuration boundaries; File 4 added
  `--static-only`, moved the live tenant probe to IdP, and versioned the release fixture with IdP tenant and PLATFORM
  SERVICE-token contracts. The Step 18 commit was amended once to keep all declared paths in the single semantic commit;
  Plan/Spec docs remain unstaged.

- Validation working directory: `/Users/mario/SelfProject/Egon-COLA`
- Verification command:
  `bash scripts/unified-xingyuan/test-tenant-authority-migration.sh && ./mvnw -B -ntp -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing,egon-cola-xingyuan/egon-cola-tianquan-jianshen,egon-cola-xingyuan/egon-cola-tianshu -am test && (cd egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin-web && npm test && npm run lint && npm run typecheck && npm run build) && (cd egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin-web && npm test && npm run lint && npm run typecheck && npm run build) && bash scripts/unified-xingyuan/verify-local-stack.sh --static-only`
- Expected result: all command segments exit 0；migration tool contract、three Maven reactors、two Web suites/builds、static
  deletion/config/permission gates通过；不声称live topology。
- Validation correction: Maven reactor selection uses the three executable Admin module paths (IdP Admin, RBAC3 Admin,
  and DDC Admin) with `-am`; the reliable selector is `-pl :egon-cola-tianquan-shoubing-admin,:egon-cola-tianquan-jianshen-admin,:egon-cola-tianshu-admin`.
- Validation evidence: shell contract, affected-reactor `-DskipTests` compilation, IdP focused OAuth tests (26), DDC
  focused registration/migration tests (34), RBAC topology test (1), both Web full suites/lint/typecheck/builds, static
  verification, `git diff --check`, and strict Plan validation passed. The unfiltered affected-reactor test command was
  also attempted; it stops before the target modules in the unrelated `egon-cola-component-access-guard-starter` tests
  because the local test classpath has no Jakarta Bean Validation provider. This is recorded as an environment-bound
  full-suite gap, not suppressed as a pass; compile and migration-specific gates remain green.
- Failure returns to: File1 artifact/gate；File2 runbook；File3 module docs；File4 config/smoke/static aggregation，或对应owner
  Step 1–17。
- Completion criteria: 21 requirements全trace，V5/V8/one-shot migration/release/rollback/docs/tests完整，legacy
  path归零，USER/Signing regression通过。
- Rollback: four source/doc path groups 可 revert；真实cutover严格按runbook phase restore/forward-fix，V8后整组rollback。
- Commit paths: `scripts/unified-xingyuan/test-tenant-authority-migration.sh; scripts/unified-xingyuan/migrate-tenant-authority.sh`,   `docs/runbooks/unified-identity-oauth-client-tenant-cutover.md; egon-cola-xingyuan/egon-cola-tianquan-shoubing/README.md; egon-cola-xingyuan/egon-cola-tianshu/README.md; egon-cola-xingyuan/egon-cola-tianquan-jianshen/README.md`,   `scripts/unified-xingyuan/verify-local-stack.sh; scripts/unified-xingyuan/prepare-local-stack.sh; scripts/unified-xingyuan/fixtures/unified-platform-release.json`
- Commit: `docs(identity): add oauth tenant cutover gates and runbook`

## 8. Test, Validation, and Quality Gates

| Gate/order               | Working directory               | Command or method                                                                                                                                                                   | Scope                            | Expected result                                                   | Failure returns to     | Requirements/runtime boundary                     |
|--------------------------|---------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|----------------------------------|-------------------------------------------------------------------|------------------------|---------------------------------------------------|
| RED Step 1               | repo root                       | Step 1 selector before V5                                                                                                                                                           | IdP migration                    | fails only for absent V5/schema                                   | Step 1 File 1          | `REQ-002/003/008/011/014/015/018`; Testcontainers |
| GREEN Step 1             | repo root                       | `./mvnw -B -ntp -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=IdpClientTenantV5MigrationTest -Dsurefire.failIfNoSpecifiedTests=false test` | IdP V5                           | exit 0, constraint/checksum assertions pass                       | Step 1 File 2          | migration rehearsal, not production               |
| GREEN Steps 2–4          | repo root                       | each Step exact Maven selector                                                                                                                                                      | IdP core/admin/gateway           | exit 0, Basic/context/Secret/USER regression pass                 | owning file            | module/static                                     |
| GREEN Step 5             | repo root                       | Step 5 exact Maven selector                                                                                                                                                         | idp-starter                      | exit 0, manager/key/autoconfig/legacy failure pass                | Step 5                 | module/static                                     |
| GREEN Steps 6–8          | repo root                       | each Step exact Maven selector                                                                                                                                                      | IdP tenant/RPC                   | exit 0, local authority/API/RPC pass                              | owning Step            | module/Testcontainers where selected              |
| GREEN Steps 9–12         | repo root                       | each Step exact Maven selector                                                                                                                                                      | RBAC V8/state/gate/removal       | exit 0, V8/FK/version/RPC/404 pass                                | owning Step            | module/Testcontainers                             |
| GREEN Steps 13–14        | repo root                       | each Step exact Maven selector                                                                                                                                                      | DDC caller/admin                 | exit 0, Token/lease/replay/source mapping pass                    | owning Step            | module/Redis fakes or component                   |
| IdP frontend             | IdP admin-web                   | `npm test && npm run lint && npm run typecheck && npm run build`                                                                                                                    | Client/Secret/tenant/Resource UI | all scripts exit 0; no persisted Secret/JWK UI                    | Steps 15–16            | component/static bundle                           |
| RBAC frontend            | RBAC admin-web                  | `npm test && npm run lint && npm run typecheck && npm run build`                                                                                                                    | route/resource/permissions       | all scripts exit 0; no tenant page                                | Step 17                | component/static bundle                           |
| Shell contract           | repo root                       | `bash scripts/unified-xingyuan/test-tenant-authority-migration.sh`                                                                                                                  | export/import/gate/report        | exit 0; failure fixtures non-zero/redacted                        | Step 18 File 1         | deterministic tool contract                       |
| Affected reactors        | repo root                       | `./mvnw -B -ntp -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing,egon-cola-xingyuan/egon-cola-tianquan-jianshen,egon-cola-xingyuan/egon-cola-tianshu -am test`  | Java regression                  | exit 0; no unexpected skipped target tests                        | owner Step             | source/module, not live topology                  |
| Affected reactors (corrected selector) | repo root | `./mvnw -B -ntp -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin,egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin,egon-cola-tianshu/egon-cola-tianshu-admin -am test` | Java regression | same scope; valid Maven reactor selection | Step 18 | source/module, not live topology |
| Affected reactors (artifact selector) | repo root | `./mvnw -B -ntp -pl :egon-cola-tianquan-shoubing-admin,:egon-cola-tianquan-jianshen-admin,:egon-cola-tianshu-admin -am test` | Java regression | executable Admin modules and dependencies pass | Step 18 | source/module, not live topology |
| Static deletion          | repo root                       | `bash scripts/unified-xingyuan/verify-local-stack.sh --static-only` plus `rg` inventory from runbook                                                                                | old symbols/config/permissions   | zero current production hits outside immutable history/docs       | Steps 4,5,7,8,12–14,17 | static                                            |
| USER compatibility       | repo root + IdP/Gateway modules | selected Authorization Code/TokenFacade/JWKS/Gateway tests from existing suites                                                                                                     | `REQ-017`                        | exit 0; USER claim/cookie/refresh/signing behavior identical      | Steps 2/4/18           | module/static                                     |
| Live migration rehearsal | user-controlled clone databases | runbook V5/export/import/gates/V8 commands                                                                                                                                          | representative PostgreSQL        | counts/checksums/FKs/recovery proof captured                      | Steps 1,9,18           | live rehearsal, not performed by Plan             |
| Live topology smoke      | user-started stack              | runbook Token/RPC/DDC/Gateway/browser steps                                                                                                                                         | IdP/RBAC/DDC/Redis/Gateway/Web   | decoded claims, membership gate, lease binding, UI flows observed | owning Step            | user-controlled runtime/manual                    |

RED tests must fail because the named contract or behavior is absent, not because Testcontainers/Docker/npm dependencies
are missing. During execution, if a RED command cannot reach the target test, repair the test environment first and do
not count that as behavior evidence. `-Dsurefire.failIfNoSpecifiedTests=false` only prevents `-am` support modules
without matching selectors from failing; it does not suppress target-module test failures.

## 9. Migration, Compatibility, Rollout, and Rollback

### 9.1 Source and migration implementation order

1. Complete Steps 1–8 and publish compatible IdP core/admin/starter/RPC artifacts; no V5 environment deployment occurs
   from intermediate source commits.
2. Complete Steps 9–12 and prove RBAC V8/state/gate/removal against fixtures; no V8 deployment occurs before the Step 18
   external report.
3. Complete Steps 13–17 so DDC caller/admin and both Web bundles form compatible cutover artifacts.
4. Complete Step 18 and archive consumer inventory, migration artifact schema, checksums, rehearsal report, test logs
   and image/binary version set for review.

### 9.2 Maintenance-window deployment sequence

1. Freeze Client/tenant/member/role writes; verify every private_key/JWK/Admission/RBAC membership consumer is
   inventoried; take and test readable IdP/RBAC backups.
2. Stop old IdP, DDC registration callers and SERVICE consumers; apply only IdP V5.
3. Execute `migrate-tenant-authority.sh export-rbac3`, verify artifact checksum/version, import into IdP, then require
   zero placeholder/orphan/duplicate and exact ID/status/settings/membership counts.
4. Start new IdP in maintenance network; provision appId/App Key/one-time Secret, TENANT grants and DDC PLATFORM grant;
   deploy Secrets through the approved Secret Manager; incomplete clients remain DISABLED.
5. Deploy PLATFORM-aware starter/Gateway/resource consumers; verify Basic Token Endpoint, metadata, TENANT/PLATFORM
   claims and RPC-001 without opening business traffic.
6. Deploy DDC caller/admin together; register/heartbeat with registrationToken and verify no Admission traffic plus
   identity-bound lease expiry.
7. Feed the verified external report to RBAC V8, apply V8, deploy RBAC backend/Web, and bootstrap with
   `--tenant-id + --identity-sub`/`tenant-ids` only after RPC ACTIVE validation.
8. Restore traffic/writes; monitor authentication failure/latency, token cache/renewal, DDC lease/source mismatch,
   membership RPC errors and policyVersion propagation through the stabilization window.

### 9.3 Compatibility matrix

| Old/new pair                                    | Supported during cutover           | Rule                                                                                    |
|-------------------------------------------------|------------------------------------|-----------------------------------------------------------------------------------------|
| Old IdP + new Secret consumer                   | No                                 | new consumer needs Basic Token Endpoint; inventory/maintenance order prevents this pair |
| New IdP + old private_key consumer              | No                                 | deterministic `invalid_client`; no dual track                                           |
| New IdP provider + old IdentityDirectory client | Yes                                | existing batch method unchanged; additive membership method ignored                     |
| Old IdP provider + new RBAC membership client   | Only before RBAC writer activation | method unavailable causes fail closed; provider deploys first                           |
| Old DDC caller + new DDC admin or reverse       | No                                 | Ticket and registrationToken contracts are intentionally incompatible; deploy one unit  |
| New IdP V5 + old IdP binary                     | No                                 | V5 drops JWK/TTL; old binary remains stopped                                            |
| New RBAC V8 + old RBAC binary/Web               | No                                 | tenant table/routes removed; deploy one unit                                            |
| Old USER OAuth/Web + new identity backend       | Yes within `REQ-017`               | USER JWT/Cookie/JWKS/Gateway contract unchanged                                         |

### 9.4 Rollback and forward-fix boundaries

- Before V5: cancel window; no data change.
- After V5 but before valid import/Secret distribution: restore IdP snapshot and all old binaries/configs. Never edit V5
  or repair Flyway history.
- After valid import but before V8: either restore IdP snapshot and old unit or retain the additive authority data and
  fix forward; restoring private-key operation still requires the complete old snapshot/binary/config set.
- Inside V8 before commit: database transaction rollback preserves `rbac3_tenant` and original FKs.
- After V8: rollback is coordinated restoration of both IdP/RBAC snapshots plus all old IdP/RBAC/DDC/Gateway
  consumers/config; one-service or one-database rollback is forbidden.
- Any new Secret exposed/distributed during a failed attempt is rotated before the next forward attempt, even if
  snapshots restored old database rows.
- DDC retains legacy physical Redis/SQL field names, enabling compatible data reading during whole-unit rollback; this
  does not authorize mixed caller/admin versions.

## 10. Requirement-to-Step Traceability Matrix

| Requirement | Effective Spec section | Steps                 | Files/change units                           | Tests/gates                            | Completion evidence                          |
|-------------|------------------------|-----------------------|----------------------------------------------|----------------------------------------|----------------------------------------------|
| `REQ-001`   | §4/API-001             | 3,15                  | Secret admin backend/Web                     | OAuth service/controller + Client page | admin-only preprovision/no self-registration |
| `REQ-002`   | §4/§10/§11             | 1,3,4,15              | V5/Client/app claim/Web                      | migration, service, Token, component   | AppID/Key/Secret mapping identical           |
| `REQ-003`   | §4/API-001/002         | 1,3,15                | Secret table/service/one-time UI             | hash/rollback/no-leak/component        | plaintext once, hash-only persistence        |
| `REQ-004`   | §4/API-003/004         | 4,5                   | Basic authenticator/metadata/converter       | OAuth/starter tests                    | Basic only, old credential rejected          |
| `REQ-005`   | §4/INTERNAL-001        | 5,13                  | starter facade/DDC runtime                   | facade/autoconfig/runtime              | standard manager path                        |
| `REQ-006`   | §4/INTERNAL-001        | 5                     | request/key/single-flight                    | concurrency/key matrix                 | no cross-context Token reuse                 |
| `REQ-007`   | §4/SERVICE claims      | 2,4,14                | core/signer/verifier/DDC identity            | claim/verifier tests                   | all required claims enforced                 |
| `REQ-008`   | §4/context rules       | 1,2,4                 | V5 Grant/core policy/Token                   | context/tid matrix                     | server-derived TENANT/PLATFORM               |
| `REQ-009`   | §4/INTERNAL-002        | 13,14                 | DDC caller/verifier                          | runtime/component                      | direct DDC SERVICE Token, no Ticket          |
| `REQ-010`   | §4/lease               | 13,14                 | request/verified identity/repos/lease        | replay/concurrency/expiry              | exact identity binding and lease cap         |
| `REQ-011`   | §4/tenant authority    | 1,6,7,8,16            | V5/service/API/local port/RPC/Web            | tenant/backend/frontend                | IdP sole authority                           |
| `REQ-012`   | §4/RBAC state          | 9,10,11,12,17         | V8/state/consumers/removal/Web               | migration/repository/404/Web           | no RBAC catalog/member owner                 |
| `REQ-013`   | §4/RPC-001             | 8,11                  | RPC provider/RBAC adapter/writers            | RPC/user/bootstrap                     | inactive/unavailable zero-write              |
| `REQ-014`   | §4/§16                 | 1,6,9,18              | V5/V8/tool/runbook                           | migration IT/shell/live rehearsal      | exact data gate before V8                    |
| `REQ-015`   | §4/§11                 | 1,9,18                | only V5/V8/static checks                     | checksum/migration gates               | immutable history, no DDC migration          |
| `REQ-016`   | §4/§12                 | 3,7,15,16,17          | backend/Web routes/states                    | controller/component/build             | secure Client/tenant UI, no RBAC tenant page |
| `REQ-017`   | §4/unchanged boundary  | 2,4,10,18             | USER core/signer/Gateway regression          | existing USER/JWKS/Gateway suites      | USER/session/signing behavior unchanged      |
| `REQ-018`   | §4/§16                 | 1,4,5,7,8,12,13,14,18 | all legacy deletion/config/tool gates        | negative routes/static/full reactor    | no silent dual track                         |
| `REQ-019`   | §4/`DEC-006`           | 3,11,15,17            | controller/Web/permission seeds              | controller/component/topology/static   | exact existing Client permissions            |
| `REQ-020`   | §4/`DEC-007`           | 2,6,7,12              | reduced port/local model/old policy deletion | core/local/static                      | no rbac3UserId or stale USER policy          |
| `REQ-021`   | §4/`DEC-008`           | 8,9,11,17             | RPC/state/bootstrap/permissions              | bootstrap/zero-write/topology          | external IDs only, no tenantCode/TenantPO    |

## 11. Risks, Blockers, and User Decisions

| ID         | Risk or decision                              | Impacted Steps/files | Evidence                           | Owner               | Status/action                                                                                                    |
|------------|-----------------------------------------------|----------------------|------------------------------------|---------------------|------------------------------------------------------------------------------------------------------------------|
| `RISK-001` | V5/V8 destructive cutover与旧binary不兼容          | 1,4,9,12,18          | Spec §16 compatibility matrix      | Release owner       | Closed by maintenance unit, backups and no mixed-version deployment                                              |
| `RISK-002` | Argon2 Token Endpoint capacity未在source test证明 | 3–5                  | Spec §15                           | Security/operations | Accepted runtime gate: benchmark target hardware before traffic                                                  |
| `RISK-003` | 外部仓库/脚本可能仍调用JWK/Admission/tenant APIs         | 4,5,8,12–14,18       | static search只覆盖当前repo             | Release owner       | Closed for repo; external consumer inventory is mandatory pre-V5 gate                                            |
| `RISK-004` | RPC additive method registry可能要求version bump  | 8,11                 | `PLAN-CLAR-001`                    | RPC maintainer      | Closed for Plan by existing additive compatibility; execution escalates only if registry validation disproves it |
| `RISK-005` | 跨库真实数据包含duplicate/orphan/非数字tenantId          | 1,9,18               | no live DB inspection              | Data owner          | Closed as release blocker: tool must fail before V8; no inferred remediation                                     |
| `RISK-006` | DDC physical admission字段形成短期命名债               | 14                   | Spec `ASM-004`                     | DDC maintainer      | Accepted; explicit Java mapping, no schema migration in this scope                                               |
| `RISK-007` | immediate Secret rotation需要consumer配置原子更新     | 3,15,18              | Spec `ASM-002`                     | Service owner       | Accepted approved behavior; maintenance distribution and repeat rotation mitigate                                |
| `RISK-008` | Plan基于Review Spec，尚未获得Accepted/Ready门禁        | all                  | header/user planning authorization | User                | Closed for drafting only; implementation waits for explicit Spec/Plan acceptance                                 |

没有未解决的重大架构决策。实施前仍需用户显式接受主规格与本Plan；这是生命周期门禁，不是缺失设计。

## 12. Review and Acceptance

### 12.1 Original requirement fidelity

Plan覆盖AppID/App Key/Secret管理员预配、hash-only/one-time、Spring Security OAuth2 Client、无应用自注册、IdP tenant
authority、RBAC authorization-only、DDC direct SERVICE Token和用户确认的权限/port/bootstrap `1A/2A/3A`。没有恢复Admission
Ticket、private_key_jwt、Client JWK、RBAC tenant catalog或长期双轨，也没有把per-Pod identity、mTLS/SPIFFE或在线跨库事务扩入范围。

### 12.2 Spec consistency

18 Steps只实现主规格已批准元素。Simplicity audit拒绝了membership HTTP fetch、Admission第二凭证、runtime migration
service/job、auth Strategy/factory、shadow tenant catalog和frontend global tenant store。Plan Clarifications仅涉及RPC
additive version、内部路径、Boot dependency management、Java rename/physical mapping和one-shot tool
placement，不改变公开行为、schema语义或安全边界。

### 12.3 Repository executability

当前branch/commit、dirty worktree、实际modules/POMs/package
scripts、migrations、controllers/services/repositories/proto/pages/tests与消费者均已核对。每Step给出baseline/end state、RED
reason、ordered file sets、implementation-bearing Java/SQL/TypeScript/shell pseudocode、exact CWD/command、failure
return、rollback、commit paths和semantic commit。执行时必须重新核对HEAD/drift，并保护届时存在的任何无关dirty paths。

### 12.4 Test and release completeness

计划包含focused RED/GREEN、IdP/RBAC/DDC affected reactors、protobuf generation、V5/V8 Testcontainers rehearsal、两套frontend
test/lint/typecheck/build、shell artifact contract、legacy deletion/config gate、USER/JWKS/Gateway regression、maintenance
rollout与phase-specific rollback。静态/模块证据不等于真实PostgreSQL数据、Redis/DDC/Gateway topology、Secret Manager、browser
accessibility或恢复时间证明；这些明确保留为用户控制的release rehearsal/manual gates。

### 12.5 Final verdict

PASS — Ready for user review
