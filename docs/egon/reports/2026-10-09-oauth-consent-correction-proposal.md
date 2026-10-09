# OAuth 原生同意链补救结果与 USER Grant 后续提案

状态：**C9b Verified，按 Step 19 单一提交交付；C10 Approved / In Progress**。用户2026-10-09明确批准 C9b。最终范围为33个明确文件，原生回合、354个Admin测试、24个Edge测试、58个消费者测试及静态门禁通过。以下原始阻塞和候选22项保留为决策来由；实际执行以Plan Step19 File1–33为准。未启动应用或执行数据库。

## 原始阻塞（C9b 已修复）

1. native SAS请求用户同意时，先保存一个没有已签发token的OAuth2Authorization。当前 `SasAuthorizationConverter.toSource` 仅从token推导absoluteExpiresAt，因此抛 `OAUTH_AUTHORIZATION_EXPIRY_REQUIRED`。
2. `TenantAuthorizationServiceImpl.findByToken` 只能找四种已签发token；`toTokenType("state")` 返回null。native SAS不能按待同意state恢复该交易。
3. `SasAuthorizationConverter.toTarget` 重建授权请求时没有恢复原客户端state；服务端consent state也没有恢复。原属性只存SHA摘要，不能反推出原值。
4. 当前 `.csrf(...ignoringRequestMatchers(endpoints))` 覆盖同意POST；SAS1.5.8默认同意页面分支调用DefaultConsentPage时没有附加CSRF表单参数。主Spec API-005明确要求 `_csrf`。
5. 同任务新建的 `TransactionalOAuthAuthenticationProvider` 仍有手写构造器且缺少 mandated Slf4j/Lombok/Qualifier；最终规则门禁不能概括为通过。另一个 `TransactionalOAuthRevocationAuthenticationProvider` 已有这些规范，本次复核将它从候选修改清单移除。

2026-10-09通过已编译实际Mapper/Service类型做了不启动Spring应用的内存复现；输出只有三个固定结论，临时Java源码与class已删除：

```text
REPRODUCED: native pending consent cannot be mapped without an issued token expiry
REPRODUCED: reconstructed authorization request loses original client state and server consent state
REPRODUCED: SAS state token lookup has no supported internal token type
```

证据文件 `/tmp/egon-consent-audit-evidence.log`，不存在数据库/Redis/网络调用。CSRF和Provider规范结论来自当前源码及已安装SAS1.5.8字节码。

## 已批准的 C9b 决策

批准将主Spec“**不存原password/state**”明确为“**绝不持久化原password；state仅可在既有AES-GCM加密载荷中保存，普通JSON、日志和报告均不暴露**”。state仅存摘要无法满足SAS的原生同意回合与客户端回调恢复。

批准新增内部`CONSENT_STATE` token类型、受控授权载荷schemaVersion2和一个受管DDL修正版本。继续读旧v1；旧实例遇到v2必须fail-closed。未知schema、错issuer/AAD、坏tag、过期/重放/跨客户端state均拒绝。待同意state不生成JWT、不进入业务认证、不算授权完成；只有同意事务成功才产生code及已授scope。

批准新增直接AS同意页面 `GET /tenants/{tenantId}/consent`（API-046），复用现有SAS/session/CSRF与原登录HTML风格；不新增客户端授权协议、不迁RBAC业务、不引入依赖或另一个状态库。SAS默认consent URI是固定值；采用现有exact-issuer过滤边界中的小型redirect Adapter，只重写框架内部consent目标为受信issuer页面，必须通过Host伪造/正常callback不被改写测试。

数据库只增加**一个**版本 `V20261009_001__support_oauth_consent_state.sql`，其逻辑变更为扩展现有token_type CHECK。当前最新应用版本20261008_001；原SQL、原manifest项、Flyway V1–V6及业务数据保持不变。新脚本SHA仅在实际文件确定后计算。普通启动由现有MP-SDJ管理新版本；本任务仍只编写，不启动或执行数据库。

不推荐关闭USER consent、把state伪装为CODE或默默退回旧Cookie/密码协议；它们分别违反同意、安全类型或退役合同。独立Redis pending-store会扩大权威/事务边界，优先复用现有PG token事实及cipher。

## 状态/失败与兼容

- Pending：绑定issuer、client、当前SAS subject/session、请求scope/audience、PKCE/nonce、原客户端state和短期state期限；已有family绝对期限单独保持。
- Approve：同库事务内消费state、保存最小Consent并生成code；code的TTL从生成时起算，USER AT必须300–600s，confidential RT遵循既有绝对family期限。
- Deny/expired/replayed：不生成code、AT/ID/RT，不扩大scope；state失效/标记与transaction guard一致。
- CSRF缺失/不匹配、跨issuer/client/session、未知state：按新Spec固定错误，无token/cookie泄漏；依赖失败明确503，不能当作已登录。
- RT、OIDC ID Token、userinfo也必须由真实SAS filter接线回合测试证明；typed AT约束不能错误地套在ID Token上。
- 补齐受影响Provider规范；旧Schema读取/缺失默认字段兼容仅按已确认字段，禁止弱化所有DTO或忽略未知状态。
- 旧二进制不能读取新state/schema；出现新事实后禁止回滚旧库/安全世代。上线仍需单独用户控制PG/Redis/TLS/浏览器证据。

## 原始候选路径（22项，历史提案）

范围已按当前树核实，新的业务逻辑仍在既有Admin层。审核决策后先产出严格有效、可执行的逐文件Plan；若roundtrip暴露必需额外路径，必须再明确列出，不能在此提案中默默扩大。

| 操作 | 路径 | 必需职责 |
| --- | --- | --- |
| CREATE | `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/OAuthConsentRoundTripTest.java` | 真实MockMvc/SAS授权→同意→code→AT/ID/RT→userinfo/refresh，不以Service直调用替代生产接线；跨realm、state/CSRF错误、重复提交、超时/失败/回滚 |
| MODIFY | `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/repository/OAuthAuthorizationPersistenceContractTest.java` | 待同意STATE密文、expiry、客户端state/请求scope/principal重建和新的token类型正负例 |
| MODIFY | `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/TenantIssuerIsolationTest.java` | 真实同意页面、Spring CSRF、两issuer同client/state隔离、配置/错误/Host/metadata |
| MODIFY | `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/tianquan/shoubing/admin/openapi/IdpPublishedOpenApiContractTest.java` | 新同意页面仅direct AS provider，管理catalog既有断言保留 |
| MODIFY | `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/tianquan/shoubing/admin/support/runtime/SpringConstructorResolutionTest.java` | 新Controller及事务装配Qualifier/Bean验证 |
| MODIFY | `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/config/OAuthOidcProtocolContractTest.java` | 同意与code/refresh一次性CAS、JWT/IDOIDC claim类型分组、期限/标准错误 |
| CREATE | `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/domain/bo/OAuthPendingConsentBO.java` | 闭合typed schema，pending请求scope/客户端state/服务端state关联；敏感字段不出普通JSON/toString |
| MODIFY | `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/domain/bo/OAuthAuthorizationAttributesBO.java` | 仅授权载荷允许受控schemaVersion2，可读旧v1；不得引入任意Object或默认多态 |
| MODIFY | `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/domain/enums/OAuthTokenTypeEnum.java` | 明确新增CONSENT_STATE持久化值；@EnumValue/@JsonValue，不伪装成CODE |
| MODIFY | `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/domain/bo/SignedTokenClaimsBO.java` | 区分AT与ID Token的必填约束，保留OIDC nonce；AT仍严格验证所有安全字段 |
| MODIFY | `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/domain/converter/OAuthAuthorizationConverter.java` | 现有MapStruct/BaseConverter加密payload codec，state只在AES-GCM载荷内；稳定PK/issuer/realm/purpose AAD；未知schema拒绝 |
| MODIFY | `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/domain/converter/SasAuthorizationConverter.java` | 支持native pending授权，无token时明确初始化期限；state、原请求scope和受信principal/authTime/acr重建；state不作为业务AT |
| MODIFY | `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/service/impl/TenantAuthorizationServiceImpl.java` | state按digest和当前realm查找；owner/session绑定；拒绝/过期/已消费严格拒绝；只读token lookup不可跨realm |
| MODIFY | `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/service/impl/TransactionalOAuthAuthenticationProvider.java` | 复用既有Provider/事务Decorator，request/consent与ConsentRepository及Authorization/STATE/code保存同一TM；完整Slf4j/Lombok/Qualifier规范 |
| MODIFY | `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/service/impl/OAuthTokenIssueServiceImpl.java` | 检查pending不可提前签发、family绝对期限与实际native provider接线；仅根据完整roundtrip失败修必需路径 |
| CREATE | `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/controller/OAuthConsentController.java` | API-046 GET /tenants/{tenantId}/consent；SAS session、真实待同意交易、CSRF/转义/默认不勾选、独立取消form；沿用现有AS登录HTML风格 |
| MODIFY | `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/config/AuthorizationServerConfiguration.java` | 只保护consent POST的CSRF、配置原生SAS consent hook与同库事务；精确issuer页面/redirect Adapter；不复制协议栈 |
| MODIFY | `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/config/OAuthProtocolOpenApiConfiguration.java` | API-046与真实CSRF/error/filter一致；不声明未实现的同步成功 |
| CREATE | `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/resources/db/egon-mp/V20261009_001__support_oauth_consent_state.sql` | 唯一新受管ALTER版本，扩展oauth_token token_type CHECK允许CONSENT_STATE；保留现有jti约束及数据；不改任何旧SQL |
| MODIFY | `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/resources/db/egon-mp/repository-manifest.json` | 同tianquan-oauth family追加新版本与实际SHA-256，原脚本记录保持不变 |
| MODIFY | `docs/egon/spec/2026-10-08-10-18-oauth-current-code-alignment.md` | 明确定义加密state与v2内部载荷/API-046/STATE生命周期/事务/验收；旧密码/明文state存储仍禁止 |
| MODIFY | `docs/egon/plan/2026-09-23-10-42-tianquan-oauth-oidc-implementation.md` | 锁定补救有序路径/RED-GREEN/每条MC与Rule、执行顺序及单次范围提交 |

## 原始执行建议（历史提案）

当前C8c wire/docs补丁10路径尚未提交：实际签名SERVICE scope/string、存储映射和Edge无效callback错误已有可运行修正，Admin47 focused tests和Edge24 tests均通过；由于API-005实际同意合同未闭合，不能将整个协议门禁标成Verified或最终PASS。

若批准，推荐先把本提案写入有效Spec/严格Plan，并明确将C8c剩余协议闭合与C9b作为一个新的原生授权补救任务完成，保留既有18个Step/纠正提交，不amend/squash。候选22项与当前C8c10项取并集后再次锁定精确scope，完成全部RED/GREEN与17MC/10Rule，再仅作一个聚焦提交。

先运行真实SAS+内存repo/受控transaction故障fixture的授权同意全回合（不启动应用/PG/Redis），证明pending能保存/读取、原state回调一致、CSRF必要且页面能正常提交、取消/重复/跨租户拒绝、JWT/ID/refresh/UserInfo正确。随后运行所有Admin默认*Test与三安全WebMvc IT的精确类列表、Starter多issuer/Edge/RPC/MCP受影响门禁、strict文档与静态脚本检查。不得重跑未经外部fixture门控的全reactor。

源码/MockMvc通过后仍是运行验收未完成；真实DB事务/CHECK/断连、Redis故障恢复、多实例、TLS、浏览器/同意页键盘与布局由用户另行授权测试。

## 批准前复核（历史记录）

当前HEAD仍为 `4d9104f2817e051cacc2bbd99e24df4fdab88182`，补救未实施。复查确认撤销Provider已经满足Slf4j/Lombok/Qualifier，本提案只修确有缺口的AuthenticationProvider，因此候选范围由23收敛为22。与现有C8c十路径取并集后为26个聚焦路径；正式执行前仍须将每一项纳入严格Plan。自动目标续跑不代表新增state/schema合同已获审核批准。

## 明确批准与实施锁定

用户2026-10-09已批准继续执行。原22候选与C8c10取并集26；同一原生登录Filter规范修正1和两个交付报告2明确纳入，共29路径。Schema v2/state/一个新DDL/native consent page合同生效，旧SQL与manifest旧项仍不可修改；已核实AUTHORIZATION_CODE18chars在新同一迁移中兼容扩宽varchar32。实施前strict Spec/Plan检查，随后native测试RED/GREEN，单一聚焦纠正提交。

## C9b 最终实现与验证

Step19在既有SAS Provider外复用事务Decorator和GrantTransaction Strategy，并以Adapter恢复受信issuer页面路径；待同意、已同意和已取消事实继续使用现有版本状态和MP存储。没有新增依赖或状态服务。

- 授权载荷v2以既有AES-GCM保存客户端/server state和会话绑定；兼容v1，未知版本拒绝；普通JSON/toString隐藏state。
- API-046直接同意页与真实CSRF、精确scope选择、取消/过期/重放隔离闭合；原登录与同subject密码再认证恢复请求，签发时间与PASSWORD/STRONG ACR来自实际认证事件。
- 原生Code/S256 → RS256 USER AT/nonce ID → UserInfo → RT轮换已通过真实Filter/Service/生成Converter/加密JWK-source测试；事务替身控制故障回滚，不外推真实PG。
- 当前Resource版本与显式业务scope是签发上界；空USER Grant只允许经过client/consent边界的标准OIDC身份scope，不允许任何业务scope。刷新缩减不能从父授权恢复旧scope。
- 唯一新版本V20261009_001扩宽token_type至varchar32并扩展CHECK；95个原SQL字节和旧manifest项未变。新版本仅编写，未执行。

最终证据：`/tmp/egon-c9b-all-admin-green3.log`：68类/354测试；`/tmp/egon-c9b-final-edge.log`：24测试；`/tmp/egon-c9b-consumers.log`：33 Starter +17 Gateway +3 RPC +2 cancellation +3 MCP =58测试。均exit0，0失败/错误/跳过；static/strict/diff/旧SQL核验通过。对应精确命令和33路径见执行审计。

## C10 已实现纠正：显式 USER 业务 Grant

**B-04 / Closed（Step20；以下为纠正前证据）。** 主Spec §10/11 oauth_grant明确USER也必须受显式allowed_scopes限制。当前Core `ClientResourceGrant`构造器禁止非空USER scope；Admin `ResourceServerServiceImpl#validateClientGrantContext`拒绝非空USER scope，upsert持久化时强制写空；`ClientResourceGrantPage`只显示SERVICE scope输入且USER提交强制空；`OAuthMigrationServiceImpl#importGrantsForClient`对已审核USER scopes再次写空。这是实现缺口，不能归类为仅未运行。C9b签发保护已阻止扩权，但通过当前管理/导入入口无法建立合法人员业务授权。

推荐保留现有API-028/029/030/045和realm/乐观版本/原权限入口：Core允许有界、明确的USER BUSINESS scopes，继续禁止USER tenantId/scopeContext；Admin按当前client允许scope、原Resource/version边界验证并保存实际选择。UI让USER输入/展示同一显式业务scope集合。迁移只保存已审核assignment中的准确scopes，并在恢复比对时校验准确集合。既有空Grant保持拒绝业务权限，不猜测或补写全量scope；client Resource自动关联不得自动增加业务权利。标准OIDC身份scope继续由原client/consent校验。

这一变化沿现有Service/Repository和不可变Core value object，不引入新Strategy/Registry；局部集合校验直接实现更清楚。保留C9b既有事务Strategy和版本约束。**不新增DDL、不改旧SQL、不批量回填数据、不运行迁移**。

建议精确代码/测试范围（全部已在当前树定位；文档仍更新现有有效Spec/Plan/本报告/审计）：

| 操作 | 文件 | 责任 |
| --- | --- | --- |
| MODIFY | `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-core/src/main/java/top/egon/cola/platform/tianquan/shoubing/core/resource/ClientResourceGrant.java` | 只移除USER非空scope禁令，保留准确格式/数量和tenant/context互斥 |
| MODIFY | `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-core/src/test/java/top/egon/cola/platform/tianquan/shoubing/core/resource/ResourceServerPolicyTest.java` | 显式USER scope正例，空集合拒绝业务权限及租户/context负例 |
| MODIFY | `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/resource/domain/dto/UpsertClientResourceGrantDTO.java` | 复用Jakarta集合/元素边界，保持builder及现有接口字段 |
| MODIFY | `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/resource/service/impl/ResourceServerServiceImpl.java` | USER scope验证/原样保存，版本/原权限/原禁用边界保持 |
| MODIFY | `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/tianquan/shoubing/admin/resource/service/impl/ResourceServerServiceImplTest.java` | 管理保存/回读精确scope、错版本/越界/跨realm拒绝 |
| MODIFY | `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/tianquan/shoubing/admin/resource/controller/ClientResourceGrantControllerTest.java` | API-028/030明确USER scope与输入错误、API-045不回空集 |
| MODIFY | `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/service/impl/OAuthClientServiceImplTest.java` | 证明自动Resource关联不会隐式授予业务scope；不改生产创建行为 |
| MODIFY | `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/service/impl/OAuthMigrationServiceImpl.java` | 保存审核通过的USER scopes，恢复比对真实已审核集合 |
| MODIFY | `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/tianquan/shoubing/admin/support/migration/OAuthMigrationServiceTest.java` | USER审核scopes不丢失、空/扩大/已存冲突与重放负例 |
| MODIFY | `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/OAuthConsentRoundTripTest.java` | 管理后持有的明确Grant通过原生签发；收窄/禁用/错Resource版本拒绝 |
| MODIFY | `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin-web/src/features/resource-grants/ClientResourceGrantPage.tsx` | USER编辑/提交/展示准确scope，保留realm缓存和错误状态 |
| MODIFY | `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin-web/src/features/resource-grants/ClientResourceGrantPage.test.tsx` | USER非空scope实际请求与错误/缓存合同 |

批准后先在现有有效Spec/Plan形成一个精确Step20并通过strict，再执行测试RED→最小实现→GREEN→一个聚焦提交；前一步Step19提交必须先验证。门禁为Core ResourceServerPolicyTest、上述受影响Admin测试、全部安全Admin精确列表、前端Grant页面测试/typecheck及C9b原生回合。真实PG/Redis/TLS/浏览器仍由用户控制。该具体范围已由用户明确批准。

用户已明确批准C10。Step20原16路径加三个直接必要的构造/页面测试兼容路径，共19路径，已先声明并strict验证。五个生产路径完整传递明确USER BUSINESS scope，空集合拒绝业务，既有SERVICE/realm/版本/权限合同保留；新Client同事务创建的空关联仅初始化为审核集合，既有空/冲突Grant仍拒绝扩权。实际管理→原生签发、清空/禁用不消费code、迁移四种源/初始关联组合与恢复冲突均通过。最终364 Admin +29 Core、前端50/Grant11、本地共享源码typecheck及strict/旧SQL/原dirty边界通过；原安装包typecheck失败和真实运行未验单列执行审计。立即按Step20完成一个聚焦提交，无DDL/依赖/运行操作。
