# 天权 OAuth/OIDC 执行与有效 Spec 符合性审计

**Verdict: PARTIAL — Spec requirements are unmet or unverified.**

18个原Plan步骤和7个历史纠正提交保留。已批准的Step19/C9b通过33路径审查、真实原生Filter回合及354 Admin +24 Edge +58消费者测试，按一个聚焦纠正提交交付。B-01/02/03已关闭。已批准的Step20/C10关闭B-04，19路径且364 Admin +29 Core/前端50通过；真实PG/Redis/多实例/TLS/浏览器与负载仍未验证，完整reactor历史失败未豁免。因此全任务仍为PARTIAL。

## 1. 审计基线与授权

| 项 | 值 |
| --- | --- |
| Branch / baseline | main；Step20起点`9fc41f38f0638a375d049f5350d7e3a1b92051ac`；本报告随Step20唯一提交交付，实际hash由最终交付及同题git log读取 |
| 执行起点 | `ad60242402eb45a4a801730cf5151119031a98b4` |
| Plan | docs/egon/plan/2026-09-23-10-42-tianquan-oauth-oidc-implementation.md；Review；Updated 2026-10-09 17:37（文档状态不冒充Accepted） |
| 主Spec | docs/egon/spec/2026-09-22-17-14-tianquan-multitenant-oauth-oidc-design.md；历史Accepted，保留既有未提交文档修改 |
| 有效联合修订 | docs/egon/spec/2026-10-08-10-18-oauth-current-code-alignment.md；Review，依当前对话明确执行授权实施；Grant提案由本修订点名替代 |
| 依赖Spec | docs/egon/spec/2026-09-24-12-00-outbox-statemachine-cqe-integration.md；同源DataSource/SqlSessionFactory/TransactionManager与Outbox投递合同 |
| 补救结果/后续提案 | docs/egon/reports/2026-10-09-oauth-consent-correction-proposal.md；C9b已批准/验证33路径；C10已批准并实现；19路径包括3个直接必要构造/关联页面测试兼容路径 |
| 审核范围 | 原18步骤/7纠正及Step19的33路径、Step20的19路径；有效48 REQ、46 HTTP +4 INTERNAL +2 EVENT +2 JOB与91 TEST逐项矩阵。运行验收不得从测试替身推断。 |


## 2. 已关闭的纠正与剩余缺口

**B-01 / Closed，Step19。** Native pending从实际Filter保存/密文重载，v2恢复原客户端state与server state/原scope/session；CONSENT_STATE仅内部索引，300秒到期。实际SAS CSRF保护POST authorize；API-046渲染unchecked scope和独立Cancel。真实加密JWKSource经过Boot方法校验；S256 code交换、RS256 USER AT、nonce/azp ID、UserInfo、RT rotation/绝对family期限、跨realm/session/subject/client与故障回滚正负例通过。生产仍未启动，事务为受控替身，不能据此声称PG并发已验证。

**B-02 / Closed，Step19。** 事务Provider、登录Filter、issuer Filter改为RequiredArgsConstructor/Slf4j，Spring注入字段具备对应Qualifier；保留工厂显式构造的非Bean策略/delegate，不虚构Bean。真实构造消歧与SPI方法校验回归通过。

**B-03 / Closed，Step19。** C8c合并入这一逻辑纠正：实际JWT scope字符串、Token-specific scope重载、ID/AT分组校验、API-046/OAS、303登录/302同意或callback、必填state/nonce/resource边界及Edge固定Resource/stepUp合同同步。准确登记一个新DDL并保持旧项不变。

**B-04 / Closed，Step20。** Core不可变ClientResourceGrant允许准确的USER BUSINESS范围（≤64项、1–128 ASCII字符，不含六个OIDC身份scope），继续拒绝tenant/context。既有Admin upsert/batch按当前client上界、Jakarta DTO和Core域约束保存/回读准确集合；USER列表按scope排序以保持导入比对稳定，SERVICE保持原保存/上下文规则。UI支持USER编辑和原样提交；空集合清除业务权限，自动Resource关联仍为空。迁移只保存审核assignment；新Client在同事务内创建的ACTIVE version0空关联可初始化为审核范围，既有空/冲突Grant不能借该例外扩权。四种legacy scope/新关联组合、target内容冲突、真实分页游标恢复、重放和SERVICE原例通过。真实管理Service→原生Token兑换、清空/禁用/错版本不消费code的回归通过。

**B-05 / 本机共享包产物未刷新，Partial。** 已安装`@egon-cola/xingyuan-admin-web-shared@0.2.3`仍声明旧login(request)/logout():void，当前已提交共享源码0.2.4为无密码BFF login()/GatewayLogoutResult。原`npm run typecheck`在AuthContext报TS2554/TS2339；没有改消费者来迎合旧接口，没有修改安装包、依赖/锁文件或发布。临时tsconfig解析当前共享源码后完整类型检查exit0；前端原始完整Vitest50通过。用户运行/部署前需要使用与已提交共享源码一致的包产物，本报告不把该本机安装状态称作已验收。

**运行与历史失败。** §4保留两个全reactor失败及先前误启动临时Redis的真实记录。真实PG事务/CHECK/唯一性、Redis撤销/恢复、多实例、TLS、浏览器布局/键盘/SSO和负载由用户控制，尚未运行。另一次排查错误地将本机Maven配置凭据输出至工具日志，已向用户披露；报告不保留值，建议轮换所涉及凭据。

独立公共MP提交`7084c44`（9路径，2026-10-09 14:15）已在当前branch，未归入OAuth交付、未修改或重写。提交前已检查其可选statementPolicies默认为空且授柄没有启用该新配置；相关源码mtime早于16:37 Admin/16:38消费者新鲜门禁，所以这些结果基于当前组件源码。新增可选能力不改变本Step路径/接口/迁移或默认策略。

## 3. 原18步骤与纠正提交

Committed记录表示对应提交存在并已有其步骤测试证据；不推导其有效Spec已经全部满足。步骤9原闭环结论曾被B-01推翻，Step19已补足受控原生链；Step20已闭合管理/导入明确业务范围；真实运行及B-05产物状态仍单列。

| Step | 提交主题 | 状态 | Commit | 路径数 | 已执行证据 | 范围 |
| --- | --- | --- | --- | --- | --- | --- |
| 1 | chore(xingyuan): establish oauth dependency and edge module | Committed | 1f0cc584eadc21c0920251103bb5b62192614f6d | 9 | Edge/Admin编译；最终模块命令再次编译 | 附录 Step 1 精确路径 |
| 2 | feat(shoubing): define managed oauth schema and routing | Committed | eee09da9bbe36c6158be0ac453103df88978debd | 12 | V-ADMIN / V-FOCUS | 附录 Step 2 精确路径 |
| 3 | refactor(shoubing): move identity directory persistence to MP | Committed | bca73d607c58b8f8c724f96cba20b27975ca854f | 55 | V-ADMIN / V-FOCUS | 附录 Step 3 精确路径 |
| 4 | feat(shoubing): persist oauthrealm/oauthclient state | Committed | 89acea901817b3567e2053ee76de41840cc48722 | 21 | V-ADMIN / V-FOCUS | 附录 Step 4 精确路径 |
| 5 | feat(shoubing): persist oauthresource/oauthgrant state | Committed | 58db54bbe48b54c248cff575266ab506870d27ca | 17 | V-ADMIN / V-FOCUS | 附录 Step 5 精确路径 |
| 6 | feat(shoubing): persist oauthauthorization/oauthtoken/oauthconsent state | Committed；最终验收Partial | 2c7afc9da2c537b838984c8daf1a0dbc918ca7c3 | 34 | V-ADMIN / V-FOCUS | 附录 Step 6 精确路径 |
| 7 | feat(shoubing): persist oauthsigningkey/oauthsecuritystate/oauthrevocation/oauthaudit state | Committed | ff785f8e04761a11e16bdddfd6d208dff72336c8 | 32 | V-ADMIN / V-FOCUS | 附录 Step 7 精确路径 |
| 8 | feat(shoubing): add multitenant authorization-server protocol | Committed；最终验收Partial | 600f9bad9e344339cf60626607f837889dc7ba44 | 19 | V-ADMIN / V-FOCUS | 附录 Step 8 精确路径 |
| 9 | feat(shoubing): implement code pkce oidc and client grants | Committed；最终验收Partial | c621d3b4287ddde0c538c80cef0fc5f890f8344a | 21 | V-ADMIN / V-FOCUS | 附录 Step 9 精确路径 |
| 10 | feat(shoubing): scope oauth administration and rotate secrets | Committed | a7a6094295f310c270bb12e0761ca44244a13217 | 68 | V-ADMIN / V-FOCUS | 附录 Step 10 精确路径 |
| 11 | feat(shoubing): enforce shared revocation and durable audit | Committed | ee817fcbc455436dd59626b12b4a43baf288a2e9 | 54 | V-ADMIN / V-FOCUS | 附录 Step 11 精确路径 |
| 12 | feat(shoubing): verify tenant jwt locally with strict revocation | Committed | 7813a6eb80bbf6bd19634dc7e61b88ad6e67c147 | 25 | V-STARTER | 附录 Step 12 精确路径 |
| 13 | feat(shoubing): acquire scoped service tokens with webclient | Committed | 9aeb3cb7970ddfe8b84e31f951bb16b04db66168 | 10 | IdpServiceOAuth2ClientTest / V-C13 | 附录 Step 13 精确路径 |
| 14 | feat(yuheng): add oauth2 bff edge | Committed；最终验收Partial | 3fb37e6a490f0a27bbd15cc06c3383bafe307429 | 24 | V-EDGE | 附录 Step 14 精确路径 |
| 15 | feat(xingyuan): integrate edge identity with gateway rbac and tianshu | Committed | a779aaeab667fa9d1322123aa57796e89efbcf65 | 11 | V-GATEWAY / V-RPC | 附录 Step 15 精确路径 |
| 16 | feat(xingyuan): migrate admin auth to oidc bff | Committed；最终验收Partial | e71d00d3826cc96d7c46dbdc485cc972bf330ca5 | 49 | V-FRONTEND / V-EDGE | 附录 Step 16 精确路径 |
| 17 | feat(xingyuan): add oauth migration and local bff verification | Committed | 1dfd11960fa5c868766c1d50becea684a8a3ef27 | 23 | V-MIGRATION / V-STATIC | 附录 Step 17 精确路径 |
| 18 | refactor(shoubing): retire legacy oauth protocol | Committed；最终验收Partial | 8b5a8fb5b9c360997621a6f29962f9269e2ac377 | 70 | V-ADMIN / V-FOCUS | 附录 Step 18 精确路径 |
| 19 | fix(shoubing): complete native consent and oidc flow | Committed | 9fc41f38f0638a375d049f5350d7e3a1b92051ac | 33 | V-C9b-ADMIN / V-C9b-EDGE / V-C9b-CONSUMERS / V-C9b-STATIC | 附录 Step19精确路径 |
| 20 | fix(shoubing): preserve explicit user grant scopes | Verified；随本报告唯一提交，hash由最终交付核对 | 同题git log及最终交付 | 19 | V-C10-ADMIN / V-C10-UI / V-C10-TYPE / V-C10-STATIC | 附录E精确路径 |



| 纠正 | Commit | 内容 | 路径数 | 状态 |
| --- | --- | --- | --- | --- |
| C13 | c72de6fe1407430759a68acd76b8c06212720e0c | fix(oauth): select service registration for each tenant | 3 | 独立纠正提交，保留历史；见对应验证与附录 |
| C8 | e7889ee7bc31f743bfd17e78e68e164cf13b7450 | fix(shoubing): keep issuer login outside gateway catalog | 2 | 独立纠正提交，保留历史；见对应验证与附录 |
| C17 | cd2a350d0309c53e936982c5a0f86efa0ad463db | fix(shoubing): validate migration checkpoint recovery state | 2 | 独立纠正提交，保留历史；见对应验证与附录 |
| C8b | 1f09ce028ea534704ccd9acda71de891e7d844de | fix(shoubing): use accepted well-known jwks path | 2 | 独立纠正提交，保留历史；见对应验证与附录 |
| C15 | 076f34a8e36335fd3055f05e069fb15ea1263484 | fix(shoubing): wire gateway to shared issuer verification | 10 | 独立纠正提交，保留历史；见对应验证与附录 |
| C14 | 54573468864018e733f4f7542424ef8f2b9e607a | fix(yuheng): strip browser cookies before engine forwarding | 2 | 独立纠正提交，保留历史；见对应验证与附录 |
| C18 | 4d9104f2817e051cacc2bbd99e24df4fdab88182 | fix(shoubing): remove retired protocol configuration | 7 | 独立纠正提交，保留历史；见对应验证与附录 |
| C8c / C9b | 本报告Step19同次交付 | 原生consent/OIDC与scope/OAS/callback统一纠正 | 33 | 原C8c并入一个逻辑纠正，最终提交hash见交付核对 |


## 4. 实际验证与失败边界

测试结果按实际命令/日志记录，重叠测试不累加为独立覆盖。简称命令的模块全路径见Plan，保存的command.txt每行一个argv，可无shell歧义还原。

| ID | 实测输出 | 结果 | 实际命令/argv证据 | 限制 |
| --- | --- | --- | --- | --- |
| V-ADMIN | /tmp/egon-c9b-all-admin-green3.log | 68类/354 tests，0 failures/errors/skips，BUILD SUCCESS，exit0 | /tmp/egon-c9b-all-admin-command.txt（每行一个argv） | 当前最终代码全部默认Admin *Test及三安全WebMvc IT；不运行其他runtime IT |
| V-FOCUS | /tmp/egon-step18-focus-final.log | 56 tests / 0 failures/errors/skips；BUILD SUCCESS | /tmp/egon-step18-focus-command.txt | 11类与非外部设施IT |
| V-C8c | /tmp/egon-c8c-admin-green3.log | 47 tests / 0 failures/errors/skips；BUILD SUCCESS | mvn -o -pl Shoubing Admin -am -Dtest=OAuthAuthorizationPersistenceContractTest,OAuthOidcProtocolContractTest,OAuthProtocolOpenApiContractTest,OAuthTokenClientCredentialsTest,TenantIssuerIsolationTest,IdpPublishedOpenApiContractTest -Dsurefire.failIfNoSpecifiedTests=false test | 真实CC签名+captured authorization映射；无真实人员consent回合 |
| V-EDGE | /tmp/egon-c8c-edge-green2.log | 24 tests / 0 failures/errors/skips；BUILD SUCCESS | mvn -o -pl egon-cola-xingyuan/egon-cola-yuheng/yuheng-edge-gateway -am -Dtest=EdgeLoginContractTest -Dsurefire.failIfNoSpecifiedTests=false test | Mock exchange/fake Redis，不是多实例Edge/browser |
| V-STARTER | /tmp/egon-step12-green-final3.log | BUILD SUCCESS；见实际各模块测试行 | Plan Step12 §8命令；对应完整原始日志保留（不重构未知argv） | RS256/issuer/scope/revocation negative fixtures |
| V-GATEWAY | /tmp/egon-c15-green2.log | Starter11+Gateway17=28 tests，0 failures/errors/skips；BUILD SUCCESS | mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-gateway-adapter -am -Dtest=IdpGatewayJwtVerifierTest,IdpGatewayAdapterAutoConfigurationTest,IdpGatewaySecurityProviderTest,GatewayResourceServerResolverTest,MultiIssuerResourceServerTest -Dsurefire.failIfNoSpecifiedTests=false test | 实际生产Bean factory；不连Gateway/Redis |
| V-C13 | /tmp/egon-c13-green.log | BUILD SUCCESS | C13 Plan §6.5精确测试 | tenant→registration映射/credentialVersion负例 |
| V-MIGRATION | /tmp/egon-c17-final-green.log | 20 tests / 0 failures/errors/skips；BUILD SUCCESS | mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthMigrationServiceTest,IdentityMigrationContractTest,OAuthMigrationRunnerTest -Dsurefire.failIfNoSpecifiedTests=false test | 500/501、commit未知、checkpoint失败和重放；MockDAO/临时owner-only文件 |
| V-RPC | /tmp/egon-step18-rpc-final.log | 3 tests；BUILD SUCCESS | /tmp/egon-step18-rpc-command.txt | HMAC header contract，不连DDC |
| V-CANCEL | /tmp/egon-step18-cancel-final.log | 2 tests；BUILD SUCCESS | /tmp/egon-step18-cancel-command.txt | 受控流取消，不启动应用 |
| V-MCP | /tmp/egon-step18-mcp-final.log | 3 tests；BUILD SUCCESS | /tmp/egon-step18-mcp-command.txt | 短生命周期loopback测试fixture；不是实际部署 |
| V-STATIC | /tmp/egon-c18-static-green.log | PASS / exit0 | scripts/unified-xingyuan/test-direct-run-contract.sh；bash -n 受影响shell | 纯fixture文件/受控HTTP mock，不执行prepare/start/verify/import |
| V-FRONTEND | /tmp/egon-step18-shared-test.log；/tmp/egon-step18-shared-typecheck.log | 14 tests / 3 files PASS；typecheck exit0 | npm --prefix egon-cola-xingyuan/egon-cola-xingyuan-admin-web-shared run test；run typecheck | 五消费者Step16精确package门禁历史通过；未做浏览器E2E |
| V-C18 | /tmp/egon-c18-green.log | 19 tests / 0 failures/errors/skips；BUILD SUCCESS | C18 Plan §6.11精确测试 | base/local dead叶子/全local输出静态检查 |
| V-DOCS | 本轮重新运行结果见§9 | strict Spec/Plan与git diff --check PASS | python3 .agents/skills/egon-coding-writing-spec/scripts/validate_spec.py docs/egon/spec/2026-10-08-10-18-oauth-current-code-alignment.md --strict；对应validate_plan.py；git diff --check | 结构证据不豁免运行验收 |


### Step19新鲜门禁

| Gate | 精确命令/argv | 结果与日志 | 限制 |
| --- | --- | --- | --- |
| V-C9b-ADMIN | `/tmp/egon-c9b-all-admin-command.txt`；按当前树枚举65个*Test +三安全WebMvc IT，`mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=<该精确列表> -Dsurefire.failIfNoSpecifiedTests=false test` | 68类/354，0失败/错误/跳过，exit0；`/tmp/egon-c9b-all-admin-green3.log` | 实际Config/Filter/Service/Mapper与受控端口/事务替身；无真实PG/Redis |
| V-C9b-EXACT8 | `mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthConsentRoundTripTest,OAuthAuthorizationPersistenceContractTest,TenantIssuerIsolationTest,OAuthOidcProtocolContractTest,OAuthTokenClientCredentialsTest,OAuthProtocolOpenApiContractTest,IdpPublishedOpenApiContractTest,SpringConstructorResolutionTest -Dsurefire.failIfNoSpecifiedTests=false test` | `/tmp/egon-c9b-final8-serial.log` exit0；完整68类也包含此八类且在最终import/schema fixture修改之后再次通过 | 前一次并行Maven在共享protobuf临时目录清理失败，未进入测试；未计PASS，串行重跑 |
| V-C9b-EDGE | `mvn -o -pl egon-cola-xingyuan/egon-cola-yuheng/yuheng-edge-gateway -am -Dtest=EdgeLoginContractTest -Dsurefire.failIfNoSpecifiedTests=false test` | 24，0失败/错误/跳过，exit0；`/tmp/egon-c9b-final-edge.log` | 固定Resource/nonce/PKCE/stepUp/callback/OAS；无真实AS/Redis |
| V-C9b-CONSUMERS | `mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-gateway-adapter,egon-cola-xingyuan/egon-cola-yuheng/yuheng-mcp-gateway,egon-cola-xingyuan/egon-cola-yuheng/yuheng-runtime-core,egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-tianshu-adapter -am -Dtest=IdpGatewayJwtVerifierTest,IdpGatewayAdapterAutoConfigurationTest,IdpGatewaySecurityProviderTest,GatewayResourceServerResolverTest,MultiIssuerResourceServerTest,IdpServiceOAuth2ClientTest,IdpStarterAutoConfigurationTest,DdcRpcClientInterceptorFactoryTest,GatewayCancellationTest,McpTransportIntegrationTest,McpGatewayManagementSecurityConfigurationTest -Dsurefire.failIfNoSpecifiedTests=false test` | Starter33 +Gateway17 +RPC3 +cancellation2 +MCP3 =58，全部0失败/错误/跳过，exit0；`/tmp/egon-c9b-consumers.log` | MCP短生命周期loopback fixture；非真实部署/线上DDC |
| V-C9b-STATIC | `bash scripts/unified-xingyuan/test-direct-run-contract.sh`；`bash -n scripts/unified-identity-local.sh scripts/unified-xingyuan/start-local-stack.sh scripts/unified-xingyuan/test-direct-run-contract.sh`；strict Spec/Plan/resource validator；`git diff --check` | 全部exit0；`/tmp/egon-c9b-static.log`；95个执行起点db SQL全字节相等，manifest旧项exact且追加唯一真实SHA，33路径与声明相等，两个原dirty Spec SHA不变 | 不运行prepare/start/status/verify/import/live-login；结构通过不代表runtime |

RED保留：native储存RED5（18/4预期失败）、真实SPI HV000151、freshness RED（16/1行为失败）、scope RED（24/3预期失败）、allAdmin旧manifest单项失败（354/1）。每次均在相应生产/fixture纠正前观察；最终GREEN覆盖，不把最初编译/fixture错误充作行为RED。

**失败与运行偏差（不得省略）：** `/tmp/egon-step18-full-reactor.log` 原无guard全reactor在未修改common-cache的94 tests中2个ClassCast错误失败，Admin未到达，并意外启动3个Testcontainers Redis和1个Ryuk，违反禁止自动基础设施规则，已向用户承认。4个归属fixture（361d4aa747fd、460f2dfbda8c、2b216f55ee79、f8af443d5f56）均由测试自动清理，随后inspect确认已移除；未停止其他容器。此后不再运行无guard命令。`/tmp/egon-step18-safe-full.log` 加排除门禁后仍在未修改MP精确列表测试因RESTORE/restoreById断言失败：138 tests、3 failures、1 skipped。未修改无关公共组件来消除它们。**完整reactor没有PASS**；Admin精确列表和受影响消费者是已实际成功的替代证据，不证明全部仓库。

未启动项目/服务器/浏览器、未连接或迁移真实业务数据库、未运行导入、未部署/push/PR/发布。曾运行上述临时Redis容器，因此不能说“未启动任何Redis/容器”。用户既有业务数据未被本任务迁移/修改。

### Step20新鲜门禁

| Gate | 精确命令/argv | 实际结果/日志 | 限制 |
| --- | --- | --- | --- |
| V-C10-FOCUS | Plan Step20精确7类Maven命令，含SpringConstructorResolutionTest | `/tmp/egon-c10-focused-green2.log` Core3 +Admin76，exit0；末次完整门禁还覆盖随后新增两个migration参数组合 | 实际Service/生成Converter/SAS+受控端口；非PG/Redis |
| V-C10-ADMIN | `/tmp/egon-c10-all-admin-command.json`；原68类安全Admin白名单加当前9类Core，`mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=<该77类准确列表> -Dsurefire.failIfNoSpecifiedTests=false test` | `/tmp/egon-c10-all-admin-final.log` 68类/364 Admin +9类/29 Core；exit0，全部0失败/错误/跳过 | 不执行外部设施测试；不是完整reactorPASS |
| V-C10-UI | `npm --prefix egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin-web run test`；Grant文件单独命令见Plan | `/tmp/egon-c10-ui-all2.log` 12文件/50；`/tmp/egon-c10-ui-green.log` Grant11；均exit0 | Vitest/jsdom；无浏览器。原全UI50/5失败与起点HEAD隔离17/4失败在baseline日志；只修三个直接必要测试夹具/等待条件后通过 |
| V-C10-TYPE | Admin-web现有tsc；临时config extends其tsconfig.app.json，paths只把共享包指向当前repo src/index.ts，types显式指向现有Admin-web node_modules的vitest/globals、jest-dom/types/index、vite/client | 原`npm run typecheck` exit1（B-05）；`./node_modules/.bin/tsc -p /tmp/egon-c10-ui-source-tsconfig.json --noEmit --incremental false` exit0，`/tmp/egon-c10-ui-source-typecheck3.log` | 证明当前源码完整类型合同，不证明旧安装包或发布/部署；无依赖修改 |
| V-C10-STATIC | strict Spec/Plan/resource validator、精确路径git diff --check、SHA与SQL/manifest字节对照 | `/tmp/egon-c10-spec-strict.log`、plan-strict/resources/scope-proof.json；97个HEAD db SQL与manifest不变、2原dirty Spec SHA不变、19声明路径 | 不运行任何migration/stack/browser；结构不是runtime证明 |

行为RED：Core构造器拒绝非空USER（core-manage-red）；管理Service和实际managed-native链拒绝（all-admin-red）；Controller malformed DTO返200（api-import-red）；实际迁移apply之后准确scope仍空（import-red2）；UI缺scope editor（ui-red/ui-red2）。编译错误、source tenant/Page fixture、native二轮旧已消费记录不计作RED。migration恢复阶段本来正确，后来失败来自测试缺少游标后的下一页，已修正fixture且生产checkpoint阶段未改。最终全部由上述成功命令覆盖。

## 5. 每项有效需求

Satisfied只用于实现和必要非运行证据足够的原子需求；需要运行证明的行单列Runtime unverified；代码缺口使用Partial/Not satisfied。各行验收条件直接来自有效Spec，修订覆盖范围按§5.3，不将历史Spec-only限制误用为当前禁止编写已授权代码。

| ID | 有效Spec验收要求 | 源码/符号 | 提交/验证 | 状态 | 缺口与下一步 |
| --- | --- | --- | --- | --- | --- |
| REQ-001 | 授柄使用 SAS，保持鉴神/天枢/玉衡职责；只有授柄签发令牌，鉴神不保存密码/私钥 | `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/config/AuthorizationServerConfiguration.java`<br>`egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/controller/filter/LegacyOAuthRetirementFilter.java` | 原18Steps / C纠正；V-ADMIN | Satisfied | 已执行模块/合同证据；此行Satisfied只限该原子需求，不代表整个系统通过。 |
| REQ-002 | 路径 issuer 隔离租户客户端、授权状态、consent、JWK；A 租户 code/client/key 对 B 不可用；未知 issuer 无出站请求 | `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/service/impl/TenantIssuerServiceImpl.java`<br>`egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/service/impl/TenantAuthorizationServiceImpl.java`<br>`egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/service/impl/TenantJwkSourceComponent.java` | V-C9b-ADMIN / V-C9b-EXACT8 | Satisfied | Native pending/code/client/state/JWK隔离和Host正负例通过；真实共享状态另列runtime。 |
| REQ-003 | 第三方 Code 强制 S256 PKCE、redirect 白名单；缺 PKCE/plain/错 verifier/URI 通配符/重复 code 均拒绝 | `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/service/impl/OAuthTokenIssueServiceImpl.java`<br>`egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/domain/converter/SasAuthorizationConverter.java` | V-C10-ADMIN / V-C9b-ADMIN | Satisfied | 原生S256/隔离/重放与真实管理明确scope后兑换通过；runtime另列。 |
| REQ-004 | OIDC discovery、ID Token、UserInfo；iss/aud/nonce 校验、UserInfo sub 一致，ID Token 不能访问业务 | `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/config/AuthorizationServerConfiguration.java`<br>`egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/service/impl/TenantAuthorizationServiceImpl.java` | V-C9b-ADMIN / V-C9b-CONSUMERS | Satisfied | 实际RS256 AT/nonce ID/UserInfo/refresh、ID拒绝Bearer、claim分组/时间/issuer验证通过。 |
| REQ-005 | 每微服务独立 client_id 与最小 scope；SERVICE 无 refresh_token/ID Token；scope/resource 超授权拒绝 | `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/service/impl/OAuthClientServiceImpl.java`<br>`egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/tianquan/shoubing/starter/client/IdpServiceOAuth2Client.java` | V-C10-ADMIN / V-C9b-CONSUMERS | Satisfied | USER client上界/明确BUSINESS范围及SERVICE无ID/RT/最小scope通过；不外推部署。 |
| REQ-006 | RS256 本地验签及 aud/时间/主体校验；有缓存公钥时不逐请求调用授权服务器；错 aud/alg 拒绝 | `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/tianquan/shoubing/starter/security/TenantIssuerAuthenticationManagerComponent.java`<br>`egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/tianquan/shoubing/starter/security/IdpJwtVerifier.java` | 原18Steps / C纠正；V-STARTER / V-GATEWAY | Satisfied | 已执行模块/合同证据；此行Satisfied只限该原子需求，不代表整个系统通过。 |
| REQ-007 | tenant_id 可信上下文与 finally 清理；ThreadLocal、MDC、异步取消/异常均无跨请求污染 | `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/tianquan/shoubing/starter/security/VerifiedTenantContextFilter.java`<br>`egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin/src/main/java/top/egon/cola/platform/tianquan/jianshen/admin/shared/tenant/controller/filter/TenantContextFilter.java` | 原18Steps / C纠正；V-STARTER / V-GATEWAY | Satisfied | 已执行模块/合同证据；此行Satisfied只限该原子需求，不代表整个系统通过。 |
| REQ-008 | SQL 级租户隔离；两租户相同业务 ID、批处理/自定义 SQL/逻辑删除不能串租户 | `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/config/OAuthPersistenceConfiguration.java`<br>`egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/identity/repo/IdentityDirectoryContext.java` | 原18Steps / C纠正；V-ADMIN | Runtime unverified | 源码和受控测试已存在；真实PG/Redis/多实例/TLS、数据对账或负载证据尚未运行，按本行合同由用户控制验收。 |
| REQ-009 | WebClient 自动令牌与临期重获；键隔离、同键并发仅一次获取、访问令牌 300–600 秒 | `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/tianquan/shoubing/starter/client/IdpServiceOAuth2Client.java`<br>`egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/tianquan/shoubing/starter/client/ServiceTokenWebClientConfiguration.java` | 原18Steps / C纠正；V-STARTER / V-GATEWAY | Satisfied | 已执行模块/合同证据；此行Satisfied只限该原子需求，不代表整个系统通过。 |
| REQ-010 | SCG OAuth2 Client/BFF 入口；浏览器只持 HttpOnly 会话 cookie；下游仍自行验签 | `egon-cola-xingyuan/egon-cola-yuheng/yuheng-edge-gateway/src/main/java/top/egon/cola/component/yuheng/edge/config/EdgeSecurityConfiguration.java`<br>`egon-cola-xingyuan/egon-cola-yuheng/yuheng-edge-gateway/src/main/java/top/egon/cola/component/yuheng/edge/service/impl/EdgeSingleFlightAuthorizedClientManager.java` | V-C9b-EDGE / V-C9b-ADMIN / V-C9b-CONSUMERS | Runtime unverified | 入口和原生人员链、relay/无浏览器token合同受控通过；真实Cookie/TLS/Redis/多实例/browser回合未运行。 |
| REQ-011 | HTTPS 与 secret 哈希；生产 HTTP/明文 RPC 配置拒绝；新 client secret bcrypt | `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/service/impl/OAuthClientServiceImpl.java` | 原18Steps / C纠正；V-ADMIN | Runtime unverified | 源码和受控测试已存在；真实PG/Redis/多实例/TLS、数据对账或负载证据尚未运行，按本行合同由用户控制验收。 |
| REQ-012 | 即时撤销；撤销返回成功后新鉴权拒绝，Redis 不可用返回503，在途请求不取消 | `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/service/impl/TokenRevocationServiceImpl.java`<br>`egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/tianquan/shoubing/starter/security/OAuthRevocationReader.java` | 原18Steps / C纠正；V-ADMIN | Runtime unverified | 源码和受控测试已存在；真实PG/Redis/多实例/TLS、数据对账或负载证据尚未运行，按本行合同由用户控制验收。 |
| REQ-013 | 密钥定期轮换；多键重叠，旧 token 可验证至到期，泄露键可紧急拒绝 | `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/service/impl/OAuthSigningKeyRotationServiceImpl.java`<br>`egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/service/impl/TenantJwkSourceComponent.java` | 原18Steps / C纠正；V-ADMIN | Runtime unverified | 源码和受控测试已存在；真实PG/Redis/多实例/TLS、数据对账或负载证据尚未运行，按本行合同由用户控制验收。 |
| REQ-014 | token 申请/使用/撤销全量脱敏审计；不记录原 token/secret/code/password，投递有恢复证据 | `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/tianquan/shoubing/starter/audit/TokenUsageAuditComponent.java`<br>`egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/audit/service/impl/OAuthAuditConsumer.java` | 原18Steps / C纠正；V-ADMIN | Runtime unverified | 源码和受控测试已存在；真实PG/Redis/多实例/TLS、数据对账或负载证据尚未运行，按本行合同由用户控制验收。 |
| REQ-015 | 指标与告警；JWT 延迟、缓存命中、失败授权、撤销依赖/投递积压可观测 | `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/tianquan/shoubing/starter/audit/TokenUsageAuditComponent.java` | 原18Steps / C纠正；V-ADMIN | Runtime unverified | 源码和受控测试已存在；真实PG/Redis/多实例/TLS、数据对账或负载证据尚未运行，按本行合同由用户控制验收。 |
| REQ-016 | 保留隔离 PLATFORM 控制面；`/platform` 只服务控制面；业务 tenant token 必含 tenant_id | `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/service/impl/TenantIssuerServiceImpl.java`<br>`egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/tianquan/shoubing/starter/client/IdpServiceOAuth2Client.java` | 原18Steps / C纠正；V-STARTER / V-GATEWAY | Satisfied | 已执行模块/合同证据；此行Satisfied只限该原子需求，不代表整个系统通过。 |
| REQ-017 | 旧链迁移且保留业务数据；用户/tenant/client/grant 映射可审计；切换/回退界限明确 | `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/service/impl/OAuthMigrationServiceImpl.java`<br>`egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/controller/filter/LegacyOAuthRetirementFilter.java` | V-C10-ADMIN / V-MIGRATION / V-STATIC | Runtime unverified | USER审核scope四种源/新关联组合、恢复/冲突/重放已通过；真实数据对账与切流尚未执行。 |
| REQ-018 | 按最新 Java/CQE/MP/DDL 规范；§6.2、§20 每项证据闭合；保留传统业务分包 | `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/service/impl/TransactionalOAuthAuthenticationProvider.java`<br>`egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/config/OAuthPersistenceConfiguration.java` | V-C10-ADMIN / V-C10-STATIC / §9 | Satisfied | 最终Java/CQE/MP/immutable SQL与17MC/10Rule技术检查通过；全reactor/运行验收独立保持开放。 |
| REQ-019 | 仅 Spec、禁 generator；git diff 只有本 Spec；无 Plan/代码/运行态动作 |  | 原18Steps / C纠正；V-ADMIN | Satisfied | 原Spec-only阶段限制已由本次明确执行授权及联合修订取代；无自动启动或执行数据库授权。保留历史原文。 |
| REQ-020 | 认证四种目录事实统一MP；相同subject、Argon2 hash、锁定/版本/成员状态保留；新认证运行链无JPA写；四目录固定required P、tenant/user scope finally恢复，hash不出JSON/toString；四个BaseConverter映射由现有MapStruct版本按PO source→BO target方向正常生成并通过往返断言 | `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/identity/repo/IdentityPersistenceAdapter.java`<br>`egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/identity/support/rpc/IdentityDirectoryRpcProvider.java` | 原18Steps / C纠正；V-ADMIN | Satisfied | 已执行模块/合同证据；此行Satisfied只限该原子需求，不代表整个系统通过。 |
| REQ-021 | 认证写与组件Outbox同库同事务；同DataSource/SqlSessionFactory/DataSourceTransactionManager；enqueue失败全部回滚；Redis失败不误报撤销成功 | `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/config/OAuthPersistenceConfiguration.java`<br>`egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/support/outbox/service/IdentityOutboxPublisher.java` | 原18Steps / C纠正；V-ADMIN | Runtime unverified | 源码和受控测试已存在；真实PG/Redis/多实例/TLS、数据对账或负载证据尚未运行，按本行合同由用户控制验收。 |
| REQ-022 | 导入保留历史与可恢复性；逐字段含version/create/update对账、孤儿/冲突0；停写栅栏；无旧Flyway修改或自动启动导入 | `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/service/impl/OAuthMigrationServiceImpl.java` | 原18Steps / C纠正；V-MIGRATION / V-STATIC | Runtime unverified | 源码和受控测试已存在；真实PG/Redis/多实例/TLS、数据对账或负载证据尚未运行，按本行合同由用户控制验收。 |
| REQ-023 | MP装配及manifest family遵当前真实API；NATIVE/LOCAL精确路由15个业务表、13个只读legacy源、2个组件技术表；不使用public.*；组件DDL初始化、DB-shape分组校验、PostgreSQL jsonb object/string-array绑定、BaseConverter PO→BO类型方向、trustedRealm partition的MDC scope finally恢复、trust/credential version不回退、显式导入及tianquan-oauth family正/负例均有测试；任意未注册family仍拒绝；具名MP Clock与idpClock为同一实例 | `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/config/OAuthPersistenceConfiguration.java` | 原18Steps / C纠正；V-ADMIN | Runtime unverified | 源码和受控测试已存在；真实PG/Redis/多实例/TLS、数据对账或负载证据尚未运行，按本行合同由用户控制验收。 |
| REQ-024 | 保留Tianshu两层信任；PLATFORM注册验证及HMAC tenant绑定均通过；错/缺AI header拒绝；body/header不能给业务SQL选tenant | `egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/java/top/egon/cola/component/tianshu/admin/security/registration/IdpJwtDdcRegistrationCredentialVerifier.java`<br>`egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-tianshu-adapter/src/main/java/top/egon/cola/component/rpc/tianshu/security/DdcRpcClientInterceptorFactory.java` | 原18Steps / C纠正；V-RPC / V-MCP / V-CANCEL / V-GATEWAY | Satisfied | 已执行模块/合同证据；此行Satisfied只限该原子需求，不代表整个系统通过。 |
| REQ-025 | 保留玉衡实际数据面与流式行为；新SCG→biz/mcp/LLM路由，SSE增量/取消、Bearer验证、管理端口约束回归 | `egon-cola-xingyuan/egon-cola-yuheng/yuheng-edge-gateway/src/main/java/top/egon/cola/component/yuheng/edge/filter/TrustedIdentityHeaderFilter.java`<br>`egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-gateway-adapter/src/main/java/top/egon/cola/platform/tianquan/shoubing/yuheng/security/IdpGatewayJwtVerifier.java` | 原18Steps / C纠正；V-RPC / V-MCP / V-CANCEL / V-GATEWAY | Runtime unverified | 源码和受控测试已存在；真实PG/Redis/多实例/TLS、数据对账或负载证据尚未运行，按本行合同由用户控制验收。 |
| REQ-026 | 补齐现有Grant读合同；API-045同realm有序≤64条，空[]，65条报503；Repository查询最多读取65行以检测溢出；0写入；UI缓存含realm且处理错误 | `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/resource/controller/ClientResourceGrantController.java`<br>`egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/resource/repository/impl/OAuthGrantRepositoryImpl.java` | 原18Steps / C纠正；V-ADMIN | Satisfied | 已执行模块/合同证据；此行Satisfied只限该原子需求，不代表整个系统通过。 |
| REQ-027 | 文档与依赖方向同步最新基线；Starter无admin依赖；旧错误前提明确被替换；Spec/Plan验证通过且OAuth persistence converters遵守BaseConverter<S,T>方向 | `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/config/OAuthProtocolOpenApiConfiguration.java`<br>`egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/domain/converter/SasAuthorizationConverter.java` | V-C9b-ADMIN / V-C9b-EDGE / V-DOCS | Satisfied | 已批准的v2/API046/CSRF/字符串scope/实际OAS及生成BaseConverter一致；严格结构与源码门禁通过。 |
| REQ-028 | 加密授权/令牌载荷静态存储；authorization attributes、必要token value、token metadata仅以ENC:v1 AES-256-GCM密文存储；32-byte KEK只从mounted file加载，base/local配置键同构且无明文；AAD绑定canonical issuer、realmKey、purpose与authorization id/tokenDigest；realm通过converter显式传入；缺KEK、错realm/context、坏tag、未知keyId/schemaVersion均fail-closed；明文/KEK不出PO/BO/VO/log | `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/service/impl/OAuthPayloadCipherComponent.java`<br>`egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/domain/converter/OAuthAuthorizationConverter.java` | V-C9b-ADMIN | Satisfied | 已批准v2仅加密state；v1/unknown/错误AAD/tag与JSON/toString隐藏状态负例通过，旧cipher版本不变。 |
| REQ-029 | 管理API把已验证realm显式传至MP服务；realm缺省为principal tenant；显式tenant必须与principal一致；platform须principal tenant等于部署管理tenant且`AuthenticationContext.satisfies(STRONG, 5m, now)`；controller先授权再解析realm，并把registry解析出的`TrustedRealmBO`传给服务；跨realm拒绝且未知realm不泄露其他realm数据；Client/Resource管理视图保留已有时间和版本字段 | `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/service/impl/OAuthManagementRealmResolver.java`<br>`egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/controller/OAuthClientController.java` | 原18Steps / C纠正；V-ADMIN | Satisfied | 已执行模块/合同证据；此行Satisfied只限该原子需求，不代表整个系统通过。 |
| REQ-030 | Signing key管理使用issuer隔离的加密keyring；publish/activate/retire/list均以`TrustedRealmBO`与MP key/realm repository为边界；private key只接受可由当前mounted KEK解密的`ENC:v1` envelope并验证RS256、kid及3072-bit RSA keypair；签名SAS链按该realm的activeKid选key，不再依赖legacy global PEM kid；createdAt/updatedAt仅来自PO映射；wrong realm/version/unready依赖fail-closed | `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/service/impl/SigningKeyServiceImpl.java`<br>`egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/service/impl/TenantJwkSourceComponent.java` | 原18Steps / C纠正；V-ADMIN | Runtime unverified | 源码和受控测试已存在；真实PG/Redis/多实例/TLS、数据对账或负载证据尚未运行，按本行合同由用户控制验收。 |
| REQ-031 | 保留Resource Server bounded collection/read semantics；list uses only the fixed PLATFORM partition, stable `resourceServerId` order, query limit 10001; more than 10000 entries returns a conflict rather than silently truncating; detail queries stay partition-scoped and status/write behavior is unchanged | `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/resource/repository/impl/OAuthResourceRepositoryImpl.java` | 原18Steps / C纠正；V-ADMIN | Satisfied | 已执行模块/合同证据；此行Satisfied只限该原子需求，不代表整个系统通过。 |
| REQ-032 | Resource/CLIENT_CREDENTIALS Grant Redis投影通过提交后Outbox交付；Resource与service-Grant事实及版本化secret-free投影事件写入同一PRIMARY事务；管理Service不直接写Redis；DeliveryHandler只在Outbox消费时重载当前可信realm的MP Resource/Client/Grant并更新projection，重复/乱序消息不会回退到旧版本；Redis故障保留重试事件 | `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/resource/support/outbox/ResourceServerProjectionDeliveryHandler.java`<br>`egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/resource/support/outbox/TransactionalOutboxResourceServerEventAdapter.java` | 原18Steps / C纠正；V-ADMIN | Runtime unverified | 源码和受控测试已存在；真实PG/Redis/多实例/TLS、数据对账或负载证据尚未运行，按本行合同由用户控制验收。 |
| REQ-033 | Production redirect URIs remain exact HTTPS; local bootstrap alone permits exact IPv4 loopback HTTP；`https` callbacks remain exact/no-userinfo/no-fragment/no-query/no-wildcard in all profiles; only active `local` accepts `http` with host `127.0.0.1`, port 1–65535 and a nonempty exact path; all other HTTP/IP/host/query/fragment forms reject in both direct Service entry and HTTP input | `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/service/impl/OAuthClientServiceImpl.java` | 原18Steps / C纠正；V-ADMIN | Satisfied | 已执行模块/合同证据；此行Satisfied只限该原子需求，不代表整个系统通过。 |
| REQ-034 | Connect SAS and identity security changes to durable revocation, audit and rotation runtime；SAS token invalidation and identity subject/client/security-state changes write same-transaction security/audit facts plus the component Outbox event; fenced recovery seeds missing REALM/CLIENT generations only without authorization/token/revocation history, Client changes write CLIENT generation in their transaction, and a first authenticated USER authorization seeds SUBJECT generation zero only without any prior authorization history; after commit, same-revision Redis visibility gates success and failure returns unavailable while preserving the durable fact; base/local profiles bind accepted keys with identical structure, mounted files only, and complete consumer/handler registration | `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/service/impl/TransactionalOAuthRevocationAuthenticationProvider.java`<br>`egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/service/impl/TokenRevocationServiceImpl.java` | 原18Steps / C纠正；V-ADMIN | Runtime unverified | 源码和受控测试已存在；真实PG/Redis/多实例/TLS、数据对账或负载证据尚未运行，按本行合同由用户控制验收。 |
| REQ-035 | Provide version-guarded repository deletion for policy-expired Audit and Revocation rows；Repository ports expose only existing tenant/id/expectedVersion soft-delete operations; audit purges after 180d and aged Stream events cannot recreate purged rows; revocations purge only after expiresAt+30s, projection APPLIED and component active backlog count is zero; committed row soft-deletes are the durable progress marker and retries begin at the first remaining active candidate; kid tombstones and unexpired RT-family facts remain | `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/service/impl/OAuthRetentionServiceImpl.java`<br>`egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/audit/service/impl/OAuthAuditConsumer.java` | 原18Steps / C纠正；V-ADMIN | Satisfied | 已执行模块/合同证据；此行Satisfied只限该原子需求，不代表整个系统通过。 |
| REQ-036 | Protect BFF session and AuthorizedClient state at rest；The browser receives only an opaque `__Host` Secure/HttpOnly/SameSite=Lax session cookie; Spring Session identity/state and every AuthorizedClient access/refresh/ID token value in Redis are AES-GCM encrypted with a mounted 32-byte owner-only key; key ids use a restricted form and envelopes use canonical Base64url; the AuthorizedClient map key includes session binding, issuer, tenant, client, registration and subject; entries expire with session idle/absolute lifetime and are removed on logout; missing/wrong key, altered ciphertext, incompatible Redis endpoint or remote plaintext config fails closed; external wait/timeout bounds are validated; base/local configuration leaf sets match | `egon-cola-xingyuan/egon-cola-yuheng/yuheng-edge-gateway/src/main/java/top/egon/cola/component/yuheng/edge/service/impl/EdgeTokenCipher.java`<br>`egon-cola-xingyuan/egon-cola-yuheng/yuheng-edge-gateway/src/main/java/top/egon/cola/component/yuheng/edge/service/impl/EdgeAuthorizedClientRepository.java` | 原18Steps / C纠正；V-EDGE / V-C8c | Runtime unverified | 源码和受控测试已存在；真实PG/Redis/多实例/TLS、数据对账或负载证据尚未运行，按本行合同由用户控制验收。 |
| REQ-037 | Make BFF logout and remote token-family revocation recoverable；API-017 requires an authenticated OAuth2 BFF session and CSRF before work; atomically persists encrypted intents for every session-held authorized-client family before session invalidation; same-origin `client_secret_basic` calls revoke each belonging refresh-token family; only confirmation of all remote revocations yields 204; persisted but unconfirmed intent yields 503 `REMOTE_REVOCATION_PENDING`; failed atomic enqueue still clears the local session and returns 503 with `remoteState=UNKNOWN`; a bounded consumer group retries claimed/unacknowledged entries after restart and ACK/XDEL occurs only after 2xx; token/client secret never appears in logs or Redis plaintext | `egon-cola-xingyuan/egon-cola-yuheng/yuheng-edge-gateway/src/main/java/top/egon/cola/component/yuheng/edge/service/EdgeLogoutService.java`<br>`egon-cola-xingyuan/egon-cola-yuheng/yuheng-edge-gateway/src/main/java/top/egon/cola/component/yuheng/edge/service/impl/EdgeLogoutRevocationWorker.java` | 原18Steps / C纠正；V-EDGE / V-C8c | Runtime unverified | 源码和受控测试已存在；真实PG/Redis/多实例/TLS、数据对账或负载证据尚未运行，按本行合同由用户控制验收。 |
| REQ-038 | Publish the BFF session operations through the existing WebFlux OpenAPI contract；The `edge-bff` group describes GET `/bff/session`, GET `/bff/csrf`, POST `/bff/logout`; 204 has no response body; logout uses BFF session+CSRF and no token fields are exposed; generation is disabled by default and remains protected by the starter's read-scope chain when enabled | `egon-cola-xingyuan/egon-cola-yuheng/yuheng-edge-gateway/src/main/java/top/egon/cola/component/yuheng/edge/controller/EdgeSessionController.java`<br>`egon-cola-xingyuan/egon-cola-yuheng/yuheng-edge-gateway/src/main/java/top/egon/cola/component/yuheng/edge/config/EdgeSecurityConfiguration.java` | 原18Steps / C纠正；V-EDGE / V-C8c | Satisfied | 已执行模块/合同证据；此行Satisfied只限该原子需求，不代表整个系统通过。 |
| REQ-039 | Bind the Edge route table to the pinned Spring Cloud Gateway property prefix；Static authorization-server, biz and MCP route definitions bind under `spring.cloud.gateway.server.webflux.routes`; the wrong/legacy prefix is absent; every registered route preserves its original path and applies TokenRelay only to biz/MCP | `egon-cola-xingyuan/egon-cola-yuheng/yuheng-edge-gateway/src/main/java/top/egon/cola/component/yuheng/edge/config/EdgeSecurityConfiguration.java` | 原18Steps / C纠正；V-EDGE / V-C8c | Satisfied | 已执行模块/合同证据；此行Satisfied只限该原子需求，不代表整个系统通过。 |
| REQ-040 | Compile Edge constructor injection with the existing repository Lombok contract；Edge main/test sources compile using managed `${lombok.version}`; Lombok stays provided; every managed Edge Bean has a stable explicit name and each `@RequiredArgsConstructor` dependency has the exact copied `@Qualifier`; no new runtime dependency is introduced | `egon-cola-xingyuan/egon-cola-yuheng/yuheng-edge-gateway/src/main/java/top/egon/cola/component/yuheng/edge/config/EdgeSecurityConfiguration.java` | 原18Steps / C纠正；V-EDGE / V-C8c | Satisfied | 已执行模块/合同证据；此行Satisfied只限该原子需求，不代表整个系统通过。 |
| REQ-041 | Serialize BFF refresh-token rotation across Edge instances；Every TokenRelay manager refresh uses a distributed lock keyed by the complete session/client identity; manager reloads the latest encrypted AuthorizedClient after lock acquisition, writes rotated tokens before manager completion, and releases the lock on success/failure/timeout; lock wait and lease are bounded; an unavailable lock fails closed as unavailable and never replays the old refresh token | `egon-cola-xingyuan/egon-cola-yuheng/yuheng-edge-gateway/src/main/java/top/egon/cola/component/yuheng/edge/service/impl/EdgeSingleFlightAuthorizedClientManager.java` | 原18Steps / C纠正；V-EDGE / V-C8c | Runtime unverified | 源码和受控测试已存在；真实PG/Redis/多实例/TLS、数据对账或负载证据尚未运行，按本行合同由用户控制验收。 |
| REQ-042 | Make API-015 report only a currently valid BFF session；The session view never triggers token refresh; it locally verifies the exact held token and requires JWT issuer to match both ClientRegistration and trusted issuer, plus JWT/verified subject, tenant and Edge Resource audience; then checks shared revocation/generation state; invalid, expired or revoked token returns 401; unavailable shared Redis returns 503, not authenticated or 401; response remains token-free | `egon-cola-xingyuan/egon-cola-yuheng/yuheng-edge-gateway/src/main/java/top/egon/cola/component/yuheng/edge/controller/EdgeSessionController.java` | 原18Steps / C纠正；V-EDGE / V-C8c | Satisfied | 已执行模块/合同证据；此行Satisfied只限该原子需求，不代表整个系统通过。 |
| REQ-043 | Migrate Portal/four Admin consumers to passwordless OIDC through the Edge BFF and preserve high-risk role reauthentication；Login navigates only to the configured registration; session reads and CSRF logout use the BFF; browser never accepts or stores username/password/AT/RT; local UI clears on confirmed or pending logout; exact stepUp=true maps only to prompt=login and max_age=0; same-tab role intent stores only role IDs and expectedAuthVersion, validates then retries once after reauthentication; local auth harness routes all five consumers through an explicit Edge origin without browser passwords | `egon-cola-xingyuan/egon-cola-yuheng/yuheng-edge-gateway/src/main/java/top/egon/cola/component/yuheng/edge/config/EdgeSecurityConfiguration.java` | V-C9b-ADMIN / V-C9b-EDGE / V-FRONTEND | Runtime unverified | 实际SAS强制新密码事件、同subject STRONG/新auth_time/一次性请求证明与Edge固定Resource通过；五端真实浏览器SSO/高风险角色回合仍待用户验收。 |
| REQ-044 | Provide an explicitly invoked, offline and restart-safe identity/OAuth data migration；Only the `oauth-migration` profile can expose a one-shot runner; it requires `spring.main.web-application-type=none`, RPC/HTTP registration disabled, an explicit enable flag, exact realm/snapshot, matching operator freeze marker, owner-only checkpoint and snapshot-bound ownership/scope map, secret-output directory, a new owner-only JSON report file, and (for apply) the reviewed dry-run report SHA-256. The typed DAO reads only the accepted static legacy table allowlist and imports in batches of at most 500 while preserving external IDs, hashes, versions and timestamps. The report digest binds every mapped source field without disclosing one-way-hashed sensitive values. OAuth target writes use existing MP administration services/repositories; each confidential client gets a newly generated BCrypt secret written once to a protected file, never copying an old secret hash. USER sources without PKCE and SERVICE sources with refresh TTL are conflicts; SERVICE Resource authorization is imported as Grant rows, not client resourceUris. Ambiguous ownership, empty user scopes, malformed tenants, privilege conflicts or incomplete mappings stop the realm without checkpoint advancement. Ordinary bootstrap/scheduling are excluded from this profile | `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/support/migration/OAuthMigrationRunner.java`<br>`egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/service/impl/OAuthMigrationServiceImpl.java` | V-C10-ADMIN / V-MIGRATION | Runtime unverified | runner/历史字段/checkpoint/USER scope准确保存与恢复冲突已验证；真实offline导入和对账未运行。 |
| REQ-045 | Complete legacy protocol retirement and direct consumers；New deployment returns safe 410 for API-035–044 before reading credentials or issuing cookies/tokens; old JPA and signing/RT Beans are absent. Core password-change subject revocation delegates to INTERNAL-002 once through an Adapter, and user-management revocation invokes only the existing new service once. Local tooling uses explicit realm issuers/current MP directories and requires operator authorization prerequisites; it never logs in through old password/cookie routes or automatically imports legacy directory facts | `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/controller/filter/LegacyOAuthRetirementFilter.java`<br>`egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/repo/SubjectRevocationRefreshTokenStoreAdapter.java` | V-C9b-ADMIN / V-C9b-STATIC | Satisfied | 旧root410、密码/身份撤销Adapter、配置退役与native重新登录回合通过；未运行实际切流。 |
| REQ-046 | Production tenant SERVICE registration selection；Existing Jianshen HttpTenantServiceTokenSupplier selects registration from explicit service-client.tenant-registration-ids keyed by canonical positive tenant ID before calling the existing IdpServiceOAuth2Client; mapped entries require configured credential versions and exact trusted issuer/client/tenant checks still apply. An absent entry fails without token issuance; empty map preserves single-registration compatibility | `egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-starter/src/main/java/top/egon/cola/platform/tianquan/jianshen/starter/client/HttpTenantServiceTokenSupplier.java` | 原18Steps / C纠正；V-C13 | Satisfied | 已执行模块/合同证据；此行Satisfied只限该原子需求，不代表整个系统通过。 |

| REQ-047 | Native pending/CSRF/consent/code/OIDC/refresh/family/storage与唯一DDL闭合 | `OAuthConsentController` / `SasAuthorizationConverter` / `TenantAuthorizationServiceImpl` / `TransactionalOAuthAuthenticationProvider` / `AuthorizationServerConfiguration` | Step19，V-C9b-ADMIN / V-C9b-EDGE，TEST-173–178 | Satisfied | 实际Filter/Service/Converter与受控事务故障正负例通过；PG并发/DDL及浏览器另列runtime，不外推。 |


| REQ-048 | Existing USER BUSINESS scopes are bounded, explicit and client-limited through Core/manage/UI/import; empty denies and tenant/context are forbidden | `ClientResourceGrant` / `UpsertClientResourceGrantDTO` / `ResourceServerServiceImpl` / `OAuthMigrationServiceImpl` / `ClientResourceGrantPage` | Step20；V-C10-ADMIN / V-C10-UI / V-C10-TYPE；TEST-179–181 | Satisfied | 准确scope、自动空关联、四种迁移组合、真实managed-native与清空/禁用不消费code通过；无新API/DDL/数据回填。 |

## 6. 接口/事件/任务逐项核验

46 HTTP + 4 INTERNAL + 2 EVENT + 2 JOB。API-046已批准并实现。管理接口保持原Controller与授权/realm/version入口，不新增假协议Controller。

| ID | 用途 | 方法/路径/通道 | Owner/符号 | 实际证据 | 状态 | 限制 |
| --- | --- | --- | --- | --- | --- | --- |
| API-001 | tenant_oidc_discovery | GET /tenants/{tenantId}/.well-known/openid-configuration | SAS | V-C8c / TenantIssuerIsolationTest / OAuthTokenClientCredentialsTest | Satisfied | 实际SAS metadata/JWKS/client auth/filter合同；JWKS多节点传播另列Runtime unverified。 |
| API-002 | tenant_oauth_metadata | GET /.well-known/oauth-authorization-server/tenants/{tenantId} | SAS | V-C8c / TenantIssuerIsolationTest / OAuthTokenClientCredentialsTest | Satisfied | 实际SAS metadata/JWKS/client auth/filter合同；JWKS多节点传播另列Runtime unverified。 |
| API-003 | tenant_jwks | GET /tenants/{tenantId}/.well-known/jwks.json | SAS | V-C8c / TenantIssuerIsolationTest / OAuthTokenClientCredentialsTest | Satisfied | 实际SAS metadata/JWKS/client auth/filter合同；JWKS多节点传播另列Runtime unverified。 |
| API-004 | authorization_request | GET /tenants/{tenantId}/oauth2/authorize | SAS | V-C9b-ADMIN | Satisfied | 实际SAS原生回合/失败/CSRF/状态隔离通过；B-04由Step20关闭；真实部署另列。 |
| API-005 | authorization_consent | POST /tenants/{tenantId}/oauth2/authorize | SAS | V-C9b-ADMIN | Satisfied | 实际SAS原生回合/失败/CSRF/状态隔离通过；B-04由Step20关闭；真实部署另列。 |
| API-006 | tenant_token | POST /tenants/{tenantId}/oauth2/token | SAS | V-C9b-ADMIN | Satisfied | 实际SAS原生回合/失败/CSRF/状态隔离通过；B-04由Step20关闭；真实部署另列。 |
| API-007 | tenant_revoke | POST /tenants/{tenantId}/oauth2/revoke | SAS | V-ADMIN | Satisfied | 沿既有Controller/服务及MockMvc合同；真实环境边界见关联REQ。 |
| API-008 | tenant_userinfo | GET /tenants/{tenantId}/userinfo | SAS | V-C9b-ADMIN | Satisfied | 实际SAS原生回合/失败/CSRF/状态隔离通过；B-04由Step20关闭；真实部署另列。 |
| API-009 | platform_metadata | GET /.well-known/oauth-authorization-server/platform | SAS | V-C8c / TenantIssuerIsolationTest / OAuthTokenClientCredentialsTest | Satisfied | 实际SAS metadata/JWKS/client auth/filter合同；JWKS多节点传播另列Runtime unverified。 |
| API-010 | platform_jwks | GET /platform/.well-known/jwks.json | SAS | V-C8c / TenantIssuerIsolationTest / OAuthTokenClientCredentialsTest | Satisfied | 实际SAS metadata/JWKS/client auth/filter合同；JWKS多节点传播另列Runtime unverified。 |
| API-011 | platform_token | POST /platform/oauth2/token | SAS | V-C8c / TenantIssuerIsolationTest / OAuthTokenClientCredentialsTest | Satisfied | 实际SAS metadata/JWKS/client auth/filter合同；JWKS多节点传播另列Runtime unverified。 |
| API-012 | platform_revoke | POST /platform/oauth2/revoke | SAS | V-C8c / TenantIssuerIsolationTest / OAuthTokenClientCredentialsTest | Satisfied | 实际SAS metadata/JWKS/client auth/filter合同；JWKS多节点传播另列Runtime unverified。 |
| API-013 | bff_login | GET /oauth2/authorization/{registrationId} | SCG | V-C9b-EDGE / V-C9b-ADMIN | Runtime unverified | 固定Resource/PKCE/state/nonce/stepUp与实际原生人员链分别验证；真实Edge↔AS/TLS/Redis/browser集成未运行。 |
| API-014 | bff_callback | GET /login/oauth2/code/{registrationId} | SCG | V-C9b-EDGE / V-C9b-ADMIN | Runtime unverified | 固定Resource/PKCE/state/nonce/stepUp与实际原生人员链分别验证；真实Edge↔AS/TLS/Redis/browser集成未运行。 |
| API-015 | bff_session | GET /bff/session | SCG | V-EDGE | Satisfied | 会话/CSRF/多client logout fake边界已验证；真实Redis恢复见REQ-036/037/041。 |
| API-016 | bff_csrf | GET /bff/csrf | SCG | V-EDGE | Satisfied | 会话/CSRF/多client logout fake边界已验证；真实Redis恢复见REQ-036/037/041。 |
| API-017 | bff_logout | POST /bff/logout | SCG | V-EDGE | Satisfied | 会话/CSRF/多client logout fake边界已验证；真实Redis恢复见REQ-036/037/041。 |
| API-018 | sas_login_page | GET /tenants/{tenantId}/login | 授柄登录页 | V-C9b-ADMIN | Satisfied | 实际SAS原生回合/失败/CSRF/状态隔离通过；B-04由Step20关闭；真实部署另列。 |
| API-019 | sas_login_submit | POST /tenants/{tenantId}/login | SAS | V-C9b-ADMIN | Satisfied | 实际SAS原生回合/失败/CSRF/状态隔离通过；B-04由Step20关闭；真实部署另列。 |
| API-020 | client_list | GET /api/v1/tianquan-shoubing/clients | OAuthClientController | V-ADMIN | Satisfied | 沿既有Controller/服务及MockMvc合同；真实环境边界见关联REQ。 |
| API-021 | client_create | POST /api/v1/tianquan-shoubing/clients | OAuthClientController | V-C10-ADMIN / V-C10-UI | Satisfied | 准确USER scope经既有管理/列表/批处理和原生链验证；自动关联仍为零业务权限，空scope拒绝业务；runtime另列。 |
| API-022 | client_update | PATCH /api/v1/tianquan-shoubing/clients/{clientId} | OAuthClientController | V-ADMIN | Satisfied | 沿既有Controller/服务及MockMvc合同；真实环境边界见关联REQ。 |
| API-023 | client_secret_rotate | POST /api/v1/tianquan-shoubing/clients/{clientId}/secret-rotations | OAuthClientController | V-ADMIN | Satisfied | 沿既有Controller/服务及MockMvc合同；真实环境边界见关联REQ。 |
| API-024 | client_redirect_put | PUT /api/v1/tianquan-shoubing/clients/{clientId}/redirect-uris | OAuthClientController | V-ADMIN | Satisfied | 沿既有Controller/服务及MockMvc合同；真实环境边界见关联REQ。 |
| API-025 | client_redirect_delete | DELETE /api/v1/tianquan-shoubing/clients/{clientId}/redirect-uris | OAuthClientController | V-ADMIN | Satisfied | 沿既有Controller/服务及MockMvc合同；真实环境边界见关联REQ。 |
| API-026 | client_resource_put | PUT /api/v1/tianquan-shoubing/clients/{clientId}/resource-uris | OAuthClientController | V-C10-ADMIN / V-C10-UI | Satisfied | 准确USER scope经既有管理/列表/批处理和原生链验证；自动关联仍为零业务权限，空scope拒绝业务；runtime另列。 |
| API-027 | client_resource_delete | DELETE /api/v1/tianquan-shoubing/clients/{clientId}/resource-uris | OAuthClientController | V-ADMIN | Satisfied | 沿既有Controller/服务及MockMvc合同；真实环境边界见关联REQ。 |
| API-028 | grant_upsert | PUT /api/v1/tianquan-shoubing/clients/{clientId}/resources/{resourceServerId} | ClientResourceGrantController | V-C10-ADMIN / V-C10-UI | Satisfied | 准确USER scope经既有管理/列表/批处理和原生链验证；自动关联仍为零业务权限，空scope拒绝业务；runtime另列。 |
| API-029 | grant_delete | DELETE /api/v1/tianquan-shoubing/clients/{clientId}/resources/{resourceServerId} | ClientResourceGrantController | V-ADMIN | Satisfied | 沿既有Controller/服务及MockMvc合同；真实环境边界见关联REQ。 |
| API-030 | grant_batch | POST /api/v1/tianquan-shoubing/clients/{clientId}/resource-grants/actions/batch | ClientResourceGrantController | V-C10-ADMIN / V-C10-UI | Satisfied | 准确USER scope经既有管理/列表/批处理和原生链验证；自动关联仍为零业务权限，空scope拒绝业务；runtime另列。 |
| API-031 | key_list | GET /api/v1/tianquan-shoubing/signing-keys | SigningKeyController | V-ADMIN | Satisfied | 沿既有Controller/服务及MockMvc合同；真实环境边界见关联REQ。 |
| API-032 | key_publish | POST /api/v1/tianquan-shoubing/signing-keys | SigningKeyController | V-ADMIN | Satisfied | 沿既有Controller/服务及MockMvc合同；真实环境边界见关联REQ。 |
| API-033 | key_activate | POST /api/v1/tianquan-shoubing/signing-keys/{kid}/activate | SigningKeyController | V-ADMIN | Satisfied | 沿既有Controller/服务及MockMvc合同；真实环境边界见关联REQ。 |
| API-034 | key_retire | POST /api/v1/tianquan-shoubing/signing-keys/{kid}/retire | SigningKeyController | V-ADMIN | Satisfied | 沿既有Controller/服务及MockMvc合同；真实环境边界见关联REQ。 |
| API-035 | legacy_retired_1 | GET /.well-known/oauth-authorization-server | LegacyOAuthRetirementFilter | V-FOCUS / LegacyOAuthRetirementTest | Satisfied | 精确10个方法+路径410，先于credential解析、无Cookie/签发；其他路径不扩大匹配。 |
| API-036 | legacy_retired_2 | GET /oauth2/jwks | LegacyOAuthRetirementFilter | V-FOCUS / LegacyOAuthRetirementTest | Satisfied | 精确10个方法+路径410，先于credential解析、无Cookie/签发；其他路径不扩大匹配。 |
| API-037 | legacy_retired_3 | GET /oauth2/login/csrf | LegacyOAuthRetirementFilter | V-FOCUS / LegacyOAuthRetirementTest | Satisfied | 精确10个方法+路径410，先于credential解析、无Cookie/签发；其他路径不扩大匹配。 |
| API-038 | legacy_retired_4 | POST /oauth2/login | LegacyOAuthRetirementFilter | V-FOCUS / LegacyOAuthRetirementTest | Satisfied | 精确10个方法+路径410，先于credential解析、无Cookie/签发；其他路径不扩大匹配。 |
| API-039 | legacy_retired_5 | POST /oauth2/token | LegacyOAuthRetirementFilter | V-FOCUS / LegacyOAuthRetirementTest | Satisfied | 精确10个方法+路径410，先于credential解析、无Cookie/签发；其他路径不扩大匹配。 |
| API-040 | legacy_retired_6 | POST /oauth2/revoke | LegacyOAuthRetirementFilter | V-FOCUS / LegacyOAuthRetirementTest | Satisfied | 精确10个方法+路径410，先于credential解析、无Cookie/签发；其他路径不扩大匹配。 |
| API-041 | legacy_retired_7 | POST /oauth2/logout | LegacyOAuthRetirementFilter | V-FOCUS / LegacyOAuthRetirementTest | Satisfied | 精确10个方法+路径410，先于credential解析、无Cookie/签发；其他路径不扩大匹配。 |
| API-042 | legacy_retired_8 | POST /oauth2/step-up | LegacyOAuthRetirementFilter | V-FOCUS / LegacyOAuthRetirementTest | Satisfied | 精确10个方法+路径410，先于credential解析、无Cookie/签发；其他路径不扩大匹配。 |
| API-043 | legacy_retired_9 | GET /oauth2/userinfo | LegacyOAuthRetirementFilter | V-FOCUS / LegacyOAuthRetirementTest | Satisfied | 精确10个方法+路径410，先于credential解析、无Cookie/签发；其他路径不扩大匹配。 |
| API-044 | legacy_retired_10 | POST /internal/v1/oauth2/refresh-token/validate | LegacyOAuthRetirementFilter | V-FOCUS / LegacyOAuthRetirementTest | Satisfied | 精确10个方法+路径410，先于credential解析、无Cookie/签发；其他路径不扩大匹配。 |
| API-045 | List client grants | GET /api/v1/tianquan-shoubing/clients/{clientId}/resources | ClientResourceGrantController | V-C10-ADMIN / V-C10-UI | Satisfied | 准确USER scope经既有管理/列表/批处理和原生链验证；自动关联仍为零业务权限，空scope拒绝业务；runtime另列。 |
| API-046 | native_consent_page | GET /tenants/{tenantId}/consent | OAuthConsentController | Step19 / V-C9b-ADMIN / published OAS | Satisfied | escaped HTML/session/state/client/CSRF/过期/cancel/Host负例；真实布局/键盘浏览器验收另列。 |
| EVENT-001 | OAuthSecurityChangedEvent | oauth-security / redis-security-projection-v1 | OAuthSecurityChangedEvent | V-ADMIN / V-STARTER | Runtime unverified | 同事务enqueue/幂等/retry/retention单元合同存在；真实Outbox、Stream投递/重启/时钟与多节点验收未做。 |
| EVENT-002 | TokenUsageAuditEvent | egon:oauth:audit:v1 | TokenUsageAuditEvent | V-ADMIN / V-STARTER | Runtime unverified | 同事务enqueue/幂等/retry/retention单元合同存在；真实Outbox、Stream投递/重启/时钟与多节点验收未做。 |
| INTERNAL-001 | OAuthTokenIssueService.consumeAndIssue | consumeAndIssue(TrustedRealmBO, ConsumeOAuthTokenCommand) | OAuthTokenIssueService.consumeAndIssue | V-C9b-ADMIN | Satisfied | 实际SAS原生回合/失败/CSRF/状态隔离通过；B-04由Step20关闭；真实部署另列。 |
| INTERNAL-002 | TokenRevocationService.revokeSubject | revokeSubject(OAuthSubjectRevokeCommand) | TokenRevocationService.revokeSubject | V-ADMIN / V-STARTER | Satisfied | 沿既有Controller/服务及MockMvc合同；真实环境边界见关联REQ。 |
| INTERNAL-003 | OAuthRevocationReader.check | check(VerifiedJwtBO) | OAuthRevocationReader.check | V-ADMIN / V-STARTER | Satisfied | 沿既有Controller/服务及MockMvc合同；真实环境边界见关联REQ。 |
| INTERNAL-004 | IdpServiceOAuth2Client.authorize | authorize(IdpServiceTokenRequest) | IdpServiceOAuth2Client.authorize | V-ADMIN / V-STARTER | Satisfied | 沿既有Controller/服务及MockMvc合同；真实环境边界见关联REQ。 |
| JOB-001 | OAuthSigningKeyRotationService.rotateDueRealms | 每日02:00 UTC；逐issuer检查90天周期 | OAuthSigningKeyRotationService.rotateDueRealms | V-ADMIN / V-STARTER | Runtime unverified | 同事务enqueue/幂等/retry/retention单元合同存在；真实Outbox、Stream投递/重启/时钟与多节点验收未做。 |
| JOB-002 | OAuthRetentionService.cleanExpired | 每小时；每realm每批500 | OAuthRetentionService.cleanExpired | V-ADMIN / V-STARTER | Runtime unverified | 同事务enqueue/幂等/retry/retention单元合同存在；真实Outbox、Stream投递/重启/时钟与多节点验收未做。 |


## 7. 数据、状态、页面与非功能合同

所有旧Flyway内容必须保持不可变；新表由MP-SDJ管理。真实SQL、软删除唯一性/NULL、JSONB、路由、事务提交仍需PG验收。

| 表（逻辑名） | 共同边界 | 状态 | 证据 | 缺口 |
| --- | --- | --- | --- | --- |
| oauth_identity_user | MP管理PRIMARY / NATIVE / LOCAL；tenant/deleted_at/version生命周期；原SQL只新增不改旧 | Runtime unverified | V-ADMIN / OAuthManagedSchemaContractTest / persistence suites | PO/BO/BaseConverter/Mapper/Repository合同及静态DDL已检查；物理PG约束/分片/并发需要运行。 |
| oauth_identity_credential | MP管理PRIMARY / NATIVE / LOCAL；tenant/deleted_at/version生命周期；原SQL只新增不改旧 | Runtime unverified | V-ADMIN / OAuthManagedSchemaContractTest / persistence suites | PO/BO/BaseConverter/Mapper/Repository合同及静态DDL已检查；物理PG约束/分片/并发需要运行。 |
| oauth_identity_tenant | MP管理PRIMARY / NATIVE / LOCAL；tenant/deleted_at/version生命周期；原SQL只新增不改旧 | Runtime unverified | V-ADMIN / OAuthManagedSchemaContractTest / persistence suites | PO/BO/BaseConverter/Mapper/Repository合同及静态DDL已检查；物理PG约束/分片/并发需要运行。 |
| oauth_identity_membership | MP管理PRIMARY / NATIVE / LOCAL；tenant/deleted_at/version生命周期；原SQL只新增不改旧 | Runtime unverified | V-ADMIN / OAuthManagedSchemaContractTest / persistence suites | PO/BO/BaseConverter/Mapper/Repository合同及静态DDL已检查；物理PG约束/分片/并发需要运行。 |
| oauth_realm | MP管理PRIMARY / NATIVE / LOCAL；tenant/deleted_at/version生命周期；原SQL只新增不改旧 | Runtime unverified | V-ADMIN / OAuthManagedSchemaContractTest / persistence suites | PO/BO/BaseConverter/Mapper/Repository合同及静态DDL已检查；物理PG约束/分片/并发需要运行。 |
| oauth_client | MP管理PRIMARY / NATIVE / LOCAL；tenant/deleted_at/version生命周期；原SQL只新增不改旧 | Runtime unverified | V-ADMIN / OAuthManagedSchemaContractTest / persistence suites | PO/BO/BaseConverter/Mapper/Repository合同及静态DDL已检查；物理PG约束/分片/并发需要运行。 |
| oauth_grant | MP管理PRIMARY / NATIVE / LOCAL；tenant/deleted_at/version生命周期；原SQL只新增不改旧 | Runtime unverified | V-ADMIN / OAuthManagedSchemaContractTest / persistence suites | PO/BO/BaseConverter/Mapper/Repository合同及静态DDL已检查；物理PG约束/分片/并发需要运行。 |
| oauth_authorization | MP管理PRIMARY / NATIVE / LOCAL；tenant/deleted_at/version生命周期；原SQL只新增不改旧 | Runtime unverified | V-C9b-ADMIN / V-C9b-STATIC / 新V20261009_001 | 原生state/期限/同意已闭合；唯一新脚本和旧字节验证通过；真实PG CHECK/并发/managed journal未执行。 |
| oauth_token | MP管理PRIMARY / NATIVE / LOCAL；tenant/deleted_at/version生命周期；原SQL只新增不改旧 | Runtime unverified | V-C9b-ADMIN / V-C9b-STATIC / 新V20261009_001 | 原生state/期限/同意已闭合；唯一新脚本和旧字节验证通过；真实PG CHECK/并发/managed journal未执行。 |
| oauth_consent | MP管理PRIMARY / NATIVE / LOCAL；tenant/deleted_at/version生命周期；原SQL只新增不改旧 | Runtime unverified | V-C9b-ADMIN / V-C9b-STATIC / 新V20261009_001 | 原生state/期限/同意已闭合；唯一新脚本和旧字节验证通过；真实PG CHECK/并发/managed journal未执行。 |
| oauth_signing_key | MP管理PRIMARY / NATIVE / LOCAL；tenant/deleted_at/version生命周期；原SQL只新增不改旧 | Runtime unverified | V-ADMIN / OAuthManagedSchemaContractTest / persistence suites | PO/BO/BaseConverter/Mapper/Repository合同及静态DDL已检查；物理PG约束/分片/并发需要运行。 |
| oauth_resource | MP管理PRIMARY / NATIVE / LOCAL；tenant/deleted_at/version生命周期；原SQL只新增不改旧 | Runtime unverified | V-ADMIN / OAuthManagedSchemaContractTest / persistence suites | PO/BO/BaseConverter/Mapper/Repository合同及静态DDL已检查；物理PG约束/分片/并发需要运行。 |
| oauth_security_state | MP管理PRIMARY / NATIVE / LOCAL；tenant/deleted_at/version生命周期；原SQL只新增不改旧 | Runtime unverified | V-ADMIN / OAuthManagedSchemaContractTest / persistence suites | PO/BO/BaseConverter/Mapper/Repository合同及静态DDL已检查；物理PG约束/分片/并发需要运行。 |
| oauth_revocation | MP管理PRIMARY / NATIVE / LOCAL；tenant/deleted_at/version生命周期；原SQL只新增不改旧 | Runtime unverified | V-ADMIN / OAuthManagedSchemaContractTest / persistence suites | PO/BO/BaseConverter/Mapper/Repository合同及静态DDL已检查；物理PG约束/分片/并发需要运行。 |
| oauth_audit | MP管理PRIMARY / NATIVE / LOCAL；tenant/deleted_at/version生命周期；原SQL只新增不改旧 | Runtime unverified | V-ADMIN / OAuthManagedSchemaContractTest / persistence suites | PO/BO/BaseConverter/Mapper/Repository合同及静态DDL已检查；物理PG约束/分片/并发需要运行。 |


| 模型/命令/事件 | 有效字段/校验合同 | 状态 | 证据 | 限制 |
| --- | --- | --- | --- | --- |
| OAuthRealm | oauth / tianquan_oauth.oauth_realm / §11同表MP边界；REQ-002/008/012/013/014/017 | Satisfied | V-ADMIN / V-C8c / V-EDGE；实际生成Mapper与序列化合同 | 静态/模块层；关联表或完整登录运行验收不因此通过 |
| OAuthClient | oauth / tianquan_oauth.oauth_client / §11同表MP边界；REQ-002/008/012/013/014/017 | Satisfied | V-ADMIN / V-C8c / V-EDGE；实际生成Mapper与序列化合同 | 静态/模块层；关联表或完整登录运行验收不因此通过 |
| OAuthGrant | resource / tianquan_oauth.oauth_grant / §11同表MP边界；REQ-002/008/012/013/014/017 | Satisfied | V-ADMIN / V-C8c / V-EDGE；实际生成Mapper与序列化合同 | 静态/模块层；关联表或完整登录运行验收不因此通过 |
| OAuthAuthorization | oauth / tianquan_oauth.oauth_authorization / §11同表MP边界；REQ-002/008/012/013/014/017 | Satisfied | V-ADMIN / V-C8c / V-EDGE；实际生成Mapper与序列化合同 | 静态/模块层；关联表或完整登录运行验收不因此通过 |
| OAuthToken | token / tianquan_oauth.oauth_token / §11同表MP边界；REQ-002/008/012/013/014/017 | Satisfied | V-ADMIN / V-C8c / V-EDGE；实际生成Mapper与序列化合同 | 静态/模块层；关联表或完整登录运行验收不因此通过 |
| OAuthConsent | oauth / tianquan_oauth.oauth_consent / §11同表MP边界；REQ-002/008/012/013/014/017 | Satisfied | V-ADMIN / V-C8c / V-EDGE；实际生成Mapper与序列化合同 | 静态/模块层；关联表或完整登录运行验收不因此通过 |
| OAuthSigningKey | token / tianquan_oauth.oauth_signing_key / §11同表MP边界；REQ-002/008/012/013/014/017 | Satisfied | V-ADMIN / V-C8c / V-EDGE；实际生成Mapper与序列化合同 | 静态/模块层；关联表或完整登录运行验收不因此通过 |
| OAuthSecurityState | token / tianquan_oauth.oauth_security_state / §11同表MP边界；REQ-002/008/012/013/014/017 | Satisfied | V-ADMIN / V-C8c / V-EDGE；实际生成Mapper与序列化合同 | 静态/模块层；关联表或完整登录运行验收不因此通过 |
| OAuthRevocation | token / tianquan_oauth.oauth_revocation / §11同表MP边界；REQ-002/008/012/013/014/017 | Satisfied | V-ADMIN / V-C8c / V-EDGE；实际生成Mapper与序列化合同 | 静态/模块层；关联表或完整登录运行验收不因此通过 |
| OAuthResource | resource / tianquan_oauth.oauth_resource / 资源身份及管理client旧FK直接依赖，REQ-005/017 | Satisfied | V-ADMIN / V-C8c / V-EDGE；实际生成Mapper与序列化合同 | 静态/模块层；关联表或完整登录运行验收不因此通过 |
| OAuthAudit | audit / tianquan_oauth.oauth_audit / §11同表MP边界；REQ-002/008/012/013/014/017 | Satisfied | V-ADMIN / V-C8c / V-EDGE；实际生成Mapper与序列化合同 | 静态/模块层；关联表或完整登录运行验收不因此通过 |
| OAuthClientController | @Operation/@ApiResponses/@SecurityRequirement 或Filter customizer / oauthClientList；Bearer USER + 原有操作权限 + §9.0 realm管理边界 / 单一method/path、完整schema/状态/安全，无重复或secret示例 | Satisfied | V-ADMIN / V-C8c / V-EDGE；实际生成Mapper与序列化合同 | 静态/模块层；关联表或完整登录运行验收不因此通过 |
| OAuthProtocolOpenApiConfiguration | 真实SAS/SCG Filter endpoint customizer / §9.1 operationId/path/method / 协议路径与discovery一致，public覆盖全局security | Satisfied | V-ADMIN / V-C8c / V-EDGE；实际生成Mapper与序列化合同 | 静态/模块层；关联表或完整登录运行验收不因此通过 |
| EdgeSessionController | @Operation/@ApiResponses，cookie+CSRF说明 / BFF操作表 / session/logout无token泄露 | Satisfied | V-ADMIN / V-C8c / V-EDGE；实际生成Mapper与序列化合同 | 静态/模块层；关联表或完整登录运行验收不因此通过 |
| OAuthRealmPO / OAuthRealmBO | 持久化/Service内部，不作为HTTP响应 / §11 oauth_realm 所有业务列snake_case→camelCase；PO还含EgonModel八列 / PO Persisted/Insert/Update/Delete groups；BO Required/Command/Query groups | Satisfied | V-ADMIN / V-C8c / V-EDGE；实际生成Mapper与序列化合同 | 静态/模块层；关联表或完整登录运行验收不因此通过 |
| OAuthClientPO / OAuthClientBO | 持久化/Service内部，不作为HTTP响应 / §11 oauth_client 所有业务列snake_case→camelCase；PO还含EgonModel八列 / PO Persisted/Insert/Update/Delete groups；BO Required/Command/Query groups | Satisfied | V-ADMIN / V-C8c / V-EDGE；实际生成Mapper与序列化合同 | 静态/模块层；关联表或完整登录运行验收不因此通过 |
| OAuthGrantPO / OAuthGrantBO | 持久化/Service内部，不作为HTTP响应 / §11 oauth_grant 所有业务列snake_case→camelCase；PO还含EgonModel八列 / PO Persisted/Insert/Update/Delete groups；BO Required/Command/Query groups | Satisfied | V-ADMIN / V-C8c / V-EDGE；实际生成Mapper与序列化合同 | 静态/模块层；关联表或完整登录运行验收不因此通过 |
| OAuthAuthorizationPO / OAuthAuthorizationBO | 持久化/Service内部，不作为HTTP响应 / §11 oauth_authorization 所有业务列snake_case→camelCase；PO还含EgonModel八列 / PO Persisted/Insert/Update/Delete groups；BO Required/Command/Query groups | Satisfied | V-C9b-ADMIN；真实Filter与实际生成Mapper/claim分组/加密重载 | 实际native回合已验证；不代表PG或浏览器运行。 |
| OAuthTokenPO / OAuthTokenBO | 持久化/Service内部，不作为HTTP响应 / §11 oauth_token 所有业务列snake_case→camelCase；PO还含EgonModel八列 / PO Persisted/Insert/Update/Delete groups；BO Required/Command/Query groups | Satisfied | V-C9b-ADMIN；真实Filter与实际生成Mapper/claim分组/加密重载 | 实际native回合已验证；不代表PG或浏览器运行。 |
| OAuthConsentPO / OAuthConsentBO | 持久化/Service内部，不作为HTTP响应 / §11 oauth_consent 所有业务列snake_case→camelCase；PO还含EgonModel八列 / PO Persisted/Insert/Update/Delete groups；BO Required/Command/Query groups | Satisfied | V-ADMIN / V-C8c / V-EDGE；实际生成Mapper与序列化合同 | 静态/模块层；关联表或完整登录运行验收不因此通过 |
| OAuthSigningKeyPO / OAuthSigningKeyBO | 持久化/Service内部，不作为HTTP响应 / §11 oauth_signing_key 所有业务列snake_case→camelCase；PO还含EgonModel八列 / PO Persisted/Insert/Update/Delete groups；BO Required/Command/Query groups | Satisfied | V-ADMIN / V-C8c / V-EDGE；实际生成Mapper与序列化合同 | 静态/模块层；关联表或完整登录运行验收不因此通过 |
| OAuthSecurityStatePO / OAuthSecurityStateBO | 持久化/Service内部，不作为HTTP响应 / §11 oauth_security_state 所有业务列snake_case→camelCase；PO还含EgonModel八列 / PO Persisted/Insert/Update/Delete groups；BO Required/Command/Query groups | Satisfied | V-ADMIN / V-C8c / V-EDGE；实际生成Mapper与序列化合同 | 静态/模块层；关联表或完整登录运行验收不因此通过 |
| OAuthRevocationPO / OAuthRevocationBO | 持久化/Service内部，不作为HTTP响应 / §11 oauth_revocation 所有业务列snake_case→camelCase；PO还含EgonModel八列 / PO Persisted/Insert/Update/Delete groups；BO Required/Command/Query groups | Satisfied | V-ADMIN / V-C8c / V-EDGE；实际生成Mapper与序列化合同 | 静态/模块层；关联表或完整登录运行验收不因此通过 |
| OAuthAuditPO / OAuthAuditBO | 持久化/Service内部，不作为HTTP响应 / §11 oauth_audit 所有业务列snake_case→camelCase；PO还含EgonModel八列 / PO Persisted/Insert/Update/Delete groups；BO Required/Command/Query groups | Satisfied | V-ADMIN / V-C8c / V-EDGE；实际生成Mapper与序列化合同 | 静态/模块层；关联表或完整登录运行验收不因此通过 |
| OAuthSecuritySnapshotBO | SJ/token/domain/bo / realmGeneration:long, clientGeneration:long, subjectGeneration:Long?, readyEpoch:String, revision:long / 全部非负；USER subject必填，SERVICE null；readyEpoch非空只内部传递 | Satisfied | V-ADMIN / V-C8c / V-EDGE；实际生成Mapper与序列化合同 | 静态/模块层；关联表或完整登录运行验收不因此通过 |
| OAuthAuthorizationAttributesBO | SJ/oauth/domain/bo / schemaVersion:int=1, nonce:String?, codeChallenge:String?, codeChallengeMethod:String?, redirectUri:URI?, stateDigest:String?, securitySnapshot:OAuthSecuritySnapshotBO, acr:String?, authTime:Instant? / 授权码保留交易信息，clientCredentials相关字段null；不存原password/state，@Valid嵌套 | Satisfied | V-C9b-ADMIN；真实Filter与实际生成Mapper/claim分组/加密重载 | 实际native回合已验证；不代表PG或浏览器运行。 |
| OAuthTokenMetadataBO | SJ/token/domain/bo / schemaVersion:int=1, claims:SignedTokenClaimsBO?, invalidated:boolean, codeConsumed:boolean / 只允许声明字段；拒未知schema版本；不得@DefaultTyping反序列化 | Satisfied | V-ADMIN / V-C8c / V-EDGE；实际生成Mapper与序列化合同 | 静态/模块层；关联表或完整登录运行验收不因此通过 |
| SignedTokenClaimsBO | SJ/token/domain/bo / §10 JWT字段全集 + authorizationId:String, clientRegistrationId:String, securityVersion:OAuthSecurityVersionBO / 所有AT带authorization/client内部实例及安全世代，防删除重建复活 | Satisfied | V-C9b-ADMIN；真实Filter与实际生成Mapper/claim分组/加密重载 | 实际native回合已验证；不代表PG或浏览器运行。 |
| OAuthSecurityVersionBO | SJ/token/domain/bo / realm:long, client:long, subject:Long? / USER三项都有；SERVICE无subject；非负，Redis精确比较 | Satisfied | V-ADMIN / V-C8c / V-EDGE；实际生成Mapper与序列化合同 | 静态/模块层；关联表或完整登录运行验收不因此通过 |
| OAuthSecurityChangedEvent | SC/src/main/java/top/egon/cola/platform/tianquan/shoubing/core/audit / eventId:String, schemaVersion:int=1, issuer:URI, realmKey:String, revision:long, targetType:String, targetKey:String, generation:Long?, expiresAt:Instant?, reasonCode:String, occurredAt:Instant, traceId:String? / EVENT-001；发生事实，Outbox投递；字段白名单约束 | Satisfied | V-ADMIN / V-C8c / V-EDGE；实际生成Mapper与序列化合同 | 静态/模块层；关联表或完整登录运行验收不因此通过 |
| TokenUsageAuditEvent | SC/src/main/java/top/egon/cola/platform/tianquan/shoubing/core/audit / eventId:String, schemaVersion:int=1, issuer:URI?, resource:String, subjectRef:String?, clientRef:String?, tokenRef:String?, result:String, reasonCode:String, occurredAt:Instant, traceId:String? / EVENT-002；坏issuer仅有散列/空，不保存攻击URL；无原token | Satisfied | V-ADMIN / V-C8c / V-EDGE；实际生成Mapper与序列化合同 | 静态/模块层；关联表或完整登录运行验收不因此通过 |
| ConsumeOAuthTokenCommand | SJ/oauth/domain/command / realmKey:String, tokenDigest:String, clientPk:Long, tokenType:OAuthTokenTypeEnum, verifier:String?, redirectUri:URI? / 非HTTP标准SPI输入转换后校验；verifier只内存并exclude | Satisfied | V-ADMIN / V-C8c / V-EDGE；实际生成Mapper与序列化合同 | 静态/模块层；关联表或完整登录运行验收不因此通过 |
| RevokeOAuthTokenCommand | SJ/token/domain/command / realmKey:String, tokenDigest:String, clientPk:Long, reasonCode:String / 只允许拥有client，context不从digest反推跨realm | Satisfied | V-ADMIN / V-C8c / V-EDGE；实际生成Mapper与序列化合同 | 静态/模块层；关联表或完整登录运行验收不因此通过 |
| OAuthSubjectRevokeCommand | SJ/token/domain/command / subject:String, reasonCode:String / 来自已授权内部账户安全操作；枚举所有含该subject授权的realm，升generation，不能受用户header限缩 | Satisfied | V-ADMIN / V-C8c / V-EDGE；实际生成Mapper与序列化合同 | 静态/模块层；关联表或完整登录运行验收不因此通过 |
| EdgeSessionResult | edge/controller/domain / authenticated:boolean, tenantId:String, subject:String, displayName:String / 与API-015一致，无token），class+@Builder | Satisfied | V-ADMIN / V-C8c / V-EDGE；实际生成Mapper与序列化合同 | 静态/模块层；关联表或完整登录运行验收不因此通过 |
| EdgeCsrfResult | edge/controller/domain / headerName:String=X-CSRF-TOKEN, token:String / 与API-016一致；敏感exclude，no-store | Satisfied | V-ADMIN / V-C8c / V-EDGE；实际生成Mapper与序列化合同 | 静态/模块层；关联表或完整登录运行验收不因此通过 |


| JWT/state合同 | 状态 | 实际证据与缺口 |
| --- | --- | --- |
| AT issuer/aud/typ/kid/security_version/tenant/subject/scope | Satisfied | V-C9b-ADMIN/CONSUMERS真实USER与SERVICE签名、specific-token scope重载/RS256/at+jwt/issuer/aud和revocation替身正负例 |
| ID Token nonce/aud/sub/acr/auth_time与业务拒绝 | Satisfied | V-C10-ADMIN native25包含真实SAS RS256 ID/nonce/azp/auth_time/UserInfo/RT及资源拒ID Token；跨系统浏览器证明另列runtime |
| CODE/RT单次CAS、family绝对期限和重放 | Runtime unverified | native25实际pending→S256/code→AT/ID/RT、绝对family期限和顺序重放/受控回滚通过；真实PG CAS并发未运行 |
| SAS state/请求scope/用户principal重建 | Satisfied | Step19实际v2密文/原scope/state/session/principal/authTime/acr回合通过；普通JSON隐藏state |
| BFF opaque cookie/加密session与AuthorizedClient | Runtime unverified | Edge tests验证cipher/key/binding/no-token view；真实Session Redis/Lettuce+Redisson与跨实例期限/锁验收未做 |


| UI验收项（主Spec§12） | 状态 | 源码/测试证据与限制 |
| --- | --- | --- |
| 五个Admin/Portal登录导航 | Runtime unverified | 共享/五package历史门禁及新native/Edge测试通过；实际SSO/浏览器未运行 |
| AS登录表单/CSRF/错误/锁定 | Runtime unverified | 原生MockMvc登录→pending/CSRF/同意/兑换已通过；真实浏览器与Redis session仍未运行 |
| 同意页应用名/tenant/scope/取消/转义/未默认全选 | Runtime unverified | API046已实现且MockMvc页面/CSRF/unchecked/cancel/转义通过；真实浏览器布局/键盘未运行 |
| AuthContext bootstrap/401/403/503与缓存边界 | Satisfied | 共享Vitest/对应AuthContext tests；token-free BFF session |
| 本地退出/远端PENDING/UNKNOWN与重试 | Runtime unverified | fake queue/WebFlux合同通过；真实Redis恢复/远端revoke未做 |
| stepUp角色意图一次重试和authVersion校验 | Runtime unverified | 原角色/shared tests与Step19新SAS actual password/STRONG/auth_time/one-use proof通过；真实浏览器角色回合未运行 |
| Client/key/Grant权限、版本冲突和secret一次展示 | Satisfied | 现有React页面与Admin controller/service tests；未进行浏览器视觉/键盘验收 |


| NFR审计ID（非Spec新增需求） | Spec源/目标 | 状态 | 证据与下一步 |
| --- | --- | --- | --- |
| NFR-A01 | §15.1 严格撤销/fail-closed/恢复fence | Runtime unverified | revocation Reader/Projection tests；真实Redis故障、多节点并发和恢复epoch待用户环境 |
| NFR-A02 | §15.2 key生命周期/激活ready | Runtime unverified | SigningKeyService/Rotation/JWK测试；多实例激活传播/退役窗口TEST-008未做 |
| NFR-A03 | §15.3 脱敏全量审计、durable spool/Outbox/Stream | Runtime unverified | audit/consumer/spool tests；实际disk容量、Streams pending重启/backpressure待验 |
| NFR-A04 | §15.4 六指标/告警及p95/命中率 | Runtime unverified | MeterRegistry emitter code并非仪表盘运行证明；p95 JWT≤5ms/Redis≤10ms、cache≥99%、积压/拒绝/key告警未压测 |
| NFR-A05 | §15.5 TLS/秘密挂载/profile叶子和外部配置 | Runtime unverified | 配置静态/绑定正负例存在；真实证书、owner-only KEK、operator realm registry/PRIMARY endpoint尚未验收 |
| NFR-A06 | §12/§16 accessibility、部署/切流/回退 | Partial | 原生同意页/错误/状态与前端50存在；键盘/屏幕阅读器/窄屏、真实备份/停写/切流/回退未执行 |


### 配置与部署叶子合同

下列逐项保留主Spec键契约；实际改名/替代以有效联合修订§15/16为准，不将历史字段要求重新写回生产配置。C18明确删除已无消费者的Admin全局issuer/login/signing/cookie叶子，Starter显式trusted issuer/KEK/CORS及独立RPC签名仍有效。

| Owner | 主Spec配置键 | 类型/默认 | 合同 | 状态 | 证据/限制 |
| --- | --- | --- | --- | --- | --- |
| SA | egon.tianquan-shoubing.oauth.public-origin | URI；必填 | local与prod均HTTPS；与证书/反向代理一致 | Runtime unverified | base/local binding/leaf tests、V-STATIC；外部operator配置未运行；退役项按联合修订/C18替换 |
| SA/SS/SG/edge | egon.cola.platform.tianquan.shoubing.trusted-issuers | map；必填 | 每项issuer/jwks-uri/realm-key/tenant-id?/status/trust-version；不允许任意URL | Runtime unverified | base/local binding/leaf tests、V-STATIC；外部operator配置未运行；退役项按联合修订/C18替换 |
| SA | egon.tianquan-shoubing.oauth.platform-management-tenant-id | Long；必填 | 来自已有平台管理员实际tenant，正数；不默认0 | Runtime unverified | base/local binding/leaf tests、V-STATIC；外部operator配置未运行；退役项按联合修订/C18替换 |
| SA | egon.tianquan-shoubing.oauth.access-token-ttl | Duration=300s | 300–600s | Runtime unverified | base/local binding/leaf tests、V-STATIC；外部operator配置未运行；退役项按联合修订/C18替换 |
| SA | egon.tianquan-shoubing.oauth.authorization-code-ttl | Duration=60s | 30–120s | Runtime unverified | base/local binding/leaf tests、V-STATIC；外部operator配置未运行；退役项按联合修订/C18替换 |
| SA | egon.tianquan-shoubing.oauth.refresh-token-ttl | Duration=24h | 1h–7d，confidential USER才启用；family绝对期限不滑动 | Runtime unverified | base/local binding/leaf tests、V-STATIC；外部operator配置未运行；退役项按联合修订/C18替换 |
| SA | egon.tianquan-shoubing.oauth.pkce-required | boolean=true | USER不可关闭 | Runtime unverified | base/local binding/leaf tests、V-STATIC；外部operator配置未运行；退役项按联合修订/C18替换 |
| SA | egon.tianquan-shoubing.oauth.pkce-methods | set=[S256] | 禁止plain | Runtime unverified | base/local binding/leaf tests、V-STATIC；外部operator配置未运行；退役项按联合修订/C18替换 |
| SA | egon.tianquan-shoubing.oauth.key-encryption.key-id | String；必填 | KEK版本标识，不是原key | Runtime unverified | base/local binding/leaf tests、V-STATIC；外部operator配置未运行；退役项按联合修订/C18替换 |
| SA | egon.tianquan-shoubing.oauth.key-encryption.key-file | Path；必填 | 挂载32字节AES key、0600，生产拒缺失/明文配置 | Runtime unverified | base/local binding/leaf tests、V-STATIC；外部operator配置未运行；退役项按联合修订/C18替换 |
| SA | egon.tianquan-shoubing.oauth.key-rotation.period | Duration=90d | 周期不短于重叠窗口 | Runtime unverified | base/local binding/leaf tests、V-STATIC；外部operator配置未运行；退役项按联合修订/C18替换 |
| SA | egon.tianquan-shoubing.oauth.key-rotation.prepublish | Duration=24h | 公钥先于激活 | Runtime unverified | base/local binding/leaf tests、V-STATIC；外部operator配置未运行；退役项按联合修订/C18替换 |
| SA | egon.tianquan-shoubing.oauth.key-rotation.schedule | cron=0 0 2 * * * | UTC | Runtime unverified | base/local binding/leaf tests、V-STATIC；外部operator配置未运行；退役项按联合修订/C18替换 |
| SA/SS/SG/edge | egon.cola.platform.tianquan.shoubing.clock-skew | Duration=30s | 只能0–60s，不掩盖坏时钟 | Runtime unverified | base/local binding/leaf tests、V-STATIC；外部operator配置未运行；退役项按联合修订/C18替换 |
| SS/SG/edge | egon.cola.platform.tianquan.shoubing.jwks-cache.ttl | Duration=300s | 有界缓存；unknown kid合并刷新 | Runtime unverified | base/local binding/leaf tests、V-STATIC；外部operator配置未运行；退役项按联合修订/C18替换 |
| SS/SG/edge | egon.cola.platform.tianquan.shoubing.jwks-cache.max-issuers | int=1000 | 超容量拒新装载并报警，不能无界外部输入cache | Runtime unverified | base/local binding/leaf tests、V-STATIC；外部operator配置未运行；退役项按联合修订/C18替换 |
| SS | egon.cola.platform.tianquan.shoubing.service-client.renewal-skew | Duration=60s | 小于AT TTL | Runtime unverified | base/local binding/leaf tests、V-STATIC；外部operator配置未运行；退役项按联合修订/C18替换 |
| SS | egon.cola.platform.tianquan.shoubing.service-client.cache-max-entries | int=10000 | 到期/轮换驱逐 | Runtime unverified | base/local binding/leaf tests、V-STATIC；外部operator配置未运行；退役项按联合修订/C18替换 |
| SS | egon.cola.platform.tianquan.shoubing.service-client.connect-timeout | Duration=2s | TLS严格验证 | Runtime unverified | base/local binding/leaf tests、V-STATIC；外部operator配置未运行；退役项按联合修订/C18替换 |
| SS | egon.cola.platform.tianquan.shoubing.service-client.read-timeout | Duration=5s | 无无限重试 | Runtime unverified | base/local binding/leaf tests、V-STATIC；外部operator配置未运行；退役项按联合修订/C18替换 |
| SA/SS/SG/edge | egon.cola.platform.tianquan.shoubing.revocation.primary-endpoint | URI；必填 | 受fence管理的Redis PRIMARY；TLS；禁止读副本 | Runtime unverified | base/local binding/leaf tests、V-STATIC；外部operator配置未运行；退役项按联合修订/C18替换 |
| SA/SS/SG/edge | egon.cola.platform.tianquan.shoubing.revocation.mode | enum=STRICT_SHARED | 本Spec只有此模式，不提供fail-open开关 | Runtime unverified | base/local binding/leaf tests、V-STATIC；外部operator配置未运行；退役项按联合修订/C18替换 |
| SA/SS/SG/edge | egon.cola.platform.tianquan.shoubing.revocation.timeout | Duration=500ms | 超时503 | Runtime unverified | base/local binding/leaf tests、V-STATIC；外部operator配置未运行；退役项按联合修订/C18替换 |
| SA/SS/SG/edge | egon.cola.platform.tianquan.shoubing.revocation.epoch | String；必填 | 运维恢复epoch，无旧ready快照回退 | Runtime unverified | base/local binding/leaf tests、V-STATIC；外部operator配置未运行；退役项按联合修订/C18替换 |
| SA/SS/SG/edge | egon.cola.platform.tianquan.shoubing.audit.stream | String=egon:oauth:audit:v1 | 实际MQ生产，不是Pub/Sub | Runtime unverified | base/local binding/leaf tests、V-STATIC；外部operator配置未运行；退役项按联合修订/C18替换 |
| SA/SS/SG/edge | egon.cola.platform.tianquan.shoubing.audit.spool-directory | Path；必填 | 持久卷、每实例独占，local可测试临时卷但明确测试 | Runtime unverified | base/local binding/leaf tests、V-STATIC；外部operator配置未运行；退役项按联合修订/C18替换 |
| SA/SS/SG/edge | egon.cola.platform.tianquan.shoubing.audit.spool-max-bytes | long=1073741824 | 满则fail-closed，不删除未投递 | Runtime unverified | base/local binding/leaf tests、V-STATIC；外部operator配置未运行；退役项按联合修订/C18替换 |
| SA/SS/SG/edge | egon.cola.platform.tianquan.shoubing.audit.hmac-key-file | Path；必填 | 不共用signing私钥 | Runtime unverified | base/local binding/leaf tests、V-STATIC；外部operator配置未运行；退役项按联合修订/C18替换 |
| SA | spring.session.redis.namespace | String=egon:sas:session:v2 | 与BFF分开 | Runtime unverified | base/local binding/leaf tests、V-STATIC；外部operator配置未运行；退役项按联合修订/C18替换 |
| edge | spring.session.redis.namespace | String=egon:bff:session:v2 | 不等于默认TokenRelay内存store | Runtime unverified | base/local binding/leaf tests、V-STATIC；外部operator配置未运行；退役项按联合修订/C18替换 |
| SA/edge | spring.session.timeout | Duration=30m | 另有8h绝对session期限，由会话属性createdAt校验 | Runtime unverified | base/local binding/leaf tests、V-STATIC；外部operator配置未运行；退役项按联合修订/C18替换 |
| edge | egon.yuheng.edge.session.absolute-timeout | Duration=8h | 超过需重新登录 | Runtime unverified | base/local binding/leaf tests、V-STATIC；外部operator配置未运行；退役项按联合修订/C18替换 |
| edge | egon.yuheng.edge.allowed-return-paths | list=[/] | 仅站内路径，匹配前缀须路径段边界 | Runtime unverified | base/local binding/leaf tests、V-STATIC；外部operator配置未运行；退役项按联合修订/C18替换 |
| edge | egon.yuheng.edge.upstreams.authorization-server | URI；必填 | TLS静态启动路由，保留issuer path | Runtime unverified | base/local binding/leaf tests、V-STATIC；外部operator配置未运行；退役项按联合修订/C18替换 |
| edge | egon.yuheng.edge.upstreams.biz-gateway | URI；必填 | 原玉衡biz，TLS | Runtime unverified | base/local binding/leaf tests、V-STATIC；外部operator配置未运行；退役项按联合修订/C18替换 |
| edge | egon.yuheng.edge.upstreams.mcp-gateway | URI；必填 | 原玉衡MCP，TLS、stream不缓冲 | Runtime unverified | base/local binding/leaf tests、V-STATIC；外部operator配置未运行；退役项按联合修订/C18替换 |
| edge/SA | server.ssl.enabled | boolean=true | local测试证书；生产不允许false | Runtime unverified | base/local binding/leaf tests、V-STATIC；外部operator配置未运行；退役项按联合修订/C18替换 |
| edge/SA | server.ssl.key-store | secret Path/reference | 各环境均有键，证书不同 | Runtime unverified | base/local binding/leaf tests、V-STATIC；外部operator配置未运行；退役项按联合修订/C18替换 |
| edge/SA | server.ssl.key-store-password | secret env | 不打印绑定值 | Runtime unverified | base/local binding/leaf tests、V-STATIC；外部operator配置未运行；退役项按联合修订/C18替换 |
| SA | spring.flyway.enabled | boolean=false | 旧历史只读，新schema受管DDL | Runtime unverified | base/local binding/leaf tests、V-STATIC；外部operator配置未运行；退役项按联合修订/C18替换 |
| SA | egon.cola.component.mybatis-plus.tenant-id.mdc-key | String=tenantId | 与verified bridge一致 | Runtime unverified | base/local binding/leaf tests、V-STATIC；外部operator配置未运行；退役项按联合修订/C18替换 |
| SA | egon.cola.component.mybatis-plus.tenant-id.ignored-tables | set=[egon_cola_outbox_message] | 新OAuth表禁止忽略 | Runtime unverified | base/local binding/leaf tests、V-STATIC；外部operator配置未运行；退役项按联合修订/C18替换 |
| SA | egon.cola.component.mybatis-plus.sharding.config-style | enum=NATIVE | mode SHARDING，1×1统一；physical group与public SINGLE同PRIMARY | Runtime unverified | base/local binding/leaf tests、V-STATIC；外部operator配置未运行；退役项按联合修订/C18替换 |
| SA | egon.cola.component.mybatis-plus.sharding.mode | enum=SHARDING | PRIMARY only，本期不读写分离 | Runtime unverified | base/local binding/leaf tests、V-STATIC；外部operator配置未运行；退役项按联合修订/C18替换 |
| SA | egon.cola.component.mybatis-plus.ddl.enabled | boolean=true | 仅新schema显式物理target+manifest；组件实际属性形状按POM/source绑定测试 | Runtime unverified | base/local binding/leaf tests、V-STATIC；外部operator配置未运行；退役项按联合修订/C18替换 |
| SA | egon.cola.component.transactional-outbox.storage.data-source-bean-name | String=dataSource | 与MP/JPA共享同bean | Runtime unverified | base/local binding/leaf tests、V-STATIC；外部operator配置未运行；退役项按联合修订/C18替换 |
| SA | egon.cola.component.transactional-outbox.storage.transaction-manager-bean-name | String=transactionManager | 同连接回滚测试 | Runtime unverified | base/local binding/leaf tests、V-STATIC；外部operator配置未运行；退役项按联合修订/C18替换 |
| SA/edge | springdoc.api-docs.enabled | boolean=false | 测试可true，生产受限 | Runtime unverified | base/local binding/leaf tests、V-STATIC；外部operator配置未运行；退役项按联合修订/C18替换 |
| SA/edge | springdoc.api-docs.version | enum=OPENAPI_3_1 | 不引入Swagger2 | Runtime unverified | base/local binding/leaf tests、V-STATIC；外部operator配置未运行；退役项按联合修订/C18替换 |


## 8. 每项测试义务

列出TEST-001–011及联合修订的所有实际TEST-101–172行。TEST-116未在有效Spec表定义；不杜撰测试编号或称其通过。实现测试类与原设计名不完全相同，依真实类/场景对应，测试名称存在不是通过证据。部分test仅合成数据、直接Service或fake事务，因此未覆盖所有原生协议/PG/Redis回合。

| ID | 目标 | 要求场景 | Spec指定工具/类 | 已执行证据 | 状态 | 限制 |
| --- | --- | --- | --- | --- | --- | --- |
| TEST-001 | TenantIssuerService/SAS SPI | A/B 同 client_id、同 kid，恶意 issuer/Host/jku | SA `TenantIssuerIsolationTest` | V-ADMIN | Satisfied | 已执行受控单元/合同部分；不外推运行证明 |
| TEST-002 | authorize/token/discovery/userinfo | S256/confidential/public、错 URI/verifier/nonce、code 重放 | SA `OAuthOidcProtocolContractTest` | V-ADMIN | Satisfied | Step19真实原生链/分组/OAS/构造器与原有Outbox合同通过；PG/Redis另列runtime |
| TEST-003 | WebClient/IdpServiceOAuth2Client | tenant/aud/scope/secretVersion 改变、并发100次、401/403/timeout/redirect | SS `ServiceTokenWebClientTest` | V-STARTER / V-GATEWAY / V-RPC | Satisfied | 已执行受控单元/合同部分；不外推运行证明 |
| TEST-004 | JWT resolver/cache | JWKS cache 命中、unknown kid、错 aud/alg/typ、clock skew | SS `MultiIssuerResourceServerTest` | V-STARTER / V-GATEWAY / V-RPC | Satisfied | 已执行受控单元/合同部分；不外推运行证明 |
| TEST-005 | Idp→Rbac principal bridge | 原 allow/deny/data/field/Fence 样本 | J starter `StarterFailClosedSecurityMatrixTest` 扩展 | V-ADMIN | Satisfied | 已执行受控单元/合同部分；不外推运行证明 |
| TEST-006 | tenant bridge/MP | finally 异常、复用线程、Reactor cancel、两租户同业务码、自定义 SQL | SA/SS `TenantSqlIsolationIT` | V-ADMIN | Runtime unverified | 用户控制的PG/Redis/多实例义务未运行 |
| TEST-007 | revocation | 撤销成功前后并发、Redis timeout/重启/主从切换、DB commit unknown | SA `TokenRevocationIT` | V-ADMIN | Runtime unverified | 用户控制的PG/Redis/多实例义务未运行 |
| TEST-008 | key rotation | publish→activate→old expiry、多实例落后、key compromise | SA `TenantSigningKeyRotationIT` | V-ADMIN | Runtime unverified | 用户控制的PG/Redis/多实例义务未运行 |
| TEST-009 | MP DDL/import | legacy subject、超 Long tenant、重复 deleted_at、部分目标成功、恢复 | SA `OAuthPersistenceMigrationIT` | V-ADMIN | Runtime unverified | 用户控制的PG/Redis/多实例义务未运行 |
| TEST-010 | shared auth/SCG/Yuheng/DDC | fresh session、切 tenant、CSRF、stepUp、401/403/503、platform registration | W 与各 Admin tests；现有脚本 | V-ADMIN | Runtime unverified | native/Edge/五package受控合同通过；真实shared auth/AS/Redis/browser集成未运行 |
| TEST-011 | Java/OAS/config/Event | DTO 字段、Qualifier、profile keys、Outbox rollback/delivery duplicate | SA `OAuthOpenApiContractTest`/`OAuthArchitectureTest` | V-ADMIN | Satisfied | Step19真实原生链/分组/OAS/构造器与原有Outbox合同通过；PG/Redis另列runtime |
| TEST-101 | identity/tenant MP迁移 | 双租户、同username、旧锁定/版本、部分Update/Delete、P伪造 | IdentityMpPersistenceContractTest / 现有服务测试 | V-ADMIN | Runtime unverified | 用户控制的PG/Redis/多实例义务未运行 |
| TEST-102 | 同事务/DDL/Outbox | 故意JpaTM、错DS、错factory、enqueue失败、Redis失败、重复event | IdentityAuthenticationTransactionContractTest / OAuthManagedSchemaContractTest | V-ADMIN | Runtime unverified | 用户控制的PG/Redis/多实例义务未运行 |
| TEST-103 | 导入/旧历史 | version=7、历史timestamp、非numeric subject、孤儿、payload超限、重复批次、unknown commit | IdentityMigrationContractTest / OAuthMigrationServiceTest | V-ADMIN | Runtime unverified | 用户控制的PG/Redis/多实例义务未运行 |
| TEST-104 | Tianshu边界 | TENANT A/B、错/缺AI header、PLATFORM注册token、缺HMAC/错namespace | DdcTenantIsolationTest / IdpJwtDdcRegistrationCredentialVerifierTest / DdcRpcClientInterceptorFactoryTest | V-ADMIN | Satisfied | 已执行受控单元/合同部分；不外推运行证明 |
| TEST-105 | SCG/Yuheng | SSE分块、取消、慢消费者、错误aud、management端口 | EdgeLoginContractTest / 原数据面suite | V-ADMIN | Satisfied | 已执行受控单元/合同部分；不外推运行证明 |
| TEST-106 | API-045 | []、64、65、403、404、503、切realm | ClientResourceGrantControllerTest / ClientResourceGrantPage.test.tsx | V-ADMIN | Satisfied | 已执行受控单元/合同部分；不外推运行证明 |
| TEST-107 | MP DDL manifest family | Existing families; tianquan-oauth; arbitrary unknown | EgonColaDdlManifestFamilyTest | V-ADMIN | Satisfied | 已执行受控单元/合同部分；不外推运行证明 |
| TEST-108 | NATIVE physical source map | 15 target + 13 exact public legacy + 2 component nodes | OAuthManagedSchemaContractTest.nativeRoutesCoverBusinessTechnicalAndExactLegacyTables | V-ADMIN | Satisfied | 已执行受控单元/合同部分；不外推运行证明 |
| TEST-109 | Identity tenant settings jsonb | valid JSON object, scalar/null/invalid text, UTF-8 size | IdentityMpPersistenceContractTest | V-ADMIN | Satisfied | 已执行受控单元/合同部分；不外推运行证明 |
| TEST-110 | Named MP Clock alias | idpClock为Primary；常规MP clock按Clock type退让；repository/Outbox使用具名Clock | IdentityMpPersistenceContractTest | V-ADMIN | Satisfied | 已执行受控单元/合同部分；不外推运行证明 |
| TEST-111 | Generated identity/tenant converters | PO/domain fixture values with sensitive password hash | IdentityEntityMappingTest | V-ADMIN | Satisfied | 已执行受控单元/合同部分；不外推运行证明 |
| TEST-112 | oauth_client JSONB string arrays | empty/non-empty string list, wrong root or non-string element | OAuthRealmPersistenceContractTest | V-ADMIN | Satisfied | 已执行受控单元/合同部分；不外推运行证明 |
| TEST-113 | OAuth persistence converter generic direction | PO/BO fixtures for realm, client, resource, grant, authorization, token, consent, signing key, security state, revocation, and audit | All four focused suites | V-ADMIN | Satisfied | 已执行受控单元/合同部分；不外推运行证明 |
| TEST-114 | Trusted realm persistence MDC scope | TENANT partition=1001, PLATFORM partition=P, prior MDC actor, absent actor, mapper exception | OAuthRealmPersistenceContractTest | V-ADMIN | Satisfied | 已执行受控单元/合同部分；不外推运行证明 |
| TEST-115 | Realm/client version monotonicity | matching expected row version, lower trustVersion or credentialVersion | OAuthRealmPersistenceContractTest | V-ADMIN | Satisfied | 已执行受控单元/合同部分；不外推运行证明 |
| TEST-117 | OAuthPayloadCipherComponent AES-GCM envelope | temporary 32-byte KEK; missing/invalid key file; wrong keyId/issuer/realm/purpose/context; nonce/tag tamper; malformed envelope and schemaVersion != 1 | OAuthAuthorizationPersistenceContractTest | V-ADMIN | Satisfied | 已执行受控单元/合同部分；不外推运行证明 |
| TEST-118 | OAuth management realm resolver and explicit service propagation | tenant default; matching and mismatching tenant query; platform management tenant with strong/fresh and weak/stale auth context; unknown issuer | OAuthManagementRealmResolverTest and existing client/grant/key/resource controller tests | V-ADMIN | Satisfied | 已执行受控单元/合同部分；不外推运行证明 |
| TEST-119 | per-realm signing key management | two issuer keyrings, wrong AAD/keyId, invalid public/private pair, premature activation, stale key/realm version, active-key retire | SigningKeyServiceImplTest | V-ADMIN | Satisfied | 已执行受控单元/合同部分；不外推运行证明 |
| TEST-120 | Admin view timestamp fidelity | synthetic PO created/updated timestamps and changed BO payload | OAuthRealmPersistenceContractTest / OAuthResourcePersistenceContractTest / OAuthSigningKeyPersistenceContractTest | V-ADMIN | Satisfied | 已执行受控单元/合同部分；不外推运行证明 |
| TEST-121 | OAuthResource bounded directory query | tenant id, deleted row, ordered ids, result counts 0, 10000 and 10001 | OAuthResourcePersistenceContractTest / ResourceServerServiceImplTest | V-ADMIN | Satisfied | 已执行受控单元/合同部分；不外推运行证明 |
| TEST-122 | API-045 bounded grant query | result counts 0, 1, 64, 65; two realms with same clientId | OAuthResourcePersistenceContractTest / ResourceServerServiceImplTest / ClientResourceGrantControllerTest | V-ADMIN | Satisfied | 已执行受控单元/合同部分；不外推运行证明 |
| TEST-123 | committed Resource/Grant projection delivery | create/status and CC Grant upsert/disable versions; same client IDs across realms; duplicate/older events; malformed destination/schema/realm; Redis failure | ResourceServerServiceImplTest / TransactionalOutboxResourceServerEventAdapterTest / ResourceServerProjectionDeliveryHandlerTest | V-ADMIN | Satisfied | 已执行受控单元/合同部分；不外推运行证明 |
| TEST-124 | profile-gated exact redirect validation | valid HTTPS; exact loopback HTTP under local; same HTTP under nonlocal; malformed host/port/path; userinfo/query/fragment/wildcard; duplicate URI | OAuthClientServiceImplTest | V-ADMIN | Satisfied | 已执行受控单元/合同部分；不外推运行证明 |
| TEST-125 | SAS token revoke, client/subject security-generation initialization/change, projection/recovery, audit and key rotation | valid/invalid client, ACCESS/REFRESH revoke, new realm/client/subject state, missing state with/without prior authorization, status/scope/secret change, duplicate/older revision, Outbox enqueue failure, Redis unavailable/recovery, expired retention and late key instance | TokenRevocationServiceTest / IdentityAuthenticationTransactionContractTest / OAuthClientServiceImplTest / OAuthRetentionServiceTest / OAuthOidcProtocolContractTest | V-ADMIN | Runtime unverified | 用户控制的PG/Redis/多实例义务未运行 |
| TEST-126 | Retention soft-delete, restart/resume and delayed audit delivery | audit cutoff, revocation expiry+30s, PENDING vs APPLIED, stale version, permanent kid tombstone, restart after a committed batch, late audit event at/past cutoff | OAuthRetentionServiceTest / OAuthSigningKeyPersistenceContractTest / OAuthAuditConsumerTest | V-ADMIN | Satisfied | 已执行受控单元/合同部分；不外推运行证明 |
| TEST-127 | secret-free token-usage audit event, durable spool and Stream verification | missing/non-HTTPS issuer, eventId over 64 characters, missing/bad HMAC, corrupt CRC/HMAC frame, owner-only file permissions, spool quota, Redis outage and replay | TokenUsageAuditComponentTest / OAuthAuditConsumerTest | V-ADMIN | Satisfied | 已执行受控单元/合同部分；不外推运行证明 |
| TEST-128 | Starter Lombok API and Qualifier constructor propagation | starter module dependency visibility and module-local `lombok.config` | TokenUsageAuditComponentTest | V-ADMIN | Satisfied | 已执行受控单元/合同部分；不外推运行证明 |
| TEST-129 | resource-server verification outcome to HTTP mapping | valid multiissuer USER/SERVICE, invalid issuer/audience/tenant, expired/ID Token, Redis UNAVAILABLE, pooled-thread reuse | MultiIssuerResourceServerTest | V-STARTER / V-GATEWAY / V-RPC | Satisfied | 已执行受控单元/合同部分；不外推运行证明 |
| TEST-130 | Starter MapStruct API and generated verified-principal conversion | valid USER and SERVICE claim fixtures; forbidden/unknown claim; converter source/target direction and generated Spring name | MultiIssuerResourceServerTest | V-STARTER / V-GATEWAY / V-RPC | Satisfied | 已执行受控单元/合同部分；不外推运行证明 |
| TEST-131 | verified tenant/user request scope and module boundary | USER tenant A, PLATFORM service, preexisting MDC values, downstream success and exception, pooled-thread reuse, nested Jianshen scope | MultiIssuerResourceServerTest / TenantContextFilterTest | V-STARTER / V-GATEWAY / V-RPC | Satisfied | 已执行受控单元/合同部分；不外推运行证明 |
| TEST-132 | validated OAuth scope projection through the final Jianshen authentication | USER and SERVICE scope claims, blank/duplicate/oversized scope, RBAC3 permission independence, attempted non-scope authorities | MultiIssuerResourceServerTest / IdpBearerAuthenticationFilterTest / Rbac3BearerAuthenticationFilterTest | V-STARTER / V-GATEWAY / V-RPC | Satisfied | 已执行受控单元/合同部分；不外推运行证明 |
| TEST-133 | resource-server revocation dependency outcome mapping | valid, invalid, expired and shared-state unavailable verifier outcomes | IdpRpcBearerServerInterceptorTest | V-STARTER / V-GATEWAY / V-RPC | Satisfied | 已执行受控单元/合同部分；不外推运行证明 |
| TEST-134 | machine-token identity claim propagation | source client appId, client PK and credentialVersion, target resource and scopes | OAuthOidcProtocolContractTest | V-STARTER / V-GATEWAY / V-RPC | Satisfied | 已执行受控单元/合同部分；不外推运行证明 |
| TEST-135 | issuer registration refresh and manager-cache fencing | add issuer, higher trustVersion/JWKS replacement, deactivate/remove issuer and attempted rollback | MultiIssuerResourceServerTest | V-STARTER / V-GATEWAY / V-RPC | Satisfied | 已执行受控单元/合同部分；不外推运行证明 |
| TEST-136 | loopback local URL carveout | canonical `127.0.0.1`/localhost HTTP and redis, non-loopback HTTP/redis, mixed-origin JWKS, HTTPS/rediss production endpoints | MultiIssuerResourceServerTest | V-STARTER / V-GATEWAY / V-RPC | Satisfied | 已执行受控单元/合同部分；不外推运行证明 |
| TEST-137 | legacy cutover property pairing in both verifier modes | legacy single issuer with neither field, issuer only, allowlist key only, and valid complete pair; multi-issuer with a partial pair | MultiIssuerResourceServerTest | V-STARTER / V-GATEWAY / V-RPC | Satisfied | 已执行受控单元/合同部分；不外推运行证明 |
| TEST-138 | full machine-token identity key and bounded renewals | two trusted issuer registrations, different client ids and credential versions, tenant/PLATFORM, audience, exact/overbroad scope responses, remote plaintext audience, 100 concurrent same-key calls, near-expiry/expired tokens, 10,001 distinct keys, incomplete identity configuration and legacy constructor without trust inputs | IdpServiceOAuth2ClientTest | V-STARTER / V-GATEWAY / V-RPC | Satisfied | 已执行受控单元/合同部分；不外推运行证明 |
| TEST-139 | scoped Bearer injection and safe authentication retry | missing/invalid typed request, pre-existing Authorization, matching/mismatching audience origin, GET 401 once then success/401, POST 401, 403 and a loopback cross-origin 302 | IdpServiceOAuth2ClientTest / IdpStarterAutoConfigurationTest | V-STARTER / V-GATEWAY / V-RPC | Satisfied | 已执行受控单元/合同部分；不外推运行证明 |
| TEST-140 | issuer-bound Edge AuthorizedClient and encrypted Spring Session state | same session across issuers/tenants/clients/subjects; OIDC identity and OAuth state; valid/wrong/missing 32-byte key; noncanonical envelope; nonce/tag corruption; owner-only key permissions; map idle and absolute expiry | EdgeLoginContractTest | V-EDGE | Satisfied | 已执行受控单元/合同部分；不外推运行证明 |
| TEST-141 | tenant/platform registrations; invalid origin/key id/unbounded timeout config; session cookie config; anonymous API/session/logout; missing logout CSRF; operation/security/response annotations; opt-in grouped-doc config; Lettuce/Redisson mismatch or remote plaintext | browser chain rejects anonymous session/logout, requires logout CSRF, and API chain never redirects; session cookie rotates after login; `edge-bff` docs stay disabled by default and bind exact paths when enabled; routes have one operation id/status/security, logout 204 has no content; invalid config fails before serving | REQ-036/038 | V-EDGE | Satisfied | 已执行受控单元/合同部分；不外推运行证明 |
| TEST-142 | authenticated session; missing CSRF; one or more registrations; atomic batch capacity failure; remote 2xx/timeout/4xx; duplicate delivery; restart/stale claim; wrong issuer/client/endpoint and ciphertext tamper | anonymous logout returns 401/403 before revocation; an authenticated logout requires CSRF; all session refresh intents persist atomically before invalidation; remote success is 204 and only then ACK/XDEL; persisted remote failure is 503/PENDING; failed batch enqueue still expires the local session/cookie and is 503/UNKNOWN; same-origin client_secret_basic only; token never appears in Stream fields/log | REQ-037 | V-EDGE | Satisfied | 已执行受控单元/合同部分；不外推运行证明 |
| TEST-143 | pinned Spring Cloud Gateway static route binding | application/base and local route trees, authorization-server route, biz and MCP route filter lists | EdgeLoginContractTest | V-EDGE | Satisfied | 已执行受控单元/合同部分；不外推运行证明 |
| TEST-144 | Edge Lombok constructor and dependency boundary | required-args services/configuration; every constructor parameter's copied @Qualifier; stable component/controller/config bean names; provided managed Lombok scope | EdgeLoginContractTest | V-EDGE | Satisfied | 已执行受控单元/合同部分；不外推运行证明 |
| TEST-145 | distributed BFF refresh-token single-flight | two manager calls for same session/client; different tenant/client/session; refresh success/error/timeout; lock wait timeout; encrypted repository save before release | EdgeLoginContractTest | V-EDGE | Satisfied | 已执行受控单元/合同部分；不外推运行证明 |
| TEST-146 | valid tenant USER token; JWT issuer/token/subject differs from held client or registration; wrong tenant/audience; expired token; revoked subject/JTI; shared Redis unavailable; refresh token present | valid current identity returns 200 without manager/refresh invocation; any token, issuer, subject, tenant, expired, wrong-audience or revoked mismatch returns 401; shared-state outage returns 503; all responses remain token-free/no-store | REQ-042 | V-EDGE | Satisfied | 已执行受控单元/合同部分；不外推运行证明 |
| TEST-147 | login navigation, BFF session, CSRF logout, pending revocation and safe errors | ordinary login, `stepUp=true` redirect, 204/503 logout, 401/403/503 BFF reads, malformed CSRF response, no username/password/token fields or persistence | gatewayAuthClient.test.ts and five affected Vite configs | V-FRONTEND / Step16 package gates | Satisfied | 已执行受控单元/合同部分；不外推运行证明 |
| TEST-148 | configured registration; ordinary login; exact and invalid step-up flag; issuer/client binding; PKCE | ordinary authorization retains existing PKCE and omits step-up parameters; exact `stepUp=true` includes `prompt=login`/`max_age=0`; malformed flag is rejected; authorization URI remains the registered issuer | REQ-043 | V-EDGE | Satisfied | 已执行受控单元/合同部分；不外推运行证明 |
| TEST-149 | login screens; step-up-required API; callback remount; mismatched/expired expectedAuthVersion; network failure | Portal/four-Admin login views render no username/password input; high-risk activation stores only the role intent, begins OIDC step-up, retries once after a valid return and clears intent; stale intent is discarded; no password/token is retained | REQ-043 | V-FRONTEND / Step16 package gates | Satisfied | 已执行受控单元/合同部分；不外推运行证明 |
| TEST-150 | generated frontend origin; Vite auth/callback route ownership; Edge OIDC entry; BFF CSRF/session; no legacy password probes | `YUHENG_EDGE_BASE_URL` is the explicit public Edge origin persisted as `VITE_YUHENG_ORIGIN`; no `VITE_DEFAULT_TENANT_ID` is generated; `/oauth2`, `/bff`, and `/login/oauth2` auth proxies use Edge; `/api` remains separately routed to Yuheng Engine; live probe does not post password or persist USER AT/RT/cookies | REQ-043,REQ-022 | V-STATIC / V-C18 | Satisfied | 已执行受控单元/合同部分；不外推运行证明 |
| TEST-151 | exact source allowlist; frozen source versions; batches 500/501; dry-run/apply digest; stale/missing checkpoint; commit-before-checkpoint crash; unknown commit; duplicate/malformed/orphan rows; password hash and history fields | Only parameterized, fixed legacy-source SQL is accepted; valid apply preserves external identity, source hash/version/timestamps; changing a same-version password hash/settings/audit payload/resource field changes the approved SHA; conflict stops the realm; replay reconciles the canonical row hash; output/toString/logs never disclose credential/client secrets | REQ-044 | V-MIGRATION / V-ADMIN | Satisfied | 已执行受控单元/合同部分；不外推运行证明 |
| TEST-152 | default profile, explicit migration profile, false/missing enable flag, web mode, RPC/registration enabled, missing/mismatched freeze marker, incomplete command | Normal startup has no migration runner; migration profile rejects unsafe web/RPC/registration configuration; only an explicit safe command invokes one dry-run or reviewed apply and exits without starting service endpoints | REQ-044 | V-MIGRATION / V-ADMIN | Satisfied | 已执行受控单元/合同部分；不外推运行证明 |
| TEST-153 | duplicate/missing map row, sourceSnapshotId mismatch, cross-tenant grant/resource, source owner disagreement, PUBLIC/CONFIDENTIAL type, mixed purposes, empty USER scopes, malformed scope, missing target Resource, disabled facts, prior/unknown secret file and fresh secret delivery | Each imported client resolves to exactly one target realm and no more than its mapped/verified scopes; old secret hash and private-key ciphertext are never selected or copied; PUBLIC has no secret; CONFIDENTIAL gets a fresh BCrypt hash and owner-only file; unknown or conflicting input stops before checkpoint advancement; report includes file references only | REQ-044 | V-C10-ADMIN / V-MIGRATION | Satisfied | 已审核USER范围四组合/恢复冲突/重放和既有SERVICE导入均通过；非真实PG导入证明。 |
| TEST-154 | missing report path, pre-existing report, non-owner-only parent, serialization, full digest, secret field exclusion | Runner refuses unsafe/reused report paths; successful dry-run writes one owner-only JSON report containing `reportSha256` and conflicts/counts, while logs contain only the digest/path summary | REQ-044 | V-MIGRATION / V-ADMIN | Satisfied | 已执行受控单元/合同部分；不外推运行证明 |
| TEST-155 | USER client with PKCE true/false; SERVICE client with refresh TTL zero/nonzero | A USER client without legacy PKCE and a SERVICE client with a refresh TTL are explicit conflicts before apply; accepted USER clients remain PKCE-required and SERVICE clients have no refresh-token grant | REQ-044 | V-MIGRATION / V-ADMIN | Satisfied | 已执行受控单元/合同部分；不外推运行证明 |
| TEST-156 | same legacy row IDs/versions with changed password hash, tenant settings, audit payload, source IP/user agent, or Resource mapping field | Each changed mapped field changes the dry-run report digest using a one-way fingerprint; raw source password hashes and payload data never appear in the report or command output | REQ-044 | V-MIGRATION / V-ADMIN | Satisfied | 已执行受控单元/合同部分；不外推运行证明 |
| TEST-157 | platform SERVICE client with a valid management Resource and CLIENT_CREDENTIALS grant | SERVICE client creation receives no client-level resourceUris; the Resource is imported under platform and the exact Grant is persisted/projected in the same target transaction | REQ-044 | V-C10-ADMIN / V-MIGRATION | Satisfied | 已审核USER范围四组合/恢复冲突/重放和既有SERVICE导入均通过；非真实PG导入证明。 |
| TEST-158 | Subject revocation Adapter and single invocation | Core password change, malformed/null subject/reason/time, old token lifecycle entry, user disable/reset/revoke-all and projection failure | SubjectRevocationRefreshTokenStoreAdapterTest / IdentityUserServiceImplTest / SpringConstructorResolutionTest | V-ADMIN | Satisfied | 已执行受控单元/合同部分；不外推运行证明 |
| TEST-159 | Exact legacy root retirement | API-035–044 methods/paths; malformed Basic/body/cookie; authenticated and anonymous requests; context path; wrong method and tenant/platform SAS paths | LegacyOAuthRetirementTest / InternalRefreshTokenSecurityIT / IdentityProfileControllerIT | V-ADMIN | Satisfied | 已执行受控单元/合同部分；不外推运行证明 |
| TEST-160 | Client auth security and JPA retirement | Valid Basic client, malformed/duplicate headers, wrong secret, disallowed post/body auth, no matching client; immutable SQL; Java/pom references | OAuthTokenClientCredentialsTest / OAuthConfigTest / IdpPersistenceEntityContractTest / IdentityAuthenticationTransactionContractTest | V-ADMIN | Satisfied | 已执行受控单元/合同部分；不外推运行证明 |
| TEST-161 | Complete local consumer migration | Upper/lower startup, service issuer/JWK dimensions, missing protected USER materials and current MP prerequisite, legacy import/SQL/cookie/password paths | scripts/unified-xingyuan/test-direct-run-contract.sh | V-STATIC / V-C18 | Satisfied | 已执行受控单元/合同部分；不外推运行证明 |
| TEST-162 | Formal issuer matcher and raw BCrypt compatibility | Raw persisted BCrypt12 secret, correctly signed SERVICE token, root token404 regression, malformed/duplicate/mixed auth | OAuthTokenClientCredentialsTest | V-ADMIN | Satisfied | 已执行受控单元/合同部分；不外推运行证明 |
| TEST-163 | Explicit local AS registrations and protected local registry | Missing/unknown issuer, wrong token endpoint/method/grant, absent secret/version, wrong owner/mode, incomplete/duplicate registry, missing protected USER input; default/local activation | OAuthConfigTest / test-direct-run-contract.sh | V-STATIC / V-C18 | Satisfied | 已执行受控单元/合同部分；不外推运行证明 |
| TEST-164 | Explicit tenant registration selection | Two verified tenant IDs with distinct registrations/credential generations, missing map entry, malformed/overflow tenant key, absent version, empty-map mode | HttpTenantServiceTokenSupplierTest / IdpServiceOAuth2ClientTest / IdpStarterAutoConfigurationTest | V-C13 | Satisfied | 已执行受控单元/合同部分；不外推运行证明 |
| TEST-165 | Direct provider login and published catalog separation | Named Controller and exact tenant/login GET/Operation | IdpPublishedOpenApiContractTest / TenantIssuerIsolationTest | V-ADMIN | Satisfied | 已执行受控单元/合同部分；不外推运行证明 |
| TEST-166 | Closed checkpoint state and 500/501 restart boundaries | Unknown/mismatched phase, completion/count/cursor state, unknown JSON field, stale digest; commit-before-checkpoint exception and unknown commit | OAuthMigrationServiceTest | V-MIGRATION / V-ADMIN | Satisfied | 已执行受控单元/合同部分；不外推运行证明 |
| TEST-167 | Accepted canonical metadata/JWKS routes | Canonical per-tenant/platform JWKS and exact OAuth metadata suffix routes; legacy root; unsupported auth advertisement | OAuthPublicEndpointSettingsTest / TenantIssuerIsolationTest | V-ADMIN | Satisfied | 已执行受控单元/合同部分；不外推运行证明 |
| TEST-168 | Actual new verifier and old RT retirement | Two tenant USER tokens carrying client_id; PLATFORM/TENANT SERVICE; wrong audience/issuer, revoked/expired/unavailable; cookie-only and mixed legacy-cookie/Bearer; managed factory without old refresh URI | IdpGatewayJwtVerifierTest / IdpGatewayAdapterAutoConfigurationTest / IdpGatewaySecurityProviderTest / MultiIssuerResourceServerTest | V-GATEWAY | Satisfied | 已执行受控单元/合同部分；不外推运行证明 |
| TEST-169 | Browser credential boundary | Authenticated BFF and explicit Bearer API/MCP forwards; AS route; mixed and anonymous errors | EdgeLoginContractTest | V-EDGE | Satisfied | 已执行受控单元/合同部分；不外推运行证明 |
| TEST-170 | Retired root defaults removed | Exact twelve YAML profiles plus generated local trusted JWKS | test-direct-run-contract.sh | V-STATIC / V-C18 | Satisfied | 已执行受控单元/合同部分；不外推运行证明 |
| TEST-171 | Dead protocol configuration removal | Base/local parsed leaves and script emissions | OAuthPublicEndpointSettingsTest / test-direct-run-contract.sh | V-STATIC / V-C18 | Satisfied | 已执行受控单元/合同部分；不外推运行证明 |
| TEST-172 | Protocol wire/documentation closure | USER/SERVICE scope normalization, SAS metadata/JWKS/authorize/token/revoke/userinfo and root410, native Edge login/callback | OAuthProtocolOpenApiContractTest / OAuthTokenClientCredentialsTest / TenantIssuerIsolationTest / EdgeLoginContractTest | V-C8c / V-EDGE | Satisfied | V-C9b-ADMIN/EDGE原生协议/OAS/CSRF/错误边界已通过；运行另列 |


| TEST-173 | 真实Filter全回合、精确scope和scope收窄 | 有效修订§14 TEST-173准确输入/输出 | OAuthConsentRoundTripTest / OAuthAuthorizationPersistenceContractTest / OAuthOidcProtocolContractTest / OAuthManagedSchemaContractTest | V-C9b-ADMIN / V-C9b-STATIC | Satisfied | 真实生产接线+受控端口/事务；不外推PG/Redis/browser |
| TEST-174 | v1/v2/99、错误AAD/tag/unknown及普通JSON隐藏 | 有效修订§14 TEST-174准确输入/输出 | OAuthConsentRoundTripTest / OAuthAuthorizationPersistenceContractTest / OAuthOidcProtocolContractTest / OAuthManagedSchemaContractTest | V-C9b-ADMIN / V-C9b-STATIC | Satisfied | 真实生产接线+受控端口/事务；不外推PG/Redis/browser |
| TEST-175 | CSRF、跨realm/client/subject/session、Host/cancel/replay | 有效修订§14 TEST-175准确输入/输出 | OAuthConsentRoundTripTest / OAuthAuthorizationPersistenceContractTest / OAuthOidcProtocolContractTest / OAuthManagedSchemaContractTest | V-C9b-ADMIN / V-C9b-STATIC | Satisfied | 真实生产接线+受控端口/事务；不外推PG/Redis/browser |
| TEST-176 | 唯一新版本varchar32/CHECK五类、95旧SQL/旧manifest | 有效修订§14 TEST-176准确输入/输出 | OAuthConsentRoundTripTest / OAuthAuthorizationPersistenceContractTest / OAuthOidcProtocolContractTest / OAuthManagedSchemaContractTest | V-C9b-ADMIN / V-C9b-STATIC | Satisfied | 真实生产接线+受控端口/事务；不外推PG/Redis/browser |
| TEST-177 | AT/ID动态校验组与真实签名、时间metadata重载 | 有效修订§14 TEST-177准确输入/输出 | OAuthConsentRoundTripTest / OAuthAuthorizationPersistenceContractTest / OAuthOidcProtocolContractTest / OAuthManagedSchemaContractTest | V-C9b-ADMIN / V-C9b-STATIC | Satisfied | 真实生产接线+受控端口/事务；不外推PG/Redis/browser |
| TEST-178 | Consent/code/Outbox/签名故障原子回滚及family deadline | 有效修订§14 TEST-178准确输入/输出 | OAuthConsentRoundTripTest / OAuthAuthorizationPersistenceContractTest / OAuthOidcProtocolContractTest / OAuthManagedSchemaContractTest | V-C9b-ADMIN / V-C9b-STATIC | Satisfied | 真实生产接线+受控端口/事务；不外推PG/Redis/browser |
| TEST-179 | Core/management bounded scope, immutable/empty/client/context/schema/version and update/batch/list | Exact approved USER business set persists; invalid inputs have no writes and SERVICE projections are absent | REQ-005/026/048 | V-C10-ADMIN | Satisfied | Core29/Admin364包含真实Core/DTO/Service/Controller及自动空关联断言 |
| TEST-180 | Explicit/legacy-empty approved scope × missing/fresh empty association, existing empty/content conflicts and cursor recovery | Only reviewed USER set persists, replay is stable and target conflicts reject; SERVICE unchanged | REQ-017/044/048 | V-C10-ADMIN | Satisfied | Migration14，其中USER四参数组合；真实PG另列runtime |
| TEST-181 | Actual managed grant→native exchange, clear/disable/stale versions and USER UI exact editor/readback/validation | Exact grant permits scoped exchange; denied exchange does not consume code; UI sends exact set | REQ-003/004/010/048 | V-C10-ADMIN / V-C10-UI / V-C10-TYPE | Satisfied | native25+Grant11及App关联/full UI50通过；无浏览器运行 |

## 9. Step19门禁与独立最终审计

### 9.1 Step19阻塞Manual Check（33路径，提交前逐项复核）

Step19只关闭已批准的原生协议纠正；每项证据由当前diff/声明文件/真实命令核对。全Spec新增B-04不在该纠正执行范围，未偷偷实施或判PASS。

| MC ID | Applicability | 状态 | Diff/path/符号证据 | 测试/静态证据 | 发现/动作 |
| --- | --- | --- | --- | --- | --- |
| MC-ARCH-001 | Applicable | PASS | 传统Admin Controller→Service→Repository→DAO保持；只新增同目录BO/page，Edge仍OAuth入口，Starter无Admin反向依赖 | V-C9b-ADMIN/CONSUMERS编译与File1–33路径清单 | 职责/包结构不变 |
| MC-REUSE-001 | Applicable | PASS | SAS request/consent/UserInfo原Provider、Spring session/CSRF、既有MP/cipher/Outbox与BaseConverter复用 | 真实SAS/JWK/Boot validation native24及storage16 | 无第二协议或状态框架 |
| MC-DEP-001 | Applicable | PASS | 33路径无pom/package/build依赖修改；Nimbus/Hibernate/Jackson皆当前已有受管库 | 差异清单与offline Maven exit0 | 未加依赖/升级版本 |
| MC-NAME-001 | Applicable | PASS | 新OAuthPendingConsentBO及既有SignedTokenClaimsBO/AttributesBO；Controller/Provider/filter是行为扩展点，Mixin/validation group为框架技术类型 | 编译与类型声明逐项检查 | 语义命名/生命周期明确 |
| MC-VALID-001 | Applicable | PASS | ConsentController查询注解→native单值/PKCE/resource校验→Service命令groups→Repository Valid BO→持久化cipher；SignedTokenClaims动态AT/ID组，SPI不非法强化参数 | native24、OIDC17、真实方法校验、constructor8与storage16 | Step19每个受影响交接验证；旧管理USER范围缺口B-04在Final矩阵单列 |
| MC-MODEL-001 | Applicable | PASS | Pending/Attributes/SignedClaims保留完整Data/NoArgs/AllArgs/Accessors/Builder；没有新实体record；state从普通JSON/toString排除 | 实际Lombok编译、原生密文重载与组校验 | 没有构造器/模型规范降级 |
| MC-CONVERT-001 | Applicable | PASS | OAuthAuthorizationConverter/SasAuthorizationConverter仍MapStruct +BaseConverter<S,T>；copyAttributes由生成Mapper实现；SAS框架投影维持原Adapter | storage16/native24与实际target/generated-sources/annotations | 保留正确转换方向，无BeanUtils/reflection映射替代 |
| MC-LOG-001 | Applicable | PASS | AuthenticationProvider/LoginFilter/ExactIssuerFilter/ConsentController/TokenIssueService带Slf4j；仅取消常量debug，不日志state/password/token | 业务diff逐项与fault/rejection测试 | 无敏感或高频新增日志；本机配置误输出另列操作偏差 |
| MC-BEAN-001 | Applicable | PASS | ConsentController命名oauthConsentController、三依赖Qualifier+NonNull；LoginFilter与业务Provider@RequiredArgsConstructor；lombok.config拷贝Qualifier | constructor8真实同类型消歧与Native Boot context | Spring注入无手写；策略/delegate由原工厂组装 |
| MC-UTIL-001 | Applicable | PASS | 新逻辑使用JDK URI/time/collections与既有Digests、SAS/Nimbus/Spring扩展；没有Utils/额外通用工具包 | 33路径import/dependency逐项检查、offline编译 | 允许的工具/组件复用 |
| MC-JSON-001 | Applicable | PASS | managed Jackson显式storageMixin只对AES-GCM codec开放隐藏属性；v1/v2/unknown闭合；@EnumValue/JsonValue；scope String↔内部Set | storage16/OIDC17/native24和OAS/published合同 | 普通JSON隐藏state，JWT不含_token_use，未知输入拒绝 |
| MC-TIME-001 | Applicable | PASS | Clock/Instant/Duration贯穿pending300s、reauth300s和immutable family；SAS iat/exp/nbf/auth_time重载Instant | 边界/expired/freshness/RT cap与原生UserInfo测试 | 只用java.time；family期限不会因刷新延长 |
| MC-CONFIG-001 | Applicable | PASS | 本Step未改YAML或属性键；既有base/local绑定/环境同构保留，Spring CSRF只对native browser POST要求 | Admin OAuthConfig/PublicEndpointSettings/ManagedSchema与Edge profile合同、static脚本 | 无单环境键漂移；SESSION由现有Spring Session Redis依赖拥有 |
| MC-PATTERN-001 | Applicable | PASS | GrantTransaction Strategy +事务Decorator包原SASProvider；issuer redirect Adapter只改内部consent；pending/consumed/revoked版本State沿既有CAS | native同意/取消/故障/重放/精确scope/reauth实际分支通过 | 真实模式参与者、装配和失败语义可验证；局部有界输入直接校验 |
| MC-SCOPE-001 | Applicable | PASS | 33声明路径相等；File30–32直接SPI兼容、File33旧manifest fixture已先声明；2旧dirty Spec SHA不变；95旧SQL原字节 | git diff/status/strict/SQL checksum；附录精确33路径 | 无无关提交；旧archetype忽略Markdown保留 |
| MC-TEST-001 | Applicable | PASS | native行为RED5/freshness/scope与旧schema fixture RED均已观察；354 Admin+24 Edge+58消费者以及static/strict全部exit0 | /tmp/egon-c9b-all-admin-green3.log、final-edge.log、consumers.log、static.log | 模块/受控测试足以验证Step19；不是完整reactor/runtime PASS |
| MC-BLOCKER-001 | Applicable | PASS | Step19其余16MC与10Literal逐项PASS；33scope/oneDDL/user approval/no index overlap确认 | final cached diff/check与成功单一提交需在最终交付核对 | Step19可交付；全Spec新缺口B-04/runtime在Final矩阵保持开放 |

| Literal Rule | Applicability | 状态 | Diff/path/符号证据 | 测试/静态证据 | 发现/动作 |
| --- | --- | --- | --- | --- | --- |
| Rule 1 | Applicable | PASS | Pending/Attributes/SignedClaims BO与行为类语义命名，引用Step19 File10–22/30–33 | 实际编译/类型inventory/native/OAS | None；仅Step19规范通过 |
| Rule 2 | Applicable | PASS | 查询约束、request boundary、Service/Repo Valid cascade与AT/ID groups；SPI保留显式guard；不以ValidationUtils包全部业务 | native24/storage16/OIDC17与真实HV000151纠正 | None；仅Step19规范通过 |
| Rule 3 | Applicable | PASS | 普通模型完整Lombok；不可变既有VO record不改；MapStruct/BaseConverter及生成copyAttributes | 生成Mapper/constructor8/密文重载 | None；仅Step19规范通过 |
| Rule 4 | Applicable | PASS | 相关业务Slf4j；具名Spring Bean、final qualified fields、RequiredArgsConstructor；工厂delegate不伪造Bean | constructor8与实际Boot/nativecontext；日志diff | None；仅Step19规范通过 |
| Rule 5 | Applicable | PASS | JDK与既有Digests/组件/Spring/Nimbus，不加通用工具依赖 | 全部production import与pom差异清单 | None；仅Step19规范通过 |
| Rule 6 | Applicable | PASS | managed Jackson；枚举显式持久化code；storage codec与JWT wire分离，未知schema拒绝 | JSON/toString隐藏、scope/ID payload与actualJWK tests | None；仅Step19规范通过 |
| Rule 7 | Applicable | PASS | 没有YAML/属性键变化，既有所有配置profile合同保留；AS code CSRF明确保护nativePOST | OAuthConfig/PublicEndpointSettings/Edge配置tests与static gate | None；仅Step19规范通过 |
| Rule 9 | Applicable | PASS | 真实GrantTransaction Strategy、事务Decorator、框架Adapter和版本State复用，无新增抽象层 | 同意/取消/回滚/重放/reauth/范围收窄native分支 | None；仅Step19规范通过 |
| Rule 10 | Applicable | PASS | Instant/Clock/Duration及SAS metadata时刻类型；没有java.util.Date/Calendar/SimpleDateFormat | expired/pending/family/max_age/签名auth_time tests | None；仅Step19规范通过 |
| Rule 11 | Applicable | PASS | 传统分包+MP/同库Outbox；仅一个managed ALTER/manifest追加；95旧SQL/软删除与jti约束不变 | 编译/managed-schema/SQL-byte/atomic-outbox/static；PG另列runtime | None；仅Step19规范通过 |

### 9.2 独立最终Manual Check（所有交付提交与有效Specs）

逐项复看原18步骤/7纠正/Step19/Step20最终树，不从Step19预测状态复制总体结论。未改其他模块来掩盖失败；技术规则重新核对C10后PASS，必需运行/完整reactor和B-05产物限制使最终TEST/BLOCKER仍保持BLOCKED。

| MC ID | Applicability | 状态 | 最终证据/发现 | 动作 |
| --- | --- | --- | --- | --- |
| MC-ARCH-001 | Applicable | PASS | 传统Admin Controller→Service→Repository→DAO保持；只新增同目录BO/page，Edge仍OAuth入口，Starter无Admin反向依赖；V-C9b-ADMIN/CONSUMERS编译与File1–33路径清单 | None；runtime限制仍在逐REQ矩阵 |
| MC-REUSE-001 | Applicable | PASS | SAS request/consent/UserInfo原Provider、Spring session/CSRF、既有MP/cipher/Outbox与BaseConverter复用；真实SAS/JWK/Boot validation native24及storage16 | None；runtime限制仍在逐REQ矩阵 |
| MC-DEP-001 | Applicable | PASS | 33路径无pom/package/build依赖修改；Nimbus/Hibernate/Jackson皆当前已有受管库；差异清单与offline Maven exit0 | None；runtime限制仍在逐REQ矩阵 |
| MC-NAME-001 | Applicable | PASS | 新OAuthPendingConsentBO及既有SignedTokenClaimsBO/AttributesBO；Controller/Provider/filter是行为扩展点，Mixin/validation group为框架技术类型；编译与类型声明逐项检查 | None；runtime限制仍在逐REQ矩阵 |
| MC-VALID-001 | Applicable | PASS | C10 Jakarta DTO→真实Service的ValidationUtils schema→client subset/Core USER约束→原Valid Repository→MapStruct view→UI明确set；native25/migration14/Core29/管理14/Controller6通过 | None；运行验收独立开放 |
| MC-MODEL-001 | Applicable | PASS | Pending/Attributes/SignedClaims保留完整Data/NoArgs/AllArgs/Accessors/Builder；没有新实体record；state从普通JSON/toString排除；实际Lombok编译、原生密文重载与组校验 | None；runtime限制仍在逐REQ矩阵 |
| MC-CONVERT-001 | Applicable | PASS | OAuthAuthorizationConverter/SasAuthorizationConverter仍MapStruct +BaseConverter<S,T>；copyAttributes由生成Mapper实现；SAS框架投影维持原Adapter；storage16/native24与实际target/generated-sources/annotations | None；runtime限制仍在逐REQ矩阵 |
| MC-LOG-001 | Applicable | PASS | AuthenticationProvider/LoginFilter/ExactIssuerFilter/ConsentController/TokenIssueService带Slf4j；仅取消常量debug，不日志state/password/token；业务diff逐项与fault/rejection测试 | None；runtime限制仍在逐REQ矩阵 |
| MC-BEAN-001 | Applicable | PASS | 具名ResourceServerServiceImpl及@RequiredArgsConstructor/Slf4j；defaultValidator新增NonNull+Qualifier并由Lombok复制；真实constructor8和native服务调用通过 | None；无手写Spring构造器 |
| MC-UTIL-001 | Applicable | PASS | 新逻辑使用JDK URI/time/collections与既有Digests、SAS/Nimbus/Spring扩展；没有Utils/额外通用工具包；33路径import/dependency逐项检查、offline编译 | None；runtime限制仍在逐REQ矩阵 |
| MC-JSON-001 | Applicable | PASS | managed Jackson显式storageMixin只对AES-GCM codec开放隐藏属性；v1/v2/unknown闭合；@EnumValue/JsonValue；scope String↔内部Set；storage16/OIDC17/native24和OAS/published合同 | None；runtime限制仍在逐REQ矩阵 |
| MC-TIME-001 | Applicable | PASS | Clock/Instant/Duration贯穿pending300s、reauth300s和immutable family；SAS iat/exp/nbf/auth_time重载Instant；边界/expired/freshness/RT cap与原生UserInfo测试 | None；runtime限制仍在逐REQ矩阵 |
| MC-CONFIG-001 | Applicable | PASS | 本Step未改YAML或属性键；既有base/local绑定/环境同构保留，Spring CSRF只对native browser POST要求；Admin OAuthConfig/PublicEndpointSettings/ManagedSchema与Edge profile合同、static脚本 | None；runtime限制仍在逐REQ矩阵 |
| MC-PATTERN-001 | Applicable | PASS | GrantTransaction Strategy +事务Decorator包原SASProvider；issuer redirect Adapter只改内部consent；pending/consumed/revoked版本State沿既有CAS；native同意/取消/故障/重放/精确scope/reauth实际分支通过 | None；runtime限制仍在逐REQ矩阵 |
| MC-SCOPE-001 | Applicable | PASS | Step19提交33、Step20声明19路径；三个新增测试仅直接构造/页面合同兼容；2dirty Spec SHA/97个HEAD SQL与manifest不变；scripts/work原有ignored Markdown未触碰 | 提交后核对唯一Step20hash/精确19路径；无无关提交 |
| MC-TEST-001 | Applicable | BLOCKED | 当前Admin364/Core29/UI50/Grant11及共享源码类型/strict通过；原typecheck仍因已安装0.2.3旧接口失败；完整reactor历史失败和必需运行无最终PASS | 刷新正确共享包产物；用户控制运行环境和独立全reactor问题；不宣称全验收PASS |
| MC-BLOCKER-001 | Applicable | BLOCKED | B-01/02/03/04已关；技术10Rules通过；B-05安装产物、PG/Redis/TLS/多实例/browser/负载及完整reactor证明仍开放 | 保留PARTIAL并按本报告运行/依赖边界补证，不自动启动或发布 |

| Literal Rule | Applicability | 状态 | 最终diff/path与测试证据 | 发现/动作 |
| --- | --- | --- | --- | --- |
| Rule 1 | Applicable | PASS | Pending/Attributes/SignedClaims BO与行为类语义命名，引用Step19 File10–22/30–33；实际编译/类型inventory/native/OAS | None；技术规则不替代业务/运行验收 |
| Rule 2 | Applicable | PASS | C10 DTO/Controller/Service schema与Core域约束、原Valid Repository/BO、MapStruct view与UI set完整传递；native25、migration14、Controller6、管理14/Core29验证准确边界；SPI合同不强化 | B-04闭合；运行/本机旧包不因技术规则通过而豁免 |
| Rule 3 | Applicable | PASS | 普通模型完整Lombok；不可变既有VO record不改；MapStruct/BaseConverter及生成copyAttributes；生成Mapper/constructor8/密文重载 | None；技术规则不替代业务/运行验收 |
| Rule 4 | Applicable | PASS | 相关业务Slf4j；具名Spring Bean、final qualified fields、RequiredArgsConstructor；工厂delegate不伪造Bean；constructor8与实际Boot/nativecontext；日志diff | None；技术规则不替代业务/运行验收 |
| Rule 5 | Applicable | PASS | JDK与既有Digests/组件/Spring/Nimbus，不加通用工具依赖；全部production import与pom差异清单 | None；技术规则不替代业务/运行验收 |
| Rule 6 | Applicable | PASS | managed Jackson；枚举显式持久化code；storage codec与JWT wire分离，未知schema拒绝；JSON/toString隐藏、scope/ID payload与actualJWK tests | None；技术规则不替代业务/运行验收 |
| Rule 7 | Applicable | PASS | 没有YAML/属性键变化，既有所有配置profile合同保留；AS code CSRF明确保护nativePOST；OAuthConfig/PublicEndpointSettings/Edge配置tests与static gate | None；技术规则不替代业务/运行验收 |
| Rule 9 | Applicable | PASS | 真实GrantTransaction Strategy、事务Decorator、框架Adapter和版本State复用，无新增抽象层；同意/取消/回滚/重放/reauth/范围收窄native分支 | None；技术规则不替代业务/运行验收 |
| Rule 10 | Applicable | PASS | Instant/Clock/Duration及SAS metadata时刻类型；没有java.util.Date/Calendar/SimpleDateFormat；expired/pending/family/max_age/签名auth_time tests | None；技术规则不替代业务/运行验收 |
| Rule 11 | Applicable | PASS | 传统分包+MP/同库Outbox；仅一个managed ALTER/manifest追加；95旧SQL/软删除与jti约束不变；编译/managed-schema/SQL-byte/atomic-outbox/static；PG另列runtime | None；技术规则不替代业务/运行验收 |

### 9.3 Step20阻塞Manual Check与Literal Rules（19路径）

用户已批准C10；三个必要fixture兼容路径先声明并strict通过。以下仅验证C10可提交，不免除9.2的全任务运行/产物限制。

| MC ID | Applicability | 状态 | Diff/path/符号证据 | 测试/静态证据 | 发现 | 动作 |
| --- | --- | --- | --- | --- | --- | --- |
| MC-ARCH-001 | Applicable | PASS | 既有Controller→Service→Repository→DAO与Core value object；19路径无新层/模块 | Admin/Core Maven当前树编译 | 传统域分包保留 | None；全Spec限制另列 |
| MC-REUSE-001 | Applicable | PASS | Boot defaultValidator/Common ValidationUtils、既有Core guard、MP/Outbox和生成MapStruct view | 管理14/Controller6/native25/migration14 | 只复用既有能力 | None；全Spec限制另列 |
| MC-DEP-001 | Applicable | PASS | 生产5路径；无POM/package/lock/build配置修改，安装共享包原样 | diff路径与offline Maven、source typecheck | 无依赖引入/升级 | None；全Spec限制另列 |
| MC-NAME-001 | Applicable | PASS | UpsertClientResourceGrantDTO、既有GrantBO与服务/Controller；Core不可变record按值对象例外 | 类型/声明diff与编译 | 无新语义类型或伪POJO | None；全Spec限制另列 |
| MC-VALID-001 | Applicable | PASS | DTO Jakarta max64/元素128/pattern；Service手工schema与client上界，Core BUSINESS/tenant/context；原Repo Valid | 管理schema/越权/版本/批处理、Core非法OIDC/不可变与Controller400 | 每个受影响交接有边界，ValidationUtils不承载业务状态 | None；全Spec限制另列 |
| MC-MODEL-001 | Applicable | PASS | DTO完整Data/NoArgs/AllArgs/private factory Builder/Accessors与defensive Set；Core record不可变 | Caller mutation/returned Set与真实Mapper/native路径 | 无模型规范降级 | None；全Spec限制另列 |
| MC-CONVERT-001 | Applicable | PASS | 原ClientResourceGrantViewConverter/ResourceServerViewConverter的MapStruct+BaseForwardConverter继续使用 | native实际生成Converter；管理/list JSON回读；全Admin persistence合同 | 无手写映射替代 | None；全Spec限制另列 |
| MC-LOG-001 | Applicable | PASS | 两个生产Service均保留Slf4j；无新增日志，不输出scope相关凭据或tokens | production diff逐行检查 | 无敏感/高频日志新增 | None；全Spec限制另列 |
| MC-BEAN-001 | Applicable | PASS | ResourceServerServiceImpl具名@RequiredArgsConstructor，新增defaultValidator为NonNull/Qualifier final；lombok.config保持 | SpringConstructorResolutionTest8与实际native服务调用 | 现有Boot Bean消歧，无手写构造器 | None；全Spec限制另列 |
| MC-UTIL-001 | Applicable | PASS | JDK集合/regex/time和现有Common ValidationUtils；不增加Utils或第三方通用库 | imports/manifests/diff与Maven编译 | 白名单/组件复用 | None；全Spec限制另列 |
| MC-JSON-001 | Applicable | PASS | 既有Jackson接口不变，DTO set↔BO list/VO set完整；USER持久化排序，不改SERVICE列表/枚举wire | Controller6、管理14、native密文重载与migration精确比对 | 不泄漏/发明tenant/context或scope | None；全Spec限制另列 |
| MC-TIME-001 | Applicable | PASS | 本Step无日期实体变化；迁移原java.time source历史值与Clock/Instant沿用 | migration14/原历史恢复及全Admin合同 | 无Date/Calendar或时间语义漂移 | None；全Spec限制另列 |
| MC-CONFIG-001 | Applicable | PASS | 无YAML/属性键变化；新增Validator复用Boot名称；本机安装产物B-05单列 | OAuthConfig/ManagedSchema/PublicEndpoint/constructor gates | profile结构保持，源码检查不是安装包证明 | None；全Spec限制另列 |
| MC-PATTERN-001 | Applicable | PASS | C10为简单有界集合恢复，复用Service/immutable value object；C9 Strategy/Decorator不改 | upsert/update/batch/import/replay/native分支覆盖 | 无需新增抽象模式；复杂原生流程仍有既有模式 | None；全Spec限制另列 |
| MC-SCOPE-001 | Applicable | PASS | 用户C10批准+AGENTS直接必要兼容规则；19明确路径，2dirty Spec/97 SQL/manifest/ignored Markdown保持 | scope-proof/strict/diff与最终缓存清单 | 无迁移/数据/无关模块改动 | None；全Spec限制另列 |
| MC-TEST-001 | Applicable | PASS | 行为RED均观察；最终364 Admin+29 Core/50 UI与源码类型、strict成功，失败/夹具错误单列 | V-C10全日志及末次exit0；无skip | 满足本Step受控门禁，不外推full reactor/runtime | None；全Spec限制另列 |
| MC-BLOCKER-001 | Applicable | PASS | 本Step其余16MC和10Rules均逐行PASS；19路径与2dirty无重叠；最终全Spec限制单列 | 前commit缓存check及单一提交由最终交付核对 | 可交付C10；不宣称整个Goal完成 | None；全Spec限制另列 |

| Literal Rule | Applicability | 状态 | Diff/path/符号证据 | 测试/静态证据 | 发现 | 动作 |
| --- | --- | --- | --- | --- | --- | --- |
| Rule 1 | Applicable | PASS | 既有DTO/BO和行为服务名；Core为不可变领域值对象 | 编译/类型声明 | 无普通实体record | None；全Spec限制另列 |
| Rule 2 | Applicable | PASS | DTO Jakarta→Service ValidationUtils schema/client business→Core→原Repo Valid→View/UI | Core29/管理14/Controller6/native25/migration14 | 没有仅Controller验证或非法SPI强化 | None；全Spec限制另列 |
| Rule 3 | Applicable | PASS | DTO完整Lombok+factory Builder/defensive Set；Core immutable record；MapStruct BaseForwardConverter不改 | 生成Mapper/native回读及immutable tests | 按值对象例外处理 | None；全Spec限制另列 |
| Rule 4 | Applicable | PASS | 两个Service具名/Slf4j/RequiredArgsConstructor；defaultValidator qualifier final注入 | constructor8/native真实调用与log diff | 无手写Spring构造器或凭据日志 | None；全Spec限制另列 |
| Rule 5 | Applicable | PASS | JDK集合与既有Common ValidationUtils，不加工具/依赖 | import/path inventory与offline编译 | 符合白名单与能力复用 | None；全Spec限制另列 |
| Rule 6 | Applicable | PASS | 既有Boot Jackson DTO/BO/View字段/枚举code不变；USER exact set传递 | Controller JSON/原储存contract/native | 无JSON框架或ordinal引入 | None；全Spec限制另列 |
| Rule 7 | Applicable | PASS | 无环境属性键/配置文件变化；Boot defaultValidator沿用 | 全Admin配置/constructor合同 | 本机旧包产物另列B-05 | None；全Spec限制另列 |
| Rule 9 | Applicable | PASS | 简单bounded set沿Service/value object；复杂C9原生Strategy/Decorator未触碰 | upsert/batch/import/replay/native正负例 | 不为局部集合增加无价值模式 | None；全Spec限制另列 |
| Rule 10 | Applicable | PASS | 保持java.time/Clock/Instant，历史created/updated恢复值未改 | migration14与原历史合同 | 不引入旧日期类 | None；全Spec限制另列 |
| Rule 11 | Applicable | PASS | 传统分包、MP端口与同库事务/Outbox不改；USER不发SERVICE投影；97SQL/manifest不变 | 全部Admin/Core与schema/bytes/static | 无生成器/新DDL/soft-delete规则变更 | None；全Spec限制另列 |

## 10. 迁移、兼容、非目标与回退

| 合同项 | 状态 | 证据/下一步 |
| --- | --- | --- |
| 旧Flyway/受管SQL内容与checksum不可变 | Satisfied | 本轮git对照执行起点无任何历史SQL修改；新版本不以repair/覆盖旧SQL代替 |
| 15新表+13精确legacy只读源+2Outbox技术表路由 | Runtime unverified | OAuthPersistenceConfiguration/Schema contract；PG topology实际运行待验 |
| 仅明确offline profile/enable/freeze/approved SHA/owner-only报告触发导入 | Satisfied | OAuthMigrationRunner/MigrationService20 tests；本任务未执行runner |
| 身份subject/hash/version/time保留与OAuth归属/scope冲突禁止扩权 | Runtime unverified | USER审核范围/原历史字段/冲突/游标恢复已通过；实际PG对账未做 |
| 500/501与未知commit/写checkpoint失败恢复 | Satisfied | C17 20测试包含真实Service调用和临时文件；非PG transaction运行证明 |
| confidential新bcrypt secret分发一次、旧RT/cookie不导入 | Runtime unverified | migration file/clientpurpose合同；真实运营交付/旧session失效待验 |
| exact realm readiness、即时撤销和两租户负例切流门禁 | Partial | 新部署410和隔离测试存在；B-01已由Step19闭合；B-04业务Grant配置已验证；真实ready/fence/多实例仍未完成 |
| Portal/四Admin与Starter/Gateway同时更新 | Partial | 代码/配置/原生Filter与模块已验证；真实五端SSO及部署切流未运行 |
| 真实备份/停写/导入/发布后的回退边界 | Runtime unverified | runbooks规定新写后先冻结对账，禁止恢复旧generation/secret；实际备份/演练未做 |
| 鉴神RBAC3、天枢信任/目录、玉衡biz/MCP数据面保持职责 | Satisfied | 既有桥/过滤器/注册/HMAC/流取消门禁；未重建权限模型/Engine/DDC |
| 无动态注册/password/implicit/token exchange/GraphQL/移动端 | Satisfied | SAS仅批准grant入口、旧根410；无新公开相关路由 |
| 不得自动启动运行栈 | Not satisfied | 一次无guard reactor误启临时Redis已披露并确认自动清理；后续只guarded/focused，不将其改称PASS |


## 附录 A. 每个提交的精确路径

仅列git实际committed路径，不等同未提交工作树内容。

### Step 1 — 1f0cc584eadc21c0920251103bb5b62192614f6d

chore(xingyuan): establish oauth dependency and edge module

```text
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/pom.xml
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-core/lombok.config
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-core/pom.xml
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/pom.xml
egon-cola-xingyuan/egon-cola-yuheng/pom.xml
egon-cola-xingyuan/egon-cola-yuheng/yuheng-edge-gateway/lombok.config
egon-cola-xingyuan/egon-cola-yuheng/yuheng-edge-gateway/pom.xml
egon-cola-xingyuan/egon-cola-yuheng/yuheng-edge-gateway/src/main/java/top/egon/cola/component/yuheng/edge/GatewayEdgeApplication.java
egon-cola-xingyuan/pom.xml
```

### Step 2 — eee09da9bbe36c6158be0ac453103df88978debd

feat(shoubing): define managed oauth schema and routing

```text
docs/egon/plan/2026-09-23-10-42-tianquan-oauth-oidc-implementation.md
docs/egon/spec/2026-10-08-10-18-oauth-current-code-alignment.md
egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter/src/main/java/top/egon/cola/component/common/mybatis/ddl/EgonColaDdlManifestBO.java
egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter/src/test/java/top/egon/cola/component/common/mybatis/ddl/EgonColaDdlManifestFamilyTest.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/config/OAuthManagedDdlComponent.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/config/OAuthPersistenceConfiguration.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/resources/application-local.yml
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/resources/application.yml
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/resources/db/egon-mp/V20261008_001__initialize_oauth_tenant_schema.sql
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/resources/db/egon-mp/repository-manifest.json
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/resources/oauth-sharding-native.yml
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/config/OAuthManagedSchemaContractTest.java
```

### Step 3 — bca73d607c58b8f8c724f96cba20b27975ca854f

refactor(shoubing): move identity directory persistence to MP

```text
docs/egon/plan/2026-09-23-10-42-tianquan-oauth-oidc-implementation.md
docs/egon/spec/2026-10-08-10-18-oauth-current-code-alignment.md
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/pom.xml
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/identity/dao/IdentityCredentialDAO.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/identity/dao/IdentityUserDAO.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/identity/domain/converter/IdentityCredentialConverter.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/identity/domain/converter/IdentityUserConverter.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/identity/domain/po/IdentityCredentialPO.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/identity/domain/po/IdentityUserPO.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/identity/repo/IdentityDirectoryContext.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/identity/repo/IdentityPersistenceAdapter.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/identity/repo/JpaIdentityUserDirectory.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/identity/repo/MpIdentityUserDirectory.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/identity/repository/IdentityCredentialRepository.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/identity/repository/IdentityUserRepository.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/identity/support/rpc/IdentityDirectoryRpcProvider.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/config/OAuthPersistenceConfiguration.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/support/bootstrap/IdpDevelopmentTenantBootstrap.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/tenant/controller/TenantController.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/tenant/dao/IdentityTenantDAO.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/tenant/dao/IdentityTenantMembershipDAO.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/tenant/dao/IdentityTenantSettingsJsonbTypeHandler.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/tenant/domain/bo/IdentityTenantBO.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/tenant/domain/bo/IdentityTenantMembershipBO.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/tenant/domain/converter/IdentityTenantConverter.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/tenant/domain/converter/IdentityTenantMembershipConverter.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/tenant/domain/dto/UpdateTenantDTO.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/tenant/domain/dto/UpsertTenantMembershipDTO.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/tenant/domain/po/IdentityTenantMembershipPO.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/tenant/domain/po/IdentityTenantPO.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/tenant/domain/vo/TenantMembershipVO.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/tenant/domain/vo/TenantVO.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/tenant/repo/IdentityTenantMembershipRepository.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/tenant/repo/IdentityTenantRepository.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/tenant/service/TenantMembershipService.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/tenant/service/TenantService.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/tenant/service/impl/TenantMembershipServiceImpl.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/tenant/service/impl/TenantServiceImpl.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/resources/application-local.yml
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/resources/application.yml
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/resources/mapper/identity/IdentityCredentialDAO.xml
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/resources/mapper/identity/IdentityUserDAO.xml
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/resources/mapper/tenant/IdentityTenantDAO.xml
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/resources/mapper/tenant/IdentityTenantMembershipDAO.xml
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/tianquan/shoubing/admin/identity/domain/pojo/IdentityEntityMappingTest.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/tianquan/shoubing/admin/identity/repo/IdentityMpPersistenceContractTest.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/tianquan/shoubing/admin/identity/repo/IdentityPersistenceAdapterTest.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/tianquan/shoubing/admin/identity/support/rpc/IdentityDirectoryRpcProviderTest.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/tianquan/shoubing/admin/support/bootstrap/IdpDevelopmentTenantBootstrapTest.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/tianquan/shoubing/admin/tenant/controller/TenantControllerTest.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/tianquan/shoubing/admin/tenant/service/LocalTenantMembershipPortTest.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/tianquan/shoubing/admin/tenant/service/impl/TenantMembershipServiceImplTest.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/tianquan/shoubing/admin/tenant/service/impl/TenantServiceImplTest.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-core/src/main/java/top/egon/cola/platform/tianquan/shoubing/core/identity/PasswordCredential.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/pom.xml
```

### Step 4 — 89acea901817b3567e2053ee76de41840cc48722

feat(shoubing): persist oauthrealm/oauthclient state

```text
docs/egon/plan/2026-09-23-10-42-tianquan-oauth-oidc-implementation.md
docs/egon/spec/2026-10-08-10-18-oauth-current-code-alignment.md
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/dao/OAuthClientMapper.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/dao/OAuthRealmMapper.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/dao/OAuthStringListJsonbTypeHandler.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/domain/bo/OAuthClientBO.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/domain/bo/OAuthRealmBO.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/domain/bo/TrustedRealmBO.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/domain/converter/OAuthClientConverter.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/domain/converter/OAuthRealmConverter.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/domain/enums/OAuthClientPurposeEnum.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/domain/enums/RealmTypeEnum.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/domain/po/OAuthClientPO.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/domain/po/OAuthRealmPO.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/repository/OAuthClientRepository.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/repository/OAuthRealmRepository.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/repository/impl/OAuthClientRepositoryImpl.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/repository/impl/OAuthRealmRepositoryImpl.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/resources/mapper/oauth/OAuthClientMapper.xml
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/resources/mapper/oauth/OAuthRealmMapper.xml
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/repository/OAuthRealmPersistenceContractTest.java
```

### Step 5 — 58db54bbe48b54c248cff575266ab506870d27ca

feat(shoubing): persist oauthresource/oauthgrant state

```text
docs/egon/plan/2026-09-23-10-42-tianquan-oauth-oidc-implementation.md
docs/egon/spec/2026-10-08-10-18-oauth-current-code-alignment.md
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/resource/dao/OAuthGrantMapper.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/resource/dao/OAuthResourceMapper.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/resource/domain/bo/OAuthGrantBO.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/resource/domain/bo/OAuthResourceBO.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/resource/domain/converter/OAuthGrantConverter.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/resource/domain/converter/OAuthResourceConverter.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/resource/domain/po/OAuthGrantPO.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/resource/domain/po/OAuthResourcePO.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/resource/repository/OAuthGrantRepository.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/resource/repository/OAuthResourceRepository.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/resource/repository/impl/OAuthGrantRepositoryImpl.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/resource/repository/impl/OAuthResourceRepositoryImpl.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/resources/mapper/resource/OAuthGrantMapper.xml
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/resources/mapper/resource/OAuthResourceMapper.xml
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/repository/OAuthResourcePersistenceContractTest.java
```

### Step 6 — 2c7afc9da2c537b838984c8daf1a0dbc918ca7c3

feat(shoubing): persist oauthauthorization/oauthtoken/oauthconsent state

```text
docs/egon/plan/2026-09-23-10-42-tianquan-oauth-oidc-implementation.md
docs/egon/spec/2026-10-08-10-18-oauth-current-code-alignment.md
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/dao/OAuthAuthorizationMapper.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/dao/OAuthConsentMapper.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/domain/bo/OAuthAuthorizationAttributesBO.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/domain/bo/OAuthAuthorizationBO.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/domain/bo/OAuthConsentBO.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/domain/bo/OAuthSecuritySnapshotBO.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/domain/converter/OAuthAuthorizationConverter.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/domain/converter/OAuthConsentConverter.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/domain/po/OAuthAuthorizationPO.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/domain/po/OAuthConsentPO.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/repository/OAuthAuthorizationRepository.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/repository/OAuthConsentRepository.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/repository/impl/OAuthAuthorizationRepositoryImpl.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/repository/impl/OAuthConsentRepositoryImpl.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/config/OAuthKeyEncryptionProperties.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/dao/OAuthTokenMapper.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/domain/bo/OAuthTokenBO.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/domain/bo/OAuthTokenMetadataBO.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/domain/bo/SignedTokenClaimsBO.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/domain/converter/OAuthTokenConverter.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/domain/enums/OAuthTokenTypeEnum.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/domain/po/OAuthTokenPO.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/repository/OAuthTokenRepository.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/repository/impl/OAuthTokenRepositoryImpl.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/service/impl/OAuthPayloadCipherComponent.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/resources/application-local.yml
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/resources/application.yml
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/resources/mapper/oauth/OAuthAuthorizationMapper.xml
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/resources/mapper/oauth/OAuthConsentMapper.xml
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/resources/mapper/token/OAuthTokenMapper.xml
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/repository/OAuthAuthorizationPersistenceContractTest.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-core/src/main/java/top/egon/cola/platform/tianquan/shoubing/contract/OAuthSecurityVersionBO.java
```

### Step 7 — ff785f8e04761a11e16bdddfd6d208dff72336c8

feat(shoubing): persist oauthsigningkey/oauthsecuritystate/oauthrevocation/oauthaudit state

```text
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/audit/dao/OAuthAuditMapper.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/audit/domain/bo/OAuthAuditBO.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/audit/domain/converter/OAuthAuditConverter.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/audit/domain/po/OAuthAuditPO.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/audit/repository/OAuthAuditRepository.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/audit/repository/impl/OAuthAuditRepositoryImpl.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/dao/OAuthRevocationMapper.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/dao/OAuthSecurityStateMapper.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/dao/OAuthSigningKeyMapper.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/domain/bo/OAuthRevocationBO.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/domain/bo/OAuthSecurityStateBO.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/domain/bo/OAuthSigningKeyBO.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/domain/converter/OAuthRevocationConverter.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/domain/converter/OAuthSecurityStateConverter.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/domain/converter/OAuthSigningKeyConverter.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/domain/enums/OAuthKeyStatusEnum.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/domain/enums/OAuthSecurityTargetEnum.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/domain/po/OAuthRevocationPO.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/domain/po/OAuthSecurityStatePO.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/domain/po/OAuthSigningKeyPO.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/repository/OAuthRevocationRepository.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/repository/OAuthSecurityStateRepository.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/repository/OAuthSigningKeyRepository.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/repository/impl/OAuthRevocationRepositoryImpl.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/repository/impl/OAuthSecurityStateRepositoryImpl.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/repository/impl/OAuthSigningKeyRepositoryImpl.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/service/impl/OAuthKeyCipherComponent.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/resources/mapper/audit/OAuthAuditMapper.xml
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/resources/mapper/token/OAuthRevocationMapper.xml
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/resources/mapper/token/OAuthSecurityStateMapper.xml
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/resources/mapper/token/OAuthSigningKeyMapper.xml
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/repository/OAuthSigningKeyPersistenceContractTest.java
```

### Step 8 — 600f9bad9e344339cf60626607f837889dc7ba44

feat(shoubing): add multitenant authorization-server protocol

```text
docs/egon/plan/2026-09-23-10-42-tianquan-oauth-oidc-implementation.md
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/config/AuthorizationServerConfiguration.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/config/OAuthProtocolOpenApiConfiguration.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/dao/OAuthClientMapper.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/domain/converter/SasAuthorizationConverter.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/domain/converter/SasConsentConverter.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/domain/converter/SasRegisteredClientConverter.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/repository/OAuthClientRepository.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/repository/TenantRegisteredClientRepository.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/repository/impl/OAuthClientRepositoryImpl.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/service/TenantIssuerService.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/service/impl/TenantAuthorizationServiceImpl.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/service/impl/TenantConsentServiceImpl.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/service/impl/TenantIssuerServiceImpl.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/service/impl/TenantJwkSourceComponent.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/resources/application-local.yml
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/resources/application.yml
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/resources/mapper/oauth/OAuthClientMapper.xml
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/TenantIssuerIsolationTest.java
```

### Step 9 — c621d3b4287ddde0c538c80cef0fc5f890f8344a

feat(shoubing): implement code pkce oidc and client grants

```text
docs/egon/plan/2026-09-23-10-42-tianquan-oauth-oidc-implementation.md
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/audit/support/outbox/OAuthAuditOutboxDeliveryHandler.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/config/AuthorizationServerConfiguration.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/config/OAuthConfig.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/config/OAuthProtocolOpenApiConfiguration.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/controller/TenantLoginController.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/controller/filter/TenantLoginAuthenticationFilter.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/domain/command/ConsumeOAuthTokenCommand.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/domain/converter/SasAuthorizationConverter.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/repository/impl/OAuthAuthorizationRepositoryImpl.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/service/OAuthTokenIssueService.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/service/impl/OAuthTokenIssueServiceImpl.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/service/impl/TenantAuthorizationServiceImpl.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/service/impl/TransactionalOAuthAuthenticationProvider.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/config/TokenConfig.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/service/impl/OAuthSecurityProjectionComponent.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/resources/application-local.yml
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/resources/application.yml
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/TenantIssuerIsolationTest.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/config/OAuthOidcProtocolContractTest.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/repository/OAuthAuthorizationPersistenceContractTest.java
```

### Step 10 — a7a6094295f310c270bb12e0761ca44244a13217

feat(shoubing): scope oauth administration and rotate secrets

```text
docs/egon/plan/2026-09-23-10-42-tianquan-oauth-oidc-implementation.md
docs/egon/spec/2026-10-08-10-18-oauth-current-code-alignment.md
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/controller/OAuthClientController.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/domain/bo/OAuthClientBO.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/domain/converter/OAuthClientConverter.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/domain/converter/OAuthClientViewConverter.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/domain/dto/CreateOAuthClientDTO.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/domain/dto/OAuthValueDTO.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/domain/dto/RotateClientSecretDTO.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/domain/dto/UpdateOAuthClientDTO.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/domain/vo/CreatedOAuthClientVO.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/domain/vo/OAuthClientVO.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/domain/vo/RotatedClientSecretVO.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/service/OAuthClientService.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/service/impl/OAuthClientServiceImpl.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/service/impl/OAuthManagementRealmResolver.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/resource/config/ResourceServerConfig.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/resource/controller/ClientResourceGrantController.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/resource/controller/ResourceServerController.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/resource/dao/OAuthGrantMapper.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/resource/dao/OAuthResourceMapper.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/resource/domain/bo/OAuthResourceBO.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/resource/domain/converter/ClientResourceGrantViewConverter.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/resource/domain/converter/OAuthResourceConverter.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/resource/domain/converter/ResourceServerViewConverter.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/resource/domain/dto/BatchClientResourceGrantDTO.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/resource/domain/dto/DeleteClientResourceGrantDTO.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/resource/domain/dto/UpsertClientResourceGrantDTO.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/resource/domain/vo/ClientResourceGrantVO.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/resource/domain/vo/ResourceServerVO.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/resource/repository/MpResourceServerStore.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/resource/repository/OAuthGrantRepository.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/resource/repository/OAuthResourceRepository.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/resource/repository/impl/OAuthGrantRepositoryImpl.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/resource/repository/impl/OAuthResourceRepositoryImpl.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/resource/service/ResourceServerProjectionService.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/resource/service/ResourceServerService.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/resource/service/impl/ResourceServerServiceImpl.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/resource/support/outbox/ResourceServerProjectionDeliveryHandler.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/resource/support/outbox/TransactionalOutboxResourceServerEventAdapter.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/support/bootstrap/IdpDevelopmentClientBootstrap.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/controller/SigningKeyController.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/domain/bo/OAuthSigningKeyBO.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/domain/converter/OAuthSigningKeyConverter.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/domain/converter/SigningKeyViewConverter.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/domain/dto/PublishSigningKeyDTO.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/domain/vo/SigningKeyVO.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/service/SigningKeyService.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/service/impl/SigningKeyServiceImpl.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/resources/mapper/resource/OAuthGrantMapper.xml
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/resources/mapper/resource/OAuthResourceMapper.xml
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/controller/OAuthClientControllerTest.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/repository/OAuthRealmPersistenceContractTest.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/repository/OAuthResourcePersistenceContractTest.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/repository/OAuthSigningKeyPersistenceContractTest.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/service/impl/OAuthClientServiceImplTest.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/service/impl/OAuthManagementRealmResolverTest.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/tianquan/shoubing/admin/resource/controller/ClientResourceGrantControllerTest.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/tianquan/shoubing/admin/resource/controller/ResourceServerControllerTest.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/tianquan/shoubing/admin/resource/service/ResourceServerProjectionServiceTest.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/tianquan/shoubing/admin/resource/service/impl/ResourceServerServiceImplTest.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/tianquan/shoubing/admin/resource/support/outbox/ResourceServerProjectionDeliveryHandlerTest.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/tianquan/shoubing/admin/resource/support/outbox/TransactionalOutboxResourceServerEventAdapterTest.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/tianquan/shoubing/admin/support/bootstrap/IdpDevelopmentClientBootstrapTest.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/tianquan/shoubing/admin/support/runtime/SpringConstructorResolutionTest.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/tianquan/shoubing/admin/support/security/IdpAdminSecurityIT.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/tianquan/shoubing/admin/token/controller/SigningKeyControllerTest.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/tianquan/shoubing/admin/token/service/impl/SigningKeyServiceImplTest.java
```

### Step 11 — ee817fcbc455436dd59626b12b4a43baf288a2e9

feat(shoubing): enforce shared revocation and durable audit

```text
docs/egon/plan/2026-09-23-10-42-tianquan-oauth-oidc-implementation.md
docs/egon/spec/2026-10-08-10-18-oauth-current-code-alignment.md
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/audit/domain/converter/IdentityAuditViewConverter.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/audit/repository/OAuthAuditRepository.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/audit/repository/impl/OAuthAuditRepositoryImpl.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/audit/service/impl/IdentityAuditServiceImpl.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/audit/service/impl/OAuthAuditConsumer.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/audit/support/outbox/OAuthAuditOutboxDeliveryHandler.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/identity/service/impl/IdentityUserServiceImpl.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/config/AuthorizationServerConfiguration.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/service/OAuthRetentionService.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/service/impl/OAuthClientServiceImpl.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/service/impl/OAuthRetentionServiceImpl.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/service/impl/TransactionalOAuthRevocationAuthenticationProvider.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/support/outbox/service/IdentityOutboxPublisher.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/support/runtime/IdpPlatformConfiguration.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/support/web/IdpHttpExceptionHandler.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/config/OAuthKeyRotationProperties.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/config/OAuthRevocationProperties.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/domain/command/OAuthClientRevokeCommand.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/domain/command/OAuthSubjectRevokeCommand.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/domain/command/RevokeOAuthTokenCommand.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/repository/OAuthRevocationRepository.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/repository/impl/OAuthRevocationRepositoryImpl.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/service/TokenRevocationService.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/service/impl/OAuthSecurityProjectionComponent.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/service/impl/OAuthSigningKeyRotationServiceImpl.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/service/impl/TokenRevocationServiceImpl.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/support/outbox/OAuthSecurityDeliveryHandler.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/resources/application-local.yml
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/resources/application.yml
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/resources/mapper/token/OAuthRevocationMapper.xml
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/tianquan/shoubing/admin/audit/service/impl/IdentityAuditServiceImplTest.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/tianquan/shoubing/admin/audit/service/impl/OAuthAuditConsumerTest.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/tianquan/shoubing/admin/audit/support/outbox/OAuthAuditOutboxDeliveryHandlerTest.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/tianquan/shoubing/admin/identity/service/impl/IdentityAuthenticationTransactionContractTest.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/tianquan/shoubing/admin/identity/service/impl/IdentityUserServiceImplTest.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/config/OAuthOidcProtocolContractTest.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/repository/OAuthSigningKeyPersistenceContractTest.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/service/OAuthRetentionServiceTest.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/service/impl/OAuthClientServiceImplTest.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/tianquan/shoubing/admin/support/outbox/service/IdentityOutboxPublisherTest.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/tianquan/shoubing/admin/support/web/IdpHttpExceptionHandlerTest.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/tianquan/shoubing/admin/token/TokenRevocationServiceTest.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/tianquan/shoubing/admin/token/service/impl/OAuthSecurityProjectionComponentTest.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/tianquan/shoubing/admin/token/service/impl/OAuthSigningKeyRotationServiceImplTest.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-core/src/main/java/top/egon/cola/platform/tianquan/shoubing/core/audit/OAuthSecurityChangedEvent.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-core/src/main/java/top/egon/cola/platform/tianquan/shoubing/core/audit/TokenUsageAuditEvent.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/lombok.config
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/pom.xml
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/tianquan/shoubing/starter/audit/OAuthAuditProperties.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/tianquan/shoubing/starter/audit/TokenUsageAuditComponent.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/tianquan/shoubing/starter/autoconfigure/IdpStarterAutoConfiguration.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/test/java/top/egon/cola/platform/tianquan/shoubing/starter/audit/TokenUsageAuditComponentTest.java
```

### Step 12 — 7813a6eb80bbf6bd19634dc7e61b88ad6e67c147

feat(shoubing): verify tenant jwt locally with strict revocation

```text
docs/egon/plan/2026-09-23-10-42-tianquan-oauth-oidc-implementation.md
docs/egon/spec/2026-10-08-10-18-oauth-current-code-alignment.md
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/config/AuthorizationServerConfiguration.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/service/OAuthTokenIssueService.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/service/impl/OAuthTokenIssueServiceImpl.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/config/OAuthOidcProtocolContractTest.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/pom.xml
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/tianquan/shoubing/starter/autoconfigure/IdpStarterAutoConfiguration.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/tianquan/shoubing/starter/autoconfigure/IdpStarterProperties.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/tianquan/shoubing/starter/security/AccessTokenVerification.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/tianquan/shoubing/starter/security/IdpAuthenticationToken.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/tianquan/shoubing/starter/security/IdpBearerAuthenticationFilter.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/tianquan/shoubing/starter/security/IdpJwtVerifier.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/tianquan/shoubing/starter/security/LegacyIssuerCutoverPolicy.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/tianquan/shoubing/starter/security/OAuthRevocationReader.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/tianquan/shoubing/starter/security/TenantIssuerAuthenticationManagerComponent.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/tianquan/shoubing/starter/security/TrustedIssuerRegistry.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/tianquan/shoubing/starter/security/VerifiedPrincipalConverter.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/tianquan/shoubing/starter/security/VerifiedTenantContextFilter.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/tianquan/shoubing/starter/security/domain/VerifiedJwtBO.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/tianquan/shoubing/starter/security/impl/RedisOAuthRevocationReader.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/tianquan/shoubing/starter/security/reactive/ReactiveOAuthRevocationReader.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/tianquan/shoubing/starter/security/rpc/IdpRpcBearerServerInterceptor.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/test/java/top/egon/cola/platform/tianquan/shoubing/starter/security/MultiIssuerResourceServerTest.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/test/java/top/egon/cola/platform/tianquan/shoubing/starter/security/rpc/IdpRpcBearerServerInterceptorTest.java
```

### Step 13 — 9aeb3cb7970ddfe8b84e31f951bb16b04db66168

feat(shoubing): acquire scoped service tokens with webclient

```text
docs/egon/plan/2026-09-23-10-42-tianquan-oauth-oidc-implementation.md
docs/egon/spec/2026-10-08-10-18-oauth-current-code-alignment.md
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/tianquan/shoubing/starter/autoconfigure/IdpStarterAutoConfiguration.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/tianquan/shoubing/starter/autoconfigure/IdpStarterProperties.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/tianquan/shoubing/starter/client/IdpServiceOAuth2Client.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/tianquan/shoubing/starter/client/ServiceAuthorizationKey.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/tianquan/shoubing/starter/client/ServiceTokenWebClientConfiguration.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/test/java/top/egon/cola/platform/tianquan/shoubing/starter/autoconfigure/IdpStarterAutoConfigurationTest.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/test/java/top/egon/cola/platform/tianquan/shoubing/starter/client/IdpServiceOAuth2ClientTest.java
```

### Step 14 — 3fb37e6a490f0a27bbd15cc06c3383bafe307429

feat(yuheng): add oauth2 bff edge

```text
docs/egon/plan/2026-09-23-10-42-tianquan-oauth-oidc-implementation.md
docs/egon/spec/2026-10-08-10-18-oauth-current-code-alignment.md
egon-cola-xingyuan/egon-cola-yuheng/yuheng-edge-gateway/pom.xml
egon-cola-xingyuan/egon-cola-yuheng/yuheng-edge-gateway/src/main/java/top/egon/cola/component/yuheng/edge/config/EdgeOAuthProperties.java
egon-cola-xingyuan/egon-cola-yuheng/yuheng-edge-gateway/src/main/java/top/egon/cola/component/yuheng/edge/config/EdgeSecurityConfiguration.java
egon-cola-xingyuan/egon-cola-yuheng/yuheng-edge-gateway/src/main/java/top/egon/cola/component/yuheng/edge/config/EdgeSessionRedisConfiguration.java
egon-cola-xingyuan/egon-cola-yuheng/yuheng-edge-gateway/src/main/java/top/egon/cola/component/yuheng/edge/config/EdgeSessionRedisSerializer.java
egon-cola-xingyuan/egon-cola-yuheng/yuheng-edge-gateway/src/main/java/top/egon/cola/component/yuheng/edge/controller/EdgeSessionController.java
egon-cola-xingyuan/egon-cola-yuheng/yuheng-edge-gateway/src/main/java/top/egon/cola/component/yuheng/edge/controller/domain/EdgeCsrfResult.java
egon-cola-xingyuan/egon-cola-yuheng/yuheng-edge-gateway/src/main/java/top/egon/cola/component/yuheng/edge/controller/domain/EdgeLogoutFailureResult.java
egon-cola-xingyuan/egon-cola-yuheng/yuheng-edge-gateway/src/main/java/top/egon/cola/component/yuheng/edge/controller/domain/EdgeSessionResult.java
egon-cola-xingyuan/egon-cola-yuheng/yuheng-edge-gateway/src/main/java/top/egon/cola/component/yuheng/edge/filter/TrustedIdentityHeaderFilter.java
egon-cola-xingyuan/egon-cola-yuheng/yuheng-edge-gateway/src/main/java/top/egon/cola/component/yuheng/edge/service/EdgeLogoutIntentStore.java
egon-cola-xingyuan/egon-cola-yuheng/yuheng-edge-gateway/src/main/java/top/egon/cola/component/yuheng/edge/service/EdgeLogoutRevocationIntent.java
egon-cola-xingyuan/egon-cola-yuheng/yuheng-edge-gateway/src/main/java/top/egon/cola/component/yuheng/edge/service/EdgeLogoutService.java
egon-cola-xingyuan/egon-cola-yuheng/yuheng-edge-gateway/src/main/java/top/egon/cola/component/yuheng/edge/service/impl/EdgeAuthorizedClientRepository.java
egon-cola-xingyuan/egon-cola-yuheng/yuheng-edge-gateway/src/main/java/top/egon/cola/component/yuheng/edge/service/impl/EdgeLogoutRevocationWorker.java
egon-cola-xingyuan/egon-cola-yuheng/yuheng-edge-gateway/src/main/java/top/egon/cola/component/yuheng/edge/service/impl/EdgeOAuthTokenRevocationClient.java
egon-cola-xingyuan/egon-cola-yuheng/yuheng-edge-gateway/src/main/java/top/egon/cola/component/yuheng/edge/service/impl/EdgeSingleFlightAuthorizedClientManager.java
egon-cola-xingyuan/egon-cola-yuheng/yuheng-edge-gateway/src/main/java/top/egon/cola/component/yuheng/edge/service/impl/EdgeTokenCipher.java
egon-cola-xingyuan/egon-cola-yuheng/yuheng-edge-gateway/src/main/java/top/egon/cola/component/yuheng/edge/service/impl/RedissonEdgeLogoutIntentStore.java
egon-cola-xingyuan/egon-cola-yuheng/yuheng-edge-gateway/src/main/resources/application-local.yml
egon-cola-xingyuan/egon-cola-yuheng/yuheng-edge-gateway/src/main/resources/application.yml
egon-cola-xingyuan/egon-cola-yuheng/yuheng-edge-gateway/src/test/java/top/egon/cola/component/yuheng/edge/EdgeLoginContractTest.java
```

### Step 15 — a779aaeab667fa9d1322123aa57796e89efbcf65

feat(xingyuan): integrate edge identity with gateway rbac and tianshu

```text
docs/egon/plan/2026-09-23-10-42-tianquan-oauth-oidc-implementation.md
docs/egon/spec/2026-10-08-10-18-oauth-current-code-alignment.md
egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin/src/main/java/top/egon/cola/platform/tianquan/jianshen/admin/shared/tenant/controller/filter/TenantContextFilter.java
egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin/src/main/java/top/egon/cola/platform/tianquan/jianshen/admin/shared/tenant/domain/TenantContext.java
egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin/src/test/java/top/egon/cola/platform/tianquan/jianshen/admin/shared/tenant/TenantContextFilterTest.java
egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-starter/src/main/java/top/egon/cola/platform/tianquan/jianshen/starter/security/Rbac3AuthenticationToken.java
egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-starter/src/main/java/top/egon/cola/platform/tianquan/jianshen/starter/security/Rbac3BearerAuthenticationFilter.java
egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-starter/src/test/java/top/egon/cola/platform/tianquan/jianshen/starter/security/Rbac3BearerAuthenticationFilterTest.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-gateway-adapter/src/main/java/top/egon/cola/platform/tianquan/shoubing/yuheng/security/IdpUserCredentialRecoveryProvider.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-gateway-adapter/src/test/java/top/egon/cola/platform/tianquan/shoubing/yuheng/security/IdpGatewaySecurityProviderTest.java
egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/test/java/top/egon/cola/component/tianshu/admin/security/tenancy/DdcTenantIsolationTest.java
```

### Step 16 — e71d00d3826cc96d7c46dbdc485cc972bf330ca5

feat(xingyuan): migrate admin auth to oidc bff

```text
docs/egon/plan/2026-09-23-10-42-tianquan-oauth-oidc-implementation.md
docs/egon/spec/2026-10-08-10-18-oauth-current-code-alignment.md
egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin-web/e2e/auth-role-activation.spec.ts
egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin-web/e2e/authorization-version-recovery.spec.ts
egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin-web/src/features/auth/AuthenticationShell.test.tsx
egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin-web/src/features/auth/AuthenticationShell.tsx
egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin-web/src/features/auth/LoginPage.tsx
egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin-web/src/features/role-activation/RoleActivationPage.test.tsx
egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin-web/src/features/role-activation/RoleActivationPage.tsx
egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin-web/vite.config.ts
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin-web/src/api/types.ts
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin-web/src/auth/AuthContext.tsx
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin-web/src/auth/CentralLoginPage.test.tsx
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin-web/src/auth/CentralLoginPage.tsx
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin-web/src/features/clients/ClientListPage.test.tsx
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin-web/src/features/clients/ClientListPage.tsx
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin-web/src/features/keys/SigningKeyPage.test.tsx
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin-web/src/features/keys/SigningKeyPage.tsx
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin-web/src/features/resource-grants/ClientResourceGrantPage.test.tsx
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin-web/src/features/resource-grants/ClientResourceGrantPage.tsx
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin-web/src/features/resource-servers/ResourceServerListPage.test.tsx
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin-web/src/features/resource-servers/ResourceServerListPage.tsx
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin-web/vite.config.ts
egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin-web/README.md
egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin-web/README.zh-CN.md
egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin-web/e2e/tianshu-admin.spec.ts
egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin-web/src/auth/AuthContext.test.ts
egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin-web/src/auth/AuthContext.tsx
egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin-web/src/auth/LoginPage.tsx
egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin-web/src/pages/LoginPage.test.tsx
egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin-web/vite.config.ts
egon-cola-xingyuan/egon-cola-xingyuan-admin-portal/src/app/PortalLayout.test.tsx
egon-cola-xingyuan/egon-cola-xingyuan-admin-portal/src/app/PortalLayout.tsx
egon-cola-xingyuan/egon-cola-xingyuan-admin-portal/src/auth/portalAuth.ts
egon-cola-xingyuan/egon-cola-xingyuan-admin-portal/vite.config.ts
egon-cola-xingyuan/egon-cola-xingyuan-admin-web-shared/README.md
egon-cola-xingyuan/egon-cola-xingyuan-admin-web-shared/src/auth/gatewayAuthClient.test.ts
egon-cola-xingyuan/egon-cola-xingyuan-admin-web-shared/src/auth/gatewayAuthClient.ts
egon-cola-xingyuan/egon-cola-xingyuan-admin-web-shared/src/index.ts
egon-cola-xingyuan/egon-cola-yuheng/yuheng-admin-web/README.md
egon-cola-xingyuan/egon-cola-yuheng/yuheng-admin-web/README.zh-CN.md
egon-cola-xingyuan/egon-cola-yuheng/yuheng-admin-web/e2e/mcp-control-plane.spec.ts
egon-cola-xingyuan/egon-cola-yuheng/yuheng-admin-web/e2e/yuheng-admin.spec.ts
egon-cola-xingyuan/egon-cola-yuheng/yuheng-admin-web/src/auth/AuthContext.tsx
egon-cola-xingyuan/egon-cola-yuheng/yuheng-admin-web/src/auth/LoginPage.test.tsx
egon-cola-xingyuan/egon-cola-yuheng/yuheng-admin-web/src/auth/LoginPage.tsx
egon-cola-xingyuan/egon-cola-yuheng/yuheng-admin-web/vite.config.ts
egon-cola-xingyuan/egon-cola-yuheng/yuheng-edge-gateway/src/main/java/top/egon/cola/component/yuheng/edge/config/EdgeSecurityConfiguration.java
egon-cola-xingyuan/egon-cola-yuheng/yuheng-edge-gateway/src/test/java/top/egon/cola/component/yuheng/edge/EdgeLoginContractTest.java
```

### Step 17 — 1dfd11960fa5c868766c1d50becea684a8a3ef27

feat(xingyuan): add oauth migration and local bff verification

```text
docs/egon/plan/2026-09-23-10-42-tianquan-oauth-oidc-implementation.md
docs/egon/spec/2026-10-08-10-18-oauth-current-code-alignment.md
docs/runbooks/unified-identity-oauth-client-tenant-cutover.md
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/dao/IdentityMigrationDAO.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/domain/command/OAuthMigrationCommand.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/domain/dto/IdentityMigrationRowDTO.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/domain/result/OAuthMigrationReportResult.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/service/OAuthMigrationService.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/service/impl/OAuthMigrationServiceImpl.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/support/migration/OAuthMigrationRunner.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/support/runtime/IdpPlatformConfiguration.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/resources/application-oauth-migration.yml
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/resources/mapper/oauth/IdentityMigrationDAO.xml
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/tianquan/shoubing/admin/support/migration/IdentityMigrationContractTest.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/tianquan/shoubing/admin/support/migration/OAuthMigrationRunnerTest.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/tianquan/shoubing/admin/support/migration/OAuthMigrationServiceTest.java
scripts/unified-xingyuan/lib/common.sh
scripts/unified-xingyuan/prepare-local-stack.sh
scripts/unified-xingyuan/start-local-stack.sh
scripts/unified-xingyuan/status-local-stack.sh
scripts/unified-xingyuan/test-direct-run-contract.sh
scripts/unified-xingyuan/test-live-frontend-login.sh
scripts/unified-xingyuan/verify-local-stack.sh
```

### Corrective c72de6f — c72de6fe1407430759a68acd76b8c06212720e0c

fix(oauth): select service registration for each tenant

```text
egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-starter/src/main/java/top/egon/cola/platform/tianquan/jianshen/starter/client/HttpTenantServiceTokenSupplier.java
egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-starter/src/test/java/top/egon/cola/platform/tianquan/jianshen/starter/client/HttpTenantServiceTokenSupplierTest.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/tianquan/shoubing/starter/autoconfigure/IdpStarterProperties.java
```

### Corrective e7889ee — e7889ee7bc31f743bfd17e78e68e164cf13b7450

fix(shoubing): keep issuer login outside gateway catalog

```text
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/controller/TenantLoginController.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/tianquan/shoubing/admin/openapi/IdpPublishedOpenApiContractTest.java
```

### Corrective cd2a350 — cd2a350d0309c53e936982c5a0f86efa0ad463db

fix(shoubing): validate migration checkpoint recovery state

```text
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/service/impl/OAuthMigrationServiceImpl.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/tianquan/shoubing/admin/support/migration/OAuthMigrationServiceTest.java
```

### Corrective 1f09ce0 — 1f09ce028ea534704ccd9acda71de891e7d844de

fix(shoubing): use accepted well-known jwks path

```text
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/config/AuthorizationServerConfiguration.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/config/OAuthPublicEndpointSettingsTest.java
```

### Corrective 076f34a — 076f34a8e36335fd3055f05e069fb15ea1263484

fix(shoubing): wire gateway to shared issuer verification

```text
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-gateway-adapter/lombok.config
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-gateway-adapter/pom.xml
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-gateway-adapter/src/main/java/top/egon/cola/platform/tianquan/shoubing/yuheng/autoconfigure/IdpGatewayAdapterAutoConfiguration.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-gateway-adapter/src/main/java/top/egon/cola/platform/tianquan/shoubing/yuheng/autoconfigure/IdpGatewayAdapterProperties.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-gateway-adapter/src/main/java/top/egon/cola/platform/tianquan/shoubing/yuheng/security/IdpGatewayJwtVerifier.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-gateway-adapter/src/main/java/top/egon/cola/platform/tianquan/shoubing/yuheng/security/IdpUserCookieCredentialExtractor.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-gateway-adapter/src/main/java/top/egon/cola/platform/tianquan/shoubing/yuheng/security/IdpUserCredentialRecoveryProvider.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-gateway-adapter/src/test/java/top/egon/cola/platform/tianquan/shoubing/yuheng/autoconfigure/IdpGatewayAdapterAutoConfigurationTest.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-gateway-adapter/src/test/java/top/egon/cola/platform/tianquan/shoubing/yuheng/security/IdpGatewayJwtVerifierTest.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-gateway-adapter/src/test/java/top/egon/cola/platform/tianquan/shoubing/yuheng/security/IdpGatewaySecurityProviderTest.java
```

### Corrective 5457346 — 54573468864018e733f4f7542424ef8f2b9e607a

fix(yuheng): strip browser cookies before engine forwarding

```text
egon-cola-xingyuan/egon-cola-yuheng/yuheng-edge-gateway/src/main/java/top/egon/cola/component/yuheng/edge/filter/TrustedIdentityHeaderFilter.java
egon-cola-xingyuan/egon-cola-yuheng/yuheng-edge-gateway/src/test/java/top/egon/cola/component/yuheng/edge/EdgeLoginContractTest.java
```

### Step 18 — 8b5a8fb5b9c360997621a6f29962f9269e2ac377

refactor(shoubing): retire legacy oauth protocol

```text
docs/egon/plan/2026-09-23-10-42-tianquan-oauth-oidc-implementation.md
docs/egon/spec/2026-10-08-10-18-oauth-current-code-alignment.md
docs/operations/unified-identity-mcp-local-runbook.md
docs/runbooks/unified-identity-local.md
docs/runbooks/unified-identity-oauth-client-tenant-cutover.md
egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin/src/main/resources/application-local.yml
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/pom.xml
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/audit/domain/pojo/IdentityAuditLogEntity.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/audit/repo/IdentityAuditLogRepository.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/identity/domain/pojo/IdentityCredentialEntity.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/identity/domain/pojo/IdentityUserEntity.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/identity/service/impl/IdentityUserServiceImpl.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/config/AuthorizationServerConfiguration.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/config/OAuthConfig.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/controller/InternalRefreshTokenController.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/controller/OAuthLoginController.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/controller/OAuthMetadataController.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/controller/OAuthStepUpController.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/controller/OAuthTokenController.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/controller/OAuthUserInfoController.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/controller/filter/LegacyOAuthRetirementFilter.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/domain/pojo/IdentityClientEntity.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/domain/pojo/IdentityClientRedirectUriEntity.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/domain/pojo/IdentityClientSecretEntity.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/repo/IdentityClientRedirectUriRepository.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/repo/IdentityClientRepository.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/repo/IdentityClientSecretRepository.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/repo/JpaOAuthClientStore.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/service/impl/ClientSecretBasicAuthenticator.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/resource/domain/pojo/IdentityClientResourceGrantEntity.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/resource/domain/pojo/IdentityResourceServerEntity.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/resource/repo/IdentityClientResourceGrantRepository.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/resource/repo/IdentityResourceServerRepository.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/resource/repo/JpaResourceServerStore.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/support/outbox/domain/pojo/IdentityOutboxEventEntity.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/support/outbox/repo/IdentityOutboxEventRepository.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/support/runtime/IdpPlatformConfiguration.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/support/security/IdpSecurityConfig.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/tenant/domain/pojo/IdentityTenantEntity.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/tenant/domain/pojo/IdentityTenantMembershipEntity.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/config/TokenConfig.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/domain/pojo/IdentitySigningKeyEntity.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/repo/IdentitySigningKeyRepository.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/repo/SubjectRevocationRefreshTokenStoreAdapter.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/resources/application-local.yml
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/tianquan/shoubing/admin/identity/service/impl/IdentityAuthenticationTransactionContractTest.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/tianquan/shoubing/admin/identity/service/impl/IdentityUserServiceImplTest.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/LegacyOAuthRetirementTest.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/TenantIssuerIsolationTest.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/config/OAuthConfigTest.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/controller/OAuthTokenClientCredentialsTest.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/tianquan/shoubing/admin/support/ddc/DdcPolicyConfigTest.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/tianquan/shoubing/admin/support/persistence/IdpPersistenceEntityContractTest.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/tianquan/shoubing/admin/support/runtime/SpringConstructorResolutionTest.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/tianquan/shoubing/admin/support/security/InternalRefreshTokenSecurityIT.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/tianquan/shoubing/admin/support/web/IdentityProfileControllerIT.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/tianquan/shoubing/admin/token/repo/SubjectRevocationRefreshTokenStoreAdapterTest.java
egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/resources/application-local.yml
egon-cola-xingyuan/egon-cola-yuheng/yuheng-admin/src/main/resources/application-local.yml
egon-cola-xingyuan/egon-cola-yuheng/yuheng-biz-gateway/src/main/resources/application-operations.yml
egon-cola-xingyuan/egon-cola-yuheng/yuheng-biz-gateway/src/main/resources/application.yml
egon-cola-xingyuan/egon-cola-yuheng/yuheng-mcp-gateway/src/main/resources/application-operations.yml
egon-cola-xingyuan/egon-cola-yuheng/yuheng-mcp-gateway/src/main/resources/application.yml
egon-cola-xingyuan/egon-cola-yuheng/yuheng-test/yuheng-test-http-provider/src/main/resources/application.yml
egon-cola-xingyuan/egon-cola-yuheng/yuheng-test/yuheng-test-mcp-provider/src/main/resources/application.yml
egon-cola-xingyuan/egon-cola-yuheng/yuheng-test/yuheng-test-tianquan-shoubing-backend/src/main/resources/application.yml
egon-cola-xingyuan/egon-cola-yuheng/yuheng-test/yuheng-test-webflux-http-provider/src/main/resources/application.yml
scripts/unified-identity-local.sh
scripts/unified-xingyuan/start-local-stack.sh
scripts/unified-xingyuan/test-direct-run-contract.sh
```

### Corrective 4d9104f — 4d9104f2817e051cacc2bbd99e24df4fdab88182

fix(shoubing): remove retired protocol configuration

```text
docs/egon/plan/2026-09-23-10-42-tianquan-oauth-oidc-implementation.md
docs/egon/spec/2026-10-08-10-18-oauth-current-code-alignment.md
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/resources/application-local.yml
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/resources/application.yml
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/config/OAuthPublicEndpointSettingsTest.java
scripts/unified-identity-local.sh
scripts/unified-xingyuan/test-direct-run-contract.sh
```

## 附录 B. Scratch与未提交工作

本任务在仓库`scripts/work/`没有遗留脚本。实际目录另有2026-09-23既有且被.gitignore忽略的`archetype-readme-frozen-tokens.md`，与OAuth任务无关，原样保留；原“仅.gitkeep”断言不准确，已更正。原生consent短Java复现已删除，行为与故障验证已进入持久JUnit。其余临时补丁/verification脚本处理清单见下方已完成处置表；日志和argv为证据保留，不是产品代码。

两个原有dirty Spec (`2026-09-22-17-14...` 与 `2026-09-23-10-57...`)保持原状，未加入任何实现提交。C8c与C9b合并为Step19的33声明路径随本报告同次交付。两个旧dirty Spec与上述无关忽略Markdown不纳入；无push/PR。

### 本轮结构与SQL核验

- strict Spec/Plan结构检查及skill资源完整性检查通过；最初C8c缺REQ-003 trace行导致strict Spec失败，补充trace后重新执行PASS。state/schema/API046已获2026-10-09用户明确批准；结构检查仍不是运行证据。
- `git diff --check`通过。比对执行起点所有 95 个已存在`db/**/*.sql`字节完全一致；V20261008_001内容与HEAD完全一致；只新增已批准V20261009_001及manifest追加项。
- 最终354 Admin/24 Edge/58消费者与静态门禁均新鲜通过；完整reactor历史失败仍单列，未再启动无guard测试。

### 临时脚本最终处置

下列本任务已识别脚本均删除；test-first/fixture逻辑已写入对应JUnit或`test-direct-run-contract.sh`的持久门禁，补丁生成器只是一次性编辑器。原生consent独立复现Java此前已删除，正式回合与故障测试已进入JUnit并通过。保留.log/.txt/.json证据，不留产品运行入口。

| 临时文件 | 处置 |
| --- | --- |
| `/tmp/egon-c14-work.py` | 删除；对应行为见已提交JUnit/static门禁，文档/patch builder仅一次性生成 |
| `/tmp/egon-c15-amend.py` | 删除；对应行为见已提交JUnit/static门禁，文档/patch builder仅一次性生成 |
| `/tmp/egon-c15-auto-test.py` | 删除；对应行为见已提交JUnit/static门禁，文档/patch builder仅一次性生成 |
| `/tmp/egon-c15-source.py` | 删除；对应行为见已提交JUnit/static门禁，文档/patch builder仅一次性生成 |
| `/tmp/egon-c15-test-first.py` | 删除；对应行为见已提交JUnit/static门禁，文档/patch builder仅一次性生成 |
| `/tmp/egon-c17-amend.py` | 删除；对应行为见已提交JUnit/static门禁，文档/patch builder仅一次性生成 |
| `/tmp/egon-c17-tests.py` | 删除；对应行为见已提交JUnit/static门禁，文档/patch builder仅一次性生成 |
| `/tmp/egon-c18-declare.py` | 删除；对应行为见已提交JUnit/static门禁，文档/patch builder仅一次性生成 |
| `/tmp/egon-c8b-amend.py` | 删除；对应行为见已提交JUnit/static门禁，文档/patch builder仅一次性生成 |
| `/tmp/egon-c8c-declare.py` | 删除；对应行为见已提交JUnit/static门禁，文档/patch builder仅一次性生成 |
| `/tmp/egon-c8c-edge-paths.java` | 删除；对应行为见已提交JUnit/static门禁，文档/patch builder仅一次性生成 |
| `/tmp/egon-c8c-oas-helpers.java` | 删除；对应行为见已提交JUnit/static门禁，文档/patch builder仅一次性生成 |
| `/tmp/egon-step18-adapt-tests.py` | 删除；对应行为见已提交JUnit/static门禁，文档/patch builder仅一次性生成 |
| `/tmp/egon-step18-amend-plan.py` | 删除；对应行为见已提交JUnit/static门禁，文档/patch builder仅一次性生成 |
| `/tmp/egon-step18-amend-spec.py` | 删除；对应行为见已提交JUnit/static门禁，文档/patch builder仅一次性生成 |
| `/tmp/egon-step18-commit.py` | 删除；对应行为见已提交JUnit/static门禁，文档/patch builder仅一次性生成 |
| `/tmp/egon-step18-consumer-gates.py` | 删除；对应行为见已提交JUnit/static门禁，文档/patch builder仅一次性生成 |
| `/tmp/egon-step18-create-red.py` | 删除；对应行为见已提交JUnit/static门禁，文档/patch builder仅一次性生成 |
| `/tmp/egon-step18-final-scope.py` | 删除；对应行为见已提交JUnit/static门禁，文档/patch builder仅一次性生成 |
| `/tmp/egon-step18-local-helpers.sh` | 删除；对应行为见已提交JUnit/static门禁，文档/patch builder仅一次性生成 |
| `/tmp/egon-step18-patch-local.py` | 删除；对应行为见已提交JUnit/static门禁，文档/patch builder仅一次性生成 |
| `/tmp/egon-step18-patch-upper.py` | 删除；对应行为见已提交JUnit/static门禁，文档/patch builder仅一次性生成 |
| `/tmp/egon-step18-persistence-test.py` | 删除；对应行为见已提交JUnit/static门禁，文档/patch builder仅一次性生成 |
| `/tmp/egon-step18-regression-amend.py` | 删除；对应行为见已提交JUnit/static门禁，文档/patch builder仅一次性生成 |
| `/tmp/egon-step18-sas-correction.py` | 删除；对应行为见已提交JUnit/static门禁，文档/patch builder仅一次性生成 |
| `/tmp/egon-step18-sas-test.py` | 删除；对应行为见已提交JUnit/static门禁，文档/patch builder仅一次性生成 |
| `/tmp/egon-step18-security.py` | 删除；对应行为见已提交JUnit/static门禁，文档/patch builder仅一次性生成 |
| `/tmp/egon-step18-source.py` | 删除；对应行为见已提交JUnit/static门禁，文档/patch builder仅一次性生成 |
| `/tmp/egon-step18-static-registry-fixture.sh` | 删除；对应行为见已提交JUnit/static门禁，文档/patch builder仅一次性生成 |
| `/tmp/egon-step18-static-test-adapt.py` | 删除；对应行为见已提交JUnit/static门禁，文档/patch builder仅一次性生成 |
| `/tmp/egon-write-consent-proposal.py` | 删除；对应行为见已提交JUnit/static门禁，文档/patch builder仅一次性生成 |
| `/tmp/egon-write-final-audit.py` | 删除；对应行为见已提交JUnit/static门禁，文档/patch builder仅一次性生成 |

## 附录 C. 实际成功日志的逐类结果

仅保留具有BUILD SUCCESS的日志中的逐类行，重叠类不累加覆盖。此表为执行证据，不能代替每项测试义务的全部场景证明。

| 日志 | 执行类 | Tests / Fail / Error / Skip |
| --- | --- | --- |
| `/tmp/egon-step18-admin-final.log` | `top.egon.cola.platform.tianquan.shoubing.admin.token.controller.SigningKeyControllerTest` | 3 / 0 / 0 / 0 |
| `/tmp/egon-step18-admin-final.log` | `top.egon.cola.platform.tianquan.shoubing.admin.token.service.impl.OAuthSigningKeyRotationServiceImplTest` | 3 / 0 / 0 / 0 |
| `/tmp/egon-step18-admin-final.log` | `top.egon.cola.platform.tianquan.shoubing.admin.token.service.impl.RsaPemKeyLoaderTest` | 1 / 0 / 0 / 0 |
| `/tmp/egon-step18-admin-final.log` | `top.egon.cola.platform.tianquan.shoubing.admin.token.service.impl.SigningKeyServiceImplTest` | 4 / 0 / 0 / 0 |
| `/tmp/egon-step18-admin-final.log` | `top.egon.cola.platform.tianquan.shoubing.admin.token.service.impl.OAuthSecurityProjectionComponentTest` | 9 / 0 / 0 / 0 |
| `/tmp/egon-step18-admin-final.log` | `top.egon.cola.platform.tianquan.shoubing.admin.token.service.impl.ClientCredentialsTokenServiceTest` | 6 / 0 / 0 / 0 |
| `/tmp/egon-step18-admin-final.log` | `top.egon.cola.platform.tianquan.shoubing.admin.token.repo.SubjectRevocationRefreshTokenStoreAdapterTest` | 3 / 0 / 0 / 0 |
| `/tmp/egon-step18-admin-final.log` | `top.egon.cola.platform.tianquan.shoubing.admin.token.TokenRevocationServiceTest` | 5 / 0 / 0 / 0 |
| `/tmp/egon-step18-admin-final.log` | `top.egon.cola.platform.tianquan.shoubing.admin.tenant.controller.TenantControllerTest` | 4 / 0 / 0 / 0 |
| `/tmp/egon-step18-admin-final.log` | `top.egon.cola.platform.tianquan.shoubing.admin.tenant.service.impl.TenantServiceImplTest` | 4 / 0 / 0 / 0 |
| `/tmp/egon-step18-admin-final.log` | `top.egon.cola.platform.tianquan.shoubing.admin.tenant.service.impl.TenantMembershipServiceImplTest` | 2 / 0 / 0 / 0 |
| `/tmp/egon-step18-admin-final.log` | `top.egon.cola.platform.tianquan.shoubing.admin.tenant.service.LocalTenantMembershipPortTest` | 3 / 0 / 0 / 0 |
| `/tmp/egon-step18-admin-final.log` | `top.egon.cola.platform.tianquan.shoubing.admin.oauth.repository.OAuthAuthorizationPersistenceContractTest` | 14 / 0 / 0 / 0 |
| `/tmp/egon-step18-admin-final.log` | `top.egon.cola.platform.tianquan.shoubing.admin.oauth.repository.OAuthRealmPersistenceContractTest` | 12 / 0 / 0 / 0 |
| `/tmp/egon-step18-admin-final.log` | `top.egon.cola.platform.tianquan.shoubing.admin.oauth.repository.OAuthResourcePersistenceContractTest` | 12 / 0 / 0 / 0 |
| `/tmp/egon-step18-admin-final.log` | `top.egon.cola.platform.tianquan.shoubing.admin.oauth.repository.OAuthSigningKeyPersistenceContractTest` | 15 / 0 / 0 / 0 |
| `/tmp/egon-step18-admin-final.log` | `top.egon.cola.platform.tianquan.shoubing.admin.oauth.LegacyOAuthRetirementTest` | 6 / 0 / 0 / 0 |
| `/tmp/egon-step18-admin-final.log` | `top.egon.cola.platform.tianquan.shoubing.admin.oauth.config.OAuthPublicEndpointSettingsTest` | 1 / 0 / 0 / 0 |
| `/tmp/egon-step18-admin-final.log` | `top.egon.cola.platform.tianquan.shoubing.admin.oauth.config.OAuthConfigTest` | 3 / 0 / 0 / 0 |
| `/tmp/egon-step18-admin-final.log` | `top.egon.cola.platform.tianquan.shoubing.admin.oauth.config.OAuthOidcProtocolContractTest` | 16 / 0 / 0 / 0 |
| `/tmp/egon-step18-admin-final.log` | `top.egon.cola.platform.tianquan.shoubing.admin.oauth.config.OAuthManagedSchemaContractTest` | 5 / 0 / 0 / 0 |
| `/tmp/egon-step18-admin-final.log` | `top.egon.cola.platform.tianquan.shoubing.admin.oauth.controller.OAuthTokenClientCredentialsTest` | 4 / 0 / 0 / 0 |
| `/tmp/egon-step18-admin-final.log` | `top.egon.cola.platform.tianquan.shoubing.admin.oauth.controller.OAuthClientControllerTest` | 3 / 0 / 0 / 0 |
| `/tmp/egon-step18-admin-final.log` | `top.egon.cola.platform.tianquan.shoubing.admin.oauth.TenantIssuerIsolationTest` | 11 / 0 / 0 / 0 |
| `/tmp/egon-step18-admin-final.log` | `top.egon.cola.platform.tianquan.shoubing.admin.oauth.service.impl.OAuthClientServiceImplTest` | 11 / 0 / 0 / 0 |
| `/tmp/egon-step18-admin-final.log` | `top.egon.cola.platform.tianquan.shoubing.admin.oauth.service.impl.OAuthManagementRealmResolverTest` | 4 / 0 / 0 / 0 |
| `/tmp/egon-step18-admin-final.log` | `top.egon.cola.platform.tianquan.shoubing.admin.oauth.service.OAuthRetentionServiceTest` | 5 / 0 / 0 / 0 |
| `/tmp/egon-step18-admin-final.log` | `top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.vo.OAuthUserTokenResultVOTest` | 1 / 0 / 0 / 0 |
| `/tmp/egon-step18-admin-final.log` | `top.egon.cola.platform.tianquan.shoubing.admin.identity.support.rpc.IdentityDirectoryRpcProviderTest` | 4 / 0 / 0 / 0 |
| `/tmp/egon-step18-admin-final.log` | `top.egon.cola.platform.tianquan.shoubing.admin.identity.service.impl.IdentityUserServiceImplTest` | 5 / 0 / 0 / 0 |
| `/tmp/egon-step18-admin-final.log` | `top.egon.cola.platform.tianquan.shoubing.admin.identity.service.impl.IdentityUserStateServiceImplTest` | 1 / 0 / 0 / 0 |
| `/tmp/egon-step18-admin-final.log` | `top.egon.cola.platform.tianquan.shoubing.admin.identity.service.impl.SpringPasswordHashAdapterTest` | 2 / 0 / 0 / 0 |
| `/tmp/egon-step18-admin-final.log` | `top.egon.cola.platform.tianquan.shoubing.admin.identity.service.impl.IdentityAuthenticationTransactionContractTest` | 3 / 0 / 0 / 0 |
| `/tmp/egon-step18-admin-final.log` | `top.egon.cola.platform.tianquan.shoubing.admin.identity.domain.pojo.IdentityEntityMappingTest` | 2 / 0 / 0 / 0 |
| `/tmp/egon-step18-admin-final.log` | `top.egon.cola.platform.tianquan.shoubing.admin.identity.repo.IdentityMpPersistenceContractTest` | 9 / 0 / 0 / 0 |
| `/tmp/egon-step18-admin-final.log` | `top.egon.cola.platform.tianquan.shoubing.admin.identity.repo.IdentityPersistenceAdapterTest` | 2 / 0 / 0 / 0 |
| `/tmp/egon-step18-admin-final.log` | `top.egon.cola.platform.tianquan.shoubing.admin.openapi.IdpPublishedOpenApiContractTest` | 1 / 0 / 0 / 0 |
| `/tmp/egon-step18-admin-final.log` | `top.egon.cola.platform.tianquan.shoubing.admin.audit.support.outbox.OAuthAuditOutboxDeliveryHandlerTest` | 3 / 0 / 0 / 0 |
| `/tmp/egon-step18-admin-final.log` | `top.egon.cola.platform.tianquan.shoubing.admin.audit.service.impl.OAuthAuditConsumerTest` | 6 / 0 / 0 / 0 |
| `/tmp/egon-step18-admin-final.log` | `top.egon.cola.platform.tianquan.shoubing.admin.audit.service.impl.IdentityAuditServiceImplTest` | 2 / 0 / 0 / 0 |
| `/tmp/egon-step18-admin-final.log` | `top.egon.cola.platform.tianquan.shoubing.admin.support.outbox.service.IdentityOutboxPublisherTest` | 3 / 0 / 0 / 0 |
| `/tmp/egon-step18-admin-final.log` | `top.egon.cola.platform.tianquan.shoubing.admin.support.bootstrap.IdpDevelopmentTenantBootstrapTest` | 4 / 0 / 0 / 0 |
| `/tmp/egon-step18-admin-final.log` | `top.egon.cola.platform.tianquan.shoubing.admin.support.bootstrap.IdpDevelopmentClientBootstrapTest` | 10 / 0 / 0 / 0 |
| `/tmp/egon-step18-admin-final.log` | `top.egon.cola.platform.tianquan.shoubing.admin.support.bootstrap.IdpBootstrapServiceTest` | 2 / 0 / 0 / 0 |
| `/tmp/egon-step18-admin-final.log` | `top.egon.cola.platform.tianquan.shoubing.admin.support.bootstrap.IdpBootstrapRunnerTest` | 5 / 0 / 0 / 0 |
| `/tmp/egon-step18-admin-final.log` | `top.egon.cola.platform.tianquan.shoubing.admin.support.security.IdpAdminSecurityIT` | 6 / 0 / 0 / 0 |
| `/tmp/egon-step18-admin-final.log` | `top.egon.cola.platform.tianquan.shoubing.admin.support.security.InternalRefreshTokenSecurityIT` | 1 / 0 / 0 / 0 |
| `/tmp/egon-step18-admin-final.log` | `top.egon.cola.platform.tianquan.shoubing.admin.support.web.IdentityProfileControllerIT` | 5 / 0 / 0 / 0 |
| `/tmp/egon-step18-admin-final.log` | `top.egon.cola.platform.tianquan.shoubing.admin.support.web.IdpHttpExceptionHandlerTest` | 2 / 0 / 0 / 0 |
| `/tmp/egon-step18-admin-final.log` | `top.egon.cola.platform.tianquan.shoubing.admin.support.tianshu.IdpDdcPolicyApplierTest` | 3 / 0 / 0 / 0 |
| `/tmp/egon-step18-admin-final.log` | `top.egon.cola.platform.tianquan.shoubing.admin.support.tianshu.DdcPolicyConfigTest` | 3 / 0 / 0 / 0 |
| `/tmp/egon-step18-admin-final.log` | `top.egon.cola.platform.tianquan.shoubing.admin.support.runtime.IdpRedisClientResolutionTest` | 1 / 0 / 0 / 0 |
| `/tmp/egon-step18-admin-final.log` | `top.egon.cola.platform.tianquan.shoubing.admin.support.runtime.IdpHttpProviderPublicationGateTest` | 4 / 0 / 0 / 0 |
| `/tmp/egon-step18-admin-final.log` | `top.egon.cola.platform.tianquan.shoubing.admin.support.runtime.SpringConstructorResolutionTest` | 7 / 0 / 0 / 0 |
| `/tmp/egon-step18-admin-final.log` | `top.egon.cola.platform.tianquan.shoubing.admin.support.persistence.IdpPersistenceEntityContractTest` | 3 / 0 / 0 / 0 |
| `/tmp/egon-step18-admin-final.log` | `top.egon.cola.platform.tianquan.shoubing.admin.support.migration.IdpClientTenantV5MigrationTest` | 3 / 0 / 0 / 0 |
| `/tmp/egon-step18-admin-final.log` | `top.egon.cola.platform.tianquan.shoubing.admin.support.migration.IdentityMigrationContractTest` | 4 / 0 / 0 / 0 |
| `/tmp/egon-step18-admin-final.log` | `top.egon.cola.platform.tianquan.shoubing.admin.support.migration.OAuthMigrationServiceTest` | 10 / 0 / 0 / 0 |
| `/tmp/egon-step18-admin-final.log` | `top.egon.cola.platform.tianquan.shoubing.admin.support.migration.OAuthMigrationRunnerTest` | 6 / 0 / 0 / 0 |
| `/tmp/egon-step18-admin-final.log` | `top.egon.cola.platform.tianquan.shoubing.admin.resource.controller.ResourceServerControllerTest` | 3 / 0 / 0 / 0 |
| `/tmp/egon-step18-admin-final.log` | `top.egon.cola.platform.tianquan.shoubing.admin.resource.controller.ClientResourceGrantControllerTest` | 4 / 0 / 0 / 0 |
| `/tmp/egon-step18-admin-final.log` | `top.egon.cola.platform.tianquan.shoubing.admin.resource.support.outbox.ResourceServerProjectionDeliveryHandlerTest` | 5 / 0 / 0 / 0 |
| `/tmp/egon-step18-admin-final.log` | `top.egon.cola.platform.tianquan.shoubing.admin.resource.support.outbox.TransactionalOutboxResourceServerEventAdapterTest` | 3 / 0 / 0 / 0 |
| `/tmp/egon-step18-admin-final.log` | `top.egon.cola.platform.tianquan.shoubing.admin.resource.support.outbox.DdcResourceServerLifecycleDeliveryHandlerTest` | 2 / 0 / 0 / 0 |
| `/tmp/egon-step18-admin-final.log` | `top.egon.cola.platform.tianquan.shoubing.admin.resource.service.impl.ResourceServerServiceImplTest` | 11 / 0 / 0 / 0 |
| `/tmp/egon-step18-admin-final.log` | `top.egon.cola.platform.tianquan.shoubing.admin.resource.service.ResourceServerProjectionServiceTest` | 4 / 0 / 0 / 0 |
| `/tmp/egon-step18-focus-final.log` | `top.egon.cola.platform.tianquan.shoubing.admin.token.repo.SubjectRevocationRefreshTokenStoreAdapterTest` | 3 / 0 / 0 / 0 |
| `/tmp/egon-step18-focus-final.log` | `top.egon.cola.platform.tianquan.shoubing.admin.oauth.LegacyOAuthRetirementTest` | 6 / 0 / 0 / 0 |
| `/tmp/egon-step18-focus-final.log` | `top.egon.cola.platform.tianquan.shoubing.admin.oauth.config.OAuthConfigTest` | 3 / 0 / 0 / 0 |
| `/tmp/egon-step18-focus-final.log` | `top.egon.cola.platform.tianquan.shoubing.admin.oauth.config.OAuthOidcProtocolContractTest` | 16 / 0 / 0 / 0 |
| `/tmp/egon-step18-focus-final.log` | `top.egon.cola.platform.tianquan.shoubing.admin.oauth.controller.OAuthTokenClientCredentialsTest` | 4 / 0 / 0 / 0 |
| `/tmp/egon-step18-focus-final.log` | `top.egon.cola.platform.tianquan.shoubing.admin.identity.service.impl.IdentityUserServiceImplTest` | 5 / 0 / 0 / 0 |
| `/tmp/egon-step18-focus-final.log` | `top.egon.cola.platform.tianquan.shoubing.admin.identity.service.impl.IdentityAuthenticationTransactionContractTest` | 3 / 0 / 0 / 0 |
| `/tmp/egon-step18-focus-final.log` | `top.egon.cola.platform.tianquan.shoubing.admin.support.security.InternalRefreshTokenSecurityIT` | 1 / 0 / 0 / 0 |
| `/tmp/egon-step18-focus-final.log` | `top.egon.cola.platform.tianquan.shoubing.admin.support.web.IdentityProfileControllerIT` | 5 / 0 / 0 / 0 |
| `/tmp/egon-step18-focus-final.log` | `top.egon.cola.platform.tianquan.shoubing.admin.support.runtime.SpringConstructorResolutionTest` | 7 / 0 / 0 / 0 |
| `/tmp/egon-step18-focus-final.log` | `top.egon.cola.platform.tianquan.shoubing.admin.support.persistence.IdpPersistenceEntityContractTest` | 3 / 0 / 0 / 0 |
| `/tmp/egon-c8c-admin-green3.log` | `top.egon.cola.platform.tianquan.shoubing.admin.oauth.repository.OAuthAuthorizationPersistenceContractTest` | 14 / 0 / 0 / 0 |
| `/tmp/egon-c8c-admin-green3.log` | `top.egon.cola.platform.tianquan.shoubing.admin.oauth.config.OAuthOidcProtocolContractTest` | 16 / 0 / 0 / 0 |
| `/tmp/egon-c8c-admin-green3.log` | `top.egon.cola.platform.tianquan.shoubing.admin.oauth.config.OAuthProtocolOpenApiContractTest` | 1 / 0 / 0 / 0 |
| `/tmp/egon-c8c-admin-green3.log` | `top.egon.cola.platform.tianquan.shoubing.admin.oauth.controller.OAuthTokenClientCredentialsTest` | 4 / 0 / 0 / 0 |
| `/tmp/egon-c8c-admin-green3.log` | `top.egon.cola.platform.tianquan.shoubing.admin.oauth.TenantIssuerIsolationTest` | 11 / 0 / 0 / 0 |
| `/tmp/egon-c8c-admin-green3.log` | `top.egon.cola.platform.tianquan.shoubing.admin.openapi.IdpPublishedOpenApiContractTest` | 1 / 0 / 0 / 0 |
| `/tmp/egon-c8c-edge-green2.log` | `top.egon.cola.component.yuheng.edge.EdgeLoginContractTest` | 24 / 0 / 0 / 0 |
| `/tmp/egon-c15-green2.log` | `top.egon.cola.platform.tianquan.shoubing.starter.security.MultiIssuerResourceServerTest` | 11 / 0 / 0 / 0 |
| `/tmp/egon-c15-green2.log` | `top.egon.cola.platform.tianquan.shoubing.yuheng.security.IdpGatewaySecurityProviderTest` | 9 / 0 / 0 / 0 |
| `/tmp/egon-c15-green2.log` | `top.egon.cola.platform.tianquan.shoubing.yuheng.security.GatewayResourceServerResolverTest` | 2 / 0 / 0 / 0 |
| `/tmp/egon-c15-green2.log` | `top.egon.cola.platform.tianquan.shoubing.yuheng.security.IdpGatewayJwtVerifierTest` | 1 / 0 / 0 / 0 |
| `/tmp/egon-c15-green2.log` | `top.egon.cola.platform.tianquan.shoubing.yuheng.autoconfigure.IdpGatewayAdapterAutoConfigurationTest` | 5 / 0 / 0 / 0 |
| `/tmp/egon-c17-final-green.log` | `top.egon.cola.platform.tianquan.shoubing.admin.support.migration.IdentityMigrationContractTest` | 4 / 0 / 0 / 0 |
| `/tmp/egon-c17-final-green.log` | `top.egon.cola.platform.tianquan.shoubing.admin.support.migration.OAuthMigrationServiceTest` | 10 / 0 / 0 / 0 |
| `/tmp/egon-c17-final-green.log` | `top.egon.cola.platform.tianquan.shoubing.admin.support.migration.OAuthMigrationRunnerTest` | 6 / 0 / 0 / 0 |
| `/tmp/egon-step12-green-final3.log` | `top.egon.cola.platform.tianquan.shoubing.starter.security.MultiIssuerResourceServerTest` | 10 / 0 / 0 / 0 |
| `/tmp/egon-step12-green-final3.log` | `top.egon.cola.platform.tianquan.shoubing.starter.security.IdpJwtVerifierTest` | 6 / 0 / 0 / 0 |
| `/tmp/egon-step12-green-final3.log` | `top.egon.cola.platform.tianquan.shoubing.starter.security.IdpBearerAuthenticationFilterTest` | 5 / 0 / 0 / 0 |
| `/tmp/egon-step12-green-final3.log` | `top.egon.cola.platform.tianquan.shoubing.starter.security.rpc.IdpRpcBearerServerInterceptorTest` | 5 / 0 / 0 / 0 |
| `/tmp/egon-step12-green-final3.log` | `top.egon.cola.platform.tianquan.shoubing.starter.autoconfigure.IdpStarterAutoConfigurationTest` | 7 / 0 / 0 / 0 |
| `/tmp/egon-step12-green-final3.log` | `top.egon.cola.platform.tianquan.shoubing.admin.oauth.config.OAuthOidcProtocolContractTest` | 16 / 0 / 0 / 0 |
| `/tmp/egon-c13-green.log` | `top.egon.cola.platform.tianquan.shoubing.starter.autoconfigure.IdpStarterAutoConfigurationTest` | 8 / 0 / 0 / 0 |
| `/tmp/egon-c13-green.log` | `top.egon.cola.platform.tianquan.shoubing.starter.client.IdpServiceOAuth2ClientTest` | 14 / 0 / 0 / 0 |
| `/tmp/egon-c13-green.log` | `top.egon.cola.platform.tianquan.jianshen.starter.client.HttpTenantServiceTokenSupplierTest` | 5 / 0 / 0 / 0 |
| `/tmp/egon-step18-rpc-final.log` | `top.egon.cola.component.rpc.tianshu.security.DdcRpcClientInterceptorFactoryTest` | 3 / 0 / 0 / 0 |
| `/tmp/egon-step18-cancel-final.log` | `top.egon.cola.component.yuheng.runtime.transport.service.GatewayCancellationTest` | 2 / 0 / 0 / 0 |
| `/tmp/egon-step18-mcp-final.log` | `top.egon.cola.component.yuheng.mcp.engine.bootstrap.McpGatewayManagementSecurityConfigurationTest` | 1 / 0 / 0 / 0 |
| `/tmp/egon-step18-mcp-final.log` | `top.egon.cola.component.yuheng.mcp.engine.mcp.service.McpTransportIntegrationTest` | 2 / 0 / 0 / 0 |
| `/tmp/egon-c18-green.log` | `top.egon.cola.platform.tianquan.shoubing.admin.oauth.config.OAuthPublicEndpointSettingsTest` | 2 / 0 / 0 / 0 |
| `/tmp/egon-c18-green.log` | `top.egon.cola.platform.tianquan.shoubing.admin.oauth.config.OAuthConfigTest` | 3 / 0 / 0 / 0 |
| `/tmp/egon-c18-green.log` | `top.egon.cola.platform.tianquan.shoubing.admin.oauth.TenantIssuerIsolationTest` | 11 / 0 / 0 / 0 |
| `/tmp/egon-c18-green.log` | `top.egon.cola.platform.tianquan.shoubing.admin.support.persistence.IdpPersistenceEntityContractTest` | 3 / 0 / 0 / 0 |

### 最后核验与非通过结果补充

strict Spec/Plan最终复跑均exit0，`git diff --check`exit0。精确工作树14路径：C8c10 + 原有dirty Spec2 + 提案/审计报告2；staged为空。报告的所有46 REQ、53接口、82测试和17 MC/10 Rule均有各自一行状态。历史Step16 Yuheng全套`npm test`曾等待75秒后中断，只对已完成的targeted AdminLayout等测试和package typecheck记录PASS；Yuheng全套前端回归不声称通过。

## 附录 D. Step19精确交付路径

一个逻辑任务、一个聚焦提交；提交hash不自嵌入文档，由最终交付和本报告同题commit的git log解析。最终工作树只留两个既有dirty Spec；ignored archetype Markdown保留不入提交。

```text
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/OAuthConsentRoundTripTest.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/TenantIssuerIsolationTest.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/config/OAuthOidcProtocolContractTest.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/config/OAuthProtocolOpenApiContractTest.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/controller/OAuthTokenClientCredentialsTest.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/repository/OAuthAuthorizationPersistenceContractTest.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/tianquan/shoubing/admin/openapi/IdpPublishedOpenApiContractTest.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/tianquan/shoubing/admin/support/runtime/SpringConstructorResolutionTest.java
egon-cola-xingyuan/egon-cola-yuheng/yuheng-edge-gateway/src/test/java/top/egon/cola/component/yuheng/edge/EdgeLoginContractTest.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/domain/bo/OAuthAuthorizationAttributesBO.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/domain/bo/OAuthPendingConsentBO.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/domain/bo/SignedTokenClaimsBO.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/domain/enums/OAuthTokenTypeEnum.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/config/AuthorizationServerConfiguration.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/config/OAuthProtocolOpenApiConfiguration.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/controller/OAuthConsentController.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/controller/filter/TenantLoginAuthenticationFilter.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/domain/converter/OAuthAuthorizationConverter.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/domain/converter/SasAuthorizationConverter.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/service/impl/OAuthTokenIssueServiceImpl.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/service/impl/TenantAuthorizationServiceImpl.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/service/impl/TransactionalOAuthAuthenticationProvider.java
egon-cola-xingyuan/egon-cola-yuheng/yuheng-edge-gateway/src/main/java/top/egon/cola/component/yuheng/edge/config/EdgeSecurityConfiguration.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/resources/db/egon-mp/V20261009_001__support_oauth_consent_state.sql
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/resources/db/egon-mp/repository-manifest.json
docs/egon/plan/2026-09-23-10-42-tianquan-oauth-oidc-implementation.md
docs/egon/reports/2026-10-09-oauth-consent-correction-proposal.md
docs/egon/reports/2026-10-09-tianquan-oauth-oidc-execution-audit.md
docs/egon/spec/2026-10-08-10-18-oauth-current-code-alignment.md
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/repository/TenantRegisteredClientRepository.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/service/impl/TenantConsentServiceImpl.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/service/impl/TenantJwkSourceComponent.java
egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/config/OAuthManagedSchemaContractTest.java
```

### Step19一次性编辑器处置

以下九个`/tmp`临时编辑器已删除：egon-c9b-declare.py、lock-step.py、test-first.py、models.py、sas-adapter.py、services.py、provider.py、controller.py、config.py（后八项共享egon-c9b-前缀）。测试/验证逻辑进入持久JUnit或原static suite，日志/argv/范围SHA清单保留为证据。Fixture @AfterAll关闭其临时KEK/key目录。`scripts/work/archetype-readme-frozen-tokens.md`来源属于既有无关工作，不删除、不提交；没有本任务遗留脚本。

## 附录 E. Step20精确交付路径与Scratch处置

Step20仅以下19路径；完整hash在同题提交`fix(shoubing): preserve explicit user grant scopes`与最终交付读取。原2份dirty历史Spec不在清单内。

| # | 操作 | 路径 |
| --- | --- | --- |
| 1 | MODIFY | `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-core/src/test/java/top/egon/cola/platform/tianquan/shoubing/core/resource/ResourceServerPolicyTest.java` |
| 2 | MODIFY | `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/tianquan/shoubing/admin/resource/service/impl/ResourceServerServiceImplTest.java` |
| 3 | MODIFY | `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/tianquan/shoubing/admin/resource/controller/ClientResourceGrantControllerTest.java` |
| 4 | MODIFY | `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/service/impl/OAuthClientServiceImplTest.java` |
| 5 | MODIFY | `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/tianquan/shoubing/admin/support/migration/OAuthMigrationServiceTest.java` |
| 6 | MODIFY | `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/OAuthConsentRoundTripTest.java` |
| 7 | MODIFY | `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin-web/src/features/resource-grants/ClientResourceGrantPage.test.tsx` |
| 8 | MODIFY | `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-core/src/main/java/top/egon/cola/platform/tianquan/shoubing/core/resource/ClientResourceGrant.java` |
| 9 | MODIFY | `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/resource/domain/dto/UpsertClientResourceGrantDTO.java` |
| 10 | MODIFY | `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/resource/service/impl/ResourceServerServiceImpl.java` |
| 11 | MODIFY | `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/service/impl/OAuthMigrationServiceImpl.java` |
| 12 | MODIFY | `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin-web/src/features/resource-grants/ClientResourceGrantPage.tsx` |
| 13 | MODIFY | `docs/egon/spec/2026-10-08-10-18-oauth-current-code-alignment.md` |
| 14 | MODIFY | `docs/egon/plan/2026-09-23-10-42-tianquan-oauth-oidc-implementation.md` |
| 15 | MODIFY | `docs/egon/reports/2026-10-09-oauth-consent-correction-proposal.md` |
| 16 | MODIFY | `docs/egon/reports/2026-10-09-tianquan-oauth-oidc-execution-audit.md` |
| 17 | MODIFY | `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/tianquan/shoubing/admin/support/runtime/SpringConstructorResolutionTest.java` |
| 18 | MODIFY | `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin-web/src/app/App.test.tsx` |
| 19 | MODIFY | `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin-web/src/features/clients/ClientListPage.test.tsx` |

三个额外路径为Spring构造Validator fixture、App canonical realm/既有404合同fixture、ClientList等待平台返回数据，按Plan C10-01/02在编辑前先声明；没有额外生产接口/模块/数据语义。复核迁移恢复的生产阶段原本正确，仅source Page fixture修正，不存在阶段纠正提交。

Scratch outcome：Core/Admin/JUnit/Vitest行为验证保留到声明测试文件；本Step没有创建临时编辑脚本。隔离起点HEAD的TemporaryDirectory已自动删除；`/tmp/egon-c10-ui-source-tsconfig.json`与`/tmp/egon-c10-ui-source-vitest.config.mts`在检查完成后删除，生成说明保留V-C10-TYPE。argv/lock/SHA JSON与完整日志只作为本机证据，不提交。`scripts/work/archetype-readme-frozen-tokens.md`是执行前存在的ignored用户文档，SHA `7b0a5debf462ddfcd476b37e6827efa3ac9ce4691d6e7143412ae2b9042c55ec`保持不变；scripts/work没有本任务未处置脚本。

交付后只应剩原2份dirty历史Spec，SHA见scope-proof/原lock；runtime/数据库/migration/Redis/browser/GUI/deploy/push/PR均未执行。本报告未将旧安装包typecheck、完整reactor历史失败或操作偏差重新命名为PASS。
