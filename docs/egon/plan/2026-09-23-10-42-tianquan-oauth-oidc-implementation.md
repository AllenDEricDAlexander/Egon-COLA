# 天权多租户 OAuth2 / OIDC 实施 Plan

| Field | Value |
| --- | --- |
| Document | `2026-09-23-10-42-tianquan-oauth-oidc-implementation.md` |
| Template Version | `4` |
| Status | `Blocked` |
| Created | `2026-09-23 10:42 Asia/Shanghai` |
| Updated | `2026-09-23 10:58 Asia/Shanghai` |
| Owner | `mario` |
| Repository | `Egon-COLA` |
| Scope | 授柄SAS/认证MP与受管DDL、SCG、Starter、玉衡/鉴神/天枢准入、五端认证前端、切流 |
| Source Requirement | 2026-09-22 OAuth/OIDC多租户请求；2026-09-23用户确认Spec并调用egon-coding-writing-plan |
| Baseline Revision | `main@4f09f8ed2e38b291051559892318761cc0d6d873`；5.4.1发布版本，其他未跟踪__pycache__不属本任务 |
| Implements Spec | [天权多租户 OAuth2 / OIDC 与 SCG 入口融合设计](../spec/2026-09-22-17-14-tianquan-multitenant-oauth-oidc-design.md) |
| Spec Status | `Accepted` |
| Spec Revision | `2026-09-23 10:51 Asia/Shanghai`；本轮仅更新其状态和Related Plans关系元数据 |
| Effective Specs | [本Plan主Spec](../spec/2026-09-22-17-14-tianquan-multitenant-oauth-oidc-design.md)；[无Session身份设计未被替换的鉴神授权§12](../../superpowers/specs/2026-08-13-unified-identity-stateless-jwt-session-removal-design.md) |
| Depends On Plans | `None` |
| Supersedes | `None` |
| Superseded By | `None` |
| Related Plans | `None`；[待确认Grant列表修订](../spec/2026-09-23-10-57-oauth-grant-list-contract-amendment.md)是本Plan恢复前的规范依赖 |

## 1. Summary

实施主Spec的19项REQ，按上述17个独立可验证的语义Step推进。先锁定已批准依赖和受管新schema，再接OAuth持久化与SAS协议，后接资源服务器/机器客户端、SCG和旧消费链，最后迁移五端前端与切流并退役旧协议。每Step都有RED/GREEN、精确文件、运行命令和路径限定commit；生产级完整性仍要由用户启动环境后的PostgreSQL/Redis/TLS/浏览器验收证明。本轮只写Plan。当前核查发现主Spec缺少现有Grant页面调用的公开GET；已另写Review修订，待用户确认前此Plan状态为Blocked，不能据此开始实现。

## 2. Target Spec and Effective Design

### 2.1 Primary target

- Path: [主Spec](../spec/2026-09-22-17-14-tianquan-multitenant-oauth-oidc-design.md)。
- Status: Accepted；用户2026-09-23回复“确认”并调用本Plan技能。此确认不是Plan审批。
- Revision: Updated 2026-09-23 10:51 Asia/Shanghai（Related Plans 元数据回链）；规范性设计与用户确认时一致；Git基线此文档已在现有main内，本Plan单独产物。

### 2.2 Effective Spec set

| Role | Spec/link | Status/revision | Effective sections | Why included |
| --- | --- | --- | --- | --- |
| Primary | [天权多租户 OAuth/OIDC](../spec/2026-09-22-17-14-tianquan-multitenant-oauth-oidc-design.md) | Accepted 2026-09-23 | §1–§20全部 | 唯一代码设计主权威 |
| Preserved predecessor | [无Session身份设计](../../superpowers/specs/2026-08-13-unified-identity-stateless-jwt-session-removal-design.md) | 2026-08-14用户确认；主Spec部分替换 | §12鉴神角色激活/最小授权主体/授权快照中与新issuer无冲突部分 | 主Spec§16保留现有无RBAC Session授权模型 |

### 2.3 Superseded or excluded content

[Grant列表接口修订](../spec/2026-09-23-10-57-oauth-grant-list-contract-amendment.md)当前为Review，尚未成为Effective Specs；其API-045不能在本Plan中视作已获批。用户确认后须先将修订标Accepted，再把其精确§4/§7–§16纳入本Header和Step 9、Step 15及§10；此处不把缺漏藏为局部命名澄清。

主Spec `Supersedes` 旧无Session设计§7–§11、§13–§16的旧USER单issuer/AT-RT Cookie/固定PEM/无BFF契约。主Spec `Amends` 2026-08-21 OAuth Client/tenant及2026-09-05 Gateway种子中的单issuer/密钥/旧存储段落，未获批的旧方案不额外成为本Plan规范。RBAC3后续Accepted资源授权Spec仅提供不修改细粒度授权的上下文，不据它扩大本Plan权限表范围。无接受的更晚OAuth amendment。

## 3. Effective Requirements and Acceptance

| Requirement | Source Spec section | Effective statement | Observable acceptance | Implementation impact |
| --- | --- | --- | --- | --- |
| REQ-001 | 主Spec §4 | 授柄使用 SAS，保持鉴神/天枢/玉衡职责 | 只有授柄签发令牌，鉴神不保存密码/私钥 | Step 1; Step 7; Step 14; Step 17 |
| REQ-002 | 主Spec §4 | 路径 issuer 隔离租户客户端、授权状态、consent、JWK | A 租户 code/client/key 对 B 不可用；未知 issuer 无出站请求 | Step 2; Step 3; Step 7; Step 9; Step 11 |
| REQ-003 | 主Spec §4 | 第三方 Code 强制 S256 PKCE、redirect 白名单 | 缺 PKCE/plain/错 verifier/URI 通配符/重复 code 均拒绝 | Step 5; Step 8; Step 13; Step 15 |
| REQ-004 | 主Spec §4 | OIDC discovery、ID Token、UserInfo | iss/aud/nonce 校验、UserInfo sub 一致，ID Token 不能访问业务 | Step 5; Step 7; Step 8; Step 13; Step 15 |
| REQ-005 | 主Spec §4 | 每微服务独立 client_id 与最小 scope | SERVICE 无 refresh_token/ID Token；scope/resource 超授权拒绝 | Step 3; Step 4; Step 8; Step 9; Step 12; Step 14 |
| REQ-006 | 主Spec §4 | RS256 本地验签及 aud/时间/主体校验 | 有缓存公钥时不逐请求调用授权服务器；错 aud/alg 拒绝 | Step 11; Step 13; Step 14; Step 17 |
| REQ-007 | 主Spec §4 | tenant_id 可信上下文与 finally 清理 | ThreadLocal、MDC、异步取消/异常均无跨请求污染 | Step 11; Step 14 |
| REQ-008 | 主Spec §4 | SQL 级租户隔离 | 两租户相同业务 ID、批处理/自定义 SQL/逻辑删除不能串租户 | Step 2; Step 4; Step 11 |
| REQ-009 | 主Spec §4 | WebClient 自动令牌与临期重获 | 键隔离、同键并发仅一次获取、访问令牌 300–600 秒 | Step 12 |
| REQ-010 | 主Spec §4 | SCG OAuth2 Client/BFF 入口 | 浏览器只持 HttpOnly 会话 cookie；下游仍自行验签 | Step 1; Step 13; Step 14; Step 15; Step 17 |
| REQ-011 | 主Spec §4 | HTTPS 与 secret 哈希 | 生产 HTTP/明文 RPC 配置拒绝；新 client secret bcrypt | Step 1; Step 3; Step 8; Step 9; Step 12; Step 13; Step 16 |
| REQ-012 | 主Spec §4 | 即时撤销 | 撤销返回成功后新鉴权拒绝，Redis 不可用返回503，在途请求不取消 | Step 5; Step 6; Step 10; Step 11; Step 13; Step 16; Step 17 |
| REQ-013 | 主Spec §4 | 密钥定期轮换 | 多键重叠，旧 token 可验证至到期，泄露键可紧急拒绝 | Step 6; Step 7; Step 9; Step 10 |
| REQ-014 | 主Spec §4 | token 申请/使用/撤销全量脱敏审计 | 不记录原 token/secret/code/password，投递有恢复证据 | Step 2; Step 6; Step 10 |
| REQ-015 | 主Spec §4 | 指标与告警 | JWT 延迟、缓存命中、失败授权、撤销依赖/投递积压可观测 | Step 6; Step 10 |
| REQ-016 | 主Spec §4 | 保留隔离 PLATFORM 控制面 | `/platform` 只服务控制面；业务 tenant token 必含 tenant_id | Step 4; Step 7; Step 10; Step 11; Step 12; Step 14; Step 16 |
| REQ-017 | 主Spec §4 | 旧链迁移且保留业务数据 | 用户/tenant/client/grant 映射可审计；切换/回退界限明确 | Step 2; Step 4; Step 5; Step 9; Step 15; Step 16; Step 17 |
| REQ-018 | 主Spec §4 | 按最新 Java/CQE/MP/DDL 规范 | §6.2、§20 每项证据闭合；保留传统业务分包 | Step 1; Step 2; Step 3; Step 16 |
| REQ-019 | 主Spec §4 | 仅 Spec、禁 generator | git diff 只有本 Spec；无 Plan/代码/运行态动作 | Step 16; Step 17 |

## 4. Implementation Strategy and Dependency Order

### 4.1 Ordered strategy

技术合同依赖：Maven受控依赖→一个受管SQL与NATIVE物理落点→十一张认证表的类型/Mapper/Repository→SAS多issuer/SPI→Code/PKCE/OIDC及管理操作→revocation/audit→resource-server与机器客户端→SCG/BFF→玉衡/鉴神/天枢→Admin Web→逐tenant导入切流→旧协议410。后一阶段在前一阶段测试通过并提交后进入；跨Service用户业务数据库不纳入认证DB事务。缺少真实PostgreSQL/Redis proof的Step不得以unit通过宣称上线。

### 4.2 Test-first strategy

Step 1只有POM/空进程编译前置，结构验证不触发用户行为；其余每Step第一个test或编译前置后的第一个test为RED。RED必须因目标能力缺失/行为不匹配失败，不能是测试类拼错、依赖未下载或服务未启动。随后写最小业务类型/Mapper/Service/安全链/消费者让focused GREEN，再跑module+上下游合同。数据库脚本先静态契约RED；真PostgreSQL执行留用户运行环境验收。

### 4.3 Sequential and parallel boundaries

| Step | Depends on | May run in parallel with | Must not overlap with | Reason |
| --- | --- | --- | --- | --- |
| Step 1 | None | None（计划按单个责任人串行；前后端源码可读但不并行写） | 本Step所有Commit paths与其他Step互斥 | Maven reactor 能识别独立 SCG 模块；授柄SAS/MP/Session依赖及同一版本线受控 |
| Step 2 | Step 1 | None（计划按单个责任人串行；前后端源码可读但不并行写） | 本Step所有Commit paths与其他Step互斥 | 一个新SQL版本定义11表及约束；NATIVE SINGLE精准落在tianquan_oauth |
| Step 3 | Step 2 | None（计划按单个责任人串行；前后端源码可读但不并行写） | 本Step所有Commit paths与其他Step互斥 | 这组认证事实的Repository/Mapper能按同realm/tenant/active/version读写 |
| Step 4 | Step 3 | None（计划按单个责任人串行；前后端源码可读但不并行写） | 本Step所有Commit paths与其他Step互斥 | 这组认证事实的Repository/Mapper能按同realm/tenant/active/version读写 |
| Step 5 | Step 4 | None（计划按单个责任人串行；前后端源码可读但不并行写） | 本Step所有Commit paths与其他Step互斥 | 这组认证事实的Repository/Mapper能按同realm/tenant/active/version读写 |
| Step 6 | Step 5 | None（计划按单个责任人串行；前后端源码可读但不并行写） | 本Step所有Commit paths与其他Step互斥 | 这组认证事实的Repository/Mapper能按同realm/tenant/active/version读写 |
| Step 7 | Step 6 | None（计划按单个责任人串行；前后端源码可读但不并行写） | 本Step所有Commit paths与其他Step互斥 | SAS多issuer精确registry、JWK与protocol discovery逐租户独立 |
| Step 8 | Step 7 | None（计划按单个责任人串行；前后端源码可读但不并行写） | 本Step所有Commit paths与其他Step互斥 | 标准Code/PKCE/consent/login和机器Client Credentials签发保持scope/audience界限 |
| Step 9 | Step 8 | None（计划按单个责任人串行；前后端源码可读但不并行写） | 本Step所有Commit paths与其他Step互斥 | 现有管理URL按realm管理Client/Grant/Key；secret仅bcrypt一次性显示 |
| Step 10 | Step 9 | None（计划按单个责任人串行；前后端源码可读但不并行写） | 本Step所有Commit paths与其他Step互斥 | 成功撤销后新鉴权拒绝；Redis故障503；审计持久可恢复 |
| Step 11 | Step 10 | None（计划按单个责任人串行；前后端源码可读但不并行写） | 本Step所有Commit paths与其他Step互斥 | 所有受保护资源请求本地RS256后共享撤销检查，Servlet ThreadLocal/MDC最终清理 |
| Step 12 | Step 11 | None（计划按单个责任人串行；前后端源码可读但不并行写） | 本Step所有Commit paths与其他Step互斥 | 出站调用按issuer/client/凭据版/aud/tenant/scope隔离并临期重获，无client_credentials RT |
| Step 13 | Step 12 | None（计划按单个责任人串行；前后端源码可读但不并行写） | 本Step所有Commit paths与其他Step互斥 | 浏览器仅持HttpOnly opaque会话、SCG代码换token、玉衡路由原样转发Bearer |
| Step 14 | Step 13 | None（计划按单个责任人串行；前后端源码可读但不并行写） | 本Step所有Commit paths与其他Step互斥 | 平台控制SERVICE token与USER授权在现有路由/鉴神/天枢链正确准入 |
| Step 15 | Step 14 | None（计划按单个责任人串行；前后端源码可读但不并行写） | 本Step所有Commit paths与其他Step互斥 | Portal/四Admin前端按新BFF/Consent/Session契约运行且不保存token |
| Step 16 | Step 14 | None（计划按单个责任人串行；前后端源码可读但不并行写） | 本Step所有Commit paths与其他Step互斥 | 逐租户导入/凭证轮换/切流报告可审，脚本只准备用户发起的运行验收 |
| Step 17 | Step 15 | None（计划按单个责任人串行；前后端源码可读但不并行写） | 本Step所有Commit paths与其他Step互斥 | 新部署旧OAuth根协议返回410且无双签发链；所有原消费者已迁出 |

### 4.4 Commit boundaries

每Step一个语义commit，属17个可独立验收的逻辑子任务；`git add --`仅列此Step的精确文件，`git commit`不得携带本轮开始前的__pycache__或其他并行工作。一个路径若确有后续有意修改（如AuthorizationServerConfiguration先配SPIs再配Provider），Step注明上一次提交的稳定状态和后次扩展的单一语义，不把旧Step重写成未完成。若用户以后仅授权文档/Plan，**本轮不执行任何Step或commit**。

### 4.5 Spec Simplicity and Implementation-necessity Audit

阻断发现：现有`ClientResourceGrantPage.tsx#grantQuery`独立读取`GET /api/v1/tianquan-shoubing/clients/{clientId}/resources`，后端`ClientResourceGrantController`只有PUT/DELETE/批量POST，已确认主Spec API-001–044也无该GET。不能让Step 9只做写操作而Step 15沿用404页面。新Query必要性、完整wire和索引路径由修订Spec API-045定义；其获批前本Plan不把它编为已批准接口。

| Spec element | Spec necessity verdict/section | Current repository evidence | Direct/reuse alternative | Interaction/implementation cost | Plan decision |
| --- | --- | --- | --- | --- | --- |
| SAS/OIDC | Add §7.0 | 旧OAuthTokenController只有client_credentials/custom refresh | 继续手写协议不满足Code/OIDC | 授权状态与Filter/SPI适配 | Steps 7–8 |
| SCG/BFF | Add §7.0 | 玉衡biz/mcp为自研ReactorNetty | 在玉衡直接补oauth2Login已被用户否决 | 独立进程/会话/一跳 | Steps 1,12 |
| OAuth MP+DDL | Add §7.0/11 | SA仅JPA/Flyway，MP有TenantLine与Runner | 只ThreadLocal不足SQL隔离 | 新schema/迁移与一逻辑SQL | Steps 2–6 |
| perissuer Cache/WebClient | Keep/Expand §7.0 | 现有IdpServiceOAuth2Client与single-flight | 每次AS请求更慢且已有复用点 | 新key/ bounded cache | Step 11 |
| key/revoke/audit | Add §7.0/15 | 单PEM、旧RT撤销；Outbox/Redis可复用 | 纯JWT无法严格即时撤销 | Redis每次读/审核spool与运行成本 | Steps 6,9,10 |
| 全局Resource目录迁移 | Add §11.2.11 | 旧resource FK指向全局identity_client | 保留旧FK将阻塞每issuer新client | 第11表；原Resource HTTP shape保留 | Steps 2,4,8 |
| 获取tenant再原样转发API | Remove §7.0 | JWT issuer与安全principal已有 | 目标服务自行核对issuer/claim | 额外RTT/TOCTOU无价值 | 不列新API |
| Grant列表GET（待确认） | 修订Spec §7/9，尚未Accepted | 当前页面请求路由、当前Controller无GET | 写接口结果无法重建完整grant集合 | 既有前端一次GET由404变200；无新表 | 等修订获批后接入Step 9/15 |
| GraphQL/动态客户端注册/TokenExchange | N/A §3.3 | 当前消费只HTTP/RPC | 不需新协议 | 无 | 不列文件 |

### 4.6 Change-unit Dependency Matrix

| Change unit | Requirements | Proof/RED point | Compile/runtime prerequisites | Produces | Consumers/unblocks | Owning Step |
| --- | --- | --- | --- | --- | --- | --- |
| 锁定依赖和技术入口 | REQ-001, REQ-010, REQ-011, REQ-018 | Not applicable — 本Step只建POM/入口的编译前置，行为RED由后续Step测试建立；离线缓存不足时报缺依赖而非伪成功 | None | Maven reactor 能识别独立 SCG 模块；授柄SAS/MP/Session依赖及同一版本线受控 | Step 2 | Step 1 |
| 建立受管OAuth数据库与物理路由 | REQ-002, REQ-008, REQ-014, REQ-017, REQ-018 | Required — OAuthManagedSchemaContractTest先因新manifest/DDL/NATIVE资源缺失RED；随后静态/隔离schema test GREEN；真实PostgreSQL DDL归用户控制运行门禁 | Step 1 | 一个新SQL版本定义11表及约束；NATIVE SINGLE精准落在tianquan_oauth | Step 3 | Step 2 |
| 持久化issuer与客户端注册 | REQ-002, REQ-005, REQ-011, REQ-018 | Required — 必需PO/BO/Repository签名先让test编译；测试在Mapper/Impl装配前因缺Bean或缺具名SQL按目标RED，随后GREEN | Step 2 | 这组认证事实的Repository/Mapper能按同realm/tenant/active/version读写 | Step 4 | Step 3 |
| 持久化资源目录与最小授权 | REQ-005, REQ-008, REQ-016, REQ-017 | Required — 必需PO/BO/Repository签名先让test编译；测试在Mapper/Impl装配前因缺Bean或缺具名SQL按目标RED，随后GREEN | Step 3 | 这组认证事实的Repository/Mapper能按同realm/tenant/active/version读写 | Step 5 | Step 4 |
| 持久化授权码、令牌和同意 | REQ-003, REQ-004, REQ-012, REQ-017 | Required — 必需PO/BO/Repository签名先让test编译；测试在Mapper/Impl装配前因缺Bean或缺具名SQL按目标RED，随后GREEN | Step 4 | 这组认证事实的Repository/Mapper能按同realm/tenant/active/version读写 | Step 6 | Step 5 |
| 持久化密钥、撤销与审计状态 | REQ-012, REQ-013, REQ-014, REQ-015 | Required — 必需PO/BO/Repository签名先让test编译；测试在Mapper/Impl装配前因缺Bean或缺具名SQL按目标RED，随后GREEN | Step 5 | 这组认证事实的Repository/Mapper能按同realm/tenant/active/version读写 | Step 7 | Step 6 |
| 装配多issuer SAS仓储与协议发现 | REQ-001, REQ-002, REQ-004, REQ-013, REQ-016 | Required — 两issuer测试先因SPI/多路径缺失RED，再补真实SAS装配GREEN | Step 6 | SAS多issuer精确registry、JWK与protocol discovery逐租户独立 | Step 8 | Step 7 |
| 实现授权码、PKCE和登录交互 | REQ-003, REQ-004, REQ-005, REQ-011 | Required — 旧token controller无authorization_code/PKCE/OIDC，focused protocol RED后仅最小Provider/登录改动GREEN | Step 7 | 标准Code/PKCE/consent/login和机器Client Credentials签发保持scope/audience界限 | Step 9 | Step 8 |
| 改造客户端、资源与密钥管理 | REQ-002, REQ-005, REQ-011, REQ-013, REQ-017 | Required — 当前JPA/Argon2/全局资源UI路径使租户/密钥测试RED，修最小管理链后GREEN | Step 8 | 现有管理URL按realm管理Client/Grant/Key；secret仅bcrypt一次性显示 | Step 10 | Step 9 |
| 闭合撤销、审计、密钥轮换 | REQ-012, REQ-013, REQ-014, REQ-015, REQ-016 | Required — 初始无共享状态/恢复handler，RED由新用例而非容器启动失败；单元先GREEN，真实Redis/PG在用户运行门禁 | Step 9 | 成功撤销后新鉴权拒绝；Redis故障503；审计持久可恢复 | Step 11 | Step 10 |
| 在资源服务器建立多issuer本地验签和租户上下文 | REQ-002, REQ-006, REQ-007, REQ-008, REQ-012, REQ-016 | Required — 新多issuer/tenant/Redis故障测试RED；创建resolver/Bridge之后GREEN | Step 10 | 所有受保护资源请求本地RS256后共享撤销检查，Servlet ThreadLocal/MDC最终清理 | Step 12 | Step 11 |
| 改造机器客户端缓存并装配WebClient | REQ-005, REQ-009, REQ-011, REQ-016 | Required — 旧缓存键/TTL对新issuer与凭据版本不满足用例，先RED后GREEN | Step 11 | 出站调用按issuer/client/凭据版/aud/tenant/scope隔离并临期重获，无client_credentials RT | Step 13 | Step 12 |
| 建立SCG OAuth2登录和服务端会话 | REQ-003, REQ-004, REQ-006, REQ-010, REQ-011, REQ-012 | Required — 尚无SCG登录/CSRF/TokenRelay，WebTestClient RED；实现具名链后GREEN | Step 12 | 浏览器仅持HttpOnly opaque会话、SCG代码换token、玉衡路由原样转发Bearer | Step 14 | Step 13 |
| 保留玉衡、鉴神与天枢准入链 | REQ-001, REQ-005, REQ-006, REQ-007, REQ-010, REQ-016 | Required — old cookie/provider/PLATFORM acceptance routes fail new fixturesRED，再接入新principal和范围校验GREEN | Step 13 | 平台控制SERVICE token与USER授权在现有路由/鉴神/天枢链正确准入 | Step 15 | Step 14 |
| 迁移共享认证客户端与五个前端消费者 | REQ-003, REQ-004, REQ-010, REQ-017 | Required — gatewayAuthClient旧密码POST与AT/RT Cookie契约使新BFF test RED；共享客户端与各消费者更新后GREEN | Step 14 | Portal/四Admin前端按新BFF/Consent/Session契约运行且不保存token | Step 16 | Step 15 |
| 提供受控数据导入、切流及本地验证入口 | REQ-011, REQ-012, REQ-016, REQ-017, REQ-018, REQ-019 | Required — 迁移测试先对旧JPA/旧登录路径RED；新增Service/脚本/运行手册后静态与单元GREEN，真实导入由用户操作者运行 | Step 14 | 逐租户导入/凭证轮换/切流报告可审，脚本只准备用户发起的运行验收 | Step 17 | Step 16 |
| 退役旧协议并校验全域回归 | REQ-001, REQ-006, REQ-010, REQ-012, REQ-017, REQ-019 | Required — old token/login controllers still registered produce non410 RED；retirement filter/config GREEN | Step 15 | 新部署旧OAuth根协议返回410且无双签发链；所有原消费者已迁出 | 发布验收 | Step 17 |

### 4.7 Java, Spring, and Egon-COLA Implementation Standards

| Concern | Current repository evidence | Effective Spec decision | Planned implementation consequence | Owning Steps/checks |
| --- | --- | --- | --- | --- |
| Architecture profile | SJ现有oauth/resource/token业务分包+SA POM；Spec §6.1传统 | 保留admin.<domain>.controller/service/service.impl/repository/dao/domain | BO/PO与Service/Repository/Mapper在原domain嵌入，不建7层DDD | Steps 2–15/MC-ARCH-001 |
| Validation/POJO/Mapper | MP EgonModel、ValidationUtils、BaseConverter源码；SA lombok.config | Spec §6.2/10普通class完整注解、groups、MapStruct | PO承8继承字段，Service/Repository入口双校验，转换器带泛型 | Steps 3–13/MC-VALID/MODEL/CONVERT |
| Bean/secret/audit | SA lombok.config复制Qualifier；Argon2人员适配 | Spec §8.4/15 user密码不变，Client bcrypt12 | 业务Bean显名+@Slf4j+RequiredArgsConstructor+每字段Qualifier，私钥/secret不打印 | Steps 7–13/MC-LOG/BEAN |
| Config/time | SA base/local YAML；java.time Clock | Spec §15.5同键，UTC Instant/Duration | 所有owner成对profile+properties，NumericDate秒/PG微秒 | Steps 2,7,10,12/MC-CONFIG/TIME |
| Complex pattern | 现有ClientCredentialsAccessPolicy及Security Provider | Spec §13策略/适配/链/版本锁 | Provider wrappers按grant注册、issuer SPI委派、Redis revision max | Steps 7–13/MC-PATTERN |

#### Capability reuse ledger

| Need | Candidates inspected | Exact evidence | Fit/gap | Decision | Added dependency/custom code | Owning Step/check |
| --- | --- | --- | --- | --- | --- | --- |
| OAuth协议 | Spring SAS starter vs OAuthTokenController | SA/pom.xml；OAuthTokenController | 旧实现无Code/OIDC | 用户原要求和DEC-001批准SAS | Boot BOM-managed starter | Step 1/MC-DEP |
| SCG入口 | Cloud Gateway vs玉衡Netty Engine | yuheng-biz-gateway/pom.xml | 现有不是SCG；前置明确获批 | Cloud2025.0.3 BOM + WebFlux Gateway starter | 独立edge模块；成本多部署单元 | Steps1,12 |
| 共享会话 | Spring Session Redis vs默认内存 | SA/YAML Redis；Spec DEC-005 | 多实例会话需要共享 | 批准spring-session-data-redis，SAS与BFF不同namespace | 两处依赖 | Steps1,7,12 |
| 租户SQL/DDL | MP starter TenantLine/Runner vs JPA | MP EgonColaPostgreDdlRunner, EgonModel | 现有SA OAuth JPA无行隔离 | 批准MP starter；NATIVE SINGLE支持tianquan_oauth | 只认证新表迁移 | Steps1–6 |
| 可靠事件 | 已有Outbox/MQ投递 | OB DefaultTransactionalOutbox/DeliveryHandler | 签发/撤销用现有Outbox；RS无DB用现有Redisson Stream | 扩展handler、JDK受控spool | 无新增MQ坐标 | Steps6,9 |
| 私钥加密 | Common Crypto Digests/Hmacs + JDK JCA | common-crypto无AES/GCM | 必须有perissuer密文 | Spec §8.4允许局部OAuthKeyCipherComponent复用JDK | 无新库 | Step9 |
| Backend generator | scripts/egon-codegen.sh | backend-code-generation.md明确传统不支持 | 原模块传统分包且用户明确不用 | 不调用、不伪装GENERATED；PO/Mapper为人工计划 | 无 | Steps2–6/MC-ARCH |

### 4.8 User-mandated Java Rule Implementation Matrix

| Literal rule | Spec source | Repository evidence | Exact files and order | Pseudocode obligations | Validation gate | Steps | Status/blocker |
| --- | --- | --- | --- | --- | --- | --- | --- |
| Rule 1 | §8.4/10 | 模型表与现有VO/DTO | Step3–17先类型后消费者；新增类型补充见§7各Step | PO/BO/DTO/VO/Command/Event后缀，禁Data/Info/Param/Bean | rg与编译 | Step3–14 | PASS |
| Rule 2 | §10.2 | SA validation依赖 | Step3–13边界/测试 | @Valid/@Validated分组与SPI手动ValidationUtils | 负例及组测试 | Step3–13边界/测试 | PASS |
| Rule 3 | §10/11 | EgonModel与BaseConverter | Step3–9 PO/BO/Converter | 普通class四注解+Builder/SuperBuilder/callSuper；每映射MapStruct | 构造器与字段单测 | Step3–9 PO/BO/Converter | PASS |
| Rule 4 | §8.4 | SA Qualifier复制；SS无lombok.config | Step1/7–13先配置后业务Bean | @Slf4j/具名Bean/final @Qualifier/RequiredArgsConstructor | 上下文装配/生成构造参数 | Step1/7–13 | PASS |
| Rule 5 | §6.1/15 | 现有JDK/Commons/Guava | Step1/9 | FileChannel/JCA与既有组件；不增新Utils库 | import/dependency gate | Step1/9 | PASS |
| Rule 6 | §9/10 | Boot Jackson、旧VO | Step7–14契约 | JSON字段/枚举@JsonValue和@EnumValue、敏感exclusion | OAS/序列化对比 | Step7–14契约 | PASS |
| Rule 7 | §15.5 | SA base/local；新edge base/local | Step2/12同时改配对文件 | 完全相同配置键，允许值不同 | YAML key-set parity | Step2/12同时改配对文件 | PASS |
| Rule 9 | §13 | 现有Policy/Provider | Step7–13先模式参与者后wiring | Strategy/Adapter/Chain/State，禁大switch | Provider/状态并发用例 | Step7–13 | PASS |
| Rule 10 | §10/11/15 | 现有Clock与EgonModel | Step3–13模型/边界 | Instant/Duration/UTC LocalDateTime及JWT秒 | grep forbidden Date/Calendar | Step3–13模型/边界 | PASS |
| Rule 11 | §6.1/11 | 当前传统domain业务包，MP及OB | 每个Step保持原包/组件 | Egon MP+DDL、deleted_at UK/active guard、CQE Outbox或MQ | 架构/DDL/outbox测试 | 每个Step保持原包/组件 | PASS |
## 5. Change File Tree

文件所有权按Step标记。相同路径在后续Step再次出现时具有不同语义（例如SA配置先SPI后Provider）；这些重复在表中用各次操作和Step标明，不是遗漏。当前不存在的目标路径在Step的Repository evidence标记为Spec目标；实施前按最新HEAD再确认占用状态。

| Operation | Path | Current evidence/symbol | Final symbols/state | Responsibility | Step | Requirements | Validation owner |
| --- | --- | --- | --- | --- | --- | --- | --- |
| MODIFY | egon-cola-xingyuan/pom.xml | 当前文件存在：egon-cola-xingyuan/pom.xml | spring-cloud-dependencies 2025.0.3 | 由已批准的Cloud BOM管理SCG版本 | Step 1 | REQ-001, REQ-010, REQ-011, REQ-018 | mvn -o -pl egon-cola-xingyuan/egon-cola-yuheng/yuheng-edge-gateway -am -DskipTests validate |
| MODIFY | egon-cola-xingyuan/egon-cola-yuheng/pom.xml | 当前文件存在：egon-cola-xingyuan/egon-cola-yuheng/pom.xml | module yuheng-edge-gateway | 纳入现有玉衡reactor但独立部署 | Step 1 | REQ-001, REQ-010, REQ-011, REQ-018 | mvn -o -pl egon-cola-xingyuan/egon-cola-yuheng/yuheng-edge-gateway -am -DskipTests validate |
| CREATE | egon-cola-xingyuan/egon-cola-yuheng/yuheng-edge-gateway/pom.xml | 当前不存在；Spec §8/§11 明确目标，邻近包可验证 | yuheng-edge-gateway dependencies | SCG/WebFlux/OAuth2 Client/Resource Server/Session Redis的独立可执行模块 | Step 1 | REQ-001, REQ-010, REQ-011, REQ-018 | mvn -o -pl egon-cola-xingyuan/egon-cola-yuheng/yuheng-edge-gateway -am -DskipTests validate |
| MODIFY | egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/pom.xml | 当前文件存在：egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/pom.xml | SAS+MP+Session dependencies | 为后续协议和持久化提供已批准类型 | Step 1 | REQ-001, REQ-010, REQ-011, REQ-018 | mvn -o -pl egon-cola-xingyuan/egon-cola-yuheng/yuheng-edge-gateway -am -DskipTests validate |
| MODIFY | egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/pom.xml | 当前文件存在：egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/pom.xml | WebClient dependencies | 以现有spring-security-oauth2-client为基础增加WebFlux入口所需Boot管理依赖 | Step 1 | REQ-001, REQ-010, REQ-011, REQ-018 | mvn -o -pl egon-cola-xingyuan/egon-cola-yuheng/yuheng-edge-gateway -am -DskipTests validate |
| CREATE | egon-cola-xingyuan/egon-cola-yuheng/yuheng-edge-gateway/lombok.config | 当前不存在；Spec §8/§11 明确目标，邻近包可验证 | copyableAnnotations Qualifier | 新edge业务Bean的构造参数保留Qualifier | Step 1 | REQ-001, REQ-010, REQ-011, REQ-018 | mvn -o -pl egon-cola-xingyuan/egon-cola-yuheng/yuheng-edge-gateway -am -DskipTests validate |
| CREATE | egon-cola-xingyuan/egon-cola-yuheng/yuheng-edge-gateway/src/main/java/top/egon/cola/component/yuheng/edge/GatewayEdgeApplication.java | 当前不存在；Spec §8/§11 明确目标，邻近包可验证 | GatewayEdgeApplication.main | 仅新edge进程启动类 | Step 1 | REQ-001, REQ-010, REQ-011, REQ-018 | mvn -o -pl egon-cola-xingyuan/egon-cola-yuheng/yuheng-edge-gateway -am -DskipTests validate |
| CREATE | egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/config/OAuthManagedSchemaContractTest.java | 当前不存在；Spec §8/§11 明确目标，邻近包可验证 | oneManagedVersionAndRoutesAllTables | RED固定manifest/表/旧Flyway不可变 | Step 2 | REQ-002, REQ-008, REQ-014, REQ-017, REQ-018 | python3 .agents/skills/egon-coding-writing-plan/scripts/validate_skill_resources.py |
| CREATE | egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/resources/db/egon-mp/V20260922_001__initialize_oauth_tenant_schema.sql | 当前不存在；Spec §8/§11 明确目标，邻近包可验证 | tianquan_oauth 11 tables | 单逻辑DDL版本，包含Spec §11每列/索引 | Step 2 | REQ-002, REQ-008, REQ-014, REQ-017, REQ-018 | python3 .agents/skills/egon-coding-writing-plan/scripts/validate_skill_resources.py |
| CREATE | egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/resources/db/egon-mp/repository-manifest.json | 当前不存在；Spec §8/§11 明确目标，邻近包可验证 | family/scripts/version/path/sha256 | 绑定唯一新SQL字节哈希 | Step 2 | REQ-002, REQ-008, REQ-014, REQ-017, REQ-018 | python3 .agents/skills/egon-coding-writing-plan/scripts/validate_skill_resources.py |
| CREATE | egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/resources/oauth-sharding-native.yml | 当前不存在；Spec §8/§11 明确目标，邻近包可验证 | NATIVE SINGLE nodes | 明确11个tianquan_oauth节点及当前public SINGLE清单 | Step 2 | REQ-002, REQ-008, REQ-014, REQ-017, REQ-018 | python3 .agents/skills/egon-coding-writing-plan/scripts/validate_skill_resources.py |
| CREATE | egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/config/OAuthPersistenceConfiguration.java | 当前不存在；Spec §8/§11 明确目标，邻近包可验证 | egonColaShardingLogicalDataSourceFactory | 在逻辑DS创建前调用EgonColaPostgreDdlRunner | Step 2 | REQ-002, REQ-008, REQ-014, REQ-017, REQ-018 | python3 .agents/skills/egon-coding-writing-plan/scripts/validate_skill_resources.py |
| MODIFY | egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/resources/application.yml | 当前文件存在：egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/resources/application.yml | NATIVE/DDL/profile core keys | 增加Spec §15.5完整叶子结构并禁旧Flyway自动执行 | Step 2 | REQ-002, REQ-008, REQ-014, REQ-017, REQ-018 | python3 .agents/skills/egon-coding-writing-plan/scripts/validate_skill_resources.py |
| MODIFY | egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/resources/application-local.yml | 当前文件存在：egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/resources/application-local.yml | same config keys | 同步base全部新增键并允许local值不同 | Step 2 | REQ-002, REQ-008, REQ-014, REQ-017, REQ-018 | python3 .agents/skills/egon-coding-writing-plan/scripts/validate_skill_resources.py |
| CREATE | egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/domain/po/OAuthRealmPO.java | 当前不存在；Spec §8/§11 明确目标，邻近包可验证 | OAuthRealmPO | EgonModel承载本表完整列 | Step 3 | REQ-002, REQ-005, REQ-011, REQ-018 | mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthRealmPersistenceContractTest -Dsurefire.failIfNoSpecifiedTests=false test |
| CREATE | egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/domain/bo/OAuthRealmBO.java | 当前不存在；Spec §8/§11 明确目标，邻近包可验证 | OAuthRealmBO | Service计算与PO分界 | Step 3 | REQ-002, REQ-005, REQ-011, REQ-018 | mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthRealmPersistenceContractTest -Dsurefire.failIfNoSpecifiedTests=false test |
| CREATE | egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/repository/OAuthRealmRepository.java | 当前不存在；Spec §8/§11 明确目标，邻近包可验证 | OAuthRealmRepository | Service-facing typed contract | Step 3 | REQ-002, REQ-005, REQ-011, REQ-018 | mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthRealmPersistenceContractTest -Dsurefire.failIfNoSpecifiedTests=false test |
| CREATE | egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/domain/po/OAuthClientPO.java | 当前不存在；Spec §8/§11 明确目标，邻近包可验证 | OAuthClientPO | EgonModel承载本表完整列 | Step 3 | REQ-002, REQ-005, REQ-011, REQ-018 | mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthRealmPersistenceContractTest -Dsurefire.failIfNoSpecifiedTests=false test |
| CREATE | egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/domain/bo/OAuthClientBO.java | 当前不存在；Spec §8/§11 明确目标，邻近包可验证 | OAuthClientBO | Service计算与PO分界 | Step 3 | REQ-002, REQ-005, REQ-011, REQ-018 | mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthRealmPersistenceContractTest -Dsurefire.failIfNoSpecifiedTests=false test |
| CREATE | egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/repository/OAuthClientRepository.java | 当前不存在；Spec §8/§11 明确目标，邻近包可验证 | OAuthClientRepository | Service-facing typed contract | Step 3 | REQ-002, REQ-005, REQ-011, REQ-018 | mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthRealmPersistenceContractTest -Dsurefire.failIfNoSpecifiedTests=false test |
| CREATE | egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/repository/OAuthRealmPersistenceContractTest.java | 当前不存在；Spec §8/§11 明确目标，邻近包可验证 | OAuthRealmPersistenceContractTest | RED证明这组映射/查询/跨租户错误 | Step 3 | REQ-002, REQ-005, REQ-011, REQ-018 | mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthRealmPersistenceContractTest -Dsurefire.failIfNoSpecifiedTests=false test |
| CREATE | egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/domain/converter/OAuthRealmConverter.java | 当前不存在；Spec §8/§11 明确目标，邻近包可验证 | OAuthRealmConverter | PO↔BO双向MapStruct转换 | Step 3 | REQ-002, REQ-005, REQ-011, REQ-018 | mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthRealmPersistenceContractTest -Dsurefire.failIfNoSpecifiedTests=false test |
| CREATE | egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/dao/OAuthRealmMapper.java | 当前不存在；Spec §8/§11 明确目标，邻近包可验证 | OAuthRealmMapper | MP Mapper具名SQL接口 | Step 3 | REQ-002, REQ-005, REQ-011, REQ-018 | mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthRealmPersistenceContractTest -Dsurefire.failIfNoSpecifiedTests=false test |
| CREATE | egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/resources/mapper/oauth/OAuthRealmMapper.xml | 当前不存在；Spec §8/§11 明确目标，邻近包可验证 | OAuthRealmMapper SQL | 具名PostgreSQL谓词/锁 | Step 3 | REQ-002, REQ-005, REQ-011, REQ-018 | mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthRealmPersistenceContractTest -Dsurefire.failIfNoSpecifiedTests=false test |
| CREATE | egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/repository/impl/OAuthRealmRepositoryImpl.java | 当前不存在；Spec §8/§11 明确目标，邻近包可验证 | OAuthRealmRepositoryImpl | MP repository ACL | Step 3 | REQ-002, REQ-005, REQ-011, REQ-018 | mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthRealmPersistenceContractTest -Dsurefire.failIfNoSpecifiedTests=false test |
| CREATE | egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/domain/converter/OAuthClientConverter.java | 当前不存在；Spec §8/§11 明确目标，邻近包可验证 | OAuthClientConverter | PO↔BO双向MapStruct转换 | Step 3 | REQ-002, REQ-005, REQ-011, REQ-018 | mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthRealmPersistenceContractTest -Dsurefire.failIfNoSpecifiedTests=false test |
| CREATE | egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/dao/OAuthClientMapper.java | 当前不存在；Spec §8/§11 明确目标，邻近包可验证 | OAuthClientMapper | MP Mapper具名SQL接口 | Step 3 | REQ-002, REQ-005, REQ-011, REQ-018 | mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthRealmPersistenceContractTest -Dsurefire.failIfNoSpecifiedTests=false test |
| CREATE | egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/resources/mapper/oauth/OAuthClientMapper.xml | 当前不存在；Spec §8/§11 明确目标，邻近包可验证 | OAuthClientMapper SQL | 具名PostgreSQL谓词/锁 | Step 3 | REQ-002, REQ-005, REQ-011, REQ-018 | mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthRealmPersistenceContractTest -Dsurefire.failIfNoSpecifiedTests=false test |
| CREATE | egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/repository/impl/OAuthClientRepositoryImpl.java | 当前不存在；Spec §8/§11 明确目标，邻近包可验证 | OAuthClientRepositoryImpl | MP repository ACL | Step 3 | REQ-002, REQ-005, REQ-011, REQ-018 | mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthRealmPersistenceContractTest -Dsurefire.failIfNoSpecifiedTests=false test |
| CREATE | egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/resource/domain/po/OAuthResourcePO.java | 当前不存在；Spec §8/§11 明确目标，邻近包可验证 | OAuthResourcePO | EgonModel承载本表完整列 | Step 4 | REQ-005, REQ-008, REQ-016, REQ-017 | mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthResourcePersistenceContractTest -Dsurefire.failIfNoSpecifiedTests=false test |
| CREATE | egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/resource/domain/bo/OAuthResourceBO.java | 当前不存在；Spec §8/§11 明确目标，邻近包可验证 | OAuthResourceBO | Service计算与PO分界 | Step 4 | REQ-005, REQ-008, REQ-016, REQ-017 | mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthResourcePersistenceContractTest -Dsurefire.failIfNoSpecifiedTests=false test |
| CREATE | egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/resource/repository/OAuthResourceRepository.java | 当前不存在；Spec §8/§11 明确目标，邻近包可验证 | OAuthResourceRepository | Service-facing typed contract | Step 4 | REQ-005, REQ-008, REQ-016, REQ-017 | mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthResourcePersistenceContractTest -Dsurefire.failIfNoSpecifiedTests=false test |
| CREATE | egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/resource/domain/po/OAuthGrantPO.java | 当前不存在；Spec §8/§11 明确目标，邻近包可验证 | OAuthGrantPO | EgonModel承载本表完整列 | Step 4 | REQ-005, REQ-008, REQ-016, REQ-017 | mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthResourcePersistenceContractTest -Dsurefire.failIfNoSpecifiedTests=false test |
| CREATE | egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/resource/domain/bo/OAuthGrantBO.java | 当前不存在；Spec §8/§11 明确目标，邻近包可验证 | OAuthGrantBO | Service计算与PO分界 | Step 4 | REQ-005, REQ-008, REQ-016, REQ-017 | mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthResourcePersistenceContractTest -Dsurefire.failIfNoSpecifiedTests=false test |
| CREATE | egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/resource/repository/OAuthGrantRepository.java | 当前不存在；Spec §8/§11 明确目标，邻近包可验证 | OAuthGrantRepository | Service-facing typed contract | Step 4 | REQ-005, REQ-008, REQ-016, REQ-017 | mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthResourcePersistenceContractTest -Dsurefire.failIfNoSpecifiedTests=false test |
| CREATE | egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/repository/OAuthResourcePersistenceContractTest.java | 当前不存在；Spec §8/§11 明确目标，邻近包可验证 | OAuthResourcePersistenceContractTest | RED证明这组映射/查询/跨租户错误 | Step 4 | REQ-005, REQ-008, REQ-016, REQ-017 | mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthResourcePersistenceContractTest -Dsurefire.failIfNoSpecifiedTests=false test |
| CREATE | egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/resource/domain/converter/OAuthResourceConverter.java | 当前不存在；Spec §8/§11 明确目标，邻近包可验证 | OAuthResourceConverter | PO↔BO双向MapStruct转换 | Step 4 | REQ-005, REQ-008, REQ-016, REQ-017 | mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthResourcePersistenceContractTest -Dsurefire.failIfNoSpecifiedTests=false test |
| CREATE | egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/resource/dao/OAuthResourceMapper.java | 当前不存在；Spec §8/§11 明确目标，邻近包可验证 | OAuthResourceMapper | MP Mapper具名SQL接口 | Step 4 | REQ-005, REQ-008, REQ-016, REQ-017 | mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthResourcePersistenceContractTest -Dsurefire.failIfNoSpecifiedTests=false test |
| CREATE | egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/resources/mapper/resource/OAuthResourceMapper.xml | 当前不存在；Spec §8/§11 明确目标，邻近包可验证 | OAuthResourceMapper SQL | 具名PostgreSQL谓词/锁 | Step 4 | REQ-005, REQ-008, REQ-016, REQ-017 | mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthResourcePersistenceContractTest -Dsurefire.failIfNoSpecifiedTests=false test |
| CREATE | egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/resource/repository/impl/OAuthResourceRepositoryImpl.java | 当前不存在；Spec §8/§11 明确目标，邻近包可验证 | OAuthResourceRepositoryImpl | MP repository ACL | Step 4 | REQ-005, REQ-008, REQ-016, REQ-017 | mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthResourcePersistenceContractTest -Dsurefire.failIfNoSpecifiedTests=false test |
| CREATE | egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/resource/domain/converter/OAuthGrantConverter.java | 当前不存在；Spec §8/§11 明确目标，邻近包可验证 | OAuthGrantConverter | PO↔BO双向MapStruct转换 | Step 4 | REQ-005, REQ-008, REQ-016, REQ-017 | mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthResourcePersistenceContractTest -Dsurefire.failIfNoSpecifiedTests=false test |
| CREATE | egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/resource/dao/OAuthGrantMapper.java | 当前不存在；Spec §8/§11 明确目标，邻近包可验证 | OAuthGrantMapper | MP Mapper具名SQL接口 | Step 4 | REQ-005, REQ-008, REQ-016, REQ-017 | mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthResourcePersistenceContractTest -Dsurefire.failIfNoSpecifiedTests=false test |
| CREATE | egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/resources/mapper/resource/OAuthGrantMapper.xml | 当前不存在；Spec §8/§11 明确目标，邻近包可验证 | OAuthGrantMapper SQL | 具名PostgreSQL谓词/锁 | Step 4 | REQ-005, REQ-008, REQ-016, REQ-017 | mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthResourcePersistenceContractTest -Dsurefire.failIfNoSpecifiedTests=false test |
| CREATE | egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/resource/repository/impl/OAuthGrantRepositoryImpl.java | 当前不存在；Spec §8/§11 明确目标，邻近包可验证 | OAuthGrantRepositoryImpl | MP repository ACL | Step 4 | REQ-005, REQ-008, REQ-016, REQ-017 | mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthResourcePersistenceContractTest -Dsurefire.failIfNoSpecifiedTests=false test |
| CREATE | egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/domain/po/OAuthAuthorizationPO.java | 当前不存在；Spec §8/§11 明确目标，邻近包可验证 | OAuthAuthorizationPO | EgonModel承载本表完整列 | Step 5 | REQ-003, REQ-004, REQ-012, REQ-017 | mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthAuthorizationPersistenceContractTest -Dsurefire.failIfNoSpecifiedTests=false test |
| CREATE | egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/domain/bo/OAuthAuthorizationBO.java | 当前不存在；Spec §8/§11 明确目标，邻近包可验证 | OAuthAuthorizationBO | Service计算与PO分界 | Step 5 | REQ-003, REQ-004, REQ-012, REQ-017 | mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthAuthorizationPersistenceContractTest -Dsurefire.failIfNoSpecifiedTests=false test |
| CREATE | egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/repository/OAuthAuthorizationRepository.java | 当前不存在；Spec §8/§11 明确目标，邻近包可验证 | OAuthAuthorizationRepository | Service-facing typed contract | Step 5 | REQ-003, REQ-004, REQ-012, REQ-017 | mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthAuthorizationPersistenceContractTest -Dsurefire.failIfNoSpecifiedTests=false test |
| CREATE | egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/domain/po/OAuthTokenPO.java | 当前不存在；Spec §8/§11 明确目标，邻近包可验证 | OAuthTokenPO | EgonModel承载本表完整列 | Step 5 | REQ-003, REQ-004, REQ-012, REQ-017 | mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthAuthorizationPersistenceContractTest -Dsurefire.failIfNoSpecifiedTests=false test |
| CREATE | egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/domain/bo/OAuthTokenBO.java | 当前不存在；Spec §8/§11 明确目标，邻近包可验证 | OAuthTokenBO | Service计算与PO分界 | Step 5 | REQ-003, REQ-004, REQ-012, REQ-017 | mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthAuthorizationPersistenceContractTest -Dsurefire.failIfNoSpecifiedTests=false test |
| CREATE | egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/repository/OAuthTokenRepository.java | 当前不存在；Spec §8/§11 明确目标，邻近包可验证 | OAuthTokenRepository | Service-facing typed contract | Step 5 | REQ-003, REQ-004, REQ-012, REQ-017 | mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthAuthorizationPersistenceContractTest -Dsurefire.failIfNoSpecifiedTests=false test |
| CREATE | egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/domain/po/OAuthConsentPO.java | 当前不存在；Spec §8/§11 明确目标，邻近包可验证 | OAuthConsentPO | EgonModel承载本表完整列 | Step 5 | REQ-003, REQ-004, REQ-012, REQ-017 | mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthAuthorizationPersistenceContractTest -Dsurefire.failIfNoSpecifiedTests=false test |
| CREATE | egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/domain/bo/OAuthConsentBO.java | 当前不存在；Spec §8/§11 明确目标，邻近包可验证 | OAuthConsentBO | Service计算与PO分界 | Step 5 | REQ-003, REQ-004, REQ-012, REQ-017 | mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthAuthorizationPersistenceContractTest -Dsurefire.failIfNoSpecifiedTests=false test |
| CREATE | egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/repository/OAuthConsentRepository.java | 当前不存在；Spec §8/§11 明确目标，邻近包可验证 | OAuthConsentRepository | Service-facing typed contract | Step 5 | REQ-003, REQ-004, REQ-012, REQ-017 | mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthAuthorizationPersistenceContractTest -Dsurefire.failIfNoSpecifiedTests=false test |
| CREATE | egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/repository/OAuthAuthorizationPersistenceContractTest.java | 当前不存在；Spec §8/§11 明确目标，邻近包可验证 | OAuthAuthorizationPersistenceContractTest | RED证明这组映射/查询/跨租户错误 | Step 5 | REQ-003, REQ-004, REQ-012, REQ-017 | mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthAuthorizationPersistenceContractTest -Dsurefire.failIfNoSpecifiedTests=false test |
| CREATE | egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/domain/converter/OAuthAuthorizationConverter.java | 当前不存在；Spec §8/§11 明确目标，邻近包可验证 | OAuthAuthorizationConverter | PO↔BO双向MapStruct转换 | Step 5 | REQ-003, REQ-004, REQ-012, REQ-017 | mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthAuthorizationPersistenceContractTest -Dsurefire.failIfNoSpecifiedTests=false test |
| CREATE | egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/dao/OAuthAuthorizationMapper.java | 当前不存在；Spec §8/§11 明确目标，邻近包可验证 | OAuthAuthorizationMapper | MP Mapper具名SQL接口 | Step 5 | REQ-003, REQ-004, REQ-012, REQ-017 | mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthAuthorizationPersistenceContractTest -Dsurefire.failIfNoSpecifiedTests=false test |
| CREATE | egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/resources/mapper/oauth/OAuthAuthorizationMapper.xml | 当前不存在；Spec §8/§11 明确目标，邻近包可验证 | OAuthAuthorizationMapper SQL | 具名PostgreSQL谓词/锁 | Step 5 | REQ-003, REQ-004, REQ-012, REQ-017 | mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthAuthorizationPersistenceContractTest -Dsurefire.failIfNoSpecifiedTests=false test |
| CREATE | egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/repository/impl/OAuthAuthorizationRepositoryImpl.java | 当前不存在；Spec §8/§11 明确目标，邻近包可验证 | OAuthAuthorizationRepositoryImpl | MP repository ACL | Step 5 | REQ-003, REQ-004, REQ-012, REQ-017 | mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthAuthorizationPersistenceContractTest -Dsurefire.failIfNoSpecifiedTests=false test |
| CREATE | egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/domain/converter/OAuthTokenConverter.java | 当前不存在；Spec §8/§11 明确目标，邻近包可验证 | OAuthTokenConverter | PO↔BO双向MapStruct转换 | Step 5 | REQ-003, REQ-004, REQ-012, REQ-017 | mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthAuthorizationPersistenceContractTest -Dsurefire.failIfNoSpecifiedTests=false test |
| CREATE | egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/dao/OAuthTokenMapper.java | 当前不存在；Spec §8/§11 明确目标，邻近包可验证 | OAuthTokenMapper | MP Mapper具名SQL接口 | Step 5 | REQ-003, REQ-004, REQ-012, REQ-017 | mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthAuthorizationPersistenceContractTest -Dsurefire.failIfNoSpecifiedTests=false test |
| CREATE | egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/resources/mapper/token/OAuthTokenMapper.xml | 当前不存在；Spec §8/§11 明确目标，邻近包可验证 | OAuthTokenMapper SQL | 具名PostgreSQL谓词/锁 | Step 5 | REQ-003, REQ-004, REQ-012, REQ-017 | mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthAuthorizationPersistenceContractTest -Dsurefire.failIfNoSpecifiedTests=false test |
| CREATE | egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/repository/impl/OAuthTokenRepositoryImpl.java | 当前不存在；Spec §8/§11 明确目标，邻近包可验证 | OAuthTokenRepositoryImpl | MP repository ACL | Step 5 | REQ-003, REQ-004, REQ-012, REQ-017 | mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthAuthorizationPersistenceContractTest -Dsurefire.failIfNoSpecifiedTests=false test |
| CREATE | egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/domain/converter/OAuthConsentConverter.java | 当前不存在；Spec §8/§11 明确目标，邻近包可验证 | OAuthConsentConverter | PO↔BO双向MapStruct转换 | Step 5 | REQ-003, REQ-004, REQ-012, REQ-017 | mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthAuthorizationPersistenceContractTest -Dsurefire.failIfNoSpecifiedTests=false test |
| CREATE | egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/dao/OAuthConsentMapper.java | 当前不存在；Spec §8/§11 明确目标，邻近包可验证 | OAuthConsentMapper | MP Mapper具名SQL接口 | Step 5 | REQ-003, REQ-004, REQ-012, REQ-017 | mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthAuthorizationPersistenceContractTest -Dsurefire.failIfNoSpecifiedTests=false test |
| CREATE | egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/resources/mapper/oauth/OAuthConsentMapper.xml | 当前不存在；Spec §8/§11 明确目标，邻近包可验证 | OAuthConsentMapper SQL | 具名PostgreSQL谓词/锁 | Step 5 | REQ-003, REQ-004, REQ-012, REQ-017 | mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthAuthorizationPersistenceContractTest -Dsurefire.failIfNoSpecifiedTests=false test |
| CREATE | egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/repository/impl/OAuthConsentRepositoryImpl.java | 当前不存在；Spec §8/§11 明确目标，邻近包可验证 | OAuthConsentRepositoryImpl | MP repository ACL | Step 5 | REQ-003, REQ-004, REQ-012, REQ-017 | mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthAuthorizationPersistenceContractTest -Dsurefire.failIfNoSpecifiedTests=false test |
| CREATE | egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/domain/po/OAuthSigningKeyPO.java | 当前不存在；Spec §8/§11 明确目标，邻近包可验证 | OAuthSigningKeyPO | EgonModel承载本表完整列 | Step 6 | REQ-012, REQ-013, REQ-014, REQ-015 | mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthSigningKeyPersistenceContractTest -Dsurefire.failIfNoSpecifiedTests=false test |
| CREATE | egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/domain/bo/OAuthSigningKeyBO.java | 当前不存在；Spec §8/§11 明确目标，邻近包可验证 | OAuthSigningKeyBO | Service计算与PO分界 | Step 6 | REQ-012, REQ-013, REQ-014, REQ-015 | mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthSigningKeyPersistenceContractTest -Dsurefire.failIfNoSpecifiedTests=false test |
| CREATE | egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/repository/OAuthSigningKeyRepository.java | 当前不存在；Spec §8/§11 明确目标，邻近包可验证 | OAuthSigningKeyRepository | Service-facing typed contract | Step 6 | REQ-012, REQ-013, REQ-014, REQ-015 | mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthSigningKeyPersistenceContractTest -Dsurefire.failIfNoSpecifiedTests=false test |
| CREATE | egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/domain/po/OAuthSecurityStatePO.java | 当前不存在；Spec §8/§11 明确目标，邻近包可验证 | OAuthSecurityStatePO | EgonModel承载本表完整列 | Step 6 | REQ-012, REQ-013, REQ-014, REQ-015 | mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthSigningKeyPersistenceContractTest -Dsurefire.failIfNoSpecifiedTests=false test |
| CREATE | egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/domain/bo/OAuthSecurityStateBO.java | 当前不存在；Spec §8/§11 明确目标，邻近包可验证 | OAuthSecurityStateBO | Service计算与PO分界 | Step 6 | REQ-012, REQ-013, REQ-014, REQ-015 | mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthSigningKeyPersistenceContractTest -Dsurefire.failIfNoSpecifiedTests=false test |
| CREATE | egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/repository/OAuthSecurityStateRepository.java | 当前不存在；Spec §8/§11 明确目标，邻近包可验证 | OAuthSecurityStateRepository | Service-facing typed contract | Step 6 | REQ-012, REQ-013, REQ-014, REQ-015 | mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthSigningKeyPersistenceContractTest -Dsurefire.failIfNoSpecifiedTests=false test |
| CREATE | egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/domain/po/OAuthRevocationPO.java | 当前不存在；Spec §8/§11 明确目标，邻近包可验证 | OAuthRevocationPO | EgonModel承载本表完整列 | Step 6 | REQ-012, REQ-013, REQ-014, REQ-015 | mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthSigningKeyPersistenceContractTest -Dsurefire.failIfNoSpecifiedTests=false test |
| CREATE | egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/domain/bo/OAuthRevocationBO.java | 当前不存在；Spec §8/§11 明确目标，邻近包可验证 | OAuthRevocationBO | Service计算与PO分界 | Step 6 | REQ-012, REQ-013, REQ-014, REQ-015 | mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthSigningKeyPersistenceContractTest -Dsurefire.failIfNoSpecifiedTests=false test |
| CREATE | egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/repository/OAuthRevocationRepository.java | 当前不存在；Spec §8/§11 明确目标，邻近包可验证 | OAuthRevocationRepository | Service-facing typed contract | Step 6 | REQ-012, REQ-013, REQ-014, REQ-015 | mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthSigningKeyPersistenceContractTest -Dsurefire.failIfNoSpecifiedTests=false test |
| CREATE | egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/audit/domain/po/OAuthAuditPO.java | 当前不存在；Spec §8/§11 明确目标，邻近包可验证 | OAuthAuditPO | EgonModel承载本表完整列 | Step 6 | REQ-012, REQ-013, REQ-014, REQ-015 | mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthSigningKeyPersistenceContractTest -Dsurefire.failIfNoSpecifiedTests=false test |
| CREATE | egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/audit/domain/bo/OAuthAuditBO.java | 当前不存在；Spec §8/§11 明确目标，邻近包可验证 | OAuthAuditBO | Service计算与PO分界 | Step 6 | REQ-012, REQ-013, REQ-014, REQ-015 | mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthSigningKeyPersistenceContractTest -Dsurefire.failIfNoSpecifiedTests=false test |
| CREATE | egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/audit/repository/OAuthAuditRepository.java | 当前不存在；Spec §8/§11 明确目标，邻近包可验证 | OAuthAuditRepository | Service-facing typed contract | Step 6 | REQ-012, REQ-013, REQ-014, REQ-015 | mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthSigningKeyPersistenceContractTest -Dsurefire.failIfNoSpecifiedTests=false test |
| CREATE | egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/repository/OAuthSigningKeyPersistenceContractTest.java | 当前不存在；Spec §8/§11 明确目标，邻近包可验证 | OAuthSigningKeyPersistenceContractTest | RED证明这组映射/查询/跨租户错误 | Step 6 | REQ-012, REQ-013, REQ-014, REQ-015 | mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthSigningKeyPersistenceContractTest -Dsurefire.failIfNoSpecifiedTests=false test |
| CREATE | egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/domain/converter/OAuthSigningKeyConverter.java | 当前不存在；Spec §8/§11 明确目标，邻近包可验证 | OAuthSigningKeyConverter | PO↔BO双向MapStruct转换 | Step 6 | REQ-012, REQ-013, REQ-014, REQ-015 | mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthSigningKeyPersistenceContractTest -Dsurefire.failIfNoSpecifiedTests=false test |
| CREATE | egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/dao/OAuthSigningKeyMapper.java | 当前不存在；Spec §8/§11 明确目标，邻近包可验证 | OAuthSigningKeyMapper | MP Mapper具名SQL接口 | Step 6 | REQ-012, REQ-013, REQ-014, REQ-015 | mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthSigningKeyPersistenceContractTest -Dsurefire.failIfNoSpecifiedTests=false test |
| CREATE | egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/resources/mapper/token/OAuthSigningKeyMapper.xml | 当前不存在；Spec §8/§11 明确目标，邻近包可验证 | OAuthSigningKeyMapper SQL | 具名PostgreSQL谓词/锁 | Step 6 | REQ-012, REQ-013, REQ-014, REQ-015 | mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthSigningKeyPersistenceContractTest -Dsurefire.failIfNoSpecifiedTests=false test |
| CREATE | egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/repository/impl/OAuthSigningKeyRepositoryImpl.java | 当前不存在；Spec §8/§11 明确目标，邻近包可验证 | OAuthSigningKeyRepositoryImpl | MP repository ACL | Step 6 | REQ-012, REQ-013, REQ-014, REQ-015 | mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthSigningKeyPersistenceContractTest -Dsurefire.failIfNoSpecifiedTests=false test |
| CREATE | egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/domain/converter/OAuthSecurityStateConverter.java | 当前不存在；Spec §8/§11 明确目标，邻近包可验证 | OAuthSecurityStateConverter | PO↔BO双向MapStruct转换 | Step 6 | REQ-012, REQ-013, REQ-014, REQ-015 | mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthSigningKeyPersistenceContractTest -Dsurefire.failIfNoSpecifiedTests=false test |
| CREATE | egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/dao/OAuthSecurityStateMapper.java | 当前不存在；Spec §8/§11 明确目标，邻近包可验证 | OAuthSecurityStateMapper | MP Mapper具名SQL接口 | Step 6 | REQ-012, REQ-013, REQ-014, REQ-015 | mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthSigningKeyPersistenceContractTest -Dsurefire.failIfNoSpecifiedTests=false test |
| CREATE | egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/resources/mapper/token/OAuthSecurityStateMapper.xml | 当前不存在；Spec §8/§11 明确目标，邻近包可验证 | OAuthSecurityStateMapper SQL | 具名PostgreSQL谓词/锁 | Step 6 | REQ-012, REQ-013, REQ-014, REQ-015 | mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthSigningKeyPersistenceContractTest -Dsurefire.failIfNoSpecifiedTests=false test |
| CREATE | egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/repository/impl/OAuthSecurityStateRepositoryImpl.java | 当前不存在；Spec §8/§11 明确目标，邻近包可验证 | OAuthSecurityStateRepositoryImpl | MP repository ACL | Step 6 | REQ-012, REQ-013, REQ-014, REQ-015 | mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthSigningKeyPersistenceContractTest -Dsurefire.failIfNoSpecifiedTests=false test |
| CREATE | egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/domain/converter/OAuthRevocationConverter.java | 当前不存在；Spec §8/§11 明确目标，邻近包可验证 | OAuthRevocationConverter | PO↔BO双向MapStruct转换 | Step 6 | REQ-012, REQ-013, REQ-014, REQ-015 | mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthSigningKeyPersistenceContractTest -Dsurefire.failIfNoSpecifiedTests=false test |
| CREATE | egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/dao/OAuthRevocationMapper.java | 当前不存在；Spec §8/§11 明确目标，邻近包可验证 | OAuthRevocationMapper | MP Mapper具名SQL接口 | Step 6 | REQ-012, REQ-013, REQ-014, REQ-015 | mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthSigningKeyPersistenceContractTest -Dsurefire.failIfNoSpecifiedTests=false test |
| CREATE | egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/resources/mapper/token/OAuthRevocationMapper.xml | 当前不存在；Spec §8/§11 明确目标，邻近包可验证 | OAuthRevocationMapper SQL | 具名PostgreSQL谓词/锁 | Step 6 | REQ-012, REQ-013, REQ-014, REQ-015 | mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthSigningKeyPersistenceContractTest -Dsurefire.failIfNoSpecifiedTests=false test |
| CREATE | egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/repository/impl/OAuthRevocationRepositoryImpl.java | 当前不存在；Spec §8/§11 明确目标，邻近包可验证 | OAuthRevocationRepositoryImpl | MP repository ACL | Step 6 | REQ-012, REQ-013, REQ-014, REQ-015 | mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthSigningKeyPersistenceContractTest -Dsurefire.failIfNoSpecifiedTests=false test |
| CREATE | egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/audit/domain/converter/OAuthAuditConverter.java | 当前不存在；Spec §8/§11 明确目标，邻近包可验证 | OAuthAuditConverter | PO↔BO双向MapStruct转换 | Step 6 | REQ-012, REQ-013, REQ-014, REQ-015 | mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthSigningKeyPersistenceContractTest -Dsurefire.failIfNoSpecifiedTests=false test |
| CREATE | egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/audit/dao/OAuthAuditMapper.java | 当前不存在；Spec §8/§11 明确目标，邻近包可验证 | OAuthAuditMapper | MP Mapper具名SQL接口 | Step 6 | REQ-012, REQ-013, REQ-014, REQ-015 | mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthSigningKeyPersistenceContractTest -Dsurefire.failIfNoSpecifiedTests=false test |
| CREATE | egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/resources/mapper/audit/OAuthAuditMapper.xml | 当前不存在；Spec §8/§11 明确目标，邻近包可验证 | OAuthAuditMapper SQL | 具名PostgreSQL谓词/锁 | Step 6 | REQ-012, REQ-013, REQ-014, REQ-015 | mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthSigningKeyPersistenceContractTest -Dsurefire.failIfNoSpecifiedTests=false test |
| CREATE | egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/audit/repository/impl/OAuthAuditRepositoryImpl.java | 当前不存在；Spec §8/§11 明确目标，邻近包可验证 | OAuthAuditRepositoryImpl | MP repository ACL | Step 6 | REQ-012, REQ-013, REQ-014, REQ-015 | mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthSigningKeyPersistenceContractTest -Dsurefire.failIfNoSpecifiedTests=false test |
| CREATE | egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/TenantIssuerIsolationTest.java | 当前不存在；Spec §8/§11 明确目标，邻近包可验证 | isolatesSameClientAndKidAcrossIssuers | RED A/B同clientId/kid互相拒绝 | Step 7 | REQ-001, REQ-002, REQ-004, REQ-013, REQ-016 | mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=TenantIssuerIsolationTest -Dsurefire.failIfNoSpecifiedTests=false test |
| CREATE | egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/service/TenantIssuerService.java | 当前不存在；Spec §8/§11 明确目标，邻近包可验证 | resolveExactIssuer(URI) | 业务分层的registry只读接口 | Step 7 | REQ-001, REQ-002, REQ-004, REQ-013, REQ-016 | mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=TenantIssuerIsolationTest -Dsurefire.failIfNoSpecifiedTests=false test |
| CREATE | egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/service/impl/TenantIssuerServiceImpl.java | 当前不存在；Spec §8/§11 明确目标，邻近包可验证 | tenantIssuerService | exact issuer registry Adapter | Step 7 | REQ-001, REQ-002, REQ-004, REQ-013, REQ-016 | mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=TenantIssuerIsolationTest -Dsurefire.failIfNoSpecifiedTests=false test |
| CREATE | egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/repository/TenantRegisteredClientRepository.java | 当前不存在；Spec §8/§11 明确目标，邻近包可验证 | tenantRegisteredClientRepository | perissuer RegisteredClientRepository SPI | Step 7 | REQ-001, REQ-002, REQ-004, REQ-013, REQ-016 | mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=TenantIssuerIsolationTest -Dsurefire.failIfNoSpecifiedTests=false test |
| CREATE | egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/service/impl/TenantAuthorizationServiceImpl.java | 当前不存在；Spec §8/§11 明确目标，邻近包可验证 | tenantAuthorizationService | SAS OAuth2AuthorizationService SPI | Step 7 | REQ-001, REQ-002, REQ-004, REQ-013, REQ-016 | mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=TenantIssuerIsolationTest -Dsurefire.failIfNoSpecifiedTests=false test |
| CREATE | egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/service/impl/TenantConsentServiceImpl.java | 当前不存在；Spec §8/§11 明确目标，邻近包可验证 | tenantConsentService | SAS Consent SPI | Step 7 | REQ-001, REQ-002, REQ-004, REQ-013, REQ-016 | mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=TenantIssuerIsolationTest -Dsurefire.failIfNoSpecifiedTests=false test |
| CREATE | egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/service/impl/TenantJwkSourceComponent.java | 当前不存在；Spec §8/§11 明确目标，邻近包可验证 | tenantJwkSourceComponent | active private signer + all allowed public verification keys | Step 7 | REQ-001, REQ-002, REQ-004, REQ-013, REQ-016 | mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=TenantIssuerIsolationTest -Dsurefire.failIfNoSpecifiedTests=false test |
| CREATE→MODIFY | egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/config/AuthorizationServerConfiguration.java | 当前不存在；Spec §8/§11 明确目标，邻近包可验证 | token endpoint provider list（前次authorizationServerSettings/SAS SecurityFilterChain） | SAS+OIDC Filter ordering | Step 7, Step 8 | REQ-001, REQ-002, REQ-004, REQ-013, REQ-016, REQ-003, REQ-004, REQ-005, REQ-011 | mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=TenantIssuerIsolationTest -Dsurefire.failIfNoSpecifiedTests=false test, mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthOidcProtocolContractTest -Dsurefire.failIfNoSpecifiedTests=false test |
| CREATE | egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/config/OAuthProtocolOpenApiConfiguration.java | 当前不存在；Spec §8/§11 明确目标，邻近包可验证 | openapi customizer | document true SAS Filter endpoints | Step 7 | REQ-001, REQ-002, REQ-004, REQ-013, REQ-016 | mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=TenantIssuerIsolationTest -Dsurefire.failIfNoSpecifiedTests=false test |
| CREATE | egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/controller/OAuthOidcProtocolContractTest.java | 当前不存在；Spec §8/§11 明确目标，邻近包可验证 | codeS256RedirectNonceReplay | RED缺code/nonce拒绝 | Step 8 | REQ-003, REQ-004, REQ-005, REQ-011 | mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthOidcProtocolContractTest -Dsurefire.failIfNoSpecifiedTests=false test |
| CREATE | egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/service/OAuthTokenIssueService.java | 当前不存在；Spec §8/§11 明确目标，邻近包可验证 | consumeAndIssue(TrustedRealmBO,ConsumeOAuthTokenCommand) | SAS业务侧发行约束 | Step 8 | REQ-003, REQ-004, REQ-005, REQ-011 | mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthOidcProtocolContractTest -Dsurefire.failIfNoSpecifiedTests=false test |
| CREATE | egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/service/impl/OAuthTokenIssueServiceImpl.java | 当前不存在；Spec §8/§11 明确目标，邻近包可验证 | consumeAndIssue | 签发编排与事务状态守卫 | Step 8 | REQ-003, REQ-004, REQ-005, REQ-011 | mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthOidcProtocolContractTest -Dsurefire.failIfNoSpecifiedTests=false test |
| CREATE | egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/service/impl/TransactionalOAuthAuthenticationProvider.java | 当前不存在；Spec §8/§11 明确目标，邻近包可验证 | authorizeCode/refresh/clientCredentials wrappers | 保持SAS原协议处理但同事务校验 | Step 8 | REQ-003, REQ-004, REQ-005, REQ-011 | mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthOidcProtocolContractTest -Dsurefire.failIfNoSpecifiedTests=false test |
| CREATE | egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/controller/TenantLoginController.java | 当前不存在；Spec §8/§11 明确目标，邻近包可验证 | GET /tenants/{tenantId}/login | 呈现现有IdentityFacade人员认证页 | Step 8 | REQ-003, REQ-004, REQ-005, REQ-011 | mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthOidcProtocolContractTest -Dsurefire.failIfNoSpecifiedTests=false test |
| CREATE | egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/controller/filter/TenantLoginAuthenticationFilter.java | 当前不存在；Spec §8/§11 明确目标，邻近包可验证 | POST /tenants/{tenantId}/login | 使用真实密码/锁定/membership规则 | Step 8 | REQ-003, REQ-004, REQ-005, REQ-011 | mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthOidcProtocolContractTest -Dsurefire.failIfNoSpecifiedTests=false test |
| MODIFY | egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/controller/OAuthClientControllerTest.java | 当前文件存在：egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/controller/OAuthClientControllerTest.java | realmIsolatedBcryptRotation | RED现有全局client/Argon2路径 | Step 9 | REQ-002, REQ-005, REQ-011, REQ-013, REQ-017 | mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthClientControllerTest,SigningKeyServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test |
| MODIFY | egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/domain/dto/CreateOAuthClientDTO.java | 当前文件存在：egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/domain/dto/CreateOAuthClientDTO.java | purpose/allowedScopes groups | 管理输入按原wire扩展 | Step 9 | REQ-002, REQ-005, REQ-011, REQ-013, REQ-017 | mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthClientControllerTest,SigningKeyServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test |
| MODIFY | egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/domain/dto/UpdateOAuthClientDTO.java | 当前文件存在：egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/domain/dto/UpdateOAuthClientDTO.java | allowedScopes optional | 版本写与scope收缩 | Step 9 | REQ-002, REQ-005, REQ-011, REQ-013, REQ-017 | mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthClientControllerTest,SigningKeyServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test |
| MODIFY | egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/service/impl/OAuthClientServiceImpl.java | 当前文件存在：egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/service/impl/OAuthClientServiceImpl.java | create/rotate/update/redirects | MP ClientRepository + SecretEncoder | Step 9 | REQ-002, REQ-005, REQ-011, REQ-013, REQ-017 | mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthClientControllerTest,SigningKeyServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test |
| MODIFY | egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/controller/OAuthClientController.java | 当前文件存在：egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/controller/OAuthClientController.java | API-020..027 | 按已验principal传realm并保留原URL/权限 | Step 9 | REQ-002, REQ-005, REQ-011, REQ-013, REQ-017 | mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthClientControllerTest,SigningKeyServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test |
| MODIFY | egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/resource/service/impl/ResourceServerServiceImpl.java | 当前文件存在：egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/resource/service/impl/ResourceServerServiceImpl.java | resource create/status/grants | MP OAuthResource/Grant Repository | Step 9 | REQ-002, REQ-005, REQ-011, REQ-013, REQ-017 | mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthClientControllerTest,SigningKeyServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test |
| CREATE | egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/resource/repository/MpResourceServerStore.java | 当前不存在；Spec §8/§11 明确目标，邻近包可验证 | ResourceServerStore adapter | 替代JpaResourceServerStore访问OAuthResource | Step 9 | REQ-002, REQ-005, REQ-011, REQ-013, REQ-017 | mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthClientControllerTest,SigningKeyServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test |
| MODIFY | egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/service/impl/SigningKeyServiceImpl.java | 当前文件存在：egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/service/impl/SigningKeyServiceImpl.java | list/publish/activate/retire | perissuer keyring与版本守卫 | Step 9 | REQ-002, REQ-005, REQ-011, REQ-013, REQ-017 | mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthClientControllerTest,SigningKeyServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test |
| MODIFY | egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/controller/SigningKeyController.java | 当前文件存在：egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/controller/SigningKeyController.java | API-031..034 | 原URL按realm作用域增校验/文档 | Step 9 | REQ-002, REQ-005, REQ-011, REQ-013, REQ-017 | mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthClientControllerTest,SigningKeyServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test |
| CREATE | egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/tianquan/shoubing/admin/token/TokenRevocationServiceTest.java | 当前不存在；Spec §8/§11 明确目标，邻近包可验证 | revokeAckAndFenceRecovery | RED先固定DB/Redis顺序与failclosed | Step 10 | REQ-012, REQ-013, REQ-014, REQ-015, REQ-016 | mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=TokenRevocationServiceTest -Dsurefire.failIfNoSpecifiedTests=false test |
| CREATE | egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/service/impl/OAuthSecurityProjectionComponent.java | 当前不存在；Spec §8/§11 明确目标，邻近包可验证 | oauthSecurityProjectionComponent | 单调Redis Lua状态与ready/fence | Step 10 | REQ-012, REQ-013, REQ-014, REQ-015, REQ-016 | mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=TokenRevocationServiceTest -Dsurefire.failIfNoSpecifiedTests=false test |
| CREATE | egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/service/impl/TokenRevocationServiceImpl.java | 当前不存在；Spec §8/§11 明确目标，邻近包可验证 | revoke/revokeSubject | 先DB事实/Outbox后Redis可见确认 | Step 10 | REQ-012, REQ-013, REQ-014, REQ-015, REQ-016 | mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=TokenRevocationServiceTest -Dsurefire.failIfNoSpecifiedTests=false test |
| CREATE | egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/support/outbox/OAuthSecurityDeliveryHandler.java | 当前不存在；Spec §8/§11 明确目标，邻近包可验证 | oauthSecurityDeliveryHandler | 真实Outbox投影消费 | Step 10 | REQ-012, REQ-013, REQ-014, REQ-015, REQ-016 | mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=TokenRevocationServiceTest -Dsurefire.failIfNoSpecifiedTests=false test |
| CREATE | egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/service/impl/OAuthSigningKeyRotationServiceImpl.java | 当前不存在；Spec §8/§11 明确目标，邻近包可验证 | rotateDueRealms | 定时90天生命周期 | Step 10 | REQ-012, REQ-013, REQ-014, REQ-015, REQ-016 | mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=TokenRevocationServiceTest -Dsurefire.failIfNoSpecifiedTests=false test |
| CREATE | egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/tianquan/shoubing/starter/audit/TokenUsageAuditComponent.java | 当前不存在；Spec §8/§11 明确目标，邻近包可验证 | tokenUsageAuditComponent | 无DB网关/RS本地持久审核再发Redis Streams | Step 10 | REQ-012, REQ-013, REQ-014, REQ-015, REQ-016 | mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=TokenRevocationServiceTest -Dsurefire.failIfNoSpecifiedTests=false test |
| CREATE | egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/audit/service/impl/OAuthAuditConsumer.java | 当前不存在；Spec §8/§11 明确目标，邻近包可验证 | oauthAuditConsumer | Streams MQ消费到OAuthAuditPO | Step 10 | REQ-012, REQ-013, REQ-014, REQ-015, REQ-016 | mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=TokenRevocationServiceTest -Dsurefire.failIfNoSpecifiedTests=false test |
| MODIFY | egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/identity/service/impl/IdentityUserServiceImpl.java | 当前文件存在：egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/identity/service/impl/IdentityUserServiceImpl.java | revokeAll/password change callback | 旧账户安全变更接新安全世代 | Step 10 | REQ-012, REQ-013, REQ-014, REQ-015, REQ-016 | mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=TokenRevocationServiceTest -Dsurefire.failIfNoSpecifiedTests=false test |
| CREATE | egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/test/java/top/egon/cola/platform/tianquan/shoubing/starter/security/MultiIssuerResourceServerTest.java | 当前不存在；Spec §8/§11 明确目标，邻近包可验证 | twoIssuersWrongAudAndCleanup | RED当前Verifier单issuer/tid | Step 11 | REQ-002, REQ-006, REQ-007, REQ-008, REQ-012, REQ-016 | mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter -am -Dtest=MultiIssuerResourceServerTest -Dsurefire.failIfNoSpecifiedTests=false test |
| CREATE | egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/tianquan/shoubing/starter/security/TenantIssuerAuthenticationManagerComponent.java | 当前不存在；Spec §8/§11 明确目标，邻近包可验证 | tenantIssuerAuthenticationManagerComponent | 精确可信issuer resolver | Step 11 | REQ-002, REQ-006, REQ-007, REQ-008, REQ-012, REQ-016 | mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter -am -Dtest=MultiIssuerResourceServerTest -Dsurefire.failIfNoSpecifiedTests=false test |
| MODIFY | egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/tianquan/shoubing/starter/security/IdpJwtVerifier.java | 当前文件存在：egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/tianquan/shoubing/starter/security/IdpJwtVerifier.java | verifyUser/verifyService | 从新claims建原IdpPrincipal | Step 11 | REQ-002, REQ-006, REQ-007, REQ-008, REQ-012, REQ-016 | mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter -am -Dtest=MultiIssuerResourceServerTest -Dsurefire.failIfNoSpecifiedTests=false test |
| CREATE | egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/tianquan/shoubing/starter/security/VerifiedTenantContextFilter.java | 当前不存在；Spec §8/§11 明确目标，邻近包可验证 | doFilterInternal/finally | 在Spring Security验证后绑定旧TenantContext+MDC | Step 11 | REQ-002, REQ-006, REQ-007, REQ-008, REQ-012, REQ-016 | mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter -am -Dtest=MultiIssuerResourceServerTest -Dsurefire.failIfNoSpecifiedTests=false test |
| CREATE | egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/tianquan/shoubing/starter/security/LegacyIssuerCutoverPolicy.java | 当前不存在；Spec §8/§11 明确目标，邻近包可验证 | isLegacyTenantAllowed | 旧issuer按已签名tid阻断已切tenant | Step 11 | REQ-002, REQ-006, REQ-007, REQ-008, REQ-012, REQ-016 | mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter -am -Dtest=MultiIssuerResourceServerTest -Dsurefire.failIfNoSpecifiedTests=false test |
| MODIFY | egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/tianquan/shoubing/starter/autoconfigure/IdpStarterAutoConfiguration.java | 当前文件存在：egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/tianquan/shoubing/starter/autoconfigure/IdpStarterAutoConfiguration.java | security beans order and properties | 注册可信resolver/验证器/审计Reader/Filter | Step 11 | REQ-002, REQ-006, REQ-007, REQ-008, REQ-012, REQ-016 | mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter -am -Dtest=MultiIssuerResourceServerTest -Dsurefire.failIfNoSpecifiedTests=false test |
| CREATE | egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/lombok.config | 当前不存在；Spec §8/§11 明确目标，邻近包可验证 | Qualifier propagation | 对Starter新增Bean满足注入规范 | Step 11 | REQ-002, REQ-006, REQ-007, REQ-008, REQ-012, REQ-016 | mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter -am -Dtest=MultiIssuerResourceServerTest -Dsurefire.failIfNoSpecifiedTests=false test |
| MODIFY | egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/test/java/top/egon/cola/platform/tianquan/shoubing/starter/client/IdpServiceOAuth2ClientTest.java | 当前文件存在：egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/test/java/top/egon/cola/platform/tianquan/shoubing/starter/client/IdpServiceOAuth2ClientTest.java | renewAtSkewAndSingleFlightByCompleteKey | RED原key不含issuer/credentialVersion | Step 12 | REQ-005, REQ-009, REQ-011, REQ-016 | mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter -am -Dtest=IdpServiceOAuth2ClientTest -Dsurefire.failIfNoSpecifiedTests=false test |
| MODIFY | egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/tianquan/shoubing/starter/client/ServiceAuthorizationKey.java | 当前文件存在：egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/tianquan/shoubing/starter/client/ServiceAuthorizationKey.java | issuer/credentialVersion fields | 扩充缓存与AuthorizedClient分区身份 | Step 12 | REQ-005, REQ-009, REQ-011, REQ-016 | mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter -am -Dtest=IdpServiceOAuth2ClientTest -Dsurefire.failIfNoSpecifiedTests=false test |
| MODIFY | egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/tianquan/shoubing/starter/client/IdpServiceOAuth2Client.java | 当前文件存在：egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/tianquan/shoubing/starter/client/IdpServiceOAuth2Client.java | authorize(IdpServiceTokenRequest) | 复用既有AuthorizedClientManager+singleFlight | Step 12 | REQ-005, REQ-009, REQ-011, REQ-016 | mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter -am -Dtest=IdpServiceOAuth2ClientTest -Dsurefire.failIfNoSpecifiedTests=false test |
| CREATE | egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/tianquan/shoubing/starter/client/ServiceTokenWebClientConfiguration.java | 当前不存在；Spec §8/§11 明确目标，邻近包可验证 | serviceTokenWebClient | 只给受信Provider装配Bearer filter | Step 12 | REQ-005, REQ-009, REQ-011, REQ-016 | mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter -am -Dtest=IdpServiceOAuth2ClientTest -Dsurefire.failIfNoSpecifiedTests=false test |
| CREATE | egon-cola-xingyuan/egon-cola-yuheng/yuheng-edge-gateway/src/test/java/top/egon/cola/component/yuheng/edge/EdgeLoginContractTest.java | 当前不存在；Spec §8/§11 明确目标，邻近包可验证 | stateNonceTenantHeadersSession | RED新edge尚无oauth2Login/BFF | Step 13 | REQ-003, REQ-004, REQ-006, REQ-010, REQ-011, REQ-012 | mvn -o -pl egon-cola-xingyuan/egon-cola-yuheng/yuheng-edge-gateway -am -Dtest=EdgeLoginContractTest -Dsurefire.failIfNoSpecifiedTests=false test |
| CREATE | egon-cola-xingyuan/egon-cola-yuheng/yuheng-edge-gateway/src/main/java/top/egon/cola/component/yuheng/edge/config/EdgeOAuthProperties.java | 当前不存在；Spec §8/§11 明确目标，邻近包可验证 | edgeOAuthProperties | typed validated origin/upstream/return paths | Step 13 | REQ-003, REQ-004, REQ-006, REQ-010, REQ-011, REQ-012 | mvn -o -pl egon-cola-xingyuan/egon-cola-yuheng/yuheng-edge-gateway -am -Dtest=EdgeLoginContractTest -Dsurefire.failIfNoSpecifiedTests=false test |
| CREATE | egon-cola-xingyuan/egon-cola-yuheng/yuheng-edge-gateway/src/main/java/top/egon/cola/component/yuheng/edge/service/impl/EdgeAuthorizedClientServiceImpl.java | 当前不存在；Spec §8/§11 明确目标，邻近包可验证 | edgeAuthorizedClientService | 会话绑定issuer/tenant/client的AuthorizedClient | Step 13 | REQ-003, REQ-004, REQ-006, REQ-010, REQ-011, REQ-012 | mvn -o -pl egon-cola-xingyuan/egon-cola-yuheng/yuheng-edge-gateway -am -Dtest=EdgeLoginContractTest -Dsurefire.failIfNoSpecifiedTests=false test |
| CREATE | egon-cola-xingyuan/egon-cola-yuheng/yuheng-edge-gateway/src/main/java/top/egon/cola/component/yuheng/edge/filter/TrustedIdentityHeaderFilter.java | 当前不存在；Spec §8/§11 明确目标，邻近包可验证 | trustedIdentityHeaderFilter | 清除伪造身份头仅写非权威追踪头 | Step 13 | REQ-003, REQ-004, REQ-006, REQ-010, REQ-011, REQ-012 | mvn -o -pl egon-cola-xingyuan/egon-cola-yuheng/yuheng-edge-gateway -am -Dtest=EdgeLoginContractTest -Dsurefire.failIfNoSpecifiedTests=false test |
| CREATE | egon-cola-xingyuan/egon-cola-yuheng/yuheng-edge-gateway/src/main/java/top/egon/cola/component/yuheng/edge/controller/EdgeSessionController.java | 当前不存在；Spec §8/§11 明确目标，邻近包可验证 | bff/session,csrf,logout | BFF会话REST协议 | Step 13 | REQ-003, REQ-004, REQ-006, REQ-010, REQ-011, REQ-012 | mvn -o -pl egon-cola-xingyuan/egon-cola-yuheng/yuheng-edge-gateway -am -Dtest=EdgeLoginContractTest -Dsurefire.failIfNoSpecifiedTests=false test |
| CREATE | egon-cola-xingyuan/egon-cola-yuheng/yuheng-edge-gateway/src/main/java/top/egon/cola/component/yuheng/edge/config/EdgeSecurityConfiguration.java | 当前不存在；Spec §8/§11 明确目标，邻近包可验证 | SecurityWebFilterChain/oauth2Login routes | code+S256+strict BFF session | Step 13 | REQ-003, REQ-004, REQ-006, REQ-010, REQ-011, REQ-012 | mvn -o -pl egon-cola-xingyuan/egon-cola-yuheng/yuheng-edge-gateway -am -Dtest=EdgeLoginContractTest -Dsurefire.failIfNoSpecifiedTests=false test |
| CREATE | egon-cola-xingyuan/egon-cola-yuheng/yuheng-edge-gateway/src/main/resources/application.yml | 当前不存在；Spec §8/§11 明确目标，邻近包可验证 | session/oauth/upstream/TLS keys | 全配置base结构 | Step 13 | REQ-003, REQ-004, REQ-006, REQ-010, REQ-011, REQ-012 | mvn -o -pl egon-cola-xingyuan/egon-cola-yuheng/yuheng-edge-gateway -am -Dtest=EdgeLoginContractTest -Dsurefire.failIfNoSpecifiedTests=false test |
| CREATE | egon-cola-xingyuan/egon-cola-yuheng/yuheng-edge-gateway/src/main/resources/application-local.yml | 当前不存在；Spec §8/§11 明确目标，邻近包可验证 | identical profile key set | 本地HTTPS测试值 | Step 13 | REQ-003, REQ-004, REQ-006, REQ-010, REQ-011, REQ-012 | mvn -o -pl egon-cola-xingyuan/egon-cola-yuheng/yuheng-edge-gateway -am -Dtest=EdgeLoginContractTest -Dsurefire.failIfNoSpecifiedTests=false test |
| MODIFY | egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-gateway-adapter/src/test/java/top/egon/cola/platform/tianquan/shoubing/yuheng/security/IdpGatewaySecurityProviderTest.java | 当前文件存在：egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-gateway-adapter/src/test/java/top/egon/cola/platform/tianquan/shoubing/yuheng/security/IdpGatewaySecurityProviderTest.java | newBearerNoCookieRecovery | RED旧网关仍自动刷新USER Cookie | Step 14 | REQ-001, REQ-005, REQ-006, REQ-007, REQ-010, REQ-016 | mvn -o -pl egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin,egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-starter,egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-gateway-adapter -am -Dtest=IdpGatewaySecurityProviderTest,IdpJwtDdcRegistrationCredentialVerifierTest -Dsurefire.failIfNoSpecifiedTests=false test |
| MODIFY | egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-gateway-adapter/src/main/java/top/egon/cola/platform/tianquan/shoubing/yuheng/security/IdpIdentityAuthenticationProvider.java | 当前文件存在：egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-gateway-adapter/src/main/java/top/egon/cola/platform/tianquan/shoubing/yuheng/security/IdpIdentityAuthenticationProvider.java | Bearer-only identity | 玉衡身份Provider使用Starter多issuer验证 | Step 14 | REQ-001, REQ-005, REQ-006, REQ-007, REQ-010, REQ-016 | mvn -o -pl egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin,egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-starter,egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-gateway-adapter -am -Dtest=IdpGatewaySecurityProviderTest,IdpJwtDdcRegistrationCredentialVerifierTest -Dsurefire.failIfNoSpecifiedTests=false test |
| MODIFY | egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-gateway-adapter/src/main/java/top/egon/cola/platform/tianquan/shoubing/yuheng/security/IdpUserCredentialRecoveryProvider.java | 当前文件存在：egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-gateway-adapter/src/main/java/top/egon/cola/platform/tianquan/shoubing/yuheng/security/IdpUserCredentialRecoveryProvider.java | remove USER Cookie RT recovery | 新入口交SCG | Step 14 | REQ-001, REQ-005, REQ-006, REQ-007, REQ-010, REQ-016 | mvn -o -pl egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin,egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-starter,egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-gateway-adapter -am -Dtest=IdpGatewaySecurityProviderTest,IdpJwtDdcRegistrationCredentialVerifierTest -Dsurefire.failIfNoSpecifiedTests=false test |
| MODIFY | egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-starter/src/main/java/top/egon/cola/platform/tianquan/jianshen/starter/security/Rbac3BearerAuthenticationFilter.java | 当前文件存在：egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-starter/src/main/java/top/egon/cola/platform/tianquan/jianshen/starter/security/Rbac3BearerAuthenticationFilter.java | IdentityPrincipal bridge | 已验JWT仍进入鉴神UserDetails/snapshot | Step 14 | REQ-001, REQ-005, REQ-006, REQ-007, REQ-010, REQ-016 | mvn -o -pl egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin,egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-starter,egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-gateway-adapter -am -Dtest=IdpGatewaySecurityProviderTest,IdpJwtDdcRegistrationCredentialVerifierTest -Dsurefire.failIfNoSpecifiedTests=false test |
| MODIFY | egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin/src/main/java/top/egon/cola/platform/tianquan/jianshen/admin/shared/tenant/controller/filter/TenantContextFilter.java | 当前文件存在：egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin/src/main/java/top/egon/cola/platform/tianquan/jianshen/admin/shared/tenant/controller/filter/TenantContextFilter.java | finally clear or restore MDC | 配合Starter一次可信上下文绑定 | Step 14 | REQ-001, REQ-005, REQ-006, REQ-007, REQ-010, REQ-016 | mvn -o -pl egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin,egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-starter,egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-gateway-adapter -am -Dtest=IdpGatewaySecurityProviderTest,IdpJwtDdcRegistrationCredentialVerifierTest -Dsurefire.failIfNoSpecifiedTests=false test |
| MODIFY | egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/java/top/egon/cola/component/tianshu/admin/security/registration/IdpJwtDdcRegistrationCredentialVerifier.java | 当前文件存在：egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/java/top/egon/cola/component/tianshu/admin/security/registration/IdpJwtDdcRegistrationCredentialVerifier.java | PLATFORM issuer resource source guard | 天枢注册维持独立platform token | Step 14 | REQ-001, REQ-005, REQ-006, REQ-007, REQ-010, REQ-016 | mvn -o -pl egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin,egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-starter,egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-gateway-adapter -am -Dtest=IdpGatewaySecurityProviderTest,IdpJwtDdcRegistrationCredentialVerifierTest -Dsurefire.failIfNoSpecifiedTests=false test |
| MODIFY | egon-cola-xingyuan/egon-cola-xingyuan-admin-web-shared/src/auth/gatewayAuthClient.test.ts | 当前文件存在：egon-cola-xingyuan/egon-cola-xingyuan-admin-web-shared/src/auth/gatewayAuthClient.test.ts | gatewayAuthClient.test | 把现有前端登录消费者迁到BFF，保留业务页面 | Step 15 | REQ-003, REQ-004, REQ-010, REQ-017 | npm --prefix egon-cola-xingyuan/egon-cola-xingyuan-admin-web-shared run test -- src/auth/gatewayAuthClient.test.ts |
| MODIFY | egon-cola-xingyuan/egon-cola-xingyuan-admin-web-shared/src/auth/gatewayAuthClient.ts | 当前文件存在：egon-cola-xingyuan/egon-cola-xingyuan-admin-web-shared/src/auth/gatewayAuthClient.ts | gatewayAuthClient | 把现有前端登录消费者迁到BFF，保留业务页面 | Step 15 | REQ-003, REQ-004, REQ-010, REQ-017 | npm --prefix egon-cola-xingyuan/egon-cola-xingyuan-admin-web-shared run test -- src/auth/gatewayAuthClient.test.ts |
| MODIFY | egon-cola-xingyuan/egon-cola-xingyuan-admin-portal/src/auth/portalAuth.ts | 当前文件存在：egon-cola-xingyuan/egon-cola-xingyuan-admin-portal/src/auth/portalAuth.ts | portalAuth | 把现有前端登录消费者迁到BFF，保留业务页面 | Step 15 | REQ-003, REQ-004, REQ-010, REQ-017 | npm --prefix egon-cola-xingyuan/egon-cola-xingyuan-admin-web-shared run test -- src/auth/gatewayAuthClient.test.ts |
| MODIFY | egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin-web/src/auth/AuthContext.tsx | 当前文件存在：egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin-web/src/auth/AuthContext.tsx | AuthContext | 把现有前端登录消费者迁到BFF，保留业务页面 | Step 15 | REQ-003, REQ-004, REQ-010, REQ-017 | npm --prefix egon-cola-xingyuan/egon-cola-xingyuan-admin-web-shared run test -- src/auth/gatewayAuthClient.test.ts |
| MODIFY | egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin-web/src/auth/CentralLoginPage.tsx | 当前文件存在：egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin-web/src/auth/CentralLoginPage.tsx | CentralLoginPage | 把现有前端登录消费者迁到BFF，保留业务页面 | Step 15 | REQ-003, REQ-004, REQ-010, REQ-017 | npm --prefix egon-cola-xingyuan/egon-cola-xingyuan-admin-web-shared run test -- src/auth/gatewayAuthClient.test.ts |
| MODIFY | egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin-web/src/auth/AuthContext.tsx | 当前文件存在：egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin-web/src/auth/AuthContext.tsx | AuthContext | 把现有前端登录消费者迁到BFF，保留业务页面 | Step 15 | REQ-003, REQ-004, REQ-010, REQ-017 | npm --prefix egon-cola-xingyuan/egon-cola-xingyuan-admin-web-shared run test -- src/auth/gatewayAuthClient.test.ts |
| MODIFY | egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin-web/src/auth/LoginPage.tsx | 当前文件存在：egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin-web/src/auth/LoginPage.tsx | LoginPage | 把现有前端登录消费者迁到BFF，保留业务页面 | Step 15 | REQ-003, REQ-004, REQ-010, REQ-017 | npm --prefix egon-cola-xingyuan/egon-cola-xingyuan-admin-web-shared run test -- src/auth/gatewayAuthClient.test.ts |
| MODIFY | egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin-web/src/features/auth/gatewayAuth.ts | 当前文件存在：egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin-web/src/features/auth/gatewayAuth.ts | gatewayAuth | 把现有前端登录消费者迁到BFF，保留业务页面 | Step 15 | REQ-003, REQ-004, REQ-010, REQ-017 | npm --prefix egon-cola-xingyuan/egon-cola-xingyuan-admin-web-shared run test -- src/auth/gatewayAuthClient.test.ts |
| MODIFY | egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin-web/src/features/auth/AuthenticationShell.tsx | 当前文件存在：egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin-web/src/features/auth/AuthenticationShell.tsx | AuthenticationShell | 把现有前端登录消费者迁到BFF，保留业务页面 | Step 15 | REQ-003, REQ-004, REQ-010, REQ-017 | npm --prefix egon-cola-xingyuan/egon-cola-xingyuan-admin-web-shared run test -- src/auth/gatewayAuthClient.test.ts |
| MODIFY | egon-cola-xingyuan/egon-cola-yuheng/yuheng-admin-web/src/auth/AuthContext.tsx | 当前文件存在：egon-cola-xingyuan/egon-cola-yuheng/yuheng-admin-web/src/auth/AuthContext.tsx | AuthContext | 把现有前端登录消费者迁到BFF，保留业务页面 | Step 15 | REQ-003, REQ-004, REQ-010, REQ-017 | npm --prefix egon-cola-xingyuan/egon-cola-xingyuan-admin-web-shared run test -- src/auth/gatewayAuthClient.test.ts |
| MODIFY | egon-cola-xingyuan/egon-cola-yuheng/yuheng-admin-web/src/auth/LoginPage.tsx | 当前文件存在：egon-cola-xingyuan/egon-cola-yuheng/yuheng-admin-web/src/auth/LoginPage.tsx | LoginPage | 把现有前端登录消费者迁到BFF，保留业务页面 | Step 15 | REQ-003, REQ-004, REQ-010, REQ-017 | npm --prefix egon-cola-xingyuan/egon-cola-xingyuan-admin-web-shared run test -- src/auth/gatewayAuthClient.test.ts |
| CREATE | egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/tianquan/shoubing/admin/support/migration/OAuthMigrationServiceTest.java | 当前不存在；Spec §8/§11 明确目标，邻近包可验证 | dryRunNoPrivilegeExpansion | RED旧Client归属/非数字tenant处理 | Step 16 | REQ-011, REQ-012, REQ-016, REQ-017, REQ-018, REQ-019 | python3 -m py_compile scripts/unified-xingyuan/controller_ui_coverage.py |
| CREATE | egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/service/OAuthMigrationService.java | 当前不存在；Spec §8/§11 明确目标，邻近包可验证 | importRealm(realmKey,sourceSnapshotId,dryRun) | 非自动启动的迁移服务合同 | Step 16 | REQ-011, REQ-012, REQ-016, REQ-017, REQ-018, REQ-019 | python3 -m py_compile scripts/unified-xingyuan/controller_ui_coverage.py |
| CREATE | egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/service/impl/OAuthMigrationServiceImpl.java | 当前不存在；Spec §8/§11 明确目标，邻近包可验证 | oauthMigrationService | 先剖析/对账后批次写 | Step 16 | REQ-011, REQ-012, REQ-016, REQ-017, REQ-018, REQ-019 | python3 -m py_compile scripts/unified-xingyuan/controller_ui_coverage.py |
| MODIFY | scripts/unified-xingyuan/prepare-local-stack.sh | 当前文件存在：scripts/unified-xingyuan/prepare-local-stack.sh | issuer/SCG/secret mount env | 准备TLS/新入口/独立server paths | Step 16 | REQ-011, REQ-012, REQ-016, REQ-017, REQ-018, REQ-019 | python3 -m py_compile scripts/unified-xingyuan/controller_ui_coverage.py |
| MODIFY | scripts/unified-xingyuan/test-direct-run-contract.sh | 当前文件存在：scripts/unified-xingyuan/test-direct-run-contract.sh | new issuer, jwks, wrong tenant/platform checks | 静态和受控环境契约验收 | Step 16 | REQ-011, REQ-012, REQ-016, REQ-017, REQ-018, REQ-019 | python3 -m py_compile scripts/unified-xingyuan/controller_ui_coverage.py |
| MODIFY | scripts/unified-xingyuan/test-live-frontend-login.sh | 当前文件存在：scripts/unified-xingyuan/test-live-frontend-login.sh | fresh BFF login/code/PKCE/consent/tenant | 用户启动的浏览器/HTTP E2E核对 | Step 16 | REQ-011, REQ-012, REQ-016, REQ-017, REQ-018, REQ-019 | python3 -m py_compile scripts/unified-xingyuan/controller_ui_coverage.py |
| MODIFY | docs/runbooks/unified-identity-oauth-client-tenant-cutover.md | 当前文件存在：docs/runbooks/unified-identity-oauth-client-tenant-cutover.md | per-tenant PREPARED→VERIFIED→CUTOVER→RETIRED | 写真实运维命令、反向证据和前向修复界限 | Step 16 | REQ-011, REQ-012, REQ-016, REQ-017, REQ-018, REQ-019 | python3 -m py_compile scripts/unified-xingyuan/controller_ui_coverage.py |
| CREATE | egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/LegacyOAuthRetirementTest.java | 当前不存在；Spec §8/§11 明确目标，邻近包可验证 | oldTenRoutes410 | RED旧Controller还签发/刷新 | Step 17 | REQ-001, REQ-006, REQ-010, REQ-012, REQ-017, REQ-019 | mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=LegacyOAuthRetirementTest,OAuthOidcProtocolContractTest -Dsurefire.failIfNoSpecifiedTests=false test |
| CREATE | egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/controller/filter/LegacyOAuthRetirementFilter.java | 当前不存在；Spec §8/§11 明确目标，邻近包可验证 | legacyOAuthRetirementFilter | 旧固定URL明确410 | Step 17 | REQ-001, REQ-006, REQ-010, REQ-012, REQ-017, REQ-019 | mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=LegacyOAuthRetirementTest,OAuthOidcProtocolContractTest -Dsurefire.failIfNoSpecifiedTests=false test |
| MODIFY | egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/support/security/IdpSecurityConfig.java | 当前文件存在：egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/support/security/IdpSecurityConfig.java | 3 ordered filter chains | 先SAS登录/管理，再旧路径退役 | Step 17 | REQ-001, REQ-006, REQ-010, REQ-012, REQ-017, REQ-019 | mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=LegacyOAuthRetirementTest,OAuthOidcProtocolContractTest -Dsurefire.failIfNoSpecifiedTests=false test |
| MODIFY | egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/controller/OAuthTokenController.java | 当前文件存在：egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/controller/OAuthTokenController.java | remove old mapping/bean registration | 旧POST /oauth2/token/revoke/logout停止处理 | Step 17 | REQ-001, REQ-006, REQ-010, REQ-012, REQ-017, REQ-019 | mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=LegacyOAuthRetirementTest,OAuthOidcProtocolContractTest -Dsurefire.failIfNoSpecifiedTests=false test |
| MODIFY | egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/controller/OAuthMetadataController.java | 当前文件存在：egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/controller/OAuthMetadataController.java | remove fixed jwks metadata | 新issuer discovery ownsmetadata | Step 17 | REQ-001, REQ-006, REQ-010, REQ-012, REQ-017, REQ-019 | mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=LegacyOAuthRetirementTest,OAuthOidcProtocolContractTest -Dsurefire.failIfNoSpecifiedTests=false test |
| MODIFY | egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/controller/OAuthLoginController.java | 当前文件存在：egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/controller/OAuthLoginController.java | remove password login mapping | 只接受新tenant login + BFF Code | Step 17 | REQ-001, REQ-006, REQ-010, REQ-012, REQ-017, REQ-019 | mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=LegacyOAuthRetirementTest,OAuthOidcProtocolContractTest -Dsurefire.failIfNoSpecifiedTests=false test |
| MODIFY | egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/controller/InternalRefreshTokenController.java | 当前文件存在：egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/controller/InternalRefreshTokenController.java | remove old RT validation route | new BFF uses SAS revocation and server session | Step 17 | REQ-001, REQ-006, REQ-010, REQ-012, REQ-017, REQ-019 | mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=LegacyOAuthRetirementTest,OAuthOidcProtocolContractTest -Dsurefire.failIfNoSpecifiedTests=false test |

| CREATE | egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/domain/bo/TrustedRealmBO.java | 当前不存在；主Spec §7.3.1/§10 命名此对象/字段；现有相邻业务包提供放置证据 | TrustedRealmBO | 主Spec §7.3.1/§10 的必要类型/入口 | Step 3 | REQ-002, REQ-005, REQ-011, REQ-018 | `mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthRealmPersistenceContractTest -Dsurefire.failIfNoSpecifiedTests=false test`。 |
| CREATE | egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/domain/enums/RealmTypeEnum.java | 当前不存在；主Spec §7.3.1/§10 命名此对象/字段；现有相邻业务包提供放置证据 | RealmTypeEnum | 主Spec §7.3.1/§10 的必要类型/入口 | Step 3 | REQ-002, REQ-005, REQ-011, REQ-018 | `mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthRealmPersistenceContractTest -Dsurefire.failIfNoSpecifiedTests=false test`。 |
| CREATE | egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/domain/enums/OAuthClientPurposeEnum.java | 当前不存在；主Spec §7.3.1/§10 命名此对象/字段；现有相邻业务包提供放置证据 | OAuthClientPurposeEnum | 主Spec §7.3.1/§10 的必要类型/入口 | Step 3 | REQ-002, REQ-005, REQ-011, REQ-018 | `mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthRealmPersistenceContractTest -Dsurefire.failIfNoSpecifiedTests=false test`。 |
| CREATE | egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/domain/bo/OAuthAuthorizationAttributesBO.java | 当前不存在；主Spec §10.1/§11 命名此对象/字段；现有相邻业务包提供放置证据 | OAuthAuthorizationAttributesBO | 主Spec §10.1/§11 的必要类型/入口 | Step 5 | REQ-003, REQ-004, REQ-012, REQ-017 | `mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthAuthorizationPersistenceContractTest -Dsurefire.failIfNoSpecifiedTests=false test`。 |
| CREATE | egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/domain/bo/OAuthSecuritySnapshotBO.java | 当前不存在；主Spec §10.1/§11 命名此对象/字段；现有相邻业务包提供放置证据 | OAuthSecuritySnapshotBO | 主Spec §10.1/§11 的必要类型/入口 | Step 5 | REQ-003, REQ-004, REQ-012, REQ-017 | `mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthAuthorizationPersistenceContractTest -Dsurefire.failIfNoSpecifiedTests=false test`。 |
| CREATE | egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/domain/bo/OAuthTokenMetadataBO.java | 当前不存在；主Spec §10.1/§11 命名此对象/字段；现有相邻业务包提供放置证据 | OAuthTokenMetadataBO | 主Spec §10.1/§11 的必要类型/入口 | Step 5 | REQ-003, REQ-004, REQ-012, REQ-017 | `mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthAuthorizationPersistenceContractTest -Dsurefire.failIfNoSpecifiedTests=false test`。 |
| CREATE | egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/domain/bo/OAuthSecurityVersionBO.java | 当前不存在；主Spec §10.1/§11 命名此对象/字段；现有相邻业务包提供放置证据 | OAuthSecurityVersionBO | 主Spec §10.1/§11 的必要类型/入口 | Step 5 | REQ-003, REQ-004, REQ-012, REQ-017 | `mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthAuthorizationPersistenceContractTest -Dsurefire.failIfNoSpecifiedTests=false test`。 |
| CREATE | egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/domain/bo/SignedTokenClaimsBO.java | 当前不存在；主Spec §10.1/§11 命名此对象/字段；现有相邻业务包提供放置证据 | SignedTokenClaimsBO | 主Spec §10.1/§11 的必要类型/入口 | Step 5 | REQ-003, REQ-004, REQ-012, REQ-017 | `mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthAuthorizationPersistenceContractTest -Dsurefire.failIfNoSpecifiedTests=false test`。 |
| CREATE | egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/domain/enums/OAuthTokenTypeEnum.java | 当前不存在；主Spec §10.1/§11 命名此对象/字段；现有相邻业务包提供放置证据 | OAuthTokenTypeEnum | 主Spec §10.1/§11 的必要类型/入口 | Step 5 | REQ-003, REQ-004, REQ-012, REQ-017 | `mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthAuthorizationPersistenceContractTest -Dsurefire.failIfNoSpecifiedTests=false test`。 |
| CREATE | egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/domain/enums/OAuthKeyStatusEnum.java | 当前不存在；主Spec §10.3/§15.2 命名此对象/字段；现有相邻业务包提供放置证据 | OAuthKeyStatusEnum | 主Spec §10.3/§15.2 的必要类型/入口 | Step 6 | REQ-012, REQ-013, REQ-014, REQ-015 | `mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthSigningKeyPersistenceContractTest -Dsurefire.failIfNoSpecifiedTests=false test`。 |
| CREATE | egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/domain/enums/OAuthSecurityTargetEnum.java | 当前不存在；主Spec §10.3/§15.2 命名此对象/字段；现有相邻业务包提供放置证据 | OAuthSecurityTargetEnum | 主Spec §10.3/§15.2 的必要类型/入口 | Step 6 | REQ-012, REQ-013, REQ-014, REQ-015 | `mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthSigningKeyPersistenceContractTest -Dsurefire.failIfNoSpecifiedTests=false test`。 |
| CREATE | egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/service/impl/OAuthKeyCipherComponent.java | 当前不存在；主Spec §10.3/§15.2 命名此对象/字段；现有相邻业务包提供放置证据 | OAuthKeyCipherComponent | 主Spec §10.3/§15.2 的必要类型/入口 | Step 6 | REQ-012, REQ-013, REQ-014, REQ-015 | `mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthSigningKeyPersistenceContractTest -Dsurefire.failIfNoSpecifiedTests=false test`。 |
| CREATE | egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/config/OAuthKeyEncryptionProperties.java | 当前不存在；主Spec §10.3/§15.2 命名此对象/字段；现有相邻业务包提供放置证据 | OAuthKeyEncryptionProperties | 主Spec §10.3/§15.2 的必要类型/入口 | Step 6 | REQ-012, REQ-013, REQ-014, REQ-015 | `mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthSigningKeyPersistenceContractTest -Dsurefire.failIfNoSpecifiedTests=false test`。 |
| CREATE | egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/domain/converter/SasRegisteredClientConverter.java | 当前不存在；主Spec §8.4/§10.2 命名此对象/字段；现有相邻业务包提供放置证据 | SasRegisteredClientConverter | 主Spec §8.4/§10.2 的必要类型/入口 | Step 7 | REQ-001, REQ-002, REQ-004, REQ-013, REQ-016 | `mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=TenantIssuerIsolationTest -Dsurefire.failIfNoSpecifiedTests=false test`。 |
| CREATE | egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/domain/converter/SasAuthorizationConverter.java | 当前不存在；主Spec §8.4/§10.2 命名此对象/字段；现有相邻业务包提供放置证据 | SasAuthorizationConverter | 主Spec §8.4/§10.2 的必要类型/入口 | Step 7 | REQ-001, REQ-002, REQ-004, REQ-013, REQ-016 | `mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=TenantIssuerIsolationTest -Dsurefire.failIfNoSpecifiedTests=false test`。 |
| CREATE | egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/domain/converter/SasConsentConverter.java | 当前不存在；主Spec §8.4/§10.2 命名此对象/字段；现有相邻业务包提供放置证据 | SasConsentConverter | 主Spec §8.4/§10.2 的必要类型/入口 | Step 7 | REQ-001, REQ-002, REQ-004, REQ-013, REQ-016 | `mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=TenantIssuerIsolationTest -Dsurefire.failIfNoSpecifiedTests=false test`。 |
| CREATE | egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/domain/command/ConsumeOAuthTokenCommand.java | 当前不存在；主Spec §7.3.1/§9 命名此对象/字段；现有相邻业务包提供放置证据 | ConsumeOAuthTokenCommand | 主Spec §7.3.1/§9 的必要类型/入口 | Step 8 | REQ-003, REQ-004, REQ-005, REQ-011 | `mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthOidcProtocolContractTest -Dsurefire.failIfNoSpecifiedTests=false test`。 |
| MODIFY | egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/config/TokenConfig.java | 当前同名文件存在，符号/消费者由rg确认 | TokenConfig coexistence | 主Spec §7.3.1/§9 的必要类型/入口 | Step 8 | REQ-003, REQ-004, REQ-005, REQ-011 | `mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthOidcProtocolContractTest -Dsurefire.failIfNoSpecifiedTests=false test`。 |
| MODIFY | egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/config/OAuthConfig.java | 当前同名文件存在，符号/消费者由rg确认 | OAuthConfig coexistence | 主Spec §7.3.1/§9 的必要类型/入口 | Step 8 | REQ-003, REQ-004, REQ-005, REQ-011 | `mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthOidcProtocolContractTest -Dsurefire.failIfNoSpecifiedTests=false test`。 |
| CREATE | egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/domain/converter/OAuthClientViewConverter.java | 当前不存在；主Spec §9.2/§10.2 命名此对象/字段；现有相邻业务包提供放置证据 | OAuthClientViewConverter | 主Spec §9.2/§10.2 的必要类型/入口 | Step 9 | REQ-002, REQ-005, REQ-011, REQ-013, REQ-017 | `mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthClientControllerTest,SigningKeyServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test`。 |
| CREATE | egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/resource/domain/converter/ClientResourceGrantViewConverter.java | 当前不存在；主Spec §9.2/§10.2 命名此对象/字段；现有相邻业务包提供放置证据 | ClientResourceGrantViewConverter | 主Spec §9.2/§10.2 的必要类型/入口 | Step 9 | REQ-002, REQ-005, REQ-011, REQ-013, REQ-017 | `mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthClientControllerTest,SigningKeyServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test`。 |
| CREATE | egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/domain/converter/SigningKeyViewConverter.java | 当前不存在；主Spec §9.2/§10.2 命名此对象/字段；现有相邻业务包提供放置证据 | SigningKeyViewConverter | 主Spec §9.2/§10.2 的必要类型/入口 | Step 9 | REQ-002, REQ-005, REQ-011, REQ-013, REQ-017 | `mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthClientControllerTest,SigningKeyServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test`。 |
| CREATE | egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/resource/domain/converter/ResourceServerViewConverter.java | 当前不存在；主Spec §9.2/§10.2 命名此对象/字段；现有相邻业务包提供放置证据 | ResourceServerViewConverter | 主Spec §9.2/§10.2 的必要类型/入口 | Step 9 | REQ-002, REQ-005, REQ-011, REQ-013, REQ-017 | `mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthClientControllerTest,SigningKeyServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test`。 |
| MODIFY | egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/domain/vo/OAuthClientVO.java | 当前同名文件存在，符号/消费者由rg确认 | OAuthClientVO | 主Spec §9.2/§10.2 的必要类型/入口 | Step 9 | REQ-002, REQ-005, REQ-011, REQ-013, REQ-017 | `mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthClientControllerTest,SigningKeyServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test`。 |
| MODIFY | egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/domain/vo/CreatedOAuthClientVO.java | 当前同名文件存在，符号/消费者由rg确认 | CreatedOAuthClientVO | 主Spec §9.2/§10.2 的必要类型/入口 | Step 9 | REQ-002, REQ-005, REQ-011, REQ-013, REQ-017 | `mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthClientControllerTest,SigningKeyServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test`。 |
| MODIFY | egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/domain/vo/RotatedClientSecretVO.java | 当前同名文件存在，符号/消费者由rg确认 | RotatedClientSecretVO | 主Spec §9.2/§10.2 的必要类型/入口 | Step 9 | REQ-002, REQ-005, REQ-011, REQ-013, REQ-017 | `mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthClientControllerTest,SigningKeyServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test`。 |
| MODIFY | egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/resource/domain/vo/ClientResourceGrantVO.java | 当前同名文件存在，符号/消费者由rg确认 | ClientResourceGrantVO | 主Spec §9.2/§10.2 的必要类型/入口 | Step 9 | REQ-002, REQ-005, REQ-011, REQ-013, REQ-017 | `mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthClientControllerTest,SigningKeyServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test`。 |
| MODIFY | egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/resource/domain/vo/ResourceServerVO.java | 当前同名文件存在，符号/消费者由rg确认 | ResourceServerVO | 主Spec §9.2/§10.2 的必要类型/入口 | Step 9 | REQ-002, REQ-005, REQ-011, REQ-013, REQ-017 | `mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthClientControllerTest,SigningKeyServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test`。 |
| MODIFY | egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/domain/vo/SigningKeyVO.java | 当前同名文件存在，符号/消费者由rg确认 | SigningKeyVO | 主Spec §9.2/§10.2 的必要类型/入口 | Step 9 | REQ-002, REQ-005, REQ-011, REQ-013, REQ-017 | `mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthClientControllerTest,SigningKeyServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test`。 |
| MODIFY | egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/domain/dto/RotateClientSecretDTO.java | 当前同名文件存在，符号/消费者由rg确认 | RotateClientSecretDTO | 主Spec §9.2/§10.2 的必要类型/入口 | Step 9 | REQ-002, REQ-005, REQ-011, REQ-013, REQ-017 | `mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthClientControllerTest,SigningKeyServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test`。 |
| MODIFY | egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/domain/dto/OAuthValueDTO.java | 当前同名文件存在，符号/消费者由rg确认 | OAuthValueDTO | 主Spec §9.2/§10.2 的必要类型/入口 | Step 9 | REQ-002, REQ-005, REQ-011, REQ-013, REQ-017 | `mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthClientControllerTest,SigningKeyServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test`。 |
| MODIFY | egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/resource/domain/dto/UpsertClientResourceGrantDTO.java | 当前同名文件存在，符号/消费者由rg确认 | UpsertClientResourceGrantDTO | 主Spec §9.2/§10.2 的必要类型/入口 | Step 9 | REQ-002, REQ-005, REQ-011, REQ-013, REQ-017 | `mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthClientControllerTest,SigningKeyServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test`。 |
| MODIFY | egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/resource/domain/dto/DeleteClientResourceGrantDTO.java | 当前同名文件存在，符号/消费者由rg确认 | DeleteClientResourceGrantDTO | 主Spec §9.2/§10.2 的必要类型/入口 | Step 9 | REQ-002, REQ-005, REQ-011, REQ-013, REQ-017 | `mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthClientControllerTest,SigningKeyServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test`。 |
| MODIFY | egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/resource/domain/dto/BatchClientResourceGrantDTO.java | 当前同名文件存在，符号/消费者由rg确认 | BatchClientResourceGrantDTO | 主Spec §9.2/§10.2 的必要类型/入口 | Step 9 | REQ-002, REQ-005, REQ-011, REQ-013, REQ-017 | `mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthClientControllerTest,SigningKeyServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test`。 |
| MODIFY | egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/domain/dto/PublishSigningKeyDTO.java | 当前同名文件存在，符号/消费者由rg确认 | PublishSigningKeyDTO | 主Spec §9.2/§10.2 的必要类型/入口 | Step 9 | REQ-002, REQ-005, REQ-011, REQ-013, REQ-017 | `mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthClientControllerTest,SigningKeyServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test`。 |
| CREATE | egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-core/src/main/java/top/egon/cola/platform/tianquan/shoubing/core/audit/OAuthSecurityChangedEvent.java | 当前不存在；主Spec §9.2 EVENT/JOB/§15.3 命名此对象/字段；现有相邻业务包提供放置证据 | OAuthSecurityChangedEvent | 主Spec §9.2 EVENT/JOB/§15.3 的必要类型/入口 | Step 10 | REQ-012, REQ-013, REQ-014, REQ-015, REQ-016 | `mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=TokenRevocationServiceTest -Dsurefire.failIfNoSpecifiedTests=false test`。 |
| CREATE | egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-core/src/main/java/top/egon/cola/platform/tianquan/shoubing/core/audit/TokenUsageAuditEvent.java | 当前不存在；主Spec §9.2 EVENT/JOB/§15.3 命名此对象/字段；现有相邻业务包提供放置证据 | TokenUsageAuditEvent | 主Spec §9.2 EVENT/JOB/§15.3 的必要类型/入口 | Step 10 | REQ-012, REQ-013, REQ-014, REQ-015, REQ-016 | `mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=TokenRevocationServiceTest -Dsurefire.failIfNoSpecifiedTests=false test`。 |
| CREATE | egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/domain/command/RevokeOAuthTokenCommand.java | 当前不存在；主Spec §9.2 EVENT/JOB/§15.3 命名此对象/字段；现有相邻业务包提供放置证据 | RevokeOAuthTokenCommand | 主Spec §9.2 EVENT/JOB/§15.3 的必要类型/入口 | Step 10 | REQ-012, REQ-013, REQ-014, REQ-015, REQ-016 | `mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=TokenRevocationServiceTest -Dsurefire.failIfNoSpecifiedTests=false test`。 |
| CREATE | egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/domain/command/OAuthSubjectRevokeCommand.java | 当前不存在；主Spec §9.2 EVENT/JOB/§15.3 命名此对象/字段；现有相邻业务包提供放置证据 | OAuthSubjectRevokeCommand | 主Spec §9.2 EVENT/JOB/§15.3 的必要类型/入口 | Step 10 | REQ-012, REQ-013, REQ-014, REQ-015, REQ-016 | `mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=TokenRevocationServiceTest -Dsurefire.failIfNoSpecifiedTests=false test`。 |
| CREATE | egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/tianquan/shoubing/starter/audit/OAuthAuditProperties.java | 当前不存在；主Spec §9.2 EVENT/JOB/§15.3 命名此对象/字段；现有相邻业务包提供放置证据 | OAuthAuditProperties | 主Spec §9.2 EVENT/JOB/§15.3 的必要类型/入口 | Step 10 | REQ-012, REQ-013, REQ-014, REQ-015, REQ-016 | `mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=TokenRevocationServiceTest -Dsurefire.failIfNoSpecifiedTests=false test`。 |
| CREATE | egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/service/OAuthRetentionServiceTest.java | 当前不存在；主Spec §9.2 EVENT/JOB/§15.3 命名此对象/字段；现有相邻业务包提供放置证据 | OAuthRetentionServiceTest | 主Spec §9.2 EVENT/JOB/§15.3 的必要类型/入口 | Step 10 | REQ-012, REQ-013, REQ-014, REQ-015, REQ-016 | `mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=TokenRevocationServiceTest -Dsurefire.failIfNoSpecifiedTests=false test`。 |
| CREATE | egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/service/OAuthRetentionService.java | 当前不存在；主Spec §9.2 EVENT/JOB/§15.3 命名此对象/字段；现有相邻业务包提供放置证据 | OAuthRetentionService | 主Spec §9.2 EVENT/JOB/§15.3 的必要类型/入口 | Step 10 | REQ-012, REQ-013, REQ-014, REQ-015, REQ-016 | `mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=TokenRevocationServiceTest -Dsurefire.failIfNoSpecifiedTests=false test`。 |
| CREATE | egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/service/impl/OAuthRetentionServiceImpl.java | 当前不存在；主Spec §9.2 EVENT/JOB/§15.3 命名此对象/字段；现有相邻业务包提供放置证据 | OAuthRetentionServiceImpl | 主Spec §9.2 EVENT/JOB/§15.3 的必要类型/入口 | Step 10 | REQ-012, REQ-013, REQ-014, REQ-015, REQ-016 | `mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=TokenRevocationServiceTest -Dsurefire.failIfNoSpecifiedTests=false test`。 |
| CREATE | egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/tianquan/shoubing/starter/security/domain/VerifiedJwtBO.java | 当前不存在；主Spec §7.3.2/§10 命名此对象/字段；现有相邻业务包提供放置证据 | VerifiedJwtBO | 主Spec §7.3.2/§10 的必要类型/入口 | Step 11 | REQ-002, REQ-006, REQ-007, REQ-008, REQ-012, REQ-016 | `mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter -am -Dtest=MultiIssuerResourceServerTest -Dsurefire.failIfNoSpecifiedTests=false test`。 |
| CREATE | egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/tianquan/shoubing/starter/security/VerifiedPrincipalConverter.java | 当前不存在；主Spec §7.3.2/§10 命名此对象/字段；现有相邻业务包提供放置证据 | VerifiedPrincipalConverter | 主Spec §7.3.2/§10 的必要类型/入口 | Step 11 | REQ-002, REQ-006, REQ-007, REQ-008, REQ-012, REQ-016 | `mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter -am -Dtest=MultiIssuerResourceServerTest -Dsurefire.failIfNoSpecifiedTests=false test`。 |
| CREATE | egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/tianquan/shoubing/starter/security/OAuthRevocationReader.java | 当前不存在；主Spec §7.3.2/§10 命名此对象/字段；现有相邻业务包提供放置证据 | OAuthRevocationReader | 主Spec §7.3.2/§10 的必要类型/入口 | Step 11 | REQ-002, REQ-006, REQ-007, REQ-008, REQ-012, REQ-016 | `mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter -am -Dtest=MultiIssuerResourceServerTest -Dsurefire.failIfNoSpecifiedTests=false test`。 |
| CREATE | egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/tianquan/shoubing/starter/security/impl/RedisOAuthRevocationReader.java | 当前不存在；主Spec §7.3.2/§10 命名此对象/字段；现有相邻业务包提供放置证据 | RedisOAuthRevocationReader | 主Spec §7.3.2/§10 的必要类型/入口 | Step 11 | REQ-002, REQ-006, REQ-007, REQ-008, REQ-012, REQ-016 | `mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter -am -Dtest=MultiIssuerResourceServerTest -Dsurefire.failIfNoSpecifiedTests=false test`。 |
| CREATE | egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/tianquan/shoubing/starter/security/reactive/ReactiveOAuthRevocationReader.java | 当前不存在；主Spec §7.3.2/§10 命名此对象/字段；现有相邻业务包提供放置证据 | ReactiveOAuthRevocationReader | 主Spec §7.3.2/§10 的必要类型/入口 | Step 11 | REQ-002, REQ-006, REQ-007, REQ-008, REQ-012, REQ-016 | `mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter -am -Dtest=MultiIssuerResourceServerTest -Dsurefire.failIfNoSpecifiedTests=false test`。 |
| CREATE | egon-cola-xingyuan/egon-cola-yuheng/yuheng-edge-gateway/src/main/java/top/egon/cola/component/yuheng/edge/controller/domain/EdgeSessionResult.java | 当前不存在；主Spec §9.2 API-015–017 命名此对象/字段；现有相邻业务包提供放置证据 | EdgeSessionResult | 主Spec §9.2 API-015–017 的必要类型/入口 | Step 13 | REQ-003, REQ-004, REQ-006, REQ-010, REQ-011, REQ-012 | `mvn -o -pl egon-cola-xingyuan/egon-cola-yuheng/yuheng-edge-gateway -am -Dtest=EdgeLoginContractTest -Dsurefire.failIfNoSpecifiedTests=false test`。 |
| CREATE | egon-cola-xingyuan/egon-cola-yuheng/yuheng-edge-gateway/src/main/java/top/egon/cola/component/yuheng/edge/controller/domain/EdgeCsrfResult.java | 当前不存在；主Spec §9.2 API-015–017 命名此对象/字段；现有相邻业务包提供放置证据 | EdgeCsrfResult | 主Spec §9.2 API-015–017 的必要类型/入口 | Step 13 | REQ-003, REQ-004, REQ-006, REQ-010, REQ-011, REQ-012 | `mvn -o -pl egon-cola-xingyuan/egon-cola-yuheng/yuheng-edge-gateway -am -Dtest=EdgeLoginContractTest -Dsurefire.failIfNoSpecifiedTests=false test`。 |
| CREATE | egon-cola-xingyuan/egon-cola-yuheng/yuheng-edge-gateway/src/main/java/top/egon/cola/component/yuheng/edge/controller/domain/EdgeLogoutFailureResult.java | 当前不存在；主Spec §9.2 API-015–017 命名此对象/字段；现有相邻业务包提供放置证据 | EdgeLogoutFailureResult | 主Spec §9.2 API-015–017 的必要类型/入口 | Step 13 | REQ-003, REQ-004, REQ-006, REQ-010, REQ-011, REQ-012 | `mvn -o -pl egon-cola-xingyuan/egon-cola-yuheng/yuheng-edge-gateway -am -Dtest=EdgeLoginContractTest -Dsurefire.failIfNoSpecifiedTests=false test`。 |
| CREATE | egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/domain/command/OAuthMigrationCommand.java | 当前不存在；主Spec §16.2 命名此对象/字段；现有相邻业务包提供放置证据 | OAuthMigrationCommand | 主Spec §16.2 的必要类型/入口 | Step 16 | REQ-011, REQ-012, REQ-016, REQ-017, REQ-018, REQ-019 | `python3 -m py_compile scripts/unified-xingyuan/controller_ui_coverage.py`。 |
| CREATE | egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/domain/result/OAuthMigrationReportResult.java | 当前不存在；主Spec §16.2 命名此对象/字段；现有相邻业务包提供放置证据 | OAuthMigrationReportResult | 主Spec §16.2 的必要类型/入口 | Step 16 | REQ-011, REQ-012, REQ-016, REQ-017, REQ-018, REQ-019 | `python3 -m py_compile scripts/unified-xingyuan/controller_ui_coverage.py`。 |
| MODIFY | egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/controller/OAuthStepUpController.java | 当前同名文件存在，符号/消费者由rg确认 | OAuthStepUpController | 主Spec §8.4/§16 的必要类型/入口 | Step 17 | REQ-001, REQ-006, REQ-010, REQ-012, REQ-017, REQ-019 | `mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=LegacyOAuthRetirementTest,OAuthOidcProtocolContractTest -Dsurefire.failIfNoSpecifiedTests=false test`。 |
| MODIFY | egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/controller/OAuthUserInfoController.java | 当前同名文件存在，符号/消费者由rg确认 | OAuthUserInfoController | 主Spec §8.4/§16 的必要类型/入口 | Step 17 | REQ-001, REQ-006, REQ-010, REQ-012, REQ-017, REQ-019 | `mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=LegacyOAuthRetirementTest,OAuthOidcProtocolContractTest -Dsurefire.failIfNoSpecifiedTests=false test`。 |
| MODIFY | egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/support/runtime/IdpPlatformConfiguration.java | 当前同名文件存在，符号/消费者由rg确认 | IdpPlatformConfiguration | 主Spec §8.4/§16 的必要类型/入口 | Step 17 | REQ-001, REQ-006, REQ-010, REQ-012, REQ-017, REQ-019 | `mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=LegacyOAuthRetirementTest,OAuthOidcProtocolContractTest -Dsurefire.failIfNoSpecifiedTests=false test`。 |
| MODIFY | egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin-web/src/api/types.ts | 已存在页面/类型 | OAuthClientVO/CreateOAuthClientDTO/SigningKeyVO/GrantVO | 主Spec §12客户端/资源/密钥realm管理 | Step 15 | REQ-002, REQ-005, REQ-010, REQ-017 | Admin Vitest/类型检查 |
| MODIFY | egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin-web/src/features/clients/ClientListPage.test.tsx | 已存在页面/类型 | realmClientAndOneTimeSecret | 主Spec §12客户端/资源/密钥realm管理 | Step 15 | REQ-002, REQ-005, REQ-010, REQ-017 | Admin Vitest/类型检查 |
| MODIFY | egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin-web/src/features/clients/ClientListPage.tsx | 已存在页面/类型 | clientsQuery/create/update/rotate | 主Spec §12客户端/资源/密钥realm管理 | Step 15 | REQ-002, REQ-005, REQ-010, REQ-017 | Admin Vitest/类型检查 |
| MODIFY | egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin-web/src/features/resource-grants/ClientResourceGrantPage.test.tsx | 已存在页面/类型 | scopedGrantWriteAndQuery | 主Spec §12客户端/资源/密钥realm管理 | Step 15 | REQ-002, REQ-005, REQ-010, REQ-017 | Admin Vitest/类型检查 |
| MODIFY | egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin-web/src/features/resource-grants/ClientResourceGrantPage.tsx | 已存在页面/类型 | grantQuery/upsert/delete/batch | 主Spec §12客户端/资源/密钥realm管理 | Step 15 | REQ-002, REQ-005, REQ-010, REQ-017 | Admin Vitest/类型检查 |
| CREATE | egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin-web/src/features/keys/SigningKeyPage.test.tsx | 新测试路径由主Spec §14授权 | realmKeyRotationStates | 主Spec §12客户端/资源/密钥realm管理 | Step 15 | REQ-002, REQ-005, REQ-010, REQ-017 | Admin Vitest/类型检查 |
| MODIFY | egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin-web/src/features/keys/SigningKeyPage.tsx | 已存在页面/类型 | keyList/publish/activate/retire | 主Spec §12客户端/资源/密钥realm管理 | Step 15 | REQ-002, REQ-005, REQ-010, REQ-017 | Admin Vitest/类型检查 |
| MODIFY | egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin-web/src/features/resource-servers/ResourceServerListPage.test.tsx | 已存在页面/类型 | platformManagementClientBinding | 主Spec §12客户端/资源/密钥realm管理 | Step 15 | REQ-002, REQ-005, REQ-010, REQ-017 | Admin Vitest/类型检查 |
| MODIFY | egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin-web/src/features/resource-servers/ResourceServerListPage.tsx | 已存在页面/类型 | resourceList/create/disable | 主Spec §12客户端/资源/密钥realm管理 | Step 15 | REQ-002, REQ-005, REQ-010, REQ-017 | Admin Vitest/类型检查 |
```text
Step 1: 锁定依赖和技术入口
  MODIFY egon-cola-xingyuan/pom.xml
  MODIFY egon-cola-xingyuan/egon-cola-yuheng/pom.xml
  CREATE egon-cola-xingyuan/egon-cola-yuheng/yuheng-edge-gateway/pom.xml
  MODIFY egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/pom.xml
  MODIFY egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/pom.xml
  CREATE egon-cola-xingyuan/egon-cola-yuheng/yuheng-edge-gateway/lombok.config
  CREATE egon-cola-xingyuan/egon-cola-yuheng/yuheng-edge-gateway/src/main/java/top/egon/cola/component/yuheng/edge/GatewayEdgeApplication.java
Step 2: 建立受管OAuth数据库与物理路由
  CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/config/OAuthManagedSchemaContractTest.java
  CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/resources/db/egon-mp/V20260922_001__initialize_oauth_tenant_schema.sql
  CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/resources/db/egon-mp/repository-manifest.json
  CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/resources/oauth-sharding-native.yml
  CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/config/OAuthPersistenceConfiguration.java
  MODIFY egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/resources/application.yml
  MODIFY egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/resources/application-local.yml
Step 3: 持久化issuer与客户端注册
  CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/domain/po/OAuthRealmPO.java
  CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/domain/bo/OAuthRealmBO.java
  CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/repository/OAuthRealmRepository.java
  CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/domain/po/OAuthClientPO.java
  CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/domain/bo/OAuthClientBO.java
  CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/repository/OAuthClientRepository.java
  CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/repository/OAuthRealmPersistenceContractTest.java
  CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/domain/converter/OAuthRealmConverter.java
  CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/dao/OAuthRealmMapper.java
  CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/resources/mapper/oauth/OAuthRealmMapper.xml
  CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/repository/impl/OAuthRealmRepositoryImpl.java
  CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/domain/converter/OAuthClientConverter.java
  CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/dao/OAuthClientMapper.java
  CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/resources/mapper/oauth/OAuthClientMapper.xml
  CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/repository/impl/OAuthClientRepositoryImpl.java
Step 4: 持久化资源目录与最小授权
  CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/resource/domain/po/OAuthResourcePO.java
  CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/resource/domain/bo/OAuthResourceBO.java
  CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/resource/repository/OAuthResourceRepository.java
  CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/resource/domain/po/OAuthGrantPO.java
  CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/resource/domain/bo/OAuthGrantBO.java
  CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/resource/repository/OAuthGrantRepository.java
  CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/repository/OAuthResourcePersistenceContractTest.java
  CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/resource/domain/converter/OAuthResourceConverter.java
  CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/resource/dao/OAuthResourceMapper.java
  CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/resources/mapper/resource/OAuthResourceMapper.xml
  CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/resource/repository/impl/OAuthResourceRepositoryImpl.java
  CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/resource/domain/converter/OAuthGrantConverter.java
  CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/resource/dao/OAuthGrantMapper.java
  CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/resources/mapper/resource/OAuthGrantMapper.xml
  CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/resource/repository/impl/OAuthGrantRepositoryImpl.java
Step 5: 持久化授权码、令牌和同意
  CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/domain/po/OAuthAuthorizationPO.java
  CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/domain/bo/OAuthAuthorizationBO.java
  CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/repository/OAuthAuthorizationRepository.java
  CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/domain/po/OAuthTokenPO.java
  CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/domain/bo/OAuthTokenBO.java
  CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/repository/OAuthTokenRepository.java
  CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/domain/po/OAuthConsentPO.java
  CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/domain/bo/OAuthConsentBO.java
  CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/repository/OAuthConsentRepository.java
  CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/repository/OAuthAuthorizationPersistenceContractTest.java
  CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/domain/converter/OAuthAuthorizationConverter.java
  CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/dao/OAuthAuthorizationMapper.java
  CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/resources/mapper/oauth/OAuthAuthorizationMapper.xml
  CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/repository/impl/OAuthAuthorizationRepositoryImpl.java
  CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/domain/converter/OAuthTokenConverter.java
  CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/dao/OAuthTokenMapper.java
  CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/resources/mapper/token/OAuthTokenMapper.xml
  CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/repository/impl/OAuthTokenRepositoryImpl.java
  CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/domain/converter/OAuthConsentConverter.java
  CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/dao/OAuthConsentMapper.java
  CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/resources/mapper/oauth/OAuthConsentMapper.xml
  CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/repository/impl/OAuthConsentRepositoryImpl.java
Step 6: 持久化密钥、撤销与审计状态
  CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/domain/po/OAuthSigningKeyPO.java
  CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/domain/bo/OAuthSigningKeyBO.java
  CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/repository/OAuthSigningKeyRepository.java
  CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/domain/po/OAuthSecurityStatePO.java
  CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/domain/bo/OAuthSecurityStateBO.java
  CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/repository/OAuthSecurityStateRepository.java
  CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/domain/po/OAuthRevocationPO.java
  CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/domain/bo/OAuthRevocationBO.java
  CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/repository/OAuthRevocationRepository.java
  CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/audit/domain/po/OAuthAuditPO.java
  CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/audit/domain/bo/OAuthAuditBO.java
  CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/audit/repository/OAuthAuditRepository.java
  CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/repository/OAuthSigningKeyPersistenceContractTest.java
  CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/domain/converter/OAuthSigningKeyConverter.java
  CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/dao/OAuthSigningKeyMapper.java
  CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/resources/mapper/token/OAuthSigningKeyMapper.xml
  CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/repository/impl/OAuthSigningKeyRepositoryImpl.java
  CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/domain/converter/OAuthSecurityStateConverter.java
  CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/dao/OAuthSecurityStateMapper.java
  CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/resources/mapper/token/OAuthSecurityStateMapper.xml
  CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/repository/impl/OAuthSecurityStateRepositoryImpl.java
  CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/domain/converter/OAuthRevocationConverter.java
  CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/dao/OAuthRevocationMapper.java
  CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/resources/mapper/token/OAuthRevocationMapper.xml
  CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/repository/impl/OAuthRevocationRepositoryImpl.java
  CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/audit/domain/converter/OAuthAuditConverter.java
  CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/audit/dao/OAuthAuditMapper.java
  CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/resources/mapper/audit/OAuthAuditMapper.xml
  CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/audit/repository/impl/OAuthAuditRepositoryImpl.java
Step 7: 装配多issuer SAS仓储与协议发现
  CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/TenantIssuerIsolationTest.java
  CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/service/TenantIssuerService.java
  CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/service/impl/TenantIssuerServiceImpl.java
  CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/repository/TenantRegisteredClientRepository.java
  CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/service/impl/TenantAuthorizationServiceImpl.java
  CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/service/impl/TenantConsentServiceImpl.java
  CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/service/impl/TenantJwkSourceComponent.java
  CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/config/AuthorizationServerConfiguration.java
  CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/config/OAuthProtocolOpenApiConfiguration.java
Step 8: 实现授权码、PKCE和登录交互
  CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/controller/OAuthOidcProtocolContractTest.java
  CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/service/OAuthTokenIssueService.java
  CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/service/impl/OAuthTokenIssueServiceImpl.java
  CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/service/impl/TransactionalOAuthAuthenticationProvider.java
  CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/controller/TenantLoginController.java
  CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/controller/filter/TenantLoginAuthenticationFilter.java
  MODIFY egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/config/AuthorizationServerConfiguration.java
Step 9: 改造客户端、资源与密钥管理
  MODIFY egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/controller/OAuthClientControllerTest.java
  MODIFY egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/domain/dto/CreateOAuthClientDTO.java
  MODIFY egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/domain/dto/UpdateOAuthClientDTO.java
  MODIFY egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/service/impl/OAuthClientServiceImpl.java
  MODIFY egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/controller/OAuthClientController.java
  MODIFY egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/resource/service/impl/ResourceServerServiceImpl.java
  CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/resource/repository/MpResourceServerStore.java
  MODIFY egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/service/impl/SigningKeyServiceImpl.java
  MODIFY egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/controller/SigningKeyController.java
Step 10: 闭合撤销、审计、密钥轮换
  CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/tianquan/shoubing/admin/token/TokenRevocationServiceTest.java
  CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/service/impl/OAuthSecurityProjectionComponent.java
  CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/service/impl/TokenRevocationServiceImpl.java
  CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/support/outbox/OAuthSecurityDeliveryHandler.java
  CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/service/impl/OAuthSigningKeyRotationServiceImpl.java
  CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/tianquan/shoubing/starter/audit/TokenUsageAuditComponent.java
  CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/audit/service/impl/OAuthAuditConsumer.java
  MODIFY egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/identity/service/impl/IdentityUserServiceImpl.java
Step 11: 在资源服务器建立多issuer本地验签和租户上下文
  CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/test/java/top/egon/cola/platform/tianquan/shoubing/starter/security/MultiIssuerResourceServerTest.java
  CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/tianquan/shoubing/starter/security/TenantIssuerAuthenticationManagerComponent.java
  MODIFY egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/tianquan/shoubing/starter/security/IdpJwtVerifier.java
  CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/tianquan/shoubing/starter/security/VerifiedTenantContextFilter.java
  CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/tianquan/shoubing/starter/security/LegacyIssuerCutoverPolicy.java
  MODIFY egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/tianquan/shoubing/starter/autoconfigure/IdpStarterAutoConfiguration.java
  CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/lombok.config
Step 12: 改造机器客户端缓存并装配WebClient
  MODIFY egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/test/java/top/egon/cola/platform/tianquan/shoubing/starter/client/IdpServiceOAuth2ClientTest.java
  MODIFY egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/tianquan/shoubing/starter/client/ServiceAuthorizationKey.java
  MODIFY egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/tianquan/shoubing/starter/client/IdpServiceOAuth2Client.java
  CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/tianquan/shoubing/starter/client/ServiceTokenWebClientConfiguration.java
Step 13: 建立SCG OAuth2登录和服务端会话
  CREATE egon-cola-xingyuan/egon-cola-yuheng/yuheng-edge-gateway/src/test/java/top/egon/cola/component/yuheng/edge/EdgeLoginContractTest.java
  CREATE egon-cola-xingyuan/egon-cola-yuheng/yuheng-edge-gateway/src/main/java/top/egon/cola/component/yuheng/edge/config/EdgeOAuthProperties.java
  CREATE egon-cola-xingyuan/egon-cola-yuheng/yuheng-edge-gateway/src/main/java/top/egon/cola/component/yuheng/edge/service/impl/EdgeAuthorizedClientServiceImpl.java
  CREATE egon-cola-xingyuan/egon-cola-yuheng/yuheng-edge-gateway/src/main/java/top/egon/cola/component/yuheng/edge/filter/TrustedIdentityHeaderFilter.java
  CREATE egon-cola-xingyuan/egon-cola-yuheng/yuheng-edge-gateway/src/main/java/top/egon/cola/component/yuheng/edge/controller/EdgeSessionController.java
  CREATE egon-cola-xingyuan/egon-cola-yuheng/yuheng-edge-gateway/src/main/java/top/egon/cola/component/yuheng/edge/config/EdgeSecurityConfiguration.java
  CREATE egon-cola-xingyuan/egon-cola-yuheng/yuheng-edge-gateway/src/main/resources/application.yml
  CREATE egon-cola-xingyuan/egon-cola-yuheng/yuheng-edge-gateway/src/main/resources/application-local.yml
Step 14: 保留玉衡、鉴神与天枢准入链
  MODIFY egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-gateway-adapter/src/test/java/top/egon/cola/platform/tianquan/shoubing/yuheng/security/IdpGatewaySecurityProviderTest.java
  MODIFY egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-gateway-adapter/src/main/java/top/egon/cola/platform/tianquan/shoubing/yuheng/security/IdpIdentityAuthenticationProvider.java
  MODIFY egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-gateway-adapter/src/main/java/top/egon/cola/platform/tianquan/shoubing/yuheng/security/IdpUserCredentialRecoveryProvider.java
  MODIFY egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-starter/src/main/java/top/egon/cola/platform/tianquan/jianshen/starter/security/Rbac3BearerAuthenticationFilter.java
  MODIFY egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin/src/main/java/top/egon/cola/platform/tianquan/jianshen/admin/shared/tenant/controller/filter/TenantContextFilter.java
  MODIFY egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/java/top/egon/cola/component/tianshu/admin/security/registration/IdpJwtDdcRegistrationCredentialVerifier.java
Step 15: 迁移共享认证客户端与五个前端消费者
  MODIFY egon-cola-xingyuan/egon-cola-xingyuan-admin-web-shared/src/auth/gatewayAuthClient.test.ts
  MODIFY egon-cola-xingyuan/egon-cola-xingyuan-admin-web-shared/src/auth/gatewayAuthClient.ts
  MODIFY egon-cola-xingyuan/egon-cola-xingyuan-admin-portal/src/auth/portalAuth.ts
  MODIFY egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin-web/src/auth/AuthContext.tsx
  MODIFY egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin-web/src/auth/CentralLoginPage.tsx
  MODIFY egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin-web/src/auth/AuthContext.tsx
  MODIFY egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin-web/src/auth/LoginPage.tsx
  MODIFY egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin-web/src/features/auth/gatewayAuth.ts
  MODIFY egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin-web/src/features/auth/AuthenticationShell.tsx
  MODIFY egon-cola-xingyuan/egon-cola-yuheng/yuheng-admin-web/src/auth/AuthContext.tsx
  MODIFY egon-cola-xingyuan/egon-cola-yuheng/yuheng-admin-web/src/auth/LoginPage.tsx
Step 16: 提供受控数据导入、切流及本地验证入口
  CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/tianquan/shoubing/admin/support/migration/OAuthMigrationServiceTest.java
  CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/service/OAuthMigrationService.java
  CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/service/impl/OAuthMigrationServiceImpl.java
  MODIFY scripts/unified-xingyuan/prepare-local-stack.sh
  MODIFY scripts/unified-xingyuan/test-direct-run-contract.sh
  MODIFY scripts/unified-xingyuan/test-live-frontend-login.sh
  MODIFY docs/runbooks/unified-identity-oauth-client-tenant-cutover.md
Step 17: 退役旧协议并校验全域回归
  CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/LegacyOAuthRetirementTest.java
  CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/controller/filter/LegacyOAuthRetirementFilter.java
  MODIFY egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/support/security/IdpSecurityConfig.java
  MODIFY egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/controller/OAuthTokenController.java
  MODIFY egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/controller/OAuthMetadataController.java
  MODIFY egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/controller/OAuthLoginController.java
  MODIFY egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/controller/InternalRefreshTokenController.java
Step 3: CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/domain/bo/TrustedRealmBO.java
Step 3: CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/domain/enums/RealmTypeEnum.java
Step 3: CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/domain/enums/OAuthClientPurposeEnum.java
Step 5: CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/domain/bo/OAuthAuthorizationAttributesBO.java
Step 5: CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/domain/bo/OAuthSecuritySnapshotBO.java
Step 5: CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/domain/bo/OAuthTokenMetadataBO.java
Step 5: CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/domain/bo/OAuthSecurityVersionBO.java
Step 5: CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/domain/bo/SignedTokenClaimsBO.java
Step 5: CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/domain/enums/OAuthTokenTypeEnum.java
Step 6: CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/domain/enums/OAuthKeyStatusEnum.java
Step 6: CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/domain/enums/OAuthSecurityTargetEnum.java
Step 6: CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/service/impl/OAuthKeyCipherComponent.java
Step 6: CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/config/OAuthKeyEncryptionProperties.java
Step 7: CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/domain/converter/SasRegisteredClientConverter.java
Step 7: CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/domain/converter/SasAuthorizationConverter.java
Step 7: CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/domain/converter/SasConsentConverter.java
Step 8: CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/domain/command/ConsumeOAuthTokenCommand.java
Step 8: MODIFY egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/config/TokenConfig.java
Step 8: MODIFY egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/config/OAuthConfig.java
Step 9: CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/domain/converter/OAuthClientViewConverter.java
Step 9: CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/resource/domain/converter/ClientResourceGrantViewConverter.java
Step 9: CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/domain/converter/SigningKeyViewConverter.java
Step 9: CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/resource/domain/converter/ResourceServerViewConverter.java
Step 9: MODIFY egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/domain/vo/OAuthClientVO.java
Step 9: MODIFY egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/domain/vo/CreatedOAuthClientVO.java
Step 9: MODIFY egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/domain/vo/RotatedClientSecretVO.java
Step 9: MODIFY egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/resource/domain/vo/ClientResourceGrantVO.java
Step 9: MODIFY egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/resource/domain/vo/ResourceServerVO.java
Step 9: MODIFY egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/domain/vo/SigningKeyVO.java
Step 9: MODIFY egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/domain/dto/RotateClientSecretDTO.java
Step 9: MODIFY egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/domain/dto/OAuthValueDTO.java
Step 9: MODIFY egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/resource/domain/dto/UpsertClientResourceGrantDTO.java
Step 9: MODIFY egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/resource/domain/dto/DeleteClientResourceGrantDTO.java
Step 9: MODIFY egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/resource/domain/dto/BatchClientResourceGrantDTO.java
Step 9: MODIFY egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/domain/dto/PublishSigningKeyDTO.java
Step 10: CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-core/src/main/java/top/egon/cola/platform/tianquan/shoubing/core/audit/OAuthSecurityChangedEvent.java
Step 10: CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-core/src/main/java/top/egon/cola/platform/tianquan/shoubing/core/audit/TokenUsageAuditEvent.java
Step 10: CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/domain/command/RevokeOAuthTokenCommand.java
Step 10: CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/domain/command/OAuthSubjectRevokeCommand.java
Step 10: CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/tianquan/shoubing/starter/audit/OAuthAuditProperties.java
Step 10: CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/service/OAuthRetentionServiceTest.java
Step 10: CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/service/OAuthRetentionService.java
Step 10: CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/service/impl/OAuthRetentionServiceImpl.java
Step 11: CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/tianquan/shoubing/starter/security/domain/VerifiedJwtBO.java
Step 11: CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/tianquan/shoubing/starter/security/VerifiedPrincipalConverter.java
Step 11: CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/tianquan/shoubing/starter/security/OAuthRevocationReader.java
Step 11: CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/tianquan/shoubing/starter/security/impl/RedisOAuthRevocationReader.java
Step 11: CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/tianquan/shoubing/starter/security/reactive/ReactiveOAuthRevocationReader.java
Step 13: CREATE egon-cola-xingyuan/egon-cola-yuheng/yuheng-edge-gateway/src/main/java/top/egon/cola/component/yuheng/edge/controller/domain/EdgeSessionResult.java
Step 13: CREATE egon-cola-xingyuan/egon-cola-yuheng/yuheng-edge-gateway/src/main/java/top/egon/cola/component/yuheng/edge/controller/domain/EdgeCsrfResult.java
Step 13: CREATE egon-cola-xingyuan/egon-cola-yuheng/yuheng-edge-gateway/src/main/java/top/egon/cola/component/yuheng/edge/controller/domain/EdgeLogoutFailureResult.java
Step 16: CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/domain/command/OAuthMigrationCommand.java
Step 16: CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/domain/result/OAuthMigrationReportResult.java
Step 17: MODIFY egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/controller/OAuthStepUpController.java
Step 17: MODIFY egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/controller/OAuthUserInfoController.java
Step 17: MODIFY egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/support/runtime/IdpPlatformConfiguration.java
Step 17: MODIFY egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/config/TokenConfig.java
Step 17: MODIFY egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/config/OAuthConfig.java
Step 15: MODIFY egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin-web/src/api/types.ts
Step 15: MODIFY egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin-web/src/features/clients/ClientListPage.test.tsx
Step 15: MODIFY egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin-web/src/features/clients/ClientListPage.tsx
Step 15: MODIFY egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin-web/src/features/resource-grants/ClientResourceGrantPage.test.tsx
Step 15: MODIFY egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin-web/src/features/resource-grants/ClientResourceGrantPage.tsx
Step 15: CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin-web/src/features/keys/SigningKeyPage.test.tsx
Step 15: MODIFY egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin-web/src/features/keys/SigningKeyPage.tsx
Step 15: MODIFY egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin-web/src/features/resource-servers/ResourceServerListPage.test.tsx
Step 15: MODIFY egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin-web/src/features/resource-servers/ResourceServerListPage.tsx
```

## 6. Prerequisites, Constraints, and Plan Clarifications

### 6.1 Repository and worktree baseline

当前main@4f09f8ed2、根5.4.1（Spec基线5.4.0为发布版本号的语义不变更新）。工作树仅见技能/scripts的未跟踪__pycache__；不stage/清理。原请求“本次不使用code generator”继续生效；即使CLI已存在，此模块传统结构不在支持profile，也不调用。用户现只授权Plan，任何Step仅是未来执行合同。

### 6.2 Build, test, and environment prerequisites

| Concern | Exact command/source | Required state | Validation boundary |
| --- | --- | --- | --- |
| Java/Maven | mvn -version；根pom.xml | Java21、Maven3.9.14已读到；Boot3.5.16、project5.4.1 | Plan静态；不证明新依赖解析 |
| 当前依赖解析 | mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am help:effective-pom -Doutput=/tmp/egon-oauth-effective-pom.xml | 必须在执行前取得Boot/Security/SAS/Cloud兼容BOM证据；未缓存不能跳过或下载未批准版本 | 实施前置 |
| 配置/profile | SA/src/main/resources/application.yml,application-local.yml；edge同名文件 | 所有Spec §15.5键层级一致、secret只引用 | 静态/上下文测试 |
| DDL/迁移 | MP EgonColaPostgreDdlRunner + SA/src/main/resources/db/egon-mp | 一个新SQL版本+SHA-256 manifest；旧V1–V6不可改 | 静态+真实PG用户控制 |
| 前端 | W/package.json: npm --prefix egon-cola-xingyuan/egon-cola-xingyuan-admin-web-shared run test | 已安装node modules时Vitest/typecheck；Plan不npm publish | 单元/类型检查 |
| 真实基础设施 | scripts/unified-xingyuan/start-local-stack.sh / stop-local-stack.sh | 仅用户启动并管理；Codex不自动启动 | 运行/E2E门禁 |

本Plan的现有验证入口分别是 Maven Surefire 的精确`-Dtest`、前端package.json中的Vitest/typecheck、已有`scripts/unified-xingyuan`运行合同和`git diff --check`；Step 2的`OAuthManagedSchemaContractTest`还逐键比较base/local配置并校验manifest/物理节点。故当前无缺失的独立验证命令，不创建临时脚本。若实施时某新增门禁无法由这些现有命令承载，按Plan技能2026-09-23新增规则22，在`scripts/work/`以Plan ID命名临时harness并于最后Step明确迁入`scripts/checks/`或删除；未定归宿不得报Ready。该后续条件不授权本轮运行或修改脚本。

### 6.3 Immutable constraints and approved decisions

DEC-001–008全部由用户确认：SAS归授柄、SCG前置不替玉衡、传统分包认证MP/DDL、独立PLATFORM realm、Spring Session Redis、Redis逐请求撤销检查且故障拒绝、切流重新登录、客户端逐个bcrypt轮换。全链路TLS、RS256、Code S256、aud/scope/tenant/SQL与鉴神授权不放宽。旧Flyway V1–V6、已应用历史与其他用户工作均不修改。本Plan不启动项目/浏览器/数据库，不执行DDL，不编译生产代码。

### 6.4 Plan Clarifications

| ID | Small implementation inference | Repository evidence | Why semantics are unchanged | Impact if wrong |
| --- | --- | --- | --- | --- |
| PLAN-CLAR-001 | 把已存在的项目5.4.0→5.4.1只作为Maven父版本更新处理 | 当前root/SA/Y POM均5.4.1，diff主要发布版本 | 不改变Boot3.5.16、接口或Spec状态 | 若code/API随版本另改需回Spec |
| PLAN-CLAR-002 | Source原型PO名/DAO包按Spec §8.4机械展开，不运行CLI | 当前传统admin.<domain>业务包，backend-code-generation明确traditional unsupported | 仅把已命名Type/Domain代入精确目录，不改变架构 | 路径已有人工文件则暂停而非覆盖 |
| PLAN-CLAR-003 | 局部OAuthKeyCipherComponent使用JDK AES-GCM而不是新crypto依赖 | common-crypto仅Digest/Hmac/Codec，没有AES/GCM；Spec §8.4明确该类型 | 实现选定JDK原语，不增第三方能力 | 若源组件新增AES接口须复核避免重复 |
## 7. Ordered File-by-file Implementation Steps

### Step 1 — 锁定依赖和技术入口

- Requirements: REQ-001, REQ-010, REQ-011, REQ-018
- Dependencies: None
- Baseline state: 当前HEAD的对应源码/测试如每File的Repository evidence，先前Step已完成并有其focused GREEN；本Step尚不存在spring-cloud-dependencies 2025.0.3所列新行为。
- Observable outcome: Maven reactor 能识别独立 SCG 模块；授柄SAS/MP/Session依赖及同一版本线受控。
- End state: Maven reactor 能识别独立 SCG 模块；授柄SAS/MP/Session依赖及同一版本线受控；后续Step的Consumer尚未切流，不声明完整生产验收。
- Test-first gate: Not applicable — 本Step只建POM/入口的编译前置，行为RED由后续Step测试建立；离线缓存不足时报缺依赖而非伪成功。
- Manual Checks: MC-ARCH-001, MC-REUSE-001, MC-DEP-001, MC-BEAN-001, MC-SCOPE-001, MC-TEST-001
- Literal Rules: Rule 4, Rule 5, Rule 11
- Ordered files:

#### File 1 — `MODIFY egon-cola-xingyuan/pom.xml`

- Purpose: 由已批准的Cloud BOM管理SCG版本。
- Symbols: `spring-cloud-dependencies 2025.0.3`。
- Repository evidence: 当前文件存在：egon-cola-xingyuan/pom.xml；目标源于主Spec §7–§16。
- Dependencies and consumers: 输入依赖 None 与本Step此前文件；输出供 Maven reactor 能识别独立 SCG 模块；授柄SAS/MP/Session依赖及同一版本线受控及后一Step使用；Java Bean/Mapper按模块现有配置发现，前端按现有auth client消费。
- Why now: 本Step的编译前置/RED契约先锁定； 由已批准的Cloud BOM管理SCG版本在此位置后才可接消费者。
- Contract/signature changes: spring-cloud-dependencies 2025.0.3；由已批准的Cloud BOM管理SCG版本。公开行为只取主Spec §9对应API或§10/11内部合同。
- Input/output and state mapping: 仅Maven版本归属；没有认证行为。
- Error and edge behavior: 若resolved版本与Boot3.5不兼容，停止本Step并回Spec/依赖审批。
- Standards impact: MC-SCOPE-001, MC-TEST-001；按已选传统分层，输入校验与权限先于状态读取，PO不出边界，相关日志脱敏。
- Literal rule enforcement: Rule 11, Rule 5；此文件直接按所列原始规则落实命名、验证、类型/注入或配置，其他规则由同Step关联文件负责。
- Implementation pseudocode:

```xml
INPUT -> 仅Maven版本归属；没有认证行为。
FLOW -> dependencyManagement imports Cloud2025.0.3; modules内只用project.version5.4.1; 不改Boot3.5.16
ON INVALID/CONCURRENT/DEPENDENCY FAILURE -> 若resolved版本与Boot3.5不兼容，停止本Step并回Spec/依赖审批。
RESULT -> only the named symbol/contract in this File block becomes available; preserve tenant, expectedVersion, scope, UTC time and secret masking stated above.
```

- Verification contribution: 本Step `mvn -o -pl egon-cola-xingyuan/egon-cola-yuheng/yuheng-edge-gateway -am -DskipTests validate`；此文件负责 spring-cloud-dependencies 2025.0.3 的正例与 若resolved版本与Boot3.5不兼容，停止本Step并回Spec/依赖审批 负例。
- After this file: spring-cloud-dependencies 2025.0.3 的字段/入口或注册已落盘；与下一文件组合后本Step才能GREEN，不能把中间状态当完成。

#### File 2 — `MODIFY egon-cola-xingyuan/egon-cola-yuheng/pom.xml`

- Purpose: 纳入现有玉衡reactor但独立部署。
- Symbols: `module yuheng-edge-gateway`。
- Repository evidence: 当前文件存在：egon-cola-xingyuan/egon-cola-yuheng/pom.xml；目标源于主Spec §7–§16。
- Dependencies and consumers: 输入依赖 None 与本Step此前文件；输出供 Maven reactor 能识别独立 SCG 模块；授柄SAS/MP/Session依赖及同一版本线受控及后一Step使用；Java Bean/Mapper按模块现有配置发现，前端按现有auth client消费。
- Why now: 前一文件建立本文件需要的字段或调用契约； 纳入现有玉衡reactor但独立部署在此位置后才可接消费者。
- Contract/signature changes: module yuheng-edge-gateway；纳入现有玉衡reactor但独立部署。公开行为只取主Spec §9对应API或§10/11内部合同。
- Input/output and state mapping: Maven模块一项。
- Error and edge behavior: 不能替换yuheng-biz-gateway/mcp-gateway。
- Standards impact: MC-SCOPE-001, MC-TEST-001；按已选传统分层，输入校验与权限先于状态读取，PO不出边界，相关日志脱敏。
- Literal rule enforcement: Rule 11；此文件直接按所列原始规则落实命名、验证、类型/注入或配置，其他规则由同Step关联文件负责。
- Implementation pseudocode:

```xml
INPUT -> Maven模块一项。
FLOW -> modules.add(yuheng-edge-gateway); preserve biz/mcp child modules
ON INVALID/CONCURRENT/DEPENDENCY FAILURE -> 不能替换yuheng-biz-gateway/mcp-gateway。
RESULT -> only the named symbol/contract in this File block becomes available; preserve tenant, expectedVersion, scope, UTC time and secret masking stated above.
```

- Verification contribution: 本Step `mvn -o -pl egon-cola-xingyuan/egon-cola-yuheng/yuheng-edge-gateway -am -DskipTests validate`；此文件负责 module yuheng-edge-gateway 的正例与 不能替换yuheng-biz-gateway/mcp-gateway 负例。
- After this file: module yuheng-edge-gateway 的字段/入口或注册已落盘；与下一文件组合后本Step才能GREEN，不能把中间状态当完成。

#### File 3 — `CREATE egon-cola-xingyuan/egon-cola-yuheng/yuheng-edge-gateway/pom.xml`

- Purpose: SCG/WebFlux/OAuth2 Client/Resource Server/Session Redis的独立可执行模块。
- Symbols: `yuheng-edge-gateway dependencies`。
- Repository evidence: 当前不存在；Spec §8/§11 明确目标，邻近包可验证；目标源于主Spec §7–§16。
- Dependencies and consumers: 输入依赖 None 与本Step此前文件；输出供 Maven reactor 能识别独立 SCG 模块；授柄SAS/MP/Session依赖及同一版本线受控及后一Step使用；Java Bean/Mapper按模块现有配置发现，前端按现有auth client消费。
- Why now: 前一文件建立本文件需要的字段或调用契约； SCG/WebFlux/OAuth2 Client/Resource Server/Session Redis的独立可执行模块在此位置后才可接消费者。
- Contract/signature changes: yuheng-edge-gateway dependencies；SCG/WebFlux/OAuth2 Client/Resource Server/Session Redis的独立可执行模块。公开行为只取主Spec §9对应API或§10/11内部合同。
- Input/output and state mapping: 没有Controller/DAO业务代码；环境配置稍后加载。
- Error and edge behavior: 不能同时引入spring-boot-starter-web形成Servlet混合。
- Standards impact: MC-SCOPE-001, MC-TEST-001；按已选传统分层，输入校验与权限先于状态读取，PO不出边界，相关日志脱敏。
- Literal rule enforcement: Rule 11, Rule 5；此文件直接按所列原始规则落实命名、验证、类型/注入或配置，其他规则由同Step关联文件负责。
- Implementation pseudocode:

```xml
INPUT -> 没有Controller/DAO业务代码；环境配置稍后加载。
FLOW -> parent=egon-cola-yuheng:5.4.1; dependencies=Boot WebFlux,Security,OAuth2 Client,Resource Server,Spring Session Redis,Spring Cloud Gateway Server WebFlux,existingIdp starter,Micrometer,test; Boot repackage mainClass GatewayEdgeApplication
ON INVALID/CONCURRENT/DEPENDENCY FAILURE -> 不能同时引入spring-boot-starter-web形成Servlet混合。
RESULT -> only the named symbol/contract in this File block becomes available; preserve tenant, expectedVersion, scope, UTC time and secret masking stated above.
```

- Verification contribution: 本Step `mvn -o -pl egon-cola-xingyuan/egon-cola-yuheng/yuheng-edge-gateway -am -DskipTests validate`；此文件负责 yuheng-edge-gateway dependencies 的正例与 不能同时引入spring-boot-starter-web形成Servlet混合 负例。
- After this file: yuheng-edge-gateway dependencies 的字段/入口或注册已落盘；与下一文件组合后本Step才能GREEN，不能把中间状态当完成。

#### File 4 — `MODIFY egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/pom.xml`

- Purpose: 为后续协议和持久化提供已批准类型。
- Symbols: `SAS+MP+Session dependencies`。
- Repository evidence: 当前文件存在：egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/pom.xml；目标源于主Spec §7–§16。
- Dependencies and consumers: 输入依赖 None 与本Step此前文件；输出供 Maven reactor 能识别独立 SCG 模块；授柄SAS/MP/Session依赖及同一版本线受控及后一Step使用；Java Bean/Mapper按模块现有配置发现，前端按现有auth client消费。
- Why now: 前一文件建立本文件需要的字段或调用契约； 为后续协议和持久化提供已批准类型在此位置后才可接消费者。
- Contract/signature changes: SAS+MP+Session dependencies；为后续协议和持久化提供已批准类型。公开行为只取主Spec §9对应API或§10/11内部合同。
- Input/output and state mapping: OAuth新表只用MP，旧目录JPA暂存；不触碰Flyway历史。
- Error and edge behavior: 新增依赖版本只由已有Boot/Egon BOM；无第二MP版本。
- Standards impact: MC-SCOPE-001, MC-TEST-001；按已选传统分层，输入校验与权限先于状态读取，PO不出边界，相关日志脱敏。
- Literal rule enforcement: Rule 11, Rule 5；此文件直接按所列原始规则落实命名、验证、类型/注入或配置，其他规则由同Step关联文件负责。
- Implementation pseudocode:

```xml
INPUT -> OAuth新表只用MP，旧目录JPA暂存；不触碰Flyway历史。
FLOW -> add org.springframework.boot:spring-boot-starter-oauth2-authorization-server; top.egon:egon-cola-component-common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter; org.springframework.session:spring-session-data-redis; 保留JPA旧目录/Outbox
ON INVALID/CONCURRENT/DEPENDENCY FAILURE -> 新增依赖版本只由已有Boot/Egon BOM；无第二MP版本。
RESULT -> only the named symbol/contract in this File block becomes available; preserve tenant, expectedVersion, scope, UTC time and secret masking stated above.
```

- Verification contribution: 本Step `mvn -o -pl egon-cola-xingyuan/egon-cola-yuheng/yuheng-edge-gateway -am -DskipTests validate`；此文件负责 SAS+MP+Session dependencies 的正例与 新增依赖版本只由已有Boot/Egon BOM；无第二MP版本 负例。
- After this file: SAS+MP+Session dependencies 的字段/入口或注册已落盘；与下一文件组合后本Step才能GREEN，不能把中间状态当完成。

#### File 5 — `MODIFY egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/pom.xml`

- Purpose: 以现有spring-security-oauth2-client为基础增加WebFlux入口所需Boot管理依赖。
- Symbols: `WebClient dependencies`。
- Repository evidence: 当前文件存在：egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/pom.xml；目标源于主Spec §7–§16。
- Dependencies and consumers: 输入依赖 None 与本Step此前文件；输出供 Maven reactor 能识别独立 SCG 模块；授柄SAS/MP/Session依赖及同一版本线受控及后一Step使用；Java Bean/Mapper按模块现有配置发现，前端按现有auth client消费。
- Why now: 前一文件建立本文件需要的字段或调用契约； 以现有spring-security-oauth2-client为基础增加WebFlux入口所需Boot管理依赖在此位置后才可接消费者。
- Contract/signature changes: WebClient dependencies；以现有spring-security-oauth2-client为基础增加WebFlux入口所需Boot管理依赖。公开行为只取主Spec §9对应API或§10/11内部合同。
- Input/output and state mapping: 出站token仍由IdpServiceOAuth2Client缓存。
- Error and edge behavior: 重复OAuth2 Client依赖或版本漂移视失败。
- Standards impact: MC-SCOPE-001, MC-TEST-001；按已选传统分层，输入校验与权限先于状态读取，PO不出边界，相关日志脱敏。
- Literal rule enforcement: Rule 11, Rule 5；此文件直接按所列原始规则落实命名、验证、类型/注入或配置，其他规则由同Step关联文件负责。
- Implementation pseudocode:

```xml
INPUT -> 出站token仍由IdpServiceOAuth2Client缓存。
FLOW -> inspect current oauth2-client/Jose; add Boot-managed webflux only to implementing module when compiler requires; never copy credentials to starter
ON INVALID/CONCURRENT/DEPENDENCY FAILURE -> 重复OAuth2 Client依赖或版本漂移视失败。
RESULT -> only the named symbol/contract in this File block becomes available; preserve tenant, expectedVersion, scope, UTC time and secret masking stated above.
```

- Verification contribution: 本Step `mvn -o -pl egon-cola-xingyuan/egon-cola-yuheng/yuheng-edge-gateway -am -DskipTests validate`；此文件负责 WebClient dependencies 的正例与 重复OAuth2 Client依赖或版本漂移视失败 负例。
- After this file: WebClient dependencies 的字段/入口或注册已落盘；与下一文件组合后本Step才能GREEN，不能把中间状态当完成。

#### File 6 — `CREATE egon-cola-xingyuan/egon-cola-yuheng/yuheng-edge-gateway/lombok.config`

- Purpose: 新edge业务Bean的构造参数保留Qualifier。
- Symbols: `copyableAnnotations Qualifier`。
- Repository evidence: 当前不存在；Spec §8/§11 明确目标，邻近包可验证；目标源于主Spec §7–§16。
- Dependencies and consumers: 输入依赖 None 与本Step此前文件；输出供 Maven reactor 能识别独立 SCG 模块；授柄SAS/MP/Session依赖及同一版本线受控及后一Step使用；Java Bean/Mapper按模块现有配置发现，前端按现有auth client消费。
- Why now: 前一文件建立本文件需要的字段或调用契约； 新edge业务Bean的构造参数保留Qualifier在此位置后才可接消费者。
- Contract/signature changes: copyableAnnotations Qualifier；新edge业务Bean的构造参数保留Qualifier。公开行为只取主Spec §9对应API或§10/11内部合同。
- Input/output and state mapping: 生成构造器与Named Bean一致。
- Error and edge behavior: 没有Qualifier传播则编译/上下文测试失败。
- Standards impact: MC-SCOPE-001, MC-TEST-001；按已选传统分层，输入校验与权限先于状态读取，PO不出边界，相关日志脱敏。
- Literal rule enforcement: Rule 4, Rule 11；此文件直接按所列原始规则落实命名、验证、类型/注入或配置，其他规则由同Step关联文件负责。
- Implementation pseudocode:

```text
INPUT -> 生成构造器与Named Bean一致。
FLOW -> stopBubbling=true; lombok.copyableAnnotations += org.springframework.beans.factory.annotation.Qualifier
ON INVALID/CONCURRENT/DEPENDENCY FAILURE -> 没有Qualifier传播则编译/上下文测试失败。
RESULT -> only the named symbol/contract in this File block becomes available; preserve tenant, expectedVersion, scope, UTC time and secret masking stated above.
```

- Verification contribution: 本Step `mvn -o -pl egon-cola-xingyuan/egon-cola-yuheng/yuheng-edge-gateway -am -DskipTests validate`；此文件负责 copyableAnnotations Qualifier 的正例与 没有Qualifier传播则编译/上下文测试失败 负例。
- After this file: copyableAnnotations Qualifier 的字段/入口或注册已落盘；与下一文件组合后本Step才能GREEN，不能把中间状态当完成。

#### File 7 — `CREATE egon-cola-xingyuan/egon-cola-yuheng/yuheng-edge-gateway/src/main/java/top/egon/cola/component/yuheng/edge/GatewayEdgeApplication.java`

- Purpose: 仅新edge进程启动类。
- Symbols: `GatewayEdgeApplication.main`。
- Repository evidence: 当前不存在；Spec §8/§11 明确目标，邻近包可验证；目标源于主Spec §7–§16。
- Dependencies and consumers: 输入依赖 None 与本Step此前文件；输出供 Maven reactor 能识别独立 SCG 模块；授柄SAS/MP/Session依赖及同一版本线受控及后一Step使用；Java Bean/Mapper按模块现有配置发现，前端按现有auth client消费。
- Why now: 前一文件建立本文件需要的字段或调用契约； 仅新edge进程启动类在此位置后才可接消费者。
- Contract/signature changes: GatewayEdgeApplication.main；仅新edge进程启动类。公开行为只取主Spec §9对应API或§10/11内部合同。
- Input/output and state mapping: 不启动应用，只编译主类。
- Error and edge behavior: 不能把biz/mcp发动机导入同上下文。
- Standards impact: MC-ARCH-001, MC-SCOPE-001, MC-TEST-001, MC-VALID-001；按已选传统分层，输入校验与权限先于状态读取，PO不出边界，相关日志脱敏。
- Literal rule enforcement: Rule 4, Rule 11；此文件直接按所列原始规则落实命名、验证、类型/注入或配置，其他规则由同Step关联文件负责。
- Implementation pseudocode:

```java
INPUT -> 不启动应用，只编译主类。
FLOW -> @SpringBootApplication; main(args)->SpringApplication.run(GatewayEdgeApplication,args); 不注册旧玉衡路由Beans
ON INVALID/CONCURRENT/DEPENDENCY FAILURE -> 不能把biz/mcp发动机导入同上下文。
RESULT -> only the named symbol/contract in this File block becomes available; preserve tenant, expectedVersion, scope, UTC time and secret masking stated above.
```

- Verification contribution: 本Step `mvn -o -pl egon-cola-xingyuan/egon-cola-yuheng/yuheng-edge-gateway -am -DskipTests validate`；此文件负责 GatewayEdgeApplication.main 的正例与 不能把biz/mcp发动机导入同上下文 负例。
- After this file: GatewayEdgeApplication.main 的字段/入口或注册已落盘；与下一文件组合后本Step才能GREEN，不能把中间状态当完成。

- Validation working directory: `/Users/mario/SelfProject/Egon-COLA`。
- Verification command: `mvn -o -pl egon-cola-xingyuan/egon-cola-yuheng/yuheng-edge-gateway -am -DskipTests validate`。
- Expected result: 先确认上述RED由缺失目标行为引起，GREEN时命名用例成功、模块编译成功、`git diff --check`零错误；不以不存在依赖/数据库/fixture而失败作为RED。
- Failure returns to: 本Step File 1 的编译/Schema前置；若需改变外部语义、架构或DDL版本则回主Spec。
- Completion criteria: Maven reactor 能识别独立 SCG 模块；授柄SAS/MP/Session依赖及同一版本线受控；本Step的所有Manual Checks在实际执行时给出证据，未通过不能提交。
- Rollback: 提交前仅恢复本Step的精确路径；DDL已对真实数据运行后只forward-fix和新版本，不能编辑既有SQL/历史或恢复旧安全世代。
- Commit paths: `egon-cola-xingyuan/pom.xml`, `egon-cola-xingyuan/egon-cola-yuheng/pom.xml`, `egon-cola-xingyuan/egon-cola-yuheng/yuheng-edge-gateway/pom.xml`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/pom.xml`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/pom.xml`, `egon-cola-xingyuan/egon-cola-yuheng/yuheng-edge-gateway/lombok.config`, `egon-cola-xingyuan/egon-cola-yuheng/yuheng-edge-gateway/src/main/java/top/egon/cola/component/yuheng/edge/GatewayEdgeApplication.java`。
- Commit: `chore(xingyuan): establish oauth dependency and edge module`。

### Step 2 — 建立受管OAuth数据库与物理路由

- Requirements: REQ-002, REQ-008, REQ-014, REQ-017, REQ-018
- Dependencies: Step 1
- Baseline state: 当前HEAD的对应源码/测试如每File的Repository evidence，先前Step已完成并有其focused GREEN；本Step尚不存在oneManagedVersionAndRoutesAllTables所列新行为。
- Observable outcome: 一个新SQL版本定义11表及约束；NATIVE SINGLE精准落在tianquan_oauth。
- End state: 一个新SQL版本定义11表及约束；NATIVE SINGLE精准落在tianquan_oauth；后续Step的Consumer尚未切流，不声明完整生产验收。
- Test-first gate: Required — OAuthManagedSchemaContractTest先因新manifest/DDL/NATIVE资源缺失RED；随后静态/隔离schema test GREEN；真实PostgreSQL DDL归用户控制运行门禁。
- Manual Checks: MC-ARCH-001, MC-REUSE-001, MC-DEP-001, MC-VALID-001, MC-MODEL-001, MC-CONFIG-001, MC-SCOPE-001, MC-TEST-001
- Literal Rules: Rule 2, Rule 3, Rule 7, Rule 10, Rule 11
- Ordered files:

#### File 1 — `CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/config/OAuthManagedSchemaContractTest.java`

- Purpose: RED固定manifest/表/旧Flyway不可变。
- Symbols: `oneManagedVersionAndRoutesAllTables`。
- Repository evidence: 当前不存在；Spec §8/§11 明确目标，邻近包可验证；目标源于主Spec §7–§16。
- Dependencies and consumers: 输入依赖 Step 1 与本Step此前文件；输出供 一个新SQL版本定义11表及约束；NATIVE SINGLE精准落在tianquan_oauth及后一Step使用；Java Bean/Mapper按模块现有配置发现，前端按现有auth client消费。
- Why now: 本Step的编译前置/RED契约先锁定； RED固定manifest/表/旧Flyway不可变在此位置后才可接消费者。
- Contract/signature changes: oneManagedVersionAndRoutesAllTables；RED固定manifest/表/旧Flyway不可变。公开行为只取主Spec §9对应API或§10/11内部合同。
- Input/output and state mapping: 实际DDL字节SHA待SQL文件创建后断言；不连DB。
- Error and edge behavior: 无SQL或native YAML时定向RED。
- Standards impact: MC-ARCH-001, MC-SCOPE-001, MC-TEST-001, MC-VALID-001；按已选传统分层，输入校验与权限先于状态读取，PO不出边界，相关日志脱敏。
- Literal rule enforcement: Rule 2, Rule 11；此文件直接按所列原始规则落实命名、验证、类型/注入或配置，其他规则由同Step关联文件负责。
- Implementation pseudocode:

```java
INPUT -> 实际DDL字节SHA待SQL文件创建后断言；不连DB。
FLOW -> read manifest/SQL/NATIVE YAML and Boot YAML base/local; assert family=tianquan-oauth,V20260922_001 one script, 11 tables, SHA-256, business+deleted_at+active guards, no public.* or changed V1–V6; compare all Spec §15.5 leaf keys and native data-sources item schema across both profiles
ON INVALID/CONCURRENT/DEPENDENCY FAILURE -> 无SQL或native YAML时定向RED。
RESULT -> only the named symbol/contract in this File block becomes available; preserve tenant, expectedVersion, scope, UTC time and secret masking stated above.
```

- Verification contribution: 本Step `mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthManagedSchemaContractTest -Dsurefire.failIfNoSpecifiedTests=false test`；此文件负责 oneManagedVersionAndRoutesAllTables 的正例与 无SQL或native YAML时定向RED 负例。
- After this file: oneManagedVersionAndRoutesAllTables 的字段/入口或注册已落盘；与下一文件组合后本Step才能GREEN，不能把中间状态当完成。

#### File 2 — `CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/resources/db/egon-mp/V20260922_001__initialize_oauth_tenant_schema.sql`

- Purpose: 单逻辑DDL版本，包含Spec §11每列/索引。
- Symbols: `tianquan_oauth 11 tables`。
- Repository evidence: 当前不存在；Spec §8/§11 明确目标，邻近包可验证；目标源于主Spec §7–§16。
- Dependencies and consumers: 输入依赖 Step 1 与本Step此前文件；输出供 一个新SQL版本定义11表及约束；NATIVE SINGLE精准落在tianquan_oauth及后一Step使用；Java Bean/Mapper按模块现有配置发现，前端按现有auth client消费。
- Why now: 前一文件建立本文件需要的字段或调用契约； 单逻辑DDL版本，包含Spec §11每列/索引在此位置后才可接消费者。
- Contract/signature changes: tianquan_oauth 11 tables；单逻辑DDL版本，包含Spec §11每列/索引。公开行为只取主Spec §9对应API或§10/11内部合同。
- Input/output and state mapping: PRIMARY tianquan_oauth；旧public对象零DDL。
- Error and edge behavior: NULL/重复软删/tenant/主键/RT单次消费约束与Spec一致。
- Standards impact: MC-SCOPE-001, MC-TEST-001；按已选传统分层，输入校验与权限先于状态读取，PO不出边界，相关日志脱敏。
- Literal rule enforcement: Rule 3, Rule 10, Rule 11；此文件直接按所列原始规则落实命名、验证、类型/注入或配置，其他规则由同Step关联文件负责。
- Implementation pseudocode:

```sql
INPUT -> PRIMARY tianquan_oauth；旧public对象零DDL。
FLOW -> CREATE TABLE oauth_realm/client/grant/authorization/token/consent/signing_key/security_state/revocation/resource/audit; inherited EgonModel 8 columns; CHECK enums/expiry; composite tenant+business+deleted_at UK and WHERE deleted_at IS NULL; no Flyway repair; no CONCURRENTLY
ON INVALID/CONCURRENT/DEPENDENCY FAILURE -> NULL/重复软删/tenant/主键/RT单次消费约束与Spec一致。
RESULT -> only the named symbol/contract in this File block becomes available; preserve tenant, expectedVersion, scope, UTC time and secret masking stated above.
```

- Verification contribution: 本Step `mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthManagedSchemaContractTest -Dsurefire.failIfNoSpecifiedTests=false test`；此文件负责 tianquan_oauth 11 tables 的正例与 NULL/重复软删/tenant/主键/RT单次消费约束与Spec一致 负例。
- After this file: tianquan_oauth 11 tables 的字段/入口或注册已落盘；与下一文件组合后本Step才能GREEN，不能把中间状态当完成。

#### File 3 — `CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/resources/db/egon-mp/repository-manifest.json`

- Purpose: 绑定唯一新SQL字节哈希。
- Symbols: `family/scripts/version/path/sha256`。
- Repository evidence: 当前不存在；Spec §8/§11 明确目标，邻近包可验证；目标源于主Spec §7–§16。
- Dependencies and consumers: 输入依赖 Step 1 与本Step此前文件；输出供 一个新SQL版本定义11表及约束；NATIVE SINGLE精准落在tianquan_oauth及后一Step使用；Java Bean/Mapper按模块现有配置发现，前端按现有auth client消费。
- Why now: 前一文件建立本文件需要的字段或调用契约； 绑定唯一新SQL字节哈希在此位置后才可接消费者。
- Contract/signature changes: family/scripts/version/path/sha256；绑定唯一新SQL字节哈希。公开行为只取主Spec §9对应API或§10/11内部合同。
- Input/output and state mapping: manifest不存SQL文/数据库执行状态。
- Error and edge behavior: 哈希不一致启动/测试失败。
- Standards impact: MC-SCOPE-001, MC-TEST-001；按已选传统分层，输入校验与权限先于状态读取，PO不出边界，相关日志脱敏。
- Literal rule enforcement: Rule 11；此文件直接按所列原始规则落实命名、验证、类型/注入或配置，其他规则由同Step关联文件负责。
- Implementation pseudocode:

```text
INPUT -> manifest不存SQL文/数据库执行状态。
FLOW -> scripts=[{version:20260922_001,path:db/egon-mp/V20260922_001__initialize_oauth_tenant_schema.sql,sha256:sha256(file bytes)}]; family=tianquan-oauth
ON INVALID/CONCURRENT/DEPENDENCY FAILURE -> 哈希不一致启动/测试失败。
RESULT -> only the named symbol/contract in this File block becomes available; preserve tenant, expectedVersion, scope, UTC time and secret masking stated above.
```

- Verification contribution: 本Step `mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthManagedSchemaContractTest -Dsurefire.failIfNoSpecifiedTests=false test`；此文件负责 family/scripts/version/path/sha256 的正例与 哈希不一致启动/测试失败 负例。
- After this file: family/scripts/version/path/sha256 的字段/入口或注册已落盘；与下一文件组合后本Step才能GREEN，不能把中间状态当完成。

#### File 4 — `CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/resources/oauth-sharding-native.yml`

- Purpose: 明确11个tianquan_oauth节点及当前public SINGLE清单。
- Symbols: `NATIVE SINGLE nodes`。
- Repository evidence: 当前不存在；Spec §8/§11 明确目标，邻近包可验证；目标源于主Spec §7–§16。
- Dependencies and consumers: 输入依赖 Step 1 与本Step此前文件；输出供 一个新SQL版本定义11表及约束；NATIVE SINGLE精准落在tianquan_oauth及后一Step使用；Java Bean/Mapper按模块现有配置发现，前端按现有auth client消费。
- Why now: 前一文件建立本文件需要的字段或调用契约； 明确11个tianquan_oauth节点及当前public SINGLE清单在此位置后才可接消费者。
- Contract/signature changes: NATIVE SINGLE nodes；明确11个tianquan_oauth节点及当前public SINGLE清单。公开行为只取主Spec §9对应API或§10/11内部合同。
- Input/output and state mapping: 1 PRIMARY; tenant_id仍由MP拦截器SQL过滤。
- Error and edge behavior: 拒public.*和被旧V2删掉的表。
- Standards impact: MC-SCOPE-001, MC-TEST-001；按已选传统分层，输入校验与权限先于状态读取，PO不出边界，相关日志脱敏。
- Literal rule enforcement: Rule 11；此文件直接按所列原始规则落实命名、验证、类型/注入或配置，其他规则由同Step关联文件负责。
- Implementation pseudocode:

```yaml
INPUT -> 1 PRIMARY; tenant_id仍由MP拦截器SQL过滤。
FLOW -> databaseName=egon; no dataSources in YAML; !SINGLE tables oauth_primary.tianquan_oauth.<11 exact names> + public legacy list from Spec §11; transaction LOCAL
ON INVALID/CONCURRENT/DEPENDENCY FAILURE -> 拒public.*和被旧V2删掉的表。
RESULT -> only the named symbol/contract in this File block becomes available; preserve tenant, expectedVersion, scope, UTC time and secret masking stated above.
```

- Verification contribution: 本Step `mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthManagedSchemaContractTest -Dsurefire.failIfNoSpecifiedTests=false test`；此文件负责 NATIVE SINGLE nodes 的正例与 拒public.*和被旧V2删掉的表 负例。
- After this file: NATIVE SINGLE nodes 的字段/入口或注册已落盘；与下一文件组合后本Step才能GREEN，不能把中间状态当完成。

#### File 5 — `CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/config/OAuthPersistenceConfiguration.java`

- Purpose: 在逻辑DS创建前调用EgonColaPostgreDdlRunner。
- Symbols: `egonColaShardingLogicalDataSourceFactory`。
- Repository evidence: 当前不存在；Spec §8/§11 明确目标，邻近包可验证；目标源于主Spec §7–§16。
- Dependencies and consumers: 输入依赖 Step 1 与本Step此前文件；输出供 一个新SQL版本定义11表及约束；NATIVE SINGLE精准落在tianquan_oauth及后一Step使用；Java Bean/Mapper按模块现有配置发现，前端按现有auth client消费。
- Why now: 前一文件建立本文件需要的字段或调用契约； 在逻辑DS创建前调用EgonColaPostgreDdlRunner在此位置后才可接消费者。
- Contract/signature changes: egonColaShardingLogicalDataSourceFactory；在逻辑DS创建前调用EgonColaPostgreDdlRunner。公开行为只取主Spec §9对应API或§10/11内部合同。
- Input/output and state mapping: 组件受管DDL先于逻辑DS；同物理池。
- Error and edge behavior: unknown commit由runner在新连接核查，不能自动adopt非空schema。
- Standards impact: MC-ARCH-001, MC-SCOPE-001, MC-TEST-001, MC-VALID-001；按已选传统分层，输入校验与权限先于状态读取，PO不出边界，相关日志脱敏。
- Literal rule enforcement: Rule 4, Rule 7, Rule 11；此文件直接按所列原始规则落实命名、验证、类型/注入或配置，其他规则由同Step关联文件负责。
- Implementation pseudocode:

```java
INPUT -> 组件受管DDL先于逻辑DS；同物理池。
FLOW -> @Configuration(proxyBeanMethods=false); @Bean(name="egonColaShardingLogicalDataSourceFactory") LogicalDataSourceFactory(physical,yaml)->validator fingerprint, target=EgonColaDdlTargetBO(MASTER_DATA,tianquan_oauth, physical PRIMARY,manifest,fingerprint), runner.run(List.of(target)), then YamlShardingSphereDataSourceFactory.createDataSource(physical,yaml); if schema nonempty unmanaged, fail
ON INVALID/CONCURRENT/DEPENDENCY FAILURE -> unknown commit由runner在新连接核查，不能自动adopt非空schema。
RESULT -> only the named symbol/contract in this File block becomes available; preserve tenant, expectedVersion, scope, UTC time and secret masking stated above.
```

- Verification contribution: 本Step `mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthManagedSchemaContractTest -Dsurefire.failIfNoSpecifiedTests=false test`；此文件负责 egonColaShardingLogicalDataSourceFactory 的正例与 unknown commit由runner在新连接核查，不能自动adopt非空schema 负例。
- After this file: egonColaShardingLogicalDataSourceFactory 的字段/入口或注册已落盘；与下一文件组合后本Step才能GREEN，不能把中间状态当完成。

#### File 6 — `MODIFY egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/resources/application.yml`

- Purpose: 增加Spec §15.5完整叶子结构并禁旧Flyway自动执行。
- Symbols: `NATIVE/DDL/profile core keys`。
- Repository evidence: 当前文件存在：egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/resources/application.yml；目标源于主Spec §7–§16。
- Dependencies and consumers: 输入依赖 Step 1 与本Step此前文件；输出供 一个新SQL版本定义11表及约束；NATIVE SINGLE精准落在tianquan_oauth及后一Step使用；Java Bean/Mapper按模块现有配置发现，前端按现有auth client消费。
- Why now: 前一文件建立本文件需要的字段或调用契约； 增加Spec §15.5完整叶子结构并禁旧Flyway自动执行在此位置后才可接消费者。
- Contract/signature changes: NATIVE/DDL/profile core keys；增加Spec §15.5完整叶子结构并禁旧Flyway自动执行。公开行为只取主Spec §9对应API或§10/11内部合同。
- Input/output and state mapping: 密钥/Redis/issuer变量为空则Validated failstartup。
- Error and edge behavior: profile键不一致拒绝。
- Standards impact: MC-SCOPE-001, MC-TEST-001；按已选传统分层，输入校验与权限先于状态读取，PO不出边界，相关日志脱敏。
- Literal rule enforcement: Rule 7, Rule 11；此文件直接按所列原始规则落实命名、验证、类型/注入或配置，其他规则由同Step关联文件负责。
- Implementation pseudocode:

```yaml
INPUT -> 密钥/Redis/issuer变量为空则Validated failstartup。
FLOW -> spring.flyway.enabled=false; egon.cola.component.mybatis-plus.sharding.config-style=NATIVE,mode=SHARDING,native-rules-resource=classpath:oauth-sharding-native.yml; one PRIMARY data-source; ddl.enabled=true; outbox storage DS/TM names; no raw secret
ON INVALID/CONCURRENT/DEPENDENCY FAILURE -> profile键不一致拒绝。
RESULT -> only the named symbol/contract in this File block becomes available; preserve tenant, expectedVersion, scope, UTC time and secret masking stated above.
```

- Verification contribution: 本Step `mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthManagedSchemaContractTest -Dsurefire.failIfNoSpecifiedTests=false test`；此文件负责 NATIVE/DDL/profile core keys 的正例与 profile键不一致拒绝 负例。
- After this file: NATIVE/DDL/profile core keys 的字段/入口或注册已落盘；与下一文件组合后本Step才能GREEN，不能把中间状态当完成。

#### File 7 — `MODIFY egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/resources/application-local.yml`

- Purpose: 同步base全部新增键并允许local值不同。
- Symbols: `same config keys`。
- Repository evidence: 当前文件存在：egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/resources/application-local.yml；目标源于主Spec §7–§16。
- Dependencies and consumers: 输入依赖 Step 1 与本Step此前文件；输出供 一个新SQL版本定义11表及约束；NATIVE SINGLE精准落在tianquan_oauth及后一Step使用；Java Bean/Mapper按模块现有配置发现，前端按现有auth client消费。
- Why now: 前一文件建立本文件需要的字段或调用契约； 同步base全部新增键并允许local值不同在此位置后才可接消费者。
- Contract/signature changes: same config keys；同步base全部新增键并允许local值不同。公开行为只取主Spec §9对应API或§10/11内部合同。
- Input/output and state mapping: local证书也HTTPS，JPA目录未迁。
- Error and edge behavior: missing key parity FAIL。
- Standards impact: MC-SCOPE-001, MC-TEST-001；按已选传统分层，输入校验与权限先于状态读取，PO不出边界，相关日志脱敏。
- Literal rule enforcement: Rule 7, Rule 11；此文件直接按所列原始规则落实命名、验证、类型/注入或配置，其他规则由同Step关联文件负责。
- Implementation pseudocode:

```yaml
INPUT -> local证书也HTTPS，JPA目录未迁。
FLOW -> mirror YAML key tree and map item schema; secret references local protected mounts; no plaintext application config
ON INVALID/CONCURRENT/DEPENDENCY FAILURE -> missing key parity FAIL。
RESULT -> only the named symbol/contract in this File block becomes available; preserve tenant, expectedVersion, scope, UTC time and secret masking stated above.
```

- Verification contribution: 本Step `mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthManagedSchemaContractTest -Dsurefire.failIfNoSpecifiedTests=false test`；此文件负责 same config keys 的正例与 missing key parity FAIL 负例。
- After this file: same config keys 的字段/入口或注册已落盘；与下一文件组合后本Step才能GREEN，不能把中间状态当完成。

- Validation working directory: `/Users/mario/SelfProject/Egon-COLA`。
- Verification command: `mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthManagedSchemaContractTest -Dsurefire.failIfNoSpecifiedTests=false test`。
- Expected result: 先确认上述RED由缺失目标行为引起，GREEN时命名用例成功、模块编译成功、`git diff --check`零错误；不以不存在依赖/数据库/fixture而失败作为RED。
- Failure returns to: Step 1 的编译/Schema前置；若需改变外部语义、架构或DDL版本则回主Spec。
- Completion criteria: 一个新SQL版本定义11表及约束；NATIVE SINGLE精准落在tianquan_oauth；本Step的所有Manual Checks在实际执行时给出证据，未通过不能提交。
- Rollback: 提交前仅恢复本Step的精确路径；DDL已对真实数据运行后只forward-fix和新版本，不能编辑既有SQL/历史或恢复旧安全世代。
- Commit paths: `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/config/OAuthManagedSchemaContractTest.java`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/resources/db/egon-mp/V20260922_001__initialize_oauth_tenant_schema.sql`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/resources/db/egon-mp/repository-manifest.json`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/resources/oauth-sharding-native.yml`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/config/OAuthPersistenceConfiguration.java`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/resources/application.yml`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/resources/application-local.yml`。
- Commit: `feat(shoubing): define managed oauth schema and routing`。

### Step 3 — 持久化issuer与客户端注册

- Requirements: REQ-002, REQ-005, REQ-011, REQ-018
- Dependencies: Step 2
- Baseline state: 当前HEAD的对应源码/测试如每File的Repository evidence，先前Step已完成并有其focused GREEN；本Step尚不存在OAuthRealmPO所列新行为。
- Observable outcome: 这组认证事实的Repository/Mapper能按同realm/tenant/active/version读写。
- End state: 这组认证事实的Repository/Mapper能按同realm/tenant/active/version读写；后续Step的Consumer尚未切流，不声明完整生产验收。
- Test-first gate: Required — 必需PO/BO/Repository签名先让test编译；测试在Mapper/Impl装配前因缺Bean或缺具名SQL按目标RED，随后GREEN。
- Manual Checks: MC-ARCH-001, MC-REUSE-001, MC-NAME-001, MC-VALID-001, MC-MODEL-001, MC-CONVERT-001, MC-LOG-001, MC-BEAN-001, MC-JSON-001, MC-TIME-001, MC-SCOPE-001, MC-TEST-001
- Literal Rules: Rule 1, Rule 2, Rule 3, Rule 4, Rule 6, Rule 9, Rule 10, Rule 11
- Ordered files:

#### File 1 — `CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/domain/bo/TrustedRealmBO.java`

- Purpose: 关闭 `TrustedRealmBO` 在主Spec中的字段/消费依赖。
- Symbols: `TrustedRealmBO`。
- Repository evidence: 当前不存在；主Spec §7.3.1/§10 命名此对象/字段；现有相邻业务包提供放置证据。
- Dependencies and consumers: 本Step既有File块调用该类型；数据来源/消费者为主Spec §7.3.1/§10，不产生新HTTP路由。
- Why now: 该类型按本Step的测试/编译依赖顺序落位，以免由实现者临时发明。
- Contract/signature changes: issuer/realmKey/realmType/storagePartitionId/businessTenantId/trustVersion; TENANT business=storage, PLATFORM business=null; @Valid and class Lombok baseline。
- Input/output and state mapping: 精确字段由主Spec §7.3.1/§10 指定，realm从签名/注册得出，Instant为UTC，版本单调，秘密字段仅服务器内部。
- Error and edge behavior: null/未知字段/跨tenant/重复版本拒绝；不转成宽松缺省或吞异常。
- Standards impact: MC-ARCH-001, MC-NAME-001, MC-VALID-001, MC-MODEL-001, MC-CONVERT-001, MC-BEAN-001, MC-JSON-001, MC-TIME-001, MC-SCOPE-001, MC-TEST-001；行为Bean使用具名final Qualifier、@RequiredArgsConstructor和@Slf4j，载体遵循四注解与Builder。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 6, Rule 9, Rule 10, Rule 11；class按POJO语义命名，边界注解验证，跨层转换只用既有BaseConverter，Jackson和java.time字段明确。
- Implementation pseudocode:

```java
INPUT -> 仅接受主Spec §7.3.1/§10 的可信issuer/realm/输入字段；调用前 @Valid 或 ValueObject不变量检查。
FLOW -> issuer/realmKey/realmType/storagePartitionId/businessTenantId/trustVersion; TENANT business=storage, PLATFORM business=null; @Valid and class Lombok baseline。
OUTPUT -> TrustedRealmBO 的Java方法、DTO字段或配置可被本Step既有调用者编译使用；不新增业务API。
FAIL -> 空值/跨tenant/未知schema/重复版本/外部依赖错误按本Step已列错误模型fail-closed，不能输出原secret/token。
```

- Verification contribution: Step 3 的focus测试对 `TrustedRealmBO` 的字段、装配、负例及不泄密断言。
- After this file: `TrustedRealmBO` 成为已声明的Step依赖，下一文件可按原合同引用；未完成整条行为前不宣称GREEN。

#### File 2 — `CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/domain/enums/RealmTypeEnum.java`

- Purpose: 关闭 `RealmTypeEnum` 在主Spec中的字段/消费依赖。
- Symbols: `RealmTypeEnum`。
- Repository evidence: 当前不存在；主Spec §7.3.1/§10 命名此对象/字段；现有相邻业务包提供放置证据。
- Dependencies and consumers: 本Step既有File块调用该类型；数据来源/消费者为主Spec §7.3.1/§10，不产生新HTTP路由。
- Why now: 该类型按本Step的测试/编译依赖顺序落位，以免由实现者临时发明。
- Contract/signature changes: TENANT/PLATFORM stable DB @EnumValue, wire @JsonValue, unknown input rejected。
- Input/output and state mapping: 精确字段由主Spec §7.3.1/§10 指定，realm从签名/注册得出，Instant为UTC，版本单调，秘密字段仅服务器内部。
- Error and edge behavior: null/未知字段/跨tenant/重复版本拒绝；不转成宽松缺省或吞异常。
- Standards impact: MC-ARCH-001, MC-NAME-001, MC-VALID-001, MC-MODEL-001, MC-CONVERT-001, MC-BEAN-001, MC-JSON-001, MC-TIME-001, MC-SCOPE-001, MC-TEST-001；行为Bean使用具名final Qualifier、@RequiredArgsConstructor和@Slf4j，载体遵循四注解与Builder。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 6, Rule 9, Rule 10, Rule 11；class按POJO语义命名，边界注解验证，跨层转换只用既有BaseConverter，Jackson和java.time字段明确。
- Implementation pseudocode:

```java
INPUT -> 仅接受主Spec §7.3.1/§10 的可信issuer/realm/输入字段；调用前 @Valid 或 ValueObject不变量检查。
FLOW -> TENANT/PLATFORM stable DB @EnumValue, wire @JsonValue, unknown input rejected。
OUTPUT -> RealmTypeEnum 的Java方法、DTO字段或配置可被本Step既有调用者编译使用；不新增业务API。
FAIL -> 空值/跨tenant/未知schema/重复版本/外部依赖错误按本Step已列错误模型fail-closed，不能输出原secret/token。
```

- Verification contribution: Step 3 的focus测试对 `RealmTypeEnum` 的字段、装配、负例及不泄密断言。
- After this file: `RealmTypeEnum` 成为已声明的Step依赖，下一文件可按原合同引用；未完成整条行为前不宣称GREEN。

#### File 3 — `CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/domain/enums/OAuthClientPurposeEnum.java`

- Purpose: 关闭 `OAuthClientPurposeEnum` 在主Spec中的字段/消费依赖。
- Symbols: `OAuthClientPurposeEnum`。
- Repository evidence: 当前不存在；主Spec §7.3.1/§10 命名此对象/字段；现有相邻业务包提供放置证据。
- Dependencies and consumers: 本Step既有File块调用该类型；数据来源/消费者为主Spec §7.3.1/§10，不产生新HTTP路由。
- Why now: 该类型按本Step的测试/编译依赖顺序落位，以免由实现者临时发明。
- Contract/signature changes: USER/SERVICE stable DB @EnumValue and front @JsonValue; immutable purpose after create。
- Input/output and state mapping: 精确字段由主Spec §7.3.1/§10 指定，realm从签名/注册得出，Instant为UTC，版本单调，秘密字段仅服务器内部。
- Error and edge behavior: null/未知字段/跨tenant/重复版本拒绝；不转成宽松缺省或吞异常。
- Standards impact: MC-ARCH-001, MC-NAME-001, MC-VALID-001, MC-MODEL-001, MC-CONVERT-001, MC-BEAN-001, MC-JSON-001, MC-TIME-001, MC-SCOPE-001, MC-TEST-001；行为Bean使用具名final Qualifier、@RequiredArgsConstructor和@Slf4j，载体遵循四注解与Builder。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 6, Rule 9, Rule 10, Rule 11；class按POJO语义命名，边界注解验证，跨层转换只用既有BaseConverter，Jackson和java.time字段明确。
- Implementation pseudocode:

```java
INPUT -> 仅接受主Spec §7.3.1/§10 的可信issuer/realm/输入字段；调用前 @Valid 或 ValueObject不变量检查。
FLOW -> USER/SERVICE stable DB @EnumValue and front @JsonValue; immutable purpose after create。
OUTPUT -> OAuthClientPurposeEnum 的Java方法、DTO字段或配置可被本Step既有调用者编译使用；不新增业务API。
FAIL -> 空值/跨tenant/未知schema/重复版本/外部依赖错误按本Step已列错误模型fail-closed，不能输出原secret/token。
```

- Verification contribution: Step 3 的focus测试对 `OAuthClientPurposeEnum` 的字段、装配、负例及不泄密断言。
- After this file: `OAuthClientPurposeEnum` 成为已声明的Step依赖，下一文件可按原合同引用；未完成整条行为前不宣称GREEN。

#### File 4 — `CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/domain/po/OAuthRealmPO.java`

- Purpose: EgonModel承载本表完整列。
- Symbols: `OAuthRealmPO`。
- Repository evidence: 当前不存在；Spec §8/§11 明确目标，邻近包可验证；目标源于主Spec §7–§16。
- Dependencies and consumers: 输入依赖 Step 2 与本Step此前文件；输出供 这组认证事实的Repository/Mapper能按同realm/tenant/active/version读写及后一Step使用；Java Bean/Mapper按模块现有配置发现，前端按现有auth client消费。
- Why now: 本Step的编译前置/RED契约先锁定； EgonModel承载本表完整列在此位置后才可接消费者。
- Contract/signature changes: OAuthRealmPO；EgonModel承载本表完整列。公开行为只取主Spec §9对应API或§10/11内部合同。
- Input/output and state mapping: 11.2 oauth_realm native columns snake_case→camelCase; inherited id/tenant/audit/deletedAt/version only一次。
- Error and edge behavior: UNIQUE/NULL/tenant违反时映射typed conflict；不可向HTTP返回PO。
- Standards impact: MC-ARCH-001, MC-SCOPE-001, MC-TEST-001, MC-VALID-001；按已选传统分层，输入校验与权限先于状态读取，PO不出边界，相关日志脱敏。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 6, Rule 10, Rule 11；此文件直接按所列原始规则落实命名、验证、类型/注入或配置，其他规则由同Step关联文件负责。
- Implementation pseudocode:

```java
INPUT -> 11.2 oauth_realm native columns snake_case→camelCase; inherited id/tenant/audit/deletedAt/version only一次。
FLOW -> @TableName("oauth_realm") class OAuthRealmPO extends EgonModel<OAuthRealmPO>; §11 oauth_realm business fields; @Data/@NoArgsConstructor/@AllArgsConstructor/@Accessors(chain=true)/@SuperBuilder/@EqualsAndHashCode(callSuper=true); @EnumValue/secret exclude
ON INVALID/CONCURRENT/DEPENDENCY FAILURE -> UNIQUE/NULL/tenant违反时映射typed conflict；不可向HTTP返回PO。
RESULT -> only the named symbol/contract in this File block becomes available; preserve tenant, expectedVersion, scope, UTC time and secret masking stated above.
```

- Verification contribution: 本Step `mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthRealmPersistenceContractTest -Dsurefire.failIfNoSpecifiedTests=false test`；此文件负责 OAuthRealmPO 的正例与 UNIQUE/NULL/tenant违反时映射typed conflict；不可向HTTP返回PO 负例。
- After this file: OAuthRealmPO 的字段/入口或注册已落盘；与下一文件组合后本Step才能GREEN，不能把中间状态当完成。

#### File 5 — `CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/domain/bo/OAuthRealmBO.java`

- Purpose: Service计算与PO分界。
- Symbols: `OAuthRealmBO`。
- Repository evidence: 当前不存在；Spec §8/§11 明确目标，邻近包可验证；目标源于主Spec §7–§16。
- Dependencies and consumers: 输入依赖 Step 2 与本Step此前文件；输出供 这组认证事实的Repository/Mapper能按同realm/tenant/active/version读写及后一Step使用；Java Bean/Mapper按模块现有配置发现，前端按现有auth client消费。
- Why now: 前一文件建立本文件需要的字段或调用契约； Service计算与PO分界在此位置后才可接消费者。
- Contract/signature changes: OAuthRealmBO；Service计算与PO分界。公开行为只取主Spec §9对应API或§10/11内部合同。
- Input/output and state mapping: outbound business values only; platform owner context for registry bootstrap; tenant from TrustedRealmBO。
- Error and edge behavior: missing/invalid enum, scope or date rejected before Mapper。
- Standards impact: MC-ARCH-001, MC-SCOPE-001, MC-TEST-001, MC-VALID-001；按已选传统分层，输入校验与权限先于状态读取，PO不出边界，相关日志脱敏。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 6, Rule 10, Rule 11；此文件直接按所列原始规则落实命名、验证、类型/注入或配置，其他规则由同Step关联文件负责。
- Implementation pseudocode:

```java
INPUT -> outbound business values only; platform owner context for registry bootstrap; tenant from TrustedRealmBO。
FLOW -> class OAuthRealmBO { business fields from OAuthRealmPO; id,version; } @Data @NoArgsConstructor @AllArgsConstructor @Accessors(chain=true) @Builder; @Valid groups Create/Update/Query
ON INVALID/CONCURRENT/DEPENDENCY FAILURE -> missing/invalid enum, scope or date rejected before Mapper。
RESULT -> only the named symbol/contract in this File block becomes available; preserve tenant, expectedVersion, scope, UTC time and secret masking stated above.
```

- Verification contribution: 本Step `mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthRealmPersistenceContractTest -Dsurefire.failIfNoSpecifiedTests=false test`；此文件负责 OAuthRealmBO 的正例与 missing/invalid enum, scope or date rejected before Mapper 负例。
- After this file: OAuthRealmBO 的字段/入口或注册已落盘；与下一文件组合后本Step才能GREEN，不能把中间状态当完成。

#### File 6 — `CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/repository/OAuthRealmRepository.java`

- Purpose: Service-facing typed contract。
- Symbols: `OAuthRealmRepository`。
- Repository evidence: 当前不存在；Spec §8/§11 明确目标，邻近包可验证；目标源于主Spec §7–§16。
- Dependencies and consumers: 输入依赖 Step 2 与本Step此前文件；输出供 这组认证事实的Repository/Mapper能按同realm/tenant/active/version读写及后一Step使用；Java Bean/Mapper按模块现有配置发现，前端按现有auth client消费。
- Why now: 前一文件建立本文件需要的字段或调用契约； Service-facing typed contract在此位置后才可接消费者。
- Contract/signature changes: OAuthRealmRepository；Service-facing typed contract。公开行为只取主Spec §9对应API或§10/11内部合同。
- Input/output and state mapping: Service uses BO/Query/Command; no PO generic outward。
- Error and edge behavior: 0 affected row → typed conflict; no bypass tenant。
- Standards impact: MC-ARCH-001, MC-SCOPE-001, MC-TEST-001, MC-VALID-001；按已选传统分层，输入校验与权限先于状态读取，PO不出边界，相关日志脱敏。
- Literal rule enforcement: Rule 1, Rule 2, Rule 11；此文件直接按所列原始规则落实命名、验证、类型/注入或配置，其他规则由同Step关联文件负责。
- Implementation pseudocode:

```java
INPUT -> Service uses BO/Query/Command; no PO generic outward。
FLOW -> @Validated interface OAuthRealmRepository { Optional<OAuthRealmBO> find(TrustedRealmBO realm, ...); OAuthRealmBO save(@Valid OAuthRealmBO input,long expectedVersion); } names agree §11 access issuer/realm_key/status/storage_partition_id exact lookup
ON INVALID/CONCURRENT/DEPENDENCY FAILURE -> 0 affected row → typed conflict; no bypass tenant。
RESULT -> only the named symbol/contract in this File block becomes available; preserve tenant, expectedVersion, scope, UTC time and secret masking stated above.
```

- Verification contribution: 本Step `mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthRealmPersistenceContractTest -Dsurefire.failIfNoSpecifiedTests=false test`；此文件负责 OAuthRealmRepository 的正例与 0 affected row → typed conflict; no bypass tenant 负例。
- After this file: OAuthRealmRepository 的字段/入口或注册已落盘；与下一文件组合后本Step才能GREEN，不能把中间状态当完成。

#### File 7 — `CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/domain/po/OAuthClientPO.java`

- Purpose: EgonModel承载本表完整列。
- Symbols: `OAuthClientPO`。
- Repository evidence: 当前不存在；Spec §8/§11 明确目标，邻近包可验证；目标源于主Spec §7–§16。
- Dependencies and consumers: 输入依赖 Step 2 与本Step此前文件；输出供 这组认证事实的Repository/Mapper能按同realm/tenant/active/version读写及后一Step使用；Java Bean/Mapper按模块现有配置发现，前端按现有auth client消费。
- Why now: 前一文件建立本文件需要的字段或调用契约； EgonModel承载本表完整列在此位置后才可接消费者。
- Contract/signature changes: OAuthClientPO；EgonModel承载本表完整列。公开行为只取主Spec §9对应API或§10/11内部合同。
- Input/output and state mapping: 11.2 oauth_client native columns snake_case→camelCase; inherited id/tenant/audit/deletedAt/version only一次。
- Error and edge behavior: UNIQUE/NULL/tenant违反时映射typed conflict；不可向HTTP返回PO。
- Standards impact: MC-ARCH-001, MC-SCOPE-001, MC-TEST-001, MC-VALID-001；按已选传统分层，输入校验与权限先于状态读取，PO不出边界，相关日志脱敏。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 6, Rule 10, Rule 11；此文件直接按所列原始规则落实命名、验证、类型/注入或配置，其他规则由同Step关联文件负责。
- Implementation pseudocode:

```java
INPUT -> 11.2 oauth_client native columns snake_case→camelCase; inherited id/tenant/audit/deletedAt/version only一次。
FLOW -> @TableName("oauth_client") class OAuthClientPO extends EgonModel<OAuthClientPO>; §11 oauth_client business fields; @Data/@NoArgsConstructor/@AllArgsConstructor/@Accessors(chain=true)/@SuperBuilder/@EqualsAndHashCode(callSuper=true); @EnumValue/secret exclude
ON INVALID/CONCURRENT/DEPENDENCY FAILURE -> UNIQUE/NULL/tenant违反时映射typed conflict；不可向HTTP返回PO。
RESULT -> only the named symbol/contract in this File block becomes available; preserve tenant, expectedVersion, scope, UTC time and secret masking stated above.
```

- Verification contribution: 本Step `mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthRealmPersistenceContractTest -Dsurefire.failIfNoSpecifiedTests=false test`；此文件负责 OAuthClientPO 的正例与 UNIQUE/NULL/tenant违反时映射typed conflict；不可向HTTP返回PO 负例。
- After this file: OAuthClientPO 的字段/入口或注册已落盘；与下一文件组合后本Step才能GREEN，不能把中间状态当完成。

#### File 8 — `CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/domain/bo/OAuthClientBO.java`

- Purpose: Service计算与PO分界。
- Symbols: `OAuthClientBO`。
- Repository evidence: 当前不存在；Spec §8/§11 明确目标，邻近包可验证；目标源于主Spec §7–§16。
- Dependencies and consumers: 输入依赖 Step 2 与本Step此前文件；输出供 这组认证事实的Repository/Mapper能按同realm/tenant/active/version读写及后一Step使用；Java Bean/Mapper按模块现有配置发现，前端按现有auth client消费。
- Why now: 前一文件建立本文件需要的字段或调用契约； Service计算与PO分界在此位置后才可接消费者。
- Contract/signature changes: OAuthClientBO；Service计算与PO分界。公开行为只取主Spec §9对应API或§10/11内部合同。
- Input/output and state mapping: outbound business values only; bcrypt secret never maps into public DTO; tenant from TrustedRealmBO。
- Error and edge behavior: missing/invalid enum, scope or date rejected before Mapper。
- Standards impact: MC-ARCH-001, MC-SCOPE-001, MC-TEST-001, MC-VALID-001；按已选传统分层，输入校验与权限先于状态读取，PO不出边界，相关日志脱敏。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 6, Rule 10, Rule 11；此文件直接按所列原始规则落实命名、验证、类型/注入或配置，其他规则由同Step关联文件负责。
- Implementation pseudocode:

```java
INPUT -> outbound business values only; bcrypt secret never maps into public DTO; tenant from TrustedRealmBO。
FLOW -> class OAuthClientBO { business fields from OAuthClientPO; id,version; } @Data @NoArgsConstructor @AllArgsConstructor @Accessors(chain=true) @Builder; @Valid groups Create/Update/Query
ON INVALID/CONCURRENT/DEPENDENCY FAILURE -> missing/invalid enum, scope or date rejected before Mapper。
RESULT -> only the named symbol/contract in this File block becomes available; preserve tenant, expectedVersion, scope, UTC time and secret masking stated above.
```

- Verification contribution: 本Step `mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthRealmPersistenceContractTest -Dsurefire.failIfNoSpecifiedTests=false test`；此文件负责 OAuthClientBO 的正例与 missing/invalid enum, scope or date rejected before Mapper 负例。
- After this file: OAuthClientBO 的字段/入口或注册已落盘；与下一文件组合后本Step才能GREEN，不能把中间状态当完成。

#### File 9 — `CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/repository/OAuthClientRepository.java`

- Purpose: Service-facing typed contract。
- Symbols: `OAuthClientRepository`。
- Repository evidence: 当前不存在；Spec §8/§11 明确目标，邻近包可验证；目标源于主Spec §7–§16。
- Dependencies and consumers: 输入依赖 Step 2 与本Step此前文件；输出供 这组认证事实的Repository/Mapper能按同realm/tenant/active/version读写及后一Step使用；Java Bean/Mapper按模块现有配置发现，前端按现有auth client消费。
- Why now: 前一文件建立本文件需要的字段或调用契约； Service-facing typed contract在此位置后才可接消费者。
- Contract/signature changes: OAuthClientRepository；Service-facing typed contract。公开行为只取主Spec §9对应API或§10/11内部合同。
- Input/output and state mapping: Service uses BO/Query/Command; no PO generic outward。
- Error and edge behavior: 0 affected row → typed conflict; no bypass tenant。
- Standards impact: MC-ARCH-001, MC-SCOPE-001, MC-TEST-001, MC-VALID-001；按已选传统分层，输入校验与权限先于状态读取，PO不出边界，相关日志脱敏。
- Literal rule enforcement: Rule 1, Rule 2, Rule 11；此文件直接按所列原始规则落实命名、验证、类型/注入或配置，其他规则由同Step关联文件负责。
- Implementation pseudocode:

```java
INPUT -> Service uses BO/Query/Command; no PO generic outward。
FLOW -> @Validated interface OAuthClientRepository { Optional<OAuthClientBO> find(TrustedRealmBO realm, ...); OAuthClientBO save(@Valid OAuthClientBO input,long expectedVersion); } names agree §11 access tenant_id+client_id+active lookup; versioned client update
ON INVALID/CONCURRENT/DEPENDENCY FAILURE -> 0 affected row → typed conflict; no bypass tenant。
RESULT -> only the named symbol/contract in this File block becomes available; preserve tenant, expectedVersion, scope, UTC time and secret masking stated above.
```

- Verification contribution: 本Step `mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthRealmPersistenceContractTest -Dsurefire.failIfNoSpecifiedTests=false test`；此文件负责 OAuthClientRepository 的正例与 0 affected row → typed conflict; no bypass tenant 负例。
- After this file: OAuthClientRepository 的字段/入口或注册已落盘；与下一文件组合后本Step才能GREEN，不能把中间状态当完成。

#### File 10 — `CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/repository/OAuthRealmPersistenceContractTest.java`

- Purpose: RED证明这组映射/查询/跨租户错误。
- Symbols: `OAuthRealmPersistenceContractTest`。
- Repository evidence: 当前不存在；Spec §8/§11 明确目标，邻近包可验证；目标源于主Spec §7–§16。
- Dependencies and consumers: 输入依赖 Step 2 与本Step此前文件；输出供 这组认证事实的Repository/Mapper能按同realm/tenant/active/version读写及后一Step使用；Java Bean/Mapper按模块现有配置发现，前端按现有auth client消费。
- Why now: 前一文件建立本文件需要的字段或调用契约； RED证明这组映射/查询/跨租户错误在此位置后才可接消费者。
- Contract/signature changes: OAuthRealmPersistenceContractTest；RED证明这组映射/查询/跨租户错误。公开行为只取主Spec §9对应API或§10/11内部合同。
- Input/output and state mapping: fixed tenant=1001/1002; timestamps Instant/UTC; SQL actual node tianquan_oauth。
- Error and edge behavior: no implementation Bean/Mapper yet → focused contract RED，不能因fixture fail。
- Standards impact: MC-ARCH-001, MC-SCOPE-001, MC-TEST-001, MC-VALID-001；按已选传统分层，输入校验与权限先于状态读取，PO不出边界，相关日志脱敏。
- Literal rule enforcement: Rule 2, Rule 3, Rule 11；此文件直接按所列原始规则落实命名、验证、类型/注入或配置，其他规则由同Step关联文件负责。
- Implementation pseudocode:

```java
INPUT -> fixed tenant=1001/1002; timestamps Instant/UTC; SQL actual node tianquan_oauth。
FLOW -> @Test for OAuthRealm/OAuthClient: assert same external client/resource key in different trusted tenants does not cross; query includes tenant and deleted_at IS NULL; concurrent expectedVersion loser fails; secret/ciphertext absent from VO
ON INVALID/CONCURRENT/DEPENDENCY FAILURE -> no implementation Bean/Mapper yet → focused contract RED，不能因fixture fail。
RESULT -> only the named symbol/contract in this File block becomes available; preserve tenant, expectedVersion, scope, UTC time and secret masking stated above.
```

- Verification contribution: 本Step `mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthRealmPersistenceContractTest -Dsurefire.failIfNoSpecifiedTests=false test`；此文件负责 OAuthRealmPersistenceContractTest 的正例与 no implementation Bean/Mapper yet → focused contract RED，不能因fixture fail 负例。
- After this file: OAuthRealmPersistenceContractTest 的字段/入口或注册已落盘；与下一文件组合后本Step才能GREEN，不能把中间状态当完成。

#### File 11 — `CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/domain/converter/OAuthRealmConverter.java`

- Purpose: PO↔BO双向MapStruct转换。
- Symbols: `OAuthRealmConverter`。
- Repository evidence: 当前不存在；Spec §8/§11 明确目标，邻近包可验证；目标源于主Spec §7–§16。
- Dependencies and consumers: 输入依赖 Step 2 与本Step此前文件；输出供 这组认证事实的Repository/Mapper能按同realm/tenant/active/version读写及后一Step使用；Java Bean/Mapper按模块现有配置发现，前端按现有auth client消费。
- Why now: 前一文件建立本文件需要的字段或调用契约； PO↔BO双向MapStruct转换在此位置后才可接消费者。
- Contract/signature changes: OAuthRealmConverter；PO↔BO双向MapStruct转换。公开行为只取主Spec §9对应API或§10/11内部合同。
- Input/output and state mapping: OAuthRealmPO business fields from §11 → OAuthRealmBO; unknown enum拒绝，null按列约束。
- Error and edge behavior: generated implementation/constructor must compile; no BeanUtils/json copy。
- Standards impact: MC-ARCH-001, MC-SCOPE-001, MC-TEST-001, MC-VALID-001；按已选传统分层，输入校验与权限先于状态读取，PO不出边界，相关日志脱敏。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 6, Rule 10, Rule 11；此文件直接按所列原始规则落实命名、验证、类型/注入或配置，其他规则由同Step关联文件负责。
- Implementation pseudocode:

```java
INPUT -> OAuthRealmPO business fields from §11 → OAuthRealmBO; unknown enum拒绝，null按列约束。
FLOW -> @Mapper(componentModel="spring") interface OAuthRealmConverter extends BaseConverter<OAuthRealmBO,OAuthRealmPO>; @Mapping inherited tenant/audit fill ignored on insert; enum code explicit; secret excluded from toString/VO; mapping date Instant/UTC
ON INVALID/CONCURRENT/DEPENDENCY FAILURE -> generated implementation/constructor must compile; no BeanUtils/json copy。
RESULT -> only the named symbol/contract in this File block becomes available; preserve tenant, expectedVersion, scope, UTC time and secret masking stated above.
```

- Verification contribution: 本Step `mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthRealmPersistenceContractTest -Dsurefire.failIfNoSpecifiedTests=false test`；此文件负责 OAuthRealmConverter 的正例与 generated implementation/constructor must compile; no BeanUtils/json copy 负例。
- After this file: OAuthRealmConverter 的字段/入口或注册已落盘；与下一文件组合后本Step才能GREEN，不能把中间状态当完成。

#### File 12 — `CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/dao/OAuthRealmMapper.java`

- Purpose: MP Mapper具名SQL接口。
- Symbols: `OAuthRealmMapper`。
- Repository evidence: 当前不存在；Spec §8/§11 明确目标，邻近包可验证；目标源于主Spec §7–§16。
- Dependencies and consumers: 输入依赖 Step 2 与本Step此前文件；输出供 这组认证事实的Repository/Mapper能按同realm/tenant/active/version读写及后一Step使用；Java Bean/Mapper按模块现有配置发现，前端按现有auth client消费。
- Why now: 前一文件建立本文件需要的字段或调用契约； MP Mapper具名SQL接口在此位置后才可接消费者。
- Contract/signature changes: OAuthRealmMapper；MP Mapper具名SQL接口。公开行为只取主Spec §9对应API或§10/11内部合同。
- Input/output and state mapping: tenantId positive Long from verified MDC; result OAuthRealmPO。
- Error and edge behavior: no LambdaQueryChain/Wrapper; missing tenant failclosed。
- Standards impact: MC-ARCH-001, MC-SCOPE-001, MC-TEST-001, MC-VALID-001；按已选传统分层，输入校验与权限先于状态读取，PO不出边界，相关日志脱敏。
- Literal rule enforcement: Rule 1, Rule 2, Rule 11；此文件直接按所列原始规则落实命名、验证、类型/注入或配置，其他规则由同Step关联文件负责。
- Implementation pseudocode:

```java
INPUT -> tenantId positive Long from verified MDC; result OAuthRealmPO。
FLOW -> interface OAuthRealmMapper extends EgonColaMapper<OAuthRealmPO>; methods selectActiveById/Ids, deleteVersionedById, issuer/realm_key/status/storage_partition_id exact lookup SQL bindings
ON INVALID/CONCURRENT/DEPENDENCY FAILURE -> no LambdaQueryChain/Wrapper; missing tenant failclosed。
RESULT -> only the named symbol/contract in this File block becomes available; preserve tenant, expectedVersion, scope, UTC time and secret masking stated above.
```

- Verification contribution: 本Step `mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthRealmPersistenceContractTest -Dsurefire.failIfNoSpecifiedTests=false test`；此文件负责 OAuthRealmMapper 的正例与 no LambdaQueryChain/Wrapper; missing tenant failclosed 负例。
- After this file: OAuthRealmMapper 的字段/入口或注册已落盘；与下一文件组合后本Step才能GREEN，不能把中间状态当完成。

#### File 13 — `CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/resources/mapper/oauth/OAuthRealmMapper.xml`

- Purpose: 具名PostgreSQL谓词/锁。
- Symbols: `OAuthRealmMapper SQL`。
- Repository evidence: 当前不存在；Spec §8/§11 明确目标，邻近包可验证；目标源于主Spec §7–§16。
- Dependencies and consumers: 输入依赖 Step 2 与本Step此前文件；输出供 这组认证事实的Repository/Mapper能按同realm/tenant/active/version读写及后一Step使用；Java Bean/Mapper按模块现有配置发现，前端按现有auth client消费。
- Why now: 前一文件建立本文件需要的字段或调用契约； 具名PostgreSQL谓词/锁在此位置后才可接消费者。
- Contract/signature changes: OAuthRealmMapper SQL；具名PostgreSQL谓词/锁。公开行为只取主Spec §9对应API或§10/11内部合同。
- Input/output and state mapping: 0/1 precise query or capped list; indexes §11。
- Error and edge behavior: tenant range forbidden, 0 rows conflict, no cross-group write。
- Standards impact: MC-SCOPE-001, MC-TEST-001；按已选传统分层，输入校验与权限先于状态读取，PO不出边界，相关日志脱敏。
- Literal rule enforcement: Rule 2, Rule 10, Rule 11；此文件直接按所列原始规则落实命名、验证、类型/注入或配置，其他规则由同Step关联文件负责。
- Implementation pseudocode:

```xml
INPUT -> 0/1 precise query or capped list; indexes §11。
FLOW -> SELECT business columns + inherited fields FROM tianquan_oauth.oauth_realm WHERE tenant_id=#{tenantId} AND deleted_at IS NULL AND issuer/realm_key/status/storage_partition_id exact lookup; guarded UPDATE id+tenant+version+expected state; SELECT FOR UPDATE only command path; max page bounded
ON INVALID/CONCURRENT/DEPENDENCY FAILURE -> tenant range forbidden, 0 rows conflict, no cross-group write。
RESULT -> only the named symbol/contract in this File block becomes available; preserve tenant, expectedVersion, scope, UTC time and secret masking stated above.
```

- Verification contribution: 本Step `mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthRealmPersistenceContractTest -Dsurefire.failIfNoSpecifiedTests=false test`；此文件负责 OAuthRealmMapper SQL 的正例与 tenant range forbidden, 0 rows conflict, no cross-group write 负例。
- After this file: OAuthRealmMapper SQL 的字段/入口或注册已落盘；与下一文件组合后本Step才能GREEN，不能把中间状态当完成。

#### File 14 — `CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/repository/impl/OAuthRealmRepositoryImpl.java`

- Purpose: MP repository ACL。
- Symbols: `OAuthRealmRepositoryImpl`。
- Repository evidence: 当前不存在；Spec §8/§11 明确目标，邻近包可验证；目标源于主Spec §7–§16。
- Dependencies and consumers: 输入依赖 Step 2 与本Step此前文件；输出供 这组认证事实的Repository/Mapper能按同realm/tenant/active/version读写及后一Step使用；Java Bean/Mapper按模块现有配置发现，前端按现有auth client消费。
- Why now: 前一文件建立本文件需要的字段或调用契约； MP repository ACL在此位置后才可接消费者。
- Contract/signature changes: OAuthRealmRepositoryImpl；MP repository ACL。公开行为只取主Spec §9对应API或§10/11内部合同。
- Input/output and state mapping: OAuthRealmBO↔PO; raw secret/private only protected internal fields。
- Error and edge behavior: UnsupportedOperationException for generic unscoped query; errors mapped without sensitive values。
- Standards impact: MC-ARCH-001, MC-SCOPE-001, MC-TEST-001, MC-VALID-001；按已选传统分层，输入校验与权限先于状态读取，PO不出边界，相关日志脱敏。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 6, Rule 9, Rule 10, Rule 11；此文件直接按所列原始规则落实命名、验证、类型/注入或配置，其他规则由同Step关联文件负责。
- Implementation pseudocode:

```java
INPUT -> OAuthRealmBO↔PO; raw secret/private only protected internal fields。
FLOW -> @Repository("oAuthRealmRepository") @Slf4j @RequiredArgsConstructor; final @Qualifier("oAuthRealmMapper") Mapper, converter, modelValidationUtils, properties; extends EgonColaRepository<Mapper,PO>; find maps PO→BO; save maps BO→PO, guarded version update result==1
ON INVALID/CONCURRENT/DEPENDENCY FAILURE -> UnsupportedOperationException for generic unscoped query; errors mapped without sensitive values。
RESULT -> only the named symbol/contract in this File block becomes available; preserve tenant, expectedVersion, scope, UTC time and secret masking stated above.
```

- Verification contribution: 本Step `mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthRealmPersistenceContractTest -Dsurefire.failIfNoSpecifiedTests=false test`；此文件负责 OAuthRealmRepositoryImpl 的正例与 UnsupportedOperationException for generic unscoped query; errors mapped without sensitive values 负例。
- After this file: OAuthRealmRepositoryImpl 的字段/入口或注册已落盘；与下一文件组合后本Step才能GREEN，不能把中间状态当完成。

#### File 15 — `CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/domain/converter/OAuthClientConverter.java`

- Purpose: PO↔BO双向MapStruct转换。
- Symbols: `OAuthClientConverter`。
- Repository evidence: 当前不存在；Spec §8/§11 明确目标，邻近包可验证；目标源于主Spec §7–§16。
- Dependencies and consumers: 输入依赖 Step 2 与本Step此前文件；输出供 这组认证事实的Repository/Mapper能按同realm/tenant/active/version读写及后一Step使用；Java Bean/Mapper按模块现有配置发现，前端按现有auth client消费。
- Why now: 前一文件建立本文件需要的字段或调用契约； PO↔BO双向MapStruct转换在此位置后才可接消费者。
- Contract/signature changes: OAuthClientConverter；PO↔BO双向MapStruct转换。公开行为只取主Spec §9对应API或§10/11内部合同。
- Input/output and state mapping: OAuthClientPO business fields from §11 → OAuthClientBO; unknown enum拒绝，null按列约束。
- Error and edge behavior: generated implementation/constructor must compile; no BeanUtils/json copy。
- Standards impact: MC-ARCH-001, MC-SCOPE-001, MC-TEST-001, MC-VALID-001；按已选传统分层，输入校验与权限先于状态读取，PO不出边界，相关日志脱敏。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 6, Rule 10, Rule 11；此文件直接按所列原始规则落实命名、验证、类型/注入或配置，其他规则由同Step关联文件负责。
- Implementation pseudocode:

```java
INPUT -> OAuthClientPO business fields from §11 → OAuthClientBO; unknown enum拒绝，null按列约束。
FLOW -> @Mapper(componentModel="spring") interface OAuthClientConverter extends BaseConverter<OAuthClientBO,OAuthClientPO>; @Mapping inherited tenant/audit fill ignored on insert; enum code explicit; secret excluded from toString/VO; mapping date Instant/UTC
ON INVALID/CONCURRENT/DEPENDENCY FAILURE -> generated implementation/constructor must compile; no BeanUtils/json copy。
RESULT -> only the named symbol/contract in this File block becomes available; preserve tenant, expectedVersion, scope, UTC time and secret masking stated above.
```

- Verification contribution: 本Step `mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthRealmPersistenceContractTest -Dsurefire.failIfNoSpecifiedTests=false test`；此文件负责 OAuthClientConverter 的正例与 generated implementation/constructor must compile; no BeanUtils/json copy 负例。
- After this file: OAuthClientConverter 的字段/入口或注册已落盘；与下一文件组合后本Step才能GREEN，不能把中间状态当完成。

#### File 16 — `CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/dao/OAuthClientMapper.java`

- Purpose: MP Mapper具名SQL接口。
- Symbols: `OAuthClientMapper`。
- Repository evidence: 当前不存在；Spec §8/§11 明确目标，邻近包可验证；目标源于主Spec §7–§16。
- Dependencies and consumers: 输入依赖 Step 2 与本Step此前文件；输出供 这组认证事实的Repository/Mapper能按同realm/tenant/active/version读写及后一Step使用；Java Bean/Mapper按模块现有配置发现，前端按现有auth client消费。
- Why now: 前一文件建立本文件需要的字段或调用契约； MP Mapper具名SQL接口在此位置后才可接消费者。
- Contract/signature changes: OAuthClientMapper；MP Mapper具名SQL接口。公开行为只取主Spec §9对应API或§10/11内部合同。
- Input/output and state mapping: tenantId positive Long from verified MDC; result OAuthClientPO。
- Error and edge behavior: no LambdaQueryChain/Wrapper; missing tenant failclosed。
- Standards impact: MC-ARCH-001, MC-SCOPE-001, MC-TEST-001, MC-VALID-001；按已选传统分层，输入校验与权限先于状态读取，PO不出边界，相关日志脱敏。
- Literal rule enforcement: Rule 1, Rule 2, Rule 11；此文件直接按所列原始规则落实命名、验证、类型/注入或配置，其他规则由同Step关联文件负责。
- Implementation pseudocode:

```java
INPUT -> tenantId positive Long from verified MDC; result OAuthClientPO。
FLOW -> interface OAuthClientMapper extends EgonColaMapper<OAuthClientPO>; methods selectActiveById/Ids, deleteVersionedById, tenant_id+client_id+active lookup; versioned client update SQL bindings
ON INVALID/CONCURRENT/DEPENDENCY FAILURE -> no LambdaQueryChain/Wrapper; missing tenant failclosed。
RESULT -> only the named symbol/contract in this File block becomes available; preserve tenant, expectedVersion, scope, UTC time and secret masking stated above.
```

- Verification contribution: 本Step `mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthRealmPersistenceContractTest -Dsurefire.failIfNoSpecifiedTests=false test`；此文件负责 OAuthClientMapper 的正例与 no LambdaQueryChain/Wrapper; missing tenant failclosed 负例。
- After this file: OAuthClientMapper 的字段/入口或注册已落盘；与下一文件组合后本Step才能GREEN，不能把中间状态当完成。

#### File 17 — `CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/resources/mapper/oauth/OAuthClientMapper.xml`

- Purpose: 具名PostgreSQL谓词/锁。
- Symbols: `OAuthClientMapper SQL`。
- Repository evidence: 当前不存在；Spec §8/§11 明确目标，邻近包可验证；目标源于主Spec §7–§16。
- Dependencies and consumers: 输入依赖 Step 2 与本Step此前文件；输出供 这组认证事实的Repository/Mapper能按同realm/tenant/active/version读写及后一Step使用；Java Bean/Mapper按模块现有配置发现，前端按现有auth client消费。
- Why now: 前一文件建立本文件需要的字段或调用契约； 具名PostgreSQL谓词/锁在此位置后才可接消费者。
- Contract/signature changes: OAuthClientMapper SQL；具名PostgreSQL谓词/锁。公开行为只取主Spec §9对应API或§10/11内部合同。
- Input/output and state mapping: 0/1 precise query or capped list; indexes §11。
- Error and edge behavior: tenant range forbidden, 0 rows conflict, no cross-group write。
- Standards impact: MC-SCOPE-001, MC-TEST-001；按已选传统分层，输入校验与权限先于状态读取，PO不出边界，相关日志脱敏。
- Literal rule enforcement: Rule 2, Rule 10, Rule 11；此文件直接按所列原始规则落实命名、验证、类型/注入或配置，其他规则由同Step关联文件负责。
- Implementation pseudocode:

```xml
INPUT -> 0/1 precise query or capped list; indexes §11。
FLOW -> SELECT business columns + inherited fields FROM tianquan_oauth.oauth_client WHERE tenant_id=#{tenantId} AND deleted_at IS NULL AND tenant_id+client_id+active lookup; versioned client update; guarded UPDATE id+tenant+version+expected state; SELECT FOR UPDATE only command path; max page bounded
ON INVALID/CONCURRENT/DEPENDENCY FAILURE -> tenant range forbidden, 0 rows conflict, no cross-group write。
RESULT -> only the named symbol/contract in this File block becomes available; preserve tenant, expectedVersion, scope, UTC time and secret masking stated above.
```

- Verification contribution: 本Step `mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthRealmPersistenceContractTest -Dsurefire.failIfNoSpecifiedTests=false test`；此文件负责 OAuthClientMapper SQL 的正例与 tenant range forbidden, 0 rows conflict, no cross-group write 负例。
- After this file: OAuthClientMapper SQL 的字段/入口或注册已落盘；与下一文件组合后本Step才能GREEN，不能把中间状态当完成。

#### File 18 — `CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/repository/impl/OAuthClientRepositoryImpl.java`

- Purpose: MP repository ACL。
- Symbols: `OAuthClientRepositoryImpl`。
- Repository evidence: 当前不存在；Spec §8/§11 明确目标，邻近包可验证；目标源于主Spec §7–§16。
- Dependencies and consumers: 输入依赖 Step 2 与本Step此前文件；输出供 这组认证事实的Repository/Mapper能按同realm/tenant/active/version读写及后一Step使用；Java Bean/Mapper按模块现有配置发现，前端按现有auth client消费。
- Why now: 前一文件建立本文件需要的字段或调用契约； MP repository ACL在此位置后才可接消费者。
- Contract/signature changes: OAuthClientRepositoryImpl；MP repository ACL。公开行为只取主Spec §9对应API或§10/11内部合同。
- Input/output and state mapping: OAuthClientBO↔PO; raw secret/private only protected internal fields。
- Error and edge behavior: UnsupportedOperationException for generic unscoped query; errors mapped without sensitive values。
- Standards impact: MC-ARCH-001, MC-SCOPE-001, MC-TEST-001, MC-VALID-001；按已选传统分层，输入校验与权限先于状态读取，PO不出边界，相关日志脱敏。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 6, Rule 9, Rule 10, Rule 11；此文件直接按所列原始规则落实命名、验证、类型/注入或配置，其他规则由同Step关联文件负责。
- Implementation pseudocode:

```java
INPUT -> OAuthClientBO↔PO; raw secret/private only protected internal fields。
FLOW -> @Repository("oAuthClientRepository") @Slf4j @RequiredArgsConstructor; final @Qualifier("oAuthClientMapper") Mapper, converter, modelValidationUtils, properties; extends EgonColaRepository<Mapper,PO>; find maps PO→BO; save maps BO→PO, guarded version update result==1
ON INVALID/CONCURRENT/DEPENDENCY FAILURE -> UnsupportedOperationException for generic unscoped query; errors mapped without sensitive values。
RESULT -> only the named symbol/contract in this File block becomes available; preserve tenant, expectedVersion, scope, UTC time and secret masking stated above.
```

- Verification contribution: 本Step `mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthRealmPersistenceContractTest -Dsurefire.failIfNoSpecifiedTests=false test`；此文件负责 OAuthClientRepositoryImpl 的正例与 UnsupportedOperationException for generic unscoped query; errors mapped without sensitive values 负例。
- After this file: OAuthClientRepositoryImpl 的字段/入口或注册已落盘；与下一文件组合后本Step才能GREEN，不能把中间状态当完成。

- Validation working directory: `/Users/mario/SelfProject/Egon-COLA`。
- Verification command: `mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthRealmPersistenceContractTest -Dsurefire.failIfNoSpecifiedTests=false test`。
- Expected result: 先确认上述RED由缺失目标行为引起，GREEN时命名用例成功、模块编译成功、`git diff --check`零错误；不以不存在依赖/数据库/fixture而失败作为RED。
- Failure returns to: Step 2 的编译/Schema前置；若需改变外部语义、架构或DDL版本则回主Spec。
- Completion criteria: 这组认证事实的Repository/Mapper能按同realm/tenant/active/version读写；本Step的所有Manual Checks在实际执行时给出证据，未通过不能提交。
- Rollback: 提交前仅恢复本Step的精确路径；DDL已对真实数据运行后只forward-fix和新版本，不能编辑既有SQL/历史或恢复旧安全世代。
- Commit paths: `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/domain/enums/OAuthClientPurposeEnum.java`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/domain/enums/RealmTypeEnum.java`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/domain/bo/TrustedRealmBO.java`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/domain/po/OAuthRealmPO.java`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/domain/bo/OAuthRealmBO.java`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/repository/OAuthRealmRepository.java`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/domain/po/OAuthClientPO.java`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/domain/bo/OAuthClientBO.java`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/repository/OAuthClientRepository.java`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/repository/OAuthRealmPersistenceContractTest.java`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/domain/converter/OAuthRealmConverter.java`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/dao/OAuthRealmMapper.java`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/resources/mapper/oauth/OAuthRealmMapper.xml`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/repository/impl/OAuthRealmRepositoryImpl.java`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/domain/converter/OAuthClientConverter.java`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/dao/OAuthClientMapper.java`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/resources/mapper/oauth/OAuthClientMapper.xml`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/repository/impl/OAuthClientRepositoryImpl.java`。
- Commit: `feat(shoubing): persist oauthrealm/oauthclient state`。

### Step 4 — 持久化资源目录与最小授权

- Requirements: REQ-005, REQ-008, REQ-016, REQ-017
- Dependencies: Step 3
- Baseline state: 当前HEAD的对应源码/测试如每File的Repository evidence，先前Step已完成并有其focused GREEN；本Step尚不存在OAuthResourcePO所列新行为。
- Observable outcome: 这组认证事实的Repository/Mapper能按同realm/tenant/active/version读写。
- End state: 这组认证事实的Repository/Mapper能按同realm/tenant/active/version读写；后续Step的Consumer尚未切流，不声明完整生产验收。
- Test-first gate: Required — 必需PO/BO/Repository签名先让test编译；测试在Mapper/Impl装配前因缺Bean或缺具名SQL按目标RED，随后GREEN。
- Manual Checks: MC-ARCH-001, MC-REUSE-001, MC-NAME-001, MC-VALID-001, MC-MODEL-001, MC-CONVERT-001, MC-LOG-001, MC-BEAN-001, MC-JSON-001, MC-TIME-001, MC-SCOPE-001, MC-TEST-001
- Literal Rules: Rule 1, Rule 2, Rule 3, Rule 4, Rule 6, Rule 9, Rule 10, Rule 11
- Ordered files:

#### File 1 — `CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/resource/domain/po/OAuthResourcePO.java`

- Purpose: EgonModel承载本表完整列。
- Symbols: `OAuthResourcePO`。
- Repository evidence: 当前不存在；Spec §8/§11 明确目标，邻近包可验证；目标源于主Spec §7–§16。
- Dependencies and consumers: 输入依赖 Step 3 与本Step此前文件；输出供 这组认证事实的Repository/Mapper能按同realm/tenant/active/version读写及后一Step使用；Java Bean/Mapper按模块现有配置发现，前端按现有auth client消费。
- Why now: 本Step的编译前置/RED契约先锁定； EgonModel承载本表完整列在此位置后才可接消费者。
- Contract/signature changes: OAuthResourcePO；EgonModel承载本表完整列。公开行为只取主Spec §9对应API或§10/11内部合同。
- Input/output and state mapping: 11.2 oauth_resource native columns snake_case→camelCase; inherited id/tenant/audit/deletedAt/version only一次。
- Error and edge behavior: UNIQUE/NULL/tenant违反时映射typed conflict；不可向HTTP返回PO。
- Standards impact: MC-ARCH-001, MC-SCOPE-001, MC-TEST-001, MC-VALID-001；按已选传统分层，输入校验与权限先于状态读取，PO不出边界，相关日志脱敏。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 6, Rule 10, Rule 11；此文件直接按所列原始规则落实命名、验证、类型/注入或配置，其他规则由同Step关联文件负责。
- Implementation pseudocode:

```java
INPUT -> 11.2 oauth_resource native columns snake_case→camelCase; inherited id/tenant/audit/deletedAt/version only一次。
FLOW -> @TableName("oauth_resource") class OAuthResourcePO extends EgonModel<OAuthResourcePO>; §11 oauth_resource business fields; @Data/@NoArgsConstructor/@AllArgsConstructor/@Accessors(chain=true)/@SuperBuilder/@EqualsAndHashCode(callSuper=true); @EnumValue/secret exclude
ON INVALID/CONCURRENT/DEPENDENCY FAILURE -> UNIQUE/NULL/tenant违反时映射typed conflict；不可向HTTP返回PO。
RESULT -> only the named symbol/contract in this File block becomes available; preserve tenant, expectedVersion, scope, UTC time and secret masking stated above.
```

- Verification contribution: 本Step `mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthResourcePersistenceContractTest -Dsurefire.failIfNoSpecifiedTests=false test`；此文件负责 OAuthResourcePO 的正例与 UNIQUE/NULL/tenant违反时映射typed conflict；不可向HTTP返回PO 负例。
- After this file: OAuthResourcePO 的字段/入口或注册已落盘；与下一文件组合后本Step才能GREEN，不能把中间状态当完成。

#### File 2 — `CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/resource/domain/bo/OAuthResourceBO.java`

- Purpose: Service计算与PO分界。
- Symbols: `OAuthResourceBO`。
- Repository evidence: 当前不存在；Spec §8/§11 明确目标，邻近包可验证；目标源于主Spec §7–§16。
- Dependencies and consumers: 输入依赖 Step 3 与本Step此前文件；输出供 这组认证事实的Repository/Mapper能按同realm/tenant/active/version读写及后一Step使用；Java Bean/Mapper按模块现有配置发现，前端按现有auth client消费。
- Why now: 前一文件建立本文件需要的字段或调用契约； Service计算与PO分界在此位置后才可接消费者。
- Contract/signature changes: OAuthResourceBO；Service计算与PO分界。公开行为只取主Spec §9对应API或§10/11内部合同。
- Input/output and state mapping: outbound business values only; managementClientId maps platform registration; legacy external ID stable; tenant from TrustedRealmBO。
- Error and edge behavior: missing/invalid enum, scope or date rejected before Mapper。
- Standards impact: MC-ARCH-001, MC-SCOPE-001, MC-TEST-001, MC-VALID-001；按已选传统分层，输入校验与权限先于状态读取，PO不出边界，相关日志脱敏。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 6, Rule 10, Rule 11；此文件直接按所列原始规则落实命名、验证、类型/注入或配置，其他规则由同Step关联文件负责。
- Implementation pseudocode:

```java
INPUT -> outbound business values only; managementClientId maps platform registration; legacy external ID stable; tenant from TrustedRealmBO。
FLOW -> class OAuthResourceBO { business fields from OAuthResourcePO; id,version; } @Data @NoArgsConstructor @AllArgsConstructor @Accessors(chain=true) @Builder; @Valid groups Create/Update/Query
ON INVALID/CONCURRENT/DEPENDENCY FAILURE -> missing/invalid enum, scope or date rejected before Mapper。
RESULT -> only the named symbol/contract in this File block becomes available; preserve tenant, expectedVersion, scope, UTC time and secret masking stated above.
```

- Verification contribution: 本Step `mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthResourcePersistenceContractTest -Dsurefire.failIfNoSpecifiedTests=false test`；此文件负责 OAuthResourceBO 的正例与 missing/invalid enum, scope or date rejected before Mapper 负例。
- After this file: OAuthResourceBO 的字段/入口或注册已落盘；与下一文件组合后本Step才能GREEN，不能把中间状态当完成。

#### File 3 — `CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/resource/repository/OAuthResourceRepository.java`

- Purpose: Service-facing typed contract。
- Symbols: `OAuthResourceRepository`。
- Repository evidence: 当前不存在；Spec §8/§11 明确目标，邻近包可验证；目标源于主Spec §7–§16。
- Dependencies and consumers: 输入依赖 Step 3 与本Step此前文件；输出供 这组认证事实的Repository/Mapper能按同realm/tenant/active/version读写及后一Step使用；Java Bean/Mapper按模块现有配置发现，前端按现有auth client消费。
- Why now: 前一文件建立本文件需要的字段或调用契约； Service-facing typed contract在此位置后才可接消费者。
- Contract/signature changes: OAuthResourceRepository；Service-facing typed contract。公开行为只取主Spec §9对应API或§10/11内部合同。
- Input/output and state mapping: Service uses BO/Query/Command; no PO generic outward。
- Error and edge behavior: 0 affected row → typed conflict; no bypass tenant。
- Standards impact: MC-ARCH-001, MC-SCOPE-001, MC-TEST-001, MC-VALID-001；按已选传统分层，输入校验与权限先于状态读取，PO不出边界，相关日志脱敏。
- Literal rule enforcement: Rule 1, Rule 2, Rule 11；此文件直接按所列原始规则落实命名、验证、类型/注入或配置，其他规则由同Step关联文件负责。
- Implementation pseudocode:

```java
INPUT -> Service uses BO/Query/Command; no PO generic outward。
FLOW -> @Validated interface OAuthResourceRepository { Optional<OAuthResourceBO> find(TrustedRealmBO realm, ...); OAuthResourceBO save(@Valid OAuthResourceBO input,long expectedVersion); } names agree §11 access platform owner metadata + resourceUri/business source
ON INVALID/CONCURRENT/DEPENDENCY FAILURE -> 0 affected row → typed conflict; no bypass tenant。
RESULT -> only the named symbol/contract in this File block becomes available; preserve tenant, expectedVersion, scope, UTC time and secret masking stated above.
```

- Verification contribution: 本Step `mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthResourcePersistenceContractTest -Dsurefire.failIfNoSpecifiedTests=false test`；此文件负责 OAuthResourceRepository 的正例与 0 affected row → typed conflict; no bypass tenant 负例。
- After this file: OAuthResourceRepository 的字段/入口或注册已落盘；与下一文件组合后本Step才能GREEN，不能把中间状态当完成。

#### File 4 — `CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/resource/domain/po/OAuthGrantPO.java`

- Purpose: EgonModel承载本表完整列。
- Symbols: `OAuthGrantPO`。
- Repository evidence: 当前不存在；Spec §8/§11 明确目标，邻近包可验证；目标源于主Spec §7–§16。
- Dependencies and consumers: 输入依赖 Step 3 与本Step此前文件；输出供 这组认证事实的Repository/Mapper能按同realm/tenant/active/version读写及后一Step使用；Java Bean/Mapper按模块现有配置发现，前端按现有auth client消费。
- Why now: 前一文件建立本文件需要的字段或调用契约； EgonModel承载本表完整列在此位置后才可接消费者。
- Contract/signature changes: OAuthGrantPO；EgonModel承载本表完整列。公开行为只取主Spec §9对应API或§10/11内部合同。
- Input/output and state mapping: 11.2 oauth_grant native columns snake_case→camelCase; inherited id/tenant/audit/deletedAt/version only一次。
- Error and edge behavior: UNIQUE/NULL/tenant违反时映射typed conflict；不可向HTTP返回PO。
- Standards impact: MC-ARCH-001, MC-SCOPE-001, MC-TEST-001, MC-VALID-001；按已选传统分层，输入校验与权限先于状态读取，PO不出边界，相关日志脱敏。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 6, Rule 10, Rule 11；此文件直接按所列原始规则落实命名、验证、类型/注入或配置，其他规则由同Step关联文件负责。
- Implementation pseudocode:

```java
INPUT -> 11.2 oauth_grant native columns snake_case→camelCase; inherited id/tenant/audit/deletedAt/version only一次。
FLOW -> @TableName("oauth_grant") class OAuthGrantPO extends EgonModel<OAuthGrantPO>; §11 oauth_grant business fields; @Data/@NoArgsConstructor/@AllArgsConstructor/@Accessors(chain=true)/@SuperBuilder/@EqualsAndHashCode(callSuper=true); @EnumValue/secret exclude
ON INVALID/CONCURRENT/DEPENDENCY FAILURE -> UNIQUE/NULL/tenant违反时映射typed conflict；不可向HTTP返回PO。
RESULT -> only the named symbol/contract in this File block becomes available; preserve tenant, expectedVersion, scope, UTC time and secret masking stated above.
```

- Verification contribution: 本Step `mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthResourcePersistenceContractTest -Dsurefire.failIfNoSpecifiedTests=false test`；此文件负责 OAuthGrantPO 的正例与 UNIQUE/NULL/tenant违反时映射typed conflict；不可向HTTP返回PO 负例。
- After this file: OAuthGrantPO 的字段/入口或注册已落盘；与下一文件组合后本Step才能GREEN，不能把中间状态当完成。

#### File 5 — `CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/resource/domain/bo/OAuthGrantBO.java`

- Purpose: Service计算与PO分界。
- Symbols: `OAuthGrantBO`。
- Repository evidence: 当前不存在；Spec §8/§11 明确目标，邻近包可验证；目标源于主Spec §7–§16。
- Dependencies and consumers: 输入依赖 Step 3 与本Step此前文件；输出供 这组认证事实的Repository/Mapper能按同realm/tenant/active/version读写及后一Step使用；Java Bean/Mapper按模块现有配置发现，前端按现有auth client消费。
- Why now: 前一文件建立本文件需要的字段或调用契约； Service计算与PO分界在此位置后才可接消费者。
- Contract/signature changes: OAuthGrantBO；Service计算与PO分界。公开行为只取主Spec §9对应API或§10/11内部合同。
- Input/output and state mapping: outbound business values only; grant scope intersection and resourceVersion; tenant from TrustedRealmBO。
- Error and edge behavior: missing/invalid enum, scope or date rejected before Mapper。
- Standards impact: MC-ARCH-001, MC-SCOPE-001, MC-TEST-001, MC-VALID-001；按已选传统分层，输入校验与权限先于状态读取，PO不出边界，相关日志脱敏。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 6, Rule 10, Rule 11；此文件直接按所列原始规则落实命名、验证、类型/注入或配置，其他规则由同Step关联文件负责。
- Implementation pseudocode:

```java
INPUT -> outbound business values only; grant scope intersection and resourceVersion; tenant from TrustedRealmBO。
FLOW -> class OAuthGrantBO { business fields from OAuthGrantPO; id,version; } @Data @NoArgsConstructor @AllArgsConstructor @Accessors(chain=true) @Builder; @Valid groups Create/Update/Query
ON INVALID/CONCURRENT/DEPENDENCY FAILURE -> missing/invalid enum, scope or date rejected before Mapper。
RESULT -> only the named symbol/contract in this File block becomes available; preserve tenant, expectedVersion, scope, UTC time and secret masking stated above.
```

- Verification contribution: 本Step `mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthResourcePersistenceContractTest -Dsurefire.failIfNoSpecifiedTests=false test`；此文件负责 OAuthGrantBO 的正例与 missing/invalid enum, scope or date rejected before Mapper 负例。
- After this file: OAuthGrantBO 的字段/入口或注册已落盘；与下一文件组合后本Step才能GREEN，不能把中间状态当完成。

#### File 6 — `CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/resource/repository/OAuthGrantRepository.java`

- Purpose: Service-facing typed contract。
- Symbols: `OAuthGrantRepository`。
- Repository evidence: 当前不存在；Spec §8/§11 明确目标，邻近包可验证；目标源于主Spec §7–§16。
- Dependencies and consumers: 输入依赖 Step 3 与本Step此前文件；输出供 这组认证事实的Repository/Mapper能按同realm/tenant/active/version读写及后一Step使用；Java Bean/Mapper按模块现有配置发现，前端按现有auth client消费。
- Why now: 前一文件建立本文件需要的字段或调用契约； Service-facing typed contract在此位置后才可接消费者。
- Contract/signature changes: OAuthGrantRepository；Service-facing typed contract。公开行为只取主Spec §9对应API或§10/11内部合同。
- Input/output and state mapping: Service uses BO/Query/Command; no PO generic outward。
- Error and edge behavior: 0 affected row → typed conflict; no bypass tenant。
- Standards impact: MC-ARCH-001, MC-SCOPE-001, MC-TEST-001, MC-VALID-001；按已选传统分层，输入校验与权限先于状态读取，PO不出边界，相关日志脱敏。
- Literal rule enforcement: Rule 1, Rule 2, Rule 11；此文件直接按所列原始规则落实命名、验证、类型/注入或配置，其他规则由同Step关联文件负责。
- Implementation pseudocode:

```java
INPUT -> Service uses BO/Query/Command; no PO generic outward。
FLOW -> @Validated interface OAuthGrantRepository { Optional<OAuthGrantBO> find(TrustedRealmBO realm, ...); OAuthGrantBO save(@Valid OAuthGrantBO input,long expectedVersion); } names agree §11 access tenant+clientPk+resourceServerId+grantType active
ON INVALID/CONCURRENT/DEPENDENCY FAILURE -> 0 affected row → typed conflict; no bypass tenant。
RESULT -> only the named symbol/contract in this File block becomes available; preserve tenant, expectedVersion, scope, UTC time and secret masking stated above.
```

- Verification contribution: 本Step `mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthResourcePersistenceContractTest -Dsurefire.failIfNoSpecifiedTests=false test`；此文件负责 OAuthGrantRepository 的正例与 0 affected row → typed conflict; no bypass tenant 负例。
- After this file: OAuthGrantRepository 的字段/入口或注册已落盘；与下一文件组合后本Step才能GREEN，不能把中间状态当完成。

#### File 7 — `CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/repository/OAuthResourcePersistenceContractTest.java`

- Purpose: RED证明这组映射/查询/跨租户错误。
- Symbols: `OAuthResourcePersistenceContractTest`。
- Repository evidence: 当前不存在；Spec §8/§11 明确目标，邻近包可验证；目标源于主Spec §7–§16。
- Dependencies and consumers: 输入依赖 Step 3 与本Step此前文件；输出供 这组认证事实的Repository/Mapper能按同realm/tenant/active/version读写及后一Step使用；Java Bean/Mapper按模块现有配置发现，前端按现有auth client消费。
- Why now: 前一文件建立本文件需要的字段或调用契约； RED证明这组映射/查询/跨租户错误在此位置后才可接消费者。
- Contract/signature changes: OAuthResourcePersistenceContractTest；RED证明这组映射/查询/跨租户错误。公开行为只取主Spec §9对应API或§10/11内部合同。
- Input/output and state mapping: fixed tenant=1001/1002; timestamps Instant/UTC; SQL actual node tianquan_oauth。
- Error and edge behavior: no implementation Bean/Mapper yet → focused contract RED，不能因fixture fail。
- Standards impact: MC-ARCH-001, MC-SCOPE-001, MC-TEST-001, MC-VALID-001；按已选传统分层，输入校验与权限先于状态读取，PO不出边界，相关日志脱敏。
- Literal rule enforcement: Rule 2, Rule 3, Rule 11；此文件直接按所列原始规则落实命名、验证、类型/注入或配置，其他规则由同Step关联文件负责。
- Implementation pseudocode:

```java
INPUT -> fixed tenant=1001/1002; timestamps Instant/UTC; SQL actual node tianquan_oauth。
FLOW -> @Test for OAuthResource/OAuthGrant: assert same external client/resource key in different trusted tenants does not cross; query includes tenant and deleted_at IS NULL; concurrent expectedVersion loser fails; secret/ciphertext absent from VO
ON INVALID/CONCURRENT/DEPENDENCY FAILURE -> no implementation Bean/Mapper yet → focused contract RED，不能因fixture fail。
RESULT -> only the named symbol/contract in this File block becomes available; preserve tenant, expectedVersion, scope, UTC time and secret masking stated above.
```

- Verification contribution: 本Step `mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthResourcePersistenceContractTest -Dsurefire.failIfNoSpecifiedTests=false test`；此文件负责 OAuthResourcePersistenceContractTest 的正例与 no implementation Bean/Mapper yet → focused contract RED，不能因fixture fail 负例。
- After this file: OAuthResourcePersistenceContractTest 的字段/入口或注册已落盘；与下一文件组合后本Step才能GREEN，不能把中间状态当完成。

#### File 8 — `CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/resource/domain/converter/OAuthResourceConverter.java`

- Purpose: PO↔BO双向MapStruct转换。
- Symbols: `OAuthResourceConverter`。
- Repository evidence: 当前不存在；Spec §8/§11 明确目标，邻近包可验证；目标源于主Spec §7–§16。
- Dependencies and consumers: 输入依赖 Step 3 与本Step此前文件；输出供 这组认证事实的Repository/Mapper能按同realm/tenant/active/version读写及后一Step使用；Java Bean/Mapper按模块现有配置发现，前端按现有auth client消费。
- Why now: 前一文件建立本文件需要的字段或调用契约； PO↔BO双向MapStruct转换在此位置后才可接消费者。
- Contract/signature changes: OAuthResourceConverter；PO↔BO双向MapStruct转换。公开行为只取主Spec §9对应API或§10/11内部合同。
- Input/output and state mapping: OAuthResourcePO business fields from §11 → OAuthResourceBO; unknown enum拒绝，null按列约束。
- Error and edge behavior: generated implementation/constructor must compile; no BeanUtils/json copy。
- Standards impact: MC-ARCH-001, MC-SCOPE-001, MC-TEST-001, MC-VALID-001；按已选传统分层，输入校验与权限先于状态读取，PO不出边界，相关日志脱敏。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 6, Rule 10, Rule 11；此文件直接按所列原始规则落实命名、验证、类型/注入或配置，其他规则由同Step关联文件负责。
- Implementation pseudocode:

```java
INPUT -> OAuthResourcePO business fields from §11 → OAuthResourceBO; unknown enum拒绝，null按列约束。
FLOW -> @Mapper(componentModel="spring") interface OAuthResourceConverter extends BaseConverter<OAuthResourceBO,OAuthResourcePO>; @Mapping inherited tenant/audit fill ignored on insert; enum code explicit; secret excluded from toString/VO; mapping date Instant/UTC
ON INVALID/CONCURRENT/DEPENDENCY FAILURE -> generated implementation/constructor must compile; no BeanUtils/json copy。
RESULT -> only the named symbol/contract in this File block becomes available; preserve tenant, expectedVersion, scope, UTC time and secret masking stated above.
```

- Verification contribution: 本Step `mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthResourcePersistenceContractTest -Dsurefire.failIfNoSpecifiedTests=false test`；此文件负责 OAuthResourceConverter 的正例与 generated implementation/constructor must compile; no BeanUtils/json copy 负例。
- After this file: OAuthResourceConverter 的字段/入口或注册已落盘；与下一文件组合后本Step才能GREEN，不能把中间状态当完成。

#### File 9 — `CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/resource/dao/OAuthResourceMapper.java`

- Purpose: MP Mapper具名SQL接口。
- Symbols: `OAuthResourceMapper`。
- Repository evidence: 当前不存在；Spec §8/§11 明确目标，邻近包可验证；目标源于主Spec §7–§16。
- Dependencies and consumers: 输入依赖 Step 3 与本Step此前文件；输出供 这组认证事实的Repository/Mapper能按同realm/tenant/active/version读写及后一Step使用；Java Bean/Mapper按模块现有配置发现，前端按现有auth client消费。
- Why now: 前一文件建立本文件需要的字段或调用契约； MP Mapper具名SQL接口在此位置后才可接消费者。
- Contract/signature changes: OAuthResourceMapper；MP Mapper具名SQL接口。公开行为只取主Spec §9对应API或§10/11内部合同。
- Input/output and state mapping: tenantId positive Long from verified MDC; result OAuthResourcePO。
- Error and edge behavior: no LambdaQueryChain/Wrapper; missing tenant failclosed。
- Standards impact: MC-ARCH-001, MC-SCOPE-001, MC-TEST-001, MC-VALID-001；按已选传统分层，输入校验与权限先于状态读取，PO不出边界，相关日志脱敏。
- Literal rule enforcement: Rule 1, Rule 2, Rule 11；此文件直接按所列原始规则落实命名、验证、类型/注入或配置，其他规则由同Step关联文件负责。
- Implementation pseudocode:

```java
INPUT -> tenantId positive Long from verified MDC; result OAuthResourcePO。
FLOW -> interface OAuthResourceMapper extends EgonColaMapper<OAuthResourcePO>; methods selectActiveById/Ids, deleteVersionedById, platform owner metadata + resourceUri/business source SQL bindings
ON INVALID/CONCURRENT/DEPENDENCY FAILURE -> no LambdaQueryChain/Wrapper; missing tenant failclosed。
RESULT -> only the named symbol/contract in this File block becomes available; preserve tenant, expectedVersion, scope, UTC time and secret masking stated above.
```

- Verification contribution: 本Step `mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthResourcePersistenceContractTest -Dsurefire.failIfNoSpecifiedTests=false test`；此文件负责 OAuthResourceMapper 的正例与 no LambdaQueryChain/Wrapper; missing tenant failclosed 负例。
- After this file: OAuthResourceMapper 的字段/入口或注册已落盘；与下一文件组合后本Step才能GREEN，不能把中间状态当完成。

#### File 10 — `CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/resources/mapper/resource/OAuthResourceMapper.xml`

- Purpose: 具名PostgreSQL谓词/锁。
- Symbols: `OAuthResourceMapper SQL`。
- Repository evidence: 当前不存在；Spec §8/§11 明确目标，邻近包可验证；目标源于主Spec §7–§16。
- Dependencies and consumers: 输入依赖 Step 3 与本Step此前文件；输出供 这组认证事实的Repository/Mapper能按同realm/tenant/active/version读写及后一Step使用；Java Bean/Mapper按模块现有配置发现，前端按现有auth client消费。
- Why now: 前一文件建立本文件需要的字段或调用契约； 具名PostgreSQL谓词/锁在此位置后才可接消费者。
- Contract/signature changes: OAuthResourceMapper SQL；具名PostgreSQL谓词/锁。公开行为只取主Spec §9对应API或§10/11内部合同。
- Input/output and state mapping: 0/1 precise query or capped list; indexes §11。
- Error and edge behavior: tenant range forbidden, 0 rows conflict, no cross-group write。
- Standards impact: MC-SCOPE-001, MC-TEST-001；按已选传统分层，输入校验与权限先于状态读取，PO不出边界，相关日志脱敏。
- Literal rule enforcement: Rule 2, Rule 10, Rule 11；此文件直接按所列原始规则落实命名、验证、类型/注入或配置，其他规则由同Step关联文件负责。
- Implementation pseudocode:

```xml
INPUT -> 0/1 precise query or capped list; indexes §11。
FLOW -> SELECT business columns + inherited fields FROM tianquan_oauth.oauth_resource WHERE tenant_id=#{tenantId} AND deleted_at IS NULL AND platform owner metadata + resourceUri/business source; guarded UPDATE id+tenant+version+expected state; SELECT FOR UPDATE only command path; max page bounded
ON INVALID/CONCURRENT/DEPENDENCY FAILURE -> tenant range forbidden, 0 rows conflict, no cross-group write。
RESULT -> only the named symbol/contract in this File block becomes available; preserve tenant, expectedVersion, scope, UTC time and secret masking stated above.
```

- Verification contribution: 本Step `mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthResourcePersistenceContractTest -Dsurefire.failIfNoSpecifiedTests=false test`；此文件负责 OAuthResourceMapper SQL 的正例与 tenant range forbidden, 0 rows conflict, no cross-group write 负例。
- After this file: OAuthResourceMapper SQL 的字段/入口或注册已落盘；与下一文件组合后本Step才能GREEN，不能把中间状态当完成。

#### File 11 — `CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/resource/repository/impl/OAuthResourceRepositoryImpl.java`

- Purpose: MP repository ACL。
- Symbols: `OAuthResourceRepositoryImpl`。
- Repository evidence: 当前不存在；Spec §8/§11 明确目标，邻近包可验证；目标源于主Spec §7–§16。
- Dependencies and consumers: 输入依赖 Step 3 与本Step此前文件；输出供 这组认证事实的Repository/Mapper能按同realm/tenant/active/version读写及后一Step使用；Java Bean/Mapper按模块现有配置发现，前端按现有auth client消费。
- Why now: 前一文件建立本文件需要的字段或调用契约； MP repository ACL在此位置后才可接消费者。
- Contract/signature changes: OAuthResourceRepositoryImpl；MP repository ACL。公开行为只取主Spec §9对应API或§10/11内部合同。
- Input/output and state mapping: OAuthResourceBO↔PO; raw secret/private only protected internal fields。
- Error and edge behavior: UnsupportedOperationException for generic unscoped query; errors mapped without sensitive values。
- Standards impact: MC-ARCH-001, MC-SCOPE-001, MC-TEST-001, MC-VALID-001；按已选传统分层，输入校验与权限先于状态读取，PO不出边界，相关日志脱敏。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 6, Rule 9, Rule 10, Rule 11；此文件直接按所列原始规则落实命名、验证、类型/注入或配置，其他规则由同Step关联文件负责。
- Implementation pseudocode:

```java
INPUT -> OAuthResourceBO↔PO; raw secret/private only protected internal fields。
FLOW -> @Repository("oAuthResourceRepository") @Slf4j @RequiredArgsConstructor; final @Qualifier("oAuthResourceMapper") Mapper, converter, modelValidationUtils, properties; extends EgonColaRepository<Mapper,PO>; find maps PO→BO; save maps BO→PO, guarded version update result==1
ON INVALID/CONCURRENT/DEPENDENCY FAILURE -> UnsupportedOperationException for generic unscoped query; errors mapped without sensitive values。
RESULT -> only the named symbol/contract in this File block becomes available; preserve tenant, expectedVersion, scope, UTC time and secret masking stated above.
```

- Verification contribution: 本Step `mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthResourcePersistenceContractTest -Dsurefire.failIfNoSpecifiedTests=false test`；此文件负责 OAuthResourceRepositoryImpl 的正例与 UnsupportedOperationException for generic unscoped query; errors mapped without sensitive values 负例。
- After this file: OAuthResourceRepositoryImpl 的字段/入口或注册已落盘；与下一文件组合后本Step才能GREEN，不能把中间状态当完成。

#### File 12 — `CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/resource/domain/converter/OAuthGrantConverter.java`

- Purpose: PO↔BO双向MapStruct转换。
- Symbols: `OAuthGrantConverter`。
- Repository evidence: 当前不存在；Spec §8/§11 明确目标，邻近包可验证；目标源于主Spec §7–§16。
- Dependencies and consumers: 输入依赖 Step 3 与本Step此前文件；输出供 这组认证事实的Repository/Mapper能按同realm/tenant/active/version读写及后一Step使用；Java Bean/Mapper按模块现有配置发现，前端按现有auth client消费。
- Why now: 前一文件建立本文件需要的字段或调用契约； PO↔BO双向MapStruct转换在此位置后才可接消费者。
- Contract/signature changes: OAuthGrantConverter；PO↔BO双向MapStruct转换。公开行为只取主Spec §9对应API或§10/11内部合同。
- Input/output and state mapping: OAuthGrantPO business fields from §11 → OAuthGrantBO; unknown enum拒绝，null按列约束。
- Error and edge behavior: generated implementation/constructor must compile; no BeanUtils/json copy。
- Standards impact: MC-ARCH-001, MC-SCOPE-001, MC-TEST-001, MC-VALID-001；按已选传统分层，输入校验与权限先于状态读取，PO不出边界，相关日志脱敏。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 6, Rule 10, Rule 11；此文件直接按所列原始规则落实命名、验证、类型/注入或配置，其他规则由同Step关联文件负责。
- Implementation pseudocode:

```java
INPUT -> OAuthGrantPO business fields from §11 → OAuthGrantBO; unknown enum拒绝，null按列约束。
FLOW -> @Mapper(componentModel="spring") interface OAuthGrantConverter extends BaseConverter<OAuthGrantBO,OAuthGrantPO>; @Mapping inherited tenant/audit fill ignored on insert; enum code explicit; secret excluded from toString/VO; mapping date Instant/UTC
ON INVALID/CONCURRENT/DEPENDENCY FAILURE -> generated implementation/constructor must compile; no BeanUtils/json copy。
RESULT -> only the named symbol/contract in this File block becomes available; preserve tenant, expectedVersion, scope, UTC time and secret masking stated above.
```

- Verification contribution: 本Step `mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthResourcePersistenceContractTest -Dsurefire.failIfNoSpecifiedTests=false test`；此文件负责 OAuthGrantConverter 的正例与 generated implementation/constructor must compile; no BeanUtils/json copy 负例。
- After this file: OAuthGrantConverter 的字段/入口或注册已落盘；与下一文件组合后本Step才能GREEN，不能把中间状态当完成。

#### File 13 — `CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/resource/dao/OAuthGrantMapper.java`

- Purpose: MP Mapper具名SQL接口。
- Symbols: `OAuthGrantMapper`。
- Repository evidence: 当前不存在；Spec §8/§11 明确目标，邻近包可验证；目标源于主Spec §7–§16。
- Dependencies and consumers: 输入依赖 Step 3 与本Step此前文件；输出供 这组认证事实的Repository/Mapper能按同realm/tenant/active/version读写及后一Step使用；Java Bean/Mapper按模块现有配置发现，前端按现有auth client消费。
- Why now: 前一文件建立本文件需要的字段或调用契约； MP Mapper具名SQL接口在此位置后才可接消费者。
- Contract/signature changes: OAuthGrantMapper；MP Mapper具名SQL接口。公开行为只取主Spec §9对应API或§10/11内部合同。
- Input/output and state mapping: tenantId positive Long from verified MDC; result OAuthGrantPO。
- Error and edge behavior: no LambdaQueryChain/Wrapper; missing tenant failclosed。
- Standards impact: MC-ARCH-001, MC-SCOPE-001, MC-TEST-001, MC-VALID-001；按已选传统分层，输入校验与权限先于状态读取，PO不出边界，相关日志脱敏。
- Literal rule enforcement: Rule 1, Rule 2, Rule 11；此文件直接按所列原始规则落实命名、验证、类型/注入或配置，其他规则由同Step关联文件负责。
- Implementation pseudocode:

```java
INPUT -> tenantId positive Long from verified MDC; result OAuthGrantPO。
FLOW -> interface OAuthGrantMapper extends EgonColaMapper<OAuthGrantPO>; methods selectActiveById/Ids, deleteVersionedById, tenant+clientPk+resourceServerId+grantType active SQL bindings
ON INVALID/CONCURRENT/DEPENDENCY FAILURE -> no LambdaQueryChain/Wrapper; missing tenant failclosed。
RESULT -> only the named symbol/contract in this File block becomes available; preserve tenant, expectedVersion, scope, UTC time and secret masking stated above.
```

- Verification contribution: 本Step `mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthResourcePersistenceContractTest -Dsurefire.failIfNoSpecifiedTests=false test`；此文件负责 OAuthGrantMapper 的正例与 no LambdaQueryChain/Wrapper; missing tenant failclosed 负例。
- After this file: OAuthGrantMapper 的字段/入口或注册已落盘；与下一文件组合后本Step才能GREEN，不能把中间状态当完成。

#### File 14 — `CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/resources/mapper/resource/OAuthGrantMapper.xml`

- Purpose: 具名PostgreSQL谓词/锁。
- Symbols: `OAuthGrantMapper SQL`。
- Repository evidence: 当前不存在；Spec §8/§11 明确目标，邻近包可验证；目标源于主Spec §7–§16。
- Dependencies and consumers: 输入依赖 Step 3 与本Step此前文件；输出供 这组认证事实的Repository/Mapper能按同realm/tenant/active/version读写及后一Step使用；Java Bean/Mapper按模块现有配置发现，前端按现有auth client消费。
- Why now: 前一文件建立本文件需要的字段或调用契约； 具名PostgreSQL谓词/锁在此位置后才可接消费者。
- Contract/signature changes: OAuthGrantMapper SQL；具名PostgreSQL谓词/锁。公开行为只取主Spec §9对应API或§10/11内部合同。
- Input/output and state mapping: 0/1 precise query or capped list; indexes §11。
- Error and edge behavior: tenant range forbidden, 0 rows conflict, no cross-group write。
- Standards impact: MC-SCOPE-001, MC-TEST-001；按已选传统分层，输入校验与权限先于状态读取，PO不出边界，相关日志脱敏。
- Literal rule enforcement: Rule 2, Rule 10, Rule 11；此文件直接按所列原始规则落实命名、验证、类型/注入或配置，其他规则由同Step关联文件负责。
- Implementation pseudocode:

```xml
INPUT -> 0/1 precise query or capped list; indexes §11。
FLOW -> SELECT business columns + inherited fields FROM tianquan_oauth.oauth_grant WHERE tenant_id=#{tenantId} AND deleted_at IS NULL AND tenant+clientPk+resourceServerId+grantType active; guarded UPDATE id+tenant+version+expected state; SELECT FOR UPDATE only command path; max page bounded
ON INVALID/CONCURRENT/DEPENDENCY FAILURE -> tenant range forbidden, 0 rows conflict, no cross-group write。
RESULT -> only the named symbol/contract in this File block becomes available; preserve tenant, expectedVersion, scope, UTC time and secret masking stated above.
```

- Verification contribution: 本Step `mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthResourcePersistenceContractTest -Dsurefire.failIfNoSpecifiedTests=false test`；此文件负责 OAuthGrantMapper SQL 的正例与 tenant range forbidden, 0 rows conflict, no cross-group write 负例。
- After this file: OAuthGrantMapper SQL 的字段/入口或注册已落盘；与下一文件组合后本Step才能GREEN，不能把中间状态当完成。

#### File 15 — `CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/resource/repository/impl/OAuthGrantRepositoryImpl.java`

- Purpose: MP repository ACL。
- Symbols: `OAuthGrantRepositoryImpl`。
- Repository evidence: 当前不存在；Spec §8/§11 明确目标，邻近包可验证；目标源于主Spec §7–§16。
- Dependencies and consumers: 输入依赖 Step 3 与本Step此前文件；输出供 这组认证事实的Repository/Mapper能按同realm/tenant/active/version读写及后一Step使用；Java Bean/Mapper按模块现有配置发现，前端按现有auth client消费。
- Why now: 前一文件建立本文件需要的字段或调用契约； MP repository ACL在此位置后才可接消费者。
- Contract/signature changes: OAuthGrantRepositoryImpl；MP repository ACL。公开行为只取主Spec §9对应API或§10/11内部合同。
- Input/output and state mapping: OAuthGrantBO↔PO; raw secret/private only protected internal fields。
- Error and edge behavior: UnsupportedOperationException for generic unscoped query; errors mapped without sensitive values。
- Standards impact: MC-ARCH-001, MC-SCOPE-001, MC-TEST-001, MC-VALID-001；按已选传统分层，输入校验与权限先于状态读取，PO不出边界，相关日志脱敏。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 6, Rule 9, Rule 10, Rule 11；此文件直接按所列原始规则落实命名、验证、类型/注入或配置，其他规则由同Step关联文件负责。
- Implementation pseudocode:

```java
INPUT -> OAuthGrantBO↔PO; raw secret/private only protected internal fields。
FLOW -> @Repository("oAuthGrantRepository") @Slf4j @RequiredArgsConstructor; final @Qualifier("oAuthGrantMapper") Mapper, converter, modelValidationUtils, properties; extends EgonColaRepository<Mapper,PO>; find maps PO→BO; save maps BO→PO, guarded version update result==1
ON INVALID/CONCURRENT/DEPENDENCY FAILURE -> UnsupportedOperationException for generic unscoped query; errors mapped without sensitive values。
RESULT -> only the named symbol/contract in this File block becomes available; preserve tenant, expectedVersion, scope, UTC time and secret masking stated above.
```

- Verification contribution: 本Step `mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthResourcePersistenceContractTest -Dsurefire.failIfNoSpecifiedTests=false test`；此文件负责 OAuthGrantRepositoryImpl 的正例与 UnsupportedOperationException for generic unscoped query; errors mapped without sensitive values 负例。
- After this file: OAuthGrantRepositoryImpl 的字段/入口或注册已落盘；与下一文件组合后本Step才能GREEN，不能把中间状态当完成。

- Validation working directory: `/Users/mario/SelfProject/Egon-COLA`。
- Verification command: `mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthResourcePersistenceContractTest -Dsurefire.failIfNoSpecifiedTests=false test`。
- Expected result: 先确认上述RED由缺失目标行为引起，GREEN时命名用例成功、模块编译成功、`git diff --check`零错误；不以不存在依赖/数据库/fixture而失败作为RED。
- Failure returns to: Step 3 的编译/Schema前置；若需改变外部语义、架构或DDL版本则回主Spec。
- Completion criteria: 这组认证事实的Repository/Mapper能按同realm/tenant/active/version读写；本Step的所有Manual Checks在实际执行时给出证据，未通过不能提交。
- Rollback: 提交前仅恢复本Step的精确路径；DDL已对真实数据运行后只forward-fix和新版本，不能编辑既有SQL/历史或恢复旧安全世代。
- Commit paths: `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/resource/domain/po/OAuthResourcePO.java`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/resource/domain/bo/OAuthResourceBO.java`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/resource/repository/OAuthResourceRepository.java`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/resource/domain/po/OAuthGrantPO.java`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/resource/domain/bo/OAuthGrantBO.java`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/resource/repository/OAuthGrantRepository.java`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/repository/OAuthResourcePersistenceContractTest.java`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/resource/domain/converter/OAuthResourceConverter.java`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/resource/dao/OAuthResourceMapper.java`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/resources/mapper/resource/OAuthResourceMapper.xml`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/resource/repository/impl/OAuthResourceRepositoryImpl.java`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/resource/domain/converter/OAuthGrantConverter.java`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/resource/dao/OAuthGrantMapper.java`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/resources/mapper/resource/OAuthGrantMapper.xml`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/resource/repository/impl/OAuthGrantRepositoryImpl.java`。
- Commit: `feat(shoubing): persist oauthresource/oauthgrant state`。

### Step 5 — 持久化授权码、令牌和同意

- Requirements: REQ-003, REQ-004, REQ-012, REQ-017
- Dependencies: Step 4
- Baseline state: 当前HEAD的对应源码/测试如每File的Repository evidence，先前Step已完成并有其focused GREEN；本Step尚不存在OAuthAuthorizationPO所列新行为。
- Observable outcome: 这组认证事实的Repository/Mapper能按同realm/tenant/active/version读写。
- End state: 这组认证事实的Repository/Mapper能按同realm/tenant/active/version读写；后续Step的Consumer尚未切流，不声明完整生产验收。
- Test-first gate: Required — 必需PO/BO/Repository签名先让test编译；测试在Mapper/Impl装配前因缺Bean或缺具名SQL按目标RED，随后GREEN。
- Manual Checks: MC-ARCH-001, MC-REUSE-001, MC-NAME-001, MC-VALID-001, MC-MODEL-001, MC-CONVERT-001, MC-LOG-001, MC-BEAN-001, MC-JSON-001, MC-TIME-001, MC-SCOPE-001, MC-TEST-001
- Literal Rules: Rule 1, Rule 2, Rule 3, Rule 4, Rule 6, Rule 9, Rule 10, Rule 11
- Ordered files:

#### File 1 — `CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/domain/bo/OAuthAuthorizationAttributesBO.java`

- Purpose: 关闭 `OAuthAuthorizationAttributesBO` 在主Spec中的字段/消费依赖。
- Symbols: `OAuthAuthorizationAttributesBO`。
- Repository evidence: 当前不存在；主Spec §10.1/§11 命名此对象/字段；现有相邻业务包提供放置证据。
- Dependencies and consumers: 本Step既有File块调用该类型；数据来源/消费者为主Spec §10.1/§11，不产生新HTTP路由。
- Why now: 该类型按本Step的测试/编译依赖顺序落位，以免由实现者临时发明。
- Contract/signature changes: schemaVersion=1, nonce/challenge/method/redirectUri/stateDigest/securitySnapshot/acr/authTime; encrypt at repository boundary, no password。
- Input/output and state mapping: 精确字段由主Spec §10.1/§11 指定，realm从签名/注册得出，Instant为UTC，版本单调，秘密字段仅服务器内部。
- Error and edge behavior: null/未知字段/跨tenant/重复版本拒绝；不转成宽松缺省或吞异常。
- Standards impact: MC-ARCH-001, MC-NAME-001, MC-VALID-001, MC-MODEL-001, MC-CONVERT-001, MC-BEAN-001, MC-JSON-001, MC-TIME-001, MC-SCOPE-001, MC-TEST-001；行为Bean使用具名final Qualifier、@RequiredArgsConstructor和@Slf4j，载体遵循四注解与Builder。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 6, Rule 9, Rule 10, Rule 11；class按POJO语义命名，边界注解验证，跨层转换只用既有BaseConverter，Jackson和java.time字段明确。
- Implementation pseudocode:

```java
INPUT -> 仅接受主Spec §10.1/§11 的可信issuer/realm/输入字段；调用前 @Valid 或 ValueObject不变量检查。
FLOW -> schemaVersion=1, nonce/challenge/method/redirectUri/stateDigest/securitySnapshot/acr/authTime; encrypt at repository boundary, no password。
OUTPUT -> OAuthAuthorizationAttributesBO 的Java方法、DTO字段或配置可被本Step既有调用者编译使用；不新增业务API。
FAIL -> 空值/跨tenant/未知schema/重复版本/外部依赖错误按本Step已列错误模型fail-closed，不能输出原secret/token。
```

- Verification contribution: Step 5 的focus测试对 `OAuthAuthorizationAttributesBO` 的字段、装配、负例及不泄密断言。
- After this file: `OAuthAuthorizationAttributesBO` 成为已声明的Step依赖，下一文件可按原合同引用；未完成整条行为前不宣称GREEN。

#### File 2 — `CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/domain/bo/OAuthSecuritySnapshotBO.java`

- Purpose: 关闭 `OAuthSecuritySnapshotBO` 在主Spec中的字段/消费依赖。
- Symbols: `OAuthSecuritySnapshotBO`。
- Repository evidence: 当前不存在；主Spec §10.1/§11 命名此对象/字段；现有相邻业务包提供放置证据。
- Dependencies and consumers: 本Step既有File块调用该类型；数据来源/消费者为主Spec §10.1/§11，不产生新HTTP路由。
- Why now: 该类型按本Step的测试/编译依赖顺序落位，以免由实现者临时发明。
- Contract/signature changes: realm/client/subject security generations, readyEpoch, revision; nonnegative, USER subject required。
- Input/output and state mapping: 精确字段由主Spec §10.1/§11 指定，realm从签名/注册得出，Instant为UTC，版本单调，秘密字段仅服务器内部。
- Error and edge behavior: null/未知字段/跨tenant/重复版本拒绝；不转成宽松缺省或吞异常。
- Standards impact: MC-ARCH-001, MC-NAME-001, MC-VALID-001, MC-MODEL-001, MC-CONVERT-001, MC-BEAN-001, MC-JSON-001, MC-TIME-001, MC-SCOPE-001, MC-TEST-001；行为Bean使用具名final Qualifier、@RequiredArgsConstructor和@Slf4j，载体遵循四注解与Builder。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 6, Rule 9, Rule 10, Rule 11；class按POJO语义命名，边界注解验证，跨层转换只用既有BaseConverter，Jackson和java.time字段明确。
- Implementation pseudocode:

```java
INPUT -> 仅接受主Spec §10.1/§11 的可信issuer/realm/输入字段；调用前 @Valid 或 ValueObject不变量检查。
FLOW -> realm/client/subject security generations, readyEpoch, revision; nonnegative, USER subject required。
OUTPUT -> OAuthSecuritySnapshotBO 的Java方法、DTO字段或配置可被本Step既有调用者编译使用；不新增业务API。
FAIL -> 空值/跨tenant/未知schema/重复版本/外部依赖错误按本Step已列错误模型fail-closed，不能输出原secret/token。
```

- Verification contribution: Step 5 的focus测试对 `OAuthSecuritySnapshotBO` 的字段、装配、负例及不泄密断言。
- After this file: `OAuthSecuritySnapshotBO` 成为已声明的Step依赖，下一文件可按原合同引用；未完成整条行为前不宣称GREEN。

#### File 3 — `CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/domain/bo/OAuthTokenMetadataBO.java`

- Purpose: 关闭 `OAuthTokenMetadataBO` 在主Spec中的字段/消费依赖。
- Symbols: `OAuthTokenMetadataBO`。
- Repository evidence: 当前不存在；主Spec §10.1/§11 命名此对象/字段；现有相邻业务包提供放置证据。
- Dependencies and consumers: 本Step既有File块调用该类型；数据来源/消费者为主Spec §10.1/§11，不产生新HTTP路由。
- Why now: 该类型按本Step的测试/编译依赖顺序落位，以免由实现者临时发明。
- Contract/signature changes: schemaVersion=1 claims/invalidated/codeConsumed; encrypted JSON; reject unknown schema。
- Input/output and state mapping: 精确字段由主Spec §10.1/§11 指定，realm从签名/注册得出，Instant为UTC，版本单调，秘密字段仅服务器内部。
- Error and edge behavior: null/未知字段/跨tenant/重复版本拒绝；不转成宽松缺省或吞异常。
- Standards impact: MC-ARCH-001, MC-NAME-001, MC-VALID-001, MC-MODEL-001, MC-CONVERT-001, MC-BEAN-001, MC-JSON-001, MC-TIME-001, MC-SCOPE-001, MC-TEST-001；行为Bean使用具名final Qualifier、@RequiredArgsConstructor和@Slf4j，载体遵循四注解与Builder。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 6, Rule 9, Rule 10, Rule 11；class按POJO语义命名，边界注解验证，跨层转换只用既有BaseConverter，Jackson和java.time字段明确。
- Implementation pseudocode:

```java
INPUT -> 仅接受主Spec §10.1/§11 的可信issuer/realm/输入字段；调用前 @Valid 或 ValueObject不变量检查。
FLOW -> schemaVersion=1 claims/invalidated/codeConsumed; encrypted JSON; reject unknown schema。
OUTPUT -> OAuthTokenMetadataBO 的Java方法、DTO字段或配置可被本Step既有调用者编译使用；不新增业务API。
FAIL -> 空值/跨tenant/未知schema/重复版本/外部依赖错误按本Step已列错误模型fail-closed，不能输出原secret/token。
```

- Verification contribution: Step 5 的focus测试对 `OAuthTokenMetadataBO` 的字段、装配、负例及不泄密断言。
- After this file: `OAuthTokenMetadataBO` 成为已声明的Step依赖，下一文件可按原合同引用；未完成整条行为前不宣称GREEN。

#### File 4 — `CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/domain/bo/OAuthSecurityVersionBO.java`

- Purpose: 关闭 `OAuthSecurityVersionBO` 在主Spec中的字段/消费依赖。
- Symbols: `OAuthSecurityVersionBO`。
- Repository evidence: 当前不存在；主Spec §10.1/§11 命名此对象/字段；现有相邻业务包提供放置证据。
- Dependencies and consumers: 本Step既有File块调用该类型；数据来源/消费者为主Spec §10.1/§11，不产生新HTTP路由。
- Why now: 该类型按本Step的测试/编译依赖顺序落位，以免由实现者临时发明。
- Contract/signature changes: realm/client/subject security_version; SERVICE subject null, USER subject nonnegative。
- Input/output and state mapping: 精确字段由主Spec §10.1/§11 指定，realm从签名/注册得出，Instant为UTC，版本单调，秘密字段仅服务器内部。
- Error and edge behavior: null/未知字段/跨tenant/重复版本拒绝；不转成宽松缺省或吞异常。
- Standards impact: MC-ARCH-001, MC-NAME-001, MC-VALID-001, MC-MODEL-001, MC-CONVERT-001, MC-BEAN-001, MC-JSON-001, MC-TIME-001, MC-SCOPE-001, MC-TEST-001；行为Bean使用具名final Qualifier、@RequiredArgsConstructor和@Slf4j，载体遵循四注解与Builder。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 6, Rule 9, Rule 10, Rule 11；class按POJO语义命名，边界注解验证，跨层转换只用既有BaseConverter，Jackson和java.time字段明确。
- Implementation pseudocode:

```java
INPUT -> 仅接受主Spec §10.1/§11 的可信issuer/realm/输入字段；调用前 @Valid 或 ValueObject不变量检查。
FLOW -> realm/client/subject security_version; SERVICE subject null, USER subject nonnegative。
OUTPUT -> OAuthSecurityVersionBO 的Java方法、DTO字段或配置可被本Step既有调用者编译使用；不新增业务API。
FAIL -> 空值/跨tenant/未知schema/重复版本/外部依赖错误按本Step已列错误模型fail-closed，不能输出原secret/token。
```

- Verification contribution: Step 5 的focus测试对 `OAuthSecurityVersionBO` 的字段、装配、负例及不泄密断言。
- After this file: `OAuthSecurityVersionBO` 成为已声明的Step依赖，下一文件可按原合同引用；未完成整条行为前不宣称GREEN。

#### File 5 — `CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/domain/bo/SignedTokenClaimsBO.java`

- Purpose: 关闭 `SignedTokenClaimsBO` 在主Spec中的字段/消费依赖。
- Symbols: `SignedTokenClaimsBO`。
- Repository evidence: 当前不存在；主Spec §10.1/§11 命名此对象/字段；现有相邻业务包提供放置证据。
- Dependencies and consumers: 本Step既有File块调用该类型；数据来源/消费者为主Spec §10.1/§11，不产生新HTTP路由。
- Why now: 该类型按本Step的测试/编译依赖顺序落位，以免由实现者临时发明。
- Contract/signature changes: iss/aud/sub/scope/tenant_id/principal_type/jti/iat/nbf/exp/client_id/authorization_id/client_registration_id/security_version; RS256 at+jwt。
- Input/output and state mapping: 精确字段由主Spec §10.1/§11 指定，realm从签名/注册得出，Instant为UTC，版本单调，秘密字段仅服务器内部。
- Error and edge behavior: null/未知字段/跨tenant/重复版本拒绝；不转成宽松缺省或吞异常。
- Standards impact: MC-ARCH-001, MC-NAME-001, MC-VALID-001, MC-MODEL-001, MC-CONVERT-001, MC-BEAN-001, MC-JSON-001, MC-TIME-001, MC-SCOPE-001, MC-TEST-001；行为Bean使用具名final Qualifier、@RequiredArgsConstructor和@Slf4j，载体遵循四注解与Builder。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 6, Rule 9, Rule 10, Rule 11；class按POJO语义命名，边界注解验证，跨层转换只用既有BaseConverter，Jackson和java.time字段明确。
- Implementation pseudocode:

```java
INPUT -> 仅接受主Spec §10.1/§11 的可信issuer/realm/输入字段；调用前 @Valid 或 ValueObject不变量检查。
FLOW -> iss/aud/sub/scope/tenant_id/principal_type/jti/iat/nbf/exp/client_id/authorization_id/client_registration_id/security_version; RS256 at+jwt。
OUTPUT -> SignedTokenClaimsBO 的Java方法、DTO字段或配置可被本Step既有调用者编译使用；不新增业务API。
FAIL -> 空值/跨tenant/未知schema/重复版本/外部依赖错误按本Step已列错误模型fail-closed，不能输出原secret/token。
```

- Verification contribution: Step 5 的focus测试对 `SignedTokenClaimsBO` 的字段、装配、负例及不泄密断言。
- After this file: `SignedTokenClaimsBO` 成为已声明的Step依赖，下一文件可按原合同引用；未完成整条行为前不宣称GREEN。

#### File 6 — `CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/domain/enums/OAuthTokenTypeEnum.java`

- Purpose: 关闭 `OAuthTokenTypeEnum` 在主Spec中的字段/消费依赖。
- Symbols: `OAuthTokenTypeEnum`。
- Repository evidence: 当前不存在；主Spec §10.1/§11 命名此对象/字段；现有相邻业务包提供放置证据。
- Dependencies and consumers: 本Step既有File块调用该类型；数据来源/消费者为主Spec §10.1/§11，不产生新HTTP路由。
- Why now: 该类型按本Step的测试/编译依赖顺序落位，以免由实现者临时发明。
- Contract/signature changes: CODE/ACCESS/REFRESH/ID stable DB code; never ordinal。
- Input/output and state mapping: 精确字段由主Spec §10.1/§11 指定，realm从签名/注册得出，Instant为UTC，版本单调，秘密字段仅服务器内部。
- Error and edge behavior: null/未知字段/跨tenant/重复版本拒绝；不转成宽松缺省或吞异常。
- Standards impact: MC-ARCH-001, MC-NAME-001, MC-VALID-001, MC-MODEL-001, MC-CONVERT-001, MC-BEAN-001, MC-JSON-001, MC-TIME-001, MC-SCOPE-001, MC-TEST-001；行为Bean使用具名final Qualifier、@RequiredArgsConstructor和@Slf4j，载体遵循四注解与Builder。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 6, Rule 9, Rule 10, Rule 11；class按POJO语义命名，边界注解验证，跨层转换只用既有BaseConverter，Jackson和java.time字段明确。
- Implementation pseudocode:

```java
INPUT -> 仅接受主Spec §10.1/§11 的可信issuer/realm/输入字段；调用前 @Valid 或 ValueObject不变量检查。
FLOW -> CODE/ACCESS/REFRESH/ID stable DB code; never ordinal。
OUTPUT -> OAuthTokenTypeEnum 的Java方法、DTO字段或配置可被本Step既有调用者编译使用；不新增业务API。
FAIL -> 空值/跨tenant/未知schema/重复版本/外部依赖错误按本Step已列错误模型fail-closed，不能输出原secret/token。
```

- Verification contribution: Step 5 的focus测试对 `OAuthTokenTypeEnum` 的字段、装配、负例及不泄密断言。
- After this file: `OAuthTokenTypeEnum` 成为已声明的Step依赖，下一文件可按原合同引用；未完成整条行为前不宣称GREEN。

#### File 7 — `CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/domain/po/OAuthAuthorizationPO.java`

- Purpose: EgonModel承载本表完整列。
- Symbols: `OAuthAuthorizationPO`。
- Repository evidence: 当前不存在；Spec §8/§11 明确目标，邻近包可验证；目标源于主Spec §7–§16。
- Dependencies and consumers: 输入依赖 Step 4 与本Step此前文件；输出供 这组认证事实的Repository/Mapper能按同realm/tenant/active/version读写及后一Step使用；Java Bean/Mapper按模块现有配置发现，前端按现有auth client消费。
- Why now: 本Step的编译前置/RED契约先锁定； EgonModel承载本表完整列在此位置后才可接消费者。
- Contract/signature changes: OAuthAuthorizationPO；EgonModel承载本表完整列。公开行为只取主Spec §9对应API或§10/11内部合同。
- Input/output and state mapping: 11.2 oauth_authorization native columns snake_case→camelCase; inherited id/tenant/audit/deletedAt/version only一次。
- Error and edge behavior: UNIQUE/NULL/tenant违反时映射typed conflict；不可向HTTP返回PO。
- Standards impact: MC-ARCH-001, MC-SCOPE-001, MC-TEST-001, MC-VALID-001；按已选传统分层，输入校验与权限先于状态读取，PO不出边界，相关日志脱敏。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 6, Rule 10, Rule 11；此文件直接按所列原始规则落实命名、验证、类型/注入或配置，其他规则由同Step关联文件负责。
- Implementation pseudocode:

```java
INPUT -> 11.2 oauth_authorization native columns snake_case→camelCase; inherited id/tenant/audit/deletedAt/version only一次。
FLOW -> @TableName("oauth_authorization") class OAuthAuthorizationPO extends EgonModel<OAuthAuthorizationPO>; §11 oauth_authorization business fields; @Data/@NoArgsConstructor/@AllArgsConstructor/@Accessors(chain=true)/@SuperBuilder/@EqualsAndHashCode(callSuper=true); @EnumValue/secret exclude
ON INVALID/CONCURRENT/DEPENDENCY FAILURE -> UNIQUE/NULL/tenant违反时映射typed conflict；不可向HTTP返回PO。
RESULT -> only the named symbol/contract in this File block becomes available; preserve tenant, expectedVersion, scope, UTC time and secret masking stated above.
```

- Verification contribution: 本Step `mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthAuthorizationPersistenceContractTest -Dsurefire.failIfNoSpecifiedTests=false test`；此文件负责 OAuthAuthorizationPO 的正例与 UNIQUE/NULL/tenant违反时映射typed conflict；不可向HTTP返回PO 负例。
- After this file: OAuthAuthorizationPO 的字段/入口或注册已落盘；与下一文件组合后本Step才能GREEN，不能把中间状态当完成。

#### File 8 — `CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/domain/bo/OAuthAuthorizationBO.java`

- Purpose: Service计算与PO分界。
- Symbols: `OAuthAuthorizationBO`。
- Repository evidence: 当前不存在；Spec §8/§11 明确目标，邻近包可验证；目标源于主Spec §7–§16。
- Dependencies and consumers: 输入依赖 Step 4 与本Step此前文件；输出供 这组认证事实的Repository/Mapper能按同realm/tenant/active/version读写及后一Step使用；Java Bean/Mapper按模块现有配置发现，前端按现有auth client消费。
- Why now: 前一文件建立本文件需要的字段或调用契约； Service计算与PO分界在此位置后才可接消费者。
- Contract/signature changes: OAuthAuthorizationBO；Service计算与PO分界。公开行为只取主Spec §9对应API或§10/11内部合同。
- Input/output and state mapping: outbound business values only; family lock, service/user principal; tenant from TrustedRealmBO。
- Error and edge behavior: missing/invalid enum, scope or date rejected before Mapper。
- Standards impact: MC-ARCH-001, MC-SCOPE-001, MC-TEST-001, MC-VALID-001；按已选传统分层，输入校验与权限先于状态读取，PO不出边界，相关日志脱敏。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 6, Rule 10, Rule 11；此文件直接按所列原始规则落实命名、验证、类型/注入或配置，其他规则由同Step关联文件负责。
- Implementation pseudocode:

```java
INPUT -> outbound business values only; family lock, service/user principal; tenant from TrustedRealmBO。
FLOW -> class OAuthAuthorizationBO { business fields from OAuthAuthorizationPO; id,version; } @Data @NoArgsConstructor @AllArgsConstructor @Accessors(chain=true) @Builder; @Valid groups Create/Update/Query
ON INVALID/CONCURRENT/DEPENDENCY FAILURE -> missing/invalid enum, scope or date rejected before Mapper。
RESULT -> only the named symbol/contract in this File block becomes available; preserve tenant, expectedVersion, scope, UTC time and secret masking stated above.
```

- Verification contribution: 本Step `mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthAuthorizationPersistenceContractTest -Dsurefire.failIfNoSpecifiedTests=false test`；此文件负责 OAuthAuthorizationBO 的正例与 missing/invalid enum, scope or date rejected before Mapper 负例。
- After this file: OAuthAuthorizationBO 的字段/入口或注册已落盘；与下一文件组合后本Step才能GREEN，不能把中间状态当完成。

#### File 9 — `CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/repository/OAuthAuthorizationRepository.java`

- Purpose: Service-facing typed contract。
- Symbols: `OAuthAuthorizationRepository`。
- Repository evidence: 当前不存在；Spec §8/§11 明确目标，邻近包可验证；目标源于主Spec §7–§16。
- Dependencies and consumers: 输入依赖 Step 4 与本Step此前文件；输出供 这组认证事实的Repository/Mapper能按同realm/tenant/active/version读写及后一Step使用；Java Bean/Mapper按模块现有配置发现，前端按现有auth client消费。
- Why now: 前一文件建立本文件需要的字段或调用契约； Service-facing typed contract在此位置后才可接消费者。
- Contract/signature changes: OAuthAuthorizationRepository；Service-facing typed contract。公开行为只取主Spec §9对应API或§10/11内部合同。
- Input/output and state mapping: Service uses BO/Query/Command; no PO generic outward。
- Error and edge behavior: 0 affected row → typed conflict; no bypass tenant。
- Standards impact: MC-ARCH-001, MC-SCOPE-001, MC-TEST-001, MC-VALID-001；按已选传统分层，输入校验与权限先于状态读取，PO不出边界，相关日志脱敏。
- Literal rule enforcement: Rule 1, Rule 2, Rule 11；此文件直接按所列原始规则落实命名、验证、类型/注入或配置，其他规则由同Step关联文件负责。
- Implementation pseudocode:

```java
INPUT -> Service uses BO/Query/Command; no PO generic outward。
FLOW -> @Validated interface OAuthAuthorizationRepository { Optional<OAuthAuthorizationBO> find(TrustedRealmBO realm, ...); OAuthAuthorizationBO save(@Valid OAuthAuthorizationBO input,long expectedVersion); } names agree §11 access tenant+clientPk+principal/status/absoluteExpiresAt
ON INVALID/CONCURRENT/DEPENDENCY FAILURE -> 0 affected row → typed conflict; no bypass tenant。
RESULT -> only the named symbol/contract in this File block becomes available; preserve tenant, expectedVersion, scope, UTC time and secret masking stated above.
```

- Verification contribution: 本Step `mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthAuthorizationPersistenceContractTest -Dsurefire.failIfNoSpecifiedTests=false test`；此文件负责 OAuthAuthorizationRepository 的正例与 0 affected row → typed conflict; no bypass tenant 负例。
- After this file: OAuthAuthorizationRepository 的字段/入口或注册已落盘；与下一文件组合后本Step才能GREEN，不能把中间状态当完成。

#### File 10 — `CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/domain/po/OAuthTokenPO.java`

- Purpose: EgonModel承载本表完整列。
- Symbols: `OAuthTokenPO`。
- Repository evidence: 当前不存在；Spec §8/§11 明确目标，邻近包可验证；目标源于主Spec §7–§16。
- Dependencies and consumers: 输入依赖 Step 4 与本Step此前文件；输出供 这组认证事实的Repository/Mapper能按同realm/tenant/active/version读写及后一Step使用；Java Bean/Mapper按模块现有配置发现，前端按现有auth client消费。
- Why now: 前一文件建立本文件需要的字段或调用契约； EgonModel承载本表完整列在此位置后才可接消费者。
- Contract/signature changes: OAuthTokenPO；EgonModel承载本表完整列。公开行为只取主Spec §9对应API或§10/11内部合同。
- Input/output and state mapping: 11.2 oauth_token native columns snake_case→camelCase; inherited id/tenant/audit/deletedAt/version only一次。
- Error and edge behavior: UNIQUE/NULL/tenant违反时映射typed conflict；不可向HTTP返回PO。
- Standards impact: MC-ARCH-001, MC-SCOPE-001, MC-TEST-001, MC-VALID-001；按已选传统分层，输入校验与权限先于状态读取，PO不出边界，相关日志脱敏。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 6, Rule 10, Rule 11；此文件直接按所列原始规则落实命名、验证、类型/注入或配置，其他规则由同Step关联文件负责。
- Implementation pseudocode:

```java
INPUT -> 11.2 oauth_token native columns snake_case→camelCase; inherited id/tenant/audit/deletedAt/version only一次。
FLOW -> @TableName("oauth_token") class OAuthTokenPO extends EgonModel<OAuthTokenPO>; §11 oauth_token business fields; @Data/@NoArgsConstructor/@AllArgsConstructor/@Accessors(chain=true)/@SuperBuilder/@EqualsAndHashCode(callSuper=true); @EnumValue/secret exclude
ON INVALID/CONCURRENT/DEPENDENCY FAILURE -> UNIQUE/NULL/tenant违反时映射typed conflict；不可向HTTP返回PO。
RESULT -> only the named symbol/contract in this File block becomes available; preserve tenant, expectedVersion, scope, UTC time and secret masking stated above.
```

- Verification contribution: 本Step `mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthAuthorizationPersistenceContractTest -Dsurefire.failIfNoSpecifiedTests=false test`；此文件负责 OAuthTokenPO 的正例与 UNIQUE/NULL/tenant违反时映射typed conflict；不可向HTTP返回PO 负例。
- After this file: OAuthTokenPO 的字段/入口或注册已落盘；与下一文件组合后本Step才能GREEN，不能把中间状态当完成。

#### File 11 — `CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/domain/bo/OAuthTokenBO.java`

- Purpose: Service计算与PO分界。
- Symbols: `OAuthTokenBO`。
- Repository evidence: 当前不存在；Spec §8/§11 明确目标，邻近包可验证；目标源于主Spec §7–§16。
- Dependencies and consumers: 输入依赖 Step 4 与本Step此前文件；输出供 这组认证事实的Repository/Mapper能按同realm/tenant/active/version读写及后一Step使用；Java Bean/Mapper按模块现有配置发现，前端按现有auth client消费。
- Why now: 前一文件建立本文件需要的字段或调用契约； Service计算与PO分界在此位置后才可接消费者。
- Contract/signature changes: OAuthTokenBO；Service计算与PO分界。公开行为只取主Spec §9对应API或§10/11内部合同。
- Input/output and state mapping: outbound business values only; CODE/RT consumed once, ACCESS jti; tenant from TrustedRealmBO。
- Error and edge behavior: missing/invalid enum, scope or date rejected before Mapper。
- Standards impact: MC-ARCH-001, MC-SCOPE-001, MC-TEST-001, MC-VALID-001；按已选传统分层，输入校验与权限先于状态读取，PO不出边界，相关日志脱敏。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 6, Rule 10, Rule 11；此文件直接按所列原始规则落实命名、验证、类型/注入或配置，其他规则由同Step关联文件负责。
- Implementation pseudocode:

```java
INPUT -> outbound business values only; CODE/RT consumed once, ACCESS jti; tenant from TrustedRealmBO。
FLOW -> class OAuthTokenBO { business fields from OAuthTokenPO; id,version; } @Data @NoArgsConstructor @AllArgsConstructor @Accessors(chain=true) @Builder; @Valid groups Create/Update/Query
ON INVALID/CONCURRENT/DEPENDENCY FAILURE -> missing/invalid enum, scope or date rejected before Mapper。
RESULT -> only the named symbol/contract in this File block becomes available; preserve tenant, expectedVersion, scope, UTC time and secret masking stated above.
```

- Verification contribution: 本Step `mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthAuthorizationPersistenceContractTest -Dsurefire.failIfNoSpecifiedTests=false test`；此文件负责 OAuthTokenBO 的正例与 missing/invalid enum, scope or date rejected before Mapper 负例。
- After this file: OAuthTokenBO 的字段/入口或注册已落盘；与下一文件组合后本Step才能GREEN，不能把中间状态当完成。

#### File 12 — `CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/repository/OAuthTokenRepository.java`

- Purpose: Service-facing typed contract。
- Symbols: `OAuthTokenRepository`。
- Repository evidence: 当前不存在；Spec §8/§11 明确目标，邻近包可验证；目标源于主Spec §7–§16。
- Dependencies and consumers: 输入依赖 Step 4 与本Step此前文件；输出供 这组认证事实的Repository/Mapper能按同realm/tenant/active/version读写及后一Step使用；Java Bean/Mapper按模块现有配置发现，前端按现有auth client消费。
- Why now: 前一文件建立本文件需要的字段或调用契约； Service-facing typed contract在此位置后才可接消费者。
- Contract/signature changes: OAuthTokenRepository；Service-facing typed contract。公开行为只取主Spec §9对应API或§10/11内部合同。
- Input/output and state mapping: Service uses BO/Query/Command; no PO generic outward。
- Error and edge behavior: 0 affected row → typed conflict; no bypass tenant。
- Standards impact: MC-ARCH-001, MC-SCOPE-001, MC-TEST-001, MC-VALID-001；按已选传统分层，输入校验与权限先于状态读取，PO不出边界，相关日志脱敏。
- Literal rule enforcement: Rule 1, Rule 2, Rule 11；此文件直接按所列原始规则落实命名、验证、类型/注入或配置，其他规则由同Step关联文件负责。
- Implementation pseudocode:

```java
INPUT -> Service uses BO/Query/Command; no PO generic outward。
FLOW -> @Validated interface OAuthTokenRepository { Optional<OAuthTokenBO> find(TrustedRealmBO realm, ...); OAuthTokenBO save(@Valid OAuthTokenBO input,long expectedVersion); } names agree §11 access tenant+tokenDigest or authorizationPk+tokenType
ON INVALID/CONCURRENT/DEPENDENCY FAILURE -> 0 affected row → typed conflict; no bypass tenant。
RESULT -> only the named symbol/contract in this File block becomes available; preserve tenant, expectedVersion, scope, UTC time and secret masking stated above.
```

- Verification contribution: 本Step `mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthAuthorizationPersistenceContractTest -Dsurefire.failIfNoSpecifiedTests=false test`；此文件负责 OAuthTokenRepository 的正例与 0 affected row → typed conflict; no bypass tenant 负例。
- After this file: OAuthTokenRepository 的字段/入口或注册已落盘；与下一文件组合后本Step才能GREEN，不能把中间状态当完成。

#### File 13 — `CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/domain/po/OAuthConsentPO.java`

- Purpose: EgonModel承载本表完整列。
- Symbols: `OAuthConsentPO`。
- Repository evidence: 当前不存在；Spec §8/§11 明确目标，邻近包可验证；目标源于主Spec §7–§16。
- Dependencies and consumers: 输入依赖 Step 4 与本Step此前文件；输出供 这组认证事实的Repository/Mapper能按同realm/tenant/active/version读写及后一Step使用；Java Bean/Mapper按模块现有配置发现，前端按现有auth client消费。
- Why now: 前一文件建立本文件需要的字段或调用契约； EgonModel承载本表完整列在此位置后才可接消费者。
- Contract/signature changes: OAuthConsentPO；EgonModel承载本表完整列。公开行为只取主Spec §9对应API或§10/11内部合同。
- Input/output and state mapping: 11.2 oauth_consent native columns snake_case→camelCase; inherited id/tenant/audit/deletedAt/version only一次。
- Error and edge behavior: UNIQUE/NULL/tenant违反时映射typed conflict；不可向HTTP返回PO。
- Standards impact: MC-ARCH-001, MC-SCOPE-001, MC-TEST-001, MC-VALID-001；按已选传统分层，输入校验与权限先于状态读取，PO不出边界，相关日志脱敏。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 6, Rule 10, Rule 11；此文件直接按所列原始规则落实命名、验证、类型/注入或配置，其他规则由同Step关联文件负责。
- Implementation pseudocode:

```java
INPUT -> 11.2 oauth_consent native columns snake_case→camelCase; inherited id/tenant/audit/deletedAt/version only一次。
FLOW -> @TableName("oauth_consent") class OAuthConsentPO extends EgonModel<OAuthConsentPO>; §11 oauth_consent business fields; @Data/@NoArgsConstructor/@AllArgsConstructor/@Accessors(chain=true)/@SuperBuilder/@EqualsAndHashCode(callSuper=true); @EnumValue/secret exclude
ON INVALID/CONCURRENT/DEPENDENCY FAILURE -> UNIQUE/NULL/tenant违反时映射typed conflict；不可向HTTP返回PO。
RESULT -> only the named symbol/contract in this File block becomes available; preserve tenant, expectedVersion, scope, UTC time and secret masking stated above.
```

- Verification contribution: 本Step `mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthAuthorizationPersistenceContractTest -Dsurefire.failIfNoSpecifiedTests=false test`；此文件负责 OAuthConsentPO 的正例与 UNIQUE/NULL/tenant违反时映射typed conflict；不可向HTTP返回PO 负例。
- After this file: OAuthConsentPO 的字段/入口或注册已落盘；与下一文件组合后本Step才能GREEN，不能把中间状态当完成。

#### File 14 — `CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/domain/bo/OAuthConsentBO.java`

- Purpose: Service计算与PO分界。
- Symbols: `OAuthConsentBO`。
- Repository evidence: 当前不存在；Spec §8/§11 明确目标，邻近包可验证；目标源于主Spec §7–§16。
- Dependencies and consumers: 输入依赖 Step 4 与本Step此前文件；输出供 这组认证事实的Repository/Mapper能按同realm/tenant/active/version读写及后一Step使用；Java Bean/Mapper按模块现有配置发现，前端按现有auth client消费。
- Why now: 前一文件建立本文件需要的字段或调用契约； Service计算与PO分界在此位置后才可接消费者。
- Contract/signature changes: OAuthConsentBO；Service计算与PO分界。公开行为只取主Spec §9对应API或§10/11内部合同。
- Input/output and state mapping: outbound business values only; new consent after approval only; tenant from TrustedRealmBO。
- Error and edge behavior: missing/invalid enum, scope or date rejected before Mapper。
- Standards impact: MC-ARCH-001, MC-SCOPE-001, MC-TEST-001, MC-VALID-001；按已选传统分层，输入校验与权限先于状态读取，PO不出边界，相关日志脱敏。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 6, Rule 10, Rule 11；此文件直接按所列原始规则落实命名、验证、类型/注入或配置，其他规则由同Step关联文件负责。
- Implementation pseudocode:

```java
INPUT -> outbound business values only; new consent after approval only; tenant from TrustedRealmBO。
FLOW -> class OAuthConsentBO { business fields from OAuthConsentPO; id,version; } @Data @NoArgsConstructor @AllArgsConstructor @Accessors(chain=true) @Builder; @Valid groups Create/Update/Query
ON INVALID/CONCURRENT/DEPENDENCY FAILURE -> missing/invalid enum, scope or date rejected before Mapper。
RESULT -> only the named symbol/contract in this File block becomes available; preserve tenant, expectedVersion, scope, UTC time and secret masking stated above.
```

- Verification contribution: 本Step `mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthAuthorizationPersistenceContractTest -Dsurefire.failIfNoSpecifiedTests=false test`；此文件负责 OAuthConsentBO 的正例与 missing/invalid enum, scope or date rejected before Mapper 负例。
- After this file: OAuthConsentBO 的字段/入口或注册已落盘；与下一文件组合后本Step才能GREEN，不能把中间状态当完成。

#### File 15 — `CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/repository/OAuthConsentRepository.java`

- Purpose: Service-facing typed contract。
- Symbols: `OAuthConsentRepository`。
- Repository evidence: 当前不存在；Spec §8/§11 明确目标，邻近包可验证；目标源于主Spec §7–§16。
- Dependencies and consumers: 输入依赖 Step 4 与本Step此前文件；输出供 这组认证事实的Repository/Mapper能按同realm/tenant/active/version读写及后一Step使用；Java Bean/Mapper按模块现有配置发现，前端按现有auth client消费。
- Why now: 前一文件建立本文件需要的字段或调用契约； Service-facing typed contract在此位置后才可接消费者。
- Contract/signature changes: OAuthConsentRepository；Service-facing typed contract。公开行为只取主Spec §9对应API或§10/11内部合同。
- Input/output and state mapping: Service uses BO/Query/Command; no PO generic outward。
- Error and edge behavior: 0 affected row → typed conflict; no bypass tenant。
- Standards impact: MC-ARCH-001, MC-SCOPE-001, MC-TEST-001, MC-VALID-001；按已选传统分层，输入校验与权限先于状态读取，PO不出边界，相关日志脱敏。
- Literal rule enforcement: Rule 1, Rule 2, Rule 11；此文件直接按所列原始规则落实命名、验证、类型/注入或配置，其他规则由同Step关联文件负责。
- Implementation pseudocode:

```java
INPUT -> Service uses BO/Query/Command; no PO generic outward。
FLOW -> @Validated interface OAuthConsentRepository { Optional<OAuthConsentBO> find(TrustedRealmBO realm, ...); OAuthConsentBO save(@Valid OAuthConsentBO input,long expectedVersion); } names agree §11 access tenant+clientPk+subject active
ON INVALID/CONCURRENT/DEPENDENCY FAILURE -> 0 affected row → typed conflict; no bypass tenant。
RESULT -> only the named symbol/contract in this File block becomes available; preserve tenant, expectedVersion, scope, UTC time and secret masking stated above.
```

- Verification contribution: 本Step `mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthAuthorizationPersistenceContractTest -Dsurefire.failIfNoSpecifiedTests=false test`；此文件负责 OAuthConsentRepository 的正例与 0 affected row → typed conflict; no bypass tenant 负例。
- After this file: OAuthConsentRepository 的字段/入口或注册已落盘；与下一文件组合后本Step才能GREEN，不能把中间状态当完成。

#### File 16 — `CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/repository/OAuthAuthorizationPersistenceContractTest.java`

- Purpose: RED证明这组映射/查询/跨租户错误。
- Symbols: `OAuthAuthorizationPersistenceContractTest`。
- Repository evidence: 当前不存在；Spec §8/§11 明确目标，邻近包可验证；目标源于主Spec §7–§16。
- Dependencies and consumers: 输入依赖 Step 4 与本Step此前文件；输出供 这组认证事实的Repository/Mapper能按同realm/tenant/active/version读写及后一Step使用；Java Bean/Mapper按模块现有配置发现，前端按现有auth client消费。
- Why now: 前一文件建立本文件需要的字段或调用契约； RED证明这组映射/查询/跨租户错误在此位置后才可接消费者。
- Contract/signature changes: OAuthAuthorizationPersistenceContractTest；RED证明这组映射/查询/跨租户错误。公开行为只取主Spec §9对应API或§10/11内部合同。
- Input/output and state mapping: fixed tenant=1001/1002; timestamps Instant/UTC; SQL actual node tianquan_oauth。
- Error and edge behavior: no implementation Bean/Mapper yet → focused contract RED，不能因fixture fail。
- Standards impact: MC-ARCH-001, MC-SCOPE-001, MC-TEST-001, MC-VALID-001；按已选传统分层，输入校验与权限先于状态读取，PO不出边界，相关日志脱敏。
- Literal rule enforcement: Rule 2, Rule 3, Rule 11；此文件直接按所列原始规则落实命名、验证、类型/注入或配置，其他规则由同Step关联文件负责。
- Implementation pseudocode:

```java
INPUT -> fixed tenant=1001/1002; timestamps Instant/UTC; SQL actual node tianquan_oauth。
FLOW -> @Test for OAuthAuthorization/OAuthToken/OAuthConsent: assert same external client/resource key in different trusted tenants does not cross; query includes tenant and deleted_at IS NULL; concurrent expectedVersion loser fails; secret/ciphertext absent from VO
ON INVALID/CONCURRENT/DEPENDENCY FAILURE -> no implementation Bean/Mapper yet → focused contract RED，不能因fixture fail。
RESULT -> only the named symbol/contract in this File block becomes available; preserve tenant, expectedVersion, scope, UTC time and secret masking stated above.
```

- Verification contribution: 本Step `mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthAuthorizationPersistenceContractTest -Dsurefire.failIfNoSpecifiedTests=false test`；此文件负责 OAuthAuthorizationPersistenceContractTest 的正例与 no implementation Bean/Mapper yet → focused contract RED，不能因fixture fail 负例。
- After this file: OAuthAuthorizationPersistenceContractTest 的字段/入口或注册已落盘；与下一文件组合后本Step才能GREEN，不能把中间状态当完成。

#### File 17 — `CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/domain/converter/OAuthAuthorizationConverter.java`

- Purpose: PO↔BO双向MapStruct转换。
- Symbols: `OAuthAuthorizationConverter`。
- Repository evidence: 当前不存在；Spec §8/§11 明确目标，邻近包可验证；目标源于主Spec §7–§16。
- Dependencies and consumers: 输入依赖 Step 4 与本Step此前文件；输出供 这组认证事实的Repository/Mapper能按同realm/tenant/active/version读写及后一Step使用；Java Bean/Mapper按模块现有配置发现，前端按现有auth client消费。
- Why now: 前一文件建立本文件需要的字段或调用契约； PO↔BO双向MapStruct转换在此位置后才可接消费者。
- Contract/signature changes: OAuthAuthorizationConverter；PO↔BO双向MapStruct转换。公开行为只取主Spec §9对应API或§10/11内部合同。
- Input/output and state mapping: OAuthAuthorizationPO business fields from §11 → OAuthAuthorizationBO; unknown enum拒绝，null按列约束。
- Error and edge behavior: generated implementation/constructor must compile; no BeanUtils/json copy。
- Standards impact: MC-ARCH-001, MC-SCOPE-001, MC-TEST-001, MC-VALID-001；按已选传统分层，输入校验与权限先于状态读取，PO不出边界，相关日志脱敏。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 6, Rule 10, Rule 11；此文件直接按所列原始规则落实命名、验证、类型/注入或配置，其他规则由同Step关联文件负责。
- Implementation pseudocode:

```java
INPUT -> OAuthAuthorizationPO business fields from §11 → OAuthAuthorizationBO; unknown enum拒绝，null按列约束。
FLOW -> @Mapper(componentModel="spring") interface OAuthAuthorizationConverter extends BaseConverter<OAuthAuthorizationBO,OAuthAuthorizationPO>; @Mapping inherited tenant/audit fill ignored on insert; enum code explicit; secret excluded from toString/VO; mapping date Instant/UTC
ON INVALID/CONCURRENT/DEPENDENCY FAILURE -> generated implementation/constructor must compile; no BeanUtils/json copy。
RESULT -> only the named symbol/contract in this File block becomes available; preserve tenant, expectedVersion, scope, UTC time and secret masking stated above.
```

- Verification contribution: 本Step `mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthAuthorizationPersistenceContractTest -Dsurefire.failIfNoSpecifiedTests=false test`；此文件负责 OAuthAuthorizationConverter 的正例与 generated implementation/constructor must compile; no BeanUtils/json copy 负例。
- After this file: OAuthAuthorizationConverter 的字段/入口或注册已落盘；与下一文件组合后本Step才能GREEN，不能把中间状态当完成。

#### File 18 — `CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/dao/OAuthAuthorizationMapper.java`

- Purpose: MP Mapper具名SQL接口。
- Symbols: `OAuthAuthorizationMapper`。
- Repository evidence: 当前不存在；Spec §8/§11 明确目标，邻近包可验证；目标源于主Spec §7–§16。
- Dependencies and consumers: 输入依赖 Step 4 与本Step此前文件；输出供 这组认证事实的Repository/Mapper能按同realm/tenant/active/version读写及后一Step使用；Java Bean/Mapper按模块现有配置发现，前端按现有auth client消费。
- Why now: 前一文件建立本文件需要的字段或调用契约； MP Mapper具名SQL接口在此位置后才可接消费者。
- Contract/signature changes: OAuthAuthorizationMapper；MP Mapper具名SQL接口。公开行为只取主Spec §9对应API或§10/11内部合同。
- Input/output and state mapping: tenantId positive Long from verified MDC; result OAuthAuthorizationPO。
- Error and edge behavior: no LambdaQueryChain/Wrapper; missing tenant failclosed。
- Standards impact: MC-ARCH-001, MC-SCOPE-001, MC-TEST-001, MC-VALID-001；按已选传统分层，输入校验与权限先于状态读取，PO不出边界，相关日志脱敏。
- Literal rule enforcement: Rule 1, Rule 2, Rule 11；此文件直接按所列原始规则落实命名、验证、类型/注入或配置，其他规则由同Step关联文件负责。
- Implementation pseudocode:

```java
INPUT -> tenantId positive Long from verified MDC; result OAuthAuthorizationPO。
FLOW -> interface OAuthAuthorizationMapper extends EgonColaMapper<OAuthAuthorizationPO>; methods selectActiveById/Ids, deleteVersionedById, tenant+clientPk+principal/status/absoluteExpiresAt SQL bindings
ON INVALID/CONCURRENT/DEPENDENCY FAILURE -> no LambdaQueryChain/Wrapper; missing tenant failclosed。
RESULT -> only the named symbol/contract in this File block becomes available; preserve tenant, expectedVersion, scope, UTC time and secret masking stated above.
```

- Verification contribution: 本Step `mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthAuthorizationPersistenceContractTest -Dsurefire.failIfNoSpecifiedTests=false test`；此文件负责 OAuthAuthorizationMapper 的正例与 no LambdaQueryChain/Wrapper; missing tenant failclosed 负例。
- After this file: OAuthAuthorizationMapper 的字段/入口或注册已落盘；与下一文件组合后本Step才能GREEN，不能把中间状态当完成。

#### File 19 — `CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/resources/mapper/oauth/OAuthAuthorizationMapper.xml`

- Purpose: 具名PostgreSQL谓词/锁。
- Symbols: `OAuthAuthorizationMapper SQL`。
- Repository evidence: 当前不存在；Spec §8/§11 明确目标，邻近包可验证；目标源于主Spec §7–§16。
- Dependencies and consumers: 输入依赖 Step 4 与本Step此前文件；输出供 这组认证事实的Repository/Mapper能按同realm/tenant/active/version读写及后一Step使用；Java Bean/Mapper按模块现有配置发现，前端按现有auth client消费。
- Why now: 前一文件建立本文件需要的字段或调用契约； 具名PostgreSQL谓词/锁在此位置后才可接消费者。
- Contract/signature changes: OAuthAuthorizationMapper SQL；具名PostgreSQL谓词/锁。公开行为只取主Spec §9对应API或§10/11内部合同。
- Input/output and state mapping: 0/1 precise query or capped list; indexes §11。
- Error and edge behavior: tenant range forbidden, 0 rows conflict, no cross-group write。
- Standards impact: MC-SCOPE-001, MC-TEST-001；按已选传统分层，输入校验与权限先于状态读取，PO不出边界，相关日志脱敏。
- Literal rule enforcement: Rule 2, Rule 10, Rule 11；此文件直接按所列原始规则落实命名、验证、类型/注入或配置，其他规则由同Step关联文件负责。
- Implementation pseudocode:

```xml
INPUT -> 0/1 precise query or capped list; indexes §11。
FLOW -> SELECT business columns + inherited fields FROM tianquan_oauth.oauth_authorization WHERE tenant_id=#{tenantId} AND deleted_at IS NULL AND tenant+clientPk+principal/status/absoluteExpiresAt; guarded UPDATE id+tenant+version+expected state; SELECT FOR UPDATE only command path; max page bounded
ON INVALID/CONCURRENT/DEPENDENCY FAILURE -> tenant range forbidden, 0 rows conflict, no cross-group write。
RESULT -> only the named symbol/contract in this File block becomes available; preserve tenant, expectedVersion, scope, UTC time and secret masking stated above.
```

- Verification contribution: 本Step `mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthAuthorizationPersistenceContractTest -Dsurefire.failIfNoSpecifiedTests=false test`；此文件负责 OAuthAuthorizationMapper SQL 的正例与 tenant range forbidden, 0 rows conflict, no cross-group write 负例。
- After this file: OAuthAuthorizationMapper SQL 的字段/入口或注册已落盘；与下一文件组合后本Step才能GREEN，不能把中间状态当完成。

#### File 20 — `CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/repository/impl/OAuthAuthorizationRepositoryImpl.java`

- Purpose: MP repository ACL。
- Symbols: `OAuthAuthorizationRepositoryImpl`。
- Repository evidence: 当前不存在；Spec §8/§11 明确目标，邻近包可验证；目标源于主Spec §7–§16。
- Dependencies and consumers: 输入依赖 Step 4 与本Step此前文件；输出供 这组认证事实的Repository/Mapper能按同realm/tenant/active/version读写及后一Step使用；Java Bean/Mapper按模块现有配置发现，前端按现有auth client消费。
- Why now: 前一文件建立本文件需要的字段或调用契约； MP repository ACL在此位置后才可接消费者。
- Contract/signature changes: OAuthAuthorizationRepositoryImpl；MP repository ACL。公开行为只取主Spec §9对应API或§10/11内部合同。
- Input/output and state mapping: OAuthAuthorizationBO↔PO; raw secret/private only protected internal fields。
- Error and edge behavior: UnsupportedOperationException for generic unscoped query; errors mapped without sensitive values。
- Standards impact: MC-ARCH-001, MC-SCOPE-001, MC-TEST-001, MC-VALID-001；按已选传统分层，输入校验与权限先于状态读取，PO不出边界，相关日志脱敏。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 6, Rule 9, Rule 10, Rule 11；此文件直接按所列原始规则落实命名、验证、类型/注入或配置，其他规则由同Step关联文件负责。
- Implementation pseudocode:

```java
INPUT -> OAuthAuthorizationBO↔PO; raw secret/private only protected internal fields。
FLOW -> @Repository("oAuthAuthorizationRepository") @Slf4j @RequiredArgsConstructor; final @Qualifier("oAuthAuthorizationMapper") Mapper, converter, modelValidationUtils, properties; extends EgonColaRepository<Mapper,PO>; find maps PO→BO; save maps BO→PO, guarded version update result==1
ON INVALID/CONCURRENT/DEPENDENCY FAILURE -> UnsupportedOperationException for generic unscoped query; errors mapped without sensitive values。
RESULT -> only the named symbol/contract in this File block becomes available; preserve tenant, expectedVersion, scope, UTC time and secret masking stated above.
```

- Verification contribution: 本Step `mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthAuthorizationPersistenceContractTest -Dsurefire.failIfNoSpecifiedTests=false test`；此文件负责 OAuthAuthorizationRepositoryImpl 的正例与 UnsupportedOperationException for generic unscoped query; errors mapped without sensitive values 负例。
- After this file: OAuthAuthorizationRepositoryImpl 的字段/入口或注册已落盘；与下一文件组合后本Step才能GREEN，不能把中间状态当完成。

#### File 21 — `CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/domain/converter/OAuthTokenConverter.java`

- Purpose: PO↔BO双向MapStruct转换。
- Symbols: `OAuthTokenConverter`。
- Repository evidence: 当前不存在；Spec §8/§11 明确目标，邻近包可验证；目标源于主Spec §7–§16。
- Dependencies and consumers: 输入依赖 Step 4 与本Step此前文件；输出供 这组认证事实的Repository/Mapper能按同realm/tenant/active/version读写及后一Step使用；Java Bean/Mapper按模块现有配置发现，前端按现有auth client消费。
- Why now: 前一文件建立本文件需要的字段或调用契约； PO↔BO双向MapStruct转换在此位置后才可接消费者。
- Contract/signature changes: OAuthTokenConverter；PO↔BO双向MapStruct转换。公开行为只取主Spec §9对应API或§10/11内部合同。
- Input/output and state mapping: OAuthTokenPO business fields from §11 → OAuthTokenBO; unknown enum拒绝，null按列约束。
- Error and edge behavior: generated implementation/constructor must compile; no BeanUtils/json copy。
- Standards impact: MC-ARCH-001, MC-SCOPE-001, MC-TEST-001, MC-VALID-001；按已选传统分层，输入校验与权限先于状态读取，PO不出边界，相关日志脱敏。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 6, Rule 10, Rule 11；此文件直接按所列原始规则落实命名、验证、类型/注入或配置，其他规则由同Step关联文件负责。
- Implementation pseudocode:

```java
INPUT -> OAuthTokenPO business fields from §11 → OAuthTokenBO; unknown enum拒绝，null按列约束。
FLOW -> @Mapper(componentModel="spring") interface OAuthTokenConverter extends BaseConverter<OAuthTokenBO,OAuthTokenPO>; @Mapping inherited tenant/audit fill ignored on insert; enum code explicit; secret excluded from toString/VO; mapping date Instant/UTC
ON INVALID/CONCURRENT/DEPENDENCY FAILURE -> generated implementation/constructor must compile; no BeanUtils/json copy。
RESULT -> only the named symbol/contract in this File block becomes available; preserve tenant, expectedVersion, scope, UTC time and secret masking stated above.
```

- Verification contribution: 本Step `mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthAuthorizationPersistenceContractTest -Dsurefire.failIfNoSpecifiedTests=false test`；此文件负责 OAuthTokenConverter 的正例与 generated implementation/constructor must compile; no BeanUtils/json copy 负例。
- After this file: OAuthTokenConverter 的字段/入口或注册已落盘；与下一文件组合后本Step才能GREEN，不能把中间状态当完成。

#### File 22 — `CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/dao/OAuthTokenMapper.java`

- Purpose: MP Mapper具名SQL接口。
- Symbols: `OAuthTokenMapper`。
- Repository evidence: 当前不存在；Spec §8/§11 明确目标，邻近包可验证；目标源于主Spec §7–§16。
- Dependencies and consumers: 输入依赖 Step 4 与本Step此前文件；输出供 这组认证事实的Repository/Mapper能按同realm/tenant/active/version读写及后一Step使用；Java Bean/Mapper按模块现有配置发现，前端按现有auth client消费。
- Why now: 前一文件建立本文件需要的字段或调用契约； MP Mapper具名SQL接口在此位置后才可接消费者。
- Contract/signature changes: OAuthTokenMapper；MP Mapper具名SQL接口。公开行为只取主Spec §9对应API或§10/11内部合同。
- Input/output and state mapping: tenantId positive Long from verified MDC; result OAuthTokenPO。
- Error and edge behavior: no LambdaQueryChain/Wrapper; missing tenant failclosed。
- Standards impact: MC-ARCH-001, MC-SCOPE-001, MC-TEST-001, MC-VALID-001；按已选传统分层，输入校验与权限先于状态读取，PO不出边界，相关日志脱敏。
- Literal rule enforcement: Rule 1, Rule 2, Rule 11；此文件直接按所列原始规则落实命名、验证、类型/注入或配置，其他规则由同Step关联文件负责。
- Implementation pseudocode:

```java
INPUT -> tenantId positive Long from verified MDC; result OAuthTokenPO。
FLOW -> interface OAuthTokenMapper extends EgonColaMapper<OAuthTokenPO>; methods selectActiveById/Ids, deleteVersionedById, tenant+tokenDigest or authorizationPk+tokenType SQL bindings
ON INVALID/CONCURRENT/DEPENDENCY FAILURE -> no LambdaQueryChain/Wrapper; missing tenant failclosed。
RESULT -> only the named symbol/contract in this File block becomes available; preserve tenant, expectedVersion, scope, UTC time and secret masking stated above.
```

- Verification contribution: 本Step `mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthAuthorizationPersistenceContractTest -Dsurefire.failIfNoSpecifiedTests=false test`；此文件负责 OAuthTokenMapper 的正例与 no LambdaQueryChain/Wrapper; missing tenant failclosed 负例。
- After this file: OAuthTokenMapper 的字段/入口或注册已落盘；与下一文件组合后本Step才能GREEN，不能把中间状态当完成。

#### File 23 — `CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/resources/mapper/token/OAuthTokenMapper.xml`

- Purpose: 具名PostgreSQL谓词/锁。
- Symbols: `OAuthTokenMapper SQL`。
- Repository evidence: 当前不存在；Spec §8/§11 明确目标，邻近包可验证；目标源于主Spec §7–§16。
- Dependencies and consumers: 输入依赖 Step 4 与本Step此前文件；输出供 这组认证事实的Repository/Mapper能按同realm/tenant/active/version读写及后一Step使用；Java Bean/Mapper按模块现有配置发现，前端按现有auth client消费。
- Why now: 前一文件建立本文件需要的字段或调用契约； 具名PostgreSQL谓词/锁在此位置后才可接消费者。
- Contract/signature changes: OAuthTokenMapper SQL；具名PostgreSQL谓词/锁。公开行为只取主Spec §9对应API或§10/11内部合同。
- Input/output and state mapping: 0/1 precise query or capped list; indexes §11。
- Error and edge behavior: tenant range forbidden, 0 rows conflict, no cross-group write。
- Standards impact: MC-SCOPE-001, MC-TEST-001；按已选传统分层，输入校验与权限先于状态读取，PO不出边界，相关日志脱敏。
- Literal rule enforcement: Rule 2, Rule 10, Rule 11；此文件直接按所列原始规则落实命名、验证、类型/注入或配置，其他规则由同Step关联文件负责。
- Implementation pseudocode:

```xml
INPUT -> 0/1 precise query or capped list; indexes §11。
FLOW -> SELECT business columns + inherited fields FROM tianquan_oauth.oauth_token WHERE tenant_id=#{tenantId} AND deleted_at IS NULL AND tenant+tokenDigest or authorizationPk+tokenType; guarded UPDATE id+tenant+version+expected state; SELECT FOR UPDATE only command path; max page bounded
ON INVALID/CONCURRENT/DEPENDENCY FAILURE -> tenant range forbidden, 0 rows conflict, no cross-group write。
RESULT -> only the named symbol/contract in this File block becomes available; preserve tenant, expectedVersion, scope, UTC time and secret masking stated above.
```

- Verification contribution: 本Step `mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthAuthorizationPersistenceContractTest -Dsurefire.failIfNoSpecifiedTests=false test`；此文件负责 OAuthTokenMapper SQL 的正例与 tenant range forbidden, 0 rows conflict, no cross-group write 负例。
- After this file: OAuthTokenMapper SQL 的字段/入口或注册已落盘；与下一文件组合后本Step才能GREEN，不能把中间状态当完成。

#### File 24 — `CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/repository/impl/OAuthTokenRepositoryImpl.java`

- Purpose: MP repository ACL。
- Symbols: `OAuthTokenRepositoryImpl`。
- Repository evidence: 当前不存在；Spec §8/§11 明确目标，邻近包可验证；目标源于主Spec §7–§16。
- Dependencies and consumers: 输入依赖 Step 4 与本Step此前文件；输出供 这组认证事实的Repository/Mapper能按同realm/tenant/active/version读写及后一Step使用；Java Bean/Mapper按模块现有配置发现，前端按现有auth client消费。
- Why now: 前一文件建立本文件需要的字段或调用契约； MP repository ACL在此位置后才可接消费者。
- Contract/signature changes: OAuthTokenRepositoryImpl；MP repository ACL。公开行为只取主Spec §9对应API或§10/11内部合同。
- Input/output and state mapping: OAuthTokenBO↔PO; raw secret/private only protected internal fields。
- Error and edge behavior: UnsupportedOperationException for generic unscoped query; errors mapped without sensitive values。
- Standards impact: MC-ARCH-001, MC-SCOPE-001, MC-TEST-001, MC-VALID-001；按已选传统分层，输入校验与权限先于状态读取，PO不出边界，相关日志脱敏。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 6, Rule 9, Rule 10, Rule 11；此文件直接按所列原始规则落实命名、验证、类型/注入或配置，其他规则由同Step关联文件负责。
- Implementation pseudocode:

```java
INPUT -> OAuthTokenBO↔PO; raw secret/private only protected internal fields。
FLOW -> @Repository("oAuthTokenRepository") @Slf4j @RequiredArgsConstructor; final @Qualifier("oAuthTokenMapper") Mapper, converter, modelValidationUtils, properties; extends EgonColaRepository<Mapper,PO>; find maps PO→BO; save maps BO→PO, guarded version update result==1
ON INVALID/CONCURRENT/DEPENDENCY FAILURE -> UnsupportedOperationException for generic unscoped query; errors mapped without sensitive values。
RESULT -> only the named symbol/contract in this File block becomes available; preserve tenant, expectedVersion, scope, UTC time and secret masking stated above.
```

- Verification contribution: 本Step `mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthAuthorizationPersistenceContractTest -Dsurefire.failIfNoSpecifiedTests=false test`；此文件负责 OAuthTokenRepositoryImpl 的正例与 UnsupportedOperationException for generic unscoped query; errors mapped without sensitive values 负例。
- After this file: OAuthTokenRepositoryImpl 的字段/入口或注册已落盘；与下一文件组合后本Step才能GREEN，不能把中间状态当完成。

#### File 25 — `CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/domain/converter/OAuthConsentConverter.java`

- Purpose: PO↔BO双向MapStruct转换。
- Symbols: `OAuthConsentConverter`。
- Repository evidence: 当前不存在；Spec §8/§11 明确目标，邻近包可验证；目标源于主Spec §7–§16。
- Dependencies and consumers: 输入依赖 Step 4 与本Step此前文件；输出供 这组认证事实的Repository/Mapper能按同realm/tenant/active/version读写及后一Step使用；Java Bean/Mapper按模块现有配置发现，前端按现有auth client消费。
- Why now: 前一文件建立本文件需要的字段或调用契约； PO↔BO双向MapStruct转换在此位置后才可接消费者。
- Contract/signature changes: OAuthConsentConverter；PO↔BO双向MapStruct转换。公开行为只取主Spec §9对应API或§10/11内部合同。
- Input/output and state mapping: OAuthConsentPO business fields from §11 → OAuthConsentBO; unknown enum拒绝，null按列约束。
- Error and edge behavior: generated implementation/constructor must compile; no BeanUtils/json copy。
- Standards impact: MC-ARCH-001, MC-SCOPE-001, MC-TEST-001, MC-VALID-001；按已选传统分层，输入校验与权限先于状态读取，PO不出边界，相关日志脱敏。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 6, Rule 10, Rule 11；此文件直接按所列原始规则落实命名、验证、类型/注入或配置，其他规则由同Step关联文件负责。
- Implementation pseudocode:

```java
INPUT -> OAuthConsentPO business fields from §11 → OAuthConsentBO; unknown enum拒绝，null按列约束。
FLOW -> @Mapper(componentModel="spring") interface OAuthConsentConverter extends BaseConverter<OAuthConsentBO,OAuthConsentPO>; @Mapping inherited tenant/audit fill ignored on insert; enum code explicit; secret excluded from toString/VO; mapping date Instant/UTC
ON INVALID/CONCURRENT/DEPENDENCY FAILURE -> generated implementation/constructor must compile; no BeanUtils/json copy。
RESULT -> only the named symbol/contract in this File block becomes available; preserve tenant, expectedVersion, scope, UTC time and secret masking stated above.
```

- Verification contribution: 本Step `mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthAuthorizationPersistenceContractTest -Dsurefire.failIfNoSpecifiedTests=false test`；此文件负责 OAuthConsentConverter 的正例与 generated implementation/constructor must compile; no BeanUtils/json copy 负例。
- After this file: OAuthConsentConverter 的字段/入口或注册已落盘；与下一文件组合后本Step才能GREEN，不能把中间状态当完成。

#### File 26 — `CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/dao/OAuthConsentMapper.java`

- Purpose: MP Mapper具名SQL接口。
- Symbols: `OAuthConsentMapper`。
- Repository evidence: 当前不存在；Spec §8/§11 明确目标，邻近包可验证；目标源于主Spec §7–§16。
- Dependencies and consumers: 输入依赖 Step 4 与本Step此前文件；输出供 这组认证事实的Repository/Mapper能按同realm/tenant/active/version读写及后一Step使用；Java Bean/Mapper按模块现有配置发现，前端按现有auth client消费。
- Why now: 前一文件建立本文件需要的字段或调用契约； MP Mapper具名SQL接口在此位置后才可接消费者。
- Contract/signature changes: OAuthConsentMapper；MP Mapper具名SQL接口。公开行为只取主Spec §9对应API或§10/11内部合同。
- Input/output and state mapping: tenantId positive Long from verified MDC; result OAuthConsentPO。
- Error and edge behavior: no LambdaQueryChain/Wrapper; missing tenant failclosed。
- Standards impact: MC-ARCH-001, MC-SCOPE-001, MC-TEST-001, MC-VALID-001；按已选传统分层，输入校验与权限先于状态读取，PO不出边界，相关日志脱敏。
- Literal rule enforcement: Rule 1, Rule 2, Rule 11；此文件直接按所列原始规则落实命名、验证、类型/注入或配置，其他规则由同Step关联文件负责。
- Implementation pseudocode:

```java
INPUT -> tenantId positive Long from verified MDC; result OAuthConsentPO。
FLOW -> interface OAuthConsentMapper extends EgonColaMapper<OAuthConsentPO>; methods selectActiveById/Ids, deleteVersionedById, tenant+clientPk+subject active SQL bindings
ON INVALID/CONCURRENT/DEPENDENCY FAILURE -> no LambdaQueryChain/Wrapper; missing tenant failclosed。
RESULT -> only the named symbol/contract in this File block becomes available; preserve tenant, expectedVersion, scope, UTC time and secret masking stated above.
```

- Verification contribution: 本Step `mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthAuthorizationPersistenceContractTest -Dsurefire.failIfNoSpecifiedTests=false test`；此文件负责 OAuthConsentMapper 的正例与 no LambdaQueryChain/Wrapper; missing tenant failclosed 负例。
- After this file: OAuthConsentMapper 的字段/入口或注册已落盘；与下一文件组合后本Step才能GREEN，不能把中间状态当完成。

#### File 27 — `CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/resources/mapper/oauth/OAuthConsentMapper.xml`

- Purpose: 具名PostgreSQL谓词/锁。
- Symbols: `OAuthConsentMapper SQL`。
- Repository evidence: 当前不存在；Spec §8/§11 明确目标，邻近包可验证；目标源于主Spec §7–§16。
- Dependencies and consumers: 输入依赖 Step 4 与本Step此前文件；输出供 这组认证事实的Repository/Mapper能按同realm/tenant/active/version读写及后一Step使用；Java Bean/Mapper按模块现有配置发现，前端按现有auth client消费。
- Why now: 前一文件建立本文件需要的字段或调用契约； 具名PostgreSQL谓词/锁在此位置后才可接消费者。
- Contract/signature changes: OAuthConsentMapper SQL；具名PostgreSQL谓词/锁。公开行为只取主Spec §9对应API或§10/11内部合同。
- Input/output and state mapping: 0/1 precise query or capped list; indexes §11。
- Error and edge behavior: tenant range forbidden, 0 rows conflict, no cross-group write。
- Standards impact: MC-SCOPE-001, MC-TEST-001；按已选传统分层，输入校验与权限先于状态读取，PO不出边界，相关日志脱敏。
- Literal rule enforcement: Rule 2, Rule 10, Rule 11；此文件直接按所列原始规则落实命名、验证、类型/注入或配置，其他规则由同Step关联文件负责。
- Implementation pseudocode:

```xml
INPUT -> 0/1 precise query or capped list; indexes §11。
FLOW -> SELECT business columns + inherited fields FROM tianquan_oauth.oauth_consent WHERE tenant_id=#{tenantId} AND deleted_at IS NULL AND tenant+clientPk+subject active; guarded UPDATE id+tenant+version+expected state; SELECT FOR UPDATE only command path; max page bounded
ON INVALID/CONCURRENT/DEPENDENCY FAILURE -> tenant range forbidden, 0 rows conflict, no cross-group write。
RESULT -> only the named symbol/contract in this File block becomes available; preserve tenant, expectedVersion, scope, UTC time and secret masking stated above.
```

- Verification contribution: 本Step `mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthAuthorizationPersistenceContractTest -Dsurefire.failIfNoSpecifiedTests=false test`；此文件负责 OAuthConsentMapper SQL 的正例与 tenant range forbidden, 0 rows conflict, no cross-group write 负例。
- After this file: OAuthConsentMapper SQL 的字段/入口或注册已落盘；与下一文件组合后本Step才能GREEN，不能把中间状态当完成。

#### File 28 — `CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/repository/impl/OAuthConsentRepositoryImpl.java`

- Purpose: MP repository ACL。
- Symbols: `OAuthConsentRepositoryImpl`。
- Repository evidence: 当前不存在；Spec §8/§11 明确目标，邻近包可验证；目标源于主Spec §7–§16。
- Dependencies and consumers: 输入依赖 Step 4 与本Step此前文件；输出供 这组认证事实的Repository/Mapper能按同realm/tenant/active/version读写及后一Step使用；Java Bean/Mapper按模块现有配置发现，前端按现有auth client消费。
- Why now: 前一文件建立本文件需要的字段或调用契约； MP repository ACL在此位置后才可接消费者。
- Contract/signature changes: OAuthConsentRepositoryImpl；MP repository ACL。公开行为只取主Spec §9对应API或§10/11内部合同。
- Input/output and state mapping: OAuthConsentBO↔PO; raw secret/private only protected internal fields。
- Error and edge behavior: UnsupportedOperationException for generic unscoped query; errors mapped without sensitive values。
- Standards impact: MC-ARCH-001, MC-SCOPE-001, MC-TEST-001, MC-VALID-001；按已选传统分层，输入校验与权限先于状态读取，PO不出边界，相关日志脱敏。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 6, Rule 9, Rule 10, Rule 11；此文件直接按所列原始规则落实命名、验证、类型/注入或配置，其他规则由同Step关联文件负责。
- Implementation pseudocode:

```java
INPUT -> OAuthConsentBO↔PO; raw secret/private only protected internal fields。
FLOW -> @Repository("oAuthConsentRepository") @Slf4j @RequiredArgsConstructor; final @Qualifier("oAuthConsentMapper") Mapper, converter, modelValidationUtils, properties; extends EgonColaRepository<Mapper,PO>; find maps PO→BO; save maps BO→PO, guarded version update result==1
ON INVALID/CONCURRENT/DEPENDENCY FAILURE -> UnsupportedOperationException for generic unscoped query; errors mapped without sensitive values。
RESULT -> only the named symbol/contract in this File block becomes available; preserve tenant, expectedVersion, scope, UTC time and secret masking stated above.
```

- Verification contribution: 本Step `mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthAuthorizationPersistenceContractTest -Dsurefire.failIfNoSpecifiedTests=false test`；此文件负责 OAuthConsentRepositoryImpl 的正例与 UnsupportedOperationException for generic unscoped query; errors mapped without sensitive values 负例。
- After this file: OAuthConsentRepositoryImpl 的字段/入口或注册已落盘；与下一文件组合后本Step才能GREEN，不能把中间状态当完成。

- Validation working directory: `/Users/mario/SelfProject/Egon-COLA`。
- Verification command: `mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthAuthorizationPersistenceContractTest -Dsurefire.failIfNoSpecifiedTests=false test`。
- Expected result: 先确认上述RED由缺失目标行为引起，GREEN时命名用例成功、模块编译成功、`git diff --check`零错误；不以不存在依赖/数据库/fixture而失败作为RED。
- Failure returns to: Step 4 的编译/Schema前置；若需改变外部语义、架构或DDL版本则回主Spec。
- Completion criteria: 这组认证事实的Repository/Mapper能按同realm/tenant/active/version读写；本Step的所有Manual Checks在实际执行时给出证据，未通过不能提交。
- Rollback: 提交前仅恢复本Step的精确路径；DDL已对真实数据运行后只forward-fix和新版本，不能编辑既有SQL/历史或恢复旧安全世代。
- Commit paths: `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/domain/enums/OAuthTokenTypeEnum.java`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/domain/bo/SignedTokenClaimsBO.java`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/domain/bo/OAuthSecurityVersionBO.java`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/domain/bo/OAuthTokenMetadataBO.java`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/domain/bo/OAuthSecuritySnapshotBO.java`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/domain/bo/OAuthAuthorizationAttributesBO.java`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/domain/po/OAuthAuthorizationPO.java`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/domain/bo/OAuthAuthorizationBO.java`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/repository/OAuthAuthorizationRepository.java`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/domain/po/OAuthTokenPO.java`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/domain/bo/OAuthTokenBO.java`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/repository/OAuthTokenRepository.java`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/domain/po/OAuthConsentPO.java`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/domain/bo/OAuthConsentBO.java`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/repository/OAuthConsentRepository.java`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/repository/OAuthAuthorizationPersistenceContractTest.java`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/domain/converter/OAuthAuthorizationConverter.java`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/dao/OAuthAuthorizationMapper.java`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/resources/mapper/oauth/OAuthAuthorizationMapper.xml`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/repository/impl/OAuthAuthorizationRepositoryImpl.java`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/domain/converter/OAuthTokenConverter.java`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/dao/OAuthTokenMapper.java`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/resources/mapper/token/OAuthTokenMapper.xml`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/repository/impl/OAuthTokenRepositoryImpl.java`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/domain/converter/OAuthConsentConverter.java`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/dao/OAuthConsentMapper.java`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/resources/mapper/oauth/OAuthConsentMapper.xml`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/repository/impl/OAuthConsentRepositoryImpl.java`。
- Commit: `feat(shoubing): persist oauthauthorization/oauthtoken/oauthconsent state`。

### Step 6 — 持久化密钥、撤销与审计状态

- Requirements: REQ-012, REQ-013, REQ-014, REQ-015
- Dependencies: Step 5
- Baseline state: 当前HEAD的对应源码/测试如每File的Repository evidence，先前Step已完成并有其focused GREEN；本Step尚不存在OAuthSigningKeyPO所列新行为。
- Observable outcome: 这组认证事实的Repository/Mapper能按同realm/tenant/active/version读写。
- End state: 这组认证事实的Repository/Mapper能按同realm/tenant/active/version读写；后续Step的Consumer尚未切流，不声明完整生产验收。
- Test-first gate: Required — 必需PO/BO/Repository签名先让test编译；测试在Mapper/Impl装配前因缺Bean或缺具名SQL按目标RED，随后GREEN。
- Manual Checks: MC-ARCH-001, MC-REUSE-001, MC-NAME-001, MC-VALID-001, MC-MODEL-001, MC-CONVERT-001, MC-LOG-001, MC-BEAN-001, MC-JSON-001, MC-TIME-001, MC-SCOPE-001, MC-TEST-001
- Literal Rules: Rule 1, Rule 2, Rule 3, Rule 4, Rule 6, Rule 9, Rule 10, Rule 11
- Ordered files:

#### File 1 — `CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/domain/enums/OAuthKeyStatusEnum.java`

- Purpose: 关闭 `OAuthKeyStatusEnum` 在主Spec中的字段/消费依赖。
- Symbols: `OAuthKeyStatusEnum`。
- Repository evidence: 当前不存在；主Spec §10.3/§15.2 命名此对象/字段；现有相邻业务包提供放置证据。
- Dependencies and consumers: 本Step既有File块调用该类型；数据来源/消费者为主Spec §10.3/§15.2，不产生新HTTP路由。
- Why now: 该类型按本Step的测试/编译依赖顺序落位，以免由实现者临时发明。
- Contract/signature changes: PUBLISHED→ACTIVE→VERIFY_ONLY→RETIRED or COMPROMISED; stable DB/JSON values。
- Input/output and state mapping: 精确字段由主Spec §10.3/§15.2 指定，realm从签名/注册得出，Instant为UTC，版本单调，秘密字段仅服务器内部。
- Error and edge behavior: null/未知字段/跨tenant/重复版本拒绝；不转成宽松缺省或吞异常。
- Standards impact: MC-ARCH-001, MC-NAME-001, MC-VALID-001, MC-MODEL-001, MC-CONVERT-001, MC-BEAN-001, MC-JSON-001, MC-TIME-001, MC-SCOPE-001, MC-TEST-001；行为Bean使用具名final Qualifier、@RequiredArgsConstructor和@Slf4j，载体遵循四注解与Builder。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 6, Rule 9, Rule 10, Rule 11；class按POJO语义命名，边界注解验证，跨层转换只用既有BaseConverter，Jackson和java.time字段明确。
- Implementation pseudocode:

```java
INPUT -> 仅接受主Spec §10.3/§15.2 的可信issuer/realm/输入字段；调用前 @Valid 或 ValueObject不变量检查。
FLOW -> PUBLISHED→ACTIVE→VERIFY_ONLY→RETIRED or COMPROMISED; stable DB/JSON values。
OUTPUT -> OAuthKeyStatusEnum 的Java方法、DTO字段或配置可被本Step既有调用者编译使用；不新增业务API。
FAIL -> 空值/跨tenant/未知schema/重复版本/外部依赖错误按本Step已列错误模型fail-closed，不能输出原secret/token。
```

- Verification contribution: Step 6 的focus测试对 `OAuthKeyStatusEnum` 的字段、装配、负例及不泄密断言。
- After this file: `OAuthKeyStatusEnum` 成为已声明的Step依赖，下一文件可按原合同引用；未完成整条行为前不宣称GREEN。

#### File 2 — `CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/domain/enums/OAuthSecurityTargetEnum.java`

- Purpose: 关闭 `OAuthSecurityTargetEnum` 在主Spec中的字段/消费依赖。
- Symbols: `OAuthSecurityTargetEnum`。
- Repository evidence: 当前不存在；主Spec §10.3/§15.2 命名此对象/字段；现有相邻业务包提供放置证据。
- Dependencies and consumers: 本Step既有File块调用该类型；数据来源/消费者为主Spec §10.3/§15.2，不产生新HTTP路由。
- Why now: 该类型按本Step的测试/编译依赖顺序落位，以免由实现者临时发明。
- Contract/signature changes: REALM/CLIENT/SUBJECT/JTI/AUTHORIZATION/KID kinds, consistent with security_state/revocation。
- Input/output and state mapping: 精确字段由主Spec §10.3/§15.2 指定，realm从签名/注册得出，Instant为UTC，版本单调，秘密字段仅服务器内部。
- Error and edge behavior: null/未知字段/跨tenant/重复版本拒绝；不转成宽松缺省或吞异常。
- Standards impact: MC-ARCH-001, MC-NAME-001, MC-VALID-001, MC-MODEL-001, MC-CONVERT-001, MC-BEAN-001, MC-JSON-001, MC-TIME-001, MC-SCOPE-001, MC-TEST-001；行为Bean使用具名final Qualifier、@RequiredArgsConstructor和@Slf4j，载体遵循四注解与Builder。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 6, Rule 9, Rule 10, Rule 11；class按POJO语义命名，边界注解验证，跨层转换只用既有BaseConverter，Jackson和java.time字段明确。
- Implementation pseudocode:

```java
INPUT -> 仅接受主Spec §10.3/§15.2 的可信issuer/realm/输入字段；调用前 @Valid 或 ValueObject不变量检查。
FLOW -> REALM/CLIENT/SUBJECT/JTI/AUTHORIZATION/KID kinds, consistent with security_state/revocation。
OUTPUT -> OAuthSecurityTargetEnum 的Java方法、DTO字段或配置可被本Step既有调用者编译使用；不新增业务API。
FAIL -> 空值/跨tenant/未知schema/重复版本/外部依赖错误按本Step已列错误模型fail-closed，不能输出原secret/token。
```

- Verification contribution: Step 6 的focus测试对 `OAuthSecurityTargetEnum` 的字段、装配、负例及不泄密断言。
- After this file: `OAuthSecurityTargetEnum` 成为已声明的Step依赖，下一文件可按原合同引用；未完成整条行为前不宣称GREEN。

#### File 3 — `CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/service/impl/OAuthKeyCipherComponent.java`

- Purpose: 关闭 `OAuthKeyCipherComponent` 在主Spec中的字段/消费依赖。
- Symbols: `OAuthKeyCipherComponent`。
- Repository evidence: 当前不存在；主Spec §10.3/§15.2 命名此对象/字段；现有相邻业务包提供放置证据。
- Dependencies and consumers: 本Step既有File块调用该类型；数据来源/消费者为主Spec §10.3/§15.2，不产生新HTTP路由。
- Why now: 该类型按本Step的测试/编译依赖顺序落位，以免由实现者临时发明。
- Contract/signature changes: JDK AES-256-GCM envelope ENC:v1; AAD=canonical issuer+kid, mounted KEK id; never log private material。
- Input/output and state mapping: 精确字段由主Spec §10.3/§15.2 指定，realm从签名/注册得出，Instant为UTC，版本单调，秘密字段仅服务器内部。
- Error and edge behavior: null/未知字段/跨tenant/重复版本拒绝；不转成宽松缺省或吞异常。
- Standards impact: MC-ARCH-001, MC-NAME-001, MC-VALID-001, MC-MODEL-001, MC-CONVERT-001, MC-BEAN-001, MC-JSON-001, MC-TIME-001, MC-SCOPE-001, MC-TEST-001；行为Bean使用具名final Qualifier、@RequiredArgsConstructor和@Slf4j，载体遵循四注解与Builder。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 6, Rule 9, Rule 10, Rule 11；class按POJO语义命名，边界注解验证，跨层转换只用既有BaseConverter，Jackson和java.time字段明确。
- Implementation pseudocode:

```java
INPUT -> 仅接受主Spec §10.3/§15.2 的可信issuer/realm/输入字段；调用前 @Valid 或 ValueObject不变量检查。
FLOW -> JDK AES-256-GCM envelope ENC:v1; AAD=canonical issuer+kid, mounted KEK id; never log private material。
OUTPUT -> OAuthKeyCipherComponent 的Java方法、DTO字段或配置可被本Step既有调用者编译使用；不新增业务API。
FAIL -> 空值/跨tenant/未知schema/重复版本/外部依赖错误按本Step已列错误模型fail-closed，不能输出原secret/token。
```

- Verification contribution: Step 6 的focus测试对 `OAuthKeyCipherComponent` 的字段、装配、负例及不泄密断言。
- After this file: `OAuthKeyCipherComponent` 成为已声明的Step依赖，下一文件可按原合同引用；未完成整条行为前不宣称GREEN。

#### File 4 — `CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/config/OAuthKeyEncryptionProperties.java`

- Purpose: 关闭 `OAuthKeyEncryptionProperties` 在主Spec中的字段/消费依赖。
- Symbols: `OAuthKeyEncryptionProperties`。
- Repository evidence: 当前不存在；主Spec §10.3/§15.2 命名此对象/字段；现有相邻业务包提供放置证据。
- Dependencies and consumers: 本Step既有File块调用该类型；数据来源/消费者为主Spec §10.3/§15.2，不产生新HTTP路由。
- Why now: 该类型按本Step的测试/编译依赖顺序落位，以免由实现者临时发明。
- Contract/signature changes: @ConfigurationProperties key-id/key-file validated Path; base/local key parity and 0600 mount。
- Input/output and state mapping: 精确字段由主Spec §10.3/§15.2 指定，realm从签名/注册得出，Instant为UTC，版本单调，秘密字段仅服务器内部。
- Error and edge behavior: null/未知字段/跨tenant/重复版本拒绝；不转成宽松缺省或吞异常。
- Standards impact: MC-ARCH-001, MC-NAME-001, MC-VALID-001, MC-MODEL-001, MC-CONVERT-001, MC-BEAN-001, MC-JSON-001, MC-TIME-001, MC-SCOPE-001, MC-TEST-001；行为Bean使用具名final Qualifier、@RequiredArgsConstructor和@Slf4j，载体遵循四注解与Builder。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 6, Rule 9, Rule 10, Rule 11；class按POJO语义命名，边界注解验证，跨层转换只用既有BaseConverter，Jackson和java.time字段明确。
- Implementation pseudocode:

```java
INPUT -> 仅接受主Spec §10.3/§15.2 的可信issuer/realm/输入字段；调用前 @Valid 或 ValueObject不变量检查。
FLOW -> @ConfigurationProperties key-id/key-file validated Path; base/local key parity and 0600 mount。
OUTPUT -> OAuthKeyEncryptionProperties 的Java方法、DTO字段或配置可被本Step既有调用者编译使用；不新增业务API。
FAIL -> 空值/跨tenant/未知schema/重复版本/外部依赖错误按本Step已列错误模型fail-closed，不能输出原secret/token。
```

- Verification contribution: Step 6 的focus测试对 `OAuthKeyEncryptionProperties` 的字段、装配、负例及不泄密断言。
- After this file: `OAuthKeyEncryptionProperties` 成为已声明的Step依赖，下一文件可按原合同引用；未完成整条行为前不宣称GREEN。

#### File 5 — `CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/domain/po/OAuthSigningKeyPO.java`

- Purpose: EgonModel承载本表完整列。
- Symbols: `OAuthSigningKeyPO`。
- Repository evidence: 当前不存在；Spec §8/§11 明确目标，邻近包可验证；目标源于主Spec §7–§16。
- Dependencies and consumers: 输入依赖 Step 5 与本Step此前文件；输出供 这组认证事实的Repository/Mapper能按同realm/tenant/active/version读写及后一Step使用；Java Bean/Mapper按模块现有配置发现，前端按现有auth client消费。
- Why now: 本Step的编译前置/RED契约先锁定； EgonModel承载本表完整列在此位置后才可接消费者。
- Contract/signature changes: OAuthSigningKeyPO；EgonModel承载本表完整列。公开行为只取主Spec §9对应API或§10/11内部合同。
- Input/output and state mapping: 11.2 oauth_signing_key native columns snake_case→camelCase; inherited id/tenant/audit/deletedAt/version only一次。
- Error and edge behavior: UNIQUE/NULL/tenant违反时映射typed conflict；不可向HTTP返回PO。
- Standards impact: MC-ARCH-001, MC-SCOPE-001, MC-TEST-001, MC-VALID-001；按已选传统分层，输入校验与权限先于状态读取，PO不出边界，相关日志脱敏。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 6, Rule 10, Rule 11；此文件直接按所列原始规则落实命名、验证、类型/注入或配置，其他规则由同Step关联文件负责。
- Implementation pseudocode:

```java
INPUT -> 11.2 oauth_signing_key native columns snake_case→camelCase; inherited id/tenant/audit/deletedAt/version only一次。
FLOW -> @TableName("oauth_signing_key") class OAuthSigningKeyPO extends EgonModel<OAuthSigningKeyPO>; §11 oauth_signing_key business fields; @Data/@NoArgsConstructor/@AllArgsConstructor/@Accessors(chain=true)/@SuperBuilder/@EqualsAndHashCode(callSuper=true); @EnumValue/secret exclude
ON INVALID/CONCURRENT/DEPENDENCY FAILURE -> UNIQUE/NULL/tenant违反时映射typed conflict；不可向HTTP返回PO。
RESULT -> only the named symbol/contract in this File block becomes available; preserve tenant, expectedVersion, scope, UTC time and secret masking stated above.
```

- Verification contribution: 本Step `mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthSigningKeyPersistenceContractTest -Dsurefire.failIfNoSpecifiedTests=false test`；此文件负责 OAuthSigningKeyPO 的正例与 UNIQUE/NULL/tenant违反时映射typed conflict；不可向HTTP返回PO 负例。
- After this file: OAuthSigningKeyPO 的字段/入口或注册已落盘；与下一文件组合后本Step才能GREEN，不能把中间状态当完成。

#### File 6 — `CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/domain/bo/OAuthSigningKeyBO.java`

- Purpose: Service计算与PO分界。
- Symbols: `OAuthSigningKeyBO`。
- Repository evidence: 当前不存在；Spec §8/§11 明确目标，邻近包可验证；目标源于主Spec §7–§16。
- Dependencies and consumers: 输入依赖 Step 5 与本Step此前文件；输出供 这组认证事实的Repository/Mapper能按同realm/tenant/active/version读写及后一Step使用；Java Bean/Mapper按模块现有配置发现，前端按现有auth client消费。
- Why now: 前一文件建立本文件需要的字段或调用契约； Service计算与PO分界在此位置后才可接消费者。
- Contract/signature changes: OAuthSigningKeyBO；Service计算与PO分界。公开行为只取主Spec §9对应API或§10/11内部合同。
- Input/output and state mapping: outbound business values only; single ACTIVE, public JWK/private ciphertext; tenant from TrustedRealmBO。
- Error and edge behavior: missing/invalid enum, scope or date rejected before Mapper。
- Standards impact: MC-ARCH-001, MC-SCOPE-001, MC-TEST-001, MC-VALID-001；按已选传统分层，输入校验与权限先于状态读取，PO不出边界，相关日志脱敏。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 6, Rule 10, Rule 11；此文件直接按所列原始规则落实命名、验证、类型/注入或配置，其他规则由同Step关联文件负责。
- Implementation pseudocode:

```java
INPUT -> outbound business values only; single ACTIVE, public JWK/private ciphertext; tenant from TrustedRealmBO。
FLOW -> class OAuthSigningKeyBO { business fields from OAuthSigningKeyPO; id,version; } @Data @NoArgsConstructor @AllArgsConstructor @Accessors(chain=true) @Builder; @Valid groups Create/Update/Query
ON INVALID/CONCURRENT/DEPENDENCY FAILURE -> missing/invalid enum, scope or date rejected before Mapper。
RESULT -> only the named symbol/contract in this File block becomes available; preserve tenant, expectedVersion, scope, UTC time and secret masking stated above.
```

- Verification contribution: 本Step `mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthSigningKeyPersistenceContractTest -Dsurefire.failIfNoSpecifiedTests=false test`；此文件负责 OAuthSigningKeyBO 的正例与 missing/invalid enum, scope or date rejected before Mapper 负例。
- After this file: OAuthSigningKeyBO 的字段/入口或注册已落盘；与下一文件组合后本Step才能GREEN，不能把中间状态当完成。

#### File 7 — `CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/repository/OAuthSigningKeyRepository.java`

- Purpose: Service-facing typed contract。
- Symbols: `OAuthSigningKeyRepository`。
- Repository evidence: 当前不存在；Spec §8/§11 明确目标，邻近包可验证；目标源于主Spec §7–§16。
- Dependencies and consumers: 输入依赖 Step 5 与本Step此前文件；输出供 这组认证事实的Repository/Mapper能按同realm/tenant/active/version读写及后一Step使用；Java Bean/Mapper按模块现有配置发现，前端按现有auth client消费。
- Why now: 前一文件建立本文件需要的字段或调用契约； Service-facing typed contract在此位置后才可接消费者。
- Contract/signature changes: OAuthSigningKeyRepository；Service-facing typed contract。公开行为只取主Spec §9对应API或§10/11内部合同。
- Input/output and state mapping: Service uses BO/Query/Command; no PO generic outward。
- Error and edge behavior: 0 affected row → typed conflict; no bypass tenant。
- Standards impact: MC-ARCH-001, MC-SCOPE-001, MC-TEST-001, MC-VALID-001；按已选传统分层，输入校验与权限先于状态读取，PO不出边界，相关日志脱敏。
- Literal rule enforcement: Rule 1, Rule 2, Rule 11；此文件直接按所列原始规则落实命名、验证、类型/注入或配置，其他规则由同Step关联文件负责。
- Implementation pseudocode:

```java
INPUT -> Service uses BO/Query/Command; no PO generic outward。
FLOW -> @Validated interface OAuthSigningKeyRepository { Optional<OAuthSigningKeyBO> find(TrustedRealmBO realm, ...); OAuthSigningKeyBO save(@Valid OAuthSigningKeyBO input,long expectedVersion); } names agree §11 access tenant+kid/status active
ON INVALID/CONCURRENT/DEPENDENCY FAILURE -> 0 affected row → typed conflict; no bypass tenant。
RESULT -> only the named symbol/contract in this File block becomes available; preserve tenant, expectedVersion, scope, UTC time and secret masking stated above.
```

- Verification contribution: 本Step `mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthSigningKeyPersistenceContractTest -Dsurefire.failIfNoSpecifiedTests=false test`；此文件负责 OAuthSigningKeyRepository 的正例与 0 affected row → typed conflict; no bypass tenant 负例。
- After this file: OAuthSigningKeyRepository 的字段/入口或注册已落盘；与下一文件组合后本Step才能GREEN，不能把中间状态当完成。

#### File 8 — `CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/domain/po/OAuthSecurityStatePO.java`

- Purpose: EgonModel承载本表完整列。
- Symbols: `OAuthSecurityStatePO`。
- Repository evidence: 当前不存在；Spec §8/§11 明确目标，邻近包可验证；目标源于主Spec §7–§16。
- Dependencies and consumers: 输入依赖 Step 5 与本Step此前文件；输出供 这组认证事实的Repository/Mapper能按同realm/tenant/active/version读写及后一Step使用；Java Bean/Mapper按模块现有配置发现，前端按现有auth client消费。
- Why now: 前一文件建立本文件需要的字段或调用契约； EgonModel承载本表完整列在此位置后才可接消费者。
- Contract/signature changes: OAuthSecurityStatePO；EgonModel承载本表完整列。公开行为只取主Spec §9对应API或§10/11内部合同。
- Input/output and state mapping: 11.2 oauth_security_state native columns snake_case→camelCase; inherited id/tenant/audit/deletedAt/version only一次。
- Error and edge behavior: UNIQUE/NULL/tenant违反时映射typed conflict；不可向HTTP返回PO。
- Standards impact: MC-ARCH-001, MC-SCOPE-001, MC-TEST-001, MC-VALID-001；按已选传统分层，输入校验与权限先于状态读取，PO不出边界，相关日志脱敏。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 6, Rule 10, Rule 11；此文件直接按所列原始规则落实命名、验证、类型/注入或配置，其他规则由同Step关联文件负责。
- Implementation pseudocode:

```java
INPUT -> 11.2 oauth_security_state native columns snake_case→camelCase; inherited id/tenant/audit/deletedAt/version only一次。
FLOW -> @TableName("oauth_security_state") class OAuthSecurityStatePO extends EgonModel<OAuthSecurityStatePO>; §11 oauth_security_state business fields; @Data/@NoArgsConstructor/@AllArgsConstructor/@Accessors(chain=true)/@SuperBuilder/@EqualsAndHashCode(callSuper=true); @EnumValue/secret exclude
ON INVALID/CONCURRENT/DEPENDENCY FAILURE -> UNIQUE/NULL/tenant违反时映射typed conflict；不可向HTTP返回PO。
RESULT -> only the named symbol/contract in this File block becomes available; preserve tenant, expectedVersion, scope, UTC time and secret masking stated above.
```

- Verification contribution: 本Step `mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthSigningKeyPersistenceContractTest -Dsurefire.failIfNoSpecifiedTests=false test`；此文件负责 OAuthSecurityStatePO 的正例与 UNIQUE/NULL/tenant违反时映射typed conflict；不可向HTTP返回PO 负例。
- After this file: OAuthSecurityStatePO 的字段/入口或注册已落盘；与下一文件组合后本Step才能GREEN，不能把中间状态当完成。

#### File 9 — `CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/domain/bo/OAuthSecurityStateBO.java`

- Purpose: Service计算与PO分界。
- Symbols: `OAuthSecurityStateBO`。
- Repository evidence: 当前不存在；Spec §8/§11 明确目标，邻近包可验证；目标源于主Spec §7–§16。
- Dependencies and consumers: 输入依赖 Step 5 与本Step此前文件；输出供 这组认证事实的Repository/Mapper能按同realm/tenant/active/version读写及后一Step使用；Java Bean/Mapper按模块现有配置发现，前端按现有auth client消费。
- Why now: 前一文件建立本文件需要的字段或调用契约； Service计算与PO分界在此位置后才可接消费者。
- Contract/signature changes: OAuthSecurityStateBO；Service计算与PO分界。公开行为只取主Spec §9对应API或§10/11内部合同。
- Input/output and state mapping: outbound business values only; missing state failclosed; tenant from TrustedRealmBO。
- Error and edge behavior: missing/invalid enum, scope or date rejected before Mapper。
- Standards impact: MC-ARCH-001, MC-SCOPE-001, MC-TEST-001, MC-VALID-001；按已选传统分层，输入校验与权限先于状态读取，PO不出边界，相关日志脱敏。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 6, Rule 10, Rule 11；此文件直接按所列原始规则落实命名、验证、类型/注入或配置，其他规则由同Step关联文件负责。
- Implementation pseudocode:

```java
INPUT -> outbound business values only; missing state failclosed; tenant from TrustedRealmBO。
FLOW -> class OAuthSecurityStateBO { business fields from OAuthSecurityStatePO; id,version; } @Data @NoArgsConstructor @AllArgsConstructor @Accessors(chain=true) @Builder; @Valid groups Create/Update/Query
ON INVALID/CONCURRENT/DEPENDENCY FAILURE -> missing/invalid enum, scope or date rejected before Mapper。
RESULT -> only the named symbol/contract in this File block becomes available; preserve tenant, expectedVersion, scope, UTC time and secret masking stated above.
```

- Verification contribution: 本Step `mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthSigningKeyPersistenceContractTest -Dsurefire.failIfNoSpecifiedTests=false test`；此文件负责 OAuthSecurityStateBO 的正例与 missing/invalid enum, scope or date rejected before Mapper 负例。
- After this file: OAuthSecurityStateBO 的字段/入口或注册已落盘；与下一文件组合后本Step才能GREEN，不能把中间状态当完成。

#### File 10 — `CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/repository/OAuthSecurityStateRepository.java`

- Purpose: Service-facing typed contract。
- Symbols: `OAuthSecurityStateRepository`。
- Repository evidence: 当前不存在；Spec §8/§11 明确目标，邻近包可验证；目标源于主Spec §7–§16。
- Dependencies and consumers: 输入依赖 Step 5 与本Step此前文件；输出供 这组认证事实的Repository/Mapper能按同realm/tenant/active/version读写及后一Step使用；Java Bean/Mapper按模块现有配置发现，前端按现有auth client消费。
- Why now: 前一文件建立本文件需要的字段或调用契约； Service-facing typed contract在此位置后才可接消费者。
- Contract/signature changes: OAuthSecurityStateRepository；Service-facing typed contract。公开行为只取主Spec §9对应API或§10/11内部合同。
- Input/output and state mapping: Service uses BO/Query/Command; no PO generic outward。
- Error and edge behavior: 0 affected row → typed conflict; no bypass tenant。
- Standards impact: MC-ARCH-001, MC-SCOPE-001, MC-TEST-001, MC-VALID-001；按已选传统分层，输入校验与权限先于状态读取，PO不出边界，相关日志脱敏。
- Literal rule enforcement: Rule 1, Rule 2, Rule 11；此文件直接按所列原始规则落实命名、验证、类型/注入或配置，其他规则由同Step关联文件负责。
- Implementation pseudocode:

```java
INPUT -> Service uses BO/Query/Command; no PO generic outward。
FLOW -> @Validated interface OAuthSecurityStateRepository { Optional<OAuthSecurityStateBO> find(TrustedRealmBO realm, ...); OAuthSecurityStateBO save(@Valid OAuthSecurityStateBO input,long expectedVersion); } names agree §11 access tenant+targetType+targetKey exact/version
ON INVALID/CONCURRENT/DEPENDENCY FAILURE -> 0 affected row → typed conflict; no bypass tenant。
RESULT -> only the named symbol/contract in this File block becomes available; preserve tenant, expectedVersion, scope, UTC time and secret masking stated above.
```

- Verification contribution: 本Step `mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthSigningKeyPersistenceContractTest -Dsurefire.failIfNoSpecifiedTests=false test`；此文件负责 OAuthSecurityStateRepository 的正例与 0 affected row → typed conflict; no bypass tenant 负例。
- After this file: OAuthSecurityStateRepository 的字段/入口或注册已落盘；与下一文件组合后本Step才能GREEN，不能把中间状态当完成。

#### File 11 — `CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/domain/po/OAuthRevocationPO.java`

- Purpose: EgonModel承载本表完整列。
- Symbols: `OAuthRevocationPO`。
- Repository evidence: 当前不存在；Spec §8/§11 明确目标，邻近包可验证；目标源于主Spec §7–§16。
- Dependencies and consumers: 输入依赖 Step 5 与本Step此前文件；输出供 这组认证事实的Repository/Mapper能按同realm/tenant/active/version读写及后一Step使用；Java Bean/Mapper按模块现有配置发现，前端按现有auth client消费。
- Why now: 前一文件建立本文件需要的字段或调用契约； EgonModel承载本表完整列在此位置后才可接消费者。
- Contract/signature changes: OAuthRevocationPO；EgonModel承载本表完整列。公开行为只取主Spec §9对应API或§10/11内部合同。
- Input/output and state mapping: 11.2 oauth_revocation native columns snake_case→camelCase; inherited id/tenant/audit/deletedAt/version only一次。
- Error and edge behavior: UNIQUE/NULL/tenant违反时映射typed conflict；不可向HTTP返回PO。
- Standards impact: MC-ARCH-001, MC-SCOPE-001, MC-TEST-001, MC-VALID-001；按已选传统分层，输入校验与权限先于状态读取，PO不出边界，相关日志脱敏。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 6, Rule 10, Rule 11；此文件直接按所列原始规则落实命名、验证、类型/注入或配置，其他规则由同Step关联文件负责。
- Implementation pseudocode:

```java
INPUT -> 11.2 oauth_revocation native columns snake_case→camelCase; inherited id/tenant/audit/deletedAt/version only一次。
FLOW -> @TableName("oauth_revocation") class OAuthRevocationPO extends EgonModel<OAuthRevocationPO>; §11 oauth_revocation business fields; @Data/@NoArgsConstructor/@AllArgsConstructor/@Accessors(chain=true)/@SuperBuilder/@EqualsAndHashCode(callSuper=true); @EnumValue/secret exclude
ON INVALID/CONCURRENT/DEPENDENCY FAILURE -> UNIQUE/NULL/tenant违反时映射typed conflict；不可向HTTP返回PO。
RESULT -> only the named symbol/contract in this File block becomes available; preserve tenant, expectedVersion, scope, UTC time and secret masking stated above.
```

- Verification contribution: 本Step `mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthSigningKeyPersistenceContractTest -Dsurefire.failIfNoSpecifiedTests=false test`；此文件负责 OAuthRevocationPO 的正例与 UNIQUE/NULL/tenant违反时映射typed conflict；不可向HTTP返回PO 负例。
- After this file: OAuthRevocationPO 的字段/入口或注册已落盘；与下一文件组合后本Step才能GREEN，不能把中间状态当完成。

#### File 12 — `CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/domain/bo/OAuthRevocationBO.java`

- Purpose: Service计算与PO分界。
- Symbols: `OAuthRevocationBO`。
- Repository evidence: 当前不存在；Spec §8/§11 明确目标，邻近包可验证；目标源于主Spec §7–§16。
- Dependencies and consumers: 输入依赖 Step 5 与本Step此前文件；输出供 这组认证事实的Repository/Mapper能按同realm/tenant/active/version读写及后一Step使用；Java Bean/Mapper按模块现有配置发现，前端按现有auth client消费。
- Why now: 前一文件建立本文件需要的字段或调用契约； Service计算与PO分界在此位置后才可接消费者。
- Contract/signature changes: OAuthRevocationBO；Service计算与PO分界。公开行为只取主Spec §9对应API或§10/11内部合同。
- Input/output and state mapping: outbound business values only; idempotent revision and expiry; tenant from TrustedRealmBO。
- Error and edge behavior: missing/invalid enum, scope or date rejected before Mapper。
- Standards impact: MC-ARCH-001, MC-SCOPE-001, MC-TEST-001, MC-VALID-001；按已选传统分层，输入校验与权限先于状态读取，PO不出边界，相关日志脱敏。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 6, Rule 10, Rule 11；此文件直接按所列原始规则落实命名、验证、类型/注入或配置，其他规则由同Step关联文件负责。
- Implementation pseudocode:

```java
INPUT -> outbound business values only; idempotent revision and expiry; tenant from TrustedRealmBO。
FLOW -> class OAuthRevocationBO { business fields from OAuthRevocationPO; id,version; } @Data @NoArgsConstructor @AllArgsConstructor @Accessors(chain=true) @Builder; @Valid groups Create/Update/Query
ON INVALID/CONCURRENT/DEPENDENCY FAILURE -> missing/invalid enum, scope or date rejected before Mapper。
RESULT -> only the named symbol/contract in this File block becomes available; preserve tenant, expectedVersion, scope, UTC time and secret masking stated above.
```

- Verification contribution: 本Step `mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthSigningKeyPersistenceContractTest -Dsurefire.failIfNoSpecifiedTests=false test`；此文件负责 OAuthRevocationBO 的正例与 missing/invalid enum, scope or date rejected before Mapper 负例。
- After this file: OAuthRevocationBO 的字段/入口或注册已落盘；与下一文件组合后本Step才能GREEN，不能把中间状态当完成。

#### File 13 — `CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/repository/OAuthRevocationRepository.java`

- Purpose: Service-facing typed contract。
- Symbols: `OAuthRevocationRepository`。
- Repository evidence: 当前不存在；Spec §8/§11 明确目标，邻近包可验证；目标源于主Spec §7–§16。
- Dependencies and consumers: 输入依赖 Step 5 与本Step此前文件；输出供 这组认证事实的Repository/Mapper能按同realm/tenant/active/version读写及后一Step使用；Java Bean/Mapper按模块现有配置发现，前端按现有auth client消费。
- Why now: 前一文件建立本文件需要的字段或调用契约； Service-facing typed contract在此位置后才可接消费者。
- Contract/signature changes: OAuthRevocationRepository；Service-facing typed contract。公开行为只取主Spec §9对应API或§10/11内部合同。
- Input/output and state mapping: Service uses BO/Query/Command; no PO generic outward。
- Error and edge behavior: 0 affected row → typed conflict; no bypass tenant。
- Standards impact: MC-ARCH-001, MC-SCOPE-001, MC-TEST-001, MC-VALID-001；按已选传统分层，输入校验与权限先于状态读取，PO不出边界，相关日志脱敏。
- Literal rule enforcement: Rule 1, Rule 2, Rule 11；此文件直接按所列原始规则落实命名、验证、类型/注入或配置，其他规则由同Step关联文件负责。
- Implementation pseudocode:

```java
INPUT -> Service uses BO/Query/Command; no PO generic outward。
FLOW -> @Validated interface OAuthRevocationRepository { Optional<OAuthRevocationBO> find(TrustedRealmBO realm, ...); OAuthRevocationBO save(@Valid OAuthRevocationBO input,long expectedVersion); } names agree §11 access tenant+targetType+targetKey/revision
ON INVALID/CONCURRENT/DEPENDENCY FAILURE -> 0 affected row → typed conflict; no bypass tenant。
RESULT -> only the named symbol/contract in this File block becomes available; preserve tenant, expectedVersion, scope, UTC time and secret masking stated above.
```

- Verification contribution: 本Step `mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthSigningKeyPersistenceContractTest -Dsurefire.failIfNoSpecifiedTests=false test`；此文件负责 OAuthRevocationRepository 的正例与 0 affected row → typed conflict; no bypass tenant 负例。
- After this file: OAuthRevocationRepository 的字段/入口或注册已落盘；与下一文件组合后本Step才能GREEN，不能把中间状态当完成。

#### File 14 — `CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/audit/domain/po/OAuthAuditPO.java`

- Purpose: EgonModel承载本表完整列。
- Symbols: `OAuthAuditPO`。
- Repository evidence: 当前不存在；Spec §8/§11 明确目标，邻近包可验证；目标源于主Spec §7–§16。
- Dependencies and consumers: 输入依赖 Step 5 与本Step此前文件；输出供 这组认证事实的Repository/Mapper能按同realm/tenant/active/version读写及后一Step使用；Java Bean/Mapper按模块现有配置发现，前端按现有auth client消费。
- Why now: 前一文件建立本文件需要的字段或调用契约； EgonModel承载本表完整列在此位置后才可接消费者。
- Contract/signature changes: OAuthAuditPO；EgonModel承载本表完整列。公开行为只取主Spec §9对应API或§10/11内部合同。
- Input/output and state mapping: 11.2 oauth_audit native columns snake_case→camelCase; inherited id/tenant/audit/deletedAt/version only一次。
- Error and edge behavior: UNIQUE/NULL/tenant违反时映射typed conflict；不可向HTTP返回PO。
- Standards impact: MC-ARCH-001, MC-SCOPE-001, MC-TEST-001, MC-VALID-001；按已选传统分层，输入校验与权限先于状态读取，PO不出边界，相关日志脱敏。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 6, Rule 10, Rule 11；此文件直接按所列原始规则落实命名、验证、类型/注入或配置，其他规则由同Step关联文件负责。
- Implementation pseudocode:

```java
INPUT -> 11.2 oauth_audit native columns snake_case→camelCase; inherited id/tenant/audit/deletedAt/version only一次。
FLOW -> @TableName("oauth_audit") class OAuthAuditPO extends EgonModel<OAuthAuditPO>; §11 oauth_audit business fields; @Data/@NoArgsConstructor/@AllArgsConstructor/@Accessors(chain=true)/@SuperBuilder/@EqualsAndHashCode(callSuper=true); @EnumValue/secret exclude
ON INVALID/CONCURRENT/DEPENDENCY FAILURE -> UNIQUE/NULL/tenant违反时映射typed conflict；不可向HTTP返回PO。
RESULT -> only the named symbol/contract in this File block becomes available; preserve tenant, expectedVersion, scope, UTC time and secret masking stated above.
```

- Verification contribution: 本Step `mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthSigningKeyPersistenceContractTest -Dsurefire.failIfNoSpecifiedTests=false test`；此文件负责 OAuthAuditPO 的正例与 UNIQUE/NULL/tenant违反时映射typed conflict；不可向HTTP返回PO 负例。
- After this file: OAuthAuditPO 的字段/入口或注册已落盘；与下一文件组合后本Step才能GREEN，不能把中间状态当完成。

#### File 15 — `CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/audit/domain/bo/OAuthAuditBO.java`

- Purpose: Service计算与PO分界。
- Symbols: `OAuthAuditBO`。
- Repository evidence: 当前不存在；Spec §8/§11 明确目标，邻近包可验证；目标源于主Spec §7–§16。
- Dependencies and consumers: 输入依赖 Step 5 与本Step此前文件；输出供 这组认证事实的Repository/Mapper能按同realm/tenant/active/version读写及后一Step使用；Java Bean/Mapper按模块现有配置发现，前端按现有auth client消费。
- Why now: 前一文件建立本文件需要的字段或调用契约； Service计算与PO分界在此位置后才可接消费者。
- Contract/signature changes: OAuthAuditBO；Service计算与PO分界。公开行为只取主Spec §9对应API或§10/11内部合同。
- Input/output and state mapping: outbound business values only; all token events, no raw token; tenant from TrustedRealmBO。
- Error and edge behavior: missing/invalid enum, scope or date rejected before Mapper。
- Standards impact: MC-ARCH-001, MC-SCOPE-001, MC-TEST-001, MC-VALID-001；按已选传统分层，输入校验与权限先于状态读取，PO不出边界，相关日志脱敏。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 6, Rule 10, Rule 11；此文件直接按所列原始规则落实命名、验证、类型/注入或配置，其他规则由同Step关联文件负责。
- Implementation pseudocode:

```java
INPUT -> outbound business values only; all token events, no raw token; tenant from TrustedRealmBO。
FLOW -> class OAuthAuditBO { business fields from OAuthAuditPO; id,version; } @Data @NoArgsConstructor @AllArgsConstructor @Accessors(chain=true) @Builder; @Valid groups Create/Update/Query
ON INVALID/CONCURRENT/DEPENDENCY FAILURE -> missing/invalid enum, scope or date rejected before Mapper。
RESULT -> only the named symbol/contract in this File block becomes available; preserve tenant, expectedVersion, scope, UTC time and secret masking stated above.
```

- Verification contribution: 本Step `mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthSigningKeyPersistenceContractTest -Dsurefire.failIfNoSpecifiedTests=false test`；此文件负责 OAuthAuditBO 的正例与 missing/invalid enum, scope or date rejected before Mapper 负例。
- After this file: OAuthAuditBO 的字段/入口或注册已落盘；与下一文件组合后本Step才能GREEN，不能把中间状态当完成。

#### File 16 — `CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/audit/repository/OAuthAuditRepository.java`

- Purpose: Service-facing typed contract。
- Symbols: `OAuthAuditRepository`。
- Repository evidence: 当前不存在；Spec §8/§11 明确目标，邻近包可验证；目标源于主Spec §7–§16。
- Dependencies and consumers: 输入依赖 Step 5 与本Step此前文件；输出供 这组认证事实的Repository/Mapper能按同realm/tenant/active/version读写及后一Step使用；Java Bean/Mapper按模块现有配置发现，前端按现有auth client消费。
- Why now: 前一文件建立本文件需要的字段或调用契约； Service-facing typed contract在此位置后才可接消费者。
- Contract/signature changes: OAuthAuditRepository；Service-facing typed contract。公开行为只取主Spec §9对应API或§10/11内部合同。
- Input/output and state mapping: Service uses BO/Query/Command; no PO generic outward。
- Error and edge behavior: 0 affected row → typed conflict; no bypass tenant。
- Standards impact: MC-ARCH-001, MC-SCOPE-001, MC-TEST-001, MC-VALID-001；按已选传统分层，输入校验与权限先于状态读取，PO不出边界，相关日志脱敏。
- Literal rule enforcement: Rule 1, Rule 2, Rule 11；此文件直接按所列原始规则落实命名、验证、类型/注入或配置，其他规则由同Step关联文件负责。
- Implementation pseudocode:

```java
INPUT -> Service uses BO/Query/Command; no PO generic outward。
FLOW -> @Validated interface OAuthAuditRepository { Optional<OAuthAuditBO> find(TrustedRealmBO realm, ...); OAuthAuditBO save(@Valid OAuthAuditBO input,long expectedVersion); } names agree §11 access tenant+eventId or occurredAt,id cursor
ON INVALID/CONCURRENT/DEPENDENCY FAILURE -> 0 affected row → typed conflict; no bypass tenant。
RESULT -> only the named symbol/contract in this File block becomes available; preserve tenant, expectedVersion, scope, UTC time and secret masking stated above.
```

- Verification contribution: 本Step `mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthSigningKeyPersistenceContractTest -Dsurefire.failIfNoSpecifiedTests=false test`；此文件负责 OAuthAuditRepository 的正例与 0 affected row → typed conflict; no bypass tenant 负例。
- After this file: OAuthAuditRepository 的字段/入口或注册已落盘；与下一文件组合后本Step才能GREEN，不能把中间状态当完成。

#### File 17 — `CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/repository/OAuthSigningKeyPersistenceContractTest.java`

- Purpose: RED证明这组映射/查询/跨租户错误。
- Symbols: `OAuthSigningKeyPersistenceContractTest`。
- Repository evidence: 当前不存在；Spec §8/§11 明确目标，邻近包可验证；目标源于主Spec §7–§16。
- Dependencies and consumers: 输入依赖 Step 5 与本Step此前文件；输出供 这组认证事实的Repository/Mapper能按同realm/tenant/active/version读写及后一Step使用；Java Bean/Mapper按模块现有配置发现，前端按现有auth client消费。
- Why now: 前一文件建立本文件需要的字段或调用契约； RED证明这组映射/查询/跨租户错误在此位置后才可接消费者。
- Contract/signature changes: OAuthSigningKeyPersistenceContractTest；RED证明这组映射/查询/跨租户错误。公开行为只取主Spec §9对应API或§10/11内部合同。
- Input/output and state mapping: fixed tenant=1001/1002; timestamps Instant/UTC; SQL actual node tianquan_oauth。
- Error and edge behavior: no implementation Bean/Mapper yet → focused contract RED，不能因fixture fail。
- Standards impact: MC-ARCH-001, MC-SCOPE-001, MC-TEST-001, MC-VALID-001；按已选传统分层，输入校验与权限先于状态读取，PO不出边界，相关日志脱敏。
- Literal rule enforcement: Rule 2, Rule 3, Rule 11；此文件直接按所列原始规则落实命名、验证、类型/注入或配置，其他规则由同Step关联文件负责。
- Implementation pseudocode:

```java
INPUT -> fixed tenant=1001/1002; timestamps Instant/UTC; SQL actual node tianquan_oauth。
FLOW -> @Test for OAuthSigningKey/OAuthSecurityState/OAuthRevocation/OAuthAudit: assert same external client/resource key in different trusted tenants does not cross; query includes tenant and deleted_at IS NULL; concurrent expectedVersion loser fails; secret/ciphertext absent from VO
ON INVALID/CONCURRENT/DEPENDENCY FAILURE -> no implementation Bean/Mapper yet → focused contract RED，不能因fixture fail。
RESULT -> only the named symbol/contract in this File block becomes available; preserve tenant, expectedVersion, scope, UTC time and secret masking stated above.
```

- Verification contribution: 本Step `mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthSigningKeyPersistenceContractTest -Dsurefire.failIfNoSpecifiedTests=false test`；此文件负责 OAuthSigningKeyPersistenceContractTest 的正例与 no implementation Bean/Mapper yet → focused contract RED，不能因fixture fail 负例。
- After this file: OAuthSigningKeyPersistenceContractTest 的字段/入口或注册已落盘；与下一文件组合后本Step才能GREEN，不能把中间状态当完成。

#### File 18 — `CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/domain/converter/OAuthSigningKeyConverter.java`

- Purpose: PO↔BO双向MapStruct转换。
- Symbols: `OAuthSigningKeyConverter`。
- Repository evidence: 当前不存在；Spec §8/§11 明确目标，邻近包可验证；目标源于主Spec §7–§16。
- Dependencies and consumers: 输入依赖 Step 5 与本Step此前文件；输出供 这组认证事实的Repository/Mapper能按同realm/tenant/active/version读写及后一Step使用；Java Bean/Mapper按模块现有配置发现，前端按现有auth client消费。
- Why now: 前一文件建立本文件需要的字段或调用契约； PO↔BO双向MapStruct转换在此位置后才可接消费者。
- Contract/signature changes: OAuthSigningKeyConverter；PO↔BO双向MapStruct转换。公开行为只取主Spec §9对应API或§10/11内部合同。
- Input/output and state mapping: OAuthSigningKeyPO business fields from §11 → OAuthSigningKeyBO; unknown enum拒绝，null按列约束。
- Error and edge behavior: generated implementation/constructor must compile; no BeanUtils/json copy。
- Standards impact: MC-ARCH-001, MC-SCOPE-001, MC-TEST-001, MC-VALID-001；按已选传统分层，输入校验与权限先于状态读取，PO不出边界，相关日志脱敏。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 6, Rule 10, Rule 11；此文件直接按所列原始规则落实命名、验证、类型/注入或配置，其他规则由同Step关联文件负责。
- Implementation pseudocode:

```java
INPUT -> OAuthSigningKeyPO business fields from §11 → OAuthSigningKeyBO; unknown enum拒绝，null按列约束。
FLOW -> @Mapper(componentModel="spring") interface OAuthSigningKeyConverter extends BaseConverter<OAuthSigningKeyBO,OAuthSigningKeyPO>; @Mapping inherited tenant/audit fill ignored on insert; enum code explicit; secret excluded from toString/VO; mapping date Instant/UTC
ON INVALID/CONCURRENT/DEPENDENCY FAILURE -> generated implementation/constructor must compile; no BeanUtils/json copy。
RESULT -> only the named symbol/contract in this File block becomes available; preserve tenant, expectedVersion, scope, UTC time and secret masking stated above.
```

- Verification contribution: 本Step `mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthSigningKeyPersistenceContractTest -Dsurefire.failIfNoSpecifiedTests=false test`；此文件负责 OAuthSigningKeyConverter 的正例与 generated implementation/constructor must compile; no BeanUtils/json copy 负例。
- After this file: OAuthSigningKeyConverter 的字段/入口或注册已落盘；与下一文件组合后本Step才能GREEN，不能把中间状态当完成。

#### File 19 — `CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/dao/OAuthSigningKeyMapper.java`

- Purpose: MP Mapper具名SQL接口。
- Symbols: `OAuthSigningKeyMapper`。
- Repository evidence: 当前不存在；Spec §8/§11 明确目标，邻近包可验证；目标源于主Spec §7–§16。
- Dependencies and consumers: 输入依赖 Step 5 与本Step此前文件；输出供 这组认证事实的Repository/Mapper能按同realm/tenant/active/version读写及后一Step使用；Java Bean/Mapper按模块现有配置发现，前端按现有auth client消费。
- Why now: 前一文件建立本文件需要的字段或调用契约； MP Mapper具名SQL接口在此位置后才可接消费者。
- Contract/signature changes: OAuthSigningKeyMapper；MP Mapper具名SQL接口。公开行为只取主Spec §9对应API或§10/11内部合同。
- Input/output and state mapping: tenantId positive Long from verified MDC; result OAuthSigningKeyPO。
- Error and edge behavior: no LambdaQueryChain/Wrapper; missing tenant failclosed。
- Standards impact: MC-ARCH-001, MC-SCOPE-001, MC-TEST-001, MC-VALID-001；按已选传统分层，输入校验与权限先于状态读取，PO不出边界，相关日志脱敏。
- Literal rule enforcement: Rule 1, Rule 2, Rule 11；此文件直接按所列原始规则落实命名、验证、类型/注入或配置，其他规则由同Step关联文件负责。
- Implementation pseudocode:

```java
INPUT -> tenantId positive Long from verified MDC; result OAuthSigningKeyPO。
FLOW -> interface OAuthSigningKeyMapper extends EgonColaMapper<OAuthSigningKeyPO>; methods selectActiveById/Ids, deleteVersionedById, tenant+kid/status active SQL bindings
ON INVALID/CONCURRENT/DEPENDENCY FAILURE -> no LambdaQueryChain/Wrapper; missing tenant failclosed。
RESULT -> only the named symbol/contract in this File block becomes available; preserve tenant, expectedVersion, scope, UTC time and secret masking stated above.
```

- Verification contribution: 本Step `mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthSigningKeyPersistenceContractTest -Dsurefire.failIfNoSpecifiedTests=false test`；此文件负责 OAuthSigningKeyMapper 的正例与 no LambdaQueryChain/Wrapper; missing tenant failclosed 负例。
- After this file: OAuthSigningKeyMapper 的字段/入口或注册已落盘；与下一文件组合后本Step才能GREEN，不能把中间状态当完成。

#### File 20 — `CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/resources/mapper/token/OAuthSigningKeyMapper.xml`

- Purpose: 具名PostgreSQL谓词/锁。
- Symbols: `OAuthSigningKeyMapper SQL`。
- Repository evidence: 当前不存在；Spec §8/§11 明确目标，邻近包可验证；目标源于主Spec §7–§16。
- Dependencies and consumers: 输入依赖 Step 5 与本Step此前文件；输出供 这组认证事实的Repository/Mapper能按同realm/tenant/active/version读写及后一Step使用；Java Bean/Mapper按模块现有配置发现，前端按现有auth client消费。
- Why now: 前一文件建立本文件需要的字段或调用契约； 具名PostgreSQL谓词/锁在此位置后才可接消费者。
- Contract/signature changes: OAuthSigningKeyMapper SQL；具名PostgreSQL谓词/锁。公开行为只取主Spec §9对应API或§10/11内部合同。
- Input/output and state mapping: 0/1 precise query or capped list; indexes §11。
- Error and edge behavior: tenant range forbidden, 0 rows conflict, no cross-group write。
- Standards impact: MC-SCOPE-001, MC-TEST-001；按已选传统分层，输入校验与权限先于状态读取，PO不出边界，相关日志脱敏。
- Literal rule enforcement: Rule 2, Rule 10, Rule 11；此文件直接按所列原始规则落实命名、验证、类型/注入或配置，其他规则由同Step关联文件负责。
- Implementation pseudocode:

```xml
INPUT -> 0/1 precise query or capped list; indexes §11。
FLOW -> SELECT business columns + inherited fields FROM tianquan_oauth.oauth_signing_key WHERE tenant_id=#{tenantId} AND deleted_at IS NULL AND tenant+kid/status active; guarded UPDATE id+tenant+version+expected state; SELECT FOR UPDATE only command path; max page bounded
ON INVALID/CONCURRENT/DEPENDENCY FAILURE -> tenant range forbidden, 0 rows conflict, no cross-group write。
RESULT -> only the named symbol/contract in this File block becomes available; preserve tenant, expectedVersion, scope, UTC time and secret masking stated above.
```

- Verification contribution: 本Step `mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthSigningKeyPersistenceContractTest -Dsurefire.failIfNoSpecifiedTests=false test`；此文件负责 OAuthSigningKeyMapper SQL 的正例与 tenant range forbidden, 0 rows conflict, no cross-group write 负例。
- After this file: OAuthSigningKeyMapper SQL 的字段/入口或注册已落盘；与下一文件组合后本Step才能GREEN，不能把中间状态当完成。

#### File 21 — `CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/repository/impl/OAuthSigningKeyRepositoryImpl.java`

- Purpose: MP repository ACL。
- Symbols: `OAuthSigningKeyRepositoryImpl`。
- Repository evidence: 当前不存在；Spec §8/§11 明确目标，邻近包可验证；目标源于主Spec §7–§16。
- Dependencies and consumers: 输入依赖 Step 5 与本Step此前文件；输出供 这组认证事实的Repository/Mapper能按同realm/tenant/active/version读写及后一Step使用；Java Bean/Mapper按模块现有配置发现，前端按现有auth client消费。
- Why now: 前一文件建立本文件需要的字段或调用契约； MP repository ACL在此位置后才可接消费者。
- Contract/signature changes: OAuthSigningKeyRepositoryImpl；MP repository ACL。公开行为只取主Spec §9对应API或§10/11内部合同。
- Input/output and state mapping: OAuthSigningKeyBO↔PO; raw secret/private only protected internal fields。
- Error and edge behavior: UnsupportedOperationException for generic unscoped query; errors mapped without sensitive values。
- Standards impact: MC-ARCH-001, MC-SCOPE-001, MC-TEST-001, MC-VALID-001；按已选传统分层，输入校验与权限先于状态读取，PO不出边界，相关日志脱敏。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 6, Rule 9, Rule 10, Rule 11；此文件直接按所列原始规则落实命名、验证、类型/注入或配置，其他规则由同Step关联文件负责。
- Implementation pseudocode:

```java
INPUT -> OAuthSigningKeyBO↔PO; raw secret/private only protected internal fields。
FLOW -> @Repository("oAuthSigningKeyRepository") @Slf4j @RequiredArgsConstructor; final @Qualifier("oAuthSigningKeyMapper") Mapper, converter, modelValidationUtils, properties; extends EgonColaRepository<Mapper,PO>; find maps PO→BO; save maps BO→PO, guarded version update result==1
ON INVALID/CONCURRENT/DEPENDENCY FAILURE -> UnsupportedOperationException for generic unscoped query; errors mapped without sensitive values。
RESULT -> only the named symbol/contract in this File block becomes available; preserve tenant, expectedVersion, scope, UTC time and secret masking stated above.
```

- Verification contribution: 本Step `mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthSigningKeyPersistenceContractTest -Dsurefire.failIfNoSpecifiedTests=false test`；此文件负责 OAuthSigningKeyRepositoryImpl 的正例与 UnsupportedOperationException for generic unscoped query; errors mapped without sensitive values 负例。
- After this file: OAuthSigningKeyRepositoryImpl 的字段/入口或注册已落盘；与下一文件组合后本Step才能GREEN，不能把中间状态当完成。

#### File 22 — `CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/domain/converter/OAuthSecurityStateConverter.java`

- Purpose: PO↔BO双向MapStruct转换。
- Symbols: `OAuthSecurityStateConverter`。
- Repository evidence: 当前不存在；Spec §8/§11 明确目标，邻近包可验证；目标源于主Spec §7–§16。
- Dependencies and consumers: 输入依赖 Step 5 与本Step此前文件；输出供 这组认证事实的Repository/Mapper能按同realm/tenant/active/version读写及后一Step使用；Java Bean/Mapper按模块现有配置发现，前端按现有auth client消费。
- Why now: 前一文件建立本文件需要的字段或调用契约； PO↔BO双向MapStruct转换在此位置后才可接消费者。
- Contract/signature changes: OAuthSecurityStateConverter；PO↔BO双向MapStruct转换。公开行为只取主Spec §9对应API或§10/11内部合同。
- Input/output and state mapping: OAuthSecurityStatePO business fields from §11 → OAuthSecurityStateBO; unknown enum拒绝，null按列约束。
- Error and edge behavior: generated implementation/constructor must compile; no BeanUtils/json copy。
- Standards impact: MC-ARCH-001, MC-SCOPE-001, MC-TEST-001, MC-VALID-001；按已选传统分层，输入校验与权限先于状态读取，PO不出边界，相关日志脱敏。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 6, Rule 10, Rule 11；此文件直接按所列原始规则落实命名、验证、类型/注入或配置，其他规则由同Step关联文件负责。
- Implementation pseudocode:

```java
INPUT -> OAuthSecurityStatePO business fields from §11 → OAuthSecurityStateBO; unknown enum拒绝，null按列约束。
FLOW -> @Mapper(componentModel="spring") interface OAuthSecurityStateConverter extends BaseConverter<OAuthSecurityStateBO,OAuthSecurityStatePO>; @Mapping inherited tenant/audit fill ignored on insert; enum code explicit; secret excluded from toString/VO; mapping date Instant/UTC
ON INVALID/CONCURRENT/DEPENDENCY FAILURE -> generated implementation/constructor must compile; no BeanUtils/json copy。
RESULT -> only the named symbol/contract in this File block becomes available; preserve tenant, expectedVersion, scope, UTC time and secret masking stated above.
```

- Verification contribution: 本Step `mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthSigningKeyPersistenceContractTest -Dsurefire.failIfNoSpecifiedTests=false test`；此文件负责 OAuthSecurityStateConverter 的正例与 generated implementation/constructor must compile; no BeanUtils/json copy 负例。
- After this file: OAuthSecurityStateConverter 的字段/入口或注册已落盘；与下一文件组合后本Step才能GREEN，不能把中间状态当完成。

#### File 23 — `CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/dao/OAuthSecurityStateMapper.java`

- Purpose: MP Mapper具名SQL接口。
- Symbols: `OAuthSecurityStateMapper`。
- Repository evidence: 当前不存在；Spec §8/§11 明确目标，邻近包可验证；目标源于主Spec §7–§16。
- Dependencies and consumers: 输入依赖 Step 5 与本Step此前文件；输出供 这组认证事实的Repository/Mapper能按同realm/tenant/active/version读写及后一Step使用；Java Bean/Mapper按模块现有配置发现，前端按现有auth client消费。
- Why now: 前一文件建立本文件需要的字段或调用契约； MP Mapper具名SQL接口在此位置后才可接消费者。
- Contract/signature changes: OAuthSecurityStateMapper；MP Mapper具名SQL接口。公开行为只取主Spec §9对应API或§10/11内部合同。
- Input/output and state mapping: tenantId positive Long from verified MDC; result OAuthSecurityStatePO。
- Error and edge behavior: no LambdaQueryChain/Wrapper; missing tenant failclosed。
- Standards impact: MC-ARCH-001, MC-SCOPE-001, MC-TEST-001, MC-VALID-001；按已选传统分层，输入校验与权限先于状态读取，PO不出边界，相关日志脱敏。
- Literal rule enforcement: Rule 1, Rule 2, Rule 11；此文件直接按所列原始规则落实命名、验证、类型/注入或配置，其他规则由同Step关联文件负责。
- Implementation pseudocode:

```java
INPUT -> tenantId positive Long from verified MDC; result OAuthSecurityStatePO。
FLOW -> interface OAuthSecurityStateMapper extends EgonColaMapper<OAuthSecurityStatePO>; methods selectActiveById/Ids, deleteVersionedById, tenant+targetType+targetKey exact/version SQL bindings
ON INVALID/CONCURRENT/DEPENDENCY FAILURE -> no LambdaQueryChain/Wrapper; missing tenant failclosed。
RESULT -> only the named symbol/contract in this File block becomes available; preserve tenant, expectedVersion, scope, UTC time and secret masking stated above.
```

- Verification contribution: 本Step `mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthSigningKeyPersistenceContractTest -Dsurefire.failIfNoSpecifiedTests=false test`；此文件负责 OAuthSecurityStateMapper 的正例与 no LambdaQueryChain/Wrapper; missing tenant failclosed 负例。
- After this file: OAuthSecurityStateMapper 的字段/入口或注册已落盘；与下一文件组合后本Step才能GREEN，不能把中间状态当完成。

#### File 24 — `CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/resources/mapper/token/OAuthSecurityStateMapper.xml`

- Purpose: 具名PostgreSQL谓词/锁。
- Symbols: `OAuthSecurityStateMapper SQL`。
- Repository evidence: 当前不存在；Spec §8/§11 明确目标，邻近包可验证；目标源于主Spec §7–§16。
- Dependencies and consumers: 输入依赖 Step 5 与本Step此前文件；输出供 这组认证事实的Repository/Mapper能按同realm/tenant/active/version读写及后一Step使用；Java Bean/Mapper按模块现有配置发现，前端按现有auth client消费。
- Why now: 前一文件建立本文件需要的字段或调用契约； 具名PostgreSQL谓词/锁在此位置后才可接消费者。
- Contract/signature changes: OAuthSecurityStateMapper SQL；具名PostgreSQL谓词/锁。公开行为只取主Spec §9对应API或§10/11内部合同。
- Input/output and state mapping: 0/1 precise query or capped list; indexes §11。
- Error and edge behavior: tenant range forbidden, 0 rows conflict, no cross-group write。
- Standards impact: MC-SCOPE-001, MC-TEST-001；按已选传统分层，输入校验与权限先于状态读取，PO不出边界，相关日志脱敏。
- Literal rule enforcement: Rule 2, Rule 10, Rule 11；此文件直接按所列原始规则落实命名、验证、类型/注入或配置，其他规则由同Step关联文件负责。
- Implementation pseudocode:

```xml
INPUT -> 0/1 precise query or capped list; indexes §11。
FLOW -> SELECT business columns + inherited fields FROM tianquan_oauth.oauth_security_state WHERE tenant_id=#{tenantId} AND deleted_at IS NULL AND tenant+targetType+targetKey exact/version; guarded UPDATE id+tenant+version+expected state; SELECT FOR UPDATE only command path; max page bounded
ON INVALID/CONCURRENT/DEPENDENCY FAILURE -> tenant range forbidden, 0 rows conflict, no cross-group write。
RESULT -> only the named symbol/contract in this File block becomes available; preserve tenant, expectedVersion, scope, UTC time and secret masking stated above.
```

- Verification contribution: 本Step `mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthSigningKeyPersistenceContractTest -Dsurefire.failIfNoSpecifiedTests=false test`；此文件负责 OAuthSecurityStateMapper SQL 的正例与 tenant range forbidden, 0 rows conflict, no cross-group write 负例。
- After this file: OAuthSecurityStateMapper SQL 的字段/入口或注册已落盘；与下一文件组合后本Step才能GREEN，不能把中间状态当完成。

#### File 25 — `CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/repository/impl/OAuthSecurityStateRepositoryImpl.java`

- Purpose: MP repository ACL。
- Symbols: `OAuthSecurityStateRepositoryImpl`。
- Repository evidence: 当前不存在；Spec §8/§11 明确目标，邻近包可验证；目标源于主Spec §7–§16。
- Dependencies and consumers: 输入依赖 Step 5 与本Step此前文件；输出供 这组认证事实的Repository/Mapper能按同realm/tenant/active/version读写及后一Step使用；Java Bean/Mapper按模块现有配置发现，前端按现有auth client消费。
- Why now: 前一文件建立本文件需要的字段或调用契约； MP repository ACL在此位置后才可接消费者。
- Contract/signature changes: OAuthSecurityStateRepositoryImpl；MP repository ACL。公开行为只取主Spec §9对应API或§10/11内部合同。
- Input/output and state mapping: OAuthSecurityStateBO↔PO; raw secret/private only protected internal fields。
- Error and edge behavior: UnsupportedOperationException for generic unscoped query; errors mapped without sensitive values。
- Standards impact: MC-ARCH-001, MC-SCOPE-001, MC-TEST-001, MC-VALID-001；按已选传统分层，输入校验与权限先于状态读取，PO不出边界，相关日志脱敏。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 6, Rule 9, Rule 10, Rule 11；此文件直接按所列原始规则落实命名、验证、类型/注入或配置，其他规则由同Step关联文件负责。
- Implementation pseudocode:

```java
INPUT -> OAuthSecurityStateBO↔PO; raw secret/private only protected internal fields。
FLOW -> @Repository("oAuthSecurityStateRepository") @Slf4j @RequiredArgsConstructor; final @Qualifier("oAuthSecurityStateMapper") Mapper, converter, modelValidationUtils, properties; extends EgonColaRepository<Mapper,PO>; find maps PO→BO; save maps BO→PO, guarded version update result==1
ON INVALID/CONCURRENT/DEPENDENCY FAILURE -> UnsupportedOperationException for generic unscoped query; errors mapped without sensitive values。
RESULT -> only the named symbol/contract in this File block becomes available; preserve tenant, expectedVersion, scope, UTC time and secret masking stated above.
```

- Verification contribution: 本Step `mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthSigningKeyPersistenceContractTest -Dsurefire.failIfNoSpecifiedTests=false test`；此文件负责 OAuthSecurityStateRepositoryImpl 的正例与 UnsupportedOperationException for generic unscoped query; errors mapped without sensitive values 负例。
- After this file: OAuthSecurityStateRepositoryImpl 的字段/入口或注册已落盘；与下一文件组合后本Step才能GREEN，不能把中间状态当完成。

#### File 26 — `CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/domain/converter/OAuthRevocationConverter.java`

- Purpose: PO↔BO双向MapStruct转换。
- Symbols: `OAuthRevocationConverter`。
- Repository evidence: 当前不存在；Spec §8/§11 明确目标，邻近包可验证；目标源于主Spec §7–§16。
- Dependencies and consumers: 输入依赖 Step 5 与本Step此前文件；输出供 这组认证事实的Repository/Mapper能按同realm/tenant/active/version读写及后一Step使用；Java Bean/Mapper按模块现有配置发现，前端按现有auth client消费。
- Why now: 前一文件建立本文件需要的字段或调用契约； PO↔BO双向MapStruct转换在此位置后才可接消费者。
- Contract/signature changes: OAuthRevocationConverter；PO↔BO双向MapStruct转换。公开行为只取主Spec §9对应API或§10/11内部合同。
- Input/output and state mapping: OAuthRevocationPO business fields from §11 → OAuthRevocationBO; unknown enum拒绝，null按列约束。
- Error and edge behavior: generated implementation/constructor must compile; no BeanUtils/json copy。
- Standards impact: MC-ARCH-001, MC-SCOPE-001, MC-TEST-001, MC-VALID-001；按已选传统分层，输入校验与权限先于状态读取，PO不出边界，相关日志脱敏。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 6, Rule 10, Rule 11；此文件直接按所列原始规则落实命名、验证、类型/注入或配置，其他规则由同Step关联文件负责。
- Implementation pseudocode:

```java
INPUT -> OAuthRevocationPO business fields from §11 → OAuthRevocationBO; unknown enum拒绝，null按列约束。
FLOW -> @Mapper(componentModel="spring") interface OAuthRevocationConverter extends BaseConverter<OAuthRevocationBO,OAuthRevocationPO>; @Mapping inherited tenant/audit fill ignored on insert; enum code explicit; secret excluded from toString/VO; mapping date Instant/UTC
ON INVALID/CONCURRENT/DEPENDENCY FAILURE -> generated implementation/constructor must compile; no BeanUtils/json copy。
RESULT -> only the named symbol/contract in this File block becomes available; preserve tenant, expectedVersion, scope, UTC time and secret masking stated above.
```

- Verification contribution: 本Step `mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthSigningKeyPersistenceContractTest -Dsurefire.failIfNoSpecifiedTests=false test`；此文件负责 OAuthRevocationConverter 的正例与 generated implementation/constructor must compile; no BeanUtils/json copy 负例。
- After this file: OAuthRevocationConverter 的字段/入口或注册已落盘；与下一文件组合后本Step才能GREEN，不能把中间状态当完成。

#### File 27 — `CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/dao/OAuthRevocationMapper.java`

- Purpose: MP Mapper具名SQL接口。
- Symbols: `OAuthRevocationMapper`。
- Repository evidence: 当前不存在；Spec §8/§11 明确目标，邻近包可验证；目标源于主Spec §7–§16。
- Dependencies and consumers: 输入依赖 Step 5 与本Step此前文件；输出供 这组认证事实的Repository/Mapper能按同realm/tenant/active/version读写及后一Step使用；Java Bean/Mapper按模块现有配置发现，前端按现有auth client消费。
- Why now: 前一文件建立本文件需要的字段或调用契约； MP Mapper具名SQL接口在此位置后才可接消费者。
- Contract/signature changes: OAuthRevocationMapper；MP Mapper具名SQL接口。公开行为只取主Spec §9对应API或§10/11内部合同。
- Input/output and state mapping: tenantId positive Long from verified MDC; result OAuthRevocationPO。
- Error and edge behavior: no LambdaQueryChain/Wrapper; missing tenant failclosed。
- Standards impact: MC-ARCH-001, MC-SCOPE-001, MC-TEST-001, MC-VALID-001；按已选传统分层，输入校验与权限先于状态读取，PO不出边界，相关日志脱敏。
- Literal rule enforcement: Rule 1, Rule 2, Rule 11；此文件直接按所列原始规则落实命名、验证、类型/注入或配置，其他规则由同Step关联文件负责。
- Implementation pseudocode:

```java
INPUT -> tenantId positive Long from verified MDC; result OAuthRevocationPO。
FLOW -> interface OAuthRevocationMapper extends EgonColaMapper<OAuthRevocationPO>; methods selectActiveById/Ids, deleteVersionedById, tenant+targetType+targetKey/revision SQL bindings
ON INVALID/CONCURRENT/DEPENDENCY FAILURE -> no LambdaQueryChain/Wrapper; missing tenant failclosed。
RESULT -> only the named symbol/contract in this File block becomes available; preserve tenant, expectedVersion, scope, UTC time and secret masking stated above.
```

- Verification contribution: 本Step `mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthSigningKeyPersistenceContractTest -Dsurefire.failIfNoSpecifiedTests=false test`；此文件负责 OAuthRevocationMapper 的正例与 no LambdaQueryChain/Wrapper; missing tenant failclosed 负例。
- After this file: OAuthRevocationMapper 的字段/入口或注册已落盘；与下一文件组合后本Step才能GREEN，不能把中间状态当完成。

#### File 28 — `CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/resources/mapper/token/OAuthRevocationMapper.xml`

- Purpose: 具名PostgreSQL谓词/锁。
- Symbols: `OAuthRevocationMapper SQL`。
- Repository evidence: 当前不存在；Spec §8/§11 明确目标，邻近包可验证；目标源于主Spec §7–§16。
- Dependencies and consumers: 输入依赖 Step 5 与本Step此前文件；输出供 这组认证事实的Repository/Mapper能按同realm/tenant/active/version读写及后一Step使用；Java Bean/Mapper按模块现有配置发现，前端按现有auth client消费。
- Why now: 前一文件建立本文件需要的字段或调用契约； 具名PostgreSQL谓词/锁在此位置后才可接消费者。
- Contract/signature changes: OAuthRevocationMapper SQL；具名PostgreSQL谓词/锁。公开行为只取主Spec §9对应API或§10/11内部合同。
- Input/output and state mapping: 0/1 precise query or capped list; indexes §11。
- Error and edge behavior: tenant range forbidden, 0 rows conflict, no cross-group write。
- Standards impact: MC-SCOPE-001, MC-TEST-001；按已选传统分层，输入校验与权限先于状态读取，PO不出边界，相关日志脱敏。
- Literal rule enforcement: Rule 2, Rule 10, Rule 11；此文件直接按所列原始规则落实命名、验证、类型/注入或配置，其他规则由同Step关联文件负责。
- Implementation pseudocode:

```xml
INPUT -> 0/1 precise query or capped list; indexes §11。
FLOW -> SELECT business columns + inherited fields FROM tianquan_oauth.oauth_revocation WHERE tenant_id=#{tenantId} AND deleted_at IS NULL AND tenant+targetType+targetKey/revision; guarded UPDATE id+tenant+version+expected state; SELECT FOR UPDATE only command path; max page bounded
ON INVALID/CONCURRENT/DEPENDENCY FAILURE -> tenant range forbidden, 0 rows conflict, no cross-group write。
RESULT -> only the named symbol/contract in this File block becomes available; preserve tenant, expectedVersion, scope, UTC time and secret masking stated above.
```

- Verification contribution: 本Step `mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthSigningKeyPersistenceContractTest -Dsurefire.failIfNoSpecifiedTests=false test`；此文件负责 OAuthRevocationMapper SQL 的正例与 tenant range forbidden, 0 rows conflict, no cross-group write 负例。
- After this file: OAuthRevocationMapper SQL 的字段/入口或注册已落盘；与下一文件组合后本Step才能GREEN，不能把中间状态当完成。

#### File 29 — `CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/repository/impl/OAuthRevocationRepositoryImpl.java`

- Purpose: MP repository ACL。
- Symbols: `OAuthRevocationRepositoryImpl`。
- Repository evidence: 当前不存在；Spec §8/§11 明确目标，邻近包可验证；目标源于主Spec §7–§16。
- Dependencies and consumers: 输入依赖 Step 5 与本Step此前文件；输出供 这组认证事实的Repository/Mapper能按同realm/tenant/active/version读写及后一Step使用；Java Bean/Mapper按模块现有配置发现，前端按现有auth client消费。
- Why now: 前一文件建立本文件需要的字段或调用契约； MP repository ACL在此位置后才可接消费者。
- Contract/signature changes: OAuthRevocationRepositoryImpl；MP repository ACL。公开行为只取主Spec §9对应API或§10/11内部合同。
- Input/output and state mapping: OAuthRevocationBO↔PO; raw secret/private only protected internal fields。
- Error and edge behavior: UnsupportedOperationException for generic unscoped query; errors mapped without sensitive values。
- Standards impact: MC-ARCH-001, MC-SCOPE-001, MC-TEST-001, MC-VALID-001；按已选传统分层，输入校验与权限先于状态读取，PO不出边界，相关日志脱敏。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 6, Rule 9, Rule 10, Rule 11；此文件直接按所列原始规则落实命名、验证、类型/注入或配置，其他规则由同Step关联文件负责。
- Implementation pseudocode:

```java
INPUT -> OAuthRevocationBO↔PO; raw secret/private only protected internal fields。
FLOW -> @Repository("oAuthRevocationRepository") @Slf4j @RequiredArgsConstructor; final @Qualifier("oAuthRevocationMapper") Mapper, converter, modelValidationUtils, properties; extends EgonColaRepository<Mapper,PO>; find maps PO→BO; save maps BO→PO, guarded version update result==1
ON INVALID/CONCURRENT/DEPENDENCY FAILURE -> UnsupportedOperationException for generic unscoped query; errors mapped without sensitive values。
RESULT -> only the named symbol/contract in this File block becomes available; preserve tenant, expectedVersion, scope, UTC time and secret masking stated above.
```

- Verification contribution: 本Step `mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthSigningKeyPersistenceContractTest -Dsurefire.failIfNoSpecifiedTests=false test`；此文件负责 OAuthRevocationRepositoryImpl 的正例与 UnsupportedOperationException for generic unscoped query; errors mapped without sensitive values 负例。
- After this file: OAuthRevocationRepositoryImpl 的字段/入口或注册已落盘；与下一文件组合后本Step才能GREEN，不能把中间状态当完成。

#### File 30 — `CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/audit/domain/converter/OAuthAuditConverter.java`

- Purpose: PO↔BO双向MapStruct转换。
- Symbols: `OAuthAuditConverter`。
- Repository evidence: 当前不存在；Spec §8/§11 明确目标，邻近包可验证；目标源于主Spec §7–§16。
- Dependencies and consumers: 输入依赖 Step 5 与本Step此前文件；输出供 这组认证事实的Repository/Mapper能按同realm/tenant/active/version读写及后一Step使用；Java Bean/Mapper按模块现有配置发现，前端按现有auth client消费。
- Why now: 前一文件建立本文件需要的字段或调用契约； PO↔BO双向MapStruct转换在此位置后才可接消费者。
- Contract/signature changes: OAuthAuditConverter；PO↔BO双向MapStruct转换。公开行为只取主Spec §9对应API或§10/11内部合同。
- Input/output and state mapping: OAuthAuditPO business fields from §11 → OAuthAuditBO; unknown enum拒绝，null按列约束。
- Error and edge behavior: generated implementation/constructor must compile; no BeanUtils/json copy。
- Standards impact: MC-ARCH-001, MC-SCOPE-001, MC-TEST-001, MC-VALID-001；按已选传统分层，输入校验与权限先于状态读取，PO不出边界，相关日志脱敏。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 6, Rule 10, Rule 11；此文件直接按所列原始规则落实命名、验证、类型/注入或配置，其他规则由同Step关联文件负责。
- Implementation pseudocode:

```java
INPUT -> OAuthAuditPO business fields from §11 → OAuthAuditBO; unknown enum拒绝，null按列约束。
FLOW -> @Mapper(componentModel="spring") interface OAuthAuditConverter extends BaseConverter<OAuthAuditBO,OAuthAuditPO>; @Mapping inherited tenant/audit fill ignored on insert; enum code explicit; secret excluded from toString/VO; mapping date Instant/UTC
ON INVALID/CONCURRENT/DEPENDENCY FAILURE -> generated implementation/constructor must compile; no BeanUtils/json copy。
RESULT -> only the named symbol/contract in this File block becomes available; preserve tenant, expectedVersion, scope, UTC time and secret masking stated above.
```

- Verification contribution: 本Step `mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthSigningKeyPersistenceContractTest -Dsurefire.failIfNoSpecifiedTests=false test`；此文件负责 OAuthAuditConverter 的正例与 generated implementation/constructor must compile; no BeanUtils/json copy 负例。
- After this file: OAuthAuditConverter 的字段/入口或注册已落盘；与下一文件组合后本Step才能GREEN，不能把中间状态当完成。

#### File 31 — `CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/audit/dao/OAuthAuditMapper.java`

- Purpose: MP Mapper具名SQL接口。
- Symbols: `OAuthAuditMapper`。
- Repository evidence: 当前不存在；Spec §8/§11 明确目标，邻近包可验证；目标源于主Spec §7–§16。
- Dependencies and consumers: 输入依赖 Step 5 与本Step此前文件；输出供 这组认证事实的Repository/Mapper能按同realm/tenant/active/version读写及后一Step使用；Java Bean/Mapper按模块现有配置发现，前端按现有auth client消费。
- Why now: 前一文件建立本文件需要的字段或调用契约； MP Mapper具名SQL接口在此位置后才可接消费者。
- Contract/signature changes: OAuthAuditMapper；MP Mapper具名SQL接口。公开行为只取主Spec §9对应API或§10/11内部合同。
- Input/output and state mapping: tenantId positive Long from verified MDC; result OAuthAuditPO。
- Error and edge behavior: no LambdaQueryChain/Wrapper; missing tenant failclosed。
- Standards impact: MC-ARCH-001, MC-SCOPE-001, MC-TEST-001, MC-VALID-001；按已选传统分层，输入校验与权限先于状态读取，PO不出边界，相关日志脱敏。
- Literal rule enforcement: Rule 1, Rule 2, Rule 11；此文件直接按所列原始规则落实命名、验证、类型/注入或配置，其他规则由同Step关联文件负责。
- Implementation pseudocode:

```java
INPUT -> tenantId positive Long from verified MDC; result OAuthAuditPO。
FLOW -> interface OAuthAuditMapper extends EgonColaMapper<OAuthAuditPO>; methods selectActiveById/Ids, deleteVersionedById, tenant+eventId or occurredAt,id cursor SQL bindings
ON INVALID/CONCURRENT/DEPENDENCY FAILURE -> no LambdaQueryChain/Wrapper; missing tenant failclosed。
RESULT -> only the named symbol/contract in this File block becomes available; preserve tenant, expectedVersion, scope, UTC time and secret masking stated above.
```

- Verification contribution: 本Step `mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthSigningKeyPersistenceContractTest -Dsurefire.failIfNoSpecifiedTests=false test`；此文件负责 OAuthAuditMapper 的正例与 no LambdaQueryChain/Wrapper; missing tenant failclosed 负例。
- After this file: OAuthAuditMapper 的字段/入口或注册已落盘；与下一文件组合后本Step才能GREEN，不能把中间状态当完成。

#### File 32 — `CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/resources/mapper/audit/OAuthAuditMapper.xml`

- Purpose: 具名PostgreSQL谓词/锁。
- Symbols: `OAuthAuditMapper SQL`。
- Repository evidence: 当前不存在；Spec §8/§11 明确目标，邻近包可验证；目标源于主Spec §7–§16。
- Dependencies and consumers: 输入依赖 Step 5 与本Step此前文件；输出供 这组认证事实的Repository/Mapper能按同realm/tenant/active/version读写及后一Step使用；Java Bean/Mapper按模块现有配置发现，前端按现有auth client消费。
- Why now: 前一文件建立本文件需要的字段或调用契约； 具名PostgreSQL谓词/锁在此位置后才可接消费者。
- Contract/signature changes: OAuthAuditMapper SQL；具名PostgreSQL谓词/锁。公开行为只取主Spec §9对应API或§10/11内部合同。
- Input/output and state mapping: 0/1 precise query or capped list; indexes §11。
- Error and edge behavior: tenant range forbidden, 0 rows conflict, no cross-group write。
- Standards impact: MC-SCOPE-001, MC-TEST-001；按已选传统分层，输入校验与权限先于状态读取，PO不出边界，相关日志脱敏。
- Literal rule enforcement: Rule 2, Rule 10, Rule 11；此文件直接按所列原始规则落实命名、验证、类型/注入或配置，其他规则由同Step关联文件负责。
- Implementation pseudocode:

```xml
INPUT -> 0/1 precise query or capped list; indexes §11。
FLOW -> SELECT business columns + inherited fields FROM tianquan_oauth.oauth_audit WHERE tenant_id=#{tenantId} AND deleted_at IS NULL AND tenant+eventId or occurredAt,id cursor; guarded UPDATE id+tenant+version+expected state; SELECT FOR UPDATE only command path; max page bounded
ON INVALID/CONCURRENT/DEPENDENCY FAILURE -> tenant range forbidden, 0 rows conflict, no cross-group write。
RESULT -> only the named symbol/contract in this File block becomes available; preserve tenant, expectedVersion, scope, UTC time and secret masking stated above.
```

- Verification contribution: 本Step `mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthSigningKeyPersistenceContractTest -Dsurefire.failIfNoSpecifiedTests=false test`；此文件负责 OAuthAuditMapper SQL 的正例与 tenant range forbidden, 0 rows conflict, no cross-group write 负例。
- After this file: OAuthAuditMapper SQL 的字段/入口或注册已落盘；与下一文件组合后本Step才能GREEN，不能把中间状态当完成。

#### File 33 — `CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/audit/repository/impl/OAuthAuditRepositoryImpl.java`

- Purpose: MP repository ACL。
- Symbols: `OAuthAuditRepositoryImpl`。
- Repository evidence: 当前不存在；Spec §8/§11 明确目标，邻近包可验证；目标源于主Spec §7–§16。
- Dependencies and consumers: 输入依赖 Step 5 与本Step此前文件；输出供 这组认证事实的Repository/Mapper能按同realm/tenant/active/version读写及后一Step使用；Java Bean/Mapper按模块现有配置发现，前端按现有auth client消费。
- Why now: 前一文件建立本文件需要的字段或调用契约； MP repository ACL在此位置后才可接消费者。
- Contract/signature changes: OAuthAuditRepositoryImpl；MP repository ACL。公开行为只取主Spec §9对应API或§10/11内部合同。
- Input/output and state mapping: OAuthAuditBO↔PO; raw secret/private only protected internal fields。
- Error and edge behavior: UnsupportedOperationException for generic unscoped query; errors mapped without sensitive values。
- Standards impact: MC-ARCH-001, MC-SCOPE-001, MC-TEST-001, MC-VALID-001；按已选传统分层，输入校验与权限先于状态读取，PO不出边界，相关日志脱敏。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 6, Rule 9, Rule 10, Rule 11；此文件直接按所列原始规则落实命名、验证、类型/注入或配置，其他规则由同Step关联文件负责。
- Implementation pseudocode:

```java
INPUT -> OAuthAuditBO↔PO; raw secret/private only protected internal fields。
FLOW -> @Repository("oAuthAuditRepository") @Slf4j @RequiredArgsConstructor; final @Qualifier("oAuthAuditMapper") Mapper, converter, modelValidationUtils, properties; extends EgonColaRepository<Mapper,PO>; find maps PO→BO; save maps BO→PO, guarded version update result==1
ON INVALID/CONCURRENT/DEPENDENCY FAILURE -> UnsupportedOperationException for generic unscoped query; errors mapped without sensitive values。
RESULT -> only the named symbol/contract in this File block becomes available; preserve tenant, expectedVersion, scope, UTC time and secret masking stated above.
```

- Verification contribution: 本Step `mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthSigningKeyPersistenceContractTest -Dsurefire.failIfNoSpecifiedTests=false test`；此文件负责 OAuthAuditRepositoryImpl 的正例与 UnsupportedOperationException for generic unscoped query; errors mapped without sensitive values 负例。
- After this file: OAuthAuditRepositoryImpl 的字段/入口或注册已落盘；与下一文件组合后本Step才能GREEN，不能把中间状态当完成。

- Validation working directory: `/Users/mario/SelfProject/Egon-COLA`。
- Verification command: `mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthSigningKeyPersistenceContractTest -Dsurefire.failIfNoSpecifiedTests=false test`。
- Expected result: 先确认上述RED由缺失目标行为引起，GREEN时命名用例成功、模块编译成功、`git diff --check`零错误；不以不存在依赖/数据库/fixture而失败作为RED。
- Failure returns to: Step 5 的编译/Schema前置；若需改变外部语义、架构或DDL版本则回主Spec。
- Completion criteria: 这组认证事实的Repository/Mapper能按同realm/tenant/active/version读写；本Step的所有Manual Checks在实际执行时给出证据，未通过不能提交。
- Rollback: 提交前仅恢复本Step的精确路径；DDL已对真实数据运行后只forward-fix和新版本，不能编辑既有SQL/历史或恢复旧安全世代。
- Commit paths: `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/config/OAuthKeyEncryptionProperties.java`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/service/impl/OAuthKeyCipherComponent.java`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/domain/enums/OAuthSecurityTargetEnum.java`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/domain/enums/OAuthKeyStatusEnum.java`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/domain/po/OAuthSigningKeyPO.java`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/domain/bo/OAuthSigningKeyBO.java`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/repository/OAuthSigningKeyRepository.java`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/domain/po/OAuthSecurityStatePO.java`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/domain/bo/OAuthSecurityStateBO.java`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/repository/OAuthSecurityStateRepository.java`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/domain/po/OAuthRevocationPO.java`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/domain/bo/OAuthRevocationBO.java`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/repository/OAuthRevocationRepository.java`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/audit/domain/po/OAuthAuditPO.java`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/audit/domain/bo/OAuthAuditBO.java`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/audit/repository/OAuthAuditRepository.java`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/repository/OAuthSigningKeyPersistenceContractTest.java`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/domain/converter/OAuthSigningKeyConverter.java`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/dao/OAuthSigningKeyMapper.java`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/resources/mapper/token/OAuthSigningKeyMapper.xml`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/repository/impl/OAuthSigningKeyRepositoryImpl.java`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/domain/converter/OAuthSecurityStateConverter.java`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/dao/OAuthSecurityStateMapper.java`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/resources/mapper/token/OAuthSecurityStateMapper.xml`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/repository/impl/OAuthSecurityStateRepositoryImpl.java`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/domain/converter/OAuthRevocationConverter.java`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/dao/OAuthRevocationMapper.java`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/resources/mapper/token/OAuthRevocationMapper.xml`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/repository/impl/OAuthRevocationRepositoryImpl.java`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/audit/domain/converter/OAuthAuditConverter.java`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/audit/dao/OAuthAuditMapper.java`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/resources/mapper/audit/OAuthAuditMapper.xml`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/audit/repository/impl/OAuthAuditRepositoryImpl.java`。
- Commit: `feat(shoubing): persist oauthsigningkey/oauthsecuritystate/oauthrevocation/oauthaudit state`。

### Step 7 — 装配多issuer SAS仓储与协议发现

- Requirements: REQ-001, REQ-002, REQ-004, REQ-013, REQ-016
- Dependencies: Step 6
- Baseline state: 当前HEAD的对应源码/测试如每File的Repository evidence，先前Step已完成并有其focused GREEN；本Step尚不存在isolatesSameClientAndKidAcrossIssuers所列新行为。
- Observable outcome: SAS多issuer精确registry、JWK与protocol discovery逐租户独立。
- End state: SAS多issuer精确registry、JWK与protocol discovery逐租户独立；后续Step的Consumer尚未切流，不声明完整生产验收。
- Test-first gate: Required — 两issuer测试先因SPI/多路径缺失RED，再补真实SAS装配GREEN。
- Manual Checks: MC-ARCH-001, MC-REUSE-001, MC-SCOPE-001, MC-TEST-001, MC-VALID-001, MC-NAME-001, MC-MODEL-001, MC-CONVERT-001, MC-LOG-001, MC-BEAN-001, MC-JSON-001, MC-TIME-001
- Literal Rules: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11
- Ordered files:

#### File 1 — `CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/TenantIssuerIsolationTest.java`

- Purpose: RED A/B同clientId/kid互相拒绝。
- Symbols: `isolatesSameClientAndKidAcrossIssuers`。
- Repository evidence: 当前不存在；Spec §8/§11 明确目标，邻近包可验证；目标源于主Spec §7–§16。
- Dependencies and consumers: 输入依赖 Step 6 与本Step此前文件；输出供 SAS多issuer精确registry、JWK与protocol discovery逐租户独立及后一Step使用；Java Bean/Mapper按模块现有配置发现，前端按现有auth client消费。
- Why now: 本Step的编译前置/RED契约先锁定； RED A/B同clientId/kid互相拒绝在此位置后才可接消费者。
- Contract/signature changes: isolatesSameClientAndKidAcrossIssuers；RED A/B同clientId/kid互相拒绝。公开行为只取主Spec §9对应API或§10/11内部合同。
- Input/output and state mapping: same clientId≠same secret/issuer state。
- Error and edge behavior: no SAS Bean/route yields RED。
- Standards impact: MC-ARCH-001, MC-SCOPE-001, MC-TEST-001, MC-VALID-001；按已选传统分层，输入校验与权限先于状态读取，PO不出边界，相关日志脱敏。
- Literal rule enforcement: Rule 2, Rule 11；此文件直接按所列原始规则落实命名、验证、类型/注入或配置，其他规则由同Step关联文件负责。
- Implementation pseudocode:

```java
INPUT -> same clientId≠same secret/issuer state。
FLOW -> MockMvc discovery/JWKS/token endpoints with tenant1001/1002 + platform; assert mismatch credential/kid/error; unknown issuer no DNS
ON INVALID/CONCURRENT/DEPENDENCY FAILURE -> no SAS Bean/route yields RED。
RESULT -> only the named symbol/contract in this File block becomes available; preserve tenant, expectedVersion, scope, UTC time and secret masking stated above.
```

- Verification contribution: 本Step `mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=TenantIssuerIsolationTest -Dsurefire.failIfNoSpecifiedTests=false test`；此文件负责 isolatesSameClientAndKidAcrossIssuers 的正例与 no SAS Bean/route yields RED 负例。
- After this file: isolatesSameClientAndKidAcrossIssuers 的字段/入口或注册已落盘；与下一文件组合后本Step才能GREEN，不能把中间状态当完成。

#### File 2 — `CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/domain/converter/SasRegisteredClientConverter.java`

- Purpose: 关闭 `SasRegisteredClientConverter` 在主Spec中的字段/消费依赖。
- Symbols: `SasRegisteredClientConverter`。
- Repository evidence: 当前不存在；主Spec §8.4/§10.2 命名此对象/字段；现有相邻业务包提供放置证据。
- Dependencies and consumers: 本Step既有File块调用该类型；数据来源/消费者为主Spec §8.4/§10.2，不产生新HTTP路由。
- Why now: 该类型按本Step的测试/编译依赖顺序落位，以免由实现者临时发明。
- Contract/signature changes: BaseForwardConverter<OAuthClientBO,RegisteredClient>; internal id/string, grants from purpose, redirect URIs, bcrypt, TTL, PKCE。
- Input/output and state mapping: 精确字段由主Spec §8.4/§10.2 指定，realm从签名/注册得出，Instant为UTC，版本单调，秘密字段仅服务器内部。
- Error and edge behavior: null/未知字段/跨tenant/重复版本拒绝；不转成宽松缺省或吞异常。
- Standards impact: MC-ARCH-001, MC-NAME-001, MC-VALID-001, MC-MODEL-001, MC-CONVERT-001, MC-BEAN-001, MC-JSON-001, MC-TIME-001, MC-SCOPE-001, MC-TEST-001；行为Bean使用具名final Qualifier、@RequiredArgsConstructor和@Slf4j，载体遵循四注解与Builder。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 6, Rule 9, Rule 10, Rule 11；class按POJO语义命名，边界注解验证，跨层转换只用既有BaseConverter，Jackson和java.time字段明确。
- Implementation pseudocode:

```java
INPUT -> 仅接受主Spec §8.4/§10.2 的可信issuer/realm/输入字段；调用前 @Valid 或 ValueObject不变量检查。
FLOW -> BaseForwardConverter<OAuthClientBO,RegisteredClient>; internal id/string, grants from purpose, redirect URIs, bcrypt, TTL, PKCE。
OUTPUT -> SasRegisteredClientConverter 的Java方法、DTO字段或配置可被本Step既有调用者编译使用；不新增业务API。
FAIL -> 空值/跨tenant/未知schema/重复版本/外部依赖错误按本Step已列错误模型fail-closed，不能输出原secret/token。
```

- Verification contribution: Step 7 的focus测试对 `SasRegisteredClientConverter` 的字段、装配、负例及不泄密断言。
- After this file: `SasRegisteredClientConverter` 成为已声明的Step依赖，下一文件可按原合同引用；未完成整条行为前不宣称GREEN。

#### File 3 — `CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/domain/converter/SasAuthorizationConverter.java`

- Purpose: 关闭 `SasAuthorizationConverter` 在主Spec中的字段/消费依赖。
- Symbols: `SasAuthorizationConverter`。
- Repository evidence: 当前不存在；主Spec §8.4/§10.2 命名此对象/字段；现有相邻业务包提供放置证据。
- Dependencies and consumers: 本Step既有File块调用该类型；数据来源/消费者为主Spec §8.4/§10.2，不产生新HTTP路由。
- Why now: 该类型按本Step的测试/编译依赖顺序落位，以免由实现者临时发明。
- Contract/signature changes: BaseConverter<OAuthAuthorizationBO,OAuth2Authorization>; token children max4, decrypt attributes through approved cipher。
- Input/output and state mapping: 精确字段由主Spec §8.4/§10.2 指定，realm从签名/注册得出，Instant为UTC，版本单调，秘密字段仅服务器内部。
- Error and edge behavior: null/未知字段/跨tenant/重复版本拒绝；不转成宽松缺省或吞异常。
- Standards impact: MC-ARCH-001, MC-NAME-001, MC-VALID-001, MC-MODEL-001, MC-CONVERT-001, MC-BEAN-001, MC-JSON-001, MC-TIME-001, MC-SCOPE-001, MC-TEST-001；行为Bean使用具名final Qualifier、@RequiredArgsConstructor和@Slf4j，载体遵循四注解与Builder。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 6, Rule 9, Rule 10, Rule 11；class按POJO语义命名，边界注解验证，跨层转换只用既有BaseConverter，Jackson和java.time字段明确。
- Implementation pseudocode:

```java
INPUT -> 仅接受主Spec §8.4/§10.2 的可信issuer/realm/输入字段；调用前 @Valid 或 ValueObject不变量检查。
FLOW -> BaseConverter<OAuthAuthorizationBO,OAuth2Authorization>; token children max4, decrypt attributes through approved cipher。
OUTPUT -> SasAuthorizationConverter 的Java方法、DTO字段或配置可被本Step既有调用者编译使用；不新增业务API。
FAIL -> 空值/跨tenant/未知schema/重复版本/外部依赖错误按本Step已列错误模型fail-closed，不能输出原secret/token。
```

- Verification contribution: Step 7 的focus测试对 `SasAuthorizationConverter` 的字段、装配、负例及不泄密断言。
- After this file: `SasAuthorizationConverter` 成为已声明的Step依赖，下一文件可按原合同引用；未完成整条行为前不宣称GREEN。

#### File 4 — `CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/domain/converter/SasConsentConverter.java`

- Purpose: 关闭 `SasConsentConverter` 在主Spec中的字段/消费依赖。
- Symbols: `SasConsentConverter`。
- Repository evidence: 当前不存在；主Spec §8.4/§10.2 命名此对象/字段；现有相邻业务包提供放置证据。
- Dependencies and consumers: 本Step既有File块调用该类型；数据来源/消费者为主Spec §8.4/§10.2，不产生新HTTP路由。
- Why now: 该类型按本Step的测试/编译依赖顺序落位，以免由实现者临时发明。
- Contract/signature changes: BaseConverter<OAuthConsentBO,OAuth2AuthorizationConsent>; subject/client/scopes exact realm。
- Input/output and state mapping: 精确字段由主Spec §8.4/§10.2 指定，realm从签名/注册得出，Instant为UTC，版本单调，秘密字段仅服务器内部。
- Error and edge behavior: null/未知字段/跨tenant/重复版本拒绝；不转成宽松缺省或吞异常。
- Standards impact: MC-ARCH-001, MC-NAME-001, MC-VALID-001, MC-MODEL-001, MC-CONVERT-001, MC-BEAN-001, MC-JSON-001, MC-TIME-001, MC-SCOPE-001, MC-TEST-001；行为Bean使用具名final Qualifier、@RequiredArgsConstructor和@Slf4j，载体遵循四注解与Builder。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 6, Rule 9, Rule 10, Rule 11；class按POJO语义命名，边界注解验证，跨层转换只用既有BaseConverter，Jackson和java.time字段明确。
- Implementation pseudocode:

```java
INPUT -> 仅接受主Spec §8.4/§10.2 的可信issuer/realm/输入字段；调用前 @Valid 或 ValueObject不变量检查。
FLOW -> BaseConverter<OAuthConsentBO,OAuth2AuthorizationConsent>; subject/client/scopes exact realm。
OUTPUT -> SasConsentConverter 的Java方法、DTO字段或配置可被本Step既有调用者编译使用；不新增业务API。
FAIL -> 空值/跨tenant/未知schema/重复版本/外部依赖错误按本Step已列错误模型fail-closed，不能输出原secret/token。
```

- Verification contribution: Step 7 的focus测试对 `SasConsentConverter` 的字段、装配、负例及不泄密断言。
- After this file: `SasConsentConverter` 成为已声明的Step依赖，下一文件可按原合同引用；未完成整条行为前不宣称GREEN。

#### File 5 — `CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/service/TenantIssuerService.java`

- Purpose: 业务分层的registry只读接口。
- Symbols: `resolveExactIssuer(URI)`。
- Repository evidence: 当前不存在；Spec §8/§11 明确目标，邻近包可验证；目标源于主Spec §7–§16。
- Dependencies and consumers: 输入依赖 Step 6 与本Step此前文件；输出供 SAS多issuer精确registry、JWK与protocol discovery逐租户独立及后一Step使用；Java Bean/Mapper按模块现有配置发现，前端按现有auth client消费。
- Why now: 前一文件建立本文件需要的字段或调用契约； 业务分层的registry只读接口在此位置后才可接消费者。
- Contract/signature changes: resolveExactIssuer(URI)；业务分层的registry只读接口。公开行为只取主Spec §9对应API或§10/11内部合同。
- Input/output and state mapping: URL normalized first under configured public origin; then realm storage partition。
- Error and edge behavior: suffix/prefix spoof deny without outbound lookup。
- Standards impact: MC-ARCH-001, MC-SCOPE-001, MC-TEST-001, MC-VALID-001；按已选传统分层，输入校验与权限先于状态读取，PO不出边界，相关日志脱敏。
- Literal rule enforcement: Rule 1, Rule 2, Rule 11；此文件直接按所列原始规则落实命名、验证、类型/注入或配置，其他规则由同Step关联文件负责。
- Implementation pseudocode:

```java
INPUT -> URL normalized first under configured public origin; then realm storage partition。
FLOW -> @Validated TrustedRealmBO resolveExactIssuer(@NotNull URI issuer); ACTIVE only; platform special branch by registered realm type
ON INVALID/CONCURRENT/DEPENDENCY FAILURE -> suffix/prefix spoof deny without outbound lookup。
RESULT -> only the named symbol/contract in this File block becomes available; preserve tenant, expectedVersion, scope, UTC time and secret masking stated above.
```

- Verification contribution: 本Step `mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=TenantIssuerIsolationTest -Dsurefire.failIfNoSpecifiedTests=false test`；此文件负责 resolveExactIssuer(URI) 的正例与 suffix/prefix spoof deny without outbound lookup 负例。
- After this file: resolveExactIssuer(URI) 的字段/入口或注册已落盘；与下一文件组合后本Step才能GREEN，不能把中间状态当完成。

#### File 6 — `CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/service/impl/TenantIssuerServiceImpl.java`

- Purpose: exact issuer registry Adapter。
- Symbols: `tenantIssuerService`。
- Repository evidence: 当前不存在；Spec §8/§11 明确目标，邻近包可验证；目标源于主Spec §7–§16。
- Dependencies and consumers: 输入依赖 Step 6 与本Step此前文件；输出供 SAS多issuer精确registry、JWK与protocol discovery逐租户独立及后一Step使用；Java Bean/Mapper按模块现有配置发现，前端按现有auth client消费。
- Why now: 前一文件建立本文件需要的字段或调用契约； exact issuer registry Adapter在此位置后才可接消费者。
- Contract/signature changes: tenantIssuerService；exact issuer registry Adapter。公开行为只取主Spec §9对应API或§10/11内部合同。
- Input/output and state mapping: PLATFORM has null business tenant and unique storage partition。
- Error and edge behavior: unknown/disabled→404; no endsWith; cache evict on state。
- Standards impact: MC-ARCH-001, MC-SCOPE-001, MC-TEST-001, MC-VALID-001；按已选传统分层，输入校验与权限先于状态读取，PO不出边界，相关日志脱敏。
- Literal rule enforcement: Rule 1, Rule 2, Rule 4, Rule 9, Rule 11；此文件直接按所列原始规则落实命名、验证、类型/注入或配置，其他规则由同Step关联文件负责。
- Implementation pseudocode:

```java
INPUT -> PLATFORM has null business tenant and unique storage partition。
FLOW -> @Service("tenantIssuerService") @Slf4j @RequiredArgsConstructor; @Qualifier("oauthRealmRepository") final; parse strict https and decimal tenant; exact equals stored issuer; cache bounded by trustVersion; return TrustedRealmBO
ON INVALID/CONCURRENT/DEPENDENCY FAILURE -> unknown/disabled→404; no endsWith; cache evict on state。
RESULT -> only the named symbol/contract in this File block becomes available; preserve tenant, expectedVersion, scope, UTC time and secret masking stated above.
```

- Verification contribution: 本Step `mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=TenantIssuerIsolationTest -Dsurefire.failIfNoSpecifiedTests=false test`；此文件负责 tenantIssuerService 的正例与 unknown/disabled→404; no endsWith; cache evict on state 负例。
- After this file: tenantIssuerService 的字段/入口或注册已落盘；与下一文件组合后本Step才能GREEN，不能把中间状态当完成。

#### File 7 — `CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/repository/TenantRegisteredClientRepository.java`

- Purpose: perissuer RegisteredClientRepository SPI。
- Symbols: `tenantRegisteredClientRepository`。
- Repository evidence: 当前不存在；Spec §8/§11 明确目标，邻近包可验证；目标源于主Spec §7–§16。
- Dependencies and consumers: 输入依赖 Step 6 与本Step此前文件；输出供 SAS多issuer精确registry、JWK与protocol discovery逐租户独立及后一Step使用；Java Bean/Mapper按模块现有配置发现，前端按现有auth client消费。
- Why now: 前一文件建立本文件需要的字段或调用契约； perissuer RegisteredClientRepository SPI在此位置后才可接消费者。
- Contract/signature changes: tenantRegisteredClientRepository；perissuer RegisteredClientRepository SPI。公开行为只取主Spec §9对应API或§10/11内部合同。
- Input/output and state mapping: same clientId each realm isolated; no implicit wildcard scopes。
- Error and edge behavior: no context/unknown issuer fails before DAO。
- Standards impact: MC-ARCH-001, MC-SCOPE-001, MC-TEST-001, MC-VALID-001；按已选传统分层，输入校验与权限先于状态读取，PO不出边界，相关日志脱敏。
- Literal rule enforcement: Rule 1, Rule 2, Rule 4, Rule 9, Rule 11；此文件直接按所列原始规则落实命名、验证、类型/注入或配置，其他规则由同Step关联文件负责。
- Implementation pseudocode:

```java
INPUT -> same clientId each realm isolated; no implicit wildcard scopes。
FLOW -> delegate findById/findByClientId/save through AuthorizationServerContextHolder issuer→TenantIssuerService→OAuthClientRepository; SasRegisteredClientConverter maps bcrypt+grants+ttl+redirect
ON INVALID/CONCURRENT/DEPENDENCY FAILURE -> no context/unknown issuer fails before DAO。
RESULT -> only the named symbol/contract in this File block becomes available; preserve tenant, expectedVersion, scope, UTC time and secret masking stated above.
```

- Verification contribution: 本Step `mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=TenantIssuerIsolationTest -Dsurefire.failIfNoSpecifiedTests=false test`；此文件负责 tenantRegisteredClientRepository 的正例与 no context/unknown issuer fails before DAO 负例。
- After this file: tenantRegisteredClientRepository 的字段/入口或注册已落盘；与下一文件组合后本Step才能GREEN，不能把中间状态当完成。

#### File 8 — `CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/service/impl/TenantAuthorizationServiceImpl.java`

- Purpose: SAS OAuth2AuthorizationService SPI。
- Symbols: `tenantAuthorizationService`。
- Repository evidence: 当前不存在；Spec §8/§11 明确目标，邻近包可验证；目标源于主Spec §7–§16。
- Dependencies and consumers: 输入依赖 Step 6 与本Step此前文件；输出供 SAS多issuer精确registry、JWK与protocol discovery逐租户独立及后一Step使用；Java Bean/Mapper按模块现有配置发现，前端按现有auth client消费。
- Why now: 前一文件建立本文件需要的字段或调用契约； SAS OAuth2AuthorizationService SPI在此位置后才可接消费者。
- Contract/signature changes: tenantAuthorizationService；SAS OAuth2AuthorizationService SPI。公开行为只取主Spec §9对应API或§10/11内部合同。
- Input/output and state mapping: Token digest→AuthorizationBO + <=4 current token projections。
- Error and edge behavior: cross-issuer token lookup none; duplicate code invalid_grant。
- Standards impact: MC-ARCH-001, MC-SCOPE-001, MC-TEST-001, MC-VALID-001；按已选传统分层，输入校验与权限先于状态读取，PO不出边界，相关日志脱敏。
- Literal rule enforcement: Rule 1, Rule 2, Rule 4, Rule 9, Rule 11；此文件直接按所列原始规则落实命名、验证、类型/注入或配置，其他规则由同Step关联文件负责。
- Implementation pseudocode:

```java
INPUT -> Token digest→AuthorizationBO + <=4 current token projections。
FLOW -> findById/findByToken/save/remove scoped realm; authorization+tokens use same primary transaction and code/RT guarded consume; SasAuthorizationConverter controls encrypted attributes
ON INVALID/CONCURRENT/DEPENDENCY FAILURE -> cross-issuer token lookup none; duplicate code invalid_grant。
RESULT -> only the named symbol/contract in this File block becomes available; preserve tenant, expectedVersion, scope, UTC time and secret masking stated above.
```

- Verification contribution: 本Step `mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=TenantIssuerIsolationTest -Dsurefire.failIfNoSpecifiedTests=false test`；此文件负责 tenantAuthorizationService 的正例与 cross-issuer token lookup none; duplicate code invalid_grant 负例。
- After this file: tenantAuthorizationService 的字段/入口或注册已落盘；与下一文件组合后本Step才能GREEN，不能把中间状态当完成。

#### File 9 — `CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/service/impl/TenantConsentServiceImpl.java`

- Purpose: SAS Consent SPI。
- Symbols: `tenantConsentService`。
- Repository evidence: 当前不存在；Spec §8/§11 明确目标，邻近包可验证；目标源于主Spec §7–§16。
- Dependencies and consumers: 输入依赖 Step 6 与本Step此前文件；输出供 SAS多issuer精确registry、JWK与protocol discovery逐租户独立及后一Step使用；Java Bean/Mapper按模块现有配置发现，前端按现有auth client消费。
- Why now: 前一文件建立本文件需要的字段或调用契约； SAS Consent SPI在此位置后才可接消费者。
- Contract/signature changes: tenantConsentService；SAS Consent SPI。公开行为只取主Spec §9对应API或§10/11内部合同。
- Input/output and state mapping: approved scopes only, no RBAC role copied。
- Error and edge behavior: missing/other tenant denies。
- Standards impact: MC-ARCH-001, MC-SCOPE-001, MC-TEST-001, MC-VALID-001；按已选传统分层，输入校验与权限先于状态读取，PO不出边界，相关日志脱敏。
- Literal rule enforcement: Rule 1, Rule 2, Rule 4, Rule 9, Rule 11；此文件直接按所列原始规则落实命名、验证、类型/注入或配置，其他规则由同Step关联文件负责。
- Implementation pseudocode:

```java
INPUT -> approved scopes only, no RBAC role copied。
FLOW -> findById/save/remove scoped tenant+client+subject; scope increase requires new grant approval, no migrated consent
ON INVALID/CONCURRENT/DEPENDENCY FAILURE -> missing/other tenant denies。
RESULT -> only the named symbol/contract in this File block becomes available; preserve tenant, expectedVersion, scope, UTC time and secret masking stated above.
```

- Verification contribution: 本Step `mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=TenantIssuerIsolationTest -Dsurefire.failIfNoSpecifiedTests=false test`；此文件负责 tenantConsentService 的正例与 missing/other tenant denies 负例。
- After this file: tenantConsentService 的字段/入口或注册已落盘；与下一文件组合后本Step才能GREEN，不能把中间状态当完成。

#### File 10 — `CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/service/impl/TenantJwkSourceComponent.java`

- Purpose: active private signer + all allowed public verification keys。
- Symbols: `tenantJwkSourceComponent`。
- Repository evidence: 当前不存在；Spec §8/§11 明确目标，邻近包可验证；目标源于主Spec §7–§16。
- Dependencies and consumers: 输入依赖 Step 6 与本Step此前文件；输出供 SAS多issuer精确registry、JWK与protocol discovery逐租户独立及后一Step使用；Java Bean/Mapper按模块现有配置发现，前端按现有auth client消费。
- Why now: 前一文件建立本文件需要的字段或调用契约； active private signer + all allowed public verification keys在此位置后才可接消费者。
- Contract/signature changes: tenantJwkSourceComponent；active private signer + all allowed public verification keys。公开行为只取主Spec §9对应API或§10/11内部合同。
- Input/output and state mapping: issuer+kid composite; alg RS256; JWT typ at+jwt。
- Error and edge behavior: missing/compromised key fails signing/readiness。
- Standards impact: MC-ARCH-001, MC-SCOPE-001, MC-TEST-001, MC-VALID-001；按已选传统分层，输入校验与权限先于状态读取，PO不出边界，相关日志脱敏。
- Literal rule enforcement: Rule 1, Rule 2, Rule 4, Rule 9, Rule 11；此文件直接按所列原始规则落实命名、验证、类型/注入或配置，其他规则由同Step关联文件负责。
- Implementation pseudocode:

```java
INPUT -> issuer+kid composite; alg RS256; JWT typ at+jwt。
FLOW -> JWKSource.select exact issuer; ACTIVE private only for requested kid; PUBLISHED/VERIFY_ONLY public keys in JWKS; never return private fields
ON INVALID/CONCURRENT/DEPENDENCY FAILURE -> missing/compromised key fails signing/readiness。
RESULT -> only the named symbol/contract in this File block becomes available; preserve tenant, expectedVersion, scope, UTC time and secret masking stated above.
```

- Verification contribution: 本Step `mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=TenantIssuerIsolationTest -Dsurefire.failIfNoSpecifiedTests=false test`；此文件负责 tenantJwkSourceComponent 的正例与 missing/compromised key fails signing/readiness 负例。
- After this file: tenantJwkSourceComponent 的字段/入口或注册已落盘；与下一文件组合后本Step才能GREEN，不能把中间状态当完成。

#### File 11 — `CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/config/AuthorizationServerConfiguration.java`

- Purpose: SAS+OIDC Filter ordering。
- Symbols: `authorizationServerSettings/SAS SecurityFilterChain`。
- Repository evidence: 当前不存在；Spec §8/§11 明确目标，邻近包可验证；目标源于主Spec §7–§16。
- Dependencies and consumers: 输入依赖 Step 6 与本Step此前文件；输出供 SAS多issuer精确registry、JWK与protocol discovery逐租户独立及后一Step使用；Java Bean/Mapper按模块现有配置发现，前端按现有auth client消费。
- Why now: 前一文件建立本文件需要的字段或调用契约； SAS+OIDC Filter ordering在此位置后才可接消费者。
- Contract/signature changes: authorizationServerSettings/SAS SecurityFilterChain；SAS+OIDC Filter ordering。公开行为只取主Spec §9对应API或§10/11内部合同。
- Input/output and state mapping: metadata issuer/jwks_uri/claims from same registry。
- Error and edge behavior: no root discovery guessed tenant; no OAuth HTTP controller duplicates。
- Standards impact: MC-ARCH-001, MC-SCOPE-001, MC-TEST-001, MC-VALID-001；按已选传统分层，输入校验与权限先于状态读取，PO不出边界，相关日志脱敏。
- Literal rule enforcement: Rule 2, Rule 4, Rule 7, Rule 9, Rule 11；此文件直接按所列原始规则落实命名、验证、类型/注入或配置，其他规则由同Step关联文件负责。
- Implementation pseudocode:

```java
INPUT -> metadata issuer/jwks_uri/claims from same registry。
FLOW -> @Bean names; AuthorizationServerSettings.multipleIssuersAllowed(true); .oidc(); registeredClient/authorization/consent/JWKSource Beans; JWT header active kid+typ; protocol filter before management RS; exact issuer registry
ON INVALID/CONCURRENT/DEPENDENCY FAILURE -> no root discovery guessed tenant; no OAuth HTTP controller duplicates。
RESULT -> only the named symbol/contract in this File block becomes available; preserve tenant, expectedVersion, scope, UTC time and secret masking stated above.
```

- Verification contribution: 本Step `mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=TenantIssuerIsolationTest -Dsurefire.failIfNoSpecifiedTests=false test`；此文件负责 authorizationServerSettings/SAS SecurityFilterChain 的正例与 no root discovery guessed tenant; no OAuth HTTP controller duplicates 负例。
- After this file: authorizationServerSettings/SAS SecurityFilterChain 的字段/入口或注册已落盘；与下一文件组合后本Step才能GREEN，不能把中间状态当完成。

#### File 12 — `CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/config/OAuthProtocolOpenApiConfiguration.java`

- Purpose: document true SAS Filter endpoints。
- Symbols: `openapi customizer`。
- Repository evidence: 当前不存在；Spec §8/§11 明确目标，邻近包可验证；目标源于主Spec §7–§16。
- Dependencies and consumers: 输入依赖 Step 6 与本Step此前文件；输出供 SAS多issuer精确registry、JWK与protocol discovery逐租户独立及后一Step使用；Java Bean/Mapper按模块现有配置发现，前端按现有auth client消费。
- Why now: 前一文件建立本文件需要的字段或调用契约； document true SAS Filter endpoints在此位置后才可接消费者。
- Contract/signature changes: openapi customizer；document true SAS Filter endpoints。公开行为只取主Spec §9对应API或§10/11内部合同。
- Input/output and state mapping: protocol JSON OAuth errors not Egon wrapper。
- Error and edge behavior: generated /v3/api-docs matches discovery。
- Standards impact: MC-ARCH-001, MC-SCOPE-001, MC-TEST-001, MC-VALID-001；按已选传统分层，输入校验与权限先于状态读取，PO不出边界，相关日志脱敏。
- Literal rule enforcement: Rule 4, Rule 6, Rule 11；此文件直接按所列原始规则落实命名、验证、类型/注入或配置，其他规则由同Step关联文件负责。
- Implementation pseudocode:

```java
INPUT -> protocol JSON OAuth errors not Egon wrapper。
FLOW -> only add operationId/method/paths/status/schema for API-001..019 from actual settings, no fake controller
ON INVALID/CONCURRENT/DEPENDENCY FAILURE -> generated /v3/api-docs matches discovery。
RESULT -> only the named symbol/contract in this File block becomes available; preserve tenant, expectedVersion, scope, UTC time and secret masking stated above.
```

- Verification contribution: 本Step `mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=TenantIssuerIsolationTest -Dsurefire.failIfNoSpecifiedTests=false test`；此文件负责 openapi customizer 的正例与 generated /v3/api-docs matches discovery 负例。
- After this file: openapi customizer 的字段/入口或注册已落盘；与下一文件组合后本Step才能GREEN，不能把中间状态当完成。

- Validation working directory: `/Users/mario/SelfProject/Egon-COLA`。
- Verification command: `mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=TenantIssuerIsolationTest -Dsurefire.failIfNoSpecifiedTests=false test`。
- Expected result: 先确认上述RED由缺失目标行为引起，GREEN时命名用例成功、模块编译成功、`git diff --check`零错误；不以不存在依赖/数据库/fixture而失败作为RED。
- Failure returns to: Step 6 的编译/Schema前置；若需改变外部语义、架构或DDL版本则回主Spec。
- Completion criteria: SAS多issuer精确registry、JWK与protocol discovery逐租户独立；本Step的所有Manual Checks在实际执行时给出证据，未通过不能提交。
- Rollback: 提交前仅恢复本Step的精确路径；DDL已对真实数据运行后只forward-fix和新版本，不能编辑既有SQL/历史或恢复旧安全世代。
- Commit paths: `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/domain/converter/SasConsentConverter.java`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/domain/converter/SasAuthorizationConverter.java`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/domain/converter/SasRegisteredClientConverter.java`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/TenantIssuerIsolationTest.java`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/service/TenantIssuerService.java`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/service/impl/TenantIssuerServiceImpl.java`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/repository/TenantRegisteredClientRepository.java`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/service/impl/TenantAuthorizationServiceImpl.java`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/service/impl/TenantConsentServiceImpl.java`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/service/impl/TenantJwkSourceComponent.java`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/config/AuthorizationServerConfiguration.java`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/config/OAuthProtocolOpenApiConfiguration.java`。
- Commit: `feat(shoubing): add multitenant authorization-server protocol`。

### Step 8 — 实现授权码、PKCE和登录交互

- Requirements: REQ-003, REQ-004, REQ-005, REQ-011
- Dependencies: Step 7
- Baseline state: 当前HEAD的对应源码/测试如每File的Repository evidence，先前Step已完成并有其focused GREEN；本Step尚不存在codeS256RedirectNonceReplay所列新行为。
- Observable outcome: 标准Code/PKCE/consent/login和机器Client Credentials签发保持scope/audience界限。
- End state: 标准Code/PKCE/consent/login和机器Client Credentials签发保持scope/audience界限；后续Step的Consumer尚未切流，不声明完整生产验收。
- Test-first gate: Required — 旧token controller无authorization_code/PKCE/OIDC，focused protocol RED后仅最小Provider/登录改动GREEN。
- Manual Checks: MC-ARCH-001, MC-REUSE-001, MC-SCOPE-001, MC-TEST-001, MC-VALID-001, MC-NAME-001, MC-MODEL-001, MC-CONVERT-001, MC-LOG-001, MC-BEAN-001, MC-JSON-001, MC-TIME-001
- Literal Rules: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11
- Ordered files:

#### File 1 — `CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/controller/OAuthOidcProtocolContractTest.java`

- Purpose: RED缺code/nonce拒绝。
- Symbols: `codeS256RedirectNonceReplay`。
- Repository evidence: 当前不存在；Spec §8/§11 明确目标，邻近包可验证；目标源于主Spec §7–§16。
- Dependencies and consumers: 输入依赖 Step 7 与本Step此前文件；输出供 标准Code/PKCE/consent/login和机器Client Credentials签发保持scope/audience界限及后一Step使用；Java Bean/Mapper按模块现有配置发现，前端按现有auth client消费。
- Why now: 本Step的编译前置/RED契约先锁定； RED缺code/nonce拒绝在此位置后才可接消费者。
- Contract/signature changes: codeS256RedirectNonceReplay；RED缺code/nonce拒绝。公开行为只取主Spec §9对应API或§10/11内部合同。
- Input/output and state mapping: synthetic two tenants, URI allowlist, fixed Clock。
- Error and edge behavior: oldOAuthTokenController仅supports client_credentials → RED。
- Standards impact: MC-ARCH-001, MC-SCOPE-001, MC-TEST-001, MC-VALID-001；按已选传统分层，输入校验与权限先于状态读取，PO不出边界，相关日志脱敏。
- Literal rule enforcement: Rule 11, Rule 2, Rule 4；此文件直接按所列原始规则落实命名、验证、类型/注入或配置，其他规则由同Step关联文件负责。
- Implementation pseudocode:

```java
INPUT -> synthetic two tenants, URI allowlist, fixed Clock。
FLOW -> MockMvc Code+S256, clientCredentials+aud, OIDC IDToken, duplicate code/RT; assert no password/implicit/plain; no raw token log
ON INVALID/CONCURRENT/DEPENDENCY FAILURE -> oldOAuthTokenController仅supports client_credentials → RED。
RESULT -> only the named symbol/contract in this File block becomes available; preserve tenant, expectedVersion, scope, UTC time and secret masking stated above.
```

- Verification contribution: 本Step `mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthOidcProtocolContractTest -Dsurefire.failIfNoSpecifiedTests=false test`；此文件负责 codeS256RedirectNonceReplay 的正例与 oldOAuthTokenController仅supports client_credentials → RED 负例。
- After this file: codeS256RedirectNonceReplay 的字段/入口或注册已落盘；与下一文件组合后本Step才能GREEN，不能把中间状态当完成。

#### File 2 — `CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/domain/command/ConsumeOAuthTokenCommand.java`

- Purpose: 关闭 `ConsumeOAuthTokenCommand` 在主Spec中的字段/消费依赖。
- Symbols: `ConsumeOAuthTokenCommand`。
- Repository evidence: 当前不存在；主Spec §7.3.1/§9 命名此对象/字段；现有相邻业务包提供放置证据。
- Dependencies and consumers: 本Step既有File块调用该类型；数据来源/消费者为主Spec §7.3.1/§9，不产生新HTTP路由。
- Why now: 该类型按本Step的测试/编译依赖顺序落位，以免由实现者临时发明。
- Contract/signature changes: realmKey/tokenDigest/clientPk/tokenType/verifier?/redirectUri?; verifier memory only and excluded from toString。
- Input/output and state mapping: 精确字段由主Spec §7.3.1/§9 指定，realm从签名/注册得出，Instant为UTC，版本单调，秘密字段仅服务器内部。
- Error and edge behavior: null/未知字段/跨tenant/重复版本拒绝；不转成宽松缺省或吞异常。
- Standards impact: MC-ARCH-001, MC-NAME-001, MC-VALID-001, MC-MODEL-001, MC-CONVERT-001, MC-BEAN-001, MC-JSON-001, MC-TIME-001, MC-SCOPE-001, MC-TEST-001；行为Bean使用具名final Qualifier、@RequiredArgsConstructor和@Slf4j，载体遵循四注解与Builder。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 6, Rule 9, Rule 10, Rule 11；class按POJO语义命名，边界注解验证，跨层转换只用既有BaseConverter，Jackson和java.time字段明确。
- Implementation pseudocode:

```java
INPUT -> 仅接受主Spec §7.3.1/§9 的可信issuer/realm/输入字段；调用前 @Valid 或 ValueObject不变量检查。
FLOW -> realmKey/tokenDigest/clientPk/tokenType/verifier?/redirectUri?; verifier memory only and excluded from toString。
OUTPUT -> ConsumeOAuthTokenCommand 的Java方法、DTO字段或配置可被本Step既有调用者编译使用；不新增业务API。
FAIL -> 空值/跨tenant/未知schema/重复版本/外部依赖错误按本Step已列错误模型fail-closed，不能输出原secret/token。
```

- Verification contribution: Step 8 的focus测试对 `ConsumeOAuthTokenCommand` 的字段、装配、负例及不泄密断言。
- After this file: `ConsumeOAuthTokenCommand` 成为已声明的Step依赖，下一文件可按原合同引用；未完成整条行为前不宣称GREEN。

#### File 3 — `MODIFY egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/config/TokenConfig.java`

- Purpose: 关闭 `TokenConfig coexistence` 在主Spec中的字段/消费依赖。
- Symbols: `TokenConfig coexistence`。
- Repository evidence: 当前同名文件存在，符号/消费者由rg确认。
- Dependencies and consumers: 本Step既有File块调用该类型；数据来源/消费者为主Spec §7.3.1/§9，不产生新HTTP路由。
- Why now: 该类型按本Step的测试/编译依赖顺序落位，以免由实现者临时发明。
- Contract/signature changes: legacy signer and refresh Beans only on explicitly isolated legacy profile; new AS uses tenant keyring, no duplicate JwtEncoder/JwtDecoder。
- Input/output and state mapping: 精确字段由主Spec §7.3.1/§9 指定，realm从签名/注册得出，Instant为UTC，版本单调，秘密字段仅服务器内部。
- Error and edge behavior: null/未知字段/跨tenant/重复版本拒绝；不转成宽松缺省或吞异常。
- Standards impact: MC-ARCH-001, MC-NAME-001, MC-VALID-001, MC-MODEL-001, MC-CONVERT-001, MC-BEAN-001, MC-JSON-001, MC-TIME-001, MC-SCOPE-001, MC-TEST-001；行为Bean使用具名final Qualifier、@RequiredArgsConstructor和@Slf4j，载体遵循四注解与Builder。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 6, Rule 9, Rule 10, Rule 11；class按POJO语义命名，边界注解验证，跨层转换只用既有BaseConverter，Jackson和java.time字段明确。
- Implementation pseudocode:

```java
INPUT -> 仅接受主Spec §7.3.1/§9 的可信issuer/realm/输入字段；调用前 @Valid 或 ValueObject不变量检查。
FLOW -> legacy signer and refresh Beans only on explicitly isolated legacy profile; new AS uses tenant keyring, no duplicate JwtEncoder/JwtDecoder。
OUTPUT -> TokenConfig coexistence 的Java方法、DTO字段或配置可被本Step既有调用者编译使用；不新增业务API。
FAIL -> 空值/跨tenant/未知schema/重复版本/外部依赖错误按本Step已列错误模型fail-closed，不能输出原secret/token。
```

- Verification contribution: Step 8 的focus测试对 `TokenConfig coexistence` 的字段、装配、负例及不泄密断言。
- After this file: `TokenConfig coexistence` 成为已声明的Step依赖，下一文件可按原合同引用；未完成整条行为前不宣称GREEN。

#### File 4 — `MODIFY egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/config/OAuthConfig.java`

- Purpose: 关闭 `OAuthConfig coexistence` 在主Spec中的字段/消费依赖。
- Symbols: `OAuthConfig coexistence`。
- Repository evidence: 当前同名文件存在，符号/消费者由rg确认。
- Dependencies and consumers: 本Step既有File块调用该类型；数据来源/消费者为主Spec §7.3.1/§9，不产生新HTTP路由。
- Why now: 该类型按本Step的测试/编译依赖顺序落位，以免由实现者临时发明。
- Contract/signature changes: existing Basic authenticator/client policy reused by SAS adapter; legacy signer dependency isolated to legacy profile; no two /oauth2/token mappers。
- Input/output and state mapping: 精确字段由主Spec §7.3.1/§9 指定，realm从签名/注册得出，Instant为UTC，版本单调，秘密字段仅服务器内部。
- Error and edge behavior: null/未知字段/跨tenant/重复版本拒绝；不转成宽松缺省或吞异常。
- Standards impact: MC-ARCH-001, MC-NAME-001, MC-VALID-001, MC-MODEL-001, MC-CONVERT-001, MC-BEAN-001, MC-JSON-001, MC-TIME-001, MC-SCOPE-001, MC-TEST-001；行为Bean使用具名final Qualifier、@RequiredArgsConstructor和@Slf4j，载体遵循四注解与Builder。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 6, Rule 9, Rule 10, Rule 11；class按POJO语义命名，边界注解验证，跨层转换只用既有BaseConverter，Jackson和java.time字段明确。
- Implementation pseudocode:

```java
INPUT -> 仅接受主Spec §7.3.1/§9 的可信issuer/realm/输入字段；调用前 @Valid 或 ValueObject不变量检查。
FLOW -> existing Basic authenticator/client policy reused by SAS adapter; legacy signer dependency isolated to legacy profile; no two /oauth2/token mappers。
OUTPUT -> OAuthConfig coexistence 的Java方法、DTO字段或配置可被本Step既有调用者编译使用；不新增业务API。
FAIL -> 空值/跨tenant/未知schema/重复版本/外部依赖错误按本Step已列错误模型fail-closed，不能输出原secret/token。
```

- Verification contribution: Step 8 的focus测试对 `OAuthConfig coexistence` 的字段、装配、负例及不泄密断言。
- After this file: `OAuthConfig coexistence` 成为已声明的Step依赖，下一文件可按原合同引用；未完成整条行为前不宣称GREEN。

#### File 5 — `CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/service/OAuthTokenIssueService.java`

- Purpose: SAS业务侧发行约束。
- Symbols: `consumeAndIssue(TrustedRealmBO,ConsumeOAuthTokenCommand)`。
- Repository evidence: 当前不存在；Spec §8/§11 明确目标，邻近包可验证；目标源于主Spec §7–§16。
- Dependencies and consumers: 输入依赖 Step 7 与本Step此前文件；输出供 标准Code/PKCE/consent/login和机器Client Credentials签发保持scope/audience界限及后一Step使用；Java Bean/Mapper按模块现有配置发现，前端按现有auth client消费。
- Why now: 前一文件建立本文件需要的字段或调用契约； SAS业务侧发行约束在此位置后才可接消费者。
- Contract/signature changes: consumeAndIssue(TrustedRealmBO,ConsumeOAuthTokenCommand)；SAS业务侧发行约束。公开行为只取主Spec §9对应API或§10/11内部合同。
- Input/output and state mapping: resource/scope from registered+grant+consent intersection。
- Error and edge behavior: no user scope implicit all。
- Standards impact: MC-ARCH-001, MC-SCOPE-001, MC-TEST-001, MC-VALID-001；按已选传统分层，输入校验与权限先于状态读取，PO不出边界，相关日志脱敏。
- Literal rule enforcement: Rule 1, Rule 2, Rule 11；此文件直接按所列原始规则落实命名、验证、类型/注入或配置，其他规则由同Step关联文件负责。
- Implementation pseudocode:

```java
INPUT -> resource/scope from registered+grant+consent intersection。
FLOW -> @Validated OAuth2AccessTokenAuthenticationToken consumeAndIssue(TrustedRealmBO, @Valid ConsumeOAuthTokenCommand); lower-level framework handles protocol shape
ON INVALID/CONCURRENT/DEPENDENCY FAILURE -> no user scope implicit all。
RESULT -> only the named symbol/contract in this File block becomes available; preserve tenant, expectedVersion, scope, UTC time and secret masking stated above.
```

- Verification contribution: 本Step `mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthOidcProtocolContractTest -Dsurefire.failIfNoSpecifiedTests=false test`；此文件负责 consumeAndIssue(TrustedRealmBO,ConsumeOAuthTokenCommand) 的正例与 no user scope implicit all 负例。
- After this file: consumeAndIssue(TrustedRealmBO,ConsumeOAuthTokenCommand) 的字段/入口或注册已落盘；与下一文件组合后本Step才能GREEN，不能把中间状态当完成。

#### File 6 — `CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/service/impl/OAuthTokenIssueServiceImpl.java`

- Purpose: 签发编排与事务状态守卫。
- Symbols: `consumeAndIssue`。
- Repository evidence: 当前不存在；Spec §8/§11 明确目标，邻近包可验证；目标源于主Spec §7–§16。
- Dependencies and consumers: 输入依赖 Step 7 与本Step此前文件；输出供 标准Code/PKCE/consent/login和机器Client Credentials签发保持scope/audience界限及后一Step使用；Java Bean/Mapper按模块现有配置发现，前端按现有auth client消费。
- Why now: 前一文件建立本文件需要的字段或调用契约； 签发编排与事务状态守卫在此位置后才可接消费者。
- Contract/signature changes: consumeAndIssue；签发编排与事务状态守卫。公开行为只取主Spec §9对应API或§10/11内部合同。
- Input/output and state mapping: auth attributes+token rows only within same realm。
- Error and edge behavior: replayed code/RT rollback; no partial token。
- Standards impact: MC-ARCH-001, MC-SCOPE-001, MC-TEST-001, MC-VALID-001；按已选传统分层，输入校验与权限先于状态读取，PO不出边界，相关日志脱敏。
- Literal rule enforcement: Rule 1, Rule 2, Rule 4, Rule 9, Rule 10, Rule 11；此文件直接按所列原始规则落实命名、验证、类型/注入或配置，其他规则由同Step关联文件负责。
- Implementation pseudocode:

```java
INPUT -> auth attributes+token rows only within same realm。
FLOW -> @Service("oauthTokenIssueService") @Slf4j @RequiredArgsConstructor; use ClientCredentialsAccessPolicy; lock realm→client→subject→authorization→token; map grant/context to USER/SERVICE claims; enqueue audit; reject SERVICE RT/IDToken
ON INVALID/CONCURRENT/DEPENDENCY FAILURE -> replayed code/RT rollback; no partial token。
RESULT -> only the named symbol/contract in this File block becomes available; preserve tenant, expectedVersion, scope, UTC time and secret masking stated above.
```

- Verification contribution: 本Step `mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthOidcProtocolContractTest -Dsurefire.failIfNoSpecifiedTests=false test`；此文件负责 consumeAndIssue 的正例与 replayed code/RT rollback; no partial token 负例。
- After this file: consumeAndIssue 的字段/入口或注册已落盘；与下一文件组合后本Step才能GREEN，不能把中间状态当完成。

#### File 7 — `CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/service/impl/TransactionalOAuthAuthenticationProvider.java`

- Purpose: 保持SAS原协议处理但同事务校验。
- Symbols: `authorizeCode/refresh/clientCredentials wrappers`。
- Repository evidence: 当前不存在；Spec §8/§11 明确目标，邻近包可验证；目标源于主Spec §7–§16。
- Dependencies and consumers: 输入依赖 Step 7 与本Step此前文件；输出供 标准Code/PKCE/consent/login和机器Client Credentials签发保持scope/audience界限及后一Step使用；Java Bean/Mapper按模块现有配置发现，前端按现有auth client消费。
- Why now: 前一文件建立本文件需要的字段或调用契约； 保持SAS原协议处理但同事务校验在此位置后才可接消费者。
- Contract/signature changes: authorizeCode/refresh/clientCredentials wrappers；保持SAS原协议处理但同事务校验。公开行为只取主Spec §9对应API或§10/11内部合同。
- Input/output and state mapping: Issuer/Client/securityVersion from trusted context。
- Error and edge behavior: unknown commit invalid_grant; no duplicate issue。
- Standards impact: MC-ARCH-001, MC-SCOPE-001, MC-TEST-001, MC-VALID-001；按已选传统分层，输入校验与权限先于状态读取，PO不出边界，相关日志脱敏。
- Literal rule enforcement: Rule 2, Rule 4, Rule 9, Rule 11；此文件直接按所列原始规则落实命名、验证、类型/注入或配置，其他规则由同Step关联文件负责。
- Implementation pseudocode:

```java
INPUT -> Issuer/Client/securityVersion from trusted context。
FLOW -> @Component("transactionalOAuthAuthenticationProvider") @Slf4j @RequiredArgsConstructor; register three typed delegate wrappers, remove unwrapped path; TransactionTemplate(auth issue + SAS delegate save + outbox); grant-specific Strategy from provider registration not grant_type switch
ON INVALID/CONCURRENT/DEPENDENCY FAILURE -> unknown commit invalid_grant; no duplicate issue。
RESULT -> only the named symbol/contract in this File block becomes available; preserve tenant, expectedVersion, scope, UTC time and secret masking stated above.
```

- Verification contribution: 本Step `mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthOidcProtocolContractTest -Dsurefire.failIfNoSpecifiedTests=false test`；此文件负责 authorizeCode/refresh/clientCredentials wrappers 的正例与 unknown commit invalid_grant; no duplicate issue 负例。
- After this file: authorizeCode/refresh/clientCredentials wrappers 的字段/入口或注册已落盘；与下一文件组合后本Step才能GREEN，不能把中间状态当完成。

#### File 8 — `CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/controller/TenantLoginController.java`

- Purpose: 呈现现有IdentityFacade人员认证页。
- Symbols: `GET /tenants/{tenantId}/login`。
- Repository evidence: 当前不存在；Spec §8/§11 明确目标，邻近包可验证；目标源于主Spec §7–§16。
- Dependencies and consumers: 输入依赖 Step 7 与本Step此前文件；输出供 标准Code/PKCE/consent/login和机器Client Credentials签发保持scope/audience界限及后一Step使用；Java Bean/Mapper按模块现有配置发现，前端按现有auth client消费。
- Why now: 前一文件建立本文件需要的字段或调用契约； 呈现现有IdentityFacade人员认证页在此位置后才可接消费者。
- Contract/signature changes: GET /tenants/{tenantId}/login；呈现现有IdentityFacade人员认证页。公开行为只取主Spec §9对应API或§10/11内部合同。
- Input/output and state mapping: 200 HTML/CSRF, no arbitrary returnTo。
- Error and edge behavior: unknown issuer/expired transaction safe error。
- Standards impact: MC-ARCH-001, MC-SCOPE-001, MC-TEST-001, MC-VALID-001；按已选传统分层，输入校验与权限先于状态读取，PO不出边界，相关日志脱敏。
- Literal rule enforcement: Rule 2, Rule 4, Rule 6, Rule 11；此文件直接按所列原始规则落实命名、验证、类型/注入或配置，其他规则由同Step关联文件负责。
- Implementation pseudocode:

```java
INPUT -> 200 HTML/CSRF, no arbitrary returnTo。
FLOW -> @RestController("tenantLoginController") @Slf4j @RequiredArgsConstructor; only for saved authorization request; tenant from transaction; no token in HTML
ON INVALID/CONCURRENT/DEPENDENCY FAILURE -> unknown issuer/expired transaction safe error。
RESULT -> only the named symbol/contract in this File block becomes available; preserve tenant, expectedVersion, scope, UTC time and secret masking stated above.
```

- Verification contribution: 本Step `mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthOidcProtocolContractTest -Dsurefire.failIfNoSpecifiedTests=false test`；此文件负责 GET /tenants/{tenantId}/login 的正例与 unknown issuer/expired transaction safe error 负例。
- After this file: GET /tenants/{tenantId}/login 的字段/入口或注册已落盘；与下一文件组合后本Step才能GREEN，不能把中间状态当完成。

#### File 9 — `CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/controller/filter/TenantLoginAuthenticationFilter.java`

- Purpose: 使用真实密码/锁定/membership规则。
- Symbols: `POST /tenants/{tenantId}/login`。
- Repository evidence: 当前不存在；Spec §8/§11 明确目标，邻近包可验证；目标源于主Spec §7–§16。
- Dependencies and consumers: 输入依赖 Step 7 与本Step此前文件；输出供 标准Code/PKCE/consent/login和机器Client Credentials签发保持scope/audience界限及后一Step使用；Java Bean/Mapper按模块现有配置发现，前端按现有auth client消费。
- Why now: 前一文件建立本文件需要的字段或调用契约； 使用真实密码/锁定/membership规则在此位置后才可接消费者。
- Contract/signature changes: POST /tenants/{tenantId}/login；使用真实密码/锁定/membership规则。公开行为只取主Spec §9对应API或§10/11内部合同。
- Input/output and state mapping: auth_time Instant/acr from actual credential event。
- Error and edge behavior: wrong login no subject leak; no password grant。
- Standards impact: MC-ARCH-001, MC-SCOPE-001, MC-TEST-001, MC-VALID-001；按已选传统分层，输入校验与权限先于状态读取，PO不出边界，相关日志脱敏。
- Literal rule enforcement: Rule 2, Rule 4, Rule 9, Rule 10, Rule 11；此文件直接按所列原始规则落实命名、验证、类型/注入或配置，其他规则由同Step关联文件负责。
- Implementation pseudocode:

```java
INPUT -> auth_time Instant/acr from actual credential event。
FLOW -> @Component("tenantLoginAuthenticationFilter") @Slf4j @RequiredArgsConstructor; parse normalized username, untouched password; verify CSRF+request; IdentityFacade.authenticate; rotate SAS session; resume saved authorize; handle mustChangePassword via current flow
ON INVALID/CONCURRENT/DEPENDENCY FAILURE -> wrong login no subject leak; no password grant。
RESULT -> only the named symbol/contract in this File block becomes available; preserve tenant, expectedVersion, scope, UTC time and secret masking stated above.
```

- Verification contribution: 本Step `mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthOidcProtocolContractTest -Dsurefire.failIfNoSpecifiedTests=false test`；此文件负责 POST /tenants/{tenantId}/login 的正例与 wrong login no subject leak; no password grant 负例。
- After this file: POST /tenants/{tenantId}/login 的字段/入口或注册已落盘；与下一文件组合后本Step才能GREEN，不能把中间状态当完成。

#### File 10 — `MODIFY egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/config/AuthorizationServerConfiguration.java`

- Purpose: 接入授权码/同意/会话与claims。
- Symbols: `token endpoint provider list`。
- Repository evidence: 当前不存在；Spec §8/§11 明确目标，邻近包可验证；目标源于主Spec §7–§16。
- Dependencies and consumers: 输入依赖 Step 7 与本Step此前文件；输出供 标准Code/PKCE/consent/login和机器Client Credentials签发保持scope/audience界限及后一Step使用；Java Bean/Mapper按模块现有配置发现，前端按现有auth client消费。
- Why now: 前一文件建立本文件需要的字段或调用契约； 接入授权码/同意/会话与claims在此位置后才可接消费者。
- Contract/signature changes: token endpoint provider list；接入授权码/同意/会话与claims。公开行为只取主Spec §9对应API或§10/11内部合同。
- Input/output and state mapping: IDToken client aud; AT target resource aud; USER vs SERVICE。
- Error and edge behavior: typ/kid/issuer mismatch reject and do not publish unauthorized endpoint。
- Standards impact: MC-ARCH-001, MC-SCOPE-001, MC-TEST-001, MC-VALID-001；按已选传统分层，输入校验与权限先于状态读取，PO不出边界，相关日志脱敏。
- Literal rule enforcement: Rule 2, Rule 4, Rule 6, Rule 9, Rule 10, Rule 11；此文件直接按所列原始规则落实命名、验证、类型/注入或配置，其他规则由同Step关联文件负责。
- Implementation pseudocode:

```java
INPUT -> IDToken client aud; AT target resource aud; USER vs SERVICE。
FLOW -> enable Code+S256 only; TokenSettings 300–600s; OIDC id_token nonce; Basic bcrypt; resourceUri exact; issue JWT iss/aud/sub/scope/tenant_id and security_version
ON INVALID/CONCURRENT/DEPENDENCY FAILURE -> typ/kid/issuer mismatch reject and do not publish unauthorized endpoint。
RESULT -> only the named symbol/contract in this File block becomes available; preserve tenant, expectedVersion, scope, UTC time and secret masking stated above.
```

- Verification contribution: 本Step `mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthOidcProtocolContractTest -Dsurefire.failIfNoSpecifiedTests=false test`；此文件负责 token endpoint provider list 的正例与 typ/kid/issuer mismatch reject and do not publish unauthorized endpoint 负例。
- After this file: token endpoint provider list 的字段/入口或注册已落盘；与下一文件组合后本Step才能GREEN，不能把中间状态当完成。

- Validation working directory: `/Users/mario/SelfProject/Egon-COLA`。
- Verification command: `mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthOidcProtocolContractTest -Dsurefire.failIfNoSpecifiedTests=false test`。
- Expected result: 先确认上述RED由缺失目标行为引起，GREEN时命名用例成功、模块编译成功、`git diff --check`零错误；不以不存在依赖/数据库/fixture而失败作为RED。
- Failure returns to: Step 7 的编译/Schema前置；若需改变外部语义、架构或DDL版本则回主Spec。
- Completion criteria: 标准Code/PKCE/consent/login和机器Client Credentials签发保持scope/audience界限；本Step的所有Manual Checks在实际执行时给出证据，未通过不能提交。
- Rollback: 提交前仅恢复本Step的精确路径；DDL已对真实数据运行后只forward-fix和新版本，不能编辑既有SQL/历史或恢复旧安全世代。
- Commit paths: `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/config/OAuthConfig.java`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/config/TokenConfig.java`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/domain/command/ConsumeOAuthTokenCommand.java`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/controller/OAuthOidcProtocolContractTest.java`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/service/OAuthTokenIssueService.java`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/service/impl/OAuthTokenIssueServiceImpl.java`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/service/impl/TransactionalOAuthAuthenticationProvider.java`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/controller/TenantLoginController.java`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/controller/filter/TenantLoginAuthenticationFilter.java`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/config/AuthorizationServerConfiguration.java`。
- Commit: `feat(shoubing): implement code pkce oidc and client grants`。

### Step 9 — 改造客户端、资源与密钥管理

- Requirements: REQ-002, REQ-005, REQ-011, REQ-013, REQ-017
- Dependencies: Step 8
- Baseline state: 当前HEAD的对应源码/测试如每File的Repository evidence，先前Step已完成并有其focused GREEN；本Step尚不存在realmIsolatedBcryptRotation所列新行为。
- Observable outcome: 现有管理URL按realm管理Client/Grant/Key；secret仅bcrypt一次性显示。
- End state: 现有管理URL按realm管理Client/Grant/Key；secret仅bcrypt一次性显示；后续Step的Consumer尚未切流，不声明完整生产验收。
- Test-first gate: Required — 当前JPA/Argon2/全局资源UI路径使租户/密钥测试RED，修最小管理链后GREEN。
- Manual Checks: MC-ARCH-001, MC-REUSE-001, MC-SCOPE-001, MC-TEST-001, MC-VALID-001, MC-NAME-001, MC-MODEL-001, MC-CONVERT-001, MC-LOG-001, MC-BEAN-001, MC-JSON-001, MC-TIME-001
- Literal Rules: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11
- Ordered files:

#### File 1 — `MODIFY egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/controller/OAuthClientControllerTest.java`

- Purpose: RED现有全局client/Argon2路径。
- Symbols: `realmIsolatedBcryptRotation`。
- Repository evidence: 当前文件存在：egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/controller/OAuthClientControllerTest.java；目标源于主Spec §7–§16。
- Dependencies and consumers: 输入依赖 Step 8 与本Step此前文件；输出供 现有管理URL按realm管理Client/Grant/Key；secret仅bcrypt一次性显示及后一Step使用；Java Bean/Mapper按模块现有配置发现，前端按现有auth client消费。
- Why now: 本Step的编译前置/RED契约先锁定； RED现有全局client/Argon2路径在此位置后才可接消费者。
- Contract/signature changes: realmIsolatedBcryptRotation；RED现有全局client/Argon2路径。公开行为只取主Spec §9对应API或§10/11内部合同。
- Input/output and state mapping: existing controller route/permission fixtures。
- Error and edge behavior: current all-client listing and Argon2 make RED。
- Standards impact: MC-ARCH-001, MC-SCOPE-001, MC-TEST-001, MC-VALID-001；按已选传统分层，输入校验与权限先于状态读取，PO不出边界，相关日志脱敏。
- Literal rule enforcement: Rule 11, Rule 2, Rule 4；此文件直接按所列原始规则落实命名、验证、类型/注入或配置，其他规则由同Step关联文件负责。
- Implementation pseudocode:

```java
INPUT -> existing controller route/permission fixtures。
FLOW -> same clientId tenant A/B distinct hashes; invalid realm/redirect/scope/version; created/rotated secret shown once; old hash rejected by new realm
ON INVALID/CONCURRENT/DEPENDENCY FAILURE -> current all-client listing and Argon2 make RED。
RESULT -> only the named symbol/contract in this File block becomes available; preserve tenant, expectedVersion, scope, UTC time and secret masking stated above.
```

- Verification contribution: 本Step `mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthClientControllerTest,SigningKeyServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test`；此文件负责 realmIsolatedBcryptRotation 的正例与 current all-client listing and Argon2 make RED 负例。
- After this file: realmIsolatedBcryptRotation 的字段/入口或注册已落盘；与下一文件组合后本Step才能GREEN，不能把中间状态当完成。

#### File 2 — `CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/domain/converter/OAuthClientViewConverter.java`

- Purpose: 关闭 `OAuthClientViewConverter` 在主Spec中的字段/消费依赖。
- Symbols: `OAuthClientViewConverter`。
- Repository evidence: 当前不存在；主Spec §9.2/§10.2 命名此对象/字段；现有相邻业务包提供放置证据。
- Dependencies and consumers: 本Step既有File块调用该类型；数据来源/消费者为主Spec §9.2/§10.2，不产生新HTTP路由。
- Why now: 该类型按本Step的测试/编译依赖顺序落位，以免由实现者临时发明。
- Contract/signature changes: BaseForwardConverter<OAuthClientBO,OAuthClientVO>; omit secret_hash; issuer/secretHint/null PUBLIC mapping。
- Input/output and state mapping: 精确字段由主Spec §9.2/§10.2 指定，realm从签名/注册得出，Instant为UTC，版本单调，秘密字段仅服务器内部。
- Error and edge behavior: null/未知字段/跨tenant/重复版本拒绝；不转成宽松缺省或吞异常。
- Standards impact: MC-ARCH-001, MC-NAME-001, MC-VALID-001, MC-MODEL-001, MC-CONVERT-001, MC-BEAN-001, MC-JSON-001, MC-TIME-001, MC-SCOPE-001, MC-TEST-001；行为Bean使用具名final Qualifier、@RequiredArgsConstructor和@Slf4j，载体遵循四注解与Builder。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 6, Rule 9, Rule 10, Rule 11；class按POJO语义命名，边界注解验证，跨层转换只用既有BaseConverter，Jackson和java.time字段明确。
- Implementation pseudocode:

```java
INPUT -> 仅接受主Spec §9.2/§10.2 的可信issuer/realm/输入字段；调用前 @Valid 或 ValueObject不变量检查。
FLOW -> BaseForwardConverter<OAuthClientBO,OAuthClientVO>; omit secret_hash; issuer/secretHint/null PUBLIC mapping。
OUTPUT -> OAuthClientViewConverter 的Java方法、DTO字段或配置可被本Step既有调用者编译使用；不新增业务API。
FAIL -> 空值/跨tenant/未知schema/重复版本/外部依赖错误按本Step已列错误模型fail-closed，不能输出原secret/token。
```

- Verification contribution: Step 9 的focus测试对 `OAuthClientViewConverter` 的字段、装配、负例及不泄密断言。
- After this file: `OAuthClientViewConverter` 成为已声明的Step依赖，下一文件可按原合同引用；未完成整条行为前不宣称GREEN。

#### File 3 — `CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/resource/domain/converter/ClientResourceGrantViewConverter.java`

- Purpose: 关闭 `ClientResourceGrantViewConverter` 在主Spec中的字段/消费依赖。
- Symbols: `ClientResourceGrantViewConverter`。
- Repository evidence: 当前不存在；主Spec §9.2/§10.2 命名此对象/字段；现有相邻业务包提供放置证据。
- Dependencies and consumers: 本Step既有File块调用该类型；数据来源/消费者为主Spec §9.2/§10.2，不产生新HTTP路由。
- Why now: 该类型按本Step的测试/编译依赖顺序落位，以免由实现者临时发明。
- Contract/signature changes: BaseForwardConverter<OAuthGrantBO,ClientResourceGrantVO>; tenant from validated realm, scope explicit。
- Input/output and state mapping: 精确字段由主Spec §9.2/§10.2 指定，realm从签名/注册得出，Instant为UTC，版本单调，秘密字段仅服务器内部。
- Error and edge behavior: null/未知字段/跨tenant/重复版本拒绝；不转成宽松缺省或吞异常。
- Standards impact: MC-ARCH-001, MC-NAME-001, MC-VALID-001, MC-MODEL-001, MC-CONVERT-001, MC-BEAN-001, MC-JSON-001, MC-TIME-001, MC-SCOPE-001, MC-TEST-001；行为Bean使用具名final Qualifier、@RequiredArgsConstructor和@Slf4j，载体遵循四注解与Builder。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 6, Rule 9, Rule 10, Rule 11；class按POJO语义命名，边界注解验证，跨层转换只用既有BaseConverter，Jackson和java.time字段明确。
- Implementation pseudocode:

```java
INPUT -> 仅接受主Spec §9.2/§10.2 的可信issuer/realm/输入字段；调用前 @Valid 或 ValueObject不变量检查。
FLOW -> BaseForwardConverter<OAuthGrantBO,ClientResourceGrantVO>; tenant from validated realm, scope explicit。
OUTPUT -> ClientResourceGrantViewConverter 的Java方法、DTO字段或配置可被本Step既有调用者编译使用；不新增业务API。
FAIL -> 空值/跨tenant/未知schema/重复版本/外部依赖错误按本Step已列错误模型fail-closed，不能输出原secret/token。
```

- Verification contribution: Step 9 的focus测试对 `ClientResourceGrantViewConverter` 的字段、装配、负例及不泄密断言。
- After this file: `ClientResourceGrantViewConverter` 成为已声明的Step依赖，下一文件可按原合同引用；未完成整条行为前不宣称GREEN。

#### File 4 — `CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/domain/converter/SigningKeyViewConverter.java`

- Purpose: 关闭 `SigningKeyViewConverter` 在主Spec中的字段/消费依赖。
- Symbols: `SigningKeyViewConverter`。
- Repository evidence: 当前不存在；主Spec §9.2/§10.2 命名此对象/字段；现有相邻业务包提供放置证据。
- Dependencies and consumers: 本Step既有File块调用该类型；数据来源/消费者为主Spec §9.2/§10.2，不产生新HTTP路由。
- Why now: 该类型按本Step的测试/编译依赖顺序落位，以免由实现者临时发明。
- Contract/signature changes: BaseForwardConverter<OAuthSigningKeyBO,SigningKeyVO>; public JWK/status/version only, private ciphertext excluded。
- Input/output and state mapping: 精确字段由主Spec §9.2/§10.2 指定，realm从签名/注册得出，Instant为UTC，版本单调，秘密字段仅服务器内部。
- Error and edge behavior: null/未知字段/跨tenant/重复版本拒绝；不转成宽松缺省或吞异常。
- Standards impact: MC-ARCH-001, MC-NAME-001, MC-VALID-001, MC-MODEL-001, MC-CONVERT-001, MC-BEAN-001, MC-JSON-001, MC-TIME-001, MC-SCOPE-001, MC-TEST-001；行为Bean使用具名final Qualifier、@RequiredArgsConstructor和@Slf4j，载体遵循四注解与Builder。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 6, Rule 9, Rule 10, Rule 11；class按POJO语义命名，边界注解验证，跨层转换只用既有BaseConverter，Jackson和java.time字段明确。
- Implementation pseudocode:

```java
INPUT -> 仅接受主Spec §9.2/§10.2 的可信issuer/realm/输入字段；调用前 @Valid 或 ValueObject不变量检查。
FLOW -> BaseForwardConverter<OAuthSigningKeyBO,SigningKeyVO>; public JWK/status/version only, private ciphertext excluded。
OUTPUT -> SigningKeyViewConverter 的Java方法、DTO字段或配置可被本Step既有调用者编译使用；不新增业务API。
FAIL -> 空值/跨tenant/未知schema/重复版本/外部依赖错误按本Step已列错误模型fail-closed，不能输出原secret/token。
```

- Verification contribution: Step 9 的focus测试对 `SigningKeyViewConverter` 的字段、装配、负例及不泄密断言。
- After this file: `SigningKeyViewConverter` 成为已声明的Step依赖，下一文件可按原合同引用；未完成整条行为前不宣称GREEN。

#### File 5 — `CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/resource/domain/converter/ResourceServerViewConverter.java`

- Purpose: 关闭 `ResourceServerViewConverter` 在主Spec中的字段/消费依赖。
- Symbols: `ResourceServerViewConverter`。
- Repository evidence: 当前不存在；主Spec §9.2/§10.2 命名此对象/字段；现有相邻业务包提供放置证据。
- Dependencies and consumers: 本Step既有File块调用该类型；数据来源/消费者为主Spec §9.2/§10.2，不产生新HTTP路由。
- Why now: 该类型按本Step的测试/编译依赖顺序落位，以免由实现者临时发明。
- Contract/signature changes: BaseForwardConverter<OAuthResourceBO,ResourceServerVO>; external resourceServerId/URI/source/RBAC fields stable。
- Input/output and state mapping: 精确字段由主Spec §9.2/§10.2 指定，realm从签名/注册得出，Instant为UTC，版本单调，秘密字段仅服务器内部。
- Error and edge behavior: null/未知字段/跨tenant/重复版本拒绝；不转成宽松缺省或吞异常。
- Standards impact: MC-ARCH-001, MC-NAME-001, MC-VALID-001, MC-MODEL-001, MC-CONVERT-001, MC-BEAN-001, MC-JSON-001, MC-TIME-001, MC-SCOPE-001, MC-TEST-001；行为Bean使用具名final Qualifier、@RequiredArgsConstructor和@Slf4j，载体遵循四注解与Builder。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 6, Rule 9, Rule 10, Rule 11；class按POJO语义命名，边界注解验证，跨层转换只用既有BaseConverter，Jackson和java.time字段明确。
- Implementation pseudocode:

```java
INPUT -> 仅接受主Spec §9.2/§10.2 的可信issuer/realm/输入字段；调用前 @Valid 或 ValueObject不变量检查。
FLOW -> BaseForwardConverter<OAuthResourceBO,ResourceServerVO>; external resourceServerId/URI/source/RBAC fields stable。
OUTPUT -> ResourceServerViewConverter 的Java方法、DTO字段或配置可被本Step既有调用者编译使用；不新增业务API。
FAIL -> 空值/跨tenant/未知schema/重复版本/外部依赖错误按本Step已列错误模型fail-closed，不能输出原secret/token。
```

- Verification contribution: Step 9 的focus测试对 `ResourceServerViewConverter` 的字段、装配、负例及不泄密断言。
- After this file: `ResourceServerViewConverter` 成为已声明的Step依赖，下一文件可按原合同引用；未完成整条行为前不宣称GREEN。

#### File 6 — `MODIFY egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/domain/vo/OAuthClientVO.java`

- Purpose: 关闭 `OAuthClientVO` 在主Spec中的字段/消费依赖。
- Symbols: `OAuthClientVO`。
- Repository evidence: 当前同名文件存在，符号/消费者由rg确认。
- Dependencies and consumers: 本Step既有File块调用该类型；数据来源/消费者为主Spec §9.2/§10.2，不产生新HTTP路由。
- Why now: 该类型按本Step的测试/编译依赖顺序落位，以免由实现者临时发明。
- Contract/signature changes: migrate materially changed vo record to mandatory class Lombok set; preserve Spec §9 JSON field names/null/default, @Valid groups, @JsonIgnore sensitive。
- Input/output and state mapping: 精确字段由主Spec §9.2/§10.2 指定，realm从签名/注册得出，Instant为UTC，版本单调，秘密字段仅服务器内部。
- Error and edge behavior: null/未知字段/跨tenant/重复版本拒绝；不转成宽松缺省或吞异常。
- Standards impact: MC-ARCH-001, MC-NAME-001, MC-VALID-001, MC-MODEL-001, MC-CONVERT-001, MC-BEAN-001, MC-JSON-001, MC-TIME-001, MC-SCOPE-001, MC-TEST-001；行为Bean使用具名final Qualifier、@RequiredArgsConstructor和@Slf4j，载体遵循四注解与Builder。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 6, Rule 9, Rule 10, Rule 11；class按POJO语义命名，边界注解验证，跨层转换只用既有BaseConverter，Jackson和java.time字段明确。
- Implementation pseudocode:

```java
INPUT -> 仅接受主Spec §9.2/§10.2 的可信issuer/realm/输入字段；调用前 @Valid 或 ValueObject不变量检查。
FLOW -> migrate materially changed vo record to mandatory class Lombok set; preserve Spec §9 JSON field names/null/default, @Valid groups, @JsonIgnore sensitive。
OUTPUT -> OAuthClientVO 的Java方法、DTO字段或配置可被本Step既有调用者编译使用；不新增业务API。
FAIL -> 空值/跨tenant/未知schema/重复版本/外部依赖错误按本Step已列错误模型fail-closed，不能输出原secret/token。
```

- Verification contribution: Step 9 的focus测试对 `OAuthClientVO` 的字段、装配、负例及不泄密断言。
- After this file: `OAuthClientVO` 成为已声明的Step依赖，下一文件可按原合同引用；未完成整条行为前不宣称GREEN。

#### File 7 — `MODIFY egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/domain/vo/CreatedOAuthClientVO.java`

- Purpose: 关闭 `CreatedOAuthClientVO` 在主Spec中的字段/消费依赖。
- Symbols: `CreatedOAuthClientVO`。
- Repository evidence: 当前同名文件存在，符号/消费者由rg确认。
- Dependencies and consumers: 本Step既有File块调用该类型；数据来源/消费者为主Spec §9.2/§10.2，不产生新HTTP路由。
- Why now: 该类型按本Step的测试/编译依赖顺序落位，以免由实现者临时发明。
- Contract/signature changes: migrate materially changed vo record to mandatory class Lombok set; preserve Spec §9 JSON field names/null/default, @Valid groups, @JsonIgnore sensitive。
- Input/output and state mapping: 精确字段由主Spec §9.2/§10.2 指定，realm从签名/注册得出，Instant为UTC，版本单调，秘密字段仅服务器内部。
- Error and edge behavior: null/未知字段/跨tenant/重复版本拒绝；不转成宽松缺省或吞异常。
- Standards impact: MC-ARCH-001, MC-NAME-001, MC-VALID-001, MC-MODEL-001, MC-CONVERT-001, MC-BEAN-001, MC-JSON-001, MC-TIME-001, MC-SCOPE-001, MC-TEST-001；行为Bean使用具名final Qualifier、@RequiredArgsConstructor和@Slf4j，载体遵循四注解与Builder。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 6, Rule 9, Rule 10, Rule 11；class按POJO语义命名，边界注解验证，跨层转换只用既有BaseConverter，Jackson和java.time字段明确。
- Implementation pseudocode:

```java
INPUT -> 仅接受主Spec §9.2/§10.2 的可信issuer/realm/输入字段；调用前 @Valid 或 ValueObject不变量检查。
FLOW -> migrate materially changed vo record to mandatory class Lombok set; preserve Spec §9 JSON field names/null/default, @Valid groups, @JsonIgnore sensitive。
OUTPUT -> CreatedOAuthClientVO 的Java方法、DTO字段或配置可被本Step既有调用者编译使用；不新增业务API。
FAIL -> 空值/跨tenant/未知schema/重复版本/外部依赖错误按本Step已列错误模型fail-closed，不能输出原secret/token。
```

- Verification contribution: Step 9 的focus测试对 `CreatedOAuthClientVO` 的字段、装配、负例及不泄密断言。
- After this file: `CreatedOAuthClientVO` 成为已声明的Step依赖，下一文件可按原合同引用；未完成整条行为前不宣称GREEN。

#### File 8 — `MODIFY egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/domain/vo/RotatedClientSecretVO.java`

- Purpose: 关闭 `RotatedClientSecretVO` 在主Spec中的字段/消费依赖。
- Symbols: `RotatedClientSecretVO`。
- Repository evidence: 当前同名文件存在，符号/消费者由rg确认。
- Dependencies and consumers: 本Step既有File块调用该类型；数据来源/消费者为主Spec §9.2/§10.2，不产生新HTTP路由。
- Why now: 该类型按本Step的测试/编译依赖顺序落位，以免由实现者临时发明。
- Contract/signature changes: migrate materially changed vo record to mandatory class Lombok set; preserve Spec §9 JSON field names/null/default, @Valid groups, @JsonIgnore sensitive。
- Input/output and state mapping: 精确字段由主Spec §9.2/§10.2 指定，realm从签名/注册得出，Instant为UTC，版本单调，秘密字段仅服务器内部。
- Error and edge behavior: null/未知字段/跨tenant/重复版本拒绝；不转成宽松缺省或吞异常。
- Standards impact: MC-ARCH-001, MC-NAME-001, MC-VALID-001, MC-MODEL-001, MC-CONVERT-001, MC-BEAN-001, MC-JSON-001, MC-TIME-001, MC-SCOPE-001, MC-TEST-001；行为Bean使用具名final Qualifier、@RequiredArgsConstructor和@Slf4j，载体遵循四注解与Builder。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 6, Rule 9, Rule 10, Rule 11；class按POJO语义命名，边界注解验证，跨层转换只用既有BaseConverter，Jackson和java.time字段明确。
- Implementation pseudocode:

```java
INPUT -> 仅接受主Spec §9.2/§10.2 的可信issuer/realm/输入字段；调用前 @Valid 或 ValueObject不变量检查。
FLOW -> migrate materially changed vo record to mandatory class Lombok set; preserve Spec §9 JSON field names/null/default, @Valid groups, @JsonIgnore sensitive。
OUTPUT -> RotatedClientSecretVO 的Java方法、DTO字段或配置可被本Step既有调用者编译使用；不新增业务API。
FAIL -> 空值/跨tenant/未知schema/重复版本/外部依赖错误按本Step已列错误模型fail-closed，不能输出原secret/token。
```

- Verification contribution: Step 9 的focus测试对 `RotatedClientSecretVO` 的字段、装配、负例及不泄密断言。
- After this file: `RotatedClientSecretVO` 成为已声明的Step依赖，下一文件可按原合同引用；未完成整条行为前不宣称GREEN。

#### File 9 — `MODIFY egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/resource/domain/vo/ClientResourceGrantVO.java`

- Purpose: 关闭 `ClientResourceGrantVO` 在主Spec中的字段/消费依赖。
- Symbols: `ClientResourceGrantVO`。
- Repository evidence: 当前同名文件存在，符号/消费者由rg确认。
- Dependencies and consumers: 本Step既有File块调用该类型；数据来源/消费者为主Spec §9.2/§10.2，不产生新HTTP路由。
- Why now: 该类型按本Step的测试/编译依赖顺序落位，以免由实现者临时发明。
- Contract/signature changes: migrate materially changed vo record to mandatory class Lombok set; preserve Spec §9 JSON field names/null/default, @Valid groups, @JsonIgnore sensitive。
- Input/output and state mapping: 精确字段由主Spec §9.2/§10.2 指定，realm从签名/注册得出，Instant为UTC，版本单调，秘密字段仅服务器内部。
- Error and edge behavior: null/未知字段/跨tenant/重复版本拒绝；不转成宽松缺省或吞异常。
- Standards impact: MC-ARCH-001, MC-NAME-001, MC-VALID-001, MC-MODEL-001, MC-CONVERT-001, MC-BEAN-001, MC-JSON-001, MC-TIME-001, MC-SCOPE-001, MC-TEST-001；行为Bean使用具名final Qualifier、@RequiredArgsConstructor和@Slf4j，载体遵循四注解与Builder。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 6, Rule 9, Rule 10, Rule 11；class按POJO语义命名，边界注解验证，跨层转换只用既有BaseConverter，Jackson和java.time字段明确。
- Implementation pseudocode:

```java
INPUT -> 仅接受主Spec §9.2/§10.2 的可信issuer/realm/输入字段；调用前 @Valid 或 ValueObject不变量检查。
FLOW -> migrate materially changed vo record to mandatory class Lombok set; preserve Spec §9 JSON field names/null/default, @Valid groups, @JsonIgnore sensitive。
OUTPUT -> ClientResourceGrantVO 的Java方法、DTO字段或配置可被本Step既有调用者编译使用；不新增业务API。
FAIL -> 空值/跨tenant/未知schema/重复版本/外部依赖错误按本Step已列错误模型fail-closed，不能输出原secret/token。
```

- Verification contribution: Step 9 的focus测试对 `ClientResourceGrantVO` 的字段、装配、负例及不泄密断言。
- After this file: `ClientResourceGrantVO` 成为已声明的Step依赖，下一文件可按原合同引用；未完成整条行为前不宣称GREEN。

#### File 10 — `MODIFY egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/resource/domain/vo/ResourceServerVO.java`

- Purpose: 关闭 `ResourceServerVO` 在主Spec中的字段/消费依赖。
- Symbols: `ResourceServerVO`。
- Repository evidence: 当前同名文件存在，符号/消费者由rg确认。
- Dependencies and consumers: 本Step既有File块调用该类型；数据来源/消费者为主Spec §9.2/§10.2，不产生新HTTP路由。
- Why now: 该类型按本Step的测试/编译依赖顺序落位，以免由实现者临时发明。
- Contract/signature changes: migrate materially changed vo record to mandatory class Lombok set; preserve Spec §9 JSON field names/null/default, @Valid groups, @JsonIgnore sensitive。
- Input/output and state mapping: 精确字段由主Spec §9.2/§10.2 指定，realm从签名/注册得出，Instant为UTC，版本单调，秘密字段仅服务器内部。
- Error and edge behavior: null/未知字段/跨tenant/重复版本拒绝；不转成宽松缺省或吞异常。
- Standards impact: MC-ARCH-001, MC-NAME-001, MC-VALID-001, MC-MODEL-001, MC-CONVERT-001, MC-BEAN-001, MC-JSON-001, MC-TIME-001, MC-SCOPE-001, MC-TEST-001；行为Bean使用具名final Qualifier、@RequiredArgsConstructor和@Slf4j，载体遵循四注解与Builder。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 6, Rule 9, Rule 10, Rule 11；class按POJO语义命名，边界注解验证，跨层转换只用既有BaseConverter，Jackson和java.time字段明确。
- Implementation pseudocode:

```java
INPUT -> 仅接受主Spec §9.2/§10.2 的可信issuer/realm/输入字段；调用前 @Valid 或 ValueObject不变量检查。
FLOW -> migrate materially changed vo record to mandatory class Lombok set; preserve Spec §9 JSON field names/null/default, @Valid groups, @JsonIgnore sensitive。
OUTPUT -> ResourceServerVO 的Java方法、DTO字段或配置可被本Step既有调用者编译使用；不新增业务API。
FAIL -> 空值/跨tenant/未知schema/重复版本/外部依赖错误按本Step已列错误模型fail-closed，不能输出原secret/token。
```

- Verification contribution: Step 9 的focus测试对 `ResourceServerVO` 的字段、装配、负例及不泄密断言。
- After this file: `ResourceServerVO` 成为已声明的Step依赖，下一文件可按原合同引用；未完成整条行为前不宣称GREEN。

#### File 11 — `MODIFY egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/domain/vo/SigningKeyVO.java`

- Purpose: 关闭 `SigningKeyVO` 在主Spec中的字段/消费依赖。
- Symbols: `SigningKeyVO`。
- Repository evidence: 当前同名文件存在，符号/消费者由rg确认。
- Dependencies and consumers: 本Step既有File块调用该类型；数据来源/消费者为主Spec §9.2/§10.2，不产生新HTTP路由。
- Why now: 该类型按本Step的测试/编译依赖顺序落位，以免由实现者临时发明。
- Contract/signature changes: migrate materially changed vo record to mandatory class Lombok set; preserve Spec §9 JSON field names/null/default, @Valid groups, @JsonIgnore sensitive。
- Input/output and state mapping: 精确字段由主Spec §9.2/§10.2 指定，realm从签名/注册得出，Instant为UTC，版本单调，秘密字段仅服务器内部。
- Error and edge behavior: null/未知字段/跨tenant/重复版本拒绝；不转成宽松缺省或吞异常。
- Standards impact: MC-ARCH-001, MC-NAME-001, MC-VALID-001, MC-MODEL-001, MC-CONVERT-001, MC-BEAN-001, MC-JSON-001, MC-TIME-001, MC-SCOPE-001, MC-TEST-001；行为Bean使用具名final Qualifier、@RequiredArgsConstructor和@Slf4j，载体遵循四注解与Builder。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 6, Rule 9, Rule 10, Rule 11；class按POJO语义命名，边界注解验证，跨层转换只用既有BaseConverter，Jackson和java.time字段明确。
- Implementation pseudocode:

```java
INPUT -> 仅接受主Spec §9.2/§10.2 的可信issuer/realm/输入字段；调用前 @Valid 或 ValueObject不变量检查。
FLOW -> migrate materially changed vo record to mandatory class Lombok set; preserve Spec §9 JSON field names/null/default, @Valid groups, @JsonIgnore sensitive。
OUTPUT -> SigningKeyVO 的Java方法、DTO字段或配置可被本Step既有调用者编译使用；不新增业务API。
FAIL -> 空值/跨tenant/未知schema/重复版本/外部依赖错误按本Step已列错误模型fail-closed，不能输出原secret/token。
```

- Verification contribution: Step 9 的focus测试对 `SigningKeyVO` 的字段、装配、负例及不泄密断言。
- After this file: `SigningKeyVO` 成为已声明的Step依赖，下一文件可按原合同引用；未完成整条行为前不宣称GREEN。

#### File 12 — `MODIFY egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/domain/dto/RotateClientSecretDTO.java`

- Purpose: 关闭 `RotateClientSecretDTO` 在主Spec中的字段/消费依赖。
- Symbols: `RotateClientSecretDTO`。
- Repository evidence: 当前同名文件存在，符号/消费者由rg确认。
- Dependencies and consumers: 本Step既有File块调用该类型；数据来源/消费者为主Spec §9.2/§10.2，不产生新HTTP路由。
- Why now: 该类型按本Step的测试/编译依赖顺序落位，以免由实现者临时发明。
- Contract/signature changes: migrate materially changed dto record to mandatory class Lombok set; preserve Spec §9 JSON field names/null/default, @Valid groups, @JsonIgnore sensitive。
- Input/output and state mapping: 精确字段由主Spec §9.2/§10.2 指定，realm从签名/注册得出，Instant为UTC，版本单调，秘密字段仅服务器内部。
- Error and edge behavior: null/未知字段/跨tenant/重复版本拒绝；不转成宽松缺省或吞异常。
- Standards impact: MC-ARCH-001, MC-NAME-001, MC-VALID-001, MC-MODEL-001, MC-CONVERT-001, MC-BEAN-001, MC-JSON-001, MC-TIME-001, MC-SCOPE-001, MC-TEST-001；行为Bean使用具名final Qualifier、@RequiredArgsConstructor和@Slf4j，载体遵循四注解与Builder。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 6, Rule 9, Rule 10, Rule 11；class按POJO语义命名，边界注解验证，跨层转换只用既有BaseConverter，Jackson和java.time字段明确。
- Implementation pseudocode:

```java
INPUT -> 仅接受主Spec §9.2/§10.2 的可信issuer/realm/输入字段；调用前 @Valid 或 ValueObject不变量检查。
FLOW -> migrate materially changed dto record to mandatory class Lombok set; preserve Spec §9 JSON field names/null/default, @Valid groups, @JsonIgnore sensitive。
OUTPUT -> RotateClientSecretDTO 的Java方法、DTO字段或配置可被本Step既有调用者编译使用；不新增业务API。
FAIL -> 空值/跨tenant/未知schema/重复版本/外部依赖错误按本Step已列错误模型fail-closed，不能输出原secret/token。
```

- Verification contribution: Step 9 的focus测试对 `RotateClientSecretDTO` 的字段、装配、负例及不泄密断言。
- After this file: `RotateClientSecretDTO` 成为已声明的Step依赖，下一文件可按原合同引用；未完成整条行为前不宣称GREEN。

#### File 13 — `MODIFY egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/domain/dto/OAuthValueDTO.java`

- Purpose: 关闭 `OAuthValueDTO` 在主Spec中的字段/消费依赖。
- Symbols: `OAuthValueDTO`。
- Repository evidence: 当前同名文件存在，符号/消费者由rg确认。
- Dependencies and consumers: 本Step既有File块调用该类型；数据来源/消费者为主Spec §9.2/§10.2，不产生新HTTP路由。
- Why now: 该类型按本Step的测试/编译依赖顺序落位，以免由实现者临时发明。
- Contract/signature changes: migrate materially changed dto record to mandatory class Lombok set; preserve Spec §9 JSON field names/null/default, @Valid groups, @JsonIgnore sensitive。
- Input/output and state mapping: 精确字段由主Spec §9.2/§10.2 指定，realm从签名/注册得出，Instant为UTC，版本单调，秘密字段仅服务器内部。
- Error and edge behavior: null/未知字段/跨tenant/重复版本拒绝；不转成宽松缺省或吞异常。
- Standards impact: MC-ARCH-001, MC-NAME-001, MC-VALID-001, MC-MODEL-001, MC-CONVERT-001, MC-BEAN-001, MC-JSON-001, MC-TIME-001, MC-SCOPE-001, MC-TEST-001；行为Bean使用具名final Qualifier、@RequiredArgsConstructor和@Slf4j，载体遵循四注解与Builder。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 6, Rule 9, Rule 10, Rule 11；class按POJO语义命名，边界注解验证，跨层转换只用既有BaseConverter，Jackson和java.time字段明确。
- Implementation pseudocode:

```java
INPUT -> 仅接受主Spec §9.2/§10.2 的可信issuer/realm/输入字段；调用前 @Valid 或 ValueObject不变量检查。
FLOW -> migrate materially changed dto record to mandatory class Lombok set; preserve Spec §9 JSON field names/null/default, @Valid groups, @JsonIgnore sensitive。
OUTPUT -> OAuthValueDTO 的Java方法、DTO字段或配置可被本Step既有调用者编译使用；不新增业务API。
FAIL -> 空值/跨tenant/未知schema/重复版本/外部依赖错误按本Step已列错误模型fail-closed，不能输出原secret/token。
```

- Verification contribution: Step 9 的focus测试对 `OAuthValueDTO` 的字段、装配、负例及不泄密断言。
- After this file: `OAuthValueDTO` 成为已声明的Step依赖，下一文件可按原合同引用；未完成整条行为前不宣称GREEN。

#### File 14 — `MODIFY egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/resource/domain/dto/UpsertClientResourceGrantDTO.java`

- Purpose: 关闭 `UpsertClientResourceGrantDTO` 在主Spec中的字段/消费依赖。
- Symbols: `UpsertClientResourceGrantDTO`。
- Repository evidence: 当前同名文件存在，符号/消费者由rg确认。
- Dependencies and consumers: 本Step既有File块调用该类型；数据来源/消费者为主Spec §9.2/§10.2，不产生新HTTP路由。
- Why now: 该类型按本Step的测试/编译依赖顺序落位，以免由实现者临时发明。
- Contract/signature changes: migrate materially changed dto record to mandatory class Lombok set; preserve Spec §9 JSON field names/null/default, @Valid groups, @JsonIgnore sensitive。
- Input/output and state mapping: 精确字段由主Spec §9.2/§10.2 指定，realm从签名/注册得出，Instant为UTC，版本单调，秘密字段仅服务器内部。
- Error and edge behavior: null/未知字段/跨tenant/重复版本拒绝；不转成宽松缺省或吞异常。
- Standards impact: MC-ARCH-001, MC-NAME-001, MC-VALID-001, MC-MODEL-001, MC-CONVERT-001, MC-BEAN-001, MC-JSON-001, MC-TIME-001, MC-SCOPE-001, MC-TEST-001；行为Bean使用具名final Qualifier、@RequiredArgsConstructor和@Slf4j，载体遵循四注解与Builder。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 6, Rule 9, Rule 10, Rule 11；class按POJO语义命名，边界注解验证，跨层转换只用既有BaseConverter，Jackson和java.time字段明确。
- Implementation pseudocode:

```java
INPUT -> 仅接受主Spec §9.2/§10.2 的可信issuer/realm/输入字段；调用前 @Valid 或 ValueObject不变量检查。
FLOW -> migrate materially changed dto record to mandatory class Lombok set; preserve Spec §9 JSON field names/null/default, @Valid groups, @JsonIgnore sensitive。
OUTPUT -> UpsertClientResourceGrantDTO 的Java方法、DTO字段或配置可被本Step既有调用者编译使用；不新增业务API。
FAIL -> 空值/跨tenant/未知schema/重复版本/外部依赖错误按本Step已列错误模型fail-closed，不能输出原secret/token。
```

- Verification contribution: Step 9 的focus测试对 `UpsertClientResourceGrantDTO` 的字段、装配、负例及不泄密断言。
- After this file: `UpsertClientResourceGrantDTO` 成为已声明的Step依赖，下一文件可按原合同引用；未完成整条行为前不宣称GREEN。

#### File 15 — `MODIFY egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/resource/domain/dto/DeleteClientResourceGrantDTO.java`

- Purpose: 关闭 `DeleteClientResourceGrantDTO` 在主Spec中的字段/消费依赖。
- Symbols: `DeleteClientResourceGrantDTO`。
- Repository evidence: 当前同名文件存在，符号/消费者由rg确认。
- Dependencies and consumers: 本Step既有File块调用该类型；数据来源/消费者为主Spec §9.2/§10.2，不产生新HTTP路由。
- Why now: 该类型按本Step的测试/编译依赖顺序落位，以免由实现者临时发明。
- Contract/signature changes: migrate materially changed dto record to mandatory class Lombok set; preserve Spec §9 JSON field names/null/default, @Valid groups, @JsonIgnore sensitive。
- Input/output and state mapping: 精确字段由主Spec §9.2/§10.2 指定，realm从签名/注册得出，Instant为UTC，版本单调，秘密字段仅服务器内部。
- Error and edge behavior: null/未知字段/跨tenant/重复版本拒绝；不转成宽松缺省或吞异常。
- Standards impact: MC-ARCH-001, MC-NAME-001, MC-VALID-001, MC-MODEL-001, MC-CONVERT-001, MC-BEAN-001, MC-JSON-001, MC-TIME-001, MC-SCOPE-001, MC-TEST-001；行为Bean使用具名final Qualifier、@RequiredArgsConstructor和@Slf4j，载体遵循四注解与Builder。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 6, Rule 9, Rule 10, Rule 11；class按POJO语义命名，边界注解验证，跨层转换只用既有BaseConverter，Jackson和java.time字段明确。
- Implementation pseudocode:

```java
INPUT -> 仅接受主Spec §9.2/§10.2 的可信issuer/realm/输入字段；调用前 @Valid 或 ValueObject不变量检查。
FLOW -> migrate materially changed dto record to mandatory class Lombok set; preserve Spec §9 JSON field names/null/default, @Valid groups, @JsonIgnore sensitive。
OUTPUT -> DeleteClientResourceGrantDTO 的Java方法、DTO字段或配置可被本Step既有调用者编译使用；不新增业务API。
FAIL -> 空值/跨tenant/未知schema/重复版本/外部依赖错误按本Step已列错误模型fail-closed，不能输出原secret/token。
```

- Verification contribution: Step 9 的focus测试对 `DeleteClientResourceGrantDTO` 的字段、装配、负例及不泄密断言。
- After this file: `DeleteClientResourceGrantDTO` 成为已声明的Step依赖，下一文件可按原合同引用；未完成整条行为前不宣称GREEN。

#### File 16 — `MODIFY egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/resource/domain/dto/BatchClientResourceGrantDTO.java`

- Purpose: 关闭 `BatchClientResourceGrantDTO` 在主Spec中的字段/消费依赖。
- Symbols: `BatchClientResourceGrantDTO`。
- Repository evidence: 当前同名文件存在，符号/消费者由rg确认。
- Dependencies and consumers: 本Step既有File块调用该类型；数据来源/消费者为主Spec §9.2/§10.2，不产生新HTTP路由。
- Why now: 该类型按本Step的测试/编译依赖顺序落位，以免由实现者临时发明。
- Contract/signature changes: migrate materially changed dto record to mandatory class Lombok set; preserve Spec §9 JSON field names/null/default, @Valid groups, @JsonIgnore sensitive。
- Input/output and state mapping: 精确字段由主Spec §9.2/§10.2 指定，realm从签名/注册得出，Instant为UTC，版本单调，秘密字段仅服务器内部。
- Error and edge behavior: null/未知字段/跨tenant/重复版本拒绝；不转成宽松缺省或吞异常。
- Standards impact: MC-ARCH-001, MC-NAME-001, MC-VALID-001, MC-MODEL-001, MC-CONVERT-001, MC-BEAN-001, MC-JSON-001, MC-TIME-001, MC-SCOPE-001, MC-TEST-001；行为Bean使用具名final Qualifier、@RequiredArgsConstructor和@Slf4j，载体遵循四注解与Builder。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 6, Rule 9, Rule 10, Rule 11；class按POJO语义命名，边界注解验证，跨层转换只用既有BaseConverter，Jackson和java.time字段明确。
- Implementation pseudocode:

```java
INPUT -> 仅接受主Spec §9.2/§10.2 的可信issuer/realm/输入字段；调用前 @Valid 或 ValueObject不变量检查。
FLOW -> migrate materially changed dto record to mandatory class Lombok set; preserve Spec §9 JSON field names/null/default, @Valid groups, @JsonIgnore sensitive。
OUTPUT -> BatchClientResourceGrantDTO 的Java方法、DTO字段或配置可被本Step既有调用者编译使用；不新增业务API。
FAIL -> 空值/跨tenant/未知schema/重复版本/外部依赖错误按本Step已列错误模型fail-closed，不能输出原secret/token。
```

- Verification contribution: Step 9 的focus测试对 `BatchClientResourceGrantDTO` 的字段、装配、负例及不泄密断言。
- After this file: `BatchClientResourceGrantDTO` 成为已声明的Step依赖，下一文件可按原合同引用；未完成整条行为前不宣称GREEN。

#### File 17 — `MODIFY egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/domain/dto/PublishSigningKeyDTO.java`

- Purpose: 关闭 `PublishSigningKeyDTO` 在主Spec中的字段/消费依赖。
- Symbols: `PublishSigningKeyDTO`。
- Repository evidence: 当前同名文件存在，符号/消费者由rg确认。
- Dependencies and consumers: 本Step既有File块调用该类型；数据来源/消费者为主Spec §9.2/§10.2，不产生新HTTP路由。
- Why now: 该类型按本Step的测试/编译依赖顺序落位，以免由实现者临时发明。
- Contract/signature changes: migrate materially changed dto record to mandatory class Lombok set; preserve Spec §9 JSON field names/null/default, @Valid groups, @JsonIgnore sensitive。
- Input/output and state mapping: 精确字段由主Spec §9.2/§10.2 指定，realm从签名/注册得出，Instant为UTC，版本单调，秘密字段仅服务器内部。
- Error and edge behavior: null/未知字段/跨tenant/重复版本拒绝；不转成宽松缺省或吞异常。
- Standards impact: MC-ARCH-001, MC-NAME-001, MC-VALID-001, MC-MODEL-001, MC-CONVERT-001, MC-BEAN-001, MC-JSON-001, MC-TIME-001, MC-SCOPE-001, MC-TEST-001；行为Bean使用具名final Qualifier、@RequiredArgsConstructor和@Slf4j，载体遵循四注解与Builder。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 6, Rule 9, Rule 10, Rule 11；class按POJO语义命名，边界注解验证，跨层转换只用既有BaseConverter，Jackson和java.time字段明确。
- Implementation pseudocode:

```java
INPUT -> 仅接受主Spec §9.2/§10.2 的可信issuer/realm/输入字段；调用前 @Valid 或 ValueObject不变量检查。
FLOW -> migrate materially changed dto record to mandatory class Lombok set; preserve Spec §9 JSON field names/null/default, @Valid groups, @JsonIgnore sensitive。
OUTPUT -> PublishSigningKeyDTO 的Java方法、DTO字段或配置可被本Step既有调用者编译使用；不新增业务API。
FAIL -> 空值/跨tenant/未知schema/重复版本/外部依赖错误按本Step已列错误模型fail-closed，不能输出原secret/token。
```

- Verification contribution: Step 9 的focus测试对 `PublishSigningKeyDTO` 的字段、装配、负例及不泄密断言。
- After this file: `PublishSigningKeyDTO` 成为已声明的Step依赖，下一文件可按原合同引用；未完成整条行为前不宣称GREEN。

#### File 18 — `MODIFY egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/domain/dto/CreateOAuthClientDTO.java`

- Purpose: 管理输入按原wire扩展。
- Symbols: `purpose/allowedScopes groups`。
- Repository evidence: 当前文件存在：egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/domain/dto/CreateOAuthClientDTO.java；目标源于主Spec §7–§16。
- Dependencies and consumers: 输入依赖 Step 8 与本Step此前文件；输出供 现有管理URL按realm管理Client/Grant/Key；secret仅bcrypt一次性显示及后一Step使用；Java Bean/Mapper按模块现有配置发现，前端按现有auth client消费。
- Why now: 前一文件建立本文件需要的字段或调用契约； 管理输入按原wire扩展在此位置后才可接消费者。
- Contract/signature changes: purpose/allowedScopes groups；管理输入按原wire扩展。公开行为只取主Spec §9对应API或§10/11内部合同。
- Input/output and state mapping: legacy clientId/appId names retained; unknown JSON400。
- Error and edge behavior: PUBLIC secret null; confidential appId required。
- Standards impact: MC-ARCH-001, MC-SCOPE-001, MC-TEST-001, MC-VALID-001；按已选传统分层，输入校验与权限先于状态读取，PO不出边界，相关日志脱敏。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 6, Rule 11；此文件直接按所列原始规则落实命名、验证、类型/注入或配置，其他规则由同Step关联文件负责。
- Implementation pseudocode:

```java
INPUT -> legacy clientId/appId names retained; unknown JSON400。
FLOW -> class @Data @NoArgsConstructor @AllArgsConstructor @Accessors(chain=true) @Builder; Create group, USER needs PKCE/S256 redirect; SERVICE allowed empty bootstrap but cannot issue until scope/grant
ON INVALID/CONCURRENT/DEPENDENCY FAILURE -> PUBLIC secret null; confidential appId required。
RESULT -> only the named symbol/contract in this File block becomes available; preserve tenant, expectedVersion, scope, UTC time and secret masking stated above.
```

- Verification contribution: 本Step `mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthClientControllerTest,SigningKeyServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test`；此文件负责 purpose/allowedScopes groups 的正例与 PUBLIC secret null; confidential appId required 负例。
- After this file: purpose/allowedScopes groups 的字段/入口或注册已落盘；与下一文件组合后本Step才能GREEN，不能把中间状态当完成。

#### File 19 — `MODIFY egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/domain/dto/UpdateOAuthClientDTO.java`

- Purpose: 版本写与scope收缩。
- Symbols: `allowedScopes optional`。
- Repository evidence: 当前文件存在：egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/domain/dto/UpdateOAuthClientDTO.java；目标源于主Spec §7–§16。
- Dependencies and consumers: 输入依赖 Step 8 与本Step此前文件；输出供 现有管理URL按realm管理Client/Grant/Key；secret仅bcrypt一次性显示及后一Step使用；Java Bean/Mapper按模块现有配置发现，前端按现有auth client消费。
- Why now: 前一文件建立本文件需要的字段或调用契约； 版本写与scope收缩在此位置后才可接消费者。
- Contract/signature changes: allowedScopes optional；版本写与scope收缩。公开行为只取主Spec §9对应API或§10/11内部合同。
- Input/output and state mapping: no purpose/issuer mutable update。
- Error and edge behavior: scope enlarge no grant/consent auto issue。
- Standards impact: MC-ARCH-001, MC-SCOPE-001, MC-TEST-001, MC-VALID-001；按已选传统分层，输入校验与权限先于状态读取，PO不出边界，相关日志脱敏。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 6, Rule 11；此文件直接按所列原始规则落实命名、验证、类型/注入或配置，其他规则由同Step关联文件负责。
- Implementation pseudocode:

```java
INPUT -> no purpose/issuer mutable update。
FLOW -> class Lombok set; @Validated Update; absent scope retains, empty scope revokes; expectedVersion nonnegative
ON INVALID/CONCURRENT/DEPENDENCY FAILURE -> scope enlarge no grant/consent auto issue。
RESULT -> only the named symbol/contract in this File block becomes available; preserve tenant, expectedVersion, scope, UTC time and secret masking stated above.
```

- Verification contribution: 本Step `mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthClientControllerTest,SigningKeyServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test`；此文件负责 allowedScopes optional 的正例与 scope enlarge no grant/consent auto issue 负例。
- After this file: allowedScopes optional 的字段/入口或注册已落盘；与下一文件组合后本Step才能GREEN，不能把中间状态当完成。

#### File 20 — `MODIFY egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/service/impl/OAuthClientServiceImpl.java`

- Purpose: MP ClientRepository + SecretEncoder。
- Symbols: `create/rotate/update/redirects`。
- Repository evidence: 当前文件存在：egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/service/impl/OAuthClientServiceImpl.java；目标源于主Spec §7–§16。
- Dependencies and consumers: 输入依赖 Step 8 与本Step此前文件；输出供 现有管理URL按realm管理Client/Grant/Key；secret仅bcrypt一次性显示及后一Step使用；Java Bean/Mapper按模块现有配置发现，前端按现有auth client消费。
- Why now: 前一文件建立本文件需要的字段或调用契约； MP ClientRepository + SecretEncoder在此位置后才可接消费者。
- Contract/signature changes: create/rotate/update/redirects；MP ClientRepository + SecretEncoder。公开行为只取主Spec §9对应API或§10/11内部合同。
- Input/output and state mapping: one-time Result only; old Argon2 remains legacy instance。
- Error and edge behavior: lost rotate response cannot replay/restore raw secret。
- Standards impact: MC-ARCH-001, MC-SCOPE-001, MC-TEST-001, MC-VALID-001；按已选传统分层，输入校验与权限先于状态读取，PO不出边界，相关日志脱敏。
- Literal rule enforcement: Rule 2, Rule 3, Rule 4, Rule 6, Rule 9, Rule 11；此文件直接按所列原始规则落实命名、验证、类型/注入或配置，其他规则由同Step关联文件负责。
- Implementation pseudocode:

```java
INPUT -> one-time Result only; old Argon2 remains legacy instance。
FLOW -> @Service("oauthClientServiceImpl") @Slf4j @RequiredArgsConstructor final @Qualifier; SecureRandom 32 bytes base64url; BCryptPasswordEncoder cost12; store {bcrypt} hash; version CAS; honor realm guard
ON INVALID/CONCURRENT/DEPENDENCY FAILURE -> lost rotate response cannot replay/restore raw secret。
RESULT -> only the named symbol/contract in this File block becomes available; preserve tenant, expectedVersion, scope, UTC time and secret masking stated above.
```

- Verification contribution: 本Step `mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthClientControllerTest,SigningKeyServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test`；此文件负责 create/rotate/update/redirects 的正例与 lost rotate response cannot replay/restore raw secret 负例。
- After this file: create/rotate/update/redirects 的字段/入口或注册已落盘；与下一文件组合后本Step才能GREEN，不能把中间状态当完成。

#### File 21 — `MODIFY egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/controller/OAuthClientController.java`

- Purpose: 按已验principal传realm并保留原URL/权限。
- Symbols: `API-020..027`。
- Repository evidence: 当前文件存在：egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/controller/OAuthClientController.java；目标源于主Spec §7–§16。
- Dependencies and consumers: 输入依赖 Step 8 与本Step此前文件；输出供 现有管理URL按realm管理Client/Grant/Key；secret仅bcrypt一次性显示及后一Step使用；Java Bean/Mapper按模块现有配置发现，前端按现有auth client消费。
- Why now: 前一文件建立本文件需要的字段或调用契约； 按已验principal传realm并保留原URL/权限在此位置后才可接消费者。
- Contract/signature changes: API-020..027；按已验principal传realm并保留原URL/权限。公开行为只取主Spec §9对应API或§10/11内部合同。
- Input/output and state mapping: bare VO shape; issuer added output; no PO exposure。
- Error and edge behavior: cross-realm403, stale409, 201/200 exact。
- Standards impact: MC-ARCH-001, MC-SCOPE-001, MC-TEST-001, MC-VALID-001；按已选传统分层，输入校验与权限先于状态读取，PO不出边界，相关日志脱敏。
- Literal rule enforcement: Rule 2, Rule 4, Rule 6, Rule 11；此文件直接按所列原始规则落实命名、验证、类型/注入或配置，其他规则由同Step关联文件负责。
- Implementation pseudocode:

```java
INPUT -> bare VO shape; issuer added output; no PO exposure。
FLOW -> @RestController("oauthClientController") @Slf4j @RequiredArgsConstructor final @Qualifier; @Valid Create/Update/Rotate groups; optional realm query defaults own tenant; platform requires admin tenant+strong auth
ON INVALID/CONCURRENT/DEPENDENCY FAILURE -> cross-realm403, stale409, 201/200 exact。
RESULT -> only the named symbol/contract in this File block becomes available; preserve tenant, expectedVersion, scope, UTC time and secret masking stated above.
```

- Verification contribution: 本Step `mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthClientControllerTest,SigningKeyServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test`；此文件负责 API-020..027 的正例与 cross-realm403, stale409, 201/200 exact 负例。
- After this file: API-020..027 的字段/入口或注册已落盘；与下一文件组合后本Step才能GREEN，不能把中间状态当完成。

#### File 22 — `MODIFY egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/resource/service/impl/ResourceServerServiceImpl.java`

- Purpose: MP OAuthResource/Grant Repository。
- Symbols: `resource create/status/grants`。
- Repository evidence: 当前文件存在：egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/resource/service/impl/ResourceServerServiceImpl.java；目标源于主Spec §7–§16。
- Dependencies and consumers: 输入依赖 Step 8 与本Step此前文件；输出供 现有管理URL按realm管理Client/Grant/Key；secret仅bcrypt一次性显示及后一Step使用；Java Bean/Mapper按模块现有配置发现，前端按现有auth client消费。
- Why now: 前一文件建立本文件需要的字段或调用契约； MP OAuthResource/Grant Repository在此位置后才可接消费者。
- Contract/signature changes: resource create/status/grants；MP OAuthResource/Grant Repository。公开行为只取主Spec §9对应API或§10/11内部合同。
- Input/output and state mapping: preserve external resourceServerId/URI and status VO。
- Error and edge behavior: no implicit grant, wrong realm403, atomic batch。
- Standards impact: MC-ARCH-001, MC-SCOPE-001, MC-TEST-001, MC-VALID-001；按已选传统分层，输入校验与权限先于状态读取，PO不出边界，相关日志脱敏。
- Literal rule enforcement: Rule 2, Rule 4, Rule 9, Rule 11；此文件直接按所列原始规则落实命名、验证、类型/注入或配置，其他规则由同Step关联文件负责。
- Implementation pseudocode:

```java
INPUT -> preserve external resourceServerId/URI and status VO。
FLOW -> @Service("resourceServerServiceImpl") @Slf4j @RequiredArgsConstructor; management client must be platform SERVICE registration; source app/biz/env from resource; grant exact tenant/resource/version; existing DDC outbox unchanged
ON INVALID/CONCURRENT/DEPENDENCY FAILURE -> no implicit grant, wrong realm403, atomic batch。
RESULT -> only the named symbol/contract in this File block becomes available; preserve tenant, expectedVersion, scope, UTC time and secret masking stated above.
```

- Verification contribution: 本Step `mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthClientControllerTest,SigningKeyServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test`；此文件负责 resource create/status/grants 的正例与 no implicit grant, wrong realm403, atomic batch 负例。
- After this file: resource create/status/grants 的字段/入口或注册已落盘；与下一文件组合后本Step才能GREEN，不能把中间状态当完成。

#### File 23 — `CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/resource/repository/MpResourceServerStore.java`

- Purpose: 替代JpaResourceServerStore访问OAuthResource。
- Symbols: `ResourceServerStore adapter`。
- Repository evidence: 当前不存在；Spec §8/§11 明确目标，邻近包可验证；目标源于主Spec §7–§16。
- Dependencies and consumers: 输入依赖 Step 8 与本Step此前文件；输出供 现有管理URL按realm管理Client/Grant/Key；secret仅bcrypt一次性显示及后一Step使用；Java Bean/Mapper按模块现有配置发现，前端按现有auth client消费。
- Why now: 前一文件建立本文件需要的字段或调用契约； 替代JpaResourceServerStore访问OAuthResource在此位置后才可接消费者。
- Contract/signature changes: ResourceServerStore adapter；替代JpaResourceServerStore访问OAuthResource。公开行为只取主Spec §9对应API或§10/11内部合同。
- Input/output and state mapping: no old FK dependency, no source claim from request。
- Error and edge behavior: missing app/resource invalid_target; tenant context never leaked。
- Standards impact: MC-ARCH-001, MC-SCOPE-001, MC-TEST-001, MC-VALID-001；按已选传统分层，输入校验与权限先于状态读取，PO不出边界，相关日志脱敏。
- Literal rule enforcement: Rule 2, Rule 4, Rule 9, Rule 11；此文件直接按所列原始规则落实命名、验证、类型/注入或配置，其他规则由同Step关联文件负责。
- Implementation pseudocode:

```java
INPUT -> no old FK dependency, no source claim from request。
FLOW -> @Repository("mpResourceServerStore") @Slf4j @RequiredArgsConstructor; lookup resource URI/id in platform metadata storage and restore original tenant context finally; convert to existing core ResourceServer
ON INVALID/CONCURRENT/DEPENDENCY FAILURE -> missing app/resource invalid_target; tenant context never leaked。
RESULT -> only the named symbol/contract in this File block becomes available; preserve tenant, expectedVersion, scope, UTC time and secret masking stated above.
```

- Verification contribution: 本Step `mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthClientControllerTest,SigningKeyServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test`；此文件负责 ResourceServerStore adapter 的正例与 missing app/resource invalid_target; tenant context never leaked 负例。
- After this file: ResourceServerStore adapter 的字段/入口或注册已落盘；与下一文件组合后本Step才能GREEN，不能把中间状态当完成。

#### File 24 — `MODIFY egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/service/impl/SigningKeyServiceImpl.java`

- Purpose: perissuer keyring与版本守卫。
- Symbols: `list/publish/activate/retire`。
- Repository evidence: 当前文件存在：egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/service/impl/SigningKeyServiceImpl.java；目标源于主Spec §7–§16。
- Dependencies and consumers: 输入依赖 Step 8 与本Step此前文件；输出供 现有管理URL按realm管理Client/Grant/Key；secret仅bcrypt一次性显示及后一Step使用；Java Bean/Mapper按模块现有配置发现，前端按现有auth client消费。
- Why now: 前一文件建立本文件需要的字段或调用契约； perissuer keyring与版本守卫在此位置后才可接消费者。
- Contract/signature changes: list/publish/activate/retire；perissuer keyring与版本守卫。公开行为只取主Spec §9对应API或§10/11内部合同。
- Input/output and state mapping: public JWKS multiple keys, private never emitted。
- Error and edge behavior: wrong version409; late node not ready503。
- Standards impact: MC-ARCH-001, MC-SCOPE-001, MC-TEST-001, MC-VALID-001；按已选传统分层，输入校验与权限先于状态读取，PO不出边界，相关日志脱敏。
- Literal rule enforcement: Rule 2, Rule 4, Rule 9, Rule 10, Rule 11；此文件直接按所列原始规则落实命名、验证、类型/注入或配置，其他规则由同Step关联文件负责。
- Implementation pseudocode:

```java
INPUT -> public JWKS multiple keys, private never emitted。
FLOW -> @Service("signingKeyServiceImpl") @Slf4j @RequiredArgsConstructor; update status PUBLISHED→ACTIVE→VERIFY_ONLY→RETIRED, select kid by exact realm; AES-GCM private only; ack instances before activate
ON INVALID/CONCURRENT/DEPENDENCY FAILURE -> wrong version409; late node not ready503。
RESULT -> only the named symbol/contract in this File block becomes available; preserve tenant, expectedVersion, scope, UTC time and secret masking stated above.
```

- Verification contribution: 本Step `mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthClientControllerTest,SigningKeyServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test`；此文件负责 list/publish/activate/retire 的正例与 wrong version409; late node not ready503 负例。
- After this file: list/publish/activate/retire 的字段/入口或注册已落盘；与下一文件组合后本Step才能GREEN，不能把中间状态当完成。

#### File 25 — `MODIFY egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/controller/SigningKeyController.java`

- Purpose: 原URL按realm作用域增校验/文档。
- Symbols: `API-031..034`。
- Repository evidence: 当前文件存在：egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/controller/SigningKeyController.java；目标源于主Spec §7–§16。
- Dependencies and consumers: 输入依赖 Step 8 与本Step此前文件；输出供 现有管理URL按realm管理Client/Grant/Key；secret仅bcrypt一次性显示及后一Step使用；Java Bean/Mapper按模块现有配置发现，前端按现有auth client消费。
- Why now: 前一文件建立本文件需要的字段或调用契约； 原URL按realm作用域增校验/文档在此位置后才可接消费者。
- Contract/signature changes: API-031..034；原URL按realm作用域增校验/文档。公开行为只取主Spec §9对应API或§10/11内部合同。
- Input/output and state mapping: SigningKeyVO adds issuer, secret excluded。
- Error and edge behavior: wrong realm/ACTIVE retire denied。
- Standards impact: MC-ARCH-001, MC-SCOPE-001, MC-TEST-001, MC-VALID-001；按已选传统分层，输入校验与权限先于状态读取，PO不出边界，相关日志脱敏。
- Literal rule enforcement: Rule 2, Rule 4, Rule 6, Rule 11；此文件直接按所列原始规则落实命名、验证、类型/注入或配置，其他规则由同Step关联文件负责。
- Implementation pseudocode:

```java
INPUT -> SigningKeyVO adds issuer, secret excluded。
FLOW -> @RestController("signingKeyController") @Slf4j @RequiredArgsConstructor; @Valid Publish groups, @PositiveOrZero expectedVersion; realm and permissions; @Operation statuses/OAS
ON INVALID/CONCURRENT/DEPENDENCY FAILURE -> wrong realm/ACTIVE retire denied。
RESULT -> only the named symbol/contract in this File block becomes available; preserve tenant, expectedVersion, scope, UTC time and secret masking stated above.
```

- Verification contribution: 本Step `mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthClientControllerTest,SigningKeyServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test`；此文件负责 API-031..034 的正例与 wrong realm/ACTIVE retire denied 负例。
- After this file: API-031..034 的字段/入口或注册已落盘；与下一文件组合后本Step才能GREEN，不能把中间状态当完成。

- Validation working directory: `/Users/mario/SelfProject/Egon-COLA`。
- Verification command: `mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthClientControllerTest,SigningKeyServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test`。
- Expected result: 先确认上述RED由缺失目标行为引起，GREEN时命名用例成功、模块编译成功、`git diff --check`零错误；不以不存在依赖/数据库/fixture而失败作为RED。
- Failure returns to: Step 8 的编译/Schema前置；若需改变外部语义、架构或DDL版本则回主Spec。
- Completion criteria: 现有管理URL按realm管理Client/Grant/Key；secret仅bcrypt一次性显示；本Step的所有Manual Checks在实际执行时给出证据，未通过不能提交。
- Rollback: 提交前仅恢复本Step的精确路径；DDL已对真实数据运行后只forward-fix和新版本，不能编辑既有SQL/历史或恢复旧安全世代。
- Commit paths: `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/domain/dto/PublishSigningKeyDTO.java`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/resource/domain/dto/BatchClientResourceGrantDTO.java`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/resource/domain/dto/DeleteClientResourceGrantDTO.java`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/resource/domain/dto/UpsertClientResourceGrantDTO.java`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/domain/dto/OAuthValueDTO.java`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/domain/dto/RotateClientSecretDTO.java`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/domain/vo/SigningKeyVO.java`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/resource/domain/vo/ResourceServerVO.java`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/resource/domain/vo/ClientResourceGrantVO.java`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/domain/vo/RotatedClientSecretVO.java`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/domain/vo/CreatedOAuthClientVO.java`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/domain/vo/OAuthClientVO.java`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/resource/domain/converter/ResourceServerViewConverter.java`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/domain/converter/SigningKeyViewConverter.java`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/resource/domain/converter/ClientResourceGrantViewConverter.java`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/domain/converter/OAuthClientViewConverter.java`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/controller/OAuthClientControllerTest.java`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/domain/dto/CreateOAuthClientDTO.java`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/domain/dto/UpdateOAuthClientDTO.java`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/service/impl/OAuthClientServiceImpl.java`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/controller/OAuthClientController.java`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/resource/service/impl/ResourceServerServiceImpl.java`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/resource/repository/MpResourceServerStore.java`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/service/impl/SigningKeyServiceImpl.java`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/controller/SigningKeyController.java`。
- Commit: `feat(shoubing): scope oauth administration and rotate secrets`。

### Step 10 — 闭合撤销、审计、密钥轮换

- Requirements: REQ-012, REQ-013, REQ-014, REQ-015, REQ-016
- Dependencies: Step 9
- Baseline state: 当前HEAD的对应源码/测试如每File的Repository evidence，先前Step已完成并有其focused GREEN；本Step尚不存在revokeAckAndFenceRecovery所列新行为。
- Observable outcome: 成功撤销后新鉴权拒绝；Redis故障503；审计持久可恢复。
- End state: 成功撤销后新鉴权拒绝；Redis故障503；审计持久可恢复；后续Step的Consumer尚未切流，不声明完整生产验收。
- Test-first gate: Required — 初始无共享状态/恢复handler，RED由新用例而非容器启动失败；单元先GREEN，真实Redis/PG在用户运行门禁。
- Manual Checks: MC-ARCH-001, MC-REUSE-001, MC-SCOPE-001, MC-TEST-001, MC-VALID-001, MC-NAME-001, MC-MODEL-001, MC-CONVERT-001, MC-LOG-001, MC-BEAN-001, MC-JSON-001, MC-TIME-001
- Literal Rules: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11
- Ordered files:

#### File 1 — `CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/tianquan/shoubing/admin/token/TokenRevocationServiceTest.java`

- Purpose: RED先固定DB/Redis顺序与failclosed。
- Symbols: `revokeAckAndFenceRecovery`。
- Repository evidence: 当前不存在；Spec §8/§11 明确目标，邻近包可验证；目标源于主Spec §7–§16。
- Dependencies and consumers: 输入依赖 Step 9 与本Step此前文件；输出供 成功撤销后新鉴权拒绝；Redis故障503；审计持久可恢复及后一Step使用；Java Bean/Mapper按模块现有配置发现，前端按现有auth client消费。
- Why now: 本Step的编译前置/RED契约先锁定； RED先固定DB/Redis顺序与failclosed在此位置后才可接消费者。
- Contract/signature changes: revokeAckAndFenceRecovery；RED先固定DB/Redis顺序与failclosed。公开行为只取主Spec §9对应API或§10/11内部合同。
- Input/output and state mapping: Mockito/受控Fake先RED；真实PostgreSQL+Redis在§8独立门禁。
- Error and edge behavior: in-memory blacklist cannot satisfy test。
- Standards impact: MC-ARCH-001, MC-SCOPE-001, MC-TEST-001, MC-VALID-001；按已选传统分层，输入校验与权限先于状态读取，PO不出边界，相关日志脱敏。
- Literal rule enforcement: Rule 2, Rule 11；此文件直接按所列原始规则落实命名、验证、类型/注入或配置，其他规则由同Step关联文件负责。
- Implementation pseudocode:

```java
INPUT -> Mockito/受控Fake先RED；真实PostgreSQL+Redis在§8独立门禁。
FLOW -> mock同一授权的两个AT；配置Fake Repository事务结果与Fake Redis确认/超时，断言没有Redis ACK时不200、旧epoch读取503、重放后401；真实跨进程与DB锁由§8运行门禁验证
ON INVALID/CONCURRENT/DEPENDENCY FAILURE -> in-memory blacklist cannot satisfy test。
RESULT -> only the named symbol/contract in this File block becomes available; preserve tenant, expectedVersion, scope, UTC time and secret masking stated above.
```

- Verification contribution: 本Step `mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=TokenRevocationServiceTest -Dsurefire.failIfNoSpecifiedTests=false test`；此文件负责 revokeAckAndFenceRecovery 的正例与 in-memory blacklist cannot satisfy test 负例。
- After this file: revokeAckAndFenceRecovery 的字段/入口或注册已落盘；与下一文件组合后本Step才能GREEN，不能把中间状态当完成。

#### File 2 — `CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-core/src/main/java/top/egon/cola/platform/tianquan/shoubing/core/audit/OAuthSecurityChangedEvent.java`

- Purpose: 关闭 `OAuthSecurityChangedEvent` 在主Spec中的字段/消费依赖。
- Symbols: `OAuthSecurityChangedEvent`。
- Repository evidence: 当前不存在；主Spec §9.2 EVENT/JOB/§15.3 命名此对象/字段；现有相邻业务包提供放置证据。
- Dependencies and consumers: 本Step既有File块调用该类型；数据来源/消费者为主Spec §9.2 EVENT/JOB/§15.3，不产生新HTTP路由。
- Why now: 该类型按本Step的测试/编译依赖顺序落位，以免由实现者临时发明。
- Contract/signature changes: schemaVersion=1 eventId/issuer/realmKey/revision/target/generation/expiry/reason/occurredAt/trace; actual Outbox EVENT-001。
- Input/output and state mapping: 精确字段由主Spec §9.2 EVENT/JOB/§15.3 指定，realm从签名/注册得出，Instant为UTC，版本单调，秘密字段仅服务器内部。
- Error and edge behavior: null/未知字段/跨tenant/重复版本拒绝；不转成宽松缺省或吞异常。
- Standards impact: MC-ARCH-001, MC-NAME-001, MC-VALID-001, MC-MODEL-001, MC-CONVERT-001, MC-BEAN-001, MC-JSON-001, MC-TIME-001, MC-SCOPE-001, MC-TEST-001；行为Bean使用具名final Qualifier、@RequiredArgsConstructor和@Slf4j，载体遵循四注解与Builder。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 6, Rule 9, Rule 10, Rule 11；class按POJO语义命名，边界注解验证，跨层转换只用既有BaseConverter，Jackson和java.time字段明确。
- Implementation pseudocode:

```java
INPUT -> 仅接受主Spec §9.2 EVENT/JOB/§15.3 的可信issuer/realm/输入字段；调用前 @Valid 或 ValueObject不变量检查。
FLOW -> schemaVersion=1 eventId/issuer/realmKey/revision/target/generation/expiry/reason/occurredAt/trace; actual Outbox EVENT-001。
OUTPUT -> OAuthSecurityChangedEvent 的Java方法、DTO字段或配置可被本Step既有调用者编译使用；不新增业务API。
FAIL -> 空值/跨tenant/未知schema/重复版本/外部依赖错误按本Step已列错误模型fail-closed，不能输出原secret/token。
```

- Verification contribution: Step 10 的focus测试对 `OAuthSecurityChangedEvent` 的字段、装配、负例及不泄密断言。
- After this file: `OAuthSecurityChangedEvent` 成为已声明的Step依赖，下一文件可按原合同引用；未完成整条行为前不宣称GREEN。

#### File 3 — `CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-core/src/main/java/top/egon/cola/platform/tianquan/shoubing/core/audit/TokenUsageAuditEvent.java`

- Purpose: 关闭 `TokenUsageAuditEvent` 在主Spec中的字段/消费依赖。
- Symbols: `TokenUsageAuditEvent`。
- Repository evidence: 当前不存在；主Spec §9.2 EVENT/JOB/§15.3 命名此对象/字段；现有相邻业务包提供放置证据。
- Dependencies and consumers: 本Step既有File块调用该类型；数据来源/消费者为主Spec §9.2 EVENT/JOB/§15.3，不产生新HTTP路由。
- Why now: 该类型按本Step的测试/编译依赖顺序落位，以免由实现者临时发明。
- Contract/signature changes: schemaVersion=1 eventId/issuer?/resource/subjectRef/clientRef/tokenRef/result/reason/occurredAt/trace; actual Redis Streams EVENT-002。
- Input/output and state mapping: 精确字段由主Spec §9.2 EVENT/JOB/§15.3 指定，realm从签名/注册得出，Instant为UTC，版本单调，秘密字段仅服务器内部。
- Error and edge behavior: null/未知字段/跨tenant/重复版本拒绝；不转成宽松缺省或吞异常。
- Standards impact: MC-ARCH-001, MC-NAME-001, MC-VALID-001, MC-MODEL-001, MC-CONVERT-001, MC-BEAN-001, MC-JSON-001, MC-TIME-001, MC-SCOPE-001, MC-TEST-001；行为Bean使用具名final Qualifier、@RequiredArgsConstructor和@Slf4j，载体遵循四注解与Builder。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 6, Rule 9, Rule 10, Rule 11；class按POJO语义命名，边界注解验证，跨层转换只用既有BaseConverter，Jackson和java.time字段明确。
- Implementation pseudocode:

```java
INPUT -> 仅接受主Spec §9.2 EVENT/JOB/§15.3 的可信issuer/realm/输入字段；调用前 @Valid 或 ValueObject不变量检查。
FLOW -> schemaVersion=1 eventId/issuer?/resource/subjectRef/clientRef/tokenRef/result/reason/occurredAt/trace; actual Redis Streams EVENT-002。
OUTPUT -> TokenUsageAuditEvent 的Java方法、DTO字段或配置可被本Step既有调用者编译使用；不新增业务API。
FAIL -> 空值/跨tenant/未知schema/重复版本/外部依赖错误按本Step已列错误模型fail-closed，不能输出原secret/token。
```

- Verification contribution: Step 10 的focus测试对 `TokenUsageAuditEvent` 的字段、装配、负例及不泄密断言。
- After this file: `TokenUsageAuditEvent` 成为已声明的Step依赖，下一文件可按原合同引用；未完成整条行为前不宣称GREEN。

#### File 4 — `CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/domain/command/RevokeOAuthTokenCommand.java`

- Purpose: 关闭 `RevokeOAuthTokenCommand` 在主Spec中的字段/消费依赖。
- Symbols: `RevokeOAuthTokenCommand`。
- Repository evidence: 当前不存在；主Spec §9.2 EVENT/JOB/§15.3 命名此对象/字段；现有相邻业务包提供放置证据。
- Dependencies and consumers: 本Step既有File块调用该类型；数据来源/消费者为主Spec §9.2 EVENT/JOB/§15.3，不产生新HTTP路由。
- Why now: 该类型按本Step的测试/编译依赖顺序落位，以免由实现者临时发明。
- Contract/signature changes: realmKey/tokenDigest/clientPk/reasonCode; only owner client may revoke。
- Input/output and state mapping: 精确字段由主Spec §9.2 EVENT/JOB/§15.3 指定，realm从签名/注册得出，Instant为UTC，版本单调，秘密字段仅服务器内部。
- Error and edge behavior: null/未知字段/跨tenant/重复版本拒绝；不转成宽松缺省或吞异常。
- Standards impact: MC-ARCH-001, MC-NAME-001, MC-VALID-001, MC-MODEL-001, MC-CONVERT-001, MC-BEAN-001, MC-JSON-001, MC-TIME-001, MC-SCOPE-001, MC-TEST-001；行为Bean使用具名final Qualifier、@RequiredArgsConstructor和@Slf4j，载体遵循四注解与Builder。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 6, Rule 9, Rule 10, Rule 11；class按POJO语义命名，边界注解验证，跨层转换只用既有BaseConverter，Jackson和java.time字段明确。
- Implementation pseudocode:

```java
INPUT -> 仅接受主Spec §9.2 EVENT/JOB/§15.3 的可信issuer/realm/输入字段；调用前 @Valid 或 ValueObject不变量检查。
FLOW -> realmKey/tokenDigest/clientPk/reasonCode; only owner client may revoke。
OUTPUT -> RevokeOAuthTokenCommand 的Java方法、DTO字段或配置可被本Step既有调用者编译使用；不新增业务API。
FAIL -> 空值/跨tenant/未知schema/重复版本/外部依赖错误按本Step已列错误模型fail-closed，不能输出原secret/token。
```

- Verification contribution: Step 10 的focus测试对 `RevokeOAuthTokenCommand` 的字段、装配、负例及不泄密断言。
- After this file: `RevokeOAuthTokenCommand` 成为已声明的Step依赖，下一文件可按原合同引用；未完成整条行为前不宣称GREEN。

#### File 5 — `CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/domain/command/OAuthSubjectRevokeCommand.java`

- Purpose: 关闭 `OAuthSubjectRevokeCommand` 在主Spec中的字段/消费依赖。
- Symbols: `OAuthSubjectRevokeCommand`。
- Repository evidence: 当前不存在；主Spec §9.2 EVENT/JOB/§15.3 命名此对象/字段；现有相邻业务包提供放置证据。
- Dependencies and consumers: 本Step既有File块调用该类型；数据来源/消费者为主Spec §9.2 EVENT/JOB/§15.3，不产生新HTTP路由。
- Why now: 该类型按本Step的测试/编译依赖顺序落位，以免由实现者临时发明。
- Contract/signature changes: subject/reasonCode; global subject security event spans active realms。
- Input/output and state mapping: 精确字段由主Spec §9.2 EVENT/JOB/§15.3 指定，realm从签名/注册得出，Instant为UTC，版本单调，秘密字段仅服务器内部。
- Error and edge behavior: null/未知字段/跨tenant/重复版本拒绝；不转成宽松缺省或吞异常。
- Standards impact: MC-ARCH-001, MC-NAME-001, MC-VALID-001, MC-MODEL-001, MC-CONVERT-001, MC-BEAN-001, MC-JSON-001, MC-TIME-001, MC-SCOPE-001, MC-TEST-001；行为Bean使用具名final Qualifier、@RequiredArgsConstructor和@Slf4j，载体遵循四注解与Builder。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 6, Rule 9, Rule 10, Rule 11；class按POJO语义命名，边界注解验证，跨层转换只用既有BaseConverter，Jackson和java.time字段明确。
- Implementation pseudocode:

```java
INPUT -> 仅接受主Spec §9.2 EVENT/JOB/§15.3 的可信issuer/realm/输入字段；调用前 @Valid 或 ValueObject不变量检查。
FLOW -> subject/reasonCode; global subject security event spans active realms。
OUTPUT -> OAuthSubjectRevokeCommand 的Java方法、DTO字段或配置可被本Step既有调用者编译使用；不新增业务API。
FAIL -> 空值/跨tenant/未知schema/重复版本/外部依赖错误按本Step已列错误模型fail-closed，不能输出原secret/token。
```

- Verification contribution: Step 10 的focus测试对 `OAuthSubjectRevokeCommand` 的字段、装配、负例及不泄密断言。
- After this file: `OAuthSubjectRevokeCommand` 成为已声明的Step依赖，下一文件可按原合同引用；未完成整条行为前不宣称GREEN。

#### File 6 — `CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/tianquan/shoubing/starter/audit/OAuthAuditProperties.java`

- Purpose: 关闭 `OAuthAuditProperties` 在主Spec中的字段/消费依赖。
- Symbols: `OAuthAuditProperties`。
- Repository evidence: 当前不存在；主Spec §9.2 EVENT/JOB/§15.3 命名此对象/字段；现有相邻业务包提供放置证据。
- Dependencies and consumers: 本Step既有File块调用该类型；数据来源/消费者为主Spec §9.2 EVENT/JOB/§15.3，不产生新HTTP路由。
- Why now: 该类型按本Step的测试/编译依赖顺序落位，以免由实现者临时发明。
- Contract/signature changes: @ConfigurationProperties audit.stream/spool-directory/spool-max-bytes/hmac-key-file; validated key parity。
- Input/output and state mapping: 精确字段由主Spec §9.2 EVENT/JOB/§15.3 指定，realm从签名/注册得出，Instant为UTC，版本单调，秘密字段仅服务器内部。
- Error and edge behavior: null/未知字段/跨tenant/重复版本拒绝；不转成宽松缺省或吞异常。
- Standards impact: MC-ARCH-001, MC-NAME-001, MC-VALID-001, MC-MODEL-001, MC-CONVERT-001, MC-BEAN-001, MC-JSON-001, MC-TIME-001, MC-SCOPE-001, MC-TEST-001；行为Bean使用具名final Qualifier、@RequiredArgsConstructor和@Slf4j，载体遵循四注解与Builder。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 6, Rule 9, Rule 10, Rule 11；class按POJO语义命名，边界注解验证，跨层转换只用既有BaseConverter，Jackson和java.time字段明确。
- Implementation pseudocode:

```java
INPUT -> 仅接受主Spec §9.2 EVENT/JOB/§15.3 的可信issuer/realm/输入字段；调用前 @Valid 或 ValueObject不变量检查。
FLOW -> @ConfigurationProperties audit.stream/spool-directory/spool-max-bytes/hmac-key-file; validated key parity。
OUTPUT -> OAuthAuditProperties 的Java方法、DTO字段或配置可被本Step既有调用者编译使用；不新增业务API。
FAIL -> 空值/跨tenant/未知schema/重复版本/外部依赖错误按本Step已列错误模型fail-closed，不能输出原secret/token。
```

- Verification contribution: Step 10 的focus测试对 `OAuthAuditProperties` 的字段、装配、负例及不泄密断言。
- After this file: `OAuthAuditProperties` 成为已声明的Step依赖，下一文件可按原合同引用；未完成整条行为前不宣称GREEN。

#### File 7 — `CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/service/OAuthRetentionServiceTest.java`

- Purpose: 关闭 `OAuthRetentionServiceTest` 在主Spec中的字段/消费依赖。
- Symbols: `OAuthRetentionServiceTest`。
- Repository evidence: 当前不存在；主Spec §9.2 EVENT/JOB/§15.3 命名此对象/字段；现有相邻业务包提供放置证据。
- Dependencies and consumers: 本Step既有File块调用该类型；数据来源/消费者为主Spec §9.2 EVENT/JOB/§15.3，不产生新HTTP路由。
- Why now: 该类型按本Step的测试/编译依赖顺序落位，以免由实现者临时发明。
- Contract/signature changes: RED expired code/token and old RT retention; batch500 checkpoint, no early revocation/kid cleanup。
- Input/output and state mapping: 精确字段由主Spec §9.2 EVENT/JOB/§15.3 指定，realm从签名/注册得出，Instant为UTC，版本单调，秘密字段仅服务器内部。
- Error and edge behavior: null/未知字段/跨tenant/重复版本拒绝；不转成宽松缺省或吞异常。
- Standards impact: MC-ARCH-001, MC-NAME-001, MC-VALID-001, MC-MODEL-001, MC-CONVERT-001, MC-BEAN-001, MC-JSON-001, MC-TIME-001, MC-SCOPE-001, MC-TEST-001；行为Bean使用具名final Qualifier、@RequiredArgsConstructor和@Slf4j，载体遵循四注解与Builder。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 6, Rule 9, Rule 10, Rule 11；class按POJO语义命名，边界注解验证，跨层转换只用既有BaseConverter，Jackson和java.time字段明确。
- Implementation pseudocode:

```java
INPUT -> 仅接受主Spec §9.2 EVENT/JOB/§15.3 的可信issuer/realm/输入字段；调用前 @Valid 或 ValueObject不变量检查。
FLOW -> RED expired code/token and old RT retention; batch500 checkpoint, no early revocation/kid cleanup。
OUTPUT -> OAuthRetentionServiceTest 的Java方法、DTO字段或配置可被本Step既有调用者编译使用；不新增业务API。
FAIL -> 空值/跨tenant/未知schema/重复版本/外部依赖错误按本Step已列错误模型fail-closed，不能输出原secret/token。
```

- Verification contribution: Step 10 的focus测试对 `OAuthRetentionServiceTest` 的字段、装配、负例及不泄密断言。
- After this file: `OAuthRetentionServiceTest` 成为已声明的Step依赖，下一文件可按原合同引用；未完成整条行为前不宣称GREEN。

#### File 8 — `CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/service/OAuthRetentionService.java`

- Purpose: 关闭 `OAuthRetentionService` 在主Spec中的字段/消费依赖。
- Symbols: `OAuthRetentionService`。
- Repository evidence: 当前不存在；主Spec §9.2 EVENT/JOB/§15.3 命名此对象/字段；现有相邻业务包提供放置证据。
- Dependencies and consumers: 本Step既有File块调用该类型；数据来源/消费者为主Spec §9.2 EVENT/JOB/§15.3，不产生新HTTP路由。
- Why now: 该类型按本Step的测试/编译依赖顺序落位，以免由实现者临时发明。
- Contract/signature changes: cleanExpired(TrustedRealmBO,Clock) JOB-002, exact expiration+grace and 500 batch contract。
- Input/output and state mapping: 精确字段由主Spec §9.2 EVENT/JOB/§15.3 指定，realm从签名/注册得出，Instant为UTC，版本单调，秘密字段仅服务器内部。
- Error and edge behavior: null/未知字段/跨tenant/重复版本拒绝；不转成宽松缺省或吞异常。
- Standards impact: MC-ARCH-001, MC-NAME-001, MC-VALID-001, MC-MODEL-001, MC-CONVERT-001, MC-BEAN-001, MC-JSON-001, MC-TIME-001, MC-SCOPE-001, MC-TEST-001；行为Bean使用具名final Qualifier、@RequiredArgsConstructor和@Slf4j，载体遵循四注解与Builder。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 6, Rule 9, Rule 10, Rule 11；class按POJO语义命名，边界注解验证，跨层转换只用既有BaseConverter，Jackson和java.time字段明确。
- Implementation pseudocode:

```java
INPUT -> 仅接受主Spec §9.2 EVENT/JOB/§15.3 的可信issuer/realm/输入字段；调用前 @Valid 或 ValueObject不变量检查。
FLOW -> cleanExpired(TrustedRealmBO,Clock) JOB-002, exact expiration+grace and 500 batch contract。
OUTPUT -> OAuthRetentionService 的Java方法、DTO字段或配置可被本Step既有调用者编译使用；不新增业务API。
FAIL -> 空值/跨tenant/未知schema/重复版本/外部依赖错误按本Step已列错误模型fail-closed，不能输出原secret/token。
```

- Verification contribution: Step 10 的focus测试对 `OAuthRetentionService` 的字段、装配、负例及不泄密断言。
- After this file: `OAuthRetentionService` 成为已声明的Step依赖，下一文件可按原合同引用；未完成整条行为前不宣称GREEN。

#### File 9 — `CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/service/impl/OAuthRetentionServiceImpl.java`

- Purpose: 关闭 `OAuthRetentionServiceImpl` 在主Spec中的字段/消费依赖。
- Symbols: `OAuthRetentionServiceImpl`。
- Repository evidence: 当前不存在；主Spec §9.2 EVENT/JOB/§15.3 命名此对象/字段；现有相邻业务包提供放置证据。
- Dependencies and consumers: 本Step既有File块调用该类型；数据来源/消费者为主Spec §9.2 EVENT/JOB/§15.3，不产生新HTTP路由。
- Why now: 该类型按本Step的测试/编译依赖顺序落位，以免由实现者临时发明。
- Contract/signature changes: @Service("oauthRetentionService") scheduled hourly; batch≤500 per realm; RT until absolute family expiry, audit180d; no kid tombstone deletion。
- Input/output and state mapping: 精确字段由主Spec §9.2 EVENT/JOB/§15.3 指定，realm从签名/注册得出，Instant为UTC，版本单调，秘密字段仅服务器内部。
- Error and edge behavior: null/未知字段/跨tenant/重复版本拒绝；不转成宽松缺省或吞异常。
- Standards impact: MC-ARCH-001, MC-NAME-001, MC-VALID-001, MC-MODEL-001, MC-CONVERT-001, MC-BEAN-001, MC-JSON-001, MC-TIME-001, MC-SCOPE-001, MC-TEST-001；行为Bean使用具名final Qualifier、@RequiredArgsConstructor和@Slf4j，载体遵循四注解与Builder。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 6, Rule 9, Rule 10, Rule 11；class按POJO语义命名，边界注解验证，跨层转换只用既有BaseConverter，Jackson和java.time字段明确。
- Implementation pseudocode:

```java
INPUT -> 仅接受主Spec §9.2 EVENT/JOB/§15.3 的可信issuer/realm/输入字段；调用前 @Valid 或 ValueObject不变量检查。
FLOW -> @Service("oauthRetentionService") scheduled hourly; batch≤500 per realm; RT until absolute family expiry, audit180d; no kid tombstone deletion。
OUTPUT -> OAuthRetentionServiceImpl 的Java方法、DTO字段或配置可被本Step既有调用者编译使用；不新增业务API。
FAIL -> 空值/跨tenant/未知schema/重复版本/外部依赖错误按本Step已列错误模型fail-closed，不能输出原secret/token。
```

- Verification contribution: Step 10 的focus测试对 `OAuthRetentionServiceImpl` 的字段、装配、负例及不泄密断言。
- After this file: `OAuthRetentionServiceImpl` 成为已声明的Step依赖，下一文件可按原合同引用；未完成整条行为前不宣称GREEN。

#### File 10 — `CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/service/impl/OAuthSecurityProjectionComponent.java`

- Purpose: 单调Redis Lua状态与ready/fence。
- Symbols: `oauthSecurityProjectionComponent`。
- Repository evidence: 当前不存在；Spec §8/§11 明确目标，邻近包可验证；目标源于主Spec §7–§16。
- Dependencies and consumers: 输入依赖 Step 9 与本Step此前文件；输出供 成功撤销后新鉴权拒绝；Redis故障503；审计持久可恢复及后一Step使用；Java Bean/Mapper按模块现有配置发现，前端按现有auth client消费。
- Why now: 前一文件建立本文件需要的字段或调用契约； 单调Redis Lua状态与ready/fence在此位置后才可接消费者。
- Contract/signature changes: oauthSecurityProjectionComponent；单调Redis Lua状态与ready/fence。公开行为只取主Spec §9对应API或§10/11内部合同。
- Input/output and state mapping: no negative cache; redis PRIMARY controlled endpoint。
- Error and edge behavior: missing state/epoch mismatch503；read replica禁止。
- Standards impact: MC-ARCH-001, MC-SCOPE-001, MC-TEST-001, MC-VALID-001；按已选传统分层，输入校验与权限先于状态读取，PO不出边界，相关日志脱敏。
- Literal rule enforcement: Rule 2, Rule 4, Rule 9, Rule 11；此文件直接按所列原始规则落实命名、验证、类型/注入或配置，其他规则由同Step关联文件负责。
- Implementation pseudocode:

```java
INPUT -> no negative cache; redis PRIMARY controlled endpoint。
FLOW -> @Component("oauthSecurityProjectionComponent") @Slf4j @RequiredArgsConstructor; EVAL compare issuerHash/realm/client/subject generations, deny keys, epoch; write only revision higher; return VALID/REVOKED/UNAVAILABLE
ON INVALID/CONCURRENT/DEPENDENCY FAILURE -> missing state/epoch mismatch503；read replica禁止。
RESULT -> only the named symbol/contract in this File block becomes available; preserve tenant, expectedVersion, scope, UTC time and secret masking stated above.
```

- Verification contribution: 本Step `mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=TokenRevocationServiceTest -Dsurefire.failIfNoSpecifiedTests=false test`；此文件负责 oauthSecurityProjectionComponent 的正例与 missing state/epoch mismatch503；read replica禁止 负例。
- After this file: oauthSecurityProjectionComponent 的字段/入口或注册已落盘；与下一文件组合后本Step才能GREEN，不能把中间状态当完成。

#### File 11 — `CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/service/impl/TokenRevocationServiceImpl.java`

- Purpose: 先DB事实/Outbox后Redis可见确认。
- Symbols: `revoke/revokeSubject`。
- Repository evidence: 当前不存在；Spec §8/§11 明确目标，邻近包可验证；目标源于主Spec §7–§16。
- Dependencies and consumers: 输入依赖 Step 9 与本Step此前文件；输出供 成功撤销后新鉴权拒绝；Redis故障503；审计持久可恢复及后一Step使用；Java Bean/Mapper按模块现有配置发现，前端按现有auth client消费。
- Why now: 前一文件建立本文件需要的字段或调用契约； 先DB事实/Outbox后Redis可见确认在此位置后才可接消费者。
- Contract/signature changes: revoke/revokeSubject；先DB事实/Outbox后Redis可见确认。公开行为只取主Spec §9对应API或§10/11内部合同。
- Input/output and state mapping: idempotent same token/family; subject revoke all realms。
- Error and edge behavior: partial projection→503 + outbox replay；不得200。
- Standards impact: MC-ARCH-001, MC-SCOPE-001, MC-TEST-001, MC-VALID-001；按已选传统分层，输入校验与权限先于状态读取，PO不出边界，相关日志脱敏。
- Literal rule enforcement: Rule 2, Rule 4, Rule 9, Rule 10, Rule 11；此文件直接按所列原始规则落实命名、验证、类型/注入或配置，其他规则由同Step关联文件负责。
- Implementation pseudocode:

```java
INPUT -> idempotent same token/family; subject revoke all realms。
FLOW -> @Service("tokenRevocationService") @Slf4j @RequiredArgsConstructor @Transactional; locks realm→client→subject→authorization→token; append revocation and increment generation/revision; enqueue OAuthSecurityChangedEvent; after commit apply Redis; only ACK after EVAL
ON INVALID/CONCURRENT/DEPENDENCY FAILURE -> partial projection→503 + outbox replay；不得200。
RESULT -> only the named symbol/contract in this File block becomes available; preserve tenant, expectedVersion, scope, UTC time and secret masking stated above.
```

- Verification contribution: 本Step `mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=TokenRevocationServiceTest -Dsurefire.failIfNoSpecifiedTests=false test`；此文件负责 revoke/revokeSubject 的正例与 partial projection→503 + outbox replay；不得200 负例。
- After this file: revoke/revokeSubject 的字段/入口或注册已落盘；与下一文件组合后本Step才能GREEN，不能把中间状态当完成。

#### File 12 — `CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/support/outbox/OAuthSecurityDeliveryHandler.java`

- Purpose: 真实Outbox投影消费。
- Symbols: `oauthSecurityDeliveryHandler`。
- Repository evidence: 当前不存在；Spec §8/§11 明确目标，邻近包可验证；目标源于主Spec §7–§16。
- Dependencies and consumers: 输入依赖 Step 9 与本Step此前文件；输出供 成功撤销后新鉴权拒绝；Redis故障503；审计持久可恢复及后一Step使用；Java Bean/Mapper按模块现有配置发现，前端按现有auth client消费。
- Why now: 前一文件建立本文件需要的字段或调用契约； 真实Outbox投影消费在此位置后才可接消费者。
- Contract/signature changes: oauthSecurityDeliveryHandler；真实Outbox投影消费。公开行为只取主Spec §9对应API或§10/11内部合同。
- Input/output and state mapping: event contains no raw token; token digest/ids only。
- Error and edge behavior: unknown schema/issuer拒绝+DLQ。
- Standards impact: MC-ARCH-001, MC-SCOPE-001, MC-TEST-001, MC-VALID-001；按已选传统分层，输入校验与权限先于状态读取，PO不出边界，相关日志脱敏。
- Literal rule enforcement: Rule 1, Rule 2, Rule 4, Rule 9, Rule 10, Rule 11；此文件直接按所列原始规则落实命名、验证、类型/注入或配置，其他规则由同Step关联文件负责。
- Implementation pseudocode:

```java
INPUT -> event contains no raw token; token digest/ids only。
FLOW -> DeliveryHandler.channel="oauth-security"; validate issuer registry/schema1/revision; call projection.applyMax; duplicate and out-of-order idempotent; failures retry1–60s, 30 attempts DEAD
ON INVALID/CONCURRENT/DEPENDENCY FAILURE -> unknown schema/issuer拒绝+DLQ。
RESULT -> only the named symbol/contract in this File block becomes available; preserve tenant, expectedVersion, scope, UTC time and secret masking stated above.
```

- Verification contribution: 本Step `mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=TokenRevocationServiceTest -Dsurefire.failIfNoSpecifiedTests=false test`；此文件负责 oauthSecurityDeliveryHandler 的正例与 unknown schema/issuer拒绝+DLQ 负例。
- After this file: oauthSecurityDeliveryHandler 的字段/入口或注册已落盘；与下一文件组合后本Step才能GREEN，不能把中间状态当完成。

#### File 13 — `CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/service/impl/OAuthSigningKeyRotationServiceImpl.java`

- Purpose: 定时90天生命周期。
- Symbols: `rotateDueRealms`。
- Repository evidence: 当前不存在；Spec §8/§11 明确目标，邻近包可验证；目标源于主Spec §7–§16。
- Dependencies and consumers: 输入依赖 Step 9 与本Step此前文件；输出供 成功撤销后新鉴权拒绝；Redis故障503；审计持久可恢复及后一Step使用；Java Bean/Mapper按模块现有配置发现，前端按现有auth client消费。
- Why now: 前一文件建立本文件需要的字段或调用契约； 定时90天生命周期在此位置后才可接消费者。
- Contract/signature changes: rotateDueRealms；定时90天生命周期。公开行为只取主Spec §9对应API或§10/11内部合同。
- Input/output and state mapping: kid permanent, private key encrypted with mounted AES KEK。
- Error and edge behavior: late instance defers activation; cannot remove active key。
- Standards impact: MC-ARCH-001, MC-SCOPE-001, MC-TEST-001, MC-VALID-001；按已选传统分层，输入校验与权限先于状态读取，PO不出边界，相关日志脱敏。
- Literal rule enforcement: Rule 2, Rule 4, Rule 9, Rule 10, Rule 11；此文件直接按所列原始规则落实命名、验证、类型/注入或配置，其他规则由同Step关联文件负责。
- Implementation pseudocode:

```java
INPUT -> kid permanent, private key encrypted with mounted AES KEK。
FLOW -> @Service("oauthSigningKeyRotationService") @Slf4j @RequiredArgsConstructor; daily 02:00 UTC; each realm lock; publish public24h before; check all ready issuers; activate one key/version CAS; old key VERIFY_ONLY through lastIssued+TTL+skew+JWK cache; enqueue event
ON INVALID/CONCURRENT/DEPENDENCY FAILURE -> late instance defers activation; cannot remove active key。
RESULT -> only the named symbol/contract in this File block becomes available; preserve tenant, expectedVersion, scope, UTC time and secret masking stated above.
```

- Verification contribution: 本Step `mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=TokenRevocationServiceTest -Dsurefire.failIfNoSpecifiedTests=false test`；此文件负责 rotateDueRealms 的正例与 late instance defers activation; cannot remove active key 负例。
- After this file: rotateDueRealms 的字段/入口或注册已落盘；与下一文件组合后本Step才能GREEN，不能把中间状态当完成。

#### File 14 — `CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/tianquan/shoubing/starter/audit/TokenUsageAuditComponent.java`

- Purpose: 无DB网关/RS本地持久审核再发Redis Streams。
- Symbols: `tokenUsageAuditComponent`。
- Repository evidence: 当前不存在；Spec §8/§11 明确目标，邻近包可验证；目标源于主Spec §7–§16。
- Dependencies and consumers: 输入依赖 Step 9 与本Step此前文件；输出供 成功撤销后新鉴权拒绝；Redis故障503；审计持久可恢复及后一Step使用；Java Bean/Mapper按模块现有配置发现，前端按现有auth client消费。
- Why now: 前一文件建立本文件需要的字段或调用契约； 无DB网关/RS本地持久审核再发Redis Streams在此位置后才可接消费者。
- Contract/signature changes: tokenUsageAuditComponent；无DB网关/RS本地持久审核再发Redis Streams。公开行为只取主Spec §9对应API或§10/11内部合同。
- Input/output and state mapping: TokenUsageAuditEvent whitelist fields; HMAC issuer+jti, no raw token。
- Error and edge behavior: spool full and Redis unavailable→failclosed; double failure alarm。
- Standards impact: MC-ARCH-001, MC-SCOPE-001, MC-TEST-001, MC-VALID-001；按已选传统分层，输入校验与权限先于状态读取，PO不出边界，相关日志脱敏。
- Literal rule enforcement: Rule 1, Rule 2, Rule 4, Rule 5, Rule 6, Rule 9, Rule 10, Rule 11；此文件直接按所列原始规则落实命名、验证、类型/注入或配置，其他规则由同Step关联文件负责。
- Implementation pseudocode:

```java
INPUT -> TokenUsageAuditEvent whitelist fields; HMAC issuer+jti, no raw token。
FLOW -> @Component("tokenUsageAuditComponent") @Slf4j @RequiredArgsConstructor; append framed CRC event to 0600 spool FileChannel.force(true); XADD to egon:oauth:audit:v1; consumer ACK→delete segment; restart scan valid frames; 1GiB bound
ON INVALID/CONCURRENT/DEPENDENCY FAILURE -> spool full and Redis unavailable→failclosed; double failure alarm。
RESULT -> only the named symbol/contract in this File block becomes available; preserve tenant, expectedVersion, scope, UTC time and secret masking stated above.
```

- Verification contribution: 本Step `mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=TokenRevocationServiceTest -Dsurefire.failIfNoSpecifiedTests=false test`；此文件负责 tokenUsageAuditComponent 的正例与 spool full and Redis unavailable→failclosed; double failure alarm 负例。
- After this file: tokenUsageAuditComponent 的字段/入口或注册已落盘；与下一文件组合后本Step才能GREEN，不能把中间状态当完成。

#### File 15 — `CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/audit/service/impl/OAuthAuditConsumer.java`

- Purpose: Streams MQ消费到OAuthAuditPO。
- Symbols: `oauthAuditConsumer`。
- Repository evidence: 当前不存在；Spec §8/§11 明确目标，邻近包可验证；目标源于主Spec §7–§16。
- Dependencies and consumers: 输入依赖 Step 9 与本Step此前文件；输出供 成功撤销后新鉴权拒绝；Redis故障503；审计持久可恢复及后一Step使用；Java Bean/Mapper按模块现有配置发现，前端按现有auth client消费。
- Why now: 前一文件建立本文件需要的字段或调用契约； Streams MQ消费到OAuthAuditPO在此位置后才可接消费者。
- Contract/signature changes: oauthAuditConsumer；Streams MQ消费到OAuthAuditPO。公开行为只取主Spec §9对应API或§10/11内部合同。
- Input/output and state mapping: safe audit fields only, retention180d。
- Error and edge behavior: no consumer/readiness false, duplicate no duplicate DB row。
- Standards impact: MC-ARCH-001, MC-SCOPE-001, MC-TEST-001, MC-VALID-001；按已选传统分层，输入校验与权限先于状态读取，PO不出边界，相关日志脱敏。
- Literal rule enforcement: Rule 1, Rule 2, Rule 4, Rule 6, Rule 9, Rule 10, Rule 11；此文件直接按所列原始规则落实命名、验证、类型/注入或配置，其他规则由同Step关联文件负责。
- Implementation pseudocode:

```java
INPUT -> safe audit fields only, retention180d。
FLOW -> @Service("oauthAuditConsumer") @Slf4j @RequiredArgsConstructor; group oauth-audit-persist-v1; batch100, validate schema/issuer, insert unique eventId+tenant, then XACK; pending60s reclaim, 30 attempts dead stream
ON INVALID/CONCURRENT/DEPENDENCY FAILURE -> no consumer/readiness false, duplicate no duplicate DB row。
RESULT -> only the named symbol/contract in this File block becomes available; preserve tenant, expectedVersion, scope, UTC time and secret masking stated above.
```

- Verification contribution: 本Step `mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=TokenRevocationServiceTest -Dsurefire.failIfNoSpecifiedTests=false test`；此文件负责 oauthAuditConsumer 的正例与 no consumer/readiness false, duplicate no duplicate DB row 负例。
- After this file: oauthAuditConsumer 的字段/入口或注册已落盘；与下一文件组合后本Step才能GREEN，不能把中间状态当完成。

#### File 16 — `MODIFY egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/identity/service/impl/IdentityUserServiceImpl.java`

- Purpose: 旧账户安全变更接新安全世代。
- Symbols: `revokeAll/password change callback`。
- Repository evidence: 当前文件存在：egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/identity/service/impl/IdentityUserServiceImpl.java；目标源于主Spec §7–§16。
- Dependencies and consumers: 输入依赖 Step 9 与本Step此前文件；输出供 成功撤销后新鉴权拒绝；Redis故障503；审计持久可恢复及后一Step使用；Java Bean/Mapper按模块现有配置发现，前端按现有auth client消费。
- Why now: 前一文件建立本文件需要的字段或调用契约； 旧账户安全变更接新安全世代在此位置后才可接消费者。
- Contract/signature changes: revokeAll/password change callback；旧账户安全变更接新安全世代。公开行为只取主Spec §9对应API或§10/11内部合同。
- Input/output and state mapping: subject stable, generation across realms。
- Error and edge behavior: failure cannot leave issued token valid silently。
- Standards impact: MC-ARCH-001, MC-SCOPE-001, MC-TEST-001, MC-VALID-001；按已选传统分层，输入校验与权限先于状态读取，PO不出边界，相关日志脱敏。
- Literal rule enforcement: Rule 2, Rule 4, Rule 9, Rule 11；此文件直接按所列原始规则落实命名、验证、类型/注入或配置，其他规则由同Step关联文件负责。
- Implementation pseudocode:

```java
INPUT -> subject stable, generation across realms。
FLOW -> existing IdentityUserService remains JPA directory owner; call TokenRevocationService.revokeSubject under same physical transaction; old refresh port only on legacy route until cutover
ON INVALID/CONCURRENT/DEPENDENCY FAILURE -> failure cannot leave issued token valid silently。
RESULT -> only the named symbol/contract in this File block becomes available; preserve tenant, expectedVersion, scope, UTC time and secret masking stated above.
```

- Verification contribution: 本Step `mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=TokenRevocationServiceTest -Dsurefire.failIfNoSpecifiedTests=false test`；此文件负责 revokeAll/password change callback 的正例与 failure cannot leave issued token valid silently 负例。
- After this file: revokeAll/password change callback 的字段/入口或注册已落盘；与下一文件组合后本Step才能GREEN，不能把中间状态当完成。

- Validation working directory: `/Users/mario/SelfProject/Egon-COLA`。
- Verification command: `mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=TokenRevocationServiceTest -Dsurefire.failIfNoSpecifiedTests=false test`。
- Expected result: 先确认上述RED由缺失目标行为引起，GREEN时命名用例成功、模块编译成功、`git diff --check`零错误；不以不存在依赖/数据库/fixture而失败作为RED。
- Failure returns to: Step 9 的编译/Schema前置；若需改变外部语义、架构或DDL版本则回主Spec。
- Completion criteria: 成功撤销后新鉴权拒绝；Redis故障503；审计持久可恢复；本Step的所有Manual Checks在实际执行时给出证据，未通过不能提交。
- Rollback: 提交前仅恢复本Step的精确路径；DDL已对真实数据运行后只forward-fix和新版本，不能编辑既有SQL/历史或恢复旧安全世代。
- Commit paths: `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/service/impl/OAuthRetentionServiceImpl.java`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/service/OAuthRetentionService.java`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/service/OAuthRetentionServiceTest.java`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/tianquan/shoubing/starter/audit/OAuthAuditProperties.java`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/domain/command/OAuthSubjectRevokeCommand.java`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/domain/command/RevokeOAuthTokenCommand.java`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-core/src/main/java/top/egon/cola/platform/tianquan/shoubing/core/audit/TokenUsageAuditEvent.java`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-core/src/main/java/top/egon/cola/platform/tianquan/shoubing/core/audit/OAuthSecurityChangedEvent.java`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/tianquan/shoubing/admin/token/TokenRevocationServiceTest.java`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/service/impl/OAuthSecurityProjectionComponent.java`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/service/impl/TokenRevocationServiceImpl.java`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/support/outbox/OAuthSecurityDeliveryHandler.java`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/service/impl/OAuthSigningKeyRotationServiceImpl.java`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/tianquan/shoubing/starter/audit/TokenUsageAuditComponent.java`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/audit/service/impl/OAuthAuditConsumer.java`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/identity/service/impl/IdentityUserServiceImpl.java`。
- Commit: `feat(shoubing): enforce shared revocation and durable audit`。

### Step 11 — 在资源服务器建立多issuer本地验签和租户上下文

- Requirements: REQ-002, REQ-006, REQ-007, REQ-008, REQ-012, REQ-016
- Dependencies: Step 10
- Baseline state: 当前HEAD的对应源码/测试如每File的Repository evidence，先前Step已完成并有其focused GREEN；本Step尚不存在twoIssuersWrongAudAndCleanup所列新行为。
- Observable outcome: 所有受保护资源请求本地RS256后共享撤销检查，Servlet ThreadLocal/MDC最终清理。
- End state: 所有受保护资源请求本地RS256后共享撤销检查，Servlet ThreadLocal/MDC最终清理；后续Step的Consumer尚未切流，不声明完整生产验收。
- Test-first gate: Required — 新多issuer/tenant/Redis故障测试RED；创建resolver/Bridge之后GREEN。
- Manual Checks: MC-ARCH-001, MC-REUSE-001, MC-SCOPE-001, MC-TEST-001, MC-VALID-001, MC-NAME-001, MC-MODEL-001, MC-CONVERT-001, MC-LOG-001, MC-BEAN-001, MC-JSON-001, MC-TIME-001
- Literal Rules: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11
- Ordered files:

#### File 1 — `CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/test/java/top/egon/cola/platform/tianquan/shoubing/starter/security/MultiIssuerResourceServerTest.java`

- Purpose: RED当前Verifier单issuer/tid。
- Symbols: `twoIssuersWrongAudAndCleanup`。
- Repository evidence: 当前不存在；Spec §8/§11 明确目标，邻近包可验证；目标源于主Spec §7–§16。
- Dependencies and consumers: 输入依赖 Step 10 与本Step此前文件；输出供 所有受保护资源请求本地RS256后共享撤销检查，Servlet ThreadLocal/MDC最终清理及后一Step使用；Java Bean/Mapper按模块现有配置发现，前端按现有auth client消费。
- Why now: 本Step的编译前置/RED契约先锁定； RED当前Verifier单issuer/tid在此位置后才可接消费者。
- Contract/signature changes: twoIssuersWrongAudAndCleanup；RED当前Verifier单issuer/tid。公开行为只取主Spec §9对应API或§10/11内部合同。
- Input/output and state mapping: token fixture prebuilt, no AS call per knownJWKS hit。
- Error and edge behavior: old verifier accepts only fixed issuer -> targeted RED。
- Standards impact: MC-ARCH-001, MC-SCOPE-001, MC-TEST-001, MC-VALID-001；按已选传统分层，输入校验与权限先于状态读取，PO不出边界，相关日志脱敏。
- Literal rule enforcement: Rule 11, Rule 2, Rule 4；此文件直接按所列原始规则落实命名、验证、类型/注入或配置，其他规则由同Step关联文件负责。
- Implementation pseudocode:

```java
INPUT -> token fixture prebuilt, no AS call per knownJWKS hit。
FLOW -> A/B RS256 keys, kid collision, unknown issuer/jku, IDToken, wrong aud/tenant, Redis timeout, thread reuse; assert403/401/503 and MDC cleared
ON INVALID/CONCURRENT/DEPENDENCY FAILURE -> old verifier accepts only fixed issuer -> targeted RED。
RESULT -> only the named symbol/contract in this File block becomes available; preserve tenant, expectedVersion, scope, UTC time and secret masking stated above.
```

- Verification contribution: 本Step `mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter -am -Dtest=MultiIssuerResourceServerTest -Dsurefire.failIfNoSpecifiedTests=false test`；此文件负责 twoIssuersWrongAudAndCleanup 的正例与 old verifier accepts only fixed issuer -> targeted RED 负例。
- After this file: twoIssuersWrongAudAndCleanup 的字段/入口或注册已落盘；与下一文件组合后本Step才能GREEN，不能把中间状态当完成。

#### File 2 — `CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/tianquan/shoubing/starter/security/domain/VerifiedJwtBO.java`

- Purpose: 关闭 `VerifiedJwtBO` 在主Spec中的字段/消费依赖。
- Symbols: `VerifiedJwtBO`。
- Repository evidence: 当前不存在；主Spec §7.3.2/§10 命名此对象/字段；现有相邻业务包提供放置证据。
- Dependencies and consumers: 本Step既有File块调用该类型；数据来源/消费者为主Spec §7.3.2/§10，不产生新HTTP路由。
- Why now: 该类型按本Step的测试/编译依赖顺序落位，以免由实现者临时发明。
- Contract/signature changes: issuer/tenantId?/jti/kid/authorizationId/clientRegistrationId/securityVersion/principalType/expiresAt from verified signature only。
- Input/output and state mapping: 精确字段由主Spec §7.3.2/§10 指定，realm从签名/注册得出，Instant为UTC，版本单调，秘密字段仅服务器内部。
- Error and edge behavior: null/未知字段/跨tenant/重复版本拒绝；不转成宽松缺省或吞异常。
- Standards impact: MC-ARCH-001, MC-NAME-001, MC-VALID-001, MC-MODEL-001, MC-CONVERT-001, MC-BEAN-001, MC-JSON-001, MC-TIME-001, MC-SCOPE-001, MC-TEST-001；行为Bean使用具名final Qualifier、@RequiredArgsConstructor和@Slf4j，载体遵循四注解与Builder。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 6, Rule 9, Rule 10, Rule 11；class按POJO语义命名，边界注解验证，跨层转换只用既有BaseConverter，Jackson和java.time字段明确。
- Implementation pseudocode:

```java
INPUT -> 仅接受主Spec §7.3.2/§10 的可信issuer/realm/输入字段；调用前 @Valid 或 ValueObject不变量检查。
FLOW -> issuer/tenantId?/jti/kid/authorizationId/clientRegistrationId/securityVersion/principalType/expiresAt from verified signature only。
OUTPUT -> VerifiedJwtBO 的Java方法、DTO字段或配置可被本Step既有调用者编译使用；不新增业务API。
FAIL -> 空值/跨tenant/未知schema/重复版本/外部依赖错误按本Step已列错误模型fail-closed，不能输出原secret/token。
```

- Verification contribution: Step 11 的focus测试对 `VerifiedJwtBO` 的字段、装配、负例及不泄密断言。
- After this file: `VerifiedJwtBO` 成为已声明的Step依赖，下一文件可按原合同引用；未完成整条行为前不宣称GREEN。

#### File 3 — `CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/tianquan/shoubing/starter/security/VerifiedPrincipalConverter.java`

- Purpose: 关闭 `VerifiedPrincipalConverter` 在主Spec中的字段/消费依赖。
- Symbols: `VerifiedPrincipalConverter`。
- Repository evidence: 当前不存在；主Spec §7.3.2/§10 命名此对象/字段；现有相邻业务包提供放置证据。
- Dependencies and consumers: 本Step既有File块调用该类型；数据来源/消费者为主Spec §7.3.2/§10，不产生新HTTP路由。
- Why now: 该类型按本Step的测试/编译依赖顺序落位，以免由实现者临时发明。
- Contract/signature changes: BaseForwardConverter<Jwt,IdentityPrincipal> with MapStruct; user claims/tid legacy strictly separated; no manual setters。
- Input/output and state mapping: 精确字段由主Spec §7.3.2/§10 指定，realm从签名/注册得出，Instant为UTC，版本单调，秘密字段仅服务器内部。
- Error and edge behavior: null/未知字段/跨tenant/重复版本拒绝；不转成宽松缺省或吞异常。
- Standards impact: MC-ARCH-001, MC-NAME-001, MC-VALID-001, MC-MODEL-001, MC-CONVERT-001, MC-BEAN-001, MC-JSON-001, MC-TIME-001, MC-SCOPE-001, MC-TEST-001；行为Bean使用具名final Qualifier、@RequiredArgsConstructor和@Slf4j，载体遵循四注解与Builder。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 6, Rule 9, Rule 10, Rule 11；class按POJO语义命名，边界注解验证，跨层转换只用既有BaseConverter，Jackson和java.time字段明确。
- Implementation pseudocode:

```java
INPUT -> 仅接受主Spec §7.3.2/§10 的可信issuer/realm/输入字段；调用前 @Valid 或 ValueObject不变量检查。
FLOW -> BaseForwardConverter<Jwt,IdentityPrincipal> with MapStruct; user claims/tid legacy strictly separated; no manual setters。
OUTPUT -> VerifiedPrincipalConverter 的Java方法、DTO字段或配置可被本Step既有调用者编译使用；不新增业务API。
FAIL -> 空值/跨tenant/未知schema/重复版本/外部依赖错误按本Step已列错误模型fail-closed，不能输出原secret/token。
```

- Verification contribution: Step 11 的focus测试对 `VerifiedPrincipalConverter` 的字段、装配、负例及不泄密断言。
- After this file: `VerifiedPrincipalConverter` 成为已声明的Step依赖，下一文件可按原合同引用；未完成整条行为前不宣称GREEN。

#### File 4 — `CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/tianquan/shoubing/starter/security/OAuthRevocationReader.java`

- Purpose: 关闭 `OAuthRevocationReader` 在主Spec中的字段/消费依赖。
- Symbols: `OAuthRevocationReader`。
- Repository evidence: 当前不存在；主Spec §7.3.2/§10 命名此对象/字段；现有相邻业务包提供放置证据。
- Dependencies and consumers: 本Step既有File块调用该类型；数据来源/消费者为主Spec §7.3.2/§10，不产生新HTTP路由。
- Why now: 该类型按本Step的测试/编译依赖顺序落位，以免由实现者临时发明。
- Contract/signature changes: check(VerifiedJwtBO) returns VALID/REVOKED/UNAVAILABLE from shared safety state。
- Input/output and state mapping: 精确字段由主Spec §7.3.2/§10 指定，realm从签名/注册得出，Instant为UTC，版本单调，秘密字段仅服务器内部。
- Error and edge behavior: null/未知字段/跨tenant/重复版本拒绝；不转成宽松缺省或吞异常。
- Standards impact: MC-ARCH-001, MC-NAME-001, MC-VALID-001, MC-MODEL-001, MC-CONVERT-001, MC-BEAN-001, MC-JSON-001, MC-TIME-001, MC-SCOPE-001, MC-TEST-001；行为Bean使用具名final Qualifier、@RequiredArgsConstructor和@Slf4j，载体遵循四注解与Builder。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 6, Rule 9, Rule 10, Rule 11；class按POJO语义命名，边界注解验证，跨层转换只用既有BaseConverter，Jackson和java.time字段明确。
- Implementation pseudocode:

```java
INPUT -> 仅接受主Spec §7.3.2/§10 的可信issuer/realm/输入字段；调用前 @Valid 或 ValueObject不变量检查。
FLOW -> check(VerifiedJwtBO) returns VALID/REVOKED/UNAVAILABLE from shared safety state。
OUTPUT -> OAuthRevocationReader 的Java方法、DTO字段或配置可被本Step既有调用者编译使用；不新增业务API。
FAIL -> 空值/跨tenant/未知schema/重复版本/外部依赖错误按本Step已列错误模型fail-closed，不能输出原secret/token。
```

- Verification contribution: Step 11 的focus测试对 `OAuthRevocationReader` 的字段、装配、负例及不泄密断言。
- After this file: `OAuthRevocationReader` 成为已声明的Step依赖，下一文件可按原合同引用；未完成整条行为前不宣称GREEN。

#### File 5 — `CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/tianquan/shoubing/starter/security/impl/RedisOAuthRevocationReader.java`

- Purpose: 关闭 `RedisOAuthRevocationReader` 在主Spec中的字段/消费依赖。
- Symbols: `RedisOAuthRevocationReader`。
- Repository evidence: 当前不存在；主Spec §7.3.2/§10 命名此对象/字段；现有相邻业务包提供放置证据。
- Dependencies and consumers: 本Step既有File块调用该类型；数据来源/消费者为主Spec §7.3.2/§10，不产生新HTTP路由。
- Why now: 该类型按本Step的测试/编译依赖顺序落位，以免由实现者临时发明。
- Contract/signature changes: @Component("oauthRevocationReader") Redis EVAL one exact issuer group; no negative cache; missing generation/epoch→UNAVAILABLE。
- Input/output and state mapping: 精确字段由主Spec §7.3.2/§10 指定，realm从签名/注册得出，Instant为UTC，版本单调，秘密字段仅服务器内部。
- Error and edge behavior: null/未知字段/跨tenant/重复版本拒绝；不转成宽松缺省或吞异常。
- Standards impact: MC-ARCH-001, MC-NAME-001, MC-VALID-001, MC-MODEL-001, MC-CONVERT-001, MC-BEAN-001, MC-JSON-001, MC-TIME-001, MC-SCOPE-001, MC-TEST-001；行为Bean使用具名final Qualifier、@RequiredArgsConstructor和@Slf4j，载体遵循四注解与Builder。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 6, Rule 9, Rule 10, Rule 11；class按POJO语义命名，边界注解验证，跨层转换只用既有BaseConverter，Jackson和java.time字段明确。
- Implementation pseudocode:

```java
INPUT -> 仅接受主Spec §7.3.2/§10 的可信issuer/realm/输入字段；调用前 @Valid 或 ValueObject不变量检查。
FLOW -> @Component("oauthRevocationReader") Redis EVAL one exact issuer group; no negative cache; missing generation/epoch→UNAVAILABLE。
OUTPUT -> RedisOAuthRevocationReader 的Java方法、DTO字段或配置可被本Step既有调用者编译使用；不新增业务API。
FAIL -> 空值/跨tenant/未知schema/重复版本/外部依赖错误按本Step已列错误模型fail-closed，不能输出原secret/token。
```

- Verification contribution: Step 11 的focus测试对 `RedisOAuthRevocationReader` 的字段、装配、负例及不泄密断言。
- After this file: `RedisOAuthRevocationReader` 成为已声明的Step依赖，下一文件可按原合同引用；未完成整条行为前不宣称GREEN。

#### File 6 — `CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/tianquan/shoubing/starter/security/reactive/ReactiveOAuthRevocationReader.java`

- Purpose: 关闭 `ReactiveOAuthRevocationReader` 在主Spec中的字段/消费依赖。
- Symbols: `ReactiveOAuthRevocationReader`。
- Repository evidence: 当前不存在；主Spec §7.3.2/§10 命名此对象/字段；现有相邻业务包提供放置证据。
- Dependencies and consumers: 本Step既有File块调用该类型；数据来源/消费者为主Spec §7.3.2/§10，不产生新HTTP路由。
- Why now: 该类型按本Step的测试/编译依赖顺序落位，以免由实现者临时发明。
- Contract/signature changes: Reactive variant for SCG/Netty uses Redisson reactive completion/Reactor Context, never blocks event loop。
- Input/output and state mapping: 精确字段由主Spec §7.3.2/§10 指定，realm从签名/注册得出，Instant为UTC，版本单调，秘密字段仅服务器内部。
- Error and edge behavior: null/未知字段/跨tenant/重复版本拒绝；不转成宽松缺省或吞异常。
- Standards impact: MC-ARCH-001, MC-NAME-001, MC-VALID-001, MC-MODEL-001, MC-CONVERT-001, MC-BEAN-001, MC-JSON-001, MC-TIME-001, MC-SCOPE-001, MC-TEST-001；行为Bean使用具名final Qualifier、@RequiredArgsConstructor和@Slf4j，载体遵循四注解与Builder。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 6, Rule 9, Rule 10, Rule 11；class按POJO语义命名，边界注解验证，跨层转换只用既有BaseConverter，Jackson和java.time字段明确。
- Implementation pseudocode:

```java
INPUT -> 仅接受主Spec §7.3.2/§10 的可信issuer/realm/输入字段；调用前 @Valid 或 ValueObject不变量检查。
FLOW -> Reactive variant for SCG/Netty uses Redisson reactive completion/Reactor Context, never blocks event loop。
OUTPUT -> ReactiveOAuthRevocationReader 的Java方法、DTO字段或配置可被本Step既有调用者编译使用；不新增业务API。
FAIL -> 空值/跨tenant/未知schema/重复版本/外部依赖错误按本Step已列错误模型fail-closed，不能输出原secret/token。
```

- Verification contribution: Step 11 的focus测试对 `ReactiveOAuthRevocationReader` 的字段、装配、负例及不泄密断言。
- After this file: `ReactiveOAuthRevocationReader` 成为已声明的Step依赖，下一文件可按原合同引用；未完成整条行为前不宣称GREEN。

#### File 7 — `CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/tianquan/shoubing/starter/security/TenantIssuerAuthenticationManagerComponent.java`

- Purpose: 精确可信issuer resolver。
- Symbols: `tenantIssuerAuthenticationManagerComponent`。
- Repository evidence: 当前不存在；Spec §8/§11 明确目标，邻近包可验证；目标源于主Spec §7–§16。
- Dependencies and consumers: 输入依赖 Step 10 与本Step此前文件；输出供 所有受保护资源请求本地RS256后共享撤销检查，Servlet ThreadLocal/MDC最终清理及后一Step使用；Java Bean/Mapper按模块现有配置发现，前端按现有auth client消费。
- Why now: 前一文件建立本文件需要的字段或调用契约； 精确可信issuer resolver在此位置后才可接消费者。
- Contract/signature changes: tenantIssuerAuthenticationManagerComponent；精确可信issuer resolver。公开行为只取主Spec §9对应API或§10/11内部合同。
- Input/output and state mapping: iss unverified only selects preapproved manager; signature always local。
- Error and edge behavior: unknown issuer no dynamic outbound fetch; kid miss only approved jwks refresh。
- Standards impact: MC-ARCH-001, MC-SCOPE-001, MC-TEST-001, MC-VALID-001；按已选传统分层，输入校验与权限先于状态读取，PO不出边界，相关日志脱敏。
- Literal rule enforcement: Rule 2, Rule 4, Rule 9, Rule 10, Rule 11；此文件直接按所列原始规则落实命名、验证、类型/注入或配置，其他规则由同Step关联文件负责。
- Implementation pseudocode:

```java
INPUT -> iss unverified only selects preapproved manager; signature always local。
FLOW -> @Component("tenantIssuerAuthenticationManagerComponent") @Slf4j @RequiredArgsConstructor; trustedIssuerRegistry exact lookup; JwtIssuerAuthenticationManagerResolver fromTrustedIssuers with bounded managers; NimbusJwtDecoder RS256 only, custom validators iss/aud/typ/tenant/scope_context/security_version
ON INVALID/CONCURRENT/DEPENDENCY FAILURE -> unknown issuer no dynamic outbound fetch; kid miss only approved jwks refresh。
RESULT -> only the named symbol/contract in this File block becomes available; preserve tenant, expectedVersion, scope, UTC time and secret masking stated above.
```

- Verification contribution: 本Step `mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter -am -Dtest=MultiIssuerResourceServerTest -Dsurefire.failIfNoSpecifiedTests=false test`；此文件负责 tenantIssuerAuthenticationManagerComponent 的正例与 unknown issuer no dynamic outbound fetch; kid miss only approved jwks refresh 负例。
- After this file: tenantIssuerAuthenticationManagerComponent 的字段/入口或注册已落盘；与下一文件组合后本Step才能GREEN，不能把中间状态当完成。

#### File 8 — `MODIFY egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/tianquan/shoubing/starter/security/IdpJwtVerifier.java`

- Purpose: 从新claims建原IdpPrincipal。
- Symbols: `verifyUser/verifyService`。
- Repository evidence: 当前文件存在：egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/tianquan/shoubing/starter/security/IdpJwtVerifier.java；目标源于主Spec §7–§16。
- Dependencies and consumers: 输入依赖 Step 10 与本Step此前文件；输出供 所有受保护资源请求本地RS256后共享撤销检查，Servlet ThreadLocal/MDC最终清理及后一Step使用；Java Bean/Mapper按模块现有配置发现，前端按现有auth client消费。
- Why now: 前一文件建立本文件需要的字段或调用契约； 从新claims建原IdpPrincipal在此位置后才可接消费者。
- Contract/signature changes: verifyUser/verifyService；从新claims建原IdpPrincipal。公开行为只取主Spec §9对应API或§10/11内部合同。
- Input/output and state mapping: USER no forbidden client_id mistake; SERVICE no platform→business SQL。
- Error and edge behavior: invalid jti/realm 401; reader down503。
- Standards impact: MC-ARCH-001, MC-SCOPE-001, MC-TEST-001, MC-VALID-001；按已选传统分层，输入校验与权限先于状态读取，PO不出边界，相关日志脱敏。
- Literal rule enforcement: Rule 1, Rule 2, Rule 4, Rule 9, Rule 10, Rule 11；此文件直接按所列原始规则落实命名、验证、类型/注入或配置，其他规则由同Step关联文件负责。
- Implementation pseudocode:

```java
INPUT -> USER no forbidden client_id mistake; SERVICE no platform→business SQL。
FLOW -> decode once; check at+jwt, iss,aud,tenant_id, principal_type, security_version; revoked reader; convert to IdentityPrincipal/ServiceIdentityPrincipal; maintain resourceVersion/client state checks
ON INVALID/CONCURRENT/DEPENDENCY FAILURE -> invalid jti/realm 401; reader down503。
RESULT -> only the named symbol/contract in this File block becomes available; preserve tenant, expectedVersion, scope, UTC time and secret masking stated above.
```

- Verification contribution: 本Step `mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter -am -Dtest=MultiIssuerResourceServerTest -Dsurefire.failIfNoSpecifiedTests=false test`；此文件负责 verifyUser/verifyService 的正例与 invalid jti/realm 401; reader down503 负例。
- After this file: verifyUser/verifyService 的字段/入口或注册已落盘；与下一文件组合后本Step才能GREEN，不能把中间状态当完成。

#### File 9 — `CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/tianquan/shoubing/starter/security/VerifiedTenantContextFilter.java`

- Purpose: 在Spring Security验证后绑定旧TenantContext+MDC。
- Symbols: `doFilterInternal/finally`。
- Repository evidence: 当前不存在；Spec §8/§11 明确目标，邻近包可验证；目标源于主Spec §7–§16。
- Dependencies and consumers: 输入依赖 Step 10 与本Step此前文件；输出供 所有受保护资源请求本地RS256后共享撤销检查，Servlet ThreadLocal/MDC最终清理及后一Step使用；Java Bean/Mapper按模块现有配置发现，前端按现有auth client消费。
- Why now: 前一文件建立本文件需要的字段或调用契约； 在Spring Security验证后绑定旧TenantContext+MDC在此位置后才可接消费者。
- Contract/signature changes: doFilterInternal/finally；在Spring Security验证后绑定旧TenantContext+MDC。公开行为只取主Spec §9对应API或§10/11内部合同。
- Input/output and state mapping: platform bypass only control routes; never default tenant0。
- Error and edge behavior: exceptions/403 and pooled thread leave no state。
- Standards impact: MC-ARCH-001, MC-SCOPE-001, MC-TEST-001, MC-VALID-001；按已选传统分层，输入校验与权限先于状态读取，PO不出边界，相关日志脱敏。
- Literal rule enforcement: Rule 2, Rule 4, Rule 9, Rule 11；此文件直接按所列原始规则落实命名、验证、类型/注入或配置，其他规则由同Step关联文件负责。
- Implementation pseudocode:

```java
INPUT -> platform bypass only control routes; never default tenant0。
FLOW -> OncePerRequestFilter; try { assert principal.tenantId equals issuer registry and resource; TenantContext.set(...); MDC.put(tenantId,userId); chain.doFilter; } finally { TenantContext.clear(); MDC.remove tenantId/userId; } nested scope restores previous
ON INVALID/CONCURRENT/DEPENDENCY FAILURE -> exceptions/403 and pooled thread leave no state。
RESULT -> only the named symbol/contract in this File block becomes available; preserve tenant, expectedVersion, scope, UTC time and secret masking stated above.
```

- Verification contribution: 本Step `mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter -am -Dtest=MultiIssuerResourceServerTest -Dsurefire.failIfNoSpecifiedTests=false test`；此文件负责 doFilterInternal/finally 的正例与 exceptions/403 and pooled thread leave no state 负例。
- After this file: doFilterInternal/finally 的字段/入口或注册已落盘；与下一文件组合后本Step才能GREEN，不能把中间状态当完成。

#### File 10 — `CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/tianquan/shoubing/starter/security/LegacyIssuerCutoverPolicy.java`

- Purpose: 旧issuer按已签名tid阻断已切tenant。
- Symbols: `isLegacyTenantAllowed`。
- Repository evidence: 当前不存在；Spec §8/§11 明确目标，邻近包可验证；目标源于主Spec §7–§16。
- Dependencies and consumers: 输入依赖 Step 10 与本Step此前文件；输出供 所有受保护资源请求本地RS256后共享撤销检查，Servlet ThreadLocal/MDC最终清理及后一Step使用；Java Bean/Mapper按模块现有配置发现，前端按现有auth client消费。
- Why now: 前一文件建立本文件需要的字段或调用契约； 旧issuer按已签名tid阻断已切tenant在此位置后才可接消费者。
- Contract/signature changes: isLegacyTenantAllowed；旧issuer按已签名tid阻断已切tenant。公开行为只取主Spec §9对应API或§10/11内部合同。
- Input/output and state mapping: migration allowlist monotonically shrinks; not indefinite dualchain。
- Error and edge behavior: old AT/RT cannot be reused after tenant cutover。
- Standards impact: MC-ARCH-001, MC-SCOPE-001, MC-TEST-001, MC-VALID-001；按已选传统分层，输入校验与权限先于状态读取，PO不出边界，相关日志脱敏。
- Literal rule enforcement: Rule 2, Rule 4, Rule 9, Rule 11；此文件直接按所列原始规则落实命名、验证、类型/注入或配置，其他规则由同Step关联文件负责。
- Implementation pseudocode:

```java
INPUT -> migration allowlist monotonically shrinks; not indefinite dualchain。
FLOW -> only after legacy JWT signature verify: read allowlist version in shared controlled Redis; if tid removed or unavailable deny; new issuer bypass legacy policy
ON INVALID/CONCURRENT/DEPENDENCY FAILURE -> old AT/RT cannot be reused after tenant cutover。
RESULT -> only the named symbol/contract in this File block becomes available; preserve tenant, expectedVersion, scope, UTC time and secret masking stated above.
```

- Verification contribution: 本Step `mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter -am -Dtest=MultiIssuerResourceServerTest -Dsurefire.failIfNoSpecifiedTests=false test`；此文件负责 isLegacyTenantAllowed 的正例与 old AT/RT cannot be reused after tenant cutover 负例。
- After this file: isLegacyTenantAllowed 的字段/入口或注册已落盘；与下一文件组合后本Step才能GREEN，不能把中间状态当完成。

#### File 11 — `MODIFY egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/tianquan/shoubing/starter/autoconfigure/IdpStarterAutoConfiguration.java`

- Purpose: 注册可信resolver/验证器/审计Reader/Filter。
- Symbols: `security beans order and properties`。
- Repository evidence: 当前文件存在：egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/tianquan/shoubing/starter/autoconfigure/IdpStarterAutoConfiguration.java；目标源于主Spec §7–§16。
- Dependencies and consumers: 输入依赖 Step 10 与本Step此前文件；输出供 所有受保护资源请求本地RS256后共享撤销检查，Servlet ThreadLocal/MDC最终清理及后一Step使用；Java Bean/Mapper按模块现有配置发现，前端按现有auth client消费。
- Why now: 前一文件建立本文件需要的字段或调用契约； 注册可信resolver/验证器/审计Reader/Filter在此位置后才可接消费者。
- Contract/signature changes: security beans order and properties；注册可信resolver/验证器/审计Reader/Filter。公开行为只取主Spec §9对应API或§10/11内部合同。
- Input/output and state mapping: no temp HTTP header used as tenant trust。
- Error and edge behavior: missing beans fail startup; no old always-on decoder。
- Standards impact: MC-ARCH-001, MC-SCOPE-001, MC-TEST-001, MC-VALID-001；按已选传统分层，输入校验与权限先于状态读取，PO不出边界，相关日志脱敏。
- Literal rule enforcement: Rule 2, Rule 4, Rule 7, Rule 9, Rule 11；此文件直接按所列原始规则落实命名、验证、类型/注入或配置，其他规则由同Step关联文件负责。
- Implementation pseudocode:

```java
INPUT -> no temp HTTP header used as tenant trust。
FLOW -> @Bean exact names trustedIssuerRegistry,oauthRevocationReader,verifiedPrincipalConverter; @Qualifier propagation in starter lombok.config; filter after jwt before RBAC/business; reactive sibling uses Reactor Context not Servlet ThreadLocal
ON INVALID/CONCURRENT/DEPENDENCY FAILURE -> missing beans fail startup; no old always-on decoder。
RESULT -> only the named symbol/contract in this File block becomes available; preserve tenant, expectedVersion, scope, UTC time and secret masking stated above.
```

- Verification contribution: 本Step `mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter -am -Dtest=MultiIssuerResourceServerTest -Dsurefire.failIfNoSpecifiedTests=false test`；此文件负责 security beans order and properties 的正例与 missing beans fail startup; no old always-on decoder 负例。
- After this file: security beans order and properties 的字段/入口或注册已落盘；与下一文件组合后本Step才能GREEN，不能把中间状态当完成。

#### File 12 — `CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/lombok.config`

- Purpose: 对Starter新增Bean满足注入规范。
- Symbols: `Qualifier propagation`。
- Repository evidence: 当前不存在；Spec §8/§11 明确目标，邻近包可验证；目标源于主Spec §7–§16。
- Dependencies and consumers: 输入依赖 Step 10 与本Step此前文件；输出供 所有受保护资源请求本地RS256后共享撤销检查，Servlet ThreadLocal/MDC最终清理及后一Step使用；Java Bean/Mapper按模块现有配置发现，前端按现有auth client消费。
- Why now: 前一文件建立本文件需要的字段或调用契约； 对Starter新增Bean满足注入规范在此位置后才可接消费者。
- Contract/signature changes: Qualifier propagation；对Starter新增Bean满足注入规范。公开行为只取主Spec §9对应API或§10/11内部合同。
- Input/output and state mapping: consistent with SA config。
- Error and edge behavior: constructor reflection test fails if qualifier lost。
- Standards impact: MC-SCOPE-001, MC-TEST-001；按已选传统分层，输入校验与权限先于状态读取，PO不出边界，相关日志脱敏。
- Literal rule enforcement: Rule 4, Rule 11；此文件直接按所列原始规则落实命名、验证、类型/注入或配置，其他规则由同Step关联文件负责。
- Implementation pseudocode:

```text
INPUT -> consistent with SA config。
FLOW -> stopBubbling=true; lombok.copyableAnnotations+=org.springframework.beans.factory.annotation.Qualifier
ON INVALID/CONCURRENT/DEPENDENCY FAILURE -> constructor reflection test fails if qualifier lost。
RESULT -> only the named symbol/contract in this File block becomes available; preserve tenant, expectedVersion, scope, UTC time and secret masking stated above.
```

- Verification contribution: 本Step `mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter -am -Dtest=MultiIssuerResourceServerTest -Dsurefire.failIfNoSpecifiedTests=false test`；此文件负责 Qualifier propagation 的正例与 constructor reflection test fails if qualifier lost 负例。
- After this file: Qualifier propagation 的字段/入口或注册已落盘；与下一文件组合后本Step才能GREEN，不能把中间状态当完成。

- Validation working directory: `/Users/mario/SelfProject/Egon-COLA`。
- Verification command: `mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter -am -Dtest=MultiIssuerResourceServerTest -Dsurefire.failIfNoSpecifiedTests=false test`。
- Expected result: 先确认上述RED由缺失目标行为引起，GREEN时命名用例成功、模块编译成功、`git diff --check`零错误；不以不存在依赖/数据库/fixture而失败作为RED。
- Failure returns to: Step 10 的编译/Schema前置；若需改变外部语义、架构或DDL版本则回主Spec。
- Completion criteria: 所有受保护资源请求本地RS256后共享撤销检查，Servlet ThreadLocal/MDC最终清理；本Step的所有Manual Checks在实际执行时给出证据，未通过不能提交。
- Rollback: 提交前仅恢复本Step的精确路径；DDL已对真实数据运行后只forward-fix和新版本，不能编辑既有SQL/历史或恢复旧安全世代。
- Commit paths: `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/tianquan/shoubing/starter/security/reactive/ReactiveOAuthRevocationReader.java`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/tianquan/shoubing/starter/security/impl/RedisOAuthRevocationReader.java`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/tianquan/shoubing/starter/security/OAuthRevocationReader.java`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/tianquan/shoubing/starter/security/VerifiedPrincipalConverter.java`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/tianquan/shoubing/starter/security/domain/VerifiedJwtBO.java`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/test/java/top/egon/cola/platform/tianquan/shoubing/starter/security/MultiIssuerResourceServerTest.java`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/tianquan/shoubing/starter/security/TenantIssuerAuthenticationManagerComponent.java`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/tianquan/shoubing/starter/security/IdpJwtVerifier.java`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/tianquan/shoubing/starter/security/VerifiedTenantContextFilter.java`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/tianquan/shoubing/starter/security/LegacyIssuerCutoverPolicy.java`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/tianquan/shoubing/starter/autoconfigure/IdpStarterAutoConfiguration.java`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/lombok.config`。
- Commit: `feat(shoubing): verify tenant jwt locally with strict revocation`。

### Step 12 — 改造机器客户端缓存并装配WebClient

- Requirements: REQ-005, REQ-009, REQ-011, REQ-016
- Dependencies: Step 11
- Baseline state: 当前HEAD的对应源码/测试如每File的Repository evidence，先前Step已完成并有其focused GREEN；本Step尚不存在renewAtSkewAndSingleFlightByCompleteKey所列新行为。
- Observable outcome: 出站调用按issuer/client/凭据版/aud/tenant/scope隔离并临期重获，无client_credentials RT。
- End state: 出站调用按issuer/client/凭据版/aud/tenant/scope隔离并临期重获，无client_credentials RT；后续Step的Consumer尚未切流，不声明完整生产验收。
- Test-first gate: Required — 旧缓存键/TTL对新issuer与凭据版本不满足用例，先RED后GREEN。
- Manual Checks: MC-ARCH-001, MC-REUSE-001, MC-SCOPE-001, MC-TEST-001, MC-VALID-001, MC-NAME-001, MC-MODEL-001, MC-CONVERT-001, MC-LOG-001, MC-BEAN-001, MC-JSON-001, MC-TIME-001
- Literal Rules: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11
- Ordered files:

#### File 1 — `MODIFY egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/test/java/top/egon/cola/platform/tianquan/shoubing/starter/client/IdpServiceOAuth2ClientTest.java`

- Purpose: RED原key不含issuer/credentialVersion。
- Symbols: `renewAtSkewAndSingleFlightByCompleteKey`。
- Repository evidence: 当前文件存在：egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/test/java/top/egon/cola/platform/tianquan/shoubing/starter/client/IdpServiceOAuth2ClientTest.java；目标源于主Spec §7–§16。
- Dependencies and consumers: 输入依赖 Step 11 与本Step此前文件；输出供 出站调用按issuer/client/凭据版/aud/tenant/scope隔离并临期重获，无client_credentials RT及后一Step使用；Java Bean/Mapper按模块现有配置发现，前端按现有auth client消费。
- Why now: 本Step的编译前置/RED契约先锁定； RED原key不含issuer/credentialVersion在此位置后才可接消费者。
- Contract/signature changes: renewAtSkewAndSingleFlightByCompleteKey；RED原key不含issuer/credentialVersion。公开行为只取主Spec §9对应API或§10/11内部合同。
- Input/output and state mapping: Clock at exp-60s; fake authorized client manager。
- Error and edge behavior: old key collision or wrong manager principal causes RED。
- Standards impact: MC-ARCH-001, MC-SCOPE-001, MC-TEST-001, MC-VALID-001；按已选传统分层，输入校验与权限先于状态读取，PO不出边界，相关日志脱敏。
- Literal rule enforcement: Rule 11, Rule 2, Rule 4；此文件直接按所列原始规则落实命名、验证、类型/注入或配置，其他规则由同Step关联文件负责。
- Implementation pseudocode:

```java
INPUT -> Clock at exp-60s; fake authorized client manager。
FLOW -> same key 100 concurrent authorizations one token call; issuer/tenant/scope/resource/credentialVersion diverge; expired token never reused, 401 GET one retry, POST no blind replay
ON INVALID/CONCURRENT/DEPENDENCY FAILURE -> old key collision or wrong manager principal causes RED。
RESULT -> only the named symbol/contract in this File block becomes available; preserve tenant, expectedVersion, scope, UTC time and secret masking stated above.
```

- Verification contribution: 本Step `mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter -am -Dtest=IdpServiceOAuth2ClientTest -Dsurefire.failIfNoSpecifiedTests=false test`；此文件负责 renewAtSkewAndSingleFlightByCompleteKey 的正例与 old key collision or wrong manager principal causes RED 负例。
- After this file: renewAtSkewAndSingleFlightByCompleteKey 的字段/入口或注册已落盘；与下一文件组合后本Step才能GREEN，不能把中间状态当完成。

#### File 2 — `MODIFY egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/tianquan/shoubing/starter/client/ServiceAuthorizationKey.java`

- Purpose: 扩充缓存与AuthorizedClient分区身份。
- Symbols: `issuer/credentialVersion fields`。
- Repository evidence: 当前文件存在：egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/tianquan/shoubing/starter/client/ServiceAuthorizationKey.java；目标源于主Spec §7–§16。
- Dependencies and consumers: 输入依赖 Step 11 与本Step此前文件；输出供 出站调用按issuer/client/凭据版/aud/tenant/scope隔离并临期重获，无client_credentials RT及后一Step使用；Java Bean/Mapper按模块现有配置发现，前端按现有auth client消费。
- Why now: 前一文件建立本文件需要的字段或调用契约； 扩充缓存与AuthorizedClient分区身份在此位置后才可接消费者。
- Contract/signature changes: issuer/credentialVersion fields；扩充缓存与AuthorizedClient分区身份。公开行为只取主Spec §9对应API或§10/11内部合同。
- Input/output and state mapping: platform has no tenant business claim; key tenant slot explicit。
- Error and edge behavior: no two issuers share client cache entry。
- Standards impact: MC-ARCH-001, MC-SCOPE-001, MC-TEST-001, MC-VALID-001；按已选传统分层，输入校验与权限先于状态读取，PO不出边界，相关日志脱敏。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 10, Rule 11；此文件直接按所列原始规则落实命名、验证、类型/注入或配置，其他规则由同Step关联文件负责。
- Implementation pseudocode:

```java
INPUT -> platform has no tenant business claim; key tenant slot explicit。
FLOW -> key=(issuer,registrationId,clientId,credentialVersion,audience,scopeContext,tenantId-or-PLATFORM,sortedScopes); principalName derived same tuple hash; equals/hash stable
ON INVALID/CONCURRENT/DEPENDENCY FAILURE -> no two issuers share client cache entry。
RESULT -> only the named symbol/contract in this File block becomes available; preserve tenant, expectedVersion, scope, UTC time and secret masking stated above.
```

- Verification contribution: 本Step `mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter -am -Dtest=IdpServiceOAuth2ClientTest -Dsurefire.failIfNoSpecifiedTests=false test`；此文件负责 issuer/credentialVersion fields 的正例与 no two issuers share client cache entry 负例。
- After this file: issuer/credentialVersion fields 的字段/入口或注册已落盘；与下一文件组合后本Step才能GREEN，不能把中间状态当完成。

#### File 3 — `MODIFY egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/tianquan/shoubing/starter/client/IdpServiceOAuth2Client.java`

- Purpose: 复用既有AuthorizedClientManager+singleFlight。
- Symbols: `authorize(IdpServiceTokenRequest)`。
- Repository evidence: 当前文件存在：egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/tianquan/shoubing/starter/client/IdpServiceOAuth2Client.java；目标源于主Spec §7–§16。
- Dependencies and consumers: 输入依赖 Step 11 与本Step此前文件；输出供 出站调用按issuer/client/凭据版/aud/tenant/scope隔离并临期重获，无client_credentials RT及后一Step使用；Java Bean/Mapper按模块现有配置发现，前端按现有auth client消费。
- Why now: 前一文件建立本文件需要的字段或调用契约； 复用既有AuthorizedClientManager+singleFlight在此位置后才可接消费者。
- Contract/signature changes: authorize(IdpServiceTokenRequest)；复用既有AuthorizedClientManager+singleFlight。公开行为只取主Spec §9对应API或§10/11内部合同。
- Input/output and state mapping: client_credentials new AT only; no raw secret in cache key logs。
- Error and edge behavior: same-key coalescing; different-key separated。
- Standards impact: MC-ARCH-001, MC-SCOPE-001, MC-TEST-001, MC-VALID-001；按已选传统分层，输入校验与权限先于状态读取，PO不出边界，相关日志脱敏。
- Literal rule enforcement: Rule 2, Rule 4, Rule 9, Rule 10, Rule 11；此文件直接按所列原始规则落实命名、验证、类型/注入或配置，其他规则由同Step关联文件负责。
- Implementation pseudocode:

```java
INPUT -> client_credentials new AT only; no raw secret in cache key logs。
FLOW -> @Slf4j named Bean config; cache max10000, expiry removal, renewalSkew60s, active key jitter; manager authorize on miss; stop 4xx/no exception caching; finally converter.clear; no refresh grant for SERVICE
ON INVALID/CONCURRENT/DEPENDENCY FAILURE -> same-key coalescing; different-key separated。
RESULT -> only the named symbol/contract in this File block becomes available; preserve tenant, expectedVersion, scope, UTC time and secret masking stated above.
```

- Verification contribution: 本Step `mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter -am -Dtest=IdpServiceOAuth2ClientTest -Dsurefire.failIfNoSpecifiedTests=false test`；此文件负责 authorize(IdpServiceTokenRequest) 的正例与 same-key coalescing; different-key separated 负例。
- After this file: authorize(IdpServiceTokenRequest) 的字段/入口或注册已落盘；与下一文件组合后本Step才能GREEN，不能把中间状态当完成。

#### File 4 — `CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/tianquan/shoubing/starter/client/ServiceTokenWebClientConfiguration.java`

- Purpose: 只给受信Provider装配Bearer filter。
- Symbols: `serviceTokenWebClient`。
- Repository evidence: 当前不存在；Spec §8/§11 明确目标，邻近包可验证；目标源于主Spec §7–§16。
- Dependencies and consumers: 输入依赖 Step 11 与本Step此前文件；输出供 出站调用按issuer/client/凭据版/aud/tenant/scope隔离并临期重获，无client_credentials RT及后一Step使用；Java Bean/Mapper按模块现有配置发现，前端按现有auth client消费。
- Why now: 前一文件建立本文件需要的字段或调用契约； 只给受信Provider装配Bearer filter在此位置后才可接消费者。
- Contract/signature changes: serviceTokenWebClient；只给受信Provider装配Bearer filter。公开行为只取主Spec §9对应API或§10/11内部合同。
- Input/output and state mapping: A→B token never forwarded as B→C SERVICE token。
- Error and edge behavior: 403/timeout no refresh loop。
- Standards impact: MC-ARCH-001, MC-SCOPE-001, MC-TEST-001, MC-VALID-001；按已选传统分层，输入校验与权限先于状态读取，PO不出边界，相关日志脱敏。
- Literal rule enforcement: Rule 2, Rule 4, Rule 7, Rule 9, Rule 11；此文件直接按所列原始规则落实命名、验证、类型/注入或配置，其他规则由同Step关联文件负责。
- Implementation pseudocode:

```java
INPUT -> A→B token never forwarded as B→C SERVICE token。
FLOW -> @Configuration; @Bean("serviceTokenWebClient") WebClient with OAuth2 filter; target scheme/host/port from directory, no cross-origin Authorization redirect; 401 GET refresh one; POST only with approved business idempotency
ON INVALID/CONCURRENT/DEPENDENCY FAILURE -> 403/timeout no refresh loop。
RESULT -> only the named symbol/contract in this File block becomes available; preserve tenant, expectedVersion, scope, UTC time and secret masking stated above.
```

- Verification contribution: 本Step `mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter -am -Dtest=IdpServiceOAuth2ClientTest -Dsurefire.failIfNoSpecifiedTests=false test`；此文件负责 serviceTokenWebClient 的正例与 403/timeout no refresh loop 负例。
- After this file: serviceTokenWebClient 的字段/入口或注册已落盘；与下一文件组合后本Step才能GREEN，不能把中间状态当完成。

- Validation working directory: `/Users/mario/SelfProject/Egon-COLA`。
- Verification command: `mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter -am -Dtest=IdpServiceOAuth2ClientTest -Dsurefire.failIfNoSpecifiedTests=false test`。
- Expected result: 先确认上述RED由缺失目标行为引起，GREEN时命名用例成功、模块编译成功、`git diff --check`零错误；不以不存在依赖/数据库/fixture而失败作为RED。
- Failure returns to: Step 11 的编译/Schema前置；若需改变外部语义、架构或DDL版本则回主Spec。
- Completion criteria: 出站调用按issuer/client/凭据版/aud/tenant/scope隔离并临期重获，无client_credentials RT；本Step的所有Manual Checks在实际执行时给出证据，未通过不能提交。
- Rollback: 提交前仅恢复本Step的精确路径；DDL已对真实数据运行后只forward-fix和新版本，不能编辑既有SQL/历史或恢复旧安全世代。
- Commit paths: `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/test/java/top/egon/cola/platform/tianquan/shoubing/starter/client/IdpServiceOAuth2ClientTest.java`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/tianquan/shoubing/starter/client/ServiceAuthorizationKey.java`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/tianquan/shoubing/starter/client/IdpServiceOAuth2Client.java`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/tianquan/shoubing/starter/client/ServiceTokenWebClientConfiguration.java`。
- Commit: `feat(shoubing): acquire scoped service tokens with webclient`。

### Step 13 — 建立SCG OAuth2登录和服务端会话

- Requirements: REQ-003, REQ-004, REQ-006, REQ-010, REQ-011, REQ-012
- Dependencies: Step 12
- Baseline state: 当前HEAD的对应源码/测试如每File的Repository evidence，先前Step已完成并有其focused GREEN；本Step尚不存在stateNonceTenantHeadersSession所列新行为。
- Observable outcome: 浏览器仅持HttpOnly opaque会话、SCG代码换token、玉衡路由原样转发Bearer。
- End state: 浏览器仅持HttpOnly opaque会话、SCG代码换token、玉衡路由原样转发Bearer；后续Step的Consumer尚未切流，不声明完整生产验收。
- Test-first gate: Required — 尚无SCG登录/CSRF/TokenRelay，WebTestClient RED；实现具名链后GREEN。
- Manual Checks: MC-ARCH-001, MC-REUSE-001, MC-SCOPE-001, MC-TEST-001, MC-VALID-001, MC-NAME-001, MC-MODEL-001, MC-CONVERT-001, MC-LOG-001, MC-BEAN-001, MC-JSON-001, MC-TIME-001
- Literal Rules: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11
- Ordered files:

#### File 1 — `CREATE egon-cola-xingyuan/egon-cola-yuheng/yuheng-edge-gateway/src/test/java/top/egon/cola/component/yuheng/edge/EdgeLoginContractTest.java`

- Purpose: RED新edge尚无oauth2Login/BFF。
- Symbols: `stateNonceTenantHeadersSession`。
- Repository evidence: 当前不存在；Spec §8/§11 明确目标，邻近包可验证；目标源于主Spec §7–§16。
- Dependencies and consumers: 输入依赖 Step 12 与本Step此前文件；输出供 浏览器仅持HttpOnly opaque会话、SCG代码换token、玉衡路由原样转发Bearer及后一Step使用；Java Bean/Mapper按模块现有配置发现，前端按现有auth client消费。
- Why now: 本Step的编译前置/RED契约先锁定； RED新edge尚无oauth2Login/BFF在此位置后才可接消费者。
- Contract/signature changes: stateNonceTenantHeadersSession；RED新edge尚无oauth2Login/BFF。公开行为只取主Spec §9对应API或§10/11内部合同。
- Input/output and state mapping: synthetic issuer & issuer-bound registration。
- Error and edge behavior: security chain lacks BFF behavior RED。
- Standards impact: MC-ARCH-001, MC-SCOPE-001, MC-TEST-001, MC-VALID-001；按已选传统分层，输入校验与权限先于状态读取，PO不出边界，相关日志脱敏。
- Literal rule enforcement: Rule 11, Rule 2, Rule 4；此文件直接按所列原始规则落实命名、验证、类型/注入或配置，其他规则由同Step关联文件负责。
- Implementation pseudocode:

```java
INPUT -> synthetic issuer & issuer-bound registration。
FLOW -> WebTestClient mock SAS authorize/token, hostile X-Tenant headers, wrong state/nonce/issuer, static routes to biz/mcp; assert no access_token in browser, no XHR302, 503 Redis down
ON INVALID/CONCURRENT/DEPENDENCY FAILURE -> security chain lacks BFF behavior RED。
RESULT -> only the named symbol/contract in this File block becomes available; preserve tenant, expectedVersion, scope, UTC time and secret masking stated above.
```

- Verification contribution: 本Step `mvn -o -pl egon-cola-xingyuan/egon-cola-yuheng/yuheng-edge-gateway -am -Dtest=EdgeLoginContractTest -Dsurefire.failIfNoSpecifiedTests=false test`；此文件负责 stateNonceTenantHeadersSession 的正例与 security chain lacks BFF behavior RED 负例。
- After this file: stateNonceTenantHeadersSession 的字段/入口或注册已落盘；与下一文件组合后本Step才能GREEN，不能把中间状态当完成。

#### File 2 — `CREATE egon-cola-xingyuan/egon-cola-yuheng/yuheng-edge-gateway/src/main/java/top/egon/cola/component/yuheng/edge/controller/domain/EdgeSessionResult.java`

- Purpose: 关闭 `EdgeSessionResult` 在主Spec中的字段/消费依赖。
- Symbols: `EdgeSessionResult`。
- Repository evidence: 当前不存在；主Spec §9.2 API-015–017 命名此对象/字段；现有相邻业务包提供放置证据。
- Dependencies and consumers: 本Step既有File块调用该类型；数据来源/消费者为主Spec §9.2 API-015–017，不产生新HTTP路由。
- Why now: 该类型按本Step的测试/编译依赖顺序落位，以免由实现者临时发明。
- Contract/signature changes: authenticated/tenantId/subject/displayName, no AT/RT; class Lombok/Builder, Jackson exact。
- Input/output and state mapping: 精确字段由主Spec §9.2 API-015–017 指定，realm从签名/注册得出，Instant为UTC，版本单调，秘密字段仅服务器内部。
- Error and edge behavior: null/未知字段/跨tenant/重复版本拒绝；不转成宽松缺省或吞异常。
- Standards impact: MC-ARCH-001, MC-NAME-001, MC-VALID-001, MC-MODEL-001, MC-CONVERT-001, MC-BEAN-001, MC-JSON-001, MC-TIME-001, MC-SCOPE-001, MC-TEST-001；行为Bean使用具名final Qualifier、@RequiredArgsConstructor和@Slf4j，载体遵循四注解与Builder。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 6, Rule 9, Rule 10, Rule 11；class按POJO语义命名，边界注解验证，跨层转换只用既有BaseConverter，Jackson和java.time字段明确。
- Implementation pseudocode:

```java
INPUT -> 仅接受主Spec §9.2 API-015–017 的可信issuer/realm/输入字段；调用前 @Valid 或 ValueObject不变量检查。
FLOW -> authenticated/tenantId/subject/displayName, no AT/RT; class Lombok/Builder, Jackson exact。
OUTPUT -> EdgeSessionResult 的Java方法、DTO字段或配置可被本Step既有调用者编译使用；不新增业务API。
FAIL -> 空值/跨tenant/未知schema/重复版本/外部依赖错误按本Step已列错误模型fail-closed，不能输出原secret/token。
```

- Verification contribution: Step 13 的focus测试对 `EdgeSessionResult` 的字段、装配、负例及不泄密断言。
- After this file: `EdgeSessionResult` 成为已声明的Step依赖，下一文件可按原合同引用；未完成整条行为前不宣称GREEN。

#### File 3 — `CREATE egon-cola-xingyuan/egon-cola-yuheng/yuheng-edge-gateway/src/main/java/top/egon/cola/component/yuheng/edge/controller/domain/EdgeCsrfResult.java`

- Purpose: 关闭 `EdgeCsrfResult` 在主Spec中的字段/消费依赖。
- Symbols: `EdgeCsrfResult`。
- Repository evidence: 当前不存在；主Spec §9.2 API-015–017 命名此对象/字段；现有相邻业务包提供放置证据。
- Dependencies and consumers: 本Step既有File块调用该类型；数据来源/消费者为主Spec §9.2 API-015–017，不产生新HTTP路由。
- Why now: 该类型按本Step的测试/编译依赖顺序落位，以免由实现者临时发明。
- Contract/signature changes: headerName=X-CSRF-TOKEN/token; sensitive toString exclusion, no-store。
- Input/output and state mapping: 精确字段由主Spec §9.2 API-015–017 指定，realm从签名/注册得出，Instant为UTC，版本单调，秘密字段仅服务器内部。
- Error and edge behavior: null/未知字段/跨tenant/重复版本拒绝；不转成宽松缺省或吞异常。
- Standards impact: MC-ARCH-001, MC-NAME-001, MC-VALID-001, MC-MODEL-001, MC-CONVERT-001, MC-BEAN-001, MC-JSON-001, MC-TIME-001, MC-SCOPE-001, MC-TEST-001；行为Bean使用具名final Qualifier、@RequiredArgsConstructor和@Slf4j，载体遵循四注解与Builder。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 6, Rule 9, Rule 10, Rule 11；class按POJO语义命名，边界注解验证，跨层转换只用既有BaseConverter，Jackson和java.time字段明确。
- Implementation pseudocode:

```java
INPUT -> 仅接受主Spec §9.2 API-015–017 的可信issuer/realm/输入字段；调用前 @Valid 或 ValueObject不变量检查。
FLOW -> headerName=X-CSRF-TOKEN/token; sensitive toString exclusion, no-store。
OUTPUT -> EdgeCsrfResult 的Java方法、DTO字段或配置可被本Step既有调用者编译使用；不新增业务API。
FAIL -> 空值/跨tenant/未知schema/重复版本/外部依赖错误按本Step已列错误模型fail-closed，不能输出原secret/token。
```

- Verification contribution: Step 13 的focus测试对 `EdgeCsrfResult` 的字段、装配、负例及不泄密断言。
- After this file: `EdgeCsrfResult` 成为已声明的Step依赖，下一文件可按原合同引用；未完成整条行为前不宣称GREEN。

#### File 4 — `CREATE egon-cola-xingyuan/egon-cola-yuheng/yuheng-edge-gateway/src/main/java/top/egon/cola/component/yuheng/edge/controller/domain/EdgeLogoutFailureResult.java`

- Purpose: 关闭 `EdgeLogoutFailureResult` 在主Spec中的字段/消费依赖。
- Symbols: `EdgeLogoutFailureResult`。
- Repository evidence: 当前不存在；主Spec §9.2 API-015–017 命名此对象/字段；现有相邻业务包提供放置证据。
- Dependencies and consumers: 本Step既有File块调用该类型；数据来源/消费者为主Spec §9.2 API-015–017，不产生新HTTP路由。
- Why now: 该类型按本Step的测试/编译依赖顺序落位，以免由实现者临时发明。
- Contract/signature changes: code/message/localLoggedOut/remoteState PENDING/UNKNOWN, only 503 JSON, 204 has no body。
- Input/output and state mapping: 精确字段由主Spec §9.2 API-015–017 指定，realm从签名/注册得出，Instant为UTC，版本单调，秘密字段仅服务器内部。
- Error and edge behavior: null/未知字段/跨tenant/重复版本拒绝；不转成宽松缺省或吞异常。
- Standards impact: MC-ARCH-001, MC-NAME-001, MC-VALID-001, MC-MODEL-001, MC-CONVERT-001, MC-BEAN-001, MC-JSON-001, MC-TIME-001, MC-SCOPE-001, MC-TEST-001；行为Bean使用具名final Qualifier、@RequiredArgsConstructor和@Slf4j，载体遵循四注解与Builder。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 6, Rule 9, Rule 10, Rule 11；class按POJO语义命名，边界注解验证，跨层转换只用既有BaseConverter，Jackson和java.time字段明确。
- Implementation pseudocode:

```java
INPUT -> 仅接受主Spec §9.2 API-015–017 的可信issuer/realm/输入字段；调用前 @Valid 或 ValueObject不变量检查。
FLOW -> code/message/localLoggedOut/remoteState PENDING/UNKNOWN, only 503 JSON, 204 has no body。
OUTPUT -> EdgeLogoutFailureResult 的Java方法、DTO字段或配置可被本Step既有调用者编译使用；不新增业务API。
FAIL -> 空值/跨tenant/未知schema/重复版本/外部依赖错误按本Step已列错误模型fail-closed，不能输出原secret/token。
```

- Verification contribution: Step 13 的focus测试对 `EdgeLogoutFailureResult` 的字段、装配、负例及不泄密断言。
- After this file: `EdgeLogoutFailureResult` 成为已声明的Step依赖，下一文件可按原合同引用；未完成整条行为前不宣称GREEN。

#### File 5 — `CREATE egon-cola-xingyuan/egon-cola-yuheng/yuheng-edge-gateway/src/main/java/top/egon/cola/component/yuheng/edge/config/EdgeOAuthProperties.java`

- Purpose: typed validated origin/upstream/return paths。
- Symbols: `edgeOAuthProperties`。
- Repository evidence: 当前不存在；Spec §8/§11 明确目标，邻近包可验证；目标源于主Spec §7–§16。
- Dependencies and consumers: 输入依赖 Step 12 与本Step此前文件；输出供 浏览器仅持HttpOnly opaque会话、SCG代码换token、玉衡路由原样转发Bearer及后一Step使用；Java Bean/Mapper按模块现有配置发现，前端按现有auth client消费。
- Why now: 前一文件建立本文件需要的字段或调用契约； typed validated origin/upstream/return paths在此位置后才可接消费者。
- Contract/signature changes: edgeOAuthProperties；typed validated origin/upstream/return paths。公开行为只取主Spec §9对应API或§10/11内部合同。
- Input/output and state mapping: base/local same key tree, no hardcoded prod host。
- Error and edge behavior: invalid TLS/origin refuses app startup。
- Standards impact: MC-ARCH-001, MC-SCOPE-001, MC-TEST-001, MC-VALID-001；按已选传统分层，输入校验与权限先于状态读取，PO不出边界，相关日志脱敏。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 7, Rule 11；此文件直接按所列原始规则落实命名、验证、类型/注入或配置，其他规则由同Step关联文件负责。
- Implementation pseudocode:

```java
INPUT -> base/local same key tree, no hardcoded prod host。
FLOW -> @ConfigurationProperties("egon.yuheng.edge") @Validated class Lombok; HTTPS URIs, distinct biz/mcp/AS, session absolute8h, exact return path allowlist; deny unknown key
ON INVALID/CONCURRENT/DEPENDENCY FAILURE -> invalid TLS/origin refuses app startup。
RESULT -> only the named symbol/contract in this File block becomes available; preserve tenant, expectedVersion, scope, UTC time and secret masking stated above.
```

- Verification contribution: 本Step `mvn -o -pl egon-cola-xingyuan/egon-cola-yuheng/yuheng-edge-gateway -am -Dtest=EdgeLoginContractTest -Dsurefire.failIfNoSpecifiedTests=false test`；此文件负责 edgeOAuthProperties 的正例与 invalid TLS/origin refuses app startup 负例。
- After this file: edgeOAuthProperties 的字段/入口或注册已落盘；与下一文件组合后本Step才能GREEN，不能把中间状态当完成。

#### File 6 — `CREATE egon-cola-xingyuan/egon-cola-yuheng/yuheng-edge-gateway/src/main/java/top/egon/cola/component/yuheng/edge/service/impl/EdgeAuthorizedClientServiceImpl.java`

- Purpose: 会话绑定issuer/tenant/client的AuthorizedClient。
- Symbols: `edgeAuthorizedClientService`。
- Repository evidence: 当前不存在；Spec §8/§11 明确目标，邻近包可验证；目标源于主Spec §7–§16。
- Dependencies and consumers: 输入依赖 Step 12 与本Step此前文件；输出供 浏览器仅持HttpOnly opaque会话、SCG代码换token、玉衡路由原样转发Bearer及后一Step使用；Java Bean/Mapper按模块现有配置发现，前端按现有auth client消费。
- Why now: 前一文件建立本文件需要的字段或调用契约； 会话绑定issuer/tenant/client的AuthorizedClient在此位置后才可接消费者。
- Contract/signature changes: edgeAuthorizedClientService；会话绑定issuer/tenant/client的AuthorizedClient。公开行为只取主Spec §9对应API或§10/11内部合同。
- Input/output and state mapping: Cookie only session ID; no default in-memory TokenRelay repository。
- Error and edge behavior: missing/conflicting realm→401; failed refresh no old RT reuse。
- Standards impact: MC-ARCH-001, MC-SCOPE-001, MC-TEST-001, MC-VALID-001；按已选传统分层，输入校验与权限先于状态读取，PO不出边界，相关日志脱敏。
- Literal rule enforcement: Rule 1, Rule 2, Rule 4, Rule 6, Rule 9, Rule 10, Rule 11；此文件直接按所列原始规则落实命名、验证、类型/注入或配置，其他规则由同Step关联文件负责。
- Implementation pseudocode:

```java
INPUT -> Cookie only session ID; no default in-memory TokenRelay repository。
FLOW -> @Service("edgeAuthorizedClientService") @Slf4j @RequiredArgsConstructor; final @Qualifier manager/sessionRepo/properties; save refresh token server-side encrypted Redis Session, CAS rotation, new tenant triggers auth, no browser token; 30m idle/8h absolute
ON INVALID/CONCURRENT/DEPENDENCY FAILURE -> missing/conflicting realm→401; failed refresh no old RT reuse。
RESULT -> only the named symbol/contract in this File block becomes available; preserve tenant, expectedVersion, scope, UTC time and secret masking stated above.
```

- Verification contribution: 本Step `mvn -o -pl egon-cola-xingyuan/egon-cola-yuheng/yuheng-edge-gateway -am -Dtest=EdgeLoginContractTest -Dsurefire.failIfNoSpecifiedTests=false test`；此文件负责 edgeAuthorizedClientService 的正例与 missing/conflicting realm→401; failed refresh no old RT reuse 负例。
- After this file: edgeAuthorizedClientService 的字段/入口或注册已落盘；与下一文件组合后本Step才能GREEN，不能把中间状态当完成。

#### File 7 — `CREATE egon-cola-xingyuan/egon-cola-yuheng/yuheng-edge-gateway/src/main/java/top/egon/cola/component/yuheng/edge/filter/TrustedIdentityHeaderFilter.java`

- Purpose: 清除伪造身份头仅写非权威追踪头。
- Symbols: `trustedIdentityHeaderFilter`。
- Repository evidence: 当前不存在；Spec §8/§11 明确目标，邻近包可验证；目标源于主Spec §7–§16。
- Dependencies and consumers: 输入依赖 Step 12 与本Step此前文件；输出供 浏览器仅持HttpOnly opaque会话、SCG代码换token、玉衡路由原样转发Bearer及后一Step使用；Java Bean/Mapper按模块现有配置发现，前端按现有auth client消费。
- Why now: 前一文件建立本文件需要的字段或调用契约； 清除伪造身份头仅写非权威追踪头在此位置后才可接消费者。
- Contract/signature changes: trustedIdentityHeaderFilter；清除伪造身份头仅写非权威追踪头。公开行为只取主Spec §9对应API或§10/11内部合同。
- Input/output and state mapping: Reactor Context only，no ThreadLocal across event-loop。
- Error and edge behavior: disagreeing header and token denied at resource server。
- Standards impact: MC-ARCH-001, MC-SCOPE-001, MC-TEST-001, MC-VALID-001；按已选传统分层，输入校验与权限先于状态读取，PO不出边界，相关日志脱敏。
- Literal rule enforcement: Rule 2, Rule 4, Rule 9, Rule 11；此文件直接按所列原始规则落实命名、验证、类型/注入或配置，其他规则由同Step关联文件负责。
- Implementation pseudocode:

```java
INPUT -> Reactor Context only，no ThreadLocal across event-loop。
FLOW -> GlobalFilter strips all inbound identity headers; after verified JwtPrincipal writes X-Egon-Tenant-Id/User-Id/Scopes bounded CRLF-free; retain original Bearer; downstream still checks token
ON INVALID/CONCURRENT/DEPENDENCY FAILURE -> disagreeing header and token denied at resource server。
RESULT -> only the named symbol/contract in this File block becomes available; preserve tenant, expectedVersion, scope, UTC time and secret masking stated above.
```

- Verification contribution: 本Step `mvn -o -pl egon-cola-xingyuan/egon-cola-yuheng/yuheng-edge-gateway -am -Dtest=EdgeLoginContractTest -Dsurefire.failIfNoSpecifiedTests=false test`；此文件负责 trustedIdentityHeaderFilter 的正例与 disagreeing header and token denied at resource server 负例。
- After this file: trustedIdentityHeaderFilter 的字段/入口或注册已落盘；与下一文件组合后本Step才能GREEN，不能把中间状态当完成。

#### File 8 — `CREATE egon-cola-xingyuan/egon-cola-yuheng/yuheng-edge-gateway/src/main/java/top/egon/cola/component/yuheng/edge/controller/EdgeSessionController.java`

- Purpose: BFF会话REST协议。
- Symbols: `bff/session,csrf,logout`。
- Repository evidence: 当前不存在；Spec §8/§11 明确目标，邻近包可验证；目标源于主Spec §7–§16。
- Dependencies and consumers: 输入依赖 Step 12 与本Step此前文件；输出供 浏览器仅持HttpOnly opaque会话、SCG代码换token、玉衡路由原样转发Bearer及后一Step使用；Java Bean/Mapper按模块现有配置发现，前端按现有auth client消费。
- Why now: 前一文件建立本文件需要的字段或调用契约； BFF会话REST协议在此位置后才可接消费者。
- Contract/signature changes: bff/session,csrf,logout；BFF会话REST协议。公开行为只取主Spec §9对应API或§10/11内部合同。
- Input/output and state mapping: auth/tenant from session-bound AuthorizedClient。
- Error and edge behavior: no session401; remote fail localLoggedOut=true。
- Standards impact: MC-ARCH-001, MC-SCOPE-001, MC-TEST-001, MC-VALID-001；按已选传统分层，输入校验与权限先于状态读取，PO不出边界，相关日志脱敏。
- Literal rule enforcement: Rule 1, Rule 2, Rule 4, Rule 6, Rule 9, Rule 11；此文件直接按所列原始规则落实命名、验证、类型/注入或配置，其他规则由同Step关联文件负责。
- Implementation pseudocode:

```java
INPUT -> auth/tenant from session-bound AuthorizedClient。
FLOW -> @RestController("edgeSessionController") @Slf4j @RequiredArgsConstructor; GET session returns EdgeSessionResult no token; GET csrf no-store; POST logout CSRF, invalidate local session, call revoke family; 204 only after ACK, 503 pending on remote fail
ON INVALID/CONCURRENT/DEPENDENCY FAILURE -> no session401; remote fail localLoggedOut=true。
RESULT -> only the named symbol/contract in this File block becomes available; preserve tenant, expectedVersion, scope, UTC time and secret masking stated above.
```

- Verification contribution: 本Step `mvn -o -pl egon-cola-xingyuan/egon-cola-yuheng/yuheng-edge-gateway -am -Dtest=EdgeLoginContractTest -Dsurefire.failIfNoSpecifiedTests=false test`；此文件负责 bff/session,csrf,logout 的正例与 no session401; remote fail localLoggedOut=true 负例。
- After this file: bff/session,csrf,logout 的字段/入口或注册已落盘；与下一文件组合后本Step才能GREEN，不能把中间状态当完成。

#### File 9 — `CREATE egon-cola-xingyuan/egon-cola-yuheng/yuheng-edge-gateway/src/main/java/top/egon/cola/component/yuheng/edge/config/EdgeSecurityConfiguration.java`

- Purpose: code+S256+strict BFF session。
- Symbols: `SecurityWebFilterChain/oauth2Login routes`。
- Repository evidence: 当前不存在；Spec §8/§11 明确目标，邻近包可验证；目标源于主Spec §7–§16。
- Dependencies and consumers: 输入依赖 Step 12 与本Step此前文件；输出供 浏览器仅持HttpOnly opaque会话、SCG代码换token、玉衡路由原样转发Bearer及后一Step使用；Java Bean/Mapper按模块现有配置发现，前端按现有auth client消费。
- Why now: 前一文件建立本文件需要的字段或调用契约； code+S256+strict BFF session在此位置后才可接消费者。
- Contract/signature changes: SecurityWebFilterChain/oauth2Login routes；code+S256+strict BFF session。公开行为只取主Spec §9对应API或§10/11内部合同。
- Input/output and state mapping: first-party BFF vs third-party Bearer separate matcher。
- Error and edge behavior: unknown registration/redirect open redirect denied。
- Standards impact: MC-ARCH-001, MC-SCOPE-001, MC-TEST-001, MC-VALID-001；按已选传统分层，输入校验与权限先于状态读取，PO不出边界，相关日志脱敏。
- Literal rule enforcement: Rule 2, Rule 4, Rule 7, Rule 9, Rule 11；此文件直接按所列原始规则落实命名、验证、类型/注入或配置，其他规则由同Step关联文件负责。
- Implementation pseudocode:

```java
INPUT -> first-party BFF vs third-party Bearer separate matcher。
FLOW -> @Configuration; @Bean named reactive chains; oauth2Login registration exact issuer; state/nonce/verifier session; Spring Session Redis, CSRF for mutations; TokenRelay to biz/mcp only; static AS route; API 401 JSON not HTML302
ON INVALID/CONCURRENT/DEPENDENCY FAILURE -> unknown registration/redirect open redirect denied。
RESULT -> only the named symbol/contract in this File block becomes available; preserve tenant, expectedVersion, scope, UTC time and secret masking stated above.
```

- Verification contribution: 本Step `mvn -o -pl egon-cola-xingyuan/egon-cola-yuheng/yuheng-edge-gateway -am -Dtest=EdgeLoginContractTest -Dsurefire.failIfNoSpecifiedTests=false test`；此文件负责 SecurityWebFilterChain/oauth2Login routes 的正例与 unknown registration/redirect open redirect denied 负例。
- After this file: SecurityWebFilterChain/oauth2Login routes 的字段/入口或注册已落盘；与下一文件组合后本Step才能GREEN，不能把中间状态当完成。

#### File 10 — `CREATE egon-cola-xingyuan/egon-cola-yuheng/yuheng-edge-gateway/src/main/resources/application.yml`

- Purpose: 全配置base结构。
- Symbols: `session/oauth/upstream/TLS keys`。
- Repository evidence: 当前不存在；Spec §8/§11 明确目标，邻近包可验证；目标源于主Spec §7–§16。
- Dependencies and consumers: 输入依赖 Step 12 与本Step此前文件；输出供 浏览器仅持HttpOnly opaque会话、SCG代码换token、玉衡路由原样转发Bearer及后一Step使用；Java Bean/Mapper按模块现有配置发现，前端按现有auth client消费。
- Why now: 前一文件建立本文件需要的字段或调用契约； 全配置base结构在此位置后才可接消费者。
- Contract/signature changes: session/oauth/upstream/TLS keys；全配置base结构。公开行为只取主Spec §9对应API或§10/11内部合同。
- Input/output and state mapping: no plaintext secret; Spring OAuth provider issuer at bootstrap。
- Error and edge behavior: unresolved placeholders fail validated properties。
- Standards impact: MC-SCOPE-001, MC-TEST-001；按已选传统分层，输入校验与权限先于状态读取，PO不出边界，相关日志脱敏。
- Literal rule enforcement: Rule 7, Rule 11；此文件直接按所列原始规则落实命名、验证、类型/注入或配置，其他规则由同Step关联文件负责。
- Implementation pseudocode:

```yaml
INPUT -> no plaintext secret; Spring OAuth provider issuer at bootstrap。
FLOW -> spring.session.redis.namespace=egon:bff:session:v2; edge security config from §15.5; HTTPS, no default unrestricted redirect
ON INVALID/CONCURRENT/DEPENDENCY FAILURE -> unresolved placeholders fail validated properties。
RESULT -> only the named symbol/contract in this File block becomes available; preserve tenant, expectedVersion, scope, UTC time and secret masking stated above.
```

- Verification contribution: 本Step `mvn -o -pl egon-cola-xingyuan/egon-cola-yuheng/yuheng-edge-gateway -am -Dtest=EdgeLoginContractTest -Dsurefire.failIfNoSpecifiedTests=false test`；此文件负责 session/oauth/upstream/TLS keys 的正例与 unresolved placeholders fail validated properties 负例。
- After this file: session/oauth/upstream/TLS keys 的字段/入口或注册已落盘；与下一文件组合后本Step才能GREEN，不能把中间状态当完成。

#### File 11 — `CREATE egon-cola-xingyuan/egon-cola-yuheng/yuheng-edge-gateway/src/main/resources/application-local.yml`

- Purpose: 本地HTTPS测试值。
- Symbols: `identical profile key set`。
- Repository evidence: 当前不存在；Spec §8/§11 明确目标，邻近包可验证；目标源于主Spec §7–§16。
- Dependencies and consumers: 输入依赖 Step 12 与本Step此前文件；输出供 浏览器仅持HttpOnly opaque会话、SCG代码换token、玉衡路由原样转发Bearer及后一Step使用；Java Bean/Mapper按模块现有配置发现，前端按现有auth client消费。
- Why now: 前一文件建立本文件需要的字段或调用契约； 本地HTTPS测试值在此位置后才可接消费者。
- Contract/signature changes: identical profile key set；本地HTTPS测试值。公开行为只取主Spec §9对应API或§10/11内部合同。
- Input/output and state mapping: different values allowed, missing keys fail static key parity。
- Error and edge behavior: cannot quietly enable plaintext for local。
- Standards impact: MC-SCOPE-001, MC-TEST-001；按已选传统分层，输入校验与权限先于状态读取，PO不出边界，相关日志脱敏。
- Literal rule enforcement: Rule 7, Rule 11；此文件直接按所列原始规则落实命名、验证、类型/注入或配置，其他规则由同Step关联文件负责。
- Implementation pseudocode:

```yaml
INPUT -> different values allowed, missing keys fail static key parity。
FLOW -> same YAML leaf structure as application.yml; local certificate mounted and Redis test namespace
ON INVALID/CONCURRENT/DEPENDENCY FAILURE -> cannot quietly enable plaintext for local。
RESULT -> only the named symbol/contract in this File block becomes available; preserve tenant, expectedVersion, scope, UTC time and secret masking stated above.
```

- Verification contribution: 本Step `mvn -o -pl egon-cola-xingyuan/egon-cola-yuheng/yuheng-edge-gateway -am -Dtest=EdgeLoginContractTest -Dsurefire.failIfNoSpecifiedTests=false test`；此文件负责 identical profile key set 的正例与 cannot quietly enable plaintext for local 负例。
- After this file: identical profile key set 的字段/入口或注册已落盘；与下一文件组合后本Step才能GREEN，不能把中间状态当完成。

- Validation working directory: `/Users/mario/SelfProject/Egon-COLA`。
- Verification command: `mvn -o -pl egon-cola-xingyuan/egon-cola-yuheng/yuheng-edge-gateway -am -Dtest=EdgeLoginContractTest -Dsurefire.failIfNoSpecifiedTests=false test`。
- Expected result: 先确认上述RED由缺失目标行为引起，GREEN时命名用例成功、模块编译成功、`git diff --check`零错误；不以不存在依赖/数据库/fixture而失败作为RED。
- Failure returns to: Step 12 的编译/Schema前置；若需改变外部语义、架构或DDL版本则回主Spec。
- Completion criteria: 浏览器仅持HttpOnly opaque会话、SCG代码换token、玉衡路由原样转发Bearer；本Step的所有Manual Checks在实际执行时给出证据，未通过不能提交。
- Rollback: 提交前仅恢复本Step的精确路径；DDL已对真实数据运行后只forward-fix和新版本，不能编辑既有SQL/历史或恢复旧安全世代。
- Commit paths: `egon-cola-xingyuan/egon-cola-yuheng/yuheng-edge-gateway/src/main/java/top/egon/cola/component/yuheng/edge/controller/domain/EdgeLogoutFailureResult.java`, `egon-cola-xingyuan/egon-cola-yuheng/yuheng-edge-gateway/src/main/java/top/egon/cola/component/yuheng/edge/controller/domain/EdgeCsrfResult.java`, `egon-cola-xingyuan/egon-cola-yuheng/yuheng-edge-gateway/src/main/java/top/egon/cola/component/yuheng/edge/controller/domain/EdgeSessionResult.java`, `egon-cola-xingyuan/egon-cola-yuheng/yuheng-edge-gateway/src/test/java/top/egon/cola/component/yuheng/edge/EdgeLoginContractTest.java`, `egon-cola-xingyuan/egon-cola-yuheng/yuheng-edge-gateway/src/main/java/top/egon/cola/component/yuheng/edge/config/EdgeOAuthProperties.java`, `egon-cola-xingyuan/egon-cola-yuheng/yuheng-edge-gateway/src/main/java/top/egon/cola/component/yuheng/edge/service/impl/EdgeAuthorizedClientServiceImpl.java`, `egon-cola-xingyuan/egon-cola-yuheng/yuheng-edge-gateway/src/main/java/top/egon/cola/component/yuheng/edge/filter/TrustedIdentityHeaderFilter.java`, `egon-cola-xingyuan/egon-cola-yuheng/yuheng-edge-gateway/src/main/java/top/egon/cola/component/yuheng/edge/controller/EdgeSessionController.java`, `egon-cola-xingyuan/egon-cola-yuheng/yuheng-edge-gateway/src/main/java/top/egon/cola/component/yuheng/edge/config/EdgeSecurityConfiguration.java`, `egon-cola-xingyuan/egon-cola-yuheng/yuheng-edge-gateway/src/main/resources/application.yml`, `egon-cola-xingyuan/egon-cola-yuheng/yuheng-edge-gateway/src/main/resources/application-local.yml`。
- Commit: `feat(yuheng): add oauth2 bff edge`。

### Step 14 — 保留玉衡、鉴神与天枢准入链

- Requirements: REQ-001, REQ-005, REQ-006, REQ-007, REQ-010, REQ-016
- Dependencies: Step 13
- Baseline state: 当前HEAD的对应源码/测试如每File的Repository evidence，先前Step已完成并有其focused GREEN；本Step尚不存在newBearerNoCookieRecovery所列新行为。
- Observable outcome: 平台控制SERVICE token与USER授权在现有路由/鉴神/天枢链正确准入。
- End state: 平台控制SERVICE token与USER授权在现有路由/鉴神/天枢链正确准入；后续Step的Consumer尚未切流，不声明完整生产验收。
- Test-first gate: Required — old cookie/provider/PLATFORM acceptance routes fail new fixturesRED，再接入新principal和范围校验GREEN。
- Manual Checks: MC-ARCH-001, MC-REUSE-001, MC-SCOPE-001, MC-TEST-001, MC-VALID-001, MC-NAME-001, MC-MODEL-001, MC-CONVERT-001, MC-LOG-001, MC-BEAN-001, MC-JSON-001, MC-TIME-001
- Literal Rules: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11
- Ordered files:

#### File 1 — `MODIFY egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-gateway-adapter/src/test/java/top/egon/cola/platform/tianquan/shoubing/yuheng/security/IdpGatewaySecurityProviderTest.java`

- Purpose: RED旧网关仍自动刷新USER Cookie。
- Symbols: `newBearerNoCookieRecovery`。
- Repository evidence: 当前文件存在：egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-gateway-adapter/src/test/java/top/egon/cola/platform/tianquan/shoubing/yuheng/security/IdpGatewaySecurityProviderTest.java；目标源于主Spec §7–§16。
- Dependencies and consumers: 输入依赖 Step 13 与本Step此前文件；输出供 平台控制SERVICE token与USER授权在现有路由/鉴神/天枢链正确准入及后一Step使用；Java Bean/Mapper按模块现有配置发现，前端按现有auth client消费。
- Why now: 本Step的编译前置/RED契约先锁定； RED旧网关仍自动刷新USER Cookie在此位置后才可接消费者。
- Contract/signature changes: newBearerNoCookieRecovery；RED旧网关仍自动刷新USER Cookie。公开行为只取主Spec §9对应API或§10/11内部合同。
- Input/output and state mapping: existing GatewayFailClosedSecurityMatrix fixtures。
- Error and edge behavior: old cookie branch causes RED。
- Standards impact: MC-ARCH-001, MC-SCOPE-001, MC-TEST-001, MC-VALID-001；按已选传统分层，输入校验与权限先于状态读取，PO不出边界，相关日志脱敏。
- Literal rule enforcement: Rule 11, Rule 2, Rule 4；此文件直接按所列原始规则落实命名、验证、类型/注入或配置，其他规则由同Step关联文件负责。
- Implementation pseudocode:

```java
INPUT -> existing GatewayFailClosedSecurityMatrix fixtures。
FLOW -> supply new USER/SERVICE tenant JWT, platform token, hostile identity headers, no RT cookie; assert direct biz/mcp bearer, platform denied business, stable RBAC role/policy
ON INVALID/CONCURRENT/DEPENDENCY FAILURE -> old cookie branch causes RED。
RESULT -> only the named symbol/contract in this File block becomes available; preserve tenant, expectedVersion, scope, UTC time and secret masking stated above.
```

- Verification contribution: 本Step `mvn -o -pl egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin,egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-starter,egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-gateway-adapter -am -Dtest=IdpGatewaySecurityProviderTest,IdpJwtDdcRegistrationCredentialVerifierTest -Dsurefire.failIfNoSpecifiedTests=false test`；此文件负责 newBearerNoCookieRecovery 的正例与 old cookie branch causes RED 负例。
- After this file: newBearerNoCookieRecovery 的字段/入口或注册已落盘；与下一文件组合后本Step才能GREEN，不能把中间状态当完成。

#### File 2 — `MODIFY egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-gateway-adapter/src/main/java/top/egon/cola/platform/tianquan/shoubing/yuheng/security/IdpIdentityAuthenticationProvider.java`

- Purpose: 玉衡身份Provider使用Starter多issuer验证。
- Symbols: `Bearer-only identity`。
- Repository evidence: 当前文件存在：egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-gateway-adapter/src/main/java/top/egon/cola/platform/tianquan/shoubing/yuheng/security/IdpIdentityAuthenticationProvider.java；目标源于主Spec §7–§16。
- Dependencies and consumers: 输入依赖 Step 13 与本Step此前文件；输出供 平台控制SERVICE token与USER授权在现有路由/鉴神/天枢链正确准入及后一Step使用；Java Bean/Mapper按模块现有配置发现，前端按现有auth client消费。
- Why now: 前一文件建立本文件需要的字段或调用契约； 玉衡身份Provider使用Starter多issuer验证在此位置后才可接消费者。
- Contract/signature changes: Bearer-only identity；玉衡身份Provider使用Starter多issuer验证。公开行为只取主Spec §9对应API或§10/11内部合同。
- Input/output and state mapping: biz/app/env from published target rule。
- Error and edge behavior: missingBearer→401, revoked→401, Redis down→503。
- Standards impact: MC-ARCH-001, MC-SCOPE-001, MC-TEST-001, MC-VALID-001；按已选传统分层，输入校验与权限先于状态读取，PO不出边界，相关日志脱敏。
- Literal rule enforcement: Rule 2, Rule 4, Rule 9, Rule 11；此文件直接按所列原始规则落实命名、验证、类型/注入或配置，其他规则由同Step关联文件负责。
- Implementation pseudocode:

```java
INPUT -> biz/app/env from published target rule。
FLOW -> read Authorization Bearer; call IdpGatewayJwtVerifier/IdpJwtVerifier; map verified principal to current GatewayAuthContext; scrub untrusted headers; no username/tenant from request
ON INVALID/CONCURRENT/DEPENDENCY FAILURE -> missingBearer→401, revoked→401, Redis down→503。
RESULT -> only the named symbol/contract in this File block becomes available; preserve tenant, expectedVersion, scope, UTC time and secret masking stated above.
```

- Verification contribution: 本Step `mvn -o -pl egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin,egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-starter,egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-gateway-adapter -am -Dtest=IdpGatewaySecurityProviderTest,IdpJwtDdcRegistrationCredentialVerifierTest -Dsurefire.failIfNoSpecifiedTests=false test`；此文件负责 Bearer-only identity 的正例与 missingBearer→401, revoked→401, Redis down→503 负例。
- After this file: Bearer-only identity 的字段/入口或注册已落盘；与下一文件组合后本Step才能GREEN，不能把中间状态当完成。

#### File 3 — `MODIFY egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-gateway-adapter/src/main/java/top/egon/cola/platform/tianquan/shoubing/yuheng/security/IdpUserCredentialRecoveryProvider.java`

- Purpose: 新入口交SCG。
- Symbols: `remove USER Cookie RT recovery`。
- Repository evidence: 当前文件存在：egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-gateway-adapter/src/main/java/top/egon/cola/platform/tianquan/shoubing/yuheng/security/IdpUserCredentialRecoveryProvider.java；目标源于主Spec §7–§16。
- Dependencies and consumers: 输入依赖 Step 13 与本Step此前文件；输出供 平台控制SERVICE token与USER授权在现有路由/鉴神/天枢链正确准入及后一Step使用；Java Bean/Mapper按模块现有配置发现，前端按现有auth client消费。
- Why now: 前一文件建立本文件需要的字段或调用契约； 新入口交SCG在此位置后才可接消费者。
- Contract/signature changes: remove USER Cookie RT recovery；新入口交SCG。公开行为只取主Spec §9对应API或§10/11内部合同。
- Input/output and state mapping: new access token comes from SCG Bearer。
- Error and edge behavior: expired token returns401, no attempted old refresh。
- Standards impact: MC-ARCH-001, MC-SCOPE-001, MC-TEST-001, MC-VALID-001；按已选传统分层，输入校验与权限先于状态读取，PO不出边界，相关日志脱敏。
- Literal rule enforcement: Rule 2, Rule 4, Rule 9, Rule 11；此文件直接按所列原始规则落实命名、验证、类型/注入或配置，其他规则由同Step关联文件负责。
- Implementation pseudocode:

```java
INPUT -> new access token comes from SCG Bearer。
FLOW -> disable old cookie refresh on new paths; legacy isolated route only until Step17; keep no 302 for XHR/MCP
ON INVALID/CONCURRENT/DEPENDENCY FAILURE -> expired token returns401, no attempted old refresh。
RESULT -> only the named symbol/contract in this File block becomes available; preserve tenant, expectedVersion, scope, UTC time and secret masking stated above.
```

- Verification contribution: 本Step `mvn -o -pl egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin,egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-starter,egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-gateway-adapter -am -Dtest=IdpGatewaySecurityProviderTest,IdpJwtDdcRegistrationCredentialVerifierTest -Dsurefire.failIfNoSpecifiedTests=false test`；此文件负责 remove USER Cookie RT recovery 的正例与 expired token returns401, no attempted old refresh 负例。
- After this file: remove USER Cookie RT recovery 的字段/入口或注册已落盘；与下一文件组合后本Step才能GREEN，不能把中间状态当完成。

#### File 4 — `MODIFY egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-starter/src/main/java/top/egon/cola/platform/tianquan/jianshen/starter/security/Rbac3BearerAuthenticationFilter.java`

- Purpose: 已验JWT仍进入鉴神UserDetails/snapshot。
- Symbols: `IdentityPrincipal bridge`。
- Repository evidence: 当前文件存在：egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-starter/src/main/java/top/egon/cola/platform/tianquan/jianshen/starter/security/Rbac3BearerAuthenticationFilter.java；目标源于主Spec §7–§16。
- Dependencies and consumers: 输入依赖 Step 13 与本Step此前文件；输出供 平台控制SERVICE token与USER授权在现有路由/鉴神/天枢链正确准入及后一Step使用；Java Bean/Mapper按模块现有配置发现，前端按现有auth client消费。
- Why now: 前一文件建立本文件需要的字段或调用契约； 已验JWT仍进入鉴神UserDetails/snapshot在此位置后才可接消费者。
- Contract/signature changes: IdentityPrincipal bridge；已验JWT仍进入鉴神UserDetails/snapshot。公开行为只取主Spec §9对应API或§10/11内部合同。
- Input/output and state mapping: OAuth scope and RBAC both required。
- Error and edge behavior: wrong tenant/permission403, unavailable503。
- Standards impact: MC-ARCH-001, MC-SCOPE-001, MC-TEST-001, MC-VALID-001；按已选传统分层，输入校验与权限先于状态读取，PO不出边界，相关日志脱敏。
- Literal rule enforcement: Rule 2, Rule 4, Rule 9, Rule 11；此文件直接按所列原始规则落实命名、验证、类型/注入或配置，其他规则由同Step关联文件负责。
- Implementation pseudocode:

```java
INPUT -> OAuth scope and RBAC both required。
FLOW -> accept IdpAuthenticationToken built by VerifiedPrincipalConverter; lookup authorization snapshot by tenant+subject+app; field/data/SoD/Fence preserved; finally SecurityContextHolder.clearContext
ON INVALID/CONCURRENT/DEPENDENCY FAILURE -> wrong tenant/permission403, unavailable503。
RESULT -> only the named symbol/contract in this File block becomes available; preserve tenant, expectedVersion, scope, UTC time and secret masking stated above.
```

- Verification contribution: 本Step `mvn -o -pl egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin,egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-starter,egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-gateway-adapter -am -Dtest=IdpGatewaySecurityProviderTest,IdpJwtDdcRegistrationCredentialVerifierTest -Dsurefire.failIfNoSpecifiedTests=false test`；此文件负责 IdentityPrincipal bridge 的正例与 wrong tenant/permission403, unavailable503 负例。
- After this file: IdentityPrincipal bridge 的字段/入口或注册已落盘；与下一文件组合后本Step才能GREEN，不能把中间状态当完成。

#### File 5 — `MODIFY egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin/src/main/java/top/egon/cola/platform/tianquan/jianshen/admin/shared/tenant/controller/filter/TenantContextFilter.java`

- Purpose: 配合Starter一次可信上下文绑定。
- Symbols: `finally clear or restore MDC`。
- Repository evidence: 当前文件存在：egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin/src/main/java/top/egon/cola/platform/tianquan/jianshen/admin/shared/tenant/controller/filter/TenantContextFilter.java；目标源于主Spec §7–§16。
- Dependencies and consumers: 输入依赖 Step 13 与本Step此前文件；输出供 平台控制SERVICE token与USER授权在现有路由/鉴神/天枢链正确准入及后一Step使用；Java Bean/Mapper按模块现有配置发现，前端按现有auth client消费。
- Why now: 前一文件建立本文件需要的字段或调用契约； 配合Starter一次可信上下文绑定在此位置后才可接消费者。
- Contract/signature changes: finally clear or restore MDC；配合Starter一次可信上下文绑定。公开行为只取主Spec §9对应API或§10/11内部合同。
- Input/output and state mapping: Long tenant parsed from verified token, not headers。
- Error and edge behavior: pooled thread no stale tenant。
- Standards impact: MC-ARCH-001, MC-SCOPE-001, MC-TEST-001, MC-VALID-001；按已选传统分层，输入校验与权限先于状态读取，PO不出边界，相关日志脱敏。
- Literal rule enforcement: Rule 2, Rule 4, Rule 9, Rule 11；此文件直接按所列原始规则落实命名、验证、类型/注入或配置，其他规则由同Step关联文件负责。
- Implementation pseudocode:

```java
INPUT -> Long tenant parsed from verified token, not headers。
FLOW -> if verified bridge already bound, compare request.attribute, TenantContext and MDC; no duplicate override; try chain finally remove/reset outer MDC and TenantContext
ON INVALID/CONCURRENT/DEPENDENCY FAILURE -> pooled thread no stale tenant。
RESULT -> only the named symbol/contract in this File block becomes available; preserve tenant, expectedVersion, scope, UTC time and secret masking stated above.
```

- Verification contribution: 本Step `mvn -o -pl egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin,egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-starter,egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-gateway-adapter -am -Dtest=IdpGatewaySecurityProviderTest,IdpJwtDdcRegistrationCredentialVerifierTest -Dsurefire.failIfNoSpecifiedTests=false test`；此文件负责 finally clear or restore MDC 的正例与 pooled thread no stale tenant 负例。
- After this file: finally clear or restore MDC 的字段/入口或注册已落盘；与下一文件组合后本Step才能GREEN，不能把中间状态当完成。

#### File 6 — `MODIFY egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/java/top/egon/cola/component/tianshu/admin/security/registration/IdpJwtDdcRegistrationCredentialVerifier.java`

- Purpose: 天枢注册维持独立platform token。
- Symbols: `PLATFORM issuer resource source guard`。
- Repository evidence: 当前文件存在：egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/java/top/egon/cola/component/tianshu/admin/security/registration/IdpJwtDdcRegistrationCredentialVerifier.java；目标源于主Spec §7–§16。
- Dependencies and consumers: 输入依赖 Step 13 与本Step此前文件；输出供 平台控制SERVICE token与USER授权在现有路由/鉴神/天枢链正确准入及后一Step使用；Java Bean/Mapper按模块现有配置发现，前端按现有auth client消费。
- Why now: 前一文件建立本文件需要的字段或调用契约； 天枢注册维持独立platform token在此位置后才可接消费者。
- Contract/signature changes: PLATFORM issuer resource source guard；天枢注册维持独立platform token。公开行为只取主Spec §9对应API或§10/11内部合同。
- Input/output and state mapping: no Admission Ticket, no business tenant default。
- Error and edge behavior: resource disable/version mismatch denies。
- Standards impact: MC-ARCH-001, MC-SCOPE-001, MC-TEST-001, MC-VALID-001；按已选传统分层，输入校验与权限先于状态读取，PO不出边界，相关日志脱敏。
- Literal rule enforcement: Rule 2, Rule 4, Rule 9, Rule 11；此文件直接按所列原始规则落实命名、验证、类型/注入或配置，其他规则由同Step关联文件负责。
- Implementation pseudocode:

```java
INPUT -> no Admission Ticket, no business tenant default。
FLOW -> verify issuer=/platform, scope tianshu:registration:write, aud Tianshu resource, source biz/app/env matches submitted registration; reject business tenant token
ON INVALID/CONCURRENT/DEPENDENCY FAILURE -> resource disable/version mismatch denies。
RESULT -> only the named symbol/contract in this File block becomes available; preserve tenant, expectedVersion, scope, UTC time and secret masking stated above.
```

- Verification contribution: 本Step `mvn -o -pl egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin,egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-starter,egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-gateway-adapter -am -Dtest=IdpGatewaySecurityProviderTest,IdpJwtDdcRegistrationCredentialVerifierTest -Dsurefire.failIfNoSpecifiedTests=false test`；此文件负责 PLATFORM issuer resource source guard 的正例与 resource disable/version mismatch denies 负例。
- After this file: PLATFORM issuer resource source guard 的字段/入口或注册已落盘；与下一文件组合后本Step才能GREEN，不能把中间状态当完成。

- Validation working directory: `/Users/mario/SelfProject/Egon-COLA`。
- Verification command: `mvn -o -pl egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin,egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-starter,egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-gateway-adapter -am -Dtest=IdpGatewaySecurityProviderTest,IdpJwtDdcRegistrationCredentialVerifierTest -Dsurefire.failIfNoSpecifiedTests=false test`。
- Expected result: 先确认上述RED由缺失目标行为引起，GREEN时命名用例成功、模块编译成功、`git diff --check`零错误；不以不存在依赖/数据库/fixture而失败作为RED。
- Failure returns to: Step 13 的编译/Schema前置；若需改变外部语义、架构或DDL版本则回主Spec。
- Completion criteria: 平台控制SERVICE token与USER授权在现有路由/鉴神/天枢链正确准入；本Step的所有Manual Checks在实际执行时给出证据，未通过不能提交。
- Rollback: 提交前仅恢复本Step的精确路径；DDL已对真实数据运行后只forward-fix和新版本，不能编辑既有SQL/历史或恢复旧安全世代。
- Commit paths: `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-gateway-adapter/src/test/java/top/egon/cola/platform/tianquan/shoubing/yuheng/security/IdpGatewaySecurityProviderTest.java`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-gateway-adapter/src/main/java/top/egon/cola/platform/tianquan/shoubing/yuheng/security/IdpIdentityAuthenticationProvider.java`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-gateway-adapter/src/main/java/top/egon/cola/platform/tianquan/shoubing/yuheng/security/IdpUserCredentialRecoveryProvider.java`, `egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-starter/src/main/java/top/egon/cola/platform/tianquan/jianshen/starter/security/Rbac3BearerAuthenticationFilter.java`, `egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin/src/main/java/top/egon/cola/platform/tianquan/jianshen/admin/shared/tenant/controller/filter/TenantContextFilter.java`, `egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/src/main/java/top/egon/cola/component/tianshu/admin/security/registration/IdpJwtDdcRegistrationCredentialVerifier.java`。
- Commit: `feat(xingyuan): integrate edge identity with gateway rbac and tianshu`。

### Step 15 — 迁移共享认证客户端与五个前端消费者

- Requirements: REQ-003, REQ-004, REQ-010, REQ-017
- Dependencies: Step 14
- Baseline state: 当前HEAD的对应源码/测试如每File的Repository evidence，先前Step已完成并有其focused GREEN；本Step尚不存在gatewayAuthClient.test所列新行为。
- Observable outcome: Portal/四Admin前端按新BFF/Consent/Session契约运行且不保存token。
- End state: Portal/四Admin前端按新BFF/Consent/Session契约运行且不保存token；后续Step的Consumer尚未切流，不声明完整生产验收。
- Test-first gate: Required — gatewayAuthClient旧密码POST与AT/RT Cookie契约使新BFF test RED；共享客户端与各消费者更新后GREEN。
- Manual Checks: MC-ARCH-001, MC-SCOPE-001, MC-TEST-001, MC-JSON-001, MC-CONFIG-001
- Literal Rules: Rule 6, Rule 7, Rule 11
- Ordered files:

#### File 1 — `MODIFY egon-cola-xingyuan/egon-cola-xingyuan-admin-web-shared/src/auth/gatewayAuthClient.test.ts`

- Purpose: 把现有前端登录消费者迁到BFF，保留业务页面。
- Symbols: `gatewayAuthClient.test`。
- Repository evidence: 当前文件存在：egon-cola-xingyuan/egon-cola-xingyuan-admin-web-shared/src/auth/gatewayAuthClient.test.ts；目标源于主Spec §7–§16。
- Dependencies and consumers: 输入依赖 Step 14 与本Step此前文件；输出供 Portal/四Admin前端按新BFF/Consent/Session契约运行且不保存token及后一Step使用；Java Bean/Mapper按模块现有配置发现，前端按现有auth client消费。
- Why now: 本Step的编译前置/RED契约先锁定； 把现有前端登录消费者迁到BFF，保留业务页面在此位置后才可接消费者。
- Contract/signature changes: gatewayAuthClient.test；把现有前端登录消费者迁到BFF，保留业务页面。公开行为只取主Spec §9对应API或§10/11内部合同。
- Input/output and state mapping: tenantId/subject/displayName来自BFF session；无浏览器AT/RT。
- Error and edge behavior: network/timeout保留可重试态，不无限302。
- Standards impact: MC-SCOPE-001, MC-TEST-001；按已选传统分层，输入校验与权限先于状态读取，PO不出边界，相关日志脱敏。
- Literal rule enforcement: Rule 7, Rule 11；此文件直接按所列原始规则落实命名、验证、类型/注入或配置，其他规则由同Step关联文件负责。
- Implementation pseudocode:

```typescript
INPUT -> tenantId/subject/displayName来自BFF session；无浏览器AT/RT。
FLOW -> vitest: mock /bff/session,/bff/csrf,/bff/logout and login redirect; assert fresh tenant session, 401/403/503, no token in localStorage/cookie script; previous /oauth2/login test RED
ON INVALID/CONCURRENT/DEPENDENCY FAILURE -> network/timeout保留可重试态，不无限302。
RESULT -> only the named symbol/contract in this File block becomes available; preserve tenant, expectedVersion, scope, UTC time and secret masking stated above.
```

- Verification contribution: 本Step `npm --prefix egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin-web run test -- src/features/clients/ClientListPage.test.tsx src/features/resource-grants/ClientResourceGrantPage.test.tsx src/features/keys/SigningKeyPage.test.tsx src/features/resource-servers/ResourceServerListPage.test.tsx`；此文件负责 gatewayAuthClient.test 的正例与 network/timeout保留可重试态，不无限302 负例。
- After this file: gatewayAuthClient.test 的字段/入口或注册已落盘；与下一文件组合后本Step才能GREEN，不能把中间状态当完成。

#### File 2 — `MODIFY egon-cola-xingyuan/egon-cola-xingyuan-admin-web-shared/src/auth/gatewayAuthClient.ts`

- Purpose: 把现有前端登录消费者迁到BFF，保留业务页面。
- Symbols: `gatewayAuthClient`。
- Repository evidence: 当前文件存在：egon-cola-xingyuan/egon-cola-xingyuan-admin-web-shared/src/auth/gatewayAuthClient.ts；目标源于主Spec §7–§16。
- Dependencies and consumers: 输入依赖 Step 14 与本Step此前文件；输出供 Portal/四Admin前端按新BFF/Consent/Session契约运行且不保存token及后一Step使用；Java Bean/Mapper按模块现有配置发现，前端按现有auth client消费。
- Why now: 前一文件建立本文件需要的字段或调用契约； 把现有前端登录消费者迁到BFF，保留业务页面在此位置后才可接消费者。
- Contract/signature changes: gatewayAuthClient；把现有前端登录消费者迁到BFF，保留业务页面。公开行为只取主Spec §9对应API或§10/11内部合同。
- Input/output and state mapping: tenantId/subject/displayName来自BFF session；无浏览器AT/RT。
- Error and edge behavior: network/timeout保留可重试态，不无限302。
- Standards impact: MC-SCOPE-001, MC-TEST-001；按已选传统分层，输入校验与权限先于状态读取，PO不出边界，相关日志脱敏。
- Literal rule enforcement: Rule 7, Rule 11；此文件直接按所列原始规则落实命名、验证、类型/注入或配置，其他规则由同Step关联文件负责。
- Implementation pseudocode:

```typescript
INPUT -> tenantId/subject/displayName来自BFF session；无浏览器AT/RT。
FLOW -> createGatewayAuthClient: login→window.location to /oauth2/authorization/{registrationId}?returnTo=safePath; logout POST /bff/logout with X-CSRF-TOKEN; userInfo→GET /bff/session; bootstrap unchanged; never parse AT/RT
ON INVALID/CONCURRENT/DEPENDENCY FAILURE -> network/timeout保留可重试态，不无限302。
RESULT -> only the named symbol/contract in this File block becomes available; preserve tenant, expectedVersion, scope, UTC time and secret masking stated above.
```

- Verification contribution: 本Step `npm --prefix egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin-web run test -- src/features/clients/ClientListPage.test.tsx src/features/resource-grants/ClientResourceGrantPage.test.tsx src/features/keys/SigningKeyPage.test.tsx src/features/resource-servers/ResourceServerListPage.test.tsx`；此文件负责 gatewayAuthClient 的正例与 network/timeout保留可重试态，不无限302 负例。
- After this file: gatewayAuthClient 的字段/入口或注册已落盘；与下一文件组合后本Step才能GREEN，不能把中间状态当完成。

#### File 3 — `MODIFY egon-cola-xingyuan/egon-cola-xingyuan-admin-portal/src/auth/portalAuth.ts`

- Purpose: 把现有前端登录消费者迁到BFF，保留业务页面。
- Symbols: `portalAuth`。
- Repository evidence: 当前文件存在：egon-cola-xingyuan/egon-cola-xingyuan-admin-portal/src/auth/portalAuth.ts；目标源于主Spec §7–§16。
- Dependencies and consumers: 输入依赖 Step 14 与本Step此前文件；输出供 Portal/四Admin前端按新BFF/Consent/Session契约运行且不保存token及后一Step使用；Java Bean/Mapper按模块现有配置发现，前端按现有auth client消费。
- Why now: 前一文件建立本文件需要的字段或调用契约； 把现有前端登录消费者迁到BFF，保留业务页面在此位置后才可接消费者。
- Contract/signature changes: portalAuth；把现有前端登录消费者迁到BFF，保留业务页面。公开行为只取主Spec §9对应API或§10/11内部合同。
- Input/output and state mapping: tenantId/subject/displayName来自BFF session；无浏览器AT/RT。
- Error and edge behavior: network/timeout保留可重试态，不无限302。
- Standards impact: MC-SCOPE-001, MC-TEST-001；按已选传统分层，输入校验与权限先于状态读取，PO不出边界，相关日志脱敏。
- Literal rule enforcement: Rule 7, Rule 11；此文件直接按所列原始规则落实命名、验证、类型/注入或配置，其他规则由同Step关联文件负责。
- Implementation pseudocode:

```typescript
INPUT -> tenantId/subject/displayName来自BFF session；无浏览器AT/RT。
FLOW -> on mount GET /bff/session; authenticated→existing bootstrap; 401→login link, 403→forbidden, 503→retry panel; tenant change clears current authorization cache and starts new redirect; logout clears UI while remote pending shown; no form password posted to /oauth2/login
ON INVALID/CONCURRENT/DEPENDENCY FAILURE -> network/timeout保留可重试态，不无限302。
RESULT -> only the named symbol/contract in this File block becomes available; preserve tenant, expectedVersion, scope, UTC time and secret masking stated above.
```

- Verification contribution: 本Step `npm --prefix egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin-web run test -- src/features/clients/ClientListPage.test.tsx src/features/resource-grants/ClientResourceGrantPage.test.tsx src/features/keys/SigningKeyPage.test.tsx src/features/resource-servers/ResourceServerListPage.test.tsx`；此文件负责 portalAuth 的正例与 network/timeout保留可重试态，不无限302 负例。
- After this file: portalAuth 的字段/入口或注册已落盘；与下一文件组合后本Step才能GREEN，不能把中间状态当完成。

#### File 4 — `MODIFY egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin-web/src/auth/AuthContext.tsx`

- Purpose: 把现有前端登录消费者迁到BFF，保留业务页面。
- Symbols: `AuthContext`。
- Repository evidence: 当前文件存在：egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin-web/src/auth/AuthContext.tsx；目标源于主Spec §7–§16。
- Dependencies and consumers: 输入依赖 Step 14 与本Step此前文件；输出供 Portal/四Admin前端按新BFF/Consent/Session契约运行且不保存token及后一Step使用；Java Bean/Mapper按模块现有配置发现，前端按现有auth client消费。
- Why now: 前一文件建立本文件需要的字段或调用契约； 把现有前端登录消费者迁到BFF，保留业务页面在此位置后才可接消费者。
- Contract/signature changes: AuthContext；把现有前端登录消费者迁到BFF，保留业务页面。公开行为只取主Spec §9对应API或§10/11内部合同。
- Input/output and state mapping: tenantId/subject/displayName来自BFF session；无浏览器AT/RT。
- Error and edge behavior: network/timeout保留可重试态，不无限302。
- Standards impact: MC-SCOPE-001, MC-TEST-001；按已选传统分层，输入校验与权限先于状态读取，PO不出边界，相关日志脱敏。
- Literal rule enforcement: Rule 7, Rule 11；此文件直接按所列原始规则落实命名、验证、类型/注入或配置，其他规则由同Step关联文件负责。
- Implementation pseudocode:

```typescript
INPUT -> tenantId/subject/displayName来自BFF session；无浏览器AT/RT。
FLOW -> on mount GET /bff/session; authenticated→existing bootstrap; 401→login link, 403→forbidden, 503→retry panel; tenant change clears current authorization cache and starts new redirect; logout clears UI while remote pending shown; no form password posted to /oauth2/login
ON INVALID/CONCURRENT/DEPENDENCY FAILURE -> network/timeout保留可重试态，不无限302。
RESULT -> only the named symbol/contract in this File block becomes available; preserve tenant, expectedVersion, scope, UTC time and secret masking stated above.
```

- Verification contribution: 本Step `npm --prefix egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin-web run test -- src/features/clients/ClientListPage.test.tsx src/features/resource-grants/ClientResourceGrantPage.test.tsx src/features/keys/SigningKeyPage.test.tsx src/features/resource-servers/ResourceServerListPage.test.tsx`；此文件负责 AuthContext 的正例与 network/timeout保留可重试态，不无限302 负例。
- After this file: AuthContext 的字段/入口或注册已落盘；与下一文件组合后本Step才能GREEN，不能把中间状态当完成。

#### File 5 — `MODIFY egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin-web/src/auth/CentralLoginPage.tsx`

- Purpose: 把现有前端登录消费者迁到BFF，保留业务页面。
- Symbols: `CentralLoginPage`。
- Repository evidence: 当前文件存在：egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin-web/src/auth/CentralLoginPage.tsx；目标源于主Spec §7–§16。
- Dependencies and consumers: 输入依赖 Step 14 与本Step此前文件；输出供 Portal/四Admin前端按新BFF/Consent/Session契约运行且不保存token及后一Step使用；Java Bean/Mapper按模块现有配置发现，前端按现有auth client消费。
- Why now: 前一文件建立本文件需要的字段或调用契约； 把现有前端登录消费者迁到BFF，保留业务页面在此位置后才可接消费者。
- Contract/signature changes: CentralLoginPage；把现有前端登录消费者迁到BFF，保留业务页面。公开行为只取主Spec §9对应API或§10/11内部合同。
- Input/output and state mapping: tenantId/subject/displayName来自BFF session；无浏览器AT/RT。
- Error and edge behavior: network/timeout保留可重试态，不无限302。
- Standards impact: MC-SCOPE-001, MC-TEST-001；按已选传统分层，输入校验与权限先于状态读取，PO不出边界，相关日志脱敏。
- Literal rule enforcement: Rule 7, Rule 11；此文件直接按所列原始规则落实命名、验证、类型/注入或配置，其他规则由同Step关联文件负责。
- Implementation pseudocode:

```typescript
INPUT -> tenantId/subject/displayName来自BFF session；无浏览器AT/RT。
FLOW -> render issuer-bound login or consent HTML integration; prompt=consent displays approved scopes; wrong state/tenant/mustChangePassword maintains safe failure; no OAuth code/token in React state after redirect
ON INVALID/CONCURRENT/DEPENDENCY FAILURE -> network/timeout保留可重试态，不无限302。
RESULT -> only the named symbol/contract in this File block becomes available; preserve tenant, expectedVersion, scope, UTC time and secret masking stated above.
```

- Verification contribution: 本Step `npm --prefix egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin-web run test -- src/features/clients/ClientListPage.test.tsx src/features/resource-grants/ClientResourceGrantPage.test.tsx src/features/keys/SigningKeyPage.test.tsx src/features/resource-servers/ResourceServerListPage.test.tsx`；此文件负责 CentralLoginPage 的正例与 network/timeout保留可重试态，不无限302 负例。
- After this file: CentralLoginPage 的字段/入口或注册已落盘；与下一文件组合后本Step才能GREEN，不能把中间状态当完成。

#### File 6 — `MODIFY egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin-web/src/auth/AuthContext.tsx`

- Purpose: 把现有前端登录消费者迁到BFF，保留业务页面。
- Symbols: `AuthContext`。
- Repository evidence: 当前文件存在：egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin-web/src/auth/AuthContext.tsx；目标源于主Spec §7–§16。
- Dependencies and consumers: 输入依赖 Step 14 与本Step此前文件；输出供 Portal/四Admin前端按新BFF/Consent/Session契约运行且不保存token及后一Step使用；Java Bean/Mapper按模块现有配置发现，前端按现有auth client消费。
- Why now: 前一文件建立本文件需要的字段或调用契约； 把现有前端登录消费者迁到BFF，保留业务页面在此位置后才可接消费者。
- Contract/signature changes: AuthContext；把现有前端登录消费者迁到BFF，保留业务页面。公开行为只取主Spec §9对应API或§10/11内部合同。
- Input/output and state mapping: tenantId/subject/displayName来自BFF session；无浏览器AT/RT。
- Error and edge behavior: network/timeout保留可重试态，不无限302。
- Standards impact: MC-SCOPE-001, MC-TEST-001；按已选传统分层，输入校验与权限先于状态读取，PO不出边界，相关日志脱敏。
- Literal rule enforcement: Rule 7, Rule 11；此文件直接按所列原始规则落实命名、验证、类型/注入或配置，其他规则由同Step关联文件负责。
- Implementation pseudocode:

```typescript
INPUT -> tenantId/subject/displayName来自BFF session；无浏览器AT/RT。
FLOW -> on mount GET /bff/session; authenticated→existing bootstrap; 401→login link, 403→forbidden, 503→retry panel; tenant change clears current authorization cache and starts new redirect; logout clears UI while remote pending shown; no form password posted to /oauth2/login
ON INVALID/CONCURRENT/DEPENDENCY FAILURE -> network/timeout保留可重试态，不无限302。
RESULT -> only the named symbol/contract in this File block becomes available; preserve tenant, expectedVersion, scope, UTC time and secret masking stated above.
```

- Verification contribution: 本Step `npm --prefix egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin-web run test -- src/features/clients/ClientListPage.test.tsx src/features/resource-grants/ClientResourceGrantPage.test.tsx src/features/keys/SigningKeyPage.test.tsx src/features/resource-servers/ResourceServerListPage.test.tsx`；此文件负责 AuthContext 的正例与 network/timeout保留可重试态，不无限302 负例。
- After this file: AuthContext 的字段/入口或注册已落盘；与下一文件组合后本Step才能GREEN，不能把中间状态当完成。

#### File 7 — `MODIFY egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin-web/src/auth/LoginPage.tsx`

- Purpose: 把现有前端登录消费者迁到BFF，保留业务页面。
- Symbols: `LoginPage`。
- Repository evidence: 当前文件存在：egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin-web/src/auth/LoginPage.tsx；目标源于主Spec §7–§16。
- Dependencies and consumers: 输入依赖 Step 14 与本Step此前文件；输出供 Portal/四Admin前端按新BFF/Consent/Session契约运行且不保存token及后一Step使用；Java Bean/Mapper按模块现有配置发现，前端按现有auth client消费。
- Why now: 前一文件建立本文件需要的字段或调用契约； 把现有前端登录消费者迁到BFF，保留业务页面在此位置后才可接消费者。
- Contract/signature changes: LoginPage；把现有前端登录消费者迁到BFF，保留业务页面。公开行为只取主Spec §9对应API或§10/11内部合同。
- Input/output and state mapping: tenantId/subject/displayName来自BFF session；无浏览器AT/RT。
- Error and edge behavior: network/timeout保留可重试态，不无限302。
- Standards impact: MC-SCOPE-001, MC-TEST-001；按已选传统分层，输入校验与权限先于状态读取，PO不出边界，相关日志脱敏。
- Literal rule enforcement: Rule 7, Rule 11；此文件直接按所列原始规则落实命名、验证、类型/注入或配置，其他规则由同Step关联文件负责。
- Implementation pseudocode:

```typescript
INPUT -> tenantId/subject/displayName来自BFF session；无浏览器AT/RT。
FLOW -> on mount GET /bff/session; authenticated→existing bootstrap; 401→login link, 403→forbidden, 503→retry panel; tenant change clears current authorization cache and starts new redirect; logout clears UI while remote pending shown; no form password posted to /oauth2/login
ON INVALID/CONCURRENT/DEPENDENCY FAILURE -> network/timeout保留可重试态，不无限302。
RESULT -> only the named symbol/contract in this File block becomes available; preserve tenant, expectedVersion, scope, UTC time and secret masking stated above.
```

- Verification contribution: 本Step `npm --prefix egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin-web run test -- src/features/clients/ClientListPage.test.tsx src/features/resource-grants/ClientResourceGrantPage.test.tsx src/features/keys/SigningKeyPage.test.tsx src/features/resource-servers/ResourceServerListPage.test.tsx`；此文件负责 LoginPage 的正例与 network/timeout保留可重试态，不无限302 负例。
- After this file: LoginPage 的字段/入口或注册已落盘；与下一文件组合后本Step才能GREEN，不能把中间状态当完成。

#### File 8 — `MODIFY egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin-web/src/features/auth/gatewayAuth.ts`

- Purpose: 把现有前端登录消费者迁到BFF，保留业务页面。
- Symbols: `gatewayAuth`。
- Repository evidence: 当前文件存在：egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin-web/src/features/auth/gatewayAuth.ts；目标源于主Spec §7–§16。
- Dependencies and consumers: 输入依赖 Step 14 与本Step此前文件；输出供 Portal/四Admin前端按新BFF/Consent/Session契约运行且不保存token及后一Step使用；Java Bean/Mapper按模块现有配置发现，前端按现有auth client消费。
- Why now: 前一文件建立本文件需要的字段或调用契约； 把现有前端登录消费者迁到BFF，保留业务页面在此位置后才可接消费者。
- Contract/signature changes: gatewayAuth；把现有前端登录消费者迁到BFF，保留业务页面。公开行为只取主Spec §9对应API或§10/11内部合同。
- Input/output and state mapping: tenantId/subject/displayName来自BFF session；无浏览器AT/RT。
- Error and edge behavior: network/timeout保留可重试态，不无限302。
- Standards impact: MC-SCOPE-001, MC-TEST-001；按已选传统分层，输入校验与权限先于状态读取，PO不出边界，相关日志脱敏。
- Literal rule enforcement: Rule 7, Rule 11；此文件直接按所列原始规则落实命名、验证、类型/注入或配置，其他规则由同Step关联文件负责。
- Implementation pseudocode:

```typescript
INPUT -> tenantId/subject/displayName来自BFF session；无浏览器AT/RT。
FLOW -> on mount GET /bff/session; authenticated→existing bootstrap; 401→login link, 403→forbidden, 503→retry panel; tenant change clears current authorization cache and starts new redirect; logout clears UI while remote pending shown; no form password posted to /oauth2/login
ON INVALID/CONCURRENT/DEPENDENCY FAILURE -> network/timeout保留可重试态，不无限302。
RESULT -> only the named symbol/contract in this File block becomes available; preserve tenant, expectedVersion, scope, UTC time and secret masking stated above.
```

- Verification contribution: 本Step `npm --prefix egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin-web run test -- src/features/clients/ClientListPage.test.tsx src/features/resource-grants/ClientResourceGrantPage.test.tsx src/features/keys/SigningKeyPage.test.tsx src/features/resource-servers/ResourceServerListPage.test.tsx`；此文件负责 gatewayAuth 的正例与 network/timeout保留可重试态，不无限302 负例。
- After this file: gatewayAuth 的字段/入口或注册已落盘；与下一文件组合后本Step才能GREEN，不能把中间状态当完成。

#### File 9 — `MODIFY egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin-web/src/features/auth/AuthenticationShell.tsx`

- Purpose: 把现有前端登录消费者迁到BFF，保留业务页面。
- Symbols: `AuthenticationShell`。
- Repository evidence: 当前文件存在：egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin-web/src/features/auth/AuthenticationShell.tsx；目标源于主Spec §7–§16。
- Dependencies and consumers: 输入依赖 Step 14 与本Step此前文件；输出供 Portal/四Admin前端按新BFF/Consent/Session契约运行且不保存token及后一Step使用；Java Bean/Mapper按模块现有配置发现，前端按现有auth client消费。
- Why now: 前一文件建立本文件需要的字段或调用契约； 把现有前端登录消费者迁到BFF，保留业务页面在此位置后才可接消费者。
- Contract/signature changes: AuthenticationShell；把现有前端登录消费者迁到BFF，保留业务页面。公开行为只取主Spec §9对应API或§10/11内部合同。
- Input/output and state mapping: tenantId/subject/displayName来自BFF session；无浏览器AT/RT。
- Error and edge behavior: network/timeout保留可重试态，不无限302。
- Standards impact: MC-SCOPE-001, MC-TEST-001；按已选传统分层，输入校验与权限先于状态读取，PO不出边界，相关日志脱敏。
- Literal rule enforcement: Rule 7, Rule 11；此文件直接按所列原始规则落实命名、验证、类型/注入或配置，其他规则由同Step关联文件负责。
- Implementation pseudocode:

```typescript
INPUT -> tenantId/subject/displayName来自BFF session；无浏览器AT/RT。
FLOW -> on mount GET /bff/session; authenticated→existing bootstrap; 401→login link, 403→forbidden, 503→retry panel; tenant change clears current authorization cache and starts new redirect; logout clears UI while remote pending shown; no form password posted to /oauth2/login
ON INVALID/CONCURRENT/DEPENDENCY FAILURE -> network/timeout保留可重试态，不无限302。
RESULT -> only the named symbol/contract in this File block becomes available; preserve tenant, expectedVersion, scope, UTC time and secret masking stated above.
```

- Verification contribution: 本Step `npm --prefix egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin-web run test -- src/features/clients/ClientListPage.test.tsx src/features/resource-grants/ClientResourceGrantPage.test.tsx src/features/keys/SigningKeyPage.test.tsx src/features/resource-servers/ResourceServerListPage.test.tsx`；此文件负责 AuthenticationShell 的正例与 network/timeout保留可重试态，不无限302 负例。
- After this file: AuthenticationShell 的字段/入口或注册已落盘；与下一文件组合后本Step才能GREEN，不能把中间状态当完成。

#### File 10 — `MODIFY egon-cola-xingyuan/egon-cola-yuheng/yuheng-admin-web/src/auth/AuthContext.tsx`

- Purpose: 把现有前端登录消费者迁到BFF，保留业务页面。
- Symbols: `AuthContext`。
- Repository evidence: 当前文件存在：egon-cola-xingyuan/egon-cola-yuheng/yuheng-admin-web/src/auth/AuthContext.tsx；目标源于主Spec §7–§16。
- Dependencies and consumers: 输入依赖 Step 14 与本Step此前文件；输出供 Portal/四Admin前端按新BFF/Consent/Session契约运行且不保存token及后一Step使用；Java Bean/Mapper按模块现有配置发现，前端按现有auth client消费。
- Why now: 前一文件建立本文件需要的字段或调用契约； 把现有前端登录消费者迁到BFF，保留业务页面在此位置后才可接消费者。
- Contract/signature changes: AuthContext；把现有前端登录消费者迁到BFF，保留业务页面。公开行为只取主Spec §9对应API或§10/11内部合同。
- Input/output and state mapping: tenantId/subject/displayName来自BFF session；无浏览器AT/RT。
- Error and edge behavior: network/timeout保留可重试态，不无限302。
- Standards impact: MC-SCOPE-001, MC-TEST-001；按已选传统分层，输入校验与权限先于状态读取，PO不出边界，相关日志脱敏。
- Literal rule enforcement: Rule 7, Rule 11；此文件直接按所列原始规则落实命名、验证、类型/注入或配置，其他规则由同Step关联文件负责。
- Implementation pseudocode:

```typescript
INPUT -> tenantId/subject/displayName来自BFF session；无浏览器AT/RT。
FLOW -> on mount GET /bff/session; authenticated→existing bootstrap; 401→login link, 403→forbidden, 503→retry panel; tenant change clears current authorization cache and starts new redirect; logout clears UI while remote pending shown; no form password posted to /oauth2/login
ON INVALID/CONCURRENT/DEPENDENCY FAILURE -> network/timeout保留可重试态，不无限302。
RESULT -> only the named symbol/contract in this File block becomes available; preserve tenant, expectedVersion, scope, UTC time and secret masking stated above.
```

- Verification contribution: 本Step `npm --prefix egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin-web run test -- src/features/clients/ClientListPage.test.tsx src/features/resource-grants/ClientResourceGrantPage.test.tsx src/features/keys/SigningKeyPage.test.tsx src/features/resource-servers/ResourceServerListPage.test.tsx`；此文件负责 AuthContext 的正例与 network/timeout保留可重试态，不无限302 负例。
- After this file: AuthContext 的字段/入口或注册已落盘；与下一文件组合后本Step才能GREEN，不能把中间状态当完成。

#### File 11 — `MODIFY egon-cola-xingyuan/egon-cola-yuheng/yuheng-admin-web/src/auth/LoginPage.tsx`

- Purpose: 把现有前端登录消费者迁到BFF，保留业务页面。
- Symbols: `LoginPage`。
- Repository evidence: 当前文件存在：egon-cola-xingyuan/egon-cola-yuheng/yuheng-admin-web/src/auth/LoginPage.tsx；目标源于主Spec §7–§16。
- Dependencies and consumers: 输入依赖 Step 14 与本Step此前文件；输出供 Portal/四Admin前端按新BFF/Consent/Session契约运行且不保存token及后一Step使用；Java Bean/Mapper按模块现有配置发现，前端按现有auth client消费。
- Why now: 前一文件建立本文件需要的字段或调用契约； 把现有前端登录消费者迁到BFF，保留业务页面在此位置后才可接消费者。
- Contract/signature changes: LoginPage；把现有前端登录消费者迁到BFF，保留业务页面。公开行为只取主Spec §9对应API或§10/11内部合同。
- Input/output and state mapping: tenantId/subject/displayName来自BFF session；无浏览器AT/RT。
- Error and edge behavior: network/timeout保留可重试态，不无限302。
- Standards impact: MC-SCOPE-001, MC-TEST-001；按已选传统分层，输入校验与权限先于状态读取，PO不出边界，相关日志脱敏。
- Literal rule enforcement: Rule 7, Rule 11；此文件直接按所列原始规则落实命名、验证、类型/注入或配置，其他规则由同Step关联文件负责。
- Implementation pseudocode:

```typescript
INPUT -> tenantId/subject/displayName来自BFF session；无浏览器AT/RT。
FLOW -> on mount GET /bff/session; authenticated→existing bootstrap; 401→login link, 403→forbidden, 503→retry panel; tenant change clears current authorization cache and starts new redirect; logout clears UI while remote pending shown; no form password posted to /oauth2/login
ON INVALID/CONCURRENT/DEPENDENCY FAILURE -> network/timeout保留可重试态，不无限302。
RESULT -> only the named symbol/contract in this File block becomes available; preserve tenant, expectedVersion, scope, UTC time and secret masking stated above.
```

- Verification contribution: 本Step `npm --prefix egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin-web run test -- src/features/clients/ClientListPage.test.tsx src/features/resource-grants/ClientResourceGrantPage.test.tsx src/features/keys/SigningKeyPage.test.tsx src/features/resource-servers/ResourceServerListPage.test.tsx`；此文件负责 LoginPage 的正例与 network/timeout保留可重试态，不无限302 负例。
- After this file: LoginPage 的字段/入口或注册已落盘；与下一文件组合后本Step才能GREEN，不能把中间状态当完成。

#### File 12 — `MODIFY egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin-web/src/api/types.ts`

- Purpose: 主Spec §12的授柄管理页面realm/密钥/资源身份联动。
- Symbols: `OAuthClientVO/CreateOAuthClientDTO/SigningKeyVO/GrantVO`。
- Repository evidence: 当前文件存在，源接口与现有UI状态已读；当前状态/调用见文件本身。
- Dependencies and consumers: Step 9管理API/Step 13 SCG session及现有Ant Design、React Query；输出供Admin操作员。
- Why now: 共享认证Client在本Step此前文件就绪，管理页面才可绑定已验证realm。
- Contract/signature changes: 旧类型保留字段名并新增issuer/purpose/allowedScopes; tenantId null只按协议；服务端issuer上下文通过会话选择；沿用既有管理URL及主Spec §9字段。
- Input/output and state mapping: issuer由session已选realm，clientId/resourceId来自当前页面；VO只显示非秘密状态/版本，切tenant使queryKey不同并失效旧缓存。
- Error and edge behavior: 未授权不读取其他tenant；空、加载、权限、依赖失败分支独立；保存/删后按当前realm刷新。
- Standards impact: MC-ARCH-001, MC-NAME-001, MC-VALID-001, MC-JSON-001, MC-CONFIG-001, MC-SCOPE-001, MC-TEST-001；TypeScript字段与Java VO/JSON一致。
- Literal rule enforcement: Rule 6, Rule 7, Rule 11；仅消费Jackson已公开字段，保持前端原结构和各环境配置一致。
- Implementation pseudocode:

```typescript
INPUT -> 当前已验realm、已有UI路由、现有HttpClient和主Spec §9管理字段。
FLOW -> 旧类型保留字段名并新增issuer/purpose/allowedScopes; tenantId null只按协议；服务端issuer上下文通过会话选择。
OUTPUT -> OAuthClientVO/CreateOAuthClientDTO/SigningKeyVO/GrantVO使同一页只显示目标realm的数据/状态，写操作由服务端再次核验。
FAIL -> 401进入登录、403显示拒绝、404提示当前Client不存在、503保留可重试状态；不渲染令牌、私钥或跨realm缓存。
```

- Verification contribution: Step 15的Admin页面Vitest对realm/状态/安全的断言；缺失GET按修订Review状态单独阻塞。
- After this file: `OAuthClientVO/CreateOAuthClientDTO/SigningKeyVO/GrantVO`已具明确页面/测试合同；后续文件连起来完成本Step，未授权不能据此声称接口已获批。

#### File 13 — `MODIFY egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin-web/src/features/clients/ClientListPage.test.tsx`

- Purpose: 主Spec §12的授柄管理页面realm/密钥/资源身份联动。
- Symbols: `realmClientAndOneTimeSecret`。
- Repository evidence: 当前文件存在，源接口与现有UI状态已读；当前状态/调用见文件本身。
- Dependencies and consumers: Step 9管理API/Step 13 SCG session及现有Ant Design、React Query；输出供Admin操作员。
- Why now: 共享认证Client在本Step此前文件就绪，管理页面才可绑定已验证realm。
- Contract/signature changes: RED旧列表Query未按realm隔离；测试tenant/PLATFORM选择、issuer显示、新bcrypt secret只展示一次、切租户清缓存；沿用既有管理URL及主Spec §9字段。
- Input/output and state mapping: issuer由session已选realm，clientId/resourceId来自当前页面；VO只显示非秘密状态/版本，切tenant使queryKey不同并失效旧缓存。
- Error and edge behavior: 未授权不读取其他tenant；空、加载、权限、依赖失败分支独立；保存/删后按当前realm刷新。
- Standards impact: MC-ARCH-001, MC-NAME-001, MC-VALID-001, MC-JSON-001, MC-CONFIG-001, MC-SCOPE-001, MC-TEST-001；TypeScript字段与Java VO/JSON一致。
- Literal rule enforcement: Rule 6, Rule 7, Rule 11；仅消费Jackson已公开字段，保持前端原结构和各环境配置一致。
- Implementation pseudocode:

```typescript
INPUT -> 当前已验realm、已有UI路由、现有HttpClient和主Spec §9管理字段。
FLOW -> RED旧列表Query未按realm隔离；测试tenant/PLATFORM选择、issuer显示、新bcrypt secret只展示一次、切租户清缓存。
OUTPUT -> realmClientAndOneTimeSecret使同一页只显示目标realm的数据/状态，写操作由服务端再次核验。
FAIL -> 401进入登录、403显示拒绝、404提示当前Client不存在、503保留可重试状态；不渲染令牌、私钥或跨realm缓存。
```

- Verification contribution: Step 15的Admin页面Vitest对realm/状态/安全的断言；缺失GET按修订Review状态单独阻塞。
- After this file: `realmClientAndOneTimeSecret`已具明确页面/测试合同；后续文件连起来完成本Step，未授权不能据此声称接口已获批。

#### File 14 — `MODIFY egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin-web/src/features/clients/ClientListPage.tsx`

- Purpose: 主Spec §12的授柄管理页面realm/密钥/资源身份联动。
- Symbols: `clientsQuery/create/update/rotate`。
- Repository evidence: 当前文件存在，源接口与现有UI状态已读；当前状态/调用见文件本身。
- Dependencies and consumers: Step 9管理API/Step 13 SCG session及现有Ant Design、React Query；输出供Admin操作员。
- Why now: 共享认证Client在本Step此前文件就绪，管理页面才可绑定已验证realm。
- Contract/signature changes: queryKey附已验realm，PATCH可选allowedScopes，所有管理请求可选realm query；platform选择仅可获授权管理员；只一次展示secret，返回详情链接保留realm；沿用既有管理URL及主Spec §9字段。
- Input/output and state mapping: issuer由session已选realm，clientId/resourceId来自当前页面；VO只显示非秘密状态/版本，切tenant使queryKey不同并失效旧缓存。
- Error and edge behavior: 未授权不读取其他tenant；空、加载、权限、依赖失败分支独立；保存/删后按当前realm刷新。
- Standards impact: MC-ARCH-001, MC-NAME-001, MC-VALID-001, MC-JSON-001, MC-CONFIG-001, MC-SCOPE-001, MC-TEST-001；TypeScript字段与Java VO/JSON一致。
- Literal rule enforcement: Rule 6, Rule 7, Rule 11；仅消费Jackson已公开字段，保持前端原结构和各环境配置一致。
- Implementation pseudocode:

```typescript
INPUT -> 当前已验realm、已有UI路由、现有HttpClient和主Spec §9管理字段。
FLOW -> queryKey附已验realm，PATCH可选allowedScopes，所有管理请求可选realm query；platform选择仅可获授权管理员；只一次展示secret，返回详情链接保留realm。
OUTPUT -> clientsQuery/create/update/rotate使同一页只显示目标realm的数据/状态，写操作由服务端再次核验。
FAIL -> 401进入登录、403显示拒绝、404提示当前Client不存在、503保留可重试状态；不渲染令牌、私钥或跨realm缓存。
```

- Verification contribution: Step 15的Admin页面Vitest对realm/状态/安全的断言；缺失GET按修订Review状态单独阻塞。
- After this file: `clientsQuery/create/update/rotate`已具明确页面/测试合同；后续文件连起来完成本Step，未授权不能据此声称接口已获批。

#### File 15 — `MODIFY egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin-web/src/features/resource-grants/ClientResourceGrantPage.test.tsx`

- Purpose: 主Spec §12的授柄管理页面realm/密钥/资源身份联动。
- Symbols: `scopedGrantWriteAndQuery`。
- Repository evidence: 当前文件存在，源接口与现有UI状态已读；当前状态/调用见文件本身。
- Dependencies and consumers: Step 9管理API/Step 13 SCG session及现有Ant Design、React Query；输出供Admin操作员。
- Why now: 共享认证Client在本Step此前文件就绪，管理页面才可绑定已验证realm。
- Contract/signature changes: RED旧Grant queryKey无realm；测试切换realm不复用前一次数据、写入PUT/DELETE/batch也使用相同realm，404/403/503独立态；缺失GET标为修订API-045依赖；沿用既有管理URL及主Spec §9字段。
- Input/output and state mapping: issuer由session已选realm，clientId/resourceId来自当前页面；VO只显示非秘密状态/版本，切tenant使queryKey不同并失效旧缓存。
- Error and edge behavior: 未授权不读取其他tenant；空、加载、权限、依赖失败分支独立；保存/删后按当前realm刷新。
- Standards impact: MC-ARCH-001, MC-NAME-001, MC-VALID-001, MC-JSON-001, MC-CONFIG-001, MC-SCOPE-001, MC-TEST-001；TypeScript字段与Java VO/JSON一致。
- Literal rule enforcement: Rule 6, Rule 7, Rule 11；仅消费Jackson已公开字段，保持前端原结构和各环境配置一致。
- Implementation pseudocode:

```typescript
INPUT -> 当前已验realm、已有UI路由、现有HttpClient和主Spec §9管理字段。
FLOW -> RED旧Grant queryKey无realm；测试切换realm不复用前一次数据、写入PUT/DELETE/batch也使用相同realm，404/403/503独立态；缺失GET标为修订API-045依赖。
OUTPUT -> scopedGrantWriteAndQuery使同一页只显示目标realm的数据/状态，写操作由服务端再次核验。
FAIL -> 401进入登录、403显示拒绝、404提示当前Client不存在、503保留可重试状态；不渲染令牌、私钥或跨realm缓存。
```

- Verification contribution: Step 15的Admin页面Vitest对realm/状态/安全的断言；缺失GET按修订Review状态单独阻塞。
- After this file: `scopedGrantWriteAndQuery`已具明确页面/测试合同；后续文件连起来完成本Step，未授权不能据此声称接口已获批。

#### File 16 — `MODIFY egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin-web/src/features/resource-grants/ClientResourceGrantPage.tsx`

- Purpose: 主Spec §12的授柄管理页面realm/密钥/资源身份联动。
- Symbols: `grantQuery/upsert/delete/batch`。
- Repository evidence: 当前文件存在，源接口与现有UI状态已读；当前状态/调用见文件本身。
- Dependencies and consumers: Step 9管理API/Step 13 SCG session及现有Ant Design、React Query；输出供Admin操作员。
- Why now: 共享认证Client在本Step此前文件就绪，管理页面才可绑定已验证realm。
- Contract/signature changes: route已选realm入queryKey及所有管理请求；后端逐次核验；当前GET路由是否补齐取决修订Spec批准，不将其404当空列表；沿用既有管理URL及主Spec §9字段。
- Input/output and state mapping: issuer由session已选realm，clientId/resourceId来自当前页面；VO只显示非秘密状态/版本，切tenant使queryKey不同并失效旧缓存。
- Error and edge behavior: 未授权不读取其他tenant；空、加载、权限、依赖失败分支独立；保存/删后按当前realm刷新。
- Standards impact: MC-ARCH-001, MC-NAME-001, MC-VALID-001, MC-JSON-001, MC-CONFIG-001, MC-SCOPE-001, MC-TEST-001；TypeScript字段与Java VO/JSON一致。
- Literal rule enforcement: Rule 6, Rule 7, Rule 11；仅消费Jackson已公开字段，保持前端原结构和各环境配置一致。
- Implementation pseudocode:

```typescript
INPUT -> 当前已验realm、已有UI路由、现有HttpClient和主Spec §9管理字段。
FLOW -> route已选realm入queryKey及所有管理请求；后端逐次核验；当前GET路由是否补齐取决修订Spec批准，不将其404当空列表。
OUTPUT -> grantQuery/upsert/delete/batch使同一页只显示目标realm的数据/状态，写操作由服务端再次核验。
FAIL -> 401进入登录、403显示拒绝、404提示当前Client不存在、503保留可重试状态；不渲染令牌、私钥或跨realm缓存。
```

- Verification contribution: Step 15的Admin页面Vitest对realm/状态/安全的断言；缺失GET按修订Review状态单独阻塞。
- After this file: `grantQuery/upsert/delete/batch`已具明确页面/测试合同；后续文件连起来完成本Step，未授权不能据此声称接口已获批。

#### File 17 — `CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin-web/src/features/keys/SigningKeyPage.test.tsx`

- Purpose: 主Spec §12的授柄管理页面realm/密钥/资源身份联动。
- Symbols: `realmKeyRotationStates`。
- Repository evidence: 当前不存在；主Spec §12/14要求签名密钥页面负例测试；当前状态/调用见文件本身。
- Dependencies and consumers: Step 9管理API/Step 13 SCG session及现有Ant Design、React Query；输出供Admin操作员。
- Why now: 共享认证Client在本Step此前文件就绪，管理页面才可绑定已验证realm。
- Contract/signature changes: RED旧密钥列表无issuer/tenant; 测试PUBLISHED/ACTIVE/VERIFY_ONLY/RETIRED/COMPROMISED、版本冲突、平台权限与不回显私钥；沿用既有管理URL及主Spec §9字段。
- Input/output and state mapping: issuer由session已选realm，clientId/resourceId来自当前页面；VO只显示非秘密状态/版本，切tenant使queryKey不同并失效旧缓存。
- Error and edge behavior: 未授权不读取其他tenant；空、加载、权限、依赖失败分支独立；保存/删后按当前realm刷新。
- Standards impact: MC-ARCH-001, MC-NAME-001, MC-VALID-001, MC-JSON-001, MC-CONFIG-001, MC-SCOPE-001, MC-TEST-001；TypeScript字段与Java VO/JSON一致。
- Literal rule enforcement: Rule 6, Rule 7, Rule 11；仅消费Jackson已公开字段，保持前端原结构和各环境配置一致。
- Implementation pseudocode:

```typescript
INPUT -> 当前已验realm、已有UI路由、现有HttpClient和主Spec §9管理字段。
FLOW -> RED旧密钥列表无issuer/tenant; 测试PUBLISHED/ACTIVE/VERIFY_ONLY/RETIRED/COMPROMISED、版本冲突、平台权限与不回显私钥。
OUTPUT -> realmKeyRotationStates使同一页只显示目标realm的数据/状态，写操作由服务端再次核验。
FAIL -> 401进入登录、403显示拒绝、404提示当前Client不存在、503保留可重试状态；不渲染令牌、私钥或跨realm缓存。
```

- Verification contribution: Step 15的Admin页面Vitest对realm/状态/安全的断言；缺失GET按修订Review状态单独阻塞。
- After this file: `realmKeyRotationStates`已具明确页面/测试合同；后续文件连起来完成本Step，未授权不能据此声称接口已获批。

#### File 18 — `MODIFY egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin-web/src/features/keys/SigningKeyPage.tsx`

- Purpose: 主Spec §12的授柄管理页面realm/密钥/资源身份联动。
- Symbols: `keyList/publish/activate/retire`。
- Repository evidence: 当前文件存在，源接口与现有UI状态已读；当前状态/调用见文件本身。
- Dependencies and consumers: Step 9管理API/Step 13 SCG session及现有Ant Design、React Query；输出供Admin操作员。
- Why now: 共享认证Client在本Step此前文件就绪，管理页面才可绑定已验证realm。
- Contract/signature changes: 已验realm显示及可选platform; 后端操作附同realm/expectedVersion; 不渲染private key ciphertext，活跃key不能直接retire；沿用既有管理URL及主Spec §9字段。
- Input/output and state mapping: issuer由session已选realm，clientId/resourceId来自当前页面；VO只显示非秘密状态/版本，切tenant使queryKey不同并失效旧缓存。
- Error and edge behavior: 未授权不读取其他tenant；空、加载、权限、依赖失败分支独立；保存/删后按当前realm刷新。
- Standards impact: MC-ARCH-001, MC-NAME-001, MC-VALID-001, MC-JSON-001, MC-CONFIG-001, MC-SCOPE-001, MC-TEST-001；TypeScript字段与Java VO/JSON一致。
- Literal rule enforcement: Rule 6, Rule 7, Rule 11；仅消费Jackson已公开字段，保持前端原结构和各环境配置一致。
- Implementation pseudocode:

```typescript
INPUT -> 当前已验realm、已有UI路由、现有HttpClient和主Spec §9管理字段。
FLOW -> 已验realm显示及可选platform; 后端操作附同realm/expectedVersion; 不渲染private key ciphertext，活跃key不能直接retire。
OUTPUT -> keyList/publish/activate/retire使同一页只显示目标realm的数据/状态，写操作由服务端再次核验。
FAIL -> 401进入登录、403显示拒绝、404提示当前Client不存在、503保留可重试状态；不渲染令牌、私钥或跨realm缓存。
```

- Verification contribution: Step 15的Admin页面Vitest对realm/状态/安全的断言；缺失GET按修订Review状态单独阻塞。
- After this file: `keyList/publish/activate/retire`已具明确页面/测试合同；后续文件连起来完成本Step，未授权不能据此声称接口已获批。

#### File 19 — `MODIFY egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin-web/src/features/resource-servers/ResourceServerListPage.test.tsx`

- Purpose: 主Spec §12的授柄管理页面realm/密钥/资源身份联动。
- Symbols: `platformManagementClientBinding`。
- Repository evidence: 当前文件存在，源接口与现有UI状态已读；当前状态/调用见文件本身。
- Dependencies and consumers: Step 9管理API/Step 13 SCG session及现有Ant Design、React Query；输出供Admin操作员。
- Why now: 共享认证Client在本Step此前文件就绪，管理页面才可绑定已验证realm。
- Contract/signature changes: RED旧创建选择普通tenant Client；测试只提供合法platform SERVICE registration，不隐式grant；不同realm校验错误；沿用既有管理URL及主Spec §9字段。
- Input/output and state mapping: issuer由session已选realm，clientId/resourceId来自当前页面；VO只显示非秘密状态/版本，切tenant使queryKey不同并失效旧缓存。
- Error and edge behavior: 未授权不读取其他tenant；空、加载、权限、依赖失败分支独立；保存/删后按当前realm刷新。
- Standards impact: MC-ARCH-001, MC-NAME-001, MC-VALID-001, MC-JSON-001, MC-CONFIG-001, MC-SCOPE-001, MC-TEST-001；TypeScript字段与Java VO/JSON一致。
- Literal rule enforcement: Rule 6, Rule 7, Rule 11；仅消费Jackson已公开字段，保持前端原结构和各环境配置一致。
- Implementation pseudocode:

```typescript
INPUT -> 当前已验realm、已有UI路由、现有HttpClient和主Spec §9管理字段。
FLOW -> RED旧创建选择普通tenant Client；测试只提供合法platform SERVICE registration，不隐式grant；不同realm校验错误。
OUTPUT -> platformManagementClientBinding使同一页只显示目标realm的数据/状态，写操作由服务端再次核验。
FAIL -> 401进入登录、403显示拒绝、404提示当前Client不存在、503保留可重试状态；不渲染令牌、私钥或跨realm缓存。
```

- Verification contribution: Step 15的Admin页面Vitest对realm/状态/安全的断言；缺失GET按修订Review状态单独阻塞。
- After this file: `platformManagementClientBinding`已具明确页面/测试合同；后续文件连起来完成本Step，未授权不能据此声称接口已获批。

#### File 20 — `MODIFY egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin-web/src/features/resource-servers/ResourceServerListPage.tsx`

- Purpose: 主Spec §12的授柄管理页面realm/密钥/资源身份联动。
- Symbols: `resourceList/create/disable`。
- Repository evidence: 当前文件存在，源接口与现有UI状态已读；当前状态/调用见文件本身。
- Dependencies and consumers: Step 9管理API/Step 13 SCG session及现有Ant Design、React Query；输出供Admin操作员。
- Why now: 共享认证Client在本Step此前文件就绪，管理页面才可绑定已验证realm。
- Contract/signature changes: 客户端选择器明确平台SERVICE管理身份；保留resourceServerId/URI/RBAC字段和当前Admin页面布局；源app授权范围不扩大；沿用既有管理URL及主Spec §9字段。
- Input/output and state mapping: issuer由session已选realm，clientId/resourceId来自当前页面；VO只显示非秘密状态/版本，切tenant使queryKey不同并失效旧缓存。
- Error and edge behavior: 未授权不读取其他tenant；空、加载、权限、依赖失败分支独立；保存/删后按当前realm刷新。
- Standards impact: MC-ARCH-001, MC-NAME-001, MC-VALID-001, MC-JSON-001, MC-CONFIG-001, MC-SCOPE-001, MC-TEST-001；TypeScript字段与Java VO/JSON一致。
- Literal rule enforcement: Rule 6, Rule 7, Rule 11；仅消费Jackson已公开字段，保持前端原结构和各环境配置一致。
- Implementation pseudocode:

```typescript
INPUT -> 当前已验realm、已有UI路由、现有HttpClient和主Spec §9管理字段。
FLOW -> 客户端选择器明确平台SERVICE管理身份；保留resourceServerId/URI/RBAC字段和当前Admin页面布局；源app授权范围不扩大。
OUTPUT -> resourceList/create/disable使同一页只显示目标realm的数据/状态，写操作由服务端再次核验。
FAIL -> 401进入登录、403显示拒绝、404提示当前Client不存在、503保留可重试状态；不渲染令牌、私钥或跨realm缓存。
```

- Verification contribution: Step 15的Admin页面Vitest对realm/状态/安全的断言；缺失GET按修订Review状态单独阻塞。
- After this file: `resourceList/create/disable`已具明确页面/测试合同；后续文件连起来完成本Step，未授权不能据此声称接口已获批。

- Validation working directory: `/Users/mario/SelfProject/Egon-COLA`。
- Verification command: `npm --prefix egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin-web run test -- src/features/clients/ClientListPage.test.tsx src/features/resource-grants/ClientResourceGrantPage.test.tsx src/features/keys/SigningKeyPage.test.tsx src/features/resource-servers/ResourceServerListPage.test.tsx`。
- Expected result: 先确认上述RED由缺失目标行为引起，GREEN时命名用例成功、模块编译成功、`git diff --check`零错误；不以不存在依赖/数据库/fixture而失败作为RED。
- Failure returns to: Step 14 的编译/Schema前置；若需改变外部语义、架构或DDL版本则回主Spec。
- Completion criteria: Portal/四Admin前端按新BFF/Consent/Session契约运行且不保存token；本Step的所有Manual Checks在实际执行时给出证据，未通过不能提交。
- Rollback: 提交前仅恢复本Step的精确路径；DDL已对真实数据运行后只forward-fix和新版本，不能编辑既有SQL/历史或恢复旧安全世代。
- Commit paths: `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin-web/src/api/types.ts`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin-web/src/features/clients/ClientListPage.test.tsx`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin-web/src/features/clients/ClientListPage.tsx`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin-web/src/features/resource-grants/ClientResourceGrantPage.test.tsx`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin-web/src/features/resource-grants/ClientResourceGrantPage.tsx`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin-web/src/features/keys/SigningKeyPage.test.tsx`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin-web/src/features/keys/SigningKeyPage.tsx`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin-web/src/features/resource-servers/ResourceServerListPage.test.tsx`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin-web/src/features/resource-servers/ResourceServerListPage.tsx`, `egon-cola-xingyuan/egon-cola-xingyuan-admin-web-shared/src/auth/gatewayAuthClient.test.ts`, `egon-cola-xingyuan/egon-cola-xingyuan-admin-web-shared/src/auth/gatewayAuthClient.ts`, `egon-cola-xingyuan/egon-cola-xingyuan-admin-portal/src/auth/portalAuth.ts`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin-web/src/auth/AuthContext.tsx`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin-web/src/auth/CentralLoginPage.tsx`, `egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin-web/src/auth/AuthContext.tsx`, `egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin-web/src/auth/LoginPage.tsx`, `egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin-web/src/features/auth/gatewayAuth.ts`, `egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin-web/src/features/auth/AuthenticationShell.tsx`, `egon-cola-xingyuan/egon-cola-yuheng/yuheng-admin-web/src/auth/AuthContext.tsx`, `egon-cola-xingyuan/egon-cola-yuheng/yuheng-admin-web/src/auth/LoginPage.tsx`。
- Commit: `feat(xingyuan-web): move admin login to oidc bff`。

### Step 16 — 提供受控数据导入、切流及本地验证入口

- Requirements: REQ-011, REQ-012, REQ-016, REQ-017, REQ-018, REQ-019
- Dependencies: Step 14
- Baseline state: 当前HEAD的对应源码/测试如每File的Repository evidence，先前Step已完成并有其focused GREEN；本Step尚不存在dryRunNoPrivilegeExpansion所列新行为。
- Observable outcome: 逐租户导入/凭证轮换/切流报告可审，脚本只准备用户发起的运行验收。
- End state: 逐租户导入/凭证轮换/切流报告可审，脚本只准备用户发起的运行验收；后续Step的Consumer尚未切流，不声明完整生产验收。
- Test-first gate: Required — 迁移测试先对旧JPA/旧登录路径RED；新增Service/脚本/运行手册后静态与单元GREEN，真实导入由用户操作者运行。
- Manual Checks: MC-ARCH-001, MC-REUSE-001, MC-SCOPE-001, MC-TEST-001, MC-VALID-001, MC-NAME-001, MC-MODEL-001, MC-CONVERT-001, MC-LOG-001, MC-BEAN-001, MC-JSON-001, MC-TIME-001
- Literal Rules: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11
- Ordered files:

#### File 1 — `CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/tianquan/shoubing/admin/support/migration/OAuthMigrationServiceTest.java`

- Purpose: RED旧Client归属/非数字tenant处理。
- Symbols: `dryRunNoPrivilegeExpansion`。
- Repository evidence: 当前不存在；Spec §8/§11 明确目标，邻近包可验证；目标源于主Spec §7–§16。
- Dependencies and consumers: 输入依赖 Step 14 与本Step此前文件；输出供 逐租户导入/凭证轮换/切流报告可审，脚本只准备用户发起的运行验收及后一Step使用；Java Bean/Mapper按模块现有配置发现，前端按现有auth client消费。
- Why now: 本Step的编译前置/RED契约先锁定； RED旧Client归属/非数字tenant处理在此位置后才可接消费者。
- Contract/signature changes: dryRunNoPrivilegeExpansion；RED旧Client归属/非数字tenant处理。公开行为只取主Spec §9对应API或§10/11内部合同。
- Input/output and state mapping: Fake source ID到新Long PK的映射；旧secret hash未传入新注册。
- Error and edge behavior: invalid data stops that realm, leaves old source untouched。
- Standards impact: MC-ARCH-001, MC-SCOPE-001, MC-TEST-001, MC-VALID-001；按已选传统分层，输入校验与权限先于状态读取，PO不出边界，相关日志脱敏。
- Literal rule enforcement: Rule 11, Rule 2, Rule 4；此文件直接按所列原始规则落实命名、验证、类型/注入或配置，其他规则由同Step关联文件负责。
- Implementation pseudocode:

```java
INPUT -> Fake source ID到新Long PK的映射；旧secret hash未传入新注册。
FLOW -> mock旧表快照：非法tenantId、重复source归属、USER空scope、Argon2旧hash；dryRun只出报告不写新表，apply批次500可重放checkpoint；真实PostgreSQL迁移验收列于§8
ON INVALID/CONCURRENT/DEPENDENCY FAILURE -> invalid data stops that realm, leaves old source untouched。
RESULT -> only the named symbol/contract in this File block becomes available; preserve tenant, expectedVersion, scope, UTC time and secret masking stated above.
```

- Verification contribution: 本Step `python3 -m py_compile scripts/unified-xingyuan/controller_ui_coverage.py`；此文件负责 dryRunNoPrivilegeExpansion 的正例与 invalid data stops that realm, leaves old source untouched 负例。
- After this file: dryRunNoPrivilegeExpansion 的字段/入口或注册已落盘；与下一文件组合后本Step才能GREEN，不能把中间状态当完成。

#### File 2 — `CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/domain/command/OAuthMigrationCommand.java`

- Purpose: 关闭 `OAuthMigrationCommand` 在主Spec中的字段/消费依赖。
- Symbols: `OAuthMigrationCommand`。
- Repository evidence: 当前不存在；主Spec §16.2 命名此对象/字段；现有相邻业务包提供放置证据。
- Dependencies and consumers: 本Step既有File块调用该类型；数据来源/消费者为主Spec §16.2，不产生新HTTP路由。
- Why now: 该类型按本Step的测试/编译依赖顺序落位，以免由实现者临时发明。
- Contract/signature changes: realmKey/sourceSnapshotId/dryRun; internal operator input matches Spec §16.2, @Validated。
- Input/output and state mapping: 精确字段由主Spec §16.2 指定，realm从签名/注册得出，Instant为UTC，版本单调，秘密字段仅服务器内部。
- Error and edge behavior: null/未知字段/跨tenant/重复版本拒绝；不转成宽松缺省或吞异常。
- Standards impact: MC-ARCH-001, MC-NAME-001, MC-VALID-001, MC-MODEL-001, MC-CONVERT-001, MC-BEAN-001, MC-JSON-001, MC-TIME-001, MC-SCOPE-001, MC-TEST-001；行为Bean使用具名final Qualifier、@RequiredArgsConstructor和@Slf4j，载体遵循四注解与Builder。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 6, Rule 9, Rule 10, Rule 11；class按POJO语义命名，边界注解验证，跨层转换只用既有BaseConverter，Jackson和java.time字段明确。
- Implementation pseudocode:

```java
INPUT -> 仅接受主Spec §16.2 的可信issuer/realm/输入字段；调用前 @Valid 或 ValueObject不变量检查。
FLOW -> realmKey/sourceSnapshotId/dryRun; internal operator input matches Spec §16.2, @Validated。
OUTPUT -> OAuthMigrationCommand 的Java方法、DTO字段或配置可被本Step既有调用者编译使用；不新增业务API。
FAIL -> 空值/跨tenant/未知schema/重复版本/外部依赖错误按本Step已列错误模型fail-closed，不能输出原secret/token。
```

- Verification contribution: Step 16 的focus测试对 `OAuthMigrationCommand` 的字段、装配、负例及不泄密断言。
- After this file: `OAuthMigrationCommand` 成为已声明的Step依赖，下一文件可按原合同引用；未完成整条行为前不宣称GREEN。

#### File 3 — `CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/domain/result/OAuthMigrationReportResult.java`

- Purpose: 关闭 `OAuthMigrationReportResult` 在主Spec中的字段/消费依赖。
- Symbols: `OAuthMigrationReportResult`。
- Repository evidence: 当前不存在；主Spec §16.2 命名此对象/字段；现有相邻业务包提供放置证据。
- Dependencies and consumers: 本Step既有File块调用该类型；数据来源/消费者为主Spec §16.2，不产生新HTTP路由。
- Why now: 该类型按本Step的测试/编译依赖顺序落位，以免由实现者临时发明。
- Contract/signature changes: JSON report with realmKey/snapshotId/sourceCount/importedCount/conflicts/checkpointId; contains no secret or token。
- Input/output and state mapping: 精确字段由主Spec §16.2 指定，realm从签名/注册得出，Instant为UTC，版本单调，秘密字段仅服务器内部。
- Error and edge behavior: null/未知字段/跨tenant/重复版本拒绝；不转成宽松缺省或吞异常。
- Standards impact: MC-ARCH-001, MC-NAME-001, MC-VALID-001, MC-MODEL-001, MC-CONVERT-001, MC-BEAN-001, MC-JSON-001, MC-TIME-001, MC-SCOPE-001, MC-TEST-001；行为Bean使用具名final Qualifier、@RequiredArgsConstructor和@Slf4j，载体遵循四注解与Builder。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 6, Rule 9, Rule 10, Rule 11；class按POJO语义命名，边界注解验证，跨层转换只用既有BaseConverter，Jackson和java.time字段明确。
- Implementation pseudocode:

```java
INPUT -> 仅接受主Spec §16.2 的可信issuer/realm/输入字段；调用前 @Valid 或 ValueObject不变量检查。
FLOW -> JSON report with realmKey/snapshotId/sourceCount/importedCount/conflicts/checkpointId; contains no secret or token。
OUTPUT -> OAuthMigrationReportResult 的Java方法、DTO字段或配置可被本Step既有调用者编译使用；不新增业务API。
FAIL -> 空值/跨tenant/未知schema/重复版本/外部依赖错误按本Step已列错误模型fail-closed，不能输出原secret/token。
```

- Verification contribution: Step 16 的focus测试对 `OAuthMigrationReportResult` 的字段、装配、负例及不泄密断言。
- After this file: `OAuthMigrationReportResult` 成为已声明的Step依赖，下一文件可按原合同引用；未完成整条行为前不宣称GREEN。

#### File 4 — `CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/service/OAuthMigrationService.java`

- Purpose: 非自动启动的迁移服务合同。
- Symbols: `importRealm(realmKey,sourceSnapshotId,dryRun)`。
- Repository evidence: 当前不存在；Spec §8/§11 明确目标，邻近包可验证；目标源于主Spec §7–§16。
- Dependencies and consumers: 输入依赖 Step 14 与本Step此前文件；输出供 逐租户导入/凭证轮换/切流报告可审，脚本只准备用户发起的运行验收及后一Step使用；Java Bean/Mapper按模块现有配置发现，前端按现有auth client消费。
- Why now: 前一文件建立本文件需要的字段或调用契约； 非自动启动的迁移服务合同在此位置后才可接消费者。
- Contract/signature changes: importRealm(realmKey,sourceSnapshotId,dryRun)；非自动启动的迁移服务合同。公开行为只取主Spec §9对应API或§10/11内部合同。
- Input/output and state mapping: no runtime autostart。
- Error and edge behavior: unknown snapshot/version conflict stops。
- Standards impact: MC-ARCH-001, MC-SCOPE-001, MC-TEST-001, MC-VALID-001；按已选传统分层，输入校验与权限先于状态读取，PO不出边界，相关日志脱敏。
- Literal rule enforcement: Rule 11, Rule 2, Rule 4；此文件直接按所列原始规则落实命名、验证、类型/注入或配置，其他规则由同Step关联文件负责。
- Implementation pseudocode:

```java
INPUT -> no runtime autostart。
FLOW -> @Validated OAuthMigrationReportResult importRealm(@Valid OAuthMigrationCommand); dry-run report and apply same source snapshot; fields and statuses §16.2
ON INVALID/CONCURRENT/DEPENDENCY FAILURE -> unknown snapshot/version conflict stops。
RESULT -> only the named symbol/contract in this File block becomes available; preserve tenant, expectedVersion, scope, UTC time and secret masking stated above.
```

- Verification contribution: 本Step `python3 -m py_compile scripts/unified-xingyuan/controller_ui_coverage.py`；此文件负责 importRealm(realmKey,sourceSnapshotId,dryRun) 的正例与 unknown snapshot/version conflict stops 负例。
- After this file: importRealm(realmKey,sourceSnapshotId,dryRun) 的字段/入口或注册已落盘；与下一文件组合后本Step才能GREEN，不能把中间状态当完成。

#### File 5 — `CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/service/impl/OAuthMigrationServiceImpl.java`

- Purpose: 先剖析/对账后批次写。
- Symbols: `oauthMigrationService`。
- Repository evidence: 当前不存在；Spec §8/§11 明确目标，邻近包可验证；目标源于主Spec §7–§16。
- Dependencies and consumers: 输入依赖 Step 14 与本Step此前文件；输出供 逐租户导入/凭证轮换/切流报告可审，脚本只准备用户发起的运行验收及后一Step使用；Java Bean/Mapper按模块现有配置发现，前端按现有auth client消费。
- Why now: 前一文件建立本文件需要的字段或调用契约； 先剖析/对账后批次写在此位置后才可接消费者。
- Contract/signature changes: oauthMigrationService；先剖析/对账后批次写。公开行为只取主Spec §9对应API或§10/11内部合同。
- Input/output and state mapping: no old RT/code/consent; old client external ID preserved。
- Error and edge behavior: invalid/ambiguous ownership report, no auto repair。
- Standards impact: MC-ARCH-001, MC-SCOPE-001, MC-TEST-001, MC-VALID-001；按已选传统分层，输入校验与权限先于状态读取，PO不出边界，相关日志脱敏。
- Literal rule enforcement: Rule 1, Rule 2, Rule 4, Rule 9, Rule 10, Rule 11；此文件直接按所列原始规则落实命名、验证、类型/注入或配置，其他规则由同Step关联文件负责。
- Implementation pseudocode:

```java
INPUT -> no old RT/code/consent; old client external ID preserved。
FLOW -> @Service("oauthMigrationService") @Slf4j @RequiredArgsConstructor; read old snapshot via legacy read services; validate Long tenant canonical, map only granted issuer, enforce client purpose and bcrypt reissue; 500 rows per local tx, checkpoint lastSourceId, recheck source version before apply
ON INVALID/CONCURRENT/DEPENDENCY FAILURE -> invalid/ambiguous ownership report, no auto repair。
RESULT -> only the named symbol/contract in this File block becomes available; preserve tenant, expectedVersion, scope, UTC time and secret masking stated above.
```

- Verification contribution: 本Step `python3 -m py_compile scripts/unified-xingyuan/controller_ui_coverage.py`；此文件负责 oauthMigrationService 的正例与 invalid/ambiguous ownership report, no auto repair 负例。
- After this file: oauthMigrationService 的字段/入口或注册已落盘；与下一文件组合后本Step才能GREEN，不能把中间状态当完成。

#### File 6 — `MODIFY scripts/unified-xingyuan/prepare-local-stack.sh`

- Purpose: 准备TLS/新入口/独立server paths。
- Symbols: `issuer/SCG/secret mount env`。
- Repository evidence: 当前文件存在：scripts/unified-xingyuan/prepare-local-stack.sh；目标源于主Spec §7–§16。
- Dependencies and consumers: 输入依赖 Step 14 与本Step此前文件；输出供 逐租户导入/凭证轮换/切流报告可审，脚本只准备用户发起的运行验收及后一Step使用；Java Bean/Mapper按模块现有配置发现，前端按现有auth client消费。
- Why now: 前一文件建立本文件需要的字段或调用契约； 准备TLS/新入口/独立server paths在此位置后才可接消费者。
- Contract/signature changes: issuer/SCG/secret mount env；准备TLS/新入口/独立server paths。公开行为只取主Spec §9对应API或§10/11内部合同。
- Input/output and state mapping: same key structure as base/local; no secret echoed。
- Error and edge behavior: missing cert/secret fails script。
- Standards impact: MC-SCOPE-001, MC-TEST-001；按已选传统分层，输入校验与权限先于状态读取，PO不出边界，相关日志脱敏。
- Literal rule enforcement: Rule 11；此文件直接按所列原始规则落实命名、验证、类型/注入或配置，其他规则由同Step关联文件负责。
- Implementation pseudocode:

```text
INPUT -> same key structure as base/local; no secret echoed。
FLOW -> generate protected local SSL/KEK and env for SA,edge,biz,mcp,tianshu; no new service process started during Plan or code completion
ON INVALID/CONCURRENT/DEPENDENCY FAILURE -> missing cert/secret fails script。
RESULT -> only the named symbol/contract in this File block becomes available; preserve tenant, expectedVersion, scope, UTC time and secret masking stated above.
```

- Verification contribution: 本Step `python3 -m py_compile scripts/unified-xingyuan/controller_ui_coverage.py`；此文件负责 issuer/SCG/secret mount env 的正例与 missing cert/secret fails script 负例。
- After this file: issuer/SCG/secret mount env 的字段/入口或注册已落盘；与下一文件组合后本Step才能GREEN，不能把中间状态当完成。

#### File 7 — `MODIFY scripts/unified-xingyuan/test-direct-run-contract.sh`

- Purpose: 静态和受控环境契约验收。
- Symbols: `new issuer, jwks, wrong tenant/platform checks`。
- Repository evidence: 当前文件存在：scripts/unified-xingyuan/test-direct-run-contract.sh；目标源于主Spec §7–§16。
- Dependencies and consumers: 输入依赖 Step 14 与本Step此前文件；输出供 逐租户导入/凭证轮换/切流报告可审，脚本只准备用户发起的运行验收及后一Step使用；Java Bean/Mapper按模块现有配置发现，前端按现有auth client消费。
- Why now: 前一文件建立本文件需要的字段或调用契约； 静态和受控环境契约验收在此位置后才可接消费者。
- Contract/signature changes: new issuer, jwks, wrong tenant/platform checks；静态和受控环境契约验收。公开行为只取主Spec §9对应API或§10/11内部合同。
- Input/output and state mapping: expected 401/403/503 distinct。
- Error and edge behavior: do not treat HTTP200 root as login proof。
- Standards impact: MC-SCOPE-001, MC-TEST-001；按已选传统分层，输入校验与权限先于状态读取，PO不出边界，相关日志脱敏。
- Literal rule enforcement: Rule 11；此文件直接按所列原始规则落实命名、验证、类型/注入或配置，其他规则由同Step关联文件负责。
- Implementation pseudocode:

```text
INPUT -> expected 401/403/503 distinct。
FLOW -> assert role-specific env, public discovery, perissuer JWKS, platform claims, legacy allowlist cutoff; script only invoked by user-run local stack
ON INVALID/CONCURRENT/DEPENDENCY FAILURE -> do not treat HTTP200 root as login proof。
RESULT -> only the named symbol/contract in this File block becomes available; preserve tenant, expectedVersion, scope, UTC time and secret masking stated above.
```

- Verification contribution: 本Step `python3 -m py_compile scripts/unified-xingyuan/controller_ui_coverage.py`；此文件负责 new issuer, jwks, wrong tenant/platform checks 的正例与 do not treat HTTP200 root as login proof 负例。
- After this file: new issuer, jwks, wrong tenant/platform checks 的字段/入口或注册已落盘；与下一文件组合后本Step才能GREEN，不能把中间状态当完成。

#### File 8 — `MODIFY scripts/unified-xingyuan/test-live-frontend-login.sh`

- Purpose: 用户启动的浏览器/HTTP E2E核对。
- Symbols: `fresh BFF login/code/PKCE/consent/tenant`。
- Repository evidence: 当前文件存在：scripts/unified-xingyuan/test-live-frontend-login.sh；目标源于主Spec §7–§16。
- Dependencies and consumers: 输入依赖 Step 14 与本Step此前文件；输出供 逐租户导入/凭证轮换/切流报告可审，脚本只准备用户发起的运行验收及后一Step使用；Java Bean/Mapper按模块现有配置发现，前端按现有auth client消费。
- Why now: 前一文件建立本文件需要的字段或调用契约； 用户启动的浏览器/HTTP E2E核对在此位置后才可接消费者。
- Contract/signature changes: fresh BFF login/code/PKCE/consent/tenant；用户启动的浏览器/HTTP E2E核对。公开行为只取主Spec §9对应API或§10/11内部合同。
- Input/output and state mapping: no stale cookie fixture。
- Error and edge behavior: failure stops release, no auto service startup by Codex。
- Standards impact: MC-SCOPE-001, MC-TEST-001；按已选传统分层，输入校验与权限先于状态读取，PO不出边界，相关日志脱敏。
- Literal rule enforcement: Rule 11；此文件直接按所列原始规则落实命名、验证、类型/注入或配置，其他规则由同Step关联文件负责。
- Implementation pseudocode:

```text
INPUT -> no stale cookie fixture。
FLOW -> fresh session follows explicit redirect, validates cookies+CSRf, userinfo subject, RBAC bootstrap, refresh/new authorization, logout/revocation, four Admin endpoints
ON INVALID/CONCURRENT/DEPENDENCY FAILURE -> failure stops release, no auto service startup by Codex。
RESULT -> only the named symbol/contract in this File block becomes available; preserve tenant, expectedVersion, scope, UTC time and secret masking stated above.
```

- Verification contribution: 本Step `python3 -m py_compile scripts/unified-xingyuan/controller_ui_coverage.py`；此文件负责 fresh BFF login/code/PKCE/consent/tenant 的正例与 failure stops release, no auto service startup by Codex 负例。
- After this file: fresh BFF login/code/PKCE/consent/tenant 的字段/入口或注册已落盘；与下一文件组合后本Step才能GREEN，不能把中间状态当完成。

#### File 9 — `MODIFY docs/runbooks/unified-identity-oauth-client-tenant-cutover.md`

- Purpose: 写真实运维命令、反向证据和前向修复界限。
- Symbols: `per-tenant PREPARED→VERIFIED→CUTOVER→RETIRED`。
- Repository evidence: 当前文件存在：docs/runbooks/unified-identity-oauth-client-tenant-cutover.md；目标源于主Spec §7–§16。
- Dependencies and consumers: 输入依赖 Step 14 与本Step此前文件；输出供 逐租户导入/凭证轮换/切流报告可审，脚本只准备用户发起的运行验收及后一Step使用；Java Bean/Mapper按模块现有配置发现，前端按现有auth client消费。
- Why now: 前一文件建立本文件需要的字段或调用契约； 写真实运维命令、反向证据和前向修复界限在此位置后才可接消费者。
- Contract/signature changes: per-tenant PREPARED→VERIFIED→CUTOVER→RETIRED；写真实运维命令、反向证据和前向修复界限。公开行为只取主Spec §9对应API或§10/11内部合同。
- Input/output and state mapping: no production credentials or secrets in doc。
- Error and edge behavior: new grant/consent once written prohibits binary-only rollback。
- Standards impact: MC-SCOPE-001, MC-TEST-001；按已选传统分层，输入校验与权限先于状态读取，PO不出边界，相关日志脱敏。
- Literal rule enforcement: Rule 11；此文件直接按所列原始规则落实命名、验证、类型/注入或配置，其他规则由同Step关联文件负责。
- Implementation pseudocode:

```text
INPUT -> no production credentials or secrets in doc。
FLOW -> list secret rotation proofs, immutable Flyway B/V, MP manifest checksum/new schema, Redis fenced recovery, legacy issuer allowlist shrink, pertenant signoff and rollback barriers
ON INVALID/CONCURRENT/DEPENDENCY FAILURE -> new grant/consent once written prohibits binary-only rollback。
RESULT -> only the named symbol/contract in this File block becomes available; preserve tenant, expectedVersion, scope, UTC time and secret masking stated above.
```

- Verification contribution: 本Step `python3 -m py_compile scripts/unified-xingyuan/controller_ui_coverage.py`；此文件负责 per-tenant PREPARED→VERIFIED→CUTOVER→RETIRED 的正例与 new grant/consent once written prohibits binary-only rollback 负例。
- After this file: per-tenant PREPARED→VERIFIED→CUTOVER→RETIRED 的字段/入口或注册已落盘；与下一文件组合后本Step才能GREEN，不能把中间状态当完成。

- Validation working directory: `/Users/mario/SelfProject/Egon-COLA`。
- Verification command: `python3 -m py_compile scripts/unified-xingyuan/controller_ui_coverage.py`。
- Expected result: 先确认上述RED由缺失目标行为引起，GREEN时命名用例成功、模块编译成功、`git diff --check`零错误；不以不存在依赖/数据库/fixture而失败作为RED。
- Failure returns to: Step 15 的编译/Schema前置；若需改变外部语义、架构或DDL版本则回主Spec。
- Completion criteria: 逐租户导入/凭证轮换/切流报告可审，脚本只准备用户发起的运行验收；本Step的所有Manual Checks在实际执行时给出证据，未通过不能提交。
- Rollback: 提交前仅恢复本Step的精确路径；DDL已对真实数据运行后只forward-fix和新版本，不能编辑既有SQL/历史或恢复旧安全世代。
- Commit paths: `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/domain/result/OAuthMigrationReportResult.java`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/domain/command/OAuthMigrationCommand.java`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/tianquan/shoubing/admin/support/migration/OAuthMigrationServiceTest.java`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/service/OAuthMigrationService.java`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/service/impl/OAuthMigrationServiceImpl.java`, `scripts/unified-xingyuan/prepare-local-stack.sh`, `scripts/unified-xingyuan/test-direct-run-contract.sh`, `scripts/unified-xingyuan/test-live-frontend-login.sh`, `docs/runbooks/unified-identity-oauth-client-tenant-cutover.md`。
- Commit: `docs(xingyuan): prepare oauth migration and cutover verification`。

### Step 17 — 退役旧协议并校验全域回归

- Requirements: REQ-001, REQ-006, REQ-010, REQ-012, REQ-017, REQ-019
- Dependencies: Step 15
- Baseline state: 当前HEAD的对应源码/测试如每File的Repository evidence，先前Step已完成并有其focused GREEN；本Step尚不存在oldTenRoutes410所列新行为。
- Observable outcome: 新部署旧OAuth根协议返回410且无双签发链；所有原消费者已迁出。
- End state: 新部署旧OAuth根协议返回410且无双签发链；所有原消费者已迁出；后续Step的Consumer尚未切流，不声明完整生产验收。
- Test-first gate: Required — old token/login controllers still registered produce non410 RED；retirement filter/config GREEN。
- Manual Checks: MC-ARCH-001, MC-REUSE-001, MC-SCOPE-001, MC-TEST-001, MC-VALID-001, MC-NAME-001, MC-MODEL-001, MC-CONVERT-001, MC-LOG-001, MC-BEAN-001, MC-JSON-001, MC-TIME-001
- Literal Rules: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11
- Ordered files:

#### File 1 — `CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/LegacyOAuthRetirementTest.java`

- Purpose: RED旧Controller还签发/刷新。
- Symbols: `oldTenRoutes410`。
- Repository evidence: 当前不存在；Spec §8/§11 明确目标，邻近包可验证；目标源于主Spec §7–§16。
- Dependencies and consumers: 输入依赖 Step 15 与本Step此前文件；输出供 新部署旧OAuth根协议返回410且无双签发链；所有原消费者已迁出及后一Step使用；Java Bean/Mapper按模块现有配置发现，前端按现有auth client消费。
- Why now: 本Step的编译前置/RED契约先锁定； RED旧Controller还签发/刷新在此位置后才可接消费者。
- Contract/signature changes: oldTenRoutes410；RED旧Controller还签发/刷新。公开行为只取主Spec §9对应API或§10/11内部合同。
- Input/output and state mapping: legacy old instance isolated for not-yet-cutover tenant。
- Error and edge behavior: unknown old route does not redirect new login。
- Standards impact: MC-ARCH-001, MC-SCOPE-001, MC-TEST-001, MC-VALID-001；按已选传统分层，输入校验与权限先于状态读取，PO不出边界，相关日志脱敏。
- Literal rule enforcement: Rule 11, Rule 2, Rule 4；此文件直接按所列原始规则落实命名、验证、类型/注入或配置，其他规则由同Step关联文件负责。
- Implementation pseudocode:

```java
INPUT -> legacy old instance isolated for not-yet-cutover tenant。
FLOW -> GET/POST 10 old routes assert410+no token body/no user cookie/No AS fallback; grep references to old TokenFacade signing, frontend old login URLs, Tianshu platform credential path
ON INVALID/CONCURRENT/DEPENDENCY FAILURE -> unknown old route does not redirect new login。
RESULT -> only the named symbol/contract in this File block becomes available; preserve tenant, expectedVersion, scope, UTC time and secret masking stated above.
```

- Verification contribution: 本Step `mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=LegacyOAuthRetirementTest,OAuthOidcProtocolContractTest -Dsurefire.failIfNoSpecifiedTests=false test`；此文件负责 oldTenRoutes410 的正例与 unknown old route does not redirect new login 负例。
- After this file: oldTenRoutes410 的字段/入口或注册已落盘；与下一文件组合后本Step才能GREEN，不能把中间状态当完成。

#### File 2 — `CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/controller/filter/LegacyOAuthRetirementFilter.java`

- Purpose: 旧固定URL明确410。
- Symbols: `legacyOAuthRetirementFilter`。
- Repository evidence: 当前不存在；Spec §8/§11 明确目标，邻近包可验证；目标源于主Spec §7–§16。
- Dependencies and consumers: 输入依赖 Step 15 与本Step此前文件；输出供 新部署旧OAuth根协议返回410且无双签发链；所有原消费者已迁出及后一Step使用；Java Bean/Mapper按模块现有配置发现，前端按现有auth client消费。
- Why now: 前一文件建立本文件需要的字段或调用契约； 旧固定URL明确410在此位置后才可接消费者。
- Contract/signature changes: legacyOAuthRetirementFilter；旧固定URL明确410。公开行为只取主Spec §9对应API或§10/11内部合同。
- Input/output and state mapping: legacy client receives actionable error only。
- Error and edge behavior: methods not listed remain normal deny chain。
- Standards impact: MC-ARCH-001, MC-SCOPE-001, MC-TEST-001, MC-VALID-001；按已选传统分层，输入校验与权限先于状态读取，PO不出边界，相关日志脱敏。
- Literal rule enforcement: Rule 2, Rule 4, Rule 9, Rule 11；此文件直接按所列原始规则落实命名、验证、类型/注入或配置，其他规则由同Step关联文件负责。
- Implementation pseudocode:

```java
INPUT -> legacy client receives actionable error only。
FLOW -> @Component("legacyOAuthRetirementFilter") @Slf4j @RequiredArgsConstructor; match exact ten paths/methods from Spec API-035..044; return safe OAuth error+410; no old credential parse, no fallback to tenant guess
ON INVALID/CONCURRENT/DEPENDENCY FAILURE -> methods not listed remain normal deny chain。
RESULT -> only the named symbol/contract in this File block becomes available; preserve tenant, expectedVersion, scope, UTC time and secret masking stated above.
```

- Verification contribution: 本Step `mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=LegacyOAuthRetirementTest,OAuthOidcProtocolContractTest -Dsurefire.failIfNoSpecifiedTests=false test`；此文件负责 legacyOAuthRetirementFilter 的正例与 methods not listed remain normal deny chain 负例。
- After this file: legacyOAuthRetirementFilter 的字段/入口或注册已落盘；与下一文件组合后本Step才能GREEN，不能把中间状态当完成。

#### File 3 — `MODIFY egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/support/security/IdpSecurityConfig.java`

- Purpose: 先SAS登录/管理，再旧路径退役。
- Symbols: `3 ordered filter chains`。
- Repository evidence: 当前文件存在：egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/support/security/IdpSecurityConfig.java；目标源于主Spec §7–§16。
- Dependencies and consumers: 输入依赖 Step 15 与本Step此前文件；输出供 新部署旧OAuth根协议返回410且无双签发链；所有原消费者已迁出及后一Step使用；Java Bean/Mapper按模块现有配置发现，前端按现有auth client消费。
- Why now: 前一文件建立本文件需要的字段或调用契约； 先SAS登录/管理，再旧路径退役在此位置后才可接消费者。
- Contract/signature changes: 3 ordered filter chains；先SAS登录/管理，再旧路径退役。公开行为只取主Spec §9对应API或§10/11内部合同。
- Input/output and state mapping: tenant/issuer from accepted registry only。
- Error and edge behavior: no duplicate /oauth2/token handler mappings。
- Standards impact: MC-ARCH-001, MC-SCOPE-001, MC-TEST-001, MC-VALID-001；按已选传统分层，输入校验与权限先于状态读取，PO不出边界，相关日志脱敏。
- Literal rule enforcement: Rule 2, Rule 4, Rule 9, Rule 11；此文件直接按所列原始规则落实命名、验证、类型/注入或配置，其他规则由同Step关联文件负责。
- Implementation pseudocode:

```java
INPUT -> tenant/issuer from accepted registry only。
FLOW -> wire SAS higher-order protocol chain; management ResourceServer chain; legacy410/fallback deny; old OAuth token/login controller routes not registered
ON INVALID/CONCURRENT/DEPENDENCY FAILURE -> no duplicate /oauth2/token handler mappings。
RESULT -> only the named symbol/contract in this File block becomes available; preserve tenant, expectedVersion, scope, UTC time and secret masking stated above.
```

- Verification contribution: 本Step `mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=LegacyOAuthRetirementTest,OAuthOidcProtocolContractTest -Dsurefire.failIfNoSpecifiedTests=false test`；此文件负责 3 ordered filter chains 的正例与 no duplicate /oauth2/token handler mappings 负例。
- After this file: 3 ordered filter chains 的字段/入口或注册已落盘；与下一文件组合后本Step才能GREEN，不能把中间状态当完成。

#### File 4 — `MODIFY egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/controller/OAuthTokenController.java`

- Purpose: 旧POST /oauth2/token/revoke/logout停止处理。
- Symbols: `remove old mapping/bean registration`。
- Repository evidence: 当前文件存在：egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/controller/OAuthTokenController.java；目标源于主Spec §7–§16。
- Dependencies and consumers: 输入依赖 Step 15 与本Step此前文件；输出供 新部署旧OAuth根协议返回410且无双签发链；所有原消费者已迁出及后一Step使用；Java Bean/Mapper按模块现有配置发现，前端按现有auth client消费。
- Why now: 前一文件建立本文件需要的字段或调用契约； 旧POST /oauth2/token/revoke/logout停止处理在此位置后才可接消费者。
- Contract/signature changes: remove old mapping/bean registration；旧POST /oauth2/token/revoke/logout停止处理。公开行为只取主Spec §9对应API或§10/11内部合同。
- Input/output and state mapping: no new SAS forwarding controller。
- Error and edge behavior: no dual AT signing。
- Standards impact: MC-ARCH-001, MC-SCOPE-001, MC-TEST-001, MC-VALID-001；按已选传统分层，输入校验与权限先于状态读取，PO不出边界，相关日志脱敏。
- Literal rule enforcement: Rule 2, Rule 4, Rule 11；此文件直接按所列原始规则落实命名、验证、类型/注入或配置，其他规则由同Step关联文件负责。
- Implementation pseudocode:

```java
INPUT -> no new SAS forwarding controller。
FLOW -> remove old @PostMapping from new deployment or delete class after rg callers=0; old data only read by offline migration
ON INVALID/CONCURRENT/DEPENDENCY FAILURE -> no dual AT signing。
RESULT -> only the named symbol/contract in this File block becomes available; preserve tenant, expectedVersion, scope, UTC time and secret masking stated above.
```

- Verification contribution: 本Step `mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=LegacyOAuthRetirementTest,OAuthOidcProtocolContractTest -Dsurefire.failIfNoSpecifiedTests=false test`；此文件负责 remove old mapping/bean registration 的正例与 no dual AT signing 负例。
- After this file: remove old mapping/bean registration 的字段/入口或注册已落盘；与下一文件组合后本Step才能GREEN，不能把中间状态当完成。

#### File 5 — `MODIFY egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/controller/OAuthMetadataController.java`

- Purpose: 新issuer discovery ownsmetadata。
- Symbols: `remove fixed jwks metadata`。
- Repository evidence: 当前文件存在：egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/controller/OAuthMetadataController.java；目标源于主Spec §7–§16。
- Dependencies and consumers: 输入依赖 Step 15 与本Step此前文件；输出供 新部署旧OAuth根协议返回410且无双签发链；所有原消费者已迁出及后一Step使用；Java Bean/Mapper按模块现有配置发现，前端按现有auth client消费。
- Why now: 前一文件建立本文件需要的字段或调用契约； 新issuer discovery ownsmetadata在此位置后才可接消费者。
- Contract/signature changes: remove fixed jwks metadata；新issuer discovery ownsmetadata。公开行为只取主Spec §9对应API或§10/11内部合同。
- Input/output and state mapping: SAS publishes perrealm JWKS。
- Error and edge behavior: old root returns410 via filter。
- Standards impact: MC-ARCH-001, MC-SCOPE-001, MC-TEST-001, MC-VALID-001；按已选传统分层，输入校验与权限先于状态读取，PO不出边界，相关日志脱敏。
- Literal rule enforcement: Rule 2, Rule 4, Rule 11；此文件直接按所列原始规则落实命名、验证、类型/注入或配置，其他规则由同Step关联文件负责。
- Implementation pseudocode:

```java
INPUT -> SAS publishes perrealm JWKS。
FLOW -> remove fixed issuer /oauth2/jwks handlers after Spring route test; no one-key assumption
ON INVALID/CONCURRENT/DEPENDENCY FAILURE -> old root returns410 via filter。
RESULT -> only the named symbol/contract in this File block becomes available; preserve tenant, expectedVersion, scope, UTC time and secret masking stated above.
```

- Verification contribution: 本Step `mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=LegacyOAuthRetirementTest,OAuthOidcProtocolContractTest -Dsurefire.failIfNoSpecifiedTests=false test`；此文件负责 remove fixed jwks metadata 的正例与 old root returns410 via filter 负例。
- After this file: remove fixed jwks metadata 的字段/入口或注册已落盘；与下一文件组合后本Step才能GREEN，不能把中间状态当完成。

#### File 6 — `MODIFY egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/controller/OAuthLoginController.java`

- Purpose: 只接受新tenant login + BFF Code。
- Symbols: `remove password login mapping`。
- Repository evidence: 当前文件存在：egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/controller/OAuthLoginController.java；目标源于主Spec §7–§16。
- Dependencies and consumers: 输入依赖 Step 15 与本Step此前文件；输出供 新部署旧OAuth根协议返回410且无双签发链；所有原消费者已迁出及后一Step使用；Java Bean/Mapper按模块现有配置发现，前端按现有auth client消费。
- Why now: 前一文件建立本文件需要的字段或调用契约； 只接受新tenant login + BFF Code在此位置后才可接消费者。
- Contract/signature changes: remove password login mapping；只接受新tenant login + BFF Code。公开行为只取主Spec §9对应API或§10/11内部合同。
- Input/output and state mapping: new login session via TenantLoginController。
- Error and edge behavior: old cookies not issued。
- Standards impact: MC-ARCH-001, MC-SCOPE-001, MC-TEST-001, MC-VALID-001；按已选传统分层，输入校验与权限先于状态读取，PO不出边界，相关日志脱敏。
- Literal rule enforcement: Rule 2, Rule 4, Rule 11；此文件直接按所列原始规则落实命名、验证、类型/注入或配置，其他规则由同Step关联文件负责。
- Implementation pseudocode:

```java
INPUT -> new login session via TenantLoginController。
FLOW -> remove /oauth2/login/csrf and /oauth2/login handlers once UI regression green
ON INVALID/CONCURRENT/DEPENDENCY FAILURE -> old cookies not issued。
RESULT -> only the named symbol/contract in this File block becomes available; preserve tenant, expectedVersion, scope, UTC time and secret masking stated above.
```

- Verification contribution: 本Step `mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=LegacyOAuthRetirementTest,OAuthOidcProtocolContractTest -Dsurefire.failIfNoSpecifiedTests=false test`；此文件负责 remove password login mapping 的正例与 old cookies not issued 负例。
- After this file: remove password login mapping 的字段/入口或注册已落盘；与下一文件组合后本Step才能GREEN，不能把中间状态当完成。

#### File 7 — `MODIFY egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/controller/InternalRefreshTokenController.java`

- Purpose: new BFF uses SAS revocation and server session。
- Symbols: `remove old RT validation route`。
- Repository evidence: 当前文件存在：egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/controller/InternalRefreshTokenController.java；目标源于主Spec §7–§16。
- Dependencies and consumers: 输入依赖 Step 15 与本Step此前文件；输出供 新部署旧OAuth根协议返回410且无双签发链；所有原消费者已迁出及后一Step使用；Java Bean/Mapper按模块现有配置发现，前端按现有auth client消费。
- Why now: 前一文件建立本文件需要的字段或调用契约； new BFF uses SAS revocation and server session在此位置后才可接消费者。
- Contract/signature changes: remove old RT validation route；new BFF uses SAS revocation and server session。公开行为只取主Spec §9对应API或§10/11内部合同。
- Input/output and state mapping: no legacy RT backend call。
- Error and edge behavior: legacy GET/POST requests 410。
- Standards impact: MC-ARCH-001, MC-SCOPE-001, MC-TEST-001, MC-VALID-001；按已选传统分层，输入校验与权限先于状态读取，PO不出边界，相关日志脱敏。
- Literal rule enforcement: Rule 2, Rule 4, Rule 11；此文件直接按所列原始规则落实命名、验证、类型/注入或配置，其他规则由同Step关联文件负责。
- Implementation pseudocode:

```java
INPUT -> no legacy RT backend call。
FLOW -> remove /internal/v1/oauth2/refresh-token/validate handler after gateway tests
ON INVALID/CONCURRENT/DEPENDENCY FAILURE -> legacy GET/POST requests 410。
RESULT -> only the named symbol/contract in this File block becomes available; preserve tenant, expectedVersion, scope, UTC time and secret masking stated above.
```

- Verification contribution: 本Step `mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=LegacyOAuthRetirementTest,OAuthOidcProtocolContractTest -Dsurefire.failIfNoSpecifiedTests=false test`；此文件负责 remove old RT validation route 的正例与 legacy GET/POST requests 410 负例。
- After this file: remove old RT validation route 的字段/入口或注册已落盘；与下一文件组合后本Step才能GREEN，不能把中间状态当完成。

#### File 8 — `MODIFY egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/controller/OAuthStepUpController.java`

- Purpose: 关闭 `OAuthStepUpController` 在主Spec中的字段/消费依赖。
- Symbols: `OAuthStepUpController`。
- Repository evidence: 当前同名文件存在，符号/消费者由rg确认。
- Dependencies and consumers: 本Step既有File块调用该类型；数据来源/消费者为主Spec §8.4/§16，不产生新HTTP路由。
- Why now: 该类型按本Step的测试/编译依赖顺序落位，以免由实现者临时发明。
- Contract/signature changes: remove old /oauth2/step-up mapping after BFF max_age=0/acr path proven; 410 via retirement filter。
- Input/output and state mapping: 精确字段由主Spec §8.4/§16 指定，realm从签名/注册得出，Instant为UTC，版本单调，秘密字段仅服务器内部。
- Error and edge behavior: null/未知字段/跨tenant/重复版本拒绝；不转成宽松缺省或吞异常。
- Standards impact: MC-ARCH-001, MC-NAME-001, MC-VALID-001, MC-MODEL-001, MC-CONVERT-001, MC-BEAN-001, MC-JSON-001, MC-TIME-001, MC-SCOPE-001, MC-TEST-001；行为Bean使用具名final Qualifier、@RequiredArgsConstructor和@Slf4j，载体遵循四注解与Builder。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 6, Rule 9, Rule 10, Rule 11；class按POJO语义命名，边界注解验证，跨层转换只用既有BaseConverter，Jackson和java.time字段明确。
- Implementation pseudocode:

```java
INPUT -> 仅接受主Spec §8.4/§16 的可信issuer/realm/输入字段；调用前 @Valid 或 ValueObject不变量检查。
FLOW -> remove old /oauth2/step-up mapping after BFF max_age=0/acr path proven; 410 via retirement filter。
OUTPUT -> OAuthStepUpController 的Java方法、DTO字段或配置可被本Step既有调用者编译使用；不新增业务API。
FAIL -> 空值/跨tenant/未知schema/重复版本/外部依赖错误按本Step已列错误模型fail-closed，不能输出原secret/token。
```

- Verification contribution: Step 17 的focus测试对 `OAuthStepUpController` 的字段、装配、负例及不泄密断言。
- After this file: `OAuthStepUpController` 成为已声明的Step依赖，下一文件可按原合同引用；未完成整条行为前不宣称GREEN。

#### File 9 — `MODIFY egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/controller/OAuthUserInfoController.java`

- Purpose: 关闭 `OAuthUserInfoController` 在主Spec中的字段/消费依赖。
- Symbols: `OAuthUserInfoController`。
- Repository evidence: 当前同名文件存在，符号/消费者由rg确认。
- Dependencies and consumers: 本Step既有File块调用该类型；数据来源/消费者为主Spec §8.4/§16，不产生新HTTP路由。
- Why now: 该类型按本Step的测试/编译依赖顺序落位，以免由实现者临时发明。
- Contract/signature changes: remove old /oauth2/userinfo mapping; new issuer /tenants/{tenantId}/userinfo from SAS only。
- Input/output and state mapping: 精确字段由主Spec §8.4/§16 指定，realm从签名/注册得出，Instant为UTC，版本单调，秘密字段仅服务器内部。
- Error and edge behavior: null/未知字段/跨tenant/重复版本拒绝；不转成宽松缺省或吞异常。
- Standards impact: MC-ARCH-001, MC-NAME-001, MC-VALID-001, MC-MODEL-001, MC-CONVERT-001, MC-BEAN-001, MC-JSON-001, MC-TIME-001, MC-SCOPE-001, MC-TEST-001；行为Bean使用具名final Qualifier、@RequiredArgsConstructor和@Slf4j，载体遵循四注解与Builder。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 6, Rule 9, Rule 10, Rule 11；class按POJO语义命名，边界注解验证，跨层转换只用既有BaseConverter，Jackson和java.time字段明确。
- Implementation pseudocode:

```java
INPUT -> 仅接受主Spec §8.4/§16 的可信issuer/realm/输入字段；调用前 @Valid 或 ValueObject不变量检查。
FLOW -> remove old /oauth2/userinfo mapping; new issuer /tenants/{tenantId}/userinfo from SAS only。
OUTPUT -> OAuthUserInfoController 的Java方法、DTO字段或配置可被本Step既有调用者编译使用；不新增业务API。
FAIL -> 空值/跨tenant/未知schema/重复版本/外部依赖错误按本Step已列错误模型fail-closed，不能输出原secret/token。
```

- Verification contribution: Step 17 的focus测试对 `OAuthUserInfoController` 的字段、装配、负例及不泄密断言。
- After this file: `OAuthUserInfoController` 成为已声明的Step依赖，下一文件可按原合同引用；未完成整条行为前不宣称GREEN。

#### File 10 — `MODIFY egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/support/runtime/IdpPlatformConfiguration.java`

- Purpose: 关闭 `IdpPlatformConfiguration` 在主Spec中的字段/消费依赖。
- Symbols: `IdpPlatformConfiguration`。
- Repository evidence: 当前同名文件存在，符号/消费者由rg确认。
- Dependencies and consumers: 本Step既有File块调用该类型；数据来源/消费者为主Spec §8.4/§16，不产生新HTTP路由。
- Why now: 该类型按本Step的测试/编译依赖顺序落位，以免由实现者临时发明。
- Contract/signature changes: replace Rs256TokenService injection with perissuer signing/JWK components for platform control registration。
- Input/output and state mapping: 精确字段由主Spec §8.4/§16 指定，realm从签名/注册得出，Instant为UTC，版本单调，秘密字段仅服务器内部。
- Error and edge behavior: null/未知字段/跨tenant/重复版本拒绝；不转成宽松缺省或吞异常。
- Standards impact: MC-ARCH-001, MC-NAME-001, MC-VALID-001, MC-MODEL-001, MC-CONVERT-001, MC-BEAN-001, MC-JSON-001, MC-TIME-001, MC-SCOPE-001, MC-TEST-001；行为Bean使用具名final Qualifier、@RequiredArgsConstructor和@Slf4j，载体遵循四注解与Builder。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 6, Rule 9, Rule 10, Rule 11；class按POJO语义命名，边界注解验证，跨层转换只用既有BaseConverter，Jackson和java.time字段明确。
- Implementation pseudocode:

```java
INPUT -> 仅接受主Spec §8.4/§16 的可信issuer/realm/输入字段；调用前 @Valid 或 ValueObject不变量检查。
FLOW -> replace Rs256TokenService injection with perissuer signing/JWK components for platform control registration。
OUTPUT -> IdpPlatformConfiguration 的Java方法、DTO字段或配置可被本Step既有调用者编译使用；不新增业务API。
FAIL -> 空值/跨tenant/未知schema/重复版本/外部依赖错误按本Step已列错误模型fail-closed，不能输出原secret/token。
```

- Verification contribution: Step 17 的focus测试对 `IdpPlatformConfiguration` 的字段、装配、负例及不泄密断言。
- After this file: `IdpPlatformConfiguration` 成为已声明的Step依赖，下一文件可按原合同引用；未完成整条行为前不宣称GREEN。

#### File 11 — `MODIFY egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/config/TokenConfig.java`

- Purpose: 关闭 `TokenConfig` 在主Spec中的字段/消费依赖。
- Symbols: `TokenConfig`。
- Repository evidence: 当前同名文件存在，符号/消费者由rg确认。
- Dependencies and consumers: 本Step既有File块调用该类型；数据来源/消费者为主Spec §8.4/§16，不产生新HTTP路由。
- Why now: 该类型按本Step的测试/编译依赖顺序落位，以免由实现者临时发明。
- Contract/signature changes: remove legacy signer/refresh/TokenFacade Bean registration after all callers routed to SAS and legacy instance isolated。
- Input/output and state mapping: 精确字段由主Spec §8.4/§16 指定，realm从签名/注册得出，Instant为UTC，版本单调，秘密字段仅服务器内部。
- Error and edge behavior: null/未知字段/跨tenant/重复版本拒绝；不转成宽松缺省或吞异常。
- Standards impact: MC-ARCH-001, MC-NAME-001, MC-VALID-001, MC-MODEL-001, MC-CONVERT-001, MC-BEAN-001, MC-JSON-001, MC-TIME-001, MC-SCOPE-001, MC-TEST-001；行为Bean使用具名final Qualifier、@RequiredArgsConstructor和@Slf4j，载体遵循四注解与Builder。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 6, Rule 9, Rule 10, Rule 11；class按POJO语义命名，边界注解验证，跨层转换只用既有BaseConverter，Jackson和java.time字段明确。
- Implementation pseudocode:

```java
INPUT -> 仅接受主Spec §8.4/§16 的可信issuer/realm/输入字段；调用前 @Valid 或 ValueObject不变量检查。
FLOW -> remove legacy signer/refresh/TokenFacade Bean registration after all callers routed to SAS and legacy instance isolated。
OUTPUT -> TokenConfig 的Java方法、DTO字段或配置可被本Step既有调用者编译使用；不新增业务API。
FAIL -> 空值/跨tenant/未知schema/重复版本/外部依赖错误按本Step已列错误模型fail-closed，不能输出原secret/token。
```

- Verification contribution: Step 17 的focus测试对 `TokenConfig` 的字段、装配、负例及不泄密断言。
- After this file: `TokenConfig` 成为已声明的Step依赖，下一文件可按原合同引用；未完成整条行为前不宣称GREEN。

#### File 12 — `MODIFY egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/config/OAuthConfig.java`

- Purpose: 关闭 `OAuthConfig` 在主Spec中的字段/消费依赖。
- Symbols: `OAuthConfig`。
- Repository evidence: 当前同名文件存在，符号/消费者由rg确认。
- Dependencies and consumers: 本Step既有File块调用该类型；数据来源/消费者为主Spec §8.4/§16，不产生新HTTP路由。
- Why now: 该类型按本Step的测试/编译依赖顺序落位，以免由实现者临时发明。
- Contract/signature changes: remove legacy signer/client endpoint wiring after SAS provider tests/zero callers。
- Input/output and state mapping: 精确字段由主Spec §8.4/§16 指定，realm从签名/注册得出，Instant为UTC，版本单调，秘密字段仅服务器内部。
- Error and edge behavior: null/未知字段/跨tenant/重复版本拒绝；不转成宽松缺省或吞异常。
- Standards impact: MC-ARCH-001, MC-NAME-001, MC-VALID-001, MC-MODEL-001, MC-CONVERT-001, MC-BEAN-001, MC-JSON-001, MC-TIME-001, MC-SCOPE-001, MC-TEST-001；行为Bean使用具名final Qualifier、@RequiredArgsConstructor和@Slf4j，载体遵循四注解与Builder。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 6, Rule 9, Rule 10, Rule 11；class按POJO语义命名，边界注解验证，跨层转换只用既有BaseConverter，Jackson和java.time字段明确。
- Implementation pseudocode:

```java
INPUT -> 仅接受主Spec §8.4/§16 的可信issuer/realm/输入字段；调用前 @Valid 或 ValueObject不变量检查。
FLOW -> remove legacy signer/client endpoint wiring after SAS provider tests/zero callers。
OUTPUT -> OAuthConfig 的Java方法、DTO字段或配置可被本Step既有调用者编译使用；不新增业务API。
FAIL -> 空值/跨tenant/未知schema/重复版本/外部依赖错误按本Step已列错误模型fail-closed，不能输出原secret/token。
```

- Verification contribution: Step 17 的focus测试对 `OAuthConfig` 的字段、装配、负例及不泄密断言。
- After this file: `OAuthConfig` 成为已声明的Step依赖，下一文件可按原合同引用；未完成整条行为前不宣称GREEN。

- Validation working directory: `/Users/mario/SelfProject/Egon-COLA`。
- Verification command: `mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=LegacyOAuthRetirementTest,OAuthOidcProtocolContractTest -Dsurefire.failIfNoSpecifiedTests=false test`。
- Expected result: 先确认上述RED由缺失目标行为引起，GREEN时命名用例成功、模块编译成功、`git diff --check`零错误；不以不存在依赖/数据库/fixture而失败作为RED。
- Failure returns to: Step 16 的编译/Schema前置；若需改变外部语义、架构或DDL版本则回主Spec。
- Completion criteria: 新部署旧OAuth根协议返回410且无双签发链；所有原消费者已迁出；本Step的所有Manual Checks在实际执行时给出证据，未通过不能提交。
- Rollback: 提交前仅恢复本Step的精确路径；DDL已对真实数据运行后只forward-fix和新版本，不能编辑既有SQL/历史或恢复旧安全世代。
- Commit paths: `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/config/OAuthConfig.java`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/config/TokenConfig.java`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/support/runtime/IdpPlatformConfiguration.java`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/controller/OAuthUserInfoController.java`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/controller/OAuthStepUpController.java`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/LegacyOAuthRetirementTest.java`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/controller/filter/LegacyOAuthRetirementFilter.java`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/support/security/IdpSecurityConfig.java`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/controller/OAuthTokenController.java`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/controller/OAuthMetadataController.java`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/controller/OAuthLoginController.java`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/controller/InternalRefreshTokenController.java`。
- Commit: `refactor(shoubing): retire legacy oauth protocol`。

## 8. Test, Validation, and Quality Gates

Gate在**未来实施阶段**按次序执行，以下不是本轮已通过测试。先每Step focused RED/GREEN，后module regression，最后用户控制的真实PostgreSQL/Redis/TLS/浏览器验收。`mvn -o`在本机离线仓库缺未缓存BOM时只证明环境不足，不能被当成代码RED；批准的依赖必须由用户授权的执行环境供给。

| Gate/order | Working directory | Command or method | Scope | Expected result | Failure returns to | Requirements/runtime boundary |
| --- | --- | --- | --- | --- | --- | --- |
| Step 1 RED/GREEN | /Users/mario/SelfProject/Egon-COLA | mvn -o -pl egon-cola-xingyuan/egon-cola-yuheng/yuheng-edge-gateway -am -DskipTests validate | Maven reactor 能识别独立 SCG 模块；授柄SAS/MP/Session依赖及同一版本线受控 | focused class先因功能缺失RED，完成后exit0；不是缺依赖/服务 | Step 1 File 1/实现文件 | REQ-001, REQ-010, REQ-011, REQ-018；仅模块 |
| Step 2 RED/GREEN | /Users/mario/SelfProject/Egon-COLA | python3 .agents/skills/egon-coding-writing-plan/scripts/validate_skill_resources.py | 一个新SQL版本定义11表及约束；NATIVE SINGLE精准落在tianquan_oauth | focused class先因功能缺失RED，完成后exit0；不是缺依赖/服务 | Step 2 File 1/实现文件 | REQ-002, REQ-008, REQ-014, REQ-017, REQ-018；仅模块 |
| Step 3 RED/GREEN | /Users/mario/SelfProject/Egon-COLA | mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthRealmPersistenceContractTest -Dsurefire.failIfNoSpecifiedTests=false test | 这组认证事实的Repository/Mapper能按同realm/tenant/active/version读写 | focused class先因功能缺失RED，完成后exit0；不是缺依赖/服务 | Step 3 File 1/实现文件 | REQ-002, REQ-005, REQ-011, REQ-018；仅模块 |
| Step 4 RED/GREEN | /Users/mario/SelfProject/Egon-COLA | mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthResourcePersistenceContractTest -Dsurefire.failIfNoSpecifiedTests=false test | 这组认证事实的Repository/Mapper能按同realm/tenant/active/version读写 | focused class先因功能缺失RED，完成后exit0；不是缺依赖/服务 | Step 4 File 1/实现文件 | REQ-005, REQ-008, REQ-016, REQ-017；仅模块 |
| Step 5 RED/GREEN | /Users/mario/SelfProject/Egon-COLA | mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthAuthorizationPersistenceContractTest -Dsurefire.failIfNoSpecifiedTests=false test | 这组认证事实的Repository/Mapper能按同realm/tenant/active/version读写 | focused class先因功能缺失RED，完成后exit0；不是缺依赖/服务 | Step 5 File 1/实现文件 | REQ-003, REQ-004, REQ-012, REQ-017；仅模块 |
| Step 6 RED/GREEN | /Users/mario/SelfProject/Egon-COLA | mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthSigningKeyPersistenceContractTest -Dsurefire.failIfNoSpecifiedTests=false test | 这组认证事实的Repository/Mapper能按同realm/tenant/active/version读写 | focused class先因功能缺失RED，完成后exit0；不是缺依赖/服务 | Step 6 File 1/实现文件 | REQ-012, REQ-013, REQ-014, REQ-015；仅模块 |
| Step 7 RED/GREEN | /Users/mario/SelfProject/Egon-COLA | mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=TenantIssuerIsolationTest -Dsurefire.failIfNoSpecifiedTests=false test | SAS多issuer精确registry、JWK与protocol discovery逐租户独立 | focused class先因功能缺失RED，完成后exit0；不是缺依赖/服务 | Step 7 File 1/实现文件 | REQ-001, REQ-002, REQ-004, REQ-013, REQ-016；仅模块 |
| Step 8 RED/GREEN | /Users/mario/SelfProject/Egon-COLA | mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthOidcProtocolContractTest -Dsurefire.failIfNoSpecifiedTests=false test | 标准Code/PKCE/consent/login和机器Client Credentials签发保持scope/audience界限 | focused class先因功能缺失RED，完成后exit0；不是缺依赖/服务 | Step 8 File 1/实现文件 | REQ-003, REQ-004, REQ-005, REQ-011；仅模块 |
| Step 9 RED/GREEN | /Users/mario/SelfProject/Egon-COLA | mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthClientControllerTest,SigningKeyServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test | 现有管理URL按realm管理Client/Grant/Key；secret仅bcrypt一次性显示 | focused class先因功能缺失RED，完成后exit0；不是缺依赖/服务 | Step 9 File 1/实现文件 | REQ-002, REQ-005, REQ-011, REQ-013, REQ-017；仅模块 |
| Step 10 RED/GREEN | /Users/mario/SelfProject/Egon-COLA | mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=TokenRevocationServiceTest -Dsurefire.failIfNoSpecifiedTests=false test | 成功撤销后新鉴权拒绝；Redis故障503；审计持久可恢复 | focused class先因功能缺失RED，完成后exit0；不是缺依赖/服务 | Step 10 File 1/实现文件 | REQ-012, REQ-013, REQ-014, REQ-015, REQ-016；仅模块 |
| Step 11 RED/GREEN | /Users/mario/SelfProject/Egon-COLA | mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter -am -Dtest=MultiIssuerResourceServerTest -Dsurefire.failIfNoSpecifiedTests=false test | 所有受保护资源请求本地RS256后共享撤销检查，Servlet ThreadLocal/MDC最终清理 | focused class先因功能缺失RED，完成后exit0；不是缺依赖/服务 | Step 11 File 1/实现文件 | REQ-002, REQ-006, REQ-007, REQ-008, REQ-012, REQ-016；仅模块 |
| Step 12 RED/GREEN | /Users/mario/SelfProject/Egon-COLA | mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter -am -Dtest=IdpServiceOAuth2ClientTest -Dsurefire.failIfNoSpecifiedTests=false test | 出站调用按issuer/client/凭据版/aud/tenant/scope隔离并临期重获，无client_credentials RT | focused class先因功能缺失RED，完成后exit0；不是缺依赖/服务 | Step 12 File 1/实现文件 | REQ-005, REQ-009, REQ-011, REQ-016；仅模块 |
| Step 13 RED/GREEN | /Users/mario/SelfProject/Egon-COLA | mvn -o -pl egon-cola-xingyuan/egon-cola-yuheng/yuheng-edge-gateway -am -Dtest=EdgeLoginContractTest -Dsurefire.failIfNoSpecifiedTests=false test | 浏览器仅持HttpOnly opaque会话、SCG代码换token、玉衡路由原样转发Bearer | focused class先因功能缺失RED，完成后exit0；不是缺依赖/服务 | Step 13 File 1/实现文件 | REQ-003, REQ-004, REQ-006, REQ-010, REQ-011, REQ-012；仅模块 |
| Step 14 RED/GREEN | /Users/mario/SelfProject/Egon-COLA | mvn -o -pl egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin,egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-starter,egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-gateway-adapter -am -Dtest=IdpGatewaySecurityProviderTest,IdpJwtDdcRegistrationCredentialVerifierTest -Dsurefire.failIfNoSpecifiedTests=false test | 平台控制SERVICE token与USER授权在现有路由/鉴神/天枢链正确准入 | focused class先因功能缺失RED，完成后exit0；不是缺依赖/服务 | Step 14 File 1/实现文件 | REQ-001, REQ-005, REQ-006, REQ-007, REQ-010, REQ-016；仅模块 |
| Step 15 RED/GREEN | /Users/mario/SelfProject/Egon-COLA | npm --prefix egon-cola-xingyuan/egon-cola-xingyuan-admin-web-shared run test -- src/auth/gatewayAuthClient.test.ts | Portal/四Admin前端按新BFF/Consent/Session契约运行且不保存token | focused class先因功能缺失RED，完成后exit0；不是缺依赖/服务 | Step 15 File 1/实现文件 | REQ-003, REQ-004, REQ-010, REQ-017；仅模块 |
| Step 16 RED/GREEN | /Users/mario/SelfProject/Egon-COLA | python3 -m py_compile scripts/unified-xingyuan/controller_ui_coverage.py | 逐租户导入/凭证轮换/切流报告可审，脚本只准备用户发起的运行验收 | focused class先因功能缺失RED，完成后exit0；不是缺依赖/服务 | Step 16 File 1/实现文件 | REQ-011, REQ-012, REQ-016, REQ-017, REQ-018, REQ-019；仅模块 |
| Step 17 RED/GREEN | /Users/mario/SelfProject/Egon-COLA | mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=LegacyOAuthRetirementTest,OAuthOidcProtocolContractTest -Dsurefire.failIfNoSpecifiedTests=false test | 新部署旧OAuth根协议返回410且无双签发链；所有原消费者已迁出 | focused class先因功能缺失RED，完成后exit0；不是缺依赖/服务 | Step 17 File 1/实现文件 | REQ-001, REQ-006, REQ-010, REQ-012, REQ-017, REQ-019；仅模块 |

| Static contract | `/Users/mario/SelfProject/Egon-COLA` | `python3 .agents/skills/egon-coding-writing-plan/scripts/validate_plan.py <本Plan路径> --strict`、`git diff --check`、YAML/JSON解析及同键比较 | 结构/改动范围 | 全0错误；不能声称运行证明 | 本Plan/对应Step | REQ-018/019，文档/静态 |
| Affected Maven modules | `/Users/mario/SelfProject/Egon-COLA` | `mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin,egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter,egon-cola-xingyuan/egon-cola-yuheng/yuheng-edge-gateway -am test` | 新AS/RS/SCG与依赖 | exit0、无未指定测试伪通过 | owning Step | 静态/单元，不是Redis HA |
| Affected frontend | `/Users/mario/SelfProject/Egon-COLA` | `npm --prefix egon-cola-xingyuan/egon-cola-xingyuan-admin-web-shared run typecheck && npm --prefix egon-cola-xingyuan/egon-cola-xingyuan-admin-web-shared run test` | shared auth | exit0；每端另跑其现有typecheck/test脚本 | Step14 | REQ-010/017 |
| PostgreSQL schema/history | 用户控制PG环境 | `OAuthPersistenceMigrationIT`针对真实PostgreSQL，人工比对ddl_history/checksum/EXPLAIN/导入报告 | 旧V1–V6只读、新SQL单版本、11表 | 每目标SQL/history同事务，错行阻断且可续跑 | Step2–6/15 | REQ-008/017/018；运行须用户启动 |
| Redis/security failover | 用户控制Redis环境 | `TokenRevocationServiceTest`双AS双RS故障注入，先fence后restore再ready | 撤销成功点、epoch、审计spool | ACK后新请求401、Redis断503、没有已放行却无持久审计 | Step9–13 | REQ-012/014/015；运行须用户启动 |
| Fresh browser/SCG/TLS | 用户启动本地栈后 | `scripts/unified-xingyuan/test-live-frontend-login.sh` + `test-direct-run-contract.sh`，按部署计划人工检证第三方PKCE | code/consent/USER/SERVICE/PLATFORM与五端UI | 无XSS/头部伪造、tenant隔离、鉴神角色保持、无旧cookie | Step12–16 | REQ-003/010/016/017；Codex不自启 |

## 9. Migration, Compatibility, Rollout, and Rollback

按主Spec §16：先部署会识别旧issuer的限制策略与新证书/静态信任，原授权库继续作为未切tenant只读/旧部署权威；新空tianquan_oauth由Runner受管一个V20260922_001 SQL+manifest创建11表。旧public Flyway V1–V6不改，不运行repair，不将非空schema自动接管。导入用OAuthMigrationService显式dryRun→操作员审查→apply，每批500、version/checkpoint验证；不导入旧RT、code、consent，也不自动给第三方scope。每个confidential Client分发新bcrypt secret一次并确认调用方切换后，按tenant PREPARED→VERIFIED→CUTOVER→RETIRED；切流要求重新登录，旧tenant从legacy allowlist按版本删除并获所有RS ACK，新issuer生效。新写出现后不能仅回滚二进制接旧库；暂停签发、核对新事实后forward-fix。Redis PRIMARY切换先fence旧实例/撤readiness→新epoch→从DB恢复所有未过期拒绝/世代→Outbox补投→核对revision→开放；无安全fence不go-live。

本Plan只写文件和未来门禁，不执行任一数据库迁移、secret轮换或服务启动。不能要求用户把生产秘钥贴入文件/聊天；环境变量和受控挂载提供具体值。旧浏览器登录/AT/RT Cookie在切流后不接受，老端点410；旧平台机器身份scope只对天枢控制面，业务RS仍拒绝PLATFORM。

## 10. Requirement-to-Step Traceability Matrix

| Requirement | Effective Spec section | Steps | Files | Tests/gates | Completion evidence |
| --- | --- | --- | --- | --- | --- |
| REQ-001 | [主Spec](../spec/2026-09-22-17-14-tianquan-multitenant-oauth-oidc-design.md) §7/9/15/16 | Step 1; Step 7; Step 14; Step 17 | `egon-cola-xingyuan/pom.xml`; `egon-cola-xingyuan/egon-cola-yuheng/pom.xml`; `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/TenantIssuerIsolationTest.java`; `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/service/TenantIssuerService.java`; `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-gateway-adapter/src/test/java/top/egon/cola/platform/tianquan/shoubing/yuheng/security/IdpGatewaySecurityProviderTest.java`; `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-gateway-adapter/src/main/java/top/egon/cola/platform/tianquan/shoubing/yuheng/security/IdpIdentityAuthenticationProvider.java`; `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/LegacyOAuthRetirementTest.java`; `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/controller/filter/LegacyOAuthRetirementFilter.java` | 对应Step RED/GREEN与§8矩阵 | 只有授柄签发令牌，鉴神不保存密码/私钥 |
| REQ-002 | [主Spec](../spec/2026-09-22-17-14-tianquan-multitenant-oauth-oidc-design.md) §7/9/15/16 | Step 2; Step 3; Step 7; Step 9; Step 11 | `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/config/OAuthManagedSchemaContractTest.java`; `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/resources/db/egon-mp/V20260922_001__initialize_oauth_tenant_schema.sql`; `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/domain/po/OAuthRealmPO.java`; `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/domain/bo/OAuthRealmBO.java`; `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/TenantIssuerIsolationTest.java`; `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/service/TenantIssuerService.java`; `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/controller/OAuthClientControllerTest.java`; `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/domain/dto/CreateOAuthClientDTO.java`; `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/test/java/top/egon/cola/platform/tianquan/shoubing/starter/security/MultiIssuerResourceServerTest.java`; `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/tianquan/shoubing/starter/security/TenantIssuerAuthenticationManagerComponent.java` | 对应Step RED/GREEN与§8矩阵 | A 租户 code/client/key 对 B 不可用；未知 issuer 无出站请求 |
| REQ-003 | [主Spec](../spec/2026-09-22-17-14-tianquan-multitenant-oauth-oidc-design.md) §7/9/15/16 | Step 5; Step 8; Step 13; Step 15 | `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/domain/po/OAuthAuthorizationPO.java`; `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/domain/bo/OAuthAuthorizationBO.java`; `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/controller/OAuthOidcProtocolContractTest.java`; `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/service/OAuthTokenIssueService.java`; `egon-cola-xingyuan/egon-cola-yuheng/yuheng-edge-gateway/src/test/java/top/egon/cola/component/yuheng/edge/EdgeLoginContractTest.java`; `egon-cola-xingyuan/egon-cola-yuheng/yuheng-edge-gateway/src/main/java/top/egon/cola/component/yuheng/edge/config/EdgeOAuthProperties.java`; `egon-cola-xingyuan/egon-cola-xingyuan-admin-web-shared/src/auth/gatewayAuthClient.test.ts`; `egon-cola-xingyuan/egon-cola-xingyuan-admin-web-shared/src/auth/gatewayAuthClient.ts` | 对应Step RED/GREEN与§8矩阵 | 缺 PKCE/plain/错 verifier/URI 通配符/重复 code 均拒绝 |
| REQ-004 | [主Spec](../spec/2026-09-22-17-14-tianquan-multitenant-oauth-oidc-design.md) §7/9/15/16 | Step 5; Step 7; Step 8; Step 13; Step 15 | `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/domain/po/OAuthAuthorizationPO.java`; `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/domain/bo/OAuthAuthorizationBO.java`; `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/TenantIssuerIsolationTest.java`; `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/service/TenantIssuerService.java`; `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/controller/OAuthOidcProtocolContractTest.java`; `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/service/OAuthTokenIssueService.java`; `egon-cola-xingyuan/egon-cola-yuheng/yuheng-edge-gateway/src/test/java/top/egon/cola/component/yuheng/edge/EdgeLoginContractTest.java`; `egon-cola-xingyuan/egon-cola-yuheng/yuheng-edge-gateway/src/main/java/top/egon/cola/component/yuheng/edge/config/EdgeOAuthProperties.java`; `egon-cola-xingyuan/egon-cola-xingyuan-admin-web-shared/src/auth/gatewayAuthClient.test.ts`; `egon-cola-xingyuan/egon-cola-xingyuan-admin-web-shared/src/auth/gatewayAuthClient.ts` | 对应Step RED/GREEN与§8矩阵 | iss/aud/nonce 校验、UserInfo sub 一致，ID Token 不能访问业务 |
| REQ-005 | [主Spec](../spec/2026-09-22-17-14-tianquan-multitenant-oauth-oidc-design.md) §7/9/15/16 | Step 3; Step 4; Step 8; Step 9; Step 12; Step 14 | `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/domain/po/OAuthRealmPO.java`; `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/domain/bo/OAuthRealmBO.java`; `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/resource/domain/po/OAuthResourcePO.java`; `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/resource/domain/bo/OAuthResourceBO.java`; `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/controller/OAuthOidcProtocolContractTest.java`; `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/service/OAuthTokenIssueService.java`; `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/controller/OAuthClientControllerTest.java`; `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/domain/dto/CreateOAuthClientDTO.java`; `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/test/java/top/egon/cola/platform/tianquan/shoubing/starter/client/IdpServiceOAuth2ClientTest.java`; `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/tianquan/shoubing/starter/client/ServiceAuthorizationKey.java`; `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-gateway-adapter/src/test/java/top/egon/cola/platform/tianquan/shoubing/yuheng/security/IdpGatewaySecurityProviderTest.java`; `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-gateway-adapter/src/main/java/top/egon/cola/platform/tianquan/shoubing/yuheng/security/IdpIdentityAuthenticationProvider.java` | 对应Step RED/GREEN与§8矩阵 | SERVICE 无 refresh_token/ID Token；scope/resource 超授权拒绝 |
| REQ-006 | [主Spec](../spec/2026-09-22-17-14-tianquan-multitenant-oauth-oidc-design.md) §7/9/15/16 | Step 11; Step 13; Step 14; Step 17 | `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/test/java/top/egon/cola/platform/tianquan/shoubing/starter/security/MultiIssuerResourceServerTest.java`; `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/tianquan/shoubing/starter/security/TenantIssuerAuthenticationManagerComponent.java`; `egon-cola-xingyuan/egon-cola-yuheng/yuheng-edge-gateway/src/test/java/top/egon/cola/component/yuheng/edge/EdgeLoginContractTest.java`; `egon-cola-xingyuan/egon-cola-yuheng/yuheng-edge-gateway/src/main/java/top/egon/cola/component/yuheng/edge/config/EdgeOAuthProperties.java`; `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-gateway-adapter/src/test/java/top/egon/cola/platform/tianquan/shoubing/yuheng/security/IdpGatewaySecurityProviderTest.java`; `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-gateway-adapter/src/main/java/top/egon/cola/platform/tianquan/shoubing/yuheng/security/IdpIdentityAuthenticationProvider.java`; `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/LegacyOAuthRetirementTest.java`; `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/controller/filter/LegacyOAuthRetirementFilter.java` | 对应Step RED/GREEN与§8矩阵 | 有缓存公钥时不逐请求调用授权服务器；错 aud/alg 拒绝 |
| REQ-007 | [主Spec](../spec/2026-09-22-17-14-tianquan-multitenant-oauth-oidc-design.md) §7/9/15/16 | Step 11; Step 14 | `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/test/java/top/egon/cola/platform/tianquan/shoubing/starter/security/MultiIssuerResourceServerTest.java`; `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/tianquan/shoubing/starter/security/TenantIssuerAuthenticationManagerComponent.java`; `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-gateway-adapter/src/test/java/top/egon/cola/platform/tianquan/shoubing/yuheng/security/IdpGatewaySecurityProviderTest.java`; `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-gateway-adapter/src/main/java/top/egon/cola/platform/tianquan/shoubing/yuheng/security/IdpIdentityAuthenticationProvider.java` | 对应Step RED/GREEN与§8矩阵 | ThreadLocal、MDC、异步取消/异常均无跨请求污染 |
| REQ-008 | [主Spec](../spec/2026-09-22-17-14-tianquan-multitenant-oauth-oidc-design.md) §11 | Step 2; Step 4; Step 11 | `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/config/OAuthManagedSchemaContractTest.java`; `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/resources/db/egon-mp/V20260922_001__initialize_oauth_tenant_schema.sql`; `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/resource/domain/po/OAuthResourcePO.java`; `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/resource/domain/bo/OAuthResourceBO.java`; `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/test/java/top/egon/cola/platform/tianquan/shoubing/starter/security/MultiIssuerResourceServerTest.java`; `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/tianquan/shoubing/starter/security/TenantIssuerAuthenticationManagerComponent.java` | 对应Step RED/GREEN与§8矩阵 | 两租户相同业务 ID、批处理/自定义 SQL/逻辑删除不能串租户 |
| REQ-009 | [主Spec](../spec/2026-09-22-17-14-tianquan-multitenant-oauth-oidc-design.md) §7/9/15/16 | Step 12 | `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/test/java/top/egon/cola/platform/tianquan/shoubing/starter/client/IdpServiceOAuth2ClientTest.java`; `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/tianquan/shoubing/starter/client/ServiceAuthorizationKey.java` | 对应Step RED/GREEN与§8矩阵 | 键隔离、同键并发仅一次获取、访问令牌 300–600 秒 |
| REQ-010 | [主Spec](../spec/2026-09-22-17-14-tianquan-multitenant-oauth-oidc-design.md) §7/9/15/16 | Step 1; Step 13; Step 14; Step 15; Step 17 | `egon-cola-xingyuan/pom.xml`; `egon-cola-xingyuan/egon-cola-yuheng/pom.xml`; `egon-cola-xingyuan/egon-cola-yuheng/yuheng-edge-gateway/src/test/java/top/egon/cola/component/yuheng/edge/EdgeLoginContractTest.java`; `egon-cola-xingyuan/egon-cola-yuheng/yuheng-edge-gateway/src/main/java/top/egon/cola/component/yuheng/edge/config/EdgeOAuthProperties.java`; `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-gateway-adapter/src/test/java/top/egon/cola/platform/tianquan/shoubing/yuheng/security/IdpGatewaySecurityProviderTest.java`; `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-gateway-adapter/src/main/java/top/egon/cola/platform/tianquan/shoubing/yuheng/security/IdpIdentityAuthenticationProvider.java`; `egon-cola-xingyuan/egon-cola-xingyuan-admin-web-shared/src/auth/gatewayAuthClient.test.ts`; `egon-cola-xingyuan/egon-cola-xingyuan-admin-web-shared/src/auth/gatewayAuthClient.ts`; `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/LegacyOAuthRetirementTest.java`; `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/controller/filter/LegacyOAuthRetirementFilter.java` | 对应Step RED/GREEN与§8矩阵 | 浏览器只持 HttpOnly 会话 cookie；下游仍自行验签 |
| REQ-011 | [主Spec](../spec/2026-09-22-17-14-tianquan-multitenant-oauth-oidc-design.md) §7/9/15/16 | Step 1; Step 3; Step 8; Step 9; Step 12; Step 13; Step 16 | `egon-cola-xingyuan/pom.xml`; `egon-cola-xingyuan/egon-cola-yuheng/pom.xml`; `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/domain/po/OAuthRealmPO.java`; `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/domain/bo/OAuthRealmBO.java`; `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/controller/OAuthOidcProtocolContractTest.java`; `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/service/OAuthTokenIssueService.java`; `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/controller/OAuthClientControllerTest.java`; `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/domain/dto/CreateOAuthClientDTO.java`; `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/test/java/top/egon/cola/platform/tianquan/shoubing/starter/client/IdpServiceOAuth2ClientTest.java`; `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/tianquan/shoubing/starter/client/ServiceAuthorizationKey.java`; `egon-cola-xingyuan/egon-cola-yuheng/yuheng-edge-gateway/src/test/java/top/egon/cola/component/yuheng/edge/EdgeLoginContractTest.java`; `egon-cola-xingyuan/egon-cola-yuheng/yuheng-edge-gateway/src/main/java/top/egon/cola/component/yuheng/edge/config/EdgeOAuthProperties.java`; `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/tianquan/shoubing/admin/support/migration/OAuthMigrationServiceTest.java`; `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/service/OAuthMigrationService.java` | 对应Step RED/GREEN与§8矩阵 | 生产 HTTP/明文 RPC 配置拒绝；新 client secret bcrypt |
| REQ-012 | [主Spec](../spec/2026-09-22-17-14-tianquan-multitenant-oauth-oidc-design.md) §7/9/15/16 | Step 5; Step 6; Step 10; Step 11; Step 13; Step 16; Step 17 | `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/domain/po/OAuthAuthorizationPO.java`; `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/domain/bo/OAuthAuthorizationBO.java`; `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/domain/po/OAuthSigningKeyPO.java`; `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/domain/bo/OAuthSigningKeyBO.java`; `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/tianquan/shoubing/admin/token/TokenRevocationServiceTest.java`; `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/service/impl/OAuthSecurityProjectionComponent.java`; `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/test/java/top/egon/cola/platform/tianquan/shoubing/starter/security/MultiIssuerResourceServerTest.java`; `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/tianquan/shoubing/starter/security/TenantIssuerAuthenticationManagerComponent.java`; `egon-cola-xingyuan/egon-cola-yuheng/yuheng-edge-gateway/src/test/java/top/egon/cola/component/yuheng/edge/EdgeLoginContractTest.java`; `egon-cola-xingyuan/egon-cola-yuheng/yuheng-edge-gateway/src/main/java/top/egon/cola/component/yuheng/edge/config/EdgeOAuthProperties.java`; `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/tianquan/shoubing/admin/support/migration/OAuthMigrationServiceTest.java`; `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/service/OAuthMigrationService.java`; `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/LegacyOAuthRetirementTest.java`; `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/controller/filter/LegacyOAuthRetirementFilter.java` | 对应Step RED/GREEN与§8矩阵 | 撤销返回成功后新鉴权拒绝，Redis 不可用返回503，在途请求不取消 |
| REQ-013 | [主Spec](../spec/2026-09-22-17-14-tianquan-multitenant-oauth-oidc-design.md) §7/9/15/16 | Step 6; Step 7; Step 9; Step 10 | `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/domain/po/OAuthSigningKeyPO.java`; `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/domain/bo/OAuthSigningKeyBO.java`; `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/TenantIssuerIsolationTest.java`; `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/service/TenantIssuerService.java`; `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/controller/OAuthClientControllerTest.java`; `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/domain/dto/CreateOAuthClientDTO.java`; `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/tianquan/shoubing/admin/token/TokenRevocationServiceTest.java`; `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/service/impl/OAuthSecurityProjectionComponent.java` | 对应Step RED/GREEN与§8矩阵 | 多键重叠，旧 token 可验证至到期，泄露键可紧急拒绝 |
| REQ-014 | [主Spec](../spec/2026-09-22-17-14-tianquan-multitenant-oauth-oidc-design.md) §7/9/15/16 | Step 2; Step 6; Step 10 | `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/config/OAuthManagedSchemaContractTest.java`; `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/resources/db/egon-mp/V20260922_001__initialize_oauth_tenant_schema.sql`; `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/domain/po/OAuthSigningKeyPO.java`; `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/domain/bo/OAuthSigningKeyBO.java`; `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/tianquan/shoubing/admin/token/TokenRevocationServiceTest.java`; `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/service/impl/OAuthSecurityProjectionComponent.java` | 对应Step RED/GREEN与§8矩阵 | 不记录原 token/secret/code/password，投递有恢复证据 |
| REQ-015 | [主Spec](../spec/2026-09-22-17-14-tianquan-multitenant-oauth-oidc-design.md) §7/9/15/16 | Step 6; Step 10 | `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/domain/po/OAuthSigningKeyPO.java`; `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/domain/bo/OAuthSigningKeyBO.java`; `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/tianquan/shoubing/admin/token/TokenRevocationServiceTest.java`; `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/service/impl/OAuthSecurityProjectionComponent.java` | 对应Step RED/GREEN与§8矩阵 | JWT 延迟、缓存命中、失败授权、撤销依赖/投递积压可观测 |
| REQ-016 | [主Spec](../spec/2026-09-22-17-14-tianquan-multitenant-oauth-oidc-design.md) §7/9/15/16 | Step 4; Step 7; Step 10; Step 11; Step 12; Step 14; Step 16 | `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/resource/domain/po/OAuthResourcePO.java`; `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/resource/domain/bo/OAuthResourceBO.java`; `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/TenantIssuerIsolationTest.java`; `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/service/TenantIssuerService.java`; `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/tianquan/shoubing/admin/token/TokenRevocationServiceTest.java`; `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/token/service/impl/OAuthSecurityProjectionComponent.java`; `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/test/java/top/egon/cola/platform/tianquan/shoubing/starter/security/MultiIssuerResourceServerTest.java`; `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/tianquan/shoubing/starter/security/TenantIssuerAuthenticationManagerComponent.java`; `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/test/java/top/egon/cola/platform/tianquan/shoubing/starter/client/IdpServiceOAuth2ClientTest.java`; `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/tianquan/shoubing/starter/client/ServiceAuthorizationKey.java`; `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-gateway-adapter/src/test/java/top/egon/cola/platform/tianquan/shoubing/yuheng/security/IdpGatewaySecurityProviderTest.java`; `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-gateway-adapter/src/main/java/top/egon/cola/platform/tianquan/shoubing/yuheng/security/IdpIdentityAuthenticationProvider.java`; `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/tianquan/shoubing/admin/support/migration/OAuthMigrationServiceTest.java`; `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/service/OAuthMigrationService.java` | 对应Step RED/GREEN与§8矩阵 | `/platform` 只服务控制面；业务 tenant token 必含 tenant_id |
| REQ-017 | [主Spec](../spec/2026-09-22-17-14-tianquan-multitenant-oauth-oidc-design.md) §7/9/15/16 | Step 2; Step 4; Step 5; Step 9; Step 15; Step 16; Step 17 | `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/config/OAuthManagedSchemaContractTest.java`; `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/resources/db/egon-mp/V20260922_001__initialize_oauth_tenant_schema.sql`; `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/resource/domain/po/OAuthResourcePO.java`; `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/resource/domain/bo/OAuthResourceBO.java`; `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/domain/po/OAuthAuthorizationPO.java`; `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/domain/bo/OAuthAuthorizationBO.java`; `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/controller/OAuthClientControllerTest.java`; `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/domain/dto/CreateOAuthClientDTO.java`; `egon-cola-xingyuan/egon-cola-xingyuan-admin-web-shared/src/auth/gatewayAuthClient.test.ts`; `egon-cola-xingyuan/egon-cola-xingyuan-admin-web-shared/src/auth/gatewayAuthClient.ts`; `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/tianquan/shoubing/admin/support/migration/OAuthMigrationServiceTest.java`; `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/service/OAuthMigrationService.java`; `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/LegacyOAuthRetirementTest.java`; `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/controller/filter/LegacyOAuthRetirementFilter.java` | 对应Step RED/GREEN与§8矩阵 | 用户/tenant/client/grant 映射可审计；切换/回退界限明确 |
| REQ-018 | [主Spec](../spec/2026-09-22-17-14-tianquan-multitenant-oauth-oidc-design.md) §11 | Step 1; Step 2; Step 3; Step 16 | `egon-cola-xingyuan/pom.xml`; `egon-cola-xingyuan/egon-cola-yuheng/pom.xml`; `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/config/OAuthManagedSchemaContractTest.java`; `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/resources/db/egon-mp/V20260922_001__initialize_oauth_tenant_schema.sql`; `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/domain/po/OAuthRealmPO.java`; `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/domain/bo/OAuthRealmBO.java`; `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/tianquan/shoubing/admin/support/migration/OAuthMigrationServiceTest.java`; `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/service/OAuthMigrationService.java` | 对应Step RED/GREEN与§8矩阵 | §6.2、§20 每项证据闭合；保留传统业务分包 |
| REQ-019 | [主Spec](../spec/2026-09-22-17-14-tianquan-multitenant-oauth-oidc-design.md) §7/9/15/16 | Step 16; Step 17 | `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/tianquan/shoubing/admin/support/migration/OAuthMigrationServiceTest.java`; `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/service/OAuthMigrationService.java`; `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/LegacyOAuthRetirementTest.java`; `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/controller/filter/LegacyOAuthRetirementFilter.java` | 对应Step RED/GREEN与§8矩阵 | git diff 只有本 Spec；无 Plan/代码/运行态动作 |

## 11. Risks, Blockers, and User Decisions

| ID | Risk or decision | Impacted Steps/files | Evidence | Owner | Status/action |
| --- | --- | --- | --- | --- | --- |
| DEC-001–008 | 用户已确认SAS/SCG/MP/PLATFORM/Session/严格撤销/重登录/bcrypt | 全部Step | 主Spec §5.3与本轮确认 | mario | Closed；Plan不重新询问 |
| RISK-001 | 默认~/.m2未缓存Boot/Cloud BOM，离线验证需要执行环境提供已批准依赖 | Step1/所有Maven Gate | 2026-09-23本机只读目录检查；root Boot3.5.16 | 实施环境负责人 | 运行前置：批准执行前由环境负责人准备已批准坐标并核对effective POM；不构成Plan设计决定 |
| RISK-002 | NATIVE单PRIMARY与现有JPA/MP/Outbox是否共用JDBC连接 | Step2–9 | MP bootstrapper/Runner与JpaTransactionManager源码+主Spec §15.1 | 实施执行者 | 必须由真实PG事务联测证明；失败回Spec，不换双DataSource悄然继续 |
| RISK-003 | 全量audit spool与Redis受控fence能力取决于环境 | Step9/12/15 | 主Spec §15.1/15.3 | 运营/用户环境 | 运行发布门禁：依§8用户控制的故障演练验证；不可静默降级 |
| RISK-004 | 当前5.4.1是发布号漂移 | Step1 | root/SA/Y当前POM与主Spec旧基线比较 | 实施执行者 | Plan Clarification 001已闭合；行为/API未漂移 |

## 12. Review and Acceptance

### 12.1 Original requirement fidelity

REQ-001–019均出现在至少一个Step与§10；先数据与协议，后所有消费者和用户前端，最后明确旧链退出。User禁止本次code generator、服务/浏览器自动启动、旧Flyway历史改动均在§6/9执行边界。

### 12.2 Spec consistency

当前17个Step覆盖主Spec已接受的§7–§16元素（Step 15含授柄管理页realm/密钥/资源Consumer）；Grant页面现有GET仍缺主Spec合同，不能把当前Plan判定为完整。没有参数预取API；resource catalog因旧管理client FK与新realm冲突迁移第11表。JPA目录保留，认证新PO仅MP；三层业务包保留。Plan Clarification五项仅发布号/机械路径/局部JDK原语/内部输入输出类型/响应式Reader。主Spec外部API-001–044与EVENT/JOB/INTERNAL由Steps 7–17及对应§8合同检查覆盖；修订API-045等待用户确认后必须增入Step 9/15、§5/§8/§10，再重新执行本节审核。

### 12.3 Repository executability

当前HEAD/POM/Java21/SA旧Controller/SS Client/Jianshen过滤器/Y biz/mcp和前端路径已核对。每File说明输入输出、状态、错误、依赖及后置；Step compile前置、RED、GREEN、scope和路径限定commit明确。当前未跟踪__pycache__保持原样。尚未执行Step，不称测试/数据库通过。

### 12.4 Test and release completeness

§8分离单元合同、真实PostgreSQL/Redis及用户启动的完整链路；Grant列表API-045在修订获批后须补Controller/Service/Mapper和前端合同测试，当前未覆盖。审计全量、共享撤销、源/目标Tenant、TLS/PKCE/nonce/重复消费/密钥交替和旧会话失效各有验收。生产指标阈值靠真实测量，不靠本Plan静态推断。

### 12.5 Blocking Manual Check

| Check ID | Applicability | Status | Evidence | Finding | Required action/exception |
| --- | --- | --- | --- | --- | --- |
| MC-ARCH-001 | Applicable | PASS | 主Spec §6.2/本Plan §4.7–4.8/§5–8；backend-code-generation.md支持范围 | 设计和文件/验证已闭合；运行状态须未来实施验证 | None；未来执行按每Step证据完成 |
| MC-REUSE-001 | Applicable | PASS | 主Spec §6.2/本Plan §4.7–4.8/§5–8；对应Step精确文件/测试 | 设计和文件/验证已闭合；运行状态须未来实施验证 | None；未来执行按每Step证据完成 |
| MC-DEP-001 | Applicable | PASS | 主Spec §6.2/本Plan §4.7–4.8/§5–8；backend-code-generation.md支持范围 | 设计和文件/验证已闭合；运行状态须未来实施验证 | None；未来执行按每Step证据完成 |
| MC-NAME-001 | Applicable | PASS | 主Spec §6.2/本Plan §4.7–4.8/§5–8；对应Step精确文件/测试 | 设计和文件/验证已闭合；运行状态须未来实施验证 | None；未来执行按每Step证据完成 |
| MC-VALID-001 | Applicable | PASS | 主Spec §6.2/本Plan §4.7–4.8/§5–8；对应Step精确文件/测试 | 设计和文件/验证已闭合；运行状态须未来实施验证 | None；未来执行按每Step证据完成 |
| MC-MODEL-001 | Applicable | PASS | 主Spec §6.2/本Plan §4.7–4.8/§5–8；对应Step精确文件/测试 | 设计和文件/验证已闭合；运行状态须未来实施验证 | None；未来执行按每Step证据完成 |
| MC-CONVERT-001 | Applicable | PASS | 主Spec §6.2/本Plan §4.7–4.8/§5–8；对应Step精确文件/测试 | 设计和文件/验证已闭合；运行状态须未来实施验证 | None；未来执行按每Step证据完成 |
| MC-LOG-001 | Applicable | PASS | 主Spec §6.2/本Plan §4.7–4.8/§5–8；对应Step精确文件/测试 | 设计和文件/验证已闭合；运行状态须未来实施验证 | None；未来执行按每Step证据完成 |
| MC-BEAN-001 | Applicable | PASS | 主Spec §6.2/本Plan §4.7–4.8/§5–8；对应Step精确文件/测试 | 设计和文件/验证已闭合；运行状态须未来实施验证 | None；未来执行按每Step证据完成 |
| MC-UTIL-001 | Applicable | PASS | 主Spec §6.2/本Plan §4.7–4.8/§5–8；对应Step精确文件/测试 | 设计和文件/验证已闭合；运行状态须未来实施验证 | None；未来执行按每Step证据完成 |
| MC-JSON-001 | Applicable | PASS | 主Spec §6.2/本Plan §4.7–4.8/§5–8；对应Step精确文件/测试 | 设计和文件/验证已闭合；运行状态须未来实施验证 | None；未来执行按每Step证据完成 |
| MC-TIME-001 | Applicable | PASS | 主Spec §6.2/本Plan §4.7–4.8/§5–8；对应Step精确文件/测试 | 设计和文件/验证已闭合；运行状态须未来实施验证 | None；未来执行按每Step证据完成 |
| MC-CONFIG-001 | Applicable | PASS | 主Spec §6.2/本Plan §4.7–4.8/§5–8；对应Step精确文件/测试 | 设计和文件/验证已闭合；运行状态须未来实施验证 | None；未来执行按每Step证据完成 |
| MC-PATTERN-001 | Applicable | PASS | 主Spec §6.2/本Plan §4.7–4.8/§5–8；对应Step精确文件/测试 | 设计和文件/验证已闭合；运行状态须未来实施验证 | None；未来执行按每Step证据完成 |
| MC-SCOPE-001 | Applicable | BLOCKED | 主Spec §6.2/本Plan §4.7–4.8/§5–8；对应Step精确文件/测试 | 设计和文件/验证已闭合；运行状态须未来实施验证 | mario确认Grant修订；主agent据API-045补Step 9/15、追踪及测试后重验 |
| MC-TEST-001 | Applicable | BLOCKED | 主Spec §6.2/本Plan §4.7–4.8/§5–8；对应Step精确文件/测试 | 设计和文件/验证已闭合；运行状态须未来实施验证 | mario确认Grant修订；主agent据API-045补Step 9/15、追踪及测试后重验 |
| MC-BLOCKER-001 | Applicable | BLOCKED | 主Spec §6.2/本Plan §4.7–4.8/§5–8；对应Step精确文件/测试 | Grant列表公开合同修订仍待用户审核，不能把前端调用404留在可执行Plan | mario确认Grant修订；主agent据API-045补Step 9/15、追踪及测试后重验 |

### 12.6 Final verdict

**BLOCKED — Spec or user decision required**

当前Plan已具主Spec的逐文件路径和验证，但修订API-045未获确认，Status=Blocked；用户确认后补对应Step并再次严格校验，才能改为Review。本次未写生产/测试/迁移代码、未使用code generator、未启动项目或外部服务。
