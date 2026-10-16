# 星垣完整接入：隔离运行、真实准备与验收规范附录

| Field | Value |
| --- | --- |
| Document | 2026-10-11-10-26-xingyuan-native-full-integration-runtime.md |
| Template Contract | 主 Spec Template Version 7；本文件为其规范附录，不另立 RFC 生命周期 |
| Status | Accepted；主Spec规范附录，用户本轮confirmed批准DEC-007/008及整套文档；实施/运行尚未进行 |
| Type | Runtime / Integration Contract Appendix |
| Complexity | Complex：空库准备、跨进程事务恢复、资源隔离会话、真实授权与生成产品验收 |
| Created / Updated | 2026-10-11 10:26 Asia/Shanghai / 2026-10-11 15:17 Asia/Shanghai |
| Owner / Repository | mario / Egon-COLA |
| Baseline Revision | main @ b074c6673；同组设计文件仍在协作编辑 |
| Normative Parent | [星垣各平台与非 open Archetype 完整接入设计](2026-10-11-10-26-xingyuan-native-full-integration.md) §7–§18、REQ-001–013、TEST-001–036；重点运行 TEST-020–026/030/032/034–036 |
| Amends / Supersedes | None；补齐主 Spec 的 test-local CLI、provision、profile、真实运行合同 |
| Depends On | 主 Spec DEC-001–006 已批准部分及 DEC-007/008 待批准提案；[授柄、鉴神自身加固](2026-10-10-12-36-tianquan-security-self-hardening.md) §7–§10/§14–§16；[OAuth 当前代码修订](2026-10-08-10-18-oauth-current-code-alignment.md) 已批准执行部分 |
| Related Plans | 主 Spec 最终关联的接入 Plan；不重跑已完成授柄、鉴神自身旧 Step |
| Change Surface | 隔离测试 CLI、真实 test-only 数据准备、同源 TLS/进程配置、逐资源 CLI token 与浏览器/RPC live gate |
| Affected Chapters | 主 Spec §8/§9 接口、§10 输入模型、§11 事务/新测试数据、§14–§16 验证/安全/运行；治理、角色与 MC 继承主 Spec |
| Source Requirement | 用户允许新隔离本机数据与 Java/Node/Edge/浏览器，禁止 Docker；批准每资源独立 AT/CONFIDENTIAL OIDC Client、同一 BFF Session、显式目录登记/真实字段规则写入，以及全 USER scope∧PDP 和签名 route requiredOAuthScopes；旧 Grant 不自动扩权 |

**规范状态不代表实现状态。** 本轮只写 Spec/Plan，未启动任何平台、浏览器、证书代理或写数据库。下述新文件、CLI、Bean 是批准方案的设计目标；当前不存在的能力不得在执行报告中当作已实现。主 Spec 的字段规则新增 owner 必须实现并验收后才可通过正向 WRITE case。

## R1. 已确认事实、最小方案与职责

### R1.1 证据与边界

| ID | 当前来源 | 事实 | 设计 |
| --- | --- | --- | --- |
| RT-EVD-001 | 本机只读 pg_isready/Redis PING | 127.0.0.1:5432 accepting connections；6379 NOAUTH | 复用设施，不启动/关闭 PG、Redis 或容器；凭据只取用户保护文件 |
| RT-EVD-002 | scripts/unified-xingyuan/start-local-stack.sh、lib/common.sh | 直接 Java/Vite；旧脚本不管理 Edge，默认181xx，绑定旧初始化前提 | 新隔离生命周期入口，不修改或清理旧 runtime |
| RT-EVD-003 | OAuthPersistenceConfiguration / OAuthManagedDdlComponent | 单 PRIMARY、tianquan_oauth、当前 manifest、DDL-before-logical-datasource、原 transactionManager | 不另造 DDL runner，不重写/repair 历史 SQL |
| RT-EVD-004 | OAuthRealmRepositoryImpl / OAuthSigningKeyRepositoryImpl | 真实 MP tenant scope、插入version0、更新CAS、MDC finally恢复 | 初始 realm/key fixture 由真实 repositories + cipher +事务写入新 DB |
| RT-EVD-005 | OAuthSecurityProjectionComponent / OAuthClientServiceImpl | CLIENT_CREATED 需要已存在 REALM generation，CLIENT generation/Outbox由原服务建立 | 先真实 recover(realm)，后真实 Client create；不直接 insert client bypass |
| RT-EVD-006 | EdgeSecurityConfiguration / EdgeSessionController | 现有同源 CORS/CSRF、Secure __Host cookie；旧固定单 resource | 按已批准主 Spec 每 resource registration/token、同 canonical identity Session；不放宽 aud |
| RT-EVD-007 | Portal/Yuheng package.json、本机 node_modules | Portal 无 Playwright；玉衡已有 @playwright/test 1.62.0，Chromium 已缓存 | 使用玉衡现有 runner，不加 Portal 测试依赖、不下载新 runner |
| RT-EVD-008 | JA FieldPolicyMerger、field definition 与原 PO/reader | defaultAccess 只 NONE/MASKED_READ/READ；writable 是上限；有效 role rules为空→无WRITE | 正 WRITE 必须调用已批准的真实 replaceFieldRules 并等角色/权限投影，不能用 defaultAccess=WRITE |
| RT-EVD-009 | 原 Development bootstrap / main CLI | 用户、租户、client/resource/role可由真实 owner 准备；空库最初 realm/JWK 没有现成 bootstrap | test-only 最小装配补初始事实，不加生产 HTTP bootstrap |
| RT-EVD-010 | 原 live IT / fixture E2E | tid 与旧撤销假设过时，page.route 不跑真实 AS | 更新旧断言，另设真实 Code/SSO/native gate |

### R1.2 精确文件清单及必要性

路径别名与主 Spec相同，另设 SA=S/egon-cola-tianquan-shoubing-admin、JA=J/egon-cola-tianquan-jianshen-admin、YW=Y/yuheng-admin-web、TA=egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin。下面声明的是源码文件；runtime keys/证书/结果只生成在 target，禁止提交。

| Change | Exact path | 职责 / Add-Keep-Merge 判定 |
| --- | --- | --- |
| CREATE | scripts/unified-xingyuan/isolated-integration.sh | 9个原子子命令入口；现有脚本触及旧状态且无 Edge/TLS 生命周期，Add |
| CREATE | scripts/unified-xingyuan/lib/isolated_integration.py | 原嵌入 Python 实现的私有标准库模块；复用目标/PID/归属校验，独立调用同样 preflight；无新依赖/公开命令 |
| CREATE | scripts/unified-xingyuan/lib/isolated-integration.sh | 目标/PID/文件守卫及固定进程参数；复用原 common helpers，不新建业务服务，Add |
| CREATE | scripts/regression/test-isolated-integration-contract.sh | /tmp 假 CLI recorder 验证误写守卫/语法/命令选择；无DB/Redis/socket，Add |
| CREATE | SA/src/test/java/top/egon/cola/platform/tianquan/shoubing/admin/integration/IsolatedOAuthProvisioningIT.java | opt-in 入口、写库前 target gate、分阶段最小真实 Spring 装配；技术 JUnit 类，Add |
| CREATE | SA/src/test/java/top/egon/cola/platform/tianquan/shoubing/admin/integration/IsolatedOAuthProvisioningComponent.java | 实际身份/租户/realm/key/client/resource/grant事务与恢复；一个 managed Component，Add |
| CREATE | TA/src/test/java/top/egon/cola/component/tianshu/admin/integration/IsolatedTianshuCatalogProvisioningIT.java | 获批冷启动：仅显式opt-in的本模块NONE夹具经原DdcBizService/DdcAppService建立真实基础目录，默认无设施；不加Admin依赖 |
| CREATE | JA/src/test/java/top/egon/cola/platform/tianquan/jianshen/admin/integration/IsolatedAuthorizationProvisioningIT.java | 独立 JA owner 的真实catalog review/role/resource/field-rule/投影准备，不引入SA→JA test依赖，Add |
| CREATE | YW/playwright.integration.config.ts | 从已有 Playwright1.62.0解析；testDir只选真实live场景，无自动webServer，Add |
| MODIFY | YW/playwright.config.ts | 只排除 e2e/integration 的默认发现；保留既有 13 fixture cases/webServer，真实场景只由专用 config 显式运行，必要调用方兼容 |
| CREATE | YW/e2e/integration/xingyuan-identity-integration.spec.ts | Portal/四管理端真实 Code、逐资源 CLI AT、SSO、step-up/logout、安全 UI，Add |
| CREATE | Y/yuheng-test/yuheng-test-suite/src/test/java/top/egon/cola/component/yuheng/test/integration/XingyuanResourceAuthorizationIT.java | 真实逐资源issuer/aud/scope/租户/撤销与HTTP拒绝验证，Add |
| CREATE | 同目录/XingyuanNativeAuthenticationIT.java | 从真实generated artifact发起native RPC、实际注册/HMAC/credential/cache与finally证据，Add |
| MODIFY | 同模块既有tianquan/shoubing/UnifiedIdentityTenantSwitchIT.java | 当前tenant_id与resource绑定，不保留tid假设，Keep |
| MODIFY | 同模块既有tianquan/shoubing/UnifiedIdentityRevocationIT.java | 原当前AS authorization-family撤销，不保留旧logout后AT有效假设，Keep |
| MODIFY | docs/runbooks/unified-identity-local.md | 新隔离入口、材料/恢复/保留边界，Keep |
| MODIFY | docs/operations/unified-identity-mcp-local-runbook.md | 新协议live gate与未验证边界，Keep |
| MODIFY | scripts/README.md | 生命周期脚本归属及无Docker限制，Keep |

| CREATE | egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin/src/main/java/top/egon/cola/platform/tianquan/jianshen/admin/bootstrap/domain/dto/ResolvedPlatformBootstrapCommandDTO.java | §12.49获批修订：限定四个平台及真实目录引用的初始化命令 |

| MODIFY | egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin/src/main/java/top/egon/cola/platform/tianquan/jianshen/admin/bootstrap/repository/DevelopmentBootstrapPort.java | §12.49获批修订：兼容旧 bootstrap 的 additive resolved operation |

| CREATE | egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin/src/test/java/top/egon/cola/platform/tianquan/jianshen/admin/bootstrap/ResolvedPlatformBootstrapContractTest.java | §12.49获批修订：真实 bootstrap owner 的受控行为 RED/GREEN |

| MODIFY | egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin/src/main/java/top/egon/cola/platform/tianquan/jianshen/admin/bootstrap/repository/jpa/JpaDevelopmentTopologyBootstrapRepository.java | §12.49获批修订：已知初始行锁/CAS绑定及完整真实新行构造 |

Provision 顶层 Java 文件限定上述 **4个**：SA IT、SA Component、TA IT、JA IT；不再增独立 BootstrapService、Coordinator、Registry、Handler hierarchy。天枢基础目录先经TA夹具的原服务建立，JA现本地 bootstrap owner 通过真实目录复查后仅绑定无业务依赖的系统初始行；缺当前拓扑行时用真实引用沿原职责创建，仅允许已批准 §12.50 的无业务依赖、未修改系统初始行绑定当前应用编码，同时保留应用 ID 与原权限所有权；已使用数据不变。新 resolved operation 保留旧 bootstrap 调用兼容，无新HTTP/RPC/Flyway/依赖；角色、准入、Business grant与快照激活仍经原owner。后续目录登记/field-rule写入使用主 Spec native附录中的生产真实 owner/CLI，不复制一套 test DAO。技术 DTO/VO/Result/验证组、最小 TestConfiguration、报告 Converter 如确有不同语义，作为具名嵌套类型放上述文件，不能以“字段少”把可变 carrier 改为 record。

原 SAS Converter 两个生成 @Component 的 Bean 名为 sasAuthorizationConverterImpl/sasRegisteredClientConverterImpl；§12.59 的独立三路径 DI 纠正使 TenantAuthorizationServiceImpl/TenantRegisteredClientRepository 的三处 Qualifier 与实际名字一致。无新 Bean 别名、生产接口/依赖/业务规则或迁移；真实无数据库 Spring 装配测试独立验证。

### R1.3 Java类型、Bean、DI与验证

SA/JA/TA 的显式 NONE context 使用 Spring 原生 AnnotationBeanNameGenerator，使导入的原组件 Bean 名与主应用 component scan 一致；不扩大扫描或新建代理 owner。原生审核输入先绑定四实际 artifact 的 buildId、catalog SHA 及 application namespace，再验证独立权限 mapping、字段上限和精确角色清单；formal token-index 只选择对应真实 Tianshu PUBLIC USER Client 的未到期私有输入，后续服务仍执行真实 Bearer 验证，文件检查本身不证明授权成功。

| Type | 后缀/表示 | Bean及依赖 | 验证/转换/日志 |
| --- | --- | --- | --- |
| IsolatedOAuthProvisioningIT | IT技术测试；非业务Bean | JUnit入口控制两最小context；不扫描main；装配边界可按exact name取component | env opt-in；Guard先于DataSource/DDL；元数据错误不输出Throwable敏感message |
| IsolatedOAuthProvisioningComponent | Component行为 | @Component("isolatedOAuthProvisioningComponent")、@Slf4j、@RequiredArgsConstructor；所有final依赖@Qualifier | @Validated；方法入参@Valid；原生constraint+Common ValidationUtils仅用于nonproxy manual路径 |
| IsolatedTianshuCatalogProvisioningIT | IT技术测试；天枢本模块原owner | NONE最小TestConfiguration、原MP/managed DDL与service；无main/provider/scheduler | manifest/OID/owner/真实actor/tenant与阶段配方，默认无I/O |
| IsolatedAuthorizationProvisioningIT | IT技术测试；JA内独立owner | Spring/JUnit受管参数通过exact qualifiers注入真实services；无handwritten业务注入ctor | catalog、role、field DTO原注解/版本约束；审计/Outbox原机制 |
| IsolatedIntegrationManifestDTO及嵌套DatabaseTargetDTO/ProcessTargetDTO/CredentialReferenceDTO | DTO可变class，标准@Data/@NoArgsConstructor/@AllArgsConstructor/@Accessors(chain=true)/@Builder；集合防御复制 | 普通输入对象不加@RequiredArgsConstructor；不另建Bean | Strict Jackson duplicate/unknown/trailing校验；@Valid list；仅保护文件引用，无password/token字面值 |
| IsolatedProvisioningBO | BO普通class，标准Lombok/Builder | component内部已验证阶段结果；secret不进入报告BO | Default/Prepare/Provision/Runtime验证组按边界；@ToString.Exclude/必要@JsonIgnore排除secret临时值 |
| IsolatedIntegrationResult | Result技术输出class，标准Lombok/Builder | 成功/错误/steps/计数/路径均按R3 schema | managed Jackson，@JsonInclude(ALWAYS)保留必需error=null/空数组/0；Instant字段@JsonFormat(shape=STRING)UTC ISO，Duration；不受globalNON_EMPTY削弱 |
| IsolatedProvisioningResultConverter | Converter嵌套MapStruct interface | @Mapper(componentModel="spring",implementationName="IsolatedProvisioningResultConverterImpl",unmappedTargetPolicy=ERROR)，Bean isolatedProvisioningResultConverterImpl | extends BaseForwardConverter<IsolatedProvisioningBO,IsolatedIntegrationResult>；单向排除密钥/密码/token/subject、原secret VO不直接serialize |
| IsolatedOAuthProvisioningConfiguration | Configuration技术嵌套class | @TestConfiguration("isolatedOAuthProvisioningConfiguration",proxyBeanMethods=false)，@Bean均具名 | 只导入本阶段真实owner/technical autoconfig；不注册listener/securitychain/生产runner/scheduler |
| IsolatedIntegrationValidationGroups | 技术验证组嵌套接口 | Prepare/Provision/Runtime均是marker，不是业务strategy | 通用语法Default；可复用输入组不削弱约束；实体仍走原MP Insert/Update/Persisted |

SA Component分context装配；Phase A仅identity/tenant repositories/services+cipher+DDL，Phase B在真实tenant ID已产生后装配OAuth管理服务/security projection。Bean qualifiers沿现有：identityDirectoryContext、oAuthRealmRepository、oAuthSigningKeyRepository、oauthKeyCipherComponent、OAuthClientServiceImpl、resourceServerServiceImpl、oauthSecurityProjectionComponent、transactionManager、idpClock、defaultValidator；phase-local同名技术Bean不可与production context混用。TenantServiceImpl/IdpBootstrapService未因本附录变成新重构目标；test factory沿既有class构造，但新业务 Component自身只能Lombok qualified final DI，源码Compile验证lombok.config复制Qualifier。

复杂分阶段准备采用已有TransactionTemplate+**固定阶段method-reference Strategy表**（身份/realm/client/catalog/review），目标选择是本地明确recipe；共同算法guard→transaction→aftercommit核查→receipt。不建抽象handler/factory层，不按未可信请求string反射dispatch。简单constraint、文件权限/路径比较保持直接。

## R2. Immutable manifest、保护文件与误操作守卫

### R2.1 输入与派生事实分离

intent manifest是不可变的批准目标，不放未知tid或fakeJWT；runtime-registry/checkpoint是根据真实owner已提交结果生成。所有在绝对 repo/target/xingyuan-integration-<runId>/，普通文件/非symlink、调用用户拥有，目录0700、JSON/secret0600。路径canonicalize并拒绝..、NUL和越出taskroot；secret引用可以指向用户明确现存的保护凭据文件，只读复用，不复制/修改原文件。

| Field | Required/default | Type/constraint | 语义/来源 |
| --- | --- | --- | --- |
| schemaVersion | 必需/无default | integer exact1 | 输入schema；unknown field拒绝 |
| runId | 必需 | 小写UUID canonical36 | owner身份，仅技术marker，不作业务ID |
| repoRoot/runtimeRoot | 必需 | absolute规范path≤4096；runtimeRoot=repoRoot/target/xingyuan-integration-<runId> | 本任务边界，不能选择旧.runtime/全局target |
| publicOrigin | 必需 | HTTPS exact https://127.0.0.1:28443，无userinfo/path/query/fragment | 新公共同源入口 |
| postgres.endpoint/maintenanceDatabase/credentialFile | 必需 | 127.0.0.1:5432、postgres、absolute保护引用 | 不从默认业务DB推断；password不写manifest |
| databases | 必需 | @Size(min=9,max=12)，unique role/name；name=[a-z][a-z0-9_]{0,62}且exact冻结allowlist | R5九新DB；超过清单拒绝 |
| redis.endpoint/database/credentialFile | 必需 | redis://127.0.0.1:6379，database1..15且需当前empty/同owner；absolute保护引用 | local受限plaintext；不改ACL/全局配置 |
| platformStoragePartitionId | 必需 | decimal string正Long≤9223372036854775807 | P不等于后续实际两tenant ID；不是managementTenant ID |
| tenantCodes | 必需 | exact ["default","tenant-b"]；各符合[a-z][a-z0-9-]{2,63} | 新隔离DB的真实服务创建，不复用旧tid |
| userRecipe/authorizationRecipe | 必需 | 本地已实现固定recipe ID；目录build/checksum及规则由review manifest绑定 | 不接受任意SQL或“全权=true” |
| processes | 必需 | unique ID/port/artifact，@Size(max=32)；全部来自R5固定集合 | 所有平台/5Vite/4samples，不能任意exec字符串 |
| artifacts | 必需 | role→规范文件引用+SHA25664hex+buildId1..128 | 真实build/generate结果，拒绝source映射/旧JAR |
| secretFiles | 必需 | exact OAuthKEK/EdgeKey/AuditKey/TLS/密码/HMAC路径引用 | 生成task密钥与existingcredential分开；不储secret值 |
| deadlines | 必需 | startup1..300秒、operation1..900秒、HTTP1..30秒 | Duration；没有无界wait/无限retry |
| catalogFiles/policyFiles | 必需 | taskroot内regular file≤2MiB、SHA256绑定、strictschema | 用户批准测试定义；不可换成旧DB bootstrap输入 |

主stdin不是secret入口；unknown/duplicate flags、query重名、未知JSONscalar类型、null/blank/pathescape均失败。常规callback code/state/password从专用program内存/文件处理，不经过CLI argv/stdout；shell不得set -x。

私有 identity-provisioning.json 与真实 runtime-registry 另保留显式 cliResources：resourceKey、原 owner 的 resourceServerId、default/tenant-b 各实际 PUBLIC clientId、已审核的目标只读 readPath；subjects 取真实 IdentityFacts 的 admin/viewer 标识并排除诊断 toString。cliResources 不改变 immutable manifest 顶层 schema，也不授权任意 issuer/audience/scope 或增加生产接口。准备前验证配方中的 Client/Resource 关系，消费时再按已提交真实 Client/Resource facts 精确解析；不得按显示名猜 Client、将 CONFIDENTIAL BFF Client 复用为 PUBLIC 或导出 BFF Token。PUBLIC flow 只生成私有正式测试输入，此时不请求业务目标，probeStatus明确为null；真实资源启动后的200/403及副作用证明由必需R6用例独立产生，不能把输入生成当业务授权通过。

私有 runtime-registry 还保留原 owner 已提交的 realms（TrustedRealmBO：真实 issuer/realmKey/realmType/storagePartitionId/businessTenantId/trustVersion；无私钥或 Client secret），供配置生成逐字消费。完整 registry 私有读/写的明确上限为2 MiB；manifest、公开 R3 Result、阶段/DB receipt 保持128 KiB。所有文件继续受原 owner/0600/0700/no-symlink、strict JSON、run/hash 与模型集合上限约束，不增加任何 client/resource/scope/grant 数量或生产协议。配置不能猜默认 trustVersion、tenant ID 或 Client；实际资格仍由原 SDK/owner/PDP 验证。

### R2.2 每次mutation之前的double gate

1. Shell先验证manifest schema/owner/root/frozen names/ports，且XINGYUAN_ISOLATED_INTEGRATION=true。Java IT再验证同manifest hash、JDBC exact host/port/DB及owner receipt后才创建DataSource/运行DDL。
2. First prepare只允许九DB均不存在。用createdb新建并设置本任务COMMENT，记录pg_database.oid和owner；CREATE DB不是跨DB事务，每个已创建目标立刻独立receipt。
3. Resume必须runId、OID、COMMENT、manifest hash都一致。已有同名但owner缺失/不同，停止；绝不drop/clean/repair。
4. 真正连接再读current_database/inet_server_addr/port核对；JDBC URL不得有隐藏多host/意外queryoption/别库schema。所有application datasource一致受manifest检查。
5. Redis首次DBSIZE为0后set NX任务owner marker；出现竞争/非empty停止。Resume需同marker/runId，所有SDK/AS/JA/Edge用同DB，不FLUSHDB/FLUSHALL/改ACL。
6. recover(realm)会按issuer hash删自己的securityprefix；调用前exact issuer必须在本runId map且DBowner正确，不能recover用户原issuer。
7. 启动每端口必须空闲，不kill占用者。PID记录PID+启动时刻+artifactSHA+runId；stop发现复用/不匹配则拒绝。
8. nginx必须使用task -p/-c/pid路径，禁止reload/stop默认系统nginx。只读credential失败时请用户修文件，不打印/复制/重置密码。
9. Manifest advisory任务锁与真实owner事务锁互补；前者不能证明业务行一致性，后者不会授权任意DB目标。

### R2.3 File/DB双介质恢复

输出先owner-only临时文件→完整写入/force→同directory原子move→directory force。DB提交后新连接按已记录business keys/version/hash复读，才将checkpoint阶段标COMMITTED。commit outcome未知进入UNKNOWN_COMMIT，不自动重放/旋转secret；report publish失败不表示DB未提交。

Secret从真实Client create/rotate返回只写一次受保护staging文件，不直接serialize CreatedOAuthClientVO。文件已存在但DB不匹配/结果未知时保留并停止；确认同Client credentialVersion/hash后才发布引用。没有clearsecret时不自行复制旧hash或默默rotate；由明确恢复合同对本任务client处理，不能碰旧client。

## R3. CLI公共传输合同与九个原子子命令

入口固定 scripts/unified-xingyuan/isolated-integration.sh。只有当地用户已批准的test-local操作，**不增加HTTP/RPC生产接口**。作用范围每次由R2复核，不把“之前preflight通过”当新的authorization。

### R3.1 公共flags、结果和错误

| Flag | Source/type | Required/default | Validation |
| --- | --- | --- | --- |
| --manifest | operator file reference | 每命令必需 | R2 absolute普通owner-only文件≤128KiB |
| --result | task evidence reference | 每命令必需 | taskroot/evidence下规范JSON文件；不覆盖别人文件/无symlink |
| --resume | explicit recovery intent | 仅prepare/provision类optional，defaultfalse | 同runId/manifest/OID/阶段内容；不等于force/repair |
| XINGYUAN_ISOLATED_INTEGRATION | environment opt-in | mutation命令exacttrue；status可缺省 | false/unset不能运行mutation |
| --tenant-code/--resource-key | operator selector | 仅authorize-cli必需 | 从已生成registry exact查default/tenant-b与server-ownedresource，不给任意issuer/aud |
| --output-file | private token output | authorize-cli必需 | taskroot/secrets下新普通file；0600；与result不同；不打印 |
| --case-set | predefined test selection | verify必需 | exactfull；focused只供debug，不能替代full completion |
| --user-key | approved actor selector | authorize-cli必需 | fixture admin/viewer，不接受任意sub/role/scope |
| --token-index | committed formal input references | verify与consumer启动必需 | typed index按tenant+resource+client绑定，ref及expiry/currentfacts校验；不含rawtoken |
| --phase | 明确授权阶段 | CLI-004必需 | exactplatform或native；platform不登记native目录/自动nativegrant，native必须持platformreceipt及正式TianshuCLI token-index |

每命令成功写同一个 IsolatedIntegrationResult schema；stdout只一行operationId/status/result-relative-path。结果字段下列全部存在，error可null，steps/targets无项用[]，counts所有字段0表示本操作未涉及。报告从来不包含rawJWT、code/state/verifier、password、privatekey、Client clearsecret、rawclaims、subject、Cookie。

~~~jsonc
{
  "schemaVersion": 1, // 必需，报告schema exact1。
  "operationId": "CLI-001", // 必需，当前原子操作ID。
  "runId": "f7a1568f-4db3-41d9-9f37-147b62132a83", // 必需，输入manifest canonical UUID。
  "manifestSha256": "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa", // 必需，当前immutable input hash。
  "status": "COMMITTED", // 必需，COMMITTED/RUNNING/PASSED/STOPPED/BLOCKED/PARTIAL/FAILED/UNKNOWN_COMMIT。
  "completedAt": "2026-10-11T02:26:00Z", // 必需，server Clock UTC ISO Instant。
  "targets": [
    {
      "targetKey": "idp", // 必需，manifest稳定role；不放user/secret。
      "identity": "xingyuan_it_idp_20261011", // 必需，DB名或process ID；不放凭据。
      "version": "1", // 必需，OID/checkpoint revision的十进制文本，具体kind由targetKey确定。
      "state": "COMMITTED" // 必需，COMMITTED/RUNNING/READY/UNREADY/STOPPED/BLOCKED/UNKNOWN_COMMIT。
    }
  ],
  "counts": {
    "databaseCount": 9, // 必需，0..12，本次确认目标数量。
    "tenantCount": 0, // 必需，0..2，真实本次准备tenant数量。
    "clientCount": 0, // 必需，0..192，64个有界client定义×3realm上限内真实scoped row数量，不按同id跨realm折叠。
    "catalogCount": 0, // 必需，0..32，真实registered/reviewed catalog数量。
    "liveCaseCount": 0 // 必需，0..128，本次实际执行live case数量，skip不计。
  },
  "steps": [
    {
      "stepId": "CREATE_DATABASE_IDP", // 必需，受限ASCII1..64固定阶段code。
      "state": "COMMITTED", // 必需，上述状态子集。
      "evidence": "evidence/db-idp.json" // 必需，taskroot相对路径，无secret。
    }
  ],
  "error": null // 成功为null；失败按下一schema完整对象。
}
~~~

失败error对象四字段必需，不存任意Throwable message/stacktrace：

~~~jsonc
{
  "code": "XINGYUAN_TARGET_UNSAFE", // 必需，以下固定错误族。
  "message": "Isolated integration target was rejected", // 必需，稳定安全通用消息，不回显secret/SQL。
  "retryable": false, // 必需，只有重读/恢复合同允许才true。
  "stage": "TARGET_GUARD" // 必需，固定阶段code，不能请求决定。
}
~~~

| Exit | Stable code | Condition/恢复 | sideeffect许可 |
| --- | --- | --- | --- |
| 0 | 无error | 此命令目标真实完成；status只反映本命令 | 已记录允许的事实 |
| 2 | XINGYUAN_INPUT_INVALID | unknown/duplicate flag、JSON/shape/constraints失败 | 不变 |
| 3 | XINGYUAN_TARGET_UNSAFE | owner/OID/path/PID/DBlist/port/Redisowner错误 | 不变，停止核查 |
| 4 | XINGYUAN_INPUT_UNAVAILABLE | 必需文件/已批准依赖/正式token缺失 | 已提交阶段保留；不造输入 |
| 5 | XINGYUAN_STATE_CONFLICT | business version/checksum/client事实冲突 | 本事务rollback，不自动覆盖 |
| 6 | XINGYUAN_DEPENDENCY_UNAVAILABLE | timeout/PG/Redis/AS/真实owner unavailable | 本事务rollback；可在新连接核查后resume |
| 7 | XINGYUAN_PARTIAL | 前面目标commit，后续未完成或报告未发布 | 保留已提交checkpoint，不宣称全成 |
| 8 | XINGYUAN_VERIFICATION_FAILED | live assertion失败或required case skip | 保留task进程/证据，禁止PASS |
| 9 | XINGYUAN_COMMIT_UNKNOWN | DB/secret/client commit outcome不能证实 | 冻结该阶段，重读核查后再批准恢复，不能盲重试 |
| 10 | XINGYUAN_PROCESS_IDENTITY_CHANGED | stop/启动复核PID或artifact身份变化 | 不杀陌生进程 |

整体deadline1..900s，启动每process≤300s、单HTTP≤30s；wait最多30s一轮并更新安全状态，不用一个工具阻塞超过60s。network重读重试最多3次、有退避；业务Command不自动重试；用户新输入可中断wait，但不能因此另开第二组相同进程。

### CLI-001 prepare — 新目标、密钥、TLS与配置

0. 必要性：Add，独立可复核的新隔离环境；旧prepare触及旧runtime且不管理Edge。仅操作者/Plan消费receipt，不是API parameter discovery。
1. 身份：本地Command；exact prepare --manifest --result [--resume]。无业务token，R2本地allowlist取代任意地址选择。
2. 输入：仅公共flags；manifest artifact可在build之后填写，缺文件exit4；无SQL/drop/force flag。
3. 成功：exit0/statusCOMMITTED，9个DB OID/owner、任务CA/keys/保护files引用及配置hash存在，counts.databaseCount=9。尚未插入tenant/client/role，liveCaseCount=0。
4. 错误：公共exit2/3/4/6/7/9；任何旧目标不安全在写前拒绝。
5. 逻辑：guard→CLI/tools/artifact检查→逐新DB create+receipt→task密钥/TLS→固定profile env writer→TLS/SAN/权限静态check→result原子发布。CREATE DATABASE逐目标独立commit；后目标失败不自动删前目标。
6. 兼容/verification：旧prepare路径不受影响；regression验证已有DB、badowner/port、secret含newline安全处理、原子发布失败和resume；真人环境再做OID/原设施保留证明。

### CLI-002 provision-identity — 真实OAuth初始事实

0. Add，现有bootstrap不创建空realm/JWK；独立数据Command，无新生产接口。
1. exact provision-identity --manifest --result [--resume]。调用SA IsolatedOAuthProvisioningIT；默认普通mvn test不得访问外部PG/Redis。
2. 输入：PREPARED receipt、real currentartifact、task32rawKEK/auditkey及user保护密码；recipe固定两tenant/admin+viewer，不接rawJWT/SQL/手工tenant Header。
3. 成功：exit0/COMMITTED，真实identity/credential、两tenant/member、3realm/JWK、REALM/CLIENT generations、SERVICE/USER/Resource/Grant facts提交且reconciliation，生成registry仅引用秘密files；tenantCount=2。
4. 错误：shape/owner先exit2/3；halfcommit exit7；generation未ready/dependency exit6；无法证实commit/secret匹配exit9。
5. 逻辑：Phase A低依赖真实DDL/identity/tenant+realm/key事务→真实derivedtenant IDs→Phase B OAuth管理服务/投影→逐Client/Resource/Grant真实owner→新连接核查→私密registry。没有listener/securitychain/main runner。
6. 兼容/verification：R4 INTERNAL合同；真实PG rollback、两realm不同key、MDC finally、Outbox/project-before-ready、conflict/unknowncommit；无复用旧迁移source。

### CLI-003 start-control-plane — 原身份及授权控制面

0. Add，解决真实Directory/授权初始化依赖；不运行frontend/业务consumer，不伪造readiness。
1. exact start-control-plane --manifest --result。query消费committedidentityreceipt；先由TA最小NONE夹具执行本任务新库的原managed DDL/基础目录 owner 前置阶段并记录独立receipt，再开启task AS/Directory RPC、JA、Tianshu、Yuheng Admin及专用nginx的标准AS协议入口，随后Edge。**tasknginx先将/platform/**、/tenants/**直接HTTPS转发AS**，标准SAS仍自行CSRF/PKCE/Client/Scope验证；不改productionguard。Edge的Boot issuer-uri startup discovery会访问public28443，此入口不能再依赖尚未启动Edge。不启动Vite/native/业务引擎。
2. 输入：真实kernel事实/SSL/qualifiedconfig/artifactHashes；端口空闲；taskPID锁。无--repair/--skip-security。
3. 成功：exit0/RUNNING；监听器实际alive、metadata/Directory功能可用、PID/启动时刻已记录。业务授权/所有lease尚未完成时readiness可DOWN，结果不得把alive写成READY。
4. 错误：占用/PID冲突exit3；失败启动exit6/7，只记录和保留task进程，不kill外部占用者。
5. 逻辑：登记task新进程身份→AS/Directory监听可用→tasknginx直接标准AS路径→逐issuerpublicdiscovery/JWKS验证→Tianshu/JA/Admin按现lease依赖→Edge Boot正常discovery完成，再由tasknginx的BFF/API路径转发Edge。API/MCP前置resourceauth保持，尚未存在heldresourceclient时401；无frontend/businessupstream不宣称全栈READY。host原readiness/lease条件保留，不自动在启动中注册/grant业务数据。业务GRANTS由CLI-004明确准备，正式CLI-006随后可经同origin真实AS取token。
6. 兼容/verification：code/clock/JWKS/strictstate缺失正确拒绝；服务启动不能触发旧库初始化；PID复用测试；live监听/健康真实记录。

### CLI-004 provision-authorization — 显式审核、授权与目录/角色投影

0. Add，独立明确的授权Command；register机械目录不autoGrant；不把测试realm登记当用户业务许可。
1. exact provision-authorization --manifest --phase platform|native --result [--resume] [--token-index <path>]。同一原子子命令在明确phase做有界准备，不隐藏stage。platform在JA真实test-context建立原基础平台授权；native必须platformreceipt+正式TianshuUSER token-index。现有bootstrap-platform-admin只收tenant-id/identity-sub，不传密码。
2. 输入：CONTROL_PLANE RUNNING、当前Directory active membership、冻结catalog/policy recipe、buildId/checksum、expectedrole/applicationversions、真实derivedIDs。输入不是客户端任意role/field。
3. 成功：platform exit0/COMMITTED，仅基础平台角色/权限及实际projectionready，**不表示native授权完成**，之后CLI-006取得具有TIANSHU_WRITE/READ的真实Tianshu resource USER AT。native exit0/COMMITTED须真实Tianshu新biz/app→JA应用纳入→catalog REGISTERED_PENDING_REVIEW及其后**独立**显式review/permissionmapping、role resource、assignment/activation、field-rule事务逐项提交；snapshot≥结果policyVersion/authVersion且目标正例可验证。两个phase分别receipt，只有二者都commit才准许CLI-005/最终完整verify。registerResult或fieldResult CONFIRMED本身不能证明projection READY。
4. 错误：跨tenant/application/fieldmaximum/version/activepermission错误exit5；missingcatalog ownerexit4；projection未readyexit6/7，已commit数据保留；不写fakeVERIFIED状态或直接insertfield_rule。
5. 逻辑：platform只实际member/builtinplatformbootstrap/explicitapprovedroleactivation/快照核查，必须在native目录登记前执行，不能以后再次调用localbootstrap的全资源grant分支自动扩大native授权。native先用正式TianshuUSER AT访问原metadataowner创建/核查enabledbiz/app，再真实TenantApplicationFacade.admit把每实际app纳入两tenant并创建globalcatalog行；随后GlobalResourceCatalogService.registerDeploymentCatalog→PermissionCatalogService.changeStatus(ACTIVE)→ResourcePermissionMappingService.update→RoleFacade/RoleResourceGrantService/AssignmentFacade真实grant/assignment→RoleResourceGrantService.replaceFieldRules→原Outbox/ROLEprojection→真实roleactivation/快照核查。每owner原tx独立，按manifest分别显式选择，拒绝“登记等于grant”。phase缺初始真实scope/角色/天枢目录就失败，不能造fakeROLE/CAP/admin/ACTIVEAppPO或给SERVICE冒充USER。
6. 兼容/verification：R4.3合同及native附录；platform→CLI-006→native明确resume顺序；相同checksum重试、不同checksum/versionconflict、无fieldrules拒绝、rulesWRITE但definition不writable拒绝、broker/projection失败；正WRITE需当前actualrule+scope，不fakeDefaultWRITE。若初始天枢biz/app尚缺导致platformbootstrap不能形成真实READY权限，则通过已有真实owner的本任务准备补齐并明确记录，不把被依赖拒绝的platformphase判成功。

### CLI-005 start-consumers — 真实引擎、Edge、前端和四生成项目

0. Add，独立运行Command，不含隐式provision。使用当前四native生成产品，Open只是内容不变性测试。
1. exact start-consumers --manifest --token-index --result。选定的process/artifact来自冻结清单。
2. 输入：授权receipt+live lease/config、实际安装0.2.5、requiredformalCLItoken索引、allfixedbinding/TLS/envkeys。缺tokenexit4，不能用SERVICE假USER。
3. 成功：exit0/RUNNING；Biz/MCP/LLM/5Vite/4native的task PID明确；原CLI-003 Edge/nginx身份复核复用，不创建第二会话入口。必要readiness真实UP，Portal入口可导航；不代表SSO case PASSED。
4. 错误：旧artifact/Sourcealias拒绝exit3/4；process/readiness失败exit6/7。只有必要正常fixtures/provider输入才启动，不带Docker/paidLLM演示。
5. 逻辑：使用CLI-003已有真实Edge/协议入口及CLI-006formalAT；真实Tianshu注册/Definitions/Yuheng路由发布按原owner完成→引擎→5Vite→4generated样例。Edge对未就绪upstream依原协议503，最后必要lease/route/readiness真实UP；不通过假route宣称先通。同源resource registration/route token由production代码处理，不靠proxyfakeauth。
6. 兼容/verification：真实lease/resource/build一致；issuer/redirect/SAN都匹配；invalidBearer/headers不可进入业务。原用户进程/端口不改变。

CLI005 的 source bridge 以两个 CLI004 phase 各自 canonical stepId 区分授权凭据，交叉核对原六个控制面 PID/启动时间与 ledger；所有待启动的进程端口先检查无 foreign listener。正式输入绑定每个审核 cliResource 的 A/B 与 admin/viewer，真实 PUBLIC USER Client、scopes、资源和私有文件 hash/expiry，任何 SERVICE、跨租户或缺项在启动前失败。十二个新增进程捕获身份后立即落盘；重试仅复查仍存活身份，不因健康暂不可用而重启。原 root health 与 readiness 实际 UP、五同源 mount 返回对应 Vite client HTML 后才输出 RUNNING；不将 cookie/session、listener401/403/404/503或受控 transport 当业务验收。task profile 仅启用原 Boot health probes，保留 normal readiness group 与 lease/audit/SDK 约束。CLI002 原计数只确认 idp 一个 DB，其余全目标命令保留九 DB，不改原 Result 计数语义。

### CLI-006 authorize-cli — 正式PUBLIC Client单资源Code/S256

0. Add，独立正式CLI输入生成；不是BFF tokenexport、不发新tokenAPI。
1. exact authorize-cli --manifest --tenant-code --resource-key --user-key --output-file --result。使用玉衡已锁定Playwrightrunner启动专用browser与loopbackNodecallback。
2. 输入：commitregistry里的PUBLIC USERClient/exactredirect/minimalscope；AS真实login/consent页面；passwordfile由程序内读入不echo；resource单值，不能任意issuer/scope。
3. 成功：exit0/COMMITTED，真实AS授权code兑换的AT只写output0600；index绑定tenant+client+resource+issuer+expiry，安全result只记身份类别/目标key/计数，不能stdout/rawclaims。RT必须无（PUBLIC）。
4. 错误：cancel/login/consentdenial是FAILED/exit8；nonce/state/iss/redirect不符exit3；AS503/expiredtokenexit6；不自动fallback/fake/signJWT。
5. 逻辑：程序内SecureRandom verifier/state/nonce→真实authorizationRequest S256→真实AS form_csrf+password→真实consent→callback校验→服务端POST token+verifier→本地签名/精确声明及真实AS UserInfo验证→privatefile/index发布。此阶段只生成正式输入，index的probeStatus明确为null（NOT_RUN），不请求尚未启动的资源；启动后的R6实际资源调用、Scope∧PDP、SQL隔离与WRITE验证仍为必需。code一次，retry从新的正式flow重启；不重用code。
6. 兼容/verification：不能提取BFF AT/RT或读browserCookie当CLIinput；wrongverifier/state/iss回调拒绝；两tenant/每resource分别取AT；full gate使用actualcaller，不page.route。


正式输入重取的实现边界：full入口保存已验证的确切tenant/resource/admin-or-viewer清单及冻结registry；长BFF后只对这些原客户端/资源/scope重新完成Code/S256/nonce/UserInfo，不新增client、grant或身份。每项独立所属进程、全新0600 AT/result，旧文件保留；CLI006 R3及当前index须新鲜、身份不变、新路径/摘要/有效期正确，复制旧AT、换tenant或扩大scope拒绝。全部新输入再次统一验证未过期后才进入PG/native/resource；显式formal-inputs进度/汇总和公开steps保留来源，UNKNOWN_COMMIT停止且不重试。取消/timeout只TERM原创建handle所属npm组并保存出生/终态，无法确认时保留现场。900秒预算仍硬限，不能为凑时长修改真实TTL/时钟或接受过期材料。

### CLI-007 verify — 全矩阵真实验收

0. Add，独立Query/验证操作；负面case按manifest有界临时变更仅taskfacts，并恢复真实owner，不触设施全局。
1. exact verify --manifest --token-index --case-set full --result。debugfocused不得作为完成。
2. 输入：运行PID/artifact/receipts/formalAT，R6所有requiredcases；过期AT回CLI-006，不能脚本偷偷refresh浏览器token。
3. 成功：exit0/PASSED仅在fullcases真实全通过；liveCaseCount实际数。missing/skip全部exit4/8，禁止PASS-with-SKIP。
4. 错误：assertionexit8；requiredinputexit4；partialfailureexit7；恢复状态未知exit9，retainevidence，不自动结束Goal。
5. 逻辑：TLS/metadata→tenant A SSO/bootstrap→logout全部A族→NEW显式actualtenant B registration正常Code→五页面同alias/同route实际issuerB/tokenB→B refresh/stepup/全部族logout→按resource业务PDP/SQL→native注册RPC/servicecache→audit/readiness→preservation。PUBLIC CLI的B AT只证明自己的正式调用族，不替代此BFF链；每case证据来源和expectedstate按R6。full还必须执行OutboxTransactionRollbackIT的jpaRoleResourceAndMpOutboxRollback、jpaRoleResourceAndMpOutboxAppendFault、jpaRoleResourceAndMpOutboxCommit三个真实PG门禁，实际selector #jpaRoleResourceAndMpOutbox*，非0执行/0skip；受控mock/旧JDBC case不计此证明。
6. 兼容/verification：不把MockMvc/H2/typecheck/fixtureE2E计livePASS；报告typechecks/controlled/real各自scope；原旧liveIT claim/revocation更新但不扩大skip容忍。

CLI007 的执行证明使用实际 Surefire 报告：原3.2.5 plugin.xml 无 reportsDirectory user property，保留原 module/target/surefire-reports；按本次开始时间与旧 fingerprint 拒绝旧文件，class/method 与 suite/testcase 数量相符、各方法一次且无 skip/failure/error。XML UTF8≤4MiB、无DTD/entity、owner/no-symlink/稳定文件；公开结果只引用经过投影的方法数及 SHA。私有 JVM @argfile 通过原 argLine property 给测试 fork 信任任务 CA，password 不入 argv。固定 module/selector、UTC/offline/no-skip 与最小环境保留，不能接受任意测试类/外部 JVM 注入。

新增私有 case receipt 供对应真实 runner 写入 attempt/cases/<runner>/LIVE-NNN.json：schemaVersion/runId/manifestSha256/caseId/runner/status(PASSED)/executedAt(UTC Z)/testClass/testMethod/proofs 必需；proofs 为 attempt 内相对 taskroot 的 path+sha256 列表（1–32）。消费方将 class/method 绑定本次已执行测试，检查时间、完整状态、文件私有性/路径及实际 hash；单个 schema 校验/受控 receipt 测试不能代替业务断言或实际网络/DB结果。Playwright afterEach 额外只写安全 title hash/status/retry/实际执行数，不保留密码/请求步骤。所有短测试子进程使用创建句柄和 ps 的 PID/start/command/processgroup 身份，源码测试只启动有限 CPU 子进程验证 TERM 边界，不启动平台或设施。

CLI007 总编排先核实际 runner 源码/必需方法与前置凭据，缺源码exit4且不创建子进程。独立 verification.lock 防并发完整验证；每次 fresh attempt/intent 冻结测试源码SHA，结束复查。SA oracle READY 后才运行原浏览器，两创建句柄并行观察，不因轮询超时重启；二者正常终止且原报告、A/B四阶段及实际浏览器完成记录确认后，才依次执行三PG门禁、native与resource测试。Resource runner在其他case后实际执行LIVE023的task stop/原设施保留，Parent还要复查原18ledger已STOPPED；单个case声明PASSED不能代替停止。Native的LIVE011–015及Resource的其他编号必须各有对应实际执行方法与私有proof，满24才发布PASSED。source-only/controlled pipeline 的完整模拟不计实际liveCase；未实现的runner、缺baseline或缺proof继续失败，不用空实现补绿。失败保留材料并只收束创建的短测试进程。

### CLI-008 status — 当前handle、元数据与状态

0. Add/Keep，独立只读Query；与command授权复核不同。
1. exact status --manifest --result；不要求mutationoptin，但仍owner/root验证。
2. 输入：manifest/PIDrecord/artifacthash。无secretcontent；不sourceuntrustedenv。
3. 成功：exit0，结果statusRUNNING/STOPPED或PARTIAL与真实process及readiness一致；无hiddencreate/start。
4. 错误：unsafeexit3/identitychanged10；网络暂时failure标UNREADY，不能因单次timeout判processterminal。
5. 逻辑：复核同PID/starttime/artifact→只读port/health/lease/receipt→安全result。原secret权限只stat，不能cat。
6. 兼容/verification：一次观测timeout保持同handle继续poll；无phantomrestart；纯fixturestatus测试+实际PID。

### CLI-009 stop — 仅本任务进程停止

0. Add，独立Command；用户批准的任务清理，不删数据/秘密证据。
1. exact stop --manifest --result。无--all/--drop-db/--flush/--docker。
2. 输入：每task PID/starttime/artifacthash；unowned任何进程拒绝。
3. 成功：exit0/STOPPED，task进程已终止，PG/Redis/系统nginx/旧PID数据全部保留；文件/db仍可审核。
4. 错误：复用PIDexit10、不杀；task停止超时exit6；已stopped是幂等STOPPED。
5. 逻辑：按reverse依赖停止proxy/frontend/consumer/Edge/controlplane；SIGTERM≤30s，每handle核查；SIGKILL只对仍匹配同任务身份进程且记录事件。不调用默认nginx -s或旧stopstack。
6. 兼容/verification：新DB/OID及testevidence保留；原设施listener/metadata与准备前一致；完成后不再自动重启项目。

## R4. 真实owner输入、成功字段与事务映射

### R4.1 DDL、identity/tenant和初始realm/key

SA Phase A只最小真实数据装配：OAuthPersistenceConfiguration的logicalfactory→OAuthManagedDdlComponent.initialize(physical,yaml)→EgonColaPostgreDdlRunner原manifest/tianquan_oauth→OutboxLogicalDataSourceFactory原owner。具名primary transactionManager=DataSourceTransactionManager(dataSource)。所有newtarget先R2 gate，再DDL；不扫描IdpAdminApplication触发缺platformrealm的ApplicationRunner/SecurityFilterChain，也不扫描development SmartInitializingSingleton。Phase B用Phase A真实managementTenantId装配OAuth管理resolver，不向其填fakeID再偷偷修改。

关系模型change: No。只新增已批准的新DB事实，不新增表/列/index/sharding算法/迁移。所有旧db/migration与db/egon-mp脚本checksum不改；当前owner在新DB走现有权威初始化机制，不新增另一迁移框架。每schema/SQL-history仍按owner原事务，不声称跨DB全局原子。

| Reuse ledger ID / 当前方法 | 入参完整约束/normalization | 成功字段/来源 | tx/恢复/verification |
| --- | --- | --- | --- |
| INTERNAL-RT-001 IdpBootstrapService.bootstrap(username,char[]) | username按原UsernameNormalizer；testrecipe alice/viewer；password仅file程序内读取≥12并符合原passwordpolicy，不入argv；char[]finally擦除 | 方法void；通过真实IdentityUserStore.findByNormalizedUsername读id/status；不输出hash；真实credentialACTIVE | 原@Transactional；已存在不同用户不覆盖；user与credential同事务；正/拒绝/rollback核查 |
| INTERNAL-RT-002 TenantService.create(CreateTenantCommand)、update(id,UpdateTenantCommand) | tenantCode=[a-z][a-z0-9-]{2,63}按原trim/lowercase；name1..200；settings合法JSON object、按原BO字节上限；actor1..128；id为真实返回positiveLongdecimal；updateexpectedVersion≥0，INITIALIZING→ACTIVE | TenantView完整tenantId/code/name/status/settings/version/createdAt/updatedAt沿原方法；report只IDs索引/安全计数；P不得等于tid | 原service→IdentityTenantRepository在目录P；create和statusupdate独立原tx；同code冲突不overwrite，状态/版本复读 |
| INTERNAL-RT-003 TenantMembershipService.upsert(UpsertMembershipCommand) | tenantId/identitySub真实owner派生；ACTIVE；首次expectedVersion=null，已存在必须currentVersion；closedtenant不能ACTIVE；testDTO先注解验证 | MembershipView tenantId/identitySub/displayName/status/version/updatedAt；不得把rawidentity放publicmetrics/log | 原service锁membership/create或CAS；真实IdentityDirectory有效member，不写fakeuser或跨tenantcontext |
| INTERNAL-RT-004 OAuthRealmRepository.save / OAuthSigningKeyRepository.save / OAuthKeyCipherComponent.encryptPrivateKey | TrustedRealmBO issuercanonicalHTTPS；PLATFORM realmKeyplatform/P/businessTenantnull；TENANT realmKeytenant:tid/partition=tid/businessTenant=tid；keyRSA≥3072、RS256/publickty/kid/use/n/e；PKCS8仅内存 | 真实savedrealm/key由原MapStructconverter；realm/key主键由原MPfill，trustVersion0；privatecipherENC:v1使用原AAD，不报告cipher/privatekey | 每realmTransactionTemplate：realm idnull/version0→key idnull/version0 ACTIVE→realm version1 activeKid；任一步rollback；initialfixture仅本run新DB，不改生产publish/activate24h；MDCfinally |
| INTERNAL-RT-005 OAuthSecurityProjectionComponent.recover(trustedRealm) | exactregisteredrealm/runId/DBowner；无历史授权时才允许missinginitialstate；未知旧issuer拒绝 | 真实readyepoch/status/revision；随后isIssuerReady与snapshot检查，不用testRedisstub | 原realmLock、tenantrepositories+严格RedisLua；先于CLIENT_CREATED；failure503/pending，未READY不得签发或宣称接入 |

Provision manifest DTO以Prepare/Provision/Runtime组校验，在进入原服务前构造并manual验证原CreateTenantDTO/UpsertTenantMembershipDTO/CreateOAuthClientDTO等约束。原service/state constructor的业务约束仍是权威；不能依赖仅外层JSONvalidator。新直接跨层投影走嵌套MapStruct BaseForwardConverter；不JSONroundtrip/BeanUtils/setter散拷。内部existingrecord命令按原合同复用，不因命名规范重写未改旧模型。

R4的INTERNAL-RT-*是现有owner复用/事务证据索引，非新增独立服务操作；未改方法输入/成功wire/业务语义。新Affected原子contract是R3九CLI及主native附录的明确register/fieldwriter方法；不能把本复用ledger当新增API、按一行两个旧方法发新wrapper或改旧record。

### R4.2 Client/Resource/Grant（原owner）

| ID / exact方法及Bean | 入参/成功 | 安全/tx/恢复 |
| --- | --- | --- |
| INTERNAL-RT-006 OAuthClientService.create(trustedRealm,platformRealm,@Valid CreateOAuthClientDTO,operatorSub)，OAuthClientServiceImpl | clientId ASCII[A-Za-z0-9._-]1..128；SERVICE appId=[a-z][a-z0-9-]{2,127}；name1..200；typePUBLIC/CONFIDENTIAL；purposeUSER/SERVICE；AT300..600；RT0..604800；redirect/resources/scopes各非null、max16/64/64；HTTPSexactURI，scope[A-Za-z0-9._:-]1..128、不重复。SERVICEconfidential无redirect/RT；PUBLIC USER无RT；EdgeUSERconfidential RT>0/含openid+profile+offline_access/单resourceGrant。CreatedOAuthClientVO clientId/appId/name/type/purpose/status/oneTimeSecret/hint/version/createdAt/issuer；只secret私密发布，其余receipt最小计数/refs | 原@Transactional锁realm→真实resource验证→bcryptsecret→save→revokeClient(CLIENT_CREATED)→grant+audit/Outbox；generation先已建立。不同realclient_id，每微服务独立，不共享SERVICEsecret；secretwrite/commitunknown按R2 |
| INTERNAL-RT-007 ResourceServerService.create(platform,@Valid CreateResourceServerDTO)、enable(platform,id,ResourceVersionDTO)，resourceServerServiceImpl | resourceServerId1..128；resourceUri绝对HTTPS≤2048无userinfo/query/fragment；biz/app1..64；envexactlocal；name1..200；managementClientId真实SERVICE；rbacApplicationCode≤128、entryPermissionCode≤256指真实ACTIVE mapping。ResourceServerVO id/uri/source/environment/displayName/managementClient/rbacApp/entryPermission/status/version/createdAt/updatedAt原合同 | 原owner+目录PLATFORM；新resource先非ACTIVE后显式enable/CAS；client不能引用inactiveResource；noadmissionfakeheader |
| INTERNAL-RT-008 ResourceServerService.putGrant(clientRealm,platform,clientId,resourceServerId,@Valid UpsertClientResourceGrantDTO) | grantTypeUSER_DELEGATION/CLIENT_CREDENTIALS；tenantId语义按context（USERgrant不伪填租户）；scopes防御Set max64 ASCII1..128；resourceVersion≥0必current，newgrantVersionnull、updateversioncurrent。ClientResourceGrantVO完整字段只有clientId/resourceServerId/grantType/tenantId/allowedScopes/status/version，没有credentialContext/issuer；nullabletenantId省略按原JsonInclude；scope不stdout扩权 | 原service重读client/resource/currentversion、精确grant/context；USERscope是Client授权而非用户role权限；CLIENTscope按实际调用最小集合；未知结果reconcile，同keydifferentscope不能盲overwrites |

现实说明：当前CreateResourceServerDTO主要@NotBlank，具体URI/长度仍由Core ResourceServer与service校验；test-only输入先严格受限recipe/DTO防护，不能声称该旧DTO已有所有新注解。fields/null/constraints以源码为准，新增同样边界以native附录设计补齐，不做无关重构。

**USERscope AND 细权限**：USER CLI和Edge资源registration的requested/allowed/Grant scopes必须包含该资源所需的实际业务scope，不能只有openid/profile/offline_access。native原PreAuthorize验证SCOPE_<published-rootArtifactId>:<business-intent>，同时原鉴神APIResource/permission/PDP须ALLOW；Idp认证产生的SCOPE_*由RBAC桥保留。role/PDP有权限但scope不足仍403，scope有而PDP/resource未授亦403。permission与scope虽可使用同一businessintent字符串，仍是两个权域，不用一个替代另一个。

**DEC-006 的独立 OIDC Clients**：每个tenant内每个Resource使用不同CONFIDENTIAL USER clientId；registration aliases不能仍落到同一AS Client。四管理端当前业务scope数量与3个OIDC scope分开登记：授柄21+3=24、鉴神52+3=55、玉衡17+3=20、天枢6+3=9；全平台union为99，超出原CreateOAuthClientDTO.allowedScopes≤64，因此不扩大上限、不合并公共client/scope。Portal identity、native/MCP/LLM若由BFF访问也用自己的有界Client/Resource/scopes；每微服务SERVICE client仍独立，PUBLIC CLI Client另属正式输入。每client的scopes≤64、每resource singleton aud、各族RT独立。provision仅创建本任务新Client/Grant，不自动把旧Grant/userclient扩到此集合。

**DEC-008 待批准的 alias recipe**：五前端固定的 edge-ui/edge-shoubing/edge-jianshen/edge-yuheng/edge-tianshu 是逻辑resource alias，不是跨issuer复用的实际Spring registration。intent只选已审核recipe和两tenantCodes；本任务真实realm、ResourceServerService和OAuthClientService提交后，runtime-registry记录每alias的defaultRegistrationId与issuerRegistrationIds（exact可信canonicalissuer→该realm/resource唯一actualregistrationId），关联实际clientPK/clientId/resourceUri/issuer/version/secretFile引用。两tenant分别调用真实Client owner发行，不能把tenant A clientId/secret换issuer冒充B client；未知、重复、缺issuer binding或registry与真实Client/Grant不符拒绝配置发布。default只用于NEW alias入口；ACTIVE只按server canonical issuer唯一解析actualregistration，不回退default、不从query/header/body选issuer。已有 /oauth2/authorization/{actualRegistrationId} 的显式tenant B入口只可在全部logout后的NEW使用；pending绑定alias/actualregistration/issuer/generation，TokenRelay消费已解析actualregistration。alias与realreg重名仅允许default同tuple，alias入口语义优先；当前提案不声称已实现/获准。

同issuer同测试用户对不同CONFIDENTIAL Client取得的实际AT与OIDC subject必须相同且与canonicaliss/tenant一致。当前AS public sub合同需真实multi-client Code证明；若某client/provider切成pairwise sub造成不同subject，拒绝合并会话并报告配置冲突，不能把sub比较放松为displayName/header/猜测userId。Client数量receipt按scopedrows计数（上限192），不以scope≤64误限制跨realm总Client行数。

Biz/MCP/LLM的实际已签名route/definition配置requiredOAuthScopes是server-owned有界数组，必须由真实发布版本/checksum证明，不从请求header、UI权限或未签名runtime Map取。USER须全部requiredOAuthScopes AND 原resource/PDP细权限；SERVICE仍按自己固定binding/最小scope。missing/malformed未声明的受保护definition fail-closed，旧route不得默默按空集合放行；显式补声明并经过原发布流程，但不改变旧Client Grant授权事实。本任务route与新Grant由用户批准的recipe分别创建，机械目录登记不是授权。

service→web只申请目标实际published组织namespace的user:read、school-class:read；web→service只申请目标实际published评价namespace的course:read、exam:read、score:read；天枢control-plane仍只tianshu:registration:write。真实namespace、Resource URI、methods和集合来自已登记signed artifact/review清单，不能把organization/evaluation等shortalias或示例Root当实际scope。PUBLIC CLI按一resource当前case选择最小业务scope；第一方BFF每resource注册可包含该资源经批准的有界scope集合，但不得扩到别的resource。报告记录scope配置checksum/计数，不输出token。

### R4.3 JA目录登记、审核与授权（DEC-005）

主 Spec native附录中的生产owner是规范依赖：

- globalResourceCatalogService.registerDeploymentCatalog(@Valid DeploymentCatalogRegistrationCommand,actorId) → DeploymentCatalogRegistrationResultVO。输入businessCode/applicationCode/buildId/checksum64hex/expectedApplicationVersion及permissions/resources/fields，不含tenant/user/grants。每application单tx+FOR UPDATE/CAS+checksum幂等，结果REGISTERED_PENDING_REVIEW；fields为机械声明计数，不自动生效授权。
- permissionCatalogService.create/CreatePermissionRequestDTO产生PENDING_VALIDATION；changeStatus(permissionId,ChangePermissionStatusRequestDTO,actorId)明确ACTIVE。
- ResourcePermissionMappingService.update(resourceId,UpdateResourcePermissionMappingRequestDTO,actorId,Instant)重读锁/version、activePermission、同application、in-use语义。
- RoleFacade.createRole(CreateRoleCommandDTO,Instant)；RoleResourceGrantService.replace(tenantId,roleId,ReplaceRoleResourcesRequestDTO,actorId,Instant)；AssignmentFacade.assign(RoleAssignmentDTO)/change(RoleAssignmentChangeDTO)。fixture只授批准清单，不用“All permissions”。
- globalResourceCatalogService.createField(CreateFieldDefinitionRequestDTO,actorId)创建显式definition；defaultAccess合法NONE/MASKED_READ/READ，writable=true仅maxWRITE，不等于user可WRITE。
- RoleResourceGrantService.replaceFieldRules(@Valid ReplaceRoleFieldRulesCommandDTO) → RoleFieldRuleMutationVO：tenantId/applicationId/roleId/expectedRoleVersion/actorId；rules内permissionId/fieldDefinitionId/accessLevel/validFrom/validTo/status。ID由manifest审核记录及真实owner结果导出；服务重读同tenant/application/role/resource mapping/definition upper-bound与ACTIVEpermission。结果roleId/roleVersion/ruleCount/addedCount/changedCount/disabledCount/authVersion/policyVersion/statusCONFIRMED仅DB+Outboxcommit。
- 该fieldwriter复用RoleResourceGrantRepository/JpaRoleResourceGrantRepository、PESSIMISTIC_WRITE+roleCAS、现FieldRulePO、authState.increment、原events.enqueue(FIELD_RULE_CHANGED,sourceTypeROLE,sourceIdroleId)，原publisher role.policy-changed.v1。test不能直接写FieldRuleDAO/PO、不能在新register中autoGrant。
- 原roleactivation选择/替换仍调用原真实owner并检查tenant/user、SOD/auth_time。roleprojection/activation/snapshot必须≥新policy/authversion且fields真实WRITE才可写；emptyrules或writablefalse必拒绝。
- register-application-catalog与replace-role-field-rules离线子命令由native附录逐原子CLI合同拥有，本附录CLI-004只复用它们/真实管理owner。Spring配置用保护的SPRING_CONFIG_ADDITIONAL_LOCATION/env，不混入旧bootstrap CLI只允许的业务flags。

### R4.4 天枢biz/app→鉴神真实application前置（获批第四fixture、无新生产API）

现真实路径根为D/egon-cola-tianshu-admin/src/main/java/top/egon/cola/component/tianshu/admin/与JA/src/main/java/top/egon/cola/platform/tianquan/jianshen/admin/：

| Existing operation / owner | request/成功字段 | tx/前置/恢复 |
| --- | --- | --- |
| POST /api/v1/tianshu/bizs，controller/metadata/DdcBizController.save→service/metadata/DdcBizService.save，Bean ddcBizService | DdcBizBO Insert：bizCode/bizName非null各≤128，description≤512，enabled=true；publicId可缺由原repository生成，tenant/id/audit/version@JsonIgnore不由调用者指定。ResultRecord.data DdcBizResult完整id/bizCode/bizName/description/enabled/createdAt/updatedAt，id为公开ID | 原@Transactional；先query真实同code，duplicate冲突不覆盖；正式Tianshu USER scope TIANSHU_WRITE AND原CAP/细权限，不能借SERVICE当USER |
| POST /api/v1/tianshu/apps，DdcAppController.save→DdcAppService.save，Bean ddcAppService | DdcAppBO Insert：appCode/appName/bizCode非null各≤128，owner≤128、description≤512、enabled=true；publicId缺可生成。ResultRecord.data DdcAppResult完整id/appCode/bizCode/appName/owner/description/enabled/createdAt/updatedAt | 原@Transactional，biz必须真实已存在，appCode重复冲突；appCode是generated root namespace，owner明确approveddeploymentactor，不猜原应用 |
| TenantApplicationFacade.admit(Long tenantId,String actorId,AdmitApplicationAuthorizationScopeCommand(ddcApplicationId,displayPriority))，Bean tenantApplicationFacade | ddcApplicationId是真实上行DdcAppResult.id，不是appCode；displayPriority≥0；actor为真实JA用户/批准部署者，twoTenant逐个调用。返回ApplicationAuthorizationScopeVO id/business/appcode/status等原字段 | rbac3DdcCatalogGateway(RpcDdcCatalogGateway)→原DdcManagementClient.find/get真实enabled业务/应用；jpaTenantApplicationRepository.admit原@Transactional创建ApplicationPO+TenantApplicationPO，同已存在entitlement拒绝，不另直接DAO写 |
| 原HTTP POST /api/tianquan-jianshen/v1/iam/tenant-applications | 同AdmitApplicationAuthorizationScopeCommand；当前scope system:application:manage AND原RequiresPermission | 如果采用HTTP管理路径，需真实JA CLI USER AT并满足scope/PDP；runtime JA IT可直接复用同真实facade owner、文件/actor受R2验证，不新生产CLI/API |

**ID辨别**：当前JpaTenantApplicationRepository.toAuthorizationScope返回的ApplicationAuthorizationScopeVO.applicationId实际为tenant entitlement.getId，并不是Global ApplicationPO.id。随后registerDeploymentCatalog按真实applicationCode/业务owner查global应用，其ResultVO.applicationId才用于Role/field writer；checkpoint必须分别存ddcApplicationPublicId、tenantEntitlementId和globalApplicationId，不凭同名“applicationId”互换。

原DdcManagementClient已有get/list业务/应用及config upsert/publish，但**没有createBiz/createApp**，不能发明管理RPC。选用已存在上述HTTP owner时先CLI-004 phaseplatform→CLI-006正式Tianshu scope AT→CLI-004 phasenative。payload先Common原Insert group，成功后的公开id再由真实RPC catalog复读enabled/owner及namespace；不借旧prepare导入/SQLfake。首次 platform 冷启动先由新增 TA 夹具调用本模块原 DdcBizService/DdcAppService 建立基础真实目录，再由 JA bootstrap owner 的 bindResolvedApplications 验证并绑定/创建当前四应用；随后原资源 owner 建立资源，bootstrapResolved 复用原角色初始化。两步均复查真实目录，旧三参调用保留，未使用系统占位行才允许锁/CAS 绑定，已有业务依赖或异同绑定冲突失败；§12.50 获批后，当前编码缺行时先查固定系统初始 ID/旧编码，确认零业务依赖和初始权限未修改，才在同一 CAS 中绑定当前编码和目录 ID；保留应用 ID、权限 ID/permission_code/application_id，不改历史迁移或目录 publicId。当前与初始行都不存在才完整创建新行。准入、Business grant、显式角色激活及 snapshot/version READY 后才提交 platform receipt，缺输入明确失败，不关闭 eligibility。

Yuheng config/HMAC/Definition/route准备继续调用原已批准应用服务/管理接口；天枢业务/应用前置在CLI-004，不拖到CLI-005 aftercatalog。收到成功body仍核对真实lease/catalog/build/source，不手写SQL注入registration，不让CIfrontend MENU/ROUTE/ACTION scanner冒充API/GraphQL catalog。凡用于gate的native API/GraphQL/RPC资源必须通过上述真实部署目录登记并显式review/Grant。

## R5. 新库、namespace、TLS、端口及精确配置

### R5.1 冻结目标

默认单次run DB名如下；runId/ownership记录可以区分resume，**同名已由别的run占用则失败并由操作者明确选择新manifest名称，不自动drop**。总cap12，当前exact九项：

| role | new database | schema/topology owner |
| --- | --- | --- |
| idp | xingyuan_it_idp_20261011 | tianquan_oauth，既有单PRIMARY/NATIVE规则/managedmanifest与原Outbox |
| rbac | xingyuan_it_rbac_20261011 | JA原authoritative schema/transactions/projection |
| tianshu | xingyuan_it_tianshu_20261011 | Tianshu原schema/master data |
| yuheng | xingyuan_it_yuheng_20261011 | 原installation语义；不改成shared tenant schema |
| llm | xingyuan_it_llm_20261011 | 原LLM/知识owner，仅实际启用功能需要的DDL |
| light | xingyuan_it_light_20261011 | 真实generated Light原MP/sharding/DDL |
| web | xingyuan_it_web_20261011 | 真实generated Web组织业务原owner |
| service | xingyuan_it_service_20261011 | 真实generated Service评价业务原owner |
| agent | xingyuan_it_agent_20261011 | 真实generated Agent六层业务原owner |

Redis DB由prepare只读确认empty后固定，不能只因通常15不用就假定empty。AS/SDK/JA/Edge的数据库号全部一致。Key namespace显式前缀 xingyuan-it:<runId>；security ready/deny使用原issuerHash算法，由新issuer自然隔离，客户端不改Hash协议。audit stream/group、BFF session、logout stream/group、Tianshu env/namespace采用runId归属，不覆盖原namespace；spool每process不同。

| process | port | role/TLS |
| --- | ---:| --- |
| task nginx | 28443 | public HTTPS同源；task -p/-c/PID |
| Shoubing admin / Directory RPC | 28120 / 28122 | HTTPS / mTLS |
| Jianshen admin | 28130 | HTTPS |
| Tianshu admin / direct RPC | 28150 / 29080 | HTTPS / mTLS |
| Yuheng Admin | 28140 | HTTPS |
| Biz public / management | 28180 / 28182 | HTTPS，各自原authority |
| MCP public / management | 28185 / 28186 | HTTPS，原独立engine role |
| LLM | 28190 | HTTPS，原保护API；真实provider inference输入另列 |
| Edge backend | 28200 | HTTPS，public-origin仍28443 |
| Portal / Shoubing / Jianshen / Yuheng / Tianshu Vite | 28125 / 28121 / 28131 / 28141 / 28152 | 只loopback内部HTTP，外面统一HTTPS |
| generated Light HTTP / RPC | 28310 / 29310 | 仅原sample暴露协议，HTTPS/mTLS |
| generated Web HTTP / RPC | 28311 / 29311 | 原REST/GraphQL/nativeRPC，HTTPS/mTLS |
| generated Service management / businessRPC | 28312 / 29312 | 不创造businessHTTP；HTTPS/mTLS |
| generated Agent HTTP | 28313 | 原HTTP入口HTTPS；不发明AgentbusinessRPC |

process cap32、port1..65535且冻结列表不重复。Endpoint hostname用127.0.0.1/localhost和相应SAN，不改hosts。PUBLIC issuer固定28443/platform或/tenants/<real tid>；audience URI是主Speccatalog逻辑资源标识，不能按HTTP监听URI重新选一个公共aud。

### R5.2 TLS与同源配置

task CA/leaf PKCS12与PEM采用openssl/keytool；OAuth RSA签名key分开。私钥PKCS8，SAN覆盖127.0.0.1及localhost；mTLS EKU server/client按角色。Java进程专用truststore、Node NODE_EXTRA_CA_CERTS、curl --cacert；不改系统Keychain/JDK cacerts/systemnginx。浏览器只使用本任务profile的CA或本次Chromium精确leafSPKI pin；先openssl真实CA/SAN链校验，不ignoreHTTPSErrors/全局ignore-certificate-errors。SPKI模式不是生产系统CA验收，报告注明范围。

本机 nginx1.31.0 的只读nginx -V已证实http_ssl、stream、stream_ssl、stream_ssl_preread可用；本方案仍使用已批准**Redis local loopback plaintext例外**，不需要启动RedisTLS代理、不声称本次6379为rediss。原RPC/Tianshu采用真实mTLS；HTTP公共面HTTPS。private PKCS12 password只在0600properties/configtree，不能放-D/argv/JAVA_TOOL_OPTIONS（后者JVM会echo）或CIstdout。JVM专用truststore只含公开CA，运行前确认可无secretargv加载；privatekey/clientpassword由Spring SSL配置加载。不能为JDK信任实现自制TrustAll或修改全局cacerts。

任务nginx收到任何外部Forwarded/X-Forwarded-*先清除，发往Edge的Host/X-Forwarded-Host固定127.0.0.1:28443、X-Forwarded-Proto固定https、X-Forwarded-Port固定28443；不得从客户端Host/query派生这些值。nginx→Edge以及Edge→AS的后台HTTPS使用任务CA并核SAN；身份协议后端可使用原server.ssl.client-auth=need与专用clientcert，防止浏览器直连后台端口伪造代理头。SCG4.3.5实际spring-cloud-gateway-server JAR的HttpClientProperties.Ssl/HttpClientSslConfigurer已核具备key-store、key-store-type、key-store-password、trusted-x509-certificates及ssl-bundle/keyManager，use-insecure-trust-manager默认false；复用其spring.cloud.gateway.server.webflux.httpclient.ssl.*保护配置，不新TLS框架。nginx自己的proxy_ssl_certificate/key/trusted_certificate/verify也仅taskfiles，不reload系统nginx。mTLSclientcertificate对外不提供，upstream健康检查需对应taskmonitorcert，不降低client-auth。

SAS本身也不信任外部Host来选issuer：当前AuthorizationServerConfiguration.multipleIssuersAllowed(true)后，ExactIssuerContextFilter取协议path并以配置publicOriginRoot拼接，再经TenantIssuerService.resolveExactIssuer查真实registry，覆盖AuthorizationServerContext issuer并finally恢复。任务必须保留这一顺序、路径不rewrite成根OAuth、publicOrigin固定28443；proxy/spring.forward处理只服务正确publicredirect/callback，不覆盖trustedrealm判定。每issuer元数据的issuer/authorization_endpoint/token_endpoint/userinfo_endpoint/jwks_uri及回调iss都逐项exactcanonicalHTTPS核查。Origin/Host/Forwarded伪造case不能改变选定tenantissuer/redirect，否则live失败。

nginx安全access-log仅method、$uri（不含query）、status、受限request-id；不得$request/$request_uri/$args或Cookie/Authorization/POSTbody，避免code/state/verifier/password泄露。Spring/NettyDEBUG wire logging不得用于token/login链；test报告存有限脱敏请求元数据。 脚本在 bootstrap/nginx -t 前复核 task PID、私有日志/TLS 路径、禁止外部 include/module、固定 loopback listener、冻结上游与 proxy_ssl_verify；内部端口健康探测使用原 task 双用途 leaf 做 mTLS，公共28443不附加客户端证书。

同公共origin：
- /bff/**、/oauth2/authorization/**、/login/oauth2/code/**经真实Edge。
- /api/**经真实指定resource registration TokenRelay→Biz→resource；/mcp/**→MCP。
- /platform/**和/tenants/**在任务nginx直接HTTPS AS，保证Edge的Boot issuer-uri自动发现先可访问canonical28443；AS原标准CSRF/PKCE/consent/token校验不变，不绕过SAS。生产Edge已存在ASroute保留；隔离gate需用task私有mTLS连接Edgebackend的同protocolroute另作功能验证，不能把因启动环直通AS当“AS经Edge route”已测。BFF/API/MCP仍统一经过实际Edge。
- Portal根与/apps/<platform>/映射实际Vite/Router base。每frontend默认standalone /保留；新base只能来自固定部署配置，不能用户query。
- nginx/Edge/AS只信任已明确loopback代理的Forwarded/publicHost，不接受外部任意header决定issuer。验证callback/metadataissuer exact28443，不能以localhost/后台28200换掉publicURI。
- productionresource binding由主Spec实现：registration→resource、route→registration、server-owneddefault identity registration；callback同iss/sub/tenant+sessiongeneration，资源未授权401引导该fixedregistration。不能请求给任意resource，也不把5UI变成5个独立Edge/session绕过同BFF合同。

### R5.3 精确Spring键、env与profile parity

值展示均为非秘密引用；密码内容只由保护configtree/properties/env加载。新core键在宿主base/local/test/dev/prod等现存profile结构保持一致，值可不同；外部free test关闭观测/连接，不能在live关安全。task生成properties覆盖同一core键集，不引入生产integration backdoor。

| Exact key / owning prefix | base/prod | local task | controlled test | 来源/约束 |
| --- | --- | --- | --- | --- |
| egon.cola.platform.tianquan.shoubing.usage-observation.enabled | host接入true；profile显式 | true | false | 既有properties |
| egon.cola.platform.tianquan.shoubing.usage-observation.receiver | fixedreceiver | SHOUBING/JIANSHEN/TIANSHU/YUHENG_ADMIN/BIZ/MCP/LLM/EDGE/APPLICATION按host | 同hostreceiver | 扩展已有OAuthUsageObservationProperties，固定enum，不放IdpStarterProperties、不动态tags |
| egon.cola.platform.tianquan.shoubing.usage-observation.fallback-issuer | trustedHTTPSreceiverissuer | exact本任务trustedissuer | 明确testURI，无外部调用 | 不能从未验证token选issuer |
| egon.cola.platform.tianquan.shoubing.usage-observation.allow-local-plaintext | false | true且local+DEPLOYMENT_ENV=local，仅loopback例外 | false | issuer/trustedJWKS仍HTTPS |
| egon.cola.platform.tianquan.shoubing.trusted-issuers.[key].issuer/jwks-uri/realm-key/realm-type/tenant-id/status/trust-version | explicitregistry | 3actualrealm，TENANTtid真实 | 明确fixture且disabled网络 | IDs decimalstrings，无默认根issuer |
| egon.cola.platform.tianquan.shoubing.resource-server-id/resource-uri/platform-audience | exacthostbinding | exactcatalogresource/platform | explicitfixture | 不公共aud替代分资源 |
| egon.cola.platform.tianquan.shoubing.revocation.primary-endpoint/mode/timeout/epoch | rediss/STRICT_SHARED/≤原时限/realepoch | redis://127.0.0.1:6379/STRICT_SHARED/原500ms/runId | 同结构disabled连接 | endpoint无credentials/query |
| egon.cola.platform.tianquan.jianshen.runtime.redis-address/database/password-file | operatorendpoint/DB/secretref | sameisolatedRedisDB/protectedreference | externalfreeconfigured | AS投影/SDKaudit alias必须使用此真实client，同DB |
| egon.cola.platform.tianquan.jianshen.authorization.endpoint | trustedHTTPS | https://127.0.0.1:28130 | explicitdisabledfixture | PDP不可故障降级 |
| egon.cola.platform.tianquan.jianshen.user-scope-policy.enabled/permission-scopes | host显式enabled及有限本地permission→scopeSet | true；各host实际whitelist，native按publishednamespace | false及同结构boundedfixture | 扩展已有Rbac3StarterProperties，map≤128、每值1–64唯一scope；unknownpermission拒绝，scopeAND原PDP |
| egon.cola.platform.tianquan.shoubing.audit.stream/spool-directory/spool-max-bytes/hmac-key-file | exactstream/ownvolume/cap/key | xingyuan-it:<runId>:oauth:audit:v1/独占spool/<原cap>/32rawkey | tempfixture/原cap/key | auditclient alias，不另建意外连接 |
| egon.cola.platform.tianquan.shoubing.audit.delivery.receipt-ttl/confirmation-timeout/batch-size | PT1H/PT30S/100 | 同值 | 同结构bounded | existingdeliveryproperty |
| egon.tianquan-shoubing.identity-directory.platform-storage-partition-id | actualP | manifestP | fixtureP | >0且≠tenantIDs |
| egon.tianquan-shoubing.oauth.public-origin/platform-management-tenant-id | canonicalHTTPS/actualtid | 28443/PhaseAactualdefaulttid | explicitfixture | 管理tid不是P |
| egon.tianquan-shoubing.oauth.key-encryption.key-id/key-file | approvedkey/ref | taskkeyID/32raw0600 | temp32raw | exact原AES/GCM owner |
| egon.tianquan-shoubing.oauth.local-explicit-service-registrations | false | true，仅AS避免自身listener前discovery | 明确false/fixture | realexplicittrustedendpoints，不生成fakeclient |
| egon.tianquan-shoubing.development-bootstrap.enabled | false | 原bootstrapharness阶段只显式true或直接service；运行不隐式autoGrant | false | 不在production启动创建fixtures |
| egon.tianquan-jianshen.development-bootstrap.enabled/tenant-ids/identity-sub | false/explicit | fixturephaseexplicit；IDs真实directory | false/testfixture | member验证优先 |
| egon.tianquan-jianshen.development-bootstrap.auto-activate-local-admin-roles | false | 仅已审核testpolicyexplicit，原SOD/roleactivation仍验证 | false | 不给示例默认全权 |
| egon.cola.component.rpc.tls.enabled/development-plaintext/certificate-chain-path/private-key-path/trust-certificate-collection-path | true/false/operatorfiles | true/false/taskPEM | disabledprotocol明确 | exact原EgonRpcProperties.Tls |
| egon.cola.component.rpc.provider.bind-address/port | realprovider | 127.0.0.1/manifestRPCport | disabledprovider | 无foreignport |
| egon.cola.component.tianshu.rpc.target/tls.enabled/tls.development-plaintext/tls.certificate-chain-path/tls.private-key-path/tls.trust-certificate-collection-path | trusted/mTLS | dns:///127.0.0.1:29080/true/false/taskPEM | disabled | DdcRpcProperties原owner |
| egon.cola.component.tianshu.tenant-id/env/namespace | explicit | actualtid/local/runIdnamespace | explicitdisabled | 与HMAC/lease源一致 |
| server.address/port/ssl.enabled/ssl.key-store/ssl.key-store-type/ssl.key-store-password | operator/true/PKCS12/private | 127.0.0.1/manifest/true/taskfile/PKCS12/private | controlledHTTPfixture按原profile | 不能把密码值写Spec或argv |
| spring.security.oauth2.client.registration.[id].client-id/client-secret/provider/authorization-grant-type/client-authentication-method/scope | explicit | USERCONFIDENTIAL code+basic；SERVICECC+basic；leastscope | disabled/fixture | secret在protectedproperties；不同微服务clientID |
| spring.security.oauth2.client.provider.[id].issuer-uri/token-uri/jwk-set-uri/authorization-uri/user-info-uri | canonical | exactruntimeissuer路径 | explicitfixture | sameissuer，local-explicitAS不得selfdiscover |
| egon.cola.platform.tianquan.shoubing.service-client.registration-id/app-id/tenant-registration-ids.[tid]/credential-versions.[registration] | realrefs | platformcontrol/tenantbusiness分别选 | disabled/fixture | CC无RT，保留原缓存和singleflight |
| egon.yuheng.edge.public-origin/default-registration-id/allowed-return-paths | canonical/fixed/safelist | 28443/Edge附录固定默认入口/Portal与四mount安全路径 | explicitfixture | 默认入口语义及alias/actualregistration以Edge附录为唯一owner；ACTIVE按canonicalissuer唯一解析，同session不重置absolutetime |
| egon.yuheng.edge.upstreams.authorization-server/biz-gateway/mcp-gateway | trustedHTTPS | HTTPS28120/28180/28185 | fixture | 不trustall |
| spring.data.redis.url/database/username/password/timeout | sameprimary | same6379/isolatedDB/existingcredential/ref/500ms | disabled | EdgeSessionRedisConfiguration要求endpointsame，DB也要同 |
| spring.session.redis.namespace | egon:bff:session:v3 | xingyuan-it:<runId>:bff:session:v3 | fixture-v3 | 主Edge新binding schema；不复用旧v2session |
| egon.yuheng.edge.session.encryption.key-id/key-file | operator | taskID/32raw0600 | tempfixture | 不能Base64text |
| egon.yuheng.edge.logout.stream/consumer-group/max-pending/poll-interval/claim-idle/timeout | existingbounded | tasknamespace/group/10000/PT5S/PT30S/PT5S | samebounded | worker真实durableintent，noRTlog |
| management.endpoint.health.group.readiness.include | existing+idpguard组合 | 同hostactualdependency AND guard | fixture无外部 | 不把liveness当readiness |

命名校准：receiver新键只属于OAuthUsageObservationProperties。RPC/control-plane的新localbinding键及Edge registration/route-resource map，以主 SDK/Edge详细合同为唯一owner；runtime writer逐字消费该已冻结键表，不在本附录另命名第二组键。不声明生产receiver配置已存在，实施需真实Binder/metadata/constructor/profile测试。

以下新增binding键已按[Edge附录](2026-10-11-10-26-xingyuan-native-full-integration-edge.md) §8.3/8.4逐字冻结，base/local同结构，外部free测试用typed fixture、不启动Redis：

| Exact key | local运行值/约束 |
| --- | --- |
| egon.yuheng.edge.resources.<resourceAlias>.resource-uri | 1–64逻辑resource alias map；真实HTTPS单Resource；alias变resource不得混旧client |
| egon.yuheng.edge.resources.<resourceAlias>.default-registration-id | DEC-008待批准；真实default tenant已提交registry中的唯一actualregistration；只NEW alias入口可选，不给ACTIVE缺binding回退 |
| egon.yuheng.edge.resources.<resourceAlias>.issuer-registration-ids | DEC-008待批准；有限map，Spring YAML key使用quoted bracket形式 "[https://127.0.0.1:28443/tenants/<actualId>]" 保留exact可信canonicalissuer URI，值为该issuer/resource真实registry的actualregistrationId；base/local同结构并以真实Binder验证，不能用tenantCode/header或URI规范化猜键 |
| egon.yuheng.edge.resources.<resourceAlias>.route-ids | 0–64唯一routeID；Portal/session identityalias可为空，business必须有route |
| egon.yuheng.edge.resources.<resourceAlias>.request-paths | 0–64固定本地绝对path；禁止裸/**/路径穿越/同权重歧义；实际route唯一resource |
| egon.yuheng.edge.resources.<resourceAlias>.scopes | ≤64唯一；openid/offline_access必需，profile可选，加该resource已批准业务scope；与两issuer各自Client/Grant子集一致 |
| egon.yuheng.edge.resources.<resourceAlias>.return-path | server allowedReturnPaths中的固定Portal或mount路径 |
| spring.cloud.gateway.server.webflux.routes[].metadata.edge-registration-id | 固定逻辑resource alias，exact匹配上表route及目标Resource；切tenant不改route/metadata |
| spring.cloud.gateway.server.webflux.routes[].filters | EdgeResourceAuthorization=<resourceAlias>先按ACTIVE canonicalissuer解析唯一actualregistration；其后TokenRelay显式使用resolved actualregistrationId，不能TokenRelay=<alias>或defaultfallback；AS协议route无relay，外部Bearer不替换 |
| spring.cloud.gateway.server.webflux.httpclient.ssl.key-store/key-store-type/key-store-password/trusted-x509-certificates/use-insecure-trust-manager | taskclient PKCS12/privatepassword/taskCA/false；原HttpClientProperties能力，仅保护配置不argv |
| server.ssl.client-auth | 身份协议后台按taskmTLS选择need，PUBLICnginx无client-auth；原backend预期必须与健康clientcert一致 |
| server.forward-headers-strategy | Edge/AS只在R5.2清洗后的taskmTLSproxy入口使用framework，固定publicHost/proto/port；普通RS默认原值，不由browserHeader决定issuer/tenant |
| egon.cola.platform.tianquan.shoubing.rpc.consumer.bindings.<localId>.service-name/group/version/full-method-name/process-identity/mode/app-id/resource-uri/scope-context/scopes | 每项单fullMethod、本地可信selector，真实targetnamespace/scope；非请求输入；未知/重复出站前拒绝 |
| egon.cola.platform.tianquan.shoubing.rpc.consumer.bindings.<localId>.registration-id | 仅PLATFORMcontrol-plane的fixedregistration |
| egon.cola.platform.tianquan.shoubing.rpc.consumer.bindings.<localId>.tenant-registration-ids.<verifiedTenantId> | TENANT业务按已验证tenant选择exactlocalregistration；无默认tenantfallback |
| egon.cola.platform.tianquan.shoubing.rpc.provider.methods.<exactFullMethodName>.required/principal-kind/scope-context/required-scopes/expected-resource-uri | fixednative methodbinding；接入required=true；expectedaud/leastscope；reject前provider零调用 |

RPC字段使用原Properties已约定的Java camelCase字段所对应的canonical kebab-case YAML（service-name/full-method-name/process-identity/app-id/resource-uri/scope-context/tenant-registration-ids/principal-kind/required-scopes/expected-resource-uri）；绑定identity不得拿HTTPheader/Proto/body推导。runtime-generated properties按最终SDK typedmetadata证明同键，不额外命名第二套DTO或transport协议。

GatewayRuntimePolicy.configuration.requiredOAuthScopes是**签名route/definition发布数据**，不是另一个Spring环境键；每protecteddefinition有1–64唯一ASCII1..128 scope。原publisher/compiler/codec的signedhash覆盖configuration，mandatoryscopeprovider是authenticationprovider声明的依赖，先mandatory全ALLOW再普通ANY/ALL，不破坏IDENTITY_PROTECTED的ordinary空名单。runtime只按真实owner生成并发布新taskdefinition，不能给旧Grant或旧snapshot注入defaults。fixture目录checksum与runtime-scopes只读核对同artifact的实际值。

现存profile覆盖清单（只核已新增/变更core键，不为缺少的profile造文件）：SA application.yml/application-local.yml/application-oauth-migration.yml，JA application.yml/application-local.yml及src/test/resources/application-local-it.yml，Tianshu Admin application.yml/application-local.yml/application-test.yml，Yuheng Admin application.yml/application-local.yml，Biz/MCP各application.yml/application-operations.yml，LLM application.yml，Edge application.yml/application-local.yml，Light/Web/Service/Agent各source实际base/dev/test/prod。前两host自身已完成的core表保留；迁移/offline/test的观测或依赖enabled=false有同结构明确配置，不能删receiver/fallback/delivery关键层次。没有application-test的Edge等采用typedfixture验证，同结构core目标和值由各详细附录定义。

| Exact env | contract |
| --- | --- |
| XINGYUAN_ISOLATED_INTEGRATION | only opt-in mutation/live，不控制production安全 |
| XINGYUAN_INTEGRATION_MANIFEST | absoluteowner-onlymanifest；JUnit/Playwright同hash输入 |
| SPRING_CONFIG_ADDITIONAL_LOCATION | file:task/env/<process>.properties；旧bootstrapCLI业务flags之外传入 |
| SPRING_PROFILES_ACTIVE / DEPLOYMENT_ENV | local / local，明确受限Redis例外 |
| NODE_EXTRA_CA_CERTS | task CA，只Node进程 |
| VITE_BASE_PATH | 主前端附录新增固定mount path，default /；Router同值 |
| VITE_OAUTH_REGISTRATION_ID | 五前端固定逻辑alias：Portal edge-ui、授柄edge-shoubing、鉴神edge-jianshen、玉衡edge-yuheng、天枢edge-tianshu；值/现key不因tenant切换改变。DEC-008待批准：先全部logout，NEW通过已有显式actualtenant B registration路径正常登录；ACTIVE各alias由canonicalissuer B解析真实B client，query/header不选issuer/resource。createGatewayAuthClient options仍用已安装0.2.5，不发布新common包 |
| VITE_YUHENG_ORIGIN | fixedpublicHTTPS28443，shared0.2.5正常installedartifact |
| TIANQUAN_SHOUBING_BOOTSTRAP_PASSWORD_FILE | taskprivate passwordreference；program内读取，noarg/noecho |
| EGON_TIANQUAN_SHOUBING_OAUTH_KEK_ID / _KEK_FILE / _AUDIT_HMAC_KEY_FILE | manifestrefs映射原egon属性；32raw0600 |
| EGON_YUHENG_EDGE_TLS_KEYSTORE / _TLS_KEYSTORE_PASSWORD / _SESSION_KEY_ID / _SESSION_KEY_FILE | 原Edge env合同，保护加载，不写argv/log |
| EGON_YUHENG_EDGE_OIDC_CLIENT_ID / _OIDC_CLIENT_SECRET / _OIDC_ISSUER | 真实confidential USERregistration；新多资源表由主Edge附录扩展 |
| TIANSHU_TENANT_ID / TIANSHU_REDIS_USERNAME | 当前原DI合同，真实tid/明确ACL用户名；不猜Tenant为default |

运行变量不是新增框架。保护properties/env写法复用原Java Properties/shell quoting规则；typedallowlist避免把secret newline当命令执行。不得source外部任意env脚本或将JSON.stringify当shell quoting。

## R6. 真实浏览器、HTTP、RPC与验收矩阵

### R6.1 Playwright与formal token协议

Runner从 YW/node_modules/@playwright/test/package.json 实际解析1.62.0，config/test位于YW使普通Node resolution可用；不Portal pathalias/新依赖。playwright.integration.config.ts无webServer、retries=0、liveoptin/manifest必需，trace默认off，密码输入/Code/token网络response不保留。仅安全UI状态截图，报告不包含password字段值。启动script运行真实已配置process；tests不代启动另一个fixturestack。

example execution（实现后执行，目前仅设计）：
    XINGYUAN_ISOLATED_INTEGRATION=true XINGYUAN_INTEGRATION_MANIFEST=<taskpath> npm --prefix egon-cola-xingyuan/egon-cola-yuheng/yuheng-admin-web exec -- playwright test --config playwright.integration.config.ts

CLI Code按tenant+client+单Resource：
SecureRandom verifier/state/nonce→S256 GET exacttenant/oauth2/authorize→真实AS form login_csrf→真实consent→callback验state+iss→服务端code/verifier/tokenPOST→privateAT/index。PUBLICClient无RT。每目标/租户/用户分别走独立family；不得把一个tokenindex当“一个AT可四资源”。code/tokens不截图、不log；取消不能自动approveconsent。

BFF Code同canonical Session：
首次Portal identityregistration真实code/PKCE；每资源首次registration另正常consent/token，保存于原encryptedAuthorizedClientRepository，compare canonical issuer/sub/tenant/sessiongeneration及absolute createdAt。其他资源成功不重置session；不同身份/过期/latecallback拒绝；原server-ownedTokenRelay按route选择exactregistration。AScookie复用只省密码输入，不绕过每Client/resource授权。Browser只__Host Session，userInfo不证明目标资源已授权。

DEC-008待批准的两tenant浏览器recipe：先以default alias正常建立tenant A session并访问五页面；A全部heldfamily oracle就绪后真实CSRF logout至CONFIRMED。确认NEW后，浏览器通过registry中已批准的tenant B实际identityregistration走原authorization路径，真实AS login/consent/code产生issuerB canonical session。随后使用同五VITE alias、同Portal/Wujie入口及同businessroute依次正常授权；server按issuerB映射实际B clients，每次原bootstrap/list必须实际tenantB、singletonaud和scope∧PDP成功，同session/createdAt不变。ACTIVE显式tenant A registration/缺issuerB binding必须拒绝，不能defaultfallback；不通过CLI B AT、替换cookie、page.route或fakeJWT冒充BFF。继续B的真实临期refresh、prompt=login/max_age=0 stepup与全部B族logout；每阶段pending alias/realreg/issuer/generation及USED proof只保留安全refs，实际AT只由R6.1.1原AS内存oracle核实。

### R6.1.1 BFF 实际 held AT / 授权族撤销 oracle（TEST-025）

PUBLIC CLI 的授权码产生自己的 authorization family，它的 AT **不是** BFF另一次code flow的held AT。BFF logout 后，只看Cookie/session401不足以证明AS已撤销BFF族，也不能把独立CLI AT重放401当成正确预期。CLI自己的token/family撤销在独立case测试，未撤销CLI族应保留原合法状态。

oracle只复用SA本任务新库的IsolatedOAuthProvisioningIT/IsolatedOAuthProvisioningComponent，不新增生产端点、额外oracle fixture或浏览器/BFF导出能力。实际source是oauth/service/impl/TenantAuthorizationServiceImpl.java、Bean **tenantAuthorizationService**，实现标准OAuth2AuthorizationService；不是不存在的NativeOAuth2AuthorizationService类名。findById(String id)在@Transactional(readOnly=true)内通过tenantIssuerService.resolveCurrentIssuer取trustedrealm、oAuthAuthorizationRepository.findById只接受ACTIVE且未absoluteexpired，loadCurrentTokens走真实repository/cipher，再SasAuthorizationConverterImpl materialize原OAuth2Authorization。

加密存储已从真实source核实：token/domain/po/OAuthTokenPO的tokenCiphertext、metadataCiphertext不供测试自解析；OAuthTokenRepositoryImpl.findByAuthorization→toTarget→OAuthTokenConverter.tokenValueFromStorage调用原OAuthPayloadCipherComponent.decrypt(trustedRealm,"token-value",tokenDigest,ciphertext)，metadata同原token-metadata目的域；TenantAuthorizationServiceImpl.loadCurrentTokens按原规则选每type当前token。helper只用标准findById的materialized access token，不能另写AAD/envelope/parser，也不能拿原授权的旧rotation版本假作当前BFF held族。tenantIssuerService和AuthorizationServerContextHolder必须真实issuer作用域，finally恢复调用前context；不产生HTTP身份或公开后台查询入口。

1. R2双guard验证新AS DB/OID/runId、保护KEK/HMAC与exacttrustedrealm。测试浏览器完成真实resource authorization和一次目标API成功使用，产生已确认TOKEN_USE_VERIFIED（既有USED语义）审计receipt。以本次已知registration/clientPK、真实用户subject、realm/tenant/resource、action correlation与有界时间窗建立matcher，不查询任意其他用户。
2. 通过真实OAuthAuthorizationMapper.selectByPrincipalName(storagePartitionId,principalName,"ACTIVE",afterId,limit)有界PRIMARY读取候选，limit≤500、按id分页，总候选≤500；按clientPK、grantType=authorization_code、audiences exactsingleton、时间窗过滤。不是guess latest，不加生产query/新SQL；超上限或0/多匹配fail-closed。
3. 在保存/恢复AuthorizationServerContextHolder的try/finally中设当前已验证issuer+原AuthorizationServerSettings，使tenantAuthorizationService.findById(actualDecimalId)依真实trustedrealm装配。**在logout前**读取：findById会在授权已REVOKED后返回null，不能以null伪证明拿到了旧AT。
4. OAuth2Authorization.getAccessToken().getToken().getTokenValue只存helper内存。用本次真实完整local verifier取得iss/sub/aud/jti/type/expiry/authz id和原TokenUsageAuditComponent.reference/HMAC proof，验证client/sub/realm/resource与唯一候选及确认USED audit tokenRef/jtiRef吻合。候选、实际token、使用proof三者必须exact唯一，不能从browserstorage/cookie/BFFSessionRedis提取AT/RT。
5. 同helper以真实HTTP把该AT重放到相应实际资源原read入口，确认logout前200/原成功业务body；暂停等浏览器真实CSRF logout完成。报告只写安全hash/阶段/status，rawAT不stdout/file/report/trace，不记录Header或body。协调文件只用无secret的READY/LOGOUT_COMPLETE marker与proofRef。
6. 浏览器logoutCONFIRMED后，helper用**同一个尚未过期的原AT**再次真实HTTP重放，确认401且业务handler0。新连接查询原AUTHORIZATION deny/authorization/token.revokedAt及严格共享epoch/projection为已应用，可补充证据但不代替同AT request-level拒绝。PENDING/UNKNOWN阶段不要求remote已完成；localclear仍必须完成，remote恢复确认后再同AT重放。若AT已自然到期、refresh换过而matcher不指最新held族、找不到exactproof或correlation，case失败并重做fresh正常flow，不“凑401”。
7. 每资源heldfamily分别验证；并发refresh/logout按主Edge生命周期/fence门禁取得最终held族。oracle内存退出即释放，尽量擦可变buffer；JavaString不可保证擦除，禁止留长生命周期field/secretcache。私有CLI族单独调用标准真实revoke或原owner，再使用它自己的AT断言。无需token-exchange、假JWT、从session导出或放宽aud。

真实同DS JPA+MP Outbox原子性先依DEC-007获准的最小组件修订完成，原当前组件只接受DataSourceTransactionManager，**不能假定JA JpaTransactionManager已支持**。对应review步骤须证明同一连接enlist、业务/epoch/Outbox同时rollback及aftercommit再投影；不可用双TM/REQUIRES_NEW/复制OutboxDAO。未批准/未GREEN前不得执行并宣称native field/projection真实准备通过。

观察器实现仍位于原 SA IT：独立 NONE context 只装配原 tenantAuthorizationService、token issue/read repository/cipher、完整每 resource SDK verifier 与原 HMAC receipt owner，无生产 provider/filter/scheduler。使用已验证 shoubing profile/registry SHA 与同任务 Redis，观察器自身 spool 独立，不能打开/重放主 AS spool。原 SAS Converter 的依赖名称沿实际 Spring Bean。

浏览器各阶段通过一次原 /bff/session 触发各未过期 client 的实际 verifyClient/USED，记录纯时间窗；SA 只在实际 realm/已知用户/client/resource 上匹配唯一事件及 STORED receipt，再读原 owner materialized 当前 token。BEGIN→当前候选窗口上限900秒，以覆盖合法300–600秒刷新等待；最多500候选/审计行，不能取 latest。REFRESH 必须另有原 IdP REFRESH_TOKEN_ROTATED，使用原 clientPK 摘要（不同于 USED 的 clientId 摘要）；新 code 或仅新 issuedAt 不替代刷新。STEP_UP 核原 owner 的当前 ID Token auth_time、issuer/sub/audience。四业务只读接口分别保留原 raw-list、ResultRecord、PageResultRecord 形状，不统一改 API；重放 body 由 JDK 分块 consumer 限128KiB，HTTP超时仍覆盖整个响应。

Portal edge-ui 原资源没有业务 route/path，仅是 OIDC 身份入口，其族使用原 AS UserInfo 做同 AT 200(sub)→401，并明确记为 IDENTITY；不以其 UserInfo 证明四资源业务权限。四实际资源族各走原 scope∧PDP API。HOLD 只有五个真实 baseline200 都完成才发布 READY，原 AT 留在 helper 局部内存；收到真实 logoutCONFIRMED 后逐个同请求重放，401返回之后再检查仍未到期，不能用自然过期充数。REFRESH/STEP_UP/HOLD/REPLAY 分别投影4/1/5/5份安全 tuple/hash/status 证明，由 MapStruct/BaseForward 显式排除 token/subject。源默认测试的 mocks 只证明边界/类型，所有真实 owner/HTTP/PG/SAS 的执行仍须 Step18。

各 verify 尝试可用 task evidence/verify-UUID 独立目录，由 xingyuan.isolated.evidence-directory 与 XINGYUAN_VERIFICATION_EVIDENCE_DIRECTORY 共同指定，0700/owner/no-symlink/run/hash 验证。原手工缺省 evidence 兼容；旧失败尝试文件保留，不混读/覆盖。此为私有 test coordination，无新生产 API。

两个旧compatibility IT沿用同目录原helper的手工enabled入口供旧TopologyIT，隔离模式另以XING显式开启并核manifest/registry/token-index及shoubing profile SHA；只读复用原严格Python manifest validator，不启动设施。当前tenant_id/client_id/USER/TENANT/singletonaud从原SDK TenantIssuerAuthenticationManagerComponent验签后核对，并真实调用UserInfo及受保护资源；本地claims解析不当授权成功。独立A/B formal族不冒充BFF。原native namespace从已SHA绑定审核目录得出，不使用/api/mock/read。

旧撤销用例对该formalAT先真实200，再发无token内容的摘要绑定READY请求；必须等原SAowner的CONFIRMED+denyApplied确认，核run/hash/issuer/clientPK/tenant/resource及authorization摘要和时间后，同AT401且仍未到期。不能自行写CONFIRMED。原native实际SQL隔离证明还需包含A/B行hash与伪造header返回行hash=A，缺证明失败；两个兼容用例不替代完整LIVE013/007。共享test helper为必要直接依赖；suite仅添加仓库已有managed Lombok到test scope，不变版本/生产依赖，保留规定DTO/构造/日志风格。

PUBLIC正式族的撤销确认仍由原SA IT的独立opt-in revokeFormalFamily入口承担，不创建新生产接口。原TokenRevocationService对ACCESS只撤JTI、CODE/REFRESH撤authorization family；PUBLIC无RT，故由原repository在已验证的同一authorization内取唯一CODE digest，交原revoke命令，不重放code、不revokeSubject/client。原SDK验签/issuer与当前owner access-token摘要、PUBLIC USER clientPK、租户、资源、subject、authorization摘要及未到期检查全部一致后才允许该事务。CODE缺失/多条/foreign/type错拒绝；RUNNING凭据保留未知结果，后续只读reconcile，不盲重做。原shared deny reader已REVOKED且原SASfindById不再ACTIVE之后才发布CONFIRMED；默认关闭/受控测试不证明实际撤销生效。此入口目前还需接入正式验证runner的短进程桥，不能把它的源码存在当端到端用例完成。

### R6.2 必需case与主Spec trace

| RT Test / 主Test | action | 成功/拒绝 authoritative evidence |
| --- | --- | --- |
| LIVE-001 / TEST-030 | unsafeDB/root/OID/PID/port/Redisowner/manifest | 实际writer前拒绝、无SQL/kill/配置覆盖；regression与live原设施保留 |
| LIVE-002 / TEST-030/034 | phase事务失败、unknowncommit、secretfile失败及JPA/MP同事务 | 真PGrollback/新连接reconcile、无halfrealm；COMMITUNKNOWN停、不盲rotate；full必须执行OutboxTransactionRollbackIT的jpaRoleResourceAndMpOutboxRollback、jpaRoleResourceAndMpOutboxAppendFault、jpaRoleResourceAndMpOutboxCommit三case（实际selector #jpaRoleResourceAndMpOutbox*），同Hibernate/MP连接和backend PID/txid、独立事务回读；三case全部非0执行/0skip，默认不启动数据库 |
| LIVE-003 / TEST-020 | 3issuer discovery/JWKS、badrootissuer | canonicaliss、RS256≥3072各realm不同kid、无privatekey、原retiredroot410 |
| LIVE-004 / TEST-020 | Portal真实登录 | realCode/S256/state/nonce/exactredirect+iss，HTTPOnlySecure session，browserstore无AT/RT/password |
| LIVE-005 / TEST-020–021 | Portal→授柄/鉴神/天枢/玉衡 read/管理页，分别在真实A/B BFF session执行 | 每tenant每资源不同CONFIDENTIAL AS clientId、独立实际AT/RT、24/55/20/9scope及singletonaud；先A全部logout→NEW显式实际B registration真实Code→同五逻辑alias/同route均issuerB/tokenB；同issuer/currentpublicsub/tenant相同、sameBFFsession/createdAt不变；各bootstrap及一真实list成功，无第二密码输入（normalconsent仍验证），permission独立；CLI B AT不算BFF证明 |
| LIVE-006 / TEST-022 | admin/低权viewer、移除resourcegrant、PDP有grant但缺业务scope | 真API200/403、scope AND APIResource/permission、denybusiness0调用；UIbutton隐藏不算backend保护 |
| LIVE-007 / TEST-023–024 | 两tenant真实BFF切换及CLI独立输入、错误aud/伪造X-Tenant头 | A全部logout后NEW显式B正常登录，同五alias/route实际issuerB/tokenB/tenant_id B；ACTIVE显式A或缺B binding拒绝无defaultfallback；同真实用户两个canonicalissuer对应真实tenant，错aud401、无SQL入错tenant；CLI inputs另测，原installation映射保持 |
| LIVE-008 / TEST-020/024 | A/B实际session的refresh、realstepup与高风险role意图 | B从logoutA→explicitB真实Code开始，同alias/route的实际B refresh及prompt=login/max_age=0推进auth_time，pending alias/realreg/issuer/generation恢复；不切回A/default，不重置absolutecreatedAt，不fakeSTRONGprincipal |
| LIVE-009 / TEST-001–004/023 | badsignature/iss/typ/aud、共享stateUnavailable、多次请求 | local验签固定metric/JWKS/counter；401/503，非每请求AS；finally下个请求tenant/proof不泄露 |
| LIVE-010 / TEST-023 | formalPUBLIC CLI每resource两tenantAT | 实际AS PG授权code/tokenfacts、精确scope/aud/tokenRefs与outputfile；noFakeJWT/noBFFexport |
| LIVE-011 / TEST-011–013 | 各SERVICEClient HTTP/RPC出站与临期cache | differentclient_id/leastscope/resource、真实token申请/cache复用/单飞再申请，CC无refresh_token |
| LIVE-012 / TEST-027–029 | 四非Open真实generate/build/schema/architecture | source-projects修改→manifest全7派生→四真实生成项目；三Open源码/生成内容不变，不能运行source代生成 |
| LIVE-013 / TEST-027–028 | Light/Web REST/GraphQL及MP查询/command | 真JWT/tenant/PDP，denied0write、SQLtenant隔离、asyncContext/finally |
| LIVE-014 / TEST-011–013/028 | Service↔Web nativeUnary/directregistry | 真实Tianshu lease/HMAC/catalog/fullMethod绑定；SERVICE可信tenant/scope；missing/revoked/authzdeny正确gRPC状态且零providerhandler |
| LIVE-015 / TEST-028 | Agent unified身份与platform调用 | 原六层业务不变、真实Client/resource/token/inboundguard；未提供外部LLM不宣称inference已验 |
| LIVE-016 / TEST-014/024 | 正WRITE/无rule/READ/definitionmax跨tenant | 实际replaceFieldRules事务+ROLEevent+projection/activation≥新version，fieldpolicy=WRITE后业务写；空/无WRITE403且零write，不fakeDefaultWRITE |
| LIVE-017 / TEST-022 | LLM受保护协议/知识管理 | 接入真实guard/scope/permission/tenant，正确拒绝；externalprovider推理是另列input |
| LIVE-018 / TEST-025 | noCSRF/恶意Origin/正式sameorigin | 403/无副作用；exactorigin正常；健康/preflight无fakeTOKENUSE |
| LIVE-019 / TEST-025 | A全部logout→NEW显式B正常登录→五alias B授权/refresh/stepup→B全部logoutCONFIRMED | R6.1.1原AS materialize+actualUSEDproof按实际issuer/registration唯一关联A及B每BFF held AT；各自logout前200/未自然过期后同AT401、handler0；全部当前issuer实际resource族intent确认/localcookie清除/204CONFIRMED，同route无issuerA fallback；不能用PUBLICCLI族或Cookie401替代 |
| LIVE-020 / TEST-025 | 仅taskASrevocation endpoint暂不可用/queue失败 | local退出完成、503PENDING/UNKNOWN、durableintent恢复，不停systemRedis/改ACL，latecallbacks不能复活 |
| LIVE-021 / TEST-002/026 | actualHTTP/USERInfo/RPC/PDPdeny/revoke审计 | 真PG/stream/signedreceipt→只清自己的spool；fixedmetrics标签、proof/trace不重复、无rawtoken/fieldnames |
| LIVE-022 / TEST-026 | taskspoolDEAD/容量、taskendpointUnavailable | readinessDOWN、503failclosed；恢复真实UP；不能借stop全局Redis造故障 |
| LIVE-023 / TEST-030/032 | stop+preservationfinalaudit | 原DBOID/原Redisnamespace/原nginxPID+port不变；任务data/secret/evidence保留，requiredcases全可核对 |
| LIVE-024 / TEST-022/023 | PDP已授但USER业务scope不足，scope充足但PDP未授；签名route未声明/被篡改requiredOAuthScopes | 各资源实际scope∧PDP都满足才执行；缺scope403/PERMISSION_DENIED、缺细权限同样拒绝；missing/malformed/unsignedroutefail-closed，业务handler0；旧Grant不扩大，新route发布与grantownerreceipt分别证明 |

Light SQL隔离的具体源码入口为XingyuanResourceAuthorizationIT#nativeLightSqlIsolationMatchesTheSameFormalTenantCalls，默认关闭。通过原POST/GET业务接口创建/读取两tenant行，以任务light库只读SQL核对真实id/tenant/字段，伪造tenant头不能改变签名身份；原USER_NOT_FOUND合同是400，不假定404。native-tenant-isolation.json绑定本run/hash、真实resource/两tenant、当前正式AT摘要、两条真实业务行ID、HTTP/SQL摘要和UTC时间；旧兼容IT仅用经校验的行ID构造冻结本任务native-light读取URL，不能用一个静态by-ID路径假称两个不同tenant行均成功。已有ext-1业务行时拒绝自动重插；本局部源码用例不等于完整runner、24项LIVE或Outbox/远程HTTP依赖已通过。

BFF四业务族刷新按实际realm的四个alias及原Edge使用的Spring默认60秒刷新窗口等待max(TTL)−60+1秒；identity-only alias不纳入四族窗口。原provider的61/59秒边界有普通无网络契约检查，实际REFRESH_TOKEN_ROTATED/USED/同族校验仍必需。SERVICE缓存观察先完成真实初始验签并发出私有READY，再与BFF等待并行；父须同时等三进程成功及新鲜零skip缓存JUnit和完整结果，不能用READY或早期退出算通过。长浏览器与缓存观察完成后，父以明确FORMAL_CODE_INPUTS阶段逐项调用原CLI006正式PUBLIC Code流程，使用新私有输出、逐项新鲜凭据和完整index复核；该阶段不续用BFF RT。当前完整Resource/Native矩阵仍未接齐，真实运行未验证。

原生SERVICE缓存的局部源码入口为XingyuanNativeAuthenticationIT#configuredNativeServiceClientsRenewThroughTheRealAsWithSingleFlight，默认关闭。四个实际原生profile按真实注册和审核grant构造请求，原SDK及Spring Client Credentials通过真实HTTPS AS取token，原SDK验签后核对issuer/clientPK/clientId/appId/context/tenant/aud/scopes；真实UTC过期窗口内观察缓存命中与续取，不用invalidate或虚拟时钟替代临期。第二次实际AS响应在交还缓存前有界等待8个调用者经原SDK指标确认未命中，再核对8返回同一新摘要、总计两次成功token响应且无RT。该响应计数不是AS全局请求/PG审计计数，也不证明运行应用已完成HTTP/RPC出站。固定native-service-cache.json只含白名单身份key及token摘要、计数、四角色和UTC时间；完整LIVE011/014及其他requiredcases仍须各自真实证据。

full前置纯time plan从实际registry逐realm取唯一四业务BFF alias和严格整数TTL，计算原60秒窗口的顺序等待下界；下界达到operationSeconds即exit2，不创建验收intent/子进程或重新Code输入。有效计划写入本次intent供审计，但不是全部阶段的保证时长；预算仍以实际deadline为准，不改真实TTL/时钟。

RPC局部源码actualGeneratedPeerContractsUseHmacDiscoveryAndServiceCredentials从SHA-bound实际生成Boot JAR中选择唯一审核namespace的facade，原RpcContractValidator校验真实PB/grpc契约，原DdcRpcClientFactory查HMAC目录，再经原RpcDirectClientFactory/IdpRpcClientCredentialInterceptorFactory调用。对两方向/两tenant明确只读缺行请求，原业务not-found envelope和无Token的UNAUTHENTICATED分别核对；不把业务缺行当业务数据正例或零handler证据。注册lease的准入Resource取provider实际registration-resource-uri（沿原fallback），RPC业务aud另取其TENANT resource，不混淆两者。固定native-rpc-contract-probe.json标注STANDALONE_NOT_FOUND_PROTOCOL，完整LIVE014仍需原App内出站、实际有数据正例、scope/revoke/PDP与handler/SQL/audit证明。

Service→Web数据正例局部源码nativeServiceReadsUsersCreatedByTheActualNativeWebApi通过原NativeWeb USER API创建并回读两tenant任务标记用户，再用原SERVICE RPC消费者读取实际返回ID；id/name/email/status必须匹配HTTP实测，B凭据读A行须ORG_NOT_FOUND，缺Token仍UNAUTHENTICATED。固定native-rpc-user-read.json标注STANDALONE_SERVICE_TO_WEB_DATA，只保留摘要/租约/租户等白名单metadata。此独立消费者正例不等于运行App内部双向出站或完整LIVE014；Web→Service数据正例、scope/revoke/PDP/handler及其他SQL/audit门禁仍必需。原生角色批准前该入口默认关闭，尚无真实数据写入或运行证明。

Light GraphQL局部源码nativeLightGraphQlAliasesUseTheSameVerifiedTenantAsRest复用当前正式A/B族和已完成实际REST/SQL数据证明的行ID，以POST/graphql的两个user alias各自匹配同tenant REST四字段；data带errors不能判成功。伪造tenant头不得改变A数据，B读取A行要求原nullable alias null/USER_NOT_FOUND envelope，裸请求HTTP401后再次A正常。固定native-light-graphql.json标注LIGHT_GRAPHQL_TWO_TENANT_READ并绑定当前两正式族/SQL证明/run/hash/UTC/安全摘要；这不替代14个roots全部授权、真正业务worker异步/cancel、strictDENY审计去重或完整LIVE013。当前入口默认关闭，真实GraphQL尚未执行。

Agent局部源码nativeAgentKnowledgeBasesKeepTenantAndAuthorizationBoundariesWithoutInference仅用SHA-bound现有logical model配置调用原knowledge-bases create/get/list，不调用模型。两tenant同task-code各创建一库，原接口回读/分页须匹配冻结配置及独立实际ID；B读A返回原KNOWLEDGE_BASE_NOT_FOUND，伪造头不改租户，viewer新code创建403后admin查询为0，裸请求401。固定native-agent-knowledge.json标注AGENT_KNOWLEDGE_METADATA_ONLY，保存安全摘要/当前正式族/实际ID/UTC而不保存数据明文；不替代ingest文件/Outbox/异步审计、Agent平台出站或外部推理门禁，真实数据创建目前未执行。

观察本地验签需counter/计时与JWK HTTP请求计数或AS匿名安全accessmetadata证明，不输出Token本身。不可通过关闭AS整进程来证明JWKS本地cache然后对正常业务sharedrevocation做无依据降级。服务不可用gate用任务独占endpoint/路由的明确受控故障，不停止共享PG/Redis。

真正跨节点HA、持久卷掉电、生产网络/CA安装、JWK定期轮换负载不由单本机实例证明，最终报告独立列未验证；不能用这些部署未验事项缩减本机完整集成requiredcases。

原数据库维度另以prepare建库前的private original-databases.json记录pg_database名称/OID/owner（排除本任务九名，不进旧schema）。resume缺该before但已有task事实时UNKNOWN_COMMIT，原库删除/OID或owner改变拒绝；外部新增库不导致误判。父full冻结该基线，在实际owned stop后经固定maintenance只读连接再比对，固定proof标注ORIGINAL_DATABASE_IDENTITIES_ONLY。该局部证明不能代替旧表内容、Redis namespace、原nginx和全部LIVE023证据，当前真实PG保留验收尚未执行。

原nginx维度另以private original-nginx.json保存prepare建库前实际ps/lsof观察的PID/UID/start/group/command SHA/TCP端口，参数只存摘要；没有控制nginx的动作。before缺失且已有task事实时拒绝倒造，原PID/身份/监听漂移或不可读失败；新增无关进程允许。父冻结基线并在任务stop后复查，固定proof标注ORIGINAL_NGINX_IDENTITIES_AND_PORTS_ONLY。该维度不覆盖nginx配置内容、Redis namespace或全部LIVE023，真实系统nginx验收尚未执行。

## R7. 文档/Plan合并门禁与执行证据

本附录仅补主规范，不授权当前写Spec/Plan阶段实施代码/运行。后续Plan按SDK→平台/Edge→native/前端→派生→guard/provision→live→finalaudit，每逻辑Step单focusedcommit；实际artifact/baseline/结果必须绑定当前HEAD。

- 主 Spec/Plan必须采用exact同一CLI-ID、9subcommands、4顶层provision文件、Playwright位置、processcap32/DBcap12及冻结九DB/端口。
- DEC-004已批准每resourceAT+同Session，DEC-005已批准真实internalfieldwriter/目录registration，DEC-006已批准scope∧PDP/signedrequiredOAuthScopes；独立resource Client/currentpublicsub门禁和旧Grant不自动扩权一致，所有pending/onlydeny代替positive的文字需删除。
- catalog fieldCount是机械声明，不是已授权field规则数量；REGISTERED_PENDING_REVIEW不能当roleGrant/WRITE；CONFIRMED不等于projectionready。
- 示例RootArtifactId是真实命名空间，不用light/org/eval等shortalias取代actualcatalog/sourceidentity。
- 0.2.5按真实installednpmartifact验证；不新增PortalPlaywright依赖，不用sourcepaths临时映射。
- 优先已有plainData/context边界与Component/commonowner，不另造同globalThreadLocal/IDP封装/DDL/crypto/timeout框架。
- CLI结果SCHEMA、typedmodels/constraints、生成JavaBean/Qualifier、每profilekey parity、参数/exit/noSideeffects均受真实positive/negative tests。
- 普通mvn test不启动PG/Redis/Testcontainers；live envmissing必须将该live请求标BLOCKED而非SKIP-PASS；浏览器config不自动webServer。
- 尚未运行的命令、PG/Redis/SAS/browser/RPCcase不标PASS。最终实现/运行结论有scope，不能凭Spec-validator/源码编译推断SSO、SQL、服务cache或撤销。

| MC / literal rule | 附录设计证据与结论 |
| --- | --- |
| MC-ARCH-001 / Rule11 | R1.2 src/test同模块、四个明确provision文件/四源family结构保留；不新层/module/生产bootstrap |
| MC-REUSE-001 / Rule11 | R4实际DDL/repository/cipher/service/Outbox与R5现有TLS/SDK能力；不复制nativecatalog/field DAO |
| MC-DEP-001 | R1.1 resolvedPlaywright1.62.0，JDK/Spring/当前Maven依赖；本附录无新dependency |
| MC-NAME-001 / Rule1 | R1.3每技术/行为/carrier/converter suffix明确，IT不是suffixless业务类 |
| MC-VALID-001 / Rule2 | R2 DTODefault/Prepare/Provision/Runtime组，R4原生约束+业务锁，manual Common验证仅nonproxy |
| MC-MODEL-001 / Rule3 | 普通class全Lombok/Builder，immutablevalue才record；原owner对象不复制改造 |
| MC-CONVERT-001 / Rule3 | 嵌套MapStruct/BaseForward converter、真实repoPO/BO converter保持，不JSONroundtrip |
| MC-LOG-001 / Rule4 | namedSlf4jComponent，固定stage/outcome/runId安全日志，不rawThrowable/password/token |
| MC-BEAN-001 / Rule4 | R1.3 exactnamedbeans/qualifiedfinal/RequiredArgsConstructor/lombok propagation，testconfigs局部作用域 |
| MC-UTIL-001 / Rule5 | JDK/当前Spring/Commons范围，openssl/keytool仅CLI工具；无duplicateUtils |
| MC-JSON-001 / Rule6 | managedJacksonstrict input、完整R3schema、semanticwirevalues；业务JSONenum沿原owner |
| MC-TIME-001 / Rule10 | Instant UTC、Duration、Clock；PG ownermicroprecision，deadline不无限wait |
| MC-CONFIG-001 / Rule7 | R5 exactkey/parity/env表；receiver唯一owner；native/Edge新增binding由各主附录唯一表 |
| MC-PATTERN-001 / Rule9 | R1.3固定阶段method-referenceStrategy+原TransactionTemplate/Outbox，简单logic直接 |
| MC-SCOPE-001 | 只新taskdata/进程/CA，noDocker/nooldDB/SQL/Systemtrust；无Sourcealias、原模板Open不变 |
| MC-TEST-001 | R3每CLInegative/recovery，R6真实SAS/PG/Redis/browser/RPC/generation/preservationtrace |
| MC-BLOCKER-001 | 本附录已明确设计owner/合同与DEC-004/005批准边界；主代理做组内ID/key/profile/receiver最终一致性审查。实现与全部live未进行是后续执行证据边界，不单凭文档声称实施PASS |

标准matrix的设计行不等于已实施门禁；完整十条literal原文及独立17项MC状态由主 Spec §20权威保留。本附录无其他文件写入权，主代理合并最终IDs/links/keys并strict验证主Spec/Plan。

### R6 用户接受的 Agent 运行输入缺口（2026-10-15）

用户明确答复“暂不提供，保留 Agent 运行未验证”。本轮不提供模型/embedding/MCP配置，不用假key/地址、test fake bean或生产防护降级让Agent启动，不调用外部模型。Agent接入源码、原六层业务、安全默认测试和真实生成构建义务保持；其依赖这些输入的实际启动/平台出站/外部provider运行证据明确未验证，作为用户接受的运行缺口保留。其他平台与非Open模块的接入及验证要求不变。

R6 full24矩阵不能将Agent未执行用例记为PASSED、不能把required skip当运行成功；局部/其他平台证据与该缺口分别报告，不伪造完整full通过。该决定只针对Agent外部配置输入，不批准§12.55独立provisioner/临时管理策略，不解除任何生产鉴权或字段WRITE约束。


### R4 补充：已批准的隔离 provisioner（2026-10-15）

用户已明确批准 Plan §12.55。为遵守原 AssignmentFacade 的 SELF_PRIVILEGE_ESCALATION_DENIED，隔离准备通过原 IdpBootstrapService 新建独立 xingyuan-it-provisioner 身份，以原 TenantMembershipService 建立两租户 ACTIVE membership；admin/viewer 交互身份与其权限均保持原合同。新增私有 secretFiles.provisioner-password，按既有保护目录/强口令规则准备，CLI-002 owner 运行前必须验证。私有 registry 另存 provisioningSubject，必须与 subjects.admin、subjects.viewer 不同；subjects 仍恰好两个交互键，不把该身份加入正式 token/BFF 用户选择。口令不进入 registry、public结果、argv或日志。

原 JA Directory 客户端须真实 batchGet 三身份并核对两租户各三条 ACTIVE membership，control-plane-clients 私有 READY 凭据 membershipCount=6；这不是创建三套登录用户的同义声明。JA 后续须通过原 UserCrudFacade 将该真实 sub 建为租户用户，且无业务角色、BusinessAccess、OAuth Client/Grant 或 activated root。

原生角色准备使用原 ManagementPolicyFacade，按确切目标用户和确切新角色建立最多10分钟、仅ASSIGN_ROLE、禁止继承扩张、reason/ticket必填、风险和assignment时长有限的临时策略；由该独立身份调用原 AssignmentFacade，成功后以原 disable 关闭并回读确认。失败/未知提交必须保留原证据并停止盲重试；最终验证该身份无业务授权且临时策略均已禁用。不得改生产自授权防护、直接写assignment、伪造STRONG事实或扩大Client/Grant。本批准不等于实现/运行验收通过，Agent已接受运行未验证决定保持不变。


### MCP原接线执行澄清（Plan §12.120，用户已批准，2026-10-15）

受管理MCP入口以已有operationId `yuheng.mcp.<serverCode>`精确匹配已激活签名快照中SECURITY policy的既有configuration.operationIds；每次从一次active读取核同一server id/code/resourceUri及enabled状态，必须恰好一条绑定。MCP角色现没有预存security编译map，复用现有GatewaySecurityPolicyCompiler及原capabilities编译同一条已发布policy；不扩展DTO或引入另一缓存。缺快照、未ready、server不一致、绑定缺失/歧义、空/非法必需业务scope或配置错误均失败闭合503，业务调用为零；不得从token/header猜scope或使用identity-only静态默认策略。原scope AND per-capability PDP/approval/installation合同继续适用。

原身份adapter仅在内部可信attributes增加同次已验证TokenAuditProofBO的canonical issuer；managed MCP identity.issuer只消费该值，不读身份header或使用单一配置issuer回退。保留旧公开authenticator构造器ABI及旧off-policy兼容行为，当前managed工厂使用Function策略。GatewaySecurityException沿原HTTP renderer保持401/403/503及原响应头/错误JSON形状；普通业务/协议错误语义保持。范围为§12.120列明四生产文件及三个测试，不新增公开接口、权限、依赖、配置key或DDL。此为源码/定向受控验证；真实多issuer MCP/PG/Redis验收仍归Step18，Step17完成后停止。


§12.125 已批准的 MCP 严格请求接线澄清：整次原 handler route/dispatcher 用同一 Core AuthorizationObservationScope，认证成功只在本机传递原 GatewayAuthContext 引用，进入 session/capability 前移除 opaque 字段。managed fine PDP 核同一 scope 引用、issuer/resource/subject/tenant/USER audience/token/time 与原 verified proof/scopes，复用原 denyOnce 和 authorizationDenied；确认失败、缺可信证据/marker、授权依赖或版本 fence 均503、业务0。完成/异常/cancel 由唯一外层 owner close。原 dispatcher 会把实际 capability 拒绝转换成 JSON-RPC response；仅同请求已经严格确认的 MCP_FORBIDDEN 恢复原403 renderer，依赖故障即使被列表过滤/dispatcher 包装仍恢复503；普通协议/业务错误与按权限过滤列表的既有成功形状保留。身份/安全错误强制 no-store，原挑战头保留。旧 loader/authenticator 构造器保留，原已存在 Shoubing gateway-adapter 仅 runtime→compile、同GAV同版本，无新制品/配置/权限/DDL。无实际服务/数据库/Redis/HTTP运行证明，Step17只关闭源码/受控门禁，完成后停止于Step18之前。

### Step19 最终实施审计 — 24项LIVE与保留真相（PARTIAL，2026-10-16）

审计时HEAD为e8117274b。本附录R6要求的24项LIVE **全部未执行**。Step18经用户于2026-10-16明确批准跳过：本机不具备其前置输入——缺少经人工核对的`intent-manifest.json`、`postgres`/`redis`凭据文件、九角色`secretFiles`，以及两租户真实owner提交的issuer/registration/clientID registry导出（Plan Step18明确「不在intent捏造」），亦缺少带真实SHA的`artifacts`清单。判定 **PARTIAL — required runtime evidence remains unverified**。完整台账见[实施Plan](../plan/2026-10-11-12-03-xingyuan-native-full-integration-plan.md) §12.128。

| 项目 | 状态 |
| --- | --- |
| LIVE-001 至 LIVE-024（全24项） | Runtime unverified — 未执行 |
| 九CLI（prepare / provision-identity / start-control-plane / provision-authorization platform+native / authorize-cli / start-consumers / verify / status / stop） | 未调用 |
| 两phase（platform、native）真实授权facts | 未产生 |
| `OutboxTransactionRollbackIT#jpaRoleResourceAndMpOutboxRollback` / `AppendFault` / `Commit` | 真实PG执行未进行（LIVE-002） |
| 九个新任务数据库、Redis DB、TLS 28443、任务进程与真实浏览器 | 未创建、未启动 |
| owned stop后的PG/Redis/nginx保留比对（LIVE-023） | 未取得；原设施全程未被触碰 |
| `docs/egon/evidence/2026-10-11-xingyuan-native-full-integration-runtime.md` | 未创建 — 无真实结果时写该文件等于伪造 |

因此`original-databases.json`、`original-nginx.json`等prepare前基线，以及任何run-scoped OID/PID/cert/token index/receipt都不存在；本轮没有任何task DB、secret或evidence需要保留或清理，也没有半provision或UNKNOWN_COMMIT状态遗留。`target/`下三个`xingyuan-integration-*`目录来自Step17受控contract测试，非真实运行产物。

本次实际执行并exit0的唯一运行相关门禁为`bash scripts/regression/test-isolated-integration-contract.sh`（6组PASS、0 failure/error）。该测试全程在临时目录内以stub替换`psql`/`createdb`/`redis-cli`/`nginx`/`java`/`node`/`npm`/`mvnw`，零外部设施；它证明九CLI的guard、owner dispatch与受控准备/恢复契约成立，**不**证明任何LIVE case通过，其内部模拟的24case不计作live。

R6既有的「真正跨节点HA、持久卷掉电、生产网络/CA安装、JWK定期轮换负载不由单本机实例证明」边界继续保持未验证，并且本轮不得以其缩减24项required live case——这些case本身同样仍未执行。要将本附录推进到`PASS`，必须在具备人工核对manifest与真实registry导出的本机按R2–R6完成全部24项并保留owned stop证据。
