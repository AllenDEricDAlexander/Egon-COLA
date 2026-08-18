# RBAC3 注解权限、全局资源目录与无状态认证实施计划

| Field | Value |
| --- | --- |
| Document | `2026-08-17-15-07-rbac3-annotation-resource-catalog-implementation.md` |
| Status | `Ready` |
| Created | `2026-08-17 15:07 CST` |
| Updated | `2026-08-18 09:39 CST` |
| Owner | `Mario / Egon-COLA` |
| Repository | `/Users/mario/SelfProject/Egon-COLA` |
| Scope | `Gateway/IdP USER在线态、RBAC3 Contract/Starter/Admin、DDC BIZ/APP唯一性、全局资源目录/TenantApplication、React SDK/Admin Web/Shared、CI资源上报` |
| Source Requirement | `2026-08-17用户确认并要求开始Plan；最新约束为bizCode/appCode分别全局唯一、资源目录及上报无tenant、CI上线前SERVICE上报、不设1MiB限制和业务限流` |
| Baseline Revision | `main@3ba0607d4491091611f8c213c4597c33d89e6dbf；a2cde2749a9b之后仅coding skill文档/校验器变化，目标业务源码无变化；目标Spec/Plan未跟踪（2026-08-18 09:39 CST）` |
| Implements Spec | [RBAC3 注解化权限、UserDetails、字段权限与 RT 在线态改造规格](../spec/2026-08-17-09-37-rbac3-annotation-userdetails-field-authorization.md) |
| Spec Status | `Accepted` |
| Spec Revision | `Updated 2026-08-17 15:07 CST；baseline main@a2cde2749a9b` |
| Effective Specs | [RBAC3 注解化权限、UserDetails、字段权限与 RT 在线态改造规格](../spec/2026-08-17-09-37-rbac3-annotation-userdetails-field-authorization.md) |
| Depends On Plans | `None` |
| Supersedes | `None` |
| Superseded By | `None` |
| Related Plans | `None` |

## 1. Summary

本计划只实施目标Spec已确认的范围，拆成12个按依赖顺序、各自可验证且各提交一次的Step。顺序为：先收紧DDC全局code和落Gateway/IdP RT在线态，再发布IdP只读身份RPC；随后建立RBAC UserDetails、方法注解和字段序列化；再用唯一RBAC V7把资源目录全局化并新增TenantApplication；在此数据库基线之上落CI报告、全局资源/字段CRUD和Manifest删除；最后切About/common HTTP契约、React SDK、本地递归资源、Shared导航和Admin Web页面/CI脚本。

完成证据包括每步RED/GREEN测试、DDC双数据库迁移测试、RBAC PostgreSQL V7测试、Gateway/IdP/RBAC聚焦Maven回归、React SDK/Shared/Admin Web test/typecheck/build、dist秘密/报告代码守卫、Spec/Plan strict校验和path-limited commit记录。本计划本身不修改生产代码、不执行迁移、不启动服务；真实Redis/PostgreSQL/DDC/JWK/流水线闭环由用户在实现完成后启动验证。

## 2. Target Spec and Effective Design

### 2.1 Primary target

- Path：[RBAC3 注解化权限、UserDetails、字段权限与 RT 在线态改造规格](../spec/2026-08-17-09-37-rbac3-annotation-userdetails-field-authorization.md)
- Status：元数据为`Accepted`；用户已明确给出最新资源目录/CI上报/唯一性决定，并于2026-08-18显式授权执行本Plan。
- Revision：`Updated 2026-08-17 15:07 CST`，源码基线`main@a2cde2749a9b`。
- Approval evidence：用户已审核方案、补充并确认`bizCode/appCode`单列唯一、无tenant报告、CI上线前维护、无1MiB限制和无限流；2026-08-18明确调用`egon-coding-executing-plan`并要求开始执行，因此状态为`Ready`。

### 2.2 Effective Spec set

| Role | Spec/link | Status/revision | Effective sections | Why included |
| --- | --- | --- | --- | --- |
| Primary | [RBAC3 annotation/UserDetails/global catalog Spec](../spec/2026-08-17-09-37-rbac3-annotation-userdetails-field-authorization.md) | `Accepted`；2026-08-17 15:07 CST | §1–§20全部 | 唯一直接规定认证链、注解、字段、数据库、接口、前端、CI和发布顺序的设计基线 |

### 2.3 Superseded or excluded content

- Primary Spec的`Amends`关系已排除旧Session、第三Token、Gateway动态URL最终授权、Bootstrap资源树和Manifest提交/激活生命周期；Plan不重新引入。
- 前置Spec仍有效的AT/RT claims、IdP密钥、active-role显式激活和DDC主数据所有权继续生效；本Plan只按Primary Spec增加TenantApplication资格。
- `@DataScope`/SQL改写、请求字段写权限、账号冻结、用户中台、Manifest Processor/Registration Starter/Node Plugin、浏览器上报、启动时上报、1MiB特设限制和业务限流全部排除。

## 3. Effective Requirements and Acceptance

| Requirement | Source Spec section | Effective statement | Observable acceptance | Implementation impact |
| --- | --- | --- | --- | --- |
| `REQ-001` | §4 | Gateway每个保护USER请求在线确认RT | valid AT+revoked/missing RT为401并清Cookie | Gateway core/engine、IdP core/admin/adapter |
| `REQ-002` | §4 | 保持AT缺失/过期时的RT刷新状态机 | active RT刷新一次并继续原请求；非过期非法AT不刷新 | Gateway/IdP adapter |
| `REQ-003` | §4 | public与SERVICE链不错误要求USER RT | 登录/刷新/JWKS等精确开放；SERVICE不读USER RT | endpoint policy/security tests |
| `REQ-004` | §4 | `@RBACAPIResource`与`@RequiresPermission`方案A | API注解与通用注解AND、统一403 | RBAC Starter method security |
| `REQ-005` | §4 | 保留Security Chain和AuthorizationService | Filter顺序不变；Method Security调用现有决定服务 | Starter auto-config/tests |
| `REQ-006` | §4 | principal迁移`Rbac3UserDetails`并复用Snapshot Redis | SecurityContext principal类型/内容正确，不缓存AT身份 | Contract/Starter/cache |
| `REQ-007` | §4 | 只有有效、显式激活且具备BIZ/APP资格的角色进入上下文 | inactive/expired/no TenantApplication角色及权限缺失 | Admin role/snapshot/activation |
| `REQ-008` | §4 | USER Controller不显式注入principal | IdP/RBAC USER controller参数无`@AuthenticationPrincipal`；SERVICE例外保留 | CurrentIdentity/CurrentRbac3User、controllers/services |
| `REQ-009` | §4 | FIELD只在响应序列化和前端展示执行 | NONE/null、MASKED/脱敏、READ/原值；请求不变 | Jackson/SDK guards |
| `REQ-010` | §4 | FieldDefinition/FieldRule完整CRUD | common接口和Admin页面可维护，CI不覆盖安全属性 | RBAC Admin/Web |
| `REQ-011` | §4 | 前端按field code控制列 | getField/hook/guard对NONE隐藏、MASKED保留列 | React SDK/Web |
| `REQ-012` | §4 | DataRule保留CRUD/decision但不自动执行 | 无新`@DataScope`/query rewriter | Admin/architecture guard |
| `REQ-013` | §4 | 本地registry是MENU/ROUTE/ACTION/FIELD运行源 | 递归导航/routes/actions/fields全部从本地定义生成 | SDK/Web/Shared |
| `REQ-014` | §4 | CI/CD用SERVICE AT报告且无Manifest | 浏览器无入口；CI脚本一次PUT；Manifest源码/schema/page不存在 | Admin report/Web scripts/deletions |
| `REQ-015` | §4 | 报告防越权、全局幂等、不自动赋权 | source codes/scope/DDC校验；重复幂等；TenantApplication/RolePermission零变化 | report service/security/IT |
| `REQ-016` | §4 | DDC主数据、RBAC全局目录、租户资格分层 | RBAC不写DDC；TenantApplication独立 | DDC adapter/RBAC Admin |
| `REQ-017` | §4 | RBAC User最小、IdP批量只读补全、Department=OrgUnit(DEPT) | 无密码/profile列；RPC缺失时仅展示降级 | IdP RPC/RBAC user list |
| `REQ-018` | §4 | logout只撤销RT且Gateway下一请求退出 | 下个Gateway保护请求401；直连AT仅到exp | IdP/Gateway tests |
| `REQ-019` | §4 | IAM CRUD统一`/api/rbac3/v1/iam/**`和`/iam/**` | 新URL可用、旧URL 404，无fallback | Admin controllers/Web router |
| `REQ-020` | §4 | About只返回当前授权上下文 | JSON无apps/menus/routes/actions/tree/path/component | Contract/Starter/Web |
| `REQ-021` | §4 | 本地MENU/ROUTE递归和深链fail closed | visible descendant规则、hidden route、403/404正确 | SDK/Shared/Web |
| `REQ-022` | §4 | 本地ACTION/FIELD展示且后端PEP独立 | action隐藏、field隐藏/脱敏，接口仍403 | SDK/Starter/Web |
| `REQ-023` | §4 | RBAC JSON统一common Result/PageResult/PageQuery | 无旧envelope/page/error类型或嵌套page | Admin/controllers/clients |
| `REQ-024` | §4 | 破坏式删除Manifest | Contract/Starter/Admin/Web/schema全无Manifest能力 | deletes/V7/source guard |
| `REQ-025` | §4 | DDC bizCode/appCode分别全局唯一 | biz UK保留，app单列UK；跨BIZ重复appCode 409 | DDC V9/repository/service |
| `REQ-026` | §4 | 全局资源目录与租户授权解耦 | catalog表/幂等无tenant；TenantApplication唯一tenant+app | RBAC V7/PO/services/snapshot |

## 4. Implementation Strategy and Dependency Order

### 4.1 Ordered strategy

1. DDC先把appCode恢复为全局唯一，确保后续报告path和全局Application引用没有歧义；V9同时覆盖PostgreSQL/SQLite。
2. Gateway core先增加认证成功后的online-state扩展点，IdP core/admin提供RT权威查询，adapter再接线；默认SPI实现保持其他provider兼容。
3. IdP建立`CurrentIdentity`和批量身份目录RPC，先提供身份边界，再让RBAC User列表消费，避免RBAC持久化profile。
4. RBAC Contract先发布About/active role结构，Starter再用现有Snapshot cache组装UserDetails并替换两套方法权限实现。
5. 字段注解/Jackson作为独立PEP接在UserDetails之后，复用现有`SensitiveStrategyRegistry`，不提前触碰数据查询。
6. 唯一RBAC V7在维护窗口模型下清空旧授权图、删除Manifest、全局化catalog并新建TenantApplication；同Step同步PO/Repository/role eligibility，保持迁移后模块可启动。
7. 基于新schema实现全局Permission/Resource/Field CRUD和CI Report；SERVICE scope使用既有`@RequiresServiceScope`，不混入USER AuthorizationService。
8. About/common envelope/USER controller accessor一次破坏式切换，所有Java消费者同commit更新，避免双wrapper和双principal。
9. React SDK先发布About、本地registry和guards；Shared再支持递归children；Admin Web最后迁URL/page、删除Manifest并加入CI-only脚本。
10. 最终架构/source/dist/migration/cross-module gate只补测试和运行清单，不夹带新行为。

### 4.2 Test-first strategy

| Behavior | RED test and expected RED reason | Minimum GREEN | Refactor/wiring |
| --- | --- | --- | --- |
| DDC global appCode | V9/Service测试因单列UK和`existsByAppCode`不存在失败 | 双方言V9+Repository/Service create校验 | 保留update不可改code |
| RT online state | Gateway/TokenFacade/adapter测试因online hook/status API不存在失败 | SPI+IdP validate+adapter delegate | 复用refresh client错误映射 |
| Current identity/RPC | controller metadata/proto测试因accessor/proto/provider不存在失败 | accessor+proto+provider/client | 不持久化profile |
| UserDetails/method auth | Starter测试因principal仍为IdentityPrincipal且Aspect并行失败 | loader/token/manager/auto-config | 删除Admin重复security |
| Field PEP | serializer测试因注解/module/writer不存在失败 | fail-closed writer+desensitize | 与`@Sensitive`最严格组合 |
| Global catalog/TenantApplication | V7 migration/role tests因tenant catalog和资格表不存在失败 | V7+PO/FK+eligibility | 不建第二套catalog |
| CI report/CRUD | MockMvc/IT因SERVICE endpoint和CRUD缺失失败 | controller/service/repository事务 | 无size/rate filter；不写grant |
| About/common HTTP | contract/architecture测试因Bootstrap/old envelope/principal仍在失败 | About+common records+accessors | 同步所有controllers |
| Local registry/guards | Vitest因递归registry/about类型/getField缺失失败 | SDK composite/guards | browser无report export |
| Shared/Admin Web/CI | component/script tests因children/pages/script缺失失败 | recursive layout/pages/Node script | dist guard排除script/secret |

### 4.3 Sequential and parallel boundaries

| Step | Depends on | May run in parallel with | Must not overlap with | Reason |
| --- | --- | --- | --- | --- |
| Step 1 | None | Step 2 | DDC Admin files | DDC code约束独立 |
| Step 2 | None | Step 1 | Gateway core/engine、IdP token/adapter | online hook需整链一致 |
| Step 3 | Step 2 | None | IdP Starter/Admin/RPC、RBAC user adapter | 复用最终身份安全边界 |
| Step 4 | Step 3 | None | RBAC Contract/Starter/Admin security | UserDetails依赖身份与Contract |
| Step 5 | Step 4 | None | Starter field/autoconfig | field resolver依赖UserDetails |
| Step 6 | Steps 1,4 | None | RBAC migration/PO/repositories/role snapshot | schema与PO必须同commit可启动 |
| Step 7 | Step 6 | None | RBAC IAM catalog/report/Manifest | report依赖global schema |
| Step 8 | Steps 3,4,6,7 | None | IdP/RBAC controllers/common wrappers/About | 破坏式HTTP消费者同切 |
| Step 9 | Step 8 | Step 10 after SDK types | React SDK | 前端契约依赖About |
| Step 10 | Step 9 type contract | None | Admin Web Shared | 递归组件可独立发布 |
| Step 11 | Steps 7–10 | None | RBAC Admin Web/scripts | 页面依赖后端+SDK+Shared |
| Step 12 | Steps 1–11 | None | tests/docs only | 最终只做conformance gate |

### 4.4 Commit boundaries

每个Step完成一次RED/GREEN、一次聚焦验证和一个path-limited semantic commit。实施时使用`git add <该Step路径>`与`git commit --only <该Step路径>`，不得`git add .`；RBAC V7及其PO/Repository同步属于一个不可拆的schema compatibility commit，DDC PostgreSQL/SQLite V9属于同一版本的双方言变体并在Step 1同commit。任何Step失败先在本Step修复或回退，不跨Step带红测试。

## 5. Change File Tree

```text
egon-cola-platforms/
├── egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin/
│   ├── src/main/java/.../repository/DdcAppRepository.java                         MODIFY [S1]
│   ├── src/main/java/.../service/metadata/DdcAppService.java                       MODIFY [S1]
│   ├── src/main/resources/db/{postgresql,sqlite}/V9__enforce_global_biz_app_codes.sql CREATE [S1]
│   └── src/test/java/.../{controller/DdcAppControllerTest,repository/DdcV9MigrationTest}.java MODIFY/CREATE [S1]
├── egon-cola-platform-gateway/
│   ├── egon-cola-platform-gateway-core/src/main/java/.../security/{GatewayCredentialRecoveryProvider,GatewayCredentialOnlineStateResult}.java MODIFY/CREATE [S2]
│   └── egon-cola-platform-gateway-engine/src/{main,test}/java/.../security/GatewaySecurityChain*.java MODIFY [S2]
├── egon-cola-platform-idp/
│   ├── egon-cola-platform-idp-core/src/{main,test}/java/.../token/{TokenFacade,RefreshTokenStatus}*.java MODIFY/CREATE [S2]
│   ├── egon-cola-platform-idp-admin/src/main/java/.../
│   │   ├── oauth/controller/InternalRefreshTokenController.java                    CREATE [S2]
│   │   ├── identity/support/rpc/IdentityDirectoryRpcProvider.java                  CREATE [S3]
│   │   └── USER controllers/services                                               MODIFY [S3,S8]
│   ├── egon-cola-platform-idp-rpc-contract/src/main/{proto,java}/.../identity_directory.* CREATE [S3]
│   ├── egon-cola-platform-idp-starter/src/main/java/.../security/{CurrentIdentity,IdpEndpointAuthenticationPolicy}.java CREATE/MODIFY [S2,S3]
│   └── egon-cola-platform-idp-gateway-adapter/src/main/java/.../security/
│       ├── {IdpRefreshTokenStatusClient,ReactorNettyIdpRefreshTokenStatusClient,IdpUserOnlineStateProvider}.java CREATE [S2]
│       └── IdpUserCredentialRecoveryProvider.java                                  MODIFY [S2]
└── egon-cola-platform-rbac3/
    ├── egon-cola-platform-rbac3-contract/src/main/java/.../
    │   ├── authorization/{ActiveRoleDescriptor,SystemAuthorizationSnapshot,AppAuthorizationContext}.java CREATE/MODIFY [S4]
    │   ├── auth/{BootstrapView,Rbac3AboutView}.java                                 DELETE/CREATE [S4,S8]
    │   ├── error/Rbac3ErrorResponse.java                                             DELETE [S8]
    │   └── manifest/                                                                 DELETE DIRECTORY [S7]
    ├── egon-cola-platform-rbac3-starter/src/main/java/.../starter/
    │   ├── security/{Rbac3UserDetails,Rbac3UserDetailsLoader,CurrentRbac3User,RBACAPIResource,Rbac3MethodAuthorizationManager}.java CREATE [S4]
    │   ├── security/{Rbac3AuthenticationToken,Rbac3BearerAuthenticationFilter,RequiresPermission}.java MODIFY [S4]
    │   ├── security/Rbac3MethodAuthorizationAspect.java                              DELETE [S4]
    │   ├── field/                                                                    CREATE PACKAGE [S5]
    │   ├── authorization/{AuthorizationBootstrapService,Rbac3AboutService}.java      DELETE/CREATE [S8]
    │   ├── manifest/                                                                 DELETE DIRECTORY [S7]
    │   └── autoconfigure/{Rbac3StarterAutoConfiguration,Rbac3StarterProperties}.java MODIFY [S4,S5,S8]
    ├── egon-cola-platform-rbac3-admin/
    │   ├── src/main/resources/db/migration/V7__globalize_resource_catalog_and_remove_manifest.sql CREATE [S6]
    │   ├── src/main/java/.../admin/config/security/                                  DELETE DUPLICATE TYPES / MODIFY CONFIG [S4]
    │   ├── src/main/java/.../admin/iam/application/                                  MODIFY GLOBAL APP + CREATE tenant entitlement [S6]
    │   ├── src/main/java/.../admin/iam/{permission,resource,policy,role}/             MODIFY/CREATE [S6,S7,S8]
    │   ├── src/main/java/.../admin/iam/resource/manifest/                            DELETE DIRECTORY [S7]
    │   ├── src/main/java/.../admin/bootstrap/                                        MODIFY/DELETE BOOTSTRAP QUERY PATH [S8]
    │   ├── src/main/java/.../admin/shared/domain/vo/ApiEnvelopeVO.java               DELETE [S8]
    │   └── src/test/java/...                                                         MODIFY/CREATE [S3–S8,S12]
    ├── egon-cola-platform-rbac3-react-sdk/src/                                       MODIFY/CREATE [S9]
    └── egon-cola-platform-rbac3-admin-web/
        ├── src/app/{resourceDefinitions.json,resourceRegistry.ts,navigation.ts,router.tsx} CREATE/MODIFY [S11]
        ├── src/features/{application,iam}/                                           MODIFY/CREATE [S11]
        ├── src/features/application/ManifestDetailPage.tsx                           DELETE [S11]
        └── scripts/{report-rbac-resources,verify-browser-bundle}.mjs                 CREATE [S11]
egon-cola-platforms/egon-cola-platform-admin-web-shared/src/layout/{types,EnterpriseHeader,EnterpriseLayout.test}.tsx MODIFY [S10]
```

| Operation | Path | Symbols | Responsibility | Step | Requirements |
| --- | --- | --- | --- | --- | --- |
| MODIFY | `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin/src/main/java/top/egon/cola/component/ddc/admin/repository/DdcAppRepository.java` | `existsByAppCode/findByAppCode` | 全局APP code查询 | Step 1 | `REQ-025` |
| MODIFY | `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin/src/main/java/top/egon/cola/component/ddc/admin/service/metadata/DdcAppService.java` | `save` | create全局冲突；update不改code | Step 1 | `REQ-025` |
| CREATE | `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin/src/main/resources/db/postgresql/V9__enforce_global_biz_app_codes.sql` | DDC V9 PostgreSQL | appCode单列UK、bizCode断言 | Step 1 | `REQ-025` |
| CREATE | `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin/src/main/resources/db/sqlite/V9__enforce_global_biz_app_codes.sql` | DDC V9 SQLite | 同版本双方言schema | Step 1 | `REQ-025` |
| MODIFY | `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin/src/test/java/top/egon/cola/component/ddc/admin/controller/DdcAppControllerTest.java` | duplicate appCode cases | Service/HTTP 409契约 | Step 1 | `REQ-025` |
| CREATE | `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin/src/test/java/top/egon/cola/component/ddc/admin/repository/DdcV9MigrationTest.java` | migration assertions | PostgreSQL/SQLite结构语义 | Step 1 | `REQ-025` |
| MODIFY | `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-core/src/main/java/top/egon/cola/component/gateway/core/security/GatewayCredentialRecoveryProvider.java` | `validateAuthenticated` | 认证成功后online hook | Step 2 | `REQ-001`–`REQ-003` |
| CREATE | `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-core/src/main/java/top/egon/cola/component/gateway/core/security/GatewayCredentialOnlineStateResult.java` | result record | allow/inactive/unavailable | Step 2 | `REQ-001`–`REQ-003` |
| MODIFY | `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-engine/src/main/java/top/egon/cola/component/gateway/engine/security/GatewaySecurityChain.java` | authenticated online phase | USER auth后、authorization前校验 | Step 2 | `REQ-001`–`REQ-003`,`REQ-018` |
| MODIFY | `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-engine/src/test/java/top/egon/cola/component/gateway/engine/security/GatewaySecurityChainTest.java` | online-state matrix | 401/503/public/SERVICE/refresh | Step 2 | `REQ-001`–`REQ-003`,`REQ-018` |
| MODIFY | `egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-core/src/main/java/top/egon/cola/platform/idp/core/token/TokenFacade.java` | `validateRefresh` | signer/store权威校验 | Step 2 | `REQ-001`,`REQ-018` |
| CREATE | `egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-core/src/main/java/top/egon/cola/platform/idp/core/token/RefreshTokenStatus.java` | status record | 最小sub/tenant/expiry | Step 2 | `REQ-001` |
| MODIFY | `egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-core/src/test/java/top/egon/cola/platform/idp/core/token/TokenFacadeTest.java` | validate cases | revoke/expiry/mismatch | Step 2 | `REQ-001`,`REQ-018` |
| CREATE | `egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-admin/src/main/java/top/egon/cola/platform/idp/admin/oauth/controller/InternalRefreshTokenController.java` | internal validate API | SERVICE-scope/no-store/generic401 | Step 2 | `REQ-001`,`REQ-003`,`REQ-023` |
| MODIFY | `egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-starter/src/main/java/top/egon/cola/platform/idp/starter/security/IdpEndpointAuthenticationPolicy.java` | exact public/internal policy | 防止刷新递归 | Step 2 | `REQ-003` |
| MODIFY | `egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-starter/src/test/java/top/egon/cola/platform/idp/starter/security/IdpEndpointAuthenticationPolicyTest.java` | route policy cases | public/SERVICE/USER精确分类 | Step 2 | `REQ-003` |
| CREATE | `egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-gateway-adapter/src/main/java/top/egon/cola/platform/idp/gateway/security/IdpRefreshTokenStatusClient.java` | reactive client port | internal status调用 | Step 2 | `REQ-001`–`REQ-003` |
| CREATE | `egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-gateway-adapter/src/main/java/top/egon/cola/platform/idp/gateway/security/ReactorNettyIdpRefreshTokenStatusClient.java` | HTTP adapter | no raw-token logs | Step 2 | `REQ-001` |
| CREATE | `egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-gateway-adapter/src/main/java/top/egon/cola/platform/idp/gateway/security/IdpUserOnlineStateProvider.java` | online evaluator | compare RT/AT subject+tenant | Step 2 | `REQ-001`,`REQ-018` |
| MODIFY | `egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-gateway-adapter/src/main/java/top/egon/cola/platform/idp/gateway/security/IdpUserCredentialRecoveryProvider.java` | `validateAuthenticated` | delegate online state | Step 2 | `REQ-001`–`REQ-003` |
| MODIFY | `egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-gateway-adapter/src/main/java/top/egon/cola/platform/idp/gateway/autoconfigure/IdpGatewayAdapterAutoConfiguration.java` | beans | status client/provider wiring | Step 2 | `REQ-001`–`REQ-003` |
| MODIFY | `egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-gateway-adapter/src/test/java/top/egon/cola/platform/idp/gateway/security/IdpGatewaySecurityProviderTest.java` | online/recovery matrix | cookie/status/refresh outcomes | Step 2 | `REQ-001`–`REQ-003`,`REQ-018` |

其余Step的完整文件inventory在对应Step的“Ordered files”中以一条operation对应一个精确路径列出；实施前若实际符号已因并发提交移动，必须先更新Plan和Spec关系，不能临时写到相似包。

## 6. Prerequisites, Constraints, and Plan Clarifications

### 6.1 Repository and worktree baseline

- 分支/提交：`main@a2cde2749a9bad5d68feb51a1d972f1e356fd78f`。
- `rg --files -g AGENTS.md`无仓库文件；适用用户消息中的Main Agent Rules。
- Plan创建时`git status --short`只有目标Spec未跟踪；实施前每Step重新检查并保护所有无关改动。
- 只能新增DDC V9双方言脚本和RBAC V7；绝不修改历史迁移。双方言V9是同一DDC版本，不是两个独立业务迁移。
- Protobuf生成Java属于GENERATED，不手工编辑、不加入inventory；由Maven插件从`identity_directory.proto`生成。
- 不自动启动服务、浏览器、Redis或数据库；不执行真实Flyway迁移。Testcontainers/内存测试只属于测试进程。

### 6.2 Build, test, and environment prerequisites

| Concern | Exact command/source | Required state | Validation boundary |
| --- | --- | --- | --- |
| Java/Maven | `java -version && mvn -version`；platform parent要求Java 21 | JDK21/Maven可用 | 工具链 |
| DDC | `mvn -f egon-cola-platforms/pom.xml -pl egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin -am test` | exit 0 | 双方言测试，不证明live DB |
| Gateway/IdP | `mvn -f egon-cola-platforms/pom.xml -pl egon-cola-platform-gateway/egon-cola-platform-gateway-core,egon-cola-platform-gateway/egon-cola-platform-gateway-engine,egon-cola-platform-idp/egon-cola-platform-idp-core,egon-cola-platform-idp/egon-cola-platform-idp-starter,egon-cola-platform-idp/egon-cola-platform-idp-gateway-adapter,egon-cola-platform-idp/egon-cola-platform-idp-admin -am test` | exit 0 | 模块链，不证明真实JWK/Redis |
| RBAC | `mvn -f egon-cola-platforms/pom.xml -pl egon-cola-platform-rbac3/egon-cola-platform-rbac3-contract,egon-cola-platform-rbac3/egon-cola-platform-rbac3-starter,egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin -am test` | exit 0 | Testcontainers/模块，不证明live拓扑 |
| React SDK | `npm test && npm run build` in `egon-cola-platform-rbac3-react-sdk` | exit 0 | SDK产物 |
| Shared | `npm test && npm run build` in `egon-cola-platform-admin-web-shared` | exit 0 | Shared产物；postbuild删除node_modules需在隔离步骤执行 |
| Admin Web | `npm run test && npm run typecheck && npm run lint && npm run build` with non-secret `VITE_IDP_ISSUER/VITE_IDP_CLIENT_ID/VITE_IDP_AUDIENCE` | exit 0 | Browser bundle静态，不证明UI运行 |
| Static | `git diff --check`、Plan source guards、Spec/Plan strict validators | no findings | repository consistency |

### 6.3 Immutable constraints and approved decisions

- USER token只有AT/RT；权限不进JWT；无Session、第三USER token或UserDetails密码认证。
- Gateway每个保护USER请求在线查RT，不允许正向缓存；IdP不可用503不清Cookie，确定无效401清Cookie。
- RBAC global catalog无tenant；tenant资格只有TenantApplication，角色/规则仍tenant-scoped。
- DDC bizCode/appCode分别全局唯一；RBAC application表只能对appCode唯一，bizCode仅普通归属索引，因为一个BIZ可含多个APP。
- CI报告只用短期SERVICE AT和既有`@RequiresServiceScope`，忽略principal.tenantId作为catalog维度；浏览器无报告client/button/secret。
- 报告不设1MiB总体限制和业务限流，但保留Spec列出的元素数、字符串、图、枚举和checksum正确性校验。
- 不创建Manifest替代模块、第二套catalog、通用规则引擎、BaseService层或DataScope执行器。
- Design pattern只使用现有扩展点需要的Strategy/AuthorizationManager、Adapter、Jackson Decorator/Template Method和前端Composite；直接代码足以处理TenantApplication和report事务，不增加Factory/Command Bus。

### 6.4 Plan Clarifications

| ID | Small implementation inference | Repository evidence | Why semantics are unchanged | Impact if wrong |
| --- | --- | --- | --- | --- |
| `PLAN-CLAR-001` | DDC现有`update`继续只改name/owner/description，不开放appCode/bizCode修改；换code通过明确新建 | `DdcAppService.update`当前签名/赋值 | 保持现有API行为，只收紧create/DB唯一性 | 若要原地改code需另定引用迁移，不可在本Plan扩大 |
| `PLAN-CLAR-002` | CI endpoint用既有`@RequiresServiceScope`而不是USER `@RequiresPermission` | IdP Starter已有`RequiresServiceScope/ServiceScopeAuthorization` | 严格区分机器scope和RBAC USER permission | 若混用会要求不存在的Rbac3UserDetails并破坏SERVICE链 |
| `PLAN-CLAR-003` | Admin Web纯数据声明使用JSON，浏览器本地再绑定component；Node CI脚本直接读JSON | Vite/TS无tsx runtime依赖，Spec禁止新plugin/module | 同一声明源且脚本不进入browser import graph | 若JSON不能表达某机械事实，只能扩展schema，不得回到Manifest |
| `PLAN-CLAR-004` | RBAC V7按Spec破坏式清空旧authorization/catalog facts后重建FK，不尝试tenant副本合并 | V6已要求空legacy application graph；用户允许不保旧数据 | 避免猜测租户购买和跨tenant资源合并 | 若live数据必须保留，实施必须停止并回Spec设计迁移映射 |

## 7. Ordered File-by-file Implementation Steps

> 每一步先落RED测试，再落最小GREEN实现；每步验证通过后做一个path-limited commit。下列package目录删除/创建均是确定边界，不能用同名新模块替代。

### Step 1 — 收紧 DDC BIZ/APP 全局 code 唯一性

- Requirements: `REQ-016`, `REQ-025`
- Dependencies: `None`
- Observable outcome: `biz_code`保留单列UK，`app_code`在PostgreSQL/SQLite均为单列UK；不同BIZ创建相同appCode稳定409，update仍不改code。
- Ordered files:

#### File 1 — `CREATE egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin/src/test/java/top/egon/cola/component/ddc/admin/repository/DdcV9MigrationTest.java`

- Purpose: RED锁定双方言迁移最终约束。
- Symbols: `postgresqlHasIndependentBizAndAppCodeUniqueConstraints`、`sqliteHasIndependentBizAndAppCodeUniqueConstraints`。
- Why now: 先证明V7组合唯一不是目标。
- Contract/signature changes: 预期V9存在`uk_ddc_app_code(app_code)`且不以`biz_code`参与APP唯一。
- Implementation pseudocode:

```java
migrateFixtureToV9(dialect);
assertUnique("ddc_biz", List.of("biz_code"));
assertUnique("ddc_app", List.of("app_code"));
insertApps("biz-a", "same");
assertUniqueViolation(() -> insertApps("biz-b", "same"));
```

- After this file: 因V9不存在而RED。

#### File 2 — `MODIFY egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin/src/test/java/top/egon/cola/component/ddc/admin/controller/DdcAppControllerTest.java`

- Purpose: RED/GREEN定义全局exists查询和409映射。
- Symbols: `existsByAppCode`、`rejectsDuplicateAppCodeAcrossBusinesses`。
- Why now: schema契约已固定，先补Service可读预检。
- Contract/signature changes: 删除create路径对`existsByBizCodeAndAppCode`的依赖；列表的BIZ过滤方法保留。
- Implementation pseudocode:

```java
when(repository.existsByAppCode("shared-code")).thenReturn(true);
assertCommonConflict(post(app("biz-b", "shared-code")));
```

- After this file: Controller测试因Service尚未切查询RED；Repository接口可编译。

#### File 3 — `MODIFY egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin/src/main/java/top/egon/cola/component/ddc/admin/service/metadata/DdcAppService.java`

- Purpose: 最小GREEN实现create全局冲突。
- Symbols: `save`。
- Why now: 测试已固定现有update不改code。
- Contract/signature changes: `save`规范化/校验appCode后调用`existsByAppCode`；不自动加前后缀。
- Implementation pseudocode:

```java
validateBizExists(app.bizCode());
if (appRepository.existsByAppCode(app.appCode())) throw APP_CODE_EXISTS;
assignIdAndAudit(app);
return appRepository.save(app);
```

- After this file: Service/Controller聚焦测试GREEN。

#### File 4 — `CREATE egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin/src/main/resources/db/postgresql/V9__enforce_global_biz_app_codes.sql`

- Purpose: 在两方言落同一Flyway版本。
- Symbols: drop composite UK、create single app UK、assert/retain biz UK。
- Why now: 行为测试GREEN后落持久兜底。
- Contract/signature changes: 不改V1–V8；重复历史appCode导致迁移失败并要求上线前改code。
- Implementation pseudocode:

```sql
-- fail if duplicate app_code exists
DROP INDEX/CONSTRAINT uk_ddc_app_biz_code;
CREATE UNIQUE INDEX uk_ddc_app_code ON ddc_app(app_code);
-- retain/assert uk_ddc_biz_code ON ddc_biz(biz_code)
```

- After this file: V9迁移测试GREEN，两方言结果一致。

- Verification command: `mvn -f egon-cola-platforms/pom.xml -pl egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin -am -Dsurefire.failIfNoSpecifiedTests=false -Dtest=DdcV9MigrationTest,DdcAppControllerTest test`
- Expected result: 聚焦测试exit 0；重复appCode跨BIZ为409/UK冲突。
- Completion criteria: `REQ-025`全部可观察项满足，V1–V8 diff为空。
- Rollback: 在V9尚未部署时path-limited revert；部署后只允许新V10 forward-fix或备份恢复，不改V9。
- Commit: `feat(ddc): enforce globally unique business and application codes`

### Step 2 — 在 Gateway USER 主链接入 IdP RT 在线态

- Requirements: `REQ-001`, `REQ-002`, `REQ-003`, `REQ-018`
- Dependencies: `None`
- Observable outcome: protected USER请求在AT认证成功后、授权前校验RT；inactive=401清Cookie，IdP unavailable=503保Cookie；public/SERVICE跳过，AT过期仍走一次refresh。
- Ordered files:

#### File 1 — `MODIFY egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-engine/src/test/java/top/egon/cola/component/gateway/engine/security/GatewaySecurityChainTest.java`

- Purpose: RED覆盖四层契约。
- Symbols: Spec `TEST-001`–`TEST-003`全部场景。
- Why now: 防止只改adapter而Engine从未调用。
- Contract/signature changes: 预期新的online result和internal status endpoint。
- Implementation pseudocode:

```java
assertOutcome(validAt(), revokedRt()).is401AndExpiresCookies();
assertOutcome(validAt(), idpTimeout()).is503AndKeepsCookies();
assertNoStatusCall(publicRoute());
assertRefreshThenStatus(expiredAt(), activeRt());
```

- After this file: 因SPI/status API/provider不存在RED。

#### File 2 — `MODIFY egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-core/src/main/java/top/egon/cola/component/gateway/core/security/GatewayCredentialRecoveryProvider.java`

- Purpose: 建立默认兼容的online phase。
- Symbols: `validateAuthenticated(authContext,exchange)` default、结果分支。
- Why now: Gateway core先发布SPI，IdP adapter才能实现。
- Contract/signature changes: 仅保护USER且已认证时调用；default allow保持非IdP provider兼容。
- Implementation pseudocode:

```java
authenticated = authenticateOrRecover(...);
if (authenticated.userProtected()) {
  online = recoveryProvider.validateAuthenticated(authenticated, exchange);
  if (online.inactive()) return unauthorizedAndExpireCookies();
  if (online.unavailable()) return serviceUnavailableKeepCookies();
}
return authorize(authenticated);
```

- After this file: Engine测试可用fake provider达到GREEN，IdP adapter测试仍RED。

#### File 3 — `MODIFY egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-core/src/main/java/top/egon/cola/platform/idp/core/token/TokenFacade.java`

- Purpose: 复用TokenSigner/RefreshTokenStore提供权威只读状态。
- Symbols: `validateRefresh(String)`、`POST /internal/v1/oauth2/refresh-token/validate`。
- Why now: adapter需要稳定服务端契约。
- Contract/signature changes: raw RT只在TLS form body；SERVICE scope；no-store；无效统一401。
- Implementation pseudocode:

```java
claims = signer.verifyRefresh(raw);
record = store.findValid(digest(raw));
requireSameSubjectTenantAndNotExpired(claims, record);
return new RefreshTokenStatus(subject, tenantId, expiresAt);
```

- After this file: TokenFacade/controller测试GREEN。

#### File 4 — `MODIFY egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-starter/src/main/java/top/egon/cola/platform/idp/starter/security/IdpEndpointAuthenticationPolicy.java`

- Purpose: 连接Gateway online hook与IdP endpoint。
- Symbols: status client、provider delegate、auto-config beans。
- Why now: core/endpoint均已存在。
- Contract/signature changes: compare RT status subject/tenant with authenticated AT；不转发RT下游，不记录raw token。
- Implementation pseudocode:

```java
rt = extractor.refreshCookie(exchange);
return statusClient.validate(rt)
  .map(status -> sameIdentity(status, auth) ? active() : inactive())
  .onErrorMap(confirmedInvalid, inactive)
  .onErrorMap(timeoutOr5xx, unavailable);
```

- After this file: Adapter/Engine完整矩阵GREEN。

- Verification command: `mvn -f egon-cola-platforms/pom.xml -pl egon-cola-platform-gateway/egon-cola-platform-gateway-core,egon-cola-platform-gateway/egon-cola-platform-gateway-engine,egon-cola-platform-idp/egon-cola-platform-idp-core,egon-cola-platform-idp/egon-cola-platform-idp-starter,egon-cola-platform-idp/egon-cola-platform-idp-gateway-adapter,egon-cola-platform-idp/egon-cola-platform-idp-admin -am -Dsurefire.failIfNoSpecifiedTests=false -Dtest=GatewaySecurityChainTest,TokenFacadeTest,IdpEndpointAuthenticationPolicyTest,IdpGatewaySecurityProviderTest test`
- Expected result: exit 0；401/503/cookie/status/refresh调用次数断言通过。
- Completion criteria: REQ-001/002/003/018完整，日志/source scan无raw RT。
- Rollback: path-limited revert before rollout；启用后回退会恢复最长5分钟撤销窗口，必须按Spec告警。
- Commit: `feat(identity): validate refresh-token online state on protected gateway requests`

### Step 3 — 提供 IdP 当前身份访问器和只读批量身份目录 RPC

- Requirements: `REQ-008`, `REQ-017`
- Dependencies: `Step 2`
- Observable outcome: IdP Service可从SecurityContext读取当前USER；IdentityDirectory RPC按1–100 subjects返回最小profile/missing集合；RBAC用户列表只读补全且不持久化profile。
- Ordered files:

#### File 1 — `CREATE egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-starter/src/test/java/top/egon/cola/platform/idp/starter/security/CurrentIdentityTest.java`

- Purpose: RED定义USER/anonymous/SERVICE三类访问。
- Symbols: `requiresCurrentUserIdentity`、`rejectsAnonymousAndServiceAsUser`。
- Why now: 后续Service移除Controller principal需要统一入口。
- Contract/signature changes: `CurrentIdentity.current()/require()`只返回`IdentityPrincipal` USER。
- Implementation pseudocode:

```java
setAuthentication(userPrincipal());
assertThat(currentIdentity.require()).isEqualTo(user);
setAuthentication(servicePrincipal());
assertThatThrownBy(currentIdentity::require).isUnauthorized();
```

- After this file: 因CurrentIdentity不存在RED。

#### File 2 — `CREATE egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-starter/src/main/java/top/egon/cola/platform/idp/starter/security/CurrentIdentity.java`

- Purpose: 最小GREEN访问当前USER。
- Symbols: `current`、`require`。
- Why now: 测试已固定类型边界。
- Contract/signature changes: 不缓存principal，不接受SERVICE替代USER。
- Implementation pseudocode:

```java
authentication = SecurityContextHolder.getContext().getAuthentication();
return authentication != null && authentication.getPrincipal() instanceof IdentityPrincipal user
    ? Optional.of(user) : Optional.empty();
```

- After this file: accessor测试GREEN。

#### File 3 — `CREATE egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-rpc-contract/src/main/proto/identity_directory.proto`

- Purpose: 先以proto contract RED锁定字段号和敏感字段排除。
- Symbols: `BatchGetIdentityProfiles`；request `subjects=1`；response `profiles=1/missing_subjects=2`；profile `subject=1,username=2,display_name=3,status=4,version=5`。
- Why now: provider/client均依赖生成contract。
- Contract/signature changes: 无password/email/phone/token/role。
- Implementation pseudocode:

```protobuf
rpc BatchGetIdentityProfiles(BatchGetIdentityProfilesRequest) returns (BatchGetIdentityProfilesResponse);
message BatchGetIdentityProfilesRequest { repeated string subjects = 1; }
message BatchGetIdentityProfilesResponse { repeated IdentityProfile profiles = 1; repeated string missing_subjects = 2; }
```

- After this file: contract test在生成前RED，Maven generate-sources后可编译。

#### File 4 — `CREATE egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-admin/src/main/java/top/egon/cola/platform/idp/admin/identity/support/rpc/IdentityDirectoryRpcProvider.java`

- Purpose: 批量读取IdP核心用户信息。
- Symbols: provider method、1–100/duplicate validation、missing映射。
- Why now: contract已生成。
- Contract/signature changes: 复用`IdentityUserDirectory`只读查询；SERVICE metadata由现有RPC安全负责。
- Implementation pseudocode:

```java
subjects = validateUnique(request.getSubjectsList(), 1, 100);
rows = directory.findBySubjects(subjects);
profiles = rows.map(subject, username, displayName, status, version);
missing = subjects - rows.subjects;
return response(profiles, missing);
```

- After this file: provider契约测试GREEN。

#### File 5 — `CREATE egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/iam/user/repository/IdentityProfileDirectory.java`

- Purpose: RBAC分页后一次批量补display fields，UNAVAILABLE时返回partial marker。
- Symbols: `batchGet`、User directory assembler。
- Why now: IdP provider已可调用。
- Contract/signature changes: RBAC UserPO不新增profile列；授权决策不依赖RPC结果。
- Implementation pseudocode:

```java
page = membershipRepository.page(...);
profiles = identityProfileDirectory.batchGet(uniqueSubjects(page));
return page.map(user -> mergeMinimalUserWithOptionalProfile(user, profiles));
// unavailable -> keep authorization row, mark identityProfileAvailable=false
```

- After this file: RBAC user测试覆盖enriched/missing/unavailable且DB无profile字段。

#### File 6 — `MODIFY egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/pom.xml`

- Purpose: 声明RBAC Admin对IdP RPC contract的直接编译依赖。
- Symbols: `egon-cola-platform-idp-rpc-contract` dependency。
- Why now: RPC client代码已确定，不能依赖transitive偶然可见性。
- Contract/signature changes: 使用父POM dependencyManagement版本，不新增第三方库。
- Implementation pseudocode:

```xml
<dependency>
  <groupId>top.egon</groupId>
  <artifactId>egon-cola-platform-idp-rpc-contract</artifactId>
</dependency>
```

- After this file: RBAC Admin在clean reactor中可生成/引用IdentityDirectory types。

- Verification command: `mvn -f egon-cola-platforms/pom.xml -pl egon-cola-platform-idp/egon-cola-platform-idp-rpc-contract,egon-cola-platform-idp/egon-cola-platform-idp-starter,egon-cola-platform-idp/egon-cola-platform-idp-admin,egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin -am -Dsurefire.failIfNoSpecifiedTests=false -Dtest=CurrentIdentityTest,IdentityDirectoryRpcContractTest,IdentityDirectoryRpcProviderTest,UserControllerTest test`
- Expected result: exit 0；proto字段号、边界、partial display和无敏感字段断言通过。
- Completion criteria: `REQ-017`全满足；`CurrentIdentity`可供Step 8迁移Controller。
- Rollback: revert RPC provider/client和accessor；不涉及schema/data。
- Commit: `feat(idp): expose current identity and batch profile directory`

### Step 4 — 统一 RBAC3 UserDetails 和方法级权限执行

- Requirements: `REQ-004`, `REQ-005`, `REQ-006`, `REQ-007`, `REQ-008`
- Dependencies: `Step 3`
- Observable outcome: RBAC Filter把Identity+有效Snapshot组装为`Rbac3UserDetails`；两注解共用Spring Method Security/AuthorizationService；Admin重复principal/filter/aspect删除。
- Ordered files:

#### File 1 — `MODIFY egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-contract/src/test/java/top/egon/cola/platform/rbac3/contract/ContractSerializationTest.java`

- Purpose: RED固定active role descriptor、principal内容、注解AND和403。
- Symbols: `ActiveRoleDescriptor`、loader/manager tests。
- Why now: Contract和安全行为先于实现。
- Contract/signature changes: Snapshot activeRoles从纯id扩展为id/code/applicationCode；无未激活角色。
- Implementation pseudocode:

```java
details = loader.load(identity, "rbac3-admin");
assertThat(details.activeRoles()).containsOnly(effectiveActivatedRole);
assertThat(details.permissions()).containsExactlyInAnyOrder(activePermission);
assertDenied(methodWith(api("p:a"), requires("p:b")), whenOnly("p:a"));
```

- After this file: 因records/loader/manager不存在RED。

#### File 2 — `CREATE egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-contract/src/main/java/top/egon/cola/platform/rbac3/contract/authorization/ActiveRoleDescriptor.java`

- Purpose: 发布UserDetails/About需要的稳定Contract。
- Symbols: activeRoles、landingRouteCode、permissions/fieldPolicies保留。
- Why now: Starter编译依赖。
- Contract/signature changes: 移除只为Bootstrap资源树服务的resource facts；不改变JWT。
- Implementation pseudocode:

```java
record ActiveRoleDescriptor(String roleId, String roleCode, String applicationCode) {}
snapshot = snapshot.withActiveRoles(sortedDescriptors).withoutRuntimeResourceTree();
```

- After this file: contract测试GREEN，Starter仍RED。

#### File 3 — `CREATE egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-starter/src/main/java/top/egon/cola/platform/rbac3/starter/security/Rbac3UserDetails.java`

- Purpose: 用现有Snapshot cache组装最终principal。
- Symbols: immutable UserDetails、loader、accessor、token principal。
- Why now: Contract已发布。
- Contract/signature changes: 缓存仍存Snapshot；每请求合入当前IdentityPrincipal；UserDetails password为空且不参与认证。
- Implementation pseudocode:

```java
snapshot = singleFlightLoader.load(system, tenant, subject);
validateIdentityAndVersion(snapshot, identity);
details = Rbac3UserDetails.from(identity, snapshot.activeRoles(), snapshot.permissions(), snapshot.fieldPolicies());
SecurityContextHolder.setAuthentication(new Rbac3AuthenticationToken(details));
```

- After this file: loader/filter测试GREEN。

#### File 4 — `CREATE egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-starter/src/main/java/top/egon/cola/platform/rbac3/starter/security/RBACAPIResource.java`

- Purpose: 将两注解接入一个AuthorizationManager。
- Symbols: resolver、manager/interceptor bean。
- Why now: UserDetails已能提供上下文。
- Contract/signature changes: 类型+方法+API permission去重后AND；blank启动失败；self-invocation不作为边界。
- Implementation pseudocode:

```java
required = resolver.resolve(method, targetClass);
for (permission : stableDistinct(required)) authorizationService.requirePermission(permission);
return granted();
```

- After this file: method manager测试GREEN。

#### File 5 — `DELETE egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/config/security/CurrentRbac3Principal.java`

- Purpose: 删除并行principal/PEP，只保留Starter。
- Symbols: Admin security wiring和absence assertions。
- Why now: Starter替代能力已GREEN。
- Contract/signature changes: SERVICE principal过滤链不变；USER使用Rbac3UserDetails。
- Implementation pseudocode:

```text
remove Admin principal filter and custom method interceptor beans
import/use Starter Security beans
assert exactly one USER method authorization interceptor and no duplicate principal types
```

- After this file: Admin context启动测试GREEN。

- Verification command: `mvn -f egon-cola-platforms/pom.xml -pl egon-cola-platform-rbac3/egon-cola-platform-rbac3-contract,egon-cola-platform-rbac3/egon-cola-platform-rbac3-starter,egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin -am -Dsurefire.failIfNoSpecifiedTests=false -Dtest=ContractSerializationTest,Rbac3UserDetailsLoaderTest,Rbac3MethodAuthorizationManagerTest,Rbac3AdminApplicationContextTest test`
- Expected result: exit 0；principal/注解AND/单一interceptor assertions通过。
- Completion criteria: REQ-004–008的安全主体/方法部分完成；role TenantApplication资格由Step 6补齐。
- Rollback: path-limited revert Contract+Starter+Admin security为同一commit；不得只恢复Admin filter。
- Commit: `refactor(rbac3): unify user details and method authorization`

### Step 5 — 接入字段注解和 Jackson fail-closed 序列化

- Requirements: `REQ-009`, `REQ-010`, `REQ-011`, `REQ-022`
- Dependencies: `Step 4`
- Observable outcome: 注解字段按UserDetails field policy输出null/masked/raw；异常不泄漏原值；与既有`@Sensitive`最严格者优先。
- Ordered files:

#### File 1 — `CREATE egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-starter/src/test/java/top/egon/cola/platform/rbac3/starter/field/Rbac3FieldPropertyWriterTest.java`

- Purpose: RED覆盖NONE/missing/error/MASKED/READ/WRITE和双注解顺序。
- Symbols: serializer fixtures、strategy failure。
- Why now: 字段安全必须测试先行。
- Contract/signature changes: 属性名保留且值为null，不省略字段。
- Implementation pseudocode:

```java
assertJson(policy(NONE), dto(secret)).contains("secret", null);
assertJson(policy(MASKED_READ), dto("13800138000")).contains(masked);
assertJson(policy(READ), sensitiveDto()).contains(staticSensitiveMask);
assertJson(strategyThrows(), dto(secret)).contains("secret", null);
```

- After this file: 因annotation/module/writer不存在RED。

#### File 2 — `CREATE egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-starter/src/main/java/top/egon/cola/platform/rbac3/starter/field/RBACFieldResource.java`

- Purpose: 最小GREEN Jackson扩展。
- Symbols: annotation、module、modifier、writer。
- Why now: RED已固定fail-closed。
- Contract/signature changes: `(applicationCode,resourceCode,permission,fieldCode)`查当前UserDetails；复用SensitiveStrategyRegistry。
- Implementation pseudocode:

```java
decision = currentRbac3User.current().flatMap(policyResolver::resolve).orElse(NONE);
switch (decision.level()) {
  case NONE -> writeNullField();
  case MASKED_READ -> writeMaskedOrNullOnError(strategyRegistry, rawValue);
  case READ, WRITE -> delegateRespectingExistingSensitiveWriter();
}
```

- After this file: serializer测试GREEN。

#### File 3 — `MODIFY egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-starter/src/main/java/top/egon/cola/platform/rbac3/starter/autoconfigure/Rbac3StarterAutoConfiguration.java`, `MODIFY egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-starter/src/test/java/top/egon/cola/platform/rbac3/starter/autoconfigure/Rbac3StarterAutoConfigurationTest.java`

- Purpose: 注册唯一Jackson module且支持条件装配。
- Symbols: field module bean。
- Why now: writer已独立GREEN。
- Contract/signature changes: 不影响request deserializer；无RBAC时仍fail closed。
- Implementation pseudocode:

```text
register Rbac3FieldJacksonModule once when ObjectMapper is present
inject CurrentRbac3User and SensitiveStrategyRegistry
assert no duplicate module and request deserialization unchanged
```

- After this file: auto-config测试GREEN。

- Verification command: `mvn -f egon-cola-platforms/pom.xml -pl egon-cola-platform-rbac3/egon-cola-platform-rbac3-starter -am -Dsurefire.failIfNoSpecifiedTests=false -Dtest=Rbac3FieldPropertyWriterTest,Rbac3StarterAutoConfigurationTest test`
- Expected result: exit 0；所有禁止/异常场景JSON不含原值。
- Completion criteria: 后端FIELD PEP完成；CRUD/前端展示分别在Step 7/9/11。
- Rollback: revert field package与auto-config；无持久数据。
- Commit: `feat(rbac3): enforce field authorization during response serialization`

### Step 6 — 用 RBAC V7 全局化目录并建立 TenantApplication 资格

- Requirements: `REQ-007`, `REQ-010`, `REQ-012`, `REQ-016`, `REQ-024`, `REQ-026`
- Dependencies: `Step 1`, `Step 4`
- Observable outcome: catalog五表无tenant，TenantApplication唯一tenant+app；所有tenant role/rule/service治理FK引用global ids并在Service校验资格；Manifest表/FK/trigger删除。
- Ordered files:

#### File 1 — `MODIFY egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/repository/Rbac3MigrationContractTest.java`

- Purpose: RED锁定schema、资格、跨APP/PENDING拒绝和报告隔离前提。
- Symbols: Spec TEST-021/022。
- Why now: V7破坏性最大，先固定完整目标。
- Contract/signature changes: 验证所有Spec列出的consumer FK，不只五张目录表。
- Implementation pseudocode:

```java
migrateV1ThroughV7();
assertNoColumn(globalCatalogTables, "tenant_id");
assertTable("rbac3_tenant_application").unique("tenant_id", "application_id");
assertNoTable("rbac3_resource_manifest");
assertRoleIneligible(tenantWithoutActiveApplication());
```

- After this file: 因V7/TenantApplication不存在RED。

#### File 2 — `CREATE egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/resources/db/migration/V7__globalize_resource_catalog_and_remove_manifest.sql`

- Purpose: 唯一RBAC schema迁移。
- Symbols: dependency cleanup、global FK/UK/index、TenantApplication、Manifest drop。
- Why now: migration contract已固定。
- Contract/signature changes: 按Spec §11完整表清单；不修改V1–V6，不回填旧tenant副本。
- Implementation pseudocode:

```sql
ASSERT/RAISE when unexpected retained graph violates destructive precondition;
DELETE child authorization facts in FK order;
DROP all tenant-scoped catalog FKs and manifest relationships;
ALTER application/permission/resource/permission_resource/field_definition DROP tenant_id;
CREATE global keys and report columns/checks;
CREATE rbac3_tenant_application (... UNIQUE (tenant_id, application_id));
RECREATE role/rule/service/governance FKs to global ids;
DROP TABLE rbac3_resource_manifest;
```

- After this file: schema测试可检查目标，Java JPA启动仍因PO不匹配RED。

#### File 3 — `MODIFY egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/iam/application/domain/po/ApplicationPO.java`

- Purpose: 使全部JPA映射与V7一致。
- Symbols: 去TenantScopedPO继承的global POs、global FK字段、TenantApplication。
- Why now: schema已固定。
- Contract/signature changes: global PO禁止tenant；tenant POs保留tenant/application id；不创建平行Catalog PO。
- Implementation pseudocode:

```java
ApplicationPO /* no TenantScopedPO; retain explicit existing id/version/audit fields */;
TenantApplicationPO extends TenantScopedPO { Long applicationId; Status status; Instant validFrom; ... }
RolePermissionPO keeps tenantId/applicationId/roleId and references global permissionId;
```

- After this file: JPA映射可启动，repositories/service仍需改查询。

#### File 4 — `RENAME egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/iam/application/service/ApplicationScopeFacade.java`

- Purpose: 把旧tenant Application admission显式迁为TenantApplication并接入资格/快照。
- Symbols: create/page/updateStatus、`isActiveAt`、role eligibility。
- Why now: PO/schema可用。
- Contract/signature changes: `/api/rbac3/v1/iam/tenant-applications`；Snapshot只投影满足UserBusinessAccess+TenantApplication+DDC+activation的角色。
- Implementation pseudocode:

```java
tenant = currentRbac3User.require().tenantId();
catalog = globalApplicationRepository.requireActive(command.applicationId());
requireBusinessAccess(tenant, actorUserId, catalog.ddcBusinessId());
entitlement = repository.createUnique(tenant, catalog.id(), command.windowAndSource());
invalidateAuthVersionAfterCommit(tenant);
```

- After this file: TenantApplication/role/snapshot测试GREEN。

#### File 5 — `MODIFY egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/iam/role/repository/jpa/JpaRoleRepository.java`

- Purpose: 更新所有global FK消费者查询和同APP/TenantApplication校验。
- Symbols: role grant、field/data rules、SoD/participation/service permission snapshot。
- Why now: 核心资格服务已存在。
- Contract/signature changes: 不以tenant拼global catalog查询；tenant治理事实仍从tenant切入。
- Implementation pseudocode:

```text
load tenant facts by tenant/role
join global permission/resource/field by single id
assert every referenced global row applicationId equals tenant fact applicationId
exclude facts when TenantApplication is not ACTIVE at now
```

- After this file: Admin module/migration/role/snapshot聚焦测试GREEN。

- Verification command: `mvn -f egon-cola-platforms/pom.xml -pl egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin -am -Dsurefire.failIfNoSpecifiedTests=false -Dtest=Rbac3MigrationContractTest,Rbac3GlobalCatalogV7IT,TenantApplicationServiceTest,RoleEligibilityServiceTest,UserAuthorizationSnapshotProjectorTest test`
- Expected result: exit 0；schema、JPA、资格、快照和Manifest absence断言通过。
- Completion criteria: V7唯一、历史迁移无diff、global/tenant边界完整；DataRule仍无执行器。
- Rollback: V7部署前revert；部署后备份恢复或新V8 forward-fix，绝不编辑V7。
- Commit: `feat(rbac3): globalize resource catalog and add tenant application entitlements`

### Step 7 — 落全局资源 CRUD、CI 报告并删除 Manifest 生命周期

- Requirements: `REQ-010`, `REQ-013`, `REQ-014`, `REQ-015`, `REQ-016`, `REQ-019`, `REQ-024`, `REQ-026`
- Dependencies: `Step 6`
- Observable outcome: Permission/Resource/FieldDefinition/FieldRule具备IAM CRUD；CI SERVICE endpoint事务更新全局CI_REPORT事实；浏览器/USER/错误source请求零写；Manifest生产源码和表映射全部消失。
- Ordered files:

#### File 1 — `CREATE egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/iam/resource/report/CiResourceReportControllerTest.java`

- Purpose: RED定义API-003/API-011、CRUD、source ownership和零赋权。
- Symbols: Spec TEST-008/011/013。
- Why now: schema已准备，先固定报告安全/事务边界。
- Contract/signature changes: `PUT /api/rbac3/v1/iam/resource-catalog/businesses/{bizCode}/applications/{appCode}/frontend-resources`；无tenant/body size/rate filter。
- Implementation pseudocode:

```java
assertForbidden(reportAsUser());
assertForbidden(reportAsServiceWithWrongSourceApp());
before = checksums(tenantApplication, rolePermission, fieldRule);
result = reportAsBoundService(validCompleteRegistry());
assertThat(result).hasDiffCountsAndPendingDefaults();
assertThat(checksums(...)).isEqualTo(before);
assertIdempotentReplay(sameBuildAndChecksum());
```

- After this file: 因controllers/services/repositories不存在RED。

#### File 2 — `CREATE egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/iam/permission/controller/PermissionController.java`

- Purpose: 全局Permission list/page/detail/create/update/status和ACTIVE选择。
- Symbols: `/api/rbac3/v1/iam/permissions` CRUD。
- Why now: report和Resource映射依赖permission service。
- Contract/signature changes: common Result/PageResult；PENDING不可授予；permissionCode全局唯一。
- Implementation pseudocode:

```java
create(command) { requireGlobalApplication(command.applicationId()); requireUniqueCode(command.code()); save(PENDING_OR_MANUAL); }
changeStatus(id, ACTIVE) { validateReviewFields(); saveAndInvalidateCatalogVersion(); }
findAssignable(appId) { return activeOnly(appId); }
```

- After this file: Permission CRUD测试GREEN。

#### File 3 — `MODIFY egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/iam/resource/controller/ApplicationResourceController.java`

- Purpose: 把Resource/Field CRUD改为global application并提供管理树。
- Symbols: API-011、FieldDefinition CRUD/status。
- Why now: Permission CRUD可提供required permission验证。
- Contract/signature changes: URL统一`/api/rbac3/v1/iam/resource-catalog/**`；无tenant入参；MANUAL/CI_REPORT安全字段边界。
- Implementation pseudocode:

```java
rows = repository.findByApplicationAndFilters(appId, statuses, types);
validateGraph(rows);
return assembleStableTreeWithActions(rows);
updateCiOwnedField(id, command) { rejectSecurityFieldMutationFromReportPath(); }
```

- After this file: resource/field CRUD和tree测试GREEN。

#### File 4 — `MODIFY egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/iam/policy/controller/ConstraintController.java`

- Purpose: FieldRule/DataRule URL和global permission/field引用适配。
- Symbols: field/data CRUD、decision读取。
- Why now: 全局FieldDefinition已存在。
- Contract/signature changes: FieldRule Service校验TenantApplication ACTIVE/同APP；DataRule仍无执行器。
- Implementation pseudocode:

```java
requireTenantRoleAndEntitlement(tenant, command.roleId(), command.applicationId());
permission = globalPermission.requireActive(command.permissionId());
field = globalField.require(command.fieldDefinitionId());
requireSameApplication(role, permission, field);
saveRuleAndInvalidatePolicyVersion();
```

- After this file: FieldRule/DataRule测试GREEN，source scan仍无DataScope。

#### File 5 — `CREATE egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/iam/resource/report/controller/CiResourceReportController.java`

- Purpose: 实现CI-only完整replace事务。
- Symbols: API-003、canonicalizer、source code/DDC validator、diff result。
- Why now: 所有catalog services/repositories已就绪。
- Contract/signature changes: `@RequiresServiceScope("rbac3:resource-catalog:report")`；允许SERVICE principal参数例外；不读取principal.tenantId。
- Implementation pseudocode:

```java
require(service.sourceBizCode == pathBiz && service.sourceAppCode == pathApp);
ddcApp = ddc.resolveEnabled(pathBiz, pathApp);
canonical = validateSortAndHash(request.withoutChecksum());
globalApp = lockOrCreateCatalogHead(ddcApp, expectedVersion);
if (sameBuildChecksum(globalApp)) return currentReplayResult();
if (sameBuildDifferentChecksum(globalApp)) throw conflict;
replaceCiOwnedFactsOnly(globalApp, canonical);
assertNoWritesToTenantApplicationRolePermissionFieldRule();
updateCatalogHeadAndAudit(service, counts);
```

- After this file: CI报告controller/IT测试GREEN。

#### File 6 — `DELETE egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-contract/src/main/java/top/egon/cola/platform/rbac3/contract/manifest`

- Purpose: 完整删除旧Manifest生产/测试生命周期。
- Symbols: all files under exact packages。
- Why now: CI report已替代全部消费者。
- Contract/signature changes: 无旧alias、无submit/activate/impact/history。
- Implementation pseudocode:

```text
delete exact four package trees
remove imports/beans/tests/routes referencing them
rg must return zero ResourceManifest/ManifestController/Rbac3ManifestReporter production symbols
```

- After this file: source absence guard通过，模块仍GREEN。

- Verification command: `mvn -f egon-cola-platforms/pom.xml -pl egon-cola-platform-rbac3/egon-cola-platform-rbac3-contract,egon-cola-platform-rbac3/egon-cola-platform-rbac3-starter,egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin -am -Dsurefire.failIfNoSpecifiedTests=false -Dtest=CiResourceReportControllerTest,CiResourceReportServiceIT,ResourceCrudControllerTest,FieldCrudControllerTest test`
- Expected result: exit 0；报告安全/幂等/rollback/zero-grant和CRUD/tree断言通过；Manifest source scan为0。
- Completion criteria: REQ-010/013–016/019/024/026后端部分完成；无size/rate实现。
- Rollback: 作为单commit revert仅适用于V7未部署；部署后只能forward-fix，不能恢复Manifest表。
- Commit: `feat(rbac3): replace manifests with global CI resource reporting`

### Step 8 — 切换 About、common HTTP 契约和 USER 当前主体访问

- Requirements: `REQ-006`, `REQ-008`, `REQ-017`, `REQ-019`, `REQ-020`, `REQ-023`, `REQ-024`
- Dependencies: `Steps 3,4,6,7`
- Observable outcome: `/api/v1/auth/about`返回最小授权上下文；旧bootstrap 404；RBAC JSON统一common records；所有IdP/RBAC USER Controller不再声明`@AuthenticationPrincipal`，SERVICE endpoint例外保留。
- Ordered files:

#### File 1 — `CREATE egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-starter/src/test/java/top/egon/cola/platform/rbac3/starter/authorization/Rbac3AboutServiceTest.java`

- Purpose: RED固定About禁止字段、USER principal参数为0和old wrapper为0。
- Symbols: Spec TEST-005/019/020。
- Why now: 所有新后端能力已具备，可一次切消费者。
- Contract/signature changes: SERVICE `ServiceIdentityPrincipal`参数allowlist；OAuth/RPC不套common业务wrapper。
- Implementation pseudocode:

```java
assertAboutJson().contains(user, activeRoles, permissions, fieldPolicies, versions)
  .doesNotContain("apps", "menus", "routes", "actions", "path", "componentKey");
scanUserControllers().assertNoAuthenticationPrincipalParameters();
scanJsonControllers().assertOnlyCommonResultTypes();
```

- After this file: 因Bootstrap/old wrapper/principal参数仍存在RED。

#### File 2 — `CREATE egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-contract/src/main/java/top/egon/cola/platform/rbac3/contract/auth/Rbac3AboutView.java`

- Purpose: 建立API-010并删除flat resource bootstrap。
- Symbols: `Rbac3AboutView`、`Rbac3AboutService.current()`。
- Why now: UserDetails/Contract已稳定。
- Contract/signature changes: permission `system:about:read`；只从同一Snapshot组装。
- Implementation pseudocode:

```java
details = currentRbac3User.require();
return new Rbac3AboutView(minimalUser(details), details.currentApplicationCode(),
  details.activeRoles(), sorted(details.permissions()), details.fieldPolicies(),
  details.landingRouteCode(), details.authVersion(), details.policyVersion());
```

- After this file: About service测试GREEN。

#### File 3 — `RENAME egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/bootstrap/controller/Rbac3AuthBootstrapController.java`

- Purpose: 暴露About且删除平行query path。
- Symbols: `GET /api/v1/auth/about`。
- Why now: Starter service已GREEN。
- Contract/signature changes: old `/bootstrap`无alias。
- Implementation pseudocode:

```java
@GetMapping("/api/v1/auth/about")
ResultRecord<Rbac3AboutView> about() { return success(aboutService.current()); }
```

- After this file: About MockMvc GREEN，old URL 404。

#### File 4 — `DELETE egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/shared/domain/vo/ApiEnvelopeVO.java`

- Purpose: 删除并行HTTP外层并统一错误。
- Symbols: ResultRecord/PageResultRecord/PageQuery。
- Why now: 新controllers已用common。
- Contract/signature changes: OAuth/RPC allowlist不变。
- Implementation pseudocode:

```text
single/list/tree/mutation -> ResultRecord<T>
page -> PageResultRecord<T> and PageQuery
non-2xx -> ResultRecord<Void> with HTTP status, code/status/traceId
```

- After this file: common architecture test接近GREEN。

#### File 5 — `MODIFY egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-admin/src/main/java/top/egon/cola/platform/idp/admin/identity/controller/IdentityUserController.java`

- Purpose: 移除USER principal transport参数并保持审计/tenant来源可信。
- Symbols: 所有`@AuthenticationPrincipal IdentityPrincipal/CurrentRbac3Principal`。
- Why now: 两个accessor已存在。
- Contract/signature changes: SERVICE controllers `InternalAuthorizationController/ParticipationController/CiResourceReportController`保留ServiceIdentityPrincipal例外。
- Implementation pseudocode:

```java
controller.method(dto) -> service.method(dto); // no USER principal parameter
service.method(dto) {
  actor = currentIdentityOrRbac3User.require();
  validateTenantAndAudit(actor);
  ...
}
```

- After this file: metadata/architecture测试GREEN，业务controller测试同步更新。

- Verification command: `mvn -f egon-cola-platforms/pom.xml -pl egon-cola-platform-idp/egon-cola-platform-idp-admin,egon-cola-platform-rbac3/egon-cola-platform-rbac3-contract,egon-cola-platform-rbac3/egon-cola-platform-rbac3-starter,egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin -am -Dsurefire.failIfNoSpecifiedTests=false -Dtest=Rbac3AboutServiceTest,ControllerRequestParameterMetadataTest,CommonHttpContractArchitectureTest test`
- Expected result: exit 0；old bootstrap/wrapper/principal source guards为0，SERVICE allowlist保留。
- Completion criteria: REQ-008/020/023及HTTP迁移完成；所有现有controller tests编译通过。
- Rollback: 后端/Contract必须整commit回退；不能只恢复旧前端wrapper或Bootstrap。
- Commit: `refactor(rbac3): switch user APIs to about and common result contracts`

### Step 9 — 发布本地资源 Registry、About 客户端和前端 Guards

- Requirements: `REQ-009`, `REQ-011`, `REQ-013`, `REQ-020`, `REQ-021`, `REQ-022`, `REQ-023`
- Dependencies: `Step 8`
- Observable outcome: React SDK只消费About授权事实和本地registry，支持递归MENU/ROUTE、ACTION、FIELD；unknown/denied fail closed；SDK无report export。
- Ordered files:

#### File 1 — `MODIFY egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-react-sdk/src/types.test.ts`

- Purpose: RED锁定About、registry校验和guards。
- Symbols: recursive tree、hidden/deep link、getField、common result parser。
- Why now: 后端contract已确定。
- Contract/signature changes: JSON无resource catalog；browser无report function。
- Implementation pseudocode:

```ts
expect(buildNavigation(registry, about.permissions)).toEqual(filteredTree)
expect(resolveRoute(hiddenAuthorizedPath)).toBeAllowed()
expect(resolveRoute(deniedPath)).toBeForbidden()
expect(getField('risk').level).toBe('MASKED_READ')
expect(Object.keys(publicExports)).not.toContain('reportFrontendResources')
```

- After this file: 因新types/registry/hooks缺失RED。

#### File 2 — `MODIFY egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-react-sdk/src/types.ts`

- Purpose: 切About和common Result/PageResult解析。
- Symbols: `Rbac3AboutView`、`request/requestPage`、READY/error states。
- Why now: tests先固定网络状态。
- Contract/signature changes: 删除Bootstrap apps/menus/routes/actions和旧ApiEnvelope分支。
- Implementation pseudocode:

```ts
const result = await request<ResultRecord<Rbac3AboutView>>('/api/v1/auth/about')
if (!result.success || !result.data) throw CommonApiError(result)
dispatch({ type: 'ABOUT_SUCCEEDED', about: result.data })
```

- After this file: provider/common parser testsGREEN。

#### File 3 — `CREATE egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-react-sdk/src/registry/FrontendResourceRegistry.ts`

- Purpose: 实现本地Composite和fail-closed guards。
- Symbols: registry types/validator/hooks/components。
- Why now: About provider已GREEN。
- Contract/signature changes: MENU/ROUTE父链无环深度<=20；ACTION/FIELD引用校验；component只本地绑定。
- Implementation pseudocode:

```ts
validateUniqueCodesAndAcyclicParents(definitions)
visible(node) = hasOwnPermission(node, about) && visibleChildren(node).length > 0
routeGuard = aboutReady ? permissionAllowed(route.permission) : loadingOrDenied
fieldAccess = policyMap[fieldCode] ?? NONE
```

- After this file: registry/guard testsGREEN。

#### File 4 — `MODIFY egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-react-sdk/package.json`

- Purpose: 构建/导出新SDK，不暴露Node报告子路径。
- Symbols: exports/build config。
- Why now: public API已稳定。
- Contract/signature changes: 只保留browser-safe根export。
- Implementation pseudocode:

```text
build ESM/types from src/index.ts
assert dist has guards/registry runtime only
assert no report endpoint/scope/token acquisition code in dist
```

- After this file: SDK test/build GREEN。

- Verification command: `npm test && npm run build` in `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-react-sdk`
- Expected result: exit 0；recursive/route/action/field/About tests通过，dist无report export。
- Completion criteria: SDK覆盖REQ-009/011/013/020–023；无browser report。
- Rollback: revert SDK commit；需与Step 11 Admin Web consumer保持版本一致。
- Commit: `feat(rbac3-web): add local resource registry and authorization guards`

### Step 10 — 扩展 Admin Web Shared 递归导航

- Requirements: `REQ-021`
- Dependencies: `Step 9` types
- Observable outcome: Shared Desktop Menu和Drawer用同一children树递归渲染；叶子navigate、最长path高亮、父节点只展开；现有扁平消费者兼容。
- Ordered files:

#### File 1 — `MODIFY egon-cola-platforms/egon-cola-platform-admin-web-shared/src/layout/EnterpriseLayout.test.tsx`

- Purpose: RED覆盖三级树、Drawer、keyboard、flat compatibility。
- Symbols: nested fixtures、longest path selection。
- Why now: Shared API行为先行。
- Contract/signature changes: `children/path`均可选但ROUTE leaf必须path。
- Implementation pseudocode:

```tsx
render(<EnterpriseLayout navigation={threeLevelTree} />)
expand('IAM'); expand('Authorization'); click('Roles')
expect(navigate).toHaveBeenCalledWith('/iam/roles')
expect(flatLegacyItem).toRemainNavigable()
```

- After this file: 现有Header扁平实现导致RED。

#### File 2 — `MODIFY egon-cola-platforms/egon-cola-platform-admin-web-shared/src/layout/types.ts`, `MODIFY egon-cola-platforms/egon-cola-platform-admin-web-shared/src/layout/EnterpriseHeader.tsx`, `MODIFY egon-cola-platforms/egon-cola-platform-admin-web-shared/src/layout/EnterpriseLayout.tsx`, `MODIFY egon-cola-platforms/egon-cola-platform-admin-web-shared/src/index.ts`

- Purpose: 最小递归Composite渲染和选择索引。
- Symbols: `EnterpriseNavigationItem.children`、recursive menu builder/key index。
- Why now: RED已固定兼容边界。
- Contract/signature changes: parent无path时只展开；desktop/drawer共享tree。
- Implementation pseudocode:

```ts
function toMenuItem(node) { return node.children?.length ? submenu(node, node.children.map(toMenuItem)) : leaf(node.path) }
leafIndex = flattenLeaves(tree)
selected = longestPathPrefix(location.pathname, leafIndex)
```

- After this file: Shared tests/typecheck GREEN。

- Verification command: `npm test && npm run typecheck` in `egon-cola-platforms/egon-cola-platform-admin-web-shared`；build在独立node_modules副本或重新install后执行，避免postbuild影响后续Step。
- Expected result: exit 0；三级菜单/Drawer/flat regression通过。
- Completion criteria: Shared API兼容且满足REQ-021。
- Rollback: revert Shared commit；Admin Web Step 11前不得发布不匹配版本。
- Commit: `feat(admin-web-shared): support recursive enterprise navigation`

### Step 11 — 迁移 RBAC Admin Web IAM 页面并加入 CI-only 报告脚本

- Requirements: `REQ-010`, `REQ-011`, `REQ-013`, `REQ-014`, `REQ-015`, `REQ-019`, `REQ-020`, `REQ-021`, `REQ-022`, `REQ-023`, `REQ-024`, `REQ-026`
- Dependencies: `Steps 7–10`
- Observable outcome: Admin Web使用`/iam/**`、本地递归定义、About/common client；具备global Permission/Resource/Field和TenantApplication CRUD；无Manifest/sync按钮；CI脚本在build后报告且失败阻断；dist不含脚本/scope/secret。
- Ordered files:

#### File 1 — `MODIFY egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin-web/src/app/App.integration.test.tsx`

- Purpose: RED锁定完整导航/CRUD/无sync/CI脚本。
- Symbols: old URL 404、tree/guards、common parser、script HTTP status。
- Why now: backend/SDK/Shared均已可消费。
- Contract/signature changes: CI脚本不依赖React/Vite runtime。
- Implementation pseudocode:

```ts
expect(screen.queryByRole('button', {name:/sync|report/i})).not.toBeInTheDocument()
expect(route('/iam/tenant-applications')).toRenderCrud()
runReportScript({ serviceAt, definitions }); expect(fetch).toHaveBeenCalledWith(API_003)
runReportScript({ response: 409 }).rejects.toBlockRelease()
```

- After this file: 新routes/pages/script不存在RED。

#### File 2 — `CREATE egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin-web/src/app/resourceDefinitions.json`

- Purpose: 同一纯数据定义驱动本地runtime和CI投影。
- Symbols: MENU/ROUTE/ACTION/FIELD definitions、component binding map。
- Why now: SDK registry可用。
- Contract/signature changes: routes统一`/iam/**`，无旧fallback；definitions不含component function。
- Implementation pseudocode:

```ts
definitions = validate(importedJson)
registry = bindComponents(definitions, localComponentMap)
navigation = useNavigationTree(registry)
router = createRoutes(registry.routes.map(route => guardedLocalComponent(route)))
```

- After this file: navigation/router tests部分GREEN。

#### File 3 — `MODIFY egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin-web/src/api/adminApiClient.ts`

- Purpose: 切common Result/PageResult和IAM URL。
- Symbols: request/requestPage/error parsing/query keys。
- Why now: routes已固定。
- Contract/signature changes: 删除ApiEnvelope/DirectoryPage嵌套解析；global catalog key无tenant，tenant entitlement key含tenant。
- Implementation pseudocode:

```ts
data = unwrapResult(await http(url))
page = unwrapPage(await http(url, pageQuery))
resourceKey = ['global-resource-catalog', applicationId]
tenantAppKey = ['tenant-applications', currentTenant, pageQuery]
```

- After this file: API mocks/common testsGREEN。

#### File 4 — `DELETE egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin-web/src/features/application/ManifestDetailPage.tsx`

- Purpose: 落global catalog/TenantApplication/field CRUD并移除Manifest UI。
- Symbols: list/detail/forms/status/query states。
- Why now: API client已迁。
- Contract/signature changes: ResourceCatalog只读显示last CI build/checksum/time，无sync action。
- Implementation pseudocode:

```tsx
<ResourceCatalog filters={globalApplicationFilters} reportMetadataReadOnly />
<TenantApplicationForm applicationSelector={activeGlobalApps} tenantIdHidden />
<FieldDefinitionForm securityFieldsEditableByAdminOnly />
```

- After this file: IAM page testsGREEN。

#### File 5 — `CREATE egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin-web/scripts/report-rbac-resources.mjs`

- Purpose: 提供流水线显式命令并守卫browser bundle。
- Symbols: `test:report`、`report:resources`、`verify:bundle` scripts。
- Why now: definitions和API契约均稳定。
- Contract/signature changes: 从env读取Gateway base URL、bizCode/appCode、buildId、expectedVersion和短期SERVICE AT；不持久化token；不自动取凭据。
- Implementation pseudocode:

```js
definitions = JSON.parse(readFile(resourceDefinitionsPath))
canonical = validateSortAndChecksum(definitions)
response = await fetch(apiUrl(bizCode, appCode), { method:'PUT', headers:{Authorization:`Bearer ${serviceAt}`}, body })
if (!response.ok || !result.success) throw new Error(traceSafeSummary)
scanDistForForbidden(['report-rbac-resources', 'rbac3:resource-catalog:report', serviceAtPatterns])
```

- After this file: Node script testsGREEN；失败exit非0阻断发布。

#### File 6 — `MODIFY egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin-web/package.json`

- Purpose: 对齐SDK/Shared consumer和About provider。
- Symbols: dependency version、provider props、build guard。
- Why now: 页面/脚本均已GREEN。
- Contract/signature changes: runtime import graph不引用`scripts/`；不新增SERVICE env到Vite `import.meta.env`。
- Implementation pseudocode:

```text
mount Rbac3Provider with about client and local registry
mount EnterpriseLayout with filtered recursive navigation
build then execute verify-browser-bundle.mjs only
do not execute report script during dev/start/build
```

- After this file: Admin Web test/typecheck/lint/build和dist guard GREEN。

- Verification command: in Admin Web, `npm run test && npm run test:report && npm run typecheck && npm run lint && VITE_IDP_ISSUER=https://idp.example VITE_IDP_CLIENT_ID=rbac3-admin-web VITE_IDP_AUDIENCE=rbac3-admin npm run build && node scripts/verify-browser-bundle.mjs`
- Expected result: exit 0；无旧route/Manifest/sync；CI脚本单测成功/失败分支通过；dist无SERVICE材料。
- Completion criteria: 所有前端REQ完成；报告没有在browser/startup执行。
- Rollback: Web/SDK/Shared/后端契约需成组回退；CI脚本可停止调用但不能恢复浏览器上报。
- Commit: `feat(rbac3-admin-web): migrate IAM UI and add CI resource reporting`

### Step 12 — 执行最终一致性、回归和发布边界审计

- Requirements: `REQ-001`–`REQ-026`
- Dependencies: `Steps 1–11`
- Observable outcome: 全部聚焦/模块/跨模块/static/migration/frontend gates通过；源码和产物无被禁止能力；生成发布清单但不启动服务。
- Ordered files:

#### File 1 — `CREATE egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/architecture/Rbac3AuthorizationArchitectureTest.java`

- Purpose: 汇总source/package/controller/schema边界守卫。
- Symbols: no Manifest、no old principal/envelope/bootstrap、no DataScope、SERVICE endpoint allowlist、global PO no TenantScopedPO。
- Why now: 只能在所有代码到位后验证。
- Contract/signature changes: 测试规则本身不改变runtime。
- Implementation pseudocode:

```java
assertNoClassesMatching("..manifest..", legacySymbols);
assertNoUserControllerAuthenticationPrincipal();
assertGlobalCatalogPosDoNotExtendTenantScopedPO();
assertNoClassesNamed("DataScope", "*QueryRewriter");
```

- After this file: 若任一旧符号残留则RED并返回所属Step修复。

#### File 2 — `CREATE egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin-web/scripts/verify-rbac3-conformance.mjs`

- Purpose: 汇总前端source/dist/route/report边界。
- Symbols: forbidden symbol/path/import scans。
- Why now: Admin Web产物已稳定。
- Contract/signature changes: 无runtime行为。
- Implementation pseudocode:

```js
assertNoSource(['ManifestDetailPage', '/auth/bootstrap', 'reportFrontendResources', 'system:resource:report'])
assertNoDist(['rbac3:resource-catalog:report', 'report-rbac-resources.mjs'])
assertFileExists('scripts/report-rbac-resources.mjs')
assertNoImportFromSrcToScripts()
```

- After this file: conformance scan可重复执行。

#### File 3 — `MODIFY docs/egon/plan/2026-08-17-15-07-rbac3-annotation-resource-catalog-implementation.md` only during execution status tracking

- Purpose: 每Step完成后记录真实commit/validation，不预写成功。
- Symbols: execution evidence appendix/status。
- Why now: 最终审计需要可追踪commit。
- Contract/signature changes: 文档证据，不改设计。
- Implementation pseudocode:

```text
for each Step record commit hash, exact command, exit code, and validation boundary
record runtime tests as NOT RUN until user starts environment
do not alter accepted Spec behavior during execution
```

- After this file: Plan证据与真实执行一致。

- Verification command: 依次运行§8全部gate；最后`python3 .agents/skills/egon-coding-writing-spec/scripts/validate_spec.py --strict <spec>`、`python3 .agents/skills/egon-coding-writing-plan/scripts/validate_plan.py --strict <plan>`和`git diff --check`。
- Expected result: 所有命令exit 0；runtime/live gate明确未自动执行。
- Completion criteria: Traceability无缺口，所有Step有一个commit，用户请求范围外文件无diff。
- Rollback: 仅回退Step 12测试/脚本/证据；行为失败必须回所属Step修复，不能删除守卫掩盖问题。
- Commit: `test(rbac3): audit authorization redesign conformance`

## 8. Test, Validation, and Quality Gates

| Gate/order | Command or method | Scope | Expected result | Failure returns to | Requirements |
| --- | --- | --- | --- | --- | --- |
| Step 1 RED/GREEN | DDC focused Maven command in Step 1 | DDC Service+V9 | RED missing V9/query；GREEN exit0 | Step 1 | `REQ-025` |
| Step 2 RED/GREEN | Gateway/IdP focused command | online state | 401/503/skip/refresh矩阵 | Step 2 | `REQ-001`–`REQ-003`,`REQ-018` |
| Step 3 RED/GREEN | IdP RPC/RBAC user focused command | identity boundary | proto/provider/client/partial | Step 3 | `REQ-008`,`REQ-017` |
| Step 4 RED/GREEN | RBAC Contract/Starter/Admin focused command | UserDetails/method | single principal/interceptor | Step 4 | `REQ-004`–`REQ-008` |
| Step 5 RED/GREEN | Starter field tests | Jackson | null/mask/raw/no leak | Step 5 | `REQ-009`,`REQ-022` |
| Step 6 migration | RBAC V7 focused command | PostgreSQL schema/JPA/eligibility | global/tenant split | Step 6 | `REQ-007`,`REQ-016`,`REQ-024`,`REQ-026` |
| Step 7 report/CRUD | Admin focused command | API-003/011/IAM | security/idempotency/zero grant | Step 7 | `REQ-010`,`REQ-013`–`REQ-016`,`REQ-019`,`REQ-024`,`REQ-026` |
| Step 8 HTTP | IdP/RBAC controller architecture tests | About/common/principal | old symbols/params 0 | Step 8 | `REQ-008`,`REQ-020`,`REQ-023` |
| Step 9 SDK | `npm test && npm run build` | React SDK | exit0/no report export | Step 9 | `REQ-009`,`REQ-011`,`REQ-013`,`REQ-020`–`REQ-023` |
| Step 10 Shared | `npm test && npm run typecheck` | Shared | recursive/legacy GREEN | Step 10 | `REQ-021` |
| Step 11 Web | Admin Web test/typecheck/lint/build/dist guard | UI+CI script | exit0/no secret/report bundle | Step 11 | frontend requirements |
| Java cross-module | Gateway/IdP/RBAC/DDC commands from §6.2 | platform reactor | exit0 | owning Step | all backend |
| Static | `git diff --check` + `rg`/architecture/conformance guards | repository | no findings | owning Step | all |
| Spec/Plan | strict validators | docs | PASS | Spec/Plan correction | all |
| Manual/runtime | 用户启动真实Gateway/IdP/RBAC/DDC/Redis/PostgreSQL后执行§9 checklist | live topology | observable outcomes recorded | owning deployment Step | all runtime |

## 9. Migration, Compatibility, Rollout, and Rollback

1. 预发布先扫描DDC重复`biz_code/app_code`；重复appCode由owner加稳定前/后缀并更新调用方。随后发布DDC V9和Service，验证两个单列UK；V1–V8不可修改。
2. 先发布IdP internal RT status和Gateway adapter capability，再启用Gateway protected USER online check；确认RT Cookie Path `/`、public/SERVICE route policy和503容量。
3. 发布IdP RPC/accessor与RBAC UserDetails/field PEP；在V7前完成所有编译消费者，避免schema与旧PO混跑。
4. 维护窗口停旧RBAC Admin writers并备份；执行唯一RBAC V7，清空旧authorization/catalog facts、删除Manifest、全局化catalog、创建TenantApplication。旧Admin和新schema不可滚动共存。
5. 同发布单元发布RBAC Contract/Starter/Admin/About/common HTTP和React SDK/Shared/Admin Web；清Snapshot Redis keys。旧Bootstrap/old wrapper/old URLs无兼容alias。
6. 上线每个应用前，流水线build/test成功后取得短期SERVICE AT，运行`report:resources`；API-003非2xx或`success=false`阻断发布。不要在app启动或浏览器中调用。
7. 报告成功后IAM管理员审核PENDING catalog/field安全属性，再按购买事实显式创建TenantApplication/角色/RolePermission；CI报告不代替这些动作。
8. 回滚：V9/V7部署后只能备份恢复或新V10/V8 forward-fix；不得改历史脚本。Gateway online check回退会恢复最多5分钟撤销窗口并告警。About/common/UserDetails/Web必须整组回退；不得恢复Session、Manifest或tenant资源副本。

Post-deploy只读检查：DDC单列UK存在；RBAC catalog无tenant列、TenantApplication存在、Manifest不存在；一次同build/checksum重放为idempotent；一个无TenantApplication租户的角色不进About；bundle source map/string scan无SERVICE scope/token/report script。真实检查由用户启动环境后执行，本Plan不声称已完成。

## 10. Requirement-to-Step Traceability Matrix

| Requirement | Effective Spec section | Steps | Files | Tests/gates | Completion evidence |
| --- | --- | --- | --- | --- | --- |
| `REQ-001` | §4/§7.3.2 | 2 | Gateway SPI/chain、TokenFacade、status client | TEST-001/003 | revoked RT下一请求401 |
| `REQ-002` | §4/§7.3.2 | 2 | recovery provider/chain | TEST-002 | expired AT刷新一次继续 |
| `REQ-003` | §4/§9.2.1 | 2 | endpoint policy/status API | TEST-003 | public/SERVICE跳过 |
| `REQ-004` | §4/§9.3 | 4 | annotations/manager | TEST-004 | AND/403一致 |
| `REQ-005` | §4/§7.3.3 | 4 | filter/auto-config/AuthorizationService | TEST-004/005 | 单一method PEP |
| `REQ-006` | §4/§10 | 4,8 | UserDetails/loader/About | TEST-005/020 | principal和About内容 |
| `REQ-007` | §4/§11 | 4,6 | active roles/TenantApplication/eligibility | TEST-006/022 | 未激活/无资格权限缺失 |
| `REQ-008` | §4 | 3,4,8 | CurrentIdentity/CurrentRbac3User/controllers | TEST-005 | USER controller显式principal为0 |
| `REQ-009` | §4/§7.3.4 | 5,9 | Jackson/SDK field hooks | TEST-007/009 | null/mask/raw和列展示 |
| `REQ-010` | §4/§9/§12 | 5,7,11 | field CRUD/PEP/pages | TEST-008 | CRUD与安全字段边界 |
| `REQ-011` | §4/§9.4 | 9,11 | getField/guards/pages | TEST-009 | NONE隐藏/MASKED展示 |
| `REQ-012` | §4 | 6,7,12 | DataRule/repository/guard | TEST-010 | 无DataScope执行器 |
| `REQ-013` | §4/§7.3.5 | 7,9,11 | registry/report/catalog | TEST-016/017 | 一份声明驱动UI/CI |
| `REQ-014` | §4/§9.2.3 | 7,11,12 | CI endpoint/script/Manifest delete | TEST-011/012 | CI-only且无Manifest |
| `REQ-015` | §4/§7.3.7 | 7,11 | report service/script | TEST-011/013 | 冒充零写/幂等/零赋权 |
| `REQ-016` | §4/§11 | 1,3,6,7,11 | DDC/Global App/TenantApp | TEST-013/022/023 | 所有权分层 |
| `REQ-017` | §4/§9.2.8 | 3,8 | Identity RPC/User list | TEST-014 | no profile persistence |
| `REQ-018` | §4 | 2 | TokenFacade/Gateway | TEST-001 | logout下一请求401 |
| `REQ-019` | §4/§12 | 6,7,8,11 | IAM controllers/router/pages | TEST-015 | 新URL/旧404 |
| `REQ-020` | §4/§9.2.4 | 8,9,11 | About contract/provider | TEST-020 | JSON无资源树 |
| `REQ-021` | §4/§12 | 9,10,11 | registry/router/shared layout | TEST-017/018 | 递归/hidden/deep-link |
| `REQ-022` | §4/§7.3.4 | 5,9,11 | Action/Field guards+Jackson | TEST-007/009/017 | UI与后端双PEP |
| `REQ-023` | §4/§9 | 7,8,9,11 | common records/clients | TEST-019/020 | old envelope为0 |
| `REQ-024` | §4/§11 | 6,7,8,11,12 | V7/deletes/source guard | TEST-012/021 | source/schema/UI无Manifest |
| `REQ-025` | §4/§11.4 | 1 | DDC V9/repo/service | TEST-023 | 单列UK/跨BIZ409 |
| `REQ-026` | §4/§11 | 6,7,11,12 | global catalog/TenantApp/snapshot | TEST-011/021/022 | report无tenant/资格生效 |

## 11. Risks, Blockers, and User Decisions

| ID | Risk or decision | Impacted Steps/files | Evidence | Owner | Status/action |
| --- | --- | --- | --- | --- | --- |
| `RISK-001` | RT在线调用放大IdP依赖 | Step 2 | Spec RISK-001 | Platform ops | Accepted；容量测试，503 fail closed，不加正向缓存 |
| `RISK-002` | DDC历史重复appCode使V9失败 | Step 1 | Spec RISK-012 | DDC owners | Accepted precondition；上线前改code |
| `RISK-003` | V7清空旧授权图不可应用回滚 | Step 6 | 用户允许不保旧数据/Spec RISK-013 | User/DB ops | Accepted；备份/维护窗口/forward-fix |
| `RISK-004` | API注解资源不由本次前端CI报告发现 | Steps 4,7 | Spec RISK-002 | RBAC owner | Accepted scope；API用注解PEP+CRUD维护 |
| `RISK-005` | Shared递归变更影响其他Admin Web | Step 10 | Shared为公共包 | Frontend owner | Mitigated；optional children+legacy tests |
| `RISK-006` | CI SERVICE身份配置错APP | Steps 7,11 | sourceBiz/sourceApp现有claims | Pipeline owner | Mitigated；path逐项匹配+DDC校验+PENDING/zero grant |
| `RISK-007` | common破坏式契约造成旧consumer失败 | Steps 8,9,11 | user允许breaking | Release owner | Accepted；整组发布，无双写 |
| `BLOCK-001` | live库若必须保留现有RBAC授权数据 | Step 6 | 当前Spec明确不保旧数据，未连接live DB | User | Closed by current decision；若条件改变必须停止实施回Spec |

没有未解决的设计问题或越出用户范围的新增任务。

## 12. Review and Acceptance

### 12.1 Original requirement fidelity

Plan逐项覆盖REQ-001–026，并按最新决定把资源上报固定为CI SERVICE、global catalog无tenant、TenantApplication独立、DDC codes单列唯一、无1MiB限制和无限流。Plan没有添加用户中台、DataScope执行、Manifest替代模块、浏览器/启动时上报或新Token。

### 12.2 Spec consistency

接口、sourceType、V7/V9、SERVICE scope、About字段、common records、UI无sync、CI失败阻断和发布顺序均直接来自Accepted Spec。四个Plan Clarification只收敛源码已存在的小实现边界：DDC update不可改code、复用RequiresServiceScope、JSON纯数据声明和V7破坏式清空；没有新增业务决策。

### 12.3 Repository executability

当前branch/commit/dirty state、POM模块、真实package、迁移序列和npm scripts已检查。步骤遵守Contract→实现→consumer、RED→GREEN、schema+PO同commit和每Step一次path-limited commit；不启动服务，不改历史迁移或generated Java。

### 12.4 Test and release completeness

每个行为都有RED原因和GREEN命令；包含unit/contract/controller/JPA/Testcontainers/migration/frontend/component/source/dist/cross-module gates。live Redis/PostgreSQL/DDC/JWK/流水线验证明确归用户启动后的manual gate，Plan不把静态或模块测试冒充运行证明。

### 12.5 Final verdict

`PASS — Ready for user review`
